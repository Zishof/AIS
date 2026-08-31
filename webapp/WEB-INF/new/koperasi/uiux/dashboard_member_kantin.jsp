<%--
    Adaptor native: Ringkasan Saya

    Sumber ZK   : /pages/master/koperasi/dashboard_member_kantin.zul (DashboardMemberKantinAction)
    Kontrak     : NewUiKantinMemberController
    Catatan     : angka dikirim mentah, bukan HTML seperti layar ZK, agar
                  kartu dan grafiknya digambar native oleh klien.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "koperasi");
request.setAttribute("nuiModuleLabel", "Koperasi & Unit Usaha");
request.setAttribute("nuiPage", "dashboard_member_kantin");
request.setAttribute("nuiPageTitle", "Ringkasan Saya");
request.setAttribute("nuiPageType", "dashboard");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
