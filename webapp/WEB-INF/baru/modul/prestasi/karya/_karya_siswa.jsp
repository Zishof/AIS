<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="ais.database.model.sekolah.PenghargaanSiswa"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.sekolah.Yayasan"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.rab.SatuanKerja"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.KategoriPenghargaan"%>
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

// Mengambil konteks Sekolah dan Yayasan untuk otomatisasi
SatuanKerja satuanKerja = tbmuser.ambilSatuanKerja();
Sekolah loginSebagaiSekolah = tbmuser.ambilSekolah();
Yayasan loginSebagaiYayasan =  tbmuser.ambilYayasan();

System.out.println("satker --> "  + String.valueOf(satuanKerja.getId()));

Siswa loginSebagaiSiswa = tbmuser.getSiswa();

Long idSiswaLogin = (loginSebagaiSiswa != null) ? loginSebagaiSiswa.getId() : null;
Long idSekolahLogin = (loginSebagaiSekolah != null) ? loginSebagaiSekolah.getId() : null;
Long idYayasanLogin = (loginSebagaiYayasan != null) ? loginSebagaiYayasan.getId() : null;

// Jika login sebagai sekolah, otomatis ambil data yayasan dari sekolah tersebut
if (idSekolahLogin != null && idYayasanLogin == null && loginSebagaiSekolah.getYayasan() != null) {
    idYayasanLogin = loginSebagaiSekolah.getYayasan().getId();
}

String selectedTa = Common.getCurrentTahunAkademik();
String selectedSmt = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

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
			<div
				class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
				<div
					class="card-header bg-white border-0 pt-4 pb-3 px-4 border-bottom border-light">
					<div
						class="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-2">
						<div>
							<h5 class="fw-bold text-dark mb-1">
								<i class="fas fa-trophy text-primary me-2"></i><%=Common.getBahasaConfig("Manajemen Karya Siswa")%>
							</h5>
							<small class="text-muted"><%=Common.getBahasaConfig("Kelola data pencapaian dan penghargaan siswa.")%></small>
						</div>

						<div class="d-flex align-items-center gap-2 flex-wrap">
							<div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;">
								<span class="input-group-text bg-white border-end-0"><i
									class="fas fa-search text-muted"></i></span> <input type="text"
									class="form-control border-start-0 ps-0 bg-white fw-medium"
									id="searchPenghargaan<%=rnd%>"
									placeholder="<%=Common.getBahasaConfig("Cari NIK / Nama Siswa...")%>">
							</div>

							<button
								class="btn btn-sm btn-outline-info rounded-pill px-3 shadow-sm fw-bold"
								type="button" data-bs-toggle="collapse"
								data-bs-target="#filterPanel<%=rnd%>">
								<i class="fas fa-filter me-1"></i><%=Common.getBahasaConfig("Filter")%>
							</button>

							<button
								class="btn btn-sm btn-outline-secondary rounded-pill px-3 shadow-sm fw-bold"
								onclick="resetAndLoadData<%=rnd%>()">
								<i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Segarkan")%>
							</button>

							<% if (add) { %>
							<button
								class="btn btn-sm btn-primary rounded-pill px-4 shadow-sm fw-bold"
								onclick="bukaFormTambah<%=rnd%>()">
								<i class="fas fa-plus-circle me-1"></i><%=Common.getBahasaConfig("Tambah Data")%>
							</button>
							<% } %>
						</div>
					</div>

					<div class="collapse mt-3" id="filterPanel<%=rnd%>">
						<div
							class="card card-body bg-light border-0 shadow-sm rounded-3 p-3">
							<div class="row g-3">
								<div class="col-md-3">
									<label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Tahun Akademik")%></label>
									<select id="filterTahunAkademik<%=rnd%>"
										class="form-select form-select-sm shadow-sm">
										<option value=""><%=Common.getBahasaConfig("-- Semua TA --")%></option>
										<% if (Common.tahunAngkatans != null) { for (String ta : Common.tahunAngkatans) { %>
										<option value="<%= ta %>"><%= ta %></option>
										<% } } %>
									</select>
								</div>
								<% if (idSiswaLogin == null) { %>
								<div class="col-md-3">
									<label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Yayasan")%></label>
									<select id="filterYayasan<%=rnd%>"
										class="form-select form-select-sm shadow-sm"
										onchange="loadFilterSekolah<%=rnd%>(this.value)">
										<option value=""><%=Common.getBahasaConfig("-- Semua Yayasan --")%></option>
									</select>
								</div>
								<div class="col-md-3">
									<label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Sekolah")%></label>
									<select id="filterSekolah<%=rnd%>"
										class="form-select form-select-sm shadow-sm">
										<option value=""><%=Common.getBahasaConfig("-- Semua Sekolah --")%></option>
									</select>
								</div>
								<% } %>
								<div class="col-12 text-end">
									<button type="button"
										class="btn btn-sm btn-info text-white px-4 fw-bold shadow-sm rounded-pill"
										onclick="resetAndLoadData<%=rnd%>()">
										<i class="fas fa-search me-1"></i>
										<%=Common.getBahasaConfig("Terapkan Filter")%>
									</button>
								</div>
							</div>
						</div>
					</div>
				</div>

				<div class="card-body p-4 pt-3">
					<div class="table-responsive border rounded-3 shadow-sm bg-white">
						<table class="table table-hover table-striped align-middle mb-0"
							id="tabelHTMLDataPenghargaan<%=rnd%>">
							<thead class="table-dark text-center align-middle">
								<tr>
									<th style="width: 60px;">#</th>
									<th class="text-start"><i
										class="fas fa-user-graduate me-1"></i><%=Common.getBahasaConfig("Siswa")%></th>
									<th class="text-start"><i class="fas fa-medal me-1"></i><%=Common.getBahasaConfig("Karya / Penghargaan")%></th>
									<th class="text-start"><i class="fas fa-school me-1"></i><%=Common.getBahasaConfig("Sekolah")%></th>
									<th class="text-center"><i
										class="fas fa-calendar-alt me-1"></i><%=Common.getBahasaConfig("Tanggal")%></th>
									<th class="text-center"><i class="fas fa-info-circle me-1"></i><%=Common.getBahasaConfig("Status")%></th>
									<th class="text-center no-export" style="width: 120px;"><i
										class="fas fa-cogs"></i></th>
								</tr>
							</thead>
							<tbody id="tabelDataPenghargaan<%=rnd%>">
								<tr>
									<td colspan="7" class="text-center py-5"><div
											class="spinner-border text-primary"></div></td>
								</tr>
							</tbody>
						</table>
					</div>
					<div
						class="d-flex justify-content-between align-items-center flex-wrap gap-3 mt-4">
						<div
							class="small fw-medium text-muted bg-light px-3 py-1 rounded-pill border"
							id="pagingInfoPenghargaan<%=rnd%>"></div>
						<nav>
							<ul class="pagination pagination-sm mb-0 shadow-sm">
								<li class="page-item"><button class="page-link"
										id="btnPrevPage<%=rnd%>" onclick="changePage<%=rnd%>(-1)">
										<i class="fas fa-chevron-left"></i>
									</button></li>
								<li class="page-item disabled"><span class="page-link"
									id="pageNumberDisplay<%=rnd%>">1</span></li>
								<li class="page-item"><button class="page-link"
										id="btnNextPage<%=rnd%>" onclick="changePage<%=rnd%>(1)">
										<i class="fas fa-chevron-right"></i>
									</button></li>
							</ul>
						</nav>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>

