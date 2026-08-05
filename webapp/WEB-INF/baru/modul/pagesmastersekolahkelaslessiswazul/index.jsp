<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.sekolah.KelasLesSiswa"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
String[] cols = { "ruang", "nama","sekolah", "aktif" };
String[] labels = { "Ruang", "Nama", "Sekolah"," Sekolah", "Aktif"};
String[] search = { "ruang", "nama","sekolah", "aktif" };
String[] forms = { "ruang","sekolah","matapelajaran","nama","keterangan","aktif","tingkat","yayasan","guruPembina","absensi","namaAr","namaEn","namaCh","absensiharusGuruPembina","publikasiNilaiHarusTelahDiverifikasi","guruBolehMemverifikasiSendiri",
		"masaJadwalPelajaran","jenisBiayaSekolah","syaratPengaturanPembayaran","sertifikat"};
Set<String> paramRequired = new HashSet<String>();
paramRequired.add("nama");

Set<String> paramTidakBolehSama = new HashSet<String>();
paramTidakBolehSama.add("kodeRuangan");
// Generate HTML
String htmlOutput = DynamicTableGenerator.generateTable(cols, labels, search, forms, Common.getGeneratedBarCode(5),
		"Kelas Les", KelasLesSiswa.class.getName(), paramRequired, paramTidakBolehSama);

out.println(htmlOutput);
%>


