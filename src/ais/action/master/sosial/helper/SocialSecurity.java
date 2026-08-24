package ais.action.master.sosial.helper;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public final class SocialSecurity {
    private static final SecureRandom RANDOM=new SecureRandom(); private SocialSecurity(){}
    public static String csrf(HttpServletRequest request){HttpSession s=request.getSession(true);String t=(String)s.getAttribute("SOCIAL_CSRF");if(t==null){t=token(32);s.setAttribute("SOCIAL_CSRF",t);}return t;}
    public static void requireCsrf(HttpServletRequest request){String expected=(String)request.getSession(true).getAttribute("SOCIAL_CSRF");String actual=request.getHeader("X-CSRF-Token");if(actual==null)actual=request.getParameter("csrf");if(expected==null||actual==null||!constantEquals(expected,actual))throw new SecurityException("CSRF token tidak valid.");}
    public static String token(int bytes){byte[] b=new byte[bytes];RANDOM.nextBytes(b);return hex(b);}
    public static String reference(String prefix){return prefix+"-"+System.currentTimeMillis()+"-"+UUID.randomUUID().toString().replace("-","").substring(0,12).toUpperCase();}
    public static String sha256(String value){try{return hex(MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8")));}catch(Exception e){throw new IllegalStateException(e);}}
    public static String hmacSha256(String secret,String body){try{Mac m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(secret.getBytes("UTF-8"),"HmacSHA256"));return hex(m.doFinal(body.getBytes("UTF-8")));}catch(Exception e){throw new IllegalStateException(e);}}
    public static boolean constantEquals(String a,String b){try{return MessageDigest.isEqual(a.getBytes("UTF-8"),b.getBytes("UTF-8"));}catch(Exception e){return false;}}
    private static String hex(byte[] data){StringBuilder s=new StringBuilder(data.length*2);for(byte b:data)s.append(String.format("%02x",b&255));return s.toString();}
}
