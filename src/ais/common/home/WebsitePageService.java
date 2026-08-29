package ais.common.home;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.hibernate.Session;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.KalenderAkademik;
import ais.database.model.PengumumanAkademis;
import ais.database.model.sekolah.KelasSiswa;

/** Builds tenant-scoped, indexable information pages for the public website. */
public class WebsitePageService {
    private final HomePortalSectionResolver config = new HomePortalSectionResolver();
    private final SimpleDateFormat date = new SimpleDateFormat("d MMMM yyyy", new Locale("id", "ID"));
    private final SimpleDateFormat isoDate = new SimpleDateFormat("yyyy-MM-dd");

    public WebsitePageViewModel build(HomePortalViewModel portal, String rawPath, String query) {
        String path = normalize(rawPath);
        WebsitePageViewModel page = base(portal, path);
        if ("program".equals(path)) programs(page);
        else if (path.startsWith("program/")) programDetail(page, id(path));
        else if ("berita".equals(path)) news(page);
        else if (path.startsWith("berita/")) newsDetail(page, id(path));
        else if ("agenda".equals(path)) agenda(page);
        else if (path.startsWith("agenda/")) agendaDetail(page, id(path));
        else if ("cari".equals(path)) search(page, query);
        else staticPage(page, path);
        seo(page);
        return page;
    }

    public boolean supports(String rawPath) {
        String path = normalize(rawPath);
        return path.matches("program(?:/[0-9]+)?|berita(?:/[0-9]+)?|agenda(?:/[0-9]+)?|cari|profil|penerimaan|layanan|dokumen|riset|kehidupan-kampus|pembelajaran|orang-tua|perlindungan-anak|ppid|privasi|aksesibilitas|kontak|akreditasi|staf|beasiswa");
    }

    private WebsitePageViewModel base(HomePortalViewModel portal, String path) {
        WebsitePageViewModel page = new WebsitePageViewModel();
        page.portal = portal;
        page.path = path;
        crumb(page, "Beranda", portal.contextPath + "/web");
        return page;
    }

