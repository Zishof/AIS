<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%
// Laporan Rincian Pembayaran Siswa - kontrak laporan native (form filter +
// varian + render Jasper), paritas class ZK tanpa menampilkan ZUL.
ais.common.newui.sekolah.NewUiLaporanSekolahController.handle(request, response,
        ais.common.newui.sekolah.NewUiLaporanSekolahController.JENIS_RINCIAN,
        "format1/sekolah/laporan_rincian_pembayaran_siswa");
%>
