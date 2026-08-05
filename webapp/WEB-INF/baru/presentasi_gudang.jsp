<%@page import="ais.common.Common"%>
<%@ page import="ais.database.model.sekolah.Sekolah" %>
<%@ page import="java.util.Calendar" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
// =================================================================================
// TANGKAP PARAMETER URL (WHITE LABEL SUPPORT)
// =================================================================================
String paramJudul = request.getParameter("judul");
String paramLogo = request.getParameter("logo");
String paramClient = request.getParameter("client");
String paramPresentBy = request.getParameter("present_by");
String paramTelp = request.getParameter("telp_present");

String queryString = request.getQueryString();
String targetDemoUrl = "https://ecampus.id/apps" + (queryString != null && !queryString.isEmpty() ? "?" + queryString : "");
String appendParams = (queryString != null && !queryString.isEmpty()) ? "?" + queryString : "";
String appendParamsGudang = "?modul=gudang" + (queryString != null && !queryString.isEmpty() ? "&" + queryString : "");

// Deklarasi Variabel Data Institusi (Dengan Override dari Parameter)
ais.database.model.sekolah.Sekolah sekolah = ais.action.master.sekolah.util.SekolahUtil.getSekolah(request);
String judul = (paramJudul != null && !paramJudul.trim().isEmpty()) ? paramJudul : ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama();
String logo_PerguruanTinggi = (paramLogo != null && !paramLogo.trim().isEmpty()) ? Common.ROOT + "/img/" + paramLogo : ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= Common.getBahasaConfig("Presentasi Modul Gudang, POS & Investor (Sistem Terpadu)") %> | <%=judul%></title>

    <link rel="icon" href="<%=logo_PerguruanTinggi%>" type="image/x-icon">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">

    <style>
        :root {
            --primary-color: #0284c7;
            --success-color: #16a34a;
            --warning-color: #d97706;
            --danger-color: #dc2626;
            --light-bg: #f8fafc;
            --card-bg: rgba(255, 255, 255, 0.93);
            --card-inner-bg: #ffffff;
            --text-dark: #0f172a;
            --text-gray-bright: #475569;
        }

        body, html { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; background-color: var(--light-bg); font-family: 'Segoe UI', system-ui, -apple-system, sans-serif; color: var(--text-dark); }
        .presentation-container { position: relative; width: 100vw; height: 100vh; background: linear-gradient(135deg, rgba(255, 255, 255, 0.85) 0%, rgba(241, 245, 249, 0.95) 100%); display: flex; justify-content: center; align-items: center; }
        .slide { position: absolute; top: 0; left: 0; width: 100%; height: 100%; opacity: 0; visibility: hidden; transition: opacity 0.5s ease-in-out, transform 0.5s ease-in-out; transform: scale(0.98); display: flex; flex-direction: column; justify-content: center; align-items: center; padding: 2rem 5%; }
        .slide.active { opacity: 1; visibility: visible; transform: scale(1); z-index: 10; }
        .slide-content { background: var(--card-bg); backdrop-filter: blur(15px); border: 1px solid rgba(2, 132, 199, 0.15); border-radius: 24px; padding: 3rem; width: 100%; max-width: 1350px; box-shadow: 0 20px 50px rgba(0,0,0,0.15); max-height: 88vh; overflow-y: auto; }
        .slide-content::-webkit-scrollbar { width: 8px; }
        .slide-content::-webkit-scrollbar-track { background: rgba(0,0,0,0.05); border-radius: 10px; }
        .slide-content::-webkit-scrollbar-thumb { background: var(--primary-color); border-radius: 10px; }

        h1, h2, h3, h4, h5 { font-weight: 800; color: var(--text-dark); }
        .text-highlight { color: var(--primary-color) !important; }
        .text-warning-custom { color: var(--warning-color) !important; }
        .text-success-custom { color: var(--success-color) !important; }
        .text-muted-custom { color: var(--text-gray-bright) !important; opacity: 1 !important; }

        .feature-list li { font-size: 1.1rem; margin-bottom: 0.8rem; line-height: 1.5; color: var(--text-gray-bright); }
        .feature-icon { width: 65px; height: 65px; background: rgba(2, 132, 199, 0.1); color: var(--primary-color); border-radius: 16px; display: inline-flex; align-items: center; justify-content: center; font-size: 2rem; margin-bottom: 1.2rem; border: 1px solid rgba(2, 132, 199, 0.2); }

        .module-card { background: var(--card-inner-bg); border-left: 5px solid var(--primary-color); padding: 1.5rem; border-radius: 12px; height: 100%; transition: all 0.3s ease; box-shadow: 0 4px 15px rgba(0,0,0,0.05); }
        .module-card:hover { transform: translateX(10px); background: rgba(240, 249, 255, 1); border-left-color: var(--warning-color); box-shadow: 0 8px 25px rgba(0,0,0,0.15); }
        .module-title { font-size: 1.2rem; color: var(--text-dark); margin-bottom: 0.5rem; font-weight: 700; display: flex; align-items: center; }
        .module-title i { width: 30px; color: var(--primary-color); }
        .module-desc { font-size: 0.95rem; color: var(--text-gray-bright); line-height: 1.5; margin: 0; text-align: justify; }

        .controls { position: absolute; bottom: 1.5rem; left: 50%; transform: translateX(-50%); display: flex; gap: 1.5rem; align-items: center; z-index: 100; background: rgba(255, 255, 255, 0.95); padding: 0.8rem 2rem; border-radius: 50px; border: 1px solid rgba(0,0,0,0.1); box-shadow: 0 10px 25px rgba(0,0,0,0.1); }
        .btn-nav { background: transparent; border: none; color: var(--text-dark); font-size: 1.5rem; cursor: pointer; transition: all 0.2s; }
        .btn-nav:hover { color: var(--primary-color); transform: scale(1.2); }
        .btn-nav:disabled { color: rgba(0,0,0,0.2); cursor: not-allowed; transform: none; }

        .slide-counter { font-size: 1.1rem; font-weight: 700; letter-spacing: 2px; color: var(--primary-color); }
        .logo-header { position: absolute; top: 2rem; left: 3rem; height: 70px; z-index: 50; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2)); }

        .pricing-box { background: linear-gradient(145deg, #ffffff, #f8fafc); border: 2px solid var(--primary-color); border-radius: 20px; padding: 2.5rem; height: 100%; position: relative; overflow: hidden; box-shadow: 0 20px 40px rgba(0,0,0,0.08); }
        .pricing-box::before { content: ""; position: absolute; top: 0; left: 0; width: 100%; height: 6px; background: var(--primary-color); }
        .price-badge { background: rgba(2, 132, 199, 0.1); color: var(--primary-color); padding: 8px 20px; border-radius: 30px; font-weight: 800; display: inline-block; margin-bottom: 1.5rem; border: 1px solid rgba(2, 132, 199, 0.2); }

        .hover-zoom { transition: transform 0.3s ease, box-shadow 0.3s ease; }
        .hover-zoom:hover { transform: scale(1.03); box-shadow: 0 12px 25px rgba(0,0,0,0.2) !important; }
        .info-card { background: var(--card-inner-bg); border: 1px solid rgba(0,0,0,0.05); border-radius: 16px; padding: 2rem; transition: transform 0.3s, box-shadow 0.3s; box-shadow: 0 10px 20px rgba(0,0,0,0.05); }
        .info-card:hover { transform: translateY(-5px); background: #ffffff; border-color: var(--primary-color); box-shadow: 0 15px 30px rgba(0,0,0,0.1); }

        .doc-btn {
            transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
            border-width: 2px;
            text-decoration: none;
            position: relative;
            overflow: hidden;
            background: #ffffff;
        }
        .doc-btn:hover {
            transform: translateY(-5px);
            box-shadow: 0 15px 25px rgba(0,0,0,0.15) !important;
        }
        .doc-btn::after {
            content: '';
            position: absolute;
            top: 0;
            left: -100%;
            width: 50%;
            height: 100%;
            background: linear-gradient(to right, rgba(255,255,255,0) 0%, rgba(255,255,255,0.8) 50%, rgba(255,255,255,0) 100%);
            transform: skewX(-25deg);
            transition: all 0.7s ease;
        }
        .doc-btn:hover::after {
            left: 200%;
        }

        .pulse-button { animation: pulse-primary 2s infinite; transition: transform 0.3s; }
        .pulse-button:hover { transform: scale(1.03); animation: none; box-shadow: 0 15px 30px rgba(2, 132, 199, 0.4) !important;}
        @keyframes pulse-primary {
            0% { box-shadow: 0 0 0 0 rgba(2, 132, 199, 0.7); }
            70% { box-shadow: 0 0 0 15px rgba(2, 132, 199, 0); }
            100% { box-shadow: 0 0 0 0 rgba(2, 132, 199, 0); }
        }

        :root {
            --accent-blue: #0ea5e9;
            --accent-indigo: #4f46e5;
            --accent-violet: #7c3aed;
            --soft-blue: #e0f2fe;
            --soft-gold: #fef3c7;
            --border-soft: rgba(15, 23, 42, 0.10);
            --shadow-soft: 0 24px 70px rgba(15, 23, 42, 0.16);
        }

        .presentation-container::before,
        .presentation-container::after {
            content: "";
            position: absolute;
            width: 520px;
            height: 520px;
            border-radius: 999px;
            filter: blur(38px);
            opacity: 0.35;
            pointer-events: none;
            z-index: 0;
        }
        .presentation-container::before {
            top: -180px;
            right: -160px;
            background: radial-gradient(circle, rgba(14,165,233,0.80), rgba(79,70,229,0.10) 62%, transparent 70%);
        }
        .presentation-container::after {
            bottom: -220px;
            left: -170px;
            background: radial-gradient(circle, rgba(217,119,6,0.55), rgba(22,163,74,0.10) 58%, transparent 72%);
        }
        .slide-content {
            position: relative;
            z-index: 2;
            border-radius: 32px;
            border: 1px solid rgba(255,255,255,0.72);
            box-shadow: var(--shadow-soft);
        }
        .slide-content::before {
            content: "";
            position: absolute;
            inset: 0;
            border-radius: 32px;
            padding: 1px;
            background: linear-gradient(135deg, rgba(14,165,233,0.32), rgba(217,119,6,0.18), rgba(124,58,237,0.18));
            -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
            -webkit-mask-composite: xor;
            mask-composite: exclude;
            pointer-events: none;
        }
        .slide-content::after {
            content: "Gudang, POS & Investor - Enterprise Education";
            position: absolute;
            right: 28px;
            bottom: 18px;
            font-size: 0.72rem;
            font-weight: 800;
            letter-spacing: 0.14em;
            text-transform: uppercase;
            color: rgba(15,23,42,0.18);
            pointer-events: none;
        }
        .deck-progress {
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 6px;
            background: rgba(255,255,255,0.6);
            z-index: 200;
            overflow: hidden;
        }
        #deckProgressBar {
            width: 0%;
            height: 100%;
            background: linear-gradient(90deg, var(--primary-color), var(--accent-indigo), var(--warning-color));
            transition: width .35s ease;
            box-shadow: 0 0 18px rgba(14,165,233,0.5);
        }
        .presentation-brand-chip {
            position: fixed;
            top: 1.35rem;
            right: 2rem;
            z-index: 90;
            background: rgba(255,255,255,0.82);
            backdrop-filter: blur(16px);
            border: 1px solid rgba(255,255,255,0.85);
            box-shadow: 0 12px 35px rgba(15,23,42,0.12);
            border-radius: 999px;
            padding: .68rem 1rem;
            font-size: .82rem;
            font-weight: 900;
            color: var(--text-dark);
            letter-spacing: .09em;
            text-transform: uppercase;
        }
        .presentation-brand-chip i { color: var(--primary-color); }
        .hero-gradient-title {
            background: linear-gradient(105deg, var(--primary-color), var(--accent-indigo), var(--warning-color));
            -webkit-background-clip: text;
            background-clip: text;
            -webkit-text-fill-color: transparent;
            letter-spacing: -0.04em;
        }
        .section-eyebrow {
            display: inline-flex;
            align-items: center;
            gap: .55rem;
            padding: .55rem 1rem;
            border-radius: 999px;
            color: #075985;
            background: rgba(224,242,254,0.85);
            border: 1px solid rgba(14,165,233,0.22);
            font-size: .82rem;
            font-weight: 900;
            letter-spacing: .11em;
            text-transform: uppercase;
            box-shadow: 0 8px 24px rgba(14,165,233,0.10);
        }
        .executive-lead {
            font-size: 1.34rem;
            line-height: 1.62;
            color: #334155;
            max-width: 1050px;
            margin: 0 auto;
        }
        .impact-card {
            height: 100%;
            background: linear-gradient(180deg, #ffffff, #f8fbff);
            border: 1px solid var(--border-soft);
            border-radius: 24px;
            padding: 1.6rem;
            box-shadow: 0 16px 36px rgba(15,23,42,0.08);
            transition: all .3s ease;
        }
        .impact-card:hover { transform: translateY(-8px); box-shadow: 0 24px 50px rgba(15,23,42,0.13); }
        .impact-number {
            display: inline-flex;
            width: 48px;
            height: 48px;
            border-radius: 16px;
            align-items: center;
            justify-content: center;
            background: linear-gradient(135deg, var(--primary-color), var(--accent-indigo));
            color: #fff;
            font-weight: 900;
            box-shadow: 0 14px 28px rgba(14,165,233,0.22);
            margin-bottom: .9rem;
        }
        .metric-chip {
            background: rgba(255,255,255,0.92);
            border: 1px solid rgba(14,165,233,0.18);
            border-radius: 18px;
            padding: 1rem 1.25rem;
            box-shadow: 0 14px 30px rgba(15,23,42,0.07);
        }
        .metric-chip strong { color: var(--primary-color); font-size: 1.8rem; line-height: 1; display: block; }
        .before-after {
            position: relative;
            background: #ffffff;
            border-radius: 28px;
            padding: 1.5rem;
            border: 1px solid var(--border-soft);
            box-shadow: 0 18px 42px rgba(15,23,42,0.08);
        }
        .before-after::before {
            content: "";
            position: absolute;
            top: 50%;
            left: 50%;
            width: 58px;
            height: 58px;
            transform: translate(-50%, -50%);
            background: #ffffff;
            border: 1px solid rgba(14,165,233,0.18);
            border-radius: 999px;
            box-shadow: 0 10px 30px rgba(15,23,42,0.12);
            z-index: 1;
        }
        .before-after::after {
            content: "\f061";
            font-family: "Font Awesome 6 Free";
            font-weight: 900;
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            color: var(--primary-color);
            font-size: 1.4rem;
            z-index: 2;
        }
        .value-strip {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 1rem;
            margin-top: 1.6rem;
        }
        .value-strip > div {
            background: rgba(255,255,255,0.9);
            border: 1px solid var(--border-soft);
            border-radius: 20px;
            padding: 1rem;
            box-shadow: 0 10px 25px rgba(15,23,42,0.06);
        }
        .timeline-modern {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 1rem;
            position: relative;
        }
        .timeline-step {
            position: relative;
            background: linear-gradient(180deg, #ffffff, #f8fafc);
            border: 1px solid var(--border-soft);
            border-radius: 24px;
            padding: 1.4rem;
            min-height: 290px;
            box-shadow: 0 16px 34px rgba(15,23,42,0.08);
        }
        .timeline-step .day {
            color: var(--warning-color);
            font-weight: 900;
            letter-spacing: .08em;
            text-transform: uppercase;
            font-size: .78rem;
        }
        .timeline-step .icon {
            width: 58px;
            height: 58px;
            border-radius: 20px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            background: rgba(14,165,233,0.10);
            color: var(--primary-color);
            font-size: 1.55rem;
            margin: .75rem 0 1rem;
            border: 1px solid rgba(14,165,233,0.18);
        }
        .decision-card {
            background: linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
            border-radius: 24px;
            border: 1px solid var(--border-soft);
            padding: 1.5rem;
            height: 100%;
            box-shadow: 0 16px 36px rgba(15,23,42,0.08);
        }
        .quote-panel {
            background: linear-gradient(135deg, rgba(2,132,199,0.08), rgba(217,119,6,0.09));
            border: 1px solid rgba(2,132,199,0.15);
            border-radius: 28px;
            padding: 1.6rem;
        }
        .tracking-widest { letter-spacing: .14em; }
        .kbd-hint {
            position: fixed;
            left: 2rem;
            bottom: 1.4rem;
            z-index: 90;
            font-size: .72rem;
            color: rgba(15,23,42,0.58);
            background: rgba(255,255,255,0.78);
            backdrop-filter: blur(12px);
            border: 1px solid rgba(255,255,255,0.8);
            border-radius: 999px;
            padding: .55rem .9rem;
            box-shadow: 0 10px 30px rgba(15,23,42,0.08);
        }
        .kbd-hint kbd { background: #0f172a; color: #fff; border-radius: 6px; padding: .12rem .36rem; }
        .btn-nav:focus-visible, .doc-btn:focus-visible, .btn:focus-visible {
            outline: 4px solid rgba(14,165,233,0.24) !important;
            outline-offset: 3px;
        }
        @media (max-width: 991px) {
            .slide { padding: 1.25rem 3.5%; }
            .slide-content { padding: 2rem; max-height: 86vh; }
            .value-strip, .timeline-modern { grid-template-columns: 1fr 1fr; }
            .logo-header { height: 50px; left: 1rem; top: 1rem; }
            .presentation-brand-chip { display: none; }
            .kbd-hint { display: none; }
            .controls { bottom: .85rem; padding: .55rem 1.1rem; gap: .9rem; }
        }
        @media (max-width: 640px) {
            body, html { overflow: hidden; }
            .value-strip, .timeline-modern { grid-template-columns: 1fr; }
            .before-after::before, .before-after::after { display: none; }
            .slide-content { border-radius: 22px; padding: 1.35rem; }
            .feature-list li { font-size: .98rem; }
        }
        @media print {
            body, html { overflow: visible; height: auto; background: #fff; }
            .presentation-container { display: block; width: 100%; height: auto; background: #fff; }
            .slide { position: relative; opacity: 1; visibility: visible; transform: none; page-break-after: always; min-height: 100vh; }
            .controls, .deck-progress, .kbd-hint, .presentation-brand-chip { display: none !important; }
            .slide-content { box-shadow: none; max-height: none; overflow: visible; border: 1px solid #e2e8f0; }
        }

    </style>
</head>
<body>
<jsp:include page="/WEB-INF/baru/include/pemilih_bahasa.jsp" />

    <div class="deck-progress"><div id="deckProgressBar"></div></div>
    <div class="presentation-brand-chip"><i class="fas fa-warehouse me-2"></i> Gudang & POS</div>
    <div class="kbd-hint"><kbd>←</kbd> <kbd>→</kbd> Navigasi &nbsp; <kbd>F</kbd> Fullscreen</div>

    <img src="<%=logo_PerguruanTinggi%>" class="logo-header" alt="Logo">

    <div class="presentation-container" id="presentationArea">

        <div class="slide active">
            <div class="text-center slide-content" style="overflow:hidden;">
                <span class="section-eyebrow mb-4"><i class="fas fa-bolt"></i> <%= Common.getBahasaConfig("Executive Presentation Deck") %></span>
                <i class="fas fa-warehouse fa-5x mb-4 text-warning-custom d-block"></i>
                <h1 class="display-3 mb-3 hero-gradient-title"><%= Common.getBahasaConfig("Transformasi Digital Gudang, POS & Bisnis Ritel Terpadu") %></h1>
                <h2 class="fw-bold text-warning-custom mb-4 display-6"><%= Common.getBahasaConfig("Sistem Gudang, Outlet, POS, Ekspedisi, Investor & Akuntansi") %></h2>
                <hr class="w-25 mx-auto border-info opacity-50 mb-4 border-2">
                <p class="fs-4 text-muted-custom mb-3"><%= Common.getBahasaConfig("Solusi Terpadu untuk Kasir, Gudang, HPP & Akuntansi Otomatis, Pengadaan, dan Dashboard Analitik") %></p>
                <p class="executive-lead mb-4"><%= Common.getBahasaConfig("Bagian dari ekosistem Enterprise Education — terintegrasi langsung bila Universitas, Sekolah, atau Pondok Pesantren Anda memiliki kantin, koperasi, toko kampus, gudang, atau unit usaha yang didanai investor, atau dapat diimplementasikan berdiri sendiri (standalone) untuk bisnis ritel/distribusi tanpa induk lembaga pendidikan.") %></p>
                <div class="d-flex flex-wrap justify-content-center gap-3 mt-4">
                    <div class="metric-chip"><strong><i class="fas fa-network-wired"></i></strong><span class="small fw-bold text-muted-custom"><%= Common.getBahasaConfig("Satu Data Terpadu") %></span></div>
                    <div class="metric-chip"><strong><i class="fas fa-boxes-stacked"></i></strong><span class="small fw-bold text-muted-custom"><%= Common.getBahasaConfig("Stok & Kas Selalu Akurat") %></span></div>
                    <div class="metric-chip"><strong><i class="fas fa-shield-alt"></i></strong><span class="small fw-bold text-muted-custom"><%= Common.getBahasaConfig("Kontrol, Audit, dan Transparansi") %></span></div>
                </div>

                <p class="mt-5 text-primary fw-bold bg-primary bg-opacity-10 tracking-widest text-uppercase d-inline-block px-4 py-2 rounded-pill shadow-sm border border-primary border-opacity-25"><%=judul%></p>

                <% if(paramClient != null && !paramClient.trim().isEmpty()) { %>
                    <div class="mt-4 fs-4 text-dark fw-bold"><%= Common.getBahasaConfig("Dipersembahkan eksklusif untuk:") %> <br><span class="text-warning-custom fs-3"><%=paramClient%></span></div>
                <% } %>

                <% if(paramPresentBy != null && !paramPresentBy.trim().isEmpty()) { %>
                    <div class="mt-3 fs-5 text-muted-custom"><%= Common.getBahasaConfig("Dipresentasikan oleh:") %> <strong class="text-dark"><%=paramPresentBy%></strong></div>
                <% } %>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-compass"></i> <%= Common.getBahasaConfig("Ringkasan Eksekutif") %></span>
                    <h2 class="mt-4 mb-3 hero-gradient-title"><%= Common.getBahasaConfig("Satu Platform untuk Menggerakkan Seluruh Rantai Nilai Ritel & Distribusi") %></h2>
                    <p class="executive-lead"><%= Common.getBahasaConfig("Sistem ini bukan sekadar aplikasi kasir. Sistem ini menjadi fondasi tata kelola gudang dan unit usaha: menyatukan data stok, transaksi, dan keuangan, mengurangi risiko kesalahan manual, serta menyediakan informasi yang siap dipakai pimpinan dan investor untuk mengambil keputusan.") %></p>
                </div>
                <div class="row g-4 mt-3">
                    <div class="col-md-4"><div class="impact-card"><div class="impact-number">01</div><h4><%= Common.getBahasaConfig("Integrasi Operasional") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Kasir, gudang, pengadaan, dan akuntansi berjalan dalam satu alur kerja yang saling terhubung.") %></p></div></div>
                    <div class="col-md-4"><div class="impact-card"><div class="impact-number">02</div><h4><%= Common.getBahasaConfig("Kasir Tanpa Gangguan") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("POS tetap dapat melayani transaksi walau internet terputus, lalu tersinkron otomatis begitu koneksi kembali.") %></p></div></div>
                    <div class="col-md-4"><div class="impact-card"><div class="impact-number">03</div><h4><%= Common.getBahasaConfig("Kontrol Manajemen") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Pimpinan dan investor memperoleh dashboard, audit trail, dan HPP otomatis untuk mengawal mutu dan keuangan usaha.") %></p></div></div>
                </div>
                <div class="quote-panel mt-4 text-center">
                    <h5 class="mb-1 text-dark"><i class="fas fa-quote-left text-warning-custom me-2"></i><%= Common.getBahasaConfig("Tujuan akhirnya sederhana: stok tercatat akurat, transaksi kasir tidak pernah berhenti, dan setiap angka keuangan bisa dipertanggungjawabkan sampai ke dokumen sumbernya.") %></h5>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-3"><span class="section-eyebrow"><i class="fas fa-triangle-exclamation"></i> <%= Common.getBahasaConfig("Masalah yang Perlu Diselesaikan") %></span></div>
                <h2 class="mb-5 text-center"><i class="fas fa-exclamation-circle me-3 text-warning-custom"></i><%= Common.getBahasaConfig("Tantangan Gudang, Kasir & Unit Usaha Saat Ini") %></h2>
                <div class="row g-4">
                    <div class="col-md-6">
                        <div class="module-card" style="border-left-color: var(--danger-color);">
                            <div class="module-title"><i class="fas fa-boxes text-danger"></i> <%= Common.getBahasaConfig("Stok Tidak Sinkron Antar Cabang") %></div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Pencatatan manual per lokasi membuat stok pusat dan cabang mudah selisih tanpa disadari.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card" style="border-left-color: var(--danger-color);">
                            <div class="module-title"><i class="fas fa-plug-circle-xmark text-danger"></i> <%= Common.getBahasaConfig("Kasir Lumpuh Saat Internet Putus") %></div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Aplikasi POS berbasis internet murni berhenti total ketika koneksi terganggu, transaksi tertunda.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card" style="border-left-color: var(--danger-color);">
                            <div class="module-title"><i class="fas fa-calculator text-danger"></i> <%= Common.getBahasaConfig("HPP Dihitung Manual, Sering Meleset") %></div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Harga pokok menu/produk racikan ditaksir kasar, bukan dihitung dari komposisi bahan baku sesungguhnya.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card" style="border-left-color: var(--danger-color);">
                            <div class="module-title"><i class="fas fa-people-arrows text-danger"></i> <%= Common.getBahasaConfig("Dua Kasir Menjual Stok yang Sama") %></div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Tanpa validasi stok yang ketat, dua transaksi bersamaan bisa menjual barang yang sama hingga stok minus.") %></p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-route"></i> <%= Common.getBahasaConfig("Transformasi yang Dihasilkan") %></span>
                    <h2 class="mt-4 mb-3 text-highlight"><%= Common.getBahasaConfig("Dari Pencatatan Manual Menjadi Ekosistem Ritel & Distribusi Terpadu") %></h2>
                    <p class="executive-lead"><%= Common.getBahasaConfig("Dengan pendekatan terpadu, kasir, gudang, dan akuntansi tidak lagi bekerja sendiri-sendiri. Data yang sama dapat dipakai oleh banyak fungsi sehingga proses lebih ringkas dan validasi menjadi lebih kuat.") %></p>
                </div>
                <div class="before-after mt-4">
                    <div class="row g-4 align-items-stretch">
                        <div class="col-md-6 pe-md-5">
                            <h4 class="text-danger mb-3"><i class="fas fa-times-circle me-2"></i><%= Common.getBahasaConfig("Sebelum Terintegrasi") %></h4>
                            <ul class="feature-list list-unstyled mb-0">
                                <li><i class="fas fa-minus-circle text-danger me-2"></i><%= Common.getBahasaConfig("Stok per cabang dicatat manual dan baru direkap di akhir periode.") %></li>
                                <li><i class="fas fa-minus-circle text-danger me-2"></i><%= Common.getBahasaConfig("Kasir berhenti total begitu koneksi internet terputus.") %></li>
                                <li><i class="fas fa-minus-circle text-danger me-2"></i><%= Common.getBahasaConfig("HPP produk racikan ditaksir kasar, bukan dihitung otomatis.") %></li>
                                <li><i class="fas fa-minus-circle text-danger me-2"></i><%= Common.getBahasaConfig("Jurnal akuntansi disusun manual, terpisah dari transaksi kasir.") %></li>
                            </ul>
                        </div>
                        <div class="col-md-6 ps-md-5">
                            <h4 class="text-success-custom mb-3"><i class="fas fa-check-circle me-2"></i><%= Common.getBahasaConfig("Sesudah Menggunakan Sistem Terpadu") %></h4>
                            <ul class="feature-list list-unstyled mb-0">
                                <li><i class="fas fa-check-circle text-success me-2"></i><%= Common.getBahasaConfig("Hierarki gudang pusat-cabang dengan ledger pergerakan stok yang auditable.") %></li>
                                <li><i class="fas fa-check-circle text-success me-2"></i><%= Common.getBahasaConfig("POS offline-first: kasir tetap melayani, transaksi tersinkron otomatis begitu online.") %></li>
                                <li><i class="fas fa-check-circle text-success me-2"></i><%= Common.getBahasaConfig("HPP resep dihitung otomatis dari rollup biaya bahan baku, bukan angka tebakan.") %></li>
                                <li><i class="fas fa-check-circle text-success me-2"></i><%= Common.getBahasaConfig("Jurnal akuntansi terposting otomatis dari setiap transaksi, tertelusur ke dokumen sumber.") %></li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content text-center">
                <h2 class="mb-4 text-highlight"><i class="fas fa-network-wired me-3"></i><%= Common.getBahasaConfig("Solusi: Sistem Gudang, POS & Bisnis Terpadu") %></h2>
                <p class="fs-4 mb-5 text-muted-custom"><%= Common.getBahasaConfig("Terintegrasi dengan Ekosistem Pendidikan, atau Berdiri Sendiri") %></p>
                <div class="row justify-content-center mt-4">
                    <div class="col-lg-10">
                        <div class="d-flex justify-content-around align-items-center flex-wrap gap-4">
                            <div class="p-4 module-card text-center" style="flex: 1; min-width: 280px;">
                                <i class="fas fa-graduation-cap fa-4x text-primary mb-4"></i>
                                <h3 class="text-dark">Terintegrasi Pendidikan</h3>
                                <p class="text-muted-custom mb-0 mt-2">Kantin, koperasi, toko kampus, gudang, atau unit usaha milik Universitas (eCampus), Sekolah (eSchool), atau Pondok Pesantren (ePesantren) — langsung memakai data siswa/mahasiswa/pegawai yang sudah ada</p>
                            </div>
                            <i class="fas fa-link fa-2x text-muted-custom opacity-50"></i>
                            <div class="p-4 module-card text-center" style="flex: 1; min-width: 280px; border-left-color: var(--success-color);">
                                <i class="fas fa-store fa-4x text-success mb-4"></i>
                                <h3 class="text-dark">Berdiri Sendiri</h3>
                                <p class="text-muted-custom mb-0 mt-2">Ritel, gudang, distribusi, atau unit usaha berbasis investor tanpa induk lembaga pendidikan</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-star"></i> <%= Common.getBahasaConfig("Nilai Strategis untuk Gudang, Kasir & Unit Usaha") %></span>
                    <h2 class="mt-4 mb-3 text-highlight"><%= Common.getBahasaConfig("Manfaat yang Langsung Terasa oleh Pimpinan, Kasir, dan Investor") %></h2>
                </div>
                <div class="value-strip">
                    <div><h5><i class="fas fa-stopwatch text-primary me-2"></i><%= Common.getBahasaConfig("Transaksi Lebih Cepat") %></h5><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Kasir melayani lebih cepat karena stok, harga, dan diskon tervalidasi otomatis oleh sistem.") %></p></div>
                    <div><h5><i class="fas fa-boxes-packing text-success me-2"></i><%= Common.getBahasaConfig("Stok Selalu Akurat") %></h5><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Validasi server-side dengan penguncian baris mencegah dua kasir menjual stok yang sama.") %></p></div>
                    <div><h5><i class="fas fa-database text-warning me-2"></i><%= Common.getBahasaConfig("Data Lebih Tertib") %></h5><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Setiap pergerakan stok dan jurnal tersimpan rapi, tertelusur kembali ke dokumen sumbernya.") %></p></div>
                    <div><h5><i class="fas fa-chart-pie text-info me-2"></i><%= Common.getBahasaConfig("Laporan Siap Pakai") %></h5><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Rekap operasional, HPP, dan draft jurnal tersedia real-time untuk rapat pimpinan dan investor.") %></p></div>
                </div>
                <div class="row g-4 mt-4">
                    <div class="col-md-6"><div class="impact-card"><h4><i class="fas fa-shop text-primary me-2"></i><%= Common.getBahasaConfig("Citra Usaha Lebih Profesional") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("POS modern, pembayaran non-tunai/QRIS, dan dashboard pimpinan memberi kesan usaha yang tertata dan siap bertumbuh.") %></p></div></div>
                    <div class="col-md-6"><div class="impact-card"><h4><i class="fas fa-seedling text-success-custom me-2"></i><%= Common.getBahasaConfig("Siap Bertumbuh Jangka Panjang") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Modul dapat diterapkan bertahap, mulai dari kasir dan gudang satu outlet hingga jaringan multi-cabang dengan investor.") %></p></div></div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-5 text-center text-highlight"><i class="fas fa-network-wired me-3"></i><%= Common.getBahasaConfig("Fleksibilitas Infrastruktur & Akses") %></h2>
                <div class="row g-5 mt-2">
                    <div class="col-md-6 border-end border-secondary border-opacity-25 pe-md-5">
                        <h3 class="mb-4 text-dark"><i class="fas fa-cloud text-highlight me-3"></i><%= Common.getBahasaConfig("Solusi Dual Model") %></h3>
                        <p class="fs-5 text-muted-custom"><%= Common.getBahasaConfig("Infrastruktur adaptif sesuai kebijakan operasional dan keamanan data usaha Anda:") %></p>
                        <ul class="feature-list mt-4 list-unstyled">
                            <li><i class="fas fa-check-circle text-success-custom me-3 fs-5"></i><strong>Cloud System:</strong> <%= Common.getBahasaConfig("Berbasis internet murni. Tidak butuh beli server, langsung pakai, bisa diakses dari mana saja.") %></li>
                            <li><i class="fas fa-check-circle text-success-custom me-3 fs-5"></i><strong>On-Premise:</strong> <%= Common.getBahasaConfig("Sistem dipasang di server lokal outlet/gudang untuk kendali data ekstra.") %></li>
                            <li><i class="fas fa-check-circle text-success-custom me-3 fs-5"></i><strong>Hybrid:</strong> <%= Common.getBahasaConfig("Gabungan ketangguhan penyimpanan lokal (offline-first POS) & fleksibilitas akses internet.") %></li>
                        </ul>
                    </div>
                    <div class="col-md-6 ps-md-5">
                        <h3 class="mb-4 text-dark"><i class="fas fa-users-cog text-highlight me-3"></i><%= Common.getBahasaConfig("Multi-Portal Pengguna") %></h3>
                        <p class="fs-5 text-muted-custom"><%= Common.getBahasaConfig("Antarmuka spesifik berdasarkan hak akses (Role-Based Access):") %></p>
                        <ul class="feature-list mt-4 list-unstyled">
                            <li><i class="fas fa-cash-register text-info me-3 fs-5"></i><strong>Kasir:</strong> <%= Common.getBahasaConfig("POS cepat, diskon, cashback, dan buka/tutup kas per shift.") %></li>
                            <li><i class="fas fa-warehouse text-info me-3 fs-5"></i><strong>Petugas Gudang:</strong> <%= Common.getBahasaConfig("Mutasi stok, stok opname, dan penerimaan barang antar lokasi.") %></li>
                            <li><i class="fas fa-file-invoice-dollar text-info me-3 fs-5"></i><strong>Bagian Akuntansi:</strong> <%= Common.getBahasaConfig("Draft jurnal, HPP, dan pemetaan akun yang bisa dikonfigurasi.") %></li>
                            <li><i class="fas fa-user-tie text-info me-3 fs-5"></i><strong>Pimpinan/Investor:</strong> <%= Common.getBahasaConfig("Dashboard analitik dan laporan siap pakai.") %></li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-5 text-center text-highlight"><i class="fas fa-cash-register me-3"></i><%= Common.getBahasaConfig("1. POS & Kasir Offline-First") %></h2>
                <div class="row g-4">
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-store"></i> Kasir Multi-Outlet</div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Satu sistem kasir untuk banyak outlet/cabang, dengan katalog produk, diskon, dan cashback yang konsisten.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-wifi"></i> Offline-First (PWA)</div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Antrian transaksi tersimpan lokal (IndexedDB) dan Service Worker menjadikannya aplikasi yang bisa dipasang; sinkron otomatis dan idempoten begitu koneksi kembali — tidak ada transaksi ganda.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-lock"></i> Validasi Stok Server-Side</div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Penguncian baris (row-locking) mencegah dua kasir menjual stok yang sama secara bersamaan.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-cash-register"></i> Shift Kasir & Rekonsiliasi</div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Buka/tutup kas per shift bersifat opsional, dengan rekonsiliasi selisih kas otomatis dan berbagai metode pembayaran (tunai, non-tunai, QRIS).") %></p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-5 text-center text-highlight"><i class="fas fa-warehouse me-3"></i><%= Common.getBahasaConfig("2. Gudang & Manajemen Stok") %></h2>
                <div class="row g-4">
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-sitemap"></i> Hierarki Gudang Pusat-Cabang</div><p class="module-desc"><%= Common.getBahasaConfig("Struktur induk-anak antar gudang memungkinkan distribusi stok terkendali dari pusat ke cabang.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-map-marker-alt"></i> Multi-Lokasi</div><p class="module-desc"><%= Common.getBahasaConfig("Setiap lokasi fisik dapat dipantau stoknya secara independen dan digabungkan dalam laporan rollup.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-list-check"></i> Ledger Pergerakan Stok Auditable</div><p class="module-desc"><%= Common.getBahasaConfig("Stok masuk, keluar, transfer, dan penyesuaian selalu tercatat; tidak ada perubahan stok yang senyap — setiap perubahan tertelusur ke dokumennya.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-clipboard-check"></i> Stok Opname & Rollup Laporan</div><p class="module-desc"><%= Common.getBahasaConfig("Rekonsiliasi fisik berkala, dengan laporan yang bisa digulung (rollup) ke seluruh hierarki gudang.") %></p></div></div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-5 text-center text-highlight"><i class="fas fa-file-invoice-dollar me-3"></i><%= Common.getBahasaConfig("3. HPP Otomatis & Akuntansi Terintegrasi") %></h2>
                <div class="row g-4">
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-calculator"></i> HPP Resep Dihitung Otomatis</div><p class="module-desc"><%= Common.getBahasaConfig("Harga pokok produk racikan dirollup langsung dari biaya bahan baku resep, bukan angka yang diketik manual.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-book"></i> Posting Jurnal Otomatis</div><p class="module-desc"><%= Common.getBahasaConfig("Setiap transaksi kasir/pengadaan otomatis membentuk jurnal akuntansi, tertelusur ke dokumen sumbernya.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-tachometer-alt"></i> Dashboard Draft Jurnal Real-Time</div><p class="module-desc"><%= Common.getBahasaConfig("Ringkasan kesiapan posting jurnal dapat dipantau kapan saja sebelum diposting resmi.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-sliders-h"></i> Pemetaan Akun Fleksibel</div><p class="module-desc"><%= Common.getBahasaConfig("Chart of account dapat dikonfigurasi sesuai kebijakan lembaga, tidak dikunci pada kode akun baku.") %></p></div></div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-5 text-center text-highlight"><i class="fas fa-truck-ramp-box me-3"></i><%= Common.getBahasaConfig("4. Pengadaan (Procurement)") %></h2>
                <div class="row g-4">
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-route"></i> Alur PR → PO → BAST → Pembayaran</div><p class="module-desc"><%= Common.getBahasaConfig("Pipeline pengadaan matang: Purchase Request, Persetujuan, Purchase Order, Penerimaan Barang (BAST), hingga Pembayaran.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-code-branch"></i> Alur Persetujuan Configurable</div><p class="module-desc"><%= Common.getBahasaConfig("Mesin alur kerja persetujuan generik dapat diatur sesuai struktur organisasi dan nilai pengadaan.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-truck-field"></i> Master Vendor & Portal Vendor</div><p class="module-desc"><%= Common.getBahasaConfig("Data pemasok terpusat, lengkap dengan portal tersendiri bagi vendor untuk memantau status pesanan.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-file-shield"></i> Jejak Dokumen End-to-End</div><p class="module-desc"><%= Common.getBahasaConfig("Setiap tahap pengadaan tercatat dan dapat ditelusuri kembali untuk kebutuhan audit.") %></p></div></div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-2 text-center text-highlight"><i class="fas fa-mobile-screen-button me-3"></i><%= Common.getBahasaConfig("5. Aplikasi Pendukung: Sudah Jadi, Sudah Bisa Diunduh") %></h2>
                <p class="executive-lead text-center mb-5"><%= Common.getBahasaConfig("Bukan sekadar rencana -- 3 aplikasi klien ini sudah dirilis resmi dan siap dipasang hari ini juga, semuanya terhubung ke server yang sama.") %></p>
                <div class="row g-4">
                    <div class="col-md-4">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-desktop"></i> <%= Common.getBahasaConfig("POS Kasir Desktop") %></div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Kasir andal di meja utama -- tetap melayani walau internet putus, Layar Pelanggan kedua, bayar saldo member dengan PIN, 150+ laporan siap pakai, pembaruan otomatis.") %></p>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-tablet-screen-button"></i> <%= Common.getBahasaConfig("POS Kasir Android") %></div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Kasir dalam genggaman -- responsif di tablet maupun HP, cetak struk ke printer thermal Bluetooth, tetap berjualan tanpa sinyal.") %></p>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-barcode"></i> <%= Common.getBahasaConfig("Stok Opname Android") %></div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Stok opname secepat supermarket -- scan barcode via pemindai genggam atau kamera HP, fokus satu tugas, tim lapangan cepat mahir.") %></p>
                        </div>
                    </div>
                </div>
                <p class="text-center mt-4 mb-0">
                    <a href="https://github.com/Zishof/ais-pos-kasir-desktop/releases" target="_blank" rel="noopener" class="btn btn-primary shadow-sm me-2"><i class="fab fa-github"></i> POS Desktop</a>
                    <a href="https://github.com/Zishof/ais-pos-kasir-android/releases" target="_blank" rel="noopener" class="btn btn-primary shadow-sm me-2"><i class="fab fa-github"></i> POS Android</a>
                    <a href="https://github.com/Zishof/ais-stok-opname-android/releases" target="_blank" rel="noopener" class="btn btn-primary shadow-sm"><i class="fab fa-github"></i> Stok Opname Android</a>
                </p>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-route"></i> <%= Common.getBahasaConfig("Roadmap Pengembangan") %></span>
                    <h2 class="mt-4 mb-3 text-highlight"><%= Common.getBahasaConfig("Fondasi Sudah Berjalan, Ekspedisi & Bagi Hasil Investor Sedang Disiapkan") %></h2>
                    <p class="executive-lead"><%= Common.getBahasaConfig("Modul kasir, gudang, HPP, dan akuntansi di atas sudah dibangun dan berjalan. Modul Ekspedisi dan Portal Investor sudah dirancang secara arsitektural dan akan dirilis bertahap, aditif tanpa mengganggu operasional yang sedang berjalan.") %></p>
                </div>
                <div class="timeline-modern mt-4">
                    <div class="timeline-step"><div class="day"><%= Common.getBahasaConfig("Fase 1 - Berjalan") %></div><div class="icon"><i class="fas fa-check-double"></i></div><h4><%= Common.getBahasaConfig("Fondasi Kasir, Gudang & Akuntansi") %></h4><p class="text-muted-custom"><%= Common.getBahasaConfig("POS offline-first, hierarki gudang, HPP otomatis, dan posting jurnal — sudah aktif digunakan.") %></p></div>
                    <div class="timeline-step"><div class="day"><%= Common.getBahasaConfig("Fase 2 - Roadmap") %></div><div class="icon"><i class="fas fa-route"></i></div><h4><%= Common.getBahasaConfig("Ekspedisi (Distribusi)") %></h4><p class="text-muted-custom"><%= Common.getBahasaConfig("Rute pengiriman multi-outlet dalam satu perjalanan, manifest pengiriman, bukti terima, dan pelacakan kendaraan/BBM.") %></p></div>
                    <div class="timeline-step"><div class="day"><%= Common.getBahasaConfig("Fase 3 - Roadmap") %></div><div class="icon"><i class="fas fa-handshake-angle"></i></div><h4><%= Common.getBahasaConfig("Portal Bagi Hasil Investor") %></h4><p class="text-muted-custom"><%= Common.getBahasaConfig("Skema bagi hasil 3-fase yang dapat dikonfigurasi per perjanjian, dihitung dari Laba Bersih riil, bukan omzet kotor.") %></p></div>
                    <div class="timeline-step"><div class="day"><%= Common.getBahasaConfig("Fase 4 - Roadmap") %></div><div class="icon"><i class="fas fa-users"></i></div><h4><%= Common.getBahasaConfig("Kepegawaian Outlet") %></h4><p class="text-muted-custom"><%= Common.getBahasaConfig("Perluasan mesin absensi/penggajian yang sudah matang untuk staf ritel dan gudang — tinggal konfigurasi.") %></p></div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-calendar-check"></i> <%= Common.getBahasaConfig("Metodologi Implementasi") %></span>
                    <h2 class="mt-4 mb-3 text-highlight"><%= Common.getBahasaConfig("Roadmap Implementasi Bertahap, Terukur, dan Minim Gangguan Operasional") %></h2>
                    <p class="executive-lead"><%= Common.getBahasaConfig("Implementasi dilakukan dengan pendekatan bertahap agar kasir dan petugas gudang dapat beradaptasi, data lama dapat dimigrasikan secara tertib, dan transaksi harian tetap berjalan.") %></p>
                </div>
                <div class="timeline-modern mt-4">
                    <div class="timeline-step"><div class="day"><%= Common.getBahasaConfig("Tahap 1") %></div><div class="icon"><i class="fas fa-search"></i></div><h4><%= Common.getBahasaConfig("Asesmen & Pemetaan Alur Bisnis") %></h4><p class="text-muted-custom"><%= Common.getBahasaConfig("Memetakan alur kasir-gudang-pengadaan-akuntansi, kebutuhan modul, dan prioritas implementasi.") %></p></div>
                    <div class="timeline-step"><div class="day"><%= Common.getBahasaConfig("Tahap 2") %></div><div class="icon"><i class="fas fa-cogs"></i></div><h4><%= Common.getBahasaConfig("Konfigurasi Sistem") %></h4><p class="text-muted-custom"><%= Common.getBahasaConfig("Menyiapkan master produk, harga, resep, hierarki gudang, akun akuntansi, dan hak akses.") %></p></div>
                    <div class="timeline-step"><div class="day"><%= Common.getBahasaConfig("Tahap 3") %></div><div class="icon"><i class="fas fa-people-arrows"></i></div><h4><%= Common.getBahasaConfig("Migrasi & Pelatihan") %></h4><p class="text-muted-custom"><%= Common.getBahasaConfig("Import data stok/harga, uji coba alur kerja, pelatihan kasir/petugas gudang/bagian akuntansi.") %></p></div>
                    <div class="timeline-step"><div class="day"><%= Common.getBahasaConfig("Tahap 4") %></div><div class="icon"><i class="fas fa-rocket"></i></div><h4><%= Common.getBahasaConfig("Go-Live & Evaluasi") %></h4><p class="text-muted-custom"><%= Common.getBahasaConfig("Sistem digunakan secara operasional, dipantau, lalu dievaluasi untuk penyempurnaan berkelanjutan.") %></p></div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-3 text-center text-highlight"><%= Common.getBahasaConfig("Skema Harga: Kasir (POS) per Outlet") %></h2>
                <p class="text-center text-muted-custom mb-5 fs-4"><%= Common.getBahasaConfig("Makin Banyak Outlet, Makin Hemat per Unit") %></p>

                <div class="row justify-content-center mt-4">
                    <div class="col-lg-10">
                        <div class="pricing-box">
                            <div class="text-center mb-4">
                                <div class="price-badge fs-3 px-5 py-3 mb-3"><%= Common.getBahasaConfig("Rp 175.000 - Rp 249.000 / Bulan / POS") %></div>
                                <h3 class="text-dark"><%= Common.getBahasaConfig("Tarif Bertingkat Sesuai Jumlah Outlet") %></h3>
                            </div>

                            <ul class="feature-list list-unstyled ps-4 text-start">
                                <li class="mb-3"><i class="fas fa-check-circle text-success-custom me-3 fs-4"></i><strong><%= Common.getBahasaConfig("1-5 outlet:") %></strong> <%= Common.getBahasaConfig("POS pertama Rp249.000/bulan, POS tambahan Rp99.000/bulan/unit.") %></li>
                                <li class="mb-3"><i class="fas fa-check-circle text-success-custom me-3 fs-4"></i><strong><%= Common.getBahasaConfig("6-20 outlet:") %></strong> <%= Common.getBahasaConfig("POS pertama Rp225.000/bulan, POS tambahan Rp85.000/bulan/unit.") %></li>
                                <li class="mb-3"><i class="fas fa-check-circle text-success-custom me-3 fs-4"></i><strong><%= Common.getBahasaConfig("21-50 outlet:") %></strong> <%= Common.getBahasaConfig("POS pertama Rp199.000/bulan, POS tambahan Rp75.000/bulan/unit.") %></li>
                                <li class="mb-4"><i class="fas fa-check-circle text-success-custom me-3 fs-4"></i><strong><%= Common.getBahasaConfig("Lebih dari 50 outlet:") %></strong> <%= Common.getBahasaConfig("mulai Rp175.000/bulan (POS pertama), mulai Rp60.000/bulan/unit (POS tambahan).") %></li>
                            </ul>
                            <p class="text-muted-custom fs-6 mb-0"><strong><%= Common.getBahasaConfig("Catatan:") %></strong> <%= Common.getBahasaConfig("POS pertama sudah mencakup transaksi penjualan, pembayaran tunai & nontunai, shift kasir, stok outlet, harga & diskon, data pelanggan, retur & pembatalan, laporan penjualan, hak akses, mode offline-first, sinkronisasi, dan backup cloud - di luar perangkat keras & jaringan internet.") %></p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-3 text-center text-highlight"><%= Common.getBahasaConfig("Skema Harga: Paket Modul Pusat") %></h2>
                <p class="text-center text-muted-custom mb-5 fs-4"><%= Common.getBahasaConfig("Satu Biaya untuk Seluruh Perusahaan, Bukan per Outlet") %></p>

                <div class="row g-4 justify-content-center mt-3">
                    <div class="col-lg-4">
                        <div class="pricing-box h-100">
                            <div class="text-center mb-4">
                                <div class="price-badge fs-5 px-4 py-2 mb-3"><%= Common.getBahasaConfig("Rp 750.000 / Bulan") %></div>
                                <h3 class="text-dark fs-4"><%= Common.getBahasaConfig("Central Basic") %></h3>
                            </div>
                            <ul class="feature-list list-unstyled ps-4 text-start">
                                <li class="mb-3"><i class="fas fa-check-circle text-success-custom me-3 fs-4"></i><%= Common.getBahasaConfig("Dashboard pusat") %></li>
                                <li class="mb-3"><i class="fas fa-check-circle text-success-custom me-3 fs-4"></i><%= Common.getBahasaConfig("Stok konsolidasi") %></li>
                                <li class="mb-3"><i class="fas fa-check-circle text-success-custom me-3 fs-4"></i><%= Common.getBahasaConfig("Transfer antaroutlet") %></li>
                                <li class="mb-3"><i class="fas fa-check-circle text-success-custom me-3 fs-4"></i><%= Common.getBahasaConfig("Pembelian sederhana") %></li>
                            </ul>
                        </div>
                    </div>
                    <div class="col-lg-4">
                        <div class="pricing-box h-100" style="border-color: #d97706;">
                            <div class="text-center mb-4">
                                <div class="price-badge fs-5 px-4 py-2 mb-3" style="background: rgba(217, 119, 6, 0.1); color: #d97706; border-color: #d97706;"><%= Common.getBahasaConfig("Rp 1.500.000 / Bulan") %></div>
                                <h3 class="text-dark fs-4"><%= Common.getBahasaConfig("Central Professional") %></h3>
                            </div>
                            <ul class="feature-list list-unstyled ps-4 text-start">
                                <li class="mb-3"><i class="fas fa-check-circle text-warning-custom me-3 fs-4"></i><%= Common.getBahasaConfig("Gudang pusat") %></li>
                                <li class="mb-3"><i class="fas fa-check-circle text-warning-custom me-3 fs-4"></i><%= Common.getBahasaConfig("Pengadaan PR/PO") %></li>
                                <li class="mb-3"><i class="fas fa-check-circle text-warning-custom me-3 fs-4"></i><%= Common.getBahasaConfig("Distribusi & pengiriman") %></li>
                                <li class="mb-3"><i class="fas fa-check-circle text-warning-custom me-3 fs-4"></i><%= Common.getBahasaConfig("Produksi & HPP") %></li>
                            </ul>
                        </div>
                    </div>
                    <div class="col-lg-4">
                        <div class="pricing-box h-100" style="border-color: #16a34a;">
                            <div class="text-center mb-4">
                                <div class="price-badge fs-5 px-4 py-2 mb-3" style="background: rgba(22, 163, 74, 0.1); color: #16a34a; border-color: #16a34a;"><%= Common.getBahasaConfig("Rp 2.500.000 / Bulan") %></div>
                                <h3 class="text-dark fs-4"><%= Common.getBahasaConfig("Full Integrated Suite") %></h3>
                            </div>
                            <ul class="feature-list list-unstyled ps-4 text-start">
                                <li class="mb-3"><i class="fas fa-check-circle text-success me-3 fs-4"></i><%= Common.getBahasaConfig("Seluruh modul pusat") %></li>
                                <li class="mb-3"><i class="fas fa-check-circle text-success me-3 fs-4"></i><%= Common.getBahasaConfig("Akuntansi & kepegawaian") %></li>
                                <li class="mb-3"><i class="fas fa-check-circle text-success me-3 fs-4"></i><%= Common.getBahasaConfig("Portal investor & BEP") %></li>
                                <li class="mb-3"><i class="fas fa-check-circle text-success me-3 fs-4"></i><%= Common.getBahasaConfig("Audit & analitik") %></li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-3 text-center text-highlight"><%= Common.getBahasaConfig("Simulasi & Biaya Implementasi") %></h2>
                <p class="text-center text-muted-custom mb-5 fs-4"><%= Common.getBahasaConfig("Contoh Perhitungan Nyata") %></p>

                <div class="row justify-content-center mt-4">
                    <div class="col-lg-10">
                        <div class="pricing-box">
                            <div class="text-center mb-4">
                                <h3 class="text-dark"><%= Common.getBahasaConfig("Simulasi Biaya Bulanan (Asumsi 2 POS per Outlet + Full Integrated Suite)") %></h3>
                            </div>
                            <ul class="feature-list list-unstyled ps-4 text-start">
                                <li class="mb-3"><i class="fas fa-calculator text-success-custom me-3 fs-4"></i><strong><%= Common.getBahasaConfig("Jaringan 5 outlet:") %></strong> <%= Common.getBahasaConfig("POS Rp1.740.000 + Full Integrated Suite Rp2.500.000 = total sekitar Rp4.240.000/bulan.") %></li>
                                <li class="mb-3"><i class="fas fa-calculator text-success-custom me-3 fs-4"></i><strong><%= Common.getBahasaConfig("Jaringan 10 outlet:") %></strong> <%= Common.getBahasaConfig("POS Rp3.100.000 + Full Integrated Suite Rp2.500.000 = total sekitar Rp5.600.000/bulan.") %></li>
                                <li class="mb-4"><i class="fas fa-calculator text-success-custom me-3 fs-4"></i><strong><%= Common.getBahasaConfig("Jaringan 30 outlet:") %></strong> <%= Common.getBahasaConfig("POS Rp8.220.000 + Full Integrated Suite Rp2.500.000 = total sekitar Rp10.720.000/bulan.") %></li>
                            </ul>
                            <p class="text-muted-custom fs-6 mb-2"><strong><%= Common.getBahasaConfig("Biaya Implementasi Awal:") %></strong> <%= Common.getBahasaConfig("Sekali bayar di awal, mulai dari Rp1,5 juta untuk 1 outlet hingga Rp30-50 juta untuk skala jaringan besar, tergantung kompleksitas migrasi data dan pelatihan.") %></p>
                            <p class="text-muted-custom fs-6 mb-0"><strong><%= Common.getBahasaConfig("Opsi Pelengkap:") %></strong> <%= Common.getBahasaConfig("Bagi hasil per transaksi (sekitar Rp50-100/transaksi) tersedia sebagai opsi pelengkap bagi unit usaha yang ingin memulai tanpa biaya implementasi di muka.") %></p>
                            <p class="text-muted-custom fs-6 mb-0 mt-2"><i class="fas fa-handshake me-2"></i><%= Common.getBahasaConfig("Seluruh angka di atas bersifat indikatif dan terbuka untuk dibicarakan sesuai kebutuhan serta skala jaringan usaha masing-masing.") %></p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content px-5 py-5">
                <h2 class="mb-5 text-center text-highlight"><%= Common.getBahasaConfig("Dukungan Implementasi & Pendampingan Penuh") %></h2>
                <div class="row g-4 justify-content-center mt-3">
                    <div class="col-md-6">
                        <div class="info-card text-center p-4">
                            <i class="fas fa-server fa-3x text-primary mb-4"></i>
                            <h4 class="text-highlight fw-bold mb-3"><%= Common.getBahasaConfig("Tim Ahli Infrastruktur IT") %></h4>
                            <p class="text-muted-custom fs-6 mb-0"><%= Common.getBahasaConfig("Menjamin keandalan server, keamanan data transaksi, serta pemeliharaan sistem agar berjalan lancar 24/7.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="info-card text-center p-4">
                            <i class="fas fa-chalkboard-teacher fa-3x text-success mb-4"></i>
                            <h4 class="text-highlight fw-bold mb-3"><%= Common.getBahasaConfig("Trainer & Implementator") %></h4>
                            <p class="text-muted-custom fs-6 mb-0"><%= Common.getBahasaConfig("Mendampingi migrasi data dan memberikan Pelatihan Gratis secara ONLINE maupun OFFLINE bagi kasir dan petugas gudang.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="info-card text-center p-4">
                            <i class="fas fa-diagram-project fa-3x text-warning-custom mb-4"></i>
                            <h4 class="text-highlight fw-bold mb-3"><%= Common.getBahasaConfig("Konsultan Alur Bisnis Ritel") %></h4>
                            <p class="text-muted-custom fs-6 mb-0"><%= Common.getBahasaConfig("Memastikan alur kasir, gudang, pengadaan, dan akuntansi sesuai kebutuhan operasional unit usaha.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="info-card text-center p-4">
                            <i class="fas fa-headset fa-3x text-danger mb-4"></i>
                            <h4 class="text-highlight fw-bold mb-3"><%= Common.getBahasaConfig("Layanan Bantuan (Helpdesk)") %></h4>
                            <p class="text-muted-custom fs-6 mb-0"><%= Common.getBahasaConfig("Tim dukungan teknis yang siap dihubungi untuk menyelesaikan setiap kendala operasional harian.") %></p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-clipboard-check"></i> <%= Common.getBahasaConfig("Indikator Keberhasilan") %></span>
                    <h2 class="mt-4 mb-3 text-highlight"><%= Common.getBahasaConfig("Parameter yang Dapat Dipantau Setelah Sistem Berjalan") %></h2>
                </div>
                <div class="row g-4">
                    <div class="col-md-6"><div class="decision-card"><h4><i class="fas fa-tachometer-alt text-primary me-2"></i><%= Common.getBahasaConfig("Kecepatan Transaksi Kasir") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Waktu layanan kasir dan antrean dapat dipantau melalui data sistem, termasuk saat mode offline.") %></p></div></div>
                    <div class="col-md-6"><div class="decision-card"><h4><i class="fas fa-boxes-stacked text-success me-2"></i><%= Common.getBahasaConfig("Akurasi Stok") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Selisih stok antara catatan sistem dan fisik gudang dapat diukur lewat hasil stok opname berkala.") %></p></div></div>
                    <div class="col-md-6"><div class="decision-card"><h4><i class="fas fa-coins text-warning me-2"></i><%= Common.getBahasaConfig("Kontrol Keuangan") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("HPP, jurnal, dan selisih kas dapat dimonitor lebih transparan oleh kasir, akuntansi, dan pimpinan.") %></p></div></div>
                    <div class="col-md-6"><div class="decision-card"><h4><i class="fas fa-user-shield text-danger me-2"></i><%= Common.getBahasaConfig("Akuntabilitas Proses") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Alur persetujuan pengadaan, perubahan stok, dan histori transaksi tersimpan untuk kebutuhan audit.") %></p></div></div>
                </div>
                <div class="quote-panel mt-4 text-center"><strong><%= Common.getBahasaConfig("Dengan indikator yang jelas, transformasi digital tidak berhenti pada penggunaan aplikasi, tetapi menjadi proses peningkatan mutu operasional dan keuangan yang dapat diukur.") %></strong></div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content text-center shadow-lg" style="background: var(--card-bg);">

                <div class="mb-4">
                    <i class="fas fa-rocket fa-4x text-primary mb-3" style="animation: pulse-primary 2s infinite; border-radius: 50%;"></i>
                    <h2 class="fw-bold text-dark" style="font-size: 2.2rem;"><%= Common.getBahasaConfig("Siap Digitalkan Gudang, Kasir & Unit Usaha Anda?") %></h2>
                    <p class="fs-5 text-muted-custom mt-2"><%= Common.getBahasaConfig("Jadwalkan presentasi, validasi kebutuhan modul, dan mulai susun langkah transformasi digital gudang dan bisnis ritel Anda secara bertahap mulai hari ini.") %></p>
                </div>

                <% if(paramPresentBy != null && !paramPresentBy.trim().isEmpty()) { %>
                    <div class="d-inline-block p-4 mb-4 bg-white rounded-4 border border-warning shadow-sm text-start" style="min-width: 380px;">
                        <h6 class="text-highlight mb-3 text-center text-uppercase fw-bold" style="letter-spacing: 1px;"><i class="fas fa-user-tie me-2"></i><%= Common.getBahasaConfig("Konsultan Anda") %></h6>
                        <div class="fs-5 text-dark mb-2 text-center"><strong><%=paramPresentBy%></strong></div>
                        <% if(paramTelp != null && !paramTelp.trim().isEmpty()) { %>
                            <div class="fs-5 text-dark mb-3 text-center"><i class="fab fa-whatsapp text-success me-2"></i> <%=paramTelp%></div>
                            <div class="text-center mt-3">
                                <a href="https://wa.me/<%=paramTelp.replaceAll("[^0-9]", "")%>" target="_blank" class="btn btn-success rounded-pill px-4 py-2 fw-bold shadow w-100"><i class="fab fa-whatsapp me-2"></i> Hubungi Sekarang</a>
                            </div>
                        <% } %>
                    </div>
                <% } else { %>
                    <div class="d-flex flex-wrap justify-content-center gap-4 mb-4">
                        <div class="p-3 px-4 bg-white rounded-pill border border-light shadow-sm fs-5">
                            <i class="fas fa-globe text-highlight me-2"></i> ecampus.id
                        </div>
                    </div>
                <% } %>

                <div class="row justify-content-center mt-2">
                    <div class="col-lg-12">
                        <div class="p-4 bg-white rounded-4 shadow-sm border border-light d-flex flex-column align-items-center">

                            <h6 class="text-muted-custom mb-3 fw-bold text-uppercase" style="letter-spacing: 1px;"><i class="fas fa-folder-open me-2 text-warning"></i><%= Common.getBahasaConfig("Dokumen Penawaran Modul Gudang, POS & Investor") %></h6>

                            <div class="d-flex flex-wrap justify-content-center gap-3 mb-4 w-100">
                                <a href="<%=Common.ROOT%>/penawaran<%=appendParamsGudang%>" target="_blank" class="btn btn-outline-danger fw-bold px-3 py-3 rounded-4 shadow-sm flex-grow-1 doc-btn">
                                    <i class="far fa-envelope-open fa-2x mb-2 d-block"></i> <%= Common.getBahasaConfig("Surat Penawaran") %>
                                </a>
                                <a href="<%=Common.ROOT%>/proposal<%=appendParamsGudang%>" target="_blank" class="btn btn-outline-primary fw-bold px-3 py-3 rounded-4 shadow-sm flex-grow-1 doc-btn">
                                    <i class="far fa-file-alt fa-2x mb-2 d-block"></i> <%= Common.getBahasaConfig("Lihat Proposal") %>
                                </a>
                                <a href="<%=Common.ROOT%>/pks<%=appendParamsGudang%>" target="_blank" class="btn btn-outline-success fw-bold px-3 py-3 rounded-4 shadow-sm flex-grow-1 doc-btn">
                                    <i class="far fa-handshake fa-2x mb-2 d-block"></i> <%= Common.getBahasaConfig("Draft PKS") %>
                                </a>
                            </div>

                            <a href="<%=targetDemoUrl%>" target="_blank" class="btn btn-primary fw-bold px-5 py-3 rounded-pill text-uppercase shadow-lg fs-5 w-75 pulse-button" style="letter-spacing: 2px;">
                                <%= Common.getBahasaConfig("Jelajahi Ekosistem Enterprise Education") %> <i class="fas fa-arrow-right ms-2"></i>
                            </a>

                        </div>
                    </div>
                </div>

            </div>
        </div>

        <div class="controls">
            <button class="btn-nav" id="btnPrev" title="<%= Common.getBahasaConfig("Sebelumnya") %>"><i class="fas fa-chevron-left"></i></button>
            <span class="slide-counter"><span id="currentSlideNum">1</span> / <span id="totalSlideNum">19</span></span>
            <button class="btn-nav" id="btnNext" title="<%= Common.getBahasaConfig("Selanjutnya") %>"><i class="fas fa-chevron-right"></i></button>
            <div class="vr bg-dark mx-2 opacity-25"></div>
            <button class="btn-nav" id="btnFullscreen" title="<%= Common.getBahasaConfig("Layar Penuh") %>"><i class="fas fa-expand"></i></button>
        </div>

    </div>

    <script src="<%=request.getContextPath() %>/js/pesan-formal.js"></script>
    <script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        document.addEventListener('DOMContentLoaded', () => {
            const slides = document.querySelectorAll('.slide');
            const btnPrev = document.getElementById('btnPrev');
            const btnNext = document.getElementById('btnNext');
            const btnFullscreen = document.getElementById('btnFullscreen');
            const currentSlideNum = document.getElementById('currentSlideNum');
            const totalSlideNum = document.getElementById('totalSlideNum');

            let currentIndex = 0;
            totalSlideNum.textContent = slides.length;

            function updateSlides() {
                slides.forEach((slide, index) => {
                    if(index === currentIndex) { slide.classList.add('active'); }
                    else { slide.classList.remove('active'); }
                });
                currentSlideNum.textContent = currentIndex + 1;
                var progressBar = document.getElementById('deckProgressBar');
                if (progressBar) {
                    progressBar.style.width = (((currentIndex + 1) / slides.length) * 100) + '%';
                }
                btnPrev.disabled = currentIndex === 0;
                btnNext.disabled = currentIndex === slides.length - 1;
            }

            function nextSlide() { if (currentIndex < slides.length - 1) { currentIndex++; updateSlides(); } }
            function prevSlide() { if (currentIndex > 0) { currentIndex--; updateSlides(); } }

            btnNext.addEventListener('click', nextSlide);
            btnPrev.addEventListener('click', prevSlide);

            document.addEventListener('keydown', (e) => {
                if (e.key === 'ArrowRight' || e.key === 'PageDown' || e.key === ' ') { e.preventDefault(); nextSlide(); }
                else if (e.key === 'ArrowLeft' || e.key === 'PageUp') { e.preventDefault(); prevSlide(); }
                else if (e.key === 'Home') { currentIndex = 0; updateSlides(); }
                else if (e.key === 'End') { currentIndex = slides.length - 1; updateSlides(); }
                else if (e.key.toLowerCase() === 'f') { btnFullscreen.click(); }
            });

            btnFullscreen.addEventListener('click', () => {
                if (!document.fullscreenElement) {
                    document.documentElement.requestFullscreen().catch(err => { tampilkanPesanGagalFormal("pengaktifan mode layar penuh (fullscreen)", "Rincian teknis: " + err.message, ["Pastikan peramban Bapak/Ibu mengizinkan mode layar penuh untuk halaman ini.", "Coba gunakan tombol F11 pada papan ketik sebagai alternatif."]); });
                    btnFullscreen.innerHTML = '<i class="fas fa-compress"></i>';
                } else {
                    document.exitFullscreen();
                    btnFullscreen.innerHTML = '<i class="fas fa-expand"></i>';
                }
            });

            document.addEventListener('fullscreenchange', () => {
                if (!document.fullscreenElement) { btnFullscreen.innerHTML = '<i class="fas fa-expand"></i>'; }
            });

            updateSlides();
        });
    </script>
    <jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp" />
</body>
</html>
