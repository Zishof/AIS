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
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "root");
request.setAttribute("nuiPage", "konfigurasi_lkp");
request.setAttribute("nuiPageTitle", "Pengaturan Konfigurasi SKP");
request.setAttribute("nuiPageType", "form");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
