package ais.action.master.jurnal.test;

import java.util.Date;
import java.util.HashSet;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONObject;
import ais.action.master.jurnal.JurnalAdministrationService;
import ais.action.master.jurnal.JurnalContributorService;
import ais.action.master.jurnal.JurnalIdentifierService;
import ais.action.master.jurnal.JurnalPublicationService;
import ais.action.master.jurnal.JurnalWorkflowService;
import ais.common.JurnalAksesKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.jurnal.PenugasanReviewerJurnal;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;
import ais.database.model.repository.RepoItem;

/** Rollback-only submit-review-production-publish compatibility projection journey. */
public final class JurnalWorkflowPublicationSelfTest {
    private JurnalWorkflowPublicationSelfTest() {}

    public static void main(String[] args) throws Exception {
        String target = System.getenv("AIS_JURNAL_DB_NAME");
        if (target == null || target.trim().length() == 0 || "ais".equalsIgnoreCase(target.trim()))
            throw new IllegalStateException("Test wajib diarahkan ke clone main SIT/UAT.");
        System.setProperty("javax.persistence.validation.mode", "none");
        Tbmuser manager = user("JRN_WORKFLOW_MANAGER", true, true);
        Tbmuser reviewer = user("JRN_WORKFLOW_REVIEWER", false, false);
        Session session = HibernateUtil.currentSession();
        Transaction tx = session.beginTransaction();
        try {
            JurnalPenelitian journal = new JurnalAdministrationService().create("self-test",
                    "Journal Workflow Self Test", "journal-workflow-self-test", "id_ID", manager);
            RepoItem article = new JurnalWorkflowService().createDraft(journal.getRepoCollectionId(),
                    "Integrated workflow article", "Rollback-only abstract", "id", manager, "workflow-self-test");
            new JurnalContributorService().addExternal(article.getId(), "External Author",
                    "external.author@example.invalid", "0000-0002-1825-0097", "AIS Test", "",
                    "AUTHOR", 0, true, manager);
            JurnalWorkflowService workflow = new JurnalWorkflowService();
            workflow.transition(article.getId(), null, "SUBMITTED", "SUBMIT", null, manager, "workflow-self-test");
            workflow.transition(article.getId(), null, "SCREENING", "START_SCREENING", null, manager, "workflow-self-test");
            PenugasanReviewerJurnal assignment = workflow.inviteReviewer(article.getId(), reviewer.getUserId(), 1,
                    "DOUBLE_ANONYMOUS", new Date(System.currentTimeMillis() + 86400000L),
                    new Date(System.currentTimeMillis() + 604800000L), "standard:1", manager, "workflow-self-test");
            workflow.respondInvitation(assignment.getId(), true, null, reviewer, "workflow-self-test");
            workflow.submitReview(assignment.getId(), "{\"quality\":5,\"comment\":\"accept\"}",
                    "ACCEPT", reviewer, "workflow-self-test");
            workflow.transition(article.getId(), null, "ACCEPTED", "FINAL_DECISION", null, manager, "workflow-self-test");
            workflow.transition(article.getId(), null, "COPYEDITING", "START_COPYEDIT", null, manager, "workflow-self-test");
            workflow.transition(article.getId(), null, "PRODUCTION", "START_PRODUCTION", null, manager, "workflow-self-test");
            workflow.transition(article.getId(), null, "PROOF", "START_PROOF", null, manager, "workflow-self-test");
            workflow.transition(article.getId(), null, "PUBLICATION_READY", "APPROVE_PROOF", null, manager, "workflow-self-test");
            new JurnalIdentifierService().assignDoi(article.getId(), "10.9999/ais.self-test", manager);
            new JurnalIdentifierService().assignUrn(article.getId(), "urn:ais:self-test", manager);
            JurnalPublicationService publication = new JurnalPublicationService();
            RepoItem issue = publication.createIssue(journal.getRepoCollectionId(), "Vol 1 No 1", 1, "1", 2026, null, manager);
            publication.placeArticle(issue.getId(), article.getId(), 0, manager);
            publication.publishIssue(issue.getId(), new Date(), manager);
            session.flush();
            Number projection = (Number) session.createQuery(
                    "select count(*) from Artikel where repoItemId=:i").setLong("i", article.getId()).uniqueResult();
            Number events = (Number) session.createQuery(
                    "select count(*) from RepoWorkflowEvent where itemId=:i").setLong("i", article.getId()).uniqueResult();
            if (projection.longValue() != 1L || events.longValue() < 9L
                    || !"PUBLISHED".equals(article.getWorkflowStatus()) || !"PUBLISHED".equals(issue.getWorkflowStatus()))
                throw new IllegalStateException("Journey publikasi atau projection tidak konsisten.");
            publication.projectPublishedArticle(article.getId(), manager);
            session.flush();
            Number projectionAgain = (Number) session.createQuery(
                    "select count(*) from Artikel where repoItemId=:i").setLong("i", article.getId()).uniqueResult();
            if (projectionAgain.longValue() != 1L)
                throw new IllegalStateException("Projection Artikel tidak idempoten.");
            System.out.println("JurnalWorkflowPublicationSelfTest OK submit-review-production-publish projection-idempotent rollback");
        } finally {
            if (tx.isActive()) tx.rollback();
            HibernateUtil.closeSession();
            Tbmuser.getUserRoleYgDipakai.remove(manager.getUserId());
            Tbmuser.getUserRoleYgDipakai.remove(reviewer.getUserId());
        }
        System.exit(0);
    }

    private static Tbmuser user(String id, boolean administrator, boolean all) throws Exception {
        Tbmrole role = new Tbmrole(); role.setRoleId(administrator ? Tbmrole.ADMINISTRATOR : id + "_ROLE");
        JSONObject json = JurnalAksesKatalog.modelUntukEditor(null);
        HashSet<Menu> menus = new HashSet<Menu>();
        for (JurnalAksesKatalog.Entri entry : JurnalAksesKatalog.DAFTAR) {
            boolean grant = all || "prosesReview".equals(entry.kunci);
            json.getJSONObject("menu").put(entry.kunci, grant);
            for (String action : JurnalAksesKatalog.AKSI_CRUD)
                json.getJSONObject("crud").getJSONObject(entry.kunci).put(action, grant);
            Menu menu = new Menu(); menu.setId(Long.valueOf(2000000000L + entry.child)); menus.add(menu);
        }
        for (String action : JurnalAksesKatalog.AKSI_WORKFLOW)
            json.getJSONObject("workflow").put(action, all);
        role.setJurnalAksesJson(json.toString()); role.setMenus(menus);
        Tbmuser user = new Tbmuser(); user.setUserId(id); user.setUserRole(role);
        Tbmuser.getUserRoleYgDipakai.put(id, role); return user;
    }
}
