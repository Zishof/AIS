package ais.action.master.jurnal.test;

import java.util.Date;
import java.util.HashSet;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONObject;
import ais.action.master.jurnal.JurnalAuthorizationService;
import ais.common.JurnalAksesKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.jurnal.PenugasanTahapJurnal;

/** Database-backed negative/positive object-scope gate; all writes are rolled back. */
public final class JurnalAuthorizationScopeSelfTest {
    private JurnalAuthorizationScopeSelfTest() {}

    public static void main(String[] args) throws Exception {
        String expected = System.getenv("AIS_JURNAL_DB_NAME");
        if (expected == null || expected.trim().length() == 0 || "ais".equalsIgnoreCase(expected.trim()))
            throw new IllegalStateException("Test wajib diarahkan ke clone SIT/UAT.");
        System.setProperty("javax.persistence.validation.mode", "none");
        JurnalAuthorizationService auth = new JurnalAuthorizationService();
        Tbmuser user = user("JRN_SCOPE_TEST", role("journal_test", true));
        Tbmuser admin = user("JRN_SCOPE_ADMIN", role(Tbmrole.ADMINISTRATOR, true));
        Session session = HibernateUtil.currentSession(); Transaction tx = session.beginTransaction();
        try {
            Long journalId = (Long) session.createQuery("select min(id) from JurnalPenelitian").uniqueResult();
            if (journalId == null) throw new IllegalStateException("Fixture jurnal existing tidak tersedia.");
            expectDenied(auth, session, user, journalId);
            auth.requireJournalScope(session, admin, journalId, null, null, false, "REVIEW");
            PenugasanTahapJurnal assignment = new PenugasanTahapJurnal(); assignment.setTenantKey("self-test");
            assignment.setJurnalPenelitianId(journalId); assignment.setUserId(user.getUserId()); assignment.setRoleKey("EDITOR");
            assignment.setStageKey("REVIEW"); assignment.setStatus("ACTIVE"); assignment.setStartsAt(new Date(System.currentTimeMillis() - 1000L));
            assignment.setCreatedBy(user.getUserId()); assignment.setCreatedAt(new Date()); assignment.setUpdatedAt(new Date()); assignment.setAktif(Boolean.TRUE);
            session.save(assignment); session.flush();
            auth.requireJournalScope(session, user, journalId, null, null, false, "REVIEW");
            boolean wrongStageDenied = false;
            try { auth.requireJournalScope(session, user, journalId, null, null, false, "PRODUCTION"); }
            catch (SecurityException expectedDenied) { wrongStageDenied = true; }
            if (!wrongStageDenied) throw new IllegalStateException("Assignment stage lain tidak boleh memberi akses.");
            System.out.println("JurnalAuthorizationScopeSelfTest OK database=clone negative+positive");
        } finally {
            if (tx.isActive()) tx.rollback(); Tbmuser.getUserRoleYgDipakai.remove(user.getUserId());
            Tbmuser.getUserRoleYgDipakai.remove(admin.getUserId()); HibernateUtil.closeSession();
        }
        System.exit(0);
    }

    private static void expectDenied(JurnalAuthorizationService auth, Session session, Tbmuser user, Long journalId) {
        boolean denied = false; try { auth.requireJournalScope(session, user, journalId, null, null, false, "REVIEW"); }
        catch (SecurityException expected) { denied = true; } if (!denied) throw new IllegalStateException("User tanpa assignment harus ditolak.");
    }

    private static Tbmuser user(String id, Tbmrole role) { Tbmuser user = new Tbmuser(); user.setUserId(id); user.setUserRole(role); Tbmuser.getUserRoleYgDipakai.put(id, role); return user; }
    private static Tbmrole role(String id, boolean grant) throws Exception { Tbmrole role = new Tbmrole(); role.setRoleId(id); Menu menu = new Menu(); menu.setId(Long.valueOf(2000460501L)); HashSet<Menu> menus = new HashSet<Menu>(); menus.add(menu); role.setMenus(menus); JSONObject access = JurnalAksesKatalog.modelUntukEditor(null); access.getJSONObject("menu").put("dashboard", grant); access.getJSONObject("crud").getJSONObject("dashboard").put("read", grant); role.setJurnalAksesJson(access.toString()); return role; }
}
