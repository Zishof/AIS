package ais.common.newui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ais.database.model.Menu;

/** Registry eksplisit Menu.id/URL legacy yang telah diverifikasi ke route New UI. */
public final class NewUiRouteRegistry {

    public static final int MAPPED_AND_AUTHORIZED = 0;
    public static final int MAPPED_BUT_FORBIDDEN = 1;
    public static final int UNMAPPED = 2;
    public static final int NOT_FOUND = 3;

    public static final class Route {
        private final Long menuId;
        private final String exactLegacyUrl;
        private final String module;
        private final String page;

        Route(Long menuId, String exactLegacyUrl, String module, String page) {
            this.menuId = menuId;
            this.exactLegacyUrl = normalizeUrl(exactLegacyUrl);
            this.module = module;
            this.page = page;
        }

        public Long getMenuId() { return menuId; }
        public String getModule() { return module; }
        public String getPage() { return page; }
    }

    private static final List<Route> ROUTES = new ArrayList<Route>();
    private static final Map<Long, Route> BY_ID = new HashMap<Long, Route>();
    private static final Map<String, Route> BY_EXACT_URL = new HashMap<String, Route>();
    private static final Map<String, Route> BY_NEW_ROUTE = new HashMap<String, Route>();

    static {
        // ID dan URL diverifikasi terhadap DB AIS lokal pada 2026-08-08.
        register(2L, "/pages/maintenance/job/list.zul", "root/maintenance", "tbmrole");
        register(3L, "/pages/maintenance/users/list.zul", "root/maintenance", "tbmuser");
        register(6L, "/pages/master/mahasiswa.zul", "root", "mahasiswa");
        register(121122L, "/pages/master/kkn/kelompok_kkn.zul", "kkn", "kelompok_kkn");
        register(924987L, "/pages/master/kkn/kkn_utk_mhs.zul", "kkn", "kkn_untuk_mahasiswa");
        register(100000007L, "/pages/master/agama.zul", "root", "agama");

        /*
         * Keuangan siswa/mahasiswa.
         *
         * Semua binding di bawah menunjuk service JSON di WEB-INF/new. URL ZUL
         * hanya dipakai sebagai identitas exact dari Menu.java; URL tersebut
         * tidak pernah dikirim ke Flutter dan tidak pernah dirender sebagai
         * halaman. Query string pada dua menu pem_online sengaja menjadi bagian
         * binding agar menu Siswa dan Calon Siswa tidak tertukar.
         */
        register(1732L, "/pages/master/informasi_pembayaran_mahasiswa.zul", "root", "informasi_pembayaran_mahasiswa");
        register(34569L, "/pages/master/baypass_pembayaran_mahasiswa.zul", "root", "baypass_pembayaran_mahasiswa");
        register(645681L, "ais.action.report.format1.sekolah.LaporanDepositSiswa", "root/report", "format1/sekolah/laporan_deposit_siswa");
        register(673271L, "ais.action.report.format1.sekolah.LaporanPembayaranSiswa", "root/report", "format1/sekolah/laporan_pembayaran_siswa");
        register(673272L, "ais.action.report.format1.sekolah.LaporanRincianPembayaranSiswa", "root/report", "format1/sekolah/laporan_rincian_pembayaran_siswa");
        register(677681L, "ais.action.report.format1.sekolah.LaporanRincianPembayaranSiswa", "root/report", "format1/sekolah/laporan_rincian_pembayaran_siswa");
        register(6518793L, "/pages/master/sekolah/item_biaya_sekolah.zul", "sekolah", "item_biaya_sekolah");
        register(6518796L, "/pages/master/sekolah/pengaturan_biaya_sekolah.zul", "sekolah", "pengaturan_biaya");
        register(6769091L, "ais.action.report.format1.sekolah.LaporanPembayaranSiswa", "root/report", "format1/sekolah/laporan_pembayaran_siswa");
        register(8555572L, "/pages/master/sekolah/posting_pembayaran.zul", "sekolah", "posting_cicilan_siswa");
        register(8755592L, "/pages/master/sekolah/pem_online.zul?lbl_siswa=true", "sekolah", "pembayaran_siswa");
        register(8755593L, "/pages/master/sekolah/pem_online.zul?lbl_calon_siswa=true", "sekolah", "pembayaran_calon_siswa");
        register(11098051L, "/pages/master/sekolah/diskon_siswa.zul", "sekolah", "diskon_siswa");
        register(65181292L, "/pages/master/sekolah/akun_pembayaran_siswa.zul", "sekolah", "akun_pembayaran_siswa");
        register(87657727L, "/pages/master/sekolah/tagihan.zul", "sekolah", "tagihan");
        register(87657728L, "/pages/master/sekolah/pembayaran_siswa.zul", "sekolah", "pembayaran_siswa");
        register(100000017L, "/pages/master/daftarulang_mahasiswa_lama.zul", "root", "daftar_ulang_mahasiswa_lama");
        register(100000022L, "/pages/master/daftarulang_mahasiswa_baru.zul", "root", "daftar_ulang_mahasiswa_baru");
        register(232333331L, "/pages/master/posting_cicilan_mahasiswa.zul", "root", "posting_cicilan_mahasiswa");
        register(673272211L, "ais.action.report.format1.sekolah.LaporanRekapPembayaranSiswa", "root/report", "format1/sekolah/laporan_rekap_pembayaran_siswa");
        register(1000026411L, "/pages/master/kegiatan.zul", "root", "kegiatan");
        register(1003000017L, "/pages/master/daftarulang_mahasiswa_calon.zul", "root", "daftar_ulang_calon_mahasiswa");
        register(2000460500L, "/jurnal/admin/dashboard", "jurnal", "dashboard");
        register(2000460501L, "/jurnal/admin/dashboard", "jurnal", "dashboard");
        register(2000460502L,"/jurnal/admin/journals","jurnal","journals");
        register(2000460503L,"/jurnal/admin/workflow-settings","jurnal","workflow-settings");
        register(2000460504L,"/jurnal/admin/taxonomy","jurnal","taxonomy");
        register(2000460505L,"/jurnal/admin/people","jurnal","people");
        register(2000460506L,"/jurnal/admin/editor-assignments","jurnal","editor-assignments");
        register(2000460507L,"/jurnal/admin/submissions","jurnal","submissions");
        register(2000460508L,"/jurnal/admin/screening","jurnal","screening");
        register(2000460509L,"/jurnal/admin/reviewers","jurnal","reviewers");
        register(2000460510L,"/jurnal/admin/review-assignments","jurnal","review-assignments");
        register(2000460511L,"/jurnal/admin/review-forms","jurnal","review-forms");
        register(2000460512L,"/jurnal/admin/decisions","jurnal","decisions");
        register(2000460513L,"/jurnal/admin/discussions","jurnal","discussions");
        register(2000460514L,"/jurnal/admin/copyediting","jurnal","copyediting");
        register(2000460515L,"/jurnal/admin/production","jurnal","production");
        register(2000460516L,"/jurnal/admin/issues","jurnal","issues");
        register(2000460517L,"/jurnal/admin/publications","jurnal","publications");
        register(2000460518L,"/jurnal/admin/identifiers","jurnal","identifiers");
        register(2000460519L,"/jurnal/admin/portal","jurnal","portal");
        register(2000460520L,"/jurnal/admin/announcements","jurnal","announcements");
        register(2000460521L,"/jurnal/admin/communications","jurnal","communications");
        register(2000460522L,"/jurnal/admin/subscriptions","jurnal","subscriptions");
        register(2000460523L,"/jurnal/admin/payments","jurnal","payments");
        register(2000460524L,"/jurnal/admin/statistics","jurnal","statistics");
        register(2000460525L,"/jurnal/admin/reports","jurnal","reports");
        register(2000460526L,"/jurnal/admin/integrations","jurnal","integrations");
        register(2000460527L,"/jurnal/admin/import-ojs","jurnal","import-ojs");
        register(2000460528L,"/jurnal/admin/operations","jurnal","operations");
    }

