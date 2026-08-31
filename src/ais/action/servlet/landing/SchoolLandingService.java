package ais.action.servlet.landing;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.jsoup.Jsoup;
import org.json.JSONObject;

import ais.action.master.sekolah.util.SekolahUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PengumumanAkademis;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.WaktuUtil;

/** Menyiapkan DTO website sekolah aktif tanpa membawa entity Hibernate ke JSP. */
public final class SchoolLandingService {

    private static final int MAKS_BERITA = 9;

    private SchoolLandingService() { }

    @SuppressWarnings("unchecked")
    public static boolean prepare(HttpServletRequest request, Long requestedId) {
        Session session = null;
        try {
            Long id = requestedId;
            if (id == null) {
                Sekolah context = safeSchool(request);
                id = context == null ? null : context.getId();
            }
            if (id == null) return false;

            session = HibernateUtil.getSessionFactory().openSession();
            Sekolah school = (Sekolah) session.createCriteria(Sekolah.class)
                    .add(Restrictions.eq("id", id))
                    .add(Restrictions.eq("aktif", Boolean.TRUE))
                    .setMaxResults(1).uniqueResult();
            if (school == null) return false;

            JSONObject website = SchoolWebsiteConfig.load(school, request.getContextPath());
            SchoolProfile profile = profile(request, school);
            List<NewsItem> news = newsSafe(request, session, school);
            request.setAttribute("schoolProfile", profile);
            request.setAttribute("schoolWebsite", website);
            request.setAttribute("schoolNews", news);
            return true;
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "SchoolLandingService.prepare");
            request.setAttribute("schoolContentWarning", Boolean.TRUE);
            return false;
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    private static SchoolProfile profile(HttpServletRequest request, Sekolah school) {
        String type = school.getJenisSekolah() == null ? "Sekolah" : clean(school.getJenisSekolah().getNama(), 100);
        String foundation = school.getYayasan() == null ? "" : clean(school.getYayasan().getNama(), 160);
        String logo = SekolahUtil.getSekolahMedia(request, "logo_sekolah_", school);
        String hero = SekolahUtil.getSekolahMedia(request, "background_sekolah_", school);
        return new SchoolProfile(school.getId(), clean(school.getNama(), 180), type, clean(school.getNpsn(), 60),
                clean(school.getNss(), 60), clean(school.getMotto(), 300), clean(school.getDeskripsi(), 1400),
                clean(school.getAlamat(), 500), clean(school.getTelp(), 80), clean(school.getWa(), 80),
                clean(school.getEmail(), 180), clean(school.getFax(), 80), clean(school.getNamaKepalaSekolah(), 180),
                clean(school.getNamaWakilKepalaSekolah(), 180), clean(school.getDomain(), 300), foundation, logo, hero);
    }

    @SuppressWarnings("unchecked")
    private static List<NewsItem> news(Session session, Sekolah school) {
        Date today = WaktuUtil.getDate();
        Criteria criteria = session.createCriteria(PengumumanAkademis.class)
                .add(Restrictions.eq("aktif", Boolean.TRUE))
                .add(Restrictions.eq("diperuntukkan", PengumumanAkademis.UNTUK_UMUM))
                .add(Restrictions.eq("sekolah", school))
                .add(Restrictions.le("tanggal", today))
                .add(Restrictions.or(
                        Restrictions.eq("tetapTampilkanPengumumanMeskipunSudahKelewat", Boolean.TRUE),
                        Restrictions.or(Restrictions.isNull("sampai"), Restrictions.ge("sampai", today))))
                .addOrder(Order.desc("tanggal")).addOrder(Order.desc("id")).setMaxResults(MAKS_BERITA);
        List<PengumumanAkademis> rows = criteria.list();
        List<NewsItem> result = new ArrayList<NewsItem>();
        SimpleDateFormat format = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
        for (PengumumanAkademis row : rows) {
            if (row == null || row.getId() == null) continue;
            result.add(new NewsItem(row.getId(), clean(row.getJudul(), 180), clean(row.getCatatan(), 360),
                    row.getTanggal() == null ? "" : format.format(row.getTanggal())));
        }
        return result;
    }

    private static List<NewsItem> newsSafe(HttpServletRequest request, Session session, Sekolah school) {
        try {
            return news(session, school);
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "SchoolLandingService.news");
            request.setAttribute("schoolContentWarning", Boolean.TRUE);
            return new ArrayList<NewsItem>();
        }
    }

    private static Sekolah safeSchool(HttpServletRequest request) {
        try { return SekolahUtil.getSekolah(request); }
        catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "SchoolLandingService.context"); return null; }
    }

    private static String clean(String value, int max) {
        String result = value == null ? "" : Jsoup.parse(value).text().replaceAll("\\s+", " ").trim();
        return result.length() <= max ? result : result.substring(0, Math.max(0, max - 1)).trim() + "…";
    }

    /**
     * Tipe implementasi bersarang {@link SchoolProfile} milik {@link SchoolLandingService}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link SchoolLandingService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code String name}, {@code
     * String type}, {@code String npsn}, {@code String nss}, {@code String motto}, {@code String description},
     * {@code String address}; operasi lokal: {@code getId()}, {@code getName()}, {@code getType()}, {@code
     * getNpsn()}, {@code getNss()}, {@code getMotto()}, {@code getDescription()}, {@code getAddress()}, {@code
     * getPhone()}, {@code getWhatsapp}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see SchoolLandingService
     */
    public static final class SchoolProfile {
        private final Long id;
        private final String name, type, npsn, nss, motto, description, address, phone, whatsapp, email, fax;
        private final String principal, vicePrincipal, domain, foundation, logo, hero;

        SchoolProfile(Long id, String name, String type, String npsn, String nss, String motto, String description,
                String address, String phone, String whatsapp, String email, String fax, String principal,
                String vicePrincipal, String domain, String foundation, String logo, String hero) {
            this.id = id; this.name = name; this.type = type; this.npsn = npsn; this.nss = nss;
            this.motto = motto; this.description = description; this.address = address; this.phone = phone;
            this.whatsapp = whatsapp; this.email = email; this.fax = fax; this.principal = principal;
            this.vicePrincipal = vicePrincipal; this.domain = domain; this.foundation = foundation;
            this.logo = logo; this.hero = hero;
        }

        public Long getId() { return id; } public String getName() { return name; }
        public String getType() { return type; } public String getNpsn() { return npsn; }
        public String getNss() { return nss; } public String getMotto() { return motto; }
        public String getDescription() { return description; } public String getAddress() { return address; }
        public String getPhone() { return phone; } public String getWhatsapp() { return whatsapp; }
        public String getEmail() { return email; } public String getFax() { return fax; }
        public String getPrincipal() { return principal; } public String getVicePrincipal() { return vicePrincipal; }
        public String getDomain() { return domain; } public String getFoundation() { return foundation; }
        public String getLogo() { return logo; } public String getHero() { return hero; }
    }

    /**
     * Tipe implementasi bersarang {@link NewsItem} milik {@link SchoolLandingService}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link SchoolLandingService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code String title},
     * {@code String summary}, {@code String date}; operasi lokal: {@code getId()}, {@code getTitle()}, {@code
     * getSummary()}, {@code getDate}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see SchoolLandingService
     */
    public static final class NewsItem {
        private final Long id; private final String title, summary, date;
        NewsItem(Long id, String title, String summary, String date) {
            this.id = id; this.title = title; this.summary = summary; this.date = date;
        }
        public Long getId() { return id; } public String getTitle() { return title; }
        public String getSummary() { return summary; } public String getDate() { return date; }
    }
}
