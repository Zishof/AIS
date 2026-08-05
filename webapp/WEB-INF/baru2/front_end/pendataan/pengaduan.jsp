<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Pengaduan</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-triangle-exclamation"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Pengaduan</span></div>
    <div class="ais2-ph-title">Pengaduan &amp; Pelaporan</div>
    <div class="ais2-ph-sub">Kelola laporan dan pengaduan dari civitas akademika</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Tambah Laporan</button>
  </div>
</div>
<div class="ais2-filter-card">
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>Pelapor</label><input type="text" id="f_pelapor" placeholder="Nama pelapor..."></div>
    <div class="ais2-filter-field"><label>Kategori</label>
      <select id="f_kat"><option value="">Semua</option>
        <option>Akademik</option><option>Fasilitas</option><option>Pelayanan</option>
        <option>SDM</option><option>Keuangan</option><option>Lainnya</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Status</label>
      <select id="f_status"><option value="">Semua</option>
        <option>Baru</option><option>Diproses</option><option>Selesai</option><option>Ditolak</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Periode</label>
      <input type="month" id="f_bln">
    </div>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" style="align-self:flex-end" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
  </div>
</div>
<div id="tabelPngd"></div>
<script>
var tbl=null;
var WARNA_STATUS={Baru:'warning',Diproses:'info',Selesai:'success',Ditolak:'danger'};
document.addEventListener('DOMContentLoaded',function(){
    tbl=AIS2.Table.init({
        container:'#tabelPngd',modul:'pendataan',svc:'pengaduan_list',
        bisa:{tambah:false,ubah:false,hapus:false},
        kolom:[
            {field:'nomorLaporan',label:'No. Laporan',lebar:'110px'},
            {field:'pelapor',label:'Pelapor',sort:true},
            {field:'kategori',label:'Kategori',lebar:'100px',sembunyiMobile:true},
            {field:'perihal',label:'Perihal / Judul'},
            {field:'tanggal',label:'Tanggal',lebar:'100px',sembunyiMobile:true},
            {field:'status',label:'Status',lebar:'90px',render:function(v){return AIS2.Badge.kustom(v,WARNA_STATUS);}}
        ],
        renderAksi:function(row){
            return '<div class="td-actions">'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="bukaDetail(\''+row.id+'\')" title="Lihat Detail"><i class="fa-solid fa-eye"></i></button>'
                +'<button class="ais2-btn ais2-btn-primary ais2-btn-sm" onclick="prosesLaporan(\''+row.id+'\')" title="Proses Laporan"><i class="fa-solid fa-reply"></i></button>'
                +'</div>';
        }
    });
});
function terapkan(){tbl&&tbl.setFilter({pelapor:document.getElementById('f_pelapor').value,kat:document.getElementById('f_kat').value,status:document.getElementById('f_status').value,bln:document.getElementById('f_bln').value});}
function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Pelapor <span class="req">*</span></label><input class="ais2-input" name="pelapor" value="'+esc(d.pelapor)+'" required placeholder="Nama pelapor..."></div>'
        +'<div class="ais2-field"><label>Kontak</label><input class="ais2-input" name="kontak" value="'+esc(d.kontak)+'" placeholder="Email / telepon..."></div>'
        +'<div class="ais2-field"><label>Kategori <span class="req">*</span></label><select name="kategori" class="ais2-input" required><option value="">-- Pilih --</option>'
        +['Akademik','Fasilitas','Pelayanan','SDM','Keuangan','Lainnya'].map(function(k){return '<option'+(d.kategori===k?' selected':'')+'>'+k+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Tanggal Laporan</label><input class="ais2-input" type="date" name="tanggal" value="'+esc(d.tanggal)+'"></div>'
        +'<div class="ais2-field ais2-form-full"><label>Perihal / Judul <span class="req">*</span></label><input class="ais2-input" name="perihal" value="'+esc(d.perihal)+'" required></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:12px"><label>Isi Laporan <span class="req">*</span></label><textarea class="ais2-input" name="isiLaporan" rows="5" required>'+esc(d.isiLaporan)+'</textarea></div>'
        +'<div class="ais2-field" style="margin-top:8px"><label>Status Awal</label><select name="status" class="ais2-input">'
        +['Baru','Diproses','Selesai'].map(function(s){return '<option'+(d.status===s?' selected':'')+'>'+s+'</option>';}).join('')+'</select></div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Detail Pengaduan':'Tambah Laporan Pengaduan',ikon:'fa-triangle-exclamation',ukuran:'',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.value;});
                if(!data.pelapor||!data.kategori||!data.perihal||!data.isiLaporan){AIS2.Toast.peringatan('Pelapor, Kategori, Perihal, dan Isi wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'pengaduan_ubah':'pengaduan_tambah',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Tersimpan');AIS2.Modal.tutup();tbl&&tbl.refresh();}
                    else AIS2.Toast.error((r&&r.msg)||'Gagal');
                });
            }},
            {label:'Tutup',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}
function bukaDetail(id){
    AIS2.Api.get('pendataan','pengaduan_detail',{id:id}).then(function(r){if(r&&r.data)bukaForm(r.data);});
}
function prosesLaporan(id){
    var modal=AIS2.Modal.buka({
        judul:'Tindak Lanjut Pengaduan',ikon:'fa-reply',ukuran:'',
        konten:'<input type="hidden" name="id" value="'+id+'">'
            +'<div class="ais2-field"><label>Status <span class="req">*</span></label><select name="status" class="ais2-input"><option>Diproses</option><option>Selesai</option><option>Ditolak</option></select></div>'
            +'<div class="ais2-field" style="margin-top:12px"><label>Tanggapan <span class="req">*</span></label><textarea class="ais2-input" name="tanggapan" rows="4" required></textarea></div>',
        tombol:[
            {label:'Kirim Tanggapan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.value;});
                if(!data.tanggapan){AIS2.Toast.peringatan('Tanggapan wajib diisi');return;}
                AIS2.Api.post('pendataan','pengaduan_proses',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Tanggapan dikirim');AIS2.Modal.tutup();tbl&&tbl.refresh();}
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
