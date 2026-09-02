<%-- Dialihkan dari scaffold, 2026-09-02.
Milik mahasiswa pemilik sesi dan baca saja. Sumber datanya sama dengan layar
lama (Common.generateSemestersForGrid dan Mahasiswa.ambilPerkuliahanDanParalel);
pengisian angketnya tetap milik layar angket dosen yang sudah native.
--%><%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.akademik.NewUiKuesionerMahasiswaController" %>
<%
NewUiKuesionerMahasiswaController.handle(request, response, "kuesioner_mahasiswa");
%>
