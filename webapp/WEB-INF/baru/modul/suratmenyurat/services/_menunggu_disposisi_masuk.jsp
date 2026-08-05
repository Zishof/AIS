<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
String rnd = request.getParameter("rnd");
Tbmuser tbmuser = Common.getCurrentUser(request);
%>

<style>
#dmWrap<%=rnd%> .dm-stat-chip { cursor:pointer; transition:all .2s; border-radius:12px; }
#dmWrap<%=rnd%> .dm-stat-chip:hover { transform:translateY(-2px); box-shadow:0 6px 20px rgba(0,0,0,.12) !important; }
#dmWrap<%=rnd%> .dm-stat-chip.aktif { outline:3px solid; outline-offset:2px; }
#dmWrap<%=rnd%> .dm-stat-chip.aktif.c-hijau2  { outline-color:#0d9488; }
#dmWrap<%=rnd%> .dm-stat-chip.aktif.c-kuning2 { outline-color:#d97706; }
#dmWrap<%=rnd%> .dm-stat-chip.aktif.c-biru2   { outline-color:#0284c7; }
#dmWrap<%=rnd%> .dm-stat-chip.aktif.c-merah2  { outline-color:#dc2626; }
#dmWrap<%=rnd%> .dm-baris { border-left:3px solid transparent; transition:background .15s; }
#dmWrap<%=rnd%> .dm-baris:hover { background:#f8fafc !important; }
#dmWrap<%=rnd%> .dm-baris.st-menunggu  { border-left-color:#d97706; }
#dmWrap<%=rnd%> .dm-baris.st-disposisi { border-left-color:#0284c7; }
#dmWrap<%=rnd%> .dm-baris.st-ditolak   { border-left-color:#dc2626; }
#dmWrap<%=rnd%> .dm-kartu { border-radius:12px; border-left:4px solid #dee2e6; transition:box-shadow .2s; }
#dmWrap<%=rnd%> .dm-kartu:hover { box-shadow:0 4px 14px rgba(0,0,0,.1) !important; }
#dmWrap<%=rnd%> .dm-kartu.st-menunggu  { border-left-color:#d97706; }
#dmWrap<%=rnd%> .dm-kartu.st-disposisi { border-left-color:#0284c7; }
#dmWrap<%=rnd%> .dm-kartu.st-ditolak   { border-left-color:#dc2626; }
@media (max-width:767px) {
    #dmWrap<%=rnd%> .dm-tabel-area { display:none; }
    #dmWrap<%=rnd%> .dm-kartu-area { display:block; }
}
@media (min-width:768px) {
    #dmWrap<%=rnd%> .dm-tabel-area { display:block; }
    #dmWrap<%=rnd%> .dm-kartu-area { display:none; }
}
</style>

