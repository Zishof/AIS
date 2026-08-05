<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Pegawai</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">

<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-user-tie"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Pegawai</span></div>
    <div class="ais2-ph-title">Data Pegawai</div>
    <div class="ais2-ph-sub">Kelola data tenaga kependidikan dan staf administrasi</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-outline" onclick="ekspor()"><i class="fa-solid fa-file-excel"></i> Ekspor</button>
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Tambah Pegawai</button>
  </div>
</div>

<div class="ais2-filter-card">
  <div class="ais2-filter-head">
    <i class="fa-solid fa-filter" style="color:var(--ais2-pri2)"></i>
    <span class="ais2-filter-title">Filter</span>
    <button class="ais2-btn ais2-btn-sm ais2-btn-outline" onclick="toggleLanjut()"><i class="fa-solid fa-sliders"></i> Lanjutan</button>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
    <button class="ais2-btn ais2-btn-sm ais2-btn-outline" onclick="reset()"><i class="fa-solid fa-rotate-left"></i></button>
  </div>
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>Kode / NIP</label><input type="text" id="f_kode" placeholder="Cari kode/NIP..."></div>
    <div class="ais2-filter-field"><label>Nama Pegawai</label><input type="text" id="f_nama" placeholder="Cari nama..."></div>
    <div class="ais2-filter-field"><label>Status</label>
      <select id="f_status">
        <option value="">Semua</option>
        <option value="AKTIF">Aktif</option>
        <option value="TIDAK_AKTIF">Tidak Aktif</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Satuan Kerja</label><input type="text" id="f_satuanKerja" placeholder="Nama satuan kerja..."></div>
  </div>
  <div class="ais2-filter-advanced" id="filterLanjut">
    <div class="ais2-filter-checks">
      <label class="ais2-filter-check"><input type="checkbox" id="fc_bukanDosen"> Pegawai bukan dosen/guru</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_pernahAbsen"> Pernah absen</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_belumAbsen"> Belum pernah absen</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_aktif" checked> Tampilkan aktif</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_nonaktif"> Tampilkan non-aktif</label>
    </div>
  </div>
</div>

<div id="tabelPegawai"></div>

<script>
var tbl=null;
document.addEventListener('DOMContentLoaded',function(){
    tbl=AIS2.Table.init({
        container:'#tabelPegawai', modul:'pendataan', svc:'pegawai_list',
        bisa:{tambah:false,ubah:true,hapus:false},
        sortDefault:'nama',
        kolom:[
            {field:'kode',      label:'NIP / Kode',  lebar:'120px', sort:true},
            {field:'nama',      label:'Nama Pegawai', sort:true},
            {field:'masaKerja', label:'Masa Kerja',   lebar:'90px', sembunyiMobile:true},
            {field:'tmt',       label:'TMT',          lebar:'90px', sembunyiMobile:true},
            {field:'tipePegawai',label:'Tipe',        lebar:'100px', sembunyiMobile:true},
            {field:'satuanKerja',label:'Satuan Kerja', sembunyiMobile:true},
            {field:'atasan',    label:'Atasan',       sembunyiMobile:true},
            {field:'aktif',     label:'Aktif', lebar:'65px',
             render:function(v){return AIS2.Badge.aktif(v);}}
        ],
        onUbah:function(d){bukaFormById(d.id);},
        renderAksi:function(row){
            return '<div class="td-actions">'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="bukaFormById(\''+row.id+'\')"><i class="fa-solid fa-pen"></i></button>'
                +'</div>';
        }
    });
});
function terapkan(){
    tbl&&tbl.setFilter({
        kode:document.getElementById('f_kode').value,
        nama:document.getElementById('f_nama').value,
        status:document.getElementById('f_status').value,
        satuanKerja:document.getElementById('f_satuanKerja').value,
        bukanDosen:document.getElementById('fc_bukanDosen').checked?'1':'',
        pernahAbsen:document.getElementById('fc_pernahAbsen').checked?'1':'',
        hanyaAktif:document.getElementById('fc_aktif').checked?'1':'',
        termasukNonaktif:document.getElementById('fc_nonaktif').checked?'1':''
    });
}
function reset(){
    ['f_kode','f_nama','f_satuanKerja'].forEach(function(id){var el=document.getElementById(id);if(el)el.value='';});
    document.getElementById('f_status').selectedIndex=0;
    document.getElementById('fc_aktif').checked=true;
    document.getElementById('fc_bukanDosen').checked=false;
    document.getElementById('fc_pernahAbsen').checked=false;
    document.getElementById('fc_nonaktif').checked=false;
    terapkan();
}
function toggleLanjut(){document.getElementById('filterLanjut').classList.toggle('show');}
function bukaTambah(){bukaFormById(null);}
function bukaFormById(id){
    if(!id){bukaForm({});return;}
    AIS2.Api.get('pendataan','pegawai_detail',{id:id}).then(function(r){bukaForm(r&&r.data?r.data:{});});
}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var fld=function(lbl,nm,tp){return '<div class="ais2-field"><label>'+lbl+'</label><input class="ais2-input" type="'+(tp||'text')+'" name="'+nm+'" value="'+esc(d[nm])+'"></div>';};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-section-title">Identitas Pegawai</div>'
        +'<div class="ais2-form-grid">'
        +fld('NIP / Kode <span class="req">*</span>','kode')
        +fld('Nama Lengkap <span class="req">*</span>','nama')
        +'<div class="ais2-field"><label>Tipe Pegawai</label><select name="tipePegawai" class="ais2-input"><option value="">--</option>'
        +['Tetap','Tidak Tetap','PPPK','PNS','Honorer','Magang'].map(function(t){return '<option'+(d.tipePegawai===t?' selected':'')+'>'+t+'</option>';}).join('')
        +'</select></div>'
        +'<div class="ais2-field"><label>Satuan Kerja / Unit</label><input class="ais2-input" name="satuanKerja" value="'+esc(d.satuanKerja)+'"></div>'
        +'<div class="ais2-field"><label>Atasan Langsung</label><input class="ais2-input" name="atasanRef" value="'+esc(d.atasanNama)+'" placeholder="Nama/kode atasan..."></div>'
        +fld('TMT (Mulai Kerja)','tmt','date')
        +'</div>'
        +'<div class="ais2-form-section-title" style="margin-top:20px">Kontak</div>'
        +'<div class="ais2-form-grid">'
        +fld('No. HP','hp')+fld('Email','email','email')
        +'</div>'
        +'<div class="ais2-field ais2-field-check" style="margin-top:12px"><input type="checkbox" name="aktif" '+(d.aktif!==false?'checked':'')+'><label>Aktif</label></div>'
        +'<div class="ais2-field" style="margin-top:10px"><label>Keterangan</label><textarea class="ais2-input" name="keterangan" rows="2">'+esc(d.keterangan)+'</textarea></div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Pegawai':'Tambah Pegawai',ikon:'fa-user-tie',ukuran:'',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.type==='checkbox'?(el.checked?'true':'false'):el.value;});
                if(!data.kode||!data.nama){AIS2.Toast.peringatan('Kode dan Nama wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'pegawai_ubah':'pegawai_tambah',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Tersimpan');AIS2.Modal.tutup();tbl&&tbl.refresh();}
                    else AIS2.Toast.error((r&&r.msg)||'Gagal');
                });
            }},
            {label:'Tutup',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}
function ekspor(){window.open(ROOT+'/baru2?modul=pendataan&svc=pegawai_ekspor','_blank');}
document.addEventListener('keydown',function(e){if(e.key==='Enter'&&e.target.tagName==='INPUT')terapkan();});
</script>
</body></html>
