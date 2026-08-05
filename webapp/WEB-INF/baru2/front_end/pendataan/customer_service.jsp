<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Customer Service</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-headset"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Customer Service</span></div>
    <div class="ais2-ph-title">Customer Service / Tiket Layanan</div>
    <div class="ais2-ph-sub">Kelola tiket dukungan, pertanyaan, dan permintaan layanan dari pengguna</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Tiket Baru</button>
  </div>
</div>

<!-- Stats mini cards -->
<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(160px,1fr));gap:12px;margin-bottom:16px">
  <div class="ais2-stat-card" id="statBaru"><div class="ais2-stat-val" id="valBaru">—</div><div class="ais2-stat-lbl">Tiket Baru</div></div>
  <div class="ais2-stat-card" id="statProses"><div class="ais2-stat-val" id="valProses">—</div><div class="ais2-stat-lbl">Diproses</div></div>
  <div class="ais2-stat-card" id="statSelesai"><div class="ais2-stat-val" id="valSelesai">—</div><div class="ais2-stat-lbl">Selesai Hari Ini</div></div>
  <div class="ais2-stat-card" id="statRataWaktu"><div class="ais2-stat-val" id="valRata">—</div><div class="ais2-stat-lbl">Rata-rata Respons</div></div>
</div>

<div class="ais2-filter-card">
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>No. Tiket / Judul</label><input type="text" id="f_q" placeholder="Cari tiket..."></div>
    <div class="ais2-filter-field"><label>Kategori</label>
      <select id="f_kat"><option value="">Semua</option>
        <option>Akses Sistem</option><option>Akademik</option><option>Pembayaran</option>
        <option>Fasilitas</option><option>Informasi Umum</option><option>Lainnya</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Prioritas</label>
      <select id="f_prior"><option value="">Semua</option><option>Rendah</option><option>Normal</option><option>Tinggi</option><option>Kritis</option></select>
    </div>
    <div class="ais2-filter-field"><label>Status</label>
      <select id="f_status"><option value="">Semua</option><option>Baru</option><option>Diproses</option><option>Menunggu Pengguna</option><option>Selesai</option><option>Ditutup</option></select>
    </div>
    <div class="ais2-filter-field"><label>Ditugaskan ke</label><input type="text" id="f_agt" placeholder="Nama agen..."></div>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" style="align-self:flex-end" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
  </div>
</div>

<div id="tabelCS"></div>

