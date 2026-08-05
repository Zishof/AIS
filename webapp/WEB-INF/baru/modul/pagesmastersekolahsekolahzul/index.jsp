<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.database.model.file.FotoAdmin"%>
<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
String[] cols = {"nama","jenisSekolah","namaKepalaSekolah","nss","npsn","aktif"};
String[] labels = { "Nama","Jenis Sekolah","Kepala Sekolah","NSS","NPSN","Aktif"};
String[] search = { "nama", "aktif" };
String[] forms = { "jenisSekolah","yayasan","alamat","email","motto","fax","nama","namaKepalaSekolah","nipKepalaSekolah","namaWakilKepalaSekolah","nipWakilKepalaSekolah","nss","npsn","wa","telp","aktif" };
Set<String> paramRequired = new HashSet<String>();
paramRequired.add("nama");

Set<String> paramTidakBolehSama = new HashSet<String>();
paramTidakBolehSama.add("nama");

// Generate HTML
String htmlOutput = DynamicTableGenerator.generateTable(cols, labels, search, forms, Common.getGeneratedBarCode(5),
		"Sekolah", Sekolah.class.getName(), paramRequired, paramTidakBolehSama);

out.println(htmlOutput);
%>








