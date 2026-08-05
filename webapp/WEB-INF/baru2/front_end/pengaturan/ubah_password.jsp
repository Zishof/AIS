<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common,ais.database.model.Tbmuser" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT;
   Tbmuser loginUser = null;
   Object _lo = session.getAttribute("login");
   if (_lo instanceof Tbmuser) loginUser = (Tbmuser)_lo;
   String curUserid = loginUser != null ? loginUser.getUserid() : "";
   String curNama   = loginUser != null ? (loginUser.getUsernama() != null ? loginUser.getUsernama() : curUserid) : "";
%>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Ubah Password</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<%@ include file="../_lang.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
<style>
.pw-card{max-width:480px;margin:0 auto;background:var(--ais2-card-bg);border-radius:16px;padding:32px;box-shadow:0 4px 20px rgba(0,0,0,.08);border:1px solid var(--ais2-border)}
.pw-avatar{width:64px;height:64px;border-radius:50%;background:var(--ais2-grad);display:flex;align-items:center;justify-content:center;margin:0 auto 16px;font-size:28px;color:#fff}
.pw-name{text-align:center;font-weight:700;font-size:18px;color:var(--ais2-text);margin-bottom:4px}
.pw-userid{text-align:center;font-size:13px;color:var(--ais2-text-sec);margin-bottom:24px}
.pw-sep{border:none;border-top:1px solid var(--ais2-border);margin:20px 0}
.pw-strength{height:4px;border-radius:2px;margin-top:6px;transition:all .3s}
.pw-strength-label{font-size:11px;margin-top:3px;font-weight:600}
.pw-req{font-size:12px;color:var(--ais2-text-sec);margin-top:6px;line-height:1.6}
.pw-req span{display:block}
.pw-req .ok{color:#22c55e}
.pw-req .no{color:var(--ais2-text-sec)}
.show-toggle{position:absolute;right:10px;top:50%;transform:translateY(-50%);background:none;border:none;cursor:pointer;color:var(--ais2-text-sec);padding:4px}
.inp-wrap{position:relative}
</style>
</head>
<body class="ais2-page" style="padding:24px 16px;background:var(--ais2-bg);min-height:100vh">

<div class="ais2-page-header" style="margin-bottom:24px">
  <div class="ais2-ph-icon"><i class="fa-solid fa-lock"></i></div>
  <div>
    <div class="ais2-breadcrumb">
      <a href="<%=rootCtx%>/baru2?modul=pengaturan">Pengaturan</a>
      <span class="sep"><i class="fa-solid fa-chevron-right" style="font-size:9px"></i></span>
      <span>Ubah Password</span>
    </div>
    <div class="ais2-ph-title">Ubah Password</div>
    <div class="ais2-ph-sub">Perbarui kata sandi akun Anda secara berkala untuk keamanan</div>
  </div>
</div>

<div class="pw-card">
  <div class="pw-avatar"><i class="fa-solid fa-user"></i></div>
  <div class="pw-name"><%=curNama%></div>
  <div class="pw-userid"><i class="fa-solid fa-at" style="font-size:11px"></i> <%=curUserid%></div>
  <hr class="pw-sep">

  <div class="ais2-field">
    <label>Password Lama <span class="req">*</span></label>
    <div class="inp-wrap">
      <input class="ais2-input" type="password" id="pwLama" placeholder="Masukkan password saat ini">
      <button class="show-toggle" type="button" onclick="toggleShow('pwLama',this)"><i class="fa-solid fa-eye"></i></button>
    </div>
  </div>

  <div class="ais2-field" style="margin-top:14px">
    <label>Password Baru <span class="req">*</span></label>
    <div class="inp-wrap">
      <input class="ais2-input" type="password" id="pwBaru" placeholder="Min 8 karakter, campur huruf & angka" oninput="cekKekuatan(this.value)">
      <button class="show-toggle" type="button" onclick="toggleShow('pwBaru',this)"><i class="fa-solid fa-eye"></i></button>
    </div>
    <div class="pw-strength" id="strengthBar" style="background:#e5e7eb;width:0%"></div>
    <div class="pw-strength-label" id="strengthLabel" style="color:#9ca3af">Masukkan password baru</div>
    <div class="pw-req" id="pwReq">
      <span id="rLen" class="no"><i class="fa-solid fa-circle-xmark"></i> Minimal 8 karakter</span>
      <span id="rHuruf" class="no"><i class="fa-solid fa-circle-xmark"></i> Mengandung huruf</span>
      <span id="rAngka" class="no"><i class="fa-solid fa-circle-xmark"></i> Mengandung angka</span>
    </div>
  </div>

  <div class="ais2-field" style="margin-top:14px">
    <label>Konfirmasi Password Baru <span class="req">*</span></label>
    <div class="inp-wrap">
      <input class="ais2-input" type="password" id="pwKonfirm" placeholder="Ulangi password baru" oninput="cekKonfirmasi()">
      <button class="show-toggle" type="button" onclick="toggleShow('pwKonfirm',this)"><i class="fa-solid fa-eye"></i></button>
    </div>
    <div id="konfirmMsg" style="font-size:12px;margin-top:4px"></div>
  </div>

  <div style="margin-top:24px;display:flex;gap:12px;justify-content:flex-end">
    <button class="ais2-btn ais2-btn-outline" onclick="bersih()"><i class="fa-solid fa-rotate-left"></i> Bersihkan</button>
    <button class="ais2-btn ais2-btn-primary" id="btnSimpan" onclick="simpan()"><i class="fa-solid fa-save"></i> Simpan Password</button>
  </div>
</div>

<script>
function toggleShow(id,btn){
    var inp=document.getElementById(id);
    if(!inp)return;
    var show=inp.type==='text';
    inp.type=show?'password':'text';
    btn.querySelector('i').className=show?'fa-solid fa-eye':'fa-solid fa-eye-slash';
}

var score=0;
function cekKekuatan(v){
    var len=v.length>=8;
    var huruf=/[a-zA-Z]/.test(v);
    var angka=/[0-9]/.test(v);
    var simbol=/[^a-zA-Z0-9]/.test(v);
    setReq('rLen',len);setReq('rHuruf',huruf);setReq('rAngka',angka);
    score=(len?1:0)+(huruf?1:0)+(angka?1:0)+(simbol?1:0)+(v.length>=12?1:0);
    var bar=document.getElementById('strengthBar');
    var lbl=document.getElementById('strengthLabel');
    var pct=[0,25,50,75,90,100][Math.min(score,5)];
    var colors=['#e5e7eb','#ef4444','#f97316','#eab308','#22c55e','#15803d'];
    var labels=['','Sangat lemah','Lemah','Sedang','Kuat','Sangat kuat'];
    bar.style.width=pct+'%';bar.style.background=colors[score]||'#e5e7eb';
    lbl.textContent=labels[score]||'';lbl.style.color=colors[score]||'#9ca3af';
    cekKonfirmasi();
}
function setReq(id,ok){
    var el=document.getElementById(id);if(!el)return;
    var icon=ok?'fa-circle-check':'fa-circle-xmark';
    el.innerHTML='<i class="fa-solid '+icon+'"></i> '+el.textContent.replace(/^[^\s]+\s/,'');
    el.className=ok?'ok':'no';
}
function cekKonfirmasi(){
    var b=document.getElementById('pwBaru').value;
    var k=document.getElementById('pwKonfirm').value;
    var msg=document.getElementById('konfirmMsg');
    if(!k){msg.textContent='';return;}
    if(b===k){msg.innerHTML='<span style="color:#22c55e"><i class="fa-solid fa-check"></i> Password cocok</span>';}
    else{msg.innerHTML='<span style="color:#ef4444"><i class="fa-solid fa-xmark"></i> Password tidak cocok</span>';}
}

function bersih(){
    ['pwLama','pwBaru','pwKonfirm'].forEach(function(id){document.getElementById(id).value='';});
    document.getElementById('strengthBar').style.width='0%';
    document.getElementById('strengthLabel').textContent='Masukkan password baru';
    document.getElementById('konfirmMsg').textContent='';
    ['rLen','rHuruf','rAngka'].forEach(function(id){var el=document.getElementById(id);if(el)el.className='no';});
}

function simpan(){
    var lama=document.getElementById('pwLama').value;
    var baru=document.getElementById('pwBaru').value;
    var konfirm=document.getElementById('pwKonfirm').value;
    if(!lama){AIS2.Toast.peringatan('Masukkan password lama Anda');return;}
    if(!baru||baru.length<8){AIS2.Toast.peringatan('Password baru minimal 8 karakter');return;}
    if(baru!==konfirm){AIS2.Toast.peringatan('Konfirmasi password tidak cocok');return;}
    if(score<2){AIS2.Toast.peringatan('Password terlalu lemah. Tambahkan kombinasi huruf, angka, dan simbol');return;}

    var btn=document.getElementById('btnSimpan');
    btn.disabled=true;btn.innerHTML='<i class="fa-solid fa-spinner fa-spin"></i> Menyimpan...';

    AIS2.Api.post('pengaturan','ubah_password',{passwordLama:lama,passwordBaru:baru}).then(function(r){
        btn.disabled=false;btn.innerHTML='<i class="fa-solid fa-save"></i> Simpan Password';
        if(r&&r.ok){AIS2.Toast.sukses(r.msg||'Password berhasil diubah');bersih();}
        else AIS2.Toast.error((r&&r.msg)||'Gagal mengubah password');
    }).catch(function(){
        btn.disabled=false;btn.innerHTML='<i class="fa-solid fa-save"></i> Simpan Password';
        AIS2.Toast.error('Terjadi kesalahan jaringan');
    });
}
</script>
</body></html>
