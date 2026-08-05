<%@page import="ais.database.model.*"%>
<%@page import="ais.database.model.streaming.*"%>
<%@page import="ais.database.model.file.*"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.common.Common"%>
<%@page import="java.util.List"%>
<%@page import="java.util.TreeMap"%>
<%@page import="java.util.Map"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.sendRedirect(request.getContextPath() + "/logoff"); return;
    }

    String idStr = request.getParameter("pertemuan");
    String defaultTab = request.getParameter("defaultTab");
    String filterStatusParam = request.getParameter("filter_status");
    if (filterStatusParam != null) filterStatusParam = filterStatusParam.trim();
    Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, idStr, true);
    if (pertemuan == null) {
        out.print("<div class='alert alert-danger m-3'>Data pertemuan tidak ditemukan.</div>"); return;
    }

    String rnd = Common.getGeneratedBarCode(7);
    Long pId = pertemuan.getId();
    boolean bolehUbah = pertemuan.bolehUbahAbsenSaja(tbmuser);

    // ── Hitung Statistik Kehadiran ──────────────────────────────────────────
    Map<String, Integer> statuses = pertemuan.hitungStatus();
    int cHadir = (statuses != null && statuses.get("M") != null) ? statuses.get("M") : 0;
    int cIzin  = (statuses != null && statuses.get("I") != null) ? statuses.get("I") : 0;
    int cSakit = (statuses != null && statuses.get("S") != null) ? statuses.get("S") : 0;
    int cAlpa  = (statuses != null && statuses.get("A") != null) ? statuses.get("A") : 0;

    VOPembelajaran vopCount = pertemuan.ambilVOPembelajaran();
    List<Long> mhsIds = (vopCount != null) ? vopCount.ambilMahasiswaById() : new java.util.ArrayList<Long>();
    List<Long> swIds  = (vopCount != null) ? vopCount.ambilSiswaById()     : new java.util.ArrayList<Long>();
    int totalPeserta = (mhsIds != null ? mhsIds.size() : 0) + (swIds != null ? swIds.size() : 0);
    int cBelum = Math.max(0, totalPeserta - cHadir - cIzin - cSakit - cAlpa);

    // ── Donut Chart (CSS conic-gradient) ────────────────────────────────────
    double d1 = totalPeserta > 0 ? (double)cHadir / totalPeserta * 360 : 0;
    double d2 = d1 + (totalPeserta > 0 ? (double)cIzin  / totalPeserta * 360 : 0);
    double d3 = d2 + (totalPeserta > 0 ? (double)cSakit / totalPeserta * 360 : 0);
    double d4 = d3 + (totalPeserta > 0 ? (double)cAlpa  / totalPeserta * 360 : 0);
    String donutCss = totalPeserta > 0
        ? String.format("conic-gradient(#198754 0deg %.0fdeg,#0dcaf0 %.0fdeg %.0fdeg,#ffc107 %.0fdeg %.0fdeg,#dc3545 %.0fdeg %.0fdeg,#adb5bd %.0fdeg 360deg)", d1,d1,d2,d2,d3,d3,d4,d4)
        : "conic-gradient(#dee2e6 0deg 360deg)";

    // ── Hitung Jumlah Tab ───────────────────────────────────────────────────
    Number nFile    = pertemuan.ambilJumlahPertemuanFileContent();
    Number nAudio   = pertemuan.ambilJumlahAudioPertemuan();
    Number nVideo   = pertemuan.ambilJumlahVideoPertemuan();
    Number nDiskusi = pertemuan.ambilJumlahPertemuanPunyaDiskusi();
    int countFile    = (nFile    != null) ? nFile.intValue()    : 0;
    int countAudio   = (nAudio   != null) ? nAudio.intValue()   : 0;
    int countVideo   = (nVideo   != null) ? nVideo.intValue()   : 0;
    int countDiskusi = (nDiskusi != null) ? nDiskusi.intValue() : 0;

    TreeMap<Long, PertemuanPunyaUjian> ujianMap = pertemuan.ambilPertemuanPunyaUjianTotal(tbmuser);
    int countUjian = (ujianMap != null) ? ujianMap.size() : 0;

    int countTugas = 0;
    if (pertemuan.getJudultugas() != null && !pertemuan.getJudultugas().trim().isEmpty()) countTugas++;
    TreeMap<Long, TugasPertemuan> tgsMap = pertemuan.ambilTugasPertemuanTotal();
    if (tgsMap != null) {
        for (TugasPertemuan tp : tgsMap.values())
            if (tp.getJudultugas() != null && !tp.getJudultugas().trim().isEmpty()) countTugas++;
    }
    TreeMap<Long, TugasKelompok> tgkMap = pertemuan.ambilTugasKelompokTotal();
    if (tgkMap != null) {
        for (TugasKelompok tk : tgkMap.values())
            if (tk.getJudultugas() != null && !tk.getJudultugas().trim().isEmpty()) countTugas++;
    }

    // ── Bangun Item IDs untuk Loader ────────────────────────────────────────
    StringBuilder ujianIds = new StringBuilder();
    if (ujianMap != null) {
        for (PertemuanPunyaUjian u : ujianMap.values()) {
            if (ujianIds.length() > 0) ujianIds.append(",");
            ujianIds.append(u.getId());
        }
    }
    StringBuilder materiIds = new StringBuilder();
    TreeMap<Long, PertemuanFileContent> files = pertemuan.ambilPertemuanFileContentTotal();
    if (files != null) {
        for (Long fId : files.keySet()) {
            if (materiIds.length() > 0) materiIds.append(",");
            materiIds.append(fId);
        }
    }
    StringBuilder audioIds = new StringBuilder();
    TreeMap<Long, AudioPertemuan> audios = pertemuan.ambilAudioPertemuanTotal();
    if (audios != null) {
        for (Long aId : audios.keySet()) {
            if (audioIds.length() > 0) audioIds.append(",");
            audioIds.append(aId);
        }
    }
    StringBuilder videoIds = new StringBuilder();
    TreeMap<Long, VideoPertemuan> videos = pertemuan.ambilVideoPertemuanTotal();
    if (videos != null) {
        for (Long vId : videos.keySet()) {
            if (videoIds.length() > 0) videoIds.append(",");
            videoIds.append(vId);
        }
    }

    // ── Hitungan Tombol Aksi ────────────────────────────────────────────────
    int countUploadTA = pertemuan.ambilJumlahTugasFileContent();
    TreeMap<String, String> dAkses = pertemuan.ambilData("akses", null);
    int countAkses = (dAkses != null) ? dAkses.size() : 0;
    int pengajuanCount = pertemuan.ambilJumlahPengajuanIzinTidakMasukPerkuliahan();

    // ── URL ─────────────────────────────────────────────────────────────────
    String baseUrl    = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning";
    String urlLaporan = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=laporan%2Felearning";
    String linkCetak  = urlLaporan + "&s=laporan_absensi&pertemuan=" + pId;
    String linkIzin   = baseUrl + "&s=pengajuan_izin_atau_sakit&pertemuan=" + pId;
    String linkUbah   = baseUrl + "&s=ubah_pertemuan&pertemuan=" + pId;
    String linkDiskusi = baseUrl + "&s=_diskusi&idPertemuan=" + pId;
    String linkTugas  = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning%2Ftugas&s=tugas&pertemuan=" + pId;
    String linkTugasBaru = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning%2Ftugas&s=tugas_baru&pertemuan=" + pId;
