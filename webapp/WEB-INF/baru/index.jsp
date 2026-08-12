<%@page import="ais.database.model.Tbmrole"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Menu"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="javax.servlet.RequestDispatcher"%>
<%@page import="javax.servlet.http.HttpServletRequest"%>
<%@page import="javax.servlet.http.HttpServletResponse"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
private String traceId(HttpServletRequest request) {
    String sessionId = "NOSESSION";
    try {
        if (request != null && request.getSession(false) != null) {
            sessionId = request.getSession(false).getId();
        }
    } catch (Exception e) {
        sessionId = "SESSIONERR";
    }
    return System.currentTimeMillis() + "-" + Math.abs((request == null ? 0 : request.hashCode())) + "-" + sessionId;
}

private void logIndex(String trace, long start, String message) {
    try {
        //System.out.println("[INDEX-JSP] trace=" + trace + " elapsed=" + (System.currentTimeMillis() - start) + "ms " + message);
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/index.jsp:27");
    }
}

private void reportPageFailure(HttpServletRequest request, String trace, String info, Throwable throwable) {
    Throwable actual = throwable == null ? new javax.servlet.ServletException(info) : throwable;
    ais.common.ErrorAuditUtil.ErrorAuditResult audit = ais.common.ErrorAuditUtil.recordVisibleFailure(
            actual, info, request, trace);
    request.setAttribute("baru.error.trace", trace);
    request.setAttribute("baru.error.info", info);
    request.setAttribute("baru.error.throwable", actual);
    request.setAttribute("baru.error.content", audit == null ? null : audit.getContent());
    request.setAttribute("baru.error.log_id", audit == null ? null : audit.getErrorLogId());
    request.setAttribute("baru.error.show_detail",
            Boolean.valueOf(ais.common.ErrorAuditUtil.isUiDetailActive()));
}

private void includeFailurePage(HttpServletRequest request, HttpServletResponse response) throws Exception {
    request.getRequestDispatcher("/WEB-INF/baru/componen/tidak_ketemu_page.jsp").include(request, response);
}

private boolean hasText(String value) {
    return value != null && value.trim().length() > 0;
}

private boolean isSafeInternalPath(String path) {
    if (path == null) {
        return false;
    }
    String p = path.trim();
    return p.length() > 0 && p.startsWith("/WEB-INF/") && p.indexOf("..") < 0 && p.indexOf('\\') < 0;
}

private boolean isLegacyZkPath(String path) {
    if (path == null) {
        return false;
    }
    String p = path.trim().toLowerCase();
    return p.indexOf(".zul") >= 0 || p.indexOf("/displaymenu") >= 0 || p.indexOf("/pages/") >= 0
            || p.indexOf("_zk.jsp") >= 0 || (p.indexOf("/modul/pages") >= 0 && p.endsWith("zul/index.jsp"));
}

private String safeModuleSegment(String value) {
    if (value == null) {
        return "";
    }
    String v = value.trim();
    if (v.length() == 0) {
        return "";
    }
    if (v.indexOf("..") >= 0 || v.indexOf('\\') >= 0 || v.startsWith("/") || v.indexOf("//") >= 0) {
        return "";
    }
    return v;
}

