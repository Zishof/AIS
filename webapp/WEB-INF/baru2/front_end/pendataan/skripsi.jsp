<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Skripsi / Thesis</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">

<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-file-pen"></i></div>
  <div>
    <div class="ais2-breadcrumb"><a href="<%=rootCtx%>/baru2?modul=pendataan">Pendataan</a> <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span> <span>Skripsi / Thesis</span></div>
    <div class="ais2-ph-title">Skripsi / Thesis</div>
    <div class="ais2-ph-sub">Kelola data tugas akhir mahasiswa — pendaftaran, bimbingan, dan sidang</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Daftarkan TA</button>
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
    <div class="ais2-filter-field"><label>Mahasiswa (NIM / Nama)</label><input type="text" id="f_mhs" placeholder="Cari mahasiswa..."></div>
    <div class="ais2-filter-field"><label>Gelombang Pendaftaran</label><input type="text" id="f_gelombang" placeholder="Nama gelombang..."></div>
    <div class="ais2-filter-field"><label>Fakultas</label><select id="f_fak"><option value="">Semua</option></select></div>
    <div class="ais2-filter-field"><label>Program Studi</label><select id="f_prodi"><option value="">Semua</option></select></div>
  </div>
  <div class="ais2-filter-advanced" id="filterLanjut">
    <div class="ais2-filter-row">
      <div class="ais2-filter-field"><label>Judul TA</label><input type="text" id="f_judul" placeholder="Cari judul..."></div>
      <div class="ais2-filter-field"><label>Status Sidang</label>
        <select id="f_statusSidang">
          <option value="">Semua</option>
          <option>Belum Sidang</option><option>Terdaftar</option>
          <option>Lulus</option><option>Tidak Lulus</option><option>Ditunda</option>
        </select>
      </div>
      <div class="ais2-filter-field"><label>Dosen Pembimbing</label><input type="text" id="f_dosen" placeholder="Nama/kode dosen..."></div>
      <div class="ais2-filter-field"><label>Tahun Akademik</label><select id="f_ta"><option value="">Semua TA</option></select></div>
    </div>
  </div>
</div>

<div id="tabelSkripsi"></div>

