<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Kurikulum OBE</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-bullseye"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Kurikulum OBE</span></div>
    <div class="ais2-ph-title">Kurikulum OBE (Outcome-Based Education)</div>
    <div class="ais2-ph-sub">Kelola CPL, CPMK, Bahan Kajian, dan PIKOBE per program studi</div>
  </div>
</div>

<!-- OBE Hub Cards -->
<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));gap:16px;margin-bottom:24px">
  <div class="ais2-hub-card" onclick="window.parent.postMessage({goto:'pendataan&p=cpl'},'*')">
    <div class="ais2-hub-icon" style="background:linear-gradient(135deg,#667eea,#764ba2)"><i class="fa-solid fa-trophy"></i></div>
    <div class="ais2-hub-title">CPL — Capaian Pembelajaran Lulusan</div>
    <div class="ais2-hub-sub">Kelola CPL dan Profil Lulusan per Prodi</div>
  </div>
  <div class="ais2-hub-card" onclick="window.parent.postMessage({goto:'pendataan&p=cpmk'},'*')">
    <div class="ais2-hub-icon" style="background:linear-gradient(135deg,#f093fb,#f5576c)"><i class="fa-solid fa-check-to-slot"></i></div>
    <div class="ais2-hub-title">CPMK — Capaian Pembelajaran MK</div>
    <div class="ais2-hub-sub">Pemetaan CPMK terhadap CPL</div>
  </div>
  <div class="ais2-hub-card" onclick="window.parent.postMessage({goto:'pendataan&p=bahan_kajian'},'*')">
    <div class="ais2-hub-icon" style="background:linear-gradient(135deg,#4facfe,#00f2fe)"><i class="fa-solid fa-magnifying-glass-plus"></i></div>
    <div class="ais2-hub-title">Bahan Kajian</div>
    <div class="ais2-hub-sub">Repository bahan kajian dan sub-CPMK</div>
  </div>
  <div class="ais2-hub-card" onclick="window.parent.postMessage({goto:'pendataan&p=pikobe'},'*')">
    <div class="ais2-hub-icon" style="background:linear-gradient(135deg,#43e97b,#38f9d7)"><i class="fa-solid fa-sitemap"></i></div>
    <div class="ais2-hub-title">PIKOBE — Peta Induk Kurikulum</div>
    <div class="ais2-hub-sub">Peta MK dan hubungan CPL-CPMK visual</div>
  </div>
</div>

<!-- CPL inline list -->
<div class="ais2-card" style="border-radius:14px;background:var(--ais2-card-bg);border:1px solid var(--ais2-border);padding:20px">
  <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:14px">
    <div style="font-size:15px;font-weight:600;color:var(--ais2-text)"><i class="fa-solid fa-trophy" style="color:var(--ais2-pri)"></i> CPL per Program Studi</div>
    <div style="display:flex;gap:8px;align-items:center">
      <select id="selProdi" class="ais2-input" style="width:200px" onchange="muatCPL()"><option value="">-- Pilih Prodi --</option></select>
      <button class="ais2-btn ais2-btn-primary ais2-btn-sm" onclick="tambahCPL()"><i class="fa-solid fa-plus"></i> Tambah CPL</button>
    </div>
  </div>
  <div id="listCPL" style="display:grid;gap:8px">
    <div class="ais2-empty"><i class="fa-solid fa-circle-info"></i><p>Pilih Program Studi untuk melihat CPL</p></div>
  </div>
</div>

