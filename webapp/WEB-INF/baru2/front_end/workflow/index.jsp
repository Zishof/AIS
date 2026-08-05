<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Workflow &amp; SOP</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#d97706,#b45309)">
  <div class="md-ico"><i class="fa-solid fa-sitemap"></i></div>
  <div><h2>Workflow &amp; SOP</h2><p>Alur proses kerja fleksibel, pengajuan online, disposisi multi-level &amp; tracking</p></div>
</div>
<div class="md-stats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#d97706,#b45309)"><i class="fa-solid fa-sitemap"></i></div><div class="md-sv" id="sSop">—</div><div class="md-sl">SOP Aktif</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-hourglass-half"></i></div><div class="md-sv" id="sPending">—</div><div class="md-sl">Pengajuan Pending</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-circle-check"></i></div><div class="md-sv" id="sSelesai">—</div><div class="md-sl">Selesai Bulan Ini</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-users"></i></div><div class="md-sv" id="sAktor">—</div><div class="md-sl">Aktor Terlibat</div></div>
</div>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=DaftarSOP"><div class="md-li" style="background:linear-gradient(135deg,#d97706,#b45309)"><i class="fa-solid fa-list-check"></i></div><div><h4>Kelola SOP</h4><p>Buat &amp; atur alur proses</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=PengajuanSOP"><div class="md-li" style="background:linear-gradient(135deg,#4f46e5,#7c3aed)"><i class="fa-solid fa-paper-plane"></i></div><div><h4>Pengajuan Saya</h4><p>Status &amp; riwayat pengajuan</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=DisposisiSOP"><div class="md-li" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-inbox"></i></div><div><h4>Disposisi Masuk</h4><p>Tindak lanjuti pengajuan</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=TemplateSOP"><div class="md-li" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-file-lines"></i></div><div><h4>Template Dokumen</h4><p>Template &amp; variabel surat</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=AktifikasiSOP"><div class="md-li" style="background:linear-gradient(135deg,#e11d48,#be185d)"><i class="fa-solid fa-toggle-on"></i></div><div><h4>Aktivasi &amp; Aktor</h4><p>Hak akses &amp; penanggung jawab</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=LaporanSOP"><div class="md-li" style="background:linear-gradient(135deg,#16a34a,#15803d)"><i class="fa-solid fa-chart-bar"></i></div><div><h4>Laporan &amp; Statistik</h4><p>Analisis lead time &amp; beban</p></div></a>
</div>
<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('workflow','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return; var d=r.data;
    document.getElementById('sSop').textContent=d.sopAktif||'—';
    document.getElementById('sPending').textContent=d.pengajuanPending||'—';
    document.getElementById('sSelesai').textContent=d.selesaiBulanIni||'—';
    document.getElementById('sAktor').textContent=d.aktorTerlibat||'—';
  });
});
</script></body></html>
