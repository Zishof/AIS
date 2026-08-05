package ais.common;

import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.database.hibernate.HibernateUtil;

/**
 * Helper startup untuk kolom baru yang ditambahkan ke entity kursus yang sudah ber-@Audited
 * (ProdukKursus, PesertaPunyaProdukKursus). hbm2ddl=update menambah kolom ke tabel utama di
 * "public" secara otomatis, TAPI tidak ke tabel audit-nya di schema "new_audit" (lihat komentar
 * di hibernate.cfg.xml dan pola yang sama di DatabaseTextColumnSchemaFix). Di sini kolom
 * dipastikan ada di KEDUA tabel supaya INSERT audit tidak gagal.
 *
 * Kompatibel Java 1.6/1.7 dan Hibernate 3.6.
 */
public final class KursusSchemaFix {

    private KursusSchemaFix() {
    }

    public static void initKolomBaru() {
        try {
            addColumnIfMissing("public", "produk_kursus", "instruktur", "bigint");
            addColumnIfMissing("new_audit", "produk_kursus__audit", "instruktur", "bigint");

            addColumnIfMissing("public", "produk_kursus", "status", "varchar(50)");
            addColumnIfMissing("new_audit", "produk_kursus__audit", "status", "varchar(50)");

            addColumnIfMissing("public", "produk_kursus", "gratis", "boolean");
            addColumnIfMissing("new_audit", "produk_kursus__audit", "gratis", "boolean");

            addColumnIfMissing("public", "peserta_punya_produk_kursus", "hargadibayar", "double precision");
            addColumnIfMissing("new_audit", "peserta_punya_produk_kursus__audit", "hargadibayar", "double precision");

            addColumnIfMissing("public", "peserta_punya_produk_kursus", "kupon_kursus", "bigint");
            addColumnIfMissing("new_audit", "peserta_punya_produk_kursus__audit", "kupon_kursus", "bigint");
        } catch (Exception e) {
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ignored) {
                ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/KursusSchemaFix.java:29");
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
            ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/KursusSchemaFix.java:rollback");
        }
    }

    private static void closeQuietly(Session session) {
        if (session == null) {
            return;
        }
        try {
            if (session.isOpen()) {
                session.clear();
            }
        } catch (Exception ignored) {
            ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/KursusSchemaFix.java:closeA");
        }
        try {
            session.disconnect();
        } catch (Exception ignored) {
            ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/KursusSchemaFix.java:closeB");
        }
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (Exception ignored) {
            ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/KursusSchemaFix.java:closeC");
        }
    }

    private static void log(String message) {
        try {
            System.out.println("KursusSchemaFix: " + message);
        } catch (Exception ignored) {
            ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/KursusSchemaFix.java:log");
        }
    }
}
