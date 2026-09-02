<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanUmumController" %>
<%
NewUiLaporanUmumController.handle(request, response,
        "rab_evaluasi_penetapan_kinerja",
        "format1/rab/laporan_realisasi_program_bulanan_workspace");
%>
