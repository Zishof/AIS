<%--
    Adaptor native: Ringkasan Saya

    Sumber ZK   : /pages/master/koperasi/dashboard_member_kantin.zul (DashboardMemberKantinAction)
    Kontrak     : NewUiKantinMemberController
    Catatan     : angka dikirim mentah, bukan HTML seperti layar ZK, agar
                  kartu dan grafiknya digambar native oleh klien.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.koperasi.NewUiKantinMemberController" %>
<%
NewUiKantinMemberController.handle(request, response,
        NewUiKantinMemberController.MODE_DASHBOARD, "dashboard_member_kantin");
%>
