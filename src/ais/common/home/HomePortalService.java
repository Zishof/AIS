package ais.common.home;

import javax.servlet.http.HttpServletRequest;

import ais.common.Common;

public class HomePortalService {
    private final HomePortalSectionResolver config = new HomePortalSectionResolver();
    private final HomePortalLinkResolver links = new HomePortalLinkResolver();
    private final HomePortalContentService content = new HomePortalContentService(links);

    public HomePortalViewModel build(HttpServletRequest request) {
        HomePortalViewModel vm = new HomePortalViewModel();
        vm.contextPath = request.getContextPath();
        vm.language = Common.kodeBahasaAktif();
        vm.direction = "ar".equals(vm.language) ? "rtl" : "ltr";
        vm.assetVersion = config.value("home_v3_asset_version", "3.0.0");
        vm.institution = new HomePortalInstitutionResolver().resolve(request);
        resolveTerminology(vm);
        vm.eyebrow = config.value("judul_kecil_home_portal", "Enterprise Education Portal");
        vm.headline = config.value("home_v3_headline", vm.institution.college
                ? "Membentuk generasi unggul, adaptif, dan berdampak"
                : "Menumbuhkan generasi berkarakter, berprestasi, dan siap masa depan");
        vm.description = config.value("home_v3_description", "Satu pintu untuk mengenal " + vm.terminology.institutionLabel.toLowerCase()
                + ", menemukan " + vm.terminology.programLabel.toLowerCase() + ", mengikuti penerimaan, dan mengakses seluruh layanan digital.");
        HomePortalViewModel.LinkItem login = links.resolve(config.value("link_modul_login_ecampus", "/login"), "/login");
        vm.loginUrl = login.url; vm.loginTarget = login.target; vm.loginRel = login.rel;
        HomePortalViewModel.LinkItem primary = links.resolve(config.value("home_v3_primary_cta_url", vm.institution.college ? "/pmb" : "/ppdb"), vm.institution.college ? "/pmb" : "/ppdb");
        vm.primaryLabel = config.value("home_v3_primary_cta_label", vm.terminology.admissionLabel);
        vm.primaryUrl = primary.url; vm.primaryTarget = primary.target; vm.primaryRel = primary.rel;
        HomePortalViewModel.LinkItem secondary = links.resolve(config.value("home_v3_secondary_cta_url", "#program"), "#program");
        vm.secondaryLabel = config.value("home_v3_secondary_cta_label", "Jelajahi " + vm.terminology.programLabel);
        vm.secondaryUrl = secondary.url; vm.secondaryTarget = secondary.target; vm.secondaryRel = secondary.rel;
        configureAnnouncement(vm);
        configureSections(vm);
        addServices(vm);
        if (vm.showPrograms) content.loadPrograms(vm, config);
        if (vm.showAdmission) content.loadAdmission(vm, config);
        if (vm.showNews) content.loadNews(vm, config);
        if (vm.showAgenda) content.loadAgenda(vm, config);
        content.loadConfiguredContent(vm, config);
        addImpacts(vm);
        vm.showPrograms = vm.showPrograms && !vm.programs.isEmpty();
        vm.showAdmission = vm.showAdmission && vm.admission != null;
        vm.showNews = vm.showNews && !vm.news.isEmpty();
        vm.showAgenda = vm.showAgenda && !vm.agenda.isEmpty();
        vm.androidUrl = safeAppUrl(config.value("home_v3_mobile_app_android_url", ""));
        vm.iosUrl = safeAppUrl(config.value("home_v3_mobile_app_ios_url", ""));
        vm.seo = new HomePortalSeoService().build(vm, config, request.getRequestURL().toString());
        return vm;
    }

    private void resolveTerminology(HomePortalViewModel vm) {
        if (vm.institution.college) {
            vm.terminology.learnerPlural = "Mahasiswa"; vm.terminology.teacherPlural = "Dosen";
            vm.terminology.admissionLabel = "Penerimaan Mahasiswa Baru"; vm.terminology.programLabel = "Program Studi";
            vm.terminology.institutionLabel = "Kampus";
        } else {
            vm.terminology.learnerPlural = "Siswa / Santri"; vm.terminology.teacherPlural = "Guru / Ustadz";
            vm.terminology.admissionLabel = "Penerimaan Peserta Didik Baru"; vm.terminology.programLabel = "Program Pendidikan";
            vm.terminology.institutionLabel = "Sekolah / Pesantren";
        }
    }

