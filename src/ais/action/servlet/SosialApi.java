package ais.action.servlet;

import java.io.IOException; import java.math.BigDecimal; import javax.servlet.http.*; import org.hibernate.Session; import org.hibernate.Transaction; import org.json.JSONObject;
import ais.action.master.jurnal.JurnalRateLimiter; import ais.action.master.sosial.helper.*; import ais.database.hibernate.HibernateUtil; import ais.database.model.sosial.*;

/**
 * Servlet API JSON publik untuk modul donasi sosial ("smartlink"): menangani perhitungan zakat,
 * pembuatan transaksi donasi, inisiasi pembayaran gateway, dan pengecekan status pembayaran lewat
 * satu titik masuk berbasis parameter {@code action} ({@code calculate}, {@code donation},
 * {@code payment}, {@code payment-status}). Permintaan GET diarahkan sebagai non-mutasi (hanya
 * {@code payment-status} yang valid via GET), sedangkan {@code calculate}/{@code donation}/
 * {@code payment} mewajibkan POST.
 *
 * <p>
 * Setiap permintaan diberi id pelacakan ({@code X-Request-Id}) dan dibatasi laju
 * ({@link JurjRateLimiter#allow ais.action.master.jurnal.JurnalRateLimiter}, 60 permintaan/60 detik
 * per alamat IP dengan kunci {@code "social-api"}) sebelum diproses lebih lanjut. Permintaan mutasi
 * (POST) wajib menyertakan token CSRF valid ({@link SocialSecurity#requireCsrf}). Header respons
 * memaksa {@code no-store} dan {@code nosniff} agar hasil API tidak ter-cache atau di-sniff sebagai
 * tipe konten lain. Kegagalan dipetakan ke kode HTTP: {@link SecurityException}→403,
 * {@link IllegalArgumentException}→422 (validasi), {@link IllegalStateException}→409 (belum siap),
 * lainnya→500 dengan pesan generik memuat id pelacakan (detail sesungguhnya dicatat lewat
 * {@code ErrorAuditUtil}, tidak dibocorkan ke klien).
 * </p>
 */
