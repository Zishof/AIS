<%--
    Adaptor native: Katalogisasi MARCXML

    Sumber ZK   : /pages/master/library/katalogisasi_marc.zul
                  ZUL itu HANYA membungkus <iframe src="/ais/baru?p=pustaka&s=katalogisasi">,
                  sehingga aplikasi desktop akan bergantung pada halaman web
                  eksternal bila dibiarkan.
    Kontrak     : ais.common.newui.pustaka.NewUiPustakaController (mode katalogisasi)
    Delegasi    : ais.action.master.library.modern.LibraryMarcApi - seluruh aturan bisnis, batas ukuran, penjaga hak
                  petugas, dan pemeriksaan CSRF tetap milik modul pustaka;
                  controller hanya menambah penjaga hak menu/peran New UI.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "library");
request.setAttribute("nuiPage", "katalogisasi_marc");
request.setAttribute("nuiPageTitle", "Katalogisasi MARCXML");
request.setAttribute("nuiPageType", "form");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
