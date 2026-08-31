<%--
    Adaptor native: Ringkasan Pendaftaran

    Sumber ZK   : /pages/master/sirs/dashboard_pendaftaran_overview.zul (PendaftaranOverviewDashboardAction)
    Kontrak     : ais.common.newui.sirs.NewUiDasborSirsController (mode pendaftaran)
    Catatan     : kontrak mengirim ANGKA, bukan HTML. Layar ZK merangkai grafik
                  sebagai HTML/CSS lalu menyuntikkannya ke satu wadah; klien
                  native menggambar sendiri dari angka yang sama. Agregasinya
                  tidak disalin — controller memanggil bagian data pada builder
                  dasbor yang juga dipakai layar ZK.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "sirs");
request.setAttribute("nuiModuleLabel", "SIRS / Klinik / Rumah Sakit");
request.setAttribute("nuiPage", "chart/pendaftaran_overview_dashboard");
request.setAttribute("nuiPageTitle", "Ringkasan Pendaftaran");
request.setAttribute("nuiPageType", "dashboard");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
