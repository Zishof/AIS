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
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.master.NewUiMasterUmumController" %>
<%
NewUiMasterUmumController.handle(request, response,
        NewUiMasterUmumController.MODE_KONFIGURASI_SEKOLAH, "konfigurasi_sekolah");
%>
