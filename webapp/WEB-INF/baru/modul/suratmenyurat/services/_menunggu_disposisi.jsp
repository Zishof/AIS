<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
String rnd = request.getParameter("rnd");
Tbmuser tbmuser = Common.getCurrentUser(request);
%>

<style>
#dkWrap<%=rnd%> .dk-stat-chip { cursor:pointer; transition:all .2s; border-radius:12px; }
#dkWrap<%=rnd%> .dk-stat-chip:hover { transform:translateY(-2px); box-shadow:0 6px 20px rgba(0,0,0,.12) !important; }
#dkWrap<%=rnd%> .dk-stat-chip.aktif { outline:3px solid; outline-offset:2px; }
#dkWrap<%=rnd%> .dk-stat-chip.aktif.c-biru { outline-color:#0d6efd; }
#dkWrap<%=rnd%> .dk-stat-chip.aktif.c-kuning { outline-color:#f59e0b; }
#dkWrap<%=rnd%> .dk-stat-chip.aktif.c-hijau { outline-color:#16a34a; }
#dkWrap<%=rnd%> .dk-stat-chip.aktif.c-merah { outline-color:#dc2626; }
#dkWrap<%=rnd%> .dk-baris-keluar { border-left:3px solid transparent; transition:background .15s; }
#dkWrap<%=rnd%> .dk-baris-keluar:hover { background:#f8fafc !important; }
#dkWrap<%=rnd%> .dk-baris-keluar.st-menunggu { border-left-color:#f59e0b; }
#dkWrap<%=rnd%> .dk-baris-keluar.st-disetujui { border-left-color:#16a34a; }
#dkWrap<%=rnd%> .dk-baris-keluar.st-ditolak { border-left-color:#dc2626; }
#dkWrap<%=rnd%> .dk-kartu { border-radius:12px; border-left:4px solid #dee2e6; transition:box-shadow .2s; }
#dkWrap<%=rnd%> .dk-kartu:hover { box-shadow:0 4px 14px rgba(0,0,0,.1) !important; }
#dkWrap<%=rnd%> .dk-kartu.st-menunggu { border-left-color:#f59e0b; }
#dkWrap<%=rnd%> .dk-kartu.st-disetujui { border-left-color:#16a34a; }
#dkWrap<%=rnd%> .dk-kartu.st-ditolak { border-left-color:#dc2626; }
@media (max-width:767px) {
    #dkWrap<%=rnd%> .dk-tabel-area { display:none; }
    #dkWrap<%=rnd%> .dk-kartu-area { display:block; }
}
@media (min-width:768px) {
    #dkWrap<%=rnd%> .dk-tabel-area { display:block; }
    #dkWrap<%=rnd%> .dk-kartu-area { display:none; }
}
</style>

