package ais.action.master.jurnal.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONObject;
import ais.action.master.jurnal.JurnalAdministrationService;
import ais.action.master.jurnal.JurnalEmailDeliveryService;
import ais.action.master.jurnal.JurnalEmailService;
import ais.action.master.jurnal.JurnalNotificationPreferenceService;
import ais.common.JurnalAksesKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;
import ais.database.model.Notifikasi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;

/** Rollback-only template, capture, sanitization and idempotency gate. */
public final class JurnalEmailDeliverySelfTest {
    private JurnalEmailDeliverySelfTest() {}

    public static void main(String[] args) throws Exception {
        String target = System.getenv("AIS_JURNAL_DB_NAME");
        if (target == null || target.trim().length() == 0 || "ais".equalsIgnoreCase(target.trim()))
            throw new IllegalStateException("Test wajib diarahkan ke clone main SIT/UAT.");
        if ("SMTP".equalsIgnoreCase(System.getenv("AIS_JURNAL_EMAIL_MODE")))
            throw new IllegalStateException("Self-test tidak boleh memakai mode SMTP.");
        System.setProperty("javax.persistence.validation.mode", "none");
        final Tbmuser actor = admin();
        Session session = HibernateUtil.currentSession();
        Transaction tx = session.beginTransaction();
        try {
            final JurnalPenelitian journal = new JurnalAdministrationService().create("self-test",
                    "Journal Email Self Test", "journal-email-self-test", "id_ID", actor);
            final JurnalEmailService templates = new JurnalEmailService();
            HashSet<String> allowed = new HashSet<String>(Arrays.asList("authorName", "submissionTitle"));
            templates.save(journal.getId(), journal.getTenantKey(), "SUBMISSION_ACK", "id_ID",
                    "Naskah {{submissionTitle}} diterima", "<p>Halo {{authorName}}</p>", allowed, actor);
            int seeded = templates.seedDefaults(journal.getId(), journal.getTenantKey(), actor);
            int seededAgain = templates.seedDefaults(journal.getId(), journal.getTenantKey(), actor);
            Number templateCount = (Number) session.createQuery("select count(*) from TemplateEmailJurnal where jurnalPenelitianId=:j and aktif=true")
                    .setLong("j", journal.getId()).uniqueResult();
            if (seeded != 145 || seededAgain != 0 || templateCount.intValue() != 146)
                throw new IllegalStateException("Seed 73x2 tidak idempoten: created=" + seeded + ", rerun=" + seededAgain + ", total=" + templateCount);
            expectInvalid(new Runnable() { public void run() {
                templates.save(journal.getId(), journal.getTenantKey(), "SUBMISSION_ACK", "id_ID",
                        "Unsafe", "<script>alert(1)</script>", Collections.<String>emptySet(), actor);
            }});
            final Map<String,String> values = new HashMap<String,String>();
            values.put("authorName", "Penulis Uji"); values.put("submissionTitle", "Artikel Uji");
            expectInvalid(new Runnable() { public void run() {
                Map<String,String> bad = new HashMap<String,String>(values); bad.put("unknown", "x");
                templates.render(journal.getId(), "SUBMISSION_ACK", "id_ID", bad);
            }});
            JurnalEmailDeliveryService delivery = new JurnalEmailDeliveryService();
            String key = "submission-ack-self-test";
            Notifikasi first = delivery.enqueue(journal.getId(), "SUBMISSION_ACK", "id_ID", values,
                    new String[]{"AUTHOR@example.invalid", "author@example.invalid"}, key,
                    "workflow-email-self-test", actor);
            Notifikasi second = delivery.enqueue(journal.getId(), "SUBMISSION_ACK", "id_ID", values,
                    new String[]{"author@example.invalid"}, key, "workflow-email-self-test", actor);
            if (first.getId() == null || !first.getId().equals(second.getId())
                    || !"CAPTURED".equals(first.getStatusNotif()))
                throw new IllegalStateException("Capture/idempotency email tidak konsisten.");
            JSONObject snapshot = new JSONObject(first.getJurnalSnapshotJson());
            if (snapshot.getJSONArray("recipients").length() != 1
                    || !snapshot.getString("subject").contains("Artikel Uji"))
                throw new IllegalStateException("Snapshot immutable email tidak konsisten.");
            new JurnalNotificationPreferenceService().save(journal.getId(),true,true,"DAILY",Collections.<String>emptySet(),actor);
            java.util.List<Notifikasi> deferred=delivery.enqueueUsers(journal.getId(),"SUBMISSION_ACK","id_ID",values,
                    new JurnalEmailDeliveryService.Recipient[]{new JurnalEmailDeliveryService.Recipient(actor.getUserId(),"author@example.invalid")},
                    "submission-digest-self-test","workflow-email-self-test",actor);
            if(deferred.size()!=1||!"DIGEST_DAILY".equals(deferred.get(0).getStatusNotif()))throw new IllegalStateException("Preferensi digest tidak diterapkan.");
            deferred.get(0).setWaktu(new java.util.Date(System.currentTimeMillis()-90000000L));session.update(deferred.get(0));session.flush();
            int released=delivery.releaseDueDigests(new java.util.Date(),100,actor);
            if(released<1||!"CAPTURED_DIGEST".equals(deferred.get(0).getStatusNotif()))throw new IllegalStateException("Scheduler digest tidak merilis antrean.");
            new JurnalNotificationPreferenceService().save(journal.getId(),false,true,"NONE",Collections.<String>emptySet(),actor);
            java.util.List<Notifikasi> inApp=delivery.enqueueUsers(journal.getId(),"SUBMISSION_ACK","id_ID",values,
                    new JurnalEmailDeliveryService.Recipient[]{new JurnalEmailDeliveryService.Recipient(actor.getUserId(),"author@example.invalid")},
                    "submission-in-app-self-test","workflow-email-self-test",actor);
            if(!"IN_APP_ONLY".equals(inApp.get(0).getStatusNotif())||new org.json.JSONArray(inApp.get(0).getEmails()).length()!=0)throw new IllegalStateException("Opt-out email tidak diterapkan.");
            System.out.println("JurnalEmailDeliverySelfTest OK templates=146 seeded-idempotent scoped sanitized preferences daily-digest in-app-only rollback");
        } finally {
            if (tx.isActive()) tx.rollback();
            HibernateUtil.closeSession();
            Tbmuser.getUserRoleYgDipakai.remove(actor.getUserId());
        }
        System.exit(0);
    }

