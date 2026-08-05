<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String rootCtx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Pengaturan</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<%@ include file="../_lang.jsp" %>
<script>var ROOT='<%=rootCtx%>';</script>
<style>
.set-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));gap:14px;margin-top:8px}
.set-card{display:flex;gap:14px;align-items:flex-start;padding:18px;background:var(--ais2-card-bg);border:1px solid var(--ais2-border);border-radius:14px;text-decoration:none;transition:.15s;cursor:pointer}
.set-card:hover{border-color:var(--ais2-primary);box-shadow:var(--ais2-shadow-lg);transform:translateY(-2px)}
.set-card .ic{width:44px;height:44px;flex:0 0 44px;border-radius:11px;display:flex;align-items:center;justify-content:center;font-size:19px;color:#fff;background:var(--ais2-grad)}
.set-card .tt{font-weight:700;font-size:15px;color:var(--ais2-text);margin-bottom:3px}
.set-card .ds{font-size:12.5px;color:var(--ais2-text-soft);line-height:1.45}
.set-sec{margin-top:22px;margin-bottom:6px;font-size:12px;font-weight:700;letter-spacing:.4px;text-transform:uppercase;color:var(--ais2-text-soft)}
</style>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">

<div class="ais2-page-header">
  <div class="ais2-ph-icon"><i class="fa-solid fa-gear"></i></div>
  <div>
    <div class="ais2-ph-title" id="judulHalaman">Pengaturan</div>
    <div class="ais2-ph-sub" id="deskripsiHalaman">Pusat pengaturan pengguna, grup, toko/perusahaan, dan sistem</div>
  </div>
</div>

<div class="set-sec" id="secUserToko">Pengguna &amp; Organisasi</div>
<div class="set-grid">
  <a class="set-card" href="<%=rootCtx%>/baru2?modul=pengaturan&p=grup_pengguna">
    <div class="ic"><i class="fa-solid fa-user-tag"></i></div>
    <div><div class="tt" id="cGrup">Grup Pengguna</div><div class="ds" id="cGrupD">Buat grup/peran (Admin Toko, Kasir, Inventory, dll) &amp; atur hak akses modul.</div></div>
  </a>
  <a class="set-card" href="<%=rootCtx%>/baru2?modul=pengaturan&p=pengguna">
    <div class="ic"><i class="fa-solid fa-users"></i></div>
    <div><div class="tt" id="cUser">Pengguna</div><div class="ds" id="cUserD">Kelola akun pengguna internal, grup, email, bahasa, dan status.</div></div>
  </a>
  <a class="set-card" href="<%=rootCtx%>/baru2?modul=pengaturan&p=toko">
    <div class="ic"><i class="fa-solid fa-store"></i></div>
    <div><div class="tt" id="cToko">Toko / Perusahaan</div><div class="ds" id="cTokoD">Data toko/perusahaan beserta hierarki pusat &amp; cabang.</div></div>
  </a>
</div>

<div class="set-sec" id="secSistem">Sistem &amp; Keamanan</div>
<div class="set-grid">
  <a class="set-card" href="<%=rootCtx%>/baru2?modul=pengaturan&p=modul">
    <div class="ic"><i class="fa-solid fa-language"></i></div>
    <div><div class="tt" id="cLabel">Label Bahasa</div><div class="ds" id="cLabelD">Kelola teks antarmuka multi-bahasa (Indonesia/English/Arab/Mandarin).</div></div>
  </a>
  <a class="set-card" href="<%=rootCtx%>/baru2?modul=pengaturan&p=ubah_password">
    <div class="ic"><i class="fa-solid fa-key"></i></div>
    <div><div class="tt" id="cUbahPw">Ubah Password</div><div class="ds" id="cUbahPwD">Ganti kata sandi akun Anda sendiri.</div></div>
  </a>
  <a class="set-card" href="<%=rootCtx%>/baru2?modul=pengaturan&p=reset_password">
    <div class="ic"><i class="fa-solid fa-unlock"></i></div>
    <div><div class="tt" id="cResetPw">Reset Password</div><div class="ds" id="cResetPwD">Reset kata sandi pengguna lain (admin).</div></div>
  </a>
</div>

<script>
document.addEventListener('DOMContentLoaded',function(){
    if(window.AIS2&&AIS2.L){
        var m={judulHalaman:['Pengaturan','Pengaturan'],deskripsiHalaman:['PengaturanDeskripsi','Pusat pengaturan pengguna, grup, toko/perusahaan, dan sistem'],
            secUserToko:['PenggunaOrganisasi','Pengguna & Organisasi'],secSistem:['SistemKeamanan','Sistem & Keamanan'],
            cGrup:['GrupPengguna','Grup Pengguna'],cUser:['Pengguna','Pengguna'],cToko:['TokoPerusahaan','Toko / Perusahaan'],
            cLabel:['LabelBahasa','Label Bahasa'],cUbahPw:['UbahPassword','Ubah Password'],cResetPw:['ResetPassword','Reset Password']};
        for(var id in m){var el=document.getElementById(id);if(el)el.textContent=AIS2.L(m[id][0],m[id][1]);}
    }
});
</script>
</body></html>
