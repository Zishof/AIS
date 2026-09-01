<%--
    Adaptor native: Asesemen Kinerja

    Sumber ZK   : /pages/master/bkd/asesemen.zul (AsesementAction)
    Kontrak     : ais.common.newui.bkd.NewUiBkdController (mode asesemen)
    Catatan     : layar ini wadah bertab dua tingkat atas empat belas panel
                  penilaian. Daftarnya dibaca dari konstanta AsesementAction.TABS
                  yang juga dipakai layar ZK untuk memuat panelnya, sehingga
                  keduanya tidak dapat menyimpang.
    Batas       : tiga panel (Penelitian, Publikasi Ilmiah, Pengabdian) dibangun
                  langsung oleh helper dan tidak punya halaman tersendiri; panel
                  itu diumumkan tanpa rute beserta alasannya, bukan disembunyikan.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.bkd.NewUiBkdController" %>
<%
NewUiBkdController.handle(request, response,
        NewUiBkdController.MODE_ASESEMEN, "asesement");
%>
