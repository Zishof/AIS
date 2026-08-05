<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Kurikulum</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">

<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-sitemap"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Kurikulum</span></div>
    <div class="ais2-ph-title">Kurikulum</div>
    <div class="ais2-ph-sub">Kelola struktur kurikulum per program studi dan angkatan</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Tambah Kurikulum</button>
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
    <div class="ais2-filter-field"><label>Nama Kurikulum</label><input type="text" id="f_nama" placeholder="Cari nama..."></div>
    <div class="ais2-filter-field"><label>Matakuliah (cari MK dalam kurikulum)</label><input type="text" id="f_mk" placeholder="Kode/nama MK..."></div>
    <div class="ais2-filter-field"><label>Fakultas</label>
      <select id="f_fak" onchange="muatProdi()"><option value="">Semua</option></select>
    </div>
    <div class="ais2-filter-field"><label>Program Studi</label>
      <select id="f_prodi"><option value="">Semua</option></select>
    </div>
    <div class="ais2-filter-field"><label>Jenjang</label>
      <select id="f_jenjang"><option value="">Semua</option><option>D3</option><option>D4</option><option>S1</option><option>S2</option><option>S3</option></select>
    </div>
    <div class="ais2-filter-field"><label>Tahun Mulai</label>
      <input type="number" id="f_tahun" placeholder="2022" style="width:90px"></div>
  </div>
  <div style="margin-top:10px">
    <label class="ais2-filter-check"><input type="checkbox" id="fc_aktif" checked> Aktif saja</label>
  </div>
</div>

<div id="tabelKurikulum"></div>

<script>
var tbl = null;
document.addEventListener('DOMContentLoaded', function() {
    muatFakultas();
    tbl = AIS2.Table.init({
        container: '#tabelKurikulum',
        modul: 'pendataan', svc: 'kurikulum_list',
        bisa: { tambah: false, ubah: true, hapus: true },
        kolom: [
            { field:'tahun',     label:'Tahun', lebar:'70px', sort:true },
            { field:'nama',      label:'Nama Kurikulum', sort:true },
            { field:'prodi',     label:'Prodi', sembunyiMobile:true },
            { field:'jenjang',   label:'Jenjang', lebar:'70px', sembunyiMobile:true },
            { field:'program',   label:'Program', sembunyiMobile:true },
            { field:'realisasi', label:'Realisasi', lebar:'90px' },
            { field:'obe',       label:'OBE', lebar:'55px',
              render: function(v) { return AIS2.Badge.yaTidak(v); }, sembunyiMobile:true },
            { field:'aktif',     label:'Aktif', lebar:'60px',
              render: function(v) { return AIS2.Badge.aktif(v); } }
        ],
        onUbah: function(d) { bukaForm(d); },
        onHapus: function(id,cb) { AIS2.Api.post('pendataan','kurikulum_hapus',{id:id}).then(function(r){cb(r&&r.ok,r&&r.msg);}); }
    });
});
function terapkan() {
    tbl && tbl.setFilter({
        nama:document.getElementById('f_nama').value,
        mk:document.getElementById('f_mk').value,
        fakultas:document.getElementById('f_fak').value,
        prodi:document.getElementById('f_prodi').value,
        jenjang:document.getElementById('f_jenjang').value,
        tahun:document.getElementById('f_tahun').value,
        hanyaAktif:document.getElementById('fc_aktif').checked?'1':''
    });
}
function reset() {
    ['f_nama','f_mk','f_tahun'].forEach(function(id){var el=document.getElementById(id);if(el)el.value='';});
    ['f_fak','f_prodi','f_jenjang'].forEach(function(id){var el=document.getElementById(id);if(el)el.selectedIndex=0;});
    document.getElementById('fc_aktif').checked=true;
    terapkan();
}
function muatFakultas() {
    AIS2.Api.get('pendataan','ref_fakultas',{}).then(function(res) {
        if(!res||!res.data)return;
        var sel=document.getElementById('f_fak');
        res.data.forEach(function(f){var o=document.createElement('option');o.value=f.id;o.textContent=f.nama;sel.appendChild(o);});
    });
}
function muatProdi() {
    var fak=document.getElementById('f_fak').value;
    var sel=document.getElementById('f_prodi');
    sel.innerHTML='<option value="">Semua</option>';
    if(!fak)return;
    AIS2.Api.get('pendataan','ref_prodi',{fakultas:fak}).then(function(res){
        if(!res||!res.data)return;
        res.data.forEach(function(p){var o=document.createElement('option');o.value=p.id;o.textContent=p.nama;sel.appendChild(o);});
    });
}
function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-section-title">Identitas Kurikulum</div>'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Nama Kurikulum <span class="req">*</span></label><input class="ais2-input" name="nama" value="'+esc(d.nama)+'" required></div>'
        +'<div class="ais2-field"><label>Tahun</label><input class="ais2-input" type="number" name="tahun" value="'+esc(d.tahun)+'"></div>'
        +'<div class="ais2-field"><label>Mulai Berlaku</label><input class="ais2-input" type="date" name="mulai" value="'+esc(d.mulai)+'"></div>'
        +'<div class="ais2-field"><label>Angkatan</label><input class="ais2-input" type="number" name="angkatan" value="'+esc(d.angkatan)+'"></div>'
        +'<div class="ais2-field"><label>Jenjang</label><select name="jenjang" class="ais2-input"><option value="">--</option>'
        +['D3','D4','S1','S2','S3','Profesi'].map(function(j){return '<option'+(d.jenjang===j?' selected':'')+'>'+j+'</option>';}).join('')
        +'</select></div>'
        +'<div class="ais2-field"><label>Program</label><select name="program" class="ais2-input"><option value="">--</option>'
        +['Reguler','Karyawan','RPL'].map(function(j){return '<option'+(d.program===j?' selected':'')+'>'+j+'</option>';}).join('')
        +'</select></div>'
        +'<div class="ais2-field ais2-field-check"><input type="checkbox" name="obe" '+(d.obe?'checked':'')+'><label>Kurikulum OBE</label></div>'
        +'<div class="ais2-field ais2-field-check"><input type="checkbox" name="aktif" '+(d.aktif!==false?'checked':'')+'><label>Aktif</label></div>'
        +'</div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Kurikulum':'Tambah Kurikulum',ikon:'fa-sitemap',ukuran:'',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.type==='checkbox'?(el.checked?'true':'false'):el.value;});
                if(!data.nama){AIS2.Toast.peringatan('Nama wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'kurikulum_ubah':'kurikulum_tambah',data).then(function(r){
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
