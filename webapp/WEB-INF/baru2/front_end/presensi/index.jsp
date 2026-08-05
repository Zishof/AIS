<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Presensi &amp; Kehadiran</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#0891b2,#0e7490)">
  <div class="md-ico"><i class="fa-solid fa-fingerprint"></i></div>
  <div><h2>Presensi &amp; Kehadiran</h2><p>Absensi dosen, pegawai &amp; mahasiswa via fingerprint, RFID, QR &amp; mobile</p></div>
</div>
<div class="md-stats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-user-check"></i></div><div class="md-sv" id="sHadir">—</div><div class="md-sl">Hadir Hari Ini</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#ef4444,#dc2626)"><i class="fa-solid fa-user-xmark"></i></div><div class="md-sv" id="sAbsen">—</div><div class="md-sl">Tidak Hadir</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-clock-rotate-left"></i></div><div class="md-sv" id="sTerlambat">—</div><div class="md-sl">Terlambat</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#0891b2,#0e7490)"><i class="fa-solid fa-percent"></i></div><div class="md-sv" id="sPersen">—</div><div class="md-sl">Persentase Hadir</div></div>
</div>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=AbsensiPegawai"><div class="md-li" style="background:linear-gradient(135deg,#0891b2,#0e7490)"><i class="fa-solid fa-user-tie"></i></div><div><h4>Absensi Pegawai</h4><p>Input &amp; monitoring kehadiran</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=AbsensiMhsDosen"><div class="md-li" style="background:linear-gradient(135deg,#4f46e5,#6d28d9)"><i class="fa-solid fa-users-line"></i></div><div><h4>Absensi Perkuliahan</h4><p>Kehadiran mahasiswa &amp; dosen</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=RekapAbsensi"><div class="md-li" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-table-list"></i></div><div><h4>Rekap Kehadiran</h4><p>Bulanan, harian &amp; per-pegawai</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=JadwalPiket"><div class="md-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-calendar-week"></i></div><div><h4>Jadwal Piket</h4><p>Daftar jaga &amp; absensi piket</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=LaporanPresensi"><div class="md-li" style="background:linear-gradient(135deg,#e11d48,#be185d)"><i class="fa-solid fa-file-export"></i></div><div><h4>Laporan Presensi</h4><p>Export PDF/Excel per periode</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=KonfigurasiAbsensi"><div class="md-li" style="background:linear-gradient(135deg,#16a34a,#15803d)"><i class="fa-solid fa-sliders"></i></div><div><h4>Konfigurasi</h4><p>Toleransi, shift &amp; jam kerja</p></div></a>
</div>
<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('presensi','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return; var d=r.data;
    document.getElementById('sHadir').textContent=d.hadirHariIni||'—';
    document.getElementById('sAbsen').textContent=d.tidakHadir||'—';
    document.getElementById('sTerlambat').textContent=d.terlambat||'—';
    document.getElementById('sPersen').textContent=d.persenHadir||'—';
  });
});
</script></body></html>