    private void staticPage(WebsitePageViewModel page, String path) {
        HomePortalViewModel vm = page.portal;
        if ("profil".equals(path)) {
            heading(page, "Tentang institusi", "Profil " + vm.institution.name,
                    value("profile_intro", "Kenali identitas, arah, tata kelola, dan komitmen layanan " + vm.institution.name + "."));
            section(page, "Visi, misi, dan nilai", value("profile_vision_mission", "Visi, misi, dan nilai institusi sedang disiapkan oleh pengelola konten resmi."));
            section(page, "Kepemimpinan dan tata kelola", value("profile_governance", "Informasi pimpinan, struktur organisasi, badan penyelenggara, dan unit kerja tersedia melalui kanal resmi institusi."));
            section(page, "Identitas resmi", value("profile_legal", "Status, izin operasional, identitas registrasi, dan dokumen akreditasi dipublikasikan setelah diverifikasi oleh institusi."));
        } else if ("penerimaan".equals(path)) {
            heading(page, "Penerimaan", vm.terminology.admissionLabel,
                    value("admission_intro", "Panduan jalur, jadwal, persyaratan, biaya, bantuan, dan proses registrasi."));
            section(page, "Jalur dan tanggal penting", value("admission_routes", admissionPeriod(vm)));
            section(page, "Persyaratan dan dokumen", value("admission_requirements", "Siapkan identitas, dokumen pendidikan sebelumnya, pasfoto, dan dokumen khusus sesuai jalur. Periksa kembali ketentuan pada portal pendaftaran sebelum mengirim data."));
            section(page, "Biaya dan bantuan", value("admission_fees", "Rincian biaya, keringanan, dan beasiswa harus merujuk pada keputusan resmi institusi. Hubungi layanan penerimaan bila informasi belum tersedia."));
            section(page, "Bantuan pendaftaran", value("admission_help", contact(vm)));
            card(page, "Mulai", vm.terminology.admissionLabel, "Masuk ke portal pendaftaran resmi.", status(vm), vm.primaryUrl);
        } else if ("layanan".equals(path)) {
            heading(page, "Layanan digital", vm.digitalPortalTitle, vm.digitalPortalSummary);
            for (HomePortalViewModel.ServiceItem item : vm.services) card(page, item.group, item.label, item.description, "Layanan resmi", item.url);
        } else if ("dokumen".equals(path)) {
            heading(page, "Dokumen publik", "Pedoman, regulasi, formulir, dan laporan", value("documents_intro", "Pusat dokumen resmi yang dapat diakses publik."));
            section(page, "Dokumen akademik", value("documents_academic", "Kalender, pedoman akademik, kurikulum, dan formulir dipublikasikan sesuai kewenangan unit."));
            section(page, "Kebijakan dan laporan", value("documents_policy", "Kebijakan, laporan tahunan, akreditasi, dan informasi mutu harus memuat versi serta tanggal berlaku."));
        } else if ("riset".equals(path)) {
            heading(page, "Riset dan pengabdian", vm.institution.college ? "Riset, inovasi, dan dampak" : "Inovasi, prestasi, dan dampak", value("research_intro", "Karya, kolaborasi, dan kontribusi institusi bagi masyarakat."));
            section(page, "Fokus dan pusat kegiatan", value("research_centres", "Informasi pusat studi, laboratorium, program inovasi, dan bidang unggulan sedang dikurasi."));
            section(page, "Publikasi dan kekayaan intelektual", value("research_outputs", "Publikasi, jurnal, repository, prototipe, dan kekayaan intelektual ditautkan ke sumber yang dapat diverifikasi."));
            section(page, "Pengabdian dan dampak", value("research_impact", "Program pengabdian disajikan bersama mitra, wilayah, keluaran, dan bukti dampaknya."));
        } else if ("kehidupan-kampus".equals(path)) {
            heading(page, "Pengalaman peserta didik", vm.institution.college ? "Kehidupan kampus" : "Kehidupan sekolah", value("student_life_intro", "Fasilitas, organisasi, kegiatan, dukungan, kesehatan, konseling, dan kesempatan pengembangan diri."));
            section(page, "Kegiatan dan organisasi", value("student_life_activities", "Temukan organisasi, komunitas, ekstrakurikuler, prestasi, dan kegiatan peserta didik."));
            section(page, "Dukungan dan kesejahteraan", value("student_life_support", "Akses dukungan akademik, konseling, kesehatan, aksesibilitas, keamanan, serta mekanisme bantuan."));
            section(page, "Fasilitas dan kunjungan", value("student_life_facilities", "Informasi fasilitas, lokasi, akses transportasi, dan prosedur kunjungan tersedia melalui kontak institusi."));
        } else if ("pembelajaran".equals(path)) {
            heading(page, "Pembelajaran", "Kurikulum dan pengalaman belajar", value("learning_intro", "Pendekatan pembelajaran, jenjang, kalender pendidikan, asesmen, dan dukungan belajar."));
            section(page, "Kurikulum", value("learning_curriculum", "Kurikulum, mata pelajaran, projek, dan capaian pembelajaran dipublikasikan sesuai jenjang."));
            section(page, "Kalender dan asesmen", value("learning_calendar", "Jadwal penting dan kebijakan asesmen tersedia melalui kalender serta dokumen resmi."));
        } else if ("orang-tua".equals(path)) {
            heading(page, "Orang tua dan wali", "Pusat informasi keluarga", value("parent_intro", "Satu tempat untuk kalender, kebijakan, formulir, komunikasi, pembayaran, dan bantuan."));
            section(page, "Komunikasi resmi", value("parent_communication", contact(vm)));
            section(page, "Panduan dan formulir", value("parent_resources", "Panduan kehadiran, pembelajaran, kegiatan, kesehatan, penjemputan, pembayaran, dan formulir tersedia melalui layanan resmi."));
        } else if ("perlindungan-anak".equals(path)) {
            heading(page, "Keselamatan dan kesejahteraan", "Perlindungan anak dan anti-perundungan", value("safeguarding_intro", "Komitmen menciptakan lingkungan belajar yang aman, inklusif, dan menghormati hak setiap anak."));
            section(page, "Laporkan kekhawatiran", value("safeguarding_report", "Jika ada risiko keselamatan, perundungan, kekerasan, atau pelecehan, hubungi kanal resmi institusi. Dalam keadaan darurat, hubungi layanan darurat setempat."));
            section(page, "Kebijakan", value("safeguarding_policy", "Kebijakan perlindungan anak, anti-perundungan, keamanan digital, penjemputan, kesehatan, dan penanganan keluhan ditinjau secara berkala."));
        } else if ("ppid".equals(path)) {
            heading(page, "Keterbukaan informasi", "PPID dan layanan informasi publik", value("ppid_intro", "Layanan informasi publik bagi tenant yang termasuk atau menerapkan standar Badan Publik."));
            section(page, "Daftar informasi publik", value("ppid_information_list", "Informasi berkala, serta-merta, tersedia setiap saat, dan informasi yang dikecualikan dikelola sesuai ketentuan yang berlaku."));
            section(page, "Permohonan dan keberatan", value("ppid_request", "Permohonan informasi dan keberatan diajukan melalui kanal PPID resmi dengan bukti registrasi dan tenggat layanan yang jelas."));
            section(page, "Kontak layanan", value("ppid_contact", contact(vm)));
        } else if ("privasi".equals(path)) {
            heading(page, "Kebijakan", "Pemberitahuan privasi", value("privacy_intro", "Penjelasan bagaimana data pribadi diproses saat Anda menggunakan website dan layanan digital institusi."));
            section(page, "Data dan tujuan", value("privacy_data", "Website publik memproses data teknis minimum untuk keamanan dan keandalan. Data yang dikirim melalui formulir diproses untuk memberi layanan yang diminta."));
            section(page, "Retensi, keamanan, dan pihak lain", value("privacy_retention", "Data disimpan sesuai kebutuhan layanan dan kewajiban hukum, dilindungi dengan kontrol yang proporsional, serta tidak dibagikan di luar tujuan yang sah tanpa dasar yang sesuai."));
            section(page, "Hak dan kontak", value("privacy_rights", "Untuk pertanyaan, koreksi, atau permintaan terkait data pribadi, gunakan kontak resmi: " + contact(vm)));
        } else if ("aksesibilitas".equals(path)) {
            heading(page, "Inklusi digital", "Pernyataan aksesibilitas", value("accessibility_intro", "Kami berupaya membuat website dapat digunakan oleh sebanyak mungkin orang dan menargetkan WCAG 2.2 Level AA."));
            section(page, "Kemampuan yang didukung", value("accessibility_features", "Navigasi keyboard, skip link, fokus terlihat, reflow responsif, reduced motion, struktur semantik, dan alternatif teks diterapkan serta ditinjau berkala."));
            section(page, "Laporkan hambatan", value("accessibility_feedback", "Sampaikan halaman, perangkat, browser, dan hambatan yang dialami melalui " + contact(vm)));
        } else if ("kontak".equals(path)) {
            heading(page, "Kontak", "Hubungi " + vm.institution.name, "Gunakan kanal resmi untuk pertanyaan, kunjungan, layanan, atau umpan balik.");
            section(page, "Alamat", nonEmpty(vm.institution.address, "Alamat belum dipublikasikan."));
            section(page, "Telepon dan email", contact(vm));
            section(page, "Waktu layanan", value("contact_hours", "Periksa waktu layanan sebelum berkunjung. Layanan daring dapat memiliki jadwal respons tersendiri."));
        } else if ("akreditasi".equals(path)) {
            heading(page, "Mutu", "Akreditasi dan penjaminan mutu", value("accreditation_intro", "Status mutu institusi dan program disajikan bersama sumber, nomor keputusan, dan masa berlaku."));
            section(page, "Akreditasi institusi", value("accreditation_institution", "Informasi akreditasi institusi sedang diverifikasi oleh pengelola."));
            section(page, "Penjaminan mutu", value("quality_assurance", "Kebijakan, indikator, evaluasi, dan tindak lanjut mutu diterbitkan sesuai kewenangan."));
        } else if ("staf".equals(path)) {
            heading(page, "Direktori", vm.institution.college ? "Dosen dan tenaga kependidikan" : "Guru dan tenaga kependidikan", value("staff_intro", "Direktori unit, peran, keahlian, dan kanal kontak profesional."));
            section(page, "Privasi direktori", "Hanya informasi profesional yang disetujui untuk publikasi yang ditampilkan. Data pribadi dan jadwal internal tidak dipublikasikan.");
        } else if ("beasiswa".equals(path)) {
            heading(page, "Dukungan pembiayaan", "Beasiswa dan bantuan", value("scholarship_intro", "Pilihan bantuan, kriteria, jadwal, dokumen, dan proses pengajuan."));
            section(page, "Informasi resmi", value("scholarship_content", "Pastikan persyaratan, periode, sumber dana, dan kontak diverifikasi sebelum mengajukan. Institusi tidak meminta kredensial akun melalui pesan tidak resmi."));
        }
        crumb(page, page.title, null);
    }

