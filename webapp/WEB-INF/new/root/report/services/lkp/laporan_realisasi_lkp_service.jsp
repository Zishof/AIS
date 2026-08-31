<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.kinerja.NewUiLaporanKinerjaController" %>
<%
// Kontrak native laporan kinerja; parameter Jasper disusun di Java, bukan JSP.
NewUiLaporanKinerjaController.handle(request, response,
        NewUiLaporanKinerjaController.JENIS_LKP, "lkp/laporan_realisasi_lkp");
%>
