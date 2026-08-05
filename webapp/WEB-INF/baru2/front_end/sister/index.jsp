<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>SISTER</title><link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">
<div class="md-banner" style="background:linear-gradient(135deg,#0f766e,#115e59)">
  <div class="md-ico"><i class="fa-solid fa-cloud-arrow-down"></i></div>
  <div><h2>Integrasi SISTER</h2><p>Sinkronisasi data SDM dosen ke Sistem Informasi SDM Kemdikbud (SISTER)</p></div>
</div>
<div class="md-stats">
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#0f766e,#115e59)"><i class="fa-solid fa-users"></i></div><div class="md-sv" id="sDosenSync">—</div><div class="md-sl">Dosen Sinkron</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#ef4444,#dc2626)"><i class="fa-solid fa-circle-xmark"></i></div><div class="md-sv" id="sGagal">—</div><div class="md-sl">Gagal Sync</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-clock"></i></div><div class="md-sv" id="sPending">—</div><div class="md-sl">Pending</div></div>
  <div class="md-stat"><div class="md-si" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-circle-check"></i></div><div class="md-sv" id="sOk">—</div><div class="md-sl">Berhasil</div></div>
</div>
<div class="md-links">
  <a class="md-lc" href="<%=ctx%>/main?modul=SisterSync"><div class="md-li" style="background:linear-gradient(135deg,#0f766e,#115e59)"><i class="fa-solid fa-rotate"></i></div><div><h4>Sinkronisasi Data</h4><p>Kirim data dosen ke SISTER</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=SisterBanding"><div class="md-li" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-code-compare"></i></div><div><h4>Bandingkan Data</h4><p>Lokal vs data SISTER</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=SisterLog"><div class="md-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-clock-rotate-left"></i></div><div><h4>Log Sinkronisasi</h4><p>Riwayat &amp; status kirim</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=SisterMatkul"><div class="md-li" style="background:linear-gradient(135deg,#4f46e5,#7c3aed)"><i class="fa-solid fa-book"></i></div><div><h4>Mata Kuliah SISTER</h4><p>Sinkron penugasan MK</p></div></a>
  <a class="md-lc" href="<%=ctx%>/main?modul=SisterConfig"><div class="md-li" style="background:linear-gradient(135deg,#475569,#334155)"><i class="fa-solid fa-sliders"></i></div><div><h4>Konfigurasi</h4><p>Token &amp; setting koneksi SISTER</p></div></a>
</div>
<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('sister','dasbor_stat',{}).then(function(r){
    if(!r||!r.data)return; var d=r.data;
    document.getElementById('sDosenSync').textContent=d.dosenSinkron||'—';
    document.getElementById('sGagal').textContent=d.gagalSync||'—';
    document.getElementById('sPending').textContent=d.pending||'—';
    document.getElementById('sOk').textContent=d.berhasil||'—';
  });
});
</script></body></html>
