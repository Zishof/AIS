<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Jenjang"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
String[] cols = { "kode", "nama", "keterangan", "keteranganEn", "syarat",  "jumlahSemesterLulus",
		"jumlahSemesterMaksimal", "aktifDipilih", "aktif" };
String[] labels = { "Kode", "Nama", "Keterangan", "Keterangan (EN)", "Syarat", 
		"Jml. Semester Lulus", "Maks. Semester", "Aktif Dipilih", "Aktif" };
String[] search = { "kode", "nama" };
String[] forms = { "kode", "nama", "keterangan", "keteranganEn", "syarat",  "jumlahSemesterLulus",
		"jumlahSemesterMaksimal", "aktifDipilih", "aktif" };
Set<String> paramRequired = new HashSet<String>();
paramRequired.add("nama");

Set<String> paramTidakBolehSama = new HashSet<String>();

// Generate HTML
String htmlOutput = DynamicTableGenerator.generateTable(cols, labels, search, forms, Common.getGeneratedBarCode(5),
		"Jenjang", Jenjang.class.getName(), paramRequired, paramTidakBolehSama);

out.println(htmlOutput);
%>

