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
        {"pagesmastermahasiswazul",                   "user-graduate",   "#3b82f6", "Data Mahasiswa",      "Master data & profil mahasiswa"},
        {"pagesmastermahasiswaikutiperkuliahanzul",    "chalkboard-user", "#10b981", "Ikuti Perkuliahan",   "Daftarkan mahasiswa ke perkuliahan"},
        {"pagesmastermahasiswaregistrasiwisudazul",    "graduation-cap",  "#f59e0b", "Registrasi Wisuda",   "Pendaftaran & verifikasi wisuda"},
        {"pagesmastermahasiswarequesttugasakhirzul",   "book-open",       "#8b5cf6", "Request Tugas Akhir", "Pengajuan tugas akhir / skripsi"},
        {"pagesmastermahasiswakonversizul",            "exchange-alt",    "#ef4444", "Konversi Mahasiswa",  "Konversi data mahasiswa pindahan"},
        {"pagesmastermahasiswaupdatenilaikonversizul", "star-half-alt",   "#06b6d4", "Update Nilai Konversi","Perbarui nilai hasil konversi"}
    };
%>
<% if (isMob) { %><%= MobileHubHelper.buildMobCss() %><% } %>
<%= MobileHubHelper.buildHeader(
    Common.getBahasaConfig("Mahasiswa"),
    Common.getBahasaConfig("Manajemen Data Mahasiswa"),
    "user-graduate", "linear-gradient(135deg,#8b5cf6,#6d28d9)") %>
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
