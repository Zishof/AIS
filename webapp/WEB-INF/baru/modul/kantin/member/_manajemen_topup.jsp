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
            <% if (bolehEntryTopup) { %>
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
                                <th class="fw-bold text-uppercase small"><%=Common.getBahasaConfig("Keterangan")%></th>
                                <% if (bolehEntryTopup) { %>
                                    <th class="text-center fw-bold text-uppercase small" style="width: 130px;"><%=Common.getBahasaConfig("Aksi")%></th>
                                <% } %>
                            </tr>
                        </thead>
                        <tbody id="tabelDataTopup<%=rnd%>">
                            <tr>
                                <td colspan="<%=bolehEntryTopup ? 6 : 5%>" class="text-center py-5">
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

<script>
    // ==========================================
    // VARIABEL GLOBAL & KONFIGURASI
    // ==========================================
    const masterClassTopup<%=rnd%> = "ais.database.model.Deposit";
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
        const colSpan = canEdit<%=rnd%> ? 6 : 5;
        tbody.innerHTML = '<tr><td colspan="' + colSpan + '" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Sistem sedang memuat data topup...")%></span></td></tr>';

        const sqlFilters = getFilterQueryTopup<%=rnd%>();
        const offset = (currentPageTopup<%=rnd%> - 1) * limitPerPageTopup<%=rnd%>;

        const sqlCount = "SELECT COUNT(d.id) AS jumlah FROM public.deposit d LEFT JOIN koperasi.anggota_koperasi a ON d.anggota_koperasi = a.id " + sqlFilters + ";";
        
        // Ambil ID Anggota Koperasi juga
        const sqlData = "SELECT d.id, TO_CHAR(d.waktu, 'DD Mon YYYY HH24:MI') as waktu_str, d.waktu, (case when a.nama is null then d.nama else a.kode||' '||a.nama end) as nama, d.keterangan, d.nominal, " +
                        "d.jenis_pembayaran, d.jenis_tabungan, d.anggota_koperasi, a.nama as nama_anggota, a.kode as kode_anggota " +
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

                let actBtn = '';
                if (canEdit<%=rnd%>) {
                    actBtn = '<td class="text-center text-nowrap">' +
                                '<button class="btn btn-sm btn-outline-warning text-dark shadow-sm px-2 me-1 mb-1 fw-bold" onclick="editTopup<%=rnd%>(' + row.id + ', \'' + isoWaktu + '\', \'' + row.nominal + '\', \'' + safeKet + '\', \'' + idAnggota + '\', \'' + namaAnggotaStr.replace(/'/g, "\\'") + '\', \'' + (row.jenis_pembayaran||'') + '\', \'' + (row.jenis_tabungan||'') + '\')" title="<%=Common.getBahasaConfig("Ubah Data")%>"><i class="fas fa-edit"></i></button>' +
                                '<button class="btn btn-sm btn-outline-danger shadow-sm px-2 mb-1 fw-bold" onclick="hapusTopup<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Hapus Data")%>"><i class="fas fa-trash-alt"></i></button>' +
                            '</td>';
                }

                htmlList += '<tr class="border-bottom" id="row-topup-<%=rnd%>-' + row.id + '">' +
                        '<td class="text-center text-muted fw-medium">' + (no++) + '</td>' +
                        '<td class="text-secondary small fw-semibold"><i class="far fa-clock me-1"></i>' + (row.waktu_str || '-') + '</td>' +
                        '<td class="fw-bold text-dark">' + safeNama + '</td>' +
                        '<td class="text-end fw-bolder text-success fs-6">' + formatRp<%=rnd%>(row.nominal) + '</td>' +
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

        const dataObj = {
            waktu: formattedWaktu,
            nominal: valNominal,
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

    const editTopup<%=rnd%> = (id, waktu, nominal, ket, idAnggota, namaAnggotaStr, jnsPem, jnsTab) => {
        if(!canEdit<%=rnd%>) return;

        document.getElementById('inputIdTopup<%=rnd%>').value = id;
        document.getElementById('inputWaktuTopup<%=rnd%>').value = waktu;
        document.getElementById('inputNominalTopup<%=rnd%>').value = nominal;
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