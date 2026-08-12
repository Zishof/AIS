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
        {"apotik_kasir",       "pagesmastersirstransaksizul",                 "cash-register",  "#2563eb", "Kasir Apotik",       "Penjualan obat dan pembayaran"},
        {"apotik_resep",       "pagesmastersirstransaksizul",                 "prescription",   "#0891b2", "Tebus Resep",         "Pelayanan dan penebusan resep dokter"},
        {"apotik_racikan",     "pagesmastersirsracikanzul",                   "mortar-pestle",  "#7c3aed", "Racikan",             "Formula dan pembuatan obat racikan"},
        {"apotik_formularium", "pagesmastersirsitemzul",                      "pills",          "#059669", "Formularium & Obat",  "Master obat, harga, dan formularium"},
        {"apotik_batch",       "pagesmastersirsmonitorkadaluarsaitemzul",     "calendar-times", "#dc2626", "Batch & Kedaluwarsa", "Pemantauan batch dan masa kedaluwarsa"},
        {"apotik_pengadaan",   "pagesmastersirspermintaanpembelianzul",       "truck-loading",  "#d97706", "Pengadaan / PBF",     "Permintaan dan proses pengadaan obat"},
        {"apotik_stok_opname", "pagesmastersirskoreksiitemzul",               "boxes",          "#4f46e5", "Stok Opname",        "Koreksi dan pencocokan stok apotik"},
        {"apotik_retur",       "pagesmastersirstransaksireturzul",            "undo-alt",       "#db2777", "Retur Obat",         "Retur transaksi dan persediaan obat"},
        {"apotik_narkotika",   "pagesmastersirsitemzul",                      "shield-alt",     "#9333ea", "Obat Terkendali",    "Data narkotika dan psikotropika"},
        {"apotik_laporan",     "pagesmastersirsmonitorkadaluarsaitemzul",     "chart-bar",      "#0f766e", "Laporan Apotik",     "Monitoring dan laporan operasional apotik"}
    };
    int jumlahMenu = 0;
    for (int i = 0; i < menus.length; i++) if (menu.optBoolean(menus[i][0], false)) jumlahMenu++;
%>
<% if (directPage) { %>
<jsp:include page="/WEB-INF/baru/include/header.jsp" />
<body style="background:#f3f5f9;">
<main class="main" id="top"><div class="container-fluid py-3">
<% } %>
<% if (isMob) { %><%= MobileHubHelper.buildMobCss() %><% } %>
<%= MobileHubHelper.buildHeader("POS Apotik", "Penjualan, resep, dan persediaan farmasi", "prescription-bottle-alt", "linear-gradient(135deg,#2563eb,#0891b2)") %>
<div class="container-fluid pb-3">
    <% if (jumlahMenu == 0) { %>
    <div class="alert alert-warning border-0 shadow-sm">Role ini belum memiliki hak akses menu POS Apotik. Atur hak akses eBisnis pada Grup Pengguna.</div>
    <% } else { %>
    <div class="row g-3">
        <% for (int i = 0; i < menus.length; i++) { String[] m = menus[i]; if (!menu.optBoolean(m[0], false)) continue; %>
        <%= MobileHubHelper.buildCard(m[4], ctx + "/baru?p=" + m[1], m[2], m[3], m[5]) %>
        <% } %>
    </div>
    <% } %>
</div>
<% if (isMob) { %><%= MobileHubHelper.buildMobScript(".fw-semibold") %><% } %>
<% if (directPage) { %>
</div></main><jsp:include page="/WEB-INF/baru/include/foot.jsp" />
</body></html>
<% } %>
