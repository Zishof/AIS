<%-- Dialihkan dari scaffold, 2026-09-02.
Milik mahasiswa pemilik sesi. Aksi create/delete menuntut POST + token CSRF;
kata kerjanya sengaja "create"/"delete" karena NewUiRouteGuard menolak kata
kerja yang tidak dikenalnya. Kapasitas kelas dan penjaga penghapusan disalin
dari layar lama.
--%><%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.akademik.NewUiIkutPerkuliahanController" %>
<%
NewUiIkutPerkuliahanController.handle(request, response, "mahasiswa_ikuti_perkuliahan");
%>
