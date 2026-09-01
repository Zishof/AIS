<%--
    Adaptor native: Kegiatan Kemahasiswaan

    Sumber ZK   : ais.action.master.dashboard.admin.DashboardKegiatanKemahasiswaanAdmin
    Kontrak     : ais.common.newui.lainnya.NewUiLayarLainnyaController (mode dasbor_kemahasiswaan)
    Catatan     : wadah tab atas layar kegiatan, organisasi, prestasi, karya,
                  dan catatan mahasiswa. Panel "Dasbor" dibangun langsung oleh
                  helper sehingga tidak punya halaman tersendiri; diumumkan
                  tanpa rute beserta alasannya.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "dashboard");
request.setAttribute("nuiPage", "admin/dashboard_kegiatan_kemahasiswaan_admin");
request.setAttribute("nuiPageTitle", "Kegiatan Kemahasiswaan");
request.setAttribute("nuiPageType", "dashboard");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
