<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<% String ctx = Common.ROOT == null ? "" : Common.ROOT; %>
<!doctype html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>e-Learning</title>
<link rel="shortcut icon" href="<%=ctx%>/img/favicon.ico" type="image/x-icon">
<%@ include file="../_libs.jsp" %>
<%@ include file="../_komponen.jsp" %>
<%@ include file="../_lang.jsp" %>
<%@ include file="../_modul_dasbor.jsp" %>
<script>var ROOT='<%=ctx%>';</script>
<style>
.el-page{padding:18px;max-width:1680px;margin:0 auto}
.el-page-head{display:flex;align-items:flex-start;gap:16px;margin:0 0 14px}
.el-page-head h1{font-size:22px;font-weight:850;letter-spacing:-.45px;margin:0 0 4px;color:var(--ais2-ink)}
.el-page-head p{font-size:12.5px;color:var(--ais2-ink2);margin:0}
.el-head-actions{margin-left:auto;display:flex;gap:8px;flex-wrap:wrap}
.el-hero{background:var(--ais2-grad);border-radius:18px;color:#fff;padding:24px 28px;position:relative;overflow:hidden;box-shadow:0 14px 34px rgba(10,61,117,.18);margin-bottom:14px}
.el-hero::before,.el-hero::after{content:"";position:absolute;border-radius:50%;background:rgba(255,255,255,.07)}
.el-hero::before{width:260px;height:260px;right:90px;bottom:-175px}
.el-hero::after{width:210px;height:210px;right:-55px;top:-85px}
.el-eyebrow{display:inline-flex;align-items:center;gap:7px;padding:4px 10px;border-radius:999px;background:rgba(255,255,255,.14);font-size:10.5px;font-weight:750;margin-bottom:11px;position:relative}
.el-hero h2{font-size:21px;font-weight:850;letter-spacing:-.35px;margin:0 0 6px;position:relative}
.el-hero p{font-size:12.5px;line-height:1.6;opacity:.88;max-width:760px;margin:0;position:relative}
.el-hero-actions{position:absolute;right:26px;bottom:24px;display:flex;gap:8px;z-index:2}
.el-hero-actions button{display:inline-flex;align-items:center;gap:7px;min-height:40px;padding:0 15px;border-radius:9px;font-size:12px;font-weight:750;border:1px solid rgba(255,255,255,.22);background:rgba(255,255,255,.11);color:#fff}
.el-hero-actions button:first-child{background:#fff;color:#1d4ed8;border-color:#fff}
.el-kpis{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:11px;margin-bottom:14px}
.el-kpi{border:1px solid var(--ais2-line);border-radius:14px;background:#fff;box-shadow:var(--ais2-shadow);padding:14px 15px;text-align:left;display:flex;align-items:center;gap:12px;transition:var(--ais2-transition);color:var(--ais2-ink);min-height:88px}
.el-kpi:hover{transform:translateY(-2px);box-shadow:var(--ais2-shadow-md);border-color:#bfdbfe}
.el-kpi-ic{width:40px;height:40px;border-radius:11px;display:flex;align-items:center;justify-content:center;font-size:16px;flex-shrink:0}
.el-kpi-val{font-size:22px;font-weight:850;letter-spacing:-.5px;line-height:1.05}
.el-kpi-lbl{font-size:11.5px;color:var(--ais2-ink2);margin-top:4px;line-height:1.35}
.el-skeleton{display:inline-block;width:42px;height:18px;border-radius:6px;background:linear-gradient(90deg,#e8eef7 25%,#f8fafc 45%,#e8eef7 65%);background-size:240% 100%;animation:elShimmer 1.2s infinite}
@keyframes elShimmer{to{background-position:-240% 0}}
.el-workspace{background:#fff;border:1px solid var(--ais2-line);border-radius:14px;box-shadow:var(--ais2-shadow);overflow:hidden}
.el-toolbar{padding:12px 14px;border-bottom:1px solid var(--ais2-line);display:flex;align-items:center;gap:10px;flex-wrap:wrap}
.el-search{display:flex;align-items:center;gap:8px;min-width:240px;max-width:420px;flex:1;height:40px;border:1px solid var(--ais2-line);border-radius:9px;background:var(--ais2-bg);padding:0 12px;color:var(--ais2-ink3)}
.el-search:focus-within{background:#fff;border-color:#93c5fd;box-shadow:0 0 0 3px rgba(37,99,235,.08)}
.el-search input{border:0;outline:0;background:transparent;flex:1;min-width:0;font:500 12px var(--ais2-font);color:var(--ais2-ink)}
.el-nav{display:flex;gap:4px;overflow-x:auto;padding:9px 14px;border-bottom:1px solid var(--ais2-line);scrollbar-width:thin}
.el-nav button,.el-nav span{display:inline-flex;align-items:center;gap:7px;min-height:36px;padding:0 12px;border-radius:8px;white-space:nowrap;font-size:11.5px;font-weight:700;color:var(--ais2-ink2);background:transparent;border:0}
.el-nav span.active{background:#eaf2ff;color:#1d4ed8}
.el-nav button:hover{background:var(--ais2-bg);color:var(--ais2-pri2)}
.el-content{padding:16px}
.el-section-head{display:flex;align-items:center;gap:10px;margin-bottom:12px}
.el-section-head h3{font-size:14px;font-weight:800;margin:0;color:var(--ais2-ink)}
.el-section-head p{font-size:11.5px;color:var(--ais2-ink2);margin:0}
.el-action-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:11px}
.el-action{min-height:126px;background:#fff;border:1px solid var(--ais2-line);border-radius:13px;padding:14px;text-align:left;display:flex;flex-direction:column;transition:var(--ais2-transition);color:var(--ais2-ink)}
.el-action:hover{transform:translateY(-2px);border-color:#93c5fd;box-shadow:var(--ais2-shadow-md)}
.el-action-top{display:flex;align-items:flex-start;justify-content:space-between;gap:10px;margin-bottom:11px}
.el-action-ic{width:38px;height:38px;border-radius:10px;display:flex;align-items:center;justify-content:center;color:#fff;font-size:15px}
.el-action-arrow{color:var(--ais2-ink3);font-size:11px;margin-top:4px}
.el-action h4{font-size:13px;font-weight:780;margin:0 0 4px}
.el-action p{font-size:11.5px;color:var(--ais2-ink2);line-height:1.45;margin:0}
.el-flow{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:0;margin-top:14px;border:1px solid var(--ais2-line);border-radius:13px;overflow:hidden}
.el-flow button{padding:13px 14px;background:#fff;border:0;border-right:1px solid var(--ais2-line);text-align:left;display:flex;align-items:center;gap:10px;min-height:64px;color:var(--ais2-ink)}
.el-flow button:last-child{border-right:0}
.el-flow button:hover{background:#f8fbff}
.el-flow-num{width:28px;height:28px;border-radius:50%;display:flex;align-items:center;justify-content:center;background:#eaf2ff;color:#1d4ed8;font-size:11px;font-weight:850;flex-shrink:0}
.el-flow strong{display:block;font-size:11.5px}.el-flow small{display:block;color:var(--ais2-ink2);font-size:10px;margin-top:2px}
.el-error{display:none;margin-bottom:12px;padding:11px 13px;border:1px solid #fecaca;background:#fff7f7;color:#b91c1c;border-radius:10px;font-size:12px;align-items:center;gap:9px}
.el-error.show{display:flex}.el-error button{margin-left:auto;color:#b91c1c;font-weight:750;text-decoration:underline}
.el-empty{display:none;text-align:center;padding:34px 18px;color:var(--ais2-ink2)}
.el-empty.show{display:block}.el-empty i{font-size:24px;color:var(--ais2-ink3);display:block;margin-bottom:8px}
@media(max-width:1050px){.el-action-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.el-hero-actions{position:relative;right:auto;bottom:auto;margin-top:18px}.el-kpis{grid-template-columns:repeat(2,minmax(0,1fr))}}
@media(max-width:650px){.el-page{padding:12px}.el-page-head{align-items:center}.el-head-actions{display:none}.el-page-head h1{font-size:19px}.el-hero{padding:20px}.el-hero h2{font-size:18px}.el-kpis{gap:8px}.el-kpi{padding:12px;min-height:82px}.el-kpi-ic{width:35px;height:35px}.el-kpi-val{font-size:19px}.el-action-grid{grid-template-columns:1fr}.el-flow{grid-template-columns:1fr 1fr}.el-flow button:nth-child(2){border-right:0}.el-flow button:nth-child(-n+2){border-bottom:1px solid var(--ais2-line)}.el-content{padding:12px}.el-toolbar{padding:10px}.el-search{min-width:100%}}
@media(prefers-reduced-motion:reduce){.el-skeleton{animation:none}}
</style>
</head>
<body class="ais2-page">
<main class="el-page">
  <header class="el-page-head">
    <div>
      <h1>Ruang Belajar Digital</h1>
      <p>Kelola kelas, materi, penilaian, kolaborasi, dan progres belajar dari satu tempat.</p>
    </div>
    <div class="el-head-actions">
      <button class="ais2-btn ais2-btn-outline" type="button" onclick="buka('kalender')"><i class="fa-regular fa-calendar"></i> Lihat Jadwal</button>
      <button class="ais2-btn ais2-btn-primary" type="button" onclick="buka('buat_pertemuan')"><i class="fa-solid fa-plus"></i> Buat Aktivitas</button>
    </div>
  </header>

  <section class="el-hero" aria-labelledby="elHeroTitle">
    <span class="el-eyebrow"><i class="fa-solid fa-graduation-cap"></i> e-Learning Terpadu</span>
    <h2 id="elHeroTitle">Fokus pada kegiatan belajar yang paling penting</h2>
    <p>Seluruh fungsi existing tetap tersedia: pertemuan, materi multimedia, tugas individu dan kelompok, ujian, diskusi, presensi, laporan nilai, OBE, dan bimbingan.</p>
    <div class="el-hero-actions">
      <button type="button" onclick="buka('buat_pertemuan')"><i class="fa-solid fa-plus"></i> Buat Aktivitas</button>
      <button type="button" onclick="buka('kalender')"><i class="fa-regular fa-calendar"></i> Lihat Jadwal</button>
    </div>
  </section>

  <div class="el-error" id="elError" role="alert">
    <i class="fa-solid fa-triangle-exclamation"></i>
    <span>Ringkasan belum dapat dimuat. Menu pembelajaran tetap dapat digunakan.</span>
    <button type="button" onclick="muatStatistik()">Coba lagi</button>
  </div>

  <section class="el-kpis" aria-label="Ringkasan e-Learning">
    <button class="el-kpi" type="button" onclick="buka('index')" aria-label="Buka daftar kelas aktif">
      <span class="el-kpi-ic" style="background:#dbeafe;color:#1d4ed8"><i class="fa-solid fa-book-open"></i></span>
      <span><span class="el-kpi-val" id="sKelas"><span class="el-skeleton" aria-label="Memuat"></span></span><span class="el-kpi-lbl">Kelas aktif</span></span>
    </button>
    <button class="el-kpi" type="button" onclick="buka('daftar_peserta')" aria-label="Buka daftar peserta">
      <span class="el-kpi-ic" style="background:#dcfce7;color:#15803d"><i class="fa-solid fa-users"></i></span>
      <span><span class="el-kpi-val" id="sPeserta"><span class="el-skeleton" aria-label="Memuat"></span></span><span class="el-kpi-lbl">Peserta pembelajaran</span></span>
    </button>
    <button class="el-kpi" type="button" onclick="buka('tugas')" aria-label="Buka tugas yang perlu dinilai">
      <span class="el-kpi-ic" style="background:#fef3c7;color:#b45309"><i class="fa-solid fa-file-circle-check"></i></span>
      <span><span class="el-kpi-val" id="sTugas"><span class="el-skeleton" aria-label="Memuat"></span></span><span class="el-kpi-lbl">Tugas menunggu nilai</span></span>
    </button>
    <button class="el-kpi" type="button" onclick="buka('ujian')" aria-label="Buka ujian aktif">
      <span class="el-kpi-ic" style="background:#ede9fe;color:#6d28d9"><i class="fa-solid fa-clipboard-question"></i></span>
      <span><span class="el-kpi-val" id="sUjian"><span class="el-skeleton" aria-label="Memuat"></span></span><span class="el-kpi-lbl">Ujian dan kuis aktif</span></span>
    </button>
  </section>

  <section class="el-workspace">
    <div class="el-toolbar">
      <label class="el-search" for="cariFitur">
        <i class="fa-solid fa-magnifying-glass"></i>
        <input id="cariFitur" type="search" autocomplete="off" placeholder="Cari materi, tugas, ujian, diskusi..." oninput="filterFitur(this.value)">
      </label>
      <button class="ais2-btn ais2-btn-outline" type="button" onclick="buka('laporan')"><i class="fa-solid fa-file-export"></i> Laporan</button>
      <button class="ais2-btn ais2-btn-primary" type="button" onclick="buka('buat_pertemuan')"><i class="fa-solid fa-plus"></i> Tambah Aktivitas</button>
    </div>

    <nav class="el-nav" aria-label="Navigasi e-Learning">
      <span class="active" aria-current="page"><i class="fa-solid fa-gauge-high"></i> Ringkasan</span>
      <button type="button" onclick="buka('index')"><i class="fa-solid fa-book-open"></i> Mata Kuliah</button>
      <button type="button" onclick="buka('linimasa')"><i class="fa-solid fa-timeline"></i> Linimasa</button>
      <button type="button" onclick="buka('materi')"><i class="fa-regular fa-file-lines"></i> Materi &amp; Modul</button>
      <button type="button" onclick="buka('tugas')"><i class="fa-solid fa-list-check"></i> Tugas</button>
      <button type="button" onclick="buka('ujian')"><i class="fa-solid fa-clipboard-question"></i> Ujian &amp; Kuis</button>
      <button type="button" onclick="buka('diskusi')"><i class="fa-regular fa-comments"></i> Diskusi</button>
      <button type="button" onclick="buka('kalender')"><i class="fa-regular fa-calendar"></i> Kalender</button>
      <button type="button" onclick="buka('daftar_peserta_absen')"><i class="fa-solid fa-user-check"></i> Presensi</button>
      <button type="button" onclick="buka('laporan')"><i class="fa-solid fa-chart-line"></i> Nilai &amp; Analitik</button>
      <button type="button" onclick="buka('bimbingan')"><i class="fa-solid fa-user-graduate"></i> Bimbingan</button>
    </nav>

    <div class="el-content">
      <div class="el-section-head">
        <div><h3>Pusat aktivitas pembelajaran</h3><p>Pilih ruang kerja sesuai kegiatan yang akan dilakukan.</p></div>
      </div>
      <div class="el-action-grid" id="fiturGrid">
        <button class="el-action" type="button" data-keywords="kelas mata kuliah pertemuan linimasa" onclick="buka('index')">
          <span class="el-action-top"><span class="el-action-ic" style="background:linear-gradient(135deg,#2563eb,#0e7490)"><i class="fa-solid fa-book-open-reader"></i></span><i class="fa-solid fa-chevron-right el-action-arrow"></i></span>
          <h4>Mata Kuliah &amp; Pertemuan</h4><p>Buka kelas, linimasa, ringkasan, dan perjalanan pembelajaran.</p>
        </button>
        <button class="el-action" type="button" data-keywords="materi modul dokumen pdf video audio referensi" onclick="buka('materi')">
          <span class="el-action-top"><span class="el-action-ic" style="background:linear-gradient(135deg,#059669,#0d9488)"><i class="fa-regular fa-file-lines"></i></span><i class="fa-solid fa-chevron-right el-action-arrow"></i></span>
          <h4>Materi &amp; Modul</h4><p>Kelola dokumen, video, audio, tautan, dan bahan ajar.</p>
        </button>
        <button class="el-action" type="button" data-keywords="tugas individu kelompok kumpul penilaian rubrik" onclick="buka('tugas')">
          <span class="el-action-top"><span class="el-action-ic" style="background:linear-gradient(135deg,#f59e0b,#ea580c)"><i class="fa-solid fa-file-pen"></i></span><i class="fa-solid fa-chevron-right el-action-arrow"></i></span>
          <h4>Tugas &amp; Penilaian</h4><p>Atur tugas, pengumpulan, revisi, umpan balik, dan nilai.</p>
        </button>
        <button class="el-action" type="button" data-keywords="ujian kuis bank soal monitoring hasil" onclick="buka('ujian')">
          <span class="el-action-top"><span class="el-action-ic" style="background:linear-gradient(135deg,#7c3aed,#4f46e5)"><i class="fa-solid fa-clipboard-question"></i></span><i class="fa-solid fa-chevron-right el-action-arrow"></i></span>
          <h4>Ujian &amp; Kuis</h4><p>Siapkan soal, jadwal, pengerjaan, monitoring, dan hasil ujian.</p>
        </button>
        <button class="el-action" type="button" data-keywords="diskusi forum kolaborasi topik balasan" onclick="buka('diskusi')">
          <span class="el-action-top"><span class="el-action-ic" style="background:linear-gradient(135deg,#0891b2,#2563eb)"><i class="fa-regular fa-comments"></i></span><i class="fa-solid fa-chevron-right el-action-arrow"></i></span>
          <h4>Diskusi &amp; Kolaborasi</h4><p>Kelola forum kelas, topik, balasan, dan pengumuman.</p>
        </button>
        <button class="el-action" type="button" data-keywords="kalender agenda jadwal presensi hadir izin sakit alpa" onclick="buka('kalender')">
          <span class="el-action-top"><span class="el-action-ic" style="background:linear-gradient(135deg,#e11d48,#be185d)"><i class="fa-regular fa-calendar-check"></i></span><i class="fa-solid fa-chevron-right el-action-arrow"></i></span>
          <h4>Kalender &amp; Presensi</h4><p>Lihat agenda, jadwal kelas, deadline, dan rekap kehadiran.</p>
        </button>
        <button class="el-action" type="button" data-keywords="nilai gradebook analitik obe laporan capaian" onclick="buka('laporan')">
          <span class="el-action-top"><span class="el-action-ic" style="background:linear-gradient(135deg,#16a34a,#15803d)"><i class="fa-solid fa-chart-column"></i></span><i class="fa-solid fa-chevron-right el-action-arrow"></i></span>
          <h4>Nilai, Analitik &amp; OBE</h4><p>Buka laporan nilai, ketercapaian OBE, dan analitik pembelajaran.</p>
        </button>
        <button class="el-action" type="button" data-keywords="bimbingan skripsi tugas akhir portofolio" onclick="buka('bimbingan')">
          <span class="el-action-top"><span class="el-action-ic" style="background:linear-gradient(135deg,#9333ea,#6d28d9)"><i class="fa-solid fa-user-graduate"></i></span><i class="fa-solid fa-chevron-right el-action-arrow"></i></span>
          <h4>Bimbingan &amp; Portofolio</h4><p>Akses bimbingan tugas akhir dan portofolio pembelajaran.</p>
        </button>
      </div>
      <div class="el-empty" id="fiturEmpty"><i class="fa-regular fa-folder-open"></i>Tidak ada fitur yang cocok. Coba kata kunci lain.</div>

      <div class="el-flow" aria-label="Alur cepat pembelajaran">
        <button type="button" onclick="buka('buat_pertemuan')"><span class="el-flow-num">1</span><span><strong>Rancang pertemuan</strong><small>Topik dan capaian</small></span></button>
        <button type="button" onclick="buka('materi')"><span class="el-flow-num">2</span><span><strong>Terbitkan materi</strong><small>Dokumen dan media</small></span></button>
        <button type="button" onclick="buka('tugas')"><span class="el-flow-num">3</span><span><strong>Berikan aktivitas</strong><small>Tugas, kuis, diskusi</small></span></button>
        <button type="button" onclick="buka('laporan')"><span class="el-flow-num">4</span><span><strong>Pantau capaian</strong><small>Nilai dan analitik</small></span></button>
      </div>
    </div>
  </section>
</main>

<script>
var EL_ROUTES = {
  index:'index', linimasa:'linimasa', materi:'materi', tugas:'tugas', ujian:'ujian',
  diskusi:'diskusi', kalender:'kalender', laporan:'laporan', buat_pertemuan:'buat_pertemuan',
  daftar_peserta:'daftar_peserta', daftar_peserta_absen:'daftar_peserta_absen'
};

function buka(kunci) {
  var label = String(kunci || '').replace(/_/g, ' ');
  var url;
  if (kunci === 'bimbingan') {
    url = ROOT + '/baru?p=pagesmasterbkdbimbinganskripsizul';
  } else {
    var halaman = EL_ROUTES[kunci] || 'index';
    url = ROOT + '/baru?p=elearning&s=' + encodeURIComponent(halaman);
  }
  if (window.parent && window.parent !== window && typeof window.parent.bukaModul === 'function') {
    window.parent.bukaModul('e-Learning - ' + label, url);
  } else {
    window.location.href = url;
  }
}

function isiAngka(id, nilai) {
  var el = document.getElementById(id);
  if (!el) return;
  el.textContent = nilai === 0 ? '0' : (nilai || '\u2014');
}

function muatStatistik() {
  var err = document.getElementById('elError');
  if (err) err.classList.remove('show');
  AIS2.Api.get('elearning','dasbor_stat',{}).then(function(r) {
    if (!r || !r.data) throw new Error('Data ringkasan tidak tersedia');
    isiAngka('sKelas', r.data.kelasAktif);
    isiAngka('sPeserta', r.data.peserta);
    isiAngka('sTugas', r.data.tugasBelumDinilai);
    isiAngka('sUjian', r.data.ujianAktif);
  }).catch(function() {
    isiAngka('sKelas', null); isiAngka('sPeserta', null);
    isiAngka('sTugas', null); isiAngka('sUjian', null);
    if (err) err.classList.add('show');
  });
}

function filterFitur(q) {
  q = String(q || '').toLowerCase().trim();
  var terlihat = 0;
  document.querySelectorAll('#fiturGrid .el-action').forEach(function(kartu) {
    var cocok = !q || (kartu.textContent + ' ' + (kartu.getAttribute('data-keywords') || '')).toLowerCase().indexOf(q) >= 0;
    kartu.style.display = cocok ? '' : 'none';
    if (cocok) terlihat++;
  });
  document.getElementById('fiturEmpty').classList.toggle('show', terlihat === 0);
}

document.addEventListener('DOMContentLoaded', muatStatistik);
</script>
</body>
</html>
