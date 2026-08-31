<%--
    Adaptor native: Dasbor Penjaminan Mutu - Analisis Butir Soal

    Sumber ZK   : /pages/master/spmi/analisis_butir_penjaminan_mutu.zul
                  ZUL itu hanya wadah kosong yang diisi
                  PenjaminanMutuAnalisisHelper.render() lewat zscript, sehingga
                  tidak ada kontrak JSON yang bisa dipakai ulang.
    Kontrak     : ais.common.newui.spmi.NewUiAnalisisButirController
    Sumber data : PertemuanPunyaUjian (alias pertemuan -> perkuliahan); status
                  dan catatan dibaca dari kolom analisis_catatan_json.
    Catatan     : hanya-baca. Menyetujui/meminta revisi memicu notifikasi dosen
                  yang belum direproduksi native, jadi tombolnya tidak ada.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.spmi.NewUiAnalisisButirController" %>
<%
NewUiAnalisisButirController.handle(request, response, "analisis_butir_penjaminan_mutu");
%>
