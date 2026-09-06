package ais.action.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.home.HomePortalService;
import ais.common.home.HomePortalViewModel;
import ais.common.home.WebsitePageService;
import ais.common.home.WebsitePageViewModel;

/**
 * Generator publik multi-format untuk metadata website institusi: {@code robots.txt},
 * {@code sitemap.xml}, {@code website.webmanifest} (PWA manifest), dan
 * {@code website-feed.xml} (RSS berita). Semua endpoint bersifat anonim/publik dan tenant-aware
 * (mengikuti institusi yang di-resolve oleh {@link HomePortalService#buildWebsite}); tidak ada
 * data sensitif yang diekspos -- hanya metadata SEO/PWA yang memang ditujukan untuk mesin
 * pencari, browser, dan pembaca feed publik.
 */
public class WebsiteDiscovery extends HttpServlet {
    /** Versi serialisasi tetap 1L; servlet tidak pernah benar-benar diserialisasi ke stream. */
    private static final long serialVersionUID = 1L;

    /**
     * Titik masuk tunggal; membangun model tampilan portal lalu bercabang berdasarkan akhiran
     * path permintaan ke salah satu penyaji: {@link #robots}, {@link #manifest}, {@link #feed},
     * atau (default) {@link #sitemap}. Publik/anonim -- tidak ada gerbang otentikasi karena
     * seluruh keluaran memang ditujukan untuk konsumsi publik/robot.
     *
     * @param req permintaan HTTP masuk; path (robots.txt/website.webmanifest/website-feed.xml/lainnya)
     *        menentukan penyaji yang dipanggil
     * @param res respons HTTP keluar
     * @throws ServletException tidak pernah dilempar langsung, hanya dideklarasikan oleh kontrak servlet
     * @throws IOException jika terjadi galat I/O saat menulis respons
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            HomePortalViewModel vm = new HomePortalService().buildWebsite(req);
            String origin = origin(vm.seo.canonical, req.getRequestURI());
            if (req.getRequestURI().endsWith("robots.txt")) robots(res, origin + req.getContextPath());
            else if (req.getRequestURI().endsWith("website.webmanifest")) manifest(res, req, vm, origin);
            else if (req.getRequestURI().endsWith("website-feed.xml")) feed(res, req, vm, origin);
            else sitemap(res, req, vm, origin);
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "WebsiteDiscovery");
            res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Menulis berkas {@code robots.txt} standar: mengizinkan crawler di {@code /web} dan
     * {@code /sekolah/}, melarang rute internal ({@code /login}, {@code /main}, {@code /baru}),
     * dan menunjuk lokasi {@code sitemap.xml}.
     *
     * @param res respons HTTP keluar; content type diset {@code text/plain}
     * @param base URL dasar (origin + context path) yang dipakai untuk menyusun tautan sitemap
     * @throws IOException jika terjadi galat I/O saat menulis respons
     */
    private void robots(HttpServletResponse res, String base) throws IOException {
        res.setContentType("text/plain; charset=UTF-8");
        res.setHeader("Cache-Control", "public, max-age=3600");
        PrintWriter out = res.getWriter();
        out.println("User-agent: *");
        out.println("Allow: /web");
        out.println("Allow: /sekolah/");
        out.println("Disallow: /login");
        out.println("Disallow: /main");
        out.println("Disallow: /baru");
        out.println("Sitemap: " + base + "/sitemap.xml");
    }

