package ais.action.servlet;

import java.io.BufferedReader; import java.io.IOException; import javax.servlet.http.*; import org.json.JSONObject;
import ais.action.master.jurnal.JurnalRateLimiter; import ais.action.master.sosial.helper.*;

/**
 * Servlet callback (webhook) yang menerima notifikasi pembayaran/donasi dari penyedia eksternal
 * "Smartlink" untuk modul sosial/donasi. Hanya menerima {@code POST} ({@code GET} ditolak dengan
 * 405). Setiap permintaan dibatasi laju lewat {@link JurnalRateLimiter} (maksimum 120 permintaan
 * per 60 detik per alamat IP pemanggil, kunci {@code "social-smartlink-callback"}), lalu badan
 * permintaan dibaca dengan batas ukuran 262144 byte (256 KiB) untuk mencegah payload berlebihan,
 * kemudian diverifikasi lewat {@link SocialSmartlinkCredentialService#verifyCallback} (mencocokkan
 * {@code order_id} pada payload ke kanal yang terdaftar, memeriksa alamat IP pengirim terhadap
 * daftar putih kanal, dan memvalidasi tanda tangan HMAC-SHA256 pada header
 * {@code X-Smartlink-Signature} secara constant-time) sebelum diproses oleh
 * {@link SocialCallbackService#process(String)}. Respons selalu JSON; kegagalan otorisasi/verifikasi
 * mengembalikan HTTP 401, payload tidak valid mengembalikan 422, dan galat tak terduga
 * mengembalikan 500 sekaligus dicatat ke {@link ais.common.ErrorAuditUtil}.
 *
 * <p>
 * <b>Catatan keamanan:</b> servlet ini sendiri tidak menyimpan kredensial — verifikasi tanda
 * tangan, alamat IP, dan rahasia callback didelegasikan ke {@link SocialSmartlinkCredentialService},
 * yang mengambil kredensial per kanal dari database secara terenkripsi. Tidak ditemukan kredensial
 * tertanam (hardcoded) pada file ini.
 * </p>
 */
public final class SosialSmartlinkCallback extends HttpServlet {
 private static final long serialVersionUID=1L;
 /** Menolak {@code GET} dengan status 405 — endpoint ini hanya menerima callback via {@code POST}. */
 protected void doGet(HttpServletRequest q,HttpServletResponse r)throws IOException{r.sendError(405);}
 /**
  * Menerima callback Smartlink: membatasi laju per-IP, membaca badan permintaan (maks. 256 KiB),
  * memverifikasi keaslian sumber (IP + signature HMAC) lewat {@link #verify}, lalu memprosesnya
  * lewat {@link SocialCallbackService#process(String)}. Selalu menulis respons JSON
  * {@code {"ok":..., "code":...}} dengan status HTTP yang sesuai (200 sukses, 401 tidak
  * terotorisasi, 422 payload tidak valid, 500 galat internal).
  */
 protected void doPost(HttpServletRequest q,HttpServletResponse r)throws IOException{JSONObject out=new JSONObject();r.setContentType("application/json; charset=UTF-8");r.setHeader("Cache-Control","no-store");try{if(!JurnalRateLimiter.allow("social-smartlink-callback",q.getRemoteAddr(),120,60000L))throw new SecurityException("Rate limit.");String raw=body(q,262144);verify(q,raw);out=new SocialCallbackService().process(raw);r.setStatus(200);}catch(SecurityException e){r.setStatus(401);fail(out,"UNAUTHORIZED");}catch(IllegalArgumentException e){r.setStatus(422);fail(out,"INVALID_CALLBACK");}catch(Exception e){r.setStatus(500);fail(out,"INTERNAL_ERROR");ais.common.ErrorAuditUtil.record(e,"SosialSmartlinkCallback");}finally{r.getWriter().write(out.toString());}}
 /** Mendelegasikan verifikasi callback (IP + signature HMAC) ke {@link SocialSmartlinkCredentialService#verifyCallback}. */
 private void verify(HttpServletRequest q,String raw){new SocialSmartlinkCredentialService().verifyCallback(q.getRemoteAddr(),q.getHeader("X-Smartlink-Signature"),raw);}
 /** Membaca badan permintaan sebagai teks, melempar {@link IOException} bila melebihi {@code max} byte (proteksi payload berlebihan). */
 private String body(HttpServletRequest q,int max)throws IOException{BufferedReader b=q.getReader();StringBuilder s=new StringBuilder();char[] c=new char[4096];int n;while((n=b.read(c))!=-1){s.append(c,0,n);if(s.length()>max)throw new IOException("Payload terlalu besar.");}return s.toString();}
 /** Mengisi payload respons galat standar {@code {"ok":false,"code":c}}. */
 private void fail(JSONObject o,String c){try{o.put("ok",false).put("code",c);}catch(Exception ignored){}}
}
