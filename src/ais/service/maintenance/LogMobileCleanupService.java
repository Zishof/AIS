package ais.service.maintenance;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.common.Common;
import ais.common.ErrorAuditUtil;
import ais.database.hibernate.HibernateUtil;

/**
 * <h3>Housekeeping tabel {@code public.log_mobile} -- pola scheduler SAMA dgn
 * {@link ais.service.tenant.TenantProvisioningWorker}.</h3>
 *
 * <p>Log request/response API mobile ({@code ApiMobileLogger}) menumpuk terus tanpa batas dan
 * fungsinya sekunder (debug/audit, bukan data transaksional) -- dibiarkan lama membuat tabel
 * membengkak. Sekali per START webapp (BUKAN periodik berulang), hapus baris yang lebih tua dari
 * ambang retensi (default 30 hari, dapat diubah via konfigurasi tanpa deploy ulang) di THREAD LATAR
 * daemon supaya tidak memperlambat startup.</p>
 *
 * <p>Lifecycle: {@code mulai()} dipanggil {@link ais.common.LogMobileCleanupListener}
 * (ServletContextListener terpisah, load otomatis lewat web.xml) -- SENGAJA TIDAK menyentuh
 * {@code AppStartupListener} (file panas yang sedang dikerjakan sesi lain), sama seperti alasan
 * TenantProvisioningWorker. {@code hentikan()} dari {@code contextDestroyed()} untuk jaga-jaga bila
 * tugas belum sempat jalan saat webapp direstart/di-reload cepat.</p>
 */
public final class LogMobileCleanupService {

	private static volatile ScheduledExecutorService penjadwal;

	private LogMobileCleanupService() {
	}

	private static int retensiHari() {
		try {
			return Integer.parseInt(Common
					.getKonfigurasi("log_mobile_retensi_hari", "30").getNilai().trim());
		} catch (Exception e) {
			return 30;
		}
	}

	/** Jadwalkan SATU KALI pembersihan beberapa saat setelah startup (bukan periodik berulang). */
	public static synchronized void mulai() {
		if (penjadwal != null) {
			return;
		}
		final ScheduledExecutorService s = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "log-mobile-cleanup");
				t.setDaemon(true);
				return t;
			}
		});
		// Delay 120 dtk: biarkan startup webapp (ZK/SessionFactory/listener lain) selesai lebih
		// dulu, sama seperti delay awal TenantProvisioningWorker.
		s.schedule(new Runnable() {
			public void run() {
				try {
					bersihkanSekali();
				} catch (Throwable t) {
					ErrorAuditUtil.record(t instanceof Exception ? (Exception) t : new RuntimeException(t),
							"auto-audit LogMobileCleanupService.tick");
				} finally {
					s.shutdown();
					penjadwal = null;
				}
			}
		}, 120, TimeUnit.SECONDS);
		penjadwal = s;
	}

	public static synchronized void hentikan() {
		ScheduledExecutorService s = penjadwal;
		penjadwal = null;
		if (s != null) {
			s.shutdownNow();
		}
	}

	/** Ambang jumlah baris tiap batch delete -- lihat javadoc {@link #bersihkanSekali()}. */
	private static final int UKURAN_BATCH_HAPUS = 5000;

	/** Batas jumlah iterasi batch per pemanggilan -- jaga-jaga agar tidak berjalan tanpa henti
	 * bila tabel sangat besar; sisanya akan dilanjutkan pada jadwal cleanup berikutnya. */
	private static final int MAKS_BATCH_PER_JALAN = 200;

	/**
	 * Hapus baris {@code log_mobile} lebih tua dari ambang retensi. Buka/tutup session sendiri.
	 *
	 * <p>Gap-closure "canceling statement due to statement timeout": SEBELUMNYA satu DELETE HQL
	 * tunggal tanpa batas menghapus SELURUH baris kedaluwarsa sekaligus -- pada tabel log_mobile
	 * yang tumbuh terus (dicatat tiap request mobile), jumlah baris yang cocok bisa jutaan,
	 * membuat satu statement DELETE berjalan lebih lama dari statement_timeout server dan
	 * dibatalkan paksa (transaksi gagal total, TIDAK ADA baris yang berhasil terhapus walau
	 * prosesnya sudah berjalan lama). Sekarang dihapus per-batch (native SQL + ctid, idiom umum
	 * Postgres utk "DELETE ... LIMIT") dalam transaksi kecil terpisah per batch -- tiap statement
	 * jauh lebih cepat drpd batas timeout, dan progres yang sudah terhapus tetap tersimpan walau
	 * batch berikutnya gagal/dibatalkan.</p>
	 */
	private static void bersihkanSekali() {
		Date cutoff = new Date(System.currentTimeMillis() - retensiHari() * 24L * 60L * 60L * 1000L);
		int totalDihapus = 0;
		int batchKe = 0;
		while (batchKe < MAKS_BATCH_PER_JALAN) {
			batchKe++;
			int dihapusBatch = hapusSatuBatch(cutoff);
			totalDihapus += dihapusBatch;
			if (dihapusBatch < UKURAN_BATCH_HAPUS) {
				// Batch terakhir (baris tersisa lebih sedikit dari ukuran batch, atau 0) -> selesai.
				break;
			}
		}
		System.out.println("[LogMobileCleanup] " + totalDihapus + " baris log_mobile lebih tua dari "
				+ retensiHari() + " hari dihapus dalam " + batchKe + " batch (cutoff=" + cutoff + ")");
	}

	/** Hapus SATU batch (maks {@link #UKURAN_BATCH_HAPUS} baris) dalam transaksi tersendiri. */
	private static int hapusSatuBatch(Date cutoff) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			int dihapus = session.createSQLQuery(
					"delete from public.log_mobile where ctid in ("
							+ "select ctid from public.log_mobile where login < :cutoff limit :batchSize)")
					.setTimestamp("cutoff", cutoff).setInteger("batchSize", UKURAN_BATCH_HAPUS).executeUpdate();
			tx.commit();
			return dihapus;
		} catch (Exception e) {
			try {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception rollbackEx) {
				ErrorAuditUtil.record(rollbackEx, "auto-audit(empty-catch) LogMobileCleanupService.rollback");
			}
			ErrorAuditUtil.record(e, "auto-audit LogMobileCleanupService.bersihkanSekali");
			return 0;
		} finally {
			try {
				session.clear();
				session.disconnect();
				session.close();
			} catch (Exception closeEx) {
				ErrorAuditUtil.record(closeEx, "auto-audit(empty-catch) LogMobileCleanupService.close");
			}
		}
	}
}
