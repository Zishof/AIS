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
					"SELECT CAST(round(extract(epoch from (clock_timestamp()-query_start))*1000) AS bigint), "
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
			ambilSampelPool(session);
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

	/**
	 * Rekam metrik CONNECTION POOL untuk SELURUH pool c3p0 dalam JVM ini sekaligus
	 * (utama, streaming, OJS, Radius, Openfire) — OPTIMASI FASE 4.
	 *
	 * <p><b>Kenapa perlu.</b> Ukuran pool sudah diturunkan drastis pada Fase 4 (mis. OJS/Radius/
	 * Openfire dari 2000 menjadi 5). Tanpa metrik, tidak ada cara membuktikan apakah angka baru
	 * itu cukup atau justru membuat request ANTRE menunggu koneksi. Yang paling menentukan adalah
	 * {@code numThreadsAwaitingCheckout}: bila konsisten &gt; 0, pool kekecilan; bila selalu 0
	 * sementara {@code numBusy} jauh di bawah maksimum, pool masih bisa diperkecil.</p>
	 *
	 * <p><b>Pemetaan kolom</b> pada {@code database_performance_sample} (memakai kolom yang sudah
	 * ada, TANPA migrasi schema baru): {@code source='POOL'}, {@code query_fingerprint}=nama pool,
	 * {@code duration_ms}=jumlah thread MENUNGGU koneksi, {@code calls}=koneksi terpakai,
	 * {@code rows_count}=total koneksi, {@code detail}=ringkasan terbaca. Nilai {@code source}
	 * sengaja dibedakan supaya analisis query lambat ({@code ACTIVE_QUERY}) tidak tercampur.</p>
	 *
	 * <p>Diakses lewat REFLEKSI agar kelas ini tetap jalan bila provider pool berganti/c3p0 tidak
	 * ada — seluruh kegagalan ditelan karena ini murni diagnostik dan TIDAK boleh menggagalkan
	 * sampling utama.</p>
	 */
	private static void ambilSampelPool(Session session) {
		PreparedStatement insert = null;
		try {
			Class<?> registry = Class.forName("com.mchange.v2.c3p0.C3P0Registry");
			Object hasil = registry.getMethod("getPooledDataSources", new Class[0]).invoke(null, new Object[0]);
			if (!(hasil instanceof java.util.Set)) {
				return;
			}
			java.util.Set<?> pools = (java.util.Set<?>) hasil;
			if (pools.isEmpty()) {
				return;
			}
			insert = session.connection().prepareStatement(
					"INSERT INTO public.database_performance_sample(source,query_fingerprint,duration_ms,calls,rows_count,detail) "
							+ "VALUES ('POOL',?,?,?,?,?)");
			int jumlahBaris = 0;
			for (java.util.Iterator<?> it = pools.iterator(); it.hasNext();) {
				Object pool = it.next();
				if (pool == null) {
					continue;
				}
				String nama = namaPool(pool);
				int menunggu = angkaPool(pool, "getNumThreadsAwaitingCheckoutDefaultUser");
				int terpakai = angkaPool(pool, "getNumBusyConnectionsDefaultUser");
				int nganggur = angkaPool(pool, "getNumIdleConnectionsDefaultUser");
				int total = angkaPool(pool, "getNumConnectionsDefaultUser");
				if (menunggu < 0 && terpakai < 0 && nganggur < 0 && total < 0) {
					continue; // pool tidak melaporkan metrik apa pun
				}
				insert.setString(1, nama);
				insert.setLong(2, menunggu < 0 ? 0L : menunggu);
				insert.setLong(3, terpakai < 0 ? 0L : terpakai);
				insert.setLong(4, total < 0 ? 0L : total);
				insert.setString(5, "pool=" + nama + " menunggu=" + menunggu + " terpakai=" + terpakai
						+ " nganggur=" + nganggur + " total=" + total);
				insert.addBatch();
				jumlahBaris++;
			}
			if (jumlahBaris > 0) {
				insert.executeBatch();
			}
		} catch (Throwable t) {
			ErrorAuditUtil.record(t, "DatabasePerformanceSampler.ambilSampelPool");
		} finally {
			if (insert != null) {
				try {
					insert.close();
				} catch (Throwable closeError) {
					ErrorAuditUtil.record(closeError, "DatabasePerformanceSampler.ambilSampelPool.close");
				}
			}
		}
	}

	/** Nama pool untuk pengelompokan; dipotong agar muat kolom VARCHAR(64). */
	private static String namaPool(Object pool) {
		String nama = null;
		try {
			Object hasil = pool.getClass().getMethod("getDataSourceName", new Class[0]).invoke(pool, new Object[0]);
			nama = hasil == null ? null : hasil.toString();
		} catch (Throwable abaikan) {
			nama = null;
		}
		if (nama == null || nama.trim().length() == 0) {
			nama = pool.getClass().getSimpleName() + "@" + System.identityHashCode(pool);
		}
		return nama.length() > 64 ? nama.substring(0, 64) : nama;
	}

	/** Baca satu metrik int dari pool via refleksi; -1 bila tidak tersedia. */
	private static int angkaPool(Object pool, String namaMethod) {
		try {
			Object hasil = pool.getClass().getMethod(namaMethod, new Class[0]).invoke(pool, new Object[0]);
			return hasil instanceof Number ? ((Number) hasil).intValue() : -1;
		} catch (Throwable abaikan) {
			return -1;
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
