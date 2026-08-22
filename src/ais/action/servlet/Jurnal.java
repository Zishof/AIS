package ais.action.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import ais.action.master.jurnal.JurnalAuthorizationService;
import ais.action.master.jurnal.JurnalPublicService;
import ais.common.Common;
import ais.common.JurnalAksesKatalog;
import ais.common.newui.NewUiCsrfUtil;
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
            if("/feed".equals(path)||"/feed.xml".equals(path)){feed(req,res);return;}
            if("/sitemap.xml".equals(path)){sitemap(req,res);return;}
            if("/oai".equals(path)){req.getRequestDispatcher("/oai").forward(req,res);return;}
            if(path.startsWith("/citation/")){citation(req,res,path);return;}
            if(path.startsWith("/article/")){Long id=parseLong(path.substring("/article/".length()));JurnalPublicService.ArticleCard item=publicService.article(id);if(item==null){res.sendError(404);return;}req.setAttribute("jurnalView","article");req.setAttribute("jurnalArticle",item);}
            else if(path.startsWith("/issue/")){JurnalPublicService.IssueCard issue=publicService.issue(parseLong(path.substring(7)));if(issue==null){res.sendError(404);return;}req.setAttribute("jurnalView","issue");req.setAttribute("jurnalIssue",issue);}
            else if(path.startsWith("/journal/")||path.startsWith("/archive/")){String slug=path.substring(path.indexOf('/',1)+1);JurnalPublicService.JournalCard journal=publicService.journal(slug);if(journal==null){res.sendError(404);return;}req.setAttribute("jurnalView","journal");req.setAttribute("jurnalJournal",journal);req.setAttribute("jurnalIssues",publicService.issues(journal.id,page(req),20));}
            else if("/search".equals(path)){req.setAttribute("jurnalView","search");req.setAttribute("jurnalSearchTerm",clean(req.getParameter("q")));req.setAttribute("jurnalSearch",publicService.search(req.getParameter("q"),parseLong(req.getParameter("journal")),page(req),20));}
            else{req.setAttribute("jurnalView","home");req.setAttribute("jurnalHome",publicService.home());}
            req.getRequestDispatcher(PUBLIC_JSP).forward(req,res);
        }catch(SecurityException e){if(!res.isCommitted())res.sendError(403,"Hak akses jurnal tidak tersedia.");}
        catch(Exception e){ais.common.ErrorAuditUtil.recordVisibleFailure(e,"Jurnal servlet",req,requestId);if(!res.isCommitted())res.sendError(500,"Modul jurnal belum dapat melayani permintaan. ID: "+requestId);}
        finally{HibernateUtil.closeSession();}
    }
    private void admin(HttpServletRequest req,HttpServletResponse res,String path)throws Exception{
        Tbmuser user=Common.getCurrentUser(req);if(user==null){res.sendRedirect(req.getContextPath()+"/login2?returnTo="+req.getContextPath()+"/jurnal"+path);return;}
        if("POST".equalsIgnoreCase(req.getMethod())&&!NewUiCsrfUtil.isValid(req))throw new SecurityException("Token CSRF jurnal tidak valid.");
        String key=path.length()>7?clean(path.substring(7)):"dashboard";if(key.indexOf('/')>=0)key=key.substring(0,key.indexOf('/'));
        if(!JurnalAksesKatalog.dikenal(key)) {res.sendError(404);return;}
        auth.requireRead(user,key);req.setAttribute("jurnalAdminKey",key);req.setAttribute("jurnalCsrf",NewUiCsrfUtil.getToken(req.getSession(true)));
        for(JurnalAksesKatalog.Entri e:JurnalAksesKatalog.DAFTAR)if(e.kunci.equals(key)){req.setAttribute("jurnalAdminTitle",e.label);break;}
        req.setAttribute("jurnalAdminEntries",JurnalAksesKatalog.DAFTAR);req.getRequestDispatcher(ADMIN_JSP).forward(req,res);
    }
    private String clean(String v){return v==null?"":v.trim();}
    private Long parseLong(String v){try{return Long.valueOf(v);}catch(Exception e){return null;}}
    private int page(HttpServletRequest r){try{return Math.max(0,Integer.parseInt(r.getParameter("page")));}catch(Exception e){return 0;}}
    private void citation(HttpServletRequest req,HttpServletResponse res,String path)throws Exception{String x=path.substring("/citation/".length());int dot=x.lastIndexOf('.');String format=dot<0?"bibtex":x.substring(dot+1);Long id=parseLong(dot<0?x:x.substring(0,dot));String value=publicService.citation(id,format);if(value==null){res.sendError(404);return;}res.setContentType(("ris".equalsIgnoreCase(format)?"application/x-research-info-systems":"application/x-bibtex")+"; charset=UTF-8");res.setHeader("Content-Disposition","attachment; filename=ais-journal-"+id+("ris".equalsIgnoreCase(format)?".ris":".bib"));res.getWriter().write(value);}
    private void feed(HttpServletRequest req,HttpServletResponse res)throws Exception{res.setContentType("application/atom+xml; charset=UTF-8");PrintWriter w=res.getWriter();String base=req.getRequestURL().toString().replaceAll("/feed(?:\\.xml)?$","");w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><feed xmlns=\"http://www.w3.org/2005/Atom\"><title>Jurnal Ilmiah eCampus</title><id>"+xml(base)+"</id>");for(JurnalPublicService.ArticleCard a:publicService.latest(50)){w.write("<entry><id>"+xml(base+"/article/"+a.id)+"</id><title>"+xml(a.title)+"</title><link href=\""+xml(base+"/article/"+a.id)+"\"/><updated>"+date(a.publishedAt)+"</updated><summary>"+xml(a.abstractText)+"</summary></entry>");}w.write("</feed>");}
    private void sitemap(HttpServletRequest req,HttpServletResponse res)throws Exception{res.setContentType("application/xml; charset=UTF-8");String base=req.getRequestURL().toString().replaceAll("/sitemap\\.xml$","");PrintWriter w=res.getWriter();w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"><url><loc>"+xml(base)+"</loc></url>");for(JurnalPublicService.ArticleCard a:publicService.latest(100)){w.write("<url><loc>"+xml(base+"/article/"+a.id)+"</loc><lastmod>"+date(a.publishedAt)+"</lastmod></url>");}w.write("</urlset>");}
    private void oai(HttpServletRequest req,HttpServletResponse res)throws Exception{res.setContentType("application/xml; charset=UTF-8");String verb=clean(req.getParameter("verb"));String base=req.getRequestURL().toString();PrintWriter w=res.getWriter();w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\"><responseDate>"+date(new java.util.Date())+"</responseDate><request verb=\""+xml(verb)+"\">"+xml(base)+"</request>");if("Identify".equals(verb)){w.write("<Identify><repositoryName>Jurnal Ilmiah eCampus</repositoryName><baseURL>"+xml(base)+"</baseURL><protocolVersion>2.0</protocolVersion><adminEmail>noreply@localhost</adminEmail><earliestDatestamp>1970-01-01T00:00:00Z</earliestDatestamp><deletedRecord>persistent</deletedRecord><granularity>YYYY-MM-DDThh:mm:ssZ</granularity></Identify>");}else if("ListMetadataFormats".equals(verb)){w.write("<ListMetadataFormats><metadataFormat><metadataPrefix>oai_dc</metadataPrefix><schema>http://www.openarchives.org/OAI/2.0/oai_dc.xsd</schema><metadataNamespace>http://www.openarchives.org/OAI/2.0/oai_dc/</metadataNamespace></metadataFormat></ListMetadataFormats>");}else if("ListIdentifiers".equals(verb)||"ListRecords".equals(verb)){String tag=verb;w.write("<"+tag+">");for(JurnalPublicService.ArticleCard a:publicService.latest(100)){String header="<header><identifier>oai:ais:jurnal:"+a.id+"</identifier><datestamp>"+date(a.publishedAt)+"</datestamp><setSpec>journal:"+a.collectionId+"</setSpec></header>";if("ListRecords".equals(verb))w.write("<record>"+header+"<metadata><dc xmlns=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"><title xmlns=\"http://purl.org/dc/elements/1.1/\">"+xml(a.title)+"</title><creator xmlns=\"http://purl.org/dc/elements/1.1/\">"+xml(a.authors)+"</creator><identifier xmlns=\"http://purl.org/dc/elements/1.1/\">"+xml(a.doi)+"</identifier></dc></metadata></record>");else w.write(header);}w.write("</"+tag+">");}else w.write("<error code=\"badVerb\">Verb tidak didukung</error>");w.write("</OAI-PMH>");}
    private static String date(java.util.Date d){if(d==null)d=new java.util.Date(0);SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(d);}private static String xml(String v){if(v==null)return"";return v.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
}
