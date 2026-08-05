<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Pengajuan Mahasiswa</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-paper-plane"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Pengajuan Mahasiswa</span></div>
    <div class="ais2-ph-title">Pengajuan &amp; Permohonan Mahasiswa</div>
    <div class="ais2-ph-sub">Kelola berbagai pengajuan dan permohonan non-standar mahasiswa</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Tambah Pengajuan</button>
  </div>
</div>
<div class="ais2-filter-card">
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>Mahasiswa (NIM/Nama)</label><input type="text" id="f_mhs" placeholder="Cari mahasiswa..."></div>
    <div class="ais2-filter-field"><label>Jenis Pengajuan</label>
      <select id="f_jenis"><option value="">Semua</option>
        <option>Pindah Prodi</option><option>Perpindahan Kelas</option><option>Cuti Akademik</option>
        <option>Pengunduran Diri</option><option>Aktif Kembali</option>
        <option>Dispensasi Kehadiran</option><option>Keringanan Biaya</option><option>Lainnya</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Status</label>
      <select id="f_status"><option value="">Semua</option>
        <option>Menunggu</option><option>Diproses</option><option>Disetujui</option><option>Ditolak</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Tahun Akademik</label><select id="f_ta"><option value="">Semua TA</option></select></div>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" style="align-self:flex-end" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
  </div>
</div>
<div id="tabelPjn"></div>
<script>
var tbl=null;
var WARNA={Menunggu:'warning',Diproses:'info',Disetujui:'success',Ditolak:'danger'};
document.addEventListener('DOMContentLoaded',function(){
    muatTA();
    tbl=AIS2.Table.init({
        container:'#tabelPjn',modul:'pendataan',svc:'pengajuan_mhs_list',
        bisa:{tambah:false,ubah:false,hapus:false},
        kolom:[
            {field:'nomorPengajuan',label:'No. Pengajuan',lebar:'120px'},
            {field:'nim',label:'NIM',lebar:'100px'},
            {field:'nama',label:'Nama Mahasiswa',sort:true},
            {field:'jenisPengajuan',label:'Jenis',lebar:'140px',sembunyiMobile:true},
            {field:'tanggal',label:'Tanggal',lebar:'100px',sembunyiMobile:true},
            {field:'status',label:'Status',lebar:'90px',render:function(v){return AIS2.Badge.kustom(v,WARNA);}}
        ],
        renderAksi:function(row){
            return '<div class="td-actions">'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="bukaDetail(\''+row.id+'\')" title="Lihat Detail"><i class="fa-solid fa-eye"></i></button>'
                +(row.status==='Menunggu'||row.status==='Diproses'?'<button class="ais2-btn ais2-btn-success ais2-btn-sm" onclick="setujui(\''+row.id+'\',true)" title="Setujui"><i class="fa-solid fa-check"></i></button>'
                +'<button class="ais2-btn ais2-btn-danger ais2-btn-sm" onclick="setujui(\''+row.id+'\',false)" title="Tolak"><i class="fa-solid fa-xmark"></i></button>':'')
                +'</div>';
        }
    });
});
function terapkan(){tbl&&tbl.setFilter({mhs:document.getElementById('f_mhs').value,jenis:document.getElementById('f_jenis').value,status:document.getElementById('f_status').value,ta:document.getElementById('f_ta').value});}
function muatTA(){AIS2.Api.get('pendataan','ref_tahun_akademik',{}).then(function(res){if(!res||!res.data)return;var sel=document.getElementById('f_ta');res.data.forEach(function(t){var o=document.createElement('option');o.value=t.id;o.textContent=t.label;sel.appendChild(o);});});}
function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Mahasiswa <span class="req">*</span></label><input class="ais2-input" name="mahasiswaRef" value="'+esc(d.nim||d.nama)+'" required placeholder="NIM atau nama..."></div>'
        +'<div class="ais2-field"><label>Jenis Pengajuan <span class="req">*</span></label><select name="jenisPengajuan" class="ais2-input" required><option value="">-- Pilih --</option>'
        +['Pindah Prodi','Perpindahan Kelas','Cuti Akademik','Pengunduran Diri','Aktif Kembali','Dispensasi Kehadiran','Keringanan Biaya','Lainnya'].map(function(j){return '<option'+(d.jenisPengajuan===j?' selected':'')+'>'+j+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Tahun Akademik</label><select name="tahunAkademik" class="ais2-input"><option value="">--</option></select></div>'
        +'<div class="ais2-field"><label>Semester</label><select name="semester" class="ais2-input"><option value="">--</option><option value="1">Ganjil</option><option value="2">Genap</option></select></div>'
        +'<div class="ais2-field"><label>Tanggal Pengajuan</label><input class="ais2-input" type="date" name="tanggal" value="'+esc(d.tanggal)+'"></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:12px"><label>Alasan / Keperluan <span class="req">*</span></label><textarea class="ais2-input" name="alasan" rows="4" required>'+esc(d.alasan)+'</textarea></div>'
        +'<div class="ais2-field" style="margin-top:8px"><label>Berkas Pendukung (URL / No. Berkas)</label><input class="ais2-input" name="berkas" value="'+esc(d.berkas)+'" placeholder="Link atau nomor berkas..."></div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Detail Pengajuan':'Tambah Pengajuan Mahasiswa',ikon:'fa-paper-plane',ukuran:'',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.value;});
                if(!data.mahasiswaRef||!data.jenisPengajuan||!data.alasan){AIS2.Toast.peringatan('Mahasiswa, Jenis, dan Alasan wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'pengajuan_mhs_ubah':'pengajuan_mhs_tambah',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Tersimpan');AIS2.Modal.tutup();tbl&&tbl.refresh();}
                    else AIS2.Toast.error((r&&r.msg)||'Gagal');
                });
            }},
            {label:'Tutup',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}
function bukaDetail(id){AIS2.Api.get('pendataan','pengajuan_mhs_detail',{id:id}).then(function(r){if(r&&r.data)bukaForm(r.data);});}
function setujui(id,ok){
    AIS2.Confirm.tampil({
        judul:ok?'Setujui Pengajuan':'Tolak Pengajuan',
        pesan:ok?'Yakin menyetujui pengajuan ini?':'Yakin menolak pengajuan ini?',
        tipe:ok?'info':'danger',labelOk:ok?'Ya, Setujui':'Ya, Tolak',
        onOk:function(){
            AIS2.Api.post('pendataan','pengajuan_mhs_keputusan',{id:id,setuju:ok?'true':'false'}).then(function(r){
                if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Keputusan disimpan');tbl&&tbl.refresh();}
                else AIS2.Toast.error((r&&r.msg)||'Gagal');
            });
        }
    });
}
document.addEventListener('keydown',function(e){if(e.key==='Enter'&&e.target.tagName==='INPUT')terapkan();});
</script>
</body></html>
