<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Neo Feeder</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#1e40af,#1d4ed8)">
  <div class="md-ico"><i class="fa-solid fa-cloud-arrow-up"></i></div>
  <div><h2>Neo Feeder PDDikti</h2><p>Sinkronisasi dua arah dengan PDDikti: perbandingan, validasi &amp; pengiriman data</p></div>
</div>
<div class="md-stats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#1e40af,#1d4ed8)"><i class="fa-solid fa-cloud-arrow-up"></i></div><div class="md-sv" id="sSinkron">—</div><div class="md-sl">Terkirim Semester Ini</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#ef4444,#dc2626)"><i class="fa-solid fa-triangle-exclamation"></i></div><div class="md-sv" id="sKonflik">—</div><div class="md-sl">Data Konflik</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-clock"></i></div><div class="md-sv" id="sPending">—</div><div class="md-sl">Menunggu Kirim</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-circle-check"></i></div><div class="md-sv" id="sOk">—</div><div class="md-sl">Valid &amp; Sinkron</div></div>
</div>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=Feeder"><div class="md-li" style="background:linear-gradient(135deg,#1e40af,#1d4ed8)"><i class="fa-solid fa-cloud-arrow-up"></i></div><div><h4>Kirim ke PDDikti</h4><p>Sync mahasiswa, dosen &amp; nilai</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=BandingFeeder"><div class="md-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-code-compare"></i></div><div><h4>Bandingkan Data</h4><p>Temukan perbedaan lokal vs PDDikti</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=KonflikFeeder"><div class="md-li" style="background:linear-gradient(135deg,#ef4444,#dc2626)"><i class="fa-solid fa-triangle-exclamation"></i></div><div><h4>Data Konflik</h4><p>Selesaikan ketidakcocokan</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=LogFeeder"><div class="md-li" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-clock-rotate-left"></i></div><div><h4>Log &amp; Riwayat</h4><p>Riwayat pengiriman &amp; error</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=SettingFeeder"><div class="md-li" style="background:linear-gradient(135deg,#475569,#334155)"><i class="fa-solid fa-sliders"></i></div><div><h4>Konfigurasi</h4><p>Koneksi &amp; credential PDDikti</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=AmbildataFeeder"><div class="md-li" style="background:linear-gradient(135deg,#7c3aed,#6d28d9)"><i class="fa-solid fa-cloud-arrow-down"></i></div><div><h4>Ambil dari PDDikti</h4><p>Import data referensi nasional</p></div></a>
</div>
<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('feeder','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return; var d=r.data;
    document.getElementById('sSinkron').textContent=d.terkirim||'—';
    document.getElementById('sKonflik').textContent=d.konflik||'—';
    document.getElementById('sPending').textContent=d.pending||'—';
    document.getElementById('sOk').textContent=d.validOk||'—';
  });
});
</script></body></html>