<div id="dkWrap<%=rnd%>" class="animate__animated animate__fadeIn">

    <!-- Keterangan singkat untuk pengguna -->
    <div class="d-flex align-items-start gap-2 mb-3 px-3 py-2 rounded-3"
         style="background:#eff6ff;border:1px solid #bfdbfe;border-left:4px solid #3b82f6;">
        <i class="fas fa-paper-plane text-primary mt-1" style="font-size:14px;"></i>
        <div style="font-size:13px; color:#1e40af; line-height:1.55;">
            <strong>Surat Keluar &ndash; Antrian Persetujuan.</strong>
            Berikut daftar surat yang perlu Anda setujui. Klik <strong>Tindak Lanjuti</strong> untuk memproses surat yang masih menunggu keputusan.
        </div>
    </div>

    <!-- 4 Kartu Ringkasan Statistik -->
    <div class="row g-2 mb-3">
        <div class="col-6 col-md-3">
            <div class="dk-stat-chip c-biru shadow-sm p-3 bg-white"
                 style="border-top:3px solid #0d6efd;" onclick="dkFilterStatus<%=rnd%>('')"
                 id="dkChipTotal<%=rnd%>">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <div class="fs-3 fw-bold text-primary lh-1" id="dkTotal<%=rnd%>">–</div>
                        <div class="mt-1" style="font-size:11.5px;color:#64748b;font-weight:600;">Total Surat</div>
                    </div>
                    <div class="rounded-circle d-flex align-items-center justify-content-center"
                         style="width:38px;height:38px;background:#eff6ff;">
                        <i class="fas fa-envelope text-primary" style="font-size:15px;"></i>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-6 col-md-3">
            <div class="dk-stat-chip c-kuning shadow-sm p-3 bg-white"
                 style="border-top:3px solid #f59e0b;" onclick="dkFilterStatus<%=rnd%>('menunggu')"
                 id="dkChipMenunggu<%=rnd%>">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <div class="fs-3 fw-bold lh-1" style="color:#b45309;" id="dkMenunggu<%=rnd%>">–</div>
                        <div class="mt-1" style="font-size:11.5px;color:#64748b;font-weight:600;">Menunggu</div>
                    </div>
                    <div class="rounded-circle d-flex align-items-center justify-content-center"
                         style="width:38px;height:38px;background:#fffbeb;">
                        <i class="fas fa-hourglass-half" style="color:#f59e0b;font-size:15px;"></i>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-6 col-md-3">
            <div class="dk-stat-chip c-hijau shadow-sm p-3 bg-white"
                 style="border-top:3px solid #16a34a;" onclick="dkFilterStatus<%=rnd%>('disetujui')"
                 id="dkChipDisetujui<%=rnd%>">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <div class="fs-3 fw-bold text-success lh-1" id="dkDisetujui<%=rnd%>">–</div>
                        <div class="mt-1" style="font-size:11.5px;color:#64748b;font-weight:600;">Disetujui</div>
                    </div>
                    <div class="rounded-circle d-flex align-items-center justify-content-center"
                         style="width:38px;height:38px;background:#f0fdf4;">
                        <i class="fas fa-check-circle text-success" style="font-size:15px;"></i>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-6 col-md-3">
            <div class="dk-stat-chip c-merah shadow-sm p-3 bg-white"
                 style="border-top:3px solid #dc2626;" onclick="dkFilterStatus<%=rnd%>('ditolak')"
                 id="dkChipDitolak<%=rnd%>">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <div class="fs-3 fw-bold text-danger lh-1" id="dkDitolak<%=rnd%>">–</div>
                        <div class="mt-1" style="font-size:11.5px;color:#64748b;font-weight:600;">Ditolak</div>
                    </div>
                    <div class="rounded-circle d-flex align-items-center justify-content-center"
                         style="width:38px;height:38px;background:#fff1f2;">
                        <i class="fas fa-times-circle text-danger" style="font-size:15px;"></i>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Mini visualisasi donat status -->
    <div class="bg-white rounded-3 border shadow-sm p-3 mb-3" id="dkDonatWrap<%=rnd%>" style="display:none;">
        <div class="d-flex align-items-center gap-3 flex-wrap">
            <div style="position:relative;width:52px;height:52px;flex-shrink:0;">
                <canvas id="dkDonat<%=rnd%>" width="52" height="52"></canvas>
            </div>
            <div style="font-size:12px;color:#475569;">
                <div class="fw-bold mb-1" style="color:#0f172a;">Distribusi Status Surat Keluar</div>
                <div id="dkDonatLegend<%=rnd%>" class="d-flex gap-3 flex-wrap"></div>
            </div>
        </div>
    </div>

    <!-- Bar filter + pencarian -->
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-2 mb-3 p-3 bg-white rounded-3 border shadow-sm">
        <div class="form-check form-switch mb-0">
            <input class="form-check-input" type="checkbox" role="switch"
                   id="dkChkBelum<%=rnd%>" checked onchange="dkReset<%=rnd%>()">
            <label class="form-check-label fw-semibold" style="font-size:13px;"
                   for="dkChkBelum<%=rnd%>"><%= Common.getBahasaConfig("Belum Disetujui Saja") %></label>
        </div>
        <div class="d-flex align-items-center gap-2 flex-wrap">
            <div class="input-group input-group-sm" style="width:190px;">
                <span class="input-group-text bg-white border-end-0">
                    <i class="fas fa-search text-muted" style="font-size:12px;"></i>
                </span>
                <input type="text" class="form-control border-start-0" id="dkCari<%=rnd%>"
                       placeholder="Cari perihal, kode..." style="font-size:13px;">
            </div>
            <div class="input-group input-group-sm">
                <span class="input-group-text bg-white border-end-0">
                    <i class="fas fa-calendar-alt text-muted" style="font-size:12px;"></i>
                </span>
                <input type="date" class="form-control border-start-0 border-end-0" id="dkMulai<%=rnd%>" style="font-size:13px;">
                <span class="input-group-text bg-white border-start-0 border-end-0" style="font-size:13px;">–</span>
                <input type="date" class="form-control border-start-0" id="dkSampai<%=rnd%>" style="font-size:13px;">
            </div>
            <button class="btn btn-sm btn-primary fw-semibold px-3" onclick="dkReset<%=rnd%>()"
                    style="font-size:13px;">
                <i class="fas fa-search me-1"></i>Cari
            </button>
        </div>
    </div>

    <!-- Tabel desktop -->
    <div class="dk-tabel-area border rounded-3 shadow-sm bg-white overflow-hidden mb-2">
        <table class="table table-hover align-middle mb-0" style="font-size:13.5px;">
            <thead style="background:linear-gradient(135deg,#1a56db 0%,#2563eb 100%);">
                <tr class="text-white">
                    <th class="px-3 py-3 fw-semibold" style="width:44px;">#</th>
                    <th class="px-3 py-3 fw-semibold text-start">
                        <i class="fas fa-file-alt me-1 opacity-75"></i>Informasi Surat
                    </th>
                    <th class="px-3 py-3 fw-semibold text-start" style="width:210px;">
                        <i class="fas fa-tasks me-1 opacity-75"></i>Status
                    </th>
                    <th class="px-3 py-3 fw-semibold text-center" style="width:148px;"><%= Common.getBahasaConfig("Aksi") %></th>
                </tr>
            </thead>
            <tbody id="dkTabel<%=rnd%>">
                <tr><td colspan="4" class="text-center py-5">
                    <div class="spinner-border text-primary mb-2" style="width:1.8rem;height:1.8rem;"></div>
                    <div class="text-muted small">Memuat data...</div>
                </td></tr>
            </tbody>
        </table>
    </div>

    <!-- Kartu mobile -->
    <div class="dk-kartu-area mb-2" id="dkKartu<%=rnd%>">
        <div class="text-center py-4">
            <div class="spinner-border text-primary mb-2"></div>
            <div class="small text-muted">Memuat data...</div>
        </div>
    </div>

    <!-- Paginasi -->
    <div class="d-flex flex-column flex-sm-row justify-content-between align-items-center gap-2 mt-2">
        <small class="text-muted px-3 py-1 rounded-pill border bg-light" id="dkInfo<%=rnd%>"></small>
        <nav><ul class="pagination pagination-sm mb-0 shadow-sm">
            <li class="page-item" id="dkPrev<%=rnd%>">
                <button class="page-link fw-bold" onclick="dkHalaman<%=rnd%>(-1)">
                    <i class="fas fa-chevron-left small"></i>
                </button>
            </li>
            <li class="page-item disabled">
                <span class="page-link text-dark fw-bold px-3" id="dkLblHal<%=rnd%>">1/1</span>
            </li>
            <li class="page-item" id="dkNext<%=rnd%>">
                <button class="page-link fw-bold" onclick="dkHalaman<%=rnd%>(1)">
                    <i class="fas fa-chevron-right small"></i>
                </button>
            </li>
        </ul></nav>
    </div>
