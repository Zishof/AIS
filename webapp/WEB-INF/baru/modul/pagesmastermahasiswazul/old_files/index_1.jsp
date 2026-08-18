<%@page import="java.util.Arrays"%>
<%@page import="ais.database.model.file.FotoMahasiswa"%>
<%@page import="ais.database.model.BiodataMahasiswa"%>
<%@page import="ais.common.DynamicTableMultiTabJsonGenerator"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.database.model.Kelas"%>
<%@page import="ais.database.model.Asrama"%>
<%@page import="ais.database.model.KelompokMahasiswa"%>
<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
		String fileData = "file_foto_mahasiswa;{\"jenis\":\"" + FotoMahasiswa.DEFAULT_JENIS + "\",\"clazz\":\""
				+ FotoMahasiswa.class.getName() + "\", \"id\":\"{id_ref}\"}";

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

		// --- 1. Container Utama (JSONArray) ---
		JSONArray wizardConfigs = new JSONArray();

		String[] search = { "nim", "nama", "tahunangkatan", "program", "jurusan", "kelamin;" + jsonArray.toString(),
				"keterangan", "aktif" };

		String[][] forms = {
				// Kelompok 1: Data Akademik & Identitas Utama (Sampai "program")
				{ fileData, "nim", "nama", "tempatlahir", "tanggallahir", "tahunangkatan", "kelamin;" + jsonArray.toString(),
				"jurusan", "semesterMulai", "program" },

				// Kelompok 2: Data Fisik & Agama (Setelah "program" sampai "keterangan")
				{ "berat_badan", "tinggi_badan", "golongan_darah", "agama" },

				// Kelompok 3: Data Kewarganegaraan & Status (Setelah "keterangan" sampai akhir)
				{ "warganegara", "negara", "jenisSeleksi", "ktp", "aktif" },

				// Kelompok 3: Data Kewarganegaraan & Status (Setelah "keterangan" sampai akhir)
				{ "tanggalLulus", "statusKeluar" },

				// Kelompok 3: Data Kewarganegaraan & Status (Setelah "keterangan" sampai akhir)
				{ "keterangan;editor" } };

		String[][] labels = {
				// Kelompok 1: Data Akademik & Identitas Utama (Sampai "program")
				{ "Foto Resmi", "NIM", "Nama", "Tempat Lahir", "Tanggal Lahir", "Tahun Masuk", "Jenis Kelamin", "Jurusan",
				"Semester Mulai Belajar", "Program" },

				// Kelompok 2: Data Fisik & Agama (Setelah "program" sampai "keterangan")
				{ "Berat Badan", "Tinggi Badan", "Golongan Darah", "Agama" },

				// Kelompok 3: Data Kewarganegaraan & Status (Setelah "keterangan" sampai akhir)
				{ "Warganegara", "Negara Asal", "Jenis Seleksi", "Nomor KTP", "Status Aktif" },

				// Kelompok 3: Data Kewarganegaraan & Status (Setelah "keterangan" sampai akhir)
				{ "Tanggal Lulus", "Status Keluar" }, { "Catatan" } };

		Set<String> paramRequired = new HashSet<String>();
		paramRequired.add("kode");
		paramRequired.add("nama");
		paramRequired.add("nim");

		Set<String> paramTidakBolehSama = new HashSet<String>();
		paramTidakBolehSama.add("kode");
		paramTidakBolehSama.add("nim");

		String[] stepTitles = { "Data Mahasiswa", "Biodata", "Pendaftaran", "Kelulusan", "Catatan" };

		String[] tableColumns = { "nim", "nama", "jurusan", "program", "jenisSeleksi", "tahunangkatan", "aktif" };
		String[] tableLabels = { "NIM", "Nama", "Prodi", "Program", "Jalur Masuk", "Tahun Masuk", "Status" };

		// ... kode inisialisasi variabel array Anda sebelumnya (search, forms, dll) tetap ada ...

		// ===========================================================================
		// CONFIG 1: TABEL UTAMA (MASTER) - Identitas & Akademik
		// Class: Mahasiswa
		// ===========================================================================
		JSONObject wizardConfig = new JSONObject();
		wizardConfig.put("sort_default_1", "tahunangkatan");
		wizardConfig.put("order_default_1", "desc");
		wizardConfig.put("sort_default_2", "nim");
		wizardConfig.put("order_default_2", "asc");
		wizardConfig.put("sort_default_3", "");
		wizardConfig.put("order_default_3", "");
		wizardConfig.put("sort_default_4", "");
		wizardConfig.put("order_default_4", "");
		wizardConfig.put("sort_default_5", "");
		wizardConfig.put("order_default_5", "");
		// 2. Masukkan Data Scalar
		wizardConfig.put("dataTypeName", "Mahasiswa");
		wizardConfig.put("modelClass", Mahasiswa.class.getName());
		wizardConfig.put("randomID", Common.getGeneratedBarCode(5));
		wizardConfig.put("sqlWhere", ""); // Opsional
		// wizardConfig.put("hiddenValues", new JSONObject(mapHiddenValues)); // Opsional jika ada map

		// 3. Masukkan Array 1 Dimensi
		wizardConfig.put("searchCols", new JSONArray(Arrays.asList(search)));
		wizardConfig.put("stepTitles", new JSONArray(Arrays.asList(stepTitles)));
		wizardConfig.put("tableColumns", new JSONArray(Arrays.asList(tableColumns)));
		wizardConfig.put("tableLabels", new JSONArray(Arrays.asList(tableLabels)));

		// 4. Masukkan Array 2 Dimensi (Forms & Labels)
		wizardConfig.put("formCols", new JSONArray(Arrays.asList(forms)));
		wizardConfig.put("formLabels", new JSONArray(Arrays.asList(labels)));

		// 5. Masukkan Set (Required & Unique)
		wizardConfig.put("paramRequired", new JSONArray(Arrays.asList(paramRequired)));
		wizardConfig.put("paramTidakBolehSama", new JSONArray(Arrays.asList(paramTidakBolehSama)));

		// Masukkan ke Array Utama
		wizardConfigs.put(wizardConfig);

		// ===========================================================================
		// CONFIG 2: TABEL CHILD - Data Fisik & Agama
		// Class: BiodataFisik (Contoh)
		// ===========================================================================
		JSONObject configBio = new JSONObject();
		// Link ke Class Model Baru
		configBio.put("dataTypeName", "Biodata"); // Judul Tab ke-2
		configBio.put("modelClass", BiodataMahasiswa.class.getName()); // GANTI dengan nama class model Anda

		stepTitles = new String[] { "Biodata", "Alamat", "Keluarga", "Asal Sekolah" };
		configBio.put("stepTitles", new JSONArray(Arrays.asList(stepTitles)));

		// Relasi (PENTING)
		// Artinya: Di tabel BiodataFisik, ada kolom "mahasiswa" yang berisi ID dari tabel Mahasiswa
		configBio.put("relasi_ke_tabel_utama", "mahasiswa");
		configBio.put("relasi_di_tabel_ini", "id");

		// Form Input
		String[][] bioForms = { { "beratBadan", "tinggiBadan", "hp", "operatorSeluler", "kendaraanKuliah", "golonganDarah" },
				{ "noIdentitas", "alamat;text", "rt", "rw", "kodepos", "dusun", "kelurahan", "kecamatan" },
				{ "namaAyah", "tanggalLahirAyah", "nikAyah", "telpAyah", "pekerjaanAyah", "pendidikanAyah",
				"jenisPenghasilanAyah", "namaIbu", "tanggalLahirIbu", "nikIbu", "telpIbu", "pekerjaanIbu",
				"pendidikanIbu", "jenisPenghasilanIbu", "namaWali", "tanggalLahirWali", "telpWali", "pekerjaanWali",
				"pendidikanWali", "jenisPenghasilanWali" },
				{ "asalSma", "alamatAsalSma;text", "asalSmp", "alamatAsalSmp;text", "asalSd", "alamatAsalSd;text" } };

		String[][] bioLabels = {
				{ "Berat Badan", "Tinggi Badan", "Nomor HP", "Operator Seluler", "Kendaraan Kuliah", "Golongan Darah" },
				{ "NIK", "Alamat", "RT", "RW", "Kode POS", "Dusun/Kampung'", "Kelurahan", "Kecamatan" },
				{ "Nama Ayah", "Tanggal Lahir Ayah", "NIK Ayah", "HP Ayah", "Pekerjaan Ayah", "Pendidikan Ayah",
				"Penghasilan Ayah", "Nama Ibu", "Tanggal Lahir Ibu", "NIK Ibu", "HP Ibu", "Pekerjaan Ibu",
				"Pendidikan Ibu", "Penghasilan Ibu", "Nama Wali", "Tanggal Lahir Wali", "Telp Wali", "Pekerjaan Wali",
				"Pendidikan Wali", "Jenis Penghasilan Wali" },
				{ "Asal SMA", "Alamat Asal SMA", "Asal SMP", "Alamat Asal SMP", "Asal SD", "Alamat Asal SD" } };

		configBio.put("formCols", new JSONArray(Arrays.asList(bioForms)));
		configBio.put("formLabels", new JSONArray(Arrays.asList(bioLabels)));

		wizardConfigs.put(configBio);

		try {
			// --- EKSEKUSI ---
			// Panggil fungsi generator dengan parameter JSONArray
			String htmlResult = DynamicTableMultiTabJsonGenerator.generateWizardTable(wizardConfigs);

			//System.out.println("htmlResult -> \n\n"+htmlResult);

			out.println(htmlResult);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pagesmastermahasiswazul/old_files/index_1.jsp:179");
		}
		%>


