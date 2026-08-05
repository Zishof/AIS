<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Cuti Mahasiswa</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">

<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-calendar-xmark"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Cuti Mahasiswa</span></div>
    <div class="ais2-ph-title">Pendaftaran Cuti Mahasiswa</div>
    <div class="ais2-ph-sub">Kelola permohonan dan persetujuan cuti mahasiswa</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Daftarkan Cuti</button>
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
    <div class="ais2-filter-field"><label>NIM</label><input type="text" id="f_nim" placeholder="Cari NIM..."></div>
    <div class="ais2-filter-field"><label>Nama Mahasiswa</label><input type="text" id="f_nama" placeholder="Cari nama..."></div>
    <div class="ais2-filter-field"><label>Fakultas</label><select id="f_fak"><option value="">Semua</option></select></div>
    <div class="ais2-filter-field"><label>Program Studi</label><select id="f_prodi"><option value="">Semua</option></select></div>
    <div class="ais2-filter-field"><label>Tahun Akademik</label><select id="f_ta"><option value="">Semua TA</option></select></div>
    <div class="ais2-filter-field"><label>Semester</label>
      <select id="f_smt">
        <option value="">Semua</option>
        <option value="1">Ganjil</option><option value="2">Genap</option><option value="3">Semester Pendek</option>
      </select>
    </div>
  </div>
</div>

<div id="tabelCuti"></div>

<script>
var tbl=null;
document.addEventListener('DOMContentLoaded',function(){
    muatFakultas();
    muatTA();
    tbl=AIS2.Table.init({
        container:'#tabelCuti', modul:'pendataan', svc:'cuti_list',
        bisa:{tambah:false,ubah:true,hapus:true},
        kolom:[
            {field:'nim',          label:'NIM', lebar:'110px'},
            {field:'nama',         label:'Nama Mahasiswa', sort:true},
            {field:'prodi',        label:'Prodi', sembunyiMobile:true},
            {field:'tahunAkademik',label:'TA', lebar:'80px'},
            {field:'semester',     label:'Smt', lebar:'50px'},
            {field:'keterangan',   label:'Alasan Cuti'},
            {field:'tanggal',      label:'Tanggal', lebar:'90px', sembunyiMobile:true},
            {field:'persetujuan',  label:'Disetujui', lebar:'90px',
             render:function(v){
                return v
                    ? '<span class="ais2-badge ais2-badge-green"><i class="fa-solid fa-check"></i> Disetujui</span>'
                    : '<span class="ais2-badge ais2-badge-yellow"><i class="fa-solid fa-clock"></i> Menunggu</span>';
             }}
        ],
        onUbah:function(d){bukaForm(d);},
        onHapus:function(id,cb){AIS2.Api.post('pendataan','cuti_hapus',{id:id}).then(function(r){cb(r&&r.ok,r&&r.msg);});},
        renderAksi:function(row){
            return '<div class="td-actions">'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="bukaFormById(\''+row.id+'\')" title="Ubah"><i class="fa-solid fa-pen"></i></button>'
                +'<button class="ais2-btn '+(row.persetujuan?'ais2-btn-outline':'ais2-btn-success')+' ais2-btn-sm" onclick="setujui(\''+row.id+'\','+!row.persetujuan+')" title="Setujui/Batalkan">'
                +'<i class="fa-solid '+(row.persetujuan?'fa-xmark':'fa-check')+'"></i></button>'
                +'</div>';
        }
    });
});

