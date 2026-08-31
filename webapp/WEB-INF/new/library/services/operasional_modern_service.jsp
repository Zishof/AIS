<%--
    Adaptor native: Operasional Perpustakaan

    Sumber ZK   : /pages/master/library/operasional_modern.zul
                  ZUL itu HANYA membungkus <iframe src="/ais/baru?p=pustaka&s=operasional">,
                  sehingga aplikasi desktop akan bergantung pada halaman web
                  eksternal bila dibiarkan.
    Kontrak     : ais.common.newui.pustaka.NewUiPustakaController (mode operasional)
    Delegasi    : ais.action.master.library.modern.LibraryOperationsApi - seluruh aturan bisnis, batas ukuran, penjaga hak
                  petugas, dan pemeriksaan CSRF tetap milik modul pustaka;
                  controller hanya menambah penjaga hak menu/peran New UI.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.pustaka.NewUiPustakaController" %>
<%
NewUiPustakaController.handle(request, response,
        NewUiPustakaController.MODE_OPERASIONAL, "operasional_modern");
%>
