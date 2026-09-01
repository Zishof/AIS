<%--
    Adaptor native: Integrator Kelas

    Sumber ZK   : ais.action.master.feeder.integrator.KelasIntegrator
    Kontrak     : ais.common.newui.feeder.NewUiFeederIntegratorController (mode kelas)
    Catatan     : layar ini TIDAK menyentuh server Feeder. Panel Download
                  menyusun berkas .xlsx dari data lokal untuk diunggah operator
                  sendiri; panel Upload membaca berkas .xlsx kembali ke basis
                  data lokal. Kredensial Feeder hanya dipakai layar Ekspor ke
                  Feeder yang berbeda.
    Alur        : penyusunan berkas berjalan lama, sehingga dilayani sebagai
                  pekerjaan yang kemajuannya dicatat PekerjaanRegistry --
                  export_mulai, lalu detail untuk menanyakan kemajuan, lalu
                  export untuk mengunduh berkasnya.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "feeder");
request.setAttribute("nuiPage", "integrator/kelas_integrator");
request.setAttribute("nuiPageTitle", "Integrator Kelas");
request.setAttribute("nuiPageType", "form");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
