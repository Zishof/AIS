<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%
// Pembayaran Calon Mahasiswa - per arahan paritas: ikuti logika
// daftarulang_mahasiswa_baru.zul (ZUL pembayaran_calon_mahasiswa.zul cacat sama).
// pageKey tetap "pembayaran_calon_mahasiswa" agar cocok dengan registry menu 10030017.
ais.common.newui.kampus.NewUiDaftarUlangMahasiswaController.handle(request, response,
        ais.common.newui.kampus.NewUiDaftarUlangMahasiswaController.MODE_BARU, "pembayaran_calon_mahasiswa");
%>