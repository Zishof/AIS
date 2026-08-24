package ais.common.home;

import javax.servlet.http.HttpServletRequest;

import ais.common.Common;

public class HomePortalService {
    private final HomePortalSectionResolver config = new HomePortalSectionResolver();
    private final HomePortalLinkResolver links = new HomePortalLinkResolver();
    private final HomePortalContentService content = new HomePortalContentService(links);

    public HomePortalViewModel build(HttpServletRequest request) {
        return build(request, "home_v3", false);
    }

    /** Builds the official institution website without sharing its presentation settings with the app home. */
    public HomePortalViewModel buildWebsite(HttpServletRequest request) {
        return build(request, "website_v4", true);
    }

    private HomePortalViewModel build(HttpServletRequest request, String prefix, boolean website) {
        HomePortalViewModel vm = new HomePortalViewModel();
        vm.contextPath = request.getContextPath();
        vm.language = Common.kodeBahasaAktif();
        vm.direction = "ar".equals(vm.language) ? "rtl" : "ltr";
        vm.assetVersion = config.value(prefix + "_asset_version", website ? "4.0.0" : "3.1.0");
        vm.institution = new HomePortalInstitutionResolver().resolve(request);
        resolveTerminology(vm);
        resolveDigitalPortal(vm);
        vm.eyebrow = config.value(website ? "website_v4_eyebrow" : "judul_kecil_home_portal", vm.institution.healthcare
                ? "Website Resmi Fasilitas Kesehatan" : "Website Resmi Institusi");
        vm.headline = config.value(prefix + "_headline", vm.institution.healthcare
                ? "Pelayanan kesehatan tepercaya, mudah diakses, dan berpusat pada pasien"
                : vm.institution.college
                ? "Membentuk generasi unggul, adaptif, dan berdampak"
                : "Menumbuhkan generasi berkarakter, berprestasi, dan siap masa depan");
        String descriptionFallback = vm.institution.healthcare
                ? "Satu pintu untuk mengenal " + vm.institution.name + ", melihat layanan kesehatan, jadwal, informasi pasien, dan akses layanan digital."
                : "Satu pintu untuk mengenal " + vm.terminology.institutionLabel.toLowerCase()
                + ", menemukan " + vm.terminology.programLabel.toLowerCase() + ", mengikuti penerimaan, dan mengakses seluruh layanan digital.";
        if (website) descriptionFallback = config.value("website_tagline", descriptionFallback);
        vm.description = config.value(prefix + "_description", descriptionFallback);
        String loginKey = vm.institution.healthcare ? "link_modul_login_emedic" : "link_modul_login_ecampus";
        HomePortalViewModel.LinkItem login = links.resolve(config.value(loginKey, "/login"), "/login");
        vm.loginUrl = login.url; vm.loginTarget = login.target; vm.loginRel = login.rel;
        String primaryFallback = vm.institution.healthcare ? "/login" : (vm.institution.college ? "/pmb" : "/ppdb");
        HomePortalViewModel.LinkItem primary = links.resolve(config.value(prefix + "_primary_cta_url", primaryFallback), primaryFallback);
        vm.primaryLabel = config.value(prefix + "_primary_cta_label", vm.terminology.admissionLabel);
        vm.primaryUrl = primary.url; vm.primaryTarget = primary.target; vm.primaryRel = primary.rel;
        HomePortalViewModel.LinkItem secondary = links.resolve(config.value(prefix + "_secondary_cta_url", vm.institution.healthcare ? "#layanan" : "#program"), vm.institution.healthcare ? "#layanan" : "#program");
        vm.secondaryLabel = config.value(prefix + "_secondary_cta_label", "Jelajahi " + vm.terminology.programLabel);
        vm.secondaryUrl = secondary.url; vm.secondaryTarget = secondary.target; vm.secondaryRel = secondary.rel;
        configureAnnouncement(vm, prefix);
        configureSections(vm, prefix);
        addServices(vm);
        if (vm.showPrograms) content.loadPrograms(vm, config, prefix);
        if (vm.showAdmission) content.loadAdmission(vm, config, prefix);
        if (vm.showNews) content.loadNews(vm, config, prefix);
        if (vm.showAgenda) content.loadAgenda(vm, config, prefix);
        content.loadConfiguredContent(vm, config, prefix);
        addImpacts(vm, prefix);
        vm.showPrograms = vm.showPrograms && !vm.programs.isEmpty();
        vm.showAdmission = vm.showAdmission && vm.admission != null;
        vm.showNews = vm.showNews && !vm.news.isEmpty();
        vm.showAgenda = vm.showAgenda && !vm.agenda.isEmpty();
        String androidFallback = vm.institution.healthcare ? ""
                : "https://play.google.com/store/apps/details?id=com.ecampus.zishof";
        String iosFallback = vm.institution.healthcare ? ""
                : "https://apps.apple.com/id/app/ecampus/id6503487876?l=id";
        String desktopFallback = vm.institution.healthcare ? ""
                : "https://github.com/Zishof/ecampus-eschool-releases/releases/latest";
        String appKeyPrefix = prefix + (vm.institution.healthcare ? "_health" : "");
        vm.androidUrl = safeAppUrl(config.value(appKeyPrefix + "_mobile_app_android_url", androidFallback));
        vm.iosUrl = safeAppUrl(config.value(appKeyPrefix + "_mobile_app_ios_url", iosFallback));
        vm.desktopUrl = safeAppUrl(config.value(appKeyPrefix + "_desktop_app_url", desktopFallback));
        vm.seo = new HomePortalSeoService().build(vm, config, canonicalUrl(request, prefix), prefix);
        return vm;
    }