</div>

<script>
(function() {
    /* ===== KONSTANTA ===== */
    const RND = '<%=rnd%>';
    const ROOT = '<%=Common.ROOT%>';
    const API_CLASS = 'ais.database.model.surat.AlurPersetujuanSuratKeluarStatus';
    const PER_PAGE = 10;

    /* ===== STATE ===== */
    let halaman = 1;
    let totalData = 0;
    let filterStatus = '';

    /* ===== INISIALISASI TANGGAL ===== */
    const tglSampaiEl = document.getElementById('dkSampai' + RND);
    const tglMulaiEl  = document.getElementById('dkMulai'  + RND);
    const sekarang    = new Date();
    const setahunLalu = new Date(sekarang.getFullYear() - 1, sekarang.getMonth(), sekarang.getDate());
    if (tglSampaiEl) tglSampaiEl.valueAsDate = sekarang;
    if (tglMulaiEl)  tglMulaiEl.valueAsDate  = setahunLalu;

    /* Enter key pada pencarian */
    const cariEl = document.getElementById('dkCari' + RND);
    if (cariEl) cariEl.addEventListener('keydown', e => { if (e.key === 'Enter') dkReset(); });

    /* ===== MODAL (lazy inject) ===== */
    const modalId = 'dkModal' + RND;
    if (!document.getElementById(modalId)) {
        document.body.insertAdjacentHTML('beforeend',
            '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true">' +
            '<div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">' +
            '<div class="modal-content border-0 shadow-lg rounded-4">' +
            '<div class="modal-header text-white border-0 rounded-top-4" style="background:linear-gradient(135deg,#1a56db,#2563eb);">' +
            '<h5 class="modal-title fw-bold text-white"><i class="fas fa-clipboard-check me-2"></i>Tindak Lanjut Persetujuan Surat Keluar</h5>' +
            '<button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button></div>' +
            '<div class="modal-body p-0" id="dkModalBody' + RND + '"></div>' +
            '</div></div></div>');
    }

    /* ===== HELPER FETCH ===== */
    const apiFetch = async (payload) => {
        const r = await fetch(ROOT + '/Data', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!r.ok) throw new Error('HTTP ' + r.status);
        return r.json();
    };

    const fetchCount = async (where) => {
        try {
            const d = await apiFetch({ action: 'daftar', class: API_CLASS, alias1: 'suratKeluar',
                deep: '1', max: 1, halaman: 0, count: 'true', where1: where });
            return parseInt(d.count || 0);
        } catch (e) { return 0; }
    };

    /* ===== FILTER BUILDER ===== */
    window['dkFilterStatus' + RND] = function(s) {
        filterStatus = s;
        ['', 'menunggu', 'disetujui', 'ditolak'].forEach(k => {
            const idMap = { '': 'dkChipTotal', menunggu: 'dkChipMenunggu', disetujui: 'dkChipDisetujui', ditolak: 'dkChipDitolak' };
            const el = document.getElementById(idMap[k] + RND);
            if (el) el.classList.toggle('aktif', k === s);
        });
        if (s === 'disetujui' || s === 'ditolak') {
            const chk = document.getElementById('dkChkBelum' + RND);
            if (chk) chk.checked = false;
        }
        dkReset();
    };
    window['dkReset' + RND] = function() { halaman = 1; dkLoad(); };
    window['dkHalaman' + RND] = function(arah) {
        const total = Math.ceil(totalData / PER_PAGE) || 1;
        const baru = halaman + arah;
        if (baru >= 1 && baru <= total) { halaman = baru; dkLoad(); }
    };

    const buatWhere = () => {
        const kwd = (cariEl ? cariEl.value.trim().replace(/'/g, "''") : '');
        const mulai  = tglMulaiEl  ? tglMulaiEl.value  : '';
        const sampai = tglSampaiEl ? tglSampaiEl.value : '';
        const chk    = document.getElementById('dkChkBelum' + RND);
        const blm    = chk ? chk.checked : false;

        const k = ['kodeUnik IS NOT NULL'];
        if (filterStatus === 'menunggu')   k.push("this_.disetujui = false AND this_.ditolak = false");
        else if (filterStatus === 'disetujui') k.push("this_.disetujui = true");
        else if (filterStatus === 'ditolak')   k.push("this_.ditolak = true");
        else if (blm)                          k.push("this_.disetujui = false");
        if (mulai)  k.push("suratKeluar1_.tanggal >= '" + mulai + "'");
        if (sampai) k.push("suratKeluar1_.tanggal <= '" + sampai + "'");
        if (kwd)    k.push("(suratKeluar1_.perihal ILIKE '%" + kwd + "%' OR suratKeluar1_.agenda ILIKE '%" + kwd + "%' OR suratKeluar1_.kode ILIKE '%" + kwd + "%')");
        return k.join(' AND ');
    };

    /* ===== RENDER BADGE STATUS ===== */
    const badgeStatus = (b) => {
        if (b.disetujui) return '<span class="badge fw-semibold px-2 py-1" style="background:#dcfce7;color:#166534;font-size:11.5px;"><i class="fas fa-check-circle me-1"></i>Disetujui</span>';
        if (b.ditolak)   return '<span class="badge fw-semibold px-2 py-1" style="background:#fee2e2;color:#991b1b;font-size:11.5px;"><i class="fas fa-times-circle me-1"></i>Ditolak</span>';
        return '<span class="badge fw-semibold px-2 py-1" style="background:#fef3c7;color:#92400e;font-size:11.5px;"><i class="fas fa-hourglass-half me-1"></i>Menunggu Persetujuan</span>';
    };
    const stClass = (b) => b.disetujui ? 'st-disetujui' : (b.ditolak ? 'st-ditolak' : 'st-menunggu');

    /* ===== RENDER TOMBOL AKSI ===== */
    const tombolAksi = (id) =>
        '<button class="btn btn-sm fw-semibold me-1 mb-1" style="background:#eff6ff;color:#1d4ed8;border:1px solid #bfdbfe;font-size:12px;" ' +
        'onclick="dkLihat' + RND + '(' + id + ')" title="Lihat detail surat"><i class="fas fa-eye"></i>' +
        '<span class="d-none d-xl-inline ms-1">Lihat</span></button>' +
        '<button class="btn btn-sm fw-semibold mb-1" style="background:#f0fdf4;color:#166534;border:1px solid #bbf7d0;font-size:12px;" ' +
        'onclick="dkTindak' + RND + '(' + id + ')" title="Tindak lanjuti surat ini"><i class="fas fa-check-double"></i>' +
        '<span class="d-none d-xl-inline ms-1">Tindak Lanjuti</span></button>';

    /* ===== MUAT DATA UTAMA ===== */
    const dkLoad = async () => {
        const tEl = document.getElementById('dkTabel' + RND);
        const kEl = document.getElementById('dkKartu' + RND);
        const spin = '<div class="text-center py-4"><div class="spinner-border text-primary mb-2" style="width:1.8rem;height:1.8rem;"></div>' +
                     '<div class="small text-muted fw-semibold">Mengambil data...</div></div>';
        if (tEl) tEl.innerHTML = '<tr><td colspan="4" class="p-0">' + spin + '</td></tr>';
        if (kEl) kEl.innerHTML = spin;

        try {
            const data = await apiFetch({
                action: 'daftar', class: API_CLASS, alias1: 'suratKeluar',
                deep: '1', max: PER_PAGE, halaman: halaman - 1,
                order1: 'desc', sort1: 'id', count: 'true', where1: buatWhere()
            });
            totalData = parseInt(data.count || 0);
            const daftar = data.data || [];
            let tHtml = '', kHtml = '';

            if (!daftar.length) {
                const kosong = '<div class="text-center py-5 text-muted">' +
                    '<i class="fas fa-check-double fa-3x mb-3 d-block opacity-25"></i>' +
                    '<div class="fw-semibold">Tidak ada data yang sesuai.</div>' +
                    '<div class="small mt-1">Coba ubah filter atau rentang tanggal.</div></div>';
                tHtml = '<tr><td colspan="4" class="p-0">' + kosong + '</td></tr>';
                kHtml = kosong;
            } else {
                let no = ((halaman - 1) * PER_PAGE) + 1;
                daftar.forEach(b => {
                    const perihal = b['suratKeluar.perihal'] || '-';
                    const kode    = b['suratKeluar.kode']    || '-';
                    const tglRaw  = b['suratKeluar.tanggal'];
                    const tgl     = tglRaw ? new Date(tglRaw).toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' }) : '';
                    const badge   = badgeStatus(b);
                    const sc      = stClass(b);
                    const aksi    = tombolAksi(b.id);

                    tHtml += '<tr class="dk-baris-keluar ' + sc + '">' +
                        '<td class="text-center fw-bold px-3" style="color:#94a3b8;font-size:12px;">' + no + '</td>' +
                        '<td class="px-3 py-3">' +
                        '<div class="fw-bold text-dark mb-1" style="font-size:13.5px;">' + perihal + '</div>' +
                        '<div class="d-flex flex-wrap gap-1">' +
                        '<span class="badge fw-normal" style="background:#f1f5f9;color:#475569;font-size:11px;"><i class="fas fa-barcode me-1"></i>' + kode + '</span>' +
                        (tgl ? '<span class="badge fw-normal" style="background:#f1f5f9;color:#475569;font-size:11px;"><i class="fas fa-calendar-alt me-1"></i>' + tgl + '</span>' : '') +
                        '</div></td>' +
                        '<td class="px-3 py-3">' + badge + '</td>' +
                        '<td class="text-center px-3 py-3">' + aksi + '</td></tr>';

                    kHtml += '<div class="dk-kartu shadow-sm bg-white mb-2 ' + sc + '">' +
                        '<div class="p-3">' +
                        '<div class="d-flex justify-content-between align-items-start gap-2 mb-2">' +
                        '<div class="fw-bold text-dark" style="font-size:13.5px;line-height:1.4;">' + perihal + '</div>' +
                        badge + '</div>' +
                        '<div class="d-flex gap-1 flex-wrap mb-3">' +
                        '<span class="badge fw-normal" style="background:#f1f5f9;color:#475569;font-size:11px;"><i class="fas fa-barcode me-1"></i>' + kode + '</span>' +
                        (tgl ? '<span class="badge fw-normal" style="background:#f1f5f9;color:#475569;font-size:11px;"><i class="fas fa-calendar-alt me-1"></i>' + tgl + '</span>' : '') +
                        '</div>' +
                        '<div class="d-flex gap-2">' + aksi + '</div></div></div>';
                    no++;
                });
            }
            if (tEl) tEl.innerHTML = tHtml;
            if (kEl) kEl.innerHTML = kHtml;
            perbaruiPaginasi();
        } catch (e) {
            console.error(e);
            const err = '<div class="text-center text-danger py-4"><i class="fas fa-exclamation-triangle fa-2x mb-2 d-block"></i>' +
                        '<div class="small fw-semibold">Gagal memuat data. Coba segarkan halaman.</div></div>';
            if (tEl) tEl.innerHTML = '<tr><td colspan="4" class="p-0">' + err + '</td></tr>';
            if (kEl) kEl.innerHTML = err;
        }
    };

    /* ===== PAGINASI ===== */
    const perbaruiPaginasi = () => {
        const total = Math.ceil(totalData / PER_PAGE) || 1;
        const lbl  = document.getElementById('dkLblHal' + RND);
        const info = document.getElementById('dkInfo'   + RND);
        const prev = document.getElementById('dkPrev'   + RND);
        const next = document.getElementById('dkNext'   + RND);
        if (lbl)  lbl.innerText  = halaman + '/' + total;
        if (info) info.innerText = 'Total: ' + totalData + ' data';
        if (prev) prev.classList.toggle('disabled', halaman <= 1);
        if (next) next.classList.toggle('disabled', halaman >= total);
    };

    /* ===== MUAT STATISTIK ===== */
    const mautStats = async () => {
        const base = 'kodeUnik IS NOT NULL';
        const [tot, men, dis, dit] = await Promise.all([
            fetchCount(base),
            fetchCount(base + ' AND this_.disetujui = false AND this_.ditolak = false'),
            fetchCount(base + ' AND this_.disetujui = true'),
            fetchCount(base + ' AND this_.ditolak = true')
        ]);
        const set = (id, v) => { const el = document.getElementById(id + RND); if (el) el.innerText = v; };
        set('dkTotal',    tot);
        set('dkMenunggu', men);
        set('dkDisetujui',dis);
        set('dkDitolak',  dit);

        /* Donat mini (canvas) */
        const w = document.getElementById('dkDonatWrap' + RND);
        if (w) w.style.display = 'block';
        const canvas = document.getElementById('dkDonat' + RND);
        if (canvas && canvas.getContext) {
            const ctx = canvas.getContext('2d');
            const sz = 52, cx = sz / 2, cy = sz / 2, r = 22, ri = 14;
            const segments = [
                { val: men, color: '#f59e0b' },
                { val: dis, color: '#16a34a' },
                { val: dit, color: '#dc2626' }
            ];
            const total = segments.reduce((a, s) => a + s.val, 0) || 1;
            let start = -Math.PI / 2;
            ctx.clearRect(0, 0, sz, sz);
            segments.forEach(s => {
                const sweep = (s.val / total) * 2 * Math.PI;
                ctx.beginPath();
                ctx.moveTo(cx, cy);
                ctx.arc(cx, cy, r, start, start + sweep);
                ctx.closePath();
                ctx.fillStyle = s.color;
                ctx.fill();
                start += sweep;
            });
            ctx.beginPath(); ctx.arc(cx, cy, ri, 0, 2 * Math.PI);
            ctx.fillStyle = '#ffffff'; ctx.fill();
        }
        const legend = document.getElementById('dkDonatLegend' + RND);
        if (legend) {
            legend.innerHTML =
                '<span style="display:flex;align-items:center;gap:4px;font-size:11.5px;color:#92400e;">' +
                '<span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:#f59e0b;"></span>Menunggu (' + men + ')</span>' +
                '<span style="display:flex;align-items:center;gap:4px;font-size:11.5px;color:#166534;">' +
                '<span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:#16a34a;"></span>Disetujui (' + dis + ')</span>' +
                '<span style="display:flex;align-items:center;gap:4px;font-size:11.5px;color:#991b1b;">' +
                '<span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:#dc2626;"></span>Ditolak (' + dit + ')</span>';
        }
    };

    /* ===== MODAL AKSI ===== */
    window['dkLihat' + RND] = function(id) {
        const bEl = document.getElementById('dkModalBody' + RND);
        if (bEl) bEl.innerHTML = '<div class="alert alert-info m-4 rounded-3"><i class="fas fa-info-circle me-2"></i>Memuat detail surat keluar ID ' + id + '...</div>';
        const m = document.getElementById(modalId);
        if (m) new bootstrap.Modal(m).show();
    };
    window['dkTindak' + RND] = function(id) {
        const bEl = document.getElementById('dkModalBody' + RND);
        if (bEl) bEl.innerHTML = '<div class="alert alert-success m-4 rounded-3"><i class="fas fa-pen-square me-2"></i>Membuka form persetujuan surat keluar ID ' + id + '...</div>';
        const m = document.getElementById(modalId);
        if (m) new bootstrap.Modal(m).show();
    };

    /* ===== INIT ===== */
    dkLoad();
    mautStats();
})();
</script>
