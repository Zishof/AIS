<%@page import="ais.common.Common"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="java.util.Date"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    /**
     * _wawancara.jsp — Antarmuka Pengguna Fitur Wawancara PSB/PPDB
     * ==============================================================
     *
     * Halaman antarmuka calon siswa untuk mengikuti proses wawancara
     * dalam alur Penerimaan Siswa Baru (PSB/PPDB). Halaman ini ditampilkan
     * di dalam modal Bootstrap yang dipanggil dari _sukses_login.jsp.
     *
     * FITUR UTAMA
     * -----------
     *  1. Skeleton loading — UI muncul bertahap selama data dimuat.
     *  2. Jadwal wawancara — tanggal & rentang waktu sesi aktif.
     *  3. Countdown / status waktu — hitung mundur, "Sedang Berlangsung", atau selesai.
     *  4. Profil pewawancara — foto, nama, dan nama sesi.
     *  5. Video konferensi — tombol bergradasi untuk Jitsi/Zoom/GMeet/BBB/Skype/WA/Lain.
     *  6. Informasi wawancara — konten HTML dari konfigurasi gelombang.
     *  7. Formulir kesiapan — toggle switch besar + textarea catatan opsional.
     *  8. State pasca-kirim — form dikunci, badge hijau, buka WA otomatis.
     *  9. Gate states — berkas_kurang, no_schedule, error teknis.
     *
     * POLA DESAIN: IIFE + sufiks unik rnd untuk isolasi namespace.
     * RESPONSIVITAS: Bootstrap 5 Grid, mobile-first.
     *
     * @author  Tim Pengembang AIS
     * @version 2026-07-16
     * @see     _wawancara_service.jsp
     * @see     _sukses_login.jsp#bukaModalWawancaraPSB
     */
    String rnd    = "";
    String casisId = request.getParameter("id");
    try {
        rnd = Common.getGeneratedBarCode(7);
    } catch (Exception e) {
        rnd = "wwpsb" + System.currentTimeMillis();
    }
    Date tglServer = WaktuUtil.getDate();
%>

<style>
/* ── Scoped styles agar tidak konflik dengan komponen halaman induk ── */
.ww-card-<%= rnd %> {
    border-radius: 1.375rem;
    border: none;
    box-shadow: 0 8px 32px rgba(0,0,0,.09);
    overflow: hidden;
}
.ww-header-<%= rnd %> {
    background: linear-gradient(135deg, #1e3a5f 0%, #2d6a9f 55%, #4e9fd4 100%);
    color: #fff;
    padding: 1.75rem 2rem;
}
.ww-section-<%= rnd %> {
    border-radius: 1rem;
    border: 1px solid #f1f5f9;
    background: #f8fafc;
    padding: 1.25rem;
}
.ww-switch-<%= rnd %> .form-check-input {
    width: 3.5rem;
    height: 1.875rem;
    cursor: pointer;
}
.ww-switch-<%= rnd %> .form-check-label {
    cursor: pointer;
    line-height: 1.9rem;
    font-weight: 600;
}
.ww-skel-<%= rnd %> {
    background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
    background-size: 400% 100%;
    animation: ww-shimmer-<%= rnd %> 1.5s ease infinite;
    border-radius: .5rem;
}
@keyframes ww-shimmer-<%= rnd %> {
    0%   { background-position: 100% 0 }
    100% { background-position: -100% 0 }
}
.ww-vidBtn-<%= rnd %> {
    border-radius: .875rem;
    font-weight: 700;
    letter-spacing: .02em;
    transition: transform .15s ease, box-shadow .15s ease;
}
.ww-vidBtn-<%= rnd %>:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(0,0,0,.15) !important;
}
.ww-avatar-<%= rnd %> {
    width: 130px;
    height: 130px;
    object-fit: cover;
    border-radius: 50%;
    border: 4px solid #fff;
    box-shadow: 0 4px 20px rgba(0,0,0,.13);
    background: #e2e8f0;
}
.ww-countdown-<%= rnd %> {
    font-size: .78rem;
    font-weight: 700;
    letter-spacing: .04em;
    padding: .45em .9em;
    border-radius: 100px;
}
@media (max-width: 767px) {
    .ww-header-<%= rnd %> { padding: 1.25rem; }
    .ww-avatar-<%= rnd %> { width: 100px; height: 100px; }
}
</style>

