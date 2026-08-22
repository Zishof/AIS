package ais.action.master.jurnal;

import java.net.URI;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import org.hibernate.Session;
import org.hibernate.Transaction;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.repository.RepoUsageEvent;

/** Privacy-bounded VIEW/DOWNLOAD capture using the existing repository event table. */
public final class JurnalUsageEventService {
    private static final byte[] HASH_SECRET=secret();
    private static final long DEDUP_MILLIS=30000L;
    public boolean record(Long itemId,Long bitstreamId,String type,Tbmuser actor,HttpServletRequest request){
        if(request==null||"1".equals(request.getHeader("DNT"))||"1".equals(request.getHeader("Sec-GPC")))return false;
        return record(itemId,bitstreamId,type,actor,request.getRemoteAddr(),request.getHeader("User-Agent"),request.getHeader("Referer"),new Date());
    }
    public boolean record(Long itemId,Long bitstreamId,String type,Tbmuser actor,String remote,String agent,String referrer,Date occurredAt){
        if(itemId==null||itemId.longValue()<1||!("VIEW".equals(type)||"DOWNLOAD".equals(type)))throw new IllegalArgumentException("Event usage tidak valid.");
        Date at=occurredAt==null?new Date():occurredAt;String visitor=visitorHash(remote,agent,at),agentClass=agentClass(agent),host=referrerHost(referrer);
        Session s=HibernateUtil.getSessionFactory().openSession();Transaction tx=s.beginTransaction();try{
            boolean inserted=recordInSession(s,itemId,bitstreamId,type,actor,remote,agent,referrer,at);tx.commit();return inserted;
        }catch(RuntimeException e){if(tx.isActive())tx.rollback();throw e;}finally{s.close();}
    }
    public boolean recordInSession(Session s,Long itemId,Long bitstreamId,String type,Tbmuser actor,String remote,String agent,String referrer,Date occurredAt){
        if(s==null||itemId==null||itemId.longValue()<1||!("VIEW".equals(type)||"DOWNLOAD".equals(type)))throw new IllegalArgumentException("Event usage tidak valid.");Date at=occurredAt==null?new Date():occurredAt;String visitor=visitorHash(remote,agent,at);
        Number duplicate=(Number)s.createQuery("select count(*) from RepoUsageEvent where itemId=:i and eventType=:t and visitorHash=:v and occurredAt>=:since").setLong("i",itemId).setString("t",type).setString("v",visitor).setTimestamp("since",new Date(at.getTime()-DEDUP_MILLIS)).uniqueResult();if(duplicate.longValue()>0)return false;
        RepoUsageEvent e=new RepoUsageEvent();e.setItemId(itemId);e.setBitstreamId(bitstreamId);e.setEventType(type);e.setVisitorHash(visitor);e.setActorId(actor==null?null:actor.getUserId());e.setUserAgentClass(agentClass(agent));e.setReferrerHost(referrerHost(referrer));e.setOccurredAt(at);s.save(e);s.flush();return true;
    }
    public static String visitorHash(String remote,String agent,Date at){try{SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd");f.setTimeZone(TimeZone.getTimeZone("UTC"));String material=f.format(at==null?new Date():at)+"|"+safe(remote,128)+"|"+safe(agent,512);Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(HASH_SECRET,"HmacSHA256"));return hex(mac.doFinal(material.getBytes("UTF-8")));}catch(Exception e){throw new IllegalStateException("Hash usage gagal.",e);}}
    public static String agentClass(String value){String x=safe(value,512).toLowerCase(Locale.ENGLISH);return x.matches(".*(bot|spider|crawler|slurp|monitor|uptime|headless|curl|wget).*" )?"BOT":"BROWSER";}
    public static String referrerHost(String value){try{if(value==null||value.trim().length()==0)return null;URI uri=new URI(value.trim());String host=uri.getHost();if(host==null||host.length()>253||!host.matches("[A-Za-z0-9.-]+"))return null;return host.toLowerCase(Locale.ENGLISH);}catch(Exception e){return null;}}
    private static byte[] secret(){String configured=System.getenv("AIS_JURNAL_USAGE_HASH_SECRET");if(configured!=null&&configured.length()>=32)return configured.getBytes(java.nio.charset.Charset.forName("UTF-8"));byte[] out=new byte[32];new SecureRandom().nextBytes(out);return out;}
    private static String safe(String v,int max){if(v==null)return"";String x=v.replace('\r',' ').replace('\n',' ').trim();return x.length()>max?x.substring(0,max):x;}
    private static String hex(byte[] bytes){StringBuilder b=new StringBuilder();for(byte x:bytes)b.append(String.format("%02x",x&255));return b.toString();}
}
