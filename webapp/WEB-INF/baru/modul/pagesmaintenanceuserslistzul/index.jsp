<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.database.model.file.FotoAdmin"%>
<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%


		JSONObject lk = new JSONObject();
		lk.put("id", "Laki-laki");
		lk.put("nama", "Laki-laki");

		JSONObject pr = new JSONObject();
		pr.put("id", "Perempuan");
		pr.put("nama", "Perempuan");

		JSONObject belum = new JSONObject();
		belum.put("id", "");
		belum.put("nama", "Belum Ditentukan");

		JSONArray jsonArray = new JSONArray();
		jsonArray.put(belum);
		jsonArray.put(pr);
		jsonArray.put(lk);

String[] cols = {  "userId","userNama","kelamin;" + jsonArray.toString(), "jurusan", "fakultas", "sekolah", "aktif" };
String[] labels = { "ID","Nama Lengkap","Jenis Kelamin", "Jurusan", "Fakultas", "Sekolah", "Aktif" };
String[] search = { "userNama", "userId","kelamin;" + jsonArray.toString() };
String[] forms = { "userId", "userNama", "userRole", "userRole2", "userRole3", "userRole4", "userRole5",
		"dosen", "guru", "pegawai",
		"email", "hp", "program", "jurusan", "fakultas", "yayasan", "sekolah", "perguruanTinggi",
		"aktif" };
Set<String> paramRequired = new HashSet<String>();
paramRequired.add("userId");
paramRequired.add("userNama");

Set<String> paramTidakBolehSama = new HashSet<String>();
paramTidakBolehSama.add("userId");

// Generate HTML
String htmlOutput = DynamicTableGenerator.generateTable(cols, labels, search, forms, Common.getGeneratedBarCode(5),
		"Pengguna", Tbmuser.class.getName(), paramRequired, paramTidakBolehSama);

out.println(htmlOutput);
%>
