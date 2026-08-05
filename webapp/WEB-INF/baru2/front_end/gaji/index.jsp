<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Penggajian</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#0d9488,#0f766e)">
  <div class="md-ico"><i class="fa-solid fa-money-check-dollar"></i></div>
  <div><h2>Penggajian</h2><p>Payroll terintegrasi SDM: gaji pokok, tunjangan, potongan, slip &amp; PPh 21</p></div>
</div>
<div class="md-stats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#0d9488,#0f766e)"><i class="fa-solid fa-users"></i></div><div class="md-sv" id="sPenerima">—</div><div class="md-sl">Penerima Gaji</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#4f46e5,#7c3aed)"><i class="fa-solid fa-calculator"></i></div><div class="md-sv" id="sTotal">—</div><div class="md-sl">Total Gaji (Juta)</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-file-invoice-dollar"></i></div><div class="md-sv" id="sPph">—</div><div class="md-sl">PPh 21 (Juta)</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#ef4444,#dc2626)"><i class="fa-solid fa-clock"></i></div><div class="md-sv" id="sPending">—</div><div class="md-sl">Belum Diproses</div></div>
</div>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=HitungGaji"><div class="md-li" style="background:linear-gradient(135deg,#0d9488,#0f766e)"><i class="fa-solid fa-calculator"></i></div><div><h4>Hitung Gaji</h4><p>Proses penggajian bulanan</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=SlipGaji"><div class="md-li" style="background:linear-gradient(135deg,#4f46e5,#7c3aed)"><i class="fa-solid fa-file-invoice"></i></div><div><h4>Slip Gaji</h4><p>Cetak &amp; distribusi slip</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=KomponenGaji"><div class="md-li" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-layer-group"></i></div><div><h4>Komponen Gaji</h4><p>Tunjangan, potongan &amp; masa kerja</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=PPh21"><div class="md-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-percent"></i></div><div><h4>PPh Pasal 21</h4><p>Perhitungan &amp; laporan pajak</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=LaporanGaji"><div class="md-li" style="background:linear-gradient(135deg,#16a34a,#15803d)"><i class="fa-solid fa-chart-bar"></i></div><div><h4>Laporan Payroll</h4><p>Rekap &amp; analisis gaji</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=TransferGaji"><div class="md-li" style="background:linear-gradient(135deg,#e11d48,#be185d)"><i class="fa-solid fa-building-columns"></i></div><div><h4>Transfer Bank</h4><p>File transfer gaji via perbankan</p></div></a>
</div>
<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('gaji','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return; var d=r.data;
    document.getElementById('sPenerima').textContent=d.penerima||'—';
    document.getElementById('sTotal').textContent=d.totalGajiJuta||'—';
    document.getElementById('sPph').textContent=d.pphJuta||'—';
    document.getElementById('sPending').textContent=d.pending||'—';
  });
});
</script></body></html>
