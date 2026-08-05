<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Repository Institusi</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#475569,#334155)">
  <div class="md-ico"><i class="fa-solid fa-server"></i></div>
  <div><h2>Repository Institusi</h2><p>Repository karya ilmiah mandiri, sinkronisasi otomatis &amp; statistik unduhan</p></div>
</div>
<div class="md-stats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#475569,#334155)"><i class="fa-solid fa-file-lines"></i></div><div class="md-sv" id="sTotal">—</div><div class="md-sl">Total Dokumen</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-upload"></i></div><div class="md-sv" id="sBulanIni">—</div><div class="md-sl">Upload Bulan Ini</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-download"></i></div><div class="md-sv" id="sUnduh">—</div><div class="md-sl">Total Unduhan</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-folder-open"></i></div><div class="md-sv" id="sKategori">—</div><div class="md-sl">Kategori</div></div>
</div>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=UploadRepo"><div class="md-li" style="background:linear-gradient(135deg,#475569,#334155)"><i class="fa-solid fa-file-arrow-up"></i></div><div><h4>Upload Dokumen</h4><p>Unggah karya ilmiah &amp; dokumen</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=CariRepo"><div class="md-li" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-magnifying-glass"></i></div><div><h4>Cari Dokumen</h4><p>Browse &amp; download koleksi</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=KategoriRepo"><div class="md-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-folder-tree"></i></div><div><h4>Kategori &amp; Tag</h4><p>Kelola taksonomi dokumen</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=SinkronRepo"><div class="md-li" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-cloud-arrow-up"></i></div><div><h4>Sinkronisasi</h4><p>Sync ke server publik</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=StatRepo"><div class="md-li" style="background:linear-gradient(135deg,#7c3aed,#6d28d9)"><i class="fa-solid fa-chart-bar"></i></div><div><h4>Statistik</h4><p>Top unduhan &amp; analitik</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=PengaturanRepo"><div class="md-li" style="background:linear-gradient(135deg,#e11d48,#be185d)"><i class="fa-solid fa-sliders"></i></div><div><h4>Pengaturan</h4><p>Konfigurasi &amp; metadata</p></div></a>
</div>
<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('repository','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return; var d=r.data;
    document.getElementById('sTotal').textContent=d.totalDokumen||'—';
    document.getElementById('sBulanIni').textContent=d.uploadBulanIni||'—';
    document.getElementById('sUnduh').textContent=d.totalUnduhan||'—';
    document.getElementById('sKategori').textContent=d.jumlahKategori||'—';
  });
});
</script></body></html>
