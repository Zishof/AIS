<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Manajemen Label Bahasa</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<%@ include file="../_lang.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
<style>
.lang-tab{display:flex;gap:4px;background:var(--ais2-bg);border-radius:10px;padding:4px;margin-bottom:20px;width:fit-content}
.lang-tab-btn{padding:6px 16px;border-radius:7px;border:none;cursor:pointer;font-size:13px;font-weight:600;transition:all .2s;background:none;color:var(--ais2-text-sec)}
.lang-tab-btn.active{background:var(--ais2-card-bg);color:var(--ais2-text);box-shadow:0 2px 6px rgba(0,0,0,.12)}
.lang-badge{display:inline-block;padding:2px 8px;border-radius:20px;font-size:10px;font-weight:700}
.lang-id{background:#dbeafe;color:#1e40af}
.lang-en{background:#d1fae5;color:#065f46}
.lang-ar{background:#fce7f3;color:#9d174d}
.lang-zh{background:#fef3c7;color:#92400e}
.cell-edit{display:none;position:absolute;inset:0;padding:4px}
.cell-wrap:hover .cell-edit,.cell-wrap.editing .cell-edit{display:block}
.cell-wrap{position:relative;cursor:text;padding:4px 8px;border-radius:6px;transition:.15s}
.cell-wrap:hover{background:var(--ais2-bg)}
.import-zone{border:2px dashed var(--ais2-border);border-radius:12px;padding:32px;text-align:center;cursor:pointer;transition:.2s;color:var(--ais2-text-sec)}
.import-zone:hover{border-color:var(--ais2-pri2);background:rgba(79,70,229,.04)}
</style>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">

<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-language"></i></div>
  <div>
    <div class="ais2-breadcrumb">
      <a href="<%=rootCtx%>/baru2?modul=pengaturan">Pengaturan</a>
      <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span>
      <span>Modul &amp; Label Bahasa</span>
    </div>
    <div class="ais2-ph-title">Label Bahasa Sistem</div>
    <div class="ais2-ph-sub">Kelola terjemahan kunci UI ke Bahasa Indonesia, Inggris, Arab, dan Mandarin</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-outline" onclick="importCsv()"><i class="fa-solid fa-file-import"></i> Impor CSV</button>
    <button class="ais2-btn ais2-btn-outline" onclick="ekspor()"><i class="fa-solid fa-file-export"></i> Ekspor</button>
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Tambah Label</button>
  </div>
</div>

<div class="ais2-filter-card">
  <div class="ais2-filter-row">
    <div class="ais2-filter-field" style="flex:2">
      <label>Cari Kunci / Teks</label>
      <input type="text" id="f_cari" placeholder="Cari kunci label atau isi terjemahan...">
    </div>
    <div class="ais2-filter-field">
      <label>Filter</label>
      <select id="f_filter">
        <option value="">Semua</option>
        <option value="belum_en">Belum diterjemahkan (EN)</option>
        <option value="belum_ar">Belum diterjemahkan (AR)</option>
        <option value="belum_zh">Belum diterjemahkan (ZH)</option>
      </select>
    </div>
    <button class="ais2-btn ais2-btn-primary ais2-btn-sm" style="align-self:flex-end" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
    <button class="ais2-btn ais2-btn-outline ais2-btn-sm" style="align-self:flex-end" onclick="reset()"><i class="fa-solid fa-rotate-left"></i></button>
  </div>
</div>

<div class="lang-tab">
  <button class="lang-tab-btn active" onclick="gantiTab('semua',this)">Semua Bahasa</button>
  <button class="lang-tab-btn" onclick="gantiTab('id',this)"><span class="lang-badge lang-id">ID</span> Indonesia</button>
  <button class="lang-tab-btn" onclick="gantiTab('en',this)"><span class="lang-badge lang-en">EN</span> English</button>
  <button class="lang-tab-btn" onclick="gantiTab('ar',this)"><span class="lang-badge lang-ar">AR</span> Arab</button>
  <button class="lang-tab-btn" onclick="gantiTab('zh',this)"><span class="lang-badge lang-zh">ZH</span> Mandarin</button>
</div>

<div id="tabelLabel"></div>

<script>
var tbl=null;
var tabAktif='semua';

document.addEventListener('DOMContentLoaded',function(){
    tbl=AIS2.Table.init({
        container:'#tabelLabel',modul:'pengaturan',svc:'label_list',
        bisa:{tambah:false,ubah:true,hapus:true},
        sortDefault:'nama',
        kolom:kolomUntukTab(tabAktif),
        renderAksi:function(row){
            return '<div class="td-actions">'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="bukaForm('+JSON.stringify(row).replace(/"/g,'&quot;')+')" title="Ubah"><i class="fa-solid fa-pen"></i></button>'
                +'<button class="ais2-btn ais2-btn-danger ais2-btn-sm" onclick="hapus(\''+row.id+'\')" title="Hapus"><i class="fa-solid fa-trash"></i></button>'
                +'</div>';
        }
    });
});

function kolomUntukTab(tab){
    var kol=[{field:'nama',label:'Kunci Label',lebar:'180px',sort:true}];
    if(tab==='semua'||tab==='id') kol.push({field:'indonesia',label:'<span class="lang-badge lang-id">ID</span> Indonesia'});
    if(tab==='semua'||tab==='en') kol.push({field:'english',label:'<span class="lang-badge lang-en">EN</span> English',sembunyiMobile:tab==='semua'});
    if(tab==='semua'||tab==='ar') kol.push({field:'arab',label:'<span class="lang-badge lang-ar">AR</span> Arab',sembunyiMobile:true});
    if(tab==='semua'||tab==='zh') kol.push({field:'mandarin',label:'<span class="lang-badge lang-zh">ZH</span> Mandarin',sembunyiMobile:true});
    if(tab==='semua') kol.push({field:'keterangan',label:'Keterangan',sembunyiMobile:true});
    return kol;
}

function gantiTab(tab,btn){
    tabAktif=tab;
    document.querySelectorAll('.lang-tab-btn').forEach(function(b){b.classList.remove('active');});
    btn.classList.add('active');
    if(tbl){tbl.gantiKolom(kolomUntukTab(tab));tbl.refresh();}
}

function terapkan(){tbl&&tbl.setFilter({cari:document.getElementById('f_cari').value,filter:document.getElementById('f_filter').value});}
function reset(){document.getElementById('f_cari').value='';document.getElementById('f_filter').selectedIndex=0;terapkan();}

function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var fld=function(lbl,name,val,kls,placeholder){
        return '<div class="ais2-field"><label>'+lbl+'</label>'
            +'<input class="ais2-input" name="'+name+'" value="'+esc(val)+'" placeholder="'+(placeholder||'')+'" dir="'+(name==='arab'?'rtl':'ltr')+'"></div>';
    };
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-field"><label>Kunci Label <span class="req">*</span></label>'
        +'<input class="ais2-input" name="nama" value="'+esc(d.nama)+'" required placeholder="Contoh: TambahData, JudulHalaman, Mahasiswa..."></div>'
        +'<div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-top:4px">'
        +fld('<span class="lang-badge lang-id">ID</span> Bahasa Indonesia','indonesia',d.indonesia)
        +fld('<span class="lang-badge lang-en">EN</span> English','english',d.english)
        +fld('<span class="lang-badge lang-ar">AR</span> Arab (اللغة العربية)','arab',d.arab)
        +fld('<span class="lang-badge lang-zh">ZH</span> Mandarin (中文)','mandarin',d.mandarin)
        +'</div>'
        +'<div class="ais2-field" style="margin-top:4px"><label>Keterangan</label>'
        +'<input class="ais2-input" name="keterangan" value="'+esc(d.keterangan)+'" placeholder="Konteks penggunaan label ini..."></div>';

    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Label: '+d.nama:'Tambah Label Bahasa Baru',
        ikon:'fa-language',ukuran:'',konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.value;});
                if(!data.nama){AIS2.Toast.peringatan('Kunci label wajib diisi');return;}
                if(!data.indonesia){AIS2.Toast.peringatan('Teks Bahasa Indonesia wajib diisi');return;}
                AIS2.Api.post('pengaturan',d.id?'label_ubah':'label_tambah',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Tersimpan');AIS2.Modal.tutup();tbl&&tbl.refresh();}
                    else AIS2.Toast.error((r&&r.msg)||'Gagal');
                });
            }},
            {label:'Tutup',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}

