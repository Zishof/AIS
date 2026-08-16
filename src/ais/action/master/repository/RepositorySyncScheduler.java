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
			RepositorySyncService.SyncSummary summary = RepositorySyncService.synchronizeAll(session, false, true, null);
			/* synchronizeAll dapat me-rollback transaksi per item lalu membuka transaksi
			 * pengganti. Selalu commit transaksi AKTIF milik session saat ini. */
			Transaction current = session.getTransaction();
			if (summary.isConnectionLost()) {
				if (current != null && current.isActive()) {
					current.rollback();
				}
			} else if (current != null && current.isActive()) {
				current.commit();
			}
			System.out.println("[Repository] Sinkron otomatis selesai: dipindai=" + summary.getScanned()
					+ ", berhasil=" + summary.getSynced() + ", gagal=" + summary.getFailed());
			return summary;
		} catch (RuntimeException error) {
			if (session != null) {
				try {
					Transaction current = session.getTransaction();
					if (current != null && current.isActive()) {
						current.rollback();
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
