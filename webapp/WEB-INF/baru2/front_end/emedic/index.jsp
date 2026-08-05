<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>eMedic &amp; SIRS</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#dc2626,#b91c1c)">
  <div class="md-ico"><i class="fa-solid fa-notes-medical"></i></div>
  <div><h2>eMedic &amp; SIRS</h2><p>RME, klaim JKN via VClaim, integrasi SATUSEHAT FHIR &amp; SIRS Fasyankes</p></div>
</div>
<div class="md-stats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#dc2626,#b91c1c)"><i class="fa-solid fa-hospital-user"></i></div><div class="md-sv" id="sPasien">—</div><div class="md-sl">Pasien Hari Ini</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-bed-pulse"></i></div><div class="md-sv" id="sRawatInap">—</div><div class="md-sl">Rawat Inap</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-file-medical"></i></div><div class="md-sv" id="sKlaim">—</div><div class="md-sl">Klaim JKN</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-circle-check"></i></div><div class="md-sv" id="sSatusehat">—</div><div class="md-sl">Kirim SATUSEHAT</div></div>
</div>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=RME"><div class="md-li" style="background:linear-gradient(135deg,#dc2626,#b91c1c)"><i class="fa-solid fa-file-waveform"></i></div><div><h4>Rekam Medis (RME)</h4><p>SOAP, diagnosa &amp; tindakan</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=Pendaftaran"><div class="md-li" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-clipboard-list"></i></div><div><h4>Pendaftaran Pasien</h4><p>Antrian &amp; registrasi</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=VClaim"><div class="md-li" style="background:linear-gradient(135deg,#4f46e5,#7c3aed)"><i class="fa-solid fa-shield-heart"></i></div><div><h4>Klaim JKN (VClaim)</h4><p>SEP, klaim &amp; verifikasi BPJS</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=SATUSEHAT"><div class="md-li" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-cloud-upload-alt"></i></div><div><h4>SATUSEHAT FHIR</h4><p>Kirim data kesehatan nasional</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=ApotekEmedic"><div class="md-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-capsules"></i></div><div><h4>Apotek / Farmasi</h4><p>Resep, stok &amp; pemberian obat</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=SIRS"><div class="md-li" style="background:linear-gradient(135deg,#475569,#334155)"><i class="fa-solid fa-hospital"></i></div><div><h4>SIRS Fasyankes</h4><p>Laporan RS &amp; klinik ke Kemenkes</p></div></a>
</div>
<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('emedic','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return; var d=r.data;
    document.getElementById('sPasien').textContent=d.pasienHariIni||'—';
    document.getElementById('sRawatInap').textContent=d.rawatInap||'—';
    document.getElementById('sKlaim').textContent=d.klaimJkn||'—';
    document.getElementById('sSatusehat').textContent=d.kirimSatusehat||'—';
  });
});
</script></body></html>
