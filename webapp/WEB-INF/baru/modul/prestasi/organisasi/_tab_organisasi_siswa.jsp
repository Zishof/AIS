<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.database.model.sekolah.OrganisasiSiswa"%>
<%@page import="ais.database.model.sekolah.OrganisasiSiswaPunyaSiswa"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.sekolah.Yayasan"%>
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

Sekolah loginSebagaiSekolah = tbmuser.getSekolah();
Yayasan loginSebagaiYayasan = tbmuser.getYayasan();
Long idSekolahLogin = (loginSebagaiSekolah != null) ? loginSebagaiSekolah.getId() : null;
Long idYayasanLogin = (loginSebagaiYayasan != null) ? loginSebagaiYayasan.getId() : null;

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
                                <i class="fas fa-users text-primary me-2"></i><%=Common.getBahasaConfig("Data Organisasi Siswa")%>
                            </h5>
                            <small class="text-muted"><%=Common.getBahasaConfig("Kelola data master Organisasi Siswa & Keanggotaannya.")%></small>
                        </div>

                        <div class="d-flex align-items-center gap-2 flex-wrap">
                            <div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;">
                                <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>
                                <input type="text" class="form-control border-start-0 ps-0 bg-white fw-medium" id="searchOrg<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Nama / Kode...")%>">
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
                                <div class="col-md-5">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Sekolah")%></label>
                                    <select id="filterSekolah<%=rnd%>" class="form-select form-select-sm shadow-sm">
                                        <option value=""><%=Common.getBahasaConfig("-- Semua Sekolah --")%></option>
                                    </select>
                                </div>
                                <div class="col-md-5">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Yayasan")%></label>
                                    <select id="filterYayasan<%=rnd%>" class="form-select form-select-sm shadow-sm">
                                        <option value=""><%=Common.getBahasaConfig("-- Semua Yayasan --")%></option>
                                    </select>
                                </div>
                                <div class="col-md-2 d-flex align-items-end">
                                    <button type="button" class="btn btn-sm btn-info text-white px-3 fw-bold shadow-sm rounded-pill w-100" onclick="resetAndLoadData<%=rnd%>()">
                                        <i class="fas fa-search"></i>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="card-body p-4 pt-3">
                    <div class="table-responsive border rounded-3 shadow-sm bg-white">
                        <table class="table table-hover align-middle mb-0">
                            <thead class="table-dark text-center align-middle">
                                <tr>
                                    <th style="width: 50px;"></th>
                                    <th style="width: 60px;">#</th>
                                    <th class="text-start"><i class="fas fa-hashtag me-1"></i><%=Common.getBahasaConfig("Kode")%></th>
                                    <th class="text-start"><i class="fas fa-users me-1"></i><%=Common.getBahasaConfig("Nama Organisasi")%></th>
                                    <th class="text-start"><i class="fas fa-school me-1"></i><%=Common.getBahasaConfig("Sekolah")%></th>
                                    <th class="text-center no-export" style="width: 120px;"><i class="fas fa-cogs"></i></th>
                                </tr>
                            </thead>
                            <tbody id="tabelDataOrg<%=rnd%>">
                                <tr><td colspan="6" class="text-center py-5"><div class="spinner-border text-primary"></div></td></tr>
                            </tbody>
                        </table>
                    </div>
                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mt-4">
                        <div class="small fw-medium text-muted bg-light px-3 py-1 rounded-pill border" id="pagingInfoOrg<%=rnd%>"></div>
                        <nav><ul class="pagination pagination-sm mb-0 shadow-sm">
                            <li class="page-item"><button class="page-link" onclick="changePage<%=rnd%>(-1)"><i class="fas fa-chevron-left"></i></button></li>
                            <li class="page-item disabled"><span class="page-link" id="pageNumOrg<%=rnd%>">1</span></li>
                            <li class="page-item"><button class="page-link" onclick="changePage<%=rnd%>(1)"><i class="fas fa-chevron-right"></i></button></li>
                        </ul></nav>
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
                    <h5 class="fw-bold text-dark mb-0"><i class="fas fa-edit text-success me-2"></i><%=Common.getBahasaConfig("Formulir Organisasi Siswa")%></h5>
                </div>
                <div class="card-body p-4">
                    <form id="formOrg<%=rnd%>" onsubmit="event.preventDefault(); simpanOrg<%=rnd%>();">
                        <input type="hidden" id="inputIdOrg<%=rnd%>" value="">
                        <div class="row g-4 mt-1">
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Yayasan")%></label>
                                <select class="form-select shadow-sm" id="selectYayasan<%=rnd%>" onchange="loadSekolah<%=rnd%>(this.value, null)">
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Yayasan --")%></option>
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Sekolah")%></label>
                                <select class="form-select shadow-sm" id="selectSekolah<%=rnd%>">
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Sekolah --")%></option>
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
                                <button type="submit" class="btn btn-success px-4 rounded-pill shadow-sm fw-bold" id="btnSimpanOrg<%=rnd%>"><i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Data")%></button>
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
                    <h5 class="fw-bold text-dark mb-0"><i class="fas fa-user-plus text-info me-2"></i><%=Common.getBahasaConfig("Form Keanggotaan Siswa")%></h5>
                    <small class="text-muted" id="lblNamaOrgAnggota<%=rnd%>"></small>
                </div>
                <div class="card-body p-4">
                    <form id="formAnggota<%=rnd%>" onsubmit="event.preventDefault(); simpanAnggota<%=rnd%>();">
                        <input type="hidden" id="inputIdAnggota<%=rnd%>" value="">
                        <input type="hidden" id="inputIdOrgAnggota<%=rnd%>" value="">
                        <div class="row g-3">
                            <div class="col-md-12">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Cari Siswa (NIS / Nama)")%> <span class="text-danger">*</span></label>
                                <input type="text" class="form-control fw-bold shadow-sm" id="inputCariSiswaAnggota<%=rnd%>" list="dlSiswaAnggota<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik NIS atau Nama...")%>" oninput="onInputSiswaAnggotaSearch<%=rnd%>(this.value)" onchange="checkSelectedSiswaAnggota<%=rnd%>(this.value)" autocomplete="off" required>
                                <datalist id="dlSiswaAnggota<%=rnd%>"></datalist>
                                <input type="hidden" id="hiddenIdSiswaAnggota<%=rnd%>" required>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jabatan")%></label>
                                <input type="text" class="form-control shadow-sm" id="inputJabatanAnggota<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cth: Ketua, Anggota...")%>">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tahun Kepengurusan / Periode")%></label>
                                <input type="text" class="form-control shadow-sm" id="inputPeriodeAnggota<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cth: 2024/2025")%>">
                            </div>
                            <div class="col-12 mt-4 text-end border-top pt-3">
                                <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold border" onclick="tutupFormAnggota<%=rnd%>()"><%=Common.getBahasaConfig("Batal")%></button>
                                <button type="submit" class="btn btn-info text-white px-4 rounded-pill shadow-sm fw-bold" id="btnSimpanAnggota<%=rnd%>"><i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Anggota")%></button>
                            </div>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    var masterModelOrg<%=rnd%> = "<%=OrganisasiSiswa.class.getName()%>";
    var detailModelOrg<%=rnd%> = "<%=OrganisasiSiswaPunyaSiswa.class.getName()%>";
    var currentPage<%=rnd%> = 1;
    var limitPerPage<%=rnd%> = 10;
    var totalRecords<%=rnd%> = 0;
    var idSekolahLogin<%=rnd%> = "<%= (idSekolahLogin != null) ? idSekolahLogin : "" %>";
    var idYayasanLogin<%=rnd%> = "<%= (idYayasanLogin != null) ? idYayasanLogin : "" %>";

    function showToast<%=rnd%>(msg, colorClass) {
        if(typeof tampilkanToast === "function") tampilkanToast(msg, colorClass); else alert(msg);
    }

    async function loadYayasan<%=rnd%>(targetId, selectedId) {
        var reqObj = { action: "daftar", class: "ais.database.model.sekolah.Yayasan", order1: "asc", sort1: "nama" };
        if(idYayasanLogin<%=rnd%>) reqObj.where1 = "id = " + idYayasanLogin<%=rnd%>;
        var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(reqObj) });
        var result = await res.json();
        var options = '<option value=""><%=Common.getBahasaConfig("-- Pilih Yayasan --")%></option>';
        (result.data || []).forEach(function(row) { options += '<option value="' + row.id + '" ' + (selectedId == row.id ? "selected" : "") + '>' + row.nama + '</option>'; });
        document.getElementById(targetId).innerHTML = options;
    }

    async function loadSekolah<%=rnd%>(idYayasan, selectedId) {
        var elSek = document.getElementById('selectSekolah<%=rnd%>');
        if(!idYayasan && !idSekolahLogin<%=rnd%>) { elSek.innerHTML = '<option value=""><%=Common.getBahasaConfig("-- Pilih Sekolah --")%></option>'; return; }
        var where1 = "";
        if(idSekolahLogin<%=rnd%>) where1 = "id = " + idSekolahLogin<%=rnd%>;
        else if(idYayasan) where1 = "yayasan = " + idYayasan;
        var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "daftar", class: "ais.database.model.sekolah.Sekolah", where1: where1, order1: "asc", sort1: "nama" }) });
        var result = await res.json();
        var options = '<option value=""><%=Common.getBahasaConfig("-- Pilih Sekolah --")%></option>';
        (result.data || []).forEach(function(row) { options += '<option value="' + row.id + '" ' + (selectedId == row.id ? "selected" : "") + '>' + row.nama + '</option>'; });
        elSek.innerHTML = options;
    }

    async function loadFilterSekolah<%=rnd%>() {
        var elSek = document.getElementById('filterSekolah<%=rnd%>');
        var where1 = idSekolahLogin<%=rnd%> ? "id = " + idSekolahLogin<%=rnd%> : "";
        var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "daftar", class: "ais.database.model.sekolah.Sekolah", where1: where1, order1: "asc", sort1: "nama" }) });
        var result = await res.json();
        var options = '<option value=""><%=Common.getBahasaConfig("-- Semua Sekolah --")%></option>';
        (result.data || []).forEach(function(row) { options += '<option value="' + row.id + '">' + row.nama + '</option>'; });
        elSek.innerHTML = options;
    }

    async function loadFilterYayasan<%=rnd%>() {
        var elYay = document.getElementById('filterYayasan<%=rnd%>');
        var where1 = idYayasanLogin<%=rnd%> ? "id = " + idYayasanLogin<%=rnd%> : "";
        var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "daftar", class: "ais.database.model.sekolah.Yayasan", where1: where1, order1: "asc", sort1: "nama" }) });
        var result = await res.json();
        var options = '<option value=""><%=Common.getBahasaConfig("-- Semua Yayasan --")%></option>';
        (result.data || []).forEach(function(row) { options += '<option value="' + row.id + '">' + row.nama + '</option>'; });
        elYay.innerHTML = options;
    }

    function resetAndLoadData<%=rnd%>() { currentPage<%=rnd%> = 1; loadDataOrg<%=rnd%>(); }

    async function loadDataOrg<%=rnd%>() {
        var tbody = document.getElementById('tabelDataOrg<%=rnd%>');
        var keyword = document.getElementById('searchOrg<%=rnd%>').value.trim();
        var start = (currentPage<%=rnd%> - 1);
        var payload = { action: "daftar", class: masterModelOrg<%=rnd%>, deep: "1", max: limitPerPage<%=rnd%>, halaman: start, order1: "desc", sort1: "id", count: "true" };
        var filters = [];
        if(keyword !== '') filters.push("(this_.nama ILIKE '%" + keyword + "%' OR this_.kode ILIKE '%" + keyword + "%')");
        var elSek = document.getElementById('filterSekolah<%=rnd%>');
        if(elSek && elSek.value) filters.push("this_.sekolah = " + elSek.value);
        var elYay = document.getElementById('filterYayasan<%=rnd%>');
        if(elYay && elYay.value) filters.push("this_.yayasan = " + elYay.value);
        if(idSekolahLogin<%=rnd%>) filters.push("this_.sekolah = " + idSekolahLogin<%=rnd%>);
        else if(idYayasanLogin<%=rnd%> && !filters.some(function(f){ return f.indexOf('yayasan') !== -1; })) filters.push("this_.yayasan = " + idYayasanLogin<%=rnd%>);
        if(filters.length > 0) payload.where1 = filters.join(' AND ');
        try {
            var response = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
            var result = await response.json();
            totalRecords<%=rnd%> = parseInt(result.count || 0);
            var html = '';
            (result.data || []).forEach(function(row, i) {
                var sekolahNama = row["sekolah.nama"] || '-';
                var safeNama = (row.nama || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                var collapseId = 'collapseOrg<%=rnd%>_' + row.id;
                var btnAccordion = '<button class="btn btn-sm btn-light border shadow-sm text-primary rounded-circle" type="button" data-bs-toggle="collapse" data-bs-target="#' + collapseId + '" onclick="loadAnggotaOrg<%=rnd%>(' + row.id + ')"><i class="fas fa-chevron-down"></i></button>';
                var btnEdit = '<button class="btn btn-sm btn-outline-warning me-1" onclick="editOrg<%=rnd%>(' + row.id + ')"><i class="fas fa-edit"></i></button>';
                var btnHapus = '<button class="btn btn-sm btn-outline-danger" onclick="hapusOrg<%=rnd%>(' + row.id + ')"><i class="fas fa-trash-alt"></i></button>';
                html += '<tr class="align-middle ' + (i % 2 === 0 ? 'bg-light' : '') + '">' +
                    '<td class="text-center">' + btnAccordion + '</td>' +
                    '<td class="text-center">' + (start * limitPerPage<%=rnd%> + i + 1) + '</td>' +
                    '<td><span class="badge bg-secondary">' + (row.kode || '-') + '</span></td>' +
                    '<td class="fw-bold text-primary">' + row.nama + '</td>' +
                    '<td>' + sekolahNama + '</td>' +
                    '<td class="text-center no-export">' + btnEdit + btnHapus + '</td></tr>';
                html += '<tr><td colspan="6" class="p-0 border-0">' +
                    '<div class="collapse" id="' + collapseId + '">' +
                    '<div class="card card-body m-3 bg-white border border-info shadow-sm rounded-4">' +
                    '<div class="d-flex justify-content-between align-items-center border-bottom pb-2 mb-3">' +
                    '<h6 class="fw-bold text-info mb-0"><i class="fas fa-users-cog me-2"></i><%=Common.getBahasaConfig("Keanggotaan Siswa")%>: ' + safeNama + '</h6>' +
                    '<button class="btn btn-sm btn-primary rounded-pill fw-bold shadow-sm" onclick="bukaFormTambahAnggota<%=rnd%>(' + row.id + ', \'' + safeNama + '\')"><i class="fas fa-plus-circle me-1"></i> <%=Common.getBahasaConfig("Tambah Siswa")%></button>' +
                    '</div>' +
                    '<div class="table-responsive"><table class="table table-sm table-bordered table-striped bg-white mb-0">' +
                    '<thead class="table-info text-center"><tr><th>#</th><th>NIS</th><th><%=Common.getBahasaConfig("Nama Siswa")%></th><th><%=Common.getBahasaConfig("Jabatan")%></th><th><%=Common.getBahasaConfig("Periode")%></th><th><%=Common.getBahasaConfig("Aksi")%></th></tr></thead>' +
                    '<tbody id="tabelAnggota<%=rnd%>_' + row.id + '"><tr><td colspan="6" class="text-center text-muted small py-3"><div class="spinner-border spinner-border-sm text-info me-2"></div><%=Common.getBahasaConfig("Memuat...")%></td></tr></tbody>' +
                    '</table></div></div></div></td></tr>';
            });
            tbody.innerHTML = html || '<tr><td colspan="6" class="text-center text-muted py-4"><%=Common.getBahasaConfig("Tidak ada data Organisasi.")%></td></tr>';
            updatePaginationUI<%=rnd%>();
        } catch(e) { console.error(e); }
    }

    async function loadAnggotaOrg<%=rnd%>(idOrg) {
        var tbody = document.getElementById('tabelAnggota<%=rnd%>_' + idOrg);
        try {
            var response = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "daftar", class: detailModelOrg<%=rnd%>, deep: "1", where1: "organisasiSiswa = " + idOrg, order1: "asc", sort1: "id" }) });
            var result = await response.json();
            var html = '';
            (result.data || []).forEach(function(row, i) {
                var btnHapus = '<button class="btn btn-sm btn-outline-danger px-2 py-0" onclick="hapusAnggota<%=rnd%>(' + row.id + ', ' + idOrg + ')"><i class="fas fa-times"></i></button>';
                html += '<tr>' +
                    '<td class="text-center">' + (i + 1) + '</td>' +
                    '<td class="text-center">' + (row["siswa.nis"] || row["siswa.nim"] || '-') + '</td>' +
                    '<td class="fw-semibold">' + (row["siswa.nama"] || '-') + '</td>' +
                    '<td class="text-center">' + (row.jabatan || '-') + '</td>' +
                    '<td class="text-center">' + (row.periode || '-') + '</td>' +
                    '<td class="text-center">' + btnHapus + '</td></tr>';
            });
            tbody.innerHTML = html || '<tr><td colspan="6" class="text-center text-muted small py-3 fst-italic"><%=Common.getBahasaConfig("Belum ada siswa terdaftar.")%></td></tr>';
        } catch(e) { tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger small py-3"><%=Common.getBahasaConfig("Gagal memuat data.")%></td></tr>'; }
    }

    function bukaFormTambahAnggota<%=rnd%>(idOrg, namaOrg) {
        document.getElementById('formAnggota<%=rnd%>').reset();
        document.getElementById('inputIdAnggota<%=rnd%>').value = '';
        document.getElementById('inputIdOrgAnggota<%=rnd%>').value = idOrg;
        document.getElementById('lblNamaOrgAnggota<%=rnd%>').innerText = namaOrg;
        document.getElementById('hiddenIdSiswaAnggota<%=rnd%>').value = '';
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewFormAnggota<%=rnd%>').style.display = 'block';
    }

    function tutupFormAnggota<%=rnd%>() {
        document.getElementById('viewFormAnggota<%=rnd%>').style.display = 'none';
        document.getElementById('viewList<%=rnd%>').style.display = 'block';
    }

    async function onInputSiswaAnggotaSearch<%=rnd%>(val) {
        if(val.length < 2) return;
        var where1 = "(nis ILIKE '%" + val + "%' OR nim ILIKE '%" + val + "%' OR nama ILIKE '%" + val + "%')";
        if(idSekolahLogin<%=rnd%>) where1 += " AND sekolah = " + idSekolahLogin<%=rnd%>;
        var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "daftar", class: "ais.database.model.sekolah.Siswa", where1: where1, max: 20 }) });
        var result = await res.json();
        var options = '';
        (result.data || []).forEach(function(row) { options += '<option data-id="' + row.id + '" value="' + (row.nis || row.nim || '-') + ' - ' + row.nama + '"></option>'; });
        document.getElementById('dlSiswaAnggota<%=rnd%>').innerHTML = options;
    }

    function checkSelectedSiswaAnggota<%=rnd%>(val) {
        var options = document.getElementById('dlSiswaAnggota<%=rnd%>').options;
        for(var i=0; i<options.length; i++) {
            if(options[i].value === val) { document.getElementById('hiddenIdSiswaAnggota<%=rnd%>').value = options[i].getAttribute('data-id'); return; }
        }
        document.getElementById('hiddenIdSiswaAnggota<%=rnd%>').value = '';
    }

    async function simpanAnggota<%=rnd%>() {
        var idSiswa = document.getElementById('hiddenIdSiswaAnggota<%=rnd%>').value;
        var idOrg = document.getElementById('inputIdOrgAnggota<%=rnd%>').value;
        if(!idSiswa) { showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Pilih Siswa dari daftar pencarian.")%>', "bg-warning text-dark"); return; }
        var dataObj = {
            organisasiSiswa: parseInt(idOrg),
            siswa: parseInt(idSiswa),
            jabatan: document.getElementById('inputJabatanAnggota<%=rnd%>').value.trim() || null,
            periode: document.getElementById('inputPeriodeAnggota<%=rnd%>').value.trim() || null
        };
        var btn = document.getElementById('btnSimpanAnggota<%=rnd%>');
        btn.disabled = true;
        try {
            var response = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "simpanDataRinci", log: "true", class: detailModelOrg<%=rnd%>, data: dataObj }) });
            var result = await response.json();
            if(result.status === '00' || result.status === 'success') {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Siswa berhasil ditambahkan!")%>', "bg-success text-white");
                tutupFormAnggota<%=rnd%>();
                document.getElementById('collapseOrg<%=rnd%>_' + idOrg).classList.add('show');
                loadAnggotaOrg<%=rnd%>(idOrg);
            } else { showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan.")%>', "bg-danger text-white"); }
        } catch(e) { console.error(e); } finally { btn.disabled = false; }
    }

    async function hapusAnggota<%=rnd%>(idAnggota, idOrg) {
        if(!confirm('<%=Common.getBahasaConfigJS("Keluarkan siswa ini dari organisasi?")%>')) return;
        var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "hapusDataRinci", class: detailModelOrg<%=rnd%>, id: idAnggota }) });
        var result = await res.json();
        if(result.status === '00' || result.status === 'success') { showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Siswa dikeluarkan.")%>', "bg-success text-white"); loadAnggotaOrg<%=rnd%>(idOrg); }
    }

    async function simpanOrg<%=rnd%>() {
        var currentId = document.getElementById('inputIdOrg<%=rnd%>').value;
        var dataObj = {
            id: currentId ? parseInt(currentId) : null,
            nama: document.getElementById('inputNama<%=rnd%>').value.trim(),
            kode: document.getElementById('inputKode<%=rnd%>').value.trim() || null,
            keterangan: document.getElementById('inputKeterangan<%=rnd%>').value.trim() || null
        };
        var elYay = document.getElementById('selectYayasan<%=rnd%>');
        if(elYay && elYay.value) dataObj.yayasan = parseInt(elYay.value);
        var elSek = document.getElementById('selectSekolah<%=rnd%>');
        if(elSek && elSek.value) dataObj.sekolah = parseInt(elSek.value);
        var btn = document.getElementById('btnSimpanOrg<%=rnd%>');
        btn.disabled = true;
        try {
            var response = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "simpanDataRinci", log: "true", class: masterModelOrg<%=rnd%>, data: dataObj }) });
            var result = await response.json();
            if(result.status === '00' || result.status === 'success') {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data berhasil disimpan!")%>', "bg-success text-white");
                tutupForm<%=rnd%>();
                loadDataOrg<%=rnd%>();
            } else { showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan.")%>', "bg-danger text-white"); }
        } catch(e) { console.error(e); } finally { btn.disabled = false; }
    }

    async function editOrg<%=rnd%>(id) {
        bukaFormTambah<%=rnd%>();
        document.getElementById('inputIdOrg<%=rnd%>').value = id;
        var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "load", class: masterModelOrg<%=rnd%>, id: id }) });
        var result = await res.json();
        var data = result.data;
        document.getElementById('inputNama<%=rnd%>').value = data.nama || '';
        document.getElementById('inputKode<%=rnd%>').value = data.kode || '';
        document.getElementById('inputKeterangan<%=rnd%>').value = data.keterangan || '';
        await loadYayasan<%=rnd%>('selectYayasan<%=rnd%>', data["yayasan.id"]);
        await loadSekolah<%=rnd%>(data["yayasan.id"], data["sekolah.id"]);
    }

    async function hapusOrg<%=rnd%>(id) {
        if(!confirm('<%=Common.getBahasaConfigJS("Hapus data organisasi ini secara permanen?")%>')) return;
        var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "hapusDataRinci", class: masterModelOrg<%=rnd%>, id: id }) });
        var result = await res.json();
        if(result.status === '00' || result.status === 'success') { showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data dihapus.")%>', "bg-success text-white"); loadDataOrg<%=rnd%>(); }
    }

    async function bukaFormTambah<%=rnd%>() {
        document.getElementById('formOrg<%=rnd%>').reset();
        document.getElementById('inputIdOrg<%=rnd%>').value = '';
        await loadYayasan<%=rnd%>('selectYayasan<%=rnd%>', idYayasanLogin<%=rnd%> || null);
        var elYay = document.getElementById('selectYayasan<%=rnd%>');
        if(idYayasanLogin<%=rnd%>) {
            elYay.disabled = true;
            await loadSekolah<%=rnd%>(idYayasanLogin<%=rnd%>, idSekolahLogin<%=rnd%> || null);
            if(idSekolahLogin<%=rnd%>) document.getElementById('selectSekolah<%=rnd%>').disabled = true;
        } else {
            elYay.disabled = false;
        }
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewFormAnggota<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    }

    function tutupForm<%=rnd%>() { document.getElementById('viewForm<%=rnd%>').style.display = 'none'; document.getElementById('viewList<%=rnd%>').style.display = 'block'; }

    function updatePaginationUI<%=rnd%>() {
        var totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumOrg<%=rnd%>').innerText = currentPage<%=rnd%> + ' / ' + totalPages;
        document.getElementById('pagingInfoOrg<%=rnd%>').innerText = 'Total: ' + totalRecords<%=rnd%> + ' data';
    }

    function changePage<%=rnd%>(dir) {
        var totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if(dir === -1 && currentPage<%=rnd%> > 1) { currentPage<%=rnd%>--; loadDataOrg<%=rnd%>(); }
        else if(dir === 1 && currentPage<%=rnd%> < totalPages) { currentPage<%=rnd%>++; loadDataOrg<%=rnd%>(); }
    }

    document.addEventListener("DOMContentLoaded", function() {
        loadFilterSekolah<%=rnd%>();
        loadFilterYayasan<%=rnd%>();
        document.getElementById('searchOrg<%=rnd%>').addEventListener("keydown", function(e) { if(e.key === "Enter") { e.preventDefault(); resetAndLoadData<%=rnd%>(); } });
        loadDataOrg<%=rnd%>();
    });
</script>
