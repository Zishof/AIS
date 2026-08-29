<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%
// Pembayaran Mahasiswa - per arahan paritas: ikuti logika daftarulang_mahasiswa_lama.zul
// (ZUL pembayaran_mahasiswa.zul sendiri cacat: komponen detail tidak ada, alur bayar
// tidak dapat dijalankan apa adanya). pageKey tetap "pembayaran_mahasiswa" agar cocok
// dengan NewUiRouteRegistry menu 100300017.
ais.common.newui.kampus.NewUiDaftarUlangMahasiswaController.handle(request, response,
        ais.common.newui.kampus.NewUiDaftarUlangMahasiswaController.MODE_LAMA, "pembayaran_mahasiswa");
%>