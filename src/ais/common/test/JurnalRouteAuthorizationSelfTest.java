package ais.common.test;

import java.util.HashSet;
import org.json.JSONObject;
import ais.action.master.jurnal.JurnalAuthorizationService;
import ais.common.JurnalAksesKatalog;
import ais.common.newui.NewUiRouteRegistry;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

/** Complete 28-route positive/negative RBAC contract test. */
public final class JurnalRouteAuthorizationSelfTest {
    private JurnalRouteAuthorizationSelfTest() {}

    public static void main(String[] args) throws Exception {
        Tbmuser allowed = user("JRN_ROUTE_ALLOW", true);
        Tbmuser denied = user("JRN_ROUTE_DENY", false);
        try {
            JurnalAuthorizationService auth = new JurnalAuthorizationService();
            int tested = 0;
            for (JurnalAksesKatalog.Entri entry : JurnalAksesKatalog.DAFTAR) {
                Long menuId = Long.valueOf(2000000000L + entry.child);
                NewUiRouteRegistry.Route route = NewUiRouteRegistry.routeForMenuIdAndUrl(
                        menuId, "/jurnal/admin/" + entry.kunci);
                check(route != null, "Route tidak ditemukan: " + entry.kunci);
                check("jurnal".equals(route.getModule()) && entry.kunci.equals(route.getPage()),
                        "Route tidak canonical: " + entry.kunci);
                check(auth.canMenu(allowed, entry.kunci) && auth.canRead(allowed, entry.kunci),
                        "Positive authorization gagal: " + entry.kunci);
                check(!auth.canMenu(denied, entry.kunci) && !auth.canRead(denied, entry.kunci),
                        "Default-deny gagal: " + entry.kunci);
                tested++;
            }
            check(tested == 28, "Jumlah route yang diuji bukan 28.");
            check(NewUiRouteRegistry.routeForMenuIdAndUrl(Long.valueOf(2999999999L),
                    "/jurnal/admin/tidak-ada") == null, "Route acak harus ditolak.");
            System.out.println("JurnalRouteAuthorizationSelfTest OK routes=28 positive=28 negative=28");
        } finally {
            Tbmuser.getUserRoleYgDipakai.remove(allowed.getUserId());
            Tbmuser.getUserRoleYgDipakai.remove(denied.getUserId());
        }
    }

    private static Tbmuser user(String id, boolean grant) throws Exception {
        Tbmrole role = new Tbmrole(); role.setRoleId(id + "_ROLE");
        JSONObject json = JurnalAksesKatalog.modelUntukEditor(null);
        HashSet<Menu> menus = new HashSet<Menu>();
        for (JurnalAksesKatalog.Entri entry : JurnalAksesKatalog.DAFTAR) {
            json.getJSONObject("menu").put(entry.kunci, grant);
            json.getJSONObject("crud").getJSONObject(entry.kunci).put("read", grant);
            Menu menu = new Menu(); menu.setId(Long.valueOf(2000000000L + entry.child));
            menu.setUrl("/jurnal/admin/" + entry.kunci); menus.add(menu);
        }
        role.setJurnalAksesJson(json.toString()); role.setMenus(menus);
        Tbmuser user = new Tbmuser(); user.setUserId(id); user.setUserRole(role);
        Tbmuser.getUserRoleYgDipakai.put(id, role); return user;
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
