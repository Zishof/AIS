<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Dokumen Pendukung</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-folder-open"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Dokumen Pendukung</span></div>
    <div class="ais2-ph-title">Dokumen Pendukung &amp; Arsip</div>
    <div class="ais2-ph-sub">Kelola berkas, dokumen, dan arsip digital institusi</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-upload"></i> Unggah Dokumen</button>
  </div>
</div>

<!-- Folder navigation breadcrumb style -->
<div style="display:flex;align-items:center;gap:8px;margin-bottom:12px;padding:10px 14px;background:var(--ais2-card-bg);border-radius:10px;border:1px solid var(--ais2-border)">
  <i class="fa-solid fa-folder-tree" style="color:var(--ais2-pri2)"></i>
  <span id="folderPath" style="color:var(--ais2-text-sec);font-size:13px">Semua Folder</span>
  <button class="ais2-btn ais2-btn-sm ais2-btn-outline" onclick="bukaFolderBaru()"><i class="fa-solid fa-folder-plus"></i> Folder Baru</button>
</div>

<div class="ais2-filter-card">
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>Nama Dokumen</label><input type="text" id="f_nama" placeholder="Cari nama berkas..."></div>
    <div class="ais2-filter-field"><label>Kategori</label>
      <select id="f_kat"><option value="">Semua</option>
        <option>SK / Surat Keputusan</option><option>Laporan</option><option>Kontrak</option>
        <option>Peraturan</option><option>Pedoman</option><option>Foto</option><option>Video</option><option>Lainnya</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Tahun</label><input type="number" id="f_tahun" placeholder="2024" style="width:90px"></div>
    <div class="ais2-filter-field"><label>Diunggah Oleh</label><input type="text" id="f_oleh" placeholder="Nama pengunggah..."></div>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" style="align-self:flex-end" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
  </div>
</div>

<div id="tabelDok"></div>

