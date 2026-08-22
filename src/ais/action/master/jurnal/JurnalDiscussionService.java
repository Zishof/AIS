package ais.action.master.jurnal;

import java.util.Date;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Diskusi;
import ais.database.model.DiskusiKomentar;
import ais.database.model.Tbmuser;
import ais.database.model.jurnal.PesertaDiskusiJurnal;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;
import ais.database.model.repository.RepoItem;

/** Existing Diskusi/DiskusiKomentar extended for journal workflow; only membership needs a new table. */
public final class JurnalDiscussionService {
    private final JurnalAuthorizationService auth = new JurnalAuthorizationService();

    public Diskusi create(Long journalId, Long itemId, String stage, String title, String description,
            String visibility, String anonymity, Tbmuser actor) {
        auth.requireCrud(actor, "prosesReview", "create"); required(title, "Judul diskusi wajib diisi.");
        String normalizedStage = validStage(stage); Session s = HibernateUtil.currentSession(); Transaction tx = s.getTransaction(); boolean own = !tx.isActive();
        try { if (own) tx.begin(); JurnalPenelitian journal = (JurnalPenelitian) s.get(JurnalPenelitian.class, journalId); RepoItem item = (RepoItem) s.get(RepoItem.class, itemId);
            if (journal == null || item == null || journal.getRepoCollectionId() == null || !journal.getRepoCollectionId().equals(item.getCollectionId()))
                throw new SecurityException("Naskah berada di jurnal lain.");
            auth.requireJournalScope(s, actor, journalId, itemId, item.getOwnerId(), false, normalizedStage);
            Diskusi discussion = new Diskusi(); discussion.setJurnalPenelitianId(journalId); discussion.setRepoItemId(itemId);
            discussion.setStageKey(normalizedStage); discussion.setNama(clean(title)); discussion.setKeterangan(limit(description, 1000));
            discussion.setVisibility(validVisibility(visibility)); discussion.setAnonymityMode(validAnonymity(anonymity));
            // Scalar actor identity is canonical for this journal path. Avoid a
            // fragile ORM association to the very broad legacy Tbmuser mapping.
            discussion.setTanggal(new Date()); discussion.setOlehId(actor.getUserId()); s.save(discussion); s.flush();
            participant(s, discussion, journal, item, actor.getUserId(), "CREATOR", actor); if (own) tx.commit(); return discussion;
        } catch (RuntimeException e) { if (own && tx.isActive()) tx.rollback(); throw e; }
    }

    public PesertaDiskusiJurnal addParticipant(Long discussionId, String userId, String role, Tbmuser actor) {
        auth.requireCrud(actor, "prosesReview", "update"); Session s = HibernateUtil.currentSession(); Transaction tx = s.getTransaction(); boolean own = !tx.isActive();
        try { if (own) tx.begin(); Diskusi discussion = discussion(s, discussionId); JurnalPenelitian journal = (JurnalPenelitian) s.get(JurnalPenelitian.class, discussion.getJurnalPenelitianId()); RepoItem item = (RepoItem) s.get(RepoItem.class, discussion.getRepoItemId());
            if (journal == null || item == null) throw new IllegalArgumentException("Scope diskusi jurnal tidak ditemukan.");
            auth.requireJournalScope(s, actor, journal.getId(), item.getId(), item.getOwnerId(), false, discussion.getStageKey());
            PesertaDiskusiJurnal result = participant(s, discussion, journal, item, clean(userId), validRole(role), actor);
            if (own) tx.commit(); return result;
        } catch (RuntimeException e) { if (own && tx.isActive()) tx.rollback(); throw e; }
    }

