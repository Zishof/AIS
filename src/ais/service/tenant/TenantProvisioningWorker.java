package ais.service.tenant;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.tenant.ProvisioningJob;

/**
 * <h3>Worker latar provisioning tenant -- pola scheduler {@code DepositoAroScheduler}.</h3>
 *
 * <p>Daemon {@link ScheduledExecutorService} men-poll {@code provisioning_job} berstatus QUEUED
 * (retry_at ≤ now) atau RUNNING dgn lease basi (locked_at > 15 menit -- node mati). Klaim job =
 * UPDATE locked_by/locked_at + status RUNNING dalam transaksi kecil; {@code @Version} pada entity
 * membuat dua node yang berebut job sama kalah salah satu (StaleObjectStateException → lewati).
 * SENGAJA tanpa {@code FOR UPDATE SKIP LOCKED} (PostgreSQL deployment bisa 9.3).</p>
 *
 * <p>Lifecycle: {@code mulai()} dipanggil {@code PendaftaranTenantServlet.init()} (load-on-startup),
 * {@code hentikan()} dari {@code destroy()} -- TIDAK menyentuh {@code AppStartupListener} (file
 * panas yang sedang dikerjakan sesi lain). Thread latar TIDAK lewat FilterJSP: sesi Hibernate
 * dibuka/ditutup sendiri di setiap unit kerja.</p>
 */
public final class TenantProvisioningWorker {

	private static volatile ScheduledExecutorService penjadwal;
	private static final long LEASE_BASI_MS = 15L * 60L * 1000L;

	private TenantProvisioningWorker() {
	}

	/**
	 * Selang antar-tick poller, dibaca dari konfigurasi
	 * {@code pendaftaran_provisioning_interval_detik} (default {@code 60} detik). Nilai yang
	 * tidak dapat diparse (kosong, bukan angka) jatuh ke default yang sama.
	 */
	private static int intervalDetik() {
		try {
			return Integer.parseInt(Common
					.getKonfigurasi("pendaftaran_provisioning_interval_detik", "60").getNilai().trim());
		} catch (Exception e) {
			return 60;
		}
	}

