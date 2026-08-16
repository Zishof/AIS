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
String namaClient = (paramClient != null && !paramClient.trim().isEmpty()) ? paramClient : "[NAMA RUMAH SAKIT/KLINIK/PUSKESMAS KLIEN]";
String namaPenyedia = (paramPresentBy != null && !paramPresentBy.trim().isEmpty()) ? paramPresentBy : "[NAMA PENYEDIA SISTEM / KONSULTAN]";
String kontakPenyedia = (paramTelp != null && !paramTelp.trim().isEmpty()) ? paramTelp : Telepon;

// Tanggal Hari Ini untuk format PKS
Calendar cal = Calendar.getInstance();
String hari = new SimpleDateFormat("EEEE", new Locale("id", "ID")).format(cal.getTime());
String tanggalStr = new SimpleDateFormat("dd", new Locale("id", "ID")).format(cal.getTime());
String bulan = new SimpleDateFormat("MMMM", new Locale("id", "ID")).format(cal.getTime());
String tahunStr = new SimpleDateFormat("yyyy", new Locale("id", "ID")).format(cal.getTime());

// Parameter opsional untuk kebutuhan finalisasi dokumen PKS
String nomorPks = (paramNomorPks != null && !paramNomorPks.trim().isEmpty()) ? paramNomorPks : "...... / PKS-EMEDIC / EE / " + tahunStr;
String kotaPks = (paramKotaPks != null && !paramKotaPks.trim().isEmpty()) ? paramKotaPks : ".......................................";
String lokasiPks = (paramLokasiPks != null && !paramLokasiPks.trim().isEmpty()) ? paramLokasiPks : ".......................................";
String namaWakilClient = (paramNamaWakilClient != null && !paramNamaWakilClient.trim().isEmpty()) ? paramNamaWakilClient : ".........................................................";
String jabatanClient = (paramJabatanClient != null && !paramJabatanClient.trim().isEmpty()) ? paramJabatanClient : "[JABATAN PIMPINAN / DIREKTUR]";
String alamatClient = (paramAlamatClient != null && !paramAlamatClient.trim().isEmpty()) ? paramAlamatClient : "[ALAMAT RUMAH SAKIT/KLINIK/PUSKESMAS]";
String masaKontrak = (paramMasaKontrak != null && !paramMasaKontrak.trim().isEmpty()) ? paramMasaKontrak : "...... (............) tahun";
%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= Common.getBahasaConfig("Draf PKS - Modul Kesehatan (eMedic)") %> | <%=judul%></title>

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
            content: "EMEDIC - ENTERPRISE EDUCATION";
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
            /* 7.5rem supaya lolos dari tombol "Tanya Jawab" (bantuan_button.jsp:
               right:16px; bottom:62px), yang tanpa ini tertutup toolbar cetak ini. */
            bottom: 7.5rem;
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
            .print-toolbar { left: 1rem; right: 1rem; bottom: 7.5rem; justify-content: center; }
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
        <span class="toolbar-hint"><i class="fas fa-file-contract me-1"></i> Draf PKS Modul Kesehatan siap cetak</span>
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
                <div class="doc-subtitle">PENGADAAN DAN IMPLEMENTASI SISTEM INFORMASI<br>RUMAH SAKIT, KLINIK & PUSKESMAS (eMedic)</div>
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
                <div class="summary-headline">Landasan kerja sama untuk membangun sistem informasi rumah sakit/klinik/puskesmas yang terintegrasi, terukur, aman, dan berkelanjutan.</div>
                <div class="clause-content mb-2">
                    Dokumen ini disusun sebagai draf Perjanjian Kerja Sama yang memuat ruang lingkup implementasi modul <strong>eMedic (Sistem Informasi Rumah Sakit, Klinik &amp; Puskesmas)</strong>, hak dan kewajiban PARA PIHAK, skema pembiayaan, dukungan layanan, perlindungan data, serta mekanisme pelaksanaan kerja sama. Modul ini merupakan bagian dari ekosistem <strong>Enterprise Education</strong> yang sama, sehingga dapat terintegrasi langsung apabila PIHAK PERTAMA merupakan Universitas, Sekolah, atau Pondok Pesantren yang memiliki klinik/rumah sakit/layanan medis sendiri, maupun diimplementasikan secara berdiri sendiri (<i>standalone</i>) bagi Rumah Sakit, Klinik, atau Puskesmas independen.
                </div>
                <div class="summary-card-grid">
                    <div class="summary-card">
                        <i class="fas fa-diagram-project"></i>
                        <strong>Terintegrasi End-to-End</strong>
                        <span>Menghubungkan pendaftaran, rekam medis, farmasi, gudang, billing, dan dashboard analitik dalam satu ekosistem.</span>
                    </div>
                    <div class="summary-card">
                        <i class="fas fa-chart-line"></i>
                        <strong>Bernilai Strategis</strong>
                        <span>Mendorong efisiensi operasional klinis, transparansi stok obat, percepatan layanan pasien, dan dashboard pengambilan keputusan pimpinan.</span>
                    </div>
                    <div class="summary-card">
                        <i class="fas fa-lock"></i>
                        <strong>Aman &amp; Berkelanjutan</strong>
                        <span>Menegaskan kepemilikan data rekam medis, kerahasiaan informasi pasien, jaminan layanan, pelatihan, serta pendampingan implementasi.</span>
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
                <li>Bahwa PIHAK PERTAMA adalah Rumah Sakit, Klinik, Puskesmas, atau institusi pendidikan (Universitas/Sekolah/Pondok Pesantren) yang memiliki fasilitas layanan kesehatan, yang membutuhkan sistem informasi manajemen rumah sakit/klinik (SIRS) yang komprehensif, modern, dan terintegrasi.</li>
                <li>Bahwa PIHAK KEDUA adalah badan/pihak penyedia layanan teknologi informasi yang memiliki kompetensi, hak cipta, dan kewenangan atas perangkat lunak Sistem Informasi Rumah Sakit/Klinik/Puskesmas (eMedic), yang merupakan modul dari ekosistem Enterprise Education.</li>
                <li>Bahwa PARA PIHAK memandang digitalisasi tata kelola layanan kesehatan sebagai kebutuhan strategis untuk meningkatkan mutu layanan, keselamatan pasien, transparansi pengelolaan farmasi/stok, akurasi rekam medis, serta efektivitas pengambilan keputusan berbasis informasi.</li>
                <li>Bahwa kerja sama ini tidak hanya berorientasi pada pengadaan aplikasi, tetapi juga mencakup pendampingan perubahan proses kerja klinis, penguatan kapasitas tenaga medis dan staf, serta penyelarasan sistem dengan kebutuhan operasional PIHAK PERTAMA.</li>
                <li>Bahwa PARA PIHAK berkomitmen menjalankan kerja sama secara profesional, akuntabel, saling menguntungkan, serta berlandaskan prinsip kerahasiaan data rekam medis dan keberlanjutan layanan.</li>
            </ul>

            <div class="clause-content">
                Berdasarkan hal-hal tersebut di atas, PARA PIHAK sepakat untuk mengikatkan diri dalam Perjanjian Kerja Sama Implementasi Modul eMedic dengan ketentuan dan syarat-syarat yang diatur dalam pasal-pasal berikut:
            </div>

            <div class="legal-note">
                <strong>Catatan Penyelarasan:</strong> Draf PKS ini dapat disesuaikan kembali berdasarkan hasil pembahasan teknis, legal, keuangan, dan kebutuhan operasional PIHAK PERTAMA sebelum ditandatangani. Penyesuaian tersebut dapat dituangkan dalam bentuk finalisasi naskah, berita acara, lampiran teknis, atau adendum sesuai kesepakatan PARA PIHAK.
            </div>

            <div class="article-title">PASAL 1<br>DEFINISI</div>
            <div class="clause-content">
                Kecuali ditentukan lain dalam konteks kalimatnya, istilah-istilah di bawah ini memiliki pengertian sebagai berikut:
                <ol class="list-number mt-2">
                    <li><strong>Sistem / Perangkat Lunak</strong> adalah modul <i>eMedic</i> berbasis web, yaitu Sistem Informasi Rumah Sakit, Klinik, dan Puskesmas yang dikembangkan oleh PIHAK KEDUA sebagai bagian dari ekosistem Enterprise Education (eCampus/eSchool/ePesantren).</li>
                    <li><strong>Implementasi</strong> adalah serangkaian kegiatan yang mencakup instalasi, konfigurasi, migrasi data, serta pelatihan penggunaan (<i>training</i>) Sistem kepada sumber daya manusia milik PIHAK PERTAMA (dokter, perawat, farmasis, kasir, petugas gudang, admin).</li>
                    <li><strong>Rekam Medis Elektronik</strong> adalah catatan pemeriksaan, diagnosa (berkode ICD-10), tindakan, dan resep pasien yang tersimpan pada Sistem.</li>
                    <li><strong>Transaksi Obat/Farmasi</strong> adalah transaksi penjualan obat, alat medis, atau tindakan yang tercatat dan dihitung otomatis oleh Sistem sebagai dasar tagihan pasien.</li>
                    <li><strong>Dashboard Analitik</strong> adalah halaman ringkasan informasi operasional (ringkasan pendaftaran, kunjungan, pendapatan, diagnosa terbanyak, okupansi tempat tidur, kadaluarsa farmasi) sebagai bahan monitoring dan pengambilan keputusan pimpinan.</li>
                    <li><strong>Lampiran Teknis</strong> adalah dokumen pendukung yang memuat rincian modul, konfigurasi, tahapan implementasi, kebutuhan data, jadwal pelaksanaan, atau kesepakatan teknis lain yang menjadi satu kesatuan dengan Perjanjian ini.</li>
                </ol>
            </div>

            <div class="article-title">PASAL 2<br>RUANG LINGKUP PEKERJAAN & FITUR SISTEM</div>
            <div class="clause-content">
                PIHAK KEDUA sepakat untuk menyediakan, menginstalasi, dan memelihara Sistem eMedic bagi PIHAK PERTAMA. Ruang lingkup fungsionalitas Sistem yang disediakan secara komprehensif meliputi:
            </div>

            <div class="highlight-box">
                <strong>Orientasi Solusi:</strong> eMedic dirancang sebagai platform tata kelola layanan kesehatan terpadu yang membantu Rumah Sakit, Klinik, dan Puskesmas menata proses klinis, mengurangi pekerjaan manual berulang, mempercepat layanan kepada pasien, meningkatkan akurasi stok obat, dan menghadirkan laporan manajemen yang lebih siap digunakan oleh pimpinan.
            </div>

            <div class="value-grid">
                <div class="value-card">
                    <i class="fas fa-database"></i>
                    <strong>Satu Data Fasilitas Kesehatan</strong>
                    <span>Data pasien, rekam medis, farmasi, gudang, dan billing dapat dikelola lebih konsisten dalam satu basis data terpadu.</span>
                </div>
                <div class="value-card">
                    <i class="fas fa-gauge-high"></i>
                    <strong>Proses Lebih Cepat</strong>
                    <span>Alur pendaftaran, pemeriksaan, dispensing obat, dan pembayaran dapat dipercepat melalui automasi.</span>
                </div>
                <div class="value-card">
                    <i class="fas fa-users-gear"></i>
                    <strong>Kolaborasi Unit Klinis</strong>
                    <span>Dokter, perawat, farmasis, kasir, dan petugas gudang memiliki portal dan hak akses yang sesuai perannya.</span>
                </div>
            </div>

            <div class="clause-content fw-bold mt-3">2.1. Registrasi &amp; Rekam Medis</div>
            <ul class="list-dash">
                <li><strong>Registrasi Rawat Jalan, Rawat Inap, dan UGD:</strong> pendaftaran pasien, antrian, pemilihan poli/dokter/kelas perawatan, hingga penerbitan nomor antrian otomatis.</li>
                <li><strong>Rekam Medis &amp; Diagnosa:</strong> pencatatan keluhan, pemeriksaan, dan diagnosa dengan pengkodean standar ICD-10.</li>
            </ul>

            <div class="clause-content fw-bold mt-3">2.2. Farmasi, Racikan &amp; Gudang</div>
            <ul class="list-dash">
                <li><strong>Resep Digital &amp; Racikan/Kompounding:</strong> resep dari dokter diteruskan ke farmasi, termasuk racikan dengan komposisi baku.</li>
                <li><strong>Manajemen Stok &amp; Kadaluarsa:</strong> pencatatan stok per-batch dengan tanggal kadaluarsa, prinsip FEFO (<i>First-Expired-First-Out</i>), serta hierarki gudang pusat-cabang.</li>
                <li><strong>Rantai Pasok:</strong> saldo awal, permintaan pembelian, pesanan ke pemasok, penerimaan barang, transfer antar gudang, pemakaian, dan koreksi stok.</li>
            </ul>

            <div class="clause-content fw-bold mt-3">2.3. Transaksi, Billing &amp; Kasir</div>
            <ul class="list-dash">
                <li><strong>Tagihan Terpadu:</strong> biaya obat, tindakan medis, alat medis, dan paket layanan otomatis tergabung dalam satu tagihan pasien.</li>
                <li><strong>Pembayaran:</strong> mendukung tunai, non-tunai, dan deposit, dengan struk/kwitansi tercetak otomatis.</li>
                <li><strong>Harga &amp; Tarif:</strong> HPP (Harga Pokok Penjualan) dihitung otomatis dari rollup harga beli; tarif khusus dapat diterapkan per kelas perawatan atau per payer/asuransi.</li>
            </ul>

            <div class="clause-content fw-bold mt-3">2.4. Dashboard Analitik &amp; Pelaporan</div>
            <ul class="list-dash">
                <li><strong>Dashboard Operasional:</strong> ringkasan pendaftaran, kunjungan rawat jalan (mingguan/bulanan), pendapatan, 10 diagnosa terbanyak, okupansi tempat tidur, dan kadaluarsa farmasi.</li>
                <li><strong>Laporan Siap Pakai:</strong> mencakup laporan pasien &amp; rekam medis, kunjungan (termasuk format resmi RL5/RL18/RL21), rawat inap, kasir, inventory, dan pengadaan.</li>
            </ul>

            <div class="page-break"></div>

            <div class="clause-content fw-bold mt-3">2.5. Keamanan Data &amp; Jejak Audit</div>
            <ul class="list-dash">
                <li><strong>Jejak Audit Menyeluruh:</strong> setiap perubahan data stok, resep, tarif, dan rekam medis tercatat (siapa, kapan, apa yang diubah).</li>
                <li><strong>Hak Akses Berjenjang:</strong> dokter, perawat, farmasis, kasir, petugas gudang, dan admin memiliki akses sesuai perannya.</li>
            </ul>

            <div class="clause-content fw-bold mt-3">2.6. Roadmap Integrasi BPJS/JKN &amp; SATUSEHAT</div>
            <div class="clause-content">
                PIHAK KEDUA menyediakan fondasi pengembangan bertahap untuk integrasi eksternal, tanpa mengganggu operasional yang sedang berjalan, dengan tahapan sebagai berikut: <strong>Fase 1</strong> Fondasi Data (NIK, nomor kartu BPJS, data kepesertaan pasien); <strong>Fase 2</strong> BPJS Eligibilitas (VClaim, PCare, penerbitan SEP otomatis); <strong>Fase 3</strong> Integrasi SATUSEHAT Kemenkes (rekam medis elektronik nasional); <strong>Fase 4</strong> Klaim INA-CBG. Jadwal penyelesaian tiap fase diatur lebih lanjut dalam Lampiran Teknis sesuai kesiapan regulasi dan kebutuhan PIHAK PERTAMA.
            </div>

            <div class="clause-content fw-bold mt-4">2.7. Output Implementasi dan Deliverable Utama</div>
            <div class="deliverable-grid">
                <div class="deliverable-card">
                    <i class="fas fa-server"></i>
                    <strong>Lingkungan Sistem</strong>
                    <span>Instalasi, setup server/cloud atau on-premise, konfigurasi awal, hak akses, dan pengaturan parameter fasilitas kesehatan.</span>
                </div>
                <div class="deliverable-card">
                    <i class="fas fa-file-import"></i>
                    <strong>Migrasi Data Awal</strong>
                    <span>Pendampingan penyiapan data master (pasien, dokter, item obat, tarif) dan impor data awal sesuai format yang disepakati.</span>
                </div>
                <div class="deliverable-card">
                    <i class="fas fa-chalkboard-user"></i>
                    <strong>Pelatihan Pengguna</strong>
                    <span>Transfer pengetahuan kepada dokter, perawat, farmasis, dan kasir agar Sistem dapat digunakan secara mandiri.</span>
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
                        <td>Registrasi &amp; Rekam Medis</td>
                        <td>Proses pendaftaran, pemeriksaan, dan pencatatan diagnosa berjalan lebih cepat, tertib, dan terdokumentasi.</td>
                    </tr>
                    <tr>
                        <td>Farmasi &amp; Stok</td>
                        <td>Stok obat lebih akurat, kadaluarsa terdeteksi lebih dini, dan kerugian akibat obat terbuang dapat ditekan.</td>
                    </tr>
                    <tr>
                        <td>Billing &amp; Kasir</td>
                        <td>Tagihan, pembayaran, dan laporan pendapatan dapat dipantau lebih transparan dan minim verifikasi manual.</td>
                    </tr>
                    <tr>
                        <td>Data &amp; Pelaporan</td>
                        <td>Data tersaji dalam dashboard dan laporan yang siap dipakai untuk monitoring, audit, dan akreditasi fasilitas kesehatan.</td>
                    </tr>
                    <tr>
                        <td>Adopsi Pengguna</td>
                        <td>Dokter, perawat, farmasis, kasir, petugas gudang, dan admin dapat menggunakan layanan sesuai peran masing-masing.</td>
                    </tr>
                </tbody>
            </table>

            <div class="article-title">PASAL 3<br>HAK DAN KEWAJIBAN PARA PIHAK</div>
            <div class="clause-content fw-bold">1. Hak dan Kewajiban PIHAK PERTAMA:</div>
            <ul class="list-alpha mb-3">
                <li>Berhak menerima layanan Sistem eMedic sesuai dengan ruang lingkup pada Pasal 2.</li>
                <li>Berhak menerima layanan bimbingan teknis (<i>training</i>) secara daring secara gratis dan dukungan pemeliharaan dari PIHAK KEDUA.</li>
                <li>Berkewajiban menyediakan data master awal (pasien, dokter, item obat, tarif) yang valid untuk keperluan migrasi.</li>
                <li>Berkewajiban melaksanakan kewajiban pembayaran yang telah disepakati pada Pasal 4.</li>
                <li>Berkewajiban menunjuk penanggung jawab proyek dan/atau tim pelaksana internal (mis. kepala instalasi farmasi, kepala rekam medis) untuk mendukung koordinasi, validasi kebutuhan, dan komunikasi selama implementasi.</li>
                <li>Berkewajiban menggunakan Sistem sesuai prosedur, menjaga kerahasiaan akun pengguna, serta memastikan data rekam medis dan farmasi yang diberikan kepada PIHAK KEDUA adalah data yang sah.</li>
            </ul>

            <div class="clause-content fw-bold mt-3">2. Hak dan Kewajiban PIHAK KEDUA:</div>
            <ul class="list-alpha mb-3">
                <li>Berhak menerima pembayaran atas penyediaan layanan Sistem sesuai skema yang disepakati dari PIHAK PERTAMA.</li>
                <li>Berkewajiban melakukan instalasi, setup infrastruktur (<i>Cloud/On-Premise</i>), dan kustomisasi Sistem.</li>
                <li>Berkewajiban menjaga kerahasiaan seluruh basis data (termasuk data rekam medis) milik PIHAK PERTAMA dan menanggulangi apabila terdapat kutu (<i>bug</i>) pada Sistem.</li>
                <li>Berkewajiban memberikan arahan teknis, dokumentasi penggunaan, serta pendampingan yang wajar agar proses adopsi Sistem oleh tenaga medis dapat berjalan efektif.</li>
                <li>Berkewajiban melakukan pemeliharaan, penyempurnaan, dan pembaruan Sistem secara proporsional sepanjang masih berada dalam ruang lingkup kerja sama yang disepakati.</li>
            </ul>

            <div class="article-title">PASAL 4<br>SKEMA PEMBIAYAAN DAN PEMBAYARAN</div>

            <div class="clause-content fw-bold mt-3">4.1. Klasifikasi Fasilitas &amp; Prinsip Umum Pembiayaan</div>
            <div class="clause-content">
                Terkait dengan pembiayaan implementasi dan lisensi penggunaan modul eMedic, PARA PIHAK sepakat menggunakan salah satu dari tiga model pembiayaan sebagaimana diatur pada Pasal 4.2, 4.3, dan 4.4. Besaran biaya disesuaikan dengan klasifikasi fasilitas kesehatan PIHAK PERTAMA (Klinik, Puskesmas, atau Rumah Sakit sesuai kelas/tingkatannya) sebagaimana dijabarkan pada tabel di masing-masing opsi. Seluruh nilai yang tercantum bersifat sebagai <strong>titik awal negosiasi</strong> sebagaimana diatur lebih lanjut pada Pasal 4.6.
            </div>

            <div class="clause-content text-center fst-italic my-3 p-3 border pricing-box">
                [PILIH SALAH SATU OPSI DI BAWAH DAN HAPUS YANG TIDAK PERLU, SESUAI HASIL NEGOSIASI PARA PIHAK]
            </div>

            <div class="clause-content fw-bold mt-4">4.2. Opsi A &mdash; Beli Putus (Lisensi Permanen)</div>
            <div class="clause-content">
                PIHAK PERTAMA membayar sekali di muka sesuai klasifikasi fasilitas sebagai berikut. Sistem menjadi aset milik PIHAK PERTAMA sepenuhnya, tanpa biaya lisensi bulanan. Harga sudah mencakup lisensi seluruh modul paket, implementasi dasar, dan dukungan teknis tahun pertama untuk satu lokasi fasilitas.
            </div>
            <table class="enhanced-table">
                <thead>
                    <tr>
                        <th><%= Common.getBahasaConfig("Klasifikasi Fasilitas") %></th>
                        <th><%= Common.getBahasaConfig("Harga Beli Putus") %></th>
                    </tr>
                </thead>
                <tbody>
                    <tr><td>Klinik</td><td>Rp 100.000.000,-</td></tr>
                    <tr><td>Puskesmas Nonrawat Inap Dasar</td><td>Rp 150.000.000,-</td></tr>
                    <tr><td>Puskesmas Nonrawat Inap Lengkap / 24 Jam</td><td>Rp 225.000.000,-</td></tr>
                    <tr><td>Puskesmas Rawat Inap</td><td>Rp 325.000.000,-</td></tr>
                    <tr><td>Puskesmas Rawat Inap Terpencil</td><td>Rp 425.000.000,-</td></tr>
                    <tr><td>Puskesmas Sangat Terpencil / Offline-First</td><td>Rp 500.000.000,-</td></tr>
                    <tr><td>Rumah Sakit Pratama</td><td>Rp 250.000.000,-</td></tr>
                    <tr><td>Rumah Sakit Kelas D</td><td>Rp 400.000.000,-</td></tr>
                    <tr><td>Rumah Sakit Kelas C</td><td>Rp 750.000.000,-</td></tr>
                    <tr><td>Rumah Sakit Kelas B</td><td>Rp 1.400.000.000,-</td></tr>
                    <tr><td>Rumah Sakit Kelas A</td><td>Rp 2.750.000.000,-</td></tr>
                </tbody>
            </table>
            <div class="clause-content">
                Infrastruktur server, migrasi data yang kompleks, integrasi dengan alat medis, kustomisasi khusus di luar modul paket, serta penempatan tenaga pendamping PIHAK KEDUA di lokasi (onsite) tidak termasuk dalam harga di atas dan diperhitungkan secara terpisah sesuai kebutuhan yang disepakati PARA PIHAK.
            </div>

            <div class="clause-content fw-bold mt-4">4.3. Opsi B &mdash; Langganan Bulanan</div>
            <div class="clause-content">
                PIHAK PERTAMA menanggung biaya berlangganan Sistem setiap bulan sesuai klasifikasi fasilitas sebagai berikut, termasuk hosting/cloud standar, pemeliharaan server, pembaruan sistem berkala, backup data rutin, dukungan teknis jarak jauh, dan pembaruan integrasi standar untuk satu lokasi fasilitas.
            </div>
            <table class="enhanced-table">
                <thead>
                    <tr>
                        <th><%= Common.getBahasaConfig("Klasifikasi Fasilitas") %></th>
                        <th><%= Common.getBahasaConfig("Biaya Langganan / Bulan") %></th>
                    </tr>
                </thead>
                <tbody>
                    <tr><td>Klinik</td><td>Rp 2.500.000,-</td></tr>
                    <tr><td>Puskesmas Nonrawat Inap Dasar</td><td>Rp 4.000.000,-</td></tr>
                    <tr><td>Puskesmas Nonrawat Inap Lengkap / 24 Jam</td><td>Rp 5.500.000,-</td></tr>
                    <tr><td>Puskesmas Rawat Inap</td><td>Rp 8.000.000,-</td></tr>
                    <tr><td>Puskesmas Rawat Inap Terpencil</td><td>Rp 10.500.000,-</td></tr>
                    <tr><td>Puskesmas Sangat Terpencil / Offline-First</td><td>Rp 12.500.000,-</td></tr>
                    <tr><td>Rumah Sakit Pratama</td><td>Rp 6.500.000,-</td></tr>
                    <tr><td>Rumah Sakit Kelas D</td><td>Rp 10.000.000,-</td></tr>
                    <tr><td>Rumah Sakit Kelas C</td><td>Rp 20.000.000,-</td></tr>
                    <tr><td>Rumah Sakit Kelas B</td><td>Rp 35.000.000,-</td></tr>
                    <tr><td>Rumah Sakit Kelas A</td><td>Rp 70.000.000,-</td></tr>
                </tbody>
            </table>
            <div class="clause-content">
                Perangkat keras, server lokal, perjalanan tenaga pendamping ke lokasi (onsite), integrasi alat medis, penyimpanan citra medis (PACS), migrasi data berskala besar, dan pengembangan fitur khusus tidak termasuk dalam biaya langganan dan ditagihkan secara terpisah.
            </div>

            <div class="clause-content fw-bold mt-4">4.4. Opsi C &mdash; Fee Layanan Sistem per Transaksi</div>
            <div class="clause-content">
                PIHAK PERTAMA dibebaskan dari biaya di muka. Sebagai kompensasi, PIHAK KEDUA memperoleh <strong>Fee Layanan Sistem</strong> yang dihitung berdasarkan transaksi yang berhasil diproses melalui Sistem, dengan ketentuan sebagai berikut:
            </div>
            <div class="clause-content fw-bold mt-2">a. Untuk PIHAK PERTAMA berklasifikasi Klinik:</div>
            <ul class="list-dash">
                <li>Fee Tindakan Medis sebesar <strong>0,5% (nol koma lima persen)</strong> dari pendapatan bersih tindakan (setelah dikurangi jasa profesional dokter, bahan medis habis pakai, diskon, dan pembatalan/refund); dan</li>
                <li>Fee Obat/Farmasi sebesar <strong>0,25% (nol koma dua puluh lima persen)</strong> dari penjualan bersih obat (setelah dikurangi diskon, retur, dan pajak); dengan</li>
                <li>Tagihan minimum <strong>Rp 2.500.000,-</strong> dan maksimum <strong>Rp 7.500.000,-</strong> per bulan per lokasi Klinik; atau</li>
                <li>Sebagai alternatif yang lebih sederhana, PARA PIHAK dapat sepakat menggunakan tarif tetap <strong>Rp 500,-</strong> per registrasi kunjungan berhasil, dengan batas minimum dan maksimum yang sama.</li>
            </ul>
            <div class="clause-content fw-bold mt-2">b. Untuk PIHAK PERTAMA berklasifikasi Puskesmas atau Rumah Sakit, dihitung berdasarkan fee per registrasi kunjungan berhasil sebagai berikut:</div>
            <table class="enhanced-table">
                <thead>
                    <tr>
                        <th><%= Common.getBahasaConfig("Klasifikasi Fasilitas") %></th>
                        <th><%= Common.getBahasaConfig("Tagihan Minimum") %></th>
                        <th><%= Common.getBahasaConfig("Fee per Registrasi") %></th>
                        <th><%= Common.getBahasaConfig("Tagihan Maksimum") %></th>
                    </tr>
                </thead>
                <tbody>
                    <tr><td>Puskesmas Nonrawat Inap</td><td>Rp 4.000.000,-</td><td>Rp 500,-</td><td>Rp 10.000.000,-</td></tr>
                    <tr><td>Puskesmas Nonrawat Inap Lengkap</td><td>Rp 5.500.000,-</td><td>Rp 750,-</td><td>Rp 15.000.000,-</td></tr>
                    <tr><td>Puskesmas Rawat Inap</td><td>Rp 8.000.000,-</td><td>Rp 1.000,-</td><td>Rp 20.000.000,-</td></tr>
                    <tr><td>Puskesmas Terpencil</td><td>Rp 10.500.000,-</td><td>Rp 1.250,-</td><td>Rp 25.000.000,-</td></tr>
                    <tr><td>Puskesmas Sangat Terpencil</td><td>Rp 12.500.000,-</td><td>Rp 1.500,-</td><td>Rp 30.000.000,-</td></tr>
                    <tr><td>Rumah Sakit Pratama</td><td>Rp 6.500.000,-</td><td>Rp 1.000,-</td><td>Rp 20.000.000,-</td></tr>
                    <tr><td>Rumah Sakit Kelas D</td><td>Rp 10.000.000,-</td><td>Rp 1.250,-</td><td>Rp 30.000.000,-</td></tr>
                    <tr><td>Rumah Sakit Kelas C</td><td>Rp 20.000.000,-</td><td>Rp 2.500,- (atau skema gabungan)</td><td>Rp 60.000.000,-</td></tr>
                    <tr><td>Rumah Sakit Kelas B</td><td colspan="2">Biaya dasar Rp 20.000.000,- + Rp 2.000,-/registrasi</td><td>Rp 100.000.000,-</td></tr>
                    <tr><td>Rumah Sakit Kelas A</td><td colspan="2">Biaya dasar Rp 40.000.000,- + Rp 3.500,-/registrasi</td><td>Rp 200.000.000,-</td></tr>
                </tbody>
            </table>
            <div class="clause-content fw-bold mt-2">c. Definisi Transaksi Berbayar</div>
            <div class="clause-content">
                Yang dimaksud dengan satu transaksi/registrasi berbayar adalah <strong>satu registrasi kunjungan pasien yang berhasil, memiliki nomor kunjungan, dan sudah mendapatkan pelayanan</strong>. Tidak termasuk sebagai transaksi berbayar: pendaftaran yang dibatalkan, percobaan/uji coba, duplikasi pendaftaran, koreksi administrasi, aktivitas membuka aplikasi tanpa mendaftar, pembatalan oleh petugas, data pelatihan/simulasi, dan sinkronisasi ulang data lama. Khusus rawat inap, hanya dihitung satu kali pada saat admisi, tidak dihitung berulang untuk setiap hari perawatan.
            </div>
            <div class="legal-note">
                <strong>Prinsip Kepatuhan &amp; Etika Skema Fee Layanan Sistem:</strong>
                <ol class="list-number mt-2 mb-0">
                    <li>Fee Layanan Sistem merupakan hubungan bisnis (business-to-business) antara PIHAK PERTAMA dan PIHAK KEDUA, dibayarkan dari anggaran operasional dan/atau pendapatan PIHAK PERTAMA sesuai Perjanjian ini, dan <strong>bukan merupakan pungutan tambahan yang secara otomatis dibebankan kepada pasien</strong>, termasuk kepada peserta Jaminan Kesehatan Nasional (JKN/BPJS Kesehatan).</li>
                    <li>Untuk pasien peserta JKN/BPJS, PIHAK PERTAMA menegaskan bahwa pembayaran Fee Layanan Sistem tidak akan menyimpang dari ketentuan standar tarif pelayanan program Jaminan Kesehatan Nasional yang berlaku.</li>
                    <li>Untuk pasien umum, apabila komponen biaya administrasi sistem dimasukkan sebagai bagian dari tarif resmi PIHAK PERTAMA, maka hal tersebut harus dicantumkan secara transparan, disetujui oleh manajemen PIHAK PERTAMA, dan tidak bertentangan dengan ketentuan tarif yang berlaku di wilayah PIHAK PERTAMA.</li>
                    <li>Besaran Fee Layanan Sistem <strong>tidak boleh</strong> menjadi dasar bagi dokter, tenaga kesehatan, maupun tenaga kefarmasian untuk menambah tindakan, pemeriksaan, resep, maupun jumlah obat di luar indikasi medis. Informasi mengenai fee hanya dapat diakses oleh manajemen dan bagian keuangan PIHAK PERTAMA, dan tidak ditampilkan kepada tenaga medis sebagai insentif tindakan.</li>
                </ol>
            </div>

            <div class="clause-content fw-bold mt-4">4.5. Ketentuan Umum Ketiga Opsi</div>
            <div class="clause-content">
                Klasifikasi fasilitas PIHAK PERTAMA (Klinik/Puskesmas/Rumah Sakit beserta kelas atau tingkatannya) ditetapkan berdasarkan kesepakatan PARA PIHAK dengan mengacu pada ketentuan klasifikasi dan perizinan fasilitas kesehatan yang berlaku. Perubahan klasifikasi fasilitas PIHAK PERTAMA di kemudian hari (misalnya peningkatan kelas Rumah Sakit) dapat menjadi dasar peninjauan kembali besaran biaya melalui Adendum sebagaimana diatur pada Pasal 11.
            </div>

            <div class="clause-content fw-bold mt-4">4.6. Keterbukaan Negosiasi Kemitraan</div>
            <div class="clause-content">
                PARA PIHAK saling menyadari dan memahami bahwa rincian harga, tarif langganan, maupun besaran Fee Layanan Sistem yang tercantum di dalam draf maupun proposal penawaran merupakan <strong>nilai penawaran standar yang sangat terbuka untuk dinegosiasikan</strong>. PIHAK KEDUA bersedia dan berkomitmen untuk merumuskan penyesuaian kesepakatan nilai akhir, agar Sistem ini dapat segera diimplementasikan secara optimal dan secepatnya memberikan kemanfaatan digitalisasi yang nyata bagi PIHAK PERTAMA. PARA PIHAK sepakat bahwa <strong>kecepatan realisasi implementasi Sistem menjadi prioritas utama</strong> dibandingkan kekakuan pada satu angka tertentu.
            </div>

            <div class="clause-content mt-3">
                Segala pajak yang timbul akibat pelaksanaan Perjanjian ini akan ditanggung oleh masing-masing pihak sesuai dengan regulasi perpajakan Republik Indonesia.
            </div>

            <div class="page-break"></div>

            <div class="article-title">PASAL 5<br>PELATIHAN (TRAINING) DAN PENDAMPINGAN</div>
            <ol class="list-number">
                <li>PIHAK KEDUA memberikan fasilitas Pelatihan Penggunaan Sistem (<i>Training</i>) bagi dokter, perawat, farmasis, dan kasir secara <strong>Daring (Online)</strong> tanpa dipungut biaya (100% Gratis).</li>
                <li>Apabila PIHAK PERTAMA menghendaki Pelatihan/Sosialisasi secara <strong>Tatap Muka (Offline/On-site)</strong>, jasa instruktur dari PIHAK KEDUA tetap tidak dikenakan biaya. PIHAK PERTAMA hanya berkewajiban menanggung seluruh biaya transportasi dan akomodasi bagi tim implementator PIHAK KEDUA selama bertugas di lokasi.</li>
            </ol>

            <div class="clause-content fw-bold mt-4">5.1. Tahapan Implementasi yang Direkomendasikan</div>
            <ul class="timeline-list">
                <li data-step="1"><strong>Kick-off &amp; Penyelarasan Kebutuhan:</strong> penyamaan ruang lingkup, pembentukan tim pelaksana, penentuan prioritas modul klinis, serta penyusunan rencana kerja awal.</li>
                <li data-step="2"><strong>Konfigurasi &amp; Migrasi Awal:</strong> setup lingkungan sistem, master pasien/dokter/item obat/tarif, hak akses, dan data awal sesuai format yang disepakati.</li>
                <li data-step="3"><strong>Pelatihan &amp; Uji Coba Terbatas:</strong> pelatihan dokter, perawat, farmasis, kasir; simulasi alur pendaftaran-pemeriksaan-farmasi-kasir; validasi oleh unit terkait.</li>
                <li data-step="4"><strong>Go-Live Bertahap:</strong> aktivasi modul prioritas (mis. pendaftaran &amp; rekam medis dahulu), monitoring pemakaian, pendampingan penyelesaian kendala.</li>
                <li data-step="5"><strong>Evaluasi &amp; Optimalisasi:</strong> evaluasi pemanfaatan Sistem, penyempurnaan konfigurasi, serta rekomendasi pengembangan lanjutan (termasuk roadmap BPJS/SATUSEHAT).</li>
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
                        <td>Sistem utama tidak dapat diakses secara luas atau layanan registrasi/farmasi/kasir inti berhenti berjalan.</td>
                        <td>Diprioritaskan untuk investigasi dan pemulihan layanan secepat mungkin.</td>
                    </tr>
                    <tr>
                        <td><strong>Tinggi</strong></td>
                        <td>Fungsi penting terganggu namun masih tersedia alternatif proses sementara.</td>
                        <td>Ditangani secara prioritas berdasarkan dampak operasional klinis dan jadwal layanan.</td>
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
                <li><strong>Kepemilikan Data:</strong> Segala bentuk data rekam medis, farmasi, dan finansial yang diunggah ke dalam Sistem adalah milik sah PIHAK PERTAMA. PIHAK KEDUA berkewajiban menjaga kerahasiaan penuh (termasuk kerahasiaan medis pasien) dan tidak berhak mengeksploitasinya untuk kepentingan komersial lain.</li>
                <li><strong>Hak Kekayaan Intelektual (HKI):</strong> Segala hak cipta atas desain perangkat lunak, kode program (<i>source code</i>), sistem basis data, dan arsitektur algoritma modul eMedic mutlak merupakan properti intelektual milik PIHAK KEDUA. PIHAK PERTAMA hanya memegang lisensi hak guna selama masa Perjanjian.</li>
            </ol>

            <div class="clause-content fw-bold mt-3">7.1. Kedudukan PARA PIHAK dalam Pemrosesan Data Pribadi Kesehatan</div>
            <div class="clause-content">
                Mengingat data kesehatan/rekam medis pasien tergolong sebagai data pribadi yang bersifat spesifik sebagaimana diatur dalam peraturan perundang-undangan mengenai pelindungan data pribadi, serta merujuk pada ketentuan mengenai rekam medis yang berlaku, PARA PIHAK sepakat menetapkan kedudukan hukum sebagai berikut:
            </div>
            <ul class="list-dash">
                <li><strong>PIHAK PERTAMA</strong> berkedudukan sebagai <i>pengendali data</i> (data controller) &mdash; pihak yang menentukan tujuan dan cara pemrosesan data pribadi pasien.</li>
                <li><strong>PIHAK KEDUA</strong> berkedudukan sebagai <i>pemroses data</i> (data processor) &mdash; pihak yang memproses data pribadi pasien semata-mata untuk kepentingan pelaksanaan Perjanjian ini, berdasarkan instruksi dan sepengetahuan PIHAK PERTAMA.</li>
            </ul>

            <div class="clause-content fw-bold mt-3">7.2. Kewajiban Minimal Perlindungan Data</div>
            <div class="clause-content">
                Sebagai pemroses data, PIHAK KEDUA berkewajiban menyediakan dan/atau menjalankan, sekurang-kurangnya:
            </div>
            <ul class="list-dash">
                <li>Pembatasan penggunaan data hanya untuk kepentingan pelaksanaan Perjanjian ini;</li>
                <li>Larangan menjual, menyewakan, atau memanfaatkan data pasien PIHAK PERTAMA untuk kepentingan pihak lain dalam bentuk apa pun;</li>
                <li>Hak akses berbasis peran (<i>role-based access control</i>) sesuai kewenangan masing-masing pengguna (dokter, perawat, farmasis, kasir, admin);</li>
                <li>Enkripsi data serta pencadangan (backup) data secara berkala;</li>
                <li>Jejak audit (<i>audit trail</i>) atas setiap akses dan perubahan data, termasuk pencatatan akses terhadap rekam medis;</li>
                <li>Prosedur penanganan dan pemberitahuan kepada PIHAK PERTAMA apabila terjadi insiden kebocoran atau penyalahgunaan data;</li>
                <li>Penghapusan atau pengembalian seluruh data kepada PIHAK PERTAMA pada saat Perjanjian berakhir, sebagaimana diatur pada Pasal 8; dan</li>
                <li>Jaminan bahwa seluruh data yang diproses tetap menjadi milik PIHAK PERTAMA sepenuhnya sebagaimana ditegaskan pada ayat (1) Pasal ini.</li>
            </ul>

            <div class="highlight-box">
                <strong>Prinsip Perlindungan Data Kesehatan:</strong> Setiap akses, pemrosesan, penggunaan, pencadangan, dan pemindahan data rekam medis dilakukan secara terbatas untuk kepentingan pelaksanaan Perjanjian ini. PIHAK KEDUA tidak diperkenankan menjual, mengalihkan, atau menggunakan data pasien PIHAK PERTAMA untuk kepentingan lain tanpa persetujuan tertulis dari PIHAK PERTAMA, kecuali diwajibkan oleh ketentuan peraturan perundang-undangan yang berlaku (termasuk regulasi kerahasiaan rekam medis dan perlindungan data pribadi kesehatan).
            </div>

            <div class="article-title">PASAL 8<br>MASA BERLAKU & PENGAKHIRAN PERJANJIAN</div>
            <ol class="list-number">
                <li>Perjanjian Kerja Sama ini berlaku terhitung sejak ditandatangani untuk jangka waktu <strong><%=masaKontrak%></strong> dan mengikat sepenuhnya bagi PARA PIHAK.</li>
                <li>Apabila Perjanjian ini berakhir atau diputus, PIHAK KEDUA wajib menyerahkan (<i>ekspor/dump</i>) seluruh basis data (termasuk data rekam medis) milik PIHAK PERTAMA dalam format standar (SQL/CSV/Excel) sebelum akses Sistem ditutup secara permanen.</li>
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
                Dengan ditandatanganinya Perjanjian ini, PARA PIHAK menyatakan telah membaca, memahami, dan menyetujui seluruh ketentuan yang termuat di dalam dokumen ini, serta berkomitmen untuk melaksanakan kerja sama secara profesional demi terwujudnya layanan kesehatan digital yang lebih modern, efektif, transparan, dan berkelanjutan.
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
                <span>Dokumen: Draf PKS Modul eMedic (Kesehatan)</span>
                <span><%=namaClient%> &mdash; <%=judul%></span>
            </div>

        </div>
    </div>

    <script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
    <jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp" />
</body>
</html>
