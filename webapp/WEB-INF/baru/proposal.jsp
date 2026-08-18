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
String paramNomorProposal = request.getParameter("nomor_proposal");
String paramWebsite = request.getParameter("website");
String tanggalDokumen = new java.text.SimpleDateFormat("dd MMMM yyyy", new java.util.Locale("id", "ID")).format(new java.util.Date());
String nomorProposal = (paramNomorProposal != null && !paramNomorProposal.trim().isEmpty()) ? paramNomorProposal : "EE/PROP/" + Calendar.getInstance().get(Calendar.YEAR);

// Deklarasi Variabel Data Institusi
ais.database.model.sekolah.Sekolah sekolah = ais.action.master.sekolah.util.SekolahUtil.getSekolah(request);
String Telepon = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getTelepon();
String Alamat1 = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getAlamat1();
String Email = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getEmail();

// Override Variabel berdasarkan parameter White-Label
String judul = (paramJudul != null && !paramJudul.trim().isEmpty()) ? paramJudul : ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama();
String logo_PerguruanTinggi = (paramLogo != null && !paramLogo.trim().isEmpty()) ? Common.ROOT + "/img/" + paramLogo : ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= Common.getBahasaConfig("Proposal Enterprise Education") %> | <%=judul%></title>
    
    <link rel="icon" href="<%=logo_PerguruanTinggi%>" type="image/x-icon">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
    
    <style>
        :root {
            --primary-color: #0284c7;
            --primary-dark: #075985;
            --primary-soft: #e0f2fe;
            --accent-color: #7c3aed;
            --success-color: #16a34a;
            --warning-color: #f59e0b;
            --danger-color: #dc2626;
            --secondary-color: #f8fafc;
            --text-dark: #0f172a;
            --text-muted: #475569;
            --border-color: #e2e8f0;
            --paper-gradient: radial-gradient(circle at top left, rgba(2,132,199,0.13), transparent 32%), radial-gradient(circle at top right, rgba(124,58,237,0.10), transparent 30%), #ffffff;
        }
        
        .hover-zoom { transition: transform 0.3s ease, box-shadow 0.3s ease; }
        .hover-zoom:hover { transform: scale(1.05); box-shadow: 0 10px 25px rgba(0,0,0,0.2) !important; position: relative; z-index: 10;}

        body { background: linear-gradient(135deg, #dbeafe 0%, #e2e8f0 42%, #f8fafc 100%); font-family: 'Segoe UI', system-ui, -apple-system, sans-serif; color: var(--text-dark); line-height: 1.8; }
        .proposal-paper { background: var(--paper-gradient); max-width: 1050px; margin: 3rem auto; padding: 4rem 6rem; border-radius: 22px; border: 1px solid rgba(148,163,184,0.35); box-shadow: 0 30px 80px rgba(15,23,42,0.16); position: relative; overflow: hidden; }

        /* Sampul */
        .cover-page { text-align: center; padding: 6rem 0; border-bottom: 3px solid var(--primary-color); margin-bottom: 4rem; position: relative; overflow: hidden; border-radius: 20px; background: linear-gradient(145deg, rgba(224,242,254,0.78), rgba(255,255,255,0.92)); }
        .cover-logo { max-height: 140px; margin-bottom: 2.5rem; filter: drop-shadow(0 4px 6px rgba(0,0,0,0.1)); }
        .cover-title { font-size: 2.95rem; font-weight: 900; color: var(--primary-dark); margin-bottom: 1rem; text-transform: uppercase; letter-spacing: 1px; line-height: 1.18; }
        .cover-subtitle { font-size: 1.55rem; font-weight: 650; color: var(--text-muted); margin-bottom: 1.4rem; }
        .cover-client { margin-top: 5rem; padding: 2.5rem; background: var(--secondary-color); border-radius: 16px; display: inline-block; min-width: 60%; border: 2px dashed #cbd5e1; }

        /* Typografi */
        h2.section-title { font-size: 1.8rem; font-weight: 800; color: var(--primary-color); border-bottom: 3px solid var(--primary-color); padding-bottom: 0.5rem; margin-top: 3.5rem; margin-bottom: 1.5rem; display: inline-block; text-transform: uppercase;}
        h3.subsection-title { font-size: 1.35rem; font-weight: 700; color: var(--text-dark); margin-top: 2.5rem; margin-bottom: 1rem; display: flex; align-items: center; border-left: 5px solid var(--primary-color); padding-left: 12px; background: #f1f5f9; padding-top: 8px; padding-bottom: 8px; border-radius: 0 8px 8px 0;}
        h4.module-title { font-size: 1.15rem; font-weight: 700; color: #1e293b; margin-top: 1.5rem; margin-bottom: 0.5rem; }
        p { font-size: 1.05rem; text-align: justify; margin-bottom: 1rem; }
        .highlight-box { background-color: #f0f9ff; border-left: 5px solid var(--primary-color); padding: 1.5rem; border-radius: 0 16px 16px 0; margin: 2rem 0; box-shadow: 0 10px 25px rgba(2,132,199,0.07); }

        /* Tabel & List */
        ul.custom-list { list-style: none; padding-left: 0; }
        ul.custom-list li { margin-bottom: 0.8rem; position: relative; padding-left: 2rem; font-size: 1.05rem; text-align: justify; }
        ul.custom-list li::before { content: "\f058"; font-family: "Font Awesome 6 Free"; font-weight: 900; position: absolute; left: 0; color: #16a34a; }
        
        ul.dash-list { list-style: none; padding-left: 0; }
        ul.dash-list li { margin-bottom: 0.5rem; position: relative; padding-left: 1.5rem; font-size: 1.05rem; text-align: justify; }
        ul.dash-list li::before { content: "-"; font-weight: 900; position: absolute; left: 0; color: var(--primary-color); font-size: 1.2rem;}
        
        .spec-list { list-style: none; padding-left: 0; }
        .spec-list li { margin-bottom: 0.4rem; padding-left: 1.2rem; position: relative; font-size: 0.95rem;}
        .spec-list li::before { content: "\f0da"; font-family: "Font Awesome 6 Free"; font-weight: 900; position: absolute; left: 0; color: var(--primary-color); }

        /* Pricing Card */
        .pricing-card { border: 1px solid var(--border-color); border-radius: 16px; padding: 2rem; height: 100%; box-shadow: 0 4px 10px rgba(0,0,0,0.04); transition: all 0.3s; background: #fff;}
        .pricing-card:hover { box-shadow: 0 15px 30px rgba(0,0,0,0.08); border-color: var(--primary-color); transform: translateY(-5px);}
        .pricing-header { text-align: center; margin-bottom: 1.5rem; padding-bottom: 1.5rem; border-bottom: 1px dashed var(--border-color); }
        .pricing-price { font-size: 2rem; font-weight: 800; color: var(--primary-color); }

        /* bottom:7.5rem supaya lolos dari tombol "Tanya Jawab" (bantuan_button.jsp: right:16px; bottom:62px). */
        .btn-print { position: fixed; bottom: 7.5rem; right: 2rem; z-index: 1000; border-radius: 50px; padding: 1rem 2rem; box-shadow: 0 10px 20px rgba(2, 132, 199, 0.3); font-size: 1.1rem;}



        /* Enhancement Proposal: siap cetak, modern, dan tetap profesional */
        .proposal-watermark { position: absolute; right: -70px; top: 180px; font-size: 8rem; font-weight: 900; color: rgba(2,132,199,0.035); transform: rotate(-18deg); pointer-events: none; user-select: none; letter-spacing: 2px; }
        .cover-badge { display: inline-flex; align-items: center; gap: 8px; padding: 0.65rem 1.1rem; border-radius: 999px; background: #ffffff; color: var(--primary-dark); border: 1px solid rgba(2,132,199,0.25); box-shadow: 0 8px 24px rgba(2,132,199,0.12); font-weight: 800; text-transform: uppercase; font-size: 0.82rem; letter-spacing: .08em; margin-bottom: 1.4rem; }
        .cover-intro { max-width: 780px; margin: 1.2rem auto 2.2rem auto; font-size: 1.08rem; color: #334155; text-align: center; }
        .document-meta { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin: 2.2rem auto 0 auto; max-width: 820px; }
        .meta-item { background: rgba(255,255,255,0.82); border: 1px solid rgba(148,163,184,0.30); border-radius: 18px; padding: 1rem; text-align: left; box-shadow: 0 10px 25px rgba(15,23,42,0.05); }
        .meta-label { color: var(--text-muted); font-size: .78rem; text-transform: uppercase; letter-spacing: .08em; font-weight: 800; }
        .meta-value { color: var(--text-dark); font-weight: 800; font-size: .98rem; line-height: 1.45; margin-top: .15rem; }
        .executive-panel { background: linear-gradient(135deg, #0f172a, #075985 58%, #0369a1); color: #ffffff; border-radius: 24px; padding: 2.1rem; box-shadow: 0 22px 45px rgba(15,23,42,0.20); margin: 1.6rem 0 2rem; position: relative; overflow: hidden; }
        .executive-panel:after { content: ""; position: absolute; right: -80px; bottom: -100px; width: 280px; height: 280px; background: rgba(255,255,255,0.08); border-radius: 50%; }
        .executive-panel p { text-align: left; color: rgba(255,255,255,0.88); }
        .section-kicker { display: inline-block; padding: .35rem .8rem; border-radius: 999px; background: var(--primary-soft); color: var(--primary-dark); font-size: .78rem; font-weight: 900; letter-spacing: .08em; text-transform: uppercase; margin-bottom: .6rem; }
        .value-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin: 1.5rem 0 2.2rem; }
        .value-card { background: #ffffff; border: 1px solid rgba(226,232,240,0.95); border-radius: 20px; padding: 1.2rem; box-shadow: 0 12px 26px rgba(15,23,42,0.06); height: 100%; }
        .value-icon { width: 46px; height: 46px; border-radius: 14px; display: inline-flex; align-items: center; justify-content: center; background: var(--primary-soft); color: var(--primary-dark); font-size: 1.25rem; margin-bottom: .8rem; }
        .value-title { font-size: 1.02rem; font-weight: 850; margin-bottom: .35rem; color: var(--text-dark); }
        .value-desc { font-size: .9rem; color: var(--text-muted); line-height: 1.6; text-align: left; margin-bottom: 0; }
        .toc-box { border: 1px solid var(--border-color); border-radius: 20px; padding: 1.5rem; background: #ffffff; box-shadow: 0 12px 28px rgba(15,23,42,0.05); }
        .toc-list { columns: 2; padding-left: 1.2rem; margin-bottom: 0; }
        .toc-list li { break-inside: avoid; margin-bottom: .6rem; color: var(--text-muted); font-weight: 600; }
        .impact-row { display: grid; grid-template-columns: 1fr auto 1fr; gap: 16px; align-items: stretch; margin: 1.5rem 0 2.2rem; }
        .impact-card { background: #fff; border: 1px solid var(--border-color); border-radius: 20px; padding: 1.35rem; box-shadow: 0 12px 28px rgba(15,23,42,0.05); }
        .impact-card.before { border-top: 5px solid var(--danger-color); }
        .impact-card.after { border-top: 5px solid var(--success-color); }
        .impact-arrow { display: flex; align-items: center; justify-content: center; color: var(--primary-color); font-size: 2.2rem; }
        .metric-strip { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin: 1.2rem 0 2rem; }
        .metric { padding: 1rem; border-radius: 18px; background: linear-gradient(135deg, #ffffff, #f8fafc); border: 1px solid rgba(226,232,240,.9); text-align: center; box-shadow: 0 10px 22px rgba(15,23,42,0.04); }
        .metric strong { display: block; color: var(--primary-dark); font-size: 1.15rem; }
        .metric span { color: var(--text-muted); font-size: .84rem; font-weight: 700; }
        .timeline { position: relative; margin: 1.5rem 0 2rem; }
        .timeline-step { display: grid; grid-template-columns: 62px 1fr; gap: 16px; margin-bottom: 1rem; align-items: start; }
        .timeline-number { width: 52px; height: 52px; border-radius: 18px; background: linear-gradient(135deg, var(--primary-color), var(--accent-color)); color: white; font-weight: 900; display: flex; align-items: center; justify-content: center; box-shadow: 0 12px 22px rgba(2,132,199,0.22); }
        .timeline-content { border: 1px solid var(--border-color); border-radius: 18px; padding: 1rem 1.2rem; background: #ffffff; box-shadow: 0 8px 18px rgba(15,23,42,0.04); }
        .timeline-content h5 { font-weight: 850; margin-bottom: .35rem; color: var(--text-dark); }
        .timeline-content p { margin-bottom: 0; font-size: .95rem; text-align: left; color: var(--text-muted); }
        .service-scope { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin: 1.5rem 0 2.2rem; }
        .scope-item { border-radius: 20px; padding: 1.25rem; background: #fff; border: 1px solid var(--border-color); box-shadow: 0 8px 20px rgba(15,23,42,0.045); }
        .scope-item h5 { font-weight: 850; font-size: 1rem; color: var(--primary-dark); }
        .signature-line { margin-top: 3.2rem; display: grid; grid-template-columns: 1fr 1fr; gap: 30px; }
        .signature-box { min-height: 150px; border: 1px dashed #cbd5e1; border-radius: 18px; padding: 1rem; background: #fff; }
        .confidential-note { background: #fff7ed; border: 1px solid #fed7aa; color: #9a3412; border-radius: 16px; padding: 1rem 1.2rem; font-size: .92rem; margin-top: 1.2rem; }
        .soft-divider { height: 1px; background: linear-gradient(90deg, transparent, rgba(2,132,199,.35), transparent); margin: 2rem 0; }
        .table-modern { overflow: hidden; border-radius: 18px; border: 1px solid var(--border-color); box-shadow: 0 10px 25px rgba(15,23,42,0.05); }
        .table-modern table { margin-bottom: 0; }
        .table-modern thead th { background: #0f172a; color: #fff; border-color: rgba(255,255,255,.15); }
        @media (max-width: 900px) {
            .proposal-paper { padding: 2rem 1.2rem; margin: 1rem; }
            .document-meta, .value-grid, .metric-strip, .service-scope { grid-template-columns: 1fr; }
            .impact-row { grid-template-columns: 1fr; }
            .impact-arrow { transform: rotate(90deg); }
            .toc-list { columns: 1; }
            .signature-line { grid-template-columns: 1fr; }
        }

        @media print {
            body { background-color: #fff; }
            .proposal-paper { margin: 0; padding: 0; box-shadow: none; max-width: 100%; border: 0; border-radius: 0; background: #fff; }
            .btn-print { display: none; }
            .executive-panel { box-shadow: none; print-color-adjust: exact; -webkit-print-color-adjust: exact; }
            .value-card, .pricing-card, .scope-item, .metric, .impact-card, .toc-box, .timeline-content { box-shadow: none; break-inside: avoid; }
            .document-meta, .value-grid, .metric-strip, .service-scope { break-inside: avoid; }
            a { text-decoration: none; color: inherit; }
            .cover-page { height: 100vh; display: flex; flex-direction: column; justify-content: center; border-bottom: none; }
            .page-break { page-break-before: always; }
            .hover-zoom { transition: none; }
        }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/baru/include/pemilih_bahasa.jsp" />

    <button onclick="window.print()" class="btn btn-primary btn-print fw-bold">
        <i class="fas fa-print me-2"></i> Cetak / Simpan PDF
    </button>

    <div class="container">
        <div class="proposal-paper">
            <div class="proposal-watermark">ENTERPRISE</div>
            
            <div class="cover-page">
                <img src="<%=logo_PerguruanTinggi%>" alt="Logo" class="cover-logo">
                <div class="cover-badge"><i class="fas fa-shield-halved"></i> Dokumen Penawaran Resmi Siap Cetak</div>
                <h1 class="cover-title"><i class="fas fa-file-contract me-3"></i><%= Common.getBahasaConfig("Proposal Penawaran Kerjasama") %></h1>
                <h2 class="cover-subtitle"><%= Common.getBahasaConfig("Implementasi Sistem Informasi Terpadu") %></h2>
                <p class="cover-intro">
                    Solusi <strong>Enterprise Education</strong> dirancang untuk membantu institusi pendidikan membangun ekosistem digital terpadu: lebih cepat dalam layanan, lebih akurat dalam data, lebih transparan dalam keuangan, dan lebih siap menghadapi tuntutan akreditasi serta transformasi pendidikan modern.
                </p>
                <h4 class="fw-bold text-primary mt-4 px-5 py-3 bg-light border border-primary border-opacity-25 rounded-pill d-inline-block shadow-sm">
                    <i class="fas fa-graduation-cap me-2"></i> <%=judul%>
                </h4>
                <div class="document-meta">
                    <div class="meta-item">
                        <div class="meta-label">Nomor Proposal</div>
                        <div class="meta-value"><%=nomorProposal%></div>
                    </div>
                    <div class="meta-item">
                        <div class="meta-label">Tanggal Dokumen</div>
                        <div class="meta-value"><%=tanggalDokumen%></div>
                    </div>
                    <div class="meta-item">
                        <div class="meta-label">Kategori Solusi</div>
                        <div class="meta-value">ERP Pendidikan Terpadu</div>
                    </div>
                </div>
                
                <% if(paramClient != null && !paramClient.trim().isEmpty()) { %>
                    <br>
                    <div class="cover-client shadow-sm">
                        <p class="mb-2 fs-5 fw-semibold text-muted"><%= Common.getBahasaConfig("Disiapkan secara eksklusif untuk:") %></p>
                        <h2 class="fw-bold text-dark mb-0 display-6"><%=paramClient%></h2>
                    </div>
                <% } %>
            </div>

            <div class="page-break"></div>

            <span class="section-kicker">Ringkasan Eksekutif</span>
            <div class="executive-panel">
                <h2 class="fw-bold mb-3"><i class="fas fa-rocket me-2"></i> Proposal Transformasi Digital Pendidikan</h2>
                <p class="mb-3">
                    Proposal ini disusun sebagai dasar kerja sama implementasi <strong>Enterprise Education</strong>, yaitu platform ERP pendidikan yang menyatukan layanan akademik, keuangan, SDM, aset, pembelajaran digital, pelaporan nasional, antar jemput, dan portal pengguna dalam satu sistem yang saling terhubung.
                </p>
                <p class="mb-0">
                    Fokus utama penawaran ini adalah membantu institusi membangun layanan yang lebih <strong>cepat, tertib, akuntabel, mudah diaudit, dan nyaman digunakan</strong> oleh pimpinan, dosen/guru, tenaga kependidikan, mahasiswa/siswa, orang tua, alumni, hingga mitra eksternal.
                </p>
            </div>

            <div class="metric-strip">
                <div class="metric"><strong>Terpadu</strong><span>Akademik, keuangan, SDM, aset</span></div>
                <div class="metric"><strong>Paperless</strong><span>Layanan administrasi digital</span></div>
                <div class="metric"><strong>Real-Time</strong><span>Dashboard dan monitoring pimpinan</span></div>
                <div class="metric"><strong>Scalable</strong><span>Cloud, on-premise, atau hybrid</span></div>
            </div>

            <div class="toc-box mb-5">
                <h5 class="fw-bold text-primary mb-3"><i class="fas fa-list-check me-2"></i> Struktur Proposal</h5>
                <ol class="toc-list">
                    <li>Pendahuluan dan latar belakang kebutuhan transformasi digital.</li>
                    <li>Visi, misi, tujuan, manfaat, dan nilai strategis implementasi.</li>
                    <li>Ekosistem Enterprise Education untuk eCampus, eSchool, dan ePesantren.</li>
                    <li>Rincian modul serta fungsionalitas sistem.</li>
                    <li>Inovasi perangkat keras pendukung layanan mandiri dan POS.</li>
                    <li>Metodologi implementasi dan layanan pendampingan.</li>
                    <li>Skema investasi lisensi perangkat lunak dan perangkat keras opsional.</li>
                </ol>
            </div>

            <h2 class="section-title">I. Pendahuluan & Latar Belakang</h2>
            <p>
                Lembaga pendidikan saat ini menghadapi tantangan yang sangat kompleks dalam mengelola informasi akademik, kemahasiswaan/kesiswaan, dan keuangan secara efektif. Proses operasional yang masih bersifat manual seringkali memakan waktu, rentan terhadap kesalahan (<i>human error</i>), dan menyulitkan akses informasi secara cepat bagi sivitas akademika.
            </p>
            <p>
                Perkembangan teknologi informasi telah membawa perubahan mendasar dalam dunia pendidikan. Institusi dituntut untuk terus beradaptasi agar dapat memberikan pelayanan prima kepada peserta didik, dosen/guru, dan staf, sekaligus meningkatkan daya saing di tingkat nasional maupun global.
            </p>
            <p>
                Oleh karena itu, kami hadir menawarkan <strong>Enterprise Education</strong>, sebuah solusi Sistem Informasi Akademik (SIA) <i>end-to-end</i> terpadu berbasis web. Sistem ini dirancang spesifik untuk mengotomatisasi seluruh kegiatan administratif, mulai dari pendataan mahasiswa, operasional kelas, pelaporan nasional, hingga tata kelola keuangan, dalam satu landasan digital yang kokoh.
            </p>

            <div class="highlight-box">
                <h5 class="fw-bold text-dark"><i class="fas fa-exclamation-triangle text-warning me-2"></i> Identifikasi Masalah & Tantangan Institusi:</h5>
                <ul class="dash-list mt-3 mb-0 text-dark">
                    <li><strong>Birokrasi Konvensional:</strong> Penumpukan dokumen fisik dan proses pelayanan di Tata Usaha yang lambat.</li>
                    <li><strong>Silo Data:</strong> Sistem akademik, keuangan, dan SDM yang berdiri sendiri-sendiri sehingga memicu entri data ganda (<i>double-entry</i>).</li>
                    <li><strong>Beban Pelaporan Berat:</strong> Kompleksitas sinkronisasi data yang menguras waktu operator ke PDDIKTI atau EMIS.</li>
                    <li><strong>Keterbatasan Transparansi:</strong> Orang tua/wali murid kesulitan memantau rincian tagihan dan riwayat akademik anak secara <i>real-time</i>.</li>
                </ul>
            </div>

            <h3 class="subsection-title"><i class="fas fa-arrows-spin"></i> Arah Solusi yang Ditawarkan</h3>
            <div class="impact-row">
                <div class="impact-card before">
                    <h5 class="fw-bold text-danger"><i class="fas fa-triangle-exclamation me-2"></i> Kondisi yang Sering Terjadi</h5>
                    <ul class="dash-list mb-0">
                        <li>Data tersebar di banyak file, aplikasi, dan unit kerja.</li>
                        <li>Validasi transaksi membutuhkan waktu lama dan bergantung pada proses manual.</li>
                        <li>Laporan pimpinan sering terlambat karena rekapitulasi harus dilakukan ulang.</li>
                    </ul>
                </div>
                <div class="impact-arrow"><i class="fas fa-arrow-right-long"></i></div>
                <div class="impact-card after">
                    <h5 class="fw-bold text-success"><i class="fas fa-circle-check me-2"></i> Kondisi Setelah Implementasi</h5>
                    <ul class="dash-list mb-0">
                        <li>Satu basis data terpusat yang dapat diakses sesuai hak akses.</li>
                        <li>Alur akademik, pembayaran, persuratan, dan pelaporan berjalan digital.</li>
                        <li>Dashboard manajemen tersedia untuk pengambilan keputusan berbasis data.</li>
                    </ul>
                </div>
            </div>

            <h2 class="section-title">II. Visi, Misi, Tujuan, dan Manfaat</h2>
            
            <p><strong>Visi Kami:</strong> Menjadi sistem informasi akademik terpadu yang unggul dan terpercaya, mendukung institusi pendidikan dalam mengakselerasi kualitas layanan dan standar mutu.</p>
            
            <p><strong>Misi Kami:</strong></p>
            <ul class="custom-list">
                <li>Menyediakan solusi sistem informasi yang komprehensif, terpusat, dan <i>user-friendly</i>.</li>
                <li>Meningkatkan efisiensi operasional dan transparansi tata kelola akademik.</li>
                <li>Mendukung pimpinan dalam pengambilan keputusan strategis berdasarkan analisis data yang riil.</li>
                <li>Terus berinovasi dan mengintegrasikan teknologi modern sesuai regulasi pendidikan terkini.</li>
            </ul>

            <h3 class="subsection-title"><i class="fas fa-bullseye"></i> Tujuan & Manfaat Implementasi</h3>
            <div class="row mt-3">
                <div class="col-md-6">
                    <ul class="custom-list">
                        <li><strong>Efisiensi Menyeluruh:</strong> Otomatisasi birokrasi, menghemat kertas (<i>paperless</i>), waktu, dan biaya operasional.</li>
                        <li><strong>Akurasi & Validitas:</strong> Meminimalisasi kesalahan manusia dan menjaga integritas basis data pusat.</li>
                        <li><strong>Aksesibilitas Tinggi:</strong> Platform dapat diakses melalui ragam perangkat (komputer & ponsel) kapanpun dibutuhkan.</li>
                        <li><strong>Kepatuhan Regulasi:</strong> Memastikan seluruh standar operasional pendidikan tinggi maupun dasar menengah terpenuhi.</li>
                    </ul>
                </div>
                <div class="col-md-6">
                    <ul class="custom-list">
                        <li><strong>Transparansi Mutlak:</strong> Mempermudah pelacakan audit keuangan dan keterbukaan informasi bagi orang tua.</li>
                        <li><strong>Pengambilan Keputusan:</strong> Tersedianya Dasbor Eksekutif (Executive Dashboard) untuk memantau indikator kinerja institusi.</li>
                        <li><strong>Peningkatan Reputasi:</strong> Mendukung pemenuhan instrumen akreditasi dan berkontribusi langsung pada pemeringkatan Webometrics.</li>
                    </ul>
                </div>
            </div>

            <h3 class="subsection-title"><i class="fas fa-gem"></i> Nilai Strategis untuk Calon Mitra</h3>
            <div class="value-grid">
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-bolt"></i></div>
                    <div class="value-title">Layanan Lebih Cepat</div>
                    <p class="value-desc">Proses pendaftaran, pembayaran, pengajuan surat, presensi, dan pelaporan dapat dipangkas melalui alur digital yang lebih singkat.</p>
                </div>
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-database"></i></div>
                    <div class="value-title">Data Lebih Tertib</div>
                    <p class="value-desc">Setiap unit kerja menggunakan referensi data yang sama sehingga mengurangi duplikasi, konflik data, dan keterlambatan rekap.</p>
                </div>
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-chart-line"></i></div>
                    <div class="value-title">Pimpinan Lebih Siap</div>
                    <p class="value-desc">Informasi operasional tersaji dalam dashboard sehingga evaluasi kinerja, keuangan, akademik, dan layanan dapat dilakukan lebih objektif.</p>
                </div>
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-handshake-angle"></i></div>
                    <div class="value-title">Kemitraan Fleksibel</div>
                    <p class="value-desc">Skema implementasi, infrastruktur, dan pembiayaan dapat disesuaikan dengan kebutuhan, kapasitas, serta prioritas institusi.</p>
                </div>
            </div>

            <div class="highlight-box">
                <h5 class="fw-bold text-dark"><i class="fas fa-award text-primary me-2"></i> Indikator Keberhasilan Implementasi</h5>
                <p class="mb-2">Keberhasilan transformasi tidak hanya diukur dari tersedianya aplikasi, tetapi dari perubahan cara kerja institusi. Beberapa indikator yang dapat dicapai antara lain:</p>
                <ul class="custom-list mb-0">
                    <li>Peningkatan kecepatan layanan administrasi dan berkurangnya antrean loket.</li>
                    <li>Penurunan pekerjaan rekap manual serta pengurangan risiko kesalahan input data.</li>
                    <li>Ketersediaan laporan manajemen yang lebih cepat, konsisten, dan mudah diaudit.</li>
                    <li>Peningkatan kepuasan pengguna karena layanan dapat diakses mandiri secara daring.</li>
                </ul>
            </div>

            <div class="page-break"></div>

            <h2 class="section-title">III. Ekosistem Enterprise Education</h2>
            <p>Sistem ini mengusung konsep <i>All-in-One Platform</i>. Institusi tidak perlu lagi menyewa berbagai aplikasi lunak yang terpisah. Kami membagi fokus layanan ke dalam tiga rumpun besar sesuai dengan jenjang pendidikan:</p>

            <div class="row text-center mt-4 mb-5">
                <div class="col-md-4">
                    <div class="p-4 border rounded-4 bg-light h-100 shadow-sm">
                        <i class="fas fa-university fa-3x text-primary mb-3"></i>
                        <h4 class="fw-bold">eCampus</h4>
                        <p class="small text-muted mb-0">Sistem ERP tingkat lanjut yang mengelola siklus operasional Perguruan Tinggi, Institut, Politeknik, dan Akademi secara penuh.</p>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="p-4 border rounded-4 bg-light h-100 shadow-sm border-bottom border-success border-4">
                        <i class="fas fa-school fa-3x text-success mb-3"></i>
                        <h4 class="fw-bold">eSchool</h4>
                        <p class="small text-muted mb-0">Sistem terpadu yang didesain untuk menyederhanakan administrasi sekolah tingkat TK, SD, SMP, hingga SMA/SMK.</p>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="p-4 border rounded-4 bg-light h-100 shadow-sm border-bottom border-warning border-4">
                        <i class="fas fa-mosque fa-3x text-warning mb-3"></i>
                        <h4 class="fw-bold">ePesantren</h4>
                        <p class="small text-muted mb-0">Platform ERP yang dikustomisasi khusus untuk manajemen pondok pesantren, asrama, dan kurikulum keagamaan.</p>
                    </div>
                </div>
            </div>

            <div class="highlight-box">
                <h5 class="fw-bold text-dark"><i class="fas fa-sitemap text-primary me-2"></i> Prinsip Desain Ekosistem</h5>
                <p class="mb-0">Enterprise Education tidak hanya berperan sebagai aplikasi pencatatan, tetapi sebagai <strong>tulang punggung operasional digital</strong> yang menghubungkan layanan akademik, keuangan, sumber daya manusia, aset, pembelajaran, notifikasi, dan pelaporan strategis dalam satu ekosistem kerja yang konsisten.</p>
            </div>

            <div class="value-grid">
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-user-lock"></i></div>
                    <div class="value-title">Hak Akses Bertingkat</div>
                    <p class="value-desc">Pengguna hanya melihat menu dan data sesuai kewenangan sehingga keamanan informasi tetap terjaga.</p>
                </div>
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-mobile-screen-button"></i></div>
                    <div class="value-title">Akses Multi-Perangkat</div>
                    <p class="value-desc">Antarmuka web responsif mendukung akses dari komputer, tablet, dan ponsel sesuai kebutuhan operasional.</p>
                </div>
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-cloud-arrow-up"></i></div>
                    <div class="value-title">Siap Integrasi</div>
                    <p class="value-desc">Dirancang untuk terhubung dengan layanan pembayaran, notifikasi, kalender, meeting online, dan sistem pelaporan nasional.</p>
                </div>
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-scale-balanced"></i></div>
                    <div class="value-title">Tata Kelola Tertib</div>
                    <p class="value-desc">Mendukung jejak audit, validasi berlapis, dan rekam historis aktivitas untuk kebutuhan pengawasan internal.</p>
                </div>
            </div>

            <h3 class="subsection-title"><i class="fas fa-network-wired"></i> Fleksibilitas Infrastruktur (Solusi Dual Model)</h3>
            <p>Sistem ini dirancang sangat adaptif untuk merespon kebijakan keamanan TI di institusi Anda melalui tiga skema pemasangan (<i>deployment</i>):</p>
            <ul class="custom-list">
                <li><strong>Model Cloud (Disarankan):</strong> Sistem di-<i>host</i> di server awan kami. Institusi tidak perlu investasi perangkat keras, tidak pusing dengan perawatan maupun tagihan bandwith. Cukup dengan koneksi internet, sistem siap dipakai dari mana saja.</li>
                <li><strong>Model On-Premise (Konvensional):</strong> Sistem dipasang murni pada server internal kampus/sekolah (Intranet). Sangat cocok bagi institusi dengan kebijakan privasi data yang tertutup.</li>
                <li><strong>Model Hybrid:</strong> Menggabungkan keandalan server lokal untuk menyimpan arsip pusat, namun antarmukanya tetap bisa diekspos secara online via internet untuk kemudahan akses pengguna.</li>
            </ul>

            <h3 class="subsection-title"><i class="fas fa-users-cog"></i> Hak Akses & Multi-Portal Pengguna</h3>
            <p>Keamanan dan kenyamanan navigasi dijaga melalui sistem otorisasi bertingkat (<i>Role-Based Access Control</i>) dan <i>Single Sign-On</i> (SSO) menggunakan akun Google, Facebook, Twitter, dan LinkedIn. Portal dipisah secara spesifik:</p>
            <ul class="custom-list">
                <li><strong>Portal Yayasan & Pimpinan:</strong> Pusat kendali untuk memantau performa, statistik, dan neraca seluruh unit institusi.</li>
                <li><strong>Portal Dosen & Guru:</strong> Ruang kerja untuk menyusun materi, absen harian, penilaian mahasiswa, dan laporan beban kinerja.</li>
                <li><strong>Portal Mahasiswa & Siswa:</strong> Layanan mandiri untuk pengisian KRS, melihat KHS, akses E-Learning, perpustakaan, hingga ujian online.</li>
                <li><strong>Portal Orang Tua & Wali:</strong> Visibilitas penuh terhadap tagihan SPP bulanan, tunggakan, dan monitoring kehadiran anak di kelas.</li>
            </ul>

            <div class="page-break"></div>

            <h2 class="section-title">IV. Rincian Modul & Fungsionalitas Sistem</h2>
            <p>Aplikasi ini memiliki cakupan yang luar biasa luas, membentang dari fungsi inti akademik hingga tata kelola SDM dan Aset institusi.</p>
            <p>Setiap modul dirancang saling terhubung. Data yang diinput pada satu proses dapat dimanfaatkan kembali oleh proses lain sehingga institusi tidak perlu melakukan input berulang di banyak tempat.</p>

            <div class="metric-strip">
                <div class="metric"><strong>Akademik</strong><span>PMB, KRS, KHS, jadwal, ujian</span></div>
                <div class="metric"><strong>Keuangan</strong><span>Tagihan, pembayaran, jurnal, RKAT</span></div>
                <div class="metric"><strong>SDM & Aset</strong><span>HRIS, payroll, inventaris</span></div>
                <div class="metric"><strong>Integrasi</strong><span>Notifikasi, feeder, pickup gate, POS</span></div>
            </div>

            <h3 class="subsection-title"><i class="fas fa-book-reader"></i> A. Tata Kelola Akademik & Pembelajaran</h3>
            <ul class="dash-list">
                <li><strong>Penerimaan Baru (PMB / PPDB Online):</strong> Automasi penuh dari pendaftaran daring, unggah berkas, pengaturan paket pendaftaran, Ujian Saringan Masuk (CBT), kelulusan, hingga pencetakan nomor registrasi.</li>
                <li><strong>Pendataan Master Akademik:</strong> Sentralisasi data Program Studi, Kurikulum (SKS, Prasyarat), Mata Kuliah, Dosen, Kelas, dan Biodata Mahasiswa secara terstruktur.</li>
                <li><strong>Penjadwalan Anti-Bentrok:</strong> Algoritma pintar untuk menyusun jadwal perkuliahan, ujian, dan plot ruangan untuk mendeteksi serta mencegah terjadinya bentrok (<i>clash-free</i>) antar dosen maupun kelas.</li>
                <li><strong>Manajemen KRS & KHS:</strong> Pengambilan rencana studi mandiri oleh mahasiswa dengan validasi prasyarat matkul dan persetujuan (<i>approval</i>) Dosen Pembimbing Akademik. Dilengkapi rekap KHS otomatis.</li>
                <li><strong>E-Learning & LMS Terpadu:</strong> Ruang kelas virtual. Mendukung distribusi materi (integrasi bebas kuota via <strong>Google Drive</strong> dan <strong>Dropbox</strong>), memutar video <strong>YouTube/Instagram</strong>, forum diskusi, serta penugasan mandiri.</li>
                <li><strong>Ujian Online (CBT) Anti-Curang:</strong> Modul ujian jarak jauh (pilihan ganda & esai) lengkap dengan bank soal, pengaturan durasi waktu, pengawasan layar, dan analitik nilai seketika.</li>
                <li><strong>Kurikulum Ganda & OBE:</strong> Mengimplementasikan <i>Outcome Based Education</i> (OBE) secara penuh — perumusan <strong>Profil Lulusan (PL)</strong> sebagai fondasi program studi, <strong>Capaian Pembelajaran Lulusan (CPL)</strong> berdasarkan 4 kategori SN-Dikti (<i>Sikap, Pengetahuan, Keterampilan Umum, Keterampilan Khusus</i>), <strong>Capaian Pembelajaran Mata Kuliah (CPMK)</strong> dengan bobot dan minimal ketercapaian per RPS, hingga <strong>Sub-CPMK</strong> sebagai indikator penilaian terkecil. Dilengkapi 3 siklus monitoring berkelanjutan: <strong>Loop 1</strong> (ketercapaian CPMK per semester), <strong>Loop 2</strong> (ketercapaian CPL per tahun akademik), <strong>Loop 3</strong> (evaluasi Profil Lulusan setiap 3–5 tahun via Tracer Study). Dasbor <strong>PIKOBE</strong> mengukur kesiapan OBE institusi (tier Bronze/Silver/Gold/Platinum) dan menghasilkan rekomendasi <i>Continuous Quality Improvement</i> (CQI) otomatis. Fleksibel memadukan kurikulum Diknas dengan Kitab Kuning (khusus ePesantren).</li>
            </ul>

            <h3 class="subsection-title"><i class="fas fa-user-graduate"></i> B. Administrasi Mahasiswa & Penjaminan Mutu</h3>
            <ul class="dash-list">
                <li><strong>Sistem Pengajuan Birokrasi:</strong> Digitalisasi proses permohonan cuti, pengajuan beasiswa, hingga judul skripsi/tugas akhir secara <i>paperless</i>.</li>
                <li><strong>Manajemen Magang (PKL/KKN):</strong> Mendata alokasi lokasi magang, plot pembimbing, logbook aktivitas mingguan, dan rekap penilaian akhir.</li>
                <li><strong>Pencatatan Prestasi & Tracer Study:</strong> Pemeliharaan rekam jejak prestasi mahasiswa/siswa selama studi, dan pelacakan kiprah alumni di dunia kerja (survey kuesioner) untuk kepentingan evaluasi kurikulum.</li>
                <li><strong>Pendaftaran Wisuda:</strong> Meniadakan antrean loket yudisium. Cek silang status bebas pustaka, bebas keuangan, validasi ijazah, hingga pencetakan nomor kursi wisuda secara digital.</li>
            </ul>

            <h3 class="subsection-title"><i class="fas fa-microscope"></i> C. Pelaporan Riset, Pustaka & Nasional</h3>
            <ul class="dash-list">
                <li><strong>Integrasi 1-Click Sync (Neo Feeder & EMIS):</strong> Menghilangkan beban entri data berulang. Sistem menembakkan data akademik (mahasiswa, nilai, kelas, dosen) langsung ke Pangkalan Data PDDIKTI maupun EMIS Kemenag via API resmi.</li>
                <li><strong>Perpustakaan Digital (eLibrary):</strong> Manajemen inventaris buku (OPAC), barcode sirkulasi peminjaman/pengembalian, denda keterlambatan otomatis, <i>warning system</i> jatuh tempo pinjaman, serta <strong>integrasi miliaran referensi buku dari Google Books</strong>.</li>
                <li><strong>Penelitian & Pengabdian (Simlitabmas):</strong> Tata kelola pengajuan proposal riset, pencairan dana hibah, logbook penelitian, serta publikasi hasil pengabdian masyarakat.</li>
                <li><strong>Karya Ilmiah & Akreditasi:</strong> Pengelolaan naskah jurnal via <i>eJournal (OJS)</i>, repositori digital tesis/disertasi (standar DSpace), serta penyajian rekapitulasi data borang Akreditasi institusi.</li>
            </ul>

            <div class="page-break"></div>

            <h3 class="subsection-title"><i class="fas fa-chart-pie"></i> D. Tata Kelola Keuangan & Aset</h3>
            <ul class="dash-list">
                <li><strong>Finance & Tagihan Mahasiswa/Siswa:</strong> Generator tagihan massal otomatis (SPP, UKT, Pembangunan). Mendukung skema pembayaran bertahap (cicilan) dan pemantauan status tunggakan.</li>
                <li><strong>Akuntansi Buku Besar:</strong> Catatan transaksi keuangan masuk dan keluar akan langsung terposting sebagai jurnal akun. Menyediakan fasilitas pembentukan neraca, arus kas, dan laporan laba rugi.</li>
                <li><strong>Perencanaan & Realisasi Anggaran (RKAT):</strong> Modul penyusunan estimasi anggaran tahunan, plot dana departemen, pengajuan pencairan kas kecil/uang muka, dan kontrol ketat penyerapan anggaran.</li>
                <li><strong>Manajemen Aset & Inventaris:</strong> Pendataan lokasi fisik barang, kategorisasi, status aset, perhitungan penyusutan (depresiasi), pemeliharaan berkala, hingga sistem pengajuan <i>e-procurement</i> (pengadaan barang).</li>
            </ul>

            <h3 class="subsection-title"><i class="fas fa-users"></i> E. Manajemen SDM & Evaluasi Kinerja (HRIS)</h3>
            <ul class="dash-list">
                <li><strong>Data Kepegawaian Sentral:</strong> Penyimpanan profil detail dosen dan staf, kualifikasi, riwayat pendidikan, pelatihan, pengelolaan jam kerja (<i>shift</i>), serta permohonan izin/cuti harian.</li>
                <li><strong>Penggajian Otomatis (Payroll):</strong> Kalkulasi upah, honorarium mengajar, dan tunjangan yang terintegrasi penuh dengan rekap absensi. Mampu mencetak slip gaji digital bagi setiap karyawan.</li>
                <li><strong>Beban Kinerja Dosen (BKD):</strong> Mengawasi, mengevaluasi, dan merekapitulasi laporan realisasi pemenuhan kewajiban Tridharma Perguruan Tinggi oleh seluruh staf pengajar.</li>
                <li><strong>Indikator Kinerja Pegawai (KPI):</strong> Sistem penilaian obyektif kinerja karyawan, angket penilaian 360 derajat, buku harian (logbook) aktivitas kerja, dan pelaporan produktivitas sebagai rujukan pemberian insentif.</li>
            </ul>

            <h3 class="subsection-title"><i class="fas fa-cogs"></i> F. Operasional, Ekosistem Pintar & Integrasi</h3>
            <ul class="dash-list">
                <li><strong>Presensi Cerdas Multi-Metode:</strong> Rekapitulasi absensi kelas maupun absensi kedatangan pegawai menggunakan Barcode/QR Code, pemindaian wajah (<i>Face Recognition</i>), Kartu RFID, dan <strong>Mobile GPS Geotagging</strong> (menandai titik lokasi dan radius absen pada peta).</li>
                <li><strong>Integrasi Virtual Meeting:</strong> Dosen tidak perlu membagikan <i>link</i> manual. Sistem E-Learning terkoneksi langsung dengan ruang rapat virtual <strong>Google Meet, Zoom, Jitsi, Skype, dan Big Blue Button</strong>.</li>
                <li><strong>Integrasi Kalender & Peringatan:</strong> Seluruh jadwal kuliah dan agenda ujian otomatis diimpor ke <strong>Google Calendar</strong>, memberikan peringatan terpadu ke perangkat pengguna.</li>
                <li><strong>WhatsApp Gateway & Notifikasi:</strong> Sistem akan menembakkan pesan WhatsApp (<i>blast reminder</i>) untuk jatuh tempo SPP, validasi bukti transfer pembayaran, atau pemberitahuan ketidakhadiran siswa langsung ke ponsel orang tua/wali.</li>
                <li><strong>Sistem Kasir & Koperasi (POS):</strong> Manajemen warung/toko ATK, pencatatan transaksi dagang kasir (Point of Sale), pendaftaran anggota koperasi, simpan-pinjam, hingga otomatisasi pembagian Sisa Hasil Usaha (SHU).</li>
                <li><strong>Kantin Digital (eKantin):</strong> Mengubah area makan menjadi ekosistem <i>cashless</i>. Siswa cukup menempelkan kartu pintar (RFID/NFC) mereka untuk membayar jajanan. Sangat higienis, cepat, dan transparan.</li>
                <li><strong>Antar Jemput & Pickup Gate:</strong> Mengelola kendaraan, rute, jadwal, kartu penjemput, scan barcode/RFID di gerbang, monitor antrian, panggilan soundbox kelas, dan konfirmasi serah terima peserta secara digital.</li>
                <li><strong>Manajemen Kursus / Les Privat:</strong> Layanan operasional lembaga bimbingan belajar tambahan. Mengelola pendaftaran sesi les, penjadwalan tutor, evaluasi kemajuan belajar, hingga penerbitan e-Sertifikat kelulusan.</li>
            </ul>

            
            <!-- ENHANCE_MODUL_ANTAR_JEMPUT_START -->
            <div class="highlight-box bg-info bg-opacity-10" style="border-left-color: #0284c7;">
                <h5 class="fw-bold text-dark"><i class="fas fa-shuttle-van text-primary me-2"></i> Modul Baru: Antar Jemput Terintegrasi</h5>
                <p>
                    Modul <strong>Antar Jemput</strong> dirancang untuk membantu sekolah, kampus, pesantren, dan yayasan mengatur layanan kendaraan operasional, penjemputan di gerbang, pemanggilan peserta ke kelas, serta konfirmasi serah terima secara lebih aman dan terdokumentasi.
                </p>
                <ul class="dash-list mb-0">
                    <li><strong>Master kendaraan, sopir, kenek, rute, dan kartu penjemput:</strong> kendaraan dapat dikaitkan dengan inventaris aset resmi institusi, sedangkan sopir/kenek terhubung dengan data pegawai.</li>
                    <li><strong>Jadwal dan manifest peserta:</strong> admin dapat membuat jadwal antar jemput, menentukan rute, kendaraan, petugas, peserta siswa/mahasiswa/guru/dosen/pegawai, titik jemput, titik turun, dan catatan khusus.</li>
                    <li><strong>Validasi kartu atau barcode di gerbang:</strong> satpam melakukan tap kartu atau scan barcode untuk memastikan penjemput valid, aktif, dan sesuai dengan jadwal yang sedang berjalan.</li>
                    <li><strong>Monitor antrian dan soundbox kelas:</strong> nomor antrian tampil di monitor gerbang, sementara sistem dapat mengirimkan pengumuman ke perangkat kelas agar guru menyiapkan peserta yang dijemput.</li>
                    <li><strong>Audit trail serah terima:</strong> histori scan, status panggilan, log notifikasi, waktu keluar kelas, dan waktu serah terima tersimpan sebagai bukti kontrol keselamatan.</li>
                </ul>
            </div>
            <!-- ENHANCE_MODUL_ANTAR_JEMPUT_END -->

            <div class="highlight-box bg-primary bg-opacity-10">
                <h5 class="fw-bold text-primary"><i class="fas fa-plug-circle-check me-2"></i> Keunggulan Integrasi Modul</h5>
                <p class="mb-0">Kekuatan utama Enterprise Education terletak pada keterhubungan antar modul. Contohnya, data mahasiswa/siswa dari PMB dapat langsung menjadi dasar tagihan, akun portal, presensi, kelas, perpustakaan, kartu RFID, hingga laporan manajemen. Dengan demikian, institusi memperoleh alur kerja yang lebih hemat waktu dan lebih konsisten.</p>
            </div>

            <div class="page-break"></div>

            <h2 class="section-title">V. Inovasi Perangkat Keras (Hardware)</h2>
            <p>Untuk memperkuat pengalaman layanan modern, sistem dapat dilengkapi dengan perangkat fisik pendukung. Kehadiran perangkat ini membantu institusi menghadirkan layanan mandiri, transaksi yang lebih cepat, serta pengalaman digital yang terlihat nyata oleh pengguna.</p>

            <div class="row align-items-center mt-3 mb-5">
                <div class="col-md-4 text-center">
                    <div class="d-flex flex-column gap-2 mb-2">
                        <img src="<%=Common.ROOT%>/img/anjungan1.jpg" alt="ATM Model 1" class="img-fluid rounded-4 shadow-sm hover-zoom border border-secondary border-opacity-25" style="height: 200px; width: 100%; object-fit: cover; cursor: pointer;" onclick="showImageModal(this.src)" onerror="this.style.display='none'">
                        <div class="row g-2">
                            <div class="col-6">
                                <img src="<%=Common.ROOT%>/img/anjungan2.jpg" alt="ATM Model 2" class="img-fluid rounded-4 shadow-sm hover-zoom border border-secondary border-opacity-25" style="height: 120px; width: 100%; object-fit: cover; cursor: pointer;" onclick="showImageModal(this.src)" onerror="this.style.display='none'">
                            </div>
                            <div class="col-6">
                                <img src="<%=Common.ROOT%>/img/anjungan3.jpg" alt="ATM Model 3" class="img-fluid rounded-4 shadow-sm hover-zoom border border-secondary border-opacity-25" style="height: 120px; width: 100%; object-fit: cover; cursor: pointer;" onclick="showImageModal(this.src)" onerror="this.style.display='none'">
                            </div>
                        </div>
                    </div>
                    <div class="text-center small text-muted"><i class="fas fa-search-plus me-1"></i> Klik untuk memperbesar</div>
                </div>
                
                <div class="col-md-8">
                    <h3 class="mb-3 fw-bold text-primary" style="font-size: 1.5rem;"><i class="fas fa-desktop me-2"></i> Anjungan Transaksi Mandiri (ATM eCampus)</h3>
                    <p class="mb-3">
                        Ubah wajah lobi institusi Anda menjadi pusat layanan modern 24/7. Hanya dengan menempelkan kartu/QR, mahasiswa dapat mencetak <strong>KRS, KHS, Transkrip Nilai, dan Surat Keterangan</strong> secara mandiri dalam hitungan detik tanpa antrean Tata Usaha.
                    </p>
                    
                    <div class="p-4 bg-white border rounded-4 shadow-sm">
                        <h6 class="fw-bold text-dark mb-2 border-bottom pb-2">Spesifikasi Detail Perangkat Anjungan (ATM)</h6>
                        <div class="row">
                            <div class="col-md-6">
                                <ul class="spec-list">
                                    <li><strong>Layar:</strong> 24 Inch Touchscreen</li>
                                    <li><strong>Sistem:</strong> Mini PC Industrial Intel i5 YC-Series</li>
                                    <li><strong>Processor:</strong> Intel i5 Chipset HM70</li>
                                    <li><strong>Grafis:</strong> Intel HD 2500</li>
                                    <li><strong>Memori:</strong> RAM 8G DDR3, SSD 256 Gb</li>
                                    <li><strong>Audio:</strong> Realtek HD ALC 662</li>
                                    <li><strong>Jaringan:</strong> Ethernet 10/100/1000 & Wi-Fi 802.11 b/g/n</li>
                                    <li><strong>Port:</strong> Bluetooth, HDMI 1x, VGA 1x, USB2.0 4x, Com Port 2x, Lan 2x, Wifi Antena 2x, Spk 1x, Mic 1x, On/Off 1x</li>
                                </ul>
                            </div>
                            <div class="col-md-6">
                                <ul class="spec-list">
                                    <li><strong>Printer:</strong> Epson L120
                                        <ul class="list-unstyled ms-2 text-muted" style="font-size: 0.85rem;">
                                            <li>- Print Speed: Black 8.5 ipm, Color 4.5 ipm</li>
                                            <li>- Resolution: 720 x 720 dpi</li>
                                            <li>- Max Media Size: A4</li>
                                            <li>- Input Tray: 50 sheets (75g/m²)</li>
                                        </ul>
                                    </li>
                                    <li><strong>Daya:</strong> Printing 10W, Standby 2.0W, Sleep 0.6W, Power Off 0.3W</li>
                                    <li><strong>Casing:</strong> 2mm sheet metal. Painting with duco. Include sticker.</li>
                                    <li><strong>Sistem Tertanam:</strong> Aplikasi ATM Berbasis Java (Informasi Umum, PSB, Pustaka, dll).</li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <hr class="border-secondary opacity-25 my-4">

            <div class="row align-items-center mt-4 mb-3">
                <div class="col-md-4 text-center order-md-2">
                    <div class="d-flex flex-column gap-2 mb-2">
                        <img src="<%=Common.ROOT%>/img/pos1.jpg" alt="POS Model 1" class="img-fluid rounded-4 shadow-sm hover-zoom border border-secondary border-opacity-25" style="height: 160px; width: 100%; object-fit: cover; cursor: pointer;" onclick="showImageModal(this.src)" onerror="this.src='https://via.placeholder.com/400x200?text=POS+Image+1'">
                        <img src="<%=Common.ROOT%>/img/pos2.jpg" alt="POS Model 2" class="img-fluid rounded-4 shadow-sm hover-zoom border border-secondary border-opacity-25" style="height: 160px; width: 100%; object-fit: cover; cursor: pointer;" onclick="showImageModal(this.src)" onerror="this.src='https://via.placeholder.com/400x200?text=POS+Image+2'">
                    </div>
                    <div class="text-center small text-muted"><i class="fas fa-search-plus me-1"></i> Klik untuk memperbesar</div>
                </div>
                
                <div class="col-md-8 order-md-1">
                    <h3 class="mb-3 fw-bold text-success" style="font-size: 1.5rem;"><i class="fas fa-cash-register me-2"></i> Mesin Kasir ANFO POS DUAL</h3>
                    <p class="mb-3">
                        Perangkat Point of Sale (POS) premium yang dilengkapi <strong>dua layar interaktif</strong>. Sangat ideal untuk mendukung operasional Koperasi, Kantin Digital, maupun Unit Usaha institusi Anda dengan tingkat transparansi transaksi yang tinggi bagi pelanggan.
                    </p>
                    
                    <div class="p-4 bg-white border rounded-4 shadow-sm">
                        <h6 class="fw-bold text-dark mb-2 border-bottom pb-2">Spesifikasi Detail ANFO POS DUAL</h6>
                        <div class="row">
                            <div class="col-md-6">
                                <ul class="spec-list">
                                    <li><strong>Layar Kasir:</strong> 15.6 Inch Touchscreen</li>
                                    <li><strong>Layar Pelanggan:</strong> 10 Inch Non Touchscreen</li>
                                    <li><strong>CPU:</strong> Intel Core i3-10110U (2C/4T, 4M Cache, 2.1GHz to 4.1GHz)</li>
                                    <li><strong>Graphics:</strong> Intel UHD Graphics (Base 300MHz, Max. 1000MHz), TDP 15W</li>
                                    <li><strong>Memory:</strong> 16GB DDR4 Dual Channel (Max. Exp 64GB)</li>
                                    <li><strong>Storage:</strong> 512GB SSD</li>
                                    <li><strong>Audio:</strong> HDA CODEC</li>
                                </ul>
                            </div>
                            <div class="col-md-6">
                                <ul class="spec-list">
                                    <li><strong>Thermal Printer:</strong> Thermal Line Printing
                                        <ul class="list-unstyled ms-2 text-muted" style="font-size: 0.85rem;">
                                            <li>- Print Speed: 90mm/s</li>
                                            <li>- Lebar Cetakan: Max 80mm (376 dot)</li>
                                            <li>- Lebar Kertas: 79.5 +/- 0.5 mm</li>
                                            <li>- Support RJ.11, Emulation ESC/POS</li>
                                            <li>- Interface: USB + Bluetooth 3.0, 4.0, 4.1, 4.2</li>
                                        </ul>
                                    </li>
                                    <li><strong>Cash Drawer (Laci Kasir):</strong> Merk VSC RJ-11 (49x48x16 cm). 6 Slot Uang Kertas & 4 Slot Uang Logam (3 posisi anak kunci).</li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="page-break"></div>

            <h2 class="section-title">VI. Metodologi Implementasi & Pendampingan</h2>
            <p>Implementasi Enterprise Education dilakukan secara bertahap agar proses migrasi, pelatihan, konfigurasi, dan adopsi pengguna dapat berjalan terkendali. Pendekatan ini membantu institusi mengurangi risiko perubahan sistem sekaligus memastikan setiap unit kerja memahami alur baru.</p>

            <div class="timeline">
                <div class="timeline-step">
                    <div class="timeline-number">01</div>
                    <div class="timeline-content">
                        <h5>Assessment Kebutuhan & Pemetaan Proses</h5>
                        <p>Mengidentifikasi kondisi eksisting, struktur unit kerja, format data, kebutuhan hak akses, serta prioritas modul yang akan diaktifkan.</p>
                    </div>
                </div>
                <div class="timeline-step">
                    <div class="timeline-number">02</div>
                    <div class="timeline-content">
                        <h5>Konfigurasi Sistem & Migrasi Data Awal</h5>
                        <p>Menyiapkan parameter institusi, tahun akademik, struktur akademik, akun pengguna, master biaya, dan data utama yang diperlukan.</p>
                    </div>
                </div>
                <div class="timeline-step">
                    <div class="timeline-number">03</div>
                    <div class="timeline-content">
                        <h5>Pelatihan Admin, Operator, dan Pengguna Kunci</h5>
                        <p>Memberikan pendampingan penggunaan modul, simulasi transaksi, validasi laporan, serta penyusunan alur kerja baru yang lebih efektif.</p>
                    </div>
                </div>
                <div class="timeline-step">
                    <div class="timeline-number">04</div>
                    <div class="timeline-content">
                        <h5>Go-Live Bertahap & Monitoring</h5>
                        <p>Sistem mulai digunakan pada modul prioritas, dilanjutkan pemantauan, penyempurnaan konfigurasi, dan dukungan teknis sesuai kebutuhan.</p>
                    </div>
                </div>
            </div>

            <h3 class="subsection-title"><i class="fas fa-people-arrows"></i> Ruang Lingkup Pendampingan</h3>
            <div class="service-scope">
                <div class="scope-item">
                    <h5><i class="fas fa-sliders me-2"></i> Konfigurasi</h5>
                    <p class="value-desc">Pengaturan parameter sistem, struktur data, hak akses, template dokumen, dan kebutuhan awal operasional.</p>
                </div>
                <div class="scope-item">
                    <h5><i class="fas fa-chalkboard-user me-2"></i> Pelatihan</h5>
                    <p class="value-desc">Transfer knowledge kepada operator dan pengguna kunci agar mampu menjalankan sistem secara mandiri.</p>
                </div>
                <div class="scope-item">
                    <h5><i class="fas fa-headset me-2"></i> Dukungan Teknis</h5>
                    <p class="value-desc">Bantuan teknis selama proses implementasi, penyesuaian alur, dan pendampingan saat sistem mulai digunakan.</p>
                </div>
            </div>

            <div class="highlight-box">
                <h5 class="fw-bold text-dark"><i class="fas fa-lightbulb text-warning me-2"></i> Rekomendasi Implementasi</h5>
                <p class="mb-0">Agar hasil implementasi lebih optimal, aktivasi sistem dapat dimulai dari modul prioritas seperti data master, pembayaran, portal pengguna, presensi, dan dashboard pimpinan. Setelah pengguna terbiasa, modul lanjutan seperti akuntansi, aset, e-learning, pelaporan nasional, koperasi, dan integrasi hardware dapat ditambahkan secara bertahap.</p>
            </div>

            <div class="page-break"></div>

            <h2 class="section-title">VII. Skema Investasi Lisensi & Hardware</h2>
            <p>
                Kami menyediakan dua opsi skema kemitraan untuk perangkat lunak (<i>Software / Sistem</i>). Sementara untuk perangkat keras (<i>Hardware Anjungan & POS</i>) bersifat opsional dan terpisah.
            </p>
            <div class="highlight-box bg-warning bg-opacity-10" style="border-left-color: var(--warning-color);">
                <h5 class="fw-bold text-dark"><i class="fas fa-coins text-warning me-2"></i> Prinsip Investasi yang Fleksibel</h5>
                <p class="mb-0">Skema pembiayaan disusun agar institusi dapat memilih model yang paling sesuai: <strong>tanpa beban lisensi langsung</strong> melalui skema transaksi, atau <strong>anggaran tetap yang lebih mudah diproyeksikan</strong> melalui biaya pengguna aktif bulanan.</p>
            </div>

            <h3 class="subsection-title"><i class="fas fa-laptop-code"></i> A. Pembiayaan Perangkat Lunak (Software)</h3>
            <div class="row mt-3 g-4 align-items-stretch mb-4">
                
                <div class="col-md-6">
                    <div class="pricing-card border-primary">
                        <div class="pricing-header">
                            <h4 class="fw-bold text-primary">Opsi 1: Berbasis Transaksi</h4>
                            <p class="text-muted small">Skema Zero Investment</p>
                            <div class="pricing-price">Rp 0,-</div>
                            <span class="badge bg-primary text-white rounded-pill px-3 py-2 mt-2 mb-3">Institusi Bebas Biaya Lisensi</span>
                        </div>
                        <ul class="custom-list small">
                            <li><strong><i class="fas fa-user-check text-primary me-1"></i> Syarat:</strong> Minimal 2.000 Mahasiswa/Siswa aktif dan <strong>wajib menggunakan pembayaran tiap bulan</strong>. (Jika sistem UKT hanya 1x per semester, maka <strong>tidak memenuhi syarat</strong>).</li>
                            <li><strong><i class="fas fa-hand-holding-usd text-primary me-1"></i> Biaya Admin Transaksi:</strong> Rp 7.000 / transaksi (Untuk semua channel kecuali QRIS 1%).</li>
                            <li><strong><i class="fas fa-users text-primary me-1"></i> Beban Biaya:</strong> Admin dibebankan kepada pembayar (Orang Tua / Mahasiswa). Institusi tidak keluar biaya lisensi/server bulanan sama sekali.</li>
                        </ul>
                    </div>
                </div>

                <div class="col-md-6">
                    <div class="pricing-card" style="border-color: var(--warning-color);">
                        <div class="pricing-header">
                            <h4 class="fw-bold" style="color: var(--warning-color);">Opsi 2: Berbasis Pengguna Aktif</h4>
                            <p class="text-muted small">Skema Fixed Budget</p>
                            <div class="pricing-price" style="font-size: 1.8rem; color: var(--warning-color);">Rp 2.000 <span class="fs-6 text-muted fw-normal">/ siswa / bln</span></div>
                            <div class="pricing-price mt-2" style="font-size: 1.8rem; color: var(--warning-color);">Rp 4.000 <span class="fs-6 text-muted fw-normal">/ mhsw / bln</span></div>
                        </div>
                        <ul class="custom-list small">
                            <li><strong><i class="fas fa-layer-group text-warning me-1"></i> Syarat Minimum:</strong> Tagihan dihitung minimal 1.000 pengguna aktif (Jika kurang dari 1.000, tetap dibulatkan setara 1.000 pengguna).</li>
                            <li><strong><i class="fas fa-university text-warning me-1"></i> Mekanisme:</strong> Biaya sewa aplikasi dibayarkan dari pos anggaran pengeluaran Institusi.</li>
                            <li><strong><i class="fas fa-smile text-warning me-1"></i> Kelebihan:</strong> Biaya admin di sisi orang tua murid menjadi jauh lebih ringan saat membayar SPP.</li>
                        </ul>
                    </div>
                </div>
            </div>

            <div class="table-modern mb-4">
                <table class="table table-bordered align-middle">
                    <thead>
                        <tr>
                            <th><%= Common.getBahasaConfig("Aspek") %></th>
                            <th><%= Common.getBahasaConfig("Opsi Berbasis Transaksi") %></th>
                            <th><%= Common.getBahasaConfig("Opsi Pengguna Aktif") %></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><strong>Karakter Biaya</strong></td>
                            <td>Institusi tidak membayar lisensi bulanan aplikasi.</td>
                            <td>Biaya rutin lebih mudah diproyeksikan dalam anggaran institusi.</td>
                        </tr>
                        <tr>
                            <td><strong>Cocok Untuk</strong></td>
                            <td>Institusi dengan transaksi pembayaran rutin bulanan.</td>
                            <td>Institusi yang ingin biaya admin pembayar lebih ringan.</td>
                        </tr>
                        <tr>
                            <td><strong>Fokus Manfaat</strong></td>
                            <td>Meminimalkan investasi awal dari institusi.</td>
                            <td>Menjaga stabilitas biaya dan fleksibilitas layanan kepada pengguna.</td>
                        </tr>
                    </tbody>
                </table>
            </div>
            
            <div class="highlight-box bg-success bg-opacity-10 border-success mb-5">
                <h5 class="fw-bold text-success"><i class="fas fa-handshake me-2"></i> Keterbukaan Negosiasi Kemitraan</h5>
                <p class="mb-0 text-justify small">Harga dan skema pembiayaan yang tercantum di atas merupakan penawaran awal yang <strong>mungkin dapat berbeda sesuai dengan kesepakatan akhir</strong>. Kami sangat terbuka dan membuka ruang negosiasi seluas-luasnya agar sistem ini dapat segera memberikan manfaat transformasi digital yang nyata bagi institusi pendidikan Anda.</p>
            </div>

            <h3 class="subsection-title"><i class="fas fa-server"></i> B. Pengadaan Perangkat Keras (Hardware Opsional)</h3>
            <p class="mb-3">Pengadaan perangkat keras fisik <strong>Anjungan Transaksi Mandiri (ATM)</strong> dan <strong>Mesin Kasir POS DUAL</strong> bersifat <strong>opsional dan di luar skema sewa Software</strong>. Rincian biaya per unit adalah sebagai berikut:</p>
            
            <div class="row g-4 mt-2 mb-4">
                <div class="col-md-4">
                    <div class="p-4 bg-white border border-secondary border-opacity-25 rounded-4 shadow-sm text-center h-100">
                        <div class="text-muted fw-bold small text-uppercase mb-2">Anjungan ATM</div>
                        <h6 class="fw-bold text-dark border-bottom pb-3">Tipe Non-Touchscreen</h6>
                        <div class="text-center my-3"><h4 class="fw-bold text-dark">Rp 37.500.000,-</h4></div>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="p-4 bg-white border border-primary border-opacity-50 rounded-4 shadow-sm text-center h-100">
                        <div class="text-muted fw-bold small text-uppercase mb-2">Anjungan ATM</div>
                        <h6 class="fw-bold text-primary border-bottom pb-3">Tipe Touchscreen</h6>
                        <div class="text-center my-3"><h4 class="fw-bold text-primary">Rp 42.500.000,-</h4></div>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="p-4 bg-white border border-success border-opacity-50 rounded-4 shadow-sm text-center h-100">
                        <div class="text-muted fw-bold small text-uppercase mb-2">Mesin Kasir</div>
                        <h6 class="fw-bold text-success border-bottom pb-3">ANFO POS DUAL</h6>
                        <div class="text-center my-3"><h4 class="fw-bold text-success">Rp 30.000.000,-</h4></div>
                        <div class="text-muted small">/ Paket Lengkap</div>
                    </div>
                </div>
            </div>

            <div class="alert alert-warning py-3 px-4 shadow-sm" style="border-left: 5px solid var(--warning-color);">
                <h6 class="fw-bold"><i class="fas fa-exclamation-circle me-2"></i> Ketentuan Pengadaan Hardware:</h6>
                <ul class="mb-0 small" style="padding-left: 1.2rem;">
                    <li>Harga <strong>belum termasuk Pajak</strong> dan <strong>Ongkos Kirim</strong> ke luar area Jabodetabek.</li>
                    <li>Syarat Pembayaran: <strong>DP Rp 30.000.000,- di awal</strong> (sebelum proses produksi). Sisa pelunasan dibayarkan saat barang siap dikirim.</li>
                    <li>Estimasi waktu proses manufaktur / perakitan: <strong>Kurang lebih 1 bulan</strong>.</li>
                </ul>
            </div>

            <div class="page-break"></div>

            <h2 class="section-title">VIII. Penutup & Ajakan Kerja Sama</h2>
            <div class="highlight-box">
                <h5 class="fw-bold text-dark"><i class="fas fa-handshake-angle text-primary me-2"></i> Komitmen Kemitraan</h5>
                <p class="mb-0">Kami tidak hanya menawarkan aplikasi, tetapi menawarkan pendampingan transformasi proses kerja. Dengan kerja sama yang terarah, Enterprise Education diharapkan menjadi fondasi digital yang memperkuat kualitas layanan, transparansi tata kelola, dan reputasi institusi dalam jangka panjang.</p>
            </div>

            <div class="text-center mt-3">
                <i class="fas fa-handshake fa-4x text-success mb-4"></i>
                <h2 class="fw-bold mb-4 text-dark">Melangkah ke Masa Depan Pendidikan Bersama Kami</h2>
                <p class="text-muted fs-5 mb-5 mx-auto" style="max-width: 850px;">
                    Enterprise Education telah teruji validitasnya dan dipercaya mengawal revolusi digitalisasi di lebih dari 40 perguruan tinggi dan jaringan institusi sekolah yang tersebar luas dari pesisir Sumatera hingga daratan Papua. Jangan biarkan instansi Anda tertinggal di belakang. Ambil keputusan strategis sekarang untuk melompat lebih maju dari kompetitor pendidikan Anda.
                </p>
                <p class="text-muted mb-4 fs-5">
                    Silakan jalin komunikasi dengan representatif konsultan kami untuk melakukan penjadwalan presentasi demo produk (<i>Live Demo</i>) tatap muka, diskusi teknis konfigurasi migrasi, atau sesi negosiasi pendalaman kerja sama.
                </p>
                
                <% if(paramPresentBy != null && !paramPresentBy.trim().isEmpty()) { %>
                    <div class="d-inline-block p-5 mt-3 bg-white rounded-4 border border-secondary shadow text-start" style="min-width: 400px;">
                        <h5 class="fw-bold text-primary mb-4 text-center"><i class="fas fa-user-tie me-2"></i> <%= Common.getBahasaConfig("Konsultan Resmi Institusi Anda") %></h5>
                        <div class="fs-4 mb-2 text-dark text-center"><strong><%=paramPresentBy%></strong></div>
                        <% if(paramTelp != null && !paramTelp.trim().isEmpty()) { %>
                            <div class="fs-5 mb-4 text-dark text-center"><i class="fab fa-whatsapp text-success me-2"></i> <%=paramTelp%></div>
                            <div class="text-center mt-4">
                                <a href="https://wa.me/<%=paramTelp.replaceAll("[^0-9]", "")%>" target="_blank" class="btn btn-success btn-lg rounded-pill px-5 py-3 fw-bold shadow"><i class="fab fa-whatsapp me-2 fs-5"></i> Konsultasi via WhatsApp</a>
                            </div>
                        <% } %>
                    </div>
                <% } else { %>
                    <div class="d-inline-block p-5 mt-3 bg-white rounded-4 border border-secondary shadow text-start" style="min-width: 450px;">
                        <h4 class="fw-bold text-primary mb-4 text-center">Tim Pusat Enterprise Education</h4>
                        <div class="fs-5 mb-3 border-bottom pb-2"><i class="fas fa-globe text-muted me-3"></i> <a href="<%=(paramWebsite != null && !paramWebsite.trim().isEmpty()) ? paramWebsite : "https://ecampus.id"%>" target="_blank" class="text-decoration-none text-dark fw-bold"><%=(paramWebsite != null && !paramWebsite.trim().isEmpty()) ? paramWebsite : "https://ecampus.id"%></a></div>
                        <% if(Telepon != null && !Telepon.trim().isEmpty()) { %>
                            <div class="fs-5 mb-3 border-bottom pb-2"><i class="fas fa-phone text-success me-3"></i> <%=Telepon%></div>
                        <% } %>
                        <% if(Email != null && !Email.trim().isEmpty()) { %>
                            <div class="fs-5 mb-3 border-bottom pb-2"><i class="fas fa-envelope text-primary me-3"></i> <%=Email%></div>
                        <% } %>
                        <% if(Alamat1 != null && !Alamat1.trim().isEmpty()) { %>
                            <div class="fs-6 mb-3 border-bottom pb-2"><i class="fas fa-location-dot text-danger me-3"></i> <%=Alamat1%></div>
                        <% } %>
                        <div class="fs-5 mb-3 border-bottom pb-2"><i class="fas fa-desktop text-info me-3"></i> <strong>Akses Free Trial (Live Demo):</strong> <br><a href="https://demo.ecampus.id" target="_blank" class="text-decoration-none text-primary ms-5">https://demo.ecampus.id</a>
                            <div class="ms-5 mt-2 bg-light p-2 rounded text-muted fs-6">
                                <span class="d-block"><strong>Username:</strong> demo</span>
                                <span class="d-block"><strong>Password:</strong> demo123</span>
                            </div>
                        </div>
                    </div>
                <% } %>
            </div>

            <div class="signature-line">
                <div class="signature-box">
                    <div class="meta-label">Diajukan oleh</div>
                    <div class="meta-value mt-2"><%=(paramPresentBy != null && !paramPresentBy.trim().isEmpty()) ? paramPresentBy : "Tim Enterprise Education"%></div>
                    <div class="text-muted small mt-5 pt-4">Tanda tangan dan nama jelas</div>
                </div>
                <div class="signature-box">
                    <div class="meta-label">Diterima / Ditinjau oleh</div>
                    <div class="meta-value mt-2"><%=(paramClient != null && !paramClient.trim().isEmpty()) ? paramClient : "Calon Mitra Institusi"%></div>
                    <div class="text-muted small mt-5 pt-4">Tanda tangan dan nama jelas</div>
                </div>
            </div>

            <div class="confidential-note">
                <i class="fas fa-lock me-2"></i> Dokumen ini bersifat konfidensial dan ditujukan untuk kebutuhan evaluasi kerja sama. Informasi di dalamnya tidak untuk disebarluaskan tanpa persetujuan pihak terkait.
            </div>

            <div class="text-center mt-5 pt-4 border-top">
                <small class="text-muted fw-semibold">&copy; <%=Calendar.getInstance().get(Calendar.YEAR) %> <%=judul%>. Seluruh hak kekayaan intelektual dilindungi. Dokumen proposal penawaran ini bersifat konfidensial.</small>
            </div>

        </div>
    </div>

    <div class="modal fade" id="imageModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered modal-xl">
        <div class="modal-content bg-transparent border-0">
          <div class="modal-header border-0 pb-0 justify-content-end">
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close" style="filter: invert(1) brightness(200%);"></button>
          </div>
          <div class="modal-body text-center p-0">
            <img id="modalExpandedImage" src="" class="img-fluid rounded-4 shadow-lg" alt="Hardware View" style="max-height: 85vh; width: auto;">
          </div>
        </div>
      </div>
    </div>

    <script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        // Script untuk Lightbox Modal
        function showImageModal(imageSrc) {
            document.getElementById('modalExpandedImage').src = imageSrc;
            var imageModal = new bootstrap.Modal(document.getElementById('imageModal'));
            imageModal.show();
        }
    </script>
    <jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp" />
</body>
</html>