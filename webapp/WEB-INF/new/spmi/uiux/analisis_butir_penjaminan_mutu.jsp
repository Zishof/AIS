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
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "spmi");
request.setAttribute("nuiPage", "analisis_butir_penjaminan_mutu");
request.setAttribute("nuiPageTitle", "Dasbor Penjaminan Mutu");
request.setAttribute("nuiPageType", "list");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
