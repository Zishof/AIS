<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.database.model.Menu"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
    String rnd    = Common.getGeneratedBarCode(7);
    Tbmuser tbmuser = Common.getCurrentUser(request);
    String  menu    = request.getParameter("menu") == null ? "" : request.getParameter("menu").trim();

    boolean edit    = false;
    boolean add     = false;
    boolean delete  = false;
    boolean isAdmin = false;

    if (tbmuser != null && tbmuser.getUserId() != null) {
        edit   = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser);
        delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE, tbmuser);
        add    = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE, tbmuser);
        if (Common.getApakahAdminLain(tbmuser)) {
            edit = true; delete = true; add = true; isAdmin = true;
        }
    }
%>

<!-- HEADER -->
<jsp:include page="header_repository.jsp" />

<div class="container animate__animated animate__fadeInUp">

    <!-- ── Search Card ──────────────────────────────────────────────────── -->
    <div class="card shadow-sm border-0 rounded-4 search-card-repo mb-4 p-4 bg-white">
        <div class="row justify-content-center">
            <div class="col-lg-10 col-xl-9">
                <div class="input-group input-group-lg search-group-repo">
                    <input type="text" id="inputKeyword<%=rnd%>" class="form-control"
                        placeholder="<%=Common.getBahasaConfig("Masukkan kata kunci pencarian...")%>"
                        onkeypress="if(event.key==='Enter') muatDataRepository<%=rnd%>()">
                    <select id="filterKoleksi<%=rnd%>" class="form-select">
                        <option value=""><%=Common.getBahasaConfig("Semua Koleksi")%></option>
                    </select>
                    <button class="btn btn-primary" type="button"
                        onclick="muatDataRepository<%=rnd%>()">
                        <i class="fas fa-search me-2"></i><%=Common.getBahasaConfig("Cari")%>
                    </button>
                </div>
            </div>
        </div>

        <div class="mt-4 d-flex justify-content-center gap-3 flex-wrap">
            <% if (add) { %>
            <button class="btn btn-outline-primary rounded-pill px-4 shadow-sm fw-bold"
                onclick="bukaFormTambah<%=rnd%>()">
                <i class="fas fa-plus-circle me-2"></i>
                <%=Common.getBahasaConfig("Tambah Repositori Baru")%>
            </button>
            <% } %>

            <% if (isAdmin) { %>
            <button class="btn btn-outline-primary rounded-pill px-4 shadow-sm fw-bold"
                onclick="bukaManajemenKoleksi<%=rnd%>()">
                <i class="fas fa-folder-open me-2"></i>
                <%=Common.getBahasaConfig("Manajemen Koleksi")%>
            </button>
            <% } %>

            <button class="btn btn-outline-secondary rounded-pill px-4 shadow-sm fw-bold"
                onclick="unduhExcelRepository<%=rnd%>()">
                <i class="fas fa-file-excel text-success me-2"></i>
                <%=Common.getBahasaConfig("Unduh Laporan Excel")%>
            </button>
        </div>
    </div>

    <!-- ── Stat Cards ───────────────────────────────────────────────────── -->
    <div class="row g-3 mb-4">
        <div class="col-6 col-lg-3">
            <div class="card border-0 shadow-sm rounded-4 h-100">
                <div class="card-body">
                    <small class="text-muted fw-bold text-uppercase"><%=Common.getBahasaConfig("Total Item")%></small>
                    <h3 class="text-primary fw-bold mb-0" id="statTotalItem<%=rnd%>">0</h3>
                </div>
            </div>
        </div>
        <div class="col-6 col-lg-3">
            <div class="card border-0 shadow-sm rounded-4 h-100">
                <div class="card-body">
                    <small class="text-muted fw-bold text-uppercase"><%=Common.getBahasaConfig("Koleksi")%></small>
                    <h3 class="text-success fw-bold mb-0" id="statCollection<%=rnd%>">0</h3>
                </div>
            </div>
        </div>
        <div class="col-6 col-lg-3">
            <div class="card border-0 shadow-sm rounded-4 h-100">
                <div class="card-body">
                    <small class="text-muted fw-bold text-uppercase"><%=Common.getBahasaConfig("Open Access")%></small>
                    <h3 class="text-info fw-bold mb-0" id="statOpenAccess<%=rnd%>">0</h3>
                </div>
            </div>
        </div>
        <div class="col-6 col-lg-3">
            <div class="card border-0 shadow-sm rounded-4 h-100">
                <div class="card-body">
                    <small class="text-muted fw-bold text-uppercase"><%=Common.getBahasaConfig("Indeks Turnitin")%></small>
                    <h3 class="text-warning fw-bold mb-0" id="statTurnitin<%=rnd%>">0</h3>
                </div>
            </div>
        </div>
    </div>

    <!-- ── Submenu Filter Jenis Dokumen ─────────────────────────────────── -->
    <div class="submenu-bar-repo d-flex justify-content-center flex-wrap shadow-sm mb-4">
        <a href="javascript:void(0)" class="nav-link submenu-repo-item<%=rnd%>"
            onclick="filterJenisDoc<%=rnd%>('')"
            data-doc="">
            <i class="fas fa-archive me-1"></i><%=Common.getBahasaConfig("Semua Koleksi")%>
        </a>
        <a href="javascript:void(0)" class="nav-link submenu-repo-item<%=rnd%>"
            onclick="filterJenisDoc<%=rnd%>('Thesis')"
            data-doc="Thesis">
            <i class="fas fa-graduation-cap me-1"></i><%=Common.getBahasaConfig("Tesis / Skripsi")%>
        </a>
        <a href="javascript:void(0)" class="nav-link submenu-repo-item<%=rnd%>"
            onclick="filterJenisDoc<%=rnd%>('Book')"
            data-doc="Book">
            <i class="fas fa-book me-1"></i><%=Common.getBahasaConfig("Buku")%>
        </a>
        <a href="javascript:void(0)" class="nav-link submenu-repo-item<%=rnd%>"
            onclick="filterJenisDoc<%=rnd%>('Article')"
            data-doc="Article">
            <i class="fas fa-file-alt me-1"></i><%=Common.getBahasaConfig("Jurnal Ilmiah")%>
        </a>
        <a href="javascript:void(0)" class="nav-link submenu-repo-item<%=rnd%>"
            onclick="filterJenisDoc<%=rnd%>('Research')"
            data-doc="Research">
            <i class="fas fa-flask me-1"></i><%=Common.getBahasaConfig("Penelitian / Pengabdian")%>
        </a>
        <a href="javascript:void(0)" class="nav-link submenu-repo-item<%=rnd%>"
            onclick="filterJenisDoc<%=rnd%>('Image')"
            data-doc="Image">
            <i class="far fa-images me-1"></i><%=Common.getBahasaConfig("Gambar")%>
        </a>
    </div>

    <!-- ── Tabel Hasil Pencarian ─────────────────────────────────────────── -->
    <div class="card shadow-sm border-0 rounded-4 mb-5">
        <div class="card-body px-4 pt-4 pb-4">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h5 class="text-primary fw-bold mb-0">
                    <i class="fas fa-list-ul me-2"></i>
                    <%=Common.getBahasaConfig("Hasil Pencarian Repositori")%>
                </h5>
                <small class="text-primary fw-bold bg-primary bg-opacity-10 px-3 py-1 rounded-pill"
                    id="infoTotal<%=rnd%>">
                    <%=Common.getBahasaConfig("Menampilkan data terbaru...")%>
                </small>
            </div>

            <div class="table-responsive shadow-sm rounded border">
                <table class="table table-hover table-striped align-middle mb-0">
                    <thead class="table-dark text-center">
                        <tr>
                            <th width="5%"  class="py-3"><%=Common.getBahasaConfig("No")%></th>
                            <th width="20%" class="py-3"><%=Common.getBahasaConfig("Identifier OAI")%></th>
                            <th width="35%" class="py-3"><%=Common.getBahasaConfig("Judul Karya")%></th>
                            <th width="15%" class="py-3"><%=Common.getBahasaConfig("Penulis")%></th>
                            <th width="10%" class="py-3"><%=Common.getBahasaConfig("Tahun")%></th>
                            <th width="15%" class="py-3"><%=Common.getBahasaConfig("Tindakan")%></th>
                        </tr>
                    </thead>
                    <tbody id="tbodyRepo<%=rnd%>">
                        <tr>
                            <td colspan="6" class="text-center text-muted py-5">
                                <i class="fas fa-spinner fa-spin fa-2x mb-3 text-primary"></i><br>
                                <%=Common.getBahasaConfig("Sistem sedang memuat data, mohon menunggu...")%>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

