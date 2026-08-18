<%@page import="ais.database.model.recruitment.CalonPegawai"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.KarirConfigUtil"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.action.master.sekolah.util.SekolahUtil"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.sekolah.Yayasan"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
    private String hk(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    try { Common.ROOT = request.getContextPath(); Common.REAL_PATH = application.getRealPath("/"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_header_karir.jsp:21");}
    String root = request.getContextPath();
    String rnd = Common.getGeneratedBarCode(7);

    CalonPegawai calonLogged = null;
    try { calonLogged = (CalonPegawai) request.getSession().getAttribute("KARIR_LOGGED_IN"); } catch(Exception e) { calonLogged = null; }
    if (calonLogged == null) {
        try { calonLogged = (CalonPegawai) request.getSession().getAttribute("CalonPegawai"); } catch(Exception e) { calonLogged = null; }
    }
    if (calonLogged == null) {
        calonLogged = KarirConfigUtil.resolveLoggedCandidate(request);
    }
    boolean isLoggedIn = calonLogged != null;

    String namaInstansi = "Instansi";
    String motto = "Portal Seleksi Penerimaan Pegawai / Karyawan Baru";
    String logo = root + "/img/logo.png";
    try {
        PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
        Sekolah sekolah = SekolahUtil.getSekolah();
        Yayasan yayasan = SekolahUtil.getYayasan();
        if (sekolah != null && sekolah.getNama() != null && sekolah.getNama().trim().length() > 0) namaInstansi = sekolah.getNama();
        else if (pt != null && pt.getNama() != null && pt.getNama().trim().length() > 0) namaInstansi = pt.getNama();
        else if (yayasan != null && yayasan.getNama() != null && yayasan.getNama().trim().length() > 0) namaInstansi = yayasan.getNama();
        if (pt != null && pt.getMotto() != null && pt.getMotto().trim().length() > 0) motto = pt.getMotto();
        String media = SekolahUtil.getSekolahMedia("logo_sekolah_");
        if (media == null || media.trim().length() == 0) media = SekolahUtil.getYayasanMedia("logo_yayasan_");
        if (media == null || media.trim().length() == 0) media = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
        if (media != null && media.trim().length() > 0) logo = media;
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_header_karir.jsp:50");}

    String judulPortal = KarirConfigUtil.text("label_karir_portal", "Portal KARIR");
    String warnaUtama = KarirConfigUtil.text("warna_utama_portal_karir", "var(--theme-primary, #2563eb)");
    String warnaKedua = KarirConfigUtil.text("warna_kedua_portal_karir", "var(--theme-hover, #7c3aed)");
    if ("#2563eb".equalsIgnoreCase(warnaUtama)) warnaUtama = "var(--theme-primary, #2563eb)";
    if ("#7c3aed".equalsIgnoreCase(warnaKedua)) warnaKedua = "var(--theme-hover, #7c3aed)";
    String labelLowongan = KarirConfigUtil.text("karir_menu_label_lowongan", "Info Lowongan Kerja");
    String labelPendaftaran = KarirConfigUtil.text("karir_menu_label_pendaftaran", "Pendaftaran Calon Pegawai");
    String labelSyarat = KarirConfigUtil.text("karir_menu_label_syarat", "Syarat & Ketentuan");
    String labelLoginPengumuman = KarirConfigUtil.text("karir_menu_label_login_pengumuman", "Login Pengumuman");
    String labelPanduan = KarirConfigUtil.text("karir_footer_label_panduan_pendaftaran", "Panduan Pendaftaran");

    String karirThemeCss = KarirConfigUtil.text("theme_css_portal_karir", "hijau_kuning.css");
    if (karirThemeCss == null || karirThemeCss.trim().length() == 0) karirThemeCss = "hijau_kuning.css";
    karirThemeCss = karirThemeCss.replace("/", "").replace("\\", "").trim();
    if (karirThemeCss.indexOf("..") >= 0 || !karirThemeCss.toLowerCase().endsWith(".css")) karirThemeCss = "hijau_kuning.css";
%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <meta name="description" content="Portal seleksi penerimaan pegawai dan karyawan baru">
    <title><%=hk(judulPortal)%> - <%=hk(namaInstansi)%></title>
    <link rel="shortcut icon" type="image/x-icon" href="<%=root%>/img/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/animate.css/4.1.1/animate.min.css" rel="stylesheet">
    <link href="<%=root%>/css/baru/base-theme.css?v=<%=rnd%>" rel="stylesheet">
    <link href="<%=root%>/css/baru/base-karir.css?v=<%=rnd%>" rel="stylesheet">
    <link href="<%=root%>/css/baru/<%=hk(karirThemeCss)%>?v=<%=rnd%>" rel="stylesheet">
    <style>
        :root {
            --karir-primary: <%=hk(warnaUtama)%>;
            --karir-secondary: <%=hk(warnaKedua)%>;
            --karir-gradient: linear-gradient(135deg, var(--karir-primary), var(--karir-secondary));
        }
    </style>
</head>
<body class="karir-body-<%=rnd%>">
<div class="karir-bg-<%=rnd%>"></div>
<div id="karirGlobalLoader" class="karir-loader-<%=rnd%>">
    <div class="text-center text-white p-4 rounded-4 karir-loader-box">
        <div class="spinner-border text-warning mb-3 karir-spinner-lg"></div>
        <h6 class="fw-bold mb-1">Memproses data KARIR...</h6>
        <div class="small text-white-50">Mohon tunggu, sistem sedang mengambil data terbaru.</div>
    </div>
</div>
<nav class="navbar navbar-expand-lg sticky-top karir-navbar-<%=rnd%>">
    <div class="container py-2">
        <a class="navbar-brand d-flex align-items-center gap-3" href="<%=root%>/karir">
            <img src="<%=hk(logo)%>" class="karir-brand-logo-<%=rnd%>" alt="Logo">
            <span>
                <span class="d-block fw-bold karir-gradient-text-<%=rnd%> karir-brand-title"><%=hk(judulPortal)%></span>
                <small class="text-muted"><%=hk(namaInstansi)%></small>
            </span>
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#karirNavbar<%=rnd%>">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="karirNavbar<%=rnd%>">
            <ul class="navbar-nav ms-auto mb-2 mb-lg-0 align-items-lg-center gap-lg-2">
                <% if (!isLoggedIn) { %>
                    <li class="nav-item"><a class="nav-link fw-semibold" href="<%=root%>/karir#lowongan"><i class="fas fa-briefcase me-1"></i><%=hk(labelLowongan)%></a></li>
                    <li class="nav-item"><a class="nav-link fw-semibold" href="<%=root%>/karir#pendaftaran"><i class="fas fa-user-plus me-1"></i><%=hk(labelPendaftaran)%></a></li>
                    <li class="nav-item"><a class="nav-link fw-semibold" href="<%=root%>/karir?halaman=syarat_ketentuan"><i class="fas fa-file-signature me-1"></i><%=hk(labelSyarat)%></a></li>
                    <li class="nav-item"><a class="nav-link fw-semibold" href="<%=root%>/karir?halaman=panduan_pendaftaran"><i class="fas fa-book-open me-1"></i><%=hk(labelPanduan)%></a></li>
                    <li class="nav-item"><a class="btn btn-sm karir-btn-gradient-<%=rnd%> karir-pill-<%=rnd%> px-3" href="javascript:void(0)" onclick="karirOpenLoginModal()"><i class="fas fa-right-to-bracket me-1"></i><%=hk(labelLoginPengumuman)%></a></li>
                <% } else { %>
                    <li class="nav-item"><span class="nav-link fw-bold"><i class="fas fa-user-tie me-1"></i><%=hk(calonLogged.getNama())%></span></li>
                    <li class="nav-item"><a class="btn btn-sm btn-outline-danger karir-pill-<%=rnd%> px-3" href="javascript:void(0)" onclick="karirConfirmLogout()"><i class="fas fa-sign-out-alt me-1"></i>Keluar</a></li>
                <% } %>
            </ul>
        </div>
    </div>
</nav>

<div class="modal fade" id="karirLoginModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 rounded-5 shadow-lg overflow-hidden">
            <div class="modal-header border-0 text-white karir-modal-gradient">
                <div>
                    <h5 class="modal-title fw-bold"><i class="fas fa-user-lock me-2"></i>Login Portal KARIR</h5>
                    <small class="text-white-50">Gunakan username/email dan password yang dikirim ke email Anda.</small>
                </div>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
            </div>
            <form id="karirLoginForm" class="modal-body p-4">
                <input type="hidden" name="action" value="login">
                <div class="mb-3">
                    <label class="form-label fw-semibold"><%=Common.getBahasaConfig("Username / Email")%></label>
                    <input class="form-control rounded-4 py-3" name="username" autocomplete="username" required>
                </div>
                <div class="mb-3">
                    <label class="form-label fw-semibold"><%=Common.getBahasaConfig("Password")%></label>
                    <input class="form-control rounded-4 py-3" name="password" type="password" autocomplete="current-password" required>
                </div>
                <button class="btn karir-btn-gradient-<%=rnd%> rounded-pill w-100 py-3 fw-bold"><i class="fas fa-right-to-bracket me-2"></i>Masuk ke Portal</button>
                <div class="text-center mt-3 small text-muted">Lupa akses? Gunakan fitur kirim ulang akses di halaman utama.</div>
            </form>
        </div>
    </div>
</div>

<script>
    window.karirServiceUrl = '<%=root%>/karir?hanya_tampil_jsp=true&p=karir&s=_karir_service';
    window.karirRootUrl = '<%=root%>/karir';
    window.karirShowLoading = function(){ var x=document.getElementById('karirGlobalLoader'); if(x) x.classList.add('show'); };
    window.karirHideLoading = function(){ var x=document.getElementById('karirGlobalLoader'); if(x) x.classList.remove('show'); };
    window.karirOpenLoginModal = function(){ var el = document.getElementById('karirLoginModal'); if(!el) return; if(window.bootstrap && bootstrap.Modal){ bootstrap.Modal.getOrCreateInstance(el).show(); } else { el.style.display='block'; el.classList.add('show'); } };
    window.karirConfirmLogout = function(){ if(confirm('Keluar dari Portal KARIR?')) window.location.href = '<%=root%>/karir?auth_action=logout'; };
    window.karirToast = function(message, type){
        var container = document.getElementById('karirToastContainer');
        if(!container){ container = document.createElement('div'); container.id='karirToastContainer'; container.className='toast-container position-fixed top-0 end-0 p-3'; container.style.zIndex='10000'; container.style.maxWidth='380px'; container.style.width='min(380px, calc(100vw - 24px))'; document.body.appendChild(container); }
        var bg = type === 'danger' ? 'bg-danger' : (type === 'warning' ? 'bg-warning text-dark' : 'bg-success');
        var icon = type === 'danger' ? 'fas fa-triangle-exclamation' : (type === 'warning' ? 'fas fa-circle-info' : 'fas fa-circle-check');
        var id = 'toast_' + new Date().getTime();
        container.insertAdjacentHTML('beforeend', '<div id="'+id+'" class="toast '+bg+' text-white border-0 shadow-lg rounded-4 mb-2 w-100" role="alert"><div class="d-flex"><div class="toast-body fw-semibold"><i class="'+icon+' me-2"></i>'+karirEscapeHtml(message)+'</div><button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div></div>');
        var el = document.getElementById(id); var toast = new bootstrap.Toast(el, {delay:5200}); toast.show(); el.addEventListener('hidden.bs.toast', function(){ el.remove(); });
    };
    window.karirEscapeHtml = function(s){ return String(s == null ? '' : s).replace(/[&<>'"]/g, function(c){ return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]; }); };
    document.addEventListener('DOMContentLoaded', function(){
        var f = document.getElementById('karirLoginForm');
        if(f){ f.addEventListener('submit', async function(e){
            e.preventDefault(); karirShowLoading();
            try { var res = await fetch(window.karirServiceUrl, {method:'POST', body:new URLSearchParams(new FormData(f))}); var json = await res.json(); karirHideLoading();
                if(json.status === 'success') { karirToast('Login berhasil. Mengalihkan ke dashboard...', 'success'); setTimeout(function(){ window.location.href = window.karirRootUrl; }, 700); }
                else karirToast(json.message || 'Login gagal.', 'danger');
            } catch(ex) { karirHideLoading(); karirToast('Terjadi kesalahan koneksi.', 'danger'); }
        }); }
    });
</script>
