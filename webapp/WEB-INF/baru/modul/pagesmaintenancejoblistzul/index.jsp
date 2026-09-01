<%@page import="ais.database.model.Tbmrole"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.database.model.file.FotoAdmin"%>
<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.sekolah.Yayasan"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
	/* Snapshot lama membuat menu id=4 ("Ubah Password") menunjuk URL halaman
	   role ini. Flutter sudah mengarahkannya menurut ID ke endpoint password
	   milik sendiri; permintaan API generik tetap harus ditolak agar pemilik
	   menu Ubah Password tidak memperoleh CRUD Grup Pengguna. */
	String actionNative = request.getParameter("action");
	String menuIdNative = request.getParameter("menuId");
	if (actionNative != null && "4".equals(menuIdNative)) {
		response.setStatus(409);
		out.print("{\"success\":false,\"code\":\"PASSWORD_ROUTE_MISMATCH\","
				+ "\"message\":\"Menu Ubah Password wajib memakai endpoint kredensial milik sendiri.\"}");
		return;
	}


String[] cols = { "kode", "roleName", "satuanKerjas", "program", "jurusan", "fakultas", "yayasan", "sekolah", "aktif" };
String[] labels = { "KODE", "Nama", "Satker", "Program", "Jurusan", "Fakultas", "Yayasan", "Sekolah", "Aktif" };
String[] search = { "kode", "roleName", "satuanKerjas","program", "jurusan", "fakultas", "aktif"};
String[] forms = { "kode", "roleName", "satuanKerjas", "aktif", "jenisJabatan", "program", "jurusan", "fakultas",
		"yayasan", "sekolah", "elearning", "pustaka", "dashboard", "workflow", "kegiatanDanPrestasi", "pengadaan",
		"keuangan", "kepegawaian", "presensiKehadiran", "absenLangsung", "pembayaran", "kalenderAkademik",
		"infoKegiatan", "melihatDataPegawaiLain", "melihatDataSatkerLain", "melihatSemuaSurat", "updateFormatLaporan" };
Set<String> paramRequired = new HashSet<String>();
paramRequired.add("kode");
paramRequired.add("roleName");

Set<String> paramTidakBolehSama = new HashSet<String>();
paramTidakBolehSama.add("roleName");

// Generate HTML
String htmlOutput = DynamicTableGenerator.generateTable(cols, labels, search, forms, Common.getGeneratedBarCode(5),
		"Hak Akses", Tbmrole.class.getName(), paramRequired, paramTidakBolehSama);

out.println(htmlOutput);
%>



