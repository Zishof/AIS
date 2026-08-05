<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>KRS Mahasiswa</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">

<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-list-check"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>KRS Mahasiswa</span></div>
    <div class="ais2-ph-title">KRS Mahasiswa (Admin)</div>
    <div class="ais2-ph-sub">Pantau dan kelola Kartu Rencana Studi mahasiswa seluruh angkatan</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-outline" onclick="ekspor()"><i class="fa-solid fa-file-excel"></i> Ekspor</button>
    <button class="ais2-btn ais2-btn-primary" onclick="ambilKRS()"><i class="fa-solid fa-plus"></i> Ambilkan KRS</button>
  </div>
</div>

<div class="ais2-filter-card">
  <div class="ais2-filter-head">
    <i class="fa-solid fa-filter" style="color:var(--ais2-pri2)"></i>
    <span class="ais2-filter-title">Filter</span>
    <button class="ais2-btn ais2-btn-sm ais2-btn-outline" onclick="toggleLanjut()"><i class="fa-solid fa-sliders"></i> Lanjutan</button>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
    <button class="ais2-btn ais2-btn-sm ais2-btn-outline" onclick="reset()"><i class="fa-solid fa-rotate-left"></i></button>
  </div>
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>NIM</label><input type="text" id="f_nim" placeholder="NIM mahasiswa..."></div>
    <div class="ais2-filter-field"><label>Nama Mahasiswa</label><input type="text" id="f_nama" placeholder="Cari nama..."></div>
    <div class="ais2-filter-field"><label>Dosen PA</label><input type="text" id="f_dosen" placeholder="Nama/kode dosen..."></div>
    <div class="ais2-filter-field"><label>Tahun Akademik</label><select id="f_ta"><option value="">Semua TA</option></select></div>
    <div class="ais2-filter-field"><label>Semester</label>
      <select id="f_smt"><option value="">Semua</option><option value="1">Ganjil</option><option value="2">Genap</option><option value="3">SP</option></select>
    </div>
  </div>
  <div class="ais2-filter-advanced" id="filterLanjut">
    <div class="ais2-filter-row" style="margin-bottom:12px">
      <div class="ais2-filter-field"><label>Angkatan</label><input type="number" id="f_angkatan" placeholder="2022"></div>
      <div class="ais2-filter-field"><label>Fakultas</label><select id="f_fak"><option value="">Semua</option></select></div>
      <div class="ais2-filter-field"><label>Program Studi</label><select id="f_prodi"><option value="">Semua</option></select></div>
      <div class="ais2-filter-field"><label>Program</label>
        <select id="f_program"><option value="">Semua</option><option>Reguler</option><option>Karyawan</option></select>
      </div>
      <div class="ais2-filter-field"><label>SKS Min</label><input type="number" id="f_sksMin" style="width:80px"></div>
      <div class="ais2-filter-field"><label>SKS Maks</label><input type="number" id="f_sksMaks" style="width:80px"></div>
    </div>
    <div class="ais2-filter-checks">
      <label class="ais2-filter-check"><input type="checkbox" id="fc_sp"> Semester Pendek saja</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_bukanSP"> Bukan Semester Pendek</label>
    </div>
  </div>
</div>

<div id="tabelKRS"></div>

