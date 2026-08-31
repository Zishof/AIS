<%--
    Adaptor native: Pembagian SHU

    Sumber ZK   : /pages/master/koperasi/pembagian_shu.zul (PembagianShuAction)
    Kontrak     : NewUiKoperasiOperasiController
    Catatan     : rumus pembagian milik PembagianShuHelper yang juga dipakai
                  layar ZK, sehingga bagian tiap anggota identik dari layar mana
                  pun. Menghitung ulang tahun yang sama bersifat mengganti.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "koperasi");
request.setAttribute("nuiModuleLabel", "Koperasi & Unit Usaha");
request.setAttribute("nuiPage", "pembagian_shu");
request.setAttribute("nuiPageTitle", "Pembagian SHU");
request.setAttribute("nuiPageType", "form");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
