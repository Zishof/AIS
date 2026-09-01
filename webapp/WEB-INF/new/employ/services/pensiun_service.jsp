<%--
    Adaptor native: Pengajuan Pensiun

    Sumber ZK   : /pages/master/employ/pensiun.zul (PensiunAction -> PegawaiAction, disaring status pensiun)
    Kontrak     : ais.common.newui.employ.NewUiKepegawaianController (mode pensiun)
    Batas       : BACA SAJA. Layar ini tampilan tersaring atas data pegawai;
                  pada layar lama pun tombol tambah disembunyikan untuk tampilan
                  ini, dan penyuntingan tetap milik layar master pegawai.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.employ.NewUiKepegawaianController" %>
<%
NewUiKepegawaianController.handle(request, response,
        NewUiKepegawaianController.MODE_PENSIUN, "pensiun");
%>
