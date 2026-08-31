<%--
    Adaptor native: Notifikasi Saya

    Sumber ZK   : /pages/master/koperasi/notifikasi_member_kantin.zul (NotifikasiMemberKantinAction)
    Kontrak     : NewUiKantinMemberController
    Catatan     : notifikasi menempel pada userId, bukan pada keanggotaan
                  koperasi, sehingga pengguna non-anggota pun berhak membacanya.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.koperasi.NewUiKantinMemberController" %>
<%
NewUiKantinMemberController.handle(request, response,
        NewUiKantinMemberController.MODE_NOTIFIKASI, "notifikasi_member_kantin");
%>
