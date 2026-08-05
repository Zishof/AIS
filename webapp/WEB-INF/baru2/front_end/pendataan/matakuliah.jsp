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
<title>Matakuliah</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT = '<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">

<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-book-open"></i></div>
  <div>
    <div class="ais2-breadcrumb">
      <a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a>
      <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span>
      <span>Matakuliah</span>
    </div>
    <div class="ais2-ph-title">Master Matakuliah</div>
    <div class="ais2-ph-sub">Kelola daftar matakuliah, SKS, dan pengaturan akademik</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-outline" onclick="ekspor()"><i class="fa-solid fa-file-excel"></i> Ekspor</button>
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Tambah MK</button>
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
    <div class="ais2-filter-field" style="min-width:120px;max-width:160px">
      <label>Kode MK</label>
      <input type="text" id="f_kode" placeholder="Cari kode...">
    </div>
    <div class="ais2-filter-field">
      <label>Nama Matakuliah</label>
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
      <select id="f_prodi"><option value="">Semua Prodi</option></select>
    </div>
    <div class="ais2-filter-field">
      <label>Jenjang</label>
      <select id="f_jenjang">
        <option value="">Semua</option>
        <option>D3</option><option>D4</option><option>S1</option><option>S2</option><option>S3</option>
      </select>
    </div>
  </div>
  <div class="ais2-filter-advanced" id="filterLanjut">
    <div class="ais2-filter-checks">
      <label class="ais2-filter-check"><input type="checkbox" id="fc_aktif" checked> Aktif saja</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_global"> Milik Global / Universitas</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_extra"> Ekstrakulikuler</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_praktek"> Praktek</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_teori"> Teori</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_simulasi"> Simulasi</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_praktekLapangan"> Praktek Lapangan</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_pra"> Pra Perkuliahan</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_umum"> Perkuliahan Umum</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_subMk"> Sub MK / Modul</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_belumFeeder"> Belum ke Feeder</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_masukFeeder"> Sudah Feeder</label>
    </div>
  </div>
</div>

<!-- TABS -->
<div class="ais2-tabs" id="tabMk">
  <div class="ais2-tab active" onclick="pilihTab('daftar')"><i class="fa-solid fa-list"></i> Daftar MK</div>
  <div class="ais2-tab" onclick="pilihTab('obe')"><i class="fa-solid fa-chart-line"></i> OBE / CPMK</div>
</div>

<div class="ais2-tab-pane active" id="tab_daftar">
  <div id="tabelMK"></div>
</div>

<div class="ais2-tab-pane" id="tab_obe">
  <div class="ais2-filter-card" style="margin-bottom:12px">
    <div class="ais2-filter-row">
      <div class="ais2-filter-field">
        <label>Pilih Matakuliah</label>
        <input type="text" id="f_mk_obe" placeholder="Cari kode atau nama MK...">
      </div>
      <div style="display:flex;align-items:flex-end">
        <button class="ais2-btn ais2-btn-primary" onclick="muatOBE()"><i class="fa-solid fa-eye"></i> Lihat OBE</button>
      </div>
    </div>
  </div>
  <div id="tabelOBE">
    <div class="ais2-empty"><i class="fa-solid fa-chart-line"></i><h4>Pilih Matakuliah</h4><p>Pilih matakuliah untuk melihat data OBE/CPMK-nya.</p></div>
  </div>
</div>

<script>
var tabelMK = null;

document.addEventListener('DOMContentLoaded', function() {
    muatFakultas();
    initTabel();
});

