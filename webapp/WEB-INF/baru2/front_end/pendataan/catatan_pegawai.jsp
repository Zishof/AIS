<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Catatan Pegawai</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-clipboard-user"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Catatan Pegawai</span></div>
    <div class="ais2-ph-title">Catatan Kinerja &amp; Pegawai</div>
    <div class="ais2-ph-sub">Rekam catatan evaluasi, pelanggaran, dan penghargaan pegawai</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Tambah Catatan</button>
  </div>
</div>
<div class="ais2-filter-card">
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>Pegawai</label><input type="text" id="f_pgw" placeholder="NIP / nama pegawai..."></div>
    <div class="ais2-filter-field"><label>Jenis Catatan</label>
      <select id="f_jenis"><option value="">Semua</option>
        <option>Evaluasi Kinerja</option><option>Pelanggaran Disiplin</option><option>Penghargaan</option>
        <option>Surat Peringatan</option><option>Pengembangan Diri</option><option>Lainnya</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Tahun</label><input type="number" id="f_tahun" placeholder="2024" style="width:90px"></div>
    <div class="ais2-filter-field"><label>Unit Kerja</label><input type="text" id="f_unit" placeholder="Unit/divisi..."></div>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" style="align-self:flex-end" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
  </div>
</div>
<div id="tabelCP"></div>
<script>
var tbl=null;
document.addEventListener('DOMContentLoaded',function(){
    tbl=AIS2.Table.init({
        container:'#tabelCP',modul:'pendataan',svc:'catatan_pegawai_list',
        bisa:{tambah:false,ubah:true,hapus:true},
        kolom:[
            {field:'nipPegawai',label:'NIP',lebar:'110px'},
            {field:'namaPegawai',label:'Nama Pegawai',sort:true},
            {field:'jenisCatatan',label:'Jenis',lebar:'150px'},
            {field:'isiCatatan',label:'Isi Catatan'},
            {field:'tanggal',label:'Tanggal',lebar:'100px',sembunyiMobile:true},
            {field:'dicatatOleh',label:'Dicatat Oleh',lebar:'130px',sembunyiMobile:true}
        ],
        onUbah:function(d){bukaForm(d);},
        onHapus:function(id,cb){AIS2.Api.post('pendataan','catatan_pegawai_hapus',{id:id}).then(function(r){cb(r&&r.ok,r&&r.msg);});}
    });
});
function terapkan(){tbl&&tbl.setFilter({pgw:document.getElementById('f_pgw').value,jenis:document.getElementById('f_jenis').value,tahun:document.getElementById('f_tahun').value,unit:document.getElementById('f_unit').value});}
function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Pegawai <span class="req">*</span></label><input class="ais2-input" name="pegawaiRef" value="'+esc(d.nipPegawai||d.namaPegawai)+'" required placeholder="NIP atau nama..."></div>'
        +'<div class="ais2-field"><label>Jenis Catatan <span class="req">*</span></label><select name="jenisCatatan" class="ais2-input" required><option value="">-- Pilih --</option>'
        +['Evaluasi Kinerja','Pelanggaran Disiplin','Penghargaan','Surat Peringatan','Pengembangan Diri','Lainnya'].map(function(j){return '<option'+(d.jenisCatatan===j?' selected':'')+'>'+j+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Tanggal</label><input class="ais2-input" type="date" name="tanggal" value="'+esc(d.tanggal)+'"></div>'
        +'<div class="ais2-field"><label>Perihal / Judul</label><input class="ais2-input" name="perihal" value="'+esc(d.perihal)+'"></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:12px"><label>Isi Catatan <span class="req">*</span></label>'
        +'<textarea class="ais2-input" name="isiCatatan" rows="5" required>'+esc(d.isiCatatan)+'</textarea></div>'
        +'<div class="ais2-field" style="margin-top:8px"><label>Tindak Lanjut</label><textarea class="ais2-input" name="tindakLanjut" rows="2">'+esc(d.tindakLanjut)+'</textarea></div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Catatan Pegawai':'Tambah Catatan Pegawai',ikon:'fa-clipboard-user',ukuran:'',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.value;});
                if(!data.pegawaiRef||!data.jenisCatatan||!data.isiCatatan){AIS2.Toast.peringatan('Pegawai, Jenis, dan Isi wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'catatan_pegawai_ubah':'catatan_pegawai_tambah',data).then(function(r){
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
