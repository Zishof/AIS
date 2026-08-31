<%--
    Adaptor native: Rekap Angket Dosen Per Dosen

    Sumber menu : URL menu berupa kata kunci "rekapAngketDosenPerDosen" (bukan berkas ZUL);
                  ais.common.Common memetakannya ke LaporanAngketDosenPerDosenWindow.
    Template    : rekap_angket_dosen_per_dosen_saja
    Pemetaan    : jendela ZK hanya menyusun parameter Jasper, sehingga
                  parameternya disalin ke NewUiLaporanRekapController dan PDF
                  dikirim sebagai pdfBase64.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanRekapController" %>
<%
NewUiLaporanRekapController.handle(request, response,
        NewUiLaporanRekapController.JENIS_ANGKET_DOSEN, "rekap_angket_dosen_per_dosen");
%>
