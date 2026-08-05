<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Kerjasama</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-handshake"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Kerjasama</span></div>
    <div class="ais2-ph-title">Kerjasama Antar Instansi</div>
    <div class="ais2-ph-sub">Kelola MoU, MoA, dan kerjasama institusi dengan pihak luar</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Tambah Kerjasama</button>
  </div>
</div>
<div class="ais2-filter-card">
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>Nama Instansi / Mitra</label><input type="text" id="f_mitra" placeholder="Cari mitra..."></div>
    <div class="ais2-filter-field"><label>Jenis Kerjasama</label>
      <select id="f_jenis"><option value="">Semua</option>
        <option>MoU</option><option>MoA</option><option>PKS</option><option>Kolaborasi Penelitian</option>
        <option>Student Exchange</option><option>Guest Lecture</option><option>Magang / PKL</option><option>Lainnya</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Ruang Lingkup</label>
      <select id="f_ruang"><option value="">Semua</option><option>Lokal</option><option>Nasional</option><option>Internasional</option></select>
    </div>
    <div class="ais2-filter-field"><label>Status</label>
      <select id="f_status"><option value="">Semua</option><option>Aktif</option><option>Berakhir</option><option>Diperbarui</option></select>
    </div>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" style="align-self:flex-end" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
  </div>
</div>
<div id="tabelKS"></div>
<script>
var tbl=null;
document.addEventListener('DOMContentLoaded',function(){
    tbl=AIS2.Table.init({
        container:'#tabelKS',modul:'pendataan',svc:'kerjasama_list',
        bisa:{tambah:false,ubah:true,hapus:true},
        kolom:[
            {field:'namaMitra',label:'Nama Mitra / Instansi',sort:true},
            {field:'jenis',label:'Jenis',lebar:'130px'},
            {field:'ruangLingkup',label:'Lingkup',lebar:'100px',sembunyiMobile:true},
            {field:'tanggalMulai',label:'Mulai',lebar:'90px',sembunyiMobile:true},
            {field:'tanggalBerakhir',label:'Berakhir',lebar:'90px',sembunyiMobile:true},
            {field:'status',label:'Status',lebar:'80px',render:function(v){
                var w={Aktif:'success',Berakhir:'danger',Diperbarui:'info'};
                return AIS2.Badge.kustom(v,w);
            }}
        ],
        onUbah:function(d){bukaForm(d);},
        onHapus:function(id,cb){AIS2.Api.post('pendataan','kerjasama_hapus',{id:id}).then(function(r){cb(r&&r.ok,r&&r.msg);});}
    });
});
function terapkan(){tbl&&tbl.setFilter({mitra:document.getElementById('f_mitra').value,jenis:document.getElementById('f_jenis').value,ruang:document.getElementById('f_ruang').value,status:document.getElementById('f_status').value});}
function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field ais2-form-full"><label>Nama Mitra / Instansi <span class="req">*</span></label><input class="ais2-input" name="namaMitra" value="'+esc(d.namaMitra)+'" required></div>'
        +'<div class="ais2-field"><label>Jenis Kerjasama <span class="req">*</span></label><select name="jenis" class="ais2-input" required><option value="">-- Pilih --</option>'
        +['MoU','MoA','PKS','Kolaborasi Penelitian','Student Exchange','Guest Lecture','Magang / PKL','Lainnya'].map(function(j){return '<option'+(d.jenis===j?' selected':'')+'>'+j+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Ruang Lingkup</label><select name="ruangLingkup" class="ais2-input"><option value="">--</option>'
        +['Lokal','Nasional','Internasional'].map(function(r){return '<option'+(d.ruangLingkup===r?' selected':'')+'>'+r+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Nomor MoU / Kontrak</label><input class="ais2-input" name="nomorKontrak" value="'+esc(d.nomorKontrak)+'"></div>'
        +'<div class="ais2-field"><label>Tanggal Mulai</label><input class="ais2-input" type="date" name="tanggalMulai" value="'+esc(d.tanggalMulai)+'"></div>'
        +'<div class="ais2-field"><label>Tanggal Berakhir</label><input class="ais2-input" type="date" name="tanggalBerakhir" value="'+esc(d.tanggalBerakhir)+'"></div>'
        +'<div class="ais2-field"><label>PIC Institusi</label><input class="ais2-input" name="picInstitusi" value="'+esc(d.picInstitusi)+'"></div>'
        +'<div class="ais2-field"><label>PIC Mitra</label><input class="ais2-input" name="picMitra" value="'+esc(d.picMitra)+'"></div>'
        +'<div class="ais2-field"><label>Status</label><select name="status" class="ais2-input">'
        +['Aktif','Berakhir','Diperbarui'].map(function(s){return '<option'+(d.status===s?' selected':'')+'>'+s+'</option>';}).join('')+'</select></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:12px"><label>Lingkup Kegiatan / Keterangan</label><textarea class="ais2-input" name="keterangan" rows="3">'+esc(d.keterangan)+'</textarea></div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Kerjasama':'Tambah Kerjasama',ikon:'fa-handshake',ukuran:'',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.value;});
                if(!data.namaMitra||!data.jenis){AIS2.Toast.peringatan('Nama Mitra dan Jenis wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'kerjasama_ubah':'kerjasama_tambah',data).then(function(r){
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
