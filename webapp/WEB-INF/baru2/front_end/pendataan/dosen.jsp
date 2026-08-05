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
<title>Dosen</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT = '<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">

<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-person-chalkboard"></i></div>
  <div>
    <div class="ais2-breadcrumb">
      <a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a>
      <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span>
      <span>Dosen</span>
    </div>
    <div class="ais2-ph-title">Data Dosen</div>
    <div class="ais2-ph-sub">Kelola biodata, kepegawaian, dan penugasan dosen</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-outline" onclick="ekspor()"><i class="fa-solid fa-file-excel"></i> Ekspor</button>
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Tambah Dosen</button>
  </div>
</div>

<!-- FILTER -->
<div class="ais2-filter-card">
  <div class="ais2-filter-head">
    <i class="fa-solid fa-filter" style="color:var(--ais2-pri2)"></i>
    <span class="ais2-filter-title">Filter Pencarian</span>
    <button class="ais2-btn ais2-btn-sm ais2-btn-outline" onclick="toggleLanjut()"><i class="fa-solid fa-sliders"></i> Lanjutan</button>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
    <button class="ais2-btn ais2-btn-sm ais2-btn-outline" onclick="reset()"><i class="fa-solid fa-rotate-left"></i></button>
  </div>
  <div class="ais2-filter-row">
    <div class="ais2-filter-field">
      <label>Kode / NIP / NIDN</label>
      <input type="text" id="f_kode" placeholder="Cari kode/NIP/NIDN...">
    </div>
    <div class="ais2-filter-field">
      <label>Nama Dosen</label>
      <input type="text" id="f_nama" placeholder="Cari nama...">
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
    <div class="ais2-filter-field">
      <label>Status</label>
      <select id="f_status">
        <option value="">Semua</option>
        <option value="AKTIF">Aktif</option>
        <option value="TIDAK_AKTIF">Tidak Aktif</option>
        <option value="PENSIUN">Pensiun</option>
      </select>
    </div>
  </div>
  <div class="ais2-filter-advanced" id="filterLanjut">
    <div class="ais2-filter-row" style="margin-bottom:12px">
      <div class="ais2-filter-field">
        <label>Ikatan Kerja</label>
        <select id="f_ikatanKerja">
          <option value="">Semua</option>
          <option>PNS</option><option>PPPK</option><option>Tetap Yayasan</option>
          <option>Tidak Tetap</option><option>DLB</option>
        </select>
      </div>
      <div class="ais2-filter-field">
        <label>Jabatan Fungsional</label>
        <select id="f_jabatanFungsional">
          <option value="">Semua</option>
          <option>Asisten Ahli</option><option>Lektor</option>
          <option>Lektor Kepala</option><option>Profesor / Guru Besar</option>
        </select>
      </div>
      <div class="ais2-filter-field">
        <label>Golongan</label>
        <select id="f_golongan">
          <option value="">Semua</option>
          <option>III/a</option><option>III/b</option><option>III/c</option><option>III/d</option>
          <option>IV/a</option><option>IV/b</option><option>IV/c</option><option>IV/d</option><option>IV/e</option>
        </select>
      </div>
      <div class="ais2-filter-field">
        <label>Jenis Pendidik</label>
        <select id="f_jenisPendidik">
          <option value="">Semua</option>
          <option>Dosen</option><option>Guru</option><option>Tenaga Kependidikan</option>
        </select>
      </div>
    </div>
    <div class="ais2-filter-checks">
      <label class="ais2-filter-check"><input type="checkbox" id="fc_tampilkanSemua"> Tampilkan semua (termasuk non-aktif)</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_bolehMengajarLain"> Boleh mengajar prodi lain</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_sertifikasi"> Tersertifikasi</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_belumFeeder"> Belum ke Feeder</label>
    </div>
  </div>
</div>

<!-- TABEL -->
<div class="ais2-tabs" id="tabDosen">
  <div class="ais2-tab active" onclick="pilihTab('daftar')"><i class="fa-solid fa-list"></i> Daftar Dosen</div>
  <div class="ais2-tab" onclick="pilihTab('penugasan')"><i class="fa-solid fa-chalkboard"></i> Penugasan Mengajar</div>
</div>

<div class="ais2-tab-pane active" id="tab_daftar">
  <div id="tabelDosen"></div>
</div>