    private NewUiRouteRegistry() {
    }

    private static void register(long menuId, String exactLegacyUrl, String module, String page) {
        Route route = new Route(Long.valueOf(menuId), exactLegacyUrl, module, page);
        ROUTES.add(route);
        BY_ID.put(route.menuId, route);
        BY_EXACT_URL.put(route.exactLegacyUrl, route);
        BY_NEW_ROUTE.put(key(module, page), route);
    }

    /** Mapping harus cocok ID, atau URL exact yang telah diverifikasi; tidak memakai contains. */
    public static String[] routeForMenu(Menu menu) {
        Route route = routeForMenuIdAndUrl(menu == null ? null : menu.getId(), menu == null ? null : menu.getUrl());
        return route == null ? null : new String[] { route.module, route.page };
    }

    public static Route routeForMenuIdAndUrl(Long menuId, String legacyUrl) {
        Route byId = menuId == null ? null : BY_ID.get(menuId);
        if (byId != null) {
            /*
             * ID yang sudah dikenal tidak boleh berpindah ke route milik ID
             * lain melalui fallback URL. Ini penting untuk pasangan menu yang
             * memakai halaman ZUL sama tetapi query/semantik berbeda, seperti
             * Pembayaran Siswa dan Pembayaran Calon Siswa.
             */
            return byId.exactLegacyUrl.length() == 0
                    || byId.exactLegacyUrl.equals(normalizeUrl(legacyUrl))
                    ? byId : null;
        }
        return BY_EXACT_URL.get(normalizeUrl(legacyUrl));
    }

    public static Route routeForNewUi(String module, String page) {
        return BY_NEW_ROUTE.get(key(module, page));
    }

    public static boolean isKnownNewUiRoute(String module, String page) {
        return routeForNewUi(module, page) != null;
    }

    public static boolean isSafeLegacyUrl(String url) {
        if (url == null) return false;
        String value = url.trim();
        if (value.length() == 0 || value.indexOf("..") >= 0 || value.indexOf('\\') >= 0
                || value.indexOf('?') >= 0 || value.indexOf('#') >= 0
                || value.indexOf("://") >= 0 || value.startsWith("//")) return false;
        return value.matches("/[A-Za-z0-9_./-]+") || value.matches("[A-Za-z_$][A-Za-z0-9_.$]*");
    }

    private static String normalizeUrl(String url) {
        if (url == null) return "";
        return url.trim().replace('\\', '/').toLowerCase();
    }

    private static String key(String module, String page) {
        String m = module == null ? "" : module.trim().toLowerCase();
        String p = page == null || page.trim().length() == 0 ? "index" : page.trim().toLowerCase();
        return m + "|" + p;
    }
}
