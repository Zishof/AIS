<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Keuangan</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#16a34a,#15803d)">
  <div class="md-ico"><i class="fa-solid fa-money-bill-trend-up"></i></div>
  <div><h2>Keuangan</h2><p>Anggaran &amp; realisasi, kas besar/kecil, uang muka, LPJ, PO/PR/BAST</p></div>
</div>
<div class="md-stats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#16a34a,#15803d)"><i class="fa-solid fa-arrow-trend-up"></i></div><div class="md-sv" id="sPenerimaan">—</div><div class="md-sl">Penerimaan (Juta)</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#ef4444,#dc2626)"><i class="fa-solid fa-arrow-trend-down"></i></div><div class="md-sv" id="sPengeluaran">—</div><div class="md-sl">Pengeluaran (Juta)</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-piggy-bank"></i></div><div class="md-sv" id="sSaldo">—</div><div class="md-sl">Saldo Kas</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-file-invoice"></i></div><div class="md-sv" id="sPending">—</div><div class="md-sl">Tagihan Pending</div></div>
</div>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=RAB"><div class="md-li" style="background:linear-gradient(135deg,#16a34a,#15803d)"><i class="fa-solid fa-calculator"></i></div><div><h4>Anggaran (RAB)</h4><p>Rencana anggaran &amp; realisasi</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=KasBesar"><div class="md-li" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-vault"></i></div><div><h4>Kas Besar &amp; Kecil</h4><p>Mutasi &amp; saldo kas</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=UangMuka"><div class="md-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-money-bill-wave"></i></div><div><h4>Uang Muka / LPJ</h4><p>Pengajuan &amp; pertanggungjawaban</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=PurchaseOrder"><div class="md-li" style="background:linear-gradient(135deg,#7c3aed,#6d28d9)"><i class="fa-solid fa-cart-shopping"></i></div><div><h4>PO / PR / BAST</h4><p>Purchase order &amp; terima barang</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=Termin"><div class="md-li" style="background:linear-gradient(135deg,#e11d48,#be185d)"><i class="fa-solid fa-calendar-check"></i></div><div><h4>Termin Pembayaran</h4><p>Jadwal &amp; realisasi termin</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=LaporanKeuangan"><div class="md-li" style="background:linear-gradient(135deg,#0891b2,#0e7490)"><i class="fa-solid fa-chart-line"></i></div><div><h4>Laporan Keuangan</h4><p>Neraca, L/R &amp; arus kas</p></div></a>
</div>
<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('keuangan','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return; var d=r.data;
    document.getElementById('sPenerimaan').textContent=d.penerimaanJuta||'—';
    document.getElementById('sPengeluaran').textContent=d.pengeluaranJuta||'—';
    document.getElementById('sSaldo').textContent=d.saldoKas||'—';
    document.getElementById('sPending').textContent=d.tagihanPending||'—';
  });
});
</script></body></html>
