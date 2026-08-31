package ais.action.master.jurnal;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Locale;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.jurnal.PenugasanTahapJurnal;
import ais.database.model.jurnal.UndanganPeranJurnal;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;

/** One-time invitation; hanya hash token yang disimpan dan penerima terikat email. */
public final class JurnalInvitationService {
    private final JurnalAuthorizationService auth = new JurnalAuthorizationService();
    private final SecureRandom random = new SecureRandom();
    /**
     * Tipe implementasi bersarang {@link Issued} milik {@link JurnalInvitationService}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * JurnalInvitationService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan
     * dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code String token},
     * {@code Date expiresAt}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     *
     * @see JurnalInvitationService
     */
    public static final class Issued { public Long id; public String token; public Date expiresAt; }

    public Issued issue(Long journalId, String ignoredTenant, String email, String roleKey, String scopeType,
            String scopeKey, long ttlMillis, Tbmuser actor) {
        auth.requireCrud(actor, "people", "create"); String targetEmail = email(email);
        String role = role(roleKey); String stage = stage(scopeType);
        if (ttlMillis < 60000 || ttlMillis > 2592000000L) throw new IllegalArgumentException("Masa berlaku undangan tidak valid.");
        byte[] bytes = new byte[32]; random.nextBytes(bytes); String token = hex(bytes); String hash = sha256(token);
        Date expiry = new Date(System.currentTimeMillis() + ttlMillis); Session s = HibernateUtil.currentSession(); Transaction tx = s.getTransaction(); boolean own = !tx.isActive();
        try { if (own) tx.begin(); JurnalPenelitian journal = (JurnalPenelitian) s.get(JurnalPenelitian.class, journalId);
            if (journal == null || !Boolean.TRUE.equals(journal.getAktif())) throw new IllegalArgumentException("Jurnal tidak ditemukan.");
            auth.requireJournalScope(s, actor, journalId, null, null, false, "JOURNAL");
            Number pending = (Number) s.createQuery("select count(*) from UndanganPeranJurnal where jurnalPenelitianId=:j and lower(email)=:e and roleKey=:r and scopeType=:s and status='PENDING' and expiresAt>:now and aktif=true")
                    .setLong("j", journalId).setString("e", targetEmail).setString("r", role).setString("s", stage).setTimestamp("now", new Date()).uniqueResult();
            if (pending.longValue() > 0) throw new IllegalStateException("Undangan aktif untuk email dan scope tersebut sudah ada.");
            UndanganPeranJurnal invitation = new UndanganPeranJurnal(); invitation.setTenantKey(journal.getTenantKey());
            invitation.setJurnalPenelitianId(journalId); invitation.setEmail(targetEmail); invitation.setRoleKey(role);
            invitation.setScopeType(stage); invitation.setScopeKey(clean(scopeKey)); invitation.setTokenHash(hash);
            invitation.setStatus("PENDING"); invitation.setExpiresAt(expiry); invitation.setCreatedBy(actor.getUserId());
            invitation.setCreatedAt(new Date()); invitation.setUpdatedAt(new Date()); invitation.setAktif(Boolean.TRUE); s.save(invitation);
            if (own) tx.commit(); Issued out = new Issued(); out.id = invitation.getId(); out.token = token; out.expiresAt = expiry; return out;
        } catch (RuntimeException e) { if (own && tx.isActive()) tx.rollback(); throw e; }
    }

