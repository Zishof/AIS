<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.database.model.PerguruanTinggi" %>
<%@ page import="ais.database.model.sekolah.Sekolah" %>
<%@ page import="ais.database.model.sekolah.Yayasan" %>
<%@ page import="ais.action.master.helper.util.PerguruanTinggiUtil" %>
<%@ page import="ais.action.master.sekolah.util.SekolahUtil" %>
<%@ page import="ais.common.Common" %>
<%--
    front_end/pmb/index.jsp — Halaman PMB (Penerimaan Mahasiswa Baru) untuk tampilan baru.
    Dipanggil ketika Pmb.java mendeteksi pilihan tampilan "baru".
    Menampilkan informasi PMB dan link ke proses pendaftaran.
--%>
<%!
    private String safeHtml(String v) {
        if (v == null) return "";
        return v.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }
%>
<%
    String nama = "Sistem Informasi Akademik";
    String motto = "Penerimaan Mahasiswa Baru";
    String logo = "/img/logo.png";
    String rootCtx = Common.ROOT == null ? "" : Common.ROOT;

    try {
        PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
        if (pt != null) {
            if (pt.getNama() != null && !pt.getNama().trim().isEmpty()) nama = pt.getNama();
            if (pt.getMotto() != null && !pt.getMotto().trim().isEmpty()) motto = pt.getMotto();
        }
        String logoPT = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
        if (logoPT != null && !logoPT.trim().isEmpty()) logo = logoPT;

        boolean[] ptAtauSekolah = Common.chekPtAtauSekolah();
        boolean sekolahMode = ptAtauSekolah != null && ptAtauSekolah.length > 1 && ptAtauSekolah[1];
        if (sekolahMode) {
            Sekolah sekolah = SekolahUtil.getSekolah(request);
            if (sekolah != null && sekolah.getId() != null) {
                if (sekolah.getNama() != null) nama = sekolah.getNama();
                String logoS = SekolahUtil.getSekolahMedia(request, "logo_sekolah_");
                if (logoS != null && !logoS.trim().isEmpty() && !logoS.endsWith("logo.png")) logo = logoS;
            }
        }
    } catch (Exception ex) { /* ignore */ }
