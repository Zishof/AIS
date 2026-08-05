package ais.common;

import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.database.hibernate.HibernateUtil;

/**
 * Helper startup untuk kolom {@code jenis_diskon_mahasiswa} yang direferensikan oleh 4 entity
 * ({@link ais.database.model.DiskonMahasiswa}, {@link ais.database.model.GelombangPendaftaran},
 * {@link ais.database.model.JenisSeleksi}, {@link ais.database.model.KelompokMahasiswa}) tapi
 * belum ter-propagate ke semua instalasi/skema DB (hbm2ddl.auto=update tidak selalu jalan di
 * semua deployment -- lihat pola sama di {@link SirsSchemaFix}/{@link KursusSchemaFix}).
 *
 * <p>Tanpa kolom ini, load massal entity saat startup ({@code InitDataHelper.doInitData})
 * gagal dengan {@code SQLGrammarException: column ... jenis_diskon_mahasiswa does not exist},
 * membatalkan preload cache utk entity terkait.</p>
 *
 * <p>Semua kolom di sini ADITIF & NULLABLE -- tidak mengubah perilaku alur lama. Kompatibel
 * Java 1.6/1.7 dan Hibernate 3.6. Dipanggil dari {@code ais.common.InitData.doInitData()}
 * bersama schema-fix lain saat startup.</p>
 */
public final class DiskonMahasiswaSchemaFix {

    private DiskonMahasiswaSchemaFix() {
    }

    public static void initKolomBaru() {
        try {
            addColumnIfMissing("public", "diskon_mahasiswa", "jenis_diskon_mahasiswa", "bigint");
            addColumnIfMissing("new_audit", "diskon_mahasiswa__audit", "jenis_diskon_mahasiswa", "bigint");

            addColumnIfMissing("public", "gelombang_pendaftaran", "jenis_diskon_mahasiswa", "bigint");
            addColumnIfMissing("new_audit", "gelombang_pendaftaran__audit", "jenis_diskon_mahasiswa", "bigint");

            addColumnIfMissing("public", "jenis_seleksi", "jenis_diskon_mahasiswa", "bigint");
            addColumnIfMissing("new_audit", "jenis_seleksi__audit", "jenis_diskon_mahasiswa", "bigint");

            addColumnIfMissing("public", "kelompok_mahasiswa", "jenis_diskon_mahasiswa", "bigint");
            addColumnIfMissing("new_audit", "kelompok_mahasiswa__audit", "jenis_diskon_mahasiswa", "bigint");
        } catch (Exception e) {
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ignored) {
                ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DiskonMahasiswaSchemaFix.java:initKolomBaru");
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
            ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DiskonMahasiswaSchemaFix.java:rollback");
        }
    }

    private static void closeQuietly(Session session) {
        if (session == null) {
            return;
        }
        try {
            session.disconnect();
        } catch (Exception ignored) {
            ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DiskonMahasiswaSchemaFix.java:disconnect");
        }
        try {
            session.close();
        } catch (Exception ignored) {
            ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DiskonMahasiswaSchemaFix.java:close");
        }
    }

    private static void log(String msg) {
        System.out.println("DiskonMahasiswaSchemaFix: " + msg);
    }
}
