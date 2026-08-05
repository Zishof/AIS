<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.BankHost"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
String[] cols = { "nama", "ip", "keterangan" };
String[] labels = { "Nama", "IP", "Keterangan" };
String[] search = { "nama" };
String[] forms = { "nama", "ip", "username", "password", "keterangan" };
Set<String> paramRequired = new HashSet<String>();
paramRequired.add("nama");

Set<String> paramTidakBolehSama = new HashSet<String>();

// Generate HTML
String htmlOutput = DynamicTableGenerator.generateTable(cols, labels, search, forms, Common.getGeneratedBarCode(5),
		"Bank Host", BankHost.class.getName(), paramRequired, paramTidakBolehSama);

out.println(htmlOutput);
%>

