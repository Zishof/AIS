<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Info Kegiatan</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#7c3aed,#5b21b6)">
  <div class="md-ico"><i class="fa-solid fa-calendar-check"></i></div>
  <div><h2>Info &amp; Kegiatan Kampus</h2><p>Pengumuman, agenda, berita resmi, pendaftaran kegiatan &amp; notifikasi civitas</p></div>
</div>
<div class="md-stats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#7c3aed,#5b21b6)"><i class="fa-solid fa-bullhorn"></i></div><div class="md-sv" id="sPengumuman">—</div><div class="md-sl">Pengumuman Aktif</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-calendar-day"></i></div><div class="md-sv" id="sAgenda">—</div><div class="md-sl">Agenda Bulan Ini</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-newspaper"></i></div><div class="md-sv" id="sBerita">—</div><div class="md-sl">Berita Terkini</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-bell"></i></div><div class="md-sv" id="sNotif">—</div><div class="md-sl">Notifikasi Belum Dibaca</div></div>
</div>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=Pengumuman"><div class="md-li" style="background:linear-gradient(135deg,#7c3aed,#5b21b6)"><i class="fa-solid fa-bullhorn"></i></div><div><h4>Pengumuman</h4><p>Kelola &amp; publikasi pengumuman</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=AgendaKampus"><div class="md-li" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-calendar-day"></i></div><div><h4>Agenda Kegiatan</h4><p>Jadwal acara &amp; seminar</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=BeritaKampus"><div class="md-li" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-newspaper"></i></div><div><h4>Berita &amp; Artikel</h4><p>Publikasi berita resmi kampus</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=DaftarKegiatan"><div class="md-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-clipboard-list"></i></div><div><h4>Daftar Kegiatan</h4><p>Peserta &amp; laporan kegiatan</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=BannerSlide"><div class="md-li" style="background:linear-gradient(135deg,#e11d48,#be185d)"><i class="fa-solid fa-image"></i></div><div><h4>Banner &amp; Slide</h4><p>Konten beranda &amp; papan digital</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=NotifikasiMassal"><div class="md-li" style="background:linear-gradient(135deg,#475569,#334155)"><i class="fa-solid fa-paper-plane"></i></div><div><h4>Kirim Notifikasi</h4><p>Push &amp; email ke civitas</p></div></a>
</div>
<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('kegiatan','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return; var d=r.data;
    document.getElementById('sPengumuman').textContent=d.pengumumanAktif||'—';
    document.getElementById('sAgenda').textContent=d.agendaBulanIni||'—';
    document.getElementById('sBerita').textContent=d.beritaTerkini||'—';
    document.getElementById('sNotif').textContent=d.notifBelumDibaca||'—';
  });
});
</script></body></html>
