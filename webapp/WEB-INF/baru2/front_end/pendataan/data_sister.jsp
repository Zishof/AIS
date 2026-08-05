<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Data SISTER</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-arrows-rotate"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Data SISTER</span></div>
    <div class="ais2-ph-title">Sinkronisasi SISTER / Feeder</div>
    <div class="ais2-ph-sub">Kelola data dan sinkronisasi dosen dengan sistem SISTER Kemdikbud</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-success" onclick="sinkronSekarang()"><i class="fa-solid fa-rotate"></i> Sinkron Sekarang</button>
    <button class="ais2-btn ais2-btn-outline" onclick="lihatLog()"><i class="fa-solid fa-list-ul"></i> Log Sinkron</button>
  </div>
</div>

<!-- Status sinkronisasi -->
<div class="ais2-alert" id="alertSync" style="display:none;margin-bottom:16px;padding:12px 16px;border-radius:10px;background:var(--ais2-info-bg,#e8f4ff);border:1px solid var(--ais2-info,#2196f3);color:var(--ais2-text)">
  <i class="fa-solid fa-circle-info"></i> <span id="alertMsg">Memeriksa status sinkronisasi...</span>
</div>

<div class="ais2-tab-bar">
  <button class="ais2-tab-btn active" id="tab_dosen" onclick="gTab('dosen')"><i class="fa-solid fa-chalkboard-user"></i> Data Dosen</button>
  <button class="ais2-tab-btn" id="tab_matkul" onclick="gTab('matkul')"><i class="fa-solid fa-book"></i> Mata Kuliah</button>
  <button class="ais2-tab-btn" id="tab_riwayat" onclick="gTab('riwayat')"><i class="fa-solid fa-clock-rotate-left"></i> Riwayat Sync</button>
</div>

<div class="ais2-filter-card">
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>NIDN / NIP</label><input type="text" id="f_nidn" placeholder="NIDN atau NIP..."></div>
    <div class="ais2-filter-field"><label>Nama Dosen</label><input type="text" id="f_nama" placeholder="Nama dosen..."></div>
    <div class="ais2-filter-field"><label>Status SISTER</label>
      <select id="f_status"><option value="">Semua</option>
        <option>Tersinkron</option><option>Belum Sinkron</option><option>Konflik Data</option><option>Tidak Ditemukan</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Fakultas</label><select id="f_fak"><option value="">Semua</option></select></div>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" style="align-self:flex-end" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
  </div>
</div>

<div id="tabelSister"></div>

<script>
var tbl=null, tabAktif='dosen';
var WARNA_SYNC={Tersinkron:'success','Belum Sinkron':'warning','Konflik Data':'danger','Tidak Ditemukan':'secondary'};
document.addEventListener('DOMContentLoaded',function(){
    muatFak();
    muatStatusSync();
    tbl=AIS2.Table.init({
        container:'#tabelSister',modul:'pendataan',svc:'sister_list',
        bisa:{tambah:false,ubah:false,hapus:false},
        kolom:[
            {field:'nidn',label:'NIDN',lebar:'110px'},
            {field:'nama',label:'Nama Dosen',sort:true},
            {field:'jabatan',label:'Jabatan Fungsional',sembunyiMobile:true},
            {field:'statusSister',label:'Status SISTER',lebar:'140px',render:function(v){return AIS2.Badge.kustom(v,WARNA_SYNC);}},
            {field:'terakhirSync',label:'Terakhir Sync',lebar:'130px',sembunyiMobile:true},
            {field:'keterangan',label:'Keterangan',sembunyiMobile:true}
        ],
        renderAksi:function(row){
            return '<div class="td-actions">'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="sinkronSatu(\''+row.id+'\')" title="Sync data ini"><i class="fa-solid fa-rotate"></i></button>'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="lihatDetail(\''+row.id+'\')" title="Lihat Detail"><i class="fa-solid fa-eye"></i></button>'
                +'</div>';
        }
    });
});
function gTab(t){
    tabAktif=t;
    ['dosen','matkul','riwayat'].forEach(function(k){document.getElementById('tab_'+k).classList.toggle('active',k===t);});
    terapkan();
}
function terapkan(){tbl&&tbl.setFilter({tab:tabAktif,nidn:document.getElementById('f_nidn').value,nama:document.getElementById('f_nama').value,status:document.getElementById('f_status').value,fak:document.getElementById('f_fak').value});}
function muatFak(){AIS2.Api.get('pendataan','ref_fakultas',{}).then(function(res){if(!res||!res.data)return;var sel=document.getElementById('f_fak');res.data.forEach(function(f){var o=document.createElement('option');o.value=f.id;o.textContent=f.nama;sel.appendChild(o);});});}
function muatStatusSync(){
    AIS2.Api.get('pendataan','sister_status',{}).then(function(r){
        var al=document.getElementById('alertSync');
        var msg=document.getElementById('alertMsg');
        if(r&&r.msg){al.style.display='flex';al.style.gap='8px';al.style.alignItems='center';msg.textContent=r.msg;}
    });
}
function sinkronSekarang(){
    AIS2.Confirm.tampil({judul:'Sinkronisasi SISTER',pesan:'Mulai sinkronisasi data dosen dengan SISTER Kemdikbud? Proses ini bisa memerlukan waktu beberapa menit.',tipe:'info',labelOk:'Ya, Sinkronkan',onOk:function(){
        document.getElementById('alertSync').style.display='flex';
        document.getElementById('alertMsg').textContent='Proses sinkronisasi berjalan...';
        AIS2.Api.post('pendataan','sister_sync_all',{}).then(function(r){
            if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Sinkronisasi selesai');tbl&&tbl.refresh();}
            else AIS2.Toast.error((r&&r.msg)||'Sinkronisasi gagal');
            muatStatusSync();
        });
    }});
}
function sinkronSatu(id){
    AIS2.Api.post('pendataan','sister_sync_satu',{id:id}).then(function(r){
        if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Data tersinkron');tbl&&tbl.refresh();}
        else AIS2.Toast.error((r&&r.msg)||'Gagal sinkron');
    });
}
function lihatDetail(id){AIS2.Toast.info('Memuat detail SISTER untuk dosen ini...');}
function lihatLog(){AIS2.Toast.info('Fitur log sinkronisasi — lihat tab Riwayat Sync.');}
document.addEventListener('keydown',function(e){if(e.key==='Enter'&&e.target.tagName==='INPUT')terapkan();});
</script>
</body></html>
