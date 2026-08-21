<% if (vm.showNews || vm.showAgenda) { %>
<section class="home-section home-info-section" id="informasi" aria-labelledby="information-title">
    <div class="home-shell"><div class="home-section-heading"><div><p class="home-kicker">Informasi institusi</p><h2 id="information-title">Berita, agenda, dan pengumuman terbaru</h2><p>Informasi resmi yang dipublikasikan oleh institusi.</p></div></div>
        <div class="home-info-grid">
            <% if (vm.showNews) { %><div class="home-news-grid"><% for (HomePortalViewModel.NewsItem item : vm.news) { %><article class="home-news-card"><p><%=h(item.category)%></p><h3><%=h(item.label)%></h3><div><%=h(item.summary)%></div><small><%=h(item.date)%></small><a href="<%=h(item.url)%>" target="<%=h(item.target)%>" rel="<%=h(item.rel)%>">Baca informasi <span aria-hidden="true">&rarr;</span></a></article><% } %></div><% } %>
            <% if (vm.showAgenda) { %><aside class="home-agenda-list" aria-label="Agenda"><% for (HomePortalViewModel.AgendaItem item : vm.agenda) { %><a href="<%=h(item.url)%>" target="<%=h(item.target)%>" rel="<%=h(item.rel)%>"><time datetime="<%=h(item.date)%>"><strong><%=h(item.day)%></strong><small><%=h(item.month)%></small></time><span><strong><%=h(item.label)%></strong><small><%=h(item.description)%></small></span></a><% } %></aside><% } %>
        </div>
    </div>
</section>
<% } %>
