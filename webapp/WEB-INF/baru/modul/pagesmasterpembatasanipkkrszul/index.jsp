<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.Common"%>
<%@page
	import="ais.database.model.PembatasanNilaiIPKUntukPengambilanKRS"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
String[] cols = { "batasTerendahIPK", "batasMaksimumIPKYangBolehDiambil", "fakultas", "jurusan", "program",
		"minimumAngkatan", "semesterPendek", "aktif" };
String[] labels = { "Min. IPK", "Maks IPK", "Fakultas", "Jurusan", "Program", "Min. Angkatan", "Smt. Pendek", "Aktif" };
String[] search = { "fakultas", "jurusan", "program" };
String[] forms = { "batasTerendahIPK", "batasMaksimumIPKYangBolehDiambil", "fakultas", "jurusan", "program",
		"minimumAngkatan", "keterangan", "mahasiswa", "semesterPendek", "aktif" };
Set<String> paramRequired = new HashSet<String>();
paramRequired.add("batasTerendahIPK");
paramRequired.add("batasMaksimumIPKYangBolehDiambil");

Set<String> paramTidakBolehSama = new HashSet<String>();

// Generate HTML
String htmlOutput = DynamicTableGenerator.generateTable(cols, labels, search, forms, Common.getGeneratedBarCode(5),
		"Pembatasan IPK KRS", PembatasanNilaiIPKUntukPengambilanKRS.class.getName(), paramRequired,
		paramTidakBolehSama);

out.println(htmlOutput);
%>