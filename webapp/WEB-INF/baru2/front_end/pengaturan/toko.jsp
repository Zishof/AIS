<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Toko / Perusahaan</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<%@ include file="../_lang.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">

<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-store"></i></div>
  <div>
    <div class="ais2-breadcrumb">
      <a href="<%=rootCtx%>/baru2?modul=pengaturan">Pengaturan</a>
      <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span>
      <span id="judulBreadcrumb">Toko / Perusahaan</span>
    </div>
    <div class="ais2-ph-title" id="judulHalaman">Toko / Perusahaan</div>
    <div class="ais2-ph-sub" id="deskripsiHalaman">Kelola data toko/perusahaan</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> <span id="btnTambahLabel">Tambah Toko/Perusahaan</span></button>
  </div>
</div>

<div class="ais2-filter-card">
  <div class="ais2-filter-row">
    <div class="ais2-filter-field"><label id="lblCari">Cari</label>
      <input type="text" id="f_nama" placeholder="Nama atau kode toko...">
    </div>
    <div class="ais2-filter-field"><label id="lblStatus">Status</label>
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

<div id="tabelToko"></div>

<script>
var tbl=null;
function esc(v){return String(v==null?'':v).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');}

document.addEventListener('DOMContentLoaded',function(){
    tbl=AIS2.Table.init({
        container:'#tabelToko',modul:'pengaturan',svc:'toko_list',
        bisa:{tambah:false,ubah:true,hapus:false},
        kolom:[
            {field:'kode',label:AIS2.L('Kode','Kode'),lebar:'110px',sort:true},
            {field:'nama',label:AIS2.L('Nama','Nama'),sort:true},
            {field:'kota',label:AIS2.L('Kota','Kota'),lebar:'140px',sembunyiMobile:true},
            {field:'telp',label:AIS2.L('Telp','Telp'),lebar:'140px',sembunyiMobile:true},
            {field:'aktif',label:AIS2.L('Status','Status'),lebar:'75px',render:function(v){return AIS2.Badge.aktif(v);}}
        ],
        renderAksi:function(row){
            return '<div class="td-actions">'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="editToko('+row.id+')" title="'+AIS2.L('Ubah','Ubah')+'"><i class="fa-solid fa-pen"></i></button>'
                +'</div>';
        }
    });
});

function terapkan(){tbl&&tbl.setFilter({nama:document.getElementById('f_nama').value,aktif:document.getElementById('f_aktif').value});}
function bukaTambah(){bukaForm({});}

function editToko(id){
    AIS2.Api.get('pengaturan','toko_detail',{id:id}).then(function(r){
        if(r&&r.data&&(r.total>0)){bukaForm(r.data);}
        else AIS2.Toast.error(AIS2.L('DataTidakDitemukan','Data tidak ditemukan'));
    });
}

function fld(label,name,val,ph,tipe){
    return '<div class="ais2-field"><label>'+label+'</label>'
        +'<input class="ais2-input" type="'+(tipe||'text')+'" name="'+name+'" value="'+esc(val)+'" placeholder="'+(ph||'')+'"></div>';
}

function bukaForm(d){
    d=d||{};
    var isEdit=!!d.id;
    var html=''
      +'<input type="hidden" name="id" value="'+esc(d.id)+'">'
      +'<div class="ais2-form-grid">'
      +'  <div class="ais2-field"><label>'+AIS2.L('Nama','Nama')+' <span class="req">*</span></label>'
      +'    <input class="ais2-input" name="nama" value="'+esc(d.nama)+'" required></div>'
      +   fld(AIS2.L('Kode','Kode'),'kode',d.kode,'')
      +   fld(AIS2.L('Npwp','NPWP / ID Pajak'),'npwp',d.npwp,'')
      +   fld(AIS2.L('Telp','Telepon'),'telp',d.telp,'')
      +   fld(AIS2.L('Email','Email'),'email',d.email,'','email')
      +   fld(AIS2.L('Kota','Kota'),'kota',d.kota,'')
      +   fld(AIS2.L('KodePos','Kode Pos'),'kodePos',d.kodePos,'')
      +   fld(AIS2.L('PicNama','Nama PIC / Penanggung Jawab'),'picNama',d.picNama,'')
      +   fld(AIS2.L('PicHp','HP PIC'),'picHp',d.picHp,'')
      +   fld(AIS2.L('JamOperasional','Jam Operasional'),'jamOperasional',d.jamOperasional,'08:00 - 21:00 (Senin-Sabtu)')
      +'</div>'
      +'<div class="ais2-field" style="margin-top:10px"><label>'+AIS2.L('Alamat','Alamat')+'</label>'
      +'  <textarea class="ais2-input" name="alamat" rows="2">'+esc(d.alamat)+'</textarea></div>'
      +'<div class="ais2-field" style="margin-top:8px"><label>'+AIS2.L('Keterangan','Keterangan')+'</label>'
      +'  <textarea class="ais2-input" name="keterangan" rows="2">'+esc(d.keterangan)+'</textarea></div>'
      +'<div style="margin-top:12px"><label class="ais2-switch"><input type="checkbox" name="aktif" value="true" '+(d.aktif!==false?'checked':'')+'><span>'+AIS2.L('Aktif','Aktif')+'</span></label></div>';

    var modal=AIS2.Modal.buka({
        judul:isEdit?AIS2.L('UbahToko','Ubah Toko/Perusahaan'):AIS2.L('TambahToko','Tambah Toko/Perusahaan'),
        ikon:'fa-store',ukuran:'lg',konten:html,
        tombol:[
            {label:AIS2.L('Simpan','Simpan'),kelas:'ais2-btn-primary',onClick:function(){
                var body=modal.body();
                var data={};
                body.querySelectorAll('[name]').forEach(function(el){
                    data[el.name]=el.type==='checkbox'?(el.checked?'true':'false'):el.value;
                });
                if(!data.nama||!data.nama.trim()){AIS2.Toast.peringatan(AIS2.L('NamaWajib','Nama wajib diisi'));return;}
                AIS2.Api.post('pengaturan','toko_simpan',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||AIS2.L('Tersimpan','Tersimpan'));AIS2.Modal.tutup();tbl&&tbl.refresh();}
                    else AIS2.Toast.error((r&&r.msg)||AIS2.L('Gagal','Gagal'));
                });
            }},
            {label:AIS2.L('Tutup','Tutup'),kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}

document.addEventListener('keydown',function(e){if(e.key==='Enter'&&e.target.tagName==='INPUT'&&e.target.closest('.ais2-filter-card'))terapkan();});
</script>
</body></html>