    @SuppressWarnings("unchecked")
    private void programs(WebsitePageViewModel page) {
        heading(page, "Akademik", page.portal.terminology.programLabel, "Jelajahi program aktif dan informasi utamanya.");
        Session s = null;
        try {
            s = HibernateUtil.getSessionFactory().openSession();
            if (page.portal.institution.college && page.portal.institution.id != null) {
                List<Jurusan> rows = s.createQuery("select distinct j from Jurusan j left join fetch j.jenjang left join fetch j.fakultas where j.aktif = true and j.fakultas.perguruanTinggi.id = :tenant order by j.nama")
                        .setParameter("tenant", page.portal.institution.id).setMaxResults(100).list();
                for (Jurusan row : rows) card(page, level(row), clean(row.getNama()), compact(first(row.getDeskripsi(), row.getProfil()), 260), accreditation(row), url(page, "program/" + row.getId()));
            } else if (page.portal.institution.schoolId != null) {
                List<KelasSiswa> rows = s.createQuery("from KelasSiswa k where k.aktif = true and k.sekolah.id = :tenant order by k.tingkat, k.nama")
                        .setParameter("tenant", page.portal.institution.schoolId).setMaxResults(100).list();
                for (KelasSiswa row : rows) card(page, row.getTingkat() == null ? "Program" : "Tingkat " + row.getTingkat(), clean(row.getNama()), compact(row.getKeterangan(), 260), clean(row.getTahunAjaran()), url(page, "program/" + row.getId()));
            }
        } catch (Exception e) { audit(e, "programs"); } finally { close(s); }
        crumb(page, page.title, null);
    }

