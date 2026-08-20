<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
/*
 * Tema repository mengikuti konfigurasi institusi yang sama dengan halaman AIS.
 * Sekolah lebih spesifik daripada PerguruanTinggi, sehingga diprioritaskan.
 * Hanya nama berkas sederhana yang diterima agar nilai dari database tidak dapat
 * berubah menjadi URL/path atau menyisipkan markup ke halaman.
 */
String repositoryThemeCss = null;
try {
    ais.database.model.PerguruanTinggi repositoryPt =
            ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request);
    if (repositoryPt != null) repositoryThemeCss = repositoryPt.getCss();

    ais.database.model.sekolah.Sekolah repositorySekolah =
            ais.action.master.sekolah.util.SekolahUtil.getSekolah(request);
    if (repositorySekolah != null && repositorySekolah.getId() != null
            && repositorySekolah.getCss() != null
            && !repositorySekolah.getCss().trim().isEmpty()) {
        repositoryThemeCss = repositorySekolah.getCss();
    }
} catch (Exception repositoryThemeError) {
    ais.common.ErrorAuditUtil.record(repositoryThemeError, "repository institution theme");
}

if (repositoryThemeCss != null) {
    repositoryThemeCss = repositoryThemeCss.replace('\\', '/');
    repositoryThemeCss = repositoryThemeCss.substring(repositoryThemeCss.lastIndexOf('/') + 1).trim();
    if (!repositoryThemeCss.matches("[A-Za-z0-9_-]+\\.css")) repositoryThemeCss = null;
}
long repositoryThemeVersion = System.currentTimeMillis();
%>
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/baru/base-theme.css?v=<%=repositoryThemeVersion%>">
<% if (repositoryThemeCss != null) { %>
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/baru/<%=repositoryThemeCss%>?v=<%=repositoryThemeVersion%>">
<% } %>
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/repository-modern.css?v=<%=repositoryThemeVersion%>">
