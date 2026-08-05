<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Beasiswa</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">

<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-hand-holding-dollar"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Beasiswa</span></div>
    <div class="ais2-ph-title">Program Beasiswa</div>
    <div class="ais2-ph-sub">Kelola program, persyaratan, dan seleksi penerima beasiswa</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Tambah Program</button>
  </div>
</div>

<!-- FILTER -->
<div class="ais2-filter-card">
  <div class="ais2-filter-head">
    <i class="fa-solid fa-filter" style="color:var(--ais2-pri2)"></i>
    <span class="ais2-filter-title">Filter</span>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
    <button class="ais2-btn ais2-btn-sm ais2-btn-outline" onclick="reset()"><i class="fa-solid fa-rotate-left"></i></button>
  </div>
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>Nama Program</label><input type="text" id="f_nama" placeholder="Cari nama program..."></div>
    <div class="ais2-filter-field"><label>Tahun</label>
      <select id="f_tahun"><option value="">Semua Tahun</option></select>
    </div>
    <div class="ais2-filter-field"><label>Jenis Beasiswa</label>
      <select id="f_jenis">
        <option value="">Semua</option>
        <option>Beasiswa Pemerintah</option><option>Beasiswa Swasta</option>
        <option>Beasiswa Internal</option><option>Beasiswa Bidikmisi/KIP</option>
        <option>Beasiswa Alumni</option><option>Beasiswa Prestasi</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Mahasiswa</label><input type="text" id="f_mhs" placeholder="Cari NIM/nama penerima..."></div>
  </div>
</div>

<!-- TABS -->
<div class="ais2-tabs" id="tabBs">
  <div class="ais2-tab active" onclick="pilihTab('program')"><i class="fa-solid fa-list"></i> Program Beasiswa</div>
  <div class="ais2-tab" onclick="pilihTab('penerima')"><i class="fa-solid fa-users"></i> Penerima Beasiswa</div>
  <div class="ais2-tab" onclick="pilihTab('jenis')"><i class="fa-solid fa-tags"></i> Jenis Beasiswa</div>
</div>

<div class="ais2-tab-pane active" id="tab_program">
  <div id="tabelBeasiswa"></div>
</div>

<div class="ais2-tab-pane" id="tab_penerima">
  <div class="ais2-filter-card" style="margin-bottom:12px">
    <div class="ais2-filter-row">
      <div class="ais2-filter-field"><label>Program Beasiswa</label>
        <select id="f_programPenerima"><option value="">Semua Program</option></select>
      </div>
      <div class="ais2-filter-field"><label>Tahun</label>
        <select id="f_tahunPenerima"><option value="">Semua Tahun</option></select>
      </div>
      <div style="display:flex;align-items:flex-end">
        <button class="ais2-btn ais2-btn-primary" onclick="muatPenerima()"><i class="fa-solid fa-magnifying-glass"></i> Tampilkan</button>
      </div>
    </div>
  </div>
  <div id="tabelPenerima"></div>
</div>

<div class="ais2-tab-pane" id="tab_jenis">
  <div id="tabelJenisBeasiswa"></div>
</div>

<script>
var tbl=null, tblPenerima=null;

document.addEventListener('DOMContentLoaded',function(){
    muatTahun();
    initTabel();
});

function initTabel(){
    tbl=AIS2.Table.init({
        container:'#tabelBeasiswa', modul:'pendataan', svc:'beasiswa_list',
        bisa:{tambah:false,ubah:true,hapus:true},
        kolom:[
            {field:'tahun',     label:'Tahun', lebar:'70px', sort:true},
            {field:'nama',      label:'Nama Program', sort:true},
            {field:'jenis',     label:'Jenis', sembunyiMobile:true},
            {field:'tanggalBuka', label:'Buka', lebar:'90px', sembunyiMobile:true},
            {field:'tanggalTutup',label:'Tutup',lebar:'90px', sembunyiMobile:true},
            {field:'sponsor',   label:'Instansi/Sponsor', sembunyiMobile:true},
            {field:'dibuka',    label:'Dibuka', lebar:'70px',
             render:function(v){return AIS2.Badge.aktif(v);}}
        ],
        onUbah:function(d){bukaForm(d);},
        onHapus:function(id,cb){AIS2.Api.post('pendataan','beasiswa_hapus',{id:id}).then(function(r){cb(r&&r.ok,r&&r.msg);});}
    });
}

