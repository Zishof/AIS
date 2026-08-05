<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Absen Piket</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-clipboard-check"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Absen Piket</span></div>
    <div class="ais2-ph-title">Absensi Piket Mahasiswa</div>
    <div class="ais2-ph-sub">Kelola kehadiran piket harian mahasiswa di unit kerja/laboratorium</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-outline" onclick="ekspor()"><i class="fa-solid fa-file-excel"></i> Ekspor Rekap</button>
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Input Absen</button>
  </div>
</div>
<div class="ais2-tab-bar">
  <button class="ais2-tab-btn active" id="tab_absen" onclick="gTab('absen')">Rekap Absensi</button>
  <button class="ais2-tab-btn" id="tab_jadwal" onclick="gTab('jadwal')">Jadwal Piket</button>
</div>
<div class="ais2-filter-card">
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>Mahasiswa</label><input type="text" id="f_mhs" placeholder="NIM / nama..."></div>
    <div class="ais2-filter-field"><label>Unit Piket / Lab</label><input type="text" id="f_unit" placeholder="Unit atau laboratorium..."></div>
    <div class="ais2-filter-field"><label>Periode</label><input type="month" id="f_bln"></div>
    <div class="ais2-filter-field"><label>Status</label>
      <select id="f_status"><option value="">Semua</option><option>Hadir</option><option>Tidak Hadir</option><option>Izin</option><option>Sakit</option></select>
    </div>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" style="align-self:flex-end" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
  </div>
</div>
<div id="tabelAP"></div>
<script>
var tbl=null, tabAktif='absen';
document.addEventListener('DOMContentLoaded',function(){
    var d=new Date();
    document.getElementById('f_bln').value=d.getFullYear()+'-'+(('0'+(d.getMonth()+1)).slice(-2));
    tbl=AIS2.Table.init({
        container:'#tabelAP',modul:'pendataan',svc:'absen_piket_list',
        bisa:{tambah:false,ubah:true,hapus:true},
        kolom:[
            {field:'tanggal',label:'Tanggal',lebar:'100px',sort:true},
            {field:'nim',label:'NIM',lebar:'100px'},
            {field:'nama',label:'Nama Mahasiswa',sort:true},
            {field:'unitPiket',label:'Unit Piket',sembunyiMobile:true},
            {field:'jamMasuk',label:'Masuk',lebar:'70px'},
            {field:'jamKeluar',label:'Keluar',lebar:'70px'},
            {field:'status',label:'Status',lebar:'90px',render:function(v){
                var w={Hadir:'success','Tidak Hadir':'danger',Izin:'warning',Sakit:'info'};
                return AIS2.Badge.kustom(v,w);
            }}
        ],
        onUbah:function(d){bukaForm(d);},
        onHapus:function(id,cb){AIS2.Api.post('pendataan','absen_piket_hapus',{id:id}).then(function(r){cb(r&&r.ok,r&&r.msg);});}
    });
});
function gTab(t){tabAktif=t;['absen','jadwal'].forEach(function(k){document.getElementById('tab_'+k).classList.toggle('active',k===t);});terapkan();}
function terapkan(){tbl&&tbl.setFilter({tab:tabAktif,mhs:document.getElementById('f_mhs').value,unit:document.getElementById('f_unit').value,bln:document.getElementById('f_bln').value,status:document.getElementById('f_status').value});}
function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Mahasiswa <span class="req">*</span></label><input class="ais2-input" name="mahasiswaRef" value="'+esc(d.nim||d.nama)+'" required placeholder="NIM atau nama..."></div>'
        +'<div class="ais2-field"><label>Unit Piket <span class="req">*</span></label><input class="ais2-input" name="unitPiket" value="'+esc(d.unitPiket)+'" required placeholder="Unit atau laboratorium..."></div>'
        +'<div class="ais2-field"><label>Tanggal <span class="req">*</span></label><input class="ais2-input" type="date" name="tanggal" value="'+esc(d.tanggal)+'" required></div>'
        +'<div class="ais2-field"><label>Jam Masuk</label><input class="ais2-input" type="time" name="jamMasuk" value="'+esc(d.jamMasuk)+'"></div>'
        +'<div class="ais2-field"><label>Jam Keluar</label><input class="ais2-input" type="time" name="jamKeluar" value="'+esc(d.jamKeluar)+'"></div>'
        +'<div class="ais2-field"><label>Status <span class="req">*</span></label><select name="status" class="ais2-input" required>'
        +['Hadir','Tidak Hadir','Izin','Sakit'].map(function(s){return '<option'+(d.status===s?' selected':'')+'>'+s+'</option>';}).join('')+'</select></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:8px"><label>Keterangan</label><input class="ais2-input" name="keterangan" value="'+esc(d.keterangan)+'"></div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Absen Piket':'Input Absen Piket',ikon:'fa-clipboard-check',ukuran:'',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.value;});
                if(!data.mahasiswaRef||!data.unitPiket||!data.tanggal||!data.status){AIS2.Toast.peringatan('Mahasiswa, Unit, Tanggal, dan Status wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'absen_piket_ubah':'absen_piket_tambah',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Tersimpan');AIS2.Modal.tutup();tbl&&tbl.refresh();}
                    else AIS2.Toast.error((r&&r.msg)||'Gagal');
                });
            }},
            {label:'Tutup',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}
function ekspor(){window.open(ROOT+'/baru2?modul=pendataan&svc=absen_piket_ekspor&bln='+document.getElementById('f_bln').value,'_blank');}
document.addEventListener('keydown',function(e){if(e.key==='Enter'&&e.target.tagName==='INPUT')terapkan();});
</script>
</body></html>
