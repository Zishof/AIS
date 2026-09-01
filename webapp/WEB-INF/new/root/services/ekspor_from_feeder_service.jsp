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
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.master.NewUiMasterUmumController" %>
<%
NewUiMasterUmumController.handle(request, response,
        NewUiMasterUmumController.MODE_EKSPOR_FEEDER, "ekspor_from_feeder");
%>
