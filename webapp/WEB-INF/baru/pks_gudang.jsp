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
String paramNomorPks = request.getParameter("nomor_pks");
String paramKotaPks = request.getParameter("kota_pks");
String paramLokasiPks = request.getParameter("lokasi_pks");
String paramNamaWakilClient = request.getParameter("nama_wakil_client");
String paramJabatanClient = request.getParameter("jabatan_client");
String paramAlamatClient = request.getParameter("alamat_client");
String paramMasaKontrak = request.getParameter("masa_kontrak");

// Deklarasi Variabel Data Institusi
ais.database.model.sekolah.Sekolah sekolah = ais.action.master.sekolah.util.SekolahUtil.getSekolah(request);
String Telepon = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getTelepon();
String Alamat1 = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getAlamat1();

// Override Variabel berdasarkan parameter White-Label
String judul = (paramJudul != null && !paramJudul.trim().isEmpty()) ? paramJudul : ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama();
String logo_PerguruanTinggi = (paramLogo != null && !paramLogo.trim().isEmpty()) ? Common.ROOT + "/img/" + paramLogo : ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");

// Variabel Identitas Hukum (Fallback jika parameter kosong)
String namaClient = (paramClient != null && !paramClient.trim().isEmpty()) ? paramClient : "[NAMA INSTITUSI/BADAN USAHA KLIEN]";
String namaPenyedia = (paramPresentBy != null && !paramPresentBy.trim().isEmpty()) ? paramPresentBy : "[NAMA PENYEDIA SISTEM / KONSULTAN]";
String kontakPenyedia = (paramTelp != null && !paramTelp.trim().isEmpty()) ? paramTelp : Telepon;

// Tanggal Hari Ini untuk format PKS
Calendar cal = Calendar.getInstance();
String hari = new SimpleDateFormat("EEEE", new Locale("id", "ID")).format(cal.getTime());
String tanggalStr = new SimpleDateFormat("dd", new Locale("id", "ID")).format(cal.getTime());
String bulan = new SimpleDateFormat("MMMM", new Locale("id", "ID")).format(cal.getTime());
String tahunStr = new SimpleDateFormat("yyyy", new Locale("id", "ID")).format(cal.getTime());

