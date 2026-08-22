package ais.action.master.jurnal;

import org.hibernate.Session;
import org.json.JSONObject;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Tbmuser;

/** Authenticated readiness detail without credentials or database names. */
public final class JurnalHealthService {
    private final JurnalAuthorizationService auth=new JurnalAuthorizationService();
    public JSONObject check(Tbmuser actor){auth.requireCrud(actor,"operations","read");JSONObject out=new JSONObject();Session streaming=null;org.hibernate.Transaction stx=null;try{Number main=(Number)HibernateUtil.currentSession().createSQLQuery("select count(*) from information_schema.tables where table_schema='penelitiandanpengabdian' and table_name in ('langganan_jurnal','import_job_ojs','template_email_jurnal')").uniqueResult();streaming=StreamingHibernateUtil.getInstance().currentSession();stx=streaming.beginTransaction();Number blob=(Number)streaming.createSQLQuery("select count(*) from information_schema.tables where table_schema='public' and table_name='lampiran_jurnal'").uniqueResult();stx.commit();boolean ready=main!=null&&main.intValue()==3&&blob!=null&&blob.intValue()==1;out.put("status",ready?"UP":"DOWN").put("mainJournalSchema",main==null?0:main.intValue()).put("streamingSchema",blob==null?0:blob.intValue()).put("schemaMutation","DISABLED");return out;}catch(Exception e){try{ais.common.ErrorAuditUtil.record(e,"JurnalHealthService.check");return out.put("status","DOWN").put("error","DEPENDENCY_UNAVAILABLE");}catch(Exception ignored){throw new IllegalStateException(e);}}finally{if(stx!=null&&stx.isActive())try{stx.rollback();}catch(Exception ignored){}if(streaming!=null)try{StreamingHibernateUtil.getInstance().closeSession();}catch(Exception ignored){}}}
}
