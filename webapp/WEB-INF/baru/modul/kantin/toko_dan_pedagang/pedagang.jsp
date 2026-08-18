<%@page import="java.net.URLEncoder"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// 1. SECURITY CHECK & ROLE IDENTIFICATION
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	out.print("{\"status\":\"error\", \"message\":\""
	+ Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
	return;
}
Pedagang pedagangLogin = tbmuser.getPedagang();
Toko tokoLogin = pedagangLogin == null ? null : pedagangLogin.getToko();
String rnd = Common.getGeneratedBarCode(7);

// Aturan: Admin bisa semua toko. Pedagang bisa kelola akun di TOKONYA SENDIRI.
boolean isAdmin = (tokoLogin == null);
String idTokoAktif = tokoLogin != null ? String.valueOf(tokoLogin.getId()) : "";
String namaTokoAktif = tokoLogin != null ? tokoLogin.getNama() : "";
%>

<div id="viewList<%=rnd%>" class="animate__animated animate__fadeInUp">
    <div class="row mb-4">
        <div class="col-12">
            <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
                
                <div class="card-header bg-white border-0 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-3 border-bottom border-light">
                    <div>
                        <h5 class="fw-bold text-dark mb-1">
                            <i class="fas fa-users-cog text-primary me-2"></i>
                            <% if (isAdmin) { %>
                                <%=Common.getBahasaConfig("Manajemen Akun Pedagang (Semua Toko)")%>
                            <% } else { %>
                                <%=Common.getBahasaConfig("Manajemen Akun - ")%> <span class="text-primary"><%=namaTokoAktif%></span>
                            <% } %>
                        </h5>
                        <small class="text-muted"><%=Common.getBahasaConfig("Kelola akses login (UserID dan Kata Sandi) untuk penjaga kios.")%></small>
                    </div>
                    
                    <div class="d-flex align-items-center gap-2 flex-wrap">
                        <button class="btn btn-sm btn-outline-secondary rounded-pill px-3 shadow-sm fw-bold" onclick="resetAndLoadData<%=rnd%>()" title="<%=Common.getBahasaConfig("Muat Ulang Data")%>">
                            <i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Segarkan")%>
                        </button>
                        
                        <button class="btn btn-sm btn-primary rounded-pill px-4 shadow-sm fw-bold text-nowrap" onclick="bukaFormTambah<%=rnd%>()">
                            <i class="fas fa-user-plus me-1"></i><%=Common.getBahasaConfig("Tambah Akun")%>
                        </button>
                    </div>
                </div>
                
                <div class="card-body p-4 pt-3">
                    
                    <div class="bg-light p-3 rounded-3 border shadow-sm mb-4">
                        <div class="row g-2 align-items-end">
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("User ID")%></label>
                                <input type="text" class="form-control form-control-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterUserId<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari User ID...")%>">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Nama Pedagang")%></label>
                                <input type="text" class="form-control form-control-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterNama<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari nama pedagang...")%>">
                            </div>
                            
                            <% if (isAdmin) { %>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Filter Toko/Kios")%></label>
                                <select class="form-select form-select-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterToko<%=rnd%>">
                                    <option value=""><%=Common.getBahasaConfig("- Semua Toko -")%></option>
                                </select>
                            </div>
                            <% } else { %>
                                <input type="hidden" id="filterToko<%=rnd%>" value="<%=idTokoAktif%>">
                            <% } %>

                            <div class="col-md-auto ms-auto">
                                <button class="btn btn-sm btn-secondary fw-bold px-3 shadow-sm" onclick="triggerSearchPedagang<%=rnd%>()">
                                    <i class="fas fa-search me-1"></i> <%=Common.getBahasaConfig("Saring")%>
                                </button>
                            </div>
                        </div>
                    </div>

                    <div class="table-responsive border rounded-3 shadow-sm bg-white">
                        <table class="table table-hover table-striped align-middle mb-0">
                            <thead class="table-dark text-center align-middle">
                                <tr>
                                    <th class="fw-semibold text-uppercase small px-3 py-3" style="width: 80px;"><%=Common.getBahasaConfig("Foto")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-id-badge me-1"></i><%=Common.getBahasaConfig("User ID (Login)")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-user me-1"></i><%=Common.getBahasaConfig("Nama Pedagang")%></th>
                                    <% if (isAdmin) { %>
                                        <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-store me-1"></i><%=Common.getBahasaConfig("Toko")%></th>
                                    <% } %>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-align-left me-1"></i><%=Common.getBahasaConfig("Keterangan")%></th>
                                    <th class="fw-semibold text-uppercase small px-3" style="width: 120px;"><i class="fas fa-toggle-on me-1"></i><%=Common.getBahasaConfig("Status")%></th>
                                    <th class="fw-semibold text-uppercase small px-3 text-center" style="width: 100px;"><i class="fas fa-cogs"></i></th>
                                </tr>
                            </thead>
                            <tbody id="tabelDataPedagang<%=rnd%>">
                                <tr><td colspan="<%= isAdmin ? 7 : 6 %>" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Memuat data akun pedagang...")%></span></td></tr>
                            </tbody>
                        </table>
                    </div>

                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mt-4">
                        <div class="small fw-medium text-muted bg-light px-3 py-1 rounded-pill border" id="pagingInfoPedagang<%=rnd%>">
                        </div>
                        <nav>
                            <ul class="pagination pagination-sm mb-0 shadow-sm">
                                <li class="page-item">
                                    <button class="page-link text-primary rounded-start-pill px-3 fw-bold" id="btnPrevPage<%=rnd%>" onclick="changePagePedagang<%=rnd%>(-1)">
                                        <i class="fas fa-chevron-left me-1"></i><%=Common.getBahasaConfig("Sebelumnnya")%>
                                    </button>
                                </li>
                                <li class="page-item disabled">
                                    <span class="page-link bg-light text-dark px-3 fw-bold" id="pageNumberDisplay<%=rnd%>">1</span>
                                </li>
                                <li class="page-item">
                                    <button class="page-link text-primary rounded-end-pill px-3 fw-bold" id="btnNextPage<%=rnd%>" onclick="changePagePedagang<%=rnd%>(1)">
                                        <%=Common.getBahasaConfig("Selanjutnya")%><i class="fas fa-chevron-right ms-1"></i>
                                    </button>
                                </li>
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
        <div class="col-lg-7">
            <div class="card border-0 shadow-sm rounded-4 border-top border-success border-4">
                <div class="card-header bg-white border-0 pt-4 pb-0 px-4">
                    <div class="d-flex justify-content-between align-items-center">
                        <h5 class="fw-bold text-dark mb-0" id="formTitle<%=rnd%>">
                            <i class="fas fa-user-shield text-success me-2"></i><%=Common.getBahasaConfig("Formulir Akun Pedagang")%>
                        </h5>
                        
                        <div class="form-check form-switch fs-5 mb-0">
                            <input class="form-check-input" type="checkbox" id="inputStatusAktif<%=rnd%>" checked style="cursor: pointer;">
                            <label class="form-check-label fs-6 fw-bold text-success" for="inputStatusAktif<%=rnd%>" id="labelStatusAktif<%=rnd%>"><%=Common.getBahasaConfig("Aktif")%></label>
                        </div>
                    </div>
                </div>
                <div class="card-body p-4">
                    <form id="formPedagang<%=rnd%>" onsubmit="event.preventDefault(); simpanPedagang<%=rnd%>();">
                        <input type="hidden" id="inputIdPedagang<%=rnd%>" value="">
                        <input type="hidden" id="inputOldUserId<%=rnd%>" value="">
                        
                        <div class="row g-3 mt-1">
                            
                            <% if (isAdmin) { %>
                            <div class="col-md-12">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Pilih Toko/Kios")%> <span class="text-danger">*</span></label>
                                <select class="form-select shadow-sm fw-semibold" id="inputTokoForm<%=rnd%>" required>
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Toko --")%></option>
                                </select>
                            </div>
                            <% } else { %>
                                <input type="hidden" id="inputTokoForm<%=rnd%>" value="<%=idTokoAktif%>">
                            <% } %>
                            
                            <div class="col-md-12 mb-3 mt-4 text-center" id="uploadPhotoArea<%=rnd%>" style="display: none;">
                                <label class="form-label small fw-bold text-secondary d-block"><%=Common.getBahasaConfig("Foto Profil Pedagang")%></label>
                                
                                <div class="mb-2">
                                    <img id="previewImage<%=rnd%>" src="<%=Common.ROOT%>/img/administrator-icon_default.png" onerror="this.onerror=null; this.src='<%=Common.ROOT%>/img/administrator-icon_default.png';" alt="Profile" class="rounded-circle shadow-sm object-fit-cover border border-2 border-primary" style="width: 120px; height: 120px; background-color: #f8f9fa;">
                                </div>
                                
                                <div class="d-flex justify-content-center align-items-center gap-2 mt-3">
                                    <input type="file" id="input_dofile_foto_<%=rnd%>" class="d-none" accept="image/*" onchange="uploadFile<%=rnd%>()">
                                    <input type="hidden" id="input_foto_<%=rnd%>" value="">
                                    <input type="hidden" id="fileNameDisplay<%=rnd%>" value="">
                                    
                                    <button type="button" class="btn btn-sm btn-outline-primary fw-bold rounded-pill shadow-sm px-4" onclick="document.getElementById('input_dofile_foto_<%=rnd%>').click();">
                                        <i class="fas fa-camera me-2"></i><%=Common.getBahasaConfig("Ganti Foto")%>
                                    </button>
                                </div>

                                <div id="progress-wrapper-<%=rnd%>" class="mt-2" style="display: none; max-width: 300px; margin: 0 auto;">
                                    <div class="progress" style="height: 15px;">
                                        <div id="progress-bar-<%=rnd%>" class="progress-bar bg-primary progress-bar-striped progress-bar-animated fw-bold" role="progressbar" style="width: 0%; font-size: 10px;">0%</div>
                                    </div>
                                </div>
                                <small class="text-muted mt-2 d-block fst-italic"><%=Common.getBahasaConfig("Hanya file JPG, JPEG, PNG yang diperbolehkan.")%></small>
                            </div>

                            <div class="col-md-6">
                                <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("User ID (Untuk Login)")%> <span class="text-danger">*</span></label>
                                <div class="input-group input-group-sm shadow-sm">
                                    <span class="input-group-text bg-light border-end-0"><i class="fas fa-user-lock text-muted"></i></span>
                                    <input type="text" class="form-control fw-bold border-start-0" id="inputUserId<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: kios_agus")%>" required>
                                </div>
                                <small class="text-muted fst-italic" id="hintUserId<%=rnd%>" style="font-size: 10px; display: none;"><%=Common.getBahasaConfig("*UserID tidak dapat diubah setelah dibuat.")%></small>
                            </div>

                            <div class="col-md-6">
                                <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Kata Sandi (Password)")%> <span class="text-danger" id="reqPassword<%=rnd%>">*</span></label>
                                <div class="input-group input-group-sm shadow-sm">
                                    <span class="input-group-text bg-light border-end-0"><i class="fas fa-key text-muted"></i></span>
                                    <input type="password" class="form-control fw-bold border-start-0 border-end-0" id="inputPassword<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Rahasiakan...")%>">
                                    <button class="btn btn-outline-secondary bg-white border-start-0" type="button" onclick="togglePassword<%=rnd%>()"><i class="fas fa-eye" id="eyeIcon<%=rnd%>"></i></button>
                                </div>
                                <small class="text-muted fst-italic" id="hintPassword<%=rnd%>" style="font-size: 10px; display: none;"><%=Common.getBahasaConfig("*Kosongkan jika tidak ingin mengubah kata sandi lama.")%></small>
                            </div>

                            <div class="col-md-12 mt-4">
                                <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Nama Lengkap Pedagang/Kasir")%> <span class="text-danger">*</span></label>
                                <div class="input-group input-group-sm shadow-sm">
                                    <span class="input-group-text bg-light border-end-0"><i class="fas fa-id-card-alt text-muted"></i></span>
                                    <input type="text" class="form-control fw-bold border-start-0" id="inputNama<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: Agus Setiawan")%>" required>
                                </div>
                            </div>

                            <div class="col-12">
                                <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Keterangan Tambahan")%></label>
                                <textarea class="form-control text-muted shadow-sm" id="inputKeterangan<%=rnd%>" rows="3" placeholder="<%=Common.getBahasaConfig("Shift pagi, nomor darurat, dll...")%>"></textarea>
                            </div>

                            <div class="col-12 mt-4">
                                <div class="d-flex align-items-center justify-content-between bg-light rounded-3 border p-3">
                                    <div class="pe-3">
                                        <label class="form-label small fw-bold text-secondary mb-0" for="chkSupervisor<%=rnd%>">
                                            <i class="fas fa-user-shield text-warning me-1"></i><%=Common.getBahasaConfig("Supervisor (Boleh Tambah/Ubah/Hapus Data)")%>
                                        </label>
                                        <div class="text-muted" style="font-size: 11px;"><%=Common.getBahasaConfig("Hak tampil menu POS/JSP dan toko yang boleh diakses sekarang diatur dari Grup Pengguna.")%></div>
                                    </div>
                                    <div class="form-check form-switch fs-4 mb-0 flex-shrink-0">
                                        <input class="form-check-input" type="checkbox" id="chkSupervisor<%=rnd%>" style="cursor: pointer;">
                                    </div>
                                </div>
                            </div>

                            <div class="col-12 mt-4 text-end border-top pt-3">
                                <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold border shadow-sm" onclick="tutupForm<%=rnd%>()"><i class="fas fa-times text-danger me-2"></i><%=Common.getBahasaConfig("Batal")%></button>
                                <button type="submit" class="btn btn-success px-4 rounded-pill shadow-sm fw-bold" id="btnSimpan<%=rnd%>">
                                    <i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Akun")%>
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
    const masterModelClass<%=rnd%> = "ais.database.model.inventory.Pedagang";
    
    const isAdmin<%=rnd%> = <%=isAdmin%>;
    const idTokoLogin<%=rnd%> = '<%=idTokoAktif%>';
    const colSpan<%=rnd%> = isAdmin<%=rnd%> ? 7 : 6;

    // Variabel Paging
    let currentPage<%=rnd%> = 1;
    const limitPerPage<%=rnd%> = 10;
    let totalRecords<%=rnd%> = 0;
    
    // Track current editing userid for image upload context
    let activeEditingUserId<%=rnd%> = "";

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

    const showError<%=rnd%> = (msg) => {
        showToast<%=rnd%>(msg, 'bg-danger text-white');
    };

    // Label UI Switch Status Aktif
    const uiStatusAktif<%=rnd%> = document.getElementById('inputStatusAktif<%=rnd%>');
    if (uiStatusAktif<%=rnd%>) {
        uiStatusAktif<%=rnd%>.addEventListener('change', function() {
            const lbl = document.getElementById('labelStatusAktif<%=rnd%>');
            if(this.checked) {
                lbl.innerText = '<%=Common.getBahasaConfigJS("Aktif")%>';
                lbl.className = 'form-check-label fs-6 fw-bold text-success';
            } else {
                lbl.innerText = '<%=Common.getBahasaConfigJS("Tidak Aktif")%>';
                lbl.className = 'form-check-label fs-6 fw-bold text-danger';
            }
        });
    }

    const togglePassword<%=rnd%> = () => {
        const inputPwd = document.getElementById('inputPassword<%=rnd%>');
        const eyeIcon = document.getElementById('eyeIcon<%=rnd%>');
        if (inputPwd.type === "password") {
            inputPwd.type = "text";
            eyeIcon.className = "fas fa-eye-slash";
        } else {
            inputPwd.type = "password";
            eyeIcon.className = "fas fa-eye";
        }
    };

    // ==========================================
    // INIT COMBO BOX (TOKO)
    // ==========================================
    const loadComboToko<%=rnd%> = async () => {
        if (isAdmin<%=rnd%>) {
            const sqlToko = "SELECT id, nama FROM koperasi.toko WHERE aktif = true ORDER BY nama ASC";
            const resToko = await fetchData<%=rnd%>(sqlToko);
            
            let optFilterToko = '<option value=""><%=Common.getBahasaConfig("- Semua Toko -")%></option>';
            let optFormToko = '<option value=""><%=Common.getBahasaConfig("-- Pilih Toko Terlebih Dahulu --")%></option>';
            
            resToko.forEach(item => {
                const opt = '<option value="' + item.id + '">' + item.nama + '</option>';
                optFilterToko += opt;
                optFormToko += opt;
            });
            
            document.getElementById('filterToko<%=rnd%>').innerHTML = optFilterToko;
            const formToko = document.getElementById('inputTokoForm<%=rnd%>');
            if (formToko) formToko.innerHTML = optFormToko;
        }
    };

    // ==========================================
    // READ (TAMPILKAN TABEL)
    // ==========================================
    const buildFilterSQL<%=rnd%> = () => {
        const userid = document.getElementById('filterUserId<%=rnd%>').value.trim().replace(/'/g, "''");
        const nama = document.getElementById('filterNama<%=rnd%>').value.trim().replace(/'/g, "''");
        
        const elToko = document.getElementById('filterToko<%=rnd%>');
        const toko = elToko ? elToko.value : "";

        let filters = " WHERE 1=1 ";
        
        if (userid !== '') {
            filters += " AND p.userid ILIKE '%" + userid + "%' ";
        }
        
        if (nama !== '') {
            filters += " AND p.nama ILIKE '%" + nama + "%' ";
        }

        // Penguncian akses toko
        if (!isAdmin<%=rnd%>) {
            filters += " AND p.toko = " + idTokoLogin<%=rnd%> + " ";
        } else if (toko !== "") {
            filters += " AND p.toko = " + toko + " ";
        }

        return filters;
    };

    const triggerSearchPedagang<%=rnd%> = () => {
        currentPage<%=rnd%> = 1; 
        loadDataPedagang<%=rnd%>();
    };
    
    const resetAndLoadData<%=rnd%> = () => {
        document.getElementById('filterUserId<%=rnd%>').value = '';
        document.getElementById('filterNama<%=rnd%>').value = '';
        if (isAdmin<%=rnd%>) {
            document.getElementById('filterToko<%=rnd%>').value = '';
        }
        currentPage<%=rnd%> = 1; 
        loadDataPedagang<%=rnd%>();
    };

    const loadDataPedagang<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataPedagang<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Mengambil data...")%></span></td></tr>';

        const sqlFilters = buildFilterSQL<%=rnd%>();
        const offset = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%>;

        const sqlCount = "SELECT COUNT(*) AS jumlah FROM koperasi.pedagang p " + sqlFilters + ";";
        
        // Join dengan tabel toko untuk ambil nama toko
        const sqlData = "SELECT p.id, p.userid, p.nama, p.keterangan, p.aktif, p.toko as id_toko, t.nama as nama_toko " +
                        "FROM koperasi.pedagang p " +
                        "LEFT JOIN koperasi.toko t ON p.toko = t.id " + 
                        sqlFilters + " ORDER BY p.userid ASC LIMIT " + limitPerPage<%=rnd%> + " OFFSET " + offset + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetchData<%=rnd%>(sqlCount),
                fetchData<%=rnd%>(sqlData)
            ]);

            totalRecords<%=rnd%> = (resCount && resCount[0]) ? parseInt(resCount[0].jumlah || 0) : 0;
            const recordList = resData || [];

            let htmlList = '';

            if (recordList.length === 0) {
                htmlList = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center text-muted py-5"><i class="fas fa-users-slash fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Tidak ada akun pedagang ditemukan.")%></td></tr>';
            } else {
                let no = offset + 1; 
                recordList.forEach((row) => {

                    const isStatusAktif = (row.aktif === null || row.aktif === true || row.aktif === 't' || row.aktif === 'true');
                    const badgeStatus = isStatusAktif ? '<span class="badge bg-success bg-opacity-10 text-success border border-success px-2 py-1"><i class="fas fa-check-circle me-1"></i>Aktif</span>' : '<span class="badge bg-danger bg-opacity-10 text-danger border border-danger px-2 py-1"><i class="fas fa-times-circle me-1"></i>Non-Aktif</span>';

                    const safeNama = (row.nama || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    const safeKet = (row.keterangan || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    
                    // URL Foto Profil. Menggunakan Data API FotoAdmin dengan reference userid
                    const imgUrl = "<%=Common.ROOT%>/Data?action=file&class=<%=ais.database.model.file.FotoAdmin.class.getName()%>&ref=" + encodeURIComponent(row.userid) + "&jenis=<%=URLEncoder.encode(ais.database.model.file.FotoAdmin.DEFAULT_JENIS, "UTF-8")%>&render=true";

                    htmlList += '<tr>' +
                            '<td class="text-center">' +
                                '<img src="' + imgUrl + '" onerror="this.onerror=null; this.src=\'<%=Common.ROOT%>/img/administrator-icon_default.png\';" class="rounded-circle shadow-sm object-fit-cover border border-secondary" style="width: 45px; height: 45px; background-color: #f8f9fa;" alt="Avatar">' +
                            '</td>' +
                            '<td class="text-start fw-bold text-primary"><i class="fas fa-user-circle me-1 text-muted"></i>' + (row.userid || '-') + '</td>' +
                            '<td class="text-start fw-bold text-dark">' + (row.nama || '-') + '</td>';
                    
                    if (isAdmin<%=rnd%>) {
                        htmlList += '<td><span class="text-muted">' + (row.nama_toko || '-') + '</span></td>';
                    }        
                            
                    htmlList += '<td class="text-start text-muted small">' + (row.keterangan || '-') + '</td>' +
                            '<td class="text-center">' + badgeStatus + '</td>' +
                            '<td class="text-center text-nowrap">' +
                                '<button class="btn btn-sm btn-outline-warning text-dark shadow-sm px-2 me-1 fw-bold" onclick="editPedagang<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Ubah Data & Foto")%>"><i class="fas fa-edit"></i></button>' +
                                '<button class="btn btn-sm btn-outline-danger shadow-sm px-2 fw-bold" onclick="hapusPedagang<%=rnd%>(' + row.id + ', \'' + (row.userid || '') + '\')" title="<%=Common.getBahasaConfig("Hapus Data")%>"><i class="fas fa-trash-alt"></i></button>' +
                            '</td>' +
                        '</tr>';
                });
            }
            tbody.innerHTML = htmlList;
            updatePaginationUI<%=rnd%>();

        } catch (error) {
            console.error("Gagal load tabel pedagang:", error);
            tbody.innerHTML = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal menarik data. Silakan muat ulang.")%></td></tr>';
        }
    };

    const updatePaginationUI<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumberDisplay<%=rnd%>').innerText = currentPage<%=rnd%> + " / " + totalPages;
        
        const startIdx = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%> + 1;
        const endIdx = Math.min(currentPage<%=rnd%> * limitPerPage<%=rnd%>, totalRecords<%=rnd%>);
        const startInfo = totalRecords<%=rnd%> > 0 ? startIdx : 0;
        
        document.getElementById('pagingInfoPedagang<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startInfo + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari")%> <b class="text-dark">' + totalRecords<%=rnd%> + '</b> <%=Common.getBahasaConfig("data")%>';
        
        document.getElementById('btnPrevPage<%=rnd%>').disabled = (currentPage<%=rnd%> === 1);
        document.getElementById('btnNextPage<%=rnd%>').disabled = (currentPage<%=rnd%> === totalPages);
    };

    const changePagePedagang<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if (direction === -1 && currentPage<%=rnd%> > 1) {
            currentPage<%=rnd%>--;
            loadDataPedagang<%=rnd%>();
        } else if (direction === 1 && currentPage<%=rnd%> < totalPages) {
            currentPage<%=rnd%>++;
            loadDataPedagang<%=rnd%>();
        }
    };


    const DAFTAR_AKSES_MENU<%=rnd%> = [];
    const setSemuaAksesMenu<%=rnd%> = (nilai) => {};

    // ==========================================
    // BAGIAN 3: LOGIKA FORM & DINAMIS UI
    // ==========================================
    const bukaFormTambah<%=rnd%> = () => {
        
        document.getElementById('inputIdPedagang<%=rnd%>').value = '';
        document.getElementById('inputOldUserId<%=rnd%>').value = '';
        document.getElementById('formPedagang<%=rnd%>').reset();
        
        if (isAdmin<%=rnd%>) {
            const elToko = document.getElementById('inputTokoForm<%=rnd%>');
            elToko.value = "";
            elToko.disabled = false; // Buka kunci Toko
        }
        
        // Reset defaults untuk Tambah
        const elUserId = document.getElementById('inputUserId<%=rnd%>');
        elUserId.readOnly = false; // Buka kunci UserID
        elUserId.classList.remove("bg-light");

        document.getElementById('hintUserId<%=rnd%>').style.display = 'none';

        document.getElementById('inputPassword<%=rnd%>').required = true;
        document.getElementById('reqPassword<%=rnd%>').style.display = 'inline';
        document.getElementById('hintPassword<%=rnd%>').style.display = 'none';

        document.getElementById('inputStatusAktif<%=rnd%>').checked = true;
        document.getElementById('inputStatusAktif<%=rnd%>').dispatchEvent(new Event('change'));

        // Default: Supervisor NON-aktif. Hak tampil menu diatur dari Grup Pengguna.
        document.getElementById('chkSupervisor<%=rnd%>').checked = false;

        // Sembunyikan area upload foto saat tambah (harus save dulu baru punya userid paten)
        document.getElementById('uploadPhotoArea<%=rnd%>').style.display = 'none';
        activeEditingUserId<%=rnd%> = "";

        document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-user-plus text-primary me-2"></i><%=Common.getBahasaConfig("Pendaftaran Akun Baru")%>';
        
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    };

    const tutupForm<%=rnd%> = () => {
        document.getElementById('viewForm<%=rnd%>').style.display = 'none';
        document.getElementById('viewList<%=rnd%>').style.display = 'block';
        loadDataPedagang<%=rnd%>();
    };


    // ==========================================
    // BAGIAN 4: SIMPAN, EDIT, & HAPUS
    // ==========================================
    const simpanPedagang<%=rnd%> = async () => {
        const idPedagang = document.getElementById('inputIdPedagang<%=rnd%>').value;
        const newUserId = document.getElementById('inputUserId<%=rnd%>').value.trim();
        const oldUserId = document.getElementById('inputOldUserId<%=rnd%>').value.trim();
        const pwd = document.getElementById('inputPassword<%=rnd%>').value;
        
        const tokoId = document.getElementById('inputTokoForm<%=rnd%>').value;
        if(tokoId === "") {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Pilih toko terlebih dahulu!")%>', 'bg-warning text-dark');
            return;
        }

        if(newUserId === "") {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("UserID tidak boleh kosong!")%>', 'bg-warning text-dark');
            return;
        }

        const btnSimpan = document.getElementById('btnSimpan<%=rnd%>');
        const oriBtn = btnSimpan.innerHTML;
        btnSimpan.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Mengecek...")%>';
        btnSimpan.disabled = true;

        try {
            // STEP 1: VALIDASI KETERSEDIAAN USERID PADA TBMUSER
            // Jika ini Tambah Baru, maka wajib dicek. (Karena edit tidak bisa ubah userid, kondisinya aman)
            if (idPedagang === "") {
                const safeUserId = newUserId.replace(/'/g, "''");
                const sqlCekUser = "SELECT userid FROM public.tbmuser WHERE userid ILIKE '" + safeUserId + "'";
                const cek = await fetchData<%=rnd%>(sqlCekUser);
                
                if (cek.length > 0) {
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal: UserID (Login) tersebut sudah terpakai oleh pengguna lain di sistem. Gunakan kombinasi lain!")%>', 'bg-danger text-white');
                    btnSimpan.innerHTML = oriBtn;
                    btnSimpan.disabled = false;
                    document.getElementById('inputUserId<%=rnd%>').focus();
                    return; // Batalkan penyimpanan
                }
            }

            // STEP 2: SIMPAN DATA
            btnSimpan.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyimpan...")%>';

            // Object utama Pedagang.java
            const dataObj = {
                userid: newUserId,
                nama: document.getElementById('inputNama<%=rnd%>').value.trim(),
                keterangan: document.getElementById('inputKeterangan<%=rnd%>').value.trim(),
                aktif: document.getElementById('inputStatusAktif<%=rnd%>').checked,
                toko: tokoId,
                supervisor: document.getElementById('chkSupervisor<%=rnd%>').checked
            };

            // Password hanya dikirim jika diisi
            if (pwd.trim() !== '') {
                dataObj.pass = pwd;
            }

            const payload = { 
                action: "simpanDataRinci", 
                class: masterModelClass<%=rnd%>, 
                data: dataObj
            };

            if (idPedagang !== '') {
                payload.id = idPedagang;
            }

            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const result = await response.json();
            
            if (result.status === '00' || result.status === 'success' || result.id) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Akun pedagang berhasil disimpan!")%>', 'bg-success text-white');
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

    const editPedagang<%=rnd%> = async (id) => {

        const sql = 'SELECT id, userid, nama, keterangan, aktif, toko, supervisor FROM koperasi.pedagang WHERE id = ' + id;
        const res = await fetchData<%=rnd%>(sql);

        if (res.length > 0) {
            const data = res[0];
            
            document.getElementById('inputIdPedagang<%=rnd%>').value = data.id;
            
            // Set UserID dan jadikan Readonly
            const elUserId = document.getElementById('inputUserId<%=rnd%>');
            elUserId.value = data.userid || '';
            elUserId.readOnly = true;
            elUserId.classList.add("bg-light"); // Beri efek abu abu disabled
            document.getElementById('inputOldUserId<%=rnd%>').value = data.userid || '';
            document.getElementById('hintUserId<%=rnd%>').style.display = 'block';

            activeEditingUserId<%=rnd%> = data.userid || ''; // Set ID for image fetch

            document.getElementById('inputNama<%=rnd%>').value = data.nama || '';
            document.getElementById('inputKeterangan<%=rnd%>').value = data.keterangan || '';
            
            // Set Toko dan Kunci Jika Admin
            if (isAdmin<%=rnd%>) {
                const elToko = document.getElementById('inputTokoForm<%=rnd%>');
                elToko.value = data.toko || '';
                // Disable saat edit agar toko tidak terubah ke toko lain
                elToko.disabled = true; 
            }

            // Saat edit, password tidak wajib 
            document.getElementById('inputPassword<%=rnd%>').value = '';
            document.getElementById('inputPassword<%=rnd%>').required = false;
            document.getElementById('reqPassword<%=rnd%>').style.display = 'none';
            document.getElementById('hintPassword<%=rnd%>').style.display = 'block';

            const isAktif = (data.aktif === null || data.aktif === true || data.aktif === 'true' || data.aktif === 't');
            const chkAktif = document.getElementById('inputStatusAktif<%=rnd%>');
            chkAktif.checked = isAktif;
            chkAktif.dispatchEvent(new Event('change'));

            // Supervisor: default NON-aktif (beda dari Hak Akses Menu di bawah) -- hanya tercentang jika eksplisit true.
            const isSupervisor = (data.supervisor === true || data.supervisor === 'true' || data.supervisor === 't');
            document.getElementById('chkSupervisor<%=rnd%>').checked = isSupervisor;

            // Siapkan Area Upload Foto
            document.getElementById('uploadPhotoArea<%=rnd%>').style.display = 'block';
            
            // Render gambar langsung dari API
            const imgUrl = "<%=Common.ROOT%>/Data?action=file&class=<%=ais.database.model.file.FotoAdmin.class.getName()%>&ref=" + encodeURIComponent(activeEditingUserId<%=rnd%>) + "&jenis=<%=URLEncoder.encode(ais.database.model.file.FotoAdmin.DEFAULT_JENIS, "UTF-8")%>&render=true&time=" + new Date().getTime(); 
            $('#previewImage<%=rnd%>').attr('src', imgUrl);

            document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-user-edit text-warning me-2"></i><%=Common.getBahasaConfig("Ubah Data Akun")%>';
            
            document.getElementById('viewList<%=rnd%>').style.display = 'none';
            document.getElementById('viewForm<%=rnd%>').style.display = 'block';
        }
    };

    const hapusPedagang<%=rnd%> = async (id, userid) => {
        if (typeof prosesDeleteData === 'function') {
            const confirmMsg = '<%=Common.getBahasaConfigJS("Hapus akses akun")%> ' + userid + '?';
            prosesDeleteData(masterModelClass<%=rnd%>, id, function(){ 
                 if (document.querySelectorAll('#tabelDataPedagang<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) {
                     currentPage<%=rnd%>--;
                 }
                 loadDataPedagang<%=rnd%>();
            });
        } else {
            const isConfirm = confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus akun")%> ' + userid + '?');
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
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Berhasil dihapus!")%>', 'bg-success text-white');
                    if (document.querySelectorAll('#tabelDataPedagang<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) {
                        currentPage<%=rnd%>--;
                    }
                    loadDataPedagang<%=rnd%>();
                } else {
                    showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Data gagal dihapus.")%>', 'bg-danger text-white');
                }
            } catch (error) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal terhubung ke peladen.")%>', 'bg-danger text-white');
            }
        }
    };

    // ==========================================
    // UPLOAD FOTO PROFIL PEDAGANG (AJAX)
    // ==========================================
    const generatePreview<%=rnd%> = (fileData) => {
        if(fileData && fileData.nama) {
             const imgUrl = "<%=Common.ROOT%>/Data?action=file&class=<%=ais.database.model.file.FotoAdmin.class.getName()%>&ref=" + encodeURIComponent(activeEditingUserId<%=rnd%>) + "&jenis=<%=URLEncoder.encode(ais.database.model.file.FotoAdmin.DEFAULT_JENIS, "UTF-8")%>&render=true&time=" + new Date().getTime(); 
             $('#previewImage<%=rnd%>').attr('src', imgUrl);
        }
    };

    const resetUpload<%=rnd%> = () => {
        $('#fileNameDisplay<%=rnd%>').val("");
        $('#progress-bar-<%=rnd%>').css('width', '0%').text('0%').removeClass('bg-success bg-danger').addClass('bg-primary');
        $('#progress-wrapper-<%=rnd%>').hide();
    };

    function uploadFile<%=rnd%>() {
        
        var fileInput = $('#input_dofile_foto_<%=rnd%>')[0];
        if (fileInput.files.length === 0) return;
        if (!activeEditingUserId<%=rnd%>) {
            showError<%=rnd%>('<%=Common.getBahasaConfigJS("Error: Referensi UserID tidak ditemukan!")%>');
            return;
        }

        var file = fileInput.files[0];
        // Validasi tipe gambar
        if (!file.type.match('image.*')) {
            showError<%=rnd%>('<%=Common.getBahasaConfigJS("File yang dipilih bukan gambar (JPG/PNG).")%>');
            fileInput.value = '';
            return;
        }

        resetUpload<%=rnd%>();
        $('#fileNameDisplay<%=rnd%>').val(file.name); 
        $('#progress-wrapper-<%=rnd%>').fadeIn();
        $('#progress-bar-<%=rnd%>').css('width', '10%').text('10%').removeClass('bg-success bg-danger').addClass('bg-primary progress-bar-animated');

        var formData = new FormData();
        formData.append('nama', file.name);
        formData.append('clazz', '<%=ais.database.model.file.FotoAdmin.class.getName()%>');
        formData.append('jenis', '<%=ais.database.model.file.FotoAdmin.DEFAULT_JENIS%>');
        formData.append('id', activeEditingUserId<%=rnd%>); // Binding file ke string userid TBMUser
        formData.append('tanpaLogin', 'true');
        formData.append('fileContent', file);

        $.ajax({
            url: '<%=Common.ROOT%>/DoUpload',
            type: 'POST',
            data: formData,
            cache: false,
            contentType: false,
            processData: false,
            xhr: function() {
                var xhr = new window.XMLHttpRequest();
                xhr.upload.addEventListener("progress", function(evt) {
                    if (evt.lengthComputable) {
                        var percentComplete = Math.round((evt.loaded / evt.total) * 100);
                        $('#progress-bar-<%=rnd%>').css('width', percentComplete + '%').text(percentComplete + '%');
                    }
                }, false);
                return xhr;
            },
            success: function(response) {
                $('#progress-bar-<%=rnd%>').removeClass('bg-primary progress-bar-animated').addClass('bg-success');
                $('#progress-bar-<%=rnd%>').css('width', '100%').text('<%=Common.getBahasaConfigJS("Selesai")%>');

                setTimeout(function(){ $('#progress-wrapper-<%=rnd%>').fadeOut(); }, 1500);

                // Reload preview setelah sukses upload
                if(response.status === 'Sukses' || response.status === '00' || response.keterangan === 'Sukses'){
                   // Render gambar baru
                   generatePreview<%=rnd%>({nama: file.name});
                   showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Foto profil berhasil diperbarui!")%>', 'bg-success text-white');
                } else {
                   showError<%=rnd%>(response.keterangan || '<%=Common.getBahasaConfigJS("Gagal menyimpan file ke database.")%>');
                }
                fileInput.value = ''; // clean input
            },
            error: function(jqXHR, textStatus, errorThrown) {
                showError<%=rnd%>('<%=Common.getBahasaConfigJS("Koneksi bermasalah: ")%> ' + textStatus);
                $('#progress-bar-<%=rnd%>').removeClass('bg-primary progress-bar-animated').addClass('bg-danger');
                $('#progress-bar-<%=rnd%>').text('<%=Common.getBahasaConfigJS("Gagal")%>');
            }
        });
    }

    // ==========================================
    // INISIALISASI HALAMAN
    // ==========================================
    document.addEventListener("DOMContentLoaded", () => {
        
        loadComboToko<%=rnd%>();

        // Listener Filter Enter Key
        const filterInputs = document.querySelectorAll('.filter-input-<%=rnd%>');
        filterInputs.forEach(input => {
            if (input.tagName === 'SELECT') {
                input.addEventListener("change", () => triggerSearchPedagang<%=rnd%>());
            } else {
                input.addEventListener("keydown", (event) => {
                    if (event.key === "Enter") {
                        event.preventDefault(); 
                        triggerSearchPedagang<%=rnd%>();
                    }
                });
            }
        });
        
        // PENTING: loadDataPedagang harus dipanggil agar data tampil!
        loadDataPedagang<%=rnd%>();
    });
</script>
