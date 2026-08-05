<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<%
    String rootCtx = Common.ROOT == null ? "" : Common.ROOT;
%>
<!doctype html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Mahasiswa</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT = '<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">

<!-- ===== PAGE HEADER ===== -->
<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-user-graduate"></i></div>
  <div>
    <div class="ais2-breadcrumb">
      <a href="javascript:void(0)" onclick="parent.openModule && parent.openModule('Pendataan','pendataan')">Pendataan</a>
      <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span>
      <span>Mahasiswa</span>
    </div>
    <div class="ais2-ph-title">Data Mahasiswa</div>
    <div class="ais2-ph-sub">Kelola biodata, status, dan informasi akademik seluruh mahasiswa</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-outline" onclick="bukaCetakExcel()">
      <i class="fa-solid fa-file-excel"></i> Ekspor
    </button>
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()">
      <i class="fa-solid fa-plus"></i> Tambah Mahasiswa
    </button>
  </div>
</div>

<!-- ===== FILTER ===== -->
<div class="ais2-filter-card">
  <div class="ais2-filter-head">
    <i class="fa-solid fa-filter" style="color:var(--ais2-pri2)"></i>
    <span class="ais2-filter-title">Filter Pencarian</span>
    <button class="ais2-btn ais2-btn-sm ais2-btn-outline" onclick="toggleFilterLanjut()">
      <i class="fa-solid fa-sliders"></i> Filter Lanjut
    </button>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" onclick="terapkanFilter()">
      <i class="fa-solid fa-magnifying-glass"></i> Cari
    </button>
    <button class="ais2-btn ais2-btn-sm ais2-btn-outline" onclick="resetFilter()">
      <i class="fa-solid fa-rotate-left"></i>
    </button>
  </div>
  <div class="ais2-filter-row">
    <div class="ais2-filter-field" style="min-width:130px;max-width:160px">
      <label>NIM</label>
      <input type="text" id="f_nim" placeholder="Cari NIM...">
    </div>
    <div class="ais2-filter-field">
      <label>Nama Mahasiswa</label>
      <input type="text" id="f_nama" placeholder="Cari nama...">
    </div>
    <div class="ais2-filter-field" style="min-width:110px;max-width:140px">
      <label>Angkatan</label>
      <input type="number" id="f_angkatan" placeholder="2022">
    </div>
    <div class="ais2-filter-field">
      <label>Status</label>
      <select id="f_status">
        <option value="">Semua Status</option>
        <option value="AKTIF">Aktif</option>
        <option value="CUTI">Cuti</option>
        <option value="LULUS">Lulus</option>
        <option value="KELUAR">Keluar/Pindah</option>
        <option value="MENINGGAL">Meninggal</option>
      </select>
    </div>
    <div class="ais2-filter-field">
      <label>Fakultas</label>
      <select id="f_fakultas" onchange="muatProdi()">
        <option value="">Semua Fakultas</option>
      </select>
    </div>
    <div class="ais2-filter-field">
      <label>Program Studi</label>
      <select id="f_prodi">
        <option value="">Semua Prodi</option>
      </select>
    </div>
  </div>

  <!-- Filter lanjutan -->
  <div class="ais2-filter-advanced" id="filterLanjutPanel">
    <div class="ais2-filter-row" style="margin-bottom:12px">
      <div class="ais2-filter-field">
        <label>Jenjang</label>
        <select id="f_jenjang">
          <option value="">Semua Jenjang</option>
          <option>D3</option><option>D4</option><option>S1</option>
          <option>S2</option><option>S3</option><option>Profesi</option>
        </select>
      </div>
      <div class="ais2-filter-field">
        <label>Program</label>
        <select id="f_program">
          <option value="">Semua Program</option>
          <option>Reguler</option><option>Karyawan</option><option>RPL</option>
        </select>
      </div>
      <div class="ais2-filter-field">
        <label>Dosen PA</label>
        <input type="text" id="f_dosenPA" placeholder="Nama/kode dosen...">
      </div>
      <div class="ais2-filter-field">
        <label>Kelas</label>
        <input type="text" id="f_kelas" placeholder="A, B, Pagi, Malam...">
      </div>
      <div class="ais2-filter-field">
        <label>Status Keluar</label>
        <select id="f_statusKeluar">
          <option value="">Semua</option>
          <option value="LULUS">Lulus</option>
          <option value="DO">Drop Out</option>
          <option value="PINDAH">Pindah</option>
          <option value="MENGUNDURKAN_DIRI">Mengundurkan Diri</option>
        </select>
      </div>
    </div>
    <div class="ais2-filter-checks">
      <label class="ais2-filter-check"><input type="checkbox" id="fc_aktif" checked> Aktif saja</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_tanpaPA"> Tanpa Dosen PA</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_adaKelas"> Ada Kelas</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_tanpaKelas"> Tanpa Kelas</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_belumFeeder"> Belum ke Feeder</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_masukFeeder"> Sudah Feeder</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_wni"> WNI</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_wna"> WNA</label>
    </div>
  </div>
