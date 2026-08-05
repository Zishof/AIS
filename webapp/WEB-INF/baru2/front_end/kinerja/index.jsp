<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Kinerja</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#9333ea,#7e22ce)">
  <div class="md-ico"><i class="fa-solid fa-chart-line"></i></div>
  <div><h2>Kinerja</h2><p>SKP &amp; KPI dosen/pegawai, evaluasi, angket EDOM &amp; laporan kinerja terstruktur</p></div>
</div>
<div class="md-stats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#9333ea,#7e22ce)"><i class="fa-solid fa-users"></i></div><div class="md-sv" id="sSudahSkp">—</div><div class="md-sl">Sudah Isi SKP</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#ef4444,#dc2626)"><i class="fa-solid fa-user-clock"></i></div><div class="md-sv" id="sBelumSkp">—</div><div class="md-sl">Belum Isi SKP</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-square-poll-horizontal"></i></div><div class="md-sv" id="sEdom">—</div><div class="md-sl">Resp. EDOM</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-star"></i></div><div class="md-sv" id="sRataKpi">—</div><div class="md-sl">Rata Nilai KPI</div></div>
</div>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=SKP"><div class="md-li" style="background:linear-gradient(135deg,#9333ea,#7e22ce)"><i class="fa-solid fa-bullseye"></i></div><div><h4>SKP / Sasaran Kinerja</h4><p>Isi &amp; verifikasi sasaran kinerja</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=KPI"><div class="md-li" style="background:linear-gradient(135deg,#4f46e5,#6d28d9)"><i class="fa-solid fa-chart-line"></i></div><div><h4>KPI Institusi</h4><p>Indikator &amp; capaian organisasi</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=EDOM"><div class="md-li" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-square-poll-horizontal"></i></div><div><h4>Angket EDOM</h4><p>Evaluasi dosen oleh mahasiswa</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=EvaluasiKinerja"><div class="md-li" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-star-half-stroke"></i></div><div><h4>Evaluasi Kinerja</h4><p>Penilaian tahunan &amp; rekap</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=LaporanKinerja"><div class="md-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-file-chart-column"></i></div><div><h4>Laporan Kinerja</h4><p>Export &amp; analisis kinerja</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=KursusInternal"><div class="md-li" style="background:linear-gradient(135deg,#e11d48,#be185d)"><i class="fa-solid fa-chalkboard-user"></i></div><div><h4>Kursus Internal</h4><p>Platform pengembangan SDM</p></div></a>
</div>
<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('kinerja','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return; var d=r.data;
    document.getElementById('sSudahSkp').textContent=d.sudahSkp||'—';
    document.getElementById('sBelumSkp').textContent=d.belumSkp||'—';
    document.getElementById('sEdom').textContent=d.respondenEdom||'—';
    document.getElementById('sRataKpi').textContent=d.rataKpi||'—';
  });
});
</script></body></html>
