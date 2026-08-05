<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Pengadaan &amp; Aset</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#854d0e,#713f12)">
  <div class="md-ico"><i class="fa-solid fa-boxes-stacked"></i></div>
  <div><h2>Pengadaan &amp; Aset</h2><p>Aset &amp; inventaris kampus, peminjaman, vendor, seleksi &amp; laporan pengadaan</p></div>
</div>
<div class="md-stats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#854d0e,#713f12)"><i class="fa-solid fa-box"></i></div><div class="md-sv" id="sAset">—</div><div class="md-sl">Total Aset</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-spinner"></i></div><div class="md-sv" id="sProses">—</div><div class="md-sl">Sedang Diproses</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-arrow-right-arrow-left"></i></div><div class="md-sv" id="sPinjam">—</div><div class="md-sl">Aset Dipinjam</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-truck-moving"></i></div><div class="md-sv" id="sVendor">—</div><div class="md-sl">Vendor Aktif</div></div>
</div>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=Aset"><div class="md-li" style="background:linear-gradient(135deg,#854d0e,#713f12)"><i class="fa-solid fa-box-archive"></i></div><div><h4>Data Aset</h4><p>Inventaris &amp; kelola aset kampus</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=PeminjamanAset"><div class="md-li" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-arrow-right-arrow-left"></i></div><div><h4>Peminjaman Aset</h4><p>Pengajuan &amp; pengembalian</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=PurchaseRequest"><div class="md-li" style="background:linear-gradient(135deg,#4f46e5,#7c3aed)"><i class="fa-solid fa-cart-plus"></i></div><div><h4>Purchase Request</h4><p>Pengajuan kebutuhan pengadaan</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=Vendor"><div class="md-li" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-truck-moving"></i></div><div><h4>Kelola Vendor</h4><p>Database &amp; seleksi vendor</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=OpnameAset"><div class="md-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-clipboard-list"></i></div><div><h4>Opname Aset</h4><p>Pengecekan fisik &amp; kondisi</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=LaporanAset"><div class="md-li" style="background:linear-gradient(135deg,#e11d48,#be185d)"><i class="fa-solid fa-file-chart-column"></i></div><div><h4>Laporan Pengadaan</h4><p>Rekap belanja &amp; nilai aset</p></div></a>
</div>
<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('pengadaan','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return; var d=r.data;
    document.getElementById('sAset').textContent=d.totalAset||'—';
    document.getElementById('sProses').textContent=d.sedangDiproses||'—';
    document.getElementById('sPinjam').textContent=d.asetDipinjam||'—';
    document.getElementById('sVendor').textContent=d.vendorAktif||'—';
  });
});
</script></body></html>
