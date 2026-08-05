<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Jurusan"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
String[] cols = { "kode", "nama", "fakultas", "aktif" };
String[] labels = { "Kode", "Nama", "Fakultas", "Aktif" };
String[] search = { "kode", "nama", "fakultas", "aktif" };
String[] forms = { "nama", "namaEn", "fakultas", "kode", "jenjang", "kodeEpsbed", "kodeLain", "bahasaPengantar",
		"kaprodi", "gelar", "singkatanGelar", "gelarEn", "singkatanGelarEn", "peringkatAkreditasi", "akreditasi",
		"noSkAkreditasi", "tanggalAkreditasi", "jenjangProgramStudi", "deskripsi", "grupJurusan", "alamat", "alamat2",
		"aktif", "jenisJurusan", "statusAkreditasi", "feeder", "rasioDosenDanMahasiswa", "labelPejabat1",
		"labelPejabat2", "labelPejabat3", "pegawai1", "pegawai2", "pegawai3", "wa", "akreditasiKes",
		"statusAkreditasiKes", "peringkatAkreditasiKes", "noSkAkreditasiKes", "tanggalAkreditasiKes", "satuanKerja",
		"dosenHarusPakaiSatuanKerja" };
Set<String> paramRequired = new HashSet<String>();
paramRequired.add("kode");
paramRequired.add("nama");

Set<String> paramTidakBolehSama = new HashSet<String>();
paramTidakBolehSama.add("kode");

// Generate HTML
String htmlOutput = DynamicTableGenerator.generateTable(cols, labels, search, forms, Common.getGeneratedBarCode(5),
		"Program Studi", Jurusan.class.getName(), paramRequired, paramTidakBolehSama);

out.println(htmlOutput);
%>


