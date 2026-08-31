package ais.action.master.sosial.helper;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Kumpulan utilitas keamanan untuk modul sosial (donasi/smartlink): pembangkitan token CSRF
 * per-sesi, token acak dan referensi transaksi, hashing SHA-256, HMAC-SHA256 untuk verifikasi
 * signature (mis. webhook payment gateway), serta perbandingan string waktu-konstan untuk
 * mencegah timing attack pada perbandingan token/signature rahasia.
 */
public final class SocialSecurity {
    private static final SecureRandom RANDOM=new SecureRandom(); private SocialSecurity(){}
    /** Mengembalikan token CSRF yang tersimpan pada sesi HTTP saat ini, membangkitkan dan menyimpannya (32 byte acak) bila belum ada. */
    public static String csrf(HttpServletRequest request){HttpSession s=request.getSession(true);String t=(String)s.getAttribute("SOCIAL_CSRF");if(t==null){t=token(32);s.setAttribute("SOCIAL_CSRF",t);}return t;}
    /** Memvalidasi token CSRF pada permintaan (header {@code X-CSRF-Token} atau parameter {@code csrf}) terhadap token tersimpan di sesi, memakai perbandingan waktu-konstan; melempar {@link SecurityException} bila tidak cocok atau tidak ada. */
    public static void requireCsrf(HttpServletRequest request){String expected=(String)request.getSession(true).getAttribute("SOCIAL_CSRF");String actual=request.getHeader("X-CSRF-Token");if(actual==null)actual=request.getParameter("csrf");if(expected==null||actual==null||!constantEquals(expected,actual))throw new SecurityException("CSRF token tidak valid.");}
    /** Membangkitkan token acak kriptografis sepanjang {@code bytes} byte, dikodekan sebagai heksadesimal. */
    public static String token(int bytes){byte[] b=new byte[bytes];RANDOM.nextBytes(b);return hex(b);}
    public static String reference(String prefix){return prefix+"-"+System.currentTimeMillis()+"-"+UUID.randomUUID().toString().replace("-","").substring(0,12).toUpperCase();}
    public static String sha256(String value){try{return hex(MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8")));}catch(Exception e){throw new IllegalStateException(e);}}
    public static String hmacSha256(String secret,String body){try{Mac m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(secret.getBytes("UTF-8"),"HmacSHA256"));return hex(m.doFinal(body.getBytes("UTF-8")));}catch(Exception e){throw new IllegalStateException(e);}}
    public static boolean constantEquals(String a,String b){try{return MessageDigest.isEqual(a.getBytes("UTF-8"),b.getBytes("UTF-8"));}catch(Exception e){return false;}}
    private static String hex(byte[] data){StringBuilder s=new StringBuilder(data.length*2);for(byte b:data)s.append(String.format("%02x",b&255));return s.toString();}
}