    private void programDetail(WebsitePageViewModel page, Long id) {
        if (id == null) { notFound(page); return; }
        Session s = null;
        try {
            s = HibernateUtil.getSessionFactory().openSession();
            if (page.portal.institution.college && page.portal.institution.id != null) {
                List<?> rows = s.createQuery("select distinct j from Jurusan j left join fetch j.jenjang left join fetch j.fakultas where j.id = :id and j.aktif = true and j.fakultas.perguruanTinggi.id = :tenant")
                        .setParameter("id", id).setParameter("tenant", page.portal.institution.id).setMaxResults(1).list();
                if (rows.isEmpty()) { notFound(page); return; }
                Jurusan row = (Jurusan) rows.get(0);
                page.schemaType = "EducationalOccupationalProgram";
                heading(page, level(row), clean(row.getNama()), compact(first(row.getDeskripsi(), row.getProfil()), 320));
                section(page, "Profil program", nonEmpty(clean(first(row.getProfil(), row.getDeskripsi())), "Profil program sedang disiapkan oleh unit pengelola."));
                section(page, "Gelar dan bahasa pengantar", join("Gelar: " + clean(first(row.getGelar(), row.getSingkatanGelar())), "Bahasa pengantar: " + clean(row.getBahasaPengantar())));
                section(page, "Akreditasi", nonEmpty(accreditation(row), "Status akreditasi sedang diverifikasi."));
                section(page, "Kontak program", join(clean(row.getAlamat()), clean(row.getWa())));
            } else if (page.portal.institution.schoolId != null) {
                List<?> rows = s.createQuery("from KelasSiswa k where k.id = :id and k.aktif = true and k.sekolah.id = :tenant")
                        .setParameter("id", id).setParameter("tenant", page.portal.institution.schoolId).setMaxResults(1).list();
                if (rows.isEmpty()) { notFound(page); return; }
                KelasSiswa row = (KelasSiswa) rows.get(0);
                page.schemaType = "EducationalOccupationalProgram";
                heading(page, row.getTingkat() == null ? "Program pendidikan" : "Tingkat " + row.getTingkat(), clean(row.getNama()), clean(row.getKeterangan()));
                section(page, "Kurikulum dan pembelajaran", row.getKurikulumSekolah() == null ? "Informasi kurikulum sedang disiapkan." : clean(row.getKurikulumSekolah().toString()));
                section(page, "Tahun ajaran", clean(row.getTahunAjaran()));
            } else { notFound(page); return; }
            card(page, "Penerimaan", page.portal.terminology.admissionLabel, "Periksa jalur, persyaratan, biaya, dan tanggal penting.", status(page.portal), url(page, "penerimaan"));
        } catch (Exception e) { audit(e, "programDetail"); notFound(page); } finally { close(s); }
        crumb(page, page.portal.terminology.programLabel, url(page, "program"));
        crumb(page, page.title, null);
    }

