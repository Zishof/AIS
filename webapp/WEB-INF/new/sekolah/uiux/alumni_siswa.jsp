<%--
    Adaptor native: Alumni Siswa

    Sumber ZK   : /pages/master/sekolah/siswa_alumni.zul (AlumniSiswaAction -> SiswaAction, alumni=true)
    Kontrak     : ais.common.newui.lainnya.NewUiLayarLainnyaController (mode alumni_siswa)
    Catatan     : penanda alumni adalah statusKeluar.id = 1, disalin apa adanya
                  dari layar ZK. Menebak penanda lain menghasilkan daftar yang
                  tampak masuk akal namun salah orang.
    Batas       : BACA SAJA; penyuntingan data siswa tetap di layar master.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "sekolah");
request.setAttribute("nuiPage", "alumni_siswa");
request.setAttribute("nuiPageTitle", "Alumni Siswa");
request.setAttribute("nuiPageType", "list");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
