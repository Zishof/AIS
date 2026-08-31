package ais.action.master.jurnal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;
import ais.database.model.repository.RepoUserPreference;

/** Self-service journal notification preferences stored in existing RepoUserPreference. */
public final class JurnalNotificationPreferenceService {
    private static final String TYPE="JOURNAL_NOTIFICATION";
    private static final Set<String> DIGESTS=Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("IMMEDIATE","DAILY","WEEKLY","NONE")));
    private static final Set<String> MANDATORY=Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("PASSWORD_RESET_CONFIRM","USER_VALIDATE_CONTEXT","USER_VALIDATE_SITE","CHANGE_EMAIL")));
    /**
     * Tipe implementasi bersarang {@link Preference} milik {@link JurnalNotificationPreferenceService}. Kelas ini
     * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * JurnalNotificationPreferenceService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code boolean email}, {@code boolean
     * inApp}, {@code String digest}, {@code Set unsubscribed}. Aturan bisnis bersama tetap berada pada kelas induk
     * atau service yang dipanggilnya.</p>
     *
     * @see JurnalNotificationPreferenceService
     */
    public static final class Preference{public boolean email=true,inApp=true;public String digest="IMMEDIATE";public final Set<String> unsubscribed=new HashSet<String>();}

    public Preference load(Long journalId,String userId){if(journalId==null||blank(userId))throw new IllegalArgumentException("Jurnal dan pengguna wajib diisi.");Session s=HibernateUtil.currentSession();RepoUserPreference row=row(s,journalId,userId);return row==null?new Preference():parse(row.getQueryValue());}
    public RepoUserPreference save(Long journalId,boolean email,boolean inApp,String digest,Set<String> unsubscribed,Tbmuser actor){
        if(actor==null||blank(actor.getUserId()))throw new SecurityException("Login diperlukan.");String d=upper(digest);if(!DIGESTS.contains(d))throw new IllegalArgumentException("Frekuensi digest tidak valid.");Set<String> keys=unsubscribed==null?Collections.<String>emptySet():unsubscribed;if(keys.size()>73)throw new IllegalArgumentException("Daftar unsubscribe tidak valid.");for(String key:keys)if(!JurnalEmailTemplateCatalog.contains(key)||MANDATORY.contains(key))throw new IllegalArgumentException("Template tidak dapat di-unsubscribe: "+key);
        Session s=HibernateUtil.currentSession();Transaction tx=s.getTransaction();boolean own=!tx.isActive();try{if(own)tx.begin();JurnalPenelitian journal=(JurnalPenelitian)s.get(JurnalPenelitian.class,journalId);if(journal==null||!Boolean.TRUE.equals(journal.getAktif()))throw new IllegalArgumentException("Jurnal tidak ditemukan.");RepoUserPreference p=row(s,journalId,actor.getUserId());if(p==null){p=new RepoUserPreference();p.setTenantKey(blank(journal.getTenantKey())?"default":journal.getTenantKey());p.setUserId(actor.getUserId());p.setPreferenceType(TYPE);p.setLabel(label(journalId));p.setCreatedAt(new Date());p.setAktif(Boolean.TRUE);}JSONArray a=new JSONArray();for(String key:keys)a.put(key);p.setQueryValue(new JSONObject().put("schemaVersion",1).put("email",email).put("inApp",inApp).put("digest",d).put("unsubscribed",a).toString());if(p.getId()==null)s.save(p);else s.update(p);if(own)tx.commit();return p;}catch(RuntimeException e){if(own&&tx.isActive())tx.rollback();throw e;}catch(Exception e){if(own&&tx.isActive())tx.rollback();throw new IllegalArgumentException("Preferensi notifikasi tidak valid.",e);}
    }
    public boolean allows(Long journalId,String userId,String templateKey,String channel){if(MANDATORY.contains(templateKey))return true;Preference p=load(journalId,userId);if(p.unsubscribed.contains(templateKey))return false;return"EMAIL".equalsIgnoreCase(channel)?p.email:"IN_APP".equalsIgnoreCase(channel)?p.inApp:false;}
    private static RepoUserPreference row(Session s,Long journalId,String userId){Query q=s.createQuery("from RepoUserPreference where userId=:u and preferenceType=:p and label=:l and aktif=true order by id desc");q.setString("u",userId.trim());q.setString("p",TYPE);q.setString("l",label(journalId));q.setMaxResults(1);return(RepoUserPreference)q.uniqueResult();}
    private static Preference parse(String raw){try{JSONObject j=new JSONObject(raw);if(j.optInt("schemaVersion",0)!=1)throw new Exception();Preference p=new Preference();p.email=j.optBoolean("email",true);p.inApp=j.optBoolean("inApp",true);p.digest=upper(j.optString("digest","IMMEDIATE"));if(!DIGESTS.contains(p.digest))throw new Exception();JSONArray a=j.optJSONArray("unsubscribed");if(a!=null)for(int i=0;i<a.length();i++){String key=a.getString(i);if(JurnalEmailTemplateCatalog.contains(key)&&!MANDATORY.contains(key))p.unsubscribed.add(key);}return p;}catch(Exception e){throw new IllegalStateException("Kontrak preferensi notifikasi rusak.",e);}}
    private static String label(Long id){return"journal:"+id;}private static String upper(String v){return v==null?"":v.trim().toUpperCase(Locale.ENGLISH);}private static boolean blank(String v){return v==null||v.trim().length()==0;}
}