    @SuppressWarnings("unchecked")
    private void news(WebsitePageViewModel page) {
        heading(page, "Publikasi", "Berita dan pengumuman", "Informasi resmi terbaru dari institusi.");
        Session s = null;
        try {
            s = HibernateUtil.getSessionFactory().openSession();
            org.hibernate.Query q = scopedNews(s, page.portal, " order by p.tanggal desc, p.id desc");
            if (q != null) {
                List<PengumumanAkademis> rows = q.setMaxResults(100).list();
                for (PengumumanAkademis row : rows) card(page, "Pengumuman", clean(row.getJudul()), compact(row.getCatatan(), 280), format(row.getTanggal()), url(page, "berita/" + row.getId()));
            }
        } catch (Exception e) { audit(e, "news"); } finally { close(s); }
        crumb(page, page.title, null);
    }

    private void newsDetail(WebsitePageViewModel page, Long id) {
        if (id == null) { notFound(page); return; }
        Session s = null;
        try {
            s = HibernateUtil.getSessionFactory().openSession();
            org.hibernate.Query q = scopedNews(s, page.portal, " and p.id = :id");
            if (q == null) { notFound(page); return; }
            List<?> rows = q.setParameter("id", id).setMaxResults(1).list();
            if (rows.isEmpty()) { notFound(page); return; }
            PengumumanAkademis row = (PengumumanAkademis) rows.get(0);
            page.schemaType = "NewsArticle";
            heading(page, "Pengumuman", clean(row.getJudul()), compact(row.getCatatan(), 320));
            page.body = clean(row.getCatatan());
            page.updatedLabel = format(row.getTanggal());
            page.publishedIso = iso(row.getTanggal());
        } catch (Exception e) { audit(e, "newsDetail"); notFound(page); } finally { close(s); }
        crumb(page, "Berita", url(page, "berita"));
        crumb(page, page.title, null);
    }

