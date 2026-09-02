<%-- Dialihkan dari scaffold, 2026-09-02.
Milik mahasiswa pemilik sesi dan baca saja. Pertemuan disaring dengan
statusPertemuan.getUjian() seperti layar lama; teks dosen dan jadwal memakai
pembangkit yang sama (PerkuliahanUIHelper), bukan disusun ulang.
--%><%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.akademik.NewUiUjianMahasiswaController" %>
<%
NewUiUjianMahasiswaController.handle(request, response, "ujian_mahasiswa");
%>