private void includePage(HttpServletRequest request, HttpServletResponse response, javax.servlet.jsp.JspWriter out,
        String page, String label, String trace, long start) throws Exception {
    // Output include() ditulis langsung ke response, sedangkan template text JSP induk
    // tertahan di buffer JspWriter. Tanpa flush, urutan HTML jadi acak (konten keluar
    // sebelum <body>, navbar melayang menutupi konten). Flush dulu agar urutannya benar.
    if (out != null) {
        out.flush();
    }
    if (!isSafeInternalPath(page)) {
        logIndex(trace, start, "SKIP include unsafe path label=" + label + " page=" + page);
        reportPageFailure(request, trace, "Path include tidak aman: label=" + label + " target=" + page,
                new SecurityException("Target JSP ditolak oleh validasi path"));
        includeFailurePage(request, response);
        return;
    }
    if (isLegacyZkPath(page)) {
        logIndex(trace, start, "SKIP include legacy ZK path label=" + label + " page=" + page);
        reportPageFailure(request, trace, "Route masih memakai ZK/ZUL: label=" + label + " target=" + page,
                new IllegalStateException("Route CRUD belum mempunyai halaman JSP yang dapat dimuat"));
        includeFailurePage(request, response);
        return;
    }
    logIndex(trace, start, "BEFORE include " + label + " page=" + page);
    /* Dispatcher tetap non-null walau file JSP-nya tidak ada; include-nya baru
       meledak ServletException "JSP file not found" (kasus: parameter p berisi
       path zul yang dimangkas, mis. p=pagesmasterinformasipembayaranmahasiswazul
       dari menu yang modulnya belum tersedia di tampilan baru). Cek dulu
       file-nya benar-benar ada, kalau tidak tampilkan halaman ramah. */
    try {
        String realPath = request.getServletContext().getRealPath(page);
        if (realPath != null && !(new java.io.File(realPath).exists())) {
            logIndex(trace, start, "MISSING file " + label + " page=" + page);
            reportPageFailure(request, trace, "File JSP tidak ditemukan: label=" + label + " target=" + page,
                    new java.io.FileNotFoundException(page));
            includeFailurePage(request, response);
            return;
        }
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/index.jsp:83");
    }
    RequestDispatcher dispatcher = request.getRequestDispatcher(page);
    if (dispatcher == null) {
        logIndex(trace, start, "NULL dispatcher " + label + " page=" + page);
        reportPageFailure(request, trace, "RequestDispatcher tidak tersedia: label=" + label + " target=" + page,
                new java.io.FileNotFoundException(page));
        includeFailurePage(request, response);
        return;
    }
    try {
        dispatcher.include(request, response);
        logIndex(trace, start, "AFTER include " + label + " page=" + page + " committed=" + response.isCommitted());
    } catch (Throwable throwable) {
        logIndex(trace, start, "ERROR include " + label + " page=" + page + " "
                + throwable.getClass().getName() + ":" + throwable.getMessage());
        reportPageFailure(request, trace, "Gagal include JSP: label=" + label + " target=" + page, throwable);
        includeFailurePage(request, response);
    }
}

private boolean isKonfigurasiAktif(String key, String defaultValue) {
    try {
        Konfigurasi konfigurasi = Common.getKonfigurasi(key, defaultValue);
        return konfigurasi != null && konfigurasi.getNilai() != null
                && konfigurasi.getNilai().trim().equalsIgnoreCase(Konfigurasi.AKTIF);
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/index.jsp:101");
    }
    return Konfigurasi.AKTIF.equalsIgnoreCase(defaultValue);
}

