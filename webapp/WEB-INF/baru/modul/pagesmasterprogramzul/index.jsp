<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Program"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
String[] cols = { "nama","namaEn","keterangan" };
String[] labels = { "Nama","Nama (En)","Keterangan" };
String[] search = { "nama" };
String[] forms = { "nama","namaBaru","namaEn","keterangan", "num" };
Set<String> paramRequired = new HashSet<String>();
paramRequired.add("nama");

Set<String> paramTidakBolehSama = new HashSet<String>();

// Generate HTML
String htmlOutput = DynamicTableGenerator.generateTable(cols, labels, search, forms, Common.getGeneratedBarCode(5),
		"Program", Program.class.getName(), paramRequired, paramTidakBolehSama);

out.println(htmlOutput);
%>