    private void configureAnnouncement(HomePortalViewModel vm) {
        vm.announcementText = config.value("home_v3_announcement_text", "");
        HomePortalViewModel.LinkItem link = links.resolve(config.value("home_v3_announcement_url", vm.primaryUrl), vm.primaryUrl);
        vm.announcementUrl = link.url; vm.announcementTarget = link.target; vm.announcementRel = link.rel;
        vm.showAnnouncement = config.enabled("home_v3_announcement_enabled", false) && vm.announcementText.length() > 0;
    }

    private void configureSections(HomePortalViewModel vm) {
        vm.showPrograms = config.enabled("home_v3_show_programs", true);
        vm.showAdmission = config.enabled("home_v3_show_admission", true);
        vm.showNews = config.enabled("home_v3_show_news", true);
        vm.showAgenda = config.enabled("home_v3_show_agenda", true);
        vm.showImpact = config.enabled("home_v3_show_research", true) || config.enabled("home_v3_show_achievements", true);
        vm.showPartners = config.enabled("home_v3_show_partners", false);
        vm.showSearch = config.enabled("home_v3_show_site_search", false);
        vm.showLanguage = config.enabled("home_v3_show_language_switcher", true);
    }

    private void addServices(HomePortalViewModel vm) {
        if (vm.institution.college) {
            service(vm, "tampilkan_modul_login_ecampus", true, "Login eCampus", "Akses akademik untuk mahasiswa, dosen, pegawai, pimpinan, dan operator.", "fa-sign-in-alt", "link_modul_login_ecampus", "/login", "akademik");
            service(vm, "tampilkan_modul_pmb", true, "Penerimaan Mahasiswa", "Informasi jalur, formulir, seleksi, dan registrasi online.", "fa-graduation-cap", "link_modul_pmb", "/pmb", "penerimaan");
            service(vm, "tampilkan_modul_pustaka", true, "Perpustakaan Digital", "Katalog, sirkulasi, dan akses layanan perpustakaan.", "fa-book-open", "link_modul_pustaka", "/pustaka", "publik");
            service(vm, "tampilkan_modul_repository", true, "Repository Institusi", "Karya ilmiah, publikasi, dan dokumen akademik.", "fa-archive", "link_modul_repository", "/repository", "publik");
            service(vm, "tampilkan_modul_alumni", true, "Tracer Study", "Pendataan alumni dan umpan balik dunia kerja.", "fa-user-graduate", "link_modul_alumni", "/alumni", "publik");
            service(vm, "tampilkan_modul_dokumen", true, "Dokumen Publik", "Pedoman, regulasi, formulir, dan arsip resmi.", "fa-file-alt", "link_modul_dokumen", "/document", "publik");
            service(vm, "tampilkan_modul_dashboard_pimpinan", true, "Dashboard Pimpinan", "Ringkasan data strategis institusi.", "fa-chart-line", "link_modul_dashboard_pimpinan", "/dsh", "pendukung");
            service(vm, "tampilkan_modul_ejournal", false, "E-Journal", "Publikasi jurnal dan artikel ilmiah.", "fa-newspaper", "link_modul_ejournal", "/login", "publik");
            service(vm, "tampilkan_modul_akreditasi", false, "Akreditasi", "Informasi mutu dan dokumen pendukung.", "fa-award", "link_modul_akreditasi", "/login", "pendukung");
            service(vm, "tampilkan_modul_simlitabmas", false, "Riset & Pengabdian", "Penelitian, inovasi, dan pengabdian masyarakat.", "fa-flask", "link_modul_simlitabmas", "/login", "publik");
        } else {
            service(vm, "tampilkan_modul_portal_sekolah", true, "Portal Sekolah / Yayasan", "Akses pengelola, guru, operator, dan pimpinan.", "fa-school", "link_modul_portal_sekolah", "/login", "akademik");
            service(vm, "tampilkan_modul_ppdb_sekolah", true, "PPDB Online", "Pendaftaran peserta didik dan informasi penerimaan.", "fa-id-card", "link_modul_ppdb_sekolah", "/ppdb", "penerimaan");
            service(vm, "tampilkan_modul_login_siswa_wali", true, "Login Siswa / Wali", "Akademik, pembayaran, dan layanan siswa/wali.", "fa-users", "link_modul_login_siswa_wali", "/login", "akademik");
            service(vm, "tampilkan_modul_absen_siswa", true, "Absensi Siswa", "Akses cepat layanan presensi siswa.", "fa-user-clock", "link_modul_absen_siswa", "/welsis", "akademik");
        }
        service(vm, "tampilkan_modul_e_kantin", true, "eKantin", "Transaksi kantin dan unit usaha institusi.", "fa-store", "link_modul_e_kantin", "/kantin", "pendukung");
        service(vm, "tampilkan_modul_anjungan", true, "Anjungan Mandiri", "Akses informasi dan layanan mandiri.", "fa-desktop", "link_modul_anjungan", "/anjungan", "pendukung");
        service(vm, "tampilkan_modul_karir", true, "Karier", "Lowongan, seleksi, dan informasi karier.", "fa-briefcase", "link_modul_karir", "/karir", "pendukung");
        service(vm, "tampilkan_modul_buku_tamu", true, "Buku Tamu", "Pencatatan tamu dan kunjungan digital.", "fa-address-book", "link_modul_buku_tamu", "/tamu", "pendukung");
        service(vm, "tampilkan_modul_portal_rekanan", true, "Portal Rekanan", "Layanan rekanan dan vendor institusi.", "fa-truck", "link_modul_portal_rekanan", "/vendor", "pendukung");
        for (int n = 1; n <= 10; n++) {
            String no = n < 10 ? "0" + n : String.valueOf(n);
            String label = config.value("label_button_tambahan_home_" + no, "");
            String url = config.value("link_button_tambahan_home_" + no, "");
            if (!config.enabled("tampilkan_button_tambahan_home_" + no, false) || label.length() == 0 || url.length() == 0) continue;
            HomePortalViewModel.ServiceItem item = new HomePortalViewModel.ServiceItem();
            item.label = label; item.description = config.value("deskripsi_button_tambahan_home_" + no, "Layanan tambahan institusi.");
            item.icon = icon(config.value("icon_button_tambahan_home_" + no, "fa-link")); item.group = "custom";
            apply(item, links.resolve(url, "#")); vm.services.add(item);
        }
    }

