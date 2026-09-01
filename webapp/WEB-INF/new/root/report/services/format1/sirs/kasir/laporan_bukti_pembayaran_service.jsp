<%--
    Adaptor native: Laporan Bukti Pembayaran

    Sumber menu : ais.action.report.format1.sirs.kasir.LaporanBuktiPembayaranWindow
    Kontrak     : ais.common.newui.laporan.NewUiLaporanSirsController
                  (kunci "struk_pembayaran", template sirs/struk_pembayaran)
    Pemetaan    : satu pembayaran yang dipilih; nama pasien dan nomor rekam
                  medis diturunkan darinya, tidak diminta terpisah.
    Catatan     : laporan ini pernah dinyatakan tidak dapat dikonversi karena
                  "kelasnya tidak ada di repositori ini". Anggapan itu keliru --
                  kelasnya ada; yang saya cari dulu adalah nama paket yang saya
                  tebak sendiri, bukan nama kelasnya.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanSirsController" %>
<%
NewUiLaporanSirsController.handle(request, response, "struk_pembayaran", "format1/sirs/kasir/laporan_bukti_pembayaran");
%>
