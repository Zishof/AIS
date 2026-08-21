<section class="home-section home-services-section" id="layanan" aria-labelledby="services-title">
    <div class="home-shell">
        <div class="home-section-heading"><div><p class="home-kicker">Akses cepat</p><h2 id="services-title">Semua layanan dalam satu portal</h2><p>Layanan mengikuti tipe institusi dan konfigurasi modul aktif.</p></div></div>
        <% if (!vm.services.isEmpty()) { %>
        <div class="home-service-grid">
            <% for (HomePortalViewModel.ServiceItem item : vm.services) { %>
            <a class="home-service-card" href="<%=h(item.url)%>" target="<%=h(item.target)%>" rel="<%=h(item.rel)%>">
                <span class="home-service-icon"><i class="fas <%=h(item.icon)%>" aria-hidden="true"></i></span><span class="home-card-arrow" aria-hidden="true">&nearr;</span>
                <strong><%=h(item.label)%></strong><small><%=h(item.description)%></small>
            </a>
            <% } %>
        </div>
        <% } else { %><div class="home-empty-state">Belum ada layanan publik yang diaktifkan.</div><% } %>
    </div>
</section>
