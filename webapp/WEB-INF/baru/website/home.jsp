<%@page import="java.util.Calendar"%>
<%@page import="ais.common.home.HomePortalViewModel"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
private String esc(String value) {
    if (value == null) return "";
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
}
private String text(String value, String fallback) {
    return value == null || value.trim().length() == 0 ? fallback : value.trim();
}
private String attrs(HomePortalViewModel.LinkItem item) {
    if (item == null) return "";
    StringBuilder result = new StringBuilder();
    if (item.target != null && item.target.length() > 0) result.append(" target=\"").append(esc(item.target)).append("\"");
    if (item.rel != null && item.rel.length() > 0) result.append(" rel=\"").append(esc(item.rel)).append("\"");
    return result.toString();
}
private String initials(String name) {
    if (name == null || name.trim().length() == 0) return "AI";
    String[] words = name.trim().split("\\s+");
    StringBuilder value = new StringBuilder();
    for (int i = 0; i < words.length && value.length() < 2; i++) {
        if (words[i].length() > 0) value.append(Character.toUpperCase(words[i].charAt(0)));
    }
    return value.toString();
}
private boolean sectionOnly(String url, String section) {
    return url == null || url.trim().length() == 0 || section.equals(url.trim());
}
%>
<%
HomePortalViewModel vm = (HomePortalViewModel) request.getAttribute("website");
if (vm == null) {
    response.sendError(HttpServletResponse.SC_NOT_FOUND);
    return;
}
String root = request.getContextPath();
String primary = text(vm.institution.themePrimary, "#163d78");
String primaryDark = text(vm.institution.themePrimaryDark, "#0d2855");
String institutionType = vm.institution.healthcare ? "Website Resmi Fasilitas Kesehatan" : "Website Resmi Institusi";
String activeLearner = vm.institution.healthcare ? "Pasien & keluarga" : vm.terminology.learnerPlural + " aktif";
String candidate = vm.institution.healthcare ? "Pasien" : "Calon " + vm.terminology.learnerPlural.toLowerCase();
String parent = vm.institution.healthcare ? "Keluarga pasien" : "Orang tua / wali";
String alumni = vm.institution.healthcare ? "Mitra layanan" : "Alumni";
String impactAudience = vm.institution.healthcare ? "Tenaga kesehatan" : "Mitra industri";
String media = "Media & masyarakat";
String impactAnchor = root + "/web/" + (vm.institution.college ? "riset" : "pembelajaran");
String informationAnchor = root + "/web/berita";
String version = esc(text(vm.assetVersion, "4.0.0"));
%>
<!doctype html>
<html lang="<%=esc(text(vm.language, "id"))%>" dir="<%=esc(text(vm.direction, "ltr"))%>">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%=esc(vm.seo.title)%></title>
    <meta name="description" content="<%=esc(vm.seo.description)%>">
    <link rel="canonical" href="<%=esc(vm.seo.canonical)%>">
    <meta property="og:type" content="website">
    <meta property="og:title" content="<%=esc(vm.seo.title)%>">
    <meta property="og:description" content="<%=esc(vm.seo.description)%>">
    <meta property="og:url" content="<%=esc(vm.seo.canonical)%>">
    <meta property="og:image" content="<%=esc(vm.seo.image)%>">
    <meta property="og:site_name" content="<%=esc(vm.institution.name)%>">
    <meta property="og:locale" content="<%=esc(text(vm.language, "id"))%>_ID">
    <meta name="twitter:card" content="summary_large_image"><meta name="twitter:title" content="<%=esc(vm.seo.title)%>"><meta name="twitter:description" content="<%=esc(vm.seo.description)%>"><meta name="twitter:image" content="<%=esc(vm.seo.image)%>">
    <link rel="alternate" hreflang="<%=esc(text(vm.language, "id"))%>" href="<%=esc(vm.seo.canonical)%>"><link rel="alternate" hreflang="x-default" href="<%=esc(vm.seo.canonical)%>">
    <link rel="alternate" type="application/rss+xml" title="Berita <%=esc(vm.institution.name)%>" href="<%=root%>/website-feed.xml">
    <link rel="manifest" href="<%=root%>/website.webmanifest">
    <meta name="theme-color" content="<%=esc(primary)%>">
    <link rel="icon" href="<%=esc(vm.institution.logoUrl)%>">
    <link rel="stylesheet" href="<%=root%>/css/baru/website-v4.css?v=<%=version%>">
    <style nonce="<%=esc(String.valueOf(request.getAttribute("websiteCspNonce")))%>">:root{--brand:<%=esc(primary)%>;--brand-dark:<%=esc(primaryDark)%>}</style>
    <% if (vm.institution.themeCss != null && vm.institution.themeCss.length() > 0) { %>
    <link rel="stylesheet" href="<%=root%><%=esc(vm.institution.themeCss)%>">
    <% } %>
    <% if (vm.seo.jsonLd != null && vm.seo.jsonLd.trim().length() > 0) { %>
    <script type="application/ld+json" nonce="<%=esc(String.valueOf(request.getAttribute("websiteCspNonce")))%>"><%=vm.seo.jsonLd%></script>
    <% } %>
