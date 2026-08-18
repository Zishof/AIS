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
    <title><%= Common.getBahasaConfig("Presentasi Enterprise Education") %> | <%=judul%></title>
    
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
        .presentation-container { position: relative; width: 100vw; height: 100vh; background: linear-gradient(135deg, rgba(255, 255, 255, 0.85) 0%, rgba(241, 245, 249, 0.95) 100%), url('<%=Common.ROOT%>/img/pmb_bg.jpg') center/cover; display: flex; justify-content: center; align-items: center; }
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
        
        /* Hover Zoom & Modal Info */
        .hover-zoom { transition: transform 0.3s ease, box-shadow 0.3s ease; }
        .hover-zoom:hover { transform: scale(1.03); box-shadow: 0 12px 25px rgba(0,0,0,0.2) !important; }
        .info-card { background: var(--card-inner-bg); border: 1px solid rgba(0,0,0,0.05); border-radius: 16px; padding: 2rem; transition: transform 0.3s, box-shadow 0.3s; box-shadow: 0 10px 20px rgba(0,0,0,0.05); }
        .info-card:hover { transform: translateY(-5px); background: #ffffff; border-color: var(--primary-color); box-shadow: 0 15px 30px rgba(0,0,0,0.1); }

        /* Efek Tombol Dokumen (Slide Terakhir) yang Elegan & Eye Catching */
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
        /* Efek Kilauan (Shine) saat Hover pada Tombol */
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


        /* =====================================================================
           ENHANCEMENT PRESENTASI - Modern Executive PowerPoint-like Experience
           ===================================================================== */
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
            content: "Enterprise Education";
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
    <div class="presentation-brand-chip"><i class="fas fa-layer-group me-2"></i> Enterprise Education</div>
    <div class="kbd-hint"><kbd>←</kbd> <kbd>→</kbd> Navigasi &nbsp; <kbd>F</kbd> Fullscreen</div>

    <img src="<%=logo_PerguruanTinggi%>" class="logo-header" alt="Logo">

    <div class="presentation-container" id="presentationArea">
        
        <div class="slide active">
            <div class="text-center slide-content" style="overflow:hidden;">
                <span class="section-eyebrow mb-4"><i class="fas fa-bolt"></i> <%= Common.getBahasaConfig("Executive Presentation Deck") %></span>
                <i class="fas fa-graduation-cap fa-5x mb-4 text-warning-custom d-block"></i>
                <h1 class="display-3 mb-3 hero-gradient-title"><%= Common.getBahasaConfig("Transformasi Digital Pendidikan") %></h1>
                <h2 class="fw-bold text-warning-custom mb-4 display-6"><%= Common.getBahasaConfig("Enterprise Education (eCampus, eSchool, ePesantren)") %></h2>
                <hr class="w-25 mx-auto border-info opacity-50 mb-4 border-2">
                <p class="fs-4 text-muted-custom mb-3"><%= Common.getBahasaConfig("Solusi All-in-One untuk Manajemen Akademik, Administrasi, dan Keuangan") %></p>
                <p class="executive-lead mb-4"><%= Common.getBahasaConfig("Dirancang untuk membantu institusi pendidikan mengintegrasikan layanan akademik, keuangan, SDM, aset, pembelajaran digital, pelaporan nasional, dan komunikasi orang tua dalam satu ekosistem yang rapi, terukur, serta siap berkembang.") %></p>
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
                    <h2 class="mt-4 mb-3 hero-gradient-title"><%= Common.getBahasaConfig("Satu Platform untuk Menggerakkan Seluruh Siklus Pendidikan") %></h2>
                    <p class="executive-lead"><%= Common.getBahasaConfig("Enterprise Education bukan sekadar aplikasi administrasi. Sistem ini menjadi fondasi tata kelola institusi: menyatukan data, mempercepat layanan, mengurangi pekerjaan manual, dan menyediakan informasi yang siap dipakai pimpinan untuk mengambil keputusan strategis.") %></p>
                </div>
                <div class="row g-4 mt-3">
                    <div class="col-md-4"><div class="impact-card"><div class="impact-number">01</div><h4><%= Common.getBahasaConfig("Integrasi Operasional") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Akademik, pembayaran, SDM, aset, persuratan, LMS, dan pelaporan nasional berjalan dalam alur kerja yang saling terhubung.") %></p></div></div>
                    <div class="col-md-4"><div class="impact-card"><div class="impact-number">02</div><h4><%= Common.getBahasaConfig("Layanan Lebih Cepat") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Mahasiswa, siswa, orang tua, dosen/guru, dan staf mendapatkan layanan mandiri yang mengurangi antrean dan mempercepat verifikasi.") %></p></div></div>
                    <div class="col-md-4"><div class="impact-card"><div class="impact-number">03</div><h4><%= Common.getBahasaConfig("Kontrol Manajemen") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Pimpinan memperoleh dashboard, audit trail, indikator kinerja, dan rekap real-time untuk mengawal mutu dan keuangan institusi.") %></p></div></div>
                </div>
                <div class="quote-panel mt-4 text-center">
                    <h5 class="mb-1 text-dark"><i class="fas fa-quote-left text-warning-custom me-2"></i><%= Common.getBahasaConfig("Tujuan akhirnya sederhana: institusi menjadi lebih rapi, responsif, transparan, dan siap bersaing di era pendidikan digital.") %></h5>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-3"><span class="section-eyebrow"><i class="fas fa-triangle-exclamation"></i> <%= Common.getBahasaConfig("Masalah yang Perlu Diselesaikan") %></span></div>
                <h2 class="mb-5 text-center"><i class="fas fa-exclamation-circle me-3 text-warning-custom"></i><%= Common.getBahasaConfig("Tantangan Institusi Saat Ini") %></h2>
                <div class="row g-4">
                    <div class="col-md-6">
                        <div class="module-card" style="border-left-color: var(--danger-color);">
                            <div class="module-title"><i class="fas fa-folder-open text-danger"></i> <%= Common.getBahasaConfig("Birokrasi Manual & Lambat") %></div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Penumpukan dokumen fisik, antrean panjang di Tata Usaha, dan proses layanan administrasi yang tidak efisien membuang waktu produktif staf dan mahasiswa.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card" style="border-left-color: var(--danger-color);">
                            <div class="module-title"><i class="fas fa-server text-danger"></i> <%= Common.getBahasaConfig("Silo Data (Terpisah)") %></div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Sistem akademik, keuangan, dan SDM yang berdiri sendiri-sendiri memicu terjadinya entri data ganda (double-entry) dan rentan terhadap inkonsistensi informasi.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card" style="border-left-color: var(--danger-color);">
                            <div class="module-title"><i class="fas fa-file-export text-danger"></i> <%= Common.getBahasaConfig("Beban Pelaporan Nasional") %></div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Kesulitan teknis dan besarnya beban kerja operator saat harus menyusun, memvalidasi, dan mensinkronisasikan data massal ke pusat (PDDIKTI atau EMIS).") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card" style="border-left-color: var(--danger-color);">
                            <div class="module-title"><i class="fas fa-eye-slash text-danger"></i> <%= Common.getBahasaConfig("Kurangnya Transparansi") %></div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Orang tua/wali murid kesulitan memantau rincian tagihan finansial yang harus dibayar serta progres capaian akademik anak mereka secara mandiri dan real-time.") %></p>
                        </div>
                    </div>
                </div>
            </div>
        </div>


        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-route"></i> <%= Common.getBahasaConfig("Transformasi yang Dihasilkan") %></span>
                    <h2 class="mt-4 mb-3 text-highlight"><%= Common.getBahasaConfig("Dari Sistem Terpisah Menjadi Ekosistem Terpadu") %></h2>
                    <p class="executive-lead"><%= Common.getBahasaConfig("Dengan pendekatan enterprise, setiap unit tidak lagi bekerja sendiri-sendiri. Data yang sama dapat dipakai oleh banyak fungsi sehingga proses lebih ringkas, laporan lebih cepat, dan validasi menjadi lebih kuat.") %></p>
                </div>
                <div class="before-after mt-4">
                    <div class="row g-4 align-items-stretch">
                        <div class="col-md-6 pe-md-5">
                            <h4 class="text-danger mb-3"><i class="fas fa-times-circle me-2"></i><%= Common.getBahasaConfig("Sebelum Terintegrasi") %></h4>
                            <ul class="feature-list list-unstyled mb-0">
                                <li><i class="fas fa-minus-circle text-danger me-2"></i><%= Common.getBahasaConfig("Banyak data dicatat ulang di unit berbeda.") %></li>
                                <li><i class="fas fa-minus-circle text-danger me-2"></i><%= Common.getBahasaConfig("Laporan pimpinan lambat karena perlu rekap manual.") %></li>
                                <li><i class="fas fa-minus-circle text-danger me-2"></i><%= Common.getBahasaConfig("Pembayaran, akademik, dan administrasi sering tidak sinkron.") %></li>
                                <li><i class="fas fa-minus-circle text-danger me-2"></i><%= Common.getBahasaConfig("Sulit menelusuri riwayat perubahan data dan persetujuan.") %></li>
                            </ul>
                        </div>
                        <div class="col-md-6 ps-md-5">
                            <h4 class="text-success-custom mb-3"><i class="fas fa-check-circle me-2"></i><%= Common.getBahasaConfig("Sesudah Menggunakan Enterprise Education") %></h4>
                            <ul class="feature-list list-unstyled mb-0">
                                <li><i class="fas fa-check-circle text-success me-2"></i><%= Common.getBahasaConfig("Satu sumber data untuk banyak kebutuhan layanan.") %></li>
                                <li><i class="fas fa-check-circle text-success me-2"></i><%= Common.getBahasaConfig("Dashboard real-time untuk akademik, keuangan, SDM, dan mutu.") %></li>
                                <li><i class="fas fa-check-circle text-success me-2"></i><%= Common.getBahasaConfig("Pembayaran dapat otomatis memperbarui tagihan dan jurnal keuangan.") %></li>
                                <li><i class="fas fa-check-circle text-success me-2"></i><%= Common.getBahasaConfig("Audit trail membantu menjaga akuntabilitas proses digital.") %></li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content text-center">
                <h2 class="mb-4 text-highlight"><i class="fas fa-network-wired me-3"></i><%= Common.getBahasaConfig("Solusi: Enterprise Education") %></h2>
                <p class="fs-4 mb-5 text-muted-custom"><%= Common.getBahasaConfig("Satu Ekosistem Terpadu untuk Seluruh Kebutuhan Institusi Pendidikan") %></p>
                <div class="row justify-content-center mt-4">
                    <div class="col-lg-10">
                        <div class="d-flex justify-content-around align-items-center flex-wrap gap-4">
                            <div class="p-4 module-card text-center" style="flex: 1; min-width: 250px;">
                                <i class="fas fa-university fa-4x text-primary mb-4"></i>
                                <h3 class="text-dark">eCampus</h3>
                                <p class="text-muted-custom mb-0 mt-2">Untuk Perguruan Tinggi, Institut, Akademi, & Politeknik</p>
                            </div>
                            <i class="fas fa-link fa-2x text-muted-custom opacity-50"></i>
                            <div class="p-4 module-card text-center" style="flex: 1; min-width: 250px; border-left-color: var(--success-color);">
                                <i class="fas fa-school fa-4x text-success mb-4"></i>
                                <h3 class="text-dark">eSchool</h3>
                                <p class="text-muted-custom mb-0 mt-2">Untuk Pendidikan Dasar dan Menengah (SD, SMP, SMA/K)</p>
                            </div>
                            <i class="fas fa-link fa-2x text-muted-custom opacity-50"></i>
                            <div class="p-4 module-card text-center" style="flex: 1; min-width: 250px; border-left-color: var(--warning-color);">
                                <i class="fas fa-mosque fa-4x text-warning-custom mb-4"></i>
                                <h3 class="text-dark">ePesantren</h3>
                                <p class="text-muted-custom mb-0 mt-2">Untuk Asrama dan Kurikulum Keagamaan Pondok Pesantren</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>


        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-star"></i> <%= Common.getBahasaConfig("Nilai Strategis untuk Institusi") %></span>
                    <h2 class="mt-4 mb-3 text-highlight"><%= Common.getBahasaConfig("Manfaat yang Langsung Terasa oleh Pimpinan, Unit Kerja, dan Pengguna") %></h2>
                </div>
                <div class="value-strip">
                    <div><h5><i class="fas fa-stopwatch text-primary me-2"></i><%= Common.getBahasaConfig("Efisiensi Waktu") %></h5><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Proses layanan yang sebelumnya berulang dan manual dapat dialihkan menjadi alur digital yang terdokumentasi.") %></p></div>
                    <div><h5><i class="fas fa-user-check text-success me-2"></i><%= Common.getBahasaConfig("Kepuasan Pengguna") %></h5><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Mahasiswa, siswa, wali, dosen/guru, dan staf memperoleh akses layanan mandiri yang lebih cepat dan jelas.") %></p></div>
                    <div><h5><i class="fas fa-database text-warning me-2"></i><%= Common.getBahasaConfig("Data Lebih Tertib") %></h5><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Validasi, histori, lampiran, dan status proses tersimpan rapi sehingga mudah ditelusuri kembali.") %></p></div>
                    <div><h5><i class="fas fa-chart-pie text-info me-2"></i><%= Common.getBahasaConfig("Laporan Siap Pakai") %></h5><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Rekap operasional dan manajerial tersedia untuk mendukung rapat pimpinan, audit, dan evaluasi mutu.") %></p></div>
                </div>
                <div class="row g-4 mt-4">
                    <div class="col-md-6"><div class="impact-card"><h4><i class="fas fa-building-columns text-primary me-2"></i><%= Common.getBahasaConfig("Citra Institusi Lebih Profesional") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Portal digital, layanan pembayaran modern, antar jemput terintegrasi, dashboard pimpinan, serta dokumen otomatis memberi kesan institusi yang tertata dan siap melayani generasi digital.") %></p></div></div>
                    <div class="col-md-6"><div class="impact-card"><h4><i class="fas fa-seedling text-success-custom me-2"></i><%= Common.getBahasaConfig("Siap Bertumbuh Jangka Panjang") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Modul dapat diterapkan bertahap sesuai prioritas, mulai dari kebutuhan paling mendesak hingga pengembangan ekosistem penuh.") %></p></div></div>
                </div>
            </div>
        </div>


        <!-- ENHANCE_MODUL_ANTAR_JEMPUT_START -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-shuttle-van"></i> <%= Common.getBahasaConfig("Modul Baru") %></span>
                    <h2 class="mt-4 mb-3 text-highlight"><%= Common.getBahasaConfig("Antar Jemput Terintegrasi untuk Keamanan dan Layanan Operasional") %></h2>
                    <p class="fs-5 text-muted-custom mx-auto" style="max-width: 980px;">
                        <%= Common.getBahasaConfig("Modul ini membantu sekolah, kampus, pesantren, dan yayasan mengelola kendaraan, rute, jadwal, kartu penjemput, antrian gerbang, panggilan kelas, dan bukti serah terima dalam satu alur digital yang tertib.") %>
                    </p>
                </div>

                <div class="row g-4 mt-3">
                    <div class="col-md-3">
                        <div class="impact-card h-100">
                            <h4><i class="fas fa-car-side text-primary me-2"></i><%= Common.getBahasaConfig("Master Operasional") %></h4>
                            <p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Kendaraan, sopir, kenek, rute, jadwal, dan kartu penjemput dikelola sebagai data operasional yang terhubung dengan aset dan pegawai.") %></p>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="impact-card h-100">
                            <h4><i class="fas fa-id-card text-success me-2"></i><%= Common.getBahasaConfig("Validasi Gerbang") %></h4>
                            <p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Satpam melakukan tap kartu atau scan barcode untuk mengecek status kartu, masa berlaku, dan jadwal aktif peserta.") %></p>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="impact-card h-100">
                            <h4><i class="fas fa-tv text-warning me-2"></i><%= Common.getBahasaConfig("Monitor & Soundbox") %></h4>
                            <p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Nomor antrian tampil di monitor gerbang dan panggilan dapat dikirim ke soundbox kelas agar peserta segera disiapkan.") %></p>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="impact-card h-100">
                            <h4><i class="fas fa-shield-halved text-info me-2"></i><%= Common.getBahasaConfig("Audit Serah Terima") %></h4>
                            <p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Histori scan, panggilan, keluar kelas, dan konfirmasi serah terima tersimpan untuk kontrol keselamatan dan penelusuran komplain.") %></p>
                        </div>
                    </div>
                </div>

                <div class="pricing-box mt-4">
                    <h3 class="text-dark mb-3"><i class="fas fa-route me-2 text-primary"></i><%= Common.getBahasaConfig("Alur Singkat Layanan Antar Jemput") %></h3>
                    <div class="row g-3 text-start">
                        <div class="col-md-4"><div class="info-card h-100"><strong>1. Persiapan Jadwal</strong><p class="text-muted-custom mb-0">Admin menyiapkan rute, kendaraan, petugas, jadwal, dan manifest peserta.</p></div></div>
                        <div class="col-md-4"><div class="info-card h-100"><strong>2. Scan di Gerbang</strong><p class="text-muted-custom mb-0">Petugas memvalidasi kartu/barcode penjemput dan sistem membuat transaksi penjemputan.</p></div></div>
                        <div class="col-md-4"><div class="info-card h-100"><strong>3. Panggilan & Serah Terima</strong><p class="text-muted-custom mb-0">Kelas menerima panggilan, peserta keluar, lalu petugas mengonfirmasi serah terima.</p></div></div>
                    </div>
                </div>
            </div>
        </div>
        <!-- ENHANCE_MODUL_ANTAR_JEMPUT_END -->

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-5 text-center text-highlight"><%= Common.getBahasaConfig("Fleksibilitas Infrastruktur & Akses") %></h2>
                <div class="row g-5 mt-2">
                    <div class="col-md-6 border-end border-secondary border-opacity-25 pe-md-5">
                        <h3 class="mb-4 text-dark"><i class="fas fa-cloud text-highlight me-3"></i><%= Common.getBahasaConfig("Solusi Dual Model") %></h3>
                        <p class="fs-5 text-muted-custom"><%= Common.getBahasaConfig("Infrastruktur adaptif sesuai kebijakan keamanan institusi Anda:") %></p>
                        <ul class="feature-list mt-4 list-unstyled">
                            <li><i class="fas fa-check-circle text-success-custom me-3 fs-5"></i><strong>Cloud System:</strong> <%= Common.getBahasaConfig("Berbasis internet murni. Tidak butuh beli server, langsung pakai, bisa diakses dari mana saja.") %></li>
                            <li><i class="fas fa-check-circle text-success-custom me-3 fs-5"></i><strong>On-Premise:</strong> <%= Common.getBahasaConfig("Sistem dipasang di Server Lokal kampus/sekolah untuk privasi dan keamanan ekstra.") %></li>
                            <li><i class="fas fa-check-circle text-success-custom me-3 fs-5"></i><strong>Hybrid:</strong> <%= Common.getBahasaConfig("Gabungan ketangguhan penyimpanan lokal & fleksibilitas akses internet.") %></li>
                        </ul>
                    </div>
                    <div class="col-md-6 ps-md-5">
                        <h3 class="mb-4 text-dark"><i class="fas fa-users-cog text-highlight me-3"></i><%= Common.getBahasaConfig("Multi-Portal Pengguna") %></h3>
                        <p class="fs-5 text-muted-custom"><%= Common.getBahasaConfig("Antarmuka spesifik berdasarkan hak akses (Role-Based Access):") %></p>
                        <ul class="feature-list mt-4 list-unstyled">
                            <li><i class="fas fa-user-shield text-info me-3 fs-5"></i><strong>Admin/Yayasan:</strong> <%= Common.getBahasaConfig("Dashboard pusat kendali, laporan neraca, & analitik performa.") %></li>
                            <li><i class="fas fa-chalkboard-teacher text-info me-3 fs-5"></i><strong>Dosen/Guru:</strong> <%= Common.getBahasaConfig("Isi nilai, absensi, beban kinerja (BKD), dan ruang kelas LMS.") %></li>
                            <li><i class="fas fa-user-graduate text-info me-3 fs-5"></i><strong>Siswa/Mahasiswa:</strong> <%= Common.getBahasaConfig("Isi KRS mandiri, pantau KHS, jadwal, tugas, & ujian online.") %></li>
                            <li><i class="fas fa-user-tie text-info me-3 fs-5"></i><strong>Orang Tua:</strong> <%= Common.getBahasaConfig("Pantau progres akademik anak & notifikasi tagihan bulanan.") %></li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-5 text-center text-highlight"><i class="fas fa-laptop-code me-3"></i><%= Common.getBahasaConfig("Arsitektur Teknologi & Antarmuka") %></h2>
                <div class="row g-4">
                    <div class="col-md-6">
                        <div class="info-card h-100">
                            <h4 class="text-primary mb-3 border-bottom pb-2"><i class="fas fa-paint-brush me-2"></i> User Interface (UI) Fleksibel</h4>
                            <p class="text-muted-custom mb-3"><%= Common.getBahasaConfig("Sistem dirancang dengan fleksibilitas tinggi, memiliki dua opsi tampilan (tema) yang bisa disesuaikan dengan preferensi kenyamanan pengguna institusi Anda:") %></p>
                            <ul class="feature-list list-unstyled">
                                <li><i class="fas fa-check text-success-custom me-2"></i><strong>Tipe Klasik:</strong> Menggunakan <i>ZK Framework</i>. Tampilan ringkas, sangat stabil, hemat <i>bandwidth</i>, dan dioptimalkan penuh untuk responsivitas Mobile & Desktop.</li>
                                <li><i class="fas fa-check text-success-custom me-2"></i><strong>Tipe Modern:</strong> Menggunakan fondasi <i>Bootstrap, CSS, & Font Awesome</i>. Menawarkan <i>tools</i> UI/UX terbaru, grafik yang lebih interaktif, dan fungsi pelaporan canggih seperti ekspor data langsung ke Excel via Javascript.</li>
                            </ul>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="info-card h-100 border-primary border-opacity-50">
                            <h4 class="text-primary mb-3 border-bottom pb-2"><i class="fas fa-shield-alt me-2"></i> Keamanan & Core Engine</h4>
                            <p class="text-muted-custom mb-3"><%= Common.getBahasaConfig("Berbasis Enterprise-Grade untuk menjamin performa saat menangani ribuan akses bersamaan (seperti saat KRS-an atau Ujian Serentak):") %></p>
                            <ul class="feature-list list-unstyled">
                                <li><i class="fas fa-server text-info me-2"></i><strong>Java & Spring Boot:</strong> Dapur pacu utama yang handal dan *scalable*. Terhubung dengan basis data <strong>PostgreSQL</strong>.</li>
                                <li><i class="fas fa-key text-warning me-2"></i><strong>Single Sign-On (SSO):</strong> Integrasi *login* lintas aplikasi tanpa perlu mengingat banyak password.</li>
                                <li><i class="fas fa-search text-danger me-2"></i><strong>Audit Trail:</strong> Fitur pelacakan riwayat aktivitas pengguna untuk mendeteksi perubahan data yang tidak sah.</li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-5 text-center text-highlight"><i class="fas fa-book-reader me-3"></i><%= Common.getBahasaConfig("1. Tata Kelola Akademik & Pembelajaran") %></h2>
                <div class="row g-4">
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-user-plus"></i> Penerimaan Baru (PMB/PPDB)</div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Sistem automasi pendaftaran daring, unggah berkas, seleksi ujian CBT, pengumuman, hingga integrasi pembayaran biaya pendaftaran secara otomatis.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-university"></i> Akademik Anti-Bentrok</div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Algoritma penjadwalan cerdas yang mencegah bentrok waktu antar dosen dan ruangan. Mengawal proses KRS-an mahasiswa hingga penerbitan transkrip akhir.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-laptop-code"></i> E-Learning & LMS Terpadu</div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Integrasi ruang kelas virtual. Mendukung penugasan, distribusi materi via Google Drive, pemutaran video YouTube, hingga Ujian Online (CBT) anti-curang.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-bullseye"></i> Kurikulum OBE & Ganda</div>
                            <p class="module-desc"><%= Common.getBahasaConfig("Implementasi penuh Outcome Based Education (OBE): hierarki Sub-CPMK → CPMK → CPL (4 kategori SN-Dikti: Sikap, Pengetahuan, Keterampilan Umum & Khusus) → Profil Lulusan. Tiga siklus monitoring: Loop 1 (CPMK/semester), Loop 2 (CPL/tahun), Loop 3 (PL/3–5 tahun via Tracer Study). Dasbor PIKOBE skor 0–100 (Bronze/Silver/Gold/Platinum) + rekomendasi CQI otomatis. Ditambah kurikulum ganda (Nasional & Kitab Kuning) untuk pesantren.") %></p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-5 text-center text-highlight"><i class="fas fa-user-graduate me-3"></i><%= Common.getBahasaConfig("2. Administrasi Mahasiswa/Siswa & Mutu") %></h2>
                <div class="row g-4">
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-trophy"></i> Pencatatan Prestasi</div><p class="module-desc"><%= Common.getBahasaConfig("Katalog terstruktur untuk mendokumentasikan raihan prestasi akademik maupun non-akademik siswa sebagai rujukan evaluasi institusional berkelanjutan.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-file-signature"></i> Sistem Pengajuan Birokrasi</div><p class="module-desc"><%= Common.getBahasaConfig("Digitalisasi permohonan judul tugas akhir, penjadwalan sidang, cuti akademik, hingga pengajuan beasiswa dengan alur persetujuan (approval) berjenjang.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-users-cog"></i> Manajemen Magang (PKL)</div><p class="module-desc"><%= Common.getBahasaConfig("Sentralisasi operasional PKL/Magang industri. Meliputi alokasi kelompok, pemantauan logbook bimbingan daring, hingga rekapitulasi evaluasi nilai akhir.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-graduation-cap"></i> Seleksi Kelulusan / Wisuda</div><p class="module-desc"><%= Common.getBahasaConfig("Meniadakan antrean loket yudisium. Pendaftaran, cek silang bebas pustaka/keuangan, hingga validasi berkas wisuda dilakukan sepenuhnya secara daring.") %></p></div></div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-5 text-center text-highlight"><i class="fas fa-microscope me-3"></i><%= Common.getBahasaConfig("3. Riset, Penjaminan Mutu & Pustaka") %></h2>
                <div class="row g-4">
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-sync-alt"></i> Integrasi Feeder & EMIS</div><p class="module-desc"><%= Common.getBahasaConfig("Sinkronisasi otomatis (API) 1-click ke pusat (PDDIKTI/EMIS). Menghilangkan beban double-entry data mahasiswa, absensi, dan nilai oleh operator kampus.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-check-double"></i> Penjaminan Mutu (SPMI)</div><p class="module-desc"><%= Common.getBahasaConfig("Instrumen pengawalan standar mutu internal (audit, evaluasi kinerja departemen) dan integrasi penyusunan data acuan borang akreditasi nasional.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-flask"></i> Penelitian & Karya Ilmiah</div><p class="module-desc"><%= Common.getBahasaConfig("Modul tata kelola proposal, pencairan hibah PPM (Simlitabmas), penerbitan E-Journal (OJS), serta Repositori digital karya intelektual sivitas akademika.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-book-reader"></i> Perpustakaan (eLibrary)</div><p class="module-desc"><%= Common.getBahasaConfig("Katalogisasi aset perpustakaan (OPAC), sirkulasi peminjaman barcode, perhitungan otomatis denda keterlambatan, dan peringatan jatuh tempo via sistem.") %></p></div></div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-5 text-center text-highlight"><i class="fas fa-chart-pie me-3"></i><%= Common.getBahasaConfig("4. Tata Kelola Keuangan & Aset") %></h2>
                <div class="row g-4">
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-file-invoice-dollar"></i> Finance & Penagihan</div><p class="module-desc"><%= Common.getBahasaConfig("Pembuatan tagihan SPP/UKT otomatis, akomodasi skema cicilan, dan manajemen kontrol piutang peserta didik yang terhubung ke portal orang tua.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-calculator"></i> Akuntansi Institusi</div><p class="module-desc"><%= Common.getBahasaConfig("Sistem Buku Besar (General Ledger). Transaksi pembayaran langsung terposting menjadi jurnal. Mendukung pembuatan neraca & laba rugi standar akuntansi.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-wallet"></i> Perencanaan Anggaran</div><p class="module-desc"><%= Common.getBahasaConfig("Modul penyusunan estimasi RKAT tahunan, plotting alokasi dana strategis, pengajuan pencairan kas kecil, dan pengawasan ketat kurva serapan anggaran.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-boxes"></i> Manajemen Aset & Pengadaan</div><p class="module-desc"><%= Common.getBahasaConfig("Inventarisasi barang fisik menggunakan Barcode/RFID, perhitungan depresiasi penyusutan aset, jadwal pemeliharaan, serta alur pengadaan (e-procurement).") %></p></div></div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-5 text-center text-highlight"><i class="fas fa-users me-3"></i><%= Common.getBahasaConfig("5. Manajemen SDM & Penilaian Kinerja") %></h2>
                <div class="row g-4">
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-id-badge"></i> Data Pegawai (HRIS)</div><p class="module-desc"><%= Common.getBahasaConfig("Sentralisasi manajemen arsip dosen dan pegawai. Merekam riwayat jabatan, kepangkatan, mutasi, hingga pengelolaan jatah izin dan cuti tahunan staf.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-money-check-alt"></i> Penggajian (Payroll) Otomatis</div><p class="module-desc"><%= Common.getBahasaConfig("Kalkulasi honorarium dan tunjangan yang terintegrasi penuh dengan data rekap absensi untuk memastikan distribusi gaji presisi dan mencetak slip gaji digital.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-chalkboard-teacher"></i> Beban Kinerja Dosen (BKD)</div><p class="module-desc"><%= Common.getBahasaConfig("Fasilitas khusus bagi perguruan tinggi untuk mengawasi, mengevaluasi, dan merekapitulasi laporan pemenuhan kewajiban Tridharma staf pengajar akademik.") %></p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-chart-line"></i> Indikator Kinerja (KPI)</div><p class="module-desc"><%= Common.getBahasaConfig("Penilaian objektif kinerja (KPI), evaluasi umpan balik 360 derajat, pelaporan buku harian (logbook) aktivitas kerja, dan referensi kebijakan insentif.") %></p></div></div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-5 text-center text-highlight"><i class="fas fa-cogs me-3"></i><%= Common.getBahasaConfig("6. Operasional & Smart Campus") %></h2>
                <div class="row g-4">
                    <div class="col-md-4"><div class="module-card"><div class="module-title"><i class="fas fa-fingerprint"></i> Presensi Cerdas</div><p class="module-desc"><%= Common.getBahasaConfig("Absensi menggunakan Face Recognition, Kartu RFID, dan Aplikasi Mobile berbasis limitasi radius lokasi GPS (Geotagging).") %></p></div></div>
                    <div class="col-md-4"><div class="module-card"><div class="module-title"><i class="fab fa-whatsapp"></i> WhatsApp Gateway</div><p class="module-desc"><%= Common.getBahasaConfig("Mesin otomatis (blast) untuk mengingatkan tagihan SPP dan mengirim resi pembayaran langsung ke nomor HP wali murid.") %></p></div></div>
                    <div class="col-md-4"><div class="module-card"><div class="module-title"><i class="fas fa-envelope-open-text"></i> Sistem Persuratan</div><p class="module-desc"><%= Common.getBahasaConfig("Digitalisasi dokumentasi arsip surat masuk/keluar, disposisi birokrasi pimpinan, dan pembuatan templat surat otomatis.") %></p></div></div>
                    <div class="col-md-4"><div class="module-card"><div class="module-title"><i class="fas fa-store"></i> eKantin Digital</div><p class="module-desc"><%= Common.getBahasaConfig("Mengubah area kantin menjadi zona cashless. Siswa bertransaksi makanan menggunakan saldo pada kartu pintar RFID/NFC.") %></p></div></div>
                    <div class="col-md-4"><div class="module-card"><div class="module-title"><i class="fas fa-hand-holding-usd"></i> Koperasi & Unit Usaha</div><p class="module-desc"><%= Common.getBahasaConfig("Dilengkapi Sistem Kasir toko (POS), rekam transaksi simpan pinjam anggota, dan kalkulasi otomatis pembagian SHU.") %></p></div></div>
                    <div class="col-md-4"><div class="module-card"><div class="module-title"><i class="fas fa-video"></i> Integrasi V-Meeting</div><p class="module-desc"><%= Common.getBahasaConfig("Kelas Online terintegrasi secara seamless dengan Google Meet, Zoom, Jitsi, serta impor jadwal otomatis ke Google Calendar.") %></p></div></div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <div class="row align-items-center g-5">
                    <div class="col-lg-3">
                        <div class="d-flex flex-column gap-3 mb-2">
                            <img src="<%=Common.ROOT%>/img/anjungan1.jpg" alt="ATM Model 1" class="img-fluid rounded-4 shadow border border-white border-2 hover-zoom" style="height: 180px; width: 100%; object-fit: cover; cursor: pointer;" onclick="showImageModal(this.src)" onerror="this.style.display='none'">
                            <img src="<%=Common.ROOT%>/img/anjungan2.jpg" alt="ATM Model 2" class="img-fluid rounded-4 shadow border border-white border-2 hover-zoom" style="height: 180px; width: 100%; object-fit: cover; cursor: pointer;" onclick="showImageModal(this.src)" onerror="this.style.display='none'">
                            <img src="<%=Common.ROOT%>/img/anjungan3.jpg" alt="ATM Model 3" class="img-fluid rounded-4 shadow border border-white border-2 hover-zoom" style="height: 180px; width: 100%; object-fit: cover; cursor: pointer;" onclick="showImageModal(this.src)" onerror="this.style.display='none'">
                        </div>
                        <div class="text-center small text-muted fst-italic"><i class="fas fa-search-plus me-1"></i> Klik untuk memperbesar</div>
                    </div>
                    
                    <div class="col-lg-9">
                        <h2 class="mb-2 text-highlight"><i class="fas fa-desktop me-3"></i><%= Common.getBahasaConfig("Anjungan Transaksi Mandiri (ATM)") %></h2>
                        <h4 class="mb-3 text-dark"><%= Common.getBahasaConfig("Layanan Mandiri 24/7 Tanpa Antrean") %></h4>
                        
                        <div class="bg-light p-3 rounded-4 border border-secondary border-opacity-25 shadow-sm">
                            <h6 class="fw-bold text-primary mb-2"><i class="fas fa-microchip me-2"></i>Spesifikasi Hardware:</h6>
                            <div class="row">
                                <div class="col-md-6">
                                    <ul class="feature-list list-unstyled mb-0" style="font-size: 0.85rem;">
                                        <li><i class="fas fa-check-circle text-success me-2"></i> 24 Inch Touchscreen</li>
                                        <li><i class="fas fa-check-circle text-success me-2"></i> Mini PC Industrial Intel i5</li>
                                        <li><i class="fas fa-check-circle text-success me-2"></i> RAM 8G DDR3 & SSD 256 Gb</li>
                                    </ul>
                                </div>
                                <div class="col-md-6">
                                    <ul class="feature-list list-unstyled mb-0" style="font-size: 0.85rem;">
                                        <li><i class="fas fa-check-circle text-success me-2"></i> Printer: Epson L120 (A4)</li>
                                        <li><i class="fas fa-check-circle text-success me-2"></i> Jaringan: Ethernet & WiFi</li>
                                        <li><i class="fas fa-check-circle text-success me-2"></i> Casing 2mm sheet metal</li>
                                    </ul>
                                </div>
                            </div>
                        </div>
                        
                        <div class="row mt-3 g-3">
                            <div class="col-md-6">
                                <div class="bg-white p-2 rounded-3 shadow-sm border border-primary border-opacity-25 text-center h-100 d-flex flex-column justify-content-center">
                                    <div class="fw-bold text-dark text-uppercase small mb-1">Harga Unit:</div>
                                    <div class="text-primary fw-bold fs-5">Rp 37.500.000 (NTS)</div>
                                    <div class="text-primary fw-bold fs-5">Rp 42.500.000 (TS)</div>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="alert alert-warning py-2 px-3 mb-0 shadow-sm h-100" style="font-size: 0.8rem; border-left: 4px solid var(--warning-color);">
                                    <i class="fas fa-info-circle text-warning me-1"></i> <strong>Penting:</strong> Pengadaan Hardware bersifat opsional & di luar sewa Software. Belum termasuk Pajak & Ongkir luar Jabodetabek. <strong>Syarat: DP Rp 30 Juta di awal, proses manufaktur kurang lebih 1 bulan.</strong>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <div class="row align-items-center g-5">
                    <div class="col-lg-4">
                        <div class="d-flex flex-column gap-3 mb-2">
                            <img src="<%=Common.ROOT%>/img/pos1.jpg" alt="POS Model 1" class="img-fluid rounded-4 shadow border border-white border-2 hover-zoom" style="height: 220px; width: 100%; object-fit: cover; cursor: pointer;" onclick="showImageModal(this.src)" onerror="this.src='https://via.placeholder.com/400x220?text=POS+Image+1'">
                            <img src="<%=Common.ROOT%>/img/pos2.jpg" alt="POS Model 2" class="img-fluid rounded-4 shadow border border-white border-2 hover-zoom" style="height: 220px; width: 100%; object-fit: cover; cursor: pointer;" onclick="showImageModal(this.src)" onerror="this.src='https://via.placeholder.com/400x220?text=POS+Image+2'">
                        </div>
                        <div class="text-center small text-muted fst-italic"><i class="fas fa-search-plus me-1"></i> Klik untuk memperbesar</div>
                    </div>
                    
                    <div class="col-lg-8">
                        <h2 class="mb-2 text-highlight"><i class="fas fa-cash-register me-3"></i><%= Common.getBahasaConfig("Mesin Kasir ANFO POS DUAL") %></h2>
                        <h4 class="mb-3 text-dark"><%= Common.getBahasaConfig("Layar Ganda untuk Koperasi & Kantin Digital") %></h4>
                        <p class="text-muted-custom mb-3" style="text-align:justify;">Perangkat Point of Sale (POS) premium yang dilengkapi dua layar interaktif untuk petugas kasir dan pelanggan. Memastikan transaksi yang transparan dan sangat profesional di setiap unit usaha institusi Anda.</p>
                        
                        <div class="bg-light p-3 rounded-4 border border-secondary border-opacity-25 shadow-sm">
                            <h6 class="fw-bold text-primary mb-2"><i class="fas fa-list-ul me-2"></i>Spesifikasi Hardware POS DUAL:</h6>
                            <div class="row">
                                <div class="col-md-6">
                                    <ul class="feature-list list-unstyled mb-0" style="font-size: 0.85rem;">
                                        <li><i class="fas fa-desktop text-success me-2"></i> <strong>Layar Kasir:</strong> 15.6" Touchscreen</li>
                                        <li><i class="fas fa-tv text-success me-2"></i> <strong>Layar Pelanggan:</strong> 10" Non-Touchscreen</li>
                                        <li><i class="fas fa-microchip text-success me-2"></i> <strong>CPU:</strong> Intel Core i3-10110U (up to 4.1GHz)</li>
                                        <li><i class="fas fa-memory text-success me-2"></i> <strong>Memory:</strong> 16GB DDR4 Dual Channel</li>
                                        <li><i class="fas fa-hdd text-success me-2"></i> <strong>Storage:</strong> SSD 512GB</li>
                                    </ul>
                                </div>
                                <div class="col-md-6">
                                    <ul class="feature-list list-unstyled mb-0" style="font-size: 0.85rem;">
                                        <li><i class="fas fa-print text-success me-2"></i> <strong>Thermal Printer:</strong> 80mm, Speed 90mm/s (USB+BT)</li>
                                        <li><i class="fas fa-box text-success me-2"></i> <strong>Cash Drawer:</strong> VSC RJ-11 (49x48x16 cm)</li>
                                        <li><i class="fas fa-coins text-success me-2"></i> <strong>Slot Laci:</strong> 6 Uang Kertas & 4 Logam</li>
                                        <li><i class="fas fa-bolt text-success me-2"></i> <strong>Daya/Grafis:</strong> TDP 15W / Intel UHD Graphics</li>
                                    </ul>
                                </div>
                            </div>
                        </div>
                        
                        <div class="row mt-3">
                            <div class="col-12">
                                <div class="bg-white p-3 rounded-3 shadow-sm border border-danger border-opacity-25 d-flex justify-content-between align-items-center">
                                    <div class="text-start">
                                        <div class="fw-bold text-dark text-uppercase small mb-1" style="letter-spacing: 1px;">Harga Pembelian Paket:</div>
                                        <div class="text-muted small"><i class="fas fa-info-circle text-warning me-1"></i> Bersifat opsional & di luar skema sewa Software.</div>
                                    </div>
                                    <div class="text-end">
                                        <div class="text-primary fw-bold display-6 mb-0">Rp 30.000.000</div>
                                        <div class="text-muted small fw-bold">/ Paket Lengkap</div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content text-center">
                <h2 class="mb-4 text-highlight"><i class="fas fa-money-check-alt me-3"></i><%= Common.getBahasaConfig("Infrastruktur Pembayaran Digital") %></h2>
                <p class="fs-4 text-muted-custom mb-5"><%= Common.getBahasaConfig("Pembayaran SPP/UKT yang Aman, Instan, dan Tervalidasi Otomatis (Auto-Sync)") %></p>
                <div class="d-flex flex-wrap justify-content-center gap-4 mb-5 mt-5">
                    <span class="bg-white text-dark border border-light px-4 py-3 rounded-pill fw-bold fs-5 shadow-sm"><i class="fas fa-university text-primary me-2"></i>Virtual Account Bank</span>
                    <span class="bg-white text-dark border border-light px-4 py-3 rounded-pill fw-bold fs-5 shadow-sm"><i class="fas fa-qrcode text-info me-2"></i>QRIS & E-Wallet</span>
                    <span class="bg-white text-dark border border-light px-4 py-3 rounded-pill fw-bold fs-5 shadow-sm"><i class="fas fa-store text-danger me-2"></i>Alfamart / Indomaret</span>
                    <span class="bg-white text-dark border border-light px-4 py-3 rounded-pill fw-bold fs-5 shadow-sm"><i class="fas fa-credit-card text-secondary me-2"></i>Kartu Kredit</span>
                </div>
                <div class="p-5 mt-5 rounded-4 module-card border-info" style="max-width: 900px; margin: 0 auto; background: rgba(2, 132, 199, 0.05);">
                    <h3 class="text-highlight mb-3"><i class="fas fa-check-double me-3"></i><%= Common.getBahasaConfig("Lupakan Verifikasi Manual!") %></h3>
                    <p class="text-dark mt-3 mb-0 fs-5"><%= Common.getBahasaConfig("Berkat sistem Host-to-Host, setiap pembayaran yang dilakukan wali murid akan langsung merubah status tagihan menjadi 'LUNAS' di aplikasi dan jurnal Keuangan, tanpa perlu verifikasi kasir.") %></p>
                </div>
            </div>
        </div>


        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-calendar-check"></i> <%= Common.getBahasaConfig("Metodologi Implementasi") %></span>
                    <h2 class="mt-4 mb-3 text-highlight"><%= Common.getBahasaConfig("Roadmap Implementasi Bertahap, Terukur, dan Minim Gangguan Operasional") %></h2>
                    <p class="executive-lead"><%= Common.getBahasaConfig("Implementasi dilakukan dengan pendekatan bertahap agar tim internal dapat beradaptasi, data lama dapat dimigrasikan secara tertib, dan layanan utama tetap berjalan.") %></p>
                </div>
                <div class="timeline-modern mt-4">
                    <div class="timeline-step"><div class="day"><%= Common.getBahasaConfig("Tahap 1") %></div><div class="icon"><i class="fas fa-search"></i></div><h4><%= Common.getBahasaConfig("Asesmen & Pemetaan") %></h4><p class="text-muted-custom"><%= Common.getBahasaConfig("Memetakan proses bisnis, struktur unit, kebutuhan modul, data awal, serta prioritas implementasi.") %></p></div>
                    <div class="timeline-step"><div class="day"><%= Common.getBahasaConfig("Tahap 2") %></div><div class="icon"><i class="fas fa-cogs"></i></div><h4><%= Common.getBahasaConfig("Konfigurasi Sistem") %></h4><p class="text-muted-custom"><%= Common.getBahasaConfig("Menyiapkan parameter akademik, keuangan, hak akses, template dokumen, kanal pembayaran, dan identitas institusi.") %></p></div>
                    <div class="timeline-step"><div class="day"><%= Common.getBahasaConfig("Tahap 3") %></div><div class="icon"><i class="fas fa-people-arrows"></i></div><h4><%= Common.getBahasaConfig("Migrasi & Pelatihan") %></h4><p class="text-muted-custom"><%= Common.getBahasaConfig("Melakukan import data, uji coba alur kerja, pelatihan operator, dan pendampingan pengguna kunci.") %></p></div>
                    <div class="timeline-step"><div class="day"><%= Common.getBahasaConfig("Tahap 4") %></div><div class="icon"><i class="fas fa-rocket"></i></div><h4><%= Common.getBahasaConfig("Go-Live & Evaluasi") %></h4><p class="text-muted-custom"><%= Common.getBahasaConfig("Sistem digunakan secara operasional, dipantau melalui helpdesk, lalu dievaluasi untuk penyempurnaan berkelanjutan.") %></p></div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-3 text-center text-highlight"><%= Common.getBahasaConfig("Pembiayaan Sistem / Software: Opsi 1") %></h2>
                <p class="text-center text-muted-custom mb-5 fs-4"><%= Common.getBahasaConfig("Skema Investasi Berbasis Transaksi (Zero Investment)") %></p>
                
                <div class="row justify-content-center mt-4">
                    <div class="col-lg-10">
                        <div class="pricing-box">
                            <div class="text-center mb-4">
                                <div class="price-badge fs-3 px-5 py-3 mb-3"><%= Common.getBahasaConfig("Rp 0,- Biaya Sistem Institusi") %></div>
                                <h3 class="text-dark"><%= Common.getBahasaConfig("Per Transaksi Pembayaran") %></h3>
                            </div>
                            
                            <ul class="feature-list list-unstyled ps-4 text-start">
                                <li class="mb-3"><i class="fas fa-check-circle text-success-custom me-3 fs-4"></i><strong><%= Common.getBahasaConfig("Syarat:") %></strong> <%= Common.getBahasaConfig("Minimal 2.000 Mahasiswa/Siswa aktif dan <strong>wajib menggunakan pembayaran tiap bulan</strong>. Jika perguruan tinggi memiliki sifat pembayaran UKT yang hanya 1x per semester, maka <strong>tidak memenuhi syarat</strong> untuk biaya per transaksi ini.") %></li>
                                <li class="mb-3"><i class="fas fa-check-circle text-success-custom me-3 fs-4"></i><strong><%= Common.getBahasaConfig("Biaya Admin Transaksi:") %></strong> <%= Common.getBahasaConfig("Rp 7.000 / transaksi (Untuk semua channel kecuali QRIS 1%).") %></li>
                                <li class="mb-4"><i class="fas fa-check-circle text-success-custom me-3 fs-4"></i><strong><%= Common.getBahasaConfig("Beban Biaya:") %></strong> <%= Common.getBahasaConfig("Admin dibebankan kepada pembayar (Orang Tua / Mahasiswa). Institusi tidak keluar biaya lisensi/server bulanan sama sekali.") %></li>
                            </ul>
                            
                            <div class="p-4 mt-4 bg-white rounded-4 border border-info border-opacity-50 shadow-sm text-start">
                                <h5 class="mb-2 text-highlight fw-bold"><i class="fas fa-info-circle me-2"></i><%= Common.getBahasaConfig("Simulasi Contoh:") %></h5>
                                <p class="mb-0 text-muted-custom fs-5"><%= Common.getBahasaConfig("Jika tagihan SPP adalah Rp 1.000.000, maka orang tua akan membayar total <strong class='text-dark'>Rp 1.007.000</strong>. (Rp 7.000 otomatis dialokasikan untuk Payment Gateway & Admin Sistem).") %></p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content">
                <h2 class="mb-3 text-center text-highlight"><%= Common.getBahasaConfig("Pembiayaan Sistem / Software: Opsi 2") %></h2>
                <p class="text-center text-muted-custom mb-5 fs-4"><%= Common.getBahasaConfig("Skema Investasi Berbasis Pengguna (Anggaran Pasti)") %></p>
                
                <div class="row justify-content-center mt-4">
                    <div class="col-lg-10">
                        <div class="pricing-box" style="border-color: #d97706;">
                            <div class="text-center mb-5">
                                <div class="price-badge fs-3 px-5 py-3 mb-4" style="background: rgba(217, 119, 6, 0.1); color: #d97706; border-color: #d97706;"><%= Common.getBahasaConfig("Biaya Fix per Pengguna") %></div>
                                <h3 class="text-dark"><%= Common.getBahasaConfig("Per Siswa / Mahasiswa Aktif") %></h3>
                            </div>
                            <ul class="feature-list list-unstyled ps-4 text-start">
                                <li class="mb-3"><i class="fas fa-check-circle text-warning-custom me-3 fs-4"></i><strong><%= Common.getBahasaConfig("Sekolah (eSchool/ePesantren):") %></strong> <%= Common.getBahasaConfig("Rp 2.000 / siswa / bulan.") %></li>
                                <li class="mb-3"><i class="fas fa-check-circle text-warning-custom me-3 fs-4"></i><strong><%= Common.getBahasaConfig("Perguruan Tinggi (eCampus):") %></strong> <%= Common.getBahasaConfig("Rp 4.000 / mahasiswa / bulan.") %></li>
                                <li class="mb-3"><i class="fas fa-check-circle text-warning-custom me-3 fs-4"></i><strong><%= Common.getBahasaConfig("Syarat Minimum:") %></strong> <%= Common.getBahasaConfig("Dihitung minimal 1.000 pengguna. Jika jumlah aktual di bawah 1.000, tagihan tetap dibulatkan flat untuk 1.000 pengguna.") %></li>
                                <li class="mb-4"><i class="fas fa-handshake text-warning-custom me-3 fs-4"></i><strong><%= Common.getBahasaConfig("Negosiasi Terbuka:") %></strong> <%= Common.getBahasaConfig("Harga di atas merupakan penawaran awal. Kami sangat terbuka untuk bernegosiasi agar sistem ini dapat segera memberikan manfaat bagi institusi Anda.") %></li>
                            </ul>
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
                            <p class="text-muted-custom fs-6 mb-0"><%= Common.getBahasaConfig("Menjamin keandalan server, keamanan data (enkripsi), serta pemeliharaan sistem agar senantiasa berjalan lancar 24/7 tanpa henti.") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="info-card text-center p-4">
                            <i class="fas fa-chalkboard-teacher fa-3x text-success mb-4"></i>
                            <h4 class="text-highlight fw-bold mb-3"><%= Common.getBahasaConfig("Trainer & Implementator") %></h4>
                            <p class="text-muted-custom fs-6 mb-0"><%= Common.getBahasaConfig("Mendampingi proses migrasi data dan memberikan Pelatihan (Training) Gratis secara ONLINE maupun OFFLINE (biaya akomodasi ditanggung institusi).") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="info-card text-center p-4">
                            <i class="fas fa-user-graduate fa-3x text-warning-custom mb-4"></i>
                            <h4 class="text-highlight fw-bold mb-3"><%= Common.getBahasaConfig("Konsultan Akademik & Regulasi") %></h4>
                            <p class="text-muted-custom fs-6 mb-0"><%= Common.getBahasaConfig("Selalu memastikan alur kerja aplikasi mematuhi dan rutin beradaptasi dengan regulasi terbaru dari Kemdikbud (PDDIKTI) atau Kemenag (EMIS).") %></p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="info-card text-center p-4">
                            <i class="fas fa-headset fa-3x text-danger mb-4"></i>
                            <h4 class="text-highlight fw-bold mb-3"><%= Common.getBahasaConfig("Layanan Bantuan (Helpdesk)") %></h4>
                            <p class="text-muted-custom fs-6 mb-0"><%= Common.getBahasaConfig("Tim dukungan teknis yang komunikatif, siap sedia dihubungi untuk menyelesaikan setiap kendala operasional yang dialami pengguna sehari-hari.") %></p>
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
                    <div class="col-md-6"><div class="decision-card"><h4><i class="fas fa-tachometer-alt text-primary me-2"></i><%= Common.getBahasaConfig("Kecepatan Layanan") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Waktu layanan administrasi, verifikasi pembayaran, pendaftaran, pengajuan surat, dan akses informasi dapat dipantau melalui data sistem.") %></p></div></div>
                    <div class="col-md-6"><div class="decision-card"><h4><i class="fas fa-file-circle-check text-success me-2"></i><%= Common.getBahasaConfig("Kelengkapan Data") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Data peserta didik, pegawai, kurikulum, tagihan, aset, arsip, dan transaksi menjadi lebih lengkap serta mudah divalidasi.") %></p></div></div>
                    <div class="col-md-6"><div class="decision-card"><h4><i class="fas fa-coins text-warning me-2"></i><%= Common.getBahasaConfig("Kontrol Keuangan") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Piutang, pembayaran, jurnal, kas, anggaran, dan rekonsiliasi dapat dimonitor lebih transparan oleh unit terkait dan pimpinan.") %></p></div></div>
                    <div class="col-md-6"><div class="decision-card"><h4><i class="fas fa-user-shield text-danger me-2"></i><%= Common.getBahasaConfig("Akuntabilitas Proses") %></h4><p class="text-muted-custom mb-0"><%= Common.getBahasaConfig("Alur persetujuan, perubahan data, aktivitas pengguna, dan histori layanan tersimpan untuk kebutuhan audit serta pengawasan internal.") %></p></div></div>
                </div>
                <div class="quote-panel mt-4 text-center"><strong><%= Common.getBahasaConfig("Dengan indikator yang jelas, transformasi digital tidak berhenti pada penggunaan aplikasi, tetapi menjadi proses peningkatan tata kelola yang dapat diukur.") %></strong></div>
            </div>
        </div>

        <div class="slide">
            <div class="slide-content text-center shadow-lg" style="background: var(--card-bg);">
                
                <div class="mb-4">
                    <i class="fas fa-rocket fa-4x text-primary mb-3" style="animation: pulse-primary 2s infinite; border-radius: 50%;"></i>
                    <h2 class="fw-bold text-dark" style="font-size: 2.2rem;"><%= Common.getBahasaConfig("Siap Bertransformasi?") %></h2>
                    <p class="fs-5 text-muted-custom mt-2"><%= Common.getBahasaConfig("Jangan tertinggal. Jadwalkan demo sistem, validasi kebutuhan utama, dan mulai susun langkah transformasi digital institusi Anda secara bertahap mulai hari ini.") %></p>
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
                            
                            <h6 class="text-muted-custom mb-3 fw-bold text-uppercase" style="letter-spacing: 1px;"><i class="fas fa-folder-open me-2 text-warning"></i><%= Common.getBahasaConfig("Dokumen Penawaran Eksekutif") %></h6>
                            
                            <div class="d-flex flex-wrap justify-content-center gap-3 mb-4 w-100">
                                <a href="<%=Common.ROOT%>/penawaran<%=appendParams%>" target="_blank" class="btn btn-outline-danger fw-bold px-3 py-3 rounded-4 shadow-sm flex-grow-1 doc-btn">
                                    <i class="far fa-envelope-open fa-2x mb-2 d-block"></i> <%= Common.getBahasaConfig("Surat Penawaran") %>
                                </a>
                                <a href="<%=Common.ROOT%>/proposal<%=appendParams%>" target="_blank" class="btn btn-outline-primary fw-bold px-3 py-3 rounded-4 shadow-sm flex-grow-1 doc-btn">
                                    <i class="far fa-file-alt fa-2x mb-2 d-block"></i> <%= Common.getBahasaConfig("Lihat Proposal") %>
                                </a>
                                <a href="<%=Common.ROOT%>/pks<%=appendParams%>" target="_blank" class="btn btn-outline-success fw-bold px-3 py-3 rounded-4 shadow-sm flex-grow-1 doc-btn">
                                    <i class="far fa-handshake fa-2x mb-2 d-block"></i> <%= Common.getBahasaConfig("Draft PKS") %>
                                </a>
                            </div>
                            
                            <a href="<%=targetDemoUrl%>" target="_blank" class="btn btn-primary fw-bold px-5 py-3 rounded-pill text-uppercase shadow-lg fs-5 w-75 pulse-button" style="letter-spacing: 2px;">
                                <%= Common.getBahasaConfig("Lanjutkan Demo Sistem") %> <i class="fas fa-arrow-right ms-2"></i>
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

    <div class="modal fade" id="imageModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered modal-xl">
        <div class="modal-content bg-transparent border-0">
          <div class="modal-header border-0 pb-0 justify-content-end">
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close" style="filter: invert(1) grayscale(100%) brightness(200%);"></button>
          </div>
          <div class="modal-body text-center p-0">
            <img id="modalExpandedImage" src="" class="img-fluid rounded-4 shadow-lg" alt="Expanded Hardware" style="max-height: 85vh; width: auto;">
          </div>
        </div>
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

        // Fungsi untuk menampilkan gambar ke dalam modal popup
        function showImageModal(imageSrc) {
            document.getElementById('modalExpandedImage').src = imageSrc;
            var imageModal = new bootstrap.Modal(document.getElementById('imageModal'));
            imageModal.show();
        }
    </script>
    <jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp" />
</body>
</html>