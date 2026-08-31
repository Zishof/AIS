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
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "root");
request.setAttribute("nuiPage", "proposal_sidang_atau_munaqosah");
request.setAttribute("nuiPageTitle", "Pengajuan Proposal Tugas Akhir / Skripsi");
request.setAttribute("nuiPageType", "list");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
