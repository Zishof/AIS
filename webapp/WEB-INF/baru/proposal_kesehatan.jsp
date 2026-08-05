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
String nomorProposal = (paramNomorProposal != null && !paramNomorProposal.trim().isEmpty()) ? paramNomorProposal : "EE/PROP-EMEDIC/" + Calendar.getInstance().get(Calendar.YEAR);

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
    <title><%= Common.getBahasaConfig("Proposal Modul Kesehatan (eMedic)") %> | <%=judul%></title>

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
        .cover-title { font-size: 2.65rem; font-weight: 900; color: var(--primary-dark); margin-bottom: 1rem; text-transform: uppercase; letter-spacing: 1px; line-height: 1.18; }
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

        .btn-print { position: fixed; bottom: 2rem; right: 2rem; z-index: 1000; border-radius: 50px; padding: 1rem 2rem; box-shadow: 0 10px 20px rgba(2, 132, 199, 0.3); font-size: 1.1rem;}

        .proposal-watermark { position: absolute; right: -70px; top: 180px; font-size: 7rem; font-weight: 900; color: rgba(2,132,199,0.035); transform: rotate(-18deg); pointer-events: none; user-select: none; letter-spacing: 2px; }
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
            <div class="proposal-watermark">EMEDIC</div>

            <div class="cover-page">
                <img src="<%=logo_PerguruanTinggi%>" alt="Logo" class="cover-logo">
                <div class="cover-badge"><i class="fas fa-shield-halved"></i> Dokumen Penawaran Resmi Siap Cetak</div>
                <h1 class="cover-title"><i class="fas fa-hospital me-3"></i><%= Common.getBahasaConfig("Proposal Penawaran Kerjasama") %></h1>
                <h2 class="cover-subtitle"><%= Common.getBahasaConfig("Implementasi Sistem Informasi Rumah Sakit, Klinik & Puskesmas (eMedic)") %></h2>
                <p class="cover-intro">
                    Solusi <strong>eMedic</strong> dirancang untuk membantu Rumah Sakit, Klinik, Puskesmas, dan layanan fasilitas kesehatan lainnya membangun ekosistem digital terpadu: lebih cepat dalam layanan pasien, lebih akurat dalam stok obat, lebih transparan dalam billing, dan siap terintegrasi dengan BPJS/JKN serta SATUSEHAT Kemenkes di masa mendatang.
                </p>
                <h4 class="fw-bold text-primary mt-4 px-5 py-3 bg-light border border-primary border-opacity-25 rounded-pill d-inline-block shadow-sm">
                    <i class="fas fa-hospital-user me-2"></i> <%=judul%>
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
                        <div class="meta-value">Sistem Informasi Rumah Sakit Terpadu</div>
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
                <h2 class="fw-bold mb-3"><i class="fas fa-rocket me-2"></i> Proposal Transformasi Digital Layanan Kesehatan</h2>
                <p class="mb-3">
                    Proposal ini disusun sebagai dasar kerja sama implementasi <strong>eMedic</strong>, yaitu modul Sistem Informasi Rumah Sakit/Klinik/Puskesmas yang menyatukan layanan registrasi, rekam medis, farmasi, gudang, billing, dashboard analitik, dan pelaporan dalam satu sistem yang saling terhubung.
                </p>
                <p class="mb-0">
                    Fokus utama penawaran ini adalah membantu fasilitas kesehatan membangun layanan yang lebih <strong>cepat, tertib, akuntabel, mudah diaudit, dan aman bagi keselamatan pasien</strong> — baik sebagai unit kesehatan yang bernaung di bawah institusi pendidikan (klinik kampus/sekolah/pesantren), maupun sebagai Rumah Sakit/Klinik/Puskesmas yang berdiri sendiri.
                </p>
            </div>

            <div class="metric-strip">
                <div class="metric"><strong>Terpadu</strong><span>Registrasi, rekam medis, farmasi, billing</span></div>
                <div class="metric"><strong>Real-Time</strong><span>Stok obat dan peringatan kadaluarsa</span></div>
                <div class="metric"><strong>Teraudit</strong><span>Jejak perubahan data penuh</span></div>
                <div class="metric"><strong>Scalable</strong><span>Cloud, on-premise, atau hybrid</span></div>
            </div>

            <div class="toc-box mb-5">
                <h5 class="fw-bold text-primary mb-3"><i class="fas fa-list-check me-2"></i> Struktur Proposal</h5>
                <ol class="toc-list">
                    <li>Pendahuluan dan latar belakang kebutuhan digitalisasi layanan kesehatan.</li>
                    <li>Visi, misi, tujuan, manfaat, dan nilai strategis implementasi.</li>
                    <li>Ekosistem eMedic: terintegrasi dengan eCampus/eSchool/ePesantren maupun berdiri sendiri.</li>
                    <li>Rincian modul serta fungsionalitas sistem.</li>
                    <li>Roadmap integrasi BPJS/JKN dan SATUSEHAT Kemenkes.</li>
                    <li>Metodologi implementasi dan layanan pendampingan.</li>
                    <li>Skema investasi (Beli Putus, Langganan Bulanan, atau Bagi Hasil per Transaksi).</li>
                </ol>
            </div>

            <h2 class="section-title">I. Pendahuluan & Latar Belakang</h2>
            <p>
                Rumah Sakit, Klinik, dan Puskesmas saat ini menghadapi tantangan yang kompleks dalam mengelola rekam medis, farmasi, dan administrasi pasien secara efektif. Proses yang masih bersifat manual seringkali memakan waktu, rentan terhadap kesalahan (<i>human error</i>), dan menyulitkan akses informasi secara cepat bagi tenaga medis maupun pimpinan.
            </p>
            <p>
                Perkembangan teknologi informasi kesehatan telah membawa perubahan mendasar. Fasilitas kesehatan dituntut untuk terus beradaptasi agar dapat memberikan pelayanan yang aman, cepat, dan akuntabel kepada pasien, sekaligus meningkatkan kesiapan menghadapi regulasi nasional seperti BPJS/JKN dan SATUSEHAT.
            </p>
            <p>
                Oleh karena itu, kami hadir menawarkan <strong>eMedic</strong>, sebuah solusi Sistem Informasi Rumah Sakit (SIRS) <i>end-to-end</i> terpadu berbasis web. Modul ini merupakan bagian dari ekosistem <strong>Enterprise Education</strong> yang lebih luas, sehingga jika Universitas, Sekolah, atau Pondok Pesantren Anda memiliki klinik, rumah sakit, atau layanan medis sendiri, modul ini dapat langsung terintegrasi dengan data mahasiswa/siswa/santri/pegawai yang sudah ada — tanpa perlu membangun sistem terpisah.
            </p>

            <div class="highlight-box">
                <h5 class="fw-bold text-dark"><i class="fas fa-exclamation-triangle text-warning me-2"></i> Identifikasi Masalah & Tantangan Fasilitas Kesehatan:</h5>
                <ul class="dash-list mt-3 mb-0 text-dark">
                    <li><strong>Resep Kertas:</strong> Tulisan tangan dokter yang sulit dibaca meningkatkan risiko kesalahan pemberian obat/dosis.</li>
                    <li><strong>Stok Farmasi Tidak Akurat:</strong> Obat dapat kosong mendadak atau menumpuk berlebih tanpa disadari.</li>
                    <li><strong>Kadaluarsa Tak Terdeteksi Dini:</strong> Obat kadaluarsa terpakai atau terbuang tanpa peringatan lebih awal.</li>
                    <li><strong>Billing Terpisah dari Rekam Medis:</strong> Tagihan pasien tidak selalu sinkron dengan tindakan/obat yang diberikan.</li>
                </ul>
            </div>

            <h3 class="subsection-title"><i class="fas fa-arrows-spin"></i> Arah Solusi yang Ditawarkan</h3>
            <div class="impact-row">
                <div class="impact-card before">
                    <h5 class="fw-bold text-danger"><i class="fas fa-triangle-exclamation me-2"></i> Kondisi yang Sering Terjadi</h5>
                    <ul class="dash-list mb-0">
                        <li>Rekam medis dan resep tersebar di banyak berkas fisik.</li>
                        <li>Validasi stok obat membutuhkan waktu lama dan bergantung pada proses manual.</li>
                        <li>Laporan pimpinan sering terlambat karena rekapitulasi harus dilakukan ulang.</li>
                    </ul>
                </div>
                <div class="impact-arrow"><i class="fas fa-arrow-right-long"></i></div>
                <div class="impact-card after">
                    <h5 class="fw-bold text-success"><i class="fas fa-circle-check me-2"></i> Kondisi Setelah Implementasi</h5>
                    <ul class="dash-list mb-0">
                        <li>Satu basis data terpusat yang dapat diakses sesuai hak akses.</li>
                        <li>Alur registrasi, rekam medis, farmasi, dan billing berjalan digital.</li>
                        <li>Dashboard analitik tersedia untuk pengambilan keputusan berbasis data.</li>
                    </ul>
                </div>
            </div>

            <h2 class="section-title">II. Visi, Misi, Tujuan, dan Manfaat</h2>

            <p><strong>Visi Kami:</strong> Menjadi sistem informasi rumah sakit/klinik terpadu yang unggul dan terpercaya, mendukung fasilitas kesehatan mengakselerasi mutu layanan dan keselamatan pasien.</p>

            <p><strong>Misi Kami:</strong></p>
            <ul class="custom-list">
                <li>Menyediakan solusi sistem informasi kesehatan yang komprehensif, terpusat, dan <i>user-friendly</i>.</li>
                <li>Meningkatkan efisiensi operasional klinis dan transparansi tata kelola farmasi/stok.</li>
                <li>Mendukung pimpinan dalam pengambilan keputusan strategis berdasarkan analisis data yang riil.</li>
                <li>Menyiapkan fondasi integrasi BPJS/JKN dan SATUSEHAT secara bertahap dan aman.</li>
            </ul>

            <h3 class="subsection-title"><i class="fas fa-bullseye"></i> Tujuan & Manfaat Implementasi</h3>
            <div class="row mt-3">
                <div class="col-md-6">
                    <ul class="custom-list">
                        <li><strong>Efisiensi Menyeluruh:</strong> Otomatisasi registrasi, resep, dan billing, menghemat waktu dan mengurangi pekerjaan manual berulang.</li>
                        <li><strong>Akurasi & Keselamatan:</strong> Meminimalisasi kesalahan pemberian obat dan menjaga integritas rekam medis.</li>
                        <li><strong>Aksesibilitas Tinggi:</strong> Platform dapat diakses melalui ragam perangkat sesuai kebutuhan operasional klinis.</li>
                    </ul>
                </div>
                <div class="col-md-6">
                    <ul class="custom-list">
                        <li><strong>Transparansi Mutlak:</strong> Mempermudah pelacakan audit stok farmasi dan keuangan.</li>
                        <li><strong>Pengambilan Keputusan:</strong> Tersedianya Dashboard Analitik untuk memantau indikator kinerja fasilitas kesehatan.</li>
                        <li><strong>Kesiapan Regulasi:</strong> Fondasi data untuk integrasi BPJS/JKN dan SATUSEHAT Kemenkes disiapkan secara bertahap.</li>
                    </ul>
                </div>
            </div>

            <h3 class="subsection-title"><i class="fas fa-gem"></i> Nilai Strategis untuk Calon Mitra</h3>
            <div class="value-grid">
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-bolt"></i></div>
                    <div class="value-title">Layanan Lebih Cepat</div>
                    <p class="value-desc">Proses pendaftaran, pemeriksaan, dispensing obat, dan pembayaran dapat dipangkas melalui alur digital yang lebih singkat.</p>
                </div>
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-database"></i></div>
                    <div class="value-title">Data Lebih Tertib</div>
                    <p class="value-desc">Rekam medis, farmasi, dan billing menggunakan referensi data yang sama sehingga mengurangi duplikasi dan konflik data.</p>
                </div>
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-chart-line"></i></div>
                    <div class="value-title">Pimpinan Lebih Siap</div>
                    <p class="value-desc">Informasi operasional tersaji dalam dashboard sehingga evaluasi okupansi, pendapatan, dan diagnosa dapat dilakukan lebih objektif.</p>
                </div>
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-handshake-angle"></i></div>
                    <div class="value-title">Kemitraan Fleksibel</div>
                    <p class="value-desc">Skema implementasi, infrastruktur, dan pembiayaan (3 opsi) dapat disesuaikan dengan kebutuhan dan skala fasilitas kesehatan.</p>
                </div>
            </div>

            <div class="page-break"></div>

            <h2 class="section-title">III. Ekosistem eMedic</h2>
            <p>eMedic mengusung konsep <i>All-in-One Platform</i> untuk layanan kesehatan, dengan dua pola pemanfaatan:</p>

            <div class="row text-center mt-4 mb-5">
                <div class="col-md-6">
                    <div class="p-4 border rounded-4 bg-light h-100 shadow-sm">
                        <i class="fas fa-graduation-cap fa-3x text-primary mb-3"></i>
                        <h4 class="fw-bold">Terintegrasi dengan Institusi Pendidikan</h4>
                        <p class="small text-muted mb-0">Bagi Universitas (eCampus), Sekolah (eSchool), atau Pondok Pesantren (ePesantren) yang memiliki klinik/poliklinik/rumah sakit sendiri, eMedic terhubung langsung dengan data mahasiswa, siswa, santri, dan pegawai yang sudah ada.</p>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="p-4 border rounded-4 bg-light h-100 shadow-sm border-bottom border-success border-4">
                        <i class="fas fa-hospital fa-3x text-success mb-3"></i>
                        <h4 class="fw-bold">Berdiri Sendiri (Standalone)</h4>
                        <p class="small text-muted mb-0">Rumah Sakit, Klinik, Puskesmas, atau layanan fasilitas kesehatan lain yang tidak bernaung di bawah institusi pendidikan dapat mengimplementasikan eMedic secara independen.</p>
                    </div>
                </div>
            </div>

            <div class="highlight-box">
                <h5 class="fw-bold text-dark"><i class="fas fa-sitemap text-primary me-2"></i> Prinsip Desain Ekosistem</h5>
                <p class="mb-0">eMedic tidak hanya berperan sebagai aplikasi pencatatan, tetapi sebagai <strong>tulang punggung operasional digital</strong> yang menghubungkan layanan registrasi, rekam medis, farmasi, gudang, billing, dan pelaporan strategis dalam satu ekosistem kerja yang konsisten dan teraudit penuh.</p>
            </div>

            <div class="value-grid">
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-user-lock"></i></div>
                    <div class="value-title">Hak Akses Bertingkat</div>
                    <p class="value-desc">Dokter, perawat, farmasis, kasir, dan petugas gudang hanya melihat menu dan data sesuai kewenangan.</p>
                </div>
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-mobile-screen-button"></i></div>
                    <div class="value-title">Akses Multi-Perangkat</div>
                    <p class="value-desc">Antarmuka web responsif mendukung akses dari komputer, tablet, dan ponsel sesuai kebutuhan operasional klinis.</p>
                </div>
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-cloud-arrow-up"></i></div>
                    <div class="value-title">Siap Integrasi</div>
                    <p class="value-desc">Dirancang untuk terhubung dengan BPJS/JKN dan SATUSEHAT Kemenkes secara bertahap di masa mendatang.</p>
                </div>
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-scale-balanced"></i></div>
                    <div class="value-title">Tata Kelola Tertib</div>
                    <p class="value-desc">Mendukung jejak audit penuh, validasi berlapis, dan rekam historis aktivitas untuk kebutuhan pengawasan internal.</p>
                </div>
            </div>

            <h3 class="subsection-title"><i class="fas fa-network-wired"></i> Fleksibilitas Infrastruktur (Solusi Dual Model)</h3>
            <p>Sistem ini dirancang adaptif untuk merespon kebijakan keamanan data kesehatan fasilitas Anda melalui tiga skema pemasangan (<i>deployment</i>):</p>
            <ul class="custom-list">
                <li><strong>Model Cloud (Disarankan):</strong> Sistem di-<i>host</i> di server awan kami. Fasilitas kesehatan tidak perlu investasi perangkat keras server, cukup dengan koneksi internet.</li>
                <li><strong>Model On-Premise (Konvensional):</strong> Sistem dipasang murni pada server internal fasilitas kesehatan (Intranet). Cocok bagi fasilitas dengan kebijakan privasi data rekam medis yang tertutup.</li>
                <li><strong>Model Hybrid:</strong> Menggabungkan keandalan server lokal untuk menyimpan arsip rekam medis, namun antarmukanya tetap dapat diakses via internet.</li>
            </ul>

            <div class="page-break"></div>

            <h2 class="section-title">IV. Rincian Modul & Fungsionalitas Sistem</h2>
            <p>Modul eMedic memiliki cakupan yang luas, membentang dari registrasi pasien hingga tata kelola farmasi dan dashboard analitik.</p>

            <div class="metric-strip">
                <div class="metric"><strong>Registrasi</strong><span>Rawat jalan, rawat inap, UGD</span></div>
                <div class="metric"><strong>Rekam Medis</strong><span>Diagnosa ber-ICD-10</span></div>
                <div class="metric"><strong>Farmasi</strong><span>Resep, racikan, kadaluarsa/FEFO</span></div>
                <div class="metric"><strong>Billing</strong><span>Tagihan otomatis, dashboard</span></div>
            </div>

            <h3 class="subsection-title"><i class="fas fa-notes-medical"></i> A. Registrasi & Rekam Medis</h3>
            <ul class="dash-list">
                <li><strong>Registrasi Rawat Jalan:</strong> pendaftaran, antrian otomatis, pemilihan poli dan dokter sesuai jadwal.</li>
                <li><strong>Registrasi Rawat Inap:</strong> admisi, pemilihan kamar/tempat tidur/kelas perawatan, deposit awal, visite dokter harian, hingga pemulangan dan pelunasan.</li>
                <li><strong>Registrasi UGD:</strong> registrasi dan transaksi medis dilakukan bersamaan dalam satu langkah agar penanganan pasien tidak tertunda administrasi.</li>
                <li><strong>Rekam Medis & Diagnosa:</strong> pencatatan keluhan, pemeriksaan, dan diagnosa dengan pengkodean standar ICD-10.</li>
            </ul>

            <h3 class="subsection-title"><i class="fas fa-mortar-pestle"></i> B. Farmasi & Racikan</h3>
            <ul class="dash-list">
                <li><strong>Resep Digital:</strong> ditulis langsung oleh dokter saat pemeriksaan, diteruskan ke farmasi tanpa kertas.</li>
                <li><strong>Racikan / Kompounding:</strong> master komposisi baku untuk memastikan mutu racikan tetap konsisten dan dapat ditelusuri.</li>
                <li><strong>Stok Otomatis:</strong> sistem ledger stok memperbarui persediaan real-time saat transaksi disimpan.</li>
            </ul>

            <h3 class="subsection-title"><i class="fas fa-warehouse"></i> C. Gudang, Rantai Pasok & Kadaluarsa</h3>
            <ul class="dash-list">
                <li><strong>Rantai Pasok End-to-End:</strong> saldo awal, permintaan pembelian, pesanan ke pemasok, penerimaan barang (dengan pencatatan batch), transfer antar gudang, pemakaian, dan koreksi stok (opname).</li>
                <li><strong>Hierarki Gudang Pusat-Cabang:</strong> stok dapat dipantau dan dikonsolidasi dari gudang pusat ke seluruh apotek/cabang.</li>
                <li><strong>Manajemen Kadaluarsa (FEFO):</strong> setiap penerimaan tercatat per-batch dengan tanggal kadaluarsa; dashboard peringatan dini menyoroti item yang mendekati kadaluarsa; prinsip <i>First-Expired-First-Out</i> membantu petugas mengeluarkan stok yang lebih dulu kadaluarsa.</li>
            </ul>

            <h3 class="subsection-title"><i class="fas fa-file-invoice-dollar"></i> D. Transaksi, Billing & Kasir</h3>
            <ul class="dash-list">
                <li><strong>Tagihan Terpadu:</strong> biaya obat, tindakan medis, alat medis, dan paket layanan otomatis tergabung dalam satu tagihan pasien.</li>
                <li><strong>Harga & HPP:</strong> HPP dihitung otomatis dari rollup harga beli riil; tarif khusus dapat diterapkan per kelas perawatan atau per payer/asuransi (Asuransi tetap sebagai master payer, BPJS menjadi salah satu tipe payer).</li>
                <li><strong>Pembayaran:</strong> mendukung tunai, non-tunai, dan deposit dengan struk/kwitansi tercetak otomatis.</li>
            </ul>

            <h3 class="subsection-title"><i class="fas fa-chart-pie"></i> E. Dashboard Analitik & Laporan</h3>
            <ul class="dash-list">
                <li><strong>7 Dashboard Operasional:</strong> ringkasan pendaftaran, kunjungan rawat jalan (mingguan/bulanan), pendapatan, 10 diagnosa terbanyak, okupansi tempat tidur, dan kadaluarsa farmasi.</li>
                <li><strong>60+ Laporan Siap Pakai:</strong> mencakup laporan pasien & rekam medis, kunjungan (termasuk format resmi RL5/RL18/RL21), rawat inap, kasir, inventory, dan pengadaan.</li>
            </ul>

            <h3 class="subsection-title"><i class="fas fa-shield-halved"></i> F. Keamanan Data & Jejak Audit</h3>
            <ul class="dash-list">
                <li><strong>Jejak Audit Menyeluruh:</strong> setiap perubahan stok, resep, tarif, dan rekam medis tercatat: siapa, kapan, dan apa yang diubah.</li>
                <li><strong>Hak Akses Berjenjang:</strong> dokter, perawat, farmasis, kasir, petugas gudang, dan admin masing-masing memiliki akses sesuai peran.</li>
                <li><strong>Data Sepenuhnya Milik Fasilitas Kesehatan:</strong> seluruh data tersimpan aman dan menjadi milik penuh fasilitas kesehatan, bukan pihak ketiga.</li>
            </ul>

            <div class="highlight-box bg-primary bg-opacity-10">
                <h5 class="fw-bold text-primary"><i class="fas fa-plug-circle-check me-2"></i> Keunggulan Integrasi Modul</h5>
                <p class="mb-0">Kekuatan utama eMedic terletak pada keterhubungan antar modul. Data pasien dari registrasi dapat langsung menjadi dasar rekam medis, resep, tagihan, hingga laporan manajemen — sehingga fasilitas kesehatan memperoleh alur kerja yang lebih hemat waktu dan konsisten.</p>
            </div>

            <div class="page-break"></div>

            <h2 class="section-title">V. Roadmap Integrasi BPJS/JKN & SATUSEHAT</h2>
            <p>
                Fondasi data untuk integrasi eksternal disiapkan secara bertahap dan aditif, tanpa mengganggu operasional yang sedang berjalan:
            </p>

            <div class="timeline">
                <div class="timeline-step">
                    <div class="timeline-number">01</div>
                    <div class="timeline-content">
                        <h5>Fase 1: Fondasi Data <span class="badge bg-warning text-dark ms-2">Sedang Dibangun</span></h5>
                        <p>NIK, nomor kartu BPJS, dan data kepesertaan pasien mulai disiapkan sebagai fondasi.</p>
                    </div>
                </div>
                <div class="timeline-step">
                    <div class="timeline-number">02</div>
                    <div class="timeline-content">
                        <h5>Fase 2: BPJS Eligibilitas</h5>
                        <p>Integrasi VClaim, PCare, dan penerbitan SEP otomatis.</p>
                    </div>
                </div>
                <div class="timeline-step">
                    <div class="timeline-number">03</div>
                    <div class="timeline-content">
                        <h5>Fase 3: SATUSEHAT</h5>
                        <p>Integrasi rekam medis elektronik nasional (Kemenkes).</p>
                    </div>
                </div>
                <div class="timeline-step">
                    <div class="timeline-number">04</div>
                    <div class="timeline-content">
                        <h5>Fase 4: Klaim INA-CBG</h5>
                        <p>Pengajuan dan pelacakan klaim otomatis ke BPJS.</p>
                    </div>
                </div>
            </div>

            <div class="highlight-box">
                <h5 class="fw-bold text-dark"><i class="fas fa-lightbulb text-warning me-2"></i> Prinsip Pengembangan Bertahap</h5>
                <p class="mb-0">Setiap fase dikembangkan secara aditif (menambah, bukan mengubah struktur yang sudah berjalan) sehingga operasional harian tidak terganggu. Asuransi tetap menjadi master payer tunggal; BPJS diperlakukan sebagai salah satu tipe payer di dalamnya, sehingga fasilitas kesehatan yang sudah melayani pasien umum maupun asuransi swasta tidak perlu mengubah alur kerja yang sudah berjalan.</p>
            </div>

            <h2 class="section-title">VI. Metodologi Implementasi & Pendampingan</h2>
            <p>Implementasi eMedic dilakukan secara bertahap agar proses migrasi data, pelatihan tenaga medis, konfigurasi, dan adopsi pengguna dapat berjalan terkendali.</p>

            <div class="timeline">
                <div class="timeline-step">
                    <div class="timeline-number">01</div>
                    <div class="timeline-content">
                        <h5>Assessment Kebutuhan & Pemetaan Alur Klinis</h5>
                        <p>Mengidentifikasi kondisi eksisting, alur pendaftaran-pemeriksaan-farmasi-kasir, dan prioritas modul yang akan diaktifkan.</p>
                    </div>
                </div>
                <div class="timeline-step">
                    <div class="timeline-number">02</div>
                    <div class="timeline-content">
                        <h5>Konfigurasi Sistem & Migrasi Data Awal</h5>
                        <p>Menyiapkan master pasien, dokter, item obat, tarif, poli/kelas perawatan, dan hak akses pengguna.</p>
                    </div>
                </div>
                <div class="timeline-step">
                    <div class="timeline-number">03</div>
                    <div class="timeline-content">
                        <h5>Pelatihan Tenaga Medis & Pengguna Kunci</h5>
                        <p>Memberikan pendampingan penggunaan modul kepada dokter, perawat, farmasis, dan kasir.</p>
                    </div>
                </div>
                <div class="timeline-step">
                    <div class="timeline-number">04</div>
                    <div class="timeline-content">
                        <h5>Go-Live Bertahap & Monitoring</h5>
                        <p>Sistem mulai digunakan pada modul prioritas, dilanjutkan pemantauan dan dukungan teknis sesuai kebutuhan.</p>
                    </div>
                </div>
            </div>

            <h3 class="subsection-title"><i class="fas fa-people-arrows"></i> Ruang Lingkup Pendampingan</h3>
            <div class="service-scope">
                <div class="scope-item">
                    <h5><i class="fas fa-sliders me-2"></i> Konfigurasi</h5>
                    <p class="value-desc">Pengaturan parameter sistem, master data klinis, hak akses, dan kebutuhan awal operasional.</p>
                </div>
                <div class="scope-item">
                    <h5><i class="fas fa-chalkboard-user me-2"></i> Pelatihan</h5>
                    <p class="value-desc">Transfer knowledge kepada dokter, perawat, farmasis, dan kasir agar mampu menjalankan sistem secara mandiri.</p>
                </div>
                <div class="scope-item">
                    <h5><i class="fas fa-headset me-2"></i> Dukungan Teknis</h5>
                    <p class="value-desc">Bantuan teknis selama proses implementasi, penyesuaian alur klinis, dan pendampingan saat go-live.</p>
                </div>
            </div>

            <div class="page-break"></div>

            <h2 class="section-title">VII. Skema Investasi</h2>
            <p>
                Kami menyediakan <strong>tiga opsi</strong> skema kemitraan untuk modul eMedic, agar fasilitas kesehatan &mdash; mulai dari Klinik hingga Rumah Sakit Kelas A &mdash; dapat memilih model yang paling sesuai dengan kondisi anggaran dan tahap pertumbuhannya. Seluruh angka pada bagian ini bersifat sebagai <strong>titik awal diskusi</strong> dan sepenuhnya terbuka untuk dinegosiasikan; prioritas kami adalah agar sistem dapat segera terealisasi dan digunakan.
            </p>
            <div class="highlight-box bg-warning bg-opacity-10" style="border-left-color: var(--warning-color);">
                <h5 class="fw-bold text-dark"><i class="fas fa-coins text-warning me-2"></i> Prinsip Investasi yang Fleksibel</h5>
                <p class="mb-0">Skema pembiayaan disusun agar fasilitas kesehatan dapat memilih: <strong>kepemilikan penuh</strong> melalui Beli Putus, <strong>anggaran tetap yang mudah diproyeksikan</strong> melalui Langganan Bulanan, atau <strong>tanpa biaya di muka sama sekali</strong> melalui Fee Layanan Sistem per Transaksi.</p>
            </div>

            <h3 class="subsection-title"><i class="fas fa-money-bill-wave"></i> A. Beli Putus (Lisensi Permanen)</h3>
            <p>Sistem menjadi aset milik fasilitas kesehatan untuk selamanya melalui satu kali pembayaran. Harga sudah mencakup lisensi seluruh modul paket, implementasi dasar, dan dukungan teknis tahun pertama.</p>
            <div class="table-modern mb-3">
                <table class="table table-bordered align-middle mb-0">
                    <thead>
                        <tr>
                            <th><%= Common.getBahasaConfig("Jenis Fasilitas") %></th>
                            <th class="text-end"><%= Common.getBahasaConfig("Harga Beli Putus (Mulai Dari)") %></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr><td><strong>Klinik</strong></td><td class="text-end">Rp 100.000.000</td></tr>
                        <tr><td>Puskesmas Nonrawat Inap Dasar</td><td class="text-end">Rp 150.000.000</td></tr>
                        <tr><td>Puskesmas Nonrawat Inap Lengkap / 24 Jam</td><td class="text-end">Rp 225.000.000</td></tr>
                        <tr><td>Puskesmas Rawat Inap</td><td class="text-end">Rp 325.000.000</td></tr>
                        <tr><td>Puskesmas Rawat Inap Terpencil</td><td class="text-end">Rp 425.000.000</td></tr>
                        <tr><td>Puskesmas Sangat Terpencil / Offline-First</td><td class="text-end">Rp 500.000.000</td></tr>
                        <tr><td><strong>Rumah Sakit Pratama</strong></td><td class="text-end">Rp 250.000.000</td></tr>
                        <tr><td><strong>Rumah Sakit Kelas D</strong></td><td class="text-end">Rp 400.000.000</td></tr>
                        <tr><td><strong>Rumah Sakit Kelas C</strong></td><td class="text-end">Rp 750.000.000</td></tr>
                        <tr><td><strong>Rumah Sakit Kelas B</strong></td><td class="text-end">Rp 1.400.000.000</td></tr>
                        <tr><td><strong>Rumah Sakit Kelas A</strong></td><td class="text-end">Rp 2.750.000.000</td></tr>
                    </tbody>
                </table>
            </div>
            <p class="small text-muted mb-4"><i class="fas fa-circle-info me-1"></i> Harga berlaku untuk <strong>satu lokasi fasilitas</strong>. Infrastruktur server, migrasi data kompleks, integrasi alat medis, kustomisasi khusus, dan penempatan tenaga pendamping di lokasi (onsite) diperhitungkan terpisah.</p>

            <h3 class="subsection-title"><i class="fas fa-calendar-check"></i> B. Langganan Bulanan</h3>
            <p>Tanpa investasi awal besar. Tarif disusun setara kurang lebih 2,5% dari nilai Beli Putus setiap bulan (impas dalam kurang lebih 40 bulan).</p>
            <div class="table-modern mb-3">
                <table class="table table-bordered align-middle mb-0">
                    <thead>
                        <tr>
                            <th><%= Common.getBahasaConfig("Jenis Fasilitas") %></th>
                            <th class="text-end"><%= Common.getBahasaConfig("Biaya Langganan / Bulan") %></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr><td><strong>Klinik</strong></td><td class="text-end">Rp 2.500.000</td></tr>
                        <tr><td>Puskesmas Nonrawat Inap Dasar</td><td class="text-end">Rp 4.000.000</td></tr>
                        <tr><td>Puskesmas Nonrawat Inap Lengkap / 24 Jam</td><td class="text-end">Rp 5.500.000</td></tr>
                        <tr><td>Puskesmas Rawat Inap</td><td class="text-end">Rp 8.000.000</td></tr>
                        <tr><td>Puskesmas Rawat Inap Terpencil</td><td class="text-end">Rp 10.500.000</td></tr>
                        <tr><td>Puskesmas Sangat Terpencil / Offline-First</td><td class="text-end">Rp 12.500.000</td></tr>
                        <tr><td><strong>Rumah Sakit Pratama</strong></td><td class="text-end">Rp 6.500.000</td></tr>
                        <tr><td><strong>Rumah Sakit Kelas D</strong></td><td class="text-end">Rp 10.000.000</td></tr>
                        <tr><td><strong>Rumah Sakit Kelas C</strong></td><td class="text-end">Rp 20.000.000</td></tr>
                        <tr><td><strong>Rumah Sakit Kelas B</strong></td><td class="text-end">Rp 35.000.000</td></tr>
                        <tr><td><strong>Rumah Sakit Kelas A</strong></td><td class="text-end">Rp 70.000.000</td></tr>
                    </tbody>
                </table>
            </div>
            <p class="small text-muted mb-4"><i class="fas fa-circle-info me-1"></i> Sudah termasuk lisensi seluruh modul paket, hosting/cloud standar, backup berkala, pemeliharaan aplikasi, pembaruan minor, dukungan jarak jauh, dan pembaruan integrasi standar untuk satu lokasi fasilitas. Perangkat, server lokal, perjalanan onsite, integrasi alat medis, penyimpanan PACS, migrasi besar, dan pengembangan khusus ditagihkan terpisah.</p>

            <h3 class="subsection-title"><i class="fas fa-chart-pie"></i> C. Fee Layanan Sistem per Transaksi</h3>
            <p>Skema tanpa biaya di muka sama sekali. Kami sengaja tidak menggunakan persentase besar dari total omzet (misalnya persentase langsung dari seluruh nilai transaksi), karena dapat menghasilkan tagihan yang jauh lebih besar daripada langganan bulanan dan memberatkan fasilitas kesehatan. Skema berikut dirancang lebih proporsional dan mudah diaudit.</p>

            <div class="value-grid" style="grid-template-columns: repeat(2, 1fr);">
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-clinic-medical"></i></div>
                    <div class="value-title">Skema Klinik</div>
                    <p class="value-desc mb-2"><strong>0,5%</strong> dari pendapatan bersih tindakan (setelah dikurangi jasa dokter, bahan medis habis pakai, diskon, dan refund) <strong>+ 0,25%</strong> dari penjualan bersih obat (setelah dikurangi diskon, retur, pajak).</p>
                    <p class="value-desc mb-2">Batas aman: minimum <strong>Rp 2.500.000</strong>, maksimum <strong>Rp 7.500.000</strong> per bulan per lokasi.</p>
                    <p class="value-desc mb-0">Alternatif sederhana: <strong>Rp 500 per registrasi</strong> kunjungan berhasil (batas minimum/maksimum sama).</p>
                </div>
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-hospital"></i></div>
                    <div class="value-title">Skema Puskesmas &amp; Rumah Sakit</div>
                    <p class="value-desc mb-0">Dihitung dari <strong>fee per registrasi kunjungan berhasil</strong>, dengan tagihan minimum dan maksimum bulanan sebagai jaring pengaman &mdash; lihat tabel di bawah.</p>
                </div>
            </div>

            <div class="table-modern mb-3">
                <table class="table table-bordered align-middle mb-0">
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
                        <tr><td>Rumah Sakit Kelas B</td><td>Dasar Rp 20.000.000 + Rp 2.000/registrasi</td><td>&mdash;</td><td>Rp 100.000.000</td></tr>
                        <tr><td>Rumah Sakit Kelas A</td><td>Dasar Rp 40.000.000 + Rp 3.500/registrasi</td><td>&mdash;</td><td>Rp 200.000.000</td></tr>
                    </tbody>
                </table>
            </div>

            <div class="highlight-box">
                <h5 class="fw-bold text-dark"><i class="fas fa-clipboard-question text-primary me-2"></i> Yang Dimaksud "Satu Transaksi Berbayar"</h5>
                <p class="mb-0">Satu transaksi yang dihitung adalah <strong>satu registrasi kunjungan pasien yang berhasil, memiliki nomor kunjungan, dan sudah mendapatkan pelayanan</strong>. Tidak dihitung: pendaftaran batal, percobaan/uji coba, duplikasi, koreksi administrasi, membuka aplikasi tanpa mendaftar, dibatalkan petugas, data pelatihan/dummy, dan sinkronisasi ulang. Rawat inap hanya dihitung satu kali pada saat admisi, bukan setiap hari rawat.</p>
            </div>

            <div class="highlight-box bg-primary bg-opacity-10 mt-3">
                <h5 class="fw-bold text-primary"><i class="fas fa-shield-heart me-2"></i> Prinsip Kepatuhan &amp; Etika</h5>
                <ul class="dash-list mb-0">
                    <li><strong>Bukan pungutan kepada pasien:</strong> fee merupakan hubungan bisnis (B2B) antara fasilitas kesehatan dan penyedia sistem, dibayar dari anggaran/pendapatan fasilitas &mdash; bukan pungutan otomatis kepada pasien, terutama peserta JKN/BPJS.</li>
                    <li><strong>Tidak memengaruhi keputusan medis:</strong> fee tidak boleh menjadi dasar penambahan tindakan, pemeriksaan, atau obat; informasi fee hanya dapat diakses manajemen dan keuangan, tidak ditampilkan kepada tenaga medis.</li>
                    <li><strong>Selaras dengan regulasi tarif JKN</strong> sebagaimana diatur oleh Kementerian Kesehatan.</li>
                    <li><strong>Selaras dengan perlindungan data pasien</strong> sesuai Undang-Undang Pelindungan Data Pribadi dan ketentuan rekam medis yang berlaku &mdash; fasilitas kesehatan berkedudukan sebagai pengendali data, penyedia sistem sebagai pemroses data.</li>
                </ul>
            </div>

            <div class="highlight-box bg-success bg-opacity-10 border-success mb-5 mt-3">
                <h5 class="fw-bold text-success"><i class="fas fa-handshake me-2"></i> Keterbukaan Negosiasi Kemitraan</h5>
                <p class="mb-0 text-justify small">Seluruh nilai investasi, tarif langganan, maupun fee bagi hasil yang tercantum di atas merupakan penawaran awal yang <strong>bersifat ilustratif dan sepenuhnya dapat dinegosiasikan</strong> sesuai volume transaksi dan kesepakatan akhir. Kami mengutamakan tercapainya kesepakatan dan cepatnya sistem dapat direalisasikan, dibandingkan kekakuan pada satu angka tertentu.</p>
            </div>

            <h2 class="section-title">VIII. Penutup & Ajakan Kerja Sama</h2>
            <div class="highlight-box">
                <h5 class="fw-bold text-dark"><i class="fas fa-handshake-angle text-primary me-2"></i> Komitmen Kemitraan</h5>
                <p class="mb-0">Kami tidak hanya menawarkan aplikasi, tetapi menawarkan pendampingan transformasi proses kerja klinis. Dengan kerja sama yang terarah, eMedic diharapkan menjadi fondasi digital yang memperkuat mutu layanan, keselamatan pasien, dan reputasi fasilitas kesehatan dalam jangka panjang.</p>
            </div>

            <div class="highlight-box bg-primary bg-opacity-10 mt-3">
                <h5 class="fw-bold text-dark"><i class="fas fa-desktop text-primary me-2"></i> Coba Demo eMedic Secara Langsung</h5>
                <p class="mb-2">Sebelum mengambil keputusan, calon mitra dapat langsung menjelajahi tampilan dan alur kerja eMedic secara mandiri melalui akun demo berikut:</p>
                <p class="mb-1">Tautan Demo: <a href="https://apps.emedik.id" target="_blank"><strong>https://apps.emedik.id</strong></a></p>
                <p class="mb-1">Username: <strong>demo</strong></p>
                <p class="mb-0">Password: <strong>demo123</strong></p>
            </div>

            <div class="text-center mt-3">
                <i class="fas fa-hospital fa-4x text-success mb-4"></i>
                <h2 class="fw-bold mb-4 text-dark">Melangkah ke Masa Depan Layanan Kesehatan Digital</h2>
                <p class="text-muted fs-5 mb-5 mx-auto" style="max-width: 850px;">
                    eMedic dibangun di atas ekosistem Enterprise Education yang telah teruji dan dipercaya oleh puluhan institusi pendidikan. Modul eMedic menghadirkan pendekatan yang sama untuk layanan kesehatan: satu platform, terintegrasi, teraudit, dan siap berkembang bersama regulasi nasional.
                </p>
                <p class="text-muted mb-4 fs-5">
                    Silakan jalin komunikasi dengan representatif konsultan kami untuk melakukan penjadwalan presentasi, diskusi teknis konfigurasi migrasi, atau sesi negosiasi pendalaman kerja sama.
                </p>

                <% if(paramPresentBy != null && !paramPresentBy.trim().isEmpty()) { %>
                    <div class="d-inline-block p-5 mt-3 bg-white rounded-4 border border-secondary shadow text-start" style="min-width: 400px;">
                        <h5 class="fw-bold text-primary mb-4 text-center"><i class="fas fa-user-tie me-2"></i> <%= Common.getBahasaConfig("Konsultan Resmi Fasilitas Kesehatan Anda") %></h5>
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
                        <h4 class="fw-bold text-primary mb-4 text-center">Tim Pusat Enterprise Education - Modul eMedic</h4>
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
                    </div>
                <% } %>
            </div>

            <div class="signature-line">
                <div class="signature-box">
                    <div class="meta-label">Diajukan oleh</div>
                    <div class="meta-value mt-2"><%=(paramPresentBy != null && !paramPresentBy.trim().isEmpty()) ? paramPresentBy : "Tim eMedic - Enterprise Education"%></div>
                    <div class="text-muted small mt-5 pt-4">Tanda tangan dan nama jelas</div>
                </div>
                <div class="signature-box">
                    <div class="meta-label">Diterima / Ditinjau oleh</div>
                    <div class="meta-value mt-2"><%=(paramClient != null && !paramClient.trim().isEmpty()) ? paramClient : "Calon Mitra Fasilitas Kesehatan"%></div>
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

    <script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
    <jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp" />
</body>
</html>
