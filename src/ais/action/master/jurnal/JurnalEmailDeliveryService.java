package ais.action.master.jurnal;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Notifikasi;
import ais.database.model.Tbmuser;

/** Immutable template snapshot and duplicate-send prevention on the existing Notifikasi ledger. */
public final class JurnalEmailDeliveryService {
    private final JurnalEmailService templates=new JurnalEmailService();
    private final JurnalAuthorizationService auth=new JurnalAuthorizationService();
    /**
     * Tipe implementasi bersarang {@link Recipient} milik {@link JurnalEmailDeliveryService}. Kelas ini memberi
     * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * JurnalEmailDeliveryService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan
     * dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String userId}, {@code String email}.
     * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see JurnalEmailDeliveryService
     */
    public static final class Recipient { public final String userId,email; public Recipient(String userId,String email){required(userId,"User ID penerima wajib diisi.");this.userId=userId.trim();this.email=validateEmail(email);} }

    /** Preference-aware per-user ledger. Deferred digest rows remain in the existing Notifikasi table. */
    public List<Notifikasi> enqueueUsers(Long journalId,String templateKey,String locale,Map<String,String> values,
            Recipient[] recipients,String idempotencyKey,String correlationId,Tbmuser actor){
        if(recipients==null||recipients.length==0||recipients.length>100)throw new IllegalArgumentException("Penerima wajib diisi dan maksimum 100.");
        Session s=HibernateUtil.currentSession();Transaction tx=s.getTransaction();boolean own=!tx.isActive();List<Notifikasi> out=new ArrayList<Notifikasi>();
        try{if(own)tx.begin();JurnalNotificationPreferenceService preferences=new JurnalNotificationPreferenceService();
            for(Recipient recipient:recipients){JurnalNotificationPreferenceService.Preference p=preferences.load(journalId,recipient.userId);
                boolean email=preferences.allows(journalId,recipient.userId,templateKey,"EMAIL");boolean inApp=preferences.allows(journalId,recipient.userId,templateKey,"IN_APP");
                String perUser=token(idempotencyKey,150)+"-"+shortHash(recipient.userId);Notifikasi n=enqueue(journalId,templateKey,locale,values,new String[]{recipient.email},perUser,correlationId,actor);
                JSONObject snapshot=new JSONObject(n.getJurnalSnapshotJson());snapshot.put("recipientUserId",recipient.userId).put("inApp",inApp).put("emailEnabled",email).put("digest",p.digest);
                n.setNama(new JSONArray().put(recipient.userId).toString());
                if(!email){n.setEmails(new JSONArray().toString());snapshot.put("recipients",new JSONArray());n.setStatusNotif(inApp?"IN_APP_ONLY":"SUPPRESSED");n.setHasil(inApp?"JURNAL_IN_APP":"JURNAL_PREFERENCE_SUPPRESSED");}
                else if("DAILY".equals(p.digest)||"WEEKLY".equals(p.digest)){n.setStatusNotif("DIGEST_"+p.digest);n.setHasil("JURNAL_EMAIL_DIGEST_DEFERRED");}
                n.setJurnalSnapshotJson(snapshot.toString());n.setKeterangan(snapshot.toString());s.update(n);out.add(n);
            }if(own)tx.commit();return out;
        }catch(RuntimeException e){if(own&&tx.isActive())tx.rollback();throw e;}catch(Exception e){if(own&&tx.isActive())tx.rollback();throw new IllegalArgumentException("Pengantrean notifikasi pengguna gagal.",e);}
    }

