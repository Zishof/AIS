<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanSirsController" %>
<%
NewUiLaporanSirsController.handle(request, response, "umum_data_pasien_periode", "format1/sirs/umum/laporan_data_pasien_per_periode");
%>
