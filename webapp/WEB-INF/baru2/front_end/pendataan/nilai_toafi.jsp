<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Nilai Toafi / Toefl</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">

<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-language"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Nilai Toafi / Toefl</span></div>
    <div class="ais2-ph-title">Nilai Toafi / Toefl Mahasiswa</div>
    <div class="ais2-ph-sub">Rekam skor bahasa (TOAFL/TOEFL/IELTS) mahasiswa</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-outline" onclick="ekspor()"><i class="fa-solid fa-file-excel"></i> Ekspor</button>
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Input Nilai</button>
  </div>
</div>

<div class="ais2-filter-card">
  <div class="ais2-filter-head">
    <i class="fa-solid fa-filter" style="color:var(--ais2-pri2)"></i>
    <span class="ais2-filter-title">Filter</span>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
    <button class="ais2-btn ais2-btn-sm ais2-btn-outline" onclick="reset()"><i class="fa-solid fa-rotate-left"></i></button>
  </div>
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>Mahasiswa (NIM/Nama)</label><input type="text" id="f_mhs" placeholder="Cari mahasiswa..."></div>
    <div class="ais2-filter-field"><label>Jenis Tes</label>
      <select id="f_jenis">
        <option value="">Semua</option>
        <option>TOAFL</option><option>TOEFL ITP</option><option>TOEFL IBT</option>
        <option>IELTS</option><option>Bahasa Arab Internal</option><option>Bahasa Inggris Internal</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Tahun</label><input type="number" id="f_tahun" placeholder="2024" style="width:90px"></div>
    <div class="ais2-filter-field"><label>Fakultas</label><select id="f_fak"><option value="">Semua</option></select></div>
    <div class="ais2-filter-field"><label>Program Studi</label><select id="f_prodi"><option value="">Semua</option></select></div>
  </div>
</div>

<div id="tabelToafi"></div>

<script>
var tbl=null;
document.addEventListener('DOMContentLoaded',function(){
    muatFakultas();
    tbl=AIS2.Table.init({
        container:'#tabelToafi', modul:'pendataan', svc:'nilai_toafi_list',
        bisa:{tambah:false,ubah:true,hapus:true},
        kolom:[
            {field:'nim',    label:'NIM', lebar:'110px', sort:true},
            {field:'nama',   label:'Nama Mahasiswa', sort:true},
            {field:'angkatan',label:'Angkatan', lebar:'80px', sembunyiMobile:true},
            {field:'prodi',  label:'Prodi', sembunyiMobile:true},
            {field:'jenisTes',label:'Jenis Tes', lebar:'130px'},
            {field:'skor',   label:'Skor', lebar:'70px', sort:true},
            {field:'tanggal',label:'Tanggal Tes', lebar:'100px', sembunyiMobile:true},
            {field:'lembagaPenyelenggara',label:'Lembaga', sembunyiMobile:true}
        ],
        onUbah:function(d){bukaForm(d);},
        onHapus:function(id,cb){AIS2.Api.post('pendataan','nilai_toafi_hapus',{id:id}).then(function(r){cb(r&&r.ok,r&&r.msg);});}
    });
});
function terapkan(){tbl&&tbl.setFilter({mhs:document.getElementById('f_mhs').value,jenis:document.getElementById('f_jenis').value,tahun:document.getElementById('f_tahun').value,fakultas:document.getElementById('f_fak').value,prodi:document.getElementById('f_prodi').value});}
function reset(){['f_mhs','f_tahun'].forEach(function(id){var el=document.getElementById(id);if(el)el.value='';});['f_jenis','f_fak','f_prodi'].forEach(function(id){var el=document.getElementById(id);if(el)el.selectedIndex=0;});terapkan();}
function muatFakultas(){
    AIS2.Api.get('pendataan','ref_fakultas',{}).then(function(res){
        if(!res||!res.data)return;
        ['f_fak','f_prodi'].forEach(function(id){});
        var sel=document.getElementById('f_fak');
        res.data.forEach(function(f){var o=document.createElement('option');o.value=f.id;o.textContent=f.nama;sel.appendChild(o);});
    });
}
function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-section-title">Data Nilai Bahasa</div>'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Mahasiswa <span class="req">*</span></label><input class="ais2-input" name="mahasiswaRef" value="'+esc(d.nimMahasiswa||d.namaMahasiswa)+'" required placeholder="NIM atau nama..."></div>'
        +'<div class="ais2-field"><label>Jenis Tes <span class="req">*</span></label><select name="jenisTes" class="ais2-input" required><option value="">-- Pilih --</option>'
        +['TOAFL','TOEFL ITP','TOEFL IBT','IELTS','Bahasa Arab Internal','Bahasa Inggris Internal','Lainnya'].map(function(j){return '<option'+(d.jenisTes===j?' selected':'')+'>'+j+'</option>';}).join('')
        +'</select></div>'
        +'<div class="ais2-field"><label>Skor <span class="req">*</span></label><input class="ais2-input" type="number" step="0.01" name="skor" value="'+esc(d.skor)+'" required></div>'
        +'<div class="ais2-field"><label>Tanggal Tes</label><input class="ais2-input" type="date" name="tanggal" value="'+esc(d.tanggal)+'"></div>'
        +'<div class="ais2-field"><label>Lembaga Penyelenggara</label><input class="ais2-input" name="lembagaPenyelenggara" value="'+esc(d.lembagaPenyelenggara)+'"></div>'
        +'<div class="ais2-field"><label>Nomor Sertifikat</label><input class="ais2-input" name="nomorSertifikat" value="'+esc(d.nomorSertifikat)+'"></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:12px"><label>Keterangan</label><textarea class="ais2-input" name="keterangan" rows="2">'+esc(d.keterangan)+'</textarea></div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Nilai':'Input Nilai Toafi/Toefl',ikon:'fa-language',ukuran:'',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.type==='checkbox'?(el.checked?'true':'false'):el.value;});
                if(!data.mahasiswaRef||!data.jenisTes||!data.skor){AIS2.Toast.peringatan('Mahasiswa, Jenis, dan Skor wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'nilai_toafi_ubah':'nilai_toafi_tambah',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Tersimpan');AIS2.Modal.tutup();tbl&&tbl.refresh();}
                    else AIS2.Toast.error((r&&r.msg)||'Gagal');
                });
            }},
            {label:'Tutup',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}
function ekspor(){window.open(ROOT+'/baru2?modul=pendataan&svc=nilai_toafi_ekspor','_blank');}
document.addEventListener('keydown',function(e){if(e.key==='Enter'&&e.target.tagName==='INPUT')terapkan();});
</script>
</body></html>
