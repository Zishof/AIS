<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Formulir Kegiatan</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-file-lines"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Formulir Kegiatan</span></div>
    <div class="ais2-ph-title">Formulir Kegiatan</div>
    <div class="ais2-ph-sub">Kelola template formulir dan data kegiatan akademik/non-akademik</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Buat Formulir</button>
  </div>
</div>
<div class="ais2-tab-bar">
  <button class="ais2-tab-btn active" id="tab_template" onclick="gTab('template')">Template Formulir</button>
  <button class="ais2-tab-btn" id="tab_pengisian" onclick="gTab('pengisian')">Data Pengisian</button>
</div>
<div class="ais2-filter-card">
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>Judul / Nama Formulir</label><input type="text" id="f_judul" placeholder="Cari formulir..."></div>
    <div class="ais2-filter-field"><label>Kategori</label>
      <select id="f_kat"><option value="">Semua</option>
        <option>Kegiatan Akademik</option><option>Kemahasiswaan</option><option>Administrasi</option>
        <option>Penelitian</option><option>Pengabdian Masyarakat</option><option>Lainnya</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Status</label>
      <select id="f_status"><option value="">Semua</option><option>Aktif</option><option>Nonaktif</option><option>Draft</option></select>
    </div>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" style="align-self:flex-end" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
  </div>
</div>
<div id="tabelFK"></div>
<script>
var tbl=null, tabAktif='template';
document.addEventListener('DOMContentLoaded',function(){
    tbl=AIS2.Table.init({
        container:'#tabelFK',modul:'pendataan',svc:'formulir_kegiatan_list',
        bisa:{tambah:false,ubah:true,hapus:true},
        kolom:[
            {field:'namaFormulir',label:'Nama Formulir',sort:true},
            {field:'kategori',label:'Kategori',lebar:'150px',sembunyiMobile:true},
            {field:'jumlahPengisian',label:'Pengisian',lebar:'80px'},
            {field:'dibuat',label:'Dibuat',lebar:'100px',sembunyiMobile:true},
            {field:'status',label:'Status',lebar:'80px',render:function(v){
                var w={Aktif:'success',Nonaktif:'secondary',Draft:'warning'};
                return AIS2.Badge.kustom(v,w);
            }}
        ],
        renderAksi:function(row){
            return '<div class="td-actions">'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="lihatIsi(\''+row.id+'\')" title="Lihat Pengisian"><i class="fa-solid fa-table-list"></i></button>'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="bukaForm('+JSON.stringify(row).replace(/"/g,'&quot;')+')" title="Ubah"><i class="fa-solid fa-pen"></i></button>'
                +'<button class="ais2-btn ais2-btn-danger ais2-btn-sm" onclick="hapus(\''+row.id+'\')" title="Hapus"><i class="fa-solid fa-trash"></i></button>'
                +'</div>';
        }
    });
});
function gTab(t){tabAktif=t;['template','pengisian'].forEach(function(k){document.getElementById('tab_'+k).classList.toggle('active',k===t);});terapkan();}
function terapkan(){tbl&&tbl.setFilter({tab:tabAktif,judul:document.getElementById('f_judul').value,kat:document.getElementById('f_kat').value,status:document.getElementById('f_status').value});}
function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field ais2-form-full"><label>Nama Formulir <span class="req">*</span></label><input class="ais2-input" name="namaFormulir" value="'+esc(d.namaFormulir)+'" required></div>'
        +'<div class="ais2-field"><label>Kategori <span class="req">*</span></label><select name="kategori" class="ais2-input" required><option value="">-- Pilih --</option>'
        +['Kegiatan Akademik','Kemahasiswaan','Administrasi','Penelitian','Pengabdian Masyarakat','Lainnya'].map(function(k){return '<option'+(d.kategori===k?' selected':'')+'>'+k+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Status</label><select name="status" class="ais2-input">'
        +['Aktif','Nonaktif','Draft'].map(function(s){return '<option'+(d.status===s?' selected':'')+'>'+s+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Tanggal Buka</label><input class="ais2-input" type="date" name="tanggalBuka" value="'+esc(d.tanggalBuka)+'"></div>'
        +'<div class="ais2-field"><label>Tanggal Tutup</label><input class="ais2-input" type="date" name="tanggalTutup" value="'+esc(d.tanggalTutup)+'"></div>'
        +'<div class="ais2-field"><label>Batas Pengisian</label><input class="ais2-input" type="number" name="batasPengisian" value="'+esc(d.batasPengisian)+'" placeholder="0 = tidak terbatas"></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:12px"><label>Deskripsi / Instruksi</label><textarea class="ais2-input" name="deskripsi" rows="3">'+esc(d.deskripsi)+'</textarea></div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Formulir':'Buat Formulir Kegiatan',ikon:'fa-file-lines',ukuran:'',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.value;});
                if(!data.namaFormulir||!data.kategori){AIS2.Toast.peringatan('Nama dan Kategori wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'formulir_kegiatan_ubah':'formulir_kegiatan_tambah',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Tersimpan');AIS2.Modal.tutup();tbl&&tbl.refresh();}
                    else AIS2.Toast.error((r&&r.msg)||'Gagal');
                });
            }},
            {label:'Tutup',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}
function hapus(id){AIS2.Confirm.tampil({judul:'Hapus Formulir',pesan:'Formulir ini akan dihapus beserta semua data pengisian?',tipe:'danger',labelOk:'Hapus',onOk:function(){AIS2.Api.post('pendataan','formulir_kegiatan_hapus',{id:id}).then(function(r){if(r&&r.ok){AIS2.Toast.sukses('Dihapus');tbl&&tbl.refresh();}else AIS2.Toast.error((r&&r.msg)||'Gagal');});}});}
function lihatIsi(id){AIS2.Toast.info('Memuat data pengisian formulir...');}
document.addEventListener('keydown',function(e){if(e.key==='Enter'&&e.target.tagName==='INPUT')terapkan();});
</script>
</body></html>
