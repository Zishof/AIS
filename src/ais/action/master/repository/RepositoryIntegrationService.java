package ais.action.master.repository;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.util.Date;
import org.apache.commons.codec.binary.Base64;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.repository.RepoIntegrationEvent;
import ais.database.model.repository.RepoItem;

/** Adapter integrasi eksternal. Semua layanan nonaktif secara aman sampai konfigurasi tersedia. */
public class RepositoryIntegrationService {
    private final RepositoryWorkflowService workflow=new RepositoryWorkflowService();
    public static class Result{public boolean success,configured;public String status="",message="",externalId="";}

    public Result mintOrUpdateDoi(Long itemId,String landingUrl,Tbmuser actor,String requestId){
        requireAdmin(actor);Session session=HibernateUtil.openSession();Transaction tx=null;RepoItem item=null;String payload="";
        try{tx=session.beginTransaction();item=(RepoItem)session.get(RepoItem.class,itemId);requiredTenant(item);String endpoint=property("ais.repository.dataciteUrl","https://api.test.datacite.org");String user=property("ais.repository.dataciteUser","");String password=property("ais.repository.datacitePassword","");String prefix=property("ais.repository.datacitePrefix","");
            if(user.length()==0||password.length()==0||prefix.length()==0){rollback(tx);return auditResult(itemId,"DATACITE","MINT_OR_UPDATE","NOT_CONFIGURED",payload,"","DataCite credentials/prefix belum dikonfigurasi.",actor,requestId,false,false);}
            String doi=clean(item.getDoi());if(doi.length()==0)doi=prefix+"/ais."+item.getId();payload=dataCitePayload(item,doi,landingUrl).toString();
            String method=clean(item.getDoi()).length()==0?"POST":"PUT";String target=endpoint+"/dois"+("PUT".equals(method)?"/"+URLEncoder.encode(doi,"UTF-8"):"");HttpResult response=http(method,target,payload,"Basic "+new String(Base64.encodeBase64((user+":"+password).getBytes("UTF-8")),"US-ASCII"));
            if(response.code>=200&&response.code<300){item.setDoi(doi);item.setDoiState(property("ais.repository.dataciteEvent","register").equals("publish")?"FINDABLE":"REGISTERED");item.setDoiUpdatedAt(new Date());session.update(item);tx.commit();return auditResult(itemId,"DATACITE","MINT_OR_UPDATE","SUCCESS",payload,response.body,"",actor,requestId,true,true);}
            rollback(tx);return auditResult(itemId,"DATACITE","MINT_OR_UPDATE","FAILED",payload,response.body,"HTTP "+response.code,actor,requestId,false,true);
        }catch(Exception e){rollback(tx);return auditResult(itemId,"DATACITE","MINT_OR_UPDATE","FAILED",payload,"",e.getMessage(),actor,requestId,false,true);}finally{HibernateUtil.closeSessionQuietly(session);}
    }

    public Result sendCoarNotify(Long itemId,String landingUrl,String relationUrl,Tbmuser actor,String requestId){
        requireAdmin(actor);Session session=HibernateUtil.openSession();try{RepoItem item=(RepoItem)session.get(RepoItem.class,itemId);requiredTenant(item);String endpoint=property("ais.repository.coarNotifyUrl","");String token=property("ais.repository.coarNotifyToken","");JSONObject payload=new JSONObject();payload.put("@context",new JSONArray().put("https://www.w3.org/ns/activitystreams").put("https://purl.org/coar/notify"));payload.put("id",landingUrl+"#notify-"+requestId);payload.put("type","Announce");payload.put("actor",new JSONObject().put("id",landingUrl).put("type","Service"));payload.put("object",new JSONObject().put("id",landingUrl).put("type","sorg:ScholarlyArticle"));if(clean(relationUrl).length()>0)payload.put("target",new JSONObject().put("id",relationUrl));
            if(endpoint.length()==0)return auditResult(itemId,"COAR_NOTIFY","ANNOUNCE","NOT_CONFIGURED",payload.toString(),"","COAR Notify endpoint belum dikonfigurasi.",actor,requestId,false,false);HttpResult response=http("POST",endpoint,payload.toString(),token.length()==0?"":"Bearer "+token);boolean ok=response.code>=200&&response.code<300;return auditResult(itemId,"COAR_NOTIFY","ANNOUNCE",ok?"SUCCESS":"FAILED",payload.toString(),response.body,ok?"":"HTTP "+response.code,actor,requestId,ok,true);
        }catch(Exception e){return auditResult(itemId,"COAR_NOTIFY","ANNOUNCE","FAILED","","",e.getMessage(),actor,requestId,false,true);}finally{HibernateUtil.closeSessionQuietly(session);}
    }

