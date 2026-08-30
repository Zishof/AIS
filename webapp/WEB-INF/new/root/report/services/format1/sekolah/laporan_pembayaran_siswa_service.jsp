<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%
// Laporan Pembayaran Siswa - kontrak laporan native (form filter + varian + render Jasper),
// paritas class ZK tanpa menampilkan ZUL. Perilaku ada di controller Java.
ais.common.newui.sekolah.NewUiLaporanSekolahController.handle(request, response,
        ais.common.newui.sekolah.NewUiLaporanSekolahController.JENIS_PEMBAYARAN,
        "format1/sekolah/laporan_pembayaran_siswa");
%>
