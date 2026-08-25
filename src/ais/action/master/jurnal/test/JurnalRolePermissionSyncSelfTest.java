package ais.action.master.jurnal.test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONObject;

import ais.action.master.jurnal.JurnalAuthorizationService;
import ais.common.JurnalAksesKatalog;
import ais.common.JurnalRoleMenuSynchronizer;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

/** Database-backed role JSON/menu/cache synchronization; transaction always rolls back. */
public final class JurnalRolePermissionSyncSelfTest {
    private JurnalRolePermissionSyncSelfTest() {}

    public static void main(String[] args) throws Exception {
        String db = System.getenv("AIS_JURNAL_DB_NAME");
        if (db == null || !(db.toLowerCase().contains("_sit") || db.toLowerCase().contains("_uat")))
            throw new IllegalStateException("Test sinkronisasi role wajib memakai clone SIT/UAT.");
        System.setProperty("javax.persistence.validation.mode", "none");
        Session session = HibernateUtil.currentSession();
        Transaction tx = session.beginTransaction();
        Tbmuser actor = null;
        try {
            Tbmrole role = (Tbmrole) session.get(Tbmrole.class, Tbmrole.ADMINISTRATOR);
            if (role == null) throw new IllegalStateException("Role administrator tidak ditemukan.");
            int unrelatedBefore = unrelated(role);
            JSONObject access = JurnalAksesKatalog.modelUntukEditor(null);
            access.getJSONObject("menu").put("journals", true);
            access.getJSONObject("crud").getJSONObject("journals").put("create", true);
            access.getJSONObject("crud").getJSONObject("journals").put("delete", true);
            access.getJSONObject("menu").put("integrations", true);
            access.getJSONObject("crud").getJSONObject("integrations").put("create", true);
            access.getJSONObject("workflow").put("manageImport", true);
            String raw = access.toString();
            role.setJurnalAksesJson(raw);
            JurnalRoleMenuSynchronizer.synchronize(session, role, raw);
            session.update(role);
            session.flush();
            check(has(role, 2000460500L), "Parent jurnal tidak terpasang.");
            check(has(role, 2000460502L), "Menu Identitas Jurnal tidak terpasang.");
            check(has(role, 2000460526L), "Menu Integrasi tidak terpasang.");
            check(unrelated(role) == unrelatedBefore, "Menu non-jurnal berubah.");

            actor = new Tbmuser();
            actor.setUserId("JRN_ROLE_SYNC_SELF_TEST");
            actor.setUserRole(role);
            Tbmuser.getUserRoleYgDipakai.put(actor.getUserId(), new Tbmrole(Tbmrole.ADMINISTRATOR));
            Tbmuser.refreshHakAksesUntukRole(role);
            check(actor.hakAkses() == role, "Cache active-role tidak disegarkan.");
            JurnalAuthorizationService auth = new JurnalAuthorizationService();
            auth.requireCrud(actor, "journals", "create");
            auth.requireCrud(actor, "journals", "delete");
            auth.requireCrud(actor, "integrations", "create");
            auth.requireWorkflow(actor, "manageImport");
            System.out.println("JurnalRolePermissionSyncSelfTest OK json+menu+cache+authorization rollback");
        } finally {
            if (tx.isActive()) tx.rollback();
            if (actor != null) Tbmuser.getUserRoleYgDipakai.remove(actor.getUserId());
            HibernateUtil.closeSession();
        }
        System.exit(0);
    }

    private static boolean has(Tbmrole role, long id) {
        if (role.getMenus() == null) return false;
        for (Menu menu : role.getMenus())
            if (menu != null && menu.getId() != null && menu.getId().longValue() == id) return true;
        return false;
    }

    private static int unrelated(Tbmrole role) {
        int count = 0;
        if (role.getMenus() != null) for (Menu menu : role.getMenus())
            if (menu != null && menu.getId() != null && !JurnalRoleMenuSynchronizer.isManaged(menu.getId().longValue())) count++;
        return count;
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
