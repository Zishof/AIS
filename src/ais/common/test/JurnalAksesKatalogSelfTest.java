package ais.common.test;

import org.json.JSONObject;
import ais.common.JurnalAksesKatalog;
import ais.common.JurnalRoleMenuSynchronizer;

public final class JurnalAksesKatalogSelfTest {
    private JurnalAksesKatalogSelfTest() {}
    public static void main(String[] args) throws Exception {
        check(JurnalAksesKatalog.DAFTAR.size() == 28, "28 menu canonical");
        check(!JurnalAksesKatalog.bolehMenu(null, "dashboard"), "null deny");
        check(!JurnalAksesKatalog.bolehMenu("{}", "dashboard"), "missing version deny");
        check(!JurnalAksesKatalog.bolehMenu("{\"schemaVersion\":2,\"menu\":{},\"crud\":{},\"workflow\":{}}", "dashboard"), "unknown version deny");
        JSONObject editor = JurnalAksesKatalog.modelUntukEditor(null);
        check(!JurnalAksesKatalog.bolehMenu(editor.toString(), "dashboard"), "editor defaults deny");
        check(JurnalRoleMenuSynchronizer.desiredMenuIds(editor.toString()).isEmpty(), "default deny tidak memasang menu fisik");
        editor.getJSONObject("menu").put("journals", true);
        check(JurnalRoleMenuSynchronizer.desiredMenuIds(editor.toString()).contains(Long.valueOf(2000460500L)), "parent jurnal otomatis");
        check(JurnalRoleMenuSynchronizer.desiredMenuIds(editor.toString()).contains(Long.valueOf(2000460502L)), "menu journals otomatis");
        editor.getJSONObject("menu").put("dashboard", true);
        editor.getJSONObject("crud").getJSONObject("dashboard").put("read", true);
        editor.getJSONObject("workflow").put("viewAudit", true);
        String raw = editor.toString();
        check(JurnalAksesKatalog.bolehMenu(raw, "dashboard"), "known menu allow");
        check(JurnalAksesKatalog.bolehCrud(raw, "dashboard", "read"), "known crud allow");
        check(JurnalAksesKatalog.bolehWorkflow(raw, "viewAudit"), "known workflow allow");
        check(!JurnalAksesKatalog.bolehMenu(raw, "unknown"), "unknown key deny");
        editor.getJSONObject("menu").put("dashboard", "true");
        check(!JurnalAksesKatalog.bolehMenu(editor.toString(), "dashboard"), "non boolean deny");
        System.out.println("JurnalAksesKatalogSelfTest OK");
    }
    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