<script>
document.addEventListener('DOMContentLoaded',function(){muatProdi();});
function muatProdi(){
    AIS2.Api.get('pendataan','ref_prodi_all',{}).then(function(res){
        if(!res||!res.data)return;
        var sel=document.getElementById('selProdi');
        res.data.forEach(function(p){var o=document.createElement('option');o.value=p.id;o.textContent=p.nama;sel.appendChild(o);});
    });
}
function muatCPL(){
    var prodiId=document.getElementById('selProdi').value;
    if(!prodiId){document.getElementById('listCPL').innerHTML='<div class="ais2-empty"><i class="fa-solid fa-circle-info"></i><p>Pilih Program Studi</p></div>';return;}
    document.getElementById('listCPL').innerHTML='<div class="ais2-empty"><i class="fa-solid fa-spinner fa-spin"></i><p>Memuat CPL...</p></div>';
    AIS2.Api.get('pendataan','cpl_list',{prodiId:prodiId}).then(function(res){
        if(!res||!res.data||!res.data.length){document.getElementById('listCPL').innerHTML='<div class="ais2-empty"><i class="fa-solid fa-trophy"></i><p>Belum ada CPL terdaftar</p></div>';return;}
        var html='';
        res.data.forEach(function(cpl){
            html+='<div style="display:flex;align-items:flex-start;gap:12px;padding:12px 14px;background:var(--ais2-bg);border-radius:10px;border:1px solid var(--ais2-border)">'
                +'<span style="background:var(--ais2-grad);color:#fff;border-radius:6px;padding:2px 8px;font-size:12px;font-weight:700;white-space:nowrap;margin-top:2px">'+cpl.kode+'</span>'
                +'<div style="flex:1"><div style="font-size:13px;font-weight:600;color:var(--ais2-text)">'+cpl.nama+'</div>'
                +'<div style="font-size:12px;color:var(--ais2-text-sec);margin-top:2px">'+cpl.deskripsi+'</div></div>'
                +'<div class="td-actions">'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="ubahCPL(\''+cpl.id+'\')" title="Ubah"><i class="fa-solid fa-pen"></i></button>'
                +'<button class="ais2-btn ais2-btn-danger ais2-btn-sm" onclick="hapusCPL(\''+cpl.id+'\')" title="Hapus"><i class="fa-solid fa-trash"></i></button>'
                +'</div></div>';
        });
        document.getElementById('listCPL').innerHTML=html;
    });
}
function tambahCPL(){
    var prodiId=document.getElementById('selProdi').value;
    if(!prodiId){AIS2.Toast.peringatan('Pilih Program Studi terlebih dahulu');return;}
    bukaFormCPL({prodiId:prodiId});
}
function ubahCPL(id){
    AIS2.Api.get('pendataan','cpl_detail',{id:id}).then(function(r){if(r&&r.data)bukaFormCPL(r.data);});
}
function bukaFormCPL(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<input type="hidden" name="prodiId" value="'+esc(d.prodiId)+'">'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Kode CPL <span class="req">*</span></label><input class="ais2-input" name="kode" value="'+esc(d.kode)+'" required placeholder="CPL-01, A1, P1..."></div>'
        +'<div class="ais2-field ais2-form-full"><label>Nama / Rumusan CPL <span class="req">*</span></label><input class="ais2-input" name="nama" value="'+esc(d.nama)+'" required></div>'
        +'<div class="ais2-field"><label>Kelompok</label><select name="kelompok" class="ais2-input"><option value="">--</option>'
        +['Sikap','Pengetahuan','Keterampilan Umum','Keterampilan Khusus'].map(function(k){return '<option'+(d.kelompok===k?' selected':'')+'>'+k+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Kode Referensi KKNI</label><input class="ais2-input" name="kodeKKNI" value="'+esc(d.kodeKKNI)+'"></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:12px"><label>Deskripsi / Rincian CPL <span class="req">*</span></label><textarea class="ais2-input" name="deskripsi" rows="4" required>'+esc(d.deskripsi)+'</textarea></div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah CPL':'Tambah Capaian Pembelajaran Lulusan',ikon:'fa-trophy',ukuran:'',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.value;});
                if(!data.kode||!data.nama||!data.deskripsi){AIS2.Toast.peringatan('Kode, Nama, dan Deskripsi wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'cpl_ubah':'cpl_tambah',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||'CPL tersimpan');AIS2.Modal.tutup();muatCPL();}
                    else AIS2.Toast.error((r&&r.msg)||'Gagal');
                });
            }},
            {label:'Tutup',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}
function hapusCPL(id){AIS2.Confirm.tampil({judul:'Hapus CPL',pesan:'CPL ini akan dihapus? Pastikan tidak ada CPMK yang masih mengacu ke CPL ini.',tipe:'danger',labelOk:'Hapus',onOk:function(){AIS2.Api.post('pendataan','cpl_hapus',{id:id}).then(function(r){if(r&&r.ok){AIS2.Toast.sukses('CPL dihapus');muatCPL();}else AIS2.Toast.error((r&&r.msg)||'Gagal');});}});}
</script>
<style>
.ais2-hub-card{background:var(--ais2-card-bg);border:1px solid var(--ais2-border);border-radius:14px;padding:20px 18px;cursor:pointer;transition:.2s;display:flex;flex-direction:column;gap:12px}
.ais2-hub-card:hover{transform:translateY(-2px);box-shadow:0 6px 24px rgba(0,0,0,.12);border-color:var(--ais2-pri)}
.ais2-hub-icon{width:48px;height:48px;border-radius:12px;display:flex;align-items:center;justify-content:center;font-size:20px;color:#fff}
.ais2-hub-title{font-size:14px;font-weight:600;color:var(--ais2-text)}
.ais2-hub-sub{font-size:12px;color:var(--ais2-text-sec)}
</style>
</body></html>
