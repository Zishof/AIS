<%@page import="ais.common.Common"%>
<%@ page import="ais.database.model.sekolah.Sekolah" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Locale" %>
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
String paramKotaSurat = request.getParameter("kota_surat");
String paramNomorSurat = request.getParameter("nomor_surat");

// Deklarasi Variabel Data Institusi Penyedia (Dengan Override dari Parameter)
ais.database.model.sekolah.Sekolah sekolah = ais.action.master.sekolah.util.SekolahUtil.getSekolah(request);
String Telepon = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getTelepon();
String Alamat1 = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getAlamat1();
String Email = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getEmail();

// Override Variabel berdasarkan parameter White-Label
String judul = (paramJudul != null && !paramJudul.trim().isEmpty()) ? paramJudul : ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama();
String logo_PerguruanTinggi = (paramLogo != null && !paramLogo.trim().isEmpty()) ? Common.ROOT + "/img/" + paramLogo : ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");

// Fallback Data Klien & Presenter
String namaClient = (paramClient != null && !paramClient.trim().isEmpty()) ? paramClient : "Pimpinan Institusi / Yayasan Pendidikan";
String namaPenyedia = (paramPresentBy != null && !paramPresentBy.trim().isEmpty()) ? paramPresentBy : "Tim Konsultan " + judul;
String kontakPenyedia = (paramTelp != null && !paramTelp.trim().isEmpty()) ? paramTelp : Telepon;
String kotaSurat = (paramKotaSurat != null && !paramKotaSurat.trim().isEmpty()) ? paramKotaSurat : "Jakarta";