function initTabel() {
    tabelMK = AIS2.Table.init({
        container: '#tabelMK',
        modul: 'pendataan',
        svc: 'matakuliah_list',
        bisa: { tambah: false, ubah: true, hapus: true },
        sortDefault: 'kode',
        ukuranHalaman: 30,
        kolom: [
            { field:'kode',   label:'Kode', lebar:'100px', sort:true },
            { field:'nama',   label:'Nama Matakuliah', sort:true },
            { field:'namaEn', label:'Nama (Inggris)', sembunyiMobile:true },
            { field:'prodi',  label:'Prodi', sembunyiMobile:true },
            { field:'jenjang',label:'Jenjang', lebar:'70px', sembunyiMobile:true },
            { field:'jenisMatakuliah', label:'Jenis', lebar:'90px', sembunyiMobile:true },
            { field:'kelompokMatakuliah', label:'Kelompok', sembunyiMobile:true },
            { field:'sks',    label:'SKS', lebar:'50px', sort:true },
            { field:'milikUniversitas', label:'Global', lebar:'60px',
              render: function(v) { return AIS2.Badge.yaTidak(v); }, sembunyiMobile:true },
            { field:'extraKulikuler', label:'Extra', lebar:'55px',
              render: function(v) { return v ? '<span class="ais2-badge ais2-badge-purple">Ya</span>' : ''; }, sembunyiMobile:true },
            { field:'aktif', label:'Aktif', lebar:'60px',
              render: function(v) { return AIS2.Badge.aktif(v); } }
        ],
        onUbah: function(d) { bukaFormMK(d); },
        onHapus: function(id, cb) {
            AIS2.Api.post('pendataan','matakuliah_hapus',{ id:id }).then(function(res) {
                cb(res && res.ok, res && res.msg);
            });
        }
    });
}

function terapkan() {
    if (!tabelMK) return;
    tabelMK.setFilter({
        kode: document.getElementById('f_kode').value,
        nama: document.getElementById('f_nama').value,
        fakultas: document.getElementById('f_fakultas').value,
        prodi: document.getElementById('f_prodi').value,
        jenjang: document.getElementById('f_jenjang').value,
        hanyaAktif: document.getElementById('fc_aktif').checked ? '1' : '',
        milikGlobal: document.getElementById('fc_global').checked ? '1' : '',
        ekstra: document.getElementById('fc_extra').checked ? '1' : '',
        praktek: document.getElementById('fc_praktek').checked ? '1' : '',
        teori: document.getElementById('fc_teori').checked ? '1' : '',
        simulasi: document.getElementById('fc_simulasi').checked ? '1' : '',
        praktekLapangan: document.getElementById('fc_praktekLapangan').checked ? '1' : '',
        belumFeeder: document.getElementById('fc_belumFeeder').checked ? '1' : '',
        masukFeeder: document.getElementById('fc_masukFeeder').checked ? '1' : ''
    });
}

function reset() {
    ['f_kode','f_nama'].forEach(function(id) { var el=document.getElementById(id); if(el) el.value=''; });
    ['f_fakultas','f_prodi','f_jenjang'].forEach(function(id) { var el=document.getElementById(id); if(el) el.selectedIndex=0; });
    ['fc_global','fc_extra','fc_praktek','fc_teori','fc_simulasi','fc_praktekLapangan','fc_pra','fc_umum','fc_subMk','fc_belumFeeder','fc_masukFeeder'].forEach(function(id) {
        var el=document.getElementById(id); if(el) el.checked=false;
    });
    document.getElementById('fc_aktif').checked = true;
    terapkan();
}

function toggleLanjut() { document.getElementById('filterLanjut').classList.toggle('show'); }

function pilihTab(k) {
    document.querySelectorAll('#tabMk .ais2-tab').forEach(function(t,i) { t.classList.toggle('active',['daftar','obe'][i]===k); });
    document.querySelectorAll('.ais2-tab-pane').forEach(function(p) { p.classList.remove('active'); });
    document.getElementById('tab_' + k).classList.add('active');
}

function muatFakultas() {
    AIS2.Api.get('pendataan','ref_fakultas',{}).then(function(res) {
        if (!res || !res.data) return;
        var sel = document.getElementById('f_fakultas');
        res.data.forEach(function(f) {
            var o=document.createElement('option'); o.value=f.id; o.textContent=f.nama; sel.appendChild(o);
        });
    });
}
function muatProdi() {
    var fak = document.getElementById('f_fakultas').value;
    var sel = document.getElementById('f_prodi');
    sel.innerHTML = '<option value="">Semua Prodi</option>';
    if (!fak) return;
    AIS2.Api.get('pendataan','ref_prodi',{ fakultas:fak }).then(function(res) {
        if (!res || !res.data) return;
        res.data.forEach(function(p) { var o=document.createElement('option'); o.value=p.id; o.textContent=p.nama; sel.appendChild(o); });
    });
}

