package ais.action.master.akunting.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.action.master.helper.PostingJurnalHelper;
import ais.common.Common;
import ais.database.model.CicilanPembayaran;
import ais.database.model.Deposit;
import ais.database.model.DetailKegiatan;
import ais.database.model.LogPembayaran;
import ais.database.model.PengeluaranMahasiswa;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.KasBesar;
import ais.database.model.akunting.KasKecil;
import ais.database.model.akunting.Pajak;
import ais.database.model.akunting.Pertangungjawaban;
import ais.database.model.akunting.Transaksi;
import ais.database.model.akunting.Transitori;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenyusutanAsset;
import ais.database.model.asset.SaldoAwalMasterAsset;
import ais.database.model.payroll.PembayaranGaji;
import ais.database.model.sekolah.DepositSiswa;
import ais.database.model.sekolah.PembayaranSiswaDetail;
import ais.database.model.sekolah.Tagihan;

/**
 * Penghitung angka dasbor <b>Draft Jurnal</b>: berapa dokumen yang masih draft, sudah terposting,
 * dan sudah terkunci closing, per jenis jurnal, pada satu rentang tanggal.
 *
 * <p><b>Kenapa terpisah dari layar.</b> Angka ini semula hanya hidup di dalam composer ZK
 * {@code DrafJurnalAction} dan menulis langsung ke {@code Rows} ZK, sehingga kanal lain (POS
 * Desktop/Android lewat API) tidak punya cara memakainya selain menyalin kriterianya. Dua salinan
 * kriteria untuk pertanyaan "berapa jurnal yang belum diposting" adalah cara tercepat membuat dua
 * kanal menjawab beda pada hal yang paling tidak boleh berbeda. Kelas ini bebas ZK: masukannya
 * Session + rentang tanggal, keluarannya data.</p>
 *
 * <p><b>Yang TIDAK dipindahkan ke sini</b>: URL jendela rincian dan perenderan grid -- keduanya
 * khas ZK dan tetap tinggal di composer-nya. Kunci modul ({@link #KUNCI}) menjadi jembatannya.</p>
 *
 * <p><b>STATUS PERALIHAN (baca sebelum mengubah kriteria).</b> Isi kelas ini adalah PORT PERSIS
 * dari penghitung di {@code DrafJurnalAction} -- kriteria, urutan, dan kalimat uraiannya sama.
 * Untuk sementara layar ZK MASIH menjalankan salinannya sendiri; pengalihan layar itu ke kelas ini
 * dikerjakan terpisah supaya perubahan dasbor web bisa diuji sendiri. Selama masa peralihan,
 * perubahan kriteria WAJIB dilakukan di kedua tempat.</p>
 *
 * <p><b>Kegagalan satu modul tidak menjatuhkan dasbor.</b> Setiap penghitung menangkap
 * exception-nya sendiri dan mengembalikan 0, persis seperti perilaku layar ZK sebelumnya: dasbor
 * dengan satu baris bernilai 0 masih berguna, dasbor yang gagal total tidak.</p>
 */
public final class DraftJurnalRingkasanUtil {

    /** Satu baris dasbor: satu jenis jurnal berikut angka draft/terposting/closing-nya. */
    public static final class Baris {
        private final String kunci;
        private final String nama;
        private final String keterangan;
        private final int draft;
        private final int posting;
        private final int closing;

        public Baris(String kunci, String nama, String keterangan, int draft, int posting, int closing) {
            this.kunci = kunci;
            this.nama = nama;
            this.keterangan = keterangan;
            this.draft = draft;
            this.posting = posting;
            this.closing = closing;
        }

        public String getKunci() { return kunci; }

        public String getNama() { return nama; }

        public String getKeterangan() { return keterangan; }

        public int getDraft() { return draft; }

        public int getPosting() { return posting; }

        public int getClosing() { return closing; }
    }

    /**
     * Kunci modul, dalam URUTAN TAMPIL dasbor. Satu kunci dapat menghasilkan lebih dari satu baris
     * (mis. {@code mahasiswa} dan {@code siswa}).
     */
    public static final String[] KUNCI = { "jurnal_umum", "uang_muka", "kas_kecil", "kas_besar", "pajak",
            "tagihan_vendor", "pekerjaan_vendor", "dp_vendor", "dp_pekerjaan_vendor", "jurnal_balik_dp_pekerjaan",
            "gaji", "mahasiswa", "siswa", "penyusutan", "pengajuan_transfer", "transitori", "closing",
            "posting_hpp" };

    private DraftJurnalRingkasanUtil() {
    }

    public static List<String> daftarKunci() {
        List<String> hasil = new ArrayList<String>();
        for (int i = 0; i < KUNCI.length; i++) {
            hasil.add(KUNCI[i]);
        }
        return hasil;
    }

    /** Seluruh baris dasbor, berurutan. Dipakai pemanggil yang tidak perlu paralel (API). */
    public static List<Baris> hitungSemua(Session session, Date mulai, Date sampai) {
        List<Baris> hasil = new ArrayList<Baris>();
        for (int i = 0; i < KUNCI.length; i++) {
            hasil.addAll(hitungModul(session, KUNCI[i], mulai, sampai));
        }
        return hasil;
    }

