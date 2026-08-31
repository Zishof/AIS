<%--
    Adaptor native: Okupansi Tempat Tidur

    Sumber ZK   : /pages/master/sirs/dashboard_okupansi_tempat_tidur.zul (OkupansiTempatTidurDashboardAction)
    Kontrak     : ais.common.newui.sirs.NewUiDasborSirsController (mode okupansi)
    Catatan     : kontrak mengirim ANGKA, bukan HTML. Layar ZK merangkai grafik
                  sebagai HTML/CSS lalu menyuntikkannya ke satu wadah; klien
                  native menggambar sendiri dari angka yang sama. Agregasinya
                  tidak disalin — controller memanggil bagian data pada builder
                  dasbor yang juga dipakai layar ZK.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.sirs.NewUiDasborSirsController" %>
<%
NewUiDasborSirsController.handle(request, response,
        NewUiDasborSirsController.MODE_OKUPANSI, "chart/okupansi_tempat_tidur_dashboard");
%>