</div>

<!-- ===== TABS ===== -->
<div class="ais2-tabs" id="tabMhs">
  <div class="ais2-tab active" onclick="pilihTab('daftar')"><i class="fa-solid fa-list"></i> Daftar Mahasiswa</div>
  <div class="ais2-tab" onclick="pilihTab('statistik')"><i class="fa-solid fa-chart-pie"></i> Statistik</div>
</div>

<!-- Tab: Daftar -->
<div class="ais2-tab-pane active" id="tab_daftar">
  <div id="tabelMahasiswa"></div>
</div>

<!-- Tab: Statistik -->
<div class="ais2-tab-pane" id="tab_statistik">
  <div style="padding:48px;text-align:center;color:var(--ais2-ink3)">
    <i class="fa-solid fa-chart-pie" style="font-size:48px;margin-bottom:14px;opacity:.4;display:block"></i>
    <div style="font-size:14px">Statistik mahasiswa akan ditampilkan di sini.</div>
  </div>
</div>

<!-- ===== MODAL BIODATA ===== -->
<div id="modalBiodata" style="display:none"><!-- diisi oleh JS saat buka modal --></div>

<script>
/* ======================================================
   HALAMAN MAHASISWA — Controller
   Menggunakan AIS2.Table untuk daftar, AIS2.Modal untuk form biodata.
   ====================================================== */
var tabelMhs = null;

document.addEventListener('DOMContentLoaded', function() {
    muatFakultas();
    initTabel();
});

/** Inisialisasi tabel mahasiswa menggunakan AIS2.Table */
function initTabel() {
    tabelMhs = AIS2.Table.init({
        container: '#tabelMahasiswa',
        modul: 'pendataan',
        svc: 'mahasiswa_list',
        bisa: { tambah: false, ubah: true, hapus: false },
        sortDefault: 'nim',
        ukuranHalaman: 25,
        kolom: [
            { field: 'nim',      label: 'NIM',    lebar: '130px', sort: true },
            { field: 'nama',     label: 'Nama Mahasiswa', sort: true },
            { field: 'angkatan', label: 'Angkatan', lebar: '90px', sort: true, sembunyiMobile: true },
            { field: 'kelas',    label: 'Kelas',   lebar: '80px', sembunyiMobile: true },
            { field: 'prodi',    label: 'Prodi',   sembunyiMobile: true },
            { field: 'status',   label: 'Status',  lebar: '90px',
              render: function(v) {
                var peta = {
                    AKTIF:    { label:'Aktif',    kelas:'ais2-badge-green' },
                    CUTI:     { label:'Cuti',     kelas:'ais2-badge-yellow' },
                    LULUS:    { label:'Lulus',    kelas:'ais2-badge-blue' },
                    KELUAR:   { label:'Keluar',   kelas:'ais2-badge-red' },
                    _default: { label: v||'-', kelas:'ais2-badge-gray' }
                };
                return AIS2.Badge.kustom(v, peta);
              }
            },
            { field: 'dosenPA',  label: 'Dosen PA', sembunyiMobile: true }
        ],
        onUbah: function(data) { bukaEditBiodata(data.id || data.nim); },
        renderAksi: function(row) {
            return '<div class="td-actions">'
                + '<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="bukaEditBiodata(\'' + row.id + '\')" title="Lihat/Ubah Biodata"><i class="fa-solid fa-pen"></i></button>'
                + '<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="cetakKRS(\'' + row.id + '\')" title="Cetak KRS"><i class="fa-solid fa-print"></i></button>'
                + '<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="cetakSuratAktif(\'' + row.id + '\')" title="Surat Keterangan Aktif"><i class="fa-solid fa-file-lines"></i></button>'
                + '</div>';
        }
    });
}

