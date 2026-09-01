<%--
    Adaptor native: Alumni Siswa

    Sumber ZK   : /pages/master/sekolah/siswa_alumni.zul (AlumniSiswaAction -> SiswaAction, alumni=true)
    Kontrak     : ais.common.newui.lainnya.NewUiLayarLainnyaController (mode alumni_siswa)
    Catatan     : penanda alumni adalah statusKeluar.id = 1, disalin apa adanya
                  dari layar ZK. Menebak penanda lain menghasilkan daftar yang
                  tampak masuk akal namun salah orang.
    Batas       : BACA SAJA; penyuntingan data siswa tetap di layar master.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.lainnya.NewUiLayarLainnyaController" %>
<%
NewUiLayarLainnyaController.handle(request, response,
        NewUiLayarLainnyaController.MODE_ALUMNI_SISWA, "alumni_siswa");
%>
