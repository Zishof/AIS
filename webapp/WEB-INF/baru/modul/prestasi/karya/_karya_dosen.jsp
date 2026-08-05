<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="ais.database.model.PenghargaanDosen"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.KategoriPenghargaan"%>
<%
// 1. SECURITY CHECK
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	out.print("{\"status\":\"error\", \"message\":\""
	+ Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
	return;
}
String rnd = Common.getGeneratedBarCode(7);

// Cek hak akses unit
Fakultas loginSebagaiFakultas = tbmuser.getFakultas();
Jurusan loginSebagaiJurusan = tbmuser.getJurusan();
Dosen loginSebagaiDosen = tbmuser.getDosen();
Long idDosenLogin = (loginSebagaiDosen != null) ? loginSebagaiDosen.getId() : null;

Long idFakLogin = (loginSebagaiJurusan != null && loginSebagaiJurusan.getFakultas() != null) ? loginSebagaiJurusan.getFakultas().getId() : (loginSebagaiFakultas != null ? loginSebagaiFakultas.getId() : null);
Long idJurLogin = (loginSebagaiJurusan != null) ? loginSebagaiJurusan.getId() : null;

String selectedTa = Common.getCurrentTahunAkademik();
String selectedSmt = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

// PRIVILEGES CHECK (RBAC)
boolean edit = false;
boolean add = false;
boolean delete = false;
boolean isAdmin = false;

