<%--
    Adaptor native: Pengaturan Konfigurasi SKP

    Sumber ZK   : /pages/master/konfigurasi_lkp.zul (KonfigurasiLkpAction)
    Kontrak     : ais.common.newui.master.NewUiMasterUmumController (mode konfigurasi_skp)
    Catatan     : kunci, label, dan nilai bawaannya dibaca dari
                  SkemaKonfigurasi.SKP -- sumber yang sama dengan layar ZK.
                  Common.getKonfigurasi MEMBUAT baris dengan bawaan yang disebut
                  pemanggil bila belum ada, sehingga bawaan yang berbeda antara
                  dua layar akan menetapkan nilai berbeda secara permanen.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.master.NewUiMasterUmumController" %>
<%
NewUiMasterUmumController.handle(request, response,
        NewUiMasterUmumController.MODE_KONFIGURASI_SKP, "konfigurasi_lkp");
%>
