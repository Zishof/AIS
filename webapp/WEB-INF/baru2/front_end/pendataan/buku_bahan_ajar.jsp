<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Buku &amp; Bahan Ajar</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-book-open-reader"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Buku &amp; Bahan Ajar</span></div>
    <div class="ais2-ph-title">Buku &amp; Bahan Ajar</div>
    <div class="ais2-ph-sub">Kelola referensi, modul, dan bahan ajar digital per mata kuliah</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Tambah Referensi</button>
  </div>
</div>

<div class="ais2-tab-bar">
  <button class="ais2-tab-btn active" id="tab_buku" onclick="gTab('buku')"><i class="fa-solid fa-book"></i> Buku Referensi</button>
  <button class="ais2-tab-btn" id="tab_modul" onclick="gTab('modul')"><i class="fa-solid fa-file-lines"></i> Modul / Diktat</button>
  <button class="ais2-tab-btn" id="tab_digital" onclick="gTab('digital')"><i class="fa-solid fa-video"></i> Media Digital</button>
</div>

<div class="ais2-filter-card">
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>Judul / ISBN</label><input type="text" id="f_judul" placeholder="Judul atau ISBN..."></div>
    <div class="ais2-filter-field"><label>Penulis / Pengarang</label><input type="text" id="f_penulis" placeholder="Nama penulis..."></div>
    <div class="ais2-filter-field"><label>Mata Kuliah</label><input type="text" id="f_mk" placeholder="Kode / nama MK..."></div>
    <div class="ais2-filter-field"><label>Tahun Terbit</label><input type="number" id="f_tahun" placeholder="2023" style="width:90px"></div>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" style="align-self:flex-end" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
  </div>
</div>

<div id="tabelBBA"></div>

<script>
var tbl=null, tabAktif='buku';
document.addEventListener('DOMContentLoaded',function(){
    tbl=AIS2.Table.init({
        container:'#tabelBBA',modul:'pendataan',svc:'buku_bahan_ajar_list',
        bisa:{tambah:false,ubah:true,hapus:true},
        kolom:[
            {field:'judul',label:'Judul',sort:true},
            {field:'penulis',label:'Penulis / Pengarang',sembunyiMobile:true},
            {field:'penerbit',label:'Penerbit',lebar:'120px',sembunyiMobile:true},
            {field:'tahunTerbit',label:'Tahun',lebar:'70px'},
            {field:'mataKuliah',label:'Digunakan di MK',sembunyiMobile:true},
            {field:'jenis',label:'Jenis',lebar:'90px',render:function(v){
                var w={Buku:'primary',Modul:'info','Media Digital':'warning'};
                return AIS2.Badge.kustom(v,w);
            }}
        ],
        renderAksi:function(row){
            return '<div class="td-actions">'
                +(row.urlDigital?'<a class="ais2-btn ais2-btn-success ais2-btn-sm" href="'+row.urlDigital+'" target="_blank" title="Buka File"><i class="fa-solid fa-external-link-alt"></i></a>':'')
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="bukaForm('+JSON.stringify(row).replace(/"/g,'&quot;')+')" title="Ubah"><i class="fa-solid fa-pen"></i></button>'
                +'<button class="ais2-btn ais2-btn-danger ais2-btn-sm" onclick="hapus(\''+row.id+'\')" title="Hapus"><i class="fa-solid fa-trash"></i></button>'
                +'</div>';
        }
    });
});
function gTab(t){tabAktif=t;['buku','modul','digital'].forEach(function(k){document.getElementById('tab_'+k).classList.toggle('active',k===t);});terapkan();}
function terapkan(){tbl&&tbl.setFilter({tab:tabAktif,judul:document.getElementById('f_judul').value,penulis:document.getElementById('f_penulis').value,mk:document.getElementById('f_mk').value,tahun:document.getElementById('f_tahun').value});}
function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field ais2-form-full"><label>Judul <span class="req">*</span></label><input class="ais2-input" name="judul" value="'+esc(d.judul)+'" required></div>'
        +'<div class="ais2-field"><label>Jenis <span class="req">*</span></label><select name="jenis" class="ais2-input" required><option value="">-- Pilih --</option>'
        +['Buku Teks','Modul / Diktat','E-Book','Video Pembelajaran','Media Digital Lain'].map(function(j){return '<option'+(d.jenis===j?' selected':'')+'>'+j+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Penulis / Pengarang</label><input class="ais2-input" name="penulis" value="'+esc(d.penulis)+'"></div>'
        +'<div class="ais2-field"><label>Penerbit</label><input class="ais2-input" name="penerbit" value="'+esc(d.penerbit)+'"></div>'
        +'<div class="ais2-field"><label>Tahun Terbit</label><input class="ais2-input" type="number" name="tahunTerbit" value="'+esc(d.tahunTerbit)+'"></div>'
        +'<div class="ais2-field"><label>ISBN / ISSN</label><input class="ais2-input" name="isbn" value="'+esc(d.isbn)+'"></div>'
        +'<div class="ais2-field"><label>Edisi / Cetakan</label><input class="ais2-input" name="edisi" value="'+esc(d.edisi)+'"></div>'
        +'<div class="ais2-field"><label>Mata Kuliah Terkait</label><input class="ais2-input" name="mataKuliahRef" value="'+esc(d.mataKuliah)+'" placeholder="Kode atau nama MK..."></div>'
        +'<div class="ais2-field ais2-form-full"><label>URL Digital / Link</label><input class="ais2-input" name="urlDigital" value="'+esc(d.urlDigital)+'" placeholder="https://..."></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:8px"><label>Keterangan</label><textarea class="ais2-input" name="keterangan" rows="2">'+esc(d.keterangan)+'</textarea></div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Referensi':'Tambah Buku / Bahan Ajar',ikon:'fa-book-open-reader',ukuran:'',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.value;});
                if(!data.judul||!data.jenis){AIS2.Toast.peringatan('Judul dan Jenis wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'buku_bahan_ajar_ubah':'buku_bahan_ajar_tambah',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Tersimpan');AIS2.Modal.tutup();tbl&&tbl.refresh();}
                    else AIS2.Toast.error((r&&r.msg)||'Gagal');
                });
            }},
            {label:'Tutup',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}
function hapus(id){AIS2.Confirm.tampil({judul:'Hapus Referensi',pesan:'Data referensi ini akan dihapus?',tipe:'danger',labelOk:'Hapus',onOk:function(){AIS2.Api.post('pendataan','buku_bahan_ajar_hapus',{id:id}).then(function(r){if(r&&r.ok){AIS2.Toast.sukses('Dihapus');tbl&&tbl.refresh();}else AIS2.Toast.error((r&&r.msg)||'Gagal');});}});}
document.addEventListener('keydown',function(e){if(e.key==='Enter'&&e.target.tagName==='INPUT')terapkan();});
</script>
</body></html>
