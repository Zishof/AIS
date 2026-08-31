<%--
    Adaptor native: Pesanan Saya

    Sumber ZK   : /pages/master/koperasi/draft_pesanan_member_kantin.zul (DraftPesananMemberKantinAction)
    Kontrak     : NewUiKantinMemberController
    Catatan     : pembatalan memeriksa ulang status lunas di dalam transaksi
                  (anti balapan dengan kasir) dan menyertakan pemilik pada setiap
                  klausa penghapusan.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.koperasi.NewUiKantinMemberController" %>
<%
NewUiKantinMemberController.handle(request, response,
        NewUiKantinMemberController.MODE_PESANAN, "draft_pesanan_member_kantin");
%>
