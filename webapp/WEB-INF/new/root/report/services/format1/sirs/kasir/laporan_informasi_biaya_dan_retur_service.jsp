<%--
    Adaptor native: Laporan Informasi Biaya dan Retur

    Sumber menu : ais.action.report.format1.sirs.kasir.LaporanInformasiBiayaDanReturWindow
    Kontrak     : ais.common.newui.laporan.NewUiLaporanSirsController
                  (kunci "informasi_biaya_dan_retur", template sirs/informasi_biaya_dan_retur)
    Pemetaan    : sama permukaannya dengan Informasi Tagihan; yang berbeda
                  hanya templatenya.
    Catatan     : laporan ini pernah dinyatakan tidak dapat dikonversi karena
                  "kelasnya tidak ada di repositori ini". Anggapan itu keliru --
                  kelasnya ada; yang saya cari dulu adalah nama paket yang saya
                  tebak sendiri, bukan nama kelasnya.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanSirsController" %>
<%
NewUiLaporanSirsController.handle(request, response, "informasi_biaya_dan_retur", "format1/sirs/kasir/laporan_informasi_biaya_dan_retur");
%>
