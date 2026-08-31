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
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.koperasi.NewUiKoperasiOperasiController" %>
<%
NewUiKoperasiOperasiController.handle(request, response,
        NewUiKoperasiOperasiController.MODE_PERSETUJUAN, "persetujuan_transaksi_koperasi");
%>
