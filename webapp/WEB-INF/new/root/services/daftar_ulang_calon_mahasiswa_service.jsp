<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%
// Kasir pendaftaran calon mahasiswa - paritas daftarulang_mahasiswa_calon.zul
// (subclass Baru: jenis dipatok PENDAFTARAN_CALON_MAHASISWA, semester 0).
// Registrasi read-only GenericCrudDefinitionRegistry.buildDaftarUlangCalonMahasiswa
// tetap ada untuk jalur admin; route menu ini kini dilayani controller kasir.
ais.common.newui.kampus.NewUiDaftarUlangMahasiswaController.handle(request, response,
        ais.common.newui.kampus.NewUiDaftarUlangMahasiswaController.MODE_CALON);
%>