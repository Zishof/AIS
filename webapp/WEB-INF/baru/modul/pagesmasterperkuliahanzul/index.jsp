<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.DynamicJspCrudGenerator"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // === TAB 1: CRUD Jadwal Perkuliahan (semua field dari ZUL asli) ===
    String[] cols   = { "matakuliah", "dosen1", "kelas", "semester", "tahunAjaran", "hari", "ruang", "waktuMulai", "waktuSelesai" };
    String[] lbls   = { "Matakuliah", "Dosen", "Kelas", "Semester", "Tahun Akademik", "Hari", "Ruang", "Mulai", "Sampai" };
    String[] search = { "matakuliah", "dosen1", "kelas", "semester", "tahunAjaran", "hari", "ruang", "jurusan" };
    String[] forms  = {
        "matakuliah", "dosen1", "dosen2", "dosen3", "dosen4", "dosen5",
        "dosen6", "dosen7", "dosen8", "dosen9", "dosen10",
        "kelas", "ruang", "hari", "waktuMulai", "waktuSelesai", "waktuMulaiD", "waktuSelesaiD",
        "semester", "ganjilGenap", "tahunAjaran", "masaPerkuliahan",
        "jurusan", "program", "kapasitasKelas", "jumlahMahasiswa",
        "kurikulum", "kurikulumPunyaMatakuliah", "pembombotanNilai", "pembombotanNilaiBackup",
        "jenisEvaluasi", "jenis", "mode", "lingkup", "waktu",
        "statusSemesterPendek", "merupakan_paralel", "perkuliahan_paralel",
        "merupakanRemedial", "merupakanPraPerkuliahan", "merupakanPerkuliahanUmum",
        "merupakanTeamTeaching", "terdapatKegiatanPraktek",
        "planning_jumlah_tatap_muka", "jumlahRencanaPertemuanMengikutiKurikulum",
        "jumlahMaksimalPertemuan", "minggu1", "minggu2", "minggu3", "minggu4", "minggu5",
        "tanggalMulaiPerkuliahan", "bolehMenentukanTanggalMulaiPerkuliahan",
        "perkuliahanDimulai", "perkuliahanSampai",
        "lewatiTanggalMerahNasional", "dosenBisaMerubahTanggalPerkuliahan",
        "mahasiswaBolehAbsenMenggunakanFoto", "dosenBolehAbsenMenggunakanFoto",
        "bolehAbsenWaktuIkutiPerkuliahan",
        "bolehAbsenSebelumWaktuMulaiDalamMenit", "bolehAbsenSetelahWaktuMulaiDalamMenit",
        "kehadiranDosenHarusDiinputSesuaiJadwal", "kehadiranDosenHarusDiinputDiIpYangDitentukan",
        "kehadiranMahasiswaHarusDiinputSesuaiJadwal", "kehadiranMahasiswaHarusDiinputDiIpYangDitentukan",
        "adminBolehMenginputKehadiranDiluarJadwalDanIp",
        "persenKehadiranDinilai0", "batasWaktuBolehAbsenKehadiran",
        "dosenBolehVerifikasiNilaiSendiri", "waktuPerkuliahanOnlineBebas",
        "sembunyikanNilaiJikaBelumDiverifikasi", "hanyaInputNilaiHuruf",
        "sembunyikanFormatPenilaian", "ambilMkDiluarSemesterKurikulum",
        "nilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir", "jikaAdaNilai0TidakMenghitungNilaiAkhir",
        "semuaPertemuanSesuaiRps", "semuaNilaiSesuaiRps", "catatanSesuaiRps",
        "tampilkanSaatPengambilanKrs", "abaikanWaktuBentrokDenganJadwalLain",
        "feeder", "feeder1", "feeder2", "feeder3", "feeder4", "feeder5",
        "feeder6", "feeder7", "feeder8", "feeder9", "feeder10",
        "capaianPembelajaranProdi", "deskripsiPembelajaran", "pendahuluan",
        "keterangan", "keteranganJadwal", "aktif"
    };
    Set required = new HashSet();
    required.add("matakuliah");
    required.add("semester");
    required.add("kelas");
    String crudTab1 = DynamicTableGenerator.generateTable(cols, lbls, search, forms,
        Common.getGeneratedBarCode(5), "Jadwal Perkuliahan", Perkuliahan.class.getName(), required, new HashSet());

    // === TAB 2: Laporan & Rekap (17 sub-tab laporan dari ZUL asli) ===
    String p  = "?p=";
    String si = "&s=index";
    JSONArray linkLaporan = new JSONArray();
    JSONObject l;
    // Laporan inline di-sub-page perkuliahan (parameter dipakai bersama via LaporanPerkuliahanJspHelper)
    String lp = "?p=pagesmasterperkuliahanzul";
    l = new JSONObject(); l.put("title","Jadwal Per Hari");     l.put("url", lp+"&s=laporan_jadwal_perhari"); l.put("icon","fas fa-calendar-day");       l.put("description","Jadwal perkuliahan dikelompok per hari");         linkLaporan.put(l);
    l = new JSONObject(); l.put("title","Jadwal Per Dosen");    l.put("url", lp+"&s=laporan_per_dosen");     l.put("icon","fas fa-chalkboard-teacher");  l.put("description","Rekap jadwal mengajar per dosen");                linkLaporan.put(l);
    l = new JSONObject(); l.put("title","Rekap Semua Jadwal");  l.put("url", lp+"&s=laporan_rekap");         l.put("icon","fas fa-table");               l.put("description","Daftar lengkap seluruh kelas perkuliahan aktif"); linkLaporan.put(l);
    l = new JSONObject(); l.put("title","Daftar Hadir Dosen");  l.put("url", p+"pagesmasterabsensikehadirandosenharianzul"+si); l.put("icon","fas fa-clipboard-check"); l.put("description","Rekap kehadiran dosen per perkuliahan");   linkLaporan.put(l);
    l = new JSONObject(); l.put("title","Per Kelas");           l.put("url", lp+"&s=laporan_per_kelas");          l.put("icon","fas fa-door-open");       l.put("description","Jadwal perkuliahan per kelas/semester");        linkLaporan.put(l);
    l = new JSONObject(); l.put("title","Per Ruangan");         l.put("url", lp+"&s=laporan_per_ruangan");        l.put("icon","fas fa-building");         l.put("description","Pemakaian ruangan per slot waktu");             linkLaporan.put(l);
    l = new JSONObject(); l.put("title","Per Matakuliah");      l.put("url", lp+"&s=laporan_per_matakuliah");     l.put("icon","fas fa-book-open");        l.put("description","Jadwal per matakuliah lintas kelas");           linkLaporan.put(l);
    l = new JSONObject(); l.put("title","Per Asisten");         l.put("url", lp+"&s=laporan_per_asisten");        l.put("icon","fas fa-user-graduate");    l.put("description","Jadwal asisten dosen mengajar");                linkLaporan.put(l);
    l = new JSONObject(); l.put("title","Jml SKS Dosen");       l.put("url", lp+"&s=laporan_jml_sks_dosen");      l.put("icon","fas fa-weight");           l.put("description","Total SKS yang diampu tiap dosen");             linkLaporan.put(l);
    l = new JSONObject(); l.put("title","Jml SKS Matakuliah");  l.put("url", lp+"&s=laporan_jml_sks_matakuliah"); l.put("icon","fas fa-list-ol");          l.put("description","Jumlah SKS per matakuliah");                    linkLaporan.put(l);
    l = new JSONObject(); l.put("title","Rekap Dosen");         l.put("url", lp+"&s=laporan_rekap_dosen");        l.put("icon","fas fa-chart-bar");        l.put("description","Rekap total beban mengajar dosen");             linkLaporan.put(l);
    l = new JSONObject(); l.put("title","Dosen dan Mahasiswa"); l.put("url", p+"pagesmastermahasiswaikutiperkuliahanzul"+si);  l.put("icon","fas fa-users");            l.put("description","Laporan dosen pembina dan mahasiswanya");        linkLaporan.put(l);
    l = new JSONObject(); l.put("title","Dosen Pembina MK");    l.put("url", lp+"&s=laporan_dosen_pembina_mk");   l.put("icon","fas fa-user-tie");         l.put("description","Data dosen pembina per matakuliah");             linkLaporan.put(l);
    l = new JSONObject(); l.put("title","Jadwal Paralel");      l.put("url", lp+"&s=laporan_jadwal_paralel");     l.put("icon","fas fa-columns");          l.put("description","Perkuliahan paralel lintas kelas");              linkLaporan.put(l);
    l = new JSONObject(); l.put("title","Pengumuman");          l.put("url", p+"pagesmasterpengumumanperkuliahanzul"+si);      l.put("icon","fas fa-bullhorn");         l.put("description","Pengumuman terkait jadwal perkuliahan");         linkLaporan.put(l);

    // === TAB 3: SP ===
    JSONArray linkSP = new JSONArray();
    l = new JSONObject(); l.put("title","Jadwal SP"); l.put("url", p+"pagesmasterperkuliahanspzul"+si); l.put("icon","fas fa-calendar-plus"); l.put("description","Semester Pendek — tambah dan kelola jadwal"); linkSP.put(l);
    l = new JSONObject(); l.put("title","Penilaian SP"); l.put("url", p+"pagesmasterpenilaianspzul"+si); l.put("icon","fas fa-star-half-alt"); l.put("description","Input dan rekap nilai Semester Pendek"); linkSP.put(l);
    l = new JSONObject(); l.put("title","KRS SP"); l.put("url", p+"pagesmasterkrsspzul"+si); l.put("icon","fas fa-file-alt"); l.put("description","KRS mahasiswa Semester Pendek"); linkSP.put(l);
    l = new JSONObject(); l.put("title","Ujian SP"); l.put("url", p+"pagesmasterujianmahasiswaspzul"+si); l.put("icon","fas fa-pencil-alt"); l.put("description","Jadwal dan data ujian SP"); linkSP.put(l);
    l = new JSONObject(); l.put("title","Absensi SP"); l.put("url", p+"pagesmasterabsensispzul"+si); l.put("icon","fas fa-clipboard"); l.put("description","Kehadiran mahasiswa Semester Pendek"); linkSP.put(l);
    l = new JSONObject(); l.put("title","Kalender SP"); l.put("url", p+"pagesmasterkalendermahasiswaspzul"+si); l.put("icon","fas fa-calendar"); l.put("description","Kalender akademik Semester Pendek"); linkSP.put(l);

    // === TAB 4: Remedial ===
    JSONArray linkRemedial = new JSONArray();
    l = new JSONObject(); l.put("title","Jadwal Remedial"); l.put("url", p+"pagesmasterperkuliahanremedialzul"+si); l.put("icon","fas fa-redo"); l.put("description","Perkuliahan remedial — kelola jadwal"); linkRemedial.put(l);
    l = new JSONObject(); l.put("title","Penilaian Remedial"); l.put("url", p+"pagesmasterpenilaianremedialzul"+si); l.put("icon","fas fa-check-double"); l.put("description","Input nilai remedial mahasiswa"); linkRemedial.put(l);
    l = new JSONObject(); l.put("title","Pertemuan Remedial"); l.put("url", p+"pagesmasterpertemuanremedialzul"+si); l.put("icon","fas fa-chalkboard"); l.put("description","Daftar pertemuan kelas remedial"); linkRemedial.put(l);
    l = new JSONObject(); l.put("title","Absensi Remedial"); l.put("url", p+"pagesmasterabsensiremediallzul"+si); l.put("icon","fas fa-clipboard-list"); l.put("description","Kehadiran mahasiswa remedial"); linkRemedial.put(l);

    // === TAB 5: Ekstrakurikuler ===
    JSONArray linkEkstra = new JSONArray();
    l = new JSONObject(); l.put("title","Jadwal Ekstra"); l.put("url", p+"pagesmasterperkuliahanekstrakurikulerzul"+si); l.put("icon","fas fa-running"); l.put("description","Perkuliahan / kegiatan ekstrakurikuler"); linkEkstra.put(l);
    l = new JSONObject(); l.put("title","Absensi Ekstra"); l.put("url", p+"pagesmasterabsensiekstrakurikulerzul"+si); l.put("icon","fas fa-clipboard"); l.put("description","Kehadiran peserta ekstrakurikuler"); linkEkstra.put(l);
    l = new JSONObject(); l.put("title","Penilaian Ekstra"); l.put("url", p+"pagesmasterpenilaianekstrakurikulerzul"+si); l.put("icon","fas fa-award"); l.put("description","Nilai kegiatan ekstrakurikuler"); linkEkstra.put(l);
    l = new JSONObject(); l.put("title","Pertemuan Ekstra"); l.put("url", p+"pagesmasterpertemuanekstrakulikulerzul"+si); l.put("icon","fas fa-chalkboard-teacher"); l.put("description","Pertemuan kegiatan ekstrakurikuler"); linkEkstra.put(l);

    // === TAB 6: Pra Perkuliahan ===
    JSONArray linkPra = new JSONArray();
    l = new JSONObject(); l.put("title","Jadwal Pra-Perkuliahan"); l.put("url", p+"pagesmasterperkuliahanpraperkuliahanzul"+si); l.put("icon","fas fa-hourglass-start"); l.put("description","Perkuliahan pra-semester / orientasi"); linkPra.put(l);
    l = new JSONObject(); l.put("title","Absensi Pra-Perkuliahan"); l.put("url", p+"pagesmasterabsensipraperkuliahanzul"+si); l.put("icon","fas fa-clipboard-list"); l.put("description","Kehadiran saat pra-perkuliahan"); linkPra.put(l);
    l = new JSONObject(); l.put("title","Penilaian Pra-Perkuliahan"); l.put("url", p+"pagesmasterpenilaianpraperkuliahanzul"+si); l.put("icon","fas fa-star"); l.put("description","Nilai kegiatan pra-perkuliahan"); linkPra.put(l);
    l = new JSONObject(); l.put("title","Pertemuan Pra-Perkuliahan"); l.put("url", p+"pagesmasterpertemuanpraperkuliahanzul"+si); l.put("icon","fas fa-chalkboard"); l.put("description","Daftar pertemuan pra-perkuliahan"); linkPra.put(l);

    // === TAB 7: Perkuliahan Umum ===
    JSONArray linkUmum = new JSONArray();
    l = new JSONObject(); l.put("title","Jadwal Umum"); l.put("url", p+"pagesmasterperkuliahanperkuliahanumumzul"+si); l.put("icon","fas fa-globe"); l.put("description","Perkuliahan umum lintas prodi/jurusan"); linkUmum.put(l);

    // === TAB 8: Masa & Jam ===
    JSONArray linkMasa = new JSONArray();
    l = new JSONObject(); l.put("title","Masa Perkuliahan"); l.put("url", p+"pagesmastermasaperkuliahanzul"+si); l.put("icon","fas fa-calendar-alt"); l.put("description","Konfigurasi masa/periode perkuliahan aktif"); linkMasa.put(l);
    l = new JSONObject(); l.put("title","Jam Perkuliahan"); l.put("url", p+"pagesmasterjamperkuliahanzul"+si); l.put("icon","fas fa-clock"); l.put("description","Slot jam perkuliahan yang tersedia"); linkMasa.put(l);
    l = new JSONObject(); l.put("title","Template Perkuliahan"); l.put("url", p+"pagesmastertemplateperkuliahanzul"+si); l.put("icon","fas fa-copy"); l.put("description","Template jadwal untuk copy antar semester"); linkMasa.put(l);
    l = new JSONObject(); l.put("title","Statistik Perkuliahan"); l.put("url", p+"pagesmasterinformasijadwalperkuliahanzul"+si); l.put("icon","fas fa-chart-pie"); l.put("description","Statistik dan rekap jadwal perkuliahan"); linkMasa.put(l);

    // === Bangun tab panel ===
    JSONArray tabDefs = new JSONArray();

    JSONObject tab1 = new JSONObject(); tab1.put("tabTitle","Jadwal Perkuliahan"); tab1.put("tabIcon","fas fa-chalkboard"); tab1.put("type","html"); tab1.put("html",crudTab1); tabDefs.put(tab1);
    JSONObject tab2 = new JSONObject(); tab2.put("tabTitle","Laporan & Rekap"); tab2.put("tabIcon","fas fa-chart-bar"); tab2.put("type","links"); tab2.put("panelTitle","Laporan & Rekap Jadwal Perkuliahan"); tab2.put("links",linkLaporan); tabDefs.put(tab2);
    JSONObject tab3 = new JSONObject(); tab3.put("tabTitle","SP"); tab3.put("tabIcon","fas fa-calendar-plus"); tab3.put("type","links"); tab3.put("panelTitle","Semester Pendek (SP)"); tab3.put("links",linkSP); tabDefs.put(tab3);
    JSONObject tab4 = new JSONObject(); tab4.put("tabTitle","Remedial"); tab4.put("tabIcon","fas fa-redo"); tab4.put("type","links"); tab4.put("panelTitle","Perkuliahan Remedial"); tab4.put("links",linkRemedial); tabDefs.put(tab4);
    JSONObject tab5 = new JSONObject(); tab5.put("tabTitle","Ekstrakurikuler"); tab5.put("tabIcon","fas fa-running"); tab5.put("type","links"); tab5.put("panelTitle","Perkuliahan Ekstrakurikuler"); tab5.put("links",linkEkstra); tabDefs.put(tab5);
    JSONObject tab6 = new JSONObject(); tab6.put("tabTitle","Pra Perkuliahan"); tab6.put("tabIcon","fas fa-hourglass-start"); tab6.put("type","links"); tab6.put("panelTitle","Pra Perkuliahan / Orientasi"); tab6.put("links",linkPra); tabDefs.put(tab6);
    JSONObject tab7 = new JSONObject(); tab7.put("tabTitle","Umum"); tab7.put("tabIcon","fas fa-globe"); tab7.put("type","links"); tab7.put("panelTitle","Perkuliahan Umum Lintas Prodi"); tab7.put("links",linkUmum); tabDefs.put(tab7);
    JSONObject tab8 = new JSONObject(); tab8.put("tabTitle","Masa & Jam"); tab8.put("tabIcon","fas fa-clock"); tab8.put("type","links"); tab8.put("panelTitle","Masa Perkuliahan & Konfigurasi Jam"); tab8.put("links",linkMasa); tabDefs.put(tab8);

    out.println(DynamicJspCrudGenerator.generateTabPanel(tabDefs));
%>
