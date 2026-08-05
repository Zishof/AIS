<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Grup Pengguna</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<%@ include file="../_lang.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">

<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-user-tag"></i></div>
  <div>
    <div class="ais2-breadcrumb">
      <a href="<%=rootCtx%>/baru2?modul=pengaturan">Pengaturan</a>
      <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span>
      <span id="judulBreadcrumb">Grup Pengguna</span>
    </div>
    <div class="ais2-ph-title" id="judulHalaman">Grup Pengguna (Role)</div>
    <div class="ais2-ph-sub" id="deskripsiHalaman">Kelola grup/peran pengguna beserta hak akses modul yang diperbolehkan</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> <span id="btnTambahLabel">Tambah Grup</span></button>
  </div>
</div>

<div class="ais2-filter-card">
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label id="lblCariGrup">Cari Grup</label>
      <input type="text" id="f_nama" placeholder="Nama atau kode grup...">
    </div>
    <div class="ais2-filter-field"><label id="lblStatusFilter">Status</label>
      <select id="f_aktif">
        <option value="">Semua</option>
        <option value="1" id="optAktif">Aktif</option>
        <option value="0" id="optNonaktif">Nonaktif</option>
      </select>
    </div>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" style="align-self:flex-end" onclick="terapkan()">
      <i class="fa-solid fa-magnifying-glass"></i> <span id="btnCariLabel">Cari</span>
    </button>
  </div>
</div>

<div id="tabelGrup"></div>

<!-- Modal Hak Akses Modul -->
<div id="aksesModulModal" style="display:none;position:fixed;inset:0;z-index:9000;background:rgba(0,0,0,.5);overflow-y:auto">
  <div style="max-width:780px;margin:40px auto;background:var(--ais2-card-bg);border-radius:16px;padding:0;box-shadow:0 20px 60px rgba(0,0,0,.3)">
    <div style="background:var(--ais2-grad);padding:18px 24px;border-radius:16px 16px 0 0;display:flex;align-items:center;gap:12px">
      <i class="fa-solid fa-shield-halved" style="color:#fff;font-size:20px"></i>
      <span style="color:#fff;font-weight:700;font-size:16px" id="aksesModalJudul">Hak Akses Modul</span>
      <button onclick="tutupAksesModal()" style="margin-left:auto;background:rgba(255,255,255,.2);border:none;color:#fff;border-radius:8px;padding:6px 12px;cursor:pointer"><i class="fa-solid fa-xmark"></i></button>
    </div>
    <div style="padding:20px">
      <input type="hidden" id="aksesRoleId">
      <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:10px" id="modulGrid"></div>
      <div style="margin-top:20px;display:flex;justify-content:flex-end;gap:10px">
        <button class="ais2-btn ais2-btn-primary" onclick="simpanAkses()"><i class="fa-solid fa-save"></i> Simpan Hak Akses</button>
        <button class="ais2-btn ais2-btn-outline" onclick="tutupAksesModal()">Tutup</button>
      </div>
    </div>
  </div>
</div>

<script>
var tbl=null;

