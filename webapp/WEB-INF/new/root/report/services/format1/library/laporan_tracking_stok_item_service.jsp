<%--
    Adaptor native: Laporan Tracking Stok Item perpustakaan.

    Kontrak memetakan parameter layar ZK apa adanya: perpustakaan, banyak item,
    mulai, dan sampai. PDF tetap dirender dari template Jasper lama supaya hasil
    cetak web, desktop, dan Android identik.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanUmumController" %>
<%
NewUiLaporanUmumController.handle(request, response, "library_tracking_stok_item", "format1/library/laporan_tracking_stok_item");
%>
