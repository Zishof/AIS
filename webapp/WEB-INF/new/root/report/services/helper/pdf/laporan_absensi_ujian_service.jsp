<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanDaftarHadirUjianController" %>
<%
NewUiLaporanDaftarHadirUjianController.handle(request, response, "helper/pdf/laporan_absensi_ujian");
%>
