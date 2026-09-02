<%-- Dialihkan dari scaffold ke kontrak laporan umum, 2026-09-02.
Nama dan tipe parameter diambil dari deklarasi <parameter> pada jrxml, bukan
dari nama widget layar ZK.
--%><%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanUmumController" %>
<%
NewUiLaporanUmumController.handle(request, response, "akademik_rekap_alumni_jurusan", "format1/akademik/laporan_rekapitulasi_alumni_jurusan");
%>
