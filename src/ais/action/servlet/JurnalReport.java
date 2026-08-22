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

/** Authorized bounded CSV and COUNTER 5 report endpoint. */
public final class JurnalReport extends HttpServlet {
    private static final long serialVersionUID=1L;
        protected void doGet(HttpServletRequest req,HttpServletResponse res)throws IOException{String requestId=Long.toHexString(System.currentTimeMillis())+Integer.toHexString(System.identityHashCode(req));res.setHeader("Cache-Control","private, no-store");res.setHeader("X-Content-Type-Options","nosniff");res.setHeader("X-Request-Id",requestId);try{Tbmuser actor=Common.getCurrentUser(req);if(actor==null){res.sendError(401);return;}Long journalId=id(req.getParameter("journalId"));String type=text(req.getParameter("type"));JurnalReportService service=new JurnalReportService();if("COUNTER5".equals(type)){Date from=date(req.getParameter("from")),to=date(req.getParameter("to"));res.setContentType("application/json; charset=UTF-8");res.getWriter().write(service.counter5(journalId,from,to,actor).toString());}else{if(!type.matches("ARTICLES|REVIEWS|SUBSCRIPTIONS|USERS"))throw new IllegalArgumentException("Jenis laporan tidak didukung.");res.setContentType("text/csv; charset=UTF-8");res.setHeader("Content-Disposition","attachment; filename=jurnal-"+journalId+"-"+type.toLowerCase()+".csv");if("USERS".equals(type))new JurnalUserExchangeService().exportCsv(journalId,res.getWriter(),actor);else service.exportCsv(journalId,type,null,null,res.getWriter(),actor);}}catch(SecurityException e){if(!res.isCommitted())res.sendError(403);}catch(IllegalArgumentException e){if(!res.isCommitted())res.sendError(422,e.getMessage());}catch(Exception e){ais.common.ErrorAuditUtil.record(e,"JurnalReport:"+requestId);if(!res.isCommitted())res.sendError(500,"Laporan jurnal gagal. ID: "+requestId);}finally{HibernateUtil.closeSession();}}
    private static Long id(String v){try{Long x=Long.valueOf(v);if(x.longValue()<1)throw new Exception();return x;}catch(Exception e){throw new IllegalArgumentException("journalId tidak valid.");}}private static Date date(String v){try{SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd");f.setLenient(false);return f.parse(v);}catch(Exception e){throw new IllegalArgumentException("Tanggal laporan tidak valid.");}}private static String text(String v){return v==null?"":v.trim().toUpperCase();}
}
