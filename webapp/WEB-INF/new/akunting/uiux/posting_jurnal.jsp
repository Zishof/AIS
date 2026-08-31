<%--
    Adaptor native: Draft Jurnal

    Sumber ZK   : /pages/master/akunting/posting_jurnal.zul (PostingJurnalAction)
    Kontrak     : ais.common.newui.akunting.NewUiPersetujuanAkuntingController (mode draft_jurnal)
    Catatan     : layar ini tidak punya data sendiri, hanya wadah sampai dua
                  puluh layar posting lain. Daftar tabnya dibaca dari konstanta
                  PostingJurnalAction.TABS dan tetap tunduk pada saklar
                  Konfigurasi, sehingga tab yang dimatikan admin juga tidak
                  muncul di sini.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "akunting");
request.setAttribute("nuiModuleLabel", "Akuntansi & Keuangan");
request.setAttribute("nuiPage", "posting_jurnal");
request.setAttribute("nuiPageTitle", "Draft Jurnal");
request.setAttribute("nuiPageType", "dashboard");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
