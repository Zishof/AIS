<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>e-Learning</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
<%@ include file="../_modul_dasbor.jsp" %>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#0ea5e9,#4f46e5)">
  <div class="md-ico"><i class="fa-solid fa-laptop-code"></i></div>
  <div><h2>e-Learning</h2><p>Platform pembelajaran digital: materi, tugas, ujian online, diskusi &amp; anti-curang</p></div>
</div>
<div class="md-stats" id="mdStats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-chalkboard-user"></i></div><div class="md-sv" id="sKelas">—</div><div class="md-sl">Kelas Aktif</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-users"></i></div><div class="md-sv" id="sPeserta">—</div><div class="md-sl">Peserta</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-file-pen"></i></div><div class="md-sv" id="sTugas">—</div><div class="md-sl">Tugas Belum Dinilai</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#7c3aed,#6d28d9)"><i class="fa-solid fa-square-poll-vertical"></i></div><div class="md-sv" id="sUjian">—</div><div class="md-sl">Ujian Aktif</div></div>
</div>
<div class="md-links">
  <a class="md-lc" onclick="buka('perkuliahan')"><div class="md-li" style="background:linear-gradient(135deg,#0ea5e9,#4f46e5)"><i class="fa-solid fa-book-open-reader"></i></div><div><h4>Perkuliahan</h4><p>Daftar kelas &amp; pertemuan aktif</p></div></a>
  <a class="md-lc" onclick="buka('materi')"><div class="md-li" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-file-lines"></i></div><div><h4>Materi &amp; Modul</h4><p>Unggah bahan ajar &amp; video</p></div></a>
  <a class="md-lc" onclick="buka('tugas')"><div class="md-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-file-pen"></i></div><div><h4>Tugas &amp; Kumpul</h4><p>Berikan tugas, kumpulkan &amp; nilai</p></div></a>
  <a class="md-lc" onclick="buka('ujian')"><div class="md-li" style="background:linear-gradient(135deg,#7c3aed,#6d28d9)"><i class="fa-solid fa-clipboard-question"></i></div><div><h4>Ujian Online</h4><p>Bank soal, acak, anti-curang</p></div></a>
  <a class="md-lc" onclick="buka('absensi_online')"><div class="md-li" style="background:linear-gradient(135deg,#e11d48,#be185d)"><i class="fa-solid fa-fingerprint"></i></div><div><h4>Absensi Online</h4><p>Kehadiran digital dengan QR &amp; link</p></div></a>
  <a class="md-lc" onclick="buka('diskusi')"><div class="md-li" style="background:linear-gradient(135deg,#0891b2,#0e7490)"><i class="fa-solid fa-comments"></i></div><div><h4>Diskusi &amp; Forum</h4><p>Ruang diskusi per-kelas</p></div></a>
  <a class="md-lc" onclick="buka('nilai_el')"><div class="md-li" style="background:linear-gradient(135deg,#16a34a,#15803d)"><i class="fa-solid fa-chart-bar"></i></div><div><h4>Penilaian</h4><p>Rekap &amp; export nilai e-Learning</p></div></a>
  <a class="md-lc" onclick="buka('bimbingan_el')"><div class="md-li" style="background:linear-gradient(135deg,#9333ea,#7e22ce)"><i class="fa-solid fa-user-graduate"></i></div><div><h4>Bimbingan Online</h4><p>TA &amp; skripsi via platform e-L</p></div></a>
</div>
<script>
function buka(p){window.parent&&window.parent.openModul?window.parent.openModul('elearning/'+p):void(0);}
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('elearning','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return;
    var d=r.data;
    document.getElementById('sKelas').textContent=d.kelasAktif||'—';
    document.getElementById('sPeserta').textContent=d.peserta||'—';
    document.getElementById('sTugas').textContent=d.tugasBelumDinilai||'—';
    document.getElementById('sUjian').textContent=d.ujianAktif||'—';
  });
});
</script></body></html>
