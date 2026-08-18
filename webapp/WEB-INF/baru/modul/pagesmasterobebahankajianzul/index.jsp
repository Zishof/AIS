<%@page import="java.util.List"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.database.model.obe.BahanKajian"%>
<%@page import="ais.database.model.obe.ReferensiLulusan"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Fakultas"%>
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

// 3. FAKULTAS & JURUSAN LOCK LOGIC
Fakultas currentFakultas = null;
Jurusan currentJurusan = null;
try {
    currentFakultas = tbmuser.getFakultas(); 
    currentJurusan = tbmuser.getJurusan();
} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmasterobebahankajianzul/index.jsp:51");}

Long idFakLogin = null;
Long idJurLogin = null;

if (currentJurusan != null) {
    idJurLogin = currentJurusan.getId();
    if (currentJurusan.getFakultas() != null) {
        idFakLogin = currentJurusan.getFakultas().getId();
    }
} else if (currentFakultas != null) {
    idFakLogin = currentFakultas.getId();
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
    
    /* Kotak Checkbox dengan tinggi otomatis mengikuti isi data */
    .checkbox-container-auto {
        border: 1px solid #dee2e6;
        border-radius: 0.375rem;
        background-color: #f8f9fa;
        padding: 0.75rem;
        height: auto;
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
                                <i class="fas fa-book-open text-primary me-2"></i><%=Common.getBahasaConfig("Manajemen Bahan Kajian")%>
                            </h5>
                            <small class="text-muted"><%=Common.getBahasaConfig("Kelola data bahan kajian kurikulum pada Program Studi.")%></small>
                        </div>
                        
                        <div class="d-flex align-items-center gap-2 flex-wrap">
                            <div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;">
                                <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>
                                <input type="text" class="form-control border-start-0 ps-0 bg-white fw-medium" id="searchBahanKajian<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Kode / Nama...")%>">
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
                            <button class="btn btn-sm btn-primary rounded-pill px-4 shadow-sm fw-bold text-nowrap" onclick="bukaFormBahanKajian<%=rnd%>()">
                                <i class="fas fa-plus-circle me-1"></i><%=Common.getBahasaConfig("Tambah Data")%>
                            </button>
                            <% } %>
                        </div>
                    </div>

                    <div class="collapse mt-3" id="filterPanel<%=rnd%>">
                        <div class="card card-body bg-light border-0 shadow-sm rounded-3 p-3">
                            <h6 class="fw-bold text-secondary mb-3"><i class="fas fa-sliders-h me-2"></i><%=Common.getBahasaConfig("Penyaringan Lanjutan")%></h6>
                            <div class="row g-3">
                                <div class="col-md-3">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Fakultas")%></label>
                                    <select id="filterFakultas<%=rnd%>" class="form-select form-select-sm shadow-sm" onchange="loadJurusanUI<%=rnd%>(this.value, 'filterJurusan<%=rnd%>')">
                                        <option value=""><%=Common.getBahasaConfig("-- Semua Fakultas --")%></option>
                                    </select>
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Program Studi (Jurusan)")%></label>
                                    <select id="filterJurusan<%=rnd%>" class="form-select form-select-sm shadow-sm">
                                        <option value=""><%=Common.getBahasaConfig("-- Semua Program Studi --")%></option>
                                    </select>
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Khusus Buat MK")%></label>
                                    <input type="text" class="form-control form-control-sm shadow-sm" id="filterMk<%=rnd%>" list="dlFilterMk<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik Kode / Nama Matakuliah...")%>" oninput="onInputMatakuliah<%=rnd%>(this.value, true)" onchange="checkSelectedMatakuliah<%=rnd%>(this.value, true)" autocomplete="off">
                                    <datalist id="dlFilterMk<%=rnd%>"></datalist>
                                    <input type="hidden" id="filterHiddenMk<%=rnd%>">
                                </div>
                                <div class="col-md-2">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Status Aktif")%></label>
                                    <select id="filterStatus<%=rnd%>" class="form-select form-select-sm shadow-sm">
                                        <option value="ALL"><%=Common.getBahasaConfig("-- Semua --")%></option>
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
                        <table class="table table-hover table-striped align-middle mb-0" id="tabelHTMLDataBahanKajian<%=rnd%>">
                            <thead class="table-dark text-center align-middle">
                                <tr>
                                    <th class="fw-semibold text-uppercase small px-3 py-3" style="width: 50px;">#</th>
                                    <th class="text-start fw-semibold text-uppercase small px-3" style="width: 100px;"><i class="fas fa-barcode me-1"></i><%=Common.getBahasaConfig("Kode")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-book-open me-1"></i><%=Common.getBahasaConfig("Nama Bahan Kajian")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-link me-1"></i><%=Common.getBahasaConfig("Referensi")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-book me-1"></i><%=Common.getBahasaConfig("Khusus MK")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-university me-1"></i><%=Common.getBahasaConfig("Fakultas / Program Studi")%></th>
                                    <th class="text-center fw-semibold text-uppercase small px-3"><i class="fas fa-toggle-on me-1"></i><%=Common.getBahasaConfig("Status")%></th>
                                    <th class="fw-semibold text-uppercase small px-3 text-center no-print" style="width: 100px;"><i class="fas fa-cogs"></i></th>
                                </tr>
                            </thead>
                            <tbody id="tabelDataBahanKajian<%=rnd%>">
                                <tr><td colspan="8" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Memuat data...")%></span></td></tr>
                            </tbody>
                        </table>
                    </div>

                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mt-4 no-print">
                        <div class="small fw-medium text-muted bg-light px-3 py-1 rounded-pill border" id="pagingInfoBahanKajian<%=rnd%>"></div>
                        <nav>
                            <ul class="pagination pagination-sm mb-0 shadow-sm">
                                <li class="page-item"><button class="page-link text-primary rounded-start-pill px-3 fw-bold" id="btnPrevPage<%=rnd%>" onclick="changePageBahanKajian<%=rnd%>(-1)"><i class="fas fa-chevron-left"></i></button></li>
                                <li class="page-item disabled"><span class="page-link bg-light text-dark px-3 fw-bold" id="pageNumberDisplay<%=rnd%>">1</span></li>
                                <li class="page-item"><button class="page-link text-primary rounded-end-pill px-3 fw-bold" id="btnNextPage<%=rnd%>" onclick="changePageBahanKajian<%=rnd%>(1)"><i class="fas fa-chevron-right"></i></button></li>
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
    const classModel<%=rnd%> = "<%=BahanKajian.class.getName()%>";
    const classReferensi<%=rnd%> = "<%=ReferensiLulusan.class.getName()%>";
    const classMatakuliah<%=rnd%> = "<%=Matakuliah.class.getName()%>";
    const classJurusan<%=rnd%> = "<%=Jurusan.class.getName()%>";
    const classFakultas<%=rnd%> = "<%=Fakultas.class.getName()%>";
    
    let currentPage<%=rnd%> = 1;
    const limitPerPage<%=rnd%> = 10;
    let totalRecords<%=rnd%> = 0;
    
    let mapReferensi<%=rnd%> = {}; // Menyimpan dictionary ID -> Nama Referensi

    const idFakultasLogin<%=rnd%> = <%= idFakLogin != null ? idFakLogin : "null" %>;
    const idJurusanLogin<%=rnd%> = <%= idJurLogin != null ? idJurLogin : "null" %>;
    
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

        if(!document.getElementById('modalFormBahanKajian<%=rnd%>')) {
            var modalHtml = 
            '<div class="modal fade" id="modalFormBahanKajian<%=rnd%>" tabindex="-1" aria-labelledby="modalLabel<%=rnd%>" aria-hidden="true" data-bs-backdrop="static">' +
                '<div class="modal-dialog modal-dialog-centered modal-lg modal-dialog-scrollable">' +
                    '<div class="modal-content border-0 shadow-lg rounded-4">' +
                        '<div class="modal-header border-bottom-0 pb-0 pt-4 px-4">' +
                            '<h5 class="modal-title fw-bold text-dark" id="modalLabel<%=rnd%>">' +
                                '<i class="fas fa-edit text-primary me-2"></i><span id="formTitleText<%=rnd%>"><%=Common.getBahasaConfig("Formulir Bahan Kajian")%></span>' +
                            '</h5>' +
                            '<button type="button" class="btn-close shadow-sm" data-bs-dismiss="modal" aria-label="Close"></button>' +
                        '</div>' +
                        '<div class="modal-body p-4">' +
                            '<form id="formBahanKajian<%=rnd%>" onsubmit="event.preventDefault(); simpanBahanKajian<%=rnd%>();">' +
                                '<input type="hidden" id="inputIdBahanKajian<%=rnd%>">' +
                                
                                '<div class="row g-3">' +
                                    '<div class="col-md-3">' +
                                        '<label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Kode")%></label>' +
                                        '<input type="text" class="form-control shadow-sm fw-bold" id="inputKode<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cth: BK-01")%>">' +
                                    '</div>' +
                                    '<div class="col-md-9">' +
                                        '<label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Bahan Kajian")%> <span class="text-danger">*</span></label>' +
                                        '<input type="text" class="form-control shadow-sm fw-bold" id="inputNama<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: Dasar-Dasar Algoritma...")%>" required>' +
                                    '</div>' +

                                    '<div class="col-md-6">' +
                                        '<label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Fakultas")%></label>' +
                                        '<select class="form-select shadow-sm" id="selectFakultas<%=rnd%>" onchange="loadJurusanUI<%=rnd%>(this.value, \'selectJurusan<%=rnd%>\')">' +
                                            '<option value=""><%=Common.getBahasaConfig("-- Pilih Fakultas --")%></option>' +
                                        '</select>' +
                                    '</div>' +
                                    '<div class="col-md-6">' +
                                        '<label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Program Studi (Jurusan)")%> <span class="text-danger">*</span></label>' +
                                        '<select class="form-select shadow-sm" id="selectJurusan<%=rnd%>" required>' +
                                            '<option value=""><%=Common.getBahasaConfig("-- Pilih Program Studi --")%></option>' +
                                        '</select>' +
                                    '</div>' +

                                    '' +
                                    '<div class="col-md-12">' +
                                        '<label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Khusus Buat Matakuliah (Opsional)")%></label>' +
                                        '<input type="text" class="form-control shadow-sm border-info fw-semibold" id="inputMk<%=rnd%>" list="dlFormMk<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik Kode atau Nama MK untuk mencari...")%>" oninput="onInputMatakuliah<%=rnd%>(this.value, false)" onchange="checkSelectedMatakuliah<%=rnd%>(this.value, false)" autocomplete="off">' +
                                        '<datalist id="dlFormMk<%=rnd%>"></datalist>' +
                                        '<input type="hidden" id="hiddenIdMk<%=rnd%>">' +
                                    '</div>' +

                                    '' +
                                    '<div class="col-md-12 mt-4">' +
                                        '<div class="d-flex justify-content-between align-items-end mb-2">' +
                                            '<label class="form-label small fw-bold text-secondary m-0"><i class="fas fa-link text-info me-1"></i><%=Common.getBahasaConfig("Korelasi Referensi Lulusan")%></label>' +
                                            '<div class="input-group input-group-sm w-auto">' +
                                                '<span class="input-group-text bg-white text-muted border-end-0"><i class="fas fa-search"></i></span>' +
                                                '<input type="text" class="form-control form-control-sm border-start-0 shadow-sm" id="searchReferensiForm<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Referensi...")%>" style="width: 200px;" onkeyup="filterReferensi<%=rnd%>(this.value)">' +
                                            '</div>' +
                                        '</div>' +
                                        '<div id="containerReferensi<%=rnd%>" class="checkbox-container-auto row g-2 m-0 shadow-sm">' +
                                            '<div class="text-center py-2"><div class="spinner-border spinner-border-sm text-primary"></div></div>' +
                                        '</div>' +
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
    // BAGIAN 3: LOAD DROP DOWN & DATALIST
    // ==========================================
    const loadMasterReferensi<%=rnd%> = async () => {
        let reqObj = { 
            "action": "daftar", "class": classReferensi<%=rnd%>, 
            "where1": "(aktif = true or aktif is null)", "order1": "asc", "sort1": "nama" 
        };
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(reqObj) });
            const result = await res.json();
            const data = result.data || [];
            
            mapReferensi<%=rnd%> = {};
            data.forEach(r => { mapReferensi<%=rnd%>[r.id] = (r.kode ? r.kode + ' - ' : '') + r.nama; });
        } catch(e) { console.error("Error load map referensi", e); }
    };

    const loadFakultasUI<%=rnd%> = async (targetId, selectedId) => {
        let reqObj = { 
            "action": "daftar", "class": classFakultas<%=rnd%>, 
            "where1": "(aktif = true or aktif is null)", "order1": "asc", "sort1": "nama" 
        };
        if (idFakultasLogin<%=rnd%> !== null) reqObj.where1 += " AND id = " + idFakultasLogin<%=rnd%>;

        try {
            const res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(reqObj) });
            const result = await res.json();
            const data = result.data || [];
            
            let targetEl = document.getElementById(targetId);
            let options = '<option value="">' + (targetId.includes('filter') ? '<%=Common.getBahasaConfigJS("-- Semua Fakultas --")%>' : '<%=Common.getBahasaConfigJS("-- Pilih Fakultas --")%>') + '</option>';
            
            if (data.length === 1 || idFakultasLogin<%=rnd%> !== null) {
                options = ''; targetEl.disabled = true;
            } else {
                targetEl.disabled = false;
            }

            data.forEach((row) => {
                const selected = (selectedId && selectedId == row.id) || (data.length === 1) ? "selected" : "";
                options += '<option value="' + row.id + '" ' + selected + '>' + row.nama + '</option>';
            });
            targetEl.innerHTML = options;
        } catch (e) { console.error("Error load Fakultas", e); }
    };

    const loadJurusanUI<%=rnd%> = async (idFak, targetId, selectedId) => {
        let reqObj = { 
            "action": "daftar", "class": classJurusan<%=rnd%>, 
            "where1": "(aktif = true or aktif is null)", "order1": "asc", "sort1": "nama" 
        };

        let filterWhere = [];
        if (idJurusanLogin<%=rnd%> !== null) filterWhere.push("id = " + idJurusanLogin<%=rnd%>);
        else {
            if (idFak) filterWhere.push("fakultas = " + idFak);
            else if (idFakultasLogin<%=rnd%> !== null) filterWhere.push("fakultas = " + idFakultasLogin<%=rnd%>);
        }
        if (filterWhere.length > 0) reqObj.where1 += " AND " + filterWhere.join(' AND ');

        try {
            const res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(reqObj) });
            const result = await res.json();
            const data = result.data || [];

            let targetEl = document.getElementById(targetId);
            let options = '<option value="">' + (targetId.includes('filter') ? '<%=Common.getBahasaConfigJS("-- Semua Program Studi --")%>' : '<%=Common.getBahasaConfigJS("-- Pilih Program Studi --")%>') + '</option>';
            
            if (data.length === 1 || idJurusanLogin<%=rnd%> !== null) {
                options = ''; targetEl.disabled = true;
            } else {
                targetEl.disabled = false;
            }

            data.forEach((row) => {
                const selected = (selectedId && selectedId == row.id) || (data.length === 1) ? "selected" : "";
                options += '<option value="' + row.id + '" ' + selected + '>' + row.nama + '</option>';
            });
            targetEl.innerHTML = options;
        } catch (e) { console.error("Error load Jurusan", e); }
    };

    // Datlist Matakuliah
    const onInputMatakuliah<%=rnd%> = async (val, isFilter) => {
        if (val.length < 2) return;
        const datalistId = isFilter ? 'dlFilterMk<%=rnd%>' : 'dlFormMk<%=rnd%>';
        var reqObj = { 
            "action": "daftar", "class": classMatakuliah<%=rnd%>, 
            "where1": "aktif = true AND (kode ILIKE '%" + val + "%' OR nama ILIKE '%" + val + "%')",
            "max": 25
        };
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(reqObj) });
            const result = await res.json();
            let options = '';
            (result.data || []).forEach(row => {
                options += '<option data-id="' + row.id + '" value="' + row.kode + ' - ' + row.nama + '"></option>';
            });
            document.getElementById(datalistId).innerHTML = options;
        } catch (e) { console.error(e); }
    };

    const checkSelectedMatakuliah<%=rnd%> = (val, isFilter) => {
        const datalistId = isFilter ? 'dlFilterMk<%=rnd%>' : 'dlFormMk<%=rnd%>';
        const hiddenId = isFilter ? 'filterHiddenMk<%=rnd%>' : 'hiddenIdMk<%=rnd%>';
        const dl = document.getElementById(datalistId);
        for(let i=0; i < dl.options.length; i++) {
            if(dl.options[i].value === val) {
                document.getElementById(hiddenId).value = dl.options[i].getAttribute('data-id');
                return;
            }
        }
        document.getElementById(hiddenId).value = '';
    };

    // ==========================================
    // BAGIAN 4: LOAD DYNAMIC CHECKBOXES & FILTERING
    // ==========================================
    const loadCheckboxesReferensi<%=rnd%> = async (selectedStr) => {
        let targetEl = document.getElementById('containerReferensi<%=rnd%>');
        targetEl.innerHTML = '<div class="text-center py-2"><div class="spinner-border spinner-border-sm text-primary"></div></div>';
        
        let reqObj = { 
            "action": "daftar", "class": classReferensi<%=rnd%>, 
            "where1": "(aktif = true or aktif is null)", "order1": "asc", "sort1": "nama" 
        };

        try {
            const res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(reqObj) });
            const result = await res.json();
            const data = result.data || [];

            if (data.length === 0) {
                targetEl.innerHTML = '<div class="text-danger small fst-italic text-center py-2"><i class="fas fa-exclamation-triangle me-1"></i><%=Common.getBahasaConfig("Belum ada data Referensi Lulusan yang tersedia.")%></div>';
                return;
            }

            let html = '';
            data.forEach((row) => {
                let isChecked = false;
                if(selectedStr) {
                    let checkVal = "," + row.id + ",";
                    if(selectedStr.indexOf(checkVal) !== -1 || selectedStr === row.id.toString()) isChecked = true;
                }
                
                let labelText = (row.kode ? '<b>' + row.kode + '</b> - ' : '') + row.nama;
                // Tambahkan class 'referensi-item' untuk kebutuhan filtering js
                html += '<div class="col-md-6 referensi-item<%=rnd%>">' +
                        '  <div class="form-check">' +
                        '    <input class="form-check-input chk-referensi<%=rnd%> shadow-sm" type="checkbox" value="' + row.id + '" id="chkRef_' + row.id + '<%=rnd%>" ' + (isChecked ? 'checked' : '') + '>' +
                        '    <label class="form-check-label text-dark small cursor-pointer" for="chkRef_' + row.id + '<%=rnd%>">' + labelText + '</label>' +
                        '  </div>' +
                        '</div>';
            });
            targetEl.innerHTML = html;
        } catch (e) { console.error(e); targetEl.innerHTML = '<div class="text-danger small py-2"><%=Common.getBahasaConfig("Gagal memuat data.")%></div>'; }
    };

    // Client-side Javascript Filtering untuk checkbox
    const filterReferensi<%=rnd%> = (val) => {
        const filter = val.toLowerCase();
        const container = document.getElementById('containerReferensi<%=rnd%>');
        const items = container.getElementsByClassName('referensi-item<%=rnd%>');

        for (let i = 0; i < items.length; i++) {
            const label = items[i].getElementsByTagName('label')[0];
            const textValue = label.textContent || label.innerText;
            if (textValue.toLowerCase().indexOf(filter) > -1) {
                items[i].style.display = "";
            } else {
                items[i].style.display = "none";
            }
        }
    };

    // ==========================================
    // BAGIAN 5: LOGIKA DATA TABEL & PAGING
    // ==========================================
    const buildFilterSQL<%=rnd%> = () => {
        const keyword = document.getElementById('searchBahanKajian<%=rnd%>').value.trim().replace(/'/g, "''");
        let filters = [];
        
        if (keyword !== '') {
            filters.push("(this_.nama ILIKE '%" + keyword + "%' OR this_.kode ILIKE '%" + keyword + "%')");
        }
        
        const fFak = document.getElementById('filterFakultas<%=rnd%>').value;
        const fJur = document.getElementById('filterJurusan<%=rnd%>').value;
        const fMk = document.getElementById('filterHiddenMk<%=rnd%>').value;
        
        if (idJurusanLogin<%=rnd%> !== null) {
            filters.push("this_.jurusan = " + idJurusanLogin<%=rnd%>);
        } else if (fJur) {
            filters.push("this_.jurusan = " + fJur);
        } else if (idFakultasLogin<%=rnd%> !== null) {
            filters.push("jurusan1_.fakultas = " + idFakultasLogin<%=rnd%>);
        } else if (fFak) {
            filters.push("jurusan1_.fakultas = " + fFak);
        }

        if (fMk) {
            filters.push("this_.khususBuatMk = " + fMk);
        }
        
        const stt = document.getElementById('filterStatus<%=rnd%>').value;
        if (stt === 'TRUE') filters.push("(this_.aktif = true OR this_.aktif IS NULL)");
        else if (stt === 'FALSE') filters.push("this_.aktif = false");

        return filters.length > 0 ? filters.join(' AND ') : '';
    };

    const resetAndLoadData<%=rnd%> = () => {
        currentPage<%=rnd%> = 1; 
        loadDataBahanKajian<%=rnd%>();
    };

    const loadDataBahanKajian<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataBahanKajian<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="8" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Mengambil data...")%></span></td></tr>';
        
        const start = (currentPage<%=rnd%> - 1);
        const whereClause = buildFilterSQL<%=rnd%>();
        
        let reqObj = {
            "action": "daftar", "class": classModel<%=rnd%>, "deep": "2", 
            "max": limitPerPage<%=rnd%>, "halaman": start,
            "order1": "asc", "sort1": "nama", "count": "true",
            "alias1": "jurusan", "alias2": "jurusan.fakultas", "alias3": "khususBuatMk"
        };
        if (whereClause) reqObj["where1"] = whereClause;

        try {
            const res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(reqObj) });
            const dataResponse = await res.json();
            
            totalRecords<%=rnd%> = parseInt(dataResponse.count || 0);
            const recordList = dataResponse.data || [];
            let htmlList = '';

            if (recordList.length === 0) {
                htmlList = '<tr><td colspan="8" class="text-center text-muted py-5"><i class="fas fa-folder-open fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Tidak ada data ditemukan.")%></td></tr>';
            } else {
                let no = (start * limitPerPage<%=rnd%>) + 1;
                recordList.forEach((row) => {
                    const safeNama = (row.nama || '-').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    const kode = row.kode || '-';
                    const statusVal = (row.aktif !== false) ? '<span class="badge bg-success"><i class="fas fa-check-circle me-1"></i><%=Common.getBahasaConfig("Aktif")%></span>' : '<span class="badge bg-danger"><i class="fas fa-times-circle me-1"></i><%=Common.getBahasaConfig("Tidak Aktif")%></span>';
                    
                    const jurNama = row["jurusan.nama"] || '-';
                    const fakNama = row["jurusan.fakultas.nama"] || '-';
                    const pTinggi = '<div class="fw-bold text-dark fs-6">' + jurNama + '</div><small class="text-muted"><i class="fas fa-building me-1"></i>' + fakNama + '</small>';
                    
                    const namaMk = row["khususBuatMk.nama"] || '-';
                    const detailMk = namaMk !== '-' ? '<span class="text-info fw-medium">' + namaMk + '</span>' : '<span class="text-muted small">-</span>';

                    // Translasi ID Referensi ke Text (Menggunakan Map dari initial load)
                    let refHtml = '<span class="text-muted small fst-italic">-</span>';
                    if(row.referensi) {
                        let ids = row.referensi.split(',').filter(x => x.trim() !== '');
                        if(ids.length > 0) {
                            refHtml = '<ul class="text-start mb-0 ps-3 small text-secondary">';
                            ids.forEach(id => {
                                if(mapReferensi<%=rnd%>[id]) {
                                    refHtml += '<li>' + mapReferensi<%=rnd%>[id] + '</li>';
                                }
                            });
                            refHtml += '</ul>';
                        }
                    }

                    let actionBtns = '';
                    if (privEdit<%=rnd%>) {
                        actionBtns += '<button class="btn btn-sm btn-outline-warning text-dark shadow-sm px-2 me-1 fw-bold no-print" onclick="editBahanKajian<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Ubah Data")%>"><i class="fas fa-edit"></i></button>';
                    }
                    if (privDelete<%=rnd%>) {
                        actionBtns += '<button class="btn btn-sm btn-outline-danger shadow-sm px-2 fw-bold no-print" onclick="hapusBahanKajian<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Hapus Data")%>"><i class="fas fa-trash-alt"></i></button>';
                    }

                    htmlList += '<tr>' +
                                    '<td class="text-center text-muted fw-medium">' + (no++) + '</td>' +
                                    '<td class="text-start fw-bold text-secondary">' + kode + '</td>' +
                                    '<td class="text-start fw-semibold text-primary">' + safeNama + '</td>' +
                                    '<td class="text-start">' + refHtml + '</td>' +
                                    '<td class="text-start">' + detailMk + '</td>' +
                                    '<td class="text-start">' + pTinggi + '</td>' +
                                    '<td class="text-center align-middle">' + statusVal + '</td>' +
                                    '<td class="text-center text-nowrap no-print">' + actionBtns + '</td>' +
                                '</tr>';
                });
            }
            tbody.innerHTML = htmlList;
            updatePaginationUI<%=rnd%>();
        } catch (error) {
            console.error(error);
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal menarik data. Silakan muat ulang.")%></td></tr>';
        }
    };

    const updatePaginationUI<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumberDisplay<%=rnd%>').innerText = currentPage<%=rnd%> + " / " + totalPages;
        const startIdx = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%> + 1;
        const endIdx = Math.min(currentPage<%=rnd%> * limitPerPage<%=rnd%>, totalRecords<%=rnd%>);
        const startInfo = totalRecords<%=rnd%> > 0 ? startIdx : 0;
        
        document.getElementById('pagingInfoBahanKajian<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startInfo + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari")%> <b class="text-dark">' + totalRecords<%=rnd%> + '</b> <%=Common.getBahasaConfig("data")%>';
        document.getElementById('btnPrevPage<%=rnd%>').disabled = (currentPage<%=rnd%> === 1);
        document.getElementById('btnNextPage<%=rnd%>').disabled = (currentPage<%=rnd%> === totalPages);
    };

    const changePageBahanKajian<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if (direction === -1 && currentPage<%=rnd%> > 1) {
            currentPage<%=rnd%>--; loadDataBahanKajian<%=rnd%>();
        } else if (direction === 1 && currentPage<%=rnd%> < totalPages) {
            currentPage<%=rnd%>++; loadDataBahanKajian<%=rnd%>();
        }
    };

    // ==========================================
    // BAGIAN 6: FORM CRUD LOGIC (SIMPAN & EDIT)
    // ==========================================
    const bukaFormBahanKajian<%=rnd%> = () => {
        document.getElementById('formBahanKajian<%=rnd%>').reset();
        document.getElementById('inputIdBahanKajian<%=rnd%>').value = '';
        document.getElementById('inputAktif<%=rnd%>').checked = true;
        
        // Reset Kolom Search MK dan Referensi
        document.getElementById('inputMk<%=rnd%>').value = '';
        document.getElementById('hiddenIdMk<%=rnd%>').value = '';
        document.getElementById('searchReferensiForm<%=rnd%>').value = '';

        document.getElementById('formTitleText<%=rnd%>').innerText = '<%=Common.getBahasaConfigJS("Tambah Data Bahan Kajian")%>';
        
        loadFakultasUI<%=rnd%>('selectFakultas<%=rnd%>', idFakultasLogin<%=rnd%>);
        loadJurusanUI<%=rnd%>(idFakultasLogin<%=rnd%>, 'selectJurusan<%=rnd%>', idJurusanLogin<%=rnd%>);
        loadCheckboxesReferensi<%=rnd%>(null); // Load checkbox data

        const modalForm = new bootstrap.Modal(document.getElementById('modalFormBahanKajian<%=rnd%>'));
        modalForm.show();
    };

    const editBahanKajian<%=rnd%> = async (id) => {
        showLoading<%=rnd%>();
        try {
            const response = await fetch('<%=Common.ROOT%>/Data', { 
                method: 'POST', headers: { 'Content-Type': 'application/json' }, 
                body: JSON.stringify({ action: "load", class: classModel<%=rnd%>, id: id, deep: "2" }) 
            });
            const result = await response.json();
            
            if (result.status === '00' || result.status === 'success') {
                const data = result.data;
                document.getElementById('inputIdBahanKajian<%=rnd%>').value = data.id;
                document.getElementById('inputKode<%=rnd%>').value = data.kode || '';
                document.getElementById('inputNama<%=rnd%>').value = data.nama || '';
                document.getElementById('inputKeterangan<%=rnd%>').value = data.keterangan || '';
                document.getElementById('inputAktif<%=rnd%>').checked = (data.aktif !== false);
                document.getElementById('searchReferensiForm<%=rnd%>').value = ''; // Reset Searchbox filter
                
                // Set data Matakuliah jika ada
                if (data["khususBuatMk.id"]) {
                    document.getElementById('hiddenIdMk<%=rnd%>').value = data["khususBuatMk.id"];
                    document.getElementById('inputMk<%=rnd%>').value = (data["khususBuatMk.kode"] || '') + ' - ' + (data["khususBuatMk.nama"] || '');
                } else {
                    document.getElementById('hiddenIdMk<%=rnd%>').value = '';
                    document.getElementById('inputMk<%=rnd%>').value = '';
                }

                document.getElementById('formTitleText<%=rnd%>').innerText = '<%=Common.getBahasaConfigJS("Ubah Data Bahan Kajian")%>';

                let idFak = data["jurusan.fakultas.id"];
                let idJur = data["jurusan.id"];
                let valReferensi = data.referensi;

                await loadFakultasUI<%=rnd%>('selectFakultas<%=rnd%>', idFak);
                await loadJurusanUI<%=rnd%>(idFak, 'selectJurusan<%=rnd%>', idJur);
                
                // Set Checkbox Referensi pre-selected
                await loadCheckboxesReferensi<%=rnd%>(valReferensi);

                const modalForm = new bootstrap.Modal(document.getElementById('modalFormBahanKajian<%=rnd%>'));
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

    const simpanBahanKajian<%=rnd%> = async () => {
        const idData = document.getElementById('inputIdBahanKajian<%=rnd%>').value;
        const jurVal = document.getElementById('selectJurusan<%=rnd%>').value;
        const mkVal = document.getElementById('hiddenIdMk<%=rnd%>').value;
        const btnSimpan = document.getElementById('btnSimpan<%=rnd%>');

        if (!jurVal) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Program Studi (Jurusan) wajib dipilih.")%>', 'bg-warning text-dark');
            return;
        }

        // Logic pengumpulan selected checkboxes (Referensi Lulusan)
        let arrRef = [];
        document.querySelectorAll('.chk-referensi<%=rnd%>:checked').forEach(chk => { arrRef.push(chk.value); });
        let valRef = arrRef.length > 0 ? "," + arrRef.join(",") + "," : null; // format DB sesuai model

        const dataObj = {
            kode: document.getElementById('inputKode<%=rnd%>').value.trim() || null,
            nama: document.getElementById('inputNama<%=rnd%>').value.trim(),
            keterangan: document.getElementById('inputKeterangan<%=rnd%>').value.trim() || null,
            aktif: document.getElementById('inputAktif<%=rnd%>').checked,
            jurusan: parseInt(jurVal),
            khususBuatMk: mkVal ? parseInt(mkVal) : null,
            referensi: valRef
        };

        const payload = { action: "simpanDataRinci", log: "true", class: classModel<%=rnd%>, data: dataObj };
        if (idData !== '') payload.id = idData;

        showLoading<%=rnd%>();

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
            const result = await response.json();
            
            if (result.status === '00' || result.status === 'success' || result.id) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data Bahan Kajian berhasil disimpan!")%>', 'bg-success text-white');
                bootstrap.Modal.getInstance(document.getElementById('modalFormBahanKajian<%=rnd%>')).hide();
                
                // Clear filter MK setelah save
                document.getElementById('filterMk<%=rnd%>').value = '';
                document.getElementById('filterHiddenMk<%=rnd%>').value = '';
                
                loadDataBahanKajian<%=rnd%>();
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

    const hapusBahanKajian<%=rnd%> = async (id) => {
        if (typeof proseshapusDataRinci === 'function') {
            proseshapusDataRinci(classModel<%=rnd%>, id, function(){ 
                 if (document.querySelectorAll('#tabelDataBahanKajian<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) {
                     currentPage<%=rnd%>--;
                 }
                 loadDataBahanKajian<%=rnd%>();
            });
        } else {
            if (!confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus data ini?")%>')) return;
            showLoading<%=rnd%>();
            try {
                const res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "hapusDataRinci", class: classModel<%=rnd%>, id: id }) });
                const r = await res.json();
                if(r.status === '00' || r.status === 'success') {
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data berhasil dihapus.")%>', 'bg-success text-white');
                    loadDataBahanKajian<%=rnd%>();
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
    // BAGIAN 7: EKSPOR, IMPOR, & CETAK PDF
    // ==========================================
    const downloadExcel<%=rnd%> = () => {
        try {
            const table = document.getElementById('tabelHTMLDataBahanKajian<%=rnd%>');
            const cloneTable = table.cloneNode(true);
            const elementsToRemove = cloneTable.querySelectorAll('.no-print');
            elementsToRemove.forEach(el => el.parentNode.removeChild(el));

            if(typeof XLSX !== 'undefined') {
                const wb = XLSX.utils.table_to_book(cloneTable, {sheet: "Bahan_Kajian"});
                XLSX.writeFile(wb, "Data_Bahan_Kajian.xlsx");
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

        document.getElementById('searchBahanKajian<%=rnd%>').addEventListener("keydown", (e) => {
            if (e.key === "Enter") { e.preventDefault(); resetAndLoadData<%=rnd%>(); }
        });

        // 1. Tarik Data Master Referensi Global Dahulu untuk translasi table
        await loadMasterReferensi<%=rnd%>();
        
        // 2. Tarik Fakultas & Jurusan UI
        await loadFakultasUI<%=rnd%>('filterFakultas<%=rnd%>', idFakultasLogin<%=rnd%>);
        await loadJurusanUI<%=rnd%>(idFakultasLogin<%=rnd%>, 'filterJurusan<%=rnd%>', idJurusanLogin<%=rnd%>);
        
        // 3. Tarik Data Tabel Utama
        await loadDataBahanKajian<%=rnd%>();
        
        hideLoading<%=rnd%>();
    });
</script>