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
        {"pagesmasterrecruitmentcalonpegawaizul",                       "user-plus",       "#3b82f6", "Calon Pegawai",          "Data pendaftar calon pegawai"},
        {"pagesmasterrecruitmentgelombangpendaftarancalonpegawaizul",    "calendar-alt",    "#10b981", "Gelombang Pendaftaran",  "Periode & gelombang rekrutmen"},
        {"pagesmasterrecruitmentkelompokpendaftaranpegawaizul",          "layer-group",     "#f59e0b", "Kelompok Pendaftaran",   "Kelompok posisi yang dibuka"},
        {"pagesmasterrecruitmentkelompokparametertambahancalonpegawaizul","sliders",        "#8b5cf6", "Parameter Tambahan",     "Konfigurasi parameter rekrutmen"},
        {"pagesmasterrecruitmentparameterverifikasicalonpegawaizul",     "list-check",      "#ef4444", "Parameter Verifikasi",   "Kriteria verifikasi berkas"},
        {"pagesmasterrecruitmentverifikasikelengkapancalonpegawaizul",   "clipboard-check", "#06b6d4", "Verifikasi Kelengkapan", "Cek kelengkapan dokumen pendaftar"},
        {"pagesmasterrecruitmentcalonpegawaipunyaverifikasidokumenzul",  "file-circle-check","#f97316","Verifikasi Dokumen",     "Status verifikasi dokumen pendaftar"}
    };
%>
<% if (isMob) { %><%= MobileHubHelper.buildMobCss() %><% } %>
<%= MobileHubHelper.buildHeader(
    Common.getBahasaConfig("Rekrutmen Pegawai"),
    Common.getBahasaConfig("Manajemen Seleksi & Penerimaan Pegawai"),
    "user-tie", "linear-gradient(135deg,#0ea5e9,#0369a1)") %>
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
