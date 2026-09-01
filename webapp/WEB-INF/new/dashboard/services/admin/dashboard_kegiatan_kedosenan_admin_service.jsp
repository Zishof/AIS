<%--
    Adaptor native: Kegiatan Kedosenan

    Sumber ZK   : ais.action.master.dashboard.admin.DashboardKegiatanKedosenanAdmin
    Kontrak     : ais.common.newui.lainnya.NewUiLayarLainnyaController (mode dasbor_kedosenan)
    Catatan     : wadah tab atas layar kegiatan, organisasi, prestasi, dan
                  karya dosen.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.lainnya.NewUiLayarLainnyaController" %>
<%
NewUiLayarLainnyaController.handle(request, response,
        NewUiLayarLainnyaController.MODE_DASBOR_KEDOSENAN, "admin/dashboard_kegiatan_kedosenan_admin");
%>
