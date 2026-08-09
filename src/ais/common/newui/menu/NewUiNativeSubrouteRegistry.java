package ais.common.newui.menu;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Allow-list subhalaman native untuk handler tab Action existing. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class NewUiNativeSubrouteRegistry {
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
