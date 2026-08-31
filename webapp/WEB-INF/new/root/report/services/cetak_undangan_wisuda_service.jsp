<%--
    Adaptor native: Cetak Undangan Wisuda

    Sumber menu : URL menu berupa kata kunci "cetakUndanganWisuda" (bukan berkas ZUL);
                  ais.common.Common memetakannya ke GenerateUndanganWisudaWindow.
    Kontrak     : NewUiUndanganWisudaController
    Catatan     : dua prasyarat ZK ditegakkan sebelum render: biodata dengan nama ayah, dan nomor kursi wisuda sudah terbit.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.akademik.NewUiUndanganWisudaController" %>
<%
NewUiUndanganWisudaController.handle(request, response, "cetak_undangan_wisuda");
%>
