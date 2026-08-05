<%@page import="javax.servlet.http.HttpServletResponse"%>
<%@page import="ais.database.model.repository.RepoCollection"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// ── Security Gate ──────────────────────────────────────────────────────────
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    out.clear();
    out.print("{\"status\":\"error\", \"message\":\""
        + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.")
        + "\"}");
    return;
}
String rnd = Common.getGeneratedBarCode(7);
final int LIMIT_PER_PAGE = 10;
%>

<!-- ── LIST VIEW ─────────────────────────────────────────────────────────── -->
<div id="viewList<%=rnd%>" class="animate__animated animate__fadeInUp">
    <div class="row mb-4">
        <div class="col-12">
            <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">

                <div class="card-header bg-white border-0 pt-4 pb-3 px-4 border-bottom border-light">
                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-2">
                        <div>
                            <h5 class="fw-bold text-dark mb-1">
                                <i class="fas fa-folder-open text-primary me-2"></i>
                                <%=Common.getBahasaConfig("Manajemen Koleksi Repositori")%>
                            </h5>
                            <small class="text-muted">
                                <%=Common.getBahasaConfig("Kelola daftar pengelompokan (koleksi) dokumen repositori digital.")%>
                            </small>
                        </div>

                        <div class="d-flex align-items-center gap-2 flex-wrap">
                            <div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;">
                                <span class="input-group-text bg-white border-end-0">
                                    <i class="fas fa-search text-muted"></i>
                                </span>
                                <input type="text" id="searchKoleksi<%=rnd%>"
                                    class="form-control border-start-0 ps-0 bg-white fw-medium"
                                    placeholder="<%=Common.getBahasaConfig("Cari Nama / Kode Koleksi...")%>"
                                    onkeydown="if(event.key==='Enter'){event.preventDefault();resetAndLoadData<%=rnd%>();}">
                            </div>

                            <button class="btn btn-sm btn-outline-secondary rounded-pill px-3 shadow-sm fw-bold"
                                onclick="resetAndLoadData<%=rnd%>()"
                                title="<%=Common.getBahasaConfig("Muat Ulang Data")%>">
                                <i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Segarkan")%>
                            </button>

                            <button class="btn btn-sm btn-outline-success rounded-pill px-3 shadow-sm fw-bold"
                                onclick="downloadExcelKoleksi<%=rnd%>()"
                                title="<%=Common.getBahasaConfig("Unduh Data Excel")%>">
                                <i class="fas fa-file-excel me-1"></i><%=Common.getBahasaConfig("Excel")%>
                            </button>

                            <button class="btn btn-sm btn-primary rounded-pill px-4 shadow-sm fw-bold text-nowrap"
                                onclick="bukaFormTambah<%=rnd%>()">
                                <i class="fas fa-plus-circle me-1"></i><%=Common.getBahasaConfig("Tambah Koleksi")%>
                            </button>
                        </div>
                    </div>
                </div>

                <div class="card-body p-4 pt-3">
                    <div class="table-responsive border rounded-3 shadow-sm bg-white">
                        <table class="table table-hover table-striped align-middle mb-0"
                            id="tabelHTMLDataKoleksi<%=rnd%>">
                            <thead class="table-dark text-center align-middle">
                                <tr>
                                    <th class="fw-semibold text-uppercase small px-3 py-3" style="width:55px;">#</th>
                                    <th class="text-start fw-semibold text-uppercase small px-3" style="width:15%;">
                                        <i class="fas fa-barcode me-1"></i><%=Common.getBahasaConfig("Kode")%>
                                    </th>
                                    <th class="text-start fw-semibold text-uppercase small px-3" style="width:35%;">
                                        <i class="fas fa-book me-1"></i><%=Common.getBahasaConfig("Nama Koleksi")%>
                                    </th>
                                    <th class="text-start fw-semibold text-uppercase small px-3">
                                        <i class="fas fa-align-left me-1"></i><%=Common.getBahasaConfig("Deskripsi")%>
                                    </th>
                                    <th class="fw-semibold text-uppercase small px-3 text-center no-export"
                                        style="width:110px;">
                                        <i class="fas fa-cogs"></i>
                                    </th>
                                </tr>
                            </thead>
                            <tbody id="tabelDataKoleksi<%=rnd%>">
                                <tr>
                                    <td colspan="5" class="text-center py-5">
                                        <div class="spinner-border text-primary mb-3"></div><br>
                                        <span class="text-muted fw-medium">
                                            <%=Common.getBahasaConfig("Memuat data koleksi...")%>
                                        </span>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>

                    <!-- Paging -->
                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mt-4">
                        <div class="small fw-medium text-muted bg-light px-3 py-1 rounded-pill border"
                            id="pagingInfoKoleksi<%=rnd%>"></div>
                        <nav>
                            <ul class="pagination pagination-sm mb-0 shadow-sm">
                                <li class="page-item">
                                    <button class="page-link text-primary rounded-start-pill px-3 fw-bold"
                                        id="btnPrevPage<%=rnd%>"
                                        onclick="changePageKoleksi<%=rnd%>(-1)">
                                        <i class="fas fa-chevron-left"></i>
                                    </button>
                                </li>
                                <li class="page-item disabled">
                                    <span class="page-link bg-light text-dark px-3 fw-bold"
                                        id="pageNumberDisplay<%=rnd%>">1</span>
                                </li>
                                <li class="page-item">
                                    <button class="page-link text-primary rounded-end-pill px-3 fw-bold"
                                        id="btnNextPage<%=rnd%>"
                                        onclick="changePageKoleksi<%=rnd%>(1)">
                                        <i class="fas fa-chevron-right"></i>
                                    </button>
                                </li>
                            </ul>
                        </nav>
                    </div>
                </div>

            </div>
        </div>
    </div>
