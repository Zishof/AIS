<%@page import="ais.common.CommonPrivilages"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="ais.database.model.OrganisasiIntraKampus"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// 1. SECURITY CHECK & CONTEXT LOGIN
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	out.print("{\"status\":\"error\", \"message\":\""
	+ Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
	return;
}
String rnd = Common.getGeneratedBarCode(7);

// Mengambil konteks Fakultas dan Jurusan untuk otomatisasi
Fakultas loginSebagaiFakultas = tbmuser.getFakultas();
Jurusan loginSebagaiJurusan = tbmuser.getJurusan();

Long idFakLogin = (loginSebagaiJurusan != null && loginSebagaiJurusan.getFakultas() != null) ? loginSebagaiJurusan.getFakultas().getId() : (loginSebagaiFakultas != null ? loginSebagaiFakultas.getId() : null);
Long idJurLogin = (loginSebagaiJurusan != null) ? loginSebagaiJurusan.getId() : null;

// PRIVILEGES CHECK
boolean edit = false;
boolean add = false;
boolean delete = false;

if(tbmuser != null && tbmuser.getUserId() != null){
    edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser);
    delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE, tbmuser);
    add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE, tbmuser);
    if(Common.getApakahAdminLain(tbmuser)){
        edit = true; delete = true; add = true;
    }
}
%>

