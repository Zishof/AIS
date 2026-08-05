<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.DynamicTableGenerator"%>
<%@page import="ais.common.DynamicJspCrudGenerator"%>
<%@page import="ais.database.model.Pegawai"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // === TAB 1: CRUD Data Pegawai ===
    String[] cols   = { "code", "nama", "tipePegawai", "statusPegawai", "satuanKerja", "tanggalmasuk", "aktif" };
    String[] lbls   = { "NIP/Kode", "Nama", "Tipe Pegawai", "Status", "Satuan Kerja", "TMT", "Aktif" };
    String[] search = { "code", "nama", "tipePegawai", "statusPegawai", "satuanKerja", "aktif" };
    String[] forms  = { "idfinger", "code", "mycode", "nama", "orangTua", "ktp", "alamat", "email", "telp",
        "telpDarurat", "namaDarurat", "statusDarurat", "kelamin", "tempatlahir", "pangkat", "golongan",
        "jabatan", "spesialisasi1", "spesialisasi2", "spesialisasi3", "tanggallahir", "awalmasuk",
        "tanggalmasuk", "tanggalkeluar", "tanggalmasukHonorer", "tanggalkeluarHonorer",
        "tanggalmasukSemiTetap", "tanggalkeluarSemiTetap", "tanggalMulaiPengalanKerja",
        "tanggalSampaiPengalanKerja", "jamsostek", "bagian", "unitKerja", "ikatanKerjaDosen",
        "tipePegawai", "tipeMasaKerja", "masaKerja", "tetap", "statusPegawai", "satuanKerja",
        "agama", "statusPerkawinan", "alamatJalan", "alamatKelurahan", "sertifikasi",
        "alamatKecamatan", "alamatKabupaten", "alamatPropinsi", "pendidikan",
        "deskripsiPendidikan", "jenisGajiPegawai", "propinsi", "kota",
        "keteranganBadanTinggi", "keteranganBadanBerat", "keteranganBadanRambut",
        "keteranganBadanBentukMuka", "keteranganBadanWarnaKulit", "keteranganBadanCiriKhas",
        "keteranganBadanCacat", "aktif" };

    Set required = new HashSet();
    required.add("code");
    required.add("nama");
    Set unique = new HashSet();
    unique.add("code");

    String mainCrud = DynamicTableGenerator.generateTable(cols, lbls, search, forms,
        Common.getGeneratedBarCode(5), "Pegawai", Pegawai.class.getName(), required, unique);

    String p  = "?p=";
    String si = "&s=index";

    // === TAB 2: Riwayat Kepegawaian ===
    JSONArray linkRiwayat = new JSONArray();
    JSONObject l;
    l = new JSONObject(); l.put("title","Riwayat Pendidikan"); l.put("url", p+"pagesmasteremployriwayatpendidikanpegawaizul"+si); l.put("icon","fas fa-graduation-cap"); l.put("description","Riwayat pendidikan formal pegawai"); linkRiwayat.put(l);
    l = new JSONObject(); l.put("title","Riwayat Kerja"); l.put("url", p+"pagesmasteremployriwayatkerjapegawaizul"+si); l.put("icon","fas fa-briefcase"); l.put("description","Pengalaman kerja sebelumnya"); linkRiwayat.put(l);
    l = new JSONObject(); l.put("title","Riwayat Pelatihan"); l.put("url", p+"pagesmasteremployriwayatpelatihanpegawaizul"+si); l.put("icon","fas fa-chalkboard"); l.put("description","Diklat dan pelatihan yang diikuti"); linkRiwayat.put(l);
    l = new JSONObject(); l.put("title","Riwayat Tanda Jasa"); l.put("url", p+"pagesmasteremployriwayattandajasapegawaizul"+si); l.put("icon","fas fa-medal"); l.put("description","Penghargaan dan tanda jasa"); linkRiwayat.put(l);
    l = new JSONObject(); l.put("title","Riwayat Status Kepegawaian"); l.put("url", p+"pagesmasteremployriwayatstatuskepegawaianzul"+si); l.put("icon","fas fa-id-card"); l.put("description","Perubahan status/jenis kepegawaian"); linkRiwayat.put(l);
    l = new JSONObject(); l.put("title","Riwayat Keluar Negeri"); l.put("url", p+"pagesmasteremployriwayatkeluarnegeripegawaizul"+si); l.put("icon","fas fa-globe"); l.put("description","Perjalanan dinas luar negeri"); linkRiwayat.put(l);
    l = new JSONObject(); l.put("title","Riwayat Organisasi Kampus"); l.put("url", p+"pagesmasteremployriwayatorganisasikampuspegawaizul"+si); l.put("icon","fas fa-users"); l.put("description","Keikutsertaan di organisasi kampus"); linkRiwayat.put(l);
    l = new JSONObject(); l.put("title","Riwayat Organisasi Lain"); l.put("url", p+"pagesmasteremployriwayatorganisasilainpegawaizul"+si); l.put("icon","fas fa-network-wired"); l.put("description","Organisasi di luar kampus"); linkRiwayat.put(l);
    l = new JSONObject(); l.put("title","Riwayat Organisasi Sekolah"); l.put("url", p+"pagesmasteremployriwayatorganisasisekolahpegawaizul"+si); l.put("icon","fas fa-school"); l.put("description","Organisasi di tingkat sekolah"); linkRiwayat.put(l);
    l = new JSONObject(); l.put("title","Keterangan Lain"); l.put("url", p+"pagesmasteremployriwayatketeranganlainpegawaizul"+si); l.put("icon","fas fa-info-circle"); l.put("description","Keterangan tambahan kepegawaian"); linkRiwayat.put(l);
    l = new JSONObject(); l.put("title","Catatan Pegawai"); l.put("url", p+"pagesmastercatatanpegawaizul"+si); l.put("icon","fas fa-sticky-note"); l.put("description","Catatan & notifikasi per pegawai"); linkRiwayat.put(l);
    l = new JSONObject(); l.put("title","Pelanggaran Pegawai"); l.put("url", p+"pagesmasteremploypelanggaranpegawaizul"+si); l.put("icon","fas fa-exclamation-circle"); l.put("description","Rekam pelanggaran disiplin"); linkRiwayat.put(l);
    l = new JSONObject(); l.put("title","Tipe Pegawai"); l.put("url", p+"pagesmasteremploytipepegawaizul"+si); l.put("icon","fas fa-tag"); l.put("description","Master tipe/jenis kepegawaian"); linkRiwayat.put(l);

    // === TAB 3: Pensiun ===
    JSONArray linkPensiun = new JSONArray();
    l = new JSONObject(); l.put("title","Pegawai Pensiun"); l.put("url", p+"pagesmasteremploypegawaipensiunzul"+si); l.put("icon","fas fa-user-clock"); l.put("description","Daftar pegawai yang akan/sudah pensiun"); linkPensiun.put(l);
    l = new JSONObject(); l.put("title","Usulan Pensiun"); l.put("url", p+"pagesmasteremployusulanpegawaipensiunzul"+si); l.put("icon","fas fa-file-alt"); l.put("description","Proses usulan pensiun pegawai"); linkPensiun.put(l);

    // === TAB 4: Payroll & Absensi ===
    JSONArray linkPayroll = new JSONArray();
    l = new JSONObject(); l.put("title","Gaji Pegawai"); l.put("url", p+"pagesmasterpayrollgajipegawaizul"+si); l.put("icon","fas fa-money-bill-wave"); l.put("description","Komponen gaji pegawai"); linkPayroll.put(l);
    l = new JSONObject(); l.put("title","Bayar Gaji"); l.put("url", p+"pagesmasterpayrollbayargajipegawaizul"+si); l.put("icon","fas fa-cash-register"); l.put("description","Proses pembayaran gaji"); linkPayroll.put(l);
    l = new JSONObject(); l.put("title","Absensi Harian Pegawai"); l.put("url", p+"pagesmasterabsensikehadiranpegawaiharianzul"+si); l.put("icon","fas fa-user-check"); l.put("description","Rekam kehadiran harian pegawai"); linkPayroll.put(l);
    l = new JSONObject(); l.put("title","Absensi (Payroll)"); l.put("url", p+"pagesmasterpayrollabsensikehadiranpegawaiharianzul"+si); l.put("icon","fas fa-clipboard-check"); l.put("description","Absensi integrasi payroll"); linkPayroll.put(l);
    l = new JSONObject(); l.put("title","Jenis Shift"); l.put("url", p+"pagesmasterpayrolljenisshiftpegawaizul"+si); l.put("icon","fas fa-clock"); l.put("description","Konfigurasi shift kerja"); linkPayroll.put(l);
    l = new JSONObject(); l.put("title","Jenis Transaksi Pegawai"); l.put("url", p+"pagesmasterpayrolljenistransaksipegawaizul"+si); l.put("icon","fas fa-list"); l.put("description","Jenis tunjangan/potongan"); linkPayroll.put(l);
    l = new JSONObject(); l.put("title","Pengajuan Transaksi"); l.put("url", p+"pagesmasterpayrollpengajuantransaksipegawaizul"+si); l.put("icon","fas fa-file-invoice"); l.put("description","Pengajuan tunjangan/potongan"); linkPayroll.put(l);
    l = new JSONObject(); l.put("title","Persetujuan Transaksi"); l.put("url", p+"pagesmasterpayrollpersetujuanpengajuantransaksipegawaizul"+si); l.put("icon","fas fa-check-circle"); l.put("description","Approval pengajuan transaksi"); linkPayroll.put(l);
    l = new JSONObject(); l.put("title","Posting Transaksi"); l.put("url", p+"pagesmasterpayrollpostingtransaksipegawaizul"+si); l.put("icon","fas fa-paper-plane"); l.put("description","Posting transaksi penggajian"); linkPayroll.put(l);
    l = new JSONObject(); l.put("title","Asuransi Pegawai"); l.put("url", p+"pagesmasterpayrollasuransipegawaizul"+si); l.put("icon","fas fa-shield-alt"); l.put("description","Data asuransi/BPJS pegawai"); linkPayroll.put(l);
    l = new JSONObject(); l.put("title","PTKP Pegawai"); l.put("url", p+"pagesmasterpayrollptkppegawaizul"+si); l.put("icon","fas fa-calculator"); l.put("description","Penghasilan Tidak Kena Pajak"); linkPayroll.put(l);

    // === TAB 5: LKP & Target ===
    JSONArray linkLkp = new JSONArray();
    l = new JSONObject(); l.put("title","Target Kerja Pegawai"); l.put("url", p+"pagesmasterlkptargetkerjapegawaizul"+si); l.put("icon","fas fa-bullseye"); l.put("description","Target kerja/SKP pegawai"); linkLkp.put(l);
    l = new JSONObject(); l.put("title","Target Kerja Tahunan"); l.put("url", p+"pagesmasterlkptargetkerjapegawaitahunanzul"+si); l.put("icon","fas fa-calendar"); l.put("description","Rencana kerja tahunan pegawai"); linkLkp.put(l);
    l = new JSONObject(); l.put("title","Realisasi Kerja Pegawai"); l.put("url", p+"pagesmasterlkprealisasikerjapegawaizul"+si); l.put("icon","fas fa-check"); l.put("description","Capaian/realisasi kerja"); linkLkp.put(l);
    l = new JSONObject(); l.put("title","Realisasi Kerja Tahunan"); l.put("url", p+"pagesmasterlkprealisasikerjapegawaitahunanzul"+si); l.put("icon","fas fa-chart-line"); l.put("description","Rekap realisasi kerja tahunan"); linkLkp.put(l);

    // === Bangun tab panel ===
    JSONArray tabDefs = new JSONArray();

    JSONObject tab1 = new JSONObject();
    tab1.put("tabTitle", "Data Pegawai");
    tab1.put("tabIcon", "fas fa-user-tie");
    tab1.put("type", "html");
    tab1.put("html", mainCrud);
    tabDefs.put(tab1);

    JSONObject tab2 = new JSONObject();
    tab2.put("tabTitle", "Riwayat Kepegawaian");
    tab2.put("tabIcon", "fas fa-history");
    tab2.put("type", "links");
    tab2.put("panelTitle", "Riwayat & Catatan Kepegawaian");
    tab2.put("links", linkRiwayat);
    tabDefs.put(tab2);

    JSONObject tab3 = new JSONObject();
    tab3.put("tabTitle", "Pensiun");
    tab3.put("tabIcon", "fas fa-user-clock");
    tab3.put("type", "links");
    tab3.put("panelTitle", "Manajemen Pensiun Pegawai");
    tab3.put("links", linkPensiun);
    tabDefs.put(tab3);

    JSONObject tab4 = new JSONObject();
    tab4.put("tabTitle", "Payroll & Absensi");
    tab4.put("tabIcon", "fas fa-money-check-alt");
    tab4.put("type", "links");
    tab4.put("panelTitle", "Penggajian, Absensi & Transaksi");
    tab4.put("links", linkPayroll);
    tabDefs.put(tab4);

    JSONObject tab5 = new JSONObject();
    tab5.put("tabTitle", "LKP & Target Kerja");
    tab5.put("tabIcon", "fas fa-bullseye");
    tab5.put("type", "links");
    tab5.put("panelTitle", "Laporan Kinerja Pegawai (LKP)");
    tab5.put("links", linkLkp);
    tabDefs.put(tab5);

    out.println(DynamicJspCrudGenerator.generateTabPanel(tabDefs));
%>
