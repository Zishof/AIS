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
    <title><%= Common.getBahasaConfig("Presentasi Keuangan Mahasiswa") %> | <%=judul%></title>
    
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
            content: "Keuangan Mahasiswa";
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
    <div class="presentation-brand-chip"><i class="fas fa-coins me-2"></i> Keuangan Mahasiswa</div>
    <div class="kbd-hint"><kbd>←</kbd> <kbd>→</kbd> Navigasi &nbsp; <kbd>F</kbd> Fullscreen</div>

    <img src="<%=logo_PerguruanTinggi%>" class="logo-header" alt="Logo">

    <div class="presentation-container" id="presentationArea">

        <!-- ============ SLIDE 1 : COVER ============ -->
        <div class="slide active">
            <div class="text-center slide-content" style="overflow:hidden;">
                <span class="section-eyebrow mb-4"><i class="fas fa-coins"></i> Modul Keuangan Mahasiswa</span>
                <h1 class="display-3 mb-3 hero-gradient-title">Dari Tagihan Mahasiswa<br>Menuju Buku Besar</h1>
                <h2 class="fw-bold text-warning-custom mb-4 display-6">Tagihan &middot; Pembayaran &middot; Posting Jurnal &middot; Buku Besar</h2>
                <p class="executive-lead mb-4">Satu siklus terintegrasi: setiap rupiah yang ditagihkan dan dibayar mahasiswa
                    otomatis terhubung ke jurnal akuntansi dan buku besar &mdash; tanpa input ganda, tanpa rekonsiliasi manual,
                    dengan jejak audit penuh untuk <strong><%=judul%></strong>.</p>
                <div class="d-flex flex-wrap justify-content-center gap-3 mt-2">
                    <span class="metric-chip"><strong>4 Tahap</strong> Setup &rarr; Tagihan &rarr; Bayar &rarr; Posting</span>
                    <span class="metric-chip"><strong>Otomatis</strong> Jurnal &amp; Buku Besar</span>
                    <span class="metric-chip"><strong>Real-time</strong> Pelaporan Keuangan</span>
                </div>
            </div>
        </div>

        <!-- ============ SLIDE 2 : RINGKASAN EKSEKUTIF ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-compass"></i> Ringkasan Eksekutif</span>
                    <h2 class="mt-4 mb-3 hero-gradient-title">Mengubah Penerimaan Mahasiswa Menjadi Catatan Akuntansi Secara Otomatis</h2>
                </div>
                <p class="executive-lead text-center mb-4">Di banyak institusi, bagian <strong>Keuangan/Loket</strong> dan bagian
                    <strong>Akuntansi</strong> bekerja dengan data terpisah. Mahasiswa membayar, lalu staf akuntansi menjurnal ulang
                    secara manual. Modul ini menyatukannya: <em>tagihan</em>, <em>pembayaran</em>, dan <em>jurnal</em> berada pada
                    satu rangkaian data yang sama.</p>
                <div class="row g-4">
                    <div class="col-md-4">
                        <div class="impact-card">
                            <span class="impact-number">1</span>
                            <h5 class="fw-bold mb-2">Satu Sumber Data</h5>
                            <p class="module-desc">Tagihan, pembayaran, dan jurnal memakai master akun yang sama sehingga angka di loket = angka di buku besar.</p>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="impact-card">
                            <span class="impact-number">2</span>
                            <h5 class="fw-bold mb-2">Jurnal Otomatis</h5>
                            <p class="module-desc">Setiap penerimaan langsung membentuk jurnal penerimaan kas/bank sesuai pemetaan akun per item biaya.</p>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="impact-card">
                            <span class="impact-number">3</span>
                            <h5 class="fw-bold mb-2">Laporan Seketika</h5>
                            <p class="module-desc">Buku besar, neraca saldo, laba rugi, dan neraca dapat dilihat kapan saja tanpa menunggu tutup buku manual.</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- ============ SLIDE 3 : TANTANGAN ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-triangle-exclamation"></i> Masalah yang Diselesaikan</span>
                    <h2 class="mt-4 mb-2">Tantangan Pengelolaan Keuangan Mahasiswa Secara Manual</h2>
                </div>
                <div class="row g-4">
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-file-invoice-dollar"></i> Tagihan Tidak Konsisten</div>
                            <p class="module-desc">Besaran SPP/biaya berbeda antar angkatan, prodi, dan jalur, sehingga rentan salah hitung dan sulit diaudit.</p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-arrows-rotate"></i> Rekonsiliasi Melelahkan</div>
                            <p class="module-desc">Pembayaran via teller, transfer, dan Virtual Account harus dicocokkan satu per satu dengan tagihan mahasiswa.</p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-keyboard"></i> Input Ganda ke Akuntansi</div>
                            <p class="module-desc">Data penerimaan diketik ulang ke aplikasi akuntansi &mdash; sumber utama selisih angka dan keterlambatan laporan.</p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-magnifying-glass-chart"></i> Sulit Telusur</div>
                            <p class="module-desc">Saat audit, sulit menelusuri dari satu baris buku besar kembali ke kuitansi dan mahasiswa pembayarnya.</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- ============ SLIDE 4 : ALUR BESAR 4 TAHAP ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-route"></i> Peta Besar Proses</span>
                    <h2 class="mt-4 mb-2 text-highlight">Empat Tahap dari Setup sampai Buku Besar</h2>
                </div>
                <div class="timeline-modern">
                    <div class="timeline-step">
                        <span class="day">Tahap 1</span>
                        <div class="icon"><i class="fas fa-sliders"></i></div>
                        <h5 class="fw-bold">Setup &amp; Pemetaan Akun</h5>
                        <p class="module-desc">Susun bagan akun (COA), item biaya, dan kaitkan tiap item biaya ke akun pendapatan/piutang serta kas/bank.</p>
                    </div>
                    <div class="timeline-step">
                        <span class="day">Tahap 2</span>
                        <div class="icon"><i class="fas fa-file-invoice"></i></div>
                        <h5 class="fw-bold">Pembentukan Tagihan</h5>
                        <p class="module-desc">Kegiatan keuangan menghasilkan tagihan per mahasiswa: SPP, daftar ulang, dengan keringanan &amp; cicilan.</p>
                    </div>
                    <div class="timeline-step">
                        <span class="day">Tahap 3</span>
                        <div class="icon"><i class="fas fa-cash-register"></i></div>
                        <h5 class="fw-bold">Pembayaran Mahasiswa</h5>
                        <p class="module-desc">Bayar via loket, transfer/VA, pembayaran online, atau saldo tabungan &mdash; terverifikasi &amp; berkuitansi.</p>
                    </div>
                    <div class="timeline-step">
                        <span class="day">Tahap 4</span>
                        <div class="icon"><i class="fas fa-book"></i></div>
                        <h5 class="fw-bold">Posting Jurnal &amp; Buku Besar</h5>
                        <p class="module-desc">Penerimaan membentuk jurnal otomatis, lalu di-posting menjadi buku besar dan laporan keuangan.</p>
                    </div>
                </div>
                <p class="text-center text-muted-custom mt-4 mb-0"><i class="fas fa-circle-info me-2"></i>Setiap tahap mewarisi data dari tahap sebelumnya &mdash; tidak ada pengetikan ulang antar bagian.</p>
            </div>
        </div>

        <!-- ============ SLIDE 5 : TAHAP 1 SETUP & PEMETAAN AKUN ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-sliders"></i> Tahap 1 &mdash; Fondasi</span>
                    <h2 class="mt-4 mb-2">Master Biaya &amp; Pemetaan ke Bagan Akun</h2>
                    <p class="text-muted-custom mb-0">Pekerjaan satu kali yang membuat seluruh otomatisasi jurnal menjadi mungkin.</p>
                </div>
                <div class="row g-3">
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-sitemap"></i> Bagan Akun (COA)</div>
                            <p class="module-desc">Master <strong>Akun</strong> &amp; <strong>Grup Akun</strong> menyusun struktur aset, kewajiban, pendapatan, dan beban sebagai dasar buku besar.</p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-tags"></i> Item Biaya</div>
                            <p class="module-desc">Definisi komponen tagihan: SPP, SKS, praktikum, daftar ulang, wisuda &mdash; lengkap dengan besaran per angkatan/prodi.</p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-link"></i> Item Biaya &rarr; Akun</div>
                            <p class="module-desc"><strong>Pemetaan kunci:</strong> setiap item biaya ditautkan ke akun pendapatan / piutangnya, sehingga sistem tahu harus menjurnal ke mana.</p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-building-columns"></i> Kas / Bank Penerimaan</div>
                            <p class="module-desc">Tiap kanal &amp; rekening pembayaran ditautkan ke akun kas/bank, dasar sisi debet pada jurnal penerimaan.</p>
                        </div>
                    </div>
                </div>
                <div class="quote-panel mt-4 text-center">
                    <i class="fas fa-quote-left text-highlight me-2"></i>
                    <strong>Pemetaan item biaya &amp; kas/bank ke akun adalah jembatan</strong> antara dunia administrasi mahasiswa dan dunia akuntansi. Sekali benar, jurnal selalu benar.
                </div>
            </div>
        </div>

        <!-- ============ SLIDE 6 : TAHAP 2 PEMBENTUKAN TAGIHAN ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-file-invoice"></i> Tahap 2 &mdash; Tagihan</span>
                    <h2 class="mt-4 mb-2 text-highlight">Pembentukan Tagihan Mahasiswa</h2>
                </div>
                <div class="row g-4 align-items-stretch">
                    <div class="col-md-7">
                        <div class="info-card h-100">
                            <h5 class="fw-bold mb-3"><i class="fas fa-gears text-highlight me-2"></i>Bagaimana Tagihan Dibentuk</h5>
                            <ul class="feature-list mb-0">
                                <li><i class="fas fa-check text-success-custom me-2"></i><strong>Kegiatan keuangan</strong> (mis. Daftar Ulang Semester) memilih sasaran: angkatan, prodi, jalur.</li>
                                <li><i class="fas fa-check text-success-custom me-2"></i>Sistem menarik <strong>item biaya</strong> yang berlaku dan menghitung tagihan per mahasiswa.</li>
                                <li><i class="fas fa-check text-success-custom me-2"></i><strong>Keringanan, beasiswa, potongan</strong> diterapkan sehingga nilai tagihan akhir akurat per individu.</li>
                                <li><i class="fas fa-check text-success-custom me-2"></i>Tagihan dapat dipecah menjadi <strong>cicilan</strong> dengan jadwal jatuh tempo.</li>
                            </ul>
                        </div>
                    </div>
                    <div class="col-md-5">
                        <div class="decision-card h-100">
                            <h5 class="fw-bold mb-3"><i class="fas fa-bell-concierge text-warning-custom me-2"></i>Disampaikan ke Mahasiswa</h5>
                            <p class="module-desc mb-2"><i class="fas fa-circle-dot me-2 text-highlight"></i>Rincian tagihan tampil di portal mahasiswa &amp; halaman daftar ulang.</p>
                            <p class="module-desc mb-2"><i class="fas fa-circle-dot me-2 text-highlight"></i>Notifikasi tagihan dikirim ke mahasiswa / orang tua (in-app, email, WhatsApp).</p>
                            <p class="module-desc mb-0"><i class="fas fa-circle-dot me-2 text-highlight"></i>Status lunas / kurang / menunggak terlihat transparan.</p>
                        </div>
                    </div>
                </div>
                <div class="value-strip mt-4">
                    <div><i class="fas fa-layer-group fa-lg text-highlight mb-2"></i><div class="fw-bold">Per Item</div><small class="text-muted-custom">SPP, SKS, praktikum</small></div>
                    <div><i class="fas fa-percent fa-lg text-highlight mb-2"></i><div class="fw-bold">Keringanan</div><small class="text-muted-custom">beasiswa &amp; potongan</small></div>
                    <div><i class="fas fa-calendar-days fa-lg text-highlight mb-2"></i><div class="fw-bold">Cicilan</div><small class="text-muted-custom">jadwal jatuh tempo</small></div>
                    <div><i class="fas fa-piggy-bank fa-lg text-highlight mb-2"></i><div class="fw-bold">Tabungan</div><small class="text-muted-custom">saldo &amp; deposit</small></div>
                </div>
            </div>
        </div>

        <!-- ============ SLIDE 7 : TAHAP 3 PEMBAYARAN ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-cash-register"></i> Tahap 3 &mdash; Pembayaran</span>
                    <h2 class="mt-4 mb-2">Kanal Pembayaran Mahasiswa</h2>
                </div>
                <div class="row g-3">
                    <div class="col-md-3 col-6">
                        <div class="info-card text-center h-100 hover-zoom">
                            <div class="feature-icon mx-auto"><i class="fas fa-store"></i></div>
                            <h6 class="fw-bold">Loket / Teller</h6>
                            <p class="module-desc">Petugas keuangan menginput pembayaran &amp; mencetak kuitansi.</p>
                        </div>
                    </div>
                    <div class="col-md-3 col-6">
                        <div class="info-card text-center h-100 hover-zoom">
                            <div class="feature-icon mx-auto"><i class="fas fa-building-columns"></i></div>
                            <h6 class="fw-bold">Transfer / VA</h6>
                            <p class="module-desc">Virtual Account host-to-host bank, terekonsiliasi otomatis ke tagihan.</p>
                        </div>
                    </div>
                    <div class="col-md-3 col-6">
                        <div class="info-card text-center h-100 hover-zoom">
                            <div class="feature-icon mx-auto"><i class="fas fa-globe"></i></div>
                            <h6 class="fw-bold">Pembayaran Online</h6>
                            <p class="module-desc">Mahasiswa membayar mandiri lewat portal / payment gateway.</p>
                        </div>
                    </div>
                    <div class="col-md-3 col-6">
                        <div class="info-card text-center h-100 hover-zoom">
                            <div class="feature-icon mx-auto"><i class="fas fa-piggy-bank"></i></div>
                            <h6 class="fw-bold">Saldo Tabungan</h6>
                            <p class="module-desc">Potong dari tabungan/deposit; bila kurang otomatis terangsur sesuai saldo.</p>
                        </div>
                    </div>
                </div>
                <div class="row g-4 mt-1">
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-list-check"></i> Cicilan &amp; Pelunasan</div>
                            <p class="module-desc">Pembayaran dialokasikan ke cicilan/tagihan terkait; sisa kurang bayar dan pelunasan terpantau otomatis.</p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-shield-halved"></i> Verifikasi &amp; Kuitansi</div>
                            <p class="module-desc">Setiap pembayaran divalidasi, diberi nomor kuitansi, dan menjadi dokumen sumber untuk jurnal.</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- ============ SLIDE 8 : TAHAP 4 POSTING JURNAL ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-right-left"></i> Tahap 4 &mdash; Jurnal</span>
                    <h2 class="mt-4 mb-2 text-highlight">Penerimaan Otomatis Menjadi Jurnal</h2>
                    <p class="text-muted-custom mb-0">Begitu pembayaran tervalidasi, sistem membentuk <strong>Jurnal Penerimaan</strong> berpasangan (double-entry) berdasarkan pemetaan akun.</p>
                </div>
                <div class="info-card">
                    <div class="d-flex flex-wrap justify-content-between align-items-center mb-3">
                        <h6 class="fw-bold mb-0"><i class="fas fa-receipt text-highlight me-2"></i>Contoh: SPP Rp 6.975.000 dibayar via transfer bank</h6>
                        <span class="price-badge mb-0">Jurnal Penerimaan Kas/Bank</span>
                    </div>
                    <table style="width:100%; border-collapse:collapse; font-size:1.05rem;">
                        <thead>
                            <tr style="background:rgba(2,132,199,0.10); color:#075985;">
                                <th style="text-align:left; padding:12px 14px; border-radius:10px 0 0 0;"><%= Common.getBahasaConfig("Akun") %></th>
                                <th style="text-align:right; padding:12px 14px;"><%= Common.getBahasaConfig("Debet") %></th>
                                <th style="text-align:right; padding:12px 14px; border-radius:0 10px 0 0;"><%= Common.getBahasaConfig("Kredit") %></th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr style="border-bottom:1px solid rgba(15,23,42,0.08);">
                                <td style="padding:12px 14px;"><i class="fas fa-building-columns me-2 text-highlight"></i>Bank &mdash; Penerimaan Mahasiswa</td>
                                <td style="padding:12px 14px; text-align:right; font-weight:800; color:var(--primary-color);">6.975.000</td>
                                <td style="padding:12px 14px; text-align:right; color:#94a3b8;">&ndash;</td>
                            </tr>
                            <tr>
                                <td style="padding:12px 14px;"><i class="fas fa-graduation-cap me-2 text-warning-custom"></i>Pendapatan SPP</td>
                                <td style="padding:12px 14px; text-align:right; color:#94a3b8;">&ndash;</td>
                                <td style="padding:12px 14px; text-align:right; font-weight:800; color:var(--warning-color);">6.975.000</td>
                            </tr>
                            <tr style="background:rgba(22,163,74,0.08);">
                                <td style="padding:12px 14px; font-weight:800; border-radius:0 0 0 10px;">Total (Seimbang)</td>
                                <td style="padding:12px 14px; text-align:right; font-weight:900;">6.975.000</td>
                                <td style="padding:12px 14px; text-align:right; font-weight:900; border-radius:0 0 10px 0;">6.975.000</td>
                            </tr>
                        </tbody>
                    </table>
                    <p class="module-desc mt-3 mb-0"><i class="fas fa-wand-magic-sparkles text-success-custom me-2"></i>
                        Akun <strong>Bank</strong> diambil dari kanal pembayaran; akun <strong>Pendapatan SPP</strong> diambil dari pemetaan item biaya. Tanggal, nomor bukti, dan referensi mahasiswa melekat pada jurnal.</p>
                </div>
            </div>
        </div>

        <!-- ============ SLIDE 9 : BUKU BESAR & LAPORAN ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-book"></i> Posting &amp; Pelaporan</span>
                    <h2 class="mt-4 mb-2">Dari Jurnal ke Buku Besar &amp; Laporan Keuangan</h2>
                </div>
                <div class="timeline-modern" style="grid-template-columns:repeat(5,1fr);">
                    <div class="timeline-step" style="min-height:240px;">
                        <div class="icon"><i class="fas fa-list-ol"></i></div>
                        <h6 class="fw-bold">Jurnal</h6>
                        <p class="module-desc">Catatan transaksi berpasangan dari setiap penerimaan.</p>
                    </div>
                    <div class="timeline-step" style="min-height:240px;">
                        <div class="icon"><i class="fas fa-book-open"></i></div>
                        <h6 class="fw-bold">Buku Besar</h6>
                        <p class="module-desc">Posting jurnal dikelompokkan &amp; diakumulasi per akun (saldo berjalan).</p>
                    </div>
                    <div class="timeline-step" style="min-height:240px;">
                        <div class="icon"><i class="fas fa-scale-balanced"></i></div>
                        <h6 class="fw-bold">Neraca Saldo</h6>
                        <p class="module-desc">Ringkasan saldo seluruh akun untuk memastikan debet = kredit.</p>
                    </div>
                    <div class="timeline-step" style="min-height:240px;">
                        <div class="icon"><i class="fas fa-chart-pie"></i></div>
                        <h6 class="fw-bold">Laba Rugi &amp; Neraca</h6>
                        <p class="module-desc">Laporan posisi keuangan &amp; kinerja tersusun otomatis.</p>
                    </div>
                    <div class="timeline-step" style="min-height:240px;">
                        <div class="icon"><i class="fas fa-lock"></i></div>
                        <h6 class="fw-bold">Tutup Buku</h6>
                        <p class="module-desc">Closing periode mengunci data &amp; membawa saldo ke periode berikut.</p>
                    </div>
                </div>
                <p class="text-center text-muted-custom mt-4 mb-0"><i class="fas fa-circle-info me-2"></i>Laporan <strong>Buku Besar</strong> menampilkan mutasi tiap akun lengkap dengan referensi balik ke jurnal &amp; kuitansi mahasiswa.</p>
            </div>
        </div>

        <!-- ============ SLIDE 10 : CONTOH KASUS LENGKAP ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-diagram-project"></i> Studi Kasus</span>
                    <h2 class="mt-4 mb-2 text-highlight">Satu Mahasiswa, Dua Skenario, Tetap Seimbang</h2>
                </div>
                <div class="row g-4">
                    <div class="col-md-6">
                        <div class="info-card h-100">
                            <h6 class="fw-bold mb-3"><i class="fas fa-money-bill-wave text-success-custom me-2"></i>A. Bayar Lunas Tunai/Transfer</h6>
                            <table style="width:100%; border-collapse:collapse;">
                                <tr style="border-bottom:1px solid rgba(15,23,42,0.08);">
                                    <td style="padding:9px 6px;">Kas/Bank <span class="badge bg-light text-dark">D</span></td>
                                    <td style="padding:9px 6px; text-align:right; font-weight:700; color:var(--primary-color);">5.000.000</td>
                                </tr>
                                <tr>
                                    <td style="padding:9px 6px;">Pendapatan SPP <span class="badge bg-light text-dark">K</span></td>
                                    <td style="padding:9px 6px; text-align:right; font-weight:700; color:var(--warning-color);">5.000.000</td>
                                </tr>
                            </table>
                            <p class="module-desc mt-3 mb-0">Penerimaan langsung diakui sebagai pendapatan pada periode berjalan.</p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="info-card h-100">
                            <h6 class="fw-bold mb-3"><i class="fas fa-list-check text-highlight me-2"></i>B. Cicilan (Piutang)</h6>
                            <p class="module-desc mb-2"><strong>Saat tagihan dibuat</strong> (opsional, basis akrual):</p>
                            <table style="width:100%; border-collapse:collapse; margin-bottom:.6rem;">
                                <tr style="border-bottom:1px solid rgba(15,23,42,0.08);">
                                    <td style="padding:7px 6px;">Piutang Mahasiswa <span class="badge bg-light text-dark">D</span></td>
                                    <td style="padding:7px 6px; text-align:right; font-weight:700;">5.000.000</td>
                                </tr>
                                <tr><td style="padding:7px 6px;">Pendapatan SPP <span class="badge bg-light text-dark">K</span></td>
                                    <td style="padding:7px 6px; text-align:right; font-weight:700;">5.000.000</td></tr>
                            </table>
                            <p class="module-desc mb-2"><strong>Saat cicilan ke-1 dibayar:</strong></p>
                            <table style="width:100%; border-collapse:collapse;">
                                <tr style="border-bottom:1px solid rgba(15,23,42,0.08);">
                                    <td style="padding:7px 6px;">Kas/Bank <span class="badge bg-light text-dark">D</span></td>
                                    <td style="padding:7px 6px; text-align:right; font-weight:700; color:var(--primary-color);">2.000.000</td>
                                </tr>
                                <tr><td style="padding:7px 6px;">Piutang Mahasiswa <span class="badge bg-light text-dark">K</span></td>
                                    <td style="padding:7px 6px; text-align:right; font-weight:700; color:var(--warning-color);">2.000.000</td></tr>
                            </table>
                        </div>
                    </div>
                </div>
                <div class="quote-panel mt-4 text-center">
                    <i class="fas fa-scale-balanced text-highlight me-2"></i>Pada skenario apa pun, <strong>total debet selalu sama dengan total kredit</strong> &mdash; buku besar dijamin seimbang tanpa koreksi manual.
                </div>
            </div>
        </div>

        <!-- ============ SLIDE 11 : PIPELINE END-TO-END ============ -->
        <div class="slide">
            <div class="slide-content text-center">
                <span class="section-eyebrow mb-4"><i class="fas fa-network-wired"></i> Alur Data End-to-End</span>
                <h2 class="mb-5 mt-3 text-highlight">Satu Aliran, Tanpa Pengetikan Ulang</h2>
                <div class="d-flex flex-wrap justify-content-center align-items-stretch gap-2">
                    <div class="info-card hover-zoom" style="width:170px;">
                        <div class="feature-icon mx-auto"><i class="fas fa-tags"></i></div>
                        <h6 class="fw-bold mb-1">Item Biaya</h6>
                        <small class="text-muted-custom">+ pemetaan akun</small>
                    </div>
                    <div class="d-flex align-items-center"><i class="fas fa-arrow-right fa-lg text-highlight"></i></div>
                    <div class="info-card hover-zoom" style="width:170px;">
                        <div class="feature-icon mx-auto"><i class="fas fa-file-invoice-dollar"></i></div>
                        <h6 class="fw-bold mb-1">Tagihan</h6>
                        <small class="text-muted-custom">per mahasiswa</small>
                    </div>
                    <div class="d-flex align-items-center"><i class="fas fa-arrow-right fa-lg text-highlight"></i></div>
                    <div class="info-card hover-zoom" style="width:170px;">
                        <div class="feature-icon mx-auto"><i class="fas fa-cash-register"></i></div>
                        <h6 class="fw-bold mb-1">Pembayaran</h6>
                        <small class="text-muted-custom">+ kuitansi</small>
                    </div>
                    <div class="d-flex align-items-center"><i class="fas fa-arrow-right fa-lg text-highlight"></i></div>
                    <div class="info-card hover-zoom" style="width:170px;">
                        <div class="feature-icon mx-auto"><i class="fas fa-right-left"></i></div>
                        <h6 class="fw-bold mb-1">Jurnal</h6>
                        <small class="text-muted-custom">otomatis</small>
                    </div>
                    <div class="d-flex align-items-center"><i class="fas fa-arrow-right fa-lg text-highlight"></i></div>
                    <div class="info-card hover-zoom" style="width:170px;">
                        <div class="feature-icon mx-auto"><i class="fas fa-book-open"></i></div>
                        <h6 class="fw-bold mb-1">Buku Besar</h6>
                        <small class="text-muted-custom">&amp; laporan</small>
                    </div>
                </div>
                <p class="executive-lead mt-5 mb-0">Data lahir sekali di hulu (item biaya) dan mengalir utuh sampai hilir (buku besar). Inilah inti integrasi keuangan mahasiswa.</p>
            </div>
        </div>

        <!-- ============ SLIDE 12 : KONTROL, REKONSILIASI & AUDIT ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-shield-halved"></i> Tata Kelola</span>
                    <h2 class="mt-4 mb-2">Kontrol, Rekonsiliasi &amp; Jejak Audit</h2>
                </div>
                <div class="row g-4">
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-arrows-rotate"></i> Rekonsiliasi Otomatis</div>
                            <p class="module-desc">Mutasi bank/VA dicocokkan otomatis dengan tagihan &amp; jurnal &mdash; selisih langsung tampak.</p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-lock"></i> Kunci Setelah Posting</div>
                            <p class="module-desc">Jurnal yang sudah diposting &amp; periode yang sudah ditutup tidak dapat diubah diam-diam.</p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-fingerprint"></i> Jejak Audit Penuh</div>
                            <p class="module-desc">Setiap perubahan tercatat (siapa, kapan, nilai lama/baru) untuk kebutuhan audit internal &amp; eksternal.</p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="module-card">
                            <div class="module-title"><i class="fas fa-user-shield"></i> Hak Akses Berjenjang</div>
                            <p class="module-desc">Pemisahan peran loket, verifikator, dan akuntansi sesuai prinsip pengendalian internal.</p>
                        </div>
                    </div>
                </div>
                <div class="quote-panel mt-4 text-center">
                    <i class="fas fa-route text-highlight me-2"></i>Dari <strong>satu baris buku besar</strong> dapat ditelusuri mundur ke jurnal, kuitansi, tagihan, hingga mahasiswa pembayarnya.
                </div>
            </div>
        </div>

        <!-- ============ SLIDE 13 : MANFAAT / NILAI STRATEGIS ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-star"></i> Nilai Strategis</span>
                    <h2 class="mt-4 mb-2 text-highlight">Manfaat yang Langsung Terasa</h2>
                </div>
                <div class="row g-4">
                    <div class="col-md-4">
                        <div class="impact-card h-100">
                            <div class="feature-icon"><i class="fas fa-bolt"></i></div>
                            <h6 class="fw-bold">Tanpa Input Ganda</h6>
                            <p class="module-desc">Loket dan akuntansi memakai data yang sama &mdash; hemat waktu &amp; bebas selisih ketik.</p>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="impact-card h-100">
                            <div class="feature-icon"><i class="fas fa-gauge-high"></i></div>
                            <h6 class="fw-bold">Laporan Real-time</h6>
                            <p class="module-desc">Buku besar &amp; laporan keuangan siap kapan saja untuk pengambilan keputusan pimpinan.</p>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="impact-card h-100">
                            <div class="feature-icon"><i class="fas fa-bullseye"></i></div>
                            <h6 class="fw-bold">Akurasi Tinggi</h6>
                            <p class="module-desc">Jurnal berpasangan otomatis memastikan setiap penerimaan tercatat tepat &amp; seimbang.</p>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="impact-card h-100">
                            <div class="feature-icon"><i class="fas fa-eye"></i></div>
                            <h6 class="fw-bold">Transparan ke Mahasiswa</h6>
                            <p class="module-desc">Mahasiswa melihat tagihan, riwayat bayar, dan sisa secara mandiri di portal.</p>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="impact-card h-100">
                            <div class="feature-icon"><i class="fas fa-clipboard-check"></i></div>
                            <h6 class="fw-bold">Siap Audit</h6>
                            <p class="module-desc">Telusur penuh dari laporan ke dokumen sumber mempercepat proses audit.</p>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="impact-card h-100">
                            <div class="feature-icon"><i class="fas fa-handshake-angle"></i></div>
                            <h6 class="fw-bold">Pelayanan Lebih Baik</h6>
                            <p class="module-desc">Antrian loket berkurang berkat kanal mandiri &amp; pembayaran online.</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- ============ SLIDE 14 : DASHBOARD & MONITORING ============ -->
        <div class="slide">
            <div class="slide-content">
                <div class="text-center mb-4">
                    <span class="section-eyebrow"><i class="fas fa-chart-line"></i> Pemantauan</span>
                    <h2 class="mt-4 mb-2">Dashboard Keuangan Mahasiswa</h2>
                    <p class="text-muted-custom mb-0">Pimpinan &amp; bagian keuangan memantau kondisi secara ringkas dan langsung.</p>
                </div>
                <div class="row g-3 text-center">
                    <div class="col-md-3 col-6"><div class="metric-chip h-100"><strong>Penerimaan</strong><small class="text-muted-custom d-block mt-1">harian / bulanan per kanal</small></div></div>
                    <div class="col-md-3 col-6"><div class="metric-chip h-100"><strong>Tunggakan</strong><small class="text-muted-custom d-block mt-1">per prodi / angkatan</small></div></div>
                    <div class="col-md-3 col-6"><div class="metric-chip h-100"><strong>Realisasi</strong><small class="text-muted-custom d-block mt-1">target vs penerimaan</small></div></div>
                    <div class="col-md-3 col-6"><div class="metric-chip h-100"><strong>Posisi Kas</strong><small class="text-muted-custom d-block mt-1">saldo buku besar</small></div></div>
                </div>
                <div class="row g-4 mt-1">
                    <div class="col-md-6">
                        <div class="info-card h-100">
                            <h6 class="fw-bold mb-2"><i class="fas fa-magnifying-glass-dollar text-highlight me-2"></i>Telusur Cepat</h6>
                            <p class="module-desc mb-0">Klik angka penerimaan untuk membuka rincian pembayaran, kuitansi, dan jurnal terkait.</p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="info-card h-100">
                            <h6 class="fw-bold mb-2"><i class="fas fa-triangle-exclamation text-warning-custom me-2"></i>Deteksi Dini Tunggakan</h6>
                            <p class="module-desc mb-0">Daftar mahasiswa menunggak otomatis terbentuk untuk tindak lanjut penagihan.</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- ============ SLIDE 15 : PENUTUP / CTA ============ -->
        <div class="slide">
            <div class="slide-content text-center">
                <span class="section-eyebrow mb-4"><i class="fas fa-flag-checkered"></i> Penutup</span>
                <h1 class="display-5 mb-3 hero-gradient-title">Keuangan Mahasiswa yang Tertib,<br>Akuntansi yang Otomatis</h1>
                <p class="executive-lead mb-4">Dari tagihan hingga buku besar dalam satu sistem &mdash; akurat, transparan, dan siap audit untuk <strong><%=judul%></strong>.</p>
                <div class="d-flex flex-wrap justify-content-center gap-3 mb-4">
                    <span class="metric-chip"><strong><i class="fas fa-file-invoice-dollar text-highlight"></i></strong> Tagihan Terstruktur</span>
                    <span class="metric-chip"><strong><i class="fas fa-cash-register text-highlight"></i></strong> Multi-Kanal Bayar</span>
                    <span class="metric-chip"><strong><i class="fas fa-book text-highlight"></i></strong> Jurnal &amp; Buku Besar Otomatis</span>
                </div>
                <a href="<%=targetDemoUrl%>" target="_blank" class="btn btn-primary fw-bold px-5 py-3 rounded-pill text-uppercase shadow-lg fs-5 pulse-button" style="letter-spacing: 2px;">
                    Lihat Demo Sistem <i class="fas fa-arrow-right ms-2"></i>
                </a>
            </div>
        </div>


                <div class="controls">
            <button class="btn-nav" id="btnPrev" title="<%= Common.getBahasaConfig("Sebelumnya") %>"><i class="fas fa-chevron-left"></i></button>
            <span class="slide-counter"><span id="currentSlideNum">1</span> / <span id="totalSlideNum">15</span></span>
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
</html>