    public PenugasanTahapJurnal accept(String token, Tbmuser actor) {
        if (actor == null) throw new SecurityException("Login diperlukan."); required(token, "Token wajib diisi.");
        Session s = HibernateUtil.currentSession(); Transaction tx = s.getTransaction(); boolean own = !tx.isActive();
        try { if (own) tx.begin(); Query q = s.createQuery("from UndanganPeranJurnal where tokenHash=:h and status='PENDING' and aktif=true");
            q.setString("h", sha256(token)); q.setMaxResults(1); UndanganPeranJurnal invitation = (UndanganPeranJurnal) q.uniqueResult();
            if (invitation == null || invitation.getExpiresAt().before(new Date())) throw new SecurityException("Undangan tidak valid atau kedaluwarsa.");
            if (!email(actor.getEmail()).equals(email(invitation.getEmail()))) throw new SecurityException("Undangan diterbitkan untuk email pengguna lain.");
            Query existingQuery = s.createQuery("from PenugasanTahapJurnal where jurnalPenelitianId=:j and userId=:u and roleKey=:r and stageKey=:s and status='ACTIVE' and aktif=true");
            existingQuery.setLong("j", invitation.getJurnalPenelitianId()); existingQuery.setString("u", actor.getUserId());
            existingQuery.setString("r", invitation.getRoleKey()); existingQuery.setString("s", invitation.getScopeType()); existingQuery.setMaxResults(1);
            PenugasanTahapJurnal assignment = (PenugasanTahapJurnal) existingQuery.uniqueResult();
            if (assignment == null) {
                assignment = new PenugasanTahapJurnal(); assignment.setTenantKey(invitation.getTenantKey());
                assignment.setJurnalPenelitianId(invitation.getJurnalPenelitianId()); assignment.setUserId(actor.getUserId());
                assignment.setRoleKey(invitation.getRoleKey()); assignment.setStageKey(invitation.getScopeType());
                assignment.setSectionKey(invitation.getScopeKey()); assignment.setStatus("ACTIVE"); assignment.setStartsAt(new Date());
                assignment.setCreatedBy(actor.getUserId()); assignment.setCreatedAt(new Date()); assignment.setUpdatedAt(new Date()); assignment.setAktif(Boolean.TRUE); s.save(assignment);
            }
            invitation.setStatus("ACCEPTED"); invitation.setAcceptedAt(new Date()); invitation.setInvitedUserId(actor.getUserId()); s.update(invitation);
            if (own) tx.commit(); return assignment;
        } catch (RuntimeException e) { if (own && tx.isActive()) tx.rollback(); throw e; }
    }

    public void revoke(Long id, Tbmuser actor) {
        auth.requireCrud(actor, "people", "delete"); Session s = HibernateUtil.currentSession(); Transaction tx = s.getTransaction(); boolean own = !tx.isActive();
        try { if (own) tx.begin(); UndanganPeranJurnal invitation = (UndanganPeranJurnal) s.get(UndanganPeranJurnal.class, id);
            if (invitation == null) throw new IllegalArgumentException("Undangan tidak ditemukan.");
            auth.requireJournalScope(s, actor, invitation.getJurnalPenelitianId(), null, null, false, "JOURNAL");
            if ("ACCEPTED".equals(invitation.getStatus())) throw new IllegalStateException("Undangan sudah diterima.");
            invitation.setStatus("REVOKED"); invitation.setRevokedAt(new Date()); invitation.setUpdatedAt(new Date()); s.update(invitation); if (own) tx.commit();
        } catch (RuntimeException e) { if (own && tx.isActive()) tx.rollback(); throw e; }
    }

    private static String role(String v) { String x = clean(v).toUpperCase(Locale.ENGLISH); if (!x.matches("MANAGER|EDITOR|SECTION_EDITOR|AUTHOR|REVIEWER|COPYEDITOR|PRODUCTION|PROOFREADER")) throw new IllegalArgumentException("Peran undangan tidak valid."); return x; }
    private static String stage(String v) { String x = clean(v).toUpperCase(Locale.ENGLISH); if (!x.matches("JOURNAL|SECTION|SUBMISSION|REVIEW|COPYEDITING|PRODUCTION|PROOF")) throw new IllegalArgumentException("Scope undangan tidak valid."); return x; }
    private static String email(String v) { String x = clean(v).toLowerCase(Locale.ENGLISH); int comma = x.indexOf(','); if (comma >= 0) x = x.substring(0, comma).trim(); if (!x.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) throw new IllegalArgumentException("Email undangan tidak valid."); return x; }
    private static String sha256(String v) { try { return hex(MessageDigest.getInstance("SHA-256").digest(v.getBytes("UTF-8"))); } catch (Exception e) { throw new IllegalStateException(e); } }
    private static String hex(byte[] bytes) { StringBuilder s = new StringBuilder(); for (byte x : bytes) s.append(String.format("%02x", x & 255)); return s.toString(); }
    private static void required(String v, String m) { if (clean(v).length() == 0) throw new IllegalArgumentException(m); }
    private static String clean(String v) { return v == null ? "" : v.trim(); }
}
