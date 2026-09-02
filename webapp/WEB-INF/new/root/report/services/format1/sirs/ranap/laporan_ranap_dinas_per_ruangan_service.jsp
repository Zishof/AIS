<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanSirsController" %>
<%
NewUiLaporanSirsController.handle(request, response, "laporan_ranap_pasien_dinas", "format1/sirs/ranap/laporan_ranap_dinas_per_ruangan");
%>
