<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanSirsController" %>
<%
NewUiLaporanSirsController.handle(request, response, "umum_biaya_alat_medis", "format1/sirs/umum/laporan_biaya_alat_medis");
%>
