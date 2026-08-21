<% if (vm.showPrograms) { %>
<section class="home-section home-program-section" id="program" aria-labelledby="program-title">
    <div class="home-shell">
        <div class="home-section-heading"><div><p class="home-kicker">Akademik</p><h2 id="program-title"><%=h(vm.terminology.programLabel)%> untuk masa depan</h2><p>Daftar berikut berasal dari data aktif institusi.</p></div></div>
        <div class="home-program-grid">
            <% int programIndex = 0; for (HomePortalViewModel.ProgramItem item : vm.programs) { programIndex++; %>
            <article class="home-program-card home-program-<%=((programIndex - 1) % 3) + 1%>">
                <span class="home-program-no"><%=programIndex < 10 ? "0" + programIndex : String.valueOf(programIndex)%></span><p><%=h(item.level.length() > 0 ? item.level : vm.terminology.programLabel)%></p>
                <h3><%=h(item.label)%></h3><% if (item.unit.length() > 0) { %><small><%=h(item.unit)%></small><% } %>
                <% if (item.description.length() > 0) { %><div><%=h(item.description)%></div><% } %>
                <% if (item.accreditation.length() > 0) { %><span class="home-program-badge"><%=h(item.accreditation)%></span><% } %>
                <a href="<%=h(item.url)%>" target="<%=h(item.target)%>" rel="<%=h(item.rel)%>">Lihat informasi <span aria-hidden="true">&rarr;</span></a>
            </article>
            <% } %>
        </div>
    </div>
</section>
<% } %>