</head>
<body>
<a class="skip-link" href="#konten-utama">Lewati ke konten utama</a>

<% if (vm.showAnnouncement) { %>
<div class="announcement" role="status">
    <div class="wrap announcement__inner">
        <strong>Informasi terbaru</strong>
        <span><%=esc(vm.announcementText)%></span>
        <a href="<%=esc(vm.announcementUrl)%>" target="<%=esc(vm.announcementTarget)%>" rel="<%=esc(vm.announcementRel)%>">Lihat informasi <span aria-hidden="true">→</span></a>
    </div>
</div>
<% } %>

<header class="site-header" data-site-header>
    <div class="wrap header-row">
        <a class="brand" href="<%=root%>/web" aria-label="Beranda <%=esc(vm.institution.name)%>">
            <span class="brand-logo">
                <img src="<%=esc(vm.institution.logoUrl)%>" alt="Logo <%=esc(vm.institution.name)%>" width="52" height="52">
                <span class="brand-fallback" aria-hidden="true"><%=esc(initials(vm.institution.name))%></span>
            </span>
            <span class="brand-copy"><strong><%=esc(vm.institution.name)%></strong><small><%=esc(institutionType)%></small></span>
        </a>
        <nav class="desktop-nav" aria-label="Navigasi utama">
            <a href="<%=root%>/web/profil">Tentang</a>
            <% if (vm.showPrograms) { %><a href="<%=root%>/web/program"><%=esc(vm.terminology.programLabel)%></a><% } %>
            <a href="<%=root%>/web/penerimaan">Penerimaan</a>
            <% if (vm.showImpact) { %><a href="<%=root%>/web/<%=vm.institution.college ? "riset" : "pembelajaran"%>"><%=vm.institution.college ? "Riset & Dampak" : "Pembelajaran"%></a><% } %>
            <a href="<%=root%>/web/berita">Berita &amp; Agenda</a>
            <a href="<%=root%>/web/layanan">Layanan</a>
        </nav>
        <div class="header-actions">
            <% if (vm.showSearch) { %><a class="button button--quiet desktop-action" href="<%=root%>/web/cari">Cari</a><% } %>
            <a class="button button--quiet desktop-action" href="<%=esc(vm.loginUrl)%>" target="<%=esc(vm.loginTarget)%>" rel="<%=esc(vm.loginRel)%>">Masuk Portal</a>
            <a class="button button--primary desktop-action" href="<%=esc(vm.primaryUrl)%>" target="<%=esc(vm.primaryTarget)%>" rel="<%=esc(vm.primaryRel)%>"><%=esc(vm.primaryLabel)%></a>
            <button class="menu-button" type="button" data-menu-open aria-controls="mobile-menu" aria-expanded="false"><span>Menu</span><span aria-hidden="true">☰</span></button>
        </div>
    </div>
</header>

