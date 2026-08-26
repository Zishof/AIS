<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.Collections"%>
<%@ page import="java.util.List"%>
<%@ page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@ page import="ais.action.servlet.landing.PesantrenLandingService.Profil"%>
<%@ page import="ais.action.servlet.landing.PesantrenLandingService.UnitPendidikan"%>
<%@ page import="ais.action.servlet.landing.PesantrenLandingService.Berita"%>
<%@ page import="ais.common.Common"%>
<%!
    private static String e(Object value) {
        return StringEscapeUtils.escapeHtml(value == null ? "" : String.valueOf(value));
    }
    private static String href(String value, String fallback) {
        String v = value == null ? "" : value.trim();
        if (v.startsWith("https://") || v.startsWith("http://") || v.startsWith("/")
                || v.startsWith("#") || v.startsWith("mailto:") || v.startsWith("tel:")) {
            return StringEscapeUtils.escapeHtml(v);
        }
        return StringEscapeUtils.escapeHtml(fallback == null ? "#" : fallback);
    }
    private static String digits(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }
%>
<%
    Profil profil = (Profil) request.getAttribute("pesantrenProfil");
    if (profil == null) {
        profil = new Profil("Pondok Pesantren", "Ilmu, adab, dan kemandirian", "", "", "", "", "", "",
                "#0f766e", request.getContextPath() + "/img/logo.png", request.getContextPath() + "/img/main.jpg");
    }
    List<UnitPendidikan> sekolahs = (List<UnitPendidikan>) request.getAttribute("pesantrenSekolah");
    List<UnitPendidikan> perguruanTinggis = (List<UnitPendidikan>) request.getAttribute("pesantrenPerguruanTinggi");
    List<Berita> berita = (List<Berita>) request.getAttribute("pesantrenBerita");
    if (sekolahs == null) sekolahs = Collections.emptyList();
    if (perguruanTinggis == null) perguruanTinggis = Collections.emptyList();
    if (berita == null) berita = Collections.emptyList();
    String root = request.getContextPath();
    String wa = digits(profil.getWa());
    if (wa.startsWith("0")) wa = "62" + wa.substring(1);