function terapkanFilter() {
    if (!tabelMhs) return;
    tabelMhs.setFilter({
        nim:          document.getElementById('f_nim').value,
        nama:         document.getElementById('f_nama').value,
        angkatan:     document.getElementById('f_angkatan').value,
        status:       document.getElementById('f_status').value,
        fakultas:     document.getElementById('f_fakultas').value,
        prodi:        document.getElementById('f_prodi').value,
        jenjang:      document.getElementById('f_jenjang').value,
        program:      document.getElementById('f_program').value,
        dosenPA:      document.getElementById('f_dosenPA').value,
        kelas:        document.getElementById('f_kelas').value,
        statusKeluar: document.getElementById('f_statusKeluar').value,
        hanyaAktif:   document.getElementById('fc_aktif').checked ? '1' : '',
        tanpaPA:      document.getElementById('fc_tanpaPA').checked ? '1' : '',
        belumFeeder:  document.getElementById('fc_belumFeeder').checked ? '1' : '',
        masukFeeder:  document.getElementById('fc_masukFeeder').checked ? '1' : ''
    });
}

function resetFilter() {
    ['f_nim','f_nama','f_angkatan','f_dosenPA','f_kelas'].forEach(function(id) {
        var el = document.getElementById(id);
        if (el) el.value = '';
    });
    ['f_status','f_fakultas','f_prodi','f_jenjang','f_program','f_statusKeluar'].forEach(function(id) {
        var el = document.getElementById(id);
        if (el) el.selectedIndex = 0;
    });
    ['fc_aktif','fc_tanpaPA','fc_adaKelas','fc_tanpaKelas','fc_belumFeeder','fc_masukFeeder','fc_wni','fc_wna'].forEach(function(id) {
        var el = document.getElementById(id);
        if (el) el.checked = (id === 'fc_aktif');
    });
    terapkanFilter();
}

function toggleFilterLanjut() {
    var p = document.getElementById('filterLanjutPanel');
    p.classList.toggle('show');
}

/** Muat daftar fakultas dari API */
function muatFakultas() {
    AIS2.Api.get('pendataan', 'ref_fakultas', {}).then(function(res) {
        if (!res || !res.data) return;
        var sel = document.getElementById('f_fakultas');
        res.data.forEach(function(f) {
            var opt = document.createElement('option');
            opt.value = f.id; opt.textContent = f.nama;
            sel.appendChild(opt);
        });
    });
}

function muatProdi() {
    var fakId = document.getElementById('f_fakultas').value;
    var sel = document.getElementById('f_prodi');
    sel.innerHTML = '<option value="">Semua Prodi</option>';
    if (!fakId) return;
    AIS2.Api.get('pendataan', 'ref_prodi', { fakultas: fakId }).then(function(res) {
        if (!res || !res.data) return;
        res.data.forEach(function(p) {
            var opt = document.createElement('option');
            opt.value = p.id; opt.textContent = p.nama;
            sel.appendChild(opt);
        });
    });
}

function pilihTab(kunci) {
    document.querySelectorAll('#tabMhs .ais2-tab').forEach(function(t, i) {
        t.classList.toggle('active', ['daftar','statistik'][i] === kunci);
    });
    document.querySelectorAll('.ais2-tab-pane').forEach(function(p) {
        p.classList.remove('active');
    });
    document.getElementById('tab_' + kunci).classList.add('active');
}

/** Buka form tambah mahasiswa baru */
function bukaTambah() {
    bukaFormBiodata(null);
}

/** Buka form edit biodata berdasarkan ID mahasiswa */
function bukaEditBiodata(mhsId) {
    if (!mhsId) return;
    if (typeof NProgress !== 'undefined') NProgress.start();
    AIS2.Api.get('pendataan', 'mahasiswa_detail', { id: mhsId }).then(function(res) {
        var data = (res && res.data) ? res.data : {};
        bukaFormBiodata(data);
    });
}

/**
 * Membuka modal form biodata mahasiswa (tambah atau edit).
 * Form diorganisasi dalam beberapa tab: Data Utama, Alamat, Keluarga, Bank, Alumni.
 * @param {Object|null} data - Data mahasiswa yang akan diedit. null = mode tambah baru.
 */
