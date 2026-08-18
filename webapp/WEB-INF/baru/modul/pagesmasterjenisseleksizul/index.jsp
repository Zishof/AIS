<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.JenisSeleksi"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
String[] cols = { "kode", "nama", "deskripsi", "aktif", "keterangan" };
String[] labels = { "Kode", "Nama", "Deskripsi", "Aktif", "Keterangan" };
String[] search = { "kode", "nama" };
String[] forms = { "kodeLain", "kode", "nama", "deskripsi", "keterangan", "feeder", "aktif", "jenisDiskonMahasiswa",
		"kelompokJenisSeleksi" };
Set<String> paramRequired = new HashSet<String>();
paramRequired.add("nama");

Set<String> paramTidakBolehSama = new HashSet<String>();

// Generate HTML
String htmlOutput = DynamicTableGenerator.generateTable(cols, labels, search, forms, Common.getGeneratedBarCode(5),
		"Jenis Seleksi", JenisSeleksi.class.getName(), paramRequired, paramTidakBolehSama);

out.println(htmlOutput);
%>
