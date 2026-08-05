<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Akuntansi</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#1e40af,#1e3a8a)">
  <div class="md-ico"><i class="fa-solid fa-calculator"></i></div>
  <div><h2>Akuntansi</h2><p>Jurnal umum, buku besar, laporan keuangan &amp; akuntansi Yayasan terintegrasi</p></div>
</div>
<div class="md-stats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#1e40af,#1e3a8a)"><i class="fa-solid fa-book"></i></div><div class="md-sv" id="sJurnal">—</div><div class="md-sl">Jurnal Bulan Ini</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#ef4444,#dc2626)"><i class="fa-solid fa-triangle-exclamation"></i></div><div class="md-sv" id="sBelumPosting">—</div><div class="md-sl">Belum Diposting</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-check-double"></i></div><div class="md-sv" id="sPosting">—</div><div class="md-sl">Sudah Diposting</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#7c3aed,#6d28d9)"><i class="fa-solid fa-chart-pie"></i></div><div class="md-sv" id="sAkun">—</div><div class="md-sl">Akun COA</div></div>
</div>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=JurnalUmum"><div class="md-li" style="background:linear-gradient(135deg,#1e40af,#4f46e5)"><i class="fa-solid fa-book-open"></i></div><div><h4>Jurnal Umum</h4><p>Entri &amp; posting jurnal</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=BukuBesar"><div class="md-li" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-book-bookmark"></i></div><div><h4>Buku Besar</h4><p>Mutasi &amp; saldo per akun</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=COA"><div class="md-li" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-list-tree"></i></div><div><h4>Chart of Account</h4><p>Kelola akun &amp; kode akuntansi</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=PostingHPP"><div class="md-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-arrow-right-to-bracket"></i></div><div><h4>Posting HPP</h4><p>Harga pokok &amp; biaya produksi</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=Neraca"><div class="md-li" style="background:linear-gradient(135deg,#7c3aed,#6d28d9)"><i class="fa-solid fa-scale-balanced"></i></div><div><h4>Neraca &amp; L/R</h4><p>Laporan keuangan bulanan</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=ArusKas"><div class="md-li" style="background:linear-gradient(135deg,#e11d48,#be185d)"><i class="fa-solid fa-water"></i></div><div><h4>Arus Kas</h4><p>Cash flow &amp; proyeksi</p></div></a>
</div>
<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('akunting','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return; var d=r.data;
    document.getElementById('sJurnal').textContent=d.jurnalBulanIni||'—';
    document.getElementById('sBelumPosting').textContent=d.belumPosting||'—';
    document.getElementById('sPosting').textContent=d.sudahPosting||'—';
    document.getElementById('sAkun').textContent=d.akunCoa||'—';
  });
});
</script></body></html>
