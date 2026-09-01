<%--
    Adaptor native: Status Kehadiran

    Sumber ZK   : /pages/master/absensi_mahasiswa.zul (AbsensiMahasiswaAction)
    Kontrak     : ais.common.newui.master.NewUiMasterUmumController (mode absensi)
    Catatan     : terkunci identitas. Mahasiswa diambil dari sesi, tidak pernah
                  dari parameter; menerimanya dari klien akan membuat siapa pun
                  dapat membaca kehadiran orang lain. Pembacaan memakai
                  keDatabase=false supaya tidak menulis ulang KRS.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.master.NewUiMasterUmumController" %>
<%
NewUiMasterUmumController.handle(request, response,
        NewUiMasterUmumController.MODE_ABSENSI, "absensi_mahasiswa");
%>
