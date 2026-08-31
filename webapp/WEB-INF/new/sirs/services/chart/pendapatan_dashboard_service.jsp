<%--
    Adaptor native: Pendapatan Pasien

    Sumber ZK   : /pages/master/sirs/dashboard_pendapatan.zul (PendapatanDashboardAction)
    Kontrak     : ais.common.newui.sirs.NewUiDasborSirsController (mode pendapatan)
    Catatan     : kontrak mengirim ANGKA, bukan HTML. Layar ZK merangkai grafik
                  sebagai HTML/CSS lalu menyuntikkannya ke satu wadah; klien
                  native menggambar sendiri dari angka yang sama. Agregasinya
                  tidak disalin — controller memanggil bagian data pada builder
                  dasbor yang juga dipakai layar ZK.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.sirs.NewUiDasborSirsController" %>
<%
NewUiDasborSirsController.handle(request, response,
        NewUiDasborSirsController.MODE_PENDAPATAN, "chart/pendapatan_dashboard");
%>
