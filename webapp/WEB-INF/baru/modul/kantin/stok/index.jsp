<%@page import="java.net.URLEncoder"%>
<%@page import="ais.database.model.inventory.StokOpname"%>
<%@page import="ais.database.model.inventory.Produk"%>
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
Pedagang pedagangLogin = tbmuser.getPedagang();
Toko tokoLogin = pedagangLogin == null ? null : pedagangLogin.getToko();
String rnd = Common.getGeneratedBarCode(7);

// Aturan: Admin bisa semua toko. Pedagang HANYA tokonya sendiri, tapi BOLEH CRUD.
boolean isAdmin = (tokoLogin == null);
String idTokoAktif = tokoLogin != null ? String.valueOf(tokoLogin.getId()) : "";
String namaTokoAktif = tokoLogin != null ? tokoLogin.getNama() : "";
%>

<script data-cfasync="false" src="https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js"></script>

<div id="viewList<%=rnd%>" class="animate__animated animate__fadeInUp">
    <div class="row mb-4">
        <div class="col-12">
            <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
                
                <div class="card-header bg-white border-0 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-3 border-bottom border-light">
                    <div>
                        <h5 class="fw-bold text-dark mb-1">
                            <i class="fas fa-clipboard-check text-primary me-2"></i>
                            <% if (isAdmin) { %>
                                <%=Common.getBahasaConfig("Manajemen Stok Opname (Semua Toko)")%>
                            <% } else { %>
                                <%=Common.getBahasaConfig("Stok Opname - ")%> <span class="text-primary"><%=namaTokoAktif%></span>
                            <% } %>
                        </h5>
                        <small class="text-muted"><%=Common.getBahasaConfig("Penyesuaian stok sistem dengan stok fisik nyata di kios/gudang.")%></small>
                    </div>
                    
                    <div class="d-flex align-items-center gap-2 flex-wrap">
                        <button class="btn btn-info text-white rounded-pill px-3 shadow-sm fw-bold" onclick="sinkronisasiStok<%=rnd%>()" id="btnSyncStok<%=rnd%>" title="<%=Common.getBahasaConfig("Kalkulasi ulang stok produk")%>">
                            <i class="fas fa-sync me-1"></i><%=Common.getBahasaConfig("Sinkronkan Stok")%>
                        </button>
                        
                        <button class="btn btn-warning rounded-pill px-3 shadow-sm fw-bold text-dark" onclick="showUploadModal<%=rnd%>()" title="<%=Common.getBahasaConfig("Unggah file Excel")%>">
                            <i class="fas fa-file-upload me-1"></i><%=Common.getBahasaConfig("Unggah")%>
                        </button>

                        <button class="btn btn-success rounded-pill px-3 shadow-sm fw-bold" onclick="downloadExcelOpname<%=rnd%>()" title="<%=Common.getBahasaConfig("Unduh file Excel")%>">
                            <i class="fas fa-file-download me-1"></i><%=Common.getBahasaConfig("Unduh")%>
                        </button>

                        <button class="btn btn-outline-secondary rounded-pill px-3 shadow-sm fw-bold" onclick="cetakPdfOpname<%=rnd%>()" title="<%=Common.getBahasaConfig("Cetak PDF")%>">
                            <i class="fas fa-file-pdf me-1"></i><%=Common.getBahasaConfig("Cetak PDF")%>
                        </button>

                        <button class="btn btn-primary rounded-pill px-4 shadow-sm fw-bold text-nowrap" onclick="bukaFormTambah<%=rnd%>()">
                            <i class="fas fa-plus-circle me-1"></i><%=Common.getBahasaConfig("Catat Opname")%>
                        </button>
                    </div>
                </div>
                
                <div class="card-body p-4 pt-3">
                    
                    <div class="bg-light p-3 rounded-3 border shadow-sm mb-4">
                        <div class="row g-2 align-items-end">
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Nama/Kode Produk")%></label>
                                <input type="text" class="form-control form-control-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterProduk<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari produk...")%>">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Bulan Opname")%></label>
                                <input type="month" class="form-control form-control-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterBulan<%=rnd%>">
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
                                <button class="btn btn-sm btn-secondary fw-bold px-3 shadow-sm" onclick="triggerSearchOpname<%=rnd%>()">
                                    <i class="fas fa-search me-1"></i> <%=Common.getBahasaConfig("Saring")%>
                                </button>
                            </div>
                        </div>
                    </div>

                    <div class="table-responsive border rounded-3 shadow-sm bg-white">
                        <table class="table table-hover table-striped align-middle mb-0">
                            <thead class="table-dark text-center align-middle">
                                <tr>
                                    <th class="fw-semibold text-uppercase small px-3 py-3" style="width: 60px;">#</th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-calendar-alt me-1"></i><%=Common.getBahasaConfig("Waktu Opname")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-box me-1"></i><%=Common.getBahasaConfig("Produk")%></th>
                                    <% if (isAdmin) { %>
                                        <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-store me-1"></i><%=Common.getBahasaConfig("Toko")%></th>
                                    <% } %>
                                    <th class="text-end fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Stok Sistem")%></th>
                                    <th class="text-end fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Stok Fisik")%></th>
                                    <th class="text-end fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Selisih")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Keterangan")%></th>
                                    <th class="fw-semibold text-uppercase small px-3 text-center" style="width: 100px;"><i class="fas fa-cogs"></i></th>
                                </tr>
                            </thead>
                            <tbody id="tabelDataOpname<%=rnd%>">
                                <tr><td colspan="<%= isAdmin ? 9 : 8 %>" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Memuat data...")%></span></td></tr>
                            </tbody>
                        </table>
                    </div>

                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mt-4">
                        <div class="small fw-medium text-muted bg-light px-3 py-1 rounded-pill border" id="pagingInfoOpname<%=rnd%>">
                        </div>
                        <nav>
                            <ul class="pagination pagination-sm mb-0 shadow-sm">
                                <li class="page-item">
                                    <button class="page-link text-primary rounded-start-pill px-3 fw-bold" id="btnPrevPage<%=rnd%>" onclick="changePageOpname<%=rnd%>(-1)">
                                        <i class="fas fa-chevron-left me-1"></i><%=Common.getBahasaConfig("Sebelumnnya")%>
                                    </button>
                                </li>
                                <li class="page-item disabled">
                                    <span class="page-link bg-light text-dark px-3 fw-bold" id="pageNumberDisplay<%=rnd%>">1</span>
                                </li>
                                <li class="page-item">
                                    <button class="page-link text-primary rounded-end-pill px-3 fw-bold" id="btnNextPage<%=rnd%>" onclick="changePageOpname<%=rnd%>(1)">
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
        <div class="col-lg-8">
            <div class="card border-0 shadow-sm rounded-4 border-top border-success border-4">
                <div class="card-header bg-white border-0 pt-4 pb-0 px-4">
                    <div class="d-flex justify-content-between align-items-center">
                        <h5 class="fw-bold text-dark mb-0" id="formTitle<%=rnd%>">
                            <i class="fas fa-clipboard-check text-success me-2"></i><%=Common.getBahasaConfig("Formulir Stok Opname")%>
                        </h5>
                    </div>
                </div>
                <div class="card-body p-4">
                    <form id="formOpname<%=rnd%>" onsubmit="event.preventDefault(); simpanOpname<%=rnd%>();">
                        <input type="hidden" id="inputIdOpname<%=rnd%>" value="">
                        
                        <div class="row g-3 mt-1">
                            
                            <% if (isAdmin) { %>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Pilih Toko/Kios")%> <span class="text-danger">*</span></label>
                                <select class="form-select shadow-sm fw-semibold" id="inputTokoForm<%=rnd%>" onchange="loadComboProduk<%=rnd%>()" required>
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Toko --")%></option>
                                </select>
                            </div>
                            <% } else { %>
                                <input type="hidden" id="inputTokoForm<%=rnd%>" value="<%=idTokoAktif%>">
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

                            <div class="col-md-12 mt-3">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Waktu Opname")%> <span class="text-danger">*</span></label>
                                <input type="datetime-local" class="form-control shadow-sm" id="inputWaktuOpname<%=rnd%>" required>
                            </div>

                            <div class="col-md-4 mt-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Stok Sistem (Aplikasi)")%></label>
                                <div class="input-group input-group-sm shadow-sm">
                                    <input type="number" step="0.01" class="form-control fw-bold bg-light text-muted text-end" id="inputStokSistem<%=rnd%>" placeholder="0" readonly>
                                    <span class="input-group-text bg-light text-muted">Qty</span>
                                </div>
                            </div>

                            <div class="col-md-4 mt-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Stok Fisik (Nyata)")%> <span class="text-danger">*</span></label>
                                <div class="input-group input-group-sm shadow-sm border border-primary rounded">
                                    <input type="number" step="0.01" class="form-control fw-bold text-primary text-end border-0" id="inputStokFisik<%=rnd%>" placeholder="0" oninput="hitungSelisih<%=rnd%>()" required>
                                    <span class="input-group-text bg-white border-0 text-primary">Qty</span>
                                </div>
                            </div>

                            <div class="col-md-4 mt-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Selisih (Fisik - Sistem)")%></label>
                                <div class="input-group input-group-sm shadow-sm">
                                    <input type="number" step="0.01" class="form-control fw-bold bg-light text-end" id="inputSelisih<%=rnd%>" placeholder="0" readonly>
                                    <span class="input-group-text bg-light text-muted">Qty</span>
                                </div>
                                <small class="text-muted fst-italic" style="font-size: 10px;">*Minus = Hilang/Rusak</small>
                            </div>

                            <div class="col-12 mt-3">
                                <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Keterangan Tambahan / Alasan Selisih")%></label>
                                <textarea class="form-control text-muted shadow-sm" id="inputKeterangan<%=rnd%>" rows="2" placeholder="<%=Common.getBahasaConfig("Contoh: Barang basi, hilang, dsb...")%>"></textarea>
                            </div>

                            <div class="col-12 mt-4 text-end border-top pt-3">
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

