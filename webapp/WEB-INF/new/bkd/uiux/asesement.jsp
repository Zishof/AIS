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
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "bkd");
request.setAttribute("nuiPage", "asesement");
request.setAttribute("nuiPageTitle", "Asesemen Kinerja");
request.setAttribute("nuiPageType", "dashboard");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
