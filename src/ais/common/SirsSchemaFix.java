package ais.common;

import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.database.hibernate.HibernateUtil;

/**
 * Helper startup untuk kolom-kolom FONDASI DATA SIRS (Blueprint Integrasi SIRS — Fase 2)
 * yang ditambahkan ke entity SIRS ber-@Audited yang SUDAH ADA (Pasien, Asuransi).
 *
 * <p>Latar belakang teknis (sama seperti {@link KursusSchemaFix} dan
 * {@link DatabaseTextColumnSchemaFix}): {@code hbm2ddl.auto=update} menambah kolom baru
 * ke tabel utama secara otomatis, TAPI tidak selalu ke tabel audit-nya di schema
 * {@code new_audit} (lihat komentar di {@code hibernate.cfg.xml}). Bila kolom baru
 * ditambahkan ke entity @Audited namun tabel {@code new_audit.<tabel>__audit} ketinggalan
 * kolom, maka INSERT audit gagal → flush rollback → DATA TIDAK TERSIMPAN. Karena itu setiap
 * kolom dipastikan ada di KEDUA tabel: tabel utama di schema {@code sirs} dan tabel audit di
 * schema {@code new_audit}.</p>
 *
 * <p>Semua kolom di sini bersifat ADITIF & NULLABLE — tidak mengubah perilaku alur lama.
 * Tidak ada unique constraint yang dipasang di sini karena data legacy dapat mengandung
 * NIK/nomor kosong atau duplikat; keunikan (bila diperlukan) ditegakkan di lapisan aplikasi
 * atau lewat migrasi terpisah setelah data dibersihkan.</p>
 *
 * <p>Catatan skema: tabel utama SIRS memakai schema {@code sirs} (mis. {@code sirs.pasien}),
 * BUKAN {@code public}. Tabel audit tetap di {@code new_audit} karena Envers dikonfigurasi
 * {@code org.hibernate.envers.default_schema=new_audit} untuk semua entity @Audited.</p>
 *
 * <p>Kolom yang dijamin ada oleh helper ini:</p>
 * <ul>
 *   <li>{@code sirs.pasien.nik} — Nomor Induk Kependudukan (prasyarat MPI / KYC SATUSEHAT).</li>
 *   <li>{@code sirs.pasien.no_kartu_bpjs} — Nomor kartu peserta BPJS/JKN (prasyarat VClaim/PCare).</li>
 *   <li>{@code sirs.pasien.ihs_number} — IHS Number (identitas pasien di SATUSEHAT).</li>
 *   <li>{@code sirs.asuransi.jenis_payer} — Diskriminator tipe penjamin (UMUM/BPJS/ASURANSI_SWASTA/...).</li>
 *   <li>{@code sirs.asuransi.kode_payer} — Kode eksternal payer (mis. kode BPJS).</li>
 *   <li>{@code sirs.asuransi.nomor_pks} — Nomor Perjanjian Kerja Sama dengan payer.</li>
 *   <li>{@code sirs.asuransi.aktif} — Penanda payer masih aktif dipakai.</li>
 * </ul>
 *
 * <p>Kompatibel Java 1.6/1.7 dan Hibernate 3.6. Dipanggil dari
 * {@code ais.common.InitData.doInitData()} bersama schema-fix lain saat startup.</p>
 */
public final class SirsSchemaFix {

    private SirsSchemaFix() {
    }

