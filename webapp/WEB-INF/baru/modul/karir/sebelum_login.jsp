<%@page import="ais.common.Common"%>
<%@page import="ais.common.KarirConfigUtil"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
    private String sk(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    String rnd = Common.getGeneratedBarCode(7);
    String judul = "Instansi";
    String motto = "Membangun tim terbaik untuk pelayanan terbaik";
    String logo = Common.ROOT + "/img/logo.png";
    String bg = Common.ROOT + "/img/bg1.png";
    try {
        PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
        if (pt != null) { judul = pt.getNama() == null ? judul : pt.getNama(); motto = pt.getMotto() == null ? motto : pt.getMotto(); }
        String mediaLogo = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
        String mediaBg = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "background_perguruanTinggi_");
        if (mediaLogo != null && mediaLogo.trim().length() > 0) logo = mediaLogo;
        if (mediaBg != null && mediaBg.trim().length() > 0) bg = mediaBg;
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/sebelum_login.jsp:25");}
    String teksHeroBadge = KarirConfigUtil.text("karir_hero_badge", "Portal Seleksi Terintegrasi ERP");
    String teksHeroJudul = KarirConfigUtil.text("karir_hero_judul", "Bergabung Bersama Tim Terbaik Kami");
    String teksHeroDeskripsi = KarirConfigUtil.text("karir_hero_deskripsi", "Temukan lowongan sesuai kompetensi Anda, lengkapi data diri, unggah dokumen persyaratan, dan pantau hasil seleksi dalam satu halaman.");
    String teksAlurJudul = KarirConfigUtil.text("karir_alur_judul", "Alur Seleksi Singkat");
    String teksAlurDeskripsi = KarirConfigUtil.text("karir_alur_deskripsi", "Pilih lowongan, kirim data, lalu pantau pengumuman melalui akun calon pegawai.");
    String teksPengumumanDeskripsi = KarirConfigUtil.text("karir_pengumuman_deskripsi", "Informasi resmi untuk pelamar ditampilkan di sini.");
    String teksSyaratDeskripsi = KarirConfigUtil.text("karir_syarat_deskripsi", "Siapkan data diri dan dokumen dengan benar agar proses pemeriksaan lebih cepat.");
%>
<section class="position-relative overflow-hidden karir-hero" style="background:linear-gradient(135deg, rgba(15,23,42,.92), rgba(37,99,235,.82)), url('<%=sk(bg)%>') center/cover no-repeat;">
    <div class="container py-5 py-lg-6 position-relative">
        <div class="row align-items-center g-5 py-4">
            <div class="col-lg-7 text-white animate__animated animate__fadeInLeft">
                <span class="badge rounded-pill px-3 py-2 mb-3 karir-hero-badge">
                    <i class="fas fa-sparkles me-1 text-warning"></i> <%=sk(teksHeroBadge)%>
                </span>
                <h1 class="display-4 fw-black text-white mb-3 karir-hero-title"><%=sk(teksHeroJudul)%></h1>
                <p class="lead text-white-50 mb-4 karir-max-760"><%=sk(teksHeroDeskripsi)%></p>
                <div class="d-flex flex-wrap gap-3">
                    <a href="#lowongan" class="btn btn-warning btn-lg rounded-pill fw-bold px-4 shadow"><i class="fas fa-briefcase me-2"></i>Lihat Lowongan</a>
                    <button class="btn btn-light btn-lg rounded-pill fw-bold px-4" onclick="karirOpenLoginModal()"><i class="fas fa-bullhorn me-2"></i>Cek Pengumuman</button>
                </div>
                <div class="row g-3 mt-4">
                    <div class="col-sm-4"><div class="p-3 rounded-4 karir-hero-mini"><i class="fas fa-layer-group text-warning me-2"></i><strong>Terstruktur</strong><br><small class="text-white-50">alur seleksi jelas</small></div></div>
                    <div class="col-sm-4"><div class="p-3 rounded-4 karir-hero-mini"><i class="fas fa-cloud-upload-alt text-warning me-2"></i><strong>Online</strong><br><small class="text-white-50">upload dokumen</small></div></div>
                    <div class="col-sm-4"><div class="p-3 rounded-4 karir-hero-mini"><i class="fas fa-user-check text-warning me-2"></i><strong>Transparan</strong><br><small class="text-white-50">status dapat dipantau</small></div></div>
                </div>
            </div>
            <div class="col-lg-5 animate__animated animate__fadeInRight">
                <div class="karir-soft-card-<%=rnd%> karir-flow-card-<%=rnd%> p-4">
                    <div class="d-flex align-items-center gap-3 mb-3">
                        <img src="<%=sk(logo)%>" class="karir-logo-md rounded-4 bg-white shadow-sm p-2" alt="Logo">
                        <div>
                            <h5 class="fw-bold mb-0"><%=sk(judul)%></h5>
                            <div class="text-muted small"><%=sk(motto)%></div>
                        </div>
                    </div>
                    <h4 class="fw-bold mb-2"><i class="fas fa-route text-primary me-2"></i><%=sk(teksAlurJudul)%></h4>
                    <div class="karir-flow-note-<%=rnd%> mb-2"><i class="fas fa-circle-info me-2"></i><%=sk(teksAlurDeskripsi)%></div>
                    <div>
                        <div class="karir-step"><div class="karir-step-icon"><i class="fas fa-briefcase"></i></div><div><strong>1. Pilih Lowongan</strong><br><small class="text-muted">Cek periode, kebutuhan, persyaratan, dan tanggung jawab.</small></div></div>
                        <div class="karir-step"><div class="karir-step-icon"><i class="fas fa-upload"></i></div><div><strong>2. Lengkapi Pendaftaran</strong><br><small class="text-muted">Isi biodata dan unggah seluruh dokumen yang diminta.</small></div></div>
                        <div class="karir-step"><div class="karir-step-icon"><i class="fas fa-bullhorn"></i></div><div><strong>3. Pantau Hasil</strong><br><small class="text-muted">Lihat verifikasi, jadwal interview, diterima, atau ditolak.</small></div></div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</section>

<section id="pengumuman-karir" class="container py-5 pb-3">
    <div class="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4">
        <div>
            <span class="badge karir-badge-soft rounded-pill px-3 py-2 mb-2"><i class="fas fa-bullhorn me-1"></i>Pengumuman KARIR</span>
            <h2 class="karir-section-title-<%=rnd%> mb-0">Informasi Resmi Seleksi Pegawai</h2>
            <p class="text-muted mb-0"><%=sk(teksPengumumanDeskripsi)%></p>
        </div>
        <button class="btn btn-outline-primary rounded-pill fw-bold" onclick="karirLoadPengumumanKarir()"><i class="fas fa-sync-alt me-2"></i>Refresh Pengumuman</button>
    </div>
    <div id="karirPengumumanUmumContainer" class="row g-4">
        <div class="col-12 text-center py-4"><div class="spinner-border text-primary"></div><div class="small text-muted mt-2">Memuat pengumuman KARIR...</div></div>
    </div>
</section>

<section id="lowongan" class="container py-5">
    <div class="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4">
        <div>
            <span class="badge karir-badge-soft rounded-pill px-3 py-2 mb-2"><i class="fas fa-briefcase me-1"></i>Info Lowongan Kerja</span>
            <h2 class="karir-section-title-<%=rnd%> mb-0">Pilih Jenis Pekerjaan yang Tersedia</h2>
            <p class="text-muted mb-0">Setiap lowongan dapat dipilih sebagai tujuan pendaftaran calon pegawai.</p>
        </div>
        <div class="d-flex gap-2">
            <input id="karirSearchLowongan" class="form-control rounded-pill karir-search-input" placeholder="Cari lowongan...">
            <button class="btn btn-outline-primary rounded-pill fw-bold" onclick="karirLoadLowongan()"><i class="fas fa-search me-2"></i>Cari</button>
        </div>
    </div>
    <div id="karirLowonganContainer" class="row g-4"><div class="col-12 text-center py-5"><div class="spinner-border text-primary"></div><div class="small text-muted mt-2">Memuat lowongan...</div></div></div>
</section>

<section id="pendaftaran" class="container py-5">
    <div class="row g-4 align-items-start">
        <div class="col-lg-8">
            <div class="karir-soft-card-<%=rnd%> p-4 p-lg-5 h-100">
                <span class="badge karir-badge-soft rounded-pill px-3 py-2 mb-2"><i class="fas fa-user-plus me-1"></i>Menu Pendaftaran</span>
                <h2 class="karir-section-title-<%=rnd%> mb-2">Daftar Melalui Lowongan yang Dipilih</h2>
                <p class="text-muted mb-4">Klik tombol <strong>Daftar Sekarang</strong> pada lowongan yang diminati. Form pendaftaran akan muncul dalam popup dan pilihan <strong>lowongan</strong> otomatis terkunci sesuai posisi yang dipilih.</p>
                <div class="row g-3">
                    <div class="col-md-4"><div class="p-3 rounded-4 bg-white border h-100"><i class="fas fa-lock text-success me-2"></i><strong>Lowongan Terkunci</strong><br><small class="text-muted">Menghindari salah pilih gelombang.</small></div></div>
                    <div class="col-md-4"><div class="p-3 rounded-4 bg-white border h-100"><i class="fas fa-file-arrow-up text-primary me-2"></i><strong>Upload Dokumen</strong><br><small class="text-muted">Identitas, pendidikan, lamaran, CV, dan photo.</small></div></div>
                    <div class="col-md-4"><div class="p-3 rounded-4 bg-white border h-100"><i class="fas fa-envelope-circle-check text-warning me-2"></i><strong>Akses Dikirim</strong><br><small class="text-muted">Username dan password masuk ke email.</small></div></div>
                </div>
            </div>
        </div>
        <div class="col-lg-4">
            <div class="karir-soft-card-<%=rnd%> p-4 mb-4">
                <h5 class="fw-bold"><i class="fas fa-envelope-open-text text-primary me-2"></i>Kirim Ulang Akses</h5>
                <p class="text-muted small">Gunakan fitur ini jika email username/password hilang, terhapus, atau tidak diterima. Masukkan email yang sudah terdaftar sebagai calon pegawai.</p>
                <form id="karirResendForm">
                    <input type="hidden" name="action" value="resend_access">
                    <label class="form-label fw-semibold"><%=Common.getBahasaConfig("Email Terdaftar")%></label>
                    <input name="email" type="email" class="form-control rounded-3 mb-3" required>
                    <button class="btn btn-outline-primary rounded-pill fw-bold w-100"><i class="fas fa-paper-plane me-2"></i>Kirim Ulang Akses</button>
                </form>
            </div>
            <div id="syarat" class="karir-soft-card-<%=rnd%> p-4">
                <h5 class="fw-bold"><i class="fas fa-file-contract text-primary me-2"></i>Syarat dan Ketentuan</h5>
                <ul class="text-muted small lh-lg mb-0">
                    <li>Pelamar memilih satu lowongan yang masih aktif dan sesuai kompetensi.</li>
                    <li>Data diri, email, dan nomor telepon wajib aktif agar informasi seleksi dapat diterima.</li>
                    <li>Dokumen persyaratan wajib jelas, terbaca, dan sesuai kategori dokumen.</li>
                    <li>Tim ERP melakukan verifikasi berkas pada modul Pendataan Calon Pegawai.</li>
                    <li>Status interview, diterima, atau ditolak hanya dapat dilihat setelah login ke Portal KARIR.</li>
                    <li>Keputusan panitia seleksi bersifat final dan mengikuti ketentuan instansi.</li>
                </ul>
            </div>
        </div>
    </div>
</section>

<div class="modal fade karir-modal-form-<%=rnd%>" id="karirRegisterModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content">
            <div class="modal-header p-4">
                <div>
                    <h5 class="modal-title fw-bold mb-1"><i class="fas fa-user-plus me-2 text-warning"></i>Form Pendaftaran Calon Pegawai / Karyawan</h5>
                    <small class="text-white-50">Lowongan sudah dipilih dan terkunci. Lengkapi data diri serta dokumen persyaratan di bawah ini.</small>
                </div>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
            </div>
            <form id="karirRegisterForm" enctype="multipart/form-data">
                <div class="modal-body p-4 p-lg-5">
                    <input type="hidden" name="action" value="register">
                    <input type="hidden" id="karirLowonganId" name="lowongan_id">
                    <div class="karir-selected-lock-<%=rnd%> mb-4" id="karirLowonganTerpilih"><i class="fas fa-circle-info me-2"></i>Silakan pilih lowongan terlebih dahulu dari daftar lowongan kerja.</div>
                    <div id="karirKelompokWrapper" class="d-none mb-4 p-3 rounded-4 karir-white-panel">
                        <label class="form-label fw-semibold"><i class="fas fa-layer-group text-primary me-2"></i>Kelompok / Jalur Pendaftaran</label>
                        <select id="karirKelompokSelect" name="kelompok_id" class="form-select rounded-3"></select>
                        <div id="karirKelompokInfo" class="small text-muted mt-2"></div>
                    </div>
                    <div class="row g-3">
                        <div class="col-md-6"><label class="form-label karir-required fw-semibold"><%=Common.getBahasaConfig("Nama Lengkap")%></label><input name="nama" class="form-control rounded-3" required></div>
                        <div class="col-md-6"><label class="form-label karir-required fw-semibold"><%=Common.getBahasaConfig("Email Aktif")%></label><input name="email" type="email" class="form-control rounded-3" required></div>
                        <div class="col-md-6"><label class="form-label karir-required fw-semibold"><%=Common.getBahasaConfig("Tempat Lahir")%></label><input name="tempat_lahir" class="form-control rounded-3" required></div>
                        <div class="col-md-6"><label class="form-label karir-required fw-semibold"><%=Common.getBahasaConfig("Tanggal Lahir")%></label><input name="tanggal_lahir" type="date" class="form-control rounded-3" required></div>
                        <div class="col-md-6"><label class="form-label karir-required fw-semibold"><%=Common.getBahasaConfig("Jenis Kelamin")%></label><select name="jenis_kelamin" class="form-select rounded-3" required><option value="">Pilih</option><option>Laki-laki</option><option>Perempuan</option></select></div>
                        <div class="col-md-6"><label class="form-label fw-semibold"><%=Common.getBahasaConfig("Agama")%></label><select id="karirAgamaSelect" name="agama" class="form-select rounded-3"><option value="">Memuat...</option></select></div>
                        <div class="col-md-6"><label class="form-label karir-required fw-semibold"><%=Common.getBahasaConfig("Telepon / HP")%></label><input name="telp" class="form-control rounded-3" required></div>
                        <div class="col-12"><label class="form-label karir-required fw-semibold"><%=Common.getBahasaConfig("Alamat Lengkap")%></label><textarea name="alamat" class="form-control rounded-3" rows="3" required></textarea></div>
                    </div>
                    <hr class="my-4">
                    <h5 class="fw-bold mb-3"><i class="fas fa-folder-open text-primary me-2"></i>Upload Dokumen Persyaratan</h5>
                    <div class="row g-3">
                        <div class="col-md-6"><label class="form-label karir-required fw-semibold"><%=Common.getBahasaConfig("Kartu Identitas")%></label><input name="dokumen_kartu_identitas" type="file" class="form-control rounded-3" required></div>
                        <div class="col-md-6"><label class="form-label karir-required fw-semibold"><%=Common.getBahasaConfig("Surat Keterangan Pendidikan")%></label><input name="dokumen_pendidikan" type="file" class="form-control rounded-3" required></div>
                        <div class="col-md-6"><label class="form-label fw-semibold"><%=Common.getBahasaConfig("Surat Keterangan Pengalaman Kerja")%></label><input name="dokumen_pengalaman" type="file" class="form-control rounded-3"></div>
                        <div class="col-md-6"><label class="form-label karir-required fw-semibold"><%=Common.getBahasaConfig("Surat Lamaran")%></label><input name="dokumen_lamaran" type="file" class="form-control rounded-3" required></div>
                        <div class="col-md-6"><label class="form-label karir-required fw-semibold"><%=Common.getBahasaConfig("Curriculum Vitae")%></label><input name="dokumen_cv" type="file" class="form-control rounded-3" required></div>
                        <div class="col-md-6"><label class="form-label karir-required fw-semibold"><%=Common.getBahasaConfig("Pas Photo Terbaru")%></label><input name="dokumen_photo" type="file" class="form-control rounded-3" accept="image/*" required></div>
                    </div>
                    <div class="form-check mt-4">
                        <input class="form-check-input" type="checkbox" id="karirSetuju" required>
                        <label class="form-check-label" for="karirSetuju"><%=Common.getBahasaConfig("Saya menyatakan seluruh data dan dokumen yang dikirimkan benar serta dapat dipertanggungjawabkan.")%></label>
                    </div>
                </div>
                <div class="modal-footer border-0 bg-white p-4 d-flex flex-column flex-md-row gap-2 justify-content-end">
                    <button type="button" class="btn btn-light rounded-pill px-4 py-2 fw-bold" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
                    <button type="submit" class="btn karir-btn-gradient-<%=rnd%> karir-submit-register-btn-<%=rnd%> rounded-pill px-4 py-2 fw-bold"><i class="fas fa-paper-plane me-2"></i>Daftar Calon Pegawai</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
async function karirLoadAgama(){
    try { var res = await fetch(window.karirServiceUrl + '&action=list_agama'); var json = await res.json(); var sel = document.getElementById('karirAgamaSelect'); if(!sel) return; sel.innerHTML = '<option value="">Pilih agama</option>'; (json.data || []).forEach(function(a){ var opt=document.createElement('option'); opt.value=a.nama || ''; opt.textContent=a.nama || ''; sel.appendChild(opt); }); }
    catch(e) { var sel = document.getElementById('karirAgamaSelect'); if(sel) sel.innerHTML = '<option value="">Pilih agama</option>'; }
}
function karirText(v, emptyText){
    var s = String((v === null || v === undefined || String(v).trim() === '') ? (emptyText || '-') : v);
    return karirEscapeHtml(s).replace(/\r\n|\n|\r/g, '<br>');
}
function karirShortText(v, max){
    var s = String((v === null || v === undefined) ? '' : v).replace(/\s+/g, ' ').trim();
    if(!s) return '-';
    if(max && s.length > max) s = s.substring(0, max - 3) + '...';
    return karirEscapeHtml(s);
}
function karirBoolLabel(v){ return (v === true || String(v).toLowerCase() === 'true') ? 'Ya' : 'Tidak'; }
function karirActiveBadge(l){
    var status = l.statusPendaftaran || '';
    if(l.pendaftaranTerlewat === true || String(l.pendaftaranTerlewat).toLowerCase() === 'true') {
        return '<span class="badge bg-warning text-dark rounded-pill px-3 py-2"><i class="fas fa-calendar-xmark me-1"></i>Pendaftaran Terlewat</span>';
    }
    if(l.pendaftaranBelumDibuka === true || String(l.pendaftaranBelumDibuka).toLowerCase() === 'true') {
        return '<span class="badge bg-info text-dark rounded-pill px-3 py-2"><i class="fas fa-hourglass-start me-1"></i>Belum Dibuka</span>';
    }
    var aktif = (l.aktif === true || String(l.aktif).toLowerCase() === 'true');
    return aktif ? '<span class="badge bg-success rounded-pill px-3 py-2"><i class="fas fa-circle-check me-1"></i>'+(status ? karirEscapeHtml(status) : 'Aktif')+'</span>' : '<span class="badge bg-secondary rounded-pill px-3 py-2"><i class="fas fa-circle-xmark me-1"></i>Tidak Aktif</span>';
}
function karirPeriodeNote(l){
    var msg = l.pesanPendaftaran || '';
    if(!msg) return '';
    var cls = 'open'; var icon = 'fa-circle-check';
    if(l.pendaftaranTerlewat === true || String(l.pendaftaranTerlewat).toLowerCase() === 'true') { cls = 'closed'; icon = 'fa-calendar-xmark'; }
    else if(l.pendaftaranBelumDibuka === true || String(l.pendaftaranBelumDibuka).toLowerCase() === 'true') { cls = 'future'; icon = 'fa-hourglass-start'; }
    return '<div class="karir-period-note-<%=rnd%> '+cls+' mb-3"><i class="fas '+icon+' me-2"></i>'+karirEscapeHtml(msg)+'</div>';
}
function karirDaftarButton(l){
    var bisa = (l.dapatDaftar === true || String(l.dapatDaftar).toLowerCase() === 'true');
    if(!bisa) {
        var label = (l.pendaftaranTerlewat === true || String(l.pendaftaranTerlewat).toLowerCase() === 'true') ? 'Pendaftaran Sudah Terlewat' : ((l.pendaftaranBelumDibuka === true || String(l.pendaftaranBelumDibuka).toLowerCase() === 'true') ? 'Pendaftaran Belum Dibuka' : 'Pendaftaran Ditutup');
        return '<button type="button" class="btn karir-apply-btn-<%=rnd%> rounded-pill fw-bold w-100 mt-3" disabled><i class="fas fa-lock me-2"></i>'+label+'</button>';
    }
    return '<button type="button" class="btn karir-apply-btn-<%=rnd%> rounded-pill fw-bold w-100 mt-3 karirPilihLowonganBtn" data-id="'+l.id+'" data-nama="'+karirEscapeHtml(l.nama || 'Lowongan Kerja')+'"><i class="fas fa-user-plus me-2"></i>Daftar Sekarang</button>';
}
function karirInfoPill(icon, label, value){
    return '<div class="karir-job-mini-<%=rnd%>"><div class="karir-job-mini-label"><i class="'+icon+' me-1"></i>'+karirEscapeHtml(label)+'</div><div class="karir-job-mini-value">'+karirText(value, '-')+'</div></div>';
}
function karirLongField(icon, title, value, emptyText){
    return '<div class="col-lg-6"><div class="karir-job-long-<%=rnd%>"><h6><i class="'+icon+' text-primary"></i>'+karirEscapeHtml(title)+'</h6><div class="karir-long-text">'+karirText(value, emptyText || 'Belum ada informasi khusus.')+'</div></div></div>';
}
function karirFormFlag(label, value, icon){
    var ya = (value === true || String(value).toLowerCase() === 'true');
    return '<span class="karir-form-flag-<%=rnd%>"><i class="'+icon+' me-1 '+(ya ? 'text-success' : 'text-muted')+'"></i>'+karirEscapeHtml(label)+': '+(ya ? 'Tampil' : 'Tidak Tampil')+'</span>';
}
async function karirLoadPengumumanKarir(){
    var c = document.getElementById('karirPengumumanUmumContainer');
    if(!c) return;
    c.innerHTML = '<div class="col-12 text-center py-4"><div class="spinner-border text-primary"></div><div class="small text-muted mt-2">Memuat pengumuman KARIR...</div></div>';
    try {
        var res = await fetch(window.karirServiceUrl + '&action=list_pengumuman_karir');
        var json = await res.json();
        if(json.status !== 'success') {
            c.innerHTML = '<div class="col-12"><div class="alert alert-warning rounded-4 border-0"><i class="fas fa-triangle-exclamation me-2"></i>'+karirEscapeHtml(json.message || 'Pengumuman KARIR belum dapat dimuat.')+'</div></div>';
            return;
        }
        var data = json.data || [];
        if(data.length === 0) {
            c.innerHTML = '<div class="col-12"><div class="karir-ann-empty-<%=rnd%> p-4 text-center"><i class="fas fa-circle-info text-primary me-2"></i>Belum ada pengumuman resmi untuk KARIR yang aktif saat ini.</div></div>';
            return;
        }
        var html = '';
        data.forEach(function(p){
            var tanggal = p.periode || p.tanggal || 'Tanggal mengikuti informasi panitia';
            var kategori = p.kategori ? '<span class="badge bg-light text-dark rounded-pill px-3 py-2"><i class="fas fa-tag me-1"></i>'+karirEscapeHtml(p.kategori)+'</span>' : '';
            html += '<div class="col-lg-4 col-md-6"><article class="karir-ann-card-<%=rnd%> p-4">'
                + '<div class="d-flex flex-wrap gap-2 align-items-center mb-3"><span class="karir-ann-date-<%=rnd%>"><i class="far fa-calendar-alt"></i>'+karirEscapeHtml(tanggal)+'</span>'+kategori+'</div>'
                + '<h5 class="fw-bold mb-2 karir-ann-title">'+karirEscapeHtml(p.judul || 'Pengumuman KARIR')+'</h5>'
                + '<div class="karir-ann-text-<%=rnd%>">'+karirText(p.isi, 'Detail pengumuman mengikuti informasi resmi panitia seleksi.')+'</div>'
                + '</article></div>';
        });
        c.innerHTML = html;
    } catch(e) {
        c.innerHTML = '<div class="col-12"><div class="alert alert-danger rounded-4 border-0"><i class="fas fa-triangle-exclamation me-2"></i>Gagal memuat pengumuman KARIR.</div></div>';
    }
}

async function karirLoadLowongan(){
    var c = document.getElementById('karirLowonganContainer');
    var q = document.getElementById('karirSearchLowongan').value || '';
    c.innerHTML = '<div class="col-12 text-center py-5"><div class="spinner-border text-primary"></div><div class="small text-muted mt-2">Memuat lowongan...</div></div>';
    try { var res = await fetch(window.karirServiceUrl + '&action=list_lowongan&q=' + encodeURIComponent(q)); var json = await res.json();
        if(json.status !== 'success') { c.innerHTML = '<div class="col-12"><div class="alert alert-warning rounded-4">'+karirEscapeHtml(json.message || 'Lowongan belum tersedia.')+'</div></div>'; return; }
        var html = ''; var data = json.data || [];
        window.karirLowonganMap = {};
        if(data.length === 0) html = '<div class="col-12"><div class="alert alert-info rounded-4 border-0"><i class="fas fa-circle-info me-2"></i>Belum ada lowongan aktif yang dapat ditampilkan.</div></div>';
        data.forEach(function(l, idx){
            window.karirLowonganMap[String(l.id)] = l;
            var kelompokInfo = (l.kelompok && l.kelompok.length) ? '<span class="badge bg-info-subtle text-info-emphasis rounded-pill px-3 py-2"><i class="fas fa-layer-group me-1"></i>'+l.kelompok.length+' kelompok/jalur</span>' : '<span class="badge bg-light text-dark rounded-pill px-3 py-2"><i class="fas fa-layer-group me-1"></i>Jalur umum</span>';
            var jadwalInfo = l.jadwalSeleksi ? karirEscapeHtml(l.jadwalSeleksi) : 'Menunggu pengumuman panitia';
            html += '<div class="col-12"><article class="karir-soft-card-<%=rnd%> karir-hover-card-<%=rnd%> karir-job-card-<%=rnd%> karirJobCardItem p-0" data-id="'+l.id+'" data-nama="'+karirEscapeHtml(l.nama || 'Lowongan Kerja')+'">'
                + '<div class="p-4 p-lg-5">'
                + '<div class="row g-4 align-items-start">'
                + '<div class="col-lg-8">'
                + '<div class="karir-job-topline-<%=rnd%> mb-3">'+karirActiveBadge(l)+'<span class="badge karir-badge-soft rounded-pill px-3 py-2"><i class="fas fa-briefcase me-1"></i>'+karirEscapeHtml(l.jenis || 'Pegawai Umum dan Tendik')+'</span>'+kelompokInfo+'<span class="badge bg-dark rounded-pill px-3 py-2"><i class="fas fa-users me-1"></i>'+karirEscapeHtml(l.kuota || '-')+' kebutuhan</span></div>'
                + '<h3 class="fw-black mb-2 karir-job-title">'+karirEscapeHtml(l.nama || 'Lowongan Kerja')+'</h3>'
                + '<div class="text-muted mb-3"><i class="far fa-calendar-alt me-1 text-primary"></i><strong>Periode berlaku:</strong> '+karirEscapeHtml(l.periode || 'Periode mengikuti ketentuan panitia')+'</div>'
                + karirPeriodeNote(l)
                + '<div class="d-flex flex-wrap gap-2 mb-3">'
                    + karirFormFlag('Form tambahan saat registrasi', l.tampilFormTambahanSaatRegistrasi, 'fas fa-file-pen')
                    + karirFormFlag('Form tambahan saat login', l.tampilFormTambahanSaatLoginCalonPegawai, 'fas fa-user-gear')
                + '</div>'
                + '<div class="karir-lowongan-detail-<%=rnd%>">'
                    + '<div class="karir-lowongan-detail-title-<%=rnd%>"><div class="title-box"><div class="title-icon"><i class="fas fa-clipboard-check"></i></div><div><h5 class="fw-bold mb-1">Detail Lengkap Lowongan</h5><div class="text-muted small">Persyaratan, tanggung jawab, fasilitas, informasi tambahan, dan disclaimer.</div></div></div></div>'
                    + '<div class="row g-3 mb-3">'
                        + karirLongField('fas fa-list-check', 'Persyaratan', l.persyaratan, 'Belum ada persyaratan khusus.')
                        + karirLongField('fas fa-person-chalkboard', 'Tanggung Jawab', l.tanggungJawab, 'Belum ada uraian tanggung jawab khusus.')
                        + karirLongField('fas fa-gift', 'Fasilitas', l.fasilitas, 'Belum ada informasi fasilitas khusus.')
                        + karirLongField('fas fa-circle-info', 'Informasi Tambahan', l.informasi || l.keterangan, 'Informasi tambahan mengikuti ketentuan panitia seleksi.')
                    + '</div>'
                    + '<div class="karir-disclaimer-<%=rnd%>"><div class="fw-bold mb-1"><i class="fas fa-shield-halved me-2"></i>Disclaimer / Catatan Penting</div><div class="small lh-lg">'+karirText(l.disclaimer, 'Melamar pekerjaan di portal ini tidak dipungut biaya. Hati-hati terhadap penipuan yang mengatasnamakan panitia seleksi.')+'</div></div>'
                + '</div>'
                + '</div>'
                + '<div class="col-lg-4"><div class="karir-job-side-<%=rnd%>">'
                    + '<h5 class="fw-bold mb-3"><i class="fas fa-star text-warning me-2"></i>Ringkasan Posisi</h5>'
                    + '<div class="karir-side-row"><div class="karir-job-side-icon"><i class="fas fa-diagram-project"></i></div><div><small>Fungsi Kerja</small><div class="fw-bold">'+karirShortText(l.fungsiKerja, 110)+'</div></div></div>'
                    + '<div class="karir-side-row"><div class="karir-job-side-icon"><i class="fas fa-graduation-cap"></i></div><div><small>Lulusan & Jurusan</small><div class="fw-bold">'+karirShortText((l.lulusan || '-') + ' / ' + (l.jurusan || '-'), 120)+'</div></div></div>'
                    + '<div class="karir-side-row"><div class="karir-job-side-icon"><i class="fas fa-medal"></i></div><div><small>Pengalaman</small><div class="fw-bold">'+karirShortText(l.pengalaman, 90)+'</div></div></div>'
                    + '<div class="karir-side-row"><div class="karir-job-side-icon"><i class="fas fa-calendar-check"></i></div><div><small>Jadwal Seleksi Terdekat</small><div class="fw-bold">'+jadwalInfo+'</div></div></div>'
                    + karirDaftarButton(l)
                + '</div></div>'
                + '</div></div>'
                + '</article></div>';
        });
        c.innerHTML = html;
        document.querySelectorAll('.karirPilihLowonganBtn').forEach(function(btn){ btn.addEventListener('click', function(e){ if(e){ e.preventDefault(); e.stopPropagation(); } karirPilihLowongan(this.getAttribute('data-id'), this.getAttribute('data-nama')); }); });
        document.querySelectorAll('.karirJobCardItem').forEach(function(card){
            card.removeAttribute('tabindex');
        });
    } catch(e) { c.innerHTML = '<div class="col-12"><div class="alert alert-danger rounded-4">Gagal memuat lowongan.</div></div>'; }
}
function karirPilihLowongan(id, nama){
    document.getElementById('karirLowonganId').value = id;
    var l = (window.karirLowonganMap || {})[String(id)] || {};
    document.querySelectorAll('.karirJobCardItem').forEach(function(card){ card.classList.remove('ring-3'); card.style.outline=''; });
    var selectedCard = document.querySelector('.karirJobCardItem[data-id="'+String(id).replace(/"/g,'\"')+'"]');
    if(selectedCard) selectedCard.style.outline = '4px solid rgba(37,99,235,.22)';
    document.getElementById('karirLowonganTerpilih').className = 'karir-selected-lock-<%=rnd%> mb-4';
    document.getElementById('karirLowonganTerpilih').innerHTML = '<div class="d-flex gap-3 align-items-start"><div><i class="fas fa-lock text-success fa-lg mt-1"></i></div><div><div class="fw-bold">Lowongan terpilih dan terkunci</div><div>Lowongan: <strong>'+karirEscapeHtml(nama)+'</strong></div>' + (l.jadwalSeleksi ? '<small class="text-muted">Jadwal seleksi terdekat: '+karirEscapeHtml(l.jadwalSeleksi)+'</small>' : '<small class="text-muted">Jadwal seleksi mengikuti pengumuman panitia.</small>') + '</div></div>';
    karirRenderKelompokLowongan(l);
    var modalEl = document.getElementById('karirRegisterModal');
    if(modalEl){ var modal = bootstrap.Modal.getOrCreateInstance(modalEl); modal.show(); }
}
function karirRenderKelompokLowongan(l){
    var wrap = document.getElementById('karirKelompokWrapper'), sel = document.getElementById('karirKelompokSelect'), info = document.getElementById('karirKelompokInfo');
    if(!wrap || !sel || !info) return;
    var list = (l && l.kelompok) ? l.kelompok : [];
    if(!list.length){ wrap.classList.add('d-none'); sel.innerHTML=''; info.innerHTML=''; return; }
    wrap.classList.remove('d-none'); sel.innerHTML = '<option value="">Pilih kelompok/jalur pendaftaran</option>';
    list.forEach(function(k){ sel.insertAdjacentHTML('beforeend', '<option value="'+karirEscapeHtml(k.id)+'" data-deskripsi="'+karirEscapeHtml(k.deskripsi || '')+'" data-kuota="'+karirEscapeHtml(k.kuota || '')+'">'+karirEscapeHtml(k.nama || '-')+' - Kuota '+karirEscapeHtml(k.kuota || '-')+'</option>'); });
    info.innerHTML = 'Pilih kelompok yang paling sesuai. Jika tidak dipilih, sistem tetap menyimpan pendaftaran pada lowongan utama.';
    sel.onchange = function(){ var opt = sel.options[sel.selectedIndex]; info.innerHTML = opt && opt.value ? ('<strong>Info kelompok:</strong> '+karirEscapeHtml(opt.getAttribute('data-deskripsi') || 'Tidak ada deskripsi khusus.')+' <span class="badge bg-light text-dark ms-1">Kuota '+karirEscapeHtml(opt.getAttribute('data-kuota') || '-')+'</span>') : 'Pilih kelompok yang paling sesuai. Jika tidak dipilih, sistem tetap menyimpan pendaftaran pada lowongan utama.'; };
}
async function karirSubmitRegister(form){
    var lowonganInput = document.getElementById('karirLowonganId');
    var lowonganId = lowonganInput ? String(lowonganInput.value || '').trim() : '';
    var kelompokSelect = document.getElementById('karirKelompokSelect');
    var kelompokId = kelompokSelect ? String(kelompokSelect.value || '').trim() : '';
    if(!lowonganId){ karirToast('Silakan klik tombol Daftar Sekarang pada salah satu lowongan terlebih dahulu.', 'warning'); document.getElementById('lowongan').scrollIntoView({behavior:'smooth'}); return; }
    karirShowLoading();
    try {
        var fd = new FormData(form);
        fd.set('action', 'register');
        fd.set('lowongan_id', lowonganId);
        if(kelompokId) fd.set('kelompok_id', kelompokId);
        /* lowongan_id juga dikirim via query-string karena beberapa container lama/ZK/Tomcat
           tidak selalu membaca parameter text dari multipart/form-data melalui request.getParameter(). */
        var url = window.karirServiceUrl + '&action=register&lowongan_id=' + encodeURIComponent(lowonganId);
        if(kelompokId) url += '&kelompok_id=' + encodeURIComponent(kelompokId);

        /*
         * FIX V13:
         * Beberapa environment JSP/ZK/Tomcat lama tidak membaca field teks dari
         * multipart/form-data melalui request.getParameter(), walaupun file upload
         * tetap terkirim. Akibatnya validasi server menganggap nama/email/telp/dll
         * kosong dan menampilkan toast wajib isi. Karena itu field teks penting
         * dikirim juga melalui query-string, sedangkan file tetap dikirim melalui
         * FormData. Ini menjaga kompatibilitas Java 1.7 dan container lama tanpa
         * mengganti mekanisme upload dokumen.
         */
        ['nama','email','tempat_lahir','tanggal_lahir','jenis_kelamin','agama','telp','alamat'].forEach(function(name){
            var value = fd.get(name);
            if(value !== null && value !== undefined) {
                url += '&' + encodeURIComponent(name) + '=' + encodeURIComponent(String(value));
            }
        });

        var res = await fetch(url, {method:'POST', body:fd});
        var json = await res.json(); karirHideLoading();
        if(json.status === 'success') { karirToast(json.message || 'Pendaftaran berhasil.', 'success'); var modalEl=document.getElementById('karirRegisterModal'); if(modalEl){ bootstrap.Modal.getOrCreateInstance(modalEl).hide(); } form.reset(); if(lowonganInput) lowonganInput.value=''; var kw=document.getElementById('karirKelompokWrapper'); if(kw) kw.classList.add('d-none'); var lock=document.getElementById('karirLowonganTerpilih'); if(lock){ lock.innerHTML='<i class="fas fa-circle-info me-2"></i>Silakan pilih lowongan terlebih dahulu dari daftar lowongan kerja.'; } setTimeout(function(){ karirOpenLoginModal(); }, 900); }
        else karirToast(json.message || 'Pendaftaran gagal.', 'danger');
    } catch(e) { karirHideLoading(); karirToast('Terjadi kesalahan ketika mengirim pendaftaran.', 'danger'); }
}
async function karirSubmitResend(form){
    karirShowLoading();
    try { var res = await fetch(window.karirServiceUrl + '&action=resend_access', {method:'POST', body:new URLSearchParams(new FormData(form))}); var json = await res.json(); karirHideLoading();
        if(json.status === 'success') karirToast(json.message || 'Akses berhasil dikirim ulang.', 'success');
        else karirToast(json.message || 'Gagal mengirim ulang akses.', 'danger');
    } catch(e) { karirHideLoading(); karirToast('Terjadi kesalahan koneksi.', 'danger'); }
}
document.addEventListener('DOMContentLoaded', function(){
    karirLoadPengumumanKarir(); karirLoadLowongan(); karirLoadAgama();
    var q = document.getElementById('karirSearchLowongan'); if(q) q.addEventListener('keyup', function(e){ if(e.key === 'Enter') karirLoadLowongan(); });
    var f = document.getElementById('karirRegisterForm'); if(f) f.addEventListener('submit', function(e){ e.preventDefault(); karirSubmitRegister(this); });
    var r = document.getElementById('karirResendForm'); if(r) r.addEventListener('submit', function(e){ e.preventDefault(); karirSubmitResend(this); });
});
</script>
