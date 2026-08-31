<%--
    Adaptor native: Laporan Arus Kas

    Sumber menu : ais.action.report.format1.akunting.LaporanArus12Bulan
    Kontrak     : ais.common.newui.laporan.NewUiLaporanUmumController
                  (kunci registri "akunting_arus_kas", template akunting/laporan_arus_12_bulan)
    Pemetaan    : laporan berparameter sederhana; filter dideklarasikan server
                  sehingga klien tidak perlu halaman khusus.
    PENTING     : nama berkas WAJIB mengikuti nama kelas ZK (tanpa akhiran
                  Window/Action), karena itulah kunci pencocokan resolver.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanUmumController" %>
<%
NewUiLaporanUmumController.handle(request, response, "akunting_arus_kas", "format1/akunting/laporan_arus12_bulan");
%>
