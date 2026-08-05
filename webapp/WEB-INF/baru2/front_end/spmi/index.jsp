<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>SPMI</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#065f46,#047857)">
  <div class="md-ico"><i class="fa-solid fa-clipboard-check"></i></div>
  <div><h2>SPMI &amp; Penjaminan Mutu</h2><p>Standar mutu, instrumen, audit internal, temuan &amp; akreditasi multi-LAM</p></div>
</div>
<div class="md-stats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#065f46,#047857)"><i class="fa-solid fa-shield-check"></i></div><div class="md-sv" id="sStandar">—</div><div class="md-sl">Standar Aktif</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-triangle-exclamation"></i></div><div class="md-sv" id="sTemuan">—</div><div class="md-sl">Temuan Audit</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-circle-check"></i></div><div class="md-sv" id="sSelesai">—</div><div class="md-sl">Temuan Ditangani</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#4f46e5,#7c3aed)"><i class="fa-solid fa-award"></i></div><div class="md-sv" id="sAkreditasi">—</div><div class="md-sl">Akreditasi Aktif</div></div>
</div>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=StandarMutu"><div class="md-li" style="background:linear-gradient(135deg,#065f46,#047857)"><i class="fa-solid fa-file-shield"></i></div><div><h4>Standar Mutu</h4><p>Tetapkan &amp; kelola standar</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=InstrumenAudit"><div class="md-li" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-list-check"></i></div><div><h4>Instrumen &amp; Borang</h4><p>Template audit &amp; self-assessment</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=AuditInternal"><div class="md-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-magnifying-glass"></i></div><div><h4>Audit Mutu Internal</h4><p>Jadwal &amp; proses AMI</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=TemuanAudit"><div class="md-li" style="background:linear-gradient(135deg,#ef4444,#dc2626)"><i class="fa-solid fa-triangle-exclamation"></i></div><div><h4>Temuan &amp; Tindak Lanjut</h4><p>Kelola &amp; pantau perbaikan</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=Akreditasi"><div class="md-li" style="background:linear-gradient(135deg,#4f46e5,#7c3aed)"><i class="fa-solid fa-award"></i></div><div><h4>Akreditasi</h4><p>BAN-PT, LAM &amp; dokumen</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=LaporanSPMI"><div class="md-li" style="background:linear-gradient(135deg,#16a34a,#15803d)"><i class="fa-solid fa-chart-bar"></i></div><div><h4>Laporan SPMI</h4><p>Dashboard mutu &amp; trend</p></div></a>
</div>
<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('spmi','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return; var d=r.data;
    document.getElementById('sStandar').textContent=d.standarAktif||'—';
    document.getElementById('sTemuan').textContent=d.temuanAudit||'—';
    document.getElementById('sSelesai').textContent=d.temuanDitangani||'—';
    document.getElementById('sAkreditasi').textContent=d.akreditasiAktif||'—';
  });
});
</script></body></html>
