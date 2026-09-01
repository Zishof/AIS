<%--
    Adaptor native: Terima Tagihan

    Sumber ZK   : /pages/master/asset/terima_tagihan.zul (TerimaTagihanAction -> SaldoAwalMasterAssetAction, persetujuan)
    Kontrak     : ais.common.newui.lainnya.NewUiLayarLainnyaController (mode terima_tagihan)
    Batas       : BACA SAJA. Menyetujui tagihan melepaskan pembayaran pada
                  rantai pengadaan; kontrak yang sekadar menandai kolom akan
                  memutus rangkaiannya.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.lainnya.NewUiLayarLainnyaController" %>
<%
NewUiLayarLainnyaController.handle(request, response,
        NewUiLayarLainnyaController.MODE_TERIMA_TAGIHAN, "terima_tagihan");
%>
