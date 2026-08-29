<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.Collections"%>
<%@ page import="java.util.List"%>
<%@ page import="org.json.JSONArray"%>
<%@ page import="org.json.JSONObject"%>
<%@ page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@ page import="ais.action.servlet.landing.PesantrenWebsiteConfig"%>
<%@ page import="ais.action.servlet.landing.PesantrenLandingService.Profil"%>
<%@ page import="ais.action.servlet.landing.PesantrenLandingService.UnitPendidikan"%>
<%@ page import="ais.action.servlet.landing.PesantrenLandingService.Berita"%>
<%@ page import="ais.common.Common"%>
<%!
    private static String e(Object value) {
        return StringEscapeUtils.escapeHtml(value == null ? "" : String.valueOf(value));
    }
    private static String href(String value, String fallback) {
        String v = value == null ? "" : value.trim();
        if (v.startsWith("https://") || v.startsWith("http://") || (v.startsWith("/") && !v.startsWith("//"))
                || v.startsWith("#") || v.startsWith("mailto:") || v.startsWith("tel:")) {
            return StringEscapeUtils.escapeHtml(v);
        }
        return StringEscapeUtils.escapeHtml(fallback == null ? "#" : fallback);
    }
    private static String image(String value, String fallback) {
        String v = value == null ? "" : value.trim();
        if (v.startsWith("https://") || (v.startsWith("/") && !v.startsWith("//"))) return v;
        return fallback == null ? "" : fallback;
    }
    private static String jsonScript(JSONObject value) {
        return value.toString().replace("&", "\\u0026").replace("<", "\\u003c").replace(">", "\\u003e");
    }
    private static String digits(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }
    private static JSONObject item(JSONArray values, int index) {
        JSONObject value = values == null ? null : values.optJSONObject(index);
        return value == null ? new JSONObject() : value;
    }
    private static String t(JSONObject value, String key, String fallback) {
        return PesantrenWebsiteConfig.text(value, key, fallback);
    }
    private static String color(JSONObject value, String key, String fallback) {
        String candidate = t(value, key, fallback);
        return candidate.matches("#[0-9a-fA-F]{6}") ? candidate : fallback;
    }
%>
<%
    Profil profil = (Profil) request.getAttribute("pesantrenProfil");
    if (profil == null) {
        profil = new Profil("Pondok Pesantren", "Ilmu, adab, dan kemandirian", "", "", "", "", "", "",
                "#0f766e", request.getContextPath() + "/img/logo.png", request.getContextPath() + "/img/main.jpg");
    }
    List<UnitPendidikan> sekolahs = (List<UnitPendidikan>) request.getAttribute("pesantrenSekolah");
    List<UnitPendidikan> perguruanTinggis = (List<UnitPendidikan>) request.getAttribute("pesantrenPerguruanTinggi");
    List<Berita> berita = (List<Berita>) request.getAttribute("pesantrenBerita");
    if (sekolahs == null) sekolahs = Collections.emptyList();
    if (perguruanTinggis == null) perguruanTinggis = Collections.emptyList();
    if (berita == null) berita = Collections.emptyList();
    String root = request.getContextPath();
    JSONObject site = (JSONObject) request.getAttribute("pesantrenWebsite");
    if (site == null) site = new JSONObject();
    JSONObject identity = PesantrenWebsiteConfig.object(site, "identity");
    JSONObject theme = PesantrenWebsiteConfig.object(site, "theme");
    JSONObject seo = PesantrenWebsiteConfig.object(site, "seo");
    JSONObject announcement = PesantrenWebsiteConfig.object(site, "announcement");
    JSONArray navigation = PesantrenWebsiteConfig.array(site, "navigation");
    JSONObject hero = PesantrenWebsiteConfig.object(site, "hero");
    JSONObject heroPrimary = PesantrenWebsiteConfig.object(hero, "primaryAction");
    JSONObject heroSecondary = PesantrenWebsiteConfig.object(hero, "secondaryAction");
    JSONObject heroTertiary = PesantrenWebsiteConfig.object(hero, "tertiaryAction");
    JSONObject profileContent = PesantrenWebsiteConfig.object(site, "profile");
    JSONArray stats = PesantrenWebsiteConfig.array(site, "stats");
    JSONObject unitsSection = PesantrenWebsiteConfig.object(site, "unitsSection");
    JSONObject pillarsSection = PesantrenWebsiteConfig.object(site, "pillarsSection");
    JSONArray pillars = PesantrenWebsiteConfig.array(site, "pillars");
    JSONObject dailySection = PesantrenWebsiteConfig.object(site, "dailySection");
    JSONArray dailyTimeline = PesantrenWebsiteConfig.array(site, "dailyTimeline");
    JSONObject workflowSection = PesantrenWebsiteConfig.object(site, "workflowSection");
    JSONArray workflows = PesantrenWebsiteConfig.array(site, "workflows");
    JSONObject diagramSection = PesantrenWebsiteConfig.object(site, "diagramSection");
    JSONArray diagrams = PesantrenWebsiteConfig.array(site, "diagrams");
    JSONObject biometric = PesantrenWebsiteConfig.object(site, "biometric");
    JSONObject servicesSection = PesantrenWebsiteConfig.object(site, "servicesSection");
    JSONArray serviceGroups = PesantrenWebsiteConfig.array(site, "serviceGroups");
    JSONObject gallerySection = PesantrenWebsiteConfig.object(site, "gallerySection");
    JSONArray gallery = PesantrenWebsiteConfig.array(site, "gallery");
    JSONObject newsSection = PesantrenWebsiteConfig.object(site, "newsSection");
    JSONObject cta = PesantrenWebsiteConfig.object(site, "cta");
    JSONObject footerContent = PesantrenWebsiteConfig.object(site, "footer");
    String language = Common.kodeBahasaAktif();
    if (language == null || language.trim().isEmpty()) language = "id";
    String direction = "ar".equalsIgnoreCase(language) ? "rtl" : "ltr";
    String nonce = String.valueOf(request.getAttribute("pesantrenCspNonce"));
    if ("null".equals(nonce)) nonce = "";
    String seoTitle = t(seo, "title", profil.getNama() + " - Portal ePesantren");
    String seoDescription = t(seo, "description", "Portal terpadu " + profil.getNama());
    String canonical = t(seo, "canonical", root + "/index");
    if (!(canonical.startsWith("https://") || canonical.startsWith("http://")
            || (canonical.startsWith("/") && !canonical.startsWith("//")))) canonical = root + "/index";
    String socialImage = image(t(theme, "heroImage", profil.getLatar()), root + "/img/pesantren/hero-default-v1.png");
    JSONObject structuredData = new JSONObject();
    structuredData.put("@context", "https://schema.org");
    structuredData.put("@type", "EducationalOrganization");
    structuredData.put("name", profil.getNama());
    structuredData.put("description", seoDescription);
    if (canonical.startsWith("http")) structuredData.put("url", canonical);
    if (!profil.getLogo().isEmpty()) structuredData.put("logo", image(profil.getLogo(), root + "/img/logo.png"));
    if (!profil.getAlamat().isEmpty()) structuredData.put("address", profil.getAlamat());
    if (!profil.getTelepon().isEmpty()) structuredData.put("telephone", profil.getTelepon());
    if (!profil.getEmail().isEmpty()) structuredData.put("email", profil.getEmail());
    String wa = digits(profil.getWa());
    if (wa.startsWith("0")) wa = "62" + wa.substring(1);
