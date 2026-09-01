<%--
    Adaptor native: Persetujuan Pengajuan Transaksi Pegawai

    Sumber ZK   : /pages/master/payroll/persetujuan_pengajuan_transaksi_pegawai.zul (PersetujuanPengajuanTransaksiPegawaiAction)
    Kontrak     : ais.common.newui.employ.NewUiKepegawaianController (mode persetujuan_transaksi_pegawai)
    Batas       : BACA SAJA. Menyetujui bukan sekadar menandai satu kolom:
                  transaksi yang disetujui masuk ke rangkaian penggajian dan
                  disposisi SOP berikutnya. Persetujuan tetap di layar lama.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.employ.NewUiKepegawaianController" %>
<%
NewUiKepegawaianController.handle(request, response,
        NewUiKepegawaianController.MODE_PERSETUJUAN_TRANSAKSI, "persetujuan_pengajuan_transaksi_pegawai");
%>
