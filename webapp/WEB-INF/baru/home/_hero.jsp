<section class="home-hero-wrap" id="profil" aria-labelledby="home-hero-title">
    <div class="home-shell">
        <div class="home-hero">
            <div class="home-hero-copy">
                <p class="home-eyebrow"><span></span><%=h(vm.eyebrow)%></p>
                <h1 id="home-hero-title"><%=h(vm.headline)%></h1>
                <p class="home-lead"><%=h(vm.description)%></p>
                <div class="home-hero-actions">
                    <a class="home-btn home-btn-primary" href="<%=h(vm.primaryUrl)%>" target="<%=h(vm.primaryTarget)%>" rel="<%=h(vm.primaryRel)%>"><%=h(vm.primaryLabel)%></a>
                    <a class="home-btn home-btn-outline" href="<%=h(vm.secondaryUrl)%>" target="<%=h(vm.secondaryTarget)%>" rel="<%=h(vm.secondaryRel)%>"><%=h(vm.secondaryLabel)%></a>
                    <a class="home-btn home-btn-outline" href="#layanan">Portal Layanan</a>
                </div>
                <ul class="home-trust" aria-label="Keunggulan portal"><li>Layanan terintegrasi</li><li>Informasi resmi institusi</li><li>Ramah perangkat</li></ul>
            </div>
            <div class="home-hero-media" aria-hidden="true">
                <img src="<%=h(vm.institution.heroUrl)%>" width="720" height="620" alt="" fetchpriority="high">
                <div class="home-hero-media-overlay"></div>
                <div class="home-float-card"><i class="fas <%=vm.institution.healthcare ? "fa-building" : "fa-university"%>" aria-hidden="true"></i><span><strong><%=h(vm.terminology.institutionLabel)%> Digital</strong><small><%=vm.institution.healthcare ? "Pendaftaran, jadwal, rekam medis" : "Akademik, pustaka, repository"%></small></span></div>
            </div>
            <% if (!vm.statistics.isEmpty()) { %>
            <dl class="home-stats">
                <% for (HomePortalViewModel.StatItem stat : vm.statistics) { %><div><dt><%=h(stat.value)%></dt><dd><%=h(stat.label)%></dd></div><% } %>
            </dl>
            <% } %>
        </div>
    </div>
</section>
