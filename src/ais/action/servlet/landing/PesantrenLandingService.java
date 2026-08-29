package ais.action.servlet.landing;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PengumumanAkademis;
import ais.database.model.PerguruanTinggi;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.WaktuUtil;

/**
 * Menyiapkan model baca-saja untuk landing page publik ePesantren.
 *
 * <p>DTO sengaja dilepas dari sesi Hibernate agar JSP tidak memicu lazy load,
 * tidak membuka entitas lain, dan tetap dapat dirender setelah sesi ditutup.</p>
 */
public final class PesantrenLandingService {

    private static final int MAKS_BERITA = 8;

    private PesantrenLandingService() {
    }

    @SuppressWarnings("unchecked")
    public static void prepare(HttpServletRequest request) {
        Yayasan yayasan = safeYayasan(request);
        Sekolah sekolahKonteks = safeSekolah(request);
        PerguruanTinggi ptKonteks = safePerguruanTinggi(request);
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            if (valid(yayasan)) {
                yayasan = (Yayasan) session.get(Yayasan.class, yayasan.getId());
            }
            if (!valid(yayasan) && valid(sekolahKonteks) && sekolahKonteks.getYayasan() != null) {
                yayasan = (Yayasan) session.get(Yayasan.class, sekolahKonteks.getYayasan().getId());
            }

            List<Sekolah> sekolahs = new ArrayList<Sekolah>();
            if (valid(yayasan)) {
                sekolahs = session.createCriteria(Sekolah.class)
                        .add(Restrictions.eq("yayasan", yayasan))
                        .add(Restrictions.eq("aktif", Boolean.TRUE))
                        .addOrder(Order.asc("nama")).list();
            } else if (valid(sekolahKonteks)) {
                Sekolah loaded = loadSekolahAktif(session, sekolahKonteks.getId());
                if (loaded != null) {
                    sekolahs.add(loaded);
                }
            }

            List<PerguruanTinggi> pts = collectPerguruanTinggi(session, sekolahs, ptKonteks);
            JSONObject websiteConfig = PesantrenWebsiteConfig.load(yayasan, request.getContextPath());
            Profil profil = profil(request, yayasan, sekolahKonteks, ptKonteks, websiteConfig);
            List<UnitPendidikan> unitSekolah = sekolahDtos(sekolahs, request.getContextPath());
            List<UnitPendidikan> unitPt = ptDtos(pts, request.getContextPath());
            List<Berita> berita = berita(session, yayasan, sekolahs, pts);

            request.setAttribute("pesantrenProfil", profil);
            request.setAttribute("pesantrenSekolah", unitSekolah);
            request.setAttribute("pesantrenPerguruanTinggi", unitPt);
            request.setAttribute("pesantrenBerita", berita);
            request.setAttribute("pesantrenJumlahUnit", Integer.valueOf(unitSekolah.size() + unitPt.size()));
            request.setAttribute("pesantrenWebsite", websiteConfig);
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "PesantrenLandingService.prepare");
            JSONObject websiteConfig = PesantrenWebsiteConfig.load(yayasan, request.getContextPath());
            request.setAttribute("pesantrenProfil", profil(request, yayasan, sekolahKonteks, ptKonteks, websiteConfig));
            request.setAttribute("pesantrenSekolah", new ArrayList<UnitPendidikan>());
            request.setAttribute("pesantrenPerguruanTinggi", new ArrayList<UnitPendidikan>());
            request.setAttribute("pesantrenBerita", new ArrayList<Berita>());
            request.setAttribute("pesantrenJumlahUnit", Integer.valueOf(0));
            request.setAttribute("pesantrenWebsite", websiteConfig);
            request.setAttribute("pesantrenContentWarning", Boolean.TRUE);
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    private static Profil profil(HttpServletRequest request, Yayasan yayasan, Sekolah sekolah,
            PerguruanTinggi pt, JSONObject websiteConfig) {
        String namaDefault = valid(yayasan) ? yayasan.getNama()
                : valid(sekolah) ? sekolah.getNama() : valid(pt) ? pt.getNama() : "Pondok Pesantren";
        String nama = namaDefault;
        String mottoDefault = valid(yayasan) ? yayasan.getMotto()
                : valid(sekolah) ? sekolah.getMotto() : valid(pt) ? pt.getMotto() : "Ilmu, adab, dan kemandirian";
        String motto = mottoDefault;
        String deskripsiDefault = valid(yayasan) ? teks(yayasan.getDeskripsi(), 900) : "";
        String deskripsi = deskripsiDefault;
        String alamatDefault = valid(yayasan) ? yayasan.getAlamat()
                : valid(sekolah) ? sekolah.getAlamat() : valid(pt) ? gabung(pt.getAlamat1(), pt.getAlamat2()) : "";
        String alamat = alamatDefault;
        String teleponDefault = valid(yayasan) ? yayasan.getTelp() : valid(sekolah) ? sekolah.getTelp() : "";
        String telepon = teleponDefault;
        String wa = valid(yayasan) ? yayasan.getWa() : valid(sekolah) ? sekolah.getWa() : telepon;
        String emailDefault = valid(yayasan) ? yayasan.getEmail()
                : valid(sekolah) ? sekolah.getEmail() : valid(pt) ? pt.getEmail() : "";
        String email = emailDefault;
        String website = url(null, valid(yayasan) ? yayasan.getDomain()
                : valid(sekolah) ? sekolah.getDomain() : valid(pt) ? pt.getDomain() : null, "");
        String warna = validWarna(valid(yayasan) ? yayasan.getWarna() : null);
        String logoDefault = valid(yayasan)
                ? SekolahUtil.getYayasanMedia(request, "logo_yayasan_", yayasan)
                : valid(sekolah) ? SekolahUtil.getSekolahMedia(request, "logo_sekolah_", sekolah)
                        : PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_pt_");
        String logo = logoDefault;
        String latarDefault = valid(yayasan)
                ? SekolahUtil.getYayasanMedia(request, "background_yayasan_", yayasan)
                : valid(sekolah) ? SekolahUtil.getSekolahMedia(request, "background_sekolah_", sekolah)
                        : PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "background_pt_");
        String latar = latarDefault;
        return new Profil(nama, motto, deskripsi, alamat, telepon, wa, email, website, warna, logo, latar);
    }

    @SuppressWarnings("unchecked")
    private static List<PerguruanTinggi> collectPerguruanTinggi(Session session, List<Sekolah> sekolahs,
            PerguruanTinggi ptKonteks) {
        List<PerguruanTinggi> hasil = new ArrayList<PerguruanTinggi>();
        Set<Long> ids = new HashSet<Long>();
        for (Sekolah sekolah : sekolahs) {
            PerguruanTinggi pt = sekolah == null ? null : sekolah.getPerguruanTinggi();
            if (valid(pt) && ids.add(pt.getId())) {
                PerguruanTinggi loaded = loadPerguruanTinggiAktif(session, pt.getId());
                if (loaded != null) {
                    hasil.add(loaded);
                }
            }
        }
        if (valid(ptKonteks) && ids.add(ptKonteks.getId())) {
            PerguruanTinggi loaded = loadPerguruanTinggiAktif(session, ptKonteks.getId());
            if (loaded != null) {
                hasil.add(loaded);
            }
        }
        return hasil;
    }

    private static Sekolah loadSekolahAktif(Session session, Long id) {
        if (session == null || id == null) {
            return null;
        }
        return (Sekolah) session.createCriteria(Sekolah.class)
                .add(Restrictions.eq("id", id))
                .add(Restrictions.eq("aktif", Boolean.TRUE))
                .setMaxResults(1).uniqueResult();
    }

    private static PerguruanTinggi loadPerguruanTinggiAktif(Session session, Long id) {
        if (session == null || id == null) {
            return null;
        }
        return (PerguruanTinggi) session.createCriteria(PerguruanTinggi.class)
                .add(Restrictions.eq("id", id))
                .add(Restrictions.eq("aktif", Boolean.TRUE))
                .setMaxResults(1).uniqueResult();
    }

    private static List<UnitPendidikan> sekolahDtos(List<Sekolah> sekolahs, String root) {
        List<UnitPendidikan> hasil = new ArrayList<UnitPendidikan>();
        for (Sekolah sekolah : sekolahs) {
            if (sekolah == null || sekolah.getId() == null) {
                continue;
            }
            hasil.add(new UnitPendidikan(sekolah.getNama(), "Sekolah / Madrasah", sekolah.getMotto(),
                    sekolah.getAlamat(), root + "/sekolah/" + sekolah.getId()));
        }
        return hasil;
    }

    private static List<UnitPendidikan> ptDtos(List<PerguruanTinggi> pts, String root) {
        List<UnitPendidikan> hasil = new ArrayList<UnitPendidikan>();
        for (PerguruanTinggi pt : pts) {
            if (pt == null || pt.getId() == null) {
                continue;
            }
            hasil.add(new UnitPendidikan(pt.getNama(), "Perguruan Tinggi", pt.getMotto(),
                    gabung(pt.getAlamat1(), pt.getAlamat2()), url(pt.getWebsite(), pt.getDomain(), root + "/login")));
        }
        return hasil;
    }

    @SuppressWarnings("unchecked")
    private static List<Berita> berita(Session session, Yayasan yayasan, List<Sekolah> sekolahs,
            List<PerguruanTinggi> pts) {
        Criteria criteria = session.createCriteria(PengumumanAkademis.class)
                .add(Restrictions.eq("aktif", true))
                .add(Restrictions.eq("diperuntukkan", PengumumanAkademis.UNTUK_UMUM))
                .add(Restrictions.or(
                        Restrictions.eq("tetapTampilkanPengumumanMeskipunSudahKelewat", true),
                        Restrictions.or(Restrictions.isNull("sampai"),
                                Restrictions.ge("sampai", WaktuUtil.getDate()))))
                .add(Restrictions.le("tanggal", WaktuUtil.getDate()))
                .add(scopeBerita(yayasan, sekolahs, pts))
                .addOrder(Order.desc("tanggal")).addOrder(Order.desc("id")).setMaxResults(MAKS_BERITA);
        List<PengumumanAkademis> rows = criteria.list();
        List<Berita> hasil = new ArrayList<Berita>();
        SimpleDateFormat tanggal = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
        for (PengumumanAkademis row : rows) {
            if (row == null || row.getId() == null) {
                continue;
            }
            hasil.add(new Berita(row.getId(), teks(row.getJudul(), 180), teks(row.getCatatan(), 360),
                    row.getTanggal() == null ? "" : tanggal.format(row.getTanggal())));
        }
        return hasil;
    }

    private static Criterion scopeBerita(Yayasan yayasan, List<Sekolah> sekolahs, List<PerguruanTinggi> pts) {
        // Fail closed: halaman tenant tidak boleh mengambil pengumuman global atau
        // milik tenant lain ketika konteks institusi tidak berhasil ditemukan.
        Criterion scope = Restrictions.eq("id", Long.valueOf(-1L));
        if (valid(yayasan)) {
            scope = Restrictions.or(scope, Restrictions.eq("yayasan", yayasan));
        }
        if (sekolahs != null && !sekolahs.isEmpty()) {
            scope = Restrictions.or(scope, Restrictions.in("sekolah", sekolahs));
        }
        if (pts != null && !pts.isEmpty()) {
            scope = Restrictions.or(scope, Restrictions.in("perguruanTinggi", pts));
        }
        return scope;
    }

    private static Yayasan safeYayasan(HttpServletRequest request) {
        try { return SekolahUtil.getYayasan(request); } catch (Exception e) { return null; }
    }

    private static Sekolah safeSekolah(HttpServletRequest request) {
        try { return SekolahUtil.getSekolah(request); } catch (Exception e) { return null; }
    }

    private static PerguruanTinggi safePerguruanTinggi(HttpServletRequest request) {
        try { return PerguruanTinggiUtil.getPerguruanTinggi(request); } catch (Exception e) { return null; }
    }

    private static boolean valid(Yayasan value) { return value != null && value.getId() != null; }
    private static boolean valid(Sekolah value) { return value != null && value.getId() != null; }
    private static boolean valid(PerguruanTinggi value) { return value != null && value.getId() != null; }

    private static String teks(String html, int maks) {
        String value = html == null ? "" : Jsoup.parse(html).text().replaceAll("\\s+", " ").trim();
        return value.length() <= maks ? value : value.substring(0, Math.max(0, maks - 1)).trim() + "…";
    }

    private static String gabung(String a, String b) {
        if (a == null || a.trim().length() == 0) return b == null ? "" : b.trim();
        if (b == null || b.trim().length() == 0) return a.trim();
        return a.trim() + ", " + b.trim();
    }

    private static String url(String website, String domain, String fallback) {
        String firstDomain = "";
        if (domain != null && domain.trim().length() > 0) {
            List<String> domains = Common.pisahDomain(domain);
            firstDomain = domains.isEmpty() ? "" : domains.get(0);
        }
        String value = website != null && website.trim().length() > 0 ? website.trim()
                : firstDomain.length() > 0 ? firstDomain : fallback;
        if (value.startsWith("/") || value.startsWith("http://") || value.startsWith("https://")) return value;
        return "https://" + value;
    }

    private static String validWarna(String warna) {
        return warna != null && warna.matches("#[0-9a-fA-F]{6}") ? warna : "#0f766e";
    }

    public static final class Profil {
        private final String nama, motto, deskripsi, alamat, telepon, wa, email, website, warna, logo, latar;
        public Profil(String nama, String motto, String deskripsi, String alamat, String telepon, String wa, String email,
                String website, String warna, String logo, String latar) {
            this.nama = nama == null ? "Pondok Pesantren" : nama; this.motto = motto == null ? "" : motto;
            this.deskripsi = deskripsi == null ? "" : deskripsi; this.alamat = alamat == null ? "" : alamat;
            this.telepon = telepon == null ? "" : telepon; this.wa = wa == null ? "" : wa;
            this.email = email == null ? "" : email; this.website = website == null ? "" : website;
            this.warna = warna; this.logo = logo; this.latar = latar;
        }
        public String getNama() { return nama; } public String getMotto() { return motto; }
        public String getDeskripsi() { return deskripsi; } public String getAlamat() { return alamat; }
        public String getTelepon() { return telepon; } public String getWa() { return wa; }
        public String getEmail() { return email; } public String getWebsite() { return website; }
        public String getWarna() { return warna; } public String getLogo() { return logo; }
        public String getLatar() { return latar; }
    }

    public static final class UnitPendidikan {
        private final String nama, jenis, motto, alamat, url;
        UnitPendidikan(String nama, String jenis, String motto, String alamat, String url) {
            this.nama = nama == null ? "Unit Pendidikan" : nama; this.jenis = jenis; this.motto = motto == null ? "" : motto;
            this.alamat = alamat == null ? "" : alamat; this.url = url;
        }
        public String getNama() { return nama; } public String getJenis() { return jenis; }
        public String getMotto() { return motto; } public String getAlamat() { return alamat; }
        public String getUrl() { return url; }
    }

    public static final class Berita {
        private final Long id; private final String judul, ringkasan, tanggal;
        Berita(Long id, String judul, String ringkasan, String tanggal) {
            this.id = id; this.judul = judul; this.ringkasan = ringkasan; this.tanggal = tanggal;
        }
        public Long getId() { return id; } public String getJudul() { return judul; }
        public String getRingkasan() { return ringkasan; } public String getTanggal() { return tanggal; }
    }
}
