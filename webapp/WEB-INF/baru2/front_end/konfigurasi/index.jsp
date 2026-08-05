<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common,ais.database.model.Tbmuser" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT;
   Object _loK = session.getAttribute("login");
   Tbmuser loginUser = (_loK instanceof Tbmuser) ? (Tbmuser)_loK :
       (session.getAttribute("usersTemp") instanceof Tbmuser ? (Tbmuser)session.getAttribute("usersTemp") : null);
   boolean isRoot = loginUser != null && loginUser.isRoot();
%>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Konfigurasi Sistem</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#374151,#1f2937)">
  <div class="md-ico"><i class="fa-solid fa-gear"></i></div>
  <div><h2>Konfigurasi &amp; Pengaturan</h2><p>Parameterisasi sistem, integrasi, hak akses &amp; personalisasi platform</p></div>
</div>

<% if (isRoot) { %>
<div class="alert alert-warning d-flex align-items-center gap-2 mb-3" style="border-radius:12px;font-size:.9rem">
  <i class="fa-solid fa-shield-halved"></i>
  <span>Anda login sebagai <strong>Root/Super Admin</strong> — seluruh konfigurasi dapat diubah.</span>
</div>
<% } %>

<h5 class="mb-3" style="color:var(--ais2-text);font-weight:700">Pengaturan Sistem</h5>
<div class="md-links mb-4">
  <a class="md-lc" href="<%=ctx%>/baru2?modul=pengaturan&page=pengguna"><div class="md-li" style="background:linear-gradient(135deg,#374151,#1f2937)"><i class="fa-solid fa-users-gear"></i></div><div><h4>Pengguna &amp; Grup</h4><p>Akun, role &amp; hak akses</p></div></a>
  <a class="md-lc" href="<%=ctx%>/baru2?modul=pengaturan&page=modul"><div class="md-li" style="background:linear-gradient(135deg,#4f46e5,#7c3aed)"><i class="fa-solid fa-language"></i></div><div><h4>Label Bahasa</h4><p>Terjemahan &amp; multi-bahasa</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=SettingInstitusi"><div class="md-li" style="background:linear-gradient(135deg,#0369a1,#0c4a6e)"><i class="fa-solid fa-building-columns"></i></div><div><h4>Profil Institusi</h4><p>Logo, nama, alamat &amp; NPSN</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=SettingAkademik"><div class="md-li" style="background:linear-gradient(135deg,#1e40af,#1d4ed8)"><i class="fa-solid fa-graduation-cap"></i></div><div><h4>Konfigurasi Akademik</h4><p>Semester aktif, satuan kredit, kurikulum</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=SettingKeuangan"><div class="md-li" style="background:linear-gradient(135deg,#16a34a,#15803d)"><i class="fa-solid fa-money-bill-trend-up"></i></div><div><h4>Konfigurasi Keuangan</h4><p>Tagihan, VA, gateway &amp; akun jurnal</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=SettingEmail"><div class="md-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-envelope-circle-check"></i></div><div><h4>Email &amp; Notifikasi</h4><p>SMTP, template &amp; WhatsApp gateway</p></div></a>
</div>

<h5 class="mb-3" style="color:var(--ais2-text);font-weight:700">Integrasi Eksternal</h5>
<div class="md-links mb-4">
  <a class="md-lc" href="<%=ctx%>/main?modul=SettingFeeder"><div class="md-li" style="background:linear-gradient(135deg,#1e40af,#1d4ed8)"><i class="fa-solid fa-cloud-arrow-up"></i></div><div><h4>Neo Feeder PDDikti</h4><p>Credential &amp; endpoint Feeder</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=SisterConfig"><div class="md-li" style="background:linear-gradient(135deg,#0f766e,#115e59)"><i class="fa-solid fa-cloud-arrow-down"></i></div><div><h4>Konfigurasi SISTER</h4><p>Token API &amp; sinkronisasi SDM</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=SettingOAuth"><div class="md-li" style="background:linear-gradient(135deg,#dc2626,#b91c1c)"><i class="fa-brands fa-google"></i></div><div><h4>OAuth &amp; SSO</h4><p>Google, Facebook &amp; LDAP</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=SettingGDrive"><div class="md-li" style="background:linear-gradient(135deg,#065f46,#047857)"><i class="fa-brands fa-google-drive"></i></div><div><h4>Google Drive</h4><p>Backup &amp; storage dokumen</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=SettingAI"><div class="md-li" style="background:linear-gradient(135deg,#7c3aed,#6d28d9)"><i class="fa-solid fa-robot"></i></div><div><h4>Integrasi AI</h4><p>Ollama, Gemini &amp; OpenAI</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=SettingOllama"><div class="md-li" style="background:linear-gradient(135deg,#475569,#334155)"><i class="fa-solid fa-microchip"></i></div><div><h4>Server Ollama Lokal</h4><p>Model &amp; endpoint AI lokal</p></div></a>
</div>

<% if (isRoot) { %>
<h5 class="mb-3" style="color:var(--ais2-text);font-weight:700">Sistem &amp; Debug (Root Only)</h5>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=LogAudit"><div class="md-li" style="background:linear-gradient(135deg,#374151,#1f2937)"><i class="fa-solid fa-scroll"></i></div><div><h4>Log Audit</h4><p>Riwayat aktivitas pengguna</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=Performa"><div class="md-li" style="background:linear-gradient(135deg,#0891b2,#0e7490)"><i class="fa-solid fa-gauge-high"></i></div><div><h4>Monitor Performa</h4><p>Pool DB, heap &amp; thread</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=CacheManager"><div class="md-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-database"></i></div><div><h4>Cache &amp; Snapshot</h4><p>Bersihkan &amp; rebuild cache</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=MenuAdmin"><div class="md-li" style="background:linear-gradient(135deg,#4f46e5,#7c3aed)"><i class="fa-solid fa-sitemap"></i></div><div><h4>Kelola Menu</h4><p>Struktur &amp; visibilitas menu</p></div></a>
</div>
<% } %>
</body></html>
