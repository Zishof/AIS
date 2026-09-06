package ais.action.servlet;

import java.io.IOException;
import java.math.BigDecimal;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import ais.action.master.jurnal.JurnalPaymentCallbackService;
import ais.action.master.jurnal.JurnalRateLimiter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.LogPembayaran;

/**
 * Endpoint callback server-ke-server dari penyedia pembayaran ke modul Jurnal, memetakan
 * {@code POST /jurnal-payment-callback}.
 *
 * <p><b>Otentikasi</b>: BUKAN memakai sesi/login pengguna -- ini panggilan mesin-ke-mesin.
 * Keaslian pemanggil dibuktikan lewat header {@code X-Journal-Signature} berisi HMAC-SHA256
 * atas field-field kanonis (timestamp, provider, subscriptionId, externalReference, amount,
 * currency, providerReference), diverifikasi dalam waktu-tetap di
 * {@link JurnalPaymentCallbackService#settle}. Layanan tersebut juga menolak {@code timestamp}
 * yang sudah kedaluwarsa (jendela default 5 menit, mencegah <i>replay</i>) dan provider yang
 * tidak ada dalam allow-list. Selain itu, {@link JurnalRateLimiter} membatasi 60 percobaan per
 * menit per alamat IP untuk memperlambat percobaan tebak signature/DoS.</p>
 */
public final class JurnalPaymentCallback extends HttpServlet {
    /** ID versi serialisasi servlet ini (kontrak {@link java.io.Serializable} bawaan {@code HttpServlet}). */
    private static final long serialVersionUID = 1L;

    /**
     * Menolak seluruh permintaan {@code GET}; callback pembayaran ini hanya menerima {@code POST}.
     *
     * @param q permintaan HTTP masuk (tidak dipakai selain oleh kontrak servlet)
     * @param r tanggapan HTTP; selalu diisi status 405
     * @throws IOException bila penulisan status gagal
     */
    protected void doGet(HttpServletRequest q, HttpServletResponse r) throws IOException { r.sendError(405); }

    /**
     * Memverifikasi dan memproses satu callback pembayaran: mengecek rate limit per IP,
     * memvalidasi signature HMAC lewat {@link JurnalPaymentCallbackService#settle}, lalu
     * mencatat pembayaran sebagai terverifikasi.
     *
     * <p>Selalu membalas JSON, tidak pernah kode status tanpa isi. Alur galat: rate limit
     * terlampaui membalas 429 {@code RATE_LIMITED}; {@link SecurityException} (signature atau
     * provider tidak valid) membalas 401 {@code UNAUTHORIZED}; {@link IllegalArgumentException}
     * (payload tidak lengkap/tidak valid) membalas 422 {@code VALIDATION_FAILED} dengan
     * pesannya; galat lain dicatat lewat {@link ais.common.ErrorAuditUtil} dan membalas 500
     * {@code INTERNAL_ERROR} dengan ID jejak, tanpa membocorkan detail internal ke pemanggil.</p>
     *
     * @param q permintaan HTTP; parameter {@code timestamp}, {@code subscriptionId},
     *          {@code externalReference}, {@code amount}, {@code currency}, {@code provider},
     *          {@code providerReference} wajib diisi, dan header {@code X-Journal-Signature}
     *          wajib berisi HMAC-SHA256 hex 64 karakter atas field kanonis tersebut
     * @param r tanggapan HTTP; selalu diisi JSON {@code {ok, ...}} atau {@code {ok:false, code, message}}
     * @throws IOException bila penulisan tanggapan gagal
     */
    protected void doPost(HttpServletRequest q, HttpServletResponse r) throws IOException {
        String trace=Long.toHexString(System.currentTimeMillis())+Integer.toHexString(System.identityHashCode(q));
        JSONObject out=new JSONObject(); r.setContentType("application/json; charset=UTF-8");
        r.setHeader("Cache-Control","no-store"); r.setHeader("X-Content-Type-Options","nosniff"); r.setHeader("X-Request-Id",trace);
        try {
            if(!JurnalRateLimiter.allow("payment-callback",q.getRemoteAddr(),60,60000L)){r.setStatus(429);fail(out,"RATE_LIMITED","Terlalu banyak callback.");return;}
            LogPembayaran x=new JurnalPaymentCallbackService().settle(longValue(q,"timestamp"),q.getHeader("X-Journal-Signature"),
                    longValue(q,"subscriptionId"),req(q,"externalReference"),decimal(q,"amount"),req(q,"currency"),
                    req(q,"provider"),req(q,"providerReference"));
            out.put("ok",true).put("paymentId",x.getId());
        } catch(SecurityException e) { r.setStatus(401); fail(out,"UNAUTHORIZED","Callback pembayaran ditolak.");
        } catch(IllegalArgumentException e) { r.setStatus(422); fail(out,"VALIDATION_FAILED",e.getMessage());
        } catch(Exception e) { r.setStatus(500); fail(out,"INTERNAL_ERROR","Callback pembayaran gagal. ID: "+trace); ais.common.ErrorAuditUtil.record(e,"JurnalPaymentCallback:"+trace);
        } finally { try{r.getWriter().write(out.toString());}catch(Exception ignored){} HibernateUtil.closeSession(); }
    }
    /**
     * Mengambil parameter {@code n} dan memastikan terisi (tidak {@code null}/kosong setelah di-trim).
     *
     * @param q permintaan HTTP
     * @param n nama parameter
     * @return nilai parameter yang sudah di-trim
     * @throws IllegalArgumentException bila parameter tidak ada atau kosong
     */
    private static String req(HttpServletRequest q,String n){String v=q.getParameter(n);if(v==null||v.trim().length()==0)throw new IllegalArgumentException(n+" wajib diisi.");return v.trim();}

    /**
     * Mengambil parameter {@code n} wajib dan mem-parsingnya sebagai {@link Long}.
     *
     * @param q permintaan HTTP
     * @param n nama parameter
     * @return nilai parameter sebagai {@link Long}
     * @throws IllegalArgumentException bila parameter tidak ada, kosong, atau bukan angka bulat
     */
    private static Long longValue(HttpServletRequest q,String n){try{return Long.valueOf(req(q,n));}catch(Exception e){throw new IllegalArgumentException(n+" tidak valid.");}}

    /**
     * Mengambil parameter {@code n} wajib dan mem-parsingnya sebagai {@link BigDecimal}
     * (dipakai untuk nominal transaksi {@code amount}).
     *
     * @param q permintaan HTTP
     * @param n nama parameter
     * @return nilai parameter sebagai {@link BigDecimal}
     * @throws IllegalArgumentException bila parameter tidak ada, kosong, atau bukan angka desimal valid
     */
    private static BigDecimal decimal(HttpServletRequest q,String n){try{return new BigDecimal(req(q,n));}catch(Exception e){throw new IllegalArgumentException(n+" tidak valid.");}}

    /**
     * Mengisi objek JSON tanggapan galat dengan {@code ok=false} dan kode/pesan yang diberikan.
     * Kegagalan penyusunan JSON (praktis tidak pernah terjadi untuk nilai String biasa)
     * sengaja diabaikan agar penulisan tanggapan galat itu sendiri tidak ikut gagal.
     *
     * @param o objek JSON tanggapan yang akan diisi
     * @param c kode galat mesin-terbaca, mis. {@code "UNAUTHORIZED"}
     * @param m pesan galat untuk manusia
     */
    private static void fail(JSONObject o,String c,String m){try{o.put("ok",false).put("code",c).put("message",m);}catch(Exception ignored){}}
}
