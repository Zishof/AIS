<%--
    Adaptor native: Status Kehadiran

    Sumber ZK   : /pages/master/absensi_mahasiswa.zul (AbsensiMahasiswaAction)
    Kontrak     : ais.common.newui.master.NewUiMasterUmumController (mode absensi)
    Catatan     : terkunci identitas. Mahasiswa diambil dari sesi, tidak pernah
                  dari parameter; menerimanya dari klien akan membuat siapa pun
                  dapat membaca kehadiran orang lain. Pembacaan memakai
                  keDatabase=false supaya tidak menulis ulang KRS.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "root");
request.setAttribute("nuiPage", "absensi_mahasiswa");
request.setAttribute("nuiPageTitle", "Status Kehadiran");
request.setAttribute("nuiPageType", "list");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