    /**
     * Menyusun dan menulis {@code sitemap.xml} berisi URL halaman publik institusi: halaman
     * sekolah (jika ada), landing {@code /web}, sejumlah halaman statis bersama (profil,
     * program, penerimaan, dll.), halaman khusus jenjang (riset untuk perguruan tinggi;
     * pembelajaran/orang-tua/perlindungan-anak untuk non-kesehatan), serta halaman dinamis
     * program/berita/agenda yang dikumpulkan lewat {@link WebsitePageService}. Duplikat URL
     * dihilangkan otomatis via {@link LinkedHashSet}.
     *
     * @param res respons HTTP keluar; content type diset {@code application/xml}
     * @param req permintaan HTTP masuk, dipakai untuk context path
     * @param vm model tampilan portal (informasi institusi) hasil {@link HomePortalService}
     * @param origin skema+host+port asal permintaan
     * @throws IOException jika terjadi galat I/O saat menulis respons
     */
    private void sitemap(HttpServletResponse res, HttpServletRequest req, HomePortalViewModel vm, String origin) throws IOException {
        Set<String> urls = new LinkedHashSet<String>();
        String web = origin + req.getContextPath() + "/web";
        if (vm.institution.schoolId != null) {
            urls.add(origin + req.getContextPath() + "/sekolah/" + vm.institution.schoolId);
        }
        add(urls, web, "");
        String[] shared = {"profil", "program", "penerimaan", "layanan", "berita", "agenda", "dokumen", "akreditasi", "staf", "beasiswa", "kehidupan-kampus", "kontak", "privasi", "aksesibilitas", "ppid"};
        for (String path : shared) add(urls, web, path);
        if (vm.institution.college) add(urls, web, "riset");
        else if (!vm.institution.healthcare) { add(urls, web, "pembelajaran"); add(urls, web, "orang-tua"); add(urls, web, "perlindungan-anak"); }

        WebsitePageService pages = new WebsitePageService();
        collect(urls, origin, pages.build(vm, "/program", null));
        collect(urls, origin, pages.build(vm, "/berita", null));
        collect(urls, origin, pages.build(vm, "/agenda", null));

        res.setContentType("application/xml; charset=UTF-8");
        res.setHeader("Cache-Control", "public, max-age=900, stale-while-revalidate=3600");
        PrintWriter out = res.getWriter();
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
        for (String url : urls) out.println("  <url><loc>" + xml(url) + "</loc></url>");
        out.println("</urlset>");
    }

    /**
     * Menulis {@code website.webmanifest} (PWA manifest) berisi identitas, warna tema, dan ikon
     * institusi, sehingga situs publik dapat "diinstal" sebagai aplikasi web progresif.
     *
     * @param res respons HTTP keluar; content type diset {@code application/manifest+json}
     * @param req permintaan HTTP masuk, dipakai untuk context path
     * @param vm model tampilan portal (nama, nama singkat, deskripsi, bahasa, warna tema, logo)
     * @param origin skema+host+port asal permintaan
     * @throws IOException jika terjadi galat I/O saat menulis respons
     */
    private void manifest(HttpServletResponse res, HttpServletRequest req, HomePortalViewModel vm, String origin) throws IOException {
        res.setContentType("application/manifest+json; charset=UTF-8");
        res.setHeader("Cache-Control", "public, max-age=3600");
        String start = origin + req.getContextPath() + "/web";
        res.getWriter().write("{\"id\":\"" + json(start) + "\",\"name\":\"" + json(vm.institution.name)
                + "\",\"short_name\":\"" + json(vm.institution.shortName) + "\",\"description\":\""
                + json(vm.description) + "\",\"lang\":\"" + json(vm.language) + "\",\"start_url\":\""
                + json(start) + "\",\"scope\":\"" + json(origin + req.getContextPath() + "/web/")
                + "\",\"display\":\"standalone\",\"background_color\":\"#ffffff\",\"theme_color\":\""
                + json(vm.institution.themePrimary == null || vm.institution.themePrimary.length() == 0 ? "#163d78" : vm.institution.themePrimary) + "\",\"icons\":[{\"src\":\"" + json(vm.institution.logoUrl)
                + "\",\"sizes\":\"any\",\"purpose\":\"any\"}]}" );
    }