<script>
var tbl=null;
var WARNA_STATUS={Baru:'warning',Diproses:'info','Menunggu Pengguna':'secondary',Selesai:'success',Ditutup:'dark'};
var WARNA_PRIOR={Rendah:'secondary',Normal:'info',Tinggi:'warning',Kritis:'danger'};
document.addEventListener('DOMContentLoaded',function(){
    muatStat();
    tbl=AIS2.Table.init({
        container:'#tabelCS',modul:'pendataan',svc:'cs_tiket_list',
        bisa:{tambah:false,ubah:false,hapus:false},
        kolom:[
            {field:'nomorTiket',label:'No. Tiket',lebar:'100px',sort:true},
            {field:'judul',label:'Judul / Pertanyaan'},
            {field:'pelapor',label:'Pelapor',lebar:'130px',sembunyiMobile:true},
            {field:'kategori',label:'Kategori',lebar:'120px',sembunyiMobile:true},
            {field:'prioritas',label:'Prioritas',lebar:'80px',render:function(v){return AIS2.Badge.kustom(v,WARNA_PRIOR);}},
            {field:'status',label:'Status',lebar:'140px',render:function(v){return AIS2.Badge.kustom(v,WARNA_STATUS);}},
            {field:'ditugaskanKe',label:'Agen',lebar:'120px',sembunyiMobile:true}
        ],
        renderAksi:function(row){
            return '<div class="td-actions">'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="bukaDetail(\''+row.id+'\')" title="Buka Tiket"><i class="fa-solid fa-ticket"></i></button>'
                +'<button class="ais2-btn ais2-btn-primary ais2-btn-sm" onclick="jawabTiket(\''+row.id+'\')" title="Balas"><i class="fa-solid fa-reply"></i></button>'
                +'</div>';
        }
    });
});
function terapkan(){tbl&&tbl.setFilter({q:document.getElementById('f_q').value,kat:document.getElementById('f_kat').value,prior:document.getElementById('f_prior').value,status:document.getElementById('f_status').value,agt:document.getElementById('f_agt').value});}
function muatStat(){
    AIS2.Api.get('pendataan','cs_stat',{}).then(function(r){
        if(!r)return;
        ['Baru','Proses','Selesai','Rata'].forEach(function(k){
            var el=document.getElementById('val'+k);
            if(el&&r[k.toLowerCase()!==undefined])el.textContent=r[k.toLowerCase()]||'0';
        });
    });
}
function bukaTambah(){
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Pelapor <span class="req">*</span></label><input class="ais2-input" name="pelapor" required placeholder="Nama pelapor..."></div>'
        +'<div class="ais2-field"><label>Kontak</label><input class="ais2-input" name="kontak" placeholder="Email / telepon..."></div>'
        +'<div class="ais2-field"><label>Kategori <span class="req">*</span></label><select name="kategori" class="ais2-input" required><option value="">-- Pilih --</option>'
        +['Akses Sistem','Akademik','Pembayaran','Fasilitas','Informasi Umum','Lainnya'].map(function(k){return '<option>'+k+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Prioritas</label><select name="prioritas" class="ais2-input"><option>Rendah</option><option selected>Normal</option><option>Tinggi</option><option>Kritis</option></select></div>'
        +'<div class="ais2-field ais2-form-full"><label>Judul <span class="req">*</span></label><input class="ais2-input" name="judul" required></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:12px"><label>Isi Pertanyaan / Masalah <span class="req">*</span></label><textarea class="ais2-input" name="isi" rows="5" required></textarea></div>';
    var modal=AIS2.Modal.buka({
        judul:'Tiket Layanan Baru',ikon:'fa-headset',ukuran:'',
        konten:html,
        tombol:[
            {label:'Kirim Tiket',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.value;});
                if(!data.pelapor||!data.kategori||!data.judul||!data.isi){AIS2.Toast.peringatan('Lengkapi semua field wajib');return;}
                AIS2.Api.post('pendataan','cs_tiket_baru',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Tiket dikirim');AIS2.Modal.tutup();tbl&&tbl.refresh();muatStat();}
                    else AIS2.Toast.error((r&&r.msg)||'Gagal');
                });
            }},
            {label:'Batal',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}
function bukaDetail(id){AIS2.Toast.info('Memuat detail tiket #'+id);}
function jawabTiket(id){
    var modal=AIS2.Modal.buka({
        judul:'Balas Tiket',ikon:'fa-reply',ukuran:'',
        konten:'<input type="hidden" name="id" value="'+id+'">'
            +'<div class="ais2-field"><label>Status setelah dibalas</label><select name="status" class="ais2-input"><option>Diproses</option><option>Menunggu Pengguna</option><option>Selesai</option></select></div>'
            +'<div class="ais2-field" style="margin-top:10px"><label>Balasan <span class="req">*</span></label><textarea class="ais2-input" name="balasan" rows="5" required></textarea></div>',
        tombol:[
            {label:'Kirim Balasan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.value;});
                if(!data.balasan){AIS2.Toast.peringatan('Balasan wajib diisi');return;}
                AIS2.Api.post('pendataan','cs_tiket_balas',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses('Balasan dikirim');AIS2.Modal.tutup();tbl&&tbl.refresh();muatStat();}
                    else AIS2.Toast.error((r&&r.msg)||'Gagal');
                });
            }},
            {label:'Batal',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}
document.addEventListener('keydown',function(e){if(e.key==='Enter'&&e.target.tagName==='INPUT')terapkan();});
</script>
<style>
.ais2-stat-card{background:var(--ais2-card-bg);border:1px solid var(--ais2-border);border-radius:12px;padding:14px 16px;text-align:center}
.ais2-stat-val{font-size:28px;font-weight:700;color:var(--ais2-pri)}
.ais2-stat-lbl{font-size:12px;color:var(--ais2-text-sec);margin-top:2px}
</style>
</body></html>
