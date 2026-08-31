<%--
    Adaptor native: Kunjungan Rawat Jalan (Mingguan)

    Sumber ZK   : /pages/master/sirs/dashboard_rawat_jalan_mingguan.zul (RawatJalanMingguanDashboardAction)
    Kontrak     : ais.common.newui.sirs.NewUiDasborSirsController (mode rawat_jalan_mingguan)
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
request.setAttribute("nuiPage", "chart/rawat_jalan_mingguan_dashboard");
request.setAttribute("nuiPageTitle", "Kunjungan Rawat Jalan (Mingguan)");
request.setAttribute("nuiPageType", "dashboard");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
