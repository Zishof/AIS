package ais.action.servlet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import ais.action.master.jurnal.JurnalReportService;
import ais.action.master.jurnal.JurnalUserExchangeService;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;

/**
 * Servlet pelaporan jurnal terautentikasi, memetakan {@code GET /jurnal-report}.
 *
 * <p>Dua jenis keluaran: {@code type=COUNTER5} membalas ringkasan pemakaian format
 * COUNTER Release 5 sebagai JSON; jenis lain ({@code ARTICLES}, {@code REVIEWS},
 * {@code SUBSCRIPTIONS}, {@code USERS}) membalas berkas CSV terunduh untuk satu
 * {@code journalId}. Login wajib ({@link Common#getCurrentUser}); otorisasi rinci per
 * jurnal (mis. hanya editor/administrator jurnal terkait yang boleh melihat laporan)
 * didelegasikan ke {@link JurnalReportService} dan {@link JurnalUserExchangeService} --
 * servlet ini hanya mem-parsing parameter dan menerjemahkan galat menjadi status HTTP.</p>
 */
public final class JurnalReport extends HttpServlet {
    /** ID versi serialisasi servlet ini (kontrak {@link java.io.Serializable} bawaan {@code HttpServlet}). */
    private static final long serialVersionUID=1L;

    /**
     * Melayani permintaan laporan: memvalidasi {@code journalId} dan {@code type}, lalu
     * menulis JSON COUNTER 5 atau berkas CSV sesuai jenis yang diminta.
     *
     * <p>{@code actor} yang belum login dibalas 401. {@link SecurityException} (otorisasi
     * jurnal ditolak oleh service) dibalas 403; {@link IllegalArgumentException} (parameter
     * tidak valid atau jenis laporan tak didukung) dibalas 422 dengan pesannya; galat lain
     * dicatat lewat {@link ais.common.ErrorAuditUtil} dan dibalas 500 dengan ID jejak.</p>
     *
     * @param req permintaan HTTP; parameter {@code journalId} (wajib, angka positif),
     *            {@code type}, dan untuk {@code COUNTER5} juga {@code from}/{@code to}
     * @param res tanggapan HTTP; diisi JSON atau CSV, atau kode kesalahan
     * @throws IOException bila penulisan tanggapan gagal
     */
        protected void doGet(HttpServletRequest req,HttpServletResponse res)throws IOException{String requestId=Long.toHexString(System.currentTimeMillis())+Integer.toHexString(System.identityHashCode(req));res.setHeader("Cache-Control","private, no-store");res.setHeader("X-Content-Type-Options","nosniff");res.setHeader("X-Request-Id",requestId);try{Tbmuser actor=Common.getCurrentUser(req);if(actor==null){res.sendError(401);return;}Long journalId=id(req.getParameter("journalId"));String type=text(req.getParameter("type"));JurnalReportService service=new JurnalReportService();if("COUNTER5".equals(type)){Date from=date(req.getParameter("from")),to=date(req.getParameter("to"));res.setContentType("application/json; charset=UTF-8");res.getWriter().write(service.counter5(journalId,from,to,actor).toString());}else{if(!type.matches("ARTICLES|REVIEWS|SUBSCRIPTIONS|USERS"))throw new IllegalArgumentException("Jenis laporan tidak didukung.");res.setContentType("text/csv; charset=UTF-8");res.setHeader("Content-Disposition","attachment; filename=jurnal-"+journalId+"-"+type.toLowerCase()+".csv");if("USERS".equals(type))new JurnalUserExchangeService().exportCsv(journalId,res.getWriter(),actor);else service.exportCsv(journalId,type,null,null,res.getWriter(),actor);}}catch(SecurityException e){if(!res.isCommitted())res.sendError(403);}catch(IllegalArgumentException e){if(!res.isCommitted())res.sendError(422,e.getMessage());}catch(Exception e){ais.common.ErrorAuditUtil.record(e,"JurnalReport:"+requestId);if(!res.isCommitted())res.sendError(500,"Laporan jurnal gagal. ID: "+requestId);}finally{HibernateUtil.closeSession();}}

    /**
     * Mem-parsing dan memvalidasi {@code journalId}: harus berupa bilangan bulat bernilai
     * minimal 1.
     *
     * @param v teks parameter {@code journalId}
     * @return ID jurnal yang valid
     * @throws IllegalArgumentException bila {@code v} bukan angka atau kurang dari 1
     */
    private static Long id(String v){try{Long x=Long.valueOf(v);if(x.longValue()<1)throw new Exception();return x;}catch(Exception e){throw new IllegalArgumentException("journalId tidak valid.");}}

    /**
     * Mem-parsing tanggal berformat {@code yyyy-MM-dd} secara ketat (tidak lenient).
     *
     * @param v teks tanggal, mis. dari parameter {@code from}/{@code to}
     * @return tanggal hasil parse
     * @throws IllegalArgumentException bila {@code v} bukan tanggal valid pada format tersebut
     */
    private static Date date(String v){try{SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd");f.setLenient(false);return f.parse(v);}catch(Exception e){throw new IllegalArgumentException("Tanggal laporan tidak valid.");}}

    /**
     * Menormalkan parameter jenis laporan: {@code null} menjadi string kosong, selain itu
     * di-trim dan diubah ke huruf besar untuk pencocokan case-insensitive.
     *
     * @param v teks parameter {@code type} apa adanya (boleh {@code null})
     * @return teks yang sudah dinormalkan, tidak pernah {@code null}
     */
    private static String text(String v){return v==null?"":v.trim().toUpperCase();}
}
