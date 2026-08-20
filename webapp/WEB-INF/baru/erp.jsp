<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.common.Common"%>
<%@ page import="ais.database.model.sekolah.Sekolah" %>
<%@ page import="java.util.Calendar" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
// ============================================================================================
// TANGKAP PARAMETER URL (WHITE LABEL SUPPORT)
// ============================================================================================
String paramJudul = request.getParameter("judul");
String paramLogo = request.getParameter("logo");
String paramClient = request.getParameter("client");
String paramPresentBy = request.getParameter("present_by");
String paramTelp = request.getParameter("telp_present");

// ============================================================================================
// OPTIMASI LOGIKA: DEKLARASI VARIABEL DATA INSTITUSI
// Mengambil object Perguruan Tinggi sekali saja untuk mencegah NullPointerException 
// dan meningkatkan efisiensi pembacaan ke database.
// ============================================================================================
ais.database.model.sekolah.Sekolah sekolah = ais.action.master.sekolah.util.SekolahUtil.getSekolah(request);
PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);

// Inisialisasi variabel dengan default value untuk mencegah halaman Crash (Error 500)
String Telepon = "-";
String Alamat1 = "Alamat belum dikonfigurasi";
String Email = "-";
String namaPt = "Enterprise Education";

// Validasi null check sebelum mengambil data
if (pt != null) {
    if (pt.getTelepon() != null && !pt.getTelepon().isEmpty()) {
        Telepon = pt.getTelepon();
    }
    if (pt.getAlamat1() != null && !pt.getAlamat1().isEmpty()) {
        Alamat1 = pt.getAlamat1();
    }
    if (pt.getEmail() != null && !pt.getEmail().isEmpty()) {
        Email = pt.getEmail();
    }
    if (pt.getNama() != null && !pt.getNama().isEmpty()) {
        namaPt = pt.getNama();
    }
}

String background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "background_perguruanTinggi_");

// Override Judul dan Logo jika parameter White-Label tersedia melalui URL
String judul = (paramJudul != null && !paramJudul.trim().isEmpty()) ? paramJudul : namaPt;
String logo_PerguruanTinggi = (paramLogo != null && !paramLogo.trim().isEmpty()) ? Common.ROOT + "/img/" + paramLogo : ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");

// ============================================================================================
// KONFIGURASI HEADER
// ============================================================================================
String judulHeader = "";
try {
    Konfigurasi configHeader = Common.getKonfigurasi("judul_header", "eCampus");
    if (configHeader != null && configHeader.getNilai() != null) {
        judulHeader = configHeader.getNilai();
    } else {
        judulHeader = "eCampus";
    }
} catch (Exception e) {
    judulHeader = "eCampus"; 
}

// Randomizer untuk cache busting pada class tertentu
String rnd = String.valueOf(System.currentTimeMillis());
long cacheBuster = System.currentTimeMillis();

// ============================================================================================
// SEO & URL PARAMETER HANDLING
// ============================================================================================
// Mendapatkan Full URL Dinamis untuk keperluan SEO Canonical dan Open Graph
String currentUrl = request.getRequestURL().toString();

