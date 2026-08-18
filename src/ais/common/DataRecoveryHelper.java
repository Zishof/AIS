package ais.common; // Sesuaikan package Anda

import java.util.ArrayList;
import java.util.List;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.PengaturanBiaya;

public class DataRecoveryHelper {

    /**
     * Method (Tunggal) untuk merestore data pembayaran 1 Pengaturan Biaya.
     * ME-REUSE method kolektif agar kode lebih bersih (DRY Principle).
     */
    public static void restoreDeletedDataFromAudit(PengaturanBiaya pengaturanBiaya, String namaSiswa, List<String> warnings, ProgressListener progress) {
        if (pengaturanBiaya == null || pengaturanBiaya.getId() == null) {
            if (warnings != null) warnings.add("PengaturanBiaya tidak valid.");
            return;
        }

        List<PengaturanBiaya> listPengaturanBiaya = new ArrayList<PengaturanBiaya>();
        listPengaturanBiaya.add(pengaturanBiaya);
        restoreDeletedDataFromAudit(listPengaturanBiaya, namaSiswa, warnings, progress);
    }

    /**
     * Method (Kolektif / UTAMA) untuk merestore data pembayaran spesifik per banyak Pengaturan Biaya.
     * Dilengkapi Dynamic Query Optimization untuk membuang LEFT JOIN jika namaSiswa kosong.
     */
    public static void restoreDeletedDataFromAudit(List<PengaturanBiaya> pengaturanBiayas, String namaSiswa, List<String> warnings, ProgressListener progress) {
        if (pengaturanBiayas == null || pengaturanBiayas.isEmpty()) {
            if (warnings != null) warnings.add("Daftar PengaturanBiaya kosong, proses dibatalkan.");
            return;
        }

        List<Long> pbIds = new ArrayList<Long>();
        for (PengaturanBiaya pb : pengaturanBiayas) {
            if (pb != null && pb.getId() != null) {
                pbIds.add(pb.getId());
            }
        }

        if (pbIds.isEmpty()) {
            if (warnings != null) warnings.add("Tidak ada ID PengaturanBiaya yang valid untuk diproses.");
            return;
        }

        // =================================================================================
        // DYNAMIC QUERY OPTIMIZATION BUILDER
        // Jika namaSiswa kosong, kita buang LEFT JOIN agar query berjalan super cepat
        // =================================================================================
        boolean hasFilter = namaSiswa != null && !namaSiswa.trim().isEmpty();
        String safeName = hasFilter ? namaSiswa.trim() : "";
        
        // 1. Kondisi Filter WHERE
        String condSiswa = hasFilter ? 
                " AND (c.nomor_induk ILIKE '%" + safeName + "%' OR c.nama_siswa ILIKE '%" + safeName + "%' " +
                "      OR cs.nomor_induk ILIKE '%" + safeName + "%' OR cs.nama_siswa ILIKE '%" + safeName + "%') " : "";
        
        // 2. String JOIN sesuai dengan alias tabel utama di masing-masing step
        String joinA1 = hasFilter ? " LEFT JOIN sekolah.siswa c ON c.id = a1.siswa_id LEFT JOIN sekolah.calon_siswa cs ON cs.id = a1.calon_siswa_id " : "";
        String joinA  = hasFilter ? " LEFT JOIN sekolah.siswa c ON c.id = a.siswa_id LEFT JOIN sekolah.calon_siswa cs ON cs.id = a.calon_siswa_id " : "";
        String joinPs = hasFilter ? " LEFT JOIN sekolah.pembayaran_siswa ps ON ps.id = a.pembayaran_siswa_id LEFT JOIN sekolah.siswa c ON c.id = ps.siswa_id LEFT JOIN sekolah.calon_siswa cs ON cs.id = ps.calon_siswa_id " : "";
        
        // 3. String Pelindung FK (Foreign Key) Khusus untuk INSERT
        String fkProtectorSiswa = 
                " AND (a.siswa_id IS NULL OR EXISTS (SELECT 1 FROM sekolah.siswa s WHERE s.id = a.siswa_id)) " +
                " AND (a.calon_siswa_id IS NULL OR EXISTS (SELECT 1 FROM sekolah.calon_siswa ccs WHERE ccs.id = a.calon_siswa_id)) ";

        Session session = null;
        Transaction tx = null;

        try {
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();

            // =================================================================================
            // STEP 1. UPDATE ID TAGIHAN KOLEKTIF
            // =================================================================================
            if (progress != null) progress.onProgress(10, "Menyiapkan Data Tagihan...");
            
            String sqlUpdateTagihanId = 
                    "UPDATE sekolah.tagihan " +
                    "SET id = a.id, nominal = a.nominal, denda = a.denda, diskon = a.diskon " + 
                    "FROM (" +
                    "    SELECT a1.id, a1.kode_unik, a1.nominal, a1.denda, a1.diskon " +
                    "    FROM new_audit.tagihan__audit a1 " +
                    "    INNER JOIN (" +
                    "        SELECT id, MAX(REV) as max_rev " +
                    "        FROM new_audit.tagihan__audit " +
                    "        WHERE REVTYPE IN (0, 1) AND pengaturan_biaya IN (:pbIds) " +
                    "        GROUP BY id" +
                    "    ) a2 ON a1.id = a2.id AND a1.REV = a2.max_rev " +
                         joinA1 + // <- Inject Dynamic Join
                    "    WHERE a1.pengaturan_biaya IN (:pbIds) " +
                    "      AND a1.pembayaran_siswa_detail_id IS NOT NULL " + condSiswa + 
                    ") a " +
                    "WHERE sekolah.tagihan.kode_unik = a.kode_unik " +
                    "  AND sekolah.tagihan.pembayaran_siswa_detail_id IS NULL";

            SQLQuery qUpdateTagihanId = session.createSQLQuery(sqlUpdateTagihanId);
            qUpdateTagihanId.setTimeout(600);
            qUpdateTagihanId.setParameterList("pbIds", pbIds);
            qUpdateTagihanId.executeUpdate();


            // =================================================================================
            // STEP 1.5 INSERT TAGIHAN YANG HILANG TOTAL
            // =================================================================================
            if (progress != null) progress.onProgress(25, "Memulihkan Tagihan yang hilang...");

            String sqlInsertTagihan = 
                    "INSERT INTO sekolah.tagihan (id, kode_unik, nominal, denda, diskon, bulan, tahun, bayarke, pengaturan_biaya, siswa_id, calon_siswa_id, item_biaya_id, nominal_biaya_id) " + 
                    "SELECT a.id, a.kode_unik, a.nominal, a.denda, a.diskon, a.bulan, a.tahun, a.bayarke, a.pengaturan_biaya, a.siswa_id, a.calon_siswa_id, a.item_biaya_id, a.nominal_biaya_id " +
                    "FROM new_audit.tagihan__audit a " +
                    "INNER JOIN (" +
                    "    SELECT id, MAX(REV) as max_rev " +
                    "    FROM new_audit.tagihan__audit " +
                    "    WHERE REVTYPE IN (0, 1) AND pengaturan_biaya IN (:pbIds) " +
                    "    GROUP BY id" +
                    ") a2 ON a.id = a2.id AND a.REV = a2.max_rev " +
                    joinA + // <- Inject Dynamic Join
                    "WHERE a.pengaturan_biaya IN (:pbIds) " +
                    "  AND a.pembayaran_siswa_detail_id IS NOT NULL " +
                    "  AND NOT EXISTS (SELECT 1 FROM sekolah.tagihan utama WHERE utama.id = a.id) " +
                    "  AND NOT EXISTS (SELECT 1 FROM sekolah.tagihan cek_kode WHERE cek_kode.kode_unik = a.kode_unik) " + // PELINDUNG DUPLIKAT KODE UNIK
                    fkProtectorSiswa + // <- Pelindung FK Aman
                    condSiswa;

            SQLQuery qInsertTagihan = session.createSQLQuery(sqlInsertTagihan);
            qInsertTagihan.setTimeout(600);
            qInsertTagihan.setParameterList("pbIds", pbIds);
            int insertedTagihan = qInsertTagihan.executeUpdate();
            System.out.println("Berhasil me-restore " + insertedTagihan + " data Tagihan yang hilang total.");


            // =================================================================================
            // STEP 2. INSERT PEMBAYARAN SISWA KOLEKTIF
            // Perbaikan nama kolom lowercase: olehid, nominalmanual, daritabungan
            // =================================================================================
            if (progress != null) progress.onProgress(40, "Mengembalikan Header Pembayaran...");
            
            String sqlPembayaran = 
                    "INSERT INTO sekolah.pembayaran_siswa (" +
                    "id, oleh, olehid, tanggal_dirubah, nama, bank_host_id, jenis_biaya_id, " +
                    "akun_pembayaran_siswa_id, sekolah_id, siswa_id, calon_siswa_id, yayasan_id, " +
                    "bulan, inquiry_pembayaran, nominal, nominalmanual, daritabungan, daritabunganmanual, " +
                    "sisa_deposit, tahun, tahun_dan_bulan, tanggal, tanggal_bayar, tambahan_deposit, " +
                    "total_deposit, bri_request_id, bni_request_id, bsi_request_id, virtual_account_bank, " +
                    "validator, validator_user, keterangan) " + 
                    "SELECT " +
                    "a.id, a.oleh, a.olehid, a.tanggal_dirubah, a.nama, a.bank_host_id, a.jenis_biaya_id, " +
                    "a.akun_pembayaran_siswa_id, a.sekolah_id, a.siswa_id, a.calon_siswa_id, a.yayasan_id, " +
                    "a.bulan, a.inquiry_pembayaran, a.nominal, a.nominalmanual, a.daritabungan, a.daritabunganmanual, " +
                    "a.sisa_deposit, a.tahun, a.tahun_dan_bulan, a.tanggal, a.tanggal_bayar, a.tambahan_deposit, " +
                    "a.total_deposit, a.bri_request_id, a.bni_request_id, a.bsi_request_id, a.virtual_account_bank, " +
                    "a.validator, a.validator_user, a.keterangan " + 
                    "FROM new_audit.pembayaran_siswa__audit a " +
                    "INNER JOIN (" +
                    "    SELECT id, MAX(REV) as max_rev " +
                    "    FROM new_audit.pembayaran_siswa__audit " +
                    "    WHERE REVTYPE IN (0, 1) " +
                    "      AND id IN (" +
                    "          SELECT ad.pembayaran_siswa_id " +
                    "          FROM new_audit.pembayaran_siswa_detail__audit ad " +
                    "          WHERE ad.tagihan IN (" +
                    "              SELECT atag.id FROM new_audit.tagihan__audit atag " +
                    "              WHERE atag.pengaturan_biaya IN (:pbIds) AND atag.pembayaran_siswa_detail_id IS NOT NULL" +
                    "          )" +
                    "      ) " +
                    "    GROUP BY id" +
                    ") data_terakhir ON a.id = data_terakhir.id AND a.REV = data_terakhir.max_rev " +
                    joinA + // <- Inject Dynamic Join
                    "WHERE NOT EXISTS (SELECT 1 FROM sekolah.pembayaran_siswa utama WHERE utama.id = a.id) " +
                    fkProtectorSiswa + // <- Pelindung FK Aman
                    condSiswa;

            SQLQuery qPembayaran = session.createSQLQuery(sqlPembayaran);
            qPembayaran.setTimeout(600);
            qPembayaran.setParameterList("pbIds", pbIds);
            int restoredPembayaran = qPembayaran.executeUpdate();
            System.out.println("Berhasil mengembalikan " + restoredPembayaran + " data PembayaranSiswa.");


            // =================================================================================
            // STEP 3. INSERT PEMBAYARAN SISWA DETAIL KOLEKTIF
            // =================================================================================
            if (progress != null) progress.onProgress(65, "Mengembalikan Rincian Pembayaran...");
            
            String sqlDetail = 
                    "INSERT INTO sekolah.pembayaran_siswa_detail (id, nominal, pembayaran_siswa_id, tagihan, item_biaya_id) " + 
                    "SELECT a.id, a.nominal, a.pembayaran_siswa_id, a.tagihan, a.item_biaya_id " + 
                    "FROM new_audit.pembayaran_siswa_detail__audit a " +
                    "INNER JOIN (" +
                    "    SELECT id, MAX(REV) as max_rev " +
                    "    FROM new_audit.pembayaran_siswa_detail__audit " +
                    "    WHERE REVTYPE IN (0, 1) " +
                    "      AND tagihan IN (" +
                    "          SELECT atag.id FROM new_audit.tagihan__audit atag " +
                    "          WHERE atag.pengaturan_biaya IN (:pbIds) AND atag.pembayaran_siswa_detail_id IS NOT NULL" +
                    "      ) " +
                    "    GROUP BY id" +
                    ") data_terakhir ON a.id = data_terakhir.id AND a.REV = data_terakhir.max_rev " +
                    joinPs + // <- Inject Dynamic Join
                    "WHERE NOT EXISTS (SELECT 1 FROM sekolah.pembayaran_siswa_detail utama WHERE utama.id = a.id) " +
                    "  AND EXISTS (SELECT 1 FROM sekolah.pembayaran_siswa ps_check WHERE ps_check.id = a.pembayaran_siswa_id) " + 
                    "  AND EXISTS (SELECT 1 FROM sekolah.tagihan tg_check WHERE tg_check.id = a.tagihan) " + 
                    condSiswa; 

            SQLQuery qDetail = session.createSQLQuery(sqlDetail);
            qDetail.setTimeout(600);
            qDetail.setParameterList("pbIds", pbIds);
            int restoredDetail = qDetail.executeUpdate();
            System.out.println("Berhasil mengembalikan " + restoredDetail + " data PembayaranSiswaDetail.");


            // =================================================================================
            // STEP 4. LINK/SAMBUNGKAN KEMBALI KOLEKTIF
            // =================================================================================
            if (progress != null) progress.onProgress(90, "Menyambungkan Relasi Data...");
            
            String sqlLinkTagihanDetail = 
                    "UPDATE sekolah.tagihan " +
                    "SET pembayaran_siswa_detail_id = a.pembayaran_siswa_detail_id " +
                    "FROM (" +
                    "    SELECT a1.id, a1.pembayaran_siswa_detail_id " +
                    "    FROM new_audit.tagihan__audit a1 " +
                    "    INNER JOIN (" +
                    "        SELECT id, MAX(REV) as max_rev " +
                    "        FROM new_audit.tagihan__audit " +
                    "        WHERE REVTYPE IN (0, 1) AND pengaturan_biaya IN (:pbIds) " + 
                    "        GROUP BY id" +
                    "    ) a2 ON a1.id = a2.id AND a1.REV = a2.max_rev " +
                         joinA1 + // <- Inject Dynamic Join
                    "    WHERE a1.pengaturan_biaya IN (:pbIds) " +
                    "      AND a1.pembayaran_siswa_detail_id IS NOT NULL " + condSiswa +
                    ") a " +
                    "WHERE sekolah.tagihan.id = a.id " +
                    "  AND sekolah.tagihan.pembayaran_siswa_detail_id IS NULL";

            SQLQuery qLinkTagihanDetail = session.createSQLQuery(sqlLinkTagihanDetail);
            qLinkTagihanDetail.setTimeout(600);
            qLinkTagihanDetail.setParameterList("pbIds", pbIds);
            qLinkTagihanDetail.executeUpdate();

            // Commit Transaksi
            tx.commit();
            if (progress != null) progress.onProgress(100, "Selesai memproses data!");

        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            if (warnings != null) {
                warnings.add("Gagal memulihkan data pembayaran: " + e.getMessage());
            }
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataRecoveryHelper.java:261");
        } finally {
            if (session != null) {
                try { 
                    if (session.isOpen()) { 
                        session.disconnect(); 
                        session.close(); 
                    } 
                } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/DataRecoveryHelper.java:269");
                    // Abaikan exception saat menutup koneksi
                }
            }
        }
    }
}