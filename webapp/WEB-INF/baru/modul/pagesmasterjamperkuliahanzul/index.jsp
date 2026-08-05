<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.JamPerkuliahan"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
String[] cols = { "nama","jurusan","fakultas","waktuMulai","waktuSelesai","aktif" };
String[] labels = { "Nama","Jurusan","Fakultas","Waktu Mulai","Waktu Selesai","Aktif" };
String[] search = { "nama" };
String[] forms = { "nama","mulai","sampai","jurusan","fakultas","program","keterangan","waktuMulai","waktuSelesai","sks","aktif"};
Set<String> paramRequired = new HashSet<String>();
paramRequired.add("nama");
paramRequired.add("mulai");
paramRequired.add("sampai");

Set<String> paramTidakBolehSama = new HashSet<String>();

// Generate HTML
String htmlOutput = DynamicTableGenerator.generateTable(cols, labels, search, forms, Common.getGeneratedBarCode(5),
		"Jam Perkuliahan", JamPerkuliahan.class.getName(), paramRequired, paramTidakBolehSama);

out.println(htmlOutput);
%>
