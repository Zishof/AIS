<%--
    Adaptor native: Pengaturan Kantin

    Sumber ZK   : /pages/master/koperasi/pengaturan_kantin.zul (PengaturanKantinAction)
    Kontrak     : NewUiKoperasiOperasiController
    Catatan     : layar ini tidak punya data sendiri, hanya wadah tab atas
                  sebelas layar master lain. Daftar tabnya dibaca dari konstanta
                  PengaturanKantinAction.TABS supaya tidak pernah menyimpang.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "koperasi");
request.setAttribute("nuiModuleLabel", "Koperasi & Unit Usaha");
request.setAttribute("nuiPage", "pengaturan_kantin");
request.setAttribute("nuiPageTitle", "Pengaturan Kantin");
request.setAttribute("nuiPageType", "dashboard");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
