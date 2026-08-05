<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Kegiatan Kemahasiswaan</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-flag"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Kegiatan Kemahasiswaan</span></div>
    <div class="ais2-ph-title">Kegiatan Kemahasiswaan</div>
    <div class="ais2-ph-sub">Kelola data kegiatan organisasi, UKM, dan prestasi kemahasiswaan</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Tambah Kegiatan</button>
  </div>
</div>
<div class="ais2-filter-card">
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>Nama Kegiatan</label><input type="text" id="f_nama" placeholder="Cari kegiatan..."></div>
    <div class="ais2-filter-field"><label>Jenis</label>
      <select id="f_jenis"><option value="">Semua</option>
        <option>Organisasi Kemahasiswaan</option><option>UKM</option><option>Prestasi</option>
        <option>Pengabdian Masyarakat</option><option>Seni & Budaya</option><option>Olahraga</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Tahun Akademik</label><select id="f_ta"><option value="">Semua</option></select></div>
    <div class="ais2-filter-field"><label>Mahasiswa</label><input type="text" id="f_mhs" placeholder="NIM / nama..."></div>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" style="align-self:flex-end" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
  </div>
</div>
<div id="tabelKK"></div>
<script>
var tbl=null;
document.addEventListener('DOMContentLoaded',function(){
    muatTA();
    tbl=AIS2.Table.init({
        container:'#tabelKK',modul:'pendataan',svc:'kegiatan_kemahasiswaan_list',
        bisa:{tambah:false,ubah:true,hapus:true},
        kolom:[
            {field:'nama',label:'Nama Kegiatan',sort:true},
            {field:'jenis',label:'Jenis',lebar:'150px'},
            {field:'tahunAkademik',label:'TA',lebar:'80px',sembunyiMobile:true},
            {field:'tingkat',label:'Tingkat',lebar:'90px',sembunyiMobile:true},
            {field:'prestasi',label:'Prestasi/Hasil',sembunyiMobile:true},
            {field:'jumlahPeserta',label:'Peserta',lebar:'70px'}
        ],
        onUbah:function(d){bukaForm(d);},
        onHapus:function(id,cb){AIS2.Api.post('pendataan','kegiatan_km_hapus',{id:id}).then(function(r){cb(r&&r.ok,r&&r.msg);});}
    });
});
function terapkan(){tbl&&tbl.setFilter({nama:document.getElementById('f_nama').value,jenis:document.getElementById('f_jenis').value,ta:document.getElementById('f_ta').value,mhs:document.getElementById('f_mhs').value});}
function muatTA(){AIS2.Api.get('pendataan','ref_tahun_akademik',{}).then(function(res){if(!res||!res.data)return;var sel=document.getElementById('f_ta');res.data.forEach(function(t){var o=document.createElement('option');o.value=t.id;o.textContent=t.label;sel.appendChild(o);});});}
function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field ais2-form-full"><label>Nama Kegiatan <span class="req">*</span></label><input class="ais2-input" name="nama" value="'+esc(d.nama)+'" required></div>'
        +'<div class="ais2-field"><label>Jenis Kegiatan</label><select name="jenis" class="ais2-input"><option value="">--</option>'
        +['Organisasi Kemahasiswaan','UKM','Prestasi','Pengabdian Masyarakat','Seni & Budaya','Olahraga','Lainnya'].map(function(j){return '<option'+(d.jenis===j?' selected':'')+'>'+j+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Tingkat</label><select name="tingkat" class="ais2-input"><option value="">--</option>'
        +['Lokal','Regional','Nasional','Internasional'].map(function(t){return '<option'+(d.tingkat===t?' selected':'')+'>'+t+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Tahun Akademik</label><select name="tahunAkademik" class="ais2-input"><option value="">--</option></select></div>'
        +'<div class="ais2-field"><label>Tanggal Mulai</label><input class="ais2-input" type="date" name="tanggalMulai" value="'+esc(d.tanggalMulai)+'"></div>'
        +'<div class="ais2-field"><label>Tanggal Selesai</label><input class="ais2-input" type="date" name="tanggalSelesai" value="'+esc(d.tanggalSelesai)+'"></div>'
        +'<div class="ais2-field"><label>Prestasi / Hasil</label><input class="ais2-input" name="prestasi" value="'+esc(d.prestasi)+'"></div>'
        +'<div class="ais2-field"><label>Jumlah Peserta</label><input class="ais2-input" type="number" name="jumlahPeserta" value="'+esc(d.jumlahPeserta)+'"></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:12px"><label>Keterangan</label><textarea class="ais2-input" name="keterangan" rows="3">'+esc(d.keterangan)+'</textarea></div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Kegiatan':'Tambah Kegiatan Kemahasiswaan',ikon:'fa-flag',ukuran:'',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.type==='checkbox'?(el.checked?'true':'false'):el.value;});
                if(!data.nama){AIS2.Toast.peringatan('Nama wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'kegiatan_km_ubah':'kegiatan_km_tambah',data).then(function(r){
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
