<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.DynamicJspCrudGenerator"%>
<%@page import="ais.database.model.Dosen"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // === TAB 1: CRUD Data Dosen ===
    String[] cols   = { "code", "nidn", "nama", "jurusan", "aktif" };
    String[] lbls   = { "Kode", "NIDN", "Nama", "Jurusan", "Aktif" };
    String[] search = { "code", "nama", "nidn" };
    String[] forms  = { "code", "mycode", "nidn", "niyNigk", "nuptk", "gelarDepan", "nomorSertifikasi",
        "gelarBelakang", "lockId", "aktif", "skCpns", "tglSkCpns", "tmtPns", "skAngkat", "tmtSkAngkat",
        "aLisensiKepsek", "jmlSekolahBinaan", "aDiklatAwas", "aktaIjinAjar", "nira", "aBraille",
        "aBhsIsyarat", "npwp", "ktp", "nama", "alamat", "email", "telp", "hp", "kelamin",
        "tempatlahir", "pangkat", "golongan", "golonganPegawai", "golonganPns", "pendidikan",
        "jabatan", "spesialisasi1", "spesialisasi2", "spesialisasi3", "tanggallahir", "milikUniversitas" };

    Set required = new HashSet();
    required.add("nama");
    Set unique = new HashSet();

    String mainCrud = DynamicTableGenerator.generateTable(cols, lbls, search, forms,
        Common.getGeneratedBarCode(5), "Dosen", Dosen.class.getName(), required, unique);

    String p  = "?p=";
    String si = "&s=index";

    // === TAB 2: Akademik & Jadwal ===
    JSONArray linkAkademik = new JSONArray();
    JSONObject l;
    l = new JSONObject(); l.put("title","Jadwal Ajar Dosen"); l.put("url", p+"pagesmasterinformasijadwalajardosenzul"+si); l.put("icon","fas fa-calendar-check"); l.put("description","Informasi jadwal mengajar dosen"); linkAkademik.put(l);
    l = new JSONObject(); l.put("title","Kalender Perkuliahan"); l.put("url", p+"pagesmasterkalenderperkuliahanpenjadwalandosenzul"+si); l.put("icon","fas fa-calendar-alt"); l.put("description","Kalender penjadwalan perkuliahan"); linkAkademik.put(l);
    l = new JSONObject(); l.put("title","Dosen Pembimbing PA"); l.put("url", p+"pagesmasterdosenpembimbingakademikzul"+si); l.put("icon","fas fa-user-tie"); l.put("description","Penunjukan dosen penasihat akademik"); linkAkademik.put(l);
    l = new JSONObject(); l.put("title","Absensi Dosen Harian"); l.put("url", p+"pagesmasterabsensikehadirandosenharianzul"+si); l.put("icon","fas fa-clipboard-check"); l.put("description","Kehadiran harian dosen"); linkAkademik.put(l);
    l = new JSONObject(); l.put("title","Checklist Penilaian Dosen"); l.put("url", p+"pagesmasterchecklistpenilaiandosenzul"+si); l.put("icon","fas fa-tasks"); l.put("description","Checklist evaluasi kinerja dosen"); linkAkademik.put(l);
    l = new JSONObject(); l.put("title","Checklist oleh Mahasiswa"); l.put("url", p+"pagesmasterchecklistpenilaiandosenolehmhszul"+si); l.put("icon","fas fa-star"); l.put("description","Penilaian dosen dari mahasiswa"); linkAkademik.put(l);
    l = new JSONObject(); l.put("title","Grup Checklist Penilaian"); l.put("url", p+"pagesmastergrupchecklistpenilaiandosenzul"+si); l.put("icon","fas fa-layer-group"); l.put("description","Pengelompokan checklist penilaian"); linkAkademik.put(l);
    l = new JSONObject(); l.put("title","Angket Penilaian Dosen"); l.put("url", p+"pagesmasterangketpenilaiandosenzul"+si); l.put("icon","fas fa-poll"); l.put("description","Survey evaluasi kinerja dosen"); linkAkademik.put(l);
    l = new JSONObject(); l.put("title","Reset Password"); l.put("url", p+"pagesmasterresetpassworddosenmahasiswazul"+si); l.put("icon","fas fa-key"); l.put("description","Reset password akun dosen"); linkAkademik.put(l);

    // === TAB 3: BKD & Kinerja ===
    JSONArray linkBkd = new JSONArray();
    l = new JSONObject(); l.put("title","Kewajiban Beban Dosen (BKD)"); l.put("url", p+"pagesmasterbkdkewajibanbebandosenzul"+si); l.put("icon","fas fa-balance-scale"); l.put("description","Pengaturan BKD per dosen"); linkBkd.put(l);
    l = new JSONObject(); l.put("title","Status Kewajiban BKD"); l.put("url", p+"pagesmasterbkdstatuskewajibanbebandosenzul"+si); l.put("icon","fas fa-chart-bar"); l.put("description","Pantau status pemenuhan BKD"); linkBkd.put(l);
    l = new JSONObject(); l.put("title","Penunjang Kinerja Dosen"); l.put("url", p+"pagesmasterbkdasesorpenunjangkinerjadosenzul"+si); l.put("icon","fas fa-award"); l.put("description","Asesor & penunjang kinerja"); linkBkd.put(l);

    // === TAB 4: Feeder & EPSBED ===
    JSONArray linkFeeder = new JSONArray();
    l = new JSONObject(); l.put("title","Master Dosen (EPSBED)"); l.put("url", p+"pagesmasterepsbedmasterdosenzul"+si); l.put("icon","fas fa-database"); l.put("description","Data master dosen format EPSBED"); linkFeeder.put(l);
    l = new JSONObject(); l.put("title","Riwayat Pendidikan"); l.put("url", p+"pagesmasterepsbedmasterriwayatpendidikandosenzul"+si); l.put("icon","fas fa-graduation-cap"); l.put("description","Riwayat studi/pendidikan dosen"); linkFeeder.put(l);
    l = new JSONObject(); l.put("title","Transaksi Mengajar"); l.put("url", p+"pagesmasterepsbedtransaksimengajardosenzul"+si); l.put("icon","fas fa-chalkboard"); l.put("description","Log transaksi aktivitas mengajar"); linkFeeder.put(l);
    l = new JSONObject(); l.put("title","Publikasi Dosen"); l.put("url", p+"pagesmasterepsbedtransaksipublikasidosenzul"+si); l.put("icon","fas fa-scroll"); l.put("description","Data publikasi ilmiah dosen"); linkFeeder.put(l);
    l = new JSONObject(); l.put("title","Status Dosen (EPSBED)"); l.put("url", p+"pagesmasterepsbedtransaksistatusdosenzul"+si); l.put("icon","fas fa-user-check"); l.put("description","Status aktif dosen di EPSBED"); linkFeeder.put(l);

    // === Bangun tab panel ===
    JSONArray tabDefs = new JSONArray();

    JSONObject tab1 = new JSONObject();
    tab1.put("tabTitle", "Data Dosen");
    tab1.put("tabIcon", "fas fa-chalkboard-teacher");
    tab1.put("type", "html");
    tab1.put("html", mainCrud);
    tabDefs.put(tab1);

    JSONObject tab2 = new JSONObject();
    tab2.put("tabTitle", "Akademik & Jadwal");
    tab2.put("tabIcon", "fas fa-calendar-check");
    tab2.put("type", "links");
    tab2.put("panelTitle", "Kegiatan Akademik & Jadwal Dosen");
    tab2.put("links", linkAkademik);
    tabDefs.put(tab2);

    JSONObject tab3 = new JSONObject();
    tab3.put("tabTitle", "BKD & Kinerja");
    tab3.put("tabIcon", "fas fa-balance-scale");
    tab3.put("type", "links");
    tab3.put("panelTitle", "Beban Kerja Dosen & Penilaian Kinerja");
    tab3.put("links", linkBkd);
    tabDefs.put(tab3);

    JSONObject tab4 = new JSONObject();
    tab4.put("tabTitle", "Feeder & EPSBED");
    tab4.put("tabIcon", "fas fa-database");
    tab4.put("type", "links");
    tab4.put("panelTitle", "Sinkronisasi Feeder & Data EPSBED");
    tab4.put("links", linkFeeder);
    tabDefs.put(tab4);

    out.println(DynamicJspCrudGenerator.generateTabPanel(tabDefs));
%>
