<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
    private String dmsHeaderH(Object value) {
        if (value == null) return "";
        String s = String.valueOf(value);
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    String namaInstansi = "Enterprise Education";
    try {
        Object conf = Common.getKonfigurasi("judul_header", "Enterprise Education");
        if (conf != null) {
            java.lang.reflect.Method m = conf.getClass().getMethod("getNilai", new Class[0]);
            Object nilai = m.invoke(conf, new Object[0]);
            if (nilai != null && String.valueOf(nilai).trim().length() > 0) {
                namaInstansi = String.valueOf(nilai).trim();
            }
        }
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/dms/_header_dms.jsp:21");}

    // Tema institusi: MENGIKUTI tema terpilih (pt.getCss()) persis seperti modul Portal Vendor.
    // base-theme.css hanya default; file tema institusi menimpanya sehingga variabel --theme-* ikut,
    // dan seluruh warna DMS (yang sudah diturunkan dari --theme-*) otomatis mengikuti tema.
    String cssTema = "";
    try {
        ais.database.model.PerguruanTinggi ptDms = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request);
        if (ptDms != null) {
            String rawCssDms = ptDms.getCss();
            if (rawCssDms != null && rawCssDms.trim().length() > 0) {
                String fileNameDms = rawCssDms.substring(rawCssDms.lastIndexOf("/") + 1);
                cssTema = "/css/baru/" + fileNameDms;
            }
        }
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/dms/_header_dms.jsp:36");}

    String baseUrl = request.getAttribute("DMS_BASE_URL") == null ? request.getContextPath() + "/document" : String.valueOf(request.getAttribute("DMS_BASE_URL"));
    String userDisplay = request.getAttribute("DMS_USER_DISPLAY") == null ? "Pengunjung" : String.valueOf(request.getAttribute("DMS_USER_DISPLAY"));
    Boolean loggedObjHeader = (Boolean) request.getAttribute("DMS_LOGGED_IN");
    boolean loggedHeader = loggedObjHeader != null && loggedObjHeader.booleanValue();
    long cacheBusterHeader = System.currentTimeMillis();
%>
<!doctype html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= Common.getBahasaConfig("Portal Dokumen Manajemen Sistem") %> | <%= dmsHeaderH(namaInstansi) %></title>
    <meta name="description" content="Portal Dokumen Manajemen Sistem untuk melihat katalog dokumen resmi, pedoman, SOP, formulir, arsip akreditasi, dan informasi institusi.">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
    <link href="<%=request.getContextPath()%>/css/baru/base-theme.css?v=<%=cacheBusterHeader%>" rel="stylesheet">
    <% if (cssTema != null && cssTema.trim().length() > 0) { %>
        <link href="<%=request.getContextPath()%><%=cssTema%>?v=<%=cacheBusterHeader%>" rel="stylesheet">
    <% } %>
    <style>
        :root {
            /* Warna brand DMS MENGIKUTI TEMA terpilih (base-theme.css). Nilai #hex hanya fallback
               bila variabel tema belum ada. Pola sama dengan modul Portal KARIR. */
            --dms-primary:var(--theme-primary, #2563eb);
            --dms-primary-dark:var(--theme-primary-dark, #1e40af);
            --dms-accent:var(--theme-hover, #06b6d4);
            --dms-success:#16a34a;   /* warna status — independen tema */
            --dms-warning:#f59e0b;   /* warna status — independen tema */
            --dms-slate:#0f172a;
            --dms-muted:#64748b;
            --dms-soft:#f8fafc;
            --dms-border:#e2e8f0;
            --dms-gradient:linear-gradient(135deg, var(--theme-primary-dark, #0f172a) 0%, var(--theme-primary, #2563eb) 52%, var(--theme-hover, #06b6d4) 100%);
        }
        html { scroll-behavior:smooth; }
        body.dms-public-body { background:#f8fafc; color:#1e293b; min-height:100vh; overflow-x:hidden; }
        .dms-bg-pattern { position:fixed; inset:0; pointer-events:none; z-index:-2; background:radial-gradient(circle at 10% 5%, var(--theme-light, rgba(37,99,235,.16)), transparent 28%), radial-gradient(circle at 90% 20%, rgba(6,182,212,.16), transparent 25%), radial-gradient(circle at 50% 95%, rgba(22,163,74,.10), transparent 24%), #f8fafc; }
        .dms-navbar { background:rgba(255,255,255,.94); backdrop-filter:blur(16px); border-bottom:1px solid rgba(226,232,240,.92); box-shadow:0 8px 26px rgba(15,23,42,.04); }
        .dms-brand-icon { width:44px; height:44px; border-radius:16px; display:flex; align-items:center; justify-content:center; background:var(--dms-gradient); color:#fff; box-shadow:0 12px 26px rgba(15,23,42,.22); }
        .dms-hero { position:relative; overflow:hidden; background:var(--dms-gradient); color:#fff; border-radius:0 0 34px 34px; box-shadow:0 22px 55px rgba(15,23,42,.24); }
        .dms-hero:before { content:""; position:absolute; inset:0; background:radial-gradient(circle at 14% 20%, rgba(255,255,255,.26), transparent 24%), radial-gradient(circle at 82% 0%, rgba(255,255,255,.18), transparent 31%); opacity:.95; }
        .dms-hero .container { position:relative; z-index:1; }
        .dms-pill { border-radius:999px; background:rgba(255,255,255,.16); border:1px solid rgba(255,255,255,.25); padding:.48rem .9rem; display:inline-flex; align-items:center; gap:.45rem; color:#fff; font-size:.86rem; font-weight:800; }
        .dms-card { border:1px solid rgba(226,232,240,.92); border-radius:24px; box-shadow:0 18px 45px rgba(15,23,42,.075); background:rgba(255,255,255,.98); }
        .dms-glass { background:rgba(255,255,255,.12); border:1px solid rgba(255,255,255,.18); border-radius:24px; box-shadow:0 20px 45px rgba(15,23,42,.15); backdrop-filter:blur(12px); }
        .dms-feature { height:100%; border:1px solid var(--dms-border); border-radius:20px; padding:1.2rem; background:#fff; transition:.25s ease; }
        .dms-feature:hover { transform:translateY(-4px); box-shadow:0 18px 34px rgba(15,23,42,.10); }
        .dms-feature i { width:44px; height:44px; border-radius:15px; display:inline-flex; align-items:center; justify-content:center; background:var(--theme-light, #eff6ff); color:var(--dms-primary); margin-bottom:.85rem; }
        .dms-section-title { font-weight:900; letter-spacing:.2px; color:#0f172a; }
        .dms-muted { color:#64748b; }
        .dms-service-box { min-height:300px; }
        .dms-loading { padding:2rem; text-align:center; color:#64748b; }
        .dms-breadcrumb .btn { border-radius:999px; padding:.38rem .78rem; font-size:.82rem; }
        .dms-entry { border:1px solid var(--dms-border); border-radius:20px; padding:1rem; background:#fff; height:100%; transition:.25s ease; position:relative; overflow:hidden; }
        .dms-entry:before { content:""; position:absolute; left:0; top:0; bottom:0; width:5px; background:linear-gradient(180deg,var(--dms-primary),var(--dms-accent)); opacity:.75; }
        .dms-entry:hover { transform:translateY(-4px); box-shadow:0 18px 36px rgba(15,23,42,.12); border-color:#bfdbfe; }
        .dms-entry-icon { width:54px; height:54px; border-radius:18px; display:flex; align-items:center; justify-content:center; background:#f8fafc; font-size:1.45rem; flex:0 0 auto; }
        .dms-entry-title { font-weight:900; color:#0f172a; word-break:break-word; }
        .dms-empty { border:2px dashed #cbd5e1; border-radius:22px; padding:2.6rem; text-align:center; color:#64748b; background:#f8fafc; }
        .dms-footer { background:#0f172a; color:#cbd5e1; margin-top:4rem; }
        .dms-footer a { color:#fff; text-decoration:none; }
        .btn-dms-gradient { background:linear-gradient(135deg,var(--dms-primary),var(--dms-accent)); border:0; color:#fff; box-shadow:0 10px 24px rgba(15,23,42,.20); }
        .btn-dms-gradient:hover { color:#fff; transform:translateY(-2px); box-shadow:0 16px 30px rgba(15,23,42,.28); }
        .dms-clamp { display:-webkit-box; -webkit-line-clamp:3; -webkit-box-orient:vertical; overflow:hidden; }
        .dms-metric-card { border:1px solid var(--dms-border); background:#fff; border-radius:20px; padding:1rem; height:100%; box-shadow:0 12px 26px rgba(15,23,42,.055); }
        .dms-metric-icon { width:42px; height:42px; border-radius:15px; display:flex; align-items:center; justify-content:center; background:var(--theme-light, #eff6ff); color:var(--dms-primary); }
        .dms-css-bar { height:8px; background:#e2e8f0; border-radius:999px; overflow:hidden; }
        .dms-css-bar span { display:block; height:100%; border-radius:999px; background:linear-gradient(90deg,var(--dms-primary),var(--dms-accent)); min-width:16%; }
        .dms-help-panel { border-radius:22px; border:1px solid #dbeafe; background:linear-gradient(135deg,var(--theme-light, #eff6ff),#ffffff); padding:1.2rem; }
        .dms-login-modal .modal-content { border:0; border-radius:26px; overflow:hidden; box-shadow:0 30px 75px rgba(15,23,42,.27); }
        .dms-login-side { background:var(--dms-gradient); color:#fff; }
        .dms-login-icon { width:58px; height:58px; border-radius:20px; display:flex; align-items:center; justify-content:center; background:rgba(255,255,255,.18); color:#fff; }
        @media (max-width:767.98px) {
            .dms-hero { border-radius:0 0 24px 24px; }
            .display-5 { font-size:2rem; }
            .dms-navbar .btn { width:100%; margin-top:.5rem; }
        }
    </style>
</head>
<body class="dms-public-body" data-dms-base-url="<%=dmsHeaderH(baseUrl)%>">
<div class="dms-bg-pattern"></div>
<nav class="navbar navbar-expand-lg dms-navbar sticky-top">
    <div class="container">
        <a class="navbar-brand d-flex align-items-center gap-2 fw-bold" href="<%=dmsHeaderH(baseUrl)%>">
            <span class="dms-brand-icon"><i class="fa fa-folder-tree"></i></span>
            <span>
                <span class="d-block"><%= Common.getBahasaConfig("Portal Dokumen") %></span>
                <small class="d-block text-muted fw-semibold" style="font-size:.72rem;"><%= dmsHeaderH(namaInstansi) %></small>
            </span>
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#dmsNav" aria-controls="dmsNav" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="dmsNav">
            <ul class="navbar-nav ms-auto align-items-lg-center gap-lg-2">
                <li class="nav-item"><a class="nav-link fw-semibold" href="#dokumen"><i class="fa fa-layer-group me-1"></i>Dokumen</a></li>
                <li class="nav-item"><a class="nav-link fw-semibold" href="#ketentuan"><i class="fa fa-shield-halved me-1"></i>Ketentuan</a></li>
                <% if (loggedHeader) { %>
                    <li class="nav-item"><span class="badge text-bg-success rounded-pill px-3 py-2"><i class="fa fa-user-check me-1"></i><%= dmsHeaderH(userDisplay) %></span></li>
                <% } else { %>
                    <li class="nav-item"><button type="button" class="btn btn-sm btn-dms-gradient rounded-pill px-3" data-dms-login-open="true"><i class="fa fa-right-to-bracket me-1"></i>Login untuk Download</button></li>
                <% } %>
            </ul>
        </div>
    </div>
</nav>
