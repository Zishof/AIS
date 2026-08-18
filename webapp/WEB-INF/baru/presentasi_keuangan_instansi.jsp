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
    <title><%= Common.getBahasaConfig("Presentasi Keuangan Instansi & Yayasan") %> | <%=judul%></title>
    
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
            content: "Keuangan Instansi";
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
    <div class="presentation-brand-chip"><i class="fas fa-landmark me-2"></i> Keuangan Instansi</div>
    <div class="kbd-hint"><kbd>←</kbd> <kbd>→</kbd> Navigasi &nbsp; <kbd>F</kbd> Fullscreen</div>

    <img src="<%=logo_PerguruanTinggi%>" class="logo-header" alt="Logo">

    <div class="presentation-container" id="presentationArea">

        <!-- ============ 1 COVER ============ -->
        <div class="slide active">
            <div class="text-center slide-content" style="overflow:hidden;">
                <span class="section-eyebrow mb-4"><i class="fas fa-landmark"></i> Keuangan Instansi &amp; Yayasan</span>
                <h1 class="display-4 mb-3 hero-gradient-title">Dari Perencanaan Anggaran<br>Sampai Tutup Buku</h1>
                <h2 class="fw-bold text-warning-custom mb-4 h3">Budgeting &middot; Realisasi &middot; Pengadaan &middot; Aset &middot; Payroll &middot; Jurnal &middot; Laporan</h2>
                <p class="executive-lead mb-4">Satu sistem keuangan terpadu untuk yayasan &amp; instansi: anggaran menjadi pagar belanja,
                    setiap transaksi terkendali otorisasi berjenjang, dan seluruhnya otomatis terbukukan ke jurnal,
                    laporan keuangan, hingga closing &mdash; untuk <strong><%=judul%></strong>.</p>
                <div class="d-flex flex-wrap justify-content-center gap-2 mt-2">
                    <span class="metric-chip"><strong>Plan</strong> Anggaran &amp; Realisasi</span>
                    <span class="metric-chip"><strong>Procure</strong> PR &rarr; PO &rarr; BAST</span>
                    <span class="metric-chip"><strong>Pay</strong> Kas, Uang Muka, Payroll</span>
                    <span class="metric-chip"><strong>Report</strong> Jurnal &rarr; Closing</span>
                </div>
            </div>
        </div>

        <!-- ============ 2 PETA SIKLUS ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-compass"></i> Ringkasan Eksekutif</span>
                    <h2 class="mt-4 mb-2 hero-gradient-title">Satu Siklus Tata Kelola Keuangan yang Utuh</h2>
                </div>
                <p class="executive-lead text-center mb-4">Mulai dari <strong>perencanaan anggaran</strong>, dieksekusi melalui
                    <strong>pengadaan, kas, dan penggajian</strong>, dikelola pada <strong>aset &amp; persediaan</strong>, lalu
                    seluruh transaksi mengalir otomatis ke <strong>jurnal, laporan keuangan, dan tutup buku</strong> &mdash; tanpa input ganda.</p>
                <div class="row g-3">
                    <div class="col-md-3 col-6"><div class="impact-card h-100"><span class="impact-number">1</span><h6 class="fw-bold mb-1">Perencanaan</h6><p class="module-desc">Anggaran (RAB) &amp; realisasi sebagai pagu kendali belanja.</p></div></div>
                    <div class="col-md-3 col-6"><div class="impact-card h-100"><span class="impact-number">2</span><h6 class="fw-bold mb-1">Eksekusi</h6><p class="module-desc">Pengadaan, kas besar/kecil, uang muka, LPJ, payroll.</p></div></div>
                    <div class="col-md-3 col-6"><div class="impact-card h-100"><span class="impact-number">3</span><h6 class="fw-bold mb-1">Aset &amp; Stok</h6><p class="module-desc">Persediaan, kantin, manajemen aset &amp; penyusutan.</p></div></div>
                    <div class="col-md-3 col-6"><div class="impact-card h-100"><span class="impact-number">4</span><h6 class="fw-bold mb-1">Pelaporan</h6><p class="module-desc">Posting jurnal, laporan keuangan, closing periode.</p></div></div>
                </div>
            </div>
        </div>

        <!-- ============ 3 PERENCANAAN ANGGARAN ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-sitemap"></i> Tahap 1 &mdash; Perencanaan</span>
                    <h2 class="mt-4 mb-2">Penyusunan Anggaran (RAB / RKAKL)</h2>
                </div>
                <div class="row g-4">
                    <div class="col-md-7">
                        <div class="info-card h-100">
                            <h5 class="fw-bold mb-3"><i class="fas fa-folder-tree text-highlight me-2"></i>Anggaran Berbasis Pohon</h5>
                            <ul class="feature-list mb-0">
                                <li><i class="fas fa-check text-success-custom me-2"></i>Struktur <strong>pohon kegiatan &rarr; sub-kegiatan &rarr; rincian belanja</strong> yang rapi dan dapat ditelusuri.</li>
                                <li><i class="fas fa-check text-success-custom me-2"></i>Dipisah per <strong>sumber dana, unit/divisi, dan tahun anggaran</strong>.</li>
                                <li><i class="fas fa-check text-success-custom me-2"></i>Impor cepat dari <strong>RKAKL/DBF</strong> serta <strong>revisi anggaran</strong> berjenjang.</li>
                                <li><i class="fas fa-check text-success-custom me-2"></i>Pagu anggaran menjadi <strong>batas belanja</strong> untuk seluruh transaksi.</li>
                            </ul>
                        </div>
                    </div>
                    <div class="col-md-5">
                        <div class="decision-card h-100">
                            <h5 class="fw-bold mb-3"><i class="fas fa-shield-halved text-warning-custom me-2"></i>Mengapa Penting</h5>
                            <p class="module-desc mb-2"><i class="fas fa-circle-dot me-2 text-highlight"></i>Belanja tidak bisa melampaui pagu tanpa persetujuan.</p>
                            <p class="module-desc mb-2"><i class="fas fa-circle-dot me-2 text-highlight"></i>Setiap rupiah punya mata anggaran &amp; penanggung jawab.</p>
                            <p class="module-desc mb-0"><i class="fas fa-circle-dot me-2 text-highlight"></i>Dasar perbandingan anggaran vs realisasi.</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- ============ 4 REALISASI ANGGARAN ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-chart-line"></i> Tahap 2 &mdash; Realisasi</span>
                    <h2 class="mt-4 mb-2 text-highlight">Realisasi &amp; Pengendalian Anggaran</h2>
                </div>
                <div class="row g-4">
                    <div class="col-md-4"><div class="module-card h-100"><div class="module-title"><i class="fas fa-scale-balanced"></i> Anggaran vs Realisasi</div><p class="module-desc">Pantau serapan tiap mata anggaran secara real-time: pagu, terpakai, dan sisa.</p></div></div>
                    <div class="col-md-4"><div class="module-card h-100"><div class="module-title"><i class="fas fa-gauge-high"></i> Kontrol Pagu Otomatis</div><p class="module-desc">Transaksi belanja memotong sisa pagu; melampaui pagu ditahan/berstatus revisi.</p></div></div>
                    <div class="col-md-4"><div class="module-card h-100"><div class="module-title"><i class="fas fa-calendar-week"></i> Realisasi Bulanan</div><p class="module-desc">Rencana penarikan &amp; realisasi per bulan untuk arus kas yang terukur.</p></div></div>
                </div>
                <div class="value-strip mt-4">
                    <div><i class="fas fa-coins fa-lg text-highlight mb-2"></i><div class="fw-bold">Pagu</div><small class="text-muted-custom">batas belanja</small></div>
                    <div><i class="fas fa-fill-drip fa-lg text-highlight mb-2"></i><div class="fw-bold">Serapan</div><small class="text-muted-custom">% terealisasi</small></div>
                    <div><i class="fas fa-wallet fa-lg text-highlight mb-2"></i><div class="fw-bold">Sisa</div><small class="text-muted-custom">dana tersedia</small></div>
                    <div><i class="fas fa-diagram-project fa-lg text-highlight mb-2"></i><div class="fw-bold">Per Unit</div><small class="text-muted-custom">divisi/sumber dana</small></div>
                </div>
            </div>
        </div>

        <!-- ============ 5 PENGADAAN PR PO BAST ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-cart-flatbed"></i> Tahap 3 &mdash; Pengadaan</span>
                    <h2 class="mt-4 mb-2">Procure-to-Pay: PR &rarr; PO &rarr; BAST &rarr; Tagihan</h2>
                </div>
                <div class="timeline-modern">
                    <div class="timeline-step"><span class="day">Langkah 1</span><div class="icon"><i class="fas fa-file-pen"></i></div><h6 class="fw-bold">Permintaan (PR)</h6><p class="module-desc">Unit mengajukan permintaan pembelian/pengadaan, dicek ketersediaan pagu anggaran.</p></div>
                    <div class="timeline-step"><span class="day">Langkah 2</span><div class="icon"><i class="fas fa-file-signature"></i></div><h6 class="fw-bold">Pesanan (PO)</h6><p class="module-desc">PR disetujui menjadi Purchase Order ke penyedia/vendor terpilih.</p></div>
                    <div class="timeline-step"><span class="day">Langkah 3</span><div class="icon"><i class="fas fa-clipboard-check"></i></div><h6 class="fw-bold">Penerimaan (BAST)</h6><p class="module-desc">Barang/jasa diterima &mdash; Berita Acara Serah Terima &amp; masuk stok/aset.</p></div>
                    <div class="timeline-step"><span class="day">Langkah 4</span><div class="icon"><i class="fas fa-file-invoice-dollar"></i></div><h6 class="fw-bold">Terima Tagihan</h6><p class="module-desc">Tagihan vendor di-3-way match (PO, BAST, invoice) lalu dibayar.</p></div>
                </div>
                <p class="text-center text-muted-custom mt-4 mb-0"><i class="fas fa-circle-info me-2"></i>Setiap langkah terhubung: tidak ada PO tanpa PR yang disetujui, tidak ada pembayaran tanpa BAST.</p>
            </div>
        </div>

        <!-- ============ 6 UANG MUKA & LPJ ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-hand-holding-dollar"></i> Pencairan &amp; Pertanggungjawaban</span>
                    <h2 class="mt-4 mb-2 text-highlight">Uang Muka &amp; LPJ</h2>
                </div>
                <div class="row g-4">
                    <div class="col-md-6">
                        <div class="info-card h-100">
                            <h5 class="fw-bold mb-3"><i class="fas fa-money-check-dollar text-highlight me-2"></i>Uang Muka (Advance)</h5>
                            <p class="module-desc mb-2">Pencairan dana di muka untuk kegiatan/perjalanan dinas berdasarkan anggaran yang disetujui.</p>
                            <p class="module-desc mb-0"><i class="fas fa-arrow-right me-2 text-highlight"></i>Tercatat sebagai <strong>uang muka (piutang sementara)</strong> hingga dipertanggungjawabkan.</p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="info-card h-100">
                            <h5 class="fw-bold mb-3"><i class="fas fa-receipt text-success-custom me-2"></i>LPJ / Pertanggungjawaban</h5>
                            <p class="module-desc mb-2">Realisasi penggunaan dilaporkan dengan bukti; sistem menghitung <strong>sisa/kurang</strong> otomatis.</p>
                            <p class="module-desc mb-0"><i class="fas fa-arrow-right me-2 text-success-custom"></i>Selisih dikembalikan atau ditambah, lalu uang muka <strong>ditutup</strong>.</p>
                        </div>
                    </div>
                </div>
                <div class="quote-panel mt-4 text-center"><i class="fas fa-lock text-highlight me-2"></i>Dana tidak dianggap sebagai beban sampai <strong>LPJ disahkan</strong> &mdash; menjaga akuntabilitas penggunaan dana yayasan.</div>
            </div>
        </div>

        <!-- ============ 7 KAS BESAR & KAS KECIL ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-cash-register"></i> Pengelolaan Kas</span>
                    <h2 class="mt-4 mb-2">Kas Besar &amp; Kas Kecil</h2>
                </div>
                <div class="row g-4">
                    <div class="col-md-6">
                        <div class="module-card h-100" style="border-left-color:var(--primary-color);">
                            <div class="module-title"><i class="fas fa-vault"></i> Kas Besar</div>
                            <p class="module-desc mb-2">Transaksi penerimaan &amp; pengeluaran bernilai besar melalui bank/brankas dengan otorisasi.</p>
                            <p class="module-desc mb-0"><i class="fas fa-circle-dot me-2 text-highlight"></i>Pertanggungjawaban &amp; rekonsiliasi saldo kas besar.</p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card h-100" style="border-left-color:var(--warning-color);">
                            <div class="module-title"><i class="fas fa-wallet"></i> Kas Kecil</div>
                            <p class="module-desc mb-2">Pengeluaran operasional harian (imprest), dengan <strong>penggantian (reimbursement)</strong> saat saldo menipis.</p>
                            <p class="module-desc mb-0"><i class="fas fa-circle-dot me-2 text-warning-custom"></i>Per jenis kas kecil &amp; per pemegang.</p>
                        </div>
                    </div>
                </div>
                <div class="value-strip mt-4">
                    <div><i class="fas fa-arrow-down-long fa-lg text-success-custom mb-2"></i><div class="fw-bold">Penerimaan</div></div>
                    <div><i class="fas fa-arrow-up-long fa-lg text-warning-custom mb-2"></i><div class="fw-bold">Pengeluaran</div></div>
                    <div><i class="fas fa-rotate fa-lg text-highlight mb-2"></i><div class="fw-bold">Penggantian</div></div>
                    <div><i class="fas fa-scale-balanced fa-lg text-highlight mb-2"></i><div class="fw-bold">Rekonsiliasi</div></div>
                </div>
            </div>
        </div>

        <!-- ============ 8 BARANG PERSEDIAAN ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-boxes-stacked"></i> Inventory</span>
                    <h2 class="mt-4 mb-2 text-highlight">Barang Persediaan</h2>
                </div>
                <div class="row g-4">
                    <div class="col-md-4"><div class="module-card h-100"><div class="module-title"><i class="fas fa-warehouse"></i> Kartu Stok</div><p class="module-desc">Stok masuk dari penerimaan pengadaan, keluar saat pemakaian &mdash; saldo per item selalu terkini.</p></div></div>
                    <div class="col-md-4"><div class="module-card h-100"><div class="module-title"><i class="fas fa-magnifying-glass-chart"></i> Monitor &amp; Tracking</div><p class="module-desc">Pantau stok per jenis/tipe item, lacak mutasi, dan deteksi stok minimum.</p></div></div>
                    <div class="col-md-4"><div class="module-card h-100"><div class="module-title"><i class="fas fa-file-lines"></i> Laporan Stok</div><p class="module-desc">Nilai persediaan akhir menjadi komponen neraca &mdash; tersaji otomatis.</p></div></div>
                </div>
                <div class="quote-panel mt-4 text-center"><i class="fas fa-link text-highlight me-2"></i>Persediaan terhubung dengan <strong>pengadaan</strong> (masuk) dan <strong>pemakaian unit</strong> (keluar) &mdash; nilai stok langsung memengaruhi laporan keuangan.</div>
            </div>
        </div>

        <!-- ============ 9 KANTIN ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-utensils"></i> Unit Usaha</span>
                    <h2 class="mt-4 mb-2">Kantin</h2>
                </div>
                <div class="row g-3">
                    <div class="col-md-3 col-6"><div class="info-card text-center h-100 hover-zoom"><div class="feature-icon mx-auto"><i class="fas fa-id-card"></i></div><h6 class="fw-bold">Member &amp; Saldo</h6><p class="module-desc">Anggota kantin dengan saldo/dompet untuk transaksi non-tunai.</p></div></div>
                    <div class="col-md-3 col-6"><div class="info-card text-center h-100 hover-zoom"><div class="feature-icon mx-auto"><i class="fas fa-cash-register"></i></div><h6 class="fw-bold">Penjualan</h6><p class="module-desc">Transaksi penjualan &amp; pesanan member tercatat per produk.</p></div></div>
                    <div class="col-md-3 col-6"><div class="info-card text-center h-100 hover-zoom"><div class="feature-icon mx-auto"><i class="fas fa-box"></i></div><h6 class="fw-bold">Stok Kantin</h6><p class="module-desc">Stok bahan/produk terpantau, sinkron dengan persediaan.</p></div></div>
                    <div class="col-md-3 col-6"><div class="info-card text-center h-100 hover-zoom"><div class="feature-icon mx-auto"><i class="fas fa-chart-simple"></i></div><h6 class="fw-bold">Forecast</h6><p class="module-desc">Prakiraan penjualan &amp; dashboard kinerja kantin.</p></div></div>
                </div>
                <p class="text-center text-muted-custom mt-4 mb-0"><i class="fas fa-circle-info me-2"></i>Pendapatan &amp; harga pokok kantin ikut terbukukan ke jurnal sebagai unit usaha yayasan.</p>
            </div>
        </div>

        <!-- ============ 10 ASET & PENYUSUTAN ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-building-shield"></i> Aktiva Tetap</span>
                    <h2 class="mt-4 mb-2 text-highlight">Manajemen Aset &amp; Penyusutan</h2>
                </div>
                <div class="row g-4">
                    <div class="col-md-6">
                        <div class="info-card h-100">
                            <h5 class="fw-bold mb-3"><i class="fas fa-boxes-packing text-highlight me-2"></i>Siklus Aset</h5>
                            <ul class="feature-list mb-0">
                                <li><i class="fas fa-check text-success-custom me-2"></i>Aset masuk dari pengadaan (DP, termin, pelunasan) &amp; kartu aset.</li>
                                <li><i class="fas fa-check text-success-custom me-2"></i><strong>Pemakaian &amp; peminjaman</strong> aset per unit terlacak.</li>
                                <li><i class="fas fa-check text-success-custom me-2"></i><strong>Penghapusan/pemindahan</strong> aset dengan persetujuan.</li>
                            </ul>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="info-card h-100">
                            <h5 class="fw-bold mb-3"><i class="fas fa-chart-line-down text-warning-custom me-2"></i>Penyusutan Otomatis</h5>
                            <p class="module-desc mb-2">Sistem menghitung penyusutan berkala sesuai metode &amp; umur ekonomis aset.</p>
                            <p class="module-desc mb-0"><i class="fas fa-arrow-right me-2 text-warning-custom"></i>Beban penyusutan &amp; akumulasi penyusutan otomatis menjurnal &mdash; nilai buku aset terjaga akurat.</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- ============ 11 PAYROLL ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-users-gear"></i> Penggajian</span>
                    <h2 class="mt-4 mb-2">Payroll Pegawai</h2>
                </div>
                <div class="row g-3">
                    <div class="col-md-3 col-6"><div class="module-card h-100"><div class="module-title"><i class="fas fa-money-bill-1"></i> Gaji Pokok</div><p class="module-desc">Master gaji pokok per golongan/jabatan &amp; kenaikan gaji berkala terjadwal.</p></div></div>
                    <div class="col-md-3 col-6"><div class="module-card h-100"><div class="module-title"><i class="fas fa-plus-minus"></i> Item Gaji</div><p class="module-desc">Komponen tunjangan, potongan, BPJS, pajak &mdash; per format gaji pegawai.</p></div></div>
                    <div class="col-md-3 col-6"><div class="module-card h-100"><div class="module-title"><i class="fas fa-calculator"></i> Hitung &amp; Bayar</div><p class="module-desc">Proses penggajian massal, slip gaji, dan pembayaran via bank/transfer.</p></div></div>
                    <div class="col-md-3 col-6"><div class="module-card h-100"><div class="module-title"><i class="fas fa-chart-pie"></i> Analisis</div><p class="module-desc">Dashboard beban penggajian per unit/periode untuk pimpinan.</p></div></div>
                </div>
                <div class="quote-panel mt-4 text-center"><i class="fas fa-right-left text-highlight me-2"></i>Hasil penggajian otomatis membentuk <strong>jurnal beban gaji</strong> beserta utang potongan (pajak/BPJS) &mdash; siap dibayarkan &amp; dilaporkan.</div>
            </div>
        </div>

        <!-- ============ 12 POSTING JURNAL ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-right-left"></i> Inti Akuntansi</span>
                    <h2 class="mt-4 mb-2 text-highlight">Posting Jurnal Otomatis</h2>
                    <p class="text-muted-custom mb-0">Setiap modul di atas memetakan transaksinya ke akun &mdash; sistem membentuk jurnal berpasangan (double-entry) tanpa input ulang.</p>
                </div>
                <div class="info-card">
                    <div class="d-flex flex-wrap justify-content-between align-items-center mb-3">
                        <h6 class="fw-bold mb-0"><i class="fas fa-receipt text-highlight me-2"></i>Contoh: Pembayaran tagihan vendor ATK Rp 10.000.000 dari Kas Besar</h6>
                        <span class="price-badge mb-0">Jurnal Pengeluaran</span>
                    </div>
                    <table style="width:100%; border-collapse:collapse; font-size:1.05rem;">
                        <thead><tr style="background:rgba(2,132,199,0.10); color:#075985;">
                            <th style="text-align:left; padding:12px 14px; border-radius:10px 0 0 0;"><%= Common.getBahasaConfig("Akun") %></th>
                            <th style="text-align:right; padding:12px 14px;"><%= Common.getBahasaConfig("Debet") %></th>
                            <th style="text-align:right; padding:12px 14px; border-radius:0 10px 0 0;"><%= Common.getBahasaConfig("Kredit") %></th>
                        </tr></thead>
                        <tbody>
                            <tr style="border-bottom:1px solid rgba(15,23,42,0.08);"><td style="padding:12px 14px;"><i class="fas fa-box me-2 text-highlight"></i>Persediaan ATK / Beban ATK</td><td style="padding:12px 14px; text-align:right; font-weight:800; color:var(--primary-color);">10.000.000</td><td style="padding:12px 14px; text-align:right; color:#94a3b8;">&ndash;</td></tr>
                            <tr><td style="padding:12px 14px;"><i class="fas fa-vault me-2 text-warning-custom"></i>Kas Besar / Bank</td><td style="padding:12px 14px; text-align:right; color:#94a3b8;">&ndash;</td><td style="padding:12px 14px; text-align:right; font-weight:800; color:var(--warning-color);">10.000.000</td></tr>
                            <tr style="background:rgba(22,163,74,0.08);"><td style="padding:12px 14px; font-weight:800; border-radius:0 0 0 10px;">Total (Seimbang)</td><td style="padding:12px 14px; text-align:right; font-weight:900;">10.000.000</td><td style="padding:12px 14px; text-align:right; font-weight:900; border-radius:0 0 10px 0;">10.000.000</td></tr>
                        </tbody>
                    </table>
                    <p class="module-desc mt-3 mb-0"><i class="fas fa-wand-magic-sparkles text-success-custom me-2"></i>Jurnal umum, penerimaan, dan pengeluaran terbentuk dari sumbernya &mdash; mengurangi pagu anggaran sekaligus mencatat akuntansi.</p>
                </div>
            </div>
        </div>

        <!-- ============ 13 LAPORAN KEUANGAN ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-book"></i> Pelaporan</span>
                    <h2 class="mt-4 mb-2">Buku Besar &amp; Laporan Keuangan</h2>
                </div>
                <div class="timeline-modern" style="grid-template-columns:repeat(5,1fr);">
                    <div class="timeline-step" style="min-height:230px;"><div class="icon"><i class="fas fa-book-open"></i></div><h6 class="fw-bold">Buku Besar</h6><p class="module-desc">Mutasi &amp; saldo berjalan tiap akun.</p></div>
                    <div class="timeline-step" style="min-height:230px;"><div class="icon"><i class="fas fa-scale-balanced"></i></div><h6 class="fw-bold">Neraca Saldo</h6><p class="module-desc">Memastikan debet = kredit.</p></div>
                    <div class="timeline-step" style="min-height:230px;"><div class="icon"><i class="fas fa-chart-pie"></i></div><h6 class="fw-bold">Laba Rugi</h6><p class="module-desc">Pendapatan vs beban periode.</p></div>
                    <div class="timeline-step" style="min-height:230px;"><div class="icon"><i class="fas fa-building-columns"></i></div><h6 class="fw-bold">Neraca</h6><p class="module-desc">Aset, kewajiban, &amp; ekuitas.</p></div>
                    <div class="timeline-step" style="min-height:230px;"><div class="icon"><i class="fas fa-water"></i></div><h6 class="fw-bold">Arus Kas</h6><p class="module-desc">Operasi, investasi, pendanaan.</p></div>
                </div>
                <p class="text-center text-muted-custom mt-4 mb-0"><i class="fas fa-circle-info me-2"></i>Termasuk laporan <strong>penyusutan</strong>, <strong>realisasi anggaran</strong>, dan laporan per <strong>kelompok/grup laporan</strong> yang dapat dikonfigurasi.</p>
            </div>
        </div>

        <!-- ============ 14 CLOSING ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-lock"></i> Tutup Buku</span>
                    <h2 class="mt-4 mb-2 text-highlight">Closing Periode</h2>
                </div>
                <div class="row g-4">
                    <div class="col-md-4"><div class="impact-card h-100"><div class="feature-icon"><i class="fas fa-clipboard-check"></i></div><h6 class="fw-bold">Pemeriksaan Akhir</h6><p class="module-desc">Pastikan seluruh transaksi terjurnal &amp; neraca saldo seimbang sebelum ditutup.</p></div></div>
                    <div class="col-md-4"><div class="impact-card h-100"><div class="feature-icon"><i class="fas fa-lock"></i></div><h6 class="fw-bold">Kunci Periode</h6><p class="module-desc">Periode yang ditutup dikunci &mdash; tidak bisa diubah tanpa membuka kembali secara resmi.</p></div></div>
                    <div class="col-md-4"><div class="impact-card h-100"><div class="feature-icon"><i class="fas fa-forward"></i></div><h6 class="fw-bold">Saldo Awal Berikut</h6><p class="module-desc">Saldo akhir menjadi saldo awal periode berikutnya secara otomatis.</p></div></div>
                </div>
                <div class="quote-panel mt-4 text-center"><i class="fas fa-shield-halved text-highlight me-2"></i>Closing menjamin <strong>integritas data historis</strong> dan menjadi titik tolak audit serta laporan tahunan yayasan.</div>
            </div>
        </div>

        <!-- ============ 15 TATA KELOLA & AUDIT ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-shield-halved"></i> Tata Kelola</span>
                    <h2 class="mt-4 mb-2">Otorisasi, Kontrol &amp; Audit</h2>
                </div>
                <div class="row g-4">
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-list-check"></i> Persetujuan Berjenjang</div><p class="module-desc">Setiap pengadaan, pencairan, dan pembayaran melalui alur persetujuan sesuai kewenangan.</p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-shield"></i> Anggaran sebagai Pagar</div><p class="module-desc">Belanja dibatasi pagu &amp; mata anggaran &mdash; mencegah pengeluaran di luar rencana.</p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-fingerprint"></i> Jejak Audit Penuh</div><p class="module-desc">Semua transaksi terekam (siapa, kapan, nilai) &mdash; dapat ditelusuri dari laporan ke dokumen sumber.</p></div></div>
                    <div class="col-md-6"><div class="module-card"><div class="module-title"><i class="fas fa-user-shield"></i> Pemisahan Peran</div><p class="module-desc">Pengaju, penyetuju, pembayar, dan pencatat terpisah sesuai pengendalian internal.</p></div></div>
                </div>
            </div>
        </div>

        <!-- ============ 16 MANFAAT ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-star"></i> Nilai Strategis</span>
                    <h2 class="mt-4 mb-2 text-highlight">Manfaat untuk Yayasan &amp; Instansi</h2>
                </div>
                <div class="row g-4">
                    <div class="col-md-4"><div class="impact-card h-100"><div class="feature-icon"><i class="fas fa-link"></i></div><h6 class="fw-bold">Satu Aliran Data</h6><p class="module-desc">Anggaran, pengadaan, kas, payroll, aset &mdash; semua menyatu ke akuntansi.</p></div></div>
                    <div class="col-md-4"><div class="impact-card h-100"><div class="feature-icon"><i class="fas fa-gauge-high"></i></div><h6 class="fw-bold">Laporan Real-time</h6><p class="module-desc">Posisi anggaran &amp; keuangan siap kapan saja untuk keputusan pimpinan.</p></div></div>
                    <div class="col-md-4"><div class="impact-card h-100"><div class="feature-icon"><i class="fas fa-bullseye"></i></div><h6 class="fw-bold">Akurat &amp; Seimbang</h6><p class="module-desc">Jurnal otomatis berpasangan menutup celah selisih &amp; input ganda.</p></div></div>
                    <div class="col-md-4"><div class="impact-card h-100"><div class="feature-icon"><i class="fas fa-clipboard-check"></i></div><h6 class="fw-bold">Siap Audit</h6><p class="module-desc">Telusur penuh dari laporan ke PR/PO/BAST/kuitansi.</p></div></div>
                    <div class="col-md-4"><div class="impact-card h-100"><div class="feature-icon"><i class="fas fa-shield-halved"></i></div><h6 class="fw-bold">Akuntabel</h6><p class="module-desc">Uang muka &amp; LPJ menjaga pertanggungjawaban dana.</p></div></div>
                    <div class="col-md-4"><div class="impact-card h-100"><div class="feature-icon"><i class="fas fa-piggy-bank"></i></div><h6 class="fw-bold">Efisien</h6><p class="module-desc">Otomasi mengurangi pekerjaan manual &amp; mempercepat tutup buku.</p></div></div>
                </div>
            </div>
        </div>

        <!-- ============ 17 PENUTUP ============ -->
        <div class="slide">
            <div class="slide-content text-center">
                <span class="section-eyebrow mb-4"><i class="fas fa-flag-checkered"></i> Penutup</span>
                <h1 class="display-5 mb-3 hero-gradient-title">Tata Kelola Keuangan Yayasan<br>yang Terintegrasi &amp; Akuntabel</h1>
                <p class="executive-lead mb-4">Dari perencanaan anggaran sampai tutup buku &mdash; satu sistem, terkendali, dan siap audit untuk <strong><%=judul%></strong>.</p>
                <div class="d-flex flex-wrap justify-content-center gap-2 mb-4">
                    <span class="metric-chip"><strong><i class="fas fa-sitemap text-highlight"></i></strong> Anggaran &amp; Realisasi</span>
                    <span class="metric-chip"><strong><i class="fas fa-cart-flatbed text-highlight"></i></strong> Pengadaan &amp; Aset</span>
                    <span class="metric-chip"><strong><i class="fas fa-users-gear text-highlight"></i></strong> Payroll</span>
                    <span class="metric-chip"><strong><i class="fas fa-book text-highlight"></i></strong> Jurnal &rarr; Closing</span>
                </div>
                <a href="<%=targetDemoUrl%>" target="_blank" class="btn btn-primary fw-bold px-5 py-3 rounded-pill text-uppercase shadow-lg fs-5 pulse-button" style="letter-spacing: 2px;">
                    Lihat Demo Sistem <i class="fas fa-arrow-right ms-2"></i>
                </a>
            </div>
        </div>


                <div class="controls">
            <button class="btn-nav" id="btnPrev" title="<%= Common.getBahasaConfig("Sebelumnya") %>"><i class="fas fa-chevron-left"></i></button>
            <span class="slide-counter"><span id="currentSlideNum">1</span> / <span id="totalSlideNum">17</span></span>
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