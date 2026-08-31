<%--
    Adaptor native: Laporan Keuangan Koperasi

    Sumber ZK   : /pages/master/koperasi/laporan_keuangan_koperasi.zul (LaporanKeuanganKoperasiAction)
    Kontrak     : NewUiLaporanKeuanganKoperasiController
    Batas       : yang disajikan adalah laporan keuangan resmi (Neraca/Laba
                  Rugi/Arus Kas) dari LaporanKeuanganCoaHelper.susun(). Dasbor
                  ikhtisar dan CALK pada layar ZK belum dipisahkan dari
                  penyajiannya sehingga belum tercakup; klien membaca
                  bagianTersedia, bukan mengandaikan seluruh layar tercakup.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.koperasi.NewUiLaporanKeuanganKoperasiController" %>
<%
NewUiLaporanKeuanganKoperasiController.handle(request, response, "laporan_keuangan_koperasi");
%>
