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
private boolean cfgBool(String key, boolean def) {
    String value = cfgText(key, def ? "true" : "false");
    return "true".equalsIgnoreCase(value) || "1".equals(value) || "ya".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
}
private String elHtml(String value) {
    if (value == null) return "";
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
}
%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    response.sendRedirect(request.getContextPath() + "/logoff");
    return;
}
boolean elWorkspaceModern = cfgBool("elearning_workspace_modern_aktif", true);
if (!elWorkspaceModern) {
%><jsp:include page="/WEB-INF/baru/modul/elearning/index_classic.jsp" /><%
    return;
}
boolean elPengajar = tbmuser.ambilDosen() != null || tbmuser.ambilGuru() != null;
boolean elPeserta = tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null;
String elNama = tbmuser.getUserNama() == null ? tbmuser.getUserId() : tbmuser.getUserNama();
String elPeran = elPengajar ? "Dosen / Guru" : (elPeserta ? "Mahasiswa / Siswa / Santri" : "Pengelola e-Learning");
String elJudul = cfgText("elearning_judul_utama", "Ruang Belajar Digital");
String elDeskripsi = cfgText("elearning_deskripsi_utama", "Akses kelas, materi, tugas, ujian, diskusi, presensi, dan analitik dalam satu ruang belajar.");
%>
<div class="elearning-workspace" data-elearning-workspace data-role="<%=elPengajar ? "pengajar" : (elPeserta ? "peserta" : "pengelola")%>">
  <aside class="el-workspace-nav" aria-label="Navigasi e-Learning">
    <div class="el-brand-block">
      <span class="el-brand-mark" aria-hidden="true">eL</span>
      <span><strong>eLearning</strong><small>eCampus · eSchool · ePesantren</small></span>
    </div>
    <div class="el-nav-label">Ruang pembelajaran</div>
    <nav class="nav nav-pills el-module-nav" role="tablist" aria-orientation="vertical">
      <button class="nav-link active" id="Dasbor-tab" data-bs-toggle="pill" data-bs-target="#Dasbor" type="button" role="tab" aria-controls="Dasbor" aria-selected="true" data-el-title="Ringkasan"><i class="fas fa-home" aria-hidden="true"></i><span>Ringkasan</span></button>
      <button class="nav-link" id="Ringkasan-tab" data-bs-toggle="pill" data-bs-target="#Ringkasan" type="button" role="tab" aria-controls="Ringkasan" aria-selected="false" data-el-title="Mata Kuliah / Kelas"><i class="fas fa-book-open" aria-hidden="true"></i><span>Mata Kuliah / Kelas</span></button>
      <button class="nav-link" id="Linimasa-tab" data-bs-toggle="pill" data-bs-target="#Linimasa" type="button" role="tab" aria-controls="Linimasa" aria-selected="false" data-el-title="Linimasa"><i class="fas fa-stream" aria-hidden="true"></i><span>Linimasa</span></button>
      <button class="nav-link" id="Materi-tab" data-bs-toggle="pill" data-bs-target="#Materi" type="button" role="tab" aria-controls="Materi" aria-selected="false" data-el-title="Materi &amp; Modul"><i class="fas fa-file-alt" aria-hidden="true"></i><span>Materi &amp; Modul</span></button>
      <button class="nav-link" id="Tugas-tab" data-bs-toggle="pill" data-bs-target="#Tugas" type="button" role="tab" aria-controls="Tugas" aria-selected="false" data-el-title="Tugas"><i class="fas fa-check-square" aria-hidden="true"></i><span>Tugas</span></button>
      <button class="nav-link" id="Ujian-tab" data-bs-toggle="pill" data-bs-target="#Ujian" type="button" role="tab" aria-controls="Ujian" aria-selected="false" data-el-title="Ujian &amp; Kuis"><i class="fas fa-clipboard-list" aria-hidden="true"></i><span>Ujian &amp; Kuis</span></button>
      <button class="nav-link" id="Diskusi-tab" data-bs-toggle="pill" data-bs-target="#Diskusi" type="button" role="tab" aria-controls="Diskusi" aria-selected="false" data-el-title="Diskusi"><i class="fas fa-comments" aria-hidden="true"></i><span>Diskusi</span></button>
      <button class="nav-link" id="Kalender-tab" data-bs-toggle="pill" data-bs-target="#Kalender" type="button" role="tab" aria-controls="Kalender" aria-selected="false" data-el-title="Kalender &amp; Presensi"><i class="fas fa-calendar-alt" aria-hidden="true"></i><span>Kalender &amp; Presensi</span></button>
      <button class="nav-link" id="Obe-tab" data-bs-toggle="pill" data-bs-target="#Obe" type="button" role="tab" aria-controls="Obe" aria-selected="false" data-el-title="Nilai &amp; Analitik"><i class="fas fa-chart-line" aria-hidden="true"></i><span>Nilai &amp; Analitik</span></button>
      <button class="nav-link" id="Laporan-tab" data-bs-toggle="pill" data-bs-target="#Laporan" type="button" role="tab" aria-controls="Laporan" aria-selected="false" data-el-title="Laporan"><i class="fas fa-file-export" aria-hidden="true"></i><span>Laporan</span></button>
    </nav>
    <div class="el-session-card">
      <small>Ruang aktif</small><strong><%=elHtml(elPeran)%></strong>
      <span><i class="fas fa-circle" aria-hidden="true"></i> Terhubung ke data AIS</span>
    </div>
  </aside>

  <main class="el-workspace-main">
    <header class="el-workspace-head">
      <div>
        <div class="el-breadcrumb"><span>eCampus</span><i class="fas fa-chevron-right" aria-hidden="true"></i><strong id="elCurrentSection">Ringkasan</strong></div>
        <h1><%=elHtml(elJudul)%></h1>
        <p><%=elHtml(elDeskripsi)%></p>
      </div>
      <div class="el-user-chip"><span class="el-user-avatar"><i class="fas fa-user" aria-hidden="true"></i></span><span><strong><%=elHtml(elNama)%></strong><small><%=elHtml(elPeran)%></small></span></div>
    </header>

    <div class="el-mobile-nav-wrap">
      <label for="elMobileSection">Buka bagian</label>
      <select id="elMobileSection" class="form-select" aria-label="Pilih bagian e-Learning">
        <option value="Dasbor">Ringkasan</option><option value="Ringkasan">Mata Kuliah / Kelas</option>
        <option value="Linimasa">Linimasa</option><option value="Materi">Materi &amp; Modul</option>
        <option value="Tugas">Tugas</option><option value="Ujian">Ujian &amp; Kuis</option>
        <option value="Diskusi">Diskusi</option><option value="Kalender">Kalender &amp; Presensi</option>
        <option value="Obe">Nilai &amp; Analitik</option><option value="Laporan">Laporan</option>
      </select>
    </div>

    <section class="el-role-hero" aria-label="Aksi utama e-Learning">
      <div class="el-role-hero-copy">
        <span class="el-role-kicker"><i class="fas <%=elPengajar ? "fa-chalkboard-teacher" : (elPeserta ? "fa-user-graduate" : "fa-cogs")%>" aria-hidden="true"></i> <%=elHtml(elPeran)%></span>
        <h2><%=elPengajar ? "Kelola pembelajaran hari ini" : (elPeserta ? "Lanjutkan perjalanan belajar Anda" : "Pantau operasional pembelajaran")%></h2>
        <p><%=elPengajar ? "Buka kelas, siapkan materi, periksa tugas, dan tindak lanjuti peserta dari satu tempat." : (elPeserta ? "Temukan materi berikutnya, deadline terdekat, ujian, diskusi, serta perkembangan nilai Anda." : "Gunakan ringkasan, katalog kelas, kalender, laporan, dan analitik sesuai hak akses aktif.")%></p>
      </div>
      <div class="el-quick-actions" role="group" aria-label="Aksi cepat">
        <% if (elPengajar) { %>
        <button type="button" data-el-open="Ringkasan"><i class="fas fa-layer-group"></i><span><strong>Kelola kelas</strong><small>Daftar mata kuliah</small></span></button>
        <button type="button" data-el-open="Tugas"><i class="fas fa-clipboard-check"></i><span><strong>Periksa tugas</strong><small>Pengumpulan &amp; nilai</small></span></button>
        <button type="button" data-el-open="Kalender"><i class="fas fa-calendar-check"></i><span><strong>Agenda &amp; presensi</strong><small>Jadwal hari ini</small></span></button>
        <% } else if (elPeserta) { %>
        <button type="button" data-el-open="Materi"><i class="fas fa-play-circle"></i><span><strong>Lanjut belajar</strong><small>Materi &amp; modul</small></span></button>
        <button type="button" data-el-open="Tugas"><i class="fas fa-hourglass-half"></i><span><strong>Lihat deadline</strong><small>Tugas aktif</small></span></button>
        <button type="button" data-el-open="Obe"><i class="fas fa-chart-line"></i><span><strong>Perkembangan</strong><small>Nilai &amp; capaian</small></span></button>
        <% } else { %>
        <button type="button" data-el-open="Dasbor"><i class="fas fa-chart-pie"></i><span><strong>Pantau sistem</strong><small>Ringkasan aktivitas</small></span></button>
        <button type="button" data-el-open="Ringkasan"><i class="fas fa-book-open"></i><span><strong>Katalog kelas</strong><small>Seluruh pembelajaran</small></span></button>
        <button type="button" data-el-open="Laporan"><i class="fas fa-file-export"></i><span><strong>Buka laporan</strong><small>Cetak &amp; ekspor</small></span></button>
        <% } %>
      </div>
    </section>

    <section class="el-course-context" id="elCourseContext" hidden aria-live="polite">
      <div class="el-course-context-main">
        <span class="el-course-icon"><i class="fas fa-book-reader" aria-hidden="true"></i></span>
        <span><small>Workspace mata kuliah / kelas</small><strong id="elCourseTitle">-</strong></span>
      </div>
      <nav aria-label="Bagian workspace mata kuliah">
        <button type="button" data-el-course-open="Ringkasan">Ringkasan</button>
        <button type="button" data-el-course-open="Linimasa">Pertemuan</button>
        <button type="button" data-el-course-open="Materi">Materi</button>
        <button type="button" data-el-course-open="Tugas">Tugas</button>
        <button type="button" data-el-course-open="Ujian">Ujian</button>
        <button type="button" data-el-course-open="Diskusi">Diskusi</button>
        <button type="button" data-el-course-open="Kalender">Presensi</button>
        <button type="button" data-el-course-open="Obe">Nilai / OBE</button>
      </nav>
      <button type="button" class="el-course-close" id="elCourseClose" aria-label="Tutup konteks mata kuliah"><i class="fas fa-times"></i></button>
    </section>

    <section class="el-content-card">
      <div class="el-runtime-error" id="elRuntimeError" hidden role="alert">
        <i class="fas fa-exclamation-triangle" aria-hidden="true"></i>
        <span><strong>Terjadi kendala saat memuat bagian e-Learning.</strong><small id="elRuntimeErrorMessage">Silakan muat ulang atau hubungi administrator.</small></span>
        <button type="button" id="elRuntimeErrorClose" aria-label="Tutup pesan error"><i class="fas fa-times"></i></button>
      </div>
      <div class="tab-content">
        <div class="tab-pane fade show active" id="Dasbor" role="tabpanel" aria-labelledby="Dasbor-tab"><jsp:include page="/WEB-INF/baru/modul/elearning/dasbor.jsp"><jsp:param value="${tbmuser}" name="tbmuser" /></jsp:include></div>
        <div class="tab-pane fade" id="Ringkasan" role="tabpanel" aria-labelledby="Ringkasan-tab"><jsp:include page="/WEB-INF/baru/modul/elearning/ringkasan.jsp"><jsp:param value="${tbmuser}" name="tbmuser" /></jsp:include></div>
        <div class="tab-pane fade" id="Linimasa" role="tabpanel" aria-labelledby="Linimasa-tab"><jsp:include page="/WEB-INF/baru/modul/elearning/linimasa.jsp"><jsp:param value="${tbmuser}" name="tbmuser" /></jsp:include></div>
        <div class="tab-pane fade" id="Materi" role="tabpanel" aria-labelledby="Materi-tab"><jsp:include page="/WEB-INF/baru/modul/elearning/materi.jsp"><jsp:param value="${tbmuser}" name="tbmuser" /></jsp:include></div>
        <div class="tab-pane fade" id="Tugas" role="tabpanel" aria-labelledby="Tugas-tab"><jsp:include page="/WEB-INF/baru/modul/elearning/tugas.jsp"><jsp:param value="${tbmuser}" name="tbmuser" /></jsp:include></div>
        <div class="tab-pane fade" id="Ujian" role="tabpanel" aria-labelledby="Ujian-tab"><jsp:include page="/WEB-INF/baru/modul/elearning/ujian.jsp"><jsp:param value="${tbmuser}" name="tbmuser" /></jsp:include></div>
        <div class="tab-pane fade" id="Diskusi" role="tabpanel" aria-labelledby="Diskusi-tab"><jsp:include page="/WEB-INF/baru/modul/elearning/diskusi.jsp"><jsp:param value="${tbmuser}" name="tbmuser" /></jsp:include></div>
        <div class="tab-pane fade" id="Kalender" role="tabpanel" aria-labelledby="Kalender-tab"><jsp:include page="/WEB-INF/baru/modul/elearning/kalender.jsp"><jsp:param value="${tbmuser}" name="tbmuser" /></jsp:include></div>
        <div class="tab-pane fade" id="Obe" role="tabpanel" aria-labelledby="Obe-tab"><jsp:include page="/WEB-INF/baru/modul/elearning/obe.jsp"><jsp:param value="${tbmuser}" name="tbmuser" /></jsp:include></div>
        <div class="tab-pane fade" id="Laporan" role="tabpanel" aria-labelledby="Laporan-tab"><jsp:include page="/WEB-INF/baru/modul/elearning/laporan.jsp"><jsp:param value="${tbmuser}" name="tbmuser" /></jsp:include></div>
      </div>
    </section>
  </main>
