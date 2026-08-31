<%--
    Adaptor native: Statistik Surat Masuk dan Keluar

    Sumber menu : ais.action.report.format1.surat.LaporanStatistikSuratMasukDanSuratKeluar
    Kontrak     : ais.common.newui.laporan.NewUiLaporanUmumController
                  (kunci registri "surat_statistik", template surat/laporan_statistic_surat_masuk_dan_keluar)
    Pemetaan    : laporan ini hanya menyusun beberapa parameter lalu menyerahkan
                  render ke Jasper, sehingga dilayani kontrak laporan generik
                  yang filternya dideklarasikan server. Menambah laporan sejenis
                  cukup satu baris registri; klien tidak perlu diubah.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanUmumController" %>
<%
NewUiLaporanUmumController.handle(request, response, "surat_statistik", "format1/surat/laporan_statistik_surat_masuk_dan_surat_keluar");
%>