    @SuppressWarnings("unchecked")
    private void agenda(WebsitePageViewModel page) {
        heading(page, "Kalender", "Agenda", "Jadwal kegiatan mendatang yang dipublikasikan institusi.");
        if (page.portal.institution.college && !config.enabled("website_v4_college_agenda_shared", false)) {
            section(page, "Agenda kampus", "Agenda kampus ditutup sementara sampai sumber kalender memiliki kepemilikan tenant yang dapat diverifikasi.");
        } else {
            Session s = null;
            try {
                s = HibernateUtil.getSessionFactory().openSession();
                org.hibernate.Query q = scopedAgenda(s, page.portal, " order by k.tanggalMulai asc");
                if (q != null) {
                    List<KalenderAkademik> rows = q.setMaxResults(100).list();
                    for (KalenderAkademik row : rows) card(page, "Agenda", clean(row.getNamaKegiatanAkademik()), compact(row.getDeskripsiKegiatanAkademik(), 240), range(row.getTanggalMulai(), row.getTanggalSelesai()), url(page, "agenda/" + row.getId()));
                }
            } catch (Exception e) { audit(e, "agenda"); } finally { close(s); }
        }
        crumb(page, page.title, null);
    }

    private void agendaDetail(WebsitePageViewModel page, Long id) {
        if (id == null || (page.portal.institution.college && !config.enabled("website_v4_college_agenda_shared", false))) { notFound(page); return; }
        Session s = null;
        try {
            s = HibernateUtil.getSessionFactory().openSession();
            org.hibernate.Query q = scopedAgenda(s, page.portal, " and k.id = :id");
            if (q == null) { notFound(page); return; }
            List<?> rows = q.setParameter("id", id).setMaxResults(1).list();
            if (rows.isEmpty()) { notFound(page); return; }
            KalenderAkademik row = (KalenderAkademik) rows.get(0);
            page.schemaType = "Event";
            heading(page, "Agenda", clean(row.getNamaKegiatanAkademik()), clean(row.getDeskripsiKegiatanAkademik()));
            section(page, "Waktu", range(row.getTanggalMulai(), row.getTanggalSelesai()));
            section(page, "Status", clean(row.getStatus()));
            page.startIso = iso(row.getTanggalMulai());
            page.endIso = iso(row.getTanggalSelesai());
        } catch (Exception e) { audit(e, "agendaDetail"); notFound(page); } finally { close(s); }
        crumb(page, "Agenda", url(page, "agenda"));
        crumb(page, page.title, null);
    }

    private void search(WebsitePageViewModel page, String query) {
        page.searchPage = true;
        page.searchQuery = query == null ? "" : clean(query).trim();
        heading(page, "Pencarian", "Cari di website", page.searchQuery.length() == 0 ? "Masukkan kata kunci untuk mencari program, berita, dan halaman layanan." : "Hasil untuk “" + page.searchQuery + "”.");
        if (page.searchQuery.length() > 1) {
            programs(page);
            news(page);
            String needle = page.searchQuery.toLowerCase(new Locale("id", "ID"));
            for (int i = page.cards.size() - 1; i >= 0; i--) {
                WebsitePageViewModel.Card c = page.cards.get(i);
                String hay = (nonEmpty(c.title, "") + " " + nonEmpty(c.summary, "") + " " + nonEmpty(c.meta, "")).toLowerCase(new Locale("id", "ID"));
                if (!hay.contains(needle)) page.cards.remove(i);
            }
            page.title = "Hasil pencarian";
            page.description = page.cards.isEmpty() ? "Tidak ada hasil. Coba kata yang lebih umum atau hubungi institusi." : page.cards.size() + " hasil ditemukan.";
        }
        page.breadcrumbs.clear(); crumb(page, "Beranda", page.portal.contextPath + "/web"); crumb(page, "Pencarian", null);
    }

    private org.hibernate.Query scopedNews(Session s, HomePortalViewModel vm, String suffix) {
        String hql = "from PengumumanAkademis p where p.aktif = true and p.diperuntukkan = :audience"
                + " and p.tanggal <= :today and (p.tetapTampilkanPengumumanMeskipunSudahKelewat = true"
                + " or p.sampai is null or p.sampai >= :today)";
        Long tenant;
        if (vm.institution.college && vm.institution.id != null) { hql += " and p.perguruanTinggi.id = :tenant"; tenant = vm.institution.id; }
        else if (vm.institution.schoolId != null) { hql += " and p.sekolah.id = :tenant"; tenant = vm.institution.schoolId; }
        else if (vm.institution.foundationId != null) { hql += " and p.yayasan.id = :tenant"; tenant = vm.institution.foundationId; }
        else return null;
        return s.createQuery(hql + suffix).setParameter("audience", PengumumanAkademis.UNTUK_UMUM)
                .setParameter("today", new Date()).setParameter("tenant", tenant);
    }