<div class="ais2-tab-pane" id="tab_penugasan">
  <div class="ais2-filter-card" style="margin-bottom:12px">
    <div class="ais2-filter-row">
      <div class="ais2-filter-field">
        <label>Tahun Akademik</label>
        <select id="f_ta_penugasan"><option value="">Semua TA</option></select>
      </div>
      <div class="ais2-filter-field">
        <label>Semester</label>
        <select id="f_smt_penugasan">
          <option value="">Semua</option>
          <option value="1">Ganjil</option><option value="2">Genap</option>
        </select>
      </div>
      <div style="display:flex;align-items:flex-end">
        <button class="ais2-btn ais2-btn-primary" onclick="refreshPenugasan()"><i class="fa-solid fa-magnifying-glass"></i> Tampilkan</button>
      </div>
    </div>
  </div>
  <div id="tabelPenugasan"></div>
</div>

<script>
var tabelDsn = null;

document.addEventListener('DOMContentLoaded', function() {
    muatFakultas();
    initTabel();
});

function initTabel() {
    tabelDsn = AIS2.Table.init({
        container: '#tabelDosen',
        modul: 'pendataan',
        svc: 'dosen_list',
        bisa: { tambah: false, ubah: true, hapus: false },
        sortDefault: 'nama',
        ukuranHalaman: 25,
        kolom: [
            { field: 'kode',   label: 'Kode/NIP',   lebar:'120px', sort:true },
            { field: 'nidn',   label: 'NIDN/NUPN',  lebar:'120px', sembunyiMobile:true },
            { field: 'nama',   label: 'Nama Dosen',  sort:true },
            { field: 'gelarDepan', label: 'Gelar', lebar:'100px', sembunyiMobile:true },
            { field: 'prodi',  label: 'Prodi Home Base', sembunyiMobile:true },
            { field: 'ikatanKerja', label: 'Ikatan Kerja', lebar:'120px', sembunyiMobile:true },
            { field: 'jabatanFungsional', label: 'Jabatan Fungsional', sembunyiMobile:true },
            { field: 'email',  label: 'Email', sembunyiMobile:true },
            { field: 'aktif',  label: 'Aktif', lebar:'70px',
              render: function(v) { return AIS2.Badge.aktif(v); }
            }
        ],
        onUbah: function(data) { bukaBiodata(data.id); },
        renderAksi: function(row) {
            return '<div class="td-actions">'
                + '<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="bukaBiodata(\'' + row.id + '\')" title="Ubah Biodata"><i class="fa-solid fa-pen"></i></button>'
                + '<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="lihatJadwal(\'' + row.id + '\')" title="Jadwal Mengajar"><i class="fa-solid fa-calendar"></i></button>'
                + '</div>';
        }
    });
}

function terapkan() {
    if (!tabelDsn) return;
    tabelDsn.setFilter({
        kode: document.getElementById('f_kode').value,
        nama: document.getElementById('f_nama').value,
        fakultas: document.getElementById('f_fakultas').value,
        prodi: document.getElementById('f_prodi').value,
        status: document.getElementById('f_status').value,
        ikatanKerja: document.getElementById('f_ikatanKerja').value,
        jabatanFungsional: document.getElementById('f_jabatanFungsional').value,
        golongan: document.getElementById('f_golongan').value,
        sertifikasi: document.getElementById('fc_sertifikasi').checked ? '1' : ''
    });
}

function reset() {
    ['f_kode','f_nama'].forEach(function(id) {
        var el = document.getElementById(id); if (el) el.value = '';
    });
    ['f_fakultas','f_prodi','f_status','f_ikatanKerja','f_jabatanFungsional','f_golongan'].forEach(function(id) {
        var el = document.getElementById(id); if (el) el.selectedIndex = 0;
    });
    terapkan();
}

function toggleLanjut() {
    document.getElementById('filterLanjut').classList.toggle('show');
}

function pilihTab(k) {
    document.querySelectorAll('#tabDosen .ais2-tab').forEach(function(t, i) {
        t.classList.toggle('active', ['daftar','penugasan'][i] === k);
    });
    document.querySelectorAll('.ais2-tab-pane').forEach(function(p) { p.classList.remove('active'); });
    document.getElementById('tab_' + k).classList.add('active');
}

function muatFakultas() {
    AIS2.Api.get('pendataan','ref_fakultas',{}).then(function(res) {
        if (!res || !res.data) return;
        var sel = document.getElementById('f_fakultas');
        res.data.forEach(function(f) {
            var o = document.createElement('option'); o.value = f.id; o.textContent = f.nama; sel.appendChild(o);
        });
    });
}

function muatProdi() {
    var fakId = document.getElementById('f_fakultas').value;
    var sel = document.getElementById('f_prodi');
    sel.innerHTML = '<option value="">Semua Prodi</option>';
    if (!fakId) return;
    AIS2.Api.get('pendataan','ref_prodi',{ fakultas:fakId }).then(function(res) {
        if (!res || !res.data) return;
        res.data.forEach(function(p) {
            var o = document.createElement('option'); o.value = p.id; o.textContent = p.nama; sel.appendChild(o);
        });
    });
}