function bukaFormBiodata(data) {
    var isEdit = data && data.id;
    var judulModal = isEdit ? 'Ubah Biodata: ' + (data.nama || '') : 'Tambah Mahasiswa Baru';
    var html = _htmlFormBiodata(data || {});
    var modal = AIS2.Modal.buka({
        judul: judulModal,
        ikon: 'fa-user-graduate',
        ukuran: 'lg',
        konten: html,
        tombol: [
            { label: 'Simpan', kelas: 'ais2-btn-primary', onClick: function() { simpanBiodata(modal, isEdit); } },
            { label: 'Tutup',  kelas: 'ais2-btn-outline', onClick: function() { AIS2.Modal.tutup(); } }
        ]
    });
}

/** Render HTML form biodata bertab */
function _htmlFormBiodata(d) {
    var esc = function(v) { return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); };
    return ''
    + '<div class="ais2-tabs" id="tabBiodata">'
    + '<div class="ais2-tab active" onclick="_pilihTabModal(\'bData\')"><i class="fa-solid fa-id-card"></i> Data Utama</div>'
    + '<div class="ais2-tab" onclick="_pilihTabModal(\'bAlamat\')"><i class="fa-solid fa-map-pin"></i> Alamat</div>'
    + '<div class="ais2-tab" onclick="_pilihTabModal(\'bKeluarga\')"><i class="fa-solid fa-people-roof"></i> Keluarga</div>'
    + '<div class="ais2-tab" onclick="_pilihTabModal(\'bBank\')"><i class="fa-solid fa-building-columns"></i> Rekening</div>'
    + '<div class="ais2-tab" onclick="_pilihTabModal(\'bAlumni\')"><i class="fa-solid fa-graduation-cap"></i> Alumni</div>'
    + '</div>'
    + '<input type="hidden" name="id" value="' + esc(d.id) + '">'

    /* ── Tab Data Utama ── */
    + '<div class="ais2-tab-pane active" id="mb_bData">'
    + '<div class="ais2-form-section-title">Identitas Akademik</div>'
    + '<div class="ais2-form-grid">'
    + _fld('NIM','nim',d,'wajib')
    + _fld('Nama Lengkap','nama',d,'wajib')
    + _fld('Nama Untuk Ijazah','namaUntukIjazah',d)
    + _fld('Nama Arab (karakter Arab)','namaArab',d)
    + _fld('Nama Tionghoa','namaTionghoa',d)
    + _fldSel('Jenis Kelamin','kelamin',d,['Laki-laki','Perempuan'])
    + _fldSel('Agama','agama',d,['Islam','Kristen Protestan','Kristen Katolik','Hindu','Buddha','Konghucu','Lainnya'])
    + _fldSel('Status Pernikahan','statusNikah',d,['Belum Kawin','Kawin','Cerai'])
    + _fld('Tempat Lahir','tempatlahir',d)
    + _fld('Tanggal Lahir','tanggallahir',d,'','date')
    + _fld('No. HP','hp',d)
    + _fld('Email','email',d,'','email')
    + '</div>'
    + '<div class="ais2-form-section-title" style="margin-top:20px">Data Pendidikan Sebelumnya</div>'
    + '<div class="ais2-form-grid">'
    + _fldSel('Jenis Pendidikan Sebelumnya','jenisSekolah',d,['SMA/MA','SMK','Paket C','Diploma','Lainnya'])
    + _fld('Asal Sekolah/PT Sebelumnya','asalSma_nama',d)
    + _fld('NPSN Sekolah Sebelumnya','npsn',d)
    + _fld('No. Ijazah','noIjazah',d)
    + _fld('NISN','nisn',d)
    + _fld('NIRM','nirm',d)
    + '</div>'
    + '<div class="ais2-form-section-title" style="margin-top:20px">Informasi Tambahan</div>'
    + '<div class="ais2-form-grid">'
    + _fld('Golongan Darah','golonganDarah',d)
    + _fld('NPWP','npwp',d)
    + _fld('Nomor KTP','noIdentitas',d)
    + _fldSel('Kewarganegaraan','kewarganegaraan',d,['WNI','WNA'])
    + _fld('No. SIM','suratIzinMengemudi',d)
    + _fldSel('Transportasi Kuliah','alatTransportasiMahasiswa',d,['Motor','Mobil','Angkutan Umum','Jalan Kaki','Lainnya'])
    + _fldSel('Tempat Tinggal Saat Kuliah','jenisTinggalMahasiswa',d,['Kos','Kontrak','Orang Tua','Saudara','Asrama'])
    + _fld('Hobi','hobi',d)
    + _fld('Minat Seni','minatSeni',d)
    + '</div>'
    + '</div>'

    /* ── Tab Alamat ── */
    + '<div class="ais2-tab-pane" id="mb_bAlamat">'
    + '<div class="ais2-form-section-title">Alamat Domisili</div>'
    + '<div class="ais2-form-grid">'
    + '<div class="ais2-field ais2-form-full"><label>Alamat Jalan</label><input class="ais2-input" type="text" name="alamat" value="' + esc(d.alamat) + '"></div>'
    + _fld('Dusun / Kampung','dusun',d)
    + _fld('RT','rt',d)
    + _fld('RW','rw',d)
    + _fld('Kode Pos','kodepos',d)
    + _fld('Kelurahan / Desa','kelurahan',d)
    + _fld('Kecamatan','kecamatan_nama',d)
    + _fld('Kota / Kabupaten','kota',d)
    + _fld('Provinsi','propinsi',d)
    + _fld('Telepon Rumah','teleponRumah',d)
    + '</div>'
    + '</div>'

    /* ── Tab Keluarga ── */
    + '<div class="ais2-tab-pane" id="mb_bKeluarga">'
    + '<div class="ais2-form-section-title">Data Ayah</div>'
    + '<div class="ais2-form-grid">'
    + _fld('Nama Ayah','namaAyah',d)
    + _fld('No. KTP Ayah','nikAyah',d)
    + _fld('HP Ayah','telpAyah',d)
    + _fld('Tgl Lahir Ayah','tanggalLahirAyah',d,'','date')
    + _fldSel('Pekerjaan Ayah','jenisPekerjaanAyah',d,['PNS/TNI/Polri','Swasta','Wiraswasta','Petani/Nelayan','Tidak Bekerja','Meninggal'])
    + _fldSel('Penghasilan Ayah','jenisPenghasilanAyah',d,['< 1 juta','1-3 juta','3-5 juta','5-10 juta','> 10 juta'])
    + _fldSel('Pendidikan Ayah','jenjangPendidikanAyah',d,['SD/MI','SMP/MTs','SMA/SMK/MA','D3','S1','S2','S3','Tidak Sekolah'])
    + '</div>'
    + '<div class="ais2-form-section-title" style="margin-top:20px">Data Ibu</div>'
    + '<div class="ais2-form-grid">'
    + _fld('Nama Ibu','namaIbu',d)
    + _fld('No. KTP Ibu','nikIbu',d)
    + _fld('HP Ibu','telpIbu',d)
    + _fld('Tgl Lahir Ibu','tanggalLahirIbu',d,'','date')
    + _fldSel('Pekerjaan Ibu','jenisPekerjaanIbu',d,['PNS/TNI/Polri','Swasta','Wiraswasta','Ibu Rumah Tangga','Petani/Nelayan','Tidak Bekerja','Meninggal'])
    + _fldSel('Penghasilan Ibu','jenisPenghasilanIbu',d,['< 1 juta','1-3 juta','3-5 juta','5-10 juta','> 10 juta'])
    + _fldSel('Pendidikan Ibu','jenjangPendidikanIbu',d,['SD/MI','SMP/MTs','SMA/SMK/MA','D3','S1','S2','S3','Tidak Sekolah'])
    + '</div>'
    + '<div class="ais2-form-section-title" style="margin-top:20px">Data Wali</div>'
    + '<div class="ais2-form-grid">'
    + _fld('Nama Wali','namaWali',d)
    + _fld('HP Wali','telpWali',d)
    + _fldSel('Pekerjaan Wali','jenisPekerjaanWali',d,['PNS/TNI/Polri','Swasta','Wiraswasta','Petani/Nelayan','Lainnya'])
    + '</div>'
    + '</div>'

    /* ── Tab Rekening ── */
    + '<div class="ais2-tab-pane" id="mb_bBank">'
    + '<div class="ais2-form-section-title">Rekening Bank</div>'
    + '<div class="ais2-form-grid ais2-form-grid-3">'
    + _fld('Nama Bank','kodeKerjaan',d)
    + _fld('Atas Nama','cabangBri',d)
    + _fld('No. Rekening','no_rek_bri',d)
    + '</div>'
    + '</div>'

    /* ── Tab Alumni ── */
    + '<div class="ais2-tab-pane" id="mb_bAlumni">'
    + '<div class="ais2-form-section-title">Status Setelah Lulus</div>'
    + '<div class="ais2-form-grid">'
    + _fldSel('Status Setelah Lulus','statusSetelahLulus',d,['Melanjutkan Studi','Bekerja','Wirausaha','Belum Bekerja'])
    + _fldSel('Status Pekerjaan','statusPekerjaanSetelahLulus',d,['Sesuai Bidang','Tidak Sesuai Bidang','Freelance','Lainnya'])
    + _fldSel('Status Domisili','statusDomisiliSetelahLulus',d,['Dalam Kota','Luar Kota','Luar Provinsi','Luar Negeri'])
    + _fld('Email Atasan','emailAtasan',d,'','email')
    + '</div>'
    + '</div>';
}

