<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Prestasi &amp; Kegiatan</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#d97706,#b45309)">
  <div class="md-ico"><i class="fa-solid fa-trophy"></i></div>
  <div><h2>Prestasi &amp; Kegiatan</h2><p>Prestasi mahasiswa/dosen, kegiatan kemahasiswaan, UKM &amp; penghargaan</p></div>
</div>
<div class="md-stats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#d97706,#b45309)"><i class="fa-solid fa-trophy"></i></div><div class="md-sv" id="sPrestasiMhs">—</div><div class="md-sl">Prestasi Mhs (Th ini)</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#7c3aed,#6d28d9)"><i class="fa-solid fa-chalkboard-teacher"></i></div><div class="md-sv" id="sPrestasiDosen">—</div><div class="md-sl">Prestasi Dosen</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-people-group"></i></div><div class="md-sv" id="sUKM">—</div><div class="md-sl">UKM Aktif</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-globe"></i></div><div class="md-sv" id="sNasional">—</div><div class="md-sl">Tingkat Nasional+</div></div>
</div>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=PrestasiMhs"><div class="md-li" style="background:linear-gradient(135deg,#d97706,#b45309)"><i class="fa-solid fa-medal"></i></div><div><h4>Prestasi Mahasiswa</h4><p>Input &amp; verifikasi prestasi</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=PrestasiDosen"><div class="md-li" style="background:linear-gradient(135deg,#7c3aed,#6d28d9)"><i class="fa-solid fa-award"></i></div><div><h4>Prestasi Dosen</h4><p>Penghargaan &amp; hak cipta</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=KegiatanKemahasiswaan"><div class="md-li" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-calendar-star"></i></div><div><h4>Kegiatan</h4><p>Kegiatan &amp; event mahasiswa</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=UKM"><div class="md-li" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-people-group"></i></div><div><h4>UKM &amp; Organisasi</h4><p>Kelola UKM &amp; ormawa</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=SertifikatPrestasi"><div class="md-li" style="background:linear-gradient(135deg,#e11d48,#be185d)"><i class="fa-solid fa-certificate"></i></div><div><h4>Sertifikat</h4><p>Cetak &amp; validasi sertifikat</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=LaporanPrestasi"><div class="md-li" style="background:linear-gradient(135deg,#16a34a,#15803d)"><i class="fa-solid fa-chart-column"></i></div><div><h4>Laporan Prestasi</h4><p>Rekap &amp; analisis capaian</p></div></a>
</div>
<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('prestasi','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return; var d=r.data;
    document.getElementById('sPrestasiMhs').textContent=d.prestasiMhsTahunIni||'—';
    document.getElementById('sPrestasiDosen').textContent=d.prestasiDosen||'—';
    document.getElementById('sUKM').textContent=d.ukmAktif||'—';
    document.getElementById('sNasional').textContent=d.tingkatNasionalPlus||'—';
  });
});
</script></body></html>
