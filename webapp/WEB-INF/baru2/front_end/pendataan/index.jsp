<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<%
    String rootCtx = Common.ROOT == null ? "" : Common.ROOT;
%>
<!doctype html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Pendataan</title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<style>
:root{--pri:#1e40af;--pri2:#4f46e5;--acc:#6d28d9;--grad:linear-gradient(120deg,#1e3a8a 0%,#4f46e5 55%,#6d28d9 100%);--ink:#1e293b;--ink2:#64748b;--line:#e2e8f0;--bg:#f8fafc;--card:#fff;--r:12px;--sh:0 1px 3px rgba(0,0,0,.06),0 4px 12px rgba(0,0,0,.04);--font:"Plus Jakarta Sans","Segoe UI",system-ui,sans-serif}
*{box-sizing:border-box;margin:0;padding:0}
body{font-family:var(--font);background:var(--bg);color:var(--ink);padding:20px}
.hub-header{background:var(--grad);border-radius:var(--r);padding:24px 28px;color:#fff;margin-bottom:24px;display:flex;align-items:center;gap:18px}
.hub-header i{font-size:36px;opacity:.9}
.hub-header h1{font-size:20px;font-weight:900}
.hub-header p{font-size:13.5px;opacity:.85;margin-top:4px}
.search-bar{display:flex;gap:10px;margin-bottom:22px}
.search-bar input{flex:1;padding:11px 16px 11px 44px;border:1.5px solid var(--line);border-radius:10px;font-size:14px;font-family:var(--font);outline:none;background:#fff;transition:.15s}
.search-bar input:focus{border-color:var(--pri2);box-shadow:0 0 0 3px rgba(79,70,229,.1)}
.search-wrap{position:relative;flex:1}
.search-wrap i{position:absolute;left:15px;top:50%;transform:translateY(-50%);color:#94a3b8;font-size:14px}
.group-title{font-size:11.5px;font-weight:800;text-transform:uppercase;letter-spacing:.8px;color:var(--ink2);margin:0 0 10px;padding-left:2px}
.modul-group{margin-bottom:28px}
.modul-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(190px,1fr));gap:10px}
.modul-card{background:var(--card);border:1.5px solid var(--line);border-radius:11px;padding:14px 16px;cursor:pointer;transition:.15s;display:flex;align-items:center;gap:12px;text-decoration:none;color:inherit}
.modul-card:hover{border-color:transparent;box-shadow:0 6px 20px rgba(79,70,229,.18);transform:translateY(-2px)}
.modul-card:hover .mc-ic{background:var(--grad)}
.mc-ic{width:40px;height:40px;border-radius:10px;background:#f0f4ff;color:var(--pri2);display:flex;align-items:center;justify-content:center;font-size:17px;flex-shrink:0;transition:.15s}
.mc-label{font-size:13px;font-weight:700;line-height:1.3;color:var(--ink)}
.mc-label small{display:block;font-size:11px;color:var(--ink2);font-weight:400;margin-top:2px}
.modul-card.hidden{display:none}
@media(max-width:640px){.modul-grid{grid-template-columns:1fr 1fr}.hub-header h1{font-size:16px}}
</style>
</head>
<body>

<div class="hub-header">
  <i class="fa-solid fa-database"></i>
  <div>
    <h1>Modul Pendataan</h1>
    <p>Kelola seluruh data master akademik, kepegawaian, dan kemahasiswaan dari satu tempat.</p>
  </div>
</div>

<div class="search-bar">
  <div class="search-wrap">
    <i class="fa-solid fa-magnifying-glass"></i>
    <input type="text" id="carIModul" placeholder="Cari modul pendataan..." oninput="filterModul(this.value)">
  </div>
</div>

<div id="grupContainer">

  <div class="modul-group">
    <div class="group-title"><i class="fa-solid fa-users"></i> &nbsp;Data Akademik Mahasiswa</div>
    <div class="modul-grid">
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=mahasiswa">
        <div class="mc-ic"><i class="fa-solid fa-user-graduate"></i></div>
        <div class="mc-label">Mahasiswa<small>Biodata & status</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=krs_mahasiswa">
        <div class="mc-ic"><i class="fa-solid fa-list-check"></i></div>
        <div class="mc-label">KRS Mahasiswa<small>Kartu rencana studi</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=catatan_mahasiswa">
        <div class="mc-ic"><i class="fa-solid fa-note-sticky"></i></div>
        <div class="mc-label">Catatan Mahasiswa<small>Konseling & catatan PA</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=cuti_mahasiswa">
        <div class="mc-ic"><i class="fa-solid fa-calendar-xmark"></i></div>
        <div class="mc-label">Cuti Mahasiswa<small>Pendaftaran cuti</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=absen_piket">
        <div class="mc-ic"><i class="fa-solid fa-clipboard-user"></i></div>
        <div class="mc-label">Absen Piket<small>Kehadiran piket mhs</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=kedisiplinan">
        <div class="mc-ic"><i class="fa-solid fa-gavel"></i></div>
        <div class="mc-label">Kedisiplinan<small>Pelanggaran & sanksi</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=nilai_toafi">
        <div class="mc-ic"><i class="fa-solid fa-language"></i></div>
        <div class="mc-label">Nilai Toafi/Toefl<small>Skor bahasa mahasiswa</small></div>
      </a>
    </div>
  </div>

  <div class="modul-group">
    <div class="group-title"><i class="fa-solid fa-chalkboard-teacher"></i> &nbsp;Data Akademik Dosen & MK</div>
    <div class="modul-grid">
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=dosen">
        <div class="mc-ic"><i class="fa-solid fa-person-chalkboard"></i></div>
        <div class="mc-label">Dosen<small>Biodata & kepegawaian</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=matakuliah">
        <div class="mc-ic"><i class="fa-solid fa-book-open"></i></div>
        <div class="mc-label">Matakuliah<small>Master MK & SKS</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=kurikulum">
        <div class="mc-ic"><i class="fa-solid fa-sitemap"></i></div>
        <div class="mc-label">Kurikulum<small>Struktur kurikulum</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=kurikulum_obe">
        <div class="mc-ic"><i class="fa-solid fa-chart-line"></i></div>
        <div class="mc-label">Kurikulum OBE<small>Outcome-based education</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=paket_perkuliahan">
        <div class="mc-ic"><i class="fa-solid fa-layer-group"></i></div>
        <div class="mc-label">Paket Perkuliahan<small>Bundel MK per angkatan</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=buku_bahan_ajar">
        <div class="mc-ic"><i class="fa-solid fa-book"></i></div>
        <div class="mc-label">Buku Bahan Ajar<small>Referensi & materi ajar</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=skripsi">
        <div class="mc-ic"><i class="fa-solid fa-file-pen"></i></div>
        <div class="mc-label">Skripsi / Thesis<small>Tugas akhir mahasiswa</small></div>
      </a>
    </div>
  </div>

  <div class="modul-group">
    <div class="group-title"><i class="fa-solid fa-briefcase"></i> &nbsp;Kegiatan & Program</div>
    <div class="modul-grid">
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=kkn">
        <div class="mc-ic"><i class="fa-solid fa-people-carry-box"></i></div>
        <div class="mc-label">KKN<small>Kuliah kerja nyata</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=pkl">
        <div class="mc-ic"><i class="fa-solid fa-hard-hat"></i></div>
        <div class="mc-label">PKL<small>Praktik kerja lapangan</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=beasiswa">
        <div class="mc-ic"><i class="fa-solid fa-hand-holding-dollar"></i></div>
        <div class="mc-label">Beasiswa<small>Program & seleksi</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=kegiatan_kemahasiswaan">
        <div class="mc-ic"><i class="fa-solid fa-flag"></i></div>
        <div class="mc-label">Kegiatan Kemahasiswaan<small>Organisasi & UKM</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=kegiatan_dosen">
        <div class="mc-ic"><i class="fa-solid fa-medal"></i></div>
        <div class="mc-label">Kegiatan Dosen<small>Tri dharma & penelitian</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=pengajuan_mahasiswa">
        <div class="mc-ic"><i class="fa-solid fa-paper-plane"></i></div>
        <div class="mc-label">Pengajuan Mahasiswa<small>Pengajuan non-standar</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=formulir_kegiatan">
        <div class="mc-ic"><i class="fa-solid fa-clipboard-list"></i></div>
        <div class="mc-label">Formulir Kegiatan<small>Template formulir</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=kerjasama">
        <div class="mc-ic"><i class="fa-solid fa-handshake"></i></div>
        <div class="mc-label">Kerjasama Instansi<small>MoU & perjanjian</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=status_kerjasama">
        <div class="mc-ic"><i class="fa-solid fa-link"></i></div>
        <div class="mc-label">Status Kerjasama Mhs<small>Mhs di institusi mitra</small></div>
      </a>
    </div>
  </div>

  <div class="modul-group">
    <div class="group-title"><i class="fa-solid fa-id-badge"></i> &nbsp;Data Kepegawaian</div>
    <div class="modul-grid">
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=pegawai">
        <div class="mc-ic"><i class="fa-solid fa-user-tie"></i></div>
        <div class="mc-label">Pegawai<small>Data tenaga kependidikan</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=jabatan">
        <div class="mc-ic"><i class="fa-solid fa-sitemap"></i></div>
        <div class="mc-label">Jabatan<small>Struktur jabatan</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=staff">
        <div class="mc-ic"><i class="fa-solid fa-users-gear"></i></div>
        <div class="mc-label">Staff<small>Data staf administrasi</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=catatan_pegawai">
        <div class="mc-ic"><i class="fa-solid fa-file-circle-exclamation"></i></div>
        <div class="mc-label">Catatan Pegawai<small>Riwayat catatan kinerja</small></div>
      </a>
    </div>
  </div>

  <div class="modul-group">
    <div class="group-title"><i class="fa-solid fa-shield-halved"></i> &nbsp;Mutu, Aduan & Layanan</div>
    <div class="modul-grid">
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=spmi">
        <div class="mc-ic"><i class="fa-solid fa-award"></i></div>
        <div class="mc-label">SPMI<small>Sistem penjaminan mutu</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=penjaminan_mutu">
        <div class="mc-ic"><i class="fa-solid fa-clipboard-check"></i></div>
        <div class="mc-label">Penjaminan Mutu<small>Instrumen & audit</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=pengaduan">
        <div class="mc-ic"><i class="fa-solid fa-comment-dots"></i></div>
        <div class="mc-label">Pengaduan<small>Pelaporan & pengaduan</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=customer_service">
        <div class="mc-ic"><i class="fa-solid fa-headset"></i></div>
        <div class="mc-label">Customer Service<small>Tiket layanan & FAQ</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=bank_soal">
        <div class="mc-ic"><i class="fa-solid fa-question-circle"></i></div>
        <div class="mc-label">Bank Soal<small>Repository soal ujian</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=dokumen_pendukung">
        <div class="mc-ic"><i class="fa-solid fa-folder-open"></i></div>
        <div class="mc-label">Dokumen Pendukung<small>Arsip & berkas penting</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=template_surat">
        <div class="mc-ic"><i class="fa-solid fa-envelope-open-text"></i></div>
        <div class="mc-label">Template Surat<small>Rancangan surat resmi</small></div>
      </a>
      <a class="modul-card" href="<%=rootCtx%>/baru2?modul=pendataan&p=data_sister">
        <div class="mc-ic"><i class="fa-solid fa-satellite-dish"></i></div>
        <div class="mc-label">Data Sister<small>Sinkronisasi SISTER</small></div>
      </a>
    </div>
  </div>

</div>

<script>
function filterModul(q) {
    q = q.toLowerCase().trim();
    document.querySelectorAll('.modul-card').forEach(function(card) {
        var label = card.textContent.toLowerCase();
        card.classList.toggle('hidden', q !== '' && label.indexOf(q) === -1);
    });
    document.querySelectorAll('.modul-group').forEach(function(g) {
        var visible = g.querySelectorAll('.modul-card:not(.hidden)').length;
        g.style.display = visible === 0 && q !== '' ? 'none' : '';
    });
}
</script>
</body>
</html>
