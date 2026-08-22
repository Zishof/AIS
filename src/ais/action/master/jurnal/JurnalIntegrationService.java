package ais.action.master.jurnal;

import java.util.Date;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.repository.RepoIntegrationEvent;

/** Audited, idempotent attempt ledger for DOI/deposit/export/payment integrations. */
public final class JurnalIntegrationService {
    private final JurnalAuthorizationService auth=new JurnalAuthorizationService();

    public RepoIntegrationEvent begin(Long itemId,String tenant,String service,String action,String requestId,
            String sanitizedPayload,Tbmuser actor){
        authorize(actor,service,action);required(requestId,"Request ID wajib diisi.");
        Session s=HibernateUtil.currentSession();Transaction tx=s.getTransaction();boolean own=!tx.isActive();
        try{if(own)tx.begin();Query q=s.createQuery("from RepoIntegrationEvent where tenantKey=:t and serviceName=:s and actionName=:a and requestId=:r order by id desc");q.setString("t",clean(tenant));q.setString("s",upper(service));q.setString("a",upper(action));q.setString("r",clean(requestId));q.setMaxResults(1);RepoIntegrationEvent existing=(RepoIntegrationEvent)q.uniqueResult();if(existing!=null){if(own)tx.commit();return existing;}RepoIntegrationEvent e=new RepoIntegrationEvent();e.setItemId(itemId);e.setTenantKey(clean(tenant));e.setServiceName(upper(service));e.setActionName(upper(action));e.setStatus("PENDING");e.setActorId(actor.getUserId());e.setRequestId(clean(requestId));e.setRequestPayload(limit(sanitizedPayload,262144));e.setCreatedAt(new Date());s.save(e);if(own)tx.commit();return e;}catch(RuntimeException e){if(own&&tx.isActive())tx.rollback();throw e;}
    }

    public RepoIntegrationEvent finish(Long eventId,boolean success,String sanitizedResponse,String error,Tbmuser actor){
        Session s=HibernateUtil.currentSession();Transaction tx=s.getTransaction();boolean own=!tx.isActive();try{if(own)tx.begin();RepoIntegrationEvent e=(RepoIntegrationEvent)s.get(RepoIntegrationEvent.class,eventId);if(e==null)throw new IllegalArgumentException("Attempt integrasi tidak ditemukan.");authorize(actor,e.getServiceName(),e.getActionName());if(!"PENDING".equals(e.getStatus())&&!"RETRYING".equals(e.getStatus()))return e;e.setStatus(success?"SUCCEEDED":"FAILED");e.setResponsePayload(limit(sanitizedResponse,262144));e.setErrorMessage(limit(error,4000));s.update(e);if(own)tx.commit();return e;}catch(RuntimeException e){if(own&&tx.isActive())tx.rollback();throw e;}
    }

    public RepoIntegrationEvent retry(Long failedEventId,String newRequestId,Tbmuser actor){Session s=HibernateUtil.currentSession();RepoIntegrationEvent failed=(RepoIntegrationEvent)s.get(RepoIntegrationEvent.class,failedEventId);if(failed==null||!"FAILED".equals(failed.getStatus()))throw new IllegalArgumentException("Attempt gagal tidak ditemukan.");return begin(failed.getItemId(),failed.getTenantKey(),failed.getServiceName(),failed.getActionName(),newRequestId,failed.getRequestPayload(),actor);}
    private void authorize(Tbmuser actor,String service,String action){String s=upper(service);if("CROSSREF".equals(s)||"DATACITE".equals(s)||"DOI".equals(s)||"URN".equals(s))auth.requireWorkflow(actor,"manageIdentifier");else auth.requireCrud(actor,"pluginIntegrasi","update");}
    private static String upper(String v){return clean(v).toUpperCase(java.util.Locale.ENGLISH);}private static String clean(String v){return v==null?"":v.trim();}private static String limit(String v,int n){return v==null?null:(v.length()<=n?v:v.substring(0,n));}private static void required(String v,String m){if(clean(v).length()==0)throw new IllegalArgumentException(m);}
}