%>
<!doctype html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>PMB — <%=safeHtml(nama)%></title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<style>
:root{--pri:#123d77;--pri2:#2563eb;--acc:#06b6d4;--grad:linear-gradient(135deg,#0b3d75 0%,#12669a 58%,#0f8fa7 100%);--ok:#10b981;--ink:#10243e;--ink2:#5b6b80;--line:#dfe7f1;--bg:#f2f6fc;--card:#fff;--shadow:0 1px 2px rgba(15,39,71,.04),0 8px 24px rgba(15,39,71,.055);--r:14px;--font:"Plus Jakarta Sans","Segoe UI",system-ui,-apple-system,sans-serif}
*{box-sizing:border-box;margin:0;padding:0}
body{font-family:var(--font);background:var(--bg);color:var(--ink);min-height:100vh;-webkit-font-smoothing:antialiased}
.topbar{background:#fff;border-bottom:1px solid var(--line);display:flex;align-items:center;gap:12px;padding:0 24px;height:64px;box-shadow:0 1px 8px rgba(15,39,71,.05)}
.topbar img{height:32px}
.topbar-txt{color:var(--ink)}
.topbar-txt b{display:block;font-size:14px;font-weight:800}
.topbar-txt span{font-size:11px;color:var(--ink2)}
.topbar-right{margin-left:auto;display:flex;gap:10px}
.topbar-right a{color:var(--ink2);font-size:12.5px;font-weight:650;padding:8px 14px;border-radius:9px;border:1px solid var(--line);text-decoration:none;transition:.15s}
.topbar-right a:hover{background:#eff6ff;color:var(--pri2);border-color:#bfdbfe}
.hero{background:var(--grad);padding:48px 24px 56px;text-align:center;position:relative;overflow:hidden}
.hero::after{content:"";position:absolute;right:-80px;top:-100px;width:350px;height:350px;border-radius:50%;background:rgba(255,255,255,.06)}
.hero h1{color:#fff;font-size:28px;font-weight:900;margin-bottom:10px;position:relative}
.hero p{color:rgba(255,255,255,.85);font-size:14.5px;max-width:520px;margin:0 auto 24px;position:relative;line-height:1.6}
.hero .cta-group{display:flex;gap:12px;justify-content:center;flex-wrap:wrap;position:relative}
.cta{display:inline-flex;align-items:center;gap:8px;padding:13px 24px;border-radius:12px;font-size:14px;font-weight:800;cursor:pointer;transition:.15s;text-decoration:none;border:none}
.cta-primary{background:#fff;color:var(--pri2)}
.cta-primary:hover{transform:translateY(-2px);box-shadow:0 8px 24px rgba(0,0,0,.15)}
.cta-ghost{background:rgba(255,255,255,.15);color:#fff;border:1px solid rgba(255,255,255,.3)}
.cta-ghost:hover{background:rgba(255,255,255,.25)}
.content{padding:40px 24px;max-width:960px;margin:0 auto}
.section-title{font-size:20px;font-weight:800;margin-bottom:16px;color:var(--ink)}
.cards-row{display:grid;grid-template-columns:repeat(auto-fill,minmax(260px,1fr));gap:16px;margin-bottom:40px}
.card{background:var(--card);border:1px solid var(--line);border-radius:var(--r);padding:20px;box-shadow:var(--shadow);transition:.15s}
.card:hover{transform:translateY(-3px);box-shadow:0 8px 28px rgba(30,41,59,.12);border-color:transparent}
.card-ic{width:48px;height:48px;border-radius:13px;display:flex;align-items:center;justify-content:center;color:#fff;font-size:20px;margin-bottom:14px}
.card h3{font-size:15px;font-weight:800;margin-bottom:6px}
.card p{font-size:13px;color:var(--ink2);line-height:1.5}
.step-list{display:flex;flex-direction:column;gap:16px;margin-bottom:40px}
.step{display:flex;gap:16px;align-items:flex-start;padding:18px 20px;background:var(--card);border:1px solid var(--line);border-radius:var(--r);box-shadow:var(--shadow)}
.step-num{width:36px;height:36px;border-radius:50%;background:var(--grad);color:#fff;font-weight:900;font-size:15px;display:flex;align-items:center;justify-content:center;flex-shrink:0}
.step h4{font-size:14px;font-weight:700;margin-bottom:3px}
.step p{font-size:12.5px;color:var(--ink2)}
.contact-banner{background:var(--grad);border-radius:var(--r);padding:24px 28px;color:#fff;display:flex;align-items:center;justify-content:space-between;gap:16px;flex-wrap:wrap}
.contact-banner h3{font-size:17px;font-weight:800;margin-bottom:4px}
.contact-banner p{font-size:13px;opacity:.85}
.contact-banner .cta-ghost{padding:10px 20px;font-size:13px}
@media(max-width:640px){.topbar{padding:0 14px}.topbar-txt span{display:none}.topbar-right a{padding:8px 10px}.topbar-right a:first-child{display:none}.hero{padding:38px 18px 44px}.hero h1{font-size:23px}.content{padding:28px 16px}}
</style>
</head>
<body>
<div class="topbar">
  <img src="<%=rootCtx%><%=safeHtml(logo)%>" alt="Logo" onerror="this.src='<%=rootCtx%>/img/logo.png'">
  <div class="topbar-txt">
    <b><%=safeHtml(nama)%></b>
    <span>Penerimaan Mahasiswa/Siswa Baru</span>
  </div>
  <div class="topbar-right">
    <a href="<%=rootCtx%>/pmb"><i class="fa-solid fa-user-plus"></i> Daftar Sekarang</a>
    <a href="<%=rootCtx%>/z"><i class="fa-solid fa-globe"></i> Masuk</a>
  </div>
</div>

<section class="hero">
  <h1>Selamat Datang di Portal PMB<br><%=safeHtml(nama)%></h1>
  <p>Pendaftaran mahasiswa baru secara online — mudah, cepat, dan transparan. Daftarkan diri Anda sekarang dan raih masa depan bersama kami.</p>
  <div class="cta-group">
    <a class="cta cta-primary" href="<%=rootCtx%>/pmb">
      <i class="fa-solid fa-user-plus"></i> Daftar Sekarang
    </a>
    <a class="cta cta-ghost" href="<%=rootCtx%>/pmb?page=cek">
      <i class="fa-solid fa-magnifying-glass"></i> Cek Status Pendaftaran
    </a>
    <a class="cta cta-ghost" href="<%=rootCtx%>/pmb?page=bayar">
      <i class="fa-solid fa-credit-card"></i> Bayar Biaya Pendaftaran
    </a>
  </div>
</section>

<div class="content">
  <div class="section-title">Jalur Pendaftaran</div>
  <div class="cards-row">
    <div class="card">
      <div class="card-ic" style="background:linear-gradient(135deg,#4f46e5,#6d28d9)"><i class="fa-solid fa-star"></i></div>
      <h3>Jalur Reguler</h3>
      <p>Pendaftaran mahasiswa baru melalui jalur reguler dengan seleksi berkas dan/atau tes tertulis.</p>
    </div>
    <div class="card">
      <div class="card-ic" style="background:linear-gradient(135deg,#0891b2,#0ea5e9)"><i class="fa-solid fa-trophy"></i></div>
      <h3>Jalur Prestasi</h3>
      <p>Tersedia bagi calon mahasiswa berprestasi di bidang akademik, olahraga, atau seni budaya.</p>
    </div>
    <div class="card">
      <div class="card-ic" style="background:linear-gradient(135deg,#059669,#10b981)"><i class="fa-solid fa-rotate"></i></div>
      <h3>Rekognisi Pembelajaran Lampau (RPL)</h3>
      <p>Pengakuan pengalaman kerja dan pendidikan non-formal sebagai pengganti beberapa SKS.</p>
    </div>
    <div class="card">
      <div class="card-ic" style="background:linear-gradient(135deg,#b45309,#d97706)"><i class="fa-solid fa-briefcase"></i></div>
      <h3>Kelas Karyawan</h3>
      <p>Kelas khusus karyawan yang bekerja dengan jadwal kuliah di luar jam kerja.</p>
    </div>
  </div>

  <div class="section-title">Alur Pendaftaran</div>
  <div class="step-list">
    <div class="step">
      <div class="step-num">1</div>
      <div>
        <h4>Registrasi Akun &amp; Isi Formulir</h4>
        <p>Buat akun pendaftar, lengkapi formulir biodata dan unggah dokumen yang diperlukan.</p>
      </div>
    </div>
    <div class="step">
      <div class="step-num">2</div>
      <div>
        <h4>Bayar Biaya Pendaftaran</h4>
        <p>Lakukan pembayaran biaya pendaftaran melalui kanal yang tersedia (transfer bank, virtual account, dll.).</p>
      </div>
    </div>
    <div class="step">
      <div class="step-num">3</div>
      <div>
        <h4>Seleksi</h4>
        <p>Ikuti tahap seleksi sesuai jalur yang dipilih: berkas, tes tulis, atau wawancara.</p>
      </div>
    </div>
    <div class="step">
      <div class="step-num">4</div>
      <div>
        <h4>Pengumuman &amp; Daftar Ulang</h4>
        <p>Cek pengumuman kelulusan seleksi, lalu lakukan daftar ulang jika dinyatakan lulus.</p>
      </div>
    </div>
  </div>

  <div class="contact-banner">
    <div>
      <h3>Butuh Bantuan?</h3>
      <p>Hubungi tim PMB kami melalui WhatsApp atau kunjungi kantor pendaftaran.</p>
    </div>
    <a class="cta cta-ghost" href="<%=rootCtx%>/pmb?page=kontak">
      <i class="fa-solid fa-headset"></i> Hubungi Kami
    </a>
  </div>
</div>
</body>
</html>
