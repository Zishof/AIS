<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common"%>
<%@ page import="ais.common.MobileHubHelper"%>
<%@ page import="ais.database.model.Tbmrole"%>
<%@ page import="ais.database.model.Tbmuser"%>
<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || !tbmuser.getAktif()) { return; }
    Tbmrole role = tbmuser.hakAkses();
    if (role == null || !Boolean.TRUE.equals(role.getDashboardKoperasi())) { return; }
    String ctx = request.getContextPath();
    boolean isMob = MobileHubHelper.isMobile(request);

    String[][] menus = {
        {"pagesmasterkoperasidashboardkoperasizul",     "gauge",          "#3b82f6", "Dashboard",        "Ringkasan & statistik koperasi"},
        {"pagesmasterkoperasianggotakoperasizul",       "users",          "#10b981", "Anggota",          "Kelola anggota koperasi"},
        {"pagesmasterkoperasipembeliananggotakoperasizul","shopping-cart", "#f59e0b", "Pembelian",        "Transaksi pembelian anggota"},
        {"pagesmasterkoperasipembayarananggotakoperasizul","money-bill-wave","#8b5cf6","Pembayaran",      "Pembayaran & cicilan anggota"},
        {"pagesmasterkoperasidashboardsimpanpinjamzul", "piggy-bank",     "#ef4444", "Simpan Pinjam",    "Dashboard simpan pinjam"},
        {"pagesmasterkoperasilaporankeuangankoperasizul","file-invoice",   "#06b6d4", "Laporan Keuangan", "Laporan keuangan koperasi"},
        {"pagesmasterkoperasilaporansimpanpinjamzul",   "chart-line",     "#f97316", "Laporan SP",       "Laporan simpan pinjam"},
        {"pagesmasterkoperasipembagianshuzul",          "divide",         "#ec4899", "Pembagian SHU",    "Perhitungan & distribusi SHU"},
        {"pagesmasterkoperasimodalpenyertaankoperasizul","coins",          "#14b8a6", "Modal Penyertaan", "Kelola modal penyertaan"},
        {"pagesmasterkoperasikoperasizul",              "building-columns","#64748b", "Profil Koperasi",  "Data & profil koperasi"}
    };
%>
<% if (isMob) { %><%= MobileHubHelper.buildMobCss() %><% } %>
<%= MobileHubHelper.buildHeader(
    Common.getBahasaConfig("Koperasi"),
    Common.getBahasaConfig("Manajemen Koperasi & Simpan Pinjam"),
    "handshake", "linear-gradient(135deg,#10b981,#059669)") %>
<div class="container-fluid pb-3">
    <div class="row g-3">
    <% for (int i = 0; i < menus.length; i++) {
        String[] m = menus[i]; %>
    <%= MobileHubHelper.buildCard(
        Common.getBahasaConfig(m[3]),
        ctx+"/baru?p="+m[0],
        m[1], m[2],
        Common.getBahasaConfig(m[4])) %>
    <% } %>
    </div>
</div>
<% if (isMob) { %><%= MobileHubHelper.buildMobScript(".fw-semibold") %><% } %>
