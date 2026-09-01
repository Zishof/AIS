<%--
    Adaptor native: Laporan Informasi Tagihan

    Sumber menu : ais.action.report.format1.sirs.kasir.LaporanInformasiTagihanWindow
    Kontrak     : ais.common.newui.laporan.NewUiLaporanSirsController
                  (kunci "informasi_tagihan", template sirs/informasi_tagihan)
    Pemetaan    : laporan per objek -- menuntut pendaftaran, pasien, dan/atau
                  transaksi yang dipilih lebih dulu. Layar lama memakai banbox
                  pencari; jalur native memakai aksi lookup berkata kunci.
    Catatan     : laporan ini pernah dinyatakan tidak dapat dikonversi karena
                  "kelasnya tidak ada di repositori ini". Anggapan itu keliru --
                  kelasnya ada; yang saya cari dulu adalah nama paket yang saya
                  tebak sendiri, bukan nama kelasnya.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanSirsController" %>
<%
NewUiLaporanSirsController.handle(request, response, "informasi_tagihan", "format1/sirs/kasir/laporan_informasi_tagihan");
%>
