<%-- Dialihkan dari scaffold ke kontrak laporan umum, 2026-09-02.
Nama dan tipe parameter diambil dari deklarasi <parameter> pada jrxml, bukan
dari nama widget layar ZK.
--%><%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanUmumController" %>
<%
NewUiLaporanUmumController.handle(request, response, "akunting_laporan_akun", "format1/akunting/laporan_akun");
%>