/** Helper render field input */
function _fld(label, name, data, opt, type) {
    var esc = function(v) { return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); };
    var req = opt === 'wajib' ? '<span class="req">*</span>' : '';
    var required = opt === 'wajib' ? ' required' : '';
    var inputType = type || 'text';
    return '<div class="ais2-field"><label>' + label + req + '</label>'
        + '<input class="ais2-input" type="' + inputType + '" name="' + name + '" value="' + esc(data[name]) + '"' + required + '></div>';
}

/** Helper render select field */
function _fldSel(label, name, data, opsi) {
    var esc = function(v) { return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); };
    var val = data[name] || '';
    var opts = opsi.map(function(o) {
        return '<option value="' + esc(o) + '"' + (val === o ? ' selected' : '') + '>' + esc(o) + '</option>';
    }).join('');
    return '<div class="ais2-field"><label>' + label + '</label>'
        + '<select name="' + name + '"><option value="">-- Pilih --</option>' + opts + '</select></div>';
}

function _pilihTabModal(kunci) {
    document.querySelectorAll('#tabBiodata .ais2-tab').forEach(function(t, i) {
        var keys = ['bData','bAlamat','bKeluarga','bBank','bAlumni'];
        t.classList.toggle('active', keys[i] === kunci);
    });
    document.querySelectorAll('[id^="mb_b"]').forEach(function(p) {
        p.classList.remove('active');
    });
    var el = document.getElementById('mb_' + kunci);
    if (el) el.classList.add('active');
}

