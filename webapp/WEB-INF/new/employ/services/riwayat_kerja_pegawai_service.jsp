<%--
    Adaptor native: Riwayat Bekerja Pegawai

    Sumber ZK   : /pages/master/employ/riwayat_kerja_pegawai.zul (RiwayatKerjaPegawaiAction)
    Kontrak     : ais.common.newui.employ.NewUiKepegawaianController (mode riwayat_kerja)
    Catatan     : TERKUNCI IDENTITAS. Pegawai selalu diambil dari sesi, tidak
                  pernah dari parameter. Pada layar ZK kotak pemilih pegawai
                  diisi lalu dinonaktifkan begitu pegawai pemilik sesi
                  diketahui; kontrak menegakkan hal yang sama secara mutlak.
    Batas       : baris yang sudah diverifikasi tidak dapat diubah maupun
                  dihapus, dan status verifikasi TIDAK dapat diubah dari sini —
                  membukanya berarti pegawai berpotensi mengesahkan riwayatnya
                  sendiri. Lampiran foto/berkas belum tercakup.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.employ.NewUiKepegawaianController" %>
<%
NewUiKepegawaianController.handle(request, response,
        NewUiKepegawaianController.MODE_RIWAYAT_KERJA, "riwayat_kerja_pegawai");
%>
