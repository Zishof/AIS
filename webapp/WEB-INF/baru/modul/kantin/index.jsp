<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common"%>
<%@ page import="ais.common.MobileHubHelper"%>
<%@ page import="ais.database.model.Tbmrole"%>
<%@ page import="ais.database.model.Tbmuser"%>
<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || !tbmuser.getAktif()) { return; }
    Tbmrole role = tbmuser.hakAkses();
    String ctx = request.getContextPath();
    boolean isMob = MobileHubHelper.isMobile(request);
    boolean directPage = Boolean.TRUE.equals(request.getAttribute("kantinDirectPage"));
    boolean anggotaKoperasi = tbmuser.getAnggotaKoperasi() != null && tbmuser.getPedagang() == null;

    if ((role == null || !Boolean.TRUE.equals(role.getKantin())) && anggotaKoperasi) {
        if (directPage) {
%>
<jsp:include page="/WEB-INF/baru/include/header.jsp" />
<body>
    <main class="main" id="top">
        <div class="container-fluid">
            <div class="content">
                <jsp:include page="/WEB-INF/baru/include/navbar.jsp" />
                <jsp:include page="/WEB-INF/baru/modul/kantin/beranda.jsp" />
                <jsp:include page="/WEB-INF/baru/include/footer.jsp" />
            </div>
            <jsp:include page="/WEB-INF/baru/include/dialog-modal.jsp" />
        </div>
    </main>
    <jsp:include page="/WEB-INF/baru/include/foot.jsp" />
</body>
</html>
<%
        } else {
%>
<jsp:include page="/WEB-INF/baru/modul/kantin/beranda.jsp" />
<%
        }
        return;
    }

    if (role == null || !Boolean.TRUE.equals(role.getKantin())) { return; }

    String[][] menus = {
        {"pagesmasterkantinkaskasirzul",         "cash-register", "#3b82f6", "Kas Kasir",        "Transaksi penjualan & pembayaran", "kas"},
        {"pagesmasterkantinpergudanganzul",      "box-open",      "#10b981", "Pergudangan",      "Stok & manajemen bahan makanan",   "stok"},
        {"pagesmasterkantinsetorantenantzul",    "money-bill",    "#f59e0b", "Setoran Tenant",   "Setoran & tagihan tenant kantin",  "tenant"},
        {"pagesmasterkantinlaporankeuanganzul",  "file-invoice",  "#8b5cf6", "Laporan Keuangan", "Laporan & rekap keuangan kantin",  "laporan"}
    };
%>
<% if (directPage) { %>
<jsp:include page="/WEB-INF/baru/include/header.jsp" />
<body style="background:#f3f5f9;">
    <main class="main" id="top">
        <div class="container-fluid py-3">
<% } %>
<% if (isMob) { %><%= MobileHubHelper.buildMobCss() %><% } %>
<%= MobileHubHelper.buildHeader(
    Common.getBahasaConfig("Kantin"),
    Common.getBahasaConfig("Manajemen Kantin & Penjualan"),
    "utensils", "linear-gradient(135deg,#f97316,#ea580c)") %>
<div class="container-fluid pb-3">
    <div class="row g-3">
    <% for (int i = 0; i < menus.length; i++) {
        String[] m = menus[i];
        boolean tampil = true;
        org.json.JSONObject menuTersimpanKantinIdx = ais.common.EbisnisMenuKatalog.urai(role == null ? null : role.getEbisnisMenu()).optJSONObject("menu");
        if ("kas".equals(m[5])) tampil = menuTersimpanKantinIdx.optBoolean("kaskasir", true);
        if ("stok".equals(m[5])) tampil = menuTersimpanKantinIdx.optBoolean("stokopname", true);
        if ("tenant".equals(m[5])) tampil = menuTersimpanKantinIdx.optBoolean("setorantenant", true);
        if ("laporan".equals(m[5])) tampil = menuTersimpanKantinIdx.optBoolean("laporan", true);
        if (!tampil) continue;
    %>
    <%= MobileHubHelper.buildCard(
        Common.getBahasaConfig(m[3]),
        "kas".equals(m[5]) ? ctx+"/baru?p=kantin&s=kas" : ctx+"/baru?p="+m[0],
        m[1], m[2],
        Common.getBahasaConfig(m[4])) %>
    <% } %>
    </div>
</div>
<% if (isMob) { %><%= MobileHubHelper.buildMobScript(".fw-semibold") %><% } %>
<% if (directPage) { %>
        </div>
    </main>
    <jsp:include page="/WEB-INF/baru/include/foot.jsp" />
</body>
</html>
<% } %>
