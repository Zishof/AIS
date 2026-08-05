<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common"%>
<%@ page import="ais.common.MobileHubHelper"%>
<%@ page import="ais.database.model.Tbmrole"%>
<%@ page import="ais.database.model.Tbmuser"%>
<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || !tbmuser.getAktif()) { return; }
    Tbmrole role = tbmuser.hakAkses();
    if (role == null || !Boolean.TRUE.equals(role.getDasboardAntarJemput())) { return; }
    String ctx = request.getContextPath();
    boolean isMob = MobileHubHelper.isMobile(request);

    String[][] menus = {
        {"pagesmasterantarjemputpanelkendaraanzul","van-shuttle",   "#3b82f6","Kendaraan",       "Data & armada kendaraan"},
        {"pagesmasterantarjemputpanelrutezul",     "map-marked-alt","#ef4444","Rute",             "Pengaturan rute perjalanan"},
        {"pagesmasterantarjemputpaneljadwalzul",   "calendar-days", "#10b981","Jadwal",           "Jadwal & rencana perjalanan"},
        {"pagesmasterantarjemputpanelpesertazul",  "users",         "#8b5cf6","Peserta",          "Daftar peserta antar jemput"},
        {"pagesmasterantarjemputpanelkartuzul",    "id-card",       "#ec4899","Kartu Penjemput",  "Kartu identitas peserta"},
        {"pagesmasterantarjemputpaneltransaksizul","receipt",       "#06b6d4","Transaksi",        "Histori & biaya transaksi"},
        {"pagesmasterantarjemputpaneldetailzul",   "phone-volume",  "#f97316","Detail Panggilan", "Detail panggilan & penjemputan"},
        {"pagesmasterantarjemputpanellogzul",      "clipboard-list","#64748b","Log Notifikasi",   "Log & aktivitas notifikasi"}
    };
%>
<% if (isMob) { %><%= MobileHubHelper.buildMobCss() %><% } %>
<%= MobileHubHelper.buildHeader(
    Common.getBahasaConfig("Antar Jemput"),
    Common.getBahasaConfig("Manajemen Transportasi & Antar Jemput"),
    "bus", "linear-gradient(135deg,#6366f1,#4f46e5)") %>
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
