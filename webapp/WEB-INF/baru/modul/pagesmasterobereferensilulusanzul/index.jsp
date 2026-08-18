<%@page import="java.util.List"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.database.model.obe.ReferensiLulusan"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.common.ConstantValues"%>
<%
// 1. SECURITY & INITIALIZATION CHECK
String rnd = Common.getGeneratedBarCode(7);
Tbmuser tbmuser = Common.getCurrentUser(request);

if (tbmuser == null || tbmuser.getUserId() == null) {
    response.setStatus(401);
    out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
    return;
}

// 2. PRIVILEGES CHECK
boolean edit = false;
boolean add = false;
boolean delete = false;
boolean isAdmin = false;

if (tbmuser != null && tbmuser.getUserId() != null) {
    edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser);
    delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE, tbmuser);
    add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE, tbmuser);
    
    if (Common.getApakahAdminLain(tbmuser)) {
        edit = true;
        delete = true;
        add = true;
        isAdmin = true;
    }
}
%>

<style>
    @media print {
        body * { visibility: hidden; }
        #printArea<%=rnd%>, #printArea<%=rnd%> * { visibility: visible; }
        #printArea<%=rnd%> { position: absolute; left: 0; top: 0; width: 100%; }
        .no-print { display: none !important; }
        .table { border-collapse: collapse !important; width: 100% !important; }
        .table th, .table td { border: 1px solid #000 !important; padding: 5px !important; }
    }
</style>

<div id="viewList<%=rnd%>" class="animate__animated animate__fadeInUp">
    <div class="row mb-4">
        <div class="col-12">
            <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
                
                <div class="card-header bg-white border-0 pt-4 pb-3 px-4 border-bottom border-light no-print">
                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-2">
                        <div>
                            <h5 class="fw-bold text-dark mb-1">
                                <i class="fas fa-bookmark text-primary me-2"></i><%=Common.getBahasaConfig("Manajemen Referensi Lulusan")%>
                            </h5>
                            <small class="text-muted"><%=Common.getBahasaConfig("Kelola data referensi/daftar pustaka standar tingkat institusi.")%></small>
                        </div>
                        
                        <div class="d-flex align-items-center gap-2 flex-wrap">
                            <div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;">
                                <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>
                                <input type="text" class="form-control border-start-0 ps-0 bg-white fw-medium" id="searchReferensi<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Kode / Nama Referensi...")%>">
                            </div>
                            
                            <button class="btn btn-sm btn-outline-info rounded-pill px-3 shadow-sm fw-bold" type="button" data-bs-toggle="collapse" data-bs-target="#filterPanel<%=rnd%>">
                                <i class="fas fa-filter me-1"></i><%=Common.getBahasaConfig("Saring")%>
                            </button>

                            <button class="btn btn-sm btn-outline-secondary rounded-pill px-3 shadow-sm fw-bold" onclick="resetAndLoadData<%=rnd%>()" title="<%=Common.getBahasaConfig("Muat Ulang Data")%>">
                                <i class="fas fa-sync-alt"></i>
                            </button>

                            <div class="btn-group shadow-sm">
                                <button class="btn btn-sm btn-outline-success fw-bold" onclick="downloadExcel<%=rnd%>()" title="<%=Common.getBahasaConfig("Unduh Data Excel")%>"><i class="fas fa-file-excel me-1"></i>Ekspor</button>
                                <% if (add && edit) { %>
                                <button class="btn btn-sm btn-outline-primary fw-bold" onclick="document.getElementById('fileUploadExcel<%=rnd%>').click()" title="<%=Common.getBahasaConfig("Unggah Data Excel")%>"><i class="fas fa-upload me-1"></i>Impor</button>
                                <input type="file" id="fileUploadExcel<%=rnd%>" class="d-none" accept=".xlsx, .xls" onchange="uploadExcel<%=rnd%>(this)">
                                <% } %>
                                <button class="btn btn-sm btn-outline-danger fw-bold" onclick="cetakPDF<%=rnd%>()" title="<%=Common.getBahasaConfig("Cetak PDF")%>"><i class="fas fa-print me-1"></i>PDF</button>
                            </div>
                            
                            <% if (add) { %>
                            <button class="btn btn-sm btn-primary rounded-pill px-4 shadow-sm fw-bold text-nowrap" onclick="bukaFormReferensi<%=rnd%>()">
                                <i class="fas fa-plus-circle me-1"></i><%=Common.getBahasaConfig("Tambah Data")%>
                            </button>
                            <% } %>
                        </div>
                    </div>

                    <div class="collapse mt-3" id="filterPanel<%=rnd%>">
                        <div class="card card-body bg-light border-0 shadow-sm rounded-3 p-3">
                            <h6 class="fw-bold text-secondary mb-3"><i class="fas fa-sliders-h me-2"></i><%=Common.getBahasaConfig("Penyaringan Lanjutan")%></h6>
                            <div class="row g-3">
                                <div class="col-md-4">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Status Aktif")%></label>
                                    <select id="filterStatus<%=rnd%>" class="form-select form-select-sm shadow-sm">
                                        <option value="ALL"><%=Common.getBahasaConfig("-- Semua Status --")%></option>
                                        <option value="TRUE" selected><%=Common.getBahasaConfig("Aktif")%></option>
                                        <option value="FALSE"><%=Common.getBahasaConfig("Tidak Aktif")%></option>
                                    </select>
                                </div>
                                <div class="col-12 text-end mt-3">
                                    <button type="button" class="btn btn-sm btn-info text-white px-4 fw-bold shadow-sm rounded-pill" onclick="resetAndLoadData<%=rnd%>()">
                                        <i class="fas fa-search me-1"></i> <%=Common.getBahasaConfig("Terapkan Penyaringan")%>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                
                <div class="card-body p-4 pt-3" id="printArea<%=rnd%>">
                    <div class="table-responsive border rounded-3 shadow-sm bg-white">
                        <table class="table table-hover table-striped align-middle mb-0" id="tabelHTMLDataReferensi<%=rnd%>">
                            <thead class="table-dark text-center align-middle">
                                <tr>
                                    <th class="fw-semibold text-uppercase small px-3 py-3" style="width: 50px;">#</th>
                                    <th class="text-start fw-semibold text-uppercase small px-3" style="width: 150px;"><i class="fas fa-barcode me-1"></i><%=Common.getBahasaConfig("Kode")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-quote-right me-1"></i><%=Common.getBahasaConfig("Nama Referensi / Sitasi")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-info-circle me-1"></i><%=Common.getBahasaConfig("Keterangan")%></th>
                                    <th class="text-center fw-semibold text-uppercase small px-3"><i class="fas fa-toggle-on me-1"></i><%=Common.getBahasaConfig("Status")%></th>
                                    <th class="fw-semibold text-uppercase small px-3 text-center no-print" style="width: 100px;"><i class="fas fa-cogs"></i></th>
                                </tr>
                            </thead>
                            <tbody id="tabelDataReferensi<%=rnd%>">
                                <tr><td colspan="6" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Memuat data...")%></span></td></tr>
                            </tbody>
                        </table>
                    </div>

                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mt-4 no-print">
                        <div class="small fw-medium text-muted bg-light px-3 py-1 rounded-pill border" id="pagingInfoReferensi<%=rnd%>"></div>
                        <nav>
                            <ul class="pagination pagination-sm mb-0 shadow-sm">
                                <li class="page-item"><button class="page-link text-primary rounded-start-pill px-3 fw-bold" id="btnPrevPage<%=rnd%>" onclick="changePageReferensi<%=rnd%>(-1)"><i class="fas fa-chevron-left"></i></button></li>
                                <li class="page-item disabled"><span class="page-link bg-light text-dark px-3 fw-bold" id="pageNumberDisplay<%=rnd%>">1</span></li>
                                <li class="page-item"><button class="page-link text-primary rounded-end-pill px-3 fw-bold" id="btnNextPage<%=rnd%>" onclick="changePageReferensi<%=rnd%>(1)"><i class="fas fa-chevron-right"></i></button></li>
                            </ul>
                        </nav>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    // ==========================================
    // BAGIAN 1: VARIABEL GLOBAL & STATE
    // ==========================================
    const classModel<%=rnd%> = "<%=ReferensiLulusan.class.getName()%>";
    
    let currentPage<%=rnd%> = 1;
    const limitPerPage<%=rnd%> = 10;
    let totalRecords<%=rnd%> = 0;
    
    const privEdit<%=rnd%> = <%= edit %>;
    const privDelete<%=rnd%> = <%= delete %>;

    // ==========================================
    // BAGIAN 2: INJEKSI DOM (MODAL & LOADING)
    // ==========================================
    const injectDOMElements<%=rnd%> = () => {
        if(!document.getElementById('loadingOverlay<%=rnd%>')) {
            var overlayHtml = 
            '<div id="loadingOverlay<%=rnd%>" class="d-none" style="position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(255,255,255,0.75); z-index:10050; justify-content:center; align-items:center; flex-direction:column; backdrop-filter: blur(2px);">' +
                '<div class="spinner-border text-primary mb-3" style="width: 4rem; height: 4rem; border-width: 0.35em;" role="status"></div>' +
                '<h6 class="fw-bold text-dark m-0 shadow-sm px-4 py-2 bg-white rounded-pill border"><i class="fas fa-hourglass-half text-warning me-2"></i><%=Common.getBahasaConfig("Sedang Memproses...")%></h6>' +
            '</div>';
            document.body.insertAdjacentHTML('beforeend', overlayHtml);
        }

        if(!document.getElementById('modalFormReferensi<%=rnd%>')) {
            var modalHtml = 
            '<div class="modal fade" id="modalFormReferensi<%=rnd%>" tabindex="-1" aria-labelledby="modalLabel<%=rnd%>" aria-hidden="true" data-bs-backdrop="static">' +
                '<div class="modal-dialog modal-dialog-centered modal-lg modal-dialog-scrollable">' +
                    '<div class="modal-content border-0 shadow-lg rounded-4">' +
                        '<div class="modal-header border-bottom-0 pb-0 pt-4 px-4">' +
                            '<h5 class="modal-title fw-bold text-dark" id="modalLabel<%=rnd%>">' +
                                '<i class="fas fa-edit text-primary me-2"></i><span id="formTitleText<%=rnd%>"><%=Common.getBahasaConfig("Formulir Referensi Lulusan")%></span>' +
                            '</h5>' +
                            '<button type="button" class="btn-close shadow-sm" data-bs-dismiss="modal" aria-label="Close"></button>' +
                        '</div>' +
                        '<div class="modal-body p-4">' +
                            '<form id="formReferensi<%=rnd%>" onsubmit="event.preventDefault(); simpanReferensi<%=rnd%>();">' +
                                '<input type="hidden" id="inputIdReferensi<%=rnd%>">' +
                                
                                '<div class="row g-3">' +
                                    '<div class="col-md-4">' +
                                        '<label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Kode Referensi")%></label>' +
                                        '<input type="text" class="form-control shadow-sm fw-bold" id="inputKode<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cth: REF-01")%>">' +
                                    '</div>' +

                                    '<div class="col-md-12">' +
                                        '<label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Referensi / Sitasi")%> <span class="text-danger">*</span></label>' +
                                        '<textarea class="form-control shadow-sm fw-semibold" id="inputNama<%=rnd%>" rows="3" placeholder="<%=Common.getBahasaConfig("Contoh: M. F. Zein, Panduan Menggunakan Media Sosial, Jakarta: 2019...")%>" required></textarea>' +
                                    '</div>' +

                                    '<div class="col-md-12 mt-3">' +
                                        '<label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Keterangan Tambahan")%></label>' +
                                        '<textarea class="form-control shadow-sm" id="inputKeterangan<%=rnd%>" rows="2" placeholder="<%=Common.getBahasaConfig("Catatan opsional...")%>"></textarea>' +
                                    '</div>' +

                                    '<div class="col-md-12 mt-3">' +
                                        '<div class="form-check form-switch form-check-inline fs-5 align-middle">' +
                                            '<input class="form-check-input shadow-sm cursor-pointer" type="checkbox" role="switch" id="inputAktif<%=rnd%>" checked>' +
                                            '<label class="form-check-label ms-2 fs-6 fw-bold text-dark mt-1" for="inputAktif<%=rnd%>"><%=Common.getBahasaConfig("Status Aktif")%></label>' +
                                        '</div>' +
                                    '</div>' +
                                '</div>' +
                                
                                '<div class="d-flex justify-content-end border-top pt-4 mt-4">' +
                                    '<button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold border shadow-sm" data-bs-dismiss="modal"><i class="fas fa-times text-danger me-2"></i><%=Common.getBahasaConfig("Batal")%></button>' +
                                    '<button type="submit" class="btn btn-success px-4 rounded-pill shadow-sm fw-bold" id="btnSimpan<%=rnd%>">' +
                                        '<i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Data")%>' +
                                    '</button>' +
                                '</div>' +
                            '</form>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
            document.body.insertAdjacentHTML('beforeend', modalHtml);
        }
    };

    const showLoading<%=rnd%> = () => { 
        const el = document.getElementById('loadingOverlay<%=rnd%>');
        if(el) { el.classList.remove('d-none'); el.classList.add('d-flex'); }
    };
    
    const hideLoading<%=rnd%> = () => { 
        const el = document.getElementById('loadingOverlay<%=rnd%>');
        if(el) { el.classList.remove('d-flex'); el.classList.add('d-none'); }
    };

    const showToast<%=rnd%> = (msg, colorClass) => {
        if(typeof tampilkanToast === "function") {
            tampilkanToast(msg, colorClass);
        } else {
            alert(msg);
        }
    };

    // ==========================================
    // BAGIAN 3: LOGIKA DATA TABEL & PAGING
    // ==========================================
    const buildFilterSQL<%=rnd%> = () => {
        const keyword = document.getElementById('searchReferensi<%=rnd%>').value.trim().replace(/'/g, "''");
        let filters = [];
        
        if (keyword !== '') {
            filters.push("(this_.nama ILIKE '%" + keyword + "%' OR this_.kode ILIKE '%" + keyword + "%')");
        }
        
        const stt = document.getElementById('filterStatus<%=rnd%>').value;
        if (stt === 'TRUE') filters.push("(this_.aktif = true OR this_.aktif IS NULL)");
        else if (stt === 'FALSE') filters.push("this_.aktif = false");

        return filters.length > 0 ? filters.join(' AND ') : '';
    };

    const resetAndLoadData<%=rnd%> = () => {
        currentPage<%=rnd%> = 1; 
        loadDataReferensi<%=rnd%>();
    };

    const loadDataReferensi<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataReferensi<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="6" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Mengambil data...")%></span></td></tr>';
        
        const start = (currentPage<%=rnd%> - 1);
        const whereClause = buildFilterSQL<%=rnd%>();
        
        let reqObj = {
            "action": "daftar", "class": classModel<%=rnd%>, 
            "deep": "1", // === DIUBAH MENJADI 1 SESUAI PERMINTAAN ===
            "max": limitPerPage<%=rnd%>, "halaman": start,
            "order1": "asc", "sort1": "nama", "count": "true"
        };
        if (whereClause) reqObj["where1"] = whereClause;

        try {
            const res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(reqObj) });
            const dataResponse = await res.json();
            
            totalRecords<%=rnd%> = parseInt(dataResponse.count || 0);
            const recordList = dataResponse.data || [];
            let htmlList = '';

            if (recordList.length === 0) {
                htmlList = '<tr><td colspan="6" class="text-center text-muted py-5"><i class="fas fa-folder-open fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Tidak ada data ditemukan.")%></td></tr>';
            } else {
                let no = (start * limitPerPage<%=rnd%>) + 1;
                recordList.forEach((row) => {
                    const safeNama = (row.nama || '-').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    const kode = row.kode || '-';
                    const keterangan = row.keterangan || '-';
                    const statusVal = (row.aktif !== false) ? '<span class="badge bg-success"><i class="fas fa-check-circle me-1"></i><%=Common.getBahasaConfig("Aktif")%></span>' : '<span class="badge bg-danger"><i class="fas fa-times-circle me-1"></i><%=Common.getBahasaConfig("Tidak Aktif")%></span>';

                    let actionBtns = '';
                    if (privEdit<%=rnd%>) {
                        actionBtns += '<button class="btn btn-sm btn-outline-warning text-dark shadow-sm px-2 me-1 fw-bold no-print" onclick="editReferensi<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Ubah Data")%>"><i class="fas fa-edit"></i></button>';
                    }
                    if (privDelete<%=rnd%>) {
                        actionBtns += '<button class="btn btn-sm btn-outline-danger shadow-sm px-2 fw-bold no-print" onclick="hapusReferensi<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Hapus Data")%>"><i class="fas fa-trash-alt"></i></button>';
                    }

                    htmlList += '<tr>' +
                                    '<td class="text-center text-muted fw-medium">' + (no++) + '</td>' +
                                    '<td class="text-start fw-bold text-secondary">' + kode + '</td>' +
                                    '<td class="text-start fw-semibold text-primary">' + safeNama + '</td>' +
                                    '<td class="text-start text-muted small">' + keterangan + '</td>' +
                                    '<td class="text-center align-middle">' + statusVal + '</td>' +
                                    '<td class="text-center text-nowrap no-print">' + actionBtns + '</td>' +
                                '</tr>';
                });
            }
            tbody.innerHTML = htmlList;
            updatePaginationUI<%=rnd%>();
        } catch (error) {
            console.error(error);
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal menarik data. Silakan muat ulang.")%></td></tr>';
        }
    };

    const updatePaginationUI<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumberDisplay<%=rnd%>').innerText = currentPage<%=rnd%> + " / " + totalPages;
        const startIdx = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%> + 1;
        const endIdx = Math.min(currentPage<%=rnd%> * limitPerPage<%=rnd%>, totalRecords<%=rnd%>);
        const startInfo = totalRecords<%=rnd%> > 0 ? startIdx : 0;
        
        document.getElementById('pagingInfoReferensi<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startInfo + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari")%> <b class="text-dark">' + totalRecords<%=rnd%> + '</b> <%=Common.getBahasaConfig("data")%>';
        document.getElementById('btnPrevPage<%=rnd%>').disabled = (currentPage<%=rnd%> === 1);
        document.getElementById('btnNextPage<%=rnd%>').disabled = (currentPage<%=rnd%> === totalPages);
    };

    const changePageReferensi<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if (direction === -1 && currentPage<%=rnd%> > 1) {
            currentPage<%=rnd%>--; loadDataReferensi<%=rnd%>();
        } else if (direction === 1 && currentPage<%=rnd%> < totalPages) {
            currentPage<%=rnd%>++; loadDataReferensi<%=rnd%>();
        }
    };

    // ==========================================
    // BAGIAN 4: FORM CRUD LOGIC (SIMPAN & EDIT)
    // ==========================================
    const bukaFormReferensi<%=rnd%> = () => {
        document.getElementById('formReferensi<%=rnd%>').reset();
        document.getElementById('inputIdReferensi<%=rnd%>').value = '';
        document.getElementById('inputAktif<%=rnd%>').checked = true;

        document.getElementById('formTitleText<%=rnd%>').innerText = '<%=Common.getBahasaConfigJS("Tambah Data Referensi Lulusan")%>';
        
        const modalForm = new bootstrap.Modal(document.getElementById('modalFormReferensi<%=rnd%>'));
        modalForm.show();
    };

    const editReferensi<%=rnd%> = async (id) => {
        showLoading<%=rnd%>();
        try {
            const response = await fetch('<%=Common.ROOT%>/Data', { 
                method: 'POST', headers: { 'Content-Type': 'application/json' }, 
                body: JSON.stringify({ action: "load", class: classModel<%=rnd%>, id: id, 
                deep: "2" // === DIUBAH MENJADI 2 SESUAI PERMINTAAN ===
                }) 
            });
            const result = await response.json();
            
            if (result.status === '00' || result.status === 'success') {
                const data = result.data;
                document.getElementById('inputIdReferensi<%=rnd%>').value = data.id;
                document.getElementById('inputKode<%=rnd%>').value = data.kode || '';
                document.getElementById('inputNama<%=rnd%>').value = data.nama || '';
                document.getElementById('inputKeterangan<%=rnd%>').value = data.keterangan || '';
                document.getElementById('inputAktif<%=rnd%>').checked = (data.aktif !== false);
                
                document.getElementById('formTitleText<%=rnd%>').innerText = '<%=Common.getBahasaConfigJS("Ubah Data Referensi Lulusan")%>';

                const modalForm = new bootstrap.Modal(document.getElementById('modalFormReferensi<%=rnd%>'));
                modalForm.show();
            } else {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal mengambil detail data dari server.")%>', 'bg-danger text-white');
            }
        } catch (error) {
            console.error(error);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi saat membuka form.")%>', 'bg-danger text-white');
        } finally {
            hideLoading<%=rnd%>();
        }
    };

    const simpanReferensi<%=rnd%> = async () => {
        const idData = document.getElementById('inputIdReferensi<%=rnd%>').value;
        const btnSimpan = document.getElementById('btnSimpan<%=rnd%>');

        const dataObj = {
            kode: document.getElementById('inputKode<%=rnd%>').value.trim() || null,
            nama: document.getElementById('inputNama<%=rnd%>').value.trim(),
            keterangan: document.getElementById('inputKeterangan<%=rnd%>').value.trim() || null,
            aktif: document.getElementById('inputAktif<%=rnd%>').checked
        };

        const payload = { action: "simpanDataRinci", log: "true", class: classModel<%=rnd%>, data: dataObj };
        if (idData !== '') payload.id = idData;

        showLoading<%=rnd%>();

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
            const result = await response.json();
            
            if (result.status === '00' || result.status === 'success' || result.id) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data Referensi berhasil disimpan!")%>', 'bg-success text-white');
                bootstrap.Modal.getInstance(document.getElementById('modalFormReferensi<%=rnd%>')).hide();
                loadDataReferensi<%=rnd%>();
            } else {
                showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan data.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            console.error(e);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi.")%>', 'bg-danger text-white');
        } finally {
            hideLoading<%=rnd%>();
        }
    };

    const hapusReferensi<%=rnd%> = async (id) => {
        if (typeof proseshapusDataRinci === 'function') {
            proseshapusDataRinci(classModel<%=rnd%>, id, function(){ 
                 if (document.querySelectorAll('#tabelDataReferensi<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) {
                     currentPage<%=rnd%>--;
                 }
                 loadDataReferensi<%=rnd%>();
            });
        } else {
            if (!confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus data ini?")%>')) return;
            showLoading<%=rnd%>();
            try {
                const res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "hapusDataRinci", class: classModel<%=rnd%>, id: id }) });
                const r = await res.json();
                if(r.status === '00' || r.status === 'success') {
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data berhasil dihapus.")%>', 'bg-success text-white');
                    loadDataReferensi<%=rnd%>();
                } else {
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal menghapus data.")%>', 'bg-danger text-white');
                }
            } catch(e) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Kesalahan koneksi.")%>', 'bg-danger text-white');
            } finally {
                hideLoading<%=rnd%>();
            }
        }
    };

    // ==========================================
    // BAGIAN 5: EKSPOR, IMPOR, & CETAK PDF
    // ==========================================
    const downloadExcel<%=rnd%> = () => {
        try {
            const table = document.getElementById('tabelHTMLDataReferensi<%=rnd%>');
            const cloneTable = table.cloneNode(true);
            const elementsToRemove = cloneTable.querySelectorAll('.no-print');
            elementsToRemove.forEach(el => el.parentNode.removeChild(el));

            if(typeof XLSX !== 'undefined') {
                const wb = XLSX.utils.table_to_book(cloneTable, {sheet: "Referensi_Lulusan"});
                XLSX.writeFile(wb, "Data_Referensi_Lulusan.xlsx");
            } else {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Library Excel tidak dimuat. Gagal mengunduh.")%>', 'bg-warning text-dark');
            }
        } catch(e) { console.error(e); }
    };

    const uploadExcel<%=rnd%> = async (input) => {
        if(input.files.length === 0) return;
        const file = input.files[0];
        const formData = new FormData();
        formData.append('fileExcel', file);
        formData.append('class', classModel<%=rnd%>);
        
        showLoading<%=rnd%>();
        try {
            const res = await fetch('<%=Common.ROOT%>/UploadExcel', { method: 'POST', body: formData });
            if(res.ok) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data Excel berhasil diimpor!")%>', 'bg-success text-white');
                resetAndLoadData<%=rnd%>();
            } else {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal memproses file Excel pada Server.")%>', 'bg-danger text-white');
            }
        } catch(e) {
            console.error(e);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan saat mengunggah file.")%>', 'bg-danger text-white');
        } finally {
            input.value = '';
            hideLoading<%=rnd%>();
        }
    };

    const cetakPDF<%=rnd%> = () => {
        window.print();
    };

    // ==========================================
    // INITIALIZATION DOM READY
    // ==========================================
    document.addEventListener("DOMContentLoaded", async () => {
        injectDOMElements<%=rnd%>();
        showLoading<%=rnd%>();

        document.getElementById('searchReferensi<%=rnd%>').addEventListener("keydown", (e) => {
            if (e.key === "Enter") { e.preventDefault(); resetAndLoadData<%=rnd%>(); }
        });

        // Langsung muat data tabel
        await loadDataReferensi<%=rnd%>();
        
        hideLoading<%=rnd%>();
    });
</script>