%>
<!doctype html>
<html lang="<%=e(language)%>" dir="<%=direction%>">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="<%=e(profil.getWarna())%>">
    <meta name="description" content="<%=e(seoDescription)%>">
    <meta name="robots" content="index,follow,max-image-preview:large">
    <meta property="og:type" content="website">
    <meta property="og:locale" content="id_ID">
    <meta property="og:title" content="<%=e(seoTitle)%>">
    <meta property="og:description" content="<%=e(seoDescription)%>">
    <meta property="og:url" content="<%=e(canonical)%>">
    <meta property="og:image" content="<%=e(socialImage)%>">
    <meta property="og:site_name" content="<%=e(profil.getNama())%>">
    <meta name="twitter:card" content="summary_large_image">
    <meta name="twitter:title" content="<%=e(seoTitle)%>">
    <meta name="twitter:description" content="<%=e(seoDescription)%>">
    <link rel="canonical" href="<%=e(canonical)%>">
    <link rel="icon" href="<%=e(image(profil.getLogo(), root + "/img/logo.png"))%>">
    <link rel="manifest" href="<%=e(root)%>/website.webmanifest">
    <link rel="alternate" type="application/rss+xml" title="Kabar <%=e(profil.getNama())%>" href="<%=e(root)%>/website-feed.xml">
    <title><%=e(seoTitle)%></title>
    <script type="application/ld+json" nonce="<%=e(nonce)%>"><%=jsonScript(structuredData)%></script>
    <style nonce="<%=e(nonce)%>">
        :root {
            --primary: <%=e(profil.getWarna())%>;
            --ink: <%=e(color(theme, "ink", "#102a2a"))%>;
            --muted: #607475;
            --cream: <%=e(color(theme, "cream", "#fbfaf5"))%>;
            --paper: #ffffff;
            --gold: <%=e(color(theme, "secondary", "#c79a3b"))%>;
            --emerald: #0f766e;
            --emerald-dark: #073f3d;
            --line: rgba(15, 118, 110, .14);
            --shadow: 0 22px 70px rgba(7, 63, 61, .12);
            --radius: 26px;
        }
        * { box-sizing: border-box; }
        html { scroll-behavior: smooth; }
        body { margin: 0; color: var(--ink); background: linear-gradient(180deg, var(--cream), #f7f5ed); font: 16px/1.65 Inter, "Segoe UI", Arial, sans-serif; }
        a { color: inherit; }
        img { max-width: 100%; }
        a:focus-visible, summary:focus-visible { outline: 3px solid var(--gold); outline-offset: 4px; border-radius: 8px; }
        .skip { position: fixed; left: 16px; top: -80px; z-index: 999; padding: 10px 16px; background: #fff; border-radius: 12px; }
        .skip:focus { top: 12px; }
        .announcement { padding: 9px 20px; color: #fff; background: var(--emerald-dark); text-align: center; font-size: 14px; }
        .announcement a { color: #f2d68e; font-weight: 800; text-decoration: none; }
        .wrap { width: min(1180px, calc(100% - 40px)); margin: auto; }
        .topbar { position: sticky; top: 0; z-index: 50; background: rgba(251,250,245,.94); backdrop-filter: blur(16px); border-top: 3px solid var(--gold); border-bottom: 1px solid var(--line); }
        .topbar-inner { min-height: 78px; display: flex; align-items: center; justify-content: space-between; gap: 28px; }
        .brand { display: flex; align-items: center; gap: 12px; text-decoration: none; min-width: 0; }
        .brand img { width: 46px; height: 46px; object-fit: contain; border-radius: 12px; background: #fff; padding: 4px; }
        .brand strong { display: block; font: 700 17px/1.2 Georgia, serif; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 330px; }
        .brand span { display: block; color: var(--muted); font-size: 12px; letter-spacing: .1em; text-transform: uppercase; }
        .desktop-nav { display: flex; align-items: center; gap: 22px; }
        .desktop-nav a { text-decoration: none; font-size: 14px; font-weight: 650; }
        .mobile-nav { display: none; position: relative; }
        .mobile-nav summary { display: grid; place-items: center; width: 44px; height: 44px; border: 1px solid var(--line); border-radius: 14px; background: #fff; color: var(--ink); cursor: pointer; list-style: none; font-size: 0; }
        .mobile-nav summary::-webkit-details-marker { display: none; }
        .mobile-nav summary::before { content: "☰"; font-size: 22px; line-height: 1; }
        .mobile-nav[open] summary::before { content: "×"; font-size: 28px; }
        .mobile-menu { position: absolute; right: 0; top: 54px; display: grid; width: min(320px, calc(100vw - 28px)); padding: 14px; border: 1px solid var(--line); border-radius: 20px; background: #fff; box-shadow: var(--shadow); }
        .mobile-menu a { padding: 12px 14px; border-radius: 12px; text-decoration: none; font-weight: 750; }
        .mobile-menu a:hover { background: #eef5f2; }
        .mobile-menu .button { margin-top: 8px; color: #fff; }
        .button { display: inline-flex; align-items: center; justify-content: center; gap: 9px; min-height: 46px; padding: 0 20px; border-radius: 999px; border: 1px solid transparent; text-decoration: none; font-weight: 750; transition: transform .2s ease, box-shadow .2s ease; }
        .button:hover { transform: translateY(-2px); }
        .button-primary { color: #fff; background: var(--primary); box-shadow: 0 12px 28px rgba(15,118,110,.22); }
        .button-light { color: var(--ink); background: #fff; border-color: rgba(255,255,255,.55); }
        .button-outline { border-color: var(--line); background: rgba(255,255,255,.72); }
        .hero { position: relative; overflow: hidden; min-height: 670px; color: #fff; background: var(--emerald-dark); }
        .hero-media { position: absolute; inset: 0; z-index: 0; width: 100%; height: 100%; object-fit: cover; object-position: center; }
        .hero::before { content: ""; position: absolute; inset: 0; z-index: 1; background: linear-gradient(90deg, rgba(4,45,43,.97) 0%, rgba(4,45,43,.9) 48%, rgba(4,45,43,.45) 100%); }
        .hero::after { content: ""; position: absolute; inset: 0; z-index: 1; opacity: .18; background-image: radial-gradient(circle at 20px 20px, #e4bf6a 1.5px, transparent 1.8px); background-size: 42px 42px; mask-image: linear-gradient(to right, #000, transparent 78%); }
        .hero-grid { position: relative; z-index: 2; display: grid; grid-template-columns: 1.12fr .88fr; gap: 70px; align-items: center; min-height: 670px; padding: 84px 0; }
        .eyebrow { display: inline-flex; align-items: center; gap: 10px; color: #f2d68e; font-size: 13px; font-weight: 800; letter-spacing: .16em; text-transform: uppercase; }
        .eyebrow::before { content: ""; width: 34px; height: 1px; background: currentColor; }
        h1, h2, h3 { font-family: Georgia, "Times New Roman", serif; }
        h1 { max-width: 760px; margin: 18px 0 20px; font-size: clamp(44px, 6vw, 78px); line-height: 1.04; letter-spacing: -.035em; }
        .hero-lead { max-width: 720px; margin: 0 0 30px; color: rgba(255,255,255,.82); font-size: 19px; }
        .hero-actions { display: flex; flex-wrap: wrap; gap: 12px; }
        .hero-card { align-self: end; padding: 30px; border: 1px solid rgba(255,255,255,.18); border-radius: 32px; background: rgba(255,255,255,.1); backdrop-filter: blur(14px); box-shadow: 0 26px 80px rgba(0,0,0,.22); }
        .hero-card img { width: 80px; height: 80px; object-fit: contain; padding: 7px; border-radius: 20px; background: #fff; }
        .hero-card h2 { margin: 22px 0 8px; font-size: 30px; line-height: 1.18; }
        .hero-card p { margin: 0; color: rgba(255,255,255,.75); }
        .hero-facts { display: grid; grid-template-columns: repeat(3,1fr); gap: 10px; margin-top: 26px; }
        .fact { padding: 15px 10px; text-align: center; border-radius: 16px; background: rgba(0,0,0,.14); }
        .fact strong { display: block; font-size: 20px; color: #f3d786; }
        .fact span { font-size: 11px; color: rgba(255,255,255,.7); }
        .stat-ribbon { position: relative; z-index: 5; margin-top: -42px; }
        .stat-grid { display: grid; grid-template-columns: repeat(4,1fr); border-radius: 24px; overflow: hidden; background: #fff; box-shadow: var(--shadow); }
        .stat-item { padding: 24px; text-align: center; border-right: 1px solid var(--line); }
        .stat-item:last-child { border-right: 0; }
        .stat-item strong { display: block; color: var(--primary); font: 700 25px Georgia,serif; }
        .stat-item span { color: var(--muted); font-size: 12px; font-weight: 750; }
        section { padding: 96px 0; }
        .section-head { display: grid; grid-template-columns: .85fr 1.15fr; gap: 60px; align-items: end; margin-bottom: 46px; }
        .section-head h2 { margin: 10px 0 0; font-size: clamp(36px, 4vw, 56px); line-height: 1.08; letter-spacing: -.025em; }
        .section-head p { margin: 0; color: var(--muted); font-size: 18px; }
        .kicker { color: var(--primary); font-weight: 850; letter-spacing: .14em; text-transform: uppercase; font-size: 12px; }
        .unit-grid, .news-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
        .unit, .news { position: relative; padding: 28px; background: var(--paper); border: 1px solid var(--line); border-radius: var(--radius); box-shadow: 0 12px 36px rgba(16,42,42,.06); }
        .unit::before { content: ""; position: absolute; left: 0; top: 26px; bottom: 26px; width: 4px; border-radius: 0 4px 4px 0; background: var(--primary); }
        .label { display: inline-block; padding: 5px 10px; border-radius: 999px; color: var(--emerald-dark); background: #e6f3ef; font-size: 11px; font-weight: 800; text-transform: uppercase; letter-spacing: .08em; }
        .unit h3, .news h3 { margin: 16px 0 8px; font-size: 24px; line-height: 1.2; }
        .unit p, .news p { color: var(--muted); margin: 0 0 18px; }
        .unit a { color: var(--primary); font-weight: 800; text-decoration: none; }
        .pillars { background: #eef5f2; }
        .pillar-grid { display: grid; grid-template-columns: repeat(4,1fr); gap: 18px; counter-reset: pilar; }
        .pillar { min-height: 240px; padding: 28px; border-radius: 24px; background: #fff; border: 1px solid rgba(15,118,110,.11); counter-increment: pilar; }
        .pillar::before { content: "0" counter(pilar); display: block; margin-bottom: 45px; color: var(--gold); font: 700 14px Georgia, serif; }
        .pillar h3 { margin: 0 0 10px; font-size: 22px; line-height: 1.18; }
        .pillar p { margin: 0; color: var(--muted); font-size: 14px; }
        .biometric { background: var(--emerald-dark); color: #fff; overflow: hidden; }
        .bio-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 72px; align-items: center; }
        .bio-grid h2 { margin: 12px 0 20px; font-size: clamp(38px,5vw,62px); line-height: 1.06; }
        .bio-grid > div > p { color: rgba(255,255,255,.75); font-size: 18px; }
        .bio-flow { display: grid; gap: 14px; }
        .bio-step { display: grid; grid-template-columns: 48px 1fr; gap: 16px; align-items: start; padding: 19px; background: rgba(255,255,255,.08); border: 1px solid rgba(255,255,255,.13); border-radius: 18px; }
        .bio-step b { display: grid; place-items: center; width: 48px; height: 48px; border-radius: 15px; background: #f1d27d; color: var(--emerald-dark); }
        .bio-step strong { display: block; margin-bottom: 3px; }
        .bio-step span { color: rgba(255,255,255,.7); font-size: 14px; }
        .services { background: #fff; }
        .service-columns { display: grid; grid-template-columns: repeat(3,1fr); gap: 22px; }
        .service-group { padding: 26px; border-radius: 24px; background: var(--cream); border: 1px solid var(--line); }
        .service-group h3 { margin: 0 0 18px; font-size: 24px; }
        .service-links { display: grid; gap: 9px; }
        .service-links a { display: flex; justify-content: space-between; gap: 12px; padding: 12px 14px; border-radius: 13px; text-decoration: none; background: #fff; border: 1px solid rgba(15,118,110,.1); font-weight: 700; font-size: 14px; }
        .service-links a::after { content: "↗"; color: var(--primary); }
        .daily { background: #fff; }
        .timeline { position: relative; display: grid; grid-template-columns: repeat(4,1fr); gap: 18px; }
        .timeline::before { content: ""; position: absolute; left: 8%; right: 8%; top: 35px; height: 2px; background: linear-gradient(90deg,var(--gold),var(--primary)); }
        .timeline-item { position: relative; padding: 72px 24px 26px; border: 1px solid var(--line); border-radius: 24px; background: var(--cream); }
        .timeline-item::before { content: ""; position: absolute; top: 24px; left: 24px; width: 22px; height: 22px; border: 7px solid #fff; border-radius: 50%; background: var(--primary); box-shadow: 0 0 0 1px var(--line); }
        .timeline-item time { color: var(--gold); font-weight: 900; font-size: 12px; }
        .timeline-item h3 { margin: 10px 0; font-size: 20px; }
        .timeline-item p { margin: 0; color: var(--muted); font-size: 14px; }
        .workflows { background: #eef5f2; }
        .workflow-list { display: grid; gap: 26px; }
        .workflow-card { overflow: hidden; border: 1px solid var(--line); border-radius: 28px; background: #fff; box-shadow: 0 12px 34px rgba(16,42,42,.06); }
        .workflow-head { display: grid; grid-template-columns: .65fr 1.35fr; gap: 22px; padding: 25px 28px; color: #fff; background: var(--emerald-dark); }
        .workflow-head small { color: #f2d68e; text-transform: uppercase; letter-spacing: .12em; font-weight: 850; }
        .workflow-head h3 { margin: 4px 0 0; font-size: 27px; }
        .workflow-head p { margin: 4px 0 0; color: rgba(255,255,255,.72); }
        .workflow-steps { display: grid; grid-template-columns: repeat(auto-fit,minmax(130px,1fr)); padding: 28px; }
        .workflow-step { position: relative; min-height: 132px; padding: 48px 16px 16px; text-align: center; }
        .workflow-step:not(:last-child)::after { content: "→"; position: absolute; right: -9px; top: 52px; color: var(--gold); font-size: 25px; font-weight: 900; }
        .workflow-step b { position: absolute; left: 50%; top: 0; display: grid; place-items: center; width: 36px; height: 36px; transform: translateX(-50%); border-radius: 50%; color: #fff; background: var(--primary); }
        .workflow-step strong { display: block; margin-bottom: 5px; font-size: 14px; }
        .workflow-step span { color: var(--muted); font-size: 12px; }
        .diagrams { background: var(--emerald-dark); color: #fff; }
        .diagrams .section-head p { color: rgba(255,255,255,.72); }
        .diagrams .kicker { color: #f2d68e; }
        .diagram-grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 22px; }
        .diagram { min-height: 390px; padding: 26px; border: 1px solid rgba(255,255,255,.14); border-radius: 28px; background: rgba(255,255,255,.07); }
        .diagram h3 { margin: 0 0 28px; font-size: 24px; }
        .diagram-center { display: grid; place-items: center; min-height: 82px; margin: 22px 30px; padding: 16px; border-radius: 20px; color: var(--emerald-dark); background: #f1d27d; font-weight: 900; text-align: center; }
        .diagram-items { display: flex; flex-wrap: wrap; justify-content: center; gap: 9px; }
        .diagram-items span { position: relative; padding: 9px 12px; border: 1px solid rgba(255,255,255,.16); border-radius: 999px; background: rgba(255,255,255,.08); font-size: 12px; font-weight: 750; }
        .diagram-items span::before { content: "↕"; margin-right: 6px; color: #f2d68e; }
        .gallery { background: #fff; }
        .gallery-grid { display: grid; grid-template-columns: 1.35fr .85fr .85fr; grid-auto-rows: 330px; gap: 18px; }
        .gallery-item { position: relative; overflow: hidden; border-radius: 26px; background: linear-gradient(135deg,var(--emerald-dark),var(--primary)); }
        .gallery-item img { width: 100%; height: 100%; object-fit: cover; transition: transform .5s ease; }
        .gallery-item:hover img { transform: scale(1.035); }
        .gallery-copy { position: absolute; inset: auto 0 0; padding: 60px 25px 24px; color: #fff; background: linear-gradient(transparent,rgba(4,45,43,.92)); }
        .gallery-copy h3 { margin: 0 0 4px; font-size: 22px; }
        .gallery-copy p { margin: 0; color: rgba(255,255,255,.76); font-size: 13px; }
        .gallery-placeholder { display: grid; place-items: center; height: 100%; padding: 28px; color: rgba(255,255,255,.8); text-align: center; }
        .news time { color: var(--gold); font-weight: 800; font-size: 12px; text-transform: uppercase; letter-spacing: .08em; }
        .empty { padding: 42px; text-align: center; border: 1px dashed rgba(15,118,110,.28); border-radius: 24px; color: var(--muted); }
        .content-warning { margin: 22px auto 0; padding: 14px 18px; border: 1px solid #e4c875; border-radius: 16px; color: #5d4b12; background: #fff8d9; font-size: 14px; }
        .news-link { display: inline-flex; align-items: center; gap: 8px; color: var(--primary); text-decoration: none; font-weight: 850; }
        .news-link::after { content: "→"; }
        .contact { padding-top: 0; }
        .contact-card { display: grid; grid-template-columns: 1.2fr .8fr; gap: 44px; padding: 54px; border-radius: 34px; background: linear-gradient(135deg, var(--primary), var(--emerald-dark)); color: #fff; box-shadow: var(--shadow); }
        .contact-card h2 { margin: 8px 0 16px; font-size: 44px; line-height: 1.05; }
        .contact-card p { color: rgba(255,255,255,.78); }
        .contact-list { display: grid; gap: 10px; align-content: center; }
        .contact-list a, .contact-list span { padding: 12px 16px; border-radius: 14px; background: rgba(255,255,255,.09); text-decoration: none; }
        footer { padding: 38px 0; color: var(--muted); border-top: 1px solid var(--line); background: #fff; }
        .footer-inner { display: flex; justify-content: space-between; gap: 20px; flex-wrap: wrap; }
        .footer-links { display: flex; gap: 14px; flex-wrap: wrap; }
        .footer-links a { color: var(--ink); text-decoration-thickness: 1px; text-underline-offset: 4px; }
        @media (max-width: 980px) {
            .desktop-nav { display: none; }
            .mobile-nav { display: block; }
            .hero-grid, .bio-grid, .contact-card, .section-head { grid-template-columns: 1fr; }
            .hero-grid { gap: 30px; }
            .hero-card { align-self: auto; }
            .unit-grid, .news-grid, .service-columns, .diagram-grid { grid-template-columns: repeat(2,1fr); }
            .pillar-grid { grid-template-columns: repeat(2,1fr); }
            .timeline { grid-template-columns: repeat(2,1fr); }
            .workflow-steps { grid-template-columns: repeat(3,1fr); }
            .gallery-grid { grid-template-columns: 1fr 1fr; }
        }
        @media (max-width: 640px) {
            .wrap { width: min(100% - 26px, 1180px); }
            .topbar-inner { min-height: 68px; }
            .brand strong { max-width: 180px; }
            .mobile-nav .button { padding: 0 14px; min-height: 40px; }
            .hero-grid { min-height: auto; padding: 68px 0; }
            h1 { font-size: 42px; }
            section { padding: 72px 0; }
            .unit-grid, .news-grid, .service-columns, .pillar-grid, .timeline, .diagram-grid, .gallery-grid { grid-template-columns: 1fr; }
            .stat-grid { grid-template-columns: repeat(2,1fr); }
            .stat-item:nth-child(2) { border-right: 0; }
            .workflow-head { grid-template-columns: 1fr; }
            .workflow-steps { grid-template-columns: 1fr; }
            .workflow-step:not(:last-child)::after { content: "↓"; right: auto; left: 50%; top: auto; bottom: -13px; }
            .pillar { min-height: 190px; }
            .pillar::before { margin-bottom: 28px; }
            .contact-card { padding: 34px 24px; }
            .contact-card h2 { font-size: 36px; }
        }
        @media (prefers-reduced-motion: reduce) { html { scroll-behavior: auto; } * { transition: none !important; } }
    </style>
</head>
<body>
<a class="skip" href="#konten">Lewati ke konten utama</a>
<% if (announcement.optBoolean("enabled", false) && !t(announcement, "text", "").isEmpty()) { %>
<div class="announcement"><strong><%=e(t(announcement, "label", "Informasi"))%>:</strong>
    <a href="<%=href(t(announcement, "url", "#berita"), "#berita")%>"><%=e(t(announcement, "text", ""))%></a>
</div>
<% } %>
<header class="topbar">
    <div class="wrap topbar-inner">
        <a class="brand" href="#awal" aria-label="Beranda <%=e(profil.getNama())%>">
            <img src="<%=href(profil.getLogo(), root + "/img/logo.png")%>" alt="Logo <%=e(profil.getNama())%>">
            <span><strong><%=e(profil.getNama())%></strong><span><%=e(t(identity, "shortName", "ePesantren"))%></span></span>
        </a>
        <nav class="desktop-nav" aria-label="Navigasi utama">
            <% for (int i = 0; i < navigation.length(); i++) { JSONObject menu = item(navigation, i); %>
            <a href="<%=href(t(menu, "url", "#"), "#")%>"><%=e(t(menu, "label", "Menu"))%></a>
            <% } %>
            <a class="button button-primary" href="<%=href(t(heroPrimary, "url", root + "/login"), "#")%>"><%=e(t(heroPrimary, "label", "Masuk Sistem"))%></a>
        </nav>
        <details class="mobile-nav">
            <summary aria-label="Buka navigasi">Menu</summary>
            <nav class="mobile-menu" aria-label="Navigasi seluler">
                <% for (int i = 0; i < navigation.length(); i++) { JSONObject menu = item(navigation, i); %>
                <a href="<%=href(t(menu, "url", "#"), "#")%>"><%=e(t(menu, "label", "Menu"))%></a>
                <% } %>
                <a class="button button-primary" href="<%=href(t(heroPrimary, "url", root + "/login"), "#")%>"><%=e(t(heroPrimary, "label", "Masuk Sistem"))%></a>
            </nav>
        </details>
    </div>
</header>
<% if (Boolean.TRUE.equals(request.getAttribute("pesantrenContentWarning"))) { %>
<div class="wrap content-warning" role="status">Sebagian informasi dinamis sedang tidak tersedia. Tautan layanan utama tetap dapat digunakan.</div>
<% } %>
<main id="konten">
    <section class="hero" id="awal">
        <img class="hero-media" src="<%=e(image(profil.getLatar(), root + "/img/pesantren/hero-default-v1.png"))%>" alt="" aria-hidden="true" fetchpriority="high" decoding="async">
        <div class="wrap hero-grid">
            <div>
                <div class="eyebrow"><%=e(t(hero, "eyebrow", "Satu Pesantren · Satu Sistem · Satu Data"))%></div>
                <h1><%=e(t(hero, "title", profil.getNama()))%></h1>
                <p class="hero-lead"><%=e(t(hero, "lead", profil.getMotto()))%></p>
                <div class="hero-actions">
                    <a class="button button-primary" href="<%=href(t(heroPrimary, "url", root + "/login"), "#")%>"><%=e(t(heroPrimary, "label", "Masuk ePesantren"))%></a>
                    <a class="button button-light" href="<%=href(t(heroSecondary, "url", "#layanan"), "#layanan")%>"><%=e(t(heroSecondary, "label", "Jelajahi Layanan"))%></a>
                    <a class="button button-outline" href="<%=href(t(heroTertiary, "url", root + "/psb"), "#")%>"><%=e(t(heroTertiary, "label", "Pendaftaran Santri"))%></a>
                </div>
            </div>
            <aside class="hero-card" id="profil" aria-label="Profil lembaga">
                <img src="<%=href(profil.getLogo(), root + "/img/logo.png")%>" alt="">
                <h2><%=e(t(profileContent, "title", "Berakar pada adab, bertumbuh dengan teknologi"))%></h2>
                <p><%=e(t(profileContent, "body", profil.getDeskripsi()))%></p>
                <div class="hero-facts"><% for (int i = 0; i < Math.min(3, stats.length()); i++) { JSONObject factItem = item(stats, i); String factValue = t(factItem, "value", ""); if ("Dinamis".equalsIgnoreCase(factValue)) factValue = String.valueOf(sekolahs.size() + perguruanTinggis.size()); %><div class="fact"><strong><%=e(factValue)%></strong><span><%=e(t(factItem, "label", "Statistik"))%></span></div><% } %></div>
            </aside>
        </div>
    </section>

    <% if (stats.length() > 0) { %>
    <div class="stat-ribbon"><div class="wrap"><div class="stat-grid">
        <% for (int i = 0; i < stats.length(); i++) { JSONObject stat = item(stats, i); String nilai = t(stat, "value", ""); if ("Dinamis".equalsIgnoreCase(nilai)) nilai = String.valueOf(sekolahs.size() + perguruanTinggis.size()); %>
        <div class="stat-item"><strong><%=e(nilai)%></strong><span><%=e(t(stat, "label", "Statistik"))%></span></div>
        <% } %>
    </div></div></div>
    <% } %>

    <% if (PesantrenWebsiteConfig.visible(site, "units", true)) { %>
    <section id="unit">
        <div class="wrap">
            <div class="section-head"><div><span class="kicker"><%=e(t(unitsSection, "eyebrow", "Jaringan pendidikan"))%></span><h2><%=e(t(unitsSection, "title", "Satu yayasan, seluruh unit terhubung"))%></h2></div><p><%=e(t(unitsSection, "body", "Sekolah, madrasah, dan perguruan tinggi tampil langsung dari data AIS."))%></p></div>
            <% if (sekolahs.isEmpty() && perguruanTinggis.isEmpty()) { %><div class="empty">Unit pendidikan belum dipublikasikan. Administrator dapat melengkapinya pada data Yayasan dan Sekolah.</div><% } else { %>
            <div class="unit-grid">
                <% for (UnitPendidikan unit : sekolahs) { %><article class="unit"><span class="label"><%=e(unit.getJenis())%></span><h3><%=e(unit.getNama())%></h3><p><%=e(unit.getMotto().isEmpty() ? unit.getAlamat() : unit.getMotto())%></p><a href="<%=href(unit.getUrl(), root + "/login")%>">Kunjungi unit →</a></article><% } %>
                <% for (UnitPendidikan unit : perguruanTinggis) { %><article class="unit"><span class="label"><%=e(unit.getJenis())%></span><h3><%=e(unit.getNama())%></h3><p><%=e(unit.getMotto().isEmpty() ? unit.getAlamat() : unit.getMotto())%></p><a href="<%=href(unit.getUrl(), root + "/login")%>">Kunjungi unit →</a></article><% } %>
            </div><% } %>
        </div>
    </section>
    <% } %>

    <% if (PesantrenWebsiteConfig.visible(site, "pillars", true)) { %>
    <section class="pillars" id="ekosistem">
        <div class="wrap">
            <div class="section-head"><div><span class="kicker"><%=e(t(pillarsSection, "eyebrow", "Ekosistem ePesantren"))%></span><h2><%=e(t(pillarsSection, "title", "Pilar kehidupan pondok"))%></h2></div><p><%=e(t(pillarsSection, "body", "Seluruh layanan memakai identitas dan audit yang sama."))%></p></div>
            <div class="pillar-grid">
                <% for (int i = 0; i < pillars.length(); i++) { JSONObject pillar = item(pillars, i); %>
                <article class="pillar"><h3><%=e(t(pillar, "title", "Layanan"))%></h3><p><%=e(t(pillar, "description", ""))%></p></article>
                <% } %>
            </div>
        </div>
    </section>
    <% } %>

    <% if (PesantrenWebsiteConfig.visible(site, "dailyTimeline", true)) { %>
    <section class="daily" id="alur-harian"><div class="wrap">
        <div class="section-head"><div><span class="kicker"><%=e(t(dailySection, "eyebrow", "Operasional sehari-hari"))%></span><h2><%=e(t(dailySection, "title", "Sistem mengikuti ritme pondok"))%></h2></div><p><%=e(t(dailySection, "body", ""))%></p></div>
        <div class="timeline">
            <% for (int i = 0; i < dailyTimeline.length(); i++) { JSONObject dailyItem = item(dailyTimeline, i); %>
            <article class="timeline-item"><time><%=e(t(dailyItem, "time", ""))%></time><h3><%=e(t(dailyItem, "title", "Agenda"))%></h3><p><%=e(t(dailyItem, "description", ""))%></p></article>
            <% } %>
        </div>
    </div></section>
    <% } %>

    <% if (PesantrenWebsiteConfig.visible(site, "workflows", true)) { %>
    <section class="workflows" id="workflow"><div class="wrap">
        <div class="section-head"><div><span class="kicker"><%=e(t(workflowSection, "eyebrow", "Workflow operasional"))%></span><h2><%=e(t(workflowSection, "title", "Alur kerja terlihat sebelum fitur dibuka"))%></h2></div><p><%=e(t(workflowSection, "body", ""))%></p></div>
        <div class="workflow-list">
            <% for (int i = 0; i < workflows.length(); i++) { JSONObject flow = item(workflows, i); JSONArray steps = PesantrenWebsiteConfig.array(flow, "steps"); %>
            <article class="workflow-card">
                <div class="workflow-head"><div><small>Workflow <%=e(String.valueOf(i + 1))%></small><h3><%=e(t(flow, "title", "Alur kerja"))%></h3></div><p><%=e(t(flow, "subtitle", ""))%></p></div>
                <div class="workflow-steps">
                    <% for (int j = 0; j < steps.length(); j++) { JSONObject step = item(steps, j); %>
                    <div class="workflow-step"><b><%=e(String.valueOf(j + 1))%></b><strong><%=e(t(step, "title", "Langkah"))%></strong><span><%=e(t(step, "description", ""))%></span></div>
                    <% } %>
                </div>
            </article>
            <% } %>
        </div>
    </div></section>
    <% } %>

    <% if (PesantrenWebsiteConfig.visible(site, "diagrams", true)) { %>
    <section class="diagrams" id="diagram"><div class="wrap">
        <div class="section-head"><div><span class="kicker"><%=e(t(diagramSection, "eyebrow", "Diagram ekosistem"))%></span><h2><%=e(t(diagramSection, "title", "Satu identitas menghubungkan seluruh layanan"))%></h2></div><p><%=e(t(diagramSection, "body", ""))%></p></div>
        <div class="diagram-grid">
            <% for (int i = 0; i < diagrams.length(); i++) { JSONObject diagram = item(diagrams, i); JSONArray diagramItems = PesantrenWebsiteConfig.array(diagram, "items"); %>
            <article class="diagram"><h3><%=e(t(diagram, "title", "Diagram"))%></h3><div class="diagram-center"><%=e(t(diagram, "center", profil.getNama()))%></div><div class="diagram-items">
                <% for (int j = 0; j < diagramItems.length(); j++) { %><span><%=e(diagramItems.optString(j, ""))%></span><% } %>
            </div></article>
            <% } %>
        </div>
    </div></section>
    <% } %>

    <% if (PesantrenWebsiteConfig.visible(site, "biometric", true)) { %>
    <section class="biometric" id="biometrik">
        <div class="wrap bio-grid">
            <div><span class="eyebrow"><%=e(t(biometric, "eyebrow", "Identitas biometrik local-first"))%></span><h2><%=e(t(biometric, "title", "Satu verifikasi untuk layanan penting santri"))%></h2><p><%=e(t(biometric, "body", ""))%></p><% JSONObject bioAction = PesantrenWebsiteConfig.object(biometric, "action"); %><a class="button button-light" href="<%=href(t(bioAction, "url", root + "/login"), "#")%>"><%=e(t(bioAction, "label", "Kelola melalui akun berwenang"))%></a></div>
            <div class="bio-flow"><% JSONArray bioSteps = PesantrenWebsiteConfig.array(biometric, "steps"); for (int i = 0; i < bioSteps.length(); i++) { JSONObject bioStep = item(bioSteps, i); %>
                <div class="bio-step"><b><%=e(String.format("%02d", Integer.valueOf(i + 1)))%></b><div><strong><%=e(t(bioStep, "title", "Tahap biometrik"))%></strong><span><%=e(t(bioStep, "description", ""))%></span></div></div>
                <% } %>
            </div>
        </div>
    </section>
    <% } %>

    <% if (PesantrenWebsiteConfig.visible(site, "services", true)) { %>
    <section class="services" id="layanan">
        <div class="wrap">
            <div class="section-head"><div><span class="kicker"><%=e(t(servicesSection, "eyebrow", "Fasilitas digital"))%></span><h2><%=e(t(servicesSection, "title", "Seluruh pintu layanan dalam satu halaman"))%></h2></div><p><%=e(t(servicesSection, "body", ""))%></p></div>
            <div class="service-columns">
                <% for (int i = 0; i < serviceGroups.length(); i++) { JSONObject group = item(serviceGroups, i); JSONArray links = PesantrenWebsiteConfig.array(group, "items"); %>
                <div class="service-group"><h3><%=e(t(group, "title", "Layanan"))%></h3><div class="service-links">
                    <% for (int j = 0; j < links.length(); j++) { JSONObject link = item(links, j); %><a href="<%=href(t(link, "url", "#"), "#")%>"><%=e(t(link, "label", "Buka layanan"))%></a><% } %>
                </div></div>
                <% } %>
            </div>
        </div>
    </section>
    <% } %>

    <% if (PesantrenWebsiteConfig.visible(site, "gallery", true)) { %>
    <section class="gallery" id="galeri"><div class="wrap">
        <div class="section-head"><div><span class="kicker"><%=e(t(gallerySection, "eyebrow", "Suasana dan fasilitas"))%></span><h2><%=e(t(gallerySection, "title", "Kehidupan pondok dalam gambar"))%></h2></div><p><%=e(t(gallerySection, "body", ""))%></p></div>
        <div class="gallery-grid">
            <% for (int i = 0; i < gallery.length(); i++) { JSONObject photo = item(gallery, i); String image = t(photo, "image", ""); %>
            <article class="gallery-item"><% if (!image.isEmpty()) { %><img src="<%=e(image(image, root + "/img/pesantren/hero-default-v1.png"))%>" alt="<%=e(t(photo, "title", "Foto pondok"))%>" loading="lazy" decoding="async"><% } else { %><div class="gallery-placeholder">Foto dapat diisi dari JSON Yayasan tanpa mengubah JSP.</div><% } %><div class="gallery-copy"><h3><%=e(t(photo, "title", "Kegiatan pondok"))%></h3><p><%=e(t(photo, "caption", ""))%></p></div></article>
            <% } %>
        </div>
    </div></section>
    <% } %>

    <% if (PesantrenWebsiteConfig.visible(site, "news", true)) { %>
    <section id="berita">
        <div class="wrap">
            <div class="section-head"><div><span class="kicker"><%=e(t(newsSection, "eyebrow", "Kabar pondok"))%></span><h2><%=e(t(newsSection, "title", "Pengumuman umum terbaru"))%></h2></div><p><%=e(t(newsSection, "body", ""))%></p></div>
            <% if (berita.isEmpty()) { %><div class="empty">Belum ada pengumuman umum yang dipublikasikan.</div><% } else { %><div class="news-grid"><% for (Berita item : berita) { %><article class="news"><time><%=e(item.getTanggal())%></time><h3><%=e(item.getJudul())%></h3><p><%=e(item.getRingkasan())%></p><a class="news-link" href="<%=e(root)%>/web/berita/<%=e(item.getId())%>" aria-label="Baca selengkapnya: <%=e(item.getJudul())%>">Baca selengkapnya</a></article><% } %></div><% } %>
        </div>
    </section>
    <% } %>

    <% if (PesantrenWebsiteConfig.visible(site, "contact", true)) { %>
    <section class="contact" id="kontak"><div class="wrap"><div class="contact-card"><div><span class="eyebrow"><%=e(t(cta, "eyebrow", "Terhubung dengan pondok"))%></span><h2><%=e(t(cta, "title", "Informasi dan layanan resmi " + profil.getNama()))%></h2><p><%=e(t(cta, "body", profil.getAlamat()))%></p><div class="hero-actions"><a class="button button-light" href="<%=href(t(cta, "primaryUrl", root + "/login"), "#")%>"><%=e(t(cta, "primaryLabel", "Masuk Sistem"))%></a><% if (!wa.isEmpty()) { %><a class="button button-outline" href="https://wa.me/<%=e(wa)%>"><%=e(t(cta, "secondaryLabel", "Hubungi WhatsApp"))%></a><% } %></div></div><div class="contact-list"><% if (!profil.getTelepon().isEmpty()) { %><a href="tel:<%=e(digits(profil.getTelepon()))%>">Telepon · <%=e(profil.getTelepon())%></a><% } %><% if (!profil.getEmail().isEmpty()) { %><a href="mailto:<%=e(profil.getEmail())%>">Email · <%=e(profil.getEmail())%></a><% } %><% if (!profil.getWebsite().isEmpty()) { %><a href="<%=href(profil.getWebsite(), "#")%>">Website resmi</a><% } %><span>Seluruh konten halaman ini dapat disesuaikan dari JSON Website pada master Yayasan.</span></div></div></div></section>
    <% } %>
</main>
<footer><div class="wrap footer-inner"><span>© <%=new java.text.SimpleDateFormat("yyyy").format(new java.util.Date())%> <%=e(t(footerContent, "left", profil.getNama()))%></span><nav class="footer-links" aria-label="Informasi situs"><a href="<%=e(root)%>/web/privasi">Privasi</a><a href="<%=e(root)%>/web/aksesibilitas">Aksesibilitas</a><a href="<%=e(root)%>/web/ppid">PPID</a></nav><span><%=e(t(footerContent, "right", "Didukung ePesantren · CV. Zishof"))%></span></div></footer>
</body>
</html>
