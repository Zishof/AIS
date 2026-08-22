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

/** Provider-to-server endpoint. Authentication is an HMAC over canonical fields. */
public final class JurnalPaymentCallback extends HttpServlet {
    private static final long serialVersionUID = 1L;
    protected void doGet(HttpServletRequest q, HttpServletResponse r) throws IOException { r.sendError(405); }
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
    private static String req(HttpServletRequest q,String n){String v=q.getParameter(n);if(v==null||v.trim().length()==0)throw new IllegalArgumentException(n+" wajib diisi.");return v.trim();}
    private static Long longValue(HttpServletRequest q,String n){try{return Long.valueOf(req(q,n));}catch(Exception e){throw new IllegalArgumentException(n+" tidak valid.");}}
    private static BigDecimal decimal(HttpServletRequest q,String n){try{return new BigDecimal(req(q,n));}catch(Exception e){throw new IllegalArgumentException(n+" tidak valid.");}}
    private static void fail(JSONObject o,String c,String m){try{o.put("ok",false).put("code",c).put("message",m);}catch(Exception ignored){}}
}
