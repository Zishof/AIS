package ais.action.master.sosial.helper;

import java.util.Locale;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import ais.common.Common;
import ais.database.model.Tbmuser;

/** Immutable security context; tenant never comes from a browser parameter. */
public final class SocialRequestContext {
    private final String tenantKey, requestId, actorId, host;
    private final Tbmuser user;
    private SocialRequestContext(String tenantKey,String requestId,String actorId,String host,Tbmuser user){this.tenantKey=tenantKey;this.requestId=requestId;this.actorId=actorId;this.host=host;this.user=user;}
    static SocialRequestContext trusted(String tenantKey,String actorId){return new SocialRequestContext(tenantKey,SocialSecurity.reference("SYS"),actorId,"internal",null);}
    public static SocialRequestContext from(HttpServletRequest request){
        Tbmuser user=Common.getCurrentUser(request); String host=request.getServerName()==null?"unknown":request.getServerName().toLowerCase(Locale.ENGLISH);
        String tenant=null;
        try{if(user!=null&&user.getPerguruanTinggi()!=null&&user.getPerguruanTinggi().getId()!=null)tenant="PT:"+user.getPerguruanTinggi().getId();}catch(Exception ignored){}
        try{if(tenant==null&&user!=null&&user.getSekolah()!=null&&user.getSekolah().getId()!=null)tenant="SEKOLAH:"+user.getSekolah().getId();}catch(Exception ignored){}
        if(tenant==null)tenant="HOST:"+host.replaceAll("[^a-z0-9.-]","_");
        String rid=safeHeader(request.getHeader("X-Request-Id")); if(rid==null)rid=UUID.randomUUID().toString();
        String actor=user==null?"anonymous":String.valueOf(user.getUserId());
        return new SocialRequestContext(tenant,rid,actor,host,user);
    }
    private static String safeHeader(String v){if(v==null)return null;v=v.trim();return v.matches("[A-Za-z0-9._:-]{8,120}")?v:null;}
    public String getTenantKey(){return tenantKey;} public String getRequestId(){return requestId;} public String getActorId(){return actorId;} public String getHost(){return host;} public Tbmuser getUser(){return user;} public boolean isAuthenticated(){return user!=null;}
}