function bukaTambah() { bukaBiodataForm(null); }

function bukaBiodata(id) {
    AIS2.Api.get('pendataan','dosen_detail',{ id:id }).then(function(res) {
        bukaBiodataForm(res && res.data ? res.data : {});
    });
}

function bukaBiodataForm(d) {
    d = d || {};
    var esc = function(v) { return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); };
    var fld = function(lbl, nm, tp) {
        return '<div class="ais2-field"><label>' + lbl + '</label>'
            + '<input class="ais2-input" type="' + (tp||'text') + '" name="' + nm + '" value="' + esc(d[nm]) + '"></div>';
    };
    var sel = function(lbl, nm, opsi) {
        var opts = opsi.map(function(o) {
            return '<option value="' + esc(o) + '"' + (d[nm]===o?' selected':'') + '>' + esc(o) + '</option>';
        }).join('');
        return '<div class="ais2-field"><label>' + lbl + '</label><select name="' + nm + '"><option value="">-- Pilih --</option>' + opts + '</select></div>';
    };

    var html = '<input type="hidden" name="id" value="' + esc(d.id) + '">'
        + '<div class="ais2-tabs" id="tabBiodataDsn">'
        + '<div class="ais2-tab active" onclick="_dsn(\'utama\')"><i class="fa-solid fa-id-card"></i> Identitas</div>'
        + '<div class="ais2-tab" onclick="_dsn(\'kepegawaian\')"><i class="fa-solid fa-briefcase"></i> Kepegawaian</div>'
        + '<div class="ais2-tab" onclick="_dsn(\'pendidikan\')"><i class="fa-solid fa-graduation-cap"></i> Pendidikan</div>'
        + '</div>'
        /* Tab Identitas */
        + '<div class="ais2-tab-pane active" id="dsn_utama">'
        + '<div class="ais2-form-section-title">Identitas Dosen</div>'
        + '<div class="ais2-form-grid">'
        + fld('NIP / Kode Dosen','kode') + fld('NIP (PNS)','mycode') + fld('NIDN / NUPN','nidn')
        + fld('Nama Lengkap','nama') + fld('Gelar Depan','gelarDepan') + fld('Gelar Belakang','gelarBelakang')
        + sel('Jenis Kelamin','kelamin',['Laki-laki','Perempuan'])
        + fld('Tempat Lahir','tempatlahir') + fld('Tanggal Lahir','tanggallahir','date')
        + fld('No. HP / Telp','telp') + fld('Email','email','email')
        + fld('NPWP','npwp') + fld('NUPTK','nuptk') + fld('NIY / NIGB','niyNigk')
        + fld('Kode Sinta','kodeSinta') + fld('Google Scholar','googleScholar')
        + fld('No. Akta & Ijin Mengajar','aktaIjinAjar')
        + '</div>'
        + '</div>'
        /* Tab Kepegawaian */
        + '<div class="ais2-tab-pane" id="dsn_kepegawaian">'
        + '<div class="ais2-form-section-title">Data Kepegawaian</div>'
        + '<div class="ais2-form-grid">'
        + sel('Ikatan Kerja','ikatanKerjaDosen',['PNS','PPPK','Tetap Yayasan','Tidak Tetap (Kontrak)','DLB'])
        + sel('Status Kepegawaian','statusKepegawaian',['Aktif','Nonaktif','Pensiun','Meninggal'])
        + sel('Golongan','golonganPegawai',['I/a','I/b','I/c','I/d','II/a','II/b','II/c','II/d','III/a','III/b','III/c','III/d','IV/a','IV/b','IV/c','IV/d','IV/e'])
        + sel('Jabatan Fungsional','jabatanFungsionalDosen',['Asisten Ahli','Lektor','Lektor Kepala','Profesor / Guru Besar','Instruktur','Guru Pembimbing','Tenaga Pengajar'])
        + sel('Sumber Gaji','sumberGaji',['PT Sendiri','APBN','APBD','Yayasan','Komite','Lainnya'])
        + sel('Jenis Pendidik & Tenaga Kependidikan','jenisPendidikDanTenagaKependidikan',['Dosen','Guru','Tenaga Kependidikan','Tutor','Instruktur'])
        + sel('Lembaga Pengangkat','lembagaPengangkat',['PT/Yayasan Sendiri','Dikti/Diknas','Pemerintah Daerah','Swasta','Lainnya'])
        + fld('Nomor SK CPNS','skCpns') + fld('Tanggal SK CPNS','tglSkCpns','date')
        + fld('Nomor SK Pengangkatan','skAngkat') + fld('TMT Pengangkatan','tmtSkAngkat','date')
        + fld('TMT PNS','tmtPns','date')
        + '</div>'
        + '<div class="ais2-form-section-title" style="margin-top:20px">Spesialisasi Bidang Ilmu</div>'
        + '<div class="ais2-form-grid">'
        + fld('Bidang Ilmu I','spesialisasi1') + fld('Bidang Ilmu II','spesialisasi2') + fld('Bidang Ilmu III','spesialisasi3')
        + '</div>'
        + '</div>'
        /* Tab Pendidikan */
        + '<div class="ais2-tab-pane" id="dsn_pendidikan">'
        + '<div class="ais2-form-section-title">Riwayat Pendidikan</div>'
        + '<div class="ais2-form-grid">'
        + sel('Pendidikan Terakhir','pendidikan',['S1','S2','S3','Profesi','Vokasi','Lainnya'])
        + fld('Bidang Ilmu S1','pendidikans1') + fld('Bidang Ilmu S2','pendidikans2') + fld('Bidang Ilmu S3','pendidikans3')
        + '</div>'
        + '<div class="ais2-form-section-title" style="margin-top:20px">Sertifikasi</div>'
        + '<div class="ais2-form-grid">'
        + '<div class="ais2-field ais2-field-check"><input type="checkbox" name="sertifikasi" ' + (d.sertifikasi?'checked':'') + '><label>Sudah Tersertifikasi</label></div>'
        + fld('Nomor Sertifikasi','nomorSertifikasi')
        + '<div class="ais2-field ais2-field-check"><input type="checkbox" name="sesuaiBidangKeilmuan" ' + (d.sesuaiBidangKeilmuan?'checked':'') + '><label>Sesuai Bidang Keilmuan</label></div>'
        + '</div>'
        + '</div>';

    var modal = AIS2.Modal.buka({
        judul: d.id ? 'Ubah: ' + (d.nama||'') : 'Tambah Dosen Baru',
        ikon: 'fa-person-chalkboard', ukuran: 'lg',
        konten: html,
        tombol: [
            { label:'Simpan', kelas:'ais2-btn-primary', onClick: function() { simpanDosen(modal, !!d.id); } },
            { label:'Tutup',  kelas:'ais2-btn-outline', onClick: function() { AIS2.Modal.tutup(); } }
        ]
    });
}

