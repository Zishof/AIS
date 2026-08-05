<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.database.model.PerguruanTinggi" %>
<%@ page import="ais.database.model.sekolah.Sekolah" %>
<%@ page import="ais.database.model.sekolah.Yayasan" %>
<%@ page import="ais.database.model.Tbmuser" %>
<%@ page import="ais.database.model.Tbmrole" %>
<%@ page import="ais.action.master.helper.util.PerguruanTinggiUtil" %>
<%@ page import="ais.action.master.sekolah.util.SekolahUtil" %>
<%@ page import="ais.common.Common" %>
<%--
    front_end/utama/index.jsp — Halaman utama (shell) aplikasi versi baru & modern.
    Menggabungkan desain mockup TBU (tbu.css) dengan fitur-fitur dari index.zul:
    - Header: logo, nama institusi, motto, tombol-tombol modul
    - Sidebar navigasi responsive
    - Konten area tab-based
    - Back-to-top, WhatsApp CS, Online users

    Akses: melalui servlet /baru2 atau /main?versi_baru=true dengan piilhanTampilan=baru
--%>
<%!
    private String safeHtml(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
    private String safeJs(String v) {
        if (v == null) return "";
        return v.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'").replace("\n", " ").replace("\r", "");
    }
    private String sapaanWaktu() {
        int h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (h >= 4 && h < 11) return "Pagi";
        if (h >= 11 && h < 15) return "Siang";
        if (h >= 15 && h < 19) return "Sore";
        return "Malam";
    }
    private String roleToLabel(String r) {
        if (r == null) return "";
        if ("am".equals(r))             return "Administrator";
        if ("mhs".equals(r))            return "Mahasiswa";
        if ("mhss2".equals(r))          return "Mahasiswa S2";
        if ("Dosen".equals(r))          return "Dosen";
        if ("Guru".equals(r))           return "Guru";
        if ("siswa".equals(r))          return "Siswa";
        if ("Akademik".equals(r))       return "Staf Akademik";
        if ("peg".equals(r))            return "Pegawai";
        if ("ortu".equals(r)||"Ortu".equals(r)) return "Orang Tua";
        if ("Kantin".equals(r))         return "Kasir Kantin";
        if ("angt_koperasi".equals(r))  return "Anggota Koperasi";
        if ("angt_pustaka".equals(r))   return "Anggota Perpustakaan";
        if ("peserta".equals(r))        return "Peserta";
        if ("calon_peg".equals(r))      return "Calon Pegawai";
        if ("Presensi".equals(r))       return "Operator Presensi";
        if ("Dokter".equals(r))         return "Dokter";
        if ("SPI".equals(r))            return "SPI";
        return r; // fallback: tampilkan apa adanya
    }
%>
<%
    // === Data Institusi ===
    String nama = "";
    String motto = "eCampus Information System";
    String logo = "/img/logo.png";
    String waCs = "";
    String rootCtx = Common.ROOT == null ? "" : Common.ROOT;

    // === Data Pengguna ===
    String usernameDisplay = "";
    String namaLengkap = "Pengguna";
    String roleDisplay = "";
    String avatarText = "US";
    boolean isLoggedIn = false;

    try {
        // Institusi
        PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
        if (pt != null) {
            nama = pt.getNama() == null ? "" : pt.getNama();
            if (pt.getMotto() != null && !pt.getMotto().trim().isEmpty()) motto = pt.getMotto();
            waCs = pt.getWa() == null ? "" : pt.getWa();
        }
        String logoPT = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
        if (logoPT != null && !logoPT.trim().isEmpty()) logo = logoPT;

        boolean[] ptSekolah = Common.chekPtAtauSekolah();
        boolean sekolahMode = ptSekolah != null && ptSekolah.length > 1 && ptSekolah[1];
        if (sekolahMode) {
            Sekolah sekolah = SekolahUtil.getSekolah(request);
            if (sekolah != null && sekolah.getId() != null) {
                if (sekolah.getNama() != null && !sekolah.getNama().trim().isEmpty()) nama = sekolah.getNama();
                if (sekolah.getMotto() != null && !sekolah.getMotto().trim().isEmpty()) motto = sekolah.getMotto();
                if (sekolah.getWa() != null && !sekolah.getWa().trim().isEmpty()) waCs = sekolah.getWa();
                String logoS = SekolahUtil.getSekolahMedia(request, "logo_sekolah_");
                if (logoS != null && !logoS.trim().isEmpty() && !logoS.endsWith("logo.png")) logo = logoS;
            } else {
                Yayasan yayasan = SekolahUtil.getYayasan(request);
                if (yayasan != null && yayasan.getId() != null) {
                    if (yayasan.getNama() != null && !yayasan.getNama().trim().isEmpty()) nama = yayasan.getNama();
                    if (yayasan.getMotto() != null && !yayasan.getMotto().trim().isEmpty()) motto = yayasan.getMotto();
                    if (yayasan.getWa() != null && !yayasan.getWa().trim().isEmpty()) waCs = yayasan.getWa();
                    String logoY = SekolahUtil.getYayasanMedia(request, "logo_yayasan_");
                    if (logoY != null && !logoY.trim().isEmpty() && !logoY.endsWith("logo.png")) logo = logoY;
                }
            }
        }

        // Pengguna
        javax.servlet.http.HttpSession sesiHttp = request.getSession(false);
        if (sesiHttp != null) {
            /* "login" = LogLogin setelah AJAX login; Tbmuser ada di "usersTemp" */
            Object loginAttr = sesiHttp.getAttribute("login");
            Tbmuser user = null;
            if (loginAttr instanceof Tbmuser) {
                user = (Tbmuser) loginAttr;
            } else if (sesiHttp.getAttribute("usersTemp") instanceof Tbmuser) {
                user = (Tbmuser) sesiHttp.getAttribute("usersTemp");
            }
            if (user != null) {
                isLoggedIn = true;
                usernameDisplay = user.getUserId() == null ? "" : user.getUserId();
                namaLengkap = usernameDisplay;
                try {
                    if (user.getPegawai() != null && user.getPegawai().getNama() != null) namaLengkap = user.getPegawai().getNama();
                    else if (user.getMahasiswa() != null && user.getMahasiswa().getNama() != null) namaLengkap = user.getMahasiswa().getNama();
                } catch (Exception ex) { /* ignore */ }
                try {
                    Tbmrole hakAkses = user.hakAkses();
                    if (hakAkses != null && hakAkses.getRoleId() != null) roleDisplay = roleToLabel(hakAkses.getRoleId());
                } catch (Exception ex) { /* ignore */ }
                if (!namaLengkap.trim().isEmpty()) {
                    String[] parts = namaLengkap.trim().split("\\s+");
                    avatarText = parts.length >= 2
                        ? ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase()
                        : ("" + parts[0].charAt(0)).toUpperCase();
                }
            }
        }
    } catch (Exception ex) { /* ignore */ }

    boolean isMobile = Common.isMobile(request);
%>
<!doctype html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
<title><%=safeHtml(nama.isEmpty() ? "Sistem Informasi Akademik" : nama)%></title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<style>
/* === Reset & Root === */
:root{
  --pri:#1e40af; --pri2:#4f46e5; --acc:#6d28d9; --gold:#d4a017;
  --grad:linear-gradient(120deg,#1e3a8a 0%,#4f46e5 55%,#6d28d9 100%);
  --ok:#10b981; --warn:#f59e0b; --bad:#ef4444; --info:#0ea5e9;
  --ink:#1e293b; --ink2:#64748b; --ink3:#94a3b8;
  --line:#e6e9f0; --chip:#f1f5f9; --bg:#f5f7fb; --card:#ffffff;
  --shadow:0 4px 16px rgba(30,41,59,.06);
  --shadow-lg:0 12px 32px rgba(30,41,59,.12);
  --r:14px; --r-sm:10px;
  --sbw:260px;
  --font:"Plus Jakarta Sans","Segoe UI",system-ui,-apple-system,sans-serif;
  --topbar-h:58px;
}
*{box-sizing:border-box;margin:0;padding:0}
html,body{height:100%;overflow:hidden}
body{font-family:var(--font);background:var(--bg);color:var(--ink);font-size:14px;line-height:1.5;-webkit-font-smoothing:antialiased}
a{text-decoration:none;color:inherit}
::-webkit-scrollbar{width:6px;height:6px}
::-webkit-scrollbar-thumb{background:#cbd5e1;border-radius:20px}
button{font-family:inherit;cursor:pointer;border:none;background:none}

/* === Shell Layout === */
.shell{display:grid;grid-template-columns:var(--sbw) 1fr;grid-template-rows:var(--topbar-h) 1fr;height:100vh;overflow:hidden}
.topbar{grid-column:1/-1;grid-row:1;z-index:50;background:var(--grad);display:flex;align-items:center;gap:0;padding:0}
.sidebar{grid-column:1;grid-row:2;background:var(--card);border-right:1px solid var(--line);overflow-y:auto;height:100%;transition:.25s}
.main{grid-column:2;grid-row:2;display:flex;flex-direction:column;overflow:hidden;height:100%}
.content-area{flex:1;overflow:hidden;position:relative}

/* === Topbar === */
.tb-brand{display:flex;align-items:center;gap:11px;padding:0 18px;border-right:1px solid rgba(255,255,255,.15);height:100%;min-width:var(--sbw)}
.tb-brand img{height:36px;width:auto;max-width:160px;object-fit:contain}
.tb-brand-txt{color:#fff}
.tb-brand-txt b{display:block;font-size:13.5px;font-weight:800;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:170px}
.tb-brand-txt span{font-size:10.5px;opacity:.8;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:170px;display:block}
.tb-modules{display:flex;align-items:center;gap:3px;padding:0 10px;flex:1;overflow-x:auto;scrollbar-width:none}
.tb-modules::-webkit-scrollbar{display:none}
.tb-btn{display:inline-flex;align-items:center;gap:6px;padding:7px 12px;border-radius:9px;color:rgba(255,255,255,.85);font-size:12.5px;font-weight:600;white-space:nowrap;transition:.15s;border:1px solid transparent}
.tb-btn:hover{background:rgba(255,255,255,.15);color:#fff;border-color:rgba(255,255,255,.2)}
.tb-btn.active{background:rgba(255,255,255,.2);color:#fff;border-color:rgba(255,255,255,.3)}
.tb-btn i{font-size:13px;width:15px;text-align:center}
.tb-right{display:flex;align-items:center;gap:8px;padding:0 14px;border-left:1px solid rgba(255,255,255,.15);flex-shrink:0}
.tb-icon-btn{width:34px;height:34px;border-radius:9px;border:1px solid rgba(255,255,255,.2);background:rgba(255,255,255,.1);color:#fff;display:flex;align-items:center;justify-content:center;font-size:14px;transition:.15s;position:relative}
.tb-icon-btn:hover{background:rgba(255,255,255,.2)}
.notif-dot{position:absolute;top:-4px;right:-4px;width:16px;height:16px;background:var(--bad);border-radius:50%;font-size:9px;font-weight:700;color:#fff;display:flex;align-items:center;justify-content:center}
.tb-profile{display:flex;align-items:center;gap:9px;padding-left:10px;cursor:pointer}
.tb-avatar{width:34px;height:34px;border-radius:50%;background:rgba(255,255,255,.25);color:#fff;display:flex;align-items:center;justify-content:center;font-weight:800;font-size:12px;border:2px solid rgba(255,255,255,.4);flex-shrink:0}
.tb-profile-txt{color:#fff}
.tb-profile-txt b{display:block;font-size:12.5px;font-weight:700;max-width:120px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.tb-profile-txt span{font-size:10.5px;opacity:.8}
.tb-online{display:flex;align-items:center;gap:6px;color:rgba(255,255,255,.85);font-size:12px;font-weight:600;cursor:pointer;padding:6px 10px;border-radius:9px;transition:.15s;white-space:nowrap}
.tb-online:hover{background:rgba(255,255,255,.15)}
.tb-online .dot{width:8px;height:8px;border-radius:50%;background:#4ade80;box-shadow:0 0 0 2px rgba(74,222,128,.3);flex-shrink:0}
.hamb{display:none;color:#fff;font-size:19px;padding:8px 14px;margin-right:4px}

/* === Sidebar === */
.sb-head{display:none} /* hidden di desktop, shown di mobile */
.sb-nav{padding:10px 10px 24px}
.sb-group{font-size:10px;font-weight:800;letter-spacing:.9px;text-transform:uppercase;color:var(--pri2);opacity:.7;padding:16px 12px 6px}
.sb-item{display:flex;align-items:center;gap:10px;padding:9px 12px;border-radius:10px;color:#475569;font-weight:600;font-size:13px;cursor:pointer;transition:.15s;margin-bottom:2px;position:relative}
.sb-item .ic{width:18px;text-align:center;color:#94a3b8;flex-shrink:0;font-size:13px}
.sb-item:hover{background:#f1f5ff;color:var(--pri2)}
.sb-item:hover .ic{color:var(--pri2)}
.sb-item.active{background:var(--grad);color:#fff;box-shadow:var(--shadow)}
.sb-item.active .ic{color:#fff}
.sb-item .chev{margin-left:auto;color:#cbd5e1;font-size:10px;transition:.15s}
.sb-backdrop{display:none;position:fixed;inset:0;background:rgba(15,23,42,.4);z-index:45}

/* === Tab bar (tab strip di bawah topbar) === */
.tab-bar{background:var(--card);border-bottom:1px solid var(--line);display:flex;align-items:stretch;overflow-x:auto;scrollbar-width:none;flex-shrink:0}
.tab-bar::-webkit-scrollbar{display:none}
.tab-item{display:inline-flex;align-items:center;gap:7px;padding:10px 16px;font-size:13px;font-weight:600;color:var(--ink2);cursor:pointer;white-space:nowrap;border-bottom:2px solid transparent;transition:.15s;flex-shrink:0}
.tab-item:hover{color:var(--pri2);background:#f8f9ff}
.tab-item.active{color:var(--pri2);border-bottom-color:var(--pri2)}
.tab-item img{height:16px;width:16px}
.tab-close{margin-left:4px;width:18px;height:18px;border-radius:5px;color:#94a3b8;display:flex;align-items:center;justify-content:center;font-size:11px}
.tab-close:hover{background:#fee2e2;color:var(--bad)}
.tab-home img,.tab-home i{color:var(--pri)}

/* === Home Panel (default konten) === */
.home-panel{height:100%;overflow-y:auto;padding:24px}
.home-hero{border-radius:var(--r);background:var(--grad);color:#fff;padding:26px 28px;position:relative;overflow:hidden;margin-bottom:22px;box-shadow:var(--shadow-lg)}
.home-hero::after{content:"";position:absolute;right:-40px;top:-60px;width:260px;height:260px;border-radius:50%;background:rgba(255,255,255,.07)}
.home-hero h2{font-size:22px;font-weight:800;margin-bottom:6px;position:relative}
.home-hero p{opacity:.9;margin-bottom:16px;font-size:13.5px;position:relative;max-width:520px}
.home-hero .pills{display:flex;gap:10px;flex-wrap:wrap;position:relative}
.pill{display:inline-flex;align-items:center;gap:6px;background:rgba(255,255,255,.18);border-radius:30px;padding:6px 13px;font-size:12px;font-weight:600}
.stats-row{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin-bottom:22px}
.stat-card{background:var(--card);border:1px solid var(--line);border-radius:var(--r);padding:16px;box-shadow:var(--shadow);display:flex;flex-direction:column;gap:8px}
.stat-card .s-top{display:flex;align-items:center;justify-content:space-between}
.stat-card .s-ic{width:40px;height:40px;border-radius:11px;display:flex;align-items:center;justify-content:center;color:#fff;font-size:16px}
.stat-card .s-val{font-size:24px;font-weight:800;letter-spacing:-.5px}
.stat-card .s-lbl{color:var(--ink2);font-size:12px}
.mods-section{margin-bottom:26px}
.mods-head{display:flex;align-items:center;gap:9px;margin-bottom:13px}
.mods-head .accent-bar{width:5px;height:20px;border-radius:4px}
.mods-head h3{font-size:15px;font-weight:800}
.badge{font-size:11px;font-weight:700;padding:2px 9px;border-radius:20px;display:inline-flex;align-items:center}
.badge-gray{color:#475569;background:#eef2f7}
.mod-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));gap:12px}
.mod-card{display:flex;gap:12px;align-items:flex-start;padding:14px;border:1px solid var(--line);border-radius:12px;background:var(--card);box-shadow:var(--shadow);transition:.16s;cursor:pointer}
.mod-card:hover{transform:translateY(-3px);box-shadow:var(--shadow-lg);border-color:transparent}
.mod-card .m-ic{width:42px;height:42px;border-radius:12px;display:flex;align-items:center;justify-content:center;color:#fff;font-size:17px;flex-shrink:0;box-shadow:var(--shadow)}
.mod-card h4{margin:0 0 2px;font-size:13.5px;font-weight:700;line-height:1.2}
.mod-card p{margin:0;font-size:11.5px;color:var(--ink2);line-height:1.4}
.mod-card .m-tag{display:inline-block;margin-top:6px;font-size:10px;font-weight:700;color:#3730a3;background:#e0e7ff;padding:2px 7px;border-radius:20px}

/* === Papan Pengumuman + Kalender Akademik (kolom kiri) === */
.home-2col{display:grid;grid-template-columns:1fr 300px;gap:20px;align-items:start}
.left-col{display:flex;flex-direction:column;gap:10px}
.ann-panel{background:var(--card);border:1px solid var(--line);border-radius:var(--r);display:flex;flex-direction:column;overflow:hidden;box-shadow:var(--shadow);max-height:340px;flex-shrink:0}
.ann-header{padding:14px 16px;border-bottom:1px solid var(--line);flex-shrink:0}
.ann-title{display:flex;align-items:center;gap:8px;margin-bottom:10px}
.ann-title h3{font-size:14px;font-weight:800;flex:1}
.ann-search{display:flex;gap:6px}
.ann-search input{flex:1;border:1px solid var(--line);border-radius:8px;padding:7px 11px;font-size:12.5px;font-family:inherit;outline:none;background:var(--bg);color:var(--ink);transition:.15s}
.ann-search input:focus{border-color:var(--pri)}
.ann-search button{padding:7px 12px;border-radius:8px;background:var(--pri2);color:#fff;font-size:12px;font-weight:700;border:none;cursor:pointer}
.ann-list{flex:1;overflow-y:auto;padding:10px}
.ann-group-title{font-size:10px;font-weight:800;text-transform:uppercase;letter-spacing:.7px;color:var(--ink3);padding:8px 2px 5px;border-bottom:1px solid var(--line);margin-bottom:6px}
.ann-card{padding:11px 12px;border-radius:10px;border:1px solid var(--line);margin-bottom:7px;cursor:pointer;transition:.15s;background:var(--card)}
.ann-card:hover{border-color:var(--pri);background:#f0f4ff}
.ann-card.utama{border-left:3px solid var(--pri2);background:#f8f9ff}
.ann-card.utama:hover{background:#eef2ff}
.ann-chips{display:flex;flex-wrap:wrap;gap:4px;margin-bottom:6px}
.ann-chip{font-size:10px;font-weight:700;padding:2px 7px;border-radius:20px;white-space:nowrap}
.ann-chip.cp{background:#e0e7ff;color:#3730a3}
.ann-chip.cd{background:#f0fdf4;color:#15803d}
.ann-chip.ca{background:#fef3c7;color:#92400e}
.ann-chip.t1{background:#fce7f3;color:#9d174d}
.ann-chip.t2{background:#ede9fe;color:#5b21b6}
.ann-chip.t3{background:#e0f2fe;color:#075985}
.ann-chip.t4{background:#f0fdf4;color:#14532d}
.ann-chip.t5{background:#fff7ed;color:#7c2d12}
.ann-card h4{font-size:12.5px;font-weight:700;line-height:1.4;margin-bottom:3px;color:var(--ink)}
.ann-card p{font-size:11.5px;color:var(--ink2);line-height:1.5;overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}
.ann-pager{display:flex;align-items:center;justify-content:space-between;padding:8px 12px;border-top:1px solid var(--line);flex-shrink:0}
.ann-pb{padding:5px 11px;border-radius:7px;font-size:12px;font-weight:600;border:1px solid var(--line);background:var(--card);color:var(--ink2);cursor:pointer;transition:.15s}
.ann-pb:hover:not(:disabled){background:var(--pri2);color:#fff;border-color:var(--pri2)}
.ann-pb:disabled{opacity:.4;cursor:default}
.ann-empty{text-align:center;padding:30px 12px;color:var(--ink3);font-size:13px}
/* Modal pengumuman detail */
.ann-modal{display:none;position:fixed;inset:0;z-index:1050;background:rgba(15,23,42,.55);align-items:center;justify-content:center;padding:20px}
.ann-modal.show{display:flex}
.ann-mbox{background:var(--card);border-radius:var(--r);max-width:720px;width:100%;max-height:88vh;display:flex;flex-direction:column;box-shadow:var(--shadow-lg);overflow:hidden}
.ann-mhead{padding:18px 20px 12px;border-bottom:1px solid var(--line);flex-shrink:0}
.ann-mhead h3{font-size:15px;font-weight:800;line-height:1.4;margin-top:8px}
.ann-mbody{flex:1;overflow-y:auto;padding:18px 20px;line-height:1.75;font-size:14px;color:var(--ink)}
.ann-mbody img{max-width:100%;height:auto;border-radius:8px;margin:8px 0}
.ann-mfoot{padding:12px 20px;border-top:1px solid var(--line);flex-shrink:0;display:flex;justify-content:flex-end;gap:10px}
.ann-mbtn{padding:8px 18px;border-radius:9px;font-size:13px;font-weight:700;border:none;cursor:pointer}
.ann-mbtn.p{background:var(--pri2);color:#fff}
.ann-mbtn.s{background:var(--line);color:var(--ink)}
/* === Kalender Akademik — kartu grid === */
.kal-panel{background:var(--card);border:1px solid var(--line);border-radius:var(--r);display:flex;flex-direction:column;overflow:hidden;box-shadow:var(--shadow)}
.kal-header{padding:12px 14px 12px;border-bottom:1px solid var(--line);flex-shrink:0}
.kal-eyebrow{font-size:9px;font-weight:800;text-transform:uppercase;letter-spacing:1.2px;color:var(--pri2);opacity:.7;margin-bottom:3px}
.kal-title{display:flex;align-items:center;gap:7px;margin-bottom:4px}
.kal-title h3{font-size:13.5px;font-weight:800;flex:1}
.kal-subtitle{font-size:11px;color:var(--ink3);line-height:1.4;margin-bottom:10px}
.kal-filters{display:flex;gap:7px}
.kal-filters select{flex:1;border:1px solid var(--line);border-radius:8px;padding:6px 8px;font-size:11.5px;font-family:inherit;outline:none;background:var(--bg);color:var(--ink);cursor:pointer}
.kal-cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(130px,1fr));gap:10px;padding:14px;max-height:380px;overflow-y:auto}
.kal-card{background:#1e3a6e;border-radius:12px;padding:12px 10px;color:#fff;cursor:pointer;transition:.15s}
.kal-card:hover{transform:translateY(-2px);filter:brightness(1.1)}
.kal-card-day{font-size:28px;font-weight:800;line-height:1;margin-bottom:1px}
.kal-card-mon{font-size:9px;text-transform:uppercase;letter-spacing:.6px;opacity:.7;font-weight:700;margin-bottom:8px}
.kal-card-name{font-size:11.5px;font-weight:700;line-height:1.4;margin-bottom:6px;overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}
.kal-card-meta{display:flex;flex-wrap:wrap;gap:3px;margin-bottom:4px}
.kal-cbadge{font-size:9px;font-weight:700;padding:2px 6px;border-radius:8px;white-space:nowrap}
.kal-cbadge.ta{background:rgba(255,255,255,.18);color:#fff}
.kal-cbadge.ganjil{background:#f59e0b;color:#fff}
.kal-cbadge.genap{background:#3b82f6;color:#fff}
.kal-cbadge.berlangsung{background:#10b981;color:#fff}
.kal-cbadge.selesai{background:rgba(255,255,255,.15);color:rgba(255,255,255,.75)}
.kal-cbadge.belum{background:rgba(255,255,255,.12);color:rgba(255,255,255,.85)}
.kal-card-range{font-size:9px;opacity:.6;line-height:1.4}
/* right-col sticky agar profil mengikuti scroll */
.right-col{position:sticky;top:16px;align-self:start}
@media(max-width:960px){.home-2col{grid-template-columns:1fr}.right-col{position:static;order:-1}.ann-panel{max-height:360px;margin-bottom:0}.kal-cards{max-height:260px;grid-template-columns:repeat(auto-fill,minmax(110px,1fr))}}
@media(max-width:680px){.home-2col{gap:12px}}

/* === Iframe container === */
.iframe-pane{width:100%;height:100%;border:none;display:none}
.iframe-pane.visible{display:block}

/* === Aksi bawah (CS + back-to-top) === */
.float-actions{position:fixed;bottom:22px;right:18px;display:flex;flex-direction:column;gap:10px;z-index:99}
.float-btn{width:46px;height:46px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:19px;box-shadow:var(--shadow-lg);transition:.2s;border:none;cursor:pointer}
.float-btn:hover{transform:scale(1.1)}
.float-wa{background:#25d366;color:#fff}
.float-top{background:var(--grad);color:#fff;display:none}
.float-top.show{display:flex}

/* === Panel Profil Pengguna (kolom kanan home) === */
.profil-panel{background:var(--card);border:1px solid var(--line);border-radius:var(--r);overflow:hidden;box-shadow:var(--shadow);display:flex;flex-direction:column;min-width:0}
.profil-header{padding:22px 18px 18px;color:#fff;position:relative;overflow:hidden;text-align:center}
.profil-header::before{content:"";position:absolute;top:-30px;right:-30px;width:120px;height:120px;border-radius:50%;background:rgba(255,255,255,.07)}
.profil-header::after{content:"";position:absolute;bottom:-20px;left:-20px;width:80px;height:80px;border-radius:50%;background:rgba(255,255,255,.05)}
.profil-sapaan{font-size:11.5px;font-weight:700;opacity:.85;letter-spacing:.3px;margin-bottom:10px;position:relative}
.profil-avatar-wrap{position:relative;display:inline-block;margin-bottom:10px}
.profil-avatar{width:60px;height:60px;border-radius:50%;background:rgba(255,255,255,.22);color:#fff;display:flex;align-items:center;justify-content:center;font-size:22px;font-weight:800;border:3px solid rgba(255,255,255,.35);margin:0 auto;position:relative}
.profil-nama{font-size:14.5px;font-weight:800;line-height:1.3;position:relative;max-width:220px;margin:0 auto 6px}
.profil-badge-row{margin-bottom:6px;position:relative}
.profil-badge{display:inline-block;font-size:10px;font-weight:700;padding:2px 10px;border-radius:20px;color:#fff;letter-spacing:.3px}
.profil-unit{font-size:11px;opacity:.8;position:relative;margin-bottom:4px;max-width:200px;margin-left:auto;margin-right:auto}
.profil-id-row{font-size:10.5px;opacity:.75;position:relative}
.profil-id-lbl{font-weight:700;margin-right:3px}
.profil-id-val{font-family:monospace;letter-spacing:.3px}
.profil-stats{display:grid;grid-template-columns:repeat(3,1fr);gap:0;border-bottom:1px solid var(--line)}
.profil-stat{padding:13px 8px;text-align:center;border-right:1px solid var(--line);display:flex;flex-direction:column;align-items:center;gap:4px}
.profil-stat:last-child{border-right:none}
.profil-stat-ic{font-size:16px;margin-bottom:2px;opacity:.9}
.profil-stat-val{font-size:15px;font-weight:800;letter-spacing:-.3px;color:var(--ink);line-height:1.1;word-break:break-all;max-width:100%}
.profil-stat-lbl{font-size:10px;color:var(--ink2);font-weight:600;line-height:1.2}
.profil-links{padding:14px 14px 8px}
.profil-links-title{font-size:10px;font-weight:800;letter-spacing:.8px;text-transform:uppercase;color:var(--ink3);margin-bottom:8px}
.profil-links-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:7px}
.profil-link{display:flex;align-items:center;gap:9px;padding:9px 10px;border-radius:10px;border:1px solid var(--line);background:var(--bg);text-decoration:none;color:var(--ink);transition:.15s;cursor:pointer}
.profil-link:hover{border-color:var(--pri);background:#f0f4ff;color:var(--pri)}
.profil-link-ic{width:30px;height:30px;border-radius:8px;display:flex;align-items:center;justify-content:center;font-size:13px;flex-shrink:0}
.profil-link-lbl{font-size:12px;font-weight:700;line-height:1.2}
.profil-footer{padding:10px 14px 13px}
.profil-full-btn{width:100%;padding:9px 14px;border-radius:10px;background:transparent;border:1px solid var(--line);color:var(--ink2);font-size:12.5px;font-weight:700;cursor:pointer;transition:.15s;font-family:inherit;display:flex;align-items:center;justify-content:center;gap:7px}
.profil-full-btn:hover{background:var(--pri2);color:#fff;border-color:var(--pri2)}
/* right-col di home-2col */
.right-col{min-width:0;display:flex;flex-direction:column;gap:10px}
@media(max-width:960px){.profil-panel .profil-stats{grid-template-columns:repeat(3,1fr)}.profil-links-grid{grid-template-columns:repeat(2,1fr)}}

/* NProgress sudah menangani loading bar via _libs.jsp */

/* === Responsive === */
@media(max-width:1024px){
  :root{--sbw:0px}
  .shell{grid-template-columns:1fr}
  .topbar{grid-column:1/-1}
  .sidebar{position:fixed;left:-270px;width:260px;top:var(--topbar-h);height:calc(100vh - var(--topbar-h));z-index:100;transition:.25s;box-shadow:var(--shadow-lg)}
  .sidebar.open{left:0}
  .sb-backdrop.show{display:block}
  .main{grid-column:1}
  .hamb{display:flex}
  .tb-brand{min-width:auto;padding:0 12px}
  .tb-brand img{height:30px}
  .tb-modules{display:none}
  .tb-profile-txt{display:none}
  .stats-row{grid-template-columns:repeat(2,1fr)}
}
@media(max-width:680px){
  .stats-row{grid-template-columns:1fr 1fr}
  .home-panel{padding:14px}
  .home-hero{padding:18px}
  .home-hero h2{font-size:18px}
  .mod-grid{grid-template-columns:1fr}
  .tb-online span{display:none}
  .tb-brand-txt{display:none}
}

/* === Ganti Bahasa === */
.tb-lang-wrap{position:relative}
.lang-btn{display:flex;align-items:center;gap:5px;padding:5px 10px;border-radius:8px;background:rgba(255,255,255,.12);border:1px solid rgba(255,255,255,.2);color:#fff;font-size:11px;font-weight:700;cursor:pointer;transition:.13s;font-family:inherit;white-space:nowrap}
.lang-btn:hover{background:rgba(255,255,255,.22)}
.lang-menu{position:absolute;top:calc(100% + 8px);right:0;background:var(--card);border:1px solid var(--line);border-radius:11px;box-shadow:var(--shadow-lg);min-width:178px;z-index:500;overflow:hidden;display:none}
.lang-menu.open{display:block}
.lang-item{display:flex;align-items:center;gap:10px;padding:10px 14px;cursor:pointer;font-size:13px;font-weight:500;transition:.12s;color:var(--ink)}
.lang-item:hover,.lang-item.aktif{background:var(--bg);color:var(--pri)}
.lang-flag{font-size:16px}

/* === Modal Bantuan === */
.help-modal{display:none;position:fixed;inset:0;z-index:1100;background:rgba(15,23,42,.55);align-items:center;justify-content:center;padding:16px}
.help-modal.show{display:flex}
.help-box{background:var(--card);border-radius:var(--r);width:100%;max-width:860px;max-height:90vh;display:flex;flex-direction:column;box-shadow:var(--shadow-lg);overflow:hidden;animation:mfadein .15s ease}
.help-head{padding:14px 18px;border-bottom:1px solid var(--line);display:flex;align-items:center;gap:10px;flex-shrink:0}
.help-head h2{font-size:15px;font-weight:800;flex:1;color:var(--ink)}
.help-hclose{background:none;border:none;font-size:20px;cursor:pointer;color:var(--ink2);line-height:1;padding:4px 7px;border-radius:7px;font-family:inherit}
.help-hclose:hover{background:var(--line);color:var(--ink)}
.help-body{display:flex;flex:1;overflow:hidden;min-height:0}
.help-nav{width:215px;flex-shrink:0;border-right:1px solid var(--line);overflow-y:auto;padding:8px 0}
.help-nav-grp{font-size:9px;font-weight:800;text-transform:uppercase;letter-spacing:.7px;color:var(--ink3);padding:10px 16px 3px}
.help-topic{display:flex;align-items:center;gap:9px;padding:8px 16px;cursor:pointer;font-size:12.5px;font-weight:600;transition:.12s;color:var(--ink2);border-right:2px solid transparent}
.help-topic:hover{background:var(--bg);color:var(--ink)}
.help-topic.aktif{background:color-mix(in srgb,var(--pri) 8%,var(--card));color:var(--pri);border-right-color:var(--pri)}
.help-topic i{width:16px;text-align:center;font-size:11px;flex-shrink:0}
.help-content{flex:1;overflow-y:auto;padding:24px 26px}
.help-content h3{font-size:16px;font-weight:800;margin-bottom:12px;color:var(--ink)}
.help-content p,.help-content li{font-size:13.5px;color:var(--ink2);line-height:1.8}
.help-content ul{margin:0 0 12px 20px;padding:0}
.help-content li{margin-bottom:4px}
.help-content p{margin-bottom:12px}
.help-tip{background:var(--bg);border:1px solid var(--line);border-left:3px solid var(--pri2);border-radius:8px;padding:11px 14px;margin-bottom:14px;font-size:12.5px;color:var(--ink2);line-height:1.75}
.help-tip strong{color:var(--ink);display:block;margin-bottom:3px}
.help-foot{padding:9px 18px;border-top:1px solid var(--line);display:flex;align-items:center;justify-content:space-between;font-size:11px;color:var(--ink3);flex-shrink:0}
@media(max-width:640px){.help-body{flex-direction:column}.help-nav{width:100%;border-right:none;border-bottom:1px solid var(--line);max-height:130px;overflow-y:auto}}
</style>
<%@ include file="../_lang.jsp" %>
</head>
<body>

<div class="shell" id="shell">

  <!-- ======= TOPBAR ======= -->
  <header class="topbar">
    <button class="hamb" id="hambBtn" aria-label="Menu" onclick="toggleSidebar()">
      <i class="fa-solid fa-bars"></i>
    </button>

    <!-- Brand -->
    <div class="tb-brand">
      <img id="headerLogo" src="<%=rootCtx%><%=safeHtml(logo)%>" alt="Logo"
           onerror="this.src='<%=rootCtx%>/img/logo.png'">
      <div class="tb-brand-txt">
        <b id="headerNama"><%=safeHtml(nama.isEmpty() ? "Sistem Informasi" : nama)%></b>
        <span id="headerMotto"><%=safeHtml(motto)%></span>
      </div>
    </div>

    <!-- Toolbar modul (dari index.zul) -->
    <nav class="tb-modules" id="tbModules">
      <button class="tb-btn" id="tbMenu" onclick="openMenu()">
        <i class="fa-solid fa-list-ul"></i> Menu
      </button>
      <button class="tb-btn" id="tbElearning" style="display:none" onclick="openModul('elearning')">
        <i class="fa-solid fa-book"></i> e-Learning
      </button>
      <button class="tb-btn" id="tbPrestasi" style="display:none" onclick="openModul('prestasi')">
        <i class="fa-solid fa-trophy"></i> Prestasi
      </button>
      <button class="tb-btn" id="tbPustaka" style="display:none" onclick="openModul('pustaka')">
        <i class="fa-solid fa-book-open"></i> Pustaka
      </button>
      <button class="tb-btn" id="tbWorkflow" style="display:none" onclick="openModul('workflow')">
        <i class="fa-solid fa-sitemap"></i> Workflow
      </button>
      <button class="tb-btn" id="tbRepository" style="display:none" onclick="openModul('repository')">
        <i class="fa-solid fa-folder"></i> Repository
      </button>
      <button class="tb-btn" id="tbSpmi" style="display:none" onclick="openModul('spmi')">
        <i class="fa-solid fa-clipboard-check"></i> SPMI
      </button>
      <button class="tb-btn" id="tbKantin" style="display:none" onclick="openModul('kantin')">
        <i class="fa-solid fa-basket-shopping"></i> Toko
      </button>
      <button class="tb-btn" id="tbAkademik" style="display:none" onclick="openModul('akademik')">
        <i class="fa-solid fa-gauge-high"></i> Akademik
      </button>
      <button class="tb-btn" id="tbAdministrasi" style="display:none" onclick="openModul('administrasi')">
        <i class="fa-solid fa-book-bookmark"></i> Administrasi
      </button>
      <button class="tb-btn" id="tbPembayaran" style="display:none" onclick="openModul('pembayaran')">
        <i class="fa-solid fa-credit-card"></i> Pembayaran
      </button>
      <button class="tb-btn" id="tbKeuangan" style="display:none" onclick="openModul('keuangan')">
        <i class="fa-solid fa-money-bill"></i> Keuangan
      </button>
      <button class="tb-btn" id="tbKepegawaian" style="display:none" onclick="openModul('kepegawaian')">
        <i class="fa-solid fa-user-tie"></i> Kepegawaian
      </button>
      <button class="tb-btn" id="tbGaji" style="display:none" onclick="openModul('gaji')">
        <i class="fa-solid fa-money-check-dollar"></i> Gaji
      </button>
      <button class="tb-btn" id="tbFeeder" style="display:none" onclick="openModul('feeder')">
        <i class="fa-solid fa-cloud-arrow-up"></i> Neo Feeder
      </button>
      <button class="tb-btn" id="tbSister" style="display:none" onclick="openModul('sister')">
        <i class="fa-solid fa-cloud-arrow-down"></i> SISTER
      </button>
      <button class="tb-btn" id="tbEmedic" style="display:none" onclick="openModul('emedic')">
        <i class="fa-solid fa-notes-medical"></i> eMedic
      </button>
      <button class="tb-btn" id="tbKoperasi" style="display:none" onclick="openModul('koperasi')">
        <i class="fa-solid fa-handshake"></i> Koperasi
      </button>
      <button class="tb-btn" id="tbAkunting" style="display:none" onclick="openModul('akunting')">
        <i class="fa-solid fa-calculator"></i> Akuntansi
      </button>
      <button class="tb-btn" id="tbPengadaan" style="display:none" onclick="openModul('pengadaan')">
        <i class="fa-solid fa-boxes-stacked"></i> Pengadaan
      </button>
      <button class="tb-btn" id="tbKinerja" style="display:none" onclick="openModul('kinerja')">
        <i class="fa-solid fa-chart-line"></i> Kinerja
      </button>
      <button class="tb-btn" id="tbPresensi" style="display:none" onclick="openModul('presensi')">
        <i class="fa-solid fa-fingerprint"></i> Presensi
      </button>
      <button class="tb-btn" id="tbKalender" style="display:none" onclick="openModul('kalender')">
        <i class="fa-solid fa-calendar-days"></i> Kalender
      </button>
      <button class="tb-btn" id="tbKegiatan" style="display:none" onclick="openModul('kegiatan')">
        <i class="fa-solid fa-calendar-check"></i> Kegiatan
      </button>
      <button class="tb-btn" id="tbAntarJemput" style="display:none" onclick="openModul('anterjemput')">
        <i class="fa-solid fa-bus"></i> Antar Jemput
      </button>
    </nav>

    <!-- Kanan: online users + profil -->
    <div class="tb-right">
      <div class="tb-online" id="onlineBtn" title="Pengguna Online" onclick="lihatOnline()">
        <span class="dot"></span>
        <span id="onlineCount">Online: 1</span>
      </div>
      <button class="tb-icon-btn" title="Notifikasi" onclick="bukaNotifikasi()">
        <i class="fa-regular fa-bell"></i>
        <span class="notif-dot" id="notifDot" style="display:none">0</span>
      </button>
      <!-- Ganti Bahasa -->
      <div class="tb-lang-wrap" id="langWrap">
        <button class="lang-btn" id="langBtn" onclick="toggleLangMenu()" title="Ganti Bahasa">
          <i class="fa-solid fa-globe"></i>
          <span id="langLabel">ID</span>
        </button>
        <div class="lang-menu" id="langMenu">
          <div class="lang-item" id="liID" onclick="setLang('')">
            <span class="lang-flag">🇮🇩</span> Bahasa Indonesia
          </div>
          <div class="lang-item" id="liEN" onclick="setLang('English')">
            <span class="lang-flag">🇬🇧</span> English
          </div>
          <div class="lang-item" id="liAR" onclick="setLang('Arabic')">
            <span class="lang-flag">🇸🇦</span> العربية
          </div>
          <div class="lang-item" id="liZH" onclick="setLang('Mandarin')">
            <span class="lang-flag">🇨🇳</span> 中文 (Mandarin)
          </div>
        </div>
      </div>
      <!-- Bantuan -->
      <button class="tb-icon-btn" title="Bantuan" onclick="bukaHelp()">
        <i class="fa-regular fa-circle-question"></i>
      </button>
      <div class="tb-profile" onclick="bukaProfilMenu(this)">
        <div class="tb-avatar" id="tbAvatar"><%=safeHtml(avatarText)%></div>
        <div class="tb-profile-txt">
          <b id="tbNama"><%=safeHtml(namaLengkap)%></b>
          <span id="tbRole"><%=safeHtml(roleDisplay.isEmpty() ? "Pengguna" : roleDisplay)%></span>
        </div>
      </div>
      <button class="tb-icon-btn" title="Pengaturan" onclick="bukaPengaturan()">
        <i class="fa-solid fa-cog"></i>
      </button>
    </div>
  </header>

  <!-- ======= SIDEBAR (server-side dynamic, mirroring Tbmrole flags dari nav.jsp) ======= -->
  <aside class="sidebar" id="sidebar">
    <nav class="sb-nav" id="sbNav">
<%
  try {
    String _ctx = request.getContextPath();
    Tbmuser _sbU = Common.getCurrentUser(request);
    Tbmrole _sbR = (_sbU != null) ? _sbU.hakAkses() : null;
    boolean _sOk = _sbR != null;
    // Pre-compute group visibility
    boolean _gAkd = _sOk && (_sbR.getDashboard() || _sbR.getElearning() || _sbR.getPresensiKehadiran() || _sbR.getKalenderAkademik());
    boolean _gKeu = _sOk && (_sbR.getPembayaran() || _sbR.getKantin() || _sbR.getDashboardKoperasi() || _sbR.getAkunting() || _sbR.getKeuangan());
    boolean _gSdm = _sOk && (_sbR.getKepegawaian() || _sbR.getTampilkanGaji() || _sbR.getTampilkanSpmi() || _sbR.getKinerja());
    boolean _gLay = _sOk && (_sbR.getAdministrasi() || _sbR.getWorkflow() || _sbR.getInfoKegiatan() || _sbR.getPengadaan() || _sbR.getDasborRepository() || _sbR.getDashboard());
    boolean _gLain = (_sOk && (_sbR.getDasboardAntarJemput() || _sbR.getEmedic())) || Common.getApakahAdminBolehAksesFeeder();
%>
      <div class="sb-group"><%=Common.getBahasaConfig("Navigasi Utama")%></div>
      <a class="sb-item active" href="javascript:void(0)" onclick="tampilkanHome()">
        <span class="ic"><i class="fa-solid fa-house"></i></span>
        <%=Common.getBahasaConfig("Beranda")%>
      </a>

<%  if (_gAkd) { %>
      <div class="sb-group"><%=Common.getBahasaConfig("Akademik")%></div>
<%    if (_sbR.getDashboard()) { %>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('pmb','<%=_ctx%>/pmb')">
        <span class="ic"><i class="fa-solid fa-user-plus"></i></span>
        <%=Common.getBahasaConfig("PMB / PPDB")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('akademik','<%=_ctx%>/baru?p=akademik')">
        <span class="ic"><i class="fa-solid fa-graduation-cap"></i></span>
        <%=Common.getBahasaConfig("Akademik & KRS")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('penilaian','<%=_ctx%>/baru?p=akademik&s=penilaian')">
        <span class="ic"><i class="fa-solid fa-square-poll-vertical"></i></span>
        <%=Common.getBahasaConfig("Penilaian")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
<%    } %>
<%    if (_sbR.getKalenderAkademik() || _sbR.getDashboard()) { %>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('jadwal','<%=_ctx%>/baru?p=kalenderakademik')">
        <span class="ic"><i class="fa-solid fa-calendar-days"></i></span>
        <%=Common.getBahasaConfig("Penjadwalan")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
<%    } %>
<%    if (_sbR.getElearning()) { %>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('lms','<%=_ctx%>/baru?p=elearning')">
        <span class="ic"><i class="fa-solid fa-laptop-code"></i></span>
        <%=Common.getBahasaConfig("e-Learning")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
<%    } %>
<%    if (_sbR.getPresensiKehadiran()) { %>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('kehadiran','<%=_ctx%>/baru?p=presensi')">
        <span class="ic"><i class="fa-solid fa-fingerprint"></i></span>
        <%=Common.getBahasaConfig("Kehadiran")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
<%    } %>
<%  } // end _gAkd %>

<%  if (_gKeu) { %>
      <div class="sb-group"><%=Common.getBahasaConfig("Keuangan")%></div>
<%    if (_sbR.getPembayaran()) { %>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('tagihan','<%=_ctx%>/baru?p=pembayaran')">
        <span class="ic"><i class="fa-solid fa-credit-card"></i></span>
        <%=Common.getBahasaConfig("Tagihan & Bayar")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
<%      if (_sbR.getKeuangan()) { %>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('beasiswa','<%=_ctx%>/baru?p=pembayaran&s=beasiswa')">
        <span class="ic"><i class="fa-solid fa-hand-holding-heart"></i></span>
        <%=Common.getBahasaConfig("Beasiswa")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
<%      } %>
<%    } %>
<%    if (_sbR.getKantin() || _sbR.getDashboardKoperasi()) { %>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('pos','<%=_ctx%>/baru?p=kantin')">
        <span class="ic"><i class="fa-solid fa-wallet"></i></span>
        <%=Common.getBahasaConfig("POS & Koperasi")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
<%    } %>
<%    if (_sbR.getKeuangan()) { %>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('keuangan','<%=_ctx%>/baru?p=keuangan')">
        <span class="ic"><i class="fa-solid fa-coins"></i></span>
        <%=Common.getBahasaConfig("Keuangan")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
<%    } %>
<%    if (_sbR.getAkunting()) { %>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('akuntansi','<%=_ctx%>/baru?p=akuntansi')">
        <span class="ic"><i class="fa-solid fa-calculator"></i></span>
        <%=Common.getBahasaConfig("Akuntansi")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
<%    } %>
<%  } // end _gKeu %>

<%  if (_gSdm) { %>
      <div class="sb-group"><%=Common.getBahasaConfig("SDM & Mutu")%></div>
<%    if (_sbR.getKepegawaian()) { %>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('sdm','<%=_ctx%>/baru?p=kepegawaian')">
        <span class="ic"><i class="fa-solid fa-users-gear"></i></span>
        <%=Common.getBahasaConfig("SDM & Kepegawaian")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
<%    } %>
<%    if (_sbR.getTampilkanGaji()) { %>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('payroll','<%=_ctx%>/baru?p=gaji')">
        <span class="ic"><i class="fa-solid fa-money-check-dollar"></i></span>
        <%=Common.getBahasaConfig("Payroll")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
<%    } %>
<%    if (_sbR.getTampilkanSpmi()) { %>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('spmi','<%=_ctx%>/baru?p=spmi')">
        <span class="ic"><i class="fa-solid fa-clipboard-check"></i></span>
        <%=Common.getBahasaConfig("SPMI & AMI")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
<%    } %>
<%    if (_sbR.getKinerja()) { %>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('kinerja','<%=_ctx%>/baru?p=kinerja')">
        <span class="ic"><i class="fa-solid fa-award"></i></span>
        <%=Common.getBahasaConfig("Akreditasi & Kinerja")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
<%    } %>
<%  } // end _gSdm %>

<%  if (_gLay) { %>
      <div class="sb-group"><%=Common.getBahasaConfig("Layanan")%></div>
<%    if (_sbR.getAdministrasi()) { %>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('surat','<%=_ctx%>/baru?p=suratmenyurat')">
        <span class="ic"><i class="fa-solid fa-envelope-open-text"></i></span>
        <%=Common.getBahasaConfig("Persuratan")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
<%    } %>
<%    if (_sbR.getWorkflow()) { %>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('sop','<%=_ctx%>/baru?p=pagesmastersoppengajuananda')">
        <span class="ic"><i class="fa-solid fa-file-circle-check"></i></span>
        <%=Common.getBahasaConfig("Pengajuan Anda")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
<%    } %>
<%    if (_sbR.getPengadaan()) { %>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('pengadaan','<%=_ctx%>/baru?p=pengadaan')">
        <span class="ic"><i class="fa-solid fa-boxes-stacked"></i></span>
        <%=Common.getBahasaConfig("Pengadaan")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
<%    } %>
<%    if (_sbR.getInfoKegiatan()) { %>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('kegiatan','<%=_ctx%>/baru?p=infokegiatan')">
        <span class="ic"><i class="fa-solid fa-bullhorn"></i></span>
        <%=Common.getBahasaConfig("Info Kegiatan")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
<%    } %>
<%    if (_sbR.getDasborRepository()) { %>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('repo','<%=_ctx%>/baru?p=repository')">
        <span class="ic"><i class="fa-solid fa-database"></i></span>
        <%=Common.getBahasaConfig("Repository")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
<%    } %>
<%    if (_sbR.getDashboard()) { %>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('alumni','<%=_ctx%>/baru?p=akademik&s=alumni')">
        <span class="ic"><i class="fa-solid fa-people-roof"></i></span>
        <%=Common.getBahasaConfig("Alumni")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
<%    } %>
<%  } // end _gLay %>

<%  if (_gLain) { %>
      <div class="sb-group"><%=Common.getBahasaConfig("Lainnya")%></div>
<%    if (Common.getApakahAdminBolehAksesFeeder()) { %>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('feeder','<%=_ctx%>/baru?p=feeder')">
        <span class="ic"><i class="fa-solid fa-plug-circle-bolt"></i></span>
        <%=Common.getBahasaConfig("Neo Feeder")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
<%    } %>
<%    if (_sOk && _sbR.getDasboardAntarJemput()) { %>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('jemput','<%=_ctx%>/baru?p=antarjemput')">
        <span class="ic"><i class="fa-solid fa-van-shuttle"></i></span>
        <%=Common.getBahasaConfig("Antar Jemput")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
<%    } %>
<%    if (_sOk && _sbR.getEmedic()) { %>
      <a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar('emedic','<%=_ctx%>/baru?p=emedic')">
        <span class="ic"><i class="fa-solid fa-heart-pulse"></i></span>
        <%=Common.getBahasaConfig("e-Medic")%><span class="chev"><i class="fa-solid fa-chevron-right"></i></span>
      </a>
<%    } %>
<%  } // end _gLain %>
<%  } catch (Exception _sbEx) { /* sidebar generation error - silent fallback */ } %>
    </nav>
  </aside>

  <!-- ======= MAIN AREA ======= -->
  <main class="main" id="mainArea">

    <!-- Tab bar (seperti di index.zul tabbox) -->
    <div class="tab-bar" id="tabBar">
      <div class="tab-item active tab-home" id="tab_home" onclick="switchTab('home')">
        <i class="fa-solid fa-house" style="color:var(--pri);font-size:13px"></i>
        Home
      </div>
      <!-- Tab tambahan di-append oleh JS -->
    </div>

    <!-- Content area: home panel + iframe panels -->
    <div class="content-area" id="contentArea">

      <!-- Home Panel -->
      <div class="home-panel" id="panel_home" style="display:block" onscroll="onHomeScroll(this)">

        <!-- Hero -->
        <div class="home-hero">
          <h2 id="heroNama">Selamat <%=sapaanWaktu()%>, <%=safeHtml(namaLengkap.isEmpty() ? "Pengguna" : namaLengkap.split("\\s+")[0])%>!</h2>
          <p id="heroMotto"><%=safeHtml(motto.isEmpty() ? "Sistem Informasi Akademik Terpadu" : motto)%></p>
          <div class="pills">
            <span class="pill"><i class="fa-solid fa-layer-group"></i> 53+ Modul Terintegrasi</span>
            <span class="pill"><i class="fa-solid fa-plug"></i> Neo Feeder &amp; SISTER</span>
            <span class="pill"><i class="fa-solid fa-mobile-screen-button"></i> Web, Android &amp; iOS</span>
            <%if(!roleDisplay.isEmpty()){%>
            <span class="pill"><i class="fa-solid fa-id-badge"></i> <%=safeHtml(roleDisplay)%></span>
            <%}%>
          </div>
        </div>

        <!-- Stat cards -->
        <div class="stats-row">
          <div class="stat-card">
            <div class="s-top">
              <div class="s-ic" style="background:linear-gradient(135deg,#4f46e5,#6d28d9)"><i class="fa-solid fa-layer-group"></i></div>
            </div>
            <div class="s-val" id="statModul">53</div>
            <div class="s-lbl">Modul Terintegrasi</div>
          </div>
          <div class="stat-card">
            <div class="s-top">
              <div class="s-ic" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-plug"></i></div>
            </div>
            <div class="s-val">2</div>
            <div class="s-lbl">Integrasi Nasional</div>
          </div>
          <div class="stat-card">
            <div class="s-top">
              <div class="s-ic" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-mobile-screen-button"></i></div>
            </div>
            <div class="s-val">3</div>
            <div class="s-lbl">Kanal Akses</div>
          </div>
          <div class="stat-card">
            <div class="s-top">
              <div class="s-ic" style="background:linear-gradient(135deg,#7c3aed,#6d28d9)"><i class="fa-solid fa-calendar-check"></i></div>
            </div>
            <div class="s-val">2 Thn</div>
            <div class="s-lbl">Roadmap Implementasi</div>
          </div>
        </div>

        <!-- Layout 2 kolom: Papan Pengumuman+Kalender (kiri) + Profil Pengguna (kanan) -->
        <div class="home-2col">

          <!-- Kolom kiri: Papan Pengumuman + Kalender Akademik -->
          <div class="left-col">

            <!-- ── Papan Pengumuman ─────────────────────────────────── -->
            <div class="ann-panel" id="annPanel">
              <div class="ann-header">
                <div class="ann-title">
                  <i class="fa-solid fa-bullhorn" style="color:var(--pri2);font-size:15px"></i>
                  <h3>Papan Pengumuman</h3>
                  <span class="badge badge-gray" id="annCount" style="margin-left:auto">…</span>
                </div>
                <div class="ann-search">
                  <input type="text" id="annSearchInput" placeholder="Cari pengumuman…" oninput="filterAnn(this.value)">
                  <button onclick="filterAnn(document.getElementById('annSearchInput').value)" title="Cari">
                    <i class="fa-solid fa-magnifying-glass"></i>
                  </button>
                </div>
              </div>
              <div class="ann-list" id="annList">
                <div class="ann-empty"><i class="fa-solid fa-spinner fa-spin"></i> Memuat pengumuman…</div>
              </div>
              <div class="ann-pager" id="annPager" style="display:none">
                <button class="ann-pb" id="annPrev" onclick="annMovePage(-1)">← Sebelumnya</button>
                <span id="annPageInfo" style="font-size:11px;color:var(--ink2)">1 / 1</span>
                <button class="ann-pb" id="annNext" onclick="annMovePage(1)">Berikutnya →</button>
              </div>
            </div>

            <!-- ── Kalender Akademik ───────────────────────────────── -->
            <div class="kal-panel" id="kalPanel">
              <div class="kal-header">
                <div class="kal-eyebrow">AGENDA KAMPUS</div>
                <div class="kal-title">
                  <i class="fa-solid fa-calendar-days" style="color:#d97706;font-size:14px"></i>
                  <h3>Kalender Akademik</h3>
                  <span class="badge badge-gray" id="kalCount" style="margin-left:auto">…</span>
                </div>
                <p class="kal-subtitle">Semua jadwal kegiatan penting kampus – pilih tahun/semester untuk melihat agenda yang diinginkan.</p>
                <div class="kal-filters">
                  <select id="kalTahun" onchange="filterKal()">
                    <option value="">Semua Tahun</option>
                  </select>
                  <select id="kalSemester" onchange="filterKal()">
                    <option value="">Semua Semester</option>
                    <option value="Ganjil">Ganjil</option>
                    <option value="Genap">Genap</option>
                  </select>
                </div>
              </div>
              <div class="kal-cards" id="kalList">
                <div class="ann-empty" style="grid-column:1/-1"><i class="fa-solid fa-spinner fa-spin"></i> Memuat kalender…</div>
              </div>
            </div>

          </div><!-- .left-col -->

          <!-- Kolom kanan: Panel Profil Pengguna -->
          <div class="right-col">
            <jsp:include page="panel_profil.jsp" />
          </div>

        </div><!-- .home-2col -->

        <!-- ── Peta Modul (full width di bawah) ─────────────────────── -->
        <div id="petaModul" style="margin-top:22px"></div>

      </div>

      <!-- Iframe panels untuk modul -->
      <div id="iframePanels"></div>

    </div>
  </main>

</div><!-- .shell -->

<!-- Sidebar backdrop -->
<div class="sb-backdrop" id="sbBackdrop" onclick="closeSidebar()"></div>

<!-- Modal Detail Pengumuman -->
<div class="ann-modal" id="annModal" onclick="if(event.target===this)closeAnnModal()">
  <div class="ann-mbox">
    <div class="ann-mhead">
      <div class="ann-chips" id="annMChips"></div>
      <h3 id="annMTitle"></h3>
    </div>
    <div class="ann-mbody" id="annMBody"></div>
    <div class="ann-mfoot">
      <button class="ann-mbtn s" onclick="closeAnnModal()">Tutup</button>
    </div>
  </div>
</div>

<!-- Modal Bantuan -->
<div class="help-modal" id="helpModal" onclick="if(event.target===this)tutupHelp()">
  <div class="help-box">
    <div class="help-head">
      <i class="fa-regular fa-circle-question" style="color:var(--pri2);font-size:16px"></i>
      <h2>Bantuan &amp; Panduan</h2>
      <button class="help-hclose" onclick="tutupHelp()" title="Tutup">&times;</button>
    </div>
    <div class="help-body">
      <div class="help-nav" id="helpNav"></div>
      <div class="help-content" id="helpContent"></div>
    </div>
    <div class="help-foot">
      <span>Tekan <kbd>Esc</kbd> untuk menutup</span>
      <span>Versi eCampus Baru2 &mdash; Powered by AIS</span>
    </div>
  </div>
</div>

<!-- Floating actions -->
<div class="float-actions">
  <%if(!waCs.isEmpty()){%>
  <button class="float-btn float-wa" id="waBtn" title="Hubungi CS via WhatsApp"
    onclick="window.open('https://wa.me/<%=safeHtml(waCs.replaceAll("[^0-9+]",""))%>','_blank')">
    <i class="fa-brands fa-whatsapp"></i>
  </button>
  <%}%>
  <button class="float-btn float-top" id="backTopBtn" title="Kembali ke Atas" onclick="scrollToTop()">
    <i class="fa-solid fa-chevron-up"></i>
  </button>
</div>

<script>
/* ============================================================
   BARU2 — Halaman Utama (index.jsp)
   Root context path dari server
   ============================================================ */
var ROOT = '<%=safeJs(rootCtx)%>';
var API_BASE = ROOT + '/baru2?modul=utama&svc=';
var MAIN_URL = ROOT + '/main';

/* === Peta Modul (dari mockup index.html) === */
var GROUPS = [
  {g:'Penerimaan, Setup & Kepemimpinan', c:'#4f46e5', m:[
    {t:'PMB / PPDB & RPL', d:'Pendaftaran mahasiswa baru, jalur RPL, parameter & form custom.', i:'fa-user-plus', url: ROOT+'/pmb'},
    {t:'Setup & Utility', d:'Konfigurasi institusi, parameter sistem, hak akses & utilitas.', i:'fa-sliders', url: MAIN_URL},
    {t:'Dasbor Pimpinan', d:'Dasbor eksekutif seluruh aktivitas & modul kampus real-time.', i:'fa-gauge-high', url: MAIN_URL},
    {t:'Pendataan (Master Data)', d:'Seluruh data master perguruan tinggi: mahasiswa, dosen, prodi.', i:'fa-database', url: MAIN_URL}
  ]},
  {g:'Akademik & Pembelajaran', c:'#0ea5e9', m:[
    {t:'Penjadwalan', d:'Penjadwalan drag-and-drop, pencegahan jam bentrok otomatis.', i:'fa-calendar-days', url: MAIN_URL},
    {t:'Akademik Terpadu', d:'KRS, penilaian, kurikulum OBE & non-OBE, KKN, PKL, magang.', i:'fa-graduation-cap', url: MAIN_URL},
    {t:'Kurikulum OBE', d:'CPL, CPMK, Sub-CPMK, pemetaan, RPS & asesmen capaian.', i:'fa-diagram-project', url: MAIN_URL},
    {t:'LMS (e-Learning)', d:'Tugas, ujian, anti-curang, materi, video/audio, absensi online.', i:'fa-laptop-code', url: MAIN_URL},
    {t:'Manajemen Kehadiran', d:'Kehadiran dosen/mahasiswa/tendik + dasbor & statistik.', i:'fa-fingerprint', url: MAIN_URL},
    {t:'Bimbingan', d:'TA/Skripsi, KKN, PKL, akademik — jadwal, nilai, statistik.', i:'fa-user-graduate', url: MAIN_URL},
    {t:'PKL, KKN & Magang MBKM', d:'Pendaftaran, logbook harian, penilaian & konversi SKS.', i:'fa-briefcase', url: MAIN_URL},
    {t:'Pengujian & Sidang', d:'Sidang TA/Skripsi: jadwal, penguji, nilai, dasbor.', i:'fa-gavel', url: MAIN_URL},
    {t:'Penilaian Terintegrasi', d:'Penilaian otomatis terhubung LMS & feeder.', i:'fa-square-poll-vertical', url: MAIN_URL},
    {t:'Sertifikat & Pelatihan', d:'Sertifikat digital ber-QR, terintegrasi penilaian & LMS.', i:'fa-certificate', url: MAIN_URL}
  ]},
  {g:'Integrasi Pelaporan Nasional', c:'#0d9488', m:[
    {t:'Neo Feeder', d:'Sinkronisasi dua arah PDDikti — perbandingan & samakan data.', i:'fa-cloud-arrow-up', url: MAIN_URL},
    {t:'Integrasi Sister', d:'Sinkronisasi data SDM dosen ke SISTER Kemdikbud.', i:'fa-cloud-arrow-down', url: MAIN_URL}
  ]},
  {g:'Keuangan, Usaha & Akuntansi', c:'#10b981', m:[
    {t:'Finance & e-Wallet Mahasiswa', d:'Tagihan bulanan/insidentil, diskon, tabungan, multi-channel.', i:'fa-credit-card', url: MAIN_URL},
    {t:'POS & Koperasi', d:'e-Wallet kampus, POS kantin/unit usaha, koperasi.', i:'fa-wallet', url: MAIN_URL},
    {t:'Beasiswa', d:'KIP-K, Yayasan, prestasi & CSR — seleksi berjenjang.', i:'fa-hand-holding-heart', url: MAIN_URL},
    {t:'Akuntansi Yayasan', d:'Finance kampus terintegrasi akuntansi Yayasan.', i:'fa-calculator', url: MAIN_URL},
    {t:'Anggaran & Realisasi', d:'Kas besar/kecil, uang muka, LPJ, PR/PO/BAST, termin.', i:'fa-money-bill-trend-up', url: MAIN_URL},
    {t:'Pengadaan & Logistik', d:'Aset & inventaris, peminjaman, vendor & seleksi.', i:'fa-boxes-stacked', url: MAIN_URL},
    {t:'Pembayaran Vendor (DPC)', d:'Pengajuan transfer & pembayaran vendor via Yayasan.', i:'fa-file-invoice-dollar', url: MAIN_URL}
  ]},
  {g:'SDM, Kinerja & Pengembangan', c:'#7c3aed', m:[
    {t:'SDM & Kepegawaian', d:'Registrasi dosen/tendik, data kepegawaian.', i:'fa-users-gear', url: MAIN_URL},
    {t:'Payroll', d:'Penggajian terintegrasi SDM & pembayaran Yayasan.', i:'fa-money-check-dollar', url: MAIN_URL},
    {t:'Kinerja (SKP & KPI)', d:'Sasaran kinerja dosen/pegawai terhubung KPI.', i:'fa-chart-line', url: MAIN_URL},
    {t:'Angket & Survey (EDOM)', d:'Evaluasi dosen, survey sarpras, layanan & fasilitas.', i:'fa-square-poll-horizontal', url: MAIN_URL},
    {t:'Kursus Internal', d:'Platform kursus subscribe internal kampus.', i:'fa-chalkboard-user', url: MAIN_URL},
    {t:'Prestasi', d:'Prestasi dosen/mahasiswa/tendik, hak cipta, sertifikat.', i:'fa-trophy', url: MAIN_URL},
    {t:'Kedisiplinan', d:'Kedisiplinan dosen/mahasiswa/tendik + punishment.', i:'fa-scale-balanced', url: MAIN_URL}
  ]},
  {g:'Penelitian, Mutu & Dokumen', c:'#e11d48', m:[
    {t:'Penelitian & PPM', d:'Penelitian, pengabdian, jurnal, karya ilmiah, BKD.', i:'fa-flask', url: MAIN_URL},
    {t:'Repository Institusi', d:'Repository mandiri, sinkron otomatis.', i:'fa-server', url: MAIN_URL},
    {t:'Perpustakaan & eLibrary', d:'Sirkulasi, katalog, barcoding, baca online.', i:'fa-book-open', url: MAIN_URL},
    {t:'Manajemen Dokumen', d:'Dokumen penting kampus termasuk akreditasi.', i:'fa-folder-tree', url: MAIN_URL},
    {t:'Akreditasi (Multi-LAM)', d:'BAN-PT, LAMEMBA, LAM-INFOKOM, LAMDIK, LAM Teknik.', i:'fa-award', url: MAIN_URL},
    {t:'SPMI & AMI', d:'Penjaminan Mutu Internal + Audit Mutu Internal.', i:'fa-clipboard-check', url: MAIN_URL},
    {t:'SPI', d:'Satuan Pengawasan Internal terintegrasi Yayasan.', i:'fa-user-shield', url: MAIN_URL}
  ]},
  {g:'Layanan, Persuratan & Workflow', c:'#f59e0b', m:[
    {t:'Persuratan Mahasiswa/Dosen', d:'Surat izin penelitian, keterangan lulus, dll.', i:'fa-envelope-open-text', url: MAIN_URL},
    {t:'SOP & Workflow Fleksibel', d:'Alur proses custom mendukung seluruh modul pengajuan.', i:'fa-sitemap', url: MAIN_URL},
    {t:'Kelulusan & Wisuda', d:'Seleksi & pengajuan daftar wisuda mahasiswa.', i:'fa-user-graduate', url: MAIN_URL},
    {t:'Alumni & Tracer Study', d:'Tracer study + kuesioner alumni & atasan alumni.', i:'fa-people-roof', url: MAIN_URL},
    {t:'Pengaduan Terpadu', d:'E-ticketing pengaduan + tindak lanjut & dasbor.', i:'fa-headset', url: MAIN_URL}
  ]},
  {g:'Kecerdasan Buatan & Otomasi', c:'#7c3aed', m:[
    {t:'Asisten AI Terintegrasi', d:'Generate kurikulum OBE, buat & koreksi soal, analisis modul.', i:'fa-wand-magic-sparkles', url: MAIN_URL},
    {t:'Notifikasi WhatsApp', d:'Broadcast WA resmi, template pesan, otomasi pemicu & log.', i:'fa-comment-dots', url: MAIN_URL}
  ]},
  {g:'Fasilitas Kampus', c:'#0891b2', m:[
    {t:'Anjungan Informasi', d:'Kios/anjungan layanan informasi mandiri kampus.', i:'fa-desktop', url: MAIN_URL},
    {t:'Kunjungan Perpustakaan', d:'Pencatatan & statistik kunjungan perpustakaan.', i:'fa-door-open', url: MAIN_URL},
    {t:'SIM Klinik / Rumah Sakit', d:'SIM-RS/klinik bila kampus memiliki Fakultas Kedokteran.', i:'fa-hospital', url: MAIN_URL},
    {t:'eMedic & SIRS', d:'RME, klaim JKN via VClaim, SATUSEHAT FHIR & SIRS.', i:'fa-notes-medical', url: MAIN_URL}
  ]}
];

/* === Tabs State === */
var tabs = [{id:'home', label:'Home', icon:'fa-house', url: null, type:'home'}];
var activeTab = 'home';

/* === Render Peta Modul === */
function renderPetaModul() {
  var host = document.getElementById('petaModul');
  if (!host) return;
  host.innerHTML = '';
  GROUPS.forEach(function(G) {
    var sec = document.createElement('div');
    sec.className = 'mods-section';
    sec.innerHTML = '<div class="mods-head"><span class="accent-bar" style="background:'+G.c+'"></span><h3>'+G.g+'</h3><span class="badge badge-gray">'+G.m.length+' modul</span></div>';
    var grid = document.createElement('div');
    grid.className = 'mod-grid';
    G.m.forEach(function(M) {
      var card = document.createElement('div');
      card.className = 'mod-card';
      card.onclick = function() { bukaModul(M.t, M.url || MAIN_URL); };
      card.innerHTML = '<div class="m-ic" style="background:linear-gradient(135deg,'+G.c+','+G.c+'cc)"><i class="fa-solid '+M.i+'"></i></div>'
        +'<div><h4>'+M.t+'</h4><p>'+M.d+'</p><span class="m-tag"><i class="fa-solid fa-arrow-right" style="font-size:9px"></i> Buka</span></div>';
      grid.appendChild(card);
    });
    sec.appendChild(grid);
    host.appendChild(sec);
  });
}

/* === Sidebar Nav === */
var SB_NAV = [
  {group: null, items:[{id:'home', label:'Beranda', ic:'fa-house', action:'home'}]},
  {group:'Akademik', items:[
    {id:'pmb', label:'PMB / PPDB', ic:'fa-user-plus', url: ROOT+'/pmb'},
    {id:'akademik', label:'Akademik & KRS', ic:'fa-graduation-cap', url: MAIN_URL},
    {id:'jadwal', label:'Penjadwalan', ic:'fa-calendar-days', url: MAIN_URL},
    {id:'lms', label:'e-Learning', ic:'fa-laptop-code', url: MAIN_URL},
    {id:'penilaian', label:'Penilaian', ic:'fa-square-poll-vertical', url: MAIN_URL},
    {id:'kehadiran', label:'Kehadiran', ic:'fa-fingerprint', url: MAIN_URL}
  ]},
  {group:'Keuangan', items:[
    {id:'keuangan', label:'Tagihan & Bayar', ic:'fa-credit-card', url: MAIN_URL},
    {id:'ewallet', label:'POS & Koperasi', ic:'fa-wallet', url: MAIN_URL},
    {id:'beasiswa', label:'Beasiswa', ic:'fa-hand-holding-heart', url: MAIN_URL},
    {id:'akuntansi', label:'Akuntansi', ic:'fa-calculator', url: MAIN_URL}
  ]},
  {group:'SDM & Mutu', items:[
    {id:'sdm', label:'SDM & Kepegawaian', ic:'fa-users-gear', url: MAIN_URL},
    {id:'payroll', label:'Payroll', ic:'fa-money-check-dollar', url: MAIN_URL},
    {id:'spmi', label:'SPMI & AMI', ic:'fa-clipboard-check', url: MAIN_URL},
    {id:'akreditasi', label:'Akreditasi', ic:'fa-award', url: MAIN_URL}
  ]},
  {group:'Layanan', items:[
    {id:'persuratan', label:'Persuratan', ic:'fa-envelope-open-text', url: MAIN_URL},
    {id:'alumni', label:'Alumni', ic:'fa-people-roof', url: MAIN_URL},
    {id:'pengaduan', label:'Pengaduan', ic:'fa-headset', url: MAIN_URL}
  ]}
];

function renderSidebar() {
  var cont = document.getElementById('dynamicMenuContainer');
  if (!cont) return;
  var html = '';
  SB_NAV.forEach(function(sec) {
    if (sec.group) html += '<div class="sb-group">'+sec.group+'</div>';
    sec.items.forEach(function(it) {
      if (it.action === 'home') return; // sudah ada hardcoded di atas
      html += '<a class="sb-item" href="javascript:void(0)" onclick="bukaModulSidebar(\''+it.id+'\', \''+escJs(it.url || MAIN_URL)+'\')">'
           +'<span class="ic"><i class="fa-solid '+it.ic+'"></i></span>'+escHtml(it.label)+'<span class="chev"><i class="fa-solid fa-chevron-right"></i></span>'
           +'</a>';
    });
  });
  cont.innerHTML = html;
}

/* === Tab Management === */
function addTab(id, label, url) {
  for (var i = 0; i < tabs.length; i++) {
    if (tabs[i].id === id) { switchTab(id); return; }
  }
  tabs.push({id: id, label: label, url: url, type: 'iframe'});
  renderTabBar();
  switchTab(id);
}

function removeTab(id, e) {
  if (e) e.stopPropagation();
  tabs = tabs.filter(function(t) { return t.id !== id; });
  var panel = document.getElementById('panel_' + id);
  if (panel) panel.parentNode.removeChild(panel);
  renderTabBar();
  if (activeTab === id) {
    switchTab(tabs[tabs.length - 1].id);
  }
}

function switchTab(id) {
  activeTab = id;
  // Sembunyikan semua panel
  var panels = document.querySelectorAll('.home-panel, .iframe-pane');
  panels.forEach(function(p) { p.style.display = 'none'; });
  // Tampilkan panel aktif
  var target = document.getElementById('panel_' + id);
  if (target) target.style.display = id === 'home' ? 'block' : 'block';
  renderTabBar();
  // Update sidebar active
  document.querySelectorAll('.sb-item').forEach(function(el) { el.classList.remove('active'); });
  if (id === 'home') {
    var homeItem = document.querySelector('.sb-item[onclick*="tampilkanHome"]');
    if (homeItem) homeItem.classList.add('active');
  }
}

function renderTabBar() {
  var bar = document.getElementById('tabBar');
  if (!bar) return;
  bar.innerHTML = '';
  tabs.forEach(function(t) {
    var div = document.createElement('div');
    div.className = 'tab-item' + (t.id === activeTab ? ' active' : '') + (t.type === 'home' ? ' tab-home' : '');
    div.onclick = function() { switchTab(t.id); };
    var ico = t.type === 'home' ? '<i class="fa-solid fa-house"></i>' : '<i class="fa-solid fa-square-arrow-up-right" style="color:var(--ink3);font-size:11px"></i>';
    var closeBtn = t.type !== 'home' ? '<span class="tab-close" onclick="removeTab(\''+t.id+'\', event)"><i class="fa-solid fa-xmark"></i></span>' : '';
    div.innerHTML = ico + ' ' + escHtml(t.label) + closeBtn;
    bar.appendChild(div);
  });
}

/* === Buka Modul di Tab (iframe) === */
function bukaModul(label, url) {
  var id = 'mod_' + label.replace(/[^a-zA-Z0-9]/g, '_');
  // Cek apakah sudah ada tab dengan URL yang sama
  for (var i = 0; i < tabs.length; i++) {
    if (tabs[i].url === url && tabs[i].type === 'iframe') {
      switchTab(tabs[i].id);
      return;
    }
  }
  addTab(id, label, url);
  createIframePanel(id, url);
}

function createIframePanel(id, url) {
  var panels = document.getElementById('iframePanels');
  if (!panels) return;
  // Hapus panel lama jika ada
  var old = document.getElementById('panel_' + id);
  if (old) old.parentNode.removeChild(old);
  var iframe = document.createElement('iframe');
  iframe.id = 'panel_' + id;
  iframe.src = url;
  iframe.className = 'iframe-pane';
  iframe.style.cssText = 'width:100%;height:100%;border:none;display:block';
  panels.appendChild(iframe);
  // Loading bar
  showLoading();
  iframe.onload = function() { hideLoading(); };
}

function bukaModulSidebar(id, url) {
  closeSidebar();
  bukaModul(id, url);
}

function openModul(id) {
  bukaModul(id, ROOT + '/baru2?modul=' + id);
}

function openMenu() {
  bukaModul('menu', MAIN_URL + '?hak_akses=true');
}

function tampilkanHome() {
  switchTab('home');
  closeSidebar();
}

/* === Sidebar Toggle === */
function toggleSidebar() {
  var sb = document.getElementById('sidebar');
  var bd = document.getElementById('sbBackdrop');
  sb.classList.toggle('open');
  bd.classList.toggle('show');
}
function closeSidebar() {
  var sb = document.getElementById('sidebar');
  var bd = document.getElementById('sbBackdrop');
  sb.classList.remove('open');
  bd.classList.remove('show');
}

/* === Loading Bar (NProgress) === */
function showLoading() {
  if (typeof NProgress !== 'undefined') NProgress.start();
}
function hideLoading() {
  if (typeof NProgress !== 'undefined') NProgress.done();
}

/* === Back to Top === */
function onHomeScroll(el) {
  var btn = document.getElementById('backTopBtn');
  if (btn) btn.classList.toggle('show', el.scrollTop > 300);
}
function scrollToTop() {
  var panel = document.getElementById('panel_home');
  if (panel) panel.scrollTop = 0;
}

/* === Aksi Tombol === */
function lihatOnline() { /* Buka popup pengguna online */ }
function bukaNotifikasi() { bukaModul('notifikasi', MAIN_URL + '?notif=true'); }
function bukaPengaturan() { bukaModul('pengaturan', ROOT + '/baru2?modul=pengaturan'); }
function bukaProfilMenu() { bukaModul('profil', ROOT + '/baru2?modul=profil'); }

/* === Util === */
function escHtml(s) { return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }
function escJs(s) { return String(s).replace(/\\/g,'\\\\').replace(/'/g,"\\'"); }

/* ============================================================
   PAPAN PENGUMUMAN AKADEMIK
   Replikasi MainAction.appendModernPengumumanPanel() dan
   renderModernPengumumanPage() dalam JS murni (tanpa ZK).
   Data dimuat via svc=pengumuman, difilter & dipaginasi client-side.
   ============================================================ */
var ANN_ALL      = [];   // semua data dari server
var ANN_FILTERED = [];   // setelah filter pencarian
var ANN_PAGE     = 0;
var ANN_PAGE_SIZE = 5;

function loadPengumuman() {
  var xhr = new XMLHttpRequest();
  xhr.open('GET', ROOT + '/baru2?modul=utama&svc=pengumuman', true);
  xhr.onload = function() {
    if (xhr.status === 200) {
      try {
        var r = JSON.parse(xhr.responseText);
        if (!r.ok && r.msg) { annErr(r.msg); return; }
        ANN_ALL = Array.isArray(r.data) ? r.data : [];
        ANN_FILTERED = ANN_ALL.slice();
        ANN_PAGE = 0;
        var el = document.getElementById('annCount');
        if (el) el.textContent = ANN_ALL.length;
        renderAnn();
      } catch(e) {
        console.error('[pengumuman] parse error:', e.message, '\nResponse:', xhr.responseText.slice(0,400));
        annErr('Gagal memuat: ' + (e.message || 'respons tidak valid'));
      }
    } else { annErr('Server error ' + xhr.status); }
  };
  xhr.onerror = function() { annErr('Tidak dapat terhubung ke server.'); };
  xhr.send();
}

function annErr(msg) {
  var el = document.getElementById('annList');
  if (el) el.innerHTML = '<div class="ann-empty"><i class="fa-solid fa-triangle-exclamation"></i> ' + escHtml(msg) + '</div>';
  var p = document.getElementById('annPager');
  if (p) p.style.display = 'none';
}

function filterAnn(q) {
  q = (q || '').toLowerCase().trim();
  if (!q) {
    ANN_FILTERED = ANN_ALL.slice();
  } else {
    ANN_FILTERED = ANN_ALL.filter(function(d) {
      return (d.judul       || '').toLowerCase().indexOf(q) >= 0
          || (d.kategoriNama|| '').toLowerCase().indexOf(q) >= 0
          || (d.preview     || '').toLowerCase().indexOf(q) >= 0
          || (d.tanggalStr  || '').toLowerCase().indexOf(q) >= 0
          || (d.diperuntukkan||'').toLowerCase().indexOf(q) >= 0;
    });
  }
  ANN_PAGE = 0;
  renderAnn();
}

function annMovePage(dir) {
  var total = Math.max(1, Math.ceil(ANN_FILTERED.length / ANN_PAGE_SIZE));
  ANN_PAGE = Math.max(0, Math.min(total - 1, ANN_PAGE + dir));
  renderAnn();
}

/* Urutan: utama (nomorUrut ≤ 1) dulu, lalu per kategori nomorUrut ASC, tanggal DESC */
function annSort(arr) {
  return arr.slice().sort(function(a, b) {
    var au = a.utama ? 0 : 1, bu = b.utama ? 0 : 1;
    if (au !== bu) return au - bu;
    var ku = (a.kategoriNomorUrut || 99) - (b.kategoriNomorUrut || 99);
    if (ku !== 0) return ku;
    return (b.tanggalTs || 0) - (a.tanggalTs || 0);
  });
}

function annToneClass(d) {
  if (d.utama) return 'cp';
  var id = d.kategoriId || 0;
  return 't' + ((id % 5) + 1);
}

function renderAnn() {
  var list   = document.getElementById('annList');
  var pager  = document.getElementById('annPager');
  if (!list) return;

  if (!ANN_FILTERED.length) {
    list.innerHTML = '<div class="ann-empty"><i class="fa-regular fa-folder-open"></i><br>Tidak ada pengumuman.</div>';
    if (pager) pager.style.display = 'none';
    return;
  }

  var sorted    = annSort(ANN_FILTERED);
  var totalPage = Math.ceil(sorted.length / ANN_PAGE_SIZE);
  var start     = ANN_PAGE * ANN_PAGE_SIZE;
  var page      = sorted.slice(start, start + ANN_PAGE_SIZE);

  /* Kelompokkan halaman ini: Prioritas Utama dulu, lalu per kategori */
  var groups = [], groupMap = {};
  page.forEach(function(d) {
    var key = d.utama ? '__UTAMA__' : (d.kategoriNama || 'Pengumuman');
    if (!groupMap[key]) { groupMap[key] = []; groups.push(key); }
    groupMap[key].push(d);
  });

  var html = '';
  groups.forEach(function(key) {
    var label = key === '__UTAMA__' ? 'Prioritas Utama' : key;
    html += '<div class="ann-group-title">' + escHtml(label) + '</div>';
    groupMap[key].forEach(function(d) {
      var tone = annToneClass(d);
      html += '<div class="ann-card' + (d.utama ? ' utama' : '') + '" onclick="annBuka(' + d.id + ')">';
      html += '<div class="ann-chips">';
      if (d.utama)         html += '<span class="ann-chip cp">⭐ Prioritas Utama</span>';
      else if(d.kategoriNama) html += '<span class="ann-chip ' + tone + '">' + escHtml(d.kategoriNama) + '</span>';
      if (d.tanggalStr)    html += '<span class="ann-chip cd"><i class="fa-regular fa-calendar"></i> ' + escHtml(d.tanggalStr) + '</span>';
      if (d.diperuntukkan) html += '<span class="ann-chip ca">' + escHtml(d.diperuntukkan.replace('Untuk ','')) + '</span>';
      html += '</div>';
      html += '<h4>' + escHtml(d.judul) + '</h4>';
      if (d.preview) html += '<p>' + escHtml(d.preview) + '</p>';
      html += '</div>';
    });
  });

  list.innerHTML = html;

  /* Pager */
  if (pager) pager.style.display = totalPage > 1 ? '' : 'none';
  var pi   = document.getElementById('annPageInfo');
  var prev = document.getElementById('annPrev');
  var next = document.getElementById('annNext');
  if (pi)   pi.textContent    = (ANN_PAGE + 1) + ' / ' + totalPage;
  if (prev) prev.disabled     = ANN_PAGE === 0;
  if (next) next.disabled     = ANN_PAGE >= totalPage - 1;
}

function annBuka(id) {
  var d = null;
  for (var i = 0; i < ANN_ALL.length; i++) {
    if (ANN_ALL[i].id === id) { d = ANN_ALL[i]; break; }
  }
  if (!d) return;
  var title  = document.getElementById('annMTitle');
  var body   = document.getElementById('annMBody');
  var chips  = document.getElementById('annMChips');
  var modal  = document.getElementById('annModal');
  if (!modal) return;

  if (title) title.textContent = d.judul || '';
  if (body)  body.innerHTML    = d.catatan || '<p style="color:var(--ink2)">Tidak ada isi pengumuman.</p>';
  if (chips) {
    var html = '';
    if (d.utama)         html += '<span class="ann-chip cp">⭐ Prioritas Utama</span>';
    else if(d.kategoriNama) html += '<span class="ann-chip t1">' + escHtml(d.kategoriNama) + '</span>';
    if (d.tanggalStr)    html += '<span class="ann-chip cd"><i class="fa-regular fa-calendar"></i> ' + escHtml(d.tanggalStr) + '</span>';
    if (d.diperuntukkan) html += '<span class="ann-chip ca">' + escHtml(d.diperuntukkan) + '</span>';
    if (d.oleh)          html += '<span class="ann-chip ca"><i class="fa-regular fa-user"></i> ' + escHtml(d.oleh) + '</span>';
    chips.innerHTML = html;
  }
  modal.classList.add('show');
}

function closeAnnModal() {
  var modal = document.getElementById('annModal');
  if (modal) modal.classList.remove('show');
}

/* Tutup modal dengan tombol Escape */
document.addEventListener('keydown', function(e) {
  if (e.key === 'Escape') { closeAnnModal(); tutupHelp(); closeLangMenu(); }
});
/* Klik di luar lang-menu menutupnya */
document.addEventListener('click', function(e) {
  var wrap = document.getElementById('langWrap');
  if (wrap && !wrap.contains(e.target)) closeLangMenu();
});

/* ============================================================
   GANTI BAHASA
   Menyimpan preferensi bahasa pengguna via svc=set_lang,
   kemudian reload halaman agar _lang.jsp mengambil bahasa baru.
   Nilai yang diterima: '' (Indonesia), 'English', 'Arabic', 'Mandarin'.
   ============================================================ */
var LANG_MAP = {
  '':         {lbl:'ID', flag:'🇮🇩', el:'liID'},
  'Indonesia':  {lbl:'ID', flag:'🇮🇩', el:'liID'},
  'English':    {lbl:'EN', flag:'🇬🇧', el:'liEN'},
  'Arabic':     {lbl:'AR', flag:'🇸🇦', el:'liAR'},
  'Mandarin':   {lbl:'ZH', flag:'🇨🇳', el:'liZH'}
};

function initLangLabel() {
  var bahasa = (typeof window.AIS2_BAHASA !== 'undefined') ? window.AIS2_BAHASA : '';
  var info   = LANG_MAP[bahasa] || LANG_MAP[''];
  var lbl    = document.getElementById('langLabel');
  if (lbl) lbl.textContent = info.lbl;
  /* Tandai item aktif */
  Object.keys(LANG_MAP).forEach(function(k) {
    var el = document.getElementById(LANG_MAP[k].el);
    if (el) el.classList.toggle('aktif', k === bahasa || (k === '' && (bahasa === '' || bahasa === 'Indonesia')));
  });
}

function toggleLangMenu() {
  var menu = document.getElementById('langMenu');
  if (menu) menu.classList.toggle('open');
}
function closeLangMenu() {
  var menu = document.getElementById('langMenu');
  if (menu) menu.classList.remove('open');
}

function setLang(lang) {
  closeLangMenu();
  var xhr = new XMLHttpRequest();
  xhr.open('POST', ROOT + '/baru2?modul=utama&svc=set_lang', true);
  xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
  xhr.onload = function() {
    try {
      var r = JSON.parse(xhr.responseText);
      if (r.ok) {
        window.location.reload();
      } else {
        alert('Gagal mengganti bahasa: ' + (r.msg || 'Kesalahan server'));
      }
    } catch(e) { window.location.reload(); }
  };
  xhr.onerror = function() { alert('Tidak dapat terhubung ke server.'); };
  xhr.send('lang=' + encodeURIComponent(lang));
}

/* ============================================================
   BANTUAN & PANDUAN
   Modal dua-panel: nav topik kiri, konten kanan.
   Topik ditentukan otomatis berdasarkan activeTab saat dibuka.
   ============================================================ */
var HELP_ACTIVE = 'beranda';
var HELP_MODUL_MAP = {
  home:'beranda', utama:'beranda',
  akademik:'akademik', jadwal:'akademik', penjadwalan:'akademik',
  elearning:'elearning', lms:'elearning',
  pembayaran:'pembayaran', keuangan:'pembayaran',
  kepegawaian:'kepegawaian', gaji:'kepegawaian',
  feeder:'feeder', sister:'feeder',
  emedic:'emedic',
  kantin:'toko', koperasi:'toko',
  akunting:'akunting', kinerja:'akunting',
  pengadaan:'pengadaan',
  presensi:'presensi', absensi:'presensi',
  spmi:'spmi',
  kalender:'kalender', kegiatan:'kalender',
  pustaka:'pustaka',
  konfigurasi:'konfigurasi'
};
var HELP_DATA = [
  { grp:'Umum', id:'beranda', ic:'fa-house', judul:'Beranda & Navigasi',
    konten:'<h3>Beranda & Navigasi</h3>'
      +'<p>Halaman beranda menampilkan <strong>Papan Pengumuman</strong>, <strong>Kalender Akademik</strong>, dan <strong>Peta Modul</strong> yang dapat Anda akses sesuai hak akses yang dimiliki.</p>'
      +'<div class="help-tip"><strong>Cara membuka modul</strong>Klik tombol di toolbar atas (e-Learning, Akademik, Pembayaran, dll) atau klik kartu di Peta Modul. Setiap modul membuka tab baru di bawah toolbar.</div>'
      +'<p>Untuk kembali ke beranda, klik tab <strong>Home</strong> di baris tab atau ikon rumah di sidebar kiri.</p>'
      +'<ul><li><strong>Sidebar</strong>: Daftar menu lengkap — klik ikon ☰ di pojok kiri atas untuk membuka/menutup.</li>'
      +'<li><strong>Tab</strong>: Bisa membuka beberapa modul sekaligus. Tutup tab dengan tombol ✕.</li>'
      +'<li><strong>Profil</strong>: Klik nama Anda di kanan atas untuk melihat atau mengubah data profil.</li></ul>' },

  { grp:'Umum', id:'bahasa', ic:'fa-globe', judul:'Ganti Bahasa',
    konten:'<h3>Ganti Bahasa Antarmuka</h3>'
      +'<p>Sistem mendukung <strong>4 bahasa</strong>: Bahasa Indonesia, English, العربية (Arab), dan 中文 (Mandarin).</p>'
      +'<div class="help-tip"><strong>Cara mengganti bahasa</strong>Klik tombol globe 🌐 di toolbar kanan atas → pilih bahasa yang diinginkan. Halaman akan dimuat ulang otomatis.</div>'
      +'<p>Terjemahan bersumber dari tabel <em>label_bahasa</em> yang dapat dikelola admin melalui <strong>Konfigurasi → Label Bahasa</strong>. Jika ada kata yang belum diterjemahkan, sistem akan menampilkan teks bahasa Indonesia sebagai cadangan.</p>'
      +'<ul><li>Preferensi bahasa tersimpan di akun Anda, aktif di semua halaman.</li>'
      +'<li>Admin dapat menambah/mengubah terjemahan tanpa perlu deploy ulang.</li></ul>' },

  { grp:'Umum', id:'akun', ic:'fa-user-circle', judul:'Akun & Profil',
    konten:'<h3>Akun & Profil Pengguna</h3>'
      +'<p>Setiap pengguna memiliki profil berbeda tergantung perannya: Mahasiswa, Dosen, Siswa, Guru, Admin Sekolah, atau Admin Perguruan Tinggi.</p>'
      +'<div class="help-tip"><strong>Membuka profil</strong>Klik nama/avatar di pojok kanan atas, lalu pilih "Profil Saya".</div>'
      +'<ul><li>Mahasiswa: melihat IPK, semester, KRS, dan tautan cepat ke modul akademik.</li>'
      +'<li>Dosen: melihat kelas diampu, mahasiswa bimbingan, dan akses setujui KRS.</li>'
      +'<li>Admin PT: statistik institusi (mahasiswa aktif, dosen, program studi) dan akses ke semua modul.</li></ul>'
      +'<p>Untuk mengubah password, gunakan menu <strong>Pengaturan → Ubah Password</strong>.</p>' },

  { grp:'Akademik', id:'akademik', ic:'fa-graduation-cap', judul:'KRS & Perkuliahan',
    konten:'<h3>KRS & Perkuliahan</h3>'
      +'<p>Modul Akademik mencakup pengisian <strong>Kartu Rencana Studi (KRS)</strong>, daftar perkuliahan, presensi, dan penilaian mahasiswa.</p>'
      +'<div class="help-tip"><strong>Alur KRS mahasiswa</strong>Login sebagai mahasiswa → buka modul Akademik → pilih semester dan mata kuliah → simpan KRS → tunggu persetujuan dosen wali.</div>'
      +'<ul><li>Dosen wali dapat menyetujui seluruh KRS mahasiswa sekaligus dengan tombol "Setujui KRS".</li>'
      +'<li>Admin dapat membatalkan atau mengambil KRS melalui menu penjadwalan.</li>'
      +'<li>Status KRS: Baru, Disetujui, Dibatalkan, Dikunci.</li></ul>'
      +'<p>Perkuliahan (pertemuan) dicatat per sesi. Materi dan tugas bisa diunggah langsung dari halaman pertemuan.</p>' },

  { grp:'Akademik', id:'elearning', ic:'fa-laptop-code', judul:'e-Learning (LMS)',
    konten:'<h3>e-Learning & LMS</h3>'
      +'<p>Modul e-Learning menyediakan fitur <strong>unggah materi</strong>, <strong>tugas daring</strong>, <strong>diskusi</strong>, dan <strong>ujian online</strong>.</p>'
      +'<div class="help-tip"><strong>Mengakses materi</strong>Buka modul e-Learning → pilih mata kuliah → klik tab Materi untuk melihat file yang diunggah dosen.</div>'
      +'<ul><li>Dosen: unggah materi (PDF, video, tautan), buat tugas dengan batas waktu, koreksi jawaban.</li>'
      +'<li>Mahasiswa: unduh materi, kumpulkan tugas, berdiskusi di forum, ikuti ujian online.</li>'
      +'<li>Ujian online mendukung anti-kecurangan (deteksi berpindah tab, timer ketat).</li></ul>' },

  { grp:'Keuangan', id:'pembayaran', ic:'fa-credit-card', judul:'Pembayaran & Tagihan',
    konten:'<h3>Pembayaran & Tagihan</h3>'
      +'<p>Tagihan terdiri dari UKT/SPP, biaya semester lainnya, dan cicilan. Pembayaran dapat dilakukan melalui <strong>Virtual Account</strong> bank atau QRIS.</p>'
      +'<div class="help-tip"><strong>Cara melihat tagihan</strong>Buka modul Pembayaran → pilih semester aktif → tagihan ditampilkan lengkap dengan status (Belum Bayar / Lunas / Cicilan).</div>'
      +'<ul><li>Klik "Cek Ulang" untuk memperbarui status pembayaran dari bank.</li>'
      +'<li>Cetak Kartu Ujian hanya tersedia jika tagihan sudah lunas atau dikecualikan admin.</li>'
      +'<li>Laporan pembayaran dapat diunduh dalam format PDF/Excel.</li></ul>' },

  { grp:'Keuangan', id:'akunting', ic:'fa-calculator', judul:'Akuntansi & Keuangan',
    konten:'<h3>Akuntansi & Keuangan Institusi</h3>'
      +'<p>Modul Keuangan mengelola <strong>anggaran, realisasi, posting jurnal, dan laporan keuangan</strong> institusi.</p>'
      +'<ul><li>Posting jurnal dilakukan dari modul Keuangan → pilih periode → verifikasi transaksi → posting.</li>'
      +'<li>Laporan: Neraca, Laba-Rugi, Arus Kas, dan laporan kustom dapat dicetak per periode.</li>'
      +'<li>Rekonsiliasi bank tersedia di sub-modul Kantin dan Koperasi.</li></ul>'
      +'<div class="help-tip"><strong>HPP Kantin</strong>Hitung Pokok Penjualan resep kantin melalui menu Kantin → Laporan → HPP. Sistem mengkalkulasi otomatis berdasarkan bahan baku dan resep.</div>' },

  { grp:'SDM', id:'kepegawaian', ic:'fa-user-tie', judul:'Kepegawaian & Penggajian',
    konten:'<h3>Kepegawaian & Penggajian</h3>'
      +'<p>Modul Kepegawaian mengelola data dosen/staf, absensi, cuti, dan penggajian bulanan.</p>'
      +'<div class="help-tip"><strong>Hitung gaji otomatis</strong>Kepegawaian → Penggajian → pilih bulan → klik "Hitung Ulang". Sistem menghitung gaji pokok, tunjangan, dan potongan secara otomatis.</div>'
      +'<ul><li>Absensi dapat diimpor dari mesin fingerprint atau diisi manual per pegawai.</li>'
      +'<li>Pengajuan cuti dilakukan melalui modul Kepegawaian → Cuti → Ajukan. Admin dapat menyetujui/menolak.</li>'
      +'<li>SKP (Sasaran Kinerja Pegawai) dikelola di sub-modul Kinerja.</li></ul>' },

  { grp:'Integrasi', id:'feeder', ic:'fa-cloud-arrow-up', judul:'Neo Feeder & SISTER',
    konten:'<h3>Neo Feeder PDDikti & SISTER</h3>'
      +'<p><strong>Neo Feeder</strong> mengirim data akademik (mahasiswa, dosen, mata kuliah, nilai) ke PDDikti Kemendikbud. <strong>SISTER</strong> menyinkronkan profil dosen dengan portal DIKTI.</p>'
      +'<div class="help-tip"><strong>Alur pengiriman Feeder</strong>Buka modul Neo Feeder → Pilih semester → Bandingkan data lokal vs PDDikti → Selesaikan konflik → Kirim.</div>'
      +'<ul><li>Data yang belum terkirim ditandai merah. Konflik data harus diselesaikan sebelum pengiriman.</li>'
      +'<li>Log pengiriman tersimpan dan dapat diunduh sebagai bukti laporan.</li>'
      +'<li>SISTER: sinkronisasi dua arah — tarik data dari DIKTI atau dorong perubahan lokal.</li></ul>' },

  { grp:'Layanan', id:'emedic', ic:'fa-notes-medical', judul:'eMedic & SIRS',
    konten:'<h3>eMedic & SIRS (Rekam Medis)</h3>'
      +'<p>Modul eMedic adalah <strong>Rekam Medis Elektronik (RME)</strong> untuk klinik kampus. SIRS terintegrasi dengan BPJS VClaim dan SATUSEHAT Kemenkes.</p>'
      +'<ul><li>Pendaftaran pasien: buka eMedic → Pendaftaran → isi data pasien → pilih poli.</li>'
      +'<li>Dokter mengisi diagnosis, resep, dan tindakan di modul RME.</li>'
      +'<li>Klaim JKN BPJS dikirim otomatis melalui VClaim setelah verifikasi dokter.</li>'
      +'<li>Data rekam medis dikirim ke SATUSEHAT sesuai regulasi Permenkes.</li></ul>' },

  { grp:'Layanan', id:'toko', ic:'fa-basket-shopping', judul:'Kantin & Koperasi',
    konten:'<h3>Toko, Kantin & Koperasi</h3>'
      +'<p>Modul Toko/Kantin adalah POS (Point of Sale) untuk transaksi kantin kampus. Modul Koperasi mengelola simpan pinjam anggota.</p>'
      +'<div class="help-tip"><strong>Kasir POS</strong>Buka modul Toko → pilih produk → konfirmasi pesanan → proses pembayaran (tunai/e-wallet). Struk dapat dicetak atau dikirim digital.</div>'
      +'<ul><li>Stok bahan baku dikelola di Gudang → sistem mencegah overselling otomatis.</li>'
      +'<li>Koperasi: anggota dapat mengajukan pinjaman, admin menyetujui dan menjadwalkan angsuran.</li>'
      +'<li>Laporan omzet, HPP, dan laba-rugi tersedia harian/bulanan.</li></ul>' },

  { grp:'Layanan', id:'pengadaan', ic:'fa-boxes-stacked', judul:'Pengadaan & Asset',
    konten:'<h3>Pengadaan & Asset</h3>'
      +'<p>Modul Pengadaan mengelola <strong>RAB (Rencana Anggaran Biaya)</strong>, proses pengadaan barang/jasa, dan pencatatan asset institusi.</p>'
      +'<ul><li>RAB: buat pos anggaran → setujui → realisasi per item pengadaan.</li>'
      +'<li>Pengadaan mengikuti alur: Permohonan → Persetujuan → Pembelian → Selesai.</li>'
      +'<li>Asset dicatat dengan nilai perolehan, penyusutan, dan lokasi penyimpanan.</li></ul>' },

  { grp:'Mutu', id:'spmi', ic:'fa-clipboard-check', judul:'SPMI & Akreditasi',
    konten:'<h3>SPMI & Akreditasi</h3>'
      +'<p><strong>Sistem Penjaminan Mutu Internal (SPMI)</strong> mengelola standar mutu, instrumen audit, temuan, dan tindak lanjut.</p>'
      +'<ul><li>Standar Mutu: buat/import standar → assign ke unit → monitoring pemenuhan.</li>'
      +'<li>Audit Internal: jadwalkan audit → isi instrumen → rekam temuan → tindak lanjut.</li>'
      +'<li>Akreditasi: upload dokumen borang → pantau status → cetak laporan kesiapan.</li></ul>' },

  { grp:'Mutu', id:'kalender', ic:'fa-calendar-days', judul:'Kalender & Info Kegiatan',
    konten:'<h3>Kalender Akademik & Info Kegiatan</h3>'
      +'<p>Kalender Akademik menampilkan seluruh kegiatan institusi: kuliah, UTS, UAS, wisuda, libur, dan kegiatan kemahasiswaan.</p>'
      +'<div class="help-tip"><strong>Kalender di Beranda</strong>Panel Kalender Akademik di halaman utama menampilkan kegiatan mendatang/berlangsung secara otomatis berdasarkan tanggal hari ini.</div>'
      +'<ul><li>Admin dapat menambah kegiatan kalender melalui menu Kalender Akademik → Tambah.</li>'
      +'<li>Modul Info Kegiatan mengelola pengumuman, agenda, berita, dan banner kampus.</li>'
      +'<li>Notifikasi massal dapat dikirim ke semua pengguna atau grup tertentu.</li></ul>' },

  { grp:'Mutu', id:'pustaka', ic:'fa-book-open', judul:'Perpustakaan (OPAC)',
    konten:'<h3>Perpustakaan & e-Library</h3>'
      +'<p>Modul Pustaka adalah sistem manajemen perpustakaan digital dengan OPAC (Online Public Access Catalog).</p>'
      +'<ul><li>Katalog: cari buku berdasarkan judul, pengarang, atau kata kunci.</li>'
      +'<li>Sirkulasi: proses peminjaman, pengembalian, dan denda keterlambatan.</li>'
      +'<li>e-Library: unggah dan akses buku digital, jurnal, dan repositori karya ilmiah.</li></ul>' },

  { grp:'Sistem', id:'konfigurasi', ic:'fa-sliders', judul:'Konfigurasi Sistem',
    konten:'<h3>Konfigurasi & Pengaturan Sistem</h3>'
      +'<p>Modul Konfigurasi hanya dapat diakses oleh pengguna dengan hak akses <strong>Admin Sistem</strong>.</p>'
      +'<ul><li><strong>Pengguna & Hak Akses</strong>: kelola akun, role, dan permission tiap modul.</li>'
      +'<li><strong>Label Bahasa</strong>: tambah atau ubah terjemahan antarmuka ke 4 bahasa tanpa compile ulang.</li>'
      +'<li><strong>Parameter Sistem</strong>: konfigurasi nama institusi, logo, gateway pembayaran, API integrasi.</li>'
      +'<li><strong>Debug & Log</strong>: lihat log error, audit trail, dan status koneksi integrasi.</li></ul>'
      +'<div class="help-tip"><strong>Perhatian</strong>Perubahan konfigurasi sistem berdampak ke seluruh pengguna. Pastikan Anda memahami dampaknya sebelum menyimpan.</div>' },

  { grp:'Sistem', id:'presensi', ic:'fa-fingerprint', judul:'Presensi Kehadiran',
    konten:'<h3>Presensi & Kehadiran</h3>'
      +'<p>Modul Presensi mengelola kehadiran mahasiswa di perkuliahan dan kehadiran pegawai/guru.</p>'
      +'<ul><li>Dosen mengisi kehadiran mahasiswa per pertemuan dari modul Perkuliahan.</li>'
      +'<li>Absensi pegawai dapat diimpor dari mesin fingerprint (format CSV/Excel).</li>'
      +'<li>Rekap kehadiran tersedia per periode, program studi, atau individu.</li>'
      +'<li>Mahasiswa dengan kehadiran di bawah ambang dapat diblokir mengikuti ujian (konfigurasi admin).</li></ul>' }
];

var HELP_ACTIVE_IDX = 0;

function bukaHelp() {
  var modal = document.getElementById('helpModal');
  if (!modal) return;
  /* Pilih topik berdasarkan modul aktif */
  var target = HELP_MODUL_MAP[activeTab] || 'beranda';
  renderHelpNav(target);
  renderHelpContent(target);
  modal.classList.add('show');
}

function tutupHelp() {
  var modal = document.getElementById('helpModal');
  if (modal) modal.classList.remove('show');
}

function pilihTopikHelp(id) {
  renderHelpNav(id);
  renderHelpContent(id);
}

function renderHelpNav(aktifId) {
  var nav = document.getElementById('helpNav');
  if (!nav) return;
  /* Kelompokkan topik per grp */
  var grps = {}, grpOrder = [];
  HELP_DATA.forEach(function(t) {
    if (!grps[t.grp]) { grps[t.grp] = []; grpOrder.push(t.grp); }
    grps[t.grp].push(t);
  });
  var html = '';
  grpOrder.forEach(function(g) {
    html += '<div class="help-nav-grp">' + escHtml(g) + '</div>';
    grps[g].forEach(function(t) {
      html += '<div class="help-topic' + (t.id === aktifId ? ' aktif' : '') + '" onclick="pilihTopikHelp(\'' + escJs(t.id) + '\')">'
           +  '<i class="fa-solid ' + t.ic + '"></i> ' + escHtml(t.judul)
           + '</div>';
    });
  });
  nav.innerHTML = html;
}

function renderHelpContent(id) {
  var cont = document.getElementById('helpContent');
  if (!cont) return;
  var topic = null;
  for (var i = 0; i < HELP_DATA.length; i++) {
    if (HELP_DATA[i].id === id) { topic = HELP_DATA[i]; break; }
  }
  if (!topic) {
    cont.innerHTML = '<p style="color:var(--ink3)">Topik bantuan tidak ditemukan.</p>';
    return;
  }
  cont.innerHTML = topic.konten;
}

/* ============================================================
   KALENDER AKADEMIK
   Memuat kegiatan via svc=kalender_upcoming.
   Tampil sebagai kartu grid bergaya ZK index.zul.
   ============================================================ */
var KAL_ALL      = [];
var KAL_FILTERED = [];
var KAL_BULAN    = ['Jan','Feb','Mar','Apr','Mei','Jun','Jul','Agu','Sep','Okt','Nov','Des'];

function loadKalender() {
  var xhr = new XMLHttpRequest();
  xhr.open('GET', ROOT + '/baru2?modul=utama&svc=kalender_upcoming', true);
  xhr.onload = function() {
    if (xhr.status === 200) {
      try {
        var r = JSON.parse(xhr.responseText);
        if (!r.ok && r.msg) { kalErr(r.msg); return; }
        KAL_ALL      = Array.isArray(r.data) ? r.data : [];
        KAL_FILTERED = KAL_ALL.slice();
        var el = document.getElementById('kalCount');
        if (el) el.textContent = KAL_ALL.length;
        populateKalTahun();
        renderKal();
      } catch(e) {
        console.error('[kalender] parse error:', e.message, '\nResponse:', xhr.responseText.slice(0,400));
        kalErr('Gagal memuat: ' + (e.message || 'respons tidak valid'));
      }
    } else { kalErr('Server error ' + xhr.status); }
  };
  xhr.onerror = function() { kalErr('Tidak dapat terhubung ke server.'); };
  xhr.send();
}

function kalErr(msg) {
  var el = document.getElementById('kalList');
  if (el) el.innerHTML = '<div class="ann-empty" style="grid-column:1/-1"><i class="fa-solid fa-triangle-exclamation"></i> ' + escHtml(msg) + '</div>';
}

function populateKalTahun() {
  var seen = {}, opts = '<option value="">Semua Tahun</option>';
  KAL_ALL.forEach(function(k) { if (k.tahunAkademik && !seen[k.tahunAkademik]) { seen[k.tahunAkademik] = 1; opts += '<option value="' + escHtml(k.tahunAkademik) + '">' + escHtml(k.tahunAkademik) + '</option>'; } });
  var sel = document.getElementById('kalTahun');
  if (sel) sel.innerHTML = opts;
}

function filterKal() {
  var tahun    = (document.getElementById('kalTahun')    || {}).value || '';
  var semester = (document.getElementById('kalSemester') || {}).value || '';
  KAL_FILTERED = KAL_ALL.filter(function(k) {
    if (tahun    && k.tahunAkademik !== tahun)                                 return false;
    if (semester && (k.semester||'').toLowerCase() !== semester.toLowerCase()) return false;
    return true;
  });
  var el = document.getElementById('kalCount');
  if (el) el.textContent = KAL_FILTERED.length;
  renderKal();
}

function renderKal() {
  var list = document.getElementById('kalList');
  if (!list) return;
  if (!KAL_FILTERED.length) {
    list.innerHTML = '<div class="ann-empty" style="grid-column:1/-1"><i class="fa-regular fa-calendar-xmark"></i><br>Tidak ada agenda ditemukan.</div>';
    return;
  }
  var now  = Date.now();
  var html = '';
  KAL_FILTERED.forEach(function(d) {
    var mulai  = d.tanggalMulaiTs  ? new Date(d.tanggalMulaiTs)  : null;
    var selesai= d.tanggalSelesaiTs? new Date(d.tanggalSelesaiTs): mulai;
    var day = mulai ? mulai.getDate()              : '—';
    var mon = mulai ? KAL_BULAN[mulai.getMonth()]  : '';

    var stCls = 'belum';
    if (mulai && selesai) {
      if (now >= mulai.getTime() && now <= selesai.getTime()) stCls = 'berlangsung';
      else if (now > selesai.getTime())                       stCls = 'selesai';
    }

    var semNorm = (d.semester || '').toLowerCase();
    var semCls  = semNorm.indexOf('ganjil') >= 0 ? 'ganjil' : semNorm.indexOf('genap') >= 0 ? 'genap' : '';

    html += '<div class="kal-card" title="' + escHtml(d.nama) + '">';
    html += '<div class="kal-card-day">' + day + '</div>';
    html += '<div class="kal-card-mon">' + escHtml(mon) + '</div>';
    html += '<div class="kal-card-name">' + escHtml(d.nama) + '</div>';
    html += '<div class="kal-card-meta">';
    if (d.tahunAkademik) html += '<span class="kal-cbadge ta">' + escHtml(d.tahunAkademik) + '</span>';
    if (semCls)          html += '<span class="kal-cbadge ' + semCls + '">' + escHtml(d.semester) + '</span>';
    else if (stCls === 'berlangsung') html += '<span class="kal-cbadge berlangsung">Berlangsung</span>';
    else if (stCls === 'selesai')     html += '<span class="kal-cbadge selesai">Selesai</span>';
    html += '</div>';
    if (d.tanggalMulaiStr) {
      var rangeStr = d.tanggalMulaiStr;
      if (d.tanggalSelesaiStr && d.tanggalSelesaiStr !== d.tanggalMulaiStr) rangeStr += ' s/d ' + d.tanggalSelesaiStr;
      html += '<div class="kal-card-range">' + escHtml(rangeStr) + '</div>';
    }
    html += '</div>';
  });
  list.innerHTML = html;
}

/* === Online Users (polling ringan) === */
function updateOnlineCount() {
  try {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', ROOT + '/baru2?modul=utama&svc=online', true);
    xhr.onload = function() {
      if (xhr.status === 200) {
        try {
          var data = JSON.parse(xhr.responseText);
          var el = document.getElementById('onlineCount');
          if (el) el.textContent = 'Online: ' + (data.count || 1);
        } catch(e) {}
      }
    };
    xhr.send();
  } catch(e) {}
}

/* === Toolbar visibility dari Tbmrole === */
var TB_MAP = {
  tbElearning:'elearning', tbPrestasi:'kegiatanDanPrestasi', tbPustaka:'pustaka',
  tbWorkflow:'workflow', tbRepository:'dasborRepository', tbSpmi:'tampilkanSpmi',
  tbKantin:'kantin', tbAkademik:'dashboard', tbAdministrasi:'administrasi',
  tbPembayaran:'pembayaran', tbKeuangan:'keuangan', tbKepegawaian:'kepegawaian',
  tbGaji:'tampilkanGaji', tbFeeder:'bolehAksesFeeder', tbSister:'bolehAksesSister',
  tbEmedic:'emedic', tbKoperasi:'dashboardKoperasi', tbAkunting:'akunting',
  tbPengadaan:'pengadaan', tbKinerja:'kinerja', tbPresensi:'presensiKehadiran',
  tbKalender:'kalenderAkademik', tbKegiatan:'infoKegiatan', tbAntarJemput:'dasboardAntarJemput'
};
function initToolbar() {
  var xhr = new XMLHttpRequest();
  xhr.open('GET', ROOT + '/baru2?modul=utama&svc=hak_akses', true);
  xhr.onload = function() {
    if (xhr.status !== 200) return;
    try {
      var d = JSON.parse(xhr.responseText);
      var akses = d.data || d;
      for (var btnId in TB_MAP) {
        var flag = TB_MAP[btnId];
        var btn = document.getElementById(btnId);
        if (btn && akses[flag]) btn.style.display = '';
      }
    } catch(e) {}
  };
  xhr.send();
}

/* === Init === */
document.addEventListener('DOMContentLoaded', function() {
  renderPetaModul();
  // renderSidebar() tidak dipanggil — sidebar dibuild server-side di JSP
  renderTabBar();
  updateOnlineCount();
  initToolbar();
  loadPengumuman();
  loadKalender();
  initLangLabel();
  // Polling online users setiap 60 detik
  setInterval(updateOnlineCount, 60000);
});
</script>
</body>
</html>
