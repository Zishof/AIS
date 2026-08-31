<%--
    Adaptor native: Pengembalian Per Anggota

    Sumber menu : ais.action.report.format1.library.LaporanPengembalianPerAnggota
    Kontrak     : ais.common.newui.laporan.NewUiLaporanUmumController
                  (kunci registri "library_pengembalian_per_anggota", template library/pengembalian_per_anggota_pengadaan_semua)
    Pemetaan    : laporan ini hanya menyusun beberapa parameter lalu menyerahkan
                  render ke Jasper, sehingga dilayani kontrak laporan generik
                  yang filternya dideklarasikan server. Menambah laporan sejenis
                  cukup satu baris registri; klien tidak perlu diubah.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanUmumController" %>
<%
NewUiLaporanUmumController.handle(request, response, "library_pengembalian_per_anggota", "format1/library/laporan_pengembalian_per_anggota");
%>