%>
<style>
/* ── Presensi Detail <%=rnd%> ─────────────────────────────────────── */
#ptm-wrap-<%=rnd%> .nav-tabs { border-bottom:2px solid #dee2e6; flex-wrap:nowrap; overflow-x:auto; scrollbar-width:none; -webkit-overflow-scrolling:touch; }
#ptm-wrap-<%=rnd%> .nav-tabs::-webkit-scrollbar { display:none; }
#ptm-wrap-<%=rnd%> .nav-tabs .nav-link { white-space:nowrap; font-size:.82rem; font-weight:600; color:#6c757d; border:none; border-bottom:3px solid transparent; padding:.55rem .85rem; }
#ptm-wrap-<%=rnd%> .nav-tabs .nav-link.active { color:#0d6efd; border-bottom-color:#0d6efd; background:transparent; }
#ptm-wrap-<%=rnd%> .nav-tabs .nav-link:hover:not(.active) { color:#0d6efd; border-bottom-color:#cfe2ff; }
.donut-wrap-<%=rnd%> { position:relative; width:110px; height:110px; border-radius:50%;
    background: <%=donutCss%>; flex-shrink:0; }
.donut-hole-<%=rnd%> { position:absolute; top:50%; left:50%; transform:translate(-50%,-50%);
    width:68px; height:68px; border-radius:50%; background:#fff;
    display:flex; flex-direction:column; align-items:center; justify-content:center; }
.stat-row-<%=rnd%> { display:flex; align-items:center; gap:6px; font-size:.8rem; }
.stat-dot-<%=rnd%> { width:10px; height:10px; border-radius:50%; flex-shrink:0; }
@media(max-width:767.98px) {
    #ptm-wrap-<%=rnd%> .col-xl-3, #ptm-wrap-<%=rnd%> .col-xl-6 { padding-left:.4rem; padding-right:.4rem; }
}
</style>

