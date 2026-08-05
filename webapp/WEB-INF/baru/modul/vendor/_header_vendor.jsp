<%@page import="ais.database.model.asset.PenyediaAsset"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
    private String hv(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    String rnd = Common.getGeneratedBarCode(7);
    long cacheBuster = System.currentTimeMillis();
    PenyediaAsset vendorLogged = null;
    try { vendorLogged = (PenyediaAsset) request.getSession().getAttribute("VENDOR_LOGGED_IN"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/_header_vendor.jsp:16");}
    boolean isLoggedIn = vendorLogged != null;

    String judulHeader = "Portal Vendor";
    String judulNamaPerguruanTinggi = "";
    String motto = "";
    String telepon = "";
    String alamat1 = "";
    String emailInstansi = "";
    String logoPerguruanTinggi = "";
    String backgroundPerguruanTinggi = "";
    String cssTema = "";
    String vendorMenuDaftar = "Daftar Vendor";
    String vendorMenuLogin = "Login Vendor";
    String vendorMenuDashboard = "Dashboard";
    String vendorMenuLogout = "Keluar";

    try {
        PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
        if (pt != null) {
            judulNamaPerguruanTinggi = pt.getNama() == null ? "" : pt.getNama();
            motto = pt.getMotto() == null ? "" : pt.getMotto();
            telepon = pt.getTelepon() == null ? "" : pt.getTelepon();
            alamat1 = pt.getAlamat1() == null ? "" : pt.getAlamat1();
            emailInstansi = pt.getEmail() == null ? "" : pt.getEmail();
            String rawCss = pt.getCss();
            if (rawCss != null && !rawCss.trim().isEmpty()) {
                String fileName = rawCss.substring(rawCss.lastIndexOf("/") + 1);
                cssTema = "/css/baru/" + fileName;
            }
        }
        logoPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
        backgroundPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "background_perguruanTinggi_");
        judulHeader = Common.getKonfigurasi("judul_header_vendor", "Pendaftaran Vendor").getNilai();
        vendorMenuDaftar = Common.getKonfigurasi("vendor_menu_daftar", "Daftar Vendor").getNilai();
        vendorMenuLogin = Common.getKonfigurasi("vendor_menu_login", "Login Vendor").getNilai();
        vendorMenuDashboard = Common.getKonfigurasi("vendor_menu_dashboard", "Dashboard Vendor").getNilai();
        vendorMenuLogout = Common.getKonfigurasi("vendor_menu_logout", "Keluar").getNilai();
        // Default KOSONG agar portal vendor MENGIKUTI tema institusi (pt.getCss()) seperti header.jsp.
        // Isi konfigurasi ini HANYA bila ingin memaksa tema khusus portal vendor (override opsional).
        String cfgThemeVendor = Common.getKonfigurasi("theme_css_portal_vendor", "").getNilai();
        if (cfgThemeVendor != null && cfgThemeVendor.trim().length() > 0) {
            cfgThemeVendor = cfgThemeVendor.trim();
            if (cfgThemeVendor.indexOf("/") < 0 && cfgThemeVendor.indexOf("\\") < 0 && cfgThemeVendor.endsWith(".css")) {
                cssTema = "/css/baru/" + cfgThemeVendor;
            }
        }
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/_header_vendor.jsp:63");
    }
    if (judulNamaPerguruanTinggi == null || judulNamaPerguruanTinggi.trim().isEmpty()) judulNamaPerguruanTinggi = "Instansi";
    if (logoPerguruanTinggi == null || logoPerguruanTinggi.trim().isEmpty()) logoPerguruanTinggi = Common.ROOT + "/img/logo.png";
    if (backgroundPerguruanTinggi == null || backgroundPerguruanTinggi.trim().isEmpty()) backgroundPerguruanTinggi = Common.ROOT + "/img/bg1.png";
    if (cssTema == null || cssTema.trim().isEmpty()) cssTema = "/css/baru/hijau_kuning.css";
%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= hv(judulHeader) %> | <%= hv(judulNamaPerguruanTinggi) %></title>
    <link rel="shortcut icon" href="<%= logoPerguruanTinggi %>" type="image/png"/>
    <link rel="manifest" href="<%=request.getContextPath()%>/component/uiux/assets/img/favicons/manifest.json">
    <meta name="msapplication-TileImage" content="<%=request.getContextPath()%>/component/uiux/assets/img/favicons/mstile-150x150.png">
    <meta name="theme-color" content="#ffffff">

    <script src="<%=request.getContextPath()%>/component/uiux/assets/js/config.js"></script>
    <script src="<%=request.getContextPath()%>/component/uiux/vendors/simplebar/simplebar.min.js"></script>
    <script data-cfasync="false" src="https://code.jquery.com/jquery-3.5.1.min.js"></script>

    <link rel="preconnect" href="https://fonts.gstatic.com">
    <link href="https://fonts.googleapis.com/css?family=Open+Sans:300,400,500,600,700&amp;display=swap" rel="stylesheet">
    <link href="<%=request.getContextPath()%>/component/uiux/vendors/simplebar/simplebar.min.css" rel="stylesheet">
    <link href="<%=request.getContextPath()%>/component/uiux/assets/css/theme-rtl.css" rel="stylesheet" id="style-rtl">
    <link href="<%=request.getContextPath()%>/component/uiux/assets/css/theme.css" rel="stylesheet" id="style-default">
    <link href="<%=request.getContextPath()%>/component/uiux/assets/css/user-rtl.css" rel="stylesheet" id="user-style-rtl">
    <link href="<%=request.getContextPath()%>/component/uiux/assets/css/user.css" rel="stylesheet" id="user-style-default">
    <link href="<%=request.getContextPath()%>/css/bandbox.css" rel="stylesheet">
    <link href="<%=request.getContextPath()%>/css/timeline.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/animate.css/4.1.1/animate.min.css"/>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css"/>
    <link href="<%=request.getContextPath()%>/css/baru/base-theme.css?v=<%=cacheBuster%>" rel="stylesheet">
    <link href="<%=request.getContextPath()%>/css/baru/base-vendor.css?v=<%=cacheBuster%>" rel="stylesheet">
    <% if (cssTema != null && !cssTema.trim().isEmpty()) { %>
        <link href="<%=request.getContextPath()%><%=cssTema%>?v=<%=cacheBuster%>" rel="stylesheet">
    <% } %>
    <script>
      var isRTL = JSON.parse(localStorage.getItem('isRTL'));
      if (isRTL) {
        var linkDefault = document.getElementById('style-default');
        var userLinkDefault = document.getElementById('user-style-default');
        if(linkDefault) linkDefault.setAttribute('disabled', true);
        if(userLinkDefault) userLinkDefault.setAttribute('disabled', true);
        document.querySelector('html').setAttribute('dir', 'rtl');
      } else {
        var linkRTL = document.getElementById('style-rtl');
        var userLinkRTL = document.getElementById('user-style-rtl');
        if(linkRTL) linkRTL.setAttribute('disabled', true);
        if(userLinkRTL) userLinkRTL.setAttribute('disabled', true);
      }
    </script>
    <style>
        body.vendor-body-<%=rnd%> { font-family: "Open Sans", Poppins, Arial, sans-serif; background:#f7f9fc; color:#1f2937; }
        .vendor-bg-<%=rnd%> { position:fixed; inset:0; background:linear-gradient(180deg, rgba(255,255,255,.94), rgba(248,250,252,.96)), url('<%=backgroundPerguruanTinggi%>') center/cover no-repeat; z-index:-2; }
        .vendor-navbar-<%=rnd%> { backdrop-filter: blur(16px); background:rgba(255,255,255,.92); border-bottom:1px solid rgba(148,163,184,.24); }
        .vendor-brand-logo-<%=rnd%> { width:46px; height:46px; object-fit:contain; filter:drop-shadow(0 8px 18px rgba(0,0,0,.15)); }
        .vendor-gradient-text-<%=rnd%> { background:var(--theme-gradient); -webkit-background-clip:text; background-clip:text; color:transparent; }
        .vendor-card-<%=rnd%> { border:0; border-radius:1.15rem; box-shadow:0 18px 45px rgba(15,23,42,.08); overflow:hidden; }
        .vendor-soft-card-<%=rnd%> { background:#fff; border:1px solid rgba(148,163,184,.18); border-radius:1.15rem; box-shadow:0 12px 28px rgba(15,23,42,.05); }
        .vendor-pill-<%=rnd%> { border-radius:999px; }
        .vendor-section-title-<%=rnd%> { font-weight:800; color:#0f172a; letter-spacing:-.03em; }
        .vendor-btn-gradient-<%=rnd%> { background:var(--theme-gradient)!important; color:var(--theme-text-on-primary)!important; border:0!important; box-shadow:0 10px 24px rgba(15,23,42,.18); }
        .vendor-btn-gradient-<%=rnd%>:hover { transform:translateY(-1px); filter:brightness(.98); }
        .vendor-icon-box-<%=rnd%> { width:46px; height:46px; display:inline-flex; align-items:center; justify-content:center; border-radius:14px; background:var(--theme-light); color:var(--theme-primary); }
        .vendor-stat-<%=rnd%> { border-left:4px solid var(--theme-primary); }
        .vendor-required::after { content:" *"; color:#dc3545; font-weight:700; }
        .vendor-loader-<%=rnd%> { position:fixed; inset:0; display:none; align-items:center; justify-content:center; background:rgba(15,23,42,.70); backdrop-filter:blur(8px); z-index:9999; }
        .vendor-loader-<%=rnd%>.show { display:flex; }
        .vendor-table thead th { font-size:.76rem; text-transform:uppercase; letter-spacing:.04em; color:#64748b; background:#f8fafc; }
        .vendor-badge-soft { background:var(--theme-light); color:var(--theme-primary); border:1px solid rgba(148,163,184,.24); }
        .nav-pills .nav-link { color:#334155; font-weight:700; border-radius:999px; }
        .nav-pills .nav-link.active { background:var(--theme-gradient)!important; color:var(--theme-text-on-primary)!important; }
    </style>
</head>
<body class="vendor-body-<%=rnd%>">
<div class="vendor-bg-<%=rnd%>" style="--vendor-bg-image:url('<%=backgroundPerguruanTinggi%>');"></div>
<div id="vendorGlobalLoader" class="vendor-loader-<%=rnd%>">
    <div class="text-center text-white p-4 rounded-4" style="background:rgba(15,23,42,.75); border:1px solid rgba(255,255,255,.18); min-width:260px;">
        <div class="spinner-border text-warning mb-3" style="width:3.25rem;height:3.25rem;"></div>
        <h6 class="fw-bold mb-1">Memproses data...</h6>
        <div class="small text-white-50">Mohon tunggu sebentar.</div>
    </div>
</div>
<nav class="navbar navbar-expand-lg sticky-top vendor-navbar-<%=rnd%>">
    <div class="container py-2">
        <a class="navbar-brand d-flex align-items-center gap-3" href="<%=Common.ROOT%>/vendor">
            <img src="<%=logoPerguruanTinggi%>" class="vendor-brand-logo-<%=rnd%>" alt="Logo">
            <span>
                <span class="d-block fw-bold vendor-gradient-text-<%=rnd%>" style="line-height:1;"><%=hv(judulHeader)%></span>
                <small class="text-muted"><%=hv(judulNamaPerguruanTinggi)%></small>
            </span>
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#vendorNavbar<%=rnd%>">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="vendorNavbar<%=rnd%>">
            <ul class="navbar-nav ms-auto mb-2 mb-lg-0 align-items-lg-center gap-lg-2">
                <% if (!isLoggedIn) { %>
                    <li class="nav-item"><a class="nav-link fw-semibold" href="javascript:void(0)" onclick="vendorOpenRegisterModal()"><i class="fas fa-user-plus me-1"></i><%= hv(vendorMenuDaftar) %></a></li>
                    <li class="nav-item"><a class="nav-link fw-semibold" href="#syarat"><i class="fas fa-clipboard-check me-1"></i>Syarat & Ketentuan</a></li>
                    <li class="nav-item"><a class="nav-link fw-semibold" href="#kontak"><i class="far fa-address-card me-1"></i>Kontak</a></li>
                    <li class="nav-item"><a class="btn btn-sm vendor-btn-gradient-<%=rnd%> vendor-pill-<%=rnd%> px-3" href="javascript:void(0)" onclick="vendorOpenLoginModal()"><i class="fas fa-sign-in-alt me-1"></i><%= hv(vendorMenuLogin) %></a></li>
                <% } else { %>
                    <li class="nav-item"><span class="nav-link fw-bold"><i class="fas fa-building me-1"></i><%=hv(vendorLogged.getNama())%></span></li>
                    <li class="nav-item"><a class="btn btn-sm btn-outline-danger vendor-pill-<%=rnd%> px-3" href="javascript:void(0)" onclick="vendorConfirmLogout()"><i class="fas fa-sign-out-alt me-1"></i><%= hv(vendorMenuLogout) %></a></li>
                <% } %>
            </ul>
        </div>
    </div>
</nav>
<script>
    window.vendorServiceUrl = '<%=Common.ROOT%>/vendor?hanya_tampil_jsp=true&p=vendor&s=_vendor_service';
    window.vendorRootUrl = '<%=Common.ROOT%>/vendor';
    window.vendorShowLoading = function(){ var x=document.getElementById('vendorGlobalLoader'); if(x) x.classList.add('show'); };
    window.vendorHideLoading = function(){ var x=document.getElementById('vendorGlobalLoader'); if(x) x.classList.remove('show'); };
    window.vendorToast = function(message, type){
        var container = document.getElementById('vendorToastContainer');
        if(!container){
            container = document.createElement('div');
            container.id = 'vendorToastContainer';
            container.className = 'toast-container position-fixed top-0 end-0 p-3';
            container.style.zIndex = '10000';
            document.body.appendChild(container);
        }
        var bg = type === 'danger' ? 'bg-danger' : (type === 'warning' ? 'bg-warning text-dark' : 'bg-success');
        var icon = type === 'danger' ? 'fas fa-triangle-exclamation' : (type === 'warning' ? 'fas fa-circle-info' : 'fas fa-circle-check');
        var id = 'toast_' + new Date().getTime();
        container.insertAdjacentHTML('beforeend', '<div id="'+id+'" class="toast '+bg+' text-white border-0 shadow-lg rounded-4 mb-2" role="alert"><div class="d-flex"><div class="toast-body fw-semibold"><i class="'+icon+' me-2"></i>'+message+'</div><button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div></div>');
        var el = document.getElementById(id); var toast = new bootstrap.Toast(el, {delay: 5200}); toast.show(); el.addEventListener('hidden.bs.toast', function(){ el.remove(); });
    };
    window.vendorConfirmLogout = function(){
        if(confirm('Keluar dari portal vendor?')) window.location.href = '<%=Common.ROOT%>/vendor?auth_action=logout';
    };
</script>
