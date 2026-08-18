<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="java.net.URLEncoder"%>
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
Pedagang pedagang = tbmuser.getPedagang();
Toko toko = pedagang == null ? null : pedagang.getToko();
String rnd = Common.getGeneratedBarCode(7);

boolean isAdmin = (toko == null);
String idTokoAktif = toko != null ? String.valueOf(toko.getId()) : "";
String namaTokoAktif = toko != null ? toko.getNama() : "";
String userIdLogin = tbmuser.getUserId();
%>

<style>
    body { background-color: #f4f7f6; }
    .table-custom-<%=rnd%> th { background-color: #f8f9fa; font-size: 13px; text-transform: uppercase; color: #6c757d; }
</style>

<div class="row mb-4 animate__animated animate__fadeInUp" id="viewList<%=rnd%>">
    <div class="col-12">
        <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
            
            <div class="card-header bg-white border-0 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-3 border-bottom border-light">
                <div>
                    <h5 class="fw-bold text-dark mb-1">
                        <i class="fas fa-tags text-primary me-2"></i>
                        <% if (isAdmin) { %>
                            <%=Common.getBahasaConfig("Pengajuan Perubahan Harga (Semua Toko)")%>
                        <% } else { %>
                            <%=Common.getBahasaConfig("Pengajuan Perubahan Harga - ")%> <span class="text-primary"><%=namaTokoAktif%></span>
                        <% } %>
                    </h5>
                    <small class="text-muted"><%=Common.getBahasaConfig("Kelola permintaan perubahan harga modal maupun harga jual produk.")%></small>
                </div>
                <div class="d-flex flex-wrap gap-2">
                    <button class="btn btn-light border text-primary rounded-pill px-3 fw-bold shadow-sm" onclick="triggerSearchPengajuan<%=rnd%>()" title="<%=Common.getBahasaConfig("Segarkan Data")%>">
                        <i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Segarkan")%>
                    </button>
                    
                    <% if (isAdmin) { %>
                    <button class="btn btn-info text-white rounded-pill px-3 fw-bold shadow-sm" onclick="sinkronisasiHarga<%=rnd%>()" id="btnSyncHarga<%=rnd%>" title="<%=Common.getBahasaConfig("Sinkronkan harga produk dengan data pengajuan terakhir yang disetujui")%>">
                        <i class="fas fa-sync me-1"></i><%=Common.getBahasaConfig("Sinkronkan Persetujuan")%>
                    </button>
                    <% } %>

                    <button class="btn btn-primary rounded-pill px-4 fw-bold shadow-sm" onclick="showAddModal<%=rnd%>()">
                        <i class="fas fa-plus-circle me-2"></i><%=Common.getBahasaConfig("Buat Pengajuan")%>
                    </button>
                </div>
            </div>

            <div class="card-body p-4">
                
                <!-- Filter Section -->
                <div class="bg-light p-3 rounded-3 border shadow-sm mb-4">
                    <div class="row g-2 align-items-end">
                        <div class="col-md-3">
                            <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Mulai Tanggal")%></label>
                            <input type="date" class="form-control form-control-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterStart<%=rnd%>">
                        </div>
                        <div class="col-md-3">
                            <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Sampai Tanggal")%></label>
                            <input type="date" class="form-control form-control-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterEnd<%=rnd%>">
                        </div>
                        <div class="col-md-3">
                            <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Nama Produk")%></label>
                            <input type="text" class="form-control form-control-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterNama<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari nama...")%>">
                        </div>
                        <div class="col-md-3">
                            <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Status Persetujuan")%></label>
                            <select class="form-select form-select-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterStatus<%=rnd%>">
                                <option value=""><%=Common.getBahasaConfig("- Semua Status -")%></option>
                                <option value="PENDING"><%=Common.getBahasaConfig("Menunggu Persetujuan")%></option>
                                <option value="APPROVED"><%=Common.getBahasaConfig("Telah Disetujui")%></option>
                            </select>
                        </div>
                        
                        <div class="col-md-12 d-flex justify-content-end mt-3">
                            <button class="btn btn-sm btn-secondary fw-bold px-4 shadow-sm" onclick="triggerSearchPengajuan<%=rnd%>()">
                                <i class="fas fa-filter me-1"></i> <%=Common.getBahasaConfig("Terapkan Filter")%>
                            </button>
                        </div>
                    </div>
                </div>

                <!-- Table Section -->
                <div class="table-responsive border rounded-3 shadow-sm bg-white">
                    <table class="table table-hover align-middle mb-0 table-custom-<%=rnd%>">
                        <thead class="table-light text-center align-middle border-bottom">
                            <tr>
                                <th class="fw-semibold text-uppercase small py-3" style="width: 50px;">#</th>
                                <th class="fw-semibold text-uppercase small"><%=Common.getBahasaConfig("Waktu Pengajuan")%></th>
                                <th class="fw-semibold text-uppercase small text-start"><%=Common.getBahasaConfig("Produk")%></th>
                                <% if (isAdmin) { %>
                                    <th class="fw-semibold text-uppercase small text-start"><%=Common.getBahasaConfig("Toko")%></th>
                                <% } %>
                                <th class="fw-semibold text-uppercase small text-end"><%=Common.getBahasaConfig("Harga Lama")%></th>
                                <th class="fw-semibold text-uppercase small text-end"><%=Common.getBahasaConfig("Harga Baru")%></th>
                                <th class="fw-semibold text-uppercase small text-start"><%=Common.getBahasaConfig("Alasan")%></th>
                                <th class="fw-semibold text-uppercase small"><%=Common.getBahasaConfig("Status")%></th>
                                <th class="fw-semibold text-uppercase small text-center" style="width: 140px;"><%=Common.getBahasaConfig("Aksi")%></th>
                            </tr>
                        </thead>
                        <tbody id="tablePengajuan<%=rnd%>">
                        </tbody>
                    </table>
                </div>

                <!-- Pagination -->
                <div class="d-flex justify-content-between align-items-center mt-4 pt-2">
                    <span class="text-muted small fw-medium bg-light px-3 py-1 rounded-pill border" id="pagingInfoPengajuan<%=rnd%>"></span>
                    <nav>
                        <ul class="pagination pagination-sm mb-0 shadow-sm">
                            <li class="page-item">
                                <button class="page-link text-primary rounded-start-pill px-3" id="btnPrevPengajuan<%=rnd%>" onclick="changePagePengajuan<%=rnd%>(-1)"><i class="fas fa-chevron-left"></i></button>
                            </li>
                            <li class="page-item disabled">
                                <span class="page-link fw-bold bg-light text-dark px-3" id="pageNumberPengajuan<%=rnd%>">1</span>
                            </li>
                            <li class="page-item">
                                <button class="page-link text-primary rounded-end-pill px-3" id="btnNextPengajuan<%=rnd%>" onclick="changePagePengajuan<%=rnd%>(1)"><i class="fas fa-chevron-right"></i></button>
                            </li>
                        </ul>
                    </nav>
                </div>

            </div>
        </div>
    </div>
</div>

<!-- Modal Form Pengajuan -->
<div class="modal fade" id="modalFormPengajuan<%=rnd%>" tabindex="-1" data-bs-backdrop="static" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 rounded-4 shadow-lg">
            <div class="modal-header bg-light border-bottom-0 pb-3">
                <h5 class="modal-title fw-bold text-dark" id="modalTitlePengajuan<%=rnd%>">
                    <i class="fas fa-edit text-primary me-2"></i><%=Common.getBahasaConfig("Buat Pengajuan Harga")%>
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4 pt-2">
                <form id="formPengajuan<%=rnd%>" onsubmit="event.preventDefault(); savePengajuan<%=rnd%>();">
                    <input type="hidden" id="formId<%=rnd%>" value="">
                    
                    <div class="row g-3">
                        <div class="col-md-12">
                            <label class="form-label small fw-bold text-primary mb-1"><i class="fas fa-search me-1"></i><%=Common.getBahasaConfig("Pilih Produk")%> <span class="text-danger">*</span></label>
                            <div class="input-group shadow-sm">
                                <input type="text" class="form-control fw-bold border-primary" list="listProdukTarget<%=rnd%>" id="inputCariProduk<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik nama produk untuk mencari...")%>" autocomplete="off" required>
                                <span class="input-group-text bg-white border-primary"><i class="fas fa-box"></i></span>
                            </div>
                            <datalist id="listProdukTarget<%=rnd%>"></datalist>
                            <input type="hidden" id="idProdukSelected<%=rnd%>" value="">
                        </div>

                        <!-- Panel Harga Lama -->
                        <div class="col-12 mt-3">
                            <div class="bg-light p-3 rounded-3 border shadow-sm">
                                <h6 class="fw-bold text-secondary mb-3 border-bottom pb-2"><%=Common.getBahasaConfig("Informasi Harga Saat Ini")%></h6>
                                <div class="row">
                                    <div class="col-6">
                                        <label class="form-label small fw-semibold text-muted mb-0"><%=Common.getBahasaConfig("Harga Beli / Modal")%></label>
                                        <h6 class="fw-bold text-dark mb-0" id="lblHargaBeliLama<%=rnd%>">Rp 0</h6>
                                    </div>
                                    <div class="col-6 text-end">
                                        <label class="form-label small fw-semibold text-muted mb-0"><%=Common.getBahasaConfig("Harga Jual")%></label>
                                        <h6 class="fw-bold text-dark mb-0" id="lblHargaJualLama<%=rnd%>">Rp 0</h6>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Panel Harga Baru -->
                        <div class="col-md-6 mt-3">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Ajukan Harga Beli Baru")%> <span class="text-danger">*</span></label>
                            <div class="input-group shadow-sm">
                                <span class="input-group-text bg-light border-end-0">Rp</span>
                                <input type="number" class="form-control border-start-0 fw-bold" id="formHargaBeliBaru<%=rnd%>" required placeholder="0" min="0">
                            </div>
                        </div>

                        <div class="col-md-6 mt-3">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Ajukan Harga Jual Baru")%> <span class="text-danger">*</span></label>
                            <div class="input-group shadow-sm">
                                <span class="input-group-text bg-light border-end-0">Rp</span>
                                <input type="number" class="form-control border-start-0 text-success fw-bold" id="formHargaJualBaru<%=rnd%>" required placeholder="0" min="0">
                            </div>
                        </div>

                        <div class="col-12 mt-3">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Alasan Perubahan")%> <span class="text-danger">*</span></label>
                            <textarea class="form-control shadow-sm" id="formAlasan<%=rnd%>" rows="3" required placeholder="<%=Common.getBahasaConfig("Jelaskan mengapa harga produk ini perlu diubah (Misal: Harga distributor naik).")%>"></textarea>
                        </div>
                    </div>

                    <div class="mt-4 text-end border-top pt-3">
                        <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold shadow-sm border" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
                        <button type="submit" class="btn btn-primary px-4 rounded-pill fw-bold shadow-sm" id="btnSavePengajuan<%=rnd%>">
                            <i class="fas fa-paper-plane me-2"></i><%=Common.getBahasaConfig("Ajukan Perubahan")%>
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<script>
    // ==========================================
    // VARIABEL GLOBAL & UTILITIES
    // ==========================================
    const masterModelClass<%=rnd%> = "ais.database.model.inventory.PengajuanPerubahanHargaProduk";
    const isAdminPengajuan<%=rnd%> = <%=isAdmin%>;
    const idTokoLogin<%=rnd%> = '<%=idTokoAktif%>';
    const idUserLogin<%=rnd%> = '<%=userIdLogin%>';
    let modalInstancePengajuan<%=rnd%> = null;

    let currentPagePengajuan<%=rnd%> = 1;
    const limitPerPagePengajuan<%=rnd%> = 10;
    let totalRecordsPengajuan<%=rnd%> = 0;
    
    let arrDataProduk<%=rnd%> = []; // Untuk cache Datalist

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

    const fetchUpdateData<%=rnd%> = async (sqlString) => {
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: "update_data", sql: sqlString })
            });
            return await res.json();
        } catch (e) {
            return { status: 'error' };
        }
    };

    const showToast<%=rnd%> = (msg, colorClass) => {
        if(typeof tampilkanToast === "function") {
            tampilkanToast(msg, colorClass);
        } else {
            alert(msg);
        }
    };

    // ==========================================
    // INIT COMBO BOX (DATA PRODUK)
    // ==========================================
    const loadProdukCombo<%=rnd%> = async () => {
        let sqlProduk = "SELECT p.id, p.nama, p.kode, p.hargabeli, p.hargajual, t.nama as nama_toko FROM koperasi.produk p LEFT JOIN koperasi.toko t ON p.toko = t.id WHERE p.aktif = true ";
        
        // Pedagang hanya boleh memilih produknya sendiri
        if (!isAdminPengajuan<%=rnd%> && idTokoLogin<%=rnd%> !== "") {
            sqlProduk += " AND p.toko = " + idTokoLogin<%=rnd%> + " ";
        }
        sqlProduk += " ORDER BY p.nama ASC;";

        const resProduk = await fetchSqlData<%=rnd%>(sqlProduk);
        let listHtml = '';
        arrDataProduk<%=rnd%> = resProduk;
        
        resProduk.forEach(item => {
            const kode = item.kode ? ' [' + item.kode + ']' : '';
            const label = item.nama + kode;
            listHtml += '<option value="' + label + '" data-id="' + item.id + '">';
        });
        document.getElementById('listProdukTarget<%=rnd%>').innerHTML = listHtml;
    };

    document.getElementById('inputCariProduk<%=rnd%>').addEventListener('change', function() {
        const val = this.value;
        const match = arrDataProduk<%=rnd%>.find(p => (p.nama + (p.kode ? ' [' + p.kode + ']' : '')) === val);
        
        if (match) {
            document.getElementById('idProdukSelected<%=rnd%>').value = match.id;
            document.getElementById('lblHargaBeliLama<%=rnd%>').innerText = formatRp<%=rnd%>(match.hargabeli);
            document.getElementById('lblHargaJualLama<%=rnd%>').innerText = formatRp<%=rnd%>(match.hargajual);
        } else {
            document.getElementById('idProdukSelected<%=rnd%>').value = "";
            document.getElementById('lblHargaBeliLama<%=rnd%>').innerText = "Rp 0";
            document.getElementById('lblHargaJualLama<%=rnd%>').innerText = "Rp 0";
        }
    });


    // ==========================================
    // READ (TAMPILKAN TABEL)
    // ==========================================
    const setDefaultDates<%=rnd%> = () => {
        const today = new Date();
        const start = new Date();
        start.setMonth(today.getMonth() - 1);
        document.getElementById('filterEnd<%=rnd%>').value = today.toISOString().split('T')[0];
        document.getElementById('filterStart<%=rnd%>').value = start.toISOString().split('T')[0];
    };

    const buildFiltersPengajuan<%=rnd%> = () => {
        const tglMulai = document.getElementById('filterStart<%=rnd%>').value;
        const tglAkhir = document.getElementById('filterEnd<%=rnd%>').value;
        const nama = document.getElementById('filterNama<%=rnd%>').value.trim().replace(/'/g, "''");
        const status = document.getElementById('filterStatus<%=rnd%>').value;

        let filters = " WHERE 1=1 ";
        
        if (tglMulai !== "" && tglAkhir !== "") {
            filters += " AND DATE(a.tanggal) BETWEEN DATE('" + tglMulai + "') AND DATE('" + tglAkhir + "') ";
        }

        if (nama !== "") filters += " AND (p.nama ILIKE '%" + nama + "%' OR p.kode ILIKE '%" + nama + "%') ";
        
        if (status === "PENDING") {
            filters += " AND a.disetujui_oleh IS NULL ";
        } else if (status === "APPROVED") {
            filters += " AND a.disetujui_oleh IS NOT NULL ";
        }
        
        // Pembatasan Akses Pedagang
        if (!isAdminPengajuan<%=rnd%>) {
            filters += " AND p.toko = " + idTokoLogin<%=rnd%> + " ";
        }

        return filters;
    };

    const triggerSearchPengajuan<%=rnd%> = () => {
        currentPagePengajuan<%=rnd%> = 1;
        loadDataPengajuanAPI<%=rnd%>();
    };

    const loadDataPengajuanAPI<%=rnd%> = async () => {
        const tbody = document.getElementById('tablePengajuan<%=rnd%>');
        const colSpan = isAdminPengajuan<%=rnd%> ? 9 : 8;
        
        tbody.innerHTML = '<tr><td colspan="' + colSpan + '" class="text-center py-5 text-muted"><div class="spinner-border text-primary mb-3"></div><br><%=Common.getBahasaConfig("Memuat data pengajuan...")%></td></tr>';

        const offset = (currentPagePengajuan<%=rnd%> - 1) * limitPerPagePengajuan<%=rnd%>;
        const sqlFilters = buildFiltersPengajuan<%=rnd%>();

        // Karena di java class `alasan` di mapping sbg text, kita aman menariknya
        // Catatan: Jika ada prefix di table koperasi, sesuaikan aliasnya
        const sqlQueryData = "SELECT a.id, TO_CHAR(a.tanggal, 'DD-MM-YYYY HH24:MI') as tanggal_str, " +
                             "a.hargabeli as harga_beli_baru, a.hargajual as harga_jual_baru, a.alasan, " +
                             "a.disetujui_oleh, TO_CHAR(a.tanggal_disetujui, 'DD-MM-YYYY HH24:MI') as tanggal_disetujui_str, " +
                             "p.id as id_produk, p.nama as nama_produk, p.kode as kode_produk, " +
                             "p.hargabeli as harga_beli_lama, p.hargajual as harga_jual_lama, " +
                             "t.nama as nama_toko " +
                             "FROM koperasi.pengajuan_perubahan_harga_produk a " +
                             "INNER JOIN koperasi.produk p ON a.produk = p.id " +
                             "LEFT JOIN koperasi.toko t ON p.toko = t.id " +
                             sqlFilters +
                             " ORDER BY a.tanggal DESC, a.id DESC LIMIT " + limitPerPagePengajuan<%=rnd%> + " OFFSET " + offset + ";";

        const sqlQueryCount = "SELECT COUNT(a.id) AS jumlah FROM koperasi.pengajuan_perubahan_harga_produk a INNER JOIN koperasi.produk p ON a.produk = p.id " + sqlFilters + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetchSqlData<%=rnd%>(sqlQueryCount),
                fetchSqlData<%=rnd%>(sqlQueryData)
            ]);

            totalRecordsPengajuan<%=rnd%> = (resCount && resCount[0]) ? parseInt(resCount[0].jumlah || 0) : 0;
            const recordList = resData || [];

            let htmlTbody = '';
            if (recordList.length === 0) {
                htmlTbody = '<tr><td colspan="' + colSpan + '" class="text-center text-muted py-5"><i class="fas fa-file-invoice fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Belum ada data pengajuan.")%></td></tr>';
            } else {
                let no = offset + 1;
                recordList.forEach(row => {
                    const isDisetujui = (row.disetujui_oleh !== null && row.disetujui_oleh !== "");
                    
                    let badgeStatus = '';
                    if(isDisetujui) {
                        badgeStatus = '<span class="badge bg-success shadow-sm px-2"><i class="fas fa-check-double me-1"></i>Disetujui</span><br><small class="text-muted" style="font-size:10px;">' + (row.tanggal_disetujui_str || '') + '</small>';
                    } else {
                        badgeStatus = '<span class="badge bg-warning text-dark shadow-sm px-2"><i class="fas fa-clock me-1"></i>Menunggu</span>';
                    }

                    const safeAlasan = (row.alasan || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    
                    let actionBtn = '';
                    if (!isDisetujui) {
                        // Jika Admin, bisa Setujui.
                        if (isAdminPengajuan<%=rnd%>) {
                            actionBtn += '<button class="btn btn-sm btn-success shadow-sm fw-bold me-1 mb-1 px-3 w-100" onclick="approvePengajuan<%=rnd%>(' + row.id + ', ' + row.id_produk + ', ' + row.harga_beli_baru + ', ' + row.harga_jual_baru + ')"><i class="fas fa-check me-1"></i>Setujui</button>';
                        }
                        // Pembuat / Admin bisa Hapus jika masih pending
                        actionBtn += '<button class="btn btn-sm btn-outline-danger shadow-sm fw-bold w-100" onclick="deletePengajuan<%=rnd%>(' + row.id + ')" title="Hapus/Batalkan Pengajuan"><i class="fas fa-trash-alt me-1"></i>Hapus</button>';
                    } else {
                        actionBtn = '<span class="text-success small fw-bold"><i class="fas fa-lock"></i> Selesai</span>';
                    }

                    htmlTbody += '<tr class="' + (!isDisetujui ? 'table-warning' : '') + '">';
                    htmlTbody += '<td class="text-center text-muted">' + no++ + '</td>';
                    htmlTbody += '<td class="text-muted small fw-semibold">' + (row.tanggal_str || '-') + '</td>';
                    htmlTbody += '<td><span class="fw-bold text-dark">' + (row.nama_produk || '-') + '</span><br><small class="text-primary">' + (row.kode_produk || '-') + '</small></td>';
                    
                    if (isAdminPengajuan<%=rnd%>) {
                        htmlTbody += '<td><span class="text-muted small">' + (row.nama_toko || '-') + '</span></td>';
                    }
                    
                    htmlTbody += '<td class="text-end border-end"><small class="text-muted d-block">B: ' + formatRp<%=rnd%>(row.harga_beli_lama) + '</small><span class="text-dark fw-medium">J: ' + formatRp<%=rnd%>(row.harga_jual_lama) + '</span></td>';
                    
                    // Harga Baru (Berikan warna hijau jika ada perubahan)
                    let clrBeliBaru = (row.harga_beli_baru != row.harga_beli_lama) ? 'text-success' : 'text-muted';
                    let clrJualBaru = (row.harga_jual_baru != row.harga_jual_lama) ? 'text-success fw-bold' : 'text-dark fw-medium';
                    
                    htmlTbody += '<td class="text-end"><small class="d-block ' + clrBeliBaru + '">B: ' + formatRp<%=rnd%>(row.harga_beli_baru) + '</small><span class="' + clrJualBaru + '">J: ' + formatRp<%=rnd%>(row.harga_jual_baru) + '</span></td>';
                    
                    htmlTbody += '<td><span class="small text-secondary" style="display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;" title="' + safeAlasan + '">' + (row.alasan || '-') + '</span></td>';
                    htmlTbody += '<td class="text-center">' + badgeStatus + '</td>';
                    htmlTbody += '<td class="text-center text-nowrap">' + actionBtn + '</td>';
                    htmlTbody += '</tr>';
                });
            }
            tbody.innerHTML = htmlTbody;
            updatePaginationPengajuan<%=rnd%>();

        } catch (error) {
        	console.error(error);
            tbody.innerHTML = '<tr><td colspan="' + colSpan + '" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal mengambil data.")%></td></tr>';
        }
    };

    const updatePaginationPengajuan<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecordsPengajuan<%=rnd%> / limitPerPagePengajuan<%=rnd%>) || 1;
        document.getElementById('pageNumberPengajuan<%=rnd%>').innerText = currentPagePengajuan<%=rnd%> + " / " + totalPages;
        
        const startIdx = (currentPagePengajuan<%=rnd%> - 1) * limitPerPagePengajuan<%=rnd%> + 1;
        const endIdx = Math.min(currentPagePengajuan<%=rnd%> * limitPerPagePengajuan<%=rnd%>, totalRecordsPengajuan<%=rnd%>);
        const startInfo = totalRecordsPengajuan<%=rnd%> > 0 ? startIdx : 0;
        
        document.getElementById('pagingInfoPengajuan<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startInfo + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari")%> <b class="text-dark">' + totalRecordsPengajuan<%=rnd%> + '</b> <%=Common.getBahasaConfig("data")%>';
        
        document.getElementById('btnPrevPengajuan<%=rnd%>').disabled = (currentPagePengajuan<%=rnd%> === 1);
        document.getElementById('btnNextPengajuan<%=rnd%>').disabled = (currentPagePengajuan<%=rnd%> === totalPages);
    };

    const changePagePengajuan<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecordsPengajuan<%=rnd%> / limitPerPagePengajuan<%=rnd%>);
        if (direction === -1 && currentPagePengajuan<%=rnd%> > 1) {
            currentPagePengajuan<%=rnd%>--;
            loadDataPengajuanAPI<%=rnd%>();
        } else if (direction === 1 && currentPagePengajuan<%=rnd%> < totalPages) {
            currentPagePengajuan<%=rnd%>++;
            loadDataPengajuanAPI<%=rnd%>();
        }
    };


    // ==========================================
    // CREATE (FORM MODAL)
    // ==========================================
    const showAddModal<%=rnd%> = () => {
        document.getElementById('formPengajuan<%=rnd%>').reset();
        document.getElementById('formId<%=rnd%>').value = "";
        
        document.getElementById('idProdukSelected<%=rnd%>').value = "";
        document.getElementById('lblHargaBeliLama<%=rnd%>').innerText = "Rp 0";
        document.getElementById('lblHargaJualLama<%=rnd%>').innerText = "Rp 0";
        
        if(modalInstancePengajuan<%=rnd%> === null) {
            modalInstancePengajuan<%=rnd%> = new bootstrap.Modal(document.getElementById('modalFormPengajuan<%=rnd%>'));
        }
        modalInstancePengajuan<%=rnd%>.show();
    };

    const savePengajuan<%=rnd%> = async () => {
        const idProduk = document.getElementById('idProdukSelected<%=rnd%>').value;
        const hb = parseFloat(document.getElementById('formHargaBeliBaru<%=rnd%>').value) || 0;
        const hj = parseFloat(document.getElementById('formHargaJualBaru<%=rnd%>').value) || 0;
        const alasan = document.getElementById('formAlasan<%=rnd%>').value.trim();
        
        if(idProduk === "") {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Mohon pilih Produk terlebih dahulu dari kotak pencarian!")%>', 'bg-warning text-dark');
            document.getElementById('inputCariProduk<%=rnd%>').focus();
            return;
        }

        const btnSave = document.getElementById('btnSavePengajuan<%=rnd%>');
        const oriBtn = btnSave.innerHTML;
        btnSave.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Mengirim...")%>';
        btnSave.disabled = true;

        // Note: Field 'alasan' pada Java Model mungkin di mapping sebagai tipe Boolean, namun columnDefinition text
        // Jika bermasalah di Backend, pastikan parameter ini diterima sbg String di action.
        const payload = {
        	action: "simpanDataRinci", 
        	class: masterModelClass<%=rnd%>,
        	log: "true",
            data: {
                produk: idProduk,
                hargaBeli: hb,
                hargaJual: hj,
                alasan: alasan,
                tanggal: new Date().toISOString()
            }
        };

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const result = await response.json();
            
            if (result.status === '00' || result.status === 'success') {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Pengajuan perubahan harga berhasil dikirim!")%>', 'bg-success text-white');
                modalInstancePengajuan<%=rnd%>.hide();
                loadDataPengajuanAPI<%=rnd%>();
            } else {
                showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal mengirim data pengajuan.")%>', 'bg-danger text-white');
            }
        } catch (error) {
            console.error("Save Error:", error);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Koneksi gagal. Silakan coba lagi.")%>', 'bg-danger text-white');
        } finally {
            btnSave.innerHTML = oriBtn;
            btnSave.disabled = false;
        }
    };


    // ==========================================
    // APPROVE (ADMIN ONLY) & SYNC & DELETE
    // ==========================================
    const approvePengajuan<%=rnd%> = async (idPengajuan, idProduk, hargaBeliBaru, hargaJualBaru) => {
        if (!isAdminPengajuan<%=rnd%>) return;
        
        if(!confirm('<%=Common.getBahasaConfigJS("Setujui pengajuan ini? Harga pada master produk akan otomatis diperbarui.")%>')) return;

        // Query ganda untuk merubah status di tabel pengajuan DAN update harga di tabel produk
        const sqlApprove = "UPDATE koperasi.pengajuan_perubahan_harga_produk SET disetujui_oleh = '" + idUserLogin<%=rnd%> + "', tanggal_disetujui = NOW() WHERE id = " + idPengajuan + "; " +
                           "UPDATE koperasi.produk SET hargabeli = " + hargaBeliBaru + ", hargajual = " + hargaJualBaru + " WHERE id = " + idProduk + ";";

        try {
            const resJson = await fetchUpdateData<%=rnd%>(sqlApprove);
            if (resJson.status === 'success' || resJson.status === '00') {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Pengajuan disetujui! Harga produk telah diperbarui.")%>', 'bg-success text-white');
                loadDataPengajuanAPI<%=rnd%>();
            } else {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal menyetujui pengajuan di sisi database.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            console.error("Approve error", e);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Koneksi jaringan terputus saat menyetujui data.")%>', 'bg-danger text-white');
        }
    };

    const sinkronisasiHarga<%=rnd%> = async () => {
        if (!isAdminPengajuan<%=rnd%>) return;
        
        if(!confirm('<%=Common.getBahasaConfigJS("Proses ini akan memastikan seluruh harga di master produk sejajar dengan data pengajuan terbaru yang sudah disetujui. Lanjutkan?")%>')) return;

        const btnSync = document.getElementById('btnSyncHarga<%=rnd%>');
        const oriBtn = btnSync.innerHTML;
        btnSync.disabled = true;
        btnSync.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i><%=Common.getBahasaConfig("Menyinkronkan...")%>';

        // Query canggih UPDATE FROM (PostgreSQL) menggunakan data terbaru berdasarkan produk
        const sqlSync = "UPDATE koperasi.produk p " +
                        "SET hargabeli = sub.harga_beli, " +
                        "    hargajual = sub.harga_jual " +
                        "FROM ( " +
                        "    SELECT DISTINCT ON (produk) produk, hargabeli as harga_beli, hargajual as harga_jual " +
                        "    FROM koperasi.pengajuan_perubahan_harga_produk " +
                        "    WHERE disetujui_oleh IS NOT NULL " +
                        "    ORDER BY produk, tanggal_disetujui DESC " +
                        ") sub " +
                        "WHERE p.id = sub.produk AND (p.hargabeli != sub.harga_beli OR p.hargajual != sub.harga_jual);";

        try {
            const resJson = await fetchUpdateData<%=rnd%>(sqlSync);
            if (resJson.status === 'success' || resJson.status === '00') {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Harga produk berhasil disinkronkan!")%>', 'bg-success text-white');
                loadDataPengajuanAPI<%=rnd%>();
            } else {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal mensinkronkan harga.")%>', 'bg-warning text-dark');
            }
        } catch (e) {
            console.error("Sync error", e);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Koneksi terputus saat sinkronisasi.")%>', 'bg-danger text-white');
        } finally {
            btnSync.innerHTML = oriBtn;
            btnSync.disabled = false;
        }
    };

    const deletePengajuan<%=rnd%> = async (id) => {
    	 prosesDeleteData(masterModelClass<%=rnd%>, id, function(){ 
    		 if (document.querySelectorAll('#tablePengajuan<%=rnd%> tr').length === 1 && currentPagePengajuan<%=rnd%> > 1) {
                 currentPagePengajuan<%=rnd%>--;
             }
             loadDataPengajuanAPI<%=rnd%>();
         });
    };

    // ==========================================
    // INISIALISASI HALAMAN
    // ==========================================
    document.addEventListener("DOMContentLoaded", () => {
        setDefaultDates<%=rnd%>();
        loadProdukCombo<%=rnd%>();

        // Init Modals
        modalInstancePengajuan<%=rnd%> = new bootstrap.Modal(document.getElementById('modalFormPengajuan<%=rnd%>'));

        // Listener Filter Enter Key
        const filterInputs = document.querySelectorAll('.filter-input-<%=rnd%>');
        filterInputs.forEach(input => {
            if (input.tagName === 'SELECT') {
                input.addEventListener("change", () => triggerSearchPengajuan<%=rnd%>());
            } else {
                input.addEventListener("keydown", (event) => {
                    if (event.key === "Enter") {
                        event.preventDefault(); 
                        triggerSearchPengajuan<%=rnd%>();
                    }
                });
            }
        });

        // First Load
        loadDataPengajuanAPI<%=rnd%>();
    });
</script>