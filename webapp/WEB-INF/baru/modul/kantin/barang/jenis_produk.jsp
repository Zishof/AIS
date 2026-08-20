<%@page import="ais.database.model.inventory.JenisProduk"%>
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

// Hanya Admin yang boleh melakukan Tambah, Edit, dan Hapus
boolean isAdmin = (toko == null);
%>

<div id="viewList<%=rnd%>" class="animate__animated animate__fadeIn">
    <div class="row mb-4">
        <div class="col-12">
            <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
                <div class="card-header bg-white border-0 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-3 border-bottom border-light">
                    <div>
                        <h5 class="fw-bold text-dark mb-1">
                            <i class="fas fa-boxes text-primary me-2"></i><%=Common.getBahasaConfig("Manajemen Jenis Produk")%>
                        </h5>
                        <small class="text-muted"><%=Common.getBahasaConfig("Kelola master kategori atau jenis barang/jasa yang dijual.")%></small>
                    </div>
                    
                    <div class="d-flex align-items-center gap-2 flex-wrap">
                        <div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;">
                            <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>
                            <input type="text" class="form-control border-start-0 ps-0 bg-white fw-medium" id="searchJenisProduk<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Nama...")%>">
                        </div>
                        
                        <button class="btn btn-sm btn-outline-secondary rounded-pill px-3 shadow-sm fw-bold" onclick="resetAndLoadData<%=rnd%>()" title="<%=Common.getBahasaConfig("Muat Ulang Data")%>">
                            <i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Segarkan")%>
                        </button>
                        
                        <% if (isAdmin) { %>
                        <button class="btn btn-sm btn-primary rounded-pill px-4 shadow-sm fw-bold text-nowrap" onclick="bukaFormTambah<%=rnd%>()">
                            <i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Tambah Jenis")%>
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
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-font me-1"></i><%=Common.getBahasaConfig("Nama Jenis Produk")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-align-left me-1"></i><%=Common.getBahasaConfig("Keterangan")%></th>
                                    <th class="fw-semibold text-uppercase small px-3 text-end"><i class="fas fa-chart-line me-1"></i><%=Common.getBahasaConfig("Maks. Harian")%></th>
                                    <th class="fw-semibold text-uppercase small px-3"><i class="fas fa-star me-1"></i><%=Common.getBahasaConfig("Default")%></th>
                                    <th class="fw-semibold text-uppercase small px-3" style="width: 120px;"><i class="fas fa-toggle-on me-1"></i><%=Common.getBahasaConfig("Status")%></th>
                                    <% if (isAdmin) { %>
                                    <th class="fw-semibold text-uppercase small px-3 text-center" style="width: 100px;"><i class="fas fa-cogs"></i></th>
                                    <% } %>
                                </tr>
                            </thead>
                            <tbody id="tabelDataJenisProduk<%=rnd%>">
                                <tr><td colspan="<%= isAdmin ? 7 : 6 %>" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Memuat data jenis produk...")%></span></td></tr>
                            </tbody>
                        </table>
                    </div>

                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3">
                        <div class="small fw-medium text-muted bg-light px-3 py-1 rounded-pill border" id="pagingInfoJenisProduk<%=rnd%>">
                        </div>
                        <nav>
                            <ul class="pagination pagination-sm mb-0 shadow-sm">
                                <li class="page-item">
                                    <button class="page-link text-primary rounded-start-pill px-3 fw-bold" id="btnPrevPage<%=rnd%>" onclick="changePageJenisProduk<%=rnd%>(-1)">
                                        <i class="fas fa-chevron-left me-1"></i><%=Common.getBahasaConfig("Sebelumnnya")%>
                                    </button>
                                </li>
                                <li class="page-item disabled">
                                    <span class="page-link bg-light text-dark px-3 fw-bold" id="pageNumberDisplay<%=rnd%>">1</span>
                                </li>
                                <li class="page-item">
                                    <button class="page-link text-primary rounded-end-pill px-3 fw-bold" id="btnNextPage<%=rnd%>" onclick="changePageJenisProduk<%=rnd%>(1)">
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

<% if (isAdmin) { %>
<div id="viewForm<%=rnd%>" class="animate__animated animate__fadeIn" style="display: none;">
    <div class="row justify-content-center">
        <div class="col-lg-6">
            <div class="card border-0 shadow-sm rounded-4 border-top border-success border-4">
                <div class="card-header bg-white border-0 pt-4 pb-0 px-4">
                    <div class="d-flex justify-content-between align-items-center">
                        <h5 class="fw-bold text-dark mb-0" id="formTitle<%=rnd%>">
                            <i class="fas fa-box-open text-success me-2"></i><%=Common.getBahasaConfig("Formulir Jenis Produk")%>
                        </h5>
                        
                        <div class="form-check form-switch fs-5 mb-0">
                            <input class="form-check-input" type="checkbox" id="inputStatusAktif<%=rnd%>" checked style="cursor: pointer;">
                            <label class="form-check-label fs-6 fw-bold text-success" for="inputStatusAktif<%=rnd%>" id="labelStatusAktif<%=rnd%>"><%=Common.getBahasaConfig("Aktif")%></label>
                        </div>
                    </div>
                </div>
                <div class="card-body p-4">
                    <form id="formJenisProduk<%=rnd%>" onsubmit="event.preventDefault(); simpanJenisProduk<%=rnd%>();">
                        <input type="hidden" id="inputIdJenisProduk<%=rnd%>" value="">
                        
                        <div class="row g-3 mt-1">
                            
                            <div class="col-md-12">
                                <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Nama Jenis Produk")%> <span class="text-danger">*</span></label>
                                <div class="input-group input-group-sm shadow-sm">
                                    <span class="input-group-text bg-light border-end-0"><i class="fas fa-font text-muted"></i></span>
                                    <input type="text" class="form-control fw-bold border-start-0" id="inputNama<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: Makanan, Minuman, Jasa")%>" required>
                                </div>
                            </div>
                            
                            <div class="col-md-6">
                                <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Maksimal Harian")%></label>
                                <div class="input-group input-group-sm shadow-sm">
                                    <span class="input-group-text bg-light border-end-0"><i class="fas fa-sort-numeric-up text-muted"></i></span>
                                    <input type="number" step="0.01" min="0" class="form-control fw-bold border-start-0" id="inputMaksHarian<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Batas kuota / Kosongi jika tdk ada")%>">
                                </div>
                            </div>
                            
                            <div class="col-md-6 d-flex align-items-end pb-1">
                                <div class="form-check form-switch fs-6">
                                    <input class="form-check-input" type="checkbox" id="inputDefaultProduk<%=rnd%>" style="cursor: pointer;">
                                    <label class="form-check-label fw-semibold text-dark" for="inputDefaultProduk<%=rnd%>"><%=Common.getBahasaConfig("Jadikan Default")%></label>
                                </div>
                            </div>

                            <div class="col-12 mt-3">
                                <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Keterangan Tambahan")%></label>
                                <textarea class="form-control text-muted shadow-sm" id="inputKeterangan<%=rnd%>" rows="3" placeholder="<%=Common.getBahasaConfig("Masukkan penjelasan tambahan jika ada...")%>"></textarea>
                            </div>

                            <div class="col-12 mt-3">
                                <div class="border rounded-3 p-3 bg-light-subtle">
                                    <div class="fw-bold text-secondary small mb-2"><i class="fas fa-book text-primary me-1"></i><%=Common.getBahasaConfig("Akun Akuntansi (untuk Posting Jurnal Kantin)")%></div>
                                    <div class="row g-3">
                                        <div class="col-md-6">
                                            <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Akun Pendapatan Penjualan")%></label>
                                            <input type="text" class="form-control form-control-sm shadow-sm mb-1" id="cariAkunPendapatan<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari kode atau nama akun...")%>" autocomplete="off">
                                            <select class="form-select form-select-sm shadow-sm" id="inputAkunPendapatan<%=rnd%>"><option value=""><%=Common.getBahasaConfig("- Pilih Akun -")%></option></select>
                                        </div>
                                        <div class="col-md-6">
                                            <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Akun PPN Keluaran")%></label>
                                            <input type="text" class="form-control form-control-sm shadow-sm mb-1" id="cariAkunPpnKeluaran<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari kode atau nama akun...")%>" autocomplete="off">
                                            <select class="form-select form-select-sm shadow-sm" id="inputAkunPpnKeluaran<%=rnd%>"><option value=""><%=Common.getBahasaConfig("- Pilih Akun -")%></option></select>
                                        </div>
                                        <div class="col-md-6">
                                            <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Akun HPP (Beban Pokok Penjualan)")%></label>
                                            <input type="text" class="form-control form-control-sm shadow-sm mb-1" id="cariAkunHpp<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari kode atau nama akun...")%>" autocomplete="off">
                                            <select class="form-select form-select-sm shadow-sm" id="inputAkunHpp<%=rnd%>"><option value=""><%=Common.getBahasaConfig("- Pilih Akun -")%></option></select>
                                        </div>
                                        <div class="col-md-6">
                                            <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Akun Selisih Persediaan")%></label>
                                            <input type="text" class="form-control form-control-sm shadow-sm mb-1" id="cariAkunSelisih<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari kode atau nama akun...")%>" autocomplete="off">
                                            <select class="form-select form-select-sm shadow-sm" id="inputAkunSelisih<%=rnd%>"><option value=""><%=Common.getBahasaConfig("- Pilih Akun -")%></option></select>
                                            <div class="form-text small"><%=Common.getBahasaConfig("Lawan jurnal saat selisih stok opname diposting.")%></div>
                                        <div class="col-md-6">
                                            <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Akun Retur Penjualan")%></label>
                                            <input type="text" class="form-control form-control-sm shadow-sm mb-1" id="cariAkunRetur<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari kode atau nama akun...")%>" autocomplete="off">
                                            <select class="form-select form-select-sm shadow-sm" id="inputAkunRetur<%=rnd%>"><option value=""><%=Common.getBahasaConfig("- Pilih Akun -")%></option></select>
                                            <div class="form-text small"><%=Common.getBahasaConfig("Kosongkan untuk memakai Akun Pendapatan.")%></div>
                                        </div>
                                        </div>
                                    </div>
                                </div>
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
<% } %>

