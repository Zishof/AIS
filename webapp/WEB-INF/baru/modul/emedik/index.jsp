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
        {"emedik_kasir",        "pagesmastersirspembayaranzul",               "cash-register",    "#2563eb", "Kasir eMedik",       "Pembayaran layanan dan tindakan medis"},
        {"emedik_pendaftaran",  "pagesmastersirsbookingregistrasizul",        "calendar-check",   "#0891b2", "Booking Pasien",     "Booking dan antrean registrasi pasien"},
        {"emedik_pendaftaran",  "pagesmastersirspendaftaranrawatjalanzul",    "user-plus",        "#059669", "Pendaftaran Pasien", "Registrasi pelayanan rawat jalan"},
        {"emedik_tagihan",      "pagesmastersirsstatuspembayaranzul",         "file-invoice",     "#7c3aed", "Tagihan",            "Status dan pengelolaan tagihan pasien"},
        {"emedik_deposit",      "pagesmastersirsdepositzul",                  "wallet",           "#d97706", "Deposit Pasien",     "Penerimaan dan penggunaan deposit"},
        {"emedik_penjamin",     "pagesmastersirsasuransizul",                 "hand-holding-heart","#db2777", "Penjamin & Asuransi","Master penjamin dan asuransi pasien"},
        {"emedik_laporan",      "pagesmastersirsstatuspembayaranzul",         "chart-line",       "#0f766e", "Laporan eMedik",     "Monitoring pelayanan dan pembayaran"}
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
<%= MobileHubHelper.buildHeader("eMedik", "Pendaftaran, pelayanan, dan pembayaran pasien", "clinic-medical", "linear-gradient(135deg,#059669,#0891b2)") %>
<div class="container-fluid pb-3">
    <% if (jumlahMenu == 0) { %>
    <div class="alert alert-warning border-0 shadow-sm">Role ini belum memiliki hak akses menu eMedik. Atur hak akses eBisnis pada Grup Pengguna.</div>
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
