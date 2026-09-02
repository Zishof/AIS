<%-- Dialihkan dari scaffold, 2026-09-02.
Varian dasar (bukan Admin): seluruh tab terikat pada mahasiswa pemilik sesi.
Daftar tabnya berbeda dari varian Admin dan punya tab "Form Kegiatan" yang
tidak ada di sana, jadi memakai ulang mode Admin akan menghilangkan satu tab.
--%><%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.lainnya.NewUiLayarLainnyaController" %>
<%
NewUiLayarLainnyaController.handle(request, response,
    NewUiLayarLainnyaController.MODE_DASBOR_KEMAHASISWAAN_SAYA,
    "admin/dashboard_kegiatan_kemahasiswaan");
%>