<div class="menu-overlay" data-menu-overlay hidden></div>
<aside class="mobile-menu" id="mobile-menu" data-mobile-menu aria-hidden="true" aria-label="Menu seluler">
    <div class="mobile-menu__head"><strong>Menu</strong><button type="button" data-menu-close aria-label="Tutup menu">×</button></div>
    <nav>
        <a href="<%=root%>/web/profil">Tentang</a>
        <% if (vm.showPrograms) { %><a href="<%=root%>/web/program"><%=esc(vm.terminology.programLabel)%></a><% } %>
        <a href="<%=root%>/web/penerimaan">Penerimaan</a>
        <% if (vm.showImpact) { %><a href="<%=root%>/web/<%=vm.institution.college ? "riset" : "pembelajaran"%>"><%=vm.institution.college ? "Riset & Dampak" : "Pembelajaran"%></a><% } %>
        <a href="<%=root%>/web/berita">Berita</a><a href="<%=root%>/web/agenda">Agenda</a><a href="<%=root%>/web/layanan">Layanan</a><a href="<%=root%>/web/kontak">Kontak</a><% if (vm.showSearch) { %><a href="<%=root%>/web/cari">Pencarian</a><% } %>
    </nav>
    <div class="mobile-menu__actions">
        <a class="button button--primary" href="<%=esc(vm.primaryUrl)%>" target="<%=esc(vm.primaryTarget)%>" rel="<%=esc(vm.primaryRel)%>"><%=esc(vm.primaryLabel)%></a>
        <a class="button" href="<%=esc(vm.loginUrl)%>" target="<%=esc(vm.loginTarget)%>" rel="<%=esc(vm.loginRel)%>">Masuk Portal</a>
    </div>
</aside>