    private String canonicalUrl(HttpServletRequest request, String prefix) {
        String configured = config.value(prefix + "_public_base_url", "");
        String base = configured == null ? "" : configured.trim();
        if (!base.matches("https?://[A-Za-z0-9.-]+(?::[0-9]{1,5})?(?:/[^?#]*)?")) {
            base = request.getRequestURL().toString();
            String querylessPath = request.getRequestURI();
            if (querylessPath != null && base.endsWith(querylessPath)) {
                base = base.substring(0, base.length() - querylessPath.length());
            } else {
                return request.getRequestURL().toString();
            }
        }
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String path = request.getRequestURI();
        return base + (path == null ? request.getContextPath() + "/web" : path);
    }

    private void resolveTerminology(HomePortalViewModel vm) {
        if (vm.institution.healthcare) {
            vm.terminology.learnerPlural = "Pasien"; vm.terminology.teacherPlural = "Tenaga Kesehatan";
            vm.terminology.admissionLabel = "Pendaftaran Pasien"; vm.terminology.programLabel = "Layanan Kesehatan";
            vm.terminology.institutionLabel = vm.institution.category.length() > 0 ? vm.institution.category : "Fasilitas Kesehatan";
        } else if (vm.institution.college) {
            vm.terminology.learnerPlural = "Mahasiswa"; vm.terminology.teacherPlural = "Dosen";
            vm.terminology.admissionLabel = "Penerimaan Mahasiswa Baru"; vm.terminology.programLabel = "Program Studi";
            vm.terminology.institutionLabel = "Kampus";
        } else {
            vm.terminology.learnerPlural = "Siswa / Santri"; vm.terminology.teacherPlural = "Guru / Ustadz";
            vm.terminology.admissionLabel = "Penerimaan Peserta Didik Baru"; vm.terminology.programLabel = "Program Pendidikan";
            vm.terminology.institutionLabel = "Sekolah / Pesantren";
        }
    }