</div>

<!-- FOOTER -->
<jsp:include page="footer_repository.jsp" />

<script>
(function () {
    'use strict';

    // ── Konstanta ──────────────────────────────────────────────────────────
    const ROOT      = '<%=Common.ROOT%>';
    const RND       = '<%=rnd%>';
    const CAN_EDIT  = <%=edit%>;
    const CAN_DEL   = <%=delete%>;

    // Filter aktif dari submenu
    let activeDocType = '';

    // ── Escape SQL (single-quote) ──────────────────────────────────────────
    // Mengganti ' menjadi '' agar aman dimasukkan ke dalam SQL LIKE
    const escapeSql = (s) => (s || '').replace(/'/g, "''");

    // ── Generic API helper ─────────────────────────────────────────────────
    const apiPost = (body) =>
        fetch(ROOT + '/Data', {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify(body)
        }).then(r => r.json());

    // ── Statistik ──────────────────────────────────────────────────────────
    const muatStatistik = async () => {
        try {
            const data = await apiPost({
                action:     'sql',
                tanpaLogin: 'true',
                sql:
                    "SELECT "
                    + "(SELECT COUNT(*) FROM repo_item WHERE aktif = true OR aktif IS NULL) AS total_item,"
                    + "(SELECT COUNT(*) FROM repo_collection WHERE aktif = true OR aktif IS NULL) AS total_collection,"
                    + "(SELECT COUNT(*) FROM repo_item WHERE (aktif = true OR aktif IS NULL)"
                    + " AND access_policy = 'OPEN_ACCESS') AS open_access,"
                    + "(SELECT COUNT(*) FROM repo_item WHERE (aktif = true OR aktif IS NULL)"
                    + " AND turnitin_indexed = true) AS turnitin_indexed"
            });
            if (data && data.data && data.data.length > 0) {
                document.getElementById('statTotalItem'  + RND).innerHTML = data.data[0].total_item       || 0;
                document.getElementById('statCollection' + RND).innerHTML = data.data[0].total_collection || 0;
                document.getElementById('statOpenAccess' + RND).innerHTML = data.data[0].open_access      || 0;
                document.getElementById('statTurnitin'   + RND).innerHTML = data.data[0].turnitin_indexed || 0;
            }
        } catch (e) { console.error('Gagal muatStatistik:', e); }
    };

    // ── Dropdown Koleksi ───────────────────────────────────────────────────
    const muatFilterKoleksi = async () => {
        try {
            const res = await apiPost({
                action:     'daftar',
                tanpaLogin: 'true',
                class:      'ais.database.model.repository.RepoCollection',
                max:        200,
                order1:     'asc',
                sort1:      'nama'
            });
            const sel = document.getElementById('filterKoleksi' + RND);
            sel.innerHTML = '<option value=""><%=Common.getBahasaConfig("Semua Koleksi")%></option>';
            if (res && res.data) {
                res.data.forEach(c => {
                    const opt  = document.createElement('option');
                    opt.value  = c.id;
                    opt.text   = c.nama;
                    sel.appendChild(opt);
                });
            }
        } catch (e) { console.error('Gagal muatFilterKoleksi:', e); }
    };

    // ── Submenu Filter Jenis Dokumen ───────────────────────────────────────
    window['filterJenisDoc' + RND] = (docType) => {
        activeDocType = docType;
        // Tandai link aktif
        document.querySelectorAll('.submenu-repo-item' + RND).forEach(el => {
            el.classList.toggle('active', el.dataset.doc === docType);
        });
        muatData();
    };

    // ── Muat Data Tabel ────────────────────────────────────────────────────
    const muatData = async () => {
        const tbody     = document.getElementById('tbodyRepo'   + RND);
        const infoTotal = document.getElementById('infoTotal'   + RND);
        tbody.innerHTML =
            "<tr><td colspan='6' class='text-center py-5'>"
            + "<i class='fas fa-circle-notch fa-spin text-primary fa-3x mb-3'></i><br>"
            + "<span class='text-muted fs-5'><%=Common.getBahasaConfig("Sedang menyinkronkan data dari peladen...")%></span>"
            + "</td></tr>";

        // Sanitise semua input user sebelum masuk SQL
        const keyword   = escapeSql(
            (document.getElementById('inputKeyword' + RND).value || '').trim().toLowerCase()
        );
        const koleksiId = parseInt(
            document.getElementById('filterKoleksi' + RND).value || '0', 10
        ) || 0; // pastikan integer, bukan string arbitrary

        // ── Base query (JOIN item → metadata) ──
        let baseSql =
            "SELECT i.id, i.oai_identifier, i.document_type,"
            + " MAX(CASE WHEN m.metadata_field = 'dc.title'              THEN m.metadata_value END) AS judul,"
            + " MAX(CASE WHEN m.metadata_field = 'dc.contributor.author' THEN m.metadata_value END) AS penulis,"
            + " MAX(CASE WHEN m.metadata_field = 'dc.date.issued'        THEN m.metadata_value END) AS tahun"
            + " FROM repo_item i"
            + " LEFT JOIN repo_item_metadata m ON i.id = m.item_id"
            + " WHERE (i.aktif = true OR i.aktif IS NULL)";

        if (koleksiId > 0) {
            // Nilai sudah divalidasi integer — aman diinterpolasi
            baseSql += " AND i.collection_id = " + koleksiId;
        }
        if (activeDocType !== '') {
            // activeDocType berasal dari data-doc atribut yang di-hard-code JSP — aman
            baseSql += " AND i.document_type = '" + escapeSql(activeDocType) + "'";
        }
        baseSql += " GROUP BY i.id, i.oai_identifier, i.document_type";

        // ── Outer query dengan filter keyword ──
        let finalSql = "SELECT * FROM (" + baseSql + ") AS sub WHERE 1=1";
        if (keyword !== '') {
            // keyword sudah di-escapeSql() di atas
            finalSql += " AND (LOWER(judul) LIKE '%" + keyword + "%'"
                      + " OR LOWER(penulis) LIKE '%" + keyword + "%')";
        }
        finalSql += " ORDER BY id DESC LIMIT 50";

        try {
            const res = await apiPost({ action: 'sql', sql: finalSql, tanpaLogin: 'true' });
            const data = (res && res.data) ? res.data : [];

            if (data.length === 0) {
                tbody.innerHTML =
                    "<tr><td colspan='6' class='text-center text-muted py-5'>"
                    + "<i class='fas fa-search-minus fa-4x mb-3 opacity-50 text-primary'></i><br>"
                    + "<h5 class='text-primary'><%=Common.getBahasaConfig("Tidak ada entri yang ditemukan.")%></h5>"
                    + "<p><%=Common.getBahasaConfig("Silakan ubah kata kunci atau kriteria pencarian Anda.")%></p>"
                    + "</td></tr>";
                infoTotal.innerHTML = "<%=Common.getBahasaConfig("Ditemukan 0 entri")%>";
                return;
            }

            infoTotal.innerHTML =
                "<%=Common.getBahasaConfig("Menampilkan")%> " + data.length
                + " <%=Common.getBahasaConfig("entri teratas")%>";

            let html = '';
            data.forEach((row, idx) => {
                const no       = idx + 1;
                const itemId   = row.id;
                const oaiStr   = row.oai_identifier || '-';
                const judulStr = row.judul           || '-';
                const penulisStr = row.penulis       || '-';
                const tahunStr   = row.tahun         || '-';

                let tombol =
                    "<button class='btn btn-sm btn-outline-success shadow-sm me-1 mb-1'"
                    + " onclick='tampilkanDetail" + RND + "(" + itemId + ")'"
                    + " title='<%=Common.getBahasaConfigJS("Lihat Rincian")%>'>"
                    + "<i class='fas fa-eye'></i></button>";

                if (CAN_EDIT) {
                    tombol +=
                        "<button class='btn btn-sm btn-outline-warning shadow-sm me-1 mb-1'"
                        + " onclick='ubahRepository" + RND + "(" + itemId + ")'"
                        + " title='<%=Common.getBahasaConfigJS("Perbarui Data")%>'>"
                        + "<i class='fas fa-edit'></i></button>";
                }
                if (CAN_DEL) {
                    tombol +=
                        "<button class='btn btn-sm btn-outline-danger shadow-sm mb-1'"
                        + " onclick='hapusRepository" + RND + "(" + itemId + ")'"
                        + " title='<%=Common.getBahasaConfigJS("Hapus Entri")%>'>"
                        + "<i class='fas fa-trash-alt'></i></button>";
                }

                html +=
                    "<tr>"
                    + "<td class='text-center fw-bold text-primary'>" + no + "</td>"
                    + "<td><span class='badge bg-primary bg-opacity-10 text-primary border border-primary"
                    +   " shadow-sm rounded-pill'><i class='fas fa-link me-1'></i> " + oaiStr + "</span></td>"
                    + "<td class='fw-bold text-primary'>" + judulStr + "</td>"
                    + "<td><i class='far fa-user-circle text-primary me-1'></i>"
                    +   "<span class='text-primary'>" + penulisStr + "</span></td>"
                    + "<td class='text-center fw-semibold text-primary'>" + tahunStr + "</td>"
                    + "<td class='text-center'>" + tombol + "</td>"
                    + "</tr>";
            });
            tbody.innerHTML = html;

        } catch (e) {
            console.error('Gagal muatData:', e);
            tbody.innerHTML =
                "<tr><td colspan='6' class='text-center text-danger py-5'>"
                + "<i class='fas fa-exclamation-triangle fa-2x mb-3 d-block'></i>"
                + "<%=Common.getBahasaConfig("Gagal memuat data. Periksa koneksi server.")%>"
                + "</td></tr>";
        }
    };
    window['muatDataRepository' + RND] = muatData;

    // ── Hapus ──────────────────────────────────────────────────────────────
    window['hapusRepository' + RND] = (id) => {
        const fn = typeof prosesDeleteData === 'function'     ? prosesDeleteData
                 : typeof proseshapusDataRinci === 'function' ? proseshapusDataRinci
                 : null;
        if (fn) {
            fn('ais.database.model.repository.RepoItem', id, () => muatData());
        } else {
            if (!confirm('<%=Common.getBahasaConfigJS("Hapus item ini?")%>')) return;
            apiPost({
                action: 'simpanDataRinci',
                class:  'ais.database.model.repository.RepoItem',
                id:     id,
                data:   { aktif: false }
            }).then(() => muatData()).catch(console.error);
        }
    };

    // ── Buka Form Tambah / Ubah ────────────────────────────────────────────
    window['bukaFormTambah'   + RND] = () => bukaModalForm('');
    window['ubahRepository'   + RND] = (id) => bukaModalForm(id);

    const bukaModalForm = async (itemId) => {
        const modalId = 'modalFormRepo_' + RND;
        const el      = document.getElementById(modalId);
        if (el) el.remove();

        const judulModal = itemId === ''
            ? '<%=Common.getBahasaConfigJS("Form Pendaftaran Koleksi Repositori")%>'
            : '<%=Common.getBahasaConfigJS("Perbarui Data Repositori")%>';
        const iconClass  = itemId === '' ? 'fa-cloud-upload-alt' : 'fa-edit';

        // URL form dimuat dengan hanya_tampil_jsp=true agar tidak memuat layout penuh
        const urlForm = ROOT + '/repository?hanya_tampil_jsp=true&p=repository&s=FormRepository'
            + (itemId !== '' ? '&id=' + encodeURIComponent(itemId) : '');

        document.body.insertAdjacentHTML('beforeend',
            "<div class='modal fade' id='" + modalId + "' tabindex='-1'"
            + " aria-hidden='true' data-bs-backdrop='static'>"
            + "<div class='modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable'>"
            + "<div class='modal-content border-0 shadow-lg'>"
            + "<div class='modal-header bg-primary text-white'>"
            + "<h5 class='modal-title fw-bold text-white'>"
            + "<i class='fas " + iconClass + " me-2'></i> " + judulModal + "</h5>"
            + "<button type='button' class='btn-close btn-close-white shadow-sm'"
            + " data-bs-dismiss='modal' aria-label='Close'></button>"
            + "</div>"
            + "<div class='modal-body bg-light p-0' id='kontenFormRepo" + RND + "'>"
            + "<div class='text-center py-5 my-5'>"
            + "<i class='fas fa-sync fa-spin fa-4x text-primary mb-3'></i>"
            + "<h5 class='text-muted'><%=Common.getBahasaConfig("Menyiapkan formulir...")%></h5>"
            + "</div></div></div></div></div>");

        const modalObj = new bootstrap.Modal(document.getElementById(modalId));
        modalObj.show();

        try {
            const html      = await fetch(urlForm).then(r => r.text());
            const container = document.getElementById('kontenFormRepo' + RND);
            container.innerHTML = html;
            // Re-eksekusi script yang ada dalam konten
            container.querySelectorAll('script').forEach(old => {
                const s = document.createElement('script');
                [...old.attributes].forEach(a => s.setAttribute(a.name, a.value));
                s.text = old.text;
                old.replaceWith(s);
            });
        } catch (e) {
            document.getElementById('kontenFormRepo' + RND).innerHTML =
                "<div class='p-5 text-center text-danger'>"
                + "<i class='fas fa-exclamation-triangle fa-3x mb-3'></i><br>"
                + "<h5><%=Common.getBahasaConfig("Terjadi kesalahan teknis saat memuat formulir.")%></h5>"
                + "</div>";
        }
    };

    // ── Manajemen Koleksi ──────────────────────────────────────────────────
    window['bukaManajemenKoleksi' + RND] = async () => {
        const modalId = 'modalManajemenKoleksi_' + RND;
        const el      = document.getElementById(modalId);
        if (el) el.remove();

        document.body.insertAdjacentHTML('beforeend',
            "<div class='modal fade' id='" + modalId + "' tabindex='-1'"
            + " aria-hidden='true' data-bs-backdrop='static'>"
            + "<div class='modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable'>"
            + "<div class='modal-content border-0 shadow-lg'>"
            + "<div class='modal-header bg-primary text-white'>"
            + "<h5 class='modal-title fw-bold text-white'>"
            + "<i class='fas fa-folder-open me-2'></i>"
            + "<%=Common.getBahasaConfig("Manajemen Koleksi Repositori")%></h5>"
            + "<button type='button' class='btn-close btn-close-white shadow-sm'"
            + " data-bs-dismiss='modal' aria-label='Close'></button>"
            + "</div>"
            + "<div class='modal-body bg-light p-0' id='kontenManajemenKoleksi" + RND + "'>"
            + "<div class='text-center py-5 my-5'>"
            + "<i class='fas fa-sync fa-spin fa-4x text-primary mb-3'></i>"
            + "<h5 class='text-muted'><%=Common.getBahasaConfig("Menyiapkan antarmuka manajemen koleksi...")%></h5>"
            + "</div></div></div></div></div>");

        const modalObj = new bootstrap.Modal(document.getElementById(modalId));
        modalObj.show();

        const urlKoleksi = ROOT + '/repository?hanya_tampil_jsp=true&p=repository&s=_repo_collection';
        try {
            const html      = await fetch(urlKoleksi).then(r => r.text());
            const container = document.getElementById('kontenManajemenKoleksi' + RND);
            container.innerHTML = html;
            container.querySelectorAll('script').forEach(old => {
                const s = document.createElement('script');
                [...old.attributes].forEach(a => s.setAttribute(a.name, a.value));
                s.text = old.text;
                old.replaceWith(s);
            });
            // Setelah modal koleksi ditutup, refresh dropdown koleksi di halaman utama
            document.getElementById(modalId).addEventListener('hidden.bs.modal', () => {
                muatFilterKoleksi();
            });
        } catch (e) {
            document.getElementById('kontenManajemenKoleksi' + RND).innerHTML =
                "<div class='p-5 text-center text-danger'>"
                + "<i class='fas fa-exclamation-triangle fa-3x mb-3'></i><br>"
                + "<h5><%=Common.getBahasaConfig("Terjadi kesalahan teknis saat memuat modul koleksi.")%></h5>"
                + "</div>";
        }
    };

    // ── Tampilkan Detail ───────────────────────────────────────────────────
    window['tampilkanDetail' + RND] = async (itemId) => {
        const modalId = 'modalDetail_' + RND;
        const el      = document.getElementById(modalId);
        if (el) el.remove();

        document.body.insertAdjacentHTML('beforeend',
            "<div class='modal fade' id='" + modalId + "' tabindex='-1' aria-hidden='true'>"
            + "<div class='modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable'>"
            + "<div class='modal-content border-0 shadow-lg'>"
            + "<div class='modal-header bg-primary text-white'>"
            + "<h5 class='modal-title fw-bold text-white'>"
            + "<i class='fas fa-info-circle me-2'></i>"
            + "<%=Common.getBahasaConfig("Rincian Metadata Dokumen")%></h5>"
            + "<button type='button' class='btn-close btn-close-white shadow-sm'"
            + " data-bs-dismiss='modal' aria-label='Close'></button>"
            + "</div>"
            + "<div class='modal-body bg-light'>"
            + "<div class='text-center py-5' id='loadingDetail" + RND + "'>"
            + "<i class='fas fa-spinner fa-spin fa-3x text-primary mb-3'></i>"
            + "<h6 class='text-muted'><%=Common.getBahasaConfig("Menarik rincian metadata dari peladen...")%></h6>"
            + "</div>"
            + "<div id='kontenDetail" + RND + "' style='display:none;'></div>"
            + "</div>"
            + "<div class='modal-footer bg-white border-top-0'>"
            + "<button type='button' class='btn btn-secondary shadow-sm rounded-pill px-4'"
            + " data-bs-dismiss='modal'>"
            + "<i class='fas fa-times me-1'></i><%=Common.getBahasaConfig("Tutup Jendela")%>"
            + "</button></div></div></div></div>");

        const modalObj = new bootstrap.Modal(document.getElementById(modalId));
        modalObj.show();

        try {
            // Load item + koleksi
            const resItem = await apiPost({
                action:     'sql',
                tanpaLogin: 'true',
                sql: 'SELECT i.oai_identifier, c.nama'
                   + ' FROM repo_item i'
                   + ' LEFT JOIN repo_collection c ON i.collection_id = c.id'
                   + ' WHERE i.id = ' + parseInt(itemId, 10)
            });
            let oaiId = '-', namaKoleksi = '-';
            if (resItem && resItem.data && resItem.data.length > 0) {
                oaiId       = resItem.data[0].oai_identifier || '-';
                namaKoleksi = resItem.data[0].nama           || '-';
            }

            // Load metadata EAV
            const resMd = await apiPost({
                action:     'sql',
                tanpaLogin: 'true',
                sql: 'SELECT metadata_field, metadata_value'
                   + ' FROM repo_item_metadata'
                   + ' WHERE item_id = ' + parseInt(itemId, 10)
                   + ' AND (aktif IS NULL OR aktif = true)'
            });
            let judul = '-', penulis = '-', tahun = '-', abstrak = '-';
            if (resMd && resMd.data) {
                resMd.data.forEach(row => {
                    switch (row.metadata_field) {
                        case 'dc.title':                judul   = row.metadata_value; break;
                        case 'dc.contributor.author':   penulis = row.metadata_value; break;
                        case 'dc.date.issued':          tahun   = row.metadata_value; break;
                        case 'dc.description.abstract': abstrak = row.metadata_value; break;
                    }
                });
            }

            // Load bitstream — URL unduh via RepoBitstream (jenis-based lookup)
            const resFile = await apiPost({
                action:     'sql',
                tanpaLogin: 'true',
                sql: 'SELECT id, nama_file'
                   + ' FROM repo_bitstream'
                   + ' WHERE item_id = ' + parseInt(itemId, 10)
                   + ' AND (aktif IS NULL OR aktif = true)'
            });
            let fileHtml = '';
            if (resFile && resFile.data && resFile.data.length > 0) {
                fileHtml = "<ul class='list-group shadow-sm'>";
                resFile.data.forEach(row => {
                    // URL unduh: Download servlet mencari LampiranLain berdasarkan jenis + sourceId
                    const dlUrl = ROOT + '/Download?id=' + row.id
                        + '&clazz=ais.database.model.file.LampiranLain'
                        + '&jenis=ais.database.model.repository.RepoBitstream';
                    fileHtml +=
                        "<li class='list-group-item d-flex justify-content-between align-items-center bg-white'>"
                        + "<div><i class='fas fa-file-pdf text-danger me-2 fa-lg'></i>"
                        + "<span class='fw-medium text-dark'>" + row.nama_file + "</span></div>"
                        + "<a href='" + dlUrl + "' target='_blank'"
                        + " class='btn btn-sm btn-outline-success rounded-pill px-3 shadow-sm'>"
                        + "<i class='fas fa-download me-1'></i><%=Common.getBahasaConfig("Unduh")%></a>"
                        + "</li>";
                });
                fileHtml += "</ul>";
            } else {
                fileHtml =
                    "<div class='alert alert-secondary border-0 mb-0 shadow-sm'>"
                    + "<i class='fas fa-info-circle me-2'></i>"
                    + "<%=Common.getBahasaConfig("Tidak ada lampiran dokumen yang tersedia.")%></div>";
            }

            const renderHtml =
                "<div class='card border-0 shadow-sm mb-3 rounded-3'>"
                + "<div class='card-header bg-white border-bottom pt-3 pb-2'>"
                + "<h6 class='mb-0 text-primary fw-bold'>"
                + "<i class='fas fa-bookmark me-2'></i>"
                + "<%=Common.getBahasaConfig("Informasi Identitas")%></h6></div>"
                + "<div class='card-body py-2'>"
                + "<table class='table table-borderless table-sm mb-0'>"
                + "<tr><td width='30%' class='text-primary fw-bold align-middle'>"
                + "<%=Common.getBahasaConfig("Identifier OAI")%></td>"
                + "<td width='5%' class='align-middle text-primary'>:</td>"
                + "<td class='fw-medium align-middle'>"
                + "<span class='badge bg-primary bg-opacity-10 text-primary border border-primary px-2 py-1 rounded-pill'>"
                + oaiId + "</span></td></tr>"
                + "<tr><td class='text-primary fw-bold align-middle'>"
                + "<%=Common.getBahasaConfig("Koleksi Repositori")%></td>"
                + "<td class='align-middle text-primary'>:</td>"
                + "<td class='fw-medium text-dark align-middle'>" + namaKoleksi + "</td></tr>"
                + "</table></div></div>"

                + "<div class='card border-0 shadow-sm mb-3 rounded-3'>"
                + "<div class='card-header bg-white border-bottom pt-3 pb-2'>"
                + "<h6 class='mb-0 text-primary fw-bold'><i class='fas fa-tags me-2'></i>"
                + "<%=Common.getBahasaConfig("Metadata Lengkap")%> <small class='text-muted fw-normal'>(Dublin Core)</small>"
                + "</h6></div>"
                + "<div class='card-body py-2'>"
                + "<table class='table table-borderless table-sm mb-0'>"
                + "<tr><td width='30%' class='text-primary fw-bold'>"
                + "<%=Common.getBahasaConfig("Judul Karya")%> <small class='text-muted d-block'>(dc.title)</small></td>"
                + "<td width='5%' class='text-primary'>:</td>"
                + "<td class='fw-bold text-dark fs-6'>" + judul + "</td></tr>"
                + "<tr><td class='text-primary fw-bold'>"
                + "<%=Common.getBahasaConfig("Penulis")%> <small class='text-muted d-block'>(dc.contributor.author)</small></td>"
                + "<td class='text-primary'>:</td>"
                + "<td class='fw-medium text-dark'>" + penulis + "</td></tr>"
                + "<tr><td class='text-primary fw-bold'>"
                + "<%=Common.getBahasaConfig("Tahun Terbit")%> <small class='text-muted d-block'>(dc.date.issued)</small></td>"
                + "<td class='text-primary'>:</td>"
                + "<td class='fw-medium text-dark'>" + tahun + "</td></tr>"
                + "<tr><td class='text-primary fw-bold'>"
                + "<%=Common.getBahasaConfig("Abstrak")%> <small class='text-muted d-block'>(dc.description.abstract)</small></td>"
                + "<td class='text-primary'>:</td>"
                + "<td class='fw-medium text-dark text-justify' style='white-space:pre-line;text-align:justify;'>"
                + abstrak + "</td></tr>"
                + "</table></div></div>"

                + "<div class='card border-0 shadow-sm rounded-3'>"
                + "<div class='card-header bg-white border-bottom pt-3 pb-2'>"
                + "<h6 class='mb-0 text-primary fw-bold'><i class='fas fa-copy me-2'></i>"
                + "<%=Common.getBahasaConfig("Lampiran Dokumen")%></h6></div>"
                + "<div class='card-body p-3 bg-light rounded-bottom'>" + fileHtml + "</div>"
                + "</div>";

            document.getElementById('loadingDetail' + RND).style.display = 'none';
            const konten = document.getElementById('kontenDetail' + RND);
            konten.style.display = 'block';
            konten.innerHTML     = renderHtml;

        } catch (e) {
            console.error(e);
            document.getElementById('loadingDetail' + RND).style.display = 'none';
            const konten = document.getElementById('kontenDetail' + RND);
            konten.style.display = 'block';
            konten.innerHTML =
                "<div class='alert alert-danger border-0 shadow-sm rounded-3 p-4 text-center'>"
                + "<i class='fas fa-exclamation-triangle fa-3x mb-3 text-danger'></i><br>"
                + "<h6 class='fw-bold mb-0'>"
                + "<%=Common.getBahasaConfig("Terjadi kesalahan teknis saat menarik rincian data repositori.")%>"
                + "</h6></div>";
        }
    };

    // ── Unduh CSV ──────────────────────────────────────────────────────────
    window['unduhExcelRepository' + RND] = async () => {
        const keyword   = escapeSql(
            (document.getElementById('inputKeyword' + RND).value || '').trim().toLowerCase()
        );
        const koleksiId = parseInt(
            document.getElementById('filterKoleksi' + RND).value || '0', 10
        ) || 0;

        let baseSql =
            "SELECT i.oai_identifier,"
            + " MAX(CASE WHEN m.metadata_field = 'dc.title'              THEN m.metadata_value END) AS judul,"
            + " MAX(CASE WHEN m.metadata_field = 'dc.contributor.author' THEN m.metadata_value END) AS penulis,"
            + " MAX(CASE WHEN m.metadata_field = 'dc.date.issued'        THEN m.metadata_value END) AS tahun"
            + " FROM repo_item i"
            + " LEFT JOIN repo_item_metadata m ON i.id = m.item_id"
            + " WHERE (i.aktif = true OR i.aktif IS NULL)";

        if (koleksiId > 0) baseSql += " AND i.collection_id = " + koleksiId;
        if (activeDocType !== '') baseSql += " AND i.document_type = '" + escapeSql(activeDocType) + "'";
        baseSql += " GROUP BY i.id, i.oai_identifier";

        let finalSql = "SELECT oai_identifier, judul, penulis, tahun FROM (" + baseSql + ") AS sub WHERE 1=1";
        if (keyword !== '') {
            finalSql += " AND (LOWER(judul) LIKE '%" + keyword + "%'"
                      + " OR LOWER(penulis) LIKE '%" + keyword + "%')";
        }
        finalSql += " ORDER BY judul ASC";

        const res  = await apiPost({ action: 'sql', sql: finalSql, tanpaLogin: 'true' });
        const data = (res && res.data) ? res.data : [];

        if (data.length === 0) {
            if (typeof tampilkanToast === 'function')
                tampilkanToast(
                    '<%=Common.getBahasaConfigJS("Maaf, tidak ada data yang memenuhi kriteria pencarian untuk diunduh.")%>',
                    'bg-warning text-dark');
            return;
        }

        let csv = "﻿"; // BOM UTF-8 agar Excel bisa baca karakter Indonesia
        csv += '<%=Common.getBahasaConfigJS("Identifier OAI,Judul Karya Ilmiah,Nama Penulis,Tahun Penerbitan")%>\n';
        data.forEach(row => {
            const cols = [row.oai_identifier, row.judul, row.penulis, row.tahun];
            csv += cols.map(c => c ? '"' + String(c).replace(/"/g, '""') + '"' : '""').join(',') + '\n';
        });

        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const url  = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href     = url;
        link.download = 'Laporan_Koleksi_Repositori_' + new Date().toISOString().slice(0, 10) + '.csv';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);

        if (typeof tampilkanToast === 'function')
            tampilkanToast(
                '<%=Common.getBahasaConfigJS("Laporan berhasil diunduh ke perangkat Anda.")%>',
                'bg-success text-white');
    };

    // ── Listener: setelah form simpan, reload tabel ────────────────────────
    document.addEventListener('repositorySavedEvent', () => muatData());

    // ── Init ───────────────────────────────────────────────────────────────
    muatStatistik();
    muatFilterKoleksi();
    muatData();

}());
</script>
