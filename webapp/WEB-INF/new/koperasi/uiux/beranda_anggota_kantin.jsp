<%--
    Adaptor native: Toko Online (Belanja)

    Sumber ZK   : /pages/master/koperasi/beranda_anggota_kantin.zul (BerandaAnggotaKantinAction)
    Kontrak     : NewUiKantinMemberController
    Catatan     : klien hanya mengirim (produk, toko, jumlah). Harga satuan
                  dibaca ulang dari basis data dan diskon dinilai
                  KantinDiskonEngine — mesin yang sama dengan layar ZK —
                  supaya potongan tidak dapat dikarang dari sisi klien.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "koperasi");
request.setAttribute("nuiModuleLabel", "Koperasi & Unit Usaha");
request.setAttribute("nuiPage", "beranda_anggota_kantin");
request.setAttribute("nuiPageTitle", "Toko Online (Belanja)");
request.setAttribute("nuiPageType", "form");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
