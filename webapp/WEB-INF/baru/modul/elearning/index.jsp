<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%!
private String cfgText(String key, String def) {
    try {
        Konfigurasi k = Common.getKonfigurasi(key, def);
        if (k == null || k.getNilai() == null || k.getNilai().trim().length() == 0) return def;
        return k.getNilai().trim();
    } catch (Throwable t) { return def; }
}
%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    response.sendRedirect(request.getContextPath() + "/logoff");
    return;
}
String judul = cfgText("elearning_judul_utama", "Ruang Belajar Digital");
String deskripsi = cfgText("elearning_deskripsi_utama", "Akses jadwal, ringkasan pertemuan, tugas, ujian, materi, dan kalender belajar dalam satu halaman yang mudah dipantau.");
String tabLinimasa = cfgText("elearning_tab_linimasa", "Linimasa");
String descLinimasa = cfgText("elearning_desc_linimasa", "Urutan kegiatan belajar terbaru agar pengguna tahu apa yang perlu dibuka lebih dulu.");
String tabRingkasan = cfgText("elearning_tab_ringkasan", "Ringkasan");
String descRingkasan = cfgText("elearning_desc_ringkasan", "Ikhtisar perkuliahan, kehadiran, materi, dan aktivitas belajar yang sedang berjalan.");
String tabUjian = cfgText("elearning_tab_ujian", "Ujian");
String descUjian = cfgText("elearning_desc_ujian", "Daftar ujian yang tersedia, status pengerjaan, dan hasil yang dapat dilihat sesuai hak akses.");
String tabTugas = cfgText("elearning_tab_tugas", "Tugas");
String descTugas = cfgText("elearning_desc_tugas", "Tugas individu atau kelompok yang perlu dikerjakan, dikumpulkan, dan dipantau nilainya.");
String tabMateri = cfgText("elearning_tab_materi", "Materi");
String descMateri = cfgText("elearning_desc_materi", "Bahan ajar, file, audio, video, dan referensi belajar yang dibagikan pengajar.");
String tabKalender = cfgText("elearning_tab_kalender", "Kalender");
String descKalender = cfgText("elearning_desc_kalender", "Jadwal belajar, pertemuan, ujian, dan agenda akademik yang tersusun berdasarkan waktu.");
String tabDasbor = cfgText("elearning_tab_dasbor", "Dasbor");
String descDasbor = cfgText("elearning_desc_dasbor", "Ringkasan aktivitas belajar: pertemuan, materi, tugas, ujian, video, dan audio per semester.");
String tabObe = cfgText("elearning_tab_obe", "OBE");
String descObe = cfgText("elearning_desc_obe", "Capaian Pembelajaran Lulusan (CPL), CPMK, dan status RPS OBE per tahun akademik dan semester.");
String tabDiskusi = cfgText("elearning_tab_diskusi", "Diskusi");
String descDiskusi = cfgText("elearning_desc_diskusi", "Forum tanya-jawab lintas pertemuan: lihat dan ikuti diskusi dari semua kelas yang Anda ikuti.");
String tabLaporan = cfgText("elearning_tab_laporan", "Laporan");
String descLaporan = cfgText("elearning_desc_laporan", "Cetak daftar nilai, ekspor ujian ke Excel, rekap kehadiran, dan laporan OBE per matakuliah.");
%>
<style>
/* ── E-LEARNING MOBILE STYLES ─────────────────────────────────────────────── */

/* Tab bar: touch-scroll on iOS, hide native scrollbar */
.elearning-card .card-header.scrollbar,
.elearning-card .card-header .scrollbar {
    -webkit-overflow-scrolling: touch;
    overflow-x: auto;
    scrollbar-width: none;
}
.elearning-card .card-header.scrollbar::-webkit-scrollbar { display: none; }

/* Smooth-hide tab descriptions at tablet and below */
@media (max-width: 991.98px) {
    .elearning-tab-desc { display: none !important; }
    .elearning-panel-note { display: none !important; }
}

