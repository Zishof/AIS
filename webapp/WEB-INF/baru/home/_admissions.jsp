<% if (vm.showAdmission && vm.admission != null) { %>
<section class="home-section home-admission-section" id="penerimaan" aria-labelledby="admission-title">
    <div class="home-shell home-admission-grid">
        <article class="home-admission-card">
            <p class="home-kicker">Penerimaan dibuka</p><h2 id="admission-title">Mulai perjalanan pendidikan Anda bersama <%=h(vm.institution.shortName)%></h2>
            <p><%=h(vm.admission.description)%></p><% if (vm.admission.period.length() > 0) { %><strong class="home-admission-period"><%=h(vm.admission.period)%><% if (vm.admission.endDate.length() > 0) { %> &middot; sampai <%=h(vm.admission.endDate)%><% } %></strong><% } %>
            <a class="home-btn home-btn-primary" href="<%=h(vm.admission.url)%>" target="<%=h(vm.admission.target)%>" rel="<%=h(vm.admission.rel)%>"><%=h(vm.admission.label)%></a>
        </article>
        <article class="home-admission-steps"><p class="home-kicker">Alur pendaftaran</p><h3>Empat langkah sederhana</h3><ol><li><span>1</span><div><strong>Buat akun pendaftar</strong><small>Gunakan email dan nomor aktif.</small></div></li><li><span>2</span><div><strong>Lengkapi formulir</strong><small>Isi biodata dan unggah dokumen.</small></div></li><li><span>3</span><div><strong>Ikuti proses seleksi</strong><small>Pantau jadwal dan hasil melalui portal.</small></div></li><li><span>4</span><div><strong>Registrasi ulang</strong><small>Selesaikan administrasi dan aktivasi akun.</small></div></li></ol></article>
    </div>
</section>
<% } %>