<%-- ROOT CONTAINER --%>
<div id="wawRoot<%= rnd %>">

  <div class="ww-card-<%= rnd %> bg-white">

    <%-- Header --%>
    <div class="ww-header-<%= rnd %> d-flex justify-content-between align-items-center">
      <div>
        <h4 class="fw-bold mb-1">
          <i class="fas fa-comments me-2 text-white opacity-90"></i>
          <%= Common.getBahasaConfig("Sesi Wawancara (Interview)") %>
        </h4>
        <p class="mb-0 small" style="opacity:.78">
          <%= Common.getBahasaConfig("Konfirmasi kesiapan Anda sebelum sesi wawancara dimulai.") %>
        </p>
      </div>
      <div class="d-none d-md-block">
        <i class="fas fa-microphone-lines fa-3x" style="opacity:.18"></i>
      </div>
    </div>

    <%-- Skeleton loading --%>
    <div id="wawSkeleton<%= rnd %>" class="p-4 p-md-5">
      <div class="d-flex align-items-center gap-4 mb-5">
        <div class="ww-skel-<%= rnd %> rounded-circle flex-shrink-0" style="width:100px;height:100px"></div>
        <div class="flex-grow-1">
          <div class="ww-skel-<%= rnd %> mb-2" style="height:14px;width:55%"></div>
          <div class="ww-skel-<%= rnd %> mb-2" style="height:24px;width:70%"></div>
          <div class="ww-skel-<%= rnd %>"      style="height:12px;width:35%"></div>
        </div>
      </div>
      <div class="ww-skel-<%= rnd %> mb-3" style="height:80px"></div>
      <div class="ww-skel-<%= rnd %> mb-3" style="height:56px"></div>
      <div class="ww-skel-<%= rnd %>"      style="height:100px"></div>
    </div>

    <%-- Konten utama --%>
    <div id="wawContent<%= rnd %>" class="px-3 px-md-5 pb-5 pt-3" style="display:none">

      <%-- A. Jadwal & countdown --%>
      <div class="ww-section-<%= rnd %> mb-4" id="wawJadwal<%= rnd %>">
        <div class="row g-3 align-items-center">
          <div class="col-auto">
            <div class="rounded-3 p-3 text-center" style="background:#dbeafe;min-width:56px">
              <i class="fas fa-calendar-days fa-lg" style="color:#1d4ed8"></i>
            </div>
          </div>
          <div class="col">
            <div class="text-muted small fw-bold text-uppercase mb-1" style="font-size:.7rem;letter-spacing:.05em">
              <%= Common.getBahasaConfig("Jadwal Wawancara Anda") %>
            </div>
            <div id="wawTanggal<%= rnd %>" class="fw-bold text-dark" style="font-size:1rem"></div>
            <div id="wawWaktu<%= rnd %>"   class="text-muted small mt-1"></div>
          </div>
          <div class="col-auto">
            <div id="wawStatusBadge<%= rnd %>"></div>
          </div>
        </div>
      </div>

      <%-- B. Profil Pewawancara --%>
      <div class="ww-section-<%= rnd %> mb-4">
        <div class="d-flex flex-column flex-md-row align-items-center align-items-md-start gap-4">
          <div class="text-center flex-shrink-0">
            <img id="wawFoto<%= rnd %>"
                 src=""
                 class="ww-avatar-<%= rnd %>"
                 alt="foto pewawancara"
                 onerror="this.style.display='none';document.getElementById('wawAvatarFallback<%= rnd %>').style.display='flex'">
            <div id="wawAvatarFallback<%= rnd %>"
                 class="ww-avatar-<%= rnd %> d-none align-items-center justify-content-center"
                 style="background:#e2e8f0;display:none!important">
              <i class="fas fa-user fa-2x" style="color:#94a3b8"></i>
            </div>
          </div>
          <div class="text-center text-md-start">
            <div class="text-muted small fw-bold text-uppercase mb-1" style="font-size:.7rem;letter-spacing:.05em">
              <%= Common.getBahasaConfig("Pewawancara Anda") %>
            </div>
            <div id="wawPegawai<%= rnd %>" class="fw-bold text-dark" style="font-size:1.3rem;line-height:1.3"></div>
            <div class="mt-2">
              <span class="badge rounded-pill border border-primary text-primary px-3 py-2"
                    style="background:#eff6ff;font-size:.75rem">
                <i class="fas fa-id-badge me-1"></i>
                <span id="wawNamaSesi<%= rnd %>"></span>
              </span>
            </div>
          </div>
        </div>
      </div>

      <%-- C. Tautan Video Konferensi --%>
      <div id="wawVideoSection<%= rnd %>" class="mb-4" style="display:none">
        <div class="ww-section-<%= rnd %>">
          <div class="d-flex flex-column flex-md-row align-items-center gap-3">
            <div class="rounded-3 p-3 text-center flex-shrink-0" id="wawVideoIconBox<%= rnd %>" style="min-width:56px">
              <i id="wawVideoIcon<%= rnd %>" class="fas fa-video fa-lg"></i>
            </div>
            <div class="flex-grow-1 text-center text-md-start">
              <div class="text-muted small fw-bold text-uppercase mb-1" style="font-size:.7rem;letter-spacing:.05em">
                <%= Common.getBahasaConfig("Platform Wawancara Online") %>
              </div>
              <div id="wawVideoPlatform<%= rnd %>" class="fw-bold text-dark" style="font-size:1rem"></div>
            </div>
            <div class="flex-shrink-0 d-grid d-md-block w-100" style="max-width:220px">
              <a id="wawVideoLink<%= rnd %>"
                 href="#"
                 target="_blank"
                 rel="noopener noreferrer"
                 class="btn ww-vidBtn-<%= rnd %> px-4 py-3 w-100 text-white shadow-sm">
                <i class="fas fa-arrow-up-right-from-square me-2"></i>
                <%= Common.getBahasaConfig("Bergabung Sekarang") %>
              </a>
            </div>
          </div>
        </div>
      </div>

      <%-- D. Info Wawancara dari konfigurasi gelombang --%>
      <div id="wawInfoSection<%= rnd %>" class="mb-4" style="display:none">
        <div class="alert border-0 rounded-4 p-4 mb-0"
             style="background:linear-gradient(135deg,#eff6ff,#dbeafe);border-left:4px solid #2563eb!important">
          <div class="d-flex align-items-start gap-3">
            <i class="fas fa-circle-info fa-lg mt-1 flex-shrink-0" style="color:#2563eb"></i>
            <div>
              <div class="fw-bold mb-2" style="color:#1e40af">
                <%= Common.getBahasaConfig("Informasi Wawancara") %>
              </div>
              <div id="wawInfoHtml<%= rnd %>" class="small text-dark" style="line-height:1.7"></div>
            </div>
          </div>
        </div>
      </div>

      <%-- E. Formulir Kesiapan --%>
      <div id="wawFormSection<%= rnd %>" class="ww-section-<%= rnd %> mb-4" style="display:none">
        <div class="mb-3">
          <div class="fw-bold text-dark mb-1">
            <i class="fas fa-list-check text-primary me-2"></i>
            <%= Common.getBahasaConfig("Pernyataan Kesiapan") %>
          </div>
          <p class="small text-muted mb-3">
            <%= Common.getBahasaConfig("Centang toggle di bawah ini untuk menyatakan bahwa Anda siap mengikuti sesi wawancara sekarang. Pernyataan ini akan dikirimkan secara otomatis kepada pewawancara Anda.") %>
          </p>
          <div class="form-check form-switch ww-switch-<%= rnd %> p-0 d-flex align-items-center gap-3">
            <input class="form-check-input ms-0 flex-shrink-0"
                   type="checkbox"
                   id="wawCheckSiap<%= rnd %>">
            <label class="form-check-label" for="wawCheckSiap<%= rnd %>">
              <%= Common.getBahasaConfig("Saya menyatakan SIAP untuk wawancara sekarang") %>
            </label>
          </div>
        </div>
        <div>
          <label class="form-label fw-bold small text-dark" for="wawCatatan<%= rnd %>">
            <i class="fas fa-pen-to-square text-muted me-1"></i>
            <%= Common.getBahasaConfig("Catatan (opsional)") %>
          </label>
          <textarea class="form-control rounded-3 bg-white"
                    id="wawCatatan<%= rnd %>"
                    rows="3"
                    style="border-color:#e2e8f0;font-size:.875rem"
                    placeholder="<%= Common.getBahasaConfig("Pesan, tautan konferensi Anda, atau catatan lainnya untuk pewawancara...") %>"></textarea>
        </div>
      </div>

      <%-- F. Status sudah siap --%>
      <div id="wawSudahSiap<%= rnd %>" class="mb-4" style="display:none">
        <div class="d-flex align-items-start gap-3 p-4 rounded-4"
             style="background:#f0fdf4;border:1.5px solid #86efac">
          <i class="fas fa-circle-check fa-2x mt-1 flex-shrink-0" style="color:#16a34a"></i>
          <div>
            <div class="fw-bold mb-1" style="color:#15803d;font-size:1rem">
              <%= Common.getBahasaConfig("Anda sudah menyatakan SIAP wawancara") %>
            </div>
            <div class="small text-muted" id="wawCatatanReadonly<%= rnd %>"></div>
          </div>
        </div>
      </div>

    </div><%-- /wawContent --%>

    <%-- Footer tombol aksi --%>
    <div id="wawFooter<%= rnd %>" class="card-footer bg-white border-top px-3 px-md-5 py-4" style="display:none">
      <div class="d-grid d-md-flex justify-content-md-end align-items-center gap-3">
        <div id="wawHintText<%= rnd %>" class="small text-muted text-center text-md-end flex-grow-1" style="display:none">
          <i class="fas fa-circle-info me-1 text-warning"></i>
          <%= Common.getBahasaConfig("Centang kesiapan di atas untuk mengaktifkan tombol kirim.") %>
        </div>
        <button id="wawBtnKirim<%= rnd %>"
                type="button"
                class="btn btn-primary px-5 py-3 rounded-pill fw-bold shadow-sm d-none"
                onclick="window.wawSubmitSiap<%= rnd %>()">
          <i class="fas fa-paper-plane me-2"></i>
          <%= Common.getBahasaConfig("Kirim Pernyataan Siap") %>
        </button>
      </div>
    </div>

  </div><%-- /ww-card --%>
