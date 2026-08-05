<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
    return;
}
String rnd = Common.getGeneratedBarCode(7);
boolean isMahasiswaAtauSiswa = (tbmuser.getSiswa() != null || tbmuser.getMahasiswa() != null);
%>

<style>
#dsbSurat<%=rnd%> .dsb-kpi { border-radius:14px; transition:box-shadow .2s; }
#dsbSurat<%=rnd%> .dsb-kpi:hover { box-shadow:0 8px 24px rgba(0,0,0,.12) !important; }
#dsbSurat<%=rnd%> .dsb-bar { height:28px; border-radius:6px; transition:width 1s ease; min-width:4px; }
#dsbSurat<%=rnd%> .dsb-nav-pill { border-radius:999px; font-weight:600; font-size:13px; padding:.42rem 1.1rem; }
#dsbSurat<%=rnd%> .dsb-nav-pill.active { background:var(--ais-theme-primary,#2563eb); color:#fff; }
#dsbSurat<%=rnd%> .dsb-tab-pane .card { border-radius:16px; }
/* Pastikan lebar 100% di HP */
@media (max-width: 575px) {
    #dsbSurat<%=rnd%> .dsb-tabs-scroll { overflow-x:auto; flex-wrap:nowrap !important; }
}
</style>

<div id="dsbSurat<%=rnd%>" class="container-fluid animate__animated animate__fadeIn px-0 px-sm-2">

    <!-- ═══════════════════════════════════════════
         HEADER DASBOR
    ═══════════════════════════════════════════ -->
    <div class="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-2 mb-3 px-1">
        <div>
            <h5 class="fw-bold text-dark mb-0">
                <i class="fas fa-envelope-open-text me-2" style="color:var(--ais-theme-primary,#2563eb);"></i>
                <%=Common.getBahasaConfig("Dasbor Surat Menyurat")%>
            </h5>
            <div class="text-muted mt-1" style="font-size:12.5px;">
                Pantau pengajuan dan persetujuan surat dalam satu tempat.
            </div>
        </div>
        <button class="btn btn-sm btn-outline-secondary rounded-pill px-3" onclick="dsbMuatAnalitik<%=rnd%>()"
                title="Perbarui data ringkasan" style="font-size:13px;">
            <i class="fas fa-sync-alt me-1"></i>Perbarui
        </button>
    </div>

    <% if (!isMahasiswaAtauSiswa) { %>
    <!-- ═══════════════════════════════════════════
         RINGKASAN STATISTIK CEPAT (hanya staf)
    ═══════════════════════════════════════════ -->
    <div class="row g-2 mb-3" id="dsbKpiRow<%=rnd%>">
        <!-- KPI 1: Surat Keluar Menunggu -->
        <div class="col-6 col-lg-3">
            <div class="dsb-kpi p-3 bg-white shadow-sm h-100" style="border-top:3px solid #2563eb;">
                <div class="d-flex justify-content-between align-items-start">
                    <div>
                        <div class="text-muted" style="font-size:11.5px;font-weight:600;">Keluar Menunggu</div>
                        <div class="fs-2 fw-bold text-primary lh-1 mt-1" id="dsbKelMenunggu<%=rnd%>">
                            <span class="spinner-border spinner-border-sm text-primary"></span>
                        </div>
                        <div class="mt-1" style="font-size:11px;color:#94a3b8;">persetujuan diperlukan</div>
                    </div>
                    <div class="rounded-circle d-flex align-items-center justify-content-center"
                         style="width:40px;height:40px;background:#eff6ff;flex-shrink:0;">
                        <i class="fas fa-paper-plane text-primary"></i>
                    </div>
                </div>
            </div>
        </div>
        <!-- KPI 2: Surat Masuk Menunggu -->
        <div class="col-6 col-lg-3">
            <div class="dsb-kpi p-3 bg-white shadow-sm h-100" style="border-top:3px solid #0d9488;">
                <div class="d-flex justify-content-between align-items-start">
                    <div>
                        <div class="text-muted" style="font-size:11.5px;font-weight:600;">Masuk Menunggu</div>
                        <div class="fs-2 fw-bold lh-1 mt-1" style="color:#0d9488;" id="dsbMasMenunggu<%=rnd%>">
                            <span class="spinner-border spinner-border-sm" style="color:#0d9488;"></span>
                        </div>
                        <div class="mt-1" style="font-size:11px;color:#94a3b8;">perlu disposisi</div>
                    </div>
                    <div class="rounded-circle d-flex align-items-center justify-content-center"
                         style="width:40px;height:40px;background:#f0fdfa;flex-shrink:0;">
                        <i class="fas fa-inbox" style="color:#0d9488;"></i>
                    </div>
                </div>
            </div>
        </div>
        <!-- KPI 3: Total Keluar (semua) -->
        <div class="col-6 col-lg-3">
            <div class="dsb-kpi p-3 bg-white shadow-sm h-100" style="border-top:3px solid #7c3aed;">
                <div class="d-flex justify-content-between align-items-start">
                    <div>
                        <div class="text-muted" style="font-size:11.5px;font-weight:600;">Total Alur Keluar</div>
                        <div class="fs-2 fw-bold lh-1 mt-1" style="color:#7c3aed;" id="dsbKelTotal<%=rnd%>">
                            <span class="spinner-border spinner-border-sm" style="color:#7c3aed;"></span>
                        </div>
                        <div class="mt-1" style="font-size:11px;color:#94a3b8;">sejak awal sistem</div>
                    </div>
                    <div class="rounded-circle d-flex align-items-center justify-content-center"
                         style="width:40px;height:40px;background:#f5f3ff;flex-shrink:0;">
                        <i class="fas fa-file-export" style="color:#7c3aed;"></i>
                    </div>
                </div>
            </div>
        </div>
        <!-- KPI 4: Total Masuk (semua) -->
        <div class="col-6 col-lg-3">
            <div class="dsb-kpi p-3 bg-white shadow-sm h-100" style="border-top:3px solid #dc2626;">
                <div class="d-flex justify-content-between align-items-start">
                    <div>
                        <div class="text-muted" style="font-size:11.5px;font-weight:600;">Total Alur Masuk</div>
                        <div class="fs-2 fw-bold text-danger lh-1 mt-1" id="dsbMasTotal<%=rnd%>">
                            <span class="spinner-border spinner-border-sm text-danger"></span>
                        </div>
                        <div class="mt-1" style="font-size:11px;color:#94a3b8;">sejak awal sistem</div>
                    </div>
                    <div class="rounded-circle d-flex align-items-center justify-content-center"
                         style="width:40px;height:40px;background:#fff1f2;flex-shrink:0;">
                        <i class="fas fa-file-import text-danger"></i>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- ═══════════════════════════════════════════
         PANEL ANALITIK VISUAL
    ═══════════════════════════════════════════ -->
    <div class="row g-2 mb-3">
        <!-- Progress persetujuan keluar -->
        <div class="col-md-6">
            <div class="bg-white rounded-3 border shadow-sm p-3 h-100">
                <div class="fw-bold mb-1" style="font-size:13.5px;color:#0f172a;">
                    <i class="fas fa-check-double me-1 text-primary"></i>Progres Persetujuan Surat Keluar
                </div>
                <div style="font-size:11.5px;color:#64748b;margin-bottom:10px;">
                    Seberapa banyak surat yang sudah diselesaikan vs yang masih menunggu.
                </div>
                <div id="dsbBarKeluar<%=rnd%>">
                    <div class="text-muted small text-center py-2"><i class="fas fa-circle-notch fa-spin me-1"></i>Memuat...</div>
                </div>
            </div>
        </div>
        <!-- Progress disposisi masuk -->
        <div class="col-md-6">
            <div class="bg-white rounded-3 border shadow-sm p-3 h-100">
                <div class="fw-bold mb-1" style="font-size:13.5px;color:#0f172a;">
                    <i class="fas fa-share-alt me-1" style="color:#0d9488;"></i>Progres Disposisi Surat Masuk
                </div>
                <div style="font-size:11.5px;color:#64748b;margin-bottom:10px;">
                    Gambaran surat masuk yang sudah didisposisi dan yang masih perlu ditangani.
                </div>
                <div id="dsbBarMasuk<%=rnd%>">
                    <div class="text-muted small text-center py-2"><i class="fas fa-circle-notch fa-spin me-1"></i>Memuat...</div>
                </div>
            </div>
        </div>
    </div>
    <% } %>

    <!-- ═══════════════════════════════════════════
         TAB NAVIGASI
    ═══════════════════════════════════════════ -->
    <ul class="nav mb-3 bg-white p-2 rounded-4 shadow-sm border dsb-tabs-scroll d-flex flex-row gap-1"
        id="dsbTab<%=rnd%>" role="tablist" style="flex-wrap:wrap;">
        <li class="nav-item" role="presentation">
            <button class="nav-link dsb-nav-pill active" id="dsbTabPengajuan<%=rnd%>"
                    data-bs-toggle="pill" data-bs-target="#dsbPanePengajuan<%=rnd%>"
                    type="button" role="tab" aria-selected="true"
                    onclick="dsbMuatLayanan<%=rnd%>('_pengajuan_baru','dsbKontenPengajuan<%=rnd%>')">
                <i class="fas fa-paper-plane me-1"></i><%=Common.getBahasaConfig("Pengajuan Surat Anda")%>
            </button>
        </li>
        <% if (!isMahasiswaAtauSiswa) { %>
        <li class="nav-item" role="presentation">
            <button class="nav-link dsb-nav-pill ms-1" id="dsbTabKeluar<%=rnd%>"
                    data-bs-toggle="pill" data-bs-target="#dsbPaneKeluar<%=rnd%>"
                    type="button" role="tab" aria-selected="false"
                    onclick="dsbMuatLayanan<%=rnd%>('_menunggu_disposisi','dsbKontenKeluar<%=rnd%>')">
                <i class="fas fa-file-export me-1"></i><%=Common.getBahasaConfig("Persetujuan Surat Keluar")%>
                <span class="badge ms-1 rounded-pill" id="dsbBadgeKeluar<%=rnd%>"
                      style="background:#f59e0b;color:#fff;font-size:10px;display:none;"></span>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link dsb-nav-pill ms-1" id="dsbTabMasuk<%=rnd%>"
                    data-bs-toggle="pill" data-bs-target="#dsbPaneMasuk<%=rnd%>"
                    type="button" role="tab" aria-selected="false"
                    onclick="dsbMuatLayanan<%=rnd%>('_menunggu_disposisi_masuk','dsbKontenMasuk<%=rnd%>')">
                <i class="fas fa-file-import me-1"></i><%=Common.getBahasaConfig("Disposisi Surat Masuk")%>
                <span class="badge ms-1 rounded-pill" id="dsbBadgeMasuk<%=rnd%>"
                      style="background:#d97706;color:#fff;font-size:10px;display:none;"></span>
            </button>
        </li>
        <% } %>
    </ul>

    <!-- ═══════════════════════════════════════════
         ISI TAB
    ═══════════════════════════════════════════ -->
    <div class="tab-content" id="dsbTabContent<%=rnd%>">
        <div class="tab-pane fade show active dsb-tab-pane" id="dsbPanePengajuan<%=rnd%>" role="tabpanel" tabindex="0">
            <div class="card border-0 shadow-sm">
                <div class="card-body p-3 p-md-4" id="dsbKontenPengajuan<%=rnd%>">
                    <div class="text-center py-5 text-muted">
                        <i class="fas fa-circle-notch fa-spin fa-2x mb-3 d-block opacity-50"></i>
                        <div class="fw-semibold small">Memuat pengajuan surat...</div>
                    </div>
                </div>
            </div>
        </div>

        <% if (!isMahasiswaAtauSiswa) { %>
        <div class="tab-pane fade dsb-tab-pane" id="dsbPaneKeluar<%=rnd%>" role="tabpanel" tabindex="0">
            <div class="card border-0 shadow-sm">
                <div class="card-body p-3 p-md-4" id="dsbKontenKeluar<%=rnd%>"></div>
            </div>
        </div>
        <div class="tab-pane fade dsb-tab-pane" id="dsbPaneMasuk<%=rnd%>" role="tabpanel" tabindex="0">
            <div class="card border-0 shadow-sm">
                <div class="card-body p-3 p-md-4" id="dsbKontenMasuk<%=rnd%>"></div>
            </div>
        </div>
        <% } %>
    </div>
