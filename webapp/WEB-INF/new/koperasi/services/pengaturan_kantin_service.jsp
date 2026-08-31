<%--
    Adaptor native: Pengaturan Kantin

    Sumber ZK   : /pages/master/koperasi/pengaturan_kantin.zul (PengaturanKantinAction)
    Kontrak     : NewUiKoperasiOperasiController
    Catatan     : layar ini tidak punya data sendiri, hanya wadah tab atas
                  sebelas layar master lain. Daftar tabnya dibaca dari konstanta
                  PengaturanKantinAction.TABS supaya tidak pernah menyimpang.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.koperasi.NewUiKoperasiOperasiController" %>
<%
NewUiKoperasiOperasiController.handle(request, response,
        NewUiKoperasiOperasiController.MODE_PENGATURAN, "pengaturan_kantin");
%>
