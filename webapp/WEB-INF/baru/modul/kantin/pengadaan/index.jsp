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

// Aturan: Admin bisa lihat semua toko (pilih salah satu dulu utk riwayat -- lihat catatan di JS,
// aksi kulakan_faktur_list/sinkron_stok_toko SELALU butuh SATU toko, tak ada mode "semua toko"
// agregat, sama seperti Electron/Flutter/mutasi_antar_outlet.jsp). Pedagang HANYA toko sendiri.
boolean isAdmin = (toko == null);
String idTokoAktif = toko != null ? String.valueOf(toko.getId()) : "";
String namaTokoAktif = toko != null ? toko.getNama() : "";
// Gerbang entry Kulakan (Harga Beli): SAMA PERSIS syarat dgn KantinHelper.kulakanFakturSimpan (POS
// Desktop/Android) -- admin global ATAU pedagang berstatus Supervisor. Pedagang toko biasa (bukan
// supervisor) HANYA boleh melihat riwayat, tak boleh tambah entry Kulakan dari sini.
boolean bolehEntryKulakan = isAdmin || (pedagang != null && Boolean.TRUE.equals(pedagang.getSupervisor()));
// Catatan restrukturisasi 2026-08-11: fitur "Unggah Excel" (batch import PengadaanProduk flat,
// bypass header) SENGAJA DIHAPUS di sini, bukan lupa dimigrasi -- bertentangan dgn arsitektur baru
// header (PengadaanFaktur) + item (kulakan_faktur_simpan mewajibkan SATU header per panggilan).
// Import massal via Excel utk pola per-Faktur butuh desain baru (mis. 1 file = banyak faktur
// terpisah) yg di luar lingkup batch ini. "Unduh" tetap ada (ekspor rincian per-barang, read-only).
%>

<div id="viewList<%=rnd%>" class="animate__animated animate__fadeInUp">
    <div class="row mb-4">
        <div class="col-12">
            <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">

                <div class="card-header bg-white border-0 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-3 border-bottom border-light">
                    <div>
                        <h5 class="fw-bold text-dark mb-1">
                            <i class="fas fa-truck-loading text-primary me-2"></i>
                            <% if (isAdmin) { %>
                                <%=Common.getBahasaConfig("Riwayat Kulakan / Pengadaan (per Faktur)")%>
                            <% } else { %>
                                <%=Common.getBahasaConfig("Riwayat Kulakan - ")%> <span class="text-primary"><%=namaTokoAktif%></span>
                            <% } %>
                        </h5>
                        <small class="text-muted"><%=Common.getBahasaConfig("Satu baris = satu Faktur/Nota pembelian dari supplier (bisa berisi banyak barang).")%></small>
                    </div>

                    <div class="d-flex flex-wrap gap-2">
                        <button class="btn btn-info text-white rounded-pill px-3 fw-bold shadow-sm" onclick="sinkronisasiStok<%=rnd%>()" id="btnSyncStok<%=rnd%>" title="<%=Common.getBahasaConfig("Kalkulasi ulang stok & harga modal produk berdasarkan data ini")%>">
                            <i class="fas fa-sync me-1"></i><%=Common.getBahasaConfig("Sinkronkan Stok & Harga Modal")%>
                        </button>

                        <button class="btn btn-success rounded-pill px-3 fw-bold shadow-sm" onclick="downloadExcelPengadaan<%=rnd%>()" title="<%=Common.getBahasaConfig("Unduh rincian per-barang ke Excel")%>">
                            <i class="fas fa-file-download me-1"></i><%=Common.getBahasaConfig("Unduh")%>
                        </button>

                        <% if (bolehEntryKulakan) { %>
                        <button class="btn btn-primary rounded-pill px-4 fw-bold shadow-sm" onclick="bukaFormFaktur<%=rnd%>()">
                            <i class="fas fa-plus-circle me-2"></i><%=Common.getBahasaConfig("Tambah Faktur")%>
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
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Nomor Faktur")%></label>
                                <input type="text" class="form-control form-control-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterFaktur<%=rnd%>" placeholder="<%=Common.getBahasaConfig("No. Nota/Faktur")%>">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Nama Supplier")%></label>
                                <input type="text" class="form-control form-control-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterSupplier<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari supplier...")%>">
                            </div>

                            <% if (isAdmin) { %>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Toko / Pedagang")%> <span class="text-danger">*</span></label>
                                <select class="form-select form-select-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterToko<%=rnd%>">
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Toko --")%></option>
                                </select>
                            </div>
                            <% } else { %>
                                <input type="hidden" id="filterToko<%=rnd%>" value="<%=idTokoAktif%>">
                            <% } %>

                            <div class="col-md-auto ms-auto">
                                <button class="btn btn-sm btn-secondary fw-bold px-3 shadow-sm" onclick="triggerSearchFaktur<%=rnd%>()">
                                    <i class="fas fa-search me-1"></i> <%=Common.getBahasaConfig("Saring")%>
                                </button>
                            </div>
                        </div>
                        <% if (isAdmin) { %>
                        <small class="text-muted d-block mt-2"><i class="fas fa-info-circle me-1"></i><%=Common.getBahasaConfig("Pilih Toko dulu untuk melihat riwayat kulakan toko tersebut.")%></small>
                        <% } %>
                    </div>

                    <div class="table-responsive border rounded-3 shadow-sm bg-white">
                        <table class="table table-hover table-striped align-middle mb-0">
                            <thead class="table-dark text-center align-middle">
                                <tr>
                                    <th class="fw-semibold text-uppercase small py-3" style="width: 50px;">#</th>
                                    <th class="fw-semibold text-uppercase small text-start"><%=Common.getBahasaConfig("Tanggal Faktur")%></th>
                                    <th class="fw-semibold text-uppercase small text-start"><%=Common.getBahasaConfig("No. Faktur")%></th>
                                    <th class="fw-semibold text-uppercase small text-start"><%=Common.getBahasaConfig("Supplier")%></th>
                                    <th class="fw-semibold text-uppercase small"><%=Common.getBahasaConfig("Jml Item")%></th>
                                    <th class="fw-semibold text-uppercase small text-end"><%=Common.getBahasaConfig("Total Hitung")%></th>
                                    <th class="fw-semibold text-uppercase small text-end"><%=Common.getBahasaConfig("Diskon")%></th>
                                    <th class="fw-semibold text-uppercase small text-end"><%=Common.getBahasaConfig("Total Faktur")%></th>
                                    <th class="fw-semibold text-uppercase small text-center" style="width: 80px;"><%=Common.getBahasaConfig("Aksi")%></th>
                                </tr>
                            </thead>
                            <tbody id="tableFaktur<%=rnd%>">
                                <tr><td colspan="9" class="text-center py-5 text-muted"><%=Common.getBahasaConfig("Pilih Toko di atas untuk melihat riwayat.")%></td></tr>
                            </tbody>
                        </table>
                    </div>

                    <div class="d-flex justify-content-between align-items-center mt-4 pt-2">
                        <span class="text-muted small fw-medium bg-light px-3 py-1 rounded-pill border" id="pagingInfoFaktur<%=rnd%>"></span>
                        <nav>
                            <ul class="pagination pagination-sm mb-0 shadow-sm">
                                <li class="page-item">
                                    <button class="page-link text-primary rounded-start-pill px-3" id="btnPrevFaktur<%=rnd%>" onclick="changePageFaktur<%=rnd%>(-1)"><i class="fas fa-chevron-left"></i></button>
                                </li>
                                <li class="page-item disabled">
                                    <span class="page-link fw-bold bg-light text-dark px-3" id="pageNumberFaktur<%=rnd%>">1</span>
                                </li>
                                <li class="page-item">
                                    <button class="page-link text-primary rounded-end-pill px-3" id="btnNextFaktur<%=rnd%>" onclick="changePageFaktur<%=rnd%>(1)"><i class="fas fa-chevron-right"></i></button>
                                </li>
                            </ul>
                        </nav>
                    </div>

                </div>
            </div>
        </div>
    </div>