function bukaTambah() { bukaFormMK({}); }

function bukaFormMK(d) {
    d = d || {};
    var esc = function(v) { return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); };
    var fld = function(lbl,nm,tp,req) {
        return '<div class="ais2-field"><label>' + lbl + (req?'<span class="req">*</span>':'') + '</label>'
            + '<input class="ais2-input" type="' + (tp||'text') + '" name="' + nm + '" value="' + esc(d[nm]) + '"' + (req?' required':'') + '></div>';
    };
    var sel = function(lbl,nm,opsi) {
        var opts = opsi.map(function(o) {
            return '<option value="' + esc(o) + '"' + (d[nm]===o?' selected':'') + '>' + esc(o) + '</option>';
        }).join('');
        return '<div class="ais2-field"><label>' + lbl + '</label><select name="' + nm + '"><option value="">-- Pilih --</option>' + opts + '</select></div>';
    };
    var chk = function(lbl,nm) {
        return '<div class="ais2-field ais2-field-check"><input type="checkbox" name="' + nm + '" ' + (d[nm]?'checked':'') + '><label>' + lbl + '</label></div>';
    };

    var html = '<input type="hidden" name="id" value="' + esc(d.id) + '">'
        + '<div class="ais2-form-section-title">Identitas Matakuliah</div>'
        + '<div class="ais2-form-grid">'
        + fld('Kode MK','kode','text',true)
        + fld('Nama MK','nama','text',true)
        + fld('Nama Inggris','namaEn')
        + fld('Singkatan','singkatan')
        + sel('Jenis Matakuliah','jenisMatakuliah',['Wajib','Pilihan','Pilihan Wajib','Muatan Lokal','Lintas Prodi'])
        + sel('Kelompok MK','kelompokMatakuliah',['MPK','MKK','MKB','MPB','MBB','Mata Kuliah Pilihan'])
        + sel('Tingkat Kesulitan','kesulitan',['Mudah','Sedang','Sulit'])
        + '</div>'
        + '<div class="ais2-form-section-title" style="margin-top:20px">Program Studi</div>'
        + '<div class="ais2-form-grid">'
        + '<div class="ais2-field"><label>Fakultas <span class="req">*</span></label><select name="fakultasMK" id="fMkFak" onchange="muatProdiMk()" required><option value="">-- Pilih --</option></select></div>'
        + '<div class="ais2-field"><label>Program Studi <span class="req">*</span></label><select name="jurusanMK" id="fMkProdi" required><option value="">-- Pilih --</option></select></div>'
        + '</div>'
        + '<div class="ais2-form-section-title" style="margin-top:20px">Distribusi SKS</div>'
        + '<div class="ais2-form-grid ais2-form-grid-3">'
        + fld('Total SKS','sks','number')
        + fld('SKS Tatap Muka','sksDiskusi','number')
        + fld('SKS Praktek','sksPraktek','number')
        + fld('SKS Praktek Lapangan','sksPraktekLapangan','number')
        + fld('SKS Simulasi','sksSimulasi','number')
        + fld('Maks SKS saat KRS','jumlahMaksimalSksJikaAmbilMkIni','number')
        + '</div>'
        + '<div class="ais2-form-section-title" style="margin-top:20px">Karakteristik</div>'
        + '<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));gap:8px;margin-bottom:16px">'
        + chk('Milik Universitas (Global)','milikUniversitas')
        + chk('Boleh Diambil Prodi Lain','bolehDiambilProdiLain')
        + chk('Ekstrakulikuler','extraKulikuler')
        + chk('Sub MK / Modul','merupakanModul')
        + chk('Pra Perkuliahan','merupakanPraPerkuliahan')
        + chk('Perkuliahan Umum','merupakanPerkuliahanUmum')
        + chk('MK Praktek (saja)','merupakanMkPraktek')
        + chk('MK Teori (saja)','merupakanMkTeori')
        + chk('Ada Tatap Muka/Diskusi','terdapatDiskusi')
        + chk('Ada Kegiatan Praktek','terdapatPraktek')
        + chk('Ada Praktek Lapangan','terdapatPraktekLapangan')
        + chk('Ada Simulasi','terdapatSimulasi')
        + chk('Ada UTS','terdapatUts')
        + chk('Ada UAS','terdapatUas')
        + chk('Ada SAP','adaSap')
        + chk('Ada Silabus','adaSilabus')
        + chk('Ada Bahan Ajar','adaBahanAjar')
        + chk('Ada Diktat','adaDiktat')
        + '</div>'
        + '<div class="ais2-form-section-title" style="margin-top:8px">Informasi Tambahan</div>'
        + '<div class="ais2-form-grid">'
        + fld('Kode Feeder','feeder')
        + fld('Tanggal Mulai Efektif','tanggalMulai','date')
        + fld('Tanggal Akhir Efektif','tanggalSampai','date')
        + '</div>'
        + '<div class="ais2-field ais2-form-full" style="margin-top:10px">'
        + '<label>Keterangan</label><textarea class="ais2-input" name="keterangan" rows="3">' + esc(d.keterangan) + '</textarea></div>'
        + '<div class="ais2-field ais2-form-full"><label>Deskripsi Pembelajaran</label>'
        + '<textarea class="ais2-input" name="deskripsiPembelajaran" rows="3">' + esc(d.deskripsiPembelajaran) + '</textarea></div>';

    var modal = AIS2.Modal.buka({
        judul: d.id ? 'Ubah MK: ' + (d.kode||'') : 'Tambah Matakuliah Baru',
        ikon: 'fa-book-open', ukuran: 'lg',
        konten: html,
        tombol: [
            { label:'Simpan', kelas:'ais2-btn-primary', onClick: function() { simpanMK(modal, !!d.id); } },
            { label:'Tutup',  kelas:'ais2-btn-outline', onClick: function() { AIS2.Modal.tutup(); } }
        ],
        onBuka: function() { _muatFakModalMk(d); }
    });
}

