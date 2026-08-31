<%--
    Adaptor native: Daftar Lembur Pegawai

    Sumber menu : ais.action.report.format1.payroll.LaporanLemburPegawai
    Kontrak     : ais.common.newui.laporan.NewUiLaporanUmumController
                  (kunci registri "payroll_lembur", template payroll/Laporan_Lembur_Pegawai)
    Pemetaan    : laporan ini hanya menyusun beberapa parameter lalu menyerahkan
                  render ke Jasper, sehingga dilayani kontrak laporan generik
                  yang filternya dideklarasikan server. Menambah laporan sejenis
                  cukup satu baris registri; klien tidak perlu diubah.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "root/report");
request.setAttribute("nuiPage", "format1/payroll/laporan_lembur_pegawai");
request.setAttribute("nuiPageTitle", "Daftar Lembur Pegawai");
request.setAttribute("nuiPageType", "report");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
