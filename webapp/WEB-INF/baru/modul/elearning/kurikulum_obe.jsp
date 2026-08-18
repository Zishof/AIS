<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="java.util.ArrayList"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="java.util.List"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.database.model.KurikulumPunyaMatakuliah"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.Kurikulum"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.common.ConstantValues"%>
<%
String rnd = Common.getGeneratedBarCode(7);
Tbmuser tbmuser = Common.getCurrentUser(request);

if (tbmuser == null || tbmuser.getUserId() == null) {
    response.setStatus(401);
    out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
    return;
}

boolean edit = false;
if (tbmuser != null && tbmuser.getUserId() != null) {
    edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser);
    if (Common.getApakahAdminLain(tbmuser)) {
        edit = true;
    }
}

Fakultas currentFakultas = null;
Jurusan currentJurusan = null;
try {
    currentFakultas = tbmuser.getFakultas(); 
    currentJurusan = tbmuser.getJurusan();
} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/kurikulum_obe.jsp:41");}

// ====================================================================================
// LOGIKA FILTER ROLE (DOSEN & MAHASISWA) UNTUK KURIKULUM PUNYA MATAKULIAH
// ====================================================================================
boolean isMahasiswa = tbmuser.getMahasiswa() != null;
boolean isDosen = tbmuser.ambilDosen() != null;

Long idFakLogin = null;
Long idJurLogin = null;

// Kunci dropdown Fakultas & Prodi HANYA JIKA user BUKAN Dosen, BUKAN Mahasiswa, dan BUKAN Admin Universitas.
// Hal ini memberikan kebebasan bagi Dosen & Mahasiswa untuk menyaring RPS lintas Prodi/Fakultas.
if (!isMahasiswa && !isDosen && !Common.getApakahAdminLain(tbmuser)) {
    idFakLogin = currentJurusan != null && currentJurusan.getFakultas() != null ?
        currentJurusan.getFakultas().getId() : (currentFakultas != null ? currentFakultas.getId() : null);
    idJurLogin = currentJurusan != null ? currentJurusan.getId() : null;
}

List<Long> kurikulumPunyaMatakuliahIds = new ArrayList<Long>();

if(isMahasiswa){
    List<Long> perkuliahansIds = tbmuser.getMahasiswa().ambilPerkuliahanDanParalel();
    for(Long idPerkuliahan : perkuliahansIds){
        Perkuliahan perkuliahan = (Perkuliahan)GeneralValueObject.ambilData(Perkuliahan.class, idPerkuliahan.toString(), true);
        if(perkuliahan != null && perkuliahan.getKurikulumPunyaMatakuliah() != null){
            kurikulumPunyaMatakuliahIds.add(perkuliahan.getKurikulumPunyaMatakuliah().getId());
        }
    }
} else if(isDosen){
    List<Long> perkuliahansIds = tbmuser.ambilDosen().ambilPerkuliahan();
    for(Long idPerkuliahan : perkuliahansIds){
        Perkuliahan perkuliahan = (Perkuliahan)GeneralValueObject.ambilData(Perkuliahan.class, idPerkuliahan.toString(), true);
        if(perkuliahan != null && perkuliahan.getKurikulumPunyaMatakuliah() != null){
            kurikulumPunyaMatakuliahIds.add(perkuliahan.getKurikulumPunyaMatakuliah().getId());
        }
    }
}

