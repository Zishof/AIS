package ais.common.newui.menu;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Keputusan pemetaan route yang tidak dapat disimpulkan dari nama.
 *
 * <h3>Mengapa perlu</h3>
 * <p>{@link NewUiNativeJspResolver} mencari halaman menurut nama: nama URL,
 * lalu nama kelas composer, lalu alias laporan. Tiga lapisan itu gagal pada dua
 * keadaan yang tidak dapat diperbaiki dengan penamaan:</p>
 *
 * <ul>
 *   <li><b>Seri.</b> Dua halaman bernama sama di modul berbeda — misalnya
 *       {@code laporan_absensi_pegawai.jsp} yang ada di
 *       {@code format1/akademik} sekaligus {@code format1/payroll}. Resolver
 *       sengaja menolak keduanya daripada menebak, sehingga menunya dilaporkan
 *       tidak punya adaptor.</li>
 *   <li><b>Nama tidak sepadan.</b> URL menu menyebut sesuatu yang namanya
 *       memang berbeda dari halamannya — {@code chart_perencanaan_tiap_bulan}
 *       dilayani {@code laporan_perencanaan_tiap_bulan}, dan kelas SAPTO tanpa
 *       kode dilayani halaman yang memakai kodenya.</li>
 * </ul>
 *
 * <h3>Setiap entri adalah keputusan, bukan tebakan</h3>
 * <p>Menebak salah satu dari dua halaman yang seri menghasilkan layar yang
 * tampak wajar namun menampilkan data yang salah, tanpa gejala apa pun. Karena
 * itu tiap entri di bawah disertai dasar buktinya, dan dasar itu berasal dari
 * data — cabang induk pada pohon menu, atau kode laporan pada label menu —
 * bukan dari kemiripan nama.</p>
 *
 * <p>Tabel ini dikonsultasikan <b>lebih dulu</b> daripada ketiga lapisan
 * pencarian. Keputusan yang sudah ditetapkan manusia tidak boleh dikalahkan
 * oleh penskoran nama. Sebagai imbangannya,
 * {@code NewUiRuteEksplisitRegistrySelfTest} memastikan setiap entri benar-benar
 * menunjuk halaman yang ada — entri yang salah ketik atau usang menggagalkan
 * uji, bukan mengarahkan pengguna ke halaman 404.</p>
 */
public final class NewUiRuteEksplisitRegistry {

    /** Modul dan halaman tujuan sebuah route. */
    public static final class Tujuan {
        private final String module;
        private final String page;
        private final String alasan;

        Tujuan(String module, String page, String alasan) {
            this.module = module; this.page = page; this.alasan = alasan;
        }
        public String getModule() { return module; }
        public String getPage() { return page; }
        /** Dasar keputusan; dipakai dokumentasi dan uji. */
        public String getAlasan() { return alasan; }
    }

    private static final Map<String, Tujuan> RUTE;

