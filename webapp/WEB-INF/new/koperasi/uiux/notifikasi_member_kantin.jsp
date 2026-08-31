<%--
    Adaptor native: Notifikasi Saya

    Sumber ZK   : /pages/master/koperasi/notifikasi_member_kantin.zul (NotifikasiMemberKantinAction)
    Kontrak     : NewUiKantinMemberController
    Catatan     : notifikasi menempel pada userId, bukan pada keanggotaan
                  koperasi, sehingga pengguna non-anggota pun berhak membacanya.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "koperasi");
request.setAttribute("nuiModuleLabel", "Koperasi & Unit Usaha");
request.setAttribute("nuiPage", "notifikasi_member_kantin");
request.setAttribute("nuiPageTitle", "Notifikasi Saya");
request.setAttribute("nuiPageType", "list");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
