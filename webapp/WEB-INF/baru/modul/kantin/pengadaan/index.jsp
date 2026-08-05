<%@page import="ais.database.model.inventory.PengadaanProduk"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
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
Pedagang pedagang = tbmuser.getPedagang();
Toko toko = pedagang == null ? null : pedagang.getToko();
String rnd = Common.getGeneratedBarCode(7);

// Aturan BARU: Admin bisa lihat semua toko. Pedagang BISA CRUD tetapi HANYA tokonya sendiri.
boolean isAdmin = (toko == null);
String idTokoAktif = toko != null ? String.valueOf(toko.getId()) : "";
String namaTokoAktif = toko != null ? toko.getNama() : "";
// Gerbang entry Kulakan (Harga Beli): SAMA PERSIS syarat dgn KantinHelper.kulakanSimpan (POS
// Desktop/Android) -- admin global ATAU pedagang berstatus Supervisor. Pedagang toko biasa (bukan
// supervisor) HANYA boleh melihat riwayat, tak boleh tambah/ubah/hapus/impor entry Kulakan dari
// sini -- sebelumnya layar JSP ini TIDAK punya gerbang ini sama sekali (celah dibanding POS klien).
boolean bolehEntryKulakan = isAdmin || (pedagang != null && Boolean.TRUE.equals(pedagang.getSupervisor()));
%>

<script data-cfasync="false" src="https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js"></script>

