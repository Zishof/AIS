<%--
    Adaptor native: Workspace Zakat

    Sumber ZK   : /pages/master/sosial/zakat_workspace.zul
                  (membungkus transaksi_sosial_workspace.zul, composer
                   ais.action.master.sosial.SosialTransaksiAction)
    Kategori    : ZAKAT
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
        NewUiTransaksiSosialController.KATEGORI_ZAKAT, "zakat_workspace");
%>