	/**
	 * Nyalakan poller latar bila belum berjalan. Aman dipanggil berulang -- panggilan kedua dan
	 * seterusnya tidak melakukan apa-apa selama {@link #penjadwal} sudah terisi (idempoten,
	 * dipanggil dari {@code PendaftaranTenantServlet.init()} pada load-on-startup).
	 *
	 * <p>Thread poller adalah daemon bernama {@code tenant-provisioning}, dijadwalkan lewat
	 * {@link ScheduledExecutorService#scheduleAtFixedRate} dengan delay awal 120 detik (beri
	 * waktu aplikasi selesai start-up sebelum tick pertama) dan periode berikutnya sesuai
	 * {@link #intervalDetik()}. Setiap tick memanggil {@link #prosesAntrean()} dan menelan
	 * {@link Throwable} apa pun supaya satu tick yang gagal tidak mematikan seluruh scheduler.</p>
	 */
	public static synchronized void mulai() {
		if (penjadwal != null) {
			return;
		}
		ScheduledExecutorService s = Executors.newScheduledThreadPool(1, new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "tenant-provisioning");
				t.setDaemon(true);
				return t;
			}
		});
		int interval = intervalDetik();
		s.scheduleAtFixedRate(new Runnable() {
			public void run() {
				try {
					prosesAntrean();
				} catch (Throwable t) {
					// Scheduler tidak boleh mati karena satu tick gagal.
					ais.common.ErrorAuditUtil.record(t instanceof Exception ? (Exception) t
							: new RuntimeException(t), "auto-audit TenantProvisioningWorker.tick");
				}
			}
		}, 120, interval, TimeUnit.SECONDS);
		penjadwal = s;
	}

	/**
	 * Hentikan poller latar (dipanggil dari {@code destroy()} servlet). Memakai
	 * {@link ScheduledExecutorService#shutdownNow()} -- tick yang sedang berjalan boleh
	 * diinterupsi; ini aman karena setiap unit kerja ({@link #klaimSatuJob()} maupun step
	 * provisioning) berjalan dalam transaksi pendek yang di-rollback bila terputus, bukan
	 * ditinggal dalam keadaan setengah jalan. Aman dipanggil walau poller belum pernah
	 * dinyalakan.
	 */
	public static synchronized void hentikan() {
		ScheduledExecutorService s = penjadwal;
		penjadwal = null;
		if (s != null) {
			s.shutdownNow();
		}
	}

	/** Proses maksimal 5 job per tick (jaga tick pendek; sisanya tick berikutnya). */
	public static int prosesAntrean() {
		int diproses = 0;
		for (int i = 0; i < 5; i++) {
			Long jobId = klaimSatuJob();
			if (jobId == null) {
				break;
			}
			TenantProvisioningService.jalankanJob(jobId);
			diproses++;
		}
		return diproses;
	}

	/** Klaim satu job siap-proses; null bila antrean kosong / kalah race klaim. */
	private static Long klaimSatuJob() {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			Date sekarang = new Date();
			Date leaseBasi = new Date(sekarang.getTime() - LEASE_BASI_MS);
			List<?> kandidat = session.createCriteria(ProvisioningJob.class)
					.add(Restrictions.or(
							Restrictions.and(
									Restrictions.eq("status", ProvisioningJob.STATUS_QUEUED),
									Restrictions.or(Restrictions.isNull("retryAt"),
											Restrictions.le("retryAt", sekarang))),
							Restrictions.and(
									Restrictions.eq("status", ProvisioningJob.STATUS_RUNNING),
									Restrictions.le("lockedAt", leaseBasi))))
					.addOrder(Order.asc("id")).setMaxResults(1).list();
			if (kandidat.isEmpty()) {
				session.getTransaction().rollback();
				return null;
			}
			ProvisioningJob job = (ProvisioningJob) kandidat.get(0);
			job.setStatus(ProvisioningJob.STATUS_RUNNING);
			job.setLockedBy(identitasNode());
			job.setLockedAt(sekarang);
			job.setAttempt(Integer.valueOf(job.getAttempt().intValue() + 1));
			if (job.getStartedAt() == null) {
				job.setStartedAt(sekarang);
			}
			session.saveOrUpdate(job);
			Long id = job.getId();
			session.getTransaction().commit(); // @Version: node lain yang mengklaim bersamaan gagal di sini
			return id;
		} catch (org.hibernate.StaleStateException staleEx) {
			// Kalah race klaim (StaleObjectState/StaleState via @Version) itu normal pada multi-node --
			// bukan error alur, tetap ditelan diam-diam seperti semula.
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception rollbackEx) { ais.common.ErrorAuditUtil.record(rollbackEx, "auto-audit(empty-catch) TenantProvisioningWorker.klaim.rollback");
			}
			return null;
		} catch (Exception e) {
			// FIX (gap observability): sebelumnya SEMUA Exception (termasuk PSQLException koneksi
			// mati/dead c3p0 connection -- "This connection has been closed", SQLState 08006) ikut
			// ditelan diam-diam di sini seolah cuma kalah race klaim StaleObjectState (lihat JavaDoc
			// kelas ini), sehingga gangguan koneksi DB yang sebenarnya TIDAK PERNAH terlihat di
			// Error Log. Sekarang hanya StaleStateException (race klaim -- benar-benar normal) yang
			// ditelan diam-diam; exception lain (mis. koneksi mati) diaudit di sini supaya ops bisa
			// melihat gangguan berulang, tapi worker TETAP tidak berhenti (return null, tick berikut
			// membuka session/koneksi baru dari pool seperti biasa -- tidak ada perubahan perilaku
			// pemulihan, hanya visibilitas).
			/* KE-FIX ("An I/O error occurred while sending to the backend" berulang tiap tick).
			 * Dua masalah pada koneksi yang sudah MATI:
			 *   1) rollback() di bawah memakai koneksi yang sama sehingga gagal lagi dan
			 *      menambah exception SEKUNDER yang tidak informatif;
			 *   2) worker berjalan tiap beberapa detik, jadi gangguan koneksi yang berlangsung
			 *      beberapa menit menghasilkan ratusan baris audit identik yang menenggelamkan
			 *      galat lain.
			 * Perilaku pemulihan TIDAK berubah: tetap return null dan tick berikutnya membuka
			 * session/koneksi baru dari pool. Yang berubah hanya kebisingannya. */
			boolean koneksiMati = ais.common.Common.isTransientKoneksiError(e);
			if (!koneksiMati || bolehCatatKoneksiMati()) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit TenantProvisioningWorker.klaimSatuJob");
			}
			if (!koneksiMati) {
				try {
					if (session.getTransaction() != null && session.getTransaction().isActive()) {
						session.getTransaction().rollback();
					}
				} catch (Exception rollbackEx) { ais.common.ErrorAuditUtil.record(rollbackEx, "auto-audit(empty-catch) TenantProvisioningWorker.klaim.rollback");
				}
			}
			return null;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Jeda minimal antar-catatan audit untuk gangguan koneksi yang sama. */
	private static final long JEDA_AUDIT_KONEKSI_MS = 10L * 60L * 1000L;

	/** Epoch milidetik audit koneksi-mati terakhir; 0 berarti belum pernah. */
	private static volatile long auditKoneksiTerakhirMs = 0L;

	/**
	 * @return true bila gangguan koneksi kali ini layak dicatat (catatan pertama, atau sudah
	 *         lewat {@link #JEDA_AUDIT_KONEKSI_MS} sejak catatan terakhir).
	 */
	private static synchronized boolean bolehCatatKoneksiMati() {
		long sekarang = System.currentTimeMillis();
		if (auditKoneksiTerakhirMs == 0L || sekarang - auditKoneksiTerakhirMs >= JEDA_AUDIT_KONEKSI_MS) {
			auditKoneksiTerakhirMs = sekarang;
			return true;
		}
		return false;
	}

	/**
	 * Identitas node+thread yang mengklaim sebuah job, ditulis ke
	 * {@code provisioning_job.locked_by} untuk memudahkan diagnosis lease basi pada deployment
	 * multi-node. Format {@code <hostname>|<nama thread>}, dipotong ke 128 karakter agar muat di
	 * kolom. Jatuh ke {@code "node"} bila hostname gagal diresolusi.
	 */
	private static String identitasNode() {
		String host;
		try {
			host = java.net.InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			host = "node";
		}
		String id = host + "|" + Thread.currentThread().getName();
		return id.length() > 128 ? id.substring(0, 128) : id;
	}
}
