<%--
    Adaptor native: Konfigurasi Sekolah

    Sumber ZK   : /pages/master/konfigurasi_sekolah.zul (KonfigurasiSekolahAction)
    Kontrak     : ais.common.newui.master.NewUiMasterUmumController (mode konfigurasi_sekolah)
    Catatan     : skema dibaca dari SkemaKonfigurasi.SEKOLAH.
    Batas       : baris berupa unggahan berkas (logo, banner, tanda tangan,
                  stempel, alur PDF) BUKAN pasangan kunci-nilai sehingga tidak
                  dapat disunting di sini. Daftarnya tetap diumumkan agar
                  pengguna tidak mengira seluruh pengaturan sudah tampil.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "root");
request.setAttribute("nuiPage", "konfigurasi_sekolah");
request.setAttribute("nuiPageTitle", "Konfigurasi Sekolah");
request.setAttribute("nuiPageType", "form");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
