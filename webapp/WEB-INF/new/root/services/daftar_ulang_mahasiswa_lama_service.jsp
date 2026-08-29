<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%
// Kasir daftar ulang mahasiswa (Pembayaran Mahasiswa) - paritas logika
// daftarulang_mahasiswa_lama.zul tanpa ZK. Seluruh perilaku ada di controller
// Java; JSP ini hanya delegasi.
ais.common.newui.kampus.NewUiDaftarUlangMahasiswaController.handle(request, response,
        ais.common.newui.kampus.NewUiDaftarUlangMahasiswaController.MODE_LAMA);
%>