package ais.common;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.database.hibernate.HibernateUtil;

/**
 * Migrasi skema retail yang kecil, berurutan, dan tercatat. Kelas ini menjadi
 * jembatan untuk aplikasi legacy yang belum memakai Maven/Flyway: setiap
 * perubahan mempunyai versi immutable, checksum, advisory lock PostgreSQL, dan
 * riwayat eksekusi. Setelah build AIS dimodernisasi, isi migrasi ini dapat
 * dipindah 1:1 ke Flyway tanpa kehilangan urutan atau audit.
 */
public final class RetailDatabaseMigrations {

	private static final long ADVISORY_LOCK = 7348219061401L;

	private RetailDatabaseMigrations() {
	}

	private static final class Migration {
		final String version;
		final String description;
		final String sql;

		Migration(String version, String description, String sql) {
			this.version = version;
			this.description = description;
			this.sql = sql;
		}
	}

	private static List<Migration> daftar() {
		List<Migration> result = new ArrayList<Migration>();
		result.add(new Migration("20260814.001", "retail request idempotency",
				"CREATE TABLE IF NOT EXISTS public.retail_request_idempotency ("
						+ "id BIGSERIAL PRIMARY KEY, action VARCHAR(80) NOT NULL, idempotency_key VARCHAR(160) NOT NULL, "
						+ "request_hash VARCHAR(64), status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING', "
						+ "result_reference VARCHAR(160), response_json TEXT, created_at TIMESTAMP NOT NULL DEFAULT NOW(), "
						+ "updated_at TIMESTAMP NOT NULL DEFAULT NOW(), UNIQUE(action,idempotency_key));"
						+ "CREATE INDEX IF NOT EXISTS idx_retail_idempotency_status_updated "
						+ "ON public.retail_request_idempotency(status,updated_at);"));
		result.add(new Migration("20260814.002", "inventory movement ledger",
				"CREATE TABLE IF NOT EXISTS koperasi.inventory_movement ("
						+ "id BIGSERIAL PRIMARY KEY, reference_type VARCHAR(40) NOT NULL, reference_id VARCHAR(120) NOT NULL, "
						+ "idempotency_key VARCHAR(160), produk BIGINT NOT NULL, produk_batch BIGINT, toko_asal BIGINT, toko_tujuan BIGINT, "
						+ "movement_type VARCHAR(40) NOT NULL, quantity NUMERIC(20,6) NOT NULL, unit_cost NUMERIC(20,4) NOT NULL DEFAULT 0, "
						+ "movement_value NUMERIC(22,4) NOT NULL DEFAULT 0, stock_before NUMERIC(20,6), stock_after NUMERIC(20,6), "
						+ "occurred_at TIMESTAMP NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW(), created_by VARCHAR(255), "
						+ "device_id VARCHAR(255), reason TEXT, reversal_of BIGINT, status VARCHAR(20) NOT NULL DEFAULT 'POSTED');"
						+ "CREATE UNIQUE INDEX IF NOT EXISTS uq_inventory_movement_reference "
						+ "ON koperasi.inventory_movement(reference_type,reference_id,produk,movement_type,COALESCE(produk_batch,0));"
						+ "CREATE INDEX IF NOT EXISTS idx_inventory_movement_produk_waktu "
						+ "ON koperasi.inventory_movement(produk,occurred_at DESC,id DESC);"
						+ "CREATE INDEX IF NOT EXISTS idx_inventory_movement_toko_waktu "
						+ "ON koperasi.inventory_movement(COALESCE(toko_tujuan,toko_asal),occurred_at DESC,id DESC);"));
		result.add(new Migration("20260814.003", "database performance samples",
				"CREATE TABLE IF NOT EXISTS public.database_performance_sample ("
						+ "id BIGSERIAL PRIMARY KEY, captured_at TIMESTAMP NOT NULL DEFAULT NOW(), source VARCHAR(40) NOT NULL, "
						+ "query_fingerprint VARCHAR(64), duration_ms BIGINT, calls BIGINT, rows_count BIGINT, detail TEXT);"
						+ "CREATE INDEX IF NOT EXISTS idx_db_performance_captured "
						+ "ON public.database_performance_sample(captured_at DESC,id DESC);"));
		return result;
	}

	public static void migrate() throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		Connection connection = session.connection();
		boolean oldAutoCommit = connection.getAutoCommit();
		try {
			connection.setAutoCommit(false);
			Statement bootstrap = connection.createStatement();
			bootstrap.execute("CREATE TABLE IF NOT EXISTS public.ais_schema_history ("
					+ "version VARCHAR(40) PRIMARY KEY, description VARCHAR(255) NOT NULL, checksum VARCHAR(64) NOT NULL, "
					+ "installed_at TIMESTAMP NOT NULL DEFAULT NOW(), execution_ms BIGINT NOT NULL, success BOOLEAN NOT NULL)");
			bootstrap.execute("SELECT pg_advisory_xact_lock(" + ADVISORY_LOCK + ")");
			bootstrap.close();

			for (Migration migration : daftar()) {
				String checksum = sha256(migration.sql);
				PreparedStatement check = connection.prepareStatement(
						"SELECT checksum,success FROM public.ais_schema_history WHERE version=?");
				check.setString(1, migration.version);
				ResultSet rs = check.executeQuery();
				if (rs.next()) {
					String existingChecksum = rs.getString(1);
					boolean success = rs.getBoolean(2);
					rs.close();
					check.close();
					if (!success || !checksum.equals(existingChecksum)) {
						throw new IllegalStateException("Migrasi " + migration.version
								+ " pernah dijalankan tetapi checksum/status tidak sesuai. Jangan mengubah migrasi lama; buat versi baru.");
					}
					continue;
				}
				rs.close();
				check.close();

				long mulai = System.currentTimeMillis();
				// Satu migration dapat berisi beberapa DDL. Eksekusi per pernyataan
				// supaya tetap kompatibel dengan konfigurasi JDBC yang menolak multi-query.
				for (String sql : migration.sql.split(";")) {
					if (sql == null || sql.trim().isEmpty()) continue;
					Statement statement = connection.createStatement();
					try {
						statement.execute(sql.trim());
					} finally {
						statement.close();
					}
				}
				PreparedStatement insert = connection.prepareStatement(
						"INSERT INTO public.ais_schema_history(version,description,checksum,execution_ms,success) VALUES (?,?,?,?,true)");
				insert.setString(1, migration.version);
				insert.setString(2, migration.description);
				insert.setString(3, checksum);
				insert.setLong(4, System.currentTimeMillis() - mulai);
				insert.executeUpdate();
				insert.close();
			}
			connection.commit();
		} catch (Exception e) {
			try {
				connection.rollback();
			} catch (Exception rollbackError) {
				ErrorAuditUtil.record(rollbackError, "RetailDatabaseMigrations.rollback");
			}
			throw e;
		} finally {
			try {
				connection.setAutoCommit(oldAutoCommit);
			} catch (Exception ignored) {
				ErrorAuditUtil.record(ignored, "RetailDatabaseMigrations.restoreAutoCommit");
			}
			try {
				session.close();
			} catch (Exception ignored) {
				ErrorAuditUtil.record(ignored, "RetailDatabaseMigrations.close");
			}
		}
	}

	private static String sha256(String value) throws Exception {
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(Charset.forName("UTF-8")));
		StringBuilder result = new StringBuilder();
		for (byte b : digest) {
			result.append(String.format("%02x", Integer.valueOf(b & 0xff)));
		}
		return result.toString();
	}
}
