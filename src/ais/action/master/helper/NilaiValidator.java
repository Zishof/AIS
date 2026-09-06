package ais.action.master.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.Messagebox;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BlokirMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.ui.util.MyMessageboxConfig;

/**
 * Validator terpusat untuk pertanyaan "bolehkah mahasiswa ini melihat/mencetak nilainya sekarang?"
 * — dipakai oleh layar KHS dan API laporan nilai. Menyediakan empat overload
 * {@code checkBolehLihatNilai} dengan jumlah parameter meningkat (semester, tampilkan messagebox
 * warning, kumpulan pesan peringatan opsional) yang seluruhnya bermuara pada satu implementasi
 * kanonik: {@link #checkBolehLihatNilai(Mahasiswa, int, boolean, List)}.
 *
 * <h2>Aturan yang diperiksa (berurutan, gagal di langkah pertama yang tidak lolos)</h2>
 * <ol>
 * <li>Mahasiswa yang sudah lulus pada semester sebelum semester yang diperiksa selalu lolos
 * (tidak ada pengecekan lebih lanjut).</li>
 * <li><b>Blokir</b> — bila ada {@link BlokirMahasiswa} aktif dengan flag {@code nilai=true} dan
 * keterangan tidak kosong, akses ditolak dan alasan blokir ditampilkan/dikumpulkan.</li>
 * <li><b>Pembayaran</b> — hanya diperiksa bila konfigurasi
 * {@code mhs_yg_belum_bayar_tidak_bisa_lihat_nilai} aktif (default AKTIF): persentase minimum
 * pembayaran semester lalu ({@code batas_terendah_persen_pembayaran_semester_yang_lalu_boleh_melihat_nilai})
 * dan semester saat ini ({@code batas_terendah_persen_pembayaran_semester_saat_ini_boleh_melihat_nilai})
 * harus terpenuhi, dicek lewat {@code Common#checkStatusPembayaranMahasiswaSebelumnyaUntukPenilaian}.</li>
 * </ol>
 *
 * <p>
 * <b>Catatan sejarah bug</b> — sebelumnya konfigurasi gerbang utama pembayaran salah dipetakan
 * sebagai argumen "tampilkan warning" saja, sehingga pengecekan pembayaran SELALU berjalan
 * terlepas dari status aktif/nonaktif konfigurasi tersebut, membuat KHS tampak kosong tanpa
 * penjelasan bagi mahasiswa yang seharusnya tidak digerbang. Sudah diperbaiki agar konfigurasi
 * benar-benar menjadi saklar on/off pengecekan pembayaran (bagian B pada method kanonik).
 * </p>
 */
public class NilaiValidator {

    /**
     * Varian paling ringkas: memakai semester berjalan mahasiswa ({@code mahasiswa.currentSemester()})
     * dan selalu menampilkan messagebox peringatan bila ditolak.
     *
     * @param mahasiswa mahasiswa yang akan diperiksa; {@code null} selalu mengembalikan {@code true}
     * @return {@code true} bila boleh melihat nilai, {@code false} bila ditolak
     */
    public static boolean checkBolehLihatNilai(Mahasiswa mahasiswa) {
        return (mahasiswa == null) ? true : checkBolehLihatNilai(mahasiswa, mahasiswa.currentSemester());
    }

    /**
     * Seperti {@link #checkBolehLihatNilai(Mahasiswa)}, dengan semester eksplisit. Selalu menampilkan
     * messagebox peringatan bila ditolak — dipakai jalur UI interaktif; pemanggil non-UI (mis. API
     * laporan) sebaiknya memakai overload 4-argumen dengan {@code tampilWarning=false}.
     *
     * @param mahasiswa mahasiswa yang akan diperiksa; {@code null} selalu mengembalikan {@code true}
     * @param semester  nomor semester yang nilainya akan dilihat
     * @return {@code true} bila boleh melihat nilai, {@code false} bila ditolak
     */
    public static boolean checkBolehLihatNilai(Mahasiswa mahasiswa, int semester) {
        return checkBolehLihatNilai(mahasiswa, semester, true);
    }

    /**
     * Seperti {@link #checkBolehLihatNilai(Mahasiswa, int)}, dengan kendali eksplisit apakah
     * messagebox peringatan ditampilkan ke pengguna.
     *
     * @param mahasiswa     mahasiswa yang akan diperiksa; {@code null} selalu mengembalikan {@code true}
     * @param semester      nomor semester yang nilainya akan dilihat
     * @param tampilWarning {@code true} untuk menampilkan {@link MyMessageboxConfig} saat ditolak
     * @return {@code true} bila boleh melihat nilai, {@code false} bila ditolak
     */
    public static boolean checkBolehLihatNilai(Mahasiswa mahasiswa, int semester, boolean tampilWarning) {
        return checkBolehLihatNilai(mahasiswa, semester, tampilWarning, null);
    }

