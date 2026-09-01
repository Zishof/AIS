<%--
    Adaptor native: Publikasi Ilmiah

    Sumber ZK   : /pages/master/penelitiandanpengabdian/artikel.zul (ArtikelAction)
    Kontrak     : ais.common.newui.lainnya.NewUiLayarLainnyaController (mode artikel)
    Batas       : BACA SAJA. Pengajuan artikel ditangani layar tersendiri
                  beserta unggahan berkas dan pemeriksaan plagiarisme.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "penelitiandanpengabdian");
request.setAttribute("nuiPage", "artikel");
request.setAttribute("nuiPageTitle", "Publikasi Ilmiah");
request.setAttribute("nuiPageType", "list");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