<script>
var tbl=null;
var IKON_EXT={pdf:'fa-file-pdf text-danger',doc:'fa-file-word text-primary',docx:'fa-file-word text-primary',xls:'fa-file-excel text-success',xlsx:'fa-file-excel text-success',ppt:'fa-file-powerpoint text-orange',pptx:'fa-file-powerpoint text-orange',jpg:'fa-file-image text-info',jpeg:'fa-file-image text-info',png:'fa-file-image text-info',mp4:'fa-file-video text-purple',zip:'fa-file-zipper text-muted'};
document.addEventListener('DOMContentLoaded',function(){
    tbl=AIS2.Table.init({
        container:'#tabelDok',modul:'pendataan',svc:'dokumen_list',
        bisa:{tambah:false,ubah:false,hapus:true},
        kolom:[
            {field:'nama',label:'Nama Dokumen',sort:true,render:function(v,row){
                var ext=(row.ekstensi||'').toLowerCase();
                var ikon=IKON_EXT[ext]||'fa-file text-muted';
                return '<i class="fa-solid '+ikon+'" style="margin-right:6px"></i>'+v;
            }},
            {field:'kategori',label:'Kategori',lebar:'140px',sembunyiMobile:true},
            {field:'ukuran',label:'Ukuran',lebar:'80px',sembunyiMobile:true},
            {field:'tanggalUnggah',label:'Tanggal',lebar:'100px',sembunyiMobile:true},
            {field:'diunggahOleh',label:'Diunggah Oleh',lebar:'130px',sembunyiMobile:true}
        ],
        renderAksi:function(row){
            return '<div class="td-actions">'
                +'<a class="ais2-btn ais2-btn-outline ais2-btn-sm" href="'+ROOT+'/baru2?modul=pendataan&svc=dokumen_unduh&id='+row.id+'" title="Unduh" target="_blank"><i class="fa-solid fa-download"></i></a>'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="bukaDetail(\''+row.id+'\')" title="Detail"><i class="fa-solid fa-circle-info"></i></button>'
                +'<button class="ais2-btn ais2-btn-danger ais2-btn-sm" onclick="hapusDok(\''+row.id+'\')" title="Hapus"><i class="fa-solid fa-trash"></i></button>'
                +'</div>';
        }
    });
});
function terapkan(){tbl&&tbl.setFilter({nama:document.getElementById('f_nama').value,kat:document.getElementById('f_kat').value,tahun:document.getElementById('f_tahun').value,oleh:document.getElementById('f_oleh').value});}
function bukaTambah(){
    var html='<div class="ais2-form-grid">'
        +'<div class="ais2-field ais2-form-full"><label>Nama Dokumen <span class="req">*</span></label><input class="ais2-input" name="nama" required></div>'
        +'<div class="ais2-field"><label>Kategori <span class="req">*</span></label><select name="kategori" class="ais2-input" required><option value="">-- Pilih --</option>'
        +['SK / Surat Keputusan','Laporan','Kontrak','Peraturan','Pedoman','Foto','Video','Lainnya'].map(function(k){return '<option>'+k+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Tahun</label><input class="ais2-input" type="number" name="tahun"></div>'
        +'<div class="ais2-field ais2-form-full"><label>Berkas <span class="req">*</span></label>'
        +'<div class="ais2-upload-zone" id="uzone" onclick="document.getElementById(\'fileInput\').click()">'
        +'<i class="fa-solid fa-cloud-arrow-up fa-2x"></i><p>Klik atau seret berkas ke sini</p>'
        +'<p style="font-size:11px;color:var(--ais2-text-muted)">Maks. 50 MB — PDF, Word, Excel, Gambar, Video</p>'
        +'<input id="fileInput" type="file" name="file" style="display:none" onchange="praUnggah(this)">'
        +'</div></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:8px"><label>Keterangan</label><textarea class="ais2-input" name="keterangan" rows="2"></textarea></div>';
    var modal=AIS2.Modal.buka({
        judul:'Unggah Dokumen',ikon:'fa-cloud-arrow-up',ukuran:'',
        konten:html,
        tombol:[
            {label:'Unggah',kelas:'ais2-btn-primary',onClick:function(){
                var form=modal.body();
                var nama=form.querySelector('[name=nama]').value;
                var kat=form.querySelector('[name=kategori]').value;
                var file=form.querySelector('[name=file]');
                if(!nama||!kat){AIS2.Toast.peringatan('Nama dan Kategori wajib diisi');return;}
                if(!file||!file.files.length){AIS2.Toast.peringatan('Pilih berkas terlebih dahulu');return;}
                var fd=new FormData();
                fd.append('modul','pendataan'); fd.append('svc','dokumen_unggah');
                form.querySelectorAll('[name]').forEach(function(el){if(el.type!=='file')fd.append(el.name,el.value);});
                fd.append('file',file.files[0]);
                AIS2.Api.post('pendataan','dokumen_unggah',fd).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Dokumen berhasil diunggah');AIS2.Modal.tutup();tbl&&tbl.refresh();}
                    else AIS2.Toast.error((r&&r.msg)||'Gagal');
                });
            }},
            {label:'Batal',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}
function praUnggah(inp){
    if(inp.files&&inp.files[0]){document.getElementById('uzone').querySelector('p').textContent=inp.files[0].name;}
}
function bukaDetail(id){AIS2.Toast.info('Memuat detail dokumen...');}
function hapusDok(id){
    AIS2.Confirm.tampil({judul:'Hapus Dokumen',pesan:'Dokumen akan dihapus permanen. Lanjutkan?',tipe:'danger',labelOk:'Ya, Hapus',onOk:function(){
        AIS2.Api.post('pendataan','dokumen_hapus',{id:id}).then(function(r){
            if(r&&r.ok){AIS2.Toast.sukses('Dihapus');tbl&&tbl.refresh();}else AIS2.Toast.error((r&&r.msg)||'Gagal');
        });
    }});
}
function bukaFolderBaru(){AIS2.Toast.info('Fitur folder baru — coming soon.');}
document.addEventListener('keydown',function(e){if(e.key==='Enter'&&e.target.tagName==='INPUT')terapkan();});
</script>
</body></html>
