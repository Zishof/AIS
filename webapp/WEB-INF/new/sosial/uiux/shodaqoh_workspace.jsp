<%--
    Adaptor native: Workspace Shodaqoh

    Sumber ZK   : /pages/master/sosial/shodaqoh_workspace.zul
                  (membungkus transaksi_sosial_workspace.zul, composer
                   ais.action.master.sosial.SosialTransaksiAction)
    Kategori    : SHODAQOH
    Pemetaan    : layar ZK menyaring TransaksiDonasi menurut kode jenis dana dan
                  tenant, sehingga TIDAK dapat dilayani scaffold Generic CRUD
                  yang akan mencampur seluruh kategori. Penyaringan yang sama
                  diterapkan NewUiTransaksiSosialController.
    Catatan     : kontrak ini hanya-baca; pembuatan dan perubahan transaksi tetap
                  melalui layar ZK yang memegang SocialPrivilegeGuard.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "sosial");
request.setAttribute("nuiPage", "shodaqoh_workspace");
request.setAttribute("nuiPageTitle", "Workspace Shodaqoh");
request.setAttribute("nuiPageType", "list");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
