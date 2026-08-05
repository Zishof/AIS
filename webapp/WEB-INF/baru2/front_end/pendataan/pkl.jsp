<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>PKL</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">

<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-hard-hat"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>PKL</span></div>
    <div class="ais2-ph-title">Praktik Kerja Lapangan (PKL)</div>
    <div class="ais2-ph-sub">Kelola program PKL, persyaratan, penilaian, dan pembagian kelompok</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Tambah Program PKL</button>
  </div>
</div>

<div class="ais2-filter-card">
  <div class="ais2-filter-head">
    <i class="fa-solid fa-filter" style="color:var(--ais2-pri2)"></i>
    <span class="ais2-filter-title">Filter</span>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
    <button class="ais2-btn ais2-btn-sm ais2-btn-outline" onclick="reset()"><i class="fa-solid fa-rotate-left"></i></button>
  </div>
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>Nama PKL</label><input type="text" id="f_nama" placeholder="Cari nama PKL..."></div>
    <div class="ais2-filter-field"><label>Fakultas</label><select id="f_fak"><option value="">Semua</option></select></div>
    <div class="ais2-filter-field"><label>Program Studi</label><select id="f_prodi"><option value="">Semua</option></select></div>
    <div class="ais2-filter-field"><label>Mahasiswa / Perusahaan</label><input type="text" id="f_cari" placeholder="Cari..."></div>
  </div>
</div>

<div class="ais2-tabs" id="tabPkl">
  <div class="ais2-tab active" onclick="pilihTab('program')"><i class="fa-solid fa-list"></i> Program PKL</div>
  <div class="ais2-tab" onclick="pilihTab('persyaratan')"><i class="fa-solid fa-clipboard-list"></i> Persyaratan</div>
  <div class="ais2-tab" onclick="pilihTab('penilaian')"><i class="fa-solid fa-star"></i> Komponen Penilaian</div>
  <div class="ais2-tab" onclick="pilihTab('kelompok')"><i class="fa-solid fa-people-group"></i> Pembagian Kelompok</div>
</div>

<div class="ais2-tab-pane active" id="tab_program"><div id="tabelPKL"></div></div>
<div class="ais2-tab-pane" id="tab_persyaratan">
  <div class="ais2-empty"><i class="fa-solid fa-clipboard-list"></i><h4>Pilih Program PKL</h4><p>Klik tombol Persyaratan pada baris program PKL untuk mengelola persyaratan.</p></div>
</div>
<div class="ais2-tab-pane" id="tab_penilaian">
  <div class="ais2-empty"><i class="fa-solid fa-star"></i><h4>Pilih Program PKL</h4><p>Klik tombol Penilaian pada baris program PKL.</p></div>
</div>
<div class="ais2-tab-pane" id="tab_kelompok"><div id="tabelKelompokPKL"></div></div>

