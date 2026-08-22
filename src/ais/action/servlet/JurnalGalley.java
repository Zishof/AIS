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

/** Public entitlement-aware HTML/JATS/PDF galley viewer. */
public final class JurnalGalley extends HttpServlet {
    private static final long serialVersionUID=1L;private final JurnalGalleyViewerService viewers=new JurnalGalleyViewerService();
    protected void doGet(HttpServletRequest req,HttpServletResponse res)throws IOException{String requestId=Long.toHexString(System.currentTimeMillis())+Integer.toHexString(System.identityHashCode(req));res.setHeader("X-Content-Type-Options","nosniff");res.setHeader("X-Frame-Options","SAMEORIGIN");res.setHeader("Referrer-Policy","no-referrer");res.setHeader("Cache-Control","private, no-store");res.setHeader("X-Request-Id",requestId);try{String[] part=req.getPathInfo()==null?new String[0]:req.getPathInfo().split("/");if(part.length!=3||!part[1].matches("html|jats|pdf")||!part[2].matches("[1-9][0-9]*"))throw new IllegalArgumentException("Route galley tidak valid.");Long id=Long.valueOf(part[2]);Tbmuser actor=Common.getCurrentUser(req);if("pdf".equals(part[1])){viewers.requirePdf(id,actor,req.getRemoteAddr());res.sendRedirect(req.getContextPath()+"/jurnal-file/"+id);return;}Rendered page="html".equals(part[1])?viewers.renderHtml(id,actor,req.getRemoteAddr()):viewers.renderJats(id,actor,req.getRemoteAddr());res.setContentType("text/html; charset=UTF-8");res.setHeader("Content-Security-Policy","default-src 'none'; style-src 'self'; img-src 'none'; frame-ancestors 'self'; base-uri 'none'; form-action 'none'");res.getWriter().write("<!doctype html><html lang=\"id\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>"+JurnalGalleyViewerService.html(page.title)+"</title></head><body><main><article>"+page.bodyHtml+"</article></main></body></html>");}catch(SecurityException e){if(!res.isCommitted())res.sendError(403,"Hak akses galley tidak tersedia.");}catch(java.io.FileNotFoundException e){if(!res.isCommitted()){res.setStatus(404);res.setContentType("text/plain; charset=UTF-8");res.getWriter().write("Galley tidak ditemukan.");}}catch(IllegalArgumentException e){if(!res.isCommitted())res.sendError(422,e.getMessage());}catch(Exception e){ais.common.ErrorAuditUtil.record(e,"JurnalGalley:"+requestId);if(!res.isCommitted())res.sendError(500,"Galley gagal ditampilkan. ID: "+requestId);}finally{HibernateUtil.closeSession();}}
}
