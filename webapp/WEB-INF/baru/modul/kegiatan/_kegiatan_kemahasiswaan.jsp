<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="ais.database.model.KegiatanKemahasiswaan"%>
<%@page import="ais.database.model.KelompokKegiatanKemahasiswaan"%>
<%@page import="ais.database.model.DetailKelompokKegiatanKemahasiswaan"%>
<%@page import="ais.database.model.JabatanKegiatanKemahasiswaan"%>
<%@page import="ais.database.model.SkalaKegiatanKemahasiswaan"%>
<%@page import="ais.database.model.JenisAktfitasMahasiswa"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// 1. SECURITY & IDENTITY CHECK
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	out.print("{\"status\":\"error\", \"message\":\""
	+ Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
    return;
}
String rnd = Common.getGeneratedBarCode(7);

Mahasiswa loginSebagaiMahasiswa = tbmuser.getMahasiswa();
Long idMhsLogin = (loginSebagaiMahasiswa != null) ? loginSebagaiMahasiswa.getId() : null;

String selectedTa = Common.getCurrentTahunAkademik();
String selectedSmt = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

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
// Hak tambah khusus (Bukan Dosen & Bukan Mhs = Admin Admin Prodi/Fakultas)
if(idMhsLogin == null && tbmuser.ambilDosen() == null){ add = true; }
%>

