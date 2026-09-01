<%--
    Adaptor native: Pengajuan Pensiun

    Sumber ZK   : /pages/master/employ/pensiun.zul (PensiunAction -> PegawaiAction, disaring status pensiun)
    Kontrak     : ais.common.newui.employ.NewUiKepegawaianController (mode pensiun)
    Batas       : BACA SAJA. Layar ini tampilan tersaring atas data pegawai;
                  pada layar lama pun tombol tambah disembunyikan untuk tampilan
                  ini, dan penyuntingan tetap milik layar master pegawai.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "employ");
request.setAttribute("nuiPage", "pensiun");
request.setAttribute("nuiPageTitle", "Pengajuan Pensiun");
request.setAttribute("nuiPageType", "list");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
