<%@page import="java.io.File"%>
<%@page import="javax.servlet.ServletContext"%>
<%@page import="javax.servlet.http.Cookie"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.asset.PenyediaAsset"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
    private String vn(String s) { return s == null ? "" : s.trim(); }
    private String vendorCfg(String key, String def) {
        try { return Common.getKonfigurasi(key, def).getNilai(); } catch (Exception e) { return def; }
    }
    private String vendorHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
    private String vendorParagraph(String s) {
        String safe = vendorHtml(s == null ? "" : s.trim());
        safe = safe.replace("\r\n", "\n").replace("\r", "\n");
        return safe.replace("\n\n", "</p><p>").replace("\n", "<br/>");
    }
    private boolean vendorCookieAktif() {
        try {
            String v = Common.getKonfigurasi("vendor_login_gunakan_cookie", "Tidak Aktif").getNilai();
            return "Aktif".equalsIgnoreCase(v) || "Ya".equalsIgnoreCase(v) || "true".equalsIgnoreCase(v);
        } catch (Exception e) { return false; }
    }
    private boolean vendorFooterSlugValid(String footer) {
        return "panduan_pendaftaran".equals(footer) || "syarat_ketentuan".equals(footer) || "kebijakan_privasi".equals(footer) || "bantuan_vendor".equals(footer);
    }
    private String vendorFooterTitle(String footer) {
        if ("syarat_ketentuan".equals(footer)) return vendorCfg("vendor_syarat_ketentuan_judul", "Syarat dan Ketentuan Portal Vendor");
        if ("kebijakan_privasi".equals(footer)) return vendorCfg("vendor_kebijakan_privasi_judul", "Kebijakan Privasi Portal Vendor");
        if ("bantuan_vendor".equals(footer)) return vendorCfg("vendor_bantuan_judul", "Bantuan Portal Vendor");
        return vendorCfg("vendor_panduan_pendaftaran_judul", "Panduan Pendaftaran Portal Vendor");
    }
    private String vendorFooterText(String footer) {
        if ("syarat_ketentuan".equals(footer)) return vendorCfg("vendor_syarat_ketentuan_text", "Syarat dan ketentuan Portal Vendor mengatur tata cara penggunaan layanan pendaftaran dan pengelolaan data rekanan secara daring. Vendor wajib memberikan data yang benar, lengkap, dan dapat dipertanggungjawabkan.");
        if ("kebijakan_privasi".equals(footer)) return vendorCfg("vendor_kebijakan_privasi_text", "Kebijakan privasi Portal Vendor menjelaskan bahwa data yang dikirimkan melalui portal digunakan untuk kebutuhan administrasi rekanan, verifikasi dokumen, komunikasi resmi, dan pengelolaan informasi pengadaan.");
        if ("bantuan_vendor".equals(footer)) return vendorCfg("vendor_bantuan_text", "Layanan bantuan Portal Vendor disediakan untuk membantu calon vendor atau vendor terdaftar ketika mengalami kendala pendaftaran, login, pengiriman ulang akses, pengisian profil, atau unggah dokumen.");
        return vendorCfg("vendor_panduan_pendaftaran_text", "Panduan Portal Vendor membantu calon rekanan memahami alur pendaftaran, kelengkapan data, dokumen persyaratan, verifikasi, dan penggunaan dashboard vendor secara tertib.");
    }
    private String vendorFooterFile(String footer) {
        if ("syarat_ketentuan".equals(footer)) return vendorCfg("vendor_footer_file_syarat_ketentuan", "/WEB-INF/baru/modul/vendor/syarat_ketentuan.jsp");
        if ("kebijakan_privasi".equals(footer)) return vendorCfg("vendor_footer_file_kebijakan_privasi", "/WEB-INF/baru/modul/vendor/kebijakan_privasi.jsp");
        if ("bantuan_vendor".equals(footer)) return vendorCfg("vendor_footer_file_bantuan", "/WEB-INF/baru/modul/vendor/bantuan.jsp");
        return vendorCfg("vendor_footer_file_panduan_pendaftaran", "/WEB-INF/baru/modul/vendor/panduan_pendaftaran.jsp");
    }
    private boolean vendorFooterFileAllowed(String path) {
        if (path == null) return false;
        String p = path.trim().replace('\\', '/');
        return p.startsWith("/WEB-INF/baru/modul/vendor/") && p.endsWith(".jsp") && p.indexOf("..") < 0;
    }
    private boolean vendorFooterFileExists(ServletContext application, String path) {
        if (!vendorFooterFileAllowed(path)) return false;
        try {
            String real = application.getRealPath(path);
            return real != null && new File(real).exists() && new File(real).isFile();
        } catch (Exception e) { return false; }
    }
    private String vendorCookieValue(HttpServletRequest request, String name) {
        try {
            Cookie[] cs = request.getCookies();
            if (cs == null) return "";
            for (int i = 0; i < cs.length; i++) if (name.equals(cs[i].getName())) return vn(cs[i].getValue());
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/landing_page.jsp:68");}
        return "";
    }
    private void vendorClearCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        try {
            Cookie c = new Cookie(name, "");
            c.setPath((request.getContextPath() == null || request.getContextPath().trim().isEmpty()) ? "/" : request.getContextPath());
            c.setMaxAge(0);
            response.addCookie(c);
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/landing_page.jsp:77");}
    }
%>
<%
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    String authAction = request.getParameter("auth_action");
    if ("logout".equals(authAction)) {
        try {
            request.getSession().removeAttribute("VENDOR_LOGGED_IN");
            request.getSession().removeAttribute("VENDOR_USER_LOGGED_IN");
            request.getSession().invalidate();
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/landing_page.jsp:91");}
        vendorClearCookie(request, response, "VENDOR_ID");
        vendorClearCookie(request, response, "VENDOR_TOKEN");
        response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + "/vendor?logout=1"));
        return;
    }

    PenyediaAsset vendorLogged = null;
    try { vendorLogged = (PenyediaAsset) request.getSession().getAttribute("VENDOR_LOGGED_IN"); } catch (Exception e) { vendorLogged = null; }

    if (vendorLogged == null && vendorCookieAktif()) {
        Session db = null;
        try {
            String idCookie = vendorCookieValue(request, "VENDOR_ID");
            String tokenCookie = vendorCookieValue(request, "VENDOR_TOKEN");
            if (idCookie.length() > 0 && tokenCookie.length() > 15) {
                db = HibernateUtil.getSessionFactory().openSession();
                PenyediaAsset v = (PenyediaAsset) db.get(PenyediaAsset.class, Long.valueOf(idCookie));
                if (v != null && Boolean.TRUE.equals(v.getAktif())) {
                    JSONObject extra = new JSONObject(v.getFormula() == null || !v.getFormula().trim().startsWith("{") ? "{}" : v.getFormula());
                    if (tokenCookie.equals(extra.optString("cookieToken"))) {
                        request.getSession().setAttribute("VENDOR_LOGGED_IN", v);
                        vendorLogged = v;
                    }
                }
            }
        } catch (Exception e) {
            vendorClearCookie(request, response, "VENDOR_ID");
            vendorClearCookie(request, response, "VENDOR_TOKEN");
        } finally {
            try { if (db != null && db.isOpen()) db.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/landing_page.jsp:121");}
        }
    }
%>
<%
    String vendorFooter = vn(request.getParameter("footer"));
    if (vendorFooterSlugValid(vendorFooter)) {
        String vendorFooterTitle = vendorFooterTitle(vendorFooter);
        String vendorFooterFile = vendorFooterFile(vendorFooter);
        boolean vendorFooterCustomAda = vendorFooterFileExists(application, vendorFooterFile);
%>
<jsp:include page="/WEB-INF/baru/modul/vendor/_header_vendor.jsp"></jsp:include>
<main class="py-5">
    <div class="container">
        <div class="row justify-content-center">
            <div class="col-lg-10">
                <div class="card border-0 shadow-lg rounded-4 overflow-hidden">
                    <div class="card-body p-4 p-lg-5">
                        <div class="mb-4">
                            <a href="<%=request.getContextPath()%>/vendor" class="btn btn-sm btn-outline-secondary rounded-pill"><i class="fas fa-arrow-left me-2"></i>Kembali ke Portal Vendor</a>
                        </div>
<%
        if (vendorFooterCustomAda) {
            try {
                pageContext.include(vendorFooterFile);
            } catch (Exception includeError) {
%>
                        <h1 class="fw-bold mb-3"><%=vendorHtml(vendorFooterTitle)%></h1>
                        <div class="alert alert-warning rounded-4"><i class="fas fa-triangle-exclamation me-2"></i>File panduan custom belum dapat dibuka. Teks default dari konfigurasi ditampilkan sementara.</div>
                        <div class="lh-lg text-secondary" style="font-size:1.02rem"><p><%=vendorParagraph(vendorFooterText(vendorFooter))%></p></div>
<%
            }
        } else {
%>
                        <h1 class="fw-bold mb-3"><%=vendorHtml(vendorFooterTitle)%></h1>
                        <div class="alert alert-info rounded-4"><i class="fas fa-circle-info me-2"></i>Halaman custom belum tersedia. Teks ini berasal dari konfigurasi Portal Vendor dan dapat diubah oleh admin.</div>
                        <div class="lh-lg text-secondary" style="font-size:1.02rem"><p><%=vendorParagraph(vendorFooterText(vendorFooter))%></p></div>
<%
        }
%>
                    </div>
                </div>
            </div>
        </div>
    </div>
</main>
<jsp:include page="/WEB-INF/baru/modul/vendor/_footer_vendor.jsp"></jsp:include>
<%
        return;
    }
%>
<jsp:include page="/WEB-INF/baru/modul/vendor/_header_vendor.jsp"></jsp:include>
<% if (vendorLogged != null) { %>
    <jsp:include page="/WEB-INF/baru/modul/vendor/setelah_login.jsp"></jsp:include>
<% } else { %>
    <jsp:include page="/WEB-INF/baru/modul/vendor/sebelum_login.jsp"></jsp:include>
<% } %>
<jsp:include page="/WEB-INF/baru/modul/vendor/_footer_vendor.jsp"></jsp:include>
