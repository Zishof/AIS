<%--
    Adaptor native: Proses Poin Bulanan

    Sumber ZK   : /pages/master/koperasi/proses_poin_bulanan.zul (ProsesPoinBulananAction)
    Kontrak     : NewUiKoperasiOperasiController
    Catatan     : proses aman diulang; voucher untuk periode yang sama tidak
                  diterbitkan dua kali (sifat ProsesPoinBulananHelper).
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "koperasi");
request.setAttribute("nuiModuleLabel", "Koperasi & Unit Usaha");
request.setAttribute("nuiPage", "proses_poin_bulanan");
request.setAttribute("nuiPageTitle", "Proses Poin Bulanan");
request.setAttribute("nuiPageType", "form");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
