<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanSirsController" %>
<%
NewUiLaporanSirsController.handle(request, response, "laporan_kasir_per_shift", "format1/sirs/kasir/laporan_pendapatan_kasir_per_shift");
%>
