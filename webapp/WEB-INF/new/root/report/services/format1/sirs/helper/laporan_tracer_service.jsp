<%--
    Adaptor native: Laporan Tracer Pasien

    Sumber menu : ais.action.report.format1.sirs.helper.LaporanTracerWindow
    Kontrak     : ais.common.newui.laporan.NewUiLaporanSirsController
                  (kunci "tracer_pasien", template sirs/tracer_pasien)
    Pemetaan    : satu pendaftaran RAWAT JALAN; barcode kodenya ditulis ke
                  /report/temp lalu dioper ke Jasper sebagai URL http, bentuk
                  yang dipertahankan apa adanya dari layar lama karena
                  templatenya mengharapkan parameter bertipe URL.
    Catatan     : laporan ini pernah dinyatakan tidak dapat dikonversi karena
                  "kelasnya tidak ada di repositori ini". Anggapan itu keliru --
                  kelasnya ada; yang saya cari dulu adalah nama paket yang saya
                  tebak sendiri, bukan nama kelasnya.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanSirsController" %>
<%
NewUiLaporanSirsController.handle(request, response, "tracer_pasien", "format1/sirs/helper/laporan_tracer");
%>
