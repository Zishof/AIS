<%--
    Adaptor native: Rekap Penelitian atau Pengabdian

    Sumber menu : ais.action.report.format1.penelitiandanpengabdian.LaporanPenelitian
    Kontrak     : ais.common.newui.laporan.NewUiLaporanUmumController
                  (kunci registri "penelitian_rekap_penelitian", template
                  penelitiandanpengabdian/Rekap_Penelitian)
    Pemetaan    : laporan berparameter sederhana; filter dan nama parameter
                  disalin dari generateParameter() kelas ZK.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanUmumController" %>
<%
NewUiLaporanUmumController.handle(request, response, "penelitian_rekap_penelitian", "format1/penelitiandanpengabdian/laporan_penelitian");
%>
