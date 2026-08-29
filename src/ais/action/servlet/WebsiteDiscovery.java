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

/** Tenant-aware robots.txt and XML sitemap generator. */
public class WebsiteDiscovery extends HttpServlet {
    private static final long serialVersionUID = 1L;

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

    private void collect(Set<String> urls, String origin, WebsitePageViewModel page) {
        for (WebsitePageViewModel.Card card : page.cards) {
            if (card.url == null || card.url.length() == 0) continue;
            if (card.url.startsWith("http://") || card.url.startsWith("https://")) urls.add(card.url);
            else if (card.url.startsWith("/")) urls.add(origin + card.url);
        }
    }

    private void add(Set<String> urls, String web, String path) { urls.add(path.length() == 0 ? web : web + "/" + path); }
    private String origin(String canonical, String requestUri) {
        if (canonical != null && requestUri != null && canonical.endsWith(requestUri)) return canonical.substring(0, canonical.length() - requestUri.length());
        return canonical == null ? "" : canonical.replaceFirst("(/[^/]*)?$", "");
    }
    private String xml(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    private String json(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", " ").replace("\n", " ").replace("<", "\\u003c"); }
}
