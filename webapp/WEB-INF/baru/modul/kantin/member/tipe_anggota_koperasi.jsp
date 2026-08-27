<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
//2. SECURITY CHECK
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	out.print("{\"status\":\"error\", \"message\":\""
	+ Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
	return;
}
Pedagang pedagang = tbmuser.getPedagang();
Toko toko = pedagang == null ? null : pedagang.getToko();
String rnd = Common.getGeneratedBarCode(7);

boolean isAdmin = (toko == null);
%>

<div id="viewList<%=rnd%>" class="animate__animated animate__fadeIn">
    <div class="row mb-4">
        <div class="col-12">
            <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
                <div class="card-header bg-white border-0 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-3 border-bottom border-light">
                    <div>
                        <h5 class="fw-bold text-dark mb-1">
                            <i class="fas fa-layer-group text-primary me-2"></i><%=Common.getBahasaConfig("Manajemen Tipe Anggota")%>
                        </h5>
                        <small class="text-muted"><%=Common.getBahasaConfig("Kelola master data tipe atau grup anggota koperasi.")%></small>
                    </div>
                    
                    <div class="d-flex align-items-center gap-2 flex-wrap">
                        <div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;">
                            <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>
                            <input type="text" class="form-control border-start-0 ps-0 bg-white fw-medium" id="searchTipe<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Kode atau Nama...")%>">
                        </div>
                        
                        <button class="btn btn-sm btn-outline-secondary rounded-pill px-3 shadow-sm fw-bold" onclick="resetAndLoadData<%=rnd%>()" title="<%=Common.getBahasaConfig("Muat Ulang Data")%>">
                            <i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Segarkan")%>
                        </button>
                        
                        <% if (isAdmin) { %>
                        <button class="btn btn-sm btn-primary rounded-pill px-4 shadow-sm fw-bold text-nowrap" onclick="bukaFormTambah<%=rnd%>()">
                            <i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Tambah Tipe")%>
                        </button>
                        <% } %>
                    </div>
                </div>
                
                <div class="card-body p-4 pt-3">
                    <div class="table-responsive border rounded-3 shadow-sm mb-3">
                        <table class="table table-hover table-striped align-middle mb-0">
                            <thead class="table-dark text-center align-middle">
                                <tr>
                                    <th class="fw-semibold text-uppercase small px-3 py-3" style="width: 60px;">#</th>
                                    <th class="text-start fw-semibold text-uppercase small px-3" style="width: 150px;"><i class="fas fa-barcode me-1"></i><%=Common.getBahasaConfig("Kode")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-font me-1"></i><%=Common.getBahasaConfig("Nama Tipe Anggota")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-align-left me-1"></i><%=Common.getBahasaConfig("Keterangan")%></th>
                                    <th class="text-end fw-semibold text-uppercase small px-3" style="width: 160px;"><i class="fas fa-hand-holding-usd me-1"></i><%=Common.getBahasaConfig("Maks. Boleh Utang")%></th>
                                    <th class="fw-semibold text-uppercase small px-3 text-center" style="width: 140px;"><i class="fas fa-address-book me-1"></i><%=Common.getBahasaConfig("Kontak Wajib")%></th>
                                    <th class="fw-semibold text-uppercase small px-3" style="width: 120px;"><i class="fas fa-toggle-on me-1"></i><%=Common.getBahasaConfig("Status")%></th>
                                    <% if (isAdmin) { %>
                                    <th class="fw-semibold text-uppercase small px-3 text-center" style="width: 100px;"><i class="fas fa-cogs"></i></th>
                                    <% } %>
                                </tr>
                            </thead>
                            <tbody id="tabelDataTipe<%=rnd%>">
                                <tr><td colspan="<%= isAdmin ? 8 : 7 %>" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Memuat data tipe anggota...")%></span></td></tr>
                            </tbody>
                        </table>
                    </div>

                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3">
                        <div class="small fw-medium text-muted bg-light px-3 py-1 rounded-pill border" id="pagingInfoTipe<%=rnd%>">
                            </div>
                        <nav>
                            <ul class="pagination pagination-sm mb-0 shadow-sm">
                                <li class="page-item">
                                    <button class="page-link text-primary rounded-start-pill px-3 fw-bold" id="btnPrevPage<%=rnd%>" onclick="changePageTipe<%=rnd%>(-1)">
                                        <i class="fas fa-chevron-left me-1"></i><%=Common.getBahasaConfig("Sebelumnnya")%>
                                    </button>
                                </li>
                                <li class="page-item disabled">
                                    <span class="page-link bg-light text-dark px-3 fw-bold" id="pageNumberDisplay<%=rnd%>">1</span>
                                </li>
                                <li class="page-item">
                                    <button class="page-link text-primary rounded-end-pill px-3 fw-bold" id="btnNextPage<%=rnd%>" onclick="changePageTipe<%=rnd%>(1)">
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
        <div class="col-lg-6">
            <div class="card border-0 shadow-sm rounded-4 border-top border-success border-4">
                <div class="card-header bg-white border-0 pt-4 pb-0 px-4">
                    <div class="d-flex justify-content-between align-items-center">
                        <h5 class="fw-bold text-dark mb-0" id="formTitle<%=rnd%>">
                            <i class="fas fa-layer-group text-success me-2"></i><%=Common.getBahasaConfig("Formulir Tipe Anggota")%>
                        </h5>
                        
                        <div class="form-check form-switch fs-5 mb-0">
                            <input class="form-check-input" type="checkbox" id="inputStatusAktif<%=rnd%>" checked style="cursor: pointer;">
                            <label class="form-check-label fs-6 fw-bold text-success" for="inputStatusAktif<%=rnd%>" id="labelStatusAktif<%=rnd%>"><%=Common.getBahasaConfig("Aktif")%></label>
                        </div>
                    </div>
                </div>
                <div class="card-body p-4">
                    <form id="formTipeAnggota<%=rnd%>" onsubmit="event.preventDefault(); simpanTipeAnggota<%=rnd%>();">
                        <input type="hidden" id="inputIdTipe<%=rnd%>" value="">
                        
                        <div class="row g-3 mt-1">
                            
                            <div class="col-md-12">
                                <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Kode Tipe")%></label>
                                <div class="input-group input-group-sm shadow-sm">
                                    <span class="input-group-text bg-light border-end-0"><i class="fas fa-barcode text-muted"></i></span>
                                    <input type="text" class="form-control fw-bold border-start-0" id="inputKode<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: VIP, REG, PRE")%>">
                                </div>
                            </div>
                            
                            <div class="col-md-12">
                                <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Nama Tipe Anggota")%> <span class="text-danger">*</span></label>
                                <div class="input-group input-group-sm shadow-sm">
                                    <span class="input-group-text bg-light border-end-0"><i class="fas fa-font text-muted"></i></span>
                                    <input type="text" class="form-control fw-bold border-start-0" id="inputNama<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: Anggota VIP")%>" required>
                                </div>
                            </div>

                            <div class="col-12">
                                <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Keterangan")%></label>
                                <textarea class="form-control text-muted shadow-sm" id="inputKeterangan<%=rnd%>" rows="4" placeholder="<%=Common.getBahasaConfig("Masukkan penjelasan tambahan jika ada...")%>"></textarea>
                            </div>

                            <div class="col-md-12">
                                <label class="form-label small fw-semibold text-secondary"><i class="fas fa-hand-holding-usd text-danger me-1"></i><%=Common.getBahasaConfig("Maksimal Boleh Utang")%></label>
                                <div class="input-group input-group-sm shadow-sm">
                                    <span class="input-group-text bg-light border-end-0">Rp</span>
                                    <input type="number" min="0" step="1" class="form-control fw-bold border-start-0" id="inputMaksimalBolehUtang<%=rnd%>" placeholder="0" value="0">
                                </div>
                                <small class="text-muted"><%=Common.getBahasaConfig("Batas maksimal hutang (piutang toko) yang boleh ditumpuk anggota tipe ini. 0 = tidak boleh berhutang sama sekali.")%></small>
                            </div>

                            <div class="col-12">
                                <label class="form-label small fw-bold text-primary"><i class="fas fa-chart-line me-1"></i><%=Common.getBahasaConfig("Batas Nilai Pembelian")%></label>
                                <div class="row g-2">
                                    <div class="col-md-4"><div class="input-group input-group-sm"><span class="input-group-text">Harian Rp</span><input type="number" min="0" class="form-control" id="inputMaksHarian<%=rnd%>" value="0"></div></div>
                                    <div class="col-md-4"><div class="input-group input-group-sm"><span class="input-group-text">Mingguan Rp</span><input type="number" min="0" class="form-control" id="inputMaksMingguan<%=rnd%>" value="0"></div></div>
                                    <div class="col-md-4"><div class="input-group input-group-sm"><span class="input-group-text">Bulanan Rp</span><input type="number" min="0" class="form-control" id="inputMaksBulanan<%=rnd%>" value="0"></div></div>
                                </div>
                                <small class="text-muted"><%=Common.getBahasaConfig("Nilai 0 berarti tanpa batas. Pemeriksaan akhir tetap dilakukan server sebelum transaksi disimpan.")%></small>
                            </div>

                            <div class="col-12">
                                <label class="form-label small fw-bold text-primary"><i class="fas fa-credit-card me-1"></i><%=Common.getBahasaConfig("Cara Bayar yang Diizinkan")%></label>
                                <div class="row g-2" id="containerCaraBayarTipe<%=rnd%>"><div class="col-12 text-muted small"><i class="fas fa-spinner fa-spin me-2"></i>Memuat...</div></div>
                                <div class="row g-2 mt-2">
                                    <div class="col-md-7"><select class="form-select form-select-sm" id="inputCaraBayarDefault<%=rnd%>"><option value="">-- Tidak ada default --</option></select></div>
                                    <div class="col-md-5"><div class="form-check form-switch pt-1"><input class="form-check-input" type="checkbox" id="inputKunciCaraBayar<%=rnd%>"><label class="form-check-label small fw-bold" for="inputKunciCaraBayar<%=rnd%>"><%=Common.getBahasaConfig("Tidak boleh memakai cara bayar lain")%></label></div></div>
                                </div>
                            </div>

                            <div class="col-12">
                                <label class="form-label small fw-bold text-primary"><i class="fas fa-shield-alt me-1"></i><%=Common.getBahasaConfig("Verifikasi sebelum memotong saldo")%></label>
                                <div class="form-check form-switch"><input class="form-check-input" type="checkbox" id="inputWajibPin<%=rnd%>"><label class="form-check-label small" for="inputWajibPin<%=rnd%>">Wajib PIN numerik</label></div>
                                <div class="form-check form-switch"><input class="form-check-input" type="checkbox" id="inputWajibWajah<%=rnd%>"><label class="form-check-label small" for="inputWajibWajah<%=rnd%>">Wajib biometric wajah (kamera + liveness)</label></div>
                                <div class="form-check form-switch"><input class="form-check-input" type="checkbox" id="inputWajibFingerprint<%=rnd%>"><label class="form-check-label small" for="inputWajibFingerprint<%=rnd%>">Wajib fingerprint (scanner USB/OTG + SDK)</label></div>
                            </div>

                            <div class="col-md-12">
                                <label class="form-label small fw-semibold text-secondary"><i class="fas fa-address-book text-primary me-1"></i><%=Common.getBahasaConfig("Kontak Wajib Anggota")%></label>
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox" id="inputWajibHp<%=rnd%>">
                                    <label class="form-check-label small fw-medium" for="inputWajibHp<%=rnd%>"><%=Common.getBahasaConfig("Wajib Memasukkan No. HP")%></label>
                                </div>
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox" id="inputWajibEmail<%=rnd%>">
                                    <label class="form-check-label small fw-medium" for="inputWajibEmail<%=rnd%>"><%=Common.getBahasaConfig("Wajib Memasukkan Email")%></label>
                                </div>
                                <small class="text-muted"><%=Common.getBahasaConfig("Anggota tipe ini tidak dapat disimpan tanpa kontak yang ditandai wajib.")%></small>
                            </div>

                            <div class="col-12 mt-4 text-end border-top pt-3">
                                <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold border shadow-sm" onclick="tutupForm<%=rnd%>()">
                                    <i class="fas fa-times text-danger me-2"></i><%=Common.getBahasaConfig("Batal")%>
                                </button>
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
    const masterModelClass<%=rnd%> = "ais.database.model.koperasi.TipeAnggotaKoperasi";
    let isEditMode<%=rnd%> = false; 
    
    // Inject status Admin dari Java ke JavaScript
    const isAdmin<%=rnd%> = <%=isAdmin%>;
    const colSpan<%=rnd%> = isAdmin<%=rnd%> ? 8 : 7;

    // Variabel Paging
    let currentPage<%=rnd%> = 1;
    const limitPerPage<%=rnd%> = 10;
    let totalRecords<%=rnd%> = 0;
    let listCaraBayarTipe<%=rnd%> = [];

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

    // Cermin TipeAnggotaKoperasi.defaultWajibHp: saat kolom wajib_hp NULL,
    // wajib HP dianggap true bila nama tipe mengandung pegawai/dosen/guru/umum
    // (selain itu false, mis. Mahasiswa/Siswa). wajib_email NULL -> false utk semua.
    const defaultWajibHp<%=rnd%> = (nama) => {
        const n = (nama || '').toLowerCase();
        return n.includes('pegawai') || n.includes('dosen') || n.includes('guru') || n.includes('umum');
    };
    const bacaBool<%=rnd%> = (val, defVal) => {
        if (val === null || val === undefined || val === '') return defVal;
        return (val === true || val === 't' || val === 'true');
    };

    const loadMasterCaraBayarTipe<%=rnd%> = async () => {
        listCaraBayarTipe<%=rnd%> = await fetchData<%=rnd%>("SELECT id, nama FROM koperasi.cara_pembayaran_koperasi WHERE COALESCE(aktif,true)=true ORDER BY nama");
        const box = document.getElementById('containerCaraBayarTipe<%=rnd%>');
        const select = document.getElementById('inputCaraBayarDefault<%=rnd%>');
        box.innerHTML = listCaraBayarTipe<%=rnd%>.map(x => '<div class="col-md-6"><div class="form-check border rounded p-2 ps-4"><input class="form-check-input chk-cara-tipe-<%=rnd%>" type="checkbox" value="'+x.id+'" id="cbTipeBayar<%=rnd%>_'+x.id+'"><label class="form-check-label small fw-bold" for="cbTipeBayar<%=rnd%>_'+x.id+'">'+x.nama+'</label></div></div>').join('') || '<div class="col-12 text-muted small">Tidak ada cara bayar aktif.</div>';
        select.innerHTML = '<option value="">-- Tidak ada default --</option>' + listCaraBayarTipe<%=rnd%>.map(x => '<option value="'+x.id+'">'+x.nama+'</option>').join('');
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
        const keyword = document.getElementById('searchTipe<%=rnd%>').value.trim().replace(/'/g, "''");
        let filters = " WHERE 1=1 ";
        if (keyword !== '') {
            filters += " AND (nama ILIKE '%" + keyword + "%' OR kode ILIKE '%" + keyword + "%') ";
        }
        return filters;
    };

    const resetAndLoadData<%=rnd%> = () => {
        currentPage<%=rnd%> = 1; 
        loadDataTipeAnggota<%=rnd%>();
    };

    const loadDataTipeAnggota<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataTipe<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Mengambil data...")%></span></td></tr>';

        const sqlFilters = buildFilterSQL<%=rnd%>();
        const offset = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%>;

        // Query Total Baris untuk Paginasi (Tabel: tipe_anggota_koperasi)
        const sqlCount = "SELECT COUNT(*) AS jumlah FROM koperasi.tipe_anggota_koperasi " + sqlFilters + ";";
        
        // Query Data Spesifik per Halaman
        const sqlData = "SELECT id, kode, nama, keterangan, aktif, maksimal_boleh_utang, wajib_hp, wajib_email FROM koperasi.tipe_anggota_koperasi " + sqlFilters + " ORDER BY id DESC LIMIT " + limitPerPage<%=rnd%> + " OFFSET " + offset + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetchData<%=rnd%>(sqlCount),
                fetchData<%=rnd%>(sqlData)
            ]);

            totalRecords<%=rnd%> = (resCount && resCount[0]) ? parseInt(resCount[0].jumlah || 0) : 0;
            const recordList = resData || [];

            let htmlList = '';

            if (recordList.length === 0) {
                htmlList = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center text-muted py-5"><i class="fas fa-layer-group fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Tidak ada data tipe anggota ditemukan.")%></td></tr>';
            } else {
                let no = offset + 1; 
                recordList.forEach((row) => {

                    const isStatusAktif = (row.aktif === null || row.aktif === true || row.aktif === 't' || row.aktif === 'true');
                    const badgeStatus = isStatusAktif ? '<span class="badge bg-success bg-opacity-10 text-success border border-success px-2 py-1"><i class="fas fa-check-circle me-1"></i>Aktif</span>' : '<span class="badge bg-danger bg-opacity-10 text-danger border border-danger px-2 py-1"><i class="fas fa-times-circle me-1"></i>Non-Aktif</span>';

                    const maksUtang = parseFloat(row.maksimal_boleh_utang || 0);
                    const maksUtangText = maksUtang > 0
                        ? '<span class="fw-bold text-danger">Rp ' + maksUtang.toLocaleString('id-ID') + '</span>'
                        : '<span class="text-muted small"><%=Common.getBahasaConfigJS("Tidak boleh")%></span>';

                    // Ringkasan kontak wajib (NULL -> default per nama tipe)
                    const isWajibHp = bacaBool<%=rnd%>(row.wajib_hp, defaultWajibHp<%=rnd%>(row.nama));
                    const isWajibEmail = bacaBool<%=rnd%>(row.wajib_email, false);
                    let kontakWajibHtml = '<span class="text-muted">-</span>';
                    if (isWajibHp && isWajibEmail) {
                        kontakWajibHtml = '<span class="badge bg-primary bg-opacity-10 text-primary border border-primary px-2 py-1"><%=Common.getBahasaConfigJS("No. HP + Email")%></span>';
                    } else if (isWajibHp) {
                        kontakWajibHtml = '<span class="badge bg-primary bg-opacity-10 text-primary border border-primary px-2 py-1"><%=Common.getBahasaConfigJS("No. HP")%></span>';
                    } else if (isWajibEmail) {
                        kontakWajibHtml = '<span class="badge bg-primary bg-opacity-10 text-primary border border-primary px-2 py-1"><%=Common.getBahasaConfigJS("Email")%></span>';
                    }

                    htmlList += '<tr>' +
                            '<td class="text-center text-muted fw-medium">' + (no++) + '</td>' +
                            '<td class="text-start fw-bold text-secondary">' + (row.kode || '-') + '</td>' +
                            '<td class="text-start fw-bold text-dark">' + (row.nama || '-') + '</td>' +
                            '<td class="text-start text-muted small">' + (row.keterangan || '-') + '</td>' +
                            '<td class="text-end">' + maksUtangText + '</td>' +
                            '<td class="text-center">' + kontakWajibHtml + '</td>' +
                            '<td class="text-center">' + badgeStatus + '</td>';
                    
                    // Render kolom aksi hanya jika user adalah admin
                    if (isAdmin<%=rnd%>) {
                        htmlList += '<td class="text-center text-nowrap">' +
                                        '<button class="btn btn-sm btn-outline-warning text-dark shadow-sm px-3 fw-bold" onclick="editTipeAnggota<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Edit Data")%>"><i class="fas fa-edit me-1"></i>Ubah</button>' +
                                    '</td>';
                    }
                            
                    htmlList += '</tr>';
                });
            }
            tbody.innerHTML = htmlList;
            updatePaginationUI<%=rnd%>();

        } catch (error) {
            console.error("Gagal load tabel tipe anggota:", error);
            tbody.innerHTML = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal menarik data. Silakan muat ulang.")%></td></tr>';
        }
    };

    const updatePaginationUI<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumberDisplay<%=rnd%>').innerText = currentPage<%=rnd%> + " / " + totalPages;
        
        const startIdx = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%> + 1;
        const endIdx = Math.min(currentPage<%=rnd%> * limitPerPage<%=rnd%>, totalRecords<%=rnd%>);
        const startInfo = totalRecords<%=rnd%> > 0 ? startIdx : 0;
        
        document.getElementById('pagingInfoTipe<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startInfo + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari")%> <b class="text-dark">' + totalRecords<%=rnd%> + '</b> <%=Common.getBahasaConfig("data")%>';
        
        document.getElementById('btnPrevPage<%=rnd%>').disabled = (currentPage<%=rnd%> === 1);
        document.getElementById('btnNextPage<%=rnd%>').disabled = (currentPage<%=rnd%> === totalPages);
    };

    const changePageTipe<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if (direction === -1 && currentPage<%=rnd%> > 1) {
            currentPage<%=rnd%>--;
            loadDataTipeAnggota<%=rnd%>();
        } else if (direction === 1 && currentPage<%=rnd%> < totalPages) {
            currentPage<%=rnd%>++;
            loadDataTipeAnggota<%=rnd%>();
        }
    };


    // ==========================================
    // BAGIAN 3: LOGIKA FORM & DINAMIS UI
    // ==========================================
    const bukaFormTambah<%=rnd%> = () => {
        if (!isAdmin<%=rnd%>) return; // Penjagaan ekstra
        
        isEditMode<%=rnd%> = false; 
        
        document.getElementById('inputIdTipe<%=rnd%>').value = '';
        document.getElementById('formTipeAnggota<%=rnd%>').reset();
        document.getElementById('inputMaksimalBolehUtang<%=rnd%>').value = 0;
        document.getElementById('inputWajibHp<%=rnd%>').checked = false;
        document.getElementById('inputWajibEmail<%=rnd%>').checked = false;
        document.getElementById('inputMaksHarian<%=rnd%>').value = 0;
        document.getElementById('inputMaksMingguan<%=rnd%>').value = 0;
        document.getElementById('inputMaksBulanan<%=rnd%>').value = 0;
        document.getElementById('inputCaraBayarDefault<%=rnd%>').value = '';
        document.getElementById('inputKunciCaraBayar<%=rnd%>').checked = false;
        document.getElementById('inputWajibPin<%=rnd%>').checked = false;
        document.getElementById('inputWajibWajah<%=rnd%>').checked = false;
        document.getElementById('inputWajibFingerprint<%=rnd%>').checked = false;
        document.querySelectorAll('.chk-cara-tipe-<%=rnd%>').forEach(x => x.checked = false);

        const chkStatus = document.getElementById('inputStatusAktif<%=rnd%>');
        chkStatus.checked = true;
        chkStatus.dispatchEvent(new Event('change'));
        
        document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-layer-group text-primary me-2"></i><%=Common.getBahasaConfig("Tambah Tipe Baru")%>';
        
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    };

    const tutupForm<%=rnd%> = () => {
        document.getElementById('viewForm<%=rnd%>').style.display = 'none';
        document.getElementById('viewList<%=rnd%>').style.display = 'block';
        loadDataTipeAnggota<%=rnd%>();
    };


    // ==========================================
    // BAGIAN 4: SIMPAN & EDIT (API SIMPANDATARINCI)
    // ==========================================
    const simpanTipeAnggota<%=rnd%> = async () => {
        if (!isAdmin<%=rnd%>) return; // Penjagaan ekstra
        
        const idTipe = document.getElementById('inputIdTipe<%=rnd%>').value;
        const btnSimpan = document.getElementById('btnSimpan<%=rnd%>');
        
        // Object utama TipeAnggotaKoperasi
        const caraDipilih = Array.from(document.querySelectorAll('.chk-cara-tipe-<%=rnd%>:checked')).map(x => x.value);
        const dataObj = {
            kode: document.getElementById('inputKode<%=rnd%>').value,
            nama: document.getElementById('inputNama<%=rnd%>').value,
            keterangan: document.getElementById('inputKeterangan<%=rnd%>').value,
            aktif: document.getElementById('inputStatusAktif<%=rnd%>').checked,
            maksimalBolehUtang: parseFloat(document.getElementById('inputMaksimalBolehUtang<%=rnd%>').value || 0),
            wajibHp: document.getElementById('inputWajibHp<%=rnd%>').checked,
            wajibEmail: document.getElementById('inputWajibEmail<%=rnd%>').checked,
            daftarCaraPembayaranYangBolehDiPilih: caraDipilih.length ? ',' + caraDipilih.join(',') + ',' : '',
            caraPembayaranDefaultId: document.getElementById('inputCaraBayarDefault<%=rnd%>').value || null,
            tidakBolehCaraPembayaranLain: document.getElementById('inputKunciCaraBayar<%=rnd%>').checked,
            maksimalTransaksiHarian: parseFloat(document.getElementById('inputMaksHarian<%=rnd%>').value || 0),
            maksimalTransaksiMingguan: parseFloat(document.getElementById('inputMaksMingguan<%=rnd%>').value || 0),
            maksimalTransaksiBulanan: parseFloat(document.getElementById('inputMaksBulanan<%=rnd%>').value || 0),
            wajibPin: document.getElementById('inputWajibPin<%=rnd%>').checked,
            wajibBiometricWajah: document.getElementById('inputWajibWajah<%=rnd%>').checked,
            wajibBiometricFingerprint: document.getElementById('inputWajibFingerprint<%=rnd%>').checked
        };
        
        const payload = { 
            action: "simpanDataRinci", 
            class: masterModelClass<%=rnd%>, 
            data: dataObj
        };

        if (idTipe !== '') {
            payload.id = idTipe;
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
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Tipe anggota berhasil disimpan!")%>', 'bg-success text-white');
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

    const editTipeAnggota<%=rnd%> = async (id) => {
        if (!isAdmin<%=rnd%>) return; // Penjagaan ekstra
        
        isEditMode<%=rnd%> = true; 
        
        const sql = 'SELECT id, kode, nama, keterangan, aktif, maksimal_boleh_utang, wajib_hp, wajib_email, daftar_cara_pembayaran_yang_boleh_di_pilih, cara_pembayaran_default_id, COALESCE(tidak_boleh_cara_pembayaran_lain,false) tidak_boleh_cara_pembayaran_lain, COALESCE(maksimal_transaksi_harian,0) maksimal_transaksi_harian, COALESCE(maksimal_transaksi_mingguan,0) maksimal_transaksi_mingguan, COALESCE(maksimal_transaksi_bulanan,0) maksimal_transaksi_bulanan, COALESCE(wajib_pin,false) wajib_pin, COALESCE(wajib_verifikasi_biometric_wajah,false) wajib_wajah, COALESCE(wajib_verifikasi_biometric_fingerprint,false) wajib_fingerprint FROM koperasi.tipe_anggota_koperasi WHERE id = ' + id;
        const res = await fetchData<%=rnd%>(sql);

        if (res.length > 0) {
            const data = res[0];

            // Set Form Data
            document.getElementById('inputIdTipe<%=rnd%>').value = data.id;
            document.getElementById('inputKode<%=rnd%>').value = data.kode || '';
            document.getElementById('inputNama<%=rnd%>').value = data.nama || '';
            document.getElementById('inputKeterangan<%=rnd%>').value = data.keterangan || '';
            document.getElementById('inputMaksimalBolehUtang<%=rnd%>').value = parseFloat(data.maksimal_boleh_utang || 0);

            // Kontak wajib: NULL -> default per nama tipe (lihat defaultWajibHp di atas)
            document.getElementById('inputWajibHp<%=rnd%>').checked = bacaBool<%=rnd%>(data.wajib_hp, defaultWajibHp<%=rnd%>(data.nama));
            document.getElementById('inputWajibEmail<%=rnd%>').checked = bacaBool<%=rnd%>(data.wajib_email, false);
            document.getElementById('inputMaksHarian<%=rnd%>').value = parseFloat(data.maksimal_transaksi_harian || 0);
            document.getElementById('inputMaksMingguan<%=rnd%>').value = parseFloat(data.maksimal_transaksi_mingguan || 0);
            document.getElementById('inputMaksBulanan<%=rnd%>').value = parseFloat(data.maksimal_transaksi_bulanan || 0);
            document.getElementById('inputCaraBayarDefault<%=rnd%>').value = data.cara_pembayaran_default_id || '';
            document.getElementById('inputKunciCaraBayar<%=rnd%>').checked = bacaBool<%=rnd%>(data.tidak_boleh_cara_pembayaran_lain, false);
            document.getElementById('inputWajibPin<%=rnd%>').checked = bacaBool<%=rnd%>(data.wajib_pin, false);
            document.getElementById('inputWajibWajah<%=rnd%>').checked = bacaBool<%=rnd%>(data.wajib_wajah, false);
            document.getElementById('inputWajibFingerprint<%=rnd%>').checked = bacaBool<%=rnd%>(data.wajib_fingerprint, false);
            const csvCara = ',' + (data.daftar_cara_pembayaran_yang_boleh_di_pilih || '').replace(/^,|,$/g, '') + ',';
            document.querySelectorAll('.chk-cara-tipe-<%=rnd%>').forEach(x => x.checked = csvCara.includes(',' + x.value + ','));

            const isAktif = (data.aktif === null || data.aktif === true || data.aktif === 'true' || data.aktif === 't');
            const chkAktif = document.getElementById('inputStatusAktif<%=rnd%>');
            chkAktif.checked = isAktif;
            chkAktif.dispatchEvent(new Event('change')); 
            
            document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-edit text-warning me-2"></i><%=Common.getBahasaConfig("Ubah Tipe Anggota")%>';
            
            document.getElementById('viewList<%=rnd%>').style.display = 'none';
            document.getElementById('viewForm<%=rnd%>').style.display = 'block';
        }
    };

    // INISIALISASI
    document.addEventListener("DOMContentLoaded", () => {
        document.getElementById('searchTipe<%=rnd%>').addEventListener("keydown", (e) => {
            if (e.key === "Enter") {
                e.preventDefault();
                resetAndLoadData<%=rnd%>(); 
            }
        });
        loadMasterCaraBayarTipe<%=rnd%>().then(() => loadDataTipeAnggota<%=rnd%>());
    });
</script>