    private org.hibernate.Query scopedAgenda(Session s, HomePortalViewModel vm, String suffix) {
        String hql = "from KalenderAkademik k where k.aktif = true and k.tanggalSelesai >= :today";
        org.hibernate.Query q;
        if (vm.institution.schoolId != null) q = s.createQuery(hql + " and k.sekolah.id = :tenant" + suffix).setParameter("tenant", vm.institution.schoolId);
        else if (vm.institution.foundationId != null) q = s.createQuery(hql + " and k.yayasan.id = :tenant" + suffix).setParameter("tenant", vm.institution.foundationId);
        else if (vm.institution.college && config.enabled("website_v4_college_agenda_shared", false)) q = s.createQuery(hql + " and k.sekolah is null and k.yayasan is null" + suffix);
        else return null;
        return q.setParameter("today", new Date());
    }

    private void heading(WebsitePageViewModel p, String eyebrow, String title, String description) { p.eyebrow = eyebrow; p.title = title; p.description = description; }
    private void section(WebsitePageViewModel p, String title, String body) { WebsitePageViewModel.Section s = new WebsitePageViewModel.Section(); s.title = title; s.body = nonEmpty(body, "Informasi sedang disiapkan."); p.sections.add(s); }
    private void card(WebsitePageViewModel p, String eyebrow, String title, String summary, String meta, String url) { WebsitePageViewModel.Card c = new WebsitePageViewModel.Card(); c.eyebrow = clean(eyebrow); c.title = clean(title); c.summary = clean(summary); c.meta = clean(meta); c.url = url; p.cards.add(c); }
    private void crumb(WebsitePageViewModel p, String label, String url) { WebsitePageViewModel.Crumb c = new WebsitePageViewModel.Crumb(); c.label = label; c.url = url; p.breadcrumbs.add(c); }
    private String value(String suffix, String fallback) {
        String legacy = null;
        if ("profile_intro".equals(suffix)) legacy = "website_profil_kampus";
        else if ("profile_governance".equals(suffix)) legacy = "website_struktur_organisasi";
        else if ("student_life_facilities".equals(suffix)) legacy = "website_fasilitas_layanan";
        else if ("research_intro".equals(suffix) || "learning_intro".equals(suffix) || "student_life_intro".equals(suffix)) legacy = "website_akademik_kemahasiswaan";
        else if ("contact_hours".equals(suffix)) legacy = "website_kontak_person";
        String inherited = legacy == null ? fallback : config.value(legacy, fallback);
        return config.value("website_v4_" + suffix, inherited);
    }
    private String url(WebsitePageViewModel p, String path) { return p.portal.contextPath + "/web/" + path; }
    private String format(Date d) { return d == null ? "" : date.format(d); }
    private String iso(Date d) { return d == null ? "" : isoDate.format(d); }
    private String range(Date a, Date b) { return a == null ? "Jadwal belum ditetapkan" : format(a) + (b == null || a.equals(b) ? "" : " – " + format(b)); }
    private String status(HomePortalViewModel vm) { return vm.admission == null ? "Periksa jadwal" : nonEmpty(vm.admission.period, vm.admission.open ? "Pendaftaran dibuka" : "Pendaftaran ditutup"); }
    private String admissionPeriod(HomePortalViewModel vm) { return vm.admission == null ? "Jadwal penerimaan belum dipublikasikan." : join(vm.admission.period, join(vm.admission.startDate, vm.admission.endDate)); }
    private String contact(HomePortalViewModel vm) { return join(vm.institution.phone, vm.institution.email); }
    private String level(Jurusan row) { return row.getJenjang() == null ? "Program studi" : clean(row.getJenjang().getNama()); }
    private String accreditation(Jurusan row) { return join(clean(row.getPeringkatAkreditasi()), clean(row.getNoSkAkreditasi())); }
    private String first(String a, String b) { return a != null && a.trim().length() > 0 ? a : b; }
    private String clean(String value) { return value == null ? "" : value.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim(); }
    private String compact(String value, int max) { String v = clean(value); return v.length() > max ? v.substring(0, max - 1).trim() + "…" : v; }
    private String nonEmpty(String v, String fallback) { return v == null || v.trim().length() == 0 ? fallback : v.trim(); }
    private String join(String a, String b) { String x = nonEmpty(a, ""), y = nonEmpty(b, ""); return x.length() == 0 ? y : (y.length() == 0 ? x : x + " • " + y); }
    private String normalize(String path) { if (path == null) return ""; String p = path.trim().replace('\\', '/'); while (p.startsWith("/")) p = p.substring(1); while (p.endsWith("/")) p = p.substring(0, p.length() - 1); return p.toLowerCase(Locale.ENGLISH); }
    private Long id(String path) { try { String v = path.substring(path.lastIndexOf('/') + 1); return v.matches("[0-9]+") ? Long.valueOf(v) : null; } catch (Exception e) { return null; } }
    private void notFound(WebsitePageViewModel p) { p.notFound = true; heading(p, "Tidak ditemukan", "Halaman tidak ditemukan", "Konten tidak tersedia, tidak aktif, atau bukan milik institusi ini."); }
    private void audit(Exception e, String operation) { ais.common.ErrorAuditUtil.record(e, "WebsitePageService." + operation); }
    private void close(Session s) { if (s != null && s.isOpen()) try { s.close(); } catch (Exception ignored) { } }
    private void seo(WebsitePageViewModel p) {
        p.portal.seo.title = p.title + " - " + p.portal.institution.name;
        p.portal.seo.description = compact(p.description, 180);
        StringBuilder graph = new StringBuilder("{\"@context\":\"https://schema.org\",\"@graph\":[{");
        graph.append("\"@type\":\"").append(p.schemaType).append("\",\"name\":\"").append(json(p.title))
                .append("\",\"description\":\"").append(json(p.description)).append("\",\"url\":\"")
                .append(json(p.portal.seo.canonical)).append("\",\"isPartOf\":{\"@type\":\"WebSite\",\"name\":\"")
                .append(json(p.portal.institution.name)).append("\"}");
        if (p.publishedIso != null && p.publishedIso.length() > 0) graph.append(",\"datePublished\":\"").append(p.publishedIso).append("\"");
        if (p.startIso != null && p.startIso.length() > 0) graph.append(",\"startDate\":\"").append(p.startIso).append("\"");
        if (p.endIso != null && p.endIso.length() > 0) graph.append(",\"endDate\":\"").append(p.endIso).append("\"");
        graph.append("},{\"@type\":\"BreadcrumbList\",\"itemListElement\":[");
        for (int i = 0; i < p.breadcrumbs.size(); i++) {
            WebsitePageViewModel.Crumb crumb = p.breadcrumbs.get(i);
            if (i > 0) graph.append(',');
            graph.append("{\"@type\":\"ListItem\",\"position\":").append(i + 1).append(",\"name\":\"").append(json(crumb.label)).append("\"");
            if (crumb.url != null) graph.append(",\"item\":\"").append(json(absolute(p, crumb.url))).append("\"");
            graph.append('}');
        }
        graph.append("]}]} ");
        p.jsonLd = graph.toString();
    }
    private String absolute(WebsitePageViewModel p, String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) return url;
        String canonical = p.portal.seo.canonical;
        String requestPath = p.portal.contextPath + "/web/" + p.path;
        return canonical.endsWith(requestPath) ? canonical.substring(0, canonical.length() - requestPath.length()) + url : url;
    }
    private String json(String value) { return nonEmpty(value, "").replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", " ").replace("\n", " ").replace("<", "\\u003c"); }
}