<div class="modal fade" id="modalUploadExcel<%=rnd%>" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow rounded-4">
            <div class="modal-header border-0 pb-0">
                <h5 class="fw-bold text-dark"><i class="fas fa-file-upload text-warning me-2"></i><%=Common.getBahasaConfig("Unggah Excel Stok Opname")%></h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body pt-2">
                <p class="small text-muted"><%=Common.getBahasaConfig("Gunakan format dari tombol Unduh sebagai contoh kolom. Baris dengan ID_SISTEM kosong akan disimpan sebagai data opname baru; baris dengan ID_SISTEM terisi akan memperbarui data yang sudah ada.")%></p>
                <input type="file" class="form-control shadow-sm" id="fileUploadExcel<%=rnd%>" accept=".xlsx,.xls,.csv">
                <div id="uploadStatusBox<%=rnd%>" class="mt-3" style="display:none;"></div>
            </div>
            <div class="modal-footer border-0 pt-0">
                <button type="button" class="btn btn-light rounded-pill px-3 fw-semibold border" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
                <button type="button" class="btn btn-warning text-dark rounded-pill px-3 fw-bold" id="btnProsesUpload<%=rnd%>" onclick="prosesUploadExcel<%=rnd%>()">
                    <i class="fas fa-cogs me-2"></i><%=Common.getBahasaConfig("Proses Data")%>
                </button>
            </div>
        </div>
    </div>
