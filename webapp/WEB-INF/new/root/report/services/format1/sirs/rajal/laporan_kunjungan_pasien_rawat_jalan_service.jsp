<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanSirsController" %>
<%
NewUiLaporanSirsController.handle(request, response, "rajal_periode", "format1/sirs/rajal/laporan_kunjungan_pasien_rawat_jalan");
%>
