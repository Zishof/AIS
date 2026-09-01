<%--
    Adaptor native: Target Kerja Pegawai Tahunan

    Sumber ZK   : /pages/master/lkp/target_kerja_pegawai_tahunan.zul (TargetKerjaPegawaiTahunanAction)
    Kontrak     : ais.common.newui.lkp.NewUiLkpTahunanController (mode target)
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
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "lkp");
request.setAttribute("nuiPage", "target_kerja_pegawai_tahunan");
request.setAttribute("nuiPageTitle", "Target Kerja Pegawai Tahunan");
request.setAttribute("nuiPageType", "list");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
