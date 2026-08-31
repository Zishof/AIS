<%--
    Adaptor native: Laporan Arus Harian

    Sumber menu : ais.action.report.format1.akunting.LaporanArus31Hari
    Kontrak     : ais.common.newui.laporan.NewUiLaporanUmumController
                  (kunci registri "akunting_arus_harian", template akunting/laporan_arus_31_hari)
    Pemetaan    : laporan berparameter sederhana; filter dideklarasikan server
                  sehingga klien tidak perlu halaman khusus.
    PENTING     : nama berkas WAJIB mengikuti nama kelas ZK (tanpa akhiran
                  Window/Action), karena itulah kunci pencocokan resolver.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanUmumController" %>
<%
NewUiLaporanUmumController.handle(request, response, "akunting_arus_harian", "format1/akunting/laporan_arus31_hari");
%>
