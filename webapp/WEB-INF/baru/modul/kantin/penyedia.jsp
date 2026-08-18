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
%>

<div id="viewList<%=rnd%>" class="animate__animated animate__fadeInUp">
    <div class="row mb-4">
        <div class="col-12">
            <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
                
                <div class="card-header bg-white border-0 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-3 border-bottom border-light">
                    <div>
                        <h5 class="fw-bold text-dark mb-1">
                            <i class="fas fa-truck-loading text-primary me-2"></i><%=Common.getBahasaConfig("Manajemen Data Penyedia (Supplier)")%>
                        </h5>
                        <small class="text-muted"><%=Common.getBahasaConfig("Kelola daftar mitra kerja, vendor, dan penyedia barang/jasa koperasi.")%></small>
                    </div>
                    
                    <div class="d-flex align-items-center gap-2 flex-wrap">
                        <div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;">
                            <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>
                            <input type="text" class="form-control border-start-0 ps-0 bg-white fw-medium" id="searchPenyedia<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Nama, Alamat, Kontak...")%>">
                        </div>
                        
                        <button class="btn btn-sm btn-outline-secondary rounded-pill px-3 shadow-sm fw-bold" onclick="resetAndLoadData<%=rnd%>()" title="<%=Common.getBahasaConfig("Muat Ulang Data")%>">
                            <i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Segarkan")%>
                        </button>
                        
                        <button class="btn btn-sm btn-primary rounded-pill px-4 shadow-sm fw-bold text-nowrap" onclick="bukaFormTambah<%=rnd%>()">
                            <i class="fas fa-plus-circle me-1"></i><%=Common.getBahasaConfig("Tambah Penyedia")%>
                        </button>
                    </div>
                </div>
                
                <div class="card-body p-4 pt-3">
                    <div class="table-responsive border rounded-3 shadow-sm bg-white">
                        <table class="table table-hover table-striped align-middle mb-0">
                            <thead class="table-dark text-center align-middle">
                                <tr>
                                    <th class="fw-semibold text-uppercase small px-3 py-3" style="width: 60px;">#</th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-building me-1"></i><%=Common.getBahasaConfig("Nama Perusahaan / Vendor")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-address-book me-1"></i><%=Common.getBahasaConfig("Info Kontak")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-map-marker-alt me-1"></i><%=Common.getBahasaConfig("Alamat")%></th>
                                    <th class="fw-semibold text-uppercase small px-3" style="width: 120px;"><i class="fas fa-toggle-on me-1"></i><%=Common.getBahasaConfig("Status")%></th>
                                    <th class="fw-semibold text-uppercase small px-3 text-center" style="width: 120px;"><i class="fas fa-cogs"></i></th>
                                </tr>
                            </thead>
                            <tbody id="tabelDataPenyedia<%=rnd%>">
                                <tr><td colspan="6" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Memuat data penyedia...")%></span></td></tr>
                            </tbody>
                        </table>
                    </div>

                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mt-4">
                        <div class="small fw-medium text-muted bg-light px-3 py-1 rounded-pill border" id="pagingInfoPenyedia<%=rnd%>"></div>
                        <nav>
                            <ul class="pagination pagination-sm mb-0 shadow-sm">
                                <li class="page-item"><button class="page-link text-primary rounded-start-pill px-3 fw-bold" id="btnPrevPage<%=rnd%>" onclick="changePagePenyedia<%=rnd%>(-1)"><i class="fas fa-chevron-left me-1"></i></button></li>
                                <li class="page-item disabled"><span class="page-link bg-light text-dark px-3 fw-bold" id="pageNumberDisplay<%=rnd%>">1</span></li>
                                <li class="page-item"><button class="page-link text-primary rounded-end-pill px-3 fw-bold" id="btnNextPage<%=rnd%>" onclick="changePagePenyedia<%=rnd%>(1)"><i class="fas fa-chevron-right ms-1"></i></button></li>
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
        <div class="col-lg-8">
            <div class="card border-0 shadow-sm rounded-4 border-top border-success border-4">
                <div class="card-header bg-white border-0 pt-4 pb-0 px-4">
                    <div class="d-flex justify-content-between align-items-center">
                        <h5 class="fw-bold text-dark mb-0" id="formTitle<%=rnd%>">
                            <i class="fas fa-edit text-success me-2"></i><%=Common.getBahasaConfig("Formulir Data Penyedia")%>
                        </h5>
                        <div class="form-check form-switch fs-5 mb-0">
                            <input class="form-check-input" type="checkbox" id="inputStatusAktif<%=rnd%>" checked style="cursor: pointer;">
                            <label class="form-check-label fs-6 fw-bold text-success" for="inputStatusAktif<%=rnd%>" id="labelStatusAktif<%=rnd%>"><%=Common.getBahasaConfig("Aktif")%></label>
                        </div>
                    </div>
                </div>
                <div class="card-body p-4">
                    <form id="formPenyedia<%=rnd%>" onsubmit="event.preventDefault(); simpanPenyedia<%=rnd%>();">
                        <input type="hidden" id="inputIdPenyedia<%=rnd%>" value="">
                        
                        <div class="row g-4 mt-1">
                            <div class="col-md-12">
                                <h6 class="fw-bold text-primary border-bottom pb-2 mb-0"><i class="fas fa-building me-2"></i>Profil Perusahaan / Vendor</h6>
                            </div>
                            
                            <div class="col-md-8">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Penyedia (Perusahaan/Toko)")%> <span class="text-danger">*</span></label>
                                <input type="text" class="form-control fw-bold shadow-sm" id="inputNama<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: PT. Sumber Makmur Jaya")%>" required>
                            </div>
                            
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("NPWP")%></label>
                                <input type="text" class="form-control shadow-sm" id="inputNpwp<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Nomor Pokok Wajib Pajak")%>">
                            </div>

                            <div class="col-12">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Alamat Lengkap")%></label>
                                <textarea class="form-control shadow-sm" id="inputAlamat<%=rnd%>" rows="3" placeholder="<%=Common.getBahasaConfig("Jalan, RT/RW, Kelurahan, Kota...")%>"></textarea>
                            </div>

                            <div class="col-md-12 mt-4">
                                <h6 class="fw-bold text-primary border-bottom pb-2 mb-0"><i class="fas fa-address-book me-2"></i>Informasi Kontak</h6>
                            </div>

                            <div class="col-md-12">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Kontak Person (CP/Direktur)")%> <span class="text-danger">*</span></label>
                                <div class="input-group shadow-sm">
                                    <span class="input-group-text bg-light text-muted"><i class="fas fa-user-tie"></i></span>
                                    <input type="text" class="form-control" id="inputCp<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Nama perwakilan yang bisa dihubungi")%>" required>
                                </div>
                            </div>

                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nomor HP / telp")%></label>
                                <div class="input-group shadow-sm">
                                    <span class="input-group-text bg-light text-success"><i class="fab fa-whatsapp"></i></span>
                                    <input type="tel" class="form-control fw-medium" id="inputtelp<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: 081234567890")%>">
                                </div>
                            </div>

                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Alamat Email")%></label>
                                <div class="input-group shadow-sm">
                                    <span class="input-group-text bg-light text-muted"><i class="fas fa-envelope"></i></span>
                                    <input type="email" class="form-control" id="inputEmail<%=rnd%>" placeholder="<%=Common.getBahasaConfig("email@perusahaan.com")%>">
                                </div>
                            </div>

                            <div class="col-12 mt-5 text-end border-top pt-3">
                                <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold border shadow-sm" onclick="tutupForm<%=rnd%>()"><i class="fas fa-times text-danger me-2"></i><%=Common.getBahasaConfig("Batal")%></button>
                                <button type="submit" class="btn btn-success px-4 rounded-pill shadow-sm fw-bold" id="btnSimpan<%=rnd%>">
                                    <i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Data")%>
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
    // BAGIAN 1: UTILITIES & STATE GLOBAL
    // ==========================================
    const masterModelClass<%=rnd%> = "ais.database.model.asset.PenyediaAsset";
    
    // Variabel Paging
    let currentPage<%=rnd%> = 1;
    const limitPerPage<%=rnd%> = 10;
    let totalRecords<%=rnd%> = 0;

    const fetchData<%=rnd%> = async (sql) => {
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sql: sql, action: "sql" })
            });
            const result = await res.json();
            return result.data || [];
        } catch (e) { 
            console.error("Fetch SQL Error:", e); 
            return []; 
        }
    };

    const showToast<%=rnd%> = (msg, colorClass) => {
        if(typeof tampilkanToast === "function") {
            tampilkanToast(msg, colorClass);
        } else {
            alert(msg);
        }
    };

    // Label UI Switch Status Aktif
    document.getElementById('inputStatusAktif<%=rnd%>').addEventListener('change', function() {
        const lbl = document.getElementById('labelStatusAktif<%=rnd%>');
        if(this.checked) {
            lbl.innerText = '<%=Common.getBahasaConfigJS("Aktif")%>';
            lbl.className = 'form-check-label fs-6 fw-bold text-success';
        } else {
            lbl.innerText = '<%=Common.getBahasaConfigJS("Tidak Aktif")%>';
            lbl.className = 'form-check-label fs-6 fw-bold text-danger';
        }
    });


    // ==========================================
    // BAGIAN 2: LOGIKA LIST, PAGING & SEARCH
    // ==========================================
    const buildFilterSQL<%=rnd%> = () => {
        const keyword = document.getElementById('searchPenyedia<%=rnd%>').value.trim().replace(/'/g, "''");
        let filters = " WHERE 1=1 ";
        if (keyword !== '') {
            filters += " AND (nama ILIKE '%" + keyword + "%' OR alamat ILIKE '%" + keyword + "%' OR telp ILIKE '%" + keyword + "%' OR email ILIKE '%" + keyword + "%' OR npwp ILIKE '%" + keyword + "%' OR kontak ILIKE '%" + keyword + "%') ";
        }
        return filters;
    };

    const resetAndLoadData<%=rnd%> = () => {
        currentPage<%=rnd%> = 1; 
        loadDataPenyedia<%=rnd%>();
    };

    const loadDataPenyedia<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataPenyedia<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="6" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Mengambil data...")%></span></td></tr>';

        const sqlFilters = buildFilterSQL<%=rnd%>();
        const offset = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%>;

        const sqlCount = "SELECT COUNT(*) AS jumlah FROM asset.penyedia_asset " + sqlFilters + ";";
        // Asumsi nama tabel di database adalah 'ais_penyedia_asset' atau sesuai mapping JPA Anda.
        // Asumsi field: nama, alamat, telp, email, npwp, kontak, aktif
        const sqlData = "SELECT id, nama, alamat, telp, email, npwp, kontak, aktif " +
                        "FROM asset.penyedia_asset " +
                        sqlFilters + " ORDER BY nama ASC LIMIT " + limitPerPage<%=rnd%> + " OFFSET " + offset + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetchData<%=rnd%>(sqlCount),
                fetchData<%=rnd%>(sqlData)
            ]);

            totalRecords<%=rnd%> = (resCount && resCount[0]) ? parseInt(resCount[0].jumlah || 0) : 0;
            const recordList = resData || [];

            let htmlList = '';

            if (recordList.length === 0) {
                htmlList = '<tr><td colspan="6" class="text-center text-muted py-5"><i class="fas fa-truck-loading fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Tidak ada data penyedia ditemukan.")%></td></tr>';
            } else {
                let no = offset + 1; 
                recordList.forEach((row) => {
                    
                    const isAktif = (row.aktif === null || row.aktif === true || row.aktif === 'true');
                    const badgeStatus = isAktif ? '<span class="badge bg-success bg-opacity-10 text-success border border-success px-2 py-1"><i class="fas fa-check-circle me-1"></i>Aktif</span>' : '<span class="badge bg-danger bg-opacity-10 text-danger border border-danger px-2 py-1"><i class="fas fa-times-circle me-1"></i>Non-Aktif</span>';

                    // Info Perusahaan
                    const safeNama = (row.nama || '-').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    const infoPerusahaan = '<div class="fw-bold text-dark fs-6">' + safeNama + '</div>' + 
                                           '<small class="text-muted"><i class="fas fa-id-card me-1"></i>NPWP: ' + (row.npwp || '-') + '</small>';

                    // Info Kontak
                    let infoKontak = '<div class="fw-semibold text-primary"><i class="fas fa-user-tie me-1"></i>' + (row.kontak || '-') + '</div>';
                    if (row.telp) infoKontak += '<div class="text-muted small mt-1"><i class="fab fa-whatsapp text-success me-1"></i>' + row.telp + '</div>';
                    if (row.email) infoKontak += '<div class="text-muted small"><i class="fas fa-envelope text-secondary me-1"></i>' + row.email + '</div>';

                    // Alamat (Truncate jika kepanjangan)
                    const textAlamat = row.alamat || '-';
                    const displayAlamat = textAlamat.length > 50 ? textAlamat.substring(0, 50) + '...' : textAlamat;

                    htmlList += '<tr>' +
                            '<td class="text-center text-muted fw-medium">' + (no++) + '</td>' +
                            '<td class="text-start">' + infoPerusahaan + '</td>' +
                            '<td class="text-start">' + infoKontak + '</td>' +
                            '<td class="text-start text-muted small" title="' + textAlamat + '">' + displayAlamat + '</td>' +
                            '<td class="text-center">' + badgeStatus + '</td>' +
                            '<td class="text-center text-nowrap">' +
                                '<button class="btn btn-sm btn-outline-warning text-dark shadow-sm px-2 me-1 fw-bold" onclick="editPenyedia<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Edit Data")%>"><i class="fas fa-edit"></i></button>' +
                                '<button class="btn btn-sm btn-outline-danger shadow-sm px-2 fw-bold" onclick="hapusPenyedia<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Hapus Data")%>"><i class="fas fa-trash-alt"></i></button>' +
                            '</td>' +
                        '</tr>';
                });
            }
            tbody.innerHTML = htmlList;
            updatePaginationUI<%=rnd%>();

        } catch (error) {
            console.error("Gagal load tabel penyedia:", error);
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal menarik data. Silakan muat ulang.")%></td></tr>';
        }
    };

    const updatePaginationUI<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumberDisplay<%=rnd%>').innerText = currentPage<%=rnd%> + " / " + totalPages;
        
        const startIdx = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%> + 1;
        const endIdx = Math.min(currentPage<%=rnd%> * limitPerPage<%=rnd%>, totalRecords<%=rnd%>);
        const startInfo = totalRecords<%=rnd%> > 0 ? startIdx : 0;
        
        document.getElementById('pagingInfoPenyedia<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startInfo + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari")%> <b class="text-dark">' + totalRecords<%=rnd%> + '</b> <%=Common.getBahasaConfig("data")%>';
        
        document.getElementById('btnPrevPage<%=rnd%>').disabled = (currentPage<%=rnd%> === 1);
        document.getElementById('btnNextPage<%=rnd%>').disabled = (currentPage<%=rnd%> === totalPages);
    };

    const changePagePenyedia<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if (direction === -1 && currentPage<%=rnd%> > 1) {
            currentPage<%=rnd%>--;
            loadDataPenyedia<%=rnd%>();
        } else if (direction === 1 && currentPage<%=rnd%> < totalPages) {
            currentPage<%=rnd%>++;
            loadDataPenyedia<%=rnd%>();
        }
    };

    // ==========================================
    // BAGIAN 3: LOGIKA FORM & DINAMIS UI
    // ==========================================
    const bukaFormTambah<%=rnd%> = () => {
        document.getElementById('inputIdPenyedia<%=rnd%>').value = '';
        document.getElementById('formPenyedia<%=rnd%>').reset();
        
        const chkStatus = document.getElementById('inputStatusAktif<%=rnd%>');
        chkStatus.checked = true;
        chkStatus.dispatchEvent(new Event('change'));

        document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-plus-circle text-primary me-2"></i><%=Common.getBahasaConfig("Tambah Data Penyedia Baru")%>';
        
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    };

    const tutupForm<%=rnd%> = () => {
        document.getElementById('viewForm<%=rnd%>').style.display = 'none';
        document.getElementById('viewList<%=rnd%>').style.display = 'block';
        loadDataPenyedia<%=rnd%>();
    };

    // ==========================================
    // BAGIAN 4: SIMPAN, EDIT, & HAPUS
    // ==========================================
    const simpanPenyedia<%=rnd%> = async () => {
        const idPenyedia = document.getElementById('inputIdPenyedia<%=rnd%>').value;
        const btnSimpan = document.getElementById('btnSimpan<%=rnd%>');

        // CATATAN PENTING:
        // Sesuaikan key property pada object dataObj ini dengan penamaan field persis di PenyediaAsset.java Anda.
        const dataObj = {
            nama: document.getElementById('inputNama<%=rnd%>').value.trim(),
            npwp: document.getElementById('inputNpwp<%=rnd%>').value.trim(),
            alamat: document.getElementById('inputAlamat<%=rnd%>').value.trim(),
            kontak: document.getElementById('inputCp<%=rnd%>').value.trim(), // Pastikan nama property Java nya sesuai
            telp: document.getElementById('inputtelp<%=rnd%>').value.trim(),
            email: document.getElementById('inputEmail<%=rnd%>').value.trim(),
            aktif: document.getElementById('inputStatusAktif<%=rnd%>').checked
        };
        
        const payload = { 
            action: "simpanDataRinci", 
            class: masterModelClass<%=rnd%>, 
            data: dataObj
        };

        if (idPenyedia !== '') {
            payload.id = idPenyedia;
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
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data penyedia berhasil disimpan!")%>', 'bg-success text-white');
                tutupForm<%=rnd%>(); 
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

    const editPenyedia<%=rnd%> = async (id) => {
        // Tarik data berdasarkan ID
        const sql = 'SELECT id, nama, npwp, alamat, kontak, telp, email, aktif FROM asset.penyedia_asset WHERE id = ' + id;
        const res = await fetchData<%=rnd%>(sql);
        
        if (res.length > 0) {
            const data = res[0];
            
            document.getElementById('inputIdPenyedia<%=rnd%>').value = data.id;
            document.getElementById('inputNama<%=rnd%>').value = data.nama || '';
            document.getElementById('inputNpwp<%=rnd%>').value = data.npwp || '';
            document.getElementById('inputAlamat<%=rnd%>').value = data.alamat || '';
            document.getElementById('inputCp<%=rnd%>').value = data.kontak || '';
            document.getElementById('inputtelp<%=rnd%>').value = data.telp || '';
            document.getElementById('inputEmail<%=rnd%>').value = data.email || '';

            const isAktif = (data.aktif === null || data.aktif === true || data.aktif === 'true');
            const chkAktif = document.getElementById('inputStatusAktif<%=rnd%>');
            chkAktif.checked = isAktif;
            chkAktif.dispatchEvent(new Event('change')); 
            
            document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-edit text-warning me-2"></i><%=Common.getBahasaConfig("Ubah Data Penyedia")%>';
            
            document.getElementById('viewList<%=rnd%>').style.display = 'none';
            document.getElementById('viewForm<%=rnd%>').style.display = 'block';
        }
    };

    const hapusPenyedia<%=rnd%> = async (id) => {
        if (typeof prosesDeleteData === 'function') {
            prosesDeleteData(masterModelClass<%=rnd%>, id, function(){ 
                 if (document.querySelectorAll('#tabelDataPenyedia<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) {
                     currentPage<%=rnd%>--;
                 }
                 loadDataPenyedia<%=rnd%>();
            });
        } else {
            const isConfirm = confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus data penyedia ini?")%>');
            if (!isConfirm) return;

            const payload = {
                action: "deleteData",
                class: masterModelClass<%=rnd%>,
                id: id
            };

            try {
                const response = await fetch('<%=Common.ROOT%>/Data', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });

                const result = await response.json();
                if (result.status === '00' || result.status === 'success') {
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data berhasil dihapus!")%>', 'bg-success text-white');
                    if (document.querySelectorAll('#tabelDataPenyedia<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) {
                        currentPage<%=rnd%>--;
                    }
                    loadDataPenyedia<%=rnd%>();
                } else {
                    showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Data gagal dihapus.")%>', 'bg-danger text-white');
                }
            } catch (error) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal terhubung ke server.")%>', 'bg-danger text-white');
            }
        }
    };

    // INISIALISASI
    document.addEventListener("DOMContentLoaded", () => {
        document.getElementById('searchPenyedia<%=rnd%>').addEventListener("keydown", (e) => {
            if (e.key === "Enter") {
                e.preventDefault();
                resetAndLoadData<%=rnd%>(); // Saat enter pencarian, reset paging ke 1
            }
        });
        
        // Auto Load saat halaman terbuka
        loadDataPenyedia<%=rnd%>();
    });
</script>