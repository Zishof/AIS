<%--
    Adaptor native: Pengajuan Sidang Skripsi/Tugas Akhir

    Sumber menu : URL menu berupa kata kunci "daftar_sidang_atau_munaqosah" (bukan berkas ZUL);
                  ais.common.Common memetakannya ke CommonUiFactoryHelper.tampilkanDaftarSkripsi().
    Kontrak     : NewUiSidangSkripsiController
    Catatan     : hanya-baca dan terkunci pada mahasiswa yang sedang login; pendaftaran sidang tetap melalui layar ZK karena alur persetujuan pembimbing belum direproduksi.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "root");
request.setAttribute("nuiPage", "daftar_sidang_atau_munaqosah");
request.setAttribute("nuiPageTitle", "Pengajuan Sidang Skripsi/Tugas Akhir");
request.setAttribute("nuiPageType", "list");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
