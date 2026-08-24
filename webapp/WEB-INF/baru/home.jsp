<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.action.master.sekolah.util.SekolahUtil"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%!
private String getKonfigurasiValue(String key, String defaultValue) {
    try {
        Konfigurasi konfigurasi = Common.getKonfigurasi(key, defaultValue);
        if (konfigurasi != null && konfigurasi.getNilai() != null) {
            return konfigurasi.getNilai().trim();
        }
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/home.jsp:16");
    }
    return defaultValue == null ? "" : defaultValue;
}

private boolean isKonfigurasiAktif(String key, String defaultValue) {
    return getKonfigurasiValue(key, defaultValue).equalsIgnoreCase(Konfigurasi.AKTIF);
}

private String normalizeHomeLink(String value) {
    String root = Common.ROOT == null ? "" : Common.ROOT;
    String link = value == null ? "" : value.trim();
    if (link.length() == 0) {
        return root.length() == 0 ? "/" : root;
    }
    String lower = link.toLowerCase();
    if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("mailto:")
            || lower.startsWith("tel:") || lower.startsWith("#")) {
        return link;
    }
    if (root.length() > 0 && (link.equals(root) || link.startsWith(root + "/"))) {
        return link;
    }
    if (link.startsWith("/")) {
        return root + link;
    }
    return root + "/" + link;
}

private String getModulLink(String key, String defaultPath) {
    return normalizeHomeLink(getKonfigurasiValue(key, defaultPath));
}

private String safeText(String value) {
    if (value == null) {
        return "";
    }
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
            .replace("'", "&#39;");
}

private String homeText(String key, String defaultValue) {
    String translatedDefault = defaultValue == null ? "" : Common.getBahasaConfig(defaultValue);
    return safeText(getKonfigurasiValue(key, translatedDefault));
}
%>

<%
// 1. Pengambilan Data Perguruan Tinggi
PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
String judul = (pt != null && pt.getNama() != null) ? pt.getNama() : homeText("home_text_institusi_pendidikan", "Institusi Pendidikan");
String motto = (pt != null && pt.getMotto() != null) ? pt.getMotto() : "";
String telepon = (pt != null && pt.getTelepon() != null) ? pt.getTelepon() : "-";
String alamat1 = (pt != null && pt.getAlamat1() != null) ? pt.getAlamat1() : "-";
String email = (pt != null && pt.getEmail() != null) ? pt.getEmail() : "-";

// 2. Pengambilan Media (Logo dan Background)
String backgroundPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "background_perguruanTinggi_");
String logoPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
String bgImageFinal = (backgroundPerguruanTinggi != null && !backgroundPerguruanTinggi.isEmpty()) ? backgroundPerguruanTinggi : Common.ROOT + "/img/pmb_bg.jpg";

// 3. Konfigurasi Header & CSS Tema
String judulHeader = "eCampus";
String cssTema = "";
try {
    judulHeader = Common.getKonfigurasi("judul_header", "eCampus").getNilai();
    if (pt != null && pt.getCss() != null && !pt.getCss().trim().isEmpty()) {
        String rawCss = pt.getCss();
        String fileName = rawCss.substring(rawCss.lastIndexOf("/") + 1);
        cssTema = "/css/baru/" + fileName;
    }
} catch (Exception e) {
    judulHeader = "eCampus";
}

long vCache = System.currentTimeMillis();
String judulKecilHome = getKonfigurasiValue("judul_kecil_home_portal", "Enterprise Education Portal");
String deskripsiHomePortal = getKonfigurasiValue("deskripsi_home_portal",
        "Satu pintu layanan digital untuk mempercepat akses informasi, pendaftaran, akademik, dokumen, perpustakaan, repository, layanan sekolah, dan sistem pendukung operasional institusi pendidikan secara lebih tertib, modern, dan mudah digunakan.");
boolean tampilkanJudulKecilHomePortal = isKonfigurasiAktif("tampilkan_judul_kecil_home_portal", Konfigurasi.AKTIF);
boolean tampilkanDeskripsiHeaderHome = isKonfigurasiAktif("tampilkan_deskripsi_home_portal", Konfigurasi.TIDAK_AKTIF);
boolean tampilkanPortalLayananTerpadu = isKonfigurasiAktif("tampilkan_section_portal_layanan_terpadu", Konfigurasi.TIDAK_AKTIF);
boolean tampilkanRingkasanManfaatPortalHome = isKonfigurasiAktif("tampilkan_ringkasan_manfaat_portal_home", Konfigurasi.TIDAK_AKTIF);
boolean tampilkanDeskripsiPortalPerguruanTinggi = isKonfigurasiAktif("tampilkan_deskripsi_portal_perguruan_tinggi_home", Konfigurasi.TIDAK_AKTIF);

// 4. Deteksi tipe institusi. Fitur sekolah hanya tampil saat yaData=true, fitur PT hanya tampil saat ptData=true.
boolean ptData = true;
boolean yaData = true;
try {
    boolean[] ptYa = Common.chekPtAtauSekolah(null);
    ptData = ptYa[0];
    yaData = ptYa[1];
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/home.jsp:109");
} finally {
}