    static {
        Map<String, Tujuan> m = new HashMap<String, Tujuan>();

        // --- Seri antara laporan akademik dan payroll ---------------------
        // laporan_absensi_pegawai.jsp ada di format1/akademik dan
        // format1/payroll, jadi resolver menolak keduanya. Cabang induk kedua
        // menu ini adalah "Laporan Payroll" (menu 77723128), sehingga varian
        // payroll-lah yang dimaksud. Dasarnya pohon menu, bukan kemiripan nama.
        m.put("sirs.action.report.employ.LaporanAbsensiPegawai",
                new Tujuan("root/report", "format1/payroll/laporan_absensi_pegawai",
                        "seri akademik/payroll; induk = Laporan Payroll (77723128)"));
        m.put("sirs.action.report.employ.LaporanAbsensiPegawaiPerOrang",
                new Tujuan("root/report", "format1/payroll/laporan_absensi_pegawai_per_orang",
                        "seri akademik/payroll; induk = Laporan Payroll (77723128)"));

        // --- SAPTO: halaman memakai kode laporan, URL tidak ----------------
        // Kelas pada URL tidak menyebut kode, sedangkan halamannya justru
        // dinamai menurut kode itu. Kodenya ada pada label menu, jadi
        // pemasangannya terbaca langsung dari data menu.
        m.put("ais.action.master.sapto.LaporanProfileMahasiswaDanLulusan",
                new Tujuan("sapto", "laporan_profile_mahasiswa_dan_lulusan_a_3_1_1",
                        "label menu 92234 = \"A-3.1.1 Profil Mahasiswa dan Lulusan\""));
        m.put("ais.action.master.sapto.LaporanProfileMahasiswa",
                new Tujuan("sapto", "laporan_profile_mahasiswa_a_3_1_5",
                        "label menu 322124 = \"A-3.1.5 Profil Mahasiswa\""));

        // --- Grafik RAB: ZUL tanpa apply=, dan namanya berbeda -------------
        // ZUL-nya ada tetapi tidak memasang composer apa pun (hanya MyWindow),
        // sehingga fallback composer tidak menghasilkan nama kelas. Halamannya
        // memakai awalan "laporan_", bukan "chart_"; masing-masing hanya punya
        // satu kandidat sehingga tidak ada keraguan.
        m.put("/pages/master/rab/chart_perencanaan_tiap_bulan.zul",
                new Tujuan("root/report", "format1/rab/laporan_perencanaan_tiap_bulan",
                        "ZUL tanpa apply=; kandidat tunggal berawalan laporan_"));
        m.put("/pages/master/rab/chart_realisasi_tiap_bulan.zul",
                new Tujuan("root/report", "format1/rab/laporan_realisasi_tiap_bulan",
                        "ZUL tanpa apply=; kandidat tunggal berawalan laporan_"));

        // --- Menu SIRS yang URL-nya menyisipkan segmen sirs/ ---------------
        // Empat menu SIRS menunjuk berkas yang tidak pernah ada: URL-nya sama
        // dengan varian non-SIRS, hanya disisipi segmen "sirs/". Akibatnya
        // keempatnya rusak di ZK lama maupun di New UI.
        //
        // Keempatnya sempat ditahan dengan alasan layar tujuannya "bercabang
        // menurut id menu sehingga perilakunya belum terverifikasi". Alasan itu
        // KELIRU, dan sudah diperiksa: GajiTabahanAction maupun
        // KonfigurasiAction tidak membaca id menu sama sekali. Pada data menu
        // pun dua menu non-SIRS -- "Variable Penggajian" (940223) dan "Variable
        // Pembayaran Absensi" (3747) -- sudah menunjuk satu ZUL yang sama tanpa
        // pembeda apa pun. Jadi memetakan menu SIRS ke halaman yang sama tidak
        // menghadirkan perilaku baru; ia memulihkan menu yang selama ini mati.
        //
        // Perbaikan yang lebih tepat tetap membetulkan kolom url pada data
        // menu. Pemetaan ini memulihkannya tanpa migrasi, dan uji mandiri
        // memastikan halaman tujuannya benar-benar ada.
        m.put("/pages/master/sirs/konfigurasi_detail.zul",
                new Tujuan("root", "konfigurasi",
                        "URL menyisipkan sirs/; KonfigurasiAction tidak membaca id menu"));
        m.put("/pages/master/sirs/payroll/absensi_kehadiran_pegawai_harian.zul",
                new Tujuan("root", "absens_kehadiran_pegawai_harian",
                        "URL menyisipkan sirs/; kedua varian ZUL memakai "
                        + "AbsensKehadiranPegawaiHarianAction yang sama"));
        m.put("/pages/master/sirs/payroll/gaji_tambahan.zul",
                new Tujuan("payroll", "gaji_tabahan",
                        "URL menyisipkan sirs/; melayani menu 7771361223 dan 77713612213, "
                        + "sebagaimana 940223 dan 3747 pun berbagi satu ZUL"));

        RUTE = Collections.unmodifiableMap(m);
    }

    private NewUiRuteEksplisitRegistry() { }

    /**
     * Tujuan eksplisit sebuah route.
     *
     * @param existingRoute isi kolom {@code url} milik menu
     * @return tujuan, atau {@code null} bila route ini tidak diputuskan manual
     */
    public static Tujuan cari(String existingRoute) {
        if (existingRoute == null) return null;
        return RUTE.get(existingRoute.trim());
    }

    /** Seluruh entri; dipakai uji mandiri. */
    public static Map<String, Tujuan> semua() {
        return RUTE;
    }
}
