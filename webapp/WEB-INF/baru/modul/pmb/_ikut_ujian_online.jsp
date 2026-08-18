<%@page import="java.util.Date"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String rnd    = "";
    String camaId = request.getParameter("id");
    Session sess  = null;
    try {
        rnd  = Common.getGeneratedBarCode(7);
        sess = HibernateUtil.getSessionFactory().openSession();
    } catch (Exception e) {
        System.err.println("_ikut_ujian_online.jsp init error: " + e.getMessage());
        rnd = "e" + System.currentTimeMillis();
    } finally {
        if (sess != null) {
            try { sess.clear();      } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_ikut_ujian_online.jsp:21");}
            try { sess.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_ikut_ujian_online.jsp:22");}
            try { sess.close();      } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_ikut_ujian_online.jsp:23");}
        }
    }
    Date tglServer = WaktuUtil.getDate();
%>

<style>
/* ── Scoped with rnd to avoid conflicts in multi-widget pages ──────────── */
.cbt-card-<%= rnd %> {
    border-radius: 1.25rem;
    border: none;
    box-shadow: 0 8px 32px rgba(0,0,0,.09);
    background: #ffffff;
    overflow: hidden;
}
.cbt-header-<%= rnd %> {
    background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 60%, #3b82f6 100%);
    color: #fff;
    padding: 1.75rem 2rem;
}
.cbt-hover-<%= rnd %> {
    transition: transform .18s ease, box-shadow .18s ease;
    border: 1px solid #f1f5f9;
    background: #f8fafc;
}
.cbt-hover-<%= rnd %>:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0,0,0,.07) !important;
    background: #ffffff;
    border-color: #e2e8f0;
}
.cbt-stat-tile-<%= rnd %> {
    background: #fff;
    border: 1px solid #e2e8f0;
    border-radius: .875rem;
    padding: 1.25rem;
    text-align: center;
    height: 100%;
}
.cbt-modal-fs-<%= rnd %> {
    z-index: 1080 !important;
}
.cbt-progress-<%= rnd %> {
    height: 10px;
    border-radius: 100px;
    overflow: hidden;
    background: #f1f5f9;
}
.cbt-progress-bar-<%= rnd %> {
    height: 100%;
    border-radius: 100px;
    transition: width 1.1s cubic-bezier(.4,0,.2,1);
}
body.ais-pmb-ujian-active #ais-lang-switcher,
body.ais-pmb-ujian-active #ais-lang-switcher-navbar,
body.ais-pmb-ujian-active .ais-lang-dd,
body.ais-pmb-ujian-active .ais-lang-menu,
body.ais-pmb-ujian-active .ais-pilih-bahasa-combo {
    display: none !important;
    visibility: hidden !important;
    pointer-events: none !important;
}
@media (max-width: 767px) {
    .cbt-header-<%= rnd %> { padding: 1.25rem; }
}
</style>

<%-- ═══════════════════════════════════════════════════════════════════
     MAIN CARD
     ════════════════════════════════════════════════════════════════════ --%>
<div class="cbt-card-<%= rnd %> animate__animated animate__fadeIn">

    <%-- ── Header ──────────────────────────────────────────────────── --%>
    <div class="cbt-header-<%= rnd %> d-flex justify-content-between align-items-center">
        <div>
            <h4 class="fw-bold mb-1">
                <i class="fas fa-file-signature me-2 text-warning"></i>
                <%= Common.getBahasaConfig("Sistem Ujian Online CBT") %>
            </h4>
            <p class="mb-0 small" style="opacity:.78">
                <%= Common.getBahasaConfig("Selesaikan seluruh komponen evaluasi sesuai jadwal dan kuota percobaan Anda.") %>
            </p>
        </div>
        <div class="d-none d-md-block"><i class="fas fa-laptop-code fa-3x" style="opacity:.18"></i></div>
    </div>

    <%-- ── Hasil evaluasi (tampil setelah semua ujian selesai) ──────── --%>
    <div id="hasilPanel<%= rnd %>" class="px-4 px-md-5 pt-4" style="display:none;"></div>

    <%-- ── Daftar ujian ─────────────────────────────────────────────── --%>
    <div class="px-3 px-md-5 pb-5 pt-3">

        <%-- Column headers (desktop) --%>
        <div class="d-none d-md-flex fw-bold text-muted text-uppercase mb-3 px-3 pb-3 border-bottom"
             style="font-size:.72rem;letter-spacing:.05em;">
            <div style="width:5%">#</div>
            <div style="width:28%"><%= Common.getBahasaConfig("Mata Evaluasi") %></div>
            <div style="width:27%"><%= Common.getBahasaConfig("Jadwal Pelaksanaan") %></div>
            <div style="width:17%;text-align:center"><%= Common.getBahasaConfig("Kuota Percobaan") %></div>
            <div style="width:15%;text-align:center"><%= Common.getBahasaConfig("Status") %></div>
            <div style="width:8%;text-align:right"><%= Common.getBahasaConfig("Opsi") %></div>
        </div>

        <%-- Exam rows container --%>
        <div id="examBody<%= rnd %>" class="d-flex flex-column gap-3">
            <div class="text-center py-5">
                <div class="spinner-grow text-primary mb-3" role="status"></div>
                <div class="fw-bold text-muted"><%= Common.getBahasaConfig("Menyinkronkan data evaluasi...") %></div>
            </div>
        </div>

    </div>
