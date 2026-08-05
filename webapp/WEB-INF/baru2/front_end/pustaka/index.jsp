<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Perpustakaan</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#0369a1,#0c4a6e)">
  <div class="md-ico"><i class="fa-solid fa-book-open"></i></div>
  <div><h2>Perpustakaan &amp; eLibrary</h2><p>Sirkulasi, katalog digital, barcoding, kunjungan &amp; baca buku online</p></div>
</div>
<div class="md-stats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#0369a1,#0c4a6e)"><i class="fa-solid fa-book"></i></div><div class="md-sv" id="sKoleksi">—</div><div class="md-sl">Total Koleksi</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-hand-holding-hand"></i></div><div class="md-sv" id="sDipinjam">—</div><div class="md-sl">Sedang Dipinjam</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-id-card"></i></div><div class="md-sv" id="sAnggota">—</div><div class="md-sl">Anggota Aktif</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#7c3aed,#6d28d9)"><i class="fa-solid fa-door-open"></i></div><div class="md-sv" id="sKunjungan">—</div><div class="md-sl">Kunjungan Hari Ini</div></div>
</div>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=Katalog"><div class="md-li" style="background:linear-gradient(135deg,#0369a1,#0c4a6e)"><i class="fa-solid fa-magnifying-glass"></i></div><div><h4>Katalog Koleksi</h4><p>Cari &amp; lihat ketersediaan</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=Sirkulasi"><div class="md-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-rotate"></i></div><div><h4>Sirkulasi</h4><p>Peminjaman &amp; pengembalian</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=AnggotaPustaka"><div class="md-li" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-users"></i></div><div><h4>Anggota</h4><p>Registrasi &amp; kartu anggota</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=eLibrary"><div class="md-li" style="background:linear-gradient(135deg,#4f46e5,#7c3aed)"><i class="fa-solid fa-tablet-screen-button"></i></div><div><h4>eLibrary</h4><p>Baca buku &amp; jurnal online</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=KunjunganPustaka"><div class="md-li" style="background:linear-gradient(135deg,#e11d48,#be185d)"><i class="fa-solid fa-door-open"></i></div><div><h4>Kunjungan</h4><p>Log kunjungan &amp; statistik</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=LaporanPustaka"><div class="md-li" style="background:linear-gradient(135deg,#16a34a,#15803d)"><i class="fa-solid fa-chart-bar"></i></div><div><h4>Laporan Pustaka</h4><p>Rekap pinjam &amp; kunjungan</p></div></a>
</div>
<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('pustaka','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return; var d=r.data;
    document.getElementById('sKoleksi').textContent=d.totalKoleksi||'—';
    document.getElementById('sDipinjam').textContent=d.sedangDipinjam||'—';
    document.getElementById('sAnggota').textContent=d.anggotaAktif||'—';
    document.getElementById('sKunjungan').textContent=d.kunjunganHariIni||'—';
  });
});
</script></body></html>
