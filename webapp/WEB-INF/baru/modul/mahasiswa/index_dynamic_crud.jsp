<%@page import="java.util.Arrays"%>
<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.DynamicJspCrudGenerator"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.BiodataMahasiswa"%>
<%@page import="ais.database.model.file.FotoMahasiswa"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String fileData = "file_foto_mahasiswa;{\"jenis\":\"" + FotoMahasiswa.DEFAULT_JENIS + "\",\"clazz\":\""
            + FotoMahasiswa.class.getName() + "\",\"id\":\"{id_ref}\"}";

    JSONObject belum = new JSONObject();
    belum.put("id", "");
    belum.put("nama", "Belum Ditentukan");
    JSONObject lk = new JSONObject();
    lk.put("id", "Laki-laki");
    lk.put("nama", "Laki-laki");
    JSONObject pr = new JSONObject();
    pr.put("id", "Perempuan");
    pr.put("nama", "Perempuan");
    JSONArray kelaminOptions = new JSONArray();
    kelaminOptions.put(belum);
    kelaminOptions.put(lk);
    kelaminOptions.put(pr);

    // === TAB 1: CRUD wizard Identitas + Biodata ===
    JSONArray wizardConfigs = new JSONArray();
    String randomID = Common.getGeneratedBarCode(6);

    String[] search = { "nim", "nama", "tahunangkatan", "program", "jurusan", "kelamin;" + kelaminOptions.toString(), "aktif" };
    String[] tableColumns = { "nim", "nama", "jurusan", "kelas", "program", "jenisSeleksi", "tahunangkatan", "aktif" };
    String[] tableLabels  = { "NIM", "Nama", "Prodi", "Kelas", "Program", "Jalur Masuk", "Tahun Masuk", "Status" };

    String[][] forms = {
        { fileData, "nim", "nama", "tempatlahir", "tanggallahir", "tahunangkatan",
          "kelamin;" + kelaminOptions.toString(), "jurusan", "konsentrasi", "semesterMulai",
          "program", "dosen", "kelas", "statusAwalMahasiswa", "jenisPembiayaanMahasiswa",
          "jenisSeleksi", "waktuKuliah" },
        { "berat_badan", "tinggi_badan", "golongan_darah", "agama", "warganegara", "negara", "ktp" },
        { "tanggalLulus", "tanggalYudisium", "statusKeluar", "predikatKelulusan",
          "tahunLulus", "semesterLulus", "judulSkripsi", "judulSkripsiEn",
          "nomorSkpi", "tanggalSkRektor", "noAkta2", "skDo" },
        { "merupakanPindahan", "pindahanDariKampus", "namaProdiPindah", "nimPindahan",
          "pindahDariKampusLamaDiSemester", "pindahKeKampusIniMasukSemester", "sksYangDiakui", "keteranganPindah",
          "merupakanAlihProdi", "nimLamaSebelumPindah", "sksYangDiakuiPindahProdi",
          "pindahKeProdiIniMasukSemester", "tanggalPindahProdi", "keteranganPindahProdi" },
        { "feeder", "idRegPd", "idfinger", "aktif", "tidakAdaTagihan" },
        { "keterangan;editor" } };

    String[][] labels = {
        { "Foto Resmi", "NIM", "Nama", "Tempat Lahir", "Tanggal Lahir", "Tahun Masuk",
          "Jenis Kelamin", "Jurusan/Prodi", "Konsentrasi", "Semester Mulai",
          "Program", "Dosen PA", "Kelas", "Status Awal", "Jenis Pembiayaan",
          "Jalur Masuk", "Waktu Kuliah" },
        { "Berat Badan (kg)", "Tinggi Badan (cm)", "Golongan Darah", "Agama",
          "Warganegara", "Negara Asal", "Nomor KTP" },
        { "Tanggal Lulus/Keluar", "Tanggal Yudisium", "Status Keluar", "Predikat Kelulusan",
          "Tahun Lulus", "Semester Lulus", "Judul Skripsi", "Judul Skripsi (Inggris)",
          "Nomor SKPI", "Tgl SK Rektor", "Nomor Ijazah", "Nomor SK DO" },
        { "Merupakan Pindahan?", "Kampus Asal Pindah", "Nama Prodi Asal", "NIM Lama",
          "Semester Keluar dari Kampus Asal", "Masuk ke Kampus Ini Semester", "SKS yang Diakui", "Keterangan Pindah",
          "Merupakan Alih Prodi?", "NIM Lama (Alih Prodi)", "SKS Diakui (Alih Prodi)",
          "Masuk ke Prodi Ini Semester", "Tanggal Alih Prodi", "Keterangan Alih Prodi" },
        { "Kode Feeder", "ID Reg Pd", "Kode Finger/RFID", "Status Aktif", "Tidak Ada Tagihan" },
        { "Catatan" } };

    Set required = new HashSet();
    required.add("nim");
    required.add("nama");
    Set unique = new HashSet();
    unique.add("nim");

    String[] stepTitles = { "Identitas Utama", "Data Fisik", "Kelulusan", "Pindahan & Alih Prodi", "Lain-Lain", "Catatan" };

    JSONObject master = DynamicJspCrudGenerator.newConfig(Mahasiswa.class, "Mahasiswa", randomID);
    DynamicJspCrudGenerator.putSort(master, 1, "tahunangkatan", "desc");
    DynamicJspCrudGenerator.putSort(master, 2, "nim", "asc");
    DynamicJspCrudGenerator.putArray(master, "searchCols", search);
    DynamicJspCrudGenerator.putArray(master, "tableColumns", tableColumns);
    DynamicJspCrudGenerator.putArray(master, "tableLabels", tableLabels);
    DynamicJspCrudGenerator.putArray(master, "stepTitles", stepTitles);
    DynamicJspCrudGenerator.putMatrix(master, "formCols", forms);
    DynamicJspCrudGenerator.putMatrix(master, "formLabels", labels);
    DynamicJspCrudGenerator.putSet(master, "paramRequired", required);
    DynamicJspCrudGenerator.putSet(master, "paramTidakBolehSama", unique);
    wizardConfigs.put(master);

    JSONObject bio = DynamicJspCrudGenerator.newConfig(BiodataMahasiswa.class, "Biodata", randomID);
    DynamicJspCrudGenerator.putRelationToMaster(bio, "mahasiswa");
    DynamicJspCrudGenerator.putArray(bio, "stepTitles", new String[] { "Biodata", "Alamat", "Keluarga", "Asal Sekolah" });
    DynamicJspCrudGenerator.putMatrix(bio, "formCols", new String[][] {
        { "beratBadan", "tinggiBadan", "hp", "operatorSeluler", "kendaraanKuliah", "golonganDarah" },
        { "noIdentitas", "alamat;text", "rt", "rw", "kodepos", "dusun", "kelurahan", "kecamatan" },
        { "namaAyah", "tanggalLahirAyah", "nikAyah", "telpAyah", "pekerjaanAyah", "pendidikanAyah",
          "namaIbu", "tanggalLahirIbu", "nikIbu", "telpIbu", "pekerjaanIbu", "pendidikanIbu" },
        { "asalSma", "alamatAsalSma;text", "asalSmp", "alamatAsalSmp;text", "asalSd", "alamatAsalSd;text" } });
    DynamicJspCrudGenerator.putMatrix(bio, "formLabels", new String[][] {
        { "Berat Badan", "Tinggi Badan", "Nomor HP", "Operator Seluler", "Kendaraan Kuliah", "Golongan Darah" },
        { "NIK", "Alamat", "RT", "RW", "Kode POS", "Dusun/Kampung", "Kelurahan", "Kecamatan" },
        { "Nama Ayah", "Tanggal Lahir Ayah", "NIK Ayah", "HP Ayah", "Pekerjaan Ayah", "Pendidikan Ayah",
          "Nama Ibu", "Tanggal Lahir Ibu", "NIK Ibu", "HP Ibu", "Pekerjaan Ibu", "Pendidikan Ibu" },
        { "Asal SMA", "Alamat Asal SMA", "Asal SMP", "Alamat Asal SMP", "Asal SD", "Alamat Asal SD" } });
    wizardConfigs.put(bio);

    // === TAB 2: Data Akademik — navigasi ke sub-modul ===
    JSONArray linkAkademik = new JSONArray();
    String p = "?p=";
    String si = "&s=index";

    JSONObject l = new JSONObject(); l.put("title","KRS Mahasiswa"); l.put("url", p+"pagesmasterkrszul"+si); l.put("icon","fas fa-book-open"); l.put("description","Kartu Rencana Studi per semester"); linkAkademik.put(l);
    l = new JSONObject(); l.put("title","KRS Paket"); l.put("url", p+"pagesmasterkrspaketzul"+si); l.put("icon","fas fa-cubes"); l.put("description","KRS paket/bundled per prodi"); linkAkademik.put(l);
    l = new JSONObject(); l.put("title","KRS Non-Paket"); l.put("url", p+"pagesmasterkrsnonpaketzul"+si); l.put("icon","fas fa-list-alt"); l.put("description","KRS mandiri pilih mata kuliah"); linkAkademik.put(l);
    l = new JSONObject(); l.put("title","Ikuti Perkuliahan"); l.put("url", p+"pagesmastermahasiswaikutiperkuliahanzul"+si); l.put("icon","fas fa-chalkboard-teacher"); l.put("description","Daftar perkuliahan yg diikuti mahasiswa"); linkAkademik.put(l);
    l = new JSONObject(); l.put("title","Nilai Mahasiswa"); l.put("url", p+"pagesmasternilaimahasiswazul"+si); l.put("icon","fas fa-star"); l.put("description","Rekap nilai per semester/mata kuliah"); linkAkademik.put(l);
    l = new JSONObject(); l.put("title","Absensi Mahasiswa"); l.put("url", p+"pagesmasterabsensimahasiswazul"+si); l.put("icon","fas fa-check-square"); l.put("description","Data kehadiran perkuliahan"); linkAkademik.put(l);
    l = new JSONObject(); l.put("title","Absen Piket"); l.put("url", p+"pagesmasterabsenpiketmahasiswazul"+si); l.put("icon","fas fa-clipboard-list"); l.put("description","Absensi piket harian mahasiswa"); linkAkademik.put(l);
    l = new JSONObject(); l.put("title","Monitor KRS"); l.put("url", p+"pagesmastermonitorkrsmahasiswazul"+si); l.put("icon","fas fa-eye"); l.put("description","Pantau status KRS mahasiswa"); linkAkademik.put(l);
    l = new JSONObject(); l.put("title","Belum Ambil KRS"); l.put("url", p+"pagesmastermonitormonitormahasiswabelumambilkrszul"+si); l.put("icon","fas fa-exclamation-triangle"); l.put("description","Daftar mahasiswa belum KRS"); linkAkademik.put(l);
    l = new JSONObject(); l.put("title","KRS Konversi"); l.put("url", p+"pagesmasterkrskonversizul"+si); l.put("icon","fas fa-exchange-alt"); l.put("description","Konversi mata kuliah/nilai"); linkAkademik.put(l);
    l = new JSONObject(); l.put("title","Kalender Akademik"); l.put("url", p+"pagesmasterkalendermahasiswazul"+si); l.put("icon","fas fa-calendar-alt"); l.put("description","Jadwal akademik mahasiswa"); linkAkademik.put(l);
    l = new JSONObject(); l.put("title","TOEFL/TOAFL"); l.put("url", p+"pagesmasternilaitoefltoaflmahasiswazul"+si); l.put("icon","fas fa-language"); l.put("description","Nilai bahasa mahasiswa"); linkAkademik.put(l);

    // === TAB 3: Administrasi ===
    JSONArray linkAdm = new JSONArray();
    l = new JSONObject(); l.put("title","Beasiswa"); l.put("url", p+"pagesmasterbeasiswazul"+si); l.put("icon","fas fa-graduation-cap"); l.put("description","Program beasiswa mahasiswa"); linkAdm.put(l);
    l = new JSONObject(); l.put("title","Seleksi Penerima Beasiswa"); l.put("url", p+"pagesmasterbeasiswaseleksipenerimabeasiswazul"+si); l.put("icon","fas fa-user-check"); l.put("description","Seleksi & penetapan penerima"); linkAdm.put(l);
    l = new JSONObject(); l.put("title","Persyaratan Beasiswa"); l.put("url", p+"pagesmasterbeasiswapersyaratanbeasiswazul"+si); l.put("icon","fas fa-file-alt"); l.put("description","Dokumen persyaratan beasiswa"); linkAdm.put(l);
    l = new JSONObject(); l.put("title","Alumni Mahasiswa"); l.put("url", p+"pagesmasteralumnimahasiswazul"+si); l.put("icon","fas fa-user-graduate"); l.put("description","Data alumni"); linkAdm.put(l);
    l = new JSONObject(); l.put("title","Daftar Mahasiswa Lulus"); l.put("url", p+"pagesmasterdaftarmahasiswaluluszul"+si); l.put("icon","fas fa-award"); l.put("description","Rekap kelulusan"); linkAdm.put(l);
    l = new JSONObject(); l.put("title","Pendaftaran Wisuda"); l.put("url", p+"pagesmasterpendaftaranwisudamahasiswazul"+si); l.put("icon","fas fa-hat-wizard"); l.put("description","Pendaftaran upacara wisuda"); linkAdm.put(l);
    l = new JSONObject(); l.put("title","Biodata Mahasiswa"); l.put("url", p+"pagesmasterbiodatamahasiswazul"+si); l.put("icon","fas fa-address-card"); l.put("description","Biodata lengkap & dokumen"); linkAdm.put(l);
    l = new JSONObject(); l.put("title","Catatan Mahasiswa"); l.put("url", p+"pagesmastercatatanmahasiswazul"+si); l.put("icon","fas fa-sticky-note"); l.put("description","Catatan & notifikasi per mahasiswa"); linkAdm.put(l);
    l = new JSONObject(); l.put("title","Daftar Ulang Baru"); l.put("url", p+"pagesmasterdaftarulangmahasiswabaruzul"+si); l.put("icon","fas fa-user-plus"); l.put("description","Daftar ulang mahasiswa baru"); linkAdm.put(l);
    l = new JSONObject(); l.put("title","Daftar Ulang Lama"); l.put("url", p+"pagesmasterdaftarulangmahasiswalamazul"+si); l.put("icon","fas fa-redo"); l.put("description","Daftar ulang mahasiswa lama"); linkAdm.put(l);
    l = new JSONObject(); l.put("title","Generate NIM"); l.put("url", p+"pagesmastergeneratenimakademikzul"+si); l.put("icon","fas fa-id-card"); l.put("description","Generate NIM dari calon mahasiswa"); linkAdm.put(l);
    l = new JSONObject(); l.put("title","Cari Mahasiswa"); l.put("url", p+"pagesmastercarimahasiswazul"+si); l.put("icon","fas fa-search"); l.put("description","Pencarian lanjutan mahasiswa"); linkAdm.put(l);

    // === TAB 4: Keuangan ===
    JSONArray linkKeu = new JSONArray();
    l = new JSONObject(); l.put("title","Informasi Pembayaran"); l.put("url", p+"pagesmasterinformasipembayaranmahasiswazul"+si); l.put("icon","fas fa-money-bill-wave"); l.put("description","Riwayat pembayaran mahasiswa"); linkKeu.put(l);
    l = new JSONObject(); l.put("title","Posting Cicilan"); l.put("url", p+"pagesmasterpostingcicilanmahasiswazul"+si); l.put("icon","fas fa-credit-card"); l.put("description","Posting cicilan pembayaran"); linkKeu.put(l);
    l = new JSONObject(); l.put("title","Bypass Pembayaran"); l.put("url", p+"pagesmasterbaypasspembayaranmahasiswazul"+si); l.put("icon","fas fa-unlock"); l.put("description","Bypass tagihan tertentu"); linkKeu.put(l);
    l = new JSONObject(); l.put("title","Pembatasan IPK KRS"); l.put("url", p+"pagesmasterpembatasanipkkrszul"+si); l.put("icon","fas fa-ban"); l.put("description","Batas pengambilan SKS berdasar IPK"); linkKeu.put(l);
    l = new JSONObject(); l.put("title","Cek Wisuda — Administrasi"); l.put("url", p+"pagesmastercekdaftarwisudaadministrasizul"+si); l.put("icon","fas fa-tasks"); l.put("description","Checklist wisuda administrasi"); linkKeu.put(l);
    l = new JSONObject(); l.put("title","Cek Wisuda — Keuangan"); l.put("url", p+"pagesmastercekdaftarwisudakeuanganzul"+si); l.put("icon","fas fa-coins"); l.put("description","Checklist wisuda keuangan"); linkKeu.put(l);
    l = new JSONObject(); l.put("title","Cek Wisuda — Perpustakaan"); l.put("url", p+"pagesmastercekdaftarwisudaperpustakaanzul"+si); l.put("icon","fas fa-book"); l.put("description","Checklist wisuda perpustakaan"); linkKeu.put(l);
    l = new JSONObject(); l.put("title","Registrasi Wisuda"); l.put("url", p+"pagesmastermahasiswaregistrasiwisudazul"+si); l.put("icon","fas fa-user-tie"); l.put("description","Registrasi resmi wisuda"); linkKeu.put(l);

    // === TAB 5: Penilaian & Angket ===
    JSONArray linkPenilaian = new JSONArray();
    l = new JSONObject(); l.put("title","Angket Penilaian Dosen"); l.put("url", p+"pagesmasterangketpenilaiandosenzul"+si); l.put("icon","fas fa-poll"); l.put("description","Penilaian dosen oleh mahasiswa"); linkPenilaian.put(l);
    l = new JSONObject(); l.put("title","Checklist Penilaian Dosen"); l.put("url", p+"pagesmasterchecklistpenilaiandosenolehmhszul"+si); l.put("icon","fas fa-check-double"); l.put("description","Checklist evaluasi dosen oleh mhs"); linkPenilaian.put(l);
    l = new JSONObject(); l.put("title","KPI Nilai KPI"); l.put("url", p+"pagesmasterkpinilaikpizul"+si); l.put("icon","fas fa-chart-bar"); l.put("description","Key performance indicator"); linkPenilaian.put(l);
    l = new JSONObject(); l.put("title","Informasi Kunjungan"); l.put("url", p+"pagesmasterinformasikunjunganmahasiswazul"+si); l.put("icon","fas fa-map-marker-alt"); l.put("description","Riwayat kunjungan mahasiswa"); linkPenilaian.put(l);
    l = new JSONObject(); l.put("title","Skripsi"); l.put("url", p+"pagesmasterskripsizul"+si); l.put("icon","fas fa-file-signature"); l.put("description","Manajemen skripsi/tugas akhir"); linkPenilaian.put(l);
    l = new JSONObject(); l.put("title","Format Nilai Skripsi"); l.put("url", p+"pagesmasterformatnilaiskripsizul"+si); l.put("icon","fas fa-clipboard"); l.put("description","Template format nilai skripsi"); linkPenilaian.put(l);

    // === Bangun tab panel ===
    JSONArray tabDefs = new JSONArray();

    JSONObject tab1 = new JSONObject();
    tab1.put("tabTitle", "Identitas & Biodata");
    tab1.put("tabIcon", "fas fa-id-badge");
    tab1.put("type", "crud");
    tab1.put("wizardConfigs", wizardConfigs);
    tabDefs.put(tab1);

    JSONObject tab2 = new JSONObject();
    tab2.put("tabTitle", "Data Akademik");
    tab2.put("tabIcon", "fas fa-book-open");
    tab2.put("type", "links");
    tab2.put("panelTitle", "Fitur Akademik Mahasiswa");
    tab2.put("links", linkAkademik);
    tabDefs.put(tab2);

    JSONObject tab3 = new JSONObject();
    tab3.put("tabTitle", "Administrasi");
    tab3.put("tabIcon", "fas fa-folder-open");
    tab3.put("type", "links");
    tab3.put("panelTitle", "Administrasi & Dokumen Mahasiswa");
    tab3.put("links", linkAdm);
    tabDefs.put(tab3);

    JSONObject tab4 = new JSONObject();
    tab4.put("tabTitle", "Keuangan & Wisuda");
    tab4.put("tabIcon", "fas fa-money-check-alt");
    tab4.put("type", "links");
    tab4.put("panelTitle", "Keuangan & Persiapan Wisuda");
    tab4.put("links", linkKeu);
    tabDefs.put(tab4);

    JSONObject tab5 = new JSONObject();
    tab5.put("tabTitle", "Penilaian & Lainnya");
    tab5.put("tabIcon", "fas fa-star-half-alt");
    tab5.put("type", "links");
    tab5.put("panelTitle", "Penilaian, Angket & Aktivitas");
    tab5.put("links", linkPenilaian);
    tabDefs.put(tab5);

    out.println(DynamicJspCrudGenerator.generateTabPanel(tabDefs));
%>