<div id="viewForm<%=rnd%>" class="animate__animated animate__fadeIn"
	style="display: none;">
	<div class="row justify-content-center">
		<div class="col-lg-12">
			<div
				class="card border-0 shadow-sm rounded-4 border-top border-success border-4">
				<div class="card-header bg-white border-0 pt-4 pb-0 px-4">
					<h5 class="fw-bold text-dark mb-0" id="formTitle<%=rnd%>">
						<i class="fas fa-edit text-success me-2"></i><%=Common.getBahasaConfig("Formulir Karya Siswa")%>
					</h5>
				</div>
				<div class="card-body p-4">
					<form id="formPenghargaan<%=rnd%>"
						onsubmit="event.preventDefault(); simpanPenghargaan<%=rnd%>();">
						<input type="hidden" id="inputIdPenghargaan<%=rnd%>" value="">

						<div class="row g-4 mt-1">
							<div class="col-md-12">
								<h6 class="fw-bold text-primary border-bottom pb-2">
									<i class="fas fa-user-graduate me-2"></i>1.
									<%=Common.getBahasaConfig("Data Siswa & Akademik")%></h6>
							</div>

							<% if (idSiswaLogin == null) { %>
							<div class="col-md-3">
								<label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Yayasan")%></label>
								<select class="form-select shadow-sm" id="selectYayasan<%=rnd%>"
									onchange="loadSekolah<%=rnd%>(this.value, null)">
									<option value="">-- Pilih Yayasan --</option>
								</select>
							</div>
							<div class="col-md-3">
								<label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Sekolah")%></label>
								<select class="form-select shadow-sm" id="selectSekolah<%=rnd%>">
									<option value="">-- Pilih Sekolah --</option>
								</select>
							</div>

							<div class="col-md-6">
								<label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Cari Siswa (NIK / Nama)")%>
									<span class="text-danger">*</span></label> <input type="text"
									class="form-control fw-bold shadow-sm"
									id="inputCariSiswa<%=rnd%>" list="dlSiswa<%=rnd%>"
									placeholder="<%=Common.getBahasaConfig("Ketik NIK atau Nama...")%>"
									oninput="onInputSiswaSearch<%=rnd%>(this.value)"
									onchange="checkSelectedSiswa<%=rnd%>(this.value)"
									autocomplete="off" required>
								<datalist id="dlSiswa<%=rnd%>"></datalist>
								<input type="hidden" id="hiddenIdSiswa<%=rnd%>" required>
								<small class="text-muted fst-italic"><%=Common.getBahasaConfig("Hasil pencarian dibatasi berdasarkan Sekolah terpilih.")%></small>
							</div>
							<% } else { %>
							<input type="hidden" id="hiddenIdSiswa<%=rnd%>"
								value="<%=idSiswaLogin%>">
							<% } %>

							<div class="col-md-4">
								<label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tahun Akademik")%>
									<span class="text-danger">*</span></label> <select
									class="form-select shadow-sm" id="inputTahunAkademik<%=rnd%>"
									onchange="autoFillTahun<%=rnd%>(this.value)" required>
									<option value="">-- Pilih TA --</option>
									<% if (Common.tahunAngkatans != null) { for (String ta : Common.tahunAngkatans) { %>
									<option value="<%= ta %>"><%= ta %></option>
									<% } } %>
								</select>
							</div>
							<div class="col-md-4">
								<label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Semester")%>
									<span class="text-danger">*</span></label> <select
									class="form-select shadow-sm" id="inputJenisSemester<%=rnd%>"
									required>
									<option value="">-- Pilih Semester --</option>
									<option value="<%= Perkuliahan.GANJIL %>"><%=Common.getBahasaConfig("Ganjil")%></option>
									<option value="<%= Perkuliahan.GENAP %>"><%=Common.getBahasaConfig("Genap")%></option>
								</select>
							</div>
							<div class="col-md-4">
								<label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tahun")%></label>
								<input type="number" class="form-control shadow-sm"
									id="inputTahun<%=rnd%>">
							</div>

							<div class="col-md-12">
								<h6 class="fw-bold text-primary border-bottom pb-2">
									<i class="fas fa-award me-2"></i>2.
									<%=Common.getBahasaConfig("Detail Karya / Penghargaan")%></h6>
							</div>

							<div class="col-md-6">
								<label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Karya / Penghargaan")%>
									<span class="text-danger">*</span></label> <input type="text"
									class="form-control fw-bold shadow-sm" id="inputNama<%=rnd%>"
									required>
							</div>
							<div class="col-md-6">
								<label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Karya (Inggris)")%></label>
								<input type="text" class="form-control shadow-sm"
									id="inputNamaEn<%=rnd%>">
							</div>

							<div class="col-md-4">
								<label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal")%></label>
								<input type="date" class="form-control shadow-sm"
									id="inputTanggal<%=rnd%>">
							</div>
							<div class="col-md-4">
								<label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Kategori")%></label>
								<select class="form-select shadow-sm"
									id="selectKategori<%=rnd%>">
									<option value="">-- Pilih Kategori --</option>
								</select>
							</div>
							<div class="col-md-4">
								<label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Status Persetujuan")%></label>
								<select class="form-select shadow-sm fw-bold"
									id="inputStatus<%=rnd%>">
									<option value="Belum diproses">Belum diproses</option>
									<option value="Sedang diproses">Sedang diproses</option>
									<option value="Disetujui">Disetujui</option>
									<option value="Ditolak">Ditolak</option>
								</select>
							</div>

							<div class="col-md-12">
								<label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Keterangan Tambahan")%></label>
								<textarea class="form-control shadow-sm"
									id="inputKeterangan<%=rnd%>" rows="2"></textarea>
							</div>

							<div class="col-md-12 mt-4 pt-3 border-top text-center"
								id="uploadPhotoArea<%=rnd%>" style="display: none;">
								<div class="p-3 bg-light rounded-3 border">
									<label class="form-label fw-bold text-secondary d-block"><i
										class="fas fa-file-pdf text-danger me-2"></i><%=Common.getBahasaConfig("Bukti / Lampiran")%></label>
									<div
										class="d-flex justify-content-center align-items-center gap-2 mt-2"
										id="action-buttons-<%=rnd%>"></div>
									<div id="preview-area-<%=rnd%>" class="mt-3"></div>
								</div>
							</div>

							<div class="col-12 mt-5 text-end border-top pt-3">
								<button type="button"
									class="btn btn-light px-4 me-2 rounded-pill fw-semibold border"
									onclick="tutupForm<%=rnd%>()"><%=Common.getBahasaConfig("Batal")%></button>
								<% if (add || edit) { %>
								<button type="submit"
									class="btn btn-success px-4 rounded-pill shadow-sm fw-bold"
									id="btnSimpan<%=rnd%>">
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
    // Konversi Const/Let ke Var untuk kompatibilitas AJAX
    var masterModelClass<%=rnd%> = "ais.database.model.sekolah.PenghargaanSiswa";
    var activeEditingPenghargaanSiswaId<%=rnd%> = "";
    var isDocumentUploaded<%=rnd%> = false;
    var currentPage<%=rnd%> = 1;
    var limitPerPage<%=rnd%> = 10;
    var totalRecords<%=rnd%> = 0;

    // Konstanta Konteks Login untuk Otomatisasi
    var idYayasanLogin<%=rnd%> = "<%= (idYayasanLogin != null) ? idYayasanLogin : "" %>";
    var idSekolahLogin<%=rnd%> = "<%= (idSekolahLogin != null) ? idSekolahLogin : "" %>";
    
    console.log('idYayasanLogin: ' +  idYayasanLogin<%=rnd%>);
    console.log('idSekolahLogin: ' + idSekolahLogin<%=rnd%>);
    console.log('sekolah: ' + <%=loginSebagaiSekolah%>);
    console.log('Yayasan: ' + <%=loginSebagaiYayasan%>);
    console.log('satuanKerja: ' + <%= satuanKerja.getId() %>);
    
    

    function showToast<%=rnd%>(msg, colorClass) {
        if (typeof tampilkanToast === "function") tampilkanToast(msg, colorClass);
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

    // ==========================================
    // FUNGSI PENCARIAN SISWA (FILTER BY SEKOLAH)
    // ==========================================
    async function onInputSiswaSearch<%=rnd%>(val) {
        if (val.length < 2) return;
        
        var idSekolah = document.getElementById('selectSekolah<%=rnd%>').value;
        if (!idSekolah) {
            showToast<%=rnd%>("<%=Common.getBahasaConfig("Harap pilih Sekolah terlebih dahulu untuk mencari siswa.")%>", "bg-warning text-dark");
            return;
        }

        var payload = { 
            action: "daftar", 
            class: "ais.database.model.sekolah.Siswa", 
            where1: "(nomor_induk_nasional ILIKE '%" + val + "%' OR nama_siswa ILIKE '%" + val + "%') AND sekolah_id = " + idSekolah, 
            max: 20 
        };

        try {
            var res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            var result = await res.json();
            var options = '';
            (result.data || []).forEach(function(row) {
                options += '<option data-id="' + row.id + '" value="' + (row.nomor_induk_nasional || '-') + ' - ' + (row.nama_siswa || '') + '"></option>';
            });
            document.getElementById('dlSiswa<%=rnd%>').innerHTML = options;
        } catch (e) { console.error(e); }
    }

    function checkSelectedSiswa<%=rnd%>(val) {
        var dl = document.getElementById('dlSiswa<%=rnd%>');
        var options = dl.options;
        for(var i=0; i<options.length; i++) {
            if(options[i].value === val) {
                document.getElementById('hiddenIdSiswa<%=rnd%>').value = options[i].getAttribute('data-id');
                return;
            }
        }
        document.getElementById('hiddenIdSiswa<%=rnd%>').value = '';
    }

    // ==========================================
    // FUNGSI UPLOAD & FILE
    // ==========================================
    async function checkExistingFile<%=rnd%>(idPenghargaan, isApproved) {
        var reqObj = { 
            "action": "file", 
            "class": "ais.database.model.file.LampiranLain", 
            "ref": idPenghargaan.toString(), 
            "jenis": masterModelClass<%=rnd%>,
            "refresh": "false", "usingId": false
        };
        try {
            var response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(reqObj)
            });
            var dataResponse = await response.json();
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
        var url = data.url; var filename = data.nama;
        var ext = filename ? filename.split('.').pop().toLowerCase() : '';
        var btnDownload = '<a href="' + url + '" target="_blank" class="btn btn-success fw-bold px-3 shadow-sm"><i class="fas fa-download me-1"></i> <%=Common.getBahasaConfig("Download")%></a>';
        var btnUploadHtml = '';
        if (!isApproved) {
            btnUploadHtml = '<input type="file" id="input_dofile_foto_<%=rnd%>" class="d-none" accept=".pdf, image/*" onchange="uploadFile<%=rnd%>()">' +
                            '<button type="button" class="btn btn-primary fw-bold shadow-sm px-4 me-2" onclick="document.getElementById(\'input_dofile_foto_<%=rnd%>\').click();"><i class="fas fa-sync-alt me-2"></i><%=Common.getBahasaConfig("Ganti Bukti")%></button>';
        }
        document.getElementById('action-buttons-<%=rnd%>').innerHTML = btnUploadHtml + btnDownload;
        
        var html = '';
        if (ext === 'jpg' || ext === 'jpeg' || ext === 'png') {
            html = '<img src="' + url + '" class="img-fluid rounded shadow-sm border" style="max-height: 200px;">';
        } else {
            html = '<a href="' + url + '" target="_blank"><i class="fas fa-file-pdf fa-4x text-danger"></i></a>';
        }
        document.getElementById('preview-area-<%=rnd%>').innerHTML = html;
    }

    function resetUploadButtons<%=rnd%>(isApproved) {
        if (!isApproved) {
            document.getElementById('action-buttons-<%=rnd%>').innerHTML = 
                '<input type="file" id="input_dofile_foto_<%=rnd%>" class="d-none" accept=".pdf, image/*" onchange="uploadFile<%=rnd%>()">' +
                '<button type="button" class="btn btn-primary fw-bold shadow-sm px-4" onclick="document.getElementById(\'input_dofile_foto_<%=rnd%>\').click();"><i class="fas fa-upload me-2"></i><%=Common.getBahasaConfig("Unggah Bukti")%></button>';
        } else {
            document.getElementById('action-buttons-<%=rnd%>').innerHTML = '<span class="text-danger fw-bold"><i class="fas fa-ban me-1"></i><%=Common.getBahasaConfig("Terkunci")%></span>';
        }
    }

    async function uploadFile<%=rnd%>() {
        var fileInput = document.getElementById('input_dofile_foto_<%=rnd%>');
        if (fileInput.files.length === 0 || !activeEditingPenghargaanSiswaId<%=rnd%>) return;
        var file = fileInput.files[0];
        var formData = new FormData();
        formData.append('nama', file.name); 
        formData.append('clazz', 'ais.database.model.file.LampiranLain');
        formData.append('jenis', masterModelClass<%=rnd%>); 
        formData.append('id', activeEditingPenghargaanSiswaId<%=rnd%>);
        formData.append('fileContent', file);
        
        var xhr = new XMLHttpRequest();
        xhr.open('POST', '<%=Common.ROOT%>/DoUpload', true);
        xhr.onload = function () {
            try {
                var res = JSON.parse(xhr.responseText);
                if (res.status === 'success' || res.status === '00') {
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Berkas berhasil diunggah!")%>', "bg-success text-white");
                    checkExistingFile<%=rnd%>(activeEditingPenghargaanSiswaId<%=rnd%>, false);
                }
            } catch (e) { console.error(e); }
            fileInput.value = ''; 
        };
        xhr.send(formData);
    }

    // ==========================================
    // LOAD MASTERS DENGAN PARAMETER TERPILIH
    // ==========================================
    async function loadYayasan<%=rnd%>(targetId, selectedId) {
        var res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: "daftar", class: "ais.database.model.sekolah.Yayasan", order1: "asc", sort1: "nama" })
        });
        var result = await res.json();
        var options = '<option value=""><%=Common.getBahasaConfig("-- Pilih Yayasan --")%></option>';
        (result.data || []).forEach(function(row) { 
            var sel = (selectedId == row.id) ? "selected" : "";
            options += '<option value="' + row.id + '" ' + sel + '>' + row.nama + '</option>'; 
        });
        document.getElementById(targetId).innerHTML = options;
    }

    async function loadSekolah<%=rnd%>(idYayasan, selectedId) {
        if(!idYayasan) { 
            document.getElementById('selectSekolah<%=rnd%>').innerHTML = '<option value=""><%=Common.getBahasaConfig("-- Pilih Sekolah --")%></option>'; 
            return; 
        }
        var res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: "daftar", class: "ais.database.model.sekolah.Sekolah", where1: "yayasan_id = " + idYayasan, order1: "asc", sort1: "nama" })
        });
        var result = await res.json();
        var options = '<option value=""><%=Common.getBahasaConfig("-- Pilih Sekolah --")%></option>';
        (result.data || []).forEach(function(row) { 
            var sel = (selectedId == row.id) ? "selected" : "";
            options += '<option value="' + row.id + '" ' + sel + '>' + row.nama + '</option>'; 
        });
        document.getElementById('selectSekolah<%=rnd%>').innerHTML = options;
    }

    async function loadFilterSekolah<%=rnd%>(idYayasan) {
        if(!idYayasan) { 
            document.getElementById('filterSekolah<%=rnd%>').innerHTML = '<option value=""><%=Common.getBahasaConfig("-- Semua Sekolah --")%></option>'; 
            return; 
        }
        var res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: "daftar", class: "ais.database.model.sekolah.Sekolah", where1: "yayasan_id = " + idYayasan, order1: "asc", sort1: "nama" })
        });
        var result = await res.json();
        var options = '<option value=""><%=Common.getBahasaConfig("-- Semua Sekolah --")%></option>';
        (result.data || []).forEach(function(row) { 
            options += '<option value="' + row.id + '">' + row.nama + '</option>'; 
        });
        document.getElementById('filterSekolah<%=rnd%>').innerHTML = options;
    }

    async function loadKategori<%=rnd%>(targetId, selectedId) {
        var res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: "daftar", class: "ais.database.model.KategoriPenghargaan", order1: "asc", sort1: "nama" })
        });
        var result = await res.json();
        var options = '<option value=""><%=Common.getBahasaConfig("-- Pilih Kategori --")%></option>';
        (result.data || []).forEach(function(row) { 
            var sel = (selectedId == row.id) ? "selected" : "";
            options += '<option value="' + row.id + '" ' + sel + '>' + row.nama + '</option>'; 
        });
        document.getElementById(targetId).innerHTML = options;
    }

    // ==========================================
    // CRUD LOGIC
    // ==========================================
    function resetAndLoadData<%=rnd%>() {
        currentPage<%=rnd%> = 1;
        loadDataPenghargaan<%=rnd%>();
    }

    async function loadDataPenghargaan<%=rnd%>() {
        var tbody = document.getElementById('tabelDataPenghargaan<%=rnd%>');
        var keyword = document.getElementById('searchPenghargaan<%=rnd%>').value.trim();
        var start = (currentPage<%=rnd%> - 1);
        var payload = { action: "daftar", class: masterModelClass<%=rnd%>, deep: "1", max: limitPerPage<%=rnd%>, halaman: start, order1: "desc", sort1: "id", count: "true" };
        var filters = [];
        
        if(keyword !== '') filters.push("(siswa1_.nama_siswa ILIKE '%" + keyword + "%' OR siswa1_.nomor_induk_nasional ILIKE '%" + keyword + "%')");
        
        var fTa = document.getElementById('filterTahunAkademik<%=rnd%>').value;
        if(fTa) filters.push("this_.tahunAkademik = '" + fTa + "'");
        
        var elYayasan = document.getElementById('filterYayasan<%=rnd%>');
        var fYayasan = elYayasan ? elYayasan.value : null;
        if(fYayasan) filters.push("this_.yayasan_id = " + fYayasan);

        if(filters.length > 0) { 
            payload.where1 = filters.join(' AND '); 
            payload.alias1 = "siswa"; 
        }

        try {
            var response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
            });
            var result = await response.json();
            totalRecords<%=rnd%> = parseInt(result.count || 0);
            var html = '';
            (result.data || []).forEach(function(row, i) {
                var badgeClass = (row.status === 'Disetujui') ? 'success' : (row.status === 'Ditolak' ? 'danger' : 'secondary');
                var badge = '<span class="badge bg-' + badgeClass + '">' + (row.status || 'Belum diproses') + '</span>';
                
                var btnEdit = '<button class="btn btn-sm btn-outline-warning me-1" onclick="editPenghargaan<%=rnd%>(' + row.id + ')"><i class="fas fa-edit"></i></button>';
                var btnHapus = '<button class="btn btn-sm btn-outline-danger" onclick="hapusPenghargaan<%=rnd%>(' + row.id + ')"><i class="fas fa-trash-alt"></i></button>';
                
                html += '<tr>' +
                            '<td class="text-center">' + (start * limitPerPage<%=rnd%> + i + 1) + '</td>' +
                            '<td><div class="fw-bold">' + (row["siswa.nama_siswa"] || '-') + '</div><small>' + (row["siswa.nomor_induk_nasional"] || '-') + '</small></td>' +
                            '<td>' + row.nama + '</td>' +
                            '<td>' + (row["sekolah.nama"] || '-') + '</td>' +
                            '<td class="text-center">' + (row["tanggal.formated2"] || '-') + '</td>' +
                            '<td class="text-center">' + badge + '</td>' +
                            '<td class="text-center no-export">' + btnEdit + btnHapus + '</td>' +
                        '</tr>';
            });
            tbody.innerHTML = html || '<tr><td colspan="7" class="text-center text-muted py-4"><%=Common.getBahasaConfig("Tidak ada data.")%></td></tr>';
            updatePaginationUI<%=rnd%>();
        } catch (e) { console.error(e); }
    }

    async function simpanPenghargaan<%=rnd%>() {
        var idSiswa = document.getElementById('hiddenIdSiswa<%=rnd%>').value;
        var currentId = document.getElementById('inputIdPenghargaan<%=rnd%>').value;
        
        if (!idSiswa) { 
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Harap pilih Siswa dari daftar.")%>', "bg-warning text-dark"); 
            return; 
        }
        
        var inputTahunVal = document.getElementById('inputTahun<%=rnd%>').value;
        var katVal = document.getElementById('selectKategori<%=rnd%>').value;
        
        var dataObj = {
            id: currentId ? parseInt(currentId) : null, 
            siswa: parseInt(idSiswa),
            nama: document.getElementById('inputNama<%=rnd%>').value, 
            namaEn: document.getElementById('inputNamaEn<%=rnd%>').value,
            tanggal: document.getElementById('inputTanggal<%=rnd%>').value || null, 
            tahun: inputTahunVal ? parseInt(inputTahunVal) : null,
            tahunAkademik: document.getElementById('inputTahunAkademik<%=rnd%>').value, 
            jenisSemester: document.getElementById('inputJenisSemester<%=rnd%>').value,
            status: document.getElementById('inputStatus<%=rnd%>').value, 
            keterangan: document.getElementById('inputKeterangan<%=rnd%>').value,
            kategoriPenghargaan: katVal ? parseInt(katVal) : null
        };

        var elYayasan = document.getElementById('selectYayasan<%=rnd%>');
        var yayVal = elYayasan ? elYayasan.value : null;
        if(yayVal) dataObj.yayasan = parseInt(yayVal);
        
        var elSekolah = document.getElementById('selectSekolah<%=rnd%>');
        var sekVal = elSekolah ? elSekolah.value : null;
        if(sekVal) dataObj.sekolah = parseInt(sekVal);

        var btn = document.getElementById('btnSimpan<%=rnd%>');
        btn.disabled = true;
        
        try {
            var response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: "simpanDataRinci", log: "true", class: masterModelClass<%=rnd%>, data: dataObj })
            });
            var result = await response.json();
            
            if (result.status === '00' || result.status === 'success') {
                var savedId = result.id || (result.data && result.data.id) || currentId;
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data berhasil disimpan!")%>', "bg-success text-white");
                
                activeEditingPenghargaanSiswaId<%=rnd%> = savedId;
                document.getElementById('inputIdPenghargaan<%=rnd%>').value = savedId;
                document.getElementById('uploadPhotoArea<%=rnd%>').style.display = 'block';
                checkExistingFile<%=rnd%>(savedId, dataObj.status === 'Disetujui');
                loadDataPenghargaan<%=rnd%>();
                
                if (currentId && isDocumentUploaded<%=rnd%>) {
                    tutupForm<%=rnd%>();
                }
            }
        } catch (e) { 
            console.error(e); 
        } finally { 
            btn.disabled = false; 
        }
    }

    async function editPenghargaan<%=rnd%>(id) {
        bukaFormTambah<%=rnd%>();
        activeEditingPenghargaanSiswaId<%=rnd%> = id;
        document.getElementById('inputIdPenghargaan<%=rnd%>').value = id;
        document.getElementById('uploadPhotoArea<%=rnd%>').style.display = 'block';
        
        var res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: "load", class: masterModelClass<%=rnd%>, id: id })
        });
        var result = await res.json();
        var data = result.data;
        
        if(data["siswa.id"]) {
            document.getElementById('hiddenIdSiswa<%=rnd%>').value = data["siswa.id"];
            var elCariSiswa = document.getElementById('inputCariSiswa<%=rnd%>');
            if(elCariSiswa) {
                elCariSiswa.value = (data["siswa.nomor_induk_nasional"] || '-') + ' - ' + (data["siswa.nama_siswa"] || '');
            }
        }
        
        document.getElementById('inputNama<%=rnd%>').value = data.nama || '';
        document.getElementById('inputNamaEn<%=rnd%>').value = data.namaEn || '';
        document.getElementById('inputTahunAkademik<%=rnd%>').value = data.tahunAkademik || '';
        document.getElementById('inputJenisSemester<%=rnd%>').value = data.jenisSemester || '';
        document.getElementById('inputTahun<%=rnd%>').value = data.tahun || '';
        document.getElementById('inputStatus<%=rnd%>').value = data.status || 'Belum diproses';
        document.getElementById('inputKeterangan<%=rnd%>').value = data.keterangan || '';
        
        var tgl = data["tanggal.datetime_format"] || data.tanggal;
        document.getElementById('inputTanggal<%=rnd%>').value = tgl ? tgl.substring(0, 10) : '';
        
        await loadYayasan<%=rnd%>('selectYayasan<%=rnd%>', data["yayasan.id"]);
        await loadSekolah<%=rnd%>(data["yayasan.id"], data["sekolah.id"]);
        await loadKategori<%=rnd%>('selectKategori<%=rnd%>', data["kategoriPenghargaan.id"]);
        
        checkExistingFile<%=rnd%>(id, data.status === 'Disetujui');
    }

    async function hapusPenghargaan<%=rnd%>(id) {
        if (!confirm('<%=Common.getBahasaConfigJS("Hapus data?")%>')) return;
        var res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: "hapusDataRinci", class: masterModelClass<%=rnd%>, id: id })
        });
        var result = await res.json();
        if(result.status === '00' || result.status === 'success') {
            loadDataPenghargaan<%=rnd%>();
        }
    }

    // ==========================================
    // LOGIKA AUTO-SELECT PADA TAMBAH DATA BARU
    // ==========================================
    async function bukaFormTambah<%=rnd%>() {
        document.getElementById('formPenghargaan<%=rnd%>').reset();
        document.getElementById('inputIdPenghargaan<%=rnd%>').value = '';
        activeEditingPenghargaanSiswaId<%=rnd%> = "";
        document.getElementById('uploadPhotoArea<%=rnd%>').style.display = 'none';

        // Pre-select berdasarkan konteks login
        var elYayasan = document.getElementById('selectYayasan<%=rnd%>');
        if (elYayasan) {
            if (idYayasanLogin<%=rnd%>) {
                await loadYayasan<%=rnd%>('selectYayasan<%=rnd%>', idYayasanLogin<%=rnd%>);
                elYayasan.disabled = true;
                
                var elSekolah = document.getElementById('selectSekolah<%=rnd%>');
                if (idSekolahLogin<%=rnd%>) {
                    await loadSekolah<%=rnd%>(idYayasanLogin<%=rnd%>, idSekolahLogin<%=rnd%>);
                    elSekolah.disabled = true;
                } else {
                    await loadSekolah<%=rnd%>(idYayasanLogin<%=rnd%>, null);
                    elSekolah.disabled = false;
                }
            } else {
                await loadYayasan<%=rnd%>('selectYayasan<%=rnd%>', null);
                elYayasan.disabled = false;
                loadSekolah<%=rnd%>(null, null);
                document.getElementById('selectSekolah<%=rnd%>').disabled = false;
            }
            
            // Bersihkan kolom pencarian siswa untuk data baru
            document.getElementById('inputCariSiswa<%=rnd%>').value = '';
            document.getElementById('hiddenIdSiswa<%=rnd%>').value = '';
        }

        loadKategori<%=rnd%>('selectKategori<%=rnd%>', null);
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    }

    function tutupForm<%=rnd%>() {
        document.getElementById('viewForm<%=rnd%>').style.display = 'none';
        document.getElementById('viewList<%=rnd%>').style.display = 'block';
    }

    function updatePaginationUI<%=rnd%>() {
        var totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumberDisplay<%=rnd%>').innerText = currentPage<%=rnd%> + ' / ' + totalPages;
        document.getElementById('pagingInfoPenghargaan<%=rnd%>').innerText = 'Total: ' + totalRecords<%=rnd%> + ' data';
    }

    function changePage<%=rnd%>(dir) {
        var totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if(dir === -1 && currentPage<%=rnd%> > 1) { 
            currentPage<%=rnd%>--; 
            loadDataPenghargaan<%=rnd%>(); 
        }
        else if(dir === 1 && currentPage<%=rnd%> < totalPages) { 
            currentPage<%=rnd%>++; 
            loadDataPenghargaan<%=rnd%>(); 
        }
    }

    document.addEventListener("DOMContentLoaded", function() {
        if (document.getElementById('filterYayasan<%=rnd%>')) {
            loadYayasan<%=rnd%>('filterYayasan<%=rnd%>', null);
        }
        
        document.getElementById('searchPenghargaan<%=rnd%>').addEventListener("keydown", function(e) {
            if (e.key === "Enter") {
                e.preventDefault();
                resetAndLoadData<%=rnd%>(); 
            }
        });

        loadDataPenghargaan<%=rnd%>();
    });
</script>