private boolean bolehAksesModulKantin(HttpServletRequest request, Tbmuser tbmuser, String p, String s) {
	if ("apotik".equals(p) || "emedik".equals(p)) {
		if (!hasText(s) || "index".equals(s)) return true;
		if (tbmuser == null || tbmuser.hakAkses() == null) return false;
		String menuVarian = s;
		if ("help".equals(s)) {
			menuVarian = request.getParameter("menu");
			if (!hasText(menuVarian) || !menuVarian.startsWith(p + "_")) return false;
		}
		else if ("apotik".equals(p)) {
			if ("resep".equals(s)) menuVarian = "apotik_resep";
			else if ("racikan".equals(s)) menuVarian = "apotik_racikan";
			else if ("formularium".equals(s)) menuVarian = "apotik_formularium";
			else if ("batch".equals(s)) menuVarian = "apotik_batch";
			else if ("pengadaan".equals(s)) menuVarian = "apotik_pengadaan";
			else if ("stok_opname".equals(s)) menuVarian = "apotik_stok_opname";
			else if ("retur".equals(s)) menuVarian = "apotik_retur";
			else if ("narkotika".equals(s)) menuVarian = "apotik_narkotika";
			else if ("laporan".equals(s)) menuVarian = "apotik_laporan";
			else menuVarian = "apotik_kasir";
		} else {
			menuVarian = "emedik_" + s;
		}
		org.json.JSONObject hak = ais.common.EbisnisMenuKatalog.urai(tbmuser.hakAkses().getEbisnisMenu());
		org.json.JSONObject menuVarianJson = hak.optJSONObject("menu");
		return menuVarianJson != null && menuVarianJson.optBoolean(menuVarian, false);
	}
    String menu = s;
    if (p != null && p.startsWith("kantin/")) {
        menu = p.substring("kantin/".length());
        int slash = menu.indexOf('/');
        if (slash >= 0) menu = menu.substring(0, slash);
        if ("pos".equals(menu)) menu = "pos";
        if ("barang".equals(menu)) menu = "barang";
        if ("stok".equals(menu)) menu = "stok";
        if ("kulakan".equals(menu)) menu = "kulakan";
        if ("diskon".equals(menu)) menu = "diskon";
        if ("kas".equals(menu)) menu = "kas";
        if ("tenant".equals(menu)) menu = "tenant";
        if ("member".equals(menu)) menu = "anggota";
        if ("toko_dan_pedagang".equals(menu)) menu = "pedagang";
    } else if (!"kantin".equals(p)) {
        return true;
    }
    if (!hasText(menu)) return true;
    if (tbmuser == null || tbmuser.hakAkses() == null) return true;

    Tbmrole role = tbmuser.hakAkses();
    // Role dasar Kantin yang sudah lama tersimpan umumnya memiliki menu.pedagang=false.
    // Admin kantin dikenali dari tidak adanya relasi Pedagang/Toko dan tetap harus dapat
    // membuka halaman pengelolaan akun pedagang. Role khusus tetap mengikuti konfigurasinya.
    if ("pedagang".equals(menu)
            && tbmuser.getPedagang() == null
            && role.getRoleId() != null && role.getRoleId().equalsIgnoreCase(Tbmrole.KANTIN)) {
        return true;
    }
    org.json.JSONObject ebisnisMenuRole = ais.common.EbisnisMenuKatalog.urai(role.getEbisnisMenu());
    if ("beranda".equals(menu)) return ebisnisMenuRole.optBoolean("berandaKantin", false);
    org.json.JSONObject menuTersimpan = ebisnisMenuRole.optJSONObject("menu");
    if (menuTersimpan == null) menuTersimpan = new org.json.JSONObject();
    if ("ringkasan".equals(menu)) return menuTersimpan.optBoolean("ringkasan", true);
    if ("pos".equals(menu)) return menuTersimpan.optBoolean("kasir", true);
    if ("pesanan".equals(menu)) return menuTersimpan.optBoolean("pesanan", true);
    if ("pengaturan".equals(menu)) return menuTersimpan.optBoolean("konfigurasi", true);
    if ("laporan_laporan".equals(menu)) return menuTersimpan.optBoolean("laporan", true);
    if ("anggota".equals(menu)) return menuTersimpan.optBoolean("anggota", true);
    if ("barang".equals(menu)) return menuTersimpan.optBoolean("produk", true);
    if ("kulakan".equals(menu)) return menuTersimpan.optBoolean("kulakan", true);
    if ("retur_penjualan".equals(menu)) return menuTersimpan.optBoolean("returpenjualan", true);
    if ("diskon".equals(menu)) return menuTersimpan.optBoolean("diskon", true);
    if ("pembayaran".equals(menu)) return menuTersimpan.optBoolean("pembayaran", true);
    if ("pedagang".equals(menu)) return menuTersimpan.optBoolean("pedagang", true);
    if ("stok".equals(menu)) return menuTersimpan.optBoolean("stokopname", true);
    if ("meja".equals(menu)) return menuTersimpan.optBoolean("meja", true);
    if ("penyedia".equals(menu)) return menuTersimpan.optBoolean("penyedia", true);
    if ("kas".equals(menu)) return menuTersimpan.optBoolean("kaskasir", true);
    if ("tenant".equals(menu)) return menuTersimpan.optBoolean("setorantenant", true);
    if ("opname".equals(menu)) return menuTersimpan.optBoolean("jadwalopname", true);
    if ("stok_expired".equals(menu)) return menuTersimpan.optBoolean("stokexpired", true);
    if ("limit_kredit".equals(menu)) return menuTersimpan.optBoolean("limitkredit", true);
    if ("mutasi_rekening".equals(menu)) return menuTersimpan.optBoolean("mutasirekening", true);
    if ("produksi".equals(menu)) return menuTersimpan.optBoolean("produksi", true);
    if ("pengaturan_laporan".equals(menu)) return menuTersimpan.optBoolean("pengaturanlaporan", true);
    return true;
}
%>
<%
long __start = System.currentTimeMillis();
String __trace = traceId(request);
logIndex(__trace, __start, "START uri=" + request.getRequestURI() + " query=" + request.getQueryString());