</div>

<!-- ── FORM VIEW ──────────────────────────────────────────────────────────── -->
<div id="viewForm<%=rnd%>" class="animate__animated animate__fadeIn" style="display:none;">
    <div class="row justify-content-center">
        <div class="col-lg-8">
            <div class="card border-0 shadow-sm rounded-4 border-top border-success border-4">
                <div class="card-header bg-white border-0 pt-4 pb-0 px-4">
                    <div class="d-flex justify-content-between align-items-center">
                        <h5 class="fw-bold text-dark mb-0" id="formTitle<%=rnd%>">
                            <i class="fas fa-edit text-success me-2"></i>
                            <%=Common.getBahasaConfig("Formulir Koleksi Repositori")%>
                        </h5>
                    </div>
                </div>
                <div class="card-body p-4">
                    <form id="formKoleksi<%=rnd%>"
                        onsubmit="event.preventDefault(); simpanKoleksi<%=rnd%>();">
                        <input type="hidden" id="inputIdKoleksi<%=rnd%>" value="">

                        <div class="row g-4 mt-1">
                            <div class="col-md-12">
                                <h6 class="fw-bold text-primary border-bottom pb-2 mb-0">
                                    <i class="fas fa-info-circle me-2"></i>
                                    <%=Common.getBahasaConfig("Rincian Koleksi")%>
                                </h6>
                            </div>

                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary">
                                    <%=Common.getBahasaConfig("Kode Koleksi")%>
                                    <small class="text-muted">(<%=Common.getBahasaConfig("Opsional")%>)</small>
                                </label>
                                <input type="text" class="form-control fw-bold shadow-sm"
                                    id="inputKode<%=rnd%>"
                                    placeholder="<%=Common.getBahasaConfig("Otomatis jika kosong")%>">
                            </div>

                            <div class="col-md-8">
                                <label class="form-label small fw-bold text-secondary">
                                    <%=Common.getBahasaConfig("Nama Koleksi")%>
                                    <span class="text-danger">*</span>
                                </label>
                                <input type="text" class="form-control fw-bold shadow-sm border-info"
                                    id="inputNama<%=rnd%>" required
                                    placeholder="<%=Common.getBahasaConfig("Contoh: Jurnal Teknik Informatika")%>">
                            </div>

                            <div class="col-md-12">
                                <label class="form-label small fw-bold text-secondary">
                                    <%=Common.getBahasaConfig("Deskripsi Singkat")%>
                                </label>
                                <textarea class="form-control shadow-sm" id="inputDeskripsi<%=rnd%>"
                                    rows="4"
                                    placeholder="<%=Common.getBahasaConfig("Tuliskan keterangan mengenai koleksi ini...")%>">
                                </textarea>
                            </div>

                            <div class="col-12 mt-5 text-end border-top pt-3">
                                <button type="button"
                                    class="btn btn-light px-4 me-2 rounded-pill fw-semibold border shadow-sm"
                                    onclick="tutupForm<%=rnd%>()">
                                    <i class="fas fa-times text-danger me-2"></i><%=Common.getBahasaConfig("Batal")%>
                                </button>
                                <button type="submit" id="btnSimpanKoleksi<%=rnd%>"
                                    class="btn btn-success px-4 rounded-pill shadow-sm fw-bold">
                                    <i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Data")%>
                                </button>
                            </div>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
