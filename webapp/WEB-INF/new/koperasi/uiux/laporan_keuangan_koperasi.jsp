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
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "koperasi");
request.setAttribute("nuiModuleLabel", "Koperasi & Unit Usaha");
request.setAttribute("nuiPage", "laporan_keuangan_koperasi");
request.setAttribute("nuiPageTitle", "Laporan Keuangan Koperasi");
request.setAttribute("nuiPageType", "report");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
