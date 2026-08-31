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
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.akunting.NewUiPersetujuanAkuntingController" %>
<%
NewUiPersetujuanAkuntingController.handle(request, response,
        NewUiPersetujuanAkuntingController.MODE_DRAFT_JURNAL, "posting_jurnal");
%>