    private void resolveDigitalPortal(HomePortalViewModel vm) {
        if (vm.institution.healthcare) {
            vm.digitalPortalTitle = "Fasilitas Kesehatan Digital";
            vm.digitalPortalSummary = "Pasien, rekam medis, farmasi, keuangan, SDM & layanan kesehatan";
        } else if (vm.institution.college) {
            vm.digitalPortalTitle = "eCampus Terintegrasi";
            vm.digitalPortalSummary = "Akademik, PMB, keuangan, SDM, aset & layanan kampus";
        } else if ("foundation".equals(vm.institution.type)) {
            vm.digitalPortalTitle = "Yayasan Digital";
            vm.digitalPortalSummary = "Multi-sekolah, keuangan, SDM, aset & layanan yayasan";
        } else {
            vm.digitalPortalTitle = "eSchool Terintegrasi";
            vm.digitalPortalSummary = "Akademik, PPDB, keuangan, SDM, aset & layanan sekolah";
        }
    }

    private void configureAnnouncement(HomePortalViewModel vm, String prefix) {
        vm.announcementText = config.value(prefix + "_announcement_text", "");
        HomePortalViewModel.LinkItem link = links.resolve(config.value(prefix + "_announcement_url", vm.primaryUrl), vm.primaryUrl);
        vm.announcementUrl = link.url; vm.announcementTarget = link.target; vm.announcementRel = link.rel;
        vm.showAnnouncement = config.enabled(prefix + "_announcement_enabled", false) && vm.announcementText.length() > 0;
    }

    private void configureSections(HomePortalViewModel vm, String prefix) {
        if (vm.institution.healthcare) {
            vm.showPrograms = config.enabled(prefix + "_show_health_services", true);
            vm.showAdmission = config.enabled(prefix + "_show_patient_registration", true);
            vm.showNews = config.enabled(prefix + "_show_health_news", false);
            vm.showAgenda = config.enabled(prefix + "_show_health_agenda", false);
            vm.showImpact = config.enabled(prefix + "_show_health_quality", true)
                    || config.enabled(prefix + "_show_health_facilities", true);
            vm.showPartners = config.enabled(prefix + "_show_partners", false);
            vm.showSearch = config.enabled(prefix + "_show_site_search", false);
            vm.showLanguage = config.enabled(prefix + "_show_language_switcher", true);
            return;
        }
        vm.showPrograms = config.enabled(prefix + "_show_programs", true);
        vm.showAdmission = config.enabled(prefix + "_show_admission", true);
        vm.showNews = config.enabled(prefix + "_show_news", true);
        vm.showAgenda = config.enabled(prefix + "_show_agenda", true);
        vm.showImpact = config.enabled(prefix + "_show_research", true) || config.enabled(prefix + "_show_achievements", true);
        vm.showPartners = config.enabled(prefix + "_show_partners", false);
        vm.showSearch = config.enabled(prefix + "_show_site_search", false);
        vm.showLanguage = config.enabled(prefix + "_show_language_switcher", true);
    }