<div id="ptm-wrap-<%=rnd%>" class="px-2 px-md-3 py-2">

  <!-- ── TABS NAV ─────────────────────────────────────────────────────────── -->
  <ul class="nav nav-tabs mb-3" id="ptm-tabs-<%=rnd%>" role="tablist">
    <li class="nav-item"><a class="nav-link active" data-bs-toggle="tab" href="#ptm-kehadiran-<%=rnd%>" role="tab">
        <i class="fas fa-clipboard-check me-1"></i><%=Common.getBahasaConfig("Kehadiran")%></a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#ptm-pembelajaran-<%=rnd%>" role="tab"
        onclick="lazyLoad<%=rnd%>('pembelajaran')">
        <i class="fas fa-book me-1"></i><%=Common.getBahasaConfig("Pembelajaran")%></a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#ptm-materi-<%=rnd%>" role="tab"
        onclick="lazyLoad<%=rnd%>('materi')">
        <i class="fas fa-folder-open me-1"></i><%=Common.getBahasaConfig("Materi")%>
        <% if(countFile > 0){%><span class="badge bg-danger rounded-pill ms-1"><%=countFile%></span><%}%></a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#ptm-tugas-<%=rnd%>" role="tab"
        onclick="lazyLoad<%=rnd%>('tugas')">
        <i class="fas fa-tasks me-1"></i><%=Common.getBahasaConfig("Tugas")%>
        <% if(countTugas > 0){%><span class="badge bg-danger rounded-pill ms-1"><%=countTugas%></span><%}%></a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#ptm-audio-<%=rnd%>" role="tab"
        onclick="lazyLoad<%=rnd%>('audio')">
        <i class="fas fa-music me-1"></i><%=Common.getBahasaConfig("Audio")%>
        <% if(countAudio > 0){%><span class="badge bg-danger rounded-pill ms-1"><%=countAudio%></span><%}%></a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#ptm-video-<%=rnd%>" role="tab"
        onclick="lazyLoad<%=rnd%>('video')">
        <i class="fas fa-film me-1"></i><%=Common.getBahasaConfig("Video")%>
        <% if(countVideo > 0){%><span class="badge bg-danger rounded-pill ms-1"><%=countVideo%></span><%}%></a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#ptm-ujian-<%=rnd%>" role="tab"
        onclick="lazyLoad<%=rnd%>('ujian')">
        <i class="fas fa-file-signature me-1"></i><%=Common.getBahasaConfig("Ujian")%>
        <% if(countUjian > 0){%><span class="badge bg-danger rounded-pill ms-1"><%=countUjian%></span><%}%></a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#ptm-diskusi-<%=rnd%>" role="tab"
        onclick="lazyLoad<%=rnd%>('diskusi')">
        <i class="far fa-comments me-1"></i><%=Common.getBahasaConfig("Diskusi")%>
        <% if(countDiskusi > 0){%><span class="badge bg-danger rounded-pill ms-1"><%=countDiskusi%></span><%}%></a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#ptm-hasil-<%=rnd%>" role="tab"
        onclick="lazyLoad<%=rnd%>('hasil')">
        <i class="fas fa-chart-bar me-1"></i><%=Common.getBahasaConfig("Hasil, Evaluasi, Kuesioner")%></a></li>
  </ul>

  <!-- ── TAB CONTENT ──────────────────────────────────────────────────────── -->
  <div class="tab-content">

    <!-- ═══════════════════════════════════════════════════════════════════════
         TAB KEHADIRAN
         ═══════════════════════════════════════════════════════════════════════ -->
    <div class="tab-pane fade show active" id="ptm-kehadiran-<%=rnd%>" role="tabpanel">
      <div class="row g-3">

        <!-- ── KOLOM KIRI: Info Pertemuan ── -->
        <div class="col-xl-3 col-lg-4">

          <!-- Info Card (AJAX) -->
          <div id="info_pertemuan_<%=pId%>" class="mb-3">
            <div class="card border-0 shadow-sm rounded-4 p-3 text-center">
              <div class="spinner-border spinner-border-sm text-primary"></div>
            </div>
          </div>

          <!-- Tombol Ubah Pertemuan -->
          <% if(bolehUbah) { %>
          <div class="d-grid mb-3">
            <button class="btn btn-outline-primary btn-sm fw-bold rounded-pill"
                onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Ubah Pertemuan")%>', '<%=linkUbah%>&contentModal='+cm+'&afterSave='+encodeURIComponent('doReloadAbsen<%=pId%>();'), '85%', 'hidden', '', cm);">
              <i class="far fa-edit me-1"></i><%=Common.getBahasaConfig("Ubah Pertemuan")%>
            </button>
          </div>
          <% } %>
        </div>

        <!-- ── KOLOM TENGAH: Presensi Kehadiran ── -->
        <div class="col-xl-6 col-lg-8">
          <div class="card border-0 shadow-sm rounded-4">

            <!-- Header -->
            <div class="card-header bg-white border-bottom py-2 px-3">
              <div class="d-flex justify-content-between align-items-center flex-wrap gap-2">
                <span class="fw-bold text-primary">
                  <i class="fas fa-list-alt me-1"></i><%=Common.getBahasaConfig("Presensi Kehadiran")%>
                </span>
                <div class="btn-group btn-group-sm">
                  <% if(bolehUbah){ %>
                  <button class="btn btn-outline-secondary"
                      onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Ubah Pertemuan")%>', '<%=linkUbah%>&contentModal='+cm+'&afterSave='+encodeURIComponent('doReloadAbsen<%=pId%>();'), '80%', 'hidden', '', cm);">
                    <i class="far fa-edit"></i> <%=Common.getBahasaConfig("Ubah")%>
                  </button>
                  <% } %>
                  <button class="btn btn-outline-secondary"
                      onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Cetak Kehadiran")%>', '<%=linkCetak%>&contentModal='+cm, '75%', null, '', cm);">
                    <i class="fas fa-print"></i> <%=Common.getBahasaConfig("Cetak")%>
                  </button>
                  <button class="btn btn-outline-secondary" onclick="doReloadAbsen<%=pId%>();">
                    <i class="fas fa-sync-alt"></i> <%=Common.getBahasaConfig("Refresh")%>
                  </button>
                </div>
              </div>
            </div>

            <!-- Komposisi Kehadiran (Donut) -->
            <div class="card-body border-bottom py-3 px-3">
              <div class="small fw-bold text-dark mb-1">
                <%=Common.getBahasaConfig("Komposisi Kehadiran Pertemuan Ini")%>
              </div>
              <div class="text-muted small mb-3" style="font-size:.78rem;">
                <%=Common.getBahasaConfig("Sekilas berapa peserta yang hadir, izin, sakit, alpa, dan belum diabsen pada pertemuan ini.")%>
              </div>
              <div class="d-flex align-items-center gap-4 flex-wrap">
                <!-- Donut -->
                <div class="donut-wrap-<%=rnd%>">
                  <div class="donut-hole-<%=rnd%>">
                    <span class="fw-bold fs-5 text-dark"><%=totalPeserta%></span>
                    <span class="text-muted" style="font-size:.65rem;">Total</span>
                  </div>
                </div>
                <!-- Legend -->
                <div class="d-flex flex-column gap-1">
                  <div class="stat-row-<%=rnd%>"><div class="stat-dot-<%=rnd%>" style="background:#198754;"></div><span class="text-muted"><%=Common.getBahasaConfig("Hadir")%></span><strong class="ms-auto ps-3"><%=cHadir%> <small class="text-muted">(<%=totalPeserta>0?(int)Math.round((double)cHadir/totalPeserta*100):0%>%)</small></strong></div>
                  <div class="stat-row-<%=rnd%>"><div class="stat-dot-<%=rnd%>" style="background:#0dcaf0;"></div><span class="text-muted"><%=Common.getBahasaConfig("Izin")%></span><strong class="ms-auto ps-3"><%=cIzin%> <small class="text-muted">(<%=totalPeserta>0?(int)Math.round((double)cIzin/totalPeserta*100):0%>%)</small></strong></div>
                  <div class="stat-row-<%=rnd%>"><div class="stat-dot-<%=rnd%>" style="background:#ffc107;"></div><span class="text-muted"><%=Common.getBahasaConfig("Sakit")%></span><strong class="ms-auto ps-3"><%=cSakit%> <small class="text-muted">(<%=totalPeserta>0?(int)Math.round((double)cSakit/totalPeserta*100):0%>%)</small></strong></div>
                  <div class="stat-row-<%=rnd%>"><div class="stat-dot-<%=rnd%>" style="background:#dc3545;"></div><span class="text-muted"><%=Common.getBahasaConfig("Alpa")%></span><strong class="ms-auto ps-3"><%=cAlpa%> <small class="text-muted">(<%=totalPeserta>0?(int)Math.round((double)cAlpa/totalPeserta*100):0%>%)</small></strong></div>
                  <div class="stat-row-<%=rnd%>"><div class="stat-dot-<%=rnd%>" style="background:#adb5bd;"></div><span class="text-muted"><%=Common.getBahasaConfig("Belum absen")%></span><strong class="ms-auto ps-3"><%=cBelum%> <small class="text-muted">(<%=totalPeserta>0?(int)Math.round((double)cBelum/totalPeserta*100):0%>%)</small></strong></div>
                </div>
              </div>
            </div>

            <!-- Action Buttons (Upload TA / Akses / Belum Absen) -->
            <% if(bolehUbah){ %>
            <div class="card-body border-bottom py-2 px-3">
              <div class="row g-2 align-items-center">
                <div class="col-xl-7 col-12">
                  <div class="d-flex gap-2 flex-wrap align-items-center">
                    <button type="button" class="btn btn-outline-success btn-sm position-relative px-3"
                        onclick="loadModalContent('<%=Common.getBahasaConfigJS("Pengumpulan Tugas Mandiri")%>', '<%=linkTugas%>')">
                      <i class="fas fa-upload me-1"></i><%=Common.getBahasaConfig("Upload TA")%>
                      <% if(countUploadTA > 0){ %>
                      <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger border border-light"><%=countUploadTA%></span>
                      <% } %>
                    </button>
                    <button type="button" class="btn btn-outline-primary btn-sm position-relative px-3"
                        data-bs-toggle="collapse" data-bs-target="#aksesLog_<%=rnd%>">
                      <i class="fas fa-globe me-1"></i><%=Common.getBahasaConfig("Akses")%>
                      <% if(countAkses > 0){ %>
                      <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger border border-light"><%=countAkses%></span>
                      <% } %>
                    </button>
                    <button type="button" class="btn btn-outline-warning btn-sm position-relative px-3"
                        onclick="filterStatusAbsen<%=rnd%>('<%=ConstantValues.BELUM_ABSEN != null ? ConstantValues.BELUM_ABSEN.getId() : ""%>')">
                      <i class="fas fa-user-clock me-1"></i><%=Common.getBahasaConfig("Belum Absen")%>
                      <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-secondary border border-light"><%=cBelum%></span>
                    </button>
                    <% if(cBelum > 0) { %>
                    <button type="button" class="btn btn-danger btn-sm fw-bold px-3"
                        onclick="if(confirm('<%=Common.getBahasaConfigJS("Jadikan")%> <%=cBelum%> <%=Common.getBahasaConfigJS("mahasiswa yang belum absen menjadi Alpa?")%>')){jadikanAlpa<%=rnd%>();}">
                      <i class="fas fa-user-times me-1"></i><%=Common.getBahasaConfig("Belum absen jadikan Alpa")%> (<%=cBelum%> <%=Common.getBahasaConfig("org")%>)
                    </button>
                    <% } %>
                  </div>
                  <div class="collapse mt-2" id="aksesLog_<%=rnd%>">
                    <div class="card border-0 bg-light p-2">
                      <div class="fw-bold mb-1 text-primary small"><i class="fas fa-eye me-1"></i><%=Common.getBahasaConfig("Log Akses Pertemuan")%> (<%=countAkses%>)</div>
                      <% if(dAkses != null && !dAkses.isEmpty()) { %>
                      <div style="max-height:150px;overflow-y:auto;">
                        <table class="table table-sm table-hover mb-0" style="font-size:.76rem;">
                          <tbody>
                            <% for(Map.Entry<String, String> ae : dAkses.entrySet()) { %>
                            <tr><td><%=ae.getKey()%></td><td class="text-muted"><%=ae.getValue()%></td></tr>
                            <% } %>
                          </tbody>
                        </table>
                      </div>
                      <% } else { %>
                      <div class="small text-muted text-center py-1"><i class="fas fa-inbox me-1"></i><%=Common.getBahasaConfig("Belum ada akses")%></div>
                      <% } %>
                    </div>
                  </div>
                </div>
                <div class="col-xl-5 col-12">
                  <div class="input-group input-group-sm shadow-sm">
                    <select class="form-select bg-white" id="filterStatus_<%=pId%>" style="max-width:125px;" onchange="doReloadAbsen<%=pId%>()">
                      <option value="" <%=(filterStatusParam==null||filterStatusParam.isEmpty())?"selected":""%>><%=Common.getBahasaConfig("Semua Status")%></option>
                      <option value="<%=ConstantValues.MASUK.getId()%>" <%=String.valueOf(ConstantValues.MASUK.getId()).equals(filterStatusParam)?"selected":""%>><%=Common.getBahasaConfig("Hadir")%></option>
                      <option value="<%=ConstantValues.IZIN.getId()%>" <%=String.valueOf(ConstantValues.IZIN.getId()).equals(filterStatusParam)?"selected":""%>><%=Common.getBahasaConfig("Izin")%></option>
                      <option value="<%=ConstantValues.SAKIT.getId()%>" <%=String.valueOf(ConstantValues.SAKIT.getId()).equals(filterStatusParam)?"selected":""%>><%=Common.getBahasaConfig("Sakit")%></option>
                      <option value="<%=ConstantValues.TIDAK_ADA_ALASAN.getId()%>" <%=String.valueOf(ConstantValues.TIDAK_ADA_ALASAN.getId()).equals(filterStatusParam)?"selected":""%>><%=Common.getBahasaConfig("Tidak Ada Alasan")%></option>
                      <option value="<%=ConstantValues.BELUM_ABSEN.getId()%>" <%=String.valueOf(ConstantValues.BELUM_ABSEN.getId()).equals(filterStatusParam)?"selected":""%>><%=Common.getBahasaConfig("Belum Diabsen")%></option>
                    </select>
                    <input type="text" class="form-control" id="cariText_<%=pId%>" placeholder="<%=Common.getBahasaConfig("Cari")%>..." onkeyup="if(event.keyCode===13){doReloadAbsen<%=pId%>()}">
                    <button class="btn btn-primary" onclick="doReloadAbsen<%=pId%>()"><i class="fas fa-search"></i></button>
                  </div>
                </div>
              </div>
            </div>
            <% } %>

            <!-- Daftar Mahasiswa (AJAX) -->
            <div id="daftar_mahasiswa_<%=pId%>" class="card-body p-2">
              <div class="text-center py-4">
                <div class="spinner-border text-secondary spinner-border-sm"></div>
                <p class="text-muted small mt-2"><%=Common.getBahasaConfig("Memuat daftar hadir")%>...</p>
              </div>
            </div>
          </div>
        </div>

        <!-- ── KOLOM KANAN: Izin/Sakit + Dosen + Konfirmasi ── -->
        <div class="col-xl-3 col-lg-12">

          <!-- Pengajuan Izin atau Sakit -->
          <div class="card border-0 shadow-sm rounded-4 mb-3">
            <div class="card-header bg-white border-bottom py-2 px-3">
              <div class="d-flex justify-content-between align-items-center">
                <span class="fw-bold text-primary small">
                  <i class="fas fa-user-injured me-1"></i><%=Common.getBahasaConfig("Pengajuan Izin atau Sakit")%>
                </span>
                <% if(pengajuanCount > 0) { %>
                <span class="badge bg-warning text-dark rounded-pill"><%=pengajuanCount%></span>
                <% } %>
              </div>
            </div>
            <div class="card-body p-2">
              <div class="d-grid mb-2">
                <button class="btn btn-outline-secondary btn-sm fw-bold rounded-pill"
                    onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Ajukan Izin atau Sakit")%>', '<%=linkIzin%>&contentModal='+cm, '800px', true, 'saveData_'+cm+'();doReloadAbsen<%=pId%>();', cm);">
                  <i class="fas fa-external-link-alt me-1"></i><%=Common.getBahasaConfig("Ajukan Izin atau Sakit")%>
                </button>
              </div>
              <!-- Tabel Pengajuan: header -->
              <% if(pengajuanCount > 0) { %>
              <div class="table-responsive" style="max-height:220px;overflow-y:auto;">
                <table class="table table-sm table-hover mb-0" style="font-size:.78rem;">
                  <thead class="table-light sticky-top">
                    <tr>
                      <th style="width:40px;"><%=Common.getBahasaConfig("OK")%></th>
                      <th><%=Common.getBahasaConfig("Mahasiswa")%></th>
                      <th><%=Common.getBahasaConfig("Keterangan")%></th>
                    </tr>
                  </thead>
                  <tbody id="tabel_izin_<%=pId%>">
                    <tr><td colspan="3" class="text-center"><div class="spinner-border spinner-border-sm text-secondary"></div></td></tr>
                  </tbody>
                </table>
              </div>
              <% } else { %>
              <div id="info_izin_<%=pId%>" class="small text-muted text-center py-2">
                <i class="fas fa-inbox me-1"></i><%=Common.getBahasaConfig("Belum ada pengajuan")%>
              </div>
              <% } %>
            </div>
          </div>

          <!-- Konfirmasi Kehadiran Dosen -->
          <div class="card border-0 shadow-sm rounded-4 mb-3">
            <div class="card-header bg-primary text-white py-2 px-3 small fw-bold">
              <i class="fas fa-user-check me-1"></i><%=Common.getBahasaConfig("Konfirmasi kehadiran dosen oleh perwakilan kelas")%>
            </div>
            <div id="daftar_dosen_<%=pId%>" class="card-body p-2">
              <div class="text-center text-muted small py-2"><div class="spinner-border spinner-border-sm"></div></div>
            </div>
          </div>

          <!-- Konfirmasi Kesesuaian RPS -->
          <div class="card border-0 shadow-sm rounded-4 mb-3">
            <div class="card-header bg-primary text-white py-2 px-3 small fw-bold">
              <i class="fas fa-book-reader me-1"></i><%=Common.getBahasaConfig("Konfirmasi kesesuaian dengan RPS oleh perwakilan kelas")%>
            </div>
            <div id="konfirmasi_rps_<%=pId%>" class="card-body p-2 small text-muted">
              <div class="alert alert-info py-2 mb-0 small">
                <%=Common.getBahasaConfig("Belum ada konfirmasi dari perwakilan kelas apakah pertemuan ini sesuai dengan RPS")%>
              </div>
            </div>
          </div>

        </div><!-- /col kanan -->
      </div><!-- /row kehadiran -->
    </div><!-- /tab kehadiran -->

    <!-- ═══════════════════════════════════════════════════════════════════════
         TAB PEMBELAJARAN
         ═══════════════════════════════════════════════════════════════════════ -->
    <div class="tab-pane fade" id="ptm-pembelajaran-<%=rnd%>" role="tabpanel">
      <div id="ptm-content-pembelajaran-<%=rnd%>" class="py-3">
        <div class="text-center text-muted py-5"><div class="spinner-border text-primary"></div></div>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════════════════════════════
         TAB MATERI
         ═══════════════════════════════════════════════════════════════════════ -->
    <div class="tab-pane fade" id="ptm-materi-<%=rnd%>" role="tabpanel">
      <div id="ptm-content-materi-<%=rnd%>" class="py-3">
        <% if(countFile > 0){ %>
        <div class="text-center text-muted py-5"><div class="spinner-border text-primary"></div></div>
        <% } else { %>
        <div class="text-center text-muted py-5"><i class="fas fa-folder-open fa-2x mb-2"></i><p><%=Common.getBahasaConfig("Belum ada materi untuk pertemuan ini")%>.</p></div>
        <% } %>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════════════════════════════
         TAB TUGAS
         ═══════════════════════════════════════════════════════════════════════ -->
    <div class="tab-pane fade" id="ptm-tugas-<%=rnd%>" role="tabpanel">
      <div id="ptm-content-tugas-<%=rnd%>" class="py-3">
        <% if(countTugas > 0){ %>
        <div class="text-center text-muted py-5"><div class="spinner-border text-primary"></div></div>
        <% } else { %>
        <div class="text-center text-muted py-5"><i class="fas fa-tasks fa-2x mb-2"></i><p><%=Common.getBahasaConfig("Belum ada tugas untuk pertemuan ini")%>.</p>
        <% if(bolehUbah){ %>
        <button class="btn btn-outline-success btn-sm fw-bold rounded-pill"
            onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Buat Tugas Baru")%>', '<%=linkTugasBaru%>&jenis=Pertemuan&contentModal='+cm, '75%', true, 'simpanDataHelper_'+cm+'();', cm);">
          <i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Buat Tugas Baru")%>
        </button>
        <% } %></div>
        <% } %>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════════════════════════════
         TAB AUDIO
         ═══════════════════════════════════════════════════════════════════════ -->
    <div class="tab-pane fade" id="ptm-audio-<%=rnd%>" role="tabpanel">
      <div id="ptm-content-audio-<%=rnd%>" class="py-3">
        <% if(countAudio > 0){ %>
        <div class="text-center text-muted py-5"><div class="spinner-border text-primary"></div></div>
        <% } else { %>
        <div class="text-center text-muted py-5"><i class="fas fa-music fa-2x mb-2"></i><p><%=Common.getBahasaConfig("Belum ada audio untuk pertemuan ini")%>.</p></div>
        <% } %>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════════════════════════════
         TAB VIDEO
         ═══════════════════════════════════════════════════════════════════════ -->
    <div class="tab-pane fade" id="ptm-video-<%=rnd%>" role="tabpanel">
      <div id="ptm-content-video-<%=rnd%>" class="py-3">
        <% if(countVideo > 0){ %>
        <div class="text-center text-muted py-5"><div class="spinner-border text-primary"></div></div>
        <% } else { %>
        <div class="text-center text-muted py-5"><i class="fas fa-film fa-2x mb-2"></i><p><%=Common.getBahasaConfig("Belum ada video untuk pertemuan ini")%>.</p></div>
        <% } %>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════════════════════════════
         TAB UJIAN
         ═══════════════════════════════════════════════════════════════════════ -->
    <div class="tab-pane fade" id="ptm-ujian-<%=rnd%>" role="tabpanel">
      <div id="ptm-content-ujian-<%=rnd%>" class="py-3">
        <% if(countUjian > 0){ %>
        <div class="text-center text-muted py-5"><div class="spinner-border text-primary"></div></div>
        <% } else { %>
        <div class="text-center text-muted py-5"><i class="fas fa-file-signature fa-2x mb-2"></i><p><%=Common.getBahasaConfig("Belum ada ujian untuk pertemuan ini")%>.</p></div>
        <% } %>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════════════════════════════
         TAB DISKUSI
         ═══════════════════════════════════════════════════════════════════════ -->
    <div class="tab-pane fade" id="ptm-diskusi-<%=rnd%>" role="tabpanel">
      <div id="ptm-content-diskusi-<%=rnd%>" class="py-3">
        <div class="text-center text-muted py-5"><div class="spinner-border text-primary"></div></div>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════════════════════════════
         TAB HASIL, EVALUASI, KUESIONER
         ═══════════════════════════════════════════════════════════════════════ -->
    <div class="tab-pane fade" id="ptm-hasil-<%=rnd%>" role="tabpanel">
      <div id="ptm-content-hasil-<%=rnd%>" class="py-3">
        <div class="text-center text-muted py-5"><div class="spinner-border text-primary"></div></div>
      </div>
    </div>

  </div><!-- /tab-content -->
