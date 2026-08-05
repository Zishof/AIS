<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Jabatan</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">

<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-sitemap"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Jabatan</span></div>
    <div class="ais2-ph-title">Jabatan</div>
    <div class="ais2-ph-sub">Kelola struktur jabatan dan ekuivalensi SKS</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Tambah Jabatan</button>
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
    <div class="ais2-filter-field"><label>Nama Jabatan</label><input type="text" id="f_nama" placeholder="Cari nama jabatan..."></div>
    <div class="ais2-filter-field" style="max-width:160px">
      <label>Status</label>
      <select id="f_aktif">
        <option value="">Semua</option>
        <option value="1">Aktif</option>
        <option value="0">Non-aktif</option>
      </select>
    </div>
  </div>
</div>

<div id="tabelJabatan"></div>

<script>
var tbl=null;
document.addEventListener('DOMContentLoaded',function(){
    tbl=AIS2.Table.init({
        container:'#tabelJabatan', modul:'pendataan', svc:'jabatan_list',
        bisa:{tambah:false,ubah:true,hapus:true},
        ukuranHalaman:30,
        kolom:[
            {field:'nama',         label:'Nama Jabatan', sort:true},
            {field:'keterangan',   label:'Keterangan'},
            {field:'ekuivalensiSks',label:'Ekuivalensi SKS',lebar:'110px'},
            {field:'jabatanDiPtSendiri',label:'Jabatan PT Sendiri',lebar:'130px',
             render:function(v){return AIS2.Badge.yaTidak(v);}, sembunyiMobile:true},
            {field:'aktif', label:'Aktif', lebar:'65px',
             render:function(v){return AIS2.Badge.aktif(v);}}
        ],
        onUbah:function(d){bukaForm(d);},
        onHapus:function(id,cb){AIS2.Api.post('pendataan','jabatan_hapus',{id:id}).then(function(r){cb(r&&r.ok,r&&r.msg);});}
    });
});
function terapkan(){tbl&&tbl.setFilter({nama:document.getElementById('f_nama').value,aktif:document.getElementById('f_aktif').value});}
function reset(){document.getElementById('f_nama').value='';document.getElementById('f_aktif').selectedIndex=0;terapkan();}
function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-section-title">Identitas Jabatan</div>'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field ais2-form-full"><label>Nama Jabatan <span class="req">*</span></label><input class="ais2-input" name="nama" value="'+esc(d.nama)+'" required></div>'
        +'<div class="ais2-field"><label>Ekuivalensi SKS</label><input class="ais2-input" type="number" step="0.5" name="ekuivalensiSks" value="'+esc(d.ekuivalensiSks)+'"></div>'
        +'<div class="ais2-field ais2-field-check"><input type="checkbox" name="jabatanDiPtSendiri" '+(d.jabatanDiPtSendiri?'checked':'')+'><label>Jabatan di PT Sendiri</label></div>'
        +'<div class="ais2-field ais2-field-check"><input type="checkbox" name="aktif" '+(d.aktif!==false?'checked':'')+'><label>Aktif</label></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:12px"><label>Keterangan</label>'
        +'<textarea class="ais2-input" name="keterangan" rows="3">'+esc(d.keterangan)+'</textarea></div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Jabatan':'Tambah Jabatan',ikon:'fa-sitemap',ukuran:'sm',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.type==='checkbox'?(el.checked?'true':'false'):el.value;});
                if(!data.nama){AIS2.Toast.peringatan('Nama jabatan wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'jabatan_ubah':'jabatan_tambah',data).then(function(r){
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
