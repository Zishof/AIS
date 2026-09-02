<%-- Dialihkan dari scaffold ke kontrak laporan umum, 2026-09-02.
Delapan filter; tiga di antaranya bentuknya tidak lazim: dua checkbox yang
mengirim angka penanda (Perkuliahan.SEMESTER_PENDEK / EKSTRA) dan kelas yang
parameternya berisi NAMA kelas, bukan id. Semuanya disalin dari
generateParameter() layar ZK-nya dan dicocokkan dengan deklarasi <parameter>
pada jrxml.
--%><%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanUmumController" %>
<%
NewUiLaporanUmumController.handle(request, response, "akademik_jadwal_perkuliahan", "format1/akademik/laporan_jadwal_perkuliahan");
%>
