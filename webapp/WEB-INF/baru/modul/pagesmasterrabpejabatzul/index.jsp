
<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.rab.Pejabat"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
String[] cols = { "nama", "keterangan", "jenisJabatan", "aktif" };
String[] labels = { "Nama", "Keterangan", "Jenis", "Aktif" };
String[] search = { "nama",  "aktif" };
String[] forms = { "pegawai", "dosen", "guru", "jenisJabatan", "aktif", "keterangan", "nama", "jenisPengguna",
		"usernamePengguna" };
Set<String> paramRequired = new HashSet<String>();
paramRequired.add("nama");

Set<String> paramTidakBolehSama = new HashSet<String>();

// Generate HTML
String htmlOutput = DynamicTableGenerator.generateTable(cols, labels, search, forms, Common.getGeneratedBarCode(5),
		"Pejabat", Pejabat.class.getName(), paramRequired, paramTidakBolehSama);

out.println(htmlOutput);
%>