<main id="konten-utama">
    <% if (!vm.contentWarnings.isEmpty()) { %><div class="service-status" role="status"><div class="wrap"><strong>Sebagian informasi belum dapat dimuat.</strong><span> Silakan coba kembali atau hubungi institusi jika informasi yang Anda perlukan belum tampil.</span></div></div><% } %>
    <section class="hero" id="beranda">
        <div class="hero-shape hero-shape--one"></div><div class="hero-shape hero-shape--two"></div>
        <div class="wrap hero-grid">
            <div class="hero-copy">
                <span class="eyebrow"><%=esc(institutionType)%></span>
                <h1><%=esc(vm.headline)%></h1>
                <p><%=esc(vm.description)%></p>
                <div class="hero-actions">
                    <a class="button button--accent" href="<%=esc(vm.primaryUrl)%>" target="<%=esc(vm.primaryTarget)%>" rel="<%=esc(vm.primaryRel)%>"><%=esc(vm.primaryLabel)%></a>
                    <% if (vm.showPrograms) { %><a class="button button--glass" href="#program"><%=esc(vm.secondaryLabel)%></a><% } %>
                    <a class="button button--glass" href="<%=esc(vm.loginUrl)%>" target="<%=esc(vm.loginTarget)%>" rel="<%=esc(vm.loginRel)%>">Masuk Portal</a>
                </div>
                <div class="trust-list" aria-label="Keunggulan website">
                    <span><i aria-hidden="true">✓</i> Informasi resmi</span>
                    <span><i aria-hidden="true">✓</i> Layanan terpadu</span>
                    <span><i aria-hidden="true">✓</i> Responsif &amp; aksesibel</span>
                </div>
            </div>
        </div>
    </section>

    <% if (!vm.statistics.isEmpty()) { %>
    <section class="stat-band" aria-label="Statistik institusi"><div class="wrap stats">
        <% for (HomePortalViewModel.StatItem item : vm.statistics) { %><div class="stat"><strong><%=esc(item.value)%></strong><span><%=esc(item.label)%></span></div><% } %>
    </div></section>
    <% } %>

    <section class="section section--soft" id="tentang">
        <div class="wrap"><div class="section-heading"><div><span class="kicker">Akses berdasarkan kebutuhan</span><h2>Temukan jalur informasi yang paling relevan</h2><p>Pilih kebutuhan Anda untuk menuju informasi dan layanan yang tepat tanpa menelusuri menu panjang.</p></div><a class="text-link" href="#layanan">Semua layanan <span aria-hidden="true">→</span></a></div>
        <div class="persona-grid">
            <a class="persona" href="<%=root%>/web/penerimaan"><span>01</span><strong><%=esc(candidate)%></strong><small>Program, biaya, bantuan, dan pendaftaran.</small></a>
            <a class="persona" href="<%=esc(vm.loginUrl)%>" target="<%=esc(vm.loginTarget)%>" rel="<%=esc(vm.loginRel)%>"><span>02</span><strong><%=esc(activeLearner)%></strong><small>Portal, informasi, dan layanan digital.</small></a>
            <a class="persona" href="<%=root%>/web/<%=vm.institution.college || vm.institution.healthcare ? "kontak" : "orang-tua"%>"><span>03</span><strong><%=esc(parent)%></strong><small>Informasi resmi dan kanal komunikasi.</small></a>
            <a class="persona" href="#layanan"><span>04</span><strong><%=esc(alumni)%></strong><small>Jejaring, karier, dan layanan terkait.</small></a>
            <a class="persona" href="<%=impactAnchor%>"><span>05</span><strong><%=esc(impactAudience)%></strong><small>Kolaborasi, mutu, riset, dan dampak.</small></a>
            <a class="persona" href="<%=informationAnchor%>"><span>06</span><strong><%=esc(media)%></strong><small>Berita, agenda, dokumen, dan kontak.</small></a>
        </div></div>
    </section>

    <% if (vm.showPrograms) { %>
    <section class="section" id="program"><div class="wrap">
        <div class="section-heading"><div><span class="kicker"><%=esc(vm.terminology.programLabel)%></span><h2>Temukan program sesuai tujuan Anda</h2><p>Program aktif ditampilkan dari data institusi, ringkas, dan mudah dibandingkan.</p></div><a class="text-link" href="<%=root%>/web/program">Lihat semua <span aria-hidden="true">→</span></a></div>
        <div class="program-grid">
        <% for (HomePortalViewModel.ProgramItem item : vm.programs) { %>
            <article class="program-card">
                <div class="chips"><% if (item.level != null && item.level.length() > 0) { %><span><%=esc(item.level)%></span><% } %><% if (item.accreditation != null && item.accreditation.length() > 0) { %><span>Akreditasi <%=esc(item.accreditation)%></span><% } %></div>
                <h3><%=esc(item.label)%></h3><% if (item.unit != null && item.unit.length() > 0) { %><small class="unit"><%=esc(item.unit)%></small><% } %>
                <% if (item.description != null && item.description.length() > 0) { %><p><%=esc(item.description)%></p><% } %>
                <div class="card-actions"><% if (!sectionOnly(item.url, "#program")) { %><a class="button" href="<%=esc(item.url)%>"<%=attrs(item)%>>Pelajari program</a><% } %><a class="button button--primary" href="<%=esc(vm.primaryUrl)%>" target="<%=esc(vm.primaryTarget)%>" rel="<%=esc(vm.primaryRel)%>">Daftar</a></div>
            </article>
        <% } %>
        </div>
    </div></section>
    <% } %>

    <% if (vm.showAdmission) { %>
    <section class="section section--soft" id="penerimaan"><div class="wrap admission-panel">
        <div><span class="eyebrow">Penerimaan</span><h2>Satu alur yang jelas dari minat hingga registrasi</h2><p><%=esc(text(vm.admission.description, "Informasi jalur, jadwal, persyaratan, dan status pendaftaran tersedia melalui layanan penerimaan resmi."))%></p><a class="button button--accent" href="<%=esc(vm.admission.url)%>"<%=attrs(vm.admission)%>><%=esc(text(vm.admission.label, vm.primaryLabel))%></a></div>
        <ol class="steps"><li><span>1</span><div><strong>Pilih program dan jalur</strong><small>Lihat pilihan dan persyaratan yang tersedia.</small></div></li><li><span>2</span><div><strong>Lengkapi data</strong><small>Isi data dan dokumen melalui akun pendaftaran.</small></div></li><li><span>3</span><div><strong>Ikuti seleksi</strong><small>Pantau jadwal dan hasil melalui portal resmi.</small></div></li><li><span>4</span><div><strong>Registrasi</strong><small>Selesaikan proses untuk memulai layanan pendidikan.</small></div></li></ol>
    </div></section>
    <% } %>

    <% if (vm.showNews || vm.showAgenda) { %>
    <section class="section" id="informasi"><div class="wrap">
        <div class="section-heading"><div><span class="kicker">Berita &amp; agenda</span><h2>Informasi aktual dari institusi</h2><p>Publikasi dan kegiatan resmi ditampilkan secara ringkas agar mudah dipahami.</p></div><a class="text-link" href="<%=root%>/web/berita">Lihat semua <span aria-hidden="true">→</span></a></div>
        <div class="news-grid <%=vm.showNews && vm.showAgenda ? "" : "news-grid--single"%>">
            <% if (vm.showNews) { %><div class="news-list">
                <% for (int i = 0; i < vm.news.size(); i++) { HomePortalViewModel.NewsItem item = vm.news.get(i); %>
                <article class="news-card <%=i == 0 ? "news-card--featured" : ""%>">
                    <% if (item.imageUrl != null && item.imageUrl.length() > 0) { %><img src="<%=esc(item.imageUrl)%>" alt="" loading="lazy"><% } %>
                    <div><span class="kicker"><%=esc(text(item.category, "Informasi"))%> <% if (item.date != null && item.date.length() > 0) { %>• <%=esc(item.date)%><% } %></span><h3><%=esc(item.label)%></h3><% if (item.summary != null && item.summary.length() > 0) { %><p><%=esc(item.summary)%></p><% } %><% if (!sectionOnly(item.url, "#informasi")) { %><a class="text-link" href="<%=esc(item.url)%>"<%=attrs(item)%>>Baca selengkapnya <span aria-hidden="true">→</span></a><% } %></div>
                </article><% } %>
            </div><% } %>
            <% if (vm.showAgenda) { %><aside class="agenda-panel"><span class="kicker">Agenda mendatang</span><h3>Jadwal kegiatan</h3>
                <% for (HomePortalViewModel.AgendaItem item : vm.agenda) { %><% if (sectionOnly(item.url, "#informasi")) { %><div class="agenda-item"><span class="agenda-date"><strong><%=esc(item.day)%></strong><small><%=esc(item.month)%></small></span><span><strong><%=esc(item.label)%></strong><small><%=esc(text(item.description, item.date))%></small></span></div><% } else { %><a class="agenda-item" href="<%=esc(item.url)%>"<%=attrs(item)%>><span class="agenda-date"><strong><%=esc(item.day)%></strong><small><%=esc(item.month)%></small></span><span><strong><%=esc(item.label)%></strong><small><%=esc(text(item.description, item.date))%></small></span></a><% } %><% } %>
            </aside><% } %>
        </div>
    </div></section>
    <% } %>

    <% if (vm.showImpact) { %>
    <section class="section section--soft" id="dampak"><div class="wrap"><div class="section-heading"><div><span class="kicker"><%=vm.institution.college ? "Riset, mutu &amp; dampak" : "Pembelajaran, prestasi &amp; dampak"%></span><h2>Bukti kontribusi yang dapat ditelusuri</h2><p>Setiap kartu mengarah pada layanan atau sumber informasi institusi yang nyata.</p></div></div><div class="impact-grid">
        <% for (HomePortalViewModel.ImpactItem item : vm.impacts) { %><a class="impact-card" href="<%=esc(item.url)%>"<%=attrs(item)%>><span aria-hidden="true">↗</span><h3><%=esc(item.label)%></h3><p><%=esc(item.description)%></p></a><% } %>
    </div></div></section>
    <% } %>

    <section class="section" id="layanan"><div class="wrap"><div class="section-heading"><div><span class="kicker">Ekosistem digital AIS</span><h2><%=esc(vm.digitalPortalTitle)%></h2><p><%=esc(vm.digitalPortalSummary)%>. Modul yang tampil mengikuti jenis dan konfigurasi institusi.</p></div><a class="button button--primary" href="<%=esc(vm.loginUrl)%>" target="<%=esc(vm.loginTarget)%>" rel="<%=esc(vm.loginRel)%>">Masuk Portal</a></div><div class="service-grid">
        <% for (HomePortalViewModel.ServiceItem item : vm.services) { %><a class="service-card" href="<%=esc(item.url)%>"<%=attrs(item)%>><span class="service-icon" aria-hidden="true"><%=esc(initials(item.label))%></span><span><strong><%=esc(item.label)%></strong><small><%=esc(item.description)%></small></span><i aria-hidden="true">→</i></a><% } %>
    </div>
    <% if ((vm.androidUrl != null && vm.androidUrl.length() > 0) || (vm.iosUrl != null && vm.iosUrl.length() > 0) || (vm.desktopUrl != null && vm.desktopUrl.length() > 0)) { %>
    <div class="app-availability" aria-labelledby="app-availability-title">
        <div><span class="kicker">Aplikasi resmi</span><h3 id="app-availability-title">Tersedia juga untuk perangkat Anda</h3><p>Gunakan aplikasi resmi melalui Google Play, App Store, atau versi Desktop.</p></div>
        <div class="app-links">
            <% if (vm.androidUrl != null && vm.androidUrl.length() > 0) { %><a class="app-link" href="<%=esc(vm.androidUrl)%>" target="_blank" rel="noopener noreferrer"><span aria-hidden="true">▶</span><span><small>Unduh di</small><strong>Google Play</strong><span class="sr-only"> (membuka tab baru)</span></span></a><% } %>
            <% if (vm.iosUrl != null && vm.iosUrl.length() > 0) { %><a class="app-link" href="<%=esc(vm.iosUrl)%>" target="_blank" rel="noopener noreferrer"><span aria-hidden="true">●</span><span><small>Unduh di</small><strong>App Store</strong><span class="sr-only"> (membuka tab baru)</span></span></a><% } %>
            <% if (vm.desktopUrl != null && vm.desktopUrl.length() > 0) { %><a class="app-link" href="<%=esc(vm.desktopUrl)%>" target="_blank" rel="noopener noreferrer"><span aria-hidden="true">▣</span><span><small>Unduh</small><strong>Versi Desktop</strong><span class="sr-only"> (membuka tab baru)</span></span></a><% } %>
        </div>
    </div>
    <% } %>
    </div></section>

    <section class="section section--soft" id="kontak"><div class="wrap contact-panel"><div><span class="kicker">Kontak resmi</span><h2>Terhubung dengan <%=esc(vm.institution.shortName)%></h2><p>Gunakan informasi resmi berikut untuk pertanyaan, kunjungan, dan layanan publik.</p></div><address>
        <% if (vm.institution.address != null && vm.institution.address.length() > 0) { %><div><small>Alamat</small><strong><%=esc(vm.institution.address)%></strong></div><% } %>
        <% if (vm.institution.phone != null && vm.institution.phone.length() > 0) { %><div><small>Telepon</small><a href="tel:<%=esc(vm.institution.phone.replace(" ", ""))%>"><%=esc(vm.institution.phone)%></a></div><% } %>
        <% if (vm.institution.email != null && vm.institution.email.length() > 0) { %><div><small>Email</small><a href="mailto:<%=esc(vm.institution.email)%>"><%=esc(vm.institution.email)%></a></div><% } %>
    </address></div></section>
