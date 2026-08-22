package ais.action.master.jurnal;

import java.util.Date;
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
    public Notifikasi enqueue(Long journalId,String templateKey,String locale,Map<String,String> values,
            String[] recipients,String idempotencyKey,String correlationId,Tbmuser actor){
        if(actor==null)throw new SecurityException("Login diperlukan.");auth.requireCrud(actor,"emailNotifikasi","create");if(recipients==null||recipients.length==0||recipients.length>100)throw new IllegalArgumentException("Penerima wajib diisi dan maksimum 100.");required(idempotencyKey,"Idempotency key wajib diisi.");JurnalEmailService.Rendered r=templates.render(journalId,templateKey,locale,values);Session s=HibernateUtil.currentSession();Transaction tx=s.getTransaction();boolean own=!tx.isActive();try{if(own)tx.begin();auth.requireJournalScope(s,actor,journalId,null,null,false,"JOURNAL");Query q=s.createQuery("from Notifikasi where jurnalPenelitianId=:j and jurnalIdempotencyKey=:k");q.setLong("j",journalId);q.setString("k",token(idempotencyKey,180));q.setMaxResults(1);Notifikasi old=(Notifikasi)q.uniqueResult();if(old!=null){if(own)tx.commit();return old;}JSONArray emails=new JSONArray();java.util.LinkedHashSet<String> unique=new java.util.LinkedHashSet<String>();for(String email:recipients)unique.add(validateEmail(email));for(String email:unique)emails.put(email);JSONObject snapshot=new JSONObject();snapshot.put("schemaVersion",1);snapshot.put("templateKey",r.templateKey);snapshot.put("templateVersion",r.version);snapshot.put("locale",r.locale);snapshot.put("subject",r.subject);snapshot.put("body",r.body);snapshot.put("recipients",emails);snapshot.put("correlationId",tokenOptional(correlationId,180));Notifikasi n=new Notifikasi();n.setJurnalPenelitianId(journalId);n.setJurnalTemplateKey(r.templateKey);n.setJurnalTemplateVersion(Integer.valueOf(r.version));n.setJurnalIdempotencyKey(token(idempotencyKey,180));n.setJurnalCorrelationId(tokenOptional(correlationId,180));n.setJurnalSnapshotJson(snapshot.toString());n.setNama(new JSONArray().toString());n.setEmails(emails.toString());n.setKeterangan(snapshot.toString());n.setWaktu(new Date());n.setBuka(Boolean.FALSE);String mode=clean(System.getenv("AIS_JURNAL_EMAIL_MODE")).toUpperCase();n.setStatusNotif("SMTP".equals(mode)?"QUEUED":"CAPTURED");n.setHasil("SMTP".equals(mode)?"JURNAL_EMAIL_QUEUE":"JURNAL_EMAIL_CAPTURE");n.setOlehId(actor.getUserId());s.save(n);if(own)tx.commit();return n;}catch(RuntimeException e){if(own&&tx.isActive())tx.rollback();throw e;}catch(Exception e){if(own&&tx.isActive())tx.rollback();throw new IllegalArgumentException("Snapshot email tidak valid.",e);}}
    private static String validateEmail(String v){String x=clean(v).toLowerCase();if(x.length()>254||!x.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))throw new IllegalArgumentException("Email penerima tidak valid.");return x;}private static String token(String v,int max){String x=clean(v);if(x.length()<2||x.length()>max||!x.matches("[A-Za-z0-9._:/-]+"))throw new IllegalArgumentException("Key korelasi email tidak valid.");return x;}private static String tokenOptional(String v,int max){return clean(v).length()==0?"":token(v,max);}private static void required(String v,String m){if(clean(v).length()==0)throw new IllegalArgumentException(m);}private static String clean(String v){return v==null?"":v.trim();}
}
