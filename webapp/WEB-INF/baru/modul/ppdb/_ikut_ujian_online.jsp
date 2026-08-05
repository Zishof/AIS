<%@page import="ais.common.Common"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="java.util.Date"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String rnd    = "";
    String casisId = request.getParameter("id");
    try { rnd = Common.getGeneratedBarCode(7); }
    catch (Exception e) { rnd = "ujpsb" + System.currentTimeMillis(); }
    Date tglServer = WaktuUtil.getDate();
%>
<style>
.cbt-card-<%= rnd %> {
    border-radius:1.375rem;border:none;
    box-shadow:0 8px 32px rgba(0,0,0,.09);overflow:hidden;
}
.cbt-hdr-<%= rnd %> {
    background:linear-gradient(135deg,#312e81 0%,#4f46e5 55%,#818cf8 100%);
    color:#fff;padding:1.75rem 2rem;
}
.cbt-tile-<%= rnd %> {
    border-radius:1rem;border:1.5px solid #e2e8f0;background:#fff;
    padding:1.25rem 1.5rem;transition:box-shadow .15s;
}
.cbt-tile-<%= rnd %>:hover { box-shadow:0 4px 16px rgba(0,0,0,.07); }
.cbt-bar-<%= rnd %> {
    height:8px;border-radius:100px;transition:width .6s ease;
}
.cbt-skel-<%= rnd %> {
    background:linear-gradient(90deg,#f1f5f9 25%,#e2e8f0 50%,#f1f5f9 75%);
    background-size:400% 100%;
    animation:cbt-sh-<%= rnd %> 1.4s ease infinite;border-radius:.5rem;
}
@keyframes cbt-sh-<%= rnd %> {
    0%  { background-position:100% 0 }
    100%{ background-position:-100% 0 }
}
.cbt-badge-sm-<%= rnd %> {
    font-size:.68rem;font-weight:700;letter-spacing:.04em;
    padding:.3em .75em;border-radius:100px;
}
.cbt-btn-masuk-<%= rnd %> {
    border-radius:.75rem;font-weight:700;letter-spacing:.02em;
    background:linear-gradient(135deg,#4f46e5,#818cf8);border:none;
    transition:transform .15s,box-shadow .15s;
}
.cbt-btn-masuk-<%= rnd %>:hover {
    transform:translateY(-2px);box-shadow:0 8px 20px rgba(79,70,229,.3)!important;
}
@media(max-width:767px){
    .cbt-hdr-<%= rnd %>{padding:1.25rem;}
}
</style>

<div class="cbt-card-<%= rnd %> bg-white">

  <%-- Header --%>
  <div class="cbt-hdr-<%= rnd %> d-flex justify-content-between align-items-center">
    <div>
      <h4 class="fw-bold mb-1">
        <i class="fas fa-laptop-code me-2"></i>
        <%= Common.getBahasaConfig("Ujian Seleksi Online") %>
      </h4>
      <p class="mb-0 small" style="opacity:.78">
        <%= Common.getBahasaConfig("Daftar sesi ujian yang ditugaskan kepada Anda untuk gelombang ini.") %>
      </p>
    </div>
    <div class="d-none d-md-block">
      <i class="fas fa-file-pen fa-3x" style="opacity:.16"></i>
    </div>
  </div>

  <%-- Skeleton --%>
  <div id="cbtSkel<%= rnd %>" class="p-4 p-md-5">
    <div class="cbt-skel-<%= rnd %> mb-3" style="height:80px"></div>
    <div class="cbt-skel-<%= rnd %> mb-3" style="height:80px"></div>
    <div class="cbt-skel-<%= rnd %>"      style="height:60px"></div>
  </div>

  <%-- Konten ujian --%>
  <div id="cbtBody<%= rnd %>" class="px-3 px-md-5 py-4" style="display:none">
    <div id="cbtExamRows<%= rnd %>"></div>
  </div>

  <%-- Panel Laporan Hasil --%>
  <div id="cbtHasil<%= rnd %>" class="px-3 px-md-5 pb-5" style="display:none">
    <div class="rounded-4 p-4 p-md-5 text-center"
         style="background:linear-gradient(135deg,#f0f9ff,#e0f2fe);border:1.5px solid #bae6fd">
      <div id="cbtGaugeWrap<%= rnd %>" class="d-flex justify-content-center mb-4"></div>
      <div id="cbtGradeLabel<%= rnd %>" class="fw-bold mb-2" style="font-size:1.3rem"></div>
      <div id="cbtInfoLulus<%= rnd %>"  class="text-muted small mb-4"></div>
      <div id="cbtRadarWrap<%= rnd %>"  class="d-flex justify-content-center mb-4"></div>
      <div id="cbtBarsWrap<%= rnd %>"></div>
    </div>
  </div>

</div>

<%-- Modal Konfirmasi Mulai --%>
<div class="modal fade" id="cbtKonfirm<%= rnd %>" tabindex="-1" aria-hidden="true" style="z-index:1070">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content border-0 rounded-4 shadow-lg">
      <div class="modal-header border-0 pb-0">
        <h5 class="modal-title fw-bold">
          <i class="fas fa-circle-question text-warning me-2"></i>
          <%= Common.getBahasaConfig("Konfirmasi Mulai Ujian") %>
        </h5>
      </div>
      <div class="modal-body text-muted small" style="line-height:1.8">
        <%= Common.getBahasaConfig("Apakah Anda yakin ingin memulai sesi ujian ini sekarang? Pastikan koneksi internet Anda stabil dan perangkat Anda siap sebelum melanjutkan.") %>
      </div>
      <div class="modal-footer border-0 pt-0 gap-2">
        <button type="button" class="btn btn-secondary btn-sm rounded-pill px-4" data-bs-dismiss="modal">
          <%= Common.getBahasaConfig("Batal") %>
        </button>
        <button type="button" id="cbtBtnMulai<%= rnd %>"
                class="btn btn-primary btn-sm rounded-pill px-4 fw-bold">
          <i class="fas fa-play me-1"></i><%= Common.getBahasaConfig("Mulai Sekarang") %>
        </button>
      </div>
    </div>
  </div>
</div>

<script>
(function () {
    'use strict';

    var casisId  = '<%= casisId != null ? casisId : "" %>';
    var nowMs    = <%= tglServer.getTime() %>;
    var ROOT     = '<%= Common.ROOT %>';

    // ── grade & warna ───────────────────────────────────────────────
    function getGradeKfg(grade) {
        var map = {
            'UNGGUL':      { warna:'#059669', ikon:'fas fa-trophy',       latar:'#d1fae5' },
            'BAIK SEKALI': { warna:'#2563eb', ikon:'fas fa-star',         latar:'#dbeafe' },
            'BAIK':        { warna:'#7c3aed', ikon:'fas fa-thumbs-up',    latar:'#ede9fe' },
            'CUKUP':       { warna:'#d97706', ikon:'fas fa-circle-check', latar:'#fef3c7' }
        };
        return map[grade] || map['CUKUP'];
    }
    function warnaBar(n) {
        return n >= 80 ? '#059669' : n >= 60 ? '#2563eb' : n >= 40 ? '#d97706' : '#dc2626';
    }

    // ── SVG gauge ──────────────────────────────────────────────────
    function buatGaugeSvg(score, warna) {
        var r = 70, cx = 90, cy = 90, stroke = 14;
        var circ = Math.PI * r;
        var offset = circ * (1 - Math.min(score, 100) / 100);
        return '<svg width="180" height="110" viewBox="0 0 180 110">'
            + '<path d="M ' + (cx - r) + ' ' + cy + ' A ' + r + ' ' + r + ' 0 0 1 ' + (cx + r) + ' ' + cy
            + '" fill="none" stroke="#e2e8f0" stroke-width="' + stroke + '" stroke-linecap="round"/>'
            + '<path d="M ' + (cx - r) + ' ' + cy + ' A ' + r + ' ' + r + ' 0 0 1 ' + (cx + r) + ' ' + cy
            + '" fill="none" stroke="' + warna + '" stroke-width="' + stroke + '" stroke-linecap="round"'
            + ' stroke-dasharray="' + circ + '" stroke-dashoffset="' + offset + '"/>'
            + '<text x="' + cx + '" y="' + (cy - 12) + '" text-anchor="middle" font-size="26" font-weight="700" fill="' + warna + '">'
            + Math.round(score) + '</text>'
            + '<text x="' + cx + '" y="' + (cy + 8) + '" text-anchor="middle" font-size="11" fill="#64748b">skor rata-rata</text>'
            + '</svg>';
    }

    // ── SVG radar ──────────────────────────────────────────────────
    function buatRadarSvg(items, warna) {
        var cx = 110, cy = 110, R = 80, n = items.length;
        if (n < 3) { return ''; }
        var paths = '', labels = '';
        var polyBg = '', polyData = '';
        for (var i = 0; i < n; i++) {
            var ang = (Math.PI * 2 * i / n) - Math.PI / 2;
            var x = cx + R * Math.cos(ang), y = cy + R * Math.sin(ang);
            paths  += '<line x1="' + cx + '" y1="' + cy + '" x2="' + x + '" y2="' + y + '" stroke="#e2e8f0" stroke-width="1"/>';
            polyBg += (i === 0 ? '' : ' ') + x + ',' + y;
            var v = Math.min(items[i].nilai || 0, 100) / 100;
            var dx = cx + R * v * Math.cos(ang), dy = cy + R * v * Math.sin(ang);
            polyData += (i === 0 ? '' : ' ') + dx + ',' + dy;
            var lx = cx + (R + 18) * Math.cos(ang), ly = cy + (R + 18) * Math.sin(ang);
            labels += '<text x="' + lx + '" y="' + (ly + 4) + '" text-anchor="middle" font-size="9" fill="#64748b">'
                + (items[i].nama || '').substring(0, 10) + '</text>';
        }
        return '<svg width="220" height="220" viewBox="0 0 220 220">'
            + '<polygon points="' + polyBg   + '" fill="none" stroke="#e2e8f0" stroke-width="1"/>'
            + paths
            + '<polygon points="' + polyData + '" fill="' + warna + '33" stroke="' + warna + '" stroke-width="2"/>'
            + labels + '</svg>';
    }

    // ── Panel hasil ────────────────────────────────────────────────
    function buatHasilPanel(data) {
        var hasilEl = document.getElementById('cbtHasil<%= rnd %>');
        if (!hasilEl) { return; }
        hasilEl.style.display = '';

        var kfg   = getGradeKfg(data.grade);
        var gauge = document.getElementById('cbtGaugeWrap<%= rnd %>');
        if (gauge) { gauge.innerHTML = buatGaugeSvg(data.rataRata || 0, kfg.warna); }

        var gradeEl = document.getElementById('cbtGradeLabel<%= rnd %>');
        if (gradeEl) {
            gradeEl.innerHTML = '<span class="badge rounded-pill px-4 py-2 fw-bold" style="background:'
                + kfg.latar + ';color:' + kfg.warna + ';font-size:1rem">'
                + '<i class="' + kfg.ikon + ' me-2"></i>'
                + (data.infoNilaiLabel || '') + ' <strong>' + (data.grade || '') + '</strong></span>';
        }

        var infoEl = document.getElementById('cbtInfoLulus<%= rnd %>');
        if (infoEl) { infoEl.textContent = data.infoLulus || ''; }

        // Radar (jika ≥3 ujian dengan nilai)
        var nilaiBerNilai = (data.exams || []).filter(function(e){ return e.status === 'SUDAH' && e.nilai != null; });
        var radarEl = document.getElementById('cbtRadarWrap<%= rnd %>');
        if (radarEl && nilaiBerNilai.length >= 3) {
            radarEl.innerHTML = buatRadarSvg(nilaiBerNilai, kfg.warna);
        }

        // Progress bars
        var barsEl = document.getElementById('cbtBarsWrap<%= rnd %>');
        if (barsEl && nilaiBerNilai.length > 0) {
            var html = '';
            for (var i = 0; i < nilaiBerNilai.length; i++) {
                var e = nilaiBerNilai[i];
                var n = e.nilai || 0;
                var w = warnaBar(n);
                html += '<div class="mb-3 text-start">'
                    + '<div class="d-flex justify-content-between mb-1">'
                    +   '<span class="small fw-bold text-dark">' + e.nama + '</span>'
                    +   '<span class="small fw-bold" style="color:' + w + '">' + Math.round(n) + '</span>'
                    + '</div>'
                    + '<div class="bg-light rounded-pill" style="height:8px">'
                    +   '<div class="cbt-bar-<%= rnd %> rounded-pill" style="width:' + n + '%;background:' + w + '"></div>'
                    + '</div>'
                    + '</div>';
            }
            barsEl.innerHTML = html;
        }
    }

    // ── Baris ujian per tile ───────────────────────────────────────
    function buatTileUjian(exam, idx) {
        var selesai = exam.status === 'SUDAH';
        var tglMul = exam.tanggalMulai, tglSel = exam.tanggalSelesai;
        var now    = nowMs + (Date.now ? (Date.now() - nowMs) : 0);
        var aktif  = tglMul && tglSel && now >= tglMul && now <= tglSel;
        var belum  = tglMul && now < tglMul;

        var badgeHtml = '';
        if (selesai) {
            badgeHtml = '<span class="cbt-badge-sm-<%= rnd %> bg-success text-white"><i class="fas fa-circle-check me-1"></i>'
                + '<%= Common.getBahasaConfigJS("Selesai") %></span>';
        } else if (aktif) {
            badgeHtml = '<span class="cbt-badge-sm-<%= rnd %> bg-danger text-white"><i class="fas fa-circle me-1"></i>'
                + '<%= Common.getBahasaConfigJS("Berlangsung") %></span>';
        } else if (belum) {
            badgeHtml = '<span class="cbt-badge-sm-<%= rnd %> bg-warning text-dark"><i class="fas fa-clock me-1"></i>'
                + '<%= Common.getBahasaConfigJS("Akan Datang") %></span>';
        } else {
            badgeHtml = '<span class="cbt-badge-sm-<%= rnd %> bg-secondary text-white"><i class="fas fa-hourglass-end me-1"></i>'
                + '<%= Common.getBahasaConfigJS("Selesai") %></span>';
        }

        var kuotaHtml = '';
        if (exam.maxPercobaan && exam.maxPercobaan > 0) {
            kuotaHtml = '<span class="small text-muted ms-2"><i class="fas fa-rotate-right me-1"></i>'
                + exam.jumlahIkut + '/' + exam.maxPercobaan
                + ' <%= Common.getBahasaConfig("percobaan") %></span>';
        }

        var nilaiHtml = '';
        if (selesai && exam.nilai != null) {
            var w = warnaBar(exam.nilai);
            nilaiHtml = '<div class="mt-2"><span class="badge rounded-pill px-3 py-2 fw-bold" style="background:'
                + w + '1a;color:' + w + ';font-size:.8rem">'
                + '<i class="fas fa-star me-1"></i>'
                + '<%= Common.getBahasaConfigJS("Nilai") %>: ' + Math.round(exam.nilai) + '</span></div>';
        }

        var sudahHabisKuota = exam.maxPercobaan > 0 && exam.jumlahIkut >= exam.maxPercobaan;
        var bisaMasuk = !sudahHabisKuota && !belum;
        var btnHtml = '';
        if (bisaMasuk) {
            btnHtml = '<button type="button"'
                + ' class="btn btn-sm cbt-btn-masuk-<%= rnd %> text-white px-4 py-2 flex-shrink-0"'
                + ' onclick="window.cbtKonfirmasiMulai<%= rnd %>(' + exam.id + ')">'
                + '<i class="fas fa-play me-2"></i>'
                + (selesai ? '<%= Common.getBahasaConfigJS("Ikut Lagi") %>' : '<%= Common.getBahasaConfigJS("Mulai Ujian") %>')
                + '</button>';
        }

        return '<div class="cbt-tile-<%= rnd %> mb-3">'
            + '<div class="d-flex flex-column flex-md-row align-items-md-center gap-3">'
            +   '<div class="d-flex align-items-center justify-content-center rounded-3 flex-shrink-0"'
            +       ' style="width:48px;height:48px;background:#ede9fe">'
            +     '<span class="fw-black text-primary" style="font-size:1.1rem">' + (idx + 1) + '</span>'
            +   '</div>'
            +   '<div class="flex-grow-1">'
            +     '<div class="fw-bold text-dark mb-1" style="font-size:.95rem">' + exam.nama + '</div>'
            +     '<div class="d-flex flex-wrap align-items-center gap-2">'
            +       badgeHtml + kuotaHtml
            +     '</div>'
            +     '<div class="text-muted small mt-1"><i class="fas fa-calendar me-1"></i>'
            +       exam.tglMulai + ' — ' + exam.tglSelesai + '</div>'
            +     nilaiHtml
            +   '</div>'
            +   (btnHtml ? '<div>' + btnHtml + '</div>' : '')
            + '</div>'
            + '</div>';
    }

    // ── Load data ujian ────────────────────────────────────────────
    window.cbtLoadData<%= rnd %> = async function () {
        var url = ROOT + '/psb?hanya_tampil_jsp=true&p=psb&s=_ikut_ujian_online_service&action=fetch_exams&id=' + casisId;
        try {
            var res  = await fetch(url);
            if (!res.ok) { throw new Error('<%= Common.getBahasaConfigJS("Gagal terhubung ke peladen.") %>'); }
            var data = await res.json();

            var skelEl = document.getElementById('cbtSkel<%= rnd %>');
            var bodyEl = document.getElementById('cbtBody<%= rnd %>');
            if (skelEl) { skelEl.style.display = 'none'; }
            if (bodyEl) { bodyEl.style.display  = ''; }

            if (data.status === 'success') {
                var rowsEl = document.getElementById('cbtExamRows<%= rnd %>');
                if (rowsEl) {
                    var html = '';
                    if (!data.exams || data.exams.length === 0) {
                        html = '<div class="text-center py-5 text-muted">'
                            + '<i class="fas fa-calendar-xmark fa-3x mb-3 d-block" style="opacity:.4"></i>'
                            + '<p class="mb-0"><%= Common.getBahasaConfig("Belum ada sesi ujian yang ditugaskan kepada Anda.") %></p>'
                            + '</div>';
                    } else {
                        for (var i = 0; i < data.exams.length; i++) {
                            html += buatTileUjian(data.exams[i], i);
                        }
                    }
                    rowsEl.innerHTML = html;
                }
                if (data.allFinished) {
                    buatHasilPanel(data);
                }
            } else if (data.status === 'no_schedule') {
                var bodyEl2 = document.getElementById('cbtBody<%= rnd %>');
                if (bodyEl2) {
                    bodyEl2.innerHTML =
                        '<div class="text-center py-5">'
                        + '<i class="fas fa-calendar-xmark fa-4x mb-4 d-block text-warning" style="opacity:.6"></i>'
                        + '<h6 class="fw-bold text-dark"><%= Common.getBahasaConfig("Jadwal Belum Tersedia") %></h6>'
                        + '<p class="text-muted small mb-0">' + (data.message || '') + '</p>'
                        + '</div>';
                }
            } else {
                var bodyEl3 = document.getElementById('cbtBody<%= rnd %>');
                if (bodyEl3) {
                    bodyEl3.innerHTML =
                        '<div class="alert alert-danger rounded-4 border-0 d-flex gap-3 align-items-start">'
                        + '<i class="fas fa-circle-exclamation fa-lg mt-1 flex-shrink-0"></i>'
                        + '<div><strong><%= Common.getBahasaConfig("Gagal Memuat Data") %></strong>'
                        + '<div class="small mt-1">' + (data.message || '') + '</div></div>'
                        + '</div>';
                }
            }
        } catch (e) {
            var skelE = document.getElementById('cbtSkel<%= rnd %>');
            var bodyE = document.getElementById('cbtBody<%= rnd %>');
            if (skelE) { skelE.style.display = 'none'; }
            if (bodyE) {
                bodyE.style.display = '';
                bodyE.innerHTML =
                    '<div class="alert alert-danger rounded-4 border-0 d-flex gap-3 align-items-start">'
                    + '<i class="fas fa-wifi fa-lg mt-1 flex-shrink-0"></i>'
                    + '<div><strong><%= Common.getBahasaConfig("Gagal Terhubung") %></strong>'
                    + '<div class="small mt-1"><%= Common.getBahasaConfig("Periksa koneksi internet Anda.") %></div></div>'
                    + '</div>';
            }
        }
    };

    // ── Konfirmasi mulai ───────────────────────────────────────────
    var _pendingId = null;
    window.cbtKonfirmasiMulai<%= rnd %> = function (idUjian) {
        _pendingId = idUjian;
        var modalEl = document.getElementById('cbtKonfirm<%= rnd %>');
        if (modalEl && window.bootstrap) {
            new bootstrap.Modal(modalEl).show();
        } else {
            if (confirm('<%= Common.getBahasaConfigJS("Apakah Anda yakin ingin memulai ujian sekarang?") %>')) {
                window.cbtBukaUjian<%= rnd %>(_pendingId);
            }
        }
    };

    var btnMulai = document.getElementById('cbtBtnMulai<%= rnd %>');
    if (btnMulai) {
        btnMulai.addEventListener('click', function () {
            var modalEl = document.getElementById('cbtKonfirm<%= rnd %>');
            if (modalEl && window.bootstrap) {
                var inst = bootstrap.Modal.getInstance(modalEl);
                if (inst) { inst.hide(); }
            }
            if (_pendingId) { window.cbtBukaUjian<%= rnd %>(_pendingId); }
        });
    }

    // ── Buka ujian dalam modal fullscreen ─────────────────────────
    window.cbtBukaUjian<%= rnd %> = async function (idUjian) {
        var modalId   = 'cbtUjianModal<%= rnd %>';
        var existingM = document.getElementById(modalId);
        if (existingM) { existingM.remove(); }

        var ujianUrl = ROOT + '/baru?hanya_tampil_jsp=true'
            + '&p=elearning%2Fujian&s=ujian'
            + '&ppu=' + idUjian
            + '&calonSiswa=' + casisId;

        var modalHtml =
            '<div class="modal fade" id="' + modalId + '" tabindex="-1" data-bs-backdrop="static" aria-hidden="true" style="z-index:1090">'
            + '<div class="modal-dialog modal-fullscreen">'
            +   '<div class="modal-content border-0">'
            +     '<div class="modal-header py-2 px-3 bg-dark text-white border-0">'
            +       '<span class="fw-bold small"><i class="fas fa-laptop-code me-2"></i>'
            +       '<%= Common.getBahasaConfigJS("Sesi Ujian Berlangsung") %></span>'
            +       '<button type="button" class="btn-close btn-close-white ms-auto" data-bs-dismiss="modal"></button>'
            +     '</div>'
            +     '<div class="modal-body p-0 bg-light" id="cbtUjianBody<%= rnd %>">'
            +       '<div class="d-flex align-items-center justify-content-center h-100">'
            +         '<div class="text-center"><div class="spinner-border text-primary mb-3" role="status"></div>'
            +         '<div class="fw-bold text-muted small"><%= Common.getBahasaConfig("Memuat sesi ujian...") %></div>'
            +         '</div>'
            +       '</div>'
            +     '</div>'
            +   '</div>'
            + '</div>'
            + '</div>';

        document.body.insertAdjacentHTML('beforeend', modalHtml);
        var mEl = document.getElementById(modalId);
        new bootstrap.Modal(mEl).show();

        try {
            var res  = await fetch(ujianUrl);
            if (!res.ok) { throw new Error(); }
            var html = await res.text();
            var bodyEl = document.getElementById('cbtUjianBody<%= rnd %>');
            if (bodyEl) {
                bodyEl.innerHTML = html;
                var scripts = Array.from(bodyEl.getElementsByTagName('script'));
                for (var i = 0; i < scripts.length; i++) {
                    var old = scripts[i];
                    var ns  = document.createElement('script');
                    Array.from(old.attributes).forEach(function(a){
                        if (a.name.toLowerCase() !== 'type') { ns.setAttribute(a.name, a.value); }
                    });
                    ns.type = 'text/javascript';
                    var src = old.src || old.getAttribute('data-rocketlazyloadscript') || '';
                    if (src) { ns.src = src; document.body.appendChild(ns); }
                    else     { ns.text = old.innerHTML; document.body.appendChild(ns).parentNode.removeChild(ns); }
                }
            }
        } catch (e) {
            var bEl = document.getElementById('cbtUjianBody<%= rnd %>');
            if (bEl) {
                bEl.innerHTML = '<div class="text-center py-5 text-danger">'
                    + '<i class="fas fa-circle-exclamation fa-3x mb-3"></i>'
                    + '<h5><%= Common.getBahasaConfig("Gagal membuka sesi ujian.") %></h5>'
                    + '<p class="small text-muted"><%= Common.getBahasaConfig("Periksa koneksi internet Anda dan coba lagi.") %></p>'
                    + '</div>';
            }
        }

        mEl.addEventListener('hidden.bs.modal', function () {
            this.remove();
            window.cbtLoadData<%= rnd %>();
        });
    };

    // Inisialisasi
    window.cbtLoadData<%= rnd %>();
})();
</script>
