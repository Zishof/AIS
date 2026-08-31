<%--
    Adaptor native: Pengajuan Proposal Tugas Akhir / Skripsi

    Sumber menu : URL menu berupa kata kunci "proposal_sidang_atau_munaqosah";
                  ais.common.Common memetakannya ke
                  CommonUiFactoryHelper.tampilkanTugasAkhir().
    Kontrak     : ais.common.newui.akademik.NewUiSidangSkripsiController
                  (jenis JENIS_PROPOSAL, entity MahasiswaRequestTugasAkhir)
    Catatan     : hanya-baca dan terkunci pada mahasiswa yang sedang login;
                  baris berstatus GAGAL dilewati seperti pada layar ZK.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.akademik.NewUiSidangSkripsiController" %>
<%
NewUiSidangSkripsiController.handle(request, response,
        NewUiSidangSkripsiController.JENIS_PROPOSAL, "proposal_sidang_atau_munaqosah");
%>
