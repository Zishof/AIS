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

	private static int intervalDetik() {
		try {
			return Integer.parseInt(Common
					.getKonfigurasi("pendaftaran_provisioning_interval_detik", "60").getNilai().trim());
		} catch (Exception e) {
			return 60;
		}
	}

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
		} catch (Exception e) {
			// Kalah race klaim (StaleObjectState) itu normal pada multi-node -- bukan error alur.
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception rollbackEx) { ais.common.ErrorAuditUtil.record(rollbackEx, "auto-audit(empty-catch) TenantProvisioningWorker.klaim.rollback");
			}
			return null;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

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
