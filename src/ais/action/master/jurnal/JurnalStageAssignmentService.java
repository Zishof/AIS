package ais.action.master.jurnal;

import java.util.Date;
import java.util.Locale;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.jurnal.PenugasanTahapJurnal;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;
import ais.database.model.repository.RepoItem;

/** Scoped editor/copyeditor/production/proof assignments on the shared assignment table. */
public final class JurnalStageAssignmentService {
    private final JurnalAuthorizationService auth = new JurnalAuthorizationService();

    public PenugasanTahapJurnal assign(Long journalId, Long itemId, String userId, String role, String stage,
            String section, Date starts, Date ends, String provenanceJson, Tbmuser actor) {
        authorize(stage, actor); String normalizedStage = stage(stage); String normalizedRole = role(role);
        if (blank(userId)) throw new IllegalArgumentException("Pengguna penugasan wajib diisi.");
        Date from = starts == null ? new Date() : starts;
        if (ends != null && !ends.after(from)) throw new IllegalArgumentException("Masa penugasan tidak valid.");
        Session s = HibernateUtil.currentSession(); Transaction tx = s.getTransaction(); boolean own = !tx.isActive();
        try { if (own) tx.begin();
            JurnalPenelitian journal = (JurnalPenelitian) s.get(JurnalPenelitian.class, journalId);
            RepoItem item = itemId == null ? null : (RepoItem) s.get(RepoItem.class, itemId);
            if (journal == null || (item != null && !journal.getRepoCollectionId().equals(item.getCollectionId())))
                throw new SecurityException("Scope penugasan berbeda jurnal.");
            auth.requireJournalScope(s, actor, journalId, itemId, null, false, normalizedStage);
            Query q = s.createQuery("from PenugasanTahapJurnal where jurnalPenelitianId=:j and userId=:u and roleKey=:r and stageKey=:s and ((itemId is null and :i is null) or itemId=:i) and status='ACTIVE' and aktif=true");
            q.setLong("j", journalId); q.setString("u", clean(userId)); q.setString("r", normalizedRole); q.setString("s", normalizedStage);
            if (itemId == null) q.setParameter("i", null); else q.setLong("i", itemId); q.setMaxResults(1);
            PenugasanTahapJurnal assignment = (PenugasanTahapJurnal) q.uniqueResult();
            if (assignment == null) {
                assignment = new PenugasanTahapJurnal(); assignment.setTenantKey(journal.getTenantKey()); assignment.setJurnalPenelitianId(journalId);
                assignment.setItemId(itemId); assignment.setUserId(clean(userId)); assignment.setRoleKey(normalizedRole); assignment.setStageKey(normalizedStage);
                assignment.setStatus("ACTIVE"); assignment.setCreatedBy(actor.getUserId()); assignment.setCreatedAt(new Date()); assignment.setAktif(Boolean.TRUE);
            }
            assignment.setSectionKey(clean(section)); assignment.setStartsAt(from); assignment.setEndsAt(ends);
            assignment.setProvenanceJson(validateJson(provenanceJson)); assignment.setUpdatedAt(new Date());
            if (assignment.getId() == null) s.save(assignment); else s.update(assignment);
            if (own) tx.commit(); return assignment;
        } catch (RuntimeException e) { if (own && tx.isActive()) tx.rollback(); throw e; }
    }

    public void end(Long assignmentId, Date endedAt, Tbmuser actor) {
        Session s = HibernateUtil.currentSession(); Transaction tx = s.getTransaction(); boolean own = !tx.isActive();
        try { if (own) tx.begin(); PenugasanTahapJurnal assignment = (PenugasanTahapJurnal) s.get(PenugasanTahapJurnal.class, assignmentId);
            if (assignment == null) throw new IllegalArgumentException("Penugasan tidak ditemukan.");
            authorize(assignment.getStageKey(), actor);
            auth.requireJournalScope(s, actor, assignment.getJurnalPenelitianId(), assignment.getItemId(), null, false, assignment.getStageKey());
            assignment.setStatus("ENDED"); assignment.setEndsAt(endedAt == null ? new Date() : endedAt); assignment.setUpdatedAt(new Date()); s.update(assignment);
            if (own) tx.commit();
        } catch (RuntimeException e) { if (own && tx.isActive()) tx.rollback(); throw e; }
    }

    private void authorize(String stage, Tbmuser actor) { String s = stage(stage); if ("COPYEDITING".equals(s)) auth.requireCrud(actor, "copyediting", "update"); else if ("PRODUCTION".equals(s) || "PROOF".equals(s)) auth.requireCrud(actor, "production", "update"); else auth.requireWorkflow(actor, "assignEditor"); }
    private static String stage(String v) { String x = clean(v).toUpperCase(Locale.ENGLISH); if (!x.matches("JOURNAL|SECTION|SUBMISSION|REVIEW|COPYEDITING|PRODUCTION|PROOF")) throw new IllegalArgumentException("Tahap penugasan tidak valid."); return x; }
    private static String role(String v) { String x = clean(v).toUpperCase(Locale.ENGLISH); if (!x.matches("MANAGER|EDITOR|SECTION_EDITOR|COPYEDITOR|PRODUCTION|PROOFREADER")) throw new IllegalArgumentException("Peran penugasan tidak valid."); return x; }
    private static String validateJson(String v) { if (blank(v)) return "{\"schemaVersion\":1}"; if (v.length() > 65536) throw new IllegalArgumentException("Provenance terlalu besar."); try { org.json.JSONObject o = new org.json.JSONObject(v); if (o.optInt("schemaVersion", 0) != 1) throw new Exception(); return o.toString(); } catch (Exception e) { throw new IllegalArgumentException("Provenance penugasan tidak valid.", e); } }
    private static boolean blank(String v) { return clean(v).length() == 0; }
    private static String clean(String v) { return v == null ? "" : v.trim(); }
}