if(tbmuser != null && tbmuser.getUserId() != null){
    edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser);
    delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE, tbmuser);
    add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE, tbmuser);
    
    if(Common.getApakahAdminLain(tbmuser)){
        edit = true;
        delete = true;
        add = true;
        isAdmin = true;
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
                                <i class="fas fa-award text-primary me-2"></i><%=Common.getBahasaConfig("Manajemen Karya & Penghargaan Dosen")%>
                            </h5>
                            <small class="text-muted"><%=Common.getBahasaConfig("Kelola data pencapaian, publikasi, dan penghargaan dosen.")%></small>
                        </div>
                        
                        <div class="d-flex align-items-center gap-2 flex-wrap">
                            <div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;">
                                <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>
                                <input type="text" class="form-control border-start-0 ps-0 bg-white fw-medium" id="searchPenghargaan<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Karya, NIDN / Nama...")%>">
                            </div>
                            
                            <button class="btn btn-sm btn-outline-info rounded-pill px-3 shadow-sm fw-bold" type="button" data-bs-toggle="collapse" data-bs-target="#filterPanel<%=rnd%>">
                                <i class="fas fa-filter me-1"></i><%=Common.getBahasaConfig("Filter")%>
                            </button>

                            <button class="btn btn-sm btn-outline-secondary rounded-pill px-3 shadow-sm fw-bold" onclick="resetAndLoadData<%=rnd%>()">
                                <i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Segarkan")%>
                            </button>
                            
                            <% if (add) { %>
                            <button class="btn btn-sm btn-primary rounded-pill px-4 shadow-sm fw-bold" onclick="bukaFormTambah<%=rnd%>()">
                                <i class="fas fa-plus-circle me-1"></i><%=Common.getBahasaConfig("Tambah Data")%>
                            </button>
                            <% } %>
                        </div>
                    </div>

                    <div class="collapse mt-3" id="filterPanel<%=rnd%>">
                        <div class="card card-body bg-light border-0 shadow-sm rounded-3 p-3">
                            <div class="row g-3">
                                <div class="col-md-3">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Tahun Akademik")%></label>
                                    <select id="filterTahunAkademik<%=rnd%>" class="form-select form-select-sm shadow-sm">
                                        <option value=""><%=Common.getBahasaConfig("-- Semua TA --")%></option>
                                        <% if (Common.tahunAngkatans != null) { 
                                            for (String ta : Common.tahunAngkatans) { %>
                                                <option value="<%= ta %>"><%= ta %></option>
                                        <%  } 
                                           } %>
                                    </select>
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Semester")%></label>
                                    <select id="filterSemester<%=rnd%>" class="form-select form-select-sm shadow-sm">
                                        <option value=""><%=Common.getBahasaConfig("-- Semua SMT --")%></option>
                                        <option value="<%= Perkuliahan.GANJIL %>">Ganjil</option>
                                        <option value="<%= Perkuliahan.GENAP %>">Genap</option>
                                    </select>
                                </div>
                                <% if (idDosenLogin == null) { %>
                                <div class="col-md-3">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Fakultas")%></label>
                                    <select id="filterFakultas<%=rnd%>" class="form-select form-select-sm shadow-sm" onchange="loadFilterJurusan<%=rnd%>(this.value)">
                                        <option value=""><%=Common.getBahasaConfig("-- Semua Fakultas --")%></option>
                                    </select>
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Jurusan")%></label>
                                    <select id="filterJurusan<%=rnd%>" class="form-select form-select-sm shadow-sm">
                                        <option value=""><%=Common.getBahasaConfig("-- Semua Jurusan --")%></option>
                                    </select>
                                </div>
                                <% } %>
                                <div class="col-12 text-end">
                                    <button type="button" class="btn btn-sm btn-info text-white px-4 fw-bold shadow-sm rounded-pill" onclick="resetAndLoadData<%=rnd%>()">
                                        <i class="fas fa-search me-1"></i> <%=Common.getBahasaConfig("Terapkan Filter")%>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                
                <div class="card-body p-4 pt-3">
                    <div class="table-responsive border rounded-3 shadow-sm bg-white">
                        <table class="table table-hover table-striped align-middle mb-0" id="tabelHTMLDataPenghargaan<%=rnd%>">
                            <thead class="table-dark text-center align-middle">
                                <tr>
                                    <th style="width: 60px;">#</th>
                                    <th class="text-start"><i class="fas fa-user-tie me-1"></i><%=Common.getBahasaConfig("Dosen")%></th>
                                    <th class="text-start"><i class="fas fa-medal me-1"></i><%=Common.getBahasaConfig("Karya / Penghargaan")%></th>
                                    <th class="text-start"><i class="fas fa-tags me-1"></i><%=Common.getBahasaConfig("Kategori")%></th>
                                    <th class="text-center"><i class="fas fa-calendar-alt me-1"></i><%=Common.getBahasaConfig("Tanggal")%></th>
                                    <th class="text-center"><i class="fas fa-info-circle me-1"></i><%=Common.getBahasaConfig("Status")%></th>
                                    <th class="text-center no-export" style="width: 120px;"><i class="fas fa-cogs"></i></th>
                                </tr>
                            </thead>
                            <tbody id="tabelDataPenghargaan<%=rnd%>">
                                <tr><td colspan="7" class="text-center py-5"><div class="spinner-border text-primary"></div></td></tr>
                            </tbody>
                        </table>
                    </div>
                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mt-4">
                        <div class="small fw-medium text-muted bg-light px-3 py-1 rounded-pill border" id="pagingInfoPenghargaan<%=rnd%>"></div>
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
                        <i class="fas fa-edit text-success me-2"></i><%=Common.getBahasaConfig("Formulir Karya Dosen")%>
                    </h5>
                </div>
                <div class="card-body p-4">
                    <form id="formPenghargaan<%=rnd%>" onsubmit="event.preventDefault(); simpanPenghargaan<%=rnd%>();">
                        <input type="hidden" id="inputIdPenghargaan<%=rnd%>" value="">
                        
                        <div class="row g-4 mt-1">
                            <div class="col-md-12">
                                <h6 class="fw-bold text-primary border-bottom pb-2"><i class="fas fa-user-tie me-2"></i>1. <%=Common.getBahasaConfig("Data Dosen & Akademik")%></h6>
                            </div>
                            
                            <% if (idDosenLogin == null) { %>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Cari Dosen (NIDN / Nama)")%> <span class="text-danger">*</span></label>
                                <input type="text" class="form-control fw-bold shadow-sm" id="inputCariDosen<%=rnd%>" list="dlDosen<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik NIDN atau Nama...")%>" oninput="onInputDosenSearch<%=rnd%>(this.value)" onchange="checkSelectedDosen<%=rnd%>(this.value)" autocomplete="off" required>
                                <datalist id="dlDosen<%=rnd%>"></datalist>
                                <input type="hidden" id="hiddenIdDosen<%=rnd%>" required>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Fakultas")%></label>
                                <select class="form-select shadow-sm" id="selectFakultas<%=rnd%>" onchange="loadJurusan<%=rnd%>(this.value, null)">
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Fakultas --")%></option>
                                </select>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jurusan")%></label>
                                <select class="form-select shadow-sm" id="selectJurusan<%=rnd%>">
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Jurusan --")%></option>
                                </select>
                            </div>
                            <% } else { %>
                                <input type="hidden" id="hiddenIdDosen<%=rnd%>" value="<%=idDosenLogin%>">
                            <% } %>

                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tahun Akademik")%> <span class="text-danger">*</span></label>
                                <select class="form-select shadow-sm" id="inputTahunAkademik<%=rnd%>" onchange="autoFillTahun<%=rnd%>(this.value)" required>
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Tahun Akademik --")%></option>
                                    <% if (Common.tahunAngkatans != null) { 
                                        for (String ta : Common.tahunAngkatans) { %>
                                            <option value="<%= ta %>"><%= ta %></option>
                                    <%  } 
                                       } %>
                                </select>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Semester")%> <span class="text-danger">*</span></label>
                                <select class="form-select shadow-sm" id="inputJenisSemester<%=rnd%>" required>
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Semester --")%></option>
                                    <option value="<%= Perkuliahan.GANJIL %>"><%=Common.getBahasaConfig("Ganjil")%></option>
                                    <option value="<%= Perkuliahan.GENAP %>"><%=Common.getBahasaConfig("Genap")%></option>
                                </select>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tahun")%></label>
                                <input type="number" class="form-control shadow-sm" id="inputTahun<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cth: 2025")%>">
                            </div>

                            <div class="col-md-12">
                                <h6 class="fw-bold text-primary border-bottom pb-2"><i class="fas fa-award me-2"></i>2. <%=Common.getBahasaConfig("Detail Karya / Penghargaan")%></h6>
                            </div>
                            
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Karya / Penghargaan")%> <span class="text-danger">*</span></label>
                                <input type="text" class="form-control fw-bold shadow-sm" id="inputNama<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cth: Peneliti Terbaik")%>" required>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Karya (Inggris - Opsional)")%></label>
                                <input type="text" class="form-control shadow-sm" id="inputNamaEn<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cth: Best Researcher")%>">
                            </div>

                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal Pelaksanaan")%></label>
                                <input type="date" class="form-control shadow-sm" id="inputTanggal<%=rnd%>">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal Selesai")%></label>
                                <input type="date" class="form-control shadow-sm" id="inputTanggalSelesai<%=rnd%>">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Kategori")%></label>
                                <select class="form-select shadow-sm" id="selectKategori<%=rnd%>">
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Kategori --")%></option>
                                </select>
                            </div>
                            
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Capaian")%></label>
                                <input type="text" class="form-control shadow-sm" id="inputCapaian<%=rnd%>">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("No. Sertifikat")%></label>
                                <input type="text" class="form-control shadow-sm" id="inputNomorSertifikat<%=rnd%>">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Status Persetujuan")%></label>
                                <select class="form-select shadow-sm fw-bold" id="inputStatus<%=rnd%>">
                                    <option value="Belum diproses">Belum diproses</option>
                                    <option value="Sedang diproses">Sedang diproses</option>
                                    <option value="Disetujui">Disetujui</option>
                                    <option value="Ditolak">Ditolak</option>
                                </select>
                            </div>

                            <div class="col-md-12">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("URL / Link Karya")%></label>
                                <input type="url" class="form-control shadow-sm" id="inputUrl<%=rnd%>" placeholder="<%=Common.getBahasaConfig("https://...")%>">
                            </div>

                            <div class="col-md-12">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Keterangan Tambahan")%></label>
                                <textarea class="form-control shadow-sm" id="inputKeterangan<%=rnd%>" rows="2"></textarea>
                            </div>

                            <div class="col-md-12 mt-4 pt-3 border-top text-center" id="uploadPhotoArea<%=rnd%>" style="display: none;">
                                <div class="p-3 bg-light rounded-3 border">
                                    <label class="form-label fw-bold text-secondary d-block"><i class="fas fa-file-pdf text-danger me-2"></i><%=Common.getBahasaConfig("Bukti / Lampiran")%></label>
                                    <div class="d-flex justify-content-center align-items-center gap-2 mt-2" id="action-buttons-<%=rnd%>"></div>
                                    <div id="preview-area-<%=rnd%>" class="mt-3"></div>
                                </div>
                            </div>

                            <div class="col-12 mt-5 text-end border-top pt-3">
                                <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold border" onclick="tutupForm<%=rnd%>()"><%=Common.getBahasaConfig("Batal")%></button>
                                <% if (add || edit) { %>
                                <button type="submit" class="btn btn-success px-4 rounded-pill shadow-sm fw-bold" id="btnSimpan<%=rnd%>">
                                    <i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Data")%>
                                </button>
                                <% } %>
                            </div>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    const masterModelClass<%=rnd%> = "ais.database.model.PenghargaanDosen";
    let activeEditingPenghargaanDosenId<%=rnd%> = "";
    let isDocumentUploaded<%=rnd%> = false;
    let currentPage<%=rnd%> = 1;
    const limitPerPage<%=rnd%> = 10;
    let totalRecords<%=rnd%> = 0;

    const showToast<%=rnd%> = (msg, colorClass) => {
        if(typeof tampilkanToast === "function") tampilkanToast(msg, colorClass);
        else alert(msg);
    };

    const autoFillTahun<%=rnd%> = (taValue) => {
        const inputTahun = document.getElementById('inputTahun<%=rnd%>');
        if (taValue && taValue.includes('/')) {
            inputTahun.value = taValue.split('/')[0];
        } else if (!taValue) {
            inputTahun.value = '';
        }
    };

    // ==========================================
    // FUNGSI UPLOAD LAMPIRAN
    // ==========================================
    async function checkExistingFile<%=rnd%>(idPenghargaan, isApproved) {
        var reqObj = { 
            "action": "file", 
            "class": "<%=ais.database.model.file.LampiranLain.class.getName()%>", 
            "ref": idPenghargaan.toString(), 
            "jenis": masterModelClass<%=rnd%>,
            "kondisiTambahan": "",
            "refresh": "false",
            "usingId": false
        };
        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(reqObj)
            });
            const dataResponse = await response.json();
            if(dataResponse.status == '00' && dataResponse.data){ 
                isDocumentUploaded<%=rnd%> = true;
                generatePreview<%=rnd%>(dataResponse.data, isApproved);
            } else {
                isDocumentUploaded<%=rnd%> = false;
                document.getElementById('preview-area-<%=rnd%>').innerHTML = '';
                resetUploadButtons<%=rnd%>(isApproved);
            }
        } catch (error) { console.error("Error file:", error); }
    }

    function generatePreview<%=rnd%>(data, isApproved) {
        var url = data.url;
        var filename = data.nama;
        var ext = filename ? filename.split('.').pop().toLowerCase() : '';

        var btnDownload = '<a href="' + url + '" target="_blank" class="btn btn-success fw-bold px-3 shadow-sm"><i class="fas fa-download me-1"></i> <%=Common.getBahasaConfig("Download")%></a>';
        var btnDelete = '';
        var btnUploadHtml = '';
        
        if (!isApproved) {
            <% if (delete) { %>
            btnDelete = '<button type="button" class="btn btn-danger fw-bold px-3 shadow-sm ms-2" onclick="hapusFileLain<%=rnd%>(' + data.id + ')"><i class="fas fa-trash-alt me-1"></i> <%=Common.getBahasaConfig("Hapus")%></button>';
            <% } %>
            <% if (edit || add) { %>
            btnUploadHtml = '<input type="file" id="input_dofile_foto_<%=rnd%>" class="d-none" accept=".pdf, image/*" onchange="uploadFile<%=rnd%>()">' +
                            '<button type="button" class="btn btn-primary fw-bold shadow-sm px-4 me-2" onclick="document.getElementById(\'input_dofile_foto_<%=rnd%>\').click();"><i class="fas fa-sync-alt me-2"></i><%=Common.getBahasaConfig("Ganti Bukti")%></button>';
            <% } %>
        }

        document.getElementById('action-buttons-<%=rnd%>').innerHTML = btnUploadHtml + btnDownload + btnDelete;
        
        let html = '';
        if (['jpg', 'jpeg', 'png'].includes(ext)) {
            html = '<img src="' + url + '" class="img-fluid rounded shadow-sm border" style="max-height: 200px;">';
        } else {
            html = '<a href="' + url + '" target="_blank" title="Buka PDF"><i class="fas fa-file-pdf fa-4x text-danger"></i></a>';
        }
        document.getElementById('preview-area-<%=rnd%>').innerHTML = html;
    }

    function resetUploadButtons<%=rnd%>(isApproved) {
        if (!isApproved) {
            <% if (edit || add) { %>
            document.getElementById('action-buttons-<%=rnd%>').innerHTML = 
                '<input type="file" id="input_dofile_foto_<%=rnd%>" class="d-none" accept=".pdf, image/*" onchange="uploadFile<%=rnd%>()">' +
                '<button type="button" class="btn btn-primary fw-bold shadow-sm px-4" onclick="document.getElementById(\'input_dofile_foto_<%=rnd%>\').click();"><i class="fas fa-upload me-2"></i><%=Common.getBahasaConfig("Unggah Bukti")%></button>';
            <% } else { %>
                document.getElementById('action-buttons-<%=rnd%>').innerHTML = '<span class="text-muted"><%=Common.getBahasaConfig("Tidak ada akses unggah.")%></span>';
            <% } %>
        } else {
            document.getElementById('action-buttons-<%=rnd%>').innerHTML = '<span class="text-danger fw-bold"><i class="fas fa-ban me-1"></i><%=Common.getBahasaConfig("Terkunci (Disetujui)")%></span>';
        }
    }

    const uploadFile<%=rnd%> = async () => {
        const fileInput = document.getElementById('input_dofile_foto_<%=rnd%>');
        if (fileInput.files.length === 0) return;
        if (!activeEditingPenghargaanDosenId<%=rnd%>) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Harap simpan data terlebih dahulu!")%>', "bg-warning text-dark");
            return;
        }

        const file = fileInput.files[0];
        const formData = new FormData();
        formData.append('nama', file.name);
        formData.append('clazz', '<%=ais.database.model.file.LampiranLain.class.getName()%>');
        formData.append('jenis', masterModelClass<%=rnd%>);
        formData.append('id', activeEditingPenghargaanDosenId<%=rnd%>);
        formData.append('tanpaLogin', 'true');
        formData.append('fileContent', file);

        const xhr = new XMLHttpRequest();
        xhr.open('POST', '<%=Common.ROOT%>/DoUpload', true);
        
        let btnUpload = document.getElementById('action-buttons-<%=rnd%>');
        let oriBtn = btnUpload.innerHTML;
        btnUpload.innerHTML = '<span class="spinner-border text-primary spinner-border-sm me-2"></span> <%=Common.getBahasaConfig("Mengunggah...")%>';

        xhr.onload = function () {
            try {
                const res = JSON.parse(xhr.responseText);
                if (res.status === 'success' || res.status === '00' || res.status === 'Sukses' || res.keterangan === 'Sukses') {
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Berkas berhasil diunggah!")%>', "bg-success text-white");
                    checkExistingFile<%=rnd%>(activeEditingPenghargaanDosenId<%=rnd%>, false);
                    loadDataPenghargaan<%=rnd%>();
                } else {
                    showToast<%=rnd%>(res.message || res.keterangan || '<%=Common.getBahasaConfigJS("Gagal mengunggah berkas")%>', "bg-danger text-white");
                    btnUpload.innerHTML = oriBtn;
                }
            } catch (e) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal: Respon server tidak valid.")%>', "bg-danger text-white");
                btnUpload.innerHTML = oriBtn;
            }
            fileInput.value = ''; 
        };
        xhr.onerror = function() {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Koneksi bermasalah.")%>', "bg-danger text-white");
            btnUpload.innerHTML = oriBtn;
        };
        xhr.send(formData);
    };

    const hapusFileLain<%=rnd%> = async (idLampiran) => {
        if (!confirm('<%=Common.getBahasaConfigJS("Hapus dokumen lampiran ini?")%>')) return;
        const payload = { action: "hapusDataRinci", class: "<%=ais.database.model.file.LampiranLain.class.getName()%>", id: idLampiran };
        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
            });
            const result = await response.json();
            if (result.status === '00' || result.status === 'success') {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("File dihapus!")%>', "bg-success text-white");
                isDocumentUploaded<%=rnd%> = false;
                document.getElementById('preview-area-<%=rnd%>').innerHTML = '';
                resetUploadButtons<%=rnd%>(false);
                loadDataPenghargaan<%=rnd%>();
            }
        } catch (error) { showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal koneksi")%>', "bg-danger text-white"); }
    };

    // ==========================================
    // LOAD MASTERS
    // ==========================================
    const loadFakultas<%=rnd%> = async (targetId, selectedId) => {
        const res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: "daftar", class: "<%=Fakultas.class.getName()%>", where1: "aktif = true", order1: "asc", sort1: "nama" })
        });
        const result = await res.json();
        const data = result.data || [];
        let options = '<option value=""><%=Common.getBahasaConfig("-- Pilih Fakultas --")%></option>';
        data.forEach(row => {
            const selected = (selectedId == row.id) ? "selected" : "";
            options += '<option value="' + row.id + '" ' + selected + '>' + row.nama + '</option>';
        });
        document.getElementById(targetId).innerHTML = options;
    };

    const loadJurusan<%=rnd%> = async (idFakultas, selectedId) => {
        if(!idFakultas) {
            document.getElementById('selectJurusan<%=rnd%>').innerHTML = '<option value=""><%=Common.getBahasaConfig("-- Pilih Jurusan --")%></option>';
            return;
        }
        const res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: "daftar", class: "<%=Jurusan.class.getName()%>", where1: "fakultas = " + idFakultas + " AND aktif = true", order1: "asc", sort1: "nama" })
        });
        const result = await res.json();
        const data = result.data || [];
        let options = '<option value=""><%=Common.getBahasaConfig("-- Pilih Jurusan --")%></option>';
        data.forEach(row => {
            const selected = (selectedId == row.id) ? "selected" : "";
            options += '<option value="' + row.id + '" ' + selected + '>' + row.nama + '</option>';
        });
        document.getElementById('selectJurusan<%=rnd%>').innerHTML = options;
    };

    const loadKategori<%=rnd%> = async (targetId, selectedId) => {
        const res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: "daftar", class: "<%=KategoriPenghargaan.class.getName()%>", order1: "asc", sort1: "nama" })
        });
        const result = await res.json();
        const data = result.data || [];
        let options = '<option value=""><%=Common.getBahasaConfig("-- Pilih Kategori --")%></option>';
        data.forEach(row => {
            const selected = (selectedId == row.id) ? "selected" : "";
            options += '<option value="' + row.id + '" ' + selected + '>' + row.nama + '</option>';
        });
        document.getElementById(targetId).innerHTML = options;
    };

    const loadFilterJurusan<%=rnd%> = (val) => {
        loadJurusan<%=rnd%>(val, null);
    };

    // SEARCH DOSEN
    const onInputDosenSearch<%=rnd%> = async (val) => {
        if (val.length < 2) return;
        const res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: "daftar", class: "ais.database.model.Dosen", where1: "aktif = true AND (nidn ILIKE '%" + val + "%' OR nama ILIKE '%" + val + "%')", max: 20 })
        });
        const result = await res.json();
        const data = result.data || [];
        let options = '';
        data.forEach(row => {
            options += '<option data-id="' + row.id + '" value="' + (row.nidn || '-') + ' - ' + row.nama + '"></option>';
        });
        document.getElementById('dlDosen<%=rnd%>').innerHTML = options;
    };

    const checkSelectedDosen<%=rnd%> = (val) => {
        const dl = document.getElementById('dlDosen<%=rnd%>');
        const options = dl.options;
        for(let i=0; i<options.length; i++) {
            if(options[i].value === val) {
                document.getElementById('hiddenIdDosen<%=rnd%>').value = options[i].getAttribute('data-id');
                return;
            }
        }
        document.getElementById('hiddenIdDosen<%=rnd%>').value = '';
    };

    // CRUD LOGIC
    const resetAndLoadData<%=rnd%> = () => {
        currentPage<%=rnd%> = 1;
        loadDataPenghargaan<%=rnd%>();
    };

    const loadDataPenghargaan<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataPenghargaan<%=rnd%>');
        const keyword = document.getElementById('searchPenghargaan<%=rnd%>').value.trim();
        const start = (currentPage<%=rnd%> - 1);
        
        const payload = {
            action: "daftar", class: masterModelClass<%=rnd%>, deep: "1", max: limitPerPage<%=rnd%>, halaman: start,
            order1: "desc", sort1: "id", count: "true"
        };
        
        let filters = [];
        if(keyword !== '') filters.push("(this_.nama ILIKE '%" + keyword + "%' OR dosen1_.nama ILIKE '%" + keyword + "%' OR dosen1_.nidn ILIKE '%" + keyword + "%')");
        
        const fTa = document.getElementById('filterTahunAkademik<%=rnd%>').value;
        if(fTa) filters.push("this_.tahunAkademik = '" + fTa + "'");
        
        if(filters.length > 0) {
            payload.where1 = filters.join(' AND ');
            payload.alias1 = "dosen";
        }

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const result = await response.json();
            totalRecords<%=rnd%> = parseInt(result.count || 0);
            const list = result.data || [];
            
            let html = '';
            if(list.length === 0) {
                html = '<tr><td colspan="7" class="text-center text-muted py-4"><%=Common.getBahasaConfig("Tidak ada data ditemukan.")%></td></tr>';
            } else {
                let no = (start * limitPerPage<%=rnd%>) + 1;
                list.forEach(row => {
                    let badgeColor = 'secondary';
                    if(row.status === 'Disetujui') badgeColor = 'success';
                    else if(row.status === 'Ditolak') badgeColor = 'danger';
                    
                    const statusBadge = '<span class="badge bg-' + badgeColor + '">' + (row.status || 'Belum diproses') + '</span>';
                    
                    html += '<tr>' +
                        '<td class="text-center">' + (no++) + '</td>' +
                        '<td><div class="fw-bold">' + (row["dosen.nama"] || '-') + '</div><small class="text-muted">' + (row["dosen.nidn"] || '-') + '</small></td>' +
                        '<td>' + (row.nama || '-') + '</td>' +
                        '<td>' + (row["kategoriPenghargaan.nama"] || '-') + '</td>' +
                        '<td class="text-center">' + (row["tanggal.formated2"] || '-') + '</td>' +
                        '<td class="text-center">' + statusBadge + '</td>' +
                        '<td class="text-center no-export">' +
                            '<button class="btn btn-sm btn-outline-warning shadow-sm me-1" onclick="editPenghargaan<%=rnd%>(' + row.id + ')"><i class="fas fa-edit"></i></button>' +
                            '<button class="btn btn-sm btn-outline-danger shadow-sm" onclick="hapusPenghargaan<%=rnd%>(' + row.id + ')"><i class="fas fa-trash-alt"></i></button>' +
                        '</td>' +
                    '</tr>';
                });
            }
            tbody.innerHTML = html;
            updatePaginationUI<%=rnd%>();
        } catch (e) { console.error(e); }
    };

    const simpanPenghargaan<%=rnd%> = async () => {
        const idDosen = document.getElementById('hiddenIdDosen<%=rnd%>').value;
        const currentId = document.getElementById('inputIdPenghargaan<%=rnd%>').value;
        const isNewData = (!currentId || currentId === '');

        if (!idDosen) {
             showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Harap isi dan pilih Dosen dari pencarian.")%>', "bg-warning text-dark");
             return;
        }

        const dataObj = {
            id: isNewData ? null : parseInt(currentId),
            dosen: parseInt(idDosen),
            nama: document.getElementById('inputNama<%=rnd%>').value.trim(),
            namaEn: document.getElementById('inputNamaEn<%=rnd%>').value.trim() || null,
            tanggal: document.getElementById('inputTanggal<%=rnd%>').value || null,
            tanggalSelesai: document.getElementById('inputTanggalSelesai<%=rnd%>').value || null,
            tahun: document.getElementById('inputTahun<%=rnd%>').value ? parseInt(document.getElementById('inputTahun<%=rnd%>').value) : null,
            nomorSertifikat: document.getElementById('inputNomorSertifikat<%=rnd%>').value.trim() || null,
            capaian: document.getElementById('inputCapaian<%=rnd%>').value.trim() || null,
            keterangan: document.getElementById('inputKeterangan<%=rnd%>').value.trim() || null,
            tahunAkademik: document.getElementById('inputTahunAkademik<%=rnd%>').value || null,
            jenisSemester: document.getElementById('inputJenisSemester<%=rnd%>').value || null,
            url: document.getElementById('inputUrl<%=rnd%>').value.trim() || null,
            status: document.getElementById('inputStatus<%=rnd%>').value
        };

        const fakVal = document.getElementById('selectFakultas<%=rnd%>') ? document.getElementById('selectFakultas<%=rnd%>').value : null;
        if(fakVal) dataObj.fakultas = parseInt(fakVal);
        
        const jurVal = document.getElementById('selectJurusan<%=rnd%>') ? document.getElementById('selectJurusan<%=rnd%>').value : null;
        if(jurVal) dataObj.jurusan = parseInt(jurVal);

        const katVal = document.getElementById('selectKategori<%=rnd%>') ? document.getElementById('selectKategori<%=rnd%>').value : null;
        if(katVal) dataObj.kategoriPenghargaan = parseInt(katVal);

        const btn = document.getElementById('btnSimpan<%=rnd%>');
        const oriBtn = btn.innerHTML;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyimpan...")%>';
        btn.disabled = true;

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: "simpanDataRinci", log: "true", class: masterModelClass<%=rnd%>, data: dataObj })
            });
            const result = await response.json();
            
            const savedId = result.id || (result.data && result.data.id) || currentId;
            
            if ((result.status === '00' || result.status === 'success' || result.id) && savedId) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data berhasil disimpan!")%>', "bg-success text-white");
                
                activeEditingPenghargaanDosenId<%=rnd%> = savedId;
                document.getElementById('inputIdPenghargaan<%=rnd%>').value = savedId;
                
                document.getElementById('uploadPhotoArea<%=rnd%>').style.display = 'block';
                checkExistingFile<%=rnd%>(savedId, (dataObj.status === 'Disetujui'));

                if (isNewData) {
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Silakan unggah dokumen bukti.")%>', "bg-info text-white");
                } else if (isDocumentUploaded<%=rnd%>) {
                    tutupForm<%=rnd%>();
                }
                loadDataPenghargaan<%=rnd%>();
            } else {
                showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan.")%>', "bg-danger text-white");
            }
        } catch (e) { console.error(e); }
        finally { 
            btn.innerHTML = oriBtn;
            btn.disabled = false; 
        }
    };

    const editPenghargaan<%=rnd%> = async (id) => {
        bukaFormTambah<%=rnd%>();
        activeEditingPenghargaanDosenId<%=rnd%> = id;
        document.getElementById('inputIdPenghargaan<%=rnd%>').value = id;
        document.getElementById('uploadPhotoArea<%=rnd%>').style.display = 'block';

        const res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: "load", class: masterModelClass<%=rnd%>, id: id })
        });
        const result = await res.json();
        const data = result.data;

        if(data["dosen.id"]) {
            document.getElementById('hiddenIdDosen<%=rnd%>').value = data["dosen.id"];
            <% if (idDosenLogin == null) { %>
            document.getElementById('inputCariDosen<%=rnd%>').value = (data["dosen.nidn"] || '-') + ' - ' + (data["dosen.nama"] || '');
            <% } %>
        }

        document.getElementById('inputNama<%=rnd%>').value = data.nama || '';
        document.getElementById('inputNamaEn<%=rnd%>').value = data.namaEn || '';
        document.getElementById('inputTahunAkademik<%=rnd%>').value = data.tahunAkademik || '';
        document.getElementById('inputJenisSemester<%=rnd%>').value = data.jenisSemester || '';
        document.getElementById('inputTahun<%=rnd%>').value = data.tahun || '';
        document.getElementById('inputNomorSertifikat<%=rnd%>').value = data.nomorSertifikat || '';
        document.getElementById('inputCapaian<%=rnd%>').value = data.capaian || '';
        document.getElementById('inputKeterangan<%=rnd%>').value = data.keterangan || '';
        document.getElementById('inputUrl<%=rnd%>').value = data.url || '';
        document.getElementById('inputStatus<%=rnd%>').value = data.status || 'Belum diproses';

        const formatS = (v) => {
            if (!v) return '';
            if (typeof v === 'string') return v.substring(0, 10);
            return '';
        };
        
        document.getElementById('inputTanggal<%=rnd%>').value = formatS(data["tanggal.datetime_format"] || data.tanggal);
        document.getElementById('inputTanggalSelesai<%=rnd%>').value = formatS(data["tanggalSelesai.datetime_format"] || data.tanggalSelesai);

        await loadFakultas<%=rnd%>('selectFakultas<%=rnd%>', data["fakultas.id"]);
        await loadJurusan<%=rnd%>(data["fakultas.id"], data["jurusan.id"]);
        await loadKategori<%=rnd%>('selectKategori<%=rnd%>', data["kategoriPenghargaan.id"]);
        
        checkExistingFile<%=rnd%>(id, data.status === 'Disetujui');
    };

    const hapusPenghargaan<%=rnd%> = async (id) => {
        if (!confirm('<%=Common.getBahasaConfigJS("Hapus data ini secara permanen?")%>')) return;
        const res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: "hapusDataRinci", class: masterModelClass<%=rnd%>, id: id })
        });
        const result = await res.json();
        if(result.status === '00' || result.status === 'success') {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data dihapus.")%>', "bg-success text-white");
            loadDataPenghargaan<%=rnd%>();
        }
    };

    // UTILS UI
    const bukaFormTambah<%=rnd%> = () => {
        document.getElementById('formPenghargaan<%=rnd%>').reset();
        document.getElementById('inputIdPenghargaan<%=rnd%>').value = '';
        activeEditingPenghargaanDosenId<%=rnd%> = "";
        
        document.getElementById('uploadPhotoArea<%=rnd%>').style.display = 'none';
        
        <% if (idDosenLogin == null) { %>
        document.getElementById('hiddenIdDosen<%=rnd%>').value = '';
        loadFakultas<%=rnd%>('selectFakultas<%=rnd%>', null);
        loadJurusan<%=rnd%>(null, null);
        <% } %>
        loadKategori<%=rnd%>('selectKategori<%=rnd%>', null);

        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    };

    const tutupForm<%=rnd%> = () => {
        document.getElementById('viewForm<%=rnd%>').style.display = 'none';
        document.getElementById('viewList<%=rnd%>').style.display = 'block';
    };

    const updatePaginationUI<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumberDisplay<%=rnd%>').innerText = currentPage<%=rnd%> + ' / ' + totalPages;
        document.getElementById('pagingInfoPenghargaan<%=rnd%>').innerText = '<%=Common.getBahasaConfigJS("Total:")%> ' + totalRecords<%=rnd%> + ' <%=Common.getBahasaConfig("data")%>';
    };

    const changePage<%=rnd%> = (dir) => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if(dir === -1 && currentPage<%=rnd%> > 1) { currentPage<%=rnd%>--; loadDataPenghargaan<%=rnd%>(); }
        else if(dir === 1 && currentPage<%=rnd%> < totalPages) { currentPage<%=rnd%>++; loadDataPenghargaan<%=rnd%>(); }
    };

    // INIT
    document.addEventListener("DOMContentLoaded", () => {
        document.getElementById('searchPenghargaan<%=rnd%>').addEventListener("keydown", (e) => {
            if (e.key === "Enter") {
                e.preventDefault();
                resetAndLoadData<%=rnd%>(); 
            }
        });
        
        loadDataPenghargaan<%=rnd%>();
    });
</script>