package ais.common.home;

import java.util.ArrayList;
import java.util.List;

/** Data contract for indexable public pages below /web. */
public class WebsitePageViewModel {
    public HomePortalViewModel portal;
    public String path;
    public String eyebrow;
    public String title;
    public String description;
    public String body;
    public String updatedLabel;
    public String schemaType = "WebPage";
    public String publishedIso;
    public String startIso;
    public String endIso;
    public String jsonLd;
    public boolean searchPage;
    public boolean notFound;
    public String searchQuery;
    public List<Section> sections = new ArrayList<Section>();
    public List<Card> cards = new ArrayList<Card>();
    public List<Crumb> breadcrumbs = new ArrayList<Crumb>();

    /**
     * Tipe implementasi bersarang {@link Section} milik {@link WebsitePageViewModel}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link WebsitePageViewModel}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String title}, {@code String body}.
     * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see WebsitePageViewModel
     */
    public static class Section {
        public String title;
        public String body;
    }

    /**
     * Tipe implementasi bersarang {@link Card} milik {@link WebsitePageViewModel}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link WebsitePageViewModel}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String eyebrow}, {@code String
     * title}, {@code String summary}, {@code String meta}, {@code String url}. Aturan bisnis bersama tetap berada
     * pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see WebsitePageViewModel
     */
    public static class Card {
        public String eyebrow;
        public String title;
        public String summary;
        public String meta;
        public String url;
    }

    /**
     * Tipe implementasi bersarang {@link Crumb} milik {@link WebsitePageViewModel}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link WebsitePageViewModel}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String label}, {@code String url}.
     * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see WebsitePageViewModel
     */
    public static class Crumb {
        public String label;
        public String url;
    }
}
