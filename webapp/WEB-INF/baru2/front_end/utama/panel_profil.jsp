<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<%@ page import="ais.database.model.Tbmuser" %>
<%@ page import="ais.database.model.Tbmrole" %>
<%@ page import="ais.database.hibernate.HibernateUtil" %>
<%@ page import="org.hibernate.Session" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Calendar" %>
<%!
private static String _sh(String v){
    if(v==null||v.trim().isEmpty())return "";
    return v.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
}
private static String _ini(String s){
    if(s==null||s.trim().isEmpty())return "US";
    String[] p=s.trim().split("\\s+");
    return p.length>=2?(""+p[0].charAt(0)+p[1].charAt(0)).toUpperCase():(""+p[0].charAt(0)).toUpperCase();
}
private static String _sapaan(){
    int h=Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    if(h>=4&&h<11)return "Pagi";
    if(h>=11&&h<15)return "Siang";
    if(h>=15&&h<19)return "Sore";
    return "Malam";
}
%>
<%
/* ============================================================
   panel_profil.jsp — Panel profil pengguna sisi kanan Home.
   Di-include oleh front_end/utama/index.jsp via <jsp:include>.
   Menampilkan kartu profil sesuai jenis pengguna:
     mhs/mhss2 → Mahasiswa
     Dosen     → Dosen
     Guru      → Guru
     siswa     → Siswa
     lain-lain → Admin PT atau Admin Sekolah
   ============================================================ */

String _root = Common.ROOT==null?"":Common.ROOT;
String _roleId = "";
String _userId = "";
String _namaPengguna = "Pengguna";

// --- Deteksi tipe dari sesi (tidak perlu Hibernate) ---
try {
    Tbmuser _u = Common.getCurrentUser(request);
    if(_u!=null){
        _userId = _u.getUserId()==null?"":_u.getUserId();
        _namaPengguna = _userId;
        try {
            if(_u.getPegawai()!=null&&_u.getPegawai().getNama()!=null) _namaPengguna=_u.getPegawai().getNama();
        } catch(Exception _ex){}
        try {
            Tbmrole _r=_u.hakAkses();
            if(_r!=null&&_r.getRoleId()!=null) _roleId=_r.getRoleId();
        } catch(Exception _ex){}
    }
} catch(Exception _ex){}

boolean _isMhs   = "mhs".equals(_roleId)||"mhss2".equals(_roleId);
boolean _isSiswa = "siswa".equals(_roleId);
boolean _isDosen = "Dosen".equals(_roleId);
boolean _isGuru  = "Guru".equals(_roleId);
boolean _isAdmin = !_isMhs && !_isSiswa && !_isDosen && !_isGuru;

boolean _sekolahMode = false;
try { boolean[] _c=Common.chekPtAtauSekolah(); _sekolahMode=_c!=null&&_c.length>1&&_c[1]; } catch(Exception _ex){}

// --- Data kartu profil ---
String profNama     = _namaPengguna;
String profIdLabel  = "ID";
String profIdVal    = _userId;
String profUnit     = "";
String profBadge    = _roleId.isEmpty()?"Admin":_roleId;
String profBadgeClr = "#6d28d9"; // default purple
String profHeaderClr= "linear-gradient(135deg,#1e3a8a 0%,#4f46e5 60%,#6d28d9 100%)";

String profStat1Lbl="—", profStat2Lbl="—", profStat3Lbl="—";
String profStat1Val="—", profStat2Val="—", profStat3Val="—";
String profStat1Ico="fa-circle-info", profStat2Ico="fa-circle-info", profStat3Ico="fa-circle-check";
String profStat1Clr="#4f46e5", profStat2Clr="#10b981", profStat3Clr="#10b981";

// [[icon, label, url, color]]
String[][] profLinks;

String _sapaan = _sapaan();

