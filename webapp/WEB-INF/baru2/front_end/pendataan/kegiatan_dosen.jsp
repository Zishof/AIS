<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Kegiatan Dosen</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-person-chalkboard"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Kegiatan Dosen</span></div>
    <div class="ais2-ph-title">Kegiatan Dosen (Tri Dharma &amp; Penelitian)</div>
    <div class="ais2-ph-sub">Kelola kegiatan pendidikan, penelitian, dan pengabdian masyarakat dosen</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Tambah Kegiatan</button>
  </div>
</div>

<!-- Tabs -->
<div class="ais2-tab-bar">
  <button class="ais2-tab-btn active" id="tab_pendidikan" onclick="gTab('pendidikan')">Pengajaran &amp; Pendidikan</button>
  <button class="ais2-tab-btn" id="tab_penelitian" onclick="gTab('penelitian')">Penelitian</button>
  <button class="ais2-tab-btn" id="tab_abdimas" onclick="gTab('abdimas')">Pengabdian Masyarakat</button>
</div>

<div class="ais2-filter-card">
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>Dosen</label><input type="text" id="f_dosen" placeholder="NIP / nama dosen..."></div>
    <div class="ais2-filter-field"><label>Judul / Nama Kegiatan</label><input type="text" id="f_judul" placeholder="Cari kegiatan..."></div>
    <div class="ais2-filter-field"><label>Tahun</label><input type="number" id="f_tahun" placeholder="2024" style="width:90px"></div>
    <div class="ais2-filter-field"><label>Status</label>
      <select id="f_status"><option value="">Semua</option><option>Selesai</option><option>Berjalan</option><option>Diajukan</option></select>
    </div>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" style="align-self:flex-end" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
  </div>
</div>

<div id="tabelKD"></div>

<script>
var tbl=null, tabAktif='pendidikan';
document.addEventListener('DOMContentLoaded',function(){
    tbl=AIS2.Table.init({
        container:'#tabelKD',modul:'pendataan',svc:'kegiatan_dosen_list',
        bisa:{tambah:false,ubah:true,hapus:true},
        kolom:[
            {field:'namaDosen',label:'Nama Dosen',sort:true},
            {field:'judulKegiatan',label:'Judul / Nama Kegiatan'},
            {field:'kategori',label:'Kategori',lebar:'120px',sembunyiMobile:true},
            {field:'tahun',label:'Tahun',lebar:'70px'},
            {field:'tingkat',label:'Tingkat',lebar:'90px',sembunyiMobile:true},
            {field:'status',label:'Status',lebar:'90px',render:function(v){
                var warna={Selesai:'success',Berjalan:'info',Diajukan:'warning'};
                return AIS2.Badge.kustom(v,warna);
            }}
        ],
        onUbah:function(d){bukaForm(d);},
        onHapus:function(id,cb){AIS2.Api.post('pendataan','kegiatan_dosen_hapus',{id:id}).then(function(r){cb(r&&r.ok,r&&r.msg);});}
    });
});
function gTab(t){
    tabAktif=t;
    ['pendidikan','penelitian','abdimas'].forEach(function(k){document.getElementById('tab_'+k).classList.toggle('active',k===t);});
    tbl&&tbl.setFilter({tab:t,dosen:document.getElementById('f_dosen').value,judul:document.getElementById('f_judul').value,tahun:document.getElementById('f_tahun').value,status:document.getElementById('f_status').value});
}
function terapkan(){tbl&&tbl.setFilter({tab:tabAktif,dosen:document.getElementById('f_dosen').value,judul:document.getElementById('f_judul').value,tahun:document.getElementById('f_tahun').value,status:document.getElementById('f_status').value});}
function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Dosen <span class="req">*</span></label><input class="ais2-input" name="dosenRef" value="'+esc(d.namaDosen)+'" required placeholder="NIP atau nama..."></div>'
        +'<div class="ais2-field"><label>Kategori <span class="req">*</span></label><select name="kategori" class="ais2-input" required><option value="">-- Pilih --</option>'
        +['Pengajaran','Pembimbingan TA','Seminar / Konferensi','Penelitian Mandiri','Penelitian Hibah','Pengabdian Masyarakat','Karya Ilmiah','Buku / Modul','Paten','Lainnya'].map(function(k){return '<option'+(d.kategori===k?' selected':'')+'>'+k+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field ais2-form-full"><label>Judul / Nama Kegiatan <span class="req">*</span></label><input class="ais2-input" name="judulKegiatan" value="'+esc(d.judulKegiatan)+'" required></div>'
        +'<div class="ais2-field"><label>Tahun</label><input class="ais2-input" type="number" name="tahun" value="'+esc(d.tahun)+'"></div>'
        +'<div class="ais2-field"><label>Tingkat</label><select name="tingkat" class="ais2-input"><option value="">--</option>'
        +['Lokal','Regional','Nasional','Internasional'].map(function(t){return '<option'+(d.tingkat===t?' selected':'')+'>'+t+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Tanggal Mulai</label><input class="ais2-input" type="date" name="tanggalMulai" value="'+esc(d.tanggalMulai)+'"></div>'
        +'<div class="ais2-field"><label>Tanggal Selesai</label><input class="ais2-input" type="date" name="tanggalSelesai" value="'+esc(d.tanggalSelesai)+'"></div>'
        +'<div class="ais2-field"><label>Sumber Dana / Sponsor</label><input class="ais2-input" name="sumberDana" value="'+esc(d.sumberDana)+'"></div>'
        +'<div class="ais2-field"><label>Nominal Dana (Rp)</label><input class="ais2-input" type="number" name="nominal" value="'+esc(d.nominal)+'"></div>'
        +'<div class="ais2-field"><label>Status</label><select name="status" class="ais2-input"><option value="">--</option>'
        +['Diajukan','Berjalan','Selesai','Dibatalkan'].map(function(s){return '<option'+(d.status===s?' selected':'')+'>'+s+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Nomor SK / Kontrak</label><input class="ais2-input" name="nomorSK" value="'+esc(d.nomorSK)+'"></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:12px"><label>Keterangan / Abstrak</label><textarea class="ais2-input" name="keterangan" rows="3">'+esc(d.keterangan)+'</textarea></div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Kegiatan Dosen':'Tambah Kegiatan Dosen',ikon:'fa-person-chalkboard',ukuran:'',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.value;});
                if(!data.dosenRef||!data.kategori||!data.judulKegiatan){AIS2.Toast.peringatan('Dosen, Kategori, dan Judul wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'kegiatan_dosen_ubah':'kegiatan_dosen_tambah',data).then(function(r){
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