function simpanBiodata(modal, isEdit) {
    var form = modal.body().querySelector('form') || modal.body();
    var data = {};
    modal.body().querySelectorAll('[name]').forEach(function(el) {
        data[el.name] = el.type === 'checkbox' ? (el.checked ? 'true' : 'false') : el.value;
    });
    if (!data.nim || !data.nim.trim() || !data.nama || !data.nama.trim()) {
        AIS2.Toast.peringatan('NIM dan Nama wajib diisi');
        _pilihTabModal('bData');
        return;
    }
    AIS2.Api.post('pendataan', isEdit ? 'mahasiswa_ubah' : 'mahasiswa_tambah', data).then(function(res) {
        if (res && res.ok) {
            AIS2.Toast.sukses(res.msg || 'Data mahasiswa berhasil disimpan');
            AIS2.Modal.tutup();
            if (tabelMhs) tabelMhs.refresh();
        } else {
            AIS2.Toast.error((res && res.msg) || 'Gagal menyimpan data');
        }
    });
}

function cetakKRS(id) {
    window.open(ROOT + '/baru2?modul=pendataan&svc=cetak_krs&id=' + encodeURIComponent(id), '_blank');
}

function cetakSuratAktif(id) {
    window.open(ROOT + '/baru2?modul=pendataan&svc=cetak_surat_aktif&id=' + encodeURIComponent(id), '_blank');
}

function bukaCetakExcel() {
    var filter = tabelMhs ? tabelMhs.getFilter() : {};
    var qs = Object.keys(filter).map(function(k) {
        return encodeURIComponent(k) + '=' + encodeURIComponent(filter[k]||'');
    }).join('&');
    window.open(ROOT + '/baru2?modul=pendataan&svc=mahasiswa_ekspor&' + qs, '_blank');
}

/* Enter = cari */
document.addEventListener('keydown', function(e) {
    if (e.key === 'Enter' && (e.target.tagName === 'INPUT')) terapkanFilter();
});
</script>
</body>
</html>