</div>
<script>
(function () {
  var root = document.querySelector('[data-elearning-workspace]');
  if (!root) return;
  var selector = root.querySelector('#elMobileSection');
  var title = root.querySelector('#elCurrentSection');
  var storageKey = 'ais.elearning.section.<%=tbmuser.hakAkses() == null || tbmuser.hakAkses().getRoleId() == null ? "0" : tbmuser.hakAkses().getRoleId()%>';
  function activate(id) {
    var trigger = root.querySelector('[data-bs-target="#' + id + '"]');
    if (!trigger || typeof bootstrap === 'undefined') return;
    bootstrap.Tab.getOrCreateInstance(trigger).show();
  }
  function applyCourseFilter(id, course) {
    if (!course || !course.title) return;
    var pane = root.querySelector('#' + id);
    if (!pane) return;
    var field = pane.querySelector('input[id^="filter-matakuliah-"]');
    if (field && field.value !== course.title) {
      field.value = course.title;
      field.dispatchEvent(new Event('change', { bubbles: true }));
    }
  }
  function showCourse(course) {
    var bar = root.querySelector('#elCourseContext');
    if (!bar) return;
    if (!course || !course.title) { bar.hidden = true; return; }
    root.querySelector('#elCourseTitle').textContent = course.title;
    bar.hidden = false;
  }
  function showRuntimeError(message) {
    var box = root.querySelector('#elRuntimeError');
    if (!box) return;
    root.querySelector('#elRuntimeErrorMessage').textContent = message || 'Silakan muat ulang atau hubungi administrator.';
    box.hidden = false;
  }
  root.querySelectorAll('[data-bs-toggle="pill"]').forEach(function (trigger) {
    trigger.addEventListener('shown.bs.tab', function () {
      var id = trigger.getAttribute('data-bs-target').substring(1);
      var label = trigger.getAttribute('data-el-title') || trigger.textContent.trim();
      title.textContent = label;
      selector.value = id;
      try { sessionStorage.setItem(storageKey, id); } catch (ignore) {}
      if (history.replaceState) history.replaceState(null, '', location.pathname + location.search + '#el-' + id.toLowerCase());
      var course = null;
      try { course = JSON.parse(sessionStorage.getItem(storageKey + '.course') || 'null'); } catch (ignore) {}
      applyCourseFilter(id, course);
    });
  });
  root.querySelectorAll('[data-el-open]').forEach(function (button) {
    button.addEventListener('click', function () { activate(button.getAttribute('data-el-open')); });
  });
  root.querySelectorAll('[data-el-course-open]').forEach(function (button) {
    button.addEventListener('click', function () { activate(button.getAttribute('data-el-course-open')); });
  });
  root.addEventListener('ais:elearning:course', function (event) {
    var course = event.detail || null;
    if (!course || !course.title) return;
    try { sessionStorage.setItem(storageKey + '.course', JSON.stringify(course)); } catch (ignore) {}
    showCourse(course);
    activate(course.open || 'Linimasa');
  });
  root.querySelector('#elCourseClose').addEventListener('click', function () {
    try { sessionStorage.removeItem(storageKey + '.course'); } catch (ignore) {}
    showCourse(null);
  });
  root.querySelector('#elRuntimeErrorClose').addEventListener('click', function () {
    root.querySelector('#elRuntimeError').hidden = true;
  });
  window.addEventListener('unhandledrejection', function (event) {
    var reason = event.reason;
    showRuntimeError(reason && reason.message ? reason.message : String(reason || 'Permintaan data gagal diproses.'));
  });
  window.addEventListener('error', function (event) {
    if (!event || !event.message) return;
    showRuntimeError(event.message);
  });
  selector.addEventListener('change', function () { activate(selector.value); });
  var requested = location.hash.indexOf('#el-') === 0 ? location.hash.substring(4) : '';
  var byHash = null;
  if (requested) {
    root.querySelectorAll('.tab-pane[id]').forEach(function (pane) { if (pane.id.toLowerCase() === requested) byHash = pane; });
  }
  var saved = '';
  try { saved = sessionStorage.getItem(storageKey) || ''; } catch (ignore) {}
  var savedCourse = null;
  try { savedCourse = JSON.parse(sessionStorage.getItem(storageKey + '.course') || 'null'); } catch (ignore) {}
  showCourse(savedCourse);
  activate(byHash ? byHash.id : saved);
})();
</script>
<jsp:include page="/WEB-INF/baru/modul/elearning/_footer_elearning.jsp" />
