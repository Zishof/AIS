<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="ais.database.model.PenghargaanMahasiswa"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.KategoriPenghargaan"%>
<%@page import="ais.database.model.JenisAktfitasMahasiswa"%>
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

//Jika login sebagai admin Fakultas, hanya boleh lihat di Fakultas-nya sendiri
Fakultas loginSebagaiFakultas = tbmuser.getFakultas();

//Jika login sebagai admin Jurusan, hanya boleh lihat di Jurusan-nya sendiri
Jurusan loginSebagaiJurusan = tbmuser.getJurusan();

// Jika login sebagai mahasiswa, hanya boleh lihat data-nya sendiri
Mahasiswa loginSebagaiMahasiswa = tbmuser.getMahasiswa();
Long idMhsLogin = (loginSebagaiMahasiswa != null) ? loginSebagaiMahasiswa.getId() : null;

// Menentukan ID Penguncian untuk Hak Akses Fakultas & Jurusan
Long idFakLogin = null;
Long idJurLogin = null;
if (loginSebagaiJurusan != null) {
    idJurLogin = loginSebagaiJurusan.getId();
    if (loginSebagaiJurusan.getFakultas() != null) {
        idFakLogin = loginSebagaiJurusan.getFakultas().getId();
    }
} else if (loginSebagaiFakultas != null) {
    idFakLogin = loginSebagaiFakultas.getId();
}

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
                                <i class="fas fa-trophy text-primary me-2"></i><%=Common.getBahasaConfig("Manajemen Karya & Penghargaan Mahasiswa")%>
                            </h5>
                            <small class="text-muted"><%=Common.getBahasaConfig("Kelola data pencapaian, karya, dan penghargaan mahasiswa.")%></small>
                        </div>
                        
                        <div class="d-flex align-items-center gap-2 flex-wrap">
                            <div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;">
                                <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>
                                <input type="text" class="form-control border-start-0 ps-0 bg-white fw-medium" id="searchPenghargaan<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Karya, NIM / Nama...")%>">
                            </div>
                            
                            <button class="btn btn-sm btn-outline-info rounded-pill px-3 shadow-sm fw-bold" type="button" data-bs-toggle="collapse" data-bs-target="#filterPanel<%=rnd%>" aria-expanded="false">
                                <i class="fas fa-filter me-1"></i><%=Common.getBahasaConfig("Filter")%>
                            </button>

                            <button class="btn btn-sm btn-outline-secondary rounded-pill px-3 shadow-sm fw-bold" onclick="resetAndLoadData<%=rnd%>()" title="<%=Common.getBahasaConfig("Muat Ulang Data")%>">
                                <i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Segarkan")%>
                            </button>

                            <button class="btn btn-sm btn-outline-success rounded-pill px-3 shadow-sm fw-bold" onclick="downloadExcelPenghargaan<%=rnd%>()" title="<%=Common.getBahasaConfig("Unduh Data Excel")%>">
                                <i class="fas fa-file-excel me-1"></i><%=Common.getBahasaConfig("Excel")%>
                            </button>
                            
                            <% if (add) { %>
                            <button class="btn btn-sm btn-primary rounded-pill px-4 shadow-sm fw-bold text-nowrap" onclick="bukaFormTambah<%=rnd%>()">
                                <i class="fas fa-plus-circle me-1"></i><%=Common.getBahasaConfig("Tambah Data")%>
                            </button>
                            <% } %>
                        </div>
                    </div>

                    <div class="collapse mt-3" id="filterPanel<%=rnd%>">
                        <div class="card card-body bg-light border-0 shadow-sm rounded-3 p-3">
                            <h6 class="fw-bold text-secondary mb-3"><i class="fas fa-sliders-h me-2"></i><%=Common.getBahasaConfig("Filter Lanjutan")%></h6>
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
                                        <option value="<%= Perkuliahan.GANJIL %>"><%= Common.getBahasaConfig("Ganjil") %></option>
                                        <option value="<%= Perkuliahan.GENAP %>"><%= Common.getBahasaConfig("Genap") %></option>
                                    </select>
                                </div>
                                
                                <% if (idMhsLogin == null) { %>
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
                                <% } else { %>
                                    <input type="hidden" id="filterFakultas<%=rnd%>" value="">
                                    <input type="hidden" id="filterJurusan<%=rnd%>" value="">
                                <% } %>

                                <div class="col-md-3">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Status (Multi)")%></label>
                                    <select id="filterStatus<%=rnd%>" class="form-select form-select-sm shadow-sm" multiple size="3">
                                        <option value="Belum diproses">Belum diproses</option>
                                        <option value="Sedang diproses">Sedang diproses</option>
                                        <option value="Disetujui">Disetujui</option>
                                        <option value="Ditolak">Ditolak</option>
                                    </select>
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Kategori (Multi)")%></label>
                                    <select id="filterKategori<%=rnd%>" class="form-select form-select-sm shadow-sm" multiple size="3"></select>
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Jenis Aktifitas (Multi)")%></label>
                                    <select id="filterJenis<%=rnd%>" class="form-select form-select-sm shadow-sm" multiple size="3"></select>
                                </div>
                                <div class="col-12 text-end mt-3">
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
                                    <th class="fw-semibold text-uppercase small px-3 py-3" style="width: 60px;">#</th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-user-graduate me-1"></i><%=Common.getBahasaConfig("Mahasiswa")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-medal me-1"></i><%=Common.getBahasaConfig("Karya / Penghargaan")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-list-alt me-1"></i><%=Common.getBahasaConfig("Kategori")%></th>
                                    <th class="text-center fw-semibold text-uppercase small px-3"><i class="fas fa-calendar-alt me-1"></i><%=Common.getBahasaConfig("Tanggal")%></th>
                                    <th class="text-center fw-semibold text-uppercase small px-3"><i class="fas fa-info-circle me-1"></i><%=Common.getBahasaConfig("Status")%></th>
                                    <th class="text-center fw-semibold text-uppercase small px-3 no-export"><i class="fas fa-file-alt me-1"></i><%=Common.getBahasaConfig("Bukti")%></th>
                                    <th class="fw-semibold text-uppercase small px-3 text-center no-export" style="width: 120px;"><i class="fas fa-cogs"></i></th>
                                </tr>
                            </thead>
                            <tbody id="tabelDataPenghargaan<%=rnd%>">
                                <tr><td colspan="8" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Memuat data...")%></span></td></tr>
                            </tbody>
                        </table>
                    </div>

                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mt-4">
                        <div class="small fw-medium text-muted bg-light px-3 py-1 rounded-pill border" id="pagingInfoPenghargaan<%=rnd%>"></div>
                        <nav>
                            <ul class="pagination pagination-sm mb-0 shadow-sm">
                                <li class="page-item"><button class="page-link text-primary rounded-start-pill px-3 fw-bold" id="btnPrevPage<%=rnd%>" onclick="changePagePenghargaan<%=rnd%>(-1)"><i class="fas fa-chevron-left me-1"></i></button></li>
                                <li class="page-item disabled"><span class="page-link bg-light text-dark px-3 fw-bold" id="pageNumberDisplay<%=rnd%>">1</span></li>
                                <li class="page-item"><button class="page-link text-primary rounded-end-pill px-3 fw-bold" id="btnNextPage<%=rnd%>" onclick="changePagePenghargaan<%=rnd%>(1)"><i class="fas fa-chevron-right ms-1"></i></button></li>
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
            <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
                <div class="card-header bg-white border-0 pt-4 pb-0 px-4">
                    <div class="d-flex justify-content-between align-items-center">
                        <h5 class="fw-bold text-dark mb-0" id="formTitle<%=rnd%>">
                            <i class="fas fa-edit text-success me-2"></i><%=Common.getBahasaConfig("Formulir Karya Mahasiswa")%>
                        </h5>
                    </div>
                </div>
                <div class="card-body p-4">
                    <form id="formPenghargaan<%=rnd%>" onsubmit="event.preventDefault(); simpanPenghargaan<%=rnd%>();">
                        <input type="hidden" id="inputIdPenghargaan<%=rnd%>" value="">
                        
                        <div class="row g-4 mt-1" id="formContainer<%=rnd%>">
                            
                            <div class="col-md-12">
                                <h6 class="fw-bold text-primary border-bottom pb-2 mb-0"><i class="fas fa-user-graduate me-2"></i>1. <%=Common.getBahasaConfig("Data Mahasiswa & Akademik")%></h6>
                            </div>
                            
                            <% if (idMhsLogin == null) { %>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Cari Mahasiswa (NIM / Nama)")%> <span class="text-danger">*</span></label>
                                <input type="text" class="form-control fw-bold shadow-sm field-to-disable<%=rnd%>" id="inputCariMahasiswa<%=rnd%>" list="dlMahasiswa<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik NIM atau Nama untuk mencari...")%>" oninput="onInputMahasiswa<%=rnd%>(this.value)" onchange="checkSelectedMahasiswa<%=rnd%>(this.value)" autocomplete="off" required>
                                <datalist id="dlMahasiswa<%=rnd%>"></datalist>
                                <input type="hidden" id="hiddenIdMahasiswa<%=rnd%>" required>
                                <small class="text-muted fst-italic"><%=Common.getBahasaConfig("Pilih dari daftar. Filter fakultas/jurusan di bawah akan mempengaruhi hasil pencarian.")%></small>
                            </div>
                            
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Fakultas")%></label>
                                <select class="form-select shadow-sm bg-light field-to-disable<%=rnd%>" id="selectFakultas<%=rnd%>" onchange="loadJurusan<%=rnd%>(this.value, null)">
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Fakultas --")%></option>
                                </select>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jurusan")%></label>
                                <select class="form-select shadow-sm bg-light field-to-disable<%=rnd%>" id="selectJurusan<%=rnd%>">
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Jurusan --")%></option>
                                </select>
                            </div>
                            
                            <% } else { %>
                                <input type="hidden" id="hiddenIdMahasiswa<%=rnd%>" value="<%=idMhsLogin%>">
                                <input type="hidden" id="selectFakultas<%=rnd%>" value="">
                                <input type="hidden" id="selectJurusan<%=rnd%>" value="">
                            <% } %>

                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tahun Akademik")%> <span class="text-danger">*</span></label>
                                <select class="form-select shadow-sm field-to-disable<%=rnd%>" id="inputTahunAkademik<%=rnd%>" onchange="autoFillTahun<%=rnd%>(this.value)" required>
                                    <option value=""><%= Common.getBahasaConfig("Pilih Tahun Akademik") %></option>
                                    <% if (Common.tahunAngkatans != null) { 
                                        for (String ta : Common.tahunAngkatans) { %>
                                            <option value="<%= ta %>"><%= ta %></option>
                                    <%  } 
                                       } %>
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Semester")%> <span class="text-danger">*</span></label>
                                <select class="form-select shadow-sm field-to-disable<%=rnd%>" id="inputJenisSemester<%=rnd%>" required>
                                    <option value=""><%= Common.getBahasaConfig("Pilih Semester") %></option>
                                    <option value="<%= Perkuliahan.GANJIL %>"><%= Common.getBahasaConfig("Ganjil") %></option>
                                    <option value="<%= Perkuliahan.GENAP %>"><%= Common.getBahasaConfig("Genap") %></option>
                                </select>
                            </div>

                            <div class="col-md-12 mt-4">
                                <h6 class="fw-bold text-primary border-bottom pb-2 mb-0"><i class="fas fa-info-circle me-2"></i>2. <%=Common.getBahasaConfig("Detail Karya / Penghargaan")%></h6>
                            </div>
                            
                            <div class="col-md-12">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Karya / Penghargaan")%> <span class="text-danger">*</span></label>
                                <input type="text" class="form-control fw-bold shadow-sm field-to-disable<%=rnd%>" id="inputNama<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: Publikasi Jurnal Q1 atau Juara 1 Lomba IT")%>" required>
                            </div>
                            
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal Pelaksanaan")%></label>
                                <input type="date" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputTanggal<%=rnd%>">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal Selesai")%></label>
                                <input type="date" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputTanggalSelesai<%=rnd%>">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tahun")%></label>
                                <input type="number" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputTahun<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: 2024")%>">
                            </div>

                            <div class="col-md-12">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Alamat / Lokasi")%></label>
                                <textarea class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputAlamat<%=rnd%>" rows="2" placeholder="<%=Common.getBahasaConfig("Alamat atau lokasi kegiatan")%>"></textarea>
                            </div>


                            <div class="col-md-12 mt-4">
                                <h6 class="fw-bold text-primary border-bottom pb-2 mb-0"><i class="fas fa-layer-group me-2"></i>3. <%=Common.getBahasaConfig("Klasifikasi & Informasi Pendukung")%></h6>
                            </div>

                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Kategori")%></label>
                                <select class="form-select shadow-sm field-to-disable<%=rnd%>" id="selectKategori<%=rnd%>">
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Kategori --")%></option>
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jenis Aktifitas")%></label>
                                <select class="form-select shadow-sm field-to-disable<%=rnd%>" id="selectJenisAktifitas<%=rnd%>">
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Jenis Aktifitas --")%></option>
                                </select>
                            </div>

                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("No. SK")%></label>
                                <input type="text" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputNoSk<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Nomor SK")%>">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal SK")%></label>
                                <input type="date" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputTglSk<%=rnd%>">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("No. Sertifikat")%></label>
                                <input type="text" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputNomorSertifikat<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Nomor Sertifikat")%>">
                            </div>

                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Dosen Pembina 1 (Opsional)")%></label>
                                <input type="text" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputCariDosen1<%=rnd%>" list="dlDosen1<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik Nama / NIDN Dosen...")%>" oninput="onInputDosen<%=rnd%>(this.value, 1)" onchange="checkSelectedDosen<%=rnd%>(this.value, 1)" autocomplete="off">
                                <datalist id="dlDosen1<%=rnd%>"></datalist>
                                <input type="hidden" id="hiddenIdDosen1<%=rnd%>">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Dosen Pembina 2 (Opsional)")%></label>
                                <input type="text" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputCariDosen2<%=rnd%>" list="dlDosen2<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik Nama / NIDN Dosen...")%>" oninput="onInputDosen<%=rnd%>(this.value, 2)" onchange="checkSelectedDosen<%=rnd%>(this.value, 2)" autocomplete="off">
                                <datalist id="dlDosen2<%=rnd%>"></datalist>
                                <input type="hidden" id="hiddenIdDosen2<%=rnd%>">
                            </div>

                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Capaian")%></label>
                                <input type="text" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputCapaian<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Capaian yang diperoleh")%>">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("URL / Link Publikasi")%></label>
                                <input type="url" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputUrl<%=rnd%>" placeholder="<%=Common.getBahasaConfig("https://...")%>">
                            </div>

                            <div class="col-md-12">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Keterangan Tambahan")%></label>
                                <textarea class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputKeterangan<%=rnd%>" rows="3" placeholder="<%=Common.getBahasaConfig("Catatan atau deskripsi tambahan...")%>"></textarea>
                            </div>

                            <% if (idMhsLogin == null) { %>
                            <div class="col-md-12 mt-4">
                                <h6 class="fw-bold text-primary border-bottom pb-2 mb-0"><i class="fas fa-clipboard-check me-2"></i>4. <%=Common.getBahasaConfig("Status Persetujuan & Dokumen")%></h6>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Status Persetujuan")%> <span class="text-danger">*</span></label>
                                <select class="form-select shadow-sm fw-bold border-info" id="inputStatus<%=rnd%>" required>
                                    <option value="Belum diproses"><%=Common.getBahasaConfig("Belum diproses")%></option>
                                    <option value="Sedang diproses"><%=Common.getBahasaConfig("Sedang diproses")%></option>
                                    <option value="Disetujui"><%=Common.getBahasaConfig("Disetujui")%></option>
                                    <option value="Ditolak"><%=Common.getBahasaConfig("Ditolak")%></option>
                                </select>
                            </div>
                            <% } else { %>
                                <input type="hidden" id="inputStatus<%=rnd%>" value="Belum diproses">
                            <% } %>

                            <div class="col-md-12 mt-4 pt-3 border-top text-center" id="uploadPhotoArea<%=rnd%>" style="display: none;">
                                <div class="p-3 bg-light rounded-3 border shadow-sm">
                                    <label class="form-label fw-bold text-secondary d-block"><i class="fas fa-file-pdf text-danger me-2"></i><%=Common.getBahasaConfig("Bukti / Dokumen")%></label>
                                    
                                    <div class="d-flex justify-content-center align-items-center gap-2 mt-2" id="action-buttons-<%=rnd%>">
                                    </div>

                                    <div id="progress-wrapper-<%=rnd%>" class="mt-3" style="display: none; max-width: 300px; margin: 0 auto;">
                                        <div class="progress" style="height: 15px;">
                                            <div id="progress-bar-<%=rnd%>" class="progress-bar bg-primary progress-bar-striped progress-bar-animated fw-bold" role="progressbar" style="width: 0%; font-size: 10px;">0%</div>
                                        </div>
                                    </div>

                                    <div id="preview-area-<%=rnd%>" class="mt-3 d-flex justify-content-center" style="display: none;"></div>
                                    <small class="text-muted mt-2 d-block fst-italic" id="lbl-info-upload-<%=rnd%>"><%=Common.getBahasaConfig("Format yang didukung: PDF, JPG, PNG. (Simpan data terlebih dahulu untuk mengunggah)")%></small>
                                </div>
                            </div>

                            <div class="col-12 mt-5 text-end border-top pt-3">
                                <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold border shadow-sm" onclick="tutupForm<%=rnd%>()"><i class="fas fa-times text-danger me-2"></i><%=Common.getBahasaConfig("Batal")%></button>
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
    // ==========================================
    // BAGIAN 1: UTILITIES & STATE GLOBAL
    // ==========================================
    const masterModelClass<%=rnd%> = "<%=PenghargaanMahasiswa.class.getName()%>";
    let activeEditingPenghargaanId<%=rnd%> = "";
    let isDocumentUploaded<%=rnd%> = false; 
    
    let currentPage<%=rnd%> = 1;
    const limitPerPage<%=rnd%> = 10;
    let totalRecords<%=rnd%> = 0;

    const showToast<%=rnd%> = (msg, colorClass) => {
        if(typeof tampilkanToast === "function") {
            tampilkanToast(msg, colorClass);
        } else {
            alert(msg);
        }
    };

    const getMultiSelectValues<%=rnd%> = (selectId) => {
        const el = document.getElementById(selectId);
        if (!el) return null;
        let values = [];
        for (let i = 0; i < el.options.length; i++) {
            if (el.options[i].selected && el.options[i].value !== '') {
                values.push(el.options[i].value);
            }
        }
        return values.length > 0 ? values.join(',') : null;
    };
    
    const getMultiSelectStringValues<%=rnd%> = (selectId) => {
        const el = document.getElementById(selectId);
        if (!el) return null;
        let values = [];
        for (let i = 0; i < el.options.length; i++) {
            if (el.options[i].selected && el.options[i].value !== '') {
                values.push("'" + el.options[i].value + "'");
            }
        }
        return values.length > 0 ? values.join(',') : null;
    };
    
    const disableFormIfApproved<%=rnd%> = (status) => {
        const fieldsToDisable = document.querySelectorAll('.field-to-disable<%=rnd%>');
        const isApproved = (status === 'Disetujui');
        
        fieldsToDisable.forEach(field => {
            field.disabled = isApproved;
        });

        <% if (idMhsLogin != null && (add || edit)) { %>
            document.getElementById('btnSimpan<%=rnd%>').disabled = isApproved;
        <% } %>
        
        if (isApproved) {
             document.getElementById('lbl-info-upload-<%=rnd%>').style.display = 'none';
        } else {
             document.getElementById('lbl-info-upload-<%=rnd%>').style.display = 'block';
        }
    };

    const downloadExcelPenghargaan<%=rnd%> = () => {
        const table = document.getElementById('tabelHTMLDataPenghargaan<%=rnd%>');
        if (!table) return;

        const cloneTable = table.cloneNode(true);
        const elementsToRemove = cloneTable.querySelectorAll('.no-export, button, img, select, input, a');
        elementsToRemove.forEach(el => el.parentNode ? el.parentNode.removeChild(el) : null);
        
        const rows = cloneTable.rows;
        for (let i = 0; i < rows.length; i++) {
            const row = rows[i];
            for (let j = row.cells.length - 1; j >= 0; j--) {
                if (row.cells[j].classList.contains('no-export')) {
                    row.deleteCell(j);
                }
            }
        }

        const wb = XLSX.utils.table_to_book(cloneTable, {sheet: "<%=Common.getBahasaConfig("Data Karya Mahasiswa")%>"});
        XLSX.writeFile(wb, "Data_Karya_Mahasiswa_<%=System.currentTimeMillis()%>.xlsx");
    };

    // --- Cek File Existing (API Fetch) ---
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
        const servletUrl = '<%=Common.ROOT%>/Data';
        
        try {
            const response = await fetch(servletUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(reqObj)
            });
            const dataResponse = await response.json();
            
            if(dataResponse.status == '00' && dataResponse.data){ 
                isDocumentUploaded<%=rnd%> = true;
                var fileData = dataResponse.data;
                generatePreview<%=rnd%>(fileData, isApproved);
            } else {
                isDocumentUploaded<%=rnd%> = false;
                $('#preview-area-<%=rnd%>').html('').hide();
                resetUploadButtons<%=rnd%>(isApproved);
            }
        } catch (error) {
            console.error("<%=Common.getBahasaConfig("Gagal mengambil info file:")%>", error);
        }
    }

    function generatePreview<%=rnd%>(data, isApproved) {
        var url = data.url;
        var filename = data.nama;
        var ext = filename ? filename.split('.').pop().toLowerCase() : '';

        var btnDownload = '<a href="' + url + '" target="_blank" class="btn btn-success fw-bold px-3 d-flex align-items-center" style="border-radius: 0; border-left: 1px solid rgba(255,255,255,0.4);"><i class="fas fa-download me-1"></i> <span class="d-none d-md-inline"><%=Common.getBahasaConfig("Download")%></span></a>';
        var btnDelete = '';
        var btnUploadHtml = '';
        
        if (!isApproved) {
            <% if (delete) { %>
            btnDelete = '<button type="button" class="btn btn-danger fw-bold px-3 d-flex align-items-center field-to-disable<%=rnd%>" style="border-radius: 0; border-left: 1px solid rgba(255,255,255,0.4);" onclick="hapusFile<%=rnd%>(' + data.id + ')"><i class="fas fa-trash-alt me-1"></i> <span class="d-none d-md-inline"><%=Common.getBahasaConfig("Hapus")%></span></button>';
            <% } %>

            <% if (edit || add) { %>
            btnUploadHtml = '<input type="file" id="input_dofile_foto_<%=rnd%>" class="d-none" accept=".pdf, image/*" onchange="uploadFile<%=rnd%>()">' +
                            '<button type="button" class="btn btn-primary fw-bold shadow-sm px-4 field-to-disable<%=rnd%>" id="lbl-upload-<%=rnd%>" onclick="document.getElementById(\'input_dofile_foto_<%=rnd%>\').click();" style="border-radius: 0;">' +
                            '   <i class="fas fa-sync-alt me-2"></i><%=Common.getBahasaConfig("Ganti Bukti")%>' +
                            '</button>';
            <% } %>
        }

        $('#action-buttons-<%=rnd%>').html(btnUploadHtml + btnDownload + btnDelete);

        var container = $('#preview-area-<%=rnd%>');
        var html = '';

        if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].indexOf(ext) !== -1) {
            html = '<a href="' + url + '" target="_blank" title="<%=Common.getBahasaConfig("Klik untuk memperbesar")%>"><img src="' + url + '" class="img-fluid rounded shadow-sm" style="max-height: 250px; object-fit: cover; border: 2px solid #ddd;" alt="' + filename + '"></a>';
        } else if (['mp3', 'wav', 'ogg', 'm4a'].indexOf(ext) !== -1) {
            html = '<audio controls class="w-100 rounded shadow-sm border border-info" style="max-width: 500px;"><source src="' + url + '" type="audio/' + (ext === 'mp3' ? 'mpeg' : ext) + '"></audio>';
        } else if (['mp4', 'webm', 'mov'].indexOf(ext) !== -1) {
            html = '<div class="ratio ratio-16x9 shadow-sm rounded overflow-hidden border border-dark" style="max-width: 500px;"><video controls class="w-100"><source src="' + url + '" type="video/' + ext + '"></video></div>';
        } else {
            var iconClass = 'fa-file-alt text-secondary';
            if(ext === 'pdf') { iconClass = 'fa-file-pdf text-danger'; }
            else if(['xls','xlsx','csv'].indexOf(ext) !== -1) { iconClass = 'fa-file-excel text-success'; }
            else if(['doc','docx'].indexOf(ext) !== -1) { iconClass = 'fa-file-word text-primary'; }
            else if(['zip','rar','7z'].indexOf(ext) !== -1) { iconClass = 'fa-file-archive text-warning'; }

            html = '<a href="' + url + '" target="_blank" class="text-decoration-none" title="<%=Common.getBahasaConfig("Buka / Download")%>"><i class="fas ' + iconClass + ' fa-4x p-3 bg-white rounded shadow-sm border"></i></a>';
        }

        container.html(html).fadeIn();
    }

    const resetUploadButtons<%=rnd%> = (isApproved) => {
        if (!isApproved) {
            var btnUploadHtml = '';
            <% if (edit || add) { %>
            btnUploadHtml = '<input type="file" id="input_dofile_foto_<%=rnd%>" class="d-none" accept=".pdf, image/*" onchange="uploadFile<%=rnd%>()">' +
                            '<button type="button" class="btn btn-primary fw-bold shadow-sm px-4 field-to-disable<%=rnd%>" id="lbl-upload-<%=rnd%>" onclick="document.getElementById(\'input_dofile_foto_<%=rnd%>\').click();" style="border-radius: 0;">' +
                            '   <i class="fas fa-upload me-2"></i><%=Common.getBahasaConfig("Unggah Bukti")%>' +
                            '</button>';
            <% } %>
            $('#action-buttons-<%=rnd%>').html(btnUploadHtml);
        } else {
            $('#action-buttons-<%=rnd%>').html('<span class="text-danger fw-bold"><i class="fas fa-ban me-1"></i><%=Common.getBahasaConfig("Dokumen Tidak Ada (Read-Only)")%></span>');
        }
    };

    const hapusFile<%=rnd%> = async (idLampiran) => {
        if (!confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus dokumen ini?")%>')) return;
        const payload = {
            action: "hapusDataRinci",
            class: "<%=ais.database.model.file.LampiranLain.class.getName()%>",
            id: idLampiran
        };
        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const result = await response.json();
            if (result.status === '00' || result.status === 'success') {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("File berhasil dihapus!")%>', 'bg-success text-white');
                isDocumentUploaded<%=rnd%> = false;
                $('#preview-area-<%=rnd%>').html('').hide();
                resetUploadButtons<%=rnd%>(false);
                loadDataPenghargaan<%=rnd%>();
            } else {
                showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("File gagal dihapus.")%>', 'bg-danger text-white');
            }
        } catch (error) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal terhubung ke server.")%>', 'bg-danger text-white');
        }
    };

    // ==========================================
    // BAGIAN 2: LOGIKA LOAD MASTERS
    // ==========================================
    const loadFakultas<%=rnd%> = async (targetId, selectedId) => {
        var reqObj = { 
            "action": "daftar", 
            "class": "<%=Fakultas.class.getName()%>", 
            "where1": "aktif = true or aktif is null",
            "order1": "asc", 
            "sort1": "nama" 
        };
        <% if (idFakLogin != null) { %>
            reqObj.where1 += " AND id = <%=idFakLogin%>";
        <% } %>

        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(reqObj)
            });
            const dataResponse = await res.json();
            const data = dataResponse.data || [];
            
            let options = '';
            if (data.length === 1) {
                options = '<option value="' + data[0].id + '" selected>' + data[0].nama + '</option>';
                if(targetId === 'selectFakultas<%=rnd%>') {
                    document.getElementById(targetId).disabled = true;
                }
            } else {
                options = '<option value="">-- ' + (targetId.includes('filter') ? '<%=Common.getBahasaConfigJS("Semua Fakultas")%>' : '<%=Common.getBahasaConfigJS("Pilih Fakultas")%>') + ' --</option>';
                data.forEach((row) => {
                    const selected = (selectedId && selectedId == row.id) ? "selected" : "";
                    options += '<option value="' + row.id + '" ' + selected + '>' + row.nama + '</option>';
                });
            }
            document.getElementById(targetId).innerHTML = options;
        } catch (e) { console.error("Error load Fakultas", e); }
    };

    const loadFilterJurusan<%=rnd%> = async (idFakultas) => {
        var reqObj = { 
            "action": "daftar", 
            "class": "<%=Jurusan.class.getName()%>", 
            "where1": "aktif = true or aktif is null",
            "order1": "asc", 
            "sort1": "nama" 
        };

        let filterWhere = [];
        <% if (idJurLogin != null) { %>
            filterWhere.push("id = <%=idJurLogin%>");
        <% } else { %>
            if(idFakultas) {
                filterWhere.push("fakultas = " + idFakultas);
            } <% if (idFakLogin != null) { %> else {
                filterWhere.push("fakultas = <%=idFakLogin%>");
            } <% } %>
        <% } %>
        if (filterWhere.length > 0) reqObj.where1 += " AND " + filterWhere.join(' AND ');

        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(reqObj)
            });
            const dataResponse = await res.json();
            const data = dataResponse.data || [];

            let options = '';
            if (data.length === 1) {
                options = '<option value="' + data[0].id + '" selected>' + data[0].nama + '</option>';
            } else {
                options = '<option value="">-- <%=Common.getBahasaConfig("Semua Jurusan")%> --</option>';
                data.forEach((row) => {
                    options += '<option value="' + row.id + '">' + row.nama + '</option>';
                });
            }
            document.getElementById('filterJurusan<%=rnd%>').innerHTML = options;
        } catch (e) { console.error("Error load Filter Jurusan", e); }
    };

    const loadJurusan<%=rnd%> = async (idFakultas, selectedId) => {
        var reqObj = { 
            "action": "daftar", 
            "class": "<%=Jurusan.class.getName()%>", 
            "where1": "aktif = true or aktif is null",
            "order1": "asc", 
            "sort1": "nama" 
        };

        let filterWhere = [];
        <% if (idJurLogin != null) { %>
            filterWhere.push("id = <%=idJurLogin%>");
        <% } else { %>
            if(idFakultas) {
                filterWhere.push("fakultas = " + idFakultas);
            }
        <% } %>
        if (filterWhere.length > 0) reqObj.where1 += " AND " + filterWhere.join(' AND ');

        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(reqObj)
            });
            const dataResponse = await res.json();
            const data = dataResponse.data || [];

            let options = '';
            if (data.length === 1) {
                options = '<option value="' + data[0].id + '" selected>' + data[0].nama + '</option>';
                document.getElementById('selectJurusan<%=rnd%>').disabled = true;
            } else {
                document.getElementById('selectJurusan<%=rnd%>').disabled = false;
                options = '<option value="">-- <%=Common.getBahasaConfig("Pilih Jurusan")%> --</option>'; 
                data.forEach((row) => {
                    const selected = (selectedId && selectedId == row.id) ? "selected" : "";
                    options += '<option value="' + row.id + '" ' + selected + '>' + row.nama + '</option>';
                });
            }
            document.getElementById('selectJurusan<%=rnd%>').innerHTML = options;
        } catch (e) { console.error("Error load Jurusan", e); }
    };

    const loadKategori<%=rnd%> = async (targetId, selectedId) => {
        var reqObj = { 
            "action": "daftar", 
            "class": "<%=KategoriPenghargaan.class.getName()%>", 
            "order1": "asc", 
            "sort1": "nama" 
        };
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(reqObj)
            });
            const dataResponse = await res.json();
            const data = dataResponse.data || [];

            let options = targetId.includes('filter') ? '' : '<option value="">-- <%=Common.getBahasaConfig("Pilih Kategori")%> --</option>';
            data.forEach((row) => {
                const selected = (selectedId && selectedId == row.id) ? "selected" : "";
                options += '<option value="' + row.id + '" ' + selected + '>' + row.nama + '</option>';
            });
            document.getElementById(targetId).innerHTML = options;
        } catch (e) { console.error("Error load Kategori", e); }
    };

    const loadJenisAktifitas<%=rnd%> = async (targetId, selectedId) => {
        var reqObj = { 
            "action": "daftar", 
            "class": "<%=JenisAktfitasMahasiswa.class.getName()%>", 
            "where1": "aktif = true or aktif is null",
            "order1": "asc", 
            "sort1": "nama" 
        };
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(reqObj)
            });
            const dataResponse = await res.json();
            const data = dataResponse.data || [];

            let options = targetId.includes('filter') ? '' : '<option value="">-- <%=Common.getBahasaConfig("Pilih Jenis Aktifitas")%> --</option>';
            data.forEach((row) => {
                const selected = (selectedId && selectedId == row.id) ? "selected" : "";
                options += '<option value="' + row.id + '" ' + selected + '>' + row.nama + '</option>';
            });
            document.getElementById(targetId).innerHTML = options;
        } catch (e) { console.error("Error load Jenis Aktifitas", e); }
    };

    // Pencarian Mahasiswa Datalist dg Filter FK & JR Dinamis
    <% if (idMhsLogin == null) { %>
    const onInputMahasiswa<%=rnd%> = async (val) => {
        if (val.length < 1) return;
        let valFak = document.getElementById('selectFakultas<%=rnd%>').value;
        let valJur = document.getElementById('selectJurusan<%=rnd%>').value;
        
        let extSql = "aktif = true AND (nim ILIKE '%" + val + "%' OR nama ILIKE '%" + val + "%')";
        if (valJur) {
            extSql += " AND jurusan = " + valJur;
        } else if (valFak) {
            extSql += " AND jurusan IN (SELECT id FROM public.jurusan WHERE fakultas = " + valFak + ")";
        }

        var reqObj = { 
            "action": "daftar", 
            "class": "<%=Mahasiswa.class.getName()%>", 
            "where1": extSql,
            "max": 20
        };
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(reqObj)
            });
            const dataResponse = await res.json();
            const data = dataResponse.data || [];

            let options = '';
            data.forEach((row) => {
                options += '<option data-id="' + row.id + '" value="' + row.nim + ' - ' + row.nama + '"></option>';
            });
            document.getElementById('dlMahasiswa<%=rnd%>').innerHTML = options;
        } catch (e) { console.error("Error search Mahasiswa", e); }
    };

    const checkSelectedMahasiswa<%=rnd%> = (val) => {
        const dl = document.getElementById('dlMahasiswa<%=rnd%>');
        const options = dl.options;
        for(let i=0; i<options.length; i++) {
            if(options[i].value === val) {
                document.getElementById('hiddenIdMahasiswa<%=rnd%>').value = options[i].getAttribute('data-id');
                return;
            }
        }
        document.getElementById('hiddenIdMahasiswa<%=rnd%>').value = '';
    };
    <% } %>

    // Pencarian Dosen Datalist
    const onInputDosen<%=rnd%> = async (val, no) => {
        if (val.length < 2) return;
        var reqObj = { 
            "action": "daftar", 
            "class": "ais.database.model.Dosen", 
            "where1": "aktif = true AND (nidn ILIKE '%" + val + "%' OR nama ILIKE '%" + val + "%')",
            "max": 20
        };
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(reqObj)
            });
            const dataResponse = await res.json();
            const data = dataResponse.data || [];

            let options = '';
            data.forEach((row) => {
                options += '<option data-id="' + row.id + '" value="' + (row.nidn || '-') + ' - ' + row.nama + '"></option>';
            });
            document.getElementById('dlDosen' + no + '<%=rnd%>').innerHTML = options;
        } catch (e) { console.error("Error search Dosen", e); }
    };

    const checkSelectedDosen<%=rnd%> = (val, no) => {
        const dl = document.getElementById('dlDosen' + no + '<%=rnd%>');
        const options = dl.options;
        for(let i=0; i<options.length; i++) {
            if(options[i].value === val) {
                document.getElementById('hiddenIdDosen' + no + '<%=rnd%>').value = options[i].getAttribute('data-id');
                return;
            }
        }
        document.getElementById('hiddenIdDosen' + no + '<%=rnd%>').value = '';
    };

    // ==========================================
    // BAGIAN 3: LOGIKA LIST, PAGING & SEARCH
    // ==========================================
    const buildFilterSQL<%=rnd%> = () => {
        const keyword = document.getElementById('searchPenghargaan<%=rnd%>').value.trim().replace(/'/g, "''");
        let filters = [];
        
        <% if (idMhsLogin != null) { %>
            filters.push("mahasiswa = <%=idMhsLogin%>");
        <% } else { %>
	        <% if (idJurLogin != null) { %>
	            filters.push("jurusan = <%=idJurLogin%>");
	        <% } else if (idFakLogin != null) { %>
	            filters.push("fakultas = <%=idFakLogin%>");
	        <% } %>
        <% } %>

        if (keyword !== '') {
            filters.push("(this_.nama ILIKE '%" + keyword + "%' OR mahasiswa1_.nim ILIKE '%" + keyword + "%' OR mahasiswa1_.nama ILIKE '%" + keyword + "%')");
        }
        
        const fTa = document.getElementById('filterTahunAkademik<%=rnd%>').value;
        if(fTa) filters.push("this_.tahunAkademik = '" + fTa + "'");
        
        const fSmt = document.getElementById('filterSemester<%=rnd%>').value;
        if(fSmt) filters.push("this_.jenisSemester = '" + fSmt + "'");

        <% if (idMhsLogin == null) { %>
            const fFak = document.getElementById('filterFakultas<%=rnd%>').value;
            if(fFak) filters.push("this_.fakultas = " + fFak);
            
            const fJur = document.getElementById('filterJurusan<%=rnd%>').value;
            if(fJur) filters.push("this_.jurusan = " + fJur);
        <% } %>

        const kat = getMultiSelectValues<%=rnd%>('filterKategori<%=rnd%>');
        if(kat) filters.push("this_.kategoriPenghargaan IN (" + kat + ")");

        const jns = getMultiSelectValues<%=rnd%>('filterJenis<%=rnd%>');
        if(jns) filters.push("this_.jenisAktfitasMahasiswa IN (" + jns + ")");

        const stt = getMultiSelectStringValues<%=rnd%>('filterStatus<%=rnd%>');
        if(stt) filters.push("this_.status IN (" + stt + ")");

        return filters.length > 0 ? filters.join(' AND ') : '';
    };

    const resetAndLoadData<%=rnd%> = () => {
        currentPage<%=rnd%> = 1;
        loadDataPenghargaan<%=rnd%>();
    };

    const loadDataPenghargaan<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataPenghargaan<%=rnd%>');
        const keyword = document.getElementById('searchPenghargaan<%=rnd%>').value.trim().replace(/'/g, "''");
        tbody.innerHTML = '<tr><td colspan="8" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Mengambil data...")%></span></td></tr>';
        
        const start = (currentPage<%=rnd%> - 1);
        const whereClause = buildFilterSQL<%=rnd%>();

        var reqObj = { 
            "action": "daftar", 
            "class": masterModelClass<%=rnd%>, 
            "deep": "1",
            "max": limitPerPage<%=rnd%>,
            "halaman": start,
            "order1": "desc",
            "sort1": "id",
            "count": "true"
        };

        if (keyword !== '') { reqObj["alias1"] = "mahasiswa"; }
        if (whereClause) { reqObj["where1"] = whereClause; }

        const servletUrl = '<%=Common.ROOT%>/Data';

        try {
            const response = await fetch(servletUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(reqObj)
            });
            const dataResponse = await response.json();
            totalRecords<%=rnd%> = parseInt(dataResponse.count || 0);
            const recordList = dataResponse.data || [];
            
            let htmlList = '';
            if (recordList.length === 0) {
                htmlList = '<tr><td colspan="8" class="text-center text-muted py-5"><i class="fas fa-box-open fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Tidak ada data ditemukan.")%></td></tr>';
            } else {
                let no = (start * limitPerPage<%=rnd%>) + 1;
                
                const filePromises = recordList.map(async (row) => {
                    var fileReq = { 
                        "action": "file", 
                        "class": "<%=ais.database.model.file.LampiranLain.class.getName()%>", 
                        "ref": row.id.toString(), 
                        "jenis": masterModelClass<%=rnd%>,
                        "kondisiTambahan": "",
                        "refresh": "false",
                        "usingId": false
                    };
                    try {
                        const fileRes = await fetch(servletUrl, {
                            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(fileReq)
                        });
                        const fData = await fileRes.json();
                        if(fData.status === '00' && fData.data) { row.lampiranData = fData.data; }
                    } catch(e) { console.error("Gagal menarik file row ID:", row.id, e); }
                    return row;
                });

                await Promise.all(filePromises);
                
                recordList.forEach((row) => {
                    const safeNama = (row.nama || '-').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    const mhsNim = row["mahasiswa.nim"] || '-';
                    const mhsNama = row["mahasiswa.nama"] || '-';
                    const kategoriNama = row["kategoriPenghargaan.nama"] || '-';
                    const tgl = row["tanggal.formated2"] || row["tanggal.datetime_format"] || row["tanggal"] || '-';
                    const statusVal = row.status || 'Belum diproses';
                    
                    const infoMhs = '<div class="fw-bold text-dark fs-6">' + mhsNama + '</div>' +
                                    '<small class="text-muted"><i class="fas fa-id-card me-1"></i>NIM: ' + mhsNim + '</small>';

                    let statusBadge = '<span class="badge bg-secondary">' + statusVal + '</span>';
                    if (statusVal === 'Disetujui') { statusBadge = '<span class="badge bg-success">' + statusVal + '</span>'; }
                    else if (statusVal === 'Ditolak') { statusBadge = '<span class="badge bg-danger">' + statusVal + '</span>'; }
                    else if (statusVal === 'Sedang diproses') { statusBadge = '<span class="badge bg-info">' + statusVal + '</span>'; }

                    let buktiHtml = '<span class="text-danger small fw-bold fst-italic"><i class="fas fa-times-circle me-1"></i><%=Common.getBahasaConfig("Belum Diupload")%></span>';
                    if (row.lampiranData) {
                        const lampiranData = row.lampiranData;
                        const fileName = (lampiranData.nama || '').toLowerCase();
                        const isImage = fileName.endsWith('.jpg') || fileName.endsWith('.jpeg') || fileName.endsWith('.png') || fileName.endsWith('.gif') || fileName.endsWith('.webp');
                        const fileUrl = lampiranData.url;
                        if (isImage) {
                            buktiHtml = '<img src="' + fileUrl + '" class="rounded-3 shadow-sm object-fit-cover border border-light" style="width: 50px; height: 50px; cursor:pointer;" onclick="window.open(\'' + fileUrl + '\', \'_blank\')" alt="<%=Common.getBahasaConfig("Bukti")%>" title="' + lampiranData.nama + '">';
                        } else {
                            buktiHtml = '<a href="' + fileUrl + '" target="_blank" class="btn btn-sm btn-outline-info shadow-sm py-1 fw-bold" title="' + lampiranData.nama + '"><i class="fas fa-download me-1"></i><%=Common.getBahasaConfig("Cek Bukti")%></a>';
                        }
                    }

                    let actionBtns = '';
                    <% if (edit) { %>
                        actionBtns += '<button class="btn btn-sm btn-outline-warning text-dark shadow-sm px-2 me-1 fw-bold" onclick="editPenghargaan<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Edit / Lihat Detail")%>"><i class="fas fa-edit"></i></button>';
                    <% } %>
                    <% if (delete) { %>
                        <% if (idMhsLogin != null) { %>
                            if (statusVal !== 'Disetujui') {
                                actionBtns += '<button class="btn btn-sm btn-outline-danger shadow-sm px-2 fw-bold" onclick="hapusPenghargaan<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Hapus Data")%>"><i class="fas fa-trash-alt"></i></button>';
                            }
                        <% } else { %>
                            actionBtns += '<button class="btn btn-sm btn-outline-danger shadow-sm px-2 fw-bold" onclick="hapusPenghargaan<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Hapus Data")%>"><i class="fas fa-trash-alt"></i></button>';
                        <% } %>
                    <% } %>

                    htmlList += '<tr>' +
                                '<td class="text-center text-muted fw-medium">' + (no++) + '</td>' +
                                '<td class="text-start">' + infoMhs + '</td>' +
                                '<td class="text-start fw-semibold text-primary">' + safeNama + '</td>' +
                                '<td class="text-start text-muted small">' + kategoriNama + '</td>' +
                                '<td class="text-center text-muted small">' + tgl + '</td>' +
                                '<td class="text-center align-middle">' + statusBadge + '</td>' +
                                '<td class="text-center align-middle no-export">' + buktiHtml + '</td>' +
                                '<td class="text-center text-nowrap no-export">' + actionBtns + '</td>' +
                                '</tr>';
                });
            }
            tbody.innerHTML = htmlList;
            updatePaginationUI<%=rnd%>();
        } catch (error) {
            console.error("Gagal load tabel:", error);
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal menarik data. Silakan muat ulang.")%></td></tr>';
        }
    };

    const updatePaginationUI<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumberDisplay<%=rnd%>').innerText = currentPage<%=rnd%> + " / " + totalPages;
        const startIdx = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%> + 1;
        const endIdx = Math.min(currentPage<%=rnd%> * limitPerPage<%=rnd%>, totalRecords<%=rnd%>);
        const startInfo = totalRecords<%=rnd%> > 0 ? startIdx : 0;
        
        document.getElementById('pagingInfoPenghargaan<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startInfo + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari")%> <b class="text-dark">' + totalRecords<%=rnd%> + '</b> <%=Common.getBahasaConfig("data")%>';
        document.getElementById('btnPrevPage<%=rnd%>').disabled = (currentPage<%=rnd%> === 1);
        document.getElementById('btnNextPage<%=rnd%>').disabled = (currentPage<%=rnd%> === totalPages);
    };

    const changePagePenghargaan<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if (direction === -1 && currentPage<%=rnd%> > 1) {
            currentPage<%=rnd%>--; loadDataPenghargaan<%=rnd%>();
        } else if (direction === 1 && currentPage<%=rnd%> < totalPages) {
            currentPage<%=rnd%>++; loadDataPenghargaan<%=rnd%>();
        }
    };

    // ==========================================
    // BAGIAN 4: LOGIKA FORM & UPLOAD
    // ==========================================
    const bukaFormTambah<%=rnd%> = () => {
        document.getElementById('inputIdPenghargaan<%=rnd%>').value = '';
        document.getElementById('formPenghargaan<%=rnd%>').reset();
        activeEditingPenghargaanId<%=rnd%> = "";
        isDocumentUploaded<%=rnd%> = false;
        
        disableFormIfApproved<%=rnd%>('Belum diproses');

        <% if (idMhsLogin == null) { %>
            document.getElementById('inputCariMahasiswa<%=rnd%>').value = '';
            document.getElementById('hiddenIdMahasiswa<%=rnd%>').value = '';
            if (document.getElementById('inputStatus<%=rnd%>')) {
                document.getElementById('inputStatus<%=rnd%>').value = 'Belum diproses';
            }
            loadFakultas<%=rnd%>('selectFakultas<%=rnd%>', null);
            loadJurusan<%=rnd%>(null, null);
        <% } else { %>
            if (document.getElementById('inputStatus<%=rnd%>')) {
                document.getElementById('inputStatus<%=rnd%>').value = 'Belum diproses';
            }
        <% } %>

        document.getElementById('inputTahunAkademik<%=rnd%>').value = '<%=selectedTa%>';
        document.getElementById('inputJenisSemester<%=rnd%>').value = '<%=selectedSmt%>';
        document.getElementById('inputCariDosen1<%=rnd%>').value = '';
        document.getElementById('hiddenIdDosen1<%=rnd%>').value = '';
        document.getElementById('inputCariDosen2<%=rnd%>').value = '';
        document.getElementById('hiddenIdDosen2<%=rnd%>').value = '';
        
        loadKategori<%=rnd%>('selectKategori<%=rnd%>', null);
        loadJenisAktifitas<%=rnd%>('selectJenisAktifitas<%=rnd%>', null);

        document.getElementById('uploadPhotoArea<%=rnd%>').style.display = 'none';
        document.getElementById('preview-area-<%=rnd%>').style.display = 'none';
        document.getElementById('preview-area-<%=rnd%>').innerHTML = '';
        resetUploadButtons<%=rnd%>(false);

        document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-plus-circle text-primary me-2"></i><%=Common.getBahasaConfig("Tambah Karya Mahasiswa")%>';
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    };

    const tutupForm<%=rnd%> = () => {
        document.getElementById('viewForm<%=rnd%>').style.display = 'none';
        document.getElementById('viewList<%=rnd%>').style.display = 'block';
        loadDataPenghargaan<%=rnd%>();
    };

    const uploadFile<%=rnd%> = async () => {
        const fileInput = document.getElementById('input_dofile_foto_<%=rnd%>');
        if (fileInput.files.length === 0) return;

        if (!activeEditingPenghargaanId<%=rnd%>) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Harap simpan data terlebih dahulu sebelum unggah bukti!")%>', 'bg-warning text-dark');
            return;
        }

        const file = fileInput.files[0];
        
        // Buat preview lokal sementara
        const localData = { url: URL.createObjectURL(file), nama: file.name, id: 'temp' };
        generatePreview<%=rnd%>(localData, false);

        const formData = new FormData();
        formData.append('nama', file.name);
        formData.append('clazz', '<%=ais.database.model.file.LampiranLain.class.getName()%>');
        formData.append('jenis', masterModelClass<%=rnd%>);
        formData.append('id', activeEditingPenghargaanId<%=rnd%>);
        formData.append('tanpaLogin', 'true');
        formData.append('fileContent', file);

        const progressBar = document.getElementById('progress-bar-<%=rnd%>');
        const progressWrapper = document.getElementById('progress-wrapper-<%=rnd%>');
        progressWrapper.style.display = 'block';

        const xhr = new XMLHttpRequest();
        
        // PERBAIKAN 1: Menggunakan endpoint /DoUpload sesuai template bawaan
        xhr.open('POST', '<%=Common.ROOT%>/DoUpload', true);

        xhr.upload.onprogress = function (e) {
            if (e.lengthComputable) {
                const percentComplete = Math.round((e.loaded / e.total) * 100);
                progressBar.style.width = percentComplete + '%';
                progressBar.innerText = percentComplete + '%';
            }
        };

        xhr.onload = function () {
            progressWrapper.style.display = 'none';
            if (xhr.status === 200) {
                // PERBAIKAN 2: Menggunakan Try-Catch agar tidak crash jika server membalas dengan HTML
                try {
                    const res = JSON.parse(xhr.responseText);
                    
                    // Menyesuaikan dengan parameter kembalian dari template asli
                    if (res.status === 'success' || res.status === '00' || res.status === 'Sukses' || res.keterangan === 'Sukses') {
                        isDocumentUploaded<%=rnd%> = true;
                        showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Berkas berhasil diunggah!")%>', 'bg-success text-white');
                        checkExistingFile<%=rnd%>(activeEditingPenghargaanId<%=rnd%>, false);
                        loadDataPenghargaan<%=rnd%>();
                    } else {
                        showToast<%=rnd%>(res.message || res.keterangan || '<%=Common.getBahasaConfigJS("Gagal mengunggah berkas ke server")%>', 'bg-danger text-white');
                        checkExistingFile<%=rnd%>(activeEditingPenghargaanId<%=rnd%>, false); 
                    }
                } catch (e) {
                    console.error("Respon server bukan JSON:", xhr.responseText);
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal: Server membalas dengan format tidak valid (Kemungkinan sesi habis atau error di backend).")%>', 'bg-danger text-white');
                    checkExistingFile<%=rnd%>(activeEditingPenghargaanId<%=rnd%>, false);
                }
            } else {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal terhubung ke server upload.")%>', 'bg-danger text-white');
                checkExistingFile<%=rnd%>(activeEditingPenghargaanId<%=rnd%>, false);
            }
            
            // Reset input file agar bisa memilih file yang sama lagi jika gagal
            fileInput.value = ''; 
        };

        xhr.onerror = function () {
            progressWrapper.style.display = 'none';
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Koneksi bermasalah saat proses unggah.")%>', 'bg-danger text-white');
            checkExistingFile<%=rnd%>(activeEditingPenghargaanId<%=rnd%>, false);
            fileInput.value = '';
        };

        xhr.send(formData);
    };

    const simpanPenghargaan<%=rnd%> = async () => {
        const idPenghargaan = document.getElementById('inputIdPenghargaan<%=rnd%>').value;
        const btnSimpan = document.getElementById('btnSimpan<%=rnd%>');
        
        // 1. Validasi Mahasiswa
        const mhsVal = document.getElementById('hiddenIdMahasiswa<%=rnd%>').value;
        if(!mhsVal || mhsVal === '') {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Harap isi dan pilih data Mahasiswa dari daftar pencarian.")%>', 'bg-warning text-dark');
            return;
        }

        const isNewData = (!idPenghargaan || idPenghargaan === '');

        // 2. Siapkan Objek Data (Format Integer untuk ID Relasi agar tidak null di Postgres)
        const dataObj = {
            nama: document.getElementById('inputNama<%=rnd%>').value.trim(),
            tanggal: document.getElementById('inputTanggal<%=rnd%>').value || null,
            tanggalSelesai: document.getElementById('inputTanggalSelesai<%=rnd%>').value || null,
            tahun: document.getElementById('inputTahun<%=rnd%>').value ? parseInt(document.getElementById('inputTahun<%=rnd%>').value) : null,
            alamat: document.getElementById('inputAlamat<%=rnd%>').value.trim() || null,
            tahunAkademik: document.getElementById('inputTahunAkademik<%=rnd%>').value || null,
            jenisSemester: document.getElementById('inputJenisSemester<%=rnd%>').value || null,
            noSk: document.getElementById('inputNoSk<%=rnd%>').value.trim() || null,
            tglSk: document.getElementById('inputTglSk<%=rnd%>').value || null,
            nomorSertifikat: document.getElementById('inputNomorSertifikat<%=rnd%>').value.trim() || null,
            capaian: document.getElementById('inputCapaian<%=rnd%>').value.trim() || null,
            url: document.getElementById('inputUrl<%=rnd%>').value.trim() || null,
            keterangan: document.getElementById('inputKeterangan<%=rnd%>').value.trim() || null,
            status: document.getElementById('inputStatus<%=rnd%>') ? document.getElementById('inputStatus<%=rnd%>').value : 'Belum diproses'
        };
        
        // Set Foreign Keys (Integer)
        dataObj.mahasiswa = parseInt(mhsVal);
        
        <% if (idMhsLogin == null) { %>
            const fakVal = document.getElementById('selectFakultas<%=rnd%>').value;
            dataObj.fakultas = (fakVal && fakVal !== '') ? parseInt(fakVal) : null;
            const jurVal = document.getElementById('selectJurusan<%=rnd%>').value;
            dataObj.jurusan = (jurVal && jurVal !== '') ? parseInt(jurVal) : null;
        <% } %>
        
        const katVal = document.getElementById('selectKategori<%=rnd%>').value;
        dataObj.kategoriPenghargaan = (katVal && katVal !== '') ? parseInt(katVal) : null;
        const jnsVal = document.getElementById('selectJenisAktifitas<%=rnd%>').value;
        dataObj.jenisAktfitasMahasiswa = (jnsVal && jnsVal !== '') ? parseInt(jnsVal) : null;
        const dos1Val = document.getElementById('hiddenIdDosen1<%=rnd%>').value;
        dataObj.dosenPembina1 = (dos1Val && dos1Val !== '') ? parseInt(dos1Val) : null;
        const dos2Val = document.getElementById('hiddenIdDosen2<%=rnd%>').value;
        dataObj.dosenPembina2 = (dos2Val && dos2Val !== '') ? parseInt(dos2Val) : null;

        // 3. Payload API
        const payload = { 
            action: "simpanDataRinci", 
            log: "true", 
            class: masterModelClass<%=rnd%>, 
            data: dataObj
        };

        if (!isNewData) {
            payload.id = idPenghargaan;
        }

        const oriBtn = btnSimpan.innerHTML;
        btnSimpan.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyimpan...")%>';
        btnSimpan.disabled = true;

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const result = await response.json();
            
            // 4. Menangkap ID hasil simpan
            const savedId = result.id || (result.data && result.data.id) || idPenghargaan;

            console.log('savedId ' + savedId);
            
            if ((result.status === '00' || result.status === 'success' || result.id) && savedId) {
                // UPDATE VARIABEL GLOBAL (PENTING!)
                activeEditingPenghargaanId<%=rnd%> = savedId; 
                document.getElementById('inputIdPenghargaan<%=rnd%>').value = savedId;

                if (isNewData) {
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data berhasil disimpan. Silakan unggah berkas bukti di bawah ini.")%>', 'bg-success text-white');
                    document.getElementById('uploadPhotoArea<%=rnd%>').style.display = 'block';
                    document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-edit text-success me-2"></i><%=Common.getBahasaConfig("Unggah Bukti & Edit Karya")%>';
                    loadDataPenghargaan<%=rnd%>(); 
                } else {
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data berhasil diperbarui!")%>', 'bg-success text-white');
                    loadDataPenghargaan<%=rnd%>();
                    
                    if (isDocumentUploaded<%=rnd%>) {
                        tutupForm<%=rnd%>(); 
                    } else {
                        showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data tersimpan. Harap unggah dokumen bukti.")%>', 'bg-info text-white');
                    }
                }
            } else {
                showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan data ke database.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            console.error(e);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi.")%>', 'bg-danger text-white');
        } finally {
            btnSimpan.innerHTML = oriBtn;
            btnSimpan.disabled = false;
        }
    };
    
    const editPenghargaan<%=rnd%> = async (id) => {
        activeEditingPenghargaanId<%=rnd%> = id;
        console.log('id ' + id);
        bukaFormTambah<%=rnd%>();
        document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-edit text-success me-2"></i><%=Common.getBahasaConfig("Edit Karya Mahasiswa")%>';
        document.getElementById('inputIdPenghargaan<%=rnd%>').value = id;

        const payload = { action: "load", class: masterModelClass<%=rnd%>, id: id };
        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const result = await response.json();
            
            if (result.status === '00' || result.status === 'success' || result.id) {
            	
            	activeEditingPenghargaanId<%=rnd%> = id;
                console.log('id ' + id);
            	
                const data = result.data;
                const isApproved = (data.status === 'Disetujui');

                // Fungsi Helper untuk memastikan format YYYY-MM-DD
                const formatKeInputDate = (val) => {
                    if (!val) return '';
                    // Jika val adalah string, ambil 10 karakter pertama (YYYY-MM-DD)
                    if (typeof val === 'string') return val.substring(0, 10);
                    // Jika val adalah angka (timestamp), konversi ke Date objek
                    if (typeof val === 'number') return new Date(val).toISOString().substring(0, 10);
                    return '';
                };

                // 1. Set Data Mahasiswa [cite: 406, 407]
                if(data["mahasiswa.id"]) {
                    document.getElementById('hiddenIdMahasiswa<%=rnd%>').value = data["mahasiswa.id"];
                    <% if (idMhsLogin == null) { %>
                    document.getElementById('inputCariMahasiswa<%=rnd%>').value = (data["mahasiswa.nim"] || '') + ' - ' + (data["mahasiswa.nama"] || '');
                    <% } %>
                }

                // 2. Set Field Text & Angka [cite: 401, 404]
                document.getElementById('inputTahunAkademik<%=rnd%>').value = data.tahunAkademik || '';
                document.getElementById('inputJenisSemester<%=rnd%>').value = data.jenisSemester || '';
                document.getElementById('inputNama<%=rnd%>').value = data.nama || '';
                document.getElementById('inputTahun<%=rnd%>').value = data.tahun || '';
                document.getElementById('inputAlamat<%=rnd%>').value = data.alamat || '';
                document.getElementById('inputNoSk<%=rnd%>').value = data.noSk || '';
                document.getElementById('inputNomorSertifikat<%=rnd%>').value = data.nomorSertifikat || '';
                document.getElementById('inputCapaian<%=rnd%>').value = data.capaian || '';
                document.getElementById('inputUrl<%=rnd%>').value = data.url || '';
                document.getElementById('inputKeterangan<%=rnd%>').value = data.keterangan || '';
                
                if (document.getElementById('inputStatus<%=rnd%>')) {
                    document.getElementById('inputStatus<%=rnd%>').value = data.status || 'Belum diproses';
                }

                // 3. PERBAIKAN: Set Field Tanggal menggunakan Helper [cite: 401, 402, 403]
                // Mencoba mengambil dari datetime_format, jika tidak ada gunakan raw data
                document.getElementById('inputTanggal<%=rnd%>').value = formatKeInputDate(data["tanggal.datetime_format"] || data.tanggal);
                document.getElementById('inputTanggalSelesai<%=rnd%>').value = formatKeInputDate(data["tanggalSelesai.datetime_format"] || data.tanggalSelesai);
                document.getElementById('inputTglSk<%=rnd%>').value = formatKeInputDate(data["tglSk.datetime_format"] || data.tglSk);

                // 4. Set Dosen Pembina [cite: 408, 411]
                if(data["dosenPembina1.id"]) {
                    document.getElementById('hiddenIdDosen1<%=rnd%>').value = data["dosenPembina1.id"];
                    document.getElementById('inputCariDosen1<%=rnd%>').value = (data["dosenPembina1.nidn"] || '-') + ' - ' + (data["dosenPembina1.nama"] || '');
                } else {
                    document.getElementById('hiddenIdDosen1<%=rnd%>').value = '';
                    document.getElementById('inputCariDosen1<%=rnd%>').value = '';
                }

                if(data["dosenPembina2.id"]) {
                    document.getElementById('hiddenIdDosen2<%=rnd%>').value = data["dosenPembina2.id"];
                    document.getElementById('inputCariDosen2<%=rnd%>').value = (data["dosenPembina2.nidn"] || '-') + ' - ' + (data["dosenPembina2.nama"] || '');
                } else {
                    document.getElementById('hiddenIdDosen2<%=rnd%>').value = '';
                    document.getElementById('inputCariDosen2<%=rnd%>').value = '';
                }

                // 5. Load & Set Combobox Masters [cite: 414, 416, 420]
                let idFak = data["fakultas.id"] ? data["fakultas.id"] : null;
                let idJur = data["jurusan.id"] ? data["jurusan.id"] : null;
                
                <% if (idMhsLogin == null) { %>
                await loadFakultas<%=rnd%>('selectFakultas<%=rnd%>', idFak);
                if(idFak) {
                    await loadJurusan<%=rnd%>(idFak, idJur);
                } else {
                    await loadJurusan<%=rnd%>(null, idJur);
                }
                <% } %>

                let idKategori = data["kategoriPenghargaan.id"] ? data["kategoriPenghargaan.id"] : null;
                let idJenis = data["jenisAktfitasMahasiswa.id"] ? data["jenisAktfitasMahasiswa.id"] : null;

                await loadKategori<%=rnd%>('selectKategori<%=rnd%>', idKategori);
                await loadJenisAktifitas<%=rnd%>('selectJenisAktifitas<%=rnd%>', idJenis);

                // 6. Finishing [cite: 421, 423]
                disableFormIfApproved<%=rnd%>(data.status);
                checkExistingFile<%=rnd%>(data.id, isApproved);
                document.getElementById('uploadPhotoArea<%=rnd%>').style.display = 'block';

            } else {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal memuat detail data dari server.")%>', 'bg-danger text-white');
            }
        } catch (error) {
            console.error(error);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi.")%>', 'bg-danger text-white');
        }
    };
    
 // Fungsi untuk memotong Tahun Akademik (misal: "2025/2026" menjadi "2025")
    const autoFillTahun<%=rnd%> = (taValue) => {
        const inputTahun = document.getElementById('inputTahun<%=rnd%>');
        if (taValue && taValue.includes('/')) {
            // Memecah string berdasarkan garis miring dan mengambil bagian pertama
            const tahunAwal = taValue.split('/')[0]; 
            inputTahun.value = tahunAwal;
        } else if (!taValue) {
            // Kosongkan jika user mengembalikan pilihan ke default ("Pilih Tahun Akademik")
            inputTahun.value = ''; 
        }
    };

    const hapusPenghargaan<%=rnd%> = async (id) => {
        if (!confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus data karya mahasiswa ini secara permanen?")%>')) return;
        
        const payload = { action: "hapusDataRinci", class: masterModelClass<%=rnd%>, id: id };
        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const result = await response.json();
            
            if (result.status === '00' || result.status === 'success') {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data berhasil dihapus.")%>', 'bg-success text-white');
                if (document.querySelectorAll('#tabelDataPenghargaan<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) {
                    currentPage<%=rnd%>--;
                }
                loadDataPenghargaan<%=rnd%>();
            } else {
                showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal menghapus data.")%>', 'bg-danger text-white');
            }
        } catch (error) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi.")%>', 'bg-danger text-white');
        }
    };

    // INISIALISASI
    document.addEventListener("DOMContentLoaded", () => {
        document.getElementById('searchPenghargaan<%=rnd%>').addEventListener("keydown", (e) => {
            if (e.key === "Enter") {
                e.preventDefault();
                resetAndLoadData<%=rnd%>(); 
            }
        });

        // Load Master Data untuk Fitur Filter UI Lanjutan
        <% if (idMhsLogin == null) { %>
        loadFakultas<%=rnd%>('filterFakultas<%=rnd%>', null);
        loadFilterJurusan<%=rnd%>(null);
        <% } %>
        
        loadKategori<%=rnd%>('filterKategori<%=rnd%>', null);
        loadJenisAktifitas<%=rnd%>('filterJenis<%=rnd%>', null);
        
        loadDataPenghargaan<%=rnd%>();
    });
</script>