// Generate Tanggal Hari Ini dengan Format Bahasa Indonesia
Calendar cal = Calendar.getInstance();
String tanggalSurat = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID")).format(cal.getTime());
String nomorSuratDefault = "0" + cal.get(Calendar.DAY_OF_MONTH) + "/SPH/" + cal.get(Calendar.YEAR) + "/" + new SimpleDateFormat("MM").format(cal.getTime());
String nomorSurat = (paramNomorSurat != null && !paramNomorSurat.trim().isEmpty()) ? paramNomorSurat : nomorSuratDefault;
%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= Common.getBahasaConfig("Surat Penawaran") %> | <%=judul%></title>
    
    <link rel="icon" href="<%=logo_PerguruanTinggi%>" type="image/x-icon">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
    
    <style>
        :root {
            --primary-color: #0f172a;
            --primary-soft: #eff6ff;
            --secondary-color: #0284c7;
            --accent-color: #f59e0b;
            --success-color: #16a34a;
            --danger-color: #dc2626;
            --muted-color: #64748b;
            --soft-border: #dbeafe;
            --paper-shadow: 0 24px 70px rgba(15, 23, 42, 0.16);
            --text-dark: #111827;
            --text-soft: #475569;
            --border-color: #0f172a;
        }

        * {
            box-sizing: border-box;
        }

        body {
            min-height: 100vh;
            background:
                radial-gradient(circle at top left, rgba(2,132,199,0.18), transparent 30%),
                radial-gradient(circle at top right, rgba(245,158,11,0.16), transparent 28%),
                linear-gradient(135deg, #eef2ff 0%, #f8fafc 45%, #e0f2fe 100%);
            font-family: "Segoe UI", Arial, sans-serif;
            color: var(--text-dark);
            line-height: 1.62;
            margin: 0;
            padding: 0;
        }

        .container {
            max-width: 100%;
        }

        .letter-paper {
            position: relative;
            overflow: hidden;
            background-color: #ffffff;
            width: 210mm;
            max-width: 940px;
            margin: 2.2rem auto;
            padding: 2.2rem 2.55rem 2.4rem;
            border-radius: 24px;
            box-shadow: var(--paper-shadow);
            border: 1px solid rgba(148, 163, 184, 0.28);
        }

        .letter-paper::before {
            content: "";
            position: absolute;
            left: 0;
            top: 0;
            width: 100%;
            height: 9px;
            background: linear-gradient(90deg, #0f172a, #0284c7, #22c55e, #f59e0b);
        }

        .letter-watermark {
            position: absolute;
            right: -45px;
            top: 150px;
            width: 260px;
            height: 260px;
            border-radius: 999px;
            background: radial-gradient(circle, rgba(14,165,233,0.09), rgba(14,165,233,0.02), transparent 72%);
            pointer-events: none;
        }

        .document-badge-row {
            display: flex;
            justify-content: space-between;
            gap: 1rem;
            align-items: center;
            margin-bottom: 1.1rem;
            font-size: 0.78rem;
            color: var(--muted-color);
            text-transform: uppercase;
            letter-spacing: 0.08em;
            font-weight: 800;
        }

        .doc-badge {
            display: inline-flex;
            align-items: center;
            gap: 0.45rem;
            padding: 0.45rem 0.7rem;
            border-radius: 999px;
            background: #f8fafc;
            border: 1px solid #e2e8f0;
            color: #0f172a;
            white-space: nowrap;
        }

        /* Kop Surat */
        .kop-surat {
            display: flex;
            align-items: center;
            gap: 1.25rem;
            border-bottom: 0;
            padding: 1rem 0 1.15rem;
            margin-bottom: 1.35rem;
            position: relative;
        }

        .kop-surat::after {
            content: "";
            position: absolute;
            bottom: 0;
            left: 0;
            width: 100%;
            height: 4px;
            border-radius: 999px;
            background: linear-gradient(90deg, #0f172a 0%, #0284c7 50%, #f59e0b 100%);
        }

        .kop-logo {
            width: 92px;
            height: 92px;
            max-height: 92px;
            max-width: 120px;
            object-fit: contain;
            padding: 0.6rem;
            border-radius: 22px;
            border: 1px solid #e2e8f0;
            background: #ffffff;
            box-shadow: 0 14px 28px rgba(15,23,42,0.08);
            margin-right: 0;
            flex-shrink: 0;
        }

        .kop-text {
            flex-grow: 1;
            text-align: left;
            padding-right: 0;
        }

        .kop-title {
            font-size: 1.72rem;
            line-height: 1.15;
            font-weight: 900;
            letter-spacing: -0.035em;
            text-transform: uppercase;
            margin-bottom: 0.35rem;
            color: var(--primary-color);
        }

        .kop-subtitle {
            display: inline-flex;
            align-items: center;
            gap: 0.45rem;
            font-size: 0.97rem;
            line-height: 1.35;
            font-weight: 800;
            color: var(--secondary-color);
            margin-bottom: 0.45rem;
        }

        .kop-subtitle::before {
            content: "Enterprise Education";
            font-size: 0.68rem;
            text-transform: uppercase;
            letter-spacing: 0.09em;
            color: #ffffff;
            background: linear-gradient(135deg, #0284c7, #0f172a);
            padding: 0.25rem 0.48rem;
            border-radius: 999px;
        }

        .kop-address {
            font-size: 0.88rem;
            color: var(--text-soft);
            line-height: 1.55;
        }

        /* Meta Surat */
        .meta-surat {
            display: grid;
            grid-template-columns: 1fr auto;
            gap: 1.2rem;
            align-items: stretch;
            margin-bottom: 1.35rem;
            font-size: 0.98rem;
        }

        .meta-kiri,
        .meta-kanan {
            border: 1px solid #dbeafe;
            background: linear-gradient(180deg, #f8fafc, #ffffff);
            border-radius: 18px;
            padding: 0.95rem 1rem;
        }

        .meta-kiri table {
            width: 100%;
        }

        .meta-kiri td {
            vertical-align: top;
            padding-bottom: 0.35rem;
        }

        .meta-kiri tr:last-child td {
            padding-bottom: 0;
        }

        .meta-kanan {
            min-width: 210px;
            text-align: right;
            font-weight: 800;
            color: var(--primary-color);
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .meta-kanan::before {
            content: "\f133";
            font-family: "Font Awesome 6 Free";
            font-weight: 900;
            margin-right: 0.55rem;
            color: var(--secondary-color);
        }

        .doc-quick-summary {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 0.75rem;
            margin: 0.35rem 0 1.45rem;
            page-break-inside: avoid;
        }

        .summary-pill {
            border: 1px solid #dbeafe;
            background: linear-gradient(180deg, #f8fafc 0%, #ffffff 100%);
            border-radius: 16px;
            padding: 0.8rem 0.85rem;
            min-height: 74px;
        }

        .summary-pill .label {
            display: block;
            font-size: 0.72rem;
            letter-spacing: 0.08em;
            text-transform: uppercase;
            color: var(--muted-color);
            font-weight: 800;
            margin-bottom: 0.18rem;
        }

        .summary-pill .value {
            display: block;
            font-size: 0.95rem;
            color: var(--primary-color);
            font-weight: 900;
            line-height: 1.32;
        }

        /* Alamat Tujuan */
        .recipient-section {
            margin-bottom: 1.45rem;
            font-size: 1rem;
            padding: 1rem 1.15rem;
            border-radius: 18px;
            border: 1px solid #e2e8f0;
            background:
                linear-gradient(90deg, rgba(2,132,199,0.08), rgba(255,255,255,0.96) 46%),
                #ffffff;
            page-break-inside: avoid;
        }

        .recipient-section strong {
            color: var(--primary-color);
        }

        /* Isi Surat */
        .letter-content {
            position: relative;
            z-index: 1;
            text-align: justify;
            font-size: 1rem;
            color: var(--text-dark);
        }

        .letter-content p {
            margin-bottom: 0.98rem;
        }

        .lead-paragraph {
            font-size: 1.05rem;
            color: #0f172a;
            border-left: 5px solid var(--secondary-color);
            background: #f8fafc;
            padding: 1rem 1.15rem;
            border-radius: 0 18px 18px 0;
            margin-bottom: 1.1rem !important;
        }

        .section-hl,
        .letter-content p:not(.lead-paragraph) > strong:first-child {
            display: inline-flex;
            align-items: center;
            gap: 0.45rem;
            margin: 1.05rem 0 0.55rem;
            padding: 0.48rem 0.75rem;
            border-radius: 999px;
            background: linear-gradient(135deg, #0f172a, #0284c7);
            color: #ffffff !important;
            font-size: 0.89rem;
            letter-spacing: 0.015em;
            font-weight: 900;
            page-break-after: avoid;
        }

        .letter-content p:not(.lead-paragraph) > strong:first-child::before {
            content: "\f058";
            font-family: "Font Awesome 6 Free";
            font-weight: 900;
            font-size: 0.85rem;
        }

        /* Daftar Keunggulan */
        .highlight-list {
            list-style: none;
            margin: 0 0 1.15rem 0;
            padding: 0;
        }

        .highlight-list li {
            position: relative;
            margin-bottom: 0.62rem;
            padding: 0.72rem 0.85rem 0.72rem 2.45rem;
            border-radius: 15px;
            background: #ffffff;
            border: 1px solid #e2e8f0;
            box-shadow: 0 8px 18px rgba(15,23,42,0.045);
            text-align: left;
        }

        .highlight-list li::before {
            content: "\f00c";
            font-family: "Font Awesome 6 Free";
            font-weight: 900;
            position: absolute;
            left: 0.85rem;
            top: 0.78rem;
            width: 1.12rem;
            height: 1.12rem;
            border-radius: 999px;
            display: grid;
            place-items: center;
            color: #ffffff;
            background: var(--success-color);
            font-size: 0.62rem;
        }

        .highlight-list li strong {
            color: var(--secondary-color);
            font-weight: 900;
        }

        .executive-summary {
            border-radius: 22px;
            border: 1px solid #bfdbfe;
            background:
                radial-gradient(circle at top right, rgba(14,165,233,0.18), transparent 36%),
                linear-gradient(135deg, #eff6ff 0%, #ffffff 72%);
            padding: 1.15rem 1.2rem;
            margin: 1.1rem 0 1.25rem;
            page-break-inside: avoid;
            box-shadow: 0 15px 30px rgba(2,132,199,0.08);
        }

        .executive-summary h4,
        .panel-box h4,
        .compact-table-title {
            color: var(--primary-color);
            font-size: 1.05rem;
            font-weight: 900;
            letter-spacing: -0.01em;
            margin-bottom: 0.62rem;
        }

        .executive-summary p {
            margin-bottom: 0.65rem;
        }

        .insight-grid,
        .deliverable-grid,
        .next-step-grid {
            display: grid;
            grid-template-columns: repeat(2, minmax(0, 1fr));
            gap: 0.82rem;
            margin: 0.95rem 0 1.1rem;
            page-break-inside: avoid;
        }

        .insight-card,
        .deliverable-card,
        .next-step-card,
        .hw-spec-card,
        .panel-box {
            border: 1px solid #dbeafe;
            border-radius: 18px;
            background: #ffffff;
            padding: 1rem;
            box-shadow: 0 10px 24px rgba(15, 23, 42, 0.055);
            page-break-inside: avoid;
        }

        .insight-card h5,
        .deliverable-card h5,
        .next-step-card h5 {
            font-size: 0.96rem;
            font-weight: 900;
            color: #0f172a;
            margin-bottom: 0.35rem;
        }

        .insight-card p,
        .deliverable-card p,
        .next-step-card p {
            font-size: 0.91rem;
            color: var(--text-soft);
            margin: 0;
            text-align: left;
        }

        .icon-dot {
            width: 34px;
            height: 34px;
            border-radius: 13px;
            display: inline-grid;
            place-items: center;
            color: #ffffff;
            background: linear-gradient(135deg, #0284c7, #0f172a);
            margin-bottom: 0.5rem;
        }

        .pricing-table {
            width: 100%;
            border-collapse: separate;
            border-spacing: 0;
            overflow: hidden;
            border: 1px solid #dbeafe;
            border-radius: 18px;
            margin: 0.85rem 0 1.15rem;
            font-size: 0.92rem;
            page-break-inside: avoid;
        }

        .pricing-table th {
            background: linear-gradient(135deg, #0f172a, #0284c7);
            color: #ffffff;
            padding: 0.72rem 0.8rem;
            text-align: left;
            font-weight: 900;
        }

        .pricing-table td {
            padding: 0.72rem 0.8rem;
            border-top: 1px solid #e2e8f0;
            vertical-align: top;
            background: #ffffff;
        }

        .pricing-table tr:nth-child(odd) td {
            background: #f8fafc;
        }

        .negotiation-note {
            background:
                linear-gradient(135deg, rgba(245,158,11,0.15), rgba(255,255,255,0.98)),
                #fffbeb;
            padding: 1rem 1.1rem;
            border-left: 5px solid var(--accent-color);
            border-radius: 0 18px 18px 0;
            margin-bottom: 1rem;
            font-size: 0.98rem;
            box-shadow: 0 12px 22px rgba(245,158,11,0.08);
            page-break-inside: avoid;
        }

        .implementation-timeline {
            counter-reset: step;
            margin: 0.75rem 0 1.15rem;
            padding: 0;
            list-style: none;
            page-break-inside: avoid;
        }

        .implementation-timeline li {
            counter-increment: step;
            position: relative;
            padding: 0.7rem 0.85rem 0.7rem 3rem;
            border: 1px solid #e2e8f0;
            border-radius: 16px;
            margin-bottom: 0.55rem;
            background: #ffffff;
            box-shadow: 0 8px 18px rgba(15,23,42,0.04);
        }

        .implementation-timeline li::before {
            content: counter(step);
            position: absolute;
            left: 0.85rem;
            top: 0.68rem;
            width: 1.55rem;
            height: 1.55rem;
            border-radius: 999px;
            display: grid;
            place-items: center;
            background: #0f172a;
            color: #ffffff;
            font-weight: 900;
            font-size: 0.78rem;
        }

        .implementation-timeline strong {
            color: var(--primary-color);
        }

        .hw-spec-card {
            min-height: 100%;
            background:
                radial-gradient(circle at top right, rgba(34,197,94,0.12), transparent 32%),
                #ffffff;
        }

        .hw-spec-card h6 {
            font-size: 0.95rem;
        }

        .alert-info {
            border-radius: 18px !important;
            border: 1px solid #bae6fd !important;
        }

        .closing-callout {
            border-radius: 22px;
            padding: 1.1rem 1.2rem;
            margin: 1.15rem 0;
            color: #ffffff;
            background:
                linear-gradient(135deg, rgba(15,23,42,0.96), rgba(2,132,199,0.92)),
                #0f172a;
            page-break-inside: avoid;
            text-align: left;
        }

        .closing-callout h4 {
            font-size: 1.08rem;
            font-weight: 900;
            margin-bottom: 0.45rem;
        }

        .closing-callout p {
            color: rgba(255,255,255,0.92);
            margin-bottom: 0;
        }

        /* Tanda Tangan */
        .signature-section {
            margin-top: 3.2rem;
            text-align: right;
            font-size: 1rem;
            page-break-inside: avoid;
        }

        .signature-space {
            height: 82px;
        }

        .signature-name {
            font-weight: 900;
            text-decoration: underline;
            color: var(--primary-color);
        }

        .document-footer-note {
            margin-top: 2rem;
            padding-top: 0.85rem;
            border-top: 1px dashed #cbd5e1;
            color: var(--muted-color);
            font-size: 0.78rem;
            text-align: center;
            page-break-inside: avoid;
        }

        .btn-print {
            position: fixed;
            /* 7.5rem supaya lolos dari tombol "Tanya Jawab" (bantuan_button.jsp:
               right:16px; bottom:62px). */
            bottom: 7.5rem;
            right: 2rem;
            z-index: 1000;
            border-radius: 999px;
            padding: 0.9rem 1.35rem;
            box-shadow: 0 16px 34px rgba(2, 132, 199, 0.28);
            font-family: "Segoe UI", sans-serif;
            border: 0;
            background: linear-gradient(135deg, #0284c7, #0f172a);
        }

        .btn-print:hover {
            filter: brightness(1.05);
            transform: translateY(-1px);
        }

        @page {
            size: A4;
            margin: 12mm;
        }

        @media (max-width: 900px) {
            .letter-paper {
                width: calc(100% - 1.2rem);
                padding: 1.6rem 1.1rem;
                border-radius: 18px;
            }

            .kop-surat,
            .meta-surat,
            .doc-quick-summary,
            .insight-grid,
            .deliverable-grid,
            .next-step-grid {
                grid-template-columns: 1fr;
            }

            .kop-surat {
                flex-direction: column;
                text-align: center;
            }

            .kop-text {
                text-align: center;
            }

            .meta-kanan {
                text-align: left;
                justify-content: flex-start;
                min-width: 0;
            }
        }

        @media print {
            body {
                background: #ffffff !important;
                -webkit-print-color-adjust: exact;
                print-color-adjust: exact;
            }

            .container {
                padding: 0 !important;
                margin: 0 !important;
                max-width: 100% !important;
            }

            .letter-paper {
                width: 100%;
                max-width: 100%;
                margin: 0;
                padding: 0;
                box-shadow: none;
                border: 0;
                border-radius: 0;
            }

            .letter-paper::before {
                height: 6px;
            }

            .btn-print,
            .letter-watermark {
                display: none !important;
            }

            .highlight-list li,
            .insight-card,
            .deliverable-card,
            .next-step-card,
            .hw-spec-card,
            .panel-box,
            .executive-summary {
                box-shadow: none !important;
            }

            a {
                color: inherit;
                text-decoration: none;
            }
        }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/baru/include/pemilih_bahasa.jsp" />

    <button onclick="window.print()" class="btn btn-primary btn-print fw-bold">
        <i class="fas fa-print me-2"></i> Cetak / Simpan PDF
    </button>

    <div class="container">
        <div class="letter-paper">
            <div class="letter-watermark"></div>
            <div class="document-badge-row">
                <span class="doc-badge"><i class="fas fa-file-signature"></i> Surat Penawaran Resmi</span>
                <span class="doc-badge"><i class="fas fa-shield-halved"></i> Dokumen Siap Cetak</span>
            </div>
            
            <div class="kop-surat">
                <img src="<%=logo_PerguruanTinggi%>" alt="Logo" class="kop-logo">
                <div class="kop-text">
                    <div class="kop-title"><%=judul%></div>
                    <div class="kop-subtitle">Pusat Layanan Sistem Informasi Terpadu (Enterprise Education)</div>
                    <div class="kop-address">
                        Telepon/WA: <%=kontakPenyedia%> | Email: <%=Email%>
                    </div>
                </div>
            </div>

            <div class="meta-surat">
                <div class="meta-kiri">
                    <table border="0" cellspacing="0" cellpadding="0">
                        <tr>
                            <td width="90">Nomor</td>
                            <td width="15">:</td>
                            <td><%=nomorSurat%></td>
                        </tr>
                        <tr>
                            <td>Lampiran</td>
                            <td>:</td>
                            <td>1 (Satu) Berkas Proposal & Draf PKS</td>
                        </tr>
                        <tr>
                            <td>Hal</td>
                            <td>:</td>
                            <td><strong>Penawaran Aplikasi Sistem Informasi Terintegrasi (<%=judul%>)</strong></td>
                        </tr>
                    </table>
                </div>
                <div class="meta-kanan">
                    <%=kotaSurat%>, <%=tanggalSurat%>
                </div>
            </div>

            <div class="doc-quick-summary">
                <div class="summary-pill">
                    <span class="label">Jenis Dokumen</span>
                    <span class="value">Penawaran Enterprise Education</span>
                </div>
                <div class="summary-pill">
                    <span class="label">Fokus Solusi</span>
                    <span class="value">Integrasi Akademik, Keuangan, SDM & Layanan Digital</span>
                </div>
                <div class="summary-pill">
                    <span class="label">Model Kerja Sama</span>
                    <span class="value">Fleksibel, Terukur & Dapat Dinegosiasikan</span>
                </div>
            </div>

            <div class="recipient-section">
                Kepada Yth.<br>
                <strong>Ketua Yayasan / Direktur / Pimpinan</strong><br>
                <strong><%=namaClient%></strong><br>
                di Tempat.
            </div>

            <div class="letter-content">
                <p>Dengan hormat,</p>
                <p class="lead-paragraph"><strong>Bersama surat ini, kami menyampaikan penawaran solusi transformasi digital pendidikan yang dirancang untuk membantu institusi bergerak lebih cepat, tertata, akuntabel, dan siap menghadapi kebutuhan layanan pendidikan modern.</strong></p>
                <p>
                    Menghadapi tuntutan era digital dan kebutuhan penilaian akreditasi yang semakin dinamis, pengelolaan data akademik dan birokrasi yang cepat, valid, serta terintegrasi telah menjadi kunci utama bagi kemajuan sebuah institusi pendidikan. Kami menyadari bahwa operasional yang masih bersifat konvensional atau penggunaan sistem data yang saling terpisah (<i>silo data</i>) seringkali menghambat laju perkembangan institusi dan membebani kinerja staf pendidik maupun kependidikan.
                </p>
                <p>
                    Dalam rangka mendukung langkah reformasi tata kelola institusi di <strong><%=namaClient%></strong>, kami dari tim <strong><%=judul%></strong> mengajukan solusi <strong>Enterprise Education (Sistem Informasi Terpadu)</strong>. Aplikasi ini dirancang secara khusus untuk mendigitalkan seluruh siklus manajerial kampus/sekolah—mulai dari Penerimaan Peserta Didik Baru (PMB/PPDB), Pengisian Rencana Studi (KRS), Penilaian (LMS), Tata Kelola Keuangan, Manajemen Aset, hingga Pelaporan Nasional—hanya dalam satu pintu akses (<i>Single Sign-On</i>).
                </p>

                <div class="executive-summary">
                    <h4><i class="fas fa-star me-2 text-warning"></i>Ringkasan Eksekutif Penawaran</h4>
                    <p>
                        <strong><%=judul%></strong> tidak hanya diposisikan sebagai aplikasi administrasi, tetapi sebagai <strong>ekosistem digital pendidikan</strong> yang menghubungkan proses akademik, pembayaran, kepegawaian, persuratan, pelaporan, anjungan mandiri, layanan antar jemput, dan dashboard pimpinan ke dalam satu basis data yang konsisten.
                    </p>
                    <p>
                        Dengan pendekatan implementasi bertahap, institusi dapat memulai dari modul prioritas, kemudian berkembang menuju tata kelola digital yang lebih lengkap tanpa perlu membangun sistem secara terpisah-pisah.
                    </p>
                </div>

                <div class="insight-grid">
                    <div class="insight-card">
                        <span class="icon-dot"><i class="fas fa-layer-group"></i></span>
                        <h5>Satu Data, Banyak Layanan</h5>
                        <p>Data akademik, pembayaran, surat, SDM, dan laporan pimpinan dapat bergerak dalam satu alur kerja yang lebih rapi dan mudah diaudit.</p>
                    </div>
                    <div class="insight-card">
                        <span class="icon-dot"><i class="fas fa-chart-line"></i></span>
                        <h5>Lebih Cepat Mengambil Keputusan</h5>
                        <p>Dashboard manajemen membantu pimpinan melihat kondisi layanan, capaian, tunggakan, aktivitas, dan performa secara lebih cepat.</p>
                    </div>
                    <div class="insight-card">
                        <span class="icon-dot"><i class="fas fa-users-gear"></i></span>
                        <h5>Mendukung Banyak Pihak</h5>
                        <p>Sistem dirancang untuk pengguna internal maupun eksternal: pimpinan, operator, dosen/guru, mahasiswa/siswa, wali, bendahara, dan unit layanan.</p>
                    </div>
                    <div class="insight-card">
                        <span class="icon-dot"><i class="fas fa-shield-alt"></i></span>
                        <h5>Tertib, Terukur, dan Akuntabel</h5>
                        <p>Setiap proses dapat diarahkan menjadi lebih terdokumentasi, memiliki jejak aktivitas, dan mendukung kesiapan audit maupun akreditasi.</p>
                    </div>
                </div>
                
                <p><strong>Mengapa <%=judul%> adalah Pilihan Tepat untuk Institusi Anda?</strong></p>
                <ul class="highlight-list">
                    <li><strong>Terintegrasi & Terlengkap:</strong> Menyediakan ekosistem komprehensif yang mencakup Modul Akademik (mendukung Kurikulum OBE & Ganda), Keuangan & <i>Payment Gateway</i>, Tata Persuratan, Perpustakaan Digital (OPAC), Aset Institusi, hingga Kepegawaian (HRIS/Payroll).</li>
                    <li><strong>Dukungan 1-Click Sync (PDDIKTI & EMIS):</strong> Sinkronisasi data pelaporan nasional dapat dilakukan secara otomatis, membebaskan tim operator dari rutinitas <i>double-entry</i>.</li>
                    <li><strong>Pembayaran Digital Otomatis:</strong> Dilengkapi fasilitas <i>Host-to-Host</i> dengan jaringan Bank (Virtual Account) dan QRIS. Tagihan yang dibayarkan wali murid akan seketika mengubah status menjadi LUNAS di sistem tanpa perlu verifikasi manual.</li>
                    <li><strong>Modul Antar Jemput:</strong> Mendukung pengelolaan kendaraan, sopir, kenek, rute, jadwal, kartu penjemput, scan barcode/RFID di gerbang, monitor antrian, panggilan soundbox kelas, serta audit serah terima siswa/mahasiswa/peserta.</li>
                    <li><strong>Inovasi Anjungan Mandiri (Kiosk):</strong> Integrasi opsional dengan perangkat keras Anjungan (layar sentuh) untuk memungkinkan mahasiswa/siswa mencetak KHS, KRS, atau Surat Keterangan secara mandiri. Perangkat ditenagai Mini PC Industrial Intel i5, RAM 8GB DDR3, SSD 256GB, dan <i>Chasing</i> plat besi 2mm.</li>
                    <li><strong>Arsitektur UI/UX Ganda:</strong> Kebebasan memilih antarmuka <i>Tipe Klasik</i> (berbasis ZK Framework yang super stabil & ringan) atau <i>Tipe Modern</i> (berbasis Bootstrap dengan <i>export to Excel</i> dan visualisasi mutakhir).</li>
                </ul>

                <!-- ENHANCE_OBE_START -->
                <div class="panel-box mt-3">
                    <h4><i class="fas fa-bullseye me-2 text-primary"></i>Implementasi Kurikulum OBE Terintegrasi</h4>
                    <p>
                        Sistem mendukung implementasi <strong>Outcome Based Education (OBE)</strong> secara menyeluruh dengan hierarki capaian terstruktur sesuai standar nasional:
                    </p>
                    <ul class="highlight-list">
                        <li><strong>Hierarki Capaian OBE:</strong> Sub-CPMK → CPMK (Capaian Pembelajaran Mata Kuliah) → CPL (Capaian Pembelajaran Lulusan, 4 kategori SN-Dikti: <i>Sikap, Pengetahuan, Keterampilan Umum, Keterampilan Khusus</i>) → Profil Lulusan (PL) sebagai fondasi kurikulum program studi.</li>
                        <li><strong>3 Siklus Pemantauan Berkelanjutan:</strong> <i>Loop 1</i> — monitoring ketercapaian CPMK per semester (portofolio mata kuliah); <i>Loop 2</i> — evaluasi ketercapaian CPL per tahun akademik (laporan program studi); <i>Loop 3</i> — evaluasi Profil Lulusan setiap 3–5 tahun melalui Tracer Study alumni.</li>
                        <li><strong>Dasbor PIKOBE:</strong> Skor kesiapan OBE institusi 0–100 dengan tier Bronze (&lt;50), Silver (50–74), Gold (75–86), Platinum (≥87). Menghasilkan rekomendasi <i>Continuous Quality Improvement</i> (CQI) otomatis untuk peningkatan kurikulum yang terukur.</li>
                        <li><strong>RPS OBE Lengkap:</strong> Setiap RPS menyertakan pemetaan CPL-CPMK dengan bobot penilaian, minimal ketercapaian, instrumen penilaian autentik, dan rumusan Sub-CPMK terperinci.</li>
                    </ul>
                </div>
                <!-- ENHANCE_OBE_END -->


                <!-- ENHANCE_MODUL_ANTAR_JEMPUT_START -->
                <div class="panel-box">
                    <h4><i class="fas fa-shuttle-van me-2 text-primary"></i>Tambahan Modul Antar Jemput Terintegrasi</h4>
                    <p>
                        Untuk memperluas layanan operasional institusi, kami juga menambahkan modul <strong>Antar Jemput</strong> yang dapat digunakan oleh sekolah, kampus, pesantren, dan yayasan dalam mengelola kendaraan, rute, jadwal, kartu penjemput, monitor antrian, serta proses serah terima peserta secara lebih aman.
                    </p>
                    <div class="deliverable-grid">
                        <div class="deliverable-card">
                            <span class="icon-dot"><i class="fas fa-id-card"></i></span>
                            <h5>Kartu & Barcode Penjemput</h5>
                            <p>Petugas gerbang dapat melakukan tap kartu atau scan barcode untuk memvalidasi penjemput yang aktif dan sesuai jadwal.</p>
                        </div>
                        <div class="deliverable-card">
                            <span class="icon-dot"><i class="fas fa-route"></i></span>
                            <h5>Jadwal, Rute, dan Manifest</h5>
                            <p>Admin dapat mengatur kendaraan, sopir, kenek, rute, jadwal operasional, dan daftar peserta antar jemput.</p>
                        </div>
                        <div class="deliverable-card">
                            <span class="icon-dot"><i class="fas fa-volume-high"></i></span>
                            <h5>Monitor & Soundbox Kelas</h5>
                            <p>Nomor antrian tampil di monitor gerbang dan panggilan peserta dapat dikirim ke perangkat kelas.</p>
                        </div>
                        <div class="deliverable-card">
                            <span class="icon-dot"><i class="fas fa-shield-halved"></i></span>
                            <h5>Audit Serah Terima</h5>
                            <p>Histori scan, panggilan, waktu keluar kelas, dan konfirmasi serah terima tersimpan sebagai bukti keamanan.</p>
                        </div>
                    </div>
                </div>
                <!-- ENHANCE_MODUL_ANTAR_JEMPUT_END -->

                <div class="panel-box">
                    <h4><i class="fas fa-bullseye me-2 text-primary"></i>Nilai Strategis yang Diharapkan</h4>
                    <div class="deliverable-grid">
                        <div class="deliverable-card">
                            <h5><i class="fas fa-database me-2 text-primary"></i>Data Lebih Terpusat</h5>
                            <p>Mengurangi pencatatan berulang, memperkecil risiko data ganda, dan mempercepat pencarian informasi lintas unit.</p>
                        </div>
                        <div class="deliverable-card">
                            <h5><i class="fas fa-money-check-dollar me-2 text-success"></i>Pembayaran Lebih Transparan</h5>
                            <p>Tagihan, pembayaran, rekonsiliasi, dan laporan dapat dipantau lebih cepat oleh bagian keuangan maupun pimpinan.</p>
                        </div>
                        <div class="deliverable-card">
                            <h5><i class="fas fa-award me-2 text-warning"></i>Siap Mendukung Mutu</h5>
                            <p>Dokumentasi kegiatan, pelaporan, dan pengambilan data pendukung akreditasi dapat disiapkan lebih sistematis.</p>
                        </div>
                        <div class="deliverable-card">
                            <h5><i class="fas fa-mobile-screen-button me-2 text-info"></i>Layanan Lebih Modern</h5>
                            <p>Mahasiswa, siswa, orang tua, dan pegawai memperoleh pengalaman layanan yang lebih cepat, mandiri, dan informatif.</p>
                        </div>
                    </div>
                </div>

                <p><strong>Penawaran Skema Investasi Fleksibel</strong></p>
                <p>
                    Kami sangat memahami bahwa setiap institusi memiliki kebijakan rencana anggaran yang unik. Oleh karena itu, kami menawarkan skema pembiayaan lisensi yang dapat dipilih sesuai preferensi institusi:
                </p>
                <ul class="highlight-list">
                    <li><strong>Skema Berbasis Transaksi (Zero Investment):</strong> Institusi <u>tidak dikenakan biaya lisensi dan server bulanan</u>. Biaya operasional layanan ditanggung melalui biaya admin ringan (Rp 7.000) yang disematkan pada setiap transaksi pembayaran uang sekolah/kuliah siswa (minimal 2.000 mahasiswa/siswa aktif).</li>
                    <li><strong>Skema Berbasis Pengguna Aktif (Fixed Budget):</strong> Institusi menganggarkan biaya sewa lisensi yang pasti, yakni <strong>Rp 2.000,- / siswa / bulan</strong> (untuk sekolah) atau <strong>Rp 4.000,- / mahasiswa / bulan</strong> (untuk Perguruan Tinggi). Minimal tagihan dihitung berdasarkan 1.000 peserta didik.</li>
                </ul>

                <table class="pricing-table">
                    <thead>
                        <tr>
                            <th><%= Common.getBahasaConfig("Skema") %></th>
                            <th><%= Common.getBahasaConfig("Cocok Untuk") %></th>
                            <th><%= Common.getBahasaConfig("Nilai Utama") %></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><strong>Berbasis Transaksi</strong></td>
                            <td>Institusi yang ingin memulai digitalisasi dengan beban awal lebih ringan.</td>
                            <td>Lebih fleksibel, tidak membebani lisensi bulanan, dan dapat mengikuti volume transaksi.</td>
                        </tr>
                        <tr>
                            <td><strong>Berbasis Pengguna Aktif</strong></td>
                            <td>Institusi yang membutuhkan kepastian anggaran rutin dan prediktif.</td>
                            <td>Biaya lebih mudah direncanakan, cocok untuk penganggaran tahunan dan kontrol biaya.</td>
                        </tr>
                        <tr>
                            <td><strong>Hardware Opsional</strong></td>
                            <td>Institusi yang ingin meningkatkan layanan mandiri, kasir, atau titik layanan publik.</td>
                            <td>Meningkatkan citra modern, mempercepat pelayanan, dan mengurangi antrean layanan manual.</td>
                        </tr>
                    </tbody>
                </table>

                <div class="negotiation-note">
                    <strong>PENTING - Keterbukaan Negosiasi:</strong> Harga lisensi maupun harga pengadaan Perangkat Keras Anjungan (<i>Hardware</i>) yang mungkin tercantum di proposal dan draf PKS merupakan angka penawaran standar yang <strong>mungkin dapat berbeda sesuai kesepakatan akhir</strong>. Kami sangat fleksibel dan masih <strong>membuka skema negosiasi</strong>. Tujuan utama kami adalah agar sistem teknologi ini dapat segera diimplementasikan sehingga secepatnya membawa manfaat operasional yang nyata bagi institusi Anda.
                </div>

                <p>
                    Sebagai wujud komitmen layanan prima, seluruh skema di atas sudah termasuk <strong>Pelatihan (Training) Daring secara GRATIS</strong>. <i>(Untuk pendampingan tatap muka / offline di lokasi, biaya lisensi/jasa trainer tetap gratis, institusi hanya perlu memfasilitasi akomodasi dan transportasi tim ahli kami).</i>
                </p>

                <p><strong>Rencana Implementasi yang Terarah</strong></p>
                <p>
                    Agar proses adopsi sistem berjalan efektif, kami menyarankan pendekatan implementasi bertahap yang tetap fleksibel mengikuti prioritas, kesiapan data, dan kapasitas sumber daya institusi.
                </p>
                <ol class="implementation-timeline">
                    <li><strong>Analisis Kebutuhan & Pemetaan Proses:</strong> menyelaraskan modul, alur kerja, format laporan, serta kebutuhan integrasi yang paling prioritas.</li>
                    <li><strong>Persiapan Data & Konfigurasi Awal:</strong> menyiapkan struktur pengguna, hak akses, unit kerja, tahun akademik, akun pembayaran, dan konfigurasi dasar.</li>
                    <li><strong>Pelatihan Operator & Uji Coba Modul:</strong> memastikan tim internal memahami proses utama sebelum sistem digunakan lebih luas.</li>
                    <li><strong>Go-Live Bertahap:</strong> modul dapat diaktifkan secara bertahap untuk meminimalkan gangguan operasional harian.</li>
                    <li><strong>Pendampingan & Evaluasi:</strong> melakukan penyesuaian, monitoring, dan peningkatan sesuai kebutuhan institusi.</li>
                </ol>

                <div class="deliverable-grid">
                    <div class="deliverable-card">
                        <h5><i class="fas fa-chalkboard-user me-2 text-primary"></i>Training Pengguna</h5>
                        <p>Pelatihan daring untuk administrator/operator inti agar proses operasional dapat berjalan lebih mandiri.</p>
                    </div>
                    <div class="deliverable-card">
                        <h5><i class="fas fa-headset me-2 text-success"></i>Dukungan Layanan</h5>
                        <p>Pendampingan teknis untuk membantu adaptasi, koreksi data awal, serta penguatan tata kelola penggunaan sistem.</p>
                    </div>
                </div>

                <span class="section-hl"><i class="fas fa-microchip me-2"></i>Inovasi Perangkat Keras (Hardware Opsional):</span>
                <p>Selain perangkat lunak, kami menyediakan solusi perangkat keras industrial untuk modernisasi kampus dan sekolah. Hardware ini bersifat opsional, namun dapat menjadi nilai tambah untuk memperkuat layanan mandiri, kasir, cetak dokumen, dan pengalaman pengguna yang lebih modern:</p>
                
                <div class="row g-3">
                    <div class="col-md-6">
                        <div class="hw-spec-card">
                            <h6 class="fw-bold text-primary"><i class="fas fa-desktop me-2"></i>Anjungan Mandiri (ATM)</h6>
                            <ul class="list-unstyled small mb-0">
                                <li><i class="fas fa-caret-right me-1"></i> 24" Touchscreen + Industrial Mini PC i5</li>
                                <li><i class="fas fa-caret-right me-1"></i> RAM 8GB + SSD 256GB</li>
                                <li><i class="fas fa-caret-right me-1"></i> Printer Epson L120 (Cetak KRS/KHS)</li>
                                <li class="mt-2 text-primary fw-bold">IDR 37.5 jt - 42.5 jt</li>
                            </ul>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="hw-spec-card">
                            <h6 class="fw-bold text-success"><i class="fas fa-cash-register me-2"></i>ANFO POS DUAL (Mesin Kasir)</h6>
                            <ul class="list-unstyled small mb-0">
                                <li><i class="fas fa-caret-right me-1"></i> Layar Ganda (15.6" Touch + 10" Non-Touch)</li>
                                <li><i class="fas fa-caret-right me-1"></i> Intel Core i3 + RAM 16GB + SSD 512GB</li>
                                <li><i class="fas fa-caret-right me-1"></i> Thermal Printer + Cash Drawer RJ-11</li>
                                <li class="mt-2 text-success fw-bold">IDR 30.000.000 / Paket</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div class="alert alert-info border-0 mt-4 shadow-sm" style="background-color: #f0f9ff;">
                    <h6 class="fw-bold"><i class="fas fa-info-circle me-2"></i>Ketentuan Pengadaan Hardware:</h6>
                    <p class="small mb-0 text-dark">
                        Pengadaan Hardware bersifat opsional & di luar sewa Software. Harga belum termasuk Pajak & Ongkir luar Jabodetabek. <br>
                        <strong>Syarat: DP Rp 30 Juta di awal, proses manufaktur kurang lebih 1 bulan.</strong>
                    </p>
                </div>
                
                <div class="closing-callout">
                    <h4><i class="fas fa-handshake-angle me-2"></i>Usulan Tindak Lanjut</h4>
                    <p>
                        Kami merekomendasikan sesi presentasi dan <i>live demo</i> untuk menunjukkan alur sistem secara nyata, sekaligus memetakan modul yang paling tepat untuk kebutuhan <strong><%=namaClient%></strong>.
                    </p>
                </div>

                <div class="next-step-grid">
                    <div class="next-step-card">
                        <h5><i class="fas fa-calendar-check me-2 text-primary"></i>1. Jadwalkan Live Demo</h5>
                        <p>Tim kami akan memaparkan alur utama, dashboard, pembayaran, layanan akademik, dan contoh implementasi modul.</p>
                    </div>
                    <div class="next-step-card">
                        <h5><i class="fas fa-clipboard-list me-2 text-success"></i>2. Susun Prioritas Modul</h5>
                        <p>Bersama institusi, kami dapat menentukan modul yang paling mendesak untuk diterapkan lebih dahulu.</p>
                    </div>
                    <div class="next-step-card">
                        <h5><i class="fas fa-scale-balanced me-2 text-warning"></i>3. Finalisasi Skema Kerja Sama</h5>
                        <p>Biaya, ruang lingkup, dan rencana implementasi dapat disesuaikan melalui diskusi resmi.</p>
                    </div>
                    <div class="next-step-card">
                        <h5><i class="fas fa-rocket me-2 text-info"></i>4. Mulai Implementasi Bertahap</h5>
                        <p>Proses penerapan dapat dimulai dari konfigurasi inti, pelatihan, uji coba, kemudian go-live.</p>
                    </div>
                </div>

                <p>
                    Besar harapan kami dapat segera menjadwalkan presentasi langsung (<i>Live Demo</i>) dan berdiskusi lebih lanjut untuk merumuskan kemitraan terbaik dengan Bapak/Ibu.
                </p>
                <p>
                    Demikian penawaran ini kami sampaikan. Atas waktu dan perkenan Bapak/Ibu, kami mengucapkan terima kasih.
                </p>
            </div>

            <div class="signature-section">
                Hormat kami,<br>
                <strong>Tim Pengembang <%=judul%></strong>
                <div class="signature-space">
                    </div>
                <div class="signature-name"><%=namaPenyedia%></div>
                <div>Konsultan & Representatif Layanan</div>
            </div>

            <div class="document-footer-note">
                Dokumen ini disusun sebagai bahan penawaran awal dan dapat disesuaikan kembali berdasarkan kebutuhan, ruang lingkup modul, skema kerja sama, serta hasil pembahasan bersama calon mitra.
            </div>

        </div>
    </div>

    <script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
    <jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp" />
</body>
</html>