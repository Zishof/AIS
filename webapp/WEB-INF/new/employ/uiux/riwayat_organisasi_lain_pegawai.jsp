<%--
    Adaptor native: Riwayat Organisasi Lain

    Sumber ZK   : /pages/master/employ/riwayat_organisasi_lain_pegawai.zul (RiwayatOrganisasiLainPegawaiAction)
    Kontrak     : ais.common.newui.employ.NewUiKepegawaianController (mode riwayat_organisasi_lain)
    Catatan     : TERKUNCI IDENTITAS. Pegawai selalu diambil dari sesi, tidak
                  pernah dari parameter. Pada layar ZK kotak pemilih pegawai
                  diisi lalu dinonaktifkan begitu pegawai pemilik sesi
                  diketahui; kontrak menegakkan hal yang sama secara mutlak.
    Batas       : baris yang sudah diverifikasi tidak dapat diubah maupun
                  dihapus, dan status verifikasi TIDAK dapat diubah dari sini —
                  membukanya berarti pegawai berpotensi mengesahkan riwayatnya
                  sendiri. Lampiran foto/berkas belum tercakup.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "employ");
request.setAttribute("nuiPage", "riwayat_organisasi_lain_pegawai");
request.setAttribute("nuiPageTitle", "Riwayat Organisasi Lain");
request.setAttribute("nuiPageType", "list");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