// Mendapatkan seluruh Query String untuk dipassing ke link Presentasi, Proposal, dan PKS
String queryString = request.getQueryString();
String appendParams = (queryString != null && !queryString.isEmpty()) ? "?" + queryString : "";
%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta 
        name="viewport" 
        content="width=device-width, initial-scale=1.0"
    >
    <title><%= Common.getBahasaConfig("Enterprise Education - Portal Terpadu") %> | <%=judul%></title>
    
    <meta
        name="description"
        content="<%= Common.getBahasaConfig("Enterprise Education adalah ekosistem ERP pendidikan terpadu untuk kampus, sekolah, pesantren, yayasan, unit usaha, keuangan, akademik, pembayaran digital, dashboard eksekutif, dan layanan mobile dalam satu platform modern. Kini juga terintegrasi dengan eMedic (Sistem Informasi Rumah Sakit, Klinik, Puskesmas, dan layanan medis tertentu) serta Sistem Gudang Pusat, Outlet, POS, Ekspedisi, Investor, dan Akuntansi terpadu.") %>"
    >
    <meta
        name="keywords"
        content="eCampus, eSchool, ePesantren, eMedic, SIAKAD, Sistem Informasi Akademik, ERP Pendidikan, Aplikasi Sekolah, Software Kampus, PPDB Online, Manajemen Pesantren, Sistem Terpadu, Sistem Informasi Rumah Sakit, SIRS, Aplikasi Klinik, Aplikasi Puskesmas, Rekam Medis Elektronik, Lowongan Pekerjaan Pendidikan, Portal Karir, Rekrutmen Pegawai, Antar Jemput Siswa, Shuttle Sekolah, Pickup Gate, Kartu Penjemput, Monitor Antrian, Soundbox Kelas, Sistem Gudang Pusat dan Cabang, Manajemen Outlet, Aplikasi POS Kasir, Sistem Ekspedisi, Distribusi Logistik, Bagi Hasil Investor, Sistem Akuntansi Terpadu, HPP Otomatis, Koperasi Sekolah, Kantin Sekolah"
    >
    <meta 
        name="author" 
        content="<%=judul%>"
    >
    <meta 
        name="robots" 
        content="index, follow, max-image-preview:large, max-snippet:-1, max-video-preview:-1"
    >
    <link 
        rel="canonical" 
        href="<%= currentUrl %>"
    >

    <meta 
        property="og:type" 
        content="website"
    >
    <meta 
        property="og:url" 
        content="<%= currentUrl %>"
    >
    <meta 
        property="og:title" 
        content="<%= Common.getBahasaConfig("Enterprise Education - Portal Terpadu") %> | <%=judul%>"
    >
    <meta 
        property="og:description" 
        content="<%= Common.getBahasaConfig("Modernisasi tata kelola pendidikan melalui satu platform ERP yang menghubungkan akademik, keuangan, SDM, aset, persuratan, pembayaran digital, dashboard pimpinan, layanan mobile, serta modul eMedic untuk Rumah Sakit, Klinik, dan Puskesmas.") %>"
    >
    <meta 
        property="og:image" 
        content="<%=logo_PerguruanTinggi%>"
    >
    <meta 
        property="og:site_name" 
        content="<%=judul%>"
    >

    <meta 
        name="twitter:card" 
        content="summary_large_image"
    >
    <meta 
        name="twitter:title" 
        content="<%= Common.getBahasaConfig("Enterprise Education - Portal Terpadu") %> | <%=judul%>"
    >
    <meta 
        name="twitter:description" 
        content="<%= Common.getBahasaConfig("Modernisasi tata kelola pendidikan melalui satu platform ERP yang menghubungkan akademik, keuangan, SDM, aset, persuratan, pembayaran digital, dashboard pimpinan, layanan mobile, serta modul eMedic untuk Rumah Sakit, Klinik, dan Puskesmas.") %>"
    >
    <meta 
        name="twitter:image" 
        content="<%=logo_PerguruanTinggi%>"
    >

    <link 
        rel="icon" 
        href="<%=logo_PerguruanTinggi%>" 
        type="image/x-icon"
    >
    <link 
        rel="shortcut icon" 
        href="<%=logo_PerguruanTinggi%>" 
        type="image/x-icon"
    >
    
    <link 
        href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" 
        rel="stylesheet"
    >
    <link 
        rel="stylesheet" 
        href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css"
    >
    
    <!-- MENGIMPOR TEMA GENERAL -->
    <link href="<%=request.getContextPath() %>/css/baru/base-theme.css?v=<%= cacheBuster %>" rel="stylesheet">
    <% 
    String rawCss = (pt != null) ? pt.getCss() : null;
    if (rawCss != null && !rawCss.trim().isEmpty()) {
        String fileName = rawCss.substring(rawCss.lastIndexOf("/") + 1);
    %>
        <link href="<%=request.getContextPath() %>/css/baru/<%= fileName %>?v=<%= cacheBuster %>" rel="stylesheet">
    <% } %>

    <style>
        /* Base HTML & Body Settings */
        html { scroll-behavior: smooth; }

        body {
            background-color: var(--secondary-color);
            font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
            color: var(--text-dark);
            display: flex;
            flex-direction: column;
            min-height: 100vh;
            margin: 0;
            overflow-x: hidden;
        }

        /* Background Pattern Setup */
        .page-bg-<%=rnd%> { 
            position: fixed; 
            top: 0; 
            left: 0; 
            width: 100vw; 
            height: 100vh; 
            background: url('<%=Common.ROOT%>/img/pmb_bg.jpg') center/cover no-repeat; 
            opacity: 0.05; 
            z-index: -999; 
            pointer-events: none; 
        }

        /* Header Hero Section */
        .hero-section-<%=rnd%> { 
            background: var(--theme-gradient, linear-gradient(135deg, rgba(13, 110, 253, 0.95) 0%, rgba(30, 41, 59, 0.95) 100%));
            color: white; 
            border-radius: 0 0 3rem 3rem; 
            box-shadow: 0 10px 30px rgba(0,0,0,0.15); 
            position: relative; 
            z-index: 2; 
            padding: 3rem 0 8rem 0; 
        }
        .hero-section-<%=rnd%>::before {
            content: ''; position: absolute; top: 0; left: 0; right: 0; bottom: 0;
            background: url('<%=Common.ROOT%>/img/pmb_bg.jpg') center/cover no-repeat;
            opacity: 0.15; mix-blend-mode: overlay; z-index: -1;
        }
        
        main { flex: 1; }

        /* Logo Configuration */
        .logo-img {
            height: 100px; 
            object-fit: contain; 
            margin-bottom: 1rem;
            filter: drop-shadow(0px 8px 16px rgba(0,0,0,0.3));
            background: rgba(255,255,255,0.95); 
            padding: 10px 20px; 
            border-radius: 12px;
        }

        /* Sticky Floating Navigation Bar (Smart Wrap - tanpa horizontal scrollbar) */
        .floating-nav-wrapper {
            position: sticky;
            top: 16px;
            z-index: 1050;
            display: flex;
            justify-content: center;
            margin-bottom: 2.8rem;
            padding: 0 12px;
            pointer-events: none;
        }
        .floating-nav {
            position: relative;
            width: min(1280px, 100%);
            max-width: calc(100vw - 48px);
            background: rgba(255, 255, 255, 0.94);
            backdrop-filter: blur(18px);
            -webkit-backdrop-filter: blur(18px);
            border-radius: 32px;
            padding: 12px 14px;
            box-shadow: 0 22px 55px rgba(15, 23, 42, 0.16);
            border: 1px solid rgba(226, 232, 240, 0.95);
            pointer-events: auto;
            display: block;
            overflow: visible;
            transition: transform .25s ease, box-shadow .25s ease, background .25s ease;
        }
        .floating-nav::before {
            content: "";
            position: absolute;
            inset: 0;
            border-radius: inherit;
            pointer-events: none;
            background:
                radial-gradient(circle at top left, rgba(59, 130, 246, 0.10), transparent 32%),
                radial-gradient(circle at bottom right, rgba(245, 158, 11, 0.10), transparent 32%);
        }
        .floating-nav:hover {
            transform: translateY(-2px);
            box-shadow: 0 26px 70px rgba(15, 23, 42, 0.20);
            background: rgba(255, 255, 255, 0.98);
        }
        .landing-nav-list {
            position: relative;
            z-index: 1;
            width: 100%;
            display: flex !important;
            flex-wrap: wrap !important;
            align-items: stretch;
            justify-content: center;
            gap: 8px;
            padding: 0;
            margin: 0;
        }
        .landing-nav-list .nav-item {
            display: flex;
            flex: 0 1 88px;
            min-width: 0;
        }
        .custom-nav-link {
            width: 100%;
            min-height: 58px;
            color: #334155 !important;
            background: rgba(248, 250, 252, 0.74);
            border: 1px solid rgba(226, 232, 240, 0.78);
            font-weight: 900;
            font-size: 0.66rem;
            line-height: 1.15;
            text-transform: uppercase;
            letter-spacing: 0.045rem;
            padding: 9px 7px !important;
            border-radius: 20px;
            transition: all 0.25s ease;
            margin: 0;
            display: flex !important;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            gap: 5px;
            text-align: center;
            white-space: normal;
        }
        .custom-nav-link i {
            margin: 0 !important;
            font-size: 1rem;
            line-height: 1;
            color: var(--theme-primary, #0d6efd);
            opacity: 0.9;
        }
        .custom-nav-link:hover, .custom-nav-link.active {
            background: var(--theme-gradient, linear-gradient(135deg, #0d6efd, #2563eb));
            color: #ffffff !important;
            border-color: rgba(255,255,255,0.35);
            box-shadow: 0 12px 24px rgba(37, 99, 235, 0.25);
            transform: translateY(-2px);
        }
        .custom-nav-link:hover i, .custom-nav-link.active i {
            color: #ffffff;
            opacity: 1;
        }
        .mobile-smart-nav {
            background: rgba(255,255,255,0.96) !important;
            backdrop-filter: blur(16px);
            -webkit-backdrop-filter: blur(16px);
            border-radius: 24px;
            border: 1px solid rgba(226,232,240,.95);
            box-shadow: 0 18px 42px rgba(15,23,42,.13);
        }
        .mobile-smart-nav .navbar-brand {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            color: var(--theme-primary, #0d6efd) !important;
        }
        .mobile-smart-nav .navbar-collapse {
            max-height: 72vh;
            overflow-y: auto;
            padding: 8px 0 4px;
        }
        .mobile-smart-nav .nav-link {
            border-radius: 16px;
            padding: .75rem .85rem;
            margin: .18rem 0;
            background: #f8fafc;
            border: 1px solid #edf2f7;
            transition: all .22s ease;
        }
        .mobile-smart-nav .nav-link:hover {
            background: #eff6ff;
            color: var(--theme-primary, #0d6efd) !important;
            transform: translateX(3px);
        }
        @media (min-width: 1400px) {
            .landing-nav-list .nav-item { flex-basis: 92px; }
            .custom-nav-link { font-size: 0.68rem; }
        }
        @media (max-width: 1199.98px) {
            .floating-nav { max-width: calc(100vw - 36px); }
            .landing-nav-list .nav-item { flex-basis: 94px; }
        }
        
        #section-intro, #section-executive, #section-portal, #section-value, #section-impact, #section-roadmap, #section-partnership, #section-about, #section-payment, #section-kiosk, #section-hardware, #section-investment, #section-modules, #section-documents, #section-contact { scroll-margin-top: 110px; }

        /* Main Menu Portal Panel */
        .menu-panel {
            background: rgba(255, 255, 255, 0.98); backdrop-filter: blur(15px);
            border-radius: 20px; padding: 2.5rem 1.5rem; box-shadow: 0 15px 40px rgba(0,0,0,0.08);
            border: 1px solid rgba(255, 255, 255, 0.8); 
        }

        .btn-custom-group { display: flex; flex-wrap: wrap; justify-content: center; gap: 8px; margin-bottom: 1.5rem; }
        .btn { 
            border-radius: 8px; font-weight: 600; padding: 10px 18px; 
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); 
            display: flex; align-items: center; justify-content: center; gap: 6px; 
            text-transform: uppercase; font-size: 0.8rem; letter-spacing: 0.5px;
        }
        .btn:hover { transform: translateY(-3px); box-shadow: 0 6px 15px rgba(0,0,0,0.15); }

        .section-title { 
            font-size: 1.15rem; font-weight: 800; color: var(--text-dark); margin-bottom: 1rem; 
            text-transform: uppercase; letter-spacing: 1px; position: relative; display: inline-block; 
        }
        .section-title::after { 
            content: ""; position: absolute; width: 50%; height: 3px; 
            background: var(--theme-gradient, linear-gradient(90deg, #0d6efd, #38bdf8));
            bottom: -6px; left: 25%; border-radius: 4px; 
        }

        /* Kotak Glassmorphism */
        .glass-intro {
            background: linear-gradient(to right, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.95));
            backdrop-filter: blur(15px); border: 1px solid rgba(226, 232, 240, 0.8);
            border-radius: 20px; box-shadow: 0 20px 50px rgba(0,0,0,0.08);
            transition: transform 0.3s ease; position: relative; z-index: 15; overflow: hidden;
        }
        .glass-intro::before {
            content: ""; position: absolute; top: 0; left: 0; width: 6px; height: 100%;
            background: var(--theme-gradient, linear-gradient(to bottom, #0d6efd, #38bdf8));
        }
        .glass-intro:hover { transform: translateY(-3px); box-shadow: 0 25px 60px rgba(0,0,0,0.12); }

        /* Highlight Box CTA in Intro */
        .highlight-cta {
            background: var(--theme-gradient, linear-gradient(135deg, #0d6efd 0%, #38bdf8 100%));
            color: white; border-radius: 12px; padding: 1.5rem;
            box-shadow: 0 10px 25px rgba(0,0,0,0.15); text-align: center; position: relative; overflow: hidden;
        }
        .highlight-cta h4 { font-weight: 800; letter-spacing: 0.5px; margin-bottom: 0.5rem; text-transform: uppercase; }

        /* Grid Cards Fitur */
        .feature-card {
            background: #ffffff; border: 1px solid #f1f5f9; border-radius: 16px;
            padding: 1.5rem; height: 100%; transition: all 0.4s ease;
            box-shadow: 0 4px 15px rgba(0,0,0,0.03); text-align: left;
            display: flex; flex-direction: column; position: relative; overflow: hidden;
        }
        .feature-card::before {
            content: ""; position: absolute; top: 0; left: 0; width: 100%; height: 4px;
            background: var(--theme-gradient, linear-gradient(90deg, #0d6efd, #38bdf8));
            transform: scaleX(0); transform-origin: left; transition: transform 0.4s ease;
        }
        .feature-card:hover { transform: translateY(-8px); box-shadow: 0 20px 40px rgba(0,0,0,0.08); }
        .feature-card:hover::before { transform: scaleX(1); }

        .feature-title { font-size: 1.15rem; font-weight: 800; color: var(--text-dark); margin-bottom: 1rem; margin-top: 1.5rem; display: flex; align-items: center; }
        .feature-title i { color: var(--theme-primary, #0d6efd); background: #eff6ff; padding: 10px; border-radius: 8px; margin-right: 10px; font-size: 1.2rem; }
        .feature-text { font-size: 0.95rem; color: var(--text-muted); line-height: 1.7; margin-bottom: 0; flex-grow: 1; text-align: justify; }

        /* Wrapper Video YouTube */
        .video-wrapper {
            border-radius: 12px; overflow: hidden; background-color: #cbd5e1;
            box-shadow: 0 4px 10px rgba(0,0,0,0.05); border: 1px solid #e2e8f0;
        }
        .video-wrapper iframe { transition: transform 0.3s ease; }
        .video-wrapper:hover iframe { transform: scale(1.02); }

        /* Hover Zoom Gambar */
        .hover-zoom { transition: transform 0.3s ease, box-shadow 0.3s ease; cursor: pointer; }
        .hover-zoom:hover { transform: scale(1.03); box-shadow: 0 10px 20px rgba(0,0,0,0.15) !important; }

        /* CTA Section Bottom */
        .cta-section {
            background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
            color: white; padding: 5rem 2rem; border-radius: 24px; margin-bottom: 5rem;
            box-shadow: 0 20px 50px rgba(0,0,0,0.2); position: relative; overflow: hidden;
        }
        .cta-section::after {
            content: ""; position: absolute; top: -50%; left: -50%; width: 200%; height: 200%;
            background: radial-gradient(circle, rgba(255,255,255,0.05) 0%, transparent 60%); pointer-events: none;
        }

        /* Contact Box */
        .contact-box {
            background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1);
            backdrop-filter: blur(10px); border-radius: 16px; padding: 2rem 1.5rem; text-align: center; height: 100%;
            transition: all 0.3s ease; display: flex; flex-direction: column; justify-content: flex-start; align-items: center;
        }
        .contact-box:hover { background: rgba(255,255,255,0.1); transform: translateY(-5px); }
        .contact-box h5 { color: var(--theme-text-on-primary, #38bdf8); font-weight: 700; margin-bottom: 0.5rem; font-size: 1.1rem; line-height: 1.4; }
        .contact-box .role-desc { color: #cbd5e1; font-size: 0.85rem; line-height: 1.5; text-align: center; margin-bottom: 1.5rem; flex-grow: 1; }
        .contact-box .wa-link-container { margin-top: auto; }

        /* Footer */
        .footer-custom { background-color: #0f172a; color: #94a3b8; padding-top: 5rem; padding-bottom: 2rem; margin-top: auto; }
        .footer-custom h5 { color: #e2e8f0; font-weight: 800; letter-spacing: 1px; margin-bottom: 1.5rem; }
        .footer-link { text-decoration: none; color: #94a3b8; transition: color 0.3s ease; }
        .footer-link:hover { color: var(--theme-primary, #38bdf8); }
        .store-icon { height: 32px; margin-right: 10px; transition: transform 0.3s ease; }
        .store-icon:hover { transform: scale(1.1); }
        .contact-icon { color: var(--theme-primary, #38bdf8); width: 24px; text-align: center; }


        /* =====================================================================================
           ENHANCED LANDING PAGE UI - Enterprise Education Premium Look
           ===================================================================================== */
        :root {
            --ee-primary: var(--theme-primary, var(--primary-color, #2563eb));
            --ee-primary-dark: #1e3a8a;
            --ee-accent: #38bdf8;
            --ee-gold: #f59e0b;
            --ee-success: #10b981;
            --ee-dark: #0f172a;
            --ee-soft: #f8fafc;
            --ee-border: rgba(148, 163, 184, 0.22);
            --ee-card-shadow: 0 20px 55px rgba(15, 23, 42, 0.10);
        }

        .hero-section-<%=rnd%> {
            overflow: hidden;
            padding: 3rem 0 9rem 0;
        }
        .hero-section-<%=rnd%>::after {
            content: "";
            position: absolute;
            width: 540px;
            height: 540px;
            right: -160px;
            top: -180px;
            background: radial-gradient(circle, rgba(56,189,248,0.40), rgba(37,99,235,0.18), transparent 68%);
            filter: blur(2px);
            z-index: -1;
        }
        .hero-orb-left {
            position: absolute;
            width: 360px;
            height: 360px;
            left: -140px;
            bottom: -160px;
            background: radial-gradient(circle, rgba(245,158,11,0.32), rgba(255,255,255,0.08), transparent 70%);
            pointer-events: none;
        }
        .hero-kicker {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 8px 16px;
            border-radius: 999px;
            border: 1px solid rgba(255,255,255,0.28);
            background: rgba(255,255,255,0.14);
            color: #e0f2fe;
            font-weight: 800;
            letter-spacing: 0.08em;
            text-transform: uppercase;
            font-size: 0.78rem;
            box-shadow: 0 12px 28px rgba(15,23,42,0.12);
            backdrop-filter: blur(12px);
        }
        .hero-title-modern {
            max-width: 980px;
            margin: 18px auto 0 auto;
            font-size: 3.55rem;
            line-height: 1.05;
            letter-spacing: -0.06em;
            font-weight: 900;
            color: #ffffff;
            text-shadow: 0 12px 28px rgba(15,23,42,0.28);
        }
        .hero-title-modern .text-gradient-soft {
            color: #fde68a;
        }
        .hero-subtitle-modern {
            max-width: 900px;
            margin: 22px auto 0 auto;
            color: rgba(255,255,255,0.84);
            font-size: 1.18rem;
            line-height: 1.85;
            font-weight: 400;
        }
        .hero-cta-group {
            display: flex;
            justify-content: center;
            flex-wrap: wrap;
            gap: 12px;
            margin-top: 28px;
        }
        .btn-hero-primary, .btn-hero-outline {
            min-width: 210px;
            border-radius: 999px !important;
            padding: 13px 24px !important;
            font-weight: 800 !important;
            letter-spacing: 0.04em;
            text-transform: uppercase;
        }
        .btn-hero-primary {
            background: #ffffff !important;
            color: var(--ee-primary-dark) !important;
            border: 1px solid rgba(255,255,255,0.90) !important;
            box-shadow: 0 16px 32px rgba(15,23,42,0.22) !important;
        }
        .btn-hero-outline {
            background: rgba(255,255,255,0.10) !important;
            color: #ffffff !important;
            border: 1px solid rgba(255,255,255,0.34) !important;
            backdrop-filter: blur(12px);
        }
        .hero-trust-grid {
            display: grid;
            grid-template-columns: repeat(4, minmax(0, 1fr));
            gap: 12px;
            max-width: 960px;
            margin: 34px auto 0 auto;
        }
        .hero-trust-item {
            background: rgba(255,255,255,0.13);
            border: 1px solid rgba(255,255,255,0.22);
            border-radius: 18px;
            padding: 15px 16px;
            color: #ffffff;
            text-align: left;
            backdrop-filter: blur(12px);
            box-shadow: 0 14px 30px rgba(15,23,42,0.13);
        }
        .hero-trust-item i {
            width: 34px;
            height: 34px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            border-radius: 10px;
            background: rgba(255,255,255,0.18);
            color: #fde68a;
            margin-bottom: 10px;
        }
        .hero-trust-item strong {
            display: block;
            font-size: 0.94rem;
            line-height: 1.35;
        }
        .hero-trust-item span {
            display: block;
            margin-top: 4px;
            color: rgba(255,255,255,0.76);
            font-size: 0.82rem;
            line-height: 1.45;
        }
        .client-pill-premium {
            display: inline-block;
            margin-top: 20px;
            padding: 10px 20px;
            border-radius: 999px;
            background: rgba(255,255,255,0.92);
            color: var(--ee-primary-dark);
            font-weight: 900;
            box-shadow: 0 14px 32px rgba(15,23,42,0.18);
        }
        .present-box-premium {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            margin-top: 12px;
            color: rgba(255,255,255,0.84);
            font-size: 0.96rem;
        }
        .section-eyebrow {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 7px 14px;
            border-radius: 999px;
            background: rgba(37,99,235,0.09);
            color: var(--ee-primary);
            font-size: 0.78rem;
            font-weight: 900;
            text-transform: uppercase;
            letter-spacing: 0.08em;
            margin-bottom: 12px;
        }
        .section-heading-premium {
            font-size: 2.35rem;
            line-height: 1.12;
            letter-spacing: -0.04em;
            font-weight: 900;
            color: #0f172a;
            margin-bottom: 14px;
        }
        .section-lead-premium {
            color: #64748b;
            font-size: 1.04rem;
            line-height: 1.85;
            max-width: 870px;
            margin-left: auto;
            margin-right: auto;
        }
        .premium-card {
            background: rgba(255,255,255,0.94);
            border: 1px solid var(--ee-border);
            border-radius: 24px;
            box-shadow: var(--ee-card-shadow);
            position: relative;
            overflow: hidden;
        }
        .premium-card::before {
            content: "";
            position: absolute;
            inset: 0;
            background: radial-gradient(circle at top right, rgba(56,189,248,0.14), transparent 42%);
            pointer-events: none;
        }
        .benefit-card {
            background: #ffffff;
            border: 1px solid rgba(226,232,240,0.92);
            border-radius: 22px;
            padding: 24px;
            height: 100%;
            box-shadow: 0 10px 30px rgba(15,23,42,0.055);
            transition: all 0.35s ease;
            text-align: left;
            position: relative;
            overflow: hidden;
        }
        .benefit-card:hover {
            transform: translateY(-7px);
            box-shadow: 0 22px 45px rgba(15,23,42,0.10);
            border-color: rgba(37,99,235,0.32);
        }
        .benefit-icon {
            width: 54px;
            height: 54px;
            border-radius: 16px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            color: #ffffff;
            background: linear-gradient(135deg, var(--ee-primary), var(--ee-accent));
            box-shadow: 0 12px 24px rgba(37,99,235,0.22);
            font-size: 1.25rem;
            margin-bottom: 18px;
        }
        .benefit-card h4 {
            font-size: 1.08rem;
            line-height: 1.35;
            font-weight: 900;
            color: #0f172a;
            margin-bottom: 10px;
        }
        .benefit-card p {
            color: #64748b;
            line-height: 1.75;
            font-size: 0.94rem;
            margin-bottom: 0;
            text-align: justify;
        }
        .pain-solution-row {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 18px;
        }
        .comparison-box {
            border-radius: 22px;
            padding: 24px;
            height: 100%;
            text-align: left;
        }
        .comparison-box.problem {
            background: linear-gradient(135deg, #fff7ed, #ffffff);
            border: 1px solid rgba(249,115,22,0.18);
        }
        .comparison-box.solution {
            background: linear-gradient(135deg, #ecfdf5, #ffffff);
            border: 1px solid rgba(16,185,129,0.20);
        }
        .comparison-box h4 {
            font-weight: 900;
            margin-bottom: 16px;
            color: #0f172a;
        }
        .check-list-modern, .x-list-modern {
            list-style: none;
            padding-left: 0;
            margin-bottom: 0;
        }
        .check-list-modern li, .x-list-modern li {
            display: flex;
            align-items: flex-start;
            gap: 10px;
            line-height: 1.65;
            margin-bottom: 12px;
            color: #475569;
            font-size: 0.95rem;
        }
        .check-list-modern li i {
            color: #10b981;
            margin-top: 4px;
        }
        .x-list-modern li i {
            color: #f97316;
            margin-top: 4px;
        }
        .roadmap-step {
            position: relative;
            padding: 22px;
            border-radius: 22px;
            background: #ffffff;
            border: 1px solid rgba(226,232,240,0.92);
            box-shadow: 0 10px 26px rgba(15,23,42,0.055);
            height: 100%;
            text-align: left;
        }
        .roadmap-number {
            width: 42px;
            height: 42px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            border-radius: 14px;
            color: #ffffff;
            background: linear-gradient(135deg, var(--ee-primary), var(--ee-accent));
            font-weight: 900;
            margin-bottom: 14px;
        }
        .roadmap-step h5 {
            font-weight: 900;
            color: #0f172a;
            margin-bottom: 9px;
        }
        .roadmap-step p {
            color: #64748b;
            line-height: 1.65;
            font-size: 0.93rem;
            margin-bottom: 0;
        }
        .platform-strip {
            display: grid;
            grid-template-columns: repeat(3, minmax(0, 1fr));
            gap: 14px;
            margin-top: 26px;
        }
        .platform-item {
            background: #ffffff;
            border: 1px solid rgba(226,232,240,0.88);
            border-radius: 20px;
            padding: 20px;
            text-align: left;
            box-shadow: 0 12px 30px rgba(15,23,42,0.055);
        }
        .platform-item i {
            color: var(--ee-primary);
            font-size: 1.4rem;
            margin-bottom: 12px;
        }
        .platform-item strong {
            display: block;
            color: #0f172a;
            font-weight: 900;
            margin-bottom: 6px;
        }
        .platform-item span {
            display: block;
            color: #64748b;
            font-size: 0.9rem;
            line-height: 1.55;
        }
        .sticky-cta-floating {
            position: fixed;
            right: 16px;
            /* Ditempatkan di atas tombol bantuan. Sejak bantuan_button.jsp memakai
               bentuk kebab, sudut kanan-bawah hanya terpakai SATU tombol bulat 48px
               pada bottom:16px, jadi tepi atasnya di 64px. 80px memberi jarak 16px
               di atas garis itu. Sebelumnya 122px, ketika masih ada tiga tombol pil
               bertumpuk -- dan pada praktiknya tetap bertindih dengan tombol paling
               atas ("Semua Panduan", bottom:108px, tepi atas ~141px). */
            bottom: 80px;
            z-index: 1060;
            display: flex;
            align-items: center;
            gap: 10px;
            border-radius: 999px;
            padding: 12px 18px;
            color: #ffffff !important;
            text-decoration: none;
            background: linear-gradient(135deg, #16a34a, #22c55e);
            box-shadow: 0 18px 35px rgba(22,163,74,0.32);
            font-weight: 900;
            letter-spacing: 0.02em;
        }
        .sticky-cta-floating:hover {
            transform: translateY(-3px);
            color: #ffffff !important;
        }
        .menu-panel {
            border-radius: 28px;
            box-shadow: 0 28px 70px rgba(15,23,42,0.12);
        }
        .btn-custom-group .btn {
            border-radius: 999px;
        }

        .portal-group-section {
            position: relative;
            padding: 1.18rem 1.05rem 1.05rem;
            border-radius: 24px;
            background:
                radial-gradient(circle at top right, rgba(59,130,246,0.08), transparent 34%),
                linear-gradient(135deg, rgba(255,255,255,0.82), rgba(248,250,252,0.94));
            border: 1px solid rgba(226,232,240,0.92);
            box-shadow: 0 16px 38px rgba(15,23,42,0.06);
            transition: transform .25s ease, box-shadow .25s ease, border-color .25s ease;
        }
        .portal-group-section:hover {
            transform: translateY(-2px);
            box-shadow: 0 22px 50px rgba(15,23,42,0.09);
            border-color: rgba(191,219,254,0.98);
        }
        .portal-group-section .section-title {
            margin-bottom: .9rem;
        }
        .portal-group-section + hr {
            margin-top: 1.45rem !important;
            margin-bottom: 1.45rem !important;
        }
        .portal-group-section.group-campus .portal-support-copy i {
            background: linear-gradient(135deg, #1d4ed8, #38bdf8);
        }
        .portal-group-section.group-school .portal-support-copy i {
            background: linear-gradient(135deg, #111827, #64748b);
            box-shadow: 0 14px 26px rgba(17,24,39,0.22);
        }
        .portal-group-section.group-docs .portal-support-copy i {
            background: linear-gradient(135deg, #b91c1c, #f97316);
            box-shadow: 0 14px 26px rgba(185,28,28,0.22);
        }
        .portal-group-section.group-support .portal-support-copy i {
            background: linear-gradient(135deg, #0f766e, #2563eb);
            box-shadow: 0 14px 26px rgba(37,99,235,0.22);
        }
        .portal-mini-label {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            margin: .1rem auto .55rem;
            padding: 6px 11px;
            border-radius: 999px;
            font-size: .72rem;
            font-weight: 900;
            letter-spacing: .05em;
            text-transform: uppercase;
            color: #475569;
            background: #ffffff;
            border: 1px solid rgba(226,232,240,.95);
            box-shadow: 0 8px 20px rgba(15,23,42,.05);
        }
        .portal-action-grid .btn-danger,
        .portal-action-grid .btn-dark,
        .portal-action-grid .btn-primary,
        .portal-action-grid .btn-success {
            color: #ffffff !important;
        }
        .portal-action-grid .btn-outline-dark:hover,
        .portal-action-grid .btn-outline-success:hover,
        .portal-action-grid .btn-outline-primary:hover {
            color: #ffffff !important;
        }

        .portal-support-copy {
            position: relative;
            overflow: hidden;
            display: flex;
            align-items: flex-start;
            gap: 12px;
            padding: 13px 16px;
            border-radius: 20px;
            background:
                radial-gradient(circle at top left, rgba(37, 99, 235, 0.12), transparent 38%),
                linear-gradient(135deg, rgba(255,255,255,0.96), rgba(248,250,252,0.98));
            border: 1px solid rgba(226, 232, 240, 0.96);
            box-shadow: 0 18px 38px rgba(15, 23, 42, 0.08);
            max-width: 940px;
            margin: 0 auto .95rem;
            text-align: left;
        }
        .portal-support-copy i {
            flex: 0 0 auto;
            width: 38px;
            height: 38px;
            border-radius: 14px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            color: #ffffff;
            background: var(--theme-gradient, linear-gradient(135deg, #0d6efd, #2563eb));
            box-shadow: 0 14px 26px rgba(37, 99, 235, 0.28);
        }
        .portal-support-copy strong {
            display: block;
            color: #0f172a;
            font-weight: 900;
            margin-bottom: 4px;
        }
        .portal-support-copy span {
            display: block;
            color: #64748b;
            font-size: 0.88rem;
            line-height: 1.58;
        }
        .portal-action-grid {
            align-items: stretch;
            gap: 9px;
        }
        .portal-action-grid .btn {
            position: relative;
            overflow: hidden;
            flex: 1 1 170px;
            min-height: 56px;
            justify-content: flex-start;
            text-align: left;
            padding: 10px 12px;
            border-radius: 16px !important;
            text-transform: none;
            letter-spacing: 0.005em;
            font-size: 0.84rem;
            line-height: 1.18;
            box-shadow: 0 10px 22px rgba(15, 23, 42, 0.09) !important;
        }
        .portal-action-grid .btn::after {
            content: "";
            position: absolute;
            top: -45%;
            right: -22%;
            width: 70px;
            height: 70px;
            border-radius: 999px;
            background: rgba(255,255,255,0.17);
            transition: transform .28s ease, opacity .28s ease;
        }
        .portal-action-grid .btn:hover::after {
            transform: scale(1.28);
            opacity: .9;
        }
        .portal-action-grid .btn i {
            flex: 0 0 auto;
            width: 30px;
            height: 30px;
            border-radius: 12px;
            font-size: .82rem;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            background: rgba(255,255,255,0.20);
            box-shadow: inset 0 0 0 1px rgba(255,255,255,0.16);
        }
        .portal-action-grid .btn-outline-primary i,
        .portal-action-grid .btn-outline-success i,
        .portal-action-grid .btn-outline-dark i {
            background: rgba(37, 99, 235, 0.08);
            color: inherit;
        }
        .portal-btn-featured {
            border: 0 !important;
            background: linear-gradient(135deg, #0f172a 0%, #1d4ed8 52%, #06b6d4 100%) !important;
            color: #ffffff !important;
        }
        .portal-btn-featured:hover {
            color: #ffffff !important;
            box-shadow: 0 18px 40px rgba(37, 99, 235, 0.28) !important;
        }
        .portal-support-note {
            max-width: 1040px;
            margin: .85rem auto 0;
            padding: 10px 14px;
            border-radius: 16px;
            color: #475569;
            background: linear-gradient(135deg, rgba(239,246,255,0.96), rgba(240,253,250,0.92));
            border: 1px solid rgba(191,219,254,0.92);
            font-size: 0.86rem;
            line-height: 1.55;
        }
        .portal-support-note i {
            color: var(--theme-primary, #0d6efd);
        }
        @media (min-width: 992px) {
            .portal-action-grid .btn {
                max-width: 210px;
            }
            .portal-group-section.group-docs .portal-action-grid .btn {
                max-width: 230px;
            }
        }
        @media (min-width: 1200px) {
            .portal-action-grid .btn {
                flex-basis: 176px;
            }
        }
        @media (max-width: 575.98px) {
            .portal-support-copy {
                flex-direction: column;
                text-align: center;
                align-items: center;
            }
            .portal-action-grid .btn {
                flex-basis: 100%;
                justify-content: center;
                text-align: center;
            }
        }
        .feature-card {
            border-radius: 24px;
        }
        .feature-card .video-wrapper {
            border-radius: 18px;
        }
        .cta-section {
            border-radius: 34px;
            background: radial-gradient(circle at top left, rgba(56,189,248,0.22), transparent 32%), linear-gradient(135deg, #0f172a 0%, #172554 52%, #0f172a 100%);
        }


        /* Enhanced Formal Landing Sections - Integrasi Proposal, Penawaran, Presentasi, dan PKS */
        .enterprise-ribbon-v2 {
            display: inline-flex;
            align-items: center;
            gap: 10px;
            padding: 8px 16px;
            border-radius: 999px;
            background: linear-gradient(135deg, rgba(13,110,253,0.10), rgba(56,189,248,0.14));
            color: var(--theme-primary, #0d6efd);
            font-weight: 900;
            font-size: 0.78rem;
            text-transform: uppercase;
            letter-spacing: 0.08rem;
            border: 1px solid rgba(37, 99, 235, 0.16);
            margin-bottom: 1rem;
        }
        .executive-brief-card {
            position: relative;
            overflow: hidden;
            background:
                radial-gradient(circle at top right, rgba(56,189,248,0.16), transparent 34%),
                linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
            border: 1px solid rgba(226, 232, 240, 0.95);
            border-radius: 24px;
            padding: 1.65rem;
            height: 100%;
            text-align: left;
            box-shadow: 0 16px 38px rgba(15, 23, 42, 0.055);
            transition: all .35s ease;
        }
        .executive-brief-card:hover {
            transform: translateY(-6px);
            box-shadow: 0 24px 52px rgba(15, 23, 42, 0.10);
            border-color: rgba(37, 99, 235, 0.22);
        }
        .executive-icon {
            width: 52px;
            height: 52px;
            border-radius: 18px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            color: #ffffff;
            background: var(--theme-gradient, linear-gradient(135deg, #0d6efd, #38bdf8));
            box-shadow: 0 12px 28px rgba(37, 99, 235, 0.24);
            font-size: 1.3rem;
            margin-bottom: 1.1rem;
        }
        .executive-brief-card h4,
        .partnership-card h4,
        .investment-card h4,
        .document-action-card h4 {
            font-size: 1.05rem;
            font-weight: 900;
            color: #0f172a;
            margin-bottom: .75rem;
            line-height: 1.38;
        }
        .executive-brief-card p,
        .partnership-card p,
        .investment-card p,
        .document-action-card p {
            color: #64748b;
            font-size: .96rem;
            line-height: 1.75;
            margin-bottom: 0;
            text-align: justify;
        }
        .formal-note-box {
            border-radius: 24px;
            padding: 1.4rem 1.6rem;
            background: linear-gradient(135deg, rgba(15,23,42,0.96), rgba(30,64,175,0.93));
            color: #ffffff;
            box-shadow: 0 18px 46px rgba(15, 23, 42, 0.18);
            position: relative;
            overflow: hidden;
            text-align: left;
        }
        .formal-note-box::after {
            content: "";
            position: absolute;
            right: -42px;
            bottom: -42px;
            width: 150px;
            height: 150px;
            border-radius: 999px;
            background: rgba(255,255,255,0.08);
        }
        .formal-note-box .note-title {
            font-size: 1.12rem;
            font-weight: 900;
            margin-bottom: .55rem;
        }
        .formal-note-box .note-text {
            color: #dbeafe;
            line-height: 1.75;
            margin-bottom: 0;
            position: relative;
            z-index: 2;
        }
        .deliverable-strip {
            display: grid;
            grid-template-columns: repeat(4, minmax(0, 1fr));
            gap: 12px;
            margin-top: 1.4rem;
        }
        .deliverable-item {
            border-radius: 18px;
            padding: 1rem;
            background: rgba(255,255,255,0.92);
            border: 1px solid rgba(226,232,240,0.95);
            text-align: left;
            box-shadow: 0 10px 26px rgba(15,23,42,0.04);
        }
        .deliverable-item i {
            color: var(--theme-primary, #0d6efd);
            margin-right: 8px;
        }
        .deliverable-item strong {
            display: block;
            color: #0f172a;
            font-size: .92rem;
            margin-bottom: .25rem;
        }
        .deliverable-item span {
            display: block;
            color: #64748b;
            font-size: .84rem;
            line-height: 1.55;
        }
        .partnership-grid {
            display: grid;
            grid-template-columns: repeat(3, minmax(0, 1fr));
            gap: 18px;
        }
        .partnership-card {
            background: #ffffff;
            border: 1px solid rgba(226,232,240,0.95);
            border-radius: 22px;
            padding: 1.45rem;
            height: 100%;
            text-align: left;
            box-shadow: 0 14px 34px rgba(15,23,42,0.05);
            transition: all .35s ease;
        }
        .partnership-card:hover { transform: translateY(-5px); box-shadow: 0 22px 48px rgba(15,23,42,0.09); }
        .partnership-card .badge-soft {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 7px 11px;
            border-radius: 999px;
            background: #eff6ff;
            color: #1d4ed8;
            font-size: .74rem;
            font-weight: 900;
            text-transform: uppercase;
            letter-spacing: .06rem;
            margin-bottom: .95rem;
        }
        .success-table-modern {
            width: 100%;
            border-collapse: separate;
            border-spacing: 0;
            overflow: hidden;
            border-radius: 18px;
            border: 1px solid rgba(226,232,240,0.95);
            background: #ffffff;
            box-shadow: 0 14px 34px rgba(15,23,42,0.04);
            text-align: left;
        }
        .success-table-modern th {
            background: linear-gradient(135deg, #0f172a, #1d4ed8);
            color: #ffffff;
            padding: 14px 16px;
            font-size: .86rem;
            text-transform: uppercase;
            letter-spacing: .06rem;
        }
        .success-table-modern td {
            padding: 14px 16px;
            color: #475569;
            border-top: 1px solid #e2e8f0;
            vertical-align: top;
            line-height: 1.65;
            font-size: .94rem;
        }
        .success-table-modern td:first-child {
            color: #0f172a;
            font-weight: 900;
            width: 30%;
        }
        .investment-card {
            border-radius: 24px;
            padding: 1.55rem;
            background: #ffffff;
            border: 1px solid rgba(226,232,240,0.95);
            height: 100%;
            text-align: left;
            box-shadow: 0 16px 38px rgba(15,23,42,0.055);
            position: relative;
            overflow: hidden;
        }
        .investment-card::before {
            content: "";
            position: absolute;
            inset: 0 0 auto 0;
            height: 5px;
            background: var(--theme-gradient, linear-gradient(90deg, #0d6efd, #38bdf8));
        }
        .investment-tag {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 7px 12px;
            border-radius: 999px;
            background: #f8fafc;
            border: 1px solid #e2e8f0;
            color: #334155;
            font-size: .78rem;
            font-weight: 900;
            text-transform: uppercase;
            letter-spacing: .05rem;
            margin-bottom: .9rem;
        }
        .investment-list {
            list-style: none;
            padding: 0;
            margin: 1rem 0 0;
        }
        .investment-list li {
            display: flex;
            gap: 10px;
            color: #475569;
            font-size: .93rem;
            line-height: 1.62;
            padding: .42rem 0;
            border-top: 1px dashed #e2e8f0;
        }
        .investment-list li:first-child { border-top: 0; }
        .investment-list i { color: #16a34a; margin-top: 4px; }
        .document-center-panel {
            background:
                radial-gradient(circle at top left, rgba(59,130,246,0.13), transparent 34%),
                radial-gradient(circle at bottom right, rgba(245,158,11,0.13), transparent 34%),
                #ffffff;
            border: 1px solid rgba(226,232,240,0.95);
            border-radius: 28px;
            padding: 2rem;
            box-shadow: 0 22px 56px rgba(15,23,42,0.08);
        }
        .document-action-card {
            background: rgba(255,255,255,0.96);
            border: 1px solid rgba(226,232,240,0.95);
            border-radius: 22px;
            padding: 1.4rem;
            height: 100%;
            text-align: left;
            box-shadow: 0 14px 32px rgba(15,23,42,0.05);
        }
        .document-action-card .doc-icon {
            width: 46px;
            height: 46px;
            border-radius: 16px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            color: #ffffff;
            background: linear-gradient(135deg, #dc2626, #f97316);
            box-shadow: 0 12px 26px rgba(220,38,38,.18);
            margin-bottom: 1rem;
        }
        .document-action-card .btn {
            margin-top: 1.1rem;
            width: 100%;
            justify-content: center;
        }
        .legal-footnote {
            border-radius: 18px;
            background: #fffbeb;
            color: #92400e;
            border: 1px solid #fde68a;
            padding: 1rem 1.2rem;
            text-align: left;
            line-height: 1.65;
            font-size: .92rem;
        }
        @media (max-width: 991.98px) {
            .deliverable-strip,
            .partnership-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
        }
        @media (max-width: 575.98px) {
            .deliverable-strip,
            .partnership-grid { grid-template-columns: 1fr; }
            .success-table-modern { font-size: .86rem; }
            .success-table-modern th, .success-table-modern td { padding: 12px; }
        }

        @media (max-width: 991.98px) {
            .hero-title-modern { font-size: 2.35rem; }
            .hero-subtitle-modern { font-size: 1.02rem; }
            .hero-trust-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
            .pain-solution-row { grid-template-columns: 1fr; }
            .platform-strip { grid-template-columns: 1fr; }
            .sticky-cta-floating span { display: none; }
            .sticky-cta-floating { padding: 14px 16px; }
        }
        @media (max-width: 575.98px) {
            .hero-title-modern { font-size: 2rem; }
            .hero-trust-grid { grid-template-columns: 1fr; }
            .section-heading-premium { font-size: 1.75rem; }
        }


        /* EXTRA COMPACT PORTAL BUTTONS - membuat tombol portal lebih ringan, rapi, dan tidak terlalu mendominasi halaman */
        .portal-group-section {
            padding: 1rem .95rem .95rem;
        }
        .portal-action-grid {
            gap: 7px;
            justify-content: center;
        }
        .portal-action-grid .btn {
            flex: 0 1 146px;
            max-width: 164px;
            min-height: 44px;
            padding: 7px 10px;
            border-radius: 13px !important;
            font-size: 0.75rem;
            line-height: 1.12;
            gap: 7px;
            box-shadow: 0 7px 16px rgba(15, 23, 42, 0.075) !important;
        }
        .portal-action-grid .btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 11px 22px rgba(15, 23, 42, 0.11) !important;
        }
        .portal-action-grid .btn i {
            width: 24px;
            height: 24px;
            border-radius: 9px;
            font-size: .68rem;
        }
        .portal-action-grid .btn::after {
            width: 52px;
            height: 52px;
            top: -42%;
            right: -24%;
            opacity: .72;
        }
        .portal-mini-label {
            padding: 5px 10px;
            font-size: .68rem;
            margin-bottom: .45rem;
        }
        .portal-support-note {
            margin-top: .65rem;
            padding: 8px 12px;
            font-size: .8rem;
            border-radius: 14px;
        }
        @media (min-width: 992px) {
            .portal-action-grid .btn {
                max-width: 164px;
            }
            .portal-group-section.group-docs .portal-action-grid .btn {
                max-width: 176px;
            }
        }
        @media (min-width: 1200px) {
            .portal-action-grid .btn {
                flex-basis: 148px;
            }
        }
        @media (max-width: 575.98px) {
            .portal-action-grid .btn {
                flex: 1 1 calc(50% - 8px);
                max-width: none;
                min-height: 46px;
                justify-content: center;
                text-align: center;
                font-size: .74rem;
                padding: 8px 8px;
            }
            .portal-action-grid .btn i {
                width: 23px;
                height: 23px;
            }
        }


        /* HOVER CONTRAST FIX - menjaga teks tombol portal tetap terbaca saat mouse diarahkan */
        .portal-action-grid .btn,
        .portal-action-grid .btn:hover,
        .portal-action-grid .btn:focus,
        .portal-action-grid .btn:active {
            text-decoration: none !important;
            -webkit-text-fill-color: currentColor;
        }
        .portal-action-grid .btn.btn-primary,
        .portal-action-grid .btn.btn-success,
        .portal-action-grid .btn.btn-dark,
        .portal-action-grid .btn.btn-danger,
        .portal-action-grid .btn.portal-btn-featured {
            color: #ffffff !important;
            border-color: transparent !important;
        }
        .portal-action-grid .btn.btn-primary:hover,
        .portal-action-grid .btn.btn-primary:focus,
        .portal-action-grid .btn.btn-primary:active {
            color: #ffffff !important;
            background: linear-gradient(135deg, #123f91 0%, #1d4ed8 48%, #0ea5e9 100%) !important;
            border-color: rgba(255,255,255,0.18) !important;
        }
        .portal-action-grid .btn.btn-success:hover,
        .portal-action-grid .btn.btn-success:focus,
        .portal-action-grid .btn.btn-success:active {
            color: #ffffff !important;
            background: linear-gradient(135deg, #047857 0%, #16a34a 55%, #22c55e 100%) !important;
            border-color: rgba(255,255,255,0.18) !important;
        }
        .portal-action-grid .btn.btn-dark:hover,
        .portal-action-grid .btn.btn-dark:focus,
        .portal-action-grid .btn.btn-dark:active {
            color: #ffffff !important;
            background: linear-gradient(135deg, #020617 0%, #1e293b 55%, #334155 100%) !important;
            border-color: rgba(255,255,255,0.18) !important;
        }
        .portal-action-grid .btn.btn-danger:hover,
        .portal-action-grid .btn.btn-danger:focus,
        .portal-action-grid .btn.btn-danger:active {
            color: #ffffff !important;
            background: linear-gradient(135deg, #991b1b 0%, #dc2626 55%, #f97316 100%) !important;
            border-color: rgba(255,255,255,0.18) !important;
        }
        .portal-action-grid .btn.portal-btn-featured:hover,
        .portal-action-grid .btn.portal-btn-featured:focus,
        .portal-action-grid .btn.portal-btn-featured:active {
            color: #ffffff !important;
            background: linear-gradient(135deg, #0f172a 0%, #1d4ed8 52%, #06b6d4 100%) !important;
            border-color: rgba(255,255,255,0.18) !important;
        }
        .portal-action-grid .btn.btn-outline-primary {
            color: #1d4ed8 !important;
            background-color: #ffffff !important;
            border-color: rgba(37, 99, 235, 0.38) !important;
        }
        .portal-action-grid .btn.btn-outline-success {
            color: #047857 !important;
            background-color: #ffffff !important;
            border-color: rgba(16, 185, 129, 0.42) !important;
        }
        .portal-action-grid .btn.btn-outline-dark {
            color: #111827 !important;
            background-color: #ffffff !important;
            border-color: rgba(15, 23, 42, 0.34) !important;
        }
        .portal-action-grid .btn.btn-outline-primary:hover,
        .portal-action-grid .btn.btn-outline-primary:focus,
        .portal-action-grid .btn.btn-outline-primary:active {
            color: #ffffff !important;
            background: linear-gradient(135deg, #1d4ed8 0%, #2563eb 58%, #38bdf8 100%) !important;
            border-color: transparent !important;
        }
        .portal-action-grid .btn.btn-outline-success:hover,
        .portal-action-grid .btn.btn-outline-success:focus,
        .portal-action-grid .btn.btn-outline-success:active {
            color: #ffffff !important;
            background: linear-gradient(135deg, #047857 0%, #16a34a 58%, #34d399 100%) !important;
            border-color: transparent !important;
        }
        .portal-action-grid .btn.btn-outline-dark:hover,
        .portal-action-grid .btn.btn-outline-dark:focus,
        .portal-action-grid .btn.btn-outline-dark:active {
            color: #ffffff !important;
            background: linear-gradient(135deg, #020617 0%, #111827 58%, #475569 100%) !important;
            border-color: transparent !important;
        }
        .portal-action-grid .btn:hover i,
        .portal-action-grid .btn:focus i,
        .portal-action-grid .btn:active i {
            color: #ffffff !important;
            background: rgba(255,255,255,0.24) !important;
            box-shadow: inset 0 0 0 1px rgba(255,255,255,0.22), 0 6px 12px rgba(15,23,42,0.12) !important;
        }
        .portal-action-grid .btn:hover::after,
        .portal-action-grid .btn:focus::after,
        .portal-action-grid .btn:active::after {
            opacity: .95;
        }

        /* ===== Narasi Produk (eCampus / eSchool / eMedic / ePesantren) ===== */
        .ee-prod-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(285px, 1fr));
            gap: 1.4rem;
            margin-top: 1.9rem;
        }
        .ee-prod-card {
            background: #ffffff;
            border: 1px solid rgba(226,232,240,0.92);
            border-radius: 22px;
            padding: 26px 24px 24px;
            height: 100%;
            box-shadow: 0 10px 30px rgba(15,23,42,0.055);
            transition: all 0.35s ease;
            position: relative;
            overflow: hidden;
        }
        .ee-prod-card:hover {
            transform: translateY(-7px);
            box-shadow: 0 22px 45px rgba(15,23,42,0.10);
            border-color: rgba(37,99,235,0.32);
        }
        .ee-prod-badge {
            width: 54px; height: 54px;
            border-radius: 16px;
            display: inline-flex; align-items: center; justify-content: center;
            color: #ffffff; font-size: 1.25rem;
            box-shadow: 0 12px 24px rgba(15,23,42,0.16);
            margin-bottom: 16px;
        }
        .ee-prod-card h4 {
            font-weight: 800; font-size: 1.22rem;
            color: var(--ee-dark); margin-bottom: 2px;
        }
        .ee-prod-for {
            display: block; font-size: 0.8rem; font-weight: 700;
            letter-spacing: 0.03em; text-transform: uppercase; margin-bottom: 12px;
        }
        .ee-prod-card p {
            font-size: 0.94rem; line-height: 1.78;
            color: #475569; margin-bottom: 14px;
        }
        .ee-prod-chips { display: flex; flex-wrap: wrap; gap: 6px; }
        .ee-prod-chips span {
            font-size: 0.74rem; font-weight: 600; color: #334155;
            background: #f1f5f9; border: 1px solid rgba(226,232,240,0.95);
            border-radius: 999px; padding: 3px 10px;
        }
        .ee-prod-foot {
            margin-top: 1.9rem; padding: 18px 20px; border-radius: 18px;
            background: rgba(37,99,235,0.06);
            border: 1px solid rgba(37,99,235,0.16);
            font-size: 0.95rem; line-height: 1.75; color: #1e3a8a;
        }

        /* ===== Kecerdasan Buatan ===== */
        .ee-ai-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(258px, 1fr));
            gap: 1.25rem;
            margin-top: 1.9rem;
        }
        .ee-ai-pillar {
            background: #ffffff;
            border: 1px solid rgba(226,232,240,0.92);
            border-radius: 22px;
            padding: 24px;
            height: 100%;
            box-shadow: 0 10px 30px rgba(15,23,42,0.055);
            transition: all 0.35s ease;
        }
        .ee-ai-pillar:hover {
            transform: translateY(-7px);
            box-shadow: 0 22px 45px rgba(15,23,42,0.10);
            border-color: rgba(109,40,217,0.32);
        }
        .ee-ai-num {
            width: 40px; height: 40px; border-radius: 13px;
            display: inline-flex; align-items: center; justify-content: center;
            color: #ffffff; font-weight: 800; font-size: 0.98rem;
            background: linear-gradient(135deg, var(--ee-primary), #6d28d9);
            box-shadow: 0 12px 24px rgba(109,40,217,0.22);
            margin-bottom: 14px;
        }
        .ee-ai-pillar h5 {
            font-weight: 800; font-size: 1.06rem;
            color: var(--ee-dark); margin-bottom: 8px;
        }
        .ee-ai-pillar p {
            font-size: 0.92rem; line-height: 1.75; color: #475569; margin: 0;
        }
        .ee-ai-engine {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 1.25rem; margin-top: 1.5rem;
        }
        .ee-ai-box {
            border: 1px solid rgba(226,232,240,0.92);
            border-radius: 20px; padding: 22px; background: #ffffff;
        }
        .ee-ai-box.is-primary {
            border-color: rgba(16,185,129,0.42);
            background: linear-gradient(180deg, rgba(16,185,129,0.07), #ffffff);
        }
        .ee-ai-box h6 {
            font-weight: 800; font-size: 1.02rem;
            color: var(--ee-dark); margin-bottom: 8px;
        }
        .ee-ai-box p { font-size: 0.9rem; line-height: 1.75; color: #475569; margin: 0; }
        .ee-ai-note {
            margin-top: 1.5rem; padding: 18px 20px; border-radius: 18px;
            background: rgba(15,23,42,0.045);
            border: 1px solid rgba(148,163,184,0.28);
            font-size: 0.94rem; line-height: 1.78; color: #334155;
        }

        /* ===== Satuan Pengawasan Internal ===== */
        .ee-new-pill {
            display: inline-flex; align-items: center; gap: 6px;
            font-size: 0.7rem; font-weight: 800; letter-spacing: 0.08em;
            text-transform: uppercase; color: #ffffff;
            background: linear-gradient(135deg, #f59e0b, #d97706);
            border-radius: 999px; padding: 4px 12px;
            box-shadow: 0 8px 18px rgba(217,119,6,0.28);
            margin-bottom: 14px;
        }
        .ee-spi-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(258px, 1fr));
            gap: 1.25rem; margin-top: 1.9rem;
        }
        .ee-spi-card {
            background: #ffffff;
            border: 1px solid rgba(226,232,240,0.92);
            border-radius: 22px;
            padding: 24px; height: 100%;
            box-shadow: 0 10px 30px rgba(15,23,42,0.055);
            transition: all 0.35s ease;
        }
        .ee-spi-card:hover {
            transform: translateY(-7px);
            box-shadow: 0 22px 45px rgba(15,23,42,0.10);
            border-color: rgba(15,118,110,0.32);
        }
        .ee-spi-ic {
            width: 48px; height: 48px; border-radius: 15px;
            display: inline-flex; align-items: center; justify-content: center;
            color: #ffffff; font-size: 1.12rem;
            background: linear-gradient(135deg, #0f766e, #115e59);
            box-shadow: 0 12px 24px rgba(15,118,110,0.24);
            margin-bottom: 14px;
        }
        .ee-spi-card h5 {
            font-weight: 800; font-size: 1.05rem;
            color: var(--ee-dark); margin-bottom: 8px;
        }
        .ee-spi-card p { font-size: 0.92rem; line-height: 1.75; color: #475569; margin: 0; }
        .ee-spi-verticals {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 1rem; margin-top: 1.6rem;
        }
        .ee-spi-vert {
            background: rgba(15,118,110,0.055);
            border: 1px solid rgba(15,118,110,0.16);
            border-radius: 18px; padding: 18px 20px;
        }
        .ee-spi-vert strong {
            display: block; font-size: 0.98rem; font-weight: 800;
            color: #115e59; margin-bottom: 6px;
        }
        .ee-spi-vert span { font-size: 0.88rem; line-height: 1.72; color: #475569; }
        .ee-spi-note {
            margin-top: 1.5rem; padding: 18px 20px; border-radius: 18px;
            background: rgba(109,40,217,0.06);
            border: 1px solid rgba(109,40,217,0.18);
            font-size: 0.94rem; line-height: 1.78; color: #4c1d95;
        }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/baru/include/pemilih_bahasa.jsp" />

    <div class="page-bg-<%=rnd%>"></div>

    <header class="hero-section-<%=rnd%> text-center">
        <div class="hero-orb-left"></div>
        <div class="container position-relative z-2">
            <img 
                src="<%=logo_PerguruanTinggi%>" 
                alt="<%= Common.getBahasaConfig("Logo Institusi Pendidikan") %>" 
                class="logo-img"
            />
            <div class="hero-kicker">
                <i class="fas fa-star"></i>
                <span><%= Common.getBahasaConfig("Enterprise Education Ecosystem") %></span>
            </div>
            <h1 class="hero-title-modern">
                <%= Common.getBahasaConfig("Satu Platform ERP untuk Menggerakkan Institusi Pendidikan") %>
                <span class="text-gradient-soft"><%= Common.getBahasaConfig("Lebih Cepat, Tertib, dan Terukur") %></span>
            </h1>
            <p class="hero-subtitle-modern">
                <%= Common.getBahasaConfig("Modernisasi tata kelola kampus, sekolah, pesantren, yayasan, unit usaha, akademik, keuangan, SDM, aset, pembayaran digital, dashboard pimpinan, hingga layanan mobile dalam satu ekosistem yang saling terhubung dan siap dikembangkan sesuai kebutuhan institusi.") %>
            </p>

            <div class="hero-cta-group">
                <a href="#section-portal" class="btn btn-hero-primary">
                    <i class="fas fa-play-circle me-2"></i><%= Common.getBahasaConfig("Lihat Portal Demo") %>
                </a>
                <a href="#section-contact" class="btn btn-hero-outline">
                    <i class="fas fa-headset me-2"></i><%= Common.getBahasaConfig("Konsultasi Implementasi") %>
                </a>
                <a href="<%=Common.ROOT%>/proposal<%=appendParams%>" target="_blank" class="btn btn-hero-outline">
                    <i class="fas fa-file-alt me-2"></i><%= Common.getBahasaConfig("Unduh Proposal") %>
                </a>
                <a href="#section-documents" class="btn btn-hero-outline">
                    <i class="fas fa-folder-open me-2"></i><%= Common.getBahasaConfig("Dokumen Resmi") %>
                </a>
            </div>
            
            <div class="client-pill-premium">
                <i class="fas fa-university me-2"></i><%=judul%>
            </div>
            
            <% if(paramClient != null && !paramClient.trim().isEmpty()) { %>
                <div class="mt-3 fs-5 text-white fw-medium">
                    <%= Common.getBahasaConfig("Dipersembahkan eksklusif untuk:") %> <br>
                    <span class="text-warning fs-4 fw-bold"><%=paramClient%></span>
                </div>
            <% } %>
            
            <% if(paramPresentBy != null && !paramPresentBy.trim().isEmpty()) { %>
                <div class="present-box-premium">
                    <i class="fas fa-user-tie"></i>
                    <span><%= Common.getBahasaConfig("Dipresentasikan oleh:") %> <strong class="text-white"><%=paramPresentBy%></strong></span>
                </div>
            <% } %>

            <div class="hero-trust-grid">
                <div class="hero-trust-item">
                    <i class="fas fa-layer-group"></i>
                    <strong><%= Common.getBahasaConfig("All-in-One Platform") %></strong>
                    <span><%= Common.getBahasaConfig("Seluruh layanan inti institusi berada dalam satu ekosistem data terpadu.") %></span>
                </div>
                <div class="hero-trust-item">
                    <i class="fas fa-chart-pie"></i>
                    <strong><%= Common.getBahasaConfig("Dashboard Eksekutif") %></strong>
                    <span><%= Common.getBahasaConfig("Pimpinan dapat membaca kinerja akademik, keuangan, dan operasional secara ringkas.") %></span>
                </div>
                <div class="hero-trust-item">
                    <i class="fas fa-shield-alt"></i>
                    <strong><%= Common.getBahasaConfig("Data Lebih Tertib") %></strong>
                    <span><%= Common.getBahasaConfig("Mengurangi input berulang, memperkuat validasi, dan menata alur persetujuan.") %></span>
                </div>
                <div class="hero-trust-item">
                    <i class="fas fa-mobile-alt"></i>
                    <strong><%= Common.getBahasaConfig("Web & Mobile Ready") %></strong>
                    <span><%= Common.getBahasaConfig("Mendukung akses layanan oleh pimpinan, operator, guru/dosen, siswa/mahasiswa, dan wali.") %></span>
                </div>
            </div>
            
        </div>
    </header>

    <main class="container text-center" style="margin-top: -6rem; position: relative; z-index: 10;">
        
        <div class="floating-nav-wrapper d-none d-lg-flex">
            <nav class="navbar navbar-expand-lg floating-nav">
                <ul class="navbar-nav landing-nav-list">
                    <li class="nav-item">
                        <a class="nav-link custom-nav-link" href="#section-intro"><i class="fas fa-home me-1"></i> <%= Common.getBahasaConfig("Beranda") %></a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link custom-nav-link" href="#section-executive"><i class="fas fa-clipboard-check me-1"></i> <%= Common.getBahasaConfig("Ringkasan") %></a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link custom-nav-link" href="#section-portal"><i class="fas fa-th me-1"></i> <%= Common.getBahasaConfig("Portal") %></a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link custom-nav-link" href="#section-products"><i class="fas fa-layer-group me-1"></i> <%= Common.getBahasaConfig("Produk") %></a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link custom-nav-link" href="#section-value"><i class="fas fa-gem me-1"></i> <%= Common.getBahasaConfig("Keunggulan") %></a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link custom-nav-link" href="#section-ai"><i class="fas fa-wand-magic-sparkles me-1"></i> <%= Common.getBahasaConfig("AI") %></a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link custom-nav-link" href="#section-spi"><i class="fas fa-user-shield me-1"></i> <%= Common.getBahasaConfig("SPI") %></a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link custom-nav-link" href="#section-roadmap"><i class="fas fa-route me-1"></i> <%= Common.getBahasaConfig("Implementasi") %></a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link custom-nav-link" href="#section-partnership"><i class="fas fa-handshake me-1"></i> <%= Common.getBahasaConfig("Kemitraan") %></a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link custom-nav-link" href="#section-about"><i class="fas fa-info-circle me-1"></i> <%= Common.getBahasaConfig("Tentang") %></a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link custom-nav-link" href="#section-payment"><i class="fas fa-wallet me-1"></i> <%= Common.getBahasaConfig("Pembayaran") %></a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link custom-nav-link" href="#section-kiosk"><i class="fas fa-desktop me-1"></i> <%= Common.getBahasaConfig("Anjungan") %></a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link custom-nav-link" href="#section-hardware"><i class="fas fa-cash-register me-1"></i> <%= Common.getBahasaConfig("Hardware POS") %></a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link custom-nav-link" href="#section-investment"><i class="fas fa-file-invoice-dollar me-1"></i> <%= Common.getBahasaConfig("Investasi") %></a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link custom-nav-link" href="#section-modules"><i class="fas fa-boxes me-1"></i> <%= Common.getBahasaConfig("Modul Utama") %></a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link custom-nav-link" href="#section-documents"><i class="fas fa-folder-open me-1"></i> <%= Common.getBahasaConfig("Dokumen") %></a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link custom-nav-link" href="#section-contact"><i class="fas fa-headset me-1"></i> <%= Common.getBahasaConfig("Kontak") %></a>
                    </li>
                </ul>
            </nav>
        </div>
        
        <div class="d-lg-none sticky-top mb-4" style="top: 10px; z-index: 1050;">
            <nav class="navbar navbar-light mobile-smart-nav px-3">
                <a class="navbar-brand fw-bold text-primary fs-6" href="#">
                    <i class="fas fa-bars me-2"></i><%= Common.getBahasaConfig("Menu Navigasi") %>
                </a>
                <button 
                    class="navbar-toggler border-0" 
                    type="button" 
                    data-bs-toggle="collapse" 
                    data-bs-target="#mobileNav" 
                >
                    <span class="navbar-toggler-icon" style="transform: scale(0.8);"></span>
                </button>
                <div class="collapse navbar-collapse mt-3" id="mobileNav">
                    <ul class="navbar-nav text-start ms-2 mb-2">
                        <li class="nav-item">
                            <a class="nav-link fw-semibold text-dark" href="#section-intro"><i class="fas fa-home me-2 text-primary"></i><%= Common.getBahasaConfig("Beranda") %></a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link fw-semibold text-dark" href="#section-executive"><i class="fas fa-clipboard-check me-2 text-primary"></i><%= Common.getBahasaConfig("Ringkasan Eksekutif") %></a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link fw-semibold text-dark" href="#section-portal"><i class="fas fa-th me-2 text-primary"></i><%= Common.getBahasaConfig("Portal Terpadu") %></a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link fw-semibold text-dark" href="#section-products"><i class="fas fa-layer-group me-2 text-primary"></i><%= Common.getBahasaConfig("Ragam Solusi") %></a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link fw-semibold text-dark" href="#section-value"><i class="fas fa-gem me-2 text-primary"></i><%= Common.getBahasaConfig("Keunggulan Solusi") %></a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link fw-semibold text-dark" href="#section-ai"><i class="fas fa-wand-magic-sparkles me-2 text-primary"></i><%= Common.getBahasaConfig("Kecerdasan Buatan") %></a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link fw-semibold text-dark" href="#section-spi"><i class="fas fa-user-shield me-2 text-primary"></i><%= Common.getBahasaConfig("Satuan Pengawasan Internal") %></a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link fw-semibold text-dark" href="#section-roadmap"><i class="fas fa-route me-2 text-primary"></i><%= Common.getBahasaConfig("Alur Implementasi") %></a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link fw-semibold text-dark" href="#section-partnership"><i class="fas fa-handshake me-2 text-primary"></i><%= Common.getBahasaConfig("Kemitraan & SLA") %></a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link fw-semibold text-dark" href="#section-about"><i class="fas fa-info-circle me-2 text-primary"></i><%= Common.getBahasaConfig("Tentang Sistem") %></a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link fw-semibold text-dark" href="#section-payment"><i class="fas fa-wallet me-2 text-primary"></i><%= Common.getBahasaConfig("Pembayaran") %></a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link fw-semibold text-dark" href="#section-kiosk"><i class="fas fa-desktop me-2 text-primary"></i><%= Common.getBahasaConfig("Anjungan") %></a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link fw-semibold text-dark" href="#section-hardware"><i class="fas fa-cash-register me-2 text-primary"></i><%= Common.getBahasaConfig("Hardware POS") %></a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link fw-semibold text-dark" href="#section-investment"><i class="fas fa-file-invoice-dollar me-2 text-primary"></i><%= Common.getBahasaConfig("Skema Investasi") %></a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link fw-semibold text-dark" href="#section-modules"><i class="fas fa-boxes me-2 text-primary"></i><%= Common.getBahasaConfig("Modul Utama") %></a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link fw-semibold text-dark" href="#section-documents"><i class="fas fa-folder-open me-2 text-primary"></i><%= Common.getBahasaConfig("Dokumen Resmi") %></a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link fw-semibold text-dark" href="#section-contact"><i class="fas fa-headset me-2 text-primary"></i><%= Common.getBahasaConfig("Kontak") %></a>
                        </li>
                    </ul>
                </div>
            </nav>
        </div>

        <div id="section-intro" class="row justify-content-center mb-4">
            <div class="col-lg-12 col-xl-11">
                <div class="glass-intro p-4 p-lg-5 text-start">
                    <div class="row align-items-start g-5">
                        
                        <div class="col-lg-7 order-2 order-lg-1">
                            <h2 class="fw-bold mb-3" style="color: var(--primary-color);">
                                <i class="fas fa-chart-line me-2"></i><%= Common.getBahasaConfig("Solusi Transformatif Pendidikan") %>
                            </h2>
                            <p class="text-secondary" style="line-height: 1.8; text-align: justify; font-size: 1.05rem;">
                                <%= Common.getBahasaConfig("Di era digital yang terus berkembang pesat, institusi pendidikan dituntut untuk senantiasa berinovasi dan beradaptasi guna bersaing secara global. Pengelolaan administrasi yang efisien, proses akademik yang terintegrasi, serta jaminan mutu pendidikan merupakan faktor kunci dalam mencapai keunggulan kompetitif.") %>
                            </p>
                            <p class="text-secondary mb-0" style="line-height: 1.8; text-align: justify; font-size: 1.05rem;">
                                <%= Common.getBahasaConfig("Sistem Enterprise kami hadir sebagai solusi transformatif, dirancang khusus untuk membantu institusi pendidikan menjawab tantangan zaman dan mewujudkan visi keunggulan yang diharapkan.") %>
                            </p>
                        </div>
                        
                        <div class="col-lg-5 order-1 order-lg-2">
                            <div class="video-wrapper shadow-lg sticky-md-top" style="border: 4px solid rgba(255,255,255,0.9); border-radius: 16px; top: 1rem;">
                                <div class="ratio ratio-16x9">
                                    <img 
                                        src="<%=Common.ROOT %>/img/belajar.jpg" 
                                        class="w-100 h-100 object-fit-cover"
                                    />
                                </div>
                                <div class="bg-white p-3 text-center border-top">
                                    <h6 class="fw-bold text-dark mb-0">
                                        <i class="fa fa-book text-primary me-2"></i><%= Common.getBahasaConfig("Sistem Informasi Terpadu") %>
                                    </h6>
                                </div>
                            </div>
                        </div>

                    </div>
                </div>
            </div>
        </div>


        <div id="section-executive" class="row justify-content-center mb-5">
            <div class="col-lg-12 col-xl-11">
                <div class="premium-card p-4 p-lg-5">
                    <div class="text-center position-relative mb-4">
                        <div class="enterprise-ribbon-v2">
                            <i class="fas fa-clipboard-check"></i><%= Common.getBahasaConfig("Ringkasan Eksekutif") %>
                        </div>
                        <h2 class="section-heading-premium">
                            <%= Common.getBahasaConfig("Solusi Digital yang Tidak Hanya Menjual Aplikasi, Tetapi Membangun Tata Kelola Institusi") %>
                        </h2>
                        <p class="section-lead-premium mb-0">
                            <%= Common.getBahasaConfig("Enterprise Education dirancang sebagai ekosistem kerja yang menyatukan proses akademik, administrasi, keuangan, pembayaran, SDM, aset, persuratan, dashboard, layanan mandiri, dan integrasi mobile agar lembaga pendidikan memiliki satu sumber data yang tertib, terukur, dan siap dikembangkan jangka panjang.") %>
                        </p>
                    </div>

                    <div class="row g-4">
                        <div class="col-md-6 col-xl-3">
                            <div class="executive-brief-card">
                                <div class="executive-icon"><i class="fas fa-database"></i></div>
                                <h4><%= Common.getBahasaConfig("Satu Data, Banyak Layanan") %></h4>
                                <p><%= Common.getBahasaConfig("Data utama institusi tidak lagi tersebar di banyak file dan aplikasi. Setiap unit kerja menggunakan sumber data yang sama sehingga laporan lebih konsisten, mudah diverifikasi, dan siap dipakai untuk audit maupun akreditasi.") %></p>
                            </div>
                        </div>
                        <div class="col-md-6 col-xl-3">
                            <div class="executive-brief-card">
                                <div class="executive-icon"><i class="fas fa-bolt"></i></div>
                                <h4><%= Common.getBahasaConfig("Layanan Lebih Cepat") %></h4>
                                <p><%= Common.getBahasaConfig("Alur pendaftaran, pembayaran, validasi akademik, persetujuan surat, pengajuan internal, presensi, dan layanan pengguna dapat diproses lebih singkat melalui portal digital, notifikasi, serta riwayat transaksi yang terdokumentasi.") %></p>
                            </div>
                        </div>
                        <div class="col-md-6 col-xl-3">
                            <div class="executive-brief-card">
                                <div class="executive-icon"><i class="fas fa-chart-line"></i></div>
                                <h4><%= Common.getBahasaConfig("Keputusan Berbasis Dashboard") %></h4>
                                <p><%= Common.getBahasaConfig("Pimpinan dapat membaca indikator penting seperti perkembangan akademik, penerimaan pembayaran, piutang, kinerja SDM, aset, layanan, dan aktivitas operasional melalui dashboard yang ringkas, informatif, dan mudah dipahami.") %></p>
                            </div>
                        </div>
                        <div class="col-md-6 col-xl-3">
                            <div class="executive-brief-card">
                                <div class="executive-icon"><i class="fas fa-handshake-angle"></i></div>
                                <h4><%= Common.getBahasaConfig("Kemitraan Implementasi") %></h4>
                                <p><%= Common.getBahasaConfig("Kerja sama tidak berhenti pada instalasi sistem. Kami menyiapkan pendekatan bertahap mulai dari assessment, konfigurasi, migrasi data, pelatihan, go-live, pendampingan, hingga evaluasi pemanfaatan sistem.") %></p>
                            </div>
                        </div>
                    </div>

                    <div class="formal-note-box mt-4">
                        <div class="note-title"><i class="fas fa-file-signature me-2"></i><%= Common.getBahasaConfig("Materi Resmi Disiapkan Lengkap untuk Proses Pengambilan Keputusan") %></div>
                        <p class="note-text">
                            <%= Common.getBahasaConfig("Untuk memudahkan calon mitra, ekosistem ini dilengkapi dengan halaman presentasi siap tayang, proposal siap cetak, surat penawaran, serta draf perjanjian kerja sama. Setiap dokumen disusun agar pimpinan, tim teknis, keuangan, pengadaan, dan legal dapat memahami ruang lingkup, manfaat, skema kerja sama, serta tahapan implementasi secara lebih jelas.") %>
                        </p>
                    </div>
                </div>
            </div>
        </div>


        <div id="section-products" class="row justify-content-center mb-5">
            <div class="col-lg-12 col-xl-11">
                <div class="premium-card p-4 p-lg-5">
                    <div class="text-center position-relative">
                        <div class="section-eyebrow">
                            <i class="fas fa-layer-group"></i><%= Common.getBahasaConfig("Ragam Solusi") %>
                        </div>
                        <h2 class="section-heading-premium">
                            <%= Common.getBahasaConfig("Empat Wajah, Satu Fondasi yang Sama") %>
                        </h2>
                        <p class="section-lead-premium">
                            <%= Common.getBahasaConfig("Setiap institusi pendidikan memiliki irama kerjanya sendiri. Karena itu ekosistem ini tumbuh dari satu fondasi teknologi yang sama, lalu dibentuk mengikuti cara kerja masing-masing lembaga. Anda dapat memulai dari satu produk, lalu menambah yang lain ketika kebutuhan berkembang, tanpa berganti sistem dan tanpa memindahkan data.") %>
                        </p>
                    </div>

                    <div class="ee-prod-grid">
                        <div class="ee-prod-card">
                            <div class="ee-prod-badge" style="background: linear-gradient(135deg, #1e3a8a, #4f46e5);">
                                <i class="fas fa-university"></i>
                            </div>
                            <h4>eCampus</h4>
                            <span class="ee-prod-for" style="color: #4f46e5;"><%= Common.getBahasaConfig("Untuk Perguruan Tinggi") %></span>
                            <p>
                                <%= Common.getBahasaConfig("Satu semester di kampus berarti ribuan keputusan kecil yang saling bergantung: kuota penerimaan, kontrak KRS, jadwal yang tidak boleh bentrok, nilai yang harus turun tepat waktu, dan laporan yang menunggu di ujung semuanya. eCampus merapikan seluruh rantai itu dalam satu alur, sejak calon mahasiswa mendaftar hingga alumni mengisi tracer study. Hasilnya, pelaporan PDDikti dan berkas akreditasi bukan lagi proyek dadakan di akhir periode, melainkan buah dari pekerjaan harian yang sejak awal sudah tertata.") %>
                            </p>
                            <div class="ee-prod-chips">
                                <span><%= Common.getBahasaConfig("PMB & RPL") %></span>
                                <span><%= Common.getBahasaConfig("KRS & Perwalian") %></span>
                                <span><%= Common.getBahasaConfig("Kurikulum OBE") %></span>
                                <span><%= Common.getBahasaConfig("e-Learning") %></span>
                                <span><%= Common.getBahasaConfig("Neo Feeder & SISTER") %></span>
                                <span><%= Common.getBahasaConfig("Akreditasi LAM") %></span>
                                <span><%= Common.getBahasaConfig("Keuangan Mahasiswa") %></span>
                                <span><%= Common.getBahasaConfig("Wisuda & Alumni") %></span>
                            </div>
                        </div>

                        <div class="ee-prod-card">
                            <div class="ee-prod-badge" style="background: linear-gradient(135deg, #0ea5e9, #2563eb);">
                                <i class="fas fa-school"></i>
                            </div>
                            <h4>eSchool</h4>
                            <span class="ee-prod-for" style="color: #0284c7;"><%= Common.getBahasaConfig("Untuk Sekolah & Yayasan") %></span>
                            <p>
                                <%= Common.getBahasaConfig("Sekolah jarang kekurangan data; yang kerap kurang justru waktu untuk merapikannya. eSchool menyatukan PPDB, presensi, penilaian, rapor, dan pembayaran SPP dalam satu sistem yang dapat dipantau yayasan lintas unit, mulai dari TK sampai SMK. Manfaatnya paling terasa di sisi orang tua: kehadiran, nilai, dan tagihan anak sampai ke genggaman mereka melalui WhatsApp dan aplikasi, tanpa guru perlu menyalin ulang apa pun.") %>
                            </p>
                            <div class="ee-prod-chips">
                                <span><%= Common.getBahasaConfig("PPDB Online") %></span>
                                <span><%= Common.getBahasaConfig("Presensi & Kehadiran") %></span>
                                <span><%= Common.getBahasaConfig("Nilai & Rapor") %></span>
                                <span><%= Common.getBahasaConfig("SPP & Tagihan") %></span>
                                <span><%= Common.getBahasaConfig("Portal Orang Tua") %></span>
                                <span><%= Common.getBahasaConfig("Yayasan Multi-Unit") %></span>
                                <span><%= Common.getBahasaConfig("Kantin Non-Tunai") %></span>
                                <span><%= Common.getBahasaConfig("Perpustakaan") %></span>
                            </div>
                        </div>

                        <div class="ee-prod-card">
                            <div class="ee-prod-badge" style="background: linear-gradient(135deg, #10b981, #0d9488);">
                                <i class="fas fa-notes-medical"></i>
                            </div>
                            <h4>eMedic</h4>
                            <span class="ee-prod-for" style="color: #059669;"><%= Common.getBahasaConfig("Untuk Rumah Sakit, Klinik & Puskesmas") %></span>
                            <p>
                                <%= Common.getBahasaConfig("Mutu layanan kesehatan sering dinilai dari hal-hal yang tampak sederhana: antrean yang tertib, berkas yang tidak hilang, obat yang tersedia, dan klaim yang tidak kembali. eMedic menangani rangkaian itu sejak pendaftaran, rekam medis elektronik, farmasi, sampai penagihan, sekaligus tersambung ke BPJS/JKN dan SATUSEHAT sehingga pelaporan berjalan otomatis. Dapat berdiri sendiri untuk fasilitas kesehatan, atau menempel pada kampus, sekolah, dan pesantren yang memiliki klinik sendiri.") %>
                            </p>
                            <div class="ee-prod-chips">
                                <span><%= Common.getBahasaConfig("Rawat Jalan & Inap") %></span>
                                <span><%= Common.getBahasaConfig("UGD") %></span>
                                <span><%= Common.getBahasaConfig("Rekam Medis Elektronik") %></span>
                                <span><%= Common.getBahasaConfig("Farmasi & Racikan") %></span>
                                <span><%= Common.getBahasaConfig("BPJS / JKN") %></span>
                                <span><%= Common.getBahasaConfig("SATUSEHAT") %></span>
                                <span><%= Common.getBahasaConfig("Billing & Kasir") %></span>
                                <span><%= Common.getBahasaConfig("Dashboard Analitik") %></span>
                            </div>
                        </div>

                        <div class="ee-prod-card">
                            <div class="ee-prod-badge" style="background: linear-gradient(135deg, #7c3aed, #6d28d9);">
                                <i class="fas fa-mosque"></i>
                            </div>
                            <h4>ePesantren</h4>
                            <span class="ee-prod-for" style="color: #6d28d9;"><%= Common.getBahasaConfig("Untuk Pondok Pesantren") %></span>
                            <p>
                                <%= Common.getBahasaConfig("Pesantren mengurus jauh lebih banyak daripada sekadar pelajaran: ada asrama yang harus terjaga, hafalan yang harus tercatat, perizinan santri yang harus tertelusur, serta wali santri di kejauhan yang ingin tahu kabar anaknya. ePesantren menyatukan administrasi santri, asrama, tahfidz, kesehatan, dan keuangan dalam tata kelola yang transparan, termasuk uang saku non-tunai agar santri tidak perlu memegang uang tunai, serta laporan berkala yang menenangkan orang tua.") %>
                            </p>
                            <div class="ee-prod-chips">
                                <span><%= Common.getBahasaConfig("Data Santri") %></span>
                                <span><%= Common.getBahasaConfig("Asrama & Kamar") %></span>
                                <span><%= Common.getBahasaConfig("Tahfidz & Halaqah") %></span>
                                <span><%= Common.getBahasaConfig("Perizinan Santri") %></span>
                                <span><%= Common.getBahasaConfig("Uang Saku Non-Tunai") %></span>
                                <span><%= Common.getBahasaConfig("Kesehatan Santri") %></span>
                                <span><%= Common.getBahasaConfig("Keuangan & Syahriah") %></span>
                                <span><%= Common.getBahasaConfig("Laporan Wali Santri") %></span>
                            </div>
                        </div>
                    </div>

                    <div class="ee-prod-foot">
                        <i class="fas fa-puzzle-piece me-2"></i>
                        <strong><%= Common.getBahasaConfig("Tumbuh bertahap, tanpa berganti sistem.") %></strong>
                        <%= Common.getBahasaConfig("Yayasan yang menaungi sekolah, kampus, pesantren, sekaligus klinik dapat menjalankan keempatnya di atas satu fondasi, dengan data induk yang saling terhubung dan laporan yang terkonsolidasi ke pusat. Penambahan unit atau jenjang baru cukup dilakukan melalui konfigurasi.") %>
                    </div>
                </div>
            </div>
        </div>


        <div id="section-value" class="row justify-content-center mb-5">
            <div class="col-lg-12 col-xl-11">
                <div class="premium-card p-4 p-lg-5">
                    <div class="text-center position-relative">
                        <div class="section-eyebrow">
                            <i class="fas fa-gem"></i><%= Common.getBahasaConfig("Nilai Strategis") %>
                        </div>
                        <h2 class="section-heading-premium">
                            <%= Common.getBahasaConfig("Bukan Sekadar Aplikasi, tetapi Fondasi Tata Kelola Pendidikan Modern") %>
                        </h2>
                        <p class="section-lead-premium">
                            <%= Common.getBahasaConfig("Enterprise Education membantu institusi mengubah proses manual yang tersebar menjadi sistem kerja digital yang rapi, terukur, dan mudah dikendalikan. Setiap data akademik, keuangan, administrasi, dan layanan pengguna disusun agar saling terhubung sehingga keputusan dapat diambil lebih cepat dan berbasis informasi yang valid.") %>
                        </p>
                    </div>

                    <div class="row g-4 mt-2">
                        <div class="col-md-6 col-xl-3">
                            <div class="benefit-card">
                                <div class="benefit-icon"><i class="fas fa-database"></i></div>
                                <h4><%= Common.getBahasaConfig("Satu Data Institusi") %></h4>
                                <p><%= Common.getBahasaConfig("Mengurangi data ganda dan perbedaan laporan antarbagian melalui integrasi basis data yang lebih konsisten, mudah diaudit, dan siap digunakan untuk pelaporan internal maupun eksternal.") %></p>
                            </div>
                        </div>
                        <div class="col-md-6 col-xl-3">
                            <div class="benefit-card">
                                <div class="benefit-icon"><i class="fas fa-bolt"></i></div>
                                <h4><%= Common.getBahasaConfig("Proses Lebih Cepat") %></h4>
                                <p><%= Common.getBahasaConfig("Alur kerja rutin seperti pendaftaran, pembayaran, presensi, pengajuan, persuratan, validasi dokumen, dan laporan dapat diproses lebih singkat dengan automasi yang terstruktur.") %></p>
                            </div>
                        </div>
                        <div class="col-md-6 col-xl-3">
                            <div class="benefit-card">
                                <div class="benefit-icon"><i class="fas fa-chart-line"></i></div>
                                <h4><%= Common.getBahasaConfig("Kontrol Pimpinan") %></h4>
                                <p><%= Common.getBahasaConfig("Dashboard eksekutif memudahkan pimpinan membaca kondisi institusi, memantau capaian, mengidentifikasi risiko, dan menentukan prioritas perbaikan secara lebih objektif.") %></p>
                            </div>
                        </div>
                        <div class="col-md-6 col-xl-3">
                            <div class="benefit-card">
                                <div class="benefit-icon"><i class="fas fa-handshake"></i></div>
                                <h4><%= Common.getBahasaConfig("Pendampingan Implementasi") %></h4>
                                <p><%= Common.getBahasaConfig("Implementasi tidak hanya berhenti pada instalasi sistem, tetapi dilengkapi konsultasi proses bisnis, konfigurasi modul, migrasi data, pelatihan, dan pendampingan operasional.") %></p>
                            </div>
                        </div>
                    </div>

                    <div class="platform-strip">
                        <div class="platform-item">
                            <i class="fas fa-university"></i>
                            <strong><%= Common.getBahasaConfig("Untuk Perguruan Tinggi") %></strong>
                            <span><%= Common.getBahasaConfig("Akademik, PMB, Feeder/EMIS, OBE, penelitian, pengabdian, keuangan, SDM, aset, dan dashboard pimpinan.") %></span>
                        </div>
                        <div class="platform-item">
                            <i class="fas fa-school"></i>
                            <strong><%= Common.getBahasaConfig("Untuk Sekolah & Yayasan") %></strong>
                            <span><%= Common.getBahasaConfig("PPDB, akademik, pembayaran, presensi, laporan siswa, komunikasi wali, keuangan yayasan, dan manajemen unit sekolah.") %></span>
                        </div>
                        <div class="platform-item">
                            <i class="fas fa-mosque"></i>
                            <strong><%= Common.getBahasaConfig("Untuk Pesantren") %></strong>
                            <span><%= Common.getBahasaConfig("Administrasi santri, asrama, keuangan, akademik, tahfidz, pembayaran, pelaporan, dan tata kelola pesantren yang lebih transparan.") %></span>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div id="section-ai" class="row justify-content-center mb-5">
            <div class="col-lg-12 col-xl-11">
                <div class="premium-card p-4 p-lg-5">
                    <div class="text-center position-relative">
                        <div class="section-eyebrow">
                            <i class="fas fa-wand-magic-sparkles"></i><%= Common.getBahasaConfig("Kecerdasan Buatan") %>
                        </div>
                        <h2 class="section-heading-premium">
                            <%= Common.getBahasaConfig("AI yang Bekerja di Dalam Sistem, Bukan di Aplikasi Terpisah") %>
                        </h2>
                        <p class="section-lead-premium">
                            <%= Common.getBahasaConfig("Kecerdasan buatan di sini tidak berdiri sendiri sebagai aplikasi yang harus dibuka terpisah, lalu hasilnya disalin manual ke sistem. Ia hadir sebagai tombol pada layar yang memang sudah biasa digunakan setiap hari. Pekerjaan yang selama ini paling menyita waktu dapat diselesaikan dalam hitungan detik, lalu tinggal ditinjau, disunting seperlunya, dan disetujui.") %>
                        </p>
                    </div>

                    <div class="ee-ai-grid">
                        <div class="ee-ai-pillar">
                            <div class="ee-ai-num">1</div>
                            <h5><%= Common.getBahasaConfig("Menyusun Kurikulum & Perangkat Ajar") %></h5>
                            <p><%= Common.getBahasaConfig("Mulai dari Profil Lulusan, CPL, CPMK, Sub-CPMK beserta bobotnya, bahan kajian, daftar pustaka, rubrik penilaian, sampai rincian RPS enam belas pertemuan. Pekerjaan yang biasanya menghabiskan berhari-hari kini tersaji sebagai rancangan awal yang tinggal disempurnakan dosen sesuai karakter mata kuliahnya.") %></p>
                        </div>
                        <div class="ee-ai-pillar">
                            <div class="ee-ai-num">2</div>
                            <h5><%= Common.getBahasaConfig("Membuat & Mengoreksi Soal Ujian") %></h5>
                            <p><%= Common.getBahasaConfig("AI menyusun butir soal pilihan ganda maupun esai yang relevan dengan capaian mata kuliah, dan tidak mengulang soal yang sudah pernah dipakai. Jawaban esai pun dapat dikoreksi otomatis, lengkap dengan skor serta umpan balik untuk setiap peserta, baik satu per satu maupun sekaligus untuk seluruh kelas.") %></p>
                        </div>
                        <div class="ee-ai-pillar">
                            <div class="ee-ai-num">3</div>
                            <h5><%= Common.getBahasaConfig("Menuliskan Catatan & Dokumen") %></h5>
                            <p><%= Common.getBahasaConfig("Tombol bantu AI melekat pada kolom catatan di berbagai modul: catatan pembelajaran, notulen rapat, uraian tugas, laporan kegiatan, hingga draf surat dan pengumuman. Menulis tidak lagi dimulai dari halaman kosong, melainkan dari draf yang sudah rapi dan tinggal disesuaikan.") %></p>
                        </div>
                        <div class="ee-ai-pillar">
                            <div class="ee-ai-num">4</div>
                            <h5><%= Common.getBahasaConfig("Memberi Analisis & Rekomendasi") %></h5>
                            <p><%= Common.getBahasaConfig("Karena seluruh modul berbagi satu basis data, AI dapat membaca gejala lintas bagian: kehadiran yang menurun, tunggakan yang menumpuk menjelang ujian, capaian pembelajaran yang meleset dari target, atau beban mengajar yang timpang. Temuan itu disusun menjadi rumusan masalah, analisis penyebab, dan usulan tindak lanjut yang konkret bagi pimpinan.") %></p>
                        </div>
                    </div>

                    <div class="ee-ai-engine">
                        <div class="ee-ai-box is-primary">
                            <h6><i class="fas fa-server me-2" style="color: #059669;"></i><%= Common.getBahasaConfig("Berjalan di Server Anda Sendiri") %></h6>
                            <p><%= Common.getBahasaConfig("Model AI dapat dijalankan sepenuhnya di server milik institusi. Data mahasiswa, siswa, santri, pasien, soal ujian, dan dokumen internal tidak pernah keluar dari lingkungan Anda. Tidak ada biaya per permintaan, dan layanan tetap berfungsi ketika koneksi internet sedang terganggu.") %></p>
                        </div>
                        <div class="ee-ai-box">
                            <h6><i class="fas fa-cloud me-2" style="color: #2563eb;"></i><%= Common.getBahasaConfig("Atau Layanan Cloud, Bila Diperlukan") %></h6>
                            <p><%= Common.getBahasaConfig("Apabila menginginkan kualitas keluaran tertinggi, sistem dapat dialihkan ke penyedia layanan cloud cukup melalui perubahan konfigurasi. Tanpa mengubah kode program, tanpa migrasi data, dan dapat dikembalikan sewaktu-waktu sesuai kebijakan institusi.") %></p>
                        </div>
                    </div>

                    <div class="ee-ai-note">
                        <i class="fas fa-user-shield me-2" style="color: #334155;"></i>
                        <strong><%= Common.getBahasaConfig("Keputusan tetap berada di tangan manusia.") %></strong>
                        <%= Common.getBahasaConfig("Setiap fitur AI dapat dinyalakan atau dimatikan per modul sesuai kebijakan lembaga. Seluruh keluaran AI berstatus usulan dan baru tersimpan setelah ditinjau serta disetujui pengguna yang berwenang. Setiap aktivitas terekam dalam log yang memuat pengguna, fitur, objek, dan status peninjauan, sehingga dapat diaudit sewaktu-waktu. Kemampuan ini tersedia pada eCampus, eSchool, eMedic, maupun ePesantren.") %>
                    </div>
                </div>
            </div>
        </div>

        <div id="section-spi" class="row justify-content-center mb-5">
            <div class="col-lg-12 col-xl-11">
                <div class="premium-card p-4 p-lg-5">
                    <div class="text-center position-relative">
                        <div class="ee-new-pill">
                            <i class="fas fa-star"></i><%= Common.getBahasaConfig("Modul Baru") %>
                        </div>
                        <div class="section-eyebrow">
                            <i class="fas fa-user-shield"></i><%= Common.getBahasaConfig("Satuan Pengawasan Internal") %>
                        </div>
                        <h2 class="section-heading-premium">
                            <%= Common.getBahasaConfig("Menjaga Kepercayaan, Bukan Sekadar Mencari Kesalahan") %>
                        </h2>
                        <p class="section-lead-premium">
                            <%= Common.getBahasaConfig("Aset paling berharga sebuah lembaga pendidikan bukanlah gedung atau perangkatnya, melainkan kepercayaan: dari yayasan, orang tua, mitra, dan regulator. Kepercayaan itu dijaga oleh pengawasan yang berjalan sepanjang tahun, bukan yang baru sibuk menjelang pemeriksaan. Modul Satuan Pengawasan Internal mengubah audit internal dari tumpukan berkas dan lembar kerja yang terpisah-pisah menjadi satu siklus yang tertelusur: direncanakan, dijalankan, ditindaklanjuti, lalu dibuktikan.") %>
                        </p>
                    </div>

                    <div class="ee-spi-grid">
                        <div class="ee-spi-card">
                            <div class="ee-spi-ic"><i class="fas fa-clipboard-list"></i></div>
                            <h5><%= Common.getBahasaConfig("Program Audit yang Terencana") %></h5>
                            <p><%= Common.getBahasaConfig("Susun program pengawasan tahunan lengkap dengan objek, ruang lingkup, jadwal, dan tim auditor. Setiap penugasan memiliki kertas kerjanya sendiri, sehingga proses audit tidak lagi bergantung pada catatan pribadi yang ikut hilang begitu personelnya berganti.") %></p>
                        </div>
                        <div class="ee-spi-card">
                            <div class="ee-spi-ic"><i class="fas fa-triangle-exclamation"></i></div>
                            <h5><%= Common.getBahasaConfig("Temuan dengan Bobot Risiko") %></h5>
                            <p><%= Common.getBahasaConfig("Setiap temuan tercatat lengkap: objek, unit, tingkat risiko, kriteria yang dilanggar, sebab, akibat, dan rekomendasinya. Pimpinan dapat langsung melihat mana yang berisiko tinggi dan perlu diselesaikan lebih dahulu, bukan sekadar membaca daftar panjang tanpa prioritas.") %></p>
                        </div>
                        <div class="ee-spi-card">
                            <div class="ee-spi-ic"><i class="fas fa-list-check"></i></div>
                            <h5><%= Common.getBahasaConfig("Tindak Lanjut yang Tidak Menguap") %></h5>
                            <p><%= Common.getBahasaConfig("Inilah bagian yang paling sering terputus. Setiap rekomendasi memiliki penanggung jawab, target waktu, bukti penyelesaian, dan status yang terpantau. Temuan yang sama tidak lagi muncul berulang setiap tahun hanya karena tidak ada yang mengawal penyelesaiannya.") %></p>
                        </div>
                        <div class="ee-spi-card">
                            <div class="ee-spi-ic"><i class="fas fa-database"></i></div>
                            <h5><%= Common.getBahasaConfig("Data Ditarik Langsung dari Modul Lain") %></h5>
                            <p><%= Common.getBahasaConfig("Karena keuangan, aset, pengadaan, kepegawaian, dan akademik berada dalam satu sistem, auditor tidak perlu lagi mengirim permintaan data lalu menunggu berhari-hari. Waktu yang selama ini habis untuk mengumpulkan berkas dapat dialihkan ke pekerjaan yang sesungguhnya bernilai, yaitu menganalisis.") %></p>
                        </div>
                    </div>

                    <div class="ee-spi-verticals">
                        <div class="ee-spi-vert">
                            <strong>eCampus</strong>
                            <span><%= Common.getBahasaConfig("Tata kelola perguruan tinggi menuntut SPI yang benar-benar aktif. Pengawasan keuangan, aset, pengadaan, dan kepatuhan akademik terekam rapi sehingga buktinya sudah siap ketika asesor akreditasi datang.") %></span>
                        </div>
                        <div class="ee-spi-vert">
                            <strong>eSchool</strong>
                            <span><%= Common.getBahasaConfig("Yayasan dapat mengawasi seluruh unit sekolah dari satu tempat: kas dan iuran, pengadaan sarana, honor, sampai kepatuhan pelaporan tiap jenjang, tanpa harus mendatangi unit satu per satu.") %></span>
                        </div>
                        <div class="ee-spi-vert">
                            <strong>eMedic</strong>
                            <span><%= Common.getBahasaConfig("Pengawasan atas klaim, persediaan obat, dan penerimaan kasir, yaitu area yang paling rawan selisih di fasilitas kesehatan sekaligus paling sering menjadi sorotan pemeriksa.") %></span>
                        </div>
                        <div class="ee-spi-vert">
                            <strong>ePesantren</strong>
                            <span><%= Common.getBahasaConfig("Menjaga amanah dana wali santri dan donatur melalui pemeriksaan yang tercatat, berbukti, dan dapat dipertanggungjawabkan kepada pengasuh maupun yayasan.") %></span>
                        </div>
                    </div>

                    <div class="ee-spi-note">
                        <i class="fas fa-wand-magic-sparkles me-2"></i>
                        <strong><%= Common.getBahasaConfig("Berpadu dengan kecerdasan buatan.") %></strong>
                        <%= Common.getBahasaConfig("AI membantu menyusun rumusan temuan, analisis sebab-akibat, dan rekomendasi berdasarkan data yang ada, serta menandai pola yang layak diperiksa lebih dalam, misalnya pengadaan yang selalu mendekati nilai batas kewenangan atau unit yang tunggakannya menumpuk pada periode tertentu. Auditor tetap pemegang keputusan; AI hanya mempercepat penyusunannya.") %>
                    </div>
                </div>
            </div>
        </div>

        <div id="section-impact" class="row justify-content-center mb-5">
            <div class="col-lg-12 col-xl-11">
                <div class="premium-card p-4 p-lg-5">
                    <div class="text-center position-relative mb-4">
                        <div class="section-eyebrow">
                            <i class="fas fa-arrows-rotate"></i><%= Common.getBahasaConfig("Dampak Transformasi") %>
                        </div>
                        <h2 class="section-heading-premium">
                            <%= Common.getBahasaConfig("Dari Sistem Terpisah Menjadi Ekosistem Kerja Terpadu") %>
                        </h2>
                        <p class="section-lead-premium mb-0">
                            <%= Common.getBahasaConfig("Kami memahami bahwa kendala terbesar institusi pendidikan bukan hanya teknologi, tetapi konsistensi proses, validitas data, kecepatan layanan, dan kemampuan organisasi untuk mengambil keputusan berbasis laporan yang akurat.") %>
                        </p>
                    </div>

                    <div class="pain-solution-row">
                        <div class="comparison-box problem">
                            <h4><i class="fas fa-triangle-exclamation text-warning me-2"></i><%= Common.getBahasaConfig("Kondisi yang Sering Terjadi") %></h4>
                            <ul class="x-list-modern">
                                <li><i class="fas fa-circle-xmark"></i><span><%= Common.getBahasaConfig("Data tersebar di banyak aplikasi, file spreadsheet, dan pencatatan manual sehingga laporan sering lambat dan tidak seragam.") %></span></li>
                                <li><i class="fas fa-circle-xmark"></i><span><%= Common.getBahasaConfig("Proses pembayaran, akademik, persuratan, pengajuan, dan validasi dokumen membutuhkan koordinasi berulang antarbagian.") %></span></li>
                                <li><i class="fas fa-circle-xmark"></i><span><%= Common.getBahasaConfig("Pimpinan sulit memperoleh gambaran menyeluruh karena indikator penting belum tersaji dalam dashboard yang mudah dibaca.") %></span></li>
                                <li><i class="fas fa-circle-xmark"></i><span><%= Common.getBahasaConfig("Audit, akreditasi, dan pelaporan eksternal memerlukan kompilasi data manual yang menyita waktu operator.") %></span></li>
                            </ul>
                        </div>
                        <div class="comparison-box solution">
                            <h4><i class="fas fa-circle-check text-success me-2"></i><%= Common.getBahasaConfig("Perubahan Setelah Terintegrasi") %></h4>
                            <ul class="check-list-modern">
                                <li><i class="fas fa-check-circle"></i><span><%= Common.getBahasaConfig("Setiap unit kerja menggunakan sumber data yang sama sehingga laporan lebih konsisten, mudah diverifikasi, dan cepat disajikan.") %></span></li>
                                <li><i class="fas fa-check-circle"></i><span><%= Common.getBahasaConfig("Alur layanan dibuat digital dengan notifikasi, validasi, riwayat proses, serta hak akses yang dapat disesuaikan dengan struktur institusi.") %></span></li>
                                <li><i class="fas fa-check-circle"></i><span><%= Common.getBahasaConfig("Dashboard ringkas membantu pimpinan melihat indikator akademik, keuangan, SDM, aset, dan layanan dalam satu tampilan strategis.") %></span></li>
                                <li><i class="fas fa-check-circle"></i><span><%= Common.getBahasaConfig("Kebutuhan audit, akreditasi, dan pelaporan menjadi lebih siap karena dokumen, data, dan histori transaksi tersimpan rapi.") %></span></li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div id="section-roadmap" class="row justify-content-center mb-5">
            <div class="col-lg-12 col-xl-11">
                <div class="premium-card p-4 p-lg-5">
                    <div class="text-center position-relative mb-4">
                        <div class="section-eyebrow">
                            <i class="fas fa-route"></i><%= Common.getBahasaConfig("Implementasi Terarah") %>
                        </div>
                        <h2 class="section-heading-premium">
                            <%= Common.getBahasaConfig("Tahapan Implementasi yang Sistematis dan Aman untuk Operasional Institusi") %>
                        </h2>
                        <p class="section-lead-premium mb-0">
                            <%= Common.getBahasaConfig("Setiap institusi memiliki kebutuhan dan kesiapan yang berbeda. Karena itu, implementasi dirancang bertahap agar sistem dapat digunakan secara nyata, tidak mengganggu operasional berjalan, dan tetap memberi ruang penyesuaian kebijakan internal.") %>
                        </p>
                    </div>

                    <div class="row g-4">
                        <div class="col-md-6 col-xl-4">
                            <div class="roadmap-step">
                                <div class="roadmap-number">01</div>
                                <h5><%= Common.getBahasaConfig("Assessment Proses Bisnis") %></h5>
                                <p><%= Common.getBahasaConfig("Memetakan kebutuhan, alur kerja, struktur organisasi, prioritas modul, dan target transformasi digital yang ingin dicapai institusi.") %></p>
                            </div>
                        </div>
                        <div class="col-md-6 col-xl-4">
                            <div class="roadmap-step">
                                <div class="roadmap-number">02</div>
                                <h5><%= Common.getBahasaConfig("Konfigurasi & Penyesuaian") %></h5>
                                <p><%= Common.getBahasaConfig("Menyiapkan parameter sistem, hak akses, template dokumen, master data, skema pembayaran, dan aturan akademik sesuai kebijakan internal.") %></p>
                            </div>
                        </div>
                        <div class="col-md-6 col-xl-4">
                            <div class="roadmap-step">
                                <div class="roadmap-number">03</div>
                                <h5><%= Common.getBahasaConfig("Migrasi & Validasi Data") %></h5>
                                <p><%= Common.getBahasaConfig("Membantu proses import data awal, pengecekan konsistensi, pembersihan data, dan validasi bersama operator agar sistem siap digunakan.") %></p>
                            </div>
                        </div>
                        <div class="col-md-6 col-xl-4">
                            <div class="roadmap-step">
                                <div class="roadmap-number">04</div>
                                <h5><%= Common.getBahasaConfig("Pelatihan Pengguna") %></h5>
                                <p><%= Common.getBahasaConfig("Memberikan transfer knowledge kepada admin, operator, pimpinan, dosen/guru, staf, dan pengguna layanan sesuai peran masing-masing.") %></p>
                            </div>
                        </div>
                        <div class="col-md-6 col-xl-4">
                            <div class="roadmap-step">
                                <div class="roadmap-number">05</div>
                                <h5><%= Common.getBahasaConfig("Go-Live Bertahap") %></h5>
                                <p><%= Common.getBahasaConfig("Menerapkan modul secara bertahap agar risiko operasional lebih terkendali dan setiap unit kerja dapat beradaptasi dengan nyaman.") %></p>
                            </div>
                        </div>
                        <div class="col-md-6 col-xl-4">
                            <div class="roadmap-step">
                                <div class="roadmap-number">06</div>
                                <h5><%= Common.getBahasaConfig("Monitoring & Improvement") %></h5>
                                <p><%= Common.getBahasaConfig("Melakukan evaluasi penggunaan, perbaikan konfigurasi, optimalisasi laporan, dan pengembangan fitur sesuai kebutuhan lanjutan institusi.") %></p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div id="section-partnership" class="row justify-content-center mb-5">
            <div class="col-lg-12 col-xl-11">
                <div class="premium-card p-4 p-lg-5">
                    <div class="text-center position-relative mb-4">
                        <div class="enterprise-ribbon-v2">
                            <i class="fas fa-handshake"></i><%= Common.getBahasaConfig("Kerangka Kemitraan & SLA") %>
                        </div>
                        <h2 class="section-heading-premium">
                            <%= Common.getBahasaConfig("Implementasi Dikelola dengan Ruang Lingkup, Deliverable, dan Dukungan Layanan yang Jelas") %>
                        </h2>
                        <p class="section-lead-premium mb-0">
                            <%= Common.getBahasaConfig("Agar transformasi digital berjalan aman, setiap kerja sama perlu memiliki batasan pekerjaan, tahapan pelaksanaan, indikator keberhasilan, layanan pemeliharaan, serta mekanisme komunikasi yang dipahami oleh seluruh pihak.") %>
                        </p>
                    </div>

                    <div class="partnership-grid mb-4">
                        <div class="partnership-card">
                            <div class="badge-soft"><i class="fas fa-sitemap"></i><%= Common.getBahasaConfig("Tata Kelola") %></div>
                            <h4><%= Common.getBahasaConfig("Ruang Lingkup Modul Terstruktur") %></h4>
                            <p><%= Common.getBahasaConfig("Modul dapat diaktifkan bertahap sesuai prioritas, mulai dari akademik, PMB/PPDB, pembayaran, keuangan, SDM, aset, persuratan, dashboard, pustaka, penelitian, pengabdian, kantin digital, POS, hingga portal rekanan.") %></p>
                        </div>
                        <div class="partnership-card">
                            <div class="badge-soft"><i class="fas fa-people-arrows"></i><%= Common.getBahasaConfig("Pendampingan") %></div>
                            <h4><%= Common.getBahasaConfig("Training dan Transfer Knowledge") %></h4>
                            <p><%= Common.getBahasaConfig("Tim implementasi membantu admin, operator, pimpinan, guru, dosen, staf, dan pengguna kunci memahami alur kerja sistem sehingga lembaga tidak hanya menerima aplikasi, tetapi juga memperoleh kemampuan operasional.") %></p>
                        </div>
                        <div class="partnership-card">
                            <div class="badge-soft"><i class="fas fa-headset"></i><%= Common.getBahasaConfig("Layanan") %></div>
                            <h4><%= Common.getBahasaConfig("SLA, Pemeliharaan, dan Improvement") %></h4>
                            <p><%= Common.getBahasaConfig("Dukungan layanan mencakup bantuan teknis, pemeliharaan sistem, evaluasi penggunaan, perbaikan konfigurasi, serta pengembangan lanjutan sesuai kebutuhan institusi dan kesepakatan kerja sama.") %></p>
                        </div>
                    </div>

                    <div class="deliverable-strip">
                        <div class="deliverable-item">
                            <strong><i class="fas fa-magnifying-glass-chart"></i><%= Common.getBahasaConfig("Assessment") %></strong>
                            <span><%= Common.getBahasaConfig("Pemetaan kebutuhan, proses bisnis, struktur pengguna, dan prioritas modul.") %></span>
                        </div>
                        <div class="deliverable-item">
                            <strong><i class="fas fa-sliders"></i><%= Common.getBahasaConfig("Konfigurasi") %></strong>
                            <span><%= Common.getBahasaConfig("Pengaturan hak akses, master data, template, parameter akademik, dan pembayaran.") %></span>
                        </div>
                        <div class="deliverable-item">
                            <strong><i class="fas fa-file-import"></i><%= Common.getBahasaConfig("Migrasi Data") %></strong>
                            <span><%= Common.getBahasaConfig("Import data awal, validasi, dan penyesuaian format agar sistem siap digunakan.") %></span>
                        </div>
                        <div class="deliverable-item">
                            <strong><i class="fas fa-person-chalkboard"></i><%= Common.getBahasaConfig("Pelatihan") %></strong>
                            <span><%= Common.getBahasaConfig("Pelatihan pengguna kunci dan pendampingan awal sampai proses operasional berjalan.") %></span>
                        </div>
                    </div>

                    <div class="mt-4">
                        <table class="success-table-modern">
                            <thead>
                                <tr>
                                    <th><%= Common.getBahasaConfig("Area Keberhasilan") %></th>
                                    <th><%= Common.getBahasaConfig("Indikator yang Diharapkan") %></th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td><%= Common.getBahasaConfig("Kesiapan Data") %></td>
                                    <td><%= Common.getBahasaConfig("Data master lebih tertata, duplikasi berkurang, format lebih seragam, dan riwayat transaksi dapat dilacak sesuai hak akses pengguna.") %></td>
                                </tr>
                                <tr>
                                    <td><%= Common.getBahasaConfig("Adopsi Pengguna") %></td>
                                    <td><%= Common.getBahasaConfig("Admin, operator, pimpinan, guru/dosen, staf, siswa/mahasiswa, dan wali memiliki portal sesuai peran sehingga layanan digital lebih mudah diterima.") %></td>
                                </tr>
                                <tr>
                                    <td><%= Common.getBahasaConfig("Kontrol Keuangan") %></td>
                                    <td><%= Common.getBahasaConfig("Pembayaran, tagihan, piutang, transaksi POS, dan rekonsiliasi dapat dipantau lebih transparan melalui laporan dan dashboard.") %></td>
                                </tr>
                                <tr>
                                    <td><%= Common.getBahasaConfig("Kesiapan Audit") %></td>
                                    <td><%= Common.getBahasaConfig("Dokumen, log proses, approval, rekapitulasi, dan laporan pendukung tersimpan lebih rapi untuk kebutuhan audit, akreditasi, maupun evaluasi internal.") %></td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>


        <div id="section-portal" class="row justify-content-center mb-4">
            <div class="col-lg-12 col-xl-11">
                <div class="menu-panel">
                    <div class="text-center mb-4">
                        <div class="section-eyebrow">
                            <i class="fas fa-th-large"></i><%= Common.getBahasaConfig("Portal Demo Terpadu") %>
                        </div>
                        <h2 class="section-heading-premium mb-2">
                            <%= Common.getBahasaConfig("Jelajahi Ekosistem Enterprise Education Secara Langsung") %>
                        </h2>
                        <p class="section-lead-premium mb-0">
                            <%= Common.getBahasaConfig("Gunakan tombol di bawah untuk melihat simulasi layanan utama. Setiap portal disiapkan untuk memberi gambaran nyata bagaimana pengguna, operator, pimpinan, calon peserta didik, rekanan, dan unit layanan dapat bekerja dalam satu platform digital.") %>
                        </p>
                    </div>
                    
                    <div class="portal-group-section group-campus mb-4">
                        <div class="section-title">
                            <i class="fas fa-university me-2 text-primary"></i><%= Common.getBahasaConfig("Portal Perguruan Tinggi") %>
                        </div>
                        <div class="portal-support-copy">
                            <i class="fas fa-graduation-cap"></i>
                            <div>
                                <strong><%= Common.getBahasaConfig("Portal layanan digital untuk tata kelola perguruan tinggi yang lebih terintegrasi") %></strong>
                                <span><%= Common.getBahasaConfig("Kelompok menu ini disiapkan untuk memperlihatkan alur layanan kampus mulai dari login eCampus, penerimaan mahasiswa baru, tracer study, dokumen, dashboard, pustaka digital, repository, pelaporan, akreditasi, jurnal, hingga penelitian. Setiap layanan dirancang agar pimpinan, dosen, tenaga kependidikan, mahasiswa, alumni, dan unit pendukung dapat mengakses informasi secara lebih tertib dalam satu ekosistem.") %></span>
                            </div>
                        </div>
                        <div class="btn-custom-group portal-action-grid mt-3">
                            <a href="https://demo.ecampus.id/ecampus/login" data-module-id="login_ecampus" class="btn btn-primary shadow-sm trigger-modal">
                                <i class="fas fa-sign-in-alt"></i> <%= Common.getBahasaConfig("Login eCampus") %>
                            </a>
                            <a href="<%=Common.ROOT%>/alumni" data-module-id="tracer_study" class="btn btn-primary shadow-sm trigger-modal">
                                <i class="fas fa-briefcase"></i> <%= Common.getBahasaConfig("Tracer Study") %>
                            </a>
                            <a href="<%=Common.ROOT%>/pmb" data-module-id="pmb" class="btn btn-primary shadow-sm trigger-modal">
                                <i class="fas fa-user-plus"></i> <%= Common.getBahasaConfig("Mahasiswa Baru") %>
                            </a>
                            <a href="<%=Common.ROOT%>/document" data-module-id="dokumen" class="btn btn-primary shadow-sm trigger-modal">
                                <i class="fas fa-folder-open"></i> <%= Common.getBahasaConfig("Dokumen") %>
                            </a>
                            <a href="<%=Common.ROOT%>/dsh" data-module-id="dashboard" class="btn btn-primary shadow-sm trigger-modal">
                                <i class="fas fa-chart-pie"></i> <%= Common.getBahasaConfig("Dashboard") %>
                            </a>
                            <a href="<%=Common.ROOT%>/pustaka" data-module-id="pustaka" class="btn btn-primary shadow-sm trigger-modal">
                                <i class="fas fa-book"></i> <%= Common.getBahasaConfig("E-Library") %>
                            </a>
                            <a href="<%=Common.ROOT%>/repository" data-module-id="repository" class="btn btn-primary shadow-sm trigger-modal">
                                <i class="fas fa-archive"></i> <%= Common.getBahasaConfig("E-Repository") %>
                            </a>
                            
                            <a href="#" data-module-id="pelaporan" class="btn btn-outline-primary shadow-sm trigger-modal">
                                <i class="fas fa-sync-alt"></i> <%= Common.getBahasaConfig("Feeder & EMIS") %>
                            </a>
                            <a href="#" data-module-id="akreditasi" class="btn btn-outline-primary shadow-sm trigger-modal">
                                <i class="fas fa-certificate"></i> <%= Common.getBahasaConfig("Akreditasi") %>
                            </a>
                            <a href="#" data-module-id="ejournal" class="btn btn-outline-primary shadow-sm trigger-modal">
                                <i class="fas fa-newspaper"></i> <%= Common.getBahasaConfig("E-Journal") %>
                            </a>
                            <a href="#" data-module-id="simlitabmas" class="btn btn-outline-primary shadow-sm trigger-modal">
                                <i class="fas fa-microscope"></i> <%= Common.getBahasaConfig("Simlitabmas") %>
                            </a>
                        </div>
                        <div class="portal-support-note">
                            <i class="fas fa-check-circle me-2"></i><%= Common.getBahasaConfig("Portal perguruan tinggi membantu calon mitra melihat bagaimana layanan akademik, administrasi, publikasi, pelaporan, dan dashboard pimpinan dapat saling terhubung untuk mendukung keputusan yang lebih cepat dan berbasis data.") %>
                        </div>
                    </div>

                    <hr class="text-muted opacity-10 my-4">

                    <div class="portal-group-section group-school mb-4">
                        <div class="section-title">
                            <i class="fas fa-school me-2 text-dark"></i><%= Common.getBahasaConfig("Portal Sekolah dan Yayasan bisa juga Pondok Pesantren") %>
                        </div>
                        <div class="portal-support-copy">
                            <i class="fas fa-children"></i>
                            <div>
                                <strong><%= Common.getBahasaConfig("Portal operasional sekolah, yayasan, dan pesantren dalam satu halaman layanan") %></strong>
                                <span><%= Common.getBahasaConfig("Kelompok menu ini memberikan gambaran pengelolaan lembaga pendidikan mulai dari level yayasan, TK, SD, SMP, SMA, SMK, hingga kebutuhan pondok pesantren. Pengguna dapat melihat contoh portal admin dan layanan PPDB online yang membantu proses pendaftaran, administrasi peserta didik, koordinasi unit, serta pemantauan layanan pendidikan secara lebih rapi dan profesional.") %></span>
                            </div>
                        </div>
                        <div class="portal-mini-label"><i class="fas fa-door-open"></i><%= Common.getBahasaConfig("Portal Admin & Pengelola") %></div>
                        <div class="btn-custom-group portal-action-grid mt-3 mb-3">
                            <a href="https://yayasandemo.eschool.id/ecampus/login" data-module-id="yayasan" class="btn btn-dark shadow-sm trigger-modal">
                                <i class="fas fa-building"></i> <%= Common.getBahasaConfig("Pengelola Yayasan") %>
                            </a>
                            <a href="https://tk.eschool.id/ecampus/login" data-module-id="admin_tk" class="btn btn-outline-dark shadow-sm trigger-modal">
                                <i class="fas fa-shapes"></i> <%= Common.getBahasaConfig("Admin TK") %>
                            </a>
                            <a href="https://sd.eschool.id/ecampus/login" data-module-id="admin_sd" class="btn btn-outline-dark shadow-sm trigger-modal">
                                <i class="fas fa-child"></i> <%= Common.getBahasaConfig("Admin SD") %>
                            </a>
                            <a href="https://smp.eschool.id/ecampus/login" data-module-id="admin_smp" class="btn btn-outline-dark shadow-sm trigger-modal">
                                <i class="fas fa-school"></i> <%= Common.getBahasaConfig("Admin SMP") %>
                            </a>
                            <a href="https://sma.eschool.id/ecampus/login" data-module-id="admin_sma" class="btn btn-outline-dark shadow-sm trigger-modal">
                                <i class="fas fa-school-flag"></i> <%= Common.getBahasaConfig("Admin SMA") %>
                            </a>
                            <a href="https://smk.eschool.id/ecampus/login" data-module-id="admin_smk" class="btn btn-outline-dark shadow-sm trigger-modal">
                                <i class="fas fa-user-graduate"></i> <%= Common.getBahasaConfig("Admin SMK") %>
                            </a>
                        </div>

                        <div class="portal-mini-label"><i class="fas fa-user-plus"></i><%= Common.getBahasaConfig("PPDB Online") %></div>
                        <div class="btn-custom-group portal-action-grid mb-0">
                            <a href="https://tk.eschool.id/ecampus/ppdb" data-module-id="ppdb_tk" class="btn btn-outline-success shadow-sm trigger-modal">
                                <i class="fas fa-id-card"></i> <%= Common.getBahasaConfig("PPDB TK") %>
                            </a>
                            <a href="https://sd.eschool.id/ecampus/ppdb" data-module-id="ppdb_sd" class="btn btn-outline-success shadow-sm trigger-modal">
                                <i class="fas fa-id-card"></i> <%= Common.getBahasaConfig("PPDB SD") %>
                            </a>
                            <a href="https://smp.eschool.id/ecampus/ppdb" data-module-id="ppdb_smp" class="btn btn-outline-success shadow-sm trigger-modal">
                                <i class="fas fa-id-card"></i> <%= Common.getBahasaConfig("PPDB SMP") %>
                            </a>
                            <a href="https://sma.eschool.id/ecampus/ppdb" data-module-id="ppdb_sma" class="btn btn-outline-success shadow-sm trigger-modal">
                                <i class="fas fa-id-card"></i> <%= Common.getBahasaConfig("PPDB SMA") %>
                            </a>
                            <a href="https://smk.eschool.id/ecampus/ppdb" data-module-id="ppdb_smk" class="btn btn-outline-success shadow-sm trigger-modal">
                                <i class="fas fa-id-card"></i> <%= Common.getBahasaConfig("PPDB SMK") %>
                            </a>
                        </div>
                        <div class="portal-support-note">
                            <i class="fas fa-check-circle me-2"></i><%= Common.getBahasaConfig("Portal sekolah, yayasan, dan pesantren dirancang agar layanan administrasi, PPDB, keuangan, komunikasi, dan monitoring lintas unit dapat berjalan lebih konsisten, terukur, dan mudah dikenalkan kepada seluruh pemangku kepentingan.") %>
                        </div>
                    </div>
                    
                    <hr class="text-muted opacity-10 my-4">

                    <div class="portal-group-section group-support mb-0">
                        <div class="section-title">
                            <i class="fas fa-layer-group me-2 text-info"></i><%= Common.getBahasaConfig("Sistem Pendukung") %>
                        </div>
                        <div class="portal-support-copy">
                            <i class="fas fa-puzzle-piece"></i>
                            <div>
                                <strong><%= Common.getBahasaConfig("Layanan pendukung yang memperluas ekosistem digital institusi") %></strong>
                                <span><%= Common.getBahasaConfig("Selain modul inti akademik, keuangan, SDM, dan administrasi, Enterprise Education juga menyediakan layanan pendukung untuk operasional harian, unit usaha, perpustakaan, anjungan mandiri, rekanan, publikasi lowongan kerja, pengelolaan karir, hingga kontrol layanan antar jemput peserta didik dan pegawai secara lebih tertib.") %></span>
                            </div>
                        </div>
                        <div class="btn-custom-group portal-action-grid mt-3 mb-0">
                            <a href="<%=Common.ROOT%>/kantin" data-module-id="ekantin" class="btn btn-primary shadow-sm trigger-modal">
                                <i class="fas fa-store"></i> <%= Common.getBahasaConfig("eKantin") %>
                            </a>
                            <a href="<%=Common.ROOT%>/anjungan" data-module-id="anjungan" class="btn btn-primary shadow-sm trigger-modal">
                                <i class="fas fa-desktop"></i> <%= Common.getBahasaConfig("Anjungan") %>
                            </a>
                            <a href="<%=Common.ROOT%>/pos" data-module-id="pos" class="btn btn-primary shadow-sm trigger-modal">
                                <i class="fas fa-cash-register"></i> <%= Common.getBahasaConfig("Point Of Sale") %>
                            </a>
                            <a href="<%=Common.ROOT%>/krrs" data-module-id="kursus" class="btn btn-primary shadow-sm trigger-modal">
                                <i class="fas fa-chalkboard-teacher"></i> <%= Common.getBahasaConfig("Sistem Kursus") %>
                            </a>
                            <a href="<%=Common.ROOT%>/les" data-module-id="les" class="btn btn-primary shadow-sm trigger-modal">
                                <i class="fas fa-user-edit"></i> <%= Common.getBahasaConfig("Sistem Les / Private") %>
                            </a>
                            <a href="<%=Common.ROOT%>/karir" data-module-id="karir" class="btn btn-primary shadow-sm trigger-modal portal-btn-featured">
                                <i class="fas fa-user-tie"></i> <%= Common.getBahasaConfig("Lowongan Pekerjaan / Karir") %>
                            </a>
                            <a href="<%=Common.ROOT+"/antarJemput"%>" data-module-id="antar_jemput" class="btn btn-primary shadow-sm trigger-modal portal-btn-featured">
                                <i class="fas fa-bus-alt"></i> <%= Common.getBahasaConfig("Antar Jemput") %>
                            </a>
                            <a href="<%=Common.ROOT%>/welpus" data-module-id="pengunjung_pustaka" class="btn btn-primary shadow-sm trigger-modal">
                                <i class="fas fa-address-book"></i> <%= Common.getBahasaConfig("Pengunjung Pustaka") %>
                            </a>
                            <a href="<%=Common.ROOT%>/tamu" data-module-id="buku_tamu" class="btn btn-primary shadow-sm trigger-modal">
                                <i class="fas fa-book-open"></i> <%= Common.getBahasaConfig("Buku Tamu") %>
                            </a>
                            <a href="<%=Common.ROOT%>/welsis" data-module-id="absen_siswa" class="btn btn-primary shadow-sm trigger-modal">
                                <i class="fas fa-user-clock"></i> <%= Common.getBahasaConfig("Absen Siswa") %>
                            </a>
                            <a href="<%=Common.ROOT%>/vendor" data-module-id="portal_rekanan" class="btn btn-primary shadow-sm trigger-modal">
                                <i class="fas fa-truck-loading"></i> <%= Common.getBahasaConfig("Portal Rekanan") %>
                            </a>
                        </div>
                        <div class="portal-support-note">
                            <i class="fas fa-lightbulb me-2"></i><%= Common.getBahasaConfig("Setiap layanan pendukung dapat digunakan sebagai pintu masuk digital bagi pengguna internal maupun eksternal. Modul seperti antar jemput, karir, anjungan, eKantin, POS, buku tamu, dan rekanan membantu institusi tampil lebih profesional, mudah diakses, aman, dan siap memberikan pengalaman layanan yang konsisten dari satu halaman terpadu.") %>
                        </div>
                        <div class="portal-support-note text-start">
                            <i class="fas fa-route me-2"></i><%= Common.getBahasaConfig("Fitur Antar Jemput menata alur mulai dari kendaraan, sopir, kenek, rute, jadwal, peserta, kartu penjemput, tap/scan barcode di gerbang, monitor antrian, pengumuman soundbox ke kelas, hingga konfirmasi serah terima oleh petugas.") %>
                        </div>
                    </div>

                    <hr class="text-muted opacity-10 my-4">

                    <div class="portal-group-section group-health mb-4">
                        <div class="section-title">
                            <i class="fas fa-hospital me-2 text-primary"></i><%= Common.getBahasaConfig("Portal Layanan Kesehatan: Rumah Sakit, Klinik & Puskesmas (eMedic)") %>
                        </div>
                        <div class="portal-support-copy">
                            <i class="fas fa-notes-medical"></i>
                            <div>
                                <strong><%= Common.getBahasaConfig("Satu modul untuk seluruh kebutuhan layanan kesehatan Anda") %></strong>
                                <span><%= Common.getBahasaConfig("eMedic mencakup pendaftaran pasien (rawat jalan, rawat inap, UGD), rekam medis, farmasi & racikan, manajemen stok obat, transaksi/billing, hingga dashboard analitik. Modul ini terintegrasi langsung bagi Universitas, Sekolah, atau Pondok Pesantren yang memiliki klinik/rumah sakit sendiri, dan dapat pula diimplementasikan berdiri sendiri (standalone) untuk Rumah Sakit, Klinik, Puskesmas, atau layanan fasilitas kesehatan lainnya.") %></span>
                            </div>
                        </div>
                        <div class="btn-custom-group portal-action-grid mt-3 mb-0">
                            <a href="https://apps.emedik.id" target="_blank" class="btn btn-success shadow-sm">
                                <i class="fas fa-desktop"></i> <%= Common.getBahasaConfig("Coba Demo eMedic") %>
                            </a>
                            <a href="<%=Common.ROOT%>/penawaran?modul=kesehatan<%=(appendParams.isEmpty() ? "" : "&" + appendParams.substring(1))%>" target="_blank" class="btn btn-primary shadow-sm">
                                <i class="fas fa-envelope-open-text"></i> <%= Common.getBahasaConfig("Surat Penawaran") %>
                            </a>
                            <a href="<%=Common.ROOT%>/presentasi?modul=kesehatan<%=(appendParams.isEmpty() ? "" : "&" + appendParams.substring(1))%>" target="_blank" class="btn btn-primary shadow-sm">
                                <i class="fas fa-display"></i> <%= Common.getBahasaConfig("Presentasi") %>
                            </a>
                            <a href="<%=Common.ROOT%>/proposal?modul=kesehatan<%=(appendParams.isEmpty() ? "" : "&" + appendParams.substring(1))%>" target="_blank" class="btn btn-primary shadow-sm">
                                <i class="fas fa-file-alt"></i> <%= Common.getBahasaConfig("Proposal") %>
                            </a>
                            <a href="<%=Common.ROOT%>/pks?modul=kesehatan<%=(appendParams.isEmpty() ? "" : "&" + appendParams.substring(1))%>" target="_blank" class="btn btn-primary shadow-sm">
                                <i class="fas fa-handshake"></i> <%= Common.getBahasaConfig("Draft PKS") %>
                            </a>
                        </div>
                        <div class="portal-support-note">
                            <i class="fas fa-key me-2"></i><%= Common.getBahasaConfig("Akun Demo eMedic") %> &mdash; <%= Common.getBahasaConfig("Username") %>: <strong>demo</strong> &nbsp;|&nbsp; <%= Common.getBahasaConfig("Password") %>: <strong>demo123</strong>
                        </div>
                        <div class="portal-support-note">
                            <i class="fas fa-check-circle me-2"></i><%= Common.getBahasaConfig("Dokumen di atas menjelaskan modul eMedic secara khusus: alur registrasi hingga farmasi, dashboard analitik, roadmap integrasi BPJS/SATUSEHAT, serta tiga pilihan skema investasi (Beli Putus, Langganan Bulanan, atau Bagi Hasil per Transaksi).") %>
                        </div>
                    </div>

                    <hr class="text-muted opacity-10 my-4">

                    <div class="portal-group-section group-warehouse mb-4">
                        <div class="section-title">
                            <i class="fas fa-warehouse me-2 text-primary"></i><%= Common.getBahasaConfig("Portal Sistem Gudang Pusat, Outlet, POS, Ekspedisi, Investor & Akuntansi") %>
                        </div>
                        <div class="portal-support-copy">
                            <i class="fas fa-boxes-stacked"></i>
                            <div>
                                <strong><%= Common.getBahasaConfig("Satu modul untuk seluruh kebutuhan operasional gudang & bisnis ritel Anda") %></strong>
                                <span><%= Common.getBahasaConfig("Mencakup Gudang Pusat dan Gudang Cabang, banyak Outlet/Toko, Kasir (POS) yang tetap dapat mencatat transaksi walau internet terputus, distribusi/Ekspedisi antar lokasi, skema bagi hasil Investor yang dapat dikonfigurasi, hingga Akuntansi dengan jurnal otomatis dan HPP yang dihitung langsung dari resep/bahan baku. Modul ini terintegrasi langsung bagi Universitas, Sekolah, atau Pondok Pesantren yang memiliki koperasi/kantin/toko/unit usaha sendiri, dan dapat pula diimplementasikan berdiri sendiri (standalone) untuk usaha ritel, jaringan outlet, atau koperasi lainnya.") %></span>
                            </div>
                        </div>
                        <div class="btn-custom-group portal-action-grid mt-3 mb-0">
                            <a href="<%=Common.ROOT%>/penawaran?modul=gudang<%=(appendParams.isEmpty() ? "" : "&" + appendParams.substring(1))%>" target="_blank" class="btn btn-primary shadow-sm">
                                <i class="fas fa-envelope-open-text"></i> <%= Common.getBahasaConfig("Surat Penawaran") %>
                            </a>
                            <a href="<%=Common.ROOT%>/presentasi?modul=gudang<%=(appendParams.isEmpty() ? "" : "&" + appendParams.substring(1))%>" target="_blank" class="btn btn-primary shadow-sm">
                                <i class="fas fa-display"></i> <%= Common.getBahasaConfig("Presentasi") %>
                            </a>
                            <a href="<%=Common.ROOT%>/proposal?modul=gudang<%=(appendParams.isEmpty() ? "" : "&" + appendParams.substring(1))%>" target="_blank" class="btn btn-primary shadow-sm">
                                <i class="fas fa-file-alt"></i> <%= Common.getBahasaConfig("Proposal") %>
                            </a>
                            <a href="<%=Common.ROOT%>/pks?modul=gudang<%=(appendParams.isEmpty() ? "" : "&" + appendParams.substring(1))%>" target="_blank" class="btn btn-primary shadow-sm">
                                <i class="fas fa-handshake"></i> <%= Common.getBahasaConfig("Draft PKS") %>
                            </a>
                        </div>
                        <div class="portal-support-note">
                            <i class="fas fa-check-circle me-2"></i><%= Common.getBahasaConfig("Dokumen di atas menjelaskan modul Gudang/POS/Ekspedisi/Investor/Akuntansi secara khusus: alur gudang pusat-cabang hingga kasir & distribusi, HPP otomatis, roadmap Ekspedisi & Investor, serta rincian lengkap skema harga berlangganan Kasir (POS) per Outlet dan Paket Modul Pusat, mulai dari Rp249.000 per Outlet per bulan.") %>
                        </div>
                    </div>

                    <hr class="text-muted opacity-10 my-4">

                    <div class="portal-group-section group-warehouse mb-4" style="border-color: rgba(234, 88, 12, 0.18);">
                        <div class="section-title">
                            <i class="fas fa-mobile-screen-button me-2" style="color:#ea580c;"></i><%= Common.getBahasaConfig("Aplikasi Pendamping: Kasir & Stok Opname yang Siap Diunduh Hari Ini") %>
                        </div>
                        <div class="portal-support-copy">
                            <i class="fas fa-rocket"></i>
                            <div>
                                <strong><%= Common.getBahasaConfig("Bukan sekadar rencana — 3 aplikasi klien ini sudah jadi, sudah dirilis, dan siap dipasang di perangkat Anda sekarang juga") %></strong>
                                <span><%= Common.getBahasaConfig("Seluruh modul Kasir (POS) & Gudang di atas terasa langsung di tangan lewat 3 aplikasi klien mandiri berikut, semuanya terhubung ke server AIS yang sama sehingga data selalu konsisten di manapun kasir bertugas — di meja kasir, di tablet toko, maupun saat tim gudang keliling menghitung stok.") %></span>
                            </div>
                        </div>
                        <div class="row g-3 mt-1">
                            <div class="col-md-4">
                                <div class="portal-support-copy" style="align-items:flex-start;">
                                    <i class="fas fa-desktop" style="color:#ea580c;"></i>
                                    <div>
                                        <strong><%= Common.getBahasaConfig("POS Kasir Desktop") %></strong>
                                        <span><%= Common.getBahasaConfig("Kasir andal untuk PC/laptop Windows di meja kasir utama — tetap melayani pembeli walau internet sempat putus (transaksi tersimpan otomatis, sinkron begitu koneksi pulih), lengkap dengan Layar Pelanggan kedua (dual monitor) yang menampilkan rincian belanja secara langsung ke pembeli, pembayaran saldo member dengan verifikasi PIN, top-up saldo langsung dari kasir, lebih dari 150 laporan siap pakai, dan pembaruan aplikasi otomatis tanpa perlu instal ulang manual.") %></span>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-4">
                                <div class="portal-support-copy" style="align-items:flex-start;">
                                    <i class="fas fa-tablet-screen-button" style="color:#ea580c;"></i>
                                    <div>
                                        <strong><%= Common.getBahasaConfig("POS Kasir Android") %></strong>
                                        <span><%= Common.getBahasaConfig("Mesin kasir dalam genggaman — satu aplikasi yang tampilannya otomatis menyesuaikan diri, cocok dipakai di tablet (mis. Galaxy Tab) untuk kasir tetap maupun HP Android biasa untuk kasir keliling/darurat. Mencetak struk langsung ke printer thermal Bluetooth portable, tetap bisa berjualan tanpa sinyal (transaksi diantre otomatis di perangkat), dan membawa fitur inti yang sama dengan versi Desktop: member & saldo, verifikasi PIN, buka-tutup kas, hingga ringkasan penjualan harian.") %></span>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-4">
                                <div class="portal-support-copy" style="align-items:flex-start;">
                                    <i class="fas fa-barcode" style="color:#ea580c;"></i>
                                    <div>
                                        <strong><%= Common.getBahasaConfig("Stok Opname Android") %></strong>
                                        <span><%= Common.getBahasaConfig("Aplikasi ringkas khusus untuk perhitungan stok fisik ala supermarket/minimarket, dirancang mengikuti praktik terbaik yang dipakai ritel modern di lapangan — pindai barcode memakai alat pemindai/PDT genggam (plug-and-play) atau cukup kamera HP bila alat pemindai tidak tersedia, lalu hasilnya langsung tersinkron ke server. Sengaja fokus pada satu tugas saja (entry stok opname) supaya tim lapangan cepat mahir memakainya dan proses stok opname banyak outlet/gudang selesai jauh lebih cepat.") %></span>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="btn-custom-group portal-action-grid mt-3 mb-0">
                            <a href="https://github.com/Zishof/ais-pos-kasir-desktop/releases" target="_blank" rel="noopener" class="btn btn-primary shadow-sm">
                                <i class="fab fa-github"></i> <%= Common.getBahasaConfig("Unduh POS Kasir Desktop") %>
                            </a>
                            <a href="https://github.com/Zishof/ais-pos-kasir-android/releases" target="_blank" rel="noopener" class="btn btn-primary shadow-sm">
                                <i class="fab fa-github"></i> <%= Common.getBahasaConfig("Unduh POS Kasir Android") %>
                            </a>
                            <a href="https://github.com/Zishof/ais-stok-opname-android/releases" target="_blank" rel="noopener" class="btn btn-primary shadow-sm">
                                <i class="fab fa-github"></i> <%= Common.getBahasaConfig("Unduh Stok Opname Android") %>
                            </a>
                        </div>
                        <div class="portal-support-note">
                            <i class="fas fa-check-circle me-2"></i><%= Common.getBahasaConfig("Ketiganya gratis diunduh dan dipasang — cukup arahkan ke alamat server AIS yang sama dengan sistem Anda yang sudah berjalan, tanpa server tambahan apa pun.") %>
                        </div>
                    </div>

                    <hr class="text-muted opacity-10 my-4">

                    <div class="portal-group-section group-docs mb-0">
                        <div class="section-title">
                            <i class="fas fa-file-pdf me-2 text-danger"></i><%= Common.getBahasaConfig("Materi Penawaran & Presentasi") %>
                        </div>
                        <div class="portal-support-copy">
                            <i class="fas fa-file-signature"></i>
                            <div>
                                <strong><%= Common.getBahasaConfig("Dokumen resmi untuk membantu proses evaluasi, presentasi, dan pengambilan keputusan") %></strong>
                                <span><%= Common.getBahasaConfig("Bagian ini menyediakan akses cepat menuju materi penawaran, presentasi, proposal, dan draf PKS. Seluruh dokumen disiapkan untuk memudahkan calon mitra memahami ruang lingkup solusi, skema investasi, pendekatan implementasi, serta kerangka kerja sama secara lebih formal dan siap dibahas bersama tim internal.") %></span>
                            </div>
                        </div>
                        <div class="btn-custom-group portal-action-grid mt-3 mb-0">
                            <a href="<%=Common.ROOT%>/penawaran<%=appendParams%>" target="_blank" class="btn btn-danger shadow-sm">
                                <i class="fas fa-envelope-open-text"></i> <%= Common.getBahasaConfig("Surat Penawaran") %>
                            </a>
                            <a href="<%=Common.ROOT%>/presentasi<%=appendParams%>" target="_blank" class="btn btn-danger shadow-sm">
                                <i class="fas fa-project-diagram"></i> <%= Common.getBahasaConfig("Presentasi") %>
                            </a>
                            <a href="<%=Common.ROOT%>/proposal<%=appendParams%>" target="_blank" class="btn btn-danger shadow-sm">
                                <i class="fas fa-file-alt"></i> <%= Common.getBahasaConfig("Proposal") %>
                            </a>
                            <a href="<%=Common.ROOT%>/pks<%=appendParams%>" target="_blank" class="btn btn-danger shadow-sm">
                                <i class="fas fa-handshake"></i> <%= Common.getBahasaConfig("Draft PKS") %>
                            </a>
                        </div>
                        
                        <div class="portal-support-note text-start">
                            <i class="fas fa-info-circle me-2"></i>
                            <strong><%= Common.getBahasaConfig("Informasi Pembiayaan (Pricing):") %></strong> <%= Common.getBahasaConfig("Untuk mengetahui rincian lengkap skema biaya pengadaan sistem (Software) maupun perangkat keras (Hardware Anjungan/POS), silakan klik dokumen Proposal atau Presentasi di atas. Dokumen tersebut dapat digunakan sebagai bahan awal untuk diskusi manajemen, rapat pimpinan, maupun penyusunan rencana kerja sama.") %>
                        </div>
                    </div>

                </div>
            </div>
        </div>

        <div id="section-about" class="row justify-content-center mb-5">
            <div class="col-lg-12 col-xl-11">
                <div class="glass-intro p-4 p-lg-5 text-start mb-0">
                    
                    <div class="text-center mb-5">
                        <h2 class="fw-bold mb-3" style="color: var(--primary-color);">
                            <i class="fas fa-network-wired me-2"></i><%= Common.getBahasaConfig("Enterprise Education: Ekosistem Terpadu") %>
                        </h2>
                        <p class="text-secondary mx-auto" style="max-width: 850px; line-height: 1.8; font-size: 1.05rem;">
                            <%= Common.getBahasaConfig("Enterprise Education menghadirkan pendekatan All-in-One yang mengintegrasikan seluruh modul dan fitur esensial ke dalam satu aplikasi tunggal didukung oleh fleksibilitas infrastruktur Solusi Dual Model (Cloud, On-Premise, maupun Hybrid). Institusi Anda tidak perlu lagi bergantung pada berbagai sistem terpisah yang rumit.") %>
                        </p>
                    </div>

                    <div class="row g-4 mb-4">
                        <div class="col-lg-4">
                            <div class="p-4 h-100 shadow-sm" style="background: rgba(255,255,255,0.7); border-radius: 16px; border: 1px solid rgba(13, 110, 253, 0.2);">
                                <div class="ratio ratio-16x9 video-wrapper mb-3">
                                    <iframe 
                                        src="https://www.youtube.com/embed/pgGbWFeukq8" 
                                        title="<%= Common.getBahasaConfig("Video Pengenalan eCampus") %>" 
                                        allowfullscreen 
                                        loading="lazy"
                                    ></iframe>
                                </div>
                                <h5 class="fw-bold text-dark mb-3">
                                    <i class="fas fa-university text-primary me-2"></i><%= Common.getBahasaConfig("Apa itu eCampus?") %>
                                </h5>
                                <p class="text-secondary mb-0" style="line-height: 1.7; text-align: justify;">
                                    <%= Common.getBahasaConfig("eCampus merupakan platform enterprise berbasis web dan seluler untuk pengelolaan administrasi dan akademik perguruan tinggi. Lebih dari sekadar sistem informasi, eCampus adalah ekosistem digital yang mensinergikan mahasiswa, dosen, pimpinan, dan alumni dalam lingkungan yang kolaboratif.") %>
                                </p>
                            </div>
                        </div>
                        
                        <div class="col-lg-4">
                            <div class="p-4 h-100 shadow-sm" style="background: rgba(255,255,255,0.7); border-radius: 16px; border: 1px solid rgba(13, 110, 253, 0.2);">
                                <div class="ratio ratio-16x9 video-wrapper mb-3">
                                    <iframe 
                                        src="https://www.youtube.com/embed/Eb4A5PAY9-k" 
                                        title="<%= Common.getBahasaConfig("Video Pengenalan eSchool") %>" 
                                        allowfullscreen 
                                        loading="lazy"
                                    ></iframe>
                                </div>
                                <h5 class="fw-bold text-dark mb-3">
                                    <i class="fas fa-school text-primary me-2"></i><%= Common.getBahasaConfig("Apa itu eSchool?") %>
                                </h5>
                                <p class="text-secondary mb-0" style="line-height: 1.7; text-align: justify;">
                                    <%= Common.getBahasaConfig("eSchool dirancang khusus sebagai aplikasi mutakhir untuk manajemen sekolah tingkat dasar dan menengah. Platform ini mengubah cara sekolah dikelola secara struktural untuk meningkatkan efisiensi, memfasilitasi keterlibatan orang tua, serta mendukung layanan pendidikan yang transparan.") %>
                                </p>
                            </div>
                        </div>
                        
                        <div class="col-lg-4">
                            <div class="p-4 h-100 shadow-sm" style="background: rgba(255,255,255,0.7); border-radius: 16px; border: 1px solid rgba(13, 110, 253, 0.2);">
                                <div class="ratio ratio-16x9 video-wrapper mb-3">
                                    <iframe 
                                        src="https://www.youtube.com/embed/HtmwgFYtWao" 
                                        title="<%= Common.getBahasaConfig("Video Pengenalan ePesantren") %>" 
                                        allowfullscreen 
                                        loading="lazy"
                                    ></iframe>
                                </div>
                                <h5 class="fw-bold text-dark mb-3">
                                    <i class="fas fa-mosque text-primary me-2"></i><%= Common.getBahasaConfig("Apa itu ePesantren?") %>
                                </h5>
                                <p class="text-secondary mb-0" style="line-height: 1.7; text-align: justify;">
                                    <%= Common.getBahasaConfig("SMART e-Pesantren adalah sistem ERP terpadu yang dikustomisasi untuk menjawab kebutuhan unik pondok pesantren. Memadukan standar teknologi modern dengan upaya pelestarian tradisi keilmuan Islam, memastikan transparansi keuangan dan administrasi yang akurat.") %>
                                </p>
                            </div>
                        </div>
                    </div>

                    <!-- Panel eMedic: Modul Rumah Sakit / Klinik / Puskesmas (terintegrasi atau berdiri sendiri) -->
                    <div class="p-4 p-lg-5 mt-2" style="background: linear-gradient(135deg, rgba(2,132,199,0.06), rgba(255,255,255,0.9)); border-radius: 16px; border: 1px solid rgba(2, 132, 199, 0.18);">
                        <div class="row align-items-center g-4 mb-4">
                            <div class="col-lg-2 text-center">
                                <i class="fas fa-hospital fa-4x" style="color: var(--primary-color);"></i>
                            </div>
                            <div class="col-lg-10">
                                <h5 class="fw-bold text-dark mb-3" style="font-size: 1.4rem;">
                                    <span class="badge bg-primary bg-opacity-10 text-primary border border-primary border-opacity-25 me-2" style="font-size: 0.7rem; letter-spacing: 0.05em;">BARU</span>
                                    <%= Common.getBahasaConfig("Apa itu eMedic? (Penjelasan Sederhana)") %>
                                </h5>
                                <p class="text-secondary mb-3" style="line-height: 1.85; text-align: justify; font-size: 1.03rem;">
                                    <%= Common.getBahasaConfig("Secara sederhana, eMedic adalah sebuah program komputer (aplikasi) yang membantu Rumah Sakit, Klinik, dan Puskesmas mengurus seluruh kegiatan sehari-hari secara digital — mulai dari saat pasien mendaftar di loket, diperiksa dokter, diberi resep obat, hingga membayar di kasir. Semua kegiatan tersebut, yang sebelumnya dicatat secara terpisah di buku, kertas resep, dan nota pembayaran, kini tercatat secara otomatis dalam satu sistem yang sama, sehingga tidak ada data yang tercecer atau harus dicatat berulang kali.") %>
                                </p>
                                <p class="text-secondary mb-0" style="line-height: 1.85; text-align: justify; font-size: 1.03rem;">
                                    <%= Common.getBahasaConfig("eMedic merupakan bagian resmi dari ekosistem Enterprise Education. Oleh karena itu, apabila Universitas (eCampus), Sekolah (eSchool), atau Pondok Pesantren (ePesantren) Bapak/Ibu telah menggunakan sistem kami dan memiliki klinik atau layanan kesehatan sendiri di lingkungan kampus/sekolah/pesantren, eMedic dapat langsung dipasang dan terhubung tanpa membangun sistem baru dari awal. Namun demikian, eMedic juga sepenuhnya dapat digunakan secara mandiri (berdiri sendiri) oleh Rumah Sakit, Klinik, Puskesmas, maupun fasilitas layanan kesehatan lain yang tidak memiliki keterkaitan dengan institusi pendidikan sama sekali.") %>
                                </p>
                            </div>
                        </div>

                        <div class="row g-3 mb-4">
                            <div class="col-md-3 col-6">
                                <div class="text-center p-3 h-100" style="background: #ffffff; border-radius: 14px; border: 1px solid rgba(2,132,199,0.15);">
                                    <i class="fas fa-user-injured fa-2x text-primary mb-2 d-block"></i>
                                    <div class="fw-bold text-dark small"><%= Common.getBahasaConfig("1. Pasien Datang & Didaftar") %></div>
                                    <div class="text-secondary" style="font-size: 0.85rem;"><%= Common.getBahasaConfig("Petugas cukup mengetik nama pasien, sistem otomatis memberi nomor antrean dan memilihkan dokter/poli.") %></div>
                                </div>
                            </div>
                            <div class="col-md-3 col-6">
                                <div class="text-center p-3 h-100" style="background: #ffffff; border-radius: 14px; border: 1px solid rgba(2,132,199,0.15);">
                                    <i class="fas fa-stethoscope fa-2x text-primary mb-2 d-block"></i>
                                    <div class="fw-bold text-dark small"><%= Common.getBahasaConfig("2. Diperiksa Dokter") %></div>
                                    <div class="text-secondary" style="font-size: 0.85rem;"><%= Common.getBahasaConfig("Dokter mencatat hasil pemeriksaan dan resep langsung di layar komputer, tanpa tulisan tangan yang sulit dibaca.") %></div>
                                </div>
                            </div>
                            <div class="col-md-3 col-6">
                                <div class="text-center p-3 h-100" style="background: #ffffff; border-radius: 14px; border: 1px solid rgba(2,132,199,0.15);">
                                    <i class="fas fa-mortar-pestle fa-2x text-primary mb-2 d-block"></i>
                                    <div class="fw-bold text-dark small"><%= Common.getBahasaConfig("3. Diberi Obat") %></div>
                                    <div class="text-secondary" style="font-size: 0.85rem;"><%= Common.getBahasaConfig("Resep otomatis muncul di layar petugas farmasi, stok obat langsung berkurang, tidak perlu dicatat ulang.") %></div>
                                </div>
                            </div>
                            <div class="col-md-3 col-6">
                                <div class="text-center p-3 h-100" style="background: #ffffff; border-radius: 14px; border: 1px solid rgba(2,132,199,0.15);">
                                    <i class="fas fa-cash-register fa-2x text-primary mb-2 d-block"></i>
                                    <div class="fw-bold text-dark small"><%= Common.getBahasaConfig("4. Membayar di Kasir") %></div>
                                    <div class="text-secondary" style="font-size: 0.85rem;"><%= Common.getBahasaConfig("Seluruh biaya (obat, tindakan, kamar) sudah terjumlah otomatis menjadi satu tagihan, tidak perlu dihitung manual.") %></div>
                                </div>
                            </div>
                        </div>

                        <div class="row g-4">
                            <div class="col-lg-6">
                                <h6 class="fw-bold text-dark mb-2"><i class="fas fa-users me-2 text-primary"></i><%= Common.getBahasaConfig("Untuk Siapa eMedic Ini?") %></h6>
                                <ul class="text-secondary mb-0" style="line-height: 1.9; padding-left: 1.2rem; font-size: 0.98rem;">
                                    <li><%= Common.getBahasaConfig("Klinik umum maupun klinik spesialis (praktik dokter bersama).") %></li>
                                    <li><%= Common.getBahasaConfig("Puskesmas, baik yang tanpa rawat inap maupun dengan rawat inap, termasuk yang berada di wilayah terpencil.") %></li>
                                    <li><%= Common.getBahasaConfig("Rumah Sakit dari kelas terkecil (RS Pratama) hingga Rumah Sakit besar (Kelas A).") %></li>
                                    <li><%= Common.getBahasaConfig("Klinik/poliklinik milik Universitas, Sekolah, atau Pondok Pesantren yang ingin melayani mahasiswa, siswa, santri, dan pegawainya secara lebih tertib.") %></li>
                                </ul>
                            </div>
                            <div class="col-lg-6">
                                <h6 class="fw-bold text-dark mb-2"><i class="fas fa-hand-holding-medical me-2 text-primary"></i><%= Common.getBahasaConfig("Apa Manfaatnya bagi Fasilitas Kesehatan?") %></h6>
                                <ul class="text-secondary mb-0" style="line-height: 1.9; padding-left: 1.2rem; font-size: 0.98rem;">
                                    <li><%= Common.getBahasaConfig("Riwayat kesehatan pasien tersimpan rapi dan mudah dicari kembali kapan saja, tanpa harus mencari-cari berkas kertas lama.") %></li>
                                    <li><%= Common.getBahasaConfig("Stok obat selalu terpantau, sehingga obat tidak mudah habis mendadak atau kadaluarsa tanpa diketahui.") %></li>
                                    <li><%= Common.getBahasaConfig("Tagihan pasien selalu sesuai dengan obat dan tindakan yang benar-benar diberikan, mengurangi risiko selisih kas.") %></li>
                                    <li><%= Common.getBahasaConfig("Pimpinan fasilitas dapat melihat laporan kunjungan, pendapatan, dan ketersediaan tempat tidur hanya dengan membuka satu layar dashboard.") %></li>
                                </ul>
                            </div>
                        </div>

                        <div class="mt-4 p-3 p-lg-4" style="background: rgba(255,255,255,0.75); border-radius: 14px; border: 1px dashed rgba(2,132,199,0.35);">
                            <h6 class="fw-bold text-dark mb-3"><i class="fas fa-tags me-2 text-primary"></i><%= Common.getBahasaConfig("Gambaran Umum Biaya (Estimasi Awal, Seluruhnya Dapat Dinegosiasikan)") %></h6>
                            <p class="text-secondary mb-3" style="font-size: 0.95rem; line-height: 1.7;">
                                <%= Common.getBahasaConfig("Kami memahami bahwa setiap fasilitas kesehatan memiliki kemampuan anggaran yang berbeda-beda. Oleh sebab itu, kami mengutamakan tercapainya kesepakatan dan cepatnya sistem ini dapat mulai digunakan, dibandingkan kekakuan pada angka harga. Berikut gambaran biaya beli-putus (sekali bayar, sistem menjadi milik selamanya) sebagai titik awal diskusi:") %>
                            </p>
                            <div class="table-responsive">
                                <table class="table table-sm table-bordered align-middle mb-2" style="font-size: 0.88rem; background: #fff;">
                                    <thead class="table-light">
                                        <tr>
                                            <th><%= Common.getBahasaConfig("Jenis Fasilitas") %></th>
                                            <th class="text-end"><%= Common.getBahasaConfig("Mulai Dari") %></th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr><td><%= Common.getBahasaConfig("Klinik") %></td><td class="text-end fw-bold">Rp100.000.000</td></tr>
                                        <tr><td><%= Common.getBahasaConfig("Puskesmas (nonrawat inap s.d. sangat terpencil)") %></td><td class="text-end fw-bold">Rp150.000.000 &ndash; Rp500.000.000</td></tr>
                                        <tr><td><%= Common.getBahasaConfig("Rumah Sakit Pratama") %></td><td class="text-end fw-bold">Rp250.000.000</td></tr>
                                        <tr><td><%= Common.getBahasaConfig("Rumah Sakit Kelas D / C / B / A") %></td><td class="text-end fw-bold">Rp400.000.000 &ndash; Rp2.750.000.000</td></tr>
                                    </tbody>
                                </table>
                            </div>
                            <p class="text-secondary mb-0" style="font-size: 0.9rem; line-height: 1.7;">
                                <%= Common.getBahasaConfig("Selain beli putus, tersedia pula opsi Langganan Bulanan (tanpa perlu membayar besar di muka) dan opsi Fee Layanan Sistem yang dihitung dari jumlah kunjungan pasien (tanpa biaya di muka sama sekali). Rincian lengkap seluruh skema, termasuk contoh perhitungan dan penjelasan tiap istilah, dapat dibaca pada dokumen") %>
                                <a href="<%=Common.ROOT%>/proposal?modul=kesehatan<%=(appendParams.isEmpty() ? "" : "&" + appendParams.substring(1))%>" target="_blank" class="fw-bold"><%= Common.getBahasaConfig("Proposal eMedic") %></a>
                                <%= Common.getBahasaConfig("atau") %>
                                <a href="<%=Common.ROOT%>/penawaran?modul=kesehatan<%=(appendParams.isEmpty() ? "" : "&" + appendParams.substring(1))%>" target="_blank" class="fw-bold"><%= Common.getBahasaConfig("Surat Penawaran eMedic") %></a>.
                            </p>
                        </div>

                        <div class="mt-3 p-3 p-lg-4 d-flex flex-wrap align-items-center justify-content-between gap-3" style="background: rgba(22,163,74,0.06); border-radius: 14px; border: 1px dashed rgba(22,163,74,0.4);">
                            <div>
                                <h6 class="fw-bold text-dark mb-1"><i class="fas fa-desktop me-2 text-success"></i><%= Common.getBahasaConfig("Ingin Mencoba Sebelum Memutuskan?") %></h6>
                                <p class="text-secondary mb-0" style="font-size: 0.95rem;">
                                    <%= Common.getBahasaConfig("Jelajahi langsung tampilan dan alur kerja eMedic melalui akun demo:") %>
                                    <%= Common.getBahasaConfig("Username") %>: <strong>demo</strong> &nbsp;|&nbsp; <%= Common.getBahasaConfig("Password") %>: <strong>demo123</strong>
                                </p>
                            </div>
                            <a href="https://apps.emedik.id" target="_blank" class="btn btn-success fw-bold px-4 py-2 shadow-sm flex-shrink-0">
                                <i class="fas fa-arrow-up-right-from-square me-2"></i><%= Common.getBahasaConfig("Buka Demo") %>
                            </a>
                        </div>
                    </div>

                    <!-- Panel Sistem Gudang Pusat, Outlet, POS, Ekspedisi, Investor & Akuntansi (terintegrasi atau berdiri sendiri) -->
                    <div class="p-4 p-lg-5 mt-4" style="background: linear-gradient(135deg, rgba(234,88,12,0.06), rgba(255,255,255,0.9)); border-radius: 16px; border: 1px solid rgba(234, 88, 12, 0.18);">
                        <div class="row align-items-center g-4">
                            <div class="col-lg-2 text-center">
                                <i class="fas fa-warehouse fa-4x" style="color: var(--primary-color);"></i>
                            </div>
                            <div class="col-lg-10">
                                <h5 class="fw-bold text-dark mb-3">
                                    <span class="badge bg-primary bg-opacity-10 text-primary border border-primary border-opacity-25 me-2" style="font-size: 0.7rem; letter-spacing: 0.05em;">BARU</span>
                                    <%= Common.getBahasaConfig("Apa itu Sistem Gudang, Outlet, POS, Ekspedisi, Investor & Akuntansi?") %>
                                </h5>
                                <p class="text-secondary mb-2" style="line-height: 1.7; text-align: justify;">
                                    <%= Common.getBahasaConfig("Modul ini menyatukan pengelolaan Gudang Pusat dan Gudang Cabang, banyak Outlet/Toko, Kasir (POS) yang tetap bisa mencatat transaksi walau internet terputus, distribusi/Ekspedisi antar lokasi, skema bagi hasil Investor yang dapat dikonfigurasi, hingga Akuntansi dengan jurnal otomatis dan perhitungan HPP (harga pokok penjualan) yang dihitung langsung dari resep/bahan baku — bukan angka yang diisi manual.") %>
                                </p>
                                <p class="text-secondary mb-0" style="line-height: 1.7; text-align: justify;">
                                    <%= Common.getBahasaConfig("Jika Universitas (eCampus), Sekolah (eSchool), atau Pondok Pesantren (ePesantren) Anda memiliki koperasi, kantin, toko, gudang, unit usaha, atau bisnis yang didanai investor, modul ini dapat langsung terintegrasi tanpa perlu sistem terpisah. Modul ini juga dapat diimplementasikan secara berdiri sendiri (standalone) untuk usaha ritel, jaringan outlet, atau koperasi yang tidak bernaung di bawah institusi pendidikan.") %>
                                </p>
                            </div>
                        </div>

                        <div class="mt-4 p-4" style="background: #ffffff; border-radius: 14px; border: 1px dashed rgba(234,88,12,0.35);">
                            <h6 class="fw-bold text-dark mb-2"><i class="fas fa-circle-info me-2" style="color:#ea580c;"></i><%= Common.getBahasaConfig("Bagaimana Cara Menghitung Biaya Berlangganannya? (Penjelasan Sederhana)") %></h6>
                            <p class="text-secondary mb-2" style="line-height: 1.8; text-align: justify;">
                                <%= Common.getBahasaConfig("Secara sederhana, biaya berlangganan bulanan terdiri dari dua bagian yang dijumlahkan. Bagian pertama adalah biaya mesin Kasir (POS) di setiap Outlet/Toko, yaitu mulai dari Rp249.000 per bulan untuk satu mesin kasir pertama pada tiap Outlet. Apabila dalam satu Outlet terdapat lebih dari satu mesin kasir, setiap mesin kasir tambahan hanya dikenakan mulai Rp99.000 per bulan. Semakin banyak jumlah Outlet yang berlangganan, semakin murah pula harga per mesin kasirnya, karena tersedia potongan harga volume secara bertingkat.") %>
                            </p>
                            <p class="text-secondary mb-2" style="line-height: 1.8; text-align: justify;">
                                <%= Common.getBahasaConfig("Bagian kedua adalah biaya Paket Modul Pusat, yaitu biaya bulanan yang dikenakan satu kali untuk seluruh perusahaan/yayasan (bukan per Outlet), yang besarnya disesuaikan dengan kebutuhan: mulai dari paket dasar untuk pemantauan stok terpusat, paket menengah yang sudah mencakup Gudang Pusat dan Produksi, hingga paket lengkap yang sudah mencakup seluruh modul termasuk Akuntansi, Kepegawaian, dan Investor. Apabila unit usaha hanya membutuhkan Kasir (POS) tanpa modul pusat, biaya Paket Modul Pusat ini dapat sepenuhnya ditiadakan, sehingga unit usaha berskala kecil pun tetap dapat mulai menggunakan sistem ini dengan biaya yang sangat ringan.") %>
                            </p>
                            <p class="text-secondary mb-0" style="line-height: 1.8; text-align: justify;">
                                <%= Common.getBahasaConfig("Sebagai gambaran, sebuah Outlet kecil dengan satu mesin kasir cukup membayar mulai Rp249.000 per bulan tanpa perlu mengambil paket pusat apa pun. Rincian lengkap seluruh tingkatan harga, contoh simulasi perhitungan untuk berbagai jumlah Outlet, biaya pemasangan awal, serta pilihan harga tahunan yang lebih hemat, dapat dibaca secara lengkap dan mendetail pada dokumen Surat Penawaran dan Proposal yang tersedia di bawah.") %>
                            </p>
                        </div>
                    </div>

                    <div class="highlight-cta mt-4">
                        <h4><%= Common.getBahasaConfig("Jangan Tunda Lagi!") %></h4>
                        <p class="mb-0 fs-5 fw-medium">
                            <%= Common.getBahasaConfig("Transformasikan Institusi Pendidikan Anda menuju masa depan keunggulan bersama Enterprise Education!") %>
                        </p>
                    </div>

                </div>
            </div>
        </div>

        <div id="section-payment" class="row justify-content-center mb-5">
            <div class="col-lg-12 col-xl-11">
                <div class="glass-intro p-4 p-lg-5 text-start" style="border-left: 6px solid var(--primary-color);">
                    <div class="row align-items-center g-5">
                        <div class="col-lg-7">
                            <h2 class="fw-bold mb-3" style="color: var(--primary-color);">
                                <i class="fas fa-wallet me-2"></i><%= Common.getBahasaConfig("Infrastruktur Pembayaran Digital Terintegrasi") %>
                            </h2>
                            <p class="text-secondary" style="line-height: 1.8; text-align: justify; font-size: 1.05rem;">
                                <%= Common.getBahasaConfig("Platform kami kini telah didukung oleh sistem Online Payment Gateway yang komprehensif, memungkinkan institusi pendidikan untuk menerima pembayaran kewajiban finansial secara seketika (real-time), aman, dan tervalidasi secara otomatis. Mahasiswa, peserta didik, maupun orang tua/wali dapat dengan mudah menyelesaikan transaksi administrasi kapan saja dan di mana saja melalui berbagai kanal pembayaran pilihan.") %>
                            </p>
                            
                            <!-- KARTU SYARAT TRANSAKSI -->
                            <div class="card border-0 shadow-sm rounded-4 mt-4" style="background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);">
                                <div class="card-body p-4">
                                    <h5 class="fw-bold text-primary mb-3"><i class="fas fa-hand-holding-usd me-2"></i>Skema Pembiayaan: Per Transaksi Pembayaran</h5>
                                    <div class="row g-3">
                                        <div class="col-md-12">
                                            <div class="bg-white p-3 rounded-3 shadow-sm border border-light">
                                                <h6 class="fw-bold text-dark"><i class="fas fa-clipboard-check text-success me-2"></i>Syarat & Ketentuan</h6>
                                                <p class="small text-muted mb-0">Minimal <strong>2.000 Mahasiswa/Siswa aktif</strong> dan <strong>wajib menggunakan pembayaran tiap bulan</strong>. Jika sistem pembayaran hanya 1x per semester (contoh: UKT murni), maka institusi <u>tidak memenuhi syarat</u> untuk skema biaya per transaksi ini.</p>
                                            </div>
                                        </div>
                                        <div class="col-md-6">
                                            <div class="bg-white p-3 rounded-3 shadow-sm border border-light h-100">
                                                <h6 class="fw-bold text-dark"><i class="fas fa-receipt text-danger me-2"></i>Biaya Admin Transaksi</h6>
                                                <p class="small text-muted mb-0"><strong>Rp 7.000 / transaksi</strong> (Untuk semua channel kecuali QRIS dikenakan 1%).</p>
                                            </div>
                                        </div>
                                        <div class="col-md-6">
                                            <div class="bg-white p-3 rounded-3 shadow-sm border border-light h-100">
                                                <h6 class="fw-bold text-dark"><i class="fas fa-users text-info me-2"></i>Beban Biaya (Zero Invest)</h6>
                                                <p class="small text-muted mb-0">Admin dibebankan kepada pembayar (Ortu/Mahasiswa). <strong>Institusi tidak keluar biaya lisensi/server bulanan sama sekali.</strong></p>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            
                            <h5 class="fw-bold text-dark mt-4 mb-3"><%= Common.getBahasaConfig("Kanal Pembayaran yang Didukung:") %></h5>
                            <ul class="list-unstyled text-secondary" style="line-height: 1.8; font-size: 1.05rem;">
                                <li class="mb-2">
                                    <i class="fas fa-check-circle text-success me-2"></i>
                                    <strong><%= Common.getBahasaConfig("Transfer Virtual Account:") %></strong> <%= Common.getBahasaConfig("BCA, Mandiri, BNI, BRI, BSI, Permata, Danamon, dan CIMB Niaga.") %>
                                </li>
                                <li class="mb-2">
                                    <i class="fas fa-check-circle text-success me-2"></i>
                                    <strong><%= Common.getBahasaConfig("QRIS & Dompet Digital (E-Wallet):") %></strong> <%= Common.getBahasaConfig("Mendukung pemindaian QRIS terpusat untuk aplikasi GoPay, OVO, DANA, ShopeePay, LinkAja, dan lainnya.") %>
                                </li>
                                <li class="mb-2">
                                    <i class="fas fa-check-circle text-success me-2"></i>
                                    <strong><%= Common.getBahasaConfig("Gerai Ritel Modern:") %></strong> <%= Common.getBahasaConfig("Pembayaran tunai melalui jaringan Alfamart dan Indomaret.") %>
                                </li>
                                <li class="mb-2">
                                    <i class="fas fa-check-circle text-success me-2"></i>
                                    <strong><%= Common.getBahasaConfig("Kartu Kredit & Debit:") %></strong> <%= Common.getBahasaConfig("Mendukung transaksi aman melalui jaringan VISA, Mastercard, dan JCB.") %>
                                </li>
                                <li class="mb-2">
                                    <i class="fas fa-check-circle text-success me-2"></i>
                                    <strong><%= Common.getBahasaConfig("Transfer Bank Manual:") %></strong> <%= Common.getBahasaConfig("Tersedia sebagai opsi fleksibilitas pelunasan konvensional.") %>
                                </li>
                            </ul>
                        </div>
                        
                        <div class="col-lg-5 text-center">
                            <div class="payment-gateway-wrapper p-4" style="background: rgba(255,255,255,0.7); border-radius: 16px; border: 1px dashed rgba(13, 110, 253, 0.4);">
                                <i class="fas fa-shield-alt fa-3x text-success mb-3"></i>
                                <h4 class="fw-bold text-dark mb-4"><%= Common.getBahasaConfig("Transaksi Aman & Otomatis") %></h4>
                                
                                <div class="d-flex flex-wrap justify-content-center gap-2">
                                    <span class="badge bg-primary fs-6 py-2 px-3 shadow-sm">
                                        <i class="fas fa-university me-1"></i> Virtual Account
                                    </span>
                                    <span class="badge bg-info text-dark fs-6 py-2 px-3 shadow-sm">
                                        <i class="fas fa-qrcode me-1"></i> QRIS
                                    </span>
                                    <span class="badge bg-warning text-dark fs-6 py-2 px-3 shadow-sm">
                                        <i class="fas fa-wallet me-1"></i> E-Wallet
                                    </span>
                                    <span class="badge bg-danger fs-6 py-2 px-3 shadow-sm">
                                        <i class="fas fa-store me-1"></i> Gerai Ritel
                                    </span>
                                    <span class="badge bg-secondary fs-6 py-2 px-3 shadow-sm">
                                        <i class="fas fa-credit-card me-1"></i> Kartu Kredit/Debit
                                    </span>
                                </div>
                                
                                <p class="text-muted mt-4 mb-0 text-sm" style="line-height: 1.6;">
                                    <%= Common.getBahasaConfig("Seluruh riwayat transaksi langsung tersinkronisasi dengan modul Finance dan Akuntansi institusi tanpa perlu verifikasi manual.") %>
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        
        <div id="section-kiosk" class="row justify-content-center mb-5">
            <div class="col-lg-12 col-xl-11">
                <div class="glass-intro p-4 p-lg-5 text-start" style="border-left: 6px solid #f59e0b;">
                    <div class="row align-items-center g-5">
                        <div class="col-lg-7 order-2 order-lg-1">
                            <h2 class="fw-bold mb-3" style="color: #d97706;">
                                <i class="fas fa-desktop me-2"></i><%= Common.getBahasaConfig("Anjungan Informasi Mandiri (Self-Service Kiosk)") %>
                            </h2>
                            <p class="text-secondary" style="line-height: 1.8; text-align: justify; font-size: 1.05rem;">
                                <%= Common.getBahasaConfig("Guna memaksimalkan efisiensi birokrasi, sistem Enterprise Education dilengkapi dengan fasilitas Anjungan Informasi Mandiri. Infrastruktur ini dirancang untuk memberikan layanan administrasi yang cepat, tanggap, dan terotomatisasi bagi seluruh peserta didik, baik mahasiswa, siswa, maupun santri.") %>
                            </p>
                            <p class="text-secondary mb-0" style="line-height: 1.8; text-align: justify; font-size: 1.05rem;">
                                <%= Common.getBahasaConfig("Melalui terminal layar sentuh ini, peserta didik dapat melakukan proses layanan mandiri (self-service). Sebagai contoh, pengajuan pencetakan surat keterangan aktif atau transkrip nilai kini dapat dilakukan dan dicetak langsung di anjungan tanpa perlu mengantre di loket Tata Usaha (TU).") %>
                            </p>
                            
                            <h5 class="fw-bold text-dark mt-4 mb-3"><%= Common.getBahasaConfig("Layanan Utama pada Anjungan:") %></h5>
                            <ul class="list-unstyled text-secondary" style="line-height: 1.8; font-size: 1.05rem;">
                                <li class="mb-2">
                                    <i class="fas fa-print text-warning me-2"></i>
                                    <strong><%= Common.getBahasaConfig("Pencetakan Dokumen Otomatis:") %></strong> <%= Common.getBahasaConfig("Cetak mandiri KRS, KHS, surat izin, dan dokumen akademik lainnya.") %>
                                </li>
                                <li class="mb-2">
                                    <i class="fas fa-calendar-alt text-warning me-2"></i>
                                    <strong><%= Common.getBahasaConfig("Akses Informasi Real-time:") %></strong> <%= Common.getBahasaConfig("Pengecekan jadwal perkuliahan/pelajaran, kalender akademik, dan pengumuman institusi.") %>
                                </li>
                                <li class="mb-2">
                                    <i class="fas fa-clipboard-list text-warning me-2"></i>
                                    <strong><%= Common.getBahasaConfig("Pendaftaran & Pengajuan:") %></strong> <%= Common.getBahasaConfig("Akses cepat untuk pendaftaran program khusus seperti PKL, KKN, atau ujian komprehensif.") %>
                                </li>
                            </ul>

                            <div class="alert alert-warning py-3 px-4 shadow-sm border-0 mt-4" style="background-color: #fffbeb; border-left: 5px solid #d97706 !important;">
                                <h6 class="fw-bold text-dark mb-2"><i class="fas fa-info-circle text-warning me-2"></i>Ketentuan Pengadaan Hardware ATM</h6>
                                <p class="mb-0 small text-muted text-justify">Pengadaan Hardware ini bersifat <strong>opsional</strong> dan di luar skema sewa Software. Belum termasuk Pajak & Ongkir luar Jabodetabek. <strong>Syarat: DP Rp 30 Juta di awal, proses manufaktur kurang lebih 1 bulan.</strong></p>
                            </div>
                        </div>
                        
                        <div class="col-lg-5 order-1 order-lg-2 text-center">
                            <div class="row g-3 mb-3">
                                <div class="col-12">
                                    <img 
                                        src="<%=Common.ROOT%>/img/anjungan1.jpg" 
                                        alt="ATM Model 1" 
                                        class="img-fluid rounded-4 shadow border border-white border-3 hover-zoom" 
                                        style="height: 220px; width: 100%; object-fit: cover;" 
                                        onclick="showImageModal(this.src)" 
                                        onerror="this.style.display='none'"
                                    >
                                </div>
                                <div class="col-6">
                                    <img 
                                        src="<%=Common.ROOT%>/img/anjungan2.jpg" 
                                        alt="ATM Model 2" 
                                        class="img-fluid rounded-4 shadow border border-white border-3 hover-zoom" 
                                        style="height: 130px; width: 100%; object-fit: cover;" 
                                        onclick="showImageModal(this.src)" 
                                        onerror="this.style.display='none'"
                                    >
                                </div>
                                <div class="col-6">
                                    <img 
                                        src="<%=Common.ROOT%>/img/anjungan3.jpg" 
                                        alt="ATM Model 3" 
                                        class="img-fluid rounded-4 shadow border border-white border-3 hover-zoom" 
                                        style="height: 130px; width: 100%; object-fit: cover;" 
                                        onclick="showImageModal(this.src)" 
                                        onerror="this.style.display='none'"
                                    >
                                </div>
                            </div>
                            <div class="text-center small text-muted fst-italic mb-3">
                                <i class="fas fa-search-plus me-1"></i> Klik gambar untuk memperbesar
                            </div>
                            
                            <div class="payment-gateway-wrapper p-3" style="background: rgba(255,255,255,0.7); border-radius: 16px; border: 1px dashed rgba(217, 119, 6, 0.4);">
                                <h5 class="fw-bold text-dark mb-2"><%= Common.getBahasaConfig("Modernisasi Layanan Tata Usaha") %></h5>
                                <p class="text-muted mb-0 text-sm" style="line-height: 1.5;">
                                    <%= Common.getBahasaConfig("Mengurangi beban kerja administratif staf hingga 70% dengan mengalihkan proses birokrasi rutin ke mesin anjungan yang beroperasi penuh.") %>
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div id="section-hardware" class="row justify-content-center mb-5">
            <div class="col-lg-12 col-xl-11">
                <div class="glass-intro p-4 p-lg-5 text-start" style="border-left: 6px solid #10b981;">
                    <div class="row align-items-center g-5">
                        <div class="col-lg-7">
                            <div class="d-flex align-items-center mb-3">
                                <span class="badge bg-danger rounded-pill px-3 py-2 me-3 fs-6 shadow-sm">
                                    <i class="fas fa-fire me-1"></i> Penawaran Khusus
                                </span>
                                <h2 class="fw-bold mb-0" style="color: #059669;">
                                    <i class="fas fa-cash-register me-2"></i><%= Common.getBahasaConfig("Hardware POS DUAL") %>
                                </h2>
                            </div>
                            
                            <p class="text-secondary" style="line-height: 1.8; text-align: justify; font-size: 1.05rem;">
                                <%= Common.getBahasaConfig("Kami menyediakan solusi perangkat keras (hardware) unggulan untuk mendukung transaksi Point of Sale (POS) di kantin, koperasi, atau unit usaha institusi Anda. Spesifikasi ANFO POS DUAL ini dirancang dengan standar industri yang menjamin keandalan penggunaan jangka panjang.") %>
                            </p>

                            <div class="row mt-4 text-secondary" style="font-size: 0.95rem;">
                                <div class="col-md-6 mb-3">
                                    <h6 class="fw-bold text-dark"><i class="fas fa-microchip text-success me-2"></i>Spesifikasi Utama (CPU)</h6>
                                    <ul class="list-unstyled mt-2" style="line-height: 1.7;">
                                        <li><i class="fas fa-angle-right text-muted me-2"></i><strong>Layar Kasir:</strong> 15.6 Inch Touchscreen</li>
                                        <li><i class="fas fa-angle-right text-muted me-2"></i><strong>Layar Pelanggan:</strong> 10 Inch Non Touchscreen</li>
                                        <li><i class="fas fa-angle-right text-muted me-2"></i><strong>Processor:</strong> Intel Core i3-10110U (2C/4T, 4M Cache, 2.1GHz - 4.1GHz)</li>
                                        <li><i class="fas fa-angle-right text-muted me-2"></i><strong>Graphics:</strong> Intel UHD Graphics (Base 300MHz, Max 1000MHz)</li>
                                        <li><i class="fas fa-angle-right text-muted me-2"></i><strong>TDP:</strong> 15 W</li>
                                        <li><i class="fas fa-angle-right text-muted me-2"></i><strong>Memory:</strong> 16GB DDR4 Dual Channel (2x 8GB) Max 64GB</li>
                                        <li><i class="fas fa-angle-right text-muted me-2"></i><strong>Audio:</strong> HDA CODEC</li>
                                        <li><i class="fas fa-angle-right text-muted me-2"></i><strong>Storage:</strong> 512GB SSD</li>
                                    </ul>
                                </div>
                                <div class="col-md-6 mb-3">
                                    <h6 class="fw-bold text-dark"><i class="fas fa-print text-success me-2"></i>Printer & Cash Drawer</h6>
                                    <ul class="list-unstyled mt-2" style="line-height: 1.7;">
                                        <li><i class="fas fa-angle-right text-muted me-2"></i><strong>Tipe Printer:</strong> Thermal Line Printing</li>
                                        <li><i class="fas fa-angle-right text-muted me-2"></i><strong>Print Speed:</strong> 90mm/s</li>
                                        <li><i class="fas fa-angle-right text-muted me-2"></i><strong>Lebar Cetakan:</strong> Max 80 mm (376 dot)</li>
                                        <li><i class="fas fa-angle-right text-muted me-2"></i><strong>Lebar Kertas:</strong> 79,5 +/- 0,5 mm</li>
                                        <li><i class="fas fa-angle-right text-muted me-2"></i><strong>Emulation:</strong> ESC/POS Command</li>
                                        <li><i class="fas fa-angle-right text-muted me-2"></i><strong>Interface:</strong> USB + Bluetooth 3.0, 4.0, 4.1, 4.2</li>
                                        <li><i class="fas fa-angle-right text-muted me-2"></i><strong>Laci Kasir (Drawer):</strong> RJ-11 MERK VSC</li>
                                        <li><i class="fas fa-angle-right text-muted me-2"></i><strong>Dimensi Laci:</strong> 49 x 48 x 16 cm (3 posisi anak kunci)</li>
                                        <li><i class="fas fa-angle-right text-muted me-2"></i><strong>Slot:</strong> 6 Slot Uang Kertas & 4 Slot Uang Logam</li>
                                    </ul>
                                </div>
                            </div>
                            
                            <div class="alert alert-success py-3 px-4 shadow-sm border-0 mt-2" style="background-color: #ecfdf5; border-left: 5px solid #10b981 !important;">
                                <h6 class="fw-bold text-dark mb-2"><i class="fas fa-info-circle text-success me-2"></i>Ketentuan Pengadaan Hardware POS</h6>
                                <p class="mb-0 small text-muted text-justify">Pengadaan Hardware ini bersifat <strong>opsional</strong> dan di luar skema sewa Software. Belum termasuk Pajak & Ongkir luar Jabodetabek. <strong>Syarat: DP Rp 30 Juta di awal, proses manufaktur kurang lebih 1 bulan.</strong></p>
                            </div>
                        </div>
                        
                        <div class="col-lg-5 text-center">
                            <div class="row g-3 mb-3">
                                <div class="col-12">
                                    <img 
                                        src="<%=Common.ROOT%>/img/pos1.jpg" 
                                        alt="Mesin POS Gambar 1" 
                                        class="img-fluid rounded-4 shadow border border-white border-3 hover-zoom" 
                                        style="height: 250px; width: 100%; object-fit: cover;" 
                                        onclick="showImageModal(this.src)" 
                                        onerror="this.style.display='none'"
                                    >
                                </div>
                                <div class="col-12">
                                    <img 
                                        src="<%=Common.ROOT%>/img/pos2.jpg" 
                                        alt="Mesin POS Gambar 2" 
                                        class="img-fluid rounded-4 shadow border border-white border-3 hover-zoom" 
                                        style="height: 250px; width: 100%; object-fit: cover;" 
                                        onclick="showImageModal(this.src)" 
                                        onerror="this.style.display='none'"
                                    >
                                </div>
                            </div>
                            <div class="text-center small text-muted fst-italic">
                                <i class="fas fa-search-plus me-1"></i> Klik gambar untuk memperbesar
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div id="section-investment" class="row justify-content-center mb-5">
            <div class="col-lg-12 col-xl-11">
                <div class="premium-card p-4 p-lg-5">
                    <div class="text-center position-relative mb-4">
                        <div class="enterprise-ribbon-v2">
                            <i class="fas fa-file-invoice-dollar"></i><%= Common.getBahasaConfig("Skema Investasi & Kerja Sama") %>
                        </div>
                        <h2 class="section-heading-premium">
                            <%= Common.getBahasaConfig("Fleksibel untuk Berbagai Kebutuhan Institusi, Tetap Transparan untuk Proses Pengambilan Keputusan") %>
                        </h2>
                        <p class="section-lead-premium mb-0">
                            <%= Common.getBahasaConfig("Materi proposal dan surat penawaran menekankan bahwa nilai investasi dapat disusun secara bertahap sesuai prioritas modul, kesiapan organisasi, model layanan, kebutuhan hardware, dan ruang lingkup pendampingan yang disepakati bersama.") %>
                        </p>
                    </div>

                    <div class="row g-4">
                        <div class="col-md-6 col-xl-3">
                            <div class="investment-card">
                                <div class="investment-tag"><i class="fas fa-money-bill-transfer"></i><%= Common.getBahasaConfig("Software") %></div>
                                <h4><%= Common.getBahasaConfig("Berbasis Transaksi") %></h4>
                                <p><%= Common.getBahasaConfig("Cocok untuk institusi yang ingin menyesuaikan biaya sistem dengan aktivitas pembayaran atau transaksi layanan digital yang berjalan.") %></p>
                                <ul class="investment-list">
                                    <li><i class="fas fa-check-circle"></i><span><%= Common.getBahasaConfig("Memudahkan model zero investment pada tahap awal.") %></span></li>
                                    <li><i class="fas fa-check-circle"></i><span><%= Common.getBahasaConfig("Nilai mengikuti volume pemanfaatan layanan.") %></span></li>
                                </ul>
                            </div>
                        </div>
                        <div class="col-md-6 col-xl-3">
                            <div class="investment-card">
                                <div class="investment-tag"><i class="fas fa-users"></i><%= Common.getBahasaConfig("Lisensi") %></div>
                                <h4><%= Common.getBahasaConfig("Berbasis Pengguna Aktif") %></h4>
                                <p><%= Common.getBahasaConfig("Dapat menjadi pilihan bagi lembaga yang membutuhkan perencanaan biaya lebih stabil berdasarkan jumlah pengguna aktif atau unit layanan yang digunakan.") %></p>
                                <ul class="investment-list">
                                    <li><i class="fas fa-check-circle"></i><span><%= Common.getBahasaConfig("Lebih mudah untuk perencanaan anggaran tahunan.") %></span></li>
                                    <li><i class="fas fa-check-circle"></i><span><%= Common.getBahasaConfig("Dapat disesuaikan dengan paket modul.") %></span></li>
                                </ul>
                            </div>
                        </div>
                        <div class="col-md-6 col-xl-3">
                            <div class="investment-card">
                                <div class="investment-tag"><i class="fas fa-desktop"></i><%= Common.getBahasaConfig("Hardware") %></div>
                                <h4><%= Common.getBahasaConfig("Anjungan & POS Opsional") %></h4>
                                <p><%= Common.getBahasaConfig("Perangkat fisik seperti Anjungan Transaksi Mandiri dan ANFO POS DUAL dapat ditawarkan terpisah sesuai kebutuhan operasional kampus, sekolah, pesantren, kantin, koperasi, atau unit usaha.") %></p>
                                <ul class="investment-list">
                                    <li><i class="fas fa-check-circle"></i><span><%= Common.getBahasaConfig("Pengadaan dapat dilakukan bertahap sesuai prioritas lokasi.") %></span></li>
                                    <li><i class="fas fa-check-circle"></i><span><%= Common.getBahasaConfig("Terintegrasi dengan pembayaran dan kartu identitas digital.") %></span></li>
                                </ul>
                            </div>
                        </div>
                        <div class="col-md-6 col-xl-3">
                            <div class="investment-card">
                                <div class="investment-tag"><i class="fas fa-scale-balanced"></i><%= Common.getBahasaConfig("Negosiasi") %></div>
                                <h4><%= Common.getBahasaConfig("Terbuka untuk Penyesuaian") %></h4>
                                <p><%= Common.getBahasaConfig("Nilai akhir, tahapan implementasi, masa kontrak, dukungan teknis, dan prioritas modul dapat dibahas secara profesional agar implementasi memberi manfaat nyata secepat mungkin.") %></p>
                                <ul class="investment-list">
                                    <li><i class="fas fa-check-circle"></i><span><%= Common.getBahasaConfig("Mendukung pembahasan dengan pimpinan, keuangan, pengadaan, dan legal.") %></span></li>
                                    <li><i class="fas fa-check-circle"></i><span><%= Common.getBahasaConfig("Dapat dituangkan dalam surat penawaran dan PKS.") %></span></li>
                                </ul>
                            </div>
                        </div>
                    </div>

                    <div class="legal-footnote mt-4">
                        <i class="fas fa-circle-info me-2"></i>
                        <strong><%= Common.getBahasaConfig("Catatan formal:") %></strong>
                        <%= Common.getBahasaConfig("Rincian biaya, paket modul, pengadaan hardware, jangka waktu layanan, serta ketentuan SLA bersifat mengikuti dokumen penawaran dan kesepakatan final para pihak. Seluruh informasi pada halaman ini berfungsi sebagai pengantar komersial dan teknis sebelum pembahasan resmi.") %>
                    </div>
                </div>
            </div>
        </div>


        <div id="section-modules" class="mb-5 pb-4 mt-2">
            <div class="text-center mb-5">
                <div class="section-eyebrow">
                    <i class="fas fa-boxes"></i><%= Common.getBahasaConfig("Modul Lengkap") %>
                </div>
                <h2 class="section-heading-premium"><%= Common.getBahasaConfig("Eksplorasi Modul Utama") %></h2>
                <p class="section-lead-premium mb-0">
                    <%= Common.getBahasaConfig("Rangkaian modul berikut dirancang untuk membangun alur kerja digital dari hulu ke hilir: mulai dari penerimaan peserta didik, akademik, keuangan, SDM, aset, layanan pengguna, dashboard, sampai integrasi pelaporan strategis.") %>
                </p>
            </div>
            
            <div class="row row-cols-1 row-cols-md-2 row-cols-xl-3 g-4 justify-content-center">
                
                <div class="col">
                    <div class="feature-card" style="border-top: 4px solid var(--primary-color);">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/OSPusoTw5f0" 
                                title="Transformasi Pendidikan eCampus" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-lightbulb text-primary"></i><%= Common.getBahasaConfig("Transformasi Pendidikan eCampus") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Langkah inovatif dalam mentransformasi ekosistem pendidikan tinggi menuju digitalisasi penuh. Mengintegrasikan teknologi mutakhir untuk mempercepat pelayanan, meningkatkan kualitas pembelajaran, dan menciptakan lingkungan yang adaptif.") %>
                        </p>
                    </div>
                </div>

                <div class="col">
                    <div class="feature-card">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/xVytRItXc0Y" 
                                title="Modul PMB" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-user-graduate"></i><%= Common.getBahasaConfig("Modul Penerimaan (PMB/PPDB)") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Sistem automasi penerimaan pendaftar baru secara terpadu. Memfasilitasi pendaftaran daring, pengumpulan berkas, seleksi ujian berbasis komputer (CBT), wawancara, hingga pengumuman hasil dan registrasi.") %>
                        </p>
                    </div>
                </div>

                <div class="col">
                    <div class="feature-card">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/9hrD3SysFug" 
                                title="Modul Akademik" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-university"></i><%= Common.getBahasaConfig("Modul Akademik") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Pusat operasional data akademik. Mengelola basis data master, penyusunan jadwal perkuliahan/pelajaran dengan sistem deteksi cerdas anti-bentrok (clash-free), pengisian rencana studi daring, pemantauan hasil studi, dan penerbitan transkrip nilai resmi.") %>
                        </p>
                    </div>
                </div>

                <div class="col">
                    <div class="feature-card">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/6sjD-v-2dt4" 
                                title="Modul LMS" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-laptop-code"></i><%= Common.getBahasaConfig("E-Learning & LMS") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Platform pembelajaran jarak jauh interaktif. Mendukung distribusi materi, forum kolaborasi daring, pelaksanaan Ujian Online (Computer Based Test/CBT) dengan fitur pengawasan anti-curang, dan automasi penilaian yang disinkronisasi dengan metode presensi modern.") %>
                        </p>
                    </div>
                </div>

                <div class="col">
                    <div class="feature-card" style="border-top: 4px solid var(--primary-color);">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/9hrD3SysFug" 
                                title="Integrasi Pelaporan Nasional" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-sync-alt text-primary"></i><%= Common.getBahasaConfig("Integrasi Neo Feeder & EMIS") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Fitur sinkronisasi data satu pintu (1-click sync) secara terotomatisasi dengan sistem PDDIKTI Neo Feeder (Kemdikbudristek) dan EMIS (Kemenag) tanpa perlu repot melakukan entri data secara ganda (double-entry).") %>
                        </p>
                    </div>
                </div>

                <div class="col">
                    <div class="feature-card" style="border-top: 4px solid var(--primary-color);">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/pgGbWFeukq8" 
                                title="Multi-Portal Pengguna" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-users-cog text-primary"></i><%= Common.getBahasaConfig("Multi-Portal Pengguna") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Aksesibilitas terpersonalisasi dengan antarmuka yang didesain khusus secara terpisah untuk Dosen/Guru, Mahasiswa/Siswa, dan Orang Tua/Wali guna memantau perkembangan akademik dan finansial secara real-time.") %>
                        </p>
                    </div>
                </div>
                
                <div class="col">
                    <div class="feature-card" style="border-top: 4px solid var(--primary-color);">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/9hrD3SysFug" 
                                title="Presensi Cerdas" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-fingerprint text-primary"></i><%= Common.getBahasaConfig("Presensi Multi-Metode") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Instrumen pencatatan kehadiran tingkat lanjut yang mengintegrasikan pemindaian wajah (Face Recognition), Kartu RFID, dan absensi mobile berbasis titik lokasi radius GPS (Geotagging).") %>
                        </p>
                    </div>
                </div>
                
                <div class="col">
                    <div class="feature-card" style="border-top: 4px solid var(--primary-color);">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/RE6EHGXKc4M" 
                                title="Koperasi & Unit Usaha" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-hand-holding-usd text-primary"></i><%= Common.getBahasaConfig("Koperasi & Unit Usaha") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Memfasilitasi tata kelola operasional koperasi institusi yang mencakup manajemen keanggotaan, pencatatan transaksi simpan pinjam, hingga kalkulasi pembagian Sisa Hasil Usaha (SHU) secara transparan.") %>
                        </p>
                    </div>
                </div>
                
                <div class="col">
                    <div class="feature-card" style="border-top: 4px solid var(--primary-color);">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/9hrD3SysFug" 
                                title="Notifikasi WhatsApp" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fab fa-whatsapp text-primary"></i><%= Common.getBahasaConfig("WhatsApp Gateway") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Sistem pemberitahuan proaktif yang mengirimkan notifikasi penagihan pembayaran (blast reminder) dan konfirmasi kehadiran secara seketika langsung ke perangkat seluler orang tua atau wali.") %>
                        </p>
                    </div>
                </div>

                <div class="col">
                    <div class="feature-card" style="border-top: 4px solid var(--primary-color);">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/OSPusoTw5f0" 
                                title="Kurikulum Ganda" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-book-medical text-primary"></i><%= Common.getBahasaConfig("Kurikulum Ganda") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Infrastruktur akademik yang sangat adaptif dalam mengelola sistem kurikulum pendidikan nasional secara berdampingan dengan kurikulum keagamaan khusus (seperti Kitab Kuning) di lingkungan pesantren.") %>
                        </p>
                    </div>
                </div>
                
                <div class="col">
                    <div class="feature-card" style="border-top: 4px solid var(--primary-color);">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/nBD9gnW_hOs" 
                                title="Beban Kinerja Dosen" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-chalkboard-teacher text-primary"></i><%= Common.getBahasaConfig("Beban Kinerja Dosen (BKD)") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Fasilitas khusus bagi perguruan tinggi untuk mengawasi, mengevaluasi, dan menyusun laporan realisasi pemenuhan kewajiban Tridharma Perguruan Tinggi oleh seluruh staf pengajar akademik.") %>
                        </p>
                    </div>
                </div>

                <div class="col">
                    <div class="feature-card">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/eIlaHmXSanw" 
                                title="Modul Prestasi" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-trophy"></i><%= Common.getBahasaConfig("Modul Prestasi") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Katalog pencatatan terstruktur untuk raihan prestasi akademik maupun non-akademik. Dirancang untuk mendokumentasikan pencapaian sivitas akademika sebagai rujukan evaluasi dan penghargaan institusional.") %>
                        </p>
                    </div>
                </div>

                <div class="col">
                    <div class="feature-card">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/3ueps_MkjPk" 
                                title="Modul Pengajuan" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-file-signature"></i><%= Common.getBahasaConfig("Sistem Pengajuan") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Mendigitalisasi birokrasi kemahasiswaan/kesiswaan. Menangani pengajuan judul tugas akhir, penjadwalan sidang, cuti akademik, hingga pengajuan beasiswa melalui alur persetujuan yang transparan dan dapat dikonfigurasi.") %>
                        </p>
                    </div>
                </div>

                <div class="col">
                    <div class="feature-card">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/bl8CCf2mhP8" 
                                title="Modul PPM" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-flask"></i><%= Common.getBahasaConfig("Penelitian & Pengabdian") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Fasilitas khusus bagi unit penelitian guna mengawasi siklus pengabdian institusi. Mencakup pengajuan proposal pendanaan, monitoring pelaksanaan, hingga pelaporan dokumen hasil penelitian secara terintegrasi.") %>
                        </p>
                    </div>
                </div>

                <div class="col">
                    <div class="feature-card">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/_byJDCDZ5LA" 
                                title="Manajemen Karya Ilmiah" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-book-open"></i><%= Common.getBahasaConfig("Manajemen Karya Ilmiah") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Sistem repositori berbasis DSpace untuk mengarsipkan dan mempublikasikan literatur jurnal, artikel, tesis, serta disertasi. Memperluas jangkauan aksesibilitas hasil riset institusi kepada publik.") %>
                        </p>
                    </div>
                </div>

                <div class="col">
                    <div class="feature-card">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/8YjkbwhIuCM" 
                                title="Modul SPMI" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-check-double"></i><%= Common.getBahasaConfig("Modul SPMI") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Instrumen utama pengawalan Penjaminan Mutu Internal. Memandu proses penetapan standar, audit mutu, evaluasi kinerja departemen, hingga perumusan strategi peningkatan mutu berkelanjutan.") %>
                        </p>
                    </div>
                </div>

                <div class="col">
                    <div class="feature-card">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/ZlMdJIm14IA" 
                                title="Kurikulum OBE" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-bullseye"></i><%= Common.getBahasaConfig("Kurikulum OBE") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Mengakomodasi implementasi penuh Outcome Based Education (OBE) — dari perumusan Profil Lulusan (PL) dan Capaian Pembelajaran Lulusan (CPL) berdasarkan 4 kategori SN-Dikti (Sikap, Pengetahuan, Keterampilan Umum & Khusus), hingga CPMK per RPS beserta Sub-CPMK sebagai indikator penilaian terkecil. Didukung 3 siklus monitoring berkelanjutan: Loop 1 — ketercapaian CPMK per semester; Loop 2 — ketercapaian CPL per tahun akademik; Loop 3 — evaluasi Profil Lulusan setiap 3–5 tahun via Tracer Study. Dasbor OBE menampilkan skor kesiapan OBE 0–100 (tier Bronze/Silver/Gold/Platinum) beserta rekomendasi Continuous Quality Improvement (CQI) otomatis guna mendorong peningkatan kurikulum yang terukur dan berkelanjutan.") %>
                        </p>
                    </div>
                </div>

                <div class="col">
                    <div class="feature-card">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/ZP0V2Zoh0CU" 
                                title="Seleksi Wisuda" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-graduation-cap"></i><%= Common.getBahasaConfig("Seleksi Kelulusan") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Meniadakan antrean administratif kelulusan. Calon lulusan dapat mendaftar, mengunggah persyaratan kelulusan, dan memantau status validasi yudisium langsung dari perangkat mereka.") %>
                        </p>
                    </div>
                </div>

                <div class="col">
                    <div class="feature-card">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/cQguPV7Dyk8" 
                                title="Manajemen PKL & KKN" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-users-cog"></i><%= Common.getBahasaConfig("Manajemen Magang (PKL)") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Sentralisasi administrasi program magang dan pengabdian lapangan. Mempermudah proses alokasi kelompok, bimbingan daring secara berkala, hingga evaluasi laporan akhir kegiatan peserta.") %>
                        </p>
                    </div>
                </div>

                <div class="col">
                    <div class="feature-card">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/P6ej2Fv4S08" 
                                title="Sistem Persuratan" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-envelope-open-text"></i><%= Common.getBahasaConfig("Sistem Persuratan") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Digitalisasi tata kelola persuratan resmi institusi. Menyediakan fitur rekam jejak surat masuk/keluar, sistem disposisi struktural, pembuatan templat otomatis, serta pengarsipan dokumen dengan keamanan tinggi.") %>
                        </p>
                    </div>
                </div>
                
                <div class="col">
                    <div class="feature-card">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/0Vy8zKRp33Q" 
                                title="Perpustakaan eLibrary" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-book-reader"></i><%= Common.getBahasaConfig("Perpustakaan (eLibrary)") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Manajemen modern operasional perpustakaan. Mencakup sistem pengadaan buku, katalogisasi aset digital, otomasi sirkulasi peminjaman, serta penyediaan akses katalog koleksi secara daring.") %>
                        </p>
                    </div>
                </div>

                <div class="col">
                    <div class="feature-card">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/UMrME1jYroM" 
                                title="Modul Finance" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-file-invoice-dollar"></i><%= Common.getBahasaConfig("Modul Finance Institusi") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Solusi penagihan kewajiban finansial peserta didik. Mendukung pembuatan tagihan massal, skema pembayaran cicilan, dan verifikasi mutasi pembayaran secara real-time bekerja sama dengan mitra perbankan.") %>
                        </p>
                    </div>
                </div>

                <div class="col">
                    <div class="feature-card">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/RE6EHGXKc4M" 
                                title="Akuntansi Institusi" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-calculator"></i><%= Common.getBahasaConfig("Akuntansi Institusi") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Platform pencatatan buku besar yang sistematis guna memelihara akurasi laporan keuangan. Mencakup fitur manajemen piutang, rekonsiliasi kas, dan generasi neraca laba rugi standar institusi pendidikan.") %>
                        </p>
                    </div>
                </div>

                <div class="col">
                    <div class="feature-card">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/kqeAI4i7A6w" 
                                title="Modul Anggaran" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-chart-pie"></i><%= Common.getBahasaConfig("Perencanaan Anggaran") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Modul penyusunan estimasi anggaran tahunan institusi. Memfasilitasi alokasi dana strategis berdasarkan analisis capaian kinerja (IKK), simulasi finansial, dan pemantauan serapan dana.") %>
                        </p>
                    </div>
                </div>
                
                <div class="col">
                    <div class="feature-card">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/nsyLOFKfcvw" 
                                title="Realisasi Anggaran" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-wallet"></i><%= Common.getBahasaConfig("Realisasi Anggaran") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Mengelola tertib administrasi pengeluaran kas institusi. Mendukung pencatatan bukti pertanggungjawaban uang muka, pengelolaan kas kecil/besar, dan otorisasi transfer dengan validasi bertingkat.") %>
                        </p>
                    </div>
                </div>

                <div class="col">
                    <div class="feature-card">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/NKyGRKjFIks" 
                                title="Aset & Pengadaan" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-boxes"></i><%= Common.getBahasaConfig("Aset & Pengadaan") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Pengendalian inventaris fisik dan logistik terpadu. Fitur meliputi pemindaian aset (Barcode/RFID), depresiasi nilai barang, alur persetujuan pengadaan (e-procurement), hingga pemeliharaan aset.") %>
                        </p>
                    </div>
                </div>

                <div class="col">
                    <div class="feature-card">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/6J3P2NgOtDs" 
                                title="Penggajian Payroll" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-money-check-alt"></i><%= Common.getBahasaConfig("Penggajian (Payroll)") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Automasi kalkulasi honorarium dan tunjangan staf. Terintegrasi secara langsung dengan data presensi guna memastikan distribusi gaji yang presisi, efisien, serta menyediakan slip gaji digital bagi pegawai.") %>
                        </p>
                    </div>
                </div>

                <div class="col">
                    <div class="feature-card">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/-BJmMiXUhmU" 
                                title="Sumber Daya Manusia" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-users"></i><%= Common.getBahasaConfig("Sumber Daya Manusia") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Mengelola rekam jejak kepegawaian tenaga pendidik dan staf. Meliputi pendataan profil demografis, riwayat jabatan, permohonan cuti, hingga administrasi mutasi dalam antarmuka yang intuitif.") %>
                        </p>
                    </div>
                </div>

                <div class="col">
                    <div class="feature-card">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/jzM6FGiFlro" 
                                title="Modul KPI" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-chart-line"></i><%= Common.getBahasaConfig("Indikator Kinerja (KPI)") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Memfasilitasi pemantauan obyektif atas ketercapaian Indikator Kinerja Utama (KPI) individu. Mengkalkulasi data target (SMART) secara otomatis serta memberikan transparansi evaluasi sebagai pertimbangan sistem insentif.") %>
                        </p>
                    </div>
                </div>
                
                <div class="col">
                    <div class="feature-card">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://www.youtube.com/embed/nBD9gnW_hOs" 
                                title="Kinerja Pegawai" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-user-check"></i><%= Common.getBahasaConfig("Kinerja Pegawai") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Melengkapi sistem KPI dengan formulir penilaian kualitatif, evaluasi umpan balik 360 derajat, buku harian aktivitas staf, serta pelaporan rekapitulasi produktivitas bulanan dan tahunan.") %>
                        </p>
                    </div>
                </div>

                <div class="col">
                    <div class="feature-card">
                        <div class="ratio ratio-16x9 video-wrapper mb-3">
                            <iframe 
                                src="https://drive.google.com/file/d/1gp8A5TAeqq7NQIpVgsaQkr6F86h_KGGa/preview" 
                                title="Kantin Digital eKantin" 
                                allowfullscreen 
                                loading="lazy">
                            </iframe>
                        </div>
                        <h4 class="feature-title">
                            <i class="fas fa-store text-primary me-2"></i><%= Common.getBahasaConfig("Kantin Digital (eKantin)") %>
                        </h4>
                        <p class="feature-text">
                            <%= Common.getBahasaConfig("Sistem manajemen kantin digital terpadu yang memfasilitasi transaksi tanpa tunai (cashless), pemesanan menu secara daring, dan pengelolaan inventaris pedagang. Solusi ini mewujudkan ekosistem kantin yang higienis, efisien, serta terintegrasi langsung dengan kartu identitas peserta didik dan sistem keuangan institusi.") %>
                        </p>
                    </div>
                </div>

            </div>
        </div>

        <div id="section-documents" class="row justify-content-center mb-5">
            <div class="col-lg-12 col-xl-11">
                <div class="document-center-panel">
                    <div class="text-center position-relative mb-4">
                        <div class="enterprise-ribbon-v2">
                            <i class="fas fa-folder-open"></i><%= Common.getBahasaConfig("Pusat Dokumen Penawaran") %>
                        </div>
                        <h2 class="section-heading-premium">
                            <%= Common.getBahasaConfig("Semua Materi Pendukung Disiapkan agar Proses Presentasi, Evaluasi, dan Persetujuan Lebih Mudah") %>
                        </h2>
                        <p class="section-lead-premium mb-0">
                            <%= Common.getBahasaConfig("Calon mitra dapat mempelajari solusi melalui format berbeda sesuai kebutuhan forum: presentasi untuk pemaparan, proposal untuk kajian detail, surat penawaran untuk pembahasan komersial, dan draf PKS untuk penyelarasan kerja sama formal.") %>
                        </p>
                    </div>

                    <div class="row g-4">
                        <div class="col-md-6 col-xl-3">
                            <div class="document-action-card">
                                <div class="doc-icon"><i class="fas fa-envelope-open-text"></i></div>
                                <h4><%= Common.getBahasaConfig("Surat Penawaran") %></h4>
                                <p><%= Common.getBahasaConfig("Ringkasan resmi mengenai penawaran solusi, manfaat strategis, ruang lingkup umum, skema investasi, hardware opsional, serta langkah tindak lanjut yang dapat dipakai dalam proses administrasi awal.") %></p>
                                <a href="<%=Common.ROOT%>/penawaran<%=appendParams%>" target="_blank" class="btn btn-danger shadow-sm"><i class="fas fa-file-pdf"></i><%= Common.getBahasaConfig("Buka Surat") %></a>
                            </div>
                        </div>
                        <div class="col-md-6 col-xl-3">
                            <div class="document-action-card">
                                <div class="doc-icon"><i class="fas fa-display"></i></div>
                                <h4><%= Common.getBahasaConfig("Presentasi") %></h4>
                                <p><%= Common.getBahasaConfig("Materi siap tayang bergaya slide untuk menjelaskan gambaran ekosistem, keunggulan, modul, hardware, dan manfaat implementasi kepada pimpinan maupun tim pengambil keputusan.") %></p>
                                <a href="<%=Common.ROOT%>/presentasi<%=appendParams%>" target="_blank" class="btn btn-danger shadow-sm"><i class="fas fa-display"></i><%= Common.getBahasaConfig("Buka Presentasi") %></a>
                            </div>
                        </div>
                        <div class="col-md-6 col-xl-3">
                            <div class="document-action-card">
                                <div class="doc-icon"><i class="fas fa-file-lines"></i></div>
                                <h4><%= Common.getBahasaConfig("Proposal") %></h4>
                                <p><%= Common.getBahasaConfig("Dokumen kajian yang lebih lengkap berisi latar belakang, tujuan, manfaat, rincian ekosistem, modul, hardware, metodologi implementasi, dan skema investasi untuk evaluasi internal.") %></p>
                                <a href="<%=Common.ROOT%>/proposal<%=appendParams%>" target="_blank" class="btn btn-danger shadow-sm"><i class="fas fa-file-alt"></i><%= Common.getBahasaConfig("Buka Proposal") %></a>
                            </div>
                        </div>
                        <div class="col-md-6 col-xl-3">
                            <div class="document-action-card">
                                <div class="doc-icon"><i class="fas fa-handshake"></i></div>
                                <h4><%= Common.getBahasaConfig("Draf PKS") %></h4>
                                <p><%= Common.getBahasaConfig("Rujukan awal untuk menyelaraskan ruang lingkup kerja sama, hak dan kewajiban, pembiayaan, implementasi, SLA, kerahasiaan data, masa berlaku, dan ketentuan formal para pihak.") %></p>
                                <a href="<%=Common.ROOT%>/pks<%=appendParams%>" target="_blank" class="btn btn-danger shadow-sm"><i class="fas fa-file-signature"></i><%= Common.getBahasaConfig("Buka PKS") %></a>
                            </div>
                        </div>
                    </div>

                    <div class="text-center position-relative mt-5 mb-4">
                        <div class="enterprise-ribbon-v2" style="background: rgba(2,132,199,0.1); color: var(--primary-color, #0284c7); border-color: rgba(2,132,199,0.25);">
                            <i class="fas fa-hospital"></i><%= Common.getBahasaConfig("Versi Implementasi: Klinik, Rumah Sakit & Layanan Medis (eMedic)") %>
                        </div>
                        <p class="section-lead-premium mb-0 mt-2">
                            <%= Common.getBahasaConfig("Dokumen yang sama, disusun khusus untuk kebutuhan modul eMedic: registrasi hingga farmasi, dashboard analitik, roadmap BPJS/SATUSEHAT, dan skema investasi Beli Putus / Langganan Bulanan / Bagi Hasil per Transaksi.") %>
                        </p>
                    </div>

                    <div class="row g-4">
                        <div class="col-md-6 col-xl-3">
                            <div class="document-action-card">
                                <div class="doc-icon"><i class="fas fa-envelope-open-text"></i></div>
                                <h4><%= Common.getBahasaConfig("Surat Penawaran") %></h4>
                                <p><%= Common.getBahasaConfig("Ringkasan resmi penawaran modul eMedic: manfaat strategis, ruang lingkup registrasi hingga farmasi, tiga skema investasi, serta langkah tindak lanjut untuk proses administrasi awal.") %></p>
                                <a href="<%=Common.ROOT%>/penawaran?modul=kesehatan<%=(appendParams.isEmpty() ? "" : "&" + appendParams.substring(1))%>" target="_blank" class="btn btn-primary shadow-sm"><i class="fas fa-file-pdf"></i><%= Common.getBahasaConfig("Buka Surat") %></a>
                            </div>
                        </div>
                        <div class="col-md-6 col-xl-3">
                            <div class="document-action-card">
                                <div class="doc-icon"><i class="fas fa-display"></i></div>
                                <h4><%= Common.getBahasaConfig("Presentasi") %></h4>
                                <p><%= Common.getBahasaConfig("Materi siap tayang bergaya slide untuk menjelaskan alur registrasi, rekam medis, farmasi, billing, dashboard, dan manfaat implementasi eMedic kepada pimpinan fasilitas kesehatan.") %></p>
                                <a href="<%=Common.ROOT%>/presentasi?modul=kesehatan<%=(appendParams.isEmpty() ? "" : "&" + appendParams.substring(1))%>" target="_blank" class="btn btn-primary shadow-sm"><i class="fas fa-display"></i><%= Common.getBahasaConfig("Buka Presentasi") %></a>
                            </div>
                        </div>
                        <div class="col-md-6 col-xl-3">
                            <div class="document-action-card">
                                <div class="doc-icon"><i class="fas fa-file-lines"></i></div>
                                <h4><%= Common.getBahasaConfig("Proposal") %></h4>
                                <p><%= Common.getBahasaConfig("Dokumen kajian lengkap berisi latar belakang, ekosistem eMedic, rincian modul, roadmap integrasi BPJS/SATUSEHAT, metodologi implementasi, dan skema investasi untuk evaluasi internal.") %></p>
                                <a href="<%=Common.ROOT%>/proposal?modul=kesehatan<%=(appendParams.isEmpty() ? "" : "&" + appendParams.substring(1))%>" target="_blank" class="btn btn-primary shadow-sm"><i class="fas fa-file-alt"></i><%= Common.getBahasaConfig("Buka Proposal") %></a>
                            </div>
                        </div>
                        <div class="col-md-6 col-xl-3">
                            <div class="document-action-card">
                                <div class="doc-icon"><i class="fas fa-handshake"></i></div>
                                <h4><%= Common.getBahasaConfig("Draf PKS") %></h4>
                                <p><%= Common.getBahasaConfig("Rujukan awal untuk menyelaraskan ruang lingkup kerja sama modul eMedic, hak dan kewajiban, skema pembiayaan, SLA, kerahasiaan rekam medis, dan ketentuan formal para pihak.") %></p>
                                <a href="<%=Common.ROOT%>/pks?modul=kesehatan<%=(appendParams.isEmpty() ? "" : "&" + appendParams.substring(1))%>" target="_blank" class="btn btn-primary shadow-sm"><i class="fas fa-file-signature"></i><%= Common.getBahasaConfig("Buka PKS") %></a>
                            </div>
                        </div>
                    </div>

                    <div class="text-center position-relative mt-5 mb-4">
                        <div class="enterprise-ribbon-v2" style="background: rgba(234,88,12,0.1); color: #ea580c; border-color: rgba(234,88,12,0.25);">
                            <i class="fas fa-warehouse"></i><%= Common.getBahasaConfig("Versi Implementasi: Gudang Pusat, Outlet, POS, Ekspedisi, Investor & Akuntansi") %>
                        </div>
                        <p class="section-lead-premium mb-0 mt-2">
                            <%= Common.getBahasaConfig("Dokumen yang sama, disusun khusus untuk kebutuhan Sistem Gudang Pusat/Cabang, Outlet, POS (kasir), Ekspedisi, bagi hasil Investor, dan Akuntansi (HPP otomatis & jurnal terlacak) — untuk koperasi, kantin, toko, atau unit usaha ritel.") %>
                        </p>
                    </div>

                    <div class="row g-4">
                        <div class="col-md-6 col-xl-3">
                            <div class="document-action-card">
                                <div class="doc-icon"><i class="fas fa-envelope-open-text"></i></div>
                                <h4><%= Common.getBahasaConfig("Surat Penawaran") %></h4>
                                <p><%= Common.getBahasaConfig("Ringkasan resmi penawaran Sistem Gudang, Outlet, POS, Ekspedisi, Investor & Akuntansi: manfaat strategis, ruang lingkup modul, skema investasi, serta langkah tindak lanjut untuk proses administrasi awal.") %></p>
                                <a href="<%=Common.ROOT%>/penawaran?modul=gudang<%=(appendParams.isEmpty() ? "" : "&" + appendParams.substring(1))%>" target="_blank" class="btn btn-primary shadow-sm"><i class="fas fa-file-pdf"></i><%= Common.getBahasaConfig("Buka Surat") %></a>
                            </div>
                        </div>
                        <div class="col-md-6 col-xl-3">
                            <div class="document-action-card">
                                <div class="doc-icon"><i class="fas fa-display"></i></div>
                                <h4><%= Common.getBahasaConfig("Presentasi") %></h4>
                                <p><%= Common.getBahasaConfig("Materi siap tayang bergaya slide untuk menjelaskan alur Gudang, POS, distribusi, bagi hasil Investor, dan Akuntansi kepada pimpinan maupun tim pengambil keputusan.") %></p>
                                <a href="<%=Common.ROOT%>/presentasi?modul=gudang<%=(appendParams.isEmpty() ? "" : "&" + appendParams.substring(1))%>" target="_blank" class="btn btn-primary shadow-sm"><i class="fas fa-display"></i><%= Common.getBahasaConfig("Buka Presentasi") %></a>
                            </div>
                        </div>
                        <div class="col-md-6 col-xl-3">
                            <div class="document-action-card">
                                <div class="doc-icon"><i class="fas fa-file-lines"></i></div>
                                <h4><%= Common.getBahasaConfig("Proposal") %></h4>
                                <p><%= Common.getBahasaConfig("Dokumen kajian lengkap berisi latar belakang, ruang lingkup Gudang/Outlet/POS/Ekspedisi/Investor/Akuntansi, metodologi implementasi, dan skema investasi untuk evaluasi internal.") %></p>
                                <a href="<%=Common.ROOT%>/proposal?modul=gudang<%=(appendParams.isEmpty() ? "" : "&" + appendParams.substring(1))%>" target="_blank" class="btn btn-primary shadow-sm"><i class="fas fa-file-alt"></i><%= Common.getBahasaConfig("Buka Proposal") %></a>
                            </div>
                        </div>
                        <div class="col-md-6 col-xl-3">
                            <div class="document-action-card">
                                <div class="doc-icon"><i class="fas fa-handshake"></i></div>
                                <h4><%= Common.getBahasaConfig("Draf PKS") %></h4>
                                <p><%= Common.getBahasaConfig("Rujukan awal untuk menyelaraskan ruang lingkup kerja sama modul Gudang/POS/Ekspedisi/Investor/Akuntansi, hak dan kewajiban, skema pembiayaan, dan ketentuan formal para pihak.") %></p>
                                <a href="<%=Common.ROOT%>/pks?modul=gudang<%=(appendParams.isEmpty() ? "" : "&" + appendParams.substring(1))%>" target="_blank" class="btn btn-primary shadow-sm"><i class="fas fa-file-signature"></i><%= Common.getBahasaConfig("Buka PKS") %></a>
                            </div>
                        </div>
                    </div>

                    <div class="formal-note-box mt-4">
                        <div class="note-title"><i class="fas fa-route me-2"></i><%= Common.getBahasaConfig("Alur Rekomendasi untuk Calon Mitra") %></div>
                        <p class="note-text">
                            <%= Common.getBahasaConfig("Mulailah dari live demo dan presentasi, lanjutkan dengan kajian proposal, susun prioritas modul bersama tim teknis, finalisasi surat penawaran, kemudian tetapkan ruang lingkup dan SLA ke dalam PKS. Dengan alur ini, proses pengambilan keputusan menjadi lebih tertib, transparan, dan mudah dipertanggungjawabkan.") %>
                        </p>
                    </div>
                </div>
            </div>
        </div>


        <div id="section-contact" class="cta-section">
            <div class="section-eyebrow" style="background: rgba(255,255,255,0.14); color: #bae6fd;">
                <i class="fas fa-headset"></i><%= Common.getBahasaConfig("Konsultasi Resmi") %>
            </div>
            <h2 class="fw-bold mb-4" style="font-size: 2.7rem; letter-spacing: -0.04em;"><%= Common.getBahasaConfig("Siap Membawa Institusi Anda ke Level Digital Berikutnya?") %></h2>
            <p class="fs-5 mb-5 mx-auto text-light" style="max-width: 900px; line-height: 1.85;">
                <%= Common.getBahasaConfig("Enterprise Education merupakan investasi strategis untuk meningkatkan kualitas layanan, transparansi tata kelola, kecepatan administrasi, dan akurasi data institusi. Hubungi tim kami untuk presentasi, konsultasi kebutuhan, simulasi demo, pembahasan skema implementasi, serta estimasi pengembangan sesuai kondisi lembaga Anda.") %>
            </p>
            
            <div class="row justify-content-center g-4 mb-0 pb-3">
                <% if(paramPresentBy != null && !paramPresentBy.trim().isEmpty()) { %>
                    <div class="col-md-6 col-lg-5">
                        <div class="contact-box shadow bg-white text-dark p-4 rounded-4 text-center">
                            <i class="fas fa-user-tie fa-4x mb-3 text-primary"></i>
                            <h4 class="fw-bold text-dark mb-2"><%=paramPresentBy%></h4>
                            <div class="text-muted mb-4"><%= Common.getBahasaConfig("Konsultan Resmi Enterprise Education") %></div>
                            <% if(paramTelp != null && !paramTelp.trim().isEmpty()) { %>
                                <div class="fs-5 text-dark mb-3">
                                    <i class="fab fa-whatsapp text-success me-2"></i> <%=paramTelp%>
                                </div>
                                <a 
                                    href="https://wa.me/<%=paramTelp.replaceAll("[^0-9]", "")%>" 
                                    target="_blank" 
                                    class="btn btn-success rounded-pill px-4 py-2 fw-bold shadow-sm w-100"
                                >
                                    <i class="fab fa-whatsapp me-2"></i> Hubungi Sekarang
                                </a>
                            <% } %>
                        </div>
                    </div>
                <% } else { %>
                    <% 
                    String[][] team = {
                        {"Dr. Imam Marzuki Shofi", "Project Manager / Team Leader", "0818889801"},
                        {"Mohammad Fauzi M., M.T.I", "Senior Programmer & Implementator", "081382028582"},
                        {"Dr. Ir. Sulfikar Sallu", "Marketing, Tim Ahli IT/AI, Trainer", "0811402354"},
                        {"M. Asrofi Ridho, S.T", "Modul Mobile & Integrator", "081282846429"},
                        {"Benny Agung Setiawan, S.Kom", "Modul SDM & Tata Kelola Persuratan", "082298613505"},
                        {"Pepen Dedih, SE", "Modul Akuntansi & Finance", "081221415675"},
                        {"Daniel Rosa", "Implementator Modul Akademik", "08561244485"},
                        {"Andra Wibowo", "Implementator Modul Pembayaran", "082288303926"},
                        {"Gerry Alenius", "Marketing eCampus/eSchool", "08119444417"},
                        {"Wawan Setiawan", "Implementator & Agregator Payment", "085850526148"}
                    };
                    for(String[] member : team) {
                    %>
                    <div class="col-md-6 col-lg-4">
                        <div class="contact-box shadow">
                            <i class="fas fa-user-tie fa-3x mb-4 text-info"></i>
                            <h5 class="fs-6 lh-base"><%=member[0]%></h5>
                            <div class="role-desc"><%=member[1]%></div>
                            <div class="wa-link-container">
                                <p class="mb-0 fs-6">
                                    <i class="fab fa-whatsapp me-2 text-success"></i>
                                    <a 
                                        href="https://wa.me/62<%=member[2]%>" 
                                        target="_blank" 
                                        class="text-white text-decoration-none fw-semibold"
                                    >+62 <%=member[2]%></a>
                                </p>
                            </div>
                        </div>
                    </div>
                    <% } %>
                <% } %>
            </div>
        </div>

    </main>

    <% if(paramTelp != null && !paramTelp.trim().isEmpty()) { %>
    <a href="https://wa.me/<%=paramTelp.replaceAll("[^0-9]", "")%>" target="_blank" class="sticky-cta-floating">
        <i class="fab fa-whatsapp fs-4"></i><span><%= Common.getBahasaConfig("Konsultasi Sekarang") %></span>
    </a>
    <% } else { %>
    <a href="https://wa.me/62816789081" target="_blank" class="sticky-cta-floating">
        <i class="fab fa-whatsapp fs-4"></i><span><%= Common.getBahasaConfig("Konsultasi Sekarang") %></span>
    </a>
    <% } %>

    <footer class="footer-custom shadow-lg">
        <div class="container">
            <div class="row text-center text-md-start">
                
                <div class="col-md-5 col-lg-4 mb-4">
                    <h5 class="text-uppercase">
                        <i class="fas fa-graduation-cap me-2 text-info"></i> <%=judulHeader%>
                    </h5>
                    <p class="text-sm mt-3 pe-md-4" style="line-height: 1.8;">
                        <%= Common.getBahasaConfig("Infrastruktur digital terintegrasi yang didedikasikan untuk mengakselerasi kualitas pendidikan melalui otomatisasi birokrasi dan validitas data di lingkungan") %> <strong class="text-white"><%=judul%></strong>.
                    </p>
                </div>

                <div class="col-md-3 col-lg-4 mb-4 text-center">
                    <h5 class="text-uppercase"><%= Common.getBahasaConfig("Unduh Aplikasi Mobile") %></h5>
                    <div class="d-flex flex-column align-items-center mt-4 gap-3">
                        <a 
                            target="_blank" 
                            href="https://play.google.com/store/apps/details?id=com.ecampus.zishof" 
                            class="footer-link d-flex align-items-center bg-dark p-3 rounded-3 border border-secondary w-100" 
                            style="max-width: 250px;"
                        >
                            <img 
                                src="https://upload.wikimedia.org/wikipedia/commons/d/d0/Google_Play_Arrow_logo.svg" 
                                alt="Google Play" 
                                class="store-icon"
                            /> 
                            <span class="ms-2 fw-semibold"><%= Common.getBahasaConfig("Google Play") %></span>
                        </a> 
                        <a 
                            target="_blank" 
                            href="https://apps.apple.com/id/app/ecampus/id6503487876?l=id" 
                            class="footer-link d-flex align-items-center bg-dark p-3 rounded-3 border border-secondary w-100" 
                            style="max-width: 250px;"
                        >
                            <img 
                                src="https://upload.wikimedia.org/wikipedia/commons/6/67/App_Store_%28iOS%29.svg" 
                                alt="App Store" 
                                class="store-icon"
                            /> 
                            <span class="ms-2 fw-semibold"><%= Common.getBahasaConfig("App Store") %></span>
                        </a>
                    </div>
                </div>

                <div class="col-md-4 col-lg-4 mb-4">
                    <h5 class="text-uppercase"><%= Common.getBahasaConfig("Informasi Kontak Institusi") %></h5>
                    <ul class="list-unstyled mt-4">
                        <li class="mb-3 d-flex align-items-start text-start">
                            <i class="fas fa-map-marker-alt contact-icon mt-1 me-3 fs-5"></i>
                            <span style="line-height: 1.6;"><%=Alamat1%></span>
                        </li>
                        <li class="mb-3 d-flex align-items-center text-start">
                            <i class="fas fa-phone-alt contact-icon me-3 fs-5"></i>
                            <% if(paramTelp != null && !paramTelp.trim().isEmpty()) { %>
                                <span>HP/WA: <%=paramTelp%></span>
                            <% } else { %>
                                <span><%=Telepon%> <br> HP: 081 6789 081</span>
                            <% } %>
                        </li>
                        <% if(paramPresentBy == null || paramPresentBy.trim().isEmpty()) { %>
                        <li class="mb-3 d-flex align-items-center text-start">
                            <i class="fas fa-envelope contact-icon me-3 fs-5"></i>
                            <span>zishof@gmail.com</span>
                        </li>
                        <% } %>
                    </ul>
                </div>
            </div>

            <hr class="border-secondary mt-5 mb-4 opacity-25">

            <div class="row align-items-center">
                <div class="col-md-6 text-center text-md-start mb-4 mb-md-0">
                    <small>
                        &copy; <%=Calendar.getInstance().get(Calendar.YEAR) %> <%=judulHeader%> - <%=judul%>. <%= Common.getBahasaConfig("Seluruh Hak Cipta Dilindungi.") %>
                    </small>
                </div>
                <div class="col-md-6 text-center text-md-end">
                    <a target="_blank" href="https://info.flagcounter.com/lOr6">
                        <img 
                            src="https://s11.flagcounter.com/count2/lOr6/bg_0F172A/txt_CBD5E0/border_38BDF8/columns_2/maxflags_6/viewers_0/labels_0/pageviews_0/flags_0/percent_0/" 
                            alt="Flag Counter" 
                            class="img-fluid rounded border border-info border-opacity-25 opacity-75 hover-opacity-100 transition-opacity"
                        >
                    </a>
                </div>
            </div>
        </div>
    </footer>

    <div class="modal fade" id="imageModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered modal-xl">
        <div class="modal-content bg-transparent border-0">
          <div class="modal-header border-0 pb-0 justify-content-end">
            <button 
                type="button" 
                class="btn-close btn-close-white" 
                data-bs-dismiss="modal" 
                aria-label="Close" 
                style="filter: invert(1) grayscale(100%) brightness(200%);"
            ></button>
          </div>
          <div class="modal-body text-center p-0">
            <img 
                id="modalExpandedImage" 
                src="" 
                class="img-fluid rounded-4 shadow-lg" 
                alt="Expanded Hardware Image" 
                style="max-height: 85vh; width: auto;"
            >
          </div>
        </div>
      </div>
    </div>

    <script src="<%=request.getContextPath() %>/js/pesan-formal.js"></script>
    <script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>

    <script>
        document.addEventListener('DOMContentLoaded', function() {
            // Logika untuk mendeteksi scroll dan menyorot menu navigasi yang sedang aktif
            const navLinks = document.querySelectorAll('.custom-nav-link');
            const sections = document.querySelectorAll('div[id^="section-"]');

            window.addEventListener('scroll', () => {
                let current = '';
                sections.forEach(section => {
                    const sectionTop = section.offsetTop;
                    if (scrollY >= sectionTop - 150) {
                        current = section.getAttribute('id');
                    }
                });

                navLinks.forEach(link => {
                    link.classList.remove('active');
                    if (link.getAttribute('href').includes(current)) {
                        link.classList.add('active');
                    }
                });
            });

            // ========================================================================================
            // KAMUS DATA MODUL LENGKAP
            // Dictionary ini menyimpan semua informasi spesifik untuk masing-masing modul ERP
            // ========================================================================================
            const moduleDict = {
                "login_ecampus": {
                    title: '<%= Common.getBahasaConfigJS("Sistem Login eCampus") %>',
                    desc: '<%= Common.getBahasaConfigJS("Modul portal autentikasi terpusat yang menjadi pintu gerbang utama bagi seluruh pengguna untuk memasuki ekosistem digital institusi pendidikan tinggi. Dilengkapi antarmuka spesifik (Multi-Portal) yang didesain terpisah untuk kebutuhan Dosen, Mahasiswa, maupun Orang Tua/Wali.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Memastikan keamanan privasi data sekaligus memberikan kemudahan akses instan ke seluruh layanan akademik dan administratif.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Ditenagai oleh teknologi enkripsi mutakhir dengan dukungan Single Sign-On (SSO) untuk sinkronisasi antarmodul secara menyeluruh.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Memvalidasi identitas pengguna, mengelola sesi login, dan mendistribusikan hak akses sesuai dengan struktur organisasi.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Mahasiswa, Dosen, Tenaga Kependidikan, Orang Tua, dan Pimpinan Institusi.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Sistem Login eCampus memberikan fondasi keamanan yang solid dan kemudahan akses tak terbatas untuk memaksimalkan produktivitas seluruh entitas akademik melalui portal yang dipersonalisasi.") %>',
                    requireLogin: true
                },
                "pelaporan": {
                    title: '<%= Common.getBahasaConfigJS("Sistem Sinkronisasi Pelaporan Nasional") %>',
                    desc: '<%= Common.getBahasaConfigJS("Modul integrasi cerdas yang menjembatani database institusi dengan sistem pelaporan akademik tingkat kementerian, seperti PDDIKTI Neo Feeder dan EMIS Kemenag.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Membebaskan operator kampus dari kewajiban melakukan entri data ganda (double-entry) yang memakan waktu dan berisiko memunculkan perbedaan (discrepancy) data.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Proses sinkronisasi otomatis menggunakan metode 1-Click Sync API yang dikurasi khusus agar 100% kompatibel dengan struktur web service dikti/kemenag terbaru.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Mentransmisikan data riwayat mahasiswa, rekapitulasi nilai, aktivitas dosen, hingga beban mengajar langsung ke sistem pusat secara masif dan akurat.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Operator PDDIKTI, Admin EMIS, dan Pimpinan Bagian Akademik.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Menjamin kepatuhan administrasi dan validitas data institusi Anda di tingkat nasional secara seketika, cerdas, dan efisien tanpa kompromi.") %>',
                    requireLogin: false
                },
                "tracer_study": {
                    title: '<%= Common.getBahasaConfigJS("Sistem Pelacakan Alumni (Tracer Study)") %>',
                    desc: '<%= Common.getBahasaConfigJS("Fasilitas komprehensif untuk melacak dan mengevaluasi jejak karir serta tingkat penyerapan lulusan di dunia kerja profesional.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Menyediakan data empiris yang krusial untuk evaluasi relevansi kurikulum pendidikan terhadap kebutuhan industri pasar saat ini.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Instrumen kuesioner yang dapat dikustomisasi sepenuhnya dengan kapabilitas analitik dan visualisasi data yang responsif.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Mendistribusikan survei secara masif, merekam respons alumni, dan memetakan profil karir untuk keperluan pelaporan akreditasi.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Alumni, Unit Pengelola Karir Institusi, dan Badan Akreditasi.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Mendukung institusi dalam memetakan tingkat kesuksesan alumni di dunia kerja untuk dasar peningkatan mutu kurikulum berkelanjutan.") %>',
                    requireLogin: false
                },
                "pmb": {
                    title: '<%= Common.getBahasaConfigJS("Penerimaan Mahasiswa Baru (PMB)") %>',
                    desc: '<%= Common.getBahasaConfigJS("Sistem otomasi untuk mengelola seluruh siklus penerimaan peserta didik baru, dari tahap registrasi awal hingga proses daftar ulang.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Meningkatkan efisiensi kerja panitia penerimaan sekaligus memberikan pengalaman pendaftaran yang intuitif bagi para calon mahasiswa.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Mendukung integrasi pembayaran daring (Payment Gateway), seleksi berbasis komputer (CBT), serta notifikasi status secara real-time.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Mengelola formulir pendaftaran, memverifikasi dokumen prasyarat, menyelenggarakan ujian seleksi, dan mengumumkan hasil akhir.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Calon Mahasiswa Baru, Orang Tua/Wali, dan Panitia PMB.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Menghadirkan proses seleksi dan pendaftaran calon mahasiswa baru yang mutakhir, efisien, dan transparan bagi semua pihak.") %>',
                    requireLogin: false
                },
                "dokumen": {
                    title: '<%= Common.getBahasaConfigJS("Manajemen Dokumen & Persuratan") %>',
                    desc: '<%= Common.getBahasaConfigJS("Repositori digital terpusat yang dirancang khusus untuk mengelola, menyimpan, dan melacak seluruh arsip serta surat-menyurat resmi institusi.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Mengeliminasi penggunaan kertas (paperless), mempercepat proses pencarian arsip lampau, dan meminimalisasi risiko kehilangan dokumen vital.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Sistem alur persetujuan (approval workflow) bertingkat yang fleksibel dengan dukungan tanda tangan elektronik terotentikasi.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Mendokumentasikan surat masuk/keluar, mengelola hak akses baca, serta memfasilitasi disposisi tugas antardepartemen.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Pimpinan Institusi, Staf Kesekretariatan, dan Tenaga Administrasi.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Digitalisasi persuratan menjamin keamanan tata kelola arsip, menghemat ruang fisik, dan mempercepat alur birokrasi institusi.") %>',
                    requireLogin: false
                },
                "dashboard": {
                    title: '<%= Common.getBahasaConfigJS("Executive Dashboard (Dasbor Eksekutif)") %>',
                    desc: '<%= Common.getBahasaConfigJS("Panel visualisasi data tingkat tinggi yang menyajikan ringkasan informasi strategis dan performa operasional institusi secara real-time.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Mempercepat proses pengambilan keputusan strategis di tingkat pimpinan berbasiskan data faktual (data-driven decision making).") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Tampilan infografis yang interaktif, indikator kinerja utama (KPI) yang komprehensif, dan kemampuan penelusuran data yang mendalam.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Memonitor metrik krusial seperti statistik demografi, serapan anggaran keuangan, dan pencapaian target akademik secara presisi.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Rektor, Ketua Yayasan, Dekan, dan Jajaran Manajemen Tingkat Atas.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Menyediakan pusat informasi analitik terpadu bagi pimpinan eksekutif guna menunjang akurasi pengambilan keputusan manajerial.") %>',
                    requireLogin: false
                },
                "pustaka": {
                    title: '<%= Common.getBahasaConfigJS("Perpustakaan Digital (E-Library)") %>',
                    desc: '<%= Common.getBahasaConfigJS("Platform manajemen perpustakaan modern yang mengotomatisasi sirkulasi literatur fisik dan menyediakan akses ke koleksi literatur digital secara luas.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Meningkatkan literasi sivitas akademika dengan memberikan akses referensi bahan ajar yang cepat, mudah, dan tidak terbatas ruang.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Terintegrasi dengan Online Public Access Catalog (OPAC), sistem pemindaian sirkulasi kode batang (barcode), dan automasi denda.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Mengelola inventaris buku, memfasilitasi proses peminjaman/pengembalian, serta menerbitkan surat keterangan bebas pustaka.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Mahasiswa, Dosen, Peneliti, dan Staf Kepustakaan.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Mendorong tingkat literasi sivitas akademika melalui manajemen tata kelola perpustakaan yang modern, responsif, dan kaya referensi.") %>',
                    requireLogin: false
                },
                "repository": {
                    title: '<%= Common.getBahasaConfigJS("Repositori Karya Ilmiah (E-Repository)") %>',
                    desc: '<%= Common.getBahasaConfigJS("Sistem pengarsipan digital terstruktur untuk menyimpan, melestarikan, dan mendistribusikan karya intelektual yang dihasilkan oleh sivitas akademika.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Mencegah praktik plagiarisme, melindungi hak kekayaan intelektual institusi, serta memperluas jangkauan diseminasi hasil riset kepada publik.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Kompatibel dengan standar protokol metadata internasional (seperti DSpace), fitur pencarian spesifik, dan indeksasi publikasi yang handal.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Menyimpan tesis, disertasi, prosiding, serta artikel jurnal dalam format digital yang terenkripsi dan mudah diklasifikasikan.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Dosen Peneliti, Mahasiswa Akhir, dan Komunitas Akademik Global.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Melestarikan warisan karya intelektual institusi dan memperluas eksistensi publikasi riset di ruang lingkup akademik internasional.") %>',
                    requireLogin: false
                },
                "akreditasi": {
                    title: '<%= Common.getBahasaConfigJS("Sistem Penjaminan Mutu & Akreditasi") %>',
                    desc: '<%= Common.getBahasaConfigJS("Modul strategis untuk mengawal pemenuhan standar pendidikan nasional dan mempersiapkan kelengkapan dokumen akreditasi secara sistematis.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Meringankan beban administratif saat proses re-akreditasi dan memastikan setiap departemen mematuhi standar mutu yang telah ditetapkan.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Instrumen matriks penilaian yang selalu dimutakhirkan mengikuti pedoman lembaga akreditasi resmi (seperti BAN-PT/LAM).") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Menghimpun bukti fisik, mengkalkulasi borang akreditasi, serta memantau progres perbaikan mutu secara berkelanjutan.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Badan Penjaminan Mutu (BPM), Asesor Internal, dan Pimpinan Program Studi.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Memastikan budaya kualitas institusi senantiasa terjaga melalui pengawalan standarisasi mutu pendidikan yang terkomputerisasi secara ketat.") %>',
                    requireLogin: false
                },
                "ejournal": {
                    title: '<%= Common.getBahasaConfigJS("Manajemen Jurnal Ilmiah (E-Journal)") %>',
                    desc: '<%= Common.getBahasaConfigJS("Platform tata kelola penerbitan jurnal akademik elektronik, dari tahap penyerahan naskah, tinjauan sejawat (peer-review), hingga tahap publikasi akhir.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Mengangkat reputasi akademik institusi dengan memfasilitasi proses publikasi jurnal yang kredibel, terstandarisasi, dan diakui secara global.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Alur kerja redaksional transparan yang mengadaptasi standar Open Journal Systems (OJS) secara penuh.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Menerima manuskrip dari penulis, menugaskan peninjau, mengelola revisi, serta menerbitkan edisi jurnal secara berkala.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Penulis Akademik, Dewan Redaksi Jurnal, Reviewer, dan Peneliti.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Meningkatkan prestise dan kontribusi riset institusi melalui tata kelola jurnal keilmuan yang profesional, terakreditasi, dan bereputasi tinggi.") %>',
                    requireLogin: false
                },
                "simlitabmas": {
                    title: '<%= Common.getBahasaConfigJS("Penelitian & Pengabdian Masyarakat (Simlitabmas)") %>',
                    desc: '<%= Common.getBahasaConfigJS("Sistem informasi khusus untuk mengkoordinasikan program tridharma institusi, khususnya dalam bidang riset dan pengabdian kepada masyarakat.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Meningkatkan transparansi alokasi dana hibah penelitian dan mempermudah pemantauan luaran (output) proyek secara komprehensif.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Fasilitas pengajuan proposal terstruktur, evaluasi anggaran (RAB), dan peninjauan progres (logbook) yang terpusat.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Menyeleksi proposal penelitian, mengelola kontrak hibah pendanaan, dan mempublikasikan hasil pengabdian masyarakat.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Lembaga Penelitian (LPPM), Dosen Peneliti, dan Mahasiswa.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Mengakselerasi kuantitas serta kualitas program riset dan pengabdian masyarakat melalui manajemen pendanaan hibah yang sangat transparan.") %>',
                    requireLogin: false
                },
                "yayasan": {
                    title: '<%= Common.getBahasaConfigJS("Portal Pengelola Yayasan") %>',
                    desc: '<%= Common.getBahasaConfigJS("Modul komando tertinggi yang dirancang khusus bagi entitas pemilik/yayasan untuk memonitor seluruh cabang institusi pendidikan di bawah naungannya secara tersentralisasi.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Memberikan visibilitas menyeluruh terkait stabilitas keuangan, statistik kepegawaian, dan tren demografi siswa dari seluruh unit sekolah tanpa harus hadir secara fisik.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Konsolidasi laporan lintas unit yang disajikan dalam dasbor analitik canggih, memungkinkan perbandingan kinerja antar cabang dengan tingkat akurasi tinggi.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Memantau arus kas yayasan, merumuskan kebijakan anggaran strategis, mengendalikan aset utama, dan mengevaluasi indikator kinerja (KPI) unit pendidikan.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Ketua Yayasan, Dewan Pembina, dan Direktur Operasional.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Memberikan kendali pengawasan mutlak bagi pihak yayasan untuk mengevaluasi, membandingkan, serta mengembangkan seluruh cabang pendidikan secara harmonis.") %>',
                    requireLogin: true
                },
                "admin_tk": {
                    title: '<%= Common.getBahasaConfigJS("Portal Manajemen Sekolah (TK)") %>',
                    desc: '<%= Common.getBahasaConfigJS("Sistem administrasi terpadu yang dirancang untuk mengelola tata operasional Taman Kanak-Kanak secara efisien.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Mereduksi beban kerja klerikal staf dan memastikan data perkembangan peserta didik tercatat secara sistematis dan rapi.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Antarmuka yang intuitif serta tersinkronisasi penuh dengan laporan keuangan, penilaian harian, dan modul absensi.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Mengelola biodata murid, jadwal kegiatan mengajar, sistem pembayaran SPP, dan penerbitan laporan perkembangan belajar.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Kepala Sekolah TK, Guru Wali Kelas, dan Staf Tata Usaha.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Menyederhanakan kompleksitas administrasi pendidikan usia dini agar tenaga pendidik dapat lebih memfokuskan diri pada stimulasi perkembangan esensial anak.") %>',
                    requireLogin: true
                },
                "admin_sd": {
                    title: '<%= Common.getBahasaConfigJS("Portal Manajemen Sekolah (SD)") %>',
                    desc: '<%= Common.getBahasaConfigJS("Sistem administrasi terpadu yang dirancang untuk mengelola tata operasional Sekolah Dasar secara efisien dan komprehensif.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Mereduksi beban kerja klerikal staf dan memastikan data akademik serta kehadiran peserta didik tercatat secara sistematis.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Antarmuka yang intuitif serta tersinkronisasi penuh dengan laporan keuangan, pengisian e-Rapor, dan modul absensi presisi.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Mengelola biodata siswa, jadwal mata pelajaran, sistem penagihan SPP, dan penerbitan buku laporan hasil belajar.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Kepala Sekolah SD, Guru Kelas, dan Staf Tata Usaha.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Mengintegrasikan fungsionalitas manajemen akademik dan manajerial secara utuh guna menciptakan pondasi lingkungan pendidikan dasar yang solid.") %>',
                    requireLogin: true
                },
                "admin_smp": {
                    title: '<%= Common.getBahasaConfigJS("Portal Manajemen Sekolah (SMP)") %>',
                    desc: '<%= Common.getBahasaConfigJS("Sistem administrasi terpadu yang dirancang untuk mengelola tata operasional Sekolah Menengah Pertama secara modern dan terstruktur.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Meningkatkan disiplin data akademik dan mengotomatisasi perhitungan komponen nilai secara akurat tanpa campur tangan manual.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Mendukung manajemen kurikulum yang kompleks, penjadwalan guru bidang studi, serta pemantauan pelanggaran tata tertib siswa.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Merekam absensi, mempublikasikan jadwal ujian, mengelola tagihan keuangan, serta memfasilitasi komunikasi wali murid.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Kepala Sekolah SMP, Guru Bidang Studi, Guru BK, dan Staf Tata Usaha.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Mendukung dinamika pendidikan menengah dengan instrumen pengelolaan evaluasi belajar, penagihan, serta pengawasan tingkat kedisiplinan yang tertata rapi.") %>',
                    requireLogin: true
                },
                "admin_sma": {
                    title: '<%= Common.getBahasaConfigJS("Portal Manajemen Sekolah (SMA)") %>',
                    desc: '<%= Common.getBahasaConfigJS("Sistem administrasi tingkat lanjut yang diformulasikan untuk mengelola dinamika operasional Sekolah Menengah Atas beserta sistem penjurusannya.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Mempermudah proses peminatan/penjurusan akademik serta memantau kesiapan siswa menuju fase pendidikan tinggi.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Kapasitas pemrosesan data lintas jurusan (IPA/IPS/Bahasa) dan integrasi penilaian berbasis proyek sesuai standar nasional.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Menyusun rombongan belajar, mendistribusikan transkrip nilai, memantau kegiatan ekstrakurikuler, dan mengelola bimbingan konseling.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Kepala Sekolah SMA, Wakil Kepala Sekolah, Guru Mata Pelajaran, dan Guru BK.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Mempersiapkan kematangan akademik siswa menuju jenjang pendidikan tinggi melalui manajemen evaluasi penjurusan yang sangat komprehensif.") %>',
                    requireLogin: true
                },
                "admin_smk": {
                    title: '<%= Common.getBahasaConfigJS("Portal Manajemen Sekolah (SMK)") %>',
                    desc: '<%= Common.getBahasaConfigJS("Sistem pengelolaan vokasi komprehensif yang dirancang secara spesifik untuk memfasilitasi operasional Sekolah Menengah Kejuruan.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Menjembatani manajemen akademik teoritis dengan pengelolaan kegiatan praktik industri secara terpadu di satu platform.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Dilengkapi dengan fasilitas khusus untuk mengelola program magang, inventaris bengkel praktik, dan pencatatan sertifikasi keahlian.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Mengelola penempatan Praktik Kerja Lapangan (PKL), memvalidasi laporan magang, serta menyusun nilai produktif kejuruan.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Kepala Sekolah SMK, Ketua Program Keahlian, Pembimbing PKL, dan Siswa Vokasi.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Mensinergikan kebutuhan pendidikan teori dan praktik kejuruan lapangan guna melahirkan talenta siap kerja yang kompetitif di dunia industri.") %>',
                    requireLogin: true
                },
                "ppdb_tk": {
                    title: '<%= Common.getBahasaConfigJS("Portal PPDB (Taman Kanak-Kanak)") %>',
                    desc: '<%= Common.getBahasaConfigJS("Fasilitas pendaftaran daring untuk mengelola penerimaan peserta didik jenjang TK secara digital, terpusat, dan bebas antrean.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Memberikan kemudahan maksimal bagi orang tua untuk mendaftarkan anak mereka dari rumah tanpa perlu menyerahkan formulir fisik.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Alur pendaftaran yang sangat ringkas, validasi usia otomatis, dan sistem pembayaran biaya formulir terintegrasi bank.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Merekam data calon murid, mengunggah dokumen persyaratan dasar, dan mempublikasikan status penerimaan secara transparan.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Orang Tua/Wali Calon Murid dan Panitia PPDB.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Mewujudkan proses penerimaan murid tingkat usia dini yang mudah, inklusif, dan sepenuhnya terotomatisasi secara daring.") %>',
                    requireLogin: false
                },
                "ppdb_sd": {
                    title: '<%= Common.getBahasaConfigJS("Portal PPDB (Sekolah Dasar)") %>',
                    desc: '<%= Common.getBahasaConfigJS("Fasilitas registrasi elektronik yang memfasilitasi panitia dan pendaftar dalam proses seleksi penerimaan murid Sekolah Dasar baru.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Mengurangi kerumitan administrasi manual saat tahun ajaran baru serta menjamin akurasi data pendaftar sejak tahap awal.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Sistem pemberkasan digital yang aman, pemantauan kuota daya tampung secara real-time, dan notifikasi konfirmasi penerimaan.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Menyelenggarakan alur pendaftaran, memverifikasi keabsahan dokumen, dan menghasilkan laporan rekapitulasi siswa baru.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Orang Tua/Wali Calon Siswa dan Panitia Pendaftaran.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Menjamin terselenggaranya alur pendaftaran murid dasar yang tertib, aman, serta terhindar dari potensi kelalaian input manual.") %>',
                    requireLogin: false
                },
                "ppdb_smp": {
                    title: '<%= Common.getBahasaConfigJS("Portal PPDB (Sekolah Menengah Pertama)") %>',
                    desc: '<%= Common.getBahasaConfigJS("Sistem seleksi penerimaan siswa baru jenjang SMP yang mengakomodasi berbagai jalur masuk sesuai regulasi dinas pendidikan terkini.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Menciptakan proses seleksi yang transparan, adil, dan objektif berdasarkan kriteria nilai atau zonasi wilayah yang terukur.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Kapasitas pemrosesan data bervolume tinggi, pengelompokan jalur prestasi/afirmasi/zonasi, dan fitur peringkat (ranking) dinamis.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Menghitung pembobotan nilai pendaftar, memverifikasi dokumen jalur khusus, dan menentukan kelulusan secara presisi.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Calon Siswa SMP, Orang Tua/Wali, dan Tim Seleksi.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Mengakomodasi penyaringan siswa baru secara berkeadilan tinggi melalui kalkulasi parameter zonasi dan prestasi yang transparan.") %>',
                    requireLogin: false
                },
                "ppdb_sma": {
                    title: '<%= Common.getBahasaConfigJS("Portal PPDB (Sekolah Menengah Atas)") %>',
                    desc: '<%= Common.getBahasaConfigJS("Infrastruktur pendaftaran siswa baru jenjang SMA yang dirancang untuk menangani seleksi ketat dan analisis peminatan akademik.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Membantu institusi mendapatkan kandidat siswa terbaik melalui instrumen seleksi komprehensif tanpa campur tangan manual yang berisiko.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Modul tes psikometri/peminatan terintegrasi, pemetaan nilai rapor SMP, dan pengelolaan kuota jalur masuk yang fleksibel.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Menyelenggarakan ujian saringan masuk, melakukan pemeringkatan otomatis, dan memfasilitasi administrasi daftar ulang.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Calon Siswa SMA, Orang Tua, dan Panitia Penerimaan Tingkat Menengah.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Menjamin mutu rekrutmen siswa berkualitas tinggi dengan integrasi sistem pemeringkatan analitis dan pengawasan daya tampung fleksibel.") %>',
                    requireLogin: false
                },
                "ppdb_smk": {
                    title: '<%= Common.getBahasaConfigJS("Portal PPDB (Sekolah Menengah Kejuruan)") %>',
                    desc: '<%= Common.getBahasaConfigJS("Platform penerimaan siswa baru vokasi yang menangani pendaftaran lintas program keahlian dengan kriteria seleksi fisik dan kompetensi.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Mengelola kompleksitas pendaftaran berbagai jurusan keahlian secara rapi dan mencegah terjadinya penumpukan berkas fisik di loket.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Formulir spesifik berbasis jurusan, integrasi hasil tes kesehatan/buta warna, dan sinkronisasi otomatis ke data induk siswa vokasi.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Memfasilitasi pemilihan program studi keahlian, mengatur jadwal wawancara, dan mengamankan tahapan penyelesaian pembayaran awal.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Calon Siswa Vokasi, Tim Seleksi Kejuruan, dan Manajemen Sekolah.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Mendukung kelancaran seleksi calon siswa jurusan secara sangat presisi, aman, dan tanpa kendala administrasi dokumen yang menumpuk.") %>',
                    requireLogin: false
                },
                "ekantin": {
                    title: '<%= Common.getBahasaConfigJS("Kantin Digital (eKantin)") %>',
                    desc: '<%= Common.getBahasaConfigJS("Sistem manajemen kantin modern yang memfasilitasi ekosistem transaksi tanpa uang tunai (cashless) di lingkungan institusi pendidikan.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Mewujudkan lingkungan kantin yang higienis, mempercepat antrean transaksi, serta memberikan kemudahan kontrol pengeluaran bagi orang tua siswa.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Terintegrasi penuh dengan kartu identitas pintar (RFID/NFC), pengisian saldo terpusat, dan riwayat mutasi yang transparan.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Memproses pembayaran digital, mencatat stok inventaris pedagang, dan membatasi limit belanja harian peserta didik sesuai kebijakan orang tua.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Peserta Didik, Pengelola/Pedagang Kantin, dan Orang Tua/Wali.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Menciptakan ekosistem kantin higienis, modern, terintegrasi, serta menjamin kenyamanan pengawasan sirkulasi finansial bagi orang tua murid.") %>',
                    requireLogin: false
                },
                "anjungan": {
                    title: '<%= Common.getBahasaConfigJS("Anjungan Layanan Mandiri (Kiosk)") %>',
                    desc: '<%= Common.getBahasaConfigJS("Fasilitas terminal layar sentuh terintegrasi yang ditempatkan di lobi atau area strategis institusi. Memungkinkan peserta didik untuk mengakses dan mencetak dokumen administrasi akademik secara mandiri (self-service) tanpa perlu mengantre di loket Tata Usaha.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Mereduksi hingga 70% beban kerja klerikal staf administrasi dan menghilangkan antrean panjang, sekaligus memberikan pengalaman layanan yang instan dan modern bagi mahasiswa/siswa.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Dapat diintegrasikan dengan perangkat keras industrial (Touchscreen, Thermal Printer/Epson L120, Scanner Barcode/RFID) dan beroperasi 24/7 dengan antarmuka yang sangat ramah pengguna.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Pencetakan KRS, KHS, Transkrip Nilai, Surat Keterangan Aktif, pengecekan jadwal, tagihan keuangan, dan informasi pengumuman akademik secara real-time.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Mahasiswa, Siswa, Santri, dan Staf Administrasi/Tata Usaha.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Mewujudkan transformasi lobi institusi menjadi pusat layanan digital yang mandiri, responsif, dan bebas antrean melalui integrasi sistem pintar.") %>',
                    requireLogin: true
                },
                "pos": {
                    title: '<%= Common.getBahasaConfigJS("Sistem Kasir Terpadu (Point Of Sale)") %>',
                    desc: '<%= Common.getBahasaConfigJS("Aplikasi perangkat lunak kasir pintar yang didesain khusus untuk mengelola unit usaha institusi seperti koperasi, toko alat tulis, atau fasilitas penatu (laundry).") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Meminimalisasi kebocoran finansial (fraud) pada unit usaha dan menghasilkan rekapitulasi laba rugi yang akurat secara instan.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Antarmuka layar sentuh (touchscreen) yang sangat responsif, manajemen stok barang real-time, dan pemindai kode batang (barcode scanner) terpadu.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Mencatat transaksi penjualan, mengkalkulasi harga pokok penjualan (HPP), mencetak struk digital, dan menerbitkan laporan arus kas unit usaha.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Petugas Koperasi, Kasir Unit Usaha, dan Bagian Keuangan Institusi.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Mengamankan tata kelola persediaan barang dan kalkulasi kasir untuk memaksimalkan laju profitabilitas unit bisnis institusi pendidikan.") %>',
                    requireLogin: false
                },
                "kursus": {
                    title: '<%= Common.getBahasaConfigJS("Manajemen Sistem Kursus") %>',
                    desc: '<%= Common.getBahasaConfigJS("Platform khusus untuk merencanakan, mengelola, dan mengevaluasi program pelatihan kompetensi tambahan atau kursus ekstrakurikuler di luar kegiatan akademik utama.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Menyederhanakan proses registrasi peserta pelatihan dan menjamin distribusi materi kursus tersampaikan secara efektif.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Fitur penerbitan sertifikat elektronik (e-certificate) secara otomatis dan pengelompokan jadwal berdasarkan gelombang pelatihan (batch).") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Membuka pendaftaran kelas pelatihan, mengelola presensi instruktur/peserta, dan memonitor kelulusan program sertifikasi.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Lembaga Pelatihan/Sertifikasi Internal, Instruktur Praktik, dan Peserta Kursus.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Mengoptimalkan administrasi penyelenggaraan pelatihan intensif bersertifikat secara digital penuh guna meningkatkan nilai tambah lulusan.") %>',
                    requireLogin: false
                },
                "les": {
                    title: '<%= Common.getBahasaConfigJS("Sistem Bimbingan Les / Private") %>',
                    desc: '<%= Common.getBahasaConfigJS("Sistem penjadwalan dan manajemen operasional untuk program bimbingan belajar intensif, tambahan pelajaran, atau layanan les privat bagi peserta didik.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Mengoptimalkan penugasan tenaga pengajar privat dan memastikan setiap sesi pertemuan terekam untuk keperluan penagihan dan evaluasi.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Sistem penagihan presisi berbasis jumlah kehadiran (pay-per-session), penyusunan jadwal personalisasi, dan formulir umpan balik capaian belajar.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Mencocokkan ketersediaan tutor dengan murid, merekam agenda bimbingan, dan menghasilkan lembar kemajuan belajar (progress report).") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Tutor/Pengajar Les, Siswa Bimbingan Intensif, dan Koordinator Akademik.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Memberikan layanan manajemen penjadwalan bimbingan belajar yang terstruktur, rapi, dengan fitur kalkulasi penagihan sesi tutor yang sangat presisi.") %>',
                    requireLogin: false
                },
                "karir": {
                    title: '<%= Common.getBahasaConfigJS("Lowongan Pekerjaan / Karir") %>',
                    desc: '<%= Common.getBahasaConfigJS("Modul Lowongan Pekerjaan / Karir disiapkan sebagai portal resmi institusi untuk mempublikasikan kebutuhan pegawai, dosen, guru, tenaga kependidikan, staf administrasi, maupun posisi profesional lain yang dibutuhkan secara terbuka, rapi, dan mudah diakses oleh calon pelamar.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Membantu institusi menampilkan citra rekrutmen yang profesional, mempercepat distribusi informasi lowongan, mengurangi proses pendaftaran manual, serta memudahkan calon pelamar memahami posisi, persyaratan, jadwal seleksi, dan tahapan administrasi yang harus dilengkapi.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Mendukung publikasi informasi karir secara mandiri, pendaftaran pelamar online, pengelolaan berkas digital, validasi administrasi, status seleksi, dan integrasi bertahap dengan modul SDM/Kepegawaian setelah kandidat dinyatakan diterima.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Menyediakan halaman informasi lowongan, formulir pendaftaran calon pegawai, unggah dokumen pendukung, pengelompokan posisi, monitoring proses seleksi, serta komunikasi awal antara institusi dan calon pelamar melalui alur digital yang terdokumentasi.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Calon Pegawai, Calon Dosen/Guru, Tenaga Kependidikan, HRD/Kepegawaian, Pimpinan Unit, serta Tim Seleksi Penerimaan Pegawai Baru.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Portal Karir memperkuat tata kelola rekrutmen institusi agar lebih transparan, cepat, terdokumentasi, dan memberikan pengalaman profesional sejak tahap pertama calon pegawai mengenal institusi.") %>',
                    requireLogin: false
                },
                "antar_jemput": {
                    title: '<%= Common.getBahasaConfigJS("Sistem Antar Jemput Terintegrasi") %>',
                    desc: '<%= Common.getBahasaConfigJS("Modul Antar Jemput dirancang untuk mengelola layanan kendaraan operasional, penjemput pribadi, gerbang keamanan, monitor antrian, dan pengumuman otomatis ke kelas dalam satu alur digital yang aman dan terdokumentasi.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Membantu sekolah, kampus, pesantren, dan yayasan memastikan proses penjemputan siswa, guru, mahasiswa, dosen, maupun pegawai berjalan tertib mulai dari jadwal, manifest peserta, validasi kartu penjemput, pemanggilan kelas, hingga bukti serah terima.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Mendukung master data kendaraan, sopir, kenek, rute, kartu penjemput, jadwal antar jemput, daftar peserta, transaksi harian, detail panggilan, nomor antrian, log notifikasi, serta integrasi bertahap dengan asset, kepegawaian, akademik, dan perangkat soundbox/monitor kelas.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Satpam atau petugas dapat melakukan tap kartu maupun scan barcode di gerbang. Sistem memvalidasi kartu aktif, mencari peserta sesuai jadwal, membuat transaksi penjemputan, menampilkan antrian pada monitor gerbang, mengirim pesan ke soundbox kelas, mencatat status keluar kelas, dan menyelesaikan transaksi setelah serah terima dikonfirmasi.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Admin Operasional, Satpam/Petugas Gerbang, Sopir, Kenek, Guru/Wali Kelas, Bagian Kesiswaan/Kemahasiswaan, Orang Tua/Wali, Siswa, Mahasiswa, Guru, Dosen, dan Pegawai.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Sistem Antar Jemput memperkuat kontrol keselamatan, mempercepat panggilan peserta, mengurangi antrean gerbang, serta menyediakan audit trail lengkap mulai dari kedatangan penjemput, notifikasi kelas, sampai proses serah terima selesai.") %>',
                    requireLogin: false
                },
                "pengunjung_pustaka": {
                    title: '<%= Common.getBahasaConfigJS("Pengunjung Pustaka") %>',
                    desc: '<%= Common.getBahasaConfigJS("Sistem untuk mendaftarkan diri secara mandiri (self-service) bagi peserta didik atau tamu yang ingin berkunjung ke perpustakaan institusi.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Memudahkan pendataan statistik pengunjung perpustakaan secara otomatis tanpa perlu antrean pencatatan manual.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Antarmuka yang sangat responsif, mendukung pemindaian kartu anggota, dan pelaporan statistik kunjungan waktu nyata.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Mencatat daftar hadir, tujuan kunjungan, dan merekam data diri pengunjung perpustakaan dengan cepat.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Pengunjung Perpustakaan, Pustakawan, dan Mahasiswa/Siswa.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Solusi modern untuk mencatat dan melacak kunjungan ke fasilitas perpustakaan dalam rangka evaluasi layanan secara digital.") %>',
                    requireLogin: false
                },
                "buku_tamu": {
                    title: '<%= Common.getBahasaConfigJS("Buku Tamu Institusi") %>',
                    desc: '<%= Common.getBahasaConfigJS("Sistem pendataan buku tamu institusi yang telah sepenuhnya terdigitalisasi sebagai pengganti buku registrasi tamu konvensional.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Merapikan arsip kunjungan dan memfasilitasi pelacakan riwayat tamu demi mendukung protokol keamanan institusi.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Riwayat kunjungan tersimpan aman secara terpusat, dapat dicari (searchable) dengan mudah, serta meminimalisasi kehilangan data historis.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Mendata informasi personal tamu, tujuan kunjungan, pihak yang ingin ditemui, beserta waktu kedatangan/kepulangan.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Tamu Institusi, Resepsionis, dan Petugas Keamanan (Satpam).") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Memastikan setiap kunjungan tamu tercatat rapi secara elektronik guna meningkatkan kredibilitas, kerapian arsip, dan keamanan lingkungan.") %>',
                    requireLogin: false
                },
                "absen_siswa": {
                    title: '<%= Common.getBahasaConfigJS("Sistem Absen Siswa Mandiri") %>',
                    desc: '<%= Common.getBahasaConfigJS("Modul untuk mempermudah siswa melakukan absensi secara mandiri melalui anjungan absen terintegrasi.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Meningkatkan kedisiplinan dan efisiensi pencatatan kehadiran harian peserta didik.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Antarmuka cepat, terintegrasi langsung dengan sistem akademik utama dan rekapitulasi otomatis.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Merekam data kehadiran siswa secara seketika saat siswa berinteraksi dengan anjungan absensi.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Siswa, Guru Piket, dan Staf Tata Usaha.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Menciptakan budaya disiplin melalui pencatatan absensi yang cepat, mandiri, dan transparan.") %>',
                    requireLogin: false
                },
                "portal_rekanan": {
                    title: '<%= Common.getBahasaConfigJS("Portal Rekanan (e-Procurement)") %>',
                    desc: '<%= Common.getBahasaConfigJS("Modul khusus yang dirancang untuk memfasilitasi rekanan, vendor, atau penyedia barang dan jasa dalam melakukan pendaftaran secara mandiri serta melengkapi kelengkapan berkas administratif. Ke depannya, modul ini akan berevolusi menjadi sistem e-Procurement yang utuh.") %>',
                    manfaat: '<%= Common.getBahasaConfigJS("Meningkatkan transparansi pengadaan barang dan jasa serta memberikan kemudahan akses bagi pihak ketiga untuk menjalin kerja sama dengan institusi.") %>',
                    keunggulan: '<%= Common.getBahasaConfigJS("Portal mandiri (self-service) yang efisien, mengurangi proses tatap muka untuk pendaftaran awal, dan penyimpanan berkas digital yang aman.") %>',
                    fungsi: '<%= Common.getBahasaConfigJS("Menyediakan formulir pendaftaran vendor, fasilitas unggah dokumen legalitas perusahaan, serta pusat informasi pengadaan barang dan jasa.") %>',
                    sasaran: '<%= Common.getBahasaConfigJS("Vendor, Pemasok (Supplier), Rekanan Bisnis, dan Bagian Pengadaan Logistik Institusi.") %>',
                    kesimpulan: '<%= Common.getBahasaConfigJS("Mewujudkan ekosistem pengadaan institusi yang profesional, tertib administrasi, dan siap menyongsong digitalisasi e-Procurement masa depan.") %>',
                    requireLogin: false
                }
            };

            // ========================================================================================
            // TEMPLATE MODAL INFORMASI DINAMIS
            // Membuat elemen HTML Modal secara dinamis dan menyisipkannya ke dalam DOM Body
            // ========================================================================================
            const modalHtmlTemplate = `
            <div class="modal fade" id="dynamicInfoModal" tabindex="-1" aria-labelledby="dynamicInfoModalLabel" aria-hidden="true">
              <div class="modal-dialog modal-dialog-centered modal-lg">
                <div class="modal-content border-0 shadow-lg" style="border-radius: 20px; overflow: hidden;">
                  
                  <div class="modal-header bg-primary text-white border-0 p-4">
                    <h4 class="modal-title fw-bold" id="dynamicInfoModalLabel">
                        <i class="fas fa-info-circle me-2"></i><span id="modalTitle"></span>
                    </h4>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                  </div>
                  
                  <div class="modal-body p-5 text-start">
                    
                    <div class="mb-4">
                        <h6 class="fw-bold text-primary text-uppercase" style="letter-spacing: 1px;">
                            <%= Common.getBahasaConfig("Deskripsi Modul") %>
                        </h6>
                        <p class="text-secondary" style="line-height: 1.7;" id="modalDesc"></p>
                    </div>
                    
                    <div class="row g-4 mb-4">
                        <div class="col-md-6">
                            <h6 class="fw-bold text-primary text-uppercase" style="letter-spacing: 1px;">
                                <i class="fas fa-star me-2 text-warning"></i><%= Common.getBahasaConfig("Manfaat") %>
                            </h6>
                            <p class="text-secondary text-sm" style="line-height: 1.6;" id="modalManfaat"></p>
                        </div>
                        <div class="col-md-6">
                            <h6 class="fw-bold text-primary text-uppercase" style="letter-spacing: 1px;">
                                <i class="fas fa-check-circle me-2 text-success"></i><%= Common.getBahasaConfig("Keunggulan") %>
                            </h6>
                            <p class="text-secondary text-sm" style="line-height: 1.6;" id="modalKeunggulan"></p>
                        </div>
                        <div class="col-md-6">
                            <h6 class="fw-bold text-primary text-uppercase" style="letter-spacing: 1px;">
                                <i class="fas fa-cogs me-2 text-secondary"></i><%= Common.getBahasaConfig("Fungsi Utama") %>
                            </h6>
                            <p class="text-secondary text-sm" style="line-height: 1.6;" id="modalFungsi"></p>
                        </div>
                        <div class="col-md-6">
                            <h6 class="fw-bold text-primary text-uppercase" style="letter-spacing: 1px;">
                                <i class="fas fa-users me-2 text-info"></i><%= Common.getBahasaConfig("Penerima Manfaat") %>
                            </h6>
                            <p class="text-secondary text-sm" style="line-height: 1.6;" id="modalSasaran"></p>
                        </div>
                    </div>

                    <hr class="my-4" style="opacity: 0.1;">
                    
                    <div class="mb-2">
                        <h6 class="fw-bold text-primary text-uppercase" style="letter-spacing: 1px;">
                            <i class="fas fa-lightbulb me-2 text-warning"></i><%= Common.getBahasaConfig("Kesimpulan") %>
                        </h6>
                        <p class="text-secondary fw-semibold" style="line-height: 1.7; font-style: italic;" id="modalKesimpulan"></p>
                    </div>

                    <div id="loginCredentialsAlert" class="alert alert-warning border-0 shadow-sm d-none align-items-center p-4 mt-4" role="alert" style="border-radius: 12px; background: linear-gradient(to right, #fffbeb, #fef3c7);">
                        <i class="fas fa-exclamation-triangle fa-2x me-4 text-warning"></i>
                        <div>
                            <strong class="d-block text-dark mb-1 fs-5">
                                <%= Common.getBahasaConfig("Informasi Akses Demonstrasi") %>
                            </strong>
                            <span class="text-secondary mb-2 d-block">
                                <%= Common.getBahasaConfig("Jika Anda ingin memasuki simulasi antarmuka sistem, silakan gunakan kredensial berikut:") %>
                            </span>
                            <span class="badge bg-white text-dark border border-warning shadow-sm mt-1 fs-6 py-2 px-3">
                                <%= Common.getBahasaConfig("Nama Pengguna:") %> <span class="text-primary font-monospace fw-bold ms-1">demo</span>
                            </span>
                            <span class="badge bg-white text-dark border border-warning shadow-sm mt-1 fs-6 py-2 px-3 ms-2">
                                <%= Common.getBahasaConfig("Kata Sandi:") %> <span class="text-primary font-monospace fw-bold ms-1">demo123</span>
                            </span>
                        </div>
                    </div>

                  </div>
                  
                  <div class="modal-footer bg-light justify-content-between border-0 p-4">
                    <button 
                        type="button" 
                        class="btn btn-outline-secondary px-4 py-2 fw-bold" 
                        style="border-radius: 10px;" 
                        data-bs-dismiss="modal"
                    >
                        <%= Common.getBahasaConfig("Kembali") %>
                    </button>
                    <a 
                        href="#" 
                        id="modalDemoLink" 
                        class="btn btn-primary px-5 py-2 fw-bold text-uppercase shadow-sm" 
                        style="border-radius: 10px; letter-spacing: 1px;"
                    >
                        <%= Common.getBahasaConfig("Lanjutkan Demo") %> <i class="fas fa-arrow-right ms-2"></i>
                    </a>
                  </div>
                </div>
              </div>
            </div>
            `;

            // Menyisipkan Modal ke Body HTML
            document.body.insertAdjacentHTML('beforeend', modalHtmlTemplate);
            
            // Inisialisasi Modal dengan JavaScript Bootstrap
            const dynamicModal = new bootstrap.Modal(document.getElementById('dynamicInfoModal'));

            // Memasang event listener ke seluruh tombol pemicu modul (Trigger Modal)
            const alertButtons = document.querySelectorAll('.trigger-modal');
            alertButtons.forEach(function(btn) {
                btn.addEventListener('click', function(e) {
                    e.preventDefault(); 
                    
                    const moduleId = this.getAttribute('data-module-id');
                    const targetUrl = this.getAttribute('href');
                    const data = moduleDict[moduleId];

                    // Jika Modul ID terdaftar dalam Dictionary, set data ke dalam DOM Modal
                    if(data) {
                        document.getElementById('modalTitle').textContent = data.title;
                        document.getElementById('modalDesc').textContent = data.desc;
                        document.getElementById('modalManfaat').textContent = data.manfaat;
                        document.getElementById('modalKeunggulan').textContent = data.keunggulan;
                        document.getElementById('modalFungsi').textContent = data.fungsi;
                        document.getElementById('modalSasaran').textContent = data.sasaran;
                        document.getElementById('modalKesimpulan').textContent = data.kesimpulan;

                        // Tampilkan atau Sembunyikan Alert Login tergantung properti requireLogin
                        const loginAlert = document.getElementById('loginCredentialsAlert');
                        if(data.requireLogin === true) {
                            loginAlert.classList.remove('d-none');
                            loginAlert.classList.add('d-flex');
                        } else {
                            loginAlert.classList.remove('d-flex');
                            loginAlert.classList.add('d-none');
                        }

                        // Mengelola Link URL Demo (Jika #, maka sistem belum siap. Jika URL nyata, buka di tab baru)
                        const demoLink = document.getElementById('modalDemoLink');
                        if (targetUrl === "#") {
                            demoLink.setAttribute('href', '#');
                            demoLink.removeAttribute('target');
                            demoLink.setAttribute('onclick', `tampilkanPesanGagalFormal('akses demo fasilitas ini', '<%= Common.getBahasaConfigJS("Fasilitas operasional ini masih dalam tahap penyempurnaan pengembangan sehingga belum dapat diakses.") %>', ['Silakan coba kembali pada kesempatan berikutnya setelah pengembangan fasilitas ini rampung.']); return false;`);
                        } else {
                            demoLink.setAttribute('href', targetUrl);
                            demoLink.setAttribute('target', '_blank');
                            demoLink.removeAttribute('onclick');
                        }

                        // Menampilkan Modal Popup Info Modul
                        dynamicModal.show();
                    } else {
                        // Jika Modul ID tidak terdaftar, tampilkan peringatan Default
                        tampilkanPesanGagalFormal("penampilan informasi rincian layanan", '<%= Common.getBahasaConfigJS("Informasi rincian untuk layanan ini tidak ditemukan di dalam sistem.") %>', ["Coba pilih layanan/modul lain, atau muat ulang halaman ini.", "Bila layanan ini seharusnya tersedia, segera laporkan kepada Administrator Sistem."]);
                    }
                });
            });
        });

        // ============================================================================================
        // FUNGSI ZOOM GAMBAR HARDWARE (ANJUNGAN & POS)
        // Fungsi ini akan dipanggil saat pengguna mengklik gambar Anjungan Kiosk atau POS
        // ============================================================================================
        window.showImageModal = function(imageSrc) {
            document.getElementById('modalExpandedImage').src = imageSrc;
            var imageModal = new bootstrap.Modal(document.getElementById('imageModal'));
            imageModal.show();
        };

        // ============================================================================================
        // SMART NAVIGATION: highlight menu aktif + auto-close mobile menu saat link diklik
        // ============================================================================================
        (function() {
            var navLinks = document.querySelectorAll('.custom-nav-link[href^="#"], #mobileNav .nav-link[href^="#"]');
            if (!navLinks || navLinks.length === 0) {
                return;
            }

            for (var i = 0; i < navLinks.length; i++) {
                navLinks[i].addEventListener('click', function() {
                    var mobileNav = document.getElementById('mobileNav');
                    if (mobileNav && mobileNav.classList.contains('show') && window.bootstrap && bootstrap.Collapse) {
                        var bsCollapse = bootstrap.Collapse.getInstance(mobileNav) || new bootstrap.Collapse(mobileNav, { toggle: false });
                        bsCollapse.hide();
                    }
                });
            }

            if ('IntersectionObserver' in window) {
                var sections = [];
                for (var j = 0; j < navLinks.length; j++) {
                    var href = navLinks[j].getAttribute('href');
                    if (href && href.length > 1) {
                        var section = document.querySelector(href);
                        if (section && sections.indexOf(section) === -1) {
                            sections.push(section);
                        }
                    }
                }
                var observer = new IntersectionObserver(function(entries) {
                    entries.forEach(function(entry) {
                        if (entry.isIntersecting) {
                            var id = '#' + entry.target.getAttribute('id');
                            for (var k = 0; k < navLinks.length; k++) {
                                if (navLinks[k].classList) {
                                    if (navLinks[k].getAttribute('href') === id) {
                                        navLinks[k].classList.add('active');
                                    } else {
                                        navLinks[k].classList.remove('active');
                                    }
                                }
                            }
                        }
                    });
                }, { rootMargin: '-35% 0px -55% 0px', threshold: 0.01 });

                for (var s = 0; s < sections.length; s++) {
                    observer.observe(sections[s]);
                }
            }
        })();

    </script>
    <jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp" />
</body>
</html>