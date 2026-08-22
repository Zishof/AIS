package ais.action.master.jurnal.test;

import java.util.Date;
import java.util.HashSet;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONObject;
import ais.action.master.jurnal.JurnalAccessService;
import ais.action.master.jurnal.JurnalAdministrationService;
import ais.common.JurnalAksesKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.jurnal.LanggananJurnal;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;
import ais.database.model.repository.RepoCollection;
import ais.database.model.repository.RepoItem;

/** Rollback-only open/user/institution-IP access boundary test. */
public final class JurnalAccessIpSelfTest {
    private JurnalAccessIpSelfTest() {}

    public static void main(String[] args) throws Exception {
        String target = System.getenv("AIS_JURNAL_DB_NAME");
        if (target == null || target.trim().length() == 0 || "ais".equalsIgnoreCase(target.trim()))
            throw new IllegalStateException("Test wajib diarahkan ke clone main SIT/UAT.");
        System.setProperty("javax.persistence.validation.mode", "none");
        Tbmuser actor = admin(); Session session = HibernateUtil.currentSession(); Transaction tx = session.beginTransaction();
        try {
            JurnalPenelitian journal = new JurnalAdministrationService().create("self-test", "Access Self Test",
                    "access-self-test", "id_ID", actor);
            RepoCollection collection = (RepoCollection) session.get(RepoCollection.class, journal.getRepoCollectionId());
            collection.setAccessPolicyJson("{\"schemaVersion\":1,\"policies\":[{\"policyKey\":\"subscriber\",\"active\":true,\"format\":\"SUBSCRIPTION\",\"price\":0,\"currency\":\"IDR\"},{\"policyKey\":\"institution\",\"active\":true,\"format\":\"INSTITUTION\",\"price\":0,\"currency\":\"IDR\"}]}");
            session.update(collection);
            RepoItem item = item(collection, actor); session.save(item); session.flush();
            Date start = new Date(System.currentTimeMillis() - 60000L), end = new Date(System.currentTimeMillis() + 86400000L);
            JurnalAccessService access = new JurnalAccessService();
            access.activate(journal.getId(), collection.getId(), "subscriber", "reader-self-test", null, null,
                    start, end, null, "ignored", actor);
            LanggananJurnal institution = access.activate(journal.getId(), collection.getId(), "institution", null,
                    "PERGURUAN_TINGGI", Long.valueOf(1L), start, end, null, "ignored", actor);
            access.addRange(institution.getId(), "10.10.10.10", "10.10.10.20", "Campus", actor);
            access.addRange(institution.getId(), "2001:db8::10", "2001:db8::20", "Campus IPv6", actor);
            expectInvalid(new Runnable() { public void run() {
                access.addRange(institution.getId(), "10.10.10.15", "10.10.10.25", "Overlap", actor);
            }});
            expectInvalid(new Runnable() { public void run() {
                access.addRange(institution.getId(), "localhost", "localhost", "DNS", actor);
            }});
            check(access.evaluate(item, "reader-self-test", null, null, new Date()).allowed, "User subscription");
            check(access.evaluate(item, null, null, "10.10.10.10", new Date()).allowed, "IP lower boundary");
            check(access.evaluate(item, null, null, "10.10.10.20", new Date()).allowed, "IP upper boundary");
            check(!access.evaluate(item, null, null, "10.10.10.21", new Date()).allowed, "IP outside");
            check(access.evaluate(item, null, null, "2001:db8::10", new Date()).allowed, "IPv6 lower boundary");
            check(access.evaluate(item, null, null, "2001:db8::20", new Date()).allowed, "IPv6 upper boundary");
            check(!access.evaluate(item, null, null, "2001:db8::21", new Date()).allowed, "IPv6 outside");
            check(!access.evaluate(item, null, null, "localhost", new Date()).allowed, "Hostname deny");
            check(!access.evaluate(item, "reader-self-test", null, null, new Date(end.getTime()+1L)).allowed, "Expired subscription deny");
            item.setAccessPolicy("OPEN_ACCESS"); item.setEmbargoUntil(null); session.update(item);
            check(access.evaluate(item, null, null, null, new Date()).allowed, "Open access");
            System.out.println("JurnalAccessIpSelfTest OK user institution IPv4/IPv6 boundaries overlap expiry hostname-deny rollback");
        } finally {
            if (tx.isActive()) tx.rollback(); HibernateUtil.closeSession();
            Tbmuser.getUserRoleYgDipakai.remove(actor.getUserId());
        }
        System.exit(0);
    }

    private static RepoItem item(RepoCollection c, Tbmuser actor) {
        RepoItem i = new RepoItem(); i.setCollectionId(c.getId()); i.setTenantKey(c.getTenantKey());
        i.setDocumentType("JOURNAL_SUBMISSION"); i.setWorkflowStatus("PUBLISHED"); i.setSyncStatus("PUBLISHED");
        i.setTitle("Protected article"); i.setLanguage("id"); i.setOwnerId(actor.getUserId()); i.setAccessPolicy("RESTRICTED");
        i.setAktif(Boolean.TRUE); i.setViewCount(0L); i.setDownloadCount(0L); i.setVersionNumber(1L); i.setOlehId(actor.getUserId()); return i;
    }

    private static Tbmuser admin() throws Exception {
        Tbmrole role = new Tbmrole(); role.setRoleId(Tbmrole.ADMINISTRATOR);
        JSONObject json = JurnalAksesKatalog.modelUntukEditor(null); HashSet<Menu> menus = new HashSet<Menu>();
        for (JurnalAksesKatalog.Entri entry : JurnalAksesKatalog.DAFTAR) {
            json.getJSONObject("menu").put(entry.kunci, true);
            for (String action : JurnalAksesKatalog.AKSI_CRUD) json.getJSONObject("crud").getJSONObject(entry.kunci).put(action, true);
            Menu menu = new Menu(); menu.setId(Long.valueOf(2000000000L + entry.child)); menus.add(menu);
        }
        json.getJSONObject("workflow").put("manageSubscription", true);
        role.setJurnalAksesJson(json.toString()); role.setMenus(menus);
        Tbmuser user = new Tbmuser(); user.setUserId("JRN_ACCESS_SELF_TEST"); user.setUserRole(role);
        Tbmuser.getUserRoleYgDipakai.put(user.getUserId(), role); return user;
    }

    private static void expectInvalid(Runnable action) { boolean denied=false; try{action.run();}catch(IllegalArgumentException expected){denied=true;} if(!denied)throw new IllegalStateException("Input rentang harus ditolak."); }
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message+" gagal.");}
}