    /**
     * Menulis {@code website-feed.xml} berupa RSS 2.0 dari daftar berita terbaru institusi
     * ({@code /berita}), untuk dikonsumsi pembaca feed publik.
     *
     * @param res respons HTTP keluar; content type diset {@code application/rss+xml}
     * @param req permintaan HTTP masuk, dipakai untuk context path
     * @param vm model tampilan portal (nama institusi untuk judul feed)
     * @param origin skema+host+port asal permintaan, dipakai menyusun tautan mutlak tiap item
     * @throws IOException jika terjadi galat I/O saat menulis respons
     */
    private void feed(HttpServletResponse res, HttpServletRequest req, HomePortalViewModel vm, String origin) throws IOException {
        String web = origin + req.getContextPath() + "/web";
        WebsitePageViewModel news = new WebsitePageService().build(vm, "/berita", null);
        res.setContentType("application/rss+xml; charset=UTF-8");
        res.setHeader("Cache-Control", "public, max-age=900, stale-while-revalidate=3600");
        PrintWriter out = res.getWriter();
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<rss version=\"2.0\"><channel><title>" + xml(vm.institution.name) + "</title><link>" + xml(web) + "</link><description>Berita resmi institusi</description>");
        for (WebsitePageViewModel.Card card : news.cards) {
            String link = card.url.startsWith("http") ? card.url : origin + card.url;
            out.println("<item><title>" + xml(card.title) + "</title><link>" + xml(link) + "</link><guid isPermaLink=\"true\">" + xml(link) + "</guid><description>" + xml(card.summary) + "</description></item>");
        }
        out.println("</channel></rss>");
    }

    /**
     * Menambahkan URL kartu (card) halaman dinamis ke himpunan URL sitemap, mengubah URL relatif
     * (diawali {@code /}) menjadi absolut dengan {@code origin}; URL yang sudah absolut
     * ({@code http://}/{@code https://}) dipakai apa adanya; URL kosong diabaikan.
     *
     * @param urls himpunan URL sitemap yang sedang dibangun (dimutasi/ditambah isinya)
     * @param origin skema+host+port asal permintaan, dipakai untuk melengkapi URL relatif
     * @param page halaman dinamis (mis. hasil {@link WebsitePageService#build}) berisi daftar kartu
     */
    private void collect(Set<String> urls, String origin, WebsitePageViewModel page) {
        for (WebsitePageViewModel.Card card : page.cards) {
            if (card.url == null || card.url.length() == 0) continue;
            if (card.url.startsWith("http://") || card.url.startsWith("https://")) urls.add(card.url);
            else if (card.url.startsWith("/")) urls.add(origin + card.url);
        }
    }

    /**
     * Menambahkan satu URL halaman statis ke himpunan sitemap: {@code web} itu sendiri jika
     * {@code path} kosong, atau {@code web + "/" + path} selain itu.
     *
     * @param urls himpunan URL sitemap yang sedang dibangun (dimutasi/ditambah isinya)
     * @param web URL dasar landing page ({@code /web}) institusi
     * @param path segmen path halaman statis; string kosong berarti {@code web} itu sendiri
     */
    private void add(Set<String> urls, String web, String path) { urls.add(path.length() == 0 ? web : web + "/" + path); }

    /**
     * Menurunkan origin (skema+host+port, tanpa path) dari URL kanonis SEO bila tersedia, atau
     * dengan memotong segmen path terakhir dari nilai kanonis sebagai fallback.
     *
     * @param canonical URL kanonis SEO dari model tampilan; boleh {@code null}
     * @param requestUri path permintaan saat ini, dipakai mencocokkan akhiran URL kanonis
     * @return origin yang diturunkan; string kosong jika {@code canonical} {@code null}
     */
    private String origin(String canonical, String requestUri) {
        if (canonical != null && requestUri != null && canonical.endsWith(requestUri)) return canonical.substring(0, canonical.length() - requestUri.length());
        return canonical == null ? "" : canonical.replaceFirst("(/[^/]*)?$", "");
    }

    /**
     * Meng-escape karakter spesial XML ({@code & < > "}) agar nilai aman disisipkan ke dalam
     * dokumen XML (sitemap/RSS).
     *
     * @param value nilai mentah yang akan disisipkan ke XML
     * @return nilai yang sudah di-escape
     */
    private String xml(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }

    /**
     * Meng-escape karakter spesial JSON ({@code \ "}), menghapus baris baru, dan menetralkan
     * {@code <} (jadi {@code <}) agar nilai aman disisipkan ke dalam string JSON manifest
     * (mencegah injeksi ke dalam tag {@code <script>} bila manifest disisipkan HTML).
     *
     * @param value nilai mentah yang akan disisipkan ke JSON; boleh {@code null}
     * @return nilai yang sudah di-escape; string kosong jika {@code value} {@code null}
     */
    private String json(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", " ").replace("\n", " ").replace("<", "\\u003c"); }
}