    /**
     * Implementasi kanonik — satu-satunya tempat logika validasi berada (lihat dokumentasi kelas
     * untuk urutan aturan yang diperiksa: status lulus, blokir, lalu pembayaran).
     *
     * @param mahasiswa     mahasiswa yang akan diperiksa; {@code null} selalu mengembalikan {@code true}
     * @param semester      nomor semester yang nilainya akan dilihat
     * @param tampilWarning {@code true} untuk menampilkan {@link MyMessageboxConfig} saat ditolak
     * @param warnings      bila tidak {@code null}, setiap pesan penolakan ditambahkan ke daftar ini
     *                      (dipakai pemanggil non-UI untuk mengumpulkan pesan tanpa popup)
     * @return {@code true} bila boleh melihat nilai; {@code false} bila diblokir, belum memenuhi
     *         syarat pembayaran, atau terjadi kegagalan teknis saat pemeriksaan
     */
    @SuppressWarnings("unchecked")
    public static boolean checkBolehLihatNilai(Mahasiswa mahasiswa, int semester, boolean tampilWarning, List<String> warnings) {
        if (mahasiswa == null) return true;

        // Jika sudah lulus, skip semua pengecekan
        if (mahasiswa.getSemesterLulus() != null && mahasiswa.getSemesterLulus() < semester) {
            return true;
        }

        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();

            // --- A. Pengecekan Blokir ---
            List<String> alasans = session.createCriteria(BlokirMahasiswa.class)
                    .add(Restrictions.isNotNull("keterangan"))
                    .add(Restrictions.ne("keterangan", ""))
                    .add(Restrictions.eq("mahasiswa", mahasiswa))
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .add(Restrictions.eq("nilai", true))
                    .setProjection(Projections.property("keterangan"))
                    .list();

            if (!alasans.isEmpty()) {
                String alas = Common.join(alasans, "\n\n");
                handleWarning(alas, "Informasi Lihat Nilai", tampilWarning, warnings);
                return false;
            }

            // --- B. Pengecekan Pembayaran (hanya bila fitur ini diaktifkan admin) ---
            // FIX: sebelumnya dipanggil TANPA syarat (lihat catatan di entry point 2-argumen di
            // atas) -- kini benar-benar digerbangi oleh config "mhs_yg_belum_bayar_tidak_bisa_lihat_nilai"
            // sesuai maksud aslinya (default AKTIF, lihat KonfigurasiNewAction).
            if (Common.bolehKonfigurasi("mhs_yg_belum_bayar_tidak_bisa_lihat_nilai")) {
                return validatePembayaran(session, mahasiswa, semester, tampilWarning, warnings);
            }
            return true;

        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/NilaiValidator.java:70");
            // FIX (blank KHS): sebelumnya kegagalan (mis. query Hibernate bermasalah) balik false
            // TANPA pesan apa pun -- di sisi pemanggil (mis. LaporanKHS.onKHS) ini tampak sebagai
            // layar kosong tanpa penjelasan sama sekali. Beri tahu pengguna agar tidak bingung.
            handleWarning(
                    "Terjadi kendala saat memeriksa kelayakan melihat nilai mahasiswa ini. Laporan/nilai untuk "
                            + "sementara tidak dapat ditampilkan. Silakan coba lagi, atau hubungi admin bila berlanjut.",
                    "Peringatan", tampilWarning, warnings);
            return false;
        } finally {
            // Memastikan Session ditutup untuk mencegah memory leak
            if (session != null) {
                try { session.clear(); session.disconnect(); session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/NilaiValidator.java:75");}
            }
        }
    }

