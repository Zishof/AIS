<%--
    Adaptor native: Monitor Stok Item

    Sumber menu : ais.action.report.format1.library.LaporanStokItem
    Kontrak     : ais.common.newui.laporan.NewUiLaporanUmumController
                  (kunci registri "library_stok_item", template library/stok_item)
    Pemetaan    : laporan berparameter sederhana; filter dideklarasikan server
                  sehingga klien tidak perlu halaman khusus.
    PENTING     : nama berkas WAJIB mengikuti nama kelas ZK (tanpa akhiran
                  Window/Action), karena itulah kunci pencocokan resolver.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanUmumController" %>
<%
NewUiLaporanUmumController.handle(request, response, "library_stok_item", "format1/library/laporan_stok_item");
%>
