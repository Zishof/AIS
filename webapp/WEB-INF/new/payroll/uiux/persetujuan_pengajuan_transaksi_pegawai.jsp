<%--
    Adaptor native: Persetujuan Pengajuan Transaksi Pegawai

    Sumber ZK   : /pages/master/payroll/persetujuan_pengajuan_transaksi_pegawai.zul (PersetujuanPengajuanTransaksiPegawaiAction)
    Kontrak     : ais.common.newui.employ.NewUiKepegawaianController (mode persetujuan_transaksi_pegawai)
    Batas       : BACA SAJA. Menyetujui bukan sekadar menandai satu kolom:
                  transaksi yang disetujui masuk ke rangkaian penggajian dan
                  disposisi SOP berikutnya. Persetujuan tetap di layar lama.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "payroll");
request.setAttribute("nuiPage", "persetujuan_pengajuan_transaksi_pegawai");
request.setAttribute("nuiPageTitle", "Persetujuan Pengajuan Transaksi Pegawai");
request.setAttribute("nuiPageType", "list");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