</div>

<!-- MODAL: Tambah Faktur (header sekali + multi-item) -->
<div class="modal fade" id="modalFormFaktur<%=rnd%>" tabindex="-1" data-bs-backdrop="static" aria-hidden="true">
    <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content border-0 rounded-4 shadow-lg">
            <div class="modal-header bg-light border-bottom-0 pb-3">
                <h5 class="modal-title fw-bold text-dark">
                    <i class="fas fa-file-invoice-dollar text-primary me-2"></i><%=Common.getBahasaConfig("Faktur Kulakan Baru")%>
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4 pt-2">

                <div class="row g-3">
                    <% if (isAdmin) { %>
                    <div class="col-md-3">
                        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Toko / Pedagang")%> <span class="text-danger">*</span></label>
                        <select class="form-select fw-semibold" id="formToko<%=rnd%>" onchange="gantiTokoFormFaktur<%=rnd%>()" required>
                            <option value=""><%=Common.getBahasaConfig("-- Pilih Toko --")%></option>
                        </select>
                    </div>
                    <% } else { %>
                        <input type="hidden" id="formToko<%=rnd%>" value="<%=idTokoAktif%>">
                    <% } %>

                    <div class="col-md-<%=isAdmin ? "3" : "4"%>">
                        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nomor Faktur / Nota")%> <span class="text-danger">*</span></label>
                        <input type="text" class="form-control fw-bold" id="formFaktur<%=rnd%>" placeholder="<%=Common.getBahasaConfig("mis. INV-2026-001")%>">
                    </div>

                    <div class="col-md-<%=isAdmin ? "3" : "4"%>">
                        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal Faktur")%> <span class="text-danger">*</span></label>
                        <input type="datetime-local" class="form-control" id="formTanggal<%=rnd%>">
                    </div>

                    <div class="col-md-<%=isAdmin ? "3" : "4"%>">
                        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Total Faktur (Manual, opsional)")%></label>
                        <div class="input-group">
                            <span class="input-group-text bg-light border-end-0">Rp</span>
                            <input type="number" class="form-control border-start-0" id="formTotalManual<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Kosongkan = pakai total hitung")%>" min="0">
                        </div>
                        <small class="text-muted" style="font-size: 10px;"><%=Common.getBahasaConfig("Selisih dari total hitung otomatis tercatat sbg diskon faktur.")%></small>
                    </div>

                    <div class="col-md-6">
                        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Supplier / Pemasok")%></label>
                        <div class="input-group shadow-sm">
                            <span class="input-group-text bg-white border-end-0"><i class="fas fa-building text-primary"></i></span>
                            <input type="text" class="form-control border-start-0 fw-bold" list="listSupplier<%=rnd%>" id="inputCariSupplier<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik nama supplier...")%>" autocomplete="off">
                            <button type="button" class="btn btn-outline-primary" onclick="toggleSupplierBaru<%=rnd%>()" title="<%=Common.getBahasaConfig("Supplier Baru")%>"><i class="fas fa-plus"></i></button>
                        </div>
                        <datalist id="listSupplier<%=rnd%>"></datalist>
                        <input type="hidden" id="idSupplierSelected<%=rnd%>" value="">

                        <div id="boxSupplierBaru<%=rnd%>" class="mt-2 p-3 bg-primary bg-opacity-10 rounded-3 border border-primary border-opacity-25" style="display:none;">
                            <div class="row g-2">
                                <div class="col-md-5">
                                    <input type="text" class="form-control form-control-sm" id="supBaruNama<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Nama Supplier *")%>">
                                </div>
                                <div class="col-md-3">
                                    <input type="text" class="form-control form-control-sm" id="supBaruKontak<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Kontak")%>">
                                </div>
                                <div class="col-md-3">
                                    <input type="text" class="form-control form-control-sm" id="supBaruTelp<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Telp")%>">
                                </div>
                                <div class="col-md-1">
                                    <button type="button" class="btn btn-sm btn-primary w-100" onclick="simpanSupplierBaru<%=rnd%>()" title="<%=Common.getBahasaConfig("Simpan")%>"><i class="fas fa-check"></i></button>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="col-md-6">
                        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Keterangan Faktur")%></label>
                        <input type="text" class="form-control" id="formKeteranganFaktur<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Opsional")%>">
                    </div>
                </div>

                <hr class="my-3">

                <h6 class="fw-bold text-dark mb-2"><i class="fas fa-boxes text-muted me-2"></i><%=Common.getBahasaConfig("Barang dalam Faktur ini")%></h6>

                <div class="bg-light p-3 rounded-3 border shadow-sm mb-3">
                    <div class="row g-2 align-items-end">
                        <div class="col-md-5">
                            <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Produk / Barang")%></label>
                            <div class="input-group shadow-sm">
                                <span class="input-group-text bg-white border-end-0"><i class="fas fa-box text-primary"></i></span>
                                <input type="text" class="form-control border-start-0 fw-bold" list="listProduk<%=rnd%>" id="inputCariProduk<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Pilih Toko dulu...")%>" autocomplete="off" disabled>
                            </div>
                            <datalist id="listProduk<%=rnd%>"></datalist>
                            <input type="hidden" id="idProdukSelected<%=rnd%>" value="">
                        </div>
                        <div class="col-md-2">
                            <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Qty")%></label>
                            <input type="number" class="form-control fw-bold" id="itemQty<%=rnd%>" placeholder="0" min="0.01" step="0.01">
                        </div>
                        <div class="col-md-2">
                            <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Harga Beli Satuan")%></label>
                            <input type="number" class="form-control" id="itemHarga<%=rnd%>" placeholder="0" min="0">
                        </div>
                        <div class="col-md-2">
                            <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Ket. (opsional)")%></label>
                            <input type="text" class="form-control" id="itemKeterangan<%=rnd%>" placeholder="-">
                        </div>
                        <div class="col-md-1">
                            <button type="button" class="btn btn-primary w-100 fw-bold" onclick="tambahItemFaktur<%=rnd%>()" title="<%=Common.getBahasaConfig("Tambah Barang")%>"><i class="fas fa-plus"></i></button>
                        </div>
                    </div>
                </div>

                <div class="table-responsive border rounded-3 shadow-sm bg-white mb-2">
                    <table class="table table-sm table-striped align-middle mb-0">
                        <thead class="table-light">
                            <tr>
                                <th class="text-start small"><%=Common.getBahasaConfig("Produk")%></th>
                                <th class="text-end small"><%=Common.getBahasaConfig("Qty")%></th>
                                <th class="text-end small"><%=Common.getBahasaConfig("Harga Satuan")%></th>
                                <th class="text-end small"><%=Common.getBahasaConfig("Subtotal")%></th>
                                <th class="text-start small"><%=Common.getBahasaConfig("Ket.")%></th>
                                <th style="width:40px;"></th>
                            </tr>
                        </thead>
                        <tbody id="tabelItemFaktur<%=rnd%>">
                            <tr><td colspan="6" class="text-center text-muted py-3 small"><%=Common.getBahasaConfig("Belum ada barang ditambahkan.")%></td></tr>
                        </tbody>
                        <tfoot>
                            <tr class="table-light">
                                <td colspan="3" class="text-end fw-bold"><%=Common.getBahasaConfig("Total Hitung")%></td>
                                <td class="text-end fw-bold text-success" id="totalHitungFaktur<%=rnd%>">Rp 0</td>
                                <td colspan="2"></td>
                            </tr>
                        </tfoot>
                    </table>
                </div>
            </div>
            <div class="modal-footer border-top-0 pt-0">
                <button type="button" class="btn btn-light px-4 rounded-pill fw-semibold" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
                <button type="button" class="btn btn-primary px-4 rounded-pill fw-bold shadow-sm" id="btnSimpanFaktur<%=rnd%>" onclick="simpanFaktur<%=rnd%>()">
                    <i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Faktur")%>
                </button>
            </div>
        </div>
    </div>