Session _db = null;
try {
    _db = HibernateUtil.openSession();

    if(_isMhs) {
        /* --- MAHASISWA --- */
        profBadge    = "Mahasiswa";
        profBadgeClr = "#3730a3";
        profHeaderClr= "linear-gradient(135deg,#1e40af 0%,#4338ca 60%,#3730a3 100%)";
        profIdLabel  = "NIM";
        profIdVal    = _userId;

        List _mhsList = _db.createQuery("from Mahasiswa where nim=:n").setString("n",_userId).setMaxResults(1).list();
        if(!_mhsList.isEmpty()){
            Object _mhs=_mhsList.get(0);
            Class _mc=_mhs.getClass();
            try{ String _n=(String)_mc.getMethod("getNama").invoke(_mhs); if(_n!=null&&!_n.trim().isEmpty())profNama=_n; } catch(Exception _ex){}
            try{ String _nim=(String)_mc.getMethod("getNim").invoke(_mhs); if(_nim!=null)profIdVal=_nim; } catch(Exception _ex){}
            try{
                Object _jur=_mc.getMethod("getJurusan").invoke(_mhs);
                if(_jur!=null){ String _jn=(String)_jur.getClass().getMethod("getNama").invoke(_jur); if(_jn!=null)profUnit=_jn; }
            } catch(Exception _ex){}
        }

        // Stats mahasiswa
        profStat1Lbl="Program Studi"; profStat1Val=profUnit.isEmpty()?"—":profUnit; profStat1Ico="fa-graduation-cap"; profStat1Clr="#4f46e5";
        profStat2Lbl="Status"; profStat2Val="Aktif"; profStat2Ico="fa-circle-check"; profStat2Clr="#10b981";
        profStat3Lbl="Jenjang"; profStat3Val="mhss2".equals(_roleId)?"S2":"S1"; profStat3Ico="fa-layer-group"; profStat3Clr="#0ea5e9";

        profLinks = new String[][]{
            {"fa-list-check","KRS",      _root+"/baru?p=akademik&s=krs",  "#4f46e5"},
            {"fa-star",      "Nilai",    _root+"/baru?p=akademik&s=nilai","#10b981"},
            {"fa-fingerprint","Kehadiran",_root+"/baru?p=presensi",       "#0ea5e9"},
            {"fa-credit-card","Tagihan", _root+"/baru?p=pembayaran",      "#f59e0b"}
        };

    } else if(_isDosen) {
        /* --- DOSEN --- */
        profBadge    = "Dosen";
        profBadgeClr = "#1d4ed8";
        profHeaderClr= "linear-gradient(135deg,#0f172a 0%,#1e40af 55%,#1d4ed8 100%)";
        profIdLabel  = "NIDN";

        // Load Dosen via fresh Tbmuser (Dosen is @ManyToOne → lazy loads fine with open session)
        try {
            Tbmuser _fu=(Tbmuser)_db.get(Tbmuser.class,Common.getCurrentUser(request).getId());
            if(_fu!=null&&_fu.getDosen()!=null){
                Object _dosen=_fu.getDosen();
                Class _dc=_dosen.getClass();
                try{ String _n=(String)_dc.getMethod("getNama").invoke(_dosen); if(_n!=null&&!_n.trim().isEmpty())profNama=_n; } catch(Exception _ex){}
                try{ String _nidn=(String)_dc.getMethod("getNidn").invoke(_dosen); if(_nidn!=null)profIdVal=_nidn; } catch(Exception _ex){}
                try{
                    Object _jur=_dc.getMethod("getJurusan").invoke(_dosen);
                    if(_jur!=null){ String _jn=(String)_jur.getClass().getMethod("getNama").invoke(_jur); if(_jn!=null)profUnit=_jn; }
                } catch(Exception _ex){
                    try{
                        Object _fak=_dc.getMethod("getFakultas").invoke(_dosen);
                        if(_fak!=null){ String _fn=(String)_fak.getClass().getMethod("getNama").invoke(_fak); if(_fn!=null)profUnit=_fn; }
                    } catch(Exception _ex2){}
                }
                // Stats: jurusan, status
                profStat1Lbl="Prodi/Jurusan"; profStat1Val=profUnit.isEmpty()?"—":profUnit; profStat1Ico="fa-sitemap"; profStat1Clr="#1d4ed8";
                profStat2Lbl="Jabatan";
                try{ String _jbt=(String)_dc.getMethod("getJabatan").invoke(_dosen); profStat2Val=_jbt!=null&&!_jbt.isEmpty()?_jbt:"Dosen"; } catch(Exception _ex){ profStat2Val="Dosen"; }
                profStat2Ico="fa-user-tie"; profStat2Clr="#0ea5e9";
            }
        } catch(Exception _ex){}

        profStat3Lbl="Status"; profStat3Val="Aktif"; profStat3Ico="fa-circle-check"; profStat3Clr="#10b981";
        profLinks = new String[][]{
            {"fa-chalkboard",    "Perkuliahan",  _root+"/baru?p=akademik",          "#4f46e5"},
            {"fa-user-graduate", "Bimbingan",    _root+"/baru?p=akademik&s=bimbing","#0ea5e9"},
            {"fa-square-poll-vertical","Nilai",  _root+"/baru?p=akademik&s=nilai",  "#10b981"},
            {"fa-fingerprint",   "Kehadiran",    _root+"/baru?p=presensi",          "#7c3aed"}
        };

    } else if(_isGuru) {
        /* --- GURU --- */
        profBadge    = "Guru";
        profBadgeClr = "#065f46";
        profHeaderClr= "linear-gradient(135deg,#064e3b 0%,#065f46 55%,#059669 100%)";
        profIdLabel  = "NIP";
        try {
            Tbmuser _fu=(Tbmuser)_db.get(Tbmuser.class,Common.getCurrentUser(request).getId());
            if(_fu!=null&&_fu.getGuru()!=null){
                Object _guru=_fu.getGuru();
                Class _gc=_guru.getClass();
                try{ String _n=(String)_gc.getMethod("getNama").invoke(_guru); if(_n!=null&&!_n.trim().isEmpty())profNama=_n; } catch(Exception _ex){}
                try{ String _nip=(String)_gc.getMethod("getNip").invoke(_guru); if(_nip!=null&&!_nip.isEmpty())profIdVal=_nip; } catch(Exception _ex){}
            }
        } catch(Exception _ex){}
        profStat1Lbl="Kelas"; profStat1Ico="fa-chalkboard"; profStat1Clr="#059669";
        profStat2Lbl="Mata Pelajaran"; profStat2Ico="fa-book-open"; profStat2Clr="#0891b2";
        profStat3Lbl="Status"; profStat3Val="Aktif"; profStat3Ico="fa-circle-check"; profStat3Clr="#10b981";
        profLinks = new String[][]{
            {"fa-chalkboard",          "Jadwal",     _root+"/baru?p=akademik",         "#059669"},
            {"fa-users",               "Absensi",    _root+"/baru?p=presensi",          "#0ea5e9"},
            {"fa-square-poll-vertical","Nilai Siswa",_root+"/baru?p=akademik&s=nilai",  "#4f46e5"}
        };

    } else if(_isSiswa&&_sekolahMode) {
        /* --- SISWA --- */
        profBadge    = "Siswa";
        profBadgeClr = "#7c3aed";
        profHeaderClr= "linear-gradient(135deg,#4c1d95 0%,#6d28d9 55%,#7c3aed 100%)";
        profIdLabel  = "NIS";
        profStat1Lbl="Kelas"; profStat1Ico="fa-door-open"; profStat1Clr="#7c3aed";
        profStat2Lbl="Status"; profStat2Val="Aktif"; profStat2Ico="fa-circle-check"; profStat2Clr="#10b981";
        profStat3Lbl="Institusi"; profStat3Val="Sekolah"; profStat3Ico="fa-school"; profStat3Clr="#0ea5e9";
        profLinks = new String[][]{
            {"fa-star",      "Nilai",     _root+"/baru?p=akademik&s=nilai","#7c3aed"},
            {"fa-calendar-days","Jadwal", _root+"/baru?p=akademik",         "#4f46e5"},
            {"fa-credit-card","Tagihan",  _root+"/baru?p=pembayaran",       "#f59e0b"}
        };

    } else {
        /* --- ADMIN PT atau ADMIN SEKOLAH --- */
        profBadge    = _sekolahMode?"Admin Sekolah":"Admin";
        profBadgeClr = "#92400e";
        profHeaderClr= "linear-gradient(135deg,#1c1917 0%,#78350f 55%,#92400e 100%)";
        profIdLabel  = "User";

        if(!_sekolahMode){
            try{
                Long _mhsCnt=(Long)_db.createQuery("select count(m.id) from Mahasiswa m").uniqueResult();
                profStat1Val=_mhsCnt!=null?String.valueOf(_mhsCnt):"—";
            } catch(Exception _ex){}
            profStat1Lbl="Mahasiswa"; profStat1Ico="fa-user-graduate"; profStat1Clr="#4f46e5";

            try{
                Long _dsnCnt=(Long)_db.createQuery("select count(d.id) from Dosen d").uniqueResult();
                profStat2Val=_dsnCnt!=null?String.valueOf(_dsnCnt):"—";
            } catch(Exception _ex){}
            profStat2Lbl="Dosen"; profStat2Ico="fa-chalkboard-user"; profStat2Clr="#0ea5e9";

            try{
                Long _prdCnt=(Long)_db.createQuery("select count(j.id) from Jurusan j").uniqueResult();
                profStat3Val=_prdCnt!=null?String.valueOf(_prdCnt):"—";
            } catch(Exception _ex){}
            profStat3Lbl="Prodi"; profStat3Ico="fa-sitemap"; profStat3Clr="#7c3aed";
        } else {
            try{
                Long _swCnt=(Long)_db.createQuery("select count(s.id) from Siswa s").uniqueResult();
                profStat1Val=_swCnt!=null?String.valueOf(_swCnt):"—";
            } catch(Exception _ex){}
            profStat1Lbl="Siswa"; profStat1Ico="fa-user-graduate"; profStat1Clr="#7c3aed";

            try{
                Long _grCnt=(Long)_db.createQuery("select count(g.id) from Guru g").uniqueResult();
                profStat2Val=_grCnt!=null?String.valueOf(_grCnt):"—";
            } catch(Exception _ex){}
            profStat2Lbl="Guru"; profStat2Ico="fa-chalkboard-user"; profStat2Clr="#059669";

            try{
                Long _klsCnt=(Long)_db.createQuery("select count(k.id) from Kelas k").uniqueResult();
                profStat3Val=_klsCnt!=null?String.valueOf(_klsCnt):"—";
            } catch(Exception _ex){}
            profStat3Lbl="Kelas"; profStat3Ico="fa-door-open"; profStat3Clr="#0ea5e9";
        }

        profLinks = new String[][]{
            {"fa-gauge-high",  "Dasbor",    _root+"/baru?p=akademik",      "#4f46e5"},
            {"fa-sliders",     "Konfigurasi",_root+"/main?hak_akses=true", "#7c3aed"},
            {"fa-cloud-arrow-up","Feeder",  _root+"/baru?p=feeder",        "#0d9488"},
            {"fa-users-gear",  "SDM",       _root+"/baru?p=kepegawaian",   "#92400e"}
        };
    }

} catch(Exception _ex){
    // Fallback — profLinks kosong
    profLinks = new String[][]{
        {"fa-house","Beranda","javascript:tampilkanHome()","#4f46e5"}
    };
} finally {
    if(_db!=null) try{_db.close();}catch(Exception _ign){}
}