<div id="viewList<%=rnd%>" class="animate__animated animate__fadeInUp">
    <div class="row mb-4">
        <div class="col-12">
            <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
                <div class="card-header bg-white border-0 pt-4 pb-3 px-4 border-bottom border-light">
                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-2">
                        <div>
                            <h5 class="fw-bold text-dark mb-1">
                                <i class="fas fa-users text-primary me-2"></i><%=Common.getBahasaConfig("Data Organisasi Mahasiswa")%>
                            </h5>
                            <small class="text-muted"><%=Common.getBahasaConfig("Kelola data master Organisasi & Anggotanya.")%></small>
                        </div>
                        
                        <div class="d-flex align-items-center gap-2 flex-wrap">
                            <div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;">
                                <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>
                                <input type="text" class="form-control border-start-0 ps-0 bg-white fw-medium" id="searchOrganisasi<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Nama / Kode...")%>">
                            </div>
                            
                            <button class="btn btn-sm btn-outline-info rounded-pill px-3 shadow-sm fw-bold" type="button" data-bs-toggle="collapse" data-bs-target="#filterPanel<%=rnd%>">
                                <i class="fas fa-filter me-1"></i><%=Common.getBahasaConfig("Filter")%>
                            </button>

                            <button class="btn btn-sm btn-outline-secondary rounded-pill px-3 shadow-sm fw-bold" onclick="resetAndLoadData<%=rnd%>()">
                                <i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Segarkan")%>
                            </button>
                            
                            <% if (add) { %>
                            <button class="btn btn-sm btn-primary rounded-pill px-4 shadow-sm fw-bold" onclick="bukaFormTambah<%=rnd%>()">
                                <i class="fas fa-plus-circle me-1"></i><%=Common.getBahasaConfig("Tambah Organisasi")%>
                            </button>
                            <% } %>
                        </div>
                    </div>

                    <div class="collapse mt-3" id="filterPanel<%=rnd%>">
                        <div class="card card-body bg-light border-0 shadow-sm rounded-3 p-3">
                            <div class="row g-3">
                                <div class="col-md-4">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Fakultas")%></label>
                                    <select id="filterFakultas<%=rnd%>" class="form-select form-select-sm shadow-sm" onchange="loadFilterJurusan<%=rnd%>(this.value)">
                                        <option value=""><%=Common.getBahasaConfig("-- Semua Fakultas --")%></option>
                                    </select>
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Jurusan")%></label>
                                    <select id="filterJurusan<%=rnd%>" class="form-select form-select-sm shadow-sm">
                                        <option value=""><%=Common.getBahasaConfig("-- Semua Jurusan --")%></option>
                                    </select>
                                </div>
                                <div class="col-md-4 d-flex align-items-end">
                                    <button type="button" class="btn btn-sm btn-info text-white px-4 fw-bold shadow-sm rounded-pill w-100" onclick="resetAndLoadData<%=rnd%>()">
                                        <i class="fas fa-search me-1"></i> <%=Common.getBahasaConfig("Terapkan Filter")%>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                
                <div class="card-body p-4 pt-3">
                    <div class="table-responsive border rounded-3 shadow-sm bg-white">
                        <table class="table table-hover align-middle mb-0" id="tabelHTMLDataOrganisasi<%=rnd%>">
                            <thead class="table-dark text-center align-middle">
                                <tr>
                                    <th style="width: 50px;"></th>
                                    <th style="width: 60px;">#</th>
                                    <th class="text-start"><i class="fas fa-hashtag me-1"></i><%=Common.getBahasaConfig("Kode")%></th>
                                    <th class="text-start"><i class="fas fa-users me-1"></i><%=Common.getBahasaConfig("Nama Organisasi")%></th>
                                    <th class="text-start"><i class="fas fa-building me-1"></i><%=Common.getBahasaConfig("Fakultas / Jurusan")%></th>
                                    <th class="text-center no-export" style="width: 120px;"><i class="fas fa-cogs"></i></th>
                                </tr>
                            </thead>
                            <tbody id="tabelDataOrganisasi<%=rnd%>">
                                <tr><td colspan="6" class="text-center py-5"><div class="spinner-border text-primary"></div></td></tr>
                            </tbody>
                        </table>
                    </div>
                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mt-4">
                        <div class="small fw-medium text-muted bg-light px-3 py-1 rounded-pill border" id="pagingInfoOrganisasi<%=rnd%>"></div>
                        <nav>
                            <ul class="pagination pagination-sm mb-0 shadow-sm">
                                <li class="page-item"><button class="page-link" id="btnPrevPage<%=rnd%>" onclick="changePage<%=rnd%>(-1)"><i class="fas fa-chevron-left"></i></button></li>
                                <li class="page-item disabled"><span class="page-link" id="pageNumberDisplay<%=rnd%>">1</span></li>
                                <li class="page-item"><button class="page-link" id="btnNextPage<%=rnd%>" onclick="changePage<%=rnd%>(1)"><i class="fas fa-chevron-right"></i></button></li>
                            </ul>
                        </nav>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<div id="viewForm<%=rnd%>" class="animate__animated animate__fadeIn" style="display: none;">
    <div class="row justify-content-center">
        <div class="col-lg-12"> 
            <div class="card border-0 shadow-sm rounded-4 border-top border-success border-4">
                <div class="card-header bg-white border-0 pt-4 pb-0 px-4">
                    <h5 class="fw-bold text-dark mb-0" id="formTitle<%=rnd%>">
                        <i class="fas fa-edit text-success me-2"></i><%=Common.getBahasaConfig("Formulir Organisasi Intra Kampus")%>
                    </h5>
                </div>
                <div class="card-body p-4">
                    <form id="formOrganisasi<%=rnd%>" onsubmit="event.preventDefault(); simpanOrganisasi<%=rnd%>();">
                        <input type="hidden" id="inputIdOrganisasi<%=rnd%>" value="">
                        
                        <div class="row g-4 mt-1">
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Fakultas")%></label>
                                <select class="form-select shadow-sm" id="selectFakultas<%=rnd%>" onchange="loadJurusan<%=rnd%>(this.value, null)">
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Fakultas --")%></option>
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jurusan")%></label>
                                <select class="form-select shadow-sm" id="selectJurusan<%=rnd%>">
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Jurusan --")%></option>
                                </select>
                            </div>

                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Kode Organisasi")%></label>
                                <input type="text" class="form-control shadow-sm fw-bold" id="inputKode<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Otomatis jika kosong")%>">
                            </div>
                            <div class="col-md-8">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Organisasi")%> <span class="text-danger">*</span></label>
                                <input type="text" class="form-control fw-bold shadow-sm border-info" id="inputNama<%=rnd%>" required>
                            </div>
                            <div class="col-md-12">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Keterangan")%></label>
                                <textarea class="form-control shadow-sm" id="inputKeterangan<%=rnd%>" rows="2"></textarea>
                            </div>
                            <div class="col-12 mt-5 text-end border-top pt-3">
                                <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold border" onclick="tutupForm<%=rnd%>()"><%=Common.getBahasaConfig("Batal")%></button>
                                <% if (add || edit) { %>
                                <button type="submit" class="btn btn-success px-4 rounded-pill shadow-sm fw-bold" id="btnSimpan<%=rnd%>"><i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Data")%></button>
                                <% } %>
                            </div>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>