</div><%-- /wawRoot --%>

<script>
/**
 * Modul Wawancara PSB — instance <%= rnd %>
 * Pola: IIFE — isolasi namespace. Fungsi publik di-expose via window.waw*<%= rnd %>.
 */
(function () {
    'use strict';

    var casisId    = '<%= casisId != null ? casisId : "" %>';
    var nowMs      = <%= tglServer.getTime() %>;
    var countdownTimer = null;

    var root     = document.getElementById('wawRoot<%= rnd %>');
    var skeleton = document.getElementById('wawSkeleton<%= rnd %>');
    var content  = document.getElementById('wawContent<%= rnd %>');
    var footer   = document.getElementById('wawFooter<%= rnd %>');

    // Konfigurasi visual platform video
    function getVideoPlatformConfig(iconKey) {
        var cfg = {
            jitsi:  { icon: 'fas fa-video',              bg: '#ecfdf5', fg: '#059669', btn: 'linear-gradient(135deg,#059669,#34d399)' },
            gmeet:  { icon: 'fas fa-video',              bg: '#f0fdf4', fg: '#16a34a', btn: 'linear-gradient(135deg,#16a34a,#4ade80)' },
            zoom:   { icon: 'fas fa-video',              bg: '#eff6ff', fg: '#1d4ed8', btn: 'linear-gradient(135deg,#1d4ed8,#60a5fa)' },
            bbb:    { icon: 'fas fa-chalkboard-teacher', bg: '#fef2f2', fg: '#dc2626', btn: 'linear-gradient(135deg,#dc2626,#f87171)' },
            skype:  { icon: 'fab fa-skype',              bg: '#eff6ff', fg: '#0078d4', btn: 'linear-gradient(135deg,#0078d4,#38bdf8)' },
            wa:     { icon: 'fab fa-whatsapp',           bg: '#f0fdf4', fg: '#16a34a', btn: 'linear-gradient(135deg,#16a34a,#4ade80)' },
            other:  { icon: 'fas fa-link',               bg: '#faf5ff', fg: '#7c3aed', btn: 'linear-gradient(135deg,#7c3aed,#c084fc)' }
        };
        return cfg[iconKey] || cfg['other'];
    }

    // Render badge status waktu + countdown
    function renderStatusWaktu(mulaiMs, sampaiMs) {
        var badgeEl = document.getElementById('wawStatusBadge<%= rnd %>');
        if (!badgeEl) { return; }

        function update() {
            var now = nowMs + (Date.now ? (Date.now() - nowMs) : 0);
            if (now < mulaiMs) {
                var sisa = Math.max(0, Math.floor((mulaiMs - now) / 1000));
                var jam  = Math.floor(sisa / 3600);
                var mnt  = Math.floor((sisa % 3600) / 60);
                var dtk  = sisa % 60;
                var txt  = jam > 0 ? jam + ' jam ' + mnt + ' mnt'
                    : mnt > 0 ? mnt + ' mnt ' + dtk + ' dtk' : dtk + ' dtk';
                badgeEl.innerHTML =
                    '<span class="ww-countdown-<%= rnd %> bg-warning text-dark">'
                    + '<i class="fas fa-clock me-1"></i>Mulai dalam ' + txt + '</span>';
            } else if (now >= mulaiMs && now <= sampaiMs) {
                badgeEl.innerHTML =
                    '<span class="ww-countdown-<%= rnd %> bg-success text-white">'
                    + '<i class="fas fa-circle me-1"></i>'
                    + '<%= Common.getBahasaConfigJS("Sedang Berlangsung") %></span>';
                if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null; }
            } else {
                badgeEl.innerHTML =
                    '<span class="ww-countdown-<%= rnd %> bg-secondary text-white">'
                    + '<i class="fas fa-hourglass-end me-1"></i><%= Common.getBahasaConfig("Sesi Selesai") %></span>';
                if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null; }
            }
        }

        update();
        if (nowMs < mulaiMs) {
            countdownTimer = setInterval(function () { nowMs += 1000; update(); }, 1000);
        }
    }

    // Tampilkan pesan error menggantikan root
    function tampilkanPesanRoot(icon, iconColor, title, body, extraClass) {
        root.innerHTML =
            '<div class="ww-card-<%= rnd %> p-4 p-md-5 ' + (extraClass || '') + '">'
            + '<div class="d-flex align-items-start gap-3">'
            +   '<i class="' + icon + ' fa-2x mt-1 flex-shrink-0" style="color:' + iconColor + '"></i>'
            +   '<div>'
            +     '<div class="fw-bold mb-2" style="color:#0f172a;font-size:1rem">' + title + '</div>'
            +     '<div class="small text-dark" style="white-space:pre-line;line-height:1.8">' + body + '</div>'
            +   '</div>'
            + '</div>'
            + '</div>';
    }

    function toggleKirimBtn(tampil) {
        var btn  = document.getElementById('wawBtnKirim<%= rnd %>');
        var hint = document.getElementById('wawHintText<%= rnd %>');
        if (!btn || !hint) { return; }
        if (tampil) { btn.classList.remove('d-none'); hint.style.display = 'none'; }
        else { btn.classList.add('d-none'); hint.style.display = ''; }
    }

    function kunciForms(catatan) {
        var formSec   = document.getElementById('wawFormSection<%= rnd %>');
        var sudahSiap = document.getElementById('wawSudahSiap<%= rnd %>');
        var catatanEl = document.getElementById('wawCatatanReadonly<%= rnd %>');
        var footerEl  = document.getElementById('wawFooter<%= rnd %>');
        if (formSec)   { formSec.style.display   = 'none'; }
        if (sudahSiap) { sudahSiap.style.display  = ''; }
        if (catatanEl) {
            catatanEl.textContent = catatan && catatan.trim().length > 0 ? '"' + catatan + '"' : '';
        }
        if (footerEl)  { footerEl.style.display   = 'none'; }
    }

    // Muat data wawancara dari server
    window.wawLoadData<%= rnd %> = async function () {
        var url = '<%= Common.ROOT %>/psb?hanya_tampil_jsp=true&p=psb&s=_wawancara_service&action=get_data&id=' + casisId;
        try {
            var res  = await fetch(url);
            if (!res.ok) { throw new Error('<%= Common.getBahasaConfigJS("Gagal terhubung ke peladen.") %>'); }
            var data = await res.json();

            if (skeleton) { skeleton.style.display = 'none'; }
            if (content)  { content.style.display  = ''; }

            if (data.status === 'success') {

                var tanggalEl = document.getElementById('wawTanggal<%= rnd %>');
                var waktuEl   = document.getElementById('wawWaktu<%= rnd %>');
                if (tanggalEl) { tanggalEl.textContent = data.tanggalStr || '-'; }
                if (waktuEl)   {
                    waktuEl.innerHTML = '<i class="fas fa-clock me-1"></i>'
                        + (data.mulaiStr || '-') + ' – ' + (data.sampaiStr || '-') + ' WIB';
                }
                if (data.mulaiMs && data.sampaiMs) {
                    renderStatusWaktu(data.mulaiMs, data.sampaiMs);
                }

                var fotoEl    = document.getElementById('wawFoto<%= rnd %>');
                var pegawaiEl = document.getElementById('wawPegawai<%= rnd %>');
                var namaEl    = document.getElementById('wawNamaSesi<%= rnd %>');
                if (fotoEl && data.pegawaiPhoto) {
                    fotoEl.src = data.pegawaiPhoto;
                } else if (fotoEl) {
                    fotoEl.style.display = 'none';
                    var fallback = document.getElementById('wawAvatarFallback<%= rnd %>');
                    if (fallback) {
                        fallback.style.removeProperty('display');
                        fallback.classList.remove('d-none');
                        fallback.style.display = 'flex';
                    }
                }
                if (pegawaiEl) { pegawaiEl.textContent = data.pegawaiName  || '-'; }
                if (namaEl)    { namaEl.textContent    = data.interviewerName || '-'; }

                if (data.onlineMenggunakan && data.onlineMenggunakan > 0 && data.videoLink) {
                    var vidSection = document.getElementById('wawVideoSection<%= rnd %>');
                    var vidIconBox = document.getElementById('wawVideoIconBox<%= rnd %>');
                    var vidIcon    = document.getElementById('wawVideoIcon<%= rnd %>');
                    var vidName    = document.getElementById('wawVideoPlatform<%= rnd %>');
                    var vidLink    = document.getElementById('wawVideoLink<%= rnd %>');
                    var vc         = getVideoPlatformConfig(data.videoIconKey);
                    if (vidSection) { vidSection.style.display = ''; }
                    if (vidIconBox) { vidIconBox.style.background = vc.bg; }
                    if (vidIcon)    { vidIcon.className = vc.icon + ' fa-lg'; vidIcon.style.color = vc.fg; }
                    if (vidName)    { vidName.textContent = data.videoPlatform || 'Online'; }
                    if (vidLink)    { vidLink.href = data.videoLink; vidLink.style.background = vc.btn; vidLink.style.border = 'none'; }
                }

                if (data.infoInterview && data.infoInterview.trim().length > 0) {
                    var infoSection = document.getElementById('wawInfoSection<%= rnd %>');
                    var infoHtml    = document.getElementById('wawInfoHtml<%= rnd %>');
                    if (infoSection) { infoSection.style.display = ''; }
                    if (infoHtml)    { infoHtml.innerHTML = data.infoInterview; }
                }

                if (footer) { footer.style.display = ''; }

                if (data.isSiap) {
                    kunciForms(data.catatan);
                } else {
                    var formSec = document.getElementById('wawFormSection<%= rnd %>');
                    if (formSec) { formSec.style.display = ''; }
                    var hintEl  = document.getElementById('wawHintText<%= rnd %>');
                    if (hintEl) { hintEl.style.display = ''; }
                    var checkEl  = document.getElementById('wawCheckSiap<%= rnd %>');
                    var catatanEl = document.getElementById('wawCatatan<%= rnd %>');
                    if (checkEl && data.catatan) { catatanEl.value = data.catatan; }
                    if (checkEl) {
                        checkEl.addEventListener('change', function () {
                            toggleKirimBtn(this.checked);
                        });
                    }
                }

            } else if (data.status === 'berkas_kurang') {
                tampilkanPesanRoot('fas fa-triangle-exclamation', '#f59e0b',
                    '<%= Common.getBahasaConfigJS("Berkas Wajib Belum Diunggah") %>',
                    data.message || '', 'border-warning');

            } else if (data.status === 'no_schedule') {
                if (skeleton) { skeleton.style.display = 'none'; }
                root.innerHTML =
                    '<div class="ww-card-<%= rnd %> p-5 text-center">'
                    + '<i class="fas fa-calendar-xmark fa-4x mb-4 d-block" style="color:#d97706;opacity:.7"></i>'
                    + '<h5 class="fw-bold text-dark mb-2"><%= Common.getBahasaConfig("Jadwal Belum Tersedia") %></h5>'
                    + '<p class="text-muted mb-0 small">' + (data.message || '') + '</p>'
                    + '<p class="text-muted small mt-2"><%= Common.getBahasaConfig("Mohon periksa kembali secara berkala atau hubungi panitia PPDB.") %></p>'
                    + '</div>';

            } else {
                tampilkanPesanRoot('fas fa-circle-exclamation', '#dc2626',
                    '<%= Common.getBahasaConfigJS("Tidak Dapat Memuat Data") %>',
                    data.message || '<%= Common.getBahasaConfigJS("Terjadi kesalahan yang tidak diketahui.") %>', '');
            }

        } catch (e) {
            if (skeleton) { skeleton.style.display = 'none'; }
            tampilkanPesanRoot('fas fa-wifi', '#dc2626',
                '<%= Common.getBahasaConfigJS("Gagal Terhubung ke Peladen") %>',
                '<%= Common.getBahasaConfigJS("Periksa koneksi internet Anda, lalu muat ulang halaman ini.") %>\n\n' + (e.message || ''), '');
        }
    };

    // Konfirmasi sebelum submit
    window.wawSubmitSiap<%= rnd %> = function () {
        var pesan = '<%= Common.getBahasaConfigJS("Apakah Anda yakin menyatakan SIAP untuk wawancara sekarang? Pewawancara Anda akan segera diberitahu.") %>';
        if (typeof window.showConfirmModal === 'function') {
            window.showConfirmModal(pesan, function () { window.wawEksekusiSubmit<%= rnd %>(); });
        } else if (confirm(pesan)) {
            window.wawEksekusiSubmit<%= rnd %>();
        }
    };

    // Eksekusi pengiriman
    window.wawEksekusiSubmit<%= rnd %> = async function () {
        var btn      = document.getElementById('wawBtnKirim<%= rnd %>');
        var catatanEl = document.getElementById('wawCatatan<%= rnd %>');
        var catatan  = catatanEl ? catatanEl.value : '';
        var oriHtml  = btn ? btn.innerHTML : '';

        if (btn) {
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status"></span>'
                + '<%= Common.getBahasaConfigJS("Menyimpan...") %>';
            btn.disabled = true;
        }

        try {
            var url = '<%= Common.ROOT %>/psb?hanya_tampil_jsp=true&p=psb&s=_wawancara_service'
                + '&action=submit_siap&id=' + casisId
                + '&catatan=' + encodeURIComponent(catatan);
            var res  = await fetch(url, { method: 'POST' });
            if (!res.ok) { throw new Error('<%= Common.getBahasaConfigJS("Gagal terhubung ke peladen.") %>'); }
            var data = await res.json();

            if (data.status === 'success') {
                kunciForms(catatan);
                if (typeof window.tampilkanToast === 'function') {
                    window.tampilkanToast(data.message, 'bg-success text-white');
                }
                if (data.waLink && data.waLink.trim().length > 0) {
                    setTimeout(function () {
                        window.open(data.waLink, '_blank', 'noopener,noreferrer');
                    }, 800);
                }
            } else if (data.status === 'berkas_kurang') {
                tampilkanPesanRoot('fas fa-triangle-exclamation', '#f59e0b',
                    '<%= Common.getBahasaConfigJS("Berkas Wajib Belum Diunggah") %>',
                    data.message || '', 'border-warning');
            } else {
                if (typeof window.tampilkanToast === 'function') {
                    window.tampilkanToast(data.message || '<%= Common.getBahasaConfigJS("Terjadi kesalahan.") %>', 'bg-danger text-white');
                } else {
                    alert(data.message || '<%= Common.getBahasaConfigJS("Terjadi kesalahan.") %>');
                }
                if (btn) { btn.innerHTML = oriHtml; btn.disabled = false; }
            }
        } catch (e) {
            if (typeof window.tampilkanToast === 'function') {
                window.tampilkanToast('<%= Common.getBahasaConfigJS("Gagal menyimpan kesiapan. Periksa koneksi Anda.") %>', 'bg-danger text-white');
            } else {
                alert('<%= Common.getBahasaConfigJS("Gagal menyimpan kesiapan. Periksa koneksi Anda.") %>');
            }
            if (btn) { btn.innerHTML = oriHtml; btn.disabled = false; }
        }
    };

    // Inisialisasi
    window.wawLoadData<%= rnd %>();

})();
</script>
