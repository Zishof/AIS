<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.database.model.file.FotoAdmin"%>
<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.sekolah.JenisPenilaian"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
String[] cols = {"nama","jenis","sekolah","yayasan","aktif"};
String[] labels = { "Nama","Jenis","Sekolah","Yayasan","Aktif" };
String[] search = { "nama","jenis","Sekolah","Yayasan", "aktif" };
String[] forms = { "nama","sekolah","jenis","yayasan","aktif" };
Set<String> paramRequired = new HashSet<String>();
paramRequired.add("nama");
paramRequired.add("jenis");

Set<String> paramTidakBolehSama = new HashSet<String>();
paramTidakBolehSama.add("nama");

// Generate HTML
String htmlOutput = DynamicTableGenerator.generateTable(cols, labels, search, forms, Common.getGeneratedBarCode(5),
		"Jenis Penilaian", JenisPenilaian.class.getName(), paramRequired, paramTidakBolehSama);

out.println(htmlOutput);
%>


"nama","sekolah","jenis","yayasan","aktif"



