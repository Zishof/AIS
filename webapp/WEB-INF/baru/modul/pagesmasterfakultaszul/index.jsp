<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Fakultas"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
String[] cols = { "kode", "nama", "dekan", "aktif"};
String[] labels = {  "Kode", "Nama", "Dekan", "Aktif" };
String[] search = { "kode", "nama" };
String[] forms = {"kode", "nama", "dekan", "aktif"  };
Set<String> paramRequired = new HashSet<String>();
paramRequired.add("nama");

Set<String> paramTidakBolehSama = new HashSet<String>();
paramTidakBolehSama.add("nama");

// Generate HTML
String htmlOutput = DynamicTableGenerator.generateTable(cols, labels, search, forms, Common.getGeneratedBarCode(5),
		"Fakultas", Fakultas.class.getName(), paramRequired, paramTidakBolehSama);

out.println(htmlOutput);
%>

