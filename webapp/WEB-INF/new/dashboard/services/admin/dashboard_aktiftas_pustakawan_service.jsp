<%-- Dialihkan dari scaffold, 2026-09-02.
SQL rekap disalin baris demi baris dari DashboardAktiftasPustakawan; batas atas
tanggal tetap eksklusif (sampai + 1 hari) seperti layar lama.
--%><%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.dashboard.NewUiAktifitasPustakawanController" %>
<%
NewUiAktifitasPustakawanController.handle(request, response,
    "admin/dashboard_aktiftas_pustakawan");
%>
