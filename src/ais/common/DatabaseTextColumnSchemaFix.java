package ais.common;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.database.hibernate.HibernateUtil;

/**
 * Helper startup untuk memastikan kolom-kolom yang berisi teks panjang sudah bertipe text.
 *
 * Pola kerja:
 * - Cek information_schema terlebih dahulu.
 * - Jika kolom belum text, baru jalankan ALTER COLUMN TYPE text.
 * - Jika table/kolom belum ada, proses dilewati agar kompatibel dengan database lama.
 * - Setiap ALTER memakai transaction sendiri agar kegagalan satu kolom tidak membuat semua proses aborted.
 *
 * Kompatibel Java 1.6/1.7 dan Hibernate 3.6.
 */
public final class DatabaseTextColumnSchemaFix {

    private DatabaseTextColumnSchemaFix() {
    }

    public static void initTextColumns() {
        try {
            initVirtualAccountBank();
            initKonfigurasi();
            initLabelBahasaMandarin();
            initLogHostToHost();
            initTugasPertemuan();
			initSettingBiaya();
            initTransaksiKeterangan();
			initPengajuanMahasiswa();
			initRealisasiKerjaPegawai();
        } catch (Exception e) {
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DatabaseTextColumnSchemaFix.java:35");
            }
        }
    }

    /**
     * Kolom {@code nilai_manual_json} dan {@code sub_cpmk_per_peserta} pada {@code tugas_pertemuan}.
     * Tabel utama ditangani Hibernate (hbm2ddl=update). Tabel audit di {@code new_audit} harus
     * ditambahkan di sini karena Envers tidak selalu memperbarui schema audit secara otomatis.
     */
    public static void initTugasPertemuan() {
        addColumnTextIfMissing("new_audit", "tugas_pertemuan__audit", "nilai_manual_json");
        addColumnTextIfMissing("new_audit", "tugas_pertemuan__audit", "sub_cpmk_per_peserta");
    }

    public static void initVirtualAccountBank() {
        alterColumnsToText("public", "virtual_account_bank", new String[] { "keterangan", "cicilan",
                "detailbiaya", "bulanan", "request", "response", "link", "notif", "barcode" });
    }

    /**
     * Kolom {@code oleh}/{@code olehid} pada {@code log_host_to_host} diisi OTOMATIS oleh
     * {@link ais.database.hibernate.AuditTimestampInterceptor} untuk SEMUA entitas (bukan cuma
     * tabel ini) dengan string gabungan jejak pemanggil (mis. "external_update;;Kelas:123;...")
     * yang panjangnya tak terbatas -- sebelumnya kolom ini masih varchar(255) default JPA
     * (tidak diberi {@code columnDefinition="text"} seperti kolom string lain di entitas yang
     * sama), sehingga INSERT gagal "value too long for type character varying(255)" persis
     * seperti yang pernah terjadi & diperbaiki untuk virtual_account_bank di atas. Kolom
     * {@code ip} juga ikut dilebarkan untuk konsistensi (sebelumnya sudah dipotong aman di Java
     * ke 255 karakter, tapi info IP/proxy-chain lengkap jadi hilang -- lihat
     * PembayaranGatewayHelper.potongAmanUntukKolomLog).
     *
     * <p><b>Kenapa kolom lain (request/response/info0-18/dst.) ikut dicek di sini padahal
     * entitas Java-nya SUDAH {@code columnDefinition="text"}:</b> anotasi Java hanya menentukan
     * tipe kolom saat Hibernate MEMBUAT kolom itu (hbm2ddl.auto=update TIDAK PERNAH meng-ALTER
     * tipe kolom yang SUDAH ADA). Tabel ini dibuat 2010 (komentar hbm2java) -- bila kolom
     * tersebut awalnya dibuat sebelum anotasi text ditambahkan, kolom FISIK di database bisa
     * saja masih {@code varchar(255)} lama walau Java sudah bilang text. Semua kolom string di
     * entitas ini disertakan di sini sebagai jaring pengaman menyeluruh; cek lewat
     * {@code alterColumnToTextIfNeeded} tetap idempoten & aman (no-op bila sudah text).
     */
    public static void initLogHostToHost() {
        alterColumnsToText("public", "log_host_to_host",
                new String[] { "oleh", "olehid", "ip", "nama", "keterangan", "response_description", "request",
                        "response", "item", "stack_trace", "info0", "info1", "info2", "info3", "info4", "info5",
                        "info6", "info7", "info8", "info9", "info10", "info11", "info12", "info13", "info14",
                        "info15", "info16", "info17", "info18" });
    }

    /**
     * Kolom {@code keterangan} pada {@code akunting.transaksi}. Tabel utama sudah dipastikan
     * text secara lazy per-request oleh {@code TransaksiJurnalUmumHelper.pastikanKolomTransaksiAkuntingAman}
     * (ALTER dijalankan sekali lalu hasil "sudah aman"-nya dipakai untuk melewati potongTextDb),
     * TAPI tabel audit {@code new_audit.transaksi__audit} milik Envers TIDAK ikut dilebarkan oleh
     * ALTER tabel utama tsb -- masih varchar(255) lama -- sehingga INSERT audit untuk transaksi
     * dengan keterangan panjang (setelah tabel utama sudah text & potongTextDb dilewati) gagal
     * "value too long for varchar(255)". Lebarkan KEDUA tabel di sini agar konsisten.
     */
    public static void initTransaksiKeterangan() {
        String[] columns = new String[] { "keterangan" };
        alterColumnsToText("akunting", "transaksi", columns);
        alterColumnsToText("new_audit", "transaksi__audit", columns);
    }

	/** Kolom daftar NIM pengecualian pada tabel utama dan audit Setting Biaya. */
	public static void initSettingBiaya() {
		addColumnTextIfMissing("public", "setting_biaya", "pengecualian_mahasiswa");
		addColumnTextIfMissing("new_audit", "setting_biaya__audit", "pengecualian_mahasiswa");
		addColumnBooleanIfMissing("public", "setting_biaya", "batasi_mahasiswa_tertentu");
		addColumnBooleanIfMissing("new_audit", "setting_biaya__audit", "batasi_mahasiswa_tertentu");
		addColumnIntegerIfMissing("public", "setting_biaya", "prioritas");
		addColumnIntegerIfMissing("new_audit", "setting_biaya__audit", "prioritas");
	}

	/** Tambah kolom integer bila belum ada; nilai lama dinormalisasi oleh InitIndex. */
	private static void addColumnIntegerIfMissing(String schema, String table, String column) {
		if (!isSafeIdentifier(schema) || !isSafeIdentifier(table) || !isSafeIdentifier(column)
				|| getColumnInfo(schema, table, column) != null) {
			return;
		}
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			session.createSQLQuery("ALTER TABLE " + quote(schema) + "." + quote(table) + " ADD COLUMN "
					+ quote(column) + " integer").executeUpdate();
			tx.commit();
			log("Berhasil tambah kolom integer: " + schema + "." + table + "." + column);
		} catch (Exception e) {
			rollbackQuietly(tx);
			log("Gagal tambah kolom " + schema + "." + table + "." + column + " : " + e.getMessage());
		} finally {
			closeQuietly(session);
		}
	}

	/** Tambah kolom boolean bila belum ada; dipakai juga untuk menjaga schema audit Envers. */
	private static void addColumnBooleanIfMissing(String schema, String table, String column) {
		if (!isSafeIdentifier(schema) || !isSafeIdentifier(table) || !isSafeIdentifier(column)
				|| getColumnInfo(schema, table, column) != null) {
			return;
		}
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			session.createSQLQuery("ALTER TABLE " + quote(schema) + "." + quote(table) + " ADD COLUMN "
					+ quote(column) + " boolean").executeUpdate();
			tx.commit();
			log("Berhasil tambah kolom boolean: " + schema + "." + table + "." + column);
		} catch (Exception e) {
			rollbackQuietly(tx);
			log("Gagal tambah kolom " + schema + "." + table + "." + column + " : " + e.getMessage());
		} finally {
			closeQuietly(session);
		}
	}

	/** Alasan pengajuan dapat berasal dari editor/API dan memang boleh lebih dari 255 karakter. */
	public static void initPengajuanMahasiswa() {
		String[] columns = new String[] { "keterangan" };
		alterColumnsToText("public", "pengajuan_mahasiswa", columns);
		alterColumnsToText("new_audit", "pengajuan_mahasiswa__audit", columns);
	}

	/**
	 * Menyamakan tabel utama dan tabel Envers untuk data realisasi kerja pegawai.
	 * Keterangan berasal dari input uraian pekerjaan, sedangkan oleh/olehid dapat berisi jejak
	 * perubahan yang disusun interceptor; ketiganya secara sah dapat melampaui 255 karakter.
	 * Perubahan tipe dilakukan setelah pemeriksaan information_schema sehingga aman dijalankan
	 * berulang pada setiap startup dan tidak mengubah data yang sudah tersimpan.
	 */
	public static void initRealisasiKerjaPegawai() {
		String[] columns = new String[] { "keterangan", "oleh", "olehid" };
		alterColumnsToText("public", "realisasi_kerja_pegawai", columns);
		alterColumnsToText("new_audit", "realisasi_kerja_pegawai__audit", columns);
	}

    public static void initKonfigurasi() {
        String[] columns = new String[] { "keterangan", "nilai", "nilaidikunci" };
        alterColumnsToText("public", "konfigurasi", columns);
        alterColumnsToText("new_audit", "konfigurasi__audit", columns);
    }

    /**
     * Kolom bahasa MANDARIN pada {@code label_bahasa}. hbm2ddl=update menambah kolom ke tabel utama
     * ({@code public.label_bahasa}) otomatis, TAPI tabel audit ({@code new_audit.label_bahasa__audit})
     * TIDAK — sehingga INSERT audit gagal jika kolomnya belum ada. Di sini kolom {@code mandarin}
     * dipastikan ada di KEDUA tabel.
     */
    public static void initLabelBahasaMandarin() {
        addColumnTextIfMissing("public", "label_bahasa", "mandarin");
        addColumnTextIfMissing("new_audit", "label_bahasa__audit", "mandarin");
    }

    /** Tambah kolom bertipe text bila belum ada. Aman untuk PostgreSQL lama (tanpa IF NOT EXISTS). */
    private static void addColumnTextIfMissing(String schema, String table, String column) {
        if (!isSafeIdentifier(schema) || !isSafeIdentifier(table) || !isSafeIdentifier(column)) {
            return;
        }
        ColumnInfo info = getColumnInfo(schema, table, column);
        if (info != null) {
            return; // kolom sudah ada
        }
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            String sql = "ALTER TABLE " + quote(schema) + "." + quote(table) + " ADD COLUMN " + quote(column)
                    + " text";
            session.createSQLQuery(sql).executeUpdate();
            tx.commit();
            log("Berhasil tambah kolom text: " + schema + "." + table + "." + column);
        } catch (Exception e) {
            rollbackQuietly(tx);
            log("Gagal tambah kolom " + schema + "." + table + "." + column + " : " + e.getMessage());
        } finally {
            closeQuietly(session);
        }
    }

    private static void alterColumnsToText(String schema, String table, String[] columns) {
        if (columns == null) {
            return;
        }
        for (int i = 0; i < columns.length; i++) {
            alterColumnToTextIfNeeded(schema, table, columns[i]);
        }
    }

    private static void alterColumnToTextIfNeeded(String schema, String table, String column) {
        if (!isSafeIdentifier(schema) || !isSafeIdentifier(table) || !isSafeIdentifier(column)) {
            return;
        }

        ColumnInfo info = getColumnInfo(schema, table, column);
        if (info == null) {
            log("Skip alter, kolom tidak ditemukan: " + schema + "." + table + "." + column);
            return;
        }
        if ("text".equalsIgnoreCase(info.dataType)) {
            return;
        }

        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            String sql = "ALTER TABLE " + quote(schema) + "." + quote(table) + " ALTER COLUMN " + quote(column)
                    + " TYPE text";
            session.createSQLQuery(sql).executeUpdate();
            tx.commit();
            log("Berhasil alter kolom menjadi text: " + schema + "." + table + "." + column + " dari "
                    + info.dataType + (info.maxLength == null ? "" : "(" + info.maxLength + ")"));
        } catch (Exception e) {
            rollbackQuietly(tx);
            log("Gagal alter kolom " + schema + "." + table + "." + column + " : " + e.getMessage());
        } finally {
            closeQuietly(session);
        }
    }

    private static ColumnInfo getColumnInfo(String schema, String table, String column) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            List result = session.createSQLQuery("select data_type, character_maximum_length "
                    + "from information_schema.columns "
                    + "where table_schema = :schemaName and table_name = :tableName and column_name = :columnName")
                    .setParameter("schemaName", schema).setParameter("tableName", table)
                    .setParameter("columnName", column).list();
            if (result == null || result.isEmpty()) {
                return null;
            }
            Object row = result.get(0);
            if (row instanceof Object[]) {
                Object[] arr = (Object[]) row;
                ColumnInfo info = new ColumnInfo();
                info.dataType = arr.length > 0 && arr[0] != null ? arr[0].toString() : "";
                info.maxLength = arr.length > 1 && arr[1] != null ? arr[1].toString() : null;
                return info;
            }
            ColumnInfo info = new ColumnInfo();
            info.dataType = row == null ? "" : row.toString();
            return info;
        } catch (Exception e) {
            log("Gagal cek kolom " + schema + "." + table + "." + column + " : " + e.getMessage());
            return null;
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
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DatabaseTextColumnSchemaFix.java:188");
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
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DatabaseTextColumnSchemaFix.java:200");
        }
        try {
            session.disconnect();
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DatabaseTextColumnSchemaFix.java:204");
        }
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DatabaseTextColumnSchemaFix.java:210");
        }
    }

    private static void log(String message) {
        try {
            System.out.println("DatabaseTextColumnSchemaFix: " + message);
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DatabaseTextColumnSchemaFix.java:217");
        }
    }

    private static class ColumnInfo {
        String dataType;
        String maxLength;
    }
}