    /** Releases due daily/weekly digest rows in bounded pages for the existing mail worker. */
    @SuppressWarnings("unchecked") public int releaseDueDigests(Date now,int limit,Tbmuser actor){
        if(actor==null)throw new SecurityException("Login diperlukan.");auth.requireCrud(actor,"communications","update");if(now==null||limit<1||limit>1000)throw new IllegalArgumentException("Batas scheduler digest tidak valid.");
        Session s=HibernateUtil.currentSession();Transaction tx=s.getTransaction();boolean own=!tx.isActive();try{if(own)tx.begin();Date daily=new Date(now.getTime()-86400000L),weekly=new Date(now.getTime()-604800000L);
            Query q=s.createQuery("from Notifikasi where (statusNotif='DIGEST_DAILY' and waktu<=:d) or (statusNotif='DIGEST_WEEKLY' and waktu<=:w) order by id");q.setTimestamp("d",daily);q.setTimestamp("w",weekly);q.setMaxResults(limit);List<Notifikasi> rows=q.list();String mode=clean(System.getenv("AIS_JURNAL_EMAIL_MODE")).toUpperCase();
            for(Notifikasi n:rows){auth.requireJournalScope(s,actor,n.getJurnalPenelitianId(),null,null,false,"JOURNAL");n.setStatusNotif("SMTP".equals(mode)?"QUEUED_DIGEST":"CAPTURED_DIGEST");n.setHasil("SMTP".equals(mode)?"JURNAL_EMAIL_DIGEST_QUEUE":"JURNAL_EMAIL_DIGEST_CAPTURE");s.update(n);}if(own)tx.commit();return rows.size();
        }catch(RuntimeException e){if(own&&tx.isActive())tx.rollback();throw e;}
    }
    public Notifikasi enqueue(Long journalId,String templateKey,String locale,Map<String,String> values,
            String[] recipients,String idempotencyKey,String correlationId,Tbmuser actor){
        if(actor==null)throw new SecurityException("Login diperlukan.");auth.requireCrud(actor,"communications","create");if(recipients==null||recipients.length==0||recipients.length>100)throw new IllegalArgumentException("Penerima wajib diisi dan maksimum 100.");required(idempotencyKey,"Idempotency key wajib diisi.");JurnalEmailService.Rendered r=templates.render(journalId,templateKey,locale,values);Session s=HibernateUtil.currentSession();Transaction tx=s.getTransaction();boolean own=!tx.isActive();try{if(own)tx.begin();auth.requireJournalScope(s,actor,journalId,null,null,false,"JOURNAL");Query q=s.createQuery("from Notifikasi where jurnalPenelitianId=:j and jurnalIdempotencyKey=:k");q.setLong("j",journalId);q.setString("k",token(idempotencyKey,180));q.setMaxResults(1);Notifikasi old=(Notifikasi)q.uniqueResult();if(old!=null){if(own)tx.commit();return old;}JSONArray emails=new JSONArray();java.util.LinkedHashSet<String> unique=new java.util.LinkedHashSet<String>();for(String email:recipients)unique.add(validateEmail(email));for(String email:unique)emails.put(email);JSONObject snapshot=new JSONObject();snapshot.put("schemaVersion",1);snapshot.put("templateKey",r.templateKey);snapshot.put("templateVersion",r.version);snapshot.put("locale",r.locale);snapshot.put("subject",r.subject);snapshot.put("body",r.body);snapshot.put("recipients",emails);snapshot.put("correlationId",tokenOptional(correlationId,180));Notifikasi n=new Notifikasi();n.setJurnalPenelitianId(journalId);n.setJurnalTemplateKey(r.templateKey);n.setJurnalTemplateVersion(Integer.valueOf(r.version));n.setJurnalIdempotencyKey(token(idempotencyKey,180));n.setJurnalCorrelationId(tokenOptional(correlationId,180));n.setJurnalSnapshotJson(snapshot.toString());n.setNama(new JSONArray().toString());n.setEmails(emails.toString());n.setKeterangan(snapshot.toString());n.setWaktu(new Date());n.setBuka(Boolean.FALSE);String mode=clean(System.getenv("AIS_JURNAL_EMAIL_MODE")).toUpperCase();n.setStatusNotif("SMTP".equals(mode)?"QUEUED":"CAPTURED");n.setHasil("SMTP".equals(mode)?"JURNAL_EMAIL_QUEUE":"JURNAL_EMAIL_CAPTURE");n.setOlehId(actor.getUserId());s.save(n);if(own)tx.commit();return n;}catch(RuntimeException e){if(own&&tx.isActive())tx.rollback();throw e;}catch(Exception e){if(own&&tx.isActive())tx.rollback();throw new IllegalArgumentException("Snapshot email tidak valid.",e);}}
    private static String shortHash(String value){try{byte[]h=java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8"));StringBuilder b=new StringBuilder();for(int i=0;i<8;i++)b.append(String.format("%02x",h[i]&255));return b.toString();}catch(Exception e){throw new IllegalStateException(e);}}
    private static String validateEmail(String v){String x=clean(v).toLowerCase();if(x.length()>254||!x.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))throw new IllegalArgumentException("Email penerima tidak valid.");return x;}private static String token(String v,int max){String x=clean(v);if(x.length()<2||x.length()>max||!x.matches("[A-Za-z0-9._:/-]+"))throw new IllegalArgumentException("Key korelasi email tidak valid.");return x;}private static String tokenOptional(String v,int max){return clean(v).length()==0?"":token(v,max);}private static void required(String v,String m){if(clean(v).length()==0)throw new IllegalArgumentException(m);}private static String clean(String v){return v==null?"":v.trim();}
}