/* === Extra stats untuk sekolah admin (donut gender + data rinci) === */
long _swLaki=0, _swPrm=0, _swTotal=0, _mpTotal=0;
String _swLakiS="—", _swPrmS="—", _mpTotalS="—";
int _lakiPct=0, _prmPct=0;
String _lakiDash="0 138", _prmDash="0 138";

if(_isAdmin && _sekolahMode) {
    org.hibernate.Session _db2=null;
    try {
        _db2=ais.database.hibernate.HibernateUtil.openSession();
        try{ Long v=(Long)_db2.createQuery("select count(s.id) from Siswa s where lower(s.jenisKelamin)='l' or s.jenisKelamin='Laki-laki'").uniqueResult(); if(v!=null)_swLaki=v; } catch(Exception _x){}
        try{ Long v=(Long)_db2.createQuery("select count(s.id) from Siswa s where lower(s.jenisKelamin)='p' or s.jenisKelamin='Perempuan'").uniqueResult(); if(v!=null)_swPrm=v; } catch(Exception _x){}
        try{ Long v=(Long)_db2.createQuery("select count(mp.id) from MataPelajaran mp").uniqueResult(); if(v!=null)_mpTotal=v; } catch(Exception _x){}
    } catch(Exception _x){}
    finally{ if(_db2!=null)try{_db2.close();}catch(Exception _ign){} }

    _swTotal = _swLaki + _swPrm;
    _swLakiS = String.valueOf(_swLaki);
    _swPrmS  = String.valueOf(_swPrm);
    _mpTotalS= String.valueOf(_mpTotal);

    if(_swTotal > 0) {
        _lakiPct = (int)Math.round(_swLaki * 100.0 / _swTotal);
        _prmPct  = 100 - _lakiPct;
        double _circ = 138.23;
        _lakiDash = String.format(java.util.Locale.US,"%.1f %.1f", _circ*_lakiPct/100.0, _circ*(100-_lakiPct)/100.0);
        _prmDash  = String.format(java.util.Locale.US,"%.1f %.1f", _circ*_prmPct/100.0,  _circ*(100-_prmPct)/100.0);
    }
}