</main>

<footer class="site-footer"><div class="wrap footer-grid"><div class="footer-brand"><div class="brand brand--footer"><span class="brand-logo"><img src="<%=esc(vm.institution.logoUrl)%>" alt="" width="52" height="52"></span><span class="brand-copy"><strong><%=esc(vm.institution.name)%></strong><small>Informasi institusi dan layanan digital resmi</small></span></div><p><%=esc(text(vm.institution.motto, vm.institution.address))%></p></div><div><h2>Tentang</h2><a href="<%=root%>/web">Beranda</a><a href="<%=root%>/web/profil">Profil</a><a href="<%=root%>/web/akreditasi">Akreditasi</a><a href="<%=root%>/web/kontak">Kontak</a></div><div><h2>Informasi</h2><a href="<%=root%>/web/program"><%=esc(vm.terminology.programLabel)%></a><a href="<%=root%>/web/penerimaan">Penerimaan</a><a href="<%=root%>/web/berita">Berita</a><a href="<%=root%>/web/agenda">Agenda</a><a href="<%=root%>/web/dokumen">Dokumen</a></div><div><h2>Kebijakan</h2><a href="<%=root%>/web/privasi">Privasi</a><a href="<%=root%>/web/aksesibilitas">Aksesibilitas</a><a href="<%=root%>/web/ppid">Informasi publik</a><a href="<%=esc(vm.loginUrl)%>" target="<%=esc(vm.loginTarget)%>" rel="<%=esc(vm.loginRel)%>">Portal digital</a></div></div><div class="wrap footer-bottom"><span>© <%=Calendar.getInstance().get(Calendar.YEAR)%> <%=esc(vm.institution.name)%>. Seluruh hak dilindungi.</span><a href="#konten-utama">Kembali ke atas ↑</a></div></footer>
<script src="<%=root%>/js/baru/website-v4.js?v=<%=version%>" defer></script>
</body>
</html>
