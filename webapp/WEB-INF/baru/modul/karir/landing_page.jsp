<%@page import="ais.database.model.recruitment.CalonPegawai"%>
<%@page import="ais.common.KarirConfigUtil"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String authAction = request.getParameter("auth_action");
    if ("logout".equals(authAction)) {
        try { KarirConfigUtil.clearKarirSession(request); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/landing_page.jsp:8");}
        try { KarirConfigUtil.clearLoginCookies(request, response); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/landing_page.jsp:9");}
        response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + "/karir?logout=1"));
        return;
    }
    String halamanKarir = request.getParameter("halaman");
    CalonPegawai calonLogged = null;
    try { calonLogged = (CalonPegawai) request.getSession().getAttribute("KARIR_LOGGED_IN"); } catch(Exception e) { calonLogged = null; }
    if (calonLogged == null) {
        try { calonLogged = (CalonPegawai) request.getSession().getAttribute("CalonPegawai"); } catch(Exception e) { calonLogged = null; }
    }
    if (calonLogged == null) {
        calonLogged = KarirConfigUtil.resolveLoggedCandidate(request);
    }
%>
<jsp:include page="/WEB-INF/baru/modul/karir/_header_karir.jsp"></jsp:include>
<% if (halamanKarir != null && halamanKarir.trim().length() > 0) { %>
    <jsp:include page="/WEB-INF/baru/modul/karir/_panduan_karir.jsp"></jsp:include>
<% } else if (calonLogged != null) { %>
    <jsp:include page="/WEB-INF/baru/modul/karir/setelah_login.jsp"></jsp:include>
<% } else { %>
    <jsp:include page="/WEB-INF/baru/modul/karir/sebelum_login.jsp"></jsp:include>
<% } %>
<jsp:include page="/WEB-INF/baru/modul/karir/_footer_karir.jsp"></jsp:include>
