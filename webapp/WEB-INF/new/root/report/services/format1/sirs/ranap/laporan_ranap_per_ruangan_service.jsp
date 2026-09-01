<%--
    Adaptor native: Laporan Ranap Per Ruangan

    Sumber menu : ais.action.report.format1.sirs.ranap.LaporanRanapPerRuangan
    Kontrak     : ais.common.newui.laporan.NewUiLaporanSirsController
                  (kunci "ranap_laporan_perruangan", template sirs/ranap_laporan_perruangan)
    Pemetaan    : laporan periodik dengan tambahan status pendaftaran.
                  Ketiga pilihan statusnya diambil dari konstanta Pendaftaran,
                  bukan ditulis ulang: teksnya masuk ke parameter laporan apa
                  adanya sehingga salah satu huruf menghasilkan laporan kosong.
    Catatan     : laporan ini pernah dinyatakan tidak dapat dikonversi karena
                  "kelasnya tidak ada di repositori ini". Anggapan itu keliru --
                  kelasnya ada; yang saya cari dulu adalah nama paket yang saya
                  tebak sendiri, bukan nama kelasnya.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanSirsController" %>
<%
NewUiLaporanSirsController.handle(request, response, "ranap_laporan_perruangan", "format1/sirs/ranap/laporan_ranap_per_ruangan");
%>
