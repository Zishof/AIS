<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Bank Soal</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-database"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Bank Soal</span></div>
    <div class="ais2-ph-title">Bank Soal (Repository)</div>
    <div class="ais2-ph-sub">Kelola repositori soal ujian per mata kuliah dan jenjang</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-outline" onclick="impor()"><i class="fa-solid fa-file-import"></i> Impor Soal</button>
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Tambah Soal</button>
  </div>
</div>

<!-- Tabs: Pilihan Ganda vs Essay vs Upload -->
<div class="ais2-tab-bar">
  <button class="ais2-tab-btn active" id="tab_pg" onclick="gTab('pg')"><i class="fa-solid fa-circle-dot"></i> Pilihan Ganda</button>
  <button class="ais2-tab-btn" id="tab_essay" onclick="gTab('essay')"><i class="fa-solid fa-pen-to-square"></i> Essay</button>
  <button class="ais2-tab-btn" id="tab_upload" onclick="gTab('upload')"><i class="fa-solid fa-upload"></i> File Upload</button>
</div>

<div class="ais2-filter-card">
  <div class="ais2-filter-head">
    <i class="fa-solid fa-filter" style="color:var(--ais2-pri2)"></i>
    <span class="ais2-filter-title">Filter</span>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
    <button class="ais2-btn ais2-btn-sm ais2-btn-outline" onclick="reset()"><i class="fa-solid fa-rotate-left"></i></button>
  </div>
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>Kata Kunci Soal</label><input type="text" id="f_q" placeholder="Teks pertanyaan..."></div>
    <div class="ais2-filter-field"><label>Mata Kuliah</label><input type="text" id="f_mk" placeholder="Kode / nama MK..."></div>
    <div class="ais2-filter-field"><label>Tingkat Kesulitan</label>
      <select id="f_sulit"><option value="">Semua</option><option>Mudah</option><option>Sedang</option><option>Sulit</option></select>
    </div>
    <div class="ais2-filter-field"><label>Topik / Bab</label><input type="text" id="f_topik" placeholder="Topik atau bab..."></div>
    <div class="ais2-filter-field"><label>Dosen Pembuat</label><input type="text" id="f_dosen" placeholder="Nama dosen..."></div>
  </div>
</div>

<div id="tabelBS"></div>

<script>
var tbl=null, tabAktif='pg';
document.addEventListener('DOMContentLoaded',function(){
    tbl=AIS2.Table.init({
        container:'#tabelBS',modul:'pendataan',svc:'bank_soal_list',
        bisa:{tambah:false,ubah:true,hapus:true},
        kolom:[
            {field:'nomorSoal',label:'No.',lebar:'60px'},
            {field:'pertanyaan',label:'Pertanyaan / Judul Soal'},
            {field:'mataKuliah',label:'Mata Kuliah',lebar:'120px',sembunyiMobile:true},
            {field:'kesulitan',label:'Tingkat',lebar:'80px',render:function(v){
                var w={Mudah:'success',Sedang:'warning',Sulit:'danger'};
                return AIS2.Badge.kustom(v,w);
            }},
            {field:'topik',label:'Topik',lebar:'100px',sembunyiMobile:true},
            {field:'dosenPembuat',label:'Pembuat',lebar:'120px',sembunyiMobile:true}
        ],
        onUbah:function(d){bukaForm(d);},
        onHapus:function(id,cb){AIS2.Api.post('pendataan','bank_soal_hapus',{id:id}).then(function(r){cb(r&&r.ok,r&&r.msg);});}
    });
});
function gTab(t){
    tabAktif=t;
    ['pg','essay','upload'].forEach(function(k){document.getElementById('tab_'+k).classList.toggle('active',k===t);});
    terapkan();
}
function terapkan(){tbl&&tbl.setFilter({tipe:tabAktif,q:document.getElementById('f_q').value,mk:document.getElementById('f_mk').value,sulit:document.getElementById('f_sulit').value,topik:document.getElementById('f_topik').value,dosen:document.getElementById('f_dosen').value});}
function reset(){['f_q','f_mk','f_topik','f_dosen'].forEach(function(id){var el=document.getElementById(id);if(el)el.value='';});['f_sulit'].forEach(function(id){var el=document.getElementById(id);if(el)el.selectedIndex=0;});terapkan();}
function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var pilG=tabAktif==='pg';
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<input type="hidden" name="tipeSoal" value="'+tabAktif+'">'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Mata Kuliah <span class="req">*</span></label><input class="ais2-input" name="mataKuliahRef" value="'+esc(d.mataKuliah)+'" required placeholder="Kode / nama MK..."></div>'
        +'<div class="ais2-field"><label>Topik / Bab</label><input class="ais2-input" name="topik" value="'+esc(d.topik)+'"></div>'
        +'<div class="ais2-field"><label>Tingkat Kesulitan</label><select name="kesulitan" class="ais2-input"><option value="">--</option>'
        +['Mudah','Sedang','Sulit'].map(function(k){return '<option'+(d.kesulitan===k?' selected':'')+'>'+k+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Bobot / Poin</label><input class="ais2-input" type="number" name="bobot" value="'+esc(d.bobot)+'"></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:12px"><label>Pertanyaan <span class="req">*</span></label><textarea class="ais2-input" name="pertanyaan" rows="4" required>'+esc(d.pertanyaan)+'</textarea></div>'
        +(pilG?'<div class="ais2-form-section-title" style="margin-top:12px">Pilihan Jawaban</div>'
            +'<div class="ais2-field"><label>A.</label><input class="ais2-input" name="pilihanA" value="'+esc(d.pilihanA)+'"></div>'
            +'<div class="ais2-field"><label>B.</label><input class="ais2-input" name="pilihanB" value="'+esc(d.pilihanB)+'"></div>'
            +'<div class="ais2-field"><label>C.</label><input class="ais2-input" name="pilihanC" value="'+esc(d.pilihanC)+'"></div>'
            +'<div class="ais2-field"><label>D.</label><input class="ais2-input" name="pilihanD" value="'+esc(d.pilihanD)+'"></div>'
            +'<div class="ais2-field"><label>E.</label><input class="ais2-input" name="pilihanE" value="'+esc(d.pilihanE)+'"></div>'
            +'<div class="ais2-field"><label>Jawaban Benar</label><select name="jawabanBenar" class="ais2-input"><option value="">--</option>'
            +['A','B','C','D','E'].map(function(c){return '<option'+(d.jawabanBenar===c?' selected':'')+'>'+c+'</option>';}).join('')+'</select></div>'
        :'<div class="ais2-field" style="margin-top:8px"><label>Kunci Jawaban / Rubrik</label><textarea class="ais2-input" name="jawabanKunci" rows="3">'+esc(d.jawabanKunci)+'</textarea></div>');
    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Soal':'Tambah Soal Bank Soal',ikon:'fa-database',ukuran:'',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.value;});
                if(!data.mataKuliahRef||!data.pertanyaan){AIS2.Toast.peringatan('Mata Kuliah dan Pertanyaan wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'bank_soal_ubah':'bank_soal_tambah',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Tersimpan');AIS2.Modal.tutup();tbl&&tbl.refresh();}
                    else AIS2.Toast.error((r&&r.msg)||'Gagal');
                });
            }},
            {label:'Tutup',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}
function impor(){AIS2.Toast.info('Fitur impor soal: unggah file Excel/PDF berisi soal.');}
document.addEventListener('keydown',function(e){if(e.key==='Enter'&&e.target.tagName==='INPUT')terapkan();});
</script>
</body></html>
