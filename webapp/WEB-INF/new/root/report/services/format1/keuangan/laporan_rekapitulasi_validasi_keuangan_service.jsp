<%-- Dialihkan dari scaffold ke kontrak laporan umum, 2026-09-02.
Nama dan tipe parameter diambil dari deklarasi <parameter> pada jrxml.
tahun_akademik memakai pilihan tertutup dari Common.tahunAngkatans, sama dengan
combobox readonly pada layar ZK-nya.
--%><%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanUmumController" %>
<%
NewUiLaporanUmumController.handle(request, response, "keuangan_rekap_validasi", "format1/keuangan/laporan_rekapitulasi_validasi_keuangan");
%>
