<%--
    Adaptor native: Toko Online (Belanja)

    Sumber ZK   : /pages/master/koperasi/beranda_anggota_kantin.zul (BerandaAnggotaKantinAction)
    Kontrak     : NewUiKantinMemberController
    Catatan     : klien hanya mengirim (produk, toko, jumlah). Harga satuan
                  dibaca ulang dari basis data dan diskon dinilai
                  KantinDiskonEngine — mesin yang sama dengan layar ZK —
                  supaya potongan tidak dapat dikarang dari sisi klien.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.koperasi.NewUiKantinMemberController" %>
<%
NewUiKantinMemberController.handle(request, response,
        NewUiKantinMemberController.MODE_BERANDA, "beranda_anggota_kantin");
%>