    /**
     * Baris untuk SATU modul. Dipisah begini supaya layar ZK tetap dapat menjalankan tiap modul di
     * thread-nya sendiri (dasbor ini memang lambat bila dijalankan berurutan) tanpa menduplikasi
     * kriterianya.
     */
    public static List<Baris> hitungModul(Session session, String kunci, Date mulai, Date sampai) {
        List<Baris> out = new ArrayList<Baris>();
        if ("jurnal_umum".equals(kunci)) {
            int closing = hitung(session, session.createCriteria(GrupTransaksi.class)
                    .setProjection(Projections.rowCount())
                    .add(Restrictions.eq("jenisJurnal", Transaksi.JURNAL_UMUM))
                    .add(Restrictions.sqlRestriction(dateSql("this_.tanggal_transaksi", mulai, sampai)))
                    .add(Restrictions.isNotNull("closing")));
            out.add(new Baris(kunci, "Jurnal Umum",
                    "Jurnal umum manual dipantau dari draft, terposting, sampai terkunci closing.",
                    hitungPostingHistory(session, kriteriaJurnalUmum(session, mulai, sampai), false),
                    hitungPostingHistory(session, kriteriaJurnalUmum(session, mulai, sampai), true), closing));
        } else if ("uang_muka".equals(kunci)) {
            out.add(new Baris(kunci, "Uang Muka",
                    "Pertanggungjawaban uang muka yang disetujui dipastikan menjadi jurnal.",
                    hitungPostingHistory(session, kriteriaLpj(session, mulai, sampai), false),
                    hitungPostingHistory(session, kriteriaLpj(session, mulai, sampai), true),
                    hitungClosing(session, "pertangungjawaban", null, null, mulai, sampai)));
        } else if ("kas_kecil".equals(kunci)) {
            out.add(new Baris(kunci, "Kas Kecil",
                    "Pengeluaran kas kecil yang disetujui dipantau sampai menjadi jurnal kas.",
                    hitungPostingHistory(session, kriteriaKasKecil(session, mulai, sampai), false),
                    hitungPostingHistory(session, kriteriaKasKecil(session, mulai, sampai), true),
                    hitungClosing(session, "kasKecil", null, null, mulai, sampai)));
        } else if ("kas_besar".equals(kunci)) {
            out.add(new Baris(kunci, "Kas Besar",
                    "Transaksi kas besar yang disetujui dipantau sampai menjadi jurnal kas/bank.",
                    hitungPostingHistory(session, kriteriaKasBesar(session, mulai, sampai), false),
                    hitungPostingHistory(session, kriteriaKasBesar(session, mulai, sampai), true),
                    hitungClosing(session, "kasBesar", null, null, mulai, sampai)));
        } else if ("pajak".equals(kunci)) {
            out.add(new Baris(kunci, "Pajak",
                    "Setoran pajak yang tercatat dipastikan sudah memiliki jurnal pajak.",
                    hitungPostingHistory(session, kriteriaPajak(session, mulai, sampai), false),
                    hitungPostingHistory(session, kriteriaPajak(session, mulai, sampai), true),
                    hitungClosing(session, "pajak", null, null, mulai, sampai)));
        } else if ("tagihan_vendor".equals(kunci)) {
            out.add(new Baris(kunci, "Penerimaan Tagihan Vendor",
                    "Tagihan vendor non termin yang disetujui dipantau sampai menjadi jurnal utang.",
                    hitungPostingHistory(session, kriteriaPenerimaanTagihanVendor(session, mulai, sampai), false),
                    hitungPostingHistory(session, kriteriaPenerimaanTagihanVendor(session, mulai, sampai), true),
                    hitungClosing(session, "saldoAwalMasterAsset", PostingJurnalHelper.REF_KOSONG, null, mulai,
                            sampai)));
        } else if ("pekerjaan_vendor".equals(kunci)) {
            out.add(new Baris(kunci, "Pekerjaan Vendor",
                    "Termin pekerjaan vendor yang diterima dipantau sampai menjadi jurnal.",
                    hitungPostingHistory(session, kriteriaPekerjaanVendor(session, mulai, sampai), false),
                    hitungPostingHistory(session, kriteriaPekerjaanVendor(session, mulai, sampai), true),
                    hitungClosing(session, "saldoAwalMasterAsset", PostingJurnalHelper.REF_PEKERJAAN_NON_DP, null,
                            mulai, sampai)));
        } else if ("dp_vendor".equals(kunci)) {
            out.add(new Baris(kunci, "DP Vendor",
                    "Uang muka pemesanan vendor dipantau sampai menjadi jurnal uang muka.",
                    hitungPostingHistory(session, kriteriaDpVendor(session, mulai, sampai), false),
                    hitungPostingHistory(session, kriteriaDpVendor(session, mulai, sampai), true),
                    hitungClosing(session, "pemesananPengadaanMasterAsset", PostingJurnalHelper.REF_KOSONG, null,
                            mulai, sampai)));
        } else if ("dp_pekerjaan_vendor".equals(kunci)) {
            out.add(new Baris(kunci, "DP Pekerjaan Vendor",
                    "Termin DP pekerjaan vendor dipisahkan agar jurnal uang muka tetap rapi.",
                    hitungPostingHistory(session, kriteriaDpPekerjaanVendor(session, mulai, sampai), false),
                    hitungPostingHistory(session, kriteriaDpPekerjaanVendor(session, mulai, sampai), true),
                    hitungClosing(session, "saldoAwalMasterAsset", PostingJurnalHelper.REF_DP_PEKERJAAN, null, mulai,
                            sampai)));
        } else if ("jurnal_balik_dp_pekerjaan".equals(kunci)) {
            out.add(new Baris(kunci, "Jurnal Balik DP Pekerjaan",
                    "Jurnal balik DP pekerjaan dipantau supaya uang muka tidak terhitung ganda.",
                    hitungPostingHistory(session, kriteriaJurnalBalikDpPekerjaan(session, mulai, sampai), false),
                    hitungPostingHistory(session, kriteriaJurnalBalikDpPekerjaan(session, mulai, sampai), true),
                    hitungClosing(session, "pemesananPengadaanMasterAsset",
                            PostingJurnalHelper.REF_DP_BALIK_PEKERJAAN, null, mulai, sampai)));
        } else if ("gaji".equals(kunci)) {
            out.add(new Baris(kunci, "Gaji",
                    "Pembayaran gaji yang disetujui dipastikan sudah menjadi jurnal beban gaji.",
                    hitungPostingHistory(session, kriteriaPembayaranGaji(session, mulai, sampai), false),
                    hitungPostingHistory(session, kriteriaPembayaranGaji(session, mulai, sampai), true),
                    hitungClosing(session, "pembayaranGaji", null, null, mulai, sampai)));
        } else if ("mahasiswa".equals(kunci)) {
            mahasiswa(out, session, mulai, sampai);
        } else if ("siswa".equals(kunci)) {
            siswa(out, session, mulai, sampai);
        } else if ("penyusutan".equals(kunci)) {
            penyusutan(out, session, mulai, sampai);
        } else if ("pengajuan_transfer".equals(kunci)) {
            out.add(new Baris(kunci, "Jurnal Pengajuan Transfer",
                    "Transfer yang sudah direalisasikan dapat dipastikan sudah menjadi jurnal kas/bank.",
                    countPengajuanTransfer(session, false, mulai, sampai),
                    countPengajuanTransfer(session, true, mulai, sampai),
                    hitungClosing(session, "daftarPengajuanTransfer", null, null, mulai, sampai)));
        } else if ("transitori".equals(kunci)) {
            out.add(new Baris(kunci, "Transitori",
                    "Transaksi perantara dapat dikawal sampai saldo berpindah ke akun tujuan.",
                    countTransitori(session, false, mulai, sampai), countTransitori(session, true, mulai, sampai),
                    hitungClosing(session, "transitori", null, null, mulai, sampai)));
        } else if ("closing".equals(kunci)) {
            int sudahClosing = countGrupTransaksiClosing(session, true, mulai, sampai);
            out.add(new Baris(kunci, "Closing",
                    "Jurnal yang belum dan sudah dikunci pada periode ini terlihat jelas sebelum periode ditutup.",
                    countGrupTransaksiClosing(session, false, mulai, sampai), sudahClosing, sudahClosing));
        } else if ("posting_hpp".equals(kunci)) {
            // Baris HPP hanya relevan bila tab "Posting HPP" diaktifkan admin -- dasbor yang tidak
            // memakai modul Kantin/HPP tidak perlu diganggu baris yang selalu nol.
            if (Common.bolehKonfigurasi(
                    ais.database.model.Konfigurasi.POSTING_JURNAL_TAB_PREFIX + "posting_hpp")) {
                out.add(new Baris(kunci, "Posting HPP",
                        "Beban pokok penjualan barang kantin yang tertaut persediaan aset -- diposting per periode "
                                + "(bukan per transaksi) dari tab \"Posting HPP\".",
                        ais.action.master.koperasi.PostingHppKantinAction.hitungDraftPending(session),
                        ais.action.master.koperasi.PostingHppKantinAction.hitungTerposting(session), 0));
            }
        }
        return out;
    }

    // ================================================================= kriteria per modul

    private static Criteria kriteriaJurnalUmum(Session session, Date mulai, Date sampai) {
        return session.createCriteria(GrupTransaksi.class)
                .add(Restrictions.eq("jenisJurnal", Transaksi.JURNAL_UMUM))
                .add(Restrictions.sqlRestriction(dateSql("this_.tanggal_transaksi", mulai, sampai)))
                .add(Restrictions.isNull("closing"));
    }

