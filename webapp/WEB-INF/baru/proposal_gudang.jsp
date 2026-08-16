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
String nomorProposal = (paramNomorProposal != null && !paramNomorProposal.trim().isEmpty()) ? paramNomorProposal : "EE/PROP-GUDANG/" + Calendar.getInstance().get(Calendar.YEAR);

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
    <title><%= Common.getBahasaConfig("Proposal Modul Gudang, POS & Ritel Terpadu (eGudang)") %> | <%=judul%></title>

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

        /* bottom:7.5rem supaya lolos dari tombol "Tanya Jawab" (bantuan_button.jsp: right:16px; bottom:62px). */
        .btn-print { position: fixed; bottom: 7.5rem; right: 2rem; z-index: 1000; border-radius: 50px; padding: 1rem 2rem; box-shadow: 0 10px 20px rgba(2, 132, 199, 0.3); font-size: 1.1rem;}

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
            <div class="proposal-watermark">EGUDANG</div>

            <div class="cover-page">
                <img src="<%=logo_PerguruanTinggi%>" alt="Logo" class="cover-logo">
                <div class="cover-badge"><i class="fas fa-shield-halved"></i> Dokumen Penawaran Resmi Siap Cetak</div>
                <h1 class="cover-title"><i class="fas fa-warehouse me-3"></i><%= Common.getBahasaConfig("Proposal Penawaran Kerjasama") %></h1>
                <h2 class="cover-subtitle"><%= Common.getBahasaConfig("Implementasi Sistem Gudang, Kasir (POS) & Manajemen Bisnis Ritel Terpadu (eGudang)") %></h2>
                <p class="cover-intro">
                    Solusi <strong>eGudang</strong> dirancang untuk membantu kantin, koperasi, unit usaha kampus/sekolah/pesantren, maupun bisnis ritel mandiri membangun ekosistem digital terpadu: kasir (POS) yang tetap berjalan meski koneksi internet terputus, stok gudang yang akurat dan teraudit penuh, perhitungan HPP yang presisi dan otomatis, serta pembukuan akuntansi yang selalu sinkron dengan transaksi lapangan.
                </p>
                <h4 class="fw-bold text-primary mt-4 px-5 py-3 bg-light border border-primary border-opacity-25 rounded-pill d-inline-block shadow-sm">
                    <i class="fas fa-boxes-stacked me-2"></i> <%=judul%>
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
                        <div class="meta-value">Sistem Gudang, POS & Manajemen Ritel Terpadu</div>
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
                <h2 class="fw-bold mb-3"><i class="fas fa-rocket me-2"></i> Proposal Transformasi Digital Operasional Gudang & Ritel</h2>
                <p class="mb-3">
                    Proposal ini disusun sebagai dasar kerja sama implementasi <strong>eGudang</strong>, yaitu modul yang menyatukan kasir (POS) multi-outlet, manajemen gudang berjenjang, perhitungan HPP otomatis, pembukuan akuntansi, dan pengadaan barang dalam satu sistem yang saling terhubung.
                </p>
                <p class="mb-0">
                    Fokus utama penawaran ini adalah membantu unit usaha membangun operasional yang lebih <strong>cepat, tertib, akuntabel, mudah diaudit, dan tahan gangguan koneksi</strong> — baik sebagai unit usaha (kantin, koperasi, toko kampus) yang bernaung di bawah institusi pendidikan, maupun sebagai bisnis ritel, jaringan koperasi, atau gudang yang berdiri sendiri.
                </p>
            </div>

            <div class="metric-strip">
                <div class="metric"><strong>Offline-Ready</strong><span>Kasir tetap jalan tanpa internet</span></div>
                <div class="metric"><strong>Real-Time</strong><span>Validasi stok anti dobel-jual</span></div>
                <div class="metric"><strong>Otomatis</strong><span>HPP dan posting jurnal akuntansi</span></div>
                <div class="metric"><strong>Teraudit</strong><span>Jejak setiap pergerakan stok</span></div>
            </div>

            <div class="toc-box mb-5">
                <h5 class="fw-bold text-primary mb-3"><i class="fas fa-list-check me-2"></i> Struktur Proposal</h5>
                <ol class="toc-list">
                    <li>Pendahuluan dan latar belakang kebutuhan digitalisasi operasional gudang & ritel.</li>
                    <li>Visi, misi, tujuan, manfaat, dan nilai strategis implementasi.</li>
                    <li>Ekosistem eGudang: terintegrasi dengan eCampus/eSchool/ePesantren maupun berdiri sendiri.</li>
                    <li>Rincian modul serta fungsionalitas sistem yang telah tersedia.</li>
                    <li>Roadmap pengembangan: Ekspedisi, Investor (bagi hasil), dan Kepegawaian Outlet.</li>
                    <li>Metodologi implementasi dan layanan pendampingan.</li>
                    <li>Skema harga (langganan POS per Outlet + Paket Modul Pusat sesuai kebutuhan).</li>
                </ol>
            </div>

            <h2 class="section-title">I. Pendahuluan & Latar Belakang</h2>
            <p>
                Unit usaha ritel — mulai dari kantin dan koperasi sekolah/kampus hingga jaringan toko dan gudang mandiri — saat ini menghadapi tantangan yang kompleks dalam mengelola stok, transaksi kasir, dan pembukuan secara efektif. Proses yang masih bersifat manual atau semi-manual seringkali memakan waktu, rentan terhadap selisih stok (<i>human error</i>), dan menyulitkan pimpinan memperoleh gambaran keuangan yang akurat secara cepat.
            </p>
            <p>
                Dua masalah klasik yang paling sering ditemui adalah: kasir yang bekerja secara bersamaan di outlet berbeda dapat menjual stok yang sama tanpa saling tahu (<i>overselling</i>), dan perhitungan HPP (Harga Pokok Penjualan) untuk produk berbasis resep yang masih diketik manual — sehingga rawan salah hitung dan tidak sinkron dengan pembukuan akuntansi.
            </p>
            <p>
                Oleh karena itu, kami hadir menawarkan <strong>eGudang</strong>, sebuah solusi Sistem Gudang, POS, dan Manajemen Ritel <i>end-to-end</i> terpadu berbasis web. Modul ini merupakan bagian dari ekosistem <strong>Enterprise Education</strong> yang lebih luas, sehingga jika Universitas, Sekolah, atau Pondok Pesantren Anda memiliki kantin, koperasi, toko kampus, gudang, atau unit usaha yang didanai investor, modul ini dapat langsung terintegrasi dengan data mahasiswa/siswa/santri/pegawai yang sudah ada — tanpa perlu membangun sistem terpisah atau login ganda.
            </p>

            <div class="highlight-box">
                <h5 class="fw-bold text-dark"><i class="fas fa-exclamation-triangle text-warning me-2"></i> Identifikasi Masalah & Tantangan Operasional Ritel:</h5>
                <ul class="dash-list mt-3 mb-0 text-dark">
                    <li><strong>Stok Tidak Akurat:</strong> Selisih stok fisik dan catatan sering baru diketahui saat opname, bukan saat kejadian.</li>
                    <li><strong>Overselling Kasir Ganda:</strong> Dua kasir di outlet berbeda dapat menjual stok terakhir yang sama secara bersamaan.</li>
                    <li><strong>HPP Ditebak, Bukan Dihitung:</strong> HPP produk resep sering diketik manual, tidak mengikuti rollup harga bahan baku yang sebenarnya.</li>
                    <li><strong>Jurnal Terpisah dari Transaksi:</strong> Posting akuntansi tidak selalu sinkron dan mudah ditelusuri ke dokumen sumbernya.</li>
                </ul>
            </div>

            <h3 class="subsection-title"><i class="fas fa-arrows-spin"></i> Arah Solusi yang Ditawarkan</h3>
            <div class="impact-row">
                <div class="impact-card before">
                    <h5 class="fw-bold text-danger"><i class="fas fa-triangle-exclamation me-2"></i> Kondisi yang Sering Terjadi</h5>
                    <ul class="dash-list mb-0">
                        <li>Stok gudang dan outlet tercatat di banyak buku/berkas terpisah.</li>
                        <li>Kasir berhenti total saat koneksi internet terputus.</li>
                        <li>Laporan HPP dan jurnal pimpinan sering terlambat karena rekapitulasi ulang.</li>
                    </ul>
                </div>
                <div class="impact-arrow"><i class="fas fa-arrow-right-long"></i></div>
                <div class="impact-card after">
                    <h5 class="fw-bold text-success"><i class="fas fa-circle-check me-2"></i> Kondisi Setelah Implementasi</h5>
                    <ul class="dash-list mb-0">
                        <li>Satu basis data gudang-pusat-cabang yang terkonsolidasi dan dapat diakses sesuai hak akses.</li>
                        <li>Kasir tetap dapat melayani transaksi walau internet terputus, tersinkron otomatis setelahnya.</li>
                        <li>HPP dan jurnal akuntansi terbentuk otomatis dari transaksi, siap dipantau lewat dashboard.</li>
                    </ul>
                </div>
            </div>

            <h2 class="section-title">II. Visi, Misi, Tujuan, dan Manfaat</h2>

            <p><strong>Visi Kami:</strong> Menjadi sistem gudang, POS, dan manajemen ritel terpadu yang unggul dan terpercaya, mendukung unit usaha mengakselerasi kecepatan layanan dan akurasi pembukuan.</p>

            <p><strong>Misi Kami:</strong></p>
            <ul class="custom-list">
                <li>Menyediakan solusi kasir, gudang, dan akuntansi yang komprehensif, terpusat, dan <i>user-friendly</i>.</li>
                <li>Meningkatkan efisiensi operasional transaksi dan transparansi tata kelola stok.</li>
                <li>Mendukung pimpinan dalam pengambilan keputusan strategis berdasarkan angka HPP dan laba yang akurat.</li>
                <li>Menyiapkan fondasi pengembangan distribusi antar-outlet dan skema investor secara bertahap dan aman.</li>
            </ul>

            <h3 class="subsection-title"><i class="fas fa-bullseye"></i> Tujuan & Manfaat Implementasi</h3>
            <div class="row mt-3">
                <div class="col-md-6">
                    <ul class="custom-list">
                        <li><strong>Efisiensi Menyeluruh:</strong> Otomatisasi transaksi kasir, pergerakan stok, dan posting jurnal, mengurangi pekerjaan manual berulang.</li>
                        <li><strong>Akurasi & Keandalan:</strong> Validasi stok server-side mencegah dobel-jual antar kasir yang bekerja bersamaan.</li>
                        <li><strong>Operasional Tanpa Henti:</strong> Kasir tetap dapat melayani pelanggan meski koneksi internet outlet terputus.</li>
                    </ul>
                </div>
                <div class="col-md-6">
                    <ul class="custom-list">
                        <li><strong>Transparansi Mutlak:</strong> Mempermudah pelacakan audit setiap pergerakan stok dan setiap baris jurnal.</li>
                        <li><strong>Pengambilan Keputusan:</strong> HPP dihitung otomatis dari resep, bukan diketik manual, sehingga margin laba lebih dapat dipercaya.</li>
                        <li><strong>Kesiapan Pertumbuhan:</strong> Fondasi arsitektur disiapkan untuk distribusi antar-outlet dan kemitraan investor di masa mendatang.</li>
                    </ul>
                </div>
            </div>

            <h3 class="subsection-title"><i class="fas fa-gem"></i> Nilai Strategis untuk Calon Mitra</h3>
            <div class="value-grid">
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-bolt"></i></div>
                    <div class="value-title">Transaksi Lebih Cepat</div>
                    <p class="value-desc">Kasir dapat melayani pelanggan tanpa jeda, bahkan saat koneksi internet outlet sedang terputus.</p>
                </div>
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-database"></i></div>
                    <div class="value-title">Stok Lebih Tertib</div>
                    <p class="value-desc">Gudang pusat dan cabang menggunakan referensi data yang sama sehingga mengurangi selisih dan duplikasi stok.</p>
                </div>
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-chart-line"></i></div>
                    <div class="value-title">Laba Lebih Akurat</div>
                    <p class="value-desc">HPP dihitung otomatis dari rollup resep sehingga margin laba yang tersaji ke pimpinan lebih dapat dipercaya.</p>
                </div>
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-handshake-angle"></i></div>
                    <div class="value-title">Kemitraan Fleksibel</div>
                    <p class="value-desc">Skema implementasi, infrastruktur, dan pembiayaan (3 opsi) dapat disesuaikan dengan kebutuhan dan skala unit usaha.</p>
                </div>
            </div>

            <div class="page-break"></div>

            <h2 class="section-title">III. Ekosistem eGudang</h2>
            <p>eGudang mengusung konsep <i>All-in-One Platform</i> untuk operasional gudang dan ritel, dengan dua pola pemanfaatan:</p>

            <div class="row text-center mt-4 mb-5">
                <div class="col-md-6">
                    <div class="p-4 border rounded-4 bg-light h-100 shadow-sm">
                        <i class="fas fa-graduation-cap fa-3x text-primary mb-3"></i>
                        <h4 class="fw-bold">Terintegrasi dengan Institusi Pendidikan</h4>
                        <p class="small text-muted mb-0">Bagi Universitas (eCampus), Sekolah (eSchool), atau Pondok Pesantren (ePesantren) yang memiliki kantin, koperasi, toko kampus, atau gudang sendiri, eGudang terhubung langsung dengan data mahasiswa, siswa, santri, dan pegawai yang sudah ada.</p>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="p-4 border rounded-4 bg-light h-100 shadow-sm border-bottom border-success border-4">
                        <i class="fas fa-store fa-3x text-success mb-3"></i>
                        <h4 class="fw-bold">Berdiri Sendiri (Standalone)</h4>
                        <p class="small text-muted mb-0">Jaringan ritel, koperasi, atau bisnis multi-outlet yang tidak bernaung di bawah institusi pendidikan — termasuk yang didanai investor — dapat mengimplementasikan eGudang secara independen.</p>
                    </div>
                </div>
            </div>

            <div class="highlight-box">
                <h5 class="fw-bold text-dark"><i class="fas fa-sitemap text-primary me-2"></i> Prinsip Desain Ekosistem</h5>
                <p class="mb-0">eGudang tidak hanya berperan sebagai aplikasi pencatatan, tetapi sebagai <strong>tulang punggung operasional digital</strong> yang menghubungkan kasir (POS), gudang, HPP, akuntansi, dan pengadaan dalam satu ekosistem kerja yang konsisten dan teraudit penuh.</p>
            </div>

            <div class="value-grid">
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-user-lock"></i></div>
                    <div class="value-title">Hak Akses Bertingkat</div>
                    <p class="value-desc">Kasir, petugas gudang, tim pengadaan, dan tim akuntansi hanya melihat menu dan data sesuai kewenangan.</p>
                </div>
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-wifi"></i></div>
                    <div class="value-title">Kasir Offline-Ready</div>
                    <p class="value-desc">POS dapat dipasang sebagai aplikasi (installable) dan tetap mencatat transaksi secara lokal saat internet terputus, lalu tersinkron otomatis.</p>
                </div>
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-cloud-arrow-up"></i></div>
                    <div class="value-title">Siap Dikembangkan</div>
                    <p class="value-desc">Arsitektur telah disiapkan untuk perluasan ke Ekspedisi, Investor, dan Kepegawaian Outlet secara bertahap.</p>
                </div>
                <div class="value-card">
                    <div class="value-icon"><i class="fas fa-scale-balanced"></i></div>
                    <div class="value-title">Tata Kelola Tertib</div>
                    <p class="value-desc">Mendukung jejak audit penuh, validasi berlapis, dan rekam historis aktivitas untuk kebutuhan pengawasan internal.</p>
                </div>
            </div>

            <h3 class="subsection-title"><i class="fas fa-network-wired"></i> Fleksibilitas Infrastruktur (Solusi Dual Model)</h3>
            <p>Sistem ini dirancang adaptif untuk merespon kebijakan keamanan data dan pola operasional outlet Anda melalui tiga skema pemasangan (<i>deployment</i>):</p>
            <ul class="custom-list">
                <li><strong>Model Cloud (Disarankan):</strong> Sistem di-<i>host</i> di server awan kami. Unit usaha tidak perlu investasi perangkat keras server, cukup dengan koneksi internet di masing-masing outlet.</li>
                <li><strong>Model On-Premise (Konvensional):</strong> Sistem dipasang murni pada server internal unit usaha (Intranet). Cocok bagi unit usaha dengan kebijakan privasi data yang tertutup.</li>
                <li><strong>Model Hybrid:</strong> Menggabungkan keandalan server lokal untuk menyimpan arsip transaksi gudang pusat, namun antarmukanya tetap dapat diakses via internet dari cabang.</li>
            </ul>

            <div class="page-break"></div>

            <h2 class="section-title">IV. Rincian Modul & Fungsionalitas Sistem</h2>
            <p>Modul eGudang berikut ini merupakan cakupan inti yang telah dibangun, diuji, dan dikeraskan (<i>hardened</i>) — bukan sekadar rencana di atas kertas.</p>

            <div class="metric-strip">
                <div class="metric"><strong>POS</strong><span>Multi-outlet, offline-first</span></div>
                <div class="metric"><strong>Gudang</strong><span>Hierarki pusat-cabang, teraudit</span></div>
                <div class="metric"><strong>HPP</strong><span>Rollup resep otomatis</span></div>
                <div class="metric"><strong>Pengadaan</strong><span>PR sampai pembayaran</span></div>
            </div>

            <h3 class="subsection-title"><i class="fas fa-cash-register"></i> A. Kasir (POS) Multi-Outlet & Offline-First</h3>
            <ul class="dash-list">
                <li><strong>Tetap Beroperasi Tanpa Internet:</strong> transaksi disimpan sementara di perangkat kasir (antrian lokal) dan otomatis tersinkron ke server begitu koneksi kembali tersedia, tanpa risiko transaksi tercatat ganda.</li>
                <li><strong>Dapat Dipasang Seperti Aplikasi:</strong> POS berjalan sebagai aplikasi web yang dapat di-install pada perangkat kasir, mendukung penggunaan sehari-hari layaknya aplikasi native.</li>
                <li><strong>Validasi Stok Anti-Overselling:</strong> pengecekan stok dilakukan di sisi server dengan penguncian baris data, sehingga dua kasir yang bertransaksi bersamaan tidak dapat menjual stok terakhir yang sama.</li>
                <li><strong>Shift Kasir (Buka/Tutup Kas):</strong> dapat diaktifkan sesuai kebutuhan, lengkap dengan rekonsiliasi kas otomatis (perhitungan selisih kas) di akhir shift.</li>
                <li><strong>Diskon, Cashback & Multi-Metode Pembayaran:</strong> mendukung tunai, non-tunai, dan QRIS/pembayaran digital dalam satu alur transaksi.</li>
            </ul>

            <h3 class="subsection-title"><i class="fas fa-warehouse"></i> B. Gudang & Rantai Pasok</h3>
            <ul class="dash-list">
                <li><strong>Hierarki Gudang Pusat-Cabang:</strong> struktur induk-anak antar gudang memungkinkan stok dipantau dan dikonsolidasi dari gudang pusat ke seluruh cabang/outlet.</li>
                <li><strong>Multi Lokasi:</strong> setiap lokasi fisik dapat dilacak stoknya secara independen.</li>
                <li><strong>Buku Besar Pergerakan Stok Penuh:</strong> setiap stok masuk, keluar, transfer, maupun penyesuaian tercatat dan dapat diaudit — tidak ada perubahan stok yang terjadi diam-diam, setiap perubahan tertelusur ke dokumen sumbernya.</li>
                <li><strong>Stok Opname:</strong> proses rekonsiliasi hasil hitung fisik terhadap catatan sistem.</li>
                <li><strong>Laporan Rollup:</strong> pelaporan terkonsolidasi lintas seluruh hierarki gudang.</li>
            </ul>

            <h3 class="subsection-title"><i class="fas fa-calculator"></i> C. HPP & Akuntansi Terintegrasi</h3>
            <ul class="dash-list">
                <li><strong>HPP Otomatis dari Resep:</strong> HPP produk berbasis resep dihitung otomatis dengan menggabungkan (rollup) harga seluruh bahan baku penyusunnya — bukan angka yang diketik manual, celah akurasi yang masih umum ditemui pada banyak sistem sejenis.</li>
                <li><strong>Posting Jurnal Otomatis:</strong> transaksi dari kasir maupun pengadaan diposting otomatis ke buku besar akuntansi, dengan setiap baris jurnal tertelusur balik ke dokumen sumbernya (tanpa jurnal "yatim").</li>
                <li><strong>Dashboard Draft Jurnal:</strong> panel pemantauan real-time yang menampilkan status posting — sudah terposting atau masih tertunda — di seluruh jenis transaksi.</li>
                <li><strong>Pemetaan Akun Dapat Dikonfigurasi:</strong> kode akun (chart-of-accounts) tidak dikunci secara baku (<i>hardcoded</i>), dapat disesuaikan dengan struktur akuntansi masing-masing unit usaha.</li>
            </ul>

            <h3 class="subsection-title"><i class="fas fa-truck-ramp-box"></i> D. Pengadaan (Procurement)</h3>
            <ul class="dash-list">
                <li><strong>Alur Matang End-to-End:</strong> Permintaan Pembelian &rarr; Persetujuan &rarr; Purchase Order &rarr; Penerimaan Barang (BAST) &rarr; Pembayaran.</li>
                <li><strong>Mesin Alur Persetujuan Generik:</strong> mekanisme persetujuan yang sama dapat dikonfigurasi ulang, dipakai bersama untuk pengadaan, penggajian, maupun cuti — bukan alur yang dibangun terpisah per modul.</li>
                <li><strong>Master Data Vendor/Pemasok:</strong> lengkap dengan portal tersendiri bagi vendor.</li>
            </ul>

            <h3 class="subsection-title"><i class="fas fa-chart-pie"></i> E. Dashboard Analitik & Laporan</h3>
            <ul class="dash-list">
                <li><strong>Dashboard Operasional:</strong> ringkasan penjualan per outlet, kondisi stok gudang, margin HPP, dan status Draft Jurnal secara real-time.</li>
                <li><strong>Laporan Siap Pakai:</strong> mencakup laporan transaksi kasir, pergerakan stok, hasil stok opname, hingga status pengadaan.</li>
            </ul>

            <h3 class="subsection-title"><i class="fas fa-shield-halved"></i> F. Keamanan Data & Jejak Audit</h3>
            <ul class="dash-list">
                <li><strong>Jejak Audit Menyeluruh:</strong> setiap perubahan stok, harga, dan transaksi tercatat: siapa, kapan, dan apa yang diubah.</li>
                <li><strong>Hak Akses Berjenjang:</strong> kasir, petugas gudang, tim pengadaan, tim akuntansi, dan admin masing-masing memiliki akses sesuai peran.</li>
                <li><strong>Data Sepenuhnya Milik Unit Usaha:</strong> seluruh data tersimpan aman dan menjadi milik penuh unit usaha, bukan pihak ketiga.</li>
            </ul>

            <h3 class="subsection-title"><i class="fas fa-mobile-screen-button"></i> G. Aplikasi Klien: POS Desktop, POS Android &amp; Stok Opname Android</h3>
            <p>
                Berbeda dari modul lain yang masih dalam roadmap, ketiga aplikasi klien berikut <strong>sudah selesai dibangun dan sudah dirilis resmi</strong> — dapat langsung diunduh dan diuji coba sejak dokumen ini disusun:
            </p>
            <ul class="dash-list">
                <li><strong>POS Kasir Desktop (Windows):</strong> kasir andal di meja kasir utama, tetap melayani pembeli walau internet putus (transaksi tersimpan otomatis dan tersinkron begitu koneksi pulih), Layar Pelanggan kedua (dual monitor) yang menampilkan rincian belanja langsung ke pembeli, pembayaran saldo member dengan verifikasi PIN, top-up saldo langsung dari kasir, lebih dari 150 laporan siap pakai, serta pembaruan aplikasi otomatis.</li>
                <li><strong>POS Kasir Android (tablet &amp; HP):</strong> mesin kasir dalam genggaman — satu aplikasi yang tampilannya menyesuaikan otomatis, cocok untuk tablet (mis. Galaxy Tab) sebagai kasir tetap maupun HP Android biasa untuk kasir keliling/cadangan. Mencetak struk langsung ke printer thermal Bluetooth portable dan tetap dapat berjualan tanpa sinyal.</li>
                <li><strong>Stok Opname Android:</strong> aplikasi khusus perhitungan stok fisik ala supermarket/minimarket, dirancang mengikuti praktik terbaik ritel modern — memindai barcode via alat pemindai/PDT genggam (plug-and-play) atau kamera HP bila alat pemindai tidak tersedia, hasil langsung tersinkron ke server. Sengaja fokus pada satu tugas saja agar tim lapangan cepat mahir dan proses opname banyak outlet/gudang selesai lebih cepat.</li>
                <li><strong>Terhubung ke Server yang Sama:</strong> ketiganya memakai backend eGudang/POS yang identik, sehingga data kasir, stok, dan member selalu konsisten di manapun perangkat digunakan — tanpa server tambahan.</li>
            </ul>
            <p>
                Tautan unduh resmi (GitHub Releases, gratis):
                <a href="https://github.com/Zishof/ais-pos-kasir-desktop/releases" target="_blank" rel="noopener">POS Kasir Desktop</a> &middot;
                <a href="https://github.com/Zishof/ais-pos-kasir-android/releases" target="_blank" rel="noopener">POS Kasir Android</a> &middot;
                <a href="https://github.com/Zishof/ais-stok-opname-android/releases" target="_blank" rel="noopener">Stok Opname Android</a>
            </p>

            <div class="highlight-box bg-primary bg-opacity-10">
                <h5 class="fw-bold text-primary"><i class="fas fa-plug-circle-check me-2"></i> Keunggulan Integrasi Modul</h5>
                <p class="mb-0">Kekuatan utama eGudang terletak pada keterhubungan antar modul. Transaksi dari kasir dapat langsung menjadi dasar pergerakan stok, perhitungan HPP, posting jurnal, hingga laporan manajemen — sehingga unit usaha memperoleh alur kerja yang lebih hemat waktu dan konsisten.</p>
            </div>

            <div class="page-break"></div>

            <h2 class="section-title">V. Roadmap Pengembangan: Ekspedisi, Investor & Kepegawaian Outlet</h2>
            <p>
                Di atas fondasi POS, Gudang, HPP/Akuntansi, dan Pengadaan yang telah berjalan, arsitektur eGudang telah disiapkan untuk perluasan berikut secara bertahap dan aditif, tanpa mengganggu operasional yang sedang berjalan:
            </p>

            <div class="timeline">
                <div class="timeline-step">
                    <div class="timeline-number">01</div>
                    <div class="timeline-content">
                        <h5>Fase Fondasi: POS, Gudang, HPP/Akuntansi, Pengadaan <span class="badge bg-success ms-2">Telah Tersedia</span></h5>
                        <p>Modul inti pada Bab IV telah dibangun, diuji, dan digunakan — bukan sekadar rencana.</p>
                    </div>
                </div>
                <div class="timeline-step">
                    <div class="timeline-number">02</div>
                    <div class="timeline-content">
                        <h5>Ekspedisi (Distribusi/Logistik) <span class="badge bg-warning text-dark ms-2">Roadmap - Dalam Perancangan</span></h5>
                        <p>Rute pengiriman multi-titik dari gudang ke beberapa outlet dalam satu perjalanan, manifest pengiriman, bukti serah terima, dan pelacakan kendaraan/BBM. Sudah dipetakan secara arsitektural, rilis bertahap direncanakan.</p>
                    </div>
                </div>
                <div class="timeline-step">
                    <div class="timeline-number">03</div>
                    <div class="timeline-content">
                        <h5>Investor (Bagi Hasil) <span class="badge bg-warning text-dark ms-2">Roadmap - Dalam Perancangan</span></h5>
                        <p>Model bagi hasil 3-fase yang dapat dikonfigurasi per perjanjian investasi (Fase 0 sebelum BEP pertama: porsi investor lebih besar; Fase 1 antara BEP pertama dan kedua: porsi mulai menyeimbang; Fase 2 setelah BEP kedua: porsi perusahaan meningkat). Persentase tidak pernah dikunci baku (<i>hardcoded</i>), melainkan dikonfigurasi per perjanjian/outlet/brand/tanggal efektif, dilengkapi portal investor real-time (modal, fase berjalan, persentase, akumulasi bagi hasil, jarak menuju BEP berikutnya).</p>
                    </div>
                </div>
                <div class="timeline-step">
                    <div class="timeline-number">04</div>
                    <div class="timeline-content">
                        <h5>Kepegawaian Outlet <span class="badge bg-info text-dark ms-2">Roadmap - Konfigurasi Lanjutan</span></h5>
                        <p>Menggunakan kembali mesin penggajian dan presensi yang sudah matang dan telah dipakai untuk tenaga pengajar (komponen gaji berbasis formula, presensi berbasis shift dengan geofencing, cuti melalui alur persetujuan yang sama) — perluasan ke pegawai ritel/gudang bersifat pekerjaan konfigurasi, bukan rekayasa dari nol.</p>
                    </div>
                </div>
            </div>

            <div class="highlight-box">
                <h5 class="fw-bold text-dark"><i class="fas fa-lightbulb text-warning me-2"></i> Mengapa Urutan Ini Penting</h5>
                <p class="mb-0">Skema bagi hasil investor pada banyak sistem sejenis dihitung secara kasar langsung dari omzet kotor. Pendekatan kami sengaja berbeda: distribusi ke investor baru dibangun <strong>setelah</strong> angka Laba Bersih (setelah HPP, biaya operasional, penyusutan, pajak, dan komponen lain) benar-benar akurat — karena itulah fondasi HPP dan akuntansi otomatis pada Bab IV dibangun lebih dahulu. Urutan pengerjaan ini sendiri merupakan nilai jual: angka yang benar dahulu, baru distribusi bagi hasil dibangun di atasnya.</p>
            </div>

            <h2 class="section-title">VI. Metodologi Implementasi & Pendampingan</h2>
            <p>Implementasi eGudang dilakukan secara bertahap agar proses migrasi data, pelatihan kasir dan petugas gudang, konfigurasi, dan adopsi pengguna dapat berjalan terkendali.</p>

            <div class="timeline">
                <div class="timeline-step">
                    <div class="timeline-number">01</div>
                    <div class="timeline-content">
                        <h5>Assessment Kebutuhan & Pemetaan Alur Operasional</h5>
                        <p>Mengidentifikasi kondisi eksisting, alur transaksi-stok-pembukuan, dan prioritas modul yang akan diaktifkan.</p>
                    </div>
                </div>
                <div class="timeline-step">
                    <div class="timeline-number">02</div>
                    <div class="timeline-content">
                        <h5>Konfigurasi Sistem & Migrasi Data Awal</h5>
                        <p>Menyiapkan master gudang/lokasi, produk/resep, harga, hierarki outlet, dan hak akses pengguna.</p>
                    </div>
                </div>
                <div class="timeline-step">
                    <div class="timeline-number">03</div>
                    <div class="timeline-content">
                        <h5>Pelatihan Kasir, Petugas Gudang & Tim Akuntansi</h5>
                        <p>Memberikan pendampingan penggunaan modul kepada kasir, petugas gudang, tim pengadaan, dan tim akuntansi.</p>
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
                    <p class="value-desc">Pengaturan parameter sistem, master data gudang/produk, hak akses, dan kebutuhan awal operasional.</p>
                </div>
                <div class="scope-item">
                    <h5><i class="fas fa-chalkboard-user me-2"></i> Pelatihan</h5>
                    <p class="value-desc">Transfer knowledge kepada kasir, petugas gudang, tim pengadaan, dan tim akuntansi agar mampu menjalankan sistem secara mandiri.</p>
                </div>
                <div class="scope-item">
                    <h5><i class="fas fa-headset me-2"></i> Dukungan Teknis</h5>
                    <p class="value-desc">Bantuan teknis selama proses implementasi, penyesuaian alur operasional, dan pendampingan saat go-live.</p>
                </div>
            </div>

            <div class="page-break"></div>

            <h2 class="section-title">VII. Skema Harga</h2>
            <p>
                Skema harga eGudang disusun secara <strong>berjenjang (tiered)</strong> agar biaya berlangganan selaras dengan skala usaha: unit usaha dengan sedikit outlet membayar sesuai kebutuhan minimalnya, sementara unit usaha dengan banyak outlet memperoleh harga per unit yang semakin efisien. Struktur ini terdiri dari dua komponen yang dijumlahkan setiap bulan.
            </p>
            <div class="highlight-box bg-warning bg-opacity-10" style="border-left-color: var(--warning-color);">
                <h5 class="fw-bold text-dark"><i class="fas fa-coins text-warning me-2"></i> Formula Biaya Bulanan</h5>
                <p class="mb-0">
                    <strong>Biaya Bulanan = Paket Modul Pusat + Biaya POS Pertama (tiap Outlet) + Biaya POS Tambahan.</strong>
                    Dengan kata lain, setiap Outlet dikenakan biaya POS pertama untuk terminal kasir utamanya, sedangkan terminal kasir tambahan pada Outlet yang sama dikenakan tarif POS tambahan yang lebih rendah. Di tingkat perusahaan/yayasan, satu Paket Modul Pusat dipilih untuk mengonsolidasikan seluruh Outlet.
                </p>
            </div>

            <h4 class="fw-bold text-primary mt-4 mb-2"><i class="fas fa-cash-register me-2"></i> A. Harga POS per Terminal (per Outlet)</h4>
            <p class="small text-muted mb-2">Semakin banyak jumlah Outlet yang didaftarkan, semakin rendah tarif per unit POS yang berlaku.</p>
            <div class="table-modern mb-3">
                <table class="table table-bordered align-middle">
                    <thead>
                        <tr>
                            <th><%= Common.getBahasaConfig("Jumlah Outlet") %></th>
                            <th><%= Common.getBahasaConfig("POS Pertama / bulan") %></th>
                            <th><%= Common.getBahasaConfig("POS Tambahan / bulan / unit") %></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><strong>1 – 5 Outlet</strong></td>
                            <td>Rp 249.000</td>
                            <td>Rp 99.000</td>
                        </tr>
                        <tr>
                            <td><strong>6 – 20 Outlet</strong></td>
                            <td>Rp 225.000</td>
                            <td>Rp 85.000</td>
                        </tr>
                        <tr>
                            <td><strong>21 – 50 Outlet</strong></td>
                            <td>Rp 199.000</td>
                            <td>Rp 75.000</td>
                        </tr>
                        <tr>
                            <td><strong>Lebih dari 50 Outlet</strong></td>
                            <td>Mulai Rp 175.000</td>
                            <td>Mulai Rp 60.000</td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <p class="small text-muted mb-5">
                Biaya POS pertama sudah mencakup: transaksi penjualan, pembayaran tunai &amp; nontunai, shift kasir, stok outlet, harga &amp; diskon, data pelanggan, retur &amp; pembatalan, laporan penjualan, pengguna &amp; hak akses, mode offline-first, sinkronisasi transaksi, serta backup cloud standar. Biaya ini <strong>belum termasuk</strong> perangkat keras (perangkat kasir, printer, scanner) dan biaya jaringan internet di lokasi Outlet.
            </p>

            <h4 class="fw-bold text-primary mb-2"><i class="fas fa-sitemap me-2"></i> B. Paket Modul Pusat (per Perusahaan/Yayasan)</h4>
            <p class="small text-muted mb-3">Paket Modul Pusat dipilih satu kali untuk seluruh jaringan Outlet, bukan per outlet, dan mencakup fungsi konsolidasi, pengadaan, hingga akuntansi.</p>
            <div class="row g-4 align-items-stretch mb-4">
                <div class="col-md-4">
                    <div class="pricing-card border-primary">
                        <div class="pricing-header">
                            <h4 class="fw-bold text-primary">Central Basic</h4>
                            <p class="text-muted small">Konsolidasi Dasar</p>
                            <div class="pricing-price" style="font-size: 1.8rem;">Rp 750.000 <span class="fs-6 text-muted fw-normal">/ bulan</span></div>
                        </div>
                        <ul class="custom-list small">
                            <li><strong><i class="fas fa-check text-primary me-1"></i> Termasuk:</strong> Dashboard pusat, stok konsolidasi, transfer antaroutlet, pembelian sederhana.</li>
                            <li><strong><i class="fas fa-users text-primary me-1"></i> Cocok untuk:</strong> Unit usaha yang baru mulai mengonsolidasikan banyak outlet.</li>
                        </ul>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="pricing-card" style="border-color: var(--warning-color);">
                        <div class="pricing-header">
                            <h4 class="fw-bold" style="color: var(--warning-color);">Central Professional</h4>
                            <p class="text-muted small">Rantai Pasok & Produksi</p>
                            <div class="pricing-price" style="font-size: 1.8rem; color: var(--warning-color);">Rp 1.500.000 <span class="fs-6 text-muted fw-normal">/ bulan</span></div>
                        </div>
                        <ul class="custom-list small">
                            <li><strong><i class="fas fa-check text-warning me-1"></i> Termasuk:</strong> Gudang pusat, pengadaan (PR/PO), distribusi, pengiriman, produksi &amp; HPP.</li>
                            <li><strong><i class="fas fa-users text-warning me-1"></i> Cocok untuk:</strong> Unit usaha dengan rantai pasok/produksi aktif.</li>
                        </ul>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="pricing-card" style="border-color: var(--success-color);">
                        <div class="pricing-header">
                            <h4 class="fw-bold" style="color: var(--success-color);">Full Integrated Suite</h4>
                            <p class="text-muted small">End-to-End</p>
                            <div class="pricing-price" style="font-size: 1.8rem; color: var(--success-color);">Rp 2.500.000 <span class="fs-6 text-muted fw-normal">/ bulan</span></div>
                            <span class="badge bg-success text-white rounded-pill px-3 py-2 mt-2 mb-3">Paket Paling Lengkap</span>
                        </div>
                        <ul class="custom-list small">
                            <li><strong><i class="fas fa-check text-success me-1"></i> Termasuk:</strong> Seluruh modul pusat, ditambah akuntansi, kepegawaian, investor, BEP, audit &amp; analitik.</li>
                            <li><strong><i class="fas fa-users text-success me-1"></i> Cocok untuk:</strong> Unit usaha yang ingin satu sistem end-to-end.</li>
                        </ul>
                    </div>
                </div>
            </div>

            <div class="table-modern mb-4">
                <table class="table table-bordered align-middle">
                    <thead>
                        <tr>
                            <th><%= Common.getBahasaConfig("Aspek") %></th>
                            <th><%= Common.getBahasaConfig("Central Basic") %></th>
                            <th><%= Common.getBahasaConfig("Central Professional") %></th>
                            <th><%= Common.getBahasaConfig("Full Integrated Suite") %></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><strong>Biaya Bulanan</strong></td>
                            <td>Rp 750.000</td>
                            <td>Rp 1.500.000</td>
                            <td>Rp 2.500.000</td>
                        </tr>
                        <tr>
                            <td><strong>Cakupan Gudang &amp; Produksi</strong></td>
                            <td>Konsolidasi &amp; transfer dasar</td>
                            <td>Gudang pusat, PR/PO, distribusi, produksi, HPP</td>
                            <td>Seluruh cakupan Central Professional</td>
                        </tr>
                        <tr>
                            <td><strong>Cakupan Akuntansi &amp; Investor</strong></td>
                            <td>Belum tersedia</td>
                            <td>Belum tersedia</td>
                            <td>Akuntansi, investor, BEP, audit &amp; analitik</td>
                        </tr>
                        <tr>
                            <td><strong>Cocok Untuk</strong></td>
                            <td>Awal konsolidasi banyak outlet</td>
                            <td>Rantai pasok/produksi aktif</td>
                            <td>Sistem end-to-end satu atap</td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <h4 class="fw-bold text-primary mb-2"><i class="fas fa-calculator me-2"></i> Simulasi Estimasi Biaya Total</h4>
            <p class="small text-muted mb-3">Ilustrasi berikut mengasumsikan rata-rata 2 unit POS per Outlet dan Paket Modul Pusat Full Integrated Suite.</p>
            <div class="table-modern mb-4">
                <table class="table table-bordered align-middle">
                    <thead>
                        <tr>
                            <th><%= Common.getBahasaConfig("Skala Outlet") %></th>
                            <th><%= Common.getBahasaConfig("Estimasi Biaya POS") %></th>
                            <th><%= Common.getBahasaConfig("Paket Modul Pusat") %></th>
                            <th><%= Common.getBahasaConfig("Estimasi Total / bulan") %></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><strong>5 Outlet</strong></td>
                            <td>Rp 1.740.000</td>
                            <td>Rp 2.500.000</td>
                            <td><strong>Rp 4.240.000</strong></td>
                        </tr>
                        <tr>
                            <td><strong>10 Outlet</strong></td>
                            <td>Rp 3.100.000</td>
                            <td>Rp 2.500.000</td>
                            <td><strong>Rp 5.600.000</strong></td>
                        </tr>
                        <tr>
                            <td><strong>30 Outlet</strong></td>
                            <td>Rp 8.220.000</td>
                            <td>Rp 2.500.000</td>
                            <td><strong>Rp 10.720.000</strong></td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div class="highlight-box bg-warning bg-opacity-10 mb-4" style="border-left-color: var(--warning-color);">
                <h5 class="fw-bold text-dark"><i class="fas fa-file-invoice-dollar text-warning me-2"></i> Biaya Implementasi Awal & Opsi Pembayaran Lain</h5>
                <p class="mb-2">Selain biaya bulanan di atas, terdapat biaya implementasi awal (sekali bayar) untuk instalasi, konfigurasi, migrasi data, dan pelatihan pengguna:</p>
                <ul class="custom-list small mb-3">
                    <li><strong>1 Outlet:</strong> Rp 1,5 – 3 juta.</li>
                    <li><strong>2 – 5 Outlet:</strong> Rp 5 – 10 juta.</li>
                    <li><strong>6 – 20 Outlet:</strong> Rp 15 – 25 juta.</li>
                    <li><strong>21 – 50 Outlet:</strong> Rp 30 – 50 juta.</li>
                    <li><strong>Implementasi modul pusat tambahan:</strong> Rp 10 – 30 juta, tergantung kompleksitas integrasi.</li>
                </ul>
                <p class="mb-2"><strong>Opsi Harga Tahunan:</strong> tersedia skema pembayaran tahunan (prabayar) dengan penghematan setara kurang lebih 2 bulan biaya berlangganan dibandingkan pembayaran bulanan.</p>
                <p class="mb-0"><strong>Opsi Pelengkap – Bagi Hasil per Transaksi:</strong> bagi unit usaha yang ingin memulai tanpa biaya implementasi di muka, tersedia alternatif skema Bagi Hasil sebesar Rp 50 – Rp 100 per transaksi, dengan nilai minimum yang disetarakan dengan biaya langganan POS reguler.</p>
            </div>

            <div class="highlight-box bg-success bg-opacity-10 border-success mb-5">
                <h5 class="fw-bold text-success"><i class="fas fa-handshake me-2"></i> Keterbukaan Negosiasi Kemitraan</h5>
                <p class="mb-0 text-justify small">Struktur harga berjenjang di atas — Paket Modul Pusat, tarif POS per Outlet, biaya implementasi, maupun opsi Bagi Hasil per Transaksi — merupakan <strong>standar acuan yang tetap terbuka untuk dinegosiasikan dan disesuaikan</strong> sesuai skala jaringan Outlet, kompleksitas kebutuhan, dan kesepakatan akhir. Kami sangat terbuka untuk merumuskan skema yang paling sesuai agar sistem ini dapat segera memberikan manfaat nyata bagi unit usaha Anda.</p>
            </div>

            <h2 class="section-title">VIII. Penutup & Ajakan Kerja Sama</h2>
            <div class="highlight-box">
                <h5 class="fw-bold text-dark"><i class="fas fa-handshake-angle text-primary me-2"></i> Komitmen Kemitraan</h5>
                <p class="mb-0">Kami tidak hanya menawarkan aplikasi, tetapi menawarkan pendampingan transformasi proses kerja operasional. Dengan kerja sama yang terarah, eGudang diharapkan menjadi fondasi digital yang memperkuat kecepatan layanan, akurasi pembukuan, dan pertumbuhan unit usaha dalam jangka panjang.</p>
            </div>

            <div class="text-center mt-3">
                <i class="fas fa-warehouse fa-4x text-success mb-4"></i>
                <h2 class="fw-bold mb-4 text-dark">Melangkah ke Masa Depan Operasional Gudang & Ritel Digital</h2>
                <p class="text-muted fs-5 mb-5 mx-auto" style="max-width: 850px;">
                    eGudang dibangun di atas ekosistem Enterprise Education yang telah teruji dan dipercaya oleh puluhan institusi pendidikan. Modul eGudang menghadirkan pendekatan yang sama untuk operasional gudang dan ritel: satu platform, terintegrasi, teraudit, dan siap berkembang bersama pertumbuhan bisnis Anda.
                </p>
                <p class="text-muted mb-4 fs-5">
                    Silakan jalin komunikasi dengan representatif konsultan kami untuk melakukan penjadwalan presentasi, diskusi teknis konfigurasi migrasi, atau sesi negosiasi pendalaman kerja sama.
                </p>

                <% if(paramPresentBy != null && !paramPresentBy.trim().isEmpty()) { %>
                    <div class="d-inline-block p-5 mt-3 bg-white rounded-4 border border-secondary shadow text-start" style="min-width: 400px;">
                        <h5 class="fw-bold text-primary mb-4 text-center"><i class="fas fa-user-tie me-2"></i> <%= Common.getBahasaConfig("Konsultan Resmi Unit Usaha Anda") %></h5>
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
                        <h4 class="fw-bold text-primary mb-4 text-center">Tim Pusat Enterprise Education - Modul eGudang</h4>
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
                    <div class="meta-value mt-2"><%=(paramPresentBy != null && !paramPresentBy.trim().isEmpty()) ? paramPresentBy : "Tim eGudang - Enterprise Education"%></div>
                    <div class="text-muted small mt-5 pt-4">Tanda tangan dan nama jelas</div>
                </div>
                <div class="signature-box">
                    <div class="meta-label">Diterima / Ditinjau oleh</div>
                    <div class="meta-value mt-2"><%=(paramClient != null && !paramClient.trim().isEmpty()) ? paramClient : "Calon Mitra Unit Usaha"%></div>
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
