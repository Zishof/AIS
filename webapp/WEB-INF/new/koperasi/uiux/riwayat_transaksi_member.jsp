<%--
    Adaptor native: Riwayat Transaksi Saya

    Sumber ZK   : /pages/master/koperasi/riwayat_transaksi_member_kantin.zul (RiwayatTransaksiMemberAction)
    Kontrak     : NewUiKantinMemberController
    Catatan     : kepemilikan transaksi diperiksa di klausa WHERE, sehingga
                  rincian milik anggota lain tidak pernah terbaca meski id-nya
                  ditebak.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "koperasi");
request.setAttribute("nuiModuleLabel", "Koperasi & Unit Usaha");
request.setAttribute("nuiPage", "riwayat_transaksi_member");
request.setAttribute("nuiPageTitle", "Riwayat Transaksi Saya");
request.setAttribute("nuiPageType", "list");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
