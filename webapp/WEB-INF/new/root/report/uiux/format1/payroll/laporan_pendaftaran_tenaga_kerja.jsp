<%--
    Adaptor native: Pendaftaran Tenaga Kerja

    Sumber menu : ais.action.report.format1.payroll.LaporanPendaftaranTenagaKerja
    Kontrak     : ais.common.newui.laporan.NewUiLaporanUmumController
                  (kunci registri "payroll_pendaftaran_tenaga_kerja", template payroll/Laporan_Pendaftaran_Tenaga_Kerja)
    Pemetaan    : laporan ini hanya menyusun beberapa parameter lalu menyerahkan
                  render ke Jasper, sehingga dilayani kontrak laporan generik
                  yang filternya dideklarasikan server. Menambah laporan sejenis
                  cukup satu baris registri; klien tidak perlu diubah.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "root/report");
request.setAttribute("nuiPage", "format1/payroll/laporan_pendaftaran_tenaga_kerja");
request.setAttribute("nuiPageTitle", "Pendaftaran Tenaga Kerja");
request.setAttribute("nuiPageType", "report");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
