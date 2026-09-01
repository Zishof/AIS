<%--
    Adaptor native: Laporan Kinerja Rinci

    Sumber ZK   : /pages/master/bkd/laporan_rinci.zul (LaporanBKDRinciAction)
    Kontrak     : ais.common.newui.bkd.NewUiBkdController (mode laporan_rinci)
    Catatan     : sama dengan Laporan Kinerja, memakai template versi rinci.
                  Peringkat semua dosen memakai template yang sama dengan layar
                  Laporan Kinerja, persis seperti pada ZK.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "bkd");
request.setAttribute("nuiPage", "laporan_bkd_rinci");
request.setAttribute("nuiPageTitle", "Laporan Kinerja Rinci");
request.setAttribute("nuiPageType", "report");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