    private static Tbmuser admin() throws Exception {
        Tbmrole role = new Tbmrole(); role.setRoleId(Tbmrole.ADMINISTRATOR);
        JSONObject json = JurnalAksesKatalog.modelUntukEditor(null);
        HashSet<Menu> menus = new HashSet<Menu>();
        for (JurnalAksesKatalog.Entri entry : JurnalAksesKatalog.DAFTAR) {
            json.getJSONObject("menu").put(entry.kunci, true);
            for (String action : JurnalAksesKatalog.AKSI_CRUD)
                json.getJSONObject("crud").getJSONObject(entry.kunci).put(action, true);
            Menu menu = new Menu(); menu.setId(Long.valueOf(2000000000L + entry.child)); menus.add(menu);
        }
        role.setJurnalAksesJson(json.toString()); role.setMenus(menus);
        Tbmuser user = new Tbmuser(); user.setUserId("JRN_EMAIL_SELF_TEST"); user.setUserRole(role);
        Tbmuser.getUserRoleYgDipakai.put(user.getUserId(), role); return user;
    }

    private static void expectInvalid(Runnable action) {
        boolean denied = false; try { action.run(); } catch (IllegalArgumentException expected) { denied = true; }
        if (!denied) throw new IllegalStateException("Payload email tidak valid seharusnya ditolak.");
    }
}