function terapkan(){
    tbl&&tbl.setFilter({
        nim:document.getElementById('f_nim').value,
        nama:document.getElementById('f_nama').value,
        fakultas:document.getElementById('f_fak').value,
        prodi:document.getElementById('f_prodi').value,
        ta:document.getElementById('f_ta').value,
        smt:document.getElementById('f_smt').value
    });
}
function reset(){
    ['f_nim','f_nama'].forEach(function(id){var el=document.getElementById(id);if(el)el.value='';});
    ['f_fak','f_prodi','f_ta','f_smt'].forEach(function(id){var el=document.getElementById(id);if(el)el.selectedIndex=0;});
    terapkan();
}
function muatFakultas(){
    AIS2.Api.get('pendataan','ref_fakultas',{}).then(function(res){
        if(!res||!res.data)return;
        var sel=document.getElementById('f_fak');
        res.data.forEach(function(f){var o=document.createElement('option');o.value=f.id;o.textContent=f.nama;sel.appendChild(o);});
    });
}
function muatTA(){
    AIS2.Api.get('pendataan','ref_tahun_akademik',{}).then(function(res){
        if(!res||!res.data)return;
        var sel=document.getElementById('f_ta');
        res.data.forEach(function(t){var o=document.createElement('option');o.value=t.id;o.textContent=t.label;sel.appendChild(o);});
    });
}
function bukaTambah(){bukaForm({});}
function bukaFormById(id){
    AIS2.Api.get('pendataan','cuti_detail',{id:id}).then(function(r){bukaForm(r&&r.data?r.data:{});});
}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-section-title">Data Permohonan Cuti</div>'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Mahasiswa (NIM / Nama) <span class="req">*</span></label><input class="ais2-input" name="mahasiswaRef" value="'+esc(d.nimMahasiswa||d.namaMahasiswa)+'" required placeholder="Ketik NIM atau nama..."></div>'
        +'<div class="ais2-field"><label>Kode / Nomor Cuti</label><input class="ais2-input" name="kode" value="'+esc(d.kode)+'"></div>'
        +'<div class="ais2-field"><label>Tahun Akademik <span class="req">*</span></label><select name="tahunAkademik" class="ais2-input" required><option value="">-- Pilih --</option></select></div>'
        +'<div class="ais2-field"><label>Semester</label><select name="ganjilGenap" class="ais2-input"><option value="">--</option><option value="1">Ganjil</option><option value="2">Genap</option></select></div>'
        +'<div class="ais2-field"><label>Tanggal Permohonan</label><input class="ais2-input" type="date" name="tanggal" value="'+esc(d.tanggal)+'"></div>'
        +'<div class="ais2-field ais2-field-check"><input type="checkbox" name="semesterPendek" '+(d.semesterPendek?'checked':'')+'><label>Semester Pendek</label></div>'
        +'<div class="ais2-field ais2-field-check"><input type="checkbox" name="persetujuan" '+(d.persetujuan?'checked':'')+'><label>Sudah Disetujui</label></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:14px"><label>Alasan / Keterangan Cuti <span class="req">*</span></label>'
        +'<textarea class="ais2-input" name="keterangan" rows="4" required>'+esc(d.keterangan)+'</textarea></div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Permohonan Cuti':'Daftar Cuti Mahasiswa',ikon:'fa-calendar-xmark',ukuran:'',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.type==='checkbox'?(el.checked?'true':'false'):el.value;});
                if(!data.mahasiswaRef||!data.keterangan){AIS2.Toast.peringatan('Mahasiswa dan Alasan wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'cuti_ubah':'cuti_tambah',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Tersimpan');AIS2.Modal.tutup();tbl&&tbl.refresh();}
                    else AIS2.Toast.error((r&&r.msg)||'Gagal');
                });
            }},
            {label:'Tutup',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}
function setujui(id,setuju){
    AIS2.Api.post('pendataan','cuti_setujui',{id:id,setuju:setuju?'true':'false'}).then(function(r){
        if(r&&r.ok){AIS2.Toast.sukses(setuju?'Cuti disetujui':'Persetujuan dibatalkan');tbl&&tbl.refresh();}
        else AIS2.Toast.error((r&&r.msg)||'Gagal');
    });
}
document.addEventListener('keydown',function(e){if(e.key==='Enter'&&e.target.tagName==='INPUT')terapkan();});
</script>
</body></html>
