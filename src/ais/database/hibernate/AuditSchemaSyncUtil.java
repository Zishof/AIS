package ais.database.hibernate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.jdbc.Work;

/**
 * <h3>Sinkron otomatis kolom tabel audit Envers ({@code new_audit.*__audit}) saat startup.</h3>
 *
 * <p><b>Masalah yang ditutup.</b> {@code hbm2ddl.auto=update} menambah kolom baru ke tabel
 * UTAMA, tetapi TIDAK andal menambah kolom yang sama ke tabel audit di schema lain
 * ({@code new_audit}) -- gotcha terdokumentasi di hibernate.cfg.xml (korban historis:
 * {@code tbmrole.tampilkan_gaji}, {@code dasbor_repository}; terbaru:
 * {@code toko.alasan_tahan_json}, {@code toko.unit_usaha_json}). Akibatnya INSERT audit
 * gagal, flush rollback, dan data pengguna TIDAK tersimpan. Selama ini solusinya ALTER
 * manual per rilis (webapp/sql/migrasi_*_audit.sql); util ini menghapus keharusan itu:
 * seluruh selisih kolom basis-vs-audit dideteksi dari katalog PostgreSQL dan di-ALTER
 * otomatis SEKALI saat {@link HibernateUtil#getSessionFactory()} membangun factory.</p>
 *
 * <p><b>Cara kerja.</b> Satu query katalog mencari, untuk tiap {@code new_audit.<t>__audit},
 * kolom milik tabel basis {@code <t>} (schema koperasi/public/library/dst, prioritas
 * deterministik bila nama kembar) yang BELUM ada di tabel auditnya, lengkap dengan tipe
 * persis via {@code format_type}. Tiap kolom hilang ditambahkan dengan
 * {@code ALTER TABLE ... ADD COLUMN <kolom> <tipe>} tanpa NOT NULL/DEFAULT (baris audit
 * lama sah bernilai null). Idempoten: saat semua sinkron, query kosong dan tidak ada DDL.</p>
 *
 * <p><b>Batas yang disengaja.</b> (1) Fail-soft total: kegagalan apa pun hanya dicatat
 * (ErrorAuditUtil + stderr) dan TIDAK menggagalkan startup -- lebih baik jalan dengan
 * perilaku lama daripada mati. (2) Kolom {@code @NotAudited} ikut ditambahkan ke tabel
 * audit (selalu null, tidak dipakai Envers) -- harmless, dan jauh lebih aman daripada
 * memperkenalkan introspeksi mapping Envers 3.6 di sini. (3) Hanya PostgreSQL (katalog
 * {@code pg_catalog}); di RDBMS lain util ini diam saja lewat jalur fail-soft.</p>
 */
public final class AuditSchemaSyncUtil {

	private AuditSchemaSyncUtil() {
	}

	/** Skema audit Envers, sama dgn {@code org.hibernate.envers.default_schema} di cfg. */
	private static final String SKEMA_AUDIT = "new_audit";

	private static final String SQL_KOLOM_HILANG =
			"SELECT DISTINCT ON (au.relname, a.attname) "
			+ "  au.relname AS tabel_audit, a.attname AS kolom, "
			+ "  pg_catalog.format_type(a.atttypid, a.atttypmod) AS tipe "
			+ "FROM pg_catalog.pg_class au "
			+ "JOIN pg_catalog.pg_namespace au_ns ON au_ns.oid = au.relnamespace "
			+ " AND au_ns.nspname = '" + SKEMA_AUDIT + "' "
			+ "JOIN pg_catalog.pg_class b "
			+ "  ON b.relname = substring(au.relname, 1, length(au.relname) - 7) "
			+ " AND b.relkind = 'r' AND b.oid <> au.oid "
			+ "JOIN pg_catalog.pg_namespace b_ns ON b_ns.oid = b.relnamespace "
			+ " AND b_ns.nspname NOT IN ('" + SKEMA_AUDIT + "', 'pg_catalog', 'information_schema') "
			+ "JOIN pg_catalog.pg_attribute a "
			+ "  ON a.attrelid = b.oid AND a.attnum > 0 AND NOT a.attisdropped "
			+ "WHERE au.relkind = 'r' AND au.relname LIKE '%\\_\\_audit' "
			+ "  AND NOT EXISTS (SELECT 1 FROM pg_catalog.pg_attribute aa "
			+ "      WHERE aa.attrelid = au.oid AND aa.attname = a.attname "
			+ "        AND aa.attnum > 0 AND NOT aa.attisdropped) "
			// Nama tabel kembar antar-schema: pilih basis dgn prioritas deterministik.
			+ "ORDER BY au.relname, a.attname, "
			+ "  CASE b_ns.nspname WHEN 'koperasi' THEN 1 WHEN 'public' THEN 2 "
			+ "    WHEN 'library' THEN 3 ELSE 4 END";

	/**
	 * Jalankan sinkron sekali. Dipanggil {@link HibernateUtil#getSessionFactory()} tepat
	 * setelah factory dibangun; aman dipanggil ulang (idempoten, no-op saat sudah sinkron).
	 */
	public static void sinkronKolomAudit(SessionFactory factory) {
		Session session = null;
		try {
			session = factory.openSession();
			session.doWork(new Work() {
				@Override
				public void execute(Connection koneksi) throws java.sql.SQLException {
					List<String[]> hilang = new ArrayList<String[]>();
					Statement st = koneksi.createStatement();
					try {
						ResultSet rs = st.executeQuery(SQL_KOLOM_HILANG);
						while (rs.next()) {
							hilang.add(new String[] { rs.getString(1), rs.getString(2), rs.getString(3) });
						}
						rs.close();
					} finally {
						st.close();
					}
					for (String[] baris : hilang) {
						String ddl = "ALTER TABLE " + SKEMA_AUDIT + ".\"" + baris[0]
								+ "\" ADD COLUMN \"" + baris[1] + "\" " + baris[2];
						Statement alter = koneksi.createStatement();
						try {
							alter.executeUpdate(ddl);
							System.out.println("AuditSchemaSyncUtil: " + ddl);
						} catch (java.sql.SQLException e) {
							// Satu kolom gagal (mis. race dgn node lain) jangan menghentikan
							// sisanya -- kolom yang sudah ada akan lolos NOT EXISTS run depan.
							ais.common.ErrorAuditUtil.record(e, "AuditSchemaSyncUtil: " + ddl);
						} finally {
							alter.close();
						}
					}
					if (!hilang.isEmpty()) {
						System.out.println("AuditSchemaSyncUtil: " + hilang.size()
								+ " kolom audit disinkron dari tabel basis (new_audit).");
					}
				}
			});
		} catch (Throwable e) {
			// Fail-soft: startup TIDAK boleh gagal karena sinkron audit; tanpa util ini
			// sistem berjalan dgn perilaku lama (butuh ALTER manual), bukan lebih buruk.
			try {
				ais.common.ErrorAuditUtil.record(e, "AuditSchemaSyncUtil.sinkronKolomAudit");
			} catch (Throwable abaikan) {
				e.printStackTrace();
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}
}
