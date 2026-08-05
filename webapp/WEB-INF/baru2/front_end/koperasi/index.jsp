<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Koperasi</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#0f766e,#0d9488)">
  <div class="md-ico"><i class="fa-solid fa-handshake"></i></div>
  <div><h2>Koperasi</h2><p>Simpan pinjam, anggota, jasa, laporan koperasi kampus terintegrasi</p></div>
</div>
<div class="md-stats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#0f766e,#0d9488)"><i class="fa-solid fa-users"></i></div><div class="md-sv" id="sAnggota">—</div><div class="md-sl">Anggota Aktif</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-piggy-bank"></i></div><div class="md-sv" id="sSimpanan">—</div><div class="md-sl">Total Simpanan</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-hand-holding-dollar"></i></div><div class="md-sv" id="sPinjaman">—</div><div class="md-sl">Pinjaman Aktif</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#4f46e5,#7c3aed)"><i class="fa-solid fa-chart-line"></i></div><div class="md-sv" id="sSHU">—</div><div class="md-sl">SHU Tahun Ini</div></div>
</div>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=AnggotaKoperasi"><div class="md-li" style="background:linear-gradient(135deg,#0f766e,#0d9488)"><i class="fa-solid fa-id-badge"></i></div><div><h4>Data Anggota</h4><p>Daftar &amp; kelola anggota</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=SimpananKoperasi"><div class="md-li" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-piggy-bank"></i></div><div><h4>Simpanan</h4><p>Simpanan pokok, wajib &amp; sukarela</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=PinjamanKoperasi"><div class="md-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-hand-holding-dollar"></i></div><div><h4>Pinjaman</h4><p>Pengajuan &amp; cicilan pinjaman</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=JasaKoperasi"><div class="md-li" style="background:linear-gradient(135deg,#4f46e5,#7c3aed)"><i class="fa-solid fa-percent"></i></div><div><h4>Jasa &amp; SHU</h4><p>Perhitungan &amp; pembagian SHU</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=LaporanKoperasi"><div class="md-li" style="background:linear-gradient(135deg,#e11d48,#be185d)"><i class="fa-solid fa-file-chart-column"></i></div><div><h4>Laporan Koperasi</h4><p>Neraca &amp; buku simpan pinjam</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=RAT"><div class="md-li" style="background:linear-gradient(135deg,#0891b2,#0e7490)"><i class="fa-solid fa-gavel"></i></div><div><h4>RAT</h4><p>Rapat Anggota Tahunan</p></div></a>
</div>
<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('koperasi','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return; var d=r.data;
    document.getElementById('sAnggota').textContent=d.anggotaAktif||'—';
    document.getElementById('sSimpanan').textContent=d.totalSimpanan||'—';
    document.getElementById('sPinjaman').textContent=d.pinjamanAktif||'—';
    document.getElementById('sSHU').textContent=d.shuTahunIni||'—';
  });
});
</script></body></html>
