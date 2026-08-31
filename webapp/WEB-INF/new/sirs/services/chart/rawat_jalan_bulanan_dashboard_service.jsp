<%--
    Adaptor native: Kunjungan Rawat Jalan (Bulanan)

    Sumber ZK   : /pages/master/sirs/dashboard_rawat_jalan_bulanan.zul (RawatJalanBulananDashboardAction)
    Kontrak     : ais.common.newui.sirs.NewUiDasborSirsController (mode rawat_jalan_bulanan)
    Catatan     : kontrak mengirim ANGKA, bukan HTML. Layar ZK merangkai grafik
                  sebagai HTML/CSS lalu menyuntikkannya ke satu wadah; klien
                  native menggambar sendiri dari angka yang sama. Agregasinya
                  tidak disalin — controller memanggil bagian data pada builder
                  dasbor yang juga dipakai layar ZK.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.sirs.NewUiDasborSirsController" %>
<%
NewUiDasborSirsController.handle(request, response,
        NewUiDasborSirsController.MODE_RAWAT_JALAN_BULANAN, "chart/rawat_jalan_bulanan_dashboard");
%>
