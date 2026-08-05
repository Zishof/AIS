<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Pembayaran</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#10b981,#059669)">
  <div class="md-ico"><i class="fa-solid fa-credit-card"></i></div>
  <div><h2>Pembayaran</h2><p>Tagihan mahasiswa, multi-channel payment, virtual account &amp; laporan keuangan</p></div>
</div>
<div class="md-stats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-money-bill-wave"></i></div><div class="md-sv" id="sLunas">—</div><div class="md-sl">Lunas Bulan Ini</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#ef4444,#dc2626)"><i class="fa-solid fa-clock"></i></div><div class="md-sv" id="sTunggak">—</div><div class="md-sl">Menunggak</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-coins"></i></div><div class="md-sv" id="sTotal">—</div><div class="md-sl">Total Tagihan (Juta)</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-receipt"></i></div><div class="md-sv" id="sTx">—</div><div class="md-sl">Transaksi Hari Ini</div></div>
</div>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=Tagihan"><div class="md-li" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-file-invoice-dollar"></i></div><div><h4>Tagihan Mahasiswa</h4><p>Setting &amp; monitoring tagihan</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=BayarMhs"><div class="md-li" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-credit-card"></i></div><div><h4>Pembayaran</h4><p>Input &amp; verifikasi bayar manual</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=VirtualAccount"><div class="md-li" style="background:linear-gradient(135deg,#4f46e5,#7c3aed)"><i class="fa-solid fa-qrcode"></i></div><div><h4>Virtual Account</h4><p>BNI, BRI, BSI, Mandiri &amp; lainnya</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=DiskonTagihan"><div class="md-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-tag"></i></div><div><h4>Diskon &amp; Keringanan</h4><p>Beasiswa, cicilan &amp; diskon</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=BeasiswaTagihan"><div class="md-li" style="background:linear-gradient(135deg,#e11d48,#be185d)"><i class="fa-solid fa-hand-holding-dollar"></i></div><div><h4>Beasiswa</h4><p>Beasiswa KIP-K, Yayasan &amp; CSR</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=LaporanPembayaran"><div class="md-li" style="background:linear-gradient(135deg,#16a34a,#15803d)"><i class="fa-solid fa-chart-pie"></i></div><div><h4>Laporan &amp; Rekap</h4><p>Rekap per periode &amp; prodi</p></div></a>
</div>
<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('pembayaran','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return; var d=r.data;
    document.getElementById('sLunas').textContent=d.lunasBulanIni||'—';
    document.getElementById('sTunggak').textContent=d.menunggak||'—';
    document.getElementById('sTotal').textContent=d.totalTagihanJuta||'—';
    document.getElementById('sTx').textContent=d.transaksiHariIni||'—';
  });
});
</script></body></html>
