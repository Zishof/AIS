<%--
    Adaptor native: Kegiatan Kedosenan

    Sumber ZK   : ais.action.master.dashboard.admin.DashboardKegiatanKedosenanAdmin
    Kontrak     : ais.common.newui.lainnya.NewUiLayarLainnyaController (mode dasbor_kedosenan)
    Catatan     : wadah tab atas layar kegiatan, organisasi, prestasi, dan
                  karya dosen.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "dashboard");
request.setAttribute("nuiPage", "admin/dashboard_kegiatan_kedosenan_admin");
request.setAttribute("nuiPageTitle", "Kegiatan Kedosenan");
request.setAttribute("nuiPageType", "dashboard");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
