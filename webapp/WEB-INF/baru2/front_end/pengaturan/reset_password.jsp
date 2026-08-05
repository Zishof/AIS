<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Reset Password User</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<%@ include file="../_lang.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
<style>
.rp-info-card{background:linear-gradient(135deg,#fef3c7,#fde68a);border:1px solid #f59e0b;border-radius:12px;padding:14px 16px;margin-bottom:20px;display:flex;gap:12px;align-items:flex-start}
.rp-info-card i{color:#d97706;font-size:18px;margin-top:2px;flex-shrink:0}
.rp-info-card p{color:#78350f;font-size:13px;margin:0;line-height:1.6}
.rp-user-chip{display:inline-flex;align-items:center;gap:8px;background:var(--ais2-bg);border:1px solid var(--ais2-border);border-radius:100px;padding:6px 14px;font-size:13px;color:var(--ais2-text);margin-bottom:12px}
.rp-user-chip i{color:var(--ais2-pri2)}
.rp-log-item{padding:10px 14px;border-bottom:1px solid var(--ais2-border);display:flex;align-items:center;gap:10px;font-size:13px}
.rp-log-item:last-child{border-bottom:none}
.rp-log-who{font-weight:600;color:var(--ais2-text)}
.rp-log-time{font-size:11px;color:var(--ais2-text-sec)}
</style>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">

<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-key"></i></div>
  <div>
    <div class="ais2-breadcrumb">
      <a href="<%=rootCtx%>/baru2?modul=pengaturan">Pengaturan</a>
      <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span>
      <span>Reset Password User</span>
    </div>
    <div class="ais2-ph-title">Reset Password User</div>
    <div class="ais2-ph-sub">Admin dapat mereset kata sandi pengguna yang lupa atau terkunci</div>
  </div>
</div>

<div class="ais2-filter-card">
  <div class="ais2-filter-head">
    <i class="fa-solid fa-magnifying-glass" style="color:var(--ais2-pri2)"></i>
    <span class="ais2-filter-title">Cari Pengguna</span>
  </div>
  <div class="ais2-filter-row">
    <div class="ais2-filter-field" style="flex:2">
      <label>Nama / Username / Email</label>
      <div style="display:flex;gap:8px">
        <input type="text" id="f_cari" class="ais2-input" placeholder="Ketik nama, username, atau email..." style="flex:1" onkeydown="if(event.key==='Enter')cariUser()">
        <button class="ais2-btn ais2-btn-primary" onclick="cariUser()"><i class="fa-solid fa-magnifying-glass"></i> Cari</button>
      </div>
    </div>
    <div class="ais2-filter-field">
      <label>Status</label>
      <select id="f_aktif" class="ais2-input"><option value="">Semua</option><option value="1">Aktif</option><option value="0">Nonaktif</option></select>
    </div>
  </div>
</div>

<div id="hasilCari" style="display:none" class="ais2-card" style="padding:0">
  <div style="padding:14px 16px;font-weight:600;font-size:14px;color:var(--ais2-text);border-bottom:1px solid var(--ais2-border)">
    <i class="fa-solid fa-list"></i> Hasil Pencarian (<span id="jmlHasil">0</span> pengguna)
  </div>
  <div id="listHasil"></div>
</div>

<!-- Modal Reset -->
<div id="modalReset" style="display:none;position:fixed;inset:0;z-index:9000;background:rgba(0,0,0,.5);display:none;align-items:center;justify-content:center">
  <div style="max-width:440px;width:95%;background:var(--ais2-card-bg);border-radius:16px;overflow:hidden;box-shadow:0 20px 60px rgba(0,0,0,.3)">
    <div style="background:var(--ais2-grad);padding:18px 24px;display:flex;align-items:center;gap:12px">
      <i class="fa-solid fa-key" style="color:#fff;font-size:20px"></i>
      <span style="color:#fff;font-weight:700;font-size:15px">Reset Password</span>
      <button onclick="tutupModal()" style="margin-left:auto;background:rgba(255,255,255,.2);border:none;color:#fff;border-radius:8px;padding:6px 12px;cursor:pointer"><i class="fa-solid fa-xmark"></i></button>
    </div>
    <div style="padding:24px">
      <div class="rp-info-card">
        <i class="fa-solid fa-triangle-exclamation"></i>
        <p>Password akan direset dan pengguna perlu login ulang. Pastikan Anda menginformasikan password baru kepada pengguna tersebut.</p>
      </div>

      <div id="resetUserInfo" style="margin-bottom:20px"></div>

      <input type="hidden" id="resetUserid">

      <div style="margin-bottom:12px">
        <label style="font-size:13px;font-weight:600;color:var(--ais2-text);display:block;margin-bottom:6px">
          Jenis Reset
        </label>
        <div style="display:flex;gap:10px">
          <label style="flex:1;cursor:pointer">
            <input type="radio" name="jenisReset" value="manual" checked onchange="toggleReset(this.value)">
            <span style="font-size:13px;margin-left:4px">Tentukan manual</span>
          </label>
          <label style="flex:1;cursor:pointer">
            <input type="radio" name="jenisReset" value="acak" onchange="toggleReset(this.value)">
            <span style="font-size:13px;margin-left:4px">Generate otomatis</span>
          </label>
        </div>
      </div>

      <div id="seksiManual">
        <div class="ais2-field">
          <label>Password Baru <span class="req">*</span></label>
          <div style="position:relative">
            <input class="ais2-input" type="password" id="pwBaru" placeholder="Minimal 6 karakter...">
            <button style="position:absolute;right:10px;top:50%;transform:translateY(-50%);background:none;border:none;cursor:pointer;color:var(--ais2-text-sec)" onclick="togglePw()"><i class="fa-solid fa-eye" id="eyeIcon"></i></button>
          </div>
        </div>
        <div class="ais2-field" style="margin-top:10px">
          <label>Konfirmasi <span class="req">*</span></label>
          <input class="ais2-input" type="password" id="pwKonfirm" placeholder="Ulangi password...">
        </div>
      </div>

      <div id="seksiAcak" style="display:none">
        <div style="background:var(--ais2-bg);border-radius:10px;padding:14px;text-align:center">
          <div style="font-size:22px;letter-spacing:4px;font-weight:700;font-family:monospace;color:var(--ais2-text)" id="pwAcak">—</div>
          <button class="ais2-btn ais2-btn-outline ais2-btn-sm" style="margin-top:8px" onclick="generatePw()"><i class="fa-solid fa-dice"></i> Generate Ulang</button>
        </div>
      </div>

      <div style="margin-top:20px;display:flex;gap:10px;justify-content:flex-end">
        <button class="ais2-btn ais2-btn-warning" onclick="doReset()"><i class="fa-solid fa-key"></i> Reset Sekarang</button>
        <button class="ais2-btn ais2-btn-outline" onclick="tutupModal()">Batal</button>
      </div>
    </div>
  </div>
</div>

<script>
var jenisReset='manual';
var pwAcakVal='';

function cariUser(){
    var q=document.getElementById('f_cari').value;
    var aktif=document.getElementById('f_aktif').value;
    AIS2.Api.get('pengaturan','pengguna_list',{nama:q,aktif:aktif,limit:30}).then(function(r){
        var list=r&&r.data?r.data:[];
        document.getElementById('jmlHasil').textContent=list.length;
        var html='';
        if(!list.length){html='<div style="padding:24px;text-align:center;color:var(--ais2-text-sec)"><i class="fa-solid fa-search" style="font-size:24px;opacity:.4;display:block;margin-bottom:8px"></i>Tidak ada pengguna ditemukan</div>';}
        else {
            list.forEach(function(u){
                html+='<div class="rp-log-item" style="justify-content:space-between">'
                    +'<div style="display:flex;align-items:center;gap:10px">'
                    +'<div style="width:36px;height:36px;border-radius:50%;background:var(--ais2-grad);display:flex;align-items:center;justify-content:center;font-size:16px;color:#fff;flex-shrink:0"><i class="fa-solid fa-user"></i></div>'
                    +'<div><div class="rp-log-who">'+esc(u.usernama||u.userid)+'</div>'
                    +'<div class="rp-log-time">'+esc(u.userid)+(u.email?' · '+esc(u.email):'')+'</div></div></div>'
                    +'<div style="display:flex;align-items:center;gap:8px">'
                    +AIS2.Badge.aktif(u.aktif)
                    +'<button class="ais2-btn ais2-btn-warning ais2-btn-sm" onclick="bukaModal(\''+esc(u.userid)+'\',\''+esc(u.usernama||u.userid)+'\',\''+esc(u.email||'')+'\')"><i class="fa-solid fa-key"></i> Reset</button>'
                    +'</div></div>';
            });
        }
        document.getElementById('listHasil').innerHTML=html;
        document.getElementById('hasilCari').style.display='block';
    });
}

function esc(v){return String(v||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');}

function bukaModal(userid,nama,email){
    document.getElementById('resetUserid').value=userid;
    document.getElementById('resetUserInfo').innerHTML='<div class="rp-user-chip"><i class="fa-solid fa-user"></i><div><div style="font-weight:600">'+esc(nama)+'</div><div style="font-size:11px;color:var(--ais2-text-sec)">'+esc(userid)+(email?' · '+esc(email):'')+'</div></div></div>';
    document.getElementById('pwBaru').value='';
    document.getElementById('pwKonfirm').value='';
    document.querySelector('[name=jenisReset][value=manual]').checked=true;
    toggleReset('manual');
    var m=document.getElementById('modalReset');m.style.display='flex';
    document.body.style.overflow='hidden';
}
function tutupModal(){document.getElementById('modalReset').style.display='none';document.body.style.overflow='';}

function toggleReset(v){
    jenisReset=v;
    document.getElementById('seksiManual').style.display=v==='manual'?'':'none';
    document.getElementById('seksiAcak').style.display=v==='acak'?'':'none';
    if(v==='acak')generatePw();
}
function generatePw(){
    var chars='abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ123456789@#$';
    var pw='';for(var i=0;i<10;i++)pw+=chars[Math.floor(Math.random()*chars.length)];
    pwAcakVal=pw;document.getElementById('pwAcak').textContent=pw;
}
function togglePw(){
    var inp=document.getElementById('pwBaru');
    inp.type=inp.type==='text'?'password':'text';
    document.getElementById('eyeIcon').className=inp.type==='text'?'fa-solid fa-eye-slash':'fa-solid fa-eye';
}

function doReset(){
    var userid=document.getElementById('resetUserid').value;
    var pw='';
    if(jenisReset==='manual'){
        pw=document.getElementById('pwBaru').value;
        var k=document.getElementById('pwKonfirm').value;
        if(!pw||pw.length<6){AIS2.Toast.peringatan('Password minimal 6 karakter');return;}
        if(pw!==k){AIS2.Toast.peringatan('Konfirmasi tidak cocok');return;}
    } else {
        pw=pwAcakVal;
        if(!pw){AIS2.Toast.peringatan('Generate password terlebih dahulu');return;}
    }
    AIS2.Api.post('pengaturan','pengguna_reset_pwd',{userid:userid,passwordBaru:pw}).then(function(r){
        if(r&&r.ok){
            AIS2.Toast.sukses(r.msg||'Password berhasil direset');
            tutupModal();
            if(jenisReset==='acak'){
                AIS2.Modal.buka({judul:'Password Baru',ikon:'fa-key',ukuran:'sm',konten:'<p style="font-size:14px;margin:0 0 12px">Password baru untuk <strong>'+esc(userid)+'</strong>:</p><div style="background:var(--ais2-bg);border-radius:8px;padding:14px;text-align:center;font-size:22px;font-weight:700;letter-spacing:4px;font-family:monospace">'+esc(pw)+'</div><p style="font-size:12px;color:var(--ais2-text-sec);margin-top:10px">Sampaikan kepada pengguna dan minta segera ubah password setelah login.</p>',tombol:[{label:'Tutup',kelas:'ais2-btn-outline',onClick:function(){AIS2.Modal.tutup();}}]});
            }
        } else AIS2.Toast.error((r&&r.msg)||'Gagal reset password');
    });
}

document.getElementById('modalReset').addEventListener('click',function(e){if(e.target===this)tutupModal();});
</script>
</body></html>
