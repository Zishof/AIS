<%--
    Adaptor native: Penerimaan Pengadaan Item

    Sumber menu : ais.action.report.format1.library.LaporanPenerimaan
    Kontrak     : ais.common.newui.laporan.NewUiLaporanUmumController
                  (kunci registri "library_penerimaan_pengadaan", template library/penerimaan_pengadaan_semua)
    Pemetaan    : laporan ini hanya menyusun beberapa parameter lalu menyerahkan
                  render ke Jasper, sehingga dilayani kontrak laporan generik
                  yang filternya dideklarasikan server. Menambah laporan sejenis
                  cukup satu baris registri; klien tidak perlu diubah.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "root/report");
request.setAttribute("nuiPage", "format1/library/laporan_penerimaan");
request.setAttribute("nuiPageTitle", "Penerimaan Pengadaan Item");
request.setAttribute("nuiPageType", "report");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
