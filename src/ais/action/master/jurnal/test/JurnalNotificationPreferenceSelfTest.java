package ais.action.master.jurnal.test;

import java.util.Arrays;
import java.util.HashSet;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONObject;
import ais.action.master.jurnal.JurnalAdministrationService;
import ais.action.master.jurnal.JurnalNotificationPreferenceService;
import ais.common.JurnalAksesKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;

/** Rollback-only self-service preference/unsubscribe contract. */
public final class JurnalNotificationPreferenceSelfTest {
    public static void main(String[]args)throws Exception{if("ais".equalsIgnoreCase(System.getenv("AIS_JURNAL_DB_NAME")))throw new IllegalStateException("Test wajib memakai clone.");System.setProperty("javax.persistence.validation.mode","none");final Tbmuser actor=admin();Session s=HibernateUtil.currentSession();Transaction tx=s.beginTransaction();try{final JurnalPenelitian j=new JurnalAdministrationService().create("pref-test","Preference Self Test","preference-self-test","id_ID",actor);final JurnalNotificationPreferenceService service=new JurnalNotificationPreferenceService();check(service.allows(j.getId(),actor.getUserId(),"SUBMISSION_ACK","EMAIL"),"Default email harus aktif.");service.save(j.getId(),true,true,"DAILY",new HashSet<String>(Arrays.asList("SUBMISSION_ACK")),actor);check(!service.allows(j.getId(),actor.getUserId(),"SUBMISSION_ACK","EMAIL"),"Unsubscribe tidak diterapkan.");check(service.allows(j.getId(),actor.getUserId(),"PASSWORD_RESET_CONFIRM","EMAIL"),"Notifikasi keamanan harus tetap aktif.");service.save(j.getId(),false,true,"WEEKLY",new HashSet<String>(),actor);check(!service.allows(j.getId(),actor.getUserId(),"REVIEW_REQUEST","EMAIL"),"Email channel disable gagal.");check(service.allows(j.getId(),actor.getUserId(),"REVIEW_REQUEST","IN_APP"),"In-app channel harus aktif.");Number count=(Number)s.createQuery("select count(*) from RepoUserPreference where userId=:u and preferenceType='JOURNAL_NOTIFICATION' and label=:l and aktif=true").setString("u",actor.getUserId()).setString("l","journal:"+j.getId()).uniqueResult();check(count.intValue()==1,"Upsert preferensi membuat duplikasi.");expectInvalid(new Runnable(){public void run(){service.save(j.getId(),true,true,"DAILY",new HashSet<String>(Arrays.asList("CHANGE_EMAIL")),actor);}});System.out.println("JurnalNotificationPreferenceSelfTest OK default unsubscribe mandatory digest channels idempotent rollback");}finally{if(tx.isActive())tx.rollback();HibernateUtil.closeSession();Tbmuser.getUserRoleYgDipakai.remove(actor.getUserId());}System.exit(0);}
    private static void check(boolean x,String m){if(!x)throw new IllegalStateException(m);}private static void expectInvalid(Runnable r){try{r.run();throw new IllegalStateException("Payload wajib ditolak.");}catch(IllegalArgumentException expected){}}private static Tbmuser admin()throws Exception{Tbmrole r=new Tbmrole();r.setRoleId(Tbmrole.ADMINISTRATOR);JSONObject j=JurnalAksesKatalog.modelUntukEditor(null);HashSet<Menu>ms=new HashSet<Menu>();for(JurnalAksesKatalog.Entri e:JurnalAksesKatalog.DAFTAR){j.getJSONObject("menu").put(e.kunci,true);for(String a:JurnalAksesKatalog.AKSI_CRUD)j.getJSONObject("crud").getJSONObject(e.kunci).put(a,true);Menu m=new Menu();m.setId(Long.valueOf(2000000000L+e.child));ms.add(m);}r.setJurnalAksesJson(j.toString());r.setMenus(ms);Tbmuser u=new Tbmuser();u.setUserId("JRN_PREF_SELF_TEST");u.setUserRole(r);Tbmuser.getUserRoleYgDipakai.put(u.getUserId(),r);return u;}
}
