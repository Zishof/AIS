<%--
    Adaptor native: Riwayat Transaksi Saya

    Sumber ZK   : /pages/master/koperasi/riwayat_transaksi_member_kantin.zul (RiwayatTransaksiMemberAction)
    Kontrak     : NewUiKantinMemberController
    Catatan     : kepemilikan transaksi diperiksa di klausa WHERE, sehingga
                  rincian milik anggota lain tidak pernah terbaca meski id-nya
                  ditebak.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.koperasi.NewUiKantinMemberController" %>
<%
NewUiKantinMemberController.handle(request, response,
        NewUiKantinMemberController.MODE_RIWAYAT, "riwayat_transaksi_member");
%>
