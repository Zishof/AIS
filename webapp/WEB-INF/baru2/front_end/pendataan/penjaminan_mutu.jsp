<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Penjaminan Mutu</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-star-half-stroke"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Penjaminan Mutu</span></div>
    <div class="ais2-ph-title">Penjaminan Mutu Program Studi</div>
    <div class="ais2-ph-sub">Kelola data akreditasi, instrumen evaluasi, dan monitoring mutu prodi</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Tambah Data Mutu</button>
  </div>
</div>

<div class="ais2-tab-bar">
  <button class="ais2-tab-btn active" id="tab_akreditasi" onclick="gTab('akreditasi')">Akreditasi Prodi</button>
  <button class="ais2-tab-btn" id="tab_instrumen" onclick="gTab('instrumen')">Instrumen Evaluasi</button>
  <button class="ais2-tab-btn" id="tab_monev" onclick="gTab('monev')">Monitoring &amp; Evaluasi</button>
</div>

<div class="ais2-filter-card">
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label>Program Studi</label><select id="f_prodi"><option value="">Semua Prodi</option></select></div>
    <div class="ais2-filter-field"><label>Lembaga Akreditasi</label>
      <select id="f_lembaga"><option value="">Semua</option>
        <option>BAN-PT</option><option>LAM-PTKes</option><option>LAMEMBA</option><option>IABEE</option><option>LAMINFOKOM</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Peringkat</label>
      <select id="f_peringkat"><option value="">Semua</option>
        <option>Unggul</option><option>Baik Sekali</option><option>Baik</option><option>A</option><option>B</option><option>C</option>
      </select>
    </div>
    <div class="ais2-filter-field"><label>Status</label>
      <select id="f_status"><option value="">Semua</option><option>Aktif</option><option>Kadaluarsa</option><option>Proses Reakreditasi</option></select>
    </div>
    <button class="ais2-btn ais2-btn-sm ais2-btn-primary" style="align-self:flex-end" onclick="terapkan()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
  </div>
</div>

<div id="tabelMutu"></div>

<script>
var tbl=null, tabAktif='akreditasi';
document.addEventListener('DOMContentLoaded',function(){
    muatProdi();
    tbl=AIS2.Table.init({
        container:'#tabelMutu',modul:'pendataan',svc:'penjaminan_mutu_list',
        bisa:{tambah:false,ubah:true,hapus:false},
        kolom:[
            {field:'prodi',label:'Program Studi',sort:true},
            {field:'lembaga',label:'Lembaga',lebar:'110px'},
            {field:'peringkat',label:'Peringkat',lebar:'100px',render:function(v){
                var w={Unggul:'success','Baik Sekali':'info',Baik:'primary',A:'success',B:'info',C:'warning'};
                return AIS2.Badge.kustom(v,w);
            }},
            {field:'nomorSK',label:'No. SK',lebar:'130px',sembunyiMobile:true},
            {field:'berlakuHingga',label:'Berlaku s/d',lebar:'100px'},
            {field:'status',label:'Status',lebar:'130px',render:function(v){
                var w={Aktif:'success',Kadaluarsa:'danger','Proses Reakreditasi':'warning'};
                return AIS2.Badge.kustom(v,w);
            }}
        ],
        onUbah:function(d){bukaForm(d);}
    });
});
function gTab(t){
    tabAktif=t;
    ['akreditasi','instrumen','monev'].forEach(function(k){document.getElementById('tab_'+k).classList.toggle('active',k===t);});
    terapkan();
}
function terapkan(){tbl&&tbl.setFilter({tab:tabAktif,prodi:document.getElementById('f_prodi').value,lembaga:document.getElementById('f_lembaga').value,peringkat:document.getElementById('f_peringkat').value,status:document.getElementById('f_status').value});}
function muatProdi(){AIS2.Api.get('pendataan','ref_prodi_all',{}).then(function(res){if(!res||!res.data)return;var sel=document.getElementById('f_prodi');res.data.forEach(function(p){var o=document.createElement('option');o.value=p.id;o.textContent=p.nama;sel.appendChild(o);});});}
function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Program Studi <span class="req">*</span></label><select name="prodiId" class="ais2-input" required><option value="">-- Pilih Prodi --</option></select></div>'
        +'<div class="ais2-field"><label>Lembaga Akreditasi <span class="req">*</span></label><select name="lembaga" class="ais2-input" required><option value="">-- Pilih --</option>'
        +['BAN-PT','LAM-PTKes','LAMEMBA','IABEE','LAMINFOKOM','Lainnya'].map(function(l){return '<option'+(d.lembaga===l?' selected':'')+'>'+l+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Peringkat</label><select name="peringkat" class="ais2-input"><option value="">--</option>'
        +['Unggul','Baik Sekali','Baik','A','B','C'].map(function(p){return '<option'+(d.peringkat===p?' selected':'')+'>'+p+'</option>';}).join('')+'</select></div>'
        +'<div class="ais2-field"><label>Nomor SK</label><input class="ais2-input" name="nomorSK" value="'+esc(d.nomorSK)+'"></div>'
        +'<div class="ais2-field"><label>Tanggal SK</label><input class="ais2-input" type="date" name="tanggalSK" value="'+esc(d.tanggalSK)+'"></div>'
        +'<div class="ais2-field"><label>Berlaku Hingga</label><input class="ais2-input" type="date" name="berlakuHingga" value="'+esc(d.berlakuHingga)+'"></div>'
        +'<div class="ais2-field"><label>Skor / Nilai</label><input class="ais2-input" type="number" step="0.01" name="skor" value="'+esc(d.skor)+'"></div>'
        +'<div class="ais2-field"><label>Status</label><select name="status" class="ais2-input">'
        +['Aktif','Kadaluarsa','Proses Reakreditasi'].map(function(s){return '<option'+(d.status===s?' selected':'')+'>'+s+'</option>';}).join('')+'</select></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:8px"><label>Keterangan</label><textarea class="ais2-input" name="keterangan" rows="2">'+esc(d.keterangan)+'</textarea></div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Data Akreditasi':'Tambah Data Mutu / Akreditasi',ikon:'fa-star-half-stroke',ukuran:'',
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
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.value;});
                if(!data.prodiId||!data.lembaga){AIS2.Toast.peringatan('Prodi dan Lembaga wajib dipilih');return;}
                AIS2.Api.post('pendataan',d.id?'penjaminan_mutu_ubah':'penjaminan_mutu_tambah',data).then(function(r){
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
