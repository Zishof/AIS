<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%
// Kasir daftar ulang mahasiswa baru (calon daftar ulang) - paritas logika
// daftarulang_mahasiswa_baru.zul tanpa ZK. Delegasi penuh ke controller Java.
ais.common.newui.kampus.NewUiDaftarUlangMahasiswaController.handle(request, response,
        ais.common.newui.kampus.NewUiDaftarUlangMahasiswaController.MODE_BARU);
%>