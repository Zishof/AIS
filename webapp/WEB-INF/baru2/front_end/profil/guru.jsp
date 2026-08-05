<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common,ais.database.model.Tbmuser" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT;
   Object _loGuru = session.getAttribute("login");
   Tbmuser loginUser = (_loGuru instanceof Tbmuser) ? (Tbmuser)_loGuru :
       (session.getAttribute("usersTemp") instanceof Tbmuser ? (Tbmuser)session.getAttribute("usersTemp") : null);
   Object guru = session.getAttribute("guru");
   String namaGuru = loginUser != null ? loginUser.getUserNama() : "Guru";
   String nipGuru = "—"; String statusGuru = "—";
   try {
     if (guru != null) {
       namaGuru = guru.getClass().getMethod("getNama").invoke(guru).toString();
       try { nipGuru = guru.getClass().getMethod("getNim").invoke(guru).toString(); } catch(Exception ig){}
       try {
         Object st = guru.getClass().getMethod("getStatusKepegawaian").invoke(guru);
         if (st != null) statusGuru = st.toString();
       } catch(Exception ig){}
     }
   } catch(Exception ign){}
%>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Profil Guru</title>
<link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<style>
.pf-hero{background:linear-gradient(135deg,#16a34a,#15803d);border-radius:18px;padding:28px;display:flex;gap:20px;align-items:center;margin-bottom:20px;color:#fff}
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
.pf-row{display:flex;gap:8px;padding:7px 0;border-bottom:1px solid var(--ais2-border);font-size:.85rem}
.pf-row:last-child{border-bottom:none}
.pf-lbl{color:var(--ais2-muted);min-width:150px;flex-shrink:0}
.pf-val{color:var(--ais2-text);font-weight:500}
.pf-links{display:grid;grid-template-columns:repeat(auto-fill,minmax(160px,1fr));gap:12px}
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
    <p class="pf-nama"><%=namaGuru%></p>
    <p class="pf-sub"><i class="fa-solid fa-id-badge me-1"></i>NIP/Kode: <%=nipGuru%></p>
    <p class="pf-sub"><i class="fa-solid fa-briefcase me-1"></i><%=statusGuru%></p>
    <p class="pf-sub"><i class="fa-solid fa-envelope me-1"></i><%=loginUser!=null?loginUser.getEmail():""%></p>
    <span class="pf-badge"><i class="fa-solid fa-person-chalkboard me-1"></i>Guru / Pengajar</span>
  </div>
</div>

<div class="pf-stats" id="statsGuru">
  <div class="pf-stat"><div class="pf-sv" id="sJadwal">—</div><div class="pf-sl">Jadwal Hari Ini</div></div>
  <div class="pf-stat"><div class="pf-sv" id="sMurid">—</div><div class="pf-sl">Siswa Diajar</div></div>
  <div class="pf-stat"><div class="pf-sv" id="sMatkul">—</div><div class="pf-sl">Mata Pelajaran</div></div>
</div>

<div class="pf-card">
  <h5><i class="fa-solid fa-address-card me-2" style="color:#16a34a"></i>Data Kepegawaian</h5>
  <%
  if (guru != null) {
    String[][] kepegFields = {
      {"getNim","NIP/Kode"}, {"getStatusKepegawaian","Status Kepegawaian"},
      {"getJenisGuru","Jenis PTK"}, {"getGolonganPegawai","Golongan"},
      {"getTanggalLahir","Tanggal Lahir"}, {"getAgama","Agama"},
      {"getTelepon","Telepon"}, {"getAlamat","Alamat"}
    };
    for (String[] f : kepegFields) {
      try {
        Object val = guru.getClass().getMethod(f[0]).invoke(guru);
        if (val != null && !val.toString().isEmpty()) {
  %>
  <div class="pf-row"><span class="pf-lbl"><%=f[1]%></span><span class="pf-val"><%=org.apache.commons.lang.StringEscapeUtils.escapeHtml(val.toString())%></span></div>
  <% }
      } catch(Exception ign2){}
    }
  } %>
</div>

<div class="pf-card">
  <h5><i class="fa-solid fa-grid-2 me-2" style="color:#16a34a"></i>Menu Cepat</h5>
  <div class="pf-links">
    <a class="pf-lc" href="<%=ctx%>/main?modul=JadwalMengajar"><div class="pf-li" style="background:linear-gradient(135deg,#16a34a,#15803d)"><i class="fa-solid fa-clock"></i></div><div><div style="font-weight:600;font-size:.85rem">Jadwal Mengajar</div></div></a>
    <a class="pf-lc" href="<%=ctx%>/main?modul=AbsensiSiswaGuru"><div class="pf-li" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-clipboard-check"></i></div><div><div style="font-weight:600;font-size:.85rem">Absensi Siswa</div></div></a>
    <a class="pf-lc" href="<%=ctx%>/main?modul=NilaiSiswaGuru"><div class="pf-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-star"></i></div><div><div style="font-weight:600;font-size:.85rem">Input Nilai</div></div></a>
    <a class="pf-lc" href="<%=ctx%>/main?modul=MateriGuru"><div class="pf-li" style="background:linear-gradient(135deg,#7c3aed,#6d28d9)"><i class="fa-solid fa-file-arrow-up"></i></div><div><div style="font-weight:600;font-size:.85rem">Upload Materi</div></div></a>
  </div>
</div>
</body></html>
