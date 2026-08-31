<%--
    Adaptor native: Realisasi Anggaran Per Jenis Item

    Sumber menu : ais.action.report.format1.rab.LaporanRealisasiAnggaranPerJenisWorkspace
    Kontrak     : ais.common.newui.laporan.NewUiLaporanUmumController
                  (kunci registri "rab_realisasi_per_jenis", template rab/Realisasi_Anggaran_Per_Jenis_Item_Bulanan)
    Pemetaan    : laporan berparameter sederhana; filter dideklarasikan server
                  sehingga klien tidak perlu halaman khusus.
    PENTING     : nama berkas WAJIB mengikuti nama kelas ZK (tanpa akhiran
                  Window/Action), karena itulah kunci pencocokan resolver.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanUmumController" %>
<%
NewUiLaporanUmumController.handle(request, response, "rab_realisasi_per_jenis", "format1/rab/laporan_realisasi_anggaran_per_jenis_workspace");
%>
