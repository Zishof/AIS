<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.DynamicFormTabGenerator"%>
<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Pertemuan"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	return;
}
Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
		request.getParameter("pertemuan").trim(), true);
if (pertemuan != null) {
	//System.out.println("pertemuan -> " + pertemuan.getTopik());
}
String contentModal = request.getParameter("contentModal").trim();
String afterSave = request.getParameter("afterSave") == null ? "" : request.getParameter("afterSave").trim();

if (pertemuan == null) {
	pertemuan = new Pertemuan();
}

String[][] labels = {
		// STEP 1: Perencanaan & Materi (Sampai Tanggal)
		{ "Kemampuan / kompetensi yang ingin dicapai", "Indikator", "Waktu Pembelajaran", "Pengalaman Belajar",
		"Tugas Dan Penilaian", "Bahan Kajian", "Referensi", "Metode Pembelajaran"
		},
		// STEP 2: Waktu, Dosen & Lokasi (Sampai Lokasi)
		{ "Tanggal Rencana Kegiatan Belajar Mengajar", "Tanggal Realisasi Kegiatan Belajar Mengajar", "Waktu Mulai",
		"Waktu Selesai", "Presensi kehadiran harus mengikuti jadwal yang sudah ditentukan",
		"Toleransi presensi online sebelum (menit)", "Toleransi presensi online setelah (menit)",
		"Dosen Tamu I", "Dosen Tamu II", "Mahasiswa Boleh Absen Online", "Dosen Boleh Absen Online"
		},
		// STEP 3: Detail Tambahan (Sisanya)
		{ "Ruang", "Lokasi Koordinat Absen Online", "Jarak Koordinat Absen Online (km)" },
		{ "Catatan Kegiatan Belajar Mengajar" } };

String[][] formCols = {
		// STEP 1
		// PERBAIKAN: waktupembelajaran -> waktuPembelajaran
		{ "topik;text", "indikator;text", "waktuPembelajaran", "pengalamanBelajar;text", "tugasDanPenilaian;text",
		"bukuRujukan1;text", "bukuRujukan2;text", "metodePembelajaran;text" 
		},
		// STEP 2
		// PERBAIKAN: perkulaiahnOnlineHarusSesuaiJadwal -> perkuliahanOnlineHarusSesuaiJadwal
		{ "tanggal", "tanggalRealisasi", "waktuMulai;time", "waktuSelesai;time", "perkuliahanOnlineHarusSesuaiJadwal",
		"bolehAbsenSebelumWaktuMulaiDalamMenit", "bolehAbsenSetelahWaktuMulaiDalamMenit", "dosenTamu",
		"dosenTamu2", "mahasiswaBolehAbsenMenggunakanFoto", "dosenBolehAbsenMenggunakanFoto" 
		},
		// STEP 3
		{ "ruang", "lokasi", "jarak" }, { "catatan;text;editor" } };

Set<String> paramRequired = new HashSet<String>();
paramRequired.add("topik");
paramRequired.add("tanggal");

Set<String> paramTidakBolehSama = new HashSet<String>();
String[] stepTitles = { "Perencanaan dan Materi", "Tanggal, Waktu, dan Dosen", "Ruang, Lokasi, dan Koordinat", "Catatan" };

// Generate HTML Form Dynamic
String htmlOutput = DynamicFormTabGenerator.generateForm(labels, formCols, stepTitles, contentModal, "Data Pertemuan",
		Pertemuan.class.getName(), paramRequired, paramTidakBolehSama, pertemuan, afterSave);

out.println(htmlOutput);
%>