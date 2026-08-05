<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.employ.JenisJabatan"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
String[] cols = { "kode", "nama", "aktif" };
String[] labels = { "Kode", "Nama", "Aktif" };
String[] search = { "nama", "aktif" };
String[] forms = { "kode", "nama", "keterangan", "key", "aktif", "nomorUrut", "grup", "usernamePengguna",
		"jenisPengguna" };
Set<String> paramRequired = new HashSet<String>();
paramRequired.add("kode");
paramRequired.add("nama");

Set<String> paramTidakBolehSama = new HashSet<String>();

// Generate HTML
String htmlOutput = DynamicTableGenerator.generateTable(cols, labels, search, forms, Common.getGeneratedBarCode(5),
		"Jenis Jabatan", JenisJabatan.class.getName(), paramRequired, paramTidakBolehSama);

out.println(htmlOutput);
%>
