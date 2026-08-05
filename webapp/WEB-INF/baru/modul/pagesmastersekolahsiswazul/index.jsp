<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
String[] cols = { "nim", "nama", "jenisKelamin", "tahunMasuk" };
String[] labels = { "NIS", "Nama", "Jenis Kelamin", "Angkatan" };
String[] search = { "nama", "nim", "sekolah", "tahunMasuk" };
String[] forms = { "idfinger","kelasLes", "asrama", "sekolah", "agama", "program", "alamatEmail",
		"alamatOrangTua", "alamatSiswa", "alamatWali", "anakKe", "dariAnakKe", "diterimaDiSekolahIniDiKelas",
		"jenisKelamin", "namaAyah", "namaIbu", "namaSiswa", "namaWali", "nomorInduk", "nomorIndukSantri", "padaTanggal",
		"pekerjaanAyah", "pekerjaanIbu", "pekerjaanWali", "sekolahAsal", "statusDalamKeluarga", "tahunMasuk",
		"bulanMasuk", "tahunLulus", "tanggalLahir", "tanggalLulus", "teleponOrangTua", "teleponSiswa", "teleponWali",
		"tempatLahir", "tempatLahirWali", "bahasa", "berat", "golonganDarah", "hobby", "hp1ayah", "hp1ibu", "hp2ayah",
		"hp2ibu", "hp3ayah", "hp3ibu", "jumlahSaudaraKandung", "jumlahSaudaraTiri", "kewarganegaraan", "kondisiSiswa",
		"panggilan", "pendidikanAyah", "pendidikanIbu", "penghasilanAyah", "penghasilanIbu", "riwayatPenyakit",
		"statusSiswa", "tanggalLahirAyah", "tanggalLahirIbu", "tempatLahirAyah", "tempatLahirIbu", "tinggi", "hobbyS",
		"pendidikanWali", "penghasilanAyahS", "penghasilanIbuS", "penghasilanWali", "nomorIndukNasional", "kodeUniq",
		"keterangan", "tanggalLahirWali", "negara", "aktif" };
Set<String> paramRequired = new HashSet<String>();
paramRequired.add("nama");
paramRequired.add("jenisKelamin");
paramRequired.add("tempatLahir");
paramRequired.add("tanggalLahir");

Set<String> paramTidakBolehSama = new HashSet<String>();

// Generate HTML
String htmlOutput = DynamicTableGenerator.generateTable(cols, labels, search, forms, Common.getGeneratedBarCode(5),
		"Siswa", Siswa.class.getName(), paramRequired, paramTidakBolehSama);

out.println(htmlOutput);
%>