function terapkan(){
    tbl&&tbl.setFilter({
        nama:document.getElementById('f_nama').value,
        tahun:document.getElementById('f_tahun').value,
        jenis:document.getElementById('f_jenis').value,
        mhs:document.getElementById('f_mhs').value
    });
}
function reset(){
    ['f_nama','f_mhs'].forEach(function(id){var el=document.getElementById(id);if(el)el.value='';});
    ['f_tahun','f_jenis'].forEach(function(id){var el=document.getElementById(id);if(el)el.selectedIndex=0;});
    terapkan();
}
function muatTahun(){
    var sel=document.getElementById('f_tahun'); var cur=new Date().getFullYear();
    for(var y=cur;y>=cur-10;y--){var o=document.createElement('option');o.value=y;o.textContent=y;sel.appendChild(o);}
}
function pilihTab(k){
    document.querySelectorAll('#tabBs .ais2-tab').forEach(function(t,i){t.classList.toggle('active',['program','penerima','jenis'][i]===k);});
    document.querySelectorAll('.ais2-tab-pane').forEach(function(p){p.classList.remove('active');});
    document.getElementById('tab_'+k).classList.add('active');
}
function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-section-title">Informasi Program</div>'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Nama Program <span class="req">*</span></label><input class="ais2-input" name="nama" value="'+esc(d.nama)+'" required></div>'
        +'<div class="ais2-field"><label>Tahun</label><input class="ais2-input" type="number" name="tahun" value="'+esc(d.tahun)+'"></div>'
        +'<div class="ais2-field"><label>Jenis Beasiswa</label><select name="jenis" class="ais2-input"><option value="">--</option>'
        +['Pemerintah','Swasta','Internal','Bidikmisi/KIP','Alumni','Prestasi'].map(function(j){return '<option'+(d.jenis===j?' selected':'')+'>'+j+'</option>';}).join('')
        +'</select></div>'
        +'<div class="ais2-field"><label>Instansi/Sponsor</label><input class="ais2-input" name="sponsor" value="'+esc(d.sponsor)+'"></div>'
        +'<div class="ais2-field"><label>Tanggal Buka</label><input class="ais2-input" type="date" name="tanggalBuka" value="'+esc(d.tanggalBuka)+'"></div>'
        +'<div class="ais2-field"><label>Tanggal Tutup</label><input class="ais2-input" type="date" name="tanggalTutup" value="'+esc(d.tanggalTutup)+'"></div>'
        +'</div>'
        +'<div class="ais2-form-section-title" style="margin-top:20px">Persyaratan Nilai</div>'
        +'<div class="ais2-form-grid ais2-form-grid-3">'
        +'<div class="ais2-field"><label>Min. IPK</label><input class="ais2-input" type="number" step="0.01" name="minIpk" value="'+esc(d.minIpk)+'"></div>'
        +'<div class="ais2-field"><label>Min. SKS</label><input class="ais2-input" type="number" name="minSks" value="'+esc(d.minSks)+'"></div>'
        +'<div class="ais2-field"><label>Min. Angka Kredit</label><input class="ais2-input" type="number" name="minAngkaKredit" value="'+esc(d.minAngkaKredit)+'"></div>'
        +'</div>'
        +'<div class="ais2-form-section-title" style="margin-top:20px">Lingkup</div>'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Jenjang</label><select name="jenjang" class="ais2-input"><option value="">Semua</option>'
        +['D3','D4','S1','S2','S3'].map(function(j){return '<option'+(d.jenjang===j?' selected':'')+'>'+j+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Fakultas</label><select name="fakultas" class="ais2-input"><option value="">Semua</option></select></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:12px"><label>Keterangan / Deskripsi</label>'
        +'<textarea class="ais2-input" name="keterangan" rows="3">'+esc(d.keterangan)+'</textarea></div>'
        +'<div class="ais2-field ais2-field-check" style="margin-top:10px"><input type="checkbox" name="dibuka" '+(d.dibuka?'checked':'')+'><label>Program Aktif (Dibuka)</label></div>';

    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Program Beasiswa':'Tambah Program Beasiswa',ikon:'fa-hand-holding-dollar',ukuran:'',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.type==='checkbox'?(el.checked?'true':'false'):el.value;});
                if(!data.nama){AIS2.Toast.peringatan('Nama program wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'beasiswa_ubah':'beasiswa_tambah',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Tersimpan');AIS2.Modal.tutup();tbl&&tbl.refresh();}
                    else AIS2.Toast.error((r&&r.msg)||'Gagal');
                });
            }},
            {label:'Tutup',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}
function muatPenerima(){AIS2.Toast.info('Memuat data penerima...');}
document.addEventListener('keydown',function(e){if(e.key==='Enter'&&e.target.tagName==='INPUT')terapkan();});
</script>
</body></html>
