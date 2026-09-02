<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanKursusController" %>
<%
NewUiLaporanKursusController.handle(request, response, "format1/kursus/laporan_rekap_kursus");
%>