<script>
var tbl=null;
document.addEventListener('DOMContentLoaded',function(){
    muatFakultas(); muatTA();
    tbl=AIS2.Table.init({
        container:'#tabelSkripsi', modul:'pendataan', svc:'skripsi_list',
        bisa:{tambah:false,ubah:true,hapus:false},
        kolom:[
            {field:'nim',         label:'NIM', lebar:'110px'},
            {field:'nama',        label:'Nama Mahasiswa', sort:true},
            {field:'judul',       label:'Judul TA'},
            {field:'dosenPembimbing', label:'Pembimbing', sembunyiMobile:true},
            {field:'tahunAkademik',   label:'TA',    lebar:'80px', sembunyiMobile:true},
            {field:'nilaiAngka',  label:'Nilai',  lebar:'60px', sembunyiMobile:true},
            {field:'nilaiHuruf',  label:'Huruf',  lebar:'55px',
             render:function(v){
                if(!v)return '';
                var peta={A:{k:'ais2-badge-green'},B:{k:'ais2-badge-blue'},C:{k:'ais2-badge-yellow'},D:{k:'ais2-badge-red'},E:{k:'ais2-badge-red'}};
                var cfg=peta[v]||{k:'ais2-badge-gray'};
                return '<span class="ais2-badge '+cfg.k+'">'+v+'</span>';
             }},
            {field:'statusSidang', label:'Status',  lebar:'100px',
             render:function(v){
                var peta={'Lulus':{label:'Lulus',kelas:'ais2-badge-green'},'Tidak Lulus':{label:'Tidak Lulus',kelas:'ais2-badge-red'},'Terdaftar':{label:'Terdaftar',kelas:'ais2-badge-blue'},'Ditunda':{label:'Ditunda',kelas:'ais2-badge-yellow'},_default:{label:v||'Belum',kelas:'ais2-badge-gray'}};
                return AIS2.Badge.kustom(v,peta);
             }}
        ],
        onUbah:function(d){bukaFormById(d.id);}
    });
});
function terapkan(){
    tbl&&tbl.setFilter({mhs:document.getElementById('f_mhs').value,gelombang:document.getElementById('f_gelombang').value,
        fakultas:document.getElementById('f_fak').value,prodi:document.getElementById('f_prodi').value,
        judul:document.getElementById('f_judul').value,statusSidang:document.getElementById('f_statusSidang').value,
        dosen:document.getElementById('f_dosen').value,ta:document.getElementById('f_ta').value});
}
function reset(){
    ['f_mhs','f_gelombang','f_judul','f_dosen'].forEach(function(id){var el=document.getElementById(id);if(el)el.value='';});
    ['f_fak','f_prodi','f_statusSidang','f_ta'].forEach(function(id){var el=document.getElementById(id);if(el)el.selectedIndex=0;});
    terapkan();
}
function toggleLanjut(){document.getElementById('filterLanjut').classList.toggle('show');}
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
function bukaTambah(){bukaFormById(null);}
function bukaFormById(id){
    if(!id){bukaForm({});return;}
    AIS2.Api.get('pendataan','skripsi_detail',{id:id}).then(function(r){bukaForm(r&&r.data?r.data:{});});
}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var html='<input type="hidden" name="id" value="'+esc(d.id)+'">'
        +'<div class="ais2-form-section-title">Identitas Tugas Akhir</div>'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Mahasiswa <span class="req">*</span></label><input class="ais2-input" name="mahasiswaRef" value="'+esc(d.nimMahasiswa||d.namaMahasiswa)+'" required placeholder="NIM atau nama..."></div>'
        +'<div class="ais2-field"><label>Gelombang Pendaftaran</label><input class="ais2-input" name="gelombang" value="'+esc(d.gelombang)+'"></div>'
        +'<div class="ais2-field"><label>Tahun Akademik</label><select name="tahunAkademik" class="ais2-input"><option value="">--</option></select></div>'
        +'<div class="ais2-field"><label>Semester</label><select name="semester" class="ais2-input"><option value="">--</option><option value="1">Ganjil</option><option value="2">Genap</option></select></div>'
        +'</div>'
        +'<div class="ais2-field" style="margin-top:12px"><label>Judul TA <span class="req">*</span></label>'
        +'<textarea class="ais2-input" name="judul" rows="3" required>'+esc(d.judul)+'</textarea></div>'
        +'<div class="ais2-form-section-title" style="margin-top:20px">Pembimbing & Penguji</div>'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Dosen Pembimbing 1</label><input class="ais2-input" name="dosenPembimbing1" value="'+esc(d.dosenPembimbing1Nama)+'" placeholder="Nama/kode dosen..."></div>'
        +'<div class="ais2-field"><label>Dosen Pembimbing 2</label><input class="ais2-input" name="dosenPembimbing2" value="'+esc(d.dosenPembimbing2Nama)+'" placeholder="Nama/kode dosen..."></div>'
        +'<div class="ais2-field"><label>Penguji 1</label><input class="ais2-input" name="penguji1" value="'+esc(d.penguji1Nama)+'"></div>'
        +'<div class="ais2-field"><label>Penguji 2</label><input class="ais2-input" name="penguji2" value="'+esc(d.penguji2Nama)+'"></div>'
        +'</div>'
        +'<div class="ais2-form-section-title" style="margin-top:20px">Sidang</div>'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Status Sidang</label><select name="statusSidang" class="ais2-input"><option value="">--</option>'
        +['Belum Sidang','Terdaftar','Lulus','Tidak Lulus','Ditunda'].map(function(s){return '<option'+(d.statusSidang===s?' selected':'')+'>'+s+'</option>';}).join('')
        +'</select></div>'
        +'<div class="ais2-field"><label>Tanggal Sidang</label><input class="ais2-input" type="date" name="tanggalSidang" value="'+esc(d.tanggalSidang)+'"></div>'
        +'<div class="ais2-field"><label>Nilai (Angka)</label><input class="ais2-input" type="number" step="0.01" name="nilaiAngka" value="'+esc(d.nilaiAngka)+'"></div>'
        +'<div class="ais2-field"><label>Nilai Huruf</label><select name="nilaiHuruf" class="ais2-input"><option value="">--</option>'
        +['A','A-','B+','B','B-','C+','C','D','E'].map(function(h){return '<option'+(d.nilaiHuruf===h?' selected':'')+'>'+h+'</option>';}).join('')
        +'</select></div>'
        +'</div>';
    var modal=AIS2.Modal.buka({
        judul:d.id?'Ubah Data TA':'Daftarkan Tugas Akhir',ikon:'fa-file-pen',ukuran:'lg',
        konten:html,
        tombol:[
            {label:'Simpan',kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.type==='checkbox'?(el.checked?'true':'false'):el.value;});
                if(!data.mahasiswaRef||!data.judul){AIS2.Toast.peringatan('Mahasiswa dan Judul wajib diisi');return;}
                AIS2.Api.post('pendataan',d.id?'skripsi_ubah':'skripsi_tambah',data).then(function(r){
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