@media (max-width: 767.98px) {
    /* Compact tab items */
    .elearning-card .nav-link.p-x1 { padding: 0.55rem 0.75rem !important; }
    .elearning-tab-icon { font-size: 1.05rem !important; }
    .elearning-tab-title { font-size: 0.78rem !important; line-height: 1.2 !important; }

    /* Compact card header */
    .elearning-card > .card-header:first-child { padding: 0.75rem 0.85rem !important; }
    .elearning-card > .card-body { padding: 0.6rem !important; }

    /* Sub-page containers: trim vertical padding */
    .container.py-4  { padding-top: 0.75rem !important; padding-bottom: 0.75rem !important; }
    .container-fluid.py-4 { padding-top: 0.75rem !important; padding-bottom: 0.75rem !important; }
    .container.px-xl-5 { padding-left: 0.75rem !important; padding-right: 0.75rem !important; }
    .container-fluid.px-xl-5 { padding-left: 0.75rem !important; padding-right: 0.75rem !important; }

    /* Filter action button-group: full-width on mobile */
    .btn-group.shadow-sm { width: 100% !important; display: flex !important; }
    .btn-group.shadow-sm .btn { flex: 1 1 0; font-size: 0.78rem; padding: 0.45rem 0.4rem !important; }

    /* Tables: always scrollable */
    .table-responsive { overflow-x: auto !important; -webkit-overflow-scrolling: touch; }

    /* Touch-friendly form elements (prevents iOS auto-zoom at <16px) */
    .form-control,
    .form-select,
    select.form-control { font-size: 16px !important; }

    /* Min touch target for buttons */
    .btn { min-height: 38px; }

    /* Reduce large headings in sub-pages */
    h4.fw-bold, h4.font-weight-bold { font-size: 1.05rem !important; }
    h5.fw-bold, h5.font-weight-bold { font-size: 0.9rem !important; }

    /* FullCalendar mobile tweaks */
    .fc-toolbar.fc-header-toolbar { flex-wrap: wrap !important; gap: 0.35rem; }
    .fc-toolbar-chunk { flex: 0 0 auto; }
    .fc-toolbar-title { font-size: 0.95rem !important; }
    .fc-button { padding: 0.25rem 0.5rem !important; font-size: 0.75rem !important; }
    .fc-button-group .fc-button { padding: 0.25rem 0.4rem !important; }
}

