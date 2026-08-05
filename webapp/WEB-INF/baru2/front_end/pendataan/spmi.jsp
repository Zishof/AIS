<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>SPMI</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-award"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>SPMI</span></div>
    <div class="ais2-ph-title">SPMI — Sistem Penjaminan Mutu Internal</div>
    <div class="ais2-ph-sub">Kelola standar, instrumen audit, dan dokumen mutu institusi</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Tambah Standar</button>
  </div>
</div>

<div class="ais2-tab-bar">
  <button class="ais2-tab-btn active" id="tab_standar" onclick="gTab('standar')"><i class="fa-solid fa-shield-halved"></i> Standar Mutu</button>
  <button class="ais2-tab-btn" id="tab_instrumen" onclick="gTab('instrumen')"><i class="fa-solid fa-list-check"></i> Instrumen</button>
  <button class="ais2-tab-btn" id="tab_audit" onclick="gTab('audit')"><i class="fa-solid fa-magnifying-glass-chart"></i> Audit Internal</button>
  <button class="ais2-tab-btn" id="tab_temuan" onclick="gTab('temuan')"><i class="fa-solid fa-circle-exclamation"></i> Temuan &amp; RTM</button>
</div>

<div class="ais2-filter-card">
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>Nama / Kode Standar</label><input type="text" id="f_q" placeholder="Cari standar..."></div>
    <div class="ais2-filter-field"><label>Kategori</label>
      <select id="f_kat"><option value="">Semua</option>
        <option>Standar Pendidikan</option><option>Standar Penelitian</option>
        <option>Standar Pengabdian Masyarakat</option><option>Standar Pengelolaan</option>
        <option>Standar Pembiayaan</option><option>Standar Sarana-Prasarana</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Tahun</label><input type="number" id="f_tahun" placeholder="2024" style="width:90px"></div>
    <div class="ais2-filter-field"><label>Status</label>
      <select id="f_status"><option value="">Semua</option><option>Aktif</option><option>Revisi</option><option>Nonaktif</option></select>
    </div>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" style="align-self:flex-end" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
  </div>
</div>

<div id="tabelSPMI"></div>

<script>
var tbl=null, tabAktif='standar';
document.addEventListener('DOMContentLoaded',function(){
    tbl=AIS2.Table.init({
        container:'#tabelSPMI',modul:'pendataan',svc:'spmi_list',
        bisa:{tambah:false,ubah:true,hapus:false},
        kolom:[
            {field:'kode',label:'Kode',lebar:'90px',sort:true},
            {field:'nama',label:'Nama Standar / Dokumen',sort:true},
            {field:'kategori',label:'Kategori',sembunyiMobile:true},
            {field:'versi',label:'Versi',lebar:'70px',sembunyiMobile:true},
            {field:'tahun',label:'Tahun',lebar:'70px'},
            {field:'status',label:'Status',lebar:'80px',render:function(v){
                var w={Aktif:'success',Revisi:'warning',Nonaktif:'secondary'};
                return AIS2.Badge.kustom(v,w);
            }}
        ],
        renderAksi:function(row){
            return '<div class="td-actions">'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="bukaForm('+JSON.stringify(row).replace(/"/g,'&quot;')+')" title="Ubah"><i class="fa-solid fa-pen"></i></button>'
                +'<a class="ais2-btn ais2-btn-outline ais2-btn-sm" href="'+ROOT+'/baru2?modul=pendataan&svc=spmi_unduh&id='+row.id+'" target="_blank" title="Unduh Dokumen"><i class="fa-solid fa-file-arrow-down"></i></a>'
                +'</div>';
        }
    });
});
function gTab(t){
    tabAktif=t;
    ['standar','instrumen','audit','temuan'].forEach(function(k){document.getElementById('tab_'+k).classList.toggle('active',k===t);});
    terapkan();
}
function terapkan(){tbl&&tbl.setFilter({tab:tabAktif,q:document.getElementById('f_q').value,kat:document.getElementById('f_kat').value,tahun:document.getElementById('f_tahun').value,status:document.getElementById('f_status').value});}
function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Kode Standar</label><input class="ais2-input" name="kode" value="'+esc(d.kode)+'"></div>'
        +'<div class="ais2-field ais2-form-full"><label>Nama Standar <span class="req">*</span></label><input class="ais2-input" name="nama" value="'+esc(d.nama)+'" required></div>'
        +'<div class="ais2-field"><label>Kategori <span class="req">*</span></label><select name="kategori" class="ais2-input" required><option value="">-- Pilih --</option>'
        +['Standar Pendidikan','Standar Penelitian','Standar Pengabdian Masyarakat','Standar Pengelolaan','Standar Pembiayaan','Standar Sarana-Prasarana'].map(function(k){return '<option'+(d.kategori===k?' selected':'')+'>'+k+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Versi</label><input class="ais2-input" name="versi" value="'+esc(d.versi)+'" placeholder="1.0"></div>'
        +'<div class="ais2-field"><label>Tahun</label><input class="ais2-input" type="number" name="tahun" value="'+esc(d.tahun)+'"></div>'
        +'<div class="ais2-field"><label>Tanggal Berlaku</label><input class="ais2-input" type="date" name="tanggalBerlaku" value="'+esc(d.tanggalBerlaku)+'"></div>'
        +'<div class="ais2-field"><label>Status</label><select name="status" class="ais2-input">'
        +['Aktif','Revisi','Nonaktif'].map(function(s){return '<option'+(d.status===s?' selected':'')+'>'+s+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Dokumen (URL / ID)</label><input class="ais2-input" name="urlDokumen" value="'+esc(d.urlDokumen)+'" placeholder="Link atau ID berkas..."></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:12px"><label>Deskripsi / Isi Singkat</label><textarea class="ais2-input" name="deskripsi" rows="3">'+esc(d.deskripsi)+'</textarea></div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Standar SPMI':'Tambah Standar Mutu',ikon:'fa-award',ukuran:'',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.value;});
                if(!data.nama||!data.kategori){AIS2.Toast.peringatan('Nama dan Kategori wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'spmi_ubah':'spmi_tambah',data).then(function(r){
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