(function () {
    'use strict';

    // ── Konstanta ──────────────────────────────────────────────────────────
    const ROOT          = '<%=Common.ROOT%>';
    const MODEL_CLASS   = '<%=RepoCollection.class.getName()%>';
    const LIMIT         = <%=LIMIT_PER_PAGE%>;
    const RND           = '<%=rnd%>';

    let currentPage  = 1;
    let totalRecords = 0;

    // ── Utility ────────────────────────────────────────────────────────────
    const toast = (msg, colorClass) => {
        if (typeof tampilkanToast === 'function') tampilkanToast(msg, colorClass);
        else alert(msg);
    };

    const apiPost = (body) =>
        fetch(ROOT + '/Data', {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify(body)
        }).then(r => r.json());

    // ── Filter SQL (sanitised) ─────────────────────────────────────────────
    const buildWhere = () => {
        const raw = (document.getElementById('searchKoleksi' + RND).value || '').trim();
        // Escape single-quotes to prevent SQL injection
        const kw  = raw.replace(/'/g, "''");
        const parts = ["(aktif = true OR aktif IS NULL)"];
        if (kw !== '') {
            parts.push("(nama ILIKE '%" + kw + "%' OR kode ILIKE '%" + kw + "%')");
        }
        return parts.join(' AND ');
    };

    // ── Load / Paging ──────────────────────────────────────────────────────
    const resetAndLoadData = () => { currentPage = 1; loadData(); };
    window['resetAndLoadData' + RND] = resetAndLoadData;

    const loadData = async () => {
        const tbody = document.getElementById('tabelDataKoleksi' + RND);
        tbody.innerHTML = '<tr><td colspan="5" class="text-center py-5">'
            + '<div class="spinner-border text-primary mb-3"></div><br>'
            + '<span class="text-muted fw-medium"><%=Common.getBahasaConfig("Mengambil data...")%></span>'
            + '</td></tr>';

        // API halaman adalah 0-based page index
        const pageIndex = currentPage - 1;

        try {
            const res = await apiPost({
                action:  'daftar',
                class:   MODEL_CLASS,
                max:     LIMIT,
                halaman: pageIndex,
                where1:  buildWhere(),
                order1:  'desc',
                sort1:   'id',
                count:   'true'
            });

            totalRecords = parseInt(res.count || 0);
            const rows   = res.data || [];

            if (rows.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-5">'
                    + '<i class="fas fa-box-open fa-3x mb-3 opacity-50 d-block"></i>'
                    + '<%=Common.getBahasaConfigJS("Tidak ada data koleksi yang ditemukan.")%>'
                    + '</td></tr>';
            } else {
                // Nomor urut: offset = pageIndex * LIMIT
                let no   = pageIndex * LIMIT + 1;
                let html = '';
                rows.forEach(row => {
                    const safeNama   = (row.nama     || '-').replace(/"/g, '&quot;');
                    const kode       = (row.kode     || '-');
                    const deskripsi  = (row.deskripsi|| '-');
                    html += '<tr>'
                        + '<td class="text-center text-muted fw-medium">' + (no++) + '</td>'
                        + '<td class="text-start fw-bold text-secondary">'
                        +   '<span class="badge bg-light text-dark border">' + kode + '</span></td>'
                        + '<td class="text-start fw-semibold text-primary">' + safeNama + '</td>'
                        + '<td class="text-start text-muted small">' + deskripsi + '</td>'
                        + '<td class="text-center text-nowrap no-export">'
                        +   '<button class="btn btn-sm btn-outline-warning text-dark shadow-sm px-2 me-1 fw-bold"'
                        +     ' onclick="editKoleksi' + RND + '(' + row.id + ')"'
                        +     ' title="<%=Common.getBahasaConfig("Ubah Koleksi")%>"><i class="fas fa-edit"></i></button>'
                        +   '<button class="btn btn-sm btn-outline-danger shadow-sm px-2 fw-bold"'
                        +     ' onclick="hapusKoleksi' + RND + '(' + row.id + ')"'
                        +     ' title="<%=Common.getBahasaConfig("Hapus Data")%>"><i class="fas fa-trash-alt"></i></button>'
                        + '</td>'
                        + '</tr>';
                });
                tbody.innerHTML = html;
            }
            updatePaginationUI();
        } catch (err) {
            console.error('Gagal load koleksi:', err);
            tbody.innerHTML = '<tr><td colspan="5" class="text-center text-danger py-5">'
                + '<i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i>'
                + '<%=Common.getBahasaConfigJS("Gagal menarik data. Silakan muat ulang.")%>'
                + '</td></tr>';
        }
    };

    // ── Paging UI ──────────────────────────────────────────────────────────
    const updatePaginationUI = () => {
        const totalPages = Math.ceil(totalRecords / LIMIT) || 1;
        const startIdx   = (currentPage - 1) * LIMIT + 1;
        const endIdx     = Math.min(currentPage * LIMIT, totalRecords);
        const shown      = totalRecords > 0 ? startIdx : 0;

        document.getElementById('pageNumberDisplay' + RND).innerText =
            currentPage + ' / ' + totalPages;
        document.getElementById('pagingInfoKoleksi' + RND).innerHTML =
            '<i class="fas fa-list-ul text-primary me-2"></i>'
            + '<%=Common.getBahasaConfigJS("Menampilkan")%> <b class="text-dark">'
            + shown + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari")%> <b class="text-dark">'
            + totalRecords + '</b> <%=Common.getBahasaConfig("data")%>';

        document.getElementById('btnPrevPage' + RND).disabled = (currentPage <= 1);
        document.getElementById('btnNextPage' + RND).disabled = (currentPage >= totalPages);
    };

    window['changePageKoleksi' + RND] = (dir) => {
        const totalPages = Math.ceil(totalRecords / LIMIT) || 1;
        if (dir === -1 && currentPage > 1)          { currentPage--; loadData(); }
        else if (dir === 1 && currentPage < totalPages) { currentPage++; loadData(); }
    };

    // ── Form Tambah / Edit ─────────────────────────────────────────────────
    window['bukaFormTambah' + RND] = () => {
        document.getElementById('inputIdKoleksi' + RND).value = '';
        document.getElementById('formKoleksi'    + RND).reset();
        document.getElementById('formTitle'      + RND).innerHTML =
            '<i class="fas fa-plus-circle text-primary me-2"></i>'
            + '<%=Common.getBahasaConfigJS("Tambah Koleksi Baru")%>';
        document.getElementById('viewList' + RND).style.display = 'none';
        document.getElementById('viewForm' + RND).style.display = 'block';
    };

    const tutupForm = () => {
        document.getElementById('viewForm' + RND).style.display = 'none';
        document.getElementById('viewList' + RND).style.display = 'block';
        loadData();
    };
    window['tutupForm' + RND] = tutupForm;

    // ── Simpan ─────────────────────────────────────────────────────────────
    window['simpanKoleksi' + RND] = async () => {
        const idKoleksi = document.getElementById('inputIdKoleksi' + RND).value;
        const btnSimpan = document.getElementById('btnSimpanKoleksi' + RND);
        const oriLabel  = btnSimpan.innerHTML;

        const dataObj = {
            kode:      document.getElementById('inputKode'     + RND).value.trim() || null,
            nama:      document.getElementById('inputNama'     + RND).value.trim(),
            deskripsi: document.getElementById('inputDeskripsi'+ RND).value.trim() || null,
            aktif:     true
        };
        const payload = { action: 'simpanDataRinci', log: 'true', class: MODEL_CLASS, data: dataObj };
        if (idKoleksi !== '') payload.id = idKoleksi;

        btnSimpan.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>'
            + '<%=Common.getBahasaConfigJS("Menyimpan...")%>';
        btnSimpan.disabled = true;

        try {
            const res = await apiPost(payload);
            if (res.status === '00' || res.status === 'success' || res.id) {
                toast('<%=Common.getBahasaConfigJS("Data koleksi berhasil disimpan!")%>', 'bg-success text-white');
                tutupForm();
            } else {
                toast(res.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan data ke database.")%>',
                    'bg-danger text-white');
            }
        } catch (e) {
            console.error(e);
            toast('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi saat menyimpan.")%>',
                'bg-danger text-white');
        } finally {
            btnSimpan.innerHTML = oriLabel;
            btnSimpan.disabled  = false;
        }
    };

    // ── Edit ───────────────────────────────────────────────────────────────
    window['editKoleksi' + RND] = async (idKoleksi) => {
        try {
            const res = await apiPost({ action: 'load', class: MODEL_CLASS, id: idKoleksi });
            if (res.status === '00' || res.status === 'success' || res.id) {
                const d = res.data;
                document.getElementById('inputIdKoleksi' + RND).value  = d.id;
                document.getElementById('inputKode'      + RND).value  = d.kode      || '';
                document.getElementById('inputNama'      + RND).value  = d.nama      || '';
                document.getElementById('inputDeskripsi' + RND).value  = d.deskripsi || '';
                document.getElementById('formTitle'      + RND).innerHTML =
                    '<i class="fas fa-edit text-warning me-2"></i>'
                    + '<%=Common.getBahasaConfigJS("Ubah Data Koleksi")%>';
                document.getElementById('viewList' + RND).style.display = 'none';
                document.getElementById('viewForm' + RND).style.display = 'block';
            } else {
                toast('<%=Common.getBahasaConfigJS("Gagal mengambil rincian data dari server.")%>',
                    'bg-danger text-white');
            }
        } catch (e) {
            console.error(e);
            toast('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi.")%>', 'bg-danger text-white');
        }
    };

    // ── Hapus ──────────────────────────────────────────────────────────────
    window['hapusKoleksi' + RND] = async (id) => {
        // Coba pakai fungsi global prosesDeleteData / proseshapusDataRinci jika tersedia
        const globalHapus = typeof prosesDeleteData        === 'function' ? prosesDeleteData
                          : typeof proseshapusDataRinci    === 'function' ? proseshapusDataRinci
                          : null;

        if (globalHapus) {
            globalHapus(MODEL_CLASS, id, () => {
                if (document.querySelectorAll('#tabelDataKoleksi' + RND + ' tr').length <= 1
                        && currentPage > 1) currentPage--;
                loadData();
            });
            return;
        }

        // Fallback: logical delete manual
        if (!confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus koleksi ini?")%>')) return;
        try {
            const res = await apiPost({
                action: 'simpanDataRinci',
                class:  MODEL_CLASS,
                id:     id,
                data:   { aktif: false }
            });
            if (res.status === '00' || res.status === 'success') {
                toast('<%=Common.getBahasaConfigJS("Data berhasil dihapus.")%>', 'bg-success text-white');
                if (document.querySelectorAll('#tabelDataKoleksi' + RND + ' tr').length <= 1
                        && currentPage > 1) currentPage--;
                loadData();
            } else {
                toast('<%=Common.getBahasaConfigJS("Gagal menghapus data.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            toast('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan.")%>', 'bg-danger text-white');
        }
    };

    // ── Ekspor Excel ───────────────────────────────────────────────────────
    window['downloadExcelKoleksi' + RND] = () => {
        const table = document.getElementById('tabelHTMLDataKoleksi' + RND);
        if (!table) return;
        const clone = table.cloneNode(true);
        clone.querySelectorAll('.no-export, button').forEach(el => el.remove());
        const wb = XLSX.utils.table_to_book(clone, { sheet: 'Data Koleksi' });
        XLSX.writeFile(wb, 'Data_Koleksi_Repositori_' + new Date().getTime() + '.xlsx');
    };

    // ── Init ───────────────────────────────────────────────────────────────
    loadData();

}());
</script>
