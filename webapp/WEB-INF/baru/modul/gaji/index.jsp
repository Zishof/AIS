<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common"%>
<%@ page import="ais.common.MobileHubHelper"%>
<%@ page import="ais.database.model.Tbmrole"%>
<%@ page import="ais.database.model.Tbmuser"%>
<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || !tbmuser.getAktif()) { return; }
    Tbmrole role = tbmuser.hakAkses();
    if (role == null || !Boolean.TRUE.equals(role.getTampilkanGaji())) { return; }
    String ctx = request.getContextPath();
    boolean isMob = MobileHubHelper.isMobile(request);

    String[][] menus = {
        {"pagesmasterpayrollrencanagajizul",                        "calendar-check",  "#3b82f6", "Rencana Gaji",        "Rencana & struktur gaji pegawai"},
        {"pagesmasterpayrolltransaksipegawaizul",                   "receipt",         "#10b981", "Transaksi Pegawai",   "Transaksi potongan & tunjangan"},
        {"pagesmasterpayrollpembayarangajizul",                     "money-check-alt", "#f59e0b", "Pembayaran Gaji",     "Proses pembayaran gaji bulanan"},
        {"pagesmasterpayrollpengajuanpegawaizul",                   "file-signature",  "#8b5cf6", "Pengajuan Pegawai",   "Pengajuan & klaim pegawai"},
        {"pagesmasterpayrollpersetujuanpengajuantransaksipegawaizul","check-double",   "#ef4444", "Persetujuan",         "Persetujuan transaksi pegawai"},
        {"pagesmasterpayrollpostingtransaksipegawaizul",            "database",        "#06b6d4", "Posting Transaksi",   "Posting transaksi ke jurnal"},
        {"pagesmasterpayrollpengajuantransaksipegawaizul",          "paper-plane",     "#f97316", "Pengajuan Transaksi", "Ajukan transaksi pegawai"},
        {"pagesmasterpayrollparametertambahangajipegawaizul",       "sliders",         "#ec4899", "Parameter Tambahan",  "Konfigurasi parameter gaji"},
        {"pagesmasterpayrollptkppegawaizul",                        "percent",         "#14b8a6", "PTKP Pegawai",        "Penghasilan Tidak Kena Pajak"},
        {"pagesmasterpayrollwaktushiftzul",                         "clock",           "#64748b", "Waktu & Shift",       "Pengaturan shift kerja"}
    };
%>
<% if (isMob) { %><%= MobileHubHelper.buildMobCss() %><% } %>
<%= MobileHubHelper.buildHeader(
    Common.getBahasaConfig("Penggajian"),
    Common.getBahasaConfig("Manajemen Gaji & Transaksi Pegawai"),
    "money-bill-wave", "linear-gradient(135deg,#f59e0b,#d97706)") %>
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
