package ais.action.servlet;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import ais.action.master.jurnal.JurnalGalleyViewerService;
import ais.action.master.jurnal.JurnalGalleyViewerService.Rendered;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;

/**
 * Servlet publik pembaca galley (versi tersaji) artikel jurnal AIS, memetakan
 * {@code GET /jurnal-galley/{html|jats|pdf}/{id}}.
 *
 * <p>Format {@code html} dan {@code jats} dirender langsung sebagai halaman HTML minimal
 * (CSP ketat: tanpa skrip/gambar eksternal), sedangkan {@code pdf} tidak dirender di sini --
 * setelah lolos pengecekan entitlement, permintaan dialihkan ({@code redirect}) ke
 * {@code /jurnal-file/{id}} yang menyajikan berkas aslinya. Pengecekan hak akses (langganan
 * aktif, rentang IP institusi, dsb.) seluruhnya didelegasikan ke
 * {@link JurnalGalleyViewerService}; servlet ini hanya mem-parsing rute, meneruskan
 * pemanggil ({@link Tbmuser}, bisa {@code null} untuk anonim) dan alamat IP, lalu
 * menerjemahkan hasil/galat menjadi kode status HTTP yang sesuai.</p>
 */
public final class JurnalGalley extends HttpServlet {
    /** ID versi serialisasi servlet ini (kontrak {@link java.io.Serializable} bawaan {@code HttpServlet}). */
    private static final long serialVersionUID=1L;
    /** Layanan tunggal yang menegakkan entitlement dan merender isi galley (HTML/JATS/PDF). */
    private final JurnalGalleyViewerService viewers=new JurnalGalleyViewerService();

    /**
     * Melayani permintaan galley: mem-parsing {@code pathInfo} menjadi format dan ID artikel,
     * memasang header pengeras (nosniff, frame-ancestors, no-store, CSP), lalu memanggil
     * {@link JurnalGalleyViewerService} sesuai format yang diminta.
     *
     * <p>Rute yang tidak cocok pola {@code /{html|jats|pdf}/{id angka positif}} dibalas 422.
     * {@link SecurityException} dari service (entitlement ditolak) dibalas 403,
     * {@link java.io.FileNotFoundException} dibalas 404, dan galat lain dicatat lewat
     * {@link ais.common.ErrorAuditUtil} serta dibalas 500 dengan ID jejak yang sama di
     * header {@code X-Request-Id} dan isi tanggapan, untuk memudahkan korelasi log.</p>
     *
     * @param req permintaan HTTP; {@code getPathInfo()} menentukan format dan ID artikel
     * @param res tanggapan HTTP; diisi HTML galley, dialihkan ke berkas PDF, atau kode kesalahan
     * @throws IOException bila penulisan tanggapan gagal
     */
    protected void doGet(HttpServletRequest req,HttpServletResponse res)throws IOException{String requestId=Long.toHexString(System.currentTimeMillis())+Integer.toHexString(System.identityHashCode(req));res.setHeader("X-Content-Type-Options","nosniff");res.setHeader("X-Frame-Options","SAMEORIGIN");res.setHeader("Referrer-Policy","no-referrer");res.setHeader("Cache-Control","private, no-store");res.setHeader("X-Request-Id",requestId);try{String[] part=req.getPathInfo()==null?new String[0]:req.getPathInfo().split("/");if(part.length!=3||!part[1].matches("html|jats|pdf")||!part[2].matches("[1-9][0-9]*"))throw new IllegalArgumentException("Route galley tidak valid.");Long id=Long.valueOf(part[2]);Tbmuser actor=Common.getCurrentUser(req);if("pdf".equals(part[1])){viewers.requirePdf(id,actor,req.getRemoteAddr());res.sendRedirect(req.getContextPath()+"/jurnal-file/"+id);return;}Rendered page="html".equals(part[1])?viewers.renderHtml(id,actor,req.getRemoteAddr()):viewers.renderJats(id,actor,req.getRemoteAddr());res.setContentType("text/html; charset=UTF-8");res.setHeader("Content-Security-Policy","default-src 'none'; style-src 'self'; img-src 'none'; frame-ancestors 'self'; base-uri 'none'; form-action 'none'");res.getWriter().write("<!doctype html><html lang=\"id\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>"+JurnalGalleyViewerService.html(page.title)+"</title></head><body><main><article>"+page.bodyHtml+"</article></main></body></html>");}catch(SecurityException e){if(!res.isCommitted())res.sendError(403,"Hak akses galley tidak tersedia.");}catch(java.io.FileNotFoundException e){if(!res.isCommitted()){res.setStatus(404);res.setContentType("text/plain; charset=UTF-8");res.getWriter().write("Galley tidak ditemukan.");}}catch(IllegalArgumentException e){if(!res.isCommitted())res.sendError(422,e.getMessage());}catch(Exception e){ais.common.ErrorAuditUtil.record(e,"JurnalGalley:"+requestId);if(!res.isCommitted())res.sendError(500,"Galley gagal ditampilkan. ID: "+requestId);}finally{HibernateUtil.closeSession();}}
}
