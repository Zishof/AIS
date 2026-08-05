<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common,ais.database.model.Tbmuser" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT;
   Object _loApt = session.getAttribute("login");
   Tbmuser loginUser = (_loApt instanceof Tbmuser) ? (Tbmuser)_loApt :
       (session.getAttribute("usersTemp") instanceof Tbmuser ? (Tbmuser)session.getAttribute("usersTemp") : null);
   String nama = loginUser != null ? loginUser.getUserNama() : "Administrator";
   String email = loginUser != null ? loginUser.getEmail() : "";
   String hp    = loginUser != null ? loginUser.getHp() : "";
   String unitKerja = "";
   try {
     Object prodi = loginUser.getClass().getMethod("ambilProgram").invoke(loginUser);
     if (prodi != null) unitKerja = prodi.getClass().getMethod("getNama").invoke(prodi).toString();
   } catch(Exception ign) {
     try {
       Object jur = loginUser.getClass().getMethod("ambilJurusan").invoke(loginUser);
       if (jur != null) unitKerja = jur.getClass().getMethod("getNama").invoke(jur).toString();
     } catch(Exception ig2) {}
   }
%>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Profil Admin PT</title>
<link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<style>
.pf-hero{background:linear-gradient(135deg,#1e3a8a,#1e40af);border-radius:18px;padding:28px;display:flex;gap:20px;align-items:center;margin-bottom:20px;color:#fff}
.pf-foto{width:90px;height:90px;border-radius:50%;border:3px solid rgba(255,255,255,.5);object-fit:cover;flex-shrink:0;background:#fff3}
.pf-nama{font-size:1.3rem;font-weight:700;margin:0}
.pf-sub{font-size:.85rem;opacity:.85;margin:2px 0}
.pf-badge{display:inline-block;background:rgba(255,255,255,.2);border-radius:8px;padding:3px 10px;font-size:.78rem;margin-top:6px}
.pf-kpi{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:14px;margin-bottom:20px}
.pf-kpi-card{background:var(--ais2-card);border-radius:14px;padding:18px 14px;text-align:center;border:1px solid var(--ais2-border);cursor:pointer;transition:.2s}
.pf-kpi-card:hover{border-color:var(--ais2-primary);box-shadow:0 4px 16px var(--ais2-shadow)}
.pf-kv{font-size:1.8rem;font-weight:800;color:var(--ais2-primary)}
.pf-kl{font-size:.78rem;color:var(--ais2-muted);margin-top:4px}
.pf-card{background:var(--ais2-card);border-radius:14px;border:1px solid var(--ais2-border);padding:18px;margin-bottom:16px}
.pf-card h5{font-weight:700;margin:0 0 14px;font-size:.95rem;color:var(--ais2-text)}
.pf-links{display:grid;grid-template-columns:repeat(auto-fill,minmax(170px,1fr));gap:12px}
.pf-lc{display:flex;align-items:center;gap:12px;background:var(--ais2-card);border:1px solid var(--ais2-border);border-radius:12px;padding:14px;text-decoration:none;color:var(--ais2-text);transition:.2s}
.pf-lc:hover{border-color:var(--ais2-primary);transform:translateY(-2px);box-shadow:0 4px 16px var(--ais2-shadow)}
.pf-li{width:38px;height:38px;border-radius:10px;display:flex;align-items:center;justify-content:center;color:#fff;font-size:1rem;flex-shrink:0}
@media(max-width:600px){.pf-hero{flex-direction:column;text-align:center}.pf-kpi{grid-template-columns:repeat(2,1fr)}}
</style>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">

<div class="pf-hero">
  <img class="pf-foto" src="<%=ctx%>/img/avatar_default.png" alt="foto" onerror="this.src='<%=ctx%>/img/avatar_default.png'">
  <div>
    <p class="pf-nama"><%=nama%></p>
    <% if (!unitKerja.isEmpty()) { %><p class="pf-sub"><i class="fa-solid fa-sitemap me-1"></i><%=unitKerja%></p><% } %>
    <p class="pf-sub"><i class="fa-solid fa-envelope me-1"></i><%=email%></p>
    <p class="pf-sub"><i class="fa-solid fa-phone me-1"></i><%=hp%></p>
    <span class="pf-badge"><i class="fa-solid fa-shield-halved me-1"></i>Administrator Perguruan Tinggi</span>
  </div>
</div>

<!-- Angka Penting PT -->
<h5 style="color:var(--ais2-text);font-weight:700;margin-bottom:12px">Angka Penting Institusi</h5>
<div class="pf-kpi">
  <div class="pf-kpi-card" onclick="location.href='<%=ctx%>/main?modul=DataMahasiswa'">
    <div class="pf-kv" id="kMhsAktif">—</div><div class="pf-kl">Mahasiswa Aktif</div>
  </div>
  <div class="pf-kpi-card" onclick="location.href='<%=ctx%>/main?modul=DataMahasiswaLulus'">
    <div class="pf-kv" id="kMhsLulus">—</div><div class="pf-kl">Mahasiswa Lulus</div>
  </div>
  <div class="pf-kpi-card" onclick="location.href='<%=ctx%>/main?modul=DataDosen'">
    <div class="pf-kv" id="kDosenAktif">—</div><div class="pf-kl">Dosen Aktif</div>
  </div>
  <div class="pf-kpi-card" onclick="location.href='<%=ctx%>/main?modul=DataProdi'">
    <div class="pf-kv" id="kProdiAktif">—</div><div class="pf-kl">Program Studi</div>
  </div>
  <div class="pf-kpi-card" onclick="location.href='<%=ctx%>/main?modul=DataFakultas'">
    <div class="pf-kv" id="kFakultasAktif">—</div><div class="pf-kl">Fakultas</div>
  </div>
</div>

<!-- Akses Cepat -->
<div class="pf-card">
  <h5><i class="fa-solid fa-grid-2 me-2" style="color:#1e40af"></i>Akses Cepat</h5>
  <div class="pf-links">
    <a class="pf-lc" href="<%=ctx%>/baru2?modul=akademik"><div class="pf-li" style="background:linear-gradient(135deg,#1e40af,#4f46e5)"><i class="fa-solid fa-graduation-cap"></i></div><div><div style="font-weight:600;font-size:.85rem">Akademik</div><div style="font-size:.75rem;color:var(--ais2-muted)">KRS, nilai, jadwal</div></div></a>
    <a class="pf-lc" href="<%=ctx%>/baru2?modul=pembayaran"><div class="pf-li" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-money-bill-wave"></i></div><div><div style="font-weight:600;font-size:.85rem">Pembayaran</div><div style="font-size:.75rem;color:var(--ais2-muted)">Tagihan &amp; VA</div></div></a>
    <a class="pf-lc" href="<%=ctx%>/baru2?modul=kepegawaian"><div class="pf-li" style="background:linear-gradient(135deg,#7c3aed,#6d28d9)"><i class="fa-solid fa-users"></i></div><div><div style="font-weight:600;font-size:.85rem">Kepegawaian</div><div style="font-size:.75rem;color:var(--ais2-muted)">Dosen &amp; staf</div></div></a>
    <a class="pf-lc" href="<%=ctx%>/baru2?modul=feeder"><div class="pf-li" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-cloud-arrow-up"></i></div><div><div style="font-weight:600;font-size:.85rem">Feeder PDDikti</div><div style="font-size:.75rem;color:var(--ais2-muted)">Sinkronisasi data</div></div></a>
    <a class="pf-lc" href="<%=ctx%>/baru2?modul=keuangan"><div class="pf-li" style="background:linear-gradient(135deg,#16a34a,#15803d)"><i class="fa-solid fa-chart-line"></i></div><div><div style="font-weight:600;font-size:.85rem">Keuangan</div><div style="font-size:.75rem;color:var(--ais2-muted)">RAB &amp; laporan</div></div></a>
    <a class="pf-lc" href="<%=ctx%>/baru2?modul=spmi"><div class="pf-li" style="background:linear-gradient(135deg,#065f46,#047857)"><i class="fa-solid fa-clipboard-check"></i></div><div><div style="font-weight:600;font-size:.85rem">SPMI</div><div style="font-size:.75rem;color:var(--ais2-muted)">Penjaminan mutu</div></div></a>
  </div>
</div>

<script>
document.addEventListener('DOMContentLoaded',function(){
  AIS2.Api.get('profil','admin_pt_stat',{}).then(function(r){
    if(!r||!r.data) return; var d=r.data;
    ['kMhsAktif:mahasiswaAktif','kMhsLulus:mahasiswaLulus','kDosenAktif:dosenAktif','kProdiAktif:prodiAktif','kFakultasAktif:fakultasAktif'].forEach(function(pair){
      var parts=pair.split(':');
      var el=document.getElementById(parts[0]);
      if(el && d[parts[1]]!==undefined) el.textContent=d[parts[1]];
    });
  });
});
</script>
</body></html>