    private void service(HomePortalViewModel vm, String toggle, boolean defaultOn, String label, String description, String icon, String linkKey, String fallback, String group) {
        if (!config.enabled(toggle, defaultOn)) return;
        HomePortalViewModel.ServiceItem item = new HomePortalViewModel.ServiceItem(); item.label = label; item.description = description; item.icon = icon(icon); item.group = group;
        apply(item, links.resolve(config.value(linkKey, fallback), fallback)); vm.services.add(item);
    }

    private void addImpacts(HomePortalViewModel vm) {
        if (!vm.showImpact) return;
        if (config.enabled("home_v3_show_research", true)) impact(vm, "Riset & Inovasi", "Publikasi, proyek terapan, dan inovasi civitas akademika.", "fa-flask", config.value("home_v3_research_url", "#layanan"));
        if (config.enabled("tampilkan_modul_karir", true)) impact(vm, "Karier", "Tracer study, lowongan, magang, dan pengembangan alumni.", "fa-briefcase", config.value("link_modul_karir", "/karir"));
        if (config.enabled("home_v3_show_achievements", true)) impact(vm, "Prestasi", "Pencapaian peserta didik dan tenaga pendidik.", "fa-trophy", config.value("home_v3_achievements_url", "#informasi"));
        if (config.enabled("home_v3_show_facilities", false)) impact(vm, "Fasilitas", "Ruang belajar dan fasilitas pendukung institusi.", "fa-building", config.value("home_v3_facilities_url", "#kontak"));
        vm.showImpact = !vm.impacts.isEmpty();
    }

    private void impact(HomePortalViewModel vm, String label, String description, String icon, String url) {
        HomePortalViewModel.ImpactItem item = new HomePortalViewModel.ImpactItem(); item.label = label; item.description = description; item.icon = icon;
        apply(item, links.resolve(url, "#")); vm.impacts.add(item);
    }

    private String icon(String value) {
        if (value == null) return "fa-link";
        String cleaned = value.replace("fas ", "").replace("far ", "").replace("fab ", "").replace("fa-solid ", "").trim();
        if ("fa-right-to-bracket".equals(cleaned)) return "fa-sign-in-alt";
        if ("fa-box-archive".equals(cleaned)) return "fa-archive";
        if ("fa-file-lines".equals(cleaned)) return "fa-file-alt";
        if ("fa-building-columns".equals(cleaned)) return "fa-university";
        String supported = "|fa-address-book|fa-archive|fa-award|fa-book-open|fa-briefcase|fa-building|fa-chart-line|fa-desktop|fa-file-alt|fa-flask|fa-globe|fa-graduation-cap|fa-id-card|fa-link|fa-newspaper|fa-school|fa-sign-in-alt|fa-store|fa-trophy|fa-truck|fa-university|fa-user-clock|fa-user-graduate|fa-users|";
        return supported.contains("|" + cleaned + "|") ? cleaned : "fa-link";
    }

    private void apply(HomePortalViewModel.LinkItem item, HomePortalViewModel.LinkItem link) { item.url = link.url; item.target = link.target; item.rel = link.rel; }
    private String safeAppUrl(String raw) { HomePortalViewModel.LinkItem link = links.resolve(raw, "#"); return "#".equals(link.url) ? "" : link.url; }
}
