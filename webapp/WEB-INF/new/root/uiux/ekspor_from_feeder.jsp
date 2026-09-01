<%--
    Adaptor native: Ekspor ke Feeder

    Sumber ZK   : /pages/master/export_from_feeder.zul (EksporFromFeederAction)
    Kontrak     : ais.common.newui.master.NewUiMasterUmumController (mode ekspor_feeder)
    Batas       : SENGAJA tidak dapat dijalankan dari kontrak. Ekspor
                  menyambung ke server Feeder memakai alamat beserta kata sandi
                  dari konfigurasi, dan berjalan lama sambil melaporkan
                  kemajuannya lewat widget layar -- tidak ada status pekerjaan
                  di sisi server yang dapat ditanya ulang oleh API.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "root");
request.setAttribute("nuiPage", "ekspor_from_feeder");
request.setAttribute("nuiPageTitle", "Ekspor ke Feeder");
request.setAttribute("nuiPageType", "form");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