<script>
    // ==========================================
    // BAGIAN 1: UTILITIES & STATE GLOBAL
    // ==========================================
    const masterModelClass<%=rnd%> = "ais.database.model.inventory.JenisProduk";
    
    // Inject status Admin
    const isAdmin<%=rnd%> = <%=isAdmin%>;
    const colSpan<%=rnd%> = isAdmin<%=rnd%> ? 7 : 6;

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

    // Pemilih Akun (mirror AmbilDataAkunBanbox versi ZK): daftar akun dari akunting.akun.
    // Dilengkapi kotak cari (kode & nama) karena bagan akun bisa ratusan baris
    // sehingga dropdown polos sulit dipakai.
    const ID_AKUN<%=rnd%> = ['inputAkunPendapatan<%=rnd%>', 'inputAkunPpnKeluaran<%=rnd%>', 'inputAkunHpp<%=rnd%>', 'inputAkunSelisih<%=rnd%>', 'inputAkunRetur<%=rnd%>'];
    let akunDaftar<%=rnd%> = [];
    const escAkun<%=rnd%> = (t) => (t + '').replace(/</g, '&lt;').replace(/"/g, '&quot;');
    const opsiAkunHtml<%=rnd%> = (kata, nilai) => {
        const kunci = (kata || '').trim().toLowerCase().split(/\s+/).filter((k) => k.length > 0);
        let html = '<option value=""><%=Common.getBahasaConfigJS("- Pilih Akun -")%></option>';
        (akunDaftar<%=rnd%> || []).forEach((a) => {
            const teks = ((a.kode || '') + ' ' + (a.nama || '')).toLowerCase();
            let cocok = true;
            kunci.forEach((k) => { if (teks.indexOf(k) < 0) { cocok = false; } });
            // Akun yang sedang terpilih selalu ikut ditampilkan supaya nilainya
            // tidak hilang ketika daftar sedang tersaring.
            const iniTerpilih = nilai !== null && nilai !== undefined && nilai !== '' && (a.id + '') === (nilai + '');
            if (!cocok && !iniTerpilih) { return; }
            html += '<option value="' + a.id + '">' + escAkun<%=rnd%>((a.kode || '') + ' - ' + (a.nama || '')) + '</option>';
        });
        return html;
    };
    const terapkanFilterAkun<%=rnd%> = (sid) => {
        const el = document.getElementById(sid);
        if (!el) { return; }
        const box = document.getElementById(sid.replace('input', 'cari'));
        const cur = el.value;
        el.innerHTML = opsiAkunHtml<%=rnd%>(box ? box.value : '', cur);
        el.value = cur;
    };
    const loadAkunOptions<%=rnd%> = async () => {
        const list = await fetchData<%=rnd%>("SELECT id, kode, nama FROM akunting.akun ORDER BY kode ASC");
        akunDaftar<%=rnd%> = list || [];
        ID_AKUN<%=rnd%>.forEach((sid) => { terapkanFilterAkun<%=rnd%>(sid); });
    };
    ID_AKUN<%=rnd%>.forEach((sid) => {
        const box = document.getElementById(sid.replace('input', 'cari'));
        if (box) { box.addEventListener('input', function() { terapkanFilterAkun<%=rnd%>(sid); }); }
    });
    const setAkunSelect<%=rnd%> = (sid, val) => {
        const el = document.getElementById(sid);
        if (!el) { return; }
        // Kotak cari dikosongkan dulu agar nilai dari server pasti ada di daftar.
        const box = document.getElementById(sid.replace('input', 'cari'));
        if (box) { box.value = ''; }
        const v = (val === null || val === undefined) ? '' : (val + '');
        el.innerHTML = opsiAkunHtml<%=rnd%>('', v);
        el.value = v;
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

    // ==========================================
    // BAGIAN 2: LOGIKA LIST, PAGING & SEARCH
    // ==========================================
    const buildFilterSQL<%=rnd%> = () => {
        const keyword = document.getElementById('searchJenisProduk<%=rnd%>').value.trim().replace(/'/g, "''");
        let filters = " WHERE 1=1 ";
        if (keyword !== '') {
            filters += " AND (nama ILIKE '%" + keyword + "%' OR keterangan ILIKE '%" + keyword + "%') ";
        }
        return filters;
    };

    const resetAndLoadData<%=rnd%> = () => {
        currentPage<%=rnd%> = 1; 
        loadDataJenisProduk<%=rnd%>();
    };

    const loadDataJenisProduk<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataJenisProduk<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Mengambil data...")%></span></td></tr>';

        const sqlFilters = buildFilterSQL<%=rnd%>();
        const offset = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%>;

        // Query Total Baris untuk Paginasi
        const sqlCount = "SELECT COUNT(*) AS jumlah FROM koperasi.jenis_produk " + sqlFilters + ";";
        
        // Asumsi struktur DB column names (berdasarkan Hibernate default naming)
        const sqlData = "SELECT id, nama, keterangan, maksimalharian as maksimal_harian, defaultproduk as default_produk, aktif FROM koperasi.jenis_produk " + sqlFilters + " ORDER BY nama ASC LIMIT " + limitPerPage<%=rnd%> + " OFFSET " + offset + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetchData<%=rnd%>(sqlCount),
                fetchData<%=rnd%>(sqlData)
            ]);

            totalRecords<%=rnd%> = (resCount && resCount[0]) ? parseInt(resCount[0].jumlah || 0) : 0;
            const recordList = resData || [];

            let htmlList = '';

            if (recordList.length === 0) {
                htmlList = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center text-muted py-5"><i class="fas fa-boxes fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Tidak ada data jenis produk ditemukan.")%></td></tr>';
            } else {
                let no = offset + 1; 
                recordList.forEach((row) => {

                    const isStatusAktif = (row.aktif === null || row.aktif === true || row.aktif === 't' || row.aktif === 'true');
                    const badgeStatus = isStatusAktif ? '<span class="badge bg-success bg-opacity-10 text-success border border-success px-2 py-1"><i class="fas fa-check-circle me-1"></i>Aktif</span>' : '<span class="badge bg-danger bg-opacity-10 text-danger border border-danger px-2 py-1"><i class="fas fa-times-circle me-1"></i>Non-Aktif</span>';

                    const isDefault = (row.default_produk === true || row.default_produk === 't' || row.default_produk === 'true');
                    const badgeDefault = isDefault ? '<i class="fas fa-check text-primary fw-bold"></i> Ya' : '-';
                    
                    const maksHarian = parseFloat(row.maksimal_harian || 0);
                    const txtMaksHarian = maksHarian > 0 ? maksHarian : '-';

                    htmlList += '<tr>' +
                            '<td class="text-center text-muted fw-medium">' + (no++) + '</td>' +
                            '<td class="text-start fw-bold text-dark">' + (row.nama || '-') + '</td>' +
                            '<td class="text-start text-muted small">' + (row.keterangan || '-') + '</td>' +
                            '<td class="text-end fw-semibold text-primary">' + txtMaksHarian + '</td>' +
                            '<td class="text-center">' + badgeDefault + '</td>' +
                            '<td class="text-center">' + badgeStatus + '</td>';
                    
                    // Render kolom aksi hanya jika user adalah admin
                    if (isAdmin<%=rnd%>) {
                        htmlList += '<td class="text-center text-nowrap">' +
                                        '<button class="btn btn-sm btn-outline-warning text-dark shadow-sm px-2 me-1 fw-bold" onclick="editJenisProduk<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Ubah Data")%>"><i class="fas fa-edit"></i></button>' +
                                        '<button class="btn btn-sm btn-outline-danger shadow-sm px-2 fw-bold" onclick="hapusJenisProduk<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Hapus Data")%>"><i class="fas fa-trash-alt"></i></button>' +
                                    '</td>';
                    }
                            
                    htmlList += '</tr>';
                });
            }
            tbody.innerHTML = htmlList;
            updatePaginationUI<%=rnd%>();

        } catch (error) {
            console.error("Gagal load tabel jenis produk:", error);
            tbody.innerHTML = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal menarik data. Silakan muat ulang.")%></td></tr>';
        }
    };

    const updatePaginationUI<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumberDisplay<%=rnd%>').innerText = currentPage<%=rnd%> + " / " + totalPages;
        
        const startIdx = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%> + 1;
        const endIdx = Math.min(currentPage<%=rnd%> * limitPerPage<%=rnd%>, totalRecords<%=rnd%>);
        const startInfo = totalRecords<%=rnd%> > 0 ? startIdx : 0;
        
        document.getElementById('pagingInfoJenisProduk<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startInfo + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari")%> <b class="text-dark">' + totalRecords<%=rnd%> + '</b> <%=Common.getBahasaConfig("data")%>';
        
        document.getElementById('btnPrevPage<%=rnd%>').disabled = (currentPage<%=rnd%> === 1);
        document.getElementById('btnNextPage<%=rnd%>').disabled = (currentPage<%=rnd%> === totalPages);
    };

    const changePageJenisProduk<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if (direction === -1 && currentPage<%=rnd%> > 1) {
            currentPage<%=rnd%>--;
            loadDataJenisProduk<%=rnd%>();
        } else if (direction === 1 && currentPage<%=rnd%> < totalPages) {
            currentPage<%=rnd%>++;
            loadDataJenisProduk<%=rnd%>();
        }
    };


    // ==========================================
    // BAGIAN 3: LOGIKA FORM & DINAMIS UI (ADMIN ONLY)
    // ==========================================
    const bukaFormTambah<%=rnd%> = () => {
        if (!isAdmin<%=rnd%>) return; // Penjagaan ganda
        
        document.getElementById('inputIdJenisProduk<%=rnd%>').value = '';
        document.getElementById('formJenisProduk<%=rnd%>').reset();
        
        document.getElementById('inputStatusAktif<%=rnd%>').checked = true;
        document.getElementById('inputStatusAktif<%=rnd%>').dispatchEvent(new Event('change'));
        
        document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-boxes text-primary me-2"></i><%=Common.getBahasaConfig("Tambah Jenis Produk")%>';
        
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    };

    const tutupForm<%=rnd%> = () => {
        if (!isAdmin<%=rnd%>) return;
        document.getElementById('viewForm<%=rnd%>').style.display = 'none';
        document.getElementById('viewList<%=rnd%>').style.display = 'block';
        loadDataJenisProduk<%=rnd%>();
    };


    // ==========================================
    // BAGIAN 4: SIMPAN, EDIT, & HAPUS
    // ==========================================
    const simpanJenisProduk<%=rnd%> = async () => {
        if (!isAdmin<%=rnd%>) return;
        
        const idJenis = document.getElementById('inputIdJenisProduk<%=rnd%>').value;
        const btnSimpan = document.getElementById('btnSimpan<%=rnd%>');
        
        // Object utama JenisProduk.java
        const dataObj = {
            nama: document.getElementById('inputNama<%=rnd%>').value,
            keterangan: document.getElementById('inputKeterangan<%=rnd%>').value,
            maksimalHarian: parseFloat(document.getElementById('inputMaksHarian<%=rnd%>').value) || null,
            defaultProduk: document.getElementById('inputDefaultProduk<%=rnd%>').checked,
            aktif: document.getElementById('inputStatusAktif<%=rnd%>').checked,
            // FK Akun: dikirim sebagai id (server resolve ke entity Akun; kosong = null).
            akunPendapatan: document.getElementById('inputAkunPendapatan<%=rnd%>').value || null,
            akunPpnKeluaran: document.getElementById('inputAkunPpnKeluaran<%=rnd%>').value || null,
            akunHpp: document.getElementById('inputAkunHpp<%=rnd%>').value || null,
            akunSelisihPersediaan: document.getElementById('inputAkunSelisih<%=rnd%>').value || null,
            akunReturPenjualan: document.getElementById('inputAkunRetur<%=rnd%>').value || null
        };
        
        const payload = { 
            action: "simpanDataRinci", 
            class: masterModelClass<%=rnd%>, 
            data: dataObj
        };

        if (idJenis !== '') {
            payload.id = idJenis;
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
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Jenis produk berhasil disimpan!")%>', 'bg-success text-white');
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

    const editJenisProduk<%=rnd%> = async (id) => {
        if (!isAdmin<%=rnd%>) return;
        
        const sql = 'SELECT id, nama, keterangan, maksimalharian as maksimal_harian, defaultproduk as default_produk, aktif, akun_pendapatan, akun_ppn_keluaran, akun_hpp, akun_selisih_persediaan, akun_retur_penjualan FROM koperasi.jenis_produk WHERE id = ' + id;
        const res = await fetchData<%=rnd%>(sql);

        if (res.length > 0) {
            const data = res[0];

            // Set Form Data
            document.getElementById('inputIdJenisProduk<%=rnd%>').value = data.id;
            document.getElementById('inputNama<%=rnd%>').value = data.nama || '';
            document.getElementById('inputMaksHarian<%=rnd%>').value = data.maksimal_harian || '';
            document.getElementById('inputKeterangan<%=rnd%>').value = data.keterangan || '';
            setAkunSelect<%=rnd%>('inputAkunPendapatan<%=rnd%>', data.akun_pendapatan);
            setAkunSelect<%=rnd%>('inputAkunPpnKeluaran<%=rnd%>', data.akun_ppn_keluaran);
            setAkunSelect<%=rnd%>('inputAkunHpp<%=rnd%>', data.akun_hpp);
            setAkunSelect<%=rnd%>('inputAkunSelisih<%=rnd%>', data.akun_selisih_persediaan);
            setAkunSelect<%=rnd%>('inputAkunRetur<%=rnd%>', data.akun_retur_penjualan);
            
            const isDefault = (data.default_produk === true || data.default_produk === 'true' || data.default_produk === 't');
            document.getElementById('inputDefaultProduk<%=rnd%>').checked = isDefault;

            const isAktif = (data.aktif === null || data.aktif === true || data.aktif === 'true' || data.aktif === 't');
            const chkAktif = document.getElementById('inputStatusAktif<%=rnd%>');
            chkAktif.checked = isAktif;
            chkAktif.dispatchEvent(new Event('change')); 
            
            document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-edit text-warning me-2"></i><%=Common.getBahasaConfig("Ubah Jenis Produk")%>';
            
            document.getElementById('viewList<%=rnd%>').style.display = 'none';
            document.getElementById('viewForm<%=rnd%>').style.display = 'block';
        }
    };

    const hapusJenisProduk<%=rnd%> = async (id) => {
        if (!isAdmin<%=rnd%>) return;

        
        prosesDeleteData('<%=JenisProduk.class.getName()%>', id, function(){ 
        	if (document.querySelectorAll('#tabelDataJenisProduk<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) {
            	currentPage<%=rnd%>--;
	        }
	        loadDataJenisProduk<%=rnd%>();
        });
        
    };

    // ==========================================
    // INISIALISASI HALAMAN
    // ==========================================
    document.addEventListener("DOMContentLoaded", () => {
        document.getElementById('searchJenisProduk<%=rnd%>').addEventListener("keydown", (e) => {
            if (e.key === "Enter") {
                e.preventDefault();
                resetAndLoadData<%=rnd%>(); 
            }
        });
        loadDataJenisProduk<%=rnd%>();
        loadAkunOptions<%=rnd%>();
    });
</script>