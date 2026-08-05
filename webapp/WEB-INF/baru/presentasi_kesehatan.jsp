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
String targetDemoUrl = "https://apps.emedik.id" + (queryString != null && !queryString.isEmpty() ? "?" + queryString : "");
String appendParams = (queryString != null && !queryString.isEmpty()) ? "?" + queryString : "";
String appendParamsKesehatan = "?modul=kesehatan" + (queryString != null && !queryString.isEmpty() ? "&" + queryString : "");

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
    <title><%= Common.getBahasaConfig("Presentasi Modul Kesehatan (eMedic)") %> | <%=judul%></title>

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
            content: "eMedic - Enterprise Education";
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
    <div class="presentation-brand-chip"><i class="fas fa-hospital me-2"></i> eMedic</div>
    <div class="kbd-hint"><kbd>←</kbd> <kbd>→</kbd> Navigasi &nbsp; <kbd>F</kbd> Fullscreen</div>

    <img src="<%=logo_PerguruanTinggi%>" class="logo-header" alt="Logo">

    <div class="presentation-container" id="presentationArea">

        <div class="slide active">
            <div class="text-center slide-content" style="overflow:hidden;">
                <span class="section-eyebrow mb-4"><i class="fas fa-bolt"></i> <%= Common.getBahasaConfig("Executive Presentation Deck") %></span>
                <i class="fas fa-hospital fa-5x mb-4 text-warning-custom d-block"></i>
                <h1 class="display-3 mb-3 hero-gradient-title"><%= Common.getBahasaConfig("Transformasi Digital Layanan Kesehatan") %></h1>
                <h2 class="fw-bold text-warning-custom mb-4 display-6"><%= Common.getBahasaConfig("eMedic - Sistem Informasi Rumah Sakit, Klinik & Puskesmas") %></h2>
                <hr class="w-25 mx-auto border-info opacity-50 mb-4 border-2">
                <p class="fs-4 text-muted-custom mb-3"><%= Common.getBahasaConfig("Solusi Terpadu untuk Registrasi, Rekam Medis, Farmasi, Billing, dan Dashboard Analitik") %></p>
                <p class="executive-lead mb-4"><%= Common.getBahasaConfig("Bagian dari ekosistem Enterprise Education — terintegrasi langsung bila Universitas, Sekolah, atau Pondok Pesantren Anda memiliki klinik/rumah sakit sendiri, atau dapat diimplementasikan berdiri sendiri (standalone) untuk Rumah Sakit, Klinik, Puskesmas, dan layanan fasilitas kesehatan lainnya.") %></p>
                <div class="d-flex flex-wrap justify-content-center gap-3 mt-4">
                    <div class="metric-chip"><strong><i class="fas fa-network-wired"></i></strong><span class="small fw-bold text-muted-custom"><%= Common.getBahasaConfig("Satu Data Terpadu") %></span></div>
                    <div class="metric-chip"><strong><i class="fas fa-chart-line"></i></strong><span class="small fw-bold text-muted-custom"><%= Common.getBahasaConfig("Keputusan Berbasis Dashboard") %></span></div>
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
                    <h2 class="mt-4 mb-3 hero-gradient-title"><%= Common.getBahasaConfig("Satu Platform untuk Menggerakkan Seluruh Alur Layanan Kesehatan") %></h2>
                    <p class="executive-lead"><%= Common.getBahasaConfig("eMedic bukan sekadar aplikasi administrasi. Sistem ini menjadi fondasi tata kelola fasilitas kesehatan: menyatukan data pasien, mempercepat layanan, mengurangi risiko kesalahan manual, dan menyediakan informasi yang siap dipakai pimpinan untuk mengambil keputusan strategis.") %></p>
                </div>
                <div class="row g-4 mt-3">
                    <div class="col-md-4"><div class="impact-card"><div class="impact-number">01</div><h4><%= Common.getBahasaConfig("Integrasi Klinis") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Registrasi, rekam medis, farmasi, gudang, dan billing berjalan dalam alur kerja yang saling terhubung.") %></p></div></div>
                    <div class="col-md-4"><div class="impact-card"><div class="impact-number">02</div><h4><%= Common.getBahasaConfig("Layanan Lebih Cepat") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Pasien, dokter, perawat, dan farmasis mendapatkan alur layanan yang mengurangi antrean dan mempercepat verifikasi.") %></p></div></div>
                    <div class="col-md-4"><div class="impact-card"><div class="impact-number">03</div><h4><%= Common.getBahasaConfig("Kontrol Manajemen") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Pimpinan memperoleh dashboard, audit trail, dan rekap real-time untuk mengawal mutu dan keuangan fasilitas kesehatan.") %></p></div></div>
                </div>
                <div class="quote-panel mt-4 text-center">
                    <h5 class="mb-1 text-dark"><i class="fas fa-quote-left text-warning-custom me-2"></i><%= Common.getBahasaConfig("Tujuan akhirnya sederhana: fasilitas kesehatan menjadi lebih rapi, responsif, aman bagi pasien, dan siap menghadapi regulasi kesehatan digital.") %></h5>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-3"><span class="section-eyebrow"><i class="fas fa-triangle-exclamation"></i> <%= Common.getBahasaConfig("Masalah yang Perlu Diselesaikan") %></span></div>
                <h2 class="mb-5 text-center"><i class="fas fa-exclamation-circle me-3 text-warning-custom"></i><%= Common.getBahasaConfig("Tantangan Fasilitas Kesehatan Saat Ini") %></h2>
                <div class="row g-4">
                    <div class="col-md-6">
                        <div class="module-card" style="border-left-color: var(--danger-color);">
                            <div class="module-title"><i class="fas fa-prescription text-danger"></i> <%= Common.getBahasaConfig("Resep Kertas Rawan Salah Baca") %></div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Tulisan tangan dokter yang sulit dibaca meningkatkan risiko kesalahan pemberian obat dan dosis.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card" style="border-left-color: var(--danger-color);">
                            <div class="module-title"><i class="fas fa-boxes text-danger"></i> <%= Common.getBahasaConfig("Stok Farmasi Tidak Akurat") %></div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Obat bisa kosong mendadak atau menumpuk berlebih tanpa disadari akibat pencatatan manual.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card" style="border-left-color: var(--danger-color);">
                            <div class="module-title"><i class="fas fa-thermometer-half text-danger"></i> <%= Common.getBahasaConfig("Kadaluarsa Tak Terdeteksi Dini") %></div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Obat kadaluarsa terpakai atau terbuang tanpa peringatan lebih awal, menimbulkan risiko dan kerugian.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card" style="border-left-color: var(--danger-color);">
                            <div class="module-title"><i class="fas fa-cash-register text-danger"></i> <%= Common.getBahasaConfig("Billing Terpisah dari Rekam Medis") %></div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Tagihan pasien tidak selalu sinkron dengan tindakan atau obat yang diberikan, rawan selisih kas.") %></p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-route"></i> <%= Common.getBahasaConfig("Transformasi yang Dihasilkan") %></span>
                    <h2 class="mt-4 mb-3 text-highlight"><%= Common.getBahasaConfig("Dari Sistem Terpisah Menjadi Ekosistem Kesehatan Terpadu") %></h2>
                    <p class="executive-lead"><%= Common.getBahasaConfig("Dengan pendekatan terpadu, registrasi, rekam medis, farmasi, dan kasir tidak lagi bekerja sendiri-sendiri. Data yang sama dapat dipakai oleh banyak fungsi sehingga proses lebih ringkas dan validasi menjadi lebih kuat.") %></p>
                </div>
                <div class="before-after mt-4">
                    <div class="row g-4 align-items-stretch">
                        <div class="col-md-6 pe-md-5">
                            <h4 class="text-danger mb-3"><i class="fas fa-times-circle me-2"></i><%= Common.getBahasaConfig("Sebelum Terintegrasi") %></h4>
                            <ul class="feature-list list-unstyled mb-0">
                                <li><i class="fas fa-minus-circle text-danger me-2"></i><%= Common.getBahasaConfig("Rekam medis dan resep tersebar di banyak berkas fisik.") %></li>
                                <li><i class="fas fa-minus-circle text-danger me-2"></i><%= Common.getBahasaConfig("Laporan pimpinan lambat karena perlu rekap manual.") %></li>
                                <li><i class="fas fa-minus-circle text-danger me-2"></i><%= Common.getBahasaConfig("Farmasi, tindakan, dan billing sering tidak sinkron.") %></li>
                                <li><i class="fas fa-minus-circle text-danger me-2"></i><%= Common.getBahasaConfig("Sulit menelusuri riwayat perubahan data rekam medis.") %></li>
                            </ul>
                        </div>
                        <div class="col-md-6 ps-md-5">
                            <h4 class="text-success-custom mb-3"><i class="fas fa-check-circle me-2"></i><%= Common.getBahasaConfig("Sesudah Menggunakan eMedic") %></h4>
                            <ul class="feature-list list-unstyled mb-0">
                                <li><i class="fas fa-check-circle text-success me-2"></i><%= Common.getBahasaConfig("Satu sumber data untuk pendaftaran, rekam medis, dan farmasi.") %></li>
                                <li><i class="fas fa-check-circle text-success me-2"></i><%= Common.getBahasaConfig("Dashboard real-time untuk okupansi, pendapatan, dan diagnosa.") %></li>
                                <li><i class="fas fa-check-circle text-success me-2"></i><%= Common.getBahasaConfig("Pembayaran otomatis memperbarui tagihan dan stok obat.") %></li>
                                <li><i class="fas fa-check-circle text-success me-2"></i><%= Common.getBahasaConfig("Audit trail membantu menjaga akuntabilitas proses klinis.") %></li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content text-center">
                <h2 class="mb-4 text-highlight"><i class="fas fa-network-wired me-3"></i><%= Common.getBahasaConfig("Solusi: eMedic") %></h2>
                <p class="fs-4 mb-5 text-muted-custom"><%= Common.getBahasaConfig("Terintegrasi dengan Ekosistem Pendidikan, atau Berdiri Sendiri") %></p>
                <div class="row justify-content-center mt-4">
                    <div class="col-lg-10">
                        <div class="d-flex justify-content-around align-items-center flex-wrap gap-4">
                            <div class="p-4 module-card text-center" style="flex: 1; min-width: 280px;">
                                <i class="fas fa-graduation-cap fa-4x text-primary mb-4"></i>
                                <h3 class="text-dark">Terintegrasi Pendidikan</h3>
                                <p class="text-muted-custom mb-0 mt-2">Klinik/rumah sakit milik Universitas (eCampus), Sekolah (eSchool), atau Pondok Pesantren (ePesantren)</p>
                            </div>
                            <i class="fas fa-link fa-2x text-muted-custom opacity-50"></i>
                            <div class="p-4 module-card text-center" style="flex: 1; min-width: 280px; border-left-color: var(--success-color);">
                                <i class="fas fa-hospital fa-4x text-success mb-4"></i>
                                <h3 class="text-dark">Berdiri Sendiri</h3>
                                <p class="text-muted-custom mb-0 mt-2">Rumah Sakit, Klinik, Puskesmas, atau layanan fasilitas kesehatan independen</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-star"></i> <%= Common.getBahasaConfig("Nilai Strategis untuk Fasilitas Kesehatan") %></span>
                    <h2 class="mt-4 mb-3 text-highlight"><%= Common.getBahasaConfig("Manfaat yang Langsung Terasa oleh Pimpinan, Tenaga Medis, dan Pasien") %></h2>
                </div>
                <div class="value-strip">
                    <div><h5><i class="fas fa-stopwatch text-primary me-2"></i><%= Common.getBahasaConfig("Efisiensi Waktu") %></h5><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Proses pendaftaran, pemeriksaan, dan dispensing obat dapat dialihkan menjadi alur digital yang terdokumentasi.") %></p></div>
                    <div><h5><i class="fas fa-user-check text-success me-2"></i><%= Common.getBahasaConfig("Keselamatan Pasien") %></h5><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Resep digital dan pengecekan stok mengurangi risiko kesalahan pemberian obat.") %></p></div>
                    <div><h5><i class="fas fa-database text-warning me-2"></i><%= Common.getBahasaConfig("Data Lebih Tertib") %></h5><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Rekam medis, resep, dan billing tersimpan rapi sehingga mudah ditelusuri kembali.") %></p></div>
                    <div><h5><i class="fas fa-chart-pie text-info me-2"></i><%= Common.getBahasaConfig("Laporan Siap Pakai") %></h5><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Rekap operasional dan manajerial tersedia untuk mendukung rapat pimpinan dan akreditasi.") %></p></div>
                </div>
                <div class="row g-4 mt-4">
                    <div class="col-md-6"><div class="impact-card"><h4><i class="fas fa-hospital text-primary me-2"></i><%= Common.getBahasaConfig("Citra Fasilitas Lebih Profesional") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Portal digital, layanan pembayaran modern, dan dashboard pimpinan memberi kesan fasilitas kesehatan yang tertata dan siap melayani.") %></p></div></div>
                    <div class="col-md-6"><div class="impact-card"><h4><i class="fas fa-seedling text-success-custom me-2"></i><%= Common.getBahasaConfig("Siap Bertumbuh Jangka Panjang") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Modul dapat diterapkan bertahap, mulai dari kebutuhan paling mendesak hingga integrasi BPJS/SATUSEHAT.") %></p></div></div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-5 text-center text-highlight"><i class="fas fa-network-wired me-3"></i><%= Common.getBahasaConfig("Fleksibilitas Infrastruktur & Akses") %></h2>
                <div class="row g-5 mt-2">
                    <div class="col-md-6 border-end border-secondary border-opacity-25 pe-md-5">
                        <h3 class="mb-4 text-dark"><i class="fas fa-cloud text-highlight me-3"></i><%= Common.getBahasaConfig("Solusi Dual Model") %></h3>
                        <p class="fs-5 text-muted-custom"><%= Common.getBahasaConfig("Infrastruktur adaptif sesuai kebijakan keamanan data kesehatan fasilitas Anda:") %></p>
                        <ul class="feature-list mt-4 list-unstyled">
                            <li><i class="fas fa-check-circle text-success-custom me-3 fs-5"></i><strong>Cloud System:</strong> <%= Common.getBahasaConfig("Berbasis internet murni. Tidak butuh beli server, langsung pakai, bisa diakses dari mana saja.") %></li>
                            <li><i class="fas fa-check-circle text-success-custom me-3 fs-5"></i><strong>On-Premise:</strong> <%= Common.getBahasaConfig("Sistem dipasang di server lokal fasilitas kesehatan untuk privasi rekam medis ekstra.") %></li>
                            <li><i class="fas fa-check-circle text-success-custom me-3 fs-5"></i><strong>Hybrid:</strong> <%= Common.getBahasaConfig("Gabungan ketangguhan penyimpanan lokal & fleksibilitas akses internet.") %></li>
                        </ul>
                    </div>
                    <div class="col-md-6 ps-md-5">
                        <h3 class="mb-4 text-dark"><i class="fas fa-users-cog text-highlight me-3"></i><%= Common.getBahasaConfig("Multi-Portal Pengguna") %></h3>
                        <p class="fs-5 text-muted-custom"><%= Common.getBahasaConfig("Antarmuka spesifik berdasarkan hak akses (Role-Based Access):") %></p>
                        <ul class="feature-list mt-4 list-unstyled">
                            <li><i class="fas fa-user-md text-info me-3 fs-5"></i><strong>Dokter/Perawat:</strong> <%= Common.getBahasaConfig("Rekam medis, diagnosa, resep, dan riwayat pasien.") %></li>
                            <li><i class="fas fa-mortar-pestle text-info me-3 fs-5"></i><strong>Farmasis:</strong> <%= Common.getBahasaConfig("Verifikasi resep, racikan, dan monitoring stok/kadaluarsa.") %></li>
                            <li><i class="fas fa-cash-register text-info me-3 fs-5"></i><strong>Kasir:</strong> <%= Common.getBahasaConfig("Tagihan terpadu, pembayaran tunai/non-tunai/deposit.") %></li>
                            <li><i class="fas fa-user-tie text-info me-3 fs-5"></i><strong>Pimpinan:</strong> <%= Common.getBahasaConfig("Dashboard analitik dan laporan siap pakai.") %></li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-5 text-center text-highlight"><i class="fas fa-notes-medical me-3"></i><%= Common.getBahasaConfig("1. Registrasi & Rekam Medis") %></h2>
                <div class="row g-4">
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-calendar-check"></i> Registrasi Rawat Jalan</div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Pendaftaran, antrian otomatis, pemilihan poli dan dokter sesuai jadwal.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-bed"></i> Registrasi Rawat Inap</div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Admisi, pemilihan kamar/tempat tidur/kelas perawatan, deposit awal, visite dokter harian, hingga pemulangan.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-ambulance"></i> Registrasi UGD</div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Registrasi dan transaksi medis dilakukan bersamaan dalam satu langkah agar penanganan pasien tidak tertunda administrasi.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-stethoscope"></i> Rekam Medis & Diagnosa</div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Pencatatan keluhan, pemeriksaan, dan diagnosa dengan pengkodean standar ICD-10.") %></p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-5 text-center text-highlight"><i class="fas fa-mortar-pestle me-3"></i><%= Common.getBahasaConfig("2. Farmasi, Racikan & Manajemen Stok") %></h2>
                <div class="row g-4">
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-prescription"></i> Resep Digital</div><p class="module-desc"><%= Common.getBahasaConfig("Ditulis langsung oleh dokter saat pemeriksaan, diteruskan ke farmasi tanpa kertas.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-capsules"></i> Racikan / Kompounding</div><p class="module-desc"><%= Common.getBahasaConfig("Master komposisi baku memastikan mutu racikan tetap konsisten dan dapat ditelusuri ulang.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-warehouse"></i> Rantai Pasok End-to-End</div><p class="module-desc"><%= Common.getBahasaConfig("Saldo awal, permintaan pembelian, penerimaan barang, transfer antar gudang, hingga koreksi stok (opname).") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-thermometer-half"></i> Kadaluarsa & Batch (FEFO)</div><p class="module-desc"><%= Common.getBahasaConfig("Pencatatan per-batch dengan tanggal kadaluarsa, dashboard peringatan dini, dan prinsip First-Expired-First-Out.") %></p></div></div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-5 text-center text-highlight"><i class="fas fa-file-invoice-dollar me-3"></i><%= Common.getBahasaConfig("3. Transaksi, Billing & Dashboard Analitik") %></h2>
                <div class="row g-4">
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-receipt"></i> Tagihan Terpadu</div><p class="module-desc"><%= Common.getBahasaConfig("Biaya obat, tindakan medis, alat medis, dan paket layanan otomatis tergabung dalam satu tagihan pasien.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-cash-register"></i> Pembayaran Fleksibel</div><p class="module-desc"><%= Common.getBahasaConfig("Mendukung tunai, non-tunai, dan deposit, dengan struk/kwitansi tercetak otomatis.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-chart-pie"></i> 7 Dashboard Operasional</div><p class="module-desc"><%= Common.getBahasaConfig("Ringkasan pendaftaran, kunjungan, pendapatan, 10 diagnosa terbanyak, okupansi tempat tidur, dan kadaluarsa farmasi.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-file-alt"></i> 60+ Laporan Siap Pakai</div><p class="module-desc"><%= Common.getBahasaConfig("Termasuk format resmi RL5/RL18/RL21, laporan rawat inap, kasir, inventory, dan pengadaan.") %></p></div></div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-5 text-center text-highlight"><i class="fas fa-shield-halved me-3"></i><%= Common.getBahasaConfig("4. Keamanan Data & Jejak Audit") %></h2>
                <div class="row g-4">
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-shield-alt"></i> Jejak Audit Menyeluruh</div><p class="module-desc"><%= Common.getBahasaConfig("Setiap perubahan stok, resep, tarif, dan rekam medis tercatat: siapa, kapan, dan apa yang diubah.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-lock"></i> Hak Akses Berjenjang</div><p class="module-desc"><%= Common.getBahasaConfig("Dokter, perawat, farmasis, kasir, petugas gudang, dan admin memiliki akses sesuai peran.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-award"></i> Siap untuk Audit & Akreditasi</div><p class="module-desc"><%= Common.getBahasaConfig("Mendukung kebutuhan audit stok, opname, dan proses akreditasi fasilitas kesehatan.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-database"></i> Data Milik Fasilitas Sepenuhnya</div><p class="module-desc"><%= Common.getBahasaConfig("Seluruh data tersimpan aman dan menjadi milik penuh fasilitas kesehatan, bukan pihak ketiga.") %></p></div></div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-route"></i> <%= Common.getBahasaConfig("Roadmap Integrasi") %></span>
                    <h2 class="mt-4 mb-3 text-highlight"><%= Common.getBahasaConfig("Kesiapan Integrasi BPJS/JKN & SATUSEHAT Kemenkes") %></h2>
                    <p class="executive-lead"><%= Common.getBahasaConfig("Fondasi data untuk integrasi eksternal disiapkan secara bertahap dan aditif, tanpa mengganggu operasional yang sedang berjalan.") %></p>
                </div>
                <div class="timeline-modern mt-4">
                    <div class="timeline-step"><div class="day"><%= Common.getBahasaConfig("Fase 1") %></div><div class="icon"><i class="fas fa-database"></i></div><h4><%= Common.getBahasaConfig("Fondasi Data") %></h4><p class="text-muted-custom"><%= Common.getBahasaConfig("NIK, nomor kartu BPJS, dan data kepesertaan pasien — sedang dibangun.") %></p></div>
                    <div class="timeline-step"><div class="day"><%= Common.getBahasaConfig("Fase 2") %></div><div class="icon"><i class="fas fa-id-card"></i></div><h4><%= Common.getBahasaConfig("BPJS Eligibilitas") %></h4><p class="text-muted-custom"><%= Common.getBahasaConfig("Integrasi VClaim, PCare, dan penerbitan SEP otomatis.") %></p></div>
                    <div class="timeline-step"><div class="day"><%= Common.getBahasaConfig("Fase 3") %></div><div class="icon"><i class="fas fa-plug"></i></div><h4><%= Common.getBahasaConfig("SATUSEHAT") %></h4><p class="text-muted-custom"><%= Common.getBahasaConfig("Integrasi rekam medis elektronik nasional Kemenkes.") %></p></div>
                    <div class="timeline-step"><div class="day"><%= Common.getBahasaConfig("Fase 4") %></div><div class="icon"><i class="fas fa-file-invoice"></i></div><h4><%= Common.getBahasaConfig("Klaim INA-CBG") %></h4><p class="text-muted-custom"><%= Common.getBahasaConfig("Pengajuan dan pelacakan klaim otomatis ke BPJS.") %></p></div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-calendar-check"></i> <%= Common.getBahasaConfig("Metodologi Implementasi") %></span>
                    <h2 class="mt-4 mb-3 text-highlight"><%= Common.getBahasaConfig("Roadmap Implementasi Bertahap, Terukur, dan Minim Gangguan Operasional") %></h2>
                    <p class="executive-lead"><%= Common.getBahasaConfig("Implementasi dilakukan dengan pendekatan bertahap agar tenaga medis dapat beradaptasi, data lama dapat dimigrasikan secara tertib, dan layanan pasien tetap berjalan.") %></p>
                </div>
                <div class="timeline-modern mt-4">
                    <div class="timeline-step"><div class="day"><%= Common.getBahasaConfig("Tahap 1") %></div><div class="icon"><i class="fas fa-search"></i></div><h4><%= Common.getBahasaConfig("Asesmen & Pemetaan Alur Klinis") %></h4><p class="text-muted-custom"><%= Common.getBahasaConfig("Memetakan alur pendaftaran-pemeriksaan-farmasi-kasir, kebutuhan modul, dan prioritas implementasi.") %></p></div>
                    <div class="timeline-step"><div class="day"><%= Common.getBahasaConfig("Tahap 2") %></div><div class="icon"><i class="fas fa-cogs"></i></div><h4><%= Common.getBahasaConfig("Konfigurasi Sistem") %></h4><p class="text-muted-custom"><%= Common.getBahasaConfig("Menyiapkan master pasien, dokter, item obat, tarif, kelas perawatan, dan hak akses.") %></p></div>
                    <div class="timeline-step"><div class="day"><%= Common.getBahasaConfig("Tahap 3") %></div><div class="icon"><i class="fas fa-people-arrows"></i></div><h4><%= Common.getBahasaConfig("Migrasi & Pelatihan") %></h4><p class="text-muted-custom"><%= Common.getBahasaConfig("Import data, uji coba alur kerja, pelatihan dokter/perawat/farmasis/kasir.") %></p></div>
                    <div class="timeline-step"><div class="day"><%= Common.getBahasaConfig("Tahap 4") %></div><div class="icon"><i class="fas fa-rocket"></i></div><h4><%= Common.getBahasaConfig("Go-Live & Evaluasi") %></h4><p class="text-muted-custom"><%= Common.getBahasaConfig("Sistem digunakan secara operasional, dipantau, lalu dievaluasi untuk penyempurnaan berkelanjutan.") %></p></div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-2 text-center text-highlight"><%= Common.getBahasaConfig("Model Kerja Sama: Opsi 1") %></h2>
                <p class="text-center text-muted-custom mb-3 fs-5"><%= Common.getBahasaConfig("Beli Putus - Lisensi Permanen, Sekali Bayar di Muka") %></p>
                <div class="row justify-content-center">
                    <div class="col-lg-11">
                        <table class="table table-sm table-bordered align-middle bg-white shadow-sm" style="font-size: 0.92rem;">
                            <thead class="table-light">
                                <tr><th><%= Common.getBahasaConfig("Klasifikasi Fasilitas") %></th><th class="text-end"><%= Common.getBahasaConfig("Harga Beli Putus") %></th></tr>
                            </thead>
                            <tbody>
                                <tr><td><strong>Klinik</strong></td><td class="text-end fw-bold text-primary">Rp 100.000.000</td></tr>
                                <tr><td>Puskesmas Nonrawat Inap Dasar</td><td class="text-end">Rp 150.000.000</td></tr>
                                <tr><td>Puskesmas Nonrawat Inap Lengkap / 24 Jam</td><td class="text-end">Rp 225.000.000</td></tr>
                                <tr><td>Puskesmas Rawat Inap</td><td class="text-end">Rp 325.000.000</td></tr>
                                <tr><td>Puskesmas Rawat Inap Terpencil</td><td class="text-end">Rp 425.000.000</td></tr>
                                <tr><td>Puskesmas Sangat Terpencil / Offline-First</td><td class="text-end">Rp 500.000.000</td></tr>
                                <tr><td><strong>Rumah Sakit Pratama</strong></td><td class="text-end">Rp 250.000.000</td></tr>
                                <tr><td><strong>Rumah Sakit Kelas D</strong></td><td class="text-end">Rp 400.000.000</td></tr>
                                <tr><td><strong>Rumah Sakit Kelas C</strong></td><td class="text-end">Rp 750.000.000</td></tr>
                                <tr><td><strong>Rumah Sakit Kelas B</strong></td><td class="text-end">Rp 1.400.000.000</td></tr>
                                <tr><td><strong>Rumah Sakit Kelas A</strong></td><td class="text-end">Rp 2.750.000.000</td></tr>
                            </tbody>
                        </table>
                        <p class="text-muted-custom text-center small mb-0"><i class="fas fa-circle-info me-1"></i><%= Common.getBahasaConfig("Sudah termasuk lisensi seluruh modul, implementasi dasar, dan dukungan tahun pertama untuk satu lokasi. Infrastruktur, migrasi kompleks, integrasi alat medis, kustomisasi khusus, dan onsite dihitung terpisah.") %></p>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-2 text-center text-highlight"><%= Common.getBahasaConfig("Model Kerja Sama: Opsi 2") %></h2>
                <p class="text-center text-muted-custom mb-3 fs-5"><%= Common.getBahasaConfig("Langganan Bulanan - Tanpa Investasi Awal Besar") %></p>
                <div class="row justify-content-center">
                    <div class="col-lg-11">
                        <table class="table table-sm table-bordered align-middle bg-white shadow-sm" style="font-size: 0.92rem;">
                            <thead class="table-light">
                                <tr><th><%= Common.getBahasaConfig("Klasifikasi Fasilitas") %></th><th class="text-end"><%= Common.getBahasaConfig("Biaya / Bulan") %></th></tr>
                            </thead>
                            <tbody>
                                <tr><td><strong>Klinik</strong></td><td class="text-end fw-bold" style="color:#d97706;">Rp 2.500.000</td></tr>
                                <tr><td>Puskesmas Nonrawat Inap Dasar</td><td class="text-end">Rp 4.000.000</td></tr>
                                <tr><td>Puskesmas Nonrawat Inap Lengkap / 24 Jam</td><td class="text-end">Rp 5.500.000</td></tr>
                                <tr><td>Puskesmas Rawat Inap</td><td class="text-end">Rp 8.000.000</td></tr>
                                <tr><td>Puskesmas Rawat Inap Terpencil</td><td class="text-end">Rp 10.500.000</td></tr>
                                <tr><td>Puskesmas Sangat Terpencil / Offline-First</td><td class="text-end">Rp 12.500.000</td></tr>
                                <tr><td><strong>Rumah Sakit Pratama</strong></td><td class="text-end">Rp 6.500.000</td></tr>
                                <tr><td><strong>Rumah Sakit Kelas D</strong></td><td class="text-end">Rp 10.000.000</td></tr>
                                <tr><td><strong>Rumah Sakit Kelas C</strong></td><td class="text-end">Rp 20.000.000</td></tr>
                                <tr><td><strong>Rumah Sakit Kelas B</strong></td><td class="text-end">Rp 35.000.000</td></tr>
                                <tr><td><strong>Rumah Sakit Kelas A</strong></td><td class="text-end">Rp 70.000.000</td></tr>
                            </tbody>
                        </table>
                        <p class="text-muted-custom text-center small mb-0"><i class="fas fa-circle-info me-1"></i><%= Common.getBahasaConfig("Termasuk lisensi modul, hosting/cloud standar, backup berkala, pemeliharaan, pembaruan minor, dan dukungan jarak jauh untuk satu lokasi. Perangkat, onsite, integrasi alat medis, dan migrasi besar dihitung terpisah.") %></p>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-2 text-center text-highlight"><%= Common.getBahasaConfig("Model Kerja Sama: Opsi 3a") %></h2>
                <p class="text-center text-muted-custom mb-4 fs-5"><%= Common.getBahasaConfig("Fee Layanan Sistem - Skema Klinik (Tanpa Biaya di Muka)") %></p>
                <div class="row justify-content-center mt-2">
                    <div class="col-lg-10">
                        <div class="pricing-box" style="border-color: #16a34a;">
                            <div class="text-center mb-4">
                                <div class="price-badge fs-4 px-5 py-3 mb-2" style="background: rgba(22, 163, 74, 0.1); color: #16a34a; border-color: #16a34a;">0,5% + 0,25%</div>
                                <h3 class="text-dark fs-5"><%= Common.getBahasaConfig("Tindakan Medis + Obat/Farmasi, dari Pendapatan Bersih") %></h3>
                            </div>
                            <ul class="feature-list list-unstyled ps-4 text-start">
                                <li class="mb-3"><i class="fas fa-stethoscope text-success me-3 fs-4"></i><strong><%= Common.getBahasaConfig("Fee Tindakan Medis:") %></strong> <%= Common.getBahasaConfig("0,5% dari pendapatan bersih tindakan (setelah dikurangi jasa dokter, bahan medis habis pakai, diskon, dan refund).") %></li>
                                <li class="mb-3"><i class="fas fa-mortar-pestle text-success me-3 fs-4"></i><strong><%= Common.getBahasaConfig("Fee Obat/Farmasi:") %></strong> <%= Common.getBahasaConfig("0,25% dari penjualan bersih obat (setelah dikurangi diskon, retur, dan pajak).") %></li>
                                <li class="mb-3"><i class="fas fa-shield-halved text-success me-3 fs-4"></i><strong><%= Common.getBahasaConfig("Batas Aman:") %></strong> <%= Common.getBahasaConfig("minimum Rp 2.500.000, maksimum Rp 7.500.000 per bulan per lokasi klinik.") %></li>
                                <li class="mb-4"><i class="fas fa-hand-holding-dollar text-success me-3 fs-4"></i><strong><%= Common.getBahasaConfig("Alternatif Sederhana:") %></strong> <%= Common.getBahasaConfig("Rp 500 per registrasi kunjungan berhasil (batas minimum/maksimum sama).") %></li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-2 text-center text-highlight"><%= Common.getBahasaConfig("Model Kerja Sama: Opsi 3b") %></h2>
                <p class="text-center text-muted-custom mb-3 fs-5"><%= Common.getBahasaConfig("Fee Layanan Sistem - Skema Puskesmas & Rumah Sakit") %></p>
                <div class="row justify-content-center">
                    <div class="col-lg-11">
                        <table class="table table-sm table-bordered align-middle bg-white shadow-sm" style="font-size: 0.85rem;">
                            <thead class="table-light">
                                <tr><th><%= Common.getBahasaConfig("Klasifikasi") %></th><th><%= Common.getBahasaConfig("Minimum") %></th><th><%= Common.getBahasaConfig("Fee/Registrasi") %></th><th><%= Common.getBahasaConfig("Maksimum") %></th></tr>
                            </thead>
                            <tbody>
                                <tr><td>Puskesmas Nonrawat Inap</td><td>Rp 4.000.000</td><td>Rp 500</td><td>Rp 10.000.000</td></tr>
                                <tr><td>Puskesmas Nonrawat Inap Lengkap</td><td>Rp 5.500.000</td><td>Rp 750</td><td>Rp 15.000.000</td></tr>
                                <tr><td>Puskesmas Rawat Inap</td><td>Rp 8.000.000</td><td>Rp 1.000</td><td>Rp 20.000.000</td></tr>
                                <tr><td>Puskesmas Terpencil</td><td>Rp 10.500.000</td><td>Rp 1.250</td><td>Rp 25.000.000</td></tr>
                                <tr><td>Puskesmas Sangat Terpencil</td><td>Rp 12.500.000</td><td>Rp 1.500</td><td>Rp 30.000.000</td></tr>
                                <tr><td>RS Pratama</td><td>Rp 6.500.000</td><td>Rp 1.000</td><td>Rp 20.000.000</td></tr>
                                <tr><td>RS Kelas D</td><td>Rp 10.000.000</td><td>Rp 1.250</td><td>Rp 30.000.000</td></tr>
                                <tr><td>RS Kelas C</td><td>Rp 20.000.000</td><td>Rp 2.500 / gabungan</td><td>Rp 60.000.000</td></tr>
                                <tr><td>RS Kelas B</td><td colspan="2">Dasar Rp 20jt + Rp 2.000/registrasi</td><td>Rp 100.000.000</td></tr>
                                <tr><td>RS Kelas A</td><td colspan="2">Dasar Rp 40jt + Rp 3.500/registrasi</td><td>Rp 200.000.000</td></tr>
                            </tbody>
                        </table>
                        <div class="quote-panel mt-2 p-3 text-center">
                            <span class="small text-dark"><i class="fas fa-clipboard-question me-2 text-warning-custom"></i><strong><%= Common.getBahasaConfig("Satu Transaksi Berbayar") %></strong> = <%= Common.getBahasaConfig("satu registrasi kunjungan yang berhasil, bernomor kunjungan, dan sudah dilayani. Tidak dihitung: batal, percobaan, duplikasi, koreksi, data dummy. Rawat inap: satu kali saat admisi.") %></span>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-shield-heart"></i> <%= Common.getBahasaConfig("Prinsip Kepatuhan & Etika") %></span>
                    <h2 class="mt-4 mb-3 text-highlight"><%= Common.getBahasaConfig("Skema Fee Layanan Sistem Disusun secara Bertanggung Jawab") %></h2>
                </div>
                <div class="row g-4">
                    <div class="col-md-6">
                        <div class="decision-card">
                            <h4><i class="fas fa-handshake text-primary me-2"></i><%= Common.getBahasaConfig("Bukan Pungutan kepada Pasien") %></h4>
                            <p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Fee Layanan Sistem adalah hubungan bisnis (B2B) antara fasilitas kesehatan dan penyedia sistem, dibayar dari anggaran/pendapatan fasilitas — bukan pungutan otomatis kepada pasien, terutama peserta JKN/BPJS.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="decision-card">
                            <h4><i class="fas fa-user-doctor text-danger me-2"></i><%= Common.getBahasaConfig("Tidak Memengaruhi Keputusan Medis") %></h4>
                            <p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Besaran fee tidak boleh menjadi dasar penambahan tindakan, pemeriksaan, atau obat. Informasi fee hanya dapat diakses manajemen dan keuangan, tidak ditampilkan kepada tenaga medis.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="decision-card">
                            <h4><i class="fas fa-scale-balanced text-warning-custom me-2"></i><%= Common.getBahasaConfig("Selaras Regulasi Tarif JKN") %></h4>
                            <p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Skema disusun dengan memperhatikan ketentuan standar tarif pelayanan program Jaminan Kesehatan Nasional yang berlaku.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="decision-card">
                            <h4><i class="fas fa-lock text-success me-2"></i><%= Common.getBahasaConfig("Perlindungan Data Pasien") %></h4>
                            <p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Sesuai Undang-Undang Pelindungan Data Pribadi dan ketentuan rekam medis; fasilitas kesehatan sebagai pengendali data, penyedia sistem sebagai pemroses data.") %></p>
                        </div>
                    </div>
                </div>
                <div class="quote-panel mt-4 text-center">
                    <h5 class="mb-0 text-dark"><i class="fas fa-quote-left text-warning-custom me-2"></i><%= Common.getBahasaConfig("Seluruh nilai investasi, tarif langganan, dan fee pada slide ini bersifat ilustratif dan sepenuhnya terbuka untuk dinegosiasikan. Prioritas kami adalah kecepatan realisasi sistem bagi fasilitas kesehatan Anda.") %></h5>
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
                            <p class="text-muted-custom fs-6 mb-0"><%= Common.getBahasaConfig("Menjamin keandalan server, keamanan data rekam medis (enkripsi), serta pemeliharaan sistem agar berjalan lancar 24/7.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="info-card text-center p-4">
                            <i class="fas fa-chalkboard-teacher fa-3x text-success mb-4"></i>
                            <h4 class="text-highlight fw-bold mb-3"><%= Common.getBahasaConfig("Trainer & Implementator") %></h4>
                            <p class="text-muted-custom fs-6 mb-0"><%= Common.getBahasaConfig("Mendampingi migrasi data dan memberikan Pelatihan Gratis secara ONLINE maupun OFFLINE bagi tenaga medis.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="info-card text-center p-4">
                            <i class="fas fa-user-md fa-3x text-warning-custom mb-4"></i>
                            <h4 class="text-highlight fw-bold mb-3"><%= Common.getBahasaConfig("Konsultan Alur Klinis") %></h4>
                            <p class="text-muted-custom fs-6 mb-0"><%= Common.getBahasaConfig("Memastikan alur registrasi, rekam medis, farmasi, dan billing sesuai kebutuhan operasional fasilitas kesehatan.") %></p>
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
                    <div class="col-md-6"><div class="decision-card"><h4><i class="fas fa-tachometer-alt text-primary me-2"></i><%= Common.getBahasaConfig("Kecepatan Layanan") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Waktu registrasi, pemeriksaan, dispensing obat, dan pembayaran dapat dipantau melalui data sistem.") %></p></div></div>
                    <div class="col-md-6"><div class="decision-card"><h4><i class="fas fa-file-circle-check text-success me-2"></i><%= Common.getBahasaConfig("Kelengkapan Data") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Data pasien, rekam medis, stok obat, dan transaksi menjadi lebih lengkap serta mudah divalidasi.") %></p></div></div>
                    <div class="col-md-6"><div class="decision-card"><h4><i class="fas fa-coins text-warning me-2"></i><%= Common.getBahasaConfig("Kontrol Keuangan") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Tagihan, pembayaran, dan HPP dapat dimonitor lebih transparan oleh kasir dan pimpinan.") %></p></div></div>
                    <div class="col-md-6"><div class="decision-card"><h4><i class="fas fa-user-shield text-danger me-2"></i><%= Common.getBahasaConfig("Akuntabilitas Proses") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Alur persetujuan, perubahan data, dan histori layanan tersimpan untuk kebutuhan audit dan akreditasi.") %></p></div></div>
                </div>
                <div class="quote-panel mt-4 text-center"><strong><%= Common.getBahasaConfig("Dengan indikator yang jelas, transformasi digital tidak berhenti pada penggunaan aplikasi, tetapi menjadi proses peningkatan mutu layanan kesehatan yang dapat diukur.") %></strong></div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content text-center shadow-lg" style="background: var(--card-bg);">

                <div class="mb-4">
                    <i class="fas fa-rocket fa-4x text-primary mb-3" style="animation: pulse-primary 2s infinite; border-radius: 50%;"></i>
                    <h2 class="fw-bold text-dark" style="font-size: 2.2rem;"><%= Common.getBahasaConfig("Siap Digitalkan Fasilitas Kesehatan Anda?") %></h2>
                    <p class="fs-5 text-muted-custom mt-2"><%= Common.getBahasaConfig("Jadwalkan presentasi, validasi kebutuhan modul, dan mulai susun langkah transformasi digital layanan kesehatan Anda secara bertahap mulai hari ini.") %></p>
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
                            <i class="fas fa-globe text-highlight me-2"></i> apps.emedik.id
                        </div>
                    </div>
                <% } %>

                <div class="row justify-content-center mt-2">
                    <div class="col-lg-12">
                        <div class="p-4 bg-white rounded-4 shadow-sm border border-light d-flex flex-column align-items-center">

                            <h6 class="text-muted-custom mb-3 fw-bold text-uppercase" style="letter-spacing: 1px;"><i class="fas fa-folder-open me-2 text-warning"></i><%= Common.getBahasaConfig("Dokumen Penawaran Modul Kesehatan (eMedic)") %></h6>

                            <div class="d-flex flex-wrap justify-content-center gap-3 mb-4 w-100">
                                <a href="<%=Common.ROOT%>/penawaran<%=appendParamsKesehatan%>" target="_blank" class="btn btn-outline-danger fw-bold px-3 py-3 rounded-4 shadow-sm flex-grow-1 doc-btn">
                                    <i class="far fa-envelope-open fa-2x mb-2 d-block"></i> <%= Common.getBahasaConfig("Surat Penawaran") %>
                                </a>
                                <a href="<%=Common.ROOT%>/proposal<%=appendParamsKesehatan%>" target="_blank" class="btn btn-outline-primary fw-bold px-3 py-3 rounded-4 shadow-sm flex-grow-1 doc-btn">
                                    <i class="far fa-file-alt fa-2x mb-2 d-block"></i> <%= Common.getBahasaConfig("Lihat Proposal") %>
                                </a>
                                <a href="<%=Common.ROOT%>/pks<%=appendParamsKesehatan%>" target="_blank" class="btn btn-outline-success fw-bold px-3 py-3 rounded-4 shadow-sm flex-grow-1 doc-btn">
                                    <i class="far fa-handshake fa-2x mb-2 d-block"></i> <%= Common.getBahasaConfig("Draft PKS") %>
                                </a>
                            </div>

                            <div class="p-3 mb-3 rounded-4 border border-warning-subtle bg-light text-center w-75">
                                <div class="small text-muted-custom fw-bold text-uppercase mb-1" style="letter-spacing: 1px;"><i class="fas fa-key me-1"></i><%= Common.getBahasaConfig("Akun Demo") %></div>
                                <div class="text-dark"><%= Common.getBahasaConfig("Username") %>: <strong>demo</strong> &nbsp;|&nbsp; <%= Common.getBahasaConfig("Password") %>: <strong>demo123</strong></div>
                            </div>

                            <a href="<%=targetDemoUrl%>" target="_blank" class="btn btn-primary fw-bold px-5 py-3 rounded-pill text-uppercase shadow-lg fs-5 w-75 pulse-button" style="letter-spacing: 2px;">
                                <%= Common.getBahasaConfig("Coba Demo eMedic Sekarang") %> <i class="fas fa-arrow-right ms-2"></i>
                            </a>

                        </div>
                    </div>
                </div>

            </div>
        </div>

        <div class="controls">
            <button class="btn-nav" id="btnPrev" title="<%= Common.getBahasaConfig("Sebelumnya") %>"><i class="fas fa-chevron-left"></i></button>
            <span class="slide-counter"><span id="currentSlideNum">1</span> / <span id="totalSlideNum">18</span></span>
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