// Parameter opsional untuk kebutuhan finalisasi dokumen PKS
String nomorPks = (paramNomorPks != null && !paramNomorPks.trim().isEmpty()) ? paramNomorPks : "...... / PKS-EGUDANG / EE / " + tahunStr;
String kotaPks = (paramKotaPks != null && !paramKotaPks.trim().isEmpty()) ? paramKotaPks : ".......................................";
String lokasiPks = (paramLokasiPks != null && !paramLokasiPks.trim().isEmpty()) ? paramLokasiPks : ".......................................";
String namaWakilClient = (paramNamaWakilClient != null && !paramNamaWakilClient.trim().isEmpty()) ? paramNamaWakilClient : ".........................................................";
String jabatanClient = (paramJabatanClient != null && !paramJabatanClient.trim().isEmpty()) ? paramJabatanClient : "[JABATAN PIMPINAN / DIREKTUR]";
String alamatClient = (paramAlamatClient != null && !paramAlamatClient.trim().isEmpty()) ? paramAlamatClient : "[ALAMAT INSTITUSI/BADAN USAHA KLIEN]";
String masaKontrak = (paramMasaKontrak != null && !paramMasaKontrak.trim().isEmpty()) ? paramMasaKontrak : "...... (............) tahun";
%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= Common.getBahasaConfig("Draf PKS - Modul Gudang & POS (eGudang)") %> | <%=judul%></title>

    <link rel="icon" href="<%=logo_PerguruanTinggi%>" type="image/x-icon">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">

    <style>
        :root {
            --primary-color: #0f172a;
            --secondary-color: #1d4ed8;
            --accent-color: #f59e0b;
            --success-color: #0f766e;
            --soft-blue: #eff6ff;
            --soft-gold: #fffbeb;
            --soft-green: #ecfdf5;
            --text-dark: #0f172a;
            --text-regular: #334155;
            --text-muted: #64748b;
            --border-color: #cbd5e1;
            --paper-border: #e2e8f0;
        }

        * { box-sizing: border-box; }

        body {
            background:
                radial-gradient(circle at top left, rgba(29, 78, 216, 0.16), transparent 32%),
                radial-gradient(circle at bottom right, rgba(245, 158, 11, 0.14), transparent 34%),
                #e2e8f0;
            font-family: "Times New Roman", Times, serif;
            color: var(--text-dark);
            line-height: 1.62;
            margin: 0;
        }

        .document-paper {
            position: relative;
            overflow: hidden;
            background-color: #ffffff;
            max-width: 980px;
            margin: 3rem auto;
            padding: 4.8rem 5.6rem;
            border-radius: 18px;
            border: 1px solid rgba(148, 163, 184, 0.45);
            box-shadow: 0 24px 70px rgba(15, 23, 42, 0.18);
        }

        .document-paper::before {
            content: "EGUDANG - ENTERPRISE EDUCATION";
            position: absolute;
            top: 44%;
            left: 50%;
            transform: translate(-50%, -50%) rotate(-28deg);
            font-family: Arial, sans-serif;
            font-weight: 900;
            font-size: 3.4rem;
            letter-spacing: 0.25rem;
            color: rgba(15, 23, 42, 0.035);
            white-space: nowrap;
            pointer-events: none;
        }

        .document-paper > * { position: relative; z-index: 1; }

        .doc-header {
            position: relative;
            text-align: center;
            margin: -1.8rem -2.2rem 2.4rem;
            padding: 2.4rem 2rem 2rem;
            border: 1px solid var(--paper-border);
            border-radius: 22px;
            background:
                linear-gradient(135deg, rgba(15,23,42,0.96), rgba(30,64,175,0.92)),
                radial-gradient(circle at top right, rgba(245,158,11,0.35), transparent 34%);
            color: #ffffff;
            box-shadow: 0 18px 44px rgba(15, 23, 42, 0.18);
        }

        .doc-logo {
            max-height: 96px;
            max-width: 150px;
            object-fit: contain;
            padding: 10px;
            background: #ffffff;
            border-radius: 18px;
            margin-bottom: 1.2rem;
            box-shadow: 0 10px 28px rgba(0,0,0,0.18);
        }

        .doc-eyebrow {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 7px 14px;
            border-radius: 999px;
            background: rgba(255, 255, 255, 0.12);
            border: 1px solid rgba(255, 255, 255, 0.22);
            font-family: Arial, sans-serif;
            font-size: 0.78rem;
            letter-spacing: 0.16rem;
            text-transform: uppercase;
            margin-bottom: 0.9rem;
        }

        .doc-title {
            font-size: 1.85rem;
            font-weight: 900;
            text-transform: uppercase;
            letter-spacing: 1.3px;
            margin-bottom: 0.55rem;
            line-height: 1.22;
        }

        .doc-subtitle {
            font-size: 1.12rem;
            font-weight: 700;
            text-transform: uppercase;
            line-height: 1.45;
            color: #dbeafe;
        }

        .doc-number {
            display: inline-block;
            margin-top: 1rem;
            padding: 9px 18px;
            border-radius: 999px;
            background: rgba(255, 255, 255, 0.95);
            color: var(--primary-color);
            font-size: 1rem;
            font-weight: 800;
            border: 1px solid rgba(255,255,255,0.55);
        }

        .doc-meta-grid {
            display: grid;
            grid-template-columns: repeat(3, minmax(0, 1fr));
            gap: 10px;
            margin-top: 1.25rem;
            font-family: Arial, sans-serif;
        }

        .doc-meta-item {
            padding: 10px 12px;
            border-radius: 14px;
            background: rgba(255,255,255,0.11);
            border: 1px solid rgba(255,255,255,0.16);
            text-align: left;
        }

        .doc-meta-item small {
            display: block;
            color: #bfdbfe;
            font-size: 0.68rem;
            text-transform: uppercase;
            letter-spacing: 0.08rem;
            margin-bottom: 2px;
        }

        .doc-meta-item strong { font-size: 0.88rem; color: #ffffff; }

        .article-title {
            position: relative;
            text-align: center;
            font-weight: 900;
            font-size: 1.24rem;
            margin-top: 3.2rem;
            margin-bottom: 1.5rem;
            text-transform: uppercase;
            letter-spacing: 0.06rem;
            color: var(--primary-color);
            page-break-after: avoid;
        }

        .article-title::after {
            content: "";
            display: block;
            width: 92px;
            height: 4px;
            margin: 0.75rem auto 0;
            border-radius: 999px;
            background: linear-gradient(90deg, var(--secondary-color), var(--accent-color));
        }

        .clause-content {
            text-align: justify;
            margin-bottom: 1rem;
            font-size: 1.08rem;
        }

        .lead-clause {
            font-size: 1.12rem;
            padding: 1rem 1.1rem;
            border-left: 5px solid var(--secondary-color);
            background: linear-gradient(90deg, var(--soft-blue), #ffffff);
            border-radius: 0 14px 14px 0;
        }

        .summary-panel {
            margin: 1.8rem 0 2rem;
            padding: 1.25rem;
            border-radius: 20px;
            border: 1px solid var(--paper-border);
            background: linear-gradient(135deg, #ffffff, #f8fafc);
            box-shadow: 0 14px 34px rgba(15, 23, 42, 0.06);
        }

        .summary-title {
            font-family: Arial, sans-serif;
            font-size: 0.78rem;
            letter-spacing: 0.14rem;
            text-transform: uppercase;
            color: var(--secondary-color);
            font-weight: 800;
            margin-bottom: 0.35rem;
        }

        .summary-headline {
            font-size: 1.32rem;
            font-weight: 900;
            line-height: 1.35;
            margin-bottom: 0.75rem;
        }

        .summary-card-grid,
        .value-grid,
        .deliverable-grid {
            display: grid;
            grid-template-columns: repeat(3, minmax(0, 1fr));
            gap: 12px;
            margin: 1rem 0;
        }

        .summary-card,
        .value-card,
        .deliverable-card {
            padding: 1rem;
            border-radius: 16px;
            background: #ffffff;
            border: 1px solid var(--paper-border);
            box-shadow: 0 10px 24px rgba(15, 23, 42, 0.045);
            page-break-inside: avoid;
        }

        .summary-card i,
        .value-card i,
        .deliverable-card i {
            width: 34px;
            height: 34px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            border-radius: 12px;
            background: var(--soft-blue);
            color: var(--secondary-color);
            margin-bottom: 0.6rem;
            font-size: 0.95rem;
        }

        .summary-card strong,
        .value-card strong,
        .deliverable-card strong {
            display: block;
            font-size: 0.98rem;
            margin-bottom: 0.35rem;
            color: var(--primary-color);
        }

        .summary-card span,
        .value-card span,
        .deliverable-card span {
            display: block;
            font-size: 0.9rem;
            color: var(--text-regular);
            line-height: 1.48;
        }

        .legal-note {
            margin: 1.4rem 0;
            padding: 1rem 1.15rem;
            border-radius: 16px;
            border: 1px solid #fde68a;
            background: linear-gradient(135deg, var(--soft-gold), #ffffff);
            color: #78350f;
            font-size: 0.98rem;
            text-align: justify;
        }

        .highlight-box {
            margin: 1rem 0 1.25rem;
            padding: 1.1rem 1.25rem;
            border-radius: 16px;
            border: 1px solid #bfdbfe;
            background: linear-gradient(135deg, #eff6ff, #ffffff);
            text-align: justify;
        }

        .timeline-list {
            position: relative;
            margin: 1rem 0;
            padding-left: 0;
            list-style: none;
        }

        .timeline-list li {
            position: relative;
            padding: 0.8rem 0.9rem 0.8rem 3.2rem;
            margin-bottom: 0.7rem;
            border-radius: 14px;
            background: #ffffff;
            border: 1px solid var(--paper-border);
            box-shadow: 0 8px 18px rgba(15, 23, 42, 0.04);
        }

        .timeline-list li::before {
            content: attr(data-step);
            position: absolute;
            left: 0.85rem;
            top: 0.78rem;
            width: 28px;
            height: 28px;
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 50%;
            background: var(--secondary-color);
            color: #ffffff;
            font-family: Arial, sans-serif;
            font-weight: 800;
            font-size: 0.82rem;
        }

        .enhanced-table {
            width: 100%;
            border-collapse: collapse;
            margin: 1rem 0 1.5rem;
            font-size: 0.96rem;
            page-break-inside: avoid;
        }

        .enhanced-table th {
            background: var(--primary-color);
            color: #ffffff;
            padding: 10px 12px;
            border: 1px solid var(--primary-color);
            text-align: left;
            font-family: Arial, sans-serif;
            font-size: 0.82rem;
            text-transform: uppercase;
            letter-spacing: 0.04rem;
        }

        .enhanced-table td {
            padding: 10px 12px;
            border: 1px solid var(--border-color);
            vertical-align: top;
            background: #ffffff;
        }

        .enhanced-table tr:nth-child(even) td { background: #f8fafc; }

        .pricing-box {
            border-radius: 18px;
            border: 1px solid #cbd5e1 !important;
            background: linear-gradient(135deg, #f8fafc, #ffffff);
            box-shadow: inset 0 0 0 1px rgba(255,255,255,0.85);
        }

        .list-alpha { list-style-type: lower-alpha; padding-left: 2rem; margin-bottom: 1rem; }
        .list-number { list-style-type: decimal; padding-left: 2rem; margin-bottom: 1rem; }
        .list-dash { list-style-type: none; padding-left: 1.5rem; margin-bottom: 1rem; }
        .list-dash li { position: relative; }
        .list-dash li::before { content: "—"; position: absolute; margin-left: -1.5rem; color: var(--secondary-color); font-weight: bold; }

        li { margin-bottom: 0.5rem; text-align: justify; font-size: 1.07rem; }

        .signature-section {
            margin-top: 5rem;
            display: flex;
            justify-content: space-between;
            gap: 2rem;
            page-break-inside: avoid;
        }

        .signature-box {
            text-align: center;
            width: 48%;
            padding: 1.2rem 1rem;
            border: 1px solid var(--paper-border);
            border-radius: 16px;
            background: #ffffff;
        }

        .signature-space { height: 120px; }

        .closing-note {
            margin-top: 2.5rem;
            padding: 1rem 1.25rem;
            border-radius: 16px;
            background: #f8fafc;
            border: 1px solid var(--paper-border);
            font-size: 0.96rem;
            text-align: justify;
            color: var(--text-regular);
        }

        .doc-footer {
            margin-top: 2.4rem;
            padding-top: 1rem;
            border-top: 1px solid var(--paper-border);
            display: flex;
            justify-content: space-between;
            gap: 1rem;
            color: var(--text-muted);
            font-size: 0.82rem;
            font-family: Arial, sans-serif;
        }

        .print-toolbar {
            position: fixed;
            bottom: 2rem;
            right: 2rem;
            z-index: 1000;
            display: flex;
            gap: 10px;
            align-items: center;
            padding: 10px;
            border-radius: 999px;
            background: rgba(255,255,255,0.9);
            border: 1px solid rgba(148,163,184,0.45);
            box-shadow: 0 18px 40px rgba(15,23,42,0.18);
            backdrop-filter: blur(12px);
            font-family: Arial, sans-serif;
        }

        .btn-print {
            border-radius: 999px;
            padding: 0.85rem 1.25rem;
            font-weight: 800;
        }

        .toolbar-hint {
            color: var(--text-muted);
            font-size: 0.82rem;
            padding-left: 8px;
        }

        @media (max-width: 900px) {
            .document-paper { padding: 2rem 1.3rem; margin: 1rem; border-radius: 14px; }
            .doc-header { margin: 0 0 1.4rem; }
            .doc-meta-grid, .summary-card-grid, .value-grid, .deliverable-grid { grid-template-columns: 1fr; }
            .signature-section { flex-direction: column; }
            .signature-box { width: 100%; }
            .print-toolbar { left: 1rem; right: 1rem; bottom: 1rem; justify-content: center; }
            .toolbar-hint { display: none; }
        }

        @page { size: A4; margin: 18mm 15mm; }

        @media print {
            body { background: #fff !important; }
            .document-paper {
                margin: 0;
                padding: 0;
                box-shadow: none;
                max-width: 100%;
                border-radius: 0;
                border: 0;
            }
            .document-paper::before { color: rgba(15, 23, 42, 0.025); }
            .print-toolbar, .no-print { display: none !important; }
            .page-break { page-break-before: always; }
            .doc-header {
                margin: 0 0 2rem;
                color: #ffffff !important;
                -webkit-print-color-adjust: exact;
                print-color-adjust: exact;
            }
            .summary-panel, .summary-card, .value-card, .deliverable-card, .signature-box, .highlight-box, .legal-note, .pricing-box, .timeline-list li {
                -webkit-print-color-adjust: exact;
                print-color-adjust: exact;
            }
            .article-title { page-break-after: avoid; }
            .summary-card, .value-card, .deliverable-card, .signature-section, .enhanced-table, .timeline-list li { page-break-inside: avoid; }
        }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/baru/include/pemilih_bahasa.jsp" />

    <div class="print-toolbar no-print">
        <span class="toolbar-hint"><i class="fas fa-file-contract me-1"></i> Draf PKS Modul Gudang & Ritel siap cetak</span>
        <button onclick="window.print()" class="btn btn-dark btn-print">
            <i class="fas fa-print me-2"></i> Cetak / Simpan PDF
        </button>
    </div>

    <div class="container">
        <div class="document-paper">

            <div class="doc-header">
                <img src="<%=logo_PerguruanTinggi%>" alt="Logo" class="doc-logo">
                <div class="doc-eyebrow"><i class="fas fa-shield-halved"></i> Dokumen Kerja Sama Strategis</div>
                <div class="doc-title">PERJANJIAN KERJA SAMA</div>
                <div class="doc-subtitle">PENGADAAN DAN IMPLEMENTASI SISTEM INFORMASI<br>GUDANG, POINT OF SALE (POS) & RITEL TERPADU (eGudang)</div>
                <div class="doc-number">Nomor: <%=nomorPks%></div>
                <div class="doc-meta-grid">
                    <div class="doc-meta-item">
                        <small>PIHAK PERTAMA</small>
                        <strong><%=namaClient%></strong>
                    </div>
                    <div class="doc-meta-item">
                        <small>PIHAK KEDUA</small>
                        <strong><%=judul%></strong>
                    </div>
                    <div class="doc-meta-item">
                        <small>TANGGAL DOKUMEN</small>
                        <strong><%=tanggalStr%> <%=bulan%> <%=tahunStr%></strong>
                    </div>
                </div>
            </div>

            <div class="summary-panel">
                <div class="summary-title">Ringkasan Eksekutif PKS</div>
                <div class="summary-headline">Landasan kerja sama untuk membangun sistem informasi gudang, kasir (POS), dan ritel yang terintegrasi, terukur, aman, dan berkelanjutan.</div>
                <div class="clause-content mb-2">
                    Dokumen ini disusun sebagai draf Perjanjian Kerja Sama yang memuat ruang lingkup implementasi modul <strong>eGudang (Sistem Gudang, Point of Sale &amp; Ritel Terpadu)</strong>, hak dan kewajiban PARA PIHAK, skema pembiayaan, dukungan layanan, perlindungan data, serta mekanisme pelaksanaan kerja sama. Modul ini merupakan bagian dari ekosistem <strong>Enterprise Education</strong> yang sama, sehingga dapat terintegrasi langsung dengan data eCampus/eSchool/ePesantren apabila PIHAK PERTAMA merupakan Universitas, Sekolah, atau Pondok Pesantren yang memiliki unit koperasi, kantin, gudang, atau usaha yang dibiayai investor, maupun diimplementasikan secara berdiri sendiri (<i>standalone</i>) bagi badan usaha non-pendidikan.
                </div>
                <div class="summary-card-grid">
                    <div class="summary-card">
                        <i class="fas fa-diagram-project"></i>
                        <strong>Terintegrasi End-to-End</strong>
                        <span>Menghubungkan kasir (POS), gudang, akuntansi, pengadaan, dan dashboard analitik dalam satu ekosistem.</span>
                    </div>
                    <div class="summary-card">
                        <i class="fas fa-chart-line"></i>
                        <strong>Bernilai Strategis</strong>
                        <span>Mendorong efisiensi operasional ritel, transparansi stok, kecepatan transaksi kasir, dan dashboard pengambilan keputusan pimpinan.</span>
                    </div>
                    <div class="summary-card">
                        <i class="fas fa-lock"></i>
                        <strong>Aman &amp; Berkelanjutan</strong>
                        <span>Menegaskan kepemilikan data stok dan transaksi, jejak audit menyeluruh, jaminan layanan, pelatihan, serta pendampingan implementasi.</span>
                    </div>
                </div>
            </div>

            <div class="clause-content lead-clause">
                Pada hari ini, <strong><%=hari%></strong> tanggal <strong><%=tanggalStr%></strong> bulan <strong><%=bulan%></strong> tahun <strong><%=tahunStr%></strong>, bertempat di <%=lokasiPks%>, kami yang bertanda tangan di bawah ini:
            </div>

            <ol class="list-number mt-3 mb-4">
                <li>
                    <div class="clause-content">
                        <strong><%=namaWakilClient%></strong>, selaku <%=jabatanClient%>, yang bertindak untuk dan atas nama <strong><%=namaClient%></strong>, berkedudukan di <%=alamatClient%>. Untuk selanjutnya di dalam Perjanjian ini disebut sebagai <strong>PIHAK PERTAMA</strong>.
                    </div>
                </li>
                <li>
                    <div class="clause-content">
                        <strong><%=namaPenyedia%></strong>, selaku Representatif/Konsultan dari <strong><%=judul%></strong>, berkedudukan di <%=Alamat1%> (Kontak: <%=kontakPenyedia%>). Untuk selanjutnya di dalam Perjanjian ini disebut sebagai <strong>PIHAK KEDUA</strong>.
                    </div>
                </li>
            </ol>

            <div class="clause-content">
                PIHAK PERTAMA dan PIHAK KEDUA secara bersama-sama selanjutnya disebut sebagai <strong>"PARA PIHAK"</strong>. PARA PIHAK terlebih dahulu menerangkan hal-hal sebagai berikut:
            </div>

            <ul class="list-alpha mt-2 mb-4">
                <li>Bahwa PIHAK PERTAMA adalah institusi pendidikan (Universitas/Sekolah/Pondok Pesantren) yang memiliki unit usaha koperasi, kantin, dan/atau gudang, atau badan usaha ritel/dagang independen, yang membutuhkan sistem informasi manajemen gudang, kasir (POS), dan rantai pasok yang komprehensif, modern, dan terintegrasi.</li>
                <li>Bahwa PIHAK KEDUA adalah badan/pihak penyedia layanan teknologi informasi yang memiliki kompetensi, hak cipta, dan kewenangan atas perangkat lunak Sistem Gudang, Point of Sale &amp; Ritel Terpadu (eGudang), yang merupakan modul dari ekosistem Enterprise Education.</li>
                <li>Bahwa PARA PIHAK memandang digitalisasi tata kelola gudang dan ritel sebagai kebutuhan strategis untuk meningkatkan akurasi stok, transparansi transaksi kasir, kecepatan pelayanan pelanggan, serta efektivitas pengambilan keputusan berbasis informasi.</li>
                <li>Bahwa kerja sama ini tidak hanya berorientasi pada pengadaan aplikasi, tetapi juga mencakup pendampingan perubahan proses kerja operasional, penguatan kapasitas staf kasir dan gudang, serta penyelarasan sistem dengan kebutuhan operasional PIHAK PERTAMA.</li>
                <li>Bahwa PARA PIHAK berkomitmen menjalankan kerja sama secara profesional, akuntabel, saling menguntungkan, serta berlandaskan prinsip keakuratan data stok/transaksi dan keberlanjutan layanan.</li>
            </ul>

            <div class="clause-content">
                Berdasarkan hal-hal tersebut di atas, PARA PIHAK sepakat untuk mengikatkan diri dalam Perjanjian Kerja Sama Implementasi Modul eGudang dengan ketentuan dan syarat-syarat yang diatur dalam pasal-pasal berikut:
            </div>

            <div class="legal-note">
                <strong>Catatan Penyelarasan:</strong> Draf PKS ini dapat disesuaikan kembali berdasarkan hasil pembahasan teknis, legal, keuangan, dan kebutuhan operasional PIHAK PERTAMA sebelum ditandatangani. Penyesuaian tersebut dapat dituangkan dalam bentuk finalisasi naskah, berita acara, lampiran teknis, atau adendum sesuai kesepakatan PARA PIHAK.
            </div>

            <div class="article-title">PASAL 1<br>DEFINISI</div>
            <div class="clause-content">
                Kecuali ditentukan lain dalam konteks kalimatnya, istilah-istilah di bawah ini memiliki pengertian sebagai berikut:
                <ol class="list-number mt-2">
                    <li><strong>Sistem / Perangkat Lunak</strong> adalah modul <i>eGudang</i> berbasis web, yaitu Sistem Gudang, Point of Sale (POS), dan Ritel Terpadu yang dikembangkan oleh PIHAK KEDUA sebagai bagian dari ekosistem Enterprise Education (eCampus/eSchool/ePesantren).</li>
                    <li><strong>Implementasi</strong> adalah serangkaian kegiatan yang mencakup instalasi, konfigurasi, migrasi data, serta pelatihan penggunaan (<i>training</i>) Sistem kepada sumber daya manusia milik PIHAK PERTAMA (kasir, petugas gudang, staf pengadaan, akuntan, admin).</li>
                    <li><strong>Transaksi Penjualan (POS)</strong> adalah transaksi penjualan barang/jasa yang dicatat oleh kasir dan dihitung otomatis oleh Sistem sebagai dasar tagihan pelanggan.</li>
                    <li><strong>Stok/Persediaan</strong> adalah catatan kuantitas barang pada gudang pusat maupun cabang, beserta jejak audit atas setiap perubahannya.</li>
                    <li><strong>Dashboard Analitik</strong> adalah halaman ringkasan informasi operasional (ringkasan penjualan, stok, HPP, dan kinerja outlet/gudang) sebagai bahan monitoring dan pengambilan keputusan pimpinan.</li>
                    <li><strong>Lampiran Teknis</strong> adalah dokumen pendukung yang memuat rincian modul, konfigurasi, tahapan implementasi, kebutuhan data, jadwal pelaksanaan, atau kesepakatan teknis lain yang menjadi satu kesatuan dengan Perjanjian ini.</li>
                </ol>
            </div>

            <div class="article-title">PASAL 2<br>RUANG LINGKUP PEKERJAAN & FITUR SISTEM</div>
            <div class="clause-content">
                PIHAK KEDUA sepakat untuk menyediakan, menginstalasi, dan memelihara Sistem eGudang bagi PIHAK PERTAMA. Ruang lingkup fungsionalitas Sistem yang disediakan secara komprehensif meliputi:
            </div>

            <div class="highlight-box">
                <strong>Orientasi Solusi:</strong> eGudang dirancang sebagai platform tata kelola gudang, kasir, dan rantai pasok terpadu yang membantu institusi maupun badan usaha ritel menata proses operasional, mengurangi pekerjaan manual berulang, mempercepat layanan kepada pelanggan, meningkatkan akurasi stok, dan menghadirkan laporan manajemen yang lebih siap digunakan oleh pimpinan.
            </div>

            <div class="value-grid">
                <div class="value-card">
                    <i class="fas fa-database"></i>
                    <strong>Satu Data Rantai Pasok</strong>
                    <span>Data gudang, kasir, akuntansi, dan pengadaan dapat dikelola lebih konsisten dalam satu basis data terpadu.</span>
                </div>
                <div class="value-card">
                    <i class="fas fa-gauge-high"></i>
                    <strong>Proses Lebih Cepat</strong>
                    <span>Alur transaksi kasir, stok, dan pembayaran dapat dipercepat melalui automasi.</span>
                </div>
                <div class="value-card">
                    <i class="fas fa-users-gear"></i>
                    <strong>Kolaborasi Unit Operasional</strong>
                    <span>Kasir, petugas gudang, akuntan, dan staf pengadaan memiliki portal dan hak akses yang sesuai perannya.</span>
                </div>
            </div>

            <div class="clause-content fw-bold mt-3">2.1. Point of Sale (POS) &amp; Kasir Multi-Outlet</div>
            <ul class="list-dash">
                <li><strong>Kasir Multi-Outlet &amp; Offline-First:</strong> transaksi penjualan dapat tetap berjalan meski koneksi internet terputus, dengan sinkronisasi otomatis saat koneksi kembali tersedia.</li>
                <li><strong>Proteksi Stok Sisi-Server:</strong> validasi ketersediaan stok dilakukan di server untuk mencegah penjualan melebihi stok riil meski beberapa kasir bertransaksi bersamaan.</li>
                <li><strong>Shift Kasir &amp; Rekonsiliasi Kas:</strong> buka/tutup shift, pencatatan kas masuk-keluar, dan rekonsiliasi kas otomatis di akhir shift.</li>
                <li><strong>Diskon, Cashback &amp; Metode Pembayaran:</strong> mendukung berbagai skema diskon, cashback, serta beragam metode pembayaran (tunai maupun non-tunai).</li>
            </ul>

            <div class="clause-content fw-bold mt-3">2.2. Gudang, Stok &amp; Rantai Pasok (Pengadaan)</div>
            <ul class="list-dash">
                <li><strong>Hierarki Gudang Pusat-Cabang:</strong> pelacakan stok multi-lokasi dengan struktur gudang pusat dan gudang cabang.</li>
                <li><strong>Jejak Audit Stok Menyeluruh:</strong> setiap perubahan stok tertaut ke dokumen sumber yang menyebabkannya (transaksi kasir, penerimaan barang, transfer antar gudang, atau koreksi stok).</li>
                <li><strong>Stok Opname:</strong> perhitungan fisik stok secara berkala dengan mekanisme penyesuaian yang tervalidasi.</li>
                <li><strong>Pengadaan (Procurement):</strong> alur Permintaan Pembelian &rarr; Persetujuan &rarr; Pesanan Pembelian (PO) &rarr; Penerimaan Barang &rarr; Pembayaran, dengan alur persetujuan (<i>approval workflow</i>) yang dapat dikonfigurasi serta manajemen data pemasok/vendor.</li>
            </ul>

            <div class="clause-content fw-bold mt-3">2.3. HPP &amp; Akuntansi</div>
            <ul class="list-dash">
                <li><strong>Perhitungan HPP Otomatis:</strong> harga pokok penjualan dihitung otomatis berbasis resep/komposisi (rollup harga beli bahan baku ke produk jadi).</li>
                <li><strong>Posting Jurnal Otomatis:</strong> setiap dokumen transaksi (penjualan, pembelian, pemakaian bahan baku) diposting ke jurnal akuntansi secara otomatis dan dapat ditelusuri kembali ke dokumen sumbernya.</li>
                <li><strong>Bagan Akun Terkonfigurasi:</strong> struktur akun (<i>chart of accounts</i>) dapat disesuaikan dengan kebutuhan PIHAK PERTAMA.</li>
            </ul>

            <div class="clause-content fw-bold mt-3">2.4. Dashboard Analitik &amp; Pelaporan</div>
            <ul class="list-dash">
                <li><strong>Dashboard Operasional:</strong> ringkasan penjualan, stok, HPP, dan kinerja outlet/gudang.</li>
                <li><strong>Laporan Siap Pakai:</strong> mencakup laporan penjualan (POS), stok/persediaan, HPP, pengadaan, dan akuntansi.</li>
            </ul>

            <div class="page-break"></div>

            <div class="clause-content fw-bold mt-3">2.5. Keamanan Data &amp; Jejak Audit</div>
            <ul class="list-dash">
                <li><strong>Jejak Audit Menyeluruh:</strong> setiap perubahan data stok, harga, dan transaksi tercatat (siapa, kapan, apa yang diubah).</li>
                <li><strong>Hak Akses Berjenjang:</strong> kasir, petugas gudang, akuntan, staf pengadaan, dan admin memiliki akses sesuai perannya.</li>
            </ul>

            <div class="clause-content fw-bold mt-3">2.6. Roadmap Modul Lanjutan (Ekspedisi, Investor &amp; Kepegawaian Outlet)</div>
            <div class="clause-content">
                Modul-modul berikut termasuk dalam ruang lingkup kerja sama ini, namun diimplementasikan secara bertahap pada fase lanjutan sesuai kesiapan dan prioritas PIHAK PERTAMA, tanpa mengganggu operasional yang sedang berjalan: <strong>Fase 1</strong> Ekspedisi (perutean pengiriman multi-titik, manifest pengiriman, dan bukti serah terima/<i>proof of delivery</i>); <strong>Fase 2</strong> Investor (skema bagi hasil 3-fase yang dapat dikonfigurasi sesuai kesepakatan &mdash; bukan persentase baku &mdash; disertai portal investor secara <i>real-time</i>, dengan distribusi hanya dihitung dari laba bersih yang telah diperhitungkan secara benar, bukan dari omzet/pendapatan kotor); <strong>Fase 3</strong> Kepegawaian Outlet (penggajian dan absensi staf ritel/gudang melalui mesin penggajian yang sudah tersedia pada ekosistem Enterprise Education). Jadwal penyelesaian tiap fase diatur lebih lanjut dalam Lampiran Teknis sesuai kesiapan dan kebutuhan PIHAK PERTAMA.
            </div>

            <div class="clause-content fw-bold mt-4">2.7. Output Implementasi dan Deliverable Utama</div>
            <div class="deliverable-grid">
                <div class="deliverable-card">
                    <i class="fas fa-server"></i>
                    <strong>Lingkungan Sistem</strong>
                    <span>Instalasi, setup server/cloud atau on-premise, konfigurasi awal, hak akses, dan pengaturan parameter outlet/gudang.</span>
                </div>
                <div class="deliverable-card">
                    <i class="fas fa-file-import"></i>
                    <strong>Migrasi Data Awal</strong>
                    <span>Pendampingan penyiapan data master (produk, harga, stok awal, pemasok) dan impor data awal sesuai format yang disepakati.</span>
                </div>
                <div class="deliverable-card">
                    <i class="fas fa-chalkboard-user"></i>
                    <strong>Pelatihan Pengguna</strong>
                    <span>Transfer pengetahuan kepada kasir, petugas gudang, dan staf pengadaan agar Sistem dapat digunakan secara mandiri.</span>
                </div>
            </div>

            <div class="clause-content fw-bold mt-4">2.8. Indikator Keberhasilan Implementasi</div>
            <table class="enhanced-table">
                <thead>
                    <tr>
                        <th><%= Common.getBahasaConfig("Area") %></th>
                        <th><%= Common.getBahasaConfig("Indikator yang Diharapkan") %></th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>POS &amp; Kasir</td>
                        <td>Transaksi penjualan berjalan lebih cepat, tertib, dan minim selisih kas pada akhir shift.</td>
                    </tr>
                    <tr>
                        <td>Gudang &amp; Stok</td>
                        <td>Stok lebih akurat, jejak audit lengkap, dan selisih/kehilangan stok dapat ditekan.</td>
                    </tr>
                    <tr>
                        <td>HPP &amp; Akuntansi</td>
                        <td>Perhitungan HPP dan posting jurnal lebih akurat dan minim koreksi manual.</td>
                    </tr>
                    <tr>
                        <td>Pengadaan</td>
                        <td>Proses Permintaan Pembelian-PO-Penerimaan Barang lebih tertib dan terdokumentasi.</td>
                    </tr>
                    <tr>
                        <td>Adopsi Pengguna</td>
                        <td>Kasir, petugas gudang, akuntan, staf pengadaan, dan admin dapat menggunakan layanan sesuai peran masing-masing.</td>
                    </tr>
                </tbody>
            </table>

            <div class="clause-content fw-bold mt-3">2.9. Aplikasi Klien Pendukung: POS Kasir Desktop, POS Kasir Android &amp; Stok Opname Android</div>
            <p>Berbeda dari sebagian modul lain yang masih dalam roadmap, ketiga aplikasi klien berikut <strong>sudah selesai dibangun dan sudah dirilis resmi</strong> pada saat Perjanjian ini disusun, sehingga dapat langsung diunduh dan digunakan oleh PIHAK PERTAMA:</p>
            <ul class="list-dash">
                <li><strong>POS Kasir Desktop (Windows):</strong> aplikasi kasir untuk PC/laptop yang tetap dapat mencatat transaksi meski koneksi internet terputus, dengan sinkronisasi otomatis begitu koneksi tersedia kembali, dilengkapi Layar Pelanggan kedua (dual monitor), pembayaran saldo member dengan verifikasi PIN, lebih dari 150 laporan siap pakai, dan pembaruan aplikasi otomatis.</li>
                <li><strong>POS Kasir Android (tablet &amp; HP):</strong> aplikasi kasir responsif untuk perangkat tablet maupun HP Android, mencetak struk ke printer thermal Bluetooth portable, dan tetap dapat mencatat transaksi tanpa sinyal internet (antrean transaksi lokal, sinkron otomatis).</li>
                <li><strong>Stok Opname Android:</strong> aplikasi khusus perhitungan stok fisik yang memindai barcode melalui alat pemindai/PDT genggam atau kamera perangkat, dengan hasil yang tersinkron langsung ke server.</li>
                <li><strong>Distribusi &amp; Pembaruan:</strong> ketiga aplikasi didistribusikan secara resmi dan gratis melalui GitHub Releases milik PIHAK KEDUA (<a href="https://github.com/Zishof/ais-pos-kasir-desktop/releases" target="_blank" rel="noopener">ais-pos-kasir-desktop</a>, <a href="https://github.com/Zishof/ais-pos-kasir-android/releases" target="_blank" rel="noopener">ais-pos-kasir-android</a>, <a href="https://github.com/Zishof/ais-stok-opname-android/releases" target="_blank" rel="noopener">ais-stok-opname-android</a>), dan diperbarui secara berkala mengikuti perkembangan Sistem eGudang tanpa biaya tambahan bagi PIHAK PERTAMA selama masa kerja sama berlaku.</li>
            </ul>

            <div class="article-title">PASAL 3<br>HAK DAN KEWAJIBAN PARA PIHAK</div>
            <div class="clause-content fw-bold">1. Hak dan Kewajiban PIHAK PERTAMA:</div>
            <ul class="list-alpha mb-3">
                <li>Berhak menerima layanan Sistem eGudang sesuai dengan ruang lingkup pada Pasal 2.</li>
                <li>Berhak menerima layanan bimbingan teknis (<i>training</i>) secara daring secara gratis dan dukungan pemeliharaan dari PIHAK KEDUA.</li>
                <li>Berkewajiban menyediakan data master awal (produk, harga, stok awal, pemasok) yang valid untuk keperluan migrasi.</li>
                <li>Berkewajiban melaksanakan kewajiban pembayaran yang telah disepakati pada Pasal 4.</li>
                <li>Berkewajiban menunjuk penanggung jawab proyek dan/atau tim pelaksana internal (mis. kepala gudang, penanggung jawab kasir/outlet) untuk mendukung koordinasi, validasi kebutuhan, dan komunikasi selama implementasi.</li>
                <li>Berkewajiban menggunakan Sistem sesuai prosedur, menjaga kerahasiaan akun pengguna, serta memastikan data stok dan transaksi yang diberikan kepada PIHAK KEDUA adalah data yang sah.</li>
            </ul>

            <div class="clause-content fw-bold mt-3">2. Hak dan Kewajiban PIHAK KEDUA:</div>
            <ul class="list-alpha mb-3">
                <li>Berhak menerima pembayaran atas penyediaan layanan Sistem sesuai skema yang disepakati dari PIHAK PERTAMA.</li>
                <li>Berkewajiban melakukan instalasi, setup infrastruktur (<i>Cloud/On-Premise</i>), dan kustomisasi Sistem.</li>
                <li>Berkewajiban menjaga kerahasiaan seluruh basis data (termasuk data stok dan transaksi keuangan) milik PIHAK PERTAMA dan menanggulangi apabila terdapat kutu (<i>bug</i>) pada Sistem.</li>
                <li>Berkewajiban memberikan arahan teknis, dokumentasi penggunaan, serta pendampingan yang wajar agar proses adopsi Sistem oleh kasir dan petugas gudang dapat berjalan efektif.</li>
                <li>Berkewajiban melakukan pemeliharaan, penyempurnaan, dan pembaruan Sistem secara proporsional sepanjang masih berada dalam ruang lingkup kerja sama yang disepakati.</li>
            </ul>

            <div class="article-title">PASAL 4<br>SKEMA PEMBIAYAAN DAN PEMBAYARAN</div>

            <div class="clause-content fw-bold mt-3">4.1. Struktur Biaya Berlangganan</div>
            <div class="clause-content">
                Terkait dengan pembiayaan implementasi dan penggunaan modul eGudang, PARA PIHAK sepakat bahwa biaya berlangganan bulanan yang wajib dibayarkan oleh PIHAK PERTAMA terdiri atas penjumlahan dari: (a) biaya terminal Kasir (<i>Point of Sale/POS</i>) per Outlet sebagaimana diatur pada sub-klausul 4.1.1; dan (b) biaya Paket Modul Pusat per perusahaan/yayasan sebagaimana diatur pada sub-klausul 4.1.2, dalam hal PIHAK PERTAMA memilih untuk mengaktifkan salah satu Paket Modul Pusat. Kedua komponen biaya tersebut dijumlahkan menjadi 1 (satu) tagihan bulanan yang wajib dilunasi oleh PIHAK PERTAMA kepada PIHAK KEDUA.
            </div>

            <div class="clause-content fw-bold mt-3">4.1.1. Biaya Terminal Kasir (POS) per Outlet</div>
            <div class="clause-content">
                Besaran biaya terminal POS ditetapkan secara berjenjang (<i>tiered</i>) berdasarkan jumlah Outlet yang dioperasikan oleh PIHAK PERTAMA, dengan ketentuan sebagai berikut:
            </div>

            <table class="enhanced-table">
                <thead>
                    <tr>
                        <th><%= Common.getBahasaConfig("Jumlah Outlet") %></th>
                        <th><%= Common.getBahasaConfig("Biaya POS Pertama") %></th>
                        <th><%= Common.getBahasaConfig("Biaya POS Tambahan") %></th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>1 - 5 Outlet</td>
                        <td><strong>Rp 249.000,-</strong> /bulan/outlet</td>
                        <td><strong>Rp 99.000,-</strong> /bulan/unit</td>
                    </tr>
                    <tr>
                        <td>6 - 20 Outlet</td>
                        <td><strong>Rp 225.000,-</strong> /bulan/outlet</td>
                        <td><strong>Rp 85.000,-</strong> /bulan/unit</td>
                    </tr>
                    <tr>
                        <td>21 - 50 Outlet</td>
                        <td><strong>Rp 199.000,-</strong> /bulan/outlet</td>
                        <td><strong>Rp 75.000,-</strong> /bulan/unit</td>
                    </tr>
                    <tr>
                        <td>Lebih dari 50 Outlet</td>
                        <td>Mulai <strong>Rp 175.000,-</strong> /bulan/outlet</td>
                        <td>Mulai <strong>Rp 60.000,-</strong> /bulan/unit</td>
                    </tr>
                </tbody>
            </table>

            <div class="clause-content">
                Yang dimaksud dengan "POS Pertama" adalah 1 (satu) unit terminal kasir utama pada setiap Outlet, sedangkan "POS Tambahan" adalah unit terminal kasir kedua dan seterusnya yang dioperasikan pada Outlet yang sama. PARA PIHAK sepakat bahwa biaya POS Pertama sebagaimana diatur di atas telah mencakup fitur-fitur berikut, tanpa dikenakan biaya tambahan: transaksi penjualan, pembayaran tunai & nontunai, shift kasir, stok outlet, harga & diskon, data pelanggan, retur & pembatalan transaksi, laporan penjualan, pengaturan pengguna & hak akses, mode <i>offline-first</i>, sinkronisasi transaksi, serta <i>backup</i> data ke <i>cloud</i> secara standar. Biaya tersebut <strong>tidak mencakup</strong> pengadaan perangkat keras (tablet, printer kasir, pemindai barcode/<i>barcode scanner</i>, laci kasir/<i>cash drawer</i>) maupun biaya jaringan internet, yang sepenuhnya menjadi tanggung jawab dan beban PIHAK PERTAMA.
            </div>

            <div class="clause-content fw-bold mt-4">4.1.2. Biaya Paket Modul Pusat</div>
            <div class="clause-content">
                Biaya Paket Modul Pusat dikenakan per perusahaan/yayasan (bukan per Outlet) dan bersifat opsional, sesuai dengan kebutuhan konsolidasi dan pengelolaan terpusat yang dikehendaki oleh PIHAK PERTAMA, dengan pilihan paket sebagai berikut:
            </div>

            <table class="enhanced-table">
                <thead>
                    <tr>
                        <th><%= Common.getBahasaConfig("Paket") %></th>
                        <th><%= Common.getBahasaConfig("Cakupan") %></th>
                        <th><%= Common.getBahasaConfig("Biaya per Bulan") %></th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td><strong>Tanpa Paket Pusat</strong></td>
                        <td>Hanya modul POS &amp; manajemen stok per Outlet, tanpa konsolidasi pusat.</td>
                        <td><strong>Rp 0,-</strong></td>
                    </tr>
                    <tr>
                        <td><strong>Central Basic</strong></td>
                        <td>Dasbor pusat, stok konsolidasi, transfer antar-outlet, pembelian sederhana.</td>
                        <td><strong>Rp 750.000,-</strong> /bulan</td>
                    </tr>
                    <tr>
                        <td><strong>Central Professional</strong></td>
                        <td>Gudang pusat, pengadaan (<i>Purchase Request/Purchase Order</i>), distribusi, pengiriman, produksi &amp; perhitungan HPP.</td>
                        <td><strong>Rp 1.500.000,-</strong> /bulan</td>
                    </tr>
                    <tr>
                        <td><strong>Full Integrated Suite</strong></td>
                        <td>Seluruh cakupan modul pusat ditambah akuntansi, kepegawaian, investor, titik impas (<i>Break Even Point/BEP</i>), audit, dan analitik.</td>
                        <td><strong>Rp 2.500.000,-</strong> /bulan</td>
                    </tr>
                </tbody>
            </table>

            <div class="clause-content fw-bold mt-4">4.1.3. Biaya Implementasi Awal</div>
            <div class="clause-content">
                Selain biaya berlangganan bulanan sebagaimana diatur pada sub-klausul 4.1.1 dan 4.1.2 di atas, PIHAK PERTAMA dikenakan biaya implementasi awal yang dibayarkan 1 (satu) kali di muka (<i>one-time upfront</i>), dengan besaran sebagai berikut:
            </div>

            <table class="enhanced-table">
                <thead>
                    <tr>
                        <th><%= Common.getBahasaConfig("Skala Implementasi") %></th>
                        <th><%= Common.getBahasaConfig("Biaya Implementasi") %></th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>1 Outlet</td>
                        <td><strong>Rp 1.500.000,-</strong> s.d. <strong>Rp 3.000.000,-</strong></td>
                    </tr>
                    <tr>
                        <td>2 - 5 Outlet</td>
                        <td><strong>Rp 5.000.000,-</strong> s.d. <strong>Rp 10.000.000,-</strong></td>
                    </tr>
                    <tr>
                        <td>6 - 20 Outlet</td>
                        <td><strong>Rp 15.000.000,-</strong> s.d. <strong>Rp 25.000.000,-</strong></td>
                    </tr>
                    <tr>
                        <td>21 - 50 Outlet</td>
                        <td><strong>Rp 30.000.000,-</strong> s.d. <strong>Rp 50.000.000,-</strong></td>
                    </tr>
                    <tr>
                        <td>Implementasi seluruh Paket Modul Pusat (tambahan)</td>
                        <td><strong>Rp 10.000.000,-</strong> s.d. <strong>Rp 30.000.000,-</strong></td>
                    </tr>
                </tbody>
            </table>

            <div class="clause-content">
                PARA PIHAK sepakat bahwa biaya implementasi awal sebagaimana dimaksud di atas dapat dibebaskan/digratiskan sepenuhnya, apabila PARA PIHAK menyepakati kontrak kerja sama berjangka waktu 2 (dua) tahun dengan skema pembayaran tahunan di muka (<i>annual prepaid</i>).
            </div>

            <div class="clause-content fw-bold mt-4">4.1.4. Opsi Bagi Hasil per Transaksi (Alternatif)</div>
            <div class="clause-content">
                Sebagai skema alternatif dan/atau pelengkap bagi PIHAK PERTAMA yang menghendaki kerja sama tanpa biaya implementasi di muka sebagaimana diatur pada sub-klausul 4.1.3, PARA PIHAK dapat sepakat menggunakan skema bagi hasil per transaksi, dengan ketentuan PIHAK KEDUA memperoleh <i>fee</i> sebesar <strong>Rp 50,-</strong> hingga <strong>Rp 100,-</strong> untuk setiap transaksi kasir yang tercatat melalui Sistem, dengan ketentuan tagihan bulanan minimum tetap setara dengan biaya langganan POS reguler sebagaimana diatur pada sub-klausul 4.1.1.
            </div>

            <div class="clause-content fw-bold mt-4">4.2. Keterbukaan Negosiasi Kemitraan</div>
            <div class="clause-content">
                PARA PIHAK saling menyadari dan memahami bahwa seluruh nilai nominal, jenjang (<i>tier</i>), dan skema pembiayaan sebagaimana diatur dalam Pasal 4 ini merupakan <strong>skema standar yang tetap terbuka untuk dinegosiasikan dan/atau disesuaikan</strong> sesuai dengan skala usaha, jumlah Outlet, kebutuhan modul, serta kesepakatan akhir PARA PIHAK. PIHAK KEDUA bersedia dan berkomitmen untuk merumuskan penyesuaian kesepakatan nilai akhir, agar Sistem ini dapat segera diimplementasikan secara optimal dan secepatnya memberikan kemanfaatan digitalisasi yang nyata bagi PIHAK PERTAMA.
            </div>

            <div class="clause-content mt-3">
                Segala pajak yang timbul akibat pelaksanaan Perjanjian ini akan ditanggung oleh masing-masing pihak sesuai dengan regulasi perpajakan Republik Indonesia.
            </div>

            <div class="page-break"></div>

            <div class="article-title">PASAL 5<br>PELATIHAN (TRAINING) DAN PENDAMPINGAN</div>
            <ol class="list-number">
                <li>PIHAK KEDUA memberikan fasilitas Pelatihan Penggunaan Sistem (<i>Training</i>) bagi kasir, petugas gudang, staf pengadaan, dan akuntan secara <strong>Daring (Online)</strong> tanpa dipungut biaya (100% Gratis).</li>
                <li>Apabila PIHAK PERTAMA menghendaki Pelatihan/Sosialisasi secara <strong>Tatap Muka (Offline/On-site)</strong>, jasa instruktur dari PIHAK KEDUA tetap tidak dikenakan biaya. PIHAK PERTAMA hanya berkewajiban menanggung seluruh biaya transportasi dan akomodasi bagi tim implementator PIHAK KEDUA selama bertugas di lokasi.</li>
            </ol>

            <div class="clause-content fw-bold mt-4">5.1. Tahapan Implementasi yang Direkomendasikan</div>
            <ul class="timeline-list">
                <li data-step="1"><strong>Kick-off &amp; Penyelarasan Kebutuhan:</strong> penyamaan ruang lingkup, pembentukan tim pelaksana, penentuan prioritas modul, serta penyusunan rencana kerja awal.</li>
                <li data-step="2"><strong>Konfigurasi &amp; Migrasi Awal:</strong> setup lingkungan sistem, master produk/harga/stok awal/pemasok, hak akses, dan data awal sesuai format yang disepakati.</li>
                <li data-step="3"><strong>Pelatihan &amp; Uji Coba Terbatas:</strong> pelatihan kasir, petugas gudang, dan staf pengadaan; simulasi alur transaksi kasir-stok-pengadaan; validasi oleh unit terkait.</li>
                <li data-step="4"><strong>Go-Live Bertahap:</strong> aktivasi modul prioritas (mis. POS &amp; Gudang terlebih dahulu), monitoring pemakaian, pendampingan penyelesaian kendala.</li>
                <li data-step="5"><strong>Evaluasi &amp; Optimalisasi:</strong> evaluasi pemanfaatan Sistem, penyempurnaan konfigurasi, serta rekomendasi pengembangan lanjutan (termasuk roadmap Ekspedisi, Investor, dan Kepegawaian Outlet).</li>
            </ul>

            <div class="article-title">PASAL 6<br>JAMINAN LAYANAN & PEMELIHARAAN (SLA)</div>
            <ol class="list-number">
                <li>PIHAK KEDUA menjamin bahwa Sistem yang diimplementasikan akan berfungsi dengan baik dan sesuai dengan spesifikasi.</li>
                <li><strong>Jaminan Kelangsungan Layanan:</strong> PIHAK KEDUA memberikan jaminan mutlak bahwa apabila di kemudian hari terjadi pengunduran diri personel, pemutusan kontrak sepihak dari tim teknis, atau perubahan status manajemen internal dari pihak pengembang/PIHAK KEDUA, Sistem akan <strong>tetap dapat digunakan secara normal</strong> oleh PIHAK PERTAMA hingga berakhirnya masa kontrak, tanpa penghentian layanan, pemblokiran, atau penuntutan biaya tambahan dari pihak lain mana pun.</li>
                <li>Untuk implementasi berbasis <i>Cloud</i>, PIHAK KEDUA menjamin tingkat ketersediaan (<i>Uptime</i>) tidak kurang dari 99% per tahun, di luar waktu pemeliharaan terencana.</li>
            </ol>

            <div class="clause-content fw-bold mt-4">6.1. Kategori Dukungan Layanan</div>
            <table class="enhanced-table">
                <thead>
                    <tr>
                        <th><%= Common.getBahasaConfig("Kategori") %></th>
                        <th><%= Common.getBahasaConfig("Contoh Kondisi") %></th>
                        <th><%= Common.getBahasaConfig("Prioritas Penanganan") %></th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td><strong>Kritis</strong></td>
                        <td>Sistem utama tidak dapat diakses secara luas atau layanan kasir/gudang inti berhenti berjalan.</td>
                        <td>Diprioritaskan untuk investigasi dan pemulihan layanan secepat mungkin.</td>
                    </tr>
                    <tr>
                        <td><strong>Tinggi</strong></td>
                        <td>Fungsi penting terganggu namun masih tersedia alternatif proses sementara.</td>
                        <td>Ditangani secara prioritas berdasarkan dampak operasional ritel dan jadwal layanan.</td>
                    </tr>
                    <tr>
                        <td><strong>Normal</strong></td>
                        <td>Pertanyaan penggunaan, penyesuaian minor, koreksi tampilan, atau dukungan operasional rutin.</td>
                        <td>Ditangani melalui kanal dukungan resmi sesuai antrean dan kesepakatan PARA PIHAK.</td>
                    </tr>
                </tbody>
            </table>

            <div class="article-title">PASAL 7<br>KERAHASIAAN DATA & HAK KEKAYAAN INTELEKTUAL</div>
            <ol class="list-number">
                <li><strong>Kepemilikan Data:</strong> Segala bentuk data stok, transaksi kasir, dan finansial yang diunggah ke dalam Sistem adalah milik sah PIHAK PERTAMA. PIHAK KEDUA berkewajiban menjaga kerahasiaan penuh dan tidak berhak mengeksploitasinya untuk kepentingan komersial lain.</li>
                <li><strong>Hak Kekayaan Intelektual (HKI):</strong> Segala hak cipta atas desain perangkat lunak, kode program (<i>source code</i>), sistem basis data, dan arsitektur algoritma modul eGudang mutlak merupakan properti intelektual milik PIHAK KEDUA. PIHAK PERTAMA hanya memegang lisensi hak guna selama masa Perjanjian.</li>
            </ol>

            <div class="highlight-box">
                <strong>Prinsip Perlindungan Data Bisnis:</strong> Setiap akses, pemrosesan, penggunaan, pencadangan, dan pemindahan data stok dan transaksi dilakukan secara terbatas untuk kepentingan pelaksanaan Perjanjian ini. PIHAK KEDUA tidak diperkenankan menjual, mengalihkan, atau menggunakan data PIHAK PERTAMA untuk kepentingan lain tanpa persetujuan tertulis dari PIHAK PERTAMA, kecuali diwajibkan oleh ketentuan peraturan perundang-undangan yang berlaku (termasuk regulasi perlindungan data pribadi).
            </div>

            <div class="article-title">PASAL 8<br>MASA BERLAKU & PENGAKHIRAN PERJANJIAN</div>
            <ol class="list-number">
                <li>Perjanjian Kerja Sama ini berlaku terhitung sejak ditandatangani untuk jangka waktu <strong><%=masaKontrak%></strong> dan mengikat sepenuhnya bagi PARA PIHAK.</li>
                <li>Apabila Perjanjian ini berakhir atau diputus, PIHAK KEDUA wajib menyerahkan (<i>ekspor/dump</i>) seluruh basis data (termasuk data stok dan transaksi) milik PIHAK PERTAMA dalam format standar (SQL/CSV/Excel) sebelum akses Sistem ditutup secara permanen.</li>
                <li>PARA PIHAK dapat menyusun rencana transisi layanan secara tertulis apabila terjadi pengakhiran Perjanjian, termasuk jadwal ekspor data, penyelesaian kewajiban pembayaran, dan penutupan akses pengguna.</li>
            </ol>

            <div class="article-title">PASAL 9<br>KEADAAN MEMAKSA (FORCE MAJEURE)</div>
            <div class="clause-content">
                PARA PIHAK dibebaskan dari tanggung jawab atas keterlambatan atau kegagalan pemenuhan kewajiban apabila disebabkan oleh <i>Force Majeure</i>, yang mencakup: bencana alam, peperangan, huru-hara, epidemi/pandemi berskala nasional, kebakaran, pemogokan massal, serta kebijakan pemerintah yang secara langsung menghalangi pelaksanaan Perjanjian ini.
            </div>

            <div class="article-title">PASAL 10<br>PENYELESAIAN PERSELISIHAN</div>
            <div class="clause-content">
                Setiap perselisihan yang timbul berkenaan dengan pelaksanaan Perjanjian ini akan diselesaikan secara musyawarah untuk mufakat. Apabila dalam waktu 30 (tiga puluh) hari musyawarah tidak mencapai mufakat, PARA PIHAK sepakat untuk menyelesaikan perselisihan melalui jalur hukum di yurisdiksi Pengadilan Negeri <%=kotaPks%>.
            </div>

            <div class="page-break"></div>

            <div class="article-title">PASAL 11<br>LAIN-LAIN DAN PENUTUP</div>
            <ol class="list-number mb-5">
                <li>Segala perubahan, penambahan modul fitur, atau pengurangan terhadap ketentuan di dalam Perjanjian ini hanya sah dan mengikat apabila dibuat secara tertulis dan ditandatangani oleh PARA PIHAK dalam bentuk Adendum.</li>
                <li>Perjanjian ini dibuat dalam rangkap 2 (dua) asli, masing-masing bermeterai cukup (Rp 10.000,-), ditandatangani oleh PARA PIHAK, dan mempunyai kekuatan pembuktian hukum yang sama.</li>
                <li>Lampiran teknis, berita acara, dokumen penawaran, jadwal implementasi, atau dokumen pendukung lain yang disepakati PARA PIHAK dapat menjadi bagian yang tidak terpisahkan dari Perjanjian ini.</li>
                <li>Apabila terdapat ketentuan yang belum diatur secara rinci dalam Perjanjian ini, PARA PIHAK akan mengaturnya kemudian secara tertulis berdasarkan prinsip musyawarah, kepatutan, dan saling menguntungkan.</li>
            </ol>

            <div class="clause-content text-center mb-2">
                Demikian Perjanjian ini dibuat dengan iktikad baik untuk dipatuhi dan dilaksanakan oleh PARA PIHAK.
            </div>

            <div class="closing-note">
                Dengan ditandatanganinya Perjanjian ini, PARA PIHAK menyatakan telah membaca, memahami, dan menyetujui seluruh ketentuan yang termuat di dalam dokumen ini, serta berkomitmen untuk melaksanakan kerja sama secara profesional demi terwujudnya tata kelola gudang dan ritel yang lebih modern, efektif, transparan, dan berkelanjutan.
            </div>

            <div class="signature-section">
                <div class="signature-box">
                    <strong>PIHAK PERTAMA</strong><br>
                    <%=namaClient%><br>
                    <div class="signature-space">
                        <br><br>
                        <span class="text-muted" style="font-size: 0.8rem;">(Materai Rp 10.000 & Tanda Tangan)</span>
                    </div>
                    <strong><u>...................................................</u></strong><br>
                    Pimpinan / Direktur / Perwakilan Sah
                </div>

                <div class="signature-box">
                    <strong>PIHAK KEDUA</strong><br>
                    <%=judul%><br>
                    <div class="signature-space">
                        <br><br>
                        <span class="text-muted" style="font-size: 0.8rem;">(Materai Rp 10.000 & Tanda Tangan)</span>
                    </div>
                    <strong><u><%=namaPenyedia%></u></strong><br>
                    Penyedia Sistem / Konsultan
                </div>
            </div>

            <div class="doc-footer">
                <span>Dokumen: Draf PKS Modul eGudang (Gudang & Ritel)</span>
                <span><%=namaClient%> &mdash; <%=judul%></span>
            </div>

        </div>
    </div>

    <script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
    <jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp" />
</body>
</html>