    private void addServices(HomePortalViewModel vm) {
        if (vm.institution.healthcare) {
            service(vm, "tampilkan_modul_emedic", true, "Portal eMedic", "Akses sistem informasi dan layanan operasional fasilitas kesehatan.", "fa-sign-in-alt", "link_modul_login_emedic", "/login", "kesehatan");
            service(vm, "tampilkan_modul_pendaftaran_pasien", true, "Pendaftaran Pasien", "Pendaftaran dan akses layanan pasien secara daring.", "fa-id-card", "link_modul_pendaftaran_pasien", "/login", "pasien");
            service(vm, "tampilkan_modul_jadwal_dokter", true, "Jadwal Tenaga Kesehatan", "Temukan jadwal dokter dan tenaga kesehatan.", "fa-user-clock", "link_modul_jadwal_dokter", "/login", "pasien");
            service(vm, "tampilkan_modul_rekam_medis", true, "Rekam Medis", "Akses informasi rekam medis sesuai hak pengguna.", "fa-file-alt", "link_modul_rekam_medis", "/login", "pasien");
            service(vm, "tampilkan_modul_farmasi", true, "Farmasi", "Informasi dan layanan farmasi fasilitas kesehatan.", "fa-store", "link_modul_farmasi", "/login", "kesehatan");
            service(vm, "tampilkan_modul_anjungan", true, "Anjungan Mandiri", "Akses informasi dan layanan mandiri pasien.", "fa-desktop", "link_modul_anjungan", "/anjungan", "pendukung");
            service(vm, "tampilkan_modul_buku_tamu", true, "Buku Tamu", "Pencatatan tamu dan kunjungan digital.", "fa-address-book", "link_modul_buku_tamu", "/tamu", "pendukung");
        } else if (vm.institution.college) {
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
        if (!vm.institution.healthcare) {
            service(vm, "tampilkan_modul_e_kantin", true, "eKantin", "Transaksi kantin dan unit usaha institusi.", "fa-store", "link_modul_e_kantin", "/kantin", "pendukung");
            service(vm, "tampilkan_modul_anjungan", true, "Anjungan Mandiri", "Akses informasi dan layanan mandiri.", "fa-desktop", "link_modul_anjungan", "/anjungan", "pendukung");
            service(vm, "tampilkan_modul_karir", true, "Karier", "Lowongan, seleksi, dan informasi karier.", "fa-briefcase", "link_modul_karir", "/karir", "pendukung");
            service(vm, "tampilkan_modul_buku_tamu", true, "Buku Tamu", "Pencatatan tamu dan kunjungan digital.", "fa-address-book", "link_modul_buku_tamu", "/tamu", "pendukung");
            service(vm, "tampilkan_modul_portal_rekanan", true, "Portal Rekanan", "Layanan rekanan dan vendor institusi.", "fa-truck", "link_modul_portal_rekanan", "/vendor", "pendukung");
        }
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

    private void addImpacts(HomePortalViewModel vm, String prefix) {
        if (!vm.showImpact) return;
        if (vm.institution.healthcare) {
            if (config.enabled(prefix + "_show_health_quality", true)) {
                impact(vm, "Mutu & Keselamatan", "Pelayanan yang mengutamakan mutu, keselamatan, dan kebutuhan pasien.", "fa-award", config.value(prefix + "_health_quality_url", "#layanan"));
                impact(vm, "Tenaga Kesehatan", "Informasi layanan dokter dan tenaga kesehatan profesional.", "fa-users", config.value(prefix + "_health_staff_url", "#layanan"));
            }
            if (config.enabled(prefix + "_show_health_facilities", true)) impact(vm, "Fasilitas", "Sarana layanan kesehatan yang mudah dijangkau masyarakat.", "fa-building", config.value(prefix + "_health_facilities_url", "#kontak"));
            impact(vm, "Informasi Publik", "Informasi resmi, edukasi kesehatan, dan kontak layanan.", "fa-file-alt", config.value(prefix + "_health_information_url", "#kontak"));
            vm.showImpact = !vm.impacts.isEmpty();
            return;
        }
        if (config.enabled(prefix + "_show_research", true)) impact(vm, "Riset & Inovasi", "Publikasi, proyek terapan, dan inovasi civitas akademika.", "fa-flask", config.value(prefix + "_research_url", "#layanan"));
        if (config.enabled("tampilkan_modul_karir", true)) impact(vm, "Karier", "Tracer study, lowongan, magang, dan pengembangan alumni.", "fa-briefcase", config.value("link_modul_karir", "/karir"));
        if (config.enabled(prefix + "_show_achievements", true)) impact(vm, "Prestasi", "Pencapaian peserta didik dan tenaga pendidik.", "fa-trophy", config.value(prefix + "_achievements_url", "#layanan"));
        if (config.enabled(prefix + "_show_facilities", false)) impact(vm, "Fasilitas", "Ruang belajar dan fasilitas pendukung institusi.", "fa-building", config.value(prefix + "_facilities_url", "#kontak"));
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