</div>

<!-- Modal form surat (full screen on mobile) -->
<style>
.dsb-modal-fullscreen @media (max-width:767px) { max-width:100%; margin:0; }
.dsb-modal-fullscreen .modal-content @media (max-width:767px) { min-height:100vh; border-radius:0; }
</style>

<script>
/* ===== FUNGSI GLOBAL BUKA FORM SURAT DARI DASBOR ===== */
var bukaFormSuratDashboard = async function(idSurat) {
    var idModal = 'dsbModalFormSurat<%=rnd%>';
    var rndForm = Math.floor(Math.random() * 100000);

    if (!document.getElementById(idModal)) {
        document.body.insertAdjacentHTML('beforeend',
            '<div class="modal fade" id="' + idModal + '" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">' +
            '<div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">' +
            '<div class="modal-content border-0 shadow-lg rounded-4">' +
            '<div class="modal-header text-white border-0 rounded-top-4" style="background:linear-gradient(135deg,#1a56db,#2563eb);">' +
            '<h5 class="modal-title fw-bold text-white"><i class="fas fa-pen-square me-2"></i><%=Common.getBahasaConfig("Formulir Surat Keluar")%></h5>' +
            '<button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button></div>' +
            '<div class="modal-body p-0" id="dsbModalFormSuratBody<%=rnd%>"></div>' +
            '</div></div></div>');
    }

    var modalEl = document.getElementById(idModal);
    var bodi    = document.getElementById('dsbModalFormSuratBody<%=rnd%>');
    var myModal = new bootstrap.Modal(modalEl);
    bodi.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><div class="fw-bold text-muted small"><%=Common.getBahasaConfig("Sedang menyiapkan formulir...")%></div></div>';
    myModal.show();

    try {
        var url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=pagesmastersuratsuratkeluarzul&s=edit&id=' + (idSurat || '') + '&rnd=' + rndForm;
        var resp = await fetch(url);
        if (!resp.ok) throw new Error('Gagal');
        bodi.innerHTML = await resp.text();
        bodi.querySelectorAll('script').forEach(function(s) {
            var n = document.createElement('script');
            Array.from(s.attributes).forEach(function(a) { if (a.name.toLowerCase() !== 'type') n.setAttribute(a.name, a.value); });
            n.type = 'text/javascript';
            n.appendChild(document.createTextNode(s.innerHTML));
            s.parentNode.replaceChild(n, s);
        });
        window['berhasilSimpanSuratKeluar' + rndForm] = function() {
            myModal.hide();
            if (typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Data berhasil disimpan.")%>', 'bg-success text-white');
            setTimeout(function() { location.reload(); }, 1000);
        };
    } catch (e) {
        bodi.innerHTML = '<div class="alert alert-danger m-4 rounded-3"><i class="fas fa-exclamation-triangle me-2"></i><%=Common.getBahasaConfig("Gagal memuat formulir.")%></div>';
    }
};

/* ===== MUAT SUB-MODUL LAYANAN ===== */
window['dsbMuatLayanan<%=rnd%>'] = async function(namaLayanan, targetId) {
    var el = document.getElementById(targetId);
    if (!el || el.dataset.loaded === 'true') return;
    el.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-primary mb-3"></div>' +
        '<div class="fw-bold text-muted small"><i class="fas fa-cloud-download-alt me-1"></i><%=Common.getBahasaConfig("Sedang memuat layanan...")%></div></div>';

    var url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=suratmenyurat%2Fservices&s=' + namaLayanan + '&rnd=<%=rnd%>';
    try {
        var r = await fetch(url);
        if (!r.ok) throw new Error('');
        el.innerHTML = await r.text();
        el.dataset.loaded = 'true';
        el.querySelectorAll('script').forEach(function(s) {
            var n = document.createElement('script');
            Array.from(s.attributes).forEach(function(a) { if (a.name.toLowerCase() !== 'type') n.setAttribute(a.name, a.value); });
            n.type = 'text/javascript';
            n.appendChild(document.createTextNode(s.innerHTML));
            s.parentNode.replaceChild(n, s);
        });
    } catch (e) {
        el.innerHTML = '<div class="alert alert-danger text-center rounded-3 m-2"><i class="fas fa-exclamation-triangle me-2"></i><%=Common.getBahasaConfig("Gagal memuat layanan. Silakan muat ulang halaman.")%></div>';
    }
};

/* ===== ANALITIK DASBOR ===== */
window['dsbMuatAnalitik<%=rnd%>'] = async function() {
    <% if (!isMahasiswaAtauSiswa) { %>
    const ROOT = '<%=Common.ROOT%>';
    const fetchCount = async (cls, where) => {
        try {
            const r = await fetch(ROOT + '/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: 'daftar', class: cls, alias1: cls.indexOf('Masuk') >= 0 ? 'suratMasuk' : 'suratKeluar',
                    deep: '1', max: 1, halaman: 0, count: 'true', where1: where })
            });
            const d = await r.json();
            return parseInt(d.count || 0);
        } catch(e) { return 0; }
    };

    const CK = 'ais.database.model.surat.AlurPersetujuanSuratKeluarStatus';
    const CM = 'ais.database.model.surat.AlurPersetujuanSuratMasukStatus';
    const BASE = 'kodeUnik IS NOT NULL';

    const [kelMenunggu, kelDisetujui, kelDitolak, kelTotal,
           masMenunggu, masDisposisi, masDitolak, masTotal] = await Promise.all([
        fetchCount(CK, BASE + ' AND this_.disetujui = false AND this_.ditolak = false'),
        fetchCount(CK, BASE + ' AND this_.disetujui = true'),
        fetchCount(CK, BASE + ' AND this_.ditolak = true'),
        fetchCount(CK, BASE),
        fetchCount(CM, BASE + ' AND this_.disetujui = false AND this_.ditolak = false'),
        fetchCount(CM, BASE + ' AND this_.disetujui = true'),
        fetchCount(CM, BASE + ' AND this_.ditolak = true'),
        fetchCount(CM, BASE)
    ]);

    /* ─── KPI cards ─── */
    const set = (id, v) => { const el = document.getElementById(id); if (el) el.innerText = v; };
    set('dsbKelMenunggu<%=rnd%>', kelMenunggu);
    set('dsbMasMenunggu<%=rnd%>', masMenunggu);
    set('dsbKelTotal<%=rnd%>',    kelTotal);
    set('dsbMasTotal<%=rnd%>',    masTotal);

    /* Badge pada tab (tampilkan jika ada yang menunggu) */
    const bdgKel = document.getElementById('dsbBadgeKeluar<%=rnd%>');
    const bdgMas = document.getElementById('dsbBadgeMasuk<%=rnd%>');
    if (bdgKel && kelMenunggu > 0) { bdgKel.innerText = kelMenunggu; bdgKel.style.display = 'inline-block'; }
    if (bdgMas && masMenunggu > 0) { bdgMas.innerText = masMenunggu; bdgMas.style.display = 'inline-block'; }

    /* ─── Progress bar keluar ─── */
    const renderBar = (containerId, rows) => {
        const el = document.getElementById(containerId);
        if (!el) return;
        const total = rows.reduce((s, r) => s + r.v, 0) || 1;
        el.innerHTML = rows.map(r => {
            const pct = Math.round((r.v / total) * 100);
            return '<div style="margin-bottom:10px;">' +
                '<div style="display:flex;justify-content:space-between;font-size:12px;color:#475569;margin-bottom:4px;">' +
                '<span style="font-weight:600;">' + r.label + '</span>' +
                '<span style="font-weight:700;color:' + r.color + ';">' + r.v + ' <span style="color:#94a3b8;font-weight:400;">(' + pct + '%)</span></span></div>' +
                '<div style="height:10px;background:#f1f5f9;border-radius:999px;overflow:hidden;">' +
                '<div class="dsb-bar" style="width:' + pct + '%;background:' + r.color + ';"></div></div></div>';
        }).join('');
    };

    renderBar('dsbBarKeluar<%=rnd%>', [
        { label: 'Menunggu Persetujuan', v: kelMenunggu, color: '#f59e0b' },
        { label: 'Sudah Disetujui',      v: kelDisetujui, color: '#16a34a' },
        { label: 'Ditolak',              v: kelDitolak,   color: '#dc2626' }
    ]);
    renderBar('dsbBarMasuk<%=rnd%>', [
        { label: 'Menunggu Disposisi', v: masMenunggu,  color: '#d97706' },
        { label: 'Terdisposisi',       v: masDisposisi, color: '#0284c7' },
        { label: 'Ditolak',            v: masDitolak,   color: '#dc2626' }
    ]);
    <% } %>
};

/* ===== INIT ===== */
document.addEventListener('DOMContentLoaded', function() {
    dsbMuatLayanan<%=rnd%>('_pengajuan_baru', 'dsbKontenPengajuan<%=rnd%>');
    <% if (!isMahasiswaAtauSiswa) { %>
    dsbMuatAnalitik<%=rnd%>();
    <% } %>
});
</script>
