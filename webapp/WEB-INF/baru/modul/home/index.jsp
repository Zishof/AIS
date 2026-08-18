<%@page import="ais.action.master.helper.profile.ProfileGabunganPengguna"%>
<%@page import="ais.database.model.sekolah.Guru"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
Mahasiswa mahasiswa = tbmuser != null ? tbmuser.getMahasiswa()  : null;
Siswa     siswa     = tbmuser != null ? tbmuser.getSiswa()      : null;
Dosen     dosen     = tbmuser != null ? tbmuser.ambilDosen()    : null;
Guru      guru      = tbmuser != null ? tbmuser.ambilGuru()     : null;
boolean   isAnggotaKoperasi = tbmuser != null && tbmuser.getAnggotaKoperasi() != null;
boolean   isPedagang = tbmuser != null && tbmuser.getPedagang() != null;
boolean   isAdmin   = tbmuser != null && !isAnggotaKoperasi && !isPedagang
        && mahasiswa == null && siswa == null && dosen == null && guru == null;

boolean[] ptYa = tbmuser != null ? Common.chekPtAtauSekolah(tbmuser) : new boolean[]{false, false};
boolean pt = ptYa[0];
boolean ya = ptYa[1];

// Cek ProfileGabunganPengguna (multi-role combined): ikut logika ProfileAction
boolean isGabungan = false;
try {
    isGabungan = tbmuser != null && ProfileGabunganPengguna.aktifUntukLoginSaatIni(tbmuser);
} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/home/index.jsp:24");}
%>

