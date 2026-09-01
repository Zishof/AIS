<%--
    Adaptor native: Kegiatan Kemahasiswaan

    Sumber ZK   : ais.action.master.dashboard.admin.DashboardKegiatanKemahasiswaanAdmin
    Kontrak     : ais.common.newui.lainnya.NewUiLayarLainnyaController (mode dasbor_kemahasiswaan)
    Catatan     : wadah tab atas layar kegiatan, organisasi, prestasi, karya,
                  dan catatan mahasiswa. Panel "Dasbor" dibangun langsung oleh
                  helper sehingga tidak punya halaman tersendiri; diumumkan
                  tanpa rute beserta alasannya.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.lainnya.NewUiLayarLainnyaController" %>
<%
NewUiLayarLainnyaController.handle(request, response,
        NewUiLayarLainnyaController.MODE_DASBOR_KEMAHASISWAAN, "admin/dashboard_kegiatan_kemahasiswaan_admin");
%>
