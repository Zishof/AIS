<%-- Dialihkan dari scaffold ke controller bersekat, 2026-09-02.
Layar ini batas privasi: daftar siswa disekat pada guru yang sedang masuk,
dihitung di server dan tidak dapat dilebarkan parameter permintaan apa pun.
Baca saja; penyuntingan siswa tetap milik layar master siswa.
--%><%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.sekolah.NewUiSiswaAsuhanController" %>
<%
NewUiSiswaAsuhanController.handle(request, response, "wali", "siswa_wali");
%>