<div id="viewList<%=rnd%>" class="animate__animated animate__fadeInUp">
    <div class="row mb-4">
        <div class="col-12">
            <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
                
                <div class="card-header bg-white border-0 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-3 border-bottom border-light">
                    <div>
                        <h5 class="fw-bold text-dark mb-1">
                            <i class="fas fa-truck-loading text-primary me-2"></i>
                            <% if (isAdmin) { %>
                                <%=Common.getBahasaConfig("Riwayat Pengadaan Barang (Semua Toko)")%>
                            <% } else { %>
                                <%=Common.getBahasaConfig("Riwayat Pengadaan - ")%> <span class="text-primary"><%=namaTokoAktif%></span>
                            <% } %>
                        </h5>
                        <small class="text-muted"><%=Common.getBahasaConfig("Catatan kulakan atau pembelian barang dari supplier (Pemasok).")%></small>
                    </div>
                    
                    <div class="d-flex flex-wrap gap-2">
                        <button class="btn btn-info text-white rounded-pill px-3 fw-bold shadow-sm" onclick="sinkronisasiStok<%=rnd%>()" id="btnSyncStok<%=rnd%>" title="<%=Common.getBahasaConfig("Kalkulasi ulang stok produk berdasarkan data ini")%>">
                            <i class="fas fa-sync me-1"></i><%=Common.getBahasaConfig("Sinkronkan Stok & Harga Modal")%>
                        </button>
                        
                        <% if (bolehEntryKulakan) { %>
                        <button class="btn btn-warning rounded-pill px-3 fw-bold shadow-sm text-dark" onclick="showUploadModal<%=rnd%>()" title="<%=Common.getBahasaConfig("Unggah file Excel")%>">
                            <i class="fas fa-file-upload me-1"></i><%=Common.getBahasaConfig("Unggah")%>
                        </button>
                        <% } %>

                        <button class="btn btn-success rounded-pill px-3 fw-bold shadow-sm" onclick="downloadExcelPengadaan<%=rnd%>()" title="<%=Common.getBahasaConfig("Unduh file Excel")%>">
                            <i class="fas fa-file-download me-1"></i><%=Common.getBahasaConfig("Unduh")%>
                        </button>

                        <% if (bolehEntryKulakan) { %>
                        <button class="btn btn-primary rounded-pill px-4 fw-bold shadow-sm" onclick="bukaFormTambah<%=rnd%>()">
                            <i class="fas fa-plus-circle me-2"></i><%=Common.getBahasaConfig("Tambah Pengadaan")%>
                        </button>
                        <% } else { %>
                        <span class="badge bg-secondary bg-opacity-10 text-secondary border px-3 py-2 fw-normal" title="<%=Common.getBahasaConfig("Hanya admin atau supervisor toko yang boleh mencatat Kulakan (Harga Beli).")%>">
                            <i class="fas fa-lock me-1"></i><%=Common.getBahasaConfig("Lihat Saja (Non-Supervisor)")%>
                        </span>
                        <% } %>
                    </div>
                </div>

                <div class="card-body p-4">
                    <div class="bg-light p-3 rounded-3 border shadow-sm mb-4">
                        <div class="row g-2 align-items-end">
                            <div class="col-md-2">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Nomor Faktur")%></label>
                                <input type="text" class="form-control form-control-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterFaktur<%=rnd%>" placeholder="<%=Common.getBahasaConfig("No. Nota/Faktur")%>">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Nama Supplier")%></label>
                                <input type="text" class="form-control form-control-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterSupplier<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari supplier...")%>">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Nama Produk")%></label>
                                <input type="text" class="form-control form-control-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterProduk<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari barang...")%>">
                            </div>
                            
                            <% if (isAdmin) { %>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Toko / Pedagang")%></label>
                                <select class="form-select form-select-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterToko<%=rnd%>">
                                    <option value=""><%=Common.getBahasaConfig("- Semua Toko -")%></option>
                                </select>
                            </div>
                            <% } else { %>
                                <input type="hidden" id="filterToko<%=rnd%>" value="<%=idTokoAktif%>">
                            <% } %>

                            <div class="col-md-auto ms-auto">
                                <button class="btn btn-sm btn-secondary fw-bold px-3 shadow-sm" onclick="triggerSearchPengadaan<%=rnd%>()">
                                    <i class="fas fa-search me-1"></i> <%=Common.getBahasaConfig("Saring")%>
                                </button>
                            </div>
                        </div>
                    </div>

                    <div class="table-responsive border rounded-3 shadow-sm bg-white">
                        <table class="table table-hover table-striped align-middle mb-0">
                            <thead class="table-dark text-center align-middle">
                                <tr>
                                    <th class="fw-semibold text-uppercase small py-3" style="width: 50px;">#</th>
                                    <th class="fw-semibold text-uppercase small text-start"><%=Common.getBahasaConfig("Waktu")%></th>
                                    <th class="fw-semibold text-uppercase small text-start"><%=Common.getBahasaConfig("No. Faktur")%></th>
                                    <th class="fw-semibold text-uppercase small text-start"><%=Common.getBahasaConfig("Supplier")%></th>
                                    <th class="fw-semibold text-uppercase small text-start"><%=Common.getBahasaConfig("Produk")%></th>
                                    <% if (isAdmin) { %>
                                        <th class="fw-semibold text-uppercase small text-start"><%=Common.getBahasaConfig("Toko")%></th>
                                    <% } %>
                                    <th class="fw-semibold text-uppercase small"><%=Common.getBahasaConfig("Qty")%></th>
                                    <th class="fw-semibold text-uppercase small text-end"><%=Common.getBahasaConfig("Harga Satuan")%></th>
                                    <th class="fw-semibold text-uppercase small text-end"><%=Common.getBahasaConfig("Total Harga")%></th>
                                    <th class="fw-semibold text-uppercase small text-center" style="width: 100px;"><%=Common.getBahasaConfig("Aksi")%></th>
                                </tr>
                            </thead>
                            <tbody id="tablePengadaan<%=rnd%>">
                                <tr><td colspan="<%=isAdmin ? 10 : 9%>" class="text-center py-5"><div class="spinner-border text-primary"></div></td></tr>
                            </tbody>
                        </table>
                    </div>

                    <div class="d-flex justify-content-between align-items-center mt-4 pt-2">
                        <span class="text-muted small fw-medium bg-light px-3 py-1 rounded-pill border" id="pagingInfoPengadaan<%=rnd%>"></span>
                        <nav>
                            <ul class="pagination pagination-sm mb-0 shadow-sm">
                                <li class="page-item">
                                    <button class="page-link text-primary rounded-start-pill px-3" id="btnPrevPengadaan<%=rnd%>" onclick="changePagePengadaan<%=rnd%>(-1)"><i class="fas fa-chevron-left"></i></button>
                                </li>
                                <li class="page-item disabled">
                                    <span class="page-link fw-bold bg-light text-dark px-3" id="pageNumberPengadaan<%=rnd%>">1</span>
                                </li>
                                <li class="page-item">
                                    <button class="page-link text-primary rounded-end-pill px-3" id="btnNextPengadaan<%=rnd%>" onclick="changePagePengadaan<%=rnd%>(1)"><i class="fas fa-chevron-right"></i></button>
                                </li>
                            </ul>
                        </nav>
                    </div>

                </div>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="modalFormPengadaan<%=rnd%>" tabindex="-1" data-bs-backdrop="static" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered">
        <div class="modal-content border-0 rounded-4 shadow-lg">
            <div class="modal-header bg-light border-bottom-0 pb-3">
                <h5 class="modal-title fw-bold text-dark" id="modalTitlePengadaan<%=rnd%>">
                    <i class="fas fa-truck-loading text-primary me-2"></i><%=Common.getBahasaConfig("Form Pengadaan Produk")%>
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4 pt-2">
                <form id="formPengadaan<%=rnd%>" onsubmit="event.preventDefault(); savePengadaan<%=rnd%>();">
                    <input type="hidden" id="formId<%=rnd%>" value="">
                    
                    <div class="row g-3">
                        
                        <% if (isAdmin) { %>
                        <div class="col-md-6">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Toko / Pedagang")%> <span class="text-danger">*</span></label>
                            <select class="form-select fw-semibold" id="formToko<%=rnd%>" onchange="loadComboProduk<%=rnd%>()" required>
                                <option value=""><%=Common.getBahasaConfig("-- Pilih Toko Terlebih Dahulu --")%></option>
                            </select>
                        </div>
                        <% } else { %>
                            <input type="hidden" id="formToko<%=rnd%>" value="<%=idTokoAktif%>">
                        <% } %>
                        
                        <div class="col-md-<%=isAdmin ? "6" : "12"%>">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Produk / Barang")%> <span class="text-danger">*</span></label>
                            <div class="input-group shadow-sm">
                                <span class="input-group-text bg-white border-end-0"><i class="fas fa-box text-primary"></i></span>
                                <input type="text" class="form-control border-start-0 fw-bold" list="listProduk<%=rnd%>" id="inputCariProduk<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Pilih Toko dulu...")%>" autocomplete="off" disabled required>
                            </div>
                            <datalist id="listProduk<%=rnd%>"></datalist>
                            <input type="hidden" id="idProdukSelected<%=rnd%>" value="">
                        </div>

                        <div class="col-md-12 p-3 bg-primary bg-opacity-10 rounded-3 border border-primary border-opacity-25 mt-3">
                            <label class="form-label small fw-bold text-primary mb-1"><i class="fas fa-search me-1"></i><%=Common.getBahasaConfig("Cari Supplier / Pemasok (Master)")%></label>
                            <div class="input-group shadow-sm">
                                <span class="input-group-text bg-white border-end-0"><i class="fas fa-building text-primary"></i></span>
                                <input type="text" class="form-control border-start-0 fw-bold" list="listSupplier<%=rnd%>" id="inputCariSupplier<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik nama supplier...")%>" autocomplete="off" required>
                            </div>
                            <datalist id="listSupplier<%=rnd%>"></datalist>
                            <input type="hidden" id="idSupplierSelected<%=rnd%>" value="">
                        </div>

                        <div class="col-md-6 mt-3">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nomor Faktur / Nota")%></label>
                            <input type="text" class="form-control" id="formFaktur<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Opsional")%>">
                        </div>
                        
                        <div class="col-md-6 mt-3">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Waktu Pengadaan")%> <span class="text-danger">*</span></label>
                            <input type="datetime-local" class="form-control" id="formWaktu<%=rnd%>" required>
                            <small class="text-muted" style="font-size: 10px;">*Tersimpan sbg dd-MM-yyyy HH:mm:ss</small>
                        </div>

                        <div class="col-md-4">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jumlah Masuk (Qty)")%> <span class="text-danger">*</span></label>
                            <input type="number" class="form-control text-primary fw-bold" id="formQty<%=rnd%>" required placeholder="0" min="0.01" step="0.01" oninput="hitungTotalHarga<%=rnd%>()">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Harga Beli Satuan")%> <span class="text-danger">*</span></label>
                            <div class="input-group">
                                <span class="input-group-text bg-light border-end-0">Rp</span>
                                <input type="number" class="form-control border-start-0" id="formHargaSatuan<%=rnd%>" required placeholder="0" min="0" oninput="hitungTotalHarga<%=rnd%>()">
                            </div>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Total Harga")%></label>
                            <div class="input-group">
                                <span class="input-group-text bg-light border-end-0">Rp</span>
                                <input type="number" class="form-control border-start-0 text-success fw-bold bg-light" id="formTotalHarga<%=rnd%>" readonly placeholder="0">
                            </div>
                            <small class="text-muted" style="font-size: 10px;">*Otomatis dihitung</small>
                        </div>

                        <div class="col-12">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Keterangan Tambahan")%></label>
                            <textarea class="form-control" id="formKeterangan<%=rnd%>" rows="2" placeholder="<%=Common.getBahasaConfig("Catatan...")%>"></textarea>
                        </div>
                    </div>

                    <div class="mt-4 text-end border-top pt-3">
                        <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
                        <button type="submit" class="btn btn-primary px-4 rounded-pill fw-bold shadow-sm" id="btnSavePengadaan<%=rnd%>">
                            <i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Data")%>
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="modalUploadExcel<%=rnd%>" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 rounded-4 shadow-lg">
            <div class="modal-header bg-light border-bottom-0 pb-3">
                <h5 class="modal-title fw-bold text-dark">
                    <i class="fas fa-file-upload text-warning me-2"></i><%=Common.getBahasaConfig("Unggah Data Pengadaan")%>
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4 pt-2 text-center">
                <p class="text-muted small mb-4"><%=Common.getBahasaConfig("Pastikan format file Excel yang Anda unggah sama persis dengan format file yang diunduh. Ekstensi: .xls, .xlsx")%></p>
                <input class="form-control mb-3" type="file" id="fileUploadExcel<%=rnd%>" accept=".xls,.xlsx">
                <div id="uploadStatusBox<%=rnd%>" class="alert alert-info py-2" style="display: none;"></div>
                <div class="mt-4">
                    <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
                    <button type="button" class="btn btn-warning text-dark px-4 rounded-pill fw-bold shadow-sm" id="btnProsesUpload<%=rnd%>" onclick="prosesUploadExcel<%=rnd%>()">
                        <i class="fas fa-cogs me-2"></i><%=Common.getBahasaConfig("Proses Data")%>
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    // ==========================================
    // VARIABEL GLOBAL & UTILITIES
    // ==========================================
    const masterModelClass<%=rnd%> = "ais.database.model.inventory.PengadaanProduk";
    const isAdminPengadaan<%=rnd%> = <%=isAdmin%>;
    const bolehEntryPengadaan<%=rnd%> = <%=bolehEntryKulakan%>;
    const idTokoLogin<%=rnd%> = '<%=idTokoAktif%>';
    const colSpan<%=rnd%> = isAdminPengadaan<%=rnd%> ? 10 : 9;
    
    let arrDataSupplier<%=rnd%> = [];
    let arrDataProduk<%=rnd%> = [];
    let modalInstancePengadaan<%=rnd%> = null;
    let modalInstanceUpload<%=rnd%> = null;

    let currentPagePengadaan<%=rnd%> = 1;
    const limitPerPagePengadaan<%=rnd%> = 10;
    let totalRecordsPengadaan<%=rnd%> = 0;

    const formatRp<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);
    };



    const fetchSqlData<%=rnd%> = async (sql) => {
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sql: sql, action: "sql" })
            });
            const result = await res.json();
            return result.data || [];
        } catch (e) { 
            console.error("Fetch Error: ", e); 
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

    const hitungTotalHarga<%=rnd%> = () => {
        const qty = parseFloat(document.getElementById('formQty<%=rnd%>').value) || 0;
        const harga = parseFloat(document.getElementById('formHargaSatuan<%=rnd%>').value) || 0;
        document.getElementById('formTotalHarga<%=rnd%>').value = qty * harga;
    };


    // ==========================================
    // INIT COMBO BOX (TOKO & SUPPLIER)
    // ==========================================
    const loadMasterDataCombo<%=rnd%> = async () => {
        // 1. Load Supplier (PenyediaAsset) ke Datalist - BISA UNTUK ADMIN DAN PEDAGANG
        const sqlSupplier = "SELECT id, nama FROM asset.penyedia_asset WHERE aktif = true ORDER BY nama ASC";
        const resSupplier = await fetchSqlData<%=rnd%>(sqlSupplier);
        arrDataSupplier<%=rnd%> = resSupplier;

        let listSupplierHtml = '';
        resSupplier.forEach(item => {
            listSupplierHtml += '<option value="' + item.nama + '" data-id="' + item.id + '">';
        });
        const dtListSupplier = document.getElementById('listSupplier<%=rnd%>');
        if(dtListSupplier) dtListSupplier.innerHTML = listSupplierHtml;

        // 2. Load Toko (HANYA JIKA ADMIN)
        if (isAdminPengadaan<%=rnd%>) {
            const sqlToko = "SELECT id, nama FROM koperasi.toko WHERE aktif = true ORDER BY nama ASC";
            const resToko = await fetchSqlData<%=rnd%>(sqlToko);
            
            let optFilterToko = '<option value=""><%=Common.getBahasaConfig("- Semua Toko -")%></option>';
            let optFormToko = '<option value=""><%=Common.getBahasaConfig("-- Pilih Toko Terlebih Dahulu --")%></option>';
            
            resToko.forEach(item => {
                const opt = '<option value="' + item.id + '">' + item.nama + '</option>';
                optFilterToko += opt;
                optFormToko += opt;
            });
            
            document.getElementById('filterToko<%=rnd%>').innerHTML = optFilterToko;
            const formToko = document.getElementById('formToko<%=rnd%>');
            if (formToko) formToko.innerHTML = optFormToko;
        } 
    };

    // Event Listener: Tangkap ID Supplier yang dipilih (Berlaku untuk admin dan pedagang)
    document.getElementById('inputCariSupplier<%=rnd%>').addEventListener('change', function() {
        const val = this.value;
        const match = arrDataSupplier<%=rnd%>.find(s => s.nama === val);
        document.getElementById('idSupplierSelected<%=rnd%>').value = match ? match.id : "";
    });

    // Event Listener: Tangkap ID Produk yang dipilih
    document.getElementById('inputCariProduk<%=rnd%>').addEventListener('change', function() {
        const val = this.value;
        const match = arrDataProduk<%=rnd%>.find(p => p.nama_lengkap === val);
        document.getElementById('idProdukSelected<%=rnd%>').value = match ? match.id : "";
    });

    // Load Produk secara dinamis (berdasarkan Toko) dengan format Datalist Searchbox
    const loadComboProduk<%=rnd%> = async (selectedProdukId = null, selectedProdukName = null) => {
        // Ambil TokoID, jika admin dari combo formToko, jika pedagang dari hidden input idTokoLogin
        const tokoId = isAdminPengadaan<%=rnd%> ? document.getElementById('formToko<%=rnd%>').value : idTokoLogin<%=rnd%>;
        const inputProduk = document.getElementById('inputCariProduk<%=rnd%>');
        const dtListProduk = document.getElementById('listProduk<%=rnd%>');
        
        if (tokoId === "") {
            inputProduk.value = "";
            inputProduk.placeholder = '<%=Common.getBahasaConfigJS("Pilih Toko dulu...")%>';
            inputProduk.disabled = true;
            document.getElementById('idProdukSelected<%=rnd%>').value = "";
            return;
        }

        inputProduk.placeholder = '<%=Common.getBahasaConfigJS("Memuat data...")%>';
        inputProduk.disabled = true;

        // Query produk berdasarkan toko
        const sqlProduk = "SELECT id, nama, kode FROM koperasi.produk WHERE aktif = true AND toko = " + tokoId + " ORDER BY nama ASC";
        const resProduk = await fetchSqlData<%=rnd%>(sqlProduk);
        
        let listProdukHtml = '';
        arrDataProduk<%=rnd%> = []; // Reset array lokal
        
        resProduk.forEach(item => {
            const kode = item.kode ? ' [' + item.kode + ']' : "";
            const namaLengkap = item.nama + kode;
            
            arrDataProduk<%=rnd%>.push({
                id: item.id,
                nama_lengkap: namaLengkap
            });
            
            listProdukHtml += '<option value="' + namaLengkap + '" data-id="' + item.id + '">';
        });
        
        dtListProduk.innerHTML = listProdukHtml;
        inputProduk.placeholder = '<%=Common.getBahasaConfigJS("Ketik nama / kode produk...")%>';
        inputProduk.disabled = false;

        // Auto-select jika Mode Edit
        if (selectedProdukId) {
            document.getElementById('idProdukSelected<%=rnd%>').value = selectedProdukId;
            inputProduk.value = selectedProdukName || "";
        } else {
            inputProduk.value = "";
            document.getElementById('idProdukSelected<%=rnd%>').value = "";
        }
    };


    // ==========================================
    // READ (TAMPILKAN TABEL)
    // ==========================================
    const buildFiltersPengadaan<%=rnd%> = () => {
        const faktur = document.getElementById('filterFaktur<%=rnd%>').value.trim().replace(/'/g, "''");
        const supplier = document.getElementById('filterSupplier<%=rnd%>').value.trim().replace(/'/g, "''");
        const produk = document.getElementById('filterProduk<%=rnd%>').value.trim().replace(/'/g, "''");
        
        const elToko = document.getElementById('filterToko<%=rnd%>');
        const toko = elToko ? elToko.value : "";

        let filters = " WHERE 1=1 ";
        
        if (faktur !== "") filters += " AND a.nomorfaktur ILIKE '%" + faktur + "%' ";
        if (supplier !== "") filters += " AND (a.namasupplier ILIKE '%" + supplier + "%' OR s.nama ILIKE '%" + supplier + "%') ";
        if (produk !== "") filters += " AND p.nama ILIKE '%" + produk + "%' ";
        
        if (!isAdminPengadaan<%=rnd%>) {
            filters += " AND a.toko = " + idTokoLogin<%=rnd%> + " ";
        } else if (toko !== "") {
            filters += " AND a.toko = " + toko + " ";
        }

        return filters;
    };

    const triggerSearchPengadaan<%=rnd%> = () => {
        currentPagePengadaan<%=rnd%> = 1;
        loadDataPengadaanAPI<%=rnd%>();
    };

    const loadDataPengadaanAPI<%=rnd%> = async () => {
        const tbody = document.getElementById('tablePengadaan<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center py-5 text-muted"><div class="spinner-border text-primary mb-3"></div><br><%=Common.getBahasaConfig("Memuat data pengadaan...")%></td></tr>';

        const offset = (currentPagePengadaan<%=rnd%> - 1) * limitPerPagePengadaan<%=rnd%>;
        const sqlFilters = buildFiltersPengadaan<%=rnd%>();

        const sqlQueryData = "SELECT a.id, TO_CHAR(a.waktupengadaan, 'YYYY-MM-DD HH24:MI') as waktu, " +
                             "a.nomorfaktur, COALESCE(s.nama, a.namasupplier) as namasupplier, a.qty, a.hargabelisatuan, a.totalharga, a.keterangan, " +
                             "a.waktupengadaan as w_raw, " +
                             "a.supplier as id_supplier, " +
                             "p.id AS id_produk, p.nama AS nama_produk, p.kode AS kode_produk, " +
                             "t.id AS id_toko, t.nama AS nama_toko " +
                             "FROM koperasi.pengadaan_produk a " +
                             "LEFT JOIN koperasi.produk p ON a.produk = p.id " +
                             "INNER JOIN koperasi.toko t ON (a.toko = t.id and t.aktif) " +
                             "LEFT JOIN asset.penyedia_asset s ON a.supplier = s.id " +
                             sqlFilters +
                             " ORDER BY a.waktupengadaan DESC, a.id DESC LIMIT " + limitPerPagePengadaan<%=rnd%> + " OFFSET " + offset + ";";

        const sqlQueryCount = "SELECT COUNT(*) AS jumlah FROM koperasi.pengadaan_produk a " +
                              "LEFT JOIN koperasi.produk p ON a.produk = p.id " +
                              "INNER JOIN koperasi.toko t ON (a.toko = t.id and t.aktif) " +
                              "LEFT JOIN asset.penyedia_asset s ON a.supplier = s.id " + 
                              sqlFilters + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetchSqlData<%=rnd%>(sqlQueryCount),
                fetchSqlData<%=rnd%>(sqlQueryData)
            ]);

            totalRecordsPengadaan<%=rnd%> = (resCount && resCount[0]) ? parseInt(resCount[0].jumlah || 0) : 0;
            const recordList = resData || [];

            let htmlTbody = '';
            if (recordList.length === 0) {
                htmlTbody = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center text-muted py-5"><i class="fas fa-truck-loading fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Belum ada data pengadaan.")%></td></tr>';
            } else {
                let no = offset + 1;
                recordList.forEach(row => {
                    
                    const safeSupplier = (row.namasupplier || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    const safeFaktur = (row.nomorfaktur || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    const safeKet = (row.keterangan || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    const w_edit = row.w_raw ? row.w_raw_iso : '';
                    
                    // Format Nama Produk + Kode yang aman untuk diedit
                    const k_prod = row.kode_produk ? ' [' + row.kode_produk + ']' : '';
                    const namaProdLengkap = (row.nama_produk || '') + k_prod;
                    const safeNamaProdukLengkap = namaProdLengkap.replace(/'/g, "\\'").replace(/"/g, '&quot;');

                    htmlTbody += '<tr>';
                    htmlTbody += '<td class="text-center text-muted">' + no++ + '</td>';
                    htmlTbody += '<td class="fw-medium text-secondary">' + (row.waktu || '-') + '</td>';
                    htmlTbody += '<td class="fw-bold">' + (row.nomorfaktur || '-') + '</td>';
                    htmlTbody += '<td class="text-dark">' + (row.namasupplier || '-') + '</td>';
                    htmlTbody += '<td class="fw-bold text-primary">' + namaProdLengkap + '</td>';
                    
                    if (isAdminPengadaan<%=rnd%>) {
                        htmlTbody += '<td><span class="text-muted">' + (row.nama_toko || '-') + '</span></td>';
                    }
                    
                    htmlTbody += '<td class="text-center fw-bold">' + (row.qty || 0) + '</td>';
                    htmlTbody += '<td class="text-end text-muted">' + formatRp<%=rnd%>(row.hargabelisatuan) + '</td>';
                    htmlTbody += '<td class="text-end fw-bold text-success">' + formatRp<%=rnd%>(row.totalharga) + '</td>';
                    
                    htmlTbody += '<td class="text-center text-nowrap">' +
                                    (bolehEntryPengadaan<%=rnd%> ?
                                    ('<button class="btn btn-sm btn-outline-warning text-dark shadow-sm me-1" onclick="showEditModal<%=rnd%>(' + row.id + ', \'' + safeFaktur + '\', \'' + safeSupplier + '\', \'' + w_edit + '\', ' + (row.id_toko || 'null') + ', ' + (row.id_produk || 'null') + ', \'' + safeNamaProdukLengkap + '\', ' + (row.id_supplier || 'null') + ', ' + (row.qty || 0) + ', ' + (row.hargabelisatuan || 0) + ', \'' + safeKet + '\')" title="Edit Data"><i class="fas fa-edit"></i></button>' +
                                    '<button class="btn btn-sm btn-outline-danger shadow-sm" onclick="deletePengadaan<%=rnd%>(' + row.id + ')" title="Hapus Data"><i class="fas fa-trash-alt"></i></button>')
                                    : '<span class="text-muted small"><i class="fas fa-lock"></i></span>') +
                                 '</td>';
                    
                    htmlTbody += '</tr>';
                });
            }
            tbody.innerHTML = htmlTbody;
            updatePaginationPengadaan<%=rnd%>();

        } catch (error) {
        	console.error(error);
            tbody.innerHTML = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal mengambil data.")%></td></tr>';
        }
    };

    const updatePaginationPengadaan<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecordsPengadaan<%=rnd%> / limitPerPagePengadaan<%=rnd%>) || 1;
        document.getElementById('pageNumberPengadaan<%=rnd%>').innerText = currentPagePengadaan<%=rnd%> + " / " + totalPages;
        
        const startIdx = (currentPagePengadaan<%=rnd%> - 1) * limitPerPagePengadaan<%=rnd%> + 1;
        const endIdx = Math.min(currentPagePengadaan<%=rnd%> * limitPerPagePengadaan<%=rnd%>, totalRecordsPengadaan<%=rnd%>);
        const startInfo = totalRecordsPengadaan<%=rnd%> > 0 ? startIdx : 0;
        
        document.getElementById('pagingInfoPengadaan<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startInfo + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari")%> <b class="text-dark">' + totalRecordsPengadaan<%=rnd%> + '</b> <%=Common.getBahasaConfig("data")%>';
        
        document.getElementById('btnPrevPengadaan<%=rnd%>').disabled = (currentPagePengadaan<%=rnd%> === 1);
        document.getElementById('btnNextPengadaan<%=rnd%>').disabled = (currentPagePengadaan<%=rnd%> === totalPages);
    };

    const changePagePengadaan<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecordsPengadaan<%=rnd%> / limitPerPagePengadaan<%=rnd%>);
        if (direction === -1 && currentPagePengadaan<%=rnd%> > 1) {
            currentPagePengadaan<%=rnd%>--;
            loadDataPengadaanAPI<%=rnd%>();
        } else if (direction === 1 && currentPagePengadaan<%=rnd%> < totalPages) {
            currentPagePengadaan<%=rnd%>++;
            loadDataPengadaanAPI<%=rnd%>();
        }
    };


    // ==========================================
    // SINKRONISASI STOK DAN HARGA (BARU)
    // ==========================================
    const sinkronisasiStok<%=rnd%> = async () => {
        const isConfirm = confirm('<%=Common.getBahasaConfigJS("Proses ini akan mengkalkulasi ulang seluruh stok produk, memperbarui harga beli terakhir, dan MENGKOREKSI harga jual jika ternyata lebih rendah dari modal. Lanjutkan?")%>');
        if (!isConfirm) return;

        const btnSync = document.getElementById('btnSyncStok<%=rnd%>');
        const originalBtnHtml = btnSync.innerHTML;

        btnSync.disabled = true;
        btnSync.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyinkronkan...")%>';
        
        try {
            const tokoFilter = document.getElementById('filterToko<%=rnd%>') ? document.getElementById('filterToko<%=rnd%>').value : "";
            const tokoIdSql = isAdminPengadaan<%=rnd%> ? tokoFilter : idTokoLogin<%=rnd%>; // Gunakan isAdminPengadaan jika di file Pengadaan
            
            let sqlFilters = " WHERE 1=1 ";
            if (tokoIdSql !== "") {
                 sqlFilters += " AND a.toko = " + tokoIdSql;
            }

            // PERBAIKAN SQL: Hitung Stok, Ambil Harga Beli Terbaru, lalu Amankan Harga Jual
            const sqlUpdate = "UPDATE koperasi.produk a " +
                "SET " +
                // 1. UPDATE STOK
                "stok = (" +
                "    COALESCE((SELECT SUM(qty) FROM koperasi.pengadaan_produk pp WHERE pp.produk = a.id), 0) " +
                "    + " +
                "    COALESCE((SELECT SUM(selisih) FROM koperasi.stok_opname so WHERE so.produk = a.id), 0) " +
                "    - " +
                "    COALESCE((SELECT SUM(qty) FROM koperasi.pembelian pb WHERE pb.produk = a.id), 0) " +
                "), " +
                
                // 2. UPDATE HARGA BELI (Dari pengadaan terakhir)
                "hargabeli = COALESCE(" +
                "    (SELECT pp2.hargabelisatuan FROM koperasi.pengadaan_produk pp2 WHERE pp2.produk = a.id ORDER BY pp2.waktupengadaan DESC, pp2.id DESC LIMIT 1), " +
                "    a.hargabeli " + 
                "), " +

                // 3. AUTO KOREKSI HARGA JUAL (Mencegah Margin Negatif)
                // Jika Harga Beli Terbaru >= Harga Jual Saat ini, maka Harga Jual dinaikkan sama dengan Harga Beli.
                "hargajual = CASE " +
                "    WHEN COALESCE((SELECT pp2.hargabelisatuan FROM koperasi.pengadaan_produk pp2 WHERE pp2.produk = a.id ORDER BY pp2.waktupengadaan DESC, pp2.id DESC LIMIT 1), a.hargabeli) >= a.hargajual " +
                "    THEN COALESCE((SELECT pp2.hargabelisatuan FROM koperasi.pengadaan_produk pp2 WHERE pp2.produk = a.id ORDER BY pp2.waktupengadaan DESC, pp2.id DESC LIMIT 1), a.hargabeli) " +
                "    ELSE a.hargajual " +
                "END " +
                
                sqlFilters;

            const payload = {
                action: "update_data",
                sql: sqlUpdate,
                tokoId: isAdminPengadaan<%=rnd%> ? "" : idTokoLogin<%=rnd%>
            };

            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const result = await response.json();

            if (result.status === '00' || result.status === 'success') {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Sinkronisasi Berhasil!")%>', 'bg-success text-white');
                // Refresh data tabel
                if(typeof loadDataProdukAPI<%=rnd%> === 'function') loadDataProdukAPI<%=rnd%>();
                if(typeof loadDataPengadaanAPI<%=rnd%> === 'function') loadDataPengadaanAPI<%=rnd%>();
            } else {
                showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal melakukan sinkronisasi.")%>', 'bg-danger text-white');
            }
        } catch (error) {
            console.error("Sinkronisasi Error: ", error);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Koneksi terputus saat sinkronisasi.")%>', 'bg-danger text-white');
        } finally {
            btnSync.innerHTML = originalBtnHtml;
            btnSync.disabled = false;
        }
    };

    // ==========================================
    // CREATE / UPDATE 
    // ==========================================
    const bukaFormTambah<%=rnd%> = () => {
        document.getElementById('modalTitlePengadaan<%=rnd%>').innerHTML = '<i class="fas fa-plus-circle text-primary me-2"></i><%=Common.getBahasaConfig("Catat Pengadaan Baru")%>';
        document.getElementById('formPengadaan<%=rnd%>').reset();
        document.getElementById('formId<%=rnd%>').value = "";
        
        document.getElementById('idSupplierSelected<%=rnd%>').value = "";
        document.getElementById('idProdukSelected<%=rnd%>').value = "";
        document.getElementById('inputCariProduk<%=rnd%>').value = "";
        
        if (isAdminPengadaan<%=rnd%>) {
            document.getElementById('formToko<%=rnd%>').value = "";
            document.getElementById('inputCariProduk<%=rnd%>').disabled = true;
            document.getElementById('inputCariProduk<%=rnd%>').placeholder = '<%=Common.getBahasaConfigJS("-- Pilih Toko Terlebih Dahulu --")%>';
        } else {
            // Jika pedagang, toko otomatis terpilih, load produk langsung
            loadComboProduk<%=rnd%>();
        }
        
        const now = new Date();
        now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
        document.getElementById('formWaktu<%=rnd%>').value = now.toISOString().slice(0,16);
        
        if(modalInstancePengadaan<%=rnd%> === null) {
            modalInstancePengadaan<%=rnd%> = new bootstrap.Modal(document.getElementById('modalFormPengadaan<%=rnd%>'));
        }
        modalInstancePengadaan<%=rnd%>.show();
    };

    const showEditModal<%=rnd%> = async (id, faktur, supplierName, waktu, idToko, idProduk, namaProdukFull, idSupplier, qty, hbs, ket) => {
        document.getElementById('modalTitlePengadaan<%=rnd%>').innerHTML = '<i class="fas fa-edit text-warning me-2"></i><%=Common.getBahasaConfig("Ubah Data Pengadaan")%>';
        
        document.getElementById('formId<%=rnd%>').value = id;
        document.getElementById('formFaktur<%=rnd%>').value = faktur;
        
        document.getElementById('inputCariSupplier<%=rnd%>').value = supplierName;
        // Tangani null jika idSupplier kosong (agar tidak "null" string)
        document.getElementById('idSupplierSelected<%=rnd%>').value = (idSupplier && idSupplier !== 'null') ? idSupplier : "";
		
        document.getElementById('formWaktu<%=rnd%>').value = waktu;
        
        if (isAdminPengadaan<%=rnd%>) {
            document.getElementById('formToko<%=rnd%>').value = idToko || '';
        }
        
        // Panggil fungsi untuk mengisi datalist Produk, lalu otomatis set value
        await loadComboProduk<%=rnd%>(idProduk, namaProdukFull);
        
        document.getElementById('formQty<%=rnd%>').value = qty;
        document.getElementById('formHargaSatuan<%=rnd%>').value = hbs;
        document.getElementById('formKeterangan<%=rnd%>').value = ket;
        
        hitungTotalHarga<%=rnd%>();

        if(modalInstancePengadaan<%=rnd%> === null) {
            modalInstancePengadaan<%=rnd%> = new bootstrap.Modal(document.getElementById('modalFormPengadaan<%=rnd%>'));
        }
        modalInstancePengadaan<%=rnd%>.show();
    };

    const savePengadaan<%=rnd%> = async () => {
        const id = document.getElementById('formId<%=rnd%>').value;
        const produkId = document.getElementById('idProdukSelected<%=rnd%>').value;
        const idSupplier = document.getElementById('idSupplierSelected<%=rnd%>').value;
        
        // Ambil TokoID, jika admin dari combo formToko, jika pedagang dari hidden input idTokoLogin
        const tokoId = isAdminPengadaan<%=rnd%> ? document.getElementById('formToko<%=rnd%>').value : idTokoLogin<%=rnd%>;
        
        const faktur = document.getElementById('formFaktur<%=rnd%>').value.trim();
        const strSupplier = document.getElementById('inputCariSupplier<%=rnd%>').value.trim();
        const waktuRaw = document.getElementById('formWaktu<%=rnd%>').value; 
        const qty = parseFloat(document.getElementById('formQty<%=rnd%>').value) || 0;
        const hbs = parseFloat(document.getElementById('formHargaSatuan<%=rnd%>').value) || 0;
        const ket = document.getElementById('formKeterangan<%=rnd%>').value.trim();

        if(tokoId === "" || produkId === "") {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Mohon lengkapi pilihan Toko dan Produk yang valid!")%>', 'bg-warning text-dark');
            return;
        }

        const btnSave = document.getElementById('btnSavePengadaan<%=rnd%>');
        const oriBtn = btnSave.innerHTML;
        btnSave.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyimpan...")%>';
        btnSave.disabled = true;

        // Kustomisasi Format Waktu "dd-MM-yyyy HH:mm:ss"
        const formattedWaktu = waktuRaw;

        // Mapping object untuk dikirim ke API
        const dataObj = {
            nomorFaktur: faktur,
            namaSupplier: strSupplier, 
            waktuPengadaan: waktuRaw, 
            qty: qty,
            hargaBeliSatuan: hbs,
            keterangan: ket,
            produk: produkId, 
            toko: tokoId
        };

        // Inject foreign key hanya jika valuenya tidak kosong dan tidak null literal
        if(idSupplier && idSupplier !== '') {
            dataObj.supplier = idSupplier;
        } else {
            dataObj.supplier = null;
        }

        const payload = {
        	action: "simpanDataRinci", 
        	class: "<%=PengadaanProduk.class.getName()%>",
        	log: "true",
            data: dataObj
        };

        if (id !== '') {
            payload.id = id;
        }

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const result = await response.json();
            
            if (result.status === '00' || result.status === 'success' || result.id) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data pengadaan berhasil disimpan.")%>', 'bg-success text-white');
                modalInstancePengadaan<%=rnd%>.hide();
                loadDataPengadaanAPI<%=rnd%>();
            } else {
                showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan data.")%>', 'bg-danger text-white');
            }
        } catch (error) {
            console.error("Save Error:", error);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Koneksi gagal. Silakan coba lagi.")%>', 'bg-danger text-white');
        } finally {
            btnSave.innerHTML = oriBtn;
            btnSave.disabled = false;
        }
    };

    const deletePengadaan<%=rnd%> = async (id) => {
        prosesDeleteData('<%=PengadaanProduk.class.getName()%>', id, function(){ 
             if (document.querySelectorAll('#tablePengadaan<%=rnd%> tr').length === 1 && currentPagePengadaan<%=rnd%> > 1) {
                 currentPagePengadaan<%=rnd%>--;
             }
             loadDataPengadaanAPI<%=rnd%>();
        });
    };


    // ==========================================
    // DOWNLOAD & UPLOAD EXCEL FEATURE
    // ==========================================
    
    const downloadExcelPengadaan<%=rnd%> = async () => {
        showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Sedang menyiapkan file Excel, mohon tunggu...")%>', 'bg-info text-dark');
        
        const sqlFilters = buildFiltersPengadaan<%=rnd%>();
        const sqlQuery = "SELECT a.id, TO_CHAR(a.waktupengadaan, 'YYYY-MM-DD HH24:MI:SS') as waktu, " +
                         "a.nomorfaktur, COALESCE(s.nama, a.namasupplier) as namasupplier, a.qty, a.hargabelisatuan, a.totalharga, a.keterangan, " +
                         "a.produk AS id_produk, a.toko AS id_toko, a.supplier as id_supplier " +
                         "FROM koperasi.pengadaan_produk a " +
                         "LEFT JOIN koperasi.produk p ON a.produk = p.id " +
                         "LEFT JOIN asset.penyedia_asset s ON a.supplier = s.id " +
                         sqlFilters + " ORDER BY a.waktupengadaan DESC;";

        try {
            const result = await fetchSqlData<%=rnd%>(sqlQuery);
            if (!result || result.length === 0) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Tidak ada data untuk diunduh.")%>', 'bg-warning text-dark');
                return;
            }

            let tableHtml = '<html xmlns:x="urn:schemas-microsoft-com:office:excel">';
            tableHtml += '<head><meta charset="UTF-8"><style>';
            tableHtml += 'th { background-color: #f8f9fa; font-weight: bold; border: 1px solid black; }';
            tableHtml += 'td { border: 1px solid black; }';
            tableHtml += '</style></head><body>';
            
            tableHtml += '<table><thead><tr>';
            tableHtml += '<th>ID_SISTEM</th>';
            tableHtml += '<th>ID_TOKO</th>';
            tableHtml += '<th>ID_PRODUK</th>';
            tableHtml += '<th>ID_SUPPLIER_MASTER</th>';
            tableHtml += '<th>waktupengadaan</th>';
            tableHtml += '<th>nomorfaktur</th>';
            tableHtml += '<th>namasupplier_TEKS</th>';
            tableHtml += '<th>QTY</th>';
            tableHtml += '<th>hargabelisatuan</th>';
            tableHtml += '<th>totalharga</th>';
            tableHtml += '<th>KETERANGAN</th>';
            tableHtml += '</tr></thead><tbody>';

            result.forEach(r => {
                tableHtml += '<tr>';
                tableHtml += '<td>' + (r.id || '') + '</td>';
                tableHtml += '<td>' + (r.id_toko || '') + '</td>';
                tableHtml += '<td>' + (r.id_produk || '') + '</td>';
                tableHtml += '<td>' + (r.id_supplier || '') + '</td>';
                tableHtml += '<td>' + (r.waktu || '') + '</td>';
                tableHtml += '<td>' + (r.nomorfaktur || '') + '</td>';
                tableHtml += '<td>' + (r.namasupplier || '') + '</td>';
                tableHtml += '<td>' + (r.qty || 0) + '</td>';
                tableHtml += '<td>' + (r.hargabelisatuan || 0) + '</td>';
                tableHtml += '<td>' + (r.totalharga || 0) + '</td>';
                tableHtml += '<td>' + (r.keterangan || '') + '</td>';
                tableHtml += '</tr>';
            });

            tableHtml += '</tbody></table></body></html>';

            const blob = new Blob([tableHtml], { type: 'application/vnd.ms-excel' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement("a");
            const timestamp = new Date().toISOString().slice(0,10).replace(/-/g,"");
            link.download = "Format_Pengadaan_Produk_" + timestamp + ".xls";
            link.href = url;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            setTimeout(() => URL.revokeObjectURL(url), 100);

        } catch (error) {
            console.error(error);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal mengunduh data.")%>', 'bg-danger text-white');
        }
    };

    const showUploadModal<%=rnd%> = () => {
        document.getElementById('fileUploadExcel<%=rnd%>').value = '';
        const statusBox = document.getElementById('uploadStatusBox<%=rnd%>');
        statusBox.style.display = 'none';
        statusBox.innerHTML = '';
        
        if(modalInstanceUpload<%=rnd%> === null) {
            modalInstanceUpload<%=rnd%> = new bootstrap.Modal(document.getElementById('modalUploadExcel<%=rnd%>'));
        }
        modalInstanceUpload<%=rnd%>.show();
    };

    const prosesUploadExcel<%=rnd%> = () => {
        const fileInput = document.getElementById('fileUploadExcel<%=rnd%>');
        const file = fileInput.files[0];
        
        if (!file) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Pilih file Excel terlebih dahulu!")%>', 'bg-warning text-dark');
            return;
        }
        
        const btnProcess = document.getElementById('btnProsesUpload<%=rnd%>');
        const statusBox = document.getElementById('uploadStatusBox<%=rnd%>');
        
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

                statusBox.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Mengunggah ' + excelRows.length + ' baris data ke server...';

                const batchData = excelRows.map(row => {
                    const obj = {
                        id: row['ID_SISTEM'] ? row['ID_SISTEM'].toString().trim() : "",
                        // Jika admin bisa semua, jika pedagang paksa toko ke tokonya sendiri
                        toko: isAdminPengadaan<%=rnd%> ? (row['ID_TOKO'] || "") : idTokoLogin<%=rnd%>,
                        produk: row['ID_PRODUK'] || "",
                        waktuPengadaan: row['waktupengadaan'] || "",
                        nomorFaktur: row['nomorfaktur'] || "",
                        namaSupplier: row['namasupplier_TEKS'] || "",
                        qty: parseFloat(row['QTY']) || 0,
                        hargaBeliSatuan: parseFloat(row['hargabelisatuan']) || 0,
                        keterangan: row['KETERANGAN'] || ""
                    };
                    
                    if (row['ID_SUPPLIER_MASTER']) {
                        obj.supplier = row['ID_SUPPLIER_MASTER'].toString().trim();
                    }
                    return obj;
                });

                const payload = {
                    action: "simpanBatchDataRinci", 
                    class: "<%=PengadaanProduk.class.getName()%>",
                    dataBatch: batchData
                };

                const response = await fetch('<%=Common.ROOT%>/Data', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
                
                const result = await response.json();

                if (result.status === '00' || result.status === 'success') {
                    statusBox.className = 'alert alert-success py-2 small fw-bold';
                    statusBox.innerHTML = '<i class="fas fa-check-circle me-2"></i>Data berhasil diunggah dan disimpan!';
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Upload Selesai!")%>', 'bg-success text-white');
                    
                    setTimeout(() => {
                        modalInstanceUpload<%=rnd%>.hide();
                        loadDataPengadaanAPI<%=rnd%>(); 
                    }, 1500);
                } else {
                    throw new Error(result.description || "Gagal menyimpan di sisi server.");
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
        
        loadMasterDataCombo<%=rnd%>();

        // Listener Filter Enter Key
        const filterInputs = document.querySelectorAll('.filter-input-<%=rnd%>');
        filterInputs.forEach(input => {
            if (input.tagName === 'SELECT') {
                input.addEventListener("change", () => triggerSearchPengadaan<%=rnd%>());
            } else {
                input.addEventListener("keydown", (event) => {
                    if (event.key === "Enter") {
                        event.preventDefault(); 
                        triggerSearchPengadaan<%=rnd%>();
                    }
                });
            }
        });

        // First Load
        loadDataPengadaanAPI<%=rnd%>();
    });
</script>