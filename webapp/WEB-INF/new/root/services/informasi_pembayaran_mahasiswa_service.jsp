<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%
// Informasi Pembayaran Mahasiswa - paritas informasi_pembayaran_mahasiswa.zul
// (read-only: ringkasan kegiatan pembayaran per mahasiswa + cetak kuitansi)
// tanpa ZUL. Memakai mesin kontrak kampus dengan pageKey route menu 1732.
ais.common.newui.kampus.NewUiDaftarUlangMahasiswaController.handle(request, response,
        ais.common.newui.kampus.NewUiDaftarUlangMahasiswaController.MODE_LAMA,
        "informasi_pembayaran_mahasiswa");
%>
