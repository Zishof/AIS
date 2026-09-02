<%-- Dialihkan dari scaffold ke kontrak laporan umum, 2026-09-02.
Scaffold terbit tanpa nuiServiceEntities sehingga tryAutoRegister gagal dan
dispatcher hanya menjawab stub SCAFFOLD: halaman tampil, kontrak data kosong.
Nama dan tipe parameter laporan ini diambil dari deklarasi <parameter> pada
jrxml-nya, bukan dari nama widget layar ZK.
--%><%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanUmumController" %>
<%
NewUiLaporanUmumController.handle(request, response, "akademik_cover_absensi", "format1/akademik/laporan_cover_absensi");
%>
