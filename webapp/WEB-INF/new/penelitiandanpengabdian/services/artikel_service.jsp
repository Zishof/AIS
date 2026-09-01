<%--
    Adaptor native: Publikasi Ilmiah

    Sumber ZK   : /pages/master/penelitiandanpengabdian/artikel.zul (ArtikelAction)
    Kontrak     : ais.common.newui.lainnya.NewUiLayarLainnyaController (mode artikel)
    Batas       : BACA SAJA. Pengajuan artikel ditangani layar tersendiri
                  beserta unggahan berkas dan pemeriksaan plagiarisme.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.lainnya.NewUiLayarLainnyaController" %>
<%
NewUiLayarLainnyaController.handle(request, response,
        NewUiLayarLainnyaController.MODE_ARTIKEL, "artikel");
%>
