<%-- Dialihkan dari scaffold, 2026-09-02.
Layar lama meminta PATH DIREKTORI SERVER lalu membaca seluruh isinya; di sini
berkas diunggah sebagai .zip berisi berkas RKAKL .keu (DBF), diekstrak ke
direktori sementara milik pekerjaan itu, lalu diimpor di utas latar dengan
status yang dicatat NewUiPekerjaanStore sehingga dapat ditanya ulang.
Aksi create menuntut POST + CSRF + multipart.
--%><%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.rab.NewUiImportRkaklController" %>
<%
NewUiImportRkaklController.handle(request, response, "import_from_rkakl");
%>
