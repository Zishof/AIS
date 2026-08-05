<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.sekolah.KelasSiswa"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
String[] cols = { "ruang","nama","keterangan","tingkat","sekolah", "aktif" };
String[] labels = { "Ruang", "Nama", "Keterangan","Tingkat","Sekolah", "Aktif"};
String[] search = { "ruang", "nama","sekolah", "tingkat","aktif" };
String[] forms = { "ruang","nama","keterangan","aktif","tingkat","sekolah","yayasan","tahunAjaran","kurikulumSekolah","guruPembina","guruBk","absensi","namaAr","namaEn","namaCh","mpYgTidakDiambil","absensiharusGuruPembina","absensiharusGuruBk","publikasiNilaiHarusTelahDiverifikasi","guruBolehMemverifikasiSendiri"};
Set<String> paramRequired = new HashSet<String>();
paramRequired.add("nama");
paramRequired.add("tingkat");

Set<String> paramTidakBolehSama = new HashSet<String>();

// Generate HTML
String htmlOutput = DynamicTableGenerator.generateTable(cols, labels, search, forms, Common.getGeneratedBarCode(5),
		"Kelas", KelasSiswa.class.getName(), paramRequired, paramTidakBolehSama);

out.println(htmlOutput);
%>


