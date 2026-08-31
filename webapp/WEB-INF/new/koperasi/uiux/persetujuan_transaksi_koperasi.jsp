<%--
    Adaptor native: Persetujuan Transaksi Koperasi

    Sumber ZK   : /pages/master/koperasi/persetujuan_transaksi_koperasi.zul (PersetujuanTransaksiKoperasiAction)
    Kontrak     : NewUiKoperasiOperasiController
    Batas       : BACA SAJA. Menyetujui transaksi membangkitkan pengajuan
                  transfer dan menyentuh disposisi SOP; kontrak yang hanya
                  membalik penanda status akan menghasilkan transaksi yang
                  tampak disetujui tanpa pengajuan transfer. Persetujuan tetap
                  di layar lama sampai rangkaian itu tersedia native.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "koperasi");
request.setAttribute("nuiModuleLabel", "Koperasi & Unit Usaha");
request.setAttribute("nuiPage", "persetujuan_transaksi_koperasi");
request.setAttribute("nuiPageTitle", "Persetujuan Transaksi Koperasi");
request.setAttribute("nuiPageType", "list");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
