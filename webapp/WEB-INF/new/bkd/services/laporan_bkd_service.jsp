<%--
    Adaptor native: Laporan Kinerja

    Sumber ZK   : /pages/master/bkd/laporan.zul (LaporanBKDAction)
    Kontrak     : ais.common.newui.bkd.NewUiBkdController (mode laporan)
    Catatan     : wadah tiga laporan Jasper. Bentuk ringkasan menerima pegawai
                  secara opsional -- kosong dikirim sebagai -1 persis seperti
                  layar ZK sehingga laporan mencakup seluruh dosen; bentuk
                  peringkat tidak menerima pegawai melainkan pilihan urutan.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.bkd.NewUiBkdController" %>
<%
NewUiBkdController.handle(request, response,
        NewUiBkdController.MODE_LAPORAN, "laporan_bkd");
%>