<div id="dmWrap<%=rnd%>" class="animate__animated animate__fadeIn">

    <!-- Keterangan singkat untuk pengguna -->
    <div class="d-flex align-items-start gap-2 mb-3 px-3 py-2 rounded-3"
         style="background:#f0fdf4;border:1px solid #bbf7d0;border-left:4px solid #16a34a;">
        <i class="fas fa-inbox mt-1" style="font-size:14px;color:#15803d;"></i>
        <div style="font-size:13px; color:#166534; line-height:1.55;">
            <strong>Surat Masuk &ndash; Perlu Disposisi.</strong>
            Berikut daftar surat masuk yang memerlukan tindakan dari Anda. Klik <strong>Disposisi</strong> untuk meneruskan surat kepada pihak yang tepat atau memberi instruksi penanganan.
        </div>
    </div>

    <!-- 4 Kartu Ringkasan Statistik -->
    <div class="row g-2 mb-3">
        <div class="col-6 col-md-3">
            <div class="dm-stat-chip c-hijau2 shadow-sm p-3 bg-white"
                 style="border-top:3px solid #0d9488;" onclick="dmFilterStatus<%=rnd%>('')"
                 id="dmChipTotal<%=rnd%>">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <div class="fs-3 fw-bold lh-1" style="color:#0d9488;" id="dmTotal<%=rnd%>">–</div>
                        <div class="mt-1" style="font-size:11.5px;color:#64748b;font-weight:600;">Total Surat</div>
                    </div>
                    <div class="rounded-circle d-flex align-items-center justify-content-center"
                         style="width:38px;height:38px;background:#f0fdfa;">
                        <i class="fas fa-envelope-open-text" style="color:#0d9488;font-size:15px;"></i>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-6 col-md-3">
            <div class="dm-stat-chip c-kuning2 shadow-sm p-3 bg-white"
                 style="border-top:3px solid #d97706;" onclick="dmFilterStatus<%=rnd%>('menunggu')"
                 id="dmChipMenunggu<%=rnd%>">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <div class="fs-3 fw-bold lh-1" style="color:#b45309;" id="dmMenunggu<%=rnd%>">–</div>
                        <div class="mt-1" style="font-size:11.5px;color:#64748b;font-weight:600;">Menunggu</div>
                    </div>
                    <div class="rounded-circle d-flex align-items-center justify-content-center"
                         style="width:38px;height:38px;background:#fffbeb;">
                        <i class="fas fa-hourglass-half" style="color:#d97706;font-size:15px;"></i>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-6 col-md-3">
            <div class="dm-stat-chip c-biru2 shadow-sm p-3 bg-white"
                 style="border-top:3px solid #0284c7;" onclick="dmFilterStatus<%=rnd%>('disposisi')"
                 id="dmChipDisposisi<%=rnd%>">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <div class="fs-3 fw-bold lh-1" style="color:#0369a1;" id="dmDisposisi<%=rnd%>">–</div>
                        <div class="mt-1" style="font-size:11.5px;color:#64748b;font-weight:600;">Terdisposisi</div>
                    </div>
                    <div class="rounded-circle d-flex align-items-center justify-content-center"
                         style="width:38px;height:38px;background:#e0f2fe;">
                        <i class="fas fa-check-double" style="color:#0284c7;font-size:15px;"></i>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-6 col-md-3">
            <div class="dm-stat-chip c-merah2 shadow-sm p-3 bg-white"
                 style="border-top:3px solid #dc2626;" onclick="dmFilterStatus<%=rnd%>('ditolak')"
                 id="dmChipDitolak<%=rnd%>">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <div class="fs-3 fw-bold text-danger lh-1" id="dmDitolak<%=rnd%>">–</div>
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
    <div class="bg-white rounded-3 border shadow-sm p-3 mb-3" id="dmDonatWrap<%=rnd%>" style="display:none;">
        <div class="d-flex align-items-center gap-3 flex-wrap">
            <div style="position:relative;width:52px;height:52px;flex-shrink:0;">
                <canvas id="dmDonat<%=rnd%>" width="52" height="52"></canvas>
            </div>
            <div style="font-size:12px;color:#475569;">
                <div class="fw-bold mb-1" style="color:#0f172a;">Distribusi Status Surat Masuk</div>
                <div id="dmDonatLegend<%=rnd%>" class="d-flex gap-3 flex-wrap"></div>
            </div>
        </div>
    </div>

    <!-- Bar filter + pencarian -->
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-2 mb-3 p-3 bg-white rounded-3 border shadow-sm">
        <div class="form-check form-switch mb-0">
            <input class="form-check-input" type="checkbox" role="switch"
                   id="dmChkBelum<%=rnd%>" checked onchange="dmReset<%=rnd%>()">
            <label class="form-check-label fw-semibold" style="font-size:13px;"
                   for="dmChkBelum<%=rnd%>"><%= Common.getBahasaConfig("Belum Terdisposisi Saja") %></label>
        </div>
        <div class="d-flex align-items-center gap-2 flex-wrap">
            <div class="input-group input-group-sm" style="width:200px;">
                <span class="input-group-text bg-white border-end-0">
                    <i class="fas fa-search text-muted" style="font-size:12px;"></i>
                </span>
                <input type="text" class="form-control border-start-0" id="dmCari<%=rnd%>"
                       placeholder="Cari nomor, perihal..." style="font-size:13px;">
            </div>
            <div class="input-group input-group-sm">
                <span class="input-group-text bg-white border-end-0">
                    <i class="fas fa-calendar-alt text-muted" style="font-size:12px;"></i>
                </span>
                <input type="date" class="form-control border-start-0 border-end-0" id="dmMulai<%=rnd%>" style="font-size:13px;">
                <span class="input-group-text bg-white border-start-0 border-end-0" style="font-size:13px;">–</span>
                <input type="date" class="form-control border-start-0" id="dmSampai<%=rnd%>" style="font-size:13px;">
            </div>
            <button class="btn btn-sm fw-semibold px-3" onclick="dmReset<%=rnd%>()"
                    style="background:#0d9488;color:#fff;font-size:13px;">
                <i class="fas fa-search me-1"></i>Cari
            </button>
        </div>
    </div>

    <!-- Tabel desktop -->
    <div class="dm-tabel-area border rounded-3 shadow-sm bg-white overflow-hidden mb-2">
        <table class="table table-hover align-middle mb-0" style="font-size:13.5px;">
            <thead style="background:linear-gradient(135deg,#0f766e 0%,#0d9488 100%);">
                <tr class="text-white">
                    <th class="px-3 py-3 fw-semibold" style="width:44px;">#</th>
                    <th class="px-3 py-3 fw-semibold text-start">
                        <i class="fas fa-envelope-open me-1 opacity-75"></i>Informasi Surat Masuk
                    </th>
                    <th class="px-3 py-3 fw-semibold text-start" style="width:220px;">
                        <i class="fas fa-share-alt me-1 opacity-75"></i>Status Disposisi
                    </th>
                    <th class="px-3 py-3 fw-semibold text-center" style="width:148px;"><%= Common.getBahasaConfig("Aksi") %></th>
                </tr>
            </thead>
            <tbody id="dmTabel<%=rnd%>">
                <tr><td colspan="4" class="text-center py-5">
                    <div class="spinner-border mb-2" style="width:1.8rem;height:1.8rem;color:#0d9488;"></div>
                    <div class="text-muted small">Memuat data...</div>
                </td></tr>
            </tbody>
        </table>
    </div>

    <!-- Kartu mobile -->
    <div class="dm-kartu-area mb-2" id="dmKartu<%=rnd%>">
        <div class="text-center py-4">
            <div class="spinner-border mb-2" style="color:#0d9488;"></div>
            <div class="small text-muted">Memuat data...</div>
        </div>
    </div>

    <!-- Paginasi -->
    <div class="d-flex flex-column flex-sm-row justify-content-between align-items-center gap-2 mt-2">
        <small class="text-muted px-3 py-1 rounded-pill border bg-light" id="dmInfo<%=rnd%>"></small>
        <nav><ul class="pagination pagination-sm mb-0 shadow-sm">
            <li class="page-item" id="dmPrev<%=rnd%>">
                <button class="page-link fw-bold" onclick="dmHalaman<%=rnd%>(-1)">
                    <i class="fas fa-chevron-left small"></i>
                </button>
            </li>
            <li class="page-item disabled">
                <span class="page-link text-dark fw-bold px-3" id="dmLblHal<%=rnd%>">1/1</span>
            </li>
            <li class="page-item" id="dmNext<%=rnd%>">
                <button class="page-link fw-bold" onclick="dmHalaman<%=rnd%>(1)">
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
    const API_CLASS = 'ais.database.model.surat.AlurPersetujuanSuratMasukStatus';
    const PER_PAGE = 10;

    /* ===== STATE ===== */
    let halaman = 1;
    let totalData = 0;
    let filterStatus = '';

    /* ===== INISIALISASI TANGGAL ===== */
    const tglSampaiEl = document.getElementById('dmSampai' + RND);
    const tglMulaiEl  = document.getElementById('dmMulai'  + RND);
    const sekarang    = new Date();
    const setahunLalu = new Date(sekarang.getFullYear() - 1, sekarang.getMonth(), sekarang.getDate());
    if (tglSampaiEl) tglSampaiEl.valueAsDate = sekarang;
    if (tglMulaiEl)  tglMulaiEl.valueAsDate  = setahunLalu;

    const cariEl = document.getElementById('dmCari' + RND);
    if (cariEl) cariEl.addEventListener('keydown', e => { if (e.key === 'Enter') dmReset(); });

    /* ===== MODAL (lazy inject) ===== */
    const modalId = 'dmModal' + RND;
    if (!document.getElementById(modalId)) {
        document.body.insertAdjacentHTML('beforeend',
            '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true">' +
            '<div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">' +
            '<div class="modal-content border-0 shadow-lg rounded-4">' +
            '<div class="modal-header text-white border-0 rounded-top-4" style="background:linear-gradient(135deg,#0f766e,#0d9488);">' +
            '<h5 class="modal-title fw-bold text-white"><i class="fas fa-clipboard-list me-2"></i>Tindak Lanjut Disposisi Surat Masuk</h5>' +
            '<button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button></div>' +
            '<div class="modal-body p-0" id="dmModalBody' + RND + '"></div>' +
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
            const d = await apiFetch({ action: 'daftar', class: API_CLASS, alias1: 'suratMasuk',
                deep: '1', max: 1, halaman: 0, count: 'true', where1: where });
            return parseInt(d.count || 0);
        } catch (e) { return 0; }
    };

    /* ===== FILTER BUILDER ===== */
    window['dmFilterStatus' + RND] = function(s) {
        filterStatus = s;
        ['', 'menunggu', 'disposisi', 'ditolak'].forEach(k => {
            const idMap = { '': 'dmChipTotal', menunggu: 'dmChipMenunggu', disposisi: 'dmChipDisposisi', ditolak: 'dmChipDitolak' };
            const el = document.getElementById(idMap[k] + RND);
            if (el) el.classList.toggle('aktif', k === s);
        });
        if (s === 'disposisi' || s === 'ditolak') {
            const chk = document.getElementById('dmChkBelum' + RND);
            if (chk) chk.checked = false;
        }
        dmReset();
    };
    window['dmReset' + RND] = function() { halaman = 1; dmLoad(); };
    window['dmHalaman' + RND] = function(arah) {
        const total = Math.ceil(totalData / PER_PAGE) || 1;
        const baru = halaman + arah;
        if (baru >= 1 && baru <= total) { halaman = baru; dmLoad(); }
    };

    const buatWhere = () => {
        const kwd = (cariEl ? cariEl.value.trim().replace(/'/g, "''") : '');
        const mulai  = tglMulaiEl  ? tglMulaiEl.value  : '';
        const sampai = tglSampaiEl ? tglSampaiEl.value : '';
        const chk    = document.getElementById('dmChkBelum' + RND);
        const blm    = chk ? chk.checked : false;

        const k = ['kodeUnik IS NOT NULL'];
        if (filterStatus === 'menunggu')    k.push("this_.disetujui = false AND this_.ditolak = false");
        else if (filterStatus === 'disposisi') k.push("this_.disetujui = true");
        else if (filterStatus === 'ditolak')   k.push("this_.ditolak = true");
        else if (blm)                          k.push("this_.disetujui = false");
        if (mulai)  k.push("suratMasuk1_.tanggal >= '" + mulai + "'");
        if (sampai) k.push("suratMasuk1_.tanggal <= '" + sampai + "'");
        if (kwd)    k.push("(suratMasuk1_.noSurat ILIKE '%" + kwd + "%' OR suratMasuk1_.perihal ILIKE '%" + kwd + "%' OR suratMasuk1_.kode ILIKE '%" + kwd + "%')");
        return k.join(' AND ');
    };

    /* ===== RENDER BADGE STATUS ===== */
    const badgeStatus = (b) => {
        if (b.disetujui) return '<span class="badge fw-semibold px-2 py-1" style="background:#e0f2fe;color:#075985;font-size:11.5px;"><i class="fas fa-check-double me-1"></i>Terdisposisi</span>';
        if (b.ditolak)   return '<span class="badge fw-semibold px-2 py-1" style="background:#fee2e2;color:#991b1b;font-size:11.5px;"><i class="fas fa-times-circle me-1"></i>Ditolak</span>';
        return '<span class="badge fw-semibold px-2 py-1" style="background:#fef9c3;color:#92400e;font-size:11.5px;"><i class="fas fa-hourglass-half me-1"></i>Menunggu Disposisi</span>';
    };
    const stClass = (b) => b.disetujui ? 'st-disposisi' : (b.ditolak ? 'st-ditolak' : 'st-menunggu');

    /* ===== RENDER TOMBOL AKSI ===== */
    const tombolAksi = (id) =>
        '<button class="btn btn-sm fw-semibold me-1 mb-1" style="background:#f0fdfa;color:#0f766e;border:1px solid #99f6e4;font-size:12px;" ' +
        'onclick="dmLihat' + RND + '(' + id + ')" title="Lihat detail surat masuk"><i class="fas fa-eye"></i>' +
        '<span class="d-none d-xl-inline ms-1">Lihat</span></button>' +
        '<button class="btn btn-sm fw-semibold mb-1" style="background:#eff6ff;color:#1d4ed8;border:1px solid #bfdbfe;font-size:12px;" ' +
        'onclick="dmTindak' + RND + '(' + id + ')" title="Buat disposisi surat ini"><i class="fas fa-share"></i>' +
        '<span class="d-none d-xl-inline ms-1">Disposisi</span></button>';

    /* ===== MUAT DATA UTAMA ===== */
    const dmLoad = async () => {
        const tEl = document.getElementById('dmTabel' + RND);
        const kEl = document.getElementById('dmKartu' + RND);
        const spin = '<div class="text-center py-4"><div class="spinner-border mb-2" style="width:1.8rem;height:1.8rem;color:#0d9488;"></div>' +
                     '<div class="small text-muted fw-semibold">Mengambil data...</div></div>';
        if (tEl) tEl.innerHTML = '<tr><td colspan="4" class="p-0">' + spin + '</td></tr>';
        if (kEl) kEl.innerHTML = spin;

        try {
            const data = await apiFetch({
                action: 'daftar', class: API_CLASS, alias1: 'suratMasuk',
                deep: '1', max: PER_PAGE, halaman: halaman - 1,
                order1: 'desc', sort1: 'id', count: 'true', where1: buatWhere()
            });
            totalData = parseInt(data.count || 0);
            const daftar = data.data || [];
            let tHtml = '', kHtml = '';

            if (!daftar.length) {
                const kosong = '<div class="text-center py-5 text-muted">' +
                    '<i class="fas fa-inbox fa-3x mb-3 d-block opacity-25"></i>' +
                    '<div class="fw-semibold">Tidak ada surat masuk yang memerlukan disposisi.</div>' +
                    '<div class="small mt-1">Coba ubah filter atau rentang tanggal.</div></div>';
                tHtml = '<tr><td colspan="4" class="p-0">' + kosong + '</td></tr>';
                kHtml = kosong;
            } else {
                let no = ((halaman - 1) * PER_PAGE) + 1;
                daftar.forEach(b => {
                    const perihal = b['suratMasuk.perihal'] || '-';
                    const noSurat = b['suratMasuk.noSurat'] || b['suratMasuk.kode'] || '-';
                    const tglRaw  = b['suratMasuk.tanggal'];
                    const tgl     = tglRaw ? new Date(tglRaw).toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' }) : '';
                    const badge   = badgeStatus(b);
                    const sc      = stClass(b);
                    const aksi    = tombolAksi(b.id);

                    tHtml += '<tr class="dm-baris ' + sc + '">' +
                        '<td class="text-center fw-bold px-3" style="color:#94a3b8;font-size:12px;">' + no + '</td>' +
                        '<td class="px-3 py-3">' +
                        '<div class="fw-bold text-dark mb-1" style="font-size:13.5px;">' + perihal + '</div>' +
                        '<div class="d-flex flex-wrap gap-1">' +
                        '<span class="badge fw-normal" style="background:#f1f5f9;color:#475569;font-size:11px;"><i class="fas fa-hashtag me-1"></i>' + noSurat + '</span>' +
                        (tgl ? '<span class="badge fw-normal" style="background:#f1f5f9;color:#475569;font-size:11px;"><i class="fas fa-calendar-alt me-1"></i>' + tgl + '</span>' : '') +
                        '</div></td>' +
                        '<td class="px-3 py-3">' + badge + '</td>' +
                        '<td class="text-center px-3 py-3">' + aksi + '</td></tr>';

                    kHtml += '<div class="dm-kartu shadow-sm bg-white mb-2 ' + sc + '">' +
                        '<div class="p-3">' +
                        '<div class="d-flex justify-content-between align-items-start gap-2 mb-2">' +
                        '<div class="fw-bold text-dark" style="font-size:13.5px;line-height:1.4;">' + perihal + '</div>' +
                        badge + '</div>' +
                        '<div class="d-flex gap-1 flex-wrap mb-3">' +
                        '<span class="badge fw-normal" style="background:#f1f5f9;color:#475569;font-size:11px;"><i class="fas fa-hashtag me-1"></i>' + noSurat + '</span>' +
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
        const lbl  = document.getElementById('dmLblHal' + RND);
        const info = document.getElementById('dmInfo'   + RND);
        const prev = document.getElementById('dmPrev'   + RND);
        const next = document.getElementById('dmNext'   + RND);
        if (lbl)  lbl.innerText  = halaman + '/' + total;
        if (info) info.innerText = 'Total: ' + totalData + ' data';
        if (prev) prev.classList.toggle('disabled', halaman <= 1);
        if (next) next.classList.toggle('disabled', halaman >= total);
    };

    /* ===== MUAT STATISTIK ===== */
    const muatStats = async () => {
        const base = 'kodeUnik IS NOT NULL';
        const [tot, men, dis, dit] = await Promise.all([
            fetchCount(base),
            fetchCount(base + ' AND this_.disetujui = false AND this_.ditolak = false'),
            fetchCount(base + ' AND this_.disetujui = true'),
            fetchCount(base + ' AND this_.ditolak = true')
        ]);
        const set = (id, v) => { const el = document.getElementById(id + RND); if (el) el.innerText = v; };
        set('dmTotal',     tot);
        set('dmMenunggu',  men);
        set('dmDisposisi', dis);
        set('dmDitolak',   dit);

        /* Donat mini */
        const w = document.getElementById('dmDonatWrap' + RND);
        if (w) w.style.display = 'block';
        const canvas = document.getElementById('dmDonat' + RND);
        if (canvas && canvas.getContext) {
            const ctx = canvas.getContext('2d');
            const sz = 52, cx = sz/2, cy = sz/2, r = 22, ri = 14;
            const segments = [
                { val: men, color: '#d97706' },
                { val: dis, color: '#0284c7' },
                { val: dit, color: '#dc2626' }
            ];
            const total = segments.reduce((a, s) => a + s.val, 0) || 1;
            let start = -Math.PI / 2;
            ctx.clearRect(0, 0, sz, sz);
            segments.forEach(s => {
                const sweep = (s.val / total) * 2 * Math.PI;
                ctx.beginPath(); ctx.moveTo(cx, cy);
                ctx.arc(cx, cy, r, start, start + sweep);
                ctx.closePath(); ctx.fillStyle = s.color; ctx.fill();
                start += sweep;
            });
            ctx.beginPath(); ctx.arc(cx, cy, ri, 0, 2 * Math.PI);
            ctx.fillStyle = '#ffffff'; ctx.fill();
        }
        const legend = document.getElementById('dmDonatLegend' + RND);
        if (legend) {
            legend.innerHTML =
                '<span style="display:flex;align-items:center;gap:4px;font-size:11.5px;color:#92400e;">' +
                '<span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:#d97706;"></span>Menunggu (' + men + ')</span>' +
                '<span style="display:flex;align-items:center;gap:4px;font-size:11.5px;color:#0369a1;">' +
                '<span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:#0284c7;"></span>Terdisposisi (' + dis + ')</span>' +
                '<span style="display:flex;align-items:center;gap:4px;font-size:11.5px;color:#991b1b;">' +
                '<span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:#dc2626;"></span>Ditolak (' + dit + ')</span>';
        }
    };

    /* ===== MODAL AKSI ===== */
    window['dmLihat' + RND] = function(id) {
        const bEl = document.getElementById('dmModalBody' + RND);
        if (bEl) bEl.innerHTML = '<div class="alert alert-info m-4 rounded-3"><i class="fas fa-info-circle me-2"></i>Memuat detail surat masuk ID ' + id + '...</div>';
        const m = document.getElementById(modalId);
        if (m) new bootstrap.Modal(m).show();
    };
    window['dmTindak' + RND] = function(id) {
        const bEl = document.getElementById('dmModalBody' + RND);
        if (bEl) bEl.innerHTML = '<div class="alert alert-success m-4 rounded-3"><i class="fas fa-share me-2"></i>Membuka form disposisi surat masuk ID ' + id + '...</div>';
        const m = document.getElementById(modalId);
        if (m) new bootstrap.Modal(m).show();
    };

    /* ===== INIT ===== */
    dmLoad();
    muatStats();
})();
</script>
