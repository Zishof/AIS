package ais.action.master.jurnal.test;

import java.util.HashSet;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONObject;
import ais.action.master.jurnal.JurnalAdministrationService;
import ais.action.master.jurnal.JurnalDiscussionService;
import ais.action.master.jurnal.JurnalInvitationService;
import ais.action.master.jurnal.JurnalWorkflowService;
import ais.action.master.jurnal.JurnalUserExchangeService;
import ais.common.JurnalAksesKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Diskusi;
import ais.database.model.DiskusiKomentar;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.jurnal.PenugasanTahapJurnal;
import ais.database.model.jurnal.PesertaDiskusiJurnal;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;
import ais.database.model.repository.RepoItem;

/** Rollback-only invitation token and participant discussion journey. */
public final class JurnalInvitationDiscussionSelfTest {
    private JurnalInvitationDiscussionSelfTest() {}

    public static void main(String[] args) throws Exception {
        String target = System.getenv("AIS_JURNAL_DB_NAME");
        if (target == null || target.trim().length() == 0 || "ais".equalsIgnoreCase(target.trim()))
            throw new IllegalStateException("Test wajib diarahkan ke clone main SIT/UAT.");
        System.setProperty("javax.persistence.validation.mode", "none");
        Tbmuser manager = user("JRN_INVITE_MANAGER", "manager@example.invalid", true);
        Tbmuser invited = user("JRN_INVITED_REVIEWER", "reviewer@example.invalid", false);
        Tbmuser wrong = user("JRN_WRONG_INVITEE", "wrong@example.invalid", false);
        Session session = HibernateUtil.currentSession(); Transaction tx = session.beginTransaction();
        try {
            JurnalPenelitian journal = new JurnalAdministrationService().create("self-test",
                    "Invitation Discussion Self Test", "invitation-discussion-self-test", "id_ID", manager);
            RepoItem item = new JurnalWorkflowService().createDraft(journal.getRepoCollectionId(),
                    "Invitation discussion article", "", "id", manager, "invite-discussion-self-test");
            JurnalInvitationService invitations = new JurnalInvitationService();
            JurnalInvitationService.Issued issued = invitations.issue(journal.getId(), "ignored",
                    invited.getEmail(), "REVIEWER", "REVIEW", "", 3600000L, manager);
            expectSecurity(new Runnable() { public void run() { invitations.accept(issued.token, wrong); }});
            PenugasanTahapJurnal assignment = invitations.accept(issued.token, invited);
            if (!"ACTIVE".equals(assignment.getStatus()) || !"REVIEW".equals(assignment.getStageKey()))
                throw new IllegalStateException("Assignment undangan tidak konsisten.");
            expectSecurity(new Runnable() { public void run() { invitations.accept(issued.token, invited); }});
            JurnalInvitationService.Issued revoked = invitations.issue(journal.getId(), "ignored",
                    invited.getEmail(), "COPYEDITOR", "COPYEDITING", "", 3600000L, manager);
            invitations.revoke(revoked.id, manager);
            expectSecurity(new Runnable() { public void run() { invitations.accept(revoked.token, invited); }});

            JurnalDiscussionService discussions = new JurnalDiscussionService();
            Diskusi discussion = discussions.create(journal.getId(), item.getId(), "REVIEW", "Review discussion",
                    "Diskusi uji", "REVIEWERS", "DOUBLE_ANONYMOUS", manager);
            PesertaDiskusiJurnal participant = discussions.addParticipant(discussion.getId(), invited.getUserId(),
                    "REVIEWER", manager);
            DiskusiKomentar comment = discussions.comment(discussion.getId(), "Review", "Komentar reviewer", invited);
            if (participant.getId() == null || comment.getId() == null)
                throw new IllegalStateException("Peserta/komentar diskusi tidak tersimpan.");
            JurnalUserExchangeService exchange=new JurnalUserExchangeService();java.io.StringWriter userCsv=new java.io.StringWriter();exchange.exportCsv(journal.getId(),userCsv,manager);if(userCsv.toString().indexOf(invited.getUserId())<0||userCsv.toString().indexOf("REVIEW")<0)throw new IllegalStateException("Export user-role gagal.");java.util.List<JurnalInvitationService.Issued> imported=exchange.importInvitations(journal.getId(),"email,role,scope,scope_key\nnew.author@example.invalid,AUTHOR,SUBMISSION,\n",manager);if(imported.size()!=1||imported.get(0).token==null)throw new IllegalStateException("Import undangan user gagal.");boolean malformed=false;try{exchange.importInvitations(journal.getId(),"user,password\nx,secret\n",manager);}catch(IllegalArgumentException expected){malformed=true;}if(!malformed)throw new IllegalStateException("CSV user berbahaya seharusnya ditolak.");
            System.out.println("JurnalInvitationDiscussionSelfTest OK token-bound one-time revoked participant-comment user-role-export invitation-import-safe rollback");
        } finally {
            if (tx.isActive()) tx.rollback(); HibernateUtil.closeSession();
            Tbmuser.getUserRoleYgDipakai.remove(manager.getUserId());
            Tbmuser.getUserRoleYgDipakai.remove(invited.getUserId());
            Tbmuser.getUserRoleYgDipakai.remove(wrong.getUserId());
        }
        System.exit(0);
    }

    private static Tbmuser user(String id, String email, boolean manager) throws Exception {
        Tbmrole role = new Tbmrole(); role.setRoleId(manager ? Tbmrole.ADMINISTRATOR : id + "_ROLE");
        JSONObject json = JurnalAksesKatalog.modelUntukEditor(null); HashSet<Menu> menus = new HashSet<Menu>();
        for (JurnalAksesKatalog.Entri entry : JurnalAksesKatalog.DAFTAR) {
            json.getJSONObject("menu").put(entry.kunci, manager);
            for (String action : JurnalAksesKatalog.AKSI_CRUD)
                json.getJSONObject("crud").getJSONObject(entry.kunci).put(action, manager);
            Menu menu = new Menu(); menu.setId(Long.valueOf(2000000000L + entry.child)); menus.add(menu);
        }
        role.setJurnalAksesJson(json.toString()); role.setMenus(menus);
        Tbmuser user = new Tbmuser(); user.setUserId(id); user.setEmail(email); user.setUserRole(role);
        Tbmuser.getUserRoleYgDipakai.put(id, role); return user;
    }

    private static void expectSecurity(Runnable action) {
        boolean denied = false; try { action.run(); } catch (SecurityException expected) { denied = true; }
        if (!denied) throw new IllegalStateException("Token yang tidak valid seharusnya ditolak.");
    }
}
