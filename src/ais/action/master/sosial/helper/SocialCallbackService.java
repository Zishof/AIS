package ais.action.master.sosial.helper;

import java.math.BigDecimal; import java.util.Date; import org.hibernate.LockMode; import org.hibernate.Session; import org.hibernate.Transaction; import org.hibernate.criterion.Restrictions; import org.json.JSONObject;
import ais.database.hibernate.HibernateUtil; import ais.database.model.sosial.*;

/**
 * Pemroses callback (webhook) gateway pembayaran untuk modul donasi sosial ("smartlink"). Satu
 * method publik, {@link #process(String)}, mem-parsing payload JSON mentah dari gateway,
 * mencocokkannya dengan record {@link PembayaranDonasi} yang menunggu ({@code gatewayOrderId}),
 * lalu memvalidasi kecocokan mata uang dan nominal sebelum menandai pembayaran sukses.
 *
 * <p>
 * Penanganan idempotensi/duplikasi: bila pembayaran sudah berstatus {@code PAID}, callback kedua
 * dengan sidik jari ({@code sha256} payload) berbeda dicatat sebagai varian duplikat mencurigakan
 * ({@code DUPLICATE_CALLBACK_VARIANT}) lewat {@link #exception}, tetapi tetap mengembalikan sukses
 * (idempoten terhadap pemanggil gateway). Ketidakcocokan mata uang atau nominal masing-masing
 * dicatat sebagai {@code CURRENCY_MISMATCH}/{@code AMOUNT_MISMATCH} dan mengubah status pembayaran
 * menjadi {@code MISMATCH} untuk ditinjau manual — transaksi TIDAK ditandai gagal otomatis.
 * Payload asli disimpan dalam bentuk yang sudah dipangkas ({@link #redact}) ke kolom
 * {@code callbackPayloadRedacted}, bukan payload mentah, untuk membatasi data sensitif yang
 * tersimpan permanen. Saat pembayaran benar-benar sukses, alokasi donasi terkait diposting dan
 * kwitansi dibuat (bila belum ada) lewat {@link SocialReceiptService#createIfMissing}. Seluruh
 * proses berjalan dalam satu transaksi Hibernate; kegagalan runtime memicu rollback sebelum
 * exception diteruskan ke pemanggil.
 * </p>
 */