    public static void initKolomBaru() {
        try {
			ensureAntreanFarmasiTable();
            // --- Pasien: identifier interoperabilitas (NIK / BPJS / SATUSEHAT) ---
            addColumnIfMissing("sirs", "pasien", "nik", "varchar(20)");
            addColumnIfMissing("new_audit", "pasien__audit", "nik", "varchar(20)");

            addColumnIfMissing("sirs", "pasien", "no_kartu_bpjs", "varchar(25)");
            addColumnIfMissing("new_audit", "pasien__audit", "no_kartu_bpjs", "varchar(25)");

            addColumnIfMissing("sirs", "pasien", "ihs_number", "varchar(30)");
            addColumnIfMissing("new_audit", "pasien__audit", "ihs_number", "varchar(30)");

            // --- Asuransi: pengayaan master payer (tetap master, BPJS jadi satu tipe) ---
            addColumnIfMissing("sirs", "asuransi", "jenis_payer", "varchar(30)");
            addColumnIfMissing("new_audit", "asuransi__audit", "jenis_payer", "varchar(30)");

            addColumnIfMissing("sirs", "asuransi", "kode_payer", "varchar(30)");
            addColumnIfMissing("new_audit", "asuransi__audit", "kode_payer", "varchar(30)");

            addColumnIfMissing("sirs", "asuransi", "nomor_pks", "varchar(50)");
            addColumnIfMissing("new_audit", "asuransi__audit", "nomor_pks", "varchar(50)");

            addColumnIfMissing("sirs", "asuransi", "aktif", "boolean");
            addColumnIfMissing("new_audit", "asuransi__audit", "aktif", "boolean");

            // --- Tbmuser: relasi opsional ke sirs.dokter (akun pengguna ber-role medis) ---
            // FIX SQLGrammarException "column this_.dokter does not exist": kolom ini
            // ditambahkan aditif ke entity Tbmuser (lihat Tbmuser.getDokter()/setDokter())
            // sebagai bagian Blueprint Integrasi SIRS, tapi belum ter-propagate ke semua
            // instalasi/skema DB (hbm2ddl.auto=update tidak selalu jalan di semua deployment).
            addColumnIfMissing("public", "tbmuser", "dokter", "bigint");
            addColumnIfMissing("new_audit", "tbmuser__audit", "dokter", "bigint");
        } catch (Exception e) {
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ignored) {
                ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/SirsSchemaFix.java:initKolomBaru");
            }
        }
    }

	/**
	 * Tabel baru untuk layar kedua farmasi. Deployment AIS memakai hbm2ddl=none
	 * pada sejumlah instalasi, sehingga DDL idempoten ini merupakan bagian wajib
	 * dari startup dan tidak bergantung pada perubahan manual basis data.
	 */
	private static void ensureAntreanFarmasiTable() {
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			session.createSQLQuery("create table if not exists sirs.antrean_farmasi ("
					+ "id bigserial primary key, toko_id bigint not null, resep_id bigint, "
					+ "kode_antrean varchar(30) not null, nomor_rekam_medis varchar(80), "
					+ "nama_pasien varchar(160) not null, jenis varchar(20) not null, "
					+ "status varchar(20) not null, loket varchar(40), daftar_obat text, "
					+ "catatan_publik varchar(240), urutan integer, tanggal_dibuat timestamp not null, "
					+ "tanggal_dirubah timestamp, oleh varchar(255), oleh_id varchar(255))")
					.executeUpdate();
			session.createSQLQuery("create index if not exists antrean_farmasi_toko_tanggal_idx "
					+ "on sirs.antrean_farmasi (toko_id, tanggal_dibuat, urutan)").executeUpdate();
			tx.commit();
			log("Tabel sirs.antrean_farmasi siap.");
		} catch (Exception e) {
			rollbackQuietly(tx);
			log("Gagal memastikan tabel sirs.antrean_farmasi: " + e.getMessage());
		} finally {
			closeQuietly(session);
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
            ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/SirsSchemaFix.java:rollback");
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
            ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/SirsSchemaFix.java:closeA");
        }
        try {
            session.disconnect();
        } catch (Exception ignored) {
            ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/SirsSchemaFix.java:closeB");
        }
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (Exception ignored) {
            ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/SirsSchemaFix.java:closeC");
        }
    }

    private static void log(String message) {
        try {
            System.out.println("SirsSchemaFix: " + message);
        } catch (Exception ignored) {
            ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/SirsSchemaFix.java:log");
        }
    }
}
