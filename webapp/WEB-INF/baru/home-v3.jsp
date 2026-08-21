<%@page import="ais.common.home.HomePortalViewModel"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
private String h(String value) {
    if (value == null) return "";
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
}
%>
<%
HomePortalViewModel vm = (HomePortalViewModel) request.getAttribute("homePortal");
if (vm == null) { response.sendRedirect(request.getContextPath() + "/"); return; }
%>
<!doctype html>
<html lang="<%=h(vm.language)%>" dir="<%=h(vm.direction)%>">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
    <meta name="theme-color" content="#0b2148">
    <title><%=h(vm.seo.title)%></title>
    <meta name="description" content="<%=h(vm.seo.description)%>">
    <link rel="canonical" href="<%=h(vm.seo.canonical)%>">
    <meta property="og:type" content="website">
    <meta property="og:title" content="<%=h(vm.seo.title)%>">
    <meta property="og:description" content="<%=h(vm.seo.description)%>">
    <meta property="og:url" content="<%=h(vm.seo.canonical)%>">
    <meta property="og:image" content="<%=h(vm.seo.image)%>">
    <meta name="twitter:card" content="summary_large_image">
    <link rel="icon" href="<%=h(vm.institution.logoUrl)%>">
    <link rel="preload" as="image" href="<%=h(vm.institution.heroUrl)%>" fetchpriority="high">
    <link rel="stylesheet" href="<%=h(vm.contextPath)%>/css/baru/home-v3-icons.css?v=<%=h(vm.assetVersion)%>">
    <% if (vm.institution.themeCss != null && vm.institution.themeCss.length() > 0) { %><link rel="stylesheet" href="<%=h(vm.contextPath + vm.institution.themeCss)%>?v=<%=h(vm.assetVersion)%>"><% } %>
    <link rel="stylesheet" href="<%=h(vm.contextPath)%>/css/baru/home-v3.css?v=<%=h(vm.assetVersion)%>">
    <script type="application/ld+json"><%=vm.seo.jsonLd%></script>
</head>
<body class="home-v3">
    <a class="home-skip-link" href="#konten-utama">Lewati ke konten utama</a>
    <%@ include file="home/_header.jsp" %>
    <main id="konten-utama">
        <%@ include file="home/_hero.jsp" %>
        <%@ include file="home/_quick_services.jsp" %>
        <%@ include file="home/_programs.jsp" %>
        <%@ include file="home/_admissions.jsp" %>
        <%@ include file="home/_news_agenda.jsp" %>
        <%@ include file="home/_impact.jsp" %>
    </main>
    <%@ include file="home/_footer.jsp" %>
    <script src="<%=h(vm.contextPath)%>/js/baru/home-v3.js?v=<%=h(vm.assetVersion)%>" defer></script>
    <jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp" />
</body>
</html>