</div><!-- /ptm-wrap -->

<script>
var ptmLoaded<%=rnd%> = {};

/* Reload data kehadiran */
async function reloadAbsen<%=pId%>(withDelay) {
    if (withDelay) { setTimeout(function(){ doReloadAbsen<%=pId%>(); }, 1000); }
    else { doReloadAbsen<%=pId%>(); }
}

async function doReloadAbsen<%=pId%>() {
    var base = "<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning&s=";
    var pp   = "&pertemuan=<%=pId%>";
    var elmCari   = document.getElementById("cariText_<%=pId%>");
    var elmStatus = document.getElementById("filterStatus_<%=pId%>");
    var sp = "";
    if (elmCari   && elmCari.value   !== "") sp += "&cari="          + encodeURIComponent(elmCari.value);
    if (elmStatus && elmStatus.value !== "") sp += "&filter_status=" + encodeURIComponent(elmStatus.value);
    try {
        var r1 = fetch(base + "_daftar_peserta_absen_reload_mahasiswa" + pp + sp);
        var r2 = fetch(base + "_daftar_peserta_absen_reload_dosen"     + pp);
        var r3 = fetch(base + "_info_kehadiran_pertemuan"              + pp);
        var r4 = fetch(base + "_daftar_peserta_absen_izin_dan_sakit"  + pp);
        var res = await Promise.all([r1, r2, r3, r4]);
        var htmlMhs  = await res[0].text();
        var htmlDsn  = await res[1].text();
        var htmlInfo = await res[2].text();
        var htmlIzin = await res[3].text();
        document.getElementById("daftar_mahasiswa_<%=pId%>").innerHTML = htmlMhs;
        document.getElementById("daftar_dosen_<%=pId%>").innerHTML     = htmlDsn;
        document.getElementById("info_pertemuan_<%=pId%>").innerHTML   = htmlInfo;
        /* Pengajuan: render as table rows or fallback div */
        var tblIzin = document.getElementById("tabel_izin_<%=pId%>");
        var divIzin = document.getElementById("info_izin_<%=pId%>");
        if (tblIzin) tblIzin.innerHTML = buildIzinRows<%=rnd%>(htmlIzin);
        if (divIzin) divIzin.innerHTML = htmlIzin;
    } catch(e) { console.error("Gagal memuat data presensi:", e); }
}