StringBuilder kpmIdsBuilder = new StringBuilder();
for (int i = 0; i < kurikulumPunyaMatakuliahIds.size(); i++) {
    if (i > 0) kpmIdsBuilder.append(",");
    kpmIdsBuilder.append(kurikulumPunyaMatakuliahIds.get(i));
}
String kpmIdsStr = kpmIdsBuilder.length() > 0 ? kpmIdsBuilder.toString() : "0";
// ====================================================================================

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
    
    @media (min-width: 992px) {
        .modal-rps-wide { max-width: 90% !important; }
    }
    @media (max-width: 991.98px) {
        .modal-rps-wide { max-width: 100% !important; margin: 0 !important; }
        .modal-rps-wide .modal-content { min-height: 100vh; border-radius: 0 !important; }
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
                                <i class="fas fa-project-diagram text-primary me-2"></i><%=Common.getBahasaConfig("Manajemen RPS OBE")%>
                            </h5>
                            <small class="text-muted"><%=Common.getBahasaConfig("Kelola detail Rencana Pembelajaran Semester (RPS) berbasis OBE.")%></small>
                        </div>
                        <div class="d-flex align-items-center gap-2 flex-wrap">
                            <div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;">
                                <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>
                                <input type="text" class="form-control border-start-0 ps-0 bg-white fw-medium" id="searchRps<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Kode / Nama MK...")%>">
                            </div>
                            <button class="btn btn-sm btn-outline-info rounded-pill px-3 shadow-sm fw-bold" type="button" data-bs-toggle="collapse" data-bs-target="#filterPanel<%=rnd%>">
                                <i class="fas fa-filter me-1"></i><%=Common.getBahasaConfig("Saring")%>
                            </button>
                            <button class="btn btn-sm btn-outline-secondary rounded-pill px-3 shadow-sm fw-bold" onclick="resetAndLoadData<%=rnd%>()" title="<%=Common.getBahasaConfig("Muat Ulang Data")%>">
                                <i class="fas fa-sync-alt"></i>
                            </button>
                            <div class="btn-group shadow-sm">
                                <button class="btn btn-sm btn-outline-success fw-bold" onclick="downloadExcel<%=rnd%>()"><i class="fas fa-file-excel me-1"></i>Ekspor</button>
                                <button class="btn btn-sm btn-outline-danger fw-bold" onclick="cetakPDF<%=rnd%>()"><i class="fas fa-print me-1"></i>PDF</button>
                            </div>
                        </div>
                    </div>

                    <div class="collapse mt-3" id="filterPanel<%=rnd%>">
                        <div class="card card-body bg-light border-0 shadow-sm rounded-3 p-3">
                            <h6 class="fw-bold text-secondary mb-3"><i class="fas fa-sliders-h me-2"></i><%=Common.getBahasaConfig("Penyaringan Lanjutan")%></h6>
                            <div class="row g-3">
                                <div class="col-md-4">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Fakultas")%></label>
                                    <select id="filterFakultas<%=rnd%>" class="form-select form-select-sm shadow-sm" onchange="loadJurusanUI<%=rnd%>(this.value, 'filterJurusan<%=rnd%>')">
                                        <option value=""><%=Common.getBahasaConfig("-- Semua Fakultas --")%></option>
                                    </select>
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Program Studi (Jurusan)")%></label>
                                    <select id="filterJurusan<%=rnd%>" class="form-select form-select-sm shadow-sm">
                                        <option value=""><%=Common.getBahasaConfig("-- Semua Program Studi --")%></option>
                                    </select>
                                </div>
                                <div class="col-md-4">
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
                        <table class="table table-hover table-striped align-middle mb-0" id="tabelHTMLDataRps<%=rnd%>">
                            <thead class="table-dark text-center align-middle">
                                <tr>
                                    <th class="fw-semibold text-uppercase small px-3 py-3" style="width: 50px;">#</th>
                                    <th class="text-start fw-semibold text-uppercase small px-3" style="width: 120px;"><i class="fas fa-barcode me-1"></i><%=Common.getBahasaConfig("Kode MK")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-book me-1"></i><%=Common.getBahasaConfig("Matakuliah")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-sitemap me-1"></i><%=Common.getBahasaConfig("Kurikulum")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-university me-1"></i><%=Common.getBahasaConfig("Fakultas / Jurusan")%></th>
                                    <th class="text-center fw-semibold text-uppercase small px-3"><i class="fas fa-toggle-on me-1"></i><%=Common.getBahasaConfig("Status")%></th>
                                    <th class="fw-semibold text-uppercase small px-3 text-center no-print" style="width: 80px;"><i class="fas fa-cogs"></i></th>
                                </tr>
                            </thead>
                            <tbody id="tabelDataRps<%=rnd%>">
                                <tr><td colspan="7" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Memuat data...")%></span></td></tr>
                            </tbody>
                        </table>
                    </div>

                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mt-4 no-print">
                        <div class="small fw-medium text-muted bg-light px-3 py-1 rounded-pill border" id="pagingInfoRps<%=rnd%>"></div>
                        <nav>
                            <ul class="pagination pagination-sm mb-0 shadow-sm">
                                <li class="page-item"><button class="page-link text-primary rounded-start-pill px-3 fw-bold" id="btnPrevPage<%=rnd%>" onclick="changePageRps<%=rnd%>(-1)"><i class="fas fa-chevron-left"></i></button></li>
                                <li class="page-item disabled"><span class="page-link bg-light text-dark px-3 fw-bold" id="pageNumberDisplay<%=rnd%>">1</span></li>
                                <li class="page-item"><button class="page-link text-primary rounded-end-pill px-3 fw-bold" id="btnNextPage<%=rnd%>" onclick="changePageRps<%=rnd%>(1)"><i class="fas fa-chevron-right"></i></button></li>
                            </ul>
                        </nav>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    const classModel<%=rnd%> = "<%=KurikulumPunyaMatakuliah.class.getName()%>";
    const classJurusan<%=rnd%> = "<%=Jurusan.class.getName()%>";
    const classFakultas<%=rnd%> = "<%=Fakultas.class.getName()%>";
    const rootUrl<%=rnd%> = "<%=Common.ROOT%>";
    
    let currentPage<%=rnd%> = 1;
    const limitPerPage<%=rnd%> = 10;
    let totalRecords<%=rnd%> = 0;

    const idFakultasLogin<%=rnd%> = <%= idFakLogin != null ? idFakLogin : "null" %>;
    const idJurusanLogin<%=rnd%> = <%= idJurLogin != null ? idJurLogin : "null" %>;
    
    // JS Variabel Role & Filtering
    const privEdit<%=rnd%> = <%= edit %>;
    const isMahasiswa<%=rnd%> = <%= isMahasiswa %>;
    const isDosen<%=rnd%> = <%= isDosen %>;
    const kpmIdsStr<%=rnd%> = "<%= kpmIdsStr %>";

    if(!document.getElementById('loadingOverlay<%=rnd%>')) {
        var overlayHtml = 
        '<div id="loadingOverlay<%=rnd%>" class="d-none" style="position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(255,255,255,0.75); z-index:10050; justify-content:center; align-items:center; flex-direction:column; backdrop-filter: blur(2px);">' +
            '<div class="spinner-border text-primary mb-3" style="width: 4rem; height: 4rem; border-width: 0.35em;"></div>' +
            '<h6 class="fw-bold text-dark m-0 shadow-sm px-4 py-2 bg-white rounded-pill border"><i class="fas fa-hourglass-half text-warning me-2"></i><%=Common.getBahasaConfig("Sedang Memproses...")%></h6>' +
        '</div>';
        document.body.insertAdjacentHTML('beforeend', overlayHtml);
    }

    const showLoading<%=rnd%> = () => { document.getElementById('loadingOverlay<%=rnd%>').classList.replace('d-none', 'd-flex'); };
    const hideLoading<%=rnd%> = () => { document.getElementById('loadingOverlay<%=rnd%>').classList.replace('d-flex', 'd-none'); };
    const showToast<%=rnd%> = (msg, colorClass) => { if(typeof tampilkanToast === "function") { tampilkanToast(msg, colorClass); } else { alert(msg); } };

    const loadFakultasUI<%=rnd%> = async (targetId, selectedId) => {
        let reqObj = { "action": "daftar", "class": classFakultas<%=rnd%>, "where1": "(aktif = true or aktif is null)", "order1": "asc", "sort1": "nama" };
        if (idFakultasLogin<%=rnd%> !== null) reqObj.where1 += " AND id = " + idFakultasLogin<%=rnd%>;
        try {
            const res = await fetch(rootUrl<%=rnd%> + '/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(reqObj) });
            const result = await res.json();
            const data = result.data || [];
            let targetEl = document.getElementById(targetId);
            let options = '<option value=""><%=Common.getBahasaConfig("-- Semua Fakultas --")%></option>';
            if (data.length === 1 || idFakultasLogin<%=rnd%> !== null) { options = ''; targetEl.disabled = true; } else { targetEl.disabled = false; }
            data.forEach((row) => {
                const selected = (selectedId && selectedId == row.id) || (data.length === 1) ? "selected" : "";
                options += '<option value="' + row.id + '" ' + selected + '>' + row.nama + '</option>';
            });
            targetEl.innerHTML = options;
        } catch (e) { console.error(e); }
    };

    const loadJurusanUI<%=rnd%> = async (idFak, targetId, selectedId) => {
        let reqObj = { "action": "daftar", "class": classJurusan<%=rnd%>, "where1": "(aktif = true or aktif is null)", "order1": "asc", "sort1": "nama" };
        let filterWhere = [];
        if (idJurusanLogin<%=rnd%> !== null) filterWhere.push("id = " + idJurusanLogin<%=rnd%>);
        else {
            if (idFak) filterWhere.push("fakultas = " + idFak);
            else if (idFakultasLogin<%=rnd%> !== null) filterWhere.push("fakultas = " + idFakultasLogin<%=rnd%>);
        }
        if (filterWhere.length > 0) reqObj.where1 += " AND " + filterWhere.join(' AND ');
        try {
        	console.log("reqObj", reqObj);
            const res = await fetch(rootUrl<%=rnd%> + '/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(reqObj) });
            const result = await res.json();
            const data = result.data || [];
            let targetEl = document.getElementById(targetId);
            let options = '<option value=""><%=Common.getBahasaConfig("-- Semua Program Studi --")%></option>';
            if (data.length === 1 || idJurusanLogin<%=rnd%> !== null) { options = ''; targetEl.disabled = true; } else { targetEl.disabled = false; }
            console.log("data", data);
            data.forEach((row) => {
                const selected = (selectedId && selectedId == row.id) || (data.length === 1) ? "selected" : "";
                options += '<option value="' + row.id + '" ' + selected + '>' + row.nama + '</option>';
            });
            targetEl.innerHTML = options;
        } catch (e) { console.error(e); }
    };

    const buildFilterSQL<%=rnd%> = () => {
        const keyword = document.getElementById('searchRps<%=rnd%>').value.trim().replace(/'/g, "''");
        let filters = ["kurikulum2_.obe = true"]; 
        
        if (keyword !== '') filters.push("(matakuliah1_.nama ILIKE '%" + keyword + "%' OR matakuliah1_.kode ILIKE '%" + keyword + "%')");
        
        const fFak = document.getElementById('filterFakultas<%=rnd%>').value;
        const fJur = document.getElementById('filterJurusan<%=rnd%>').value;
        
        if (idJurusanLogin<%=rnd%> !== null) filters.push("kurikulum2_.jurusan = " + idJurusanLogin<%=rnd%>);
        else if (fJur) filters.push("kurikulum2_.jurusan = " + fJur);
        else if (idFakultasLogin<%=rnd%> !== null) filters.push("jurusan3_.fakultas = " + idFakultasLogin<%=rnd%>);
        else if (fFak) filters.push("jurusan3_.fakultas = " + fFak);
        
        const stt = document.getElementById('filterStatus<%=rnd%>').value;
        if (stt === 'TRUE') filters.push("(this_.aktif = true OR this_.aktif IS NULL)");
        else if (stt === 'FALSE') filters.push("this_.aktif = false");

        // Filter Spesifik Dosen & Mahasiswa
        if (isMahasiswa<%=rnd%> || isDosen<%=rnd%>) {
            filters.push("this_.id IN (" + kpmIdsStr<%=rnd%> + ")");
        }

        return filters.length > 0 ? filters.join(' AND ') : '';
    };

    const resetAndLoadData<%=rnd%> = () => { currentPage<%=rnd%> = 1; loadDataRps<%=rnd%>(); };

    const loadDataRps<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataRps<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Mengambil data...")%></span></td></tr>';
        
        const start = (currentPage<%=rnd%> - 1);
        const whereClause = buildFilterSQL<%=rnd%>();
        
        let reqObj = {
            "action": "daftar", "class": classModel<%=rnd%>, "deep": "3",
            "max": limitPerPage<%=rnd%>, "halaman": start,
            "order1": "asc", "sort1": "matakuliah.nama", "count": "true",
            "alias1": "matakuliah", "alias2": "kurikulum", "alias3": "kurikulum.jurusan", "alias4": "kurikulum.jurusan.fakultas"
        };
        if (whereClause) reqObj["where1"] = whereClause;

        try {
            const res = await fetch(rootUrl<%=rnd%> + '/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(reqObj) });
            const dataResponse = await res.json();
            totalRecords<%=rnd%> = parseInt(dataResponse.count || 0);
            const recordList = dataResponse.data || [];
            let htmlList = '';
            
            if (recordList.length === 0) {
                htmlList = '<tr><td colspan="7" class="text-center text-muted py-5"><i class="fas fa-folder-open fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Tidak ada data ditemukan.")%></td></tr>';
            } else {
                let no = (start * limitPerPage<%=rnd%>) + 1;
                recordList.forEach((row) => {
                    const mkKode = row["matakuliah.kode"] || '-';
                    const mkNama = (row["matakuliah.nama"] || '-').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    const mkSks = row["matakuliah.sks"] || '0';
                    const kurNama = row["kurikulum.nama"] || '-';
                    const jurNama = row["kurikulum.jurusan.nama"] || '-';
                    const fakNama = row["kurikulum.jurusan.fakultas.nama"] || '-';
                    const isLocked = row["dikunci.id"] != null;
                    const statusVal = (row.aktif !== false) ? '<span class="badge bg-success"><i class="fas fa-check-circle me-1"></i><%=Common.getBahasaConfig("Aktif")%></span>' : '<span class="badge bg-danger"><i class="fas fa-times-circle me-1"></i><%=Common.getBahasaConfig("Tidak Aktif")%></span>';
                    const lockVal = isLocked ? '<br><span class="badge bg-warning text-dark mt-1"><i class="fas fa-lock me-1"></i>Dikunci</span>' : '';
                    
                    // ===============================================
                    // LOGIKA TOMBOL LIHAT/EDIT BERDASARKAN ROLE
                    // ===============================================
                    let actionBtns = '';
                    if (privEdit<%=rnd%>) {
                        const btnIcon = isLocked ? 'fa-lock' : 'fa-edit';
                        const btnColor = isLocked ? 'btn-outline-secondary' : 'btn-outline-warning text-dark';
                        actionBtns += '<button class="btn btn-sm ' + btnColor + ' shadow-sm px-2 me-1 fw-bold no-print" onclick="editRps<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Kelola RPS")%>"><i class="fas ' + btnIcon + '"></i></button>';
                    } else {
                        // Jika bukan admin (Dosen / Mahasiswa), hanya ada tombol Lihat
                        actionBtns += '<button class="btn btn-sm btn-outline-info shadow-sm px-3 me-1 fw-bold no-print" onclick="editRps<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Lihat RPS")%>"><i class="fas fa-eye me-1"></i><%=Common.getBahasaConfig("Lihat")%></button>';
                    }

                    htmlList += '<tr>' +
                                    '<td class="text-center text-muted fw-medium">' + (no++) + '</td>' +
                                    '<td class="text-start fw-bold text-secondary">' + mkKode + '</td>' +
                                    '<td class="text-start"><b class="text-primary">' + mkNama + '</b><br><small class="text-muted">' + mkSks + ' SKS</small></td>' +
                                    '<td class="text-start fw-medium text-dark small">' + kurNama + '</td>' +
                                    '<td class="text-start small"><b class="text-secondary">' + jurNama + '</b><br><span class="text-muted">' + fakNama + '</span></td>' +
                                    '<td class="text-center align-middle">' + statusVal + lockVal + '</td>' +
                                    '<td class="text-center text-nowrap no-print">' + actionBtns + '</td>' +
                                '</tr>';
                });
            }
            tbody.innerHTML = htmlList;
            updatePaginationUI<%=rnd%>();
        } catch (error) {
            console.error(error);
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal menarik data. Silakan muat ulang.")%></td></tr>';
        }
    };

    const updatePaginationUI<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumberDisplay<%=rnd%>').innerText = currentPage<%=rnd%> + " / " + totalPages;
        const startIdx = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%> + 1;
        const endIdx = Math.min(currentPage<%=rnd%> * limitPerPage<%=rnd%>, totalRecords<%=rnd%>);
        const startInfo = totalRecords<%=rnd%> > 0 ? startIdx : 0;
        document.getElementById('pagingInfoRps<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startInfo + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari")%> <b class="text-dark">' + totalRecords<%=rnd%> + '</b> <%=Common.getBahasaConfig("data")%>';
        document.getElementById('btnPrevPage<%=rnd%>').disabled = (currentPage<%=rnd%> === 1);
        document.getElementById('btnNextPage<%=rnd%>').disabled = (currentPage<%=rnd%> === totalPages);
    };

    const changePageRps<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if (direction === -1 && currentPage<%=rnd%> > 1) { currentPage<%=rnd%>--; loadDataRps<%=rnd%>(); } 
        else if (direction === 1 && currentPage<%=rnd%> < totalPages) { currentPage<%=rnd%>++; loadDataRps<%=rnd%>(); }
    };

    // ==========================================
    // LOGIKA MODAL POPUP DINAMIS UNTUK FORM EDIT
    // ==========================================
    const editRps<%=rnd%> = (id) => {
        window.variable = window.variable || 0;
        window.variable++;
        
        var variable = window.variable;
        var modalId = 'modalEditRps' + variable;
        var contentModal = modalId;
        var simpandata = 'hidden';
        
        var zIndexModal = 1050 + (variable * 2);
        const modalHtml = 
        '<div class="modal fade" id="' + contentModal + '" tabindex="-1" aria-labelledby="modalLabel" aria-hidden="true" ' + (simpandata != null && simpandata == 'hidden' ? '' : 'data-bs-backdrop="static"') + ' style="z-index: ' + zIndexModal + ';">' +
            '<div class="modal-dialog modal-dialog-scrollable mt-5 animate__animated animate__rotateInDownLeft modal-rps-wide">' +
                '<div class="modal-content shadow-lg">' +
                    '<div class="modal-header" style="background-color: rgb(180 212 255) !important; font-size: 22px;">' +
                        '<h5 class="modal-title" style="font-weight: bold; font-size: 22px;" id="modalLabel' + variable + '">' +
                            '<i class="far fa-edit"></i> <label id="labelHeader' + variable + '" class="modal-title"><%=Common.getBahasaConfig("Pengelolaan Rencana Pembelajaran Semester (RPS)")%></label>' +
                        '</h5>' +
                        '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<%=Common.getBahasaConfig("Tutup")%>"></button>' +
                    '</div>' +
                    '<div class="modal-body p-0" id="modalBodyContent' + variable + '">' +
                        '<div class="text-center py-5">' +
                            '<div class="spinner-border text-primary" role="status">' +
                                '<span class="visually-hidden"><%=Common.getBahasaConfig("Loading")%>...</span>' +
                            '</div>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>' +
        '</div>';

        document.body.insertAdjacentHTML('beforeend', modalHtml);
        var myModal = new bootstrap.Modal(document.getElementById(contentModal));
        myModal.show();

        // Meneruskan variabel isDosen & isMahasiswa ke _edit_rps.jsp
        var urlHtml = rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_edit_rps&kurikulumPunyaMatakuliah=" + encodeURIComponent(id);
        fetch(urlHtml)
            .then(response => response.text())
            .then(html => {
                const container = document.getElementById('modalBodyContent' + variable);
                container.innerHTML = html;
                Array.from(container.querySelectorAll("script")).forEach(oldScript => {
                    const newScript = document.createElement("script");
                    Array.from(oldScript.attributes).forEach(attr => { if (attr.name.toLowerCase() !== 'type') newScript.setAttribute(attr.name, attr.value); });
                    newScript.type = 'text/javascript';
                    newScript.appendChild(document.createTextNode(oldScript.innerHTML));
                    oldScript.parentNode.replaceChild(newScript, oldScript);
                });
            })
            .catch(err => {
                console.error(err);
                document.getElementById('modalBodyContent' + variable).innerHTML = 
                    '<div class="alert alert-danger m-4 rounded-4 shadow-sm"><i class="fas fa-exclamation-triangle me-2"></i><%=Common.getBahasaConfig("Gagal memuat formulir ubah data.")%></div>';
            });
    };

    const downloadExcel<%=rnd%> = () => {
        try {
            const table = document.getElementById('tabelHTMLDataRps<%=rnd%>');
            const cloneTable = table.cloneNode(true);
            const elementsToRemove = cloneTable.querySelectorAll('.no-print');
            elementsToRemove.forEach(el => el.parentNode.removeChild(el));
            if(typeof XLSX !== 'undefined') {
                const wb = XLSX.utils.table_to_book(cloneTable, {sheet: "RPS_OBE"});
                XLSX.writeFile(wb, "Data_RPS_OBE.xlsx");
            } else {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Library Excel tidak dimuat. Gagal mengunduh.")%>', 'bg-warning text-dark');
            }
        } catch(e) { console.error(e); }
    };
    const cetakPDF<%=rnd%> = () => { window.print(); };

    document.addEventListener("DOMContentLoaded", async () => {
        showLoading<%=rnd%>();
        document.getElementById('searchRps<%=rnd%>').addEventListener("keydown", (e) => {
            if (e.key === "Enter") { e.preventDefault(); resetAndLoadData<%=rnd%>(); }
        });
        await loadFakultasUI<%=rnd%>('filterFakultas<%=rnd%>', idFakultasLogin<%=rnd%>);
        await loadJurusanUI<%=rnd%>(idFakultasLogin<%=rnd%>, 'filterJurusan<%=rnd%>', idJurusanLogin<%=rnd%>);
        await loadDataRps<%=rnd%>();
        hideLoading<%=rnd%>();
    });
</script>