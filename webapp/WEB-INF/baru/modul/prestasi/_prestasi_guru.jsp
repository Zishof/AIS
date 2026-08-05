<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="ais.database.model.sekolah.PrestasiGuru"%>
<%@page import="ais.database.model.sekolah.CabangPrestasiGuru"%>
<%@page import="ais.database.model.sekolah.KategoriPrestasiGuru"%>
<%@page import="ais.database.model.sekolah.Yayasan"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.sekolah.Guru"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
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

// Penyesuaian hak akses berdasarkan level login (Contoh asumsi method getYayasan/getSekolah/getGuru ada di Tbmuser)
Yayasan loginSebagaiYayasan = null; // tbmuser.getYayasan();
Sekolah loginSebagaiSekolah = null; // tbmuser.getSekolah();
Guru loginSebagaiGuru = null;       // tbmuser.getGuru();

Long idGuruLogin = (loginSebagaiGuru != null) ? loginSebagaiGuru.getId() : null;

// Menentukan ID Penguncian untuk Hak Akses Yayasan & Sekolah
Long idYayasanLogin = null;
Long idSekolahLogin = null;

if (loginSebagaiSekolah != null) {
    idSekolahLogin = loginSebagaiSekolah.getId();
    if (loginSebagaiSekolah.getYayasan() != null) {
        idYayasanLogin = loginSebagaiSekolah.getYayasan().getId();
    }
} else if (loginSebagaiYayasan != null) {
    idYayasanLogin = loginSebagaiYayasan.getId();
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
                                <i class="fas fa-trophy text-primary me-2"></i><%=Common.getBahasaConfig("Manajemen Prestasi Guru")%>
                            </h5>
                            <small class="text-muted"><%=Common.getBahasaConfig("Kelola data pencapaian dan prestasi guru pendidik.")%></small>
                        </div>
                        
                        <div class="d-flex align-items-center gap-2 flex-wrap">
                            <div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;">
                                <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>
                                <input type="text" class="form-control border-start-0 ps-0 bg-white fw-medium" id="searchPrestasi<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Prestasi, NUPTK / Nama...")%>">
                            </div>
                            
                            <button class="btn btn-sm btn-outline-info rounded-pill px-3 shadow-sm fw-bold" type="button" data-bs-toggle="collapse" data-bs-target="#filterPanel<%=rnd%>" aria-expanded="false" aria-controls="filterPanel<%=rnd%>">
                                <i class="fas fa-filter me-1"></i><%=Common.getBahasaConfig("Filter")%>
                            </button>

                            <button class="btn btn-sm btn-outline-secondary rounded-pill px-3 shadow-sm fw-bold" onclick="resetAndLoadData<%=rnd%>()" title="<%=Common.getBahasaConfig("Muat Ulang Data")%>">
                                <i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Segarkan")%>
                            </button>

                            <button class="btn btn-sm btn-outline-success rounded-pill px-3 shadow-sm fw-bold" onclick="downloadExcelPrestasi<%=rnd%>()" title="<%=Common.getBahasaConfig("Unduh Data Excel")%>">
                                <i class="fas fa-file-excel me-1"></i><%=Common.getBahasaConfig("Excel")%>
                            </button>
                            
                            <% if (add) { %>
                            <button class="btn btn-sm btn-primary rounded-pill px-4 shadow-sm fw-bold text-nowrap" onclick="bukaFormTambah<%=rnd%>()">
                                <i class="fas fa-plus-circle me-1"></i><%=Common.getBahasaConfig("Tambah Prestasi")%>
                            </button>
                            <% } %>
                        </div>
                    </div>

                    <div class="collapse mt-3" id="filterPanel<%=rnd%>">
                        <div class="card card-body bg-light border-0 shadow-sm rounded-3 p-3">
                            <h6 class="fw-bold text-secondary mb-3"><i class="fas fa-sliders-h me-2"></i><%=Common.getBahasaConfig("Filter Lanjutan")%></h6>
                            <div class="row g-3">
                                <div class="col-md-3">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Tahun Pelajaran")%></label>
                                    <select id="filterTahunAkademik<%=rnd%>" class="form-select form-select-sm shadow-sm">
                                        <option value=""><%=Common.getBahasaConfig("-- Semua TP --")%></option>
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
                                
                                <% if (idGuruLogin == null) { %>
                                <div class="col-md-3">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Yayasan")%></label>
                                    <select id="filterYayasan<%=rnd%>" class="form-select form-select-sm shadow-sm" onchange="loadFilterSekolah<%=rnd%>(this.value)">
                                        <option value=""><%=Common.getBahasaConfig("-- Semua Yayasan --")%></option>
                                    </select>
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Sekolah")%></label>
                                    <select id="filterSekolah<%=rnd%>" class="form-select form-select-sm shadow-sm">
                                        <option value=""><%=Common.getBahasaConfig("-- Semua Sekolah --")%></option>
                                    </select>
                                </div>
                                <% } else { %>
                                    <input type="hidden" id="filterYayasan<%=rnd%>" value="">
                                    <input type="hidden" id="filterSekolah<%=rnd%>" value="">
                                <% } %>

                                <div class="col-md-4">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Status (Multi)")%></label>
                                    <select id="filterStatus<%=rnd%>" class="form-select form-select-sm shadow-sm" multiple size="3">
                                        <option value="Belum diproses">Belum diproses</option>
                                        <option value="Sedang diproses">Sedang diproses</option>
                                        <option value="Disetujui">Disetujui</option>
                                        <option value="Ditolak">Ditolak</option>
                                    </select>
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Kategori (Multi)")%></label>
                                    <select id="filterKategori<%=rnd%>" class="form-select form-select-sm shadow-sm" multiple size="3"></select>
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Cabang (Multi)")%></label>
                                    <select id="filterCabang<%=rnd%>" class="form-select form-select-sm shadow-sm" multiple size="3"></select>
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
                        <table class="table table-hover table-striped align-middle mb-0" id="tabelHTMLDataPrestasi<%=rnd%>">
                            <thead class="table-dark text-center align-middle">
                                <tr>
                                    <th class="fw-semibold text-uppercase small px-3 py-3" style="width: 60px;">#</th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-chalkboard-teacher me-1"></i><%=Common.getBahasaConfig("Guru")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-medal me-1"></i><%=Common.getBahasaConfig("Nama Prestasi")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-list-alt me-1"></i><%=Common.getBahasaConfig("Kategori / Cabang")%></th>
                                    <th class="text-center fw-semibold text-uppercase small px-3"><i class="fas fa-calendar-alt me-1"></i><%=Common.getBahasaConfig("Tanggal")%></th>
                                    <th class="text-center fw-semibold text-uppercase small px-3"><i class="fas fa-info-circle me-1"></i><%=Common.getBahasaConfig("Status")%></th>
                                    <th class="text-center fw-semibold text-uppercase small px-3 no-export"><i class="fas fa-file-alt me-1"></i><%=Common.getBahasaConfig("Bukti")%></th>
                                    <th class="fw-semibold text-uppercase small px-3 text-center no-export" style="width: 120px;"><i class="fas fa-cogs"></i></th>
                                </tr>
                            </thead>
                            <tbody id="tabelDataPrestasi<%=rnd%>">
                                <tr><td colspan="8" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Memuat data prestasi guru...")%></span></td></tr>
                            </tbody>
                        </table>
                    </div>

                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mt-4">
                        <div class="small fw-medium text-muted bg-light px-3 py-1 rounded-pill border" id="pagingInfoPrestasi<%=rnd%>"></div>
                        <nav>
                            <ul class="pagination pagination-sm mb-0 shadow-sm">
                                <li class="page-item"><button class="page-link text-primary rounded-start-pill px-3 fw-bold" id="btnPrevPage<%=rnd%>" onclick="changePagePrestasi<%=rnd%>(-1)"><i class="fas fa-chevron-left me-1"></i></button></li>
                                <li class="page-item disabled"><span class="page-link bg-light text-dark px-3 fw-bold" id="pageNumberDisplay<%=rnd%>">1</span></li>
                                <li class="page-item"><button class="page-link text-primary rounded-end-pill px-3 fw-bold" id="btnNextPage<%=rnd%>" onclick="changePagePrestasi<%=rnd%>(1)"><i class="fas fa-chevron-right ms-1"></i></button></li>
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
                    <div class="d-flex justify-content-between align-items-center">
                        <h5 class="fw-bold text-dark mb-0" id="formTitle<%=rnd%>">
                            <i class="fas fa-edit text-success me-2"></i><%=Common.getBahasaConfig("Formulir Prestasi Guru")%>
                        </h5>
                    </div>
                </div>
                <div class="card-body p-4">
                    <form id="formPrestasi<%=rnd%>" onsubmit="event.preventDefault(); simpanPrestasi<%=rnd%>();">
                        <input type="hidden" id="inputIdPrestasi<%=rnd%>" value="">
                        
                        <div class="row g-4 mt-1" id="formContainer<%=rnd%>">
                            
                            <div class="col-md-12">
                                <h6 class="fw-bold text-primary border-bottom pb-2 mb-0"><i class="fas fa-chalkboard-teacher me-2"></i>1. <%=Common.getBahasaConfig("Data Guru & Penempatan")%></h6>
                            </div>
                            
                            <% if (idGuruLogin == null) { %>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Cari Guru Utama (NUPTK / Nama)")%> <span class="text-danger">*</span></label>
                                <input type="text" class="form-control fw-bold shadow-sm field-to-disable<%=rnd%>" id="inputCariGuruUtama<%=rnd%>" list="dlGuruUtama<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik NUPTK atau Nama untuk mencari...")%>" oninput="onInputGuru<%=rnd%>(this.value, 'Utama')" onchange="checkSelectedGuru<%=rnd%>(this.value, 'Utama')" autocomplete="off" required>
                                <datalist id="dlGuruUtama<%=rnd%>"></datalist>
                                <input type="hidden" id="hiddenIdGuruUtama<%=rnd%>" required>
                            </div>
                            
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Yayasan (Opsional)")%></label>
                                <select class="form-select shadow-sm bg-light field-to-disable<%=rnd%>" id="selectYayasan<%=rnd%>" onchange="loadSekolah<%=rnd%>(this.value, null)">
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Yayasan --")%></option>
                                </select>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Sekolah (Opsional)")%></label>
                                <select class="form-select shadow-sm bg-light field-to-disable<%=rnd%>" id="selectSekolah<%=rnd%>">
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Sekolah --")%></option>
                                </select>
                            </div>
                            <% } else { %>
                                <input type="hidden" id="hiddenIdGuruUtama<%=rnd%>" value="<%=idGuruLogin%>">
                                <input type="hidden" id="selectYayasan<%=rnd%>" value="">
                                <input type="hidden" id="selectSekolah<%=rnd%>" value="">
                            <% } %>

                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tahun Pelajaran")%> <span class="text-danger">*</span></label>
                                <select class="form-select shadow-sm field-to-disable<%=rnd%>" id="inputTahunAkademik<%=rnd%>" required>
                                    <option value=""><%= Common.getBahasaConfig("Pilih Tahun Pelajaran") %></option>
                                    <% if (Common.tahunAngkatans != null) { 
                                        for (String ta : Common.tahunAngkatans) { %>
                                            <option value="<%= ta %>"><%= ta %></option>
                                    <%  } } %>
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
                                <h6 class="fw-bold text-primary border-bottom pb-2 mb-0"><i class="fas fa-info-circle me-2"></i>2. <%=Common.getBahasaConfig("Detail Prestasi Utama")%></h6>
                            </div>
                            
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Prestasi / Kegiatan")%> <span class="text-danger">*</span></label>
                                <input type="text" class="form-control fw-bold shadow-sm field-to-disable<%=rnd%>" id="inputNama<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: Guru Teladan Tingkat Provinsi")%>" required>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Prestasi (Bahasa Inggris)")%></label>
                                <input type="text" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputNamaEn<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: Exemplary Teacher at Provincial Level")%>">
                            </div>

                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Penyelenggara")%></label>
                                <input type="text" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputPenyelenggara<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Nama Institusi Penyelenggara")%>">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Juara (Cth: Harapan 1)")%></label>
                                <input type="text" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputJuara<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: Harapan I")%>">
                            </div>
                            <div class="col-md-2">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Peringkat (Angka)")%></label>
                                <input type="number" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputPeringkat<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: 1")%>">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tahun")%></label>
                                <input type="number" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputTahun<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: 2024")%>">
                            </div>

                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal Mulai / Pelaksanaan")%></label>
                                <input type="date" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputTanggal<%=rnd%>">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal Selesai")%></label>
                                <input type="date" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputTanggalSelesai<%=rnd%>">
                            </div>
                            <div class="col-md-3 d-flex align-items-end">
                                <div class="form-check form-switch fs-5 mb-1">
                                    <input class="form-check-input field-to-disable<%=rnd%>" type="checkbox" id="inputPrestasiLuarKampus<%=rnd%>" checked>
                                    <label class="form-check-label ms-2 fs-6 small fw-bold text-secondary" for="inputPrestasiLuarKampus<%=rnd%>"><%=Common.getBahasaConfig("Prestasi Luar Sekolah")%></label>
                                </div>
                            </div>

                            <div class="col-md-12">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tempat / Lokasi Penyelenggaraan")%></label>
                                <textarea class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputTempat<%=rnd%>" rows="2" placeholder="<%=Common.getBahasaConfig("Tempat kegiatan berlangsung")%>"></textarea>
                            </div>


                            <div class="col-md-12 mt-4">
                                <h6 class="fw-bold text-primary border-bottom pb-2 mb-0"><i class="fas fa-layer-group me-2"></i>3. <%=Common.getBahasaConfig("Klasifikasi & Informasi Pendukung")%></h6>
                            </div>

                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Kategori Prestasi")%></label>
                                <select class="form-select shadow-sm field-to-disable<%=rnd%>" id="selectKategori<%=rnd%>">
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Kategori --")%></option>
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Cabang Prestasi")%></label>
                                <select class="form-select shadow-sm field-to-disable<%=rnd%>" id="selectCabang<%=rnd%>">
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Cabang --")%></option>
                                </select>
                            </div>

                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Guru Pembina Ke-2 (Opsional)")%></label>
                                <input type="text" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputCariGuru2<%=rnd%>" list="dlGuru2<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik NUPTK / Nama Guru...")%>" oninput="onInputGuru<%=rnd%>(this.value, '2')" onchange="checkSelectedGuru<%=rnd%>(this.value, '2')" autocomplete="off">
                                <datalist id="dlGuru2<%=rnd%>"></datalist>
                                <input type="hidden" id="hiddenIdGuru2<%=rnd%>">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Guru Pembina Ke-3 (Opsional)")%></label>
                                <input type="text" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputCariGuru3<%=rnd%>" list="dlGuru3<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik NUPTK / Nama Guru...")%>" oninput="onInputGuru<%=rnd%>(this.value, '3')" onchange="checkSelectedGuru<%=rnd%>(this.value, '3')" autocomplete="off">
                                <datalist id="dlGuru3<%=rnd%>"></datalist>
                                <input type="hidden" id="hiddenIdGuru3<%=rnd%>">
                            </div>

                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("No. Sertifikat")%></label>
                                <input type="text" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputNomorSertifikat<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Nomor Sertifikat")%>">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jumlah Peserta")%></label>
                                <input type="text" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputJumlahPeserta<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: 150 Orang")%>">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Capaian")%></label>
                                <input type="text" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputCapaian<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Capaian yang diperoleh")%>">
                            </div>
                            
                            <div class="col-md-12">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("URL / Link Publikasi")%></label>
                                <input type="url" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputUrl<%=rnd%>" placeholder="<%=Common.getBahasaConfig("https://...")%>">
                            </div>
                            <div class="col-md-12">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Keterangan Tambahan")%></label>
                                <textarea class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputKeterangan<%=rnd%>" rows="3" placeholder="<%=Common.getBahasaConfig("Catatan atau deskripsi tambahan...")%>"></textarea>
                            </div>

                            <% if (idGuruLogin == null) { %>
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
                                    <label class="form-label fw-bold text-secondary d-block"><i class="fas fa-file-pdf text-danger me-2"></i><%=Common.getBahasaConfig("Bukti Prestasi (Sertifikat/Dokumen)")%></label>
                                    <div class="d-flex justify-content-center align-items-center gap-2 mt-2" id="action-buttons-<%=rnd%>"></div>
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
    const masterModelClass<%=rnd%> = "<%=PrestasiGuru.class.getName()%>";
    let activeEditingPrestasiId<%=rnd%> = "";
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

        <% if (idGuruLogin != null && (add || edit)) { %>
            document.getElementById('btnSimpan<%=rnd%>').disabled = isApproved;
        <% } %>
        
        if (isApproved) {
             document.getElementById('lbl-info-upload-<%=rnd%>').style.display = 'none';
        } else {
             document.getElementById('lbl-info-upload-<%=rnd%>').style.display = 'block';
        }
    };

    const downloadExcelPrestasi<%=rnd%> = () => {
        const table = document.getElementById('tabelHTMLDataPrestasi<%=rnd%>');
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

        const wb = XLSX.utils.table_to_book(cloneTable, {sheet: "<%=Common.getBahasaConfig("Data Prestasi Guru")%>"});
        XLSX.writeFile(wb, "Data_Prestasi_Guru_<%=System.currentTimeMillis()%>.xlsx");
    };

    // Fungsi Cek File Existing
    async function checkExistingFile<%=rnd%>(idPrestasi, isApproved) {
        var reqObj = { 
            "action": "file", 
            "class": "<%=ais.database.model.file.LampiranLain.class.getName()%>", 
            "ref": idPrestasi.toString(), 
            "jenis": "<%=PrestasiGuru.class.getName()%>",
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
            console.error("Gagal mengambil info file:", error);
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
            html = '<a href="' + url + '" target="_blank" title="<%=Common.getBahasaConfig("Klik untuk memperbesar")%>">' +
                   '  <img src="' + url + '" class="img-fluid rounded shadow-sm" style="max-height: 250px; object-fit: cover; border: 2px solid #ddd;" alt="' + filename + '">' +
                   '</a>';
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

            html = '<a href="' + url + '" target="_blank" class="text-decoration-none" title="<%=Common.getBahasaConfig("Buka / Download")%>">' +
                   '  <i class="fas ' + iconClass + ' fa-4x p-3 bg-white rounded shadow-sm border"></i>' +
                   '</a>';
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
                loadDataPrestasi<%=rnd%>(); 
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
    const loadYayasan<%=rnd%> = async (targetId, selectedId) => {
        var reqObj = { 
            "action": "daftar", 
            "class": "<%=Yayasan.class.getName()%>", 
            "where1": "aktif = true or aktif is null",
            "order1": "asc", 
            "sort1": "nama" 
        };
        <% if (idYayasanLogin != null) { %>
            reqObj.where1 += " AND id = <%=idYayasanLogin%>";
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
                if(targetId === 'selectYayasan<%=rnd%>') {
                    document.getElementById(targetId).disabled = true;
                }
            } else {
                options = '<option value="">-- ' + (targetId.includes('filter') ? '<%=Common.getBahasaConfigJS("Semua Yayasan")%>' : '<%=Common.getBahasaConfigJS("Pilih Yayasan")%>') + ' --</option>';
                data.forEach((row) => {
                    const selected = (selectedId && selectedId == row.id) ? "selected" : "";
                    options += '<option value="' + row.id + '" ' + selected + '>' + row.nama + '</option>';
                });
            }
            document.getElementById(targetId).innerHTML = options;
        } catch (e) { console.error("Error load Yayasan", e); }
    };

    const loadFilterSekolah<%=rnd%> = async (idYayasan) => {
        var reqObj = { 
            "action": "daftar", 
            "class": "<%=Sekolah.class.getName()%>", 
            "where1": "aktif = true or aktif is null",
            "order1": "asc", 
            "sort1": "nama" 
        };

        let filterWhere = [];
        <% if (idSekolahLogin != null) { %>
            filterWhere.push("id = <%=idSekolahLogin%>");
        <% } else { %>
            if(idYayasan) {
                filterWhere.push("yayasan_id = " + idYayasan);
            } <% if (idYayasanLogin != null) { %> else {
                filterWhere.push("yayasan_id = <%=idYayasanLogin%>");
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
                options = '<option value="">-- <%=Common.getBahasaConfig("Semua Sekolah")%> --</option>';
                data.forEach((row) => {
                    options += '<option value="' + row.id + '">' + row.nama + '</option>';
                });
            }
            document.getElementById('filterSekolah<%=rnd%>').innerHTML = options;
        } catch (e) { console.error("Error load Filter Sekolah", e); }
    };

    const loadSekolah<%=rnd%> = async (idYayasan, selectedId) => {
        var reqObj = { 
            "action": "daftar", 
            "class": "<%=Sekolah.class.getName()%>", 
            "where1": "aktif = true or aktif is null",
            "order1": "asc", 
            "sort1": "nama" 
        };

        let filterWhere = [];
        <% if (idSekolahLogin != null) { %>
            filterWhere.push("id = <%=idSekolahLogin%>");
        <% } else { %>
            if(idYayasan) {
                filterWhere.push("yayasan_id = " + idYayasan);
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
                document.getElementById('selectSekolah<%=rnd%>').disabled = true;
            } else {
        	    document.getElementById('selectSekolah<%=rnd%>').disabled = false;
                options = '<option value="">-- <%=Common.getBahasaConfig("Pilih Sekolah")%> --</option>'; 
                data.forEach((row) => {
                    const selected = (selectedId && selectedId == row.id) ? "selected" : "";
                    options += '<option value="' + row.id + '" ' + selected + '>' + row.nama + '</option>';
                });
            }
            document.getElementById('selectSekolah<%=rnd%>').innerHTML = options;
        } catch (e) { console.error("Error load Sekolah", e); }
    };

    const loadCabang<%=rnd%> = async (targetId, selectedId) => {
        var reqObj = { 
            "action": "daftar", 
            "class": "<%=CabangPrestasiGuru.class.getName()%>", 
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
            
            let options = targetId.includes('filter') ? '' : '<option value="">-- <%=Common.getBahasaConfig("Pilih Cabang")%> --</option>';
            data.forEach((row) => {
                const selected = (selectedId && selectedId == row.id) ? "selected" : "";
                options += '<option value="' + row.id + '" ' + selected + '>' + row.nama + '</option>';
            });
            document.getElementById(targetId).innerHTML = options;
        } catch (e) { console.error("Error load Cabang", e); }
    };

    const loadKategori<%=rnd%> = async (targetId, selectedId) => {
        var reqObj = { 
            "action": "daftar", 
            "class": "<%=KategoriPrestasiGuru.class.getName()%>", 
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

    // Pencarian Guru Datalist (Bisa untuk Utama, 2, atau 3)
    const onInputGuru<%=rnd%> = async (val, tipe) => {
        if (val.length < 2) return;
        var reqObj = { 
            "action": "daftar", 
            "class": "<%=Guru.class.getName()%>", 
            "where1": "aktif = true AND (nuptk ILIKE '%" + val + "%' OR nama_guru ILIKE '%" + val + "%')",
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
            	options += '<option data-id="' + row.id + '" value="' + (row.nuptk || '-') + ' - ' + (row.namaGuru || row.nama) + '"></option>';
            });
            document.getElementById('dlGuru' + tipe + '<%=rnd%>').innerHTML = options;
        } catch (e) { console.error("Error search Guru", e); }
    };

    const checkSelectedGuru<%=rnd%> = (val, tipe) => {
        const dl = document.getElementById('dlGuru' + tipe + '<%=rnd%>');
        const options = dl.options;
        for(let i=0; i<options.length; i++) {
            if(options[i].value === val) {
                document.getElementById('hiddenIdGuru' + tipe + '<%=rnd%>').value = options[i].getAttribute('data-id');
                return;
            }
        }
        document.getElementById('hiddenIdGuru' + tipe + '<%=rnd%>').value = '';
    };

    // ==========================================
    // BAGIAN 3: LOGIKA LIST, PAGING & SEARCH
    // ==========================================
    const buildFilterSQL<%=rnd%> = () => {
        const keyword = document.getElementById('searchPrestasi<%=rnd%>').value.trim().replace(/'/g, "''");
        let filters = [];
        
        <% if (idGuruLogin != null) { %>
        filters.push("guru_id = <%=idGuruLogin%>");
	    <% } else { %>
	        <% if (idSekolahLogin != null) { %>
	            filters.push("sekolah_id = <%=idSekolahLogin%>");
	        <% } else if (idYayasanLogin != null) { %>
	            filters.push("yayasan_id = <%=idYayasanLogin%>");
	        <% } %>
	    <% } %>

        if (keyword !== '') {
        	filters.push("(this_.nama ILIKE '%" + keyword + "%' OR guru1_.nuptk ILIKE '%" + keyword + "%' OR guru1_.nama_guru ILIKE '%" + keyword + "%')");
        }
        
        const fTa = document.getElementById('filterTahunAkademik<%=rnd%>').value;
        if(fTa) filters.push("this_.tahunAkademik = '" + fTa + "'");
        
        const fSmt = document.getElementById('filterSemester<%=rnd%>').value;
        if(fSmt) filters.push("this_.jenisSemester = '" + fSmt + "'");

        <% if (idGuruLogin == null) { %>
            const fYayasan = document.getElementById('filterYayasan<%=rnd%>').value;
            if(fYayasan) filters.push("this_.yayasan_id = " + fYayasan);
            
            const fSekolah = document.getElementById('filterSekolah<%=rnd%>').value;
            if(fSekolah) filters.push("this_.sekolah_id = " + fSekolah);
        <% } %>
        
        const cbg = getMultiSelectValues<%=rnd%>('filterCabang<%=rnd%>');
        if(cbg) filters.push("this_.cabang_prestasi_guru_id IN (" + cbg + ")");
        
        const kat = getMultiSelectValues<%=rnd%>('filterKategori<%=rnd%>');
        if(kat) filters.push("this_.kategori_prestasi_guru_id IN (" + kat + ")");

        const stt = getMultiSelectStringValues<%=rnd%>('filterStatus<%=rnd%>');
        if(stt) filters.push("this_.status IN (" + stt + ")");

        return filters.length > 0 ? filters.join(' AND ') : '';
    };

    const resetAndLoadData<%=rnd%> = () => {
        currentPage<%=rnd%> = 1; 
        loadDataPrestasi<%=rnd%>();
    };

    const loadDataPrestasi<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataPrestasi<%=rnd%>');
        const keyword = document.getElementById('searchPrestasi<%=rnd%>').value.trim().replace(/'/g, "''");
        tbody.innerHTML = '<tr><td colspan="8" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Mengambil data...")%></span></td></tr>';
        
        const start = (currentPage<%=rnd%> - 1);
        const whereClause = buildFilterSQL<%=rnd%>();

        var reqObj = {
            "action": "daftar", 
            "class": "<%=PrestasiGuru.class.getName()%>",
            "deep": "1", 
            "max": limitPerPage<%=rnd%>, 
            "halaman": start,
            "order1": "desc", 
            "sort1": "id", 
            "count": "true"
        };

        if (keyword !== '') {
        	reqObj["alias1"] = "guru";
        }
        
        if (whereClause) {
            reqObj["where1"] = whereClause;
        }

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
                htmlList = '<tr><td colspan="8" class="text-center text-muted py-5"><i class="fas fa-box-open fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Tidak ada data prestasi guru ditemukan.")%></td></tr>';
            } else {
                let no = (start * limitPerPage<%=rnd%>) + 1;
                
                const filePromises = recordList.map(async (row) => {
                    var fileReq = { 
                        "action": "file", 
                        "class": "<%=ais.database.model.file.LampiranLain.class.getName()%>", 
                        "ref": row.id.toString(), 
                        "jenis": "<%=PrestasiGuru.class.getName()%>",
                        "kondisiTambahan": "",
                        "refresh": "false",
                        "usingId": false
                    };
                    try {
                        const fileRes = await fetch(servletUrl, {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify(fileReq)
                        });
                        const fData = await fileRes.json();
                        if(fData.status === '00' && fData.data) {
                            row.lampiranData = fData.data;
                        }
                    } catch(e) {}
                    return row;
                });

                await Promise.all(filePromises);

                recordList.forEach((row) => {
                    const safeNama = (row.nama || '-').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    const guruNuptk = row["guru.nuptk"] || '-';
                    const guruNama = row["guru.namaGuru"] || row["guru.nama"] || '-';
                    const kategoriNama = row["kategoriPrestasiGuru.nama"] || '-';
                    const tgl = row["tanggal.formated2"] || row["tanggal.datetime_format"] || row["tanggal"] || '-';
                    const statusVal = row.status || 'Belum diproses';
                    
                    const infoGuru = '<div class="fw-bold text-dark fs-6">' + guruNama + '</div>' + 
                                     '<small class="text-muted"><i class="fas fa-id-card me-1"></i>NUPTK: ' + guruNuptk + '</small>';

                    let statusBadge = '<span class="badge bg-secondary">' + statusVal + '</span>';
                    if (statusVal === 'Disetujui') {
                        statusBadge = '<span class="badge bg-success">' + statusVal + '</span>';
                    } else if (statusVal === 'Ditolak') {
                        statusBadge = '<span class="badge bg-danger">' + statusVal + '</span>';
                    } else if (statusVal === 'Sedang diproses') {
                        statusBadge = '<span class="badge bg-info">' + statusVal + '</span>';
                    }

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
                    actionBtns += '<button class="btn btn-sm btn-outline-warning text-dark shadow-sm px-2 me-1 fw-bold" onclick="editPrestasi<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Edit / Lihat Detail")%>"><i class="fas fa-edit"></i></button>';
                    <% } %>

                    <% if (delete) { %>
                        <% if (idGuruLogin != null) { %>
                            if (statusVal !== 'Disetujui') {
                                 actionBtns += '<button class="btn btn-sm btn-outline-danger shadow-sm px-2 fw-bold" onclick="hapusPrestasi<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Hapus Data")%>"><i class="fas fa-trash-alt"></i></button>';
                            }
                        <% } else { %>
                            actionBtns += '<button class="btn btn-sm btn-outline-danger shadow-sm px-2 fw-bold" onclick="hapusPrestasi<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Hapus Data")%>"><i class="fas fa-trash-alt"></i></button>';
                        <% } %>
                    <% } %>

                    htmlList += '<tr>' +
                            '<td class="text-center text-muted fw-medium">' + (no++) + '</td>' +
                            '<td class="text-start">' + infoGuru + '</td>' +
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
            console.error("Gagal load tabel prestasi:", error);
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal menarik data. Silakan muat ulang.")%></td></tr>';
        }
    };

    const updatePaginationUI<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumberDisplay<%=rnd%>').innerText = currentPage<%=rnd%> + " / " + totalPages;
        
        const startIdx = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%> + 1;
        const endIdx = Math.min(currentPage<%=rnd%> * limitPerPage<%=rnd%>, totalRecords<%=rnd%>);
        const startInfo = totalRecords<%=rnd%> > 0 ? startIdx : 0;
        
        document.getElementById('pagingInfoPrestasi<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startInfo + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari")%> <b class="text-dark">' + totalRecords<%=rnd%> + '</b> <%=Common.getBahasaConfig("data")%>';
        document.getElementById('btnPrevPage<%=rnd%>').disabled = (currentPage<%=rnd%> === 1);
        document.getElementById('btnNextPage<%=rnd%>').disabled = (currentPage<%=rnd%> === totalPages);
    };

    const changePagePrestasi<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if (direction === -1 && currentPage<%=rnd%> > 1) {
            currentPage<%=rnd%>--;
            loadDataPrestasi<%=rnd%>();
        } else if (direction === 1 && currentPage<%=rnd%> < totalPages) {
            currentPage<%=rnd%>++;
            loadDataPrestasi<%=rnd%>();
        }
    };

    // ==========================================
    // BAGIAN 4: LOGIKA FORM & UPLOAD
    // ==========================================
    const bukaFormTambah<%=rnd%> = () => {
        document.getElementById('inputIdPrestasi<%=rnd%>').value = '';
        document.getElementById('formPrestasi<%=rnd%>').reset();
        activeEditingPrestasiId<%=rnd%> = "";
        isDocumentUploaded<%=rnd%> = false; 
        disableFormIfApproved<%=rnd%>('Belum diproses');
        
        <% if (idGuruLogin == null) { %>
            document.getElementById('inputCariGuruUtama<%=rnd%>').value = '';
            document.getElementById('hiddenIdGuruUtama<%=rnd%>').value = '';
            if (document.getElementById('inputStatus<%=rnd%>')) {
                document.getElementById('inputStatus<%=rnd%>').value = 'Belum diproses';
            }
            loadYayasan<%=rnd%>('selectYayasan<%=rnd%>', null);
            loadSekolah<%=rnd%>(null, null);
        <% } else { %>
            if (document.getElementById('inputStatus<%=rnd%>')) {
                document.getElementById('inputStatus<%=rnd%>').value = 'Belum diproses';
            }
        <% } %>
        
        document.getElementById('inputTahunAkademik<%=rnd%>').value = '<%=selectedTa%>';
        document.getElementById('inputJenisSemester<%=rnd%>').value = '<%=selectedSmt%>';
        
        document.getElementById('inputCariGuru2<%=rnd%>').value = '';
        document.getElementById('hiddenIdGuru2<%=rnd%>').value = '';
        document.getElementById('inputCariGuru3<%=rnd%>').value = '';
        document.getElementById('hiddenIdGuru3<%=rnd%>').value = '';

        loadCabang<%=rnd%>('selectCabang<%=rnd%>', null);
        loadKategori<%=rnd%>('selectKategori<%=rnd%>', null);

        document.getElementById('uploadPhotoArea<%=rnd%>').style.display = 'none'; 
        document.getElementById('preview-area-<%=rnd%>').style.display = 'none';
        document.getElementById('preview-area-<%=rnd%>').innerHTML = '';
        resetUploadButtons<%=rnd%>(false);

        document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-plus-circle text-primary me-2"></i><%=Common.getBahasaConfig("Tambah Data Prestasi Guru")%>';
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    };

    const tutupForm<%=rnd%> = () => {
        document.getElementById('viewForm<%=rnd%>').style.display = 'none';
        document.getElementById('viewList<%=rnd%>').style.display = 'block';
        loadDataPrestasi<%=rnd%>();
    };

    const uploadFile<%=rnd%> = async () => {
        const fileInput = document.getElementById('input_dofile_foto_<%=rnd%>');
        if (fileInput.files.length === 0) return;
        if (!activeEditingPrestasiId<%=rnd%>) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Harap simpan data prestasi terlebih dahulu sebelum unggah bukti!")%>', 'bg-warning text-dark');
            return;
        }

        const file = fileInput.files[0];
        const localData = {
            url: URL.createObjectURL(file),
            nama: file.name,
            id: 'temp'
        };
        generatePreview<%=rnd%>(localData, false);
        
        const formData = new FormData();
        formData.append('nama', file.name);
        formData.append('clazz', '<%=ais.database.model.file.LampiranLain.class.getName()%>');
        formData.append('jenis', masterModelClass<%=rnd%>);
        formData.append('id', activeEditingPrestasiId<%=rnd%>);
        formData.append('tanpaLogin', 'true');
        formData.append('fileContent', file);

        const progressBar = document.getElementById('progress-bar-<%=rnd%>');
        const progressWrapper = document.getElementById('progress-wrapper-<%=rnd%>');
        
        progressWrapper.style.display = 'block';
        progressBar.style.width = '20%';
        progressBar.innerText = '<%=Common.getBahasaConfigJS("Mengunggah...")%>';
        progressBar.className = 'progress-bar bg-primary progress-bar-striped progress-bar-animated fw-bold';

        try {
            const response = await fetch('<%=Common.ROOT%>/DoUpload', {
                method: 'POST',
                body: formData
            });
            const result = await response.json();
            
            if(result.status === 'Sukses' || result.status === '00' || result.keterangan === 'Sukses'){
                isDocumentUploaded<%=rnd%> = true;
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Bukti prestasi berhasil diunggah!")%>', 'bg-success text-white');
                progressBar.style.width = '100%';
                progressBar.innerText = '<%=Common.getBahasaConfigJS("Selesai")%>';
                progressBar.className = 'progress-bar bg-success fw-bold';
                checkExistingFile<%=rnd%>(activeEditingPrestasiId<%=rnd%>, false);
                setTimeout(() => { progressWrapper.style.display = 'none'; }, 2000);
            } else {
                showToast<%=rnd%>(result.keterangan || '<%=Common.getBahasaConfigJS("Gagal mengunggah file ke database.")%>', 'bg-danger text-white');
                $('#preview-area-<%=rnd%>').html('').hide(); 
                resetUploadButtons<%=rnd%>(false);
            }
        } catch (err) {
            console.error(err);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Koneksi bermasalah saat proses unggah.")%>', 'bg-danger text-white');
             $('#preview-area-<%=rnd%>').html('').hide();
             resetUploadButtons<%=rnd%>(false);
        }
        fileInput.value = '';
    };

    // ==========================================
    // BAGIAN 5: SIMPAN, EDIT, & HAPUS
    // ==========================================
    const simpanPrestasi<%=rnd%> = async () => {
        const idPrestasi = document.getElementById('inputIdPrestasi<%=rnd%>').value;
        const btnSimpan = document.getElementById('btnSimpan<%=rnd%>');
        
        if (idPrestasi !== '' && !isDocumentUploaded<%=rnd%>) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal menyimpan: Bukti prestasi wajib diunggah sebelum dapat ditutup!")%>', 'bg-danger text-white');
            return;
        }
        
        const guruVal = document.getElementById('hiddenIdGuruUtama<%=rnd%>').value;
        if(!guruVal) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Harap isi dan pilih data Guru dengan benar.")%>', 'bg-warning text-dark');
            return;
        }

        const dataObj = {
            nama: document.getElementById('inputNama<%=rnd%>').value.trim(),
            namaEn: document.getElementById('inputNamaEn<%=rnd%>').value.trim() || null,
            tempat: document.getElementById('inputTempat<%=rnd%>').value.trim() || null,
            penyelenggara: document.getElementById('inputPenyelenggara<%=rnd%>').value.trim(),
            juara: document.getElementById('inputJuara<%=rnd%>').value.trim(),
            peringkat: document.getElementById('inputPeringkat<%=rnd%>').value ? parseInt(document.getElementById('inputPeringkat<%=rnd%>').value) : null,
            tanggal: document.getElementById('inputTanggal<%=rnd%>').value || null,
            tanggalSelesai: document.getElementById('inputTanggalSelesai<%=rnd%>').value || null,
            tahun: document.getElementById('inputTahun<%=rnd%>').value ? parseInt(document.getElementById('inputTahun<%=rnd%>').value) : null,
            tahunAkademik: document.getElementById('inputTahunAkademik<%=rnd%>').value || null,
            jenisSemester: document.getElementById('inputJenisSemester<%=rnd%>').value || null,
            prestasiLuarKampus: document.getElementById('inputPrestasiLuarKampus<%=rnd%>').checked,
            nomorSertifikat: document.getElementById('inputNomorSertifikat<%=rnd%>').value.trim() || null,
            jumlahPeserta: document.getElementById('inputJumlahPeserta<%=rnd%>').value.trim() || null,
            capaian: document.getElementById('inputCapaian<%=rnd%>').value.trim() || null,
            url: document.getElementById('inputUrl<%=rnd%>').value.trim() || null,
            keterangan: document.getElementById('inputKeterangan<%=rnd%>').value.trim() || null
        };
        
        if (document.getElementById('inputStatus<%=rnd%>')) {
            dataObj.status = document.getElementById('inputStatus<%=rnd%>').value;
        }
        
        // Reset properties mapping
        dataObj.guru = "null";
        dataObj.yayasan = "null";
        dataObj.sekolah = "null";
        dataObj.cabangPrestasiGuru = "null";
        dataObj.kategoriPrestasiGuru = "null";
        
        // Guru Utama
        dataObj.guru = parseInt(guruVal);
        
        // Asumsi: mapping property untuk field custom (hanya tersimpan jika di model java telah ditambahkan field guruPembina2/guruPembina3)
        const guru2Val = document.getElementById('hiddenIdGuru2<%=rnd%>').value;
        dataObj.guruPembina2 = guru2Val ? parseInt(guru2Val) : null; 
        
        const guru3Val = document.getElementById('hiddenIdGuru3<%=rnd%>').value;
        dataObj.guruPembina3 = guru3Val ? parseInt(guru3Val) : null; 
        
        <% if (idGuruLogin == null) { %>
            const yayasanVal = document.getElementById('selectYayasan<%=rnd%>').value;
            dataObj.yayasan = yayasanVal ? parseInt(yayasanVal) : null;
            
            const sekolahVal = document.getElementById('selectSekolah<%=rnd%>').value;
            dataObj.sekolah = sekolahVal ? parseInt(sekolahVal) : null;
        <% } %>
        
        const cbgVal = document.getElementById('selectCabang<%=rnd%>').value;
        dataObj.cabangPrestasiGuru = cbgVal ? parseInt(cbgVal) : null;
        
        const katVal = document.getElementById('selectKategori<%=rnd%>').value;
        dataObj.kategoriPrestasiGuru = katVal ? parseInt(katVal) : null;

        const payload = { 
            action: "simpanDataRinci", 
            log: "true",
            class: masterModelClass<%=rnd%>, 
            data: dataObj
        };

        if (idPrestasi !== '') {
            payload.id = idPrestasi;
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
            
            if (result.status === '00' || result.status === 'success' || result.id) {
                if (!isDocumentUploaded<%=rnd%>) {
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data berhasil disimpan. WAJIB unggah Bukti Prestasi!")%>', 'bg-warning text-dark');
                    let newId = result.id || idPrestasi;
                    if(newId) {
                        activeEditingPrestasiId<%=rnd%> = newId;
                        document.getElementById('inputIdPrestasi<%=rnd%>').value = newId;
                        document.getElementById('uploadPhotoArea<%=rnd%>').style.display = 'block';
                        document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-edit text-warning me-2"></i><%=Common.getBahasaConfig("Unggah Bukti Prestasi")%>';
                    }
                    loadDataPrestasi<%=rnd%>();
                } else {
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data prestasi berhasil disimpan!")%>', 'bg-success text-white');
                    tutupForm<%=rnd%>(); 
                }
            } else {
                showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan data ke database.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            console.error(e);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi saat menyimpan.")%>', 'bg-danger text-white');
        } finally {
            btnSimpan.innerHTML = oriBtn;
            btnSimpan.disabled = false;
        }
    };

    const editPrestasi<%=rnd%> = async (idPrestasi) => {
        const payload = { 
            action: "load", 
            class: masterModelClass<%=rnd%>, 
            id: idPrestasi
        };

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const result = await response.json();
            
            if (result.status === '00' || result.status === 'success' || result.id) {
                const data = result.data;
                const isApproved = (data.status === 'Disetujui');
                
                activeEditingPrestasiId<%=rnd%> = data.id; 
                
                document.getElementById('inputIdPrestasi<%=rnd%>').value = data.id;
                document.getElementById('inputNama<%=rnd%>').value = data.nama || '';
                document.getElementById('inputNamaEn<%=rnd%>').value = data.namaEn || '';
                document.getElementById('inputTempat<%=rnd%>').value = data.tempat || '';
                document.getElementById('inputTanggal<%=rnd%>').value = data["tanggal.datetime_format"] || '';
                document.getElementById('inputTanggalSelesai<%=rnd%>').value = data["tanggalSelesai.datetime_format"] || '';
                document.getElementById('inputPenyelenggara<%=rnd%>').value = data.penyelenggara || '';
                document.getElementById('inputJuara<%=rnd%>').value = data.juara || '';
                document.getElementById('inputPeringkat<%=rnd%>').value = data.peringkat || '';

                document.getElementById('inputTahunAkademik<%=rnd%>').value = data.tahunAkademik || '';
                document.getElementById('inputJenisSemester<%=rnd%>').value = data.jenisSemester || '';
                document.getElementById('inputTahun<%=rnd%>').value = data.tahun || '';
                document.getElementById('inputPrestasiLuarKampus<%=rnd%>').checked = (data.prestasiLuarKampus === true || data.prestasiLuarKampus === 'true');
                
                document.getElementById('inputNomorSertifikat<%=rnd%>').value = data.nomorSertifikat || '';
                document.getElementById('inputJumlahPeserta<%=rnd%>').value = data.jumlahPeserta || '';
                document.getElementById('inputCapaian<%=rnd%>').value = data.capaian || '';
                document.getElementById('inputUrl<%=rnd%>').value = data.url || '';
                document.getElementById('inputKeterangan<%=rnd%>').value = data.keterangan || '';
                
                if (document.getElementById('inputStatus<%=rnd%>')) {
                    document.getElementById('inputStatus<%=rnd%>').value = data.status || 'Belum diproses';
                }

                if(data["guru.id"]) {
                    document.getElementById('hiddenIdGuruUtama<%=rnd%>').value = data["guru.id"];
                    <% if (idGuruLogin == null) { %>
                    document.getElementById('inputCariGuruUtama<%=rnd%>').value = (data["guru.nuptk"] || '') + ' - ' + (data["guru.nama"] || '');
                    <% } %>
                }

                // Asumsi mapping view untuk Guru Pembina 2 & 3 jika ditarik dari database
                if(data["guruPembina2.id"]) {
                    document.getElementById('hiddenIdGuru2<%=rnd%>').value = data["guruPembina2.id"];
                    document.getElementById('inputCariGuru2<%=rnd%>').value = (data["guruPembina2.nuptk"] || '-') + ' - ' + (data["guruPembina2.nama"] || '');
                } else {
                    document.getElementById('hiddenIdGuru2<%=rnd%>').value = '';
                    document.getElementById('inputCariGuru2<%=rnd%>').value = '';
                }

                if(data["guruPembina3.id"]) {
                    document.getElementById('hiddenIdGuru3<%=rnd%>').value = data["guruPembina3.id"];
                    document.getElementById('inputCariGuru3<%=rnd%>').value = (data["guruPembina3.nuptk"] || '-') + ' - ' + (data["guruPembina3.nama"] || '');
                } else {
                    document.getElementById('hiddenIdGuru3<%=rnd%>').value = '';
                    document.getElementById('inputCariGuru3<%=rnd%>').value = '';
                }

                let idYys = data["yayasan.id"] ? data["yayasan.id"] : null;
                let idSkl = data["sekolah.id"] ? data["sekolah.id"] : null;
                <% if (idGuruLogin == null) { %>
                loadYayasan<%=rnd%>('selectYayasan<%=rnd%>', idYys);
                if(idYys) {
                    loadSekolah<%=rnd%>(idYys, idSkl);
                } else {
                    loadSekolah<%=rnd%>(null, idSkl);
                }
                <% } %>
                
                loadCabang<%=rnd%>('selectCabang<%=rnd%>', data["cabangPrestasiGuru.id"] ? data["cabangPrestasiGuru.id"] : null);
                loadKategori<%=rnd%>('selectKategori<%=rnd%>', data["kategoriPrestasiGuru.id"] ? data["kategoriPrestasiGuru.id"] : null);

                disableFormIfApproved<%=rnd%>(data.status);
                checkExistingFile<%=rnd%>(data.id, isApproved);
                
                document.getElementById('uploadPhotoArea<%=rnd%>').style.display = 'block';
                document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-edit text-warning me-2"></i><%=Common.getBahasaConfig("Ubah Data Prestasi Guru")%>';
                document.getElementById('viewList<%=rnd%>').style.display = 'none';
                document.getElementById('viewForm<%=rnd%>').style.display = 'block';
            } else {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal mengambil data dari server.")%>', 'bg-danger text-white');
            }
        } catch (error) {
            console.error(error);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi.")%>', 'bg-danger text-white');
        }
    };

    const hapusPrestasi<%=rnd%> = async (id) => {
        if (typeof proseshapusDataRinci === 'function') {
            proseshapusDataRinci(masterModelClass<%=rnd%>, id, function(){ 
                 if (document.querySelectorAll('#tabelDataPrestasi<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) {
                     currentPage<%=rnd%>--;
                 }
                 loadDataPrestasi<%=rnd%>();
            });
        }
    };

    // INISIALISASI
    document.addEventListener("DOMContentLoaded", () => {
        document.getElementById('searchPrestasi<%=rnd%>').addEventListener("keydown", (e) => {
            if (e.key === "Enter") {
                e.preventDefault();
                resetAndLoadData<%=rnd%>(); 
            }
        });

        <% if (idGuruLogin == null) { %>
        loadYayasan<%=rnd%>('filterYayasan<%=rnd%>', null);
        loadFilterSekolah<%=rnd%>(null);
        <% } %>
        
        loadCabang<%=rnd%>('filterCabang<%=rnd%>', null);
        loadKategori<%=rnd%>('filterKategori<%=rnd%>', null);

        loadDataPrestasi<%=rnd%>();
    });
</script>