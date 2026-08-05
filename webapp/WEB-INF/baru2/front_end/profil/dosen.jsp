<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common,ais.database.model.Tbmuser" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT;
   Object _loDsn = session.getAttribute("login");
   Tbmuser loginUser = (_loDsn instanceof Tbmuser) ? (Tbmuser)_loDsn :
       (session.getAttribute("usersTemp") instanceof Tbmuser ? (Tbmuser)session.getAttribute("usersTemp") : null);
   Object dosen = session.getAttribute("dosen");
   String namaDosen = loginUser != null ? loginUser.getUserNama() : "Dosen";
   try { if (dosen != null) namaDosen = dosen.getClass().getMethod("getNama").invoke(dosen).toString(); } catch(Exception ign){}
%>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Profil Dosen</title>
<link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<style>
.pf-hero{background:linear-gradient(135deg,#7c3aed,#5b21b6);border-radius:18px;padding:28px;display:flex;gap:20px;align-items:center;margin-bottom:20px;color:#fff}
.pf-foto{width:90px;height:90px;border-radius:50%;border:3px solid rgba(255,255,255,.5);object-fit:cover;flex-shrink:0;background:#fff3}
.pf-nama{font-size:1.3rem;font-weight:700;margin:0}
.pf-sub{font-size:.85rem;opacity:.85;margin:2px 0}
.pf-badge{display:inline-block;background:rgba(255,255,255,.2);border-radius:8px;padding:3px 10px;font-size:.78rem;margin-top:6px}
.pf-stats{display:grid;grid-template-columns:repeat(auto-fill,minmax(140px,1fr));gap:14px;margin-bottom:20px}
.pf-stat{background:var(--ais2-card);border-radius:14px;padding:16px;text-align:center;border:1px solid var(--ais2-border)}
.pf-sv{font-size:1.4rem;font-weight:700;color:var(--ais2-primary)}
.pf-sl{font-size:.75rem;color:var(--ais2-muted);margin-top:4px}
.pf-card{background:var(--ais2-card);border-radius:14px;border:1px solid var(--ais2-border);padding:18px;margin-bottom:16px}
.pf-card h5{font-weight:700;margin:0 0 12px;font-size:.95rem;color:var(--ais2-text)}
.pf-links{display:grid;grid-template-columns:repeat(auto-fill,minmax(170px,1fr));gap:12px}
.pf-lc{display:flex;align-items:center;gap:12px;background:var(--ais2-card);border:1px solid var(--ais2-border);border-radius:12px;padding:14px;text-decoration:none;color:var(--ais2-text);transition:.2s}
.pf-lc:hover{border-color:var(--ais2-primary);transform:translateY(-2px);box-shadow:0 4px 16px var(--ais2-shadow)}
.pf-li{width:38px;height:38px;border-radius:10px;display:flex;align-items:center;justify-content:center;color:#fff;font-size:1rem;flex-shrink:0}
@media(max-width:600px){.pf-hero{flex-direction:column;text-align:center}.pf-stats{grid-template-columns:repeat(2,1fr)}}
</style>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">

<div class="pf-hero">
  <img class="pf-foto" src="<%=ctx%>/img/avatar_default.png" alt="foto" onerror="this.src='<%=ctx%>/img/avatar_default.png'">
  <div>
    <p class="pf-nama"><%=namaDosen%></p>
    <p class="pf-sub"><i class="fa-solid fa-id-badge me-1"></i>NIDN: <span id="nidn">—</span></p>
    <p class="pf-sub"><i class="fa-solid fa-envelope me-1"></i><%=loginUser!=null?loginUser.getEmail():""%></p>
    <p class="pf-sub"><i class="fa-solid fa-phone me-1"></i><%=loginUser!=null?loginUser.getHp():""%></p>
    <span class="pf-badge"><i class="fa-solid fa-chalkboard-teacher me-1"></i>Dosen / Tenaga Pengajar</span>
  </div>
</div>

<div class="pf-stats">
  <div class="pf-stat"><div class="pf-sv" id="sKelas">—</div><div class="pf-sl">Kelas Diampu</div></div>
  <div class="pf-stat"><div class="pf-sv" id="sMahasiswa">—</div><div class="pf-sl">Total Mahasiswa</div></div>
  <div class="pf-stat"><div class="pf-sv" id="sBimbingan">—</div><div class="pf-sl">Bimbingan TA</div></div>
  <div class="pf-stat"><div class="pf-sv" id="sPa">—</div><div class="pf-sl">Mahasiswaan PA</div></div>
</div>

<div class="pf-card">
  <h5><i class="fa-solid fa-grid-2 me-2" style="color:#7c3aed"></i>Menu Cepat Dosen</h5>
  <div class="pf-links">
    <a class="pf-lc" href="<%=ctx%>/main?modul=PerkuliahanDosen"><div class="pf-li" style="background:linear-gradient(135deg,#7c3aed,#5b21b6)"><i class="fa-solid fa-chalkboard"></i></div><div><div style="font-weight:600;font-size:.85rem">Perkuliahan</div><div style="font-size:.75rem;color:var(--ais2-muted)">Kelas &amp; absensi</div></div></a>
    <a class="pf-lc" href="<%=ctx%>/main?modul=PenilaianDosen"><div class="pf-li" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-star"></i></div><div><div style="font-weight:600;font-size:.85rem">Penilaian</div><div style="font-size:.75rem;color:var(--ais2-muted)">Input &amp; rekap nilai</div></div></a>
    <a class="pf-lc" href="<%=ctx%>/main?modul=BimbinganTA"><div class="pf-li" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-people-roof"></i></div><div><div style="font-weight:600;font-size:.85rem">Bimbingan TA</div><div style="font-size:.75rem;color:var(--ais2-muted)">Skripsi &amp; tesis</div></div></a>
    <a class="pf-lc" href="<%=ctx%>/main?modul=KrsPA"><div class="pf-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-user-check"></i></div><div><div style="font-weight:600;font-size:.85rem">Setujui KRS</div><div style="font-size:.75rem;color:var(--ais2-muted)">Dosen PA mahasiswa</div></div></a>
    <a class="pf-lc" href="<%=ctx%>/main?modul=MateriDosen"><div class="pf-li" style="background:linear-gradient(135deg,#e11d48,#be185d)"><i class="fa-solid fa-file-arrow-up"></i></div><div><div style="font-weight:600;font-size:.85rem">Upload Materi</div><div style="font-size:.75rem;color:var(--ais2-muted)">Bahan ajar &amp; tugas</div></div></a>
    <a class="pf-lc" href="<%=ctx%>/main?modul=SKP"><div class="pf-li" style="background:linear-gradient(135deg,#475569,#334155)"><i class="fa-solid fa-trophy"></i></div><div><div style="font-weight:600;font-size:.85rem">SKP &amp; Kinerja</div><div style="font-size:.75rem;color:var(--ais2-muted)">Kinerja &amp; beban kerja</div></div></a>
  </div>
</div>

<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('profil','dosen_mengajar',{}).then(function(r){
    if(!r||!r.data) return; var d=r.data;
    if(d.nidn) document.getElementById('nidn').textContent=d.nidn;
    if(d.jumlahKelas) document.getElementById('sKelas').textContent=d.jumlahKelas;
    if(d.jumlahMahasiswa) document.getElementById('sMahasiswa').textContent=d.jumlahMahasiswa;
    if(d.bimbinganTA) document.getElementById('sBimbingan').textContent=d.bimbinganTA;
  });
});
</script>
</body></html>
