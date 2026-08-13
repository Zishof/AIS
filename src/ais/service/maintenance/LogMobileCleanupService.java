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

	private static final int UKURAN_BATCH = 500;
	private static final long JEDA_BATCH_MS = 100L;
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

	/**
	 * Hapus baris {@code log_mobile} lebih tua dari ambang retensi dalam batch kecil.
	 * Satu DELETE besar pernah melewati {@code statement_timeout} PostgreSQL dan juga
	 * menahan lock serta transaksi terlalu lama pada instalasi dengan backlog log besar.
	 */
	private static void bersihkanSekali() {
		Date cutoff = new Date(System.currentTimeMillis() - retensiHari() * 24L * 60L * 60L * 1000L);
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		int totalDihapus = 0;
		try {
			while (!Thread.currentThread().isInterrupted()) {
				tx = session.beginTransaction();
				int dihapus = session.createSQLQuery(
						"delete from public.log_mobile where id in ("
								+ "select id from public.log_mobile where login < :cutoff "
								+ "order by id asc limit " + UKURAN_BATCH + ")")
						.setTimestamp("cutoff", cutoff).executeUpdate();
				tx.commit();
				tx = null;
				totalDihapus += dihapus;
				session.clear();

				if (dihapus < UKURAN_BATCH) {
					break;
				}
				try {
					Thread.sleep(JEDA_BATCH_MS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
			System.out.println("[LogMobileCleanup] " + totalDihapus + " baris log_mobile lebih tua dari "
					+ retensiHari() + " hari dihapus (cutoff=" + cutoff + ")");
		} catch (Exception e) {
			try {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception rollbackEx) {
				ErrorAuditUtil.record(rollbackEx, "auto-audit(empty-catch) LogMobileCleanupService.rollback");
			}
			ErrorAuditUtil.record(e, "auto-audit LogMobileCleanupService.bersihkanSekali");
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
