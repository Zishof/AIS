<%--
    Adaptor native: Realisasi Kerja Pegawai Tahunan

    Sumber ZK   : /pages/master/lkp/realisasi_kerja_pegawai_tahunan.zul (RealisasiKerjaPegawaiTahunanAction)
    Kontrak     : ais.common.newui.lkp.NewUiLkpTahunanController (mode realisasi)
    Catatan     : PENYARINGAN ASESOR. Memilih seorang pegawai TIDAK menyaring
                  baris menjadi miliknya saja, melainkan miliknya beserta
                  seluruh pegawai yang ia asesmen (AsesorPegawai aktif) --
                  persis seperti layar ZK. Menyaring dengan pegawai = ? saja
                  menghasilkan daftar lebih pendek yang tampak wajar.
    Batas       : BACA SAJA. Penambahan target menuntut pemilih pohon kegiatan
                  tugas jabatan, pencatatan realisasi ditangani layar
                  tersendiri yang membawa parameter tambahan per kegiatan, dan
                  penandaan verifikasi kewenangan terpisah.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.lkp.NewUiLkpTahunanController" %>
<%
NewUiLkpTahunanController.handle(request, response,
        NewUiLkpTahunanController.MODE_REALISASI, "realisasi_kerja_pegawai_tahunan");
%>
