<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Kepegawaian</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#7c3aed,#6d28d9)">
  <div class="md-ico"><i class="fa-solid fa-users-gear"></i></div>
  <div><h2>Kepegawaian</h2><p>Data SDM dosen &amp; tenaga kependidikan, jabatan, golongan &amp; riwayat karir</p></div>
</div>
<div class="md-stats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#7c3aed,#6d28d9)"><i class="fa-solid fa-user-tie"></i></div><div class="md-sv" id="sDosen">—</div><div class="md-sl">Dosen Aktif</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-user-gear"></i></div><div class="md-sv" id="sTendik">—</div><div class="md-sl">Tenaga Kependidikan</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-umbrella-beach"></i></div><div class="md-sv" id="sCutiAktif">—</div><div class="md-sl">Cuti Bulan Ini</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-id-card"></i></div><div class="md-sv" id="sKontrak">—</div><div class="md-sl">Kontrak Aktif</div></div>
</div>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=DataDosen"><div class="md-li" style="background:linear-gradient(135deg,#7c3aed,#6d28d9)"><i class="fa-solid fa-chalkboard-teacher"></i></div><div><h4>Data Dosen</h4><p>Profil, NIDN, jabatan &amp; riwayat</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=DataPegawai"><div class="md-li" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-user-gear"></i></div><div><h4>Data Pegawai</h4><p>Tendik, staf &amp; honorer</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=Jabatan"><div class="md-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-sitemap"></i></div><div><h4>Jabatan &amp; Golongan</h4><p>Struktur &amp; jenjang kepangkatan</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=Cuti"><div class="md-li" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-umbrella-beach"></i></div><div><h4>Cuti &amp; Izin</h4><p>Pengajuan, approval &amp; jatah cuti</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=Kontrak"><div class="md-li" style="background:linear-gradient(135deg,#e11d48,#be185d)"><i class="fa-solid fa-file-signature"></i></div><div><h4>Kontrak Kerja</h4><p>Masa berlaku &amp; perpanjangan</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=BKD"><div class="md-li" style="background:linear-gradient(135deg,#16a34a,#15803d)"><i class="fa-solid fa-book-bookmark"></i></div><div><h4>BKD Dosen</h4><p>Beban kerja &amp; Tri Dharma</p></div></a>
</div>
<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('kepegawaian','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return; var d=r.data;
    document.getElementById('sDosen').textContent=d.dosenAktif||'—';
    document.getElementById('sTendik').textContent=d.tendik||'—';
    document.getElementById('sCutiAktif').textContent=d.cutiAktif||'—';
    document.getElementById('sKontrak').textContent=d.kontrakAktif||'—';
  });
});
</script></body></html>