window._dsn = function(k) {
    document.querySelectorAll('#tabBiodataDsn .ais2-tab').forEach(function(t,i) {
        t.classList.toggle('active', ['utama','kepegawaian','pendidikan'][i] === k);
    });
    ['dsn_utama','dsn_kepegawaian','dsn_pendidikan'].forEach(function(id) {
        var el = document.getElementById(id); if (el) el.classList.remove('active');
    });
    var el = document.getElementById('dsn_' + k); if (el) el.classList.add('active');
};

function simpanDosen(modal, isEdit) {
    var data = {};
    modal.body().querySelectorAll('[name]').forEach(function(el) {
        data[el.name] = el.type === 'checkbox' ? (el.checked ? 'true' : 'false') : el.value;
    });
    if (!data.kode || !data.nama) {
        AIS2.Toast.peringatan('Kode dan Nama wajib diisi'); return;
    }
    AIS2.Api.post('pendataan', isEdit ? 'dosen_ubah' : 'dosen_tambah', data).then(function(res) {
        if (res && res.ok) {
            AIS2.Toast.sukses(res.msg || 'Data dosen berhasil disimpan');
            AIS2.Modal.tutup();
            if (tabelDsn) tabelDsn.refresh();
        } else {
            AIS2.Toast.error((res && res.msg) || 'Gagal menyimpan');
        }
    });
}

function lihatJadwal(id) {
    window.open(ROOT + '/baru2?modul=pendataan&svc=dosen_jadwal&id=' + encodeURIComponent(id), '_blank');
}

function refreshPenugasan() { /* load penugasan tabel */ }

function ekspor() {
    var filter = tabelDsn ? tabelDsn.getFilter() : {};
    var qs = Object.keys(filter).map(function(k) {
        return encodeURIComponent(k) + '=' + encodeURIComponent(filter[k]||'');
    }).join('&');
    window.open(ROOT + '/baru2?modul=pendataan&svc=dosen_ekspor&' + qs, '_blank');
}

document.addEventListener('keydown', function(e) {
    if (e.key === 'Enter' && e.target.tagName === 'INPUT') terapkan();
});
</script>
</body>
</html>
