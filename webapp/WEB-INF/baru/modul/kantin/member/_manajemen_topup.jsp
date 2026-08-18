<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Tbmrole"%>
<%@page import="ais.common.Common"%>
<%
// 1. Pengecekan Sesi & Keamanan Akses
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    out.print("<div class='text-center p-5'><i class='fas fa-exclamation-triangle fa-3x text-warning mb-3'></i><br><h5 class='text-muted'>" + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "</h5></div>");
    return;
}

Tbmrole tbmrole = tbmuser.hakAkses();
boolean bolehEntryTopup = tbmrole != null && tbmrole.getBolehEntryTopup() != null && tbmrole.getBolehEntryTopup();

String rnd = Common.getGeneratedBarCode(7);
%>

<style>
    body { background-color: #f4f7f6; }
    .readonly-overlay-<%=rnd%> {
        pointer-events: none;
        opacity: 0.6;
    }
</style>

<div class="container-fluid py-3 py-md-4 animate__animated animate__fadeIn" id="topupManagerContent<%=rnd%>">

    <!-- HEADER -->
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-center mb-4 gap-3">
        <div class="text-center text-md-start">
            <h4 class="fw-bolder text-primary mb-1">
                <i class="fas fa-wallet me-2"></i><%=Common.getBahasaConfig("Manajemen Saldo (Deposit)")%>
            </h4>
            <p class="text-muted small mb-0"><%=Common.getBahasaConfig("Kelola riwayat pengisian saldo (topup) anggota koperasi.")%></p>
        </div>
        
        <div class="d-flex flex-wrap gap-2 justify-content-center">
            <button class="btn btn-success rounded-pill px-3 shadow-sm fw-bold" onclick="downloadExcelTopup<%=rnd%>()" title="<%=Common.getBahasaConfig("Unduh file Excel")%>">
                <i class="fas fa-file-excel me-1"></i><%=Common.getBahasaConfig("Unduh Excel")%>
            </button>
            <% if (bolehEntryTopup) { %>
                <button class="btn btn-warning rounded-pill px-3 shadow-sm fw-bold" onclick="showUploadModalTopup<%=rnd%>()" title="<%=Common.getBahasaConfig("Unggah file Excel")%>">
                    <i class="fas fa-file-upload me-1"></i><%=Common.getBahasaConfig("Unggah Excel")%>
                </button>
                <button class="btn btn-primary rounded-pill px-4 shadow-sm fw-bold" onclick="bukaFormTopup<%=rnd%>()">
                    <i class="fas fa-plus-circle me-1"></i><%=Common.getBahasaConfig("Tambah Topup")%>
                </button>
            <% } else { %>
                <span class="badge bg-secondary py-2 px-3 fw-medium"><i class="fas fa-eye me-1"></i><%=Common.getBahasaConfig("Mode Hanya Baca (Read-Only)")%></span>
            <% } %>
        </div>
    </div>

    <!-- VIEW: LIST DATA -->
    <div id="viewListTopup<%=rnd%>" class="d-block">

        <!-- FILTER SECTION -->
        <div class="card border-0 shadow-sm rounded-4 mb-4 border-top border-primary border-3">
            <div class="card-body p-3 p-md-4">
                <div class="row g-3 align-items-end">
                    <div class="col-md-4">
                        <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Pencarian Nama / Keterangan")%></label>
                        <div class="input-group shadow-sm">
                            <span class="input-group-text bg-light border-end-0 text-muted"><i class="fas fa-search"></i></span>
                            <input type="text" class="form-control border-start-0 fw-medium filter-topup-<%=rnd%>" id="searchKeywordTopup<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik nama atau keterangan...")%>">
                        </div>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Tanggal Mulai")%></label>
                        <input type="date" class="form-control fw-medium shadow-sm filter-topup-<%=rnd%>" id="searchDateStart<%=rnd%>">
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Tanggal Akhir")%></label>
                        <input type="date" class="form-control fw-medium shadow-sm filter-topup-<%=rnd%>" id="searchDateEnd<%=rnd%>">
                    </div>
                    <div class="col-md-2 d-grid">
                        <button class="btn btn-primary rounded-3 shadow-sm fw-bold" onclick="resetAndLoadTopup<%=rnd%>()">
                            <i class="fas fa-filter me-2"></i><%=Common.getBahasaConfig("Saring")%>
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <!-- DATA TABLE SECTION -->
        <div class="card border-0 shadow-sm rounded-4 overflow-hidden">
            <div class="card-body p-0">
                <div class="table-responsive px-3 py-2" style="min-height: 400px;">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-light text-secondary">
                            <tr>
                                <th class="fw-bold text-uppercase small py-3 text-center" style="width: 50px;">#</th>
                                <th class="fw-bold text-uppercase small"><%=Common.getBahasaConfig("Waktu")%></th>
                                <th class="fw-bold text-uppercase small"><%=Common.getBahasaConfig("Nama Referensi (Deposit)")%></th>
                                <th class="fw-bold text-uppercase small text-end"><%=Common.getBahasaConfig("Nominal Topup")%></th>
                                <th class="fw-bold text-uppercase small"><%=Common.getBahasaConfig("Kadaluarsa")%></th>
                                <th class="fw-bold text-uppercase small"><%=Common.getBahasaConfig("Keterangan")%></th>
                                <% if (bolehEntryTopup) { %>
                                    <th class="text-center fw-bold text-uppercase small" style="width: 130px;"><%=Common.getBahasaConfig("Aksi")%></th>
                                <% } %>
                            </tr>
                        </thead>
                        <tbody id="tabelDataTopup<%=rnd%>">
                            <tr>
                                <td colspan="<%=bolehEntryTopup ? 7 : 6%>" class="text-center py-5">
                                    <div class="spinner-border text-primary mb-3"></div>
                                    <br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Sistem sedang memuat data topup...")%></span>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <!-- PAGING -->
                <div class="bg-light p-3 border-top d-flex flex-column flex-md-row justify-content-between align-items-center gap-3">
                    <div class="small fw-bold text-muted" id="pagingInfoTopup<%=rnd%>"></div>
                    <div class="d-flex align-items-center bg-white rounded-pill shadow-sm border p-1" id="pagingContainerTopup<%=rnd%>">
                        <button class="btn btn-sm btn-light rounded-pill px-3 fw-bold text-primary" id="btnPrevPageTopup<%=rnd%>" onclick="changePageTopup<%=rnd%>(-1)">
                            <i class="fas fa-chevron-left me-1"></i><span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Sebelumnya")%></span>
                        </button>
                        <div class="px-3 fw-bold text-dark small">
                            <span id="lblCurrentPageTopup<%=rnd%>">1</span> / <span id="lblTotalPageTopup<%=rnd%>">1</span>
                        </div>
                        <button class="btn btn-sm btn-light rounded-pill px-3 fw-bold text-primary" id="btnNextPageTopup<%=rnd%>" onclick="changePageTopup<%=rnd%>(1)">
                            <span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Selanjutnya")%></span><i class="fas fa-chevron-right ms-1"></i>
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- VIEW: FORM DATA -->
    <div id="viewFormTopup<%=rnd%>" class="d-none">
        <div class="row justify-content-center">
            <div class="col-lg-8">
                <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
                    <div class="card-header bg-white border-0 pt-4 pb-0 px-4">
                        <h5 class="fw-bold text-dark mb-0" id="formTitleTopup<%=rnd%>">
                            <i class="fas fa-plus-circle text-primary me-2"></i><%=Common.getBahasaConfig("Formulir Pengisian Saldo (Topup)")%>
                        </h5>
                    </div>
                    <div class="card-body p-4">
                        <form id="formTopup<%=rnd%>" onsubmit="event.preventDefault(); simpanDataTopup<%=rnd%>();">
                            <input type="hidden" id="inputIdTopup<%=rnd%>" value="">
                            
                            <div class="row g-4 mt-1">
                                
                                <div class="col-md-6">
                                    <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Waktu Transaksi")%> <span class="text-danger">*</span></label>
                                    <input type="datetime-local" class="form-control fw-medium" id="inputWaktuTopup<%=rnd%>" required>
                                </div>
                                
                                <div class="col-md-6">
                                    <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nominal Pengisian")%> <span class="text-danger">*</span></label>
                                    <div class="input-group">
                                        <span class="input-group-text bg-light text-muted fw-bold border-end-0">Rp</span>
                                        <input type="number" class="form-control fw-bolder text-end border-start-0" id="inputNominalTopup<%=rnd%>" placeholder="0" required min="1" step="any">
                                    </div>
                                </div>

                                <div class="col-md-6">
                                    <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal Expired / Kadaluarsa (Opsional)")%></label>
                                    <input type="date" class="form-control fw-medium" id="inputTanggalExpiredTopup<%=rnd%>">
                                    <small class="text-muted d-block mt-1" style="font-size:11px;"><i class="fas fa-info-circle me-1"></i><%=Common.getBahasaConfig("Kosongkan jika saldo tidak pernah hangus. Jika diisi, saldo ini otomatis tidak bisa dipakai lagi setelah tanggal tersebut.")%></small>
                                </div>

                                <div class="col-12 p-3 bg-light border rounded-3">
                                    <label class="form-label small fw-bold text-primary mb-1"><i class="fas fa-user me-1"></i><%=Common.getBahasaConfig("Cari Anggota Koperasi")%> <span class="text-danger">*</span></label>
                                    <div class="input-group shadow-sm">
                                        <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-primary"></i></span>
                                        <input type="text" class="form-control border-start-0 fw-bold" list="listAnggota<%=rnd%>" id="inputCariAnggota<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik kode / nama anggota...")%>" autocomplete="off" required>
                                    </div>
                                    <datalist id="listAnggota<%=rnd%>"></datalist>
                                    <input type="hidden" id="idAnggotaTerpilih<%=rnd%>" value="" required>
                                    <small class="text-muted mt-1 d-block" style="font-size: 11px;"><i class="fas fa-info-circle me-1"></i><%=Common.getBahasaConfig("Hanya menampilkan anggota yang diizinkan topup oleh admin (Tipe Anggota).")%></small>
                                </div>

                                <div class="col-md-6">
                                    <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jenis Pembayaran")%></label>
                                    <select class="form-select fw-medium shadow-sm" id="inputJenisPembayaran<%=rnd%>">
                                        <option value=""><%=Common.getBahasaConfig("Memuat...")%></option>
                                    </select>
                                </div>

                                <div class="col-md-6">
                                    <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jenis Tabungan")%></label>
                                    <select class="form-select fw-medium shadow-sm" id="inputJenisTabungan<%=rnd%>">
                                        <option value=""><%=Common.getBahasaConfig("Memuat...")%></option>
                                    </select>
                                </div>

                                <div class="col-12">
                                    <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Keterangan (Opsional)")%></label>
                                    <textarea class="form-control" id="inputKeteranganTopup<%=rnd%>" rows="3" placeholder="<%=Common.getBahasaConfig("Informasi tambahan mengenai topup ini...")%>"></textarea>
                                </div>

                                <div class="col-12 mt-5 text-end border-top pt-4">
                                    <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-bold border shadow-sm" onclick="tutupFormTopup<%=rnd%>()">
                                        <i class="fas fa-times text-danger me-2"></i><%=Common.getBahasaConfig("Batal")%>
                                    </button>
                                    <button type="submit" class="btn btn-primary px-5 rounded-pill shadow-sm fw-bold" id="btnSimpanTopup<%=rnd%>">
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

