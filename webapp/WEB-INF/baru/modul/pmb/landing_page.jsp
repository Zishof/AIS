<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="javax.servlet.http.Cookie"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // =========================================================================
    // KONTROL AUTENTIKASI: MENANGANI PERMINTAAN LOGIN DAN LOGOUT
    // =========================================================================
    String authAction = request.getParameter("auth_action");
    
    if ("login".equals(authAction)) {
        String idCamaStr = request.getParameter("cama_id");
        String alasanGagal = "id kosong";
        if (idCamaStr != null && !idCamaStr.trim().isEmpty()) {
            BiodataCalonMahasiswa camaLogin = null;
            try {
                camaLogin = (BiodataCalonMahasiswa) GeneralValueObject.ambilData(BiodataCalonMahasiswa.class, idCamaStr.trim(), true);
            } catch (Exception e) {
                alasanGagal = "ambilData: " + e.getMessage();
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/landing_page.jsp:22");
            }
            if (camaLogin == null) {
                /* Fallback: muat langsung dengan session khusus. Jalur cache
                   ambilData bisa gagal bila session thread-local worker sedang
                   korup (mis. ditutup callee lain) - tanpa fallback ini user
                   yang datanya VALID ikut tertolak ("selalu kembali ke login"). */
                org.hibernate.Session sesiLogin = null;
                try {
                    sesiLogin = ais.database.hibernate.HibernateUtil.getSessionFactory().openSession();
                    camaLogin = (BiodataCalonMahasiswa) sesiLogin.get(BiodataCalonMahasiswa.class, Long.valueOf(idCamaStr.trim()));
                } catch (Exception e) {
                    alasanGagal = "load langsung: " + e.getMessage();
                    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/landing_page.jsp:35");
                } finally {
                    if (sesiLogin != null) {
                        try { sesiLogin.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/landing_page.jsp:38");}
                        try { sesiLogin.disconnect(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/landing_page.jsp:39");}
                        try { sesiLogin.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/landing_page.jsp:40");}
                    }
                }
            }
            if (camaLogin != null) {
                try {
                    Common.setLogin(request, response, camaLogin);
                    out.print("{\"status\":\"success\"}");
                    return;
                } catch (Exception e) {
                    alasanGagal = "setLogin: " + e.getMessage();
                    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/landing_page.jsp:51");
                }
            } else if ("id kosong".equals(alasanGagal)) {
                alasanGagal = "data tidak ditemukan";
            }
        }
        String alasanAman = alasanGagal == null ? "" : alasanGagal.replace("\\", " ").replace("\"", "'");
        out.print("{\"status\":\"error\",\"reason\":\"" + alasanAman + "\"}");
        return;
        
    } else if ("logout".equals(authAction)) {
        try {
            Common.setLogout(request, response); 
        } catch(Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/landing_page.jsp:65");
        }
        
   
        
        // SOLUSI: Menggunakan JavaScript Redirect paksa untuk menghindari blokir Header JSP/ZK
        out.print("<script>window.location.replace('" + Common.ROOT + "/logoff?forward_url=pmb');</script>");
        return;
    }
    
    // Mengecek apakah Calon Mahasiswa sudah memiliki sesi aktif
    BiodataCalonMahasiswa camaLogged = Common.isLogin(request);
%>

<jsp:include page="/WEB-INF/baru/modul/pmb/_header_pmb.jsp"></jsp:include>

<% if (camaLogged != null) { %>
    <jsp:include page="/WEB-INF/baru/modul/pmb/_sukses_login.jsp"></jsp:include>
<% } else { %>
    <jsp:include page="/WEB-INF/baru/modul/pmb/_sebelum_login.jsp"></jsp:include>
<% } %>

<jsp:include page="/WEB-INF/baru/modul/pmb/_footer_pmb.jsp"></jsp:include>