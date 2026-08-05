package ais.common;

import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.database.hibernate.HibernateUtil;

/**
 * <h1>AturanCutiSchemaFix — penyelaras tabel AUDIT untuk kolom Aturan Cuti</h1>
 *
 * <p><b>Pembuatan kolom pada tabel utama diserahkan sepenuhnya ke Hibernate</b>
 * ({@code hbm2ddl.auto=update}). Kelas ini <b>TIDAK</b> menyentuh tabel di schema {@code public}
 * maupun {@code payroll}. Yang ditangani di sini hanya satu hal yang memang TIDAK dikerjakan
 * Hibernate di instalasi ini: menambahkan kolom yang sama ke <b>tabel audit Envers</b>.</p>
 *
 * <p><b>Kenapa masih perlu.</b> Lihat catatan pada {@code hibernate.cfg.xml}: mode {@code update}
 * menambah kolom baru ke tabel utama, <i>tetapi tidak selalu</i> ke tabel audit di schema
 * {@code new_audit}. Bila {@code new_audit.<tabel>__audit} ketinggalan satu kolom, maka setiap
 * penyimpanan entity ber-{@code @Audited} tersebut GAGAL saat INSERT audit → flush rollback →
 * <b>data tidak tersimpan tanpa pesan yang jelas</b> (persis insiden {@code Tbmrole.tampilkan_gaji}).</p>
 *
 * <p>Dua entity yang dipakai fitur Aturan Cuti — {@code Statusabsensi} dan
 * {@code payroll.LiburNasional} — keduanya {@code @Audited}, sehingga tabel auditnya perlu
 * diselaraskan:</p>
 * <ul>
 *   <li>{@code new_audit.statusabsensi__audit.durasi_baku_hari} (integer)</li>
 *   <li>{@code new_audit.libur_nasional__audit.libur_panjang} (boolean)</li>
 * </ul>
 *
 * <p>Aman dijalankan berulang (dicek dulu ke {@code information_schema}) dan tidak pernah
 * menghapus apa pun. Bila kelak Hibernate/Envers sudah menyelaraskan tabel audit sendiri di
 * instalasi ini, kelas ini menjadi no-op dan boleh dihapus. Alternatif manual sekali jalan:
 * DROP tabel {@code *__audit} tersebut agar dibuat ulang lengkap saat startup.</p>
 *
 * <p>Kompatibel Java 1.6/1.7 dan Hibernate 3.6. Dipanggil dari {@code ais.common.InitData}.</p>
 */
public final class AturanCutiSchemaFix {

	private AturanCutiSchemaFix() {
	}

	/**
	 * Selaraskan HANYA tabel audit Envers. Kolom pada tabel utama sengaja tidak disentuh —
	 * itu tugas Hibernate {@code hbm2ddl.auto=update}.
	 */
	public static void initKolomAudit() {
		try {
			addColumnIfMissing("new_audit", "statusabsensi__audit", "durasi_baku_hari", "integer");
			addColumnIfMissing("new_audit", "libur_nasional__audit", "libur_panjang", "boolean");
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) {
				ais.common.ErrorAuditUtil.record(ignored,
						"auto-audit(empty-catch) src/ais/common/AturanCutiSchemaFix.java:initKolomAudit");
			}
		}
	}

	/** Tambah kolom bila belum ada. Aman untuk PostgreSQL lama (tanpa IF NOT EXISTS). */
	private static void addColumnIfMissing(String schema, String table, String column, String sqlType) {
		if (!isSafeIdentifier(schema) || !isSafeIdentifier(table) || !isSafeIdentifier(column)) {
			return;
		}
		if (!tableExists(schema, table)) {
			// Tabel audit belum ada sama sekali — biarkan Envers yang membuatnya.
			return;
		}
		if (columnExists(schema, table, column)) {
			return;
		}
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			String sql = "ALTER TABLE " + quote(schema) + "." + quote(table) + " ADD COLUMN " + quote(column) + " "
					+ sqlType;
			session.createSQLQuery(sql).executeUpdate();
			tx.commit();
			log("Selaras kolom audit: " + schema + "." + table + "." + column + " (" + sqlType + ")");
		} catch (Exception e) {
			rollbackQuietly(tx);
			log("Gagal selaraskan kolom audit " + schema + "." + table + "." + column + " : " + e.getMessage());
		} finally {
			closeQuietly(session);
		}
	}

	@SuppressWarnings("rawtypes")
	private static boolean tableExists(String schema, String table) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			java.util.List result = session
					.createSQLQuery("select table_name from information_schema.tables "
							+ "where table_schema = :schemaName and table_name = :tableName")
					.setParameter("schemaName", schema).setParameter("tableName", table).list();
			return result != null && !result.isEmpty();
		} catch (Exception e) {
			log("Gagal cek tabel " + schema + "." + table + " : " + e.getMessage());
			return false;
		} finally {
			closeQuietly(session);
		}
	}

	@SuppressWarnings("rawtypes")
	private static boolean columnExists(String schema, String table, String column) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			java.util.List result = session.createSQLQuery("select column_name from information_schema.columns "
					+ "where table_schema = :schemaName and table_name = :tableName and column_name = :columnName")
					.setParameter("schemaName", schema).setParameter("tableName", table)
					.setParameter("columnName", column).list();
			return result != null && !result.isEmpty();
		} catch (Exception e) {
			log("Gagal cek kolom " + schema + "." + table + "." + column + " : " + e.getMessage());
			return true; // aman: anggap sudah ada supaya tidak mencoba ALTER berulang saat gagal cek
		} finally {
			closeQuietly(session);
		}
	}

	private static boolean isSafeIdentifier(String value) {
		if (value == null || value.trim().length() == 0) {
			return false;
		}
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
			if (!ok) {
				return false;
			}
		}
		return true;
	}

	private static String quote(String value) {
		return "\"" + value + "\"";
	}

	private static void log(String pesan) {
		try {
			System.out.println("[AturanCutiSchemaFix] " + pesan);
		} catch (Exception ignored) {
			ais.common.ErrorAuditUtil.record(ignored,
					"auto-audit(empty-catch) src/ais/common/AturanCutiSchemaFix.java:log");
		}
	}

	private static void rollbackQuietly(Transaction tx) {
		if (tx == null) {
			return;
		}
		try {
			if (tx.isActive()) {
				tx.rollback();
			}
		} catch (Exception ignored) {
			ais.common.ErrorAuditUtil.record(ignored,
					"auto-audit(empty-catch) src/ais/common/AturanCutiSchemaFix.java:rollback");
		}
	}

	private static void closeQuietly(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.disconnect();
		} catch (Exception ignored) {
			ais.common.ErrorAuditUtil.record(ignored,
					"auto-audit(empty-catch) src/ais/common/AturanCutiSchemaFix.java:disconnect");
		}
		try {
			session.close();
		} catch (Exception ignored) {
			ais.common.ErrorAuditUtil.record(ignored,
					"auto-audit(empty-catch) src/ais/common/AturanCutiSchemaFix.java:close");
		}
	}
}
