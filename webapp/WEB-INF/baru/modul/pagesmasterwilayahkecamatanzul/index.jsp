<%@page import="java.util.Map"%>
<%@page import="ais.database.model.Wilayah"%>
<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
String[] cols = { "kode", "nama", "induk", "aktif" };
String[] labels = { "Kode", "Nama Kecamatan", "Kabupaten/Kota", "Status" };
String[] search = { "kode", "nama", "induk", "aktif" };
String[] forms = { "kode", "nama", "induk", "aktif" };
Set<String> paramRequired = new HashSet<String>();
paramRequired.add("kode");
paramRequired.add("nama");


String sqlWhere = "level = 3";

Set<String> paramTidakBolehSama = new HashSet<String>();
paramTidakBolehSama.add("kode");

Map<String,String> hiddenValues = null;
// Generate HTML
String htmlOutput = DynamicTableGenerator.generateTable(cols, labels, search, forms, Common.getGeneratedBarCode(5),
		"Kecamatan", Wilayah.class.getName(), paramRequired, paramTidakBolehSama, sqlWhere, hiddenValues);

out.println(htmlOutput);
%>