/* Bangun baris tabel izin sederhana dari HTML fragment */
function buildIzinRows<%=rnd%>(html) {
    /* Ambil nama & status dari HTML card-based response */
    var div = document.createElement("div");
    div.innerHTML = html;
    var cards = div.querySelectorAll(".card");
    if (!cards || cards.length === 0) return '<tr><td colspan="3" class="text-muted text-center small py-2">Belum ada pengajuan</td></tr>';
    var rows = "";
    cards.forEach(function(c) {
        var nama  = c.querySelector("h6")   ? c.querySelector("h6").textContent.trim()   : "-";
        var nim   = c.querySelector(".badge.bg-white") ? c.querySelector(".badge.bg-white").textContent.trim() : "";
        var ket   = c.querySelector(".fst-italic") ? c.querySelector(".fst-italic").textContent.trim() : "-";
        var ok    = c.querySelector(".bg-success") ? "<i class='fas fa-check text-success'></i>" : "<i class='fas fa-hourglass-half text-secondary'></i>";
        rows += "<tr><td class='text-center'>" + ok + "</td><td><div class='fw-semibold' style='font-size:.78rem;'>" + nama + "</div><div class='text-muted' style='font-size:.72rem;'>" + nim + "</div></td><td style='font-size:.75rem;'>" + ket + "</td></tr>";
    });
    return rows;
}