</div>

<!-- MODAL: Detail Faktur (read-only) -->
<div class="modal fade" id="modalDetailFaktur<%=rnd%>" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content border-0 rounded-4 shadow-lg">
            <div class="modal-header bg-light border-bottom-0 pb-3">
                <h5 class="modal-title fw-bold text-dark"><i class="fas fa-file-invoice text-primary me-2"></i><%=Common.getBahasaConfig("Detail Faktur")%></h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4 pt-2" id="bodyDetailFaktur<%=rnd%>">
                <div class="text-center py-5"><div class="spinner-border text-primary"></div></div>
            </div>
        </div>
    </div>
</div>

<script>
    // ==========================================
    // VARIABEL GLOBAL & UTILITIES
    // ==========================================
    const isAdminFaktur<%=rnd%> = <%=isAdmin%>;
    const bolehEntryFaktur<%=rnd%> = <%=bolehEntryKulakan%>;
    const idTokoLoginFaktur<%=rnd%> = '<%=idTokoAktif%>';

    let arrDataSupplierFaktur<%=rnd%> = [];
    let arrDataProdukFaktur<%=rnd%> = [];
    let itemsFaktur<%=rnd%> = [];
    let modalInstanceFaktur<%=rnd%> = null;
    let modalInstanceDetail<%=rnd%> = null;

    let currentPageFaktur<%=rnd%> = 1;
    const limitPerPageFaktur<%=rnd%> = 20;
    let totalRecordsFaktur<%=rnd%> = 0;

    const formatRpFaktur<%=rnd%> = (angka) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);

    const fetchDataFaktur<%=rnd%> = async (payload) => {
        const res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        return await res.json();
    };

    const fetchSqlFaktur<%=rnd%> = async (sql) => {
        try {
            const res = await fetchDataFaktur<%=rnd%>({ sql: sql, action: "sql" });
            return res.data || [];
        } catch (e) {
            console.error("Fetch Error: ", e);
            return [];
        }
    };

    const showToastFaktur<%=rnd%> = (msg, colorClass) => {
        if (typeof tampilkanToast === "function") { tampilkanToast(msg, colorClass); } else { alert(msg); }
    };

    // ==========================================
    // COMBO TOKO (filter riwayat + form faktur)
    // ==========================================
    const loadComboTokoFaktur<%=rnd%> = async () => {
        if (!isAdminFaktur<%=rnd%>) return;
        const rows = await fetchSqlFaktur<%=rnd%>("SELECT id, nama FROM koperasi.toko WHERE aktif = true ORDER BY nama ASC");
        let opt = '<option value=""><%=Common.getBahasaConfigJS("-- Pilih Toko --")%></option>';
        rows.forEach(r => { opt += '<option value="' + r.id + '">' + r.nama + '</option>'; });
        const elFilter = document.getElementById('filterToko<%=rnd%>');
        if (elFilter) elFilter.innerHTML = opt;
        const elForm = document.getElementById('formToko<%=rnd%>');
        if (elForm) elForm.innerHTML = opt;
    };

    // ==========================================
    // COMBO PRODUK (dinamis per Toko, dipakai form Tambah Barang)
    // ==========================================
    const loadComboProdukFaktur<%=rnd%> = async () => {
        const tokoId = isAdminFaktur<%=rnd%> ? document.getElementById('formToko<%=rnd%>').value : idTokoLoginFaktur<%=rnd%>;
        const input = document.getElementById('inputCariProduk<%=rnd%>');
        const dt = document.getElementById('listProduk<%=rnd%>');
        arrDataProdukFaktur<%=rnd%> = [];
        document.getElementById('idProdukSelected<%=rnd%>').value = '';
        if (!tokoId) {
            input.value = '';
            input.disabled = true;
            input.placeholder = '<%=Common.getBahasaConfigJS("Pilih Toko dulu...")%>';
            return;
        }
        input.disabled = true;
        input.placeholder = '<%=Common.getBahasaConfigJS("Memuat data...")%>';
        const rows = await fetchSqlFaktur<%=rnd%>("SELECT id, nama, kode FROM koperasi.produk WHERE aktif = true AND toko = " + tokoId + " AND (jenis_item IS NULL OR jenis_item = 'JUAL') ORDER BY nama ASC");
        let html = '';
        rows.forEach(item => {
            const kode = item.kode ? ' [' + item.kode + ']' : '';
            const namaLengkap = item.nama + kode;
            arrDataProdukFaktur<%=rnd%>.push({ id: item.id, nama_lengkap: namaLengkap });
            html += '<option value="' + namaLengkap + '">';
        });
        dt.innerHTML = html;
        input.value = '';
        input.disabled = false;
        input.placeholder = '<%=Common.getBahasaConfigJS("Ketik nama / kode produk...")%>';
    };

    const gantiTokoFormFaktur<%=rnd%> = () => {
        loadComboProdukFaktur<%=rnd%>();
    };

    document.getElementById('inputCariProduk<%=rnd%>').addEventListener('change', function () {
        const match = arrDataProdukFaktur<%=rnd%>.find(p => p.nama_lengkap === this.value);
        document.getElementById('idProdukSelected<%=rnd%>').value = match ? match.id : '';
    });

    // ==========================================
    // SUPPLIER (search debounced + quick-add)
    // ==========================================
    let timerCariSupplier<%=rnd%> = null;
    document.getElementById('inputCariSupplier<%=rnd%>').addEventListener('input', function () {
        document.getElementById('idSupplierSelected<%=rnd%>').value = '';
        const kw = this.value.trim();
        clearTimeout(timerCariSupplier<%=rnd%>);
        timerCariSupplier<%=rnd%> = setTimeout(async () => {
            const res = await fetchDataFaktur<%=rnd%>({ action: 'penyedia_list', keyword: kw });
            arrDataSupplierFaktur<%=rnd%> = res.data || [];
            let html = '';
            arrDataSupplierFaktur<%=rnd%>.forEach(s => { html += '<option value="' + s.nama + '">'; });
            document.getElementById('listSupplier<%=rnd%>').innerHTML = html;
        }, 300);
    });
    document.getElementById('inputCariSupplier<%=rnd%>').addEventListener('change', function () {
        const match = arrDataSupplierFaktur<%=rnd%>.find(s => s.nama === this.value);
        document.getElementById('idSupplierSelected<%=rnd%>').value = match ? match.id : '';
    });

    const toggleSupplierBaru<%=rnd%> = () => {
        const box = document.getElementById('boxSupplierBaru<%=rnd%>');
        box.style.display = box.style.display === 'none' ? 'block' : 'none';
    };

    const simpanSupplierBaru<%=rnd%> = async () => {
        const nama = document.getElementById('supBaruNama<%=rnd%>').value.trim();
        if (!nama) {
            showToastFaktur<%=rnd%>('<%=Common.getBahasaConfigJS("Nama supplier wajib diisi.")%>', 'bg-warning text-dark');
            return;
        }
        const payload = {
            action: 'penyedia_simpan',
            nama: nama,
            kontak: document.getElementById('supBaruKontak<%=rnd%>').value.trim(),
            telp: document.getElementById('supBaruTelp<%=rnd%>').value.trim()
        };
        const res = await fetchDataFaktur<%=rnd%>(payload);
        if (res.status === '00') {
            document.getElementById('inputCariSupplier<%=rnd%>').value = res.nama;
            document.getElementById('idSupplierSelected<%=rnd%>').value = res.id;
            document.getElementById('supBaruNama<%=rnd%>').value = '';
            document.getElementById('supBaruKontak<%=rnd%>').value = '';
            document.getElementById('supBaruTelp<%=rnd%>').value = '';
            document.getElementById('boxSupplierBaru<%=rnd%>').style.display = 'none';
            showToastFaktur<%=rnd%>('<%=Common.getBahasaConfigJS("Supplier baru tersimpan.")%>', 'bg-success text-white');
        } else {
            showToastFaktur<%=rnd%>(res.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan supplier.")%>', 'bg-danger text-white');
        }
    };

    // ==========================================
    // ITEM FAKTUR (lokal, sebelum Simpan Faktur)
    // ==========================================
    const renderItemFaktur<%=rnd%> = () => {
        const tbody = document.getElementById('tabelItemFaktur<%=rnd%>');
        if (itemsFaktur<%=rnd%>.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-3 small"><%=Common.getBahasaConfig("Belum ada barang ditambahkan.")%></td></tr>';
        } else {
            let html = '';
            let total = 0;
            itemsFaktur<%=rnd%>.forEach((it, idx) => {
                const subtotal = it.qty * it.hargaBeliSatuan;
                total += subtotal;
                html += '<tr>' +
                    '<td class="text-start fw-semibold">' + it.namaProduk + '</td>' +
                    '<td class="text-end">' + it.qty + '</td>' +
                    '<td class="text-end">' + formatRpFaktur<%=rnd%>(it.hargaBeliSatuan) + '</td>' +
                    '<td class="text-end fw-bold">' + formatRpFaktur<%=rnd%>(subtotal) + '</td>' +
                    '<td class="text-start text-muted small">' + (it.keterangan || '-') + '</td>' +
                    '<td class="text-center"><button type="button" class="btn btn-sm btn-outline-danger" onclick="hapusItemFaktur<%=rnd%>(' + idx + ')"><i class="fas fa-times"></i></button></td>' +
                    '</tr>';
            });
            tbody.innerHTML = html;
        }
        const total2 = itemsFaktur<%=rnd%>.reduce((a, it) => a + (it.qty * it.hargaBeliSatuan), 0);
        document.getElementById('totalHitungFaktur<%=rnd%>').textContent = formatRpFaktur<%=rnd%>(total2);
    };

    const tambahItemFaktur<%=rnd%> = () => {
        const produkId = document.getElementById('idProdukSelected<%=rnd%>').value;
        const namaProduk = document.getElementById('inputCariProduk<%=rnd%>').value.trim();
        const qty = parseFloat(document.getElementById('itemQty<%=rnd%>').value) || 0;
        const harga = parseFloat(document.getElementById('itemHarga<%=rnd%>').value) || 0;
        const ket = document.getElementById('itemKeterangan<%=rnd%>').value.trim();

        if (!produkId) {
            showToastFaktur<%=rnd%>('<%=Common.getBahasaConfigJS("Pilih produk yang valid dari daftar terlebih dahulu.")%>', 'bg-warning text-dark');
            return;
        }
        if (qty <= 0) {
            showToastFaktur<%=rnd%>('<%=Common.getBahasaConfigJS("Jumlah (qty) harus lebih dari 0.")%>', 'bg-warning text-dark');
            return;
        }
        if (harga <= 0) {
            showToastFaktur<%=rnd%>('<%=Common.getBahasaConfigJS("Harga beli satuan harus lebih dari 0.")%>', 'bg-warning text-dark');
            return;
        }

        itemsFaktur<%=rnd%>.push({ produkId: produkId, namaProduk: namaProduk, qty: qty, hargaBeliSatuan: harga, keterangan: ket });
        renderItemFaktur<%=rnd%>();

        document.getElementById('inputCariProduk<%=rnd%>').value = '';
        document.getElementById('idProdukSelected<%=rnd%>').value = '';
        document.getElementById('itemQty<%=rnd%>').value = '';
        document.getElementById('itemHarga<%=rnd%>').value = '';
        document.getElementById('itemKeterangan<%=rnd%>').value = '';
        document.getElementById('inputCariProduk<%=rnd%>').focus();
    };

    const hapusItemFaktur<%=rnd%> = (idx) => {
        itemsFaktur<%=rnd%>.splice(idx, 1);
        renderItemFaktur<%=rnd%>();
    };

    // ==========================================
    // BUKA FORM / SIMPAN FAKTUR
    // ==========================================
    const bukaFormFaktur<%=rnd%> = () => {
        document.getElementById('formFaktur<%=rnd%>').value = '';
        document.getElementById('formTotalManual<%=rnd%>').value = '';
        document.getElementById('formKeteranganFaktur<%=rnd%>').value = '';
        document.getElementById('inputCariSupplier<%=rnd%>').value = '';
        document.getElementById('idSupplierSelected<%=rnd%>').value = '';
        document.getElementById('boxSupplierBaru<%=rnd%>').style.display = 'none';
        itemsFaktur<%=rnd%> = [];
        renderItemFaktur<%=rnd%>();

        const now = new Date();
        now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
        document.getElementById('formTanggal<%=rnd%>').value = now.toISOString().slice(0, 16);

        if (isAdminFaktur<%=rnd%>) {
            document.getElementById('formToko<%=rnd%>').value = '';
            document.getElementById('inputCariProduk<%=rnd%>').disabled = true;
            document.getElementById('inputCariProduk<%=rnd%>').placeholder = '<%=Common.getBahasaConfigJS("Pilih Toko dulu...")%>';
        } else {
            loadComboProdukFaktur<%=rnd%>();
        }

        if (modalInstanceFaktur<%=rnd%> === null) {
            modalInstanceFaktur<%=rnd%> = new bootstrap.Modal(document.getElementById('modalFormFaktur<%=rnd%>'));
        }
        modalInstanceFaktur<%=rnd%>.show();
    };

    const simpanFaktur<%=rnd%> = async () => {
        const tokoId = isAdminFaktur<%=rnd%> ? document.getElementById('formToko<%=rnd%>').value : idTokoLoginFaktur<%=rnd%>;
        const nomorFaktur = document.getElementById('formFaktur<%=rnd%>').value.trim();
        const tanggalFaktur = document.getElementById('formTanggal<%=rnd%>').value;
        const totalManualStr = document.getElementById('formTotalManual<%=rnd%>').value;
        const supplierId = document.getElementById('idSupplierSelected<%=rnd%>').value;
        const keterangan = document.getElementById('formKeteranganFaktur<%=rnd%>').value.trim();

        if (!tokoId) {
            showToastFaktur<%=rnd%>('<%=Common.getBahasaConfigJS("Toko wajib dipilih.")%>', 'bg-warning text-dark');
            return;
        }
        if (!nomorFaktur) {
            showToastFaktur<%=rnd%>('<%=Common.getBahasaConfigJS("Nomor Faktur wajib diisi.")%>', 'bg-warning text-dark');
            return;
        }
        if (itemsFaktur<%=rnd%>.length === 0) {
            showToastFaktur<%=rnd%>('<%=Common.getBahasaConfigJS("Tambahkan minimal satu barang ke faktur ini.")%>', 'bg-warning text-dark');
            return;
        }

        const btn = document.getElementById('btnSimpanFaktur<%=rnd%>');
        const asliBtn = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyimpan...")%>';

        const payload = {
            action: 'kulakan_faktur_simpan',
            toko_id: tokoId,
            nomor_faktur: nomorFaktur,
            tanggal_faktur: tanggalFaktur,
            keterangan: keterangan,
            items: itemsFaktur<%=rnd%>.map(it => ({
                produk_id: it.produkId,
                qty: it.qty,
                harga_beli_satuan: it.hargaBeliSatuan,
                keterangan: it.keterangan
            }))
        };
        if (supplierId) payload.supplier_id = supplierId;
        if (totalManualStr !== '') payload.total_faktur_manual = parseFloat(totalManualStr);

        try {
            const res = await fetchDataFaktur<%=rnd%>(payload);
            if (res.status === '00') {
                showToastFaktur<%=rnd%>('<%=Common.getBahasaConfigJS("Faktur berhasil disimpan.")%>', 'bg-success text-white');
                modalInstanceFaktur<%=rnd%>.hide();
                loadDataFakturAPI<%=rnd%>();
            } else {
                showToastFaktur<%=rnd%>(res.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan faktur.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            showToastFaktur<%=rnd%>('<%=Common.getBahasaConfigJS("Koneksi gagal. Silakan coba lagi.")%>', 'bg-danger text-white');
        } finally {
            btn.disabled = false;
            btn.innerHTML = asliBtn;
        }
    };

    // ==========================================
    // RIWAYAT (per Faktur)
    // ==========================================
    const triggerSearchFaktur<%=rnd%> = () => {
        currentPageFaktur<%=rnd%> = 1;
        loadDataFakturAPI<%=rnd%>();
    };

    const loadDataFakturAPI<%=rnd%> = async () => {
        const tbody = document.getElementById('tableFaktur<%=rnd%>');
        const tokoId = document.getElementById('filterToko<%=rnd%>').value;
        if (!tokoId) {
            tbody.innerHTML = '<tr><td colspan="9" class="text-center py-5 text-muted"><%=Common.getBahasaConfig("Pilih Toko di atas untuk melihat riwayat.")%></td></tr>';
            document.getElementById('pagingInfoFaktur<%=rnd%>').innerHTML = '';
            return;
        }
        tbody.innerHTML = '<tr><td colspan="9" class="text-center py-5 text-muted"><div class="spinner-border text-primary mb-3"></div><br><%=Common.getBahasaConfig("Memuat riwayat...")%></td></tr>';

        const faktur = document.getElementById('filterFaktur<%=rnd%>').value.trim();
        const supplier = document.getElementById('filterSupplier<%=rnd%>').value.trim();
        const keyword = (faktur || supplier);

        const payload = {
            action: 'kulakan_faktur_list',
            toko_id: tokoId,
            keyword: keyword,
            page: currentPageFaktur<%=rnd%>,
            page_size: limitPerPageFaktur<%=rnd%>
        };

        try {
            const res = await fetchDataFaktur<%=rnd%>(payload);
            const rows = res.data || [];
            totalRecordsFaktur<%=rnd%> = res.total || 0;

            if (rows.length === 0) {
                tbody.innerHTML = '<tr><td colspan="9" class="text-center text-muted py-5"><i class="fas fa-truck-loading fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Belum ada data faktur.")%></td></tr>';
            } else {
                let no = (currentPageFaktur<%=rnd%> - 1) * limitPerPageFaktur<%=rnd%> + 1;
                let html = '';
                rows.forEach(r => {
                    html += '<tr>' +
                        '<td class="text-center text-muted">' + no++ + '</td>' +
                        '<td class="fw-medium text-secondary">' + (r.tanggalFaktur || '-') + '</td>' +
                        '<td class="fw-bold">' + (r.nomorFaktur || '-') + '</td>' +
                        '<td class="text-dark">' + (r.namaSupplier || '-') + '</td>' +
                        '<td class="text-center fw-bold">' + (r.jumlahItem || 0) + '</td>' +
                        '<td class="text-end text-muted">' + formatRpFaktur<%=rnd%>(r.totalHitung) + '</td>' +
                        '<td class="text-end text-danger">' + (r.diskon > 0 ? formatRpFaktur<%=rnd%>(r.diskon) : '-') + '</td>' +
                        '<td class="text-end fw-bold text-success">' + formatRpFaktur<%=rnd%>(r.totalFakturManual != null ? r.totalFakturManual : r.totalHitung) + '</td>' +
                        '<td class="text-center"><button class="btn btn-sm btn-outline-primary shadow-sm" onclick="bukaDetailFaktur<%=rnd%>(' + r.fakturId + ')" title="<%=Common.getBahasaConfig("Lihat Detail")%>"><i class="fas fa-eye"></i></button></td>' +
                        '</tr>';
                });
                tbody.innerHTML = html;
            }
            updatePaginationFaktur<%=rnd%>();
        } catch (error) {
            console.error(error);
            tbody.innerHTML = '<tr><td colspan="9" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal mengambil data.")%></td></tr>';
        }
    };

    const updatePaginationFaktur<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecordsFaktur<%=rnd%> / limitPerPageFaktur<%=rnd%>) || 1;
        document.getElementById('pageNumberFaktur<%=rnd%>').innerText = currentPageFaktur<%=rnd%> + " / " + totalPages;

        const startIdx = (currentPageFaktur<%=rnd%> - 1) * limitPerPageFaktur<%=rnd%> + 1;
        const endIdx = Math.min(currentPageFaktur<%=rnd%> * limitPerPageFaktur<%=rnd%>, totalRecordsFaktur<%=rnd%>);
        const startInfo = totalRecordsFaktur<%=rnd%> > 0 ? startIdx : 0;

        document.getElementById('pagingInfoFaktur<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startInfo + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari")%> <b class="text-dark">' + totalRecordsFaktur<%=rnd%> + '</b> <%=Common.getBahasaConfig("data")%>';

        document.getElementById('btnPrevFaktur<%=rnd%>').disabled = (currentPageFaktur<%=rnd%> === 1);
        document.getElementById('btnNextFaktur<%=rnd%>').disabled = (currentPageFaktur<%=rnd%> === totalPages);
    };

    const changePageFaktur<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecordsFaktur<%=rnd%> / limitPerPageFaktur<%=rnd%>);
        if (direction === -1 && currentPageFaktur<%=rnd%> > 1) {
            currentPageFaktur<%=rnd%>--;
            loadDataFakturAPI<%=rnd%>();
        } else if (direction === 1 && currentPageFaktur<%=rnd%> < totalPages) {
            currentPageFaktur<%=rnd%>++;
            loadDataFakturAPI<%=rnd%>();
        }
    };

    // ==========================================
    // DETAIL FAKTUR (read-only)
    // ==========================================
    const bukaDetailFaktur<%=rnd%> = async (fakturId) => {
        if (modalInstanceDetail<%=rnd%> === null) {
            modalInstanceDetail<%=rnd%> = new bootstrap.Modal(document.getElementById('modalDetailFaktur<%=rnd%>'));
        }
        const body = document.getElementById('bodyDetailFaktur<%=rnd%>');
        body.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-primary"></div></div>';
        modalInstanceDetail<%=rnd%>.show();

        const res = await fetchDataFaktur<%=rnd%>({ action: 'kulakan_faktur_detail', faktur_id: fakturId });
        if (res.status !== '00') {
            body.innerHTML = '<div class="text-center text-danger py-5">' + (res.description || '<%=Common.getBahasaConfigJS("Gagal memuat detail faktur.")%>') + '</div>';
            return;
        }
        const h = res.header || {};
        const items = res.items || [];

        let html = '<div class="row g-2 mb-3 small">';
        html += '<div class="col-md-6"><b><%=Common.getBahasaConfigJS("No. Faktur")%>:</b> ' + (h.nomorFaktur || '-') + '</div>';
        html += '<div class="col-md-6"><b><%=Common.getBahasaConfigJS("Tanggal")%>:</b> ' + (h.tanggalFaktur || '-').replace('T', ' ') + '</div>';
        html += '<div class="col-md-6"><b><%=Common.getBahasaConfigJS("Supplier")%>:</b> ' + (h.namaSupplier || '-') + '</div>';
        html += '<div class="col-md-6"><b><%=Common.getBahasaConfigJS("Keterangan")%>:</b> ' + (h.keterangan || '-') + '</div>';
        html += '</div>';

        html += '<div class="table-responsive border rounded-3"><table class="table table-sm table-striped mb-0">';
        html += '<thead class="table-light"><tr><th class="text-start small"><%=Common.getBahasaConfigJS("Produk")%></th><th class="text-end small"><%=Common.getBahasaConfigJS("Qty")%></th><th class="text-end small"><%=Common.getBahasaConfigJS("Harga Satuan")%></th><th class="text-end small"><%=Common.getBahasaConfigJS("Subtotal")%></th></tr></thead><tbody>';
        items.forEach(it => {
            html += '<tr><td class="text-start">' + (it.namaProduk || '-') + '</td><td class="text-end">' + it.qty + '</td><td class="text-end">' + formatRpFaktur<%=rnd%>(it.hargaBeliSatuan) + '</td><td class="text-end fw-bold">' + formatRpFaktur<%=rnd%>(it.totalHarga) + '</td></tr>';
        });
        html += '</tbody></table></div>';

        html += '<div class="row g-2 mt-3 small">';
        html += '<div class="col-12 text-end"><b><%=Common.getBahasaConfigJS("Total Hitung")%>:</b> ' + formatRpFaktur<%=rnd%>(h.totalHitung) + '</div>';
        if (h.diskon > 0) {
            html += '<div class="col-12 text-end text-danger"><b><%=Common.getBahasaConfigJS("Diskon")%>:</b> -' + formatRpFaktur<%=rnd%>(h.diskon) + '</div>';
        }
        html += '<div class="col-12 text-end fs-5"><b><%=Common.getBahasaConfigJS("Total Faktur")%>:</b> <span class="text-success fw-bold">' + formatRpFaktur<%=rnd%>(h.totalFakturFinal) + '</span></div>';
        html += '</div>';

        body.innerHTML = html;
    };

    // ==========================================
    // SINKRONISASI STOK DAN HARGA (via server, bukan raw SQL lagi)
    // ==========================================
    const sinkronisasiStok<%=rnd%> = async () => {
        const tokoId = document.getElementById('filterToko<%=rnd%>').value;
        if (!tokoId) {
            showToastFaktur<%=rnd%>('<%=Common.getBahasaConfigJS("Pilih Toko di atas terlebih dahulu.")%>', 'bg-warning text-dark');
            return;
        }
        const isConfirm = confirm('<%=Common.getBahasaConfigJS("Proses ini akan mengkalkulasi ulang seluruh stok produk toko ini, memperbarui harga beli terakhir, dan MENGKOREKSI harga jual jika ternyata lebih rendah dari modal. Lanjutkan?")%>');
        if (!isConfirm) return;

        const btnSync = document.getElementById('btnSyncStok<%=rnd%>');
        const originalBtnHtml = btnSync.innerHTML;
        btnSync.disabled = true;
        btnSync.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyinkronkan...")%>';

        try {
            const result = await fetchDataFaktur<%=rnd%>({ action: 'sinkron_stok_toko', toko_id: tokoId });
            if (result.status === '00') {
                showToastFaktur<%=rnd%>('<%=Common.getBahasaConfigJS("Sinkronisasi berhasil, ")%>' + (result.jumlahProduk || 0) + '<%=Common.getBahasaConfigJS(" produk diperbarui.")%>', 'bg-success text-white');
                loadDataFakturAPI<%=rnd%>();
            } else {
                showToastFaktur<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal melakukan sinkronisasi.")%>', 'bg-danger text-white');
            }
        } catch (error) {
            console.error("Sinkronisasi Error: ", error);
            showToastFaktur<%=rnd%>('<%=Common.getBahasaConfigJS("Koneksi terputus saat sinkronisasi.")%>', 'bg-danger text-white');
        } finally {
            btnSync.innerHTML = originalBtnHtml;
            btnSync.disabled = false;
        }
    };

    // ==========================================
    // UNDUH EXCEL (rincian per-barang, tetap dari pengadaan_produk mentah)
    // ==========================================
    const downloadExcelPengadaan<%=rnd%> = async () => {
        const tokoId = document.getElementById('filterToko<%=rnd%>').value;
        if (!tokoId) {
            showToastFaktur<%=rnd%>('<%=Common.getBahasaConfigJS("Pilih Toko di atas terlebih dahulu.")%>', 'bg-warning text-dark');
            return;
        }
        showToastFaktur<%=rnd%>('<%=Common.getBahasaConfigJS("Sedang menyiapkan file Excel, mohon tunggu...")%>', 'bg-info text-dark');

        const faktur = document.getElementById('filterFaktur<%=rnd%>').value.trim().replace(/'/g, "''");
        const supplier = document.getElementById('filterSupplier<%=rnd%>').value.trim().replace(/'/g, "''");
        let filters = " WHERE a.toko = " + tokoId;
        if (faktur !== "") filters += " AND a.nomorfaktur ILIKE '%" + faktur + "%' ";
        if (supplier !== "") filters += " AND a.namasupplier ILIKE '%" + supplier + "%' ";

        // Catatan: a.namasupplier (snapshot teks) dipakai LANGSUNG, BUKAN join ke tabel supplier --
        // PengadaanProduk.supplier adalah FK LAMA ke asset.penyedia_asset (tak pernah diisi lagi oleh
        // kulakanFakturSimpan sejak restrukturisasi per-Faktur), sedang supplier "benar" yg dipilih
        // di form sekarang (library.Penyedia) hanya tersimpan di header PengadaanFaktur.supplier, tak
        // ada FK langsung di baris ini -- namasupplier adalah satu-satunya sumber yg selalu akurat
        // baik utk baris lama maupun baru.
        const sqlQuery = "SELECT TO_CHAR(a.waktupengadaan, 'YYYY-MM-DD HH24:MI:SS') as waktu, " +
                         "a.nomorfaktur, a.namasupplier, p.nama AS nama_produk, p.kode AS kode_produk, " +
                         "a.qty, a.hargabelisatuan, a.totalharga, a.keterangan " +
                         "FROM koperasi.pengadaan_produk a " +
                         "LEFT JOIN koperasi.produk p ON a.produk = p.id " +
                         filters + " ORDER BY a.waktupengadaan DESC;";

        try {
            const result = await fetchSqlFaktur<%=rnd%>(sqlQuery);
            if (!result || result.length === 0) {
                showToastFaktur<%=rnd%>('<%=Common.getBahasaConfigJS("Tidak ada data untuk diunduh.")%>', 'bg-warning text-dark');
                return;
            }

            let tableHtml = '<html xmlns:x="urn:schemas-microsoft-com:office:excel">';
            tableHtml += '<head><meta charset="UTF-8"><style>th { background-color: #f8f9fa; font-weight: bold; border: 1px solid black; } td { border: 1px solid black; }</style></head><body>';
            tableHtml += '<table><thead><tr>';
            ['Waktu', 'No. Faktur', 'Supplier', 'Produk', 'Kode', 'Qty', 'Harga Beli Satuan', 'Total Harga', 'Keterangan'].forEach(h => { tableHtml += '<th>' + h + '</th>'; });
            tableHtml += '</tr></thead><tbody>';

            result.forEach(r => {
                tableHtml += '<tr>';
                tableHtml += '<td>' + (r.waktu || '') + '</td>';
                tableHtml += '<td>' + (r.nomorfaktur || '') + '</td>';
                tableHtml += '<td>' + (r.namasupplier || '') + '</td>';
                tableHtml += '<td>' + (r.nama_produk || '') + '</td>';
                tableHtml += '<td>' + (r.kode_produk || '') + '</td>';
                tableHtml += '<td>' + (r.qty || 0) + '</td>';
                tableHtml += '<td>' + (r.hargabelisatuan || 0) + '</td>';
                tableHtml += '<td>' + (r.totalharga || 0) + '</td>';
                tableHtml += '<td>' + (r.keterangan || '') + '</td>';
                tableHtml += '</tr>';
            });
            tableHtml += '</tbody></table><jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp"/>
</body></html>';

            const blob = new Blob([tableHtml], { type: 'application/vnd.ms-excel' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement("a");
            const timestamp = new Date().toISOString().slice(0, 10).replace(/-/g, "");
            link.download = "Rincian_Kulakan_" + timestamp + ".xls";
            link.href = url;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            setTimeout(() => URL.revokeObjectURL(url), 100);
        } catch (error) {
            console.error(error);
            showToastFaktur<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal mengunduh data.")%>', 'bg-danger text-white');
        }
    };

    // ==========================================
    // INISIALISASI HALAMAN
    // ==========================================
    document.addEventListener("DOMContentLoaded", () => {
        loadComboTokoFaktur<%=rnd%>();

        const filterInputs = document.querySelectorAll('.filter-input-<%=rnd%>');
        filterInputs.forEach(input => {
            if (input.tagName === 'SELECT') {
                input.addEventListener("change", () => triggerSearchFaktur<%=rnd%>());
            } else {
                input.addEventListener("keydown", (event) => {
                    if (event.key === "Enter") {
                        event.preventDefault();
                        triggerSearchFaktur<%=rnd%>();
                    }
                });
            }
        });

        if (!isAdminFaktur<%=rnd%>) {
            loadDataFakturAPI<%=rnd%>();
        }
    });
</script>
