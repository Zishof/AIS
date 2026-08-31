<%--
    Adaptor native: Rekap Angket Dosen Per Dosen

    Sumber menu : URL menu berupa kata kunci "rekapAngketDosenPerDosen" (bukan berkas ZUL);
                  ais.common.Common memetakannya ke LaporanAngketDosenPerDosenWindow.
    Template    : rekap_angket_dosen_per_dosen_saja
    Pemetaan    : jendela ZK hanya menyusun parameter Jasper, sehingga
                  parameternya disalin ke NewUiLaporanRekapController dan PDF
                  dikirim sebagai pdfBase64.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "root/report");
request.setAttribute("nuiPage", "rekap_angket_dosen_per_dosen");
request.setAttribute("nuiPageTitle", "Rekap Angket Dosen Per Dosen");
request.setAttribute("nuiPageType", "report");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
