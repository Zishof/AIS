<%--
    Adaptor native: Proses Poin Bulanan

    Sumber ZK   : /pages/master/koperasi/proses_poin_bulanan.zul (ProsesPoinBulananAction)
    Kontrak     : NewUiKoperasiOperasiController
    Catatan     : proses aman diulang; voucher untuk periode yang sama tidak
                  diterbitkan dua kali (sifat ProsesPoinBulananHelper).
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.koperasi.NewUiKoperasiOperasiController" %>
<%
NewUiKoperasiOperasiController.handle(request, response,
        NewUiKoperasiOperasiController.MODE_POIN, "proses_poin_bulanan");
%>
