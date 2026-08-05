<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common"%>
<%@ page import="ais.common.MobileHubHelper"%>
<%@ page import="ais.database.model.Tbmuser"%>
<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || !tbmuser.getAktif()) { return; }
    String ctx = request.getContextPath();
    boolean isMob = MobileHubHelper.isMobile(request);

    String[][] menus = {
        {"pagesmasterkrszul",          "file-alt",        "#3b82f6", "KRS",             "Kartu Rencana Studi mahasiswa"},
        {"pagesmasterkrspaketzul",     "layer-group",     "#10b981", "KRS Paket",       "KRS berdasarkan paket kurikulum"},
        {"pagesmasterkrsnonpaketzul",  "list-ul",         "#f59e0b", "KRS Non-Paket",   "KRS matakuliah bebas pilih"},
        {"pagesmasterkrsspzul",        "calendar-plus",   "#8b5cf6", "KRS SP",          "KRS Semester Pendek"},
        {"pagesmasterkrsremedialzul",  "redo-alt",        "#ef4444", "KRS Remedial",    "KRS pengulangan matakuliah"},
        {"pagesmasterkrskonversizul",  "exchange-alt",    "#06b6d4", "KRS Konversi",    "KRS konversi dari kampus lain"},
        {"pagesmasterkrsmahasiswazul", "user-graduate",   "#f97316", "KRS Mahasiswa",   "Rekap KRS per mahasiswa"},
        {"pagesmasterkrsoldzul",       "history",         "#64748b", "KRS Lama",        "Arsip KRS periode sebelumnya"}
    };
%>
<% if (isMob) { %><%= MobileHubHelper.buildMobCss() %><% } %>
<%= MobileHubHelper.buildHeader(
    Common.getBahasaConfig("KRS"),
    Common.getBahasaConfig("Kartu Rencana Studi Mahasiswa"),
    "file-alt", "linear-gradient(135deg,#3b82f6,#1d4ed8)") %>
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