function _muatFakModalMk(d) {
    var sel = document.getElementById('fMkFak');
    if (!sel) return;
    AIS2.Api.get('pendataan','ref_fakultas',{}).then(function(res) {
        if (!res || !res.data) return;
        res.data.forEach(function(f) {
            var o=document.createElement('option'); o.value=f.id; o.textContent=f.nama;
            if (d.fakultasId && String(f.id) === String(d.fakultasId)) o.selected=true;
            sel.appendChild(o);
        });
        if (d.fakultasId) muatProdiMk();
    });
}

window.muatProdiMk = function() {
    var fak = document.getElementById('fMkFak');
    var selP = document.getElementById('fMkProdi');
    selP.innerHTML = '<option value="">-- Pilih --</option>';
    if (!fak || !fak.value) return;
    AIS2.Api.get('pendataan','ref_prodi',{ fakultas:fak.value }).then(function(res) {
        if (!res || !res.data) return;
        res.data.forEach(function(p) { var o=document.createElement('option'); o.value=p.id; o.textContent=p.nama; selP.appendChild(o); });
    });
};

function simpanMK(modal, isEdit) {
    var data = {};
    modal.body().querySelectorAll('[name]').forEach(function(el) {
        data[el.name] = el.type === 'checkbox' ? (el.checked ? 'true' : 'false') : el.value;
    });
    if (!data.kode || !data.nama) {
        AIS2.Toast.peringatan('Kode dan Nama MK wajib diisi'); return;
    }
    AIS2.Api.post('pendataan', isEdit ? 'matakuliah_ubah' : 'matakuliah_tambah', data).then(function(res) {
        if (res && res.ok) {
            AIS2.Toast.sukses(res.msg || 'MK berhasil disimpan');
            AIS2.Modal.tutup();
            if (tabelMK) tabelMK.refresh();
        } else {
            AIS2.Toast.error((res && res.msg) || 'Gagal menyimpan');
        }
    });
}

function muatOBE() { AIS2.Toast.info('Fitur OBE: pilih MK dari daftar.'); }
function ekspor() { window.open(ROOT + '/baru2?modul=pendataan&svc=matakuliah_ekspor', '_blank'); }

document.addEventListener('keydown', function(e) {
    if (e.key === 'Enter' && e.target.tagName === 'INPUT') terapkan();
});
</script>
</body>
</html>
