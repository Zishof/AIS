<%-- Dialihkan dari scaffold, 2026-09-02.
list menyinkronkan KRS TANPA menulis; update menulis (keDatabase=true), sama
seperti pemisahan onSearchDefault / onSearchDefaultKeDatabase pada layar lama.
Pengambilan paket belum dipindahkan; meta menyatakannya lewat
ambilPaketTersedia:false beserta alasannya.
--%><%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.akademik.NewUiKrsPaketController" %>
<%
NewUiKrsPaketController.handle(request, response, "krs_paket");
%>
