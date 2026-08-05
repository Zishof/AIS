package ais.common;

import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.database.hibernate.HibernateUtil;

/**
 * Helper startup untuk kolom {@code jenis_seleksi_target} pada
 * {@link ais.database.model.KelompokCalonMahasiswa} yang belum ter-propagate ke semua
 * instalasi/skema DB (hbm2ddl.auto=update tidak selalu jalan di semua deployment -- lihat pola
 * sama di {@link SirsSchemaFix}/{@link KursusSchemaFix}/{@link DiskonMahasiswaSchemaFix}).
 *
 * <p>Tanpa kolom ini, memuat/menyentuh entity {@code KelompokCalonMahasiswa} (langsung maupun
 * lewat proxy lazy {@code BiodataCalonMahasiswa.getStatusAwalMahasiswa()}/
 * {@code getKelompokCalonMahasiswa()}) gagal dengan
 * {@code SQLGrammarException/GenericJDBCException: column ... jenis_seleksi_target does not
 * exist}, membatalkan preload cache saat startup maupun proses simpan/tampil yang menyentuh
 * entity ini (checkKegiatanCalonMahasiswa, login PMB, cetak laporan, dsb).</p>
 *
 * <p>Kolom di sini ADITIF &amp; NULLABLE -- tidak mengubah perilaku alur lama. Kompatibel
 * Java 1.6/1.7 dan Hibernate 3.6. Dipanggil dari {@code ais.common.InitData.doInitData()}
 * bersama schema-fix lain saat startup.</p>
 */
public final class KelompokCalonMahasiswaSchemaFix {

    private KelompokCalonMahasiswaSchemaFix() {
    }

    public static void initKolomBaru() {
        try {
            addColumnIfMissing("public", "kelompok_calon_mahasiswa", "jenis_seleksi_target", "bigint");
            addColumnIfMissing("new_audit", "kelompok_calon_mahasiswa__audit", "jenis_seleksi_target", "bigint");
        } catch (Exception e) {
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ignored) {
                ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/KelompokCalonMahasiswaSchemaFix.java:initKolomBaru");
            }
        }
    }

    /** Tambah kolom bila belum ada. Aman untuk PostgreSQL lama (tanpa IF NOT EXISTS). */
    private static void addColumnIfMissing(String schema, String table, String column, String sqlType) {
        if (!isSafeIdentifier(schema) || !isSafeIdentifier(table) || !isSafeIdentifier(column)) {
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
            log("Berhasil tambah kolom: " + schema + "." + table + "." + column + " (" + sqlType + ")");
        } catch (Exception e) {
            rollbackQuietly(tx);
            log("Gagal tambah kolom " + schema + "." + table + "." + column + " : " + e.getMessage());
        } finally {
            closeQuietly(session);
        }
    }

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

    private static void rollbackQuietly(Transaction tx) {
        if (tx == null) {
            return;
        }
        try {
            if (tx.isActive()) {
                tx.rollback();
            }
        } catch (Exception ignored) {
            ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/KelompokCalonMahasiswaSchemaFix.java:rollback");
        }
    }

    private static void closeQuietly(Session session) {
        if (session == null) {
            return;
        }
        try {
            session.disconnect();
        } catch (Exception ignored) {
            ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/KelompokCalonMahasiswaSchemaFix.java:disconnect");
        }
        try {
            session.close();
        } catch (Exception ignored) {
            ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/KelompokCalonMahasiswaSchemaFix.java:close");
        }
    }

    private static void log(String msg) {
        System.out.println("KelompokCalonMahasiswaSchemaFix: " + msg);
    }
}