/* Definisi semua modul/flag Tbmrole */
var MODUL_FLAGS=[
    {key:'dashboard',lbl:'Dasbor Akademik'},
    {key:'elearning',lbl:'e-Learning'},
    {key:'kegiatanDanPrestasi',lbl:'Prestasi & Kegiatan'},
    {key:'pustaka',lbl:'Pustaka'},
    {key:'workflow',lbl:'Workflow / SOP'},
    {key:'dasborRepository',lbl:'Repository'},
    {key:'dasboardAntarJemput',lbl:'Antar Jemput'},
    {key:'tampilkanSpmi',lbl:'SPMI'},
    {key:'kantin',lbl:'Kantin / Toko'},
    {key:'dashboardKoperasi',lbl:'Koperasi'},
    {key:'administrasi',lbl:'Administrasi'},
    {key:'pengadaan',lbl:'Pengadaan'},
    {key:'pembayaran',lbl:'Pembayaran'},
    {key:'keuangan',lbl:'Keuangan'},
    {key:'akunting',lbl:'Akuntansi'},
    {key:'kepegawaian',lbl:'Kepegawaian'},
    {key:'tampilkanGaji',lbl:'Penggajian'},
    {key:'kinerja',lbl:'Kinerja'},
    {key:'presensiKehadiran',lbl:'Presensi / Kehadiran'},
    {key:'kalenderAkademik',lbl:'Kalender Akademik'},
    {key:'infoKegiatan',lbl:'Info Kegiatan'},
    {key:'bolehAksesFeeder',lbl:'Neo Feeder'},
    {key:'bolehAksesSister',lbl:'SISTER'},
    {key:'emedic',lbl:'eMedic / SIRS'},
    {key:'tampilPos',lbl:'Point of Sale (POS)'},
    {key:'bolehEntryTopup',lbl:'Entry Top-up Saldo'},
    {key:'melihatDataPegawaiLain',lbl:'Lihat Data Pegawai Lain'},
    {key:'melihatSemuaSurat',lbl:'Lihat Semua Surat'},
    {key:'melihatSemuaSop',lbl:'Lihat Semua SOP'}
];

document.addEventListener('DOMContentLoaded',function(){
    /* Terapkan label multi-bahasa */
    document.getElementById('judulHalaman').textContent=AIS2.L('GrupPengguna','Grup Pengguna (Role)');
    document.getElementById('judulBreadcrumb').textContent=AIS2.L('GrupPengguna','Grup Pengguna');
    document.getElementById('deskripsiHalaman').textContent=AIS2.L('GrupPenggunaDeskripsi','Kelola grup/peran pengguna beserta hak akses modul');
    document.getElementById('btnTambahLabel').textContent=AIS2.L('TambahGrup','Tambah Grup');
    document.getElementById('lblCariGrup').textContent=AIS2.L('CariGrup','Cari Grup');
    document.getElementById('btnCariLabel').textContent=AIS2.L('Cari','Cari');

    tbl=AIS2.Table.init({
        container:'#tabelGrup',modul:'pengaturan',svc:'grup_list',
        bisa:{tambah:false,ubah:true,hapus:false},
        kolom:[
            {field:'roleId',label:AIS2.L('KodeGrup','Kode Grup'),lebar:'110px',sort:true},
            {field:'roleName',label:AIS2.L('NamaGrup','Nama Grup'),sort:true},
            {field:'kode',label:AIS2.L('Kode','Kode'),lebar:'90px',sembunyiMobile:true},
            {field:'jumlahUser',label:AIS2.L('JumlahUser','Jml User'),lebar:'80px'},
            {field:'aktif',label:AIS2.L('Status','Status'),lebar:'75px',render:function(v){return AIS2.Badge.aktif(v);}}
        ],
        renderAksi:function(row){
            return '<div class="td-actions">'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="bukaAkses(\''+row.roleId+'\',\''+row.roleName+'\')" title="'+AIS2.L('HakAkses','Hak Akses Modul')+'"><i class="fa-solid fa-shield-halved"></i></button>'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="bukaForm('+JSON.stringify(row).replace(/"/g,'&quot;')+')" title="'+AIS2.L('Ubah','Ubah')+'"><i class="fa-solid fa-pen"></i></button>'
                +'</div>';
        }
    });
});

function terapkan(){tbl&&tbl.setFilter({nama:document.getElementById('f_nama').value,aktif:document.getElementById('f_aktif').value});}
function bukaTambah(){bukaForm({});}

function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var chk=function(v){return v?'checked':''};
    var html='<input type="hidden" name="roleIdOld" value="'+esc(d.roleId)+'">'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>'+AIS2.L('KodeGrup','Kode Grup')+' <span class="req">*</span></label>'
        +'<input class="ais2-input" name="roleId" value="'+esc(d.roleId)+'" required placeholder="am, mhs, Dosen..."></div>'
        +'<div class="ais2-field"><label>'+AIS2.L('NamaGrup','Nama Grup')+' <span class="req">*</span></label>'
        +'<input class="ais2-input" name="roleName" value="'+esc(d.roleName)+'" required></div>'
        +'<div class="ais2-field"><label>'+AIS2.L('Kode','Kode')+'</label>'
        +'<input class="ais2-input" name="kode" value="'+esc(d.kode)+'"></div>'
        +'</div>'
        +'<div style="margin-top:14px;display:flex;gap:16px;flex-wrap:wrap">'
        +'<label class="ais2-switch"><input type="checkbox" name="aktif" value="true" '+chk(d.aktif!==false)+'><span>'+AIS2.L('Aktif','Aktif')+'</span></label>'
        +'</div>';
    var modal=AIS2.Modal.buka({
        judul:d.roleId?AIS2.L('UbahGrup','Ubah Grup Pengguna'):AIS2.L('TambahGrup','Tambah Grup Pengguna'),
        ikon:'fa-user-tag',ukuran:'',konten:html,
        tombol:[
            {label:AIS2.L('Simpan','Simpan'),kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.type==='checkbox'?(el.checked?'true':'false'):el.value;});
                if(!data.roleId||!data.roleName){AIS2.Toast.peringatan(AIS2.L('WajibDiisi','Kode dan Nama wajib diisi'));return;}
                AIS2.Api.post('pengaturan',d.roleId?'grup_ubah':'grup_tambah',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||AIS2.L('Tersimpan','Tersimpan'));AIS2.Modal.tutup();tbl&&tbl.refresh();}
                    else AIS2.Toast.error((r&&r.msg)||AIS2.L('Gagal','Gagal'));
                });
            }},
            {label:AIS2.L('Tutup','Tutup'),kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}