<%
// ---- Data untuk HERO sambutan (salam berbasis jam + identitas ringkas) ----
String namaSapaan = "Pengguna";
String roleSapaan = "";
try {
    if (tbmuser != null) {
        namaSapaan = (tbmuser.getUserNama() != null && !tbmuser.getUserNama().trim().isEmpty())
                ? tbmuser.getUserNama() : (tbmuser.getUserId() != null ? tbmuser.getUserId() : "Pengguna");
        if (tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleName() != null) roleSapaan = tbmuser.hakAkses().getRoleName();
    }
} catch (Exception e) {}
int jamNow = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
String salamNow = jamNow < 11 ? "Selamat Pagi" : jamNow < 15 ? "Selamat Siang" : jamNow < 18 ? "Selamat Sore" : "Selamat Malam";
String tanggalNow;
try { tanggalNow = new java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", new java.util.Locale("id","ID")).format(ais.ui.util.WaktuUtil.getDate()); }
catch (Exception e) { tanggalNow = new java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", new java.util.Locale("id","ID")).format(new java.util.Date()); }
%>

<%-- ===================================================================
     HERO SAMBUTAN (responsif, gradien) — meniru praktik dasbor modern
     =================================================================== --%>
<div class="card border-0 shadow-sm mb-4 overflow-hidden" style="border-radius:18px;background:linear-gradient(120deg,#4f46e5 0%,#6d28d9 55%,#7c3aed 100%);">
    <div class="card-body p-4 position-relative" style="color:#fff;">
        <div class="d-none d-md-block position-absolute" style="right:-10px;top:-30px;opacity:.15;font-size:150px;line-height:1;">
            <i class="fas fa-graduation-cap"></i>
        </div>
        <div class="position-relative">
            <h3 class="fw-bold mb-1" style="letter-spacing:.2px;"><%=salamNow%>, <%=namaSapaan%> <span style="font-weight:400;">&#128075;</span></h3>
            <div class="mb-3" style="opacity:.9;font-size:.95rem;"><%=Common.getBahasaConfig("Kelola institusi dengan data terintegrasi dalam satu platform.")%></div>
            <div class="d-flex flex-wrap gap-2">
                <span class="badge rounded-pill px-3 py-2" style="background:rgba(255,255,255,.18);font-weight:500;"><i class="far fa-calendar-alt me-1"></i> <%=tanggalNow%></span>
                <% if (!roleSapaan.isEmpty()) { %>
                <span class="badge rounded-pill px-3 py-2" style="background:rgba(255,255,255,.18);font-weight:500;"><i class="fas fa-user-shield me-1"></i> <%=roleSapaan%></span>
                <% } %>
            </div>
        </div>
    </div>
</div>

<%-- ===== AKSES CEPAT MODUL (grid pintasan role-gated, dari index.zul) ===== --%>
<jsp:include page="/WEB-INF/baru/modul/home/akses_cepat_modul.jsp"></jsp:include>

<%-- ===================================================================
     LAYOUT DASBOR: 2-KOLOM (cocok dengan ZKoss renderModernHomeCenter)
     Kiri  (col-lg-8):  Kehadiran Pengajar + Pengumuman + Kalender
     Kanan (col-lg-4):  Panel Profil berdasarkan peran
     =================================================================== --%>
<style>
    /* Paksa kartu profil di kolom kanan jadi single-column agar tidak terlalu sempit */
    #home-right-col .col-xl-6,
    #home-right-col .col-sm-6 {
        width: 100% !important;
        max-width: 100% !important;
        flex: 0 0 100% !important;
    }
</style>

<div class="row g-3 mb-4">

    <%-- ===== KOLOM KIRI: info kehadiran + pengumuman + kalender ===== --%>
    <div class="col-lg-8 col-12" id="home-left-col">
<%
if (pt) {
%>
        <jsp:include page="/WEB-INF/baru/modul/home/info_kehadiran_dosen.jsp"></jsp:include>
<%
}
if (ya) {
%>
        <jsp:include page="/WEB-INF/baru/modul/home/info_kehadiran_guru.jsp"></jsp:include>
<%
}
%>
        <jsp:include page="/WEB-INF/baru/modul/home/pengumuman.jsp"></jsp:include>
        <jsp:include page="/WEB-INF/baru/modul/home/kalender_akademik.jsp"></jsp:include>
    </div>

    <%-- ===== KOLOM KANAN: profil berdasarkan peran (ikut urutan ProfileAction) ===== --%>
    <div class="col-lg-4 col-12" id="home-right-col">
<%
if (isGabungan) {
    // ProfileGabunganPengguna: tampilkan profil utama berdasarkan peran yang dominan
    // (ZKoss: new ProfileGabunganPengguna(tbmuser, tampilkanChart).init(center))
    if (mahasiswa != null) {
%>
        <jsp:include page="/WEB-INF/baru/modul/home/mahasiswa/info.jsp"></jsp:include>
        <jsp:include page="/WEB-INF/baru/modul/home/mahasiswa/tombol.jsp"></jsp:include>
<%
    } else if (dosen != null) {
%>
        <jsp:include page="/WEB-INF/baru/modul/home/dosen/info.jsp"></jsp:include>
        <jsp:include page="/WEB-INF/baru/modul/home/dosen/tombol.jsp"></jsp:include>
<%
    } else if (guru != null) {
%>
        <jsp:include page="/WEB-INF/baru/modul/home/guru/info.jsp"></jsp:include>
<%
    } else if (siswa != null) {
%>
        <jsp:include page="/WEB-INF/baru/modul/home/siswa/info.jsp"></jsp:include>
<%
    }
} else if (mahasiswa != null) {
    // ProfileMahasiswa
%>
        <jsp:include page="/WEB-INF/baru/modul/home/mahasiswa/info.jsp"></jsp:include>
        <jsp:include page="/WEB-INF/baru/modul/home/mahasiswa/tombol.jsp"></jsp:include>
<%
} else if (dosen != null) {
    // ProfileDosen
%>
        <jsp:include page="/WEB-INF/baru/modul/home/dosen/info.jsp"></jsp:include>
        <jsp:include page="/WEB-INF/baru/modul/home/dosen/tombol.jsp"></jsp:include>
<%
} else if (ya && siswa != null) {
    // ProfileSiswa (yayasan + siswa)
%>
        <jsp:include page="/WEB-INF/baru/modul/home/siswa/info.jsp"></jsp:include>
<%
} else if (ya && guru != null) {
    // ProfileGuru (yayasan + guru)
%>
        <jsp:include page="/WEB-INF/baru/modul/home/guru/info.jsp"></jsp:include>
<%
} else if (isAdmin && ya) {
    // ProfileAdminSekolah (sekolah admin)
%>
        <jsp:include page="/WEB-INF/baru/modul/home/admin_sekolah/profile.jsp"></jsp:include>
<%
} else if (isAdmin && pt) {
    // ProfileAdminPerguruanTinggi (admin PT)
%>
        <jsp:include page="/WEB-INF/baru/modul/home/admin_pt/profile.jsp"></jsp:include>
<%
} else if (isAdmin) {
    // Fallback admin tanpa PT/YA teridentifikasi
%>
        <jsp:include page="/WEB-INF/baru/modul/home/admin_pt/profile.jsp"></jsp:include>
<%
}
%>
    </div>
</div>

<%-- ===================================================================
     DASBOR ADMIN RINCI: di bawah layout 2-kolom (admin saja)
     Setara dengan panel kiri ZKoss yang memuat kehadiran dosen + pengumuman,
     tapi untuk admin juga dimuat dashboard statistik lengkap.
     =================================================================== --%>
<%
if (isAdmin) {
%>
<div class="row">
    <div class="col-12">
        <jsp:include page="/WEB-INF/baru/modul/home/admin.jsp"></jsp:include>
    </div>
</div>
<%
}
%>
