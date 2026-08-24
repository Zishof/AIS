package ais.action.servlet;

import java.io.BufferedReader; import java.io.IOException; import javax.servlet.http.*; import org.json.JSONObject;
import ais.action.master.jurnal.JurnalRateLimiter; import ais.action.master.sosial.helper.*; import ais.common.Common;

public final class SosialSmartlinkCallback extends HttpServlet {
 private static final long serialVersionUID=1L;
 protected void doGet(HttpServletRequest q,HttpServletResponse r)throws IOException{r.sendError(405);} protected void doPost(HttpServletRequest q,HttpServletResponse r)throws IOException{JSONObject out=new JSONObject();r.setContentType("application/json; charset=UTF-8");r.setHeader("Cache-Control","no-store");try{if(!JurnalRateLimiter.allow("social-smartlink-callback",q.getRemoteAddr(),120,60000L))throw new SecurityException("Rate limit.");String raw=body(q,262144);verify(q,raw);out=new SocialCallbackService().process(raw);r.setStatus(200);}catch(SecurityException e){r.setStatus(401);fail(out,"UNAUTHORIZED");}catch(IllegalArgumentException e){r.setStatus(422);fail(out,"INVALID_CALLBACK");}catch(Exception e){r.setStatus(500);fail(out,"INTERNAL_ERROR");ais.common.ErrorAuditUtil.record(e,"SosialSmartlinkCallback");}finally{r.getWriter().write(out.toString());}}
 private void verify(HttpServletRequest q,String raw){String secret=config("sosial_smartlink_callback_secret");String allowed=config("sosial_smartlink_callback_allowed_ips");if(secret==null||secret.length()<16||allowed==null||allowed.trim().isEmpty())throw new SecurityException("Callback security belum dikonfigurasi.");boolean ip=false;for(String x:allowed.split(","))if(q.getRemoteAddr().equals(x.trim()))ip=true;if(!ip)throw new SecurityException("Source ditolak.");String supplied=q.getHeader("X-Smartlink-Signature");if(supplied==null||!SocialSecurity.constantEquals(SocialSecurity.hmacSha256(secret,raw),supplied.trim().toLowerCase()))throw new SecurityException("Signature ditolak.");}
 private String config(String k){try{return Common.getKonfigurasi(k,"").getNilai();}catch(Exception e){return null;}}
 private String body(HttpServletRequest q,int max)throws IOException{BufferedReader b=q.getReader();StringBuilder s=new StringBuilder();char[] c=new char[4096];int n;while((n=b.read(c))!=-1){s.append(c,0,n);if(s.length()>max)throw new IOException("Payload terlalu besar.");}return s.toString();}
 private void fail(JSONObject o,String c){try{o.put("ok",false).put("code",c);}catch(Exception ignored){}}
}
