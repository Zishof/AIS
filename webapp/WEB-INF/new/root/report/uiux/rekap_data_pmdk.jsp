<%--
    Adaptor native: Rekap Data PMDK

    Sumber menu : URL menu berupa kata kunci "rekapDataPmdk" (bukan berkas ZUL);
                  ais.common.Common memetakannya ke LaporanRekapitulasiPMDKWindow.
    Template    : rekap_data_pmdk
    Pemetaan    : jendela ZK hanya menyusun parameter Jasper, sehingga
                  parameternya disalin ke NewUiLaporanRekapController dan PDF
                  dikirim sebagai pdfBase64.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "root/report");
request.setAttribute("nuiPage", "rekap_data_pmdk");
request.setAttribute("nuiPageTitle", "Rekap Data PMDK");
request.setAttribute("nuiPageType", "report");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
