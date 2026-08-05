<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Data Staf</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-users-gear"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Data Staf</span></div>
    <div class="ais2-ph-title">Data Staf Administrasi</div>
    <div class="ais2-ph-sub">Kelola data staf tenaga kependidikan dan administrasi institusi</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-outline" onclick="ekspor()"><i class="fa-solid fa-file-excel"></i> Ekspor</button>
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Tambah Staf</button>
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
    <div class="ais2-filter-field"><label>NIP / Kode</label><input type="text" id="f_nip" placeholder="NIP atau kode..."></div>
    <div class="ais2-filter-field"><label>Nama</label><input type="text" id="f_nama" placeholder="Nama staf..."></div>
    <div class="ais2-filter-field"><label>Unit Kerja</label><input type="text" id="f_unit" placeholder="Unit/divisi..."></div>
    <div class="ais2-filter-field"><label>Status</label>
      <select id="f_status"><option value="">Semua</option><option value="1">Aktif</option><option value="0">Nonaktif</option></select>
    </div>
  </div>
  <div class="ais2-filter-advanced" id="filterLanjut">
    <div class="ais2-filter-row">
      <div class="ais2-filter-field"><label>Jabatan</label><input type="text" id="f_jabatan" placeholder="Jabatan / fungsi..."></div>
      <div class="ais2-filter-field"><label>Golongan</label><input type="text" id="f_gol" placeholder="Gol. kepangkatan..."></div>
      <div class="ais2-filter-field"><label>Pendidikan Terakhir</label>
        <select id="f_pend"><option value="">Semua</option><option>SMA/SMK</option><option>D3</option><option>S1</option><option>S2</option><option>S3</option></select>
      </div>
    </div>
    <div class="ais2-filter-checks">
      <label class="ais2-filter-check"><input type="checkbox" id="fc_tetap"> Tetap saja</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_kontrak"> Kontrak saja</label>
    </div>
  </div>
</div>
<div id="tabelStaf"></div>
<script>
var tbl=null;
document.addEventListener('DOMContentLoaded',function(){
    tbl=AIS2.Table.init({
        container:'#tabelStaf',modul:'pendataan',svc:'staff_list',
        bisa:{tambah:false,ubah:true,hapus:false},
        kolom:[
            {field:'nip',label:'NIP / Kode',lebar:'120px',sort:true},
            {field:'nama',label:'Nama Staf',sort:true},
            {field:'jabatan',label:'Jabatan',sembunyiMobile:true},
            {field:'unitKerja',label:'Unit Kerja',sembunyiMobile:true},
            {field:'jenisStaf',label:'Jenis',lebar:'90px'},
            {field:'telepon',label:'Telepon',lebar:'120px',sembunyiMobile:true},
            {field:'aktif',label:'Status',lebar:'80px',render:function(v){return AIS2.Badge.aktif(v);}}
        ],
        onUbah:function(d){bukaForm(d);}
    });
});
function terapkan(){tbl&&tbl.setFilter({nip:document.getElementById('f_nip').value,nama:document.getElementById('f_nama').value,unit:document.getElementById('f_unit').value,status:document.getElementById('f_status').value,jabatan:document.getElementById('f_jabatan').value,gol:document.getElementById('f_gol').value,pend:document.getElementById('f_pend').value,tetap:document.getElementById('fc_tetap').checked?'1':'',kontrak:document.getElementById('fc_kontrak').checked?'1':''});}
function reset(){['f_nip','f_nama','f_unit','f_jabatan','f_gol'].forEach(function(id){var el=document.getElementById(id);if(el)el.value='';});['f_status','f_pend'].forEach(function(id){var el=document.getElementById(id);if(el)el.selectedIndex=0;});['fc_tetap','fc_kontrak'].forEach(function(id){var el=document.getElementById(id);if(el)el.checked=false;});terapkan();}
function toggleLanjut(){document.getElementById('filterLanjut').classList.toggle('show');}
function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var chk=function(v){return v?'checked':''};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-section-title">Data Identitas Staf</div>'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>NIP / Kode <span class="req">*</span></label><input class="ais2-input" name="nip" value="'+esc(d.nip)+'" required></div>'
        +'<div class="ais2-field"><label>Nama Lengkap <span class="req">*</span></label><input class="ais2-input" name="nama" value="'+esc(d.nama)+'" required></div>'
        +'<div class="ais2-field"><label>Jenis Kelamin</label><select name="jenisKelamin" class="ais2-input"><option value="">--</option><option value="L"'+(d.jenisKelamin==='L'?' selected':'')+'>Laki-laki</option><option value="P"'+(d.jenisKelamin==='P'?' selected':'')+'>Perempuan</option></select></div>'
        +'<div class="ais2-field"><label>Jenis Staf</label><select name="jenisStaf" class="ais2-input"><option value="">--</option>'
        +['Tenaga Tetap','Kontrak','Honorer','Magang'].map(function(j){return '<option'+(d.jenisStaf===j?' selected':'')+'>'+j+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Unit Kerja</label><input class="ais2-input" name="unitKerja" value="'+esc(d.unitKerja)+'"></div>'
        +'<div class="ais2-field"><label>Jabatan</label><input class="ais2-input" name="jabatan" value="'+esc(d.jabatan)+'"></div>'
        +'<div class="ais2-field"><label>Golongan</label><input class="ais2-input" name="golongan" value="'+esc(d.golongan)+'"></div>'
        +'<div class="ais2-field"><label>Pendidikan Terakhir</label><select name="pendidikanTerakhir" class="ais2-input"><option value="">--</option>'
        +['SMA/SMK','D3','D4','S1','S2','S3'].map(function(p){return '<option'+(d.pendidikanTerakhir===p?' selected':'')+'>'+p+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Tanggal Masuk</label><input class="ais2-input" type="date" name="tanggalMasuk" value="'+esc(d.tanggalMasuk)+'"></div>'
        +'<div class="ais2-field"><label>Telepon / HP</label><input class="ais2-input" name="telepon" value="'+esc(d.telepon)+'"></div>'
        +'<div class="ais2-field"><label>Email</label><input class="ais2-input" type="email" name="email" value="'+esc(d.email)+'"></div>'
        +'</div>'
        +'<div style="margin-top:12px;display:flex;gap:16px"><label class="ais2-switch"><input type="checkbox" name="aktif" '+chk(d.aktif!==false)+' value="true"><span>Aktif</span></label></div>'
        +'<div class="ais2-field" style="margin-top:12px"><label>Keterangan</label><textarea class="ais2-input" name="keterangan" rows="2">'+esc(d.keterangan)+'</textarea></div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Data Staf':'Tambah Staf',ikon:'fa-users-gear',ukuran:'',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.type==='checkbox'?(el.checked?'true':'false'):el.value;});
                if(!data.nip||!data.nama){AIS2.Toast.peringatan('NIP dan Nama wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'staff_ubah':'staff_tambah',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Tersimpan');AIS2.Modal.tutup();tbl&&tbl.refresh();}
                    else AIS2.Toast.error((r&&r.msg)||'Gagal');
                });
            }},
            {label:'Tutup',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}
function ekspor(){window.open(ROOT+'/baru2?modul=pendataan&svc=staff_ekspor','_blank');}
document.addEventListener('keydown',function(e){if(e.key==='Enter'&&e.target.tagName==='INPUT')terapkan();});
</script>
</body></html>
