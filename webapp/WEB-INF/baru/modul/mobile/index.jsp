<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ page import="ais.action.mobile.MobileNotifHelper" %>
<%@ page import="ais.action.mobile.MobileNotifHelper.NotifItem" %>
<%@ page import="ais.action.master.helper.util.PerguruanTinggiUtil" %>
<%@ page import="ais.common.Common" %>
<%@ page import="ais.common.CommonMenu" %>
<%@ page import="ais.database.model.Menu" %>
<%@ page import="ais.database.model.PerguruanTinggi" %>
<%@ page import="ais.database.model.Tbmrole" %>
<%@ page import="ais.database.model.Tbmuser" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%!
/* ── Helper methods ─────────────────────────────────────── */
private static String esc(String s) {
    if (s == null) return "";
    return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
}
private static String js(String s) {
    if (s == null) return "";
    return s.replace("\\","\\\\").replace("'","\\'").replace("\n","\\n").replace("\r","");
}
private static String initials(String name) {
    if (name == null || name.trim().isEmpty()) return "?";
    String[] parts = name.trim().split("\\s+");
    if (parts.length == 1) return parts[0].substring(0,1).toUpperCase();
    return (parts[0].substring(0,1) + parts[parts.length-1].substring(0,1)).toUpperCase();
}
private static String urlSafe(String raw) {
    if (raw == null) return "";
    return raw.replaceAll("\\p{Punct}","");
}
/* JSON string escaping */
private static String jsonStr(String s) {
    if (s == null) return "\"\"";
    return "\"" + s.replace("\\","\\\\").replace("\"","\\\"")
                   .replace("\n","\\n").replace("\r","").replace("\t","\\t") + "\"";
}
private static List rootMenus(List all) {
    List r = new ArrayList();
    if (all == null) return r;
    for (int i = 0; i < all.size(); i++) {
        Menu m = (Menu) all.get(i);
        if (m.getRoot() != null && m.getRoot().equals(0L)
                && (m.getAktif() == null || m.getAktif())) {
            r.add(m);
        }
    }
    return r;
}
private static List childMenus(Long parentChild, List all) {
    List r = new ArrayList();
    if (all == null || parentChild == null) return r;
    for (int i = 0; i < all.size(); i++) {
        Menu m = (Menu) all.get(i);
        if (parentChild.equals(m.getRoot())
                && (m.getAktif() == null || m.getAktif())) {
            r.add(m);
        }
    }
    return r;
}
private static boolean hasChildren(Menu m, List all) {
    if (m == null || m.getChild() == null) return false;
    return !childMenus(m.getChild(), all).isEmpty();
}
/* Warna tile berputar berdasarkan indeks */
private static String[] TILE_COLORS = {
    "#3b82f6","#10b981","#f59e0b","#8b5cf6",
    "#ef4444","#06b6d4","#f97316","#ec4899",
    "#14b8a6","#64748b","#6366f1","#84cc16"
};
private static String tileColor(int idx) {
    return TILE_COLORS[idx % TILE_COLORS.length];
}
%>
<%
/* ── Auth check ─────────────────────────────────────────── */
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || !tbmuser.getAktif()) {
    response.sendRedirect(request.getContextPath() + "/");
    return;
}

/* ── Ganti Bahasa (Indonesia/English/Arab) ─────────────────
   Bila ada parameter ?lang=id|en|ar → set bahasa aktif (dan simpan
   permanen ke kolom bahasa pengguna bila login), lalu redirect balik
   ke halaman ini tanpa parameter agar seluruh label ter-render ulang. */
String _pilihBahasa = request.getParameter("lang");
if (_pilihBahasa != null && _pilihBahasa.trim().length() > 0) {
    Common.initBahasaParameter(_pilihBahasa.trim());
    response.sendRedirect(request.getRequestURI());
    return;
}
String _lgAktif = Common.currentLang();
String _lgKode = Common.kodeBahasaAktif();
String _lgFlag = "en".equals(_lgKode) ? "united-kingdom"
        : ("ar".equals(_lgKode) ? "saudi-arabia" : ("zh".equals(_lgKode) ? "china" : "indonesia"));

/* ── Load data ──────────────────────────────────────────── */
Tbmrole role = tbmuser.hakAkses();
String ctx  = request.getContextPath();

