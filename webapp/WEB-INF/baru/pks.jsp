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
String namaClient = (paramClient != null && !paramClient.trim().isEmpty()) ? paramClient : "[NAMA INSTITUSI/YAYASAN KLIEN]";
String namaPenyedia = (paramPresentBy != null && !paramPresentBy.trim().isEmpty()) ? paramPresentBy : "[NAMA PENYEDIA SISTEM / KONSULTAN]";
String kontakPenyedia = (paramTelp != null && !paramTelp.trim().isEmpty()) ? paramTelp : Telepon;

// Tanggal Hari Ini untuk format PKS
Calendar cal = Calendar.getInstance();
String hari = new SimpleDateFormat("EEEE", new Locale("id", "ID")).format(cal.getTime());
String tanggalStr = new SimpleDateFormat("dd", new Locale("id", "ID")).format(cal.getTime());
String bulan = new SimpleDateFormat("MMMM", new Locale("id", "ID")).format(cal.getTime());
String tahunStr = new SimpleDateFormat("yyyy", new Locale("id", "ID")).format(cal.getTime());

// Parameter opsional untuk kebutuhan finalisasi dokumen PKS
String nomorPks = (paramNomorPks != null && !paramNomorPks.trim().isEmpty()) ? paramNomorPks : "...... / PKS / EE / " + tahunStr;
String kotaPks = (paramKotaPks != null && !paramKotaPks.trim().isEmpty()) ? paramKotaPks : ".......................................";
String lokasiPks = (paramLokasiPks != null && !paramLokasiPks.trim().isEmpty()) ? paramLokasiPks : ".......................................";
String namaWakilClient = (paramNamaWakilClient != null && !paramNamaWakilClient.trim().isEmpty()) ? paramNamaWakilClient : ".........................................................";
String jabatanClient = (paramJabatanClient != null && !paramJabatanClient.trim().isEmpty()) ? paramJabatanClient : "[JABATAN PIMPINAN]";
String alamatClient = (paramAlamatClient != null && !paramAlamatClient.trim().isEmpty()) ? paramAlamatClient : "[ALAMAT INSTITUSI]";
String masaKontrak = (paramMasaKontrak != null && !paramMasaKontrak.trim().isEmpty()) ? paramMasaKontrak : "...... (............) tahun";
%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= Common.getBahasaConfig("Draf PKS") %> | <%=judul%></title>
    
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
            content: "ENTERPRISE EDUCATION";
            position: absolute;
            top: 44%;
            left: 50%;
            transform: translate(-50%, -50%) rotate(-28deg);
            font-family: Arial, sans-serif;
            font-weight: 900;
            font-size: 4.3rem;
            letter-spacing: 0.35rem;
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
        <span class="toolbar-hint"><i class="fas fa-file-contract me-1"></i> Draf PKS siap cetak</span>
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
                <div class="doc-subtitle">PENGADAAN DAN IMPLEMENTASI SISTEM INFORMASI TERPADU<br>(ENTERPRISE EDUCATION)</div>
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
                <div class="summary-headline">Landasan kerja sama untuk membangun ekosistem pendidikan digital yang terintegrasi, terukur, aman, dan berkelanjutan.</div>
                <div class="clause-content mb-2">
                    Dokumen ini disusun sebagai draf Perjanjian Kerja Sama yang memuat ruang lingkup implementasi <strong>Enterprise Education</strong>, hak dan kewajiban PARA PIHAK, skema pembiayaan, dukungan layanan, perlindungan data, serta mekanisme pelaksanaan kerja sama. Penyusunan format dibuat lebih terstruktur agar mudah dipahami oleh pimpinan, tim hukum, tim teknologi informasi, keuangan, dan unit pelaksana di lingkungan institusi.
                </div>
                <div class="summary-card-grid">
                    <div class="summary-card">
                        <i class="fas fa-diagram-project"></i>
                        <strong>Terintegrasi End-to-End</strong>
                        <span>Menghubungkan akademik, pembayaran, SDM, aset, persuratan, pelaporan nasional, dan portal pengguna dalam satu ekosistem.</span>
                    </div>
                    <div class="summary-card">
                        <i class="fas fa-chart-line"></i>
                        <strong>Bernilai Strategis</strong>
                        <span>Mendorong efisiensi operasional, transparansi data, percepatan layanan, dan dashboard pengambilan keputusan pimpinan.</span>
                    </div>
                    <div class="summary-card">
                        <i class="fas fa-lock"></i>
                        <strong>Aman & Berkelanjutan</strong>
                        <span>Menegaskan kepemilikan data, kerahasiaan informasi, jaminan layanan, pelatihan, serta pendampingan implementasi.</span>
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
                <li>Bahwa PIHAK PERTAMA adalah sebuah institusi pendidikan yang membutuhkan sistem informasi manajemen akademik dan operasional (ERP) yang komprehensif, modern, dan terintegrasi.</li>
                <li>Bahwa PIHAK KEDUA adalah badan/pihak penyedia layanan teknologi informasi yang memiliki kompetensi, hak cipta, dan kewenangan atas perangkat lunak Sistem Informasi Terpadu (Enterprise Education).</li>
                <li>Bahwa PARA PIHAK memandang digitalisasi tata kelola pendidikan sebagai kebutuhan strategis untuk meningkatkan kualitas layanan, transparansi pengelolaan, akurasi data, serta efektivitas pengambilan keputusan berbasis informasi.</li>
                <li>Bahwa kerja sama ini tidak hanya berorientasi pada pengadaan aplikasi, tetapi juga mencakup pendampingan perubahan proses kerja, penguatan kapasitas pengguna, serta penyelarasan sistem dengan kebutuhan operasional PIHAK PERTAMA.</li>
                <li>Bahwa PARA PIHAK berkomitmen menjalankan kerja sama secara profesional, akuntabel, saling menguntungkan, serta berlandaskan prinsip kerahasiaan data dan keberlanjutan layanan.</li>
            </ul>

            <div class="clause-content">
                Berdasarkan hal-hal tersebut di atas, PARA PIHAK sepakat untuk mengikatkan diri dalam Perjanjian Kerja Sama Implementasi Layanan Enterprise Education dengan ketentuan dan syarat-syarat yang diatur dalam pasal-pasal berikut:
            </div>

            <div class="legal-note">
                <strong>Catatan Penyelarasan:</strong> Draf PKS ini dapat disesuaikan kembali berdasarkan hasil pembahasan teknis, legal, keuangan, dan kebutuhan operasional PIHAK PERTAMA sebelum ditandatangani. Penyesuaian tersebut dapat dituangkan dalam bentuk finalisasi naskah, berita acara, lampiran teknis, atau adendum sesuai kesepakatan PARA PIHAK.
            </div>

            <div class="article-title">PASAL 1<br>DEFINISI</div>
            <div class="clause-content">
                Kecuali ditentukan lain dalam konteks kalimatnya, istilah-istilah di bawah ini memiliki pengertian sebagai berikut:
                <ol class="list-number mt-2">
                    <li><strong>Sistem / Perangkat Lunak</strong> adalah aplikasi <i>Enterprise Education</i> berbasis web dan seluler yang dikembangkan oleh PIHAK KEDUA (mencakup platform eCampus, eSchool, dan/atau ePesantren).</li>
                    <li><strong>Perangkat Keras (Hardware)</strong> adalah unit fisik Anjungan Transaksi Mandiri (Kiosk) yang pengadaannya bersifat opsional dan terpisah dari layanan lisensi perangkat lunak.</li>
                    <li><strong>Implementasi</strong> adalah serangkaian kegiatan yang mencakup instalasi, konfigurasi, migrasi data, serta pelatihan penggunaan (<i>training</i>) Sistem kepada sumber daya manusia milik PIHAK PERTAMA.</li>
                    <li><strong>Pengguna Aktif</strong> adalah akun pengguna yang terdaftar dan/atau menggunakan layanan Sistem dalam periode berjalan, termasuk namun tidak terbatas pada siswa/mahasiswa, guru/dosen, pegawai, orang tua/wali, pimpinan, operator, dan administrator sesuai kebutuhan PIHAK PERTAMA.</li>
                    <li><strong>Dashboard Eksekutif</strong> adalah halaman ringkasan informasi strategis yang menampilkan indikator akademik, keuangan, SDM, aset, layanan, dan operasional sebagai bahan monitoring dan pengambilan keputusan pimpinan.</li>
                    <li><strong>Lampiran Teknis</strong> adalah dokumen pendukung yang memuat rincian modul, konfigurasi, tahapan implementasi, kebutuhan data, jadwal pelaksanaan, atau kesepakatan teknis lain yang menjadi satu kesatuan dengan Perjanjian ini.</li>
                </ol>
            </div>

            <div class="article-title">PASAL 2<br>RUANG LINGKUP PEKERJAAN & FITUR SISTEM</div>
            <div class="clause-content">
                PIHAK KEDUA sepakat untuk menyediakan, menginstalasi, dan memelihara Sistem Informasi Terpadu bagi PIHAK PERTAMA. Ruang lingkup fungsionalitas Sistem yang disediakan secara komprehensif meliputi:
            </div>

            <div class="highlight-box">
                <strong>Orientasi Solusi:</strong> Enterprise Education dirancang sebagai platform tata kelola pendidikan terpadu yang membantu institusi menata proses kerja, mengurangi pekerjaan manual berulang, mempercepat layanan kepada pengguna, meningkatkan akurasi data, dan menghadirkan laporan manajemen yang lebih siap digunakan oleh pimpinan.
            </div>

            <div class="value-grid">
                <div class="value-card">
                    <i class="fas fa-database"></i>
                    <strong>Satu Data Institusi</strong>
                    <span>Data akademik, keuangan, SDM, aset, dan layanan dapat dikelola lebih konsisten dalam satu basis data terpadu.</span>
                </div>
                <div class="value-card">
                    <i class="fas fa-gauge-high"></i>
                    <strong>Proses Lebih Cepat</strong>
                    <span>Alur pendaftaran, pembayaran, persetujuan, pelaporan, dan layanan internal dapat dipercepat melalui automasi.</span>
                </div>
                <div class="value-card">
                    <i class="fas fa-users-gear"></i>
                    <strong>Kolaborasi Unit</strong>
                    <span>Setiap unit kerja memiliki portal dan hak akses yang sesuai, sehingga koordinasi lebih tertata dan terdokumentasi.</span>
                </div>
            </div>
            
            <div class="clause-content fw-bold mt-3">2.1. Arsitektur & Antarmuka Pengguna (UI)</div>
            <div class="clause-content">
                Sistem dirancang berbasis Java (Spring Boot) dan PostgreSQL, serta menyediakan dua tipe antarmuka (User Interface) yang dapat diakses pengguna, yaitu:
            </div>
            <ul class="list-dash">
                <li><strong>Tipe Klasik:</strong> Menggunakan <i>ZK Framework</i> yang sangat tangguh, stabil, dan responsif untuk diakses melalui perangkat Mobile maupun Desktop.</li>
                <li><strong>Tipe Modern:</strong> Menggunakan <i>Bootstrap, CSS, Font Awesome</i>, dan <i>tools</i> UI/UX terbaru yang mendukung visualisasi grafik dinamis dan fungsi ekspor data <i>real-time</i> (seperti Javascript export to Excel).</li>
            </ul>

            <div class="clause-content fw-bold mt-3">2.2. Multi-Portal Pengguna & Keamanan</div>
            <ul class="list-dash">
                <li><strong>Akses & Keamanan:</strong> Dukungan <i>Single Sign-On (SSO)</i>, enkripsi basis data, <i>Audit Trail</i> (pelacakan histori log pengguna), dan persetujuan eksekusi berlapis (<i>multi-level approval</i>).</li>
                <li><strong>Multi-Portal:</strong> Penyediaan portal spesifik/terpisah untuk Yayasan/Pimpinan (<i>Executive Dashboard</i>), Dosen/Guru, Mahasiswa/Siswa, dan Orang Tua/Wali.</li>
            </ul>

            <div class="clause-content fw-bold mt-3">2.3. Modul Tata Kelola Akademik & Pembelajaran</div>
            <ul class="list-dash">
                <li><strong>PMB / PPDB Online:</strong> Automasi pendaftaran, seleksi Ujian CBT, hingga publikasi kelulusan.</li>
                <li><strong>Jadwal & KRS/KHS:</strong> Algoritma penjadwalan cerdas anti-bentrok (<i>clash-free</i>), pengisian KRS mandiri, dan pencetakan KHS/Transkrip Nilai.</li>
                <li><strong>LMS (Learning Management System):</strong> Platform E-Learning terpadu, forum diskusi, integrasi <i>Virtual Meeting</i> (Zoom/Google Meet), dan Ujian Online Anti-Curang.</li>
                <li><strong>Kurikulum OBE:</strong> Adaptasi <i>Outcome Based Education (OBE)</i> secara lengkap — perumusan <strong>Profil Lulusan (PL)</strong>, <strong>CPL</strong> berdasarkan 4 kategori SN-Dikti (<i>Sikap, Pengetahuan, Keterampilan Umum, Keterampilan Khusus</i>), <strong>CPMK</strong> per RPS dengan bobot dan minimal ketercapaian, serta <strong>Sub-CPMK</strong> sebagai indikator penilaian terkecil. Pemantauan melalui 3 loop CQI: <strong>Loop 1</strong> (ketercapaian CPMK per semester), <strong>Loop 2</strong> (ketercapaian CPL per tahun akademik), <strong>Loop 3</strong> (evaluasi PL tiap 3–5 tahun via Tracer Study). Dasbor <strong>OBE</strong> menampilkan skor kesiapan OBE (Bronze/Silver/Gold/Platinum). Ditambah Kurikulum Ganda (Diknas & Pesantren).</li>
            </ul>

            <div class="clause-content fw-bold mt-3">2.4. Modul Administrasi, Riset, & Pelaporan Nasional</div>
            <ul class="list-dash">
                <li><strong>1-Click Sync (Feeder & EMIS):</strong> Sinkronisasi data akademik otomatis ke PDDIKTI dan EMIS Kemenag via Web Service API.</li>
                <li><strong>Birokrasi & Kemahasiswaan:</strong> Modul Tracer Study, pencatatan prestasi, pendaftaran wisuda <i>paperless</i>, manajemen magang (PKL), dan pengajuan cuti/beasiswa.</li>
                <li><strong>Pustaka & Riset:</strong> Perpustakaan Digital (eLibrary/OPAC), Simlitabmas, SPMI (Akreditasi), E-Journal, dan E-Repository karya ilmiah.</li>
            </ul>

            <div class="page-break"></div>

            <div class="clause-content fw-bold mt-3">2.5. Modul Keuangan & Aset Institusi</div>
            <ul class="list-dash">
                <li><strong>Payment Gateway & Auto-Sync:</strong> Penagihan terotomatisasi yang terhubung secara <i>Host-to-Host</i> (Virtual Account, QRIS, E-Wallet, Gerai Ritel). Validasi status "LUNAS" terjadi seketika tanpa verifikasi manual oleh kasir.</li>
                <li><strong>Akuntansi & Anggaran:</strong> Jurnal otomatis buku besar, neraca, laba-rugi, dan manajemen RKAT (Perencanaan Anggaran).</li>
                <li><strong>Aset (e-Procurement):</strong> Inventarisasi <i>barcode/RFID</i>, penyusutan (depresiasi), dan tata kelola pengadaan barang.</li>
            </ul>

            <div class="clause-content fw-bold mt-3">2.6. Modul SDM (HRIS) & Operasional Kampus Pintar</div>
            <ul class="list-dash">
                <li><strong>SDM & Kinerja:</strong> Database kepegawaian, penggajian otomatis (<i>Payroll</i>) dengan slip gaji digital, Beban Kinerja Dosen (BKD), dan penilaian KPI (<i>Key Performance Indicator</i>).</li>
                <li><strong>Presensi Multi-Metode:</strong> Rekapitulasi absensi menggunakan Barcode, Pemindaian Wajah (<i>Face Recognition</i>), Kartu RFID, dan Mobile GPS (<i>Geotagging</i>).</li>
                <li><strong>Notifikasi & Komunikasi:</strong> Fitur <i>WhatsApp Gateway</i> untuk pengingat tagihan SPP dan laporan absensi ke orang tua, serta sistem persuratan digital.</li>
                <li><strong>Antar Jemput Terintegrasi:</strong> Pengelolaan kendaraan, sopir, kenek, rute, jadwal antar jemput, kartu/barcode penjemput, monitor antrian gerbang, soundbox kelas, dan audit serah terima peserta.</li>
                <li><strong>Unit Usaha:</strong> Sistem <i>Point of Sale (POS)</i> koperasi, manajemen Kantin Digital (eKantin) terintegrasi RFID (<i>cashless</i>), dan manajemen les/kursus.</li>
            </ul>


            <!-- ENHANCE_MODUL_ANTAR_JEMPUT_START -->
            <div class="clause-content fw-bold mt-3">2.7. Modul Antar Jemput dan Kontrol Serah Terima</div>
            <div class="clause-content">
                Sebagai bagian dari perluasan ekosistem <strong>Enterprise Education</strong>, Sistem juga dapat mencakup modul
                <strong>Antar Jemput</strong> untuk membantu institusi mengelola layanan kendaraan operasional, penjemputan di gerbang,
                pemanggilan peserta, dan bukti serah terima secara lebih aman, tertib, dan terdokumentasi.
            </div>
            <ul class="list-dash">
                <li><strong>Master Operasional:</strong> pengelolaan kendaraan antar jemput yang terhubung dengan data aset institusi, sopir, kenek maksimal tiga orang, rute, jadwal layanan, dan kartu/barcode penjemput.</li>
                <li><strong>Jadwal & Manifest Peserta:</strong> pengaturan jadwal antar atau jemput berdasarkan rute, kendaraan, petugas, tanggal, jam, tahun ajaran/semester, serta daftar peserta dari siswa, mahasiswa, guru, dosen, maupun pegawai.</li>
                <li><strong>Validasi Gerbang:</strong> satpam atau petugas dapat melakukan tap kartu atau scan barcode; sistem memvalidasi status kartu, masa berlaku, jadwal aktif, dan peserta yang berhak dijemput.</li>
                <li><strong>Monitor Antrian & Soundbox Kelas:</strong> transaksi valid dapat menampilkan nomor antrian di monitor gerbang dan mengirim teks panggilan ke perangkat kelas, misalnya pengumuman bahwa penjemputan ananda sudah datang.</li>
                <li><strong>Audit Trail Keselamatan:</strong> sistem menyimpan waktu scan, petugas, pintu gerbang, status panggilan, log notifikasi, waktu keluar kelas, dan waktu serah terima sebagai bukti operasional.</li>
            </ul>
            <div class="highlight-box">
                <strong>Nilai Keamanan Operasional:</strong> Modul Antar Jemput membantu mengurangi risiko salah penjemputan, memperjelas siapa yang bertugas, memastikan peserta hanya keluar melalui alur validasi, serta memberikan histori yang dapat ditelusuri ketika terjadi komplain atau kebutuhan audit keselamatan.
            </div>
            <!-- ENHANCE_MODUL_ANTAR_JEMPUT_END -->

            <div class="clause-content fw-bold mt-4">2.8. Output Implementasi dan Deliverable Utama</div>
            <div class="deliverable-grid">
                <div class="deliverable-card">
                    <i class="fas fa-server"></i>
                    <strong>Lingkungan Sistem</strong>
                    <span>Instalasi, setup server/cloud atau on-premise, konfigurasi awal, hak akses, dan pengaturan parameter institusi.</span>
                </div>
                <div class="deliverable-card">
                    <i class="fas fa-file-import"></i>
                    <strong>Migrasi Data Awal</strong>
                    <span>Pendampingan penyiapan data master dan impor data awal sesuai format yang disepakati PARA PIHAK.</span>
                </div>
                <div class="deliverable-card">
                    <i class="fas fa-chalkboard-user"></i>
                    <strong>Pelatihan Pengguna</strong>
                    <span>Transfer pengetahuan kepada admin, operator, dan perwakilan unit agar Sistem dapat digunakan secara mandiri.</span>
                </div>
            </div>

            <div class="clause-content fw-bold mt-4">2.9. Indikator Keberhasilan Implementasi</div>
            <table class="enhanced-table">
                <thead>
                    <tr>
                        <th><%= Common.getBahasaConfig("Area") %></th>
                        <th><%= Common.getBahasaConfig("Indikator yang Diharapkan") %></th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>Akademik & Layanan</td>
                        <td>Proses administrasi akademik, pendaftaran, jadwal, nilai, surat, dan layanan pengguna berjalan lebih cepat, tertib, dan terdokumentasi.</td>
                    </tr>
                    <tr>
                        <td>Keuangan & Pembayaran</td>
                        <td>Tagihan, pembayaran, rekonsiliasi, dan laporan keuangan operasional dapat dipantau lebih transparan dan minim verifikasi manual.</td>
                    </tr>
                    <tr>
                        <td>Data & Pelaporan</td>
                        <td>Data tersaji dalam dashboard dan laporan yang lebih siap dipakai untuk monitoring, audit, akreditasi, serta pelaporan eksternal.</td>
                    </tr>
                    <tr>
                        <td>Keamanan Operasional</td>
                        <td>Proses antar jemput, validasi penjemput, monitor antrian, panggilan kelas, dan serah terima peserta memiliki jejak data yang jelas dan mudah diaudit.</td>
                    </tr>
                    <tr>
                        <td>Adopsi Pengguna</td>
                        <td>Admin, operator, pimpinan, tenaga pendidik, peserta didik, dan wali/orang tua dapat menggunakan layanan sesuai peran masing-masing.</td>
                    </tr>
                </tbody>
            </table>

            <div class="article-title">PASAL 3<br>HAK DAN KEWAJIBAN PARA PIHAK</div>
            <div class="clause-content fw-bold">1. Hak dan Kewajiban PIHAK PERTAMA:</div>
            <ul class="list-alpha mb-3">
                <li>Berhak menerima layanan Sistem Informasi Perangkat Lunak sesuai dengan ruang lingkup pada Pasal 2.</li>
                <li>Berhak menerima layanan bimbingan teknis (<i>training</i>) secara daring secara gratis dan dukungan pemeliharaan dari PIHAK KEDUA.</li>
                <li>Berkewajiban menyediakan data master awal yang valid untuk keperluan migrasi.</li>
                <li>Berkewajiban melaksanakan kewajiban pembayaran yang telah disepakati pada Pasal 4.</li>
                <li>Berkewajiban menunjuk penanggung jawab proyek dan/atau tim pelaksana internal untuk mendukung koordinasi, validasi kebutuhan, pengumpulan data, serta komunikasi selama implementasi berlangsung.</li>
                <li>Berkewajiban menggunakan Sistem sesuai prosedur, menjaga kerahasiaan akun pengguna, serta memastikan data yang diberikan kepada PIHAK KEDUA adalah data yang sah dan dapat dipertanggungjawabkan.</li>
            </ul>

            <div class="clause-content fw-bold mt-3">2. Hak dan Kewajiban PIHAK KEDUA:</div>
            <ul class="list-alpha mb-3">
                <li>Berhak menerima pembayaran atas penyediaan layanan Sistem (dan perangkat keras, jika ada) dari PIHAK PERTAMA.</li>
                <li>Berkewajiban melakukan instalasi, setup infrastruktur (<i>Cloud/On-Premise</i>), dan kustomisasi Sistem.</li>
                <li>Berkewajiban menjaga kerahasiaan seluruh basis data milik PIHAK PERTAMA dan menanggulangi apabila terdapat kutu (<i>bug</i>) pada Sistem.</li>
                <li>Berkewajiban memberikan arahan teknis, dokumentasi penggunaan, serta pendampingan yang wajar agar proses adopsi Sistem dapat berjalan efektif.</li>
                <li>Berkewajiban melakukan pemeliharaan, penyempurnaan, dan pembaruan Sistem secara proporsional sepanjang masih berada dalam ruang lingkup kerja sama yang disepakati.</li>
            </ul>

            <div class="article-title">PASAL 4<br>SKEMA PEMBIAYAAN DAN PEMBAYARAN</div>
            
            <div class="clause-content fw-bold mt-3">4.1. Biaya Lisensi Sistem / Perangkat Lunak</div>
            <div class="clause-content">
                Terkait dengan pembiayaan implementasi dan lisensi penggunaan perangkat lunak (<i>Software</i>), PARA PIHAK sepakat menggunakan model pembiayaan berikut:
            </div>
            
            <div class="clause-content text-center fst-italic my-3 p-3 border pricing-box">
                [PILIH SALAH SATU DAN HAPUS YANG TIDAK PERLU]<br><br>
                <strong>OPSI A (Berbasis Transaksi / Zero Investment):</strong><br>
                PIHAK PERTAMA dibebaskan dari biaya lisensi perangkat lunak dan biaya server bulanan. Sebagai kompensasi operasional, PIHAK KEDUA akan membebankan biaya administrasi sistem sebesar Rp 7.000,- (Tujuh Ribu Rupiah) per transaksi pembayaran akademik yang ditanggung oleh pembayar (mahasiswa/wali murid). Khusus metode QRIS dikenakan admin 1%. Berlaku untuk minimum 2.000 mahasiswa/siswa aktif.<br><br>
                <strong>-- ATAU --</strong><br><br>
                <strong>OPSI B (Berbasis Pengguna Aktif / Fixed Budget):</strong><br>
                PIHAK PERTAMA menanggung biaya berlangganan Sistem sebesar <strong>Rp 2.000,- /siswa/bulan</strong> (untuk tingkat Sekolah) ATAU <strong>Rp 4.000,- /mahasiswa/bulan</strong> (untuk Perguruan Tinggi). Tagihan dihitung dengan menetapkan jumlah batas minimum (<i>threshold</i>) sebanyak 1.000 pengguna aktif setiap bulannya. Apabila jumlah pengguna aktual di bawah angka tersebut, tagihan tetap dihitung mengikat setara 1.000 pengguna.
            </div>

            <table class="enhanced-table">
                <thead>
                    <tr>
                        <th><%= Common.getBahasaConfig("Skema") %></th>
                        <th><%= Common.getBahasaConfig("Cocok untuk") %></th>
                        <th><%= Common.getBahasaConfig("Keunggulan Utama") %></th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td><strong>Opsi A - Berbasis Transaksi</strong></td>
                        <td>Institusi yang ingin memulai transformasi digital dengan investasi awal perangkat lunak yang sangat ringan.</td>
                        <td>Risiko awal lebih rendah, pembayaran mengikuti aktivitas transaksi, dan lebih mudah diterapkan untuk pengguna aktif berskala besar.</td>
                    </tr>
                    <tr>
                        <td><strong>Opsi B - Berbasis Pengguna Aktif</strong></td>
                        <td>Institusi yang menghendaki biaya bulanan lebih terprediksi dan mudah dimasukkan ke dalam perencanaan anggaran rutin.</td>
                        <td>Kontrol anggaran lebih jelas, sederhana untuk administrasi internal, dan cocok untuk model layanan berkelanjutan.</td>
                    </tr>
                </tbody>
            </table>
            
            <div class="clause-content fw-bold mt-4">4.2. Biaya Pengadaan Perangkat Keras Anjungan (Hardware Kiosk)</div>
            <div class="clause-content">
                Biaya pengadaan perangkat keras fisik mesin <strong>Anjungan Transaksi Mandiri (ATM eCampus)</strong> bersifat terpisah dan <strong>TIDAK TERMASUK</strong> ke dalam skema pembiayaan perangkat lunak (Pasal 4.1). Apabila PIHAK PERTAMA menghendaki pengadaan unit Anjungan, harga disepakati sebesar:
                <ul class="list-dash mt-2">
                    <li><strong>Rp 37.500.000,-</strong> (Tiga Puluh Tujuh Juta Lima Ratus Ribu Rupiah) untuk Tipe <i>Non-Touchscreen</i>.</li>
                    <li><strong>Rp 42.500.000,-</strong> (Empat Puluh Dua Juta Lima Ratus Ribu Rupiah) untuk Tipe <i>Touchscreen (24 Inch)</i>.</li>
                </ul>
                Spesifikasi perangkat keras mencakup Chasing Plat Besi 2mm (Finishing Cat Duco/Stiker), CPU Mini PC Industrial Intel i5 YC-Series, RAM 8GB DDR3, SSD 256GB, Jaringan Ethernet & Wireless 802.11 b/g/n, Bluetooth, Printer Inkjet Epson L120, QR Scanner 2D, Kamera, Keyboard & Mouse. Perangkat ini sudah tertanam (embedded) OS dan Aplikasi ATM Berbasis Java. 
                <br><br>Harga tersebut di luar Pajak dan biaya pengiriman di luar area Jabodetabek. Pembayaran dilakukan dengan skema <i>Down Payment (DP)</i> sebesar Rp 25.000.000,- (Dua Puluh Lima Juta Rupiah) di awal sebelum tahap produksi yang memakan waktu 1 (satu) bulan. Sisa pelunasan dibayarkan saat barang akan dikirim.
            </div>

            <div class="clause-content fw-bold mt-4">4.3. Keterbukaan Negosiasi Kemitraan</div>
            <div class="clause-content">
                PARA PIHAK saling menyadari dan memahami bahwa rincian harga, baik untuk biaya sewa lisensi Perangkat Lunak (Pasal 4.1) maupun pengadaan Perangkat Keras Anjungan (Pasal 4.2), yang tercantum di dalam draf maupun proposal penawaran merupakan <strong>nilai penawaran standar yang sangat terbuka untuk dinegosiasikan</strong>. PIHAK KEDUA bersedia dan berkomitmen untuk merumuskan penyesuaian kesepakatan nilai akhir, agar Sistem ini dapat segera diimplementasikan secara optimal dan secepatnya memberikan kemanfaatan digitalisasi yang nyata bagi instansi PIHAK PERTAMA.
            </div>

            <div class="clause-content mt-3">
                Segala pajak yang timbul akibat pelaksanaan Perjanjian ini akan ditanggung oleh masing-masing pihak sesuai dengan regulasi perpajakan Republik Indonesia.
            </div>

            <div class="page-break"></div>

            <div class="article-title">PASAL 5<br>PELATIHAN (TRAINING) DAN PENDAMPINGAN</div>
            <ol class="list-number">
                <li>PIHAK KEDUA memberikan fasilitas Pelatihan Penggunaan Sistem (<i>Training</i>) secara <strong>Daring (Online)</strong> tanpa dipungut biaya (100% Gratis).</li>
                <li>Apabila PIHAK PERTAMA menghendaki Pelatihan/Sosialisasi secara <strong>Tatap Muka (Offline/On-site)</strong>, jasa instruktur dari PIHAK KEDUA tetap tidak dikenakan biaya. PIHAK PERTAMA hanya berkewajiban menanggung seluruh biaya transportasi (tiket perjalanan) dan akomodasi (penginapan) bagi tim implementator PIHAK KEDUA selama bertugas di lokasi PIHAK PERTAMA.</li>
            </ol>

            <div class="clause-content fw-bold mt-4">5.1. Tahapan Implementasi yang Direkomendasikan</div>
            <ul class="timeline-list">
                <li data-step="1"><strong>Kick-off & Penyelarasan Kebutuhan:</strong> penyamaan ruang lingkup, pembentukan tim pelaksana, penentuan prioritas modul, serta penyusunan rencana kerja awal.</li>
                <li data-step="2"><strong>Konfigurasi & Migrasi Awal:</strong> setup lingkungan sistem, parameter institusi, hak akses, data master, dan contoh data transaksi sesuai format yang disepakati.</li>
                <li data-step="3"><strong>Pelatihan & Uji Coba Terbatas:</strong> pelatihan admin/operator, simulasi proses utama, koreksi data, serta validasi alur kerja oleh unit terkait.</li>
                <li data-step="4"><strong>Go-Live Bertahap:</strong> aktivasi modul prioritas, monitoring pemakaian, pendampingan penyelesaian kendala, dan penguatan adopsi pengguna.</li>
                <li data-step="5"><strong>Evaluasi & Optimalisasi:</strong> evaluasi pemanfaatan Sistem, penyempurnaan konfigurasi, serta rekomendasi pengembangan lanjutan.</li>
            </ul>

            <div class="article-title">PASAL 6<br>JAMINAN LAYANAN & PEMELIHARAAN (SLA)</div>
            <ol class="list-number">
                <li>PIHAK KEDUA menjamin bahwa Sistem yang diimplementasikan akan berfungsi dengan baik dan sesuai dengan spesifikasi.</li>
                <li><strong>Jaminan Kelangsungan Layanan:</strong> PIHAK KEDUA memberikan jaminan mutlak bahwa apabila di kemudian hari terjadi pengunduran diri personel, pemutusan kontrak sepihak dari tim teknis, atau perubahan status manajemen internal dari pihak pengembang/PIHAK KEDUA, Sistem akan <strong>tetap dapat digunakan secara normal</strong> oleh PIHAK PERTAMA hingga berakhirnya masa kontrak, tanpa adanya penghentian layanan, pemblokiran, atau penuntutan biaya tambahan dari pihak lain mana pun.</li>
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
                        <td>Sistem utama tidak dapat diakses secara luas atau layanan pembayaran/akademik inti berhenti berjalan.</td>
                        <td>Diprioritaskan untuk investigasi dan pemulihan layanan secepat mungkin.</td>
                    </tr>
                    <tr>
                        <td><strong>Tinggi</strong></td>
                        <td>Fungsi penting terganggu namun masih tersedia alternatif proses sementara.</td>
                        <td>Ditangani secara prioritas berdasarkan dampak operasional dan jadwal layanan.</td>
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
                <li><strong>Kepemilikan Data:</strong> Segala bentuk data akademik, finansial, dan informasi yang diunggah ke dalam Sistem adalah milik sah PIHAK PERTAMA. PIHAK KEDUA berkewajiban menjaga kerahasiaan penuh dan tidak berhak mengeksploitasinya untuk kepentingan komersial lain.</li>
                <li><strong>Hak Kekayaan Intelektual (HKI):</strong> Segala hak cipta atas desain perangkat lunak, kode program (<i>source code</i>), sistem basis data, dan arsitektur algoritma Enterprise Education mutlak merupakan properti intelektual milik PIHAK KEDUA. PIHAK PERTAMA hanya memegang lisensi hak guna selama masa Perjanjian.</li>
            </ol>

            <div class="highlight-box">
                <strong>Prinsip Perlindungan Data:</strong> Setiap akses, pemrosesan, penggunaan, pencadangan, dan pemindahan data dilakukan secara terbatas untuk kepentingan pelaksanaan Perjanjian ini. PIHAK KEDUA tidak diperkenankan menjual, mengalihkan, atau menggunakan data PIHAK PERTAMA untuk kepentingan lain tanpa persetujuan tertulis dari PIHAK PERTAMA, kecuali diwajibkan oleh ketentuan peraturan perundang-undangan yang berlaku.
            </div>

            <div class="article-title">PASAL 8<br>MASA BERLAKU & PENGAKHIRAN PERJANJIAN</div>
            <ol class="list-number">
                <li>Perjanjian Kerja Sama ini berlaku terhitung sejak ditandatangani untuk jangka waktu <strong><%=masaKontrak%></strong> dan mengikat sepenuhnya bagi PARA PIHAK.</li>
                <li>Apabila Perjanjian ini berakhir atau diputus, PIHAK KEDUA wajib menyerahkan (<i>ekspor/dump</i>) seluruh basis data milik PIHAK PERTAMA dalam format standar (SQL/CSV/Excel) sebelum akses Sistem ditutup secara permanen.</li>
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
                Dengan ditandatanganinya Perjanjian ini, PARA PIHAK menyatakan telah membaca, memahami, dan menyetujui seluruh ketentuan yang termuat di dalam dokumen ini, serta berkomitmen untuk melaksanakan kerja sama secara profesional demi terwujudnya layanan pendidikan digital yang lebih modern, efektif, transparan, dan berkelanjutan.
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
                    Pimpinan / Perwakilan Sah
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
                <span>Dokumen: Draf PKS Enterprise Education</span>
                <span><%=namaClient%> &mdash; <%=judul%></span>
            </div>

        </div>
    </div>

    <script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
    <jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp" />
</body>
</html>