%>
<!doctype html>
<html lang="id">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="<%=e(profil.getWarna())%>">
    <meta name="description" content="Portal terpadu <%=e(profil.getNama())%> untuk pendidikan, kesantrian, layanan wali, pustaka, kesehatan, dan ekonomi pesantren.">
    <title><%=e(profil.getNama())%> - Portal ePesantren</title>
    <style>
        :root {
            --primary: <%=e(profil.getWarna())%>;
            --ink: #102a2a;
            --muted: #607475;
            --cream: #fbfaf5;
            --paper: #ffffff;
            --gold: #c79a3b;
            --emerald: #0f766e;
            --emerald-dark: #073f3d;
            --line: rgba(15, 118, 110, .14);
            --shadow: 0 22px 70px rgba(7, 63, 61, .12);
            --radius: 26px;
        }
        * { box-sizing: border-box; }
        html { scroll-behavior: smooth; }
        body { margin: 0; color: var(--ink); background: var(--cream); font: 16px/1.65 Inter, "Segoe UI", Arial, sans-serif; }
        a { color: inherit; }
        img { max-width: 100%; }
        .skip { position: fixed; left: 16px; top: -80px; z-index: 999; padding: 10px 16px; background: #fff; border-radius: 12px; }
        .skip:focus { top: 12px; }
        .wrap { width: min(1180px, calc(100% - 40px)); margin: auto; }
        .topbar { position: sticky; top: 0; z-index: 50; background: rgba(251,250,245,.9); backdrop-filter: blur(16px); border-bottom: 1px solid var(--line); }
        .topbar-inner { min-height: 78px; display: flex; align-items: center; justify-content: space-between; gap: 28px; }
        .brand { display: flex; align-items: center; gap: 12px; text-decoration: none; min-width: 0; }
        .brand img { width: 46px; height: 46px; object-fit: contain; border-radius: 12px; background: #fff; padding: 4px; }
        .brand strong { display: block; font: 700 17px/1.2 Georgia, serif; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 330px; }
        .brand span { display: block; color: var(--muted); font-size: 12px; letter-spacing: .1em; text-transform: uppercase; }
        nav { display: flex; align-items: center; gap: 22px; }
        nav a { text-decoration: none; font-size: 14px; font-weight: 650; }
        .button { display: inline-flex; align-items: center; justify-content: center; gap: 9px; min-height: 46px; padding: 0 20px; border-radius: 999px; border: 1px solid transparent; text-decoration: none; font-weight: 750; transition: transform .2s ease, box-shadow .2s ease; }
        .button:hover { transform: translateY(-2px); }
        .button-primary { color: #fff; background: var(--primary); box-shadow: 0 12px 28px rgba(15,118,110,.22); }
        .button-light { background: #fff; border-color: rgba(255,255,255,.55); }
        .button-outline { border-color: var(--line); background: rgba(255,255,255,.72); }
        .hero { position: relative; overflow: hidden; min-height: 670px; color: #fff; background: var(--emerald-dark); }
        .hero::before { content: ""; position: absolute; inset: 0; background: linear-gradient(90deg, rgba(4,45,43,.96) 0%, rgba(4,45,43,.88) 50%, rgba(4,45,43,.35) 100%), url('<%=href(profil.getLatar(), root + "/img/main.jpg")%>') center/cover; }
        .hero::after { content: ""; position: absolute; inset: 0; opacity: .18; background-image: radial-gradient(circle at 20px 20px, #e4bf6a 1.5px, transparent 1.8px); background-size: 42px 42px; mask-image: linear-gradient(to right, #000, transparent 78%); }
        .hero-grid { position: relative; z-index: 2; display: grid; grid-template-columns: 1.12fr .88fr; gap: 70px; align-items: center; min-height: 670px; padding: 84px 0; }
        .eyebrow { display: inline-flex; align-items: center; gap: 10px; color: #f2d68e; font-size: 13px; font-weight: 800; letter-spacing: .16em; text-transform: uppercase; }
        .eyebrow::before { content: ""; width: 34px; height: 1px; background: currentColor; }
        h1, h2, h3 { font-family: Georgia, "Times New Roman", serif; }
        h1 { max-width: 760px; margin: 18px 0 20px; font-size: clamp(44px, 6vw, 78px); line-height: 1.04; letter-spacing: -.035em; }
        .hero-lead { max-width: 720px; margin: 0 0 30px; color: rgba(255,255,255,.82); font-size: 19px; }
        .hero-actions { display: flex; flex-wrap: wrap; gap: 12px; }
        .hero-card { align-self: end; padding: 30px; border: 1px solid rgba(255,255,255,.18); border-radius: 32px; background: rgba(255,255,255,.1); backdrop-filter: blur(14px); box-shadow: 0 26px 80px rgba(0,0,0,.22); }
        .hero-card img { width: 80px; height: 80px; object-fit: contain; padding: 7px; border-radius: 20px; background: #fff; }
        .hero-card h2 { margin: 22px 0 8px; font-size: 30px; line-height: 1.18; }
        .hero-card p { margin: 0; color: rgba(255,255,255,.75); }
        .hero-facts { display: grid; grid-template-columns: repeat(3,1fr); gap: 10px; margin-top: 26px; }
        .fact { padding: 15px 10px; text-align: center; border-radius: 16px; background: rgba(0,0,0,.14); }
        .fact strong { display: block; font-size: 20px; color: #f3d786; }
        .fact span { font-size: 11px; color: rgba(255,255,255,.7); }
        section { padding: 96px 0; }
        .section-head { display: grid; grid-template-columns: .85fr 1.15fr; gap: 60px; align-items: end; margin-bottom: 46px; }
        .section-head h2 { margin: 10px 0 0; font-size: clamp(36px, 4vw, 56px); line-height: 1.08; letter-spacing: -.025em; }
        .section-head p { margin: 0; color: var(--muted); font-size: 18px; }
        .kicker { color: var(--primary); font-weight: 850; letter-spacing: .14em; text-transform: uppercase; font-size: 12px; }
        .unit-grid, .news-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
        .unit, .news { position: relative; padding: 28px; background: var(--paper); border: 1px solid var(--line); border-radius: var(--radius); box-shadow: 0 12px 36px rgba(16,42,42,.06); }
        .unit::before { content: ""; position: absolute; left: 0; top: 26px; bottom: 26px; width: 4px; border-radius: 0 4px 4px 0; background: var(--primary); }
        .label { display: inline-block; padding: 5px 10px; border-radius: 999px; color: var(--emerald-dark); background: #e6f3ef; font-size: 11px; font-weight: 800; text-transform: uppercase; letter-spacing: .08em; }
        .unit h3, .news h3 { margin: 16px 0 8px; font-size: 24px; line-height: 1.2; }
        .unit p, .news p { color: var(--muted); margin: 0 0 18px; }
        .unit a { color: var(--primary); font-weight: 800; text-decoration: none; }
        .pillars { background: #eef5f2; }
        .pillar-grid { display: grid; grid-template-columns: repeat(4,1fr); gap: 18px; counter-reset: pilar; }
        .pillar { min-height: 240px; padding: 28px; border-radius: 24px; background: #fff; border: 1px solid rgba(15,118,110,.11); counter-increment: pilar; }
        .pillar::before { content: "0" counter(pilar); display: block; margin-bottom: 45px; color: var(--gold); font: 700 14px Georgia, serif; }
        .pillar h3 { margin: 0 0 10px; font-size: 22px; line-height: 1.18; }
        .pillar p { margin: 0; color: var(--muted); font-size: 14px; }
        .biometric { background: var(--emerald-dark); color: #fff; overflow: hidden; }
        .bio-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 72px; align-items: center; }
        .bio-grid h2 { margin: 12px 0 20px; font-size: clamp(38px,5vw,62px); line-height: 1.06; }
        .bio-grid > div > p { color: rgba(255,255,255,.75); font-size: 18px; }
        .bio-flow { display: grid; gap: 14px; }
        .bio-step { display: grid; grid-template-columns: 48px 1fr; gap: 16px; align-items: start; padding: 19px; background: rgba(255,255,255,.08); border: 1px solid rgba(255,255,255,.13); border-radius: 18px; }
        .bio-step b { display: grid; place-items: center; width: 48px; height: 48px; border-radius: 15px; background: #f1d27d; color: var(--emerald-dark); }
        .bio-step strong { display: block; margin-bottom: 3px; }
        .bio-step span { color: rgba(255,255,255,.7); font-size: 14px; }
        .services { background: #fff; }
        .service-columns { display: grid; grid-template-columns: repeat(3,1fr); gap: 22px; }
        .service-group { padding: 26px; border-radius: 24px; background: var(--cream); border: 1px solid var(--line); }
        .service-group h3 { margin: 0 0 18px; font-size: 24px; }
        .service-links { display: grid; gap: 9px; }
        .service-links a { display: flex; justify-content: space-between; gap: 12px; padding: 12px 14px; border-radius: 13px; text-decoration: none; background: #fff; border: 1px solid rgba(15,118,110,.1); font-weight: 700; font-size: 14px; }
        .service-links a::after { content: "↗"; color: var(--primary); }
        .news time { color: var(--gold); font-weight: 800; font-size: 12px; text-transform: uppercase; letter-spacing: .08em; }
        .empty { padding: 42px; text-align: center; border: 1px dashed rgba(15,118,110,.28); border-radius: 24px; color: var(--muted); }
        .contact { padding-top: 0; }
        .contact-card { display: grid; grid-template-columns: 1.2fr .8fr; gap: 44px; padding: 54px; border-radius: 34px; background: linear-gradient(135deg, var(--primary), var(--emerald-dark)); color: #fff; box-shadow: var(--shadow); }
        .contact-card h2 { margin: 8px 0 16px; font-size: 44px; line-height: 1.05; }
        .contact-card p { color: rgba(255,255,255,.78); }
        .contact-list { display: grid; gap: 10px; align-content: center; }
        .contact-list a, .contact-list span { padding: 12px 16px; border-radius: 14px; background: rgba(255,255,255,.09); text-decoration: none; }
        footer { padding: 38px 0; color: var(--muted); border-top: 1px solid var(--line); }
        .footer-inner { display: flex; justify-content: space-between; gap: 20px; flex-wrap: wrap; }
        @media (max-width: 980px) {
            nav a:not(.button) { display: none; }
            .hero-grid, .bio-grid, .contact-card, .section-head { grid-template-columns: 1fr; }
            .hero-grid { gap: 30px; }
            .hero-card { align-self: auto; }
            .unit-grid, .news-grid, .service-columns { grid-template-columns: repeat(2,1fr); }
            .pillar-grid { grid-template-columns: repeat(2,1fr); }
        }
        @media (max-width: 640px) {
            .wrap { width: min(100% - 26px, 1180px); }
            .topbar-inner { min-height: 68px; }
            .brand strong { max-width: 180px; }
            nav .button { padding: 0 14px; min-height: 40px; }
            .hero-grid { min-height: auto; padding: 68px 0; }
            h1 { font-size: 42px; }
            section { padding: 72px 0; }
            .unit-grid, .news-grid, .service-columns, .pillar-grid { grid-template-columns: 1fr; }
            .pillar { min-height: 190px; }
            .pillar::before { margin-bottom: 28px; }
            .contact-card { padding: 34px 24px; }
            .contact-card h2 { font-size: 36px; }
        }
        @media (prefers-reduced-motion: reduce) { html { scroll-behavior: auto; } * { transition: none !important; } }
    </style>
</head>
<body>
<a class="skip" href="#konten">Lewati ke konten utama</a>
<header class="topbar">
    <div class="wrap topbar-inner">
        <a class="brand" href="#awal" aria-label="Beranda <%=e(profil.getNama())%>">
            <img src="<%=href(profil.getLogo(), root + "/img/logo.png")%>" alt="Logo <%=e(profil.getNama())%>">
            <span><strong><%=e(profil.getNama())%></strong><span>Portal ePesantren</span></span>
        </a>
        <nav aria-label="Navigasi utama">
            <a href="#profil">Profil</a><a href="#unit">Unit</a><a href="#layanan">Layanan</a><a href="#berita">Berita</a>
            <a class="button button-primary" href="<%=href(root + "/login", "#")%>">Masuk Sistem</a>
        </nav>
    </div>
</header>
<main id="konten">
    <section class="hero" id="awal">
        <div class="wrap hero-grid">
            <div>
                <div class="eyebrow">Satu Pesantren · Satu Sistem · Satu Data</div>
                <h1><%=e(profil.getNama())%></h1>
                <p class="hero-lead"><%=e(profil.getMotto())%>. Pendidikan, kesantrian, layanan wali, kesehatan, ekonomi, dan tata kelola kini hadir dalam satu ekosistem digital yang amanah.</p>
                <div class="hero-actions">
                    <a class="button button-primary" href="<%=href(root + "/login", "#")%>">Masuk ePesantren</a>
                    <a class="button button-light" href="#layanan">Jelajahi Layanan</a>
                    <a class="button button-outline" href="<%=href(root + "/psb", "#")%>">Pendaftaran Santri</a>
                </div>
            </div>
            <aside class="hero-card" id="profil" aria-label="Profil lembaga">
                <img src="<%=href(profil.getLogo(), root + "/img/logo.png")%>" alt="">
                <h2>Berakar pada adab, bertumbuh dengan teknologi</h2>
                <p><%=e(profil.getDeskripsi().isEmpty() ? "Portal ini menghubungkan seluruh unit pendidikan dan layanan pondok dalam pengalaman yang konsisten bagi santri, wali, asatidz, pengurus, dan pimpinan." : profil.getDeskripsi())%></p>
                <div class="hero-facts"><div class="fact"><strong><%=sekolahs.size()%></strong><span>Sekolah</span></div><div class="fact"><strong><%=perguruanTinggis.size()%></strong><span>Perguruan tinggi</span></div><div class="fact"><strong>24/7</strong><span>Layanan digital</span></div></div>
            </aside>
        </div>
    </section>

    <section id="unit">
        <div class="wrap">
            <div class="section-head"><div><span class="kicker">Jaringan pendidikan</span><h2>Satu yayasan, seluruh unit terhubung</h2></div><p>Profil unit berikut diambil langsung dari data AIS. Setiap pondok dapat menampilkan sekolah, madrasah, dan perguruan tinggi sesuai struktur yayasannya tanpa mengubah halaman.</p></div>
            <% if (sekolahs.isEmpty() && perguruanTinggis.isEmpty()) { %><div class="empty">Unit pendidikan belum dipublikasikan. Administrator dapat melengkapinya pada data Yayasan dan Sekolah.</div><% } else { %>
            <div class="unit-grid">
                <% for (UnitPendidikan unit : sekolahs) { %><article class="unit"><span class="label"><%=e(unit.getJenis())%></span><h3><%=e(unit.getNama())%></h3><p><%=e(unit.getMotto().isEmpty() ? unit.getAlamat() : unit.getMotto())%></p><a href="<%=href(unit.getUrl(), root + "/login")%>">Kunjungi unit →</a></article><% } %>
                <% for (UnitPendidikan unit : perguruanTinggis) { %><article class="unit"><span class="label"><%=e(unit.getJenis())%></span><h3><%=e(unit.getNama())%></h3><p><%=e(unit.getMotto().isEmpty() ? unit.getAlamat() : unit.getMotto())%></p><a href="<%=href(unit.getUrl(), root + "/login")%>">Kunjungi unit →</a></article><% } %>
            </div><% } %>
        </div>
    </section>

    <section class="pillars" id="ekosistem">
        <div class="wrap">
            <div class="section-head"><div><span class="kicker">Ekosistem ePesantren</span><h2>Delapan pilar untuk kehidupan pondok yang utuh</h2></div><p>Bukan kumpulan aplikasi terpisah. Data santri, pembelajaran, kesehatan, keuangan, dan unit usaha memakai identitas serta aturan akses yang sama.</p></div>
            <div class="pillar-grid">
                <article class="pillar"><h3>Pendidikan multi-jenjang</h3><p>eSchool, eCampus, diniyah, tahfiz, kursus, LMS, penilaian, dan pelaporan nasional.</p></article>
                <article class="pillar"><h3>Kesantrian & asrama</h3><p>Penempatan kamar, pengasuhan, izin keluar-masuk, kunjungan, prestasi, dan komunikasi wali.</p></article>
                <article class="pillar"><h3>Kesehatan pesantren</h3><p>Rekam medis eMedic, klinik, farmasi, pemeriksaan berkala, rujukan, dan notifikasi wali.</p></article>
                <article class="pillar"><h3>BMT & keuangan syariah</h3><p>Tabungan santri, simpanan, pembiayaan, ZIS, e-wallet, rekonsiliasi, dan laporan amanah.</p></article>
                <article class="pillar"><h3>Unit usaha & marketplace</h3><p>POS multi-gerai, koperasi, kantin, gudang, stok, QRIS, produk pesantren, dan laporan laba.</p></article>
                <article class="pillar"><h3>Back-office & tata kelola</h3><p>SDM, payroll, pengadaan, aset, persuratan, akuntansi, LPJ, audit, dan pengawasan.</p></article>
                <article class="pillar"><h3>Pustaka & repository</h3><p>Sirkulasi buku, kitab digital baca-saja ber-watermark, karya ilmiah, dan statistik pemanfaatan.</p></article>
                <article class="pillar"><h3>Kanal mandiri & AI</h3><p>Android, desktop, web, tablet, anjungan santri tanpa ponsel, serta AI dengan tinjauan manusia.</p></article>
            </div>
        </div>
    </section>

    <section class="biometric" id="biometrik">
        <div class="wrap bio-grid">
            <div><span class="eyebrow">Identitas biometrik local-first</span><h2>Satu verifikasi untuk layanan penting santri</h2><p>Sidik jari dan pengenalan wajah dapat digunakan untuk presensi mandiri, izin gerbang, dan verifikasi transaksi member di POS. Template biometrik terenkripsi disimpan di server sebagai sumber kebenaran dan disinkronkan ke perangkat berizin agar proses tetap cepat ketika koneksi terbatas.</p><a class="button button-light" href="<%=href(root + "/login", "#")%>">Kelola melalui akun berwenang</a></div>
            <div class="bio-flow">
                <div class="bio-step"><b>01</b><div><strong>Enrolmen dengan persetujuan</strong><span>Petugas berwenang mendaftarkan biometrik, perangkat, tujuan penggunaan, dan masa berlaku.</span></div></div>
                <div class="bio-step"><b>02</b><div><strong>Verifikasi cepat di perangkat</strong><span>Perangkat menyimpan cache terenkripsi terbatas; data mentah tidak dipakai sebagai pengganti template.</span></div></div>
                <div class="bio-step"><b>03</b><div><strong>Sinkronisasi idempoten</strong><span>Peristiwa offline masuk antrean lokal, dikirim ulang aman, dan tidak menggandakan presensi atau transaksi.</span></div></div>
                <div class="bio-step"><b>04</b><div><strong>Audit, pencabutan, dan fallback</strong><span>Setiap penggunaan tercatat. Admin dapat mencabut perangkat/template; kartu, PIN, dan verifikasi petugas tetap tersedia.</span></div></div>
            </div>
        </div>
    </section>

    <section class="services" id="layanan">
        <div class="wrap">
            <div class="section-head"><div><span class="kicker">Fasilitas digital</span><h2>Seluruh pintu layanan dalam satu halaman</h2></div><p>Tautan mengikuti fasilitas yang tersedia pada portal ERP AIS. Pondok dapat mengaktifkan modul secara bertahap sesuai kesiapan dan kebijakan akses.</p></div>
            <div class="service-columns">
                <div class="service-group"><h3>Pendidikan & publikasi</h3><div class="service-links">
                    <a href="<%=href(root + "/login", "#")%>">Login ePesantren</a><a href="<%=href(root + "/psb", "#")%>">Penerimaan santri baru</a><a href="<%=href(root + "/pmb", "#")%>">Penerimaan mahasiswa baru</a><a href="<%=href(root + "/alumni", "#")%>">Alumni & tracer study</a><a href="<%=href(root + "/pustaka", "#")%>">Perpustakaan digital</a><a href="<%=href(root + "/repository", "#")%>">Repository ilmiah</a><a href="<%=href(root + "/document", "#")%>">Dokumen institusi</a><a href="<%=href(root + "/dsh", "#")%>">Dashboard pimpinan</a>
                </div></div>
                <div class="service-group"><h3>Kesantrian & layanan mandiri</h3><div class="service-links">
                    <a href="<%=href(root + "/anjungan", "#")%>">Anjungan santri</a><a href="<%=href(root + "/welsis", "#")%>">Presensi siswa</a><a href="<%=href(root + "/tamu", "#")%>">Buku tamu</a><a href="<%=href(root + "/welpus", "#")%>">Pengunjung pustaka</a><a href="<%=href(root + "/antarJemput", "#")%>">Antar jemput</a><a href="<%=href(root + "/krrs", "#")%>">Sistem kursus</a><a href="<%=href(root + "/les", "#")%>">Les / privat</a><a href="<%=href(root + "/karir", "#")%>">Lowongan & karir</a>
                </div></div>
                <div class="service-group"><h3>Ekonomi & operasional</h3><div class="service-links">
                    <a href="<%=href(root + "/kantin", "#")%>">eKantin & koperasi</a><a href="<%=href(root + "/pos", "#")%>">Point of Sale</a><a href="<%=href(root + "/vendor", "#")%>">Portal rekanan</a><a href="https://apps.emedik.id">eMedic klinik pesantren</a><a href="<%=href(root + "/penawaran", "#")%>">Surat penawaran</a><a href="<%=href(root + "/presentasi", "#")%>">Presentasi solusi</a><a href="<%=href(root + "/proposal", "#")%>">Proposal ePesantren</a><a href="<%=href(root + "/pks", "#")%>">Dokumen kerja sama</a>
                </div></div>
            </div>
        </div>
    </section>

    <section id="berita">
        <div class="wrap">
            <div class="section-head"><div><span class="kicker">Kabar pondok</span><h2>Pengumuman umum terbaru</h2></div><p>Berita berasal dari Pengumuman Akademis yang aktif dan ditujukan untuk umum, dengan pembatasan otomatis sesuai yayasan atau unit pendidikan.</p></div>
            <% if (berita.isEmpty()) { %><div class="empty">Belum ada pengumuman umum yang dipublikasikan.</div><% } else { %><div class="news-grid"><% for (Berita item : berita) { %><article class="news"><time><%=e(item.getTanggal())%></time><h3><%=e(item.getJudul())%></h3><p><%=e(item.getRingkasan())%></p></article><% } %></div><% } %>
        </div>
    </section>

    <section class="contact" id="kontak"><div class="wrap"><div class="contact-card"><div><span class="eyebrow">Terhubung dengan pondok</span><h2>Informasi dan layanan resmi <%=e(profil.getNama())%></h2><p><%=e(profil.getAlamat())%></p><div class="hero-actions"><a class="button button-light" href="<%=href(root + "/login", "#")%>">Masuk Sistem</a><% if (!wa.isEmpty()) { %><a class="button button-outline" href="https://wa.me/<%=e(wa)%>">Hubungi WhatsApp</a><% } %></div></div><div class="contact-list"><% if (!profil.getTelepon().isEmpty()) { %><a href="tel:<%=e(digits(profil.getTelepon()))%>">Telepon · <%=e(profil.getTelepon())%></a><% } %><% if (!profil.getEmail().isEmpty()) { %><a href="mailto:<%=e(profil.getEmail())%>">Email · <%=e(profil.getEmail())%></a><% } %><% if (!profil.getWebsite().isEmpty()) { %><a href="<%=href(profil.getWebsite(), "#")%>">Website resmi</a><% } %><span>Data profil dapat disesuaikan dari master Yayasan AIS.</span></div></div></div></section>
</main>
<footer><div class="wrap footer-inner"><span>© <%=new java.text.SimpleDateFormat("yyyy").format(new java.util.Date())%> <%=e(profil.getNama())%></span><span>Didukung ePesantren · CV. Zishof</span></div></footer>
</body>
</html>
