<%--
    Adaptor native: Peminjaman Belum Dikembalikan

    Sumber menu : ais.action.report.format1.library.LaporanPeminjamanPerAnggotaBelumDikembalikan
    Kontrak     : ais.common.newui.laporan.NewUiLaporanUmumController
                  (kunci registri "library_peminjaman_belum_kembali", template library/peminjaman_per_anggota_belum_dikembalikan_pengadaan_semua)
    Pemetaan    : laporan ini hanya menyusun beberapa parameter lalu menyerahkan
                  render ke Jasper, sehingga dilayani kontrak laporan generik
                  yang filternya dideklarasikan server. Menambah laporan sejenis
                  cukup satu baris registri; klien tidak perlu diubah.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanUmumController" %>
<%
NewUiLaporanUmumController.handle(request, response, "library_peminjaman_belum_kembali", "format1/library/laporan_peminjaman_per_anggota_belum_dikembalikan");
%>