<script>
var tbl=null;
document.addEventListener('DOMContentLoaded',function(){
    muatFakultas();
    tbl=AIS2.Table.init({
        container:'#tabelPKL', modul:'pendataan', svc:'pkl_list',
        bisa:{tambah:false,ubah:true,hapus:true},
        kolom:[
            {field:'nama',       label:'Nama PKL', sort:true},
            {field:'fakultas',   label:'Fakultas', sembunyiMobile:true},
            {field:'prodi',      label:'Prodi', sembunyiMobile:true},
            {field:'tanggalMulai',label:'Tgl Mulai',lebar:'90px'},
            {field:'tanggalSelesai',label:'Tgl Selesai',lebar:'90px',sembunyiMobile:true},
            {field:'sks',        label:'SKS',lebar:'50px'}
        ],
        onUbah:function(d){bukaForm(d);},
        onHapus:function(id,cb){AIS2.Api.post('pendataan','pkl_hapus',{id:id}).then(function(r){cb(r&&r.ok,r&&r.msg);});},
        renderAksi:function(row){
            return '<div class="td-actions">'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="bukaForm('+JSON.stringify(row).replace(/\"/g,"'")+')"><i class="fa-solid fa-pen"></i></button>'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" title="Persyaratan"><i class="fa-solid fa-clipboard-list"></i></button>'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" title="Penilaian"><i class="fa-solid fa-star"></i></button>'
                +'</div>';
        }
    });
});
function terapkan(){tbl&&tbl.setFilter({nama:document.getElementById('f_nama').value,fakultas:document.getElementById('f_fak').value,prodi:document.getElementById('f_prodi').value,cari:document.getElementById('f_cari').value});}
function reset(){['f_nama','f_cari'].forEach(function(id){var el=document.getElementById(id);if(el)el.value='';});['f_fak','f_prodi'].forEach(function(id){var el=document.getElementById(id);if(el)el.selectedIndex=0;});terapkan();}
function pilihTab(k){
    document.querySelectorAll('#tabPkl .ais2-tab').forEach(function(t,i){t.classList.toggle('active',['program','persyaratan','penilaian','kelompok'][i]===k);});
    document.querySelectorAll('.ais2-tab-pane').forEach(function(p){p.classList.remove('active');});
    document.getElementById('tab_'+k).classList.add('active');
}
function muatFakultas(){
    AIS2.Api.get('pendataan','ref_fakultas',{}).then(function(res){
        if(!res||!res.data)return;
        var sel=document.getElementById('f_fak');
        res.data.forEach(function(f){var o=document.createElement('option');o.value=f.id;o.textContent=f.nama;sel.appendChild(o);});
    });
}
function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-section-title">Identitas Program PKL</div>'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field ais2-form-full"><label>Nama PKL <span class="req">*</span></label><input class="ais2-input" name="nama" value="'+esc(d.nama)+'" required></div>'
        +'<div class="ais2-field"><label>Tahun Akademik</label><select name="tahunAkademik" class="ais2-input"><option value="">--</option></select></div>'
        +'<div class="ais2-field"><label>Semester</label><select name="jenisSemester" class="ais2-input"><option value="">--</option><option value="1">Ganjil</option><option value="2">Genap</option><option value="3">Semester Pendek</option></select></div>'
        +'<div class="ais2-field"><label>SKS</label><input class="ais2-input" type="number" name="sks" value="'+esc(d.sks)+'"></div>'
        +'<div class="ais2-field"><label>Min. IPK</label><input class="ais2-input" type="number" step="0.01" name="minIpk" value="'+esc(d.minIpk)+'"></div>'
        +'<div class="ais2-field"><label>Min. SKS</label><input class="ais2-input" type="number" name="minSks" value="'+esc(d.minSks)+'"></div>'
        +'<div class="ais2-field"><label>Tanggal Mulai</label><input class="ais2-input" type="date" name="tanggalMulai" value="'+esc(d.tanggalMulai)+'"></div>'
        +'<div class="ais2-field"><label>Tanggal Selesai</label><input class="ais2-input" type="date" name="tanggalSelesai" value="'+esc(d.tanggalSelesai)+'"></div>'
        +'</div>'
        +'<div class="ais2-form-section-title" style="margin-top:16px">Lingkup</div>'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Fakultas</label><select name="fakultas" class="ais2-input"><option value="">Semua</option></select></div>'
        +'<div class="ais2-field"><label>Prodi</label><select name="prodi" class="ais2-input"><option value="">Semua</option></select></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:12px"><label>Keterangan</label><textarea class="ais2-input" name="keterangan" rows="3">'+esc(d.keterangan)+'</textarea></div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Program PKL':'Tambah Program PKL',ikon:'fa-hard-hat',ukuran:'',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.type==='checkbox'?(el.checked?'true':'false'):el.value;});
                if(!data.nama){AIS2.Toast.peringatan('Nama PKL wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'pkl_ubah':'pkl_tambah',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Tersimpan');AIS2.Modal.tutup();tbl&&tbl.refresh();}
                    else AIS2.Toast.error((r&&r.msg)||'Gagal');
                });
            }},
            {label:'Tutup',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}
document.addEventListener('keydown',function(e){if(e.key==='Enter'&&e.target.tagName==='INPUT')terapkan();});
</script>
</body></html>