</div>

<script>
    // ==========================================
    // BAGIAN 1: UTILITIES & STATE GLOBAL
    // ==========================================
    const masterModelClass<%=rnd%> = "ais.database.model.inventory.StokOpname";

    const isAdmin<%=rnd%> = <%=isAdmin%>;
    const idTokoLogin<%=rnd%> = '<%=idTokoAktif%>';
    const colSpan<%=rnd%> = isAdmin<%=rnd%> ? 9 : 8;
    let modalInstanceUpload<%=rnd%> = null;

    let arrDataProduk<%=rnd%> = [];
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

    // Auto Hitung Selisih
    const hitungSelisih<%=rnd%> = () => {
        const sistem = parseFloat(document.getElementById('inputStokSistem<%=rnd%>').value) || 0;
        const fisik = parseFloat(document.getElementById('inputStokFisik<%=rnd%>').value) || 0;
        const selisih = fisik - sistem;
        
        const elSelisih = document.getElementById('inputSelisih<%=rnd%>');
        elSelisih.value = selisih;
        
        if (selisih < 0) {
            elSelisih.className = "form-control fw-bold bg-danger bg-opacity-10 text-danger text-end";
        } else if (selisih > 0) {
            elSelisih.className = "form-control fw-bold bg-success bg-opacity-10 text-success text-end";
        } else {
            elSelisih.className = "form-control fw-bold bg-light text-muted text-end";
        }
    };

    // ==========================================
    // INIT COMBO BOX (TOKO & PRODUK)
    // ==========================================
    const loadMasterDataCombo<%=rnd%> = async () => {
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

    // Event Listener: Tangkap ID Produk yang dipilih dan isi Stok Sistem
    document.getElementById('inputCariProduk<%=rnd%>').addEventListener('change', function() {
        const val = this.value;
        const match = arrDataProduk<%=rnd%>.find(p => p.nama_lengkap === val);
        
        if (match) {
            document.getElementById('idProdukSelected<%=rnd%>').value = match.id;
            document.getElementById('inputStokSistem<%=rnd%>').value = match.stok;
            hitungSelisih<%=rnd%>(); // Update selisih
        } else {
            document.getElementById('idProdukSelected<%=rnd%>').value = "";
            document.getElementById('inputStokSistem<%=rnd%>').value = "0";
            hitungSelisih<%=rnd%>();
        }
    });

    // Load Produk secara dinamis (Stok Sistem dikalkulasi langsung di SQL)
    const loadComboProduk<%=rnd%> = async (selectedProdukId = null, selectedProdukName = null, forceStokSistem = null) => {
        const tokoId = isAdmin<%=rnd%> ? document.getElementById('inputTokoForm<%=rnd%>').value : idTokoLogin<%=rnd%>;
        const inputProduk = document.getElementById('inputCariProduk<%=rnd%>');
        const dtListProduk = document.getElementById('listProduk<%=rnd%>');
        
        if (tokoId === "") {
            inputProduk.value = "";
            inputProduk.placeholder = '<%=Common.getBahasaConfigJS("Pilih Toko dulu...")%>';
            inputProduk.disabled = true;
            document.getElementById('idProdukSelected<%=rnd%>').value = "";
            document.getElementById('inputStokSistem<%=rnd%>').value = "0";
            return;
        }

        inputProduk.placeholder = '<%=Common.getBahasaConfigJS("Memuat data produk...")%>';
        inputProduk.disabled = true;

        // PERUBAHAN: Menarik nilai stok sistem secara langsung berdasarkan rekam jejak
        const sqlProduk = "SELECT a.id, a.nama, a.kode, " +
            "( " +
            "    COALESCE((SELECT SUM(qty) FROM koperasi.pengadaan_produk pp WHERE pp.produk = a.id), 0) " +
            "    + " +
            "    COALESCE((SELECT SUM(selisih) FROM koperasi.stok_opname so WHERE so.produk = a.id), 0) " +
            "    - " +
            "    COALESCE((SELECT SUM(qty) FROM koperasi.pembelian pb WHERE pb.produk = a.id), 0) " +
            ") as stok " +
            "FROM koperasi.produk a WHERE a.aktif = true AND a.toko = " + tokoId + " ORDER BY a.nama ASC";

        const resProduk = await fetchData<%=rnd%>(sqlProduk);
        
        let listProdukHtml = '';
        arrDataProduk<%=rnd%> = []; 
        
        resProduk.forEach(item => {
            const kode = item.kode ? ' [' + item.kode + ']' : "";
            const namaLengkap = item.nama + kode;
            const curStok = item.stok || 0;
            
            arrDataProduk<%=rnd%>.push({
                id: item.id,
                nama_lengkap: namaLengkap,
                stok: curStok
            });
            
            listProdukHtml += '<option value="' + namaLengkap + '" data-id="' + item.id + '">';
        });
        
        dtListProduk.innerHTML = listProdukHtml;
        inputProduk.placeholder = '<%=Common.getBahasaConfigJS("Ketik nama / kode produk...")%>';
        inputProduk.disabled = false;

        // Auto-select Mode Edit
        if (selectedProdukId) {
            document.getElementById('idProdukSelected<%=rnd%>').value = selectedProdukId;
            inputProduk.value = selectedProdukName || "";
            // Saat mode edit, biarkan menggunakan stok lama saat opname dilakukan 
            // agar data historis "Stok Sistem" tidak tiba-tiba berubah
            document.getElementById('inputStokSistem<%=rnd%>').value = forceStokSistem !== null ? forceStokSistem : 0;
        } else {
            inputProduk.value = "";
            document.getElementById('idProdukSelected<%=rnd%>').value = "";
            document.getElementById('inputStokSistem<%=rnd%>').value = "0";
        }
    };


    // ==========================================
    // READ (TAMPILKAN TABEL)
    // ==========================================
    const buildFilterSQL<%=rnd%> = () => {
        const produk = document.getElementById('filterProduk<%=rnd%>').value.trim().replace(/'/g, "''");
        const bulan = document.getElementById('filterBulan<%=rnd%>').value; // Format: YYYY-MM
        const elToko = document.getElementById('filterToko<%=rnd%>');
        const toko = elToko ? elToko.value : "";

        let filters = " WHERE 1=1 ";
        
        if (produk !== '') {
            filters += " AND (p.nama ILIKE '%" + produk + "%' OR p.kode ILIKE '%" + produk + "%') ";
        }
        
        if (bulan !== '') {
            filters += " AND TO_CHAR(a.waktuopname, 'YYYY-MM') = '" + bulan + "' ";
        }

        if (!isAdmin<%=rnd%>) {
            filters += " AND a.toko = " + idTokoLogin<%=rnd%> + " ";
        } else if (toko !== "") {
            filters += " AND a.toko = " + toko + " ";
        }

        return filters;
    };

    const triggerSearchOpname<%=rnd%> = () => {
        currentPage<%=rnd%> = 1; 
        loadDataOpname<%=rnd%>();
    };
    
    const resetAndLoadData<%=rnd%> = () => {
        document.getElementById('filterProduk<%=rnd%>').value = '';
        document.getElementById('filterBulan<%=rnd%>').value = '';
        if (isAdmin<%=rnd%>) {
            document.getElementById('filterToko<%=rnd%>').value = '';
        }
        currentPage<%=rnd%> = 1; 
        loadDataOpname<%=rnd%>();
    };

    const loadDataOpname<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataOpname<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Mengambil data...")%></span></td></tr>';

        const sqlFilters = buildFilterSQL<%=rnd%>();
        const offset = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%>;

        const sqlCount = "SELECT COUNT(*) AS jumlah FROM koperasi.stok_opname a LEFT JOIN koperasi.produk p ON a.produk = p.id " + sqlFilters + ";";
        
        // Gunakan nama kolom: stoksistem, stokfisik, waktuopname tanpa underscore
        const sqlData = "SELECT a.id, TO_CHAR(a.waktuopname, 'YYYY-MM-DD HH24:MI') as waktu, " +
                        "a.stoksistem, a.stokfisik, a.selisih, a.keterangan, a.waktuopname as w_raw, " +
                        "p.id AS id_produk, p.nama AS nama_produk, p.kode as kode_produk, " +
                        "t.id as id_toko, t.nama as nama_toko " +
                        "FROM koperasi.stok_opname a " +
                        "LEFT JOIN koperasi.produk p ON a.produk = p.id " +
                        "INNER JOIN koperasi.toko t ON (a.toko = t.id and t.aktif) " +
                        sqlFilters + " ORDER BY a.waktuopname DESC, a.id DESC LIMIT " + limitPerPage<%=rnd%> + " OFFSET " + offset + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetchData<%=rnd%>(sqlCount),
                fetchData<%=rnd%>(sqlData)
            ]);

            totalRecords<%=rnd%> = (resCount && resCount[0]) ? parseInt(resCount[0].jumlah || 0) : 0;
            const recordList = resData || [];

            let htmlList = '';

            if (recordList.length === 0) {
                htmlList = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center text-muted py-5"><i class="fas fa-clipboard-check fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Tidak ada data opname ditemukan.")%></td></tr>';
            } else {
                let no = offset + 1; 
                recordList.forEach((row) => {

                    const safeKet = (row.keterangan || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    
                    const k_prod = row.kode_produk ? ' [' + row.kode_produk + ']' : '';
                    const namaProdLengkap = (row.nama_produk || '-') + k_prod;
                    const safeNamaProduk = namaProdLengkap.replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    
                    // Format waktu raw ISO untuk datetime-local
                    const w_edit = row.w_raw ? row.w_raw_iso : '';
                    
                    const selisih = parseFloat(row.selisih || 0);
                    let selisihHtml = selisih;
                    if(selisih < 0) selisihHtml = '<span class="text-danger fw-bold">' + selisih + '</span>';
                    else if(selisih > 0) selisihHtml = '<span class="text-success fw-bold">+' + selisih + '</span>';

                    htmlList += '<tr>' +
                            '<td class="text-center text-muted fw-medium">' + (no++) + '</td>' +
                            '<td class="text-start fw-medium text-secondary">' + (row.waktu || '-') + '</td>' +
                            '<td class="text-start fw-bold text-primary">' + namaProdLengkap + '</td>';
                    
                    if (isAdmin<%=rnd%>) {
                        htmlList += '<td><span class="text-muted">' + (row.nama_toko || '-') + '</span></td>';
                    }        
                            
                    htmlList += '<td class="text-end text-muted">' + (row.stoksistem || 0) + '</td>' +
                            '<td class="text-end fw-bold text-dark">' + (row.stokfisik || 0) + '</td>' +
                            '<td class="text-end">' + selisihHtml + '</td>' +
                            '<td class="text-start text-muted small">' + (row.keterangan || '-') + '</td>' +
                            '<td class="text-center text-nowrap">' +
                                '<button class="btn btn-sm btn-outline-warning text-dark shadow-sm px-2 me-1 fw-bold" onclick="editOpname<%=rnd%>(' + row.id + ', \'' + w_edit + '\', ' + (row.id_toko || 'null') + ', ' + (row.id_produk || 'null') + ', \'' + safeNamaProduk + '\', ' + (row.stoksistem || 0) + ', ' + (row.stokfisik || 0) + ', \'' + safeKet + '\')" title="<%=Common.getBahasaConfig("Ubah Data")%>"><i class="fas fa-edit"></i></button>' +
                                '<button class="btn btn-sm btn-outline-danger shadow-sm px-2 fw-bold" onclick="hapusOpname<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Hapus Data")%>"><i class="fas fa-trash-alt"></i></button>' +
                            '</td>' +
                        '</tr>';
                });
            }
            tbody.innerHTML = htmlList;
            updatePaginationUI<%=rnd%>();

        } catch (error) {
            console.error("Gagal load tabel opname:", error);
            tbody.innerHTML = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal menarik data. Silakan muat ulang.")%></td></tr>';
        }
    };

    const updatePaginationUI<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumberDisplay<%=rnd%>').innerText = currentPage<%=rnd%> + " / " + totalPages;
        
        const startIdx = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%> + 1;
        const endIdx = Math.min(currentPage<%=rnd%> * limitPerPage<%=rnd%>, totalRecords<%=rnd%>);
        const startInfo = totalRecords<%=rnd%> > 0 ? startIdx : 0;
        
        document.getElementById('pagingInfoOpname<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startInfo + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari")%> <b class="text-dark">' + totalRecords<%=rnd%> + '</b> <%=Common.getBahasaConfig("data")%>';
        
        document.getElementById('btnPrevPage<%=rnd%>').disabled = (currentPage<%=rnd%> === 1);
        document.getElementById('btnNextPage<%=rnd%>').disabled = (currentPage<%=rnd%> === totalPages);
    };

    const changePageOpname<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if (direction === -1 && currentPage<%=rnd%> > 1) {
            currentPage<%=rnd%>--;
            loadDataOpname<%=rnd%>();
        } else if (direction === 1 && currentPage<%=rnd%> < totalPages) {
            currentPage<%=rnd%>++;
            loadDataOpname<%=rnd%>();
        }
    };


    // ==========================================
    // SINKRONISASI STOK KESELURUHAN PADA PRODUK
    // ==========================================
    const sinkronisasiStok<%=rnd%> = async () => {
        const isConfirm = confirm('<%=Common.getBahasaConfigJS("Proses ini akan mengkalkulasi ulang seluruh stok produk berdasarkan rekam jejak pengadaan, opname, dan penjualan. Proses ini mungkin memakan waktu. Lanjutkan?")%>');
        if (!isConfirm) return;

        const btnSync = document.getElementById('btnSyncStok<%=rnd%>');
        const originalBtnHtml = btnSync.innerHTML;

        btnSync.disabled = true;
        btnSync.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyinkronkan...")%>';
        
        try {
            const tokoIdSql = isAdmin<%=rnd%> ? "" : idTokoLogin<%=rnd%>;
            let sqlFilters = " WHERE 1=1 ";
            if (tokoIdSql !== "") {
                 sqlFilters += " AND a.toko = " + tokoIdSql;
            }
            
            const sqlUpdate = "UPDATE koperasi.produk a "+
            	"SET stok = ( "+
            	"	    COALESCE((SELECT SUM(qty) FROM koperasi.pengadaan_produk pp WHERE pp.produk = a.id), 0) "+
            	"	    + "+
            	"	    COALESCE((SELECT SUM(selisih) FROM koperasi.stok_opname so WHERE so.produk = a.id), 0)"+
            	"	    - "+
            	"	    COALESCE((SELECT SUM(qty) FROM koperasi.pembelian pb WHERE pb.produk = a.id), 0)"+
            	"	) "+sqlFilters;
            
            const payload = {
                action: "update_data",
                sql:sqlUpdate,
                tokoId: tokoIdSql
            };

            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const result = await response.json();

            if (result.status === '00' || result.status === 'success') {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Sinkronisasi Stok Produk Berhasil!")%>', 'bg-success text-white');
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
    // BAGIAN 3: LOGIKA FORM & DINAMIS UI 
    // ==========================================
    const bukaFormTambah<%=rnd%> = () => {
        
        document.getElementById('inputIdOpname<%=rnd%>').value = '';
        document.getElementById('formOpname<%=rnd%>').reset();
        
        document.getElementById('idProdukSelected<%=rnd%>').value = "";
        
        if (isAdmin<%=rnd%>) {
            const elToko = document.getElementById('inputTokoForm<%=rnd%>');
            elToko.value = "";
            document.getElementById('inputCariProduk<%=rnd%>').disabled = true;
            document.getElementById('inputCariProduk<%=rnd%>').placeholder = '<%=Common.getBahasaConfigJS("-- Pilih Toko Terlebih Dahulu --")%>';
        } else {
            // Jika pedagang, toko hidden otomatis terisi, load produk
            loadComboProduk<%=rnd%>();
        }
        
        // Default waktu ke saat ini
        const now = new Date();
        now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
        document.getElementById('inputWaktuOpname<%=rnd%>').value = now.toISOString().slice(0,16);

        document.getElementById('inputSelisih<%=rnd%>').className = "form-control fw-bold bg-light text-muted text-end";

        document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-plus-circle text-primary me-2"></i><%=Common.getBahasaConfig("Catat Opname Baru")%>';
        
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    };

    const tutupForm<%=rnd%> = () => {
        document.getElementById('viewForm<%=rnd%>').style.display = 'none';
        document.getElementById('viewList<%=rnd%>').style.display = 'block';
        loadDataOpname<%=rnd%>();
    };

    // ==========================================
    // BAGIAN 4: SIMPAN, EDIT, & HAPUS
    // ==========================================
    const simpanOpname<%=rnd%> = async () => {
        const idOpname = document.getElementById('inputIdOpname<%=rnd%>').value;
        const produkId = document.getElementById('idProdukSelected<%=rnd%>').value;
        const tokoId = isAdmin<%=rnd%> ? document.getElementById('inputTokoForm<%=rnd%>').value : idTokoLogin<%=rnd%>;
        
        const waktuRaw = document.getElementById('inputWaktuOpname<%=rnd%>').value; 
        const stokSistem = parseFloat(document.getElementById('inputStokSistem<%=rnd%>').value) || 0;
        const stokFisik = parseFloat(document.getElementById('inputStokFisik<%=rnd%>').value) || 0;
        const selisih = parseFloat(document.getElementById('inputSelisih<%=rnd%>').value) || 0;
        const ket = document.getElementById('inputKeterangan<%=rnd%>').value.trim();

        if(tokoId === "" || produkId === "") {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Pilih Toko dan Produk terlebih dahulu!")%>', 'bg-warning text-dark');
            return;
        }

        if(waktuRaw === "") {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Waktu Opname tidak boleh kosong!")%>', 'bg-warning text-dark');
            return;
        }

        const btnSimpan = document.getElementById('btnSimpan<%=rnd%>');
        const oriBtn = btnSimpan.innerHTML;
        btnSimpan.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyimpan...")%>';
        btnSimpan.disabled = true;

        const dataObj = {
            waktuOpname: waktuRaw, 
            stokSistem: stokSistem,
            stokFisik: stokFisik,
            selisih: selisih,
            keterangan: ket,
            produk: produkId,
            toko: tokoId
        };

        const payload = { 
            action: "simpanDataRinci", 
            class: masterModelClass<%=rnd%>, 
            data: dataObj
        };

        if (idOpname !== '') {
            payload.id = idOpname;
        }

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const result = await response.json();
            
            if (result.status === '00' || result.status === 'success' || result.id) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data stok opname berhasil disimpan!")%>', 'bg-success text-white');
                tutupForm<%=rnd%>(); 
            } else {
                showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan data.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            console.error(e);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi.")%>', 'bg-danger text-white');
        } finally {
            btnSimpan.innerHTML = oriBtn;
            btnSimpan.disabled = false;
        }
    };

    const editOpname<%=rnd%> = async (id, waktu, idToko, idProduk, namaProdukFull, stokSistem, stokFisik, ket) => {
        document.getElementById('inputIdOpname<%=rnd%>').value = id;
        document.getElementById('inputWaktuOpname<%=rnd%>').value = waktu;
        
        if (isAdmin<%=rnd%>) {
            document.getElementById('inputTokoForm<%=rnd%>').value = idToko || '';
        }
        
        // Load Produk Datalist & Set values
        await loadComboProduk<%=rnd%>(idProduk, namaProdukFull, stokSistem);
        
        document.getElementById('inputStokFisik<%=rnd%>').value = stokFisik;
        document.getElementById('inputKeterangan<%=rnd%>').value = ket;
        
        hitungSelisih<%=rnd%>();

        document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-edit text-warning me-2"></i><%=Common.getBahasaConfig("Ubah Data Opname")%>';
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    };

    const hapusOpname<%=rnd%> = async (id) => {
        if (typeof prosesDeleteData === 'function') {
            prosesDeleteData(masterModelClass<%=rnd%>, id, function(){ 
                 if (document.querySelectorAll('#tabelDataOpname<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) {
                     currentPage<%=rnd%>--;
                 }
                 loadDataOpname<%=rnd%>();
            });
        } else {
            const isConfirm = confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus data ini?")%>');
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
                    if (document.querySelectorAll('#tabelDataOpname<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) {
                        currentPage<%=rnd%>--;
                    }
                    loadDataOpname<%=rnd%>();
                } else {
                    showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Data gagal dihapus.")%>', 'bg-danger text-white');
                }
            } catch (error) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal terhubung ke server.")%>', 'bg-danger text-white');
            }
        }
    };


    // ==========================================
    // DOWNLOAD & UPLOAD EXCEL FEATURE
    // ==========================================
    const downloadExcelOpname<%=rnd%> = async () => {
        showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Menyiapkan file Excel...")%>', 'bg-info text-dark');
        
        const sqlFilters = buildFilterSQL<%=rnd%>();
        const sqlQuery = "SELECT a.id, TO_CHAR(a.waktuopname, 'YYYY-MM-DD\"T\"HH24:MI') as waktu, " +
                         "a.stoksistem, a.stokfisik, a.selisih, a.keterangan, " +
                         "a.produk AS id_produk, p.nama AS nama_produk, a.toko AS id_toko " +
                         "FROM koperasi.stok_opname a " +
                         "LEFT JOIN koperasi.produk p ON a.produk = p.id " +
                         sqlFilters + " ORDER BY a.waktuopname DESC;";

        try {
            const result = await fetchData<%=rnd%>(sqlQuery);
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
            tableHtml += '<th>NAMA_PRODUK_INFO</th>';
            tableHtml += '<th>waktuopname</th>';
            tableHtml += '<th>stoksistem</th>';
            tableHtml += '<th>stokfisik</th>';
            tableHtml += '<th>selisih</th>';
            tableHtml += '<th>KETERANGAN</th>';
            tableHtml += '</tr></thead><tbody>';

            result.forEach(r => {
                tableHtml += '<tr>';
                tableHtml += '<td>' + (r.id || '') + '</td>';
                tableHtml += '<td>' + (r.id_toko || '') + '</td>';
                tableHtml += '<td>' + (r.id_produk || '') + '</td>';
                tableHtml += '<td>' + (r.nama_produk || '') + '</td>';
                tableHtml += '<td>' + (r.waktu || '') + '</td>';
                tableHtml += '<td>' + (r.stoksistem || 0) + '</td>';
                tableHtml += '<td>' + (r.stokfisik || 0) + '</td>';
                tableHtml += '<td>' + (r.selisih || 0) + '</td>';
                tableHtml += '<td>' + (r.keterangan || '') + '</td>';
                tableHtml += '</tr>';
            });

            tableHtml += '</tbody></table></body></html>';

            const blob = new Blob([tableHtml], { type: 'application/vnd.ms-excel' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement("a");
            const timestamp = new Date().toISOString().slice(0,10).replace(/-/g,"");
            link.download = "Format_StokOpname_" + timestamp + ".xls";
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

    // ==========================================
    // CETAK PDF (print-friendly popup window)
    // ==========================================
    const cetakPdfOpname<%=rnd%> = async () => {
        showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Menyiapkan PDF...")%>', 'bg-info text-dark');

        const sqlFilters = buildFilterSQL<%=rnd%>();
        const sqlQuery = "SELECT TO_CHAR(a.waktuopname, 'YYYY-MM-DD HH24:MI') as waktu, " +
                         "a.stoksistem, a.stokfisik, a.selisih, a.keterangan, " +
                         "p.nama AS nama_produk, p.kode as kode_produk, t.nama as nama_toko " +
                         "FROM koperasi.stok_opname a " +
                         "LEFT JOIN koperasi.produk p ON a.produk = p.id " +
                         "INNER JOIN koperasi.toko t ON (a.toko = t.id and t.aktif) " +
                         sqlFilters + " ORDER BY a.waktuopname DESC;";

        const result = await fetchData<%=rnd%>(sqlQuery);
        if (!result || result.length === 0) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Tidak ada data untuk dicetak.")%>', 'bg-warning text-dark');
            return;
        }

        let baris = '';
        result.forEach(r => {
            const kode = r.kode_produk ? ' [' + r.kode_produk + ']' : '';
            const selisih = parseFloat(r.selisih || 0);
            baris += '<tr>' +
                '<td>' + (r.waktu || '-') + '</td>' +
                '<td>' + (r.nama_produk || '-') + kode + '</td>' +
                (isAdmin<%=rnd%> ? ('<td>' + (r.nama_toko || '-') + '</td>') : '') +
                '<td style="text-align:right;">' + (r.stoksistem || 0) + '</td>' +
                '<td style="text-align:right;">' + (r.stokfisik || 0) + '</td>' +
                '<td style="text-align:right;">' + (selisih >= 0 ? '+' : '') + selisih + '</td>' +
                '<td>' + (r.keterangan || '-') + '</td>' +
                '</tr>';
        });

        const kolomToko = isAdmin<%=rnd%> ? '<th><%=Common.getBahasaConfigJS("Toko")%></th>' : '';
        const html = '<!DOCTYPE html><html><head><meta charset="utf-8"><title>Stok Opname</title>' +
            '<style>body{font-family:Arial,sans-serif;font-size:11px;padding:16px;} h3{margin-bottom:4px;} p{margin-top:0;color:#555;} ' +
            'table{width:100%;border-collapse:collapse;} th,td{border:1px solid #ccc;padding:5px 7px;} th{background:#f0f0f0;text-align:left;}</style>' +
            '</head><body>' +
            '<h3><%=Common.getBahasaConfigJS("Laporan Stok Opname")%></h3>' +
            '<p>' + (isAdmin<%=rnd%> ? '<%=Common.getBahasaConfigJS("Semua Toko")%>' : '<%=namaTokoAktif%>') + '</p>' +
            '<table><thead><tr>' +
            '<th><%=Common.getBahasaConfigJS("Waktu Opname")%></th><th><%=Common.getBahasaConfigJS("Produk")%></th>' +
            kolomToko +
            '<th><%=Common.getBahasaConfigJS("Stok Sistem")%></th><th><%=Common.getBahasaConfigJS("Stok Fisik")%></th>' +
            '<th><%=Common.getBahasaConfigJS("Selisih")%></th><th><%=Common.getBahasaConfigJS("Keterangan")%></th>' +
            '</tr></thead><tbody>' + baris + '</tbody></table>' +
            '<script>window.onload = function(){ window.print(); };<' + '/script>' +
            '</body></html>';

        const jendela = window.open('', '_blank');
        jendela.document.open();
        jendela.document.write(html);
        jendela.document.close();
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
                        toko: isAdmin<%=rnd%> ? (row['ID_TOKO'] || "") : idTokoLogin<%=rnd%>,
                        produk: row['ID_PRODUK'] || "",
                        waktuOpname: row['waktuopname'] || "",
                        stokSistem: parseFloat(row['stoksistem']) || 0,
                        stokFisik: parseFloat(row['stokfisik']) || 0,
                        selisih: parseFloat(row['selisih']) || 0,
                        keterangan: row['KETERANGAN'] || ""
                    };
                    return obj;
                });

                const payload = {
                    action: "simpanBatchDataRinci", 
                    class: masterModelClass<%=rnd%>,
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
                        loadDataOpname<%=rnd%>(); 
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

        const filterInputs = document.querySelectorAll('.filter-input-<%=rnd%>');
        filterInputs.forEach(input => {
            if (input.tagName === 'SELECT' || input.type === 'month') {
                input.addEventListener("change", () => triggerSearchOpname<%=rnd%>());
            } else {
                input.addEventListener("keydown", (event) => {
                    if (event.key === "Enter") {
                        event.preventDefault(); 
                        triggerSearchOpname<%=rnd%>();
                    }
                });
            }
        });

        loadDataOpname<%=rnd%>();
    });
</script>