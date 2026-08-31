<%--
    Adaptor native: Laporan Arus Kas

    Sumber menu : ais.action.report.format1.akunting.LaporanAkuntingArusKas
    Kontrak     : ais.common.newui.laporan.NewUiLaporanUmumController
                  (kunci registri "akunting_arus_kas", template akunting/laporan_arus_12_bulan)
    Pemetaan    : laporan ini hanya menyusun beberapa parameter lalu menyerahkan
                  render ke Jasper, sehingga dilayani kontrak laporan generik
                  yang filternya dideklarasikan server. Menambah laporan sejenis
                  cukup satu baris registri; klien tidak perlu diubah.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "root/report");
request.setAttribute("nuiPage", "format1/akunting/laporan_akunting_arus_kas");
request.setAttribute("nuiPageTitle", "Laporan Arus Kas");
request.setAttribute("nuiPageType", "report");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