String _profAvatar = _ini(profNama);
%>
<!-- === PANEL PROFIL PENGGUNA === -->
<div class="profil-panel">

  <!-- Header gradient -->
  <div class="profil-header" style="background:<%=profHeaderClr%>">
    <div class="profil-sapaan">Selamat <%=_sapaan%> 👋</div>
    <div class="profil-avatar-wrap">
      <div class="profil-avatar"><%=_sh(_profAvatar)%></div>
    </div>
    <div class="profil-nama"><%=_sh(profNama)%></div>
    <div class="profil-badge-row">
      <span class="profil-badge" style="background:<%=profBadgeClr%>"><%=_sh(profBadge)%></span>
    </div>
    <%if(!profUnit.isEmpty()){%>
    <div class="profil-unit"><%=_sh(profUnit)%></div>
    <%}%>
    <%if(!profIdVal.isEmpty()&&!"—".equals(profIdVal)){%>
    <div class="profil-id-row">
      <span class="profil-id-lbl"><%=_sh(profIdLabel)%>:</span>
      <span class="profil-id-val"><%=_sh(profIdVal)%></span>
    </div>
    <%}%>
  </div>

  <!-- Stats -->
  <div class="profil-stats">
    <div class="profil-stat">
      <div class="profil-stat-ic" style="color:<%=profStat1Clr%>"><i class="fa-solid <%=_sh(profStat1Ico)%>"></i></div>
      <div class="profil-stat-val"><%=_sh(profStat1Val)%></div>
      <div class="profil-stat-lbl"><%=_sh(profStat1Lbl)%></div>
    </div>
    <div class="profil-stat">
      <div class="profil-stat-ic" style="color:<%=profStat2Clr%>"><i class="fa-solid <%=_sh(profStat2Ico)%>"></i></div>
      <div class="profil-stat-val"><%=_sh(profStat2Val)%></div>
      <div class="profil-stat-lbl"><%=_sh(profStat2Lbl)%></div>
    </div>
    <div class="profil-stat">
      <div class="profil-stat-ic" style="color:<%=profStat3Clr%>"><i class="fa-solid <%=_sh(profStat3Ico)%>"></i></div>
      <div class="profil-stat-val"><%=_sh(profStat3Val)%></div>
      <div class="profil-stat-lbl"><%=_sh(profStat3Lbl)%></div>
    </div>
  </div>

  <%-- === Angka Penting (sekolah admin): gender donut + mapel === --%>
  <%if(_isAdmin && _sekolahMode && _swTotal > 0){%>
  <div style="padding:12px 14px 0;border-top:1px solid var(--line)">
    <div style="font-size:10px;font-weight:800;text-transform:uppercase;letter-spacing:.7px;color:var(--ink3);margin-bottom:10px">
      <i class="fa-solid fa-chart-pie" style="font-size:11px;margin-right:4px;color:var(--pri2)"></i> Angka Penting
    </div>
    <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-bottom:10px">
      <%-- Donut laki-laki --%>
      <div style="display:flex;flex-direction:column;align-items:center;gap:5px">
        <svg width="64" height="64" viewBox="0 0 60 60">
          <circle cx="30" cy="30" r="22" fill="none" stroke="var(--line)" stroke-width="7"/>
          <circle cx="30" cy="30" r="22" fill="none" stroke="#3b82f6" stroke-width="7"
                  stroke-dasharray="<%=_lakiDash%>" stroke-dashoffset="-34.6"
                  transform="rotate(-90 30 30)" stroke-linecap="round"/>
          <text x="30" y="34" text-anchor="middle" font-size="11" font-weight="800" fill="var(--ink)"><%=_lakiPct%>%</text>
        </svg>
        <div style="font-size:11.5px;font-weight:700;color:var(--ink)"><%=_swLakiS%></div>
        <div style="font-size:10px;color:var(--ink2)">Siswa L</div>
      </div>
      <%-- Donut perempuan --%>
      <div style="display:flex;flex-direction:column;align-items:center;gap:5px">
        <svg width="64" height="64" viewBox="0 0 60 60">
          <circle cx="30" cy="30" r="22" fill="none" stroke="var(--line)" stroke-width="7"/>
          <circle cx="30" cy="30" r="22" fill="none" stroke="#f472b6" stroke-width="7"
                  stroke-dasharray="<%=_prmDash%>" stroke-dashoffset="-34.6"
                  transform="rotate(-90 30 30)" stroke-linecap="round"/>
          <text x="30" y="34" text-anchor="middle" font-size="11" font-weight="800" fill="var(--ink)"><%=_prmPct%>%</text>
        </svg>
        <div style="font-size:11.5px;font-weight:700;color:var(--ink)"><%=_swPrmS%></div>
        <div style="font-size:10px;color:var(--ink2)">Siswa P</div>
      </div>
    </div>
    <%if(_mpTotal > 0){%>
    <div style="display:flex;align-items:center;justify-content:space-between;padding:8px 10px;background:var(--bg);border-radius:9px;margin-bottom:10px">
      <span style="font-size:12px;font-weight:600;color:var(--ink2)"><i class="fa-solid fa-book-open" style="color:#7c3aed;margin-right:6px"></i>Mata Pelajaran</span>
      <span style="font-size:13px;font-weight:800;color:var(--ink)"><%=_mpTotalS%></span>
    </div>
    <%}%>
  </div>
  <%}%>

  <!-- Akses Cepat -->
  <%if(profLinks!=null&&profLinks.length>0){%>
  <div class="profil-links">
    <div class="profil-links-title"><i class="fa-solid fa-bolt" style="font-size:11px"></i> Akses Cepat</div>
    <div class="profil-links-grid">
      <%for(String[] _lnk:profLinks){%>
      <a class="profil-link" href="<%=_sh(_lnk[2])%>"
         onclick="bukaModul('<%=_sh(_lnk[1])%>','<%=_sh(_lnk[2])%>');return false;">
        <span class="profil-link-ic" style="background:<%=_sh(_lnk[3])%>15;color:<%=_sh(_lnk[3])%>">
          <i class="fa-solid <%=_sh(_lnk[0])%>"></i>
        </span>
        <span class="profil-link-lbl"><%=_sh(_lnk[1])%></span>
      </a>
      <%}%>
    </div>
  </div>
  <%}%>

  <!-- Footer: lihat profil lengkap -->
  <div class="profil-footer">
    <button class="profil-full-btn" onclick="bukaModul('profil','<%=_root%>/baru2?modul=profil')">
      <i class="fa-solid fa-user-circle"></i> Lihat Profil Lengkap
    </button>
  </div>

</div>