    /**
     * Memeriksa syarat minimum persentase pembayaran semester lalu dan semester saat ini
     * (dipanggil hanya bila konfigurasi {@code mhs_yg_belum_bayar_tidak_bisa_lihat_nilai} aktif —
     * lihat bagian B pada dokumentasi {@link #checkBolehLihatNilai(Mahasiswa, int, boolean, List)}).
     * Kedua batas ({@code harusLunasLalu}, {@code harusLunasSaatIni}) diambil dari konfigurasi lewat
     * {@link #parseDoubleConfig(String, double)}; batas bernilai &lt;= 0.1 melewatkan pengecekan
     * tersebut. Pesan penolakan menyebut semester atau tahap sesuai konfigurasi
     * {@link ais.common.ConstantValues#aktifkanTahapanTerhubungKeKeuangan}.
     *
     * @param session       Session Hibernate aktif untuk query status pembayaran
     * @param mahasiswa     mahasiswa yang diperiksa
     * @param semester      nomor semester yang nilainya akan dilihat
     * @param tampilWarning {@code true} untuk menampilkan {@link MyMessageboxConfig} saat ditolak
     * @param warnings      bila tidak {@code null}, pesan penolakan ditambahkan ke daftar ini
     * @return {@code true} bila kedua syarat pembayaran terpenuhi (atau dilewati karena batas 0)
     */
    // --- Helper Method untuk Pembayaran ---
    private static boolean validatePembayaran(Session session, Mahasiswa mahasiswa, int semester, boolean tampilWarning, List<String> warnings) {
        Integer tahap = mahasiswa.currentTahapan(semester);
        boolean termasukSmt1 = Common.bolehKonfigurasi("syarat_melihat_nilai_tidak_termasuk_smt_1");
        
        double harusLunasLalu = parseDoubleConfig("batas_terendah_persen_pembayaran_semester_yang_lalu_boleh_melihat_nilai", 90.0);
        double harusLunasSaatIni = parseDoubleConfig("batas_terendah_persen_pembayaran_semester_saat_ini_boleh_melihat_nilai", 0.0);

        // Validasi Smt Lalu
        if (harusLunasLalu > 0.1 && !Common.checkStatusPembayaranMahasiswaSebelumnyaUntukPenilaian(semester, tahap, mahasiswa, harusLunasLalu, termasukSmt1)) {
            String msg = constructMsg(harusLunasLalu, (ConstantValues.aktifkanTahapanTerhubungKeKeuangan && (tahap == null || tahap == 0)) ? "semester " + (semester - 1) : "tahap " + (tahap - 1));
            handleWarning(msg, "Peringatan", tampilWarning, warnings);
            return false;
        }

        // Validasi Smt Saat Ini
        if (harusLunasSaatIni > 0.1 && !Common.checkStatusPembayaranMahasiswaSebelumnyaUntukPenilaian(semester + 1, tahap, mahasiswa, harusLunasSaatIni, termasukSmt1)) {
            String msg = constructMsg(harusLunasSaatIni, (ConstantValues.aktifkanTahapanTerhubungKeKeuangan && (tahap == null || tahap == 0)) ? "semester " + semester : "tahap " + tahap);
            handleWarning(msg, "Peringatan", tampilWarning, warnings);
            return false;
        }

        return true;
    }

    /**
     * Menyalurkan satu pesan penolakan/peringatan ke messagebox (bila {@code tampilWarning}) dan/atau
     * ke daftar {@code warnings} (bila tidak {@code null}) — titik tunggal yang dipakai seluruh
     * jalur penolakan di kelas ini agar perilaku tampil/kumpul konsisten.
     *
     * @param msg           isi pesan yang ditampilkan/dikumpulkan
     * @param title         judul messagebox (diabaikan bila {@code tampilWarning} bernilai {@code false})
     * @param tampilWarning {@code true} untuk menampilkan {@link MyMessageboxConfig}
     * @param warnings      bila tidak {@code null}, {@code msg} ditambahkan ke daftar ini
     */
    // --- Private Utilities untuk Kerapihan ---
    private static void handleWarning(String msg, String title, boolean tampilWarning, List<String> warnings) {
        if (tampilWarning) {
            try { MyMessageboxConfig.show(msg, title, MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/NilaiValidator.java:108");}
        }
        if (warnings != null) warnings.add(msg);
    }

    /**
     * Menyusun teks pesan penolakan pembayaran standar, menyebutkan persentase minimum yang
     * disyaratkan dan periode (semester/tahap) yang belum terpenuhi.
     *
     * @param persen  persentase minimum pembayaran yang disyaratkan
     * @param periode label periode yang ditampilkan pada pesan (mis. "semester 3" atau "tahap 1")
     * @return teks pesan siap tampil ke pengguna
     */
    private static String constructMsg(double persen, String periode) {
        return "Untuk melihat nilai, Mahasiswa ini harus melunasi " + persen + "% biaya perkuliahan di " + periode + ". Harap hubungi bagian keuangan.";
    }

    /**
     * Membaca satu nilai konfigurasi numerik lewat {@link Common#getKonfigurasi(String, String)},
     * dengan {@code defaultValue} sebagai nilai default konfigurasi sekaligus nilai fallback bila
     * isi konfigurasi gagal di-parse sebagai {@code double}.
     *
     * @param key          kunci konfigurasi yang dibaca
     * @param defaultValue nilai default (dipakai saat auto-seed konfigurasi maupun saat parse gagal)
     * @return nilai konfigurasi ter-parse, atau {@code defaultValue} bila gagal
     */
    private static double parseDoubleConfig(String key, double defaultValue) {
        try {
            return Double.parseDouble(Common.getKonfigurasi(key, String.valueOf(defaultValue)).getNilai().trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}