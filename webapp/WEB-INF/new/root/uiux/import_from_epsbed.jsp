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
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "root");
request.setAttribute("nuiPage", "import_from_epsbed");
request.setAttribute("nuiPageTitle", "Import Data Dari EPSBED");
request.setAttribute("nuiPageType", "form");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