<script>
var tbl=null;
document.addEventListener('DOMContentLoaded',function(){
    muatTA(); muatFakultas();
    tbl=AIS2.Table.init({
        container:'#tabelKRS', modul:'pendataan', svc:'krs_list',
        bisa:{tambah:false,ubah:false,hapus:false},
        sortDefault:'nama',
        kolom:[
            {field:'nim',    label:'NIM', lebar:'110px'},
            {field:'nama',   label:'Nama Mahasiswa', sort:true},
            {field:'tahunAkademik', label:'TA', lebar:'80px'},
            {field:'semester',      label:'Smt', lebar:'45px'},
            {field:'sks',    label:'SKS', lebar:'50px'},
            {field:'ip',     label:'IP', lebar:'50px', sembunyiMobile:true},
            {field:'ipk',    label:'IPK', lebar:'50px', sembunyiMobile:true},
            {field:'info',   label:'Info / Catatan', sembunyiMobile:true}
        ],
        renderAksi:function(row){
            return '<div class="td-actions">'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="lihatDetailKRS(\''+row.id+'\')" title="Lihat Detail KRS"><i class="fa-solid fa-eye"></i></button>'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="cetakKRS(\''+row.id+'\')" title="Cetak KRS"><i class="fa-solid fa-print"></i></button>'
                +'<button class="ais2-btn ais2-btn-success ais2-btn-sm" onclick="setujuiKRS(\''+row.id+'\')" title="Setujui KRS"><i class="fa-solid fa-check"></i></button>'
                +'</div>';
        }
    });
});
function terapkan(){
    tbl&&tbl.setFilter({
        nim:document.getElementById('f_nim').value,nama:document.getElementById('f_nama').value,
        dosen:document.getElementById('f_dosen').value,ta:document.getElementById('f_ta').value,
        smt:document.getElementById('f_smt').value,angkatan:document.getElementById('f_angkatan').value,
        fakultas:document.getElementById('f_fak').value,prodi:document.getElementById('f_prodi').value,
        program:document.getElementById('f_program').value,sksMin:document.getElementById('f_sksMin').value,
        sksMaks:document.getElementById('f_sksMaks').value,
        hanyaSP:document.getElementById('fc_sp').checked?'1':'',
        bukanSP:document.getElementById('fc_bukanSP').checked?'1':''
    });
}
function reset(){
    ['f_nim','f_nama','f_dosen','f_angkatan','f_sksMin','f_sksMaks'].forEach(function(id){var el=document.getElementById(id);if(el)el.value='';});
    ['f_ta','f_smt','f_fak','f_prodi','f_program'].forEach(function(id){var el=document.getElementById(id);if(el)el.selectedIndex=0;});
    terapkan();
}
function toggleLanjut(){document.getElementById('filterLanjut').classList.toggle('show');}
function muatTA(){
    AIS2.Api.get('pendataan','ref_tahun_akademik',{}).then(function(res){
        if(!res||!res.data)return;
        var sel=document.getElementById('f_ta');
        res.data.forEach(function(t){var o=document.createElement('option');o.value=t.id;o.textContent=t.label;sel.appendChild(o);});
    });
}
function muatFakultas(){
    AIS2.Api.get('pendataan','ref_fakultas',{}).then(function(res){
        if(!res||!res.data)return;
        var sel=document.getElementById('f_fak');
        res.data.forEach(function(f){var o=document.createElement('option');o.value=f.id;o.textContent=f.nama;sel.appendChild(o);});
    });
}
function lihatDetailKRS(id){
    AIS2.Modal.buka({
        judul:'Detail KRS',ikon:'fa-list-check',ukuran:'lg',
        konten:'<div class="ais2-empty"><i class="fa-solid fa-spinner fa-spin"></i><p>Memuat detail KRS...</p></div>',
        tombol:[{label:'Tutup',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}]
    });
    AIS2.Api.get('pendataan','krs_detail',{id:id}).then(function(r){
        if(!r||!r.data){AIS2.Toast.error('Gagal memuat data');return;}
        /* render detail */
    });
}
function cetakKRS(id){window.open(ROOT+'/baru2?modul=pendataan&svc=cetak_krs&id='+encodeURIComponent(id),'_blank');}
function setujuiKRS(id){
    AIS2.Confirm.tampil({
        judul:'Setujui KRS',
        pesan:'Setujui seluruh KRS mahasiswa ini untuk semester aktif?',
        tipe:'info',labelOk:'Ya, Setujui',
        onOk:function(){
            AIS2.Api.post('pendataan','krs_setujui',{id:id}).then(function(r){
                if(r&&r.ok){AIS2.Toast.sukses(r.msg||'KRS disetujui');tbl&&tbl.refresh();}
                else AIS2.Toast.error((r&&r.msg)||'Gagal');
            });
        }
    });
}
function ambilKRS(){AIS2.Toast.info('Fitur ambilkan KRS — pilih mahasiswa terlebih dahulu.');}
function ekspor(){window.open(ROOT+'/baru2?modul=pendataan&svc=krs_ekspor','_blank');}
document.addEventListener('keydown',function(e){if(e.key==='Enter'&&e.target.tagName==='INPUT')terapkan();});
</script>
</body></html>
