<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Catatan Mahasiswa</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">

<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-note-sticky"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Catatan Mahasiswa</span></div>
    <div class="ais2-ph-title">Catatan Mahasiswa</div>
    <div class="ais2-ph-sub">Rekam catatan konseling, PA, dan catatan pembinaan dosen</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Tambah Catatan</button>
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
    <div class="ais2-filter-field"><label>Mahasiswa</label><input type="text" id="f_mhs" placeholder="NIM / nama mahasiswa..."></div>
    <div class="ais2-filter-field"><label>Jenis Catatan</label>
      <select id="f_jenis">
        <option value="">Semua</option>
        <option>Konseling PA</option><option>Prestasi</option><option>Pelanggaran</option>
        <option>Saran</option><option>Catatan Akademik</option><option>Lainnya</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Dosen</label><input type="text" id="f_dosen" placeholder="Nama dosen pembina..."></div>
    <div class="ais2-filter-field"><label>Tahun Akademik</label><select id="f_ta"><option value="">Semua TA</option></select></div>
    <div class="ais2-filter-field"><label>Semester</label>
      <select id="f_smt"><option value="">Semua</option><option value="1">Ganjil</option><option value="2">Genap</option></select>
    </div>
  </div>
</div>

<div id="tabelCatatan"></div>

<script>
var tbl=null;
document.addEventListener('DOMContentLoaded',function(){
    muatTA();
    tbl=AIS2.Table.init({
        container:'#tabelCatatan', modul:'pendataan', svc:'catatan_mhs_list',
        bisa:{tambah:false,ubah:true,hapus:true},
        kolom:[
            {field:'nim',        label:'NIM', lebar:'110px'},
            {field:'nama',       label:'Nama Mahasiswa', sort:true},
            {field:'jenisCatatan',label:'Jenis', lebar:'120px'},
            {field:'isiCatatan', label:'Isi Catatan'},
            {field:'dosen',      label:'Dosen Pembina', sembunyiMobile:true},
            {field:'tahunAkademik',label:'TA', lebar:'80px', sembunyiMobile:true}
        ],
        onUbah:function(d){bukaForm(d);},
        onHapus:function(id,cb){AIS2.Api.post('pendataan','catatan_mhs_hapus',{id:id}).then(function(r){cb(r&&r.ok,r&&r.msg);});}
    });
});
function terapkan(){tbl&&tbl.setFilter({mhs:document.getElementById('f_mhs').value,jenis:document.getElementById('f_jenis').value,dosen:document.getElementById('f_dosen').value,ta:document.getElementById('f_ta').value,smt:document.getElementById('f_smt').value});}
function reset(){['f_mhs','f_dosen'].forEach(function(id){var el=document.getElementById(id);if(el)el.value='';});['f_jenis','f_ta','f_smt'].forEach(function(id){var el=document.getElementById(id);if(el)el.selectedIndex=0;});terapkan();}
function muatTA(){
    AIS2.Api.get('pendataan','ref_tahun_akademik',{}).then(function(res){
        if(!res||!res.data)return;
        var sel=document.getElementById('f_ta');
        res.data.forEach(function(t){var o=document.createElement('option');o.value=t.id;o.textContent=t.label;sel.appendChild(o);});
    });
}
function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-section-title">Catatan Mahasiswa</div>'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Mahasiswa <span class="req">*</span></label><input class="ais2-input" name="mahasiswaRef" value="'+esc(d.nimMahasiswa||d.namaMahasiswa)+'" required placeholder="NIM atau nama..."></div>'
        +'<div class="ais2-field"><label>Dosen Pembina</label><input class="ais2-input" name="dosenRef" value="'+esc(d.dosenNama)+'" placeholder="Nama/kode dosen..."></div>'
        +'<div class="ais2-field"><label>Jenis Catatan <span class="req">*</span></label><select name="jenisCatatan" class="ais2-input" required><option value="">-- Pilih --</option>'
        +['Konseling PA','Prestasi','Pelanggaran','Saran','Catatan Akademik','Lainnya'].map(function(j){return '<option'+(d.jenisCatatan===j?' selected':'')+'>'+j+'</option>';}).join('')
        +'</select></div>'
        +'<div class="ais2-field"><label>Tahun Akademik</label><select name="tahunAkademik" class="ais2-input"><option value="">--</option></select></div>'
        +'<div class="ais2-field"><label>Semester</label><select name="semester" class="ais2-input"><option value="">--</option><option value="1">Ganjil</option><option value="2">Genap</option></select></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:14px"><label>Isi Catatan <span class="req">*</span></label>'
        +'<textarea class="ais2-input" name="isiCatatan" rows="5" required>'+esc(d.isiCatatan)+'</textarea></div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Catatan':'Tambah Catatan Mahasiswa',ikon:'fa-note-sticky',ukuran:'',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.type==='checkbox'?(el.checked?'true':'false'):el.value;});
                if(!data.mahasiswaRef||!data.jenisCatatan||!data.isiCatatan){AIS2.Toast.peringatan('Mahasiswa, Jenis, dan Isi Catatan wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'catatan_mhs_ubah':'catatan_mhs_tambah',data).then(function(r){
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
