package ais.action.master.repository;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.common.Common;
import ais.common.ErrorAuditUtil;
import ais.database.hibernate.HibernateUtil;

/**
 * Penjadwal sinkronisasi lokal repository. Data sumber yang baru/berubah masuk ke
 * repo_item secara idempoten tanpa menunggu operator menekan tombol sinkron.
 */
public final class RepositorySyncScheduler {

	private static volatile ScheduledExecutorService scheduler;

	private RepositorySyncScheduler() {
	}

	public static synchronized void mulai() {
		if (scheduler != null) {
			return;
		}
		String enabled = Common.getKonfigurasi("repository_auto_sync", "Aktif").getNilai();
		if (!"Aktif".equalsIgnoreCase(enabled) && !"true".equalsIgnoreCase(enabled)) {
			System.out.println("[Repository] Sinkron otomatis dinonaktifkan oleh konfigurasi.");
			return;
		}
		int intervalMinutes = parsePositiveInt(
				Common.getKonfigurasi("repository_auto_sync_interval_minutes", "60").getNilai(), 60);
		ScheduledExecutorService service = Executors.newScheduledThreadPool(1,
				daemonFactory("repository-auto-sync"));
		service.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					jalankanSekali();
				} catch (Throwable error) {
					ErrorAuditUtil.record(error,
							"auto-audit src/ais/action/master/repository/RepositorySyncScheduler.java:run");
				}
			}
		}, 10, intervalMinutes, TimeUnit.MINUTES);
		scheduler = service;
		System.out.println("[Repository] Sinkron otomatis aktif tiap " + intervalMinutes + " menit.");
	}

	public static synchronized void hentikan() {
		ScheduledExecutorService service = scheduler;
		scheduler = null;
		if (service != null) {
			service.shutdownNow();
		}
	}

	public static RepositorySyncService.SyncSummary jalankanSekali() {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			session.beginTransaction();
			RepositorySyncService.SyncSummary summary = RepositorySyncService.synchronizeAllBackground(session, false, true, null);
			/* synchronizeAll dapat me-rollback transaksi per item lalu membuka transaksi
			 * pengganti. Selalu commit transaksi AKTIF milik session saat ini. */
			// KE-FIX: bila summary.isConnectionLost() true, session/koneksi sudah terbukti tidak
			// sehat (lihat javadoc SyncSummary.connectionLost & KE-FIX item/flush-catch di
			// RepositorySyncService) -- rollback() di sini BISA IKUT GAGAL (mis. koneksi sudah
			// tertutup) dan sebelumnya melempar TransactionException baru yg tidak informatif
			// ("JDBC rollback failed / This connection has been closed"). Bungkus aman: cek
			// session.isOpen() & transaksi masih aktif dulu, dan jangan biarkan kegagalan rollback
			// sekunder ini menghentikan method (session akan tetap ditutup di finally lewat
			// closeSessionQuietly, dan siklus berikutnya membuka session baru).
			try {
				Transaction current = session.isOpen() ? session.getTransaction() : null;
				if (summary.isConnectionLost()) {
					/* Jangan panggil rollback pada koneksi fisik yang sudah terbukti mati.
					 * Hibernate/c3p0 akan mencoba PgConnection.rollback() dan menghasilkan
					 * rangkaian warning "PooledConnection ... still in use". Session dibuang
					 * oleh finally; siklus scheduler berikutnya membuka koneksi yang baru. */
				} else if (current != null && current.isActive()) {
					current.commit();
				}
			} catch (Exception commitEx) {
				if (!summary.isConnectionLost()) {
					// Jalur normal (bukan connection-lost yg sudah tercatat) -- catat supaya
					// kegagalan commit/rollback tetap terlihat di audit.
					ErrorAuditUtil.record(commitEx,
							"auto-audit src/ais/action/master/repository/RepositorySyncScheduler.java:commit-or-rollback");
				}
				// connection-lost sudah tercatat oleh RepositorySyncService; jangan duplikasi log
				// dgn exception sekunder yg cuma menegaskan koneksi sudah mati.
			}
			System.out.println("[Repository] Sinkron otomatis selesai: dipindai=" + summary.getScanned()
					+ ", berhasil=" + summary.getSynced() + ", gagal=" + summary.getFailed());
			return summary;
		} catch (RuntimeException error) {
			// Untuk koneksi mati, rollback pada session yang sama tidak mungkin memulihkan
			// transaksi dan justru menambah exception sekunder. Langsung buang session.
			boolean koneksiMati = Common.isTransientKoneksiError(error);
			if (!koneksiMati && session != null) {
				try {
					if (session.isOpen()) {
						Transaction current = session.getTransaction();
						if (current != null && current.isActive()) {
							current.rollback();
						}
					}
				} catch (Throwable ignored) {
					ErrorAuditUtil.record(ignored,
							"auto-audit(empty-catch) src/ais/action/master/repository/RepositorySyncScheduler.java:rollback");
				}
			}
			throw error;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static int parsePositiveInt(String value, int fallback) {
		try {
			int parsed = Integer.parseInt(value == null ? "" : value.trim());
			return parsed > 0 ? parsed : fallback;
		} catch (Exception ignored) {
			return fallback;
		}
	}

	private static ThreadFactory daemonFactory(final String name) {
		return new ThreadFactory() {
			private final AtomicInteger sequence = new AtomicInteger(1);

			@Override
			public Thread newThread(Runnable runnable) {
				Thread thread = new Thread(runnable, name + "-" + sequence.getAndIncrement());
				thread.setDaemon(true);
				return thread;
			}
		};
	}
}