function bukaAkses(roleId, roleName){
    document.getElementById('aksesRoleId').value=roleId;
    document.getElementById('aksesModalJudul').textContent=AIS2.L('HakAkses','Hak Akses Modul')+' — '+roleName;
    /* Muat flag dari server */
    AIS2.Api.get('pengaturan','grup_detail',{roleId:roleId}).then(function(r){
        var d=r&&r.data?r.data:{};
        var html='';
        MODUL_FLAGS.forEach(function(m){
            var checked=d[m.key]?'checked':'';
            html+='<label style="display:flex;align-items:center;gap:8px;padding:8px 12px;background:var(--ais2-bg);border-radius:8px;cursor:pointer;border:1px solid var(--ais2-border)">'
                +'<input type="checkbox" name="flag_'+m.key+'" value="true" '+checked+' style="width:15px;height:15px">'
                +'<span style="font-size:13px;color:var(--ais2-text)">'+AIS2.L(m.key,m.lbl)+'</span></label>';
        });
        document.getElementById('modulGrid').innerHTML=html;
    });
    document.getElementById('aksesModulModal').style.display='block';
    document.body.style.overflow='hidden';
}

function tutupAksesModal(){
    document.getElementById('aksesModulModal').style.display='none';
    document.body.style.overflow='';
}

function simpanAkses(){
    var roleId=document.getElementById('aksesRoleId').value;
    var data={roleId:roleId};
    document.querySelectorAll('#modulGrid input[type=checkbox]').forEach(function(cb){
        var key=cb.name.replace('flag_','');
        data[key]=cb.checked?'true':'false';
    });
    AIS2.Api.post('pengaturan','grup_akses_simpan',data).then(function(r){
        if(r&&r.ok){AIS2.Toast.sukses(r.msg||AIS2.L('HakAksesTersimpan','Hak akses berhasil disimpan'));tutupAksesModal();}
        else AIS2.Toast.error((r&&r.msg)||AIS2.L('Gagal','Gagal menyimpan'));
    });
}

document.addEventListener('keydown',function(e){if(e.key==='Enter'&&e.target.tagName==='INPUT')terapkan();});
document.getElementById('aksesModulModal').addEventListener('click',function(e){if(e.target===this)tutupAksesModal();});
</script>
</body></html>
