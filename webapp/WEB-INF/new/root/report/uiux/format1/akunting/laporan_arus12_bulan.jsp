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
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "root/report");
request.setAttribute("nuiPage", "format1/akunting/laporan_arus12_bulan");
request.setAttribute("nuiPageTitle", "Laporan Arus Kas");
request.setAttribute("nuiPageType", "report");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
