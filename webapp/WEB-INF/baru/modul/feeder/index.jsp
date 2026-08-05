<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common"%>
<%@ page import="ais.common.MobileHubHelper"%>
<%@ page import="ais.database.model.Tbmuser"%>
<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || !tbmuser.getAktif()) { return; }
    if (!Common.getApakahAdminBolehAksesFeeder()) { return; }
    String ctx = request.getContextPath();
    boolean isMob = MobileHubHelper.isMobile(request);

    String[][] menus = {
        {"pagesmasterexportfromfeederzul",  "cloud-arrow-up",  "#3b82f6", "Export ke Feeder",   "Kirim data akademik ke Feeder Dikti"},
        {"pagesmasterimportfromfeederzul",  "cloud-arrow-down","#10b981", "Import dari Feeder", "Ambil data referensi dari Feeder"},
        {"pagesmasterfeederlogzul",         "clipboard-list",  "#f59e0b", "Log Feeder",         "Riwayat & status sinkronisasi"}
    };
%>
<% if (isMob) { %><%= MobileHubHelper.buildMobCss() %><% } %>
<%= MobileHubHelper.buildHeader(
    Common.getBahasaConfig("Neo Feeder"),
    Common.getBahasaConfig("Sinkronisasi Data ke Sistem Feeder Nasional"),
    "cloud-arrow-up", "linear-gradient(135deg,#0ea5e9,#0284c7)") %>
<div class="container-fluid pb-3">
    <div class="row g-3">
    <% for (int i = 0; i < menus.length; i++) {
        String[] m = menus[i]; %>
    <%= MobileHubHelper.buildCard(
        Common.getBahasaConfig(m[3]),
        ctx+"/baru?p="+m[0],
        m[1], m[2],
        Common.getBahasaConfig(m[4]), "col-12 col-md-4") %>
    <% } %>
    </div>
    <%= MobileHubHelper.buildInfoAlert(Common.getBahasaConfig(
        "Feeder digunakan untuk sinkronisasi data akademik dengan sistem nasional (PDDikti). " +
        "Pastikan koneksi internet tersedia sebelum melakukan sinkronisasi.")) %>
</div>
<% if (isMob) { %><%= MobileHubHelper.buildMobScript(".fw-semibold") %><% } %>
