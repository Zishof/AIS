<%--
    Adaptor native: Pembagian SHU

    Sumber ZK   : /pages/master/koperasi/pembagian_shu.zul (PembagianShuAction)
    Kontrak     : NewUiKoperasiOperasiController
    Catatan     : rumus pembagian milik PembagianShuHelper yang juga dipakai
                  layar ZK, sehingga bagian tiap anggota identik dari layar mana
                  pun. Menghitung ulang tahun yang sama bersifat mengganti.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.koperasi.NewUiKoperasiOperasiController" %>
<%
NewUiKoperasiOperasiController.handle(request, response,
        NewUiKoperasiOperasiController.MODE_SHU, "pembagian_shu");
%>
