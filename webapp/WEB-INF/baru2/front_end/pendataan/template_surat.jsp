<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Template Surat</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-envelope-open-text"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Template Surat</span></div>
    <div class="ais2-ph-title">Template Surat Resmi</div>
    <div class="ais2-ph-sub">Kelola rancangan dan template surat resmi institusi</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Buat Template</button>
  </div>
</div>
<div class="ais2-filter-card">
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>Nama / Kode Template</label><input type="text" id="f_nama" placeholder="Cari template..."></div>
    <div class="ais2-filter-field"><label>Kategori</label>
      <select id="f_kat"><option value="">Semua</option>
        <option>Surat Keterangan</option><option>Surat Pengantar</option><option>Surat Keputusan</option>
        <option>Surat Undangan</option><option>Surat Permohonan</option><option>Surat Tugas</option>
        <option>Berita Acara</option><option>Lainnya</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Modul Terkait</label>
      <select id="f_modul"><option value="">Semua Modul</option>
        <option>Akademik</option><option>Kemahasiswaan</option><option>Kepegawaian</option>
        <option>Keuangan</option><option>Umum</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Status</label>
      <select id="f_status"><option value="">Semua</option><option>Aktif</option><option>Nonaktif</option><option>Draft</option></select>
    </div>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" style="align-self:flex-end" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
  </div>
</div>
<div id="tabelTS"></div>
<script>
var tbl=null;
document.addEventListener('DOMContentLoaded',function(){
    tbl=AIS2.Table.init({
        container:'#tabelTS',modul:'pendataan',svc:'template_surat_list',
        bisa:{tambah:false,ubah:true,hapus:true},
        kolom:[
            {field:'kode',label:'Kode',lebar:'90px'},
            {field:'nama',label:'Nama Template',sort:true},
            {field:'kategori',label:'Kategori',lebar:'150px',sembunyiMobile:true},
            {field:'modul',label:'Modul',lebar:'120px',sembunyiMobile:true},
            {field:'terakhirDiubah',label:'Terakhir Ubah',lebar:'120px',sembunyiMobile:true},
            {field:'status',label:'Status',lebar:'80px',render:function(v){
                var w={Aktif:'success',Nonaktif:'secondary',Draft:'warning'};
                return AIS2.Badge.kustom(v,w);
            }}
        ],
        renderAksi:function(row){
            return '<div class="td-actions">'
                +'<button class="ais2-btn ais2-btn-success ais2-btn-sm" onclick="pratinjau(\''+row.id+'\')" title="Pratinjau"><i class="fa-solid fa-eye"></i></button>'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="bukaForm('+JSON.stringify(row).replace(/"/g,'&quot;')+')" title="Edit Template"><i class="fa-solid fa-pen"></i></button>'
                +'<button class="ais2-btn ais2-btn-danger ais2-btn-sm" onclick="hapus(\''+row.id+'\')" title="Hapus"><i class="fa-solid fa-trash"></i></button>'
                +'</div>';
        }
    });
});
function terapkan(){tbl&&tbl.setFilter({nama:document.getElementById('f_nama').value,kat:document.getElementById('f_kat').value,modul:document.getElementById('f_modul').value,status:document.getElementById('f_status').value});}
function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Kode Template</label><input class="ais2-input" name="kode" value="'+esc(d.kode)+'" placeholder="SK-001, SRT-MHS..."></div>'
        +'<div class="ais2-field ais2-form-full"><label>Nama Template <span class="req">*</span></label><input class="ais2-input" name="nama" value="'+esc(d.nama)+'" required></div>'
        +'<div class="ais2-field"><label>Kategori <span class="req">*</span></label><select name="kategori" class="ais2-input" required><option value="">-- Pilih --</option>'
        +['Surat Keterangan','Surat Pengantar','Surat Keputusan','Surat Undangan','Surat Permohonan','Surat Tugas','Berita Acara','Lainnya'].map(function(k){return '<option'+(d.kategori===k?' selected':'')+'>'+k+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Modul Terkait</label><select name="modul" class="ais2-input"><option value="">-- Pilih --</option>'
        +['Akademik','Kemahasiswaan','Kepegawaian','Keuangan','Umum'].map(function(m){return '<option'+(d.modul===m?' selected':'')+'>'+m+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Status</label><select name="status" class="ais2-input">'
        +['Aktif','Nonaktif','Draft'].map(function(s){return '<option'+(d.status===s?' selected':'')+'>'+s+'</option>';}).join('')+'</select></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:12px"><label>Isi Template</label>'
        +'<div style="font-size:11px;color:var(--ais2-text-sec);margin-bottom:4px">Gunakan <code>{{VARIABEL}}</code> untuk variabel dinamis — mis. <code>{{NAMA}}</code>, <code>{{NIM}}</code>, <code>{{TANGGAL}}</code></div>'
        +'<textarea class="ais2-input" name="isiTemplate" rows="10" style="font-family:monospace;font-size:12px">'+esc(d.isiTemplate)+'</textarea></div>'
        +'<div class="ais2-field" style="margin-top:8px"><label>Variabel yang Tersedia</label><input class="ais2-input" name="variabelTersedia" value="'+esc(d.variabelTersedia)+'" placeholder="NAMA,NIM,PRODI,TANGGAL,..."></div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Edit Template Surat':'Buat Template Surat Baru',ikon:'fa-envelope-open-text',ukuran:'lg',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.value;});
                if(!data.nama||!data.kategori){AIS2.Toast.peringatan('Nama dan Kategori wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'template_surat_ubah':'template_surat_tambah',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Tersimpan');AIS2.Modal.tutup();tbl&&tbl.refresh();}
                    else AIS2.Toast.error((r&&r.msg)||'Gagal');
                });
            }},
            {label:'Tutup',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}
function pratinjau(id){window.open(ROOT+'/baru2?modul=pendataan&svc=template_surat_pratinjau&id='+id,'_blank');}
function hapus(id){AIS2.Confirm.tampil({judul:'Hapus Template',pesan:'Template surat ini akan dihapus permanen?',tipe:'danger',labelOk:'Hapus',onOk:function(){AIS2.Api.post('pendataan','template_surat_hapus',{id:id}).then(function(r){if(r&&r.ok){AIS2.Toast.sukses('Dihapus');tbl&&tbl.refresh();}else AIS2.Toast.error((r&&r.msg)||'Gagal');});}});}
document.addEventListener('keydown',function(e){if(e.key==='Enter'&&e.target.tagName==='INPUT')terapkan();});
</script>
</body></html>
