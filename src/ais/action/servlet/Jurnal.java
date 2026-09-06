package ais.action.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.security.SecureRandom;
import java.util.Base64;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import ais.action.master.jurnal.JurnalAuthorizationService;
import ais.action.master.jurnal.JurnalDemoDataService;
import ais.action.master.jurnal.JurnalPublicService;
import ais.action.master.jurnal.JurnalNotificationPreferenceService;
import ais.action.master.jurnal.JurnalPluginParityService;
import ais.action.master.jurnal.JurnalRateLimiter;
import ais.action.master.jurnal.JurnalUsageEventService;
import ais.common.Common;
import ais.common.JurnalAksesKatalog;
import ais.common.newui.NewUiCsrfUtil;
import ais.common.newui.PortalLoginApi;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Titik masuk kanonis tunggal untuk modul jurnal ilmiah terintegrasi (mirip OJS): melayani
 * rute publik (beranda, daftar jurnal/isu/artikel, pencarian, sitasi, ekspor, feed RSS/Atom,
 * OAI-PMH) sekaligus rute admin ({@code /admin/**}) yang digerbangi login dan
 * {@link JurnalAuthorizationService}. Semua path ditangani lewat routing manual berbasis
 * {@link HttpServletRequest#getPathInfo()} di {@link #process}, bukan {@code web.xml} per-rute.
 * Header keamanan standar (CSP, X-Frame-Options, dll.) dipasang di setiap permintaan.
 */
public final class Jurnal extends HttpServlet {
    /** Versi serialisasi tetap 1L; servlet tidak pernah benar-benar diserialisasi ke stream. */
    private static final long serialVersionUID=1L;
    /** Path JSP publik (landing page + seluruh tampilan non-admin) yang menjadi tujuan forward. */
    private static final String PUBLIC_JSP="/WEB-INF/baru/modul/jurnal/landing_page.jsp";
    /** Path JSP admin (dasbor pengelolaan jurnal) yang menjadi tujuan forward rute {@code /admin/**}. */
    private static final String ADMIN_JSP="/WEB-INF/baru/modul/jurnal/admin.jsp";
    /** Layanan baca data publik (jurnal, isu, artikel, pencarian, dll.) tanpa syarat login. */
    private final JurnalPublicService publicService=new JurnalPublicService();
    /** Layanan otorisasi rute admin jurnal (mis. {@link JurnalAuthorizationService#requireRead}). */
    private final JurnalAuthorizationService auth=new JurnalAuthorizationService();

    /**
     * Menangani permintaan GET; seluruh logika didelegasikan ke {@link #process}.
     *
     * @param req permintaan HTTP masuk
     * @param res respons HTTP keluar
     * @throws ServletException diteruskan dari {@link #process}
     * @throws IOException diteruskan dari {@link #process}
     */
    protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{process(req,res);}

    /**
     * Menangani permintaan POST; seluruh logika didelegasikan ke {@link #process} (rute yang
     * memerlukan POST, mis. {@code /login}, {@code /demo-sample}, memvalidasi metode sendiri).
     *
     * @param req permintaan HTTP masuk
     * @param res respons HTTP keluar
     * @throws ServletException diteruskan dari {@link #process}
     * @throws IOException diteruskan dari {@link #process}
     */
    protected void doPost(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{process(req,res);}

    /**
     * Router utama modul jurnal. Memasang header keamanan, membatasi laju permintaan untuk
     * rute pencarian/rekomendasi ({@link JurnalRateLimiter}), lalu mencocokkan
     * {@code pathInfo} terhadap seluruh rute yang didukung (admin, login, demo data, preferensi
     * notifikasi, consent analytics, feed, browse, sitemap, OAI, sitasi, ekspor, rekomendasi,
     * fakta integritas, halaman statis per-jurnal, artikel, pengumuman, isu, jurnal/arsip,
     * pencarian) dan sebagai fallback menampilkan beranda. Galat otorisasi/validasi/tak terduga
     * masing-masing dipetakan ke kode status HTTP yang sesuai (403/422/500) tanpa membocorkan
     * detail internal ke pengguna.
     *
     * @param req permintaan HTTP masuk
     * @param res respons HTTP keluar
     * @throws ServletException tidak pernah dilempar langsung, hanya dideklarasikan oleh kontrak servlet
     * @throws IOException jika terjadi galat I/O saat forward atau menulis respons
     */
    private void process(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        String requestId=Long.toHexString(System.currentTimeMillis())+"-"+Integer.toHexString(System.identityHashCode(req));
        res.setHeader("X-Content-Type-Options","nosniff");res.setHeader("X-Frame-Options","SAMEORIGIN");res.setHeader("Referrer-Policy","strict-origin-when-cross-origin");res.setHeader("Content-Security-Policy","default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; frame-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'");res.setHeader("Permissions-Policy","camera=(), microphone=(), geolocation=()");res.setHeader("X-Request-Id",requestId);
        try{
            req.setCharacterEncoding("UTF-8");String path=clean(req.getPathInfo());
            if(("/search".equals(path)||path.startsWith("/recommend/"))&&!JurnalRateLimiter.allow("public-query",req.getRemoteAddr(),120,60000L)){res.sendError(429,"Terlalu banyak permintaan.");return;}
            if(path.startsWith("/admin")){admin(req,res,path);return;}
            if("/login".equals(path)){login(req,res);return;}
            if("/demo-sample".equals(path)){demoSample(req,res);return;}
            if("/preferences".equals(path)){preferences(req,res);return;}
            if("/analytics-consent".equals(path)){analyticsConsent(req,res);return;}
            if("/feed".equals(path)||"/feed.xml".equals(path)){feed(req,res);return;}
            if("/browse".equals(path)){browse(req);req.getRequestDispatcher(PUBLIC_JSP).forward(req,res);return;}
            if("/sitemap.xml".equals(path)){sitemap(req,res);return;}
            if("/oai".equals(path)){req.getRequestDispatcher("/oai").forward(req,res);return;}
            if(path.startsWith("/citation/")){citation(req,res,path);return;}
            if(path.startsWith("/export/")){export(req,res,path);return;}
            if(path.startsWith("/recommend/")){recommend(req,res,path);return;}
            if(path.startsWith("/facts/")){facts(req,res,path);return;}
            if(path.matches("/journal/[^/]+/page/[^/]+")){String[] parts=path.split("/");JurnalPublicService.StaticPage page=publicService.staticPage(parts[2],parts[4]);if(page==null){res.sendError(404);return;}req.setAttribute("jurnalView","staticPage");req.setAttribute("jurnalStaticPage",page);}
            else if(path.startsWith("/article/")){Long id=parseLong(path.substring("/article/".length()));JurnalPublicService.ArticleCard item=publicService.article(id);if(item==null){res.sendError(404);return;}req.setAttribute("jurnalView","article");req.setAttribute("jurnalArticle",item);recordUsage(item.id,null,"VIEW",Common.getCurrentUser(req),req);}
            else if(path.startsWith("/announcement/")){Long id=parseLong(path.substring("/announcement/".length()));JurnalPublicService.ArticleCard item=publicService.announcement(id);if(item==null){res.sendError(404);return;}req.setAttribute("jurnalView","announcement");req.setAttribute("jurnalAnnouncement",item);}
            else if(path.startsWith("/issue/")){JurnalPublicService.IssueCard issue=publicService.issue(parseLong(path.substring(7)));if(issue==null){res.sendError(404);return;}req.setAttribute("jurnalView","issue");req.setAttribute("jurnalIssue",issue);}
            else if(path.startsWith("/journal/")||path.startsWith("/archive/")){String slug=path.substring(path.indexOf('/',1)+1);JurnalPublicService.JournalCard journal=publicService.journal(slug);if(journal==null){res.sendError(404);return;}req.setAttribute("jurnalView","journal");req.setAttribute("jurnalJournal",journal);req.setAttribute("jurnalIssues",publicService.issues(journal.id,page(req),20));configureAnalytics(req,res,journal);}
            else if("/search".equals(path)){req.setAttribute("jurnalView","search");req.setAttribute("jurnalSearchTerm",clean(req.getParameter("q")));req.setAttribute("jurnalSearch",publicService.search(req.getParameter("q"),parseLong(req.getParameter("journal")),page(req),20));}
            else{req.setAttribute("jurnalView","home");req.setAttribute("jurnalHome",publicService.home());}
            preparePortalIdentity(req);req.getRequestDispatcher(PUBLIC_JSP).forward(req,res);
        }catch(SecurityException e){if(!res.isCommitted())res.sendError(403,"Hak akses jurnal tidak tersedia.");}
        catch(IllegalArgumentException e){if(!res.isCommitted())res.sendError(422,e.getMessage());}
        catch(Exception e){ais.common.ErrorAuditUtil.recordVisibleFailure(e,"Jurnal servlet",req,requestId);if(!res.isCommitted())res.sendError(500,"Modul jurnal belum dapat melayani permintaan. ID: "+requestId);}
        finally{HibernateUtil.closeSession();}
    }
    /**
     * Menangani rute {@code /login}: GET dialihkan (303) ke beranda dengan anchor login (form
     * login tampil di JSP publik); POST memvalidasi kredensial lewat {@link PortalLoginApi} dan
     * mengalihkan ke beranda jika sukses, atau menampilkan ulang beranda dengan pesan galat.
     *
     * @param req permintaan HTTP masuk
     * @param res respons HTTP keluar
     * @throws Exception diteruskan apa adanya ke {@link #process} untuk dipetakan ke status HTTP
     */
    private void login(HttpServletRequest req,HttpServletResponse res)throws Exception{
        res.setHeader("Cache-Control","no-store");
        if(!"POST".equalsIgnoreCase(req.getMethod())){res.setStatus(HttpServletResponse.SC_SEE_OTHER);res.setHeader("Location",req.getContextPath()+"/jurnal#jurnal-login");return;}
        JSONObject result=PortalLoginApi.handle(req,res,"jurnal");
        if(result.optBoolean("ok")){res.setStatus(HttpServletResponse.SC_SEE_OTHER);res.setHeader("Location",req.getContextPath()+"/jurnal");return;}
        req.setAttribute("jurnalLoginError",result.optString("message","Otentikasi gagal."));
        req.setAttribute("jurnalView","home");req.setAttribute("jurnalHome",publicService.home());preparePortalIdentity(req);
        req.getRequestDispatcher(PUBLIC_JSP).forward(req,res);
    }
    /**
     * Menangani rute {@code /demo-sample}: hanya untuk administrator yang sudah login (gerbang
     * ganda: {@code user==null} -> 401, {@code !getApakahAdmin()} -> 403), hanya menerima POST,
     * dan wajib token CSRF valid ({@link NewUiCsrfUtil}). Men-generate atau menghapus data
     * sample 50 jurnal x 100 artikel via {@link JurnalDemoDataService}, dengan konfirmasi
     * eksplisit ({@code confirmed=generate-50x100}/{@code delete-50x100}) untuk mencegah
     * eksekusi tak sengaja. Hasil/galat disimpan sebagai flash message di sesi.
     *
     * @param req permintaan HTTP masuk
     * @param res respons HTTP keluar
     * @throws Exception diteruskan apa adanya ke {@link #process} untuk dipetakan ke status HTTP
     */
    private void demoSample(HttpServletRequest req,HttpServletResponse res)throws Exception{
        Tbmuser user=Common.getCurrentUser(req);if(user==null){res.sendError(401,"Login diperlukan.");return;}
        if(!Common.getApakahAdmin()){res.sendError(403,"Hanya administrator yang boleh mengelola data sample.");return;}
        if(!"POST".equalsIgnoreCase(req.getMethod())){res.setStatus(HttpServletResponse.SC_SEE_OTHER);res.setHeader("Location",req.getContextPath()+"/jurnal");return;}
        if(!NewUiCsrfUtil.isValid(req))throw new SecurityException("Token CSRF jurnal tidak valid.");
        HttpSession session=req.getSession(true);String action=clean(req.getParameter("sampleAction"));
        try{
            JurnalDemoDataService service=new JurnalDemoDataService();
            if("generate".equals(action)&&"generate-50x100".equals(req.getParameter("confirmed"))){JurnalDemoDataService.Result x=service.generate(50,100,Long.valueOf(245L),JurnalDemoDataService.DEFAULT_AUTHOR,"sample-50x100",JurnalDemoDataService.SAMPLE_CONFIRMATION,user);session.setAttribute("jurnalSampleFlash","Data sample siap: 50 jurnal × 100 artikel; jurnal baru "+x.journalsCreated+", artikel baru "+x.articlesCreated+".");}
            else if("delete".equals(action)&&"delete-50x100".equals(req.getParameter("confirmed"))){JurnalDemoDataService.RemoveResult x=service.removeSample("sample-50x100",JurnalDemoDataService.DELETE_SAMPLE_CONFIRMATION,user);session.setAttribute("jurnalSampleFlash","Data sample dihapus: "+x.journalsRemoved+" jurnal, "+x.articlesRemoved+" artikel.");}
            else throw new IllegalArgumentException("Konfirmasi pengelolaan data sample wajib dipilih.");
        }catch(RuntimeException e){session.setAttribute("jurnalSampleFlash","Data sample gagal: "+e.getMessage());session.setAttribute("jurnalSampleFlashError",Boolean.TRUE);}
        res.setStatus(HttpServletResponse.SC_SEE_OTHER);res.setHeader("Location",req.getContextPath()+"/jurnal");
    }
    /**
     * Menyiapkan atribut identitas portal untuk JSP publik: token CSRF, flash message sample
     * data (jika ada, sekali pakai lalu dihapus dari sesi), serta status login/nama
     * tampilan/status admin pengguna saat ini (jika ada yang login).
     *
     * @param req permintaan HTTP masuk; atribut hasil disetel langsung ke objek ini
     */
    private void preparePortalIdentity(HttpServletRequest req){HttpSession session=req.getSession(true);req.setAttribute("jurnalPortalCsrf",NewUiCsrfUtil.getToken(session));Object flash=session.getAttribute("jurnalSampleFlash");if(flash!=null){req.setAttribute("jurnalSampleFlash",flash);req.setAttribute("jurnalSampleFlashError",session.getAttribute("jurnalSampleFlashError"));session.removeAttribute("jurnalSampleFlash");session.removeAttribute("jurnalSampleFlashError");}Tbmuser user=Common.getCurrentUser(req);if(user==null)return;req.setAttribute("jurnalAuthenticated",Boolean.TRUE);req.setAttribute("jurnalCurrentUserName",clean(user.getUserNama()).length()==0?user.getUserId():user.getUserNama());try{if(Common.getApakahAdmin())req.setAttribute("jurnalAdmin",Boolean.TRUE);}catch(Exception ignored){req.removeAttribute("jurnalAdmin");}}
    /**
     * Menangani seluruh rute {@code /admin/**}: mewajibkan login (redirect ke {@code /login2}
     * dengan {@code returnTo} jika belum), token CSRF valid untuk POST, menormalkan kunci rute
     * ke bentuk kanonis via {@link JurnalAksesKatalog#canonical} (404 jika tidak dikenal, 301
     * jika ada alias non-kanonis untuk GET, 409 untuk metode non-GET pada alias), lalu
     * menggerbangi akses baca lewat {@link JurnalAuthorizationService#requireRead} sebelum
     * memforward ke {@link #ADMIN_JSP}.
     *
     * @param req permintaan HTTP masuk
     * @param res respons HTTP keluar
     * @param path sisa path setelah {@code /jurnal}, diawali {@code /admin}
     * @throws Exception diteruskan apa adanya ke {@link #process} untuk dipetakan ke status HTTP
     */
    private void admin(HttpServletRequest req,HttpServletResponse res,String path)throws Exception{
        Tbmuser user=Common.getCurrentUser(req);if(user==null){res.sendRedirect(req.getContextPath()+"/login2?returnTo="+req.getContextPath()+"/jurnal"+path);return;}
        if("POST".equalsIgnoreCase(req.getMethod())&&!NewUiCsrfUtil.isValid(req))throw new SecurityException("Token CSRF jurnal tidak valid.");
        String key=path.length()>7?clean(path.substring(7)):"dashboard";if(key.indexOf('/')>=0)key=key.substring(0,key.indexOf('/'));
        String canonical=JurnalAksesKatalog.canonical(key);if(canonical==null){res.sendError(404);return;}
        if(!canonical.equals(key)){if(!"GET".equalsIgnoreCase(req.getMethod())){res.sendError(409,"Gunakan route jurnal canonical.");return;}res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);res.setHeader("Location",req.getContextPath()+"/jurnal/admin/"+canonical);return;}key=canonical;
        auth.requireRead(user,key);req.setAttribute("jurnalAdminKey",key);req.setAttribute("jurnalCsrf",NewUiCsrfUtil.getToken(req.getSession(true)));
        for(JurnalAksesKatalog.Entri e:JurnalAksesKatalog.DAFTAR)if(e.kunci.equals(key)){req.setAttribute("jurnalAdminTitle",e.label);break;}
        req.setAttribute("jurnalAdminEntries",JurnalAksesKatalog.DAFTAR);req.getRequestDispatcher(ADMIN_JSP).forward(req,res);
    }
    /**
     * Menangani rute {@code /preferences}: memerlukan login (401 jika tidak) dan
     * {@code journalId} valid (422 jika tidak). POST (dengan CSRF valid) menyimpan preferensi
     * notifikasi (email/in-app/digest/daftar unsubscribe per topik) lewat
     * {@link JurnalNotificationPreferenceService}; setiap panggilan (GET maupun POST) membalas
     * JSON berisi preferensi terkini pengguna beserta token CSRF baru.
     *
     * @param req permintaan HTTP masuk
     * @param res respons HTTP keluar; dibalas JSON dengan {@code Cache-Control: private, no-store}
     * @throws Exception diteruskan apa adanya ke {@link #process} untuk dipetakan ke status HTTP
     */
    private void preferences(HttpServletRequest req,HttpServletResponse res)throws Exception{
        Tbmuser user=Common.getCurrentUser(req);if(user==null){res.sendError(401,"Login diperlukan.");return;}Long journalId=parseLong(req.getParameter("journalId"));if(journalId==null)throw new IllegalArgumentException("journalId wajib valid.");JurnalNotificationPreferenceService service=new JurnalNotificationPreferenceService();
        if("POST".equalsIgnoreCase(req.getMethod())){if(!NewUiCsrfUtil.isValid(req))throw new SecurityException("Token CSRF jurnal tidak valid.");Set<String> keys=new HashSet<String>();String raw=req.getParameter("unsubscribed");if(raw!=null)for(String key:raw.split(","))if(key.trim().length()>0)keys.add(key.trim());service.save(journalId,"true".equalsIgnoreCase(req.getParameter("email")),"true".equalsIgnoreCase(req.getParameter("inApp")),req.getParameter("digest"),keys,user);}
        JurnalNotificationPreferenceService.Preference p=service.load(journalId,user.getUserId());JSONObject out=new JSONObject().put("ok",true).put("journalId",journalId).put("email",p.email).put("inApp",p.inApp).put("digest",p.digest).put("unsubscribed",new JSONArray(p.unsubscribed)).put("csrf",NewUiCsrfUtil.getToken(req.getSession(true)));res.setContentType("application/json; charset=UTF-8");res.setHeader("Cache-Control","private, no-store");res.getWriter().write(out.toString());
    }
    /**
     * Menyiapkan atribut tampilan untuk rute {@code /browse} (jelajah artikel berdasarkan
     * subjek/topik, opsional dibatasi ke satu jurnal): daftar subjek yang tersedia, dan jika
     * subjek dipilih, daftar artikel dalam subjek tersebut (terpaging).
     *
     * @param req permintaan HTTP masuk; parameter {@code journal} dan {@code subject} dibaca di sini,
     *        atribut hasil disetel langsung ke objek ini
     */
    private void browse(HttpServletRequest req){Long journal=parseLong(req.getParameter("journal"));String subject=clean(req.getParameter("subject"));req.setAttribute("jurnalView","browse");req.setAttribute("jurnalBrowseSubjects",publicService.subjects(journal,100));req.setAttribute("jurnalBrowseSubject",subject);if(subject.length()>0)req.setAttribute("jurnalBrowseArticles",publicService.browseSubject(subject,journal,page(req),20));}
    /**
     * Menangani rute {@code /analytics-consent}: hanya POST (405 selain itu) dengan token CSRF
     * valid, memvalidasi pilihan consent ({@code granted}/{@code denied}) dan slug jurnal,
     * lalu menyetel cookie {@code AIS_JURNAL_ANALYTICS} (HttpOnly, SameSite=Lax, 1 tahun) dan
     * mengalihkan kembali ke halaman jurnal terkait.
     *
     * @param req permintaan HTTP masuk; parameter {@code choice} dan {@code journal} dibaca di sini
     * @param res respons HTTP keluar
     * @throws Exception diteruskan apa adanya ke {@link #process} untuk dipetakan ke status HTTP
     */
    private void analyticsConsent(HttpServletRequest req,HttpServletResponse res)throws Exception{if(!"POST".equalsIgnoreCase(req.getMethod())){res.sendError(405);return;}if(!NewUiCsrfUtil.isValid(req))throw new SecurityException("Token CSRF analytics tidak valid.");String choice=clean(req.getParameter("choice"));if(!choice.matches("granted|denied"))throw new IllegalArgumentException("Pilihan consent tidak valid.");String slug=clean(req.getParameter("journal"));if(!slug.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,79}"))throw new IllegalArgumentException("Jurnal tidak valid.");String cookie="AIS_JURNAL_ANALYTICS="+choice+"; Max-Age=31536000; Path="+req.getContextPath()+"/jurnal; HttpOnly; SameSite=Lax"+(req.isSecure()?"; Secure":"");res.addHeader("Set-Cookie",cookie);res.setStatus(303);res.setHeader("Location",req.getContextPath()+"/jurnal/journal/"+slug);}
    /**
     * Menyiapkan Google Analytics 4 (GA4) untuk halaman jurnal tertentu, hanya jika jurnal
     * mengonfigurasi provider {@code GA4} dengan measurement id valid (pola {@code G-XXXXXXXX}).
     * Skrip GA hanya benar-benar diaktifkan jika pengguna sudah memberi consent (cookie
     * {@code AIS_JURNAL_ANALYTICS=granted}) dan tidak mengirim header Do-Not-Track/Global
     * Privacy Control; nonce CSP acak dibuat per permintaan dan header
     * {@code Content-Security-Policy} diperketat untuk mengizinkan domain GA secara eksplisit.
     *
     * @param req permintaan HTTP masuk, dipakai membaca cookie consent dan header DNT/GPC
     * @param res respons HTTP keluar, dipakai menyetel header CSP saat GA diaktifkan
     * @param journal kartu jurnal yang sedang ditampilkan, berisi konfigurasi analytics-nya
     */
    private void configureAnalytics(HttpServletRequest req,HttpServletResponse res,JurnalPublicService.JournalCard journal){if(!"GA4".equals(journal.analyticsProvider)||journal.analyticsMeasurementId==null||!journal.analyticsMeasurementId.matches("G-[A-Z0-9]{6,20}"))return;req.setAttribute("jurnalAnalyticsConfigured",Boolean.TRUE);req.setAttribute("jurnalCsrf",NewUiCsrfUtil.getToken(req.getSession(true)));if(!consent(req)||"1".equals(req.getHeader("DNT"))||"1".equals(req.getHeader("Sec-GPC")))return;byte[] bytes=new byte[18];new SecureRandom().nextBytes(bytes);String nonce=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);req.setAttribute("jurnalAnalyticsMeasurementId",journal.analyticsMeasurementId);req.setAttribute("jurnalAnalyticsNonce",nonce);res.setHeader("Content-Security-Policy","default-src 'self'; img-src 'self' data: https://www.google-analytics.com; style-src 'self' 'unsafe-inline'; script-src 'self' 'nonce-"+nonce+"' https://www.googletagmanager.com; connect-src 'self' https://www.google-analytics.com https://region1.google-analytics.com; frame-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'");}
    /**
     * Membaca cookie {@code AIS_JURNAL_ANALYTICS} pada permintaan untuk menentukan apakah
     * pengguna sudah memberi consent analytics.
     *
     * @param req permintaan HTTP masuk
     * @return {@code true} jika cookie consent bernilai {@code granted}; {@code false} jika
     *         cookie tidak ada atau bernilai lain
     */
    private boolean consent(HttpServletRequest req){Cookie[] values=req.getCookies();if(values!=null)for(Cookie c:values)if("AIS_JURNAL_ANALYTICS".equals(c.getName()))return"granted".equals(c.getValue());return false;}

    /**
     * Merapikan nilai string dengan {@code trim()}, memperlakukan {@code null} sebagai string kosong.
     *
     * @param v nilai mentah; boleh {@code null}
     * @return nilai yang sudah di-trim; string kosong jika {@code v} {@code null}
     */
    private String clean(String v){return v==null?"":v.trim();}

    /**
     * Mencatat event penggunaan (mis. {@code VIEW}/{@code DOWNLOAD}) artikel/berkas jurnal lewat
     * {@link JurnalUsageEventService}; galat pencatatan tidak pernah mengganggu alur utama,
     * hanya direkam via {@link ais.common.ErrorAuditUtil}.
     *
     * @param itemId id artikel/item yang diakses; boleh {@code null} untuk event non-artikel
     * @param bitstreamId id berkas terkait; boleh {@code null}
     * @param type jenis event (mis. {@code VIEW}, {@code DOWNLOAD})
     * @param actor pengguna yang melakukan aksi; boleh {@code null} untuk pengunjung anonim
     * @param req permintaan HTTP terkait, dipakai layanan untuk konteks tambahan (mis. IP)
     */
    private void recordUsage(Long itemId,Long bitstreamId,String type,Tbmuser actor,HttpServletRequest req){try{new JurnalUsageEventService().record(itemId,bitstreamId,type,actor,req);}catch(Exception e){ais.common.ErrorAuditUtil.record(e,"Jurnal usage "+type);}}

    /**
     * Mengurai string menjadi {@link Long}, mengembalikan {@code null} alih-alih melempar
     * galat jika nilai bukan angka valid.
     *
     * @param v nilai string yang akan diurai; boleh {@code null}
     * @return nilai {@link Long} hasil parse, atau {@code null} jika gagal
     */
    private Long parseLong(String v){try{return Long.valueOf(v);}catch(Exception e){return null;}}

    /**
     * Mengurai parameter {@code page} dari permintaan sebagai indeks halaman non-negatif,
     * dengan fallback ke {@code 0} jika parameter tidak ada atau bukan angka valid.
     *
     * @param r permintaan HTTP masuk
     * @return indeks halaman (>= 0); {@code 0} jika parameter tidak valid
     */
    private int page(HttpServletRequest r){try{return Math.max(0,Integer.parseInt(r.getParameter("page")));}catch(Exception e){return 0;}}

    /**
     * Menangani rute {@code /citation/{id}.{format}}: menghasilkan sitasi artikel dalam format
     * yang diminta (bibtex, ris, apa, vancouver, ieee, csljson/json) lewat
     * {@link JurnalPublicService#citation}, dibalas sebagai unduhan (attachment) dengan MIME
     * dan ekstensi sesuai format. Format tak dikenal ditolak dengan 422; artikel tak ditemukan
     * dibalas 404.
     *
     * @param req permintaan HTTP masuk
     * @param res respons HTTP keluar
     * @param path sisa path setelah {@code /jurnal}, diawali {@code /citation/}
     * @throws Exception diteruskan apa adanya ke {@link #process} untuk dipetakan ke status HTTP
     */
    private void citation(HttpServletRequest req,HttpServletResponse res,String path)throws Exception{String x=path.substring("/citation/".length());int dot=x.lastIndexOf('.');String format=(dot<0?"bibtex":x.substring(dot+1)).toLowerCase();if(!format.matches("bibtex|bib|ris|apa|vancouver|ieee|csljson|json"))throw new IllegalArgumentException("Format sitasi tidak didukung.");if("bib".equals(format))format="bibtex";Long id=parseLong(dot<0?x:x.substring(0,dot));String value=publicService.citation(id,format);if(value==null){res.sendError(404);return;}String mime="ris".equals(format)?"application/x-research-info-systems":("csljson".equals(format)||"json".equals(format))?"application/vnd.citationstyles.csl+json":("bibtex".equals(format)?"application/x-bibtex":"text/plain");String ext="bibtex".equals(format)?"bib":("csljson".equals(format)?"json":format);res.setContentType(mime+"; charset=UTF-8");res.setHeader("Content-Disposition","attachment; filename=ais-journal-"+id+"."+ext);res.getWriter().write(value);}
    /**
     * Menangani rute {@code /export/{id}.{format}}: menghasilkan ekspor metadata artikel XML
     * (mis. untuk plugin OJS/PKP lain) lewat {@link JurnalPluginParityService#exportArticle},
     * dibalas sebagai unduhan (attachment). Artikel/format tak ditemukan dibalas 404.
     *
     * @param req permintaan HTTP masuk
     * @param res respons HTTP keluar
     * @param path sisa path setelah {@code /jurnal}, diawali {@code /export/}
     * @throws Exception diteruskan apa adanya ke {@link #process} untuk dipetakan ke status HTTP
     */
    private void export(HttpServletRequest req,HttpServletResponse res,String path)throws Exception{String x=path.substring("/export/".length());int dot=x.lastIndexOf('.');if(dot<1)throw new IllegalArgumentException("Format ekspor wajib diisi.");Long id=parseLong(x.substring(0,dot));String format=x.substring(dot+1).toLowerCase();String value=new JurnalPluginParityService().exportArticle(id,format);if(value==null){res.sendError(404);return;}res.setContentType("application/xml; charset=UTF-8");res.setHeader("Content-Disposition","attachment; filename=ais-journal-"+id+"."+format+".xml");res.getWriter().write(value);}
    /**
     * Menangani rute {@code /recommend/{id}}: mengembalikan JSON daftar artikel rekomendasi
     * (default mode {@code similarity}, maksimal 20) lewat
     * {@link JurnalPluginParityService#recommendations}, di-cache publik 5 menit. Artikel tak
     * ditemukan dibalas 404.
     *
     * @param req permintaan HTTP masuk; parameter {@code mode} opsional
     * @param res respons HTTP keluar
     * @param path sisa path setelah {@code /jurnal}, diawali {@code /recommend/}
     * @throws Exception diteruskan apa adanya ke {@link #process} untuk dipetakan ke status HTTP
     */
    private void recommend(HttpServletRequest req,HttpServletResponse res,String path)throws Exception{Long id=parseLong(path.substring("/recommend/".length()));org.json.JSONArray value=new JurnalPluginParityService().recommendations(id,clean(req.getParameter("mode")).length()==0?"similarity":req.getParameter("mode"),20);if(value==null){res.sendError(404);return;}res.setContentType("application/json; charset=UTF-8");res.setHeader("Cache-Control","public, max-age=300");res.getWriter().write(new JSONObject().put("ok",true).put("articles",value).toString());}
    /**
     * Menangani rute {@code /facts/{id}}: mengembalikan JSON fakta integritas artikel (mis.
     * riwayat sitasi/verifikasi) lewat {@link JurnalPluginParityService#integrityFacts},
     * di-cache publik 5 menit. Artikel tak ditemukan dibalas 404.
     *
     * @param req permintaan HTTP masuk
     * @param res respons HTTP keluar
     * @param path sisa path setelah {@code /jurnal}, diawali {@code /facts/}
     * @throws Exception diteruskan apa adanya ke {@link #process} untuk dipetakan ke status HTTP
     */
    private void facts(HttpServletRequest req,HttpServletResponse res,String path)throws Exception{Long id=parseLong(path.substring("/facts/".length()));JSONObject value=new JurnalPluginParityService().integrityFacts(id);if(value==null){res.sendError(404);return;}res.setContentType("application/json; charset=UTF-8");res.setHeader("Cache-Control","public, max-age=300");res.getWriter().write(value.toString());}
    /**
     * Menangani rute {@code /feed} dan {@code /feed.xml}: menghasilkan feed artikel terbaru
     * atau pengumuman ({@code kind=announcements}) dalam format Atom (default) atau RSS
     * ({@code format=rss}), maksimal 50 entri.
     *
     * @param req permintaan HTTP masuk; parameter {@code kind} dan {@code format} dibaca di sini
     * @param res respons HTTP keluar; content type Atom atau RSS sesuai parameter
     * @throws Exception diteruskan apa adanya ke {@link #process} untuk dipetakan ke status HTTP
     */
    private void feed(HttpServletRequest req,HttpServletResponse res)throws Exception{String kind=clean(req.getParameter("kind"));boolean announcements="announcements".equalsIgnoreCase(kind);boolean rss="rss".equalsIgnoreCase(req.getParameter("format"));res.setContentType((rss?"application/rss+xml":"application/atom+xml")+"; charset=UTF-8");String base=req.getRequestURL().toString().replaceAll("/feed(?:\\.xml)?$","");java.util.List<JurnalPublicService.ArticleCard>rows=announcements?publicService.announcements(parseLong(req.getParameter("journal")),50):publicService.latest(50);String segment=announcements?"/announcement/":"/article/";PrintWriter w=res.getWriter();if(rss){w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><rss version=\"2.0\"><channel><title>Jurnal Ilmiah eCampus</title><link>"+xml(base)+"</link><description>"+(announcements?"Pengumuman jurnal":"Artikel jurnal terbaru")+"</description>");for(JurnalPublicService.ArticleCard a:rows){String link=base+segment+a.id;w.write("<item><guid>"+xml(link)+"</guid><title>"+xml(a.title)+"</title><link>"+xml(link)+"</link><description>"+xml(a.abstractText)+"</description><pubDate>"+rfc822(a.publishedAt)+"</pubDate></item>");}w.write("</channel></rss>");}else{w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><feed xmlns=\"http://www.w3.org/2005/Atom\"><title>Jurnal Ilmiah eCampus</title><id>"+xml(base)+"</id>");for(JurnalPublicService.ArticleCard a:rows){String link=base+segment+a.id;w.write("<entry><id>"+xml(link)+"</id><title>"+xml(a.title)+"</title><link href=\""+xml(link)+"\"/><updated>"+date(a.publishedAt)+"</updated><summary>"+xml(a.abstractText)+"</summary></entry>");}w.write("</feed>");}}
    /**
     * Menangani rute {@code /sitemap.xml}: menghasilkan sitemap XML sederhana berisi URL
     * beranda jurnal dan hingga 100 artikel terbaru beserta tanggal modifikasinya.
     *
     * @param req permintaan HTTP masuk
     * @param res respons HTTP keluar; content type {@code application/xml}
     * @throws Exception diteruskan apa adanya ke {@link #process} untuk dipetakan ke status HTTP
     */
    private void sitemap(HttpServletRequest req,HttpServletResponse res)throws Exception{res.setContentType("application/xml; charset=UTF-8");String base=req.getRequestURL().toString().replaceAll("/sitemap\\.xml$","");PrintWriter w=res.getWriter();w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"><url><loc>"+xml(base)+"</loc></url>");for(JurnalPublicService.ArticleCard a:publicService.latest(100)){w.write("<url><loc>"+xml(base+"/article/"+a.id)+"</loc><lastmod>"+date(a.publishedAt)+"</lastmod></url>");}w.write("</urlset>");}
    /**
     * Implementasi ringkas protokol OAI-PMH 2.0 (verb {@code Identify},
     * {@code ListMetadataFormats}, {@code ListIdentifiers}, {@code ListRecords}; verb lain
     * dibalas {@code badVerb}) untuk harvesting metadata artikel oleh agregator eksternal.
     *
     * <p><b>Catatan:</b> method ini saat ini TIDAK dipanggil dari {@link #process} -- rute
     * {@code /oai} di {@link #process} memforward ke servlet lain bernama {@code /oai}, bukan
     * memanggil method ini. Method ini tampak sebagai sisa implementasi lama/duplikat; jangan
     * dihapus tanpa memastikan tidak ada pemanggil lain (reflection/servlet lama) yang bergantung
     * padanya.</p>
     *
     * @param req permintaan HTTP masuk; parameter {@code verb} dibaca di sini
     * @param res respons HTTP keluar; content type {@code application/xml}
     * @throws Exception diteruskan apa adanya ke pemanggil
     */
    private void oai(HttpServletRequest req,HttpServletResponse res)throws Exception{res.setContentType("application/xml; charset=UTF-8");String verb=clean(req.getParameter("verb"));String base=req.getRequestURL().toString();PrintWriter w=res.getWriter();w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\"><responseDate>"+date(new java.util.Date())+"</responseDate><request verb=\""+xml(verb)+"\">"+xml(base)+"</request>");if("Identify".equals(verb)){w.write("<Identify><repositoryName>Jurnal Ilmiah eCampus</repositoryName><baseURL>"+xml(base)+"</baseURL><protocolVersion>2.0</protocolVersion><adminEmail>noreply@localhost</adminEmail><earliestDatestamp>1970-01-01T00:00:00Z</earliestDatestamp><deletedRecord>persistent</deletedRecord><granularity>YYYY-MM-DDThh:mm:ssZ</granularity></Identify>");}else if("ListMetadataFormats".equals(verb)){w.write("<ListMetadataFormats><metadataFormat><metadataPrefix>oai_dc</metadataPrefix><schema>http://www.openarchives.org/OAI/2.0/oai_dc.xsd</schema><metadataNamespace>http://www.openarchives.org/OAI/2.0/oai_dc/</metadataNamespace></metadataFormat></ListMetadataFormats>");}else if("ListIdentifiers".equals(verb)||"ListRecords".equals(verb)){String tag=verb;w.write("<"+tag+">");for(JurnalPublicService.ArticleCard a:publicService.latest(100)){String header="<header><identifier>oai:ais:jurnal:"+a.id+"</identifier><datestamp>"+date(a.publishedAt)+"</datestamp><setSpec>journal:"+a.collectionId+"</setSpec></header>";if("ListRecords".equals(verb))w.write("<record>"+header+"<metadata><dc xmlns=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"><title xmlns=\"http://purl.org/dc/elements/1.1/\">"+xml(a.title)+"</title><creator xmlns=\"http://purl.org/dc/elements/1.1/\">"+xml(a.authors)+"</creator><identifier xmlns=\"http://purl.org/dc/elements/1.1/\">"+xml(a.doi)+"</identifier></dc></metadata></record>");else w.write(header);}w.write("</"+tag+">");}else w.write("<error code=\"badVerb\">Verb tidak didukung</error>");w.write("</OAI-PMH>");}
    /**
     * Memformat tanggal ke format UTC ISO-8601 ({@code yyyy-MM-dd'T'HH:mm:ss'Z'}) untuk feed
     * Atom, sitemap, dan OAI-PMH.
     *
     * @param d tanggal yang akan diformat; {@code null} diperlakukan sebagai epoch (1970-01-01)
     * @return representasi string tanggal dalam UTC
     */
    private static String date(java.util.Date d){if(d==null)d=new java.util.Date(0);SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(d);}

    /**
     * Memformat tanggal ke format RFC 822 (mis. {@code Mon, 01 Jan 2024 00:00:00 GMT}) yang
     * dipakai elemen {@code pubDate} pada feed RSS.
     *
     * @param d tanggal yang akan diformat; {@code null} diperlakukan sebagai epoch (1970-01-01)
     * @return representasi string tanggal dalam format RFC 822, zona waktu GMT
     */
    private static String rfc822(java.util.Date d){if(d==null)d=new java.util.Date(0);SimpleDateFormat f=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z",java.util.Locale.ENGLISH);f.setTimeZone(TimeZone.getTimeZone("GMT"));return f.format(d);}

    /**
     * Meng-escape karakter spesial XML ({@code & < > " '}) agar nilai aman disisipkan ke dalam
     * dokumen XML (sitemap, feed, OAI-PMH).
     *
     * @param v nilai mentah yang akan disisipkan ke XML; boleh {@code null}
     * @return nilai yang sudah di-escape; string kosong jika {@code v} {@code null}
     */
    private static String xml(String v){if(v==null)return"";return v.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
}
