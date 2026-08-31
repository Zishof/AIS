<%--
    Adaptor native: Cetak Undangan Wisuda

    Sumber menu : URL menu berupa kata kunci "cetakUndanganWisuda" (bukan berkas ZUL);
                  ais.common.Common memetakannya ke GenerateUndanganWisudaWindow.
    Kontrak     : NewUiUndanganWisudaController
    Catatan     : dua prasyarat ZK ditegakkan sebelum render: biodata dengan nama ayah, dan nomor kursi wisuda sudah terbit.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "root/report");
request.setAttribute("nuiPage", "cetak_undangan_wisuda");
request.setAttribute("nuiPageTitle", "Cetak Undangan Wisuda");
request.setAttribute("nuiPageType", "report");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