/* PT info */
PerguruanTinggi pt = null;
String instName   = "eCampus";
String logoUrl    = "";
String themeCssFile = "";
try {
    pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
    if (pt != null) {
        if (pt.getNama() != null) instName = pt.getNama();
        if (pt.getCss()  != null && !pt.getCss().trim().isEmpty()) themeCssFile = pt.getCss().trim();
    }
    logoUrl = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/mobile/index.jsp:120"); /* tema default jika gagal */ }
String themeFile = themeCssFile.isEmpty() ? "" : themeCssFile.substring(themeCssFile.lastIndexOf('/') + 1);

/* User info */
String userName  = tbmuser.getUserNama() != null && !tbmuser.getUserNama().isEmpty() ? tbmuser.getUserNama() : tbmuser.getUserId();
String userId    = tbmuser.getUserId() != null ? tbmuser.getUserId() : "";
String userRole  = role != null && role.getRoleName() != null ? role.getRoleName() : "Pengguna";
String userEmail = tbmuser.getEmail() != null ? tbmuser.getEmail() : "";
String userHp    = tbmuser.getHp()    != null ? tbmuser.getHp()    : "";

/* Salam berdasarkan jam */
int hour  = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
String salam = hour < 11 ? "Selamat Pagi" : hour < 15 ? "Selamat Siang" : hour < 18 ? "Selamat Sore" : "Selamat Malam";

/* Deteksi jenis institusi */
boolean isPT = false;
boolean isSekolah = false;
try {
    boolean[] ptYa = Common.chekPtAtauSekolah(tbmuser);
    isPT = ptYa[0]; isSekolah = ptYa[1];
} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/mobile/index.jsp:140");}
String appTitle = isSekolah ? "eSchool" : "eCampus";

/* Menus */
List allMenus = (List) request.getSession().getAttribute("current_menus");
if (allMenus == null) {
    try { CommonMenu.loadTree(tbmuser, request); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/mobile/index.jsp:146");}
    allMenus = (List) request.getSession().getAttribute("current_menus");
}
if (allMenus == null) allMenus = new ArrayList();
List roots = rootMenus(allMenus);

/* Notifications */
int    notifCount = 0;
List   notifItems = new ArrayList();
try {
    notifItems = MobileNotifHelper.load(userId, 30);
    notifCount = notifItems.size();
} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/mobile/index.jsp:158");}

/* Role flags */
boolean hasElearning   = role != null && Boolean.TRUE.equals(role.getElearning());
boolean hasPrestasi    = role != null && Boolean.TRUE.equals(role.getKegiatanDanPrestasi());
boolean hasPustaka     = role != null && Boolean.TRUE.equals(role.getPustaka());
boolean hasPembayaran  = role != null && Boolean.TRUE.equals(role.getPembayaran());
boolean hasKeuangan    = role != null && Boolean.TRUE.equals(role.getKeuangan());
boolean hasAkuntansi   = role != null && Boolean.TRUE.equals(role.getAkunting());
boolean hasKepegawaian = role != null && Boolean.TRUE.equals(role.getKepegawaian());
boolean hasPresensi    = role != null && Boolean.TRUE.equals(role.getPresensiKehadiran());
boolean hasAkademik    = role != null && Boolean.TRUE.equals(role.getDashboard());
boolean hasSurat       = role != null && Boolean.TRUE.equals(role.getAdministrasi());
boolean hasKinerja     = role != null && Boolean.TRUE.equals(role.getKinerja());
boolean hasPengadaan   = role != null && Boolean.TRUE.equals(role.getPengadaan());
boolean hasKantin      = role != null && Boolean.TRUE.equals(role.getKantin());
boolean hasWorkflow    = role != null && Boolean.TRUE.equals(role.getWorkflow());
boolean hasKalender    = role != null && Boolean.TRUE.equals(role.getKalenderAkademik());
boolean hasRepository  = role != null && Boolean.TRUE.equals(role.getDasborRepository());
boolean hasInfoKegiatan= role != null && Boolean.TRUE.equals(role.getInfoKegiatan());
boolean hasSpmi        = role != null && Boolean.TRUE.equals(role.getTampilkanSpmi());
boolean hasKoperasi    = role != null && Boolean.TRUE.equals(role.getDashboardKoperasi());
boolean hasGaji        = role != null && Boolean.TRUE.equals(role.getTampilkanGaji());
boolean hasAntarJemput = role != null && Boolean.TRUE.equals(role.getDasboardAntarJemput());
boolean hasFeeder      = ais.common.Common.getApakahAdminBolehAksesFeeder();

/* Jumlah modul yang bisa diakses */
int modCount = 0;
if (hasElearning) modCount++;
if (hasPrestasi)  modCount++;
if (hasPustaka)   modCount++;
if (hasPembayaran) modCount++;
if (hasKeuangan)  modCount++;
if (hasKepegawaian) modCount++;
if (hasPresensi)  modCount++;
if (hasAkademik)  modCount++;
modCount += roots.size();
if (hasSpmi) modCount++;
if (hasKoperasi) modCount++;
if (hasGaji) modCount++;
if (hasAntarJemput) modCount++;
if (hasFeeder) modCount++;
%>
<!DOCTYPE html>
<html lang="<%=ais.common.Common.kodeBahasaAktif()%>" dir="<%= "ar".equals(ais.common.Common.kodeBahasaAktif()) ? "rtl" : "ltr" %>">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover">
<meta name="mobile-web-app-capable" content="yes">
<meta name="apple-mobile-web-app-capable" content="yes">
<meta name="apple-mobile-web-app-status-bar-style" content="default">
<meta name="theme-color" content="#16794a">
<title><%=esc(appTitle)%> Mobile</title>
<link rel="shortcut icon" href="<%=esc(logoUrl)%>" type="image/png">

<%-- Bootstrap 5 --%>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
<%-- Font Awesome 6 --%>
<link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css" rel="stylesheet">
<%-- Google Fonts --%>
<link href="https://fonts.googleapis.com/css2?family=Open+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">

<%-- Theme CSS (sama dengan versi desktop) --%>
<% if (!themeFile.isEmpty()) { %>
<link href="<%=ctx%>/css/baru/base-theme.css?v=<%=System.currentTimeMillis()%>" rel="stylesheet">
<link href="<%=ctx%>/css/baru/<%=esc(themeFile)%>?v=<%=System.currentTimeMillis()%>" rel="stylesheet">
<% } %>
<%-- Mobile App CSS --%>
<link href="<%=ctx%>/css/baru/mobile-app.css?v=20260701a" rel="stylesheet">
</head>
<body>

<%-- ═══════════════ TOP BAR ═══════════════ --%>
<header class="mob-topbar">
    <% if (logoUrl != null && !logoUrl.isEmpty()) { %>
    <img class="mob-topbar-logo" src="<%=esc(logoUrl)%>" alt="logo">
    <% } else { %>
    <div class="mob-topbar-logo-placeholder"><%=esc(appTitle.substring(0,1))%></div>
    <% } %>
    <div class="mob-topbar-title">
        <%=esc(appTitle)%>
        <span class="mob-topbar-subtitle"><%=esc(instName)%></span>
    </div>
    <div class="mob-topbar-lang ais-lang-dd" title="<%=esc(Common.getBahasaConfig("Pilih Bahasa"))%>" style="position:relative;margin:0 6px;">
        <a href="javascript:void(0)" onclick="aisLangDD(this)" style="display:inline-flex;align-items:center;gap:3px;cursor:pointer;text-decoration:none;">
            <img src="<%=ctx%>/component/assets/media/flags/<%=_lgFlag%>.svg" style="width:18px;height:12px;border-radius:3px;box-shadow:0 0 0 1px rgba(0,0,0,.15);display:block;">
            <span style="font-size:10px;color:#cbd5e1;">&#9662;</span>
        </a>
        <div class="ais-lang-menu" style="display:none;position:absolute;right:0;top:100%;margin-top:6px;background:#fff;border:1px solid #e2e8f0;border-radius:8px;box-shadow:0 6px 18px rgba(0,0,0,.25);padding:6px;z-index:30000;">
            <a href="?lang=id" title="Indonesia" style="display:block;padding:5px;"><img src="<%=ctx%>/component/assets/media/flags/indonesia.svg" style="width:30px;height:20px;border-radius:3px;display:block;"></a>
            <a href="?lang=en" title="English" style="display:block;padding:5px;"><img src="<%=ctx%>/component/assets/media/flags/united-kingdom.svg" style="width:30px;height:20px;border-radius:3px;display:block;"></a>
            <a href="?lang=ar" title="العربية" style="display:block;padding:5px;"><img src="<%=ctx%>/component/assets/media/flags/saudi-arabia.svg" style="width:30px;height:20px;border-radius:3px;display:block;"></a>
            <a href="?lang=zh" title="中文" style="display:block;padding:5px;"><img src="<%=ctx%>/component/assets/media/flags/china.svg" style="width:30px;height:20px;border-radius:3px;display:block;"></a>
        </div>
    </div>
    <script>
    function aisLangDD(el){var m=el.parentNode.querySelector('.ais-lang-menu');if(!m)return;m.style.display=(m.style.display==='block')?'none':'block';}
    if(!window.__aisLangDDInit){window.__aisLangDDInit=true;document.addEventListener('click',function(e){var ms=document.getElementsByClassName('ais-lang-menu');for(var i=0;i<ms.length;i++){if(!ms[i].parentNode.contains(e.target))ms[i].style.display='none';}});}
    </script>
    <button class="mob-topbar-btn" onclick="switchPanel('notif')" title="Notifikasi">
        <i class="fas fa-bell"></i>
        <% if (notifCount > 0) { %><span class="mob-badge"><%=notifCount > 99 ? "99+" : notifCount%></span><% } %>
    </button>
    <button class="mob-topbar-btn" onclick="switchPanel('profil')" title="Profil">
        <span style="font-size:14px;font-weight:700;"><%=esc(initials(userName))%></span>
    </button>
</header>

<%-- ═══════════════ PANEL CONTAINER ═══════════════ --%>
<div class="mob-content">

<%-- ─── PANEL: BERANDA ─────────────────────────────── --%>
<div id="panel-home" class="mob-panel active">

    <%-- Hero / Greeting --%>
    <div class="mob-hero">
        <div class="mob-hero-ava"><%=esc(initials(userName))%></div>
        <div class="mob-hero-info">
            <div class="mob-hero-salam"><%=esc(salam)%>,</div>
            <div class="mob-hero-name"><%=esc(userName)%></div>
            <div class="mob-hero-role"><%=esc(userRole)%></div>
        </div>
    </div>

    <%-- Stats mini --%>
    <div class="mob-stats-row">
        <div class="mob-stat-card">
            <div class="mob-stat-val" style="color:var(--theme-primary,#16794a)"><%=notifCount%></div>
            <div class="mob-stat-lbl">Notifikasi</div>
        </div>
        <div class="mob-stat-card">
            <div class="mob-stat-val" style="color:#3b82f6"><%=modCount%></div>
            <div class="mob-stat-lbl">Modul Aktif</div>
        </div>
        <div class="mob-stat-card">
            <div class="mob-stat-val" style="color:#f59e0b"><%=allMenus.size()%></div>
            <div class="mob-stat-lbl">Total Menu</div>
        </div>
    </div>

    <%-- Quick Action Tiles --%>
    <div class="mob-sec-title">Akses Cepat</div>
    <div class="mob-tiles">
        <% if (hasPresensi) { %>
        <div class="mob-tile" onclick="openOverlay('<%=ctx%>/baru?p=presensi','Presensi & Kehadiran')">
            <div class="mob-tile-ic"><i class="fas fa-clock"></i></div>
            <div class="mob-tile-lbl">Presensi</div>
        </div>
        <% } %>
        <% if (hasElearning) { %>
        <div class="mob-tile" onclick="switchPanel('menu');openDrilldown('elearning','e-Learning','0')">
            <div class="mob-tile-ic"><i class="fas fa-graduation-cap"></i></div>
            <div class="mob-tile-lbl">e-Learning</div>
        </div>
        <% } %>
        <% if (hasAkademik) { %>
        <div class="mob-tile" onclick="openOverlay('<%=ctx%>/baru?p=akademik','Akademik')">
            <div class="mob-tile-ic"><i class="fas fa-university"></i></div>
            <div class="mob-tile-lbl">Akademik</div>
        </div>
        <% } %>
        <% if (hasPembayaran) { %>
        <div class="mob-tile" onclick="openOverlay('<%=ctx%>/baru?p=pembayaran','Pembayaran')">
            <div class="mob-tile-ic"><i class="fas fa-credit-card"></i></div>
            <div class="mob-tile-lbl">Pembayaran</div>
        </div>
        <% } %>
        <% if (hasSurat) { %>
        <div class="mob-tile" onclick="openOverlay('<%=ctx%>/baru?p=suratmenyurat','Surat Menyurat')">
            <div class="mob-tile-ic"><i class="fas fa-envelope"></i></div>
            <div class="mob-tile-lbl">Surat</div>
        </div>
        <% } %>
        <% if (hasKalender) { %>
        <div class="mob-tile" onclick="openOverlay('<%=ctx%>/baru?p=kalenderakademik','Kalender Akademik')">
            <div class="mob-tile-ic"><i class="fas fa-calendar-alt"></i></div>
            <div class="mob-tile-lbl">Kalender</div>
        </div>
        <% } %>
        <% if (hasPustaka) { %>
        <div class="mob-tile" onclick="switchPanel('menu');openDrilldown('pustaka','Pustaka','0')">
            <div class="mob-tile-ic"><i class="fas fa-book"></i></div>
            <div class="mob-tile-lbl">Pustaka</div>
        </div>
        <% } %>
        <% if (hasWorkflow) { %>
        <div class="mob-tile" onclick="openOverlay('<%=ctx%>/baru?p=pagesmastersoppengajuananda','Pengajuan')">
            <div class="mob-tile-ic"><i class="fas fa-file-signature"></i></div>
            <div class="mob-tile-lbl">Pengajuan</div>
        </div>
        <% } %>
        <% if (hasKepegawaian) { %>
        <div class="mob-tile" onclick="openOverlay('<%=ctx%>/baru?p=kepegawaian','Kepegawaian')">
            <div class="mob-tile-ic"><i class="fas fa-user-tie"></i></div>
            <div class="mob-tile-lbl">Kepegawaian</div>
        </div>
        <% } %>
        <% if (hasKeuangan) { %>
        <div class="mob-tile" onclick="openOverlay('<%=ctx%>/baru?p=keuangan','Keuangan')">
            <div class="mob-tile-ic"><i class="fas fa-coins"></i></div>
            <div class="mob-tile-lbl">Keuangan</div>
        </div>
        <% } %>
        <%-- Tombol "Semua Menu" selalu muncul di akhir --%>
        <div class="mob-tile more" onclick="switchPanel('menu')">
            <div class="mob-tile-ic"><i class="fas fa-th-large"></i></div>
            <div class="mob-tile-lbl">Semua Menu</div>
        </div>
    </div>

    <%-- Notifikasi terbaru (preview 3 item) --%>
    <% if (notifCount > 0) { %>
    <div class="mob-sec-title">Notifikasi Terbaru</div>
    <div class="mob-announce-card">
        <div class="mob-announce-header">
            <div class="mob-announce-title"><i class="fas fa-bell"></i> Notifikasi</div>
            <a href="#" class="mob-announce-more" onclick="switchPanel('notif');return false;">Lihat semua</a>
        </div>
        <%
        int previewMax = Math.min(notifItems.size(), 3);
        for (int i = 0; i < previewMax; i++) {
            NotifItem ni = (NotifItem) notifItems.get(i);
            String openUrl = ni.bukaZk != null && !ni.bukaZk.trim().isEmpty()
                    && !ni.bukaZk.trim().toLowerCase().endsWith(".zul") ? ni.bukaZk.trim() : "#";
            boolean hasLink = !"#".equals(openUrl);
        %>
        <div class="mob-announce-item" onclick="<%= hasLink ? "openOverlay('" + js(openUrl) + "','" + js(ni.subject) + "')" : "switchPanel('notif')" %>">
            <div class="mob-announce-dot"></div>
            <div class="mob-announce-text"><%=esc(ni.displayTitle())%></div>
        </div>
        <% } %>
    </div>
    <% } %>

    <%-- Dashboard navigasi cepat --%>
    <div class="mob-sec-title">Dashboard</div>
    <div class="mob-announce-card">
        <div class="mob-announce-item" onclick="openOverlay('<%=ctx%>/baru?p=home','Beranda')">
            <i class="fas fa-chart-pie" style="color:var(--theme-primary,#16794a);width:18px;text-align:center;font-size:13px;"></i>
            <div class="mob-announce-text" style="padding-left:4px">Buka Dashboard Lengkap</div>
            <i class="fas fa-chevron-right" style="color:#c0c8d8;font-size:11px;"></i>
        </div>
        <div class="mob-announce-item" onclick="openDesktop()">
            <i class="fas fa-desktop" style="color:#3b82f6;width:18px;text-align:center;font-size:13px;"></i>
            <div class="mob-announce-text" style="padding-left:4px">Buka Tampilan Desktop</div>
            <i class="fas fa-external-link-alt" style="color:#c0c8d8;font-size:11px;"></i>
        </div>
    </div>
    <div class="mob-spacer"></div>
</div><!-- /panel-home -->

<%-- ─── PANEL: MENU ─────────────────────────────────── --%>
<div id="panel-menu" class="mob-panel" style="position:relative;overflow:hidden;">
    <%-- Container utama: search + kategori --%>
    <div id="menu-main" style="height:100%;overflow-y:auto;-webkit-overflow-scrolling:touch;">
        <div class="mob-search-wrap">
            <div class="mob-search">
                <i class="fas fa-search"></i>
                <input type="search" id="menu-search" placeholder="Cari menu..." autocomplete="off" oninput="handleSearch(this.value)">
                <button class="mob-search-clear" id="search-clear-btn" onclick="clearSearch()"><i class="fas fa-times"></i></button>
            </div>
        </div>

        <%-- Hasil pencarian (ditampilkan via JS saat ada input) --%>
        <div id="mob-search-results" style="display:none;"></div>

        <%-- Grid kategori Dashboard Utama --%>
        <div id="menu-cats-container">
        <div class="mob-sec-title">Dashboard Utama</div>
        <div class="mob-cat-grid">
            <% int catIdx = 0; %>
            <% if (hasElearning) { %>
            <div class="mob-cat-card" onclick="openDrilldown('elearning','e-Learning','<%=catIdx%>')">
                <div class="mob-cat-ic c<%=(catIdx%10)%>"><i class="fas fa-graduation-cap"></i></div>
                <div class="mob-cat-info"><div class="mob-cat-lbl">e-Learning</div></div>
            </div>
            <% catIdx++; } %>
            <% if (hasPrestasi) { %>
            <div class="mob-cat-card" onclick="openDrilldown('prestasi','Prestasi','<%=catIdx%>')">
                <div class="mob-cat-ic c<%=(catIdx%10)%>"><i class="fas fa-trophy"></i></div>
                <div class="mob-cat-info"><div class="mob-cat-lbl">Prestasi</div></div>
            </div>
            <% catIdx++; } %>
            <% if (hasPustaka) { %>
            <div class="mob-cat-card" onclick="openDrilldown('pustaka','Pustaka','<%=catIdx%>')">
                <div class="mob-cat-ic c<%=(catIdx%10)%>"><i class="fas fa-book"></i></div>
                <div class="mob-cat-info"><div class="mob-cat-lbl">Pustaka</div></div>
            </div>
            <% catIdx++; } %>
            <% if (hasWorkflow) { %>
            <div class="mob-cat-card" onclick="openOverlay('<%=ctx%>/baru?p=pagesmastersoppengajuananda','Pengajuan Anda')">
                <div class="mob-cat-ic c<%=(catIdx%10)%>"><i class="fas fa-file-signature"></i></div>
                <div class="mob-cat-info"><div class="mob-cat-lbl">Pengajuan</div></div>
            </div>
            <% catIdx++; } %>
            <% if (hasAkademik) { %>
            <div class="mob-cat-card" onclick="openOverlay('<%=ctx%>/baru?p=akademik','Akademik')">
                <div class="mob-cat-ic c<%=(catIdx%10)%>"><i class="fas fa-university"></i></div>
                <div class="mob-cat-info"><div class="mob-cat-lbl">Akademik</div></div>
            </div>
            <% catIdx++; } %>
            <% if (hasSurat) { %>
            <div class="mob-cat-card" onclick="openOverlay('<%=ctx%>/baru?p=suratmenyurat','Surat & Dokumen')">
                <div class="mob-cat-ic c<%=(catIdx%10)%>"><i class="fas fa-envelope-open-text"></i></div>
                <div class="mob-cat-info"><div class="mob-cat-lbl">Surat &amp; Dok</div></div>
            </div>
            <% catIdx++; } %>
            <% if (hasPembayaran) { %>
            <div class="mob-cat-card" onclick="openOverlay('<%=ctx%>/baru?p=pembayaran','Pembayaran')">
                <div class="mob-cat-ic c<%=(catIdx%10)%>"><i class="fas fa-credit-card"></i></div>
                <div class="mob-cat-info"><div class="mob-cat-lbl">Pembayaran</div></div>
            </div>
            <% catIdx++; } %>
            <% if (hasKeuangan) { %>
            <div class="mob-cat-card" onclick="openOverlay('<%=ctx%>/baru?p=keuangan','Keuangan')">
                <div class="mob-cat-ic c<%=(catIdx%10)%>"><i class="fas fa-coins"></i></div>
                <div class="mob-cat-info"><div class="mob-cat-lbl">Keuangan</div></div>
            </div>
            <% catIdx++; } %>
            <% if (hasAkuntansi) { %>
            <div class="mob-cat-card" onclick="openOverlay('<%=ctx%>/baru?p=akuntansi','Akuntansi')">
                <div class="mob-cat-ic c<%=(catIdx%10)%>"><i class="fas fa-calculator"></i></div>
                <div class="mob-cat-info"><div class="mob-cat-lbl">Akuntansi</div></div>
            </div>
            <% catIdx++; } %>
            <% if (hasKepegawaian) { %>
            <div class="mob-cat-card" onclick="openOverlay('<%=ctx%>/baru?p=kepegawaian','Kepegawaian')">
                <div class="mob-cat-ic c<%=(catIdx%10)%>"><i class="fas fa-user-tie"></i></div>
                <div class="mob-cat-info"><div class="mob-cat-lbl">Kepegawaian</div></div>
            </div>
            <% catIdx++; } %>
            <% if (hasKinerja) { %>
            <div class="mob-cat-card" onclick="openOverlay('<%=ctx%>/baru?p=kinerja','Kinerja')">
                <div class="mob-cat-ic c<%=(catIdx%10)%>"><i class="fas fa-chart-line"></i></div>
                <div class="mob-cat-info"><div class="mob-cat-lbl">Kinerja</div></div>
            </div>
            <% catIdx++; } %>
            <% if (hasPresensi) { %>
            <div class="mob-cat-card" onclick="openOverlay('<%=ctx%>/baru?p=presensi','Presensi')">
                <div class="mob-cat-ic c<%=(catIdx%10)%>"><i class="fas fa-clock"></i></div>
                <div class="mob-cat-info"><div class="mob-cat-lbl">Presensi</div></div>
            </div>
            <% catIdx++; } %>
            <% if (hasKalender) { %>
            <div class="mob-cat-card" onclick="openOverlay('<%=ctx%>/baru?p=kalenderakademik','Kalender Akademik')">
                <div class="mob-cat-ic c<%=(catIdx%10)%>"><i class="fas fa-calendar-alt"></i></div>
                <div class="mob-cat-info"><div class="mob-cat-lbl">Kalender</div></div>
            </div>
            <% catIdx++; } %>
            <% if (hasPengadaan) { %>
            <div class="mob-cat-card" onclick="openOverlay('<%=ctx%>/baru?p=pengadaan','Pengadaan')">
                <div class="mob-cat-ic c<%=(catIdx%10)%>"><i class="fas fa-shopping-cart"></i></div>
                <div class="mob-cat-info"><div class="mob-cat-lbl">Pengadaan</div></div>
            </div>
            <% catIdx++; } %>
            <% if (hasKantin) { %>
            <div class="mob-cat-card" onclick="openDrilldown('kantin','e-Kantin','<%=catIdx%>')">
                <div class="mob-cat-ic c<%=(catIdx%10)%>"><i class="fas fa-utensils"></i></div>
                <div class="mob-cat-info"><div class="mob-cat-lbl">e-Kantin</div></div>
            </div>
            <% catIdx++; } %>
            <% if (hasSpmi) { %>
            <div class="mob-cat-card" onclick="openDrilldown('spmi','SPMI','<%=catIdx%>')">
                <div class="mob-cat-ic c<%=(catIdx%10)%>"><i class="fas fa-clipboard-check"></i></div>
                <div class="mob-cat-info"><div class="mob-cat-lbl">SPMI</div></div>
            </div>
            <% catIdx++; } %>
            <% if (hasKoperasi) { %>
            <div class="mob-cat-card" onclick="openDrilldown('koperasi','Koperasi','<%=catIdx%>')">
                <div class="mob-cat-ic c<%=(catIdx%10)%>"><i class="fas fa-handshake"></i></div>
                <div class="mob-cat-info"><div class="mob-cat-lbl">Koperasi</div></div>
            </div>
            <% catIdx++; } %>
            <% if (hasGaji) { %>
            <div class="mob-cat-card" onclick="openDrilldown('gaji','Gaji','<%=catIdx%>')">
                <div class="mob-cat-ic c<%=(catIdx%10)%>"><i class="fas fa-money-bill-wave"></i></div>
                <div class="mob-cat-info"><div class="mob-cat-lbl">Gaji</div></div>
            </div>
            <% catIdx++; } %>
            <% if (hasAntarJemput) { %>
            <div class="mob-cat-card" onclick="openOverlay('<%=ctx%>/baru?p=antarjemput','Antar Jemput')">
                <div class="mob-cat-ic c<%=(catIdx%10)%>"><i class="fas fa-bus"></i></div>
                <div class="mob-cat-info"><div class="mob-cat-lbl">Antar Jemput</div></div>
            </div>
            <% catIdx++; } %>
            <% if (hasRepository) { %>
            <div class="mob-cat-card" onclick="openOverlay('<%=ctx%>/baru?p=repository','Repository')">
                <div class="mob-cat-ic c<%=(catIdx%10)%>"><i class="fas fa-folder-open"></i></div>
                <div class="mob-cat-info"><div class="mob-cat-lbl">Repository</div></div>
            </div>
            <% catIdx++; } %>
            <% if (hasInfoKegiatan) { %>
            <div class="mob-cat-card" onclick="openOverlay('<%=ctx%>/baru?p=infokegiatan','Info Kegiatan')">
                <div class="mob-cat-ic c<%=(catIdx%10)%>"><i class="fas fa-bullhorn"></i></div>
                <div class="mob-cat-info"><div class="mob-cat-lbl">Info Kegiatan</div></div>
            </div>
            <% catIdx++; } %>
            <% if (hasFeeder) { %>
            <div class="mob-cat-card" onclick="openOverlay('<%=ctx%>/baru?p=feeder','Neo Feeder')">
                <div class="mob-cat-ic c<%=(catIdx%10)%>"><i class="fas fa-cloud-arrow-up"></i></div>
                <div class="mob-cat-info"><div class="mob-cat-lbl">Neo Feeder</div></div>
            </div>
            <% catIdx++; } %>
        </div>

        <%-- Modul Aplikasi (dari CommonMenu.loadTree) --%>
        <% if (!roots.isEmpty()) { %>
        <div class="mob-sec-title">Modul Aplikasi</div>
        <div class="mob-cat-grid">
            <% for (int ri = 0; ri < roots.size(); ri++) {
               Menu rootM = (Menu) roots.get(ri);
               if (rootM.getLabel() == null) continue;
               boolean rootHasChildren = hasChildren(rootM, allMenus);
               int colorIdx = (catIdx + ri) % 10;
            %>
            <div class="mob-cat-card"
                 onclick="<% if (rootHasChildren) { %>openMenuDrilldown(<%=rootM.getId()%>,'<%=js(rootM.getLabel())%>','<%=colorIdx%>')<%} else { %>openMenuLeaf('<%=js(ctx + "/baru?p=" + URLEncoder.encode(urlSafe(rootM.getUrl()),  "UTF-8") + "&menu=" + rootM.getId())%>','<%=js(rootM.getLabel())%>')<%}%>">
                <div class="mob-cat-ic c<%=colorIdx%>"><i class="fas fa-folder"></i></div>
                <div class="mob-cat-info">
                    <div class="mob-cat-lbl"><%=esc(rootM.getLabel())%></div>
                    <% List rootChildren = childMenus(rootM.getChild(), allMenus);
                       if (!rootChildren.isEmpty()) { %>
                    <div class="mob-cat-count"><%=rootChildren.size()%> menu</div>
                    <% } %>
                </div>
            </div>
            <% } %>
        </div>
        <% } %>
        </div><%-- /menu-cats-container --%>
    </div><%-- /menu-main --%>

    <%-- Drill-down overlay (dalam panel menu) --%>
    <div id="mob-drilldown" class="mob-drilldown">
        <div class="mob-drilldown-bar">
            <button class="mob-drilldown-back" onclick="closeDrilldown()"><i class="fas fa-arrow-left"></i></button>
            <span id="mob-drilldown-title" class="mob-drilldown-title">Menu</span>
        </div>
        <div class="mob-menu-list" id="mob-drilldown-list"></div>
    </div>

</div><!-- /panel-menu -->

<%-- ─── PANEL: NOTIFIKASI ───────────────────────────── --%>
<div id="panel-notif" class="mob-panel">
    <div class="mob-panel-header">
        <div class="mob-panel-header-title">Notifikasi</div>
        <% if (notifCount > 0) { %>
        <span class="mob-badge-pill"><%=notifCount%> pesan</span>
        <% } %>
    </div>

    <% if (notifItems.isEmpty()) { %>
    <div class="mob-empty">
        <i class="fas fa-bell-slash"></i>
        <div class="mob-empty-title">Tidak ada notifikasi</div>
        <div class="mob-empty-desc">Anda tidak memiliki notifikasi baru saat ini.<br>Notifikasi baru akan muncul di sini.</div>
    </div>
    <% } else { %>
    <div class="mob-notif-list">
        <% for (int ni = 0; ni < notifItems.size(); ni++) {
           NotifItem nitem = (NotifItem) notifItems.get(ni);
           String nUrl = (nitem.bukaZk != null && !nitem.bukaZk.trim().isEmpty()
                   && !nitem.bukaZk.trim().toLowerCase().endsWith(".zul")) ? nitem.bukaZk.trim() : "";
           boolean nHasLink = !nUrl.isEmpty();
        %>
        <div class="mob-notif-item" onclick="<% if (nHasLink) { %>openOverlay('<%=js(nUrl)%>','<%=js(nitem.subject)%>')<%} else {%>showNotifDetail('<%=js(nitem.displayTitle())%>','<%=js(nitem.objKet)%>')<%}%>">
            <div class="mob-notif-ic">
                <i class="fas fa-<% if (nitem.className.contains("Sop")) { %>file-alt<% } else if (nitem.className.contains("Surat")) { %>envelope<% } else if (nitem.className.contains("Pengumuman")) { %>bullhorn<% } else if (nitem.className.contains("Kegiatan")) { %>money-bill-wave<% } else if (nitem.className.contains("Pertemuan")) { %>chalkboard-teacher<% } else { %>info-circle<% } %>"></i>
            </div>
            <div class="mob-notif-body">
                <div class="mob-notif-subj"><%=esc(nitem.displayTitle())%></div>
                <% if (nitem.objKet != null && !nitem.objKet.trim().isEmpty()) { %>
                <div class="mob-notif-desc"><%=esc(nitem.objKet)%></div>
                <% } %>
            </div>
            <% if (nHasLink) { %><i class="fas fa-chevron-right mob-notif-go"></i><% } %>
        </div>
        <% } %>
    </div>
    <% } %>
    <div class="mob-spacer"></div>
</div><!-- /panel-notif -->

<%-- ─── PANEL: PROFIL ───────────────────────────────── --%>
<div id="panel-profil" class="mob-panel">
    <div class="mob-profile-head">
        <div class="mob-profile-ava"><%=esc(initials(userName))%></div>
        <div class="mob-profile-name"><%=esc(userName)%></div>
        <div class="mob-profile-role"><%=esc(userRole)%></div>
        <div class="mob-profile-inst"><%=esc(instName)%></div>
    </div>

    <div class="mob-profile-list">
        <% if (!userEmail.isEmpty() || !userHp.isEmpty()) { %>
        <div class="mob-profile-section">Kontak</div>
        <% if (!userEmail.isEmpty()) { %>
        <div class="mob-profile-row">
            <div class="mob-profile-row-ic"><i class="fas fa-envelope"></i></div>
            <div class="mob-profile-row-lbl"><%=esc(userEmail)%></div>
        </div>
        <% } %>
        <% if (!userHp.isEmpty()) { %>
        <div class="mob-profile-row">
            <div class="mob-profile-row-ic"><i class="fas fa-phone"></i></div>
            <div class="mob-profile-row-lbl"><%=esc(userHp)%></div>
        </div>
        <% } %>
        <% } %>

        <div class="mob-profile-section">Akun</div>
        <div class="mob-profile-row" onclick="openOverlay('<%=ctx%>/baru?p=home','Profil Saya')">
            <div class="mob-profile-row-ic"><i class="fas fa-user-circle"></i></div>
            <div class="mob-profile-row-lbl">Lihat Profil</div>
            <i class="fas fa-chevron-right mob-profile-row-go"></i>
        </div>
        <div class="mob-profile-row" onclick="openOverlay('<%=ctx%>/baru?p=home','Pengaturan Akun')">
            <div class="mob-profile-row-ic"><i class="fas fa-cog"></i></div>
            <div class="mob-profile-row-lbl">Pengaturan Akun</div>
            <i class="fas fa-chevron-right mob-profile-row-go"></i>
        </div>

        <div class="mob-profile-section">Tampilan</div>
        <div class="mob-profile-row">
            <div class="mob-profile-row-ic" style="background:var(--theme-light,rgba(22,121,74,.1));color:var(--theme-primary,#16794a);">
                <i class="fas fa-palette"></i>
            </div>
            <div class="mob-profile-row-lbl">Tema: <%=esc(themeFile.isEmpty() ? "Default" : themeFile.replace(".css","").replace("-"," ").replace("_"," "))%></div>
        </div>

        <div class="mob-profile-section">Lainnya</div>
        <div class="mob-profile-row" onclick="openDesktop()">
            <div class="mob-profile-row-ic"><i class="fas fa-desktop"></i></div>
            <div class="mob-profile-row-lbl">Buka Versi Desktop</div>
            <i class="fas fa-external-link-alt mob-profile-row-go"></i>
        </div>
        <div class="mob-profile-row" onclick="openOverlay('<%=ctx%>/baru?p=home','Bantuan &amp; FAQ')">
            <div class="mob-profile-row-ic"><i class="fas fa-question-circle"></i></div>
            <div class="mob-profile-row-lbl">Bantuan &amp; FAQ</div>
            <i class="fas fa-chevron-right mob-profile-row-go"></i>
        </div>

        <div class="mob-spacer"></div>
        <div class="mob-profile-row is-danger" onclick="doLogout()">
            <div class="mob-profile-row-ic"><i class="fas fa-sign-out-alt"></i></div>
            <div class="mob-profile-row-lbl">Keluar</div>
        </div>
        <div class="mob-spacer"></div>
        <div style="text-align:center;font-size:10px;color:#b0b8c8;padding:8px 0 16px;">
            <%=esc(appTitle)%> Mobile &bull; <%=esc(instName)%>
        </div>
    </div>
</div><!-- /panel-profil -->

</div><!-- /mob-content -->

<%-- ═══════════════ OVERLAY FULL-SCREEN ═══════════════ --%>
<div id="mob-overlay" class="mob-overlay">
    <div class="mob-overlay-bar">
        <button class="mob-overlay-back" onclick="closeOverlay()"><i class="fas fa-arrow-left"></i> Kembali</button>
        <span id="mob-overlay-title" class="mob-overlay-title"></span>
        <a id="mob-overlay-newwin" class="mob-overlay-newwin" href="#" target="_blank" title="Buka di tab baru">
            <i class="fas fa-external-link-alt"></i>
        </a>
    </div>
    <iframe id="mob-iframe" class="mob-iframe" src="about:blank" allowfullscreen></iframe>
</div>

<%-- Modal notifikasi detail (untuk notif tanpa link) --%>
<div id="mob-notif-modal" style="display:none;position:fixed;inset:0;z-index:9500;background:rgba(0,0,0,.5);display:none;align-items:flex-end;">
    <div style="background:#fff;border-radius:20px 20px 0 0;padding:24px 20px;width:100%;max-height:70vh;overflow-y:auto;">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:14px;">
            <div style="font-weight:700;font-size:15px;color:#1a1e2e;">Detail Notifikasi</div>
            <button onclick="closeNotifModal()" style="background:none;border:none;font-size:20px;color:#9ca3b0;cursor:pointer;"><i class="fas fa-times"></i></button>
        </div>
        <div id="mob-notif-modal-title" style="font-size:14px;font-weight:600;color:#1a1e2e;margin-bottom:8px;"></div>
        <div id="mob-notif-modal-desc" style="font-size:13px;color:#5a6272;line-height:1.6;"></div>
    </div>
</div>

<%-- ═══════════════ BOTTOM NAV ═══════════════ --%>
<nav class="mob-bottomnav">
    <button class="mob-nav-btn active" id="nav-home" onclick="switchPanel('home')">
        <i class="fas fa-home"></i><span>Beranda</span>
    </button>
    <button class="mob-nav-btn" id="nav-menu" onclick="switchPanel('menu')">
        <i class="fas fa-th-large"></i><span>Menu</span>
    </button>
    <button class="mob-nav-btn" id="nav-notif" onclick="switchPanel('notif')" style="position:relative;">
        <i class="fas fa-bell"></i><span>Notifikasi</span>
        <% if (notifCount > 0) { %>
        <span class="mob-badge" style="top:4px;right:18px;"><%=notifCount > 9 ? "9+" : notifCount%></span>
        <% } %>
    </button>
    <button class="mob-nav-btn" id="nav-profil" onclick="switchPanel('profil')">
        <i class="fas fa-user-circle"></i><span>Profil</span>
    </button>
</nav>

<%-- ═══════════════ DATA MENU (untuk JS search) ═══════════════ --%>
<%
/* ── Build JSON sepenuhnya di Java agar tidak ada bug koma ── */
StringBuilder mobJson = new StringBuilder();
mobJson.append("{\"dashCats\":[");
List dashCatJsonList = new ArrayList();
if (hasElearning) {
    String[] subEL = isPT
        ? new String[]{"dasbor","linimasa","ringkasan","ujian","tugas","tugas_kelompok","materi","audio","video","kalender","kurikulum_obe","portofolio_mahasiswa","pikobe"}
        : new String[]{"linimasa","ringkasan","ujian","tugas","tugas_kelompok","materi","audio","video","kalender"};
    StringBuilder catSubs = new StringBuilder("[");
    for (int si = 0; si < subEL.length; si++) {
        if (si > 0) catSubs.append(",");
        String sl;
        if (subEL[si].equals("tugas_kelompok")) sl = "Tugas Kelompok";
        else if (subEL[si].equals("kurikulum_obe")) sl = "Kurikulum OBE";
        else if (subEL[si].equals("portofolio_mahasiswa")) sl = "Portofolio Mahasiswa";
        else sl = subEL[si].substring(0,1).toUpperCase() + subEL[si].substring(1);
        catSubs.append("{\"label\":").append(jsonStr(sl))
               .append(",\"url\":").append(jsonStr(ctx+"/baru?p=elearning&s="+subEL[si])).append("}");
    }
    catSubs.append("]");
    dashCatJsonList.add("{\"label\":\"e-Learning\",\"icon\":\"graduation-cap\",\"color\":\"0\",\"subs\":"+catSubs+"}");
}
if (hasPrestasi) {
    dashCatJsonList.add("{\"label\":\"Prestasi\",\"icon\":\"trophy\",\"color\":\"1\",\"subs\":["
        +"{\"label\":\"Prestasi\",\"url\":"+jsonStr(ctx+"/baru?p=prestasi&s=prestasi")+"},"
        +"{\"label\":\"Kegiatan\",\"url\":"+jsonStr(ctx+"/baru?p=prestasi&s=kegiatan")+"},"
        +"{\"label\":\"Karya\",\"url\":"+jsonStr(ctx+"/baru?p=prestasi&s=karya")+"},"
        +"{\"label\":\"Organisasi\",\"url\":"+jsonStr(ctx+"/baru?p=prestasi&s=organisasi")+"},"
        +"{\"label\":\"Catatan\",\"url\":"+jsonStr(ctx+"/baru?p=prestasi&s=catatan")+"}"
        +"]}");
}
if (hasPustaka) {
    dashCatJsonList.add("{\"label\":\"Pustaka\",\"icon\":\"book\",\"color\":\"2\",\"subs\":["
        +"{\"label\":\"Katalog\",\"url\":"+jsonStr(ctx+"/baru?p=pustaka&s=katalog")+"},"
        +"{\"label\":\"Populer\",\"url\":"+jsonStr(ctx+"/baru?p=pustaka&s=populer")+"},"
        +"{\"label\":\"Sirkulasi\",\"url\":"+jsonStr(ctx+"/baru?p=pustaka&s=sirkulasi")+"},"
        +"{\"label\":\"Kunjungan\",\"url\":"+jsonStr(ctx+"/baru?p=pustaka&s=kunjungan")+"},"
        +"{\"label\":\"Dashboard\",\"url\":"+jsonStr(ctx+"/baru?p=pustaka&s=dashboard")+"},"
        +"{\"label\":\"Beranda Anggota\",\"url\":"+jsonStr(ctx+"/baru?p=pustaka&s=beranda_anggota")+"}"
        +"]}");
}
if (hasKantin) {
    dashCatJsonList.add("{\"label\":\"e-Kantin\",\"icon\":\"utensils\",\"color\":\"3\",\"subs\":["
        +"{\"label\":\"Beranda\",\"url\":"+jsonStr(ctx+"/baru?p=kantin&s=beranda")+"},"
        +"{\"label\":\"Ringkasan\",\"url\":"+jsonStr(ctx+"/baru?p=kantin&s=ringkasan")+"},"
        +"{\"label\":\"POS Kasir\",\"url\":"+jsonStr(ctx+"/baru?p=kantin&s=pos")+"},"
        +"{\"label\":\"Pesanan\",\"url\":"+jsonStr(ctx+"/baru?p=kantin&s=pesanan")+"},"
        +"{\"label\":\"Laporan\",\"url\":"+jsonStr(ctx+"/baru?p=kantin&s=laporan_laporan")+"}"
        +"]}");
}
if (hasSpmi) {
    dashCatJsonList.add("{\"label\":\"SPMI\",\"icon\":\"clipboard-check\",\"color\":\"4\",\"subs\":["
        +"{\"label\":\"Standar SPMI\",\"url\":"+jsonStr(ctx+"/baru?p=pagesmasterspmistandarspmizul")+"},"
        +"{\"label\":\"Skenario SPMI\",\"url\":"+jsonStr(ctx+"/baru?p=pagesmasterspmiskenariospmizul")+"},"
        +"{\"label\":\"Indikator\",\"url\":"+jsonStr(ctx+"/baru?p=pagesmasterspmiindikatorspmizul")+"},"
        +"{\"label\":\"Butir Mutu\",\"url\":"+jsonStr(ctx+"/baru?p=pagesmasterspmibutirmutuspmizul")+"},"
        +"{\"label\":\"Hasil SPMI\",\"url\":"+jsonStr(ctx+"/baru?p=pagesmasterspmihasilspmizul")+"},"
        +"{\"label\":\"Jenis SPMI\",\"url\":"+jsonStr(ctx+"/baru?p=pagesmasterspmijenisspmizul")+"}"
        +"]}");
}
if (hasKoperasi) {
    /* Dashboard/Simpan Pinjam/Laporan/SHU/Modal = placeholder ZK — tidak diikutkan */
    dashCatJsonList.add("{\"label\":\"Koperasi\",\"icon\":\"handshake\",\"color\":\"5\",\"subs\":["
        +"{\"label\":\"Anggota\",\"url\":"+jsonStr(ctx+"/baru?p=pagesmasterkoperasianggotakoperasizul")+"},"
        +"{\"label\":\"Pembelian\",\"url\":"+jsonStr(ctx+"/baru?p=pagesmasterkoperasipembeliananggotakoperasizul")+"},"
        +"{\"label\":\"Pembayaran\",\"url\":"+jsonStr(ctx+"/baru?p=pagesmasterkoperasipembayarananggotakoperasizul")+"},"
        +"{\"label\":\"Profil Koperasi\",\"url\":"+jsonStr(ctx+"/baru?p=pagesmasterkoperasikoperasizul")+"}"
        +"]}");
}
if (hasGaji) {
    /* Persetujuan & Posting Transaksi = placeholder ZK — tidak diikutkan */
    dashCatJsonList.add("{\"label\":\"Gaji\",\"icon\":\"money-bill-wave\",\"color\":\"6\",\"subs\":["
        +"{\"label\":\"Rencana Gaji\",\"url\":"+jsonStr(ctx+"/baru?p=pagesmasterpayrollrencanagajizul")+"},"
        +"{\"label\":\"Transaksi Pegawai\",\"url\":"+jsonStr(ctx+"/baru?p=pagesmasterpayrolltransaksipegawaizul")+"},"
        +"{\"label\":\"Pembayaran Gaji\",\"url\":"+jsonStr(ctx+"/baru?p=pagesmasterpayrollpembayarangajizul")+"},"
        +"{\"label\":\"Pengajuan Pegawai\",\"url\":"+jsonStr(ctx+"/baru?p=pagesmasterpayrollpengajuanpegawaizul")+"},"
        +"{\"label\":\"Pengajuan Transaksi\",\"url\":"+jsonStr(ctx+"/baru?p=pagesmasterpayrollpengajuantransaksipegawaizul")+"},"
        +"{\"label\":\"Parameter Tambahan\",\"url\":"+jsonStr(ctx+"/baru?p=pagesmasterpayrollparametertambahangajipegawaizul")+"},"
        +"{\"label\":\"PTKP Pegawai\",\"url\":"+jsonStr(ctx+"/baru?p=pagesmasterpayrollptkppegawaizul")+"},"
        +"{\"label\":\"Waktu & Shift\",\"url\":"+jsonStr(ctx+"/baru?p=pagesmasterpayrollwaktushiftzul")+"}"
        +"]}");
}
/* Antar Jemput: 8/9 panel = placeholder ZK → buka hub page langsung via openOverlay */
/* Neo Feeder: Export+Import = placeholder ZK → buka hub page langsung via openOverlay */
for (int di = 0; di < dashCatJsonList.size(); di++) {
    if (di > 0) mobJson.append(",");
    mobJson.append((String) dashCatJsonList.get(di));
}
mobJson.append("],\"appMenus\":[");
boolean firstApp = true;
for (int ri = 0; ri < roots.size(); ri++) {
    Menu rootM = (Menu) roots.get(ri);
    if (rootM.getLabel() == null || rootM.getUrl() == null) continue;
    if (!firstApp) mobJson.append(",");
    firstApp = false;
    String rootUrl = ctx+"/baru?p="+URLEncoder.encode(urlSafe(rootM.getUrl()),"UTF-8")+"&menu="+rootM.getId();
    mobJson.append("{\"id\":").append(rootM.getId())
           .append(",\"label\":").append(jsonStr(rootM.getLabel()))
           .append(",\"url\":").append(jsonStr(rootUrl))
           .append(",\"children\":[");
    List rootCh = childMenus(rootM.getChild(), allMenus);
    boolean firstCh = true;
    for (int ci = 0; ci < rootCh.size(); ci++) {
        Menu chM = (Menu) rootCh.get(ci);
        if (chM.getLabel() == null || chM.getUrl() == null) continue;
        if (!firstCh) mobJson.append(",");
        firstCh = false;
        String chUrl = ctx+"/baru?p="+URLEncoder.encode(urlSafe(chM.getUrl()),"UTF-8")+"&menu="+chM.getId();
        mobJson.append("{\"id\":").append(chM.getId())
               .append(",\"label\":").append(jsonStr(chM.getLabel()))
               .append(",\"url\":").append(jsonStr(chUrl))
               .append(",\"children\":[");
        List grandCh = childMenus(chM.getChild(), allMenus);
        boolean firstGr = true;
        for (int gi = 0; gi < grandCh.size(); gi++) {
            Menu grM = (Menu) grandCh.get(gi);
            if (grM.getLabel() == null || grM.getUrl() == null) continue;
            if (!firstGr) mobJson.append(",");
            firstGr = false;
            String grUrl = ctx+"/baru?p="+URLEncoder.encode(urlSafe(grM.getUrl()),"UTF-8")+"&menu="+grM.getId();
            mobJson.append("{\"id\":").append(grM.getId())
                   .append(",\"label\":").append(jsonStr(grM.getLabel()))
                   .append(",\"url\":").append(jsonStr(grUrl)).append("}");
        }
        mobJson.append("]}");
    }
    mobJson.append("]}");
}
mobJson.append("]}");
%>
<script id="mob-menu-data-json" type="application/json"><%=mobJson.toString()%></script>

<%-- ═══════════════ JAVASCRIPT ═══════════════ --%>
<script>
'use strict';
/* ── Data ─────────────────────────────────────── */
var MOB_DATA = null;
try { MOB_DATA = JSON.parse(document.getElementById('mob-menu-data-json').textContent); } catch(e) {}
var MOB_FLAT_MENUS = []; /* flat list untuk search */
if (MOB_DATA) buildFlatList(MOB_DATA);

function buildFlatList(data) {
    /* Dashboard categories & subs */
    (data.dashCats || []).forEach(function(cat) {
        MOB_FLAT_MENUS.push({label: cat.label, url: '', parent: '', isGroup: true});
        (cat.subs || []).forEach(function(s) {
            MOB_FLAT_MENUS.push({label: s.label, url: s.url, parent: cat.label, isGroup: false});
        });
    });
    /* App menus */
    (data.appMenus || []).forEach(function(root) {
        MOB_FLAT_MENUS.push({label: root.label, url: root.url, parent: '', isGroup: root.children && root.children.length > 0});
        (root.children || []).forEach(function(ch) {
            MOB_FLAT_MENUS.push({label: ch.label, url: ch.url, parent: root.label, isGroup: ch.children && ch.children.length > 0});
            (ch.children || []).forEach(function(gr) {
                MOB_FLAT_MENUS.push({label: gr.label, url: gr.url, parent: ch.label + ' > ' + root.label, isGroup: false});
            });
        });
    });
}

/* ── Panel switching ──────────────────────────── */
var currentPanel = 'home';
function switchPanel(name) {
    if (currentPanel === name) return;
    document.getElementById('panel-' + currentPanel).classList.remove('active');
    document.getElementById('panel-' + name).classList.add('active');
    document.getElementById('nav-' + currentPanel).classList.remove('active');
    document.getElementById('nav-' + name).classList.add('active');
    currentPanel = name;
    if (name === 'menu') { document.getElementById('menu-search').focus(); }
}

/* ── Overlay (full-screen iframe) ─────────────── */
function openOverlay(url, title) {
    if (!url || url === '#') return;
    var ov = document.getElementById('mob-overlay');
    var fr = document.getElementById('mob-iframe');
    var ti = document.getElementById('mob-overlay-title');
    var nw = document.getElementById('mob-overlay-newwin');
    /* Tambahkan _mob=1 ke halaman /baru internal agar iframe hanya menampilkan
       konten modul — tanpa sidebar dan navbar desktop. */
    var iframeUrl = url;
    if (url.indexOf('/baru?') >= 0 || url.indexOf('/baru&') >= 0) {
        iframeUrl = url + '&_mob=1';
    }
    fr.src = iframeUrl;
    ti.textContent = title || '';
    nw.href = url; /* link "buka di tab baru" tetap pakai URL asli (dengan desktop) */
    ov.classList.add('open');
    document.body.style.overflow = 'hidden';
}
function closeOverlay() {
    var ov = document.getElementById('mob-overlay');
    ov.classList.remove('open');
    document.body.style.overflow = '';
    setTimeout(function() {
        document.getElementById('mob-iframe').src = 'about:blank';
    }, 350);
}
/* Kembali dengan tombol hardware (Android) */
window.addEventListener('popstate', function() { closeOverlay(); });

/* ── Desktop link ─────────────────────────────── */
function openDesktop() {
    var url = window.location.protocol + '//' + window.location.host + '<%=ctx%>/baru';
    window.open(url, '_blank');
}

/* ── Logout ───────────────────────────────────── */
function doLogout() {
    if (confirm('Yakin ingin keluar?')) {
        window.location.href = '<%=ctx%>/j_spring_security_logout';
    }
}

/* ── Menu Search ──────────────────────────────── */
var searchTimeout = null;
function handleSearch(val) {
    var clearBtn = document.getElementById('search-clear-btn');
    clearBtn.style.display = val.length > 0 ? 'block' : 'none';
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(function() { doSearch(val.trim()); }, 180);
}
function clearSearch() {
    var inp = document.getElementById('menu-search');
    inp.value = '';
    document.getElementById('search-clear-btn').style.display = 'none';
    doSearch('');
    inp.focus();
}
function doSearch(q) {
    var res = document.getElementById('mob-search-results');
    var cats = document.getElementById('menu-cats-container');
    if (!q) {
        res.style.display = 'none'; res.innerHTML = '';
        cats.style.display = 'block';
        closeDrilldown();
        return;
    }
    cats.style.display = 'none';
    closeDrilldown();
    var ql = q.toLowerCase();
    var matches = MOB_FLAT_MENUS.filter(function(m) {
        return !m.isGroup && m.label.toLowerCase().indexOf(ql) >= 0;
    });
    if (matches.length === 0) {
        res.innerHTML = '<div class="mob-search-empty"><i class="fas fa-search"></i>Tidak ada menu &ldquo;' + esc(q) + '&rdquo;</div>';
    } else {
        var html = '<div style="padding:4px 0;">';
        matches.slice(0, 60).forEach(function(m) {
            html += '<div class="mob-result-item" onclick="openMenuLeaf(\'' + js(m.url) + '\',\'' + js(m.label) + '\')">'
                  + '<div class="mob-result-ic"><i class="fas fa-bars"></i></div>'
                  + '<div><div class="mob-result-lbl">' + esc(m.label) + '</div>'
                  + (m.parent ? '<span class="mob-result-parent">' + esc(m.parent) + '</span>' : '')
                  + '</div>'
                  + '<i class="fas fa-chevron-right" style="color:#c0c8d8;font-size:11px;"></i></div>';
        });
        if (matches.length > 60) html += '<div style="text-align:center;font-size:11px;color:#9ca3b0;padding:10px;">... dan ' + (matches.length-60) + ' hasil lainnya. Perinci kata kunci.</div>';
        html += '</div>';
        res.innerHTML = html;
    }
    res.style.display = 'block';
}
function esc(s) {
    return String(s || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
function js(s) {
    return String(s || '').replace(/\\/g,'\\\\').replace(/'/g,"\\'");
}

/* ── Dashboard Drilldown (e-Learning, Prestasi, dsb.) ── */
var drilldownData = {}; /* cache data per kategori */
function openDrilldown(catKey, title, colorIdx) {
    var data = MOB_DATA && MOB_DATA.dashCats ? MOB_DATA.dashCats.find(function(c){ return c.label === title; }) : null;
    var dd = document.getElementById('mob-drilldown');
    document.getElementById('mob-drilldown-title').textContent = title;
    var list = document.getElementById('mob-drilldown-list');
    list.innerHTML = '';
    if (data && data.subs && data.subs.length > 0) {
        data.subs.forEach(function(s) {
            var row = document.createElement('div');
            row.className = 'mob-menu-row';
            row.innerHTML = '<div class="mob-menu-row-ic"><i class="fas fa-angle-right"></i></div>'
                          + '<div class="mob-menu-row-lbl">' + esc(s.label) + '</div>'
                          + '<i class="fas fa-chevron-right mob-menu-row-go"></i>';
            row.onclick = function() { openMenuLeaf(s.url, s.label); };
            list.appendChild(row);
        });
    } else {
        list.innerHTML = '<div class="mob-empty"><i class="fas fa-folder-open"></i><div class="mob-empty-title">Tidak ada sub-menu</div></div>';
    }
    dd.classList.add('active');
}

/* ── App Menu Drilldown ───────────────────────── */
function openMenuDrilldown(rootId, title, colorIdx) {
    var appMenus = MOB_DATA && MOB_DATA.appMenus ? MOB_DATA.appMenus : [];
    var root = appMenus.find(function(m){ return m.id == rootId; });
    var dd = document.getElementById('mob-drilldown');
    document.getElementById('mob-drilldown-title').textContent = title;
    var list = document.getElementById('mob-drilldown-list');
    list.innerHTML = '';
    if (root && root.children && root.children.length > 0) {
        root.children.forEach(function(ch) {
            var hasKids = ch.children && ch.children.length > 0;
            var row = document.createElement('div');
            row.className = 'mob-menu-row' + (hasKids ? ' is-cat' : '');
            row.innerHTML = '<div class="mob-menu-row-ic"><i class="fas fa-' + (hasKids ? 'folder' : 'angle-right') + '"></i></div>'
                          + '<div class="mob-menu-row-lbl">' + esc(ch.label) + '</div>'
                          + '<i class="fas fa-chevron-right mob-menu-row-go"></i>';
            if (hasKids) {
                row.onclick = function() { openSubDrilldown(ch, title); };
            } else {
                row.onclick = function() { openMenuLeaf(ch.url, ch.label); };
            }
            list.appendChild(row);
        });
    } else {
        var row = document.createElement('div');
        row.className = 'mob-menu-row';
        row.innerHTML = '<div class="mob-menu-row-ic"><i class="fas fa-external-link-alt"></i></div>'
                      + '<div class="mob-menu-row-lbl">Buka ' + esc(title) + '</div>'
                      + '<i class="fas fa-chevron-right mob-menu-row-go"></i>';
        row.onclick = function() { if (root) openMenuLeaf(root.url, title); };
        list.appendChild(row);
    }
    dd.classList.add('active');
}

/* Sub-drilldown (level 2) */
var subDrilldownStack = [];
function openSubDrilldown(chNode, parentTitle) {
    subDrilldownStack.push({node: chNode, parentTitle: parentTitle});
    document.getElementById('mob-drilldown-title').textContent = chNode.label;
    var list = document.getElementById('mob-drilldown-list');
    list.innerHTML = '';
    (chNode.children || []).forEach(function(gr) {
        var row = document.createElement('div');
        row.className = 'mob-menu-row';
        row.innerHTML = '<div class="mob-menu-row-ic"><i class="fas fa-angle-right"></i></div>'
                      + '<div class="mob-menu-row-lbl">' + esc(gr.label) + '</div>'
                      + '<i class="fas fa-chevron-right mob-menu-row-go"></i>';
        row.onclick = function() { openMenuLeaf(gr.url, gr.label); };
        list.appendChild(row);
    });
}

function closeDrilldown() {
    subDrilldownStack = [];
    var dd = document.getElementById('mob-drilldown');
    dd.classList.remove('active');
    document.getElementById('mob-drilldown-list').innerHTML = '';
}

/* ── Open leaf menu in overlay ────────────────── */
function openMenuLeaf(url, label) {
    if (!url || url.indexOf('about:blank') >= 0) return;
    openOverlay(url, label);
}

/* ── Notif detail modal ───────────────────────── */
function showNotifDetail(title, desc) {
    document.getElementById('mob-notif-modal-title').textContent = title || '';
    document.getElementById('mob-notif-modal-desc').textContent = desc || 'Tidak ada keterangan tambahan.';
    document.getElementById('mob-notif-modal').style.display = 'flex';
}
function closeNotifModal() {
    document.getElementById('mob-notif-modal').style.display = 'none';
}

/* ── Swipe-back gesture (opsional) ───────────────────────── */
(function() {
    var startX = 0;
    var ov = null;
    document.addEventListener('touchstart', function(e) {
        if (e.touches.length === 1) startX = e.touches[0].clientX;
        ov = document.getElementById('mob-overlay');
    }, {passive: true});
    document.addEventListener('touchend', function(e) {
        if (!ov || !ov.classList.contains('open')) return;
        var endX = e.changedTouches[0].clientX;
        if (endX - startX > 80 && startX < 30) { closeOverlay(); }
    }, {passive: true});
})();
</script>

</body>
</html>
