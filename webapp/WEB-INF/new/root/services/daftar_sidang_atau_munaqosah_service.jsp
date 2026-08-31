<%--
    Adaptor native: Pengajuan Sidang Skripsi/Tugas Akhir

    Sumber menu : URL menu berupa kata kunci "daftar_sidang_atau_munaqosah" (bukan berkas ZUL);
                  ais.common.Common memetakannya ke CommonUiFactoryHelper.tampilkanDaftarSkripsi().
    Kontrak     : NewUiSidangSkripsiController
    Catatan     : hanya-baca dan terkunci pada mahasiswa yang sedang login; pendaftaran sidang tetap melalui layar ZK karena alur persetujuan pembimbing belum direproduksi.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.akademik.NewUiSidangSkripsiController" %>
<%
NewUiSidangSkripsiController.handle(request, response, "daftar_sidang_atau_munaqosah");
%>