    public String orcidAuthorizationUrl(String state){String client=property("ais.repository.orcidClientId","");String redirect=property("ais.repository.orcidRedirectUri","");if(client.length()==0||redirect.length()==0)return "";try{return property("ais.repository.orcidAuthorizeUrl","https://orcid.org/oauth/authorize")+"?client_id="+URLEncoder.encode(client,"UTF-8")+"&response_type=code&scope=%2Fauthenticate&redirect_uri="+URLEncoder.encode(redirect,"UTF-8")+"&state="+URLEncoder.encode(clean(state),"UTF-8");}catch(Exception e){return "";}}
    public boolean dataCiteConfigured(){return property("ais.repository.dataciteUser","").length()>0&&property("ais.repository.datacitePassword","").length()>0&&property("ais.repository.datacitePrefix","").length()>0;}
    public boolean coarConfigured(){return property("ais.repository.coarNotifyUrl","").length()>0;}
    public boolean orcidConfigured(){return property("ais.repository.orcidClientId","").length()>0&&property("ais.repository.orcidRedirectUri","").length()>0;}
    public boolean rorConfigured(){return property("ais.repository.rorUrl","https://api.ror.org").length()>0;}
    public boolean aiConfigured(){return property("ais.repository.aiEndpoint","").length()>0;}

    private JSONObject dataCitePayload(RepoItem item,String doi,String url)throws Exception{JSONObject attributes=new JSONObject();attributes.put("doi",doi);attributes.put("event",property("ais.repository.dataciteEvent","register"));attributes.put("url",url);attributes.put("titles",new JSONArray().put(new JSONObject().put("title",item.getTitle())));JSONArray creators=new JSONArray();for(String author:item.getAuthors().split("[;]"))if(clean(author).length()>0)creators.put(new JSONObject().put("name",clean(author)));attributes.put("creators",creators);attributes.put("publisher",clean(item.getPublisher()).length()==0?"Repository AIS":item.getPublisher());attributes.put("publicationYear",item.getIssuedAt()==null?new java.text.SimpleDateFormat("yyyy").format(new Date()):new java.text.SimpleDateFormat("yyyy").format(item.getIssuedAt()));attributes.put("types",new JSONObject().put("resourceTypeGeneral","Text").put("resourceType",item.getDocumentType()));return new JSONObject().put("data",new JSONObject().put("type","dois").put("attributes",attributes));}
    private HttpResult http(String method,String url,String payload,String authorization)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(20000);c.setRequestMethod(method);c.setRequestProperty("Accept","application/vnd.api+json, application/json");c.setRequestProperty("Content-Type","application/vnd.api+json");if(clean(authorization).length()>0)c.setRequestProperty("Authorization",authorization);c.setDoOutput(true);OutputStream out=c.getOutputStream();try{out.write(payload.getBytes("UTF-8"));}finally{out.close();}HttpResult r=new HttpResult();r.code=c.getResponseCode();InputStream in=r.code>=400?c.getErrorStream():c.getInputStream();r.body=read(in,200000);c.disconnect();return r;}
    private String read(InputStream in,int max)throws Exception{if(in==null)return "";ByteArrayOutputStream out=new ByteArrayOutputStream();try{byte[]b=new byte[4096];int n,total=0;while((n=in.read(b))>=0&&total<max){int use=Math.min(n,max-total);out.write(b,0,use);total+=use;}}finally{in.close();}return new String(out.toByteArray(),"UTF-8");}
    private Result auditResult(Long itemId,String service,String action,String status,String request,String response,String error,Tbmuser actor,String requestId,boolean success,boolean configured){Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();RepoIntegrationEvent e=new RepoIntegrationEvent();e.setItemId(itemId);e.setTenantKey(RepositoryTenantScope.currentKey());e.setServiceName(service);e.setActionName(action);e.setStatus(status);e.setActorId(actor==null?"":actor.getUserId());e.setRequestId(clean(requestId));e.setRequestPayload(limit(request,50000));e.setResponsePayload(limit(response,50000));e.setErrorMessage(limit(error,10000));e.setCreatedAt(new Date());s.save(e);tx.commit();}catch(Exception ex){rollback(tx);ais.common.ErrorAuditUtil.record(ex,"RepositoryIntegrationService.audit");}finally{HibernateUtil.closeSessionQuietly(s);}Result r=new Result();r.success=success;r.configured=configured;r.status=status;r.message=clean(error).length()>0?error:status;return r;}
    private void requireAdmin(Tbmuser actor){if(!workflow.isRepositoryAdmin(actor))throw new SecurityException("Hak administrator repository diperlukan.");}
    private void requiredTenant(RepoItem item){if(item==null||!RepositoryTenantScope.currentKey().equals(item.getTenantKey()))throw new IllegalArgumentException("Item repository tidak ditemukan.");}
    private static String property(String name,String fallback){return clean(System.getProperty(name,fallback));}private static String clean(String v){return v==null?"":v.trim();}private static String limit(String v,int max){String x=clean(v);return x.length()>max?x.substring(0,max):x;}private static void rollback(Transaction tx){if(tx!=null&&tx.isActive())try{tx.rollback();}catch(Exception ignored){}}
    private static class HttpResult{int code;String body="";}
}
