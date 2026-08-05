<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Kedisiplinan Mahasiswa</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-scale-balanced"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Kedisiplinan</span></div>
    <div class="ais2-ph-title">Kedisiplinan Mahasiswa</div>
    <div class="ais2-ph-sub">Kelola data pelanggaran, sanksi, dan rehabilitasi mahasiswa</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Catat Pelanggaran</button>
  </div>
</div>
<div class="ais2-filter-card">
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>Mahasiswa</label><input type="text" id="f_mhs" placeholder="NIM / nama..."></div>
    <div class="ais2-filter-field"><label>Kategori Pelanggaran</label>
      <select id="f_kat"><option value="">Semua</option>
        <option>Pelanggaran Akademik</option><option>Pelanggaran Non-Akademik</option>
        <option>Tindakan Kekerasan</option><option>Pemalsuan Dokumen</option><option>Penyalahgunaan Narkoba</option><option>Lainnya</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Tingkat Sanksi</label>
      <select id="f_sanksi"><option value="">Semua</option>
        <option>Peringatan Lisan</option><option>Surat Peringatan</option><option>Skorsing</option><option>Dikeluarkan</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Tahun Akademik</label><select id="f_ta"><option value="">Semua TA</option></select></div>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" style="align-self:flex-end" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
  </div>
</div>
<div id="tabelKD"></div>
<script>
var tbl=null;
document.addEventListener('DOMContentLoaded',function(){
    muatTA();
    tbl=AIS2.Table.init({
        container:'#tabelKD',modul:'pendataan',svc:'kedisiplinan_list',
        bisa:{tambah:false,ubah:true,hapus:true},
        kolom:[
            {field:'nim',label:'NIM',lebar:'100px'},
            {field:'nama',label:'Nama Mahasiswa',sort:true},
            {field:'kategori',label:'Kategori',sembunyiMobile:true},
            {field:'tanggalKejadian',label:'Tanggal',lebar:'100px',sembunyiMobile:true},
            {field:'sanksi',label:'Sanksi',lebar:'140px',render:function(v){
                var w={'Peringatan Lisan':'warning','Surat Peringatan':'warning',Skorsing:'danger',Dikeluarkan:'dark'};
                return AIS2.Badge.kustom(v,w);
            }},
            {field:'status',label:'Status',lebar:'90px',render:function(v){return AIS2.Badge.kustom(v,{Aktif:'danger',Selesai:'success',Banding:'info'});}}
        ],
        onUbah:function(d){bukaForm(d);},
        onHapus:function(id,cb){AIS2.Api.post('pendataan','kedisiplinan_hapus',{id:id}).then(function(r){cb(r&&r.ok,r&&r.msg);});}
    });
});
function terapkan(){tbl&&tbl.setFilter({mhs:document.getElementById('f_mhs').value,kat:document.getElementById('f_kat').value,sanksi:document.getElementById('f_sanksi').value,ta:document.getElementById('f_ta').value});}
function muatTA(){AIS2.Api.get('pendataan','ref_tahun_akademik',{}).then(function(res){if(!res||!res.data)return;var sel=document.getElementById('f_ta');res.data.forEach(function(t){var o=document.createElement('option');o.value=t.id;o.textContent=t.label;sel.appendChild(o);});});}
function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Mahasiswa <span class="req">*</span></label><input class="ais2-input" name="mahasiswaRef" value="'+esc(d.nim||d.nama)+'" required placeholder="NIM atau nama..."></div>'
        +'<div class="ais2-field"><label>Tahun Akademik</label><select name="tahunAkademik" class="ais2-input"><option value="">--</option></select></div>'
        +'<div class="ais2-field"><label>Tanggal Kejadian</label><input class="ais2-input" type="date" name="tanggalKejadian" value="'+esc(d.tanggalKejadian)+'"></div>'
        +'<div class="ais2-field"><label>Kategori Pelanggaran <span class="req">*</span></label><select name="kategori" class="ais2-input" required><option value="">-- Pilih --</option>'
        +['Pelanggaran Akademik','Pelanggaran Non-Akademik','Tindakan Kekerasan','Pemalsuan Dokumen','Penyalahgunaan Narkoba','Lainnya'].map(function(k){return '<option'+(d.kategori===k?' selected':'')+'>'+k+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Tingkat Sanksi</label><select name="sanksi" class="ais2-input"><option value="">--</option>'
        +['Peringatan Lisan','Surat Peringatan','Skorsing','Dikeluarkan'].map(function(s){return '<option'+(d.sanksi===s?' selected':'')+'>'+s+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Lama Sanksi (hari)</label><input class="ais2-input" type="number" name="lamaSanksi" value="'+esc(d.lamaSanksi)+'"></div>'
        +'<div class="ais2-field"><label>Nomor SK Sanksi</label><input class="ais2-input" name="nomorSK" value="'+esc(d.nomorSK)+'"></div>'
        +'<div class="ais2-field"><label>Status</label><select name="status" class="ais2-input">'
        +['Aktif','Selesai','Banding'].map(function(s){return '<option'+(d.status===s?' selected':'')+'>'+s+'</option>';}).join('')+'</select></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:12px"><label>Uraian Pelanggaran <span class="req">*</span></label><textarea class="ais2-input" name="uraian" rows="4" required>'+esc(d.uraian)+'</textarea></div>'
        +'<div class="ais2-field" style="margin-top:8px"><label>Tindak Lanjut / Rehabilitasi</label><textarea class="ais2-input" name="tindakLanjut" rows="2">'+esc(d.tindakLanjut)+'</textarea></div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Catatan Kedisiplinan':'Catat Pelanggaran Mahasiswa',ikon:'fa-scale-balanced',ukuran:'',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.value;});
                if(!data.mahasiswaRef||!data.kategori||!data.uraian){AIS2.Toast.peringatan('Mahasiswa, Kategori, dan Uraian wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'kedisiplinan_ubah':'kedisiplinan_tambah',data).then(function(r){
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