<div id="viewFormAnggota<%=rnd%>" class="animate__animated animate__zoomIn" style="display: none;">
    <div class="row justify-content-center">
        <div class="col-lg-8"> 
            <div class="card border-0 shadow-lg rounded-4 border-top border-info border-4 mt-4">
                <div class="card-header bg-white border-0 pt-4 pb-0 px-4">
                    <h5 class="fw-bold text-dark mb-0">
                        <i class="fas fa-user-plus text-info me-2"></i><%=Common.getBahasaConfig("Form Keanggotaan")%>
                    </h5>
                    <small class="text-muted" id="lblNamaOrganisasiFormAnggota<%=rnd%>"></small>
                </div>
                <div class="card-body p-4">
                    <form id="formAnggota<%=rnd%>" onsubmit="event.preventDefault(); simpanAnggota<%=rnd%>();">
                        <input type="hidden" id="inputIdAnggota<%=rnd%>" value="">
                        <input type="hidden" id="inputIdOrgForAnggota<%=rnd%>" value="">
                        
                        <div class="row g-3">
                            <div class="col-md-12">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Cari Mahasiswa (NIM / Nama)")%> <span class="text-danger">*</span></label>
                                <input type="text" class="form-control fw-bold shadow-sm" id="inputCariMhsAnggota<%=rnd%>" list="dlMhsAnggota<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik NIM atau Nama...")%>" oninput="onInputMhsAnggotaSearch<%=rnd%>(this.value)" onchange="checkSelectedMhsAnggota<%=rnd%>(this.value)" autocomplete="off" required>
                                <datalist id="dlMhsAnggota<%=rnd%>"></datalist>
                                <input type="hidden" id="hiddenIdMhsAnggota<%=rnd%>" required>
                            </div>
                            
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jabatan")%></label>
                                <input type="text" class="form-control shadow-sm" id="inputJabatanAnggota<%=rnd%>" placeholder="Cth: Ketua, Anggota, dll.">
                            </div>
                            
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tahun Kepengurusan / Periode")%></label>
                                <input type="text" class="form-control shadow-sm" id="inputPeriodeAnggota<%=rnd%>" placeholder="Cth: 2024/2025">
                            </div>

                            <div class="col-12 mt-4 text-end border-top pt-3">
                                <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold border" onclick="tutupFormAnggota<%=rnd%>()"><%=Common.getBahasaConfig("Batal")%></button>
                                <button type="submit" class="btn btn-info text-white px-4 rounded-pill shadow-sm fw-bold" id="btnSimpanAnggota<%=rnd%>">
                                    <i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Anggota")%>
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
    // ==========================================
    // DEKLARASI KELAS & KONTEKS (SILAKAN DISESUAIKAN)
    // ==========================================
    var masterModelClass<%=rnd%> = "ais.database.model.OrganisasiIntraKampus";
    
    // PERHATIAN: Ubah nama kelas ini dengan nama model Keanggotaan yang Anda miliki di Java!
    var detailModelClass<%=rnd%> = "ais.database.model.AnggotaOrganisasiIntraKampus"; 
    
    var activeEditingOrganisasiId<%=rnd%> = "";
    var currentPage<%=rnd%> = 1;
    var limitPerPage<%=rnd%> = 10;
    var totalRecords<%=rnd%> = 0;

    var idFakultasLogin<%=rnd%> = "<%= (idFakLogin != null) ? idFakLogin : "" %>";
    var idJurusanLogin<%=rnd%> = "<%= (idJurLogin != null) ? idJurLogin : "" %>";

    function showToast<%=rnd%>(msg, colorClass) {
        if (typeof tampilkanToast === "function") tampilkanToast(msg, colorClass);
        else alert(msg);
    }

    // ==========================================
    // LOGIKA LOAD MASTER (FAKULTAS/JURUSAN)
    // ==========================================
    async function loadFakultas<%=rnd%>(targetId, selectedId) {
        var reqObj = { action: "daftar", class: "ais.database.model.Fakultas", where1: "aktif = true OR aktif IS NULL", order1: "asc", sort1: "nama" };
        if(idFakultasLogin<%=rnd%>) reqObj.where1 += " AND id = " + idFakultasLogin<%=rnd%>;
        var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(reqObj) });
        var result = await res.json();
        var options = '<option value=""><%=Common.getBahasaConfig("-- Pilih Fakultas --")%></option>';
        (result.data || []).forEach(function(row) { 
            var sel = (selectedId == row.id) ? "selected" : "";
            options += '<option value="' + row.id + '" ' + sel + '>' + row.nama + '</option>'; 
        });
        document.getElementById(targetId).innerHTML = options;
    }

    async function loadJurusan<%=rnd%>(idFakultas, selectedId) {
        if(!idFakultas && !idJurusanLogin<%=rnd%>) { document.getElementById('selectJurusan<%=rnd%>').innerHTML = '<option value=""><%=Common.getBahasaConfig("-- Pilih Jurusan --")%></option>'; return; }
        var whereClause = "aktif = true OR aktif IS NULL";
        if(idJurusanLogin<%=rnd%>) whereClause += " AND id = " + idJurusanLogin<%=rnd%>;
        else if(idFakultas) whereClause += " AND fakultas = " + idFakultas;
        var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "daftar", class: "ais.database.model.Jurusan", where1: whereClause, order1: "asc", sort1: "nama" }) });
        var result = await res.json();
        var options = '<option value=""><%=Common.getBahasaConfig("-- Pilih Jurusan --")%></option>';
        (result.data || []).forEach(function(row) { 
            var sel = (selectedId == row.id) ? "selected" : "";
            options += '<option value="' + row.id + '" ' + sel + '>' + row.nama + '</option>'; 
        });
        document.getElementById('selectJurusan<%=rnd%>').innerHTML = options;
    }

    async function loadFilterJurusan<%=rnd%>(idFakultas) {
        if(!idFakultas && !idJurusanLogin<%=rnd%>) { document.getElementById('filterJurusan<%=rnd%>').innerHTML = '<option value=""><%=Common.getBahasaConfig("-- Semua Jurusan --")%></option>'; return; }
        var whereClause = "aktif = true OR aktif IS NULL";
        if(idJurusanLogin<%=rnd%>) whereClause += " AND id = " + idJurusanLogin<%=rnd%>;
        else if(idFakultas) whereClause += " AND fakultas = " + idFakultas;
        var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "daftar", class: "ais.database.model.Jurusan", where1: whereClause, order1: "asc", sort1: "nama" }) });
        var result = await res.json();
        var options = '<option value=""><%=Common.getBahasaConfig("-- Semua Jurusan --")%></option>';
        (result.data || []).forEach(function(row) { options += '<option value="' + row.id + '">' + row.nama + '</option>'; });
        document.getElementById('filterJurusan<%=rnd%>').innerHTML = options;
    }

    // ==========================================
    // CRUD LOGIC: MASTER ORGANISASI
    // ==========================================
    function resetAndLoadData<%=rnd%>() {
        currentPage<%=rnd%> = 1;
        loadDataOrganisasi<%=rnd%>();
    }

    async function loadDataOrganisasi<%=rnd%>() {
        var tbody = document.getElementById('tabelDataOrganisasi<%=rnd%>');
        var keyword = document.getElementById('searchOrganisasi<%=rnd%>').value.trim();
        var start = (currentPage<%=rnd%> - 1);
        var payload = { action: "daftar", class: masterModelClass<%=rnd%>, deep: "1", max: limitPerPage<%=rnd%>, halaman: start, order1: "desc", sort1: "id", count: "true" };
        var filters = [];
        
        if(keyword !== '') filters.push("(this_.nama ILIKE '%" + keyword + "%' OR this_.kode ILIKE '%" + keyword + "%')");
        
        var elFakultas = document.getElementById('filterFakultas<%=rnd%>');
        var fFak = elFakultas ? elFakultas.value : null;
        if(fFak) filters.push("this_.fakultas = " + fFak);

        var elJurusan = document.getElementById('filterJurusan<%=rnd%>');
        var fJur = elJurusan ? elJurusan.value : null;
        if(fJur) filters.push("this_.jurusan = " + fJur);

        if(filters.length > 0) { payload.where1 = filters.join(' AND '); }

        try {
            var response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
            });
            var result = await response.json();
            totalRecords<%=rnd%> = parseInt(result.count || 0);
            var html = '';
            
            (result.data || []).forEach(function(row, i) {
                var fakJur = (row["fakultas.nama"] || '-') + " / " + (row["jurusan.nama"] || '-');
                var safeNama = (row.nama || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                
                var btnEdit = '<button class="btn btn-sm btn-outline-warning me-1" onclick="editOrganisasi<%=rnd%>(' + row.id + ')" title="Edit Organisasi"><i class="fas fa-edit"></i></button>';
                var btnHapus = '<button class="btn btn-sm btn-outline-danger" onclick="hapusOrganisasi<%=rnd%>(' + row.id + ')" title="Hapus Organisasi"><i class="fas fa-trash-alt"></i></button>';
                
                var collapseId = 'collapseOrg<%=rnd%>_' + row.id;
                var btnAccordion = '<button class="btn btn-sm btn-light border shadow-sm text-primary rounded-circle" type="button" data-bs-toggle="collapse" data-bs-target="#' + collapseId + '" onclick="loadAnggotaOrganisasi<%=rnd%>(' + row.id + ')" title="Lihat Anggota"><i class="fas fa-chevron-down"></i></button>';

                // BAris 1: Data Master
                html += '<tr class="align-middle ' + (i % 2 === 0 ? 'bg-light' : '') + '">' +
                            '<td class="text-center">' + btnAccordion + '</td>' +
                            '<td class="text-center">' + (start * limitPerPage<%=rnd%> + i + 1) + '</td>' +
                            '<td><span class="badge bg-secondary">' + (row.kode || '-') + '</span></td>' +
                            '<td class="fw-bold text-primary">' + row.nama + '</td>' +
                            '<td>' + fakJur + '</td>' +
                            '<td class="text-center no-export">' + btnEdit + btnHapus + '</td>' +
                        '</tr>';
                        
                // Baris 2: Accordion Detail (Sembunyi by default)
                html += '<tr>' +
                            '<td colspan="6" class="p-0 border-0">' +
                                '<div class="collapse" id="' + collapseId + '">' +
                                    '<div class="card card-body m-3 bg-white border border-info shadow-sm rounded-4">' +
                                        '<div class="d-flex justify-content-between align-items-center border-bottom pb-2 mb-3">' +
                                            '<h6 class="fw-bold text-info mb-0"><i class="fas fa-users-cog me-2"></i>Keanggotaan: ' + safeNama + '</h6>' +
                                            '<button class="btn btn-sm btn-primary rounded-pill fw-bold shadow-sm" onclick="bukaFormTambahAnggota<%=rnd%>(' + row.id + ', \'' + safeNama + '\')"><i class="fas fa-plus-circle me-1"></i> Tambah Anggota</button>' +
                                        '</div>' +
                                        '<div class="table-responsive">' +
                                            '<table class="table table-sm table-bordered table-striped bg-white mb-0">' +
                                                '<thead class="table-info text-center">' +
                                                    '<tr><th>#</th><th>NIM</th><th><%=Common.getBahasaConfig("Nama Mahasiswa")%></th><th><%=Common.getBahasaConfig("Jabatan")%></th><th><%=Common.getBahasaConfig("Periode")%></th><th><%=Common.getBahasaConfig("Aksi")%></th></tr>' +
                                                '</thead>' +
                                                '<tbody id="tabelAnggota<%=rnd%>_' + row.id + '">' +
                                                    '<tr><td colspan="6" class="text-center text-muted small py-3"><div class="spinner-border spinner-border-sm text-info me-2"></div>Memuat data anggota...</td></tr>' +
                                                '</tbody>' +
                                            '</table>' +
                                        '</div>' +
                                    '</div>' +
                                '</div>' +
                            '</td>' +
                        '</tr>';
            });
            tbody.innerHTML = html || '<tr><td colspan="6" class="text-center text-muted py-4"><%=Common.getBahasaConfig("Tidak ada data Organisasi.")%></td></tr>';
            updatePaginationUI<%=rnd%>();
        } catch (e) { console.error(e); }
    }

    // ==========================================
    // CRUD LOGIC: DETAIL ANGGOTA (ACCORDION)
    // ==========================================
    async function loadAnggotaOrganisasi<%=rnd%>(idOrganisasi) {
        var tbody = document.getElementById('tabelAnggota<%=rnd%>_' + idOrganisasi);
        
        // Asumsi Struktur Field: organisasi, mahasiswa (relasi), jabatan, periode
        var payload = { 
            action: "daftar", 
            class: detailModelClass<%=rnd%>, 
            deep: "1", 
            where1: "organisasi = " + idOrganisasi, // Pastikan field relasi di model bernama 'organisasi'
            order1: "asc", 
            sort1: "id" 
        };

        try {
            var response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
            });
            var result = await response.json();
            var html = '';
            
            (result.data || []).forEach(function(row, i) {
                var btnHapus = '<button class="btn btn-sm btn-outline-danger shadow-sm px-2 py-0" onclick="hapusAnggota<%=rnd%>(' + row.id + ', ' + idOrganisasi + ')" title="Hapus Anggota"><i class="fas fa-times"></i></button>';
                
                // Menyesuaikan dengan asumsi field pada model AnggotaOrganisasi
                var mhsNim = row["mahasiswa.nim"] || '-';
                var mhsNama = row["mahasiswa.nama"] || '-';
                var jabatan = row.jabatan || '-';
                var periode = row.periode || '-';

                html += '<tr>' +
                            '<td class="text-center">' + (i + 1) + '</td>' +
                            '<td class="text-center">' + mhsNim + '</td>' +
                            '<td class="fw-semibold">' + mhsNama + '</td>' +
                            '<td class="text-center">' + jabatan + '</td>' +
                            '<td class="text-center">' + periode + '</td>' +
                            '<td class="text-center">' + btnHapus + '</td>' +
                        '</tr>';
            });
            tbody.innerHTML = html || '<tr><td colspan="6" class="text-center text-muted small py-3 fst-italic">Belum ada anggota/pengurus terdaftar.</td></tr>';
        } catch (e) { 
            console.error(e);
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger small py-3">Gagal memuat detail anggota. (Cek Nama Model)</td></tr>';
        }
    }

    // ==========================================
    // FORM DETAIL ANGGOTA
    // ==========================================
    function bukaFormTambahAnggota<%=rnd%>(idOrganisasi, namaOrganisasi) {
        document.getElementById('formAnggota<%=rnd%>').reset();
        document.getElementById('inputIdAnggota<%=rnd%>').value = '';
        document.getElementById('inputIdOrgForAnggota<%=rnd%>').value = idOrganisasi;
        document.getElementById('lblNamaOrganisasiFormAnggota<%=rnd%>').innerText = namaOrganisasi;
        
        document.getElementById('hiddenIdMhsAnggota<%=rnd%>').value = '';
        
        // Tampilkan Form
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewFormAnggota<%=rnd%>').style.display = 'block';
    }
    
    function tutupFormAnggota<%=rnd%>() {
        document.getElementById('viewFormAnggota<%=rnd%>').style.display = 'none';
        document.getElementById('viewList<%=rnd%>').style.display = 'block';
    }

    // PENCARIAN MAHASISWA UNTUK ANGGOTA
    async function onInputMhsAnggotaSearch<%=rnd%>(val) {
        if (val.length < 2) return;
        var payload = { 
            action: "daftar", 
            class: "ais.database.model.Mahasiswa", 
            where1: "aktif = true AND (nim ILIKE '%" + val + "%' OR nama ILIKE '%" + val + "%')", 
            max: 20 
        };
        try {
            var res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
            });
            var result = await res.json();
            var options = '';
            (result.data || []).forEach(function(row) {
                options += '<option data-id="' + row.id + '" value="' + (row.nim || '-') + ' - ' + row.nama + '"></option>';
            });
            document.getElementById('dlMhsAnggota<%=rnd%>').innerHTML = options;
        } catch (e) { console.error(e); }
    }

    function checkSelectedMhsAnggota<%=rnd%>(val) {
        var options = document.getElementById('dlMhsAnggota<%=rnd%>').options;
        for(var i=0; i<options.length; i++) {
            if(options[i].value === val) {
                document.getElementById('hiddenIdMhsAnggota<%=rnd%>').value = options[i].getAttribute('data-id');
                return;
            }
        }
        document.getElementById('hiddenIdMhsAnggota<%=rnd%>').value = '';
    }

    async function simpanAnggota<%=rnd%>() {
        var idMhs = document.getElementById('hiddenIdMhsAnggota<%=rnd%>').value;
        var idOrg = document.getElementById('inputIdOrgForAnggota<%=rnd%>').value;
        
        if (!idMhs) { 
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Pilih Mahasiswa dari daftar pencarian.")%>', "bg-warning text-dark"); 
            return; 
        }

        var dataObj = {
            organisasi: parseInt(idOrg), // Pastikan ini sesuai dengan field relasi di class detail
            mahasiswa: parseInt(idMhs),
            jabatan: document.getElementById('inputJabatanAnggota<%=rnd%>').value.trim() || null,
            periode: document.getElementById('inputPeriodeAnggota<%=rnd%>').value.trim() || null
        };

        var btn = document.getElementById('btnSimpanAnggota<%=rnd%>');
        btn.disabled = true;
        
        try {
            var response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: "simpanDataRinci", log: "true", class: detailModelClass<%=rnd%>, data: dataObj })
            });
            var result = await response.json();
            
            if (result.status === '00' || result.status === 'success') {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Anggota berhasil ditambahkan!")%>', "bg-success text-white");
                tutupFormAnggota<%=rnd%>();
                
                // Buka paksa accordion dan reload isinya
                document.getElementById('collapseOrg<%=rnd%>_' + idOrg).classList.add('show');
                loadAnggotaOrganisasi<%=rnd%>(idOrg);
            } else {
                 showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan.")%>', "bg-danger text-white");
            }
        } catch (e) { console.error(e); } finally { btn.disabled = false; }
    }

    async function hapusAnggota<%=rnd%>(idAnggota, idOrg) {
        if (!confirm('<%=Common.getBahasaConfigJS("Keluarkan anggota ini dari organisasi?")%>')) return;
        var res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: "hapusDataRinci", class: detailModelClass<%=rnd%>, id: idAnggota })
        });
        var result = await res.json();
        if(result.status === '00' || result.status === 'success') {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Anggota dikeluarkan.")%>', "bg-success text-white");
            loadAnggotaOrganisasi<%=rnd%>(idOrg); // Reload sub-tabel
        }
    }

    // ==========================================
    // SISA LOGIKA SIMPAN & EDIT ORGANISASI MASTER 
    // ==========================================
    async function simpanOrganisasi<%=rnd%>() {
        var currentId = document.getElementById('inputIdOrganisasi<%=rnd%>').value;
        var dataObj = {
            id: currentId ? parseInt(currentId) : null, 
            nama: document.getElementById('inputNama<%=rnd%>').value.trim(), 
            namaEn: document.getElementById('inputNamaEn<%=rnd%>').value.trim() || null,
            kode: document.getElementById('inputKode<%=rnd%>').value.trim() || null,
            keterangan: document.getElementById('inputKeterangan<%=rnd%>').value.trim() || null
        };

        var elFak = document.getElementById('selectFakultas<%=rnd%>');
        var fakVal = elFak ? elFak.value : null;
        if(fakVal) dataObj.fakultas = parseInt(fakVal);
        
        var elJur = document.getElementById('selectJurusan<%=rnd%>');
        var jurVal = elJur ? elJur.value : null;
        if(jurVal) dataObj.jurusan = parseInt(jurVal);

        var btn = document.getElementById('btnSimpan<%=rnd%>');
        btn.disabled = true;
        
        try {
            var response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: "simpanDataRinci", log: "true", class: masterModelClass<%=rnd%>, data: dataObj })
            });
            var result = await response.json();
            
            if (result.status === '00' || result.status === 'success') {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data berhasil disimpan!")%>', "bg-success text-white");
                tutupForm<%=rnd%>();
                loadDataOrganisasi<%=rnd%>();
            } else {
                 showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan data.")%>', "bg-danger text-white");
            }
        } catch (e) { console.error(e); } finally { btn.disabled = false; }
    }

    async function editOrganisasi<%=rnd%>(id) {
        bukaFormTambah<%=rnd%>();
        activeEditingOrganisasiId<%=rnd%> = id;
        document.getElementById('inputIdOrganisasi<%=rnd%>').value = id;
        
        var res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: "load", class: masterModelClass<%=rnd%>, id: id })
        });
        var result = await res.json();
        var data = result.data;
        
        document.getElementById('inputNama<%=rnd%>').value = data.nama || '';
        document.getElementById('inputNamaEn<%=rnd%>').value = data.namaEn || '';
        document.getElementById('inputKode<%=rnd%>').value = data.kode || '';
        document.getElementById('inputKeterangan<%=rnd%>').value = data.keterangan || '';
        
        await loadFakultas<%=rnd%>('selectFakultas<%=rnd%>', data["fakultas.id"]);
        await loadJurusan<%=rnd%>(data["fakultas.id"], data["jurusan.id"]);
    }

    async function hapusOrganisasi<%=rnd%>(id) {
        if (!confirm('<%=Common.getBahasaConfigJS("Hapus data organisasi ini secara permanen?")%>')) return;
        var res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: "hapusDataRinci", class: masterModelClass<%=rnd%>, id: id })
        });
        var result = await res.json();
        if(result.status === '00' || result.status === 'success') {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data dihapus.")%>', "bg-success text-white");
            loadDataOrganisasi<%=rnd%>();
        }
    }

    async function bukaFormTambah<%=rnd%>() {
        document.getElementById('formOrganisasi<%=rnd%>').reset();
        document.getElementById('inputIdOrganisasi<%=rnd%>').value = '';
        activeEditingOrganisasiId<%=rnd%> = "";

        var elFak = document.getElementById('selectFakultas<%=rnd%>');
        if (elFak) {
            if (idFakultasLogin<%=rnd%>) {
                await loadFakultas<%=rnd%>('selectFakultas<%=rnd%>', idFakultasLogin<%=rnd%>);
                elFak.disabled = true;
                
                var elJur = document.getElementById('selectJurusan<%=rnd%>');
                if (idJurusanLogin<%=rnd%>) {
                    await loadJurusan<%=rnd%>(idFakultasLogin<%=rnd%>, idJurusanLogin<%=rnd%>);
                    elJur.disabled = true;
                } else {
                    await loadJurusan<%=rnd%>(idFakultasLogin<%=rnd%>, null);
                    elJur.disabled = false;
                }
            } else {
                await loadFakultas<%=rnd%>('selectFakultas<%=rnd%>', null);
                elFak.disabled = false;
                loadJurusan<%=rnd%>(null, null);
                document.getElementById('selectJurusan<%=rnd%>').disabled = false;
            }
        }

        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewFormAnggota<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    }

    function tutupForm<%=rnd%>() {
        document.getElementById('viewForm<%=rnd%>').style.display = 'none';
        document.getElementById('viewList<%=rnd%>').style.display = 'block';
    }

    function updatePaginationUI<%=rnd%>() {
        var totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumberDisplay<%=rnd%>').innerText = currentPage<%=rnd%> + ' / ' + totalPages;
        document.getElementById('pagingInfoOrganisasi<%=rnd%>').innerText = 'Total: ' + totalRecords<%=rnd%> + ' data';
    }

    function changePage<%=rnd%>(dir) {
        var totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if(dir === -1 && currentPage<%=rnd%> > 1) { currentPage<%=rnd%>--; loadDataOrganisasi<%=rnd%>(); }
        else if(dir === 1 && currentPage<%=rnd%> < totalPages) { currentPage<%=rnd%>++; loadDataOrganisasi<%=rnd%>(); }
    }

    document.addEventListener("DOMContentLoaded", function() {
        if (document.getElementById('filterFakultas<%=rnd%>')) {
            loadFakultas<%=rnd%>('filterFakultas<%=rnd%>', null);
            loadFilterJurusan<%=rnd%>(null);
        }
        
        document.getElementById('searchOrganisasi<%=rnd%>').addEventListener("keydown", function(e) {
            if (e.key === "Enter") {
                e.preventDefault();
                resetAndLoadData<%=rnd%>(); 
            }
        });

        loadDataOrganisasi<%=rnd%>();
    });
</script>