<%--
    Adaptor native: Laporan Kinerja Rinci

    Sumber ZK   : /pages/master/bkd/laporan_rinci.zul (LaporanBKDRinciAction)
    Kontrak     : ais.common.newui.bkd.NewUiBkdController (mode laporan_rinci)
    Catatan     : sama dengan Laporan Kinerja, memakai template versi rinci.
                  Peringkat semua dosen memakai template yang sama dengan layar
                  Laporan Kinerja, persis seperti pada ZK.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.bkd.NewUiBkdController" %>
<%
NewUiBkdController.handle(request, response,
        NewUiBkdController.MODE_LAPORAN_RINCI, "laporan_bkd_rinci");
%>