@media (max-width: 575.98px) {
    /* Very small: stack card header title vertically */
    .elearning-card > .card-header:first-child .d-flex { flex-direction: column !important; gap: 0.35rem; }

    /* Reduce module icons */
    .elearning-card .bg-primary.rounded-4,
    .elearning-card .bg-primary.rounded { width: 40px !important; height: 40px !important; }

    /* Make stats cards more compact */
    .mini-stat-card-* .card-body { padding: 0.85rem !important; }

    /* Wrap filter checkboxes */
    .chk-kategori-label { font-size: 0.78rem !important; }

    /* Calendar: prefer list view on very small screens (handled in JS) */
}
</style>
<div class="elearning-shell">
  <div class="elearning-card card overflow-hidden mb-3">
    <div class="card-header p-3">
      <div class="d-flex flex-column flex-lg-row align-items-lg-center justify-content-between gap-2">
        <div>
          <div class="text-uppercase small fw-bold text-primary"><%=cfgText("elearning_label_portal", "E-Learning")%></div>
          <h4 class="mb-1 elearning-tab-title"><%=judul%></h4>
          <div class="elearning-tab-desc d-block"><%=deskripsi%></div>
        </div>
      </div>
    </div>
    <div class="card-header p-0 scrollbar">
      <ul class="nav nav-tabs border-0 top-courses-tab flex-nowrap" role="tablist">
        <li class="nav-item" role="presentation"><a class="nav-link p-x1 mb-0 active" role="tab" id="Linimasa-tab" data-bs-toggle="tab" href="#Linimasa" aria-controls="Linimasa" aria-selected="true"><div class="d-flex gap-2 py-1 pe-3"><span class="elearning-tab-icon bi bi-calendar-check"></span><div><h5 class="mb-0 lh-1 elearning-tab-title"><%=tabLinimasa%></h5><div class="elearning-tab-desc"><%=descLinimasa%></div></div></div></a></li>
        <li class="nav-item" role="presentation"><a class="nav-link p-x1 mb-0" role="tab" id="Ringkasan-tab" data-bs-toggle="tab" href="#Ringkasan" aria-controls="Ringkasan" aria-selected="false"><div class="d-flex gap-2 py-1 pe-3"><span class="elearning-tab-icon bi bi-journal-text"></span><div><h5 class="mb-0 lh-1 elearning-tab-title"><%=tabRingkasan%></h5><div class="elearning-tab-desc"><%=descRingkasan%></div></div></div></a></li>
        <li class="nav-item" role="presentation"><a class="nav-link p-x1 mb-0" role="tab" id="Ujian-tab" data-bs-toggle="tab" href="#Ujian" aria-controls="Ujian" aria-selected="false"><div class="d-flex gap-2 py-1 pe-3"><span class="elearning-tab-icon bi bi-clipboard-check"></span><div><h5 class="mb-0 lh-1 elearning-tab-title"><%=tabUjian%></h5><div class="elearning-tab-desc"><%=descUjian%></div></div></div></a></li>
        <li class="nav-item" role="presentation"><a class="nav-link p-x1 mb-0" role="tab" id="Tugas-tab" data-bs-toggle="tab" href="#Tugas" aria-controls="Tugas" aria-selected="false"><div class="d-flex gap-2 py-1 pe-3"><span class="elearning-tab-icon bi bi-pencil-square"></span><div><h5 class="mb-0 lh-1 elearning-tab-title"><%=tabTugas%></h5><div class="elearning-tab-desc"><%=descTugas%></div></div></div></a></li>
        <li class="nav-item" role="presentation"><a class="nav-link p-x1 mb-0" role="tab" id="Materi-tab" data-bs-toggle="tab" href="#Materi" aria-controls="Materi" aria-selected="false"><div class="d-flex gap-2 py-1 pe-3"><span class="elearning-tab-icon bi bi-collection-play"></span><div><h5 class="mb-0 lh-1 elearning-tab-title"><%=tabMateri%></h5><div class="elearning-tab-desc"><%=descMateri%></div></div></div></a></li>
        <li class="nav-item" role="presentation"><a class="nav-link p-x1 mb-0" role="tab" id="Kalender-tab" data-bs-toggle="tab" href="#Kalender" aria-controls="Kalender" aria-selected="false"><div class="d-flex gap-2 py-1 pe-3"><span class="elearning-tab-icon bi bi-calendar3"></span><div><h5 class="mb-0 lh-1 elearning-tab-title"><%=tabKalender%></h5><div class="elearning-tab-desc"><%=descKalender%></div></div></div></a></li>
        <li class="nav-item" role="presentation"><a class="nav-link p-x1 mb-0" role="tab" id="Dasbor-tab" data-bs-toggle="tab" href="#Dasbor" aria-controls="Dasbor" aria-selected="false"><div class="d-flex gap-2 py-1 pe-3"><span class="elearning-tab-icon bi bi-bar-chart-fill"></span><div><h5 class="mb-0 lh-1 elearning-tab-title"><%=tabDasbor%></h5><div class="elearning-tab-desc"><%=descDasbor%></div></div></div></a></li>
        <li class="nav-item" role="presentation"><a class="nav-link p-x1 mb-0" role="tab" id="Obe-tab" data-bs-toggle="tab" href="#Obe" aria-controls="Obe" aria-selected="false"><div class="d-flex gap-2 py-1 pe-3"><span class="elearning-tab-icon bi bi-award-fill"></span><div><h5 class="mb-0 lh-1 elearning-tab-title"><%=tabObe%></h5><div class="elearning-tab-desc"><%=descObe%></div></div></div></a></li>
        <li class="nav-item" role="presentation"><a class="nav-link p-x1 mb-0" role="tab" id="Diskusi-tab" data-bs-toggle="tab" href="#Diskusi" aria-controls="Diskusi" aria-selected="false"><div class="d-flex gap-2 py-1 pe-3"><span class="elearning-tab-icon bi bi-chat-dots-fill"></span><div><h5 class="mb-0 lh-1 elearning-tab-title"><%=tabDiskusi%></h5><div class="elearning-tab-desc"><%=descDiskusi%></div></div></div></a></li>
        <li class="nav-item" role="presentation"><a class="nav-link p-x1 mb-0" role="tab" id="Laporan-tab" data-bs-toggle="tab" href="#Laporan" aria-controls="Laporan" aria-selected="false"><div class="d-flex gap-2 py-1 pe-3"><span class="elearning-tab-icon bi bi-printer-fill"></span><div><h5 class="mb-0 lh-1 elearning-tab-title"><%=tabLaporan%></h5><div class="elearning-tab-desc"><%=descLaporan%></div></div></div></a></li>
      </ul>
    </div>
    <div class="card-body p-3">
      <div class="tab-content">
        <div class="tab-pane active" id="Linimasa" role="tabpanel" aria-labelledby="Linimasa-tab"><span class="elearning-panel-note"><%=descLinimasa%></span><jsp:include page="/WEB-INF/baru/modul/elearning/linimasa.jsp"><jsp:param value="${tbmuser}" name="tbmuser" /></jsp:include></div>
        <div class="tab-pane" id="Ringkasan" role="tabpanel" aria-labelledby="Ringkasan-tab"><span class="elearning-panel-note"><%=descRingkasan%></span><jsp:include page="/WEB-INF/baru/modul/elearning/ringkasan.jsp"><jsp:param value="${tbmuser}" name="tbmuser" /></jsp:include></div>
        <div class="tab-pane" id="Ujian" role="tabpanel" aria-labelledby="Ujian-tab"><span class="elearning-panel-note"><%=descUjian%></span><jsp:include page="/WEB-INF/baru/modul/elearning/ujian.jsp"><jsp:param value="${tbmuser}" name="tbmuser" /></jsp:include></div>
        <div class="tab-pane" id="Tugas" role="tabpanel" aria-labelledby="Tugas-tab"><span class="elearning-panel-note"><%=descTugas%></span><jsp:include page="/WEB-INF/baru/modul/elearning/tugas.jsp"><jsp:param value="${tbmuser}" name="tbmuser" /></jsp:include></div>
        <div class="tab-pane" id="Materi" role="tabpanel" aria-labelledby="Materi-tab"><span class="elearning-panel-note"><%=descMateri%></span><jsp:include page="/WEB-INF/baru/modul/elearning/materi.jsp"><jsp:param value="${tbmuser}" name="tbmuser" /></jsp:include></div>
        <div class="tab-pane" id="Kalender" role="tabpanel" aria-labelledby="Kalender-tab"><span class="elearning-panel-note"><%=descKalender%></span><jsp:include page="/WEB-INF/baru/modul/elearning/kalender.jsp"><jsp:param value="${tbmuser}" name="tbmuser" /></jsp:include></div>
        <div class="tab-pane" id="Dasbor" role="tabpanel" aria-labelledby="Dasbor-tab"><span class="elearning-panel-note"><%=descDasbor%></span><jsp:include page="/WEB-INF/baru/modul/elearning/dasbor.jsp"><jsp:param value="${tbmuser}" name="tbmuser" /></jsp:include></div>
        <div class="tab-pane" id="Obe" role="tabpanel" aria-labelledby="Obe-tab"><span class="elearning-panel-note"><%=descObe%></span><jsp:include page="/WEB-INF/baru/modul/elearning/obe.jsp"><jsp:param value="${tbmuser}" name="tbmuser" /></jsp:include></div>
        <div class="tab-pane" id="Diskusi" role="tabpanel" aria-labelledby="Diskusi-tab"><span class="elearning-panel-note"><%=descDiskusi%></span><jsp:include page="/WEB-INF/baru/modul/elearning/diskusi.jsp"><jsp:param value="${tbmuser}" name="tbmuser" /></jsp:include></div>
        <div class="tab-pane" id="Laporan" role="tabpanel" aria-labelledby="Laporan-tab"><span class="elearning-panel-note"><%=descLaporan%></span><jsp:include page="/WEB-INF/baru/modul/elearning/laporan.jsp"><jsp:param value="${tbmuser}" name="tbmuser" /></jsp:include></div>
      </div>
    </div>
  </div>
</div>
<jsp:include page="/WEB-INF/baru/modul/elearning/_footer_elearning.jsp" />