    /** disetujuiOleh ikut difilter, sama dengan PostingPertangungjawabanAction. */
    private static Criteria kriteriaLpj(Session session, Date mulai, Date sampai) {
        return session.createCriteria(Pertangungjawaban.class)
                .add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))
                .add(Restrictions.isNotNull("disetujuiOleh"))
                .add(Restrictions.sqlRestriction(dateSql("this_.tanggal_persetujuan", mulai, sampai)));
    }

    /** disetujuiOleh ikut difilter, sama dengan PostingKasKecilAction. */
    private static Criteria kriteriaKasKecil(Session session, Date mulai, Date sampai) {
        return session.createCriteria(KasKecil.class)
                .add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))
                .add(Restrictions.isNotNull("disetujuiOleh"))
                .add(Restrictions.sqlRestriction(dateSql("this_.tanggal_persetujuan", mulai, sampai)));
    }

    /** disetujuiOleh ikut difilter, sama dengan PostingKasBesarAction. */
    private static Criteria kriteriaKasBesar(Session session, Date mulai, Date sampai) {
        return session.createCriteria(KasBesar.class)
                .add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))
                .add(Restrictions.isNotNull("disetujuiOleh"))
                .add(Restrictions.sqlRestriction(dateSql("this_.tanggal_persetujuan", mulai, sampai)));
    }

    private static Criteria kriteriaPajak(Session session, Date mulai, Date sampai) {
        return session.createCriteria(Pajak.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))
                .add(Restrictions.sqlRestriction(dateSql("this_.tanggal_transaksi", mulai, sampai)));
    }

    private static Criteria kriteriaPenerimaanTagihanVendor(Session session, Date mulai, Date sampai) {
        return session.createCriteria(SaldoAwalMasterAsset.class)
                .add(Restrictions.isNull("jsonTermin")).add(Restrictions.isNotNull("disetujuiOleh"))
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))
                .add(Restrictions.sqlRestriction(dateSql("this_.tanggal_persetujuan", mulai, sampai)));
    }

    private static Criteria kriteriaPekerjaanVendor(Session session, Date mulai, Date sampai) {
        return session.createCriteria(SaldoAwalMasterAsset.class)
                .add(Restrictions.isNotNull("penerimaanPengadaanMasterAsset"))
                .add(Restrictions.isNotNull("jsonTermin")).add(Restrictions.isNotNull("disetujuiOleh"))
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))
                .add(Restrictions.sqlRestriction(dateSql("this_.tanggal_persetujuan", mulai, sampai)));
    }

    private static Criteria kriteriaDpVendor(Session session, Date mulai, Date sampai) {
        return session.createCriteria(PemesananPengadaanMasterAsset.class)
                .add(Restrictions.isNotNull("disetujuiOleh"))
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.ne("dp", 0.0)).add(Restrictions.isNotNull("dp"))
                .add(Restrictions.sqlRestriction(dateSql("this_.tanggal_persetujuan", mulai, sampai)));
    }

    private static Criteria kriteriaDpPekerjaanVendor(Session session, Date mulai, Date sampai) {
        return session.createCriteria(SaldoAwalMasterAsset.class)
                .add(Restrictions.isNotNull("penerimaanPengadaanMasterAsset"))
                .add(Restrictions.ilike("jsonTermin", "\"merupakan_dp\":true", MatchMode.ANYWHERE))
                .add(Restrictions.isNotNull("jsonTermin")).add(Restrictions.isNotNull("disetujuiOleh"))
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))
                .add(Restrictions.sqlRestriction(dateSql("this_.tanggal_persetujuan", mulai, sampai)));
    }

    private static Criteria kriteriaJurnalBalikDpPekerjaan(Session session, Date mulai, Date sampai) {
        return session.createCriteria(PemesananPengadaanMasterAsset.class)
                .add(Restrictions.isNotNull("disetujuiOleh"))
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.ilike("formula", "\"setuju\":true", MatchMode.ANYWHERE))
                .add(Restrictions.ilike("formula", "\"merupakan_dp\":true", MatchMode.ANYWHERE))
                .add(Restrictions.eq("byTermin", true))
                .add(Restrictions.sqlRestriction(dateSql("this_.tanggal_persetujuan", mulai, sampai)));
    }

    private static Criteria kriteriaPembayaranGaji(Session session, Date mulai, Date sampai) {
        return session.createCriteria(PembayaranGaji.class)
                .add(Restrictions.isNotNull("standingInstruction")).add(Restrictions.isNotNull("disetujuiOleh"))
                .add(Restrictions.sqlRestriction(dateSql("this_.waktubayar", mulai, sampai)));
    }

    // ================================================================= modul berbaris banyak

    private static void mahasiswa(List<Baris> out, Session session, Date mulai, Date sampai) {
        final String k = "mahasiswa";
        out.add(new Baris(k, "Mahasiswa - Piutang Tagihan",
                "Tagihan mahasiswa yang sudah muncul bisa dicek sebelum dibuat jurnal piutang.",
                countDetailKegiatan(session, false, mulai, sampai),
                countDetailKegiatan(session, true, mulai, sampai),
                hitungClosing(session, "detailKegiatan", null, null, mulai, sampai)));

        /* Filter nilai disamakan dengan PostingCicilanMahasiswaAction. */
        out.add(new Baris(k, "Mahasiswa - Pembayaran",
                "Pembayaran mahasiswa yang masuk dapat dipastikan sudah memiliki jurnal penerimaan.",
                countPostingByDateSql(session, CicilanPembayaran.class, "postingHistory", "this_.tanggal",
                        "this_.nilai is not null and this_.nilai <> 0", false, mulai, sampai),
                countPostingByDateSql(session, CicilanPembayaran.class, "postingHistory", "this_.tanggal",
                        "this_.nilai is not null and this_.nilai <> 0", true, mulai, sampai),
                hitungClosing(session, "cicilanPembayaran", PostingJurnalHelper.REF_KOSONG, null, mulai, sampai)));

        /* Filter nilai disamakan dengan PostingCicilanDibayarDimukaMahasiswaAction. */
        final String sqlDimuka = "date(this_.tanggal)<date(this_.tanggal_tagihan) and this_.nilai is not null "
                + "and this_.nilai <> 0";
        out.add(new Baris(k, "Mahasiswa - Dibayar Dimuka",
                "Pembayaran lebih awal dipisahkan agar pendapatan dan uang muka tidak tercampur.",
                countPostingByDateSql(session, CicilanPembayaran.class, "postingHistoryDimuka",
                        "this_.tanggal_tagihan", sqlDimuka, false, mulai, sampai),
                countPostingByDateSql(session, CicilanPembayaran.class, "postingHistoryDimuka",
                        "this_.tanggal_tagihan", sqlDimuka, true, mulai, sampai),
                hitungClosing(session, "cicilanPembayaran", PostingJurnalHelper.REF_DIMUKA, null, mulai, sampai)));

        out.add(new Baris(k, "Mahasiswa - Tabungan/Deposit",
                "Dana titipan mahasiswa terlihat terpisah dari pembayaran tagihan biasa.",
                countDepositMahasiswa(session, false, mulai, sampai),
                countDepositMahasiswa(session, true, mulai, sampai),
                hitungClosing(session, "deposit", null, null, mulai, sampai)));

        out.add(new Baris(k, "Mahasiswa - Pengeluaran/Refund",
                "Pengeluaran kepada mahasiswa dapat dipantau sebelum menjadi jurnal kas keluar.",
                countPengeluaranMahasiswa(session, false, mulai, sampai),
                countPengeluaranMahasiswa(session, true, mulai, sampai),
                hitungClosing(session, "pengeluaranMahasiswa", null, null, mulai, sampai)));

        out.add(new Baris(k, "Mahasiswa - Biaya Administrasi",
                "Biaya administrasi pembayaran dipisahkan agar pendapatan biaya layanan mudah diperiksa.",
                countLogPembayaran(session, "postingHistory", false, mulai, sampai),
                countLogPembayaran(session, "postingHistory", true, mulai, sampai),
                hitungClosing(session, "logPembayaran", null, null, mulai, sampai)));

        out.add(new Baris(k, "Mahasiswa - Biaya Payment Gateway",
                "Biaya dari kanal pembayaran online bisa dicek sebelum diposting ke akun biaya layanan.",
                countLogPembayaran(session, "postingHistoryPaymentGateway", false, mulai, sampai),
                countLogPembayaran(session, "postingHistoryPaymentGateway", true, mulai, sampai),
                hitungClosing(session, "logPembayaran", PostingJurnalHelper.REF_PAYMENT_GATEWAY, null, mulai,
                        sampai)));
    }

    private static void siswa(List<Baris> out, Session session, Date mulai, Date sampai) {
        final String k = "siswa";
        out.add(new Baris(k, "Siswa - Piutang Tagihan",
                "Tagihan siswa yang perlu dibuat jurnal piutang mudah dilihat dari satu tempat.",
                countTagihanSiswaPiutang(session, false, mulai, sampai),
                countTagihanSiswaPiutang(session, true, mulai, sampai),
                hitungClosing(session, "tagihan", null, "PIUTANG_SISWA", mulai, sampai)));

        out.add(new Baris(k, "Siswa - Pembayaran",
                "Pembayaran siswa yang masuk dapat dipastikan sudah berubah menjadi jurnal penerimaan.",
                countPembayaranSiswaDetail(session, false, mulai, sampai),
                countPembayaranSiswaDetail(session, true, mulai, sampai),
                hitungClosing(session, "pembayaranSiswaDetail", null, null, mulai, sampai)));

        out.add(new Baris(k, "Siswa - Dibayar Dimuka",
                "Pembayaran sebelum masa tagihan dipisahkan agar uang muka tetap rapi.",
                countTagihanSiswaDibayarDimuka(session, false, mulai, sampai),
                countTagihanSiswaDibayarDimuka(session, true, mulai, sampai),
                hitungClosing(session, "tagihan", null, "PEMBAYARAN_SISWA_DIBAYAR_DIMUKA", mulai, sampai)));

        out.add(new Baris(k, "Siswa - Deposit",
                "Deposit siswa dipantau terpisah dari pembayaran tagihan agar saldo titipan tetap jelas.",
                countPostingByDateSql(session, DepositSiswa.class, "postingHistory", "tanggal_bayar", null, false,
                        mulai, sampai),
                countPostingByDateSql(session, DepositSiswa.class, "postingHistory", "tanggal_bayar", null, true,
                        mulai, sampai),
                hitungClosing(session, "depositSiswa", null, null, mulai, sampai)));

        out.add(new Baris(k, "Siswa - Piutang Denda",
                "Denda yang sudah terbentuk dapat dipastikan masuk ke jurnal denda yang benar.",
                countTagihanSiswaPiutangDenda(session, false, mulai, sampai),
                countTagihanSiswaPiutangDenda(session, true, mulai, sampai),
                hitungClosing(session, "tagihan", null, "PIUTANG_DENDA_SISWA", mulai, sampai)));

        out.add(new Baris(k, "Siswa - Utang Diskon",
                "Diskon yang harus dibayar balik atau dicatat sebagai utang terlihat jelas sebelum diposting.",
                countTagihanSiswaUtangDiskon(session, false, mulai, sampai),
                countTagihanSiswaUtangDiskon(session, true, mulai, sampai),
                hitungClosing(session, "tagihan", null, "UTANG_DISKON_SISWA", mulai, sampai)));
    }

    private static void penyusutan(List<Baris> out, Session session, Date mulai, Date sampai) {
        final String k = "penyusutan";
        out.add(new Baris(k, "Fix Aset (Jurnal Saat BAST)",
                "Penerimaan (BAST) aset tetap dipantau sampai menjadi jurnal aset.",
                countPenerimaanBast(session, false, "fixasset", mulai, sampai),
                countPenerimaanBast(session, true, "fixasset", mulai, sampai),
                hitungClosing(session, "penerimaanPengadaanMasterAsset", null, null, mulai, sampai)));

        // Closing per-entitas penerimaan TIDAK bisa dipisah CIP -> ditampilkan di baris Fix Aset saja.
        out.add(new Baris(k, "Aset dalam Pekerjaan (Jurnal Saat BAST)",
                "BAST aset dalam pekerjaan (CIP) dipantau sampai menjadi jurnal.",
                countPenerimaanBast(session, false, "pekerjaan", mulai, sampai),
                countPenerimaanBast(session, true, "pekerjaan", mulai, sampai), 0));

        out.add(new Baris(k, "Jurnal Penyusutan",
                "Nilai penyusutan aset dapat dicek sebelum dicatat sebagai beban penyusutan.",
                countPenyusutan(session, false, mulai, sampai), countPenyusutan(session, true, mulai, sampai),
                hitungClosing(session, "penyusutanAsset", null, null, mulai, sampai)));
    }

    // ================================================================= kriteria dokumen

    /**
     * Kriteria DOKUMEN untuk satu baris dasbor: tanpa proyeksi hitung dan tanpa saringan status.
     *
     * <p>Ini sumber tunggal bagi dua kebutuhan yang gampang menyimpang: ANGKA di dasbor dan DAFTAR
     * dokumen saat angka itu diketuk. Bila keduanya dibangun terpisah, cepat atau lambat pengguna
     * mengetuk angka 7 dan menerima daftar berisi 5 baris -- dan yang salah menjadi mustahil
     * ditentukan. Penghitung di bawah memakai kriteria yang sama persis, hanya menambahkan
     * proyeksi rowCount.</p>
     *
     * @return null bila baris itu memang tidak punya daftar dokumen (mis. Posting HPP yang
     *         diposting per periode, bukan per dokumen).
     */
    public static Criteria kriteriaDokumen(Session session, String namaBaris, Date mulai, Date sampai) {
        if ("Jurnal Umum".equals(namaBaris)) return kriteriaJurnalUmum(session, mulai, sampai);
        if ("Uang Muka".equals(namaBaris)) return kriteriaLpj(session, mulai, sampai);
        if ("Kas Kecil".equals(namaBaris)) return kriteriaKasKecil(session, mulai, sampai);
        if ("Kas Besar".equals(namaBaris)) return kriteriaKasBesar(session, mulai, sampai);
        if ("Pajak".equals(namaBaris)) return kriteriaPajak(session, mulai, sampai);
        if ("Penerimaan Tagihan Vendor".equals(namaBaris)) {
            return kriteriaPenerimaanTagihanVendor(session, mulai, sampai);
        }
        if ("Pekerjaan Vendor".equals(namaBaris)) return kriteriaPekerjaanVendor(session, mulai, sampai);
        if ("DP Vendor".equals(namaBaris)) return kriteriaDpVendor(session, mulai, sampai);
        if ("DP Pekerjaan Vendor".equals(namaBaris)) return kriteriaDpPekerjaanVendor(session, mulai, sampai);
        if ("Jurnal Balik DP Pekerjaan".equals(namaBaris)) {
            return kriteriaJurnalBalikDpPekerjaan(session, mulai, sampai);
        }
        if ("Gaji".equals(namaBaris)) return kriteriaPembayaranGaji(session, mulai, sampai);

        if ("Mahasiswa - Piutang Tagihan".equals(namaBaris)) return kriteriaDetailKegiatan(session, mulai, sampai);
        if ("Mahasiswa - Pembayaran".equals(namaBaris)) {
            return kriteriaByDateSql(session, CicilanPembayaran.class, "this_.tanggal",
                    "this_.nilai is not null and this_.nilai <> 0", mulai, sampai);
        }
        if ("Mahasiswa - Dibayar Dimuka".equals(namaBaris)) {
            return kriteriaByDateSql(session, CicilanPembayaran.class, "this_.tanggal_tagihan", SQL_DIMUKA_MHS,
                    mulai, sampai);
        }
        if ("Mahasiswa - Tabungan/Deposit".equals(namaBaris)) {
            return kriteriaDepositMahasiswa(session, mulai, sampai);
        }
        if ("Mahasiswa - Pengeluaran/Refund".equals(namaBaris)) {
            return kriteriaPengeluaranMahasiswa(session, mulai, sampai);
        }
        if ("Mahasiswa - Biaya Administrasi".equals(namaBaris)) {
            return kriteriaLogPembayaran(session, "postingHistory", mulai, sampai);
        }
        if ("Mahasiswa - Biaya Payment Gateway".equals(namaBaris)) {
            return kriteriaLogPembayaran(session, "postingHistoryPaymentGateway", mulai, sampai);
        }

        if ("Siswa - Piutang Tagihan".equals(namaBaris)) return kriteriaTagihanSiswaPiutang(session, mulai, sampai);
        if ("Siswa - Pembayaran".equals(namaBaris)) return kriteriaPembayaranSiswaDetail(session, mulai, sampai);
        if ("Siswa - Dibayar Dimuka".equals(namaBaris)) {
            return kriteriaTagihanSiswaDibayarDimuka(session, mulai, sampai);
        }
        if ("Siswa - Deposit".equals(namaBaris)) {
            return kriteriaByDateSql(session, DepositSiswa.class, "tanggal_bayar", null, mulai, sampai);
        }
        if ("Siswa - Piutang Denda".equals(namaBaris)) {
            return kriteriaTagihanSiswaPiutangDenda(session, mulai, sampai);
        }
        if ("Siswa - Utang Diskon".equals(namaBaris)) {
            return kriteriaTagihanSiswaUtangDiskon(session, mulai, sampai);
        }

        if ("Fix Aset (Jurnal Saat BAST)".equals(namaBaris)) {
            return kriteriaPenerimaanBast(session, "fixasset", mulai, sampai);
        }
        if ("Aset dalam Pekerjaan (Jurnal Saat BAST)".equals(namaBaris)) {
            return kriteriaPenerimaanBast(session, "pekerjaan", mulai, sampai);
        }
        if ("Jurnal Penyusutan".equals(namaBaris)) return kriteriaPenyusutan(session, mulai, sampai);
        if ("Jurnal Pengajuan Transfer".equals(namaBaris)) {
            return kriteriaPengajuanTransfer(session, mulai, sampai);
        }
        if ("Transitori".equals(namaBaris)) return kriteriaTransitori(session, mulai, sampai);
        if ("Closing".equals(namaBaris)) return kriteriaGrupTransaksiSemua(session, mulai, sampai);
        // "Posting HPP" diposting per PERIODE, bukan per dokumen -- tidak ada daftar yang jujur
        // bisa ditampilkan di sini.
        return null;
    }

    /**
     * Properti posting history milik satu baris, atau null bila baris itu memakai pola JOIN
     * ({@code PostingJurnalHelper.terapkanStatusPostingHistory}) alih-alih restriksi properti.
     */
    private static String propertiPosting(String namaBaris) {
        if ("Mahasiswa - Dibayar Dimuka".equals(namaBaris)) return "postingHistoryDimuka";
        if ("Mahasiswa - Biaya Payment Gateway".equals(namaBaris)) return "postingHistoryPaymentGateway";
        if ("Siswa - Dibayar Dimuka".equals(namaBaris)) return "postingHistoryUangMuka";
        if ("Siswa - Piutang Denda".equals(namaBaris)) return "postingHistoryDenda";
        if ("Siswa - Utang Diskon".equals(namaBaris)) return "postingHistoryDiskon";
        if (namaBaris != null && (namaBaris.startsWith("Mahasiswa - ") || namaBaris.startsWith("Siswa - ")
                || namaBaris.startsWith("Fix Aset") || namaBaris.startsWith("Aset dalam Pekerjaan")
                || "Jurnal Penyusutan".equals(namaBaris) || "Jurnal Pengajuan Transfer".equals(namaBaris)
                || "Transitori".equals(namaBaris))) {
            return "postingHistory";
        }
        return null;
    }

    /** Saringan status pada kriteria dokumen: {@code draft}, {@code posting}, atau {@code closing}. */
    private static void terapkanStatus(Criteria criteria, String namaBaris, String status) {
        boolean sudahPosting = "posting".equals(status) || "closing".equals(status);
        if ("Jurnal Umum".equals(namaBaris) || "Closing".equals(namaBaris)) {
            // Dua baris ini berbicara tentang GrupTransaksi itu sendiri: statusnya dibaca dari
            // kolom closing, bukan dari posting history dokumen sumber.
            if ("closing".equals(status)) {
                criteria.add(Restrictions.isNotNull("closing"));
            } else if ("Closing".equals(namaBaris)) {
                criteria.add(Restrictions.isNull("closing"));
            } else {
                criteria.add(Restrictions.isNull("closing"));
                PostingJurnalHelper.terapkanStatusPostingHistory(criteria, "posting".equals(status));
            }
            return;
        }
        String properti = propertiPosting(namaBaris);
        if (properti == null) {
            PostingJurnalHelper.terapkanStatusPostingHistory(criteria, sudahPosting);
        } else {
            criteria.add(PostingJurnalHelper.restriksiPosting(properti, Boolean.valueOf(sudahPosting)));
        }
    }

    /** Satu dokumen pada daftar rincian: sengaja generik supaya satu tampilan melayani semua modul. */
    public static final class Dokumen {
        private final String id;
        private final String tanggal;
        private final String uraian;
        private final double nilai;

        public Dokumen(String id, String tanggal, String uraian, double nilai) {
            this.id = id;
            this.tanggal = tanggal;
            this.uraian = uraian;
            this.nilai = nilai;
        }

        public String getId() { return id; }

        public String getTanggal() { return tanggal; }

        public String getUraian() { return uraian; }

        public double getNilai() { return nilai; }
    }

    /**
     * Daftar dokumen di balik satu angka dasbor. Mengembalikan list kosong bila baris itu memang
     * tidak punya daftar dokumen; pemanggil membedakannya lewat {@link #punyaRincian(String)}.
     */
    public static List<Dokumen> rincian(Session session, String namaBaris, String status, Date mulai,
            Date sampai, int batas) {
        List<Dokumen> hasil = new ArrayList<Dokumen>();
        try {
            Criteria criteria = kriteriaDokumen(session, namaBaris, mulai, sampai);
            if (criteria == null) return hasil;
            terapkanStatus(criteria, namaBaris, status);
            criteria.setMaxResults(batas < 1 ? 100 : Math.min(500, batas));
            List<?> baris = criteria.list();
            for (int i = 0; i < baris.size(); i++) {
                hasil.add(petaDokumen(session, baris.get(i)));
            }
        } catch (Exception e) {
            gagal(session, e);
        }
        return hasil;
    }

    public static boolean punyaRincian(String namaBaris) {
        return !"Posting HPP".equals(namaBaris);
    }

    /**
     * Memetakan entity apa pun menjadi {@link Dokumen} lewat nama getter yang lazim dipakai model
     * AIS. Sengaja berbasis refleksi: dasbor ini menyentuh 20-an entity dari empat modul berbeda,
     * dan menulis pemeta khusus untuk masing-masing berarti 20 tempat yang harus diingat setiap
     * kali sebuah entity berubah. Yang dibutuhkan daftar rincian hanya tiga hal -- kapan, apa, dan
     * berapa -- sehingga pencarian berurutan atas nama-nama yang sudah baku sudah memadai.
     */
    private static Dokumen petaDokumen(Session session, Object entity) {
        String id = "";
        try {
            Object nilaiId = session.getSessionFactory()
                    .getClassMetadata(org.hibernate.proxy.HibernateProxyHelper.getClassWithoutInitializingProxy(entity))
                    .getIdentifier(entity, (org.hibernate.engine.SessionImplementor) session);
            if (nilaiId != null) id = String.valueOf(nilaiId);
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) DraftJurnalRingkasanUtil.petaDokumen-id");
        }
        Object tanggal = bacaPertama(entity, new String[] { "getTanggal", "getTanggalPersetujuan",
                "getTanggalTransaksi", "getTanggalTagihan", "getTanggalBayar", "getWaktu", "getWaktubayar",
                "getPertanggal", "getTanggalRealisasikan" });
        Object nilai = bacaPertama(entity, new String[] { "getNilai", "getNominal", "getBiaya", "getDp",
                "getNilaiPenyusutan", "getDenda", "getDiskonTidakLangsung", "getBiayaAdministrasi",
                "getBiayaPaymentGateway", "getTotal" });
        Object uraian = bacaPertama(entity, new String[] { "getKeterangan", "getUraian", "getKode", "getNama",
                "getNomor", "getNoBukti", "getDeskripsi" });
        String teksTanggal = "";
        if (tanggal instanceof Date) {
            teksTanggal = new java.text.SimpleDateFormat("yyyy-MM-dd").format((Date) tanggal);
        } else if (tanggal != null) {
            teksTanggal = String.valueOf(tanggal);
        }
        double angka = 0;
        if (nilai instanceof Number) angka = ((Number) nilai).doubleValue();
        return new Dokumen(id, teksTanggal, uraian == null ? "" : String.valueOf(uraian), angka);
    }

    private static Object bacaPertama(Object entity, String[] namaGetter) {
        for (int i = 0; i < namaGetter.length; i++) {
            try {
                java.lang.reflect.Method m = entity.getClass().getMethod(namaGetter[i], new Class[0]);
                Object v = m.invoke(entity, new Object[0]);
                if (v != null) return v;
            } catch (NoSuchMethodException e) {
                continue;
            } catch (Exception e) {
                continue;
            }
        }
        return null;
    }

    // ================================================================= penghitung

    /** EXISTS CIP: BAST punya detail-aset yang kelompoknya "pekerjaan dalam pelaksanaan" (CIP). */
    private static final String SQL_EXISTS_KELOMPOK_CIP = "exists (select 1 from asset.penerimaan_pengadaan_master_asset_detail d "
            + "join asset.master_asset m on d.masterasset = m.id "
            + "join asset.kelompok_asset k on m.kelompok_asset = k.id "
            + "where d.penerimaan_pengadaan_master_asset = this_.id "
            + "and coalesce(k.merupakanpekerjaandalampelaksanaan, false) = true)";

    private static final String SQL_DIMUKA_MHS = "date(this_.tanggal)<date(this_.tanggal_tagihan) "
            + "and this_.nilai is not null and this_.nilai <> 0";

    /**
     * Hitung dari kriteria dokumen: proyeksi rowCount ditambahkan DI SINI, bukan di pembangun
     * kriterianya, supaya kriteria yang sama dapat dipakai ulang untuk daftar rincian.
     */
    /**
     * Jumlah dokumen satu baris pada satu status -- memakai kriteria yang sama dengan daftar
     * rinciannya, sehingga angka dan daftar tidak mungkin berselisih.
     */
    public static int hitungStatus(Session session, String namaBaris, String status, Date mulai, Date sampai) {
        try {
            Criteria criteria = kriteriaDokumen(session, namaBaris, mulai, sampai);
            if (criteria == null) return 0;
            criteria.setProjection(Projections.rowCount());
            terapkanStatus(criteria, namaBaris, status);
            return PostingJurnalHelper.hitung(criteria);
        } catch (Exception e) {
            return gagal(session, e);
        }
    }

    private static Criteria kriteriaPenerimaanBast(Session session, String filterKelompok, Date mulai,
            Date sampai) {
        Criteria c = session.createCriteria(PenerimaanPengadaanMasterAsset.class)
                .createAlias("pemesananPengadaanMasterAsset", "pemesananPengadaanMasterAsset")
                .add(Restrictions.isNotNull("disetujuiOleh")).add(Restrictions.ne("nilai", 0.0))
                .add(Restrictions.isNotNull("nilai"))
                .add(Restrictions.sqlRestriction(dateSql("this_.tanggal_persetujuan", mulai, sampai)));
        if ("pekerjaan".equals(filterKelompok)) {
            c.add(Restrictions.sqlRestriction(SQL_EXISTS_KELOMPOK_CIP));
        } else if ("fixasset".equals(filterKelompok)) {
            c.add(Restrictions.sqlRestriction("not " + SQL_EXISTS_KELOMPOK_CIP));
        }
        return c;
    }

    private static int countPenerimaanBast(Session session, boolean posted, String filterKelompok, Date mulai,
            Date sampai) {
        try {
            Criteria c = kriteriaPenerimaanBast(session, filterKelompok, mulai, sampai)
                    .setProjection(Projections.rowCount())
                    .add(PostingJurnalHelper.restriksiPosting("postingHistory", Boolean.valueOf(posted)));
            return PostingJurnalHelper.hitung(c);
        } catch (Exception e) {
            return gagal(session, e);
        }
    }

    private static int hitungPostingHistory(Session session, Criteria criteria, boolean sudahPosting) {
        try {
            criteria.setProjection(Projections.rowCount());
            PostingJurnalHelper.terapkanStatusPostingHistory(criteria, sudahPosting);
            return PostingJurnalHelper.hitung(criteria);
        } catch (Exception e) {
            return gagal(session, e);
        }
    }

    private static int hitung(Session session, Criteria criteria) {
        try {
            return PostingJurnalHelper.hitung(criteria.setProjection(Projections.rowCount()));
        } catch (Exception e) {
            return gagal(session, e);
        }
    }

    private static int hitungClosing(Session session, String entitas, String ref, String jenis, Date mulai,
            Date sampai) {
        return PostingJurnalHelper.hitungClosing(session, entitas, ref, jenis, mulai, sampai);
    }

    @SuppressWarnings("rawtypes")
    private static Criteria kriteriaByDateSql(Session session, Class clazz, String dateColumn, String extraSql,
            Date mulai, Date sampai) {
        Criteria criteria = session.createCriteria(clazz)
                .add(Restrictions.sqlRestriction(dateSql(dateColumn, mulai, sampai)));
        if (extraSql != null && extraSql.trim().length() > 0) {
            criteria.add(Restrictions.sqlRestriction(extraSql));
        }
        return criteria;
    }

    @SuppressWarnings("rawtypes")
    private static int countPostingByDateSql(Session session, Class clazz, String postingField, String dateColumn,
            String extraSql, boolean posted, Date mulai, Date sampai) {
        try {
            Criteria criteria = kriteriaByDateSql(session, clazz, dateColumn, extraSql, mulai, sampai)
                    .setProjection(Projections.rowCount())
                    .add(PostingJurnalHelper.restriksiPosting(postingField, Boolean.valueOf(posted)));
            return PostingJurnalHelper.hitung(criteria);
        } catch (Exception e) {
            return gagal(session, e);
        }
    }

    private static Criteria kriteriaDetailKegiatan(Session session, Date mulai, Date sampai) {
        return session.createCriteria(DetailKegiatan.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.sqlRestriction("this_.item_biaya in (select item_biaya from item_biaya_punya_piutang where akun is not null and item_biaya is not null group by item_biaya)"))
                .add(Restrictions.isNotNull("tanggal"))
                .add(Restrictions.or(Restrictions.isNotNull("postingHistory"), Restrictions.ne("biaya", 0.0)))
                .add(Restrictions.or(Restrictions.isNotNull("postingHistory"), Restrictions.isNotNull("biaya")))
                .add(Restrictions.sqlRestriction(dateSql("this_.tanggal", mulai, sampai)));
    }

    private static int countDetailKegiatan(Session session, boolean posted, Date mulai, Date sampai) {
        try {
            Criteria criteria = kriteriaDetailKegiatan(session, mulai, sampai)
                    .setProjection(Projections.rowCount())
                    .add(PostingJurnalHelper.restriksiPosting("postingHistory", Boolean.valueOf(posted)));
            return PostingJurnalHelper.hitung(criteria);
        } catch (Exception e) {
            return gagal(session, e);
        }
    }

    private static Criteria kriteriaDepositMahasiswa(Session session, Date mulai, Date sampai) {
        return session.createCriteria(Deposit.class)
                .add(Restrictions.or(Restrictions.isNotNull("mahasiswa"),
                        Restrictions.isNotNull("biodataCalonMahasiswa")))
                .add(Restrictions.isNotNull("waktu"))
                .add(Restrictions.or(Restrictions.isNotNull("postingHistory"), Restrictions.ne("nominal", 0.0)))
                .add(Restrictions.or(Restrictions.isNotNull("postingHistory"), Restrictions.isNotNull("nominal")))
                .add(Restrictions.sqlRestriction(dateSql("this_.waktu", mulai, sampai)));
    }

    private static int countDepositMahasiswa(Session session, boolean posted, Date mulai, Date sampai) {
        try {
            Criteria criteria = kriteriaDepositMahasiswa(session, mulai, sampai)
                    .setProjection(Projections.rowCount())
                    .add(PostingJurnalHelper.restriksiPosting("postingHistory", Boolean.valueOf(posted)));
            return PostingJurnalHelper.hitung(criteria);
        } catch (Exception e) {
            return gagal(session, e);
        }
    }

    private static Criteria kriteriaPengeluaranMahasiswa(Session session, Date mulai, Date sampai) {
        return session.createCriteria(PengeluaranMahasiswa.class).add(Restrictions.isNotNull("waktu"))
                .add(Restrictions.or(Restrictions.isNotNull("postingHistory"), Restrictions.ne("nominal", 0.0)))
                .add(Restrictions.or(Restrictions.isNotNull("postingHistory"), Restrictions.isNotNull("nominal")))
                .add(Restrictions.sqlRestriction(dateSql("this_.waktu", mulai, sampai)));
    }

    private static int countPengeluaranMahasiswa(Session session, boolean posted, Date mulai, Date sampai) {
        try {
            Criteria criteria = kriteriaPengeluaranMahasiswa(session, mulai, sampai)
                    .setProjection(Projections.rowCount())
                    .add(PostingJurnalHelper.restriksiPosting("postingHistory", Boolean.valueOf(posted)));
            return PostingJurnalHelper.hitung(criteria);
        } catch (Exception e) {
            return gagal(session, e);
        }
    }

    private static Criteria kriteriaLogPembayaran(Session session, String postingField, Date mulai, Date sampai) {
        Criteria criteria = session.createCriteria(LogPembayaran.class)
                .add(Restrictions.sqlRestriction(dateSql("this_.tanggal", mulai, sampai)));
        if ("postingHistoryPaymentGateway".equals(postingField)) {
            criteria.add(Restrictions.gt("biayaPaymentGateway", 0.1));
        } else {
            criteria.add(Restrictions.gt("biayaAdministrasi", 0.1));
        }
        return criteria;
    }

    private static int countLogPembayaran(Session session, String postingField, boolean posted, Date mulai,
            Date sampai) {
        try {
            Criteria criteria = kriteriaLogPembayaran(session, postingField, mulai, sampai)
                    .setProjection(Projections.rowCount())
                    .add(PostingJurnalHelper.restriksiPosting(postingField, Boolean.valueOf(posted)));
            return PostingJurnalHelper.hitung(criteria);
        } catch (Exception e) {
            return gagal(session, e);
        }
    }

    private static Criteria kriteriaPembayaranSiswaDetail(Session session, Date mulai, Date sampai) {
        return session.createCriteria(PembayaranSiswaDetail.class)
                .createAlias("pembayaranSiswa", "pembayaranSiswa").createAlias("tagihan", "tagihan")
                .add(Restrictions.between("pembayaranSiswa.tanggalBayar", mulai, sampai));
    }

    private static int countPembayaranSiswaDetail(Session session, boolean posted, Date mulai, Date sampai) {
        try {
            Criteria criteria = kriteriaPembayaranSiswaDetail(session, mulai, sampai)
                    .setProjection(Projections.rowCount())
                    .add(PostingJurnalHelper.restriksiPosting("postingHistory", Boolean.valueOf(posted)));
            return PostingJurnalHelper.hitung(criteria);
        } catch (Exception e) {
            return gagal(session, e);
        }
    }

    private static Criteria kriteriaTagihanSiswaPiutang(Session session, Date mulai, Date sampai) {
        return session.createCriteria(Tagihan.class).createAlias("itemBiayaSekolah", "itemBiayaSekolah")
                .add(Restrictions.eq("itemBiayaSekolah.aktif", true))
                .add(Restrictions.isNotNull("itemBiayaSekolah.akunPiutang"))
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .createAlias("nominalBiaya", "nominalBiaya")
                .createAlias("nominalBiaya.pengaturanBiaya", "pengaturanBiaya")
                .createAlias("pengaturanBiaya.jenisBiayaSekolah", "jenisBiayaSekolah")
                .add(Restrictions.or(Restrictions.eq("jenisBiayaSekolah.periode", "Bulanan"),
                        Restrictions.and(Restrictions.eq("bayarKe", 1),
                                Restrictions.eq("jenisBiayaSekolah.periode", "Insidentil"))))
                .add(Restrictions.sqlRestriction(dateSql("tanggal_tagihan", mulai, sampai)));
    }

    private static int countTagihanSiswaPiutang(Session session, boolean posted, Date mulai, Date sampai) {
        try {
            Criteria criteria = kriteriaTagihanSiswaPiutang(session, mulai, sampai)
                    .setProjection(Projections.rowCount())
                    .add(PostingJurnalHelper.restriksiPosting("postingHistory", Boolean.valueOf(posted)));
            return PostingJurnalHelper.hitung(criteria);
        } catch (Exception e) {
            return gagal(session, e);
        }
    }

    private static Criteria kriteriaTagihanSiswaDibayarDimuka(Session session, Date mulai, Date sampai) {
        return session.createCriteria(Tagihan.class)
                .createAlias("pembayaranSiswaDetail", "pembayaranSiswaDetail")
                .createAlias("pembayaranSiswaDetail.pembayaranSiswa", "pembayaranSiswa")
                .add(Restrictions.gt("pembayaranSiswaDetail.nominal", 0.1))
                .add(Restrictions.sqlRestriction("date(tanggal_bayar)<date(tanggal_tagihan)"))
                .add(Restrictions.sqlRestriction(dateSql("tanggal_bayar", mulai, sampai)));
    }

    private static int countTagihanSiswaDibayarDimuka(Session session, boolean posted, Date mulai, Date sampai) {
        try {
            Criteria criteria = kriteriaTagihanSiswaDibayarDimuka(session, mulai, sampai)
                    .setProjection(Projections.rowCount())
                    .add(PostingJurnalHelper.restriksiPosting("postingHistoryUangMuka", Boolean.valueOf(posted)));
            return PostingJurnalHelper.hitung(criteria);
        } catch (Exception e) {
            return gagal(session, e);
        }
    }

    private static Criteria kriteriaTagihanSiswaPiutangDenda(Session session, Date mulai, Date sampai) {
        return session.createCriteria(Tagihan.class).add(Restrictions.isNotNull("tanggalDeadline"))
                .add(Restrictions.gt("denda", 0.1))
                .add(Restrictions.sqlRestriction(dateSql("tanggal_tagihan", mulai, sampai)));
    }

    private static int countTagihanSiswaPiutangDenda(Session session, boolean posted, Date mulai, Date sampai) {
        try {
            Criteria criteria = kriteriaTagihanSiswaPiutangDenda(session, mulai, sampai)
                    .setProjection(Projections.rowCount())
                    .add(PostingJurnalHelper.restriksiPosting("postingHistoryDenda", Boolean.valueOf(posted)));
            return PostingJurnalHelper.hitung(criteria);
        } catch (Exception e) {
            return gagal(session, e);
        }
    }

    private static Criteria kriteriaTagihanSiswaUtangDiskon(Session session, Date mulai, Date sampai) {
        return session.createCriteria(Tagihan.class).createAlias("diskonSiswa", "diskonSiswa")
                .add(Restrictions.eq("diskonSiswa.memotongTagihan", false))
                .add(Restrictions.isNotNull("tanggalBayar")).add(Restrictions.gt("diskonTidakLangsung", 0.1))
                .add(Restrictions.sqlRestriction(dateSql("this_.tanggalbayar", mulai, sampai)));
    }

    private static int countTagihanSiswaUtangDiskon(Session session, boolean posted, Date mulai, Date sampai) {
        try {
            Criteria criteria = kriteriaTagihanSiswaUtangDiskon(session, mulai, sampai)
                    .setProjection(Projections.rowCount())
                    .add(PostingJurnalHelper.restriksiPosting("postingHistoryDiskon", Boolean.valueOf(posted)));
            return PostingJurnalHelper.hitung(criteria);
        } catch (Exception e) {
            return gagal(session, e);
        }
    }

    private static Criteria kriteriaPenyusutan(Session session, Date mulai, Date sampai) {
        return session.createCriteria(PenyusutanAsset.class).add(Restrictions.ne("nilaiPenyusutan", 0.0))
                .add(Restrictions.isNotNull("nilaiPenyusutan"))
                .add(Restrictions.sqlRestriction(dateSql("this_.pertanggal", mulai, sampai)));
    }

    private static int countPenyusutan(Session session, boolean posted, Date mulai, Date sampai) {
        try {
            Criteria criteria = kriteriaPenyusutan(session, mulai, sampai)
                    .setProjection(Projections.rowCount())
                    .add(PostingJurnalHelper.restriksiPosting("postingHistory", Boolean.valueOf(posted)));
            return PostingJurnalHelper.hitung(criteria);
        } catch (Exception e) {
            return gagal(session, e);
        }
    }

    private static Criteria kriteriaPengajuanTransfer(Session session, Date mulai, Date sampai) {
        return session.createCriteria(DaftarPengajuanTransfer.class)
                .createAlias("disposisiSop", "disposisiSop", Criteria.LEFT_JOIN)
                .createAlias("prosesTransfer", "prosesTransfer")
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
                        Restrictions.eq("disposisiSop.aktif", true)))
                .add(Restrictions.isNotNull("prosesTransfer.realisasikanOleh"))
                .add(Restrictions.isNotNull("prosesTransfer.disetujuiOleh"))
                .add(Restrictions.ne("nominal", 0.0)).add(Restrictions.isNotNull("nominal"))
                .add(Restrictions.between("prosesTransfer.tanggalRealisasikan", mulai, sampai));
    }

    private static int countPengajuanTransfer(Session session, boolean posted, Date mulai, Date sampai) {
        try {
            Criteria criteria = kriteriaPengajuanTransfer(session, mulai, sampai)
                    .setProjection(Projections.rowCount())
                    .add(PostingJurnalHelper.restriksiPosting("postingHistory", Boolean.valueOf(posted)));
            return PostingJurnalHelper.hitung(criteria);
        } catch (Exception e) {
            return gagal(session, e);
        }
    }

    private static Criteria kriteriaTransitori(Session session, Date mulai, Date sampai) {
        return session.createCriteria(Transitori.class).createAlias("prosesTransitori", "prosesTransitori")
                .createAlias("daftarPengajuanTransfer", "daftarPengajuanTransfer", Criteria.LEFT_JOIN)
                .add(Restrictions.isNotNull("prosesTransitori.disetujuiOleh"))
                .add(Restrictions.ne("daftarPengajuanTransfer.nominal", 0.0))
                .add(Restrictions.isNotNull("daftarPengajuanTransfer.nominal"))
                .add(Restrictions.between("prosesTransitori.tanggalPersetujuan", mulai, sampai))
                .add(Restrictions.eq("transfer", true));
    }

    private static int countTransitori(Session session, boolean posted, Date mulai, Date sampai) {
        try {
            Criteria criteria = kriteriaTransitori(session, mulai, sampai)
                    .setProjection(Projections.rowCount())
                    .add(PostingJurnalHelper.restriksiPosting("postingHistory", Boolean.valueOf(posted)));
            return PostingJurnalHelper.hitung(criteria);
        } catch (Exception e) {
            return gagal(session, e);
        }
    }

    private static Criteria kriteriaGrupTransaksiSemua(Session session, Date mulai, Date sampai) {
        return session.createCriteria(GrupTransaksi.class)
                .add(Restrictions.sqlRestriction(dateSql("this_.tanggal_transaksi", mulai, sampai)));
    }

    private static int countGrupTransaksiClosing(Session session, boolean sudahClosing, Date mulai, Date sampai) {
        try {
            Criteria criteria = kriteriaGrupTransaksiSemua(session, mulai, sampai)
                    .setProjection(Projections.rowCount());
            criteria.add(sudahClosing ? Restrictions.isNotNull("closing") : Restrictions.isNull("closing"));
            return PostingJurnalHelper.hitung(criteria);
        } catch (Exception e) {
            return gagal(session, e);
        }
    }

    private static String dateSql(String kolom, Date mulai, Date sampai) {
        return PostingJurnalHelper.dateSql(kolom, mulai, sampai);
    }

    /**
     * Satu penghitung gagal: transaksi dibatalkan supaya session tetap bisa dipakai penghitung
     * berikutnya, kegagalannya dicatat, dan angkanya dilaporkan 0 -- sama seperti perilaku layar ZK.
     */
    private static int gagal(Session session, Exception e) {
        try {
            if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
        } catch (Exception ig) {
            ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) DraftJurnalRingkasanUtil.gagal");
        }
        ais.common.ErrorAuditUtil.record(e, "auto-audit DraftJurnalRingkasanUtil");
        return 0;
    }
}
