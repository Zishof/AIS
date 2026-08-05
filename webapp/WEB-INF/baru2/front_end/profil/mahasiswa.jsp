<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common,ais.database.model.Tbmuser" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT;
   Object _loMhs = session.getAttribute("login");
   Tbmuser loginUser = (_loMhs instanceof Tbmuser) ? (Tbmuser)_loMhs :
       (session.getAttribute("usersTemp") instanceof Tbmuser ? (Tbmuser)session.getAttribute("usersTemp") : null);
   Object mhs = session.getAttribute("mahasiswa");
   String namaMhs = loginUser != null ? loginUser.getUserNama() : "Mahasiswa";
   try { if (mhs != null) namaMhs = mhs.getClass().getMethod("getNama").invoke(mhs).toString(); } catch(Exception ign){}
%>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Profil Mahasiswa</title>
<link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<style>
.pf-hero{background:linear-gradient(135deg,#1e40af,#4f46e5);border-radius:18px;padding:28px;display:flex;gap:20px;align-items:center;margin-bottom:20px;color:#fff}
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
.pf-row{display:flex;gap:8px;padding:6px 0;border-bottom:1px solid var(--ais2-border);font-size:.85rem}
.pf-row:last-child{border-bottom:none}
.pf-lbl{color:var(--ais2-muted);min-width:130px;flex-shrink:0}
.pf-val{color:var(--ais2-text);font-weight:500}
.pf-links{display:grid;grid-template-columns:repeat(auto-fill,minmax(170px,1fr));gap:12px}
.pf-lc{display:flex;align-items:center;gap:12px;background:var(--ais2-card);border:1px solid var(--ais2-border);border-radius:12px;padding:14px;text-decoration:none;color:var(--ais2-text);transition:.2s}
.pf-lc:hover{border-color:var(--ais2-primary);transform:translateY(-2px);box-shadow:0 4px 16px var(--ais2-shadow)}
.pf-li{width:38px;height:38px;border-radius:10px;display:flex;align-items:center;justify-content:center;color:#fff;font-size:1rem;flex-shrink:0}
@media(max-width:600px){.pf-hero{flex-direction:column;text-align:center}.pf-stats{grid-template-columns:repeat(2,1fr)}}
</style>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">

<!-- Hero card -->
<div class="pf-hero">
  <img class="pf-foto" id="fotoMhs" src="<%=ctx%>/img/avatar_default.png" alt="foto" onerror="this.src='<%=ctx%>/img/avatar_default.png'">
  <div>
    <p class="pf-nama" id="namaMhs"><%=namaMhs%></p>
    <p class="pf-sub"><i class="fa-solid fa-id-card me-1"></i><span id="nim">—</span></p>
    <p class="pf-sub"><i class="fa-solid fa-envelope me-1"></i><span id="emailMhs"><%=loginUser!=null?loginUser.getEmail():""%></span></p>
    <p class="pf-sub"><i class="fa-solid fa-phone me-1"></i><span id="hpMhs">—</span></p>
    <span class="pf-badge"><i class="fa-solid fa-graduation-cap me-1"></i>Mahasiswa</span>
  </div>
</div>

<!-- Stats akademik -->
<div class="pf-stats">
  <div class="pf-stat"><div class="pf-sv" id="sSemester">—</div><div class="pf-sl">Semester</div></div>
  <div class="pf-stat"><div class="pf-sv" id="sIPK">—</div><div class="pf-sl">IPK</div></div>
  <div class="pf-stat"><div class="pf-sv" id="sSKS">—</div><div class="pf-sl">Total SKS</div></div>
  <div class="pf-stat"><div class="pf-sv" id="sTahunAkademik">—</div><div class="pf-sl">Tahun Akademik</div></div>
</div>

<!-- Info semester -->
<div class="pf-card" id="cardKRS" style="display:none">
  <h5><i class="fa-solid fa-notes me-2" style="color:#4f46e5"></i>Info KRS Semester Ini</h5>
  <div class="pf-row"><span class="pf-lbl">Tahun Akademik</span><span class="pf-val" id="infoTA">—</span></div>
  <div class="pf-row"><span class="pf-lbl">Semester</span><span class="pf-val" id="infoSmt">—</span></div>
  <div class="pf-row"><span class="pf-lbl">IPK</span><span class="pf-val" id="infoIPK">—</span></div>
  <div class="pf-row"><span class="pf-lbl">SKS Diambil</span><span class="pf-val" id="infoSKS">—</span></div>
  <div class="pf-row"><span class="pf-lbl">Catatan Dosen PA</span><span class="pf-val" id="infoCatatan">—</span></div>
</div>

<!-- Menu cepat -->
<div class="pf-card">
  <h5><i class="fa-solid fa-grid-2 me-2" style="color:#1e40af"></i>Menu Cepat</h5>
  <div class="pf-links">
    <a class="pf-lc" href="<%=ctx%>/main?modul=KrsMahasiswa"><div class="pf-li" style="background:linear-gradient(135deg,#4f46e5,#7c3aed)"><i class="fa-solid fa-list-check"></i></div><div><div style="font-weight:600;font-size:.85rem">KRS</div><div style="font-size:.75rem;color:var(--ais2-muted)">Kartu Rencana Studi</div></div></a>
    <a class="pf-lc" href="<%=ctx%>/main?modul=PerkuliahanMahasiswa"><div class="pf-li" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-chalkboard-user"></i></div><div><div style="font-weight:600;font-size:.85rem">Perkuliahan</div><div style="font-size:.75rem;color:var(--ais2-muted)">Jadwal &amp; materi</div></div></a>
    <a class="pf-lc" href="<%=ctx%>/main?modul=NilaiMahasiswa"><div class="pf-li" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-star-half-stroke"></i></div><div><div style="font-weight:600;font-size:.85rem">Nilai</div><div style="font-size:.75rem;color:var(--ais2-muted)">Transkrip &amp; KHS</div></div></a>
    <a class="pf-lc" href="<%=ctx%>/main?modul=PembayaranMahasiswa"><div class="pf-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-wallet"></i></div><div><div style="font-weight:600;font-size:.85rem">Pembayaran</div><div style="font-size:.75rem;color:var(--ais2-muted)">Tagihan &amp; riwayat</div></div></a>
    <a class="pf-lc" href="<%=ctx%>/main?modul=TugasMahasiswa"><div class="pf-li" style="background:linear-gradient(135deg,#e11d48,#be185d)"><i class="fa-solid fa-pen-ruler"></i></div><div><div style="font-weight:600;font-size:.85rem">Tugas</div><div style="font-size:.75rem;color:var(--ais2-muted)">Upload &amp; status</div></div></a>
    <a class="pf-lc" href="<%=ctx%>/main?modul=UjianMahasiswa"><div class="pf-li" style="background:linear-gradient(135deg,#475569,#334155)"><i class="fa-solid fa-pencil"></i></div><div><div style="font-weight:600;font-size:.85rem">Ujian Online</div><div style="font-size:.75rem;color:var(--ais2-muted)">Ujian &amp; hasil</div></div></a>
  </div>
</div>

<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('profil','mhs_akademik',{}).then(function(r){
    if (!r||!r.data) return; var d=r.data;
    if(d.nim){document.getElementById('nim').textContent=d.nim;}
    if(d.hp){document.getElementById('hpMhs').textContent=d.hp;}
    if(d.semester){document.getElementById('sSemester').textContent='Smt '+d.semester;document.getElementById('infoSmt').textContent=d.semester;}
    if(d.ipk){document.getElementById('sIPK').textContent=d.ipk;document.getElementById('infoIPK').textContent=d.ipk;}
    if(d.sks){document.getElementById('sSKS').textContent=d.sks;document.getElementById('infoSKS').textContent=d.sks;}
    if(d.tahunAkademik){document.getElementById('sTahunAkademik').textContent=d.tahunAkademik;document.getElementById('infoTA').textContent=d.tahunAkademik;}
    if(d.catatan){document.getElementById('infoCatatan').textContent=d.catatan;}
    if(d.semester||d.ipk){document.getElementById('cardKRS').style.display='';}
  });
});
</script>
</body></html>
