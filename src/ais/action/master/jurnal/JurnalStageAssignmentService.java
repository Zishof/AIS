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
    private final JurnalAuthorizationService auth=new JurnalAuthorizationService();
    public PenugasanTahapJurnal assign(Long journalId,Long itemId,String userId,String role,String stage,
            String section,Date starts,Date ends,String provenanceJson,Tbmuser actor){authorize(stage,actor);String st=stage(stage),rl=role(role);Date from=starts==null?new Date():starts;if(ends!=null&&!ends.after(from))throw new IllegalArgumentException("Masa penugasan tidak valid.");Session s=HibernateUtil.currentSession();Transaction tx=s.getTransaction();boolean own=!tx.isActive();try{if(own)tx.begin();JurnalPenelitian j=(JurnalPenelitian)s.get(JurnalPenelitian.class,journalId);RepoItem item=itemId==null?null:(RepoItem)s.get(RepoItem.class,itemId);if(j==null||(item!=null&&!j.getRepoCollectionId().equals(item.getCollectionId())))throw new SecurityException("Scope penugasan berbeda jurnal.");Query q=s.createQuery("from PenugasanTahapJurnal where jurnalPenelitianId=:j and userId=:u and roleKey=:r and stageKey=:s and ((itemId is null and :i is null) or itemId=:i) and status='ACTIVE' and aktif=true");q.setLong("j",journalId);q.setString("u",clean(userId));q.setString("r",rl);q.setString("s",st);if(itemId==null)q.setParameter("i",null);else q.setLong("i",itemId);q.setMaxResults(1);PenugasanTahapJurnal a=(PenugasanTahapJurnal)q.uniqueResult();if(a==null){a=new PenugasanTahapJurnal();a.setTenantKey(j.getTenantKey());a.setJurnalPenelitianId(journalId);a.setItemId(itemId);a.setUserId(clean(userId));a.setRoleKey(rl);a.setStageKey(st);a.setStatus("ACTIVE");a.setCreatedBy(actor.getUserId());a.setCreatedAt(new Date());a.setAktif(Boolean.TRUE);}a.setSectionKey(clean(section));a.setStartsAt(from);a.setEndsAt(ends);a.setProvenanceJson(validateJson(provenanceJson));a.setUpdatedAt(new Date());if(a.getId()==null)s.save(a);else s.update(a);if(own)tx.commit();return a;}catch(RuntimeException e){if(own&&tx.isActive())tx.rollback();throw e;}}
    public void end(Long assignmentId,Date endedAt,Tbmuser actor){Session s=HibernateUtil.currentSession();Transaction tx=s.getTransaction();boolean own=!tx.isActive();try{if(own)tx.begin();PenugasanTahapJurnal a=(PenugasanTahapJurnal)s.get(PenugasanTahapJurnal.class,assignmentId);if(a==null)throw new IllegalArgumentException("Penugasan tidak ditemukan.");authorize(a.getStageKey(),actor);a.setStatus("ENDED");a.setEndsAt(endedAt==null?new Date():endedAt);a.setUpdatedAt(new Date());s.update(a);if(own)tx.commit();}catch(RuntimeException e){if(own&&tx.isActive())tx.rollback();throw e;}}
    private void authorize(String stage,Tbmuser actor){String s=stage(stage);if("COPYEDITING".equals(s))auth.requireCrud(actor,"copyediting","update");else if("PRODUCTION".equals(s)||"PROOF".equals(s))auth.requireCrud(actor,"produksiGalley","update");else auth.requireWorkflow(actor,"assignEditor");}
    private static String stage(String v){String x=clean(v).toUpperCase(Locale.ENGLISH);if(!x.matches("JOURNAL|SECTION|SUBMISSION|REVIEW|COPYEDITING|PRODUCTION|PROOF"))throw new IllegalArgumentException("Tahap penugasan tidak valid.");return x;}private static String role(String v){String x=clean(v).toUpperCase(Locale.ENGLISH);if(!x.matches("MANAGER|EDITOR|SECTION_EDITOR|COPYEDITOR|PRODUCTION|PROOFREADER"))throw new IllegalArgumentException("Peran penugasan tidak valid.");return x;}private static String validateJson(String v){if(clean(v).length()==0)return"{\"schemaVersion\":1}";if(v.length()>65536)throw new IllegalArgumentException("Provenance terlalu besar.");try{org.json.JSONObject o=new org.json.JSONObject(v);if(o.optInt("schemaVersion",0)!=1)throw new Exception();return o.toString();}catch(Exception e){throw new IllegalArgumentException("Provenance penugasan tidak valid.",e);}}private static String clean(String v){return v==null?"":v.trim();}
}
