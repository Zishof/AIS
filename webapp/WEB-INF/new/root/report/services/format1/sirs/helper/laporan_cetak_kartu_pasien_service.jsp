<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanSirsController" %>
<%
NewUiLaporanSirsController.handle(request, response, "helper_kartu_pasien", "format1/sirs/helper/laporan_cetak_kartu_pasien");
%>
