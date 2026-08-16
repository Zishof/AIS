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
String namaClient = (paramClient != null && !paramClient.trim().isEmpty()) ? paramClient : "Pimpinan / Direktur Rumah Sakit, Klinik, atau Puskesmas";
String namaPenyedia = (paramPresentBy != null && !paramPresentBy.trim().isEmpty()) ? paramPresentBy : "Tim Konsultan " + judul;
String kontakPenyedia = (paramTelp != null && !paramTelp.trim().isEmpty()) ? paramTelp : Telepon;
String kotaSurat = (paramKotaSurat != null && !paramKotaSurat.trim().isEmpty()) ? paramKotaSurat : "Jakarta";

// Generate Tanggal Hari Ini dengan Format Bahasa Indonesia
Calendar cal = Calendar.getInstance();
String tanggalSurat = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID")).format(cal.getTime());
String nomorSuratDefault = "0" + cal.get(Calendar.DAY_OF_MONTH) + "/SPH-EMEDIC/" + cal.get(Calendar.YEAR) + "/" + new SimpleDateFormat("MM").format(cal.getTime());
String nomorSurat = (paramNomorSurat != null && !paramNomorSurat.trim().isEmpty()) ? paramNomorSurat : nomorSuratDefault;
%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= Common.getBahasaConfig("Surat Penawaran - Modul Kesehatan (eMedic)") %> | <%=judul%></title>

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
            content: "eMedic - Modul Kesehatan";
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
                <span class="doc-badge"><i class="fas fa-shield-halved"></i> Modul Rumah Sakit / Klinik / Puskesmas</span>
            </div>

            <div class="kop-surat">
                <img src="<%=logo_PerguruanTinggi%>" alt="Logo" class="kop-logo">
                <div class="kop-text">
                    <div class="kop-title"><%=judul%></div>
                    <div class="kop-subtitle">Sistem Informasi Rumah Sakit, Klinik & Puskesmas Terintegrasi</div>
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
                            <td>1 (Satu) Berkas Proposal & Draf PKS Modul Kesehatan</td>
                        </tr>
                        <tr>
                            <td>Hal</td>
                            <td>:</td>
                            <td><strong>Penawaran Sistem Informasi Rumah Sakit/Klinik/Puskesmas Terpadu (eMedic - <%=judul%>)</strong></td>
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
                    <span class="value">Penawaran Modul eMedic (Kesehatan)</span>
                </div>
                <div class="summary-pill">
                    <span class="label">Fokus Solusi</span>
                    <span class="value">Registrasi, Rekam Medis, Farmasi, Billing & Dashboard RS/Klinik</span>
                </div>
                <div class="summary-pill">
                    <span class="label">Model Kerja Sama</span>
                    <span class="value">Fleksibel, Terukur & Dapat Dinegosiasikan</span>
                </div>
            </div>

            <div class="recipient-section">
                Kepada Yth.<br>
                <strong>Direktur / Kepala / Pimpinan</strong><br>
                <strong><%=namaClient%></strong><br>
                di Tempat.
            </div>

            <div class="letter-content">
                <p>Dengan hormat,</p>
                <p class="lead-paragraph"><strong>Bersama surat ini, kami menyampaikan penawaran solusi Sistem Informasi Rumah Sakit, Klinik, dan Puskesmas (eMedic) yang dirancang untuk membantu fasilitas layanan kesehatan bergerak lebih cepat, tertib, akuntabel, dan siap menghadapi kebutuhan layanan medis modern.</strong></p>
                <p>
                    Pengelolaan rekam medis, farmasi, stok obat, dan administrasi pasien secara konvensional seringkali menimbulkan risiko: tulisan resep yang sulit dibaca, stok obat yang tidak akurat, obat kadaluarsa yang terlambat terdeteksi, serta tagihan pasien yang tidak sinkron dengan tindakan atau obat yang diberikan. Kondisi ini dapat menghambat mutu layanan sekaligus membebani kinerja tenaga medis dan staf administrasi.
                </p>
                <p>
                    Dalam rangka mendukung langkah digitalisasi layanan kesehatan di <strong><%=namaClient%></strong>, kami dari tim <strong><%=judul%></strong> mengajukan solusi <strong>eMedic (Sistem Informasi Rumah Sakit/Klinik/Puskesmas Terpadu)</strong>. Modul ini merupakan bagian dari ekosistem <strong>Enterprise Education</strong> yang sama, sehingga jika Universitas (eCampus), Sekolah (eSchool), atau Pondok Pesantren (ePesantren) Anda memiliki klinik, rumah sakit, atau layanan medis sendiri, modul ini dapat <strong>langsung terintegrasi</strong> tanpa perlu sistem terpisah. eMedic juga dapat diimplementasikan secara berdiri sendiri (<i>standalone</i>) untuk rumah sakit, klinik, atau puskesmas yang tidak bernaung di bawah institusi pendidikan.
                </p>

                <div class="executive-summary">
                    <h4><i class="fas fa-star me-2 text-warning"></i>Ringkasan Eksekutif Penawaran</h4>
                    <p>
                        <strong>eMedic</strong> menyatukan seluruh proses kefasilitasan kesehatan — pendaftaran &amp; antrian, pemeriksaan &amp; rekam medis, farmasi &amp; racikan, gudang &amp; pengadaan, transaksi &amp; kasir, hingga dashboard analitik dan pelaporan — dalam satu basis data yang konsisten dan teraudit penuh.
                    </p>
                    <p>
                        Dengan pendekatan implementasi bertahap, fasilitas kesehatan dapat memulai dari modul prioritas (mis. pendaftaran dan rekam medis), kemudian berkembang menuju tata kelola digital yang lebih lengkap termasuk kesiapan integrasi BPJS/JKN dan SATUSEHAT Kemenkes di masa mendatang.
                    </p>
                </div>

                <div class="insight-grid">
                    <div class="insight-card">
                        <span class="icon-dot"><i class="fas fa-layer-group"></i></span>
                        <h5>Satu Data, Banyak Layanan</h5>
                        <p>Data pasien, resep, stok obat, tindakan, dan pembayaran bergerak dalam satu alur kerja yang lebih rapi dan mudah diaudit.</p>
                    </div>
                    <div class="insight-card">
                        <span class="icon-dot"><i class="fas fa-chart-line"></i></span>
                        <h5>Lebih Cepat Mengambil Keputusan</h5>
                        <p>Dashboard analitik membantu pimpinan melihat okupansi tempat tidur, pendapatan, diagnosa terbanyak, dan kadaluarsa farmasi secara real-time.</p>
                    </div>
                    <div class="insight-card">
                        <span class="icon-dot"><i class="fas fa-users-gear"></i></span>
                        <h5>Mendukung Banyak Pihak</h5>
                        <p>Sistem dirancang untuk dokter, perawat, farmasis, kasir, petugas gudang, dan manajemen dengan hak akses sesuai peran masing-masing.</p>
                    </div>
                    <div class="insight-card">
                        <span class="icon-dot"><i class="fas fa-shield-alt"></i></span>
                        <h5>Tertib, Terukur, dan Akuntabel</h5>
                        <p>Setiap perubahan data (stok, resep, tarif, rekam medis) tercatat lengkap: siapa, kapan, dan apa yang diubah — siap mendukung audit dan akreditasi.</p>
                    </div>
                </div>

                <p><strong>Mengapa eMedic adalah Pilihan Tepat untuk Fasilitas Kesehatan Anda?</strong></p>
                <ul class="highlight-list">
                    <li><strong>Terintegrasi dengan Ekosistem Pendidikan:</strong> Bagi Universitas, Sekolah, atau Pondok Pesantren yang memiliki klinik/poliklinik/UKS berskala klinik/rumah sakit sendiri, eMedic terhubung langsung dengan data pengguna (mahasiswa, siswa, santri, pegawai) tanpa duplikasi data.</li>
                    <li><strong>Alur Klinis Lengkap:</strong> Registrasi Rawat Jalan, Rawat Inap, dan UGD; pemeriksaan &amp; diagnosa dengan pengkodean ICD-10; resep digital hingga racikan/kompounding.</li>
                    <li><strong>Farmasi &amp; Gudang Terkendali:</strong> Stok obat real-time, manajemen kadaluarsa &amp; batch dengan prinsip FEFO (<i>First-Expired-First-Out</i>), serta hierarki gudang pusat-cabang.</li>
                    <li><strong>Billing &amp; Kasir Otomatis:</strong> Biaya obat, tindakan, alat medis, dan paket layanan terhitung otomatis menjadi satu tagihan terpadu, mendukung pembayaran tunai, non-tunai, dan deposit.</li>
                    <li><strong>Dashboard Analitik Siap Pakai:</strong> 7 dashboard operasional (ringkasan pendaftaran, kunjungan mingguan/bulanan, pendapatan, 10 diagnosa terbanyak, okupansi tempat tidur, kadaluarsa farmasi) untuk mendukung keputusan pimpinan.</li>
                    <li><strong>Siap untuk Masa Depan:</strong> Fondasi data untuk integrasi BPJS/JKN (VClaim, PCare, SEP) dan SATUSEHAT Kemenkes sedang disiapkan secara bertahap tanpa mengganggu operasional berjalan.</li>
                </ul>

                <div class="panel-box mt-3">
                    <h4><i class="fas fa-hospital me-2 text-primary"></i>Cakupan Fasilitas yang Didukung</h4>
                    <p>
                        eMedic dirancang untuk mendukung beragam skala fasilitas layanan kesehatan, baik yang berdiri sendiri maupun yang bernaung di bawah institusi pendidikan:
                    </p>
                    <ul class="highlight-list">
                        <li><strong>Rumah Sakit</strong> — mencakup rawat jalan, rawat inap, UGD, farmasi, gudang, dan billing lengkap.</li>
                        <li><strong>Klinik</strong> (umum maupun spesialis) — alur ringkas pendaftaran, pemeriksaan, resep, dan kasir.</li>
                        <li><strong>Puskesmas</strong> dan layanan fasilitas kesehatan masyarakat lainnya.</li>
                        <li><strong>Klinik/Poliklinik Kampus, Sekolah, atau Pondok Pesantren</strong> — terintegrasi langsung dengan data mahasiswa/siswa/santri pada ekosistem eCampus/eSchool/ePesantren yang sudah berjalan.</li>
                    </ul>
                </div>

                <div class="panel-box">
                    <h4><i class="fas fa-bullseye me-2 text-primary"></i>Nilai Strategis yang Diharapkan</h4>
                    <div class="deliverable-grid">
                        <div class="deliverable-card">
                            <h5><i class="fas fa-database me-2 text-primary"></i>Data Lebih Terpusat</h5>
                            <p>Mengurangi pencatatan berulang antara rekam medis, farmasi, dan kasir, serta mempercepat pencarian riwayat pasien.</p>
                        </div>
                        <div class="deliverable-card">
                            <h5><i class="fas fa-money-check-dollar me-2 text-success"></i>Pembayaran Lebih Transparan</h5>
                            <p>Tagihan, pembayaran, dan laporan pendapatan dapat dipantau lebih cepat oleh bagian kasir maupun pimpinan.</p>
                        </div>
                        <div class="deliverable-card">
                            <h5><i class="fas fa-award me-2 text-warning"></i>Siap Mendukung Mutu</h5>
                            <p>Jejak audit dan laporan siap pakai membantu proses audit internal maupun akreditasi fasilitas kesehatan.</p>
                        </div>
                        <div class="deliverable-card">
                            <h5><i class="fas fa-mobile-screen-button me-2 text-info"></i>Layanan Lebih Modern</h5>
                            <p>Pasien, dokter, dan staf memperoleh pengalaman layanan yang lebih cepat, tertib, dan informatif.</p>
                        </div>
                    </div>
                </div>

                <p><strong>Penawaran Skema Investasi Fleksibel</strong></p>
                <p>
                    Kami memahami bahwa setiap fasilitas kesehatan memiliki kebijakan anggaran yang berbeda-beda, mulai dari Klinik hingga Rumah Sakit Kelas A. Oleh karena itu, kami menawarkan <strong>tiga skema pembiayaan</strong> yang dapat dipilih sesuai preferensi, dan seluruh angka di bawah ini bersifat sebagai <strong>titik awal diskusi yang terbuka untuk dinegosiasikan</strong>. Prioritas utama kami adalah agar sistem ini dapat segera terealisasi dan digunakan, bukan kekakuan pada satu angka tertentu.
                </p>

                <span class="section-hl"><i class="fas fa-money-bill-wave me-2"></i>Skema 1: Beli Putus (Lisensi Permanen)</span>
                <p>
                    Fasilitas kesehatan membayar satu kali di muka dan sistem menjadi aset milik fasilitas kesehatan sepenuhnya untuk selamanya, tanpa kewajiban biaya bulanan. Harga sudah mencakup lisensi penggunaan seluruh modul paket, implementasi dasar, dan dukungan teknis selama tahun pertama.
                </p>
                <table class="pricing-table">
                    <thead>
                        <tr>
                            <th><%= Common.getBahasaConfig("Jenis Fasilitas") %></th>
                            <th><%= Common.getBahasaConfig("Harga Beli Putus (Mulai Dari)") %></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr><td><strong>Klinik</strong></td><td>Rp 100.000.000</td></tr>
                        <tr><td>Puskesmas Nonrawat Inap Dasar</td><td>Rp 150.000.000</td></tr>
                        <tr><td>Puskesmas Nonrawat Inap Lengkap / 24 Jam</td><td>Rp 225.000.000</td></tr>
                        <tr><td>Puskesmas Rawat Inap</td><td>Rp 325.000.000</td></tr>
                        <tr><td>Puskesmas Rawat Inap Terpencil</td><td>Rp 425.000.000</td></tr>
                        <tr><td>Puskesmas Sangat Terpencil / Offline-First</td><td>Rp 500.000.000</td></tr>
                        <tr><td><strong>Rumah Sakit Pratama</strong></td><td>Rp 250.000.000</td></tr>
                        <tr><td><strong>Rumah Sakit Kelas D</strong></td><td>Rp 400.000.000</td></tr>
                        <tr><td><strong>Rumah Sakit Kelas C</strong></td><td>Rp 750.000.000</td></tr>
                        <tr><td><strong>Rumah Sakit Kelas B</strong></td><td>Rp 1.400.000.000</td></tr>
                        <tr><td><strong>Rumah Sakit Kelas A</strong></td><td>Rp 2.750.000.000</td></tr>
                    </tbody>
                </table>
                <p style="font-size: 0.92rem; color: var(--muted-color);">
                    <i class="fas fa-circle-info me-1"></i> Harga merupakan lisensi permanen untuk <strong>satu lokasi fasilitas</strong>, sudah termasuk implementasi dasar dan dukungan tahun pertama. Infrastruktur server, migrasi data yang kompleks, integrasi dengan alat medis, kustomisasi khusus, serta penempatan tenaga pendamping di lokasi (onsite) diperhitungkan secara terpisah sesuai kebutuhan.
                </p>

                <span class="section-hl"><i class="fas fa-calendar-check me-2"></i>Skema 2: Langganan Bulanan</span>
                <p>
                    Fasilitas kesehatan tidak perlu mengeluarkan investasi besar di muka. Biaya dibayarkan secara rutin setiap bulan dengan nilai yang telah disesuaikan agar sebanding dengan skema Beli Putus (kurang lebih setara nilai Beli Putus apabila dijumlahkan selama 40 bulan).
                </p>
                <table class="pricing-table">
                    <thead>
                        <tr>
                            <th><%= Common.getBahasaConfig("Jenis Fasilitas") %></th>
                            <th><%= Common.getBahasaConfig("Biaya Langganan / Bulan") %></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr><td><strong>Klinik</strong></td><td>Rp 2.500.000</td></tr>
                        <tr><td>Puskesmas Nonrawat Inap Dasar</td><td>Rp 4.000.000</td></tr>
                        <tr><td>Puskesmas Nonrawat Inap Lengkap / 24 Jam</td><td>Rp 5.500.000</td></tr>
                        <tr><td>Puskesmas Rawat Inap</td><td>Rp 8.000.000</td></tr>
                        <tr><td>Puskesmas Rawat Inap Terpencil</td><td>Rp 10.500.000</td></tr>
                        <tr><td>Puskesmas Sangat Terpencil / Offline-First</td><td>Rp 12.500.000</td></tr>
                        <tr><td><strong>Rumah Sakit Pratama</strong></td><td>Rp 6.500.000</td></tr>
                        <tr><td><strong>Rumah Sakit Kelas D</strong></td><td>Rp 10.000.000</td></tr>
                        <tr><td><strong>Rumah Sakit Kelas C</strong></td><td>Rp 20.000.000</td></tr>
                        <tr><td><strong>Rumah Sakit Kelas B</strong></td><td>Rp 35.000.000</td></tr>
                        <tr><td><strong>Rumah Sakit Kelas A</strong></td><td>Rp 70.000.000</td></tr>
                    </tbody>
                </table>
                <p style="font-size: 0.92rem; color: var(--muted-color);">
                    <i class="fas fa-circle-info me-1"></i> Biaya langganan sudah mencakup: lisensi seluruh modul paket, layanan hosting/cloud standar, pencadangan (backup) data berkala, pemeliharaan aplikasi, pembaruan minor, dukungan teknis jarak jauh, dan pembaruan integrasi standar untuk <strong>satu lokasi fasilitas</strong>. Perangkat keras, server lokal, kunjungan tenaga pendamping ke lokasi, integrasi alat medis, penyimpanan citra medis (PACS), migrasi data berskala besar, dan pengembangan fitur khusus ditagihkan secara terpisah.
                </p>

                <span class="section-hl"><i class="fas fa-chart-pie me-2"></i>Skema 3: Fee Layanan Sistem per Transaksi</span>
                <p>
                    Skema ini paling sesuai bagi fasilitas kesehatan yang ingin memulai <strong>tanpa biaya besar di muka sama sekali</strong>. Kami sengaja menghindari pemakaian persentase besar dari total pendapatan (misalnya persentase langsung dari seluruh nilai transaksi), karena secara matematis dapat menghasilkan tagihan yang jauh lebih besar daripada skema langganan dan memberatkan fasilitas kesehatan. Sebagai gantinya, kami merekomendasikan skema yang lebih proporsional dan mudah diperiksa (diaudit) sebagai berikut.
                </p>
                <div class="panel-box mt-3">
                    <h4><i class="fas fa-clinic-medical me-2 text-primary"></i>Rekomendasi Skema untuk Klinik</h4>
                    <ul class="highlight-list mb-2">
                        <li><strong>Fee Tindakan Medis:</strong> 0,5% dari pendapatan bersih tindakan (setelah dikurangi jasa profesional dokter, bahan medis habis pakai, diskon, dan pembatalan/refund).</li>
                        <li><strong>Fee Obat/Farmasi:</strong> 0,25% dari penjualan bersih obat (setelah dikurangi diskon, retur, dan pajak).</li>
                        <li><strong>Batas Aman:</strong> tagihan bulanan minimum <strong>Rp 2.500.000</strong> dan maksimum <strong>Rp 7.500.000</strong> per lokasi klinik, agar biaya tetap terkendali baik pada bulan sepi maupun bulan ramai.</li>
                        <li><strong>Alternatif yang Lebih Sederhana:</strong> apabila klinik tidak ingin membuka data pendapatan/margin, dapat dipilih tarif tetap <strong>Rp 500 per registrasi kunjungan yang berhasil</strong>, dengan batas minimum dan maksimum yang sama.</li>
                    </ul>
                </div>
                <div class="panel-box">
                    <h4><i class="fas fa-hospital me-2 text-primary"></i>Rekomendasi Skema untuk Puskesmas &amp; Rumah Sakit</h4>
                    <p>Dihitung berdasarkan <strong>fee per registrasi kunjungan yang berhasil</strong>, dengan tagihan minimum dan maksimum bulanan sebagai jaring pengaman bagi kedua belah pihak:</p>
                    <table class="pricing-table">
                        <thead>
                            <tr>
                                <th><%= Common.getBahasaConfig("Jenis Fasilitas") %></th>
                                <th><%= Common.getBahasaConfig("Tagihan Minimum") %></th>
                                <th><%= Common.getBahasaConfig("Fee per Registrasi") %></th>
                                <th><%= Common.getBahasaConfig("Tagihan Maksimum") %></th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr><td>Puskesmas Nonrawat Inap</td><td>Rp 4.000.000</td><td>Rp 500</td><td>Rp 10.000.000</td></tr>
                            <tr><td>Puskesmas Nonrawat Inap Lengkap</td><td>Rp 5.500.000</td><td>Rp 750</td><td>Rp 15.000.000</td></tr>
                            <tr><td>Puskesmas Rawat Inap</td><td>Rp 8.000.000</td><td>Rp 1.000</td><td>Rp 20.000.000</td></tr>
                            <tr><td>Puskesmas Terpencil</td><td>Rp 10.500.000</td><td>Rp 1.250</td><td>Rp 25.000.000</td></tr>
                            <tr><td>Puskesmas Sangat Terpencil</td><td>Rp 12.500.000</td><td>Rp 1.500</td><td>Rp 30.000.000</td></tr>
                            <tr><td>Rumah Sakit Pratama</td><td>Rp 6.500.000</td><td>Rp 1.000</td><td>Rp 20.000.000</td></tr>
                            <tr><td>Rumah Sakit Kelas D</td><td>Rp 10.000.000</td><td>Rp 1.250</td><td>Rp 30.000.000</td></tr>
                            <tr><td>Rumah Sakit Kelas C</td><td>Rp 20.000.000</td><td>Rp 2.500 (atau skema gabungan)</td><td>Rp 60.000.000</td></tr>
                            <tr><td>Rumah Sakit Kelas B</td><td>Biaya dasar Rp 20.000.000 + Rp 2.000/registrasi</td><td>&mdash;</td><td>Rp 100.000.000</td></tr>
                            <tr><td>Rumah Sakit Kelas A</td><td>Biaya dasar Rp 40.000.000 + Rp 3.500/registrasi</td><td>&mdash;</td><td>Rp 200.000.000</td></tr>
                        </tbody>
                    </table>
                </div>

                <div class="executive-summary">
                    <h4><i class="fas fa-clipboard-question me-2 text-primary"></i>Yang Dimaksud "Satu Transaksi Berbayar"</h4>
                    <p>Agar tidak menimbulkan salah paham di kemudian hari, satu transaksi yang dihitung dalam skema Fee Layanan Sistem adalah <strong>satu registrasi kunjungan pasien yang berhasil, memiliki nomor kunjungan, dan sudah mendapatkan pelayanan</strong>. Yang <u>tidak</u> dihitung sebagai transaksi berbayar antara lain: pendaftaran yang dibatalkan, percobaan/uji coba, duplikasi pendaftaran, koreksi administrasi, aktivitas membuka aplikasi tanpa mendaftar, pembatalan oleh petugas, data pelatihan/simulasi, dan sinkronisasi ulang data lama. Khusus rawat inap, hanya dihitung satu kali pada saat admisi (bukan dihitung setiap hari selama pasien dirawat).</p>
                </div>

                <div class="panel-box">
                    <h4><i class="fas fa-shield-heart me-2 text-primary"></i>Prinsip Kepatuhan &amp; Etika Skema Fee Layanan Sistem</h4>
                    <ul class="highlight-list mb-0">
                        <li><strong>Bukan Pungutan kepada Pasien:</strong> Fee Layanan Sistem merupakan hubungan bisnis (B2B) antara fasilitas kesehatan dan penyedia sistem, dibayarkan dari anggaran operasional/pendapatan fasilitas kesehatan sesuai kontrak — <u>bukan</u> pungutan tambahan otomatis yang dibebankan kepada pasien, terutama peserta JKN/BPJS.</li>
                        <li><strong>Tidak Memengaruhi Keputusan Medis:</strong> Besaran fee tidak boleh menjadi dasar bagi dokter maupun tenaga kesehatan untuk menambah tindakan, pemeriksaan, atau jumlah obat. Informasi fee hanya dapat diakses oleh bagian manajemen dan keuangan, tidak ditampilkan kepada tenaga medis sebagai bentuk insentif.</li>
                        <li><strong>Kepatuhan Regulasi Tarif JKN:</strong> Skema ini disusun dengan memperhatikan ketentuan standar tarif pelayanan program Jaminan Kesehatan Nasional sebagaimana diatur oleh Kementerian Kesehatan, sehingga tidak bertentangan dengan aturan pembiayaan JKN yang berlaku.</li>
                        <li><strong>Perlindungan Data Pasien:</strong> Seluruh data rekam medis dan data pribadi pasien diperlakukan sesuai dengan Undang-Undang Pelindungan Data Pribadi dan ketentuan rekam medis yang berlaku, dengan fasilitas kesehatan berkedudukan sebagai pengendali data dan penyedia sistem berkedudukan sebagai pemroses data.</li>
                    </ul>
                </div>

                <div class="negotiation-note">
                    <strong>PENTING - Keterbukaan Negosiasi:</strong> Seluruh nilai investasi, tarif langganan, maupun persentase/fee bagi hasil yang tercantum pada surat ini, proposal, dan draf PKS merupakan angka penawaran standar yang <strong>sepenuhnya dapat dinegosiasikan</strong> sesuai kesepakatan akhir dan kondisi riil fasilitas kesehatan. Kami mengutamakan tercapainya kesepakatan dan cepatnya sistem dapat direalisasikan, dibandingkan kekakuan pada satu angka tertentu.
                </div>

                <p>
                    Sebagai wujud komitmen layanan prima, seluruh skema di atas sudah termasuk <strong>Pelatihan (Training) Daring secara GRATIS</strong> untuk tenaga medis, farmasis, kasir, dan petugas gudang. <i>(Untuk pendampingan tatap muka/offline di lokasi, biaya jasa trainer tetap gratis, fasilitas kesehatan hanya perlu memfasilitasi akomodasi dan transportasi tim ahli kami).</i>
                </p>

                <p><strong>Rencana Implementasi yang Terarah</strong></p>
                <p>
                    Agar proses adopsi sistem berjalan efektif, kami menyarankan pendekatan implementasi bertahap yang tetap fleksibel mengikuti prioritas, kesiapan data, dan kapasitas sumber daya fasilitas kesehatan.
                </p>
                <ol class="implementation-timeline">
                    <li><strong>Analisis Kebutuhan &amp; Pemetaan Alur Klinis:</strong> menyelaraskan modul, alur pendaftaran-pemeriksaan-farmasi-kasir, serta prioritas kebutuhan.</li>
                    <li><strong>Persiapan Data &amp; Konfigurasi Awal:</strong> menyiapkan struktur pengguna, hak akses, master item obat, tarif, poli/dokter, dan kelas perawatan.</li>
                    <li><strong>Pelatihan Tenaga Medis &amp; Uji Coba Modul:</strong> memastikan dokter, perawat, farmasis, dan kasir memahami alur sebelum sistem digunakan lebih luas.</li>
                    <li><strong>Go-Live Bertahap:</strong> modul dapat diaktifkan bertahap (mis. pendaftaran &amp; rekam medis dahulu, lalu farmasi dan billing) untuk meminimalkan gangguan operasional harian.</li>
                    <li><strong>Pendampingan &amp; Evaluasi:</strong> penyesuaian, monitoring, dan peningkatan sesuai kebutuhan fasilitas kesehatan.</li>
                </ol>

                <div class="deliverable-grid">
                    <div class="deliverable-card">
                        <h5><i class="fas fa-chalkboard-user me-2 text-primary"></i>Training Pengguna</h5>
                        <p>Pelatihan daring untuk dokter, perawat, farmasis, dan kasir agar proses operasional dapat berjalan lebih mandiri.</p>
                    </div>
                    <div class="deliverable-card">
                        <h5><i class="fas fa-headset me-2 text-success"></i>Dukungan Layanan</h5>
                        <p>Pendampingan teknis untuk membantu adaptasi, koreksi data awal, serta penguatan tata kelola penggunaan sistem.</p>
                    </div>
                </div>

                <span class="section-hl"><i class="fas fa-route me-2"></i>Roadmap Integrasi BPJS/JKN &amp; SATUSEHAT:</span>
                <p>Fondasi data untuk integrasi eksternal disiapkan secara bertahap dan aditif — tidak mengganggu operasional yang sedang berjalan: <strong>Fase 1</strong> Fondasi Data (NIK, nomor kartu BPJS, kepesertaan pasien); <strong>Fase 2</strong> BPJS Eligibilitas (VClaim, PCare, penerbitan SEP); <strong>Fase 3</strong> Integrasi SATUSEHAT Kemenkes; <strong>Fase 4</strong> Klaim INA-CBG. Fase 1 saat ini sedang dalam pengembangan.</p>

                <div class="closing-callout">
                    <h4><i class="fas fa-handshake-angle me-2"></i>Usulan Tindak Lanjut</h4>
                    <p>
                        Kami merekomendasikan sesi presentasi dan pemaparan modul untuk menunjukkan alur sistem secara nyata, sekaligus memetakan modul yang paling tepat untuk kebutuhan <strong><%=namaClient%></strong>.
                    </p>
                </div>

                <div class="panel-box">
                    <h4><i class="fas fa-desktop me-2"></i>Coba Demo eMedic Secara Langsung</h4>
                    <p>Sebelum memutuskan skema kerja sama, Bapak/Ibu dapat langsung mencoba sistem eMedic secara mandiri melalui tautan demo berikut:</p>
                    <p style="margin-bottom: 4px;">Tautan Demo: <a href="https://apps.emedik.id" target="_blank"><strong>https://apps.emedik.id</strong></a></p>
                    <p style="margin-bottom: 4px;">Username: <strong>demo</strong></p>
                    <p style="margin-bottom: 0;">Password: <strong>demo123</strong></p>
                </div>

                <div class="next-step-grid">
                    <div class="next-step-card">
                        <h5><i class="fas fa-calendar-check me-2 text-primary"></i>1. Jadwalkan Presentasi</h5>
                        <p>Tim kami akan memaparkan alur registrasi, rekam medis, farmasi, kasir, dan dashboard eMedic.</p>
                    </div>
                    <div class="next-step-card">
                        <h5><i class="fas fa-clipboard-list me-2 text-success"></i>2. Susun Prioritas Modul</h5>
                        <p>Bersama fasilitas kesehatan, kami menentukan modul yang paling mendesak untuk diterapkan lebih dahulu.</p>
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
                    Besar harapan kami dapat segera menjadwalkan presentasi langsung dan berdiskusi lebih lanjut untuk merumuskan kemitraan terbaik dengan Bapak/Ibu.
                </p>
                <p>
                    Demikian penawaran ini kami sampaikan. Atas waktu dan perkenan Bapak/Ibu, kami mengucapkan terima kasih.
                </p>
            </div>

            <div class="signature-section">
                Hormat kami,<br>
                <strong>Tim Pengembang <%=judul%> - Modul eMedic</strong>
                <div class="signature-space">
                    </div>
                <div class="signature-name"><%=namaPenyedia%></div>
                <div>Konsultan & Representatif Layanan</div>
            </div>

            <div class="document-footer-note">
                Dokumen ini disusun sebagai bahan penawaran awal modul kesehatan (eMedic) dan dapat disesuaikan kembali berdasarkan kebutuhan, ruang lingkup modul, skema kerja sama, serta hasil pembahasan bersama calon mitra.
            </div>

        </div>
    </div>

    <script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
    <jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp" />
</body>
</html>