function hapus(id){
    AIS2.Confirm.tampil({judul:'Hapus Label?',pesan:'Label bahasa ini akan dihapus permanen.',tipe:'danger',labelOk:'Hapus',onOk:function(){
        AIS2.Api.post('pengaturan','label_hapus',{id:id}).then(function(r){
            if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Dihapus');tbl&&tbl.refresh();}
            else AIS2.Toast.error((r&&r.msg)||'Gagal');
        });
    }});
}

function ekspor(){window.open(ROOT+'/baru2?modul=pengaturan&svc=label_ekspor','_blank');}

function importCsv(){
    AIS2.Modal.buka({
        judul:'Impor Label dari CSV',ikon:'fa-file-import',ukuran:'',
        konten:'<p style="font-size:13px;color:var(--ais2-text-sec);margin:0 0 16px">Format CSV: <strong>nama,indonesia,english,arab,mandarin,keterangan</strong> (header baris pertama).</p>'
            +'<div class="import-zone" onclick="document.getElementById(\'inpCsv\').click()">'
            +'<i class="fa-solid fa-file-csv" style="font-size:32px;margin-bottom:10px;display:block;color:var(--ais2-pri2)"></i>'
            +'<div style="font-size:14px;font-weight:600">Klik atau drag file CSV di sini</div>'
            +'<div id="namaFile" style="font-size:12px;margin-top:6px">Belum ada file dipilih</div></div>'
            +'<input type="file" id="inpCsv" accept=".csv" style="display:none" onchange="piliCsv(this)">',
        tombol:[
            {label:'Upload & Impor',kelas:'ais2-btn-primary',onClick:function(){
                var inp=document.getElementById('inpCsv');
                if(!inp||!inp.files||!inp.files[0]){AIS2.Toast.peringatan('Pilih file CSV terlebih dahulu');return;}
                var fd=new FormData();fd.append('file',inp.files[0]);fd.append('svc','label_impor');
                fetch(ROOT+'/baru2?modul=pengaturan&svc=label_impor',{method:'POST',body:fd})
                    .then(function(r){return r.json();}).then(function(r){
                        if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Impor selesai');AIS2.Modal.tutup();tbl&&tbl.refresh();}
                        else AIS2.Toast.error((r&&r.msg)||'Gagal impor');
                    }).catch(function(){AIS2.Toast.error('Gagal upload');});
            }},
            {label:'Batal',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}
function piliCsv(inp){
    if(inp.files&&inp.files[0]) document.getElementById('namaFile').textContent=inp.files[0].name;
}

document.addEventListener('keydown',function(e){if(e.key==='Enter'&&e.target.tagName==='INPUT'&&e.target.id!=='f_cari')return;if(e.key==='Enter'&&e.target.id==='f_cari')terapkan();});
</script>
</body></html>
