<%--
    Adaptor native: Kewaspadaan Kadaluarsa (Farmasi)

    Sumber ZK   : /pages/master/sirs/dashboard_kadaluarsa_farmasi.zul (KadaluarsaFarmasiDashboardAction)
    Kontrak     : ais.common.newui.sirs.NewUiDasborSirsController (mode kadaluarsa)
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
request.setAttribute("nuiPage", "chart/kadaluarsa_farmasi_dashboard");
request.setAttribute("nuiPageTitle", "Kewaspadaan Kadaluarsa (Farmasi)");
request.setAttribute("nuiPageType", "dashboard");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