<div id="viewList<%=rnd%>" class="animate__animated animate__fadeInUp">
    <div class="row mb-4">
        <div class="col-12">
            <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
                <div class="card-header bg-white border-0 pt-4 pb-3 px-4 border-bottom border-light">
                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-2">
                        <div>
                            <h5 class="fw-bold text-dark mb-1">
                                <i class="fas fa-clipboard-list text-primary me-2"></i><%=Common.getBahasaConfig("Manajemen Kegiatan Kemahasiswaan")%>
                            </h5>
                            <small class="text-muted"><%=Common.getBahasaConfig("Kelola data partisipasi dan kegiatan kemahasiswaan.")%></small>
                        </div>
                        
                        <div class="d-flex align-items-center gap-2 flex-wrap">
                            <div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;">
                                <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>
                                <input type="text" class="form-control border-start-0 ps-0 bg-white fw-medium" id="searchKegiatan<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Nama Kegiatan...")%>">
                            </div>
                            <button class="btn btn-sm btn-outline-info rounded-pill px-3 shadow-sm fw-bold" type="button" data-bs-toggle="collapse" data-bs-target="#filterPanel<%=rnd%>">
                                <i class="fas fa-filter me-1"></i><%=Common.getBahasaConfig("Filter")%>
                            </button>
                            <button class="btn btn-sm btn-outline-secondary rounded-pill px-3 shadow-sm fw-bold" onclick="resetAndLoadData<%=rnd%>()">
                                <i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Segarkan")%>
                            </button>
                            <% if (add) { %>
                            <button class="btn btn-sm btn-primary rounded-pill px-4 shadow-sm fw-bold text-nowrap" onclick="bukaFormTambah<%=rnd%>()">
                                <i class="fas fa-plus-circle me-1"></i><%=Common.getBahasaConfig("Tambah Kegiatan")%>
                            </button>
                            <% } %>
                        </div>
                    </div>

                    <div class="collapse mt-3" id="filterPanel<%=rnd%>">
                        <div class="card card-body bg-light border-0 shadow-sm rounded-3 p-3">
                            <div class="row g-3">
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
                                <% } %>
                                <div class="col-md-3">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Status")%></label>
                                    <select id="filterStatus<%=rnd%>" class="form-select form-select-sm shadow-sm">
                                        <option value=""><%=Common.getBahasaConfig("-- Semua Status --")%></option>
                                        <option value="Belum diproses">Belum diproses</option>
                                        <option value="Sedang diproses">Sedang diproses</option>
                                        <option value="Disetujui">Disetujui</option>
                                        <option value="Ditolak">Ditolak</option>
                                    </select>
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Aspek")%></label>
                                    <select id="filterAspek<%=rnd%>" class="form-select form-select-sm shadow-sm">
                                        <option value=""><%=Common.getBahasaConfig("-- Semua Aspek --")%></option>
                                    </select>
                                </div>
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
                        <table class="table table-hover table-striped align-middle mb-0" id="tabelHTMLDataKegiatan<%=rnd%>">
                            <thead class="table-dark text-center">
                                <tr>
                                    <th style="width: 50px;">#</th>
                                    <th class="text-start"><%=Common.getBahasaConfig("Kegiatan")%></th>
                                    <th class="text-start"><%=Common.getBahasaConfig("Prodi / Fakultas")%></th>
                                    <th class="text-start"><%=Common.getBahasaConfig("Aspek")%></th>
                                    <th><%=Common.getBahasaConfig("Jadwal")%></th>
                                    <th><%=Common.getBahasaConfig("Status")%></th>
                                    <th style="width: 100px;"></th>
                                </tr>
                            </thead>
                            <tbody id="tabelDataKegiatan<%=rnd%>">
                                <tr><td colspan="7" class="text-center py-5"><div class="spinner-border text-primary"></div></td></tr>
                            </tbody>
                        </table>
                    </div>
                    <div class="d-flex justify-content-between align-items-center mt-4">
                        <div class="small text-muted" id="pagingInfoKegiatan<%=rnd%>"></div>
                        <nav>
                            <ul class="pagination pagination-sm mb-0">
                                <li class="page-item"><button class="page-link" id="btnPrevPage<%=rnd%>" onclick="changePageKegiatan<%=rnd%>(-1)"><i class="fas fa-chevron-left"></i></button></li>
                                <li class="page-item disabled"><span class="page-link" id="pageNumberDisplay<%=rnd%>">1</span></li>
                                <li class="page-item"><button class="page-link" id="btnNextPage<%=rnd%>" onclick="changePageKegiatan<%=rnd%>(1)"><i class="fas fa-chevron-right"></i></button></li>
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
                    <h5 class="fw-bold text-dark mb-0" id="formTitle<%=rnd%>">Formulir Kegiatan</h5>
                </div>
                <div class="card-body p-4">
                    <form id="formKegiatan<%=rnd%>" onsubmit="event.preventDefault(); simpanKegiatan<%=rnd%>();">
                        <input type="hidden" id="inputIdKegiatan<%=rnd%>" value="">
                        <div class="row g-4 mt-1">
                            <div class="col-md-12">
                                <h6 class="fw-bold text-primary border-bottom pb-2"><i class="fas fa-clipboard-list me-2"></i>1. Data Utama</h6>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Kegiatan")%> *</label>
                                <input type="text" class="form-control fw-bold shadow-sm field-to-disable<%=rnd%>" id="inputNama<%=rnd%>" required>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Kegiatan (English)")%></label>
                                <input type="text" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputNamaEn<%=rnd%>">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tempat / Alamat")%> *</label>
                                <textarea class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputTempat<%=rnd%>" rows="2" required></textarea>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal Mulai")%></label>
                                <input type="date" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputMulai<%=rnd%>">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal Selesai")%></label>
                                <input type="date" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputSampai<%=rnd%>">
                            </div>

                            <div class="col-md-12 mt-4">
                                <h6 class="fw-bold text-primary border-bottom pb-2"><i class="fas fa-layer-group me-2"></i>2. Aspek & Klasifikasi</h6>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Aspek Kegiatan")%> *</label>
                                <select class="form-select shadow-sm field-to-disable<%=rnd%>" id="selectKelompok<%=rnd%>" onchange="loadAspekRinci<%=rnd%>(this.value)" required></select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Rincian Aspek")%> *</label>
                                <select class="form-select shadow-sm field-to-disable<%=rnd%>" id="selectDetailKelompok<%=rnd%>" onchange="loadJabatanSkala<%=rnd%>(this.value)" required></select>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jabatan / Status")%> *</label>
                                <select class="form-select shadow-sm field-to-disable<%=rnd%>" id="selectJabatan<%=rnd%>" required></select>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Skala Kegiatan")%> *</label>
                                <select class="form-select shadow-sm field-to-disable<%=rnd%>" id="selectSkala<%=rnd%>" required></select>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jenis / Kampus Merdeka")%></label>
                                <select class="form-select shadow-sm field-to-disable<%=rnd%>" id="selectJenisAktifitas<%=rnd%>"></select>
                            </div>
                            <div class="col-md-12">
                                <div class="form-check form-switch">
                                    <input class="form-check-input field-to-disable<%=rnd%>" type="checkbox" id="inputBolehDipilih<%=rnd%>">
                                    <label class="form-check-label fw-bold text-secondary small" for="inputBolehDipilih<%=rnd%>"><%=Common.getBahasaConfig("Bisa dipilih mahasiswa lain")%></label>
                                </div>
                            </div>

                            <div class="col-md-12 mt-4">
                                <h6 class="fw-bold text-primary border-bottom pb-2"><i class="fas fa-university me-2"></i>3. Akademik & Pembina</h6>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tahun Akademik")%> *</label>
                                <select class="form-select shadow-sm field-to-disable<%=rnd%>" id="inputTahunAkademik<%=rnd%>" required>
                                    <option value="">Pilih TA</option>
                                    <% if (Common.tahunAngkatans != null) { for (String ta : Common.tahunAngkatans) { %> <option value="<%= ta %>"><%= ta %></option> <% } } %>
                                </select>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Semester")%> *</label>
                                <select class="form-select shadow-sm field-to-disable<%=rnd%>" id="inputJenisSemester<%=rnd%>" required>
                                    <option value="">Pilih SMT</option>
                                    <option value="<%= Perkuliahan.GANJIL %>">Ganjil</option>
                                    <option value="<%= Perkuliahan.GENAP %>">Genap</option>
                                </select>
                            </div>
                            <% if (idMhsLogin == null) { %>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Fakultas")%></label>
                                <select class="form-select shadow-sm field-to-disable<%=rnd%>" id="selectFakultas<%=rnd%>" onchange="loadJurusan<%=rnd%>(this.value, null)"></select>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jurusan")%></label>
                                <select class="form-select shadow-sm field-to-disable<%=rnd%>" id="selectJurusan<%=rnd%>"></select>
                            </div>
                            <% } else { %>
                                <input type="hidden" id="selectFakultas<%=rnd%>" value=""><input type="hidden" id="selectJurusan<%=rnd%>" value="">
                            <% } %>

                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Dosen Pembina I")%></label>
                                <input type="text" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputCariDosen1<%=rnd%>" list="dlDosen1<%=rnd%>" oninput="onInputDosen<%=rnd%>(this.value, 1)" onchange="checkSelectedDosen<%=rnd%>(this.value, 1)">
                                <datalist id="dlDosen1<%=rnd%>"></datalist><input type="hidden" id="hiddenIdDosen1<%=rnd%>">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Dosen Pembina II")%></label>
                                <input type="text" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputCariDosen2<%=rnd%>" list="dlDosen2<%=rnd%>" oninput="onInputDosen<%=rnd%>(this.value, 2)" onchange="checkSelectedDosen<%=rnd%>(this.value, 2)">
                                <datalist id="dlDosen2<%=rnd%>"></datalist><input type="hidden" id="hiddenIdDosen2<%=rnd%>">
                            </div>

                            <div class="col-md-12 mt-4">
                                <h6 class="fw-bold text-primary border-bottom pb-2"><i class="fas fa-file-contract me-2"></i>4. Legal & Keterangan</h6>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nomor SK")%></label>
                                <input type="text" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputNoSk<%=rnd%>">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal SK")%></label>
                                <input type="date" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputTglSk<%=rnd%>">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("URL Kegiatan")%></label>
                                <input type="url" class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputUrl<%=rnd%>" placeholder="https://...">
                            </div>
                            <div class="col-md-12">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Keterangan Tambahan")%></label>
                                <textarea class="form-control shadow-sm field-to-disable<%=rnd%>" id="inputKeterangan<%=rnd%>" rows="3"></textarea>
                            </div>

                            <% if (idMhsLogin == null) { %>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Status Persetujuan")%> *</label>
                                <select class="form-select shadow-sm fw-bold border-info" id="inputStatus<%=rnd%>" required>
                                    <option value="Belum diproses">Belum diproses</option>
                                    <option value="Sedang diproses">Sedang diproses</option>
                                    <option value="Disetujui">Disetujui</option>
                                    <option value="Ditolak">Ditolak</option>
                                </select>
                            </div>
                            <% } else { %>
                                <input type="hidden" id="inputStatus<%=rnd%>" value="Belum diproses">
                            <% } %>

                            <div class="col-md-12 mt-4 pt-3 border-top text-center" id="uploadPhotoArea<%=rnd%>" style="display: none;">
                                <div class="p-3 bg-light rounded-3 border">
                                    <label class="form-label fw-bold text-secondary d-block"><i class="fas fa-file-archive text-warning me-2"></i>Lampiran Kegiatan (ZIP jika > 1 file)</label>
                                    <div id="action-buttons-<%=rnd%>"></div>
                                    <div id="progress-wrapper-<%=rnd%>" class="mt-3" style="display: none; max-width: 300px; margin: 0 auto;">
                                        <div class="progress" style="height: 15px;"><div id="progress-bar-<%=rnd%>" class="progress-bar bg-primary" role="progressbar" style="width: 0%;">0%</div></div>
                                    </div>
                                    <div id="preview-area-<%=rnd%>" class="mt-3"></div>
                                </div>
                            </div>

                            <div class="col-12 mt-5 text-end border-top pt-3">
                                <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold border" onclick="tutupForm<%=rnd%>()"><%=Common.getBahasaConfig("Batal")%></button>
                                <button type="submit" class="btn btn-success px-4 rounded-pill shadow-sm fw-bold" id="btnSimpan<%=rnd%>"><%=Common.getBahasaConfig("Simpan Data")%></button>
                            </div>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    const masterModelClass<%=rnd%> = "<%=KegiatanKemahasiswaan.class.getName()%>";
    let activeEditingId<%=rnd%> = "";
    let isDocumentUploaded<%=rnd%> = false; 
    let currentPage<%=rnd%> = 1;
    const limitPerPage<%=rnd%> = 10;
    let totalRecords<%=rnd%> = 0;

    const showToast<%=rnd%> = function(msg, colorClass) { if(typeof tampilkanToast === "function") { tampilkanToast(msg, colorClass); } else { alert(msg); } };

    const disableFormIfApproved<%=rnd%> = function(status) {
        var fields = document.querySelectorAll('.field-to-disable<%=rnd%>');
        var isApproved = (status === 'Disetujui');
        for(var i=0; i<fields.length; i++) { fields[i].disabled = isApproved; }
        if(document.getElementById('btnSimpan<%=rnd%>')) document.getElementById('btnSimpan<%=rnd%>').disabled = isApproved;
    };

    // --- DROPDOWN LOADERS ---
    const loadFakultas<%=rnd%> = async function(targetId, selectedId) {
        try {
            var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({action: "daftar", class: "ais.database.model.Fakultas", where1: "aktif = true", order1: "asc", sort1: "nama"})});
            var data = (await res.json()).data || [];
            var opt = '<option value="">-- Pilih Fakultas --</option>';
            for(var i=0; i<data.length; i++) { opt += '<option value="' + data[i].id + '" ' + (selectedId == data[i].id ? 'selected':'') + '>' + data[i].nama + '</option>'; }
            document.getElementById(targetId).innerHTML = opt;
        } catch(e){}
    };

    const loadJurusan<%=rnd%> = async function(idFak, selectedId) {
        var target = document.getElementById('selectJurusan<%=rnd%>');
        if(!target) return;
        if(!idFak) { target.innerHTML = '<option value="">-- Pilih Jurusan --</option>'; return; }
        try {
            var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({action: "daftar", class: "ais.database.model.Jurusan", where1: "aktif = true AND fakultas = " + idFak, order1: "asc", sort1: "nama"})});
            var data = (await res.json()).data || [];
            var opt = '<option value="">-- Pilih Jurusan --</option>';
            for(var i=0; i<data.length; i++) { opt += '<option value="' + data[i].id + '" ' + (selectedId == data[i].id ? 'selected':'') + '>' + data[i].nama + '</option>'; }
            target.innerHTML = opt;
        } catch(e){}
    };

    const loadAspek<%=rnd%> = async function(targetId, selectedId) {
        try {
            var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({action: "daftar", class: "<%=KelompokKegiatanKemahasiswaan.class.getName()%>", where1: "aktif = true", order1: "asc", sort1: "nama"})});
            var data = (await res.json()).data || [];
            var opt = targetId.indexOf('filter') > -1 ? '<option value="">-- Semua Aspek --</option>' : '<option value="">-- Pilih Aspek --</option>';
            for(var i=0; i<data.length; i++) { opt += '<option value="' + data[i].id + '" ' + (selectedId == data[i].id ? 'selected':'') + '>' + data[i].nama + '</option>'; }
            document.getElementById(targetId).innerHTML = opt;
        } catch(e){}
    };

    const loadAspekRinci<%=rnd%> = async function(idKelompok, selectedId) {
        var target = document.getElementById('selectDetailKelompok<%=rnd%>');
        if(!idKelompok) { target.innerHTML = '<option value="">-- Pilih Rincian Aspek --</option>'; return; }
        try {
            var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({action: "daftar", class: "<%=DetailKelompokKegiatanKemahasiswaan.class.getName()%>", where1: "kelompok_kegiatan_kemahasiswaan = " + idKelompok, order1: "asc", sort1: "nama"})});
            var data = (await res.json()).data || [];
            var opt = '<option value="">-- Pilih Rincian Aspek --</option>';
            for(var i=0; i<data.length; i++) { opt += '<option value="' + data[i].id + '" ' + (selectedId == data[i].id ? 'selected':'') + '>' + data[i].nama + '</option>'; }
            target.innerHTML = opt;
        } catch(e){}
    };

    const loadJabatanSkala<%=rnd%> = async function(idDet, selJ, selS) {
        try {
            var rJ = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({action: "daftar", class: "<%=JabatanKegiatanKemahasiswaan.class.getName()%>"})});
            var dJ = (await rJ.json()).data || [];
            var oJ = '<option value="">-- Pilih Jabatan --</option>';
            for(var i=0; i<dJ.length; i++) { oJ += '<option value="' + dJ[i].id + '" ' + (selJ == dJ[i].id ? 'selected':'') + '>' + dJ[i].nama + '</option>'; }
            document.getElementById('selectJabatan<%=rnd%>').innerHTML = oJ;

            var rS = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({action: "daftar", class: "<%=SkalaKegiatanKemahasiswaan.class.getName()%>"})});
            var dS = (await rS.json()).data || [];
            var oS = '<option value="">-- Pilih Skala --</option>';
            for(var i=0; i<dS.length; i++) { oS += '<option value="' + dS[i].id + '" ' + (selS == dS[i].id ? 'selected':'') + '>' + dS[i].nama + '</option>'; }
            document.getElementById('selectSkala<%=rnd%>').innerHTML = oS;
        } catch(e){}
    };

    // --- GRID LOGIC ---
    const buildFilterSQL<%=rnd%> = function() {
        var kw = document.getElementById('searchKegiatan<%=rnd%>').value.trim().replace(/'/g, "''");
        var filters = [];
        <% if (idMhsLogin != null) { %> filters.push("id IN (SELECT kegiatanKemahasiswaan FROM ais.database.model.KegiatanKemahasiswaanPunyaMahasiswa WHERE mahasiswa = <%=idMhsLogin%>)"); <% } %>
        if (kw !== '') filters.push("(this_.nama ILIKE '%" + kw + "%')");
        var st = document.getElementById('filterStatus<%=rnd%>').value; if(st) filters.push("this_.status = '" + st + "'");
        var asp = document.getElementById('filterAspek<%=rnd%>').value; if(asp) filters.push("this_.kelompok_kegiatan_kemahasiswaan = " + asp);
        return filters.length > 0 ? filters.join(' AND ') : '';
    };

    const loadDataKegiatan<%=rnd%> = async function() {
        var tbody = document.getElementById('tabelDataKegiatan<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4"><div class="spinner-border text-primary"></div></td></tr>';
        var start = (currentPage<%=rnd%> - 1);
        var req = { action: "daftar", class: masterModelClass<%=rnd%>, deep: "1", max: limitPerPage<%=rnd%>, halaman: start, order1: "desc", sort1: "id", count: "true" };
        var wc = buildFilterSQL<%=rnd%>(); if (wc) req["where1"] = wc;
        try {
            var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(req) });
            var result = await res.json();
            totalRecords<%=rnd%> = parseInt(result.count || 0);
            var list = result.data || [];
            var html = '';
            if (list.length === 0) { html = '<tr><td colspan="7" class="text-center text-muted py-4">Tidak ada data.</td></tr>'; } 
            else {
                var no = (start * limitPerPage<%=rnd%>) + 1;
                for(var i=0; i<list.length; i++){
                    var r = list[i];
                    html += '<tr><td class="text-center">' + (no++) + '</td><td>' + r.nama + '</td><td>' + (r["jurusan.nama"]||'Semua') + '</td><td>' + (r["kelompokKegiatanKemahasiswaan.nama"]||'-') + '</td><td class="text-center small">' + (r["mulai.datetime_format"]||'-') + '</td><td class="text-center">' + r.status + '</td><td class="text-center"><button class="btn btn-sm btn-outline-warning" onclick="editKegiatan<%=rnd%>(' + r.id + ')"><i class="fas fa-edit"></i></button></td></tr>';
                }
            }
            tbody.innerHTML = html;
            document.getElementById('pageNumberDisplay<%=rnd%>').innerText = currentPage<%=rnd%> + " / " + (Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1);
        } catch(e){}
    };

    const resetAndLoadData<%=rnd%> = function() { currentPage<%=rnd%> = 1; loadDataKegiatan<%=rnd%>(); };
    const changePageKegiatan<%=rnd%> = function(d) { var tp = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>); if(d===-1 && currentPage<%=rnd%> > 1){currentPage<%=rnd%>--; loadDataKegiatan<%=rnd%>();} else if(d===1 && currentPage<%=rnd%> < tp){currentPage<%=rnd%>++; loadDataKegiatan<%=rnd%>();} };

    // --- SAVE LOGIC ---
    const simpanKegiatan<%=rnd%> = async function() {
        var id = document.getElementById('inputIdKegiatan<%=rnd%>').value;
        var btn = document.getElementById('btnSimpan<%=rnd%>');
        
        // SAFE CHECK ELEMENTS
        var elUrl = document.getElementById('inputUrl<%=rnd%>');
        var elNoSk = document.getElementById('inputNoSk<%=rnd%>');
        var elTglSk = document.getElementById('inputTglSk<%=rnd%>');
        var elKet = document.getElementById('inputKeterangan<%=rnd%>');

        var dataObj = {
            nama: document.getElementById('inputNama<%=rnd%>').value.trim(),
            namaEn: document.getElementById('inputNamaEn<%=rnd%>').value.trim(),
            tempat: document.getElementById('inputTempat<%=rnd%>').value.trim(),
            mulai: document.getElementById('inputMulai<%=rnd%>').value || null,
            sampai: document.getElementById('inputSampai<%=rnd%>').value || null,
            bolehDipilih: document.getElementById('inputBolehDipilih<%=rnd%>').checked,
            tahunAkademik: document.getElementById('inputTahunAkademik<%=rnd%>').value || null,
            jenisSemester: document.getElementById('inputJenisSemester<%=rnd%>').value || null,
            url: elUrl ? elUrl.value.trim() : null,
            noSk: elNoSk ? elNoSk.value.trim() : null,
            tglSk: elTglSk ? elTglSk.value : null,
            keterangan: elKet ? elKet.value.trim() : null,
            status: document.getElementById('inputStatus<%=rnd%>') ? document.getElementById('inputStatus<%=rnd%>').value : 'Belum diproses',
            kelompokKegiatanKemahasiswaan: parseInt(document.getElementById('selectKelompok<%=rnd%>').value) || "null",
            detailKelompokKegiatanKemahasiswaan: parseInt(document.getElementById('selectDetailKelompok<%=rnd%>').value) || "null",
            jabatanKegiatanKemahasiswaan: parseInt(document.getElementById('selectJabatan<%=rnd%>').value) || "null",
            skalaKegiatanKemahasiswaan: parseInt(document.getElementById('selectSkala<%=rnd%>').value) || "null",
            jenisAktfitasMahasiswa: parseInt(document.getElementById('selectJenisAktifitas<%=rnd%>').value) || "null",
            dosenPembina1: parseInt(document.getElementById('hiddenIdDosen1<%=rnd%>').value) || "null",
            dosenPembina2: parseInt(document.getElementById('hiddenIdDosen2<%=rnd%>').value) || "null",
            fakultas: parseInt(document.getElementById('selectFakultas<%=rnd%>').value) || "null",
            jurusan: parseInt(document.getElementById('selectJurusan<%=rnd%>').value) || "null"
        };
        <% if (idMhsLogin != null) { %> dataObj.diajukanOleh = <%=idMhsLogin%>; <% } %>

        var payload = { action: "simpanDataRinci", log: "true", class: masterModelClass<%=rnd%>, data: dataObj };
        if(id) payload.id = id;

        var oriText = btn.innerHTML; btn.innerHTML = 'Menyimpan...'; btn.disabled = true;
        try {
            var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(payload) });
            var result = await res.json();
            if(result.status === '00' || result.id) {
                activeEditingId<%=rnd%> = result.id || id;
                document.getElementById('inputIdKegiatan<%=rnd%>').value = activeEditingId<%=rnd%>;
                document.getElementById('uploadPhotoArea<%=rnd%>').style.display = 'block';
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data berhasil disimpan.")%>', 'bg-success text-white');
                loadDataKegiatan<%=rnd%>();
            } else { showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Proses gagal. Silakan coba lagi.")%>', 'bg-danger text-white'); }
        } catch(e){ console.error(e); } finally { btn.innerHTML = oriText; btn.disabled = false; }
    };

    const editKegiatan<%=rnd%> = async function(id) {
        try {
            var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({action:"load", class:masterModelClass<%=rnd%>, id:id}) });
            var data = (await res.json()).data;
            activeEditingId<%=rnd%> = data.id;
            document.getElementById('inputIdKegiatan<%=rnd%>').value = data.id;
            document.getElementById('inputNama<%=rnd%>').value = data.nama || '';
            document.getElementById('inputTempat<%=rnd%>').value = data.tempat || '';
            document.getElementById('inputTahunAkademik<%=rnd%>').value = data.tahunAkademik || '';
            document.getElementById('inputJenisSemester<%=rnd%>').value = data.jenisSemester || '';
            if(document.getElementById('inputUrl<%=rnd%>')) document.getElementById('inputUrl<%=rnd%>').value = data.url || '';
            if(document.getElementById('inputNoSk<%=rnd%>')) document.getElementById('inputNoSk<%=rnd%>').value = data.noSk || '';
            if(document.getElementById('inputTglSk<%=rnd%>')) document.getElementById('inputTglSk<%=rnd%>').value = data["tglSk.datetime_format"] || '';
            if(document.getElementById('inputKeterangan<%=rnd%>')) document.getElementById('inputKeterangan<%=rnd%>').value = data.keterangan || '';
            if(document.getElementById('inputStatus<%=rnd%>')) document.getElementById('inputStatus<%=rnd%>').value = data.status || 'Belum diproses';
            
            var idK = data["kelompokKegiatanKemahasiswaan.id"] || (data.kelompokKegiatanKemahasiswaan ? data.kelompokKegiatanKemahasiswaan.id : null);
            var idD = data["detailKelompokKegiatanKemahasiswaan.id"] || (data.detailKelompokKegiatanKemahasiswaan ? data.detailKelompokKegiatanKemahasiswaan.id : null);
            
            await loadAspek<%=rnd%>('selectKelompok<%=rnd%>', idK);
            await loadAspekRinci<%=rnd%>(idK, idD);
            await loadJabatanSkala<%=rnd%>(idD, data["jabatanKegiatanKemahasiswaan.id"], data["skalaKegiatanKemahasiswaan.id"]);
            
            <% if (idMhsLogin == null) { %>
                await loadFakultas<%=rnd%>('selectFakultas<%=rnd%>', data["fakultas.id"]);
                await loadJurusan<%=rnd%>(data["fakultas.id"], data["jurusan.id"]);
            <% } %>

            disableFormIfApproved<%=rnd%>(data.status);
            document.getElementById('uploadPhotoArea<%=rnd%>').style.display = 'block';
            document.getElementById('viewList<%=rnd%>').style.display = 'none';
            document.getElementById('viewForm<%=rnd%>').style.display = 'block';
        } catch(e){}
    };

    const bukaFormTambah<%=rnd%> = async function() {
        document.getElementById('inputIdKegiatan<%=rnd%>').value = '';
        document.getElementById('formKegiatan<%=rnd%>').reset();
        activeEditingId<%=rnd%> = "";
        await loadAspek<%=rnd%>('selectKelompok<%=rnd%>', null);
        <% if (idMhsLogin == null) { %> await loadFakultas<%=rnd%>('selectFakultas<%=rnd%>', null); <% } %>
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    };

    const tutupForm<%=rnd%> = function() { document.getElementById('viewForm<%=rnd%>').style.display = 'none'; document.getElementById('viewList<%=rnd%>').style.display = 'block'; loadDataKegiatan<%=rnd%>(); };

    document.addEventListener("DOMContentLoaded", function() {
        loadAspek<%=rnd%>('filterAspek<%=rnd%>', null);
        <% if (idMhsLogin == null) { %> loadFakultas<%=rnd%>('filterFakultas<%=rnd%>', null); <% } %>
        loadDataKegiatan<%=rnd%>();
    });
</script>