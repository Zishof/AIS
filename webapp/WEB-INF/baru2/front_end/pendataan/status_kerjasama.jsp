<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Status Kerjasama Mahasiswa</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-user-tie"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Status Kerjasama Mhs</span></div>
    <div class="ais2-ph-title">Status Kerjasama Mahasiswa</div>
    <div class="ais2-ph-sub">Pantau status mahasiswa dalam program kerjasama, exchange, dan magang institusi</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-outline" onclick="ekspor()"><i class="fa-solid fa-file-excel"></i> Ekspor</button>
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Daftarkan Mahasiswa</button>
  </div>
</div>
<div class="ais2-filter-card">
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>Mahasiswa</label><input type="text" id="f_mhs" placeholder="NIM / nama..."></div>
    <div class="ais2-filter-field"><label>Program Kerjasama</label><input type="text" id="f_prog" placeholder="Nama program..."></div>
    <div class="ais2-filter-field"><label>Jenis</label>
      <select id="f_jenis"><option value="">Semua</option>
        <option>Student Exchange</option><option>Magang</option><option>Dual Degree</option>
        <option>Kampus Merdeka</option><option>Riset Bersama</option><option>Lainnya</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Status</label>
      <select id="f_status"><option value="">Semua</option>
        <option>Aktif</option><option>Selesai</option><option>Dibatalkan</option><option>Menunggu Konfirmasi</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Tahun Akademik</label><select id="f_ta"><option value="">Semua TA</option></select></div>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" style="align-self:flex-end" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
  </div>
</div>
<div id="tabelSK"></div>
<script>
var tbl=null;
var WARNA={Aktif:'success',Selesai:'secondary',Dibatalkan:'danger','Menunggu Konfirmasi':'warning'};
document.addEventListener('DOMContentLoaded',function(){
    muatTA();
    tbl=AIS2.Table.init({
        container:'#tabelSK',modul:'pendataan',svc:'status_kerjasama_list',
        bisa:{tambah:false,ubah:true,hapus:true},
        kolom:[
            {field:'nim',label:'NIM',lebar:'100px'},
            {field:'nama',label:'Nama Mahasiswa',sort:true},
            {field:'programKerjasama',label:'Program',sembunyiMobile:true},
            {field:'jenisKerjasama',label:'Jenis',lebar:'130px'},
            {field:'instansiMitra',label:'Instansi Mitra',sembunyiMobile:true},
            {field:'tanggalMulai',label:'Mulai',lebar:'90px',sembunyiMobile:true},
            {field:'tanggalSelesai',label:'Selesai',lebar:'90px',sembunyiMobile:true},
            {field:'status',label:'Status',lebar:'140px',render:function(v){return AIS2.Badge.kustom(v,WARNA);}}
        ],
        onUbah:function(d){bukaForm(d);},
        onHapus:function(id,cb){AIS2.Api.post('pendataan','status_kerjasama_hapus',{id:id}).then(function(r){cb(r&&r.ok,r&&r.msg);});}
    });
});
function terapkan(){tbl&&tbl.setFilter({mhs:document.getElementById('f_mhs').value,prog:document.getElementById('f_prog').value,jenis:document.getElementById('f_jenis').value,status:document.getElementById('f_status').value,ta:document.getElementById('f_ta').value});}
function muatTA(){AIS2.Api.get('pendataan','ref_tahun_akademik',{}).then(function(res){if(!res||!res.data)return;var sel=document.getElementById('f_ta');res.data.forEach(function(t){var o=document.createElement('option');o.value=t.id;o.textContent=t.label;sel.appendChild(o);});});}
function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Mahasiswa <span class="req">*</span></label><input class="ais2-input" name="mahasiswaRef" value="'+esc(d.nim||d.nama)+'" required placeholder="NIM atau nama..."></div>'
        +'<div class="ais2-field"><label>Jenis Kerjasama <span class="req">*</span></label><select name="jenisKerjasama" class="ais2-input" required><option value="">-- Pilih --</option>'
        +['Student Exchange','Magang','Dual Degree','Kampus Merdeka','Riset Bersama','Lainnya'].map(function(j){return '<option'+(d.jenisKerjasama===j?' selected':'')+'>'+j+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field ais2-form-full"><label>Program / Nama Kerjasama</label><input class="ais2-input" name="programKerjasama" value="'+esc(d.programKerjasama)+'"></div>'
        +'<div class="ais2-field"><label>Instansi Mitra</label><input class="ais2-input" name="instansiMitra" value="'+esc(d.instansiMitra)+'"></div>'
        +'<div class="ais2-field"><label>Negara / Kota</label><input class="ais2-input" name="negaraKota" value="'+esc(d.negaraKota)+'"></div>'
        +'<div class="ais2-field"><label>Tanggal Mulai</label><input class="ais2-input" type="date" name="tanggalMulai" value="'+esc(d.tanggalMulai)+'"></div>'
        +'<div class="ais2-field"><label>Tanggal Selesai</label><input class="ais2-input" type="date" name="tanggalSelesai" value="'+esc(d.tanggalSelesai)+'"></div>'
        +'<div class="ais2-field"><label>Konversi SKS</label><input class="ais2-input" type="number" name="konversiSKS" value="'+esc(d.konversiSKS)+'"></div>'
        +'<div class="ais2-field"><label>Status</label><select name="status" class="ais2-input">'
        +['Menunggu Konfirmasi','Aktif','Selesai','Dibatalkan'].map(function(s){return '<option'+(d.status===s?' selected':'')+'>'+s+'</option>';}).join('')+'</select></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:8px"><label>Keterangan</label><textarea class="ais2-input" name="keterangan" rows="2">'+esc(d.keterangan)+'</textarea></div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Status Kerjasama':'Daftarkan Mahasiswa ke Program',ikon:'fa-user-tie',ukuran:'',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.value;});
                if(!data.mahasiswaRef||!data.jenisKerjasama){AIS2.Toast.peringatan('Mahasiswa dan Jenis wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'status_kerjasama_ubah':'status_kerjasama_tambah',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Tersimpan');AIS2.Modal.tutup();tbl&&tbl.refresh();}
                    else AIS2.Toast.error((r&&r.msg)||'Gagal');
                });
            }},
            {label:'Tutup',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}
function ekspor(){window.open(ROOT+'/baru2?modul=pendataan&svc=status_kerjasama_ekspor','_blank');}
document.addEventListener('keydown',function(e){if(e.key==='Enter'&&e.target.tagName==='INPUT')terapkan();});
</script>
</body></html>
