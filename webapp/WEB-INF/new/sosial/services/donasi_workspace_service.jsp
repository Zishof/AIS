<%--
    Adaptor native: Workspace Donasi

    Sumber ZK   : /pages/master/sosial/donasi_workspace.zul
                  (membungkus transaksi_sosial_workspace.zul, composer
                   ais.action.master.sosial.SosialTransaksiAction)
    Kategori    : DONASI
    Pemetaan    : layar ZK menyaring TransaksiDonasi menurut kode jenis dana dan
                  tenant, sehingga TIDAK dapat dilayani scaffold Generic CRUD
                  yang akan mencampur seluruh kategori. Penyaringan yang sama
                  diterapkan NewUiTransaksiSosialController.
    Catatan     : kontrak ini hanya-baca; pembuatan dan perubahan transaksi tetap
                  melalui layar ZK yang memegang SocialPrivilegeGuard.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.sosial.NewUiTransaksiSosialController" %>
<%
NewUiTransaksiSosialController.handle(request, response,
        NewUiTransaksiSosialController.KATEGORI_DONASI, "donasi_workspace");
%>
