<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Pengguna</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<%@ include file="../_lang.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-users"></i></div>
  <div>
    <div class="ais2-breadcrumb">
      <a href="<%=rootCtx%>/baru2?modul=pengaturan">Pengaturan</a>
      <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span>
      <span>Pengguna</span>
    </div>
    <div class="ais2-ph-title">Manajemen Pengguna</div>
    <div class="ais2-ph-sub">Kelola akun login, peran, dan data terkait semua pengguna sistem</div>
  </div>
  <div class="ais2-ph-actions">
    <button class="ais2-btn ais2-btn-outline" onclick="ekspor()"><i class="fa-solid fa-file-excel"></i> Ekspor</button>
    <button class="ais2-btn ais2-btn-primary" onclick="bukaTambah()"><i class="fa-solid fa-plus"></i> Tambah Pengguna</button>
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
    <div class="ais2-filter-field"><label>Nama / User ID</label><input type="text" id="f_nama" placeholder="Nama atau username..."></div>
    <div class="ais2-filter-field"><label>Email</label><input type="text" id="f_email" placeholder="Email..."></div>
    <div class="ais2-filter-field"><label>Grup / Role</label><select id="f_role"><option value="">Semua Grup</option></select></div>
    <div class="ais2-filter-field"><label>Status</label>
      <select id="f_aktif"><option value="">Semua</option><option value="1">Aktif</option><option value="0">Nonaktif</option></select>
    </div>
  </div>
  <div class="ais2-filter-advanced" id="filterLanjut">
    <div class="ais2-filter-row">
      <div class="ais2-filter-field"><label>Fakultas</label><select id="f_fak"><option value="">Semua</option></select></div>
      <div class="ais2-filter-field"><label>Program Studi</label><select id="f_prodi"><option value="">Semua</option></select></div>
      <div class="ais2-filter-field"><label>Unit Kerja</label><input type="text" id="f_satker" placeholder="Satuan kerja..."></div>
    </div>
    <div class="ais2-filter-checks">
      <label class="ais2-filter-check"><input type="checkbox" id="fc_root"> Superuser (root) saja</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_facebook"> Terhubung Facebook</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_google"> Terhubung Google</label>
      <label class="ais2-filter-check"><input type="checkbox" id="fc_enkripsi"> Sandi sudah dienkripsi</label>
    </div>
  </div>
</div>

<div id="tabelPengguna"></div>

