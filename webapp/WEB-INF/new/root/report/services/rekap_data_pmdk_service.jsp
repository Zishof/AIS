<%--
    Adaptor native: Rekap Data PMDK

    Sumber menu : URL menu berupa kata kunci "rekapDataPmdk" (bukan berkas ZUL);
                  ais.common.Common memetakannya ke LaporanRekapitulasiPMDKWindow.
    Template    : rekap_data_pmdk
    Pemetaan    : jendela ZK hanya menyusun parameter Jasper, sehingga
                  parameternya disalin ke NewUiLaporanRekapController dan PDF
                  dikirim sebagai pdfBase64.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanRekapController" %>
<%
NewUiLaporanRekapController.handle(request, response,
        NewUiLaporanRekapController.JENIS_PMDK, "rekap_data_pmdk");
%>
