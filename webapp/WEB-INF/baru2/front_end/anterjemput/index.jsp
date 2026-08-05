<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Antar Jemput</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#0891b2,#0e7490)">
  <div class="md-ico"><i class="fa-solid fa-bus"></i></div>
  <div><h2>Layanan Antar Jemput</h2><p>Manajemen armada bus, rute, jadwal penjemputan &amp; absensi penumpang</p></div>
</div>
<div class="md-stats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#0891b2,#0e7490)"><i class="fa-solid fa-bus"></i></div><div class="md-sv" id="sArmada">—</div><div class="md-sl">Armada Beroperasi</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-users"></i></div><div class="md-sv" id="sPenumpang">—</div><div class="md-sl">Penumpang Hari Ini</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-route"></i></div><div class="md-sv" id="sRute">—</div><div class="md-sl">Rute Aktif</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#7c3aed,#6d28d9)"><i class="fa-solid fa-id-card"></i></div><div class="md-sv" id="sTerdaftar">—</div><div class="md-sl">Peserta Terdaftar</div></div>
</div>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=ArmadaBus"><div class="md-li" style="background:linear-gradient(135deg,#0891b2,#0e7490)"><i class="fa-solid fa-bus"></i></div><div><h4>Data Armada</h4><p>Kendaraan, kapasitas &amp; kondisi</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=RuteAntarJemput"><div class="md-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-route"></i></div><div><h4>Rute &amp; Titik Stop</h4><p>Kelola jalur &amp; halte</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=JadwalAntarJemput"><div class="md-li" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-clock"></i></div><div><h4>Jadwal Keberangkatan</h4><p>Jam berangkat pagi &amp; sore</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=PesertaAntarJemput"><div class="md-li" style="background:linear-gradient(135deg,#7c3aed,#6d28d9)"><i class="fa-solid fa-users"></i></div><div><h4>Peserta Terdaftar</h4><p>Daftar &amp; kelola peserta</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=AbsensiAntarJemput"><div class="md-li" style="background:linear-gradient(135deg,#4f46e5,#6d28d9)"><i class="fa-solid fa-fingerprint"></i></div><div><h4>Absensi Penumpang</h4><p>Rekap kehadiran per rute</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=LaporanAntarJemput"><div class="md-li" style="background:linear-gradient(135deg,#16a34a,#15803d)"><i class="fa-solid fa-chart-bar"></i></div><div><h4>Laporan</h4><p>Statistik pemakaian &amp; BBM</p></div></a>
</div>
<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('anterjemput','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return; var d=r.data;
    document.getElementById('sArmada').textContent=d.armadaBeroperasi||'—';
    document.getElementById('sPenumpang').textContent=d.penumpangHariIni||'—';
    document.getElementById('sRute').textContent=d.ruteAktif||'—';
    document.getElementById('sTerdaftar').textContent=d.pesertaTerdaftar||'—';
  });
});
</script></body></html>
