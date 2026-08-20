<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common"%>
<%@ page import="ais.common.MobileHubHelper"%>
<%@ page import="ais.database.model.Tbmrole"%>
<%@ page import="ais.database.model.Tbmuser"%>
<%@ page import="org.json.JSONObject"%>
<%
    String ctx = request.getContextPath();
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null) {
        response.sendRedirect(ctx + "/login");
        return;
    }
    if (!tbmuser.getAktif()) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return;
    }
    Tbmrole role = tbmuser.hakAkses();
    JSONObject hak = ais.common.EbisnisMenuKatalog.urai(role == null ? null : role.getEbisnisMenu());
    JSONObject menu = hak.optJSONObject("menu");
    if (menu == null) menu = new JSONObject();
    boolean isMob = MobileHubHelper.isMobile(request);
    boolean directPage = Boolean.TRUE.equals(request.getAttribute("posDirectPage"));
    String[][] menus = {
        {"apotik_kasir",       "kasir",       "cash-register",  "#2563eb", "Kasir Apotik",       "Penjualan obat dan pembayaran"},
        {"apotik_resep",       "resep",       "prescription",   "#0891b2", "Tebus Resep",         "Pelayanan dan penebusan resep dokter"},
        {"apotik_racikan",     "racikan",     "mortar-pestle",  "#7c3aed", "Racikan",             "Formula dan pembuatan obat racikan"},
        {"apotik_formularium", "formularium", "pills",          "#059669", "Formularium & Obat",  "Master obat, harga, dan formularium"},
        {"apotik_batch",       "batch",       "calendar-times", "#dc2626", "Batch & Kedaluwarsa", "Pemantauan batch dan masa kedaluwarsa"},
        {"apotik_pengadaan",   "pengadaan",   "truck-loading",  "#d97706", "Pengadaan / PBF",     "Penerimaan dan proses pengadaan obat"},
        {"apotik_stok_opname", "stok_opname", "boxes",          "#4f46e5", "Stok Opname",        "Koreksi dan pencocokan stok apotik"},
        {"apotik_retur",       "retur",       "undo-alt",       "#db2777", "Retur Obat",         "Retur transaksi dan persediaan obat"},
        {"apotik_narkotika",   "narkotika",   "shield-alt",     "#9333ea", "Obat Terkendali",    "Register narkotika dan psikotropika"},
        {"apotik_laporan",     "laporan",     "chart-bar",      "#0f766e", "Laporan Apotik",     "Penjualan, obat terkendali, dan kedaluwarsa"}
    };
    int jumlahMenu = 0;
    String firstHelpMenu = null;
    for (int i = 0; i < menus.length; i++) if (menu.optBoolean(menus[i][0], false)) { jumlahMenu++; if (firstHelpMenu == null) firstHelpMenu = menus[i][0]; }
%>
<% if (directPage) { %>
<jsp:include page="/WEB-INF/baru/include/header.jsp" />
<body style="background:#f3f5f9;">
<main class="main" id="top"><div class="container-fluid py-3">
<% } %>
<% if (isMob) { %><%= MobileHubHelper.buildMobCss() %><% } %>
<%= MobileHubHelper.buildHeader("POS Apotik", "Penjualan, resep, dan persediaan farmasi", "prescription-bottle-alt", "linear-gradient(135deg,#2563eb,#0891b2)") %>
<div class="container-fluid pb-3">
    <% if (firstHelpMenu != null) { %><div class="text-end mb-3"><a class="btn btn-success me-2" href="<%=ctx%>/baru?p=apotik&amp;s=help&amp;mode=qa&amp;menu=<%=firstHelpMenu%>"><i class="fas fa-comments"></i> Tanya Jawab</a><a class="btn btn-warning" href="<%=ctx%>/baru?p=apotik&amp;s=help&amp;menu=<%=firstHelpMenu%>"><i class="fas fa-question-circle"></i> Bantuan</a></div><% } %>
    <% if (jumlahMenu == 0) { %>
    <div class="alert alert-warning border-0 shadow-sm">Role ini belum memiliki hak akses menu POS Apotik. Atur hak akses eBisnis pada Grup Pengguna.</div>
    <% } else { %>
    <div class="row g-3">
        <% for (int i = 0; i < menus.length; i++) { String[] m = menus[i]; if (!menu.optBoolean(m[0], false)) continue; %>
        <%= MobileHubHelper.buildCard(m[4], ctx + "/baru?p=apotik&s=" + m[1], m[2], m[3], m[5]) %>
        <% } %>
    </div>
    <% } %>
</div>
<% if (isMob) { %><%= MobileHubHelper.buildMobScript(".fw-semibold") %><% } %>
<% if (directPage) { %>
</div></main><jsp:include page="/WEB-INF/baru/include/foot.jsp" />
<jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp"/>
</body></html>
<% } %>
