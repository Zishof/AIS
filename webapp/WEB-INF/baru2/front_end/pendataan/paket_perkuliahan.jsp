<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Paket Perkuliahan</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-layer-group"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Paket Perkuliahan</span></div>
    <div class="ais2-ph-title">Paket Perkuliahan</div>
    <div class="ais2-ph-sub">Kelola paket / bundel mata kuliah per angkatan dan semester</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Buat Paket</button>
  </div>
</div>
<div class="ais2-tab-bar">
  <button class="ais2-tab-btn active" id="tab_paket" onclick="gTab('paket')">Paket Perkuliahan</button>
  <button class="ais2-tab-btn" id="tab_isi" onclick="gTab('isi')">Isi Paket (MK)</button>
</div>
<div class="ais2-filter-card">
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>Nama Paket</label><input type="text" id="f_nama" placeholder="Cari paket..."></div>
    <div class="ais2-filter-field"><label>Angkatan</label><input type="number" id="f_angkatan" placeholder="2022" style="width:90px"></div>
    <div class="ais2-filter-field"><label>Semester</label>
      <select id="f_smt"><option value="">Semua</option>
        <option value="1">Semester 1</option><option value="2">Semester 2</option>
        <option value="3">Semester 3</option><option value="4">Semester 4</option>
        <option value="5">Semester 5</option><option value="6">Semester 6</option>
        <option value="7">Semester 7</option><option value="8">Semester 8</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Prodi</label><select id="f_prodi"><option value="">Semua</option></select></div>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" style="align-self:flex-end" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
  </div>
</div>
<div id="tabelPaket"></div>
<script>
var tbl=null, tabAktif='paket';
document.addEventListener('DOMContentLoaded',function(){
    muatProdi();
    tbl=AIS2.Table.init({
        container:'#tabelPaket',modul:'pendataan',svc:'paket_kuliah_list',
        bisa:{tambah:false,ubah:true,hapus:true},
        kolom:[
            {field:'nama',label:'Nama Paket',sort:true},
            {field:'angkatan',label:'Angkatan',lebar:'80px'},
            {field:'semester',label:'Smt',lebar:'50px'},
            {field:'prodi',label:'Program Studi',sembunyiMobile:true},
            {field:'jumlahMK',label:'Jml MK',lebar:'70px'},
            {field:'totalSKS',label:'Total SKS',lebar:'80px'},
            {field:'aktif',label:'Aktif',lebar:'65px',render:function(v){return AIS2.Badge.aktif(v);}}
        ],
        renderAksi:function(row){
            return '<div class="td-actions">'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="bukaIsiPaket(\''+row.id+'\',\''+row.nama+'\')" title="Kelola Isi MK"><i class="fa-solid fa-list"></i></button>'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="bukaForm('+JSON.stringify(row).replace(/"/g,'&quot;')+')" title="Ubah"><i class="fa-solid fa-pen"></i></button>'
                +'<button class="ais2-btn ais2-btn-danger ais2-btn-sm" onclick="hapus(\''+row.id+'\')" title="Hapus"><i class="fa-solid fa-trash"></i></button>'
                +'</div>';
        }
    });
});
function gTab(t){
    tabAktif=t;
    ['paket','isi'].forEach(function(k){document.getElementById('tab_'+k).classList.toggle('active',k===t);});
    tbl&&tbl.setFilter({tab:t,nama:document.getElementById('f_nama').value,angkatan:document.getElementById('f_angkatan').value,smt:document.getElementById('f_smt').value,prodi:document.getElementById('f_prodi').value});
}
function terapkan(){tbl&&tbl.setFilter({tab:tabAktif,nama:document.getElementById('f_nama').value,angkatan:document.getElementById('f_angkatan').value,smt:document.getElementById('f_smt').value,prodi:document.getElementById('f_prodi').value});}
function muatProdi(){AIS2.Api.get('pendataan','ref_prodi_all',{}).then(function(res){if(!res||!res.data)return;var sel=document.getElementById('f_prodi');res.data.forEach(function(p){var o=document.createElement('option');o.value=p.id;o.textContent=p.nama;sel.appendChild(o);});});}
function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field ais2-form-full"><label>Nama Paket <span class="req">*</span></label><input class="ais2-input" name="nama" value="'+esc(d.nama)+'" required></div>'
        +'<div class="ais2-field"><label>Angkatan</label><input class="ais2-input" type="number" name="angkatan" value="'+esc(d.angkatan)+'"></div>'
        +'<div class="ais2-field"><label>Semester</label><select name="semester" class="ais2-input"><option value="">--</option>'
        +[1,2,3,4,5,6,7,8].map(function(n){return '<option value="'+n+'"'+(d.semester==n?' selected':'')+'>Semester '+n+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Prodi</label><select name="prodiId" class="ais2-input"><option value="">-- Pilih Prodi --</option></select></div>'
        +'<div class="ais2-field"><label>Jenjang</label><select name="jenjang" class="ais2-input"><option value="">--</option><option>D3</option><option>D4</option><option>S1</option><option>S2</option><option>S3</option></select></div>'
        +'</div>'
        +'<div style="margin-top:12px"><label class="ais2-switch"><input type="checkbox" name="aktif" value="true" '+(d.aktif!==false?'checked':'')+' ><span>Paket Aktif</span></label></div>'
        +'<div class="ais2-field" style="margin-top:8px"><label>Keterangan</label><textarea class="ais2-input" name="keterangan" rows="2">'+esc(d.keterangan)+'</textarea></div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Paket Perkuliahan':'Buat Paket Perkuliahan Baru',ikon:'fa-layer-group',ukuran:'',
        konten:html,
        onBuka:function(){
            AIS2.Api.get('pendataan','ref_prodi_all',{}).then(function(res){
                if(!res||!res.data)return;
                var sel=modal.body().querySelector('[name=prodiId]');
                res.data.forEach(function(p){var o=document.createElement('option');o.value=p.id;o.textContent=p.nama;if(d.prodiId&&p.id==d.prodiId)o.selected=true;sel.appendChild(o);});
            });
        },
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.type==='checkbox'?(el.checked?'true':'false'):el.value;});
                if(!data.nama){AIS2.Toast.peringatan('Nama paket wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'paket_kuliah_ubah':'paket_kuliah_tambah',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Tersimpan');AIS2.Modal.tutup();tbl&&tbl.refresh();}
                    else AIS2.Toast.error((r&&r.msg)||'Gagal');
                });
            }},
            {label:'Tutup',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}
function hapus(id){AIS2.Confirm.tampil({judul:'Hapus Paket',pesan:'Paket perkuliahan ini akan dihapus?',tipe:'danger',labelOk:'Hapus',onOk:function(){AIS2.Api.post('pendataan','paket_kuliah_hapus',{id:id}).then(function(r){if(r&&r.ok){AIS2.Toast.sukses('Dihapus');tbl&&tbl.refresh();}else AIS2.Toast.error((r&&r.msg)||'Gagal');});}});}
function bukaIsiPaket(id,nama){AIS2.Toast.info('Kelola isi MK untuk paket: '+nama);}
document.addEventListener('keydown',function(e){if(e.key==='Enter'&&e.target.tagName==='INPUT')terapkan();});
</script>
</body></html>
