<%--
    Adaptor native: Terima Tagihan

    Sumber ZK   : /pages/master/asset/terima_tagihan.zul (TerimaTagihanAction -> SaldoAwalMasterAssetAction, persetujuan)
    Kontrak     : ais.common.newui.lainnya.NewUiLayarLainnyaController (mode terima_tagihan)
    Batas       : BACA SAJA. Menyetujui tagihan melepaskan pembayaran pada
                  rantai pengadaan; kontrak yang sekadar menandai kolom akan
                  memutus rangkaiannya.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "asset");
request.setAttribute("nuiPage", "terima_tagihan");
request.setAttribute("nuiPageTitle", "Terima Tagihan");
request.setAttribute("nuiPageType", "list");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