try {
    logIndex(__trace, __start, "BEFORE ConstantValues.init");
    ConstantValues.init();
    logIndex(__trace, __start, "AFTER ConstantValues.init");

    if (session.getAttribute("mytbmuser") == null) {
        String redirectURL = request.getContextPath() + "/logoff";
        logIndex(__trace, __start, "NO mytbmuser REDIRECT " + redirectURL);
        response.sendRedirect(redirectURL);
        return;
    }

    logIndex(__trace, __start, "BEFORE Common.getCurrentUser");
    Tbmuser tbmuser = Common.getCurrentUser(request);
    logIndex(__trace, __start, "AFTER Common.getCurrentUser user=" + (tbmuser == null ? "NULL" : tbmuser.getUserId()));

    if (tbmuser == null || tbmuser.getUserId() == null) {
        String redirectURL = request.getContextPath() + "/logoff";
        logIndex(__trace, __start, "INVALID user REDIRECT " + redirectURL);
        response.sendRedirect(redirectURL);
        return;
    }

    boolean hanya_tampil_jsp = request.getParameter("hanya_tampil_jsp") != null
            && request.getParameter("hanya_tampil_jsp").trim().equalsIgnoreCase("true");

    boolean langsungHalamanMember = false;
    try {
        logIndex(__trace, __start, "BEFORE member redirect check");
        langsungHalamanMember = !hanya_tampil_jsp
                && tbmuser.hakAkses() != null
                && tbmuser.hakAkses().getRoleId() != null
                && !tbmuser.hakAkses().getRoleId().equals(Tbmrole.KANTIN)
                && tbmuser.getAnggotaKoperasi() != null
                && tbmuser.getPedagang() == null
                && !Common.getApakahAdminLain(tbmuser)
                && isKonfigurasiAktif("jika_login_sebagai_member_kecuali_admin_maka_langsung_ke_halaman_member",
                        Konfigurasi.TIDAK_AKTIF);
        logIndex(__trace, __start, "AFTER member redirect check result=" + langsungHalamanMember);
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/index.jsp:151");
        logIndex(__trace, __start, "ERROR member redirect check " + e.getClass().getName() + ":" + e.getMessage());
    }

    if (langsungHalamanMember) {
        includePage(request, response, out, "/WEB-INF/baru/modul/kantin/index.jsp", "memberLanding", __trace,
                __start);
        logIndex(__trace, __start, "FINISH memberLanding");
        return;
    }

    String menu = request.getParameter("menu") == null ? "" : request.getParameter("menu").trim();
    if (hasText(menu)) {
        try {
            logIndex(__trace, __start, "BEFORE load menu id=" + menu);
            Menu menuData = (Menu) ConstantValues.ambil(Menu.class.getName(), Long.valueOf(menu));
            if (menuData != null) {
                request.getSession().setAttribute("current_menu", menuData);
                request.getSession().setAttribute("currentMenu", menuData);
            }
            logIndex(__trace, __start, "AFTER load menu found=" + (menuData != null));
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/index.jsp:173");
            logIndex(__trace, __start, "ERROR load menu id=" + menu + " error=" + e.getClass().getName() + ":" + e.getMessage());
        }
    }

    String p = safeModuleSegment(request.getParameter("p"));
    String s = safeModuleSegment(request.getParameter("s"));
    if (!hasText(p)) {
        p = safeModuleSegment((String) request.getAttribute("default_p"));
    }
    if (!hasText(s)) {
        s = safeModuleSegment((String) request.getAttribute("default_s"));
    }
    String urlLama = request.getParameter("urlLama") == null ? "" : request.getParameter("urlLama").trim();
    logIndex(__trace, __start, "PARAM hanya_tampil_jsp=" + hanya_tampil_jsp + " p=" + p + " s=" + s
            + " urlLamaAda=" + hasText(urlLama));

    if (hanya_tampil_jsp) {
        if (hasText(p) && hasText(s)) {
            try {
                if (bolehAksesModulKantin(request, tbmuser, p, s)) {
                    includePage(request, response, out, "/WEB-INF/baru/modul/" + p + "/" + s + ".jsp", "hanyaTampilJsp", __trace,
                            __start);
                } else {
                    includePage(request, response, out, "/WEB-INF/baru/componen/tidak_ketemu_page.jsp", "aksesKantinDitolak",
                            __trace, __start);
                }
            } catch (Exception e) {
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/index.jsp:190");
                logIndex(__trace, __start, "ERROR include hanyaTampilJsp " + e.getClass().getName() + ":" + e.getMessage());
                includePage(request, response, out, "/WEB-INF/baru/componen/tidak_ketemu_page.jsp", "notFound", __trace,
                        __start);
            }
        } else {
            logIndex(__trace, __start, "hanya_tampil_jsp tanpa p/s, tidak ada modul yang di-include");
        }
        logIndex(__trace, __start, "FINISH hanya_tampil_jsp committed=" + response.isCommitted());
        return;
    }

    /* ── Mobile content-only mode (?_mob=1) ──────────────────────────────────
     * Dipakai oleh overlay iframe di tampilan mobile agar halaman yang dimuat
     * hanya berisi konten modul — tanpa sidebar, navbar desktop, dan footer.
     * CSS/JS dari header.jsp tetap disertakan supaya tampilan modul tetap rapi.
     * Mendukung: /baru?p=modul&_mob=1  (s="index" jika tidak diberikan)
     *            /baru?p=modul&s=sub&_mob=1
     * ─────────────────────────────────────────────────────────────────────── */
    boolean isMobileOnly = "1".equals(request.getParameter("_mob"));
    if (isMobileOnly) {
        String mobSub = hasText(s) ? s : "index";
        out.flush();
        includePage(request, response, out, "/WEB-INF/baru/include/header.jsp", "header", __trace, __start);
        out.write("<body style=\"padding:0;background:#f3f5f9;\">");
        if (hasText(p)) {
            try {
                if (bolehAksesModulKantin(request, tbmuser, p, mobSub)) {
                    includePage(request, response, out,
                            "/WEB-INF/baru/modul/" + p + "/" + mobSub + ".jsp",
                            "mobContent", __trace, __start);
                } else {
                    includePage(request, response, out,
                            "/WEB-INF/baru/componen/tidak_ketemu_page.jsp",
                            "aksesKantinDitolakMob", __trace, __start);
                }
            } catch (Exception e) {
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/index.jsp:221");
                logIndex(__trace, __start, "ERROR mobContent p=" + p + " s=" + mobSub
                        + " " + e.getClass().getName() + ":" + e.getMessage());
                includePage(request, response, out,
                        "/WEB-INF/baru/componen/tidak_ketemu_page.jsp",
                        "notFoundMob", __trace, __start);
            }
        }
        out.flush();
        includePage(request, response, out, "/WEB-INF/baru/include/dialog-modal.jsp", "dialogModal", __trace, __start);
        includePage(request, response, out, "/WEB-INF/baru/include/foot.jsp", "foot", __trace, __start);
        out.write("</body></html>");
        logIndex(__trace, __start, "FINISH mobile-only committed=" + response.isCommitted());
        return;
    }
%>
<% includePage(request, response, out, "/WEB-INF/baru/include/header.jsp", "header", __trace, __start); %>
<body>
    <main class="main" id="top">
        <div class="container" data-layout="container">
            <% includePage(request, response, out, "/WEB-INF/baru/include/isFluid.jsp", "isFluid", __trace, __start); %>
            <% includePage(request, response, out, "/WEB-INF/baru/include/nav.jsp", "nav", __trace, __start); %>

            <div class="content">
                <%
                try {
                    includePage(request, response, out, "/WEB-INF/baru/include/navbar.jsp", "navbar", __trace, __start);
                } catch (Exception e) {
                    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/index.jsp:249");
                    logIndex(__trace, __start, "ERROR include navbar " + e.getClass().getName() + ":" + e.getMessage());
                }

                if (hasText(urlLama)) {
                    logIndex(__trace, __start, "SKIP legacy iframe urlLama=" + urlLama);
                %>
                    <div class="alert alert-info border-0 shadow-sm" role="alert">
                        <div class="fw-bold mb-1"><%= Common.getBahasaConfig("Halaman klasik tidak dimuat di tampilan JSP") %></div>
                        <div><%= Common.getBahasaConfig("Silakan gunakan menu versi JSP yang tersedia atau buka Tampilan Klasik dari menu profil jika memang diperlukan.") %></div>
                    </div>
                <%
                } else if (!hasText(p) && !hasText(s)) {
                    String home = "/WEB-INF/baru/modul/home/index.jsp";
                    try {
                        if (tbmuser.hakAkses() != null && tbmuser.hakAkses().getHalamanUtama() != null
                                && tbmuser.hakAkses().getHalamanUtama().trim().length() > 0) {
                            home = tbmuser.hakAkses().getHalamanUtama().trim();
                        }
                    } catch (Exception e) {
                        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/index.jsp:266");
                        logIndex(__trace, __start, "ERROR get halaman utama " + e.getClass().getName() + ":" + e.getMessage());
                    }
                    try {
                        includePage(request, response, out, home, "halamanUtama", __trace, __start);
                    } catch (Exception e) {
                        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/index.jsp:272");
                        logIndex(__trace, __start, "ERROR include halamanUtama " + home + " " + e.getClass().getName() + ":" + e.getMessage());
                        includePage(request, response, out, "/WEB-INF/baru/componen/tidak_ketemu_page.jsp", "notFoundHome", __trace,
                                __start);
                    }
                } else if (hasText(p) && hasText(s)) {
                    try {
                        if (bolehAksesModulKantin(request, tbmuser, p, s)) {
                            includePage(request, response, out, "/WEB-INF/baru/modul/" + p + "/" + s + ".jsp", "modulPdanS", __trace,
                                    __start);
                        } else {
                            includePage(request, response, out, "/WEB-INF/baru/componen/tidak_ketemu_page.jsp", "aksesKantinDitolakPS", __trace,
                                    __start);
                        }
                    } catch (Exception e) {
                        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/index.jsp:282");
                        logIndex(__trace, __start, "ERROR include modul p/s " + e.getClass().getName() + ":" + e.getMessage());
                        includePage(request, response, out, "/WEB-INF/baru/componen/tidak_ketemu_page.jsp", "notFoundPS", __trace,
                                __start);
                    }
                } else {
                    try {
                        includePage(request, response, out, "/WEB-INF/baru/modul/" + p + "/index.jsp", "modulIndex", __trace,
                                __start);
                    } catch (Exception e) {
                        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/index.jsp:292");
                        logIndex(__trace, __start, "ERROR include modul/index " + e.getClass().getName() + ":" + e.getMessage());
                        includePage(request, response, out, "/WEB-INF/baru/componen/tidak_ketemu_page.jsp", "notFoundModule", __trace,
                                __start);
                    }
                }
                %>

                <% includePage(request, response, out, "/WEB-INF/baru/include/footer.jsp", "footer", __trace, __start); %>
            </div>

            <% includePage(request, response, out, "/WEB-INF/baru/include/dialog-modal.jsp", "dialogModal", __trace, __start); %>
        </div>
    </main>

    <% includePage(request, response, out, "/WEB-INF/baru/include/foot.jsp", "foot", __trace, __start); %>
</body>
</html>
<%
    logIndex(__trace, __start, "FINISH normal committed=" + response.isCommitted());
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/index.jsp:313");
    logIndex(__trace, __start, "FATAL " + e.getClass().getName() + ":" + e.getMessage());
    if (!response.isCommitted()) {
        try {
            response.setStatus(500);
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/index.jsp:318");
        }
    }
%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <title>Terjadi Kendala</title>
    <style>
        body{font-family:Arial,sans-serif;background:#f8fafc;color:#334155;padding:40px;}
        .box{max-width:760px;margin:auto;background:white;border:1px solid #e2e8f0;border-radius:18px;padding:28px;box-shadow:0 18px 45px rgba(15,23,42,.08)}
        code{background:#f1f5f9;padding:3px 6px;border-radius:6px;}
    </style>
</head>
<body>
    <div class="box">
        <h2>Halaman belum dapat ditampilkan</h2>
        <p>Terjadi kendala saat memuat halaman utama. Silakan cek log Tomcat dengan trace berikut:</p>
        <p><code><%= __trace %></code></p>
        <p><%= e.getMessage() == null ? e.getClass().getName() : e.getMessage() %></p>
    </div>
</body>
</html>
<%
}
%>
