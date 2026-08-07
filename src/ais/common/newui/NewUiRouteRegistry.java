package ais.common.newui;

import java.util.ArrayList;
import java.util.List;

import ais.database.model.Menu;

/**
 * Registry pemetaan antara route New UI (<code>module</code>/<code>page</code>)
 * dan record {@link ais.database.model.Menu} existing.
 *
 * <p>Route New UI berbasis <b>paket source</b> (mis. <code>akunting</code>), sedangkan
 * hak akses berbasis <b>record Menu</b> (ber-URL <code>.zul</code> legacy). Tidak ada
 * pemetaan 1:1 otomatis, sehingga registry ini memakai <b>tabel alias eksplisit</b>
 * berbasis nilai stabil (substring pada <code>Menu.getUrl()</code>). Route tanpa alias
 * yang cocok dianggap <code>UNMAPPED</code>.</p>
 *
 * <p><b>Catatan:</b> tabel alias di bawah masih parsial dan sengaja diisi hanya untuk
 * halaman yang identitasnya pasti (mis. manajemen role/user). Pengisian penuh dilakukan
 * setelah menjalankan <code>docs/sql/new-ui-rbac-diagnostic.sql</code> pada DB dev untuk
 * memetakan sebaran <code>Menu.url</code> ke modul New UI (lihat parity doc §4.2).</p>
 *
 * <p>Kompatibel Java 1.6.</p>
 */
public final class NewUiRouteRegistry {

    /** Status pemetaan sebuah route New UI. */
    public static final int MAPPED_AND_AUTHORIZED = 0;
    public static final int MAPPED_BUT_FORBIDDEN = 1;
    public static final int UNMAPPED = 2;
    public static final int NOT_FOUND = 3;

    /** Satu baris alias: menu yang URL-nya memuat token → route New UI module/page. */
    public static final class Alias {
        public final String menuUrlToken; // dicocokkan (lowercase, contains) ke Menu.getUrl()
        public final String module;
        public final String page;

        public Alias(String menuUrlToken, String module, String page) {
            this.menuUrlToken = menuUrlToken;
            this.module = module;
            this.page = page;
        }
    }

    private static final List<Alias> ALIASES = new ArrayList<Alias>();

    static {
        // Seed minimal yang identitasnya pasti. Tambah entri lain setelah diagnostik.
        ALIASES.add(new Alias("tbmrole", "root/maintenance", "tbmrole"));
        ALIASES.add(new Alias("tbmuser", "root/maintenance", "tbmuser"));
    }

    private NewUiRouteRegistry() {
    }

    /**
     * Route New UI untuk sebuah Menu, atau null bila UNMAPPED.
     * @return String[]{module, page} atau null.
     */
    public static String[] routeForMenu(Menu menu) {
        if (menu == null) {
            return null;
        }
        String url = menu.getUrl();
        if (url == null) {
            return null;
        }
        String lower = url.toLowerCase();
        for (int i = 0; i < ALIASES.size(); i++) {
            Alias a = ALIASES.get(i);
            if (a.menuUrlToken != null && lower.indexOf(a.menuUrlToken) >= 0) {
                return new String[] { a.module, a.page };
            }
        }
        return null;
    }

    /**
     * Menu yang mengatur sebuah route New UI, dicari di dalam daftar menu yang dapat
     * diakses user (agar tidak membocorkan menu di luar hak akses). null bila tidak ada.
     */
    public static Menu menuForRoute(String module, String page, List<Menu> accessibleMenus) {
        if (module == null || accessibleMenus == null) {
            return null;
        }
        for (int i = 0; i < accessibleMenus.size(); i++) {
            Menu m = accessibleMenus.get(i);
            String[] r = routeForMenu(m);
            if (r != null && module.equals(r[0]) && (page == null || page.equals(r[1]))) {
                return m;
            }
        }
        return null;
    }

    /** true bila token muncul pada tabel alias (route punya potensi mapping). */
    public static boolean isKnownRouteToken(String token) {
        if (token == null) {
            return false;
        }
        for (int i = 0; i < ALIASES.size(); i++) {
            Alias a = ALIASES.get(i);
            if (token.equals(a.module) || token.equals(a.module + "/" + a.page)) {
                return true;
            }
        }
        return false;
    }
}
