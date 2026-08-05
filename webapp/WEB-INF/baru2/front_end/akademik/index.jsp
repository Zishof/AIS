<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Akademik</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#1e40af,#4f46e5)">
  <div class="md-ico"><i class="fa-solid fa-graduation-cap"></i></div>
  <div><h2>Dasbor Akademik</h2><p>KRS, penilaian, kurikulum, bimbingan, penjadwalan &amp; kelulusan terintegrasi</p></div>
</div>
<div class="md-stats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#1e40af,#4f46e5)"><i class="fa-solid fa-users"></i></div><div class="md-sv" id="sMhs">—</div><div class="md-sl">Mahasiswa Aktif</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-chalkboard-user"></i></div><div class="md-sv" id="sKelas">—</div><div class="md-sl">Kelas Aktif</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-scroll"></i></div><div class="md-sv" id="sKrs">—</div><div class="md-sl">KRS Disetujui</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#7c3aed,#6d28d9)"><i class="fa-solid fa-user-graduate"></i></div><div class="md-sv" id="sLulus">—</div><div class="md-sl">Lulus Semester Ini</div></div>
</div>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=KRS"><div class="md-li" style="background:linear-gradient(135deg,#1e40af,#4f46e5)"><i class="fa-solid fa-scroll"></i></div><div><h4>KRS Mahasiswa</h4><p>Pengisian, persetujuan &amp; rekap KRS</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=Penjadwalan"><div class="md-li" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-calendar-days"></i></div><div><h4>Penjadwalan</h4><p>Jadwal perkuliahan &amp; ruang</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=Penilaian"><div class="md-li" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-star-half-stroke"></i></div><div><h4>Penilaian</h4><p>Entri nilai, rekap &amp; konversi</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=Perkuliahan"><div class="md-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-book-open-reader"></i></div><div><h4>Perkuliahan</h4><p>Absensi &amp; log pertemuan</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=KKN"><div class="md-li" style="background:linear-gradient(135deg,#e11d48,#be185d)"><i class="fa-solid fa-briefcase"></i></div><div><h4>KKN / PKL / Magang</h4><p>Bimbingan lapangan &amp; logbook</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=Skripsi"><div class="md-li" style="background:linear-gradient(135deg,#7c3aed,#6d28d9)"><i class="fa-solid fa-file-contract"></i></div><div><h4>Skripsi / TA</h4><p>Bimbingan, sidang &amp; status</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=Transkip"><div class="md-li" style="background:linear-gradient(135deg,#0891b2,#0e7490)"><i class="fa-solid fa-file-alt"></i></div><div><h4>Transkrip &amp; KHS</h4><p>Generate &amp; cetak transkrip</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=Wisuda"><div class="md-li" style="background:linear-gradient(135deg,#16a34a,#15803d)"><i class="fa-solid fa-hat-wizard"></i></div><div><h4>Kelulusan &amp; Wisuda</h4><p>Seleksi &amp; pendaftaran wisuda</p></div></a>
</div>
<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('akademik','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return; var d=r.data;
    document.getElementById('sMhs').textContent=d.mahasiswaAktif||'—';
    document.getElementById('sKelas').textContent=d.kelasAktif||'—';
    document.getElementById('sKrs').textContent=d.krsDisetujui||'—';
    document.getElementById('sLulus').textContent=d.lulus||'—';
  });
});
</script></body></html>