/* Filter status dengan pilih dropdown */
function filterStatusAbsen<%=rnd%>(val) {
    var sel = document.getElementById("filterStatus_<%=pId%>");
    if (sel) { sel.value = val; }
    doReloadAbsen<%=pId%>();
}

/* Jadikan Alpa semua yang belum absen */
async function jadikanAlpa<%=rnd%>() {
    try {
        var url = "<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning&s=jadikan_alpa_belum_absen&pertemuan=<%=pId%>";
        var res = await fetch(url);
        await res.text();
        doReloadAbsen<%=pId%>();
    } catch(e) { console.error(e); }
}

/* Lazy-load tab content */
var _lazyUrls<%=rnd%> = {
    "pembelajaran": "<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning&s=_info_kehadiran_pertemuan&pertemuan=<%=pId%>",
    "materi":       "<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning&s=load_materi&item=<%=materiIds.toString()%>",
    "audio":        "<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning&s=load_audio&item=<%=audioIds.toString()%>",
    "video":        "<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning&s=load_video&item=<%=videoIds.toString()%>",
    "ujian":        "<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning&s=load_ujian&item=<%=ujianIds.toString()%>",
    "tugas":        "<%=linkTugas%>",
    "diskusi":      "<%=linkDiskusi%>",
    "hasil":        "<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning&s=load_ringkasan&pertemuan=<%=pId%>"
};

async function lazyLoad<%=rnd%>(key) {
    if (ptmLoaded<%=rnd%>[key]) return;
    ptmLoaded<%=rnd%>[key] = true;
    var el = document.getElementById("ptm-content-" + key + "-<%=rnd%>");
    if (!el) return;
    var url = _lazyUrls<%=rnd%>[key];
    if (!url) { el.innerHTML = '<div class="text-muted text-center py-4">N/A</div>'; return; }
    try {
        var r = await fetch(url);
        el.innerHTML = await r.text();
    } catch(e) {
        el.innerHTML = '<div class="alert alert-danger py-2 small">' + e.message + '</div>';
    }
}

/* Inisialisasi */
doReloadAbsen<%=pId%>();

<% if (defaultTab != null && !defaultTab.trim().isEmpty()) { %>
/* Auto-aktifkan tab dari parameter defaultTab */
setTimeout(function() {
    var tabLink = document.querySelector('#ptm-tabs-<%=rnd%> a[href="#ptm-<%=defaultTab.replaceAll("[^a-zA-Z0-9_-]","")%>-<%=rnd%>"]');
    if (tabLink) { tabLink.click(); }
}, 150);
<% } %>
</script>