// 5. Konfigurasi modul yang ditampilkan pada halaman depan.
boolean tampilkanModuleLoginEcampus = isKonfigurasiAktif("tampilkan_modul_login_ecampus", Konfigurasi.AKTIF);
boolean tampilkanModuleDashboard = isKonfigurasiAktif("tampilkan_modul_dashboard_pimpinan", Konfigurasi.AKTIF);
boolean tampilkanModulePmb = isKonfigurasiAktif("tampilkan_modul_pmb", Konfigurasi.AKTIF);
boolean tampilkanModuleAlumni = isKonfigurasiAktif("tampilkan_modul_alumni", Konfigurasi.AKTIF);
boolean tampilkanModulePustaka = isKonfigurasiAktif("tampilkan_modul_pustaka", Konfigurasi.AKTIF);
boolean tampilkanModuleKantin = isKonfigurasiAktif("tampilkan_modul_e_kantin", Konfigurasi.AKTIF);
boolean tampilkanModuleDokumen = isKonfigurasiAktif("tampilkan_modul_dokumen", Konfigurasi.AKTIF);
boolean tampilkanModuleRepository = isKonfigurasiAktif("tampilkan_modul_repository", Konfigurasi.AKTIF);
boolean tampilkanModuleFeederEmis = isKonfigurasiAktif("tampilkan_modul_feeder_emis", Konfigurasi.TIDAK_AKTIF);
boolean tampilkanModuleAkreditasi = isKonfigurasiAktif("tampilkan_modul_akreditasi", Konfigurasi.TIDAK_AKTIF);
boolean tampilkanModuleEjournal = isKonfigurasiAktif("tampilkan_modul_ejournal", Konfigurasi.TIDAK_AKTIF);
boolean tampilkanModuleSimlitabmas = isKonfigurasiAktif("tampilkan_modul_simlitabmas", Konfigurasi.TIDAK_AKTIF);

boolean tampilkanModulePortalSekolah = isKonfigurasiAktif("tampilkan_modul_portal_sekolah", Konfigurasi.AKTIF);
boolean tampilkanModulePpdbSekolah = isKonfigurasiAktif("tampilkan_modul_ppdb_sekolah", Konfigurasi.AKTIF);
boolean tampilkanModuleLoginSiswaWali = isKonfigurasiAktif("tampilkan_modul_login_siswa_wali", Konfigurasi.AKTIF);
boolean tampilkanModuleAbsenSiswa = isKonfigurasiAktif("tampilkan_modul_absen_siswa", Konfigurasi.AKTIF);

boolean tampilkanModuleAnjungan = isKonfigurasiAktif("tampilkan_modul_anjungan", Konfigurasi.AKTIF);
boolean tampilkanModulePos = isKonfigurasiAktif("tampilkan_modul_pos", Konfigurasi.TIDAK_AKTIF);
boolean tampilkanModuleKursus = isKonfigurasiAktif("tampilkan_modul_kursus", Konfigurasi.TIDAK_AKTIF);
boolean tampilkanModuleLes = isKonfigurasiAktif("tampilkan_modul_les_private", Konfigurasi.TIDAK_AKTIF);
boolean tampilkanModuleKarir = isKonfigurasiAktif("tampilkan_modul_karir", Konfigurasi.AKTIF);
boolean tampilkanModulePengunjungPustaka = isKonfigurasiAktif("tampilkan_modul_pengunjung_pustaka", Konfigurasi.AKTIF);
boolean tampilkanModuleBukuTamu = isKonfigurasiAktif("tampilkan_modul_buku_tamu", Konfigurasi.AKTIF);
boolean tampilkanModulePortalRekanan = isKonfigurasiAktif("tampilkan_modul_portal_rekanan", Konfigurasi.AKTIF);

// 6. Link kustom modul. Default disimpan sebagai path relatif agar aman saat context-path berubah.
String linkLoginEcampus = getModulLink("link_modul_login_ecampus", "/login");
String linkDashboard = getModulLink("link_modul_dashboard_pimpinan", "/dsh");
String linkPmb = getModulLink("link_modul_pmb", "/pmb");
String linkAlumni = getModulLink("link_modul_alumni", "/alumni");
String linkPustaka = getModulLink("link_modul_pustaka", "/pustaka");
String linkKantin = getModulLink("link_modul_e_kantin", "/kantin");
String linkDokumen = getModulLink("link_modul_dokumen", "/document");
String linkRepository = getModulLink("link_modul_repository", "/repository");
String linkFeederEmis = getModulLink("link_modul_feeder_emis", "/login");
String linkAkreditasi = getModulLink("link_modul_akreditasi", "/login");
String linkEjournal = getModulLink("link_modul_ejournal", "/login");
String linkSimlitabmas = getModulLink("link_modul_simlitabmas", "/login");

String linkPortalSekolah = getModulLink("link_modul_portal_sekolah", "/login");
String linkPpdbSekolah = getModulLink("link_modul_ppdb_sekolah", "/ppdb");
String linkLoginSiswaWali = getModulLink("link_modul_login_siswa_wali", "/login");
String linkAbsenSiswa = getModulLink("link_modul_absen_siswa", "/welsis");

String linkAnjungan = getModulLink("link_modul_anjungan", "/anjungan");
String linkPos = getModulLink("link_modul_pos", "/pos");
String linkKursus = getModulLink("link_modul_kursus", "/krrs");
String linkLes = getModulLink("link_modul_les_private", "/les");
String linkKarir = getModulLink("link_modul_karir", "/karir");
String linkPengunjungPustaka = getModulLink("link_modul_pengunjung_pustaka", "/welpus");
String linkBukuTamu = getModulLink("link_modul_buku_tamu", "/tamu");
String linkPortalRekanan = getModulLink("link_modul_portal_rekanan", "/vendor");

