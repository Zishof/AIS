<%--
    Adaptor native: Saldo Item Awal

    Sumber menu : ais.action.report.format1.library.LaporanSaldoAwal
    Kontrak     : ais.common.newui.laporan.NewUiLaporanUmumController
                  (kunci registri "library_saldo_awal", template library/saldo_awal_semua)
    Pemetaan    : laporan ini hanya menyusun beberapa parameter lalu menyerahkan
                  render ke Jasper, sehingga dilayani kontrak laporan generik
                  yang filternya dideklarasikan server. Menambah laporan sejenis
                  cukup satu baris registri; klien tidak perlu diubah.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "root/report");
request.setAttribute("nuiPage", "format1/library/laporan_saldo_awal");
request.setAttribute("nuiPageTitle", "Saldo Item Awal");
request.setAttribute("nuiPageType", "report");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
