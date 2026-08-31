<%--
    Adaptor native: Pesanan Saya

    Sumber ZK   : /pages/master/koperasi/draft_pesanan_member_kantin.zul (DraftPesananMemberKantinAction)
    Kontrak     : NewUiKantinMemberController
    Catatan     : pembatalan memeriksa ulang status lunas di dalam transaksi
                  (anti balapan dengan kasir) dan menyertakan pemilik pada setiap
                  klausa penghapusan.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "koperasi");
request.setAttribute("nuiModuleLabel", "Koperasi & Unit Usaha");
request.setAttribute("nuiPage", "draft_pesanan_member_kantin");
request.setAttribute("nuiPageTitle", "Pesanan Saya");
request.setAttribute("nuiPageType", "list");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
