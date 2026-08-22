package ais.action.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import ais.action.master.jurnal.JurnalAuthorizationService;
import ais.action.master.jurnal.JurnalPublicService;
import ais.common.Common;
import ais.common.JurnalAksesKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;

/** Canonical public and protected entry point for the integrated journal. */
public final class Jurnal extends HttpServlet {
    private static final long serialVersionUID=1L;
    private static final String PUBLIC_JSP="/WEB-INF/baru/modul/jurnal/landing_page.jsp";
    private static final String ADMIN_JSP="/WEB-INF/baru/modul/jurnal/admin.jsp";
    private final JurnalPublicService publicService=new JurnalPublicService();
    private final JurnalAuthorizationService auth=new JurnalAuthorizationService();

    protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{process(req,res);}
    protected void doPost(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{process(req,res);}
    private void process(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        String requestId=Long.toHexString(System.currentTimeMillis())+"-"+Integer.toHexString(System.identityHashCode(req));
        res.setHeader("X-Content-Type-Options","nosniff");res.setHeader("X-Frame-Options","SAMEORIGIN");res.setHeader("Referrer-Policy","strict-origin-when-cross-origin");res.setHeader("X-Request-Id",requestId);
        try{
            req.setCharacterEncoding("UTF-8");String path=clean(req.getPathInfo());
            if(path.startsWith("/admin")){admin(req,res,path);return;}
            if(path.startsWith("/article/")){Long id=parseLong(path.substring("/article/".length()));JurnalPublicService.ArticleCard item=publicService.article(id);if(item==null){res.sendError(404);return;}req.setAttribute("jurnalView","article");req.setAttribute("jurnalArticle",item);}
            else{req.setAttribute("jurnalView","home");req.setAttribute("jurnalHome",publicService.home());}
            req.getRequestDispatcher(PUBLIC_JSP).forward(req,res);
        }catch(SecurityException e){if(!res.isCommitted())res.sendError(403,"Hak akses jurnal tidak tersedia.");}
        catch(Exception e){ais.common.ErrorAuditUtil.recordVisibleFailure(e,"Jurnal servlet",req,requestId);if(!res.isCommitted())res.sendError(500,"Modul jurnal belum dapat melayani permintaan. ID: "+requestId);}
        finally{HibernateUtil.closeSession();}
    }
    private void admin(HttpServletRequest req,HttpServletResponse res,String path)throws Exception{
        Tbmuser user=Common.getCurrentUser(req);if(user==null){res.sendRedirect(req.getContextPath()+"/login2?returnTo="+req.getContextPath()+"/jurnal"+path);return;}
        String key=path.length()>7?clean(path.substring(7)):"dashboard";if(key.indexOf('/')>=0)key=key.substring(0,key.indexOf('/'));
        if(!JurnalAksesKatalog.dikenal(key)) {res.sendError(404);return;}
        auth.requireRead(user,key);req.setAttribute("jurnalAdminKey",key);
        for(JurnalAksesKatalog.Entri e:JurnalAksesKatalog.DAFTAR)if(e.kunci.equals(key)){req.setAttribute("jurnalAdminTitle",e.label);break;}
        req.setAttribute("jurnalAdminEntries",JurnalAksesKatalog.DAFTAR);req.getRequestDispatcher(ADMIN_JSP).forward(req,res);
    }
    private String clean(String v){return v==null?"":v.trim();}
    private Long parseLong(String v){try{return Long.valueOf(v);}catch(Exception e){return null;}}
}
