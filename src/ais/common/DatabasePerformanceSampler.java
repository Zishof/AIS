package ais.common;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.hibernate.Session;

import ais.database.hibernate.HibernateUtil;

/** Sampel query aktif/lambat berkala untuk dashboard operasional. */
public final class DatabasePerformanceSampler {
	private static volatile ScheduledExecutorService scheduler;

	private DatabasePerformanceSampler() {
	}

	public static synchronized void mulai() {
		if (scheduler != null && !scheduler.isShutdown()) return;
		scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			public Thread newThread(Runnable runnable) {
				Thread thread = new Thread(runnable, "ais-database-performance-sampler");
				thread.setDaemon(true);
				return thread;
			}
		});
		scheduler.scheduleWithFixedDelay(new Runnable() {
			public void run() {
				try {
					ambilSampel();
				} catch (Throwable e) {
					// Best effort dan tidak boleh menjatuhkan aplikasi. ErrorAudit melakukan
					// dedup lokasi sehingga gangguan DB tidak membanjiri tabel error_log.
					ErrorAuditUtil.record(e, "DatabasePerformanceSampler.ambilSampel");
				}
			}
		}, 2, 5, TimeUnit.MINUTES);
	}

	public static synchronized void berhenti() {
		if (scheduler != null) scheduler.shutdownNow();
		scheduler = null;
	}

	private static void ambilSampel() throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			PreparedStatement read = session.connection().prepareStatement(
					"SELECT round(extract(epoch from (clock_timestamp()-query_start))*1000)::bigint, "
					+ "left(regexp_replace(coalesce(query,''),'[[:space:]]+',' ','g'),1000) "
					+ "FROM pg_stat_activity WHERE datname=current_database() AND pid<>pg_backend_pid() "
					+ "AND state='active' AND clock_timestamp()-query_start > interval '1 second' "
					+ "ORDER BY query_start ASC LIMIT 50");
			ResultSet rs = read.executeQuery();
			PreparedStatement insert = session.connection().prepareStatement(
					"INSERT INTO public.database_performance_sample(source,query_fingerprint,duration_ms,calls,detail) "
					+ "VALUES ('ACTIVE_QUERY',?,?,1,?)");
			while (rs.next()) {
				long duration = rs.getLong(1);
				String query = rs.getString(2);
				insert.setString(1, sha256(query));
				insert.setLong(2, duration);
				insert.setString(3, query);
				insert.addBatch();
			}
			insert.executeBatch();
			insert.close();
			rs.close();
			read.close();
			session.createSQLQuery(
					"DELETE FROM public.database_performance_sample WHERE captured_at < now() - interval '30 days'")
					.executeUpdate();
			session.getTransaction().commit();
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception rollbackError) {
				ErrorAuditUtil.record(rollbackError, "DatabasePerformanceSampler.rollback");
			}
			throw e;
		} finally {
			/* Sesi dibuka sendiri lewat openSession() sehingga WAJIB dilepas lengkap:
			 * clear() melepas entity dari persistence context, disconnect() mengembalikan
			 * koneksi fisik ke pool c3p0, lalu close(). Sebelumnya hanya close() -- pada
			 * jalur error koneksi bisa tertahan lebih lama dari yang diperlukan. */
			try { if (session.isOpen()) session.clear(); } catch (Exception clearError) {
				ErrorAuditUtil.record(clearError, "DatabasePerformanceSampler.clear");
			}
			try { if (session.isOpen()) session.disconnect(); } catch (Exception disconnectError) {
				ErrorAuditUtil.record(disconnectError, "DatabasePerformanceSampler.disconnect");
			}
			try { session.close(); } catch (Exception closeError) {
				ErrorAuditUtil.record(closeError, "DatabasePerformanceSampler.close");
			}
		}
	}

	private static String sha256(String value) throws Exception {
		byte[] bytes = MessageDigest.getInstance("SHA-256")
				.digest((value == null ? "" : value).getBytes(Charset.forName("UTF-8")));
		StringBuilder result = new StringBuilder();
		for (byte b : bytes) result.append(String.format("%02x", Integer.valueOf(b & 0xff)));
		return result.toString();
	}
}
