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
String namaClient = (paramClient != null && !paramClient.trim().isEmpty()) ? paramClient : "Pimpinan / Direktur Unit Usaha, Koperasi, atau Kantin";
String namaPenyedia = (paramPresentBy != null && !paramPresentBy.trim().isEmpty()) ? paramPresentBy : "Tim Konsultan " + judul;
String kontakPenyedia = (paramTelp != null && !paramTelp.trim().isEmpty()) ? paramTelp : Telepon;
String kotaSurat = (paramKotaSurat != null && !paramKotaSurat.trim().isEmpty()) ? paramKotaSurat : "Jakarta";

// Generate Tanggal Hari Ini dengan Format Bahasa Indonesia
Calendar cal = Calendar.getInstance();
String tanggalSurat = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID")).format(cal.getTime());
String nomorSuratDefault = "0" + cal.get(Calendar.DAY_OF_MONTH) + "/SPH-GUDANG/" + cal.get(Calendar.YEAR) + "/" + new SimpleDateFormat("MM").format(cal.getTime());
String nomorSurat = (paramNomorSurat != null && !paramNomorSurat.trim().isEmpty()) ? paramNomorSurat : nomorSuratDefault;
%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= Common.getBahasaConfig("Surat Penawaran - Sistem Gudang Pusat, Outlet, POS, Ekspedisi, Investor & Akuntansi") %> | <%=judul%></title>

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
            content: "Sistem Gudang & Operasional Bisnis";
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
                <span class="doc-badge"><i class="fas fa-warehouse"></i> Modul Gudang, POS, Ekspedisi, Investor & Akuntansi</span>
            </div>

            <div class="kop-surat">
                <img src="<%=logo_PerguruanTinggi%>" alt="Logo" class="kop-logo">
                <div class="kop-text">
                    <div class="kop-title"><%=judul%></div>
                    <div class="kop-subtitle">Sistem Gudang Pusat, Outlet, POS, Ekspedisi, Investor & Akuntansi Terpadu</div>
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
                            <td>1 (Satu) Berkas Proposal & Draf PKS Modul Gudang & Operasional Bisnis</td>
                        </tr>
                        <tr>
                            <td>Hal</td>
                            <td>:</td>
                            <td><strong>Penawaran Sistem Gudang Pusat, Outlet, POS, Ekspedisi, Investor & Akuntansi Terpadu (<%=judul%>)</strong></td>
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
                    <span class="value">Penawaran Modul Gudang & Operasional Bisnis</span>
                </div>
                <div class="summary-pill">
                    <span class="label">Fokus Solusi</span>
                    <span class="value">Gudang, POS Kasir, HPP & Akuntansi, Pengadaan, Ekspedisi & Investor</span>
                </div>
                <div class="summary-pill">
                    <span class="label">Model Kerja Sama</span>
                    <span class="value">Fleksibel, Terukur & Dapat Dinegosiasikan</span>
                </div>
            </div>

            <div class="recipient-section">
                Kepada Yth.<br>
                <strong>Direktur / Pimpinan / Ketua Koperasi</strong><br>
                <strong><%=namaClient%></strong><br>
                di Tempat.
            </div>

            <div class="letter-content">
                <p>Dengan hormat,</p>
                <p class="lead-paragraph"><strong>Bersama surat ini, kami menyampaikan penawaran solusi Sistem Gudang Pusat, Outlet, Point of Sale (POS), Ekspedisi, Investor, dan Akuntansi yang dirancang untuk membantu unit usaha, koperasi, kantin, dan bisnis retail bergerak lebih cepat, tertib, akuntabel, dan siap bertumbuh secara berjenjang.</strong></p>
                <p>
                    Pengelolaan stok dan transaksi retail secara konvensional seringkali menimbulkan risiko: stok dua kasir yang tercatat ganda saat bersamaan, selisih kas yang baru diketahui di akhir bulan, harga pokok penjualan (HPP) yang dihitung manual dan sering keliru, serta pembagian hasil kepada investor yang dilakukan secara kasar dari omzet kotor tanpa memperhitungkan biaya sebenarnya. Kondisi ini dapat menimbulkan kerugian senyap yang baru terasa saat rekonsiliasi dilakukan.
                </p>
                <p>
                    Dalam rangka mendukung tata kelola unit usaha yang lebih rapi di <strong><%=namaClient%></strong>, kami dari tim <strong><%=judul%></strong> mengajukan solusi <strong>Sistem Gudang Pusat, Outlet, POS, Ekspedisi, Investor & Akuntansi Terpadu</strong>. Modul ini merupakan bagian dari ekosistem <strong>Enterprise Education</strong> yang sama, sehingga jika Universitas (eCampus), Sekolah (eSchool), atau Pondok Pesantren (ePesantren) Anda memiliki unit usaha, koperasi, kantin, gudang, atau bisnis retail sendiri, modul ini dapat <strong>langsung terintegrasi</strong> dengan data mahasiswa/siswa/santri/pegawai yang sudah berjalan, tanpa perlu sistem terpisah atau login ganda. Modul ini juga dapat diimplementasikan secara berdiri sendiri (<i>standalone</i>) untuk jaringan toko, koperasi, atau bisnis multi-outlet yang tidak bernaung di bawah institusi pendidikan.
                </p>

                <div class="executive-summary">
                    <h4><i class="fas fa-star me-2 text-warning"></i>Ringkasan Eksekutif Penawaran</h4>
                    <p>
                        Sistem ini menyatukan seluruh proses operasional unit usaha — gudang pusat &amp; cabang, kasir POS multi-outlet, pengadaan &amp; penerimaan barang, harga pokok penjualan (HPP) &amp; akuntansi, hingga distribusi/ekspedisi dan bagi hasil investor — dalam satu basis data yang konsisten dan teraudit penuh.
                    </p>
                    <p>
                        Fondasi inti (Gudang, POS, HPP/Akuntansi, dan Pengadaan) telah dibangun dan telah melalui proses pengerasan (<i>hardening</i>) teknis. Modul Ekspedisi, Investor, dan Kepegawaian Outlet berada pada tahap arsitektur/desain lanjut dan menjadi bagian dari peta jalan (<i>roadmap</i>) pengembangan yang akan digulirkan bertahap tanpa mengganggu operasional yang sudah berjalan.
                    </p>
                </div>

                <div class="insight-grid">
                    <div class="insight-card">
                        <span class="icon-dot"><i class="fas fa-layer-group"></i></span>
                        <h5>Satu Data, Banyak Titik Layanan</h5>
                        <p>Data stok, transaksi kasir, penerimaan barang, dan jurnal akuntansi bergerak dalam satu alur kerja yang lebih rapi dan mudah diaudit.</p>
                    </div>
                    <div class="insight-card">
                        <span class="icon-dot"><i class="fas fa-wifi"></i></span>
                        <h5>Kasir Tetap Jalan Tanpa Internet</h5>
                        <p>POS berjalan offline-first (dapat dipasang sebagai aplikasi), transaksi tersimpan lokal dan tersinkron otomatis begitu koneksi kembali, tanpa transaksi ganda.</p>
                    </div>
                    <div class="insight-card">
                        <span class="icon-dot"><i class="fas fa-shield-halved"></i></span>
                        <h5>Stok Tidak Bisa Terjual Dua Kali</h5>
                        <p>Validasi stok di sisi server dengan penguncian baris (<i>row-locking</i>) mencegah dua kasir menjual barang yang sama secara bersamaan hingga stok minus.</p>
                    </div>
                    <div class="insight-card">
                        <span class="icon-dot"><i class="fas fa-chart-line"></i></span>
                        <h5>Angka yang Bisa Dipercaya</h5>
                        <p>Setiap perubahan stok, jurnal, dan transaksi tercatat lengkap dengan jejak dokumen sumbernya — tidak ada lagi penyesuaian stok atau jurnal "siluman".</p>
                    </div>
                </div>

                <p><strong>Mengapa Sistem Ini adalah Pilihan Tepat untuk Unit Usaha Anda?</strong></p>
                <ul class="highlight-list">
                    <li><strong>Terintegrasi dengan Ekosistem Pendidikan:</strong> Bagi Universitas, Sekolah, atau Pondok Pesantren yang memiliki koperasi, kantin, atau toko kampus sendiri, sistem ini terhubung langsung dengan data mahasiswa, siswa, santri, dan pegawai tanpa duplikasi data maupun login terpisah.</li>
                    <li><strong>Hierarki Gudang Pusat-Cabang:</strong> Struktur gudang berjenjang (pusat &amp; cabang) dengan banyak lokasi fisik yang dapat dilacak masing-masing, buku besar pergerakan stok (masuk/keluar/transfer/penyesuaian) yang teraudit penuh, dan stok opname (rekonsiliasi fisik) yang terekam rapi.</li>
                    <li><strong>POS Multi-Outlet Offline-First:</strong> Kasir tetap dapat bertransaksi walau internet terputus, sinkronisasi otomatis dan idempoten (anti duplikasi) saat koneksi kembali, lengkap dengan opsi buka/tutup kas per shift beserta rekonsiliasi selisih kas otomatis.</li>
                    <li><strong>HPP Dihitung Otomatis, Bukan Diketik Manual:</strong> Harga pokok penjualan untuk produk berbasis resep dihitung otomatis dari rincian bahan baku (bukan angka yang diketik manual seperti banyak sistem sejenis), sehingga margin yang ditampilkan benar-benar mencerminkan biaya riil.</li>
                    <li><strong>Akuntansi Terhubung Otomatis:</strong> Jurnal terposting otomatis dari transaksi POS/pengadaan, setiap baris jurnal dapat ditelusuri kembali ke dokumen sumbernya, dan tersedia dasbor "Draft Jurnal" untuk memantau status posting secara real-time.</li>
                    <li><strong>Pengadaan Barang yang Matang:</strong> Alur Permintaan Pembelian → Persetujuan → Purchase Order → Penerimaan Barang (BAST) → Pembayaran, dengan mesin alur persetujuan (<i>approval workflow</i>) yang sama dipakai lintas modul (pengadaan, penggajian, cuti), lengkap dengan data induk vendor/pemasok dan portal tersendiri.</li>
                </ul>

                <div class="panel-box mt-3">
                    <h4><i class="fas fa-store me-2 text-primary"></i>Cakupan Unit Usaha yang Didukung</h4>
                    <p>
                        Sistem ini dirancang untuk mendukung beragam skala unit usaha, baik yang berdiri sendiri maupun yang bernaung di bawah institusi pendidikan:
                    </p>
                    <ul class="highlight-list">
                        <li><strong>Koperasi &amp; Kantin Kampus/Sekolah/Pesantren</strong> — terintegrasi langsung dengan data mahasiswa/siswa/santri/pegawai pada ekosistem eCampus/eSchool/ePesantren yang sudah berjalan.</li>
                        <li><strong>Toko/Unit Usaha Milik Yayasan atau Institusi</strong> — dengan gudang pusat dan beberapa outlet cabang.</li>
                        <li><strong>Jaringan Ritel atau Koperasi Berdiri Sendiri</strong> (<i>standalone</i>) — bisnis multi-outlet yang tidak bernaung di bawah institusi pendidikan.</li>
                        <li><strong>Unit Usaha dengan Skema Investor</strong> — bisnis yang dibiayai bersama investor dan memerlukan perhitungan bagi hasil yang transparan dan berbasis laba bersih, bukan sekadar omzet kotor.</li>
                    </ul>
                </div>

                <div class="panel-box">
                    <h4><i class="fas fa-bullseye me-2 text-primary"></i>Nilai Strategis yang Diharapkan</h4>
                    <div class="deliverable-grid">
                        <div class="deliverable-card">
                            <h5><i class="fas fa-database me-2 text-primary"></i>Data Lebih Terpusat</h5>
                            <p>Mengurangi pencatatan berulang antara gudang, kasir, dan akuntansi, serta mempercepat rekonsiliasi stok dan kas.</p>
                        </div>
                        <div class="deliverable-card">
                            <h5><i class="fas fa-money-check-dollar me-2 text-success"></i>Margin yang Sebenarnya</h5>
                            <p>HPP otomatis membuat laporan laba-rugi per produk maupun per outlet lebih mencerminkan kondisi riil, bukan asumsi.</p>
                        </div>
                        <div class="deliverable-card">
                            <h5><i class="fas fa-scale-balanced me-2 text-warning"></i>Bagi Hasil yang Adil &amp; Transparan</h5>
                            <p>Fondasi keuangan yang rapi membuka jalan bagi distribusi bagi hasil investor yang dihitung dari laba bersih, bukan omzet kotor.</p>
                        </div>
                        <div class="deliverable-card">
                            <h5><i class="fas fa-mobile-screen-button me-2 text-info"></i>Layanan Lebih Modern</h5>
                            <p>Kasir, petugas gudang, dan pimpinan memperoleh pengalaman kerja yang lebih cepat, tertib, dan informatif — termasuk saat jaringan sedang tidak stabil.</p>
                        </div>
                    </div>
                </div>

                <div class="panel-box mt-3">
                    <h4><i class="fas fa-mobile-screen-button me-2 text-primary"></i>Sudah Tersedia: 3 Aplikasi Klien Siap Pakai</h4>
                    <p>
                        Modul Kasir (POS) &amp; Gudang di atas bukan sekadar rencana pengembangan — tiga aplikasi klien pendukungnya sudah selesai dibangun, sudah dirilis resmi, dan dapat langsung diunduh serta dipasang untuk uji coba tanpa menunggu tahap implementasi:
                    </p>
                    <ul class="highlight-list">
                        <li><strong>POS Kasir Desktop (Windows)</strong> — kasir andal di meja kasir utama, tetap melayani pembeli walau internet putus, dilengkapi Layar Pelanggan kedua, pembayaran saldo member dengan PIN, lebih dari 150 laporan siap pakai, dan pembaruan otomatis.</li>
                        <li><strong>POS Kasir Android (tablet &amp; HP)</strong> — mesin kasir dalam genggaman, tampilan menyesuaikan otomatis ke tablet maupun HP biasa, cetak struk ke printer thermal Bluetooth, tetap dapat berjualan tanpa sinyal.</li>
                        <li><strong>Stok Opname Android</strong> — aplikasi khusus perhitungan stok fisik ala supermarket/minimarket, memindai barcode via alat pemindai genggam atau kamera HP, fokus pada satu tugas agar tim lapangan cepat mahir.</li>
                    </ul>
                    <p class="mb-0">
                        Tautan unduh resmi (GitHub Releases, gratis):
                        <a href="https://github.com/Zishof/ais-pos-kasir-desktop/releases" target="_blank" rel="noopener">POS Kasir Desktop</a> &middot;
                        <a href="https://github.com/Zishof/ais-pos-kasir-android/releases" target="_blank" rel="noopener">POS Kasir Android</a> &middot;
                        <a href="https://github.com/Zishof/ais-stok-opname-android/releases" target="_blank" rel="noopener">Stok Opname Android</a>
                    </p>
                </div>

                <p><strong>Skema Harga Modular: POS per Outlet + Paket Modul Pusat</strong></p>
                <p>
                    Kami memahami bahwa setiap unit usaha memiliki skala dan kebijakan anggaran yang berbeda. Oleh karena itu, harga langganan disusun secara modular dan bertumbuh mengikuti jumlah outlet — <strong>bukan berdasarkan omzet</strong> — dengan formula sederhana berikut:
                </p>
                <ul class="highlight-list">
                    <li><strong>Biaya Bulanan</strong> = Paket Modul Pusat + Biaya POS Pertama tiap Outlet + Biaya POS Tambahan.</li>
                </ul>

                <p><strong>1. Harga POS per Terminal (per Outlet)</strong></p>
                <p>
                    Harga POS ditentukan oleh jumlah outlet yang berlangganan dalam jaringan Anda — semakin banyak outlet, semakin rendah harga per terminalnya:
                </p>
                <table class="pricing-table">
                    <thead>
                        <tr>
                            <th><%= Common.getBahasaConfig("Jumlah Outlet") %></th>
                            <th><%= Common.getBahasaConfig("POS Pertama / Bulan") %></th>
                            <th><%= Common.getBahasaConfig("POS Tambahan / Bulan / Unit") %></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><strong>1 - 5 outlet</strong></td>
                            <td>Rp 249.000</td>
                            <td>Rp 99.000</td>
                        </tr>
                        <tr>
                            <td><strong>6 - 20 outlet</strong></td>
                            <td>Rp 225.000</td>
                            <td>Rp 85.000</td>
                        </tr>
                        <tr>
                            <td><strong>21 - 50 outlet</strong></td>
                            <td>Rp 199.000</td>
                            <td>Rp 75.000</td>
                        </tr>
                        <tr>
                            <td><strong>Lebih dari 50 outlet</strong></td>
                            <td>mulai Rp 175.000</td>
                            <td>mulai Rp 60.000</td>
                        </tr>
                    </tbody>
                </table>

                <ul class="highlight-list">
                    <li><strong>POS Pertama sudah mencakup:</strong> transaksi penjualan, pembayaran tunai &amp; nontunai, shift kasir, stok outlet, harga &amp; diskon, data pelanggan, retur &amp; pembatalan, laporan penjualan, pengguna &amp; hak akses, mode offline-first, sinkronisasi transaksi, dan backup cloud standar.</li>
                    <li><strong>Tidak termasuk:</strong> perangkat keras (tablet, printer kasir, pemindai barcode, laci kasir) dan biaya jaringan internet.</li>
                </ul>

                <p>
                    <strong>Contoh perhitungan:</strong> 1 outlet dengan 1 POS = <strong>Rp 249.000/bulan</strong>. 1 outlet dengan 3 POS = Rp 249.000 + (2 &times; Rp 99.000) = <strong>Rp 447.000/bulan</strong>. 1 outlet dengan 5 POS = Rp 249.000 + (4 &times; Rp 99.000) = <strong>Rp 645.000/bulan</strong>.
                </p>

                <p><strong>2. Paket Modul Pusat (per Perusahaan/Yayasan)</strong></p>
                <p>
                    Paket Modul Pusat dikenakan satu kali per perusahaan/yayasan — <strong>bukan per outlet</strong> — dan menentukan seberapa dalam integrasi antara gudang pusat, outlet, akuntansi, dan investor.
                </p>
                <table class="pricing-table">
                    <thead>
                        <tr>
                            <th><%= Common.getBahasaConfig("Paket") %></th>
                            <th><%= Common.getBahasaConfig("Harga / Bulan") %></th>
                            <th><%= Common.getBahasaConfig("Cakupan Modul") %></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><strong>Tanpa Paket Pusat</strong></td>
                            <td>Rp 0</td>
                            <td>Hanya POS &amp; stok per outlet, tanpa konsolidasi pusat.</td>
                        </tr>
                        <tr>
                            <td><strong>Central Basic</strong></td>
                            <td>Rp 750.000</td>
                            <td>Dashboard pusat, stok konsolidasi, transfer antaroutlet, pembelian sederhana.</td>
                        </tr>
                        <tr>
                            <td><strong>Central Professional</strong></td>
                            <td>Rp 1.500.000</td>
                            <td>Gudang pusat, pengadaan PR/PO, distribusi, pengiriman, produksi &amp; HPP.</td>
                        </tr>
                        <tr>
                            <td><strong>Full Integrated Suite</strong></td>
                            <td>Rp 2.500.000</td>
                            <td>Seluruh modul pusat + akuntansi, kepegawaian, investor, BEP, audit &amp; analitik.</td>
                        </tr>
                    </tbody>
                </table>

                <ul class="highlight-list">
                    <li><strong>Full Integrated Suite mencakup:</strong> gudang pusat &amp; subgudang, pengadaan PR/PO, penerimaan barang, distribusi outlet, picking/packing/loading, surat jalan &amp; BAST, ekspedisi, produksi, resep &amp; bill of material, HPP otomatis, pencatatan susut/konversi, kepegawaian, penggajian dasar, akuntansi, investor, titik impas (BEP), pembagian laba, audit stok, audit kas, hingga dashboard manajemen.</li>
                    <li><strong>Dihitung terpisah:</strong> kustomisasi khusus, integrasi pihak ketiga/marketplace, pengadaan perangkat keras, dan penempatan tenaga pendamping di lokasi.</li>
                </ul>

                <p><strong>3. Simulasi Jaringan Outlet</strong> (asumsi 2 POS per outlet)</p>
                <table class="pricing-table">
                    <thead>
                        <tr>
                            <th><%= Common.getBahasaConfig("Jumlah Outlet") %></th>
                            <th><%= Common.getBahasaConfig("Total Biaya POS") %></th>
                            <th><%= Common.getBahasaConfig("Paket Pusat") %></th>
                            <th><%= Common.getBahasaConfig("Total / Bulan") %></th>
                            <th><%= Common.getBahasaConfig("Rata-rata / Outlet") %></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><strong>5 outlet</strong></td>
                            <td>Rp 1.740.000</td>
                            <td>Full Integrated Suite Rp 2.500.000</td>
                            <td><strong>Rp 4.240.000</strong></td>
                            <td>&asymp; Rp 848.000</td>
                        </tr>
                        <tr>
                            <td><strong>10 outlet</strong></td>
                            <td>Rp 3.100.000</td>
                            <td>Full Integrated Suite Rp 2.500.000</td>
                            <td><strong>Rp 5.600.000</strong></td>
                            <td>&asymp; Rp 560.000</td>
                        </tr>
                        <tr>
                            <td><strong>30 outlet</strong></td>
                            <td>Rp 8.220.000</td>
                            <td>Full Integrated Suite Rp 2.500.000</td>
                            <td><strong>Rp 10.720.000</strong></td>
                            <td>&asymp; Rp 357.000</td>
                        </tr>
                    </tbody>
                </table>

                <p><strong>4. Biaya Implementasi Awal</strong> (sekali bayar, terpisah dari langganan bulanan)</p>
                <ul class="highlight-list">
                    <li><strong>1 outlet standar:</strong> Rp 1,5 - 3 juta.</li>
                    <li><strong>2 - 5 outlet:</strong> Rp 5 - 10 juta.</li>
                    <li><strong>6 - 20 outlet:</strong> Rp 15 - 25 juta.</li>
                    <li><strong>21 - 50 outlet:</strong> Rp 30 - 50 juta.</li>
                    <li><strong>Implementasi seluruh modul pusat:</strong> tambahan Rp 10 - 30 juta.</li>
                    <li><strong>Promo:</strong> biaya implementasi dapat digratiskan untuk kontrak 2 tahun dengan pembayaran tahunan di muka.</li>
                </ul>

                <p><strong>5. Opsi Harga Tahunan</strong> (hemat &plusmn; 2 bulan bila bayar di muka)</p>
                <p>
                    Contoh: paket POS <strong>Rp 249.000/bulan</strong> menjadi <strong>Rp 2.490.000/tahun</strong> (normal Rp 2.988.000); paket POS <strong>Rp 349.000/bulan</strong> menjadi <strong>Rp 3.490.000/tahun</strong> (normal Rp 4.188.000).
                </p>

                <p><strong>6. Opsi Pelengkap: Bagi Hasil per Transaksi</strong></p>
                <p>
                    Sebagai pelengkap — bukan skema utama — tersedia pula skema Bagi Hasil per Transaksi sebesar <strong>Rp 50 - Rp 100 per transaksi</strong>, dengan tagihan bulanan minimum setara harga langganan POS reguler. Skema ini cocok bagi unit usaha yang ingin memulai tanpa biaya implementasi di muka.
                </p>

                <p>
                    <strong>Ringkasnya:</strong> ePOS mulai <strong>Rp 249.000 per outlet per bulan</strong> (termasuk satu POS), tambahan POS <strong>Rp 99.000 per terminal per bulan</strong>, diskon volume mulai enam outlet; Paket Modul Pusat mulai dari <strong>Central Basic Rp 750.000</strong>, <strong>Central Professional Rp 1,5 juta</strong>, hingga <strong>Full Integrated Suite Rp 2,5 juta</strong> per perusahaan per bulan.
                </p>

                <div class="negotiation-note">
                    <strong>PENTING - Keterbukaan Negosiasi:</strong> Seluruh angka pada tabel harga POS, Paket Modul Pusat, dan biaya implementasi di atas merupakan harga penawaran standar yang <strong>tetap terbuka untuk dinegosiasikan dan disesuaikan</strong> sesuai skala jaringan outlet, kebutuhan modul, serta kesepakatan akhir dengan unit usaha Anda. Kami sangat fleksibel dalam menyusun skema harga maupun tahapan pembayaran. Tujuan utama kami adalah agar sistem ini dapat segera diimplementasikan sehingga secepatnya membawa manfaat operasional nyata bagi unit usaha Anda.
                </div>

                <p>
                    Sebagai wujud komitmen layanan prima, seluruh skema di atas sudah termasuk <strong>Pelatihan (Training) Daring secara GRATIS</strong> untuk kasir, petugas gudang, dan staf akuntansi. <i>(Untuk pendampingan tatap muka/offline di lokasi, biaya jasa trainer tetap gratis, unit usaha hanya perlu memfasilitasi akomodasi dan transportasi tim ahli kami).</i>
                </p>

                <p><strong>Rencana Implementasi yang Terarah</strong></p>
                <p>
                    Agar proses adopsi sistem berjalan efektif, kami menyarankan pendekatan implementasi bertahap yang tetap fleksibel mengikuti prioritas, kesiapan data, dan kapasitas sumber daya unit usaha.
                </p>
                <ol class="implementation-timeline">
                    <li><strong>Analisis Kebutuhan &amp; Pemetaan Alur Operasional:</strong> menyelaraskan modul, alur gudang-kasir-akuntansi, serta prioritas kebutuhan.</li>
                    <li><strong>Persiapan Data &amp; Konfigurasi Awal:</strong> menyiapkan struktur pengguna, hak akses, master item/produk, resep &amp; bahan baku, serta struktur gudang &amp; outlet.</li>
                    <li><strong>Pelatihan Kasir &amp; Petugas Gudang serta Uji Coba Modul:</strong> memastikan kasir, petugas gudang, dan staf akuntansi memahami alur sebelum sistem digunakan lebih luas.</li>
                    <li><strong>Go-Live Bertahap:</strong> modul dapat diaktifkan bertahap (mis. gudang &amp; kasir dahulu, lalu akuntansi dan pengadaan) untuk meminimalkan gangguan operasional harian.</li>
                    <li><strong>Pendampingan &amp; Evaluasi:</strong> penyesuaian, monitoring, dan peningkatan sesuai kebutuhan unit usaha.</li>
                </ol>

                <div class="deliverable-grid">
                    <div class="deliverable-card">
                        <h5><i class="fas fa-chalkboard-user me-2 text-primary"></i>Training Pengguna</h5>
                        <p>Pelatihan daring untuk kasir, petugas gudang, dan staf akuntansi agar proses operasional dapat berjalan lebih mandiri.</p>
                    </div>
                    <div class="deliverable-card">
                        <h5><i class="fas fa-headset me-2 text-success"></i>Dukungan Layanan</h5>
                        <p>Pendampingan teknis untuk membantu adaptasi, koreksi data awal, serta penguatan tata kelola penggunaan sistem.</p>
                    </div>
                </div>

                <span class="section-hl"><i class="fas fa-route me-2"></i>Peta Jalan Ekspedisi, Investor &amp; Kepegawaian Outlet:</span>
                <p>Di atas fondasi Gudang, POS, HPP/Akuntansi, dan Pengadaan yang telah berjalan, kami menyiapkan pengembangan lanjutan secara bertahap dan aditif — tidak mengganggu operasional yang sedang berjalan: <strong>Ekspedisi/Distribusi</strong> untuk rute pengiriman multi-titik dari gudang ke outlet dalam satu perjalanan, manifest pengiriman, bukti serah terima, dan pelacakan kendaraan/BBM; <strong>Investor/Bagi Hasil</strong> dengan skema bagi hasil 3-fase yang dapat dikonfigurasi per perjanjian/outlet/merek/tanggal berlaku (persentase tidak pernah dikodekan secara tetap), lengkap dengan portal investor real-time — distribusi hanya dihitung dari laba bersih yang sudah memperhitungkan HPP, biaya operasional, penyusutan, dan pajak, sehingga jauh lebih dapat dipercaya dibanding sistem lain yang menghitung bagi hasil kasar dari omzet; dan <strong>Kepegawaian Outlet</strong> yang memanfaatkan mesin penggajian &amp; presensi yang sudah matang (dipakai untuk tenaga pengajar) untuk kasir dan staf gudang, sehingga perluasannya lebih merupakan pekerjaan konfigurasi dibanding pembangunan dari nol. Modul-modul ini saat ini berada pada tahap desain arsitektur dan akan digulirkan menyusul fondasi yang sudah solid di atas.</p>

                <div class="closing-callout">
                    <h4><i class="fas fa-handshake-angle me-2"></i>Usulan Tindak Lanjut</h4>
                    <p>
                        Kami merekomendasikan sesi presentasi dan pemaparan modul untuk menunjukkan alur sistem secara nyata, sekaligus memetakan modul yang paling tepat untuk kebutuhan <strong><%=namaClient%></strong>.
                    </p>
                </div>

                <div class="next-step-grid">
                    <div class="next-step-card">
                        <h5><i class="fas fa-calendar-check me-2 text-primary"></i>1. Jadwalkan Presentasi</h5>
                        <p>Tim kami akan memaparkan alur gudang, kasir POS, HPP/akuntansi, pengadaan, dan dasbor pelaporan.</p>
                    </div>
                    <div class="next-step-card">
                        <h5><i class="fas fa-clipboard-list me-2 text-success"></i>2. Susun Prioritas Modul</h5>
                        <p>Bersama unit usaha, kami menentukan modul yang paling mendesak untuk diterapkan lebih dahulu.</p>
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
                <strong>Tim Pengembang <%=judul%> - Modul Gudang & Operasional Bisnis</strong>
                <div class="signature-space">
                    </div>
                <div class="signature-name"><%=namaPenyedia%></div>
                <div>Konsultan & Representatif Layanan</div>
            </div>

            <div class="document-footer-note">
                Dokumen ini disusun sebagai bahan penawaran awal modul Gudang, POS, Ekspedisi, Investor & Akuntansi dan dapat disesuaikan kembali berdasarkan kebutuhan, ruang lingkup modul, skema kerja sama, serta hasil pembahasan bersama calon mitra.
            </div>

        </div>
    </div>

    <script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
    <jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp" />
</body>
</html>
