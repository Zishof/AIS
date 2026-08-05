<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Administrasi</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#b45309,#92400e)">
  <div class="md-ico"><i class="fa-solid fa-book-bookmark"></i></div>
  <div><h2>Administrasi</h2><p>Persuratan, SOP &amp; workflow, alumni, tracer study &amp; pengaduan terpadu</p></div>
</div>
<div class="md-stats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#b45309,#92400e)"><i class="fa-solid fa-envelope-open-text"></i></div><div class="md-sv" id="sSurat">—</div><div class="md-sl">Surat Bulan Ini</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#4f46e5,#7c3aed)"><i class="fa-solid fa-sitemap"></i></div><div class="md-sv" id="sSop">—</div><div class="md-sl">SOP Aktif</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-headset"></i></div><div class="md-sv" id="sPengaduan">—</div><div class="md-sl">Pengaduan Pending</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-people-roof"></i></div><div class="md-sv" id="sAlumni">—</div><div class="md-sl">Alumni Terdaftar</div></div>
</div>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=Surat"><div class="md-li" style="background:linear-gradient(135deg,#b45309,#92400e)"><i class="fa-solid fa-envelope-open-text"></i></div><div><h4>Persuratan</h4><p>Surat masuk, keluar &amp; disposisi</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=SOP"><div class="md-li" style="background:linear-gradient(135deg,#4f46e5,#7c3aed)"><i class="fa-solid fa-sitemap"></i></div><div><h4>SOP &amp; Workflow</h4><p>Alur proses &amp; persetujuan</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=Pengaduan"><div class="md-li" style="background:linear-gradient(135deg,#ef4444,#dc2626)"><i class="fa-solid fa-headset"></i></div><div><h4>Pengaduan Terpadu</h4><p>e-Ticketing &amp; tindak lanjut</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=Alumni"><div class="md-li" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-people-roof"></i></div><div><h4>Alumni &amp; Tracer</h4><p>Database alumni &amp; tracer study</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=Sertifikat"><div class="md-li" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-certificate"></i></div><div><h4>Sertifikat Digital</h4><p>Generate &amp; validasi QR code</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=Pengumuman"><div class="md-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-bullhorn"></i></div><div><h4>Pengumuman</h4><p>Publikasi &amp; notifikasi kampus</p></div></a>
</div>
<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('administrasi','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return; var d=r.data;
    document.getElementById('sSurat').textContent=d.suratBulanIni||'—';
    document.getElementById('sSop').textContent=d.sopAktif||'—';
    document.getElementById('sPengaduan').textContent=d.pengaduanPending||'—';
    document.getElementById('sAlumni').textContent=d.alumni||'—';
  });
});
</script></body></html>
