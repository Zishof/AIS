<%@page import="java.util.Calendar"%>
<%@page import="ais.common.home.HomePortalViewModel"%>
<%@page import="ais.common.home.WebsitePageViewModel"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
private String esc(String value) {
    if (value == null) return "";
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
}
private String text(String value, String fallback) { return value == null || value.trim().length() == 0 ? fallback : value.trim(); }
private String initials(String name) {
    if (name == null || name.trim().length() == 0) return "AI";
    String[] words = name.trim().split("\\s+"); StringBuilder out = new StringBuilder();
    for (int i = 0; i < words.length && out.length() < 2; i++) if (words[i].length() > 0) out.append(Character.toUpperCase(words[i].charAt(0)));
    return out.toString();
}
%>
<%
WebsitePageViewModel pageVm = (WebsitePageViewModel) request.getAttribute("websitePage");
HomePortalViewModel vm = pageVm == null ? null : pageVm.portal;
if (vm == null) { response.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
String root = request.getContextPath(), home = request.getContextPath() + "/web";
String primary = text(vm.institution.themePrimary, "#163d78");
String primaryDark = text(vm.institution.themePrimaryDark, "#0d2855");
String version = esc(text(vm.assetVersion, "4.0.0"));
String nonce = esc(String.valueOf(request.getAttribute("websiteCspNonce")));
%>
<!doctype html>
<html lang="<%=esc(text(vm.language, "id"))%>" dir="<%=esc(text(vm.direction, "ltr"))%>">
<head>
    <meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%=esc(vm.seo.title)%></title><meta name="description" content="<%=esc(vm.seo.description)%>">
    <% if (pageVm.notFound) { %><meta name="robots" content="noindex"><% } %>
    <link rel="canonical" href="<%=esc(vm.seo.canonical)%>">
    <meta property="og:type" content="article"><meta property="og:site_name" content="<%=esc(vm.institution.name)%>">
    <meta property="og:title" content="<%=esc(vm.seo.title)%>"><meta property="og:description" content="<%=esc(vm.seo.description)%>">
    <meta property="og:url" content="<%=esc(vm.seo.canonical)%>"><meta property="og:image" content="<%=esc(vm.seo.image)%>">
    <meta name="twitter:card" content="summary_large_image"><meta name="twitter:title" content="<%=esc(vm.seo.title)%>"><meta name="twitter:description" content="<%=esc(vm.seo.description)%>"><meta name="twitter:image" content="<%=esc(vm.seo.image)%>">
    <link rel="alternate" hreflang="<%=esc(text(vm.language, "id"))%>" href="<%=esc(vm.seo.canonical)%>"><link rel="alternate" hreflang="x-default" href="<%=esc(vm.seo.canonical)%>">
    <link rel="alternate" type="application/rss+xml" title="Berita <%=esc(vm.institution.name)%>" href="<%=root%>/website-feed.xml"><link rel="manifest" href="<%=root%>/website.webmanifest">
    <meta name="theme-color" content="<%=esc(primary)%>"><link rel="icon" href="<%=esc(vm.institution.logoUrl)%>">
    <link rel="stylesheet" href="<%=root%>/css/baru/website-v4.css?v=<%=version%>">
    <style nonce="<%=nonce%>">:root{--brand:<%=esc(primary)%>;--brand-dark:<%=esc(primaryDark)%>}</style>
    <% if (vm.institution.themeCss != null && vm.institution.themeCss.length() > 0) { %><link rel="stylesheet" href="<%=root%><%=esc(vm.institution.themeCss)%>"><% } %>
    <script type="application/ld+json" nonce="<%=nonce%>"><%=pageVm.jsonLd%></script>
</head>
<body class="website-page">
<a class="skip-link" href="#konten-utama">Lewati ke konten utama</a>
<header class="site-header" data-site-header><div class="wrap header-row">
    <a class="brand" href="<%=home%>" aria-label="Beranda <%=esc(vm.institution.name)%>"><span class="brand-logo"><img src="<%=esc(vm.institution.logoUrl)%>" alt="Logo <%=esc(vm.institution.name)%>" width="52" height="52"><span class="brand-fallback" aria-hidden="true"><%=esc(initials(vm.institution.name))%></span></span><span class="brand-copy"><strong><%=esc(vm.institution.name)%></strong><small>Website resmi institusi</small></span></a>
    <nav class="desktop-nav" aria-label="Navigasi utama"><a href="<%=home%>/profil">Tentang</a><a href="<%=home%>/program"><%=esc(vm.terminology.programLabel)%></a><a href="<%=home%>/penerimaan">Penerimaan</a><a href="<%=home%>/berita">Berita</a><a href="<%=home%>/layanan">Layanan</a></nav>
    <div class="header-actions"><a class="button button--quiet desktop-action" href="<%=home%>/cari">Cari</a><a class="button button--primary desktop-action" href="<%=esc(vm.loginUrl)%>">Masuk Portal</a><button class="menu-button" type="button" data-menu-open aria-controls="mobile-menu" aria-expanded="false"><span>Menu</span><span aria-hidden="true">☰</span></button></div>
</div></header>
<div class="menu-overlay" data-menu-overlay hidden></div>
<aside class="mobile-menu" id="mobile-menu" data-mobile-menu aria-hidden="true" aria-label="Menu seluler"><div class="mobile-menu__head"><strong>Menu</strong><button type="button" data-menu-close aria-label="Tutup menu">×</button></div><nav><a href="<%=home%>">Beranda</a><a href="<%=home%>/profil">Tentang</a><a href="<%=home%>/program"><%=esc(vm.terminology.programLabel)%></a><a href="<%=home%>/penerimaan">Penerimaan</a><a href="<%=home%>/berita">Berita</a><a href="<%=home%>/agenda">Agenda</a><a href="<%=home%>/dokumen">Dokumen</a><a href="<%=home%>/cari">Pencarian</a></nav></aside>
<main id="konten-utama">
    <div class="page-hero"><div class="wrap">
        <nav class="breadcrumbs" aria-label="Breadcrumb"><ol><% for (WebsitePageViewModel.Crumb crumb : pageVm.breadcrumbs) { %><li><% if (crumb.url != null) { %><a href="<%=esc(crumb.url)%>"><%=esc(crumb.label)%></a><% } else { %><span aria-current="page"><%=esc(crumb.label)%></span><% } %></li><% } %></ol></nav>
        <span class="eyebrow"><%=esc(pageVm.eyebrow)%></span><h1><%=esc(pageVm.title)%></h1><p><%=esc(pageVm.description)%></p>
        <% if (pageVm.updatedLabel != null && pageVm.updatedLabel.length() > 0) { %><small class="page-updated">Diterbitkan <time><%=esc(pageVm.updatedLabel)%></time></small><% } %>
    </div></div>
    <% if (pageVm.searchPage) { %><section class="section section--soft"><div class="wrap"><form class="site-search" action="<%=home%>/cari" method="get" role="search"><label for="site-q">Cari program, berita, atau layanan</label><div><input id="site-q" name="q" type="search" value="<%=esc(pageVm.searchQuery)%>" minlength="2" autocomplete="off"><button class="button button--primary" type="submit">Cari</button></div></form></div></section><% } %>
    <% if (pageVm.body != null && pageVm.body.length() > 0) { %><article class="section"><div class="wrap prose"><p><%=esc(pageVm.body)%></p></div></article><% } %>
    <% if (!pageVm.sections.isEmpty()) { %><section class="section"><div class="wrap content-sections"><% for (WebsitePageViewModel.Section item : pageVm.sections) { %><article><h2><%=esc(item.title)%></h2><p><%=esc(item.body)%></p></article><% } %></div></section><% } %>
    <% if (!pageVm.cards.isEmpty()) { %><section class="section section--soft"><div class="wrap"><div class="content-card-grid"><% for (WebsitePageViewModel.Card item : pageVm.cards) { %><article class="content-card"><% if (item.eyebrow != null && item.eyebrow.length() > 0) { %><span class="kicker"><%=esc(item.eyebrow)%></span><% } %><h2><%=esc(item.title)%></h2><% if (item.summary != null && item.summary.length() > 0) { %><p><%=esc(item.summary)%></p><% } %><% if (item.meta != null && item.meta.length() > 0) { %><small><%=esc(item.meta)%></small><% } %><% if (item.url != null && item.url.length() > 0) { %><a class="text-link" href="<%=esc(item.url)%>">Lihat informasi <span aria-hidden="true">→</span></a><% } %></article><% } %></div></div></section><% } %>
    <section class="section"><div class="wrap contact-panel"><div><span class="kicker">Perlu bantuan?</span><h2>Hubungi kanal resmi</h2><p>Jika informasi belum tersedia atau perlu format aksesibel, hubungi institusi.</p></div><address><% if (vm.institution.phone != null && vm.institution.phone.length() > 0) { %><div><small>Telepon</small><a href="tel:<%=esc(vm.institution.phone.replace(" ", ""))%>"><%=esc(vm.institution.phone)%></a></div><% } %><% if (vm.institution.email != null && vm.institution.email.length() > 0) { %><div><small>Email</small><a href="mailto:<%=esc(vm.institution.email)%>"><%=esc(vm.institution.email)%></a></div><% } %></address></div></section>
</main>
<footer class="site-footer"><div class="wrap footer-grid"><div class="footer-brand"><div class="brand brand--footer"><span class="brand-logo"><img src="<%=esc(vm.institution.logoUrl)%>" alt="" width="52" height="52"></span><span class="brand-copy"><strong><%=esc(vm.institution.name)%></strong><small>Website resmi institusi</small></span></div></div><div><h2>Tentang</h2><a href="<%=home%>/profil">Profil</a><a href="<%=home%>/akreditasi">Akreditasi</a><a href="<%=home%>/kontak">Kontak</a></div><div><h2>Informasi</h2><a href="<%=home%>/program"><%=esc(vm.terminology.programLabel)%></a><a href="<%=home%>/berita">Berita</a><a href="<%=home%>/agenda">Agenda</a><a href="<%=home%>/dokumen">Dokumen</a></div><div><h2>Kebijakan</h2><a href="<%=home%>/privasi">Privasi</a><a href="<%=home%>/aksesibilitas">Aksesibilitas</a><a href="<%=home%>/ppid">Informasi publik</a></div></div><div class="wrap footer-bottom"><span>© <%=Calendar.getInstance().get(Calendar.YEAR)%> <%=esc(vm.institution.name)%>.</span><a href="#konten-utama">Kembali ke atas ↑</a></div></footer>
<script src="<%=root%>/js/baru/website-v4.js?v=<%=version%>" defer></script>
</body></html>
