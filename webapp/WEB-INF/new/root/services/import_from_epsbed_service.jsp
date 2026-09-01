<%--
    Adaptor native: Import Data Dari EPSBED

    Sumber ZK   : /pages/master/import_from_epsbed.zul (ImportFromEpsbedAction)
    Kontrak     : ais.common.newui.master.NewUiMasterUmumController (mode impor_epsbed)
    Batas       : SENGAJA tidak dapat dijalankan dari kontrak. Impor menerima
                  letak folder di server yang diketik pengguna lalu membaca
                  seluruh isinya; membukanya berarti klien menentukan folder
                  mana yang dibaca server. Kontrak hanya menerangkan alasannya
                  beserta syarat yang harus ada lebih dulu.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.master.NewUiMasterUmumController" %>
<%
NewUiMasterUmumController.handle(request, response,
        NewUiMasterUmumController.MODE_IMPOR_EPSBED, "import_from_epsbed");
%>