// 7. Tombol tambahan kustom. Jika aktif tetapi Label atau URL kosong, tombol tidak ditampilkan.
boolean[] tampilkanButtonTambahan = new boolean[10];
String[] labelButtonTambahan = new String[10];
String[] linkButtonTambahan = new String[10];
String[] deskripsiButtonTambahan = new String[10];
String[] iconButtonTambahan = new String[10];
boolean adaButtonTambahan = false;
for (int i = 0; i < 10; i++) {
    String nomor = (i + 1) < 10 ? "0" + (i + 1) : String.valueOf(i + 1);
    String labelTambahan = getKonfigurasiValue("label_button_tambahan_home_" + nomor, "");
    String linkTambahanRaw = getKonfigurasiValue("link_button_tambahan_home_" + nomor, "");
    boolean aktifTambahan = isKonfigurasiAktif("tampilkan_button_tambahan_home_" + nomor, Konfigurasi.TIDAK_AKTIF);
    boolean validTambahan = aktifTambahan && labelTambahan != null && labelTambahan.trim().length() > 0
            && linkTambahanRaw != null && linkTambahanRaw.trim().length() > 0;
    tampilkanButtonTambahan[i] = validTambahan;
    labelButtonTambahan[i] = labelTambahan == null ? "" : labelTambahan.trim();
    linkButtonTambahan[i] = validTambahan ? normalizeHomeLink(linkTambahanRaw) : "#";
    deskripsiButtonTambahan[i] = getKonfigurasiValue("deskripsi_button_tambahan_home_" + nomor,
            "Layanan tambahan yang dikonfigurasi oleh admin institusi.");
    iconButtonTambahan[i] = getKonfigurasiValue("icon_button_tambahan_home_" + nomor, "fas fa-link");
    if (iconButtonTambahan[i] == null || iconButtonTambahan[i].trim().length() == 0) {
        iconButtonTambahan[i] = "fas fa-link";
    }
    if (validTambahan) {
        adaButtonTambahan = true;
    }
}

boolean adaPortalPerguruanTinggi = ptData && (tampilkanModuleLoginEcampus || tampilkanModulePmb || tampilkanModuleAlumni
        || tampilkanModulePustaka || tampilkanModuleDokumen || tampilkanModuleRepository || tampilkanModuleDashboard
        || tampilkanModuleFeederEmis || tampilkanModuleAkreditasi || tampilkanModuleEjournal || tampilkanModuleSimlitabmas);
boolean adaPortalSekolah = yaData && (tampilkanModulePortalSekolah || tampilkanModulePpdbSekolah || tampilkanModuleLoginSiswaWali
        || tampilkanModuleAbsenSiswa || tampilkanModuleBukuTamu);
boolean adaSistemPendukung = tampilkanModuleKantin || tampilkanModuleAnjungan || tampilkanModulePos || tampilkanModuleKursus
        || tampilkanModuleLes || tampilkanModuleKarir || tampilkanModulePengunjungPustaka || tampilkanModuleBukuTamu
        || tampilkanModulePortalRekanan || (yaData && tampilkanModuleAbsenSiswa) || adaButtonTambahan;
%>

<!DOCTYPE html>
<html lang="<%=ais.common.Common.kodeBahasaAktif()%>" dir="<%= "ar".equals(ais.common.Common.kodeBahasaAktif()) ? "rtl" : "ltr" %>">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= homeText("home_text_enterprise_education_portal_terpadu", "Enterprise Education - Portal Terpadu") %> | <%= safeText(judul) %></title>
    <meta name="description" content="Portal digital layanan pendidikan terpadu untuk kampus, sekolah, yayasan, pesantren, pendaftaran, perpustakaan, dokumen, repository, eKantin, karir, dan layanan pendukung institusi.">
    <link rel="icon" href="<%= logoPerguruanTinggi %>" type="image/x-icon">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
    <link href="<%=Common.ROOT%>/css/baru/base-theme.css?v=<%=vCache%>" rel="stylesheet">
    <% if(!cssTema.isEmpty()){ %>
        <link href="<%=Common.ROOT%><%=cssTema%>?v=<%=vCache%>" rel="stylesheet">
    <% } %>
    <%-- Seluruh CSS tampilan home dipusatkan di /css/baru/base-theme.css dan warna mengikuti cssTema. --%>
</head>

