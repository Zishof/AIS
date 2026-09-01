<%--
    Adaptor native: Laporan Data Pasien Rawat Inap

    Sumber menu : ais.action.report.format1.sirs.ranap.LaporanDataPasienRawatInap
    Kontrak     : ais.common.newui.laporan.NewUiLaporanSirsController
                  (kunci "data_pasien_rawat_inap", template sirs/data_pasien_rawat_inap)
    Pemetaan    : laporan periodik -- tahun dan bulan saja, persis seperti
                  generateParameter() layar lama. Tidak ada entitas yang
                  perlu dipilih lebih dulu.
    Catatan     : laporan ini pernah dinyatakan tidak dapat dikonversi karena
                  "kelasnya tidak ada di repositori ini". Anggapan itu keliru --
                  kelasnya ada; yang saya cari dulu adalah nama paket yang saya
                  tebak sendiri, bukan nama kelasnya.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanSirsController" %>
<%
NewUiLaporanSirsController.handle(request, response, "data_pasien_rawat_inap", "format1/sirs/ranap/laporan_data_pasien_rawat_inap");
%>
