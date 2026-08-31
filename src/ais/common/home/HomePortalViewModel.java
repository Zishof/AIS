package ais.common.home;

import java.util.ArrayList;
import java.util.List;

/** Immutable-at-render-time data contract for the public home page. */
public class HomePortalViewModel {
    public Institution institution = new Institution();
    public Terminology terminology = new Terminology();
    public Seo seo = new Seo();
    public String assetVersion;
    public String contextPath;
    public String language;
    public String direction;
    public String eyebrow;
    public String headline;
    public String description;
    public String digitalPortalTitle;
    public String digitalPortalSummary;
    public String primaryLabel;
    public String primaryUrl;
    public String primaryTarget;
    public String primaryRel;
    public String secondaryLabel;
    public String secondaryUrl;
    public String secondaryTarget;
    public String secondaryRel;
    public String loginUrl;
    public String loginTarget;
    public String loginRel;
    public String announcementText;
    public String announcementUrl;
    public String announcementTarget;
    public String announcementRel;
    public String androidUrl;
    public String iosUrl;
    public String desktopUrl;
    public boolean showAnnouncement;
    public boolean showPrograms;
    public boolean showAdmission;
    public boolean showNews;
    public boolean showAgenda;
    public boolean showImpact;
    public boolean showPartners;
    public boolean showSearch;
    public boolean showLanguage;
    public List<ServiceItem> services = new ArrayList<ServiceItem>();
    public List<ProgramItem> programs = new ArrayList<ProgramItem>();
    public List<StatItem> statistics = new ArrayList<StatItem>();
    public List<NewsItem> news = new ArrayList<NewsItem>();
    public List<AgendaItem> agenda = new ArrayList<AgendaItem>();
    public List<ImpactItem> impacts = new ArrayList<ImpactItem>();
    public List<String> contentWarnings = new ArrayList<String>();
    public Admission admission;

    /**
     * Tipe implementasi bersarang {@link Institution} milik {@link HomePortalViewModel}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link HomePortalViewModel}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code Long hospitalId},
     * {@code Long schoolId}, {@code Long foundationId}, {@code String type}, {@code String name}, {@code String
     * shortName}, {@code String motto}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     *
     * @see HomePortalViewModel
     */
    public static class Institution {
        public Long id;
        public Long hospitalId;
        public Long schoolId;
        public Long foundationId;
        public String type;
        public String name;
        public String shortName;
        public String motto;
        public String address;
        public String phone;
        public String email;
        public String logoUrl;
        public String heroUrl;
        public String themeCss;
        public String themePrimary;
        public String themePrimaryDark;
        public String category;
        public boolean college;
        public boolean healthcare;
    }

    /**
     * Tipe implementasi bersarang {@link Terminology} milik {@link HomePortalViewModel}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link HomePortalViewModel}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String learnerPlural}, {@code String
     * teacherPlural}, {@code String admissionLabel}, {@code String programLabel}, {@code String institutionLabel}.
     * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see HomePortalViewModel
     */
    public static class Terminology {
        public String learnerPlural;
        public String teacherPlural;
        public String admissionLabel;
        public String programLabel;
        public String institutionLabel;
    }

    /**
     * Tipe implementasi bersarang {@link Seo} milik {@link HomePortalViewModel}. Kelas ini memberi nama pada state
     * atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link HomePortalViewModel}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String title}, {@code String
     * description}, {@code String canonical}, {@code String image}, {@code String jsonLd}. Aturan bisnis bersama
     * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see HomePortalViewModel
     */
    public static class Seo {
        public String title;
        public String description;
        public String canonical;
        public String image;
        public String jsonLd;
    }

    /**
     * Tipe implementasi bersarang {@link LinkItem} milik {@link HomePortalViewModel}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link HomePortalViewModel}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String label}, {@code String url},
     * {@code String target}, {@code String rel}. Aturan bisnis bersama tetap berada pada kelas induk atau service
     * yang dipanggilnya.</p>
     *
     * @see HomePortalViewModel
     */
    public static class LinkItem {
        public String label;
        public String url;
        public String target;
        public String rel;
    }

    /**
     * Tipe implementasi bersarang {@link ServiceItem} milik {@link HomePortalViewModel}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link HomePortalViewModel}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String description}, {@code String
     * icon}, {@code String group}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     *
     * @see HomePortalViewModel
     */
    public static class ServiceItem extends LinkItem {
        public String description;
        public String icon;
        public String group;
    }

    /**
     * Tipe implementasi bersarang {@link ProgramItem} milik {@link HomePortalViewModel}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link HomePortalViewModel}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String level}, {@code String
     * accreditation}, {@code String unit}, {@code String description}. Aturan bisnis bersama tetap berada pada
     * kelas induk atau service yang dipanggilnya.</p>
     *
     * @see HomePortalViewModel
     */
    public static class ProgramItem extends LinkItem {
        public String level;
        public String accreditation;
        public String unit;
        public String description;
    }

    /**
     * Tipe implementasi bersarang {@link StatItem} milik {@link HomePortalViewModel}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link HomePortalViewModel}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String value}, {@code String label}.
     * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see HomePortalViewModel
     */
    public static class StatItem {
        public String value;
        public String label;
    }

    /**
     * Tipe implementasi bersarang {@link NewsItem} milik {@link HomePortalViewModel}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link HomePortalViewModel}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String summary}, {@code String date},
     * {@code String category}, {@code String imageUrl}. Aturan bisnis bersama tetap berada pada kelas induk atau
     * service yang dipanggilnya.</p>
     *
     * @see HomePortalViewModel
     */
    public static class NewsItem extends LinkItem {
        public String summary;
        public String date;
        public String category;
        public String imageUrl;
    }

    /**
     * Tipe implementasi bersarang {@link AgendaItem} milik {@link HomePortalViewModel}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link HomePortalViewModel}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String day}, {@code String month},
     * {@code String date}, {@code String description}. Aturan bisnis bersama tetap berada pada kelas induk atau
     * service yang dipanggilnya.</p>
     *
     * @see HomePortalViewModel
     */
    public static class AgendaItem extends LinkItem {
        public String day;
        public String month;
        public String date;
        public String description;
    }

    /**
     * Tipe implementasi bersarang {@link ImpactItem} milik {@link HomePortalViewModel}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link HomePortalViewModel}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String description}, {@code String
     * icon}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see HomePortalViewModel
     */
    public static class ImpactItem extends LinkItem {
        public String description;
        public String icon;
    }

    /**
     * Tipe implementasi bersarang {@link Admission} milik {@link HomePortalViewModel}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link HomePortalViewModel}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String period}, {@code String
     * startDate}, {@code String endDate}, {@code String description}, {@code boolean open}. Aturan bisnis bersama
     * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see HomePortalViewModel
     */
    public static class Admission extends LinkItem {
        public String period;
        public String startDate;
        public String endDate;
        public String description;
        public boolean open;
    }
}
