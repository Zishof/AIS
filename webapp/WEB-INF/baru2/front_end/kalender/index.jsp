<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Kalender Akademik</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#d97706,#b45309)">
  <div class="md-ico"><i class="fa-solid fa-calendar-days"></i></div>
  <div><h2>Kalender Akademik</h2><p>Jadwal kegiatan resmi semester: UTS, UAS, registrasi, wisuda &amp; hari libur akademik</p></div>
</div>
<div class="md-stats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#d97706,#b45309)"><i class="fa-solid fa-calendar-check"></i></div><div class="md-sv" id="sKegiatan">—</div><div class="md-sl">Kegiatan Bulan Ini</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-clock"></i></div><div class="md-sv" id="sHariIni">—</div><div class="md-sl">Hari Akademik Tersisa</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#ef4444,#dc2626)"><i class="fa-solid fa-umbrella-beach"></i></div><div class="md-sv" id="sLibur">—</div><div class="md-sl">Hari Libur</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-calendar-week"></i></div><div class="md-sv" id="sSemester">—</div><div class="md-sl">Pekan Kuliah</div></div>
</div>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=KalenderAkademik"><div class="md-li" style="background:linear-gradient(135deg,#d97706,#b45309)"><i class="fa-solid fa-calendar-days"></i></div><div><h4>Kalender Akademik</h4><p>Kelola jadwal resmi semester</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=TanggalLibur"><div class="md-li" style="background:linear-gradient(135deg,#ef4444,#dc2626)"><i class="fa-solid fa-umbrella-beach"></i></div><div><h4>Hari Libur &amp; Cuti Bersama</h4><p>Libur nasional &amp; kampus</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=JadwalUjian"><div class="md-li" style="background:linear-gradient(135deg,#4f46e5,#7c3aed)"><i class="fa-solid fa-pen-to-square"></i></div><div><h4>Jadwal UTS &amp; UAS</h4><p>Periode &amp; ruangan ujian</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=JadwalRegistrasi"><div class="md-li" href="<%=ctx%>/main?modul=JadwalRegistrasi" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-file-signature"></i></div><div><h4>Jadwal Registrasi</h4><p>KRS, her-registrasi &amp; pembayaran</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=Wisuda"><div class="md-li" style="background:linear-gradient(135deg,#0369a1,#0c4a6e)"><i class="fa-solid fa-graduation-cap"></i></div><div><h4>Wisuda &amp; Pelepasan</h4><p>Jadwal wisuda &amp; data peserta</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=CetakKalender"><div class="md-li" style="background:linear-gradient(135deg,#475569,#334155)"><i class="fa-solid fa-print"></i></div><div><h4>Cetak Kalender</h4><p>Export PDF &amp; bagikan</p></div></a>
</div>
<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('kalender','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return; var d=r.data;
    document.getElementById('sKegiatan').textContent=d.kegiatanBulanIni||'—';
    document.getElementById('sHariIni').textContent=d.hariAkademikTersisa||'—';
    document.getElementById('sLibur').textContent=d.hariLibur||'—';
    document.getElementById('sSemester').textContent=d.pekanKuliah||'—';
  });
});
</script></body></html>
