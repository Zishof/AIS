<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Toko &amp; Kantin</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#ea580c,#c2410c)">
  <div class="md-ico"><i class="fa-solid fa-basket-shopping"></i></div>
  <div><h2>Toko &amp; Kantin</h2><p>POS kantin/toko, e-Wallet mahasiswa, stok &amp; laporan omzet real-time</p></div>
</div>
<div class="md-stats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#ea580c,#c2410c)"><i class="fa-solid fa-receipt"></i></div><div class="md-sv" id="sTx">—</div><div class="md-sl">Transaksi Hari Ini</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-coins"></i></div><div class="md-sv" id="sOmzet">—</div><div class="md-sl">Omzet (Ribu)</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-box-open"></i></div><div class="md-sv" id="sProduk">—</div><div class="md-sl">Produk Aktif</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-wallet"></i></div><div class="md-sv" id="sSaldo">—</div><div class="md-sl">Saldo e-Wallet</div></div>
</div>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=POS"><div class="md-li" style="background:linear-gradient(135deg,#ea580c,#c2410c)"><i class="fa-solid fa-cash-register"></i></div><div><h4>Kasir POS</h4><p>Layar kasir &amp; transaksi</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=MenuKantin"><div class="md-li" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-utensils"></i></div><div><h4>Menu &amp; Produk</h4><p>Kelola produk, harga &amp; resep</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=StokBahan"><div class="md-li" style="background:linear-gradient(135deg,#4f46e5,#7c3aed)"><i class="fa-solid fa-boxes-stacked"></i></div><div><h4>Stok &amp; Bahan Baku</h4><p>Kelola stok &amp; HPP</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=TopupWallet"><div class="md-li" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-wallet"></i></div><div><h4>Top-up e-Wallet</h4><p>Isi saldo &amp; riwayat transaksi</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=TenantKantin"><div class="md-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-store"></i></div><div><h4>Tenant Kantin</h4><p>Kelola penyewa &amp; kios</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=LaporanKantin"><div class="md-li" style="background:linear-gradient(135deg,#16a34a,#15803d)"><i class="fa-solid fa-chart-bar"></i></div><div><h4>Laporan Omzet</h4><p>Penjualan harian &amp; bulanan</p></div></a>
</div>
<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('kantin','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return; var d=r.data;
    document.getElementById('sTx').textContent=d.transaksiHariIni||'—';
    document.getElementById('sOmzet').textContent=d.omzetRibu||'—';
    document.getElementById('sProduk').textContent=d.produkAktif||'—';
    document.getElementById('sSaldo').textContent=d.totalSaldoWallet||'—';
  });
});
</script></body></html>