public final class SosialApi extends HttpServlet {
 private static final long serialVersionUID=1L;
 /** Menangani GET: hanya action non-mutasi ({@code payment-status}) yang valid. */
 protected void doGet(HttpServletRequest q,HttpServletResponse r)throws IOException{respond(q,r,false);}
 /** Menangani POST: mengizinkan action mutasi ({@code calculate}/{@code donation}/{@code payment}) setelah verifikasi CSRF. */
 protected void doPost(HttpServletRequest q,HttpServletResponse r)throws IOException{respond(q,r,true);}
 /** Titik masuk bersama GET/POST: menerapkan rate limit, CSRF (untuk mutasi), lalu mendelegasikan ke handler sesuai parameter {@code action} dan menuliskan hasil JSON ke respons. Lihat javadoc kelas untuk pemetaan kode HTTP. */
 private void respond(HttpServletRequest q,HttpServletResponse r,boolean mutation)throws IOException{JSONObject out=new JSONObject();String trace=SocialSecurity.reference("REQ");r.setContentType("application/json; charset=UTF-8");r.setHeader("Cache-Control","no-store");r.setHeader("X-Content-Type-Options","nosniff");r.setHeader("X-Request-Id",trace);try{if(!JurnalRateLimiter.allow("social-api",q.getRemoteAddr(),60,60000L)){r.setStatus(429);fail(out,"RATE_LIMITED","Terlalu banyak permintaan.");return;}if(mutation)SocialSecurity.requireCsrf(q);SocialRequestContext c=SocialRequestContext.from(q);String action=value(q,"action");
   if("calculate".equals(action)){requirePost(mutation);out=calculate(q,c);}else if("donation".equals(action)){requirePost(mutation);out=donation(q,c);}else if("payment".equals(action)){requirePost(mutation);out=payment(q,c);}else if("payment-status".equals(action)){out=paymentStatus(q,c);}else throw new IllegalArgumentException("Action tidak dikenal.");
  }catch(SecurityException e){r.setStatus(403);fail(out,"FORBIDDEN",e.getMessage());}catch(IllegalArgumentException e){r.setStatus(422);fail(out,"VALIDATION_FAILED",e.getMessage());}catch(IllegalStateException e){r.setStatus(409);fail(out,"NOT_READY",e.getMessage());}catch(Exception e){r.setStatus(500);fail(out,"INTERNAL_ERROR","Permintaan gagal. ID: "+trace);ais.common.ErrorAuditUtil.record(e,"SosialApi:"+trace);}finally{try{r.getWriter().write(out.toString());}catch(Exception ignored){}}
 }
 /** Menghitung dan menyimpan hasil perhitungan zakat ({@link PerhitunganZakat}) dari parameter permintaan (nominal, penghasilan kotor, pengurang, gram emas, jumlah orang) untuk jenis zakat tertentu. */
 private JSONObject calculate(HttpServletRequest q,SocialRequestContext c)throws Exception{Session s=null;Transaction tx=null;try{s=HibernateUtil.openSession();tx=s.beginTransaction();JSONObject input=new JSONObject();copyNumber(q,input,"amount");copyNumber(q,input,"grossIncome");copyNumber(q,input,"deductions");copyNumber(q,input,"grams");copyNumber(q,input,"people");PerhitunganZakat x=new ZakatCalculatorService().calculateAndStore(s,c,longValue(q,"jenisZakatId"),input);tx.commit();JSONObject o=xResult(x);o.put("ok",true).put("calculationId",x.getId());return o;}catch(Exception e){if(tx!=null)try{tx.rollback();}catch(Exception ignored){}throw e;}finally{if(s!=null)try{s.close();}catch(Exception ignored){}}}
 /** Membuat transaksi donasi baru ({@link TransaksiDonasi}) dari parameter permintaan (jenis dana, program, hasil perhitungan zakat opsional, nominal, opsi anonim/doa publik) dan mengembalikan URL pembayaran berikutnya. */
 private JSONObject donation(HttpServletRequest q,SocialRequestContext c)throws Exception{TransaksiDonasi d=new SocialDonationService().create(c,longValue(q,"fundTypeId"),optionalLong(q,"programId"),optionalLong(q,"calculationId"),decimal(q,"amount",false),decimal(q,"contribution",true),bool(q,"anonymous"),q.getParameter("name"),q.getParameter("contact"),q.getParameter("prayer"),bool(q,"publicPrayer"),value(q,"idempotencyKey"));return new JSONObject().put("ok",true).put("transactionNumber",d.getTransactionNumber()).put("status",d.getStatus()).put("nextUrl",q.getContextPath()+"/sosial/pembayaran/"+d.getTransactionNumber());}
 /** Menginisiasi pembayaran gateway untuk satu transaksi donasi dan mengembalikan URL pembayaran serta waktu kedaluwarsanya. */
 private JSONObject payment(HttpServletRequest q,SocialRequestContext c)throws Exception{PembayaranDonasi p=new SocialPaymentService().initiate(c,value(q,"transactionNumber"),value(q,"gatewayId"));return new JSONObject().put("ok",true).put("orderId",p.getGatewayOrderId()).put("status",p.getPaymentStatus()).put("paymentUrl",p.getPaymentUrl()).put("expiry",p.getExpiryAt()==null?JSONObject.NULL:p.getExpiryAt().getTime());}
 /** Mengambil status pembayaran terkini untuk satu nomor transaksi; melempar {@link IllegalArgumentException} bila tidak ditemukan. */
 private JSONObject paymentStatus(HttpServletRequest q,SocialRequestContext c)throws Exception{PembayaranDonasi p=new SocialPortalService().payment(c,value(q,"transactionNumber"));if(p==null)throw new IllegalArgumentException("Pembayaran tidak ditemukan.");return new JSONObject().put("ok",true).put("status",p.getPaymentStatus()).put("paymentUrl",p.getPaymentUrl()).put("paidAt",p.getPaidAt()==null?JSONObject.NULL:p.getPaidAt().getTime()).put("expiry",p.getExpiryAt()==null?JSONObject.NULL:p.getExpiryAt().getTime());}
 /** Mengubah hasil perhitungan zakat tersimpan menjadi JSON respons, menambahkan versi kebijakan dan mata uang. */
 private JSONObject xResult(PerhitunganZakat x)throws Exception{return new JSONObject(x.getResultJson()).put("policyVersion",x.getPolicyVersion()).put("currency",x.getCurrency());}
 /** Menyalin parameter numerik {@code k} (bila ada dan tidak kosong) dari permintaan ke objek JSON {@code o} sebagai {@link BigDecimal}, membuang koma pemisah ribuan. */
 private void copyNumber(HttpServletRequest q,JSONObject o,String k)throws Exception{String v=q.getParameter(k);if(v!=null&&!v.trim().isEmpty())o.put(k,new BigDecimal(v.replace(",","")));}
 /** Mengambil parameter wajib {@code k}; melempar {@link IllegalArgumentException} bila kosong/tidak ada. */
 private String value(HttpServletRequest q,String k){String v=q.getParameter(k);if(v==null||v.trim().isEmpty())throw new IllegalArgumentException(k+" wajib diisi.");return v.trim();}
 /** Mengambil parameter wajib {@code k} sebagai {@link Long}; melempar {@link IllegalArgumentException} bila kosong atau bukan angka. */
 private Long longValue(HttpServletRequest q,String k){try{return Long.valueOf(value(q,k));}catch(Exception e){throw new IllegalArgumentException(k+" tidak valid.");}}
 /** Mengambil parameter opsional {@code k} sebagai {@link Long}, mengembalikan {@code null} bila tidak ada. */
 private Long optionalLong(HttpServletRequest q,String k){String v=q.getParameter(k);if(v==null||v.trim().isEmpty())return null;try{return Long.valueOf(v);}catch(Exception e){throw new IllegalArgumentException(k+" tidak valid.");}}
 /** Mengambil parameter {@code k} sebagai {@link BigDecimal}; bila {@code optional} dan kosong mengembalikan {@link BigDecimal#ZERO}, selain itu wajib diisi dan valid. */
 private BigDecimal decimal(HttpServletRequest q,String k,boolean optional){String v=q.getParameter(k);if((v==null||v.trim().isEmpty())&&optional)return BigDecimal.ZERO;try{return new BigDecimal(value(q,k).replace(",",""));}catch(Exception e){throw new IllegalArgumentException(k+" tidak valid.");}}
 /** Mengurai parameter {@code k} sebagai boolean, menerima "true"/"1"/"on" (tanpa memandang huruf besar/kecil). */
 private boolean bool(HttpServletRequest q,String k){String v=q.getParameter(k);return "true".equalsIgnoreCase(v)||"1".equals(v)||"on".equalsIgnoreCase(v);}
 /** Melempar {@link IllegalArgumentException} bila permintaan bukan POST (dipakai untuk action yang wajib mutasi). */
 private void requirePost(boolean m){if(!m)throw new IllegalArgumentException("POST diperlukan.");} /** Mengisi objek respons {@code o} dengan penanda gagal, kode {@code c}, dan pesan {@code m} (default ke {@code c} bila {@code m} null). */
 private void fail(JSONObject o,String c,String m){try{o.put("ok",false).put("code",c).put("message",m==null?c:m);}catch(Exception ignored){}}
}
