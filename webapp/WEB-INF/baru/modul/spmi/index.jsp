<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common"%>
<%@ page import="ais.common.MobileHubHelper"%>
<%@ page import="ais.database.model.Tbmrole"%>
<%@ page import="ais.database.model.Tbmuser"%>
<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || !tbmuser.getAktif()) { return; }
    Tbmrole role = tbmuser.hakAkses();
    if (role == null || !Boolean.TRUE.equals(role.getTampilkanSpmi())) { return; }
    String ctx = request.getContextPath();
    boolean isMob = MobileHubHelper.isMobile(request);

    String[][] menus = {
        {"pagesmasterspmistandarspmizul",   "list-check",  "#3b82f6", "Standar SPMI",  "Kelola standar penjaminan mutu"},
        {"pagesmasterspmiskenariospmizul",  "sitemap",     "#10b981", "Skenario SPMI", "Skenario & alur penilaian mutu"},
        {"pagesmasterspmiindikatorspmizul", "gauge-high",  "#f59e0b", "Indikator",     "Indikator penilaian SPMI"},
        {"pagesmasterspmibutirmutuspmizul", "check-circle","#8b5cf6", "Butir Mutu",    "Butir-butir mutu SPMI"},
        {"pagesmasterspmihasilspmizul",     "chart-bar",   "#ef4444", "Hasil SPMI",    "Hasil & capaian SPMI"},
        {"pagesmasterspmijenisspmizul",     "tags",        "#06b6d4", "Jenis SPMI",    "Jenis & kategori SPMI"}
    };
%>
<% if (isMob) { %><%= MobileHubHelper.buildMobCss() %><% } %>
<%= MobileHubHelper.buildHeader(
    Common.getBahasaConfig("SPMI"),
    Common.getBahasaConfig("Sistem Penjaminan Mutu Internal"),
    "clipboard-check", "linear-gradient(135deg,#3b82f6,#1d4ed8)") %>
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