    public DiskusiKomentar comment(Long discussionId, String subject, String body, Tbmuser actor) {
        if (actor == null) throw new SecurityException("Login diperlukan."); required(body, "Komentar wajib diisi.");
        Session s = HibernateUtil.currentSession(); Transaction tx = s.getTransaction(); boolean own = !tx.isActive();
        try { if (own) tx.begin(); Diskusi discussion = discussion(s, discussionId);
            Query q = s.createQuery("select count(*) from PesertaDiskusiJurnal where diskusiId=:d and userId=:u and aktif=true and leftAt is null");
            q.setLong("d", discussionId); q.setString("u", actor.getUserId()); Number count = (Number) q.uniqueResult();
            boolean participant = count != null && count.longValue() > 0;
            if (!participant) {
                if (!auth.canCrud(actor, "prosesReview", "update")) throw new SecurityException("Pengguna bukan peserta diskusi.");
                RepoItem item = (RepoItem) s.get(RepoItem.class, discussion.getRepoItemId());
                auth.requireJournalScope(s, actor, discussion.getJurnalPenelitianId(), discussion.getRepoItemId(),
                        item == null ? null : item.getOwnerId(), false, discussion.getStageKey());
            }
            DiskusiKomentar comment = new DiskusiKomentar(); comment.setDiskusi(discussion);
            comment.setNama(blank(subject) ? "Komentar" : limit(clean(subject), 255)); comment.setKeterangan(limit(clean(body), 262144));
            comment.setTanggal(new Date()); comment.setOlehId(actor.getUserId()); s.save(comment);
            if (own) tx.commit(); return comment;
        } catch (RuntimeException e) { if (own && tx.isActive()) tx.rollback(); throw e; }
    }

    private static Diskusi discussion(Session s, Long id) { Diskusi d = (Diskusi) s.get(Diskusi.class, id); if (d == null || d.getRepoItemId() == null || d.getJurnalPenelitianId() == null) throw new IllegalArgumentException("Diskusi jurnal tidak ditemukan."); return d; }
    private PesertaDiskusiJurnal participant(Session s, Diskusi d, JurnalPenelitian j, RepoItem item, String user, String role, Tbmuser actor) { if (blank(user)) throw new IllegalArgumentException("Pengguna peserta wajib diisi."); Query q = s.createQuery("from PesertaDiskusiJurnal where diskusiId=:d and userId=:u and aktif=true"); q.setLong("d", d.getId()); q.setString("u", user); q.setMaxResults(1); PesertaDiskusiJurnal p = (PesertaDiskusiJurnal) q.uniqueResult(); if (p != null) { p.setLeftAt(null); p.setParticipantRole(role); p.setUpdatedAt(new Date()); s.update(p); return p; } p = new PesertaDiskusiJurnal(); p.setDiskusiId(d.getId()); p.setUserId(user); p.setParticipantRole(role); p.setJoinedAt(new Date()); p.setTenantKey(item.getTenantKey()); p.setJurnalPenelitianId(j.getId()); p.setCreatedBy(actor.getUserId()); p.setCreatedAt(new Date()); p.setUpdatedAt(new Date()); p.setAktif(Boolean.TRUE); s.save(p); return p; }
    private static String validStage(String v) { String x = clean(v).toUpperCase(); if (!("SUBMISSION".equals(x) || "REVIEW".equals(x) || "COPYEDITING".equals(x) || "PRODUCTION".equals(x) || "PROOF".equals(x))) throw new IllegalArgumentException("Tahap diskusi tidak valid."); return x; }
    private static String validVisibility(String v) { String x = clean(v).toUpperCase(); if (!("INTERNAL".equals(x) || "REVIEWERS".equals(x) || "AUTHOR_EDITOR".equals(x) || "ALL_PARTICIPANTS".equals(x))) throw new IllegalArgumentException("Visibilitas diskusi tidak valid."); return x; }
    private static String validAnonymity(String v) { String x = clean(v).toUpperCase(); if (!("DOUBLE_ANONYMOUS".equals(x) || "SINGLE_ANONYMOUS".equals(x) || "OPEN".equals(x))) throw new IllegalArgumentException("Anonimitas tidak valid."); return x; }
    private static String validRole(String v) { String x = clean(v).toUpperCase(); if (!x.matches("AUTHOR|EDITOR|SECTION_EDITOR|REVIEWER|COPYEDITOR|PRODUCTION|PROOFREADER")) throw new IllegalArgumentException("Peran peserta tidak valid."); return x; }
    private static String limit(String v, int n) { if (v == null) return null; return v.length() <= n ? v : v.substring(0, n); }
    private static void required(String v, String m) { if (blank(v)) throw new IllegalArgumentException(m); }
    private static boolean blank(String v) { return clean(v).length() == 0; }
    private static String clean(String v) { return v == null ? "" : v.trim(); }
}