public final class SocialCallbackService {
 /**
  * Memproses satu payload callback pembayaran gateway.
  *
  * @param raw payload JSON mentah dari gateway (harus memiliki objek {@code data} dengan
  *            {@code order_id}, {@code amount}, dan opsional {@code currency}/{@code status}/
  *            {@code transaction_id}/{@code reference})
  * @return objek JSON ringkas berisi {@code ok}, {@code duplicate}, {@code orderId}, {@code status}
  * @throws IllegalArgumentException bila order tidak dikenal
  * @throws SecurityException bila tenant alokasi donasi tidak cocok dengan tenant callback
  * @throws Exception diteruskan dari kegagalan Hibernate/parsing JSON lainnya (transaksi di-rollback)
  */
 public JSONObject process(String raw) throws Exception {JSONObject root=new JSONObject(raw),data=root.getJSONObject("data");String order=data.getString("order_id");BigDecimal amount=new BigDecimal(String.valueOf(data.get("amount")));String currency=data.optString("currency","IDR");String status=data.optString("status","ERROR");String callbackId=data.optString("transaction_id",data.optString("reference",order));String fingerprint=SocialSecurity.sha256(raw);Session s=null;Transaction tx=null;try{s=HibernateUtil.openSession();tx=s.beginTransaction();PembayaranDonasi p=(PembayaranDonasi)s.createCriteria(PembayaranDonasi.class).add(Restrictions.eq("gatewayOrderId",order)).setMaxResults(1).uniqueResult();if(p==null)throw new IllegalArgumentException("Order tidak dikenal.");s.lock(p,LockMode.UPGRADE);
   if("PAID".equals(p.getPaymentStatus())){if(!fingerprint.equals(p.getCallbackFingerprint()))exception(s,p,"DUPLICATE_CALLBACK_VARIANT",p.getTotal(),amount);tx.commit();return ok(p,true);}
   p.setCallbackTransactionId(callbackId);p.setCallbackFingerprint(fingerprint);p.setCallbackPayloadRedacted(redact(root).toString());TransaksiDonasi d=p.getTransaction();SocialRequestContext c=systemContext(p.getTenantKey());
   if(!currency.equalsIgnoreCase(p.getCurrency())){SocialStateMachine.requirePayment(p.getPaymentStatus(),"MISMATCH");p.setPaymentStatus("MISMATCH");p.setReconciliationStatus("EXCEPTION");p.setFailureReason("Mata uang callback tidak cocok.");exception(s,p,"CURRENCY_MISMATCH",p.getTotal(),amount);tx.commit();return ok(p,false);}
   if(!amount.setScale(2,BigDecimal.ROUND_HALF_UP).equals(p.getTotal().setScale(2,BigDecimal.ROUND_HALF_UP))){SocialStateMachine.requirePayment(p.getPaymentStatus(),"MISMATCH");p.setPaymentStatus("MISMATCH");p.setReconciliationStatus("EXCEPTION");p.setFailureReason("Nominal callback tidak cocok.");exception(s,p,"AMOUNT_MISMATCH",p.getTotal(),amount);tx.commit();return ok(p,false);}
   if(!"success".equalsIgnoreCase(status)){p.setFailureReason("Callback status tidak final-sukses: "+safeStatus(status));tx.commit();return ok(p,false);}
   Date now=new Date();SocialStateMachine.requirePayment(p.getPaymentStatus(),"PAID");SocialStateMachine.requireDonation(d.getStatus(),"ALLOCATED");p.setPaymentStatus("PAID");p.setPaidAt(now);p.setReconciliationStatus("PENDING_SETTLEMENT");d.setStatus("ALLOCATED");d.setPaidAt(now);d.setUpdatedBy("smartlink-callback");java.util.List rows=s.createCriteria(AlokasiDonasi.class).add(Restrictions.eq("transaction",d)).list();for(Object o:rows){AlokasiDonasi a=(AlokasiDonasi)o;if(!c.getTenantKey().equals(a.getTenantKey()))throw new SecurityException("Tenant alokasi callback tidak cocok.");a.setStatus("POSTED");a.setPostedAt(now);a.setUpdatedBy("smartlink-callback");}
   new SocialReceiptService().createIfMissing(s,c,d);tx.commit();return ok(p,false);
  }catch(RuntimeException e){if(tx!=null)try{tx.rollback();}catch(Exception ignored){}throw e;}finally{if(s!=null)try{s.close();}catch(Exception ignored){}}
 }
 /** Membuat konteks permintaan tepercaya (bukan permintaan pengguna) atas nama proses callback gateway, untuk operasi sistem seperti pembuatan kwitansi. */
 private SocialRequestContext systemContext(final String tenant){return SocialRequestContext.trusted(tenant,"smartlink-callback");}
 /** Membangun respons sukses ringkas berisi status order dan penanda {@code duplicate}. */
 private JSONObject ok(PembayaranDonasi p,boolean duplicate){JSONObject o=new JSONObject();try{o.put("ok",true).put("duplicate",duplicate).put("orderId",p.getGatewayOrderId()).put("status",p.getPaymentStatus());}catch(Exception ignored){}return o;}
 /** Memangkas payload callback mentah menjadi subset field non-sensitif saja sebelum disimpan sebagai arsip audit ({@code callbackPayloadRedacted}). */
 private JSONObject redact(JSONObject root){JSONObject safe=new JSONObject();try{JSONObject d=root.getJSONObject("data");safe.put("data",new JSONObject().put("order_id",d.optString("order_id")).put("amount",d.optString("amount")).put("currency",d.optString("currency","IDR")).put("status",d.optString("status")).put("transaction_id",d.optString("transaction_id")).put("transaction_time",d.optString("transaction_time")));}catch(Exception ignored){}return safe;}
 /** Menyaring status callback menjadi karakter aman (alfanumerik/underscore/dash) dan memotongnya maksimal 40 karakter untuk mencegah data kotor masuk ke pesan galat. */
 private String safeStatus(String value){if(value==null)return "UNKNOWN";value=value.replaceAll("[^A-Za-z0-9_-]","");return value.length()>40?value.substring(0,40):value;}
 /** Mencatat satu baris {@link SocialPaymentReconciliation} untuk anomali callback (mis. duplikat, ketidakcocokan mata uang/nominal) agar dapat ditinjau manual, menyimpan selisih nominal yang diharapkan vs diterima. */
 private void exception(Session s,PembayaranDonasi p,String type,BigDecimal expected,BigDecimal received){SocialPaymentReconciliation r=new SocialPaymentReconciliation();r.setTenantKey(p.getTenantKey());r.setPayment(p);r.setGateway(p.getGatewayId());r.setExceptionType(type);r.setExpectedAmount(expected);r.setReceivedAmount(received);r.setDifference(received.subtract(expected));r.setStatus("EXCEPTION");r.setCreatedBy("smartlink-callback");s.save(r);}
}