</div><%-- /cbt-card --%>

<script>
/**
 * Modul Ujian Online CBT — <%= rnd %>
 *
 * Tanggung jawab modul ini:
 *   1. Mengambil daftar komponen evaluasi dari server via fetch API.
 *   2. Merender baris evaluasi (jadwal, kuota, status, tombol aksi) secara responsif.
 *   3. Membuka lembar ujian dalam popup modal layar penuh saat peserta siap memulai.
 *   4. Menampilkan panel "Laporan Hasil Evaluasi" lengkap setelah semua ujian selesai,
 *      termasuk skor gauge, progress bar per komponen, dan spider-web chart (≥ 3 ujian).
 *   5. Memblokir akses ujian jika berkas wajib belum diunggah (gate check dari server).
 *
 * Pola desain: IIFE (Immediately Invoked Function Expression) — semua fungsi terisolasi
 * dalam scope-nya sendiri agar tidak mencemari namespace global halaman induk.
 * Setiap fungsi yang perlu dipanggil dari luar di-expose ke window dengan sufiks rnd.
 */
(function () {
    'use strict';

    // Referensi elemen DOM yang sering digunakan
    var examBody    = document.getElementById('examBody<%= rnd %>');
    var hasilPanel  = document.getElementById('hasilPanel<%= rnd %>');
    var nowMs       = <%= tglServer.getTime() %>;   // server time in ms, bukan Date.now()
    var camaId      = '<%= camaId != null ? camaId : "" %>';

    function setModeUjianPmb(aktif) {
        try {
            if (!document.body) { return; }
            if (aktif) {
                document.body.classList.add('ais-pmb-ujian-active');
            } else {
                document.body.classList.remove('ais-pmb-ujian-active');
            }
        } catch (e) {}
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER: SVG Gauge Melingkar
    // Menampilkan skor numerik di dalam lingkaran seperti speedometer.
    // score  = angka 0–100
    // warna  = hex color string, misal '#1d4ed8'
    // ─────────────────────────────────────────────────────────────────
    function buatGaugeSvg(score, warna) {
        var R           = 42;
        var circ        = 2 * Math.PI * R;          // ≈ 263.9
        var validScore  = Math.min(100, Math.max(0, score || 0));
        var filled      = (circ * validScore / 100).toFixed(2);
        var empty       = (circ - parseFloat(filled)).toFixed(2);
        var warnaPilih  = validScore >= 90 ? '#16a34a'
                        : validScore >= 70 ? '#1d4ed8'
                        : validScore >= 50 ? '#0891b2'
                        : '#d97706';
        return '<svg width="110" height="110" viewBox="0 0 110 110" xmlns="http://www.w3.org/2000/svg">'
            + '<circle cx="55" cy="55" r="' + R + '" stroke="#e2e8f0" stroke-width="9" fill="none"/>'
            + '<circle cx="55" cy="55" r="' + R + '" stroke="' + warnaPilih + '" stroke-width="9" fill="none"'
            +   ' stroke-dasharray="' + filled + ' ' + empty + '"'
            +   ' stroke-linecap="round"'
            +   ' transform="rotate(-90 55 55)"/>'
            + '<text x="55" y="49" text-anchor="middle" dominant-baseline="middle"'
            +   ' font-size="17" font-weight="700" font-family="system-ui" fill="#0f172a">'
            +   score.toFixed(1)
            + '</text>'
            + '<text x="55" y="69" text-anchor="middle" font-size="9" font-family="system-ui" fill="#94a3b8">POIN</text>'
            + '</svg>';
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER: SVG Spider-Web (Radar) Chart
    // Memperlihatkan profil kemampuan peserta secara visual per komponen.
    // items  = Array [{nama, nilai}, ...]  — minimal 3 item
    // warna  = hex color string
    // ─────────────────────────────────────────────────────────────────
    function buatRadarSvg(items, warna) {
        if (!items || items.length < 3) { return ''; }
        var n = items.length;
        var CX = 150, CY = 135, R = 88;

        // Grid polygon per level (5 tingkat: 20, 40, 60, 80, 100)
        var grid = '';
        for (var g = 1; g <= 5; g++) {
            var gr = R * g / 5;
            var pts = [];
            for (var i = 0; i < n; i++) {
                var a = (i / n) * 2 * Math.PI - Math.PI / 2;
                pts.push((CX + gr * Math.cos(a)).toFixed(1) + ',' + (CY + gr * Math.sin(a)).toFixed(1));
            }
            var opacity = g === 5 ? '1' : '0.5';
            grid += '<polygon points="' + pts.join(' ') + '" fill="none" stroke="#e2e8f0" stroke-width="' + opacity + '"/>';
        }
        // Label persentil (opsional: 20, 40, 60, 80, 100)
        var pctLabels = '';
        var aRef = -Math.PI / 2;  // arah atas (12 o'clock)
        for (var g = 1; g <= 5; g++) {
            var gr = R * g / 5;
            var px = CX + gr * Math.cos(aRef);
            var py = CY + gr * Math.sin(aRef);
            pctLabels += '<text x="' + (px - 12).toFixed(1) + '" y="' + py.toFixed(1)
                + '" font-size="7.5" fill="#94a3b8" text-anchor="end">' + (g * 20) + '</text>';
        }

        // Garis sumbu
        var axes = '';
        for (var i = 0; i < n; i++) {
            var a = (i / n) * 2 * Math.PI - Math.PI / 2;
            axes += '<line x1="' + CX + '" y1="' + CY
                + '" x2="' + (CX + R * Math.cos(a)).toFixed(1)
                + '" y2="' + (CY + R * Math.sin(a)).toFixed(1)
                + '" stroke="#e2e8f0" stroke-width="1"/>';
        }

        // Polygon data
        var dataPts = [];
        for (var i = 0; i < n; i++) {
            var a   = (i / n) * 2 * Math.PI - Math.PI / 2;
            var val = Math.min(100, Math.max(0, items[i].nilai || 0)) / 100;
            dataPts.push((CX + R * val * Math.cos(a)).toFixed(1) + ',' + (CY + R * val * Math.sin(a)).toFixed(1));
        }

        // Label nama + titik skor
        var dots = '';
        var labels = '';
        for (var i = 0; i < n; i++) {
            var a    = (i / n) * 2 * Math.PI - Math.PI / 2;
            var val  = Math.min(100, Math.max(0, items[i].nilai || 0)) / 100;
            var dx   = CX + R * val * Math.cos(a);
            var dy   = CY + R * val * Math.sin(a);
            dots += '<circle cx="' + dx.toFixed(1) + '" cy="' + dy.toFixed(1) + '" r="4.5" fill="' + warna + '" stroke="#fff" stroke-width="1.5"/>';

            var lx  = CX + (R + 24) * Math.cos(a);
            var ly  = CY + (R + 24) * Math.sin(a);
            var nm  = items[i].nama;
            if (nm.length > 13) { nm = nm.substring(0, 11) + '..'; }
            labels += '<text x="' + lx.toFixed(1) + '" y="' + (ly - 3).toFixed(1)
                + '" text-anchor="middle" dominant-baseline="middle" font-size="8.5"'
                + ' font-family="system-ui" fill="#475569">' + nm + '</text>';
            labels += '<text x="' + lx.toFixed(1) + '" y="' + (ly + 9).toFixed(1)
                + '" text-anchor="middle" font-size="8" font-family="system-ui"'
                + ' font-weight="600" fill="' + warna + '">' + (items[i].nilai || 0).toFixed(0) + '</text>';
        }

        return '<svg width="300" height="270" viewBox="0 0 300 270" xmlns="http://www.w3.org/2000/svg">'
            + grid + pctLabels + axes
            + '<polygon points="' + dataPts.join(' ') + '"'
            +   ' fill="' + warna + '" fill-opacity="0.14"'
            +   ' stroke="' + warna + '" stroke-width="2" stroke-linejoin="round"/>'
            + dots + labels
            + '</svg>';
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER: Konfigurasi warna & ikon berdasarkan teks grade
    // ─────────────────────────────────────────────────────────────────
    function getGradeKonfig(grade) {
        var g = (grade || '').toUpperCase();
        if (g.indexOf('UNGGUL')      >= 0) return { hex: '#16a34a', css: 'success', ikon: 'fas fa-trophy',    warnaIkon: '#f59e0b' };
        if (g.indexOf('BAIK SEKALI') >= 0) return { hex: '#1d4ed8', css: 'primary', ikon: 'fas fa-star',      warnaIkon: '#1d4ed8' };
        if (g.indexOf('BAIK')        >= 0) return { hex: '#0891b2', css: 'info',    ikon: 'fas fa-thumbs-up', warnaIkon: '#0891b2' };
        return                               { hex: '#d97706', css: 'warning', ikon: 'fas fa-check-circle', warnaIkon: '#d97706' };
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER: Warna progress bar berdasarkan nilai
    // ─────────────────────────────────────────────────────────────────
    function warnaBar(nilai) {
        if (nilai >= 90) return '#16a34a';
        if (nilai >= 70) return '#1d4ed8';
        if (nilai >= 50) return '#0891b2';
        return '#d97706';
    }
    function labelGrade(nilai) {
        if (nilai >= 90) return 'UNGGUL';
        if (nilai >= 70) return 'BAIK SEKALI';
        if (nilai >= 50) return 'BAIK';
        return 'CUKUP';
    }

    // ─────────────────────────────────────────────────────────────────
    // BUILDER: Panel Laporan Hasil Evaluasi
    // Ditampilkan setelah peserta menyelesaikan SEMUA komponen ujian.
    // Berisi: ringkasan skor, progress per komponen, spider-web chart.
    // ─────────────────────────────────────────────────────────────────
    function buatHasilPanel(data) {
        var rt         = typeof data.rataRata === 'number' ? data.rataRata : 0;
        var grade      = data.grade || 'CUKUP';
        var exams      = data.exams || [];
        var selesai    = exams.filter(function(e) { return e.nilai !== undefined && e.nilai !== null; });
        var gk         = getGradeKonfig(grade);

        // ── Banner selamat jika diterima otomatis ──────────────────
        var bannerOtomatis = '';
        if (data.autoAccepted) {
            bannerOtomatis =
                '<div class="d-flex align-items-start gap-3 p-4 mb-4 rounded-4"'
                + ' style="background:linear-gradient(135deg,#dcfce7,#bbf7d0);border:1.5px solid #86efac">'
                + '<i class="fas fa-party-horn fa-2x" style="color:#16a34a;flex-shrink:0;margin-top:2px"></i>'
                + '<div>'
                + '<div class="fw-bold mb-1" style="color:#15803d;font-size:1.05rem">'
                + '<%= Common.getBahasaConfigJS("Selamat! Anda Dinyatakan Diterima Otomatis") %>'
                + '</div>'
                + '<div class="small" style="color:#166534">'
                + '<%= Common.getBahasaConfigJS("Hasil ujian Anda memenuhi syarat kelulusan otomatis. Panitia akan menghubungi Anda untuk langkah selanjutnya.") %>'
                + '</div>'
                + '</div></div>';
        }

        // ── Gauge + stat tiles ─────────────────────────────────────
        var gaugeSvg = buatGaugeSvg(rt, gk.hex);
        var tileStat =
            '<div class="row g-3 mb-4">'

            // Tile 1: Skor rata-rata
            + '<div class="col-6 col-md-4">'
            + '<div class="cbt-stat-tile-<%= rnd %> h-100">'
            + '<div class="mb-1">' + gaugeSvg + '</div>'
            + '<div class="small fw-semibold text-muted"><%= Common.getBahasaConfig("Skor Rata-rata") %></div>'
            + '</div></div>'

            // Tile 2: Kualifikasi
            + '<div class="col-6 col-md-4">'
            + '<div class="cbt-stat-tile-<%= rnd %> h-100 d-flex flex-column justify-content-center align-items-center">'
            + '<i class="' + gk.ikon + ' fa-2x mb-2" style="color:' + gk.warnaIkon + '"></i>'
            + '<div class="fw-bold" style="color:' + gk.hex + ';font-size:1.15rem">' + grade + '</div>'
            + '<div class="small fw-semibold text-muted mt-1"><%= Common.getBahasaConfig("Kualifikasi") %></div>'
            + '</div></div>'

            // Tile 3: Ujian selesai
            + '<div class="col-12 col-md-4">'
            + '<div class="cbt-stat-tile-<%= rnd %> h-100 d-flex flex-column justify-content-center align-items-center">'
            + '<div class="rounded-circle d-inline-flex align-items-center justify-content-center mb-2"'
            +   ' style="width:52px;height:52px;background:#dcfce7">'
            + '<i class="fas fa-circle-check fa-xl" style="color:#16a34a"></i>'
            + '</div>'
            + '<div class="fw-bold text-success" style="font-size:1.15rem">' + selesai.length
            +   ' ' + '<%= Common.getBahasaConfigJS("Ujian") %>' + '</div>'
            + '<div class="small fw-semibold text-muted mt-1"><%= Common.getBahasaConfig("Telah Diselesaikan") %></div>'
            + '</div></div>'

            + '</div>';

        // ── Progress bar per komponen ──────────────────────────────
        var progressSeksi = '';
        if (selesai.length > 0) {
            progressSeksi += '<div class="mb-4">'
                + '<div class="d-flex align-items-center gap-2 mb-3">'
                + '<i class="fas fa-chart-bar" style="color:' + gk.hex + '"></i>'
                + '<h6 class="fw-bold mb-0"><%= Common.getBahasaConfig("Nilai Per Komponen Evaluasi") %></h6>'
                + '</div>';

            for (var i = 0; i < selesai.length; i++) {
                var ex    = selesai[i];
                var nilai = typeof ex.nilai === 'number' ? ex.nilai : 0;
                var pct   = Math.min(100, Math.max(0, nilai));
                var bar   = warnaBar(nilai);
                var lbl   = labelGrade(nilai);
                progressSeksi +=
                    '<div class="mb-3">'
                    + '<div class="d-flex justify-content-between align-items-center mb-1">'
                    +   '<span class="small fw-semibold text-dark" style="max-width:60%;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">' + ex.nama + '</span>'
                    +   '<div class="d-flex align-items-center gap-2">'
                    +     '<span class="badge rounded-pill" style="background:' + bar + ';font-size:.62rem;padding:.25em .6em">' + lbl + '</span>'
                    +     '<span class="fw-bold small" style="color:' + bar + ';min-width:36px;text-align:right">' + nilai.toFixed(1) + '</span>'
                    +   '</div>'
                    + '</div>'
                    + '<div class="cbt-progress-<%= rnd %>">'
                    +   '<div class="cbt-progress-bar-<%= rnd %>"'
                    +      ' style="width:0%;background:' + bar + '"'
                    +      ' data-target="' + pct + '"></div>'
                    + '</div>'
                    + '</div>';
            }
            progressSeksi += '</div>';
        }

        // ── Spider-web chart (min 3 komponen) ─────────────────────
        var radarSeksi = '';
        if (selesai.length >= 3) {
            var radarSvg = buatRadarSvg(selesai, gk.hex);
            radarSeksi =
                '<div class="mb-4">'
                + '<div class="d-flex align-items-center gap-2 mb-3">'
                +   '<i class="fas fa-spider" style="color:' + gk.hex + '"></i>'
                +   '<h6 class="fw-bold mb-0"><%= Common.getBahasaConfig("Peta Kemampuan Anda") %></h6>'
                + '</div>'
                + '<p class="small text-muted mb-3" style="line-height:1.5">'
                +   '<%= Common.getBahasaConfigJS("Grafik jaring laba-laba ini memperlihatkan seberapa kuat Anda di setiap bidang ujian. Semakin luas polanya, semakin merata dan tinggi nilai Anda.") %>'
                + '</p>'
                + '<div class="text-center bg-white border rounded-4 py-3" style="overflow-x:auto">'
                +   radarSvg
                + '</div>'
                + '</div>';
        }

        // ── Langkah selanjutnya ───────────────────────────────────
        var langkah =
            '<div class="p-3 rounded-3 d-flex align-items-start gap-3"'
            + ' style="background:#eff6ff;border:1px solid #bfdbfe">'
            + '<i class="fas fa-arrow-right-to-bracket mt-1" style="color:#1d4ed8;flex-shrink:0"></i>'
            + '<div>'
            + '<div class="fw-bold small mb-1" style="color:#1e40af"><%= Common.getBahasaConfig("Apa yang perlu dilakukan selanjutnya?") %></div>'
            + '<div class="small text-muted">' + (data.infoLulus || '<%= Common.getBahasaConfigJS("Ikuti pengumuman dan pantau status pendaftaran Anda secara berkala.") %>') + '</div>'
            + '</div></div>';

        // ── Rakit panel penuh ─────────────────────────────────────
        return '<div class="rounded-4 mb-4 p-4"'
            + ' style="border:1.5px solid ' + gk.hex + '30;background:linear-gradient(135deg,' + gk.hex + '08 0%,#ffffff 60%)">'
            + bannerOtomatis
            + '<div class="d-flex align-items-center gap-3 mb-4">'
            +   '<div class="rounded-circle d-flex align-items-center justify-content-center"'
            +      ' style="width:52px;height:52px;flex-shrink:0;background:' + gk.hex + '18">'
            +     '<i class="fas fa-graduation-cap fa-lg" style="color:' + gk.hex + '"></i>'
            +   '</div>'
            +   '<div>'
            +     '<h5 class="fw-bold mb-0"><%= Common.getBahasaConfig("Laporan Hasil Evaluasi Seleksi") %></h5>'
            +     '<span class="badge mt-1" style="background:' + gk.hex + '">'
            +       '<i class="fas fa-circle-check me-1"></i><%= Common.getBahasaConfig("Semua Komponen Selesai") %>'
            +     '</span>'
            +   '</div>'
            + '</div>'
            + tileStat
            + progressSeksi
            + radarSeksi
            + langkah
            + '</div>';
    }

    // ─────────────────────────────────────────────────────────────────
    // FUNGSI 1: Popup Konfirmasi Mulai Ujian
    // Memastikan peserta benar-benar siap sebelum sesi ujian dibuka.
    // ─────────────────────────────────────────────────────────────────
    window.konfirmasiMulaiUjian<%= rnd %> = function (id) {
        var mid = 'modalKonfirmasi<%= rnd %>';
        var existing = document.getElementById(mid);
        if (existing) { existing.remove(); }

        var html =
            '<div class="modal fade" id="' + mid + '" tabindex="-1" aria-hidden="true" style="z-index:1080">'
          +   '<div class="modal-dialog modal-dialog-centered">'
          +     '<div class="modal-content rounded-4 border-0 shadow-lg">'
          +       '<div class="modal-body p-5 text-center">'
          +         '<div class="mb-4">'
          +           '<span class="p-4 rounded-circle d-inline-block" style="background:#fef3c7">'
          +             '<i class="fas fa-triangle-exclamation fa-3x text-warning"></i>'
          +           '</span>'
          +         '</div>'
          +         '<h5 class="fw-bold mb-2"><%= Common.getBahasaConfig("Konfirmasi Mulai Ujian") %></h5>'
          +         '<p class="text-muted mb-4"><%= Common.getBahasaConfig("Apakah Anda yakin ingin memulai ujian ini sekarang? Pastikan koneksi internet Anda dalam kondisi stabil.") %></p>'
          +         '<div class="d-flex justify-content-center gap-2">'
          +           '<button type="button" class="btn btn-light rounded-pill px-4 fw-bold" data-bs-dismiss="modal">'
          +             '<%= Common.getBahasaConfigJS("Batalkan") %>'
          +           '</button>'
          +           '<button type="button" class="btn btn-primary rounded-pill px-4 fw-bold shadow-sm"'
          +             ' onclick="window.eksekusiMulaiUjian<%= rnd %>(this,' + id + ')">'
          +             '<i class="fas fa-circle-play me-2"></i><%= Common.getBahasaConfig("Konfirmasi Mulai") %>'
          +           '</button>'
          +         '</div>'
          +       '</div>'
          +     '</div>'
          +   '</div>'
          + '</div>';

        document.body.insertAdjacentHTML('beforeend', html);
        new bootstrap.Modal(document.getElementById(mid), { backdrop: 'static' }).show();
    };

    // ─────────────────────────────────────────────────────────────────
    // FUNGSI 2: Eksekusi → Buka Lembar Ujian
    // ─────────────────────────────────────────────────────────────────
    window.eksekusiMulaiUjian<%= rnd %> = function (btn, idUjian) {
        if (btn) {
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%= Common.getBahasaConfig("Mempersiapkan...") %>';
            btn.disabled  = true;
        }
        var el = document.getElementById('modalKonfirmasi<%= rnd %>');
        if (el) {
            var inst = bootstrap.Modal.getInstance(el);
            if (inst) { inst.hide(); }
        }
        setTimeout(function () { window.bukaPopupUjian<%= rnd %>(idUjian); }, 400);
    };

    // ─────────────────────────────────────────────────────────────────
    // FUNGSI 3: Popup Lembar Ujian (Fullscreen Iframe-less Inject)
    // ─────────────────────────────────────────────────────────────────
    window.bukaPopupUjian<%= rnd %> = async function (idUjian) {
        var mid    = 'modalPopupUjianFull<%= rnd %>';
        var oldEl  = document.getElementById(mid);
        if (oldEl) { oldEl.remove(); }
        setModeUjianPmb(true);

        var targetUrl = '<%= Common.ROOT %>/baru?hanya_tampil_jsp=true&p=elearning%2Fujian&s=ujian&ppu='
            + idUjian + '&biodataCalonMahasiswa=' + camaId;

        var html =
            '<div class="modal fade cbt-modal-fs-<%= rnd %>" id="' + mid + '"'
          +   ' data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1">'
          +   '<div class="modal-dialog modal-fullscreen">'
          +     '<div class="modal-content border-0 bg-dark">'
          +       '<div class="modal-header bg-dark text-white border-0 py-3 px-4 d-flex justify-content-between align-items-center">'
          +         '<h5 class="modal-title fw-bold mb-0 text-truncate pe-3" style="max-width:75%">'
          +           '<i class="fas fa-laptop-code me-2 text-warning"></i><%= Common.getBahasaConfig("Lembar Evaluasi Online") %>'
          +         '</h5>'
          +         '<button type="button" class="btn btn-danger btn-sm rounded-pill px-4 fw-bold shadow-sm" data-bs-dismiss="modal">'
          +           '<i class="fas fa-circle-xmark me-2"></i><%= Common.getBahasaConfig("Selesai & Tutup") %>'
          +         '</button>'
          +       '</div>'
          +       '<div class="modal-body p-0 bg-white d-flex flex-column" style="overflow:auto" id="ujianInject' + mid + '">'
          +         '<div class="d-flex justify-content-center align-items-center flex-grow-1 h-100">'
          +           '<div class="text-center text-muted py-5">'
          +             '<div class="spinner-border text-primary mb-3" role="status" style="width:3rem;height:3rem"></div>'
          +             '<h5 class="fw-bold"><%= Common.getBahasaConfig("Menyinkronkan Lembar Ujian...") %></h5>'
          +           '</div>'
          +         '</div>'
          +       '</div>'
          +     '</div>'
          +   '</div>'
          + '</div>';

        document.body.insertAdjacentHTML('beforeend', html);
        var modalNode = document.getElementById(mid);
        var modal     = new bootstrap.Modal(modalNode);

        modalNode.addEventListener('hidden.bs.modal', function () {
            setModeUjianPmb(false);
            this.remove();
            window.loadExamData<%= rnd %>();
        });
        modal.show();

        try {
            var res = await fetch(targetUrl);
            if (!res.ok) { throw new Error('<%= Common.getBahasaConfigJS("Gagal mengambil modul ujian dari peladen.") %>'); }
            var htmlText  = await res.text();
            var container = document.getElementById('ujianInject' + mid);
            if (!container) { return; }
            container.innerHTML = htmlText;
            // Re-execute <script> tags (diinjeksi sebagai teks biasa, perlu di-clone ulang)
            var scripts = Array.from(container.getElementsByTagName('script'));
            for (var s = 0; s < scripts.length; s++) {
                var old    = scripts[s];
                var newScr = document.createElement('script');
                Array.from(old.attributes).forEach(function (attr) {
                    if (attr.name.toLowerCase() !== 'type') { newScr.setAttribute(attr.name, attr.value); }
                });
                newScr.type = 'text/javascript';
                var srcEff = old.src || old.getAttribute('data-rocketlazyloadscript') || '';
                if (srcEff) {
                    newScr.src = srcEff;
                    document.body.appendChild(newScr);
                } else {
                    newScr.text = old.innerHTML;
                    document.body.appendChild(newScr).parentNode.removeChild(newScr);
                }
            }
        } catch (err) {
            var container = document.getElementById('ujianInject' + mid);
            if (container) {
                container.innerHTML =
                    '<div class="p-5 text-center text-danger d-flex flex-column justify-content-center align-items-center h-100">'
                  + '<i class="fas fa-triangle-exclamation fa-4x mb-3 opacity-50"></i>'
                  + '<h5 class="fw-bold"><%= Common.getBahasaConfig("Terjadi kesalahan sistem saat memuat lembar ujian.") %></h5>'
                  + '<p class="text-muted">' + err.message + '</p>'
                  + '</div>';
            }
        }
    };

    // ─────────────────────────────────────────────────────────────────
    // FUNGSI 4: Muat & Render Daftar Evaluasi
    // ─────────────────────────────────────────────────────────────────
    window.loadExamData<%= rnd %> = async function () {
        examBody.innerHTML =
            '<div class="text-center py-5">'
          + '<div class="spinner-grow text-primary mb-3" role="status"></div>'
          + '<div class="fw-bold text-muted"><%= Common.getBahasaConfig("Menyinkronkan data evaluasi...") %></div>'
          + '</div>';

        try {
            var url  = '<%= Common.ROOT %>/pmb?hanya_tampil_jsp=true&p=pmb&s=_ikut_ujian_online_service&action=fetch_exams&id=' + camaId;
            var res  = await fetch(url);
            var data = await res.json();

            // ── Gate: berkas belum diunggah ────────────────────────
            if (data.status === 'berkas_kurang') {
                examBody.innerHTML =
                    '<div class="rounded-4 p-4" style="border-left:4px solid #f59e0b;background:#fffbeb">'
                  + '<div class="d-flex align-items-start gap-3">'
                  + '<i class="fas fa-triangle-exclamation fa-2x text-warning mt-1 flex-shrink-0"></i>'
                  + '<div>'
                  + '<div class="fw-bold text-warning mb-2"><%= Common.getBahasaConfig("Berkas Wajib Belum Diunggah") %></div>'
                  + '<p class="mb-0 small text-dark" style="white-space:pre-line;line-height:1.8">' + (data.message || '') + '</p>'
                  + '</div></div></div>';
                return;
            }

            // ── Error lainnya ──────────────────────────────────────
            if (data.status !== 'success') {
                examBody.innerHTML =
                    '<div class="text-center py-4 text-danger">'
                  + '<i class="fas fa-circle-exclamation fa-2x mb-2 d-block opacity-50"></i>'
                  + '<div class="small">' + (data.message || '<%= Common.getBahasaConfigJS("Terjadi kesalahan.") %>') + '</div>'
                  + '</div>';
                return;
            }

            // ── Kosong ────────────────────────────────────────────
            if (!data.exams || data.exams.length === 0) {
                examBody.innerHTML =
                    '<div class="text-center py-5 text-muted bg-light rounded-4">'
                  + '<i class="fas fa-folder-open fa-3x mb-3 d-block opacity-25"></i>'
                  + '<div class="fw-bold"><%= Common.getBahasaConfig("Belum terdapat jadwal evaluasi yang aktif untuk akun Anda.") %></div>'
                  + '</div>';
                return;
            }

            // ── Render rows ────────────────────────────────────────
            examBody.innerHTML = '';
            var allAtLeastOnce = true;

            data.exams.forEach(function (ex, idx) {
                var tglMulai   = ex.tanggalMulai  || 0;
                var tglSelesai = ex.tanggalSelesai || 9999999999999;
                var maks       = ex.maxPercobaan   || 1;
                var ikut       = ex.jumlahIkut     || 0;
                var sisa       = Math.max(0, maks - ikut);

                if (ikut === 0) { allAtLeastOnce = false; }

                // Tombol aksi
                var btnHtml;
                if (nowMs > tglSelesai) {
                    btnHtml = '<span class="badge rounded-pill border border-danger text-danger" style="background:#fff0f0;padding:.5em .85em">'
                            + '<i class="far fa-clock me-1"></i><%= Common.getBahasaConfig("Waktu Berakhir") %></span>';
                } else if (nowMs < tglMulai) {
                    btnHtml = '<span class="badge rounded-pill border border-secondary text-secondary" style="background:#f8f9fa;padding:.5em .85em">'
                            + '<i class="fas fa-lock me-1"></i><%= Common.getBahasaConfig("Belum Dibuka") %></span>';
                } else if (sisa > 0) {
                    var lbl   = ikut > 0 ? '<%= Common.getBahasaConfigJS("Ikut Kembali") %>' : '<%= Common.getBahasaConfigJS("Mulai Ujian") %>';
                    var theme = ikut > 0 ? 'btn-outline-primary' : 'btn-primary';
                    btnHtml = '<button class="btn ' + theme + ' btn-sm py-2 px-4 rounded-pill fw-bold w-100 w-md-auto shadow-sm"'
                            + ' onclick="window.konfirmasiMulaiUjian<%= rnd %>(' + ex.id + ')">'
                            + '<i class="fas fa-circle-play me-2"></i>' + lbl + '</button>';
                } else {
                    btnHtml = '<span class="badge rounded-pill border border-success text-success" style="background:#f0fdf4;padding:.5em .85em">'
                            + '<i class="fas fa-check-double me-1"></i><%= Common.getBahasaConfig("Kuota Habis") %></span>';
                }

                // Badge status pengerjaan
                var badgeStatus = ikut > 0
                    ? '<span class="badge rounded-pill bg-success py-2 px-3">'
                      + '<i class="fas fa-circle-check me-1"></i>' + ikut + 'x <small><%= Common.getBahasaConfig("Diselesaikan") %></small></span>'
                    : '<span class="badge rounded-pill bg-warning text-dark py-2 px-3">'
                      + '<i class="fas fa-hourglass-half me-1"></i><%= Common.getBahasaConfig("Menunggu") %></span>';

                // Nilai (jika sudah ada)
                var nilaiTag = '';
                if (ex.nilai !== undefined && ex.nilai !== null) {
                    var nb = warnaBar(ex.nilai);
                    nilaiTag = '<span class="badge ms-2 rounded-pill" style="background:' + nb + ';font-size:.65rem">'
                             + ex.nilai.toFixed(1) + ' poin</span>';
                }

                var row =
                    '<div class="cbt-hover-<%= rnd %> rounded-4 mb-2">'
                  + '<div class="p-3 p-md-4">'
                  + '<div class="row align-items-center gy-3">'

                  // #
                  + '<div class="col-md-1 text-md-center d-none d-md-block">'
                  +   '<span class="fw-bold text-muted" style="opacity:.4">' + (idx + 1) + '</span>'
                  + '</div>'

                  // Nama
                  + '<div class="col-12 col-md-3">'
                  +   '<div class="small text-muted d-md-none mb-1 text-uppercase fw-bold" style="font-size:.68rem"><%= Common.getBahasaConfig("Mata Evaluasi") %></div>'
                  +   '<div class="fw-bold text-dark">' + ex.nama + nilaiTag + '</div>'
                  + '</div>'

                  // Jadwal
                  + '<div class="col-12 col-md-3">'
                  +   '<div class="small text-muted d-md-none mb-1 text-uppercase fw-bold" style="font-size:.68rem"><%= Common.getBahasaConfig("Jadwal") %></div>'
                  +   '<div class="small text-muted mb-1"><i class="far fa-calendar-days me-2 text-primary"></i>' + (ex.tglMulai || '-') + '</div>'
                  +   '<div class="small text-muted"><i class="far fa-calendar-check me-2 text-danger"></i>' + (ex.tglSelesai || '-') + '</div>'
                  + '</div>'

                  // Kuota
                  + '<div class="col-6 col-md-2 text-md-center">'
                  +   '<div class="small text-muted d-md-none mb-1 text-uppercase fw-bold" style="font-size:.68rem"><%= Common.getBahasaConfig("Kuota") %></div>'
                  +   '<div class="fw-bold">' + ikut + ' / ' + maks + '</div>'
                  +   '<div class="small fw-semibold ' + (sisa > 0 ? 'text-success' : 'text-danger') + '"><%= Common.getBahasaConfig("Sisa") %>: ' + sisa + '</div>'
                  + '</div>'

                  // Status
                  + '<div class="col-6 col-md-2 text-md-center">'
                  +   '<div class="small text-muted d-md-none mb-1 text-uppercase fw-bold" style="font-size:.68rem"><%= Common.getBahasaConfig("Status") %></div>'
                  +   badgeStatus
                  + '</div>'

                  // Opsi
                  + '<div class="col-12 col-md-1 text-md-end mt-3 mt-md-0 d-grid d-md-block border-top border-md-0 pt-3 pt-md-0">'
                  +   btnHtml
                  + '</div>'

                  + '</div></div></div>';

                examBody.insertAdjacentHTML('beforeend', row);
            });

            // ── Panel hasil (semua selesai) ────────────────────────
            if (data.allFinished && allAtLeastOnce) {
                hasilPanel.style.display = 'block';
                hasilPanel.innerHTML     = buatHasilPanel(data);
                // Animasi progress bar (dipicu setelah render)
                setTimeout(function () {
                    var bars = hasilPanel.querySelectorAll('[data-target]');
                    for (var b = 0; b < bars.length; b++) {
                        bars[b].style.width = bars[b].getAttribute('data-target') + '%';
                    }
                }, 120);
            } else {
                hasilPanel.style.display = 'none';
                hasilPanel.innerHTML     = '';
            }

        } catch (e) {
            examBody.innerHTML =
                '<div class="text-center text-danger p-5 rounded-4" style="background:#fef2f2">'
              + '<i class="fas fa-triangle-exclamation fa-2x mb-3 opacity-50 d-block"></i>'
              + '<div class="fw-bold"><%= Common.getBahasaConfig("Gagal menyinkronkan data dengan peladen.") %></div>'
              + '<div class="small text-muted mt-1">' + (e.message || '') + '</div>'
              + '</div>';
        }
    };

    // Inisialisasi saat modul pertama kali dimuat
    window.loadExamData<%= rnd %>();

})();
</script>
