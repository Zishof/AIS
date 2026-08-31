<%--
    Adaptor native: Rekap Publikasi Ilmiah / Jurnal

    Sumber menu : ais.action.report.format1.penelitiandanpengabdian.LaporanArtikel
    Kontrak     : ais.common.newui.laporan.NewUiLaporanUmumController
                  (kunci registri "penelitian_rekap_artikel", template penelitiandanpengabdian/Rekap_Artikel)
    Pemetaan    : laporan berparameter sederhana; filter dideklarasikan server
                  sehingga klien tidak perlu halaman khusus.
    PENTING     : nama berkas WAJIB mengikuti nama kelas ZK (tanpa akhiran
                  Window/Action), karena itulah kunci pencocokan resolver.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "root/report");
request.setAttribute("nuiPage", "format1/penelitiandanpengabdian/laporan_artikel");
request.setAttribute("nuiPageTitle", "Rekap Publikasi Ilmiah / Jurnal");
request.setAttribute("nuiPageType", "report");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
