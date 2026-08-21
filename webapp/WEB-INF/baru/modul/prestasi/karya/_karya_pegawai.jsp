<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.database.model.PenghargaanDosen"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.Pegawai"%>
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

Pegawai currentPegawai = tbmuser.ambilPegawai();
Dosen dosenTerkait = tbmuser.ambilDosen();
Long idDosenLogin = (dosenTerkait != null) ? dosenTerkait.getId() : null;

Fakultas loginSebagaiFakultas = tbmuser.getFakultas();
Jurusan loginSebagaiJurusan = tbmuser.getJurusan();
Long idFakLogin = (loginSebagaiJurusan != null && loginSebagaiJurusan.getFakultas() != null) ? loginSebagaiJurusan.getFakultas().getId() : (loginSebagaiFakultas != null ? loginSebagaiFakultas.getId() : null);
Long idJurLogin = (loginSebagaiJurusan != null) ? loginSebagaiJurusan.getId() : null;

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
                                <i class="fas fa-award text-primary me-2"></i><%=Common.getBahasaConfig("Manajemen Karya & Penghargaan")%>
                            </h5>
                            <small class="text-muted">
                                <%=Common.getBahasaConfig("Kelola data pencapaian, publikasi, dan penghargaan.")%>
                                <% if (idDosenLogin != null) { %>
                                <span class="badge bg-info text-dark ms-2"><%=Common.getBahasaConfig("Mode: Dosen Saya")%></span>
                                <% } else if (isAdmin) { %>
                                <span class="badge bg-warning text-dark ms-2"><%=Common.getBahasaConfig("Mode: Semua Dosen")%></span>
                                <% } %>
                            </small>
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
                                    <th class="text-start"><i class="fas fa-user-tie me-1"></i><%=Common.getBahasaConfig("Dosen / Pengusul")%></th>
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
                        <i class="fas fa-edit text-success me-2"></i><%=Common.getBahasaConfig("Formulir Karya / Penghargaan")%>
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
                                <input type="text" class="form-control fw-bold shadow-sm" id="inputNama<%=rnd%>" required>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Karya (Inggris - Opsional)")%></label>
                                <input type="text" class="form-control shadow-sm" id="inputNamaEn<%=rnd%>">
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
                                <input type="url" class="form-control shadow-sm" id="inputUrl<%=rnd%>" placeholder="https://...">
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
    var masterModelClass<%=rnd%> = "<%=PenghargaanDosen.class.getName()%>";
    var activeEditingId<%=rnd%> = "";
    var isDocumentUploaded<%=rnd%> = false;
    var currentPage<%=rnd%> = 1;
    var limitPerPage<%=rnd%> = 10;
    var totalRecords<%=rnd%> = 0;
    var idDosenLogin<%=rnd%> = "<%= idDosenLogin != null ? idDosenLogin : "" %>";

    function showToast<%=rnd%>(msg, colorClass) {
        if(typeof tampilkanToast === "function") tampilkanToast(msg, colorClass);
        else alert(msg);
    }

    function autoFillTahun<%=rnd%>(taValue) {
        var inputTahun = document.getElementById('inputTahun<%=rnd%>');
        if (taValue && taValue.indexOf('/') !== -1) {
            inputTahun.value = taValue.split('/')[0];
        } else if (!taValue) {
            inputTahun.value = '';
        }
    }

    // Upload Lampiran
    async function checkExistingFile<%=rnd%>(idPenghargaan, isApproved) {
        try {
            var res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: "file", class: "<%=ais.database.model.file.LampiranLain.class.getName()%>", ref: idPenghargaan.toString(), jenis: masterModelClass<%=rnd%>, kondisiTambahan: "", refresh: "false", usingId: false })
            });
            var dataResponse = await res.json();
            if(dataResponse.status == '00' && dataResponse.data){
                isDocumentUploaded<%=rnd%> = true;
                generatePreview<%=rnd%>(dataResponse.data, isApproved);
            } else {
                isDocumentUploaded<%=rnd%> = false;
                document.getElementById('preview-area-<%=rnd%>').innerHTML = '';
                resetUploadButtons<%=rnd%>(isApproved);
            }
        } catch(e) { console.error(e); }
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
            btnDelete = '<button type="button" class="btn btn-danger fw-bold px-3 shadow-sm ms-2" onclick="hapusFile<%=rnd%>(' + data.id + ')"><i class="fas fa-trash-alt me-1"></i> <%=Common.getBahasaConfig("Hapus")%></button>';
            <% } %>
            <% if (edit || add) { %>
            btnUploadHtml = '<input type="file" id="input_dofile_<%=rnd%>" class="d-none" accept=".pdf, image/*" onchange="uploadFile<%=rnd%>()">' +
                            '<button type="button" class="btn btn-primary fw-bold shadow-sm px-4 me-2" onclick="document.getElementById(\'input_dofile_<%=rnd%>\').click();"><i class="fas fa-sync-alt me-2"></i><%=Common.getBahasaConfig("Ganti Bukti")%></button>';
            <% } %>
        }
        document.getElementById('action-buttons-<%=rnd%>').innerHTML = btnUploadHtml + btnDownload + btnDelete;
        var html = (['jpg','jpeg','png'].indexOf(ext) !== -1)
            ? '<img src="' + url + '" class="img-fluid rounded shadow-sm border" style="max-height: 200px;">'
            : '<a href="' + url + '" target="_blank"><i class="fas fa-file-pdf fa-4x text-danger"></i></a>';
        document.getElementById('preview-area-<%=rnd%>').innerHTML = html;
    }

    function resetUploadButtons<%=rnd%>(isApproved) {
        if (!isApproved) {
            <% if (edit || add) { %>
            document.getElementById('action-buttons-<%=rnd%>').innerHTML =
                '<input type="file" id="input_dofile_<%=rnd%>" class="d-none" accept=".pdf, image/*" onchange="uploadFile<%=rnd%>()">' +
                '<button type="button" class="btn btn-primary fw-bold shadow-sm px-4" onclick="document.getElementById(\'input_dofile_<%=rnd%>\').click();"><i class="fas fa-upload me-2"></i><%=Common.getBahasaConfig("Unggah Bukti")%></button>';
            <% } else { %>
            document.getElementById('action-buttons-<%=rnd%>').innerHTML = '<span class="text-muted"><%=Common.getBahasaConfig("Tidak ada akses unggah.")%></span>';
            <% } %>
        } else {
            document.getElementById('action-buttons-<%=rnd%>').innerHTML = '<span class="text-danger fw-bold"><i class="fas fa-ban me-1"></i><%=Common.getBahasaConfig("Terkunci (Disetujui)")%></span>';
        }
    }

    async function uploadFile<%=rnd%>() {
        var fileInput = document.getElementById('input_dofile_<%=rnd%>');
        if (!fileInput || fileInput.files.length === 0) return;
        if (!activeEditingId<%=rnd%>) { showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Harap simpan data terlebih dahulu!")%>', "bg-warning text-dark"); return; }
        var file = fileInput.files[0];
        var formData = new FormData();
        formData.append('nama', file.name);
        formData.append('clazz', '<%=ais.database.model.file.LampiranLain.class.getName()%>');
        formData.append('jenis', masterModelClass<%=rnd%>);
        formData.append('id', activeEditingId<%=rnd%>);
        formData.append('tanpaLogin', 'true');
        formData.append('fileContent', file);
        var btnArea = document.getElementById('action-buttons-<%=rnd%>');
        var oriBtn = btnArea.innerHTML;
        btnArea.innerHTML = '<span class="spinner-border text-primary spinner-border-sm me-2"></span>Mengunggah...';
        var xhr = new XMLHttpRequest();
        xhr.open('POST', '<%=Common.ROOT%>/DoUpload', true);
        xhr.onload = function() {
            try {
                var res = JSON.parse(xhr.responseText);
                if (res.status === 'success' || res.status === '00' || res.keterangan === 'Sukses') {
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Berkas berhasil diunggah!")%>', "bg-success text-white");
                    checkExistingFile<%=rnd%>(activeEditingId<%=rnd%>, false);
                } else {
                    showToast<%=rnd%>(res.message || '<%=Common.getBahasaConfigJS("Gagal mengunggah berkas")%>', "bg-danger text-white");
                    btnArea.innerHTML = oriBtn;
                }
            } catch(e) { btnArea.innerHTML = oriBtn; }
            fileInput.value = '';
        };
        xhr.onerror = function() { showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Koneksi bermasalah.")%>', "bg-danger text-white"); btnArea.innerHTML = oriBtn; };
        xhr.send(formData);
    }

    async function hapusFile<%=rnd%>(idLampiran) {
        if (!confirm('<%=Common.getBahasaConfigJS("Hapus dokumen lampiran ini?")%>')) return;
        var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "hapusDataRinci", class: "<%=ais.database.model.file.LampiranLain.class.getName()%>", id: idLampiran }) });
        var result = await res.json();
        if(result.status === '00' || result.status === 'success') {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("File dihapus!")%>', "bg-success text-white");
            isDocumentUploaded<%=rnd%> = false;
            document.getElementById('preview-area-<%=rnd%>').innerHTML = '';
            resetUploadButtons<%=rnd%>(false);
        }
    }

    // LOAD MASTERS
    async function loadFakultas<%=rnd%>(targetId, selectedId) {
        var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "daftar", class: "<%=Fakultas.class.getName()%>", where1: "aktif = true", order1: "asc", sort1: "nama" }) });
        var result = await res.json();
        var options = '<option value=""><%=Common.getBahasaConfig("-- Pilih Fakultas --")%></option>';
        (result.data || []).forEach(function(row) { options += '<option value="' + row.id + '" ' + (selectedId == row.id ? "selected" : "") + '>' + row.nama + '</option>'; });
        document.getElementById(targetId).innerHTML = options;
    }

    async function loadJurusan<%=rnd%>(idFakultas, selectedId) {
        if(!idFakultas) { document.getElementById('selectJurusan<%=rnd%>').innerHTML = '<option value=""><%=Common.getBahasaConfig("-- Pilih Jurusan --")%></option>'; return; }
        var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "daftar", class: "<%=Jurusan.class.getName()%>", where1: "fakultas = " + idFakultas + " AND aktif = true", order1: "asc", sort1: "nama" }) });
        var result = await res.json();
        var options = '<option value=""><%=Common.getBahasaConfig("-- Pilih Jurusan --")%></option>';
        (result.data || []).forEach(function(row) { options += '<option value="' + row.id + '" ' + (selectedId == row.id ? "selected" : "") + '>' + row.nama + '</option>'; });
        document.getElementById('selectJurusan<%=rnd%>').innerHTML = options;
    }

    async function loadKategori<%=rnd%>(targetId, selectedId) {
        var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "daftar", class: "<%=KategoriPenghargaan.class.getName()%>", order1: "asc", sort1: "nama" }) });
        var result = await res.json();
        var options = '<option value=""><%=Common.getBahasaConfig("-- Pilih Kategori --")%></option>';
        (result.data || []).forEach(function(row) { options += '<option value="' + row.id + '" ' + (selectedId == row.id ? "selected" : "") + '>' + row.nama + '</option>'; });
        document.getElementById(targetId).innerHTML = options;
    }

    function loadFilterJurusan<%=rnd%>(val) { loadJurusan<%=rnd%>(val, null); }

    async function onInputDosenSearch<%=rnd%>(val) {
        if (val.length < 2) return;
        var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "daftar", class: "ais.database.model.Dosen", where1: "aktif = true AND (nidn ILIKE '%" + val + "%' OR nama ILIKE '%" + val + "%')", max: 20 }) });
        var result = await res.json();
        var options = '';
        (result.data || []).forEach(function(row) { options += '<option data-id="' + row.id + '" value="' + (row.nidn || '-') + ' - ' + row.nama + '"></option>'; });
        document.getElementById('dlDosen<%=rnd%>').innerHTML = options;
    }

    function checkSelectedDosen<%=rnd%>(val) {
        var options = document.getElementById('dlDosen<%=rnd%>').options;
        for(var i=0; i<options.length; i++) {
            if(options[i].value === val) { document.getElementById('hiddenIdDosen<%=rnd%>').value = options[i].getAttribute('data-id'); return; }
        }
        document.getElementById('hiddenIdDosen<%=rnd%>').value = '';
    }

    // CRUD
    function resetAndLoadData<%=rnd%>() { currentPage<%=rnd%> = 1; loadDataPenghargaan<%=rnd%>(); }

    async function loadDataPenghargaan<%=rnd%>() {
        var tbody = document.getElementById('tabelDataPenghargaan<%=rnd%>');
        var keyword = document.getElementById('searchPenghargaan<%=rnd%>').value.trim();
        var start = (currentPage<%=rnd%> - 1);
        var payload = { action: "daftar", class: masterModelClass<%=rnd%>, deep: "1", max: limitPerPage<%=rnd%>, halaman: start, order1: "desc", sort1: "id", count: "true" };
        var filters = [];
        if(idDosenLogin<%=rnd%>) { filters.push("this_.dosen = " + idDosenLogin<%=rnd%>); }
        if(keyword !== '') filters.push("(this_.nama ILIKE '%" + keyword + "%' OR dosen1_.nama ILIKE '%" + keyword + "%' OR dosen1_.nidn ILIKE '%" + keyword + "%')");
        var fTa = document.getElementById('filterTahunAkademik<%=rnd%>').value;
        if(fTa) filters.push("this_.tahunAkademik = '" + fTa + "'");
        if(filters.length > 0) { payload.where1 = filters.join(' AND '); payload.alias1 = "dosen"; }
        try {
            var response = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
            var result = await response.json();
            totalRecords<%=rnd%> = parseInt(result.count || 0);
            var list = result.data || [];
            var html = '';
            if(list.length === 0) {
                html = '<tr><td colspan="7" class="text-center text-muted py-4"><%=Common.getBahasaConfig("Tidak ada data ditemukan.")%></td></tr>';
            } else {
                list.forEach(function(row, idx) {
                    var badgeColor = 'secondary';
                    if(row.status === 'Disetujui') badgeColor = 'success';
                    else if(row.status === 'Ditolak') badgeColor = 'danger';
                    var statusBadge = '<span class="badge bg-' + badgeColor + '">' + (row.status || 'Belum diproses') + '</span>';
                    html += '<tr>' +
                        '<td class="text-center">' + (start * limitPerPage<%=rnd%> + idx + 1) + '</td>' +
                        '<td><div class="fw-bold">' + (row["dosen.nama"] || '-') + '</div><small class="text-muted">' + (row["dosen.nidn"] || '-') + '</small></td>' +
                        '<td>' + (row.nama || '-') + '</td>' +
                        '<td>' + (row["kategoriPenghargaan.nama"] || '-') + '</td>' +
                        '<td class="text-center">' + (row["tanggal.formated2"] || '-') + '</td>' +
                        '<td class="text-center">' + statusBadge + '</td>' +
                        '<td class="text-center no-export">' + aksiBarisMenu([
                            { ikon: 'fas fa-edit', label: 'Ubah penghargaan', onclick: 'editPenghargaan<%=rnd%>(' + row.id + ')' },
                            { ikon: 'fas fa-trash-alt', label: 'Hapus penghargaan', onclick: 'hapusPenghargaan<%=rnd%>(' + row.id + ')', merusak: true }
                        ]) + '</td></tr>';
                });
            }
            tbody.innerHTML = html;
            updatePaginationUI<%=rnd%>();
        } catch(e) { console.error(e); }
    }

    async function simpanPenghargaan<%=rnd%>() {
        var idDosen = document.getElementById('hiddenIdDosen<%=rnd%>').value || idDosenLogin<%=rnd%>;
        var currentId = document.getElementById('inputIdPenghargaan<%=rnd%>').value;
        var isNewData = (!currentId || currentId === '');
        if (!idDosen) { showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Harap isi dan pilih Dosen dari pencarian.")%>', "bg-warning text-dark"); return; }
        var dataObj = {
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
        var elFak = document.getElementById('selectFakultas<%=rnd%>');
        if(elFak && elFak.value) dataObj.fakultas = parseInt(elFak.value);
        var elJur = document.getElementById('selectJurusan<%=rnd%>');
        if(elJur && elJur.value) dataObj.jurusan = parseInt(elJur.value);
        var elKat = document.getElementById('selectKategori<%=rnd%>');
        if(elKat && elKat.value) dataObj.kategoriPenghargaan = parseInt(elKat.value);
        var btn = document.getElementById('btnSimpan<%=rnd%>');
        var oriBtn = btn.innerHTML;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyimpan...")%>';
        btn.disabled = true;
        try {
            var response = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "simpanDataRinci", log: "true", class: masterModelClass<%=rnd%>, data: dataObj }) });
            var result = await response.json();
            var savedId = result.id || (result.data && result.data.id) || currentId;
            if ((result.status === '00' || result.status === 'success' || result.id) && savedId) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data berhasil disimpan!")%>', "bg-success text-white");
                activeEditingId<%=rnd%> = savedId;
                document.getElementById('inputIdPenghargaan<%=rnd%>').value = savedId;
                document.getElementById('uploadPhotoArea<%=rnd%>').style.display = 'block';
                checkExistingFile<%=rnd%>(savedId, (dataObj.status === 'Disetujui'));
                if (!isNewData && isDocumentUploaded<%=rnd%>) { tutupForm<%=rnd%>(); }
                loadDataPenghargaan<%=rnd%>();
            } else {
                showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan.")%>', "bg-danger text-white");
            }
        } catch(e) { console.error(e); } finally { btn.innerHTML = oriBtn; btn.disabled = false; }
    }

    async function editPenghargaan<%=rnd%>(id) {
        bukaFormTambah<%=rnd%>();
        activeEditingId<%=rnd%> = id;
        document.getElementById('inputIdPenghargaan<%=rnd%>').value = id;
        document.getElementById('uploadPhotoArea<%=rnd%>').style.display = 'block';
        var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "load", class: masterModelClass<%=rnd%>, id: id }) });
        var result = await res.json();
        var data = result.data;
        if(data["dosen.id"]) {
            document.getElementById('hiddenIdDosen<%=rnd%>').value = data["dosen.id"];
            <% if (idDosenLogin == null) { %>
            var ciEl = document.getElementById('inputCariDosen<%=rnd%>');
            if(ciEl) ciEl.value = (data["dosen.nidn"] || '-') + ' - ' + (data["dosen.nama"] || '');
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
        var formatS = function(v) { if(!v) return ''; if(typeof v === 'string') return v.substring(0,10); return ''; };
        document.getElementById('inputTanggal<%=rnd%>').value = formatS(data["tanggal.datetime_format"] || data.tanggal);
        document.getElementById('inputTanggalSelesai<%=rnd%>').value = formatS(data["tanggalSelesai.datetime_format"] || data.tanggalSelesai);
        var elFak = document.getElementById('selectFakultas<%=rnd%>');
        if(elFak) { await loadFakultas<%=rnd%>('selectFakultas<%=rnd%>', data["fakultas.id"]); await loadJurusan<%=rnd%>(data["fakultas.id"], data["jurusan.id"]); }
        await loadKategori<%=rnd%>('selectKategori<%=rnd%>', data["kategoriPenghargaan.id"]);
        checkExistingFile<%=rnd%>(id, data.status === 'Disetujui');
    }

    async function hapusPenghargaan<%=rnd%>(id) {
        if (!confirm('<%=Common.getBahasaConfigJS("Hapus data ini secara permanen?")%>')) return;
        var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "hapusDataRinci", class: masterModelClass<%=rnd%>, id: id }) });
        var result = await res.json();
        if(result.status === '00' || result.status === 'success') { showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data dihapus.")%>', "bg-success text-white"); loadDataPenghargaan<%=rnd%>(); }
    }

    async function bukaFormTambah<%=rnd%>() {
        document.getElementById('formPenghargaan<%=rnd%>').reset();
        document.getElementById('inputIdPenghargaan<%=rnd%>').value = '';
        activeEditingId<%=rnd%> = "";
        document.getElementById('uploadPhotoArea<%=rnd%>').style.display = 'none';
        <% if (idDosenLogin == null) { %>
        var ciEl = document.getElementById('inputCariDosen<%=rnd%>');
        if(ciEl) { document.getElementById('hiddenIdDosen<%=rnd%>').value = ''; await loadFakultas<%=rnd%>('selectFakultas<%=rnd%>', null); loadJurusan<%=rnd%>(null, null); }
        <% } %>
        await loadKategori<%=rnd%>('selectKategori<%=rnd%>', null);
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    }

    function tutupForm<%=rnd%>() { document.getElementById('viewForm<%=rnd%>').style.display = 'none'; document.getElementById('viewList<%=rnd%>').style.display = 'block'; }

    function updatePaginationUI<%=rnd%>() {
        var totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumberDisplay<%=rnd%>').innerText = currentPage<%=rnd%> + ' / ' + totalPages;
        document.getElementById('pagingInfoPenghargaan<%=rnd%>').innerText = '<%=Common.getBahasaConfigJS("Total:")%> ' + totalRecords<%=rnd%> + ' <%=Common.getBahasaConfig("data")%>';
    }

    function changePage<%=rnd%>(dir) {
        var totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if(dir === -1 && currentPage<%=rnd%> > 1) { currentPage<%=rnd%>--; loadDataPenghargaan<%=rnd%>(); }
        else if(dir === 1 && currentPage<%=rnd%> < totalPages) { currentPage<%=rnd%>++; loadDataPenghargaan<%=rnd%>(); }
    }

    document.addEventListener("DOMContentLoaded", function() {
        var searchEl = document.getElementById('searchPenghargaan<%=rnd%>');
        if(searchEl) searchEl.addEventListener("keydown", function(e) { if(e.key === "Enter") { e.preventDefault(); resetAndLoadData<%=rnd%>(); } });
        <% if (idDosenLogin == null) { %>
        if(document.getElementById('filterFakultas<%=rnd%>')) { loadFakultas<%=rnd%>('filterFakultas<%=rnd%>', null); }
        <% } %>
        loadDataPenghargaan<%=rnd%>();
    });
</script>