<body class="home-page-modern" style="--home-bg-image: url('<%= safeText(bgImageFinal) %>
<jsp:include page="/WEB-INF/baru/include/pemilih_bahasa.jsp" />');">
    <div class="bg-pattern-home"></div>
    <div class="aurora-home aurora-one"></div>
    <div class="aurora-home aurora-two"></div>

    <nav class="topbar-home py-2">
        <div class="container d-flex align-items-center justify-content-between">
            <a href="#top" class="brand-chip-home">
                <img src="<%= logoPerguruanTinggi %>" alt="<%= homeText("home_text_logo_institusi", "Logo Institusi") %>">
                <span><%= safeText(judulHeader) %></span>
            </a>
            <div class="d-flex align-items-center gap-1">
                <a href="#portal" class="topbar-link-home"><%= homeText("home_text_portal", "Portal") %></a>
                <a href="#pendukung" class="topbar-link-home"><%= homeText("home_text_sistem_pendukung", "Sistem Pendukung") %></a>
                <a href="#kontak" class="topbar-link-home"><%= homeText("home_text_kontak", "Kontak") %></a>
                <a href="<%= linkLoginEcampus %>" target="_blank" class="topbar-link-home"><i class="fas fa-right-to-bracket me-1"></i><%= homeText("home_text_login", "Login") %></a>
            </div>
        </div>
    </nav>

    <header id="top" class="hero-section-home text-center">
        <div class="container hero-content-home">
            <div class="logo-container-home">
                <img src="<%= logoPerguruanTinggi %>" alt="<%= homeText("home_text_logo_institusi", "Logo Institusi") %>" class="logo-img-home"/>
            </div>
            <% if (tampilkanJudulKecilHomePortal && judulKecilHome != null && judulKecilHome.trim().length() > 0) { %>
            <div class="hero-eyebrow">
                <i class="fas fa-layer-group"></i>
                <span><%= safeText(judulKecilHome) %></span>
            </div>
            <% } %>
            <h1 class="hero-title-home"><%= safeText(judul) %></h1>
            <% if (!motto.isEmpty()) { %>
            <p class="text-white-50 mb-3 fw-medium fs-5 fst-italic">"<%= safeText(motto) %>"</p>
            <% } %>
            <% if (tampilkanDeskripsiHeaderHome && deskripsiHomePortal != null && deskripsiHomePortal.trim().length() > 0) { %>
            <p class="hero-subtitle-home">
                <%= safeText(deskripsiHomePortal) %>
            </p>
            <% } %>
            <div class="hero-actions-home">
                <a href="<%= linkLoginEcampus %>" target="_blank" class="hero-btn-home hero-btn-primary-home">
                    <i class="fas fa-desktop"></i><%= homeText("home_text_masuk_ke_sistem", "Masuk ke Sistem") %>
                </a>
                <a href="#portal" class="hero-btn-home hero-btn-outline-home">
                    <i class="fas fa-th-large"></i><%= homeText("home_text_lihat_portal_layanan", "Lihat Portal Layanan") %>
                </a>
                <% if ((ptData && tampilkanModulePmb) || (yaData && tampilkanModulePpdbSekolah)) { %>
                <a href="<%= ptData ? linkPmb : linkPpdbSekolah %>" target="_blank" class="hero-btn-home hero-btn-outline-home">
                    <i class="fas fa-user-plus"></i><%= homeText("home_text_pendaftaran_online", "Pendaftaran Online") %>
                </a>
                <% } %>
            </div>
        </div>
    </header>

    <main class="container main-home-wrap">
        <div class="row justify-content-center">
            <div class="col-xl-11">
                <div class="menu-card-home text-center" id="portal">
                    <% if (tampilkanPortalLayananTerpadu) { %>
                    <div class="section-eyebrow-home"><i class="fas fa-star"></i><%= homeText("home_text_portal_layanan_terpadu", "Portal Layanan Terpadu") %></div>
                    <h2 class="section-title-home"><%= homeText("home_text_akses_cepat_layanan_digital_institusi", "Akses Cepat Layanan Digital Institusi") %></h2>
                    <p class="section-desc-home">
                        <%= homeText("home_text_setiap_tombol_layanan_di_bawah_ditampilkan_berdasarkan_tipe_institusi_da", "Setiap tombol layanan di bawah ditampilkan berdasarkan tipe institusi dan konfigurasi modul yang diaktifkan oleh admin. Dengan demikian, halaman depan tetap rapi, relevan, dan hanya menampilkan portal yang benar-benar digunakan oleh institusi.") %>
                    </p>

                    <% if (tampilkanRingkasanManfaatPortalHome) { %>
                    <div class="benefit-grid-home">
                        <div class="benefit-card-home">
                            <i class="fas fa-gauge-high"></i>
                            <strong><%= homeText("home_text_akses_lebih_cepat", "Akses Lebih Cepat") %></strong>
                            <span><%= homeText("home_text_pengguna_langsung_diarahkan_ke_layanan_utama_tanpa_harus_mencari_menu_te", "Pengguna langsung diarahkan ke layanan utama tanpa harus mencari menu terlalu jauh.") %></span>
                        </div>
                        <div class="benefit-card-home">
                            <i class="fas fa-shield-halved"></i>
                            <strong><%= homeText("home_text_terukur_dan_terkendali", "Terukur & Terkendali") %></strong>
                            <span><%= homeText("home_text_modul_dapat_dibuka_atau_ditutup_dari_pusat_konfigurasi_sesuai_kebutuhan", "Modul dapat dibuka atau ditutup dari pusat konfigurasi sesuai kebutuhan operasional.") %></span>
                        </div>
                        <div class="benefit-card-home">
                            <i class="fas fa-network-wired"></i>
                            <strong><%= homeText("home_text_ekosistem_terpadu", "Ekosistem Terpadu") %></strong>
                            <span><%= homeText("home_text_kampus_sekolah_yayasan_pesantren_unit_usaha_dan_layanan_publik_berada_da", "Kampus, sekolah, yayasan, pesantren, unit usaha, dan layanan publik berada dalam satu pintu.") %></span>
                        </div>
                        <div class="benefit-card-home">
                            <i class="fas fa-mobile-screen-button"></i>
                            <strong><%= homeText("home_text_ramah_perangkat", "Ramah Perangkat") %></strong>
                            <span><%= homeText("home_text_tampilan_disusun_responsif_agar_nyaman_digunakan_dari_desktop_tablet_mau", "Tampilan disusun responsif agar nyaman digunakan dari desktop, tablet, maupun ponsel.") %></span>
                        </div>
                    </div>
                    <% } %>
                    <% } %>

                    <% if (adaPortalPerguruanTinggi) { %>
                    <section class="portal-group-section mt-4 text-start">
                        <div class="portal-group-title"><i class="fas fa-university"></i><%= homeText("home_text_portal_perguruan_tinggi", "Portal Perguruan Tinggi") %></div>
                        <div class="portal-support-copy">
                            <i class="fas fa-graduation-cap"></i>
                            <div>
                                <strong><%= homeText("home_text_layanan_digital_untuk_tata_kelola_kampus_yang_lebih_terintegrasi", "Layanan digital untuk tata kelola kampus yang lebih terintegrasi") %></strong>
                                <% if (tampilkanDeskripsiPortalPerguruanTinggi) { %>
                                <span><%= homeText("home_text_kelompok_menu_ini_disiapkan_untuk_mendukung_layanan_kampus_mulai_dari_lo", "Kelompok menu ini disiapkan untuk mendukung layanan kampus mulai dari login eCampus, penerimaan mahasiswa baru, tracer study, dokumen, dashboard, pustaka digital, repository, pelaporan, akreditasi, jurnal, hingga penelitian sesuai modul yang diaktifkan.") %></span>
                                <% } %>
                            </div>
                        </div>
                        <div class="portal-action-grid">
                            <% if (tampilkanModuleLoginEcampus) { %>
                            <a href="<%= linkLoginEcampus %>" target="_blank" class="portal-card-home">
                                <span class="portal-icon-home"><i class="fas fa-right-to-bracket"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_login_ecampus", "Login eCampus") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_akses_utama_admin_dosen_mahasiswa_pimpinan_dan_operator_kampus", "Akses utama admin, dosen, mahasiswa, pimpinan, dan operator kampus.") %></span>
                            </a>
                            <% } %>
                            <% if (tampilkanModulePmb) { %>
                            <a href="<%= linkPmb %>" target="_blank" class="portal-card-home">
                                <span class="portal-icon-home"><i class="fas fa-user-graduate"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_mahasiswa_baru", "Mahasiswa Baru") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_pendaftaran_mahasiswa_baru_informasi_jalur_masuk_dan_proses_registrasi_o", "Pendaftaran mahasiswa baru, informasi jalur masuk, dan proses registrasi online.") %></span>
                            </a>
                            <% } %>
                            <% if (tampilkanModuleAlumni) { %>
                            <a href="<%= linkAlumni %>" target="_blank" class="portal-card-home">
                                <span class="portal-icon-home"><i class="fas fa-briefcase"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_tracer_study", "Tracer Study") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_pendataan_alumni_umpan_balik_lulusan_dan_dukungan_pelaporan_kinerja_lulu", "Pendataan alumni, umpan balik lulusan, dan dukungan pelaporan kinerja lulusan.") %></span>
                            </a>
                            <% } %>
                            <% if (tampilkanModulePustaka) { %>
                            <a href="<%= linkPustaka %>" target="_blank" class="portal-card-home">
                                <span class="portal-icon-home"><i class="fas fa-book-open"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_e_library", "E-Library") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_layanan_katalog_pustaka_sirkulasi_dan_akses_informasi_perpustakaan_digit", "Layanan katalog pustaka, sirkulasi, dan akses informasi perpustakaan digital.") %></span>
                            </a>
                            <% } %>
                            <% if (tampilkanModuleDokumen) { %>
                            <a href="<%= linkDokumen %>" target="_blank" class="portal-card-home">
                                <span class="portal-icon-home"><i class="fas fa-folder-open"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_dokumen", "Dokumen") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_publikasi_dokumen_arsip_pedoman_dan_file_resmi_sesuai_hak_akses_pengguna", "Publikasi dokumen, arsip, pedoman, dan file resmi sesuai hak akses pengguna.") %></span>
                            </a>
                            <% } %>
                            <% if (tampilkanModuleRepository) { %>
                            <a href="<%= linkRepository %>" target="_blank" class="portal-card-home">
                                <span class="portal-icon-home"><i class="fas fa-archive"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_e_repository", "E-Repository") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_pusat_penyimpanan_karya_ilmiah_publikasi_dan_dokumen_akademik_institusi", "Pusat penyimpanan karya ilmiah, publikasi, dan dokumen akademik institusi.") %></span>
                            </a>
                            <% } %>
                            <% if (tampilkanModuleDashboard) { %>
                            <a href="<%= linkDashboard %>" target="_blank" class="portal-card-home outline">
                                <span class="portal-icon-home"><i class="fas fa-chart-pie"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_dashboard", "Dashboard") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_ringkasan_data_strategis_untuk_pemantauan_pimpinan_dan_unit_kerja", "Ringkasan data strategis untuk pemantauan pimpinan dan unit kerja.") %></span>
                            </a>
                            <% } %>
                            <% if (tampilkanModuleFeederEmis) { %>
                            <a href="<%= linkFeederEmis %>" target="_blank" class="portal-card-home outline">
                                <span class="portal-icon-home"><i class="fas fa-sync-alt"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_feeder_dan_emis", "Feeder & EMIS") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_dukungan_integrasi_pelaporan_eksternal_sesuai_kebutuhan_institusi", "Dukungan integrasi pelaporan eksternal sesuai kebutuhan institusi.") %></span>
                            </a>
                            <% } %>
                            <% if (tampilkanModuleAkreditasi) { %>
                            <a href="<%= linkAkreditasi %>" target="_blank" class="portal-card-home outline">
                                <span class="portal-icon-home"><i class="fas fa-certificate"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_akreditasi", "Akreditasi") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_pengelolaan_dokumen_dan_data_pendukung_akreditasi_secara_lebih_rapi", "Pengelolaan dokumen dan data pendukung akreditasi secara lebih rapi.") %></span>
                            </a>
                            <% } %>
                            <% if (tampilkanModuleEjournal) { %>
                            <a href="<%= linkEjournal %>" target="_blank" class="portal-card-home outline">
                                <span class="portal-icon-home"><i class="fas fa-newspaper"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_e_journal", "E-Journal") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_akses_publikasi_jurnal_artikel_dan_kanal_ilmiah_institusi", "Akses publikasi jurnal, artikel, dan kanal ilmiah institusi.") %></span>
                            </a>
                            <% } %>
                            <% if (tampilkanModuleSimlitabmas) { %>
                            <a href="<%= linkSimlitabmas %>" target="_blank" class="portal-card-home outline">
                                <span class="portal-icon-home"><i class="fas fa-microscope"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_simlitabmas", "Simlitabmas") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_dukungan_informasi_penelitian_dan_pengabdian_kepada_masyarakat", "Dukungan informasi penelitian dan pengabdian kepada masyarakat.") %></span>
                            </a>
                            <% } %>
                        </div>
                        <div class="portal-support-note"><i class="fas fa-check-circle me-2"></i><%= homeText("home_text_portal_perguruan_tinggi_hanya_ditampilkan_saat_sistem_terdeteksi_sebagai", "Portal perguruan tinggi hanya ditampilkan saat sistem terdeteksi sebagai PT dan modul terkait aktif di konfigurasi.") %></div>
                    </section>
                    <% } %>

                    <% if (adaPortalSekolah) { %>
                    <section class="portal-group-section mt-4 text-start">
                        <div class="portal-group-title"><i class="fas fa-school"></i><%= homeText("home_text_portal_sekolah_yayasan_dan_pondok_pesantren", "Portal Sekolah, Yayasan, dan Pondok Pesantren") %></div>
                        <div class="portal-support-copy">
                            <i class="fas fa-children"></i>
                            <div>
                                <strong><%= homeText("home_text_portal_operasional_sekolah_dan_yayasan_dalam_satu_halaman_layanan", "Portal operasional sekolah dan yayasan dalam satu halaman layanan") %></strong>
                                <span><%= homeText("home_text_kelompok_menu_ini_mendukung_layanan_sekolah_yayasan_dan_pesantren_sepert", "Kelompok menu ini mendukung layanan sekolah, yayasan, dan pesantren seperti login pengelola, PPDB online, akses siswa/wali, absensi siswa, serta layanan tamu sesuai konfigurasi yang diaktifkan oleh admin.") %></span>
                            </div>
                        </div>
                        <div class="portal-mini-label"><i class="fas fa-door-open"></i><%= homeText("home_text_portal_sekolah_dan_pesantren", "Portal Sekolah & Pesantren") %></div>
                        <div class="portal-action-grid">
                            <% if (tampilkanModulePortalSekolah) { %>
                            <a href="<%= linkPortalSekolah %>" target="_blank" class="portal-card-home dark">
                                <span class="portal-icon-home"><i class="fas fa-building-columns"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_portal_sekolah_yayasan", "Portal Sekolah / Yayasan") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_akses_pengelola_admin_unit_guru_operator_dan_pimpinan_sekolah_yayasan", "Akses pengelola, admin unit, guru, operator, dan pimpinan sekolah/yayasan.") %></span>
                            </a>
                            <% } %>
                            <% if (tampilkanModulePpdbSekolah) { %>
                            <a href="<%= linkPpdbSekolah %>" target="_blank" class="portal-card-home success">
                                <span class="portal-icon-home"><i class="fas fa-id-card"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_ppdb_online", "PPDB Online") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_pendaftaran_peserta_didik_baru_pengisian_data_calon_siswa_dan_informasi", "Pendaftaran peserta didik baru, pengisian data calon siswa, dan informasi penerimaan.") %></span>
                            </a>
                            <% } %>
                            <% if (tampilkanModuleLoginSiswaWali) { %>
                            <a href="<%= linkLoginSiswaWali %>" target="_blank" class="portal-card-home dark">
                                <span class="portal-icon-home"><i class="fas fa-users"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_login_siswa_wali", "Login Siswa / Wali") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_akses_informasi_akademik_pembayaran_kehadiran_dan_layanan_sekolah_untuk", "Akses informasi akademik, pembayaran, kehadiran, dan layanan sekolah untuk siswa/wali.") %></span>
                            </a>
                            <% } %>
                            <% if (tampilkanModuleAbsenSiswa) { %>
                            <a href="<%= linkAbsenSiswa %>" target="_blank" class="portal-card-home dark">
                                <span class="portal-icon-home"><i class="fas fa-user-clock"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_absensi_siswa", "Absensi Siswa") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_pintu_cepat_untuk_layanan_presensi_siswa_sesuai_kebutuhan_operasional_se", "Pintu cepat untuk layanan presensi siswa sesuai kebutuhan operasional sekolah.") %></span>
                            </a>
                            <% } %>
                            <% if (tampilkanModuleBukuTamu) { %>
                            <a href="<%= linkBukuTamu %>" target="_blank" class="portal-card-home dark">
                                <span class="portal-icon-home"><i class="fas fa-book-open"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_buku_tamu", "Buku Tamu") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_pencatatan_tamu_kunjungan_dan_layanan_penerimaan_pengunjung_secara_digit", "Pencatatan tamu, kunjungan, dan layanan penerimaan pengunjung secara digital.") %></span>
                            </a>
                            <% } %>
                        </div>
                        <div class="portal-support-note"><i class="fas fa-check-circle me-2"></i><%= homeText("home_text_portal_sekolah_yayasan_pesantren_hanya_ditampilkan_saat_sistem_terdeteks", "Portal sekolah/yayasan/pesantren hanya ditampilkan saat sistem terdeteksi sebagai sekolah atau yayasan dan modul terkait aktif di konfigurasi.") %></div>
                    </section>
                    <% } %>

                    <% if (adaSistemPendukung) { %>
                    <section id="pendukung" class="portal-group-section mt-4 text-start">
                        <div class="portal-group-title"><i class="fas fa-puzzle-piece"></i><%= homeText("home_text_sistem_pendukung", "Sistem Pendukung") %></div>
                        <div class="portal-support-copy">
                            <i class="fas fa-layer-group"></i>
                            <div>
                                <strong><%= homeText("home_text_layanan_pendukung_untuk_memperluas_kualitas_pengalaman_digital_institusi", "Layanan pendukung untuk memperluas kualitas pengalaman digital institusi") %></strong>
                                <span><%= homeText("home_text_selain_layanan_inti_akademik_dan_administrasi_institusi_dapat_menampilka", "Selain layanan inti akademik dan administrasi, institusi dapat menampilkan pintu masuk untuk eKantin, anjungan mandiri, POS, kursus, karir, tamu, pengunjung pustaka, portal rekanan, dan layanan pendukung lain yang relevan.") %></span>
                            </div>
                        </div>
                        <div class="portal-action-grid">
                            <% if (tampilkanModuleKantin) { %>
                            <a href="<%= linkKantin %>" target="_blank" class="portal-card-home accent">
                                <span class="portal-icon-home"><i class="fas fa-store"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_ekantin", "eKantin") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_layanan_transaksi_kantin_unit_usaha_yang_lebih_praktis_dan_terdokumentas", "Layanan transaksi kantin/unit usaha yang lebih praktis dan terdokumentasi.") %></span>
                            </a>
                            <% } %>
                            <% if (tampilkanModuleAnjungan) { %>
                            <a href="<%= linkAnjungan %>" target="_blank" class="portal-card-home accent">
                                <span class="portal-icon-home"><i class="fas fa-desktop"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_anjungan", "Anjungan") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_layanan_mandiri_untuk_akses_informasi_dan_transaksi_tertentu_di_area_pub", "Layanan mandiri untuk akses informasi dan transaksi tertentu di area publik institusi.") %></span>
                            </a>
                            <% } %>
                            <% if (tampilkanModulePos) { %>
                            <a href="<%= linkPos %>" target="_blank" class="portal-card-home accent">
                                <span class="portal-icon-home"><i class="fas fa-cash-register"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_point_of_sale", "Point Of Sale") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_pengelolaan_penjualan_transaksi_kasir_dan_unit_usaha_institusi", "Pengelolaan penjualan, transaksi kasir, dan unit usaha institusi.") %></span>
                            </a>
                            <% } %>
                            <% if (tampilkanModuleKursus) { %>
                            <a href="<%= linkKursus %>" target="_blank" class="portal-card-home accent">
                                <span class="portal-icon-home"><i class="fas fa-chalkboard-teacher"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_sistem_kursus", "Sistem Kursus") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_pengelolaan_program_kursus_kelas_tambahan_jadwal_dan_peserta", "Pengelolaan program kursus, kelas tambahan, jadwal, dan peserta.") %></span>
                            </a>
                            <% } %>
                            <% if (tampilkanModuleLes) { %>
                            <a href="<%= linkLes %>" target="_blank" class="portal-card-home accent">
                                <span class="portal-icon-home"><i class="fas fa-user-edit"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_sistem_les_private", "Sistem Les / Private") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_layanan_pendampingan_belajar_private_class_dan_pengelolaan_peserta_bimbi", "Layanan pendampingan belajar, private class, dan pengelolaan peserta bimbingan.") %></span>
                            </a>
                            <% } %>
                            <% if (tampilkanModuleKarir) { %>
                            <a href="<%= linkKarir %>" target="_blank" class="portal-card-home featured">
                                <span class="portal-icon-home"><i class="fas fa-user-tie"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_lowongan_pekerjaan_karir", "Lowongan Pekerjaan / Karir") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_portal_rekrutmen_resmi_untuk_publikasi_lowongan_seleksi_calon_pegawai_da", "Portal rekrutmen resmi untuk publikasi lowongan, seleksi calon pegawai, dan informasi karir institusi.") %></span>
                            </a>
                            <% } %>
                            <% if (tampilkanModulePengunjungPustaka) { %>
                            <a href="<%= linkPengunjungPustaka %>" target="_blank" class="portal-card-home accent">
                                <span class="portal-icon-home"><i class="fas fa-address-book"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_pengunjung_pustaka", "Pengunjung Pustaka") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_pencatatan_kunjungan_perpustakaan_secara_digital_dan_mudah_dipantau", "Pencatatan kunjungan perpustakaan secara digital dan mudah dipantau.") %></span>
                            </a>
                            <% } %>
                            <% if (tampilkanModuleBukuTamu) { %>
                            <a href="<%= linkBukuTamu %>" target="_blank" class="portal-card-home accent">
                                <span class="portal-icon-home"><i class="fas fa-book-open"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_buku_tamu", "Buku Tamu") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_layanan_pencatatan_tamu_dan_kunjungan_agar_lebih_tertib_dan_terdokumenta", "Layanan pencatatan tamu dan kunjungan agar lebih tertib dan terdokumentasi.") %></span>
                            </a>
                            <% } %>
                            <% if (yaData && tampilkanModuleAbsenSiswa) { %>
                            <a href="<%= linkAbsenSiswa %>" target="_blank" class="portal-card-home accent">
                                <span class="portal-icon-home"><i class="fas fa-user-clock"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_absen_siswa", "Absen Siswa") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_akses_cepat_pencatatan_kehadiran_siswa_untuk_kebutuhan_sekolah", "Akses cepat pencatatan kehadiran siswa untuk kebutuhan sekolah.") %></span>
                            </a>
                            <% } %>
                            <% if (tampilkanModulePortalRekanan) { %>
                            <a href="<%= linkPortalRekanan %>" target="_blank" class="portal-card-home accent">
                                <span class="portal-icon-home"><i class="fas fa-truck-loading"></i></span>
                                <span class="portal-title-home"><%= homeText("home_text_portal_rekanan", "Portal Rekanan") %></span>
                                <span class="portal-desc-small"><%= homeText("home_text_akses_informasi_dan_layanan_rekanan_vendor_untuk_kebutuhan_pengadaan_ins", "Akses informasi dan layanan rekanan/vendor untuk kebutuhan pengadaan institusi.") %></span>
                            </a>
                            <% } %>
                            <% if (adaButtonTambahan) { %>
                            <div class="portal-custom-divider"><i class="fas fa-plus-circle"></i><span><%= homeText("home_text_layanan_kustom_tambahan", "Layanan Kustom Tambahan") %></span></div>
                            <% for (int i = 0; i < 10; i++) { %>
                                <% if (tampilkanButtonTambahan[i]) { %>
                                <a href="<%= linkButtonTambahan[i] %>" target="_blank" class="portal-card-home custom">
                                    <span class="portal-icon-home"><i class="<%= safeText(iconButtonTambahan[i]) %>"></i></span>
                                    <span class="portal-title-home"><%= safeText(labelButtonTambahan[i]) %></span>
                                    <span class="portal-desc-small"><%= safeText(deskripsiButtonTambahan[i]) %></span>
                                </a>
                                <% } %>
                            <% } %>
                            <% } %>
                        </div>
                        <div class="portal-support-note"><i class="fas fa-lightbulb me-2"></i><%= homeText("home_text_sistem_pendukung_dapat_digunakan_sebagai_pintu_masuk_layanan_publik_maup", "Sistem pendukung dapat digunakan sebagai pintu masuk layanan publik maupun internal sehingga halaman depan terasa lebih hidup, profesional, dan bermanfaat bagi banyak jenis pengguna.") %></div>
                    </section>
                    <% } %>

                    <% if (!adaPortalPerguruanTinggi && !adaPortalSekolah && !adaSistemPendukung) { %>
                    <div class="empty-state-home mt-4">
                        <i class="fas fa-info-circle me-2"></i><%= homeText("home_text_belum_ada_modul_halaman_depan_yang_diaktifkan_silakan_aktifkan_modul_mel", "Belum ada modul halaman depan yang diaktifkan. Silakan aktifkan modul melalui menu Konfigurasi > Portal Halaman Depan.") %>
                    </div>
                    <% } %>
                </div>
            </div>
        </div>
    </main>

    <footer id="kontak" class="footer-home shadow-lg">
        <div class="container">
            <div class="row justify-content-between gy-4 text-center text-md-start">
                <div class="col-md-5 col-lg-4">
                    <h5 class="text-uppercase"><i class="fas fa-university me-2 contact-icon-home"></i> <%= safeText(judulHeader) %></h5>
                    <p class="mt-3 pe-md-4 footer-desc-home">
                        <%= homeText("home_text_infrastruktur_digital_terintegrasi_yang_didedikasikan_untuk_mengakselera", "Infrastruktur digital terintegrasi yang didedikasikan untuk mengakselerasi kualitas pendidikan melalui otomatisasi birokrasi, keteraturan layanan, validitas data, dan kemudahan akses di lingkungan") %>
                        <strong class="text-white"><%= safeText(judul) %></strong>.
                    </p>
                </div>

                <div class="col-md-3 col-lg-3 d-flex flex-column align-items-center align-items-md-start">
                    <h5 class="text-uppercase"><%= homeText("home_text_unduh_aplikasi_mobile", "Tersedia di Perangkat Anda") %></h5>
                    <div class="d-flex flex-column mt-3 gap-3 w-100 align-items-center align-items-md-start">
                        <a href="https://play.google.com/store/apps/details?id=com.ecampus.zishof" target="_blank" rel="noopener noreferrer" class="store-btn-home">
                            <i class="fab fa-google-play fs-3 text-white" aria-hidden="true"></i>
                            <span class="ms-3 fw-semibold"><%= homeText("home_text_google_play", "Google Play") %></span>
                        </a>
                        <a href="https://apps.apple.com/id/app/ecampus/id6503487876?l=id" target="_blank" rel="noopener noreferrer" class="store-btn-home">
                            <i class="fab fa-apple fs-3 text-white"></i>
                            <span class="ms-3 fw-semibold"><%= homeText("home_text_app_store", "App Store") %></span>
                        </a>
                        <a href="https://github.com/Zishof/ecampus-eschool-releases/releases/latest" target="_blank" rel="noopener noreferrer" class="store-btn-home">
                            <i class="fas fa-desktop fs-3 text-white" aria-hidden="true"></i>
                            <span class="ms-3 fw-semibold"><%= homeText("home_text_versi_desktop", "Versi Desktop") %></span>
                        </a>
                    </div>
                </div>

                <div class="col-md-4 col-lg-4">
                    <h5 class="text-uppercase"><%= homeText("home_text_informasi_kontak", "Informasi Kontak") %></h5>
                    <ul class="list-unstyled mt-3 d-flex flex-column gap-3">
                        <li class="d-flex align-items-start text-start">
                            <i class="fas fa-map-marker-alt contact-icon-home mt-1 me-3"></i>
                            <span class="footer-contact-text"><%= safeText(alamat1) %></span>
                        </li>
                        <li class="d-flex align-items-center text-start">
                            <i class="fas fa-phone-alt contact-icon-home me-3"></i>
                            <span class="footer-contact-text"><%= safeText(telepon) %></span>
                        </li>
                        <li class="d-flex align-items-center text-start">
                            <i class="fas fa-envelope contact-icon-home me-3"></i>
                            <span class="footer-contact-text"><%= safeText(email) %></span>
                        </li>
                    </ul>
                </div>
            </div>

            <hr class="border-secondary mt-5 mb-4 opacity-25">
            <div class="row">
                <div class="col-12 text-center text-md-start">
                    <small class="text-white-50">
                        &copy; <%= java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) %> <%= safeText(judulHeader) %> - <%= safeText(judul) %>.
                        <%= homeText("home_text_seluruh_hak_cipta_dilindungi", "Seluruh Hak Cipta Dilindungi.") %>
                    </small>
                </div>
            </div>
        </div>
    </footer>

    <%-- Tombol "Masuk ke Sistem" mengambang DIHAPUS (2026-08-16): posisinya (right:18px;
         bottom:18px, lihat .floating-login-home di css/baru/base-theme.css) nyaris
         persis bertumpuk dgn tombol "Bantuan" (bantuan_button.jsp: right:16px;
         bottom:16px). Aksi login sudah tersedia jelas di tombol hero "Masuk ke Sistem"
         di bagian atas halaman, jadi duplikat mengambang ini dibuang, bukan digeser. --%>

    <script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
    <jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp" />
</body>
</html>