<script>
var tbl=null;
document.addEventListener('DOMContentLoaded',function(){
    muatRole(); muatFakultas();
    tbl=AIS2.Table.init({
        container:'#tabelPengguna',modul:'pengaturan',svc:'pengguna_list',
        bisa:{tambah:false,ubah:true,hapus:false},
        sortDefault:'usernama',
        kolom:[
            {field:'userid',label:'Username',lebar:'130px',sort:true},
            {field:'usernama',label:'Nama',sort:true},
            {field:'email',label:'Email',sembunyiMobile:true},
            {field:'roleName',label:'Grup/Role',lebar:'120px',sembunyiMobile:true},
            {field:'bahasa',label:'Bahasa',lebar:'90px',sembunyiMobile:true},
            {field:'aktif',label:'Aktif',lebar:'65px',render:function(v){return AIS2.Badge.aktif(v);}}
        ],
        renderAksi:function(row){
            return '<div class="td-actions">'
                +'<button class="ais2-btn ais2-btn-outline ais2-btn-sm" onclick="bukaForm('+JSON.stringify(row).replace(/"/g,'&quot;')+')" title="Ubah"><i class="fa-solid fa-pen"></i></button>'
                +'<button class="ais2-btn ais2-btn-warning ais2-btn-sm" onclick="resetPwd(\''+row.userid+'\')" title="Reset Password"><i class="fa-solid fa-key"></i></button>'
                +(row.aktif?'<button class="ais2-btn ais2-btn-danger ais2-btn-sm" onclick="ubahStatus(\''+row.userid+'\',false)" title="Nonaktifkan"><i class="fa-solid fa-ban"></i></button>'
                           :'<button class="ais2-btn ais2-btn-success ais2-btn-sm" onclick="ubahStatus(\''+row.userid+'\',true)" title="Aktifkan"><i class="fa-solid fa-check"></i></button>')
                +'</div>';
        }
    });
});

function terapkan(){tbl&&tbl.setFilter({nama:document.getElementById('f_nama').value,email:document.getElementById('f_email').value,role:document.getElementById('f_role').value,aktif:document.getElementById('f_aktif').value,fak:document.getElementById('f_fak').value,prodi:document.getElementById('f_prodi').value,satker:document.getElementById('f_satker').value,root:document.getElementById('fc_root').checked?'1':'',fb:document.getElementById('fc_facebook').checked?'1':'',ggl:document.getElementById('fc_google').checked?'1':'',enkripsi:document.getElementById('fc_enkripsi').checked?'1':''});}
function reset(){['f_nama','f_email','f_satker'].forEach(function(id){var el=document.getElementById(id);if(el)el.value='';});['f_role','f_aktif','f_fak','f_prodi'].forEach(function(id){var el=document.getElementById(id);if(el)el.selectedIndex=0;});['fc_root','fc_facebook','fc_google','fc_enkripsi'].forEach(function(id){var el=document.getElementById(id);if(el)el.checked=false;});terapkan();}
function toggleLanjut(){document.getElementById('filterLanjut').classList.toggle('show');}

function muatRole(){
    AIS2.Api.get('pengaturan','grup_list_all',{}).then(function(res){
        if(!res||!res.data)return;
        var sel=document.getElementById('f_role');
        res.data.forEach(function(r){var o=document.createElement('option');o.value=r.roleId;o.textContent=r.roleName||r.roleId;sel.appendChild(o);});
    });
}
function muatFakultas(){
    AIS2.Api.get('pendataan','ref_fakultas',{}).then(function(res){
        if(!res||!res.data)return;
        var sel=document.getElementById('f_fak');
        res.data.forEach(function(f){var o=document.createElement('option');o.value=f.id;o.textContent=f.nama;sel.appendChild(o);});
    });
}

function bukaTambah(){bukaForm({});}
function bukaForm(d){
    d=d||{};
    var esc=function(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');};
    var chk=function(v){return v?'checked':''};
    var html='<input type="hidden" name="useridOld" value="'+esc(d.userid)+'">'
        +'<div class="ais2-form-grid">'
        +'<div class="ais2-field"><label>Username / User ID <span class="req">*</span></label>'
        +'<input class="ais2-input" name="userid" value="'+esc(d.userid)+'" required'+(d.userid?' readonly':'')+' placeholder="Unik, tidak bisa diubah setelah dibuat"></div>'
        +'<div class="ais2-field"><label>Nama Lengkap <span class="req">*</span></label>'
        +'<input class="ais2-input" name="usernama" value="'+esc(d.usernama)+'" required></div>'
        +(d.userid?'':'<div class="ais2-field"><label>Password <span class="req">*</span></label><input class="ais2-input" type="password" name="userpassword" required></div>')
        +'<div class="ais2-field"><label>Grup / Role <span class="req">*</span></label>'
        +'<select name="userRole" class="ais2-input" required id="selRoleForm"><option value="">-- Pilih Grup --</option></select></div>'
        +'<div class="ais2-field"><label>Email</label>'
        +'<input class="ais2-input" type="email" name="email" value="'+esc(d.email)+'"></div>'
        +'<div class="ais2-field"><label>Preferensi Bahasa</label>'
        +'<select name="bahasa" class="ais2-input">'
        +['Bahasa Indonesia','English','Arabic','Mandarin'].map(function(b){return '<option value="'+b+'"'+(d.bahasa===b?' selected':'')+'>'+b+'</option>';}).join('')+'</select></div>'
        +'</div>'
        +'<div style="margin-top:12px;display:flex;gap:20px;flex-wrap:wrap">'
        +'<label class="ais2-switch"><input type="checkbox" name="aktif" value="true" '+chk(d.aktif!==false)+'><span>'+AIS2.L('Aktif','Aktif')+'</span></label>'
        +'<label class="ais2-switch"><input type="checkbox" name="usershow" value="1" '+chk(d.usershow)+'><span>Tampilkan di daftar publik</span></label>'
        +'</div>';

    var modal=AIS2.Modal.buka({
        judul:d.userid?'Ubah Pengguna — '+d.usernama:'Tambah Pengguna Baru',
        ikon:'fa-users',ukuran:'',konten:html,
        onBuka:function(){
            /* Muat opsi role ke select form */
            AIS2.Api.get('pengaturan','grup_list_all',{}).then(function(res){
                if(!res||!res.data)return;
                var sel=modal.body().querySelector('[name=userRole]');
                if(!sel)return;
                res.data.forEach(function(r){var o=document.createElement('option');o.value=r.roleId;o.textContent=r.roleName||r.roleId;if(d.roleId&&r.roleId===d.roleId)o.selected=true;sel.appendChild(o);});
            });
        },
        tombol:[
            {label:AIS2.L('Simpan','Simpan'),kelas:'ais2-btn-primary',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.type==='checkbox'?(el.checked?'true':'false'):el.value;});
                if(!data.userid||!data.usernama||!data.userRole){AIS2.Toast.peringatan('Username, Nama, dan Grup wajib diisi');return;}
                AIS2.Api.post('pengaturan',d.userid?'pengguna_ubah':'pengguna_tambah',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Tersimpan');AIS2.Modal.tutup();tbl&&tbl.refresh();}
                    else AIS2.Toast.error((r&&r.msg)||'Gagal');
                });
            }},
            {label:AIS2.L('Tutup','Tutup'),kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}

function resetPwd(userid){
    var modal=AIS2.Modal.buka({
        judul:'Reset Password',ikon:'fa-key',ukuran:'sm',
        konten:'<input type="hidden" name="userid" value="'+userid+'">'
            +'<div class="ais2-field"><label>Password Baru <span class="req">*</span></label>'
            +'<input class="ais2-input" type="password" name="passwordBaru" required placeholder="Minimal 6 karakter..."></div>'
            +'<div class="ais2-field" style="margin-top:8px"><label>Konfirmasi Password</label>'
            +'<input class="ais2-input" type="password" name="konfirmasi" placeholder="Ulangi password..."></div>',
        tombol:[
            {label:'Reset',kelas:'ais2-btn-warning',onClick:function(){
                var data={};modal.body().querySelectorAll('[name]').forEach(function(el){data[el.name]=el.value;});
                if(!data.passwordBaru||data.passwordBaru.length<6){AIS2.Toast.peringatan('Password minimal 6 karakter');return;}
                if(data.passwordBaru!==data.konfirmasi){AIS2.Toast.peringatan('Konfirmasi password tidak cocok');return;}
                AIS2.Api.post('pengaturan','pengguna_reset_pwd',data).then(function(r){
                    if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Password direset');AIS2.Modal.tutup();}
                    else AIS2.Toast.error((r&&r.msg)||'Gagal');
                });
            }},
            {label:'Batal',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}
        ]
    });
}

function ubahStatus(userid,aktif){
    AIS2.Confirm.tampil({
        judul:aktif?'Aktifkan Pengguna':'Nonaktifkan Pengguna',
        pesan:aktif?'Aktifkan akun pengguna ini?':'Nonaktifkan akun pengguna ini? Mereka tidak bisa login.',
        tipe:aktif?'info':'danger',labelOk:aktif?'Ya, Aktifkan':'Ya, Nonaktifkan',
        onOk:function(){
            AIS2.Api.post('pengaturan','pengguna_ubah_status',{userid:userid,aktif:aktif?'true':'false'}).then(function(r){
                if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Status diubah');tbl&&tbl.refresh();}
                else AIS2.Toast.error((r&&r.msg)||'Gagal');
            });
        }
    });
}

function ekspor(){window.open(ROOT+'/baru2?modul=pengaturan&svc=pengguna_ekspor','_blank');}
document.addEventListener('keydown',function(e){if(e.key==='Enter'&&e.target.tagName==='INPUT')terapkan();});
</script>
</body></html>
