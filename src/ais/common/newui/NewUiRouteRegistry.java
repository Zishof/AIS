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
        if (byId != null && (byId.exactLegacyUrl.length() == 0
                || byId.exactLegacyUrl.equals(normalizeUrl(legacyUrl)))) {
            return byId;
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