</div>

<div class="modal fade" id="modalUploadExcelTopup<%=rnd%>" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 rounded-4 shadow-lg">
            <div class="modal-header bg-light border-bottom-0 pb-3">
                <h5 class="modal-title fw-bold text-dark">
                    <i class="fas fa-file-upload text-warning me-2"></i><%=Common.getBahasaConfig("Unggah Data Topup")%>
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4 pt-2 text-center">
                <p class="text-muted small mb-4"><%=Common.getBahasaConfig("Pastikan format file Excel yang Anda unggah sama persis dengan format file yang Anda unduh dari sistem ini (kolom ID_ANGGOTA wajib diisi kode/id anggota yang valid). Ekstensi yang didukung: .xls, .xlsx")%></p>
                <input class="form-control mb-3" type="file" id="fileUploadExcelTopup<%=rnd%>" accept=".xls,.xlsx">
                <div id="uploadStatusBoxTopup<%=rnd%>" class="alert alert-info py-2" style="display: none;"></div>
                <div class="mt-4">
                    <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
                    <button type="button" class="btn btn-warning text-dark px-4 rounded-pill fw-bold shadow-sm" id="btnProsesUploadTopup<%=rnd%>" onclick="prosesUploadExcelTopup<%=rnd%>()">
                        <i class="fas fa-cogs me-2"></i><%=Common.getBahasaConfig("Proses Data")%>
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    // ==========================================
    // VARIABEL GLOBAL & KONFIGURASI
    // ==========================================
    const masterClassTopup<%=rnd%> = "ais.database.model.Deposit";
    let modalInstanceUploadTopup<%=rnd%> = null;
    const canEdit<%=rnd%> = <%=bolehEntryTopup%>;
    let currentPageTopup<%=rnd%> = 1;
    const limitPerPageTopup<%=rnd%> = 10;
    let totalRecordsTopup<%=rnd%> = 0;
    let timerPencarianAnggota<%=rnd%> = null;

    // Helper: Format Rupiah
    const formatRp<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);
    };

    // Helper: Format Date for datetime-local input
    const formatDateTimeLocal<%=rnd%> = (dateString) => {
        if (!dateString) return '';
        const d = new Date(dateString);
        return new Date(d.getTime() - (d.getTimezoneOffset() * 60000)).toISOString().slice(0, 16);
    };

    // Helper: Toast Notifikasi
    const showToastUI<%=rnd%> = (msg, colorClass) => {
        if(typeof tampilkanToast === "function") {
            tampilkanToast(msg, colorClass);
        } else {
            alert(msg);
        }
    };

    // Helper: Fetch API
    const fetchApiTopup<%=rnd%> = async (payload) => {
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            return await res.json();
        } catch (e) { 
            console.error(e); 
            return { status: "error", message: "<%=Common.getBahasaConfig("Terjadi kesalahan koneksi jaringan.")%>" }; 
        }
    };

    const setDefaultDatesTopup<%=rnd%> = () => {
        const today = new Date();
        const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
        
        const todayStr = today.getFullYear() + '-' + String(today.getMonth() + 1).padStart(2, '0') + '-' + String(today.getDate()).padStart(2, '0');
        const firstDayStr = firstDay.getFullYear() + '-' + String(firstDay.getMonth() + 1).padStart(2, '0') + '-01';
        
        document.getElementById('searchDateEnd<%=rnd%>').value = todayStr;
        document.getElementById('searchDateStart<%=rnd%>').value = firstDayStr;
    };

    // ==========================================
    // INISIALISASI COMBOBOX & LIVE SEARCH
    // ==========================================
    const loadComboboxData<%=rnd%> = async () => {
        if(!canEdit<%=rnd%>) return; // Jangan load jika readonly

        // Jenis Pembayaran
        const sqlPembayaran = "SELECT id, nama FROM public.jenis_pembayaran WHERE aktif = true ORDER BY nama ASC;";
        const resPembayaran = await fetchApiTopup<%=rnd%>({ action: "sql", sql: sqlPembayaran });
        let htmlPem = '<option value=""><%=Common.getBahasaConfig("-- Pilih Jenis Pembayaran --")%></option>';
        if (resPembayaran.data) {
            resPembayaran.data.forEach(p => { htmlPem += '<option value="' + p.id + '">' + p.nama + '</option>'; });
        }
        document.getElementById('inputJenisPembayaran<%=rnd%>').innerHTML = htmlPem;

        // Jenis Tabungan
        const sqlTabungan = "SELECT id, nama FROM public.jenis_tabungan WHERE aktif = true ORDER BY nama ASC;";
        const resTabungan = await fetchApiTopup<%=rnd%>({ action: "sql", sql: sqlTabungan });
        let htmlTab = '<option value=""><%=Common.getBahasaConfig("-- Pilih Jenis Tabungan --")%></option>';
        if (resTabungan.data) {
            resTabungan.data.forEach(p => { htmlTab += '<option value="' + p.id + '">' + p.nama + '</option>'; });
        }
        document.getElementById('inputJenisTabungan<%=rnd%>').innerHTML = htmlTab;
    };

    // Listener Live Search Anggota
    const inputCari = document.getElementById('inputCariAnggota<%=rnd%>');
    if (inputCari) {
        inputCari.addEventListener('input', function() {
            const keyword = this.value.trim();
            const isMatch = Array.from(document.getElementById('listAnggota<%=rnd%>').options).some(opt => opt.value === keyword);
            
            if (!isMatch && keyword.length >= 1) {
                clearTimeout(timerPencarianAnggota<%=rnd%>);
                timerPencarianAnggota<%=rnd%> = setTimeout(() => {
                    tarikDataAnggotaKoperasi<%=rnd%>(keyword);
                }, 600); 
            }
        });

        // Set Hidden ID saat Opsi Dipilih
        inputCari.addEventListener('change', function() {
            const val = this.value;
            const opts = document.getElementById('listAnggota<%=rnd%>').childNodes;
            let matchId = '';
            
            for (let i = 0; i < opts.length; i++) {
                if (opts[i].value === val) {
                    matchId = opts[i].getAttribute('data-id');
                    break;
                }
            }
            document.getElementById('idAnggotaTerpilih<%=rnd%>').value = matchId;
        });
    }

    const tarikDataAnggotaKoperasi<%=rnd%> = async (keyword) => {
        const safeKeyword = keyword.replace(/'/g, "''");
        // Filter: aktif=true dan jenisAnggotaKoperasi.bolehEntryTopupOlehAdmin = true
        const sql = "SELECT a.id, a.kode, a.nama " +
                    "FROM koperasi.anggota_koperasi a " +
                    "INNER JOIN koperasi.jenis_anggota_koperasi j ON a.jenis_anggota_koperasi = j.id " +
                    "WHERE a.aktif = true AND j.boleh_entry_topup_oleh_admin = true " +
                    "AND (a.nama ILIKE '%" + safeKeyword + "%' OR a.kode ILIKE '%" + safeKeyword + "%') " +
                    "ORDER BY a.nama ASC LIMIT 50";

        const res = await fetchApiTopup<%=rnd%>({ action: "sql", sql: sql });
        let htmlDatalist = '';
        if(res.data) {
            res.data.forEach(item => {
                const label = item.kode + ' - ' + item.nama;
                htmlDatalist += '<option value="' + label + '" data-id="' + item.id + '">';
            });
        }
        document.getElementById('listAnggota<%=rnd%>').innerHTML = htmlDatalist;
    };

    // ==========================================
    // NAVIGASI TAMPILAN
    // ==========================================
    const switchViewTopup<%=rnd%> = (viewId) => {
        document.getElementById('viewListTopup<%=rnd%>').classList.add('d-none');
        document.getElementById('viewListTopup<%=rnd%>').classList.remove('d-block');
        document.getElementById('viewFormTopup<%=rnd%>').classList.add('d-none');
        document.getElementById('viewFormTopup<%=rnd%>').classList.remove('d-block');
        
        document.getElementById(viewId).classList.remove('d-none');
        document.getElementById(viewId).classList.add('d-block');
    };

    // ==========================================
    // READ (TAMPILKAN TABEL & FILTER)
    // ==========================================
    const getFilterQueryTopup<%=rnd%> = () => {
        const keyword = document.getElementById('searchKeywordTopup<%=rnd%>').value.trim().replace(/'/g, "''");
        const tglAwal = document.getElementById('searchDateStart<%=rnd%>').value;
        const tglAkhir = document.getElementById('searchDateEnd<%=rnd%>').value;

        let filters = " WHERE 1=1 ";
        
        if (keyword !== '') {
            filters += " AND (d.nama ILIKE '%" + keyword + "%' OR d.keterangan ILIKE '%" + keyword + "%' OR a.nama ILIKE '%" + keyword + "%') ";
        }
        if (tglAwal !== "" && tglAkhir !== "") {
            filters += " AND d.waktu BETWEEN '" + tglAwal + " 00:00:00' AND '" + tglAkhir + " 23:59:59' ";
        }
        return filters;
    };

    const resetAndLoadTopup<%=rnd%> = () => {
        currentPageTopup<%=rnd%> = 1;
        loadDataTopup<%=rnd%>();
    };

    const changePageTopup<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecordsTopup<%=rnd%> / limitPerPageTopup<%=rnd%>);
        if (direction === -1 && currentPageTopup<%=rnd%> > 1) {
            currentPageTopup<%=rnd%>--;
            loadDataTopup<%=rnd%>();
        } else if (direction === 1 && currentPageTopup<%=rnd%> < totalPages) {
            currentPageTopup<%=rnd%>++;
            loadDataTopup<%=rnd%>();
        }
    };

    const loadDataTopup<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataTopup<%=rnd%>');
        const colSpan = canEdit<%=rnd%> ? 7 : 6;
        tbody.innerHTML = '<tr><td colspan="' + colSpan + '" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Sistem sedang memuat data topup...")%></span></td></tr>';

        const sqlFilters = getFilterQueryTopup<%=rnd%>();
        const offset = (currentPageTopup<%=rnd%> - 1) * limitPerPageTopup<%=rnd%>;

        const sqlCount = "SELECT COUNT(d.id) AS jumlah FROM public.deposit d LEFT JOIN koperasi.anggota_koperasi a ON d.anggota_koperasi = a.id " + sqlFilters + ";";
        
        // Ambil ID Anggota Koperasi juga
        const sqlData = "SELECT d.id, TO_CHAR(d.waktu, 'DD Mon YYYY HH24:MI') as waktu_str, d.waktu, (case when a.nama is null then d.nama else a.kode||' '||a.nama end) as nama, d.keterangan, d.nominal, " +
                        "d.jenis_pembayaran, d.jenis_tabungan, d.anggota_koperasi, a.nama as nama_anggota, a.kode as kode_anggota, " +
                        "d.tanggal_expired, TO_CHAR(d.tanggal_expired, 'DD Mon YYYY') as tanggal_expired_str " +
                        "FROM public.deposit d " +
                        "LEFT JOIN koperasi.anggota_koperasi a ON d.anggota_koperasi = a.id " +
                        sqlFilters + " ORDER BY d.waktu DESC, d.id DESC LIMIT " + limitPerPageTopup<%=rnd%> + " OFFSET " + offset + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetchApiTopup<%=rnd%>({ action: "sql", sql: sqlCount }),
                fetchApiTopup<%=rnd%>({ action: "sql", sql: sqlData })
            ]);

            totalRecordsTopup<%=rnd%> = (resCount.data && resCount.data[0]) ? parseInt(resCount.data[0].jumlah || 0) : 0;
            const recordList = resData.data || [];

            let htmlList = '';

            if (recordList.length === 0) {
                htmlList = '<tr><td colspan="' + colSpan + '" class="text-center text-muted py-5"><i class="fas fa-folder-open fa-3x mb-3 opacity-25"></i><br><%=Common.getBahasaConfig("Tidak ada data topup yang ditemukan.")%></td></tr>';
                tbody.innerHTML = htmlList;
                updatePagingUITopup<%=rnd%>();
                return;
            } 

            let no = offset + 1; 
            
            // Render HTML
            recordList.forEach((row) => {
                const safeNama = (row.nama || '-').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                const safeKet = (row.keterangan || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                const idAnggota = row.anggota_koperasi || '';
                const namaAnggotaStr = row.nama_anggota ? (row.kode_anggota + ' - ' + row.nama_anggota) : '';
                const isoWaktu = formatDateTimeLocal<%=rnd%>(row.waktu);
                const tglExpiredInput = row.tanggal_expired ? new Date(row.tanggal_expired).toISOString().slice(0, 10) : '';

                let selExpired = '';
                if (row.tanggal_expired) {
                    const sudahHangus = new Date(row.tanggal_expired) < new Date();
                    selExpired = '<span class="' + (sudahHangus ? 'text-danger fw-bold' : 'text-muted') + '">' +
                        row.tanggal_expired_str + (sudahHangus ? ' <span class="badge bg-danger bg-opacity-25 text-danger border border-danger">Hangus</span>' : '') +
                        '</span>';
                } else {
                    selExpired = '<span class="text-muted">-</span>';
                }

                let actBtn = '';
                if (canEdit<%=rnd%>) {
                    actBtn = '<td class="text-center text-nowrap">' +
                                '<button class="btn btn-sm btn-outline-warning text-dark shadow-sm px-2 me-1 mb-1 fw-bold" onclick="editTopup<%=rnd%>(' + row.id + ', \'' + isoWaktu + '\', \'' + row.nominal + '\', \'' + safeKet + '\', \'' + idAnggota + '\', \'' + namaAnggotaStr.replace(/'/g, "\\'") + '\', \'' + (row.jenis_pembayaran||'') + '\', \'' + (row.jenis_tabungan||'') + '\', \'' + tglExpiredInput + '\')" title="<%=Common.getBahasaConfig("Ubah Data")%>"><i class="fas fa-edit"></i></button>' +
                                '<button class="btn btn-sm btn-outline-danger shadow-sm px-2 mb-1 fw-bold" onclick="hapusTopup<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Hapus Data")%>"><i class="fas fa-trash-alt"></i></button>' +
                            '</td>';
                }

                htmlList += '<tr class="border-bottom" id="row-topup-<%=rnd%>-' + row.id + '">' +
                        '<td class="text-center text-muted fw-medium">' + (no++) + '</td>' +
                        '<td class="text-secondary small fw-semibold"><i class="far fa-clock me-1"></i>' + (row.waktu_str || '-') + '</td>' +
                        '<td class="fw-bold text-dark">' + safeNama + '</td>' +
                        '<td class="text-end fw-bolder text-success fs-6">' + formatRp<%=rnd%>(row.nominal) + '</td>' +
                        '<td class="small">' + selExpired + '</td>' +
                        '<td class="text-muted small">' + safeKet + '</td>' +
                        actBtn +
                    '</tr>';
            });
            
            tbody.innerHTML = htmlList;
            updatePagingUITopup<%=rnd%>();

        } catch (error) {
            console.error("Error loading table:", error);
            tbody.innerHTML = '<tr><td colspan="' + colSpan + '" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3"></i><br><%=Common.getBahasaConfig("Gagal menarik data dari peladen.")%></td></tr>';
        }
    };

    const updatePagingUITopup<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecordsTopup<%=rnd%> / limitPerPageTopup<%=rnd%>) || 1;
        document.getElementById('lblCurrentPageTopup<%=rnd%>').innerText = currentPageTopup<%=rnd%>;
        document.getElementById('lblTotalPageTopup<%=rnd%>').innerText = totalPages;
        
        const startIdx = totalRecordsTopup<%=rnd%> > 0 ? ((currentPageTopup<%=rnd%> - 1) * limitPerPageTopup<%=rnd%> + 1) : 0;
        const endIdx = Math.min(currentPageTopup<%=rnd%> * limitPerPageTopup<%=rnd%>, totalRecordsTopup<%=rnd%>);
        
        document.getElementById('pagingInfoTopup<%=rnd%>').innerHTML = '<%=Common.getBahasaConfigJS("Menampilkan")%> <span class="text-dark">' + startIdx + ' - ' + endIdx + '</span> <%=Common.getBahasaConfig("dari")%> <span class="text-dark">' + totalRecordsTopup<%=rnd%> + '</span> <%=Common.getBahasaConfig("topup")%>';
        
        document.getElementById('btnPrevPageTopup<%=rnd%>').disabled = (currentPageTopup<%=rnd%> === 1);
        document.getElementById('btnNextPageTopup<%=rnd%>').disabled = (currentPageTopup<%=rnd%> === totalPages);
    };

    // ==========================================
    // CREATE / UPDATE / DELETE
    // ==========================================
    const bukaFormTopup<%=rnd%> = () => {
        if(!canEdit<%=rnd%>) return;

        document.getElementById('formTopup<%=rnd%>').reset();
        document.getElementById('inputIdTopup<%=rnd%>').value = '';
        document.getElementById('idAnggotaTerpilih<%=rnd%>').value = '';
        document.getElementById('inputTanggalExpiredTopup<%=rnd%>').value = '';

        // Set Default Date to Now
        const now = new Date();
        const offset = now.getTimezoneOffset() * 60000;
        const localISOTime = (new Date(now - offset)).toISOString().slice(0,16);
        document.getElementById('inputWaktuTopup<%=rnd%>').value = localISOTime;

        document.getElementById('formTitleTopup<%=rnd%>').innerHTML = '<i class="fas fa-plus-circle text-primary me-2"></i><%=Common.getBahasaConfig("Formulir Pengisian Saldo (Topup)")%>';
        switchViewTopup<%=rnd%>('viewFormTopup<%=rnd%>');
    };

    const tutupFormTopup<%=rnd%> = () => {
        switchViewTopup<%=rnd%>('viewListTopup<%=rnd%>');
    };

    const simpanDataTopup<%=rnd%> = async () => {
        if(!canEdit<%=rnd%>) return;

        const btnSimpan = document.getElementById('btnSimpanTopup<%=rnd%>');
        const oriBtn = btnSimpan.innerHTML;
        btnSimpan.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyimpan...")%>';
        btnSimpan.disabled = true;

        const idTopup = document.getElementById('inputIdTopup<%=rnd%>').value;
        const idAnggota = document.getElementById('idAnggotaTerpilih<%=rnd%>').value;

        if (idAnggota === "") {
            showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Silakan pilih Anggota Koperasi yang valid dari daftar pencarian.")%>', 'bg-warning text-dark');
            btnSimpan.innerHTML = oriBtn;
            btnSimpan.disabled = false;
            document.getElementById('inputCariAnggota<%=rnd%>').focus();
            return;
        }

        // Trik format Tanggal utk dikirim ke backend (YYYY-MM-DD HH:mm:ss)
        const rawWaktu = document.getElementById('inputWaktuTopup<%=rnd%>').value;
        let formattedWaktu = "";
        if(rawWaktu) {
            formattedWaktu = rawWaktu.replace('T', ' ') + ":00";
        }

        // Field "nama" (referensi) otomatis diset default di backend jika null, tp kita beri nilai standard
        const valNominal = parseFloat(document.getElementById('inputNominalTopup<%=rnd%>').value || 0);

        const rawTglExpired = document.getElementById('inputTanggalExpiredTopup<%=rnd%>').value;

        const dataObj = {
            waktu: formattedWaktu,
            nominal: valNominal,
            tanggalExpired: rawTglExpired ? (rawTglExpired + " 23:59:59") : null,
            keterangan: document.getElementById('inputKeteranganTopup<%=rnd%>').value.trim(),
            anggotaKoperasi: idAnggota
        };

        const idJPem = document.getElementById('inputJenisPembayaran<%=rnd%>').value;
        if(idJPem !== "") dataObj.jenisPembayaran = idJPem;

        const idJTab = document.getElementById('inputJenisTabungan<%=rnd%>').value;
        if(idJTab !== "") dataObj.jenisTabungan = idJTab;

        const payload = { action: "simpanDataRinci", class: masterClassTopup<%=rnd%>, data: dataObj };
        if (idTopup !== '') payload.id = idTopup;

        try {
            const res = await fetchApiTopup<%=rnd%>(payload);
            if (res.status === '00' || res.status === 'success' || res.id) {
                showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Data deposit berhasil disimpan.")%>', 'bg-success text-white');
                tutupFormTopup<%=rnd%>();
                loadDataTopup<%=rnd%>();
            } else {
                showToastUI<%=rnd%>(res.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan data.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi.")%>', 'bg-danger text-white');
        } finally {
            btnSimpan.innerHTML = oriBtn;
            btnSimpan.disabled = false;
        }
    };

    const editTopup<%=rnd%> = (id, waktu, nominal, ket, idAnggota, namaAnggotaStr, jnsPem, jnsTab, tglExpired) => {
        if(!canEdit<%=rnd%>) return;

        document.getElementById('inputIdTopup<%=rnd%>').value = id;
        document.getElementById('inputWaktuTopup<%=rnd%>').value = waktu;
        document.getElementById('inputNominalTopup<%=rnd%>').value = nominal;
        document.getElementById('inputTanggalExpiredTopup<%=rnd%>').value = tglExpired || '';
        document.getElementById('inputKeteranganTopup<%=rnd%>').value = ket;

        document.getElementById('idAnggotaTerpilih<%=rnd%>').value = idAnggota;
        document.getElementById('inputCariAnggota<%=rnd%>').value = namaAnggotaStr;

        document.getElementById('inputJenisPembayaran<%=rnd%>').value = jnsPem;
        document.getElementById('inputJenisTabungan<%=rnd%>').value = jnsTab;

        document.getElementById('formTitleTopup<%=rnd%>').innerHTML = '<i class="fas fa-edit text-warning me-2"></i><%=Common.getBahasaConfig("Ubah Data Pengisian Saldo")%>';
        switchViewTopup<%=rnd%>('viewFormTopup<%=rnd%>');
    };

    const hapusTopup<%=rnd%> = async (id) => {
        if(!canEdit<%=rnd%>) return;
        
        prosesDeleteData(masterClassTopup<%=rnd%>, id, function(){ 
        	showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Data topup berhasil dihapus.")%>', 'bg-success text-white');
            if (document.querySelectorAll('#tabelDataTopup<%=rnd%> tr').length === 1 && currentPageTopup<%=rnd%> > 1) currentPageTopup<%=rnd%>--;
            loadDataTopup<%=rnd%>();
       });

    };

    // ==========================================
    // DOWNLOAD & UPLOAD EXCEL
    // ==========================================
    const downloadExcelTopup<%=rnd%> = async () => {
        showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Sedang menyiapkan file Excel, mohon tunggu...")%>', 'bg-info text-dark');

        const sqlFilters = getFilterQueryTopup<%=rnd%>();
        const sqlQuery = "SELECT d.id, a.kode as kode_anggota, (case when a.nama is null then d.nama else a.nama end) as nama, " +
                        "TO_CHAR(d.waktu, 'YYYY-MM-DD HH24:MI:SS') as waktu, d.nominal, " +
                        "TO_CHAR(d.tanggal_expired, 'YYYY-MM-DD') as tanggal_expired, d.keterangan " +
                        "FROM public.deposit d LEFT JOIN koperasi.anggota_koperasi a ON d.anggota_koperasi = a.id " +
                        sqlFilters + " ORDER BY d.waktu DESC;";

        try {
            const res = await fetchApiTopup<%=rnd%>({ action: "sql", sql: sqlQuery });
            const rows = res.data || [];
            if (rows.length === 0) {
                showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Tidak ada data untuk diunduh.")%>', 'bg-warning text-dark');
                return;
            }

            let tableHtml = '<html xmlns:x="urn:schemas-microsoft-com:office:excel"><head><meta charset="UTF-8"><style>';
            tableHtml += 'th { background-color: #f8f9fa; font-weight: bold; border: 1px solid black; } td { border: 1px solid black; mso-number-format:"\\@"; }';
            tableHtml += '</style></head><body><table><thead><tr>';
            tableHtml += '<th>ID_SISTEM</th><th>KODE_ANGGOTA</th><th>NAMA</th><th>WAKTU</th><th>NOMINAL</th><th>TANGGAL_EXPIRED</th><th>KETERANGAN</th>';
            tableHtml += '</tr></thead><tbody>';
            rows.forEach(r => {
                tableHtml += '<tr>' +
                    '<td>' + (r.id || '') + '</td>' +
                    '<td>' + (r.kode_anggota || '') + '</td>' +
                    '<td>' + (r.nama || '') + '</td>' +
                    '<td>' + (r.waktu || '') + '</td>' +
                    '<td>' + (r.nominal || 0) + '</td>' +
                    '<td>' + (r.tanggal_expired || '') + '</td>' +
                    '<td>' + (r.keterangan || '') + '</td>' +
                    '</tr>';
            });
            tableHtml += '</tbody></table></body></html>';

            const blob = new Blob([tableHtml], { type: 'application/vnd.ms-excel' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement("a");
            const timestamp = new Date().toISOString().slice(0,10).replace(/-/g,"");
            link.download = "Format_Topup_Deposit_" + timestamp + ".xls";
            link.href = url;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            setTimeout(() => URL.revokeObjectURL(url), 100);
        } catch (error) {
            console.error(error);
            showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal mengunduh data.")%>', 'bg-danger text-white');
        }
    };

    const showUploadModalTopup<%=rnd%> = () => {
        document.getElementById('fileUploadExcelTopup<%=rnd%>').value = '';
        const statusBox = document.getElementById('uploadStatusBoxTopup<%=rnd%>');
        statusBox.style.display = 'none';
        statusBox.innerHTML = '';
        if (modalInstanceUploadTopup<%=rnd%> === null) {
            modalInstanceUploadTopup<%=rnd%> = new bootstrap.Modal(document.getElementById('modalUploadExcelTopup<%=rnd%>'));
        }
        modalInstanceUploadTopup<%=rnd%>.show();
    };

    // Kolom KODE_ANGGOTA (bukan ID) dicari dulu ke koperasi.anggota_koperasi supaya file Excel
    // tetap bisa dipakai orang-ke-orang tanpa perlu tahu id numerik internal -- konsisten dgn
    // datalist pencarian anggota yg sudah ada di form Tambah/Ubah pada layar ini.
    const cariIdAnggotaByKode<%=rnd%> = async (kode) => {
        if (!kode) return null;
        const safeKode = String(kode).trim().replace(/'/g, "''");
        const sql = "SELECT id FROM koperasi.anggota_koperasi WHERE kode = '" + safeKode + "' LIMIT 1";
        const res = await fetchApiTopup<%=rnd%>({ action: "sql", sql: sql });
        return (res.data && res.data[0]) ? res.data[0].id : null;
    };

    const prosesUploadExcelTopup<%=rnd%> = () => {
        const fileInput = document.getElementById('fileUploadExcelTopup<%=rnd%>');
        const file = fileInput.files[0];
        if (!file) {
            showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Pilih file Excel terlebih dahulu!")%>', 'bg-warning text-dark');
            return;
        }

        const btnProcess = document.getElementById('btnProsesUploadTopup<%=rnd%>');
        const statusBox = document.getElementById('uploadStatusBoxTopup<%=rnd%>');
        btnProcess.disabled = true;
        btnProcess.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Membaca File...")%>';
        statusBox.style.display = 'block';
        statusBox.className = 'alert alert-info py-2 small';
        statusBox.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Mengekstrak data baris demi baris...';

        const reader = new FileReader();
        reader.onload = async (e) => {
            try {
                const data = new Uint8Array(e.target.result);
                const workbook = XLSX.read(data, {type: 'array'});
                const firstSheet = workbook.Sheets[workbook.SheetNames[0]];
                const excelRows = XLSX.utils.sheet_to_json(firstSheet, {defval: ""});

                if (excelRows.length === 0) {
                    throw new Error("File Excel Kosong atau format tidak sesuai.");
                }

                let berhasil = 0;
                let gagal = 0;
                const pesanGagal = [];

                for (let i = 0; i < excelRows.length; i++) {
                    const row = excelRows[i];
                    statusBox.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Memproses baris ' + (i + 1) + ' dari ' + excelRows.length + '...';

                    const idSistem = row['ID_SISTEM'] ? String(row['ID_SISTEM']).trim() : '';
                    const kodeAnggota = row['KODE_ANGGOTA'] ? String(row['KODE_ANGGOTA']).trim() : '';
                    const nominal = parseFloat(row['NOMINAL']) || 0;
                    const waktu = row['WAKTU'] ? String(row['WAKTU']).trim() : '';
                    const tglExpiredRaw = row['TANGGAL_EXPIRED'] ? String(row['TANGGAL_EXPIRED']).trim() : '';

                    let idAnggota = null;
                    if (kodeAnggota) {
                        idAnggota = await cariIdAnggotaByKode<%=rnd%>(kodeAnggota);
                    }
                    if (!idAnggota && !idSistem) {
                        gagal++;
                        pesanGagal.push('Baris ' + (i + 2) + ': kode anggota "' + kodeAnggota + '" tidak ditemukan.');
                        continue;
                    }
                    if (nominal === 0 && !idSistem) {
                        gagal++;
                        pesanGagal.push('Baris ' + (i + 2) + ': nominal kosong/nol.');
                        continue;
                    }

                    const dataObj = {
                        nominal: nominal,
                        waktu: waktu ? waktu : undefined,
                        tanggalExpired: tglExpiredRaw ? (tglExpiredRaw + " 23:59:59") : null,
                        keterangan: row['KETERANGAN'] || ''
                    };
                    if (idAnggota) dataObj.anggotaKoperasi = idAnggota;

                    const payload = { action: "simpanDataRinci", class: masterClassTopup<%=rnd%>, data: dataObj };
                    if (idSistem) payload.id = idSistem;

                    try {
                        const result = await fetchApiTopup<%=rnd%>(payload);
                        if (result.status === '00' || result.status === 'success' || result.id) {
                            berhasil++;
                        } else {
                            gagal++;
                            pesanGagal.push('Baris ' + (i + 2) + ': ' + (result.description || 'gagal disimpan.'));
                        }
                    } catch (errBaris) {
                        gagal++;
                        pesanGagal.push('Baris ' + (i + 2) + ': kesalahan koneksi.');
                    }
                }

                if (gagal === 0) {
                    statusBox.className = 'alert alert-success py-2 small fw-bold';
                    statusBox.innerHTML = '<i class="fas fa-check-circle me-2"></i>' + berhasil + ' baris berhasil disimpan!';
                } else {
                    statusBox.className = 'alert alert-warning py-2 small';
                    statusBox.innerHTML = '<i class="fas fa-exclamation-triangle me-2"></i>' + berhasil + ' berhasil, ' + gagal + ' gagal.<br>' +
                        pesanGagal.slice(0, 5).join('<br>') + (pesanGagal.length > 5 ? '<br>... dan ' + (pesanGagal.length - 5) + ' lainnya.' : '');
                }

                showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Upload selesai.")%>', gagal === 0 ? 'bg-success text-white' : 'bg-warning text-dark');
                loadDataTopup<%=rnd%>();
                if (gagal === 0) {
                    setTimeout(() => { modalInstanceUploadTopup<%=rnd%>.hide(); }, 1500);
                }
            } catch (error) {
                console.error("Upload Error:", error);
                statusBox.className = 'alert alert-danger py-2 small';
                statusBox.innerHTML = '<i class="fas fa-exclamation-triangle me-2"></i>Terjadi Kesalahan: ' + error.message;
            } finally {
                btnProcess.disabled = false;
                btnProcess.innerHTML = '<i class="fas fa-cogs me-2"></i><%=Common.getBahasaConfig("Proses Data")%>';
            }
        };
        reader.readAsArrayBuffer(file);
    };

    // ==========================================
    // INISIALISASI HALAMAN
    // ==========================================
    document.addEventListener("DOMContentLoaded", () => {
        setDefaultDatesTopup<%=rnd%>();
        
        const filterInputs = document.querySelectorAll('.filter-topup-<%=rnd%>');
        filterInputs.forEach(input => {
            input.addEventListener("keydown", (event) => {
                if (event.key === "Enter") {
                    event.preventDefault(); 
                    resetAndLoadTopup<%=rnd%>();
                }
            });
        });

        loadComboboxData<%=rnd%>();
        loadDataTopup<%=rnd%>();
    });

</script>