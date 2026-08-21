<% if (vm.showAnnouncement) { %>
<aside class="home-announcement" aria-label="Pengumuman penting">
    <div class="home-shell"><strong><%=h(vm.announcementText)%></strong><a href="<%=h(vm.announcementUrl)%>" target="<%=h(vm.announcementTarget)%>" rel="<%=h(vm.announcementRel)%>">Lihat informasi <span aria-hidden="true">→</span></a></div>
</aside>
<% } %>
<header class="home-header" id="beranda">
    <div class="home-shell home-header-inner">
        <a class="home-brand" href="#beranda" aria-label="Beranda <%=h(vm.institution.name)%>">
            <img src="<%=h(vm.institution.logoUrl)%>" width="54" height="54" alt="Logo <%=h(vm.institution.name)%>">
            <span><strong><%=h(vm.institution.shortName)%></strong><small>Website resmi dan layanan digital terpadu</small></span>
        </a>
        <nav class="home-nav" aria-label="Navigasi utama">
            <a href="#profil">Profil</a>
            <% if (vm.showPrograms) { %><a href="#program"><%=h(vm.terminology.programLabel)%></a><% } %>
            <% if (vm.showAdmission) { %><a href="#penerimaan">Penerimaan</a><% } %>
            <% if (vm.showImpact) { %><a href="#dampak">Riset &amp; Dampak</a><% } %>
            <% if (vm.showNews || vm.showAgenda) { %><a href="#informasi">Berita &amp; Agenda</a><% } %>
            <a href="#layanan">Layanan Digital</a>
        </nav>
        <div class="home-header-actions">
            <a class="home-btn home-btn-login" href="<%=h(vm.loginUrl)%>" target="<%=h(vm.loginTarget)%>" rel="<%=h(vm.loginRel)%>">Masuk Sistem</a>
            <button class="home-menu-button" type="button" aria-expanded="false" aria-controls="home-mobile-nav" aria-label="Buka menu"><span></span><span></span><span></span></button>
        </div>
    </div>
    <div class="home-mobile-nav" id="home-mobile-nav" hidden>
        <nav class="home-shell" aria-label="Navigasi seluler">
            <a href="#profil">Profil</a><% if (vm.showPrograms) { %><a href="#program"><%=h(vm.terminology.programLabel)%></a><% } %><% if (vm.showAdmission) { %><a href="#penerimaan">Penerimaan</a><% } %><% if (vm.showNews || vm.showAgenda) { %><a href="#informasi">Berita &amp; Agenda</a><% } %><a href="#layanan">Layanan Digital</a><a href="#kontak">Kontak</a>
        </nav>
    </div>
</header>
