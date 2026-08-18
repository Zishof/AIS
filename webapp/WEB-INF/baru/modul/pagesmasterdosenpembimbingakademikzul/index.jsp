<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.DosenPembimbingAkademikTemporary"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
String[] cols = {  "mahasiswa", "dosen"};
String[] labels = { "Mahasiswa", "Dosen" };
String[] search = { "mahasiswa", "dosen" };
String[] forms = { "mahasiswa", "dosen"};
Set<String> paramRequired = new HashSet<String>();
paramRequired.add("mahasiswa");
paramRequired.add("dosen");

Set<String> paramTidakBolehSama = new HashSet<String>();

// Generate HTML
String htmlOutput = DynamicTableGenerator.generateTable(cols, labels, search, forms, Common.getGeneratedBarCode(5),
		"Dosen Pembimbing Akademik", DosenPembimbingAkademikTemporary.class.getName(), paramRequired, paramTidakBolehSama);

out.println(htmlOutput);
%>
