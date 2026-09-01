package ais.action.master.sekolah.util;

import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.Deposit;
import ais.database.model.Mahasiswa;
import ais.database.model.PengeluaranMahasiswa;
import ais.database.model.inventory.Pembelian;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.PencairanDiskon;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.WaktuUtil;

/**
 * Helper terfokus untuk deposit. Tipe ini membungkus satu variasi kecil dari alur yang lebih umum
 * agar pemanggil memakai nama domain yang jelas dan tidak menggandakan implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code getSafeDouble()});
 * validasi/perhitungan ({@code hitungDeposit()}, {@code hitungDeposit()}, {@code hitungDeposit()}, {@code
 * hitungDeposit()}); operasi domain lain ({@code formatWaktu()}, {@code closeSessionSafely()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
public class DepositHelper {

    /**
     * Mencatat topup siswa ke buku besar {@link Deposit}. Pemanggil wajib membuka transaksi
     * Hibernate sebelum memanggil method ini. Kombinasi sumber dan referensi dipakai sebagai
     * penanda idempoten supaya callback bank yang dikirim ulang tidak menambah saldo dua kali.
     */
    public static Deposit catatTopupSiswa(Session session, Siswa siswa, CalonSiswa calonSiswa,
            Double nominal, Date waktu, String sumber, String referensi) {
        if (session == null || nominal == null || nominal.doubleValue() <= 0.1
                || (siswa == null && calonSiswa == null)) {
            return null;
        }

        Date waktuTopup = waktu == null ? WaktuUtil.getDate() : waktu;
        String sumberAman = sumber == null || sumber.trim().length() == 0 ? "LAINNYA" : sumber.trim();
        String referensiAman = referensi == null || referensi.trim().length() == 0
                ? String.valueOf(waktuTopup.getTime()) : referensi.trim();
        String penanda = "TOPUP_SISWA:" + sumberAman + ":" + referensiAman;

        Criteria criteria = session.createCriteria(Deposit.class)
                .add(Restrictions.eq("keterangan", penanda));
        if (siswa != null) {
            criteria.add(Restrictions.eq("siswa", siswa));
        } else {
            criteria.add(Restrictions.eq("calonSiswa", calonSiswa));
        }

        Deposit deposit = (Deposit) criteria.setMaxResults(1).uniqueResult();
        if (deposit != null) {
            return deposit;
        }

        deposit = new Deposit();
        deposit.setSiswa(siswa);
        deposit.setCalonSiswa(calonSiswa);
        deposit.setNominal(nominal);
        deposit.setWaktu(waktuTopup);
        deposit.setKeterangan(penanda);
        deposit.setOleh("Topup Siswa - " + sumberAman);
        deposit.setOlehId(referensiAman);
        session.save(deposit);
        return deposit;
    }

    /**
     * Helper untuk mencegah NullPointerException dan mempersingkat baris kode
     */
    private static Double getSafeDouble(Number number) {
        return number == null ? 0.0 : number.doubleValue();
    }

    /**
     * Helper formatting waktu eksekusi (milidetik dan detik)
     */
    private static String formatWaktu(long start, long end) {
        long duration = end - start;
        double seconds = duration / 1000.0;
        return duration + " ms (" + String.format("%.3f", seconds) + " detik)";
    }

    public static Double hitungDeposit(Mahasiswa mahasiswa) {
        if (mahasiswa == null) {
            return 0.0;
        }

        Session session = null;
        try {
            session = HibernateUtil.currentNativeSession();
            String dbDate = Common.databaseDateFormat.get().format(WaktuUtil.getDate());
            
            long tTotalMulai = System.currentTimeMillis();
            System.out.println("--- Mulai Kalkulasi Deposit (Mahasiswa: " + mahasiswa.getId() + ") ---");

            // 1. Total Deposit Dibayar dari Cicilan
            long t1 = System.currentTimeMillis();
            
            String hql = "SELECT SUM(c.deposit) FROM CicilanPembayaran c WHERE c.kegiatan.mahasiswa.id = :mhsId";
            Double totalDepositDibayar = getSafeDouble((Number) session.createQuery(hql)
                    .setParameter("mhsId", mahasiswa.getId())
                    .uniqueResult());
            
            long t1_end = System.currentTimeMillis();
            System.out.println("Query [Total Deposit Dibayar] selesai dalam: " + formatWaktu(t1, t1_end));

            // 2. Total Deposit Masuk
            long t2 = System.currentTimeMillis();
            Double totalDeposit = getSafeDouble((Number) session.createCriteria(Deposit.class)
                    .setProjection(Projections.sum("nominal"))
                    .add(Restrictions.eq("mahasiswa", mahasiswa))
                    .uniqueResult());
            long t2_end = System.currentTimeMillis();
            System.out.println("Query [Total Deposit Masuk] selesai dalam: " + formatWaktu(t2, t2_end));

            // 3. Total Pengeluaran Mahasiswa
            long t3 = System.currentTimeMillis();
            Double totalPengeluaran = getSafeDouble((Number) session.createCriteria(PengeluaranMahasiswa.class)
                    .setProjection(Projections.sum("nominal"))
                    .add(Restrictions.eq("mahasiswa", mahasiswa))
                    .uniqueResult());
            long t3_end = System.currentTimeMillis();
            System.out.println("Query [Total Pengeluaran Mahasiswa] selesai dalam: " + formatWaktu(t3, t3_end));

            // 4. Pengeluaran Pembelian
            long t4 = System.currentTimeMillis();
            Double pengeluaranBelanja = getSafeDouble((Number) session.createCriteria(Pembelian.class)
                    .setProjection(Projections.sum("total"))
                    .add(Restrictions.sqlRestriction("date(this_.waktu) <= date('" + dbDate + "')"))
                    .add(Restrictions.eq("mahasiswa", mahasiswa))
                    .uniqueResult());
            long t4_end = System.currentTimeMillis();
            System.out.println("Query [Pengeluaran Pembelian] selesai dalam: " + formatWaktu(t4, t4_end));

            // 5. Total Casback All (Aktif + Expired)
            long t5 = System.currentTimeMillis();
            Double totalCasbackAll = getSafeDouble((Number) session.createCriteria(PencairanDiskon.class)
                    .setProjection(Projections.sum("nominalCair"))
                    .createAlias("caraPembayaran", "caraPembayaran")
                    .createAlias("anggotaKoperasi", "anggotaKoperasi")
                    .add(Restrictions.eq("caraPembayaran.manual", false))
                    .add(Restrictions.eq("status", "BERHASIL"))
                    .add(Restrictions.eq("anggotaKoperasi.mahasiswa", mahasiswa))
                    .add(Restrictions.sqlRestriction("date(this_.waktu_pencairan) <= date('" + dbDate + "')"))
                    .uniqueResult());
            long t5_end = System.currentTimeMillis();
            System.out.println("Query [Total Cashback All] selesai dalam: " + formatWaktu(t5, t5_end));

            // 6. Total Casback Expired
            long t6 = System.currentTimeMillis();
            Double totalCasbackExpired = getSafeDouble((Number) session.createCriteria(PencairanDiskon.class)
                    .setProjection(Projections.sum("nominalCair"))
                    .createAlias("caraPembayaran", "caraPembayaran")
                    .createAlias("anggotaKoperasi", "anggotaKoperasi")
                    .add(Restrictions.eq("caraPembayaran.manual", false))
                    .add(Restrictions.eq("status", "BERHASIL"))
                    .add(Restrictions.eq("anggotaKoperasi.mahasiswa", mahasiswa))
                    .add(Restrictions.sqlRestriction("this_.tanggal_expired_jika_berupa_topup is not null and date(this_.tanggal_expired_jika_berupa_topup) < date('" + dbDate + "')"))
                    .add(Restrictions.sqlRestriction("date(this_.waktu_pencairan) <= date('" + dbDate + "')"))
                    .uniqueResult());
            long t6_end = System.currentTimeMillis();
            System.out.println("Query [Total Cashback Expired] selesai dalam: " + formatWaktu(t6, t6_end));

            // --- MULAI PERHITUNGAN AKUNTANSI ---
            long t7 = System.currentTimeMillis();
            Double totalSemuaPengeluaran = totalDepositDibayar + totalPengeluaran + pengeluaranBelanja;
            Double cashbackHangus = Math.max(0.0, totalCasbackExpired - totalSemuaPengeluaran);
            Double totalDepositSaatini = (totalDeposit + totalCasbackAll) - totalSemuaPengeluaran - cashbackHangus;
            long tTotalSelesai = System.currentTimeMillis();
            System.out.println("Proses komputasi Akuntansi selesai dalam: " + formatWaktu(t7, tTotalSelesai));

            System.out.println("Deposit -> " + mahasiswa 
                    + ", totalDeposit -> " + Common.numberFormat.get().format(totalDeposit)
                    + ", totalDepositDibayar -> " + Common.numberFormat.get().format(totalDepositDibayar)
                    + ", totalCasbackAll -> " + Common.numberFormat.get().format(totalCasbackAll)
                    + ", totalCasbackExpired -> " + Common.numberFormat.get().format(totalCasbackExpired)
                    + ", totalPengeluaran -> " + Common.numberFormat.get().format(totalPengeluaran)
                    + ", pengeluaranBelanja -> " + Common.numberFormat.get().format(pengeluaranBelanja)
                    + ", cashbackHangus -> " + Common.numberFormat.get().format(cashbackHangus)
                    + ", totalDepositSaatini -> " + Common.numberFormat.get().format(totalDepositSaatini));
                    
            System.out.println("===> TOTAL WAKTU EKSEKUSI hitungDeposit(Mahasiswa): " + formatWaktu(tTotalMulai, tTotalSelesai) + " <===");

            return Math.max(0.0, totalDepositSaatini);

        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/util/DepositHelper.java:146");
        } finally {
            closeSessionSafely(session);
        }
        return 0.0;
    }

    public static Double hitungDeposit(Siswa siswa, CalonSiswa calonSiswa) {
        boolean isSiswa = siswa != null && siswa.getId() != null;
        boolean isCalon = calonSiswa != null && calonSiswa.getId() != null;

        if (!isSiswa && !isCalon) {
            return 0.0;
        }

        // Tentukan properti dan entitas pencarian untuk menghindari if-else di dalam Criteria
        String propertyName = isSiswa ? "siswa" : "calonSiswa";
        Object entityParam = isSiswa ? siswa : calonSiswa;
        String koperasiProp = isSiswa ? "anggotaKoperasi.siswa" : "anggotaKoperasi.calonSiswa";

        Session session = null;
        try {
            session = HibernateUtil.currentNativeSession();
            String dbDate = Common.databaseDateFormat.get().format(WaktuUtil.getDate());

            long tTotalMulai = System.currentTimeMillis();
            System.out.println("--- Mulai Kalkulasi Deposit (" + (isSiswa ? "Siswa" : "Calon Siswa") + ") ---");

            // 1. Total Deposit Dibayar
            long t1 = System.currentTimeMillis();
            Double totalDepositDibayar = getSafeDouble((Number) session.createCriteria(PembayaranSiswa.class)
                    .setProjection(Projections.sum("dariTabungan"))
                    .add(Restrictions.eq(propertyName, entityParam))
                    .add(Restrictions.isNotNull("dariTabungan"))
                    .add(Restrictions.gt("dariTabungan", 0.1))
                    .uniqueResult());
            long t1_end = System.currentTimeMillis();
            System.out.println("Query [Total Deposit Dibayar] selesai dalam: " + formatWaktu(t1, t1_end));

            // 2. Deposit Siswa / Calon Siswa
            long t2 = System.currentTimeMillis();
            Double totalDeposit = getSafeDouble((Number) session.createCriteria(Deposit.class)
                    .setProjection(Projections.sum("nominal"))
                    .add(Restrictions.eq(propertyName, entityParam))
                    .uniqueResult());
            long t2_end = System.currentTimeMillis();
            System.out.println("Query [Total Deposit Masuk] selesai dalam: " + formatWaktu(t2, t2_end));

            // 3. Pengeluaran
            long t3 = System.currentTimeMillis();
            Double totalPengeluaran = getSafeDouble((Number) session.createCriteria(PengeluaranMahasiswa.class)
                    .setProjection(Projections.sum("nominal"))
                    .add(Restrictions.eq(propertyName, entityParam))
                    .uniqueResult());
            long t3_end = System.currentTimeMillis();
            System.out.println("Query [Total Pengeluaran] selesai dalam: " + formatWaktu(t3, t3_end));

            // 4. Total Pengeluaran Pembelian
            long t4 = System.currentTimeMillis();
            Double pengeluaranBelanja = getSafeDouble((Number) session.createCriteria(Pembelian.class)
                    .setProjection(Projections.sum("total"))
                    .add(Restrictions.sqlRestriction("date(this_.waktu) <= date('" + dbDate + "')"))
                    .add(Restrictions.eq(propertyName, entityParam))
                    .uniqueResult());
            long t4_end = System.currentTimeMillis();
            System.out.println("Query [Pengeluaran Pembelian] selesai dalam: " + formatWaktu(t4, t4_end));

            // 5. Total Casback All
            long t5 = System.currentTimeMillis();
            Double totalCasbackAll = getSafeDouble((Number) session.createCriteria(PencairanDiskon.class)
                    .setProjection(Projections.sum("nominalCair"))
                    .createAlias("caraPembayaran", "caraPembayaran")
                    .createAlias("anggotaKoperasi", "anggotaKoperasi")
                    .add(Restrictions.eq("caraPembayaran.manual", false))
                    .add(Restrictions.eq("status", "BERHASIL"))
                    .add(Restrictions.eq(koperasiProp, entityParam))
                    .add(Restrictions.sqlRestriction("date(this_.waktu_pencairan) <= date('" + dbDate + "')"))
                    .uniqueResult());
            long t5_end = System.currentTimeMillis();
            System.out.println("Query [Total Cashback All] selesai dalam: " + formatWaktu(t5, t5_end));

            // 6. Total Casback Expired
            long t6 = System.currentTimeMillis();
            Double totalCasbackExpired = getSafeDouble((Number) session.createCriteria(PencairanDiskon.class)
                    .setProjection(Projections.sum("nominalCair"))
                    .createAlias("caraPembayaran", "caraPembayaran")
                    .createAlias("anggotaKoperasi", "anggotaKoperasi")
                    .add(Restrictions.eq("caraPembayaran.manual", false))
                    .add(Restrictions.eq("status", "BERHASIL"))
                    .add(Restrictions.eq(koperasiProp, entityParam))
                    .add(Restrictions.sqlRestriction("this_.tanggal_expired_jika_berupa_topup is not null and date(this_.tanggal_expired_jika_berupa_topup) < date('" + dbDate + "')"))
                    .add(Restrictions.sqlRestriction("date(this_.waktu_pencairan) <= date('" + dbDate + "')"))
                    .uniqueResult());
            long t6_end = System.currentTimeMillis();
            System.out.println("Query [Total Cashback Expired] selesai dalam: " + formatWaktu(t6, t6_end));

            // --- MULAI PERHITUNGAN AKUNTANSI ---
            long t7 = System.currentTimeMillis();
            Double totalSemuaPengeluaran = totalDepositDibayar + totalPengeluaran + pengeluaranBelanja;
            Double cashbackHangus = Math.max(0.0, totalCasbackExpired - totalSemuaPengeluaran);
            Double totalDepositSaatini = (totalDeposit + totalCasbackAll) - totalSemuaPengeluaran - cashbackHangus;
            long tTotalSelesai = System.currentTimeMillis();
            System.out.println("Proses komputasi Akuntansi selesai dalam: " + formatWaktu(t7, tTotalSelesai));

            System.out.println("Deposit -> " + (isSiswa ? siswa : calonSiswa)
                    + ", totalDeposit -> " + Common.numberFormat.get().format(totalDeposit)
                    + ", totalDepositDibayar -> " + Common.numberFormat.get().format(totalDepositDibayar)
                    + ", totalCasbackAll -> " + Common.numberFormat.get().format(totalCasbackAll)
                    + ", totalCasbackExpired -> " + Common.numberFormat.get().format(totalCasbackExpired)
                    + ", totalPengeluaran -> " + Common.numberFormat.get().format(totalPengeluaran)
                    + ", pengeluaranBelanja -> " + Common.numberFormat.get().format(pengeluaranBelanja)
                    + ", cashbackHangus -> " + Common.numberFormat.get().format(cashbackHangus)
                    + ", totalDepositSaatini -> " + Common.numberFormat.get().format(totalDepositSaatini));
                    
            System.out.println("===> TOTAL WAKTU EKSEKUSI hitungDeposit(Siswa/CalonSiswa): " + formatWaktu(tTotalMulai, tTotalSelesai) + " <===");

            return Math.max(0.0, totalDepositSaatini);

        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/util/DepositHelper.java:265");
        } finally {
            closeSessionSafely(session);
        }
        return 0.0;
    }

    public static Double hitungDeposit(AnggotaKoperasi anggotaKoperasi) {
        return hitungDeposit(anggotaKoperasi, null);
    }

    public static Double hitungDeposit(AnggotaKoperasi anggotaKoperasi, Date tanggalBatas) {
        if (anggotaKoperasi == null) {
            return 0.0;
        }

        Session session = null;
        try {
            session = HibernateUtil.currentNativeSession();

            Date tanggal = (tanggalBatas != null) ? tanggalBatas : WaktuUtil.getDate();
            String dbDate = Common.databaseDateFormat.get().format(tanggal);

            long tTotalMulai = System.currentTimeMillis();
            System.out.println("--- Mulai Kalkulasi Deposit (Anggota Koperasi: " + anggotaKoperasi.getId() + ") ---");

            Double totalDepositDibayar = 0.0;
            Double totalPengeluaran = 0.0;

            // 1. Deposit
            long t1 = System.currentTimeMillis();
            Criteria critDeposit = session.createCriteria(Deposit.class)
                    .setProjection(Projections.sum("nominal"))
                    .add(Restrictions.eq("anggotaKoperasi", anggotaKoperasi));
            if (tanggalBatas != null) {
                critDeposit.add(Restrictions.le("waktu", tanggalBatas));
            }
            Double totalDeposit = getSafeDouble((Number) critDeposit.uniqueResult());
            long t1_end = System.currentTimeMillis();
            System.out.println("Query [Total Deposit Masuk] selesai dalam: " + formatWaktu(t1, t1_end));

            // 2. Pengeluaran Pembelian
            long t2 = System.currentTimeMillis();
            // Syarat pemotongan saldo: metode bayar TIDAK diverifikasi manual (perilaku lama), ATAU
            // metode bayar itu ditandai eksplisit "Memotong Deposit" (kolom baru 2026-07-26, lihat
            // CaraPembayaranKoperasi.getMemotongDeposit). Bersifat MENAMBAH: metode lama yang kolomnya
            // masih null/false tetap berperilaku persis seperti sebelumnya.
            //
            // CATATAN NULL: manual=false TIDAK mencocokkan baris ber-NULL di SQL, sehingga
            // metode bayar lama yang manual-nya NULL memang sengaja tidak ikut memotong -- sama
            // dengan getManual() yang menganggap NULL = manual = true.
            //
            // CATATAN SPLIT PEMBAYARAN (2026-08-10): dihitung dari HEADER koperasi.pembelian_anggota_
            // koperasi (bukan lagi baris detail koperasi.pembelian per item -- lihat JavaDoc
            // PembelianAnggotaKoperasi.getNominalBayar1()), karena satu transaksi kini bisa dibayar s/d
            // 5 metode berbeda sekaligus (mis. separuh Transfer + separuh Tunai) -- baris item TIDAK
            // punya cara pembayaran sendiri-sendiri, hanya menyalin metode header lama sebelum fitur ini
            // ada. Slot 1 nominalnya IMPLISIT (total_biaya dikurangi slot 2-5, lihat getNominalBayar1()),
            // makanya dihitung di sini via ekspresi yang sama, bukan kolom tersendiri.
            Double pengeluaranBelanja = getSafeDouble((Number) session.createSQLQuery(
                    "SELECT COALESCE(SUM(" +
                    "  CASE WHEN cpk1.manual = false OR cpk1.memotong_deposit = true " +
                    "       THEN GREATEST(0, COALESCE(h.total_biaya,0) - COALESCE(h.nominal_bayar_2,0) - COALESCE(h.nominal_bayar_3,0) - COALESCE(h.nominal_bayar_4,0) - COALESCE(h.nominal_bayar_5,0)) " +
                    "       ELSE 0 END " +
                    "  + CASE WHEN cpk2.manual = false OR cpk2.memotong_deposit = true THEN COALESCE(h.nominal_bayar_2,0) ELSE 0 END " +
                    "  + CASE WHEN cpk3.manual = false OR cpk3.memotong_deposit = true THEN COALESCE(h.nominal_bayar_3,0) ELSE 0 END " +
                    "  + CASE WHEN cpk4.manual = false OR cpk4.memotong_deposit = true THEN COALESCE(h.nominal_bayar_4,0) ELSE 0 END " +
                    "  + CASE WHEN cpk5.manual = false OR cpk5.memotong_deposit = true THEN COALESCE(h.nominal_bayar_5,0) ELSE 0 END " +
                    "),0) " +
                    "FROM koperasi.pembelian_anggota_koperasi h " +
                    "LEFT JOIN koperasi.cara_pembayaran_koperasi cpk1 ON h.cara_pembayaran_koperasi = cpk1.id " +
                    "LEFT JOIN koperasi.cara_pembayaran_koperasi cpk2 ON h.cara_pembayaran_koperasi_2 = cpk2.id " +
                    "LEFT JOIN koperasi.cara_pembayaran_koperasi cpk3 ON h.cara_pembayaran_koperasi_3 = cpk3.id " +
                    "LEFT JOIN koperasi.cara_pembayaran_koperasi cpk4 ON h.cara_pembayaran_koperasi_4 = cpk4.id " +
                    "LEFT JOIN koperasi.cara_pembayaran_koperasi cpk5 ON h.cara_pembayaran_koperasi_5 = cpk5.id " +
                    "WHERE h.anggota_koperasi = :anggotaId AND date(h.tanggal_pembayaran) <= date('" + dbDate + "')")
                    .setParameter("anggotaId", anggotaKoperasi.getId())
                    .uniqueResult());
            long t2_end = System.currentTimeMillis();
            System.out.println("Query [Pengeluaran Pembelian] selesai dalam: " + formatWaktu(t2, t2_end));

            // 3. Total Casback All
            long t3 = System.currentTimeMillis();
            Double totalCasbackAll = getSafeDouble((Number) session.createCriteria(PencairanDiskon.class)
                    .setProjection(Projections.sum("nominalCair"))
                    .createAlias("caraPembayaran", "caraPembayaran")
                    .add(Restrictions.eq("caraPembayaran.manual", false))
                    .add(Restrictions.eq("status", "BERHASIL"))
                    .add(Restrictions.eq("anggotaKoperasi", anggotaKoperasi))
                    .add(Restrictions.sqlRestriction("date(this_.waktu_pencairan) <= date('" + dbDate + "')"))
                    .uniqueResult());
            long t3_end = System.currentTimeMillis();
            System.out.println("Query [Total Cashback All] selesai dalam: " + formatWaktu(t3, t3_end));

            // 4. Total Casback Expired
            long t4 = System.currentTimeMillis();
            Double totalCasbackExpired = getSafeDouble((Number) session.createCriteria(PencairanDiskon.class)
                    .setProjection(Projections.sum("nominalCair"))
                    .createAlias("caraPembayaran", "caraPembayaran")
                    .add(Restrictions.eq("caraPembayaran.manual", false))
                    .add(Restrictions.eq("status", "BERHASIL"))
                    .add(Restrictions.eq("anggotaKoperasi", anggotaKoperasi))
                    .add(Restrictions.sqlRestriction("this_.tanggal_expired_jika_berupa_topup is not null and date(this_.tanggal_expired_jika_berupa_topup) < date('" + dbDate + "')"))
                    .add(Restrictions.sqlRestriction("date(this_.waktu_pencairan) <= date('" + dbDate + "')"))
                    .uniqueResult());
            long t4_end = System.currentTimeMillis();
            System.out.println("Query [Total Cashback Expired] selesai dalam: " + formatWaktu(t4, t4_end));

            // 5. Total Deposit Voucher Expired (Fitur Voucher Pegawai -- baris Deposit dgn
            //    tanggalExpired terisi & sudah lewat, lihat JavaDoc Deposit.getTanggalExpired()).
            //    Deposit top-up tunai biasa (tanggalExpired NULL) TIDAK PERNAH masuk hitungan ini.
            long t5a = System.currentTimeMillis();
            Double totalDepositVoucherExpired = getSafeDouble((Number) session.createCriteria(Deposit.class)
                    .setProjection(Projections.sum("nominal"))
                    .add(Restrictions.eq("anggotaKoperasi", anggotaKoperasi))
                    .add(Restrictions.sqlRestriction("this_.tanggal_expired is not null and this_.tanggal_expired < date('" + dbDate + "')"))
                    .add(Restrictions.sqlRestriction("date(this_.waktu) <= date('" + dbDate + "')"))
                    .uniqueResult());
            long t5a_end = System.currentTimeMillis();
            System.out.println("Query [Total Deposit Voucher Expired] selesai dalam: " + formatWaktu(t5a, t5a_end));

            // --- MULAI PERHITUNGAN AKUNTANSI ---
            long t5 = System.currentTimeMillis();
            Double totalSemuaPengeluaran = totalDepositDibayar + totalPengeluaran + pengeluaranBelanja;
            // Cashback (PencairanDiskon) dan voucher (Deposit) yg sudah expired DIGABUNG jadi SATU pool
            // sebelum dikurangi pengeluaran -- BUKAN dihitung terpisah -- supaya satu nominal belanja
            // yang sama tidak "melindungi" dua pool kedaluwarsa sekaligus (mis. belanja 100 tidak boleh
            // dianggap menghabiskan 100 cashback expired SEKALIGUS 100 voucher expired -- itu double-count).
            Double totalPoolExpired = totalCasbackExpired + totalDepositVoucherExpired;
            Double totalHangus = Math.max(0.0, totalPoolExpired - totalSemuaPengeluaran);
            Double totalDepositSaatini = (totalDeposit + totalCasbackAll) - totalSemuaPengeluaran - totalHangus;
            long tTotalSelesai = System.currentTimeMillis();
            System.out.println("Proses komputasi Akuntansi selesai dalam: " + formatWaktu(t5, tTotalSelesai));

            System.out.println("Deposit -> " + anggotaKoperasi
                    + ", totalDeposit -> " + Common.numberFormat.get().format(totalDeposit)
                    + ", totalCasbackAll -> " + Common.numberFormat.get().format(totalCasbackAll)
                    + ", totalCasbackExpired -> " + Common.numberFormat.get().format(totalCasbackExpired)
                    + ", totalDepositVoucherExpired -> " + Common.numberFormat.get().format(totalDepositVoucherExpired)
                    + ", totalPengeluaranBelanja -> " + Common.numberFormat.get().format(totalSemuaPengeluaran)
                    + ", totalHangus -> " + Common.numberFormat.get().format(totalHangus)
                    + ", totalDepositSaatini -> " + Common.numberFormat.get().format(totalDepositSaatini));
                    
            System.out.println("===> TOTAL WAKTU EKSEKUSI hitungDeposit(AnggotaKoperasi): " + formatWaktu(tTotalMulai, tTotalSelesai) + " <===");

            return Math.max(0.0, totalDepositSaatini);

        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/util/DepositHelper.java:366");
        } finally {
            closeSessionSafely(session);
        }
        return 0.0;
    }

    /**
     * Memastikan pemutusan koneksi session benar-benar terjadi tanpa throw exception baru.
     */
    private static void closeSessionSafely(Session session) {
        if (session != null && session.isOpen()) {
            try {
                session.disconnect();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/DepositHelper.java:380");
                // Ignore jika disconnect gagal
            }
            try {
                session.close();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/DepositHelper.java:385");
                // Ignore jika close gagal
            }
        }
        try {
            HibernateUtil.closeSession();
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/DepositHelper.java:391");
            // Ignore
        }
    }
}
