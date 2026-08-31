package ais.common.newui.menu;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Allow-list subhalaman native untuk handler tab Action existing. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class NewUiNativeSubrouteRegistry {
    /**
     * Tipe implementasi bersarang {@link Route} milik {@link NewUiNativeSubrouteRegistry}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiNativeSubrouteRegistry}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String module}, {@code String page};
     * operasi lokal: {@code getModule()}, {@code getPage()}, {@code target}(). Aturan bisnis bersama tetap berada
     * pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see NewUiNativeSubrouteRegistry
     */
    public static final class Route {
        private final String module;
        private final String page;
        Route(String module, String page) { this.module = module; this.page = page; }
        public String getModule() { return module; }
        public String getPage() { return page; }
        public String target(boolean service) {
            return "/WEB-INF/new/" + module + (service ? "/services/" : "/uiux/")
                    + page + (service ? "_service.jsp" : ".jsp");
        }
    }

    private static final Map MAHASISWA;
    static {
        Map routes = new LinkedHashMap();
        add(routes, "statistik", "dashboard", "admin/dashboard_mahasiswa");
        add(routes, "help", "root", "mahasiswa_bantuan");
        add(routes, "upload_password", "root", "mahasiswa_operasi");
        add(routes, "download_password", "root", "mahasiswa_operasi");
        add(routes, "upload_rfid", "root", "mahasiswa_operasi");
        add(routes, "download_rfid", "root", "mahasiswa_operasi");
        add(routes, "upload_photo_massal", "root", "mahasiswa_operasi");
        add(routes, "download_photo_massal", "root", "mahasiswa_operasi");
        add(routes, "upload_ukt", "root", "mahasiswa_operasi");
        add(routes, "upload_status", "root", "mahasiswa_operasi");
        add(routes, "sync_status", "root", "mahasiswa_operasi");
        add(routes, "import_data", "root", "mahasiswa_operasi");
        add(routes, "feeder", "root", "mahasiswa_operasi");
        add(routes, "ojs", "root", "mahasiswa_operasi");
        add(routes, "download_lampiran", "root", "mahasiswa_operasi");
        add(routes, "rekap", "root/report", "format1/akademik/laporan_rekap_jumlah_mahasiswa");
        add(routes, "surat", "root/report", "format1/akademik/laporan_surat");
        add(routes, "album", "root/report", "format1/akademik/laporan_album_mahasiswa_per_prodi_dan_angkatan");
        add(routes, "khs", "root/report", "format1/akademik/laporan_khs");
        add(routes, "transkrip", "root/report", "format1/akademik/laporan_transkip_akademik");
        add(routes, "prestasi_belajar", "root/report", "helper/nilai/laporan_daftar_prestasi_belajar");
        add(routes, "kartu", "root/report", "format1/akademik/laporan_kartu_mahasiswa");
        add(routes, "kegiatan_kemahasiswaan", "dashboard", "admin/dashboard_kegiatan_kemahasiswaan_admin");
        add(routes, "formulir", "root", "formulir_kegiatan");
        add(routes, "operator_seluler", "root", "operator_seluler");
        add(routes, "alat_transport", "root", "alat_transportasi_mahasiswa");
        add(routes, "jenis_tinggal", "root", "jenis_tinggal_mahasiswa");
        add(routes, "pekerjaan_ortu", "root", "pekerjaan");
        add(routes, "penghasilan_ortu", "root", "penghasilan");
        add(routes, "pendidikan_ortu", "root", "pendidikan_ortu");
        add(routes, "form_tambahan", "root", "parameter_tambahan_mahasiswa");
        add(routes, "perkuliahan", "helper", "manajemen_penjadwalan_mahasiswa");
        add(routes, "kelas", "root", "kelas");
        add(routes, "kelompok", "root", "kelompok_mahasiswa");
        add(routes, "status", "root", "kelompok_status_mahasiswa");
        add(routes, "program", "root", "program_mahasiswa");
        add(routes, "status_keluar", "root", "kelompok_status_keluar_mahasiswa");
        add(routes, "asrama", "root", "asrama");
        add(routes, "dosen_pa", "root", "dosen_pembimbing_akademik");
        add(routes, "export_data_lengkap", "root/report", "format1/akademik/laporan_format_emis");
        MAHASISWA = Collections.unmodifiableMap(routes);
    }

    private NewUiNativeSubrouteRegistry() { }

    public static Route resolve(NewUiHybridMenuNode selected, String key) {
        if (selected == null || key == null || !isMahasiswa(selected)) return null;
        return (Route) MAHASISWA.get(key.trim().toLowerCase());
    }

    public static boolean supportsMahasiswa(String key) {
        return key != null && MAHASISWA.containsKey(key.trim().toLowerCase());
    }

    private static boolean isMahasiswa(NewUiHybridMenuNode node) {
        String module = safe(node.getNewUiModule());
        String page = safe(node.getNewUiPage());
        String existing = safe(node.getExistingUrl()).toLowerCase();
        return ("root".equals(module) && "mahasiswa".equals(page))
                || existing.matches(".*(^|[/\\\\])mahasiswa\\.zul([?].*)?$");
    }

    private static void add(Map routes, String key, String module, String page) {
        routes.put(key, new Route(module, page));
    }
    private static String safe(String value) { return value == null ? "" : value; }
}
