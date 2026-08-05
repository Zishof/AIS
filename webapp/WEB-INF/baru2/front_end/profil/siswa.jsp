<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common,ais.database.model.Tbmuser" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT;
   Object _loSiswa = session.getAttribute("login");
   Tbmuser loginUser = (_loSiswa instanceof Tbmuser) ? (Tbmuser)_loSiswa :
       (session.getAttribute("usersTemp") instanceof Tbmuser ? (Tbmuser)session.getAttribute("usersTemp") : null);
   Object siswa = session.getAttribute("siswa");
   String namaSiswa = loginUser != null ? loginUser.getUserNama() : "Siswa";
   String nisnSiswa = "—"; String kelasSiswa = "—";
   try {
     if (siswa != null) {
       namaSiswa = siswa.getClass().getMethod("getNama").invoke(siswa).toString();
       try { nisnSiswa = siswa.getClass().getMethod("getNim").invoke(siswa).toString(); } catch(Exception ig){}
       try {
         Object kls = siswa.getClass().getMethod("getKelas").invoke(siswa);
         if (kls != null) kelasSiswa = kls.getClass().getMethod("getNamaKelas").invoke(kls).toString();
       } catch(Exception ig){}
     }
   } catch(Exception ign){}
%>
<!doctype html><html lang="id"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Profil Siswa</title>
<link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %><%@ include file="../_komponen.jsp" %><%@ include file="../_lang.jsp" %>
<style>
.pf-hero{background:linear-gradient(135deg,#ea580c,#c2410c);border-radius:18px;padding:28px;display:flex;gap:20px;align-items:center;margin-bottom:20px;color:#fff}
.pf-foto{width:90px;height:90px;border-radius:50%;border:3px solid rgba(255,255,255,.5);object-fit:cover;flex-shrink:0;background:#fff3}
.pf-nama{font-size:1.3rem;font-weight:700;margin:0}
.pf-sub{font-size:.85rem;opacity:.85;margin:2px 0}
.pf-badge{display:inline-block;background:rgba(255,255,255,.2);border-radius:8px;padding:3px 10px;font-size:.78rem;margin-top:6px}
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
@media(max-width:600px){.pf-hero{flex-direction:column;text-align:center}}
</style>
<script>var ROOT='<%=ctx%>';</script>
</head>
<body class="ais2-page" style="padding:16px;background:var(--ais2-bg);min-height:100vh">

<div class="pf-hero">
  <img class="pf-foto" src="<%=ctx%>/img/avatar_default.png" alt="foto" onerror="this.src='<%=ctx%>/img/avatar_default.png'">
  <div>
    <p class="pf-nama"><%=namaSiswa%></p>
    <p class="pf-sub"><i class="fa-solid fa-hashtag me-1"></i>NISN/NIM: <%=nisnSiswa%></p>
    <p class="pf-sub"><i class="fa-solid fa-school me-1"></i>Kelas: <%=kelasSiswa%></p>
    <p class="pf-sub"><i class="fa-solid fa-envelope me-1"></i><%=loginUser!=null?loginUser.getEmail():""%></p>
    <span class="pf-badge"><i class="fa-solid fa-user-graduate me-1"></i>Siswa</span>
  </div>
</div>

<div class="pf-card">
  <h5><i class="fa-solid fa-address-card me-2" style="color:#ea580c"></i>Biodata</h5>
  <%
  String[][] biodataFields = new String[][]{};
  if (siswa != null) {
    java.util.List<String[]> rows = new java.util.ArrayList<String[]>();
    String[][] fields = {
      {"getNama","Nama Lengkap"}, {"getNim","NISN/NIM"}, {"getPanggilan","Nama Panggilan"},
      {"getTanggalLahir","Tanggal Lahir"}, {"getJenisKelamin","Jenis Kelamin"},
      {"getAgama","Agama"}, {"getAlamat","Alamat"}, {"getTelepon","Telepon"},
      {"getEmail","Email"}
    };
    for (String[] f : fields) {
      try {
        Object val = siswa.getClass().getMethod(f[0]).invoke(siswa);
        if (val != null && !val.toString().isEmpty()) rows.add(new String[]{f[1], val.toString()});
      } catch(Exception ign2){}
    }
    for (String[] row : rows) {
  %>
  <div class="pf-row"><span class="pf-lbl"><%=row[0]%></span><span class="pf-val"><%=org.apache.commons.lang.StringEscapeUtils.escapeHtml(row[1])%></span></div>
  <% } } %>
</div>

<div class="pf-card">
  <h5><i class="fa-solid fa-grid-2 me-2" style="color:#ea580c"></i>Menu Cepat</h5>
  <div class="pf-links">
    <a class="pf-lc" href="<%=ctx%>/main?modul=JadwalPelajaran"><div class="pf-li" style="background:linear-gradient(135deg,#ea580c,#c2410c)"><i class="fa-solid fa-clock"></i></div><div><div style="font-weight:600;font-size:.85rem">Jadwal Pelajaran</div></div></a>
    <a class="pf-lc" href="<%=ctx%>/main?modul=NilaiSiswa"><div class="pf-li" style="background:linear-gradient(135deg,#10b981,#059669)"><i class="fa-solid fa-star"></i></div><div><div style="font-weight:600;font-size:.85rem">Nilai Raport</div></div></a>
    <a class="pf-lc" href="<%=ctx%>/main?modul=AbsensiSiswa"><div class="pf-li" style="background:linear-gradient(135deg,#0ea5e9,#2563eb)"><i class="fa-solid fa-fingerprint"></i></div><div><div style="font-weight:600;font-size:.85rem">Absensi</div></div></a>
    <a class="pf-lc" href="<%=ctx%>/main?modul=PembayaranSiswa"><div class="pf-li" style="background:linear-gradient(135deg,#f59e0b,#d97706)"><i class="fa-solid fa-wallet"></i></div><div><div style="font-weight:600;font-size:.85rem">Pembayaran</div></div></a>
  </div>
</div>
</body></html>
