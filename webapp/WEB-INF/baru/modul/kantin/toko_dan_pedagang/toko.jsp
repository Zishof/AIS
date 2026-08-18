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

// Aturan RBAC: 
// Admin = Akses penuh (bisa melihat semua toko dan edit)
// Pedagang = Readonly, HANYA melihat data tokonya sendiri
boolean isAdmin = (toko == null);
String idTokoAktif = toko != null ? String.valueOf(toko.getId()) : "";
%>

<div id="viewList<%=rnd%>" class="animate__animated animate__fadeInUp">
    <div class="row mb-4">
        <div class="col-12">
            <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
                
                <div class="card-header bg-white border-0 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-3 border-bottom border-light">
                    <div>
                        <h5 class="fw-bold text-dark mb-1">
                            <i class="fas fa-store text-primary me-2"></i><%=Common.getBahasaConfig("Manajemen Data Toko (Kios)")%>
                        </h5>
                        <small class="text-muted"><%=Common.getBahasaConfig("Kelola profil, identitas, dan status operasional toko/kios.")%></small>
                    </div>
                    
                    <div class="d-flex align-items-center gap-2 flex-wrap">
                        <div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;">
                            <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>
                            <input type="text" class="form-control border-start-0 ps-0 bg-white fw-medium" id="searchToko<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Kode atau Nama...")%>">
                        </div>
                        
                        <button class="btn btn-sm btn-outline-secondary rounded-pill px-3 shadow-sm fw-bold" onclick="resetAndLoadData<%=rnd%>()" title="<%=Common.getBahasaConfig("Muat Ulang Data")%>">
                            <i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Segarkan")%>
                        </button>
                        
                        <% if (isAdmin) { %>
                        <button class="btn btn-sm btn-primary rounded-pill px-4 shadow-sm fw-bold text-nowrap" onclick="bukaFormTambah<%=rnd%>()">
                            <i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Tambah Toko")%>
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
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-store me-1"></i><%=Common.getBahasaConfig("Nama Toko/Kios")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-align-left me-1"></i><%=Common.getBahasaConfig("Keterangan")%></th>
                                    <th class="fw-semibold text-uppercase small px-3" style="width: 120px;"><i class="fas fa-eye me-1"></i><%=Common.getBahasaConfig("Akses Multi-Toko")%></th>
                                    <th class="fw-semibold text-uppercase small px-3" style="width: 120px;"><i class="fas fa-toggle-on me-1"></i><%=Common.getBahasaConfig("Status")%></th>
                                    <% if (isAdmin) { %>
                                    <th class="fw-semibold text-uppercase small px-3 text-center" style="width: 100px;"><i class="fas fa-cogs"></i></th>
                                    <% } %>
                                </tr>
                            </thead>
                            <tbody id="tabelDataToko<%=rnd%>">
                                <tr><td colspan="<%= isAdmin ? 7 : 6 %>" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Memuat data toko...")%></span></td></tr>
                            </tbody>
                        </table>
                    </div>

                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3">
                        <div class="small fw-medium text-muted bg-light px-3 py-1 rounded-pill border" id="pagingInfoToko<%=rnd%>">
                        </div>
                        <nav>
                            <ul class="pagination pagination-sm mb-0 shadow-sm">
                                <li class="page-item">
                                    <button class="page-link text-primary rounded-start-pill px-3 fw-bold" id="btnPrevPage<%=rnd%>" onclick="changePageToko<%=rnd%>(-1)">
                                        <i class="fas fa-chevron-left me-1"></i><%=Common.getBahasaConfig("Sebelumnnya")%>
                                    </button>
                                </li>
                                <li class="page-item disabled">
                                    <span class="page-link bg-light text-dark px-3 fw-bold" id="pageNumberDisplay<%=rnd%>">1</span>
                                </li>
                                <li class="page-item">
                                    <button class="page-link text-primary rounded-end-pill px-3 fw-bold" id="btnNextPage<%=rnd%>" onclick="changePageToko<%=rnd%>(1)">
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
                            <i class="fas fa-store text-success me-2"></i><%=Common.getBahasaConfig("Formulir Toko")%>
                        </h5>
                        
                        <div class="form-check form-switch fs-5 mb-0">
                            <input class="form-check-input" type="checkbox" id="inputStatusAktif<%=rnd%>" checked style="cursor: pointer;">
                            <label class="form-check-label fs-6 fw-bold text-success" for="inputStatusAktif<%=rnd%>" id="labelStatusAktif<%=rnd%>"><%=Common.getBahasaConfig("Aktif")%></label>
                        </div>
                    </div>
                </div>
                <div class="card-body p-4">
                    <form id="formToko<%=rnd%>" onsubmit="event.preventDefault(); simpanToko<%=rnd%>();">
                        <input type="hidden" id="inputIdToko<%=rnd%>" value="">
                        
                        <div class="row g-3 mt-1">
                            
                            <div class="col-md-12">
                                <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Kode Toko")%></label>
                                <div class="input-group input-group-sm shadow-sm">
                                    <span class="input-group-text bg-light border-end-0"><i class="fas fa-barcode text-muted"></i></span>
                                    <input type="text" class="form-control fw-bold border-start-0" id="inputKode<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Kosongi untuk generate otomatis")%>">
                                </div>
                            </div>
                            
                            <div class="col-md-12">
                                <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Nama Toko/Kios")%> <span class="text-danger">*</span></label>
                                <div class="input-group input-group-sm shadow-sm">
                                    <span class="input-group-text bg-light border-end-0"><i class="fas fa-font text-muted"></i></span>
                                    <input type="text" class="form-control fw-bold border-start-0" id="inputNama<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: Kios Minuman A")%>" required>
                                </div>
                            </div>
                            
                            <div class="col-md-12 pt-2">
                                <div class="form-check form-switch fs-6">
                                    <input class="form-check-input" type="checkbox" id="inputBolehMelihatTokoLain<%=rnd%>" style="cursor: pointer;">
                                    <label class="form-check-label fw-semibold text-dark" for="inputBolehMelihatTokoLain<%=rnd%>"><%=Common.getBahasaConfig("Izinkan Toko Ini Melihat Data Toko Lain")%></label>
                                </div>
                                <small class="text-muted fst-italic"><%=Common.getBahasaConfig("*Berguna jika toko ini adalah cabang utama/supervisor.")%></small>
                            </div>

                            <div class="col-md-12 pt-2">
                                <div class="form-check form-switch fs-6">
                                    <input class="form-check-input" type="checkbox" id="inputBolehTransaksiStokHabis<%=rnd%>" style="cursor: pointer;">
                                    <label class="form-check-label fw-semibold text-dark" for="inputBolehTransaksiStokHabis<%=rnd%>"><%=Common.getBahasaConfig("Paksa Semua Produk Boleh Stok Minus")%></label>
                                </div>
                                <small class="text-muted fst-italic"><%=Common.getBahasaConfig("*OFF: ikuti izin stok minus pada masing-masing produk. ON: seluruh produk toko ini boleh dijual saat stok nol/minus.")%></small>
                            </div>

                            <div class="col-md-12 pt-2">
                                <label class="form-label small fw-semibold text-secondary mb-1"><%=Common.getBahasaConfig("Unit Usaha (boleh pilih lebih dari satu)")%></label>
                                <small class="text-muted fst-italic d-block mb-2"><%=Common.getBahasaConfig("*Dipakai generator Data Contoh untuk memilih katalog produk sesuai jenis usaha toko.")%></small>
                                <div class="border rounded-3 p-2" style="max-height:220px;overflow-y:auto;" id="wadahUnitUsaha<%=rnd%>">
                                    <div class="text-muted small"><span class="spinner-border spinner-border-sm me-1"></span><%=Common.getBahasaConfig("Memuat katalog unit usaha...")%></div>
                                </div>
                            </div>

                            <div class="col-12 mt-3">
                                <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Keterangan Tambahan")%></label>
                                <textarea class="form-control text-muted shadow-sm" id="inputKeterangan<%=rnd%>" rows="3" placeholder="<%=Common.getBahasaConfig("Lokasi, PIC, dll...")%>"></textarea>
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
    const masterModelClass<%=rnd%> = "ais.database.model.inventory.Toko";
    
    const isAdmin<%=rnd%> = <%=isAdmin%>;
    const idTokoLogin<%=rnd%> = '<%=idTokoAktif%>';
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

    // Katalog unit usaha -- dimuat sekali dari server (aksi unit_usaha_katalog,
    // TokoApiHelper), dirender sebagai grid checkbox ber-grup di form.
    let katalogUnitUsaha<%=rnd%> = [];
    const muatKatalogUnitUsaha<%=rnd%> = async () => {
        const wadah = document.getElementById('wadahUnitUsaha<%=rnd%>');
        if (!wadah) return;
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: 'unit_usaha_katalog' })
            });
            const hasil = await res.json();
            if (hasil.status !== '00') throw new Error(hasil.description || 'gagal');
            katalogUnitUsaha<%=rnd%> = hasil.data || [];
            let html = '';
            let grupTerakhir = null;
            katalogUnitUsaha<%=rnd%>.forEach((u) => {
                if (u.grup !== grupTerakhir) {
                    grupTerakhir = u.grup;
                    html += '<div class="fw-bold small text-primary mt-2 mb-1">' + u.grup + '</div>';
                }
                html += '<div class="form-check form-check-inline">' +
                    '<input class="form-check-input cek-unit-usaha-<%=rnd%>" type="checkbox" value="' + u.kode + '" id="uu<%=rnd%>' + u.kode + '">' +
                    '<label class="form-check-label small" for="uu<%=rnd%>' + u.kode + '">' + u.label + '</label></div>';
            });
            wadah.innerHTML = html || '<span class="text-muted small"><%=Common.getBahasaConfigJS("Katalog kosong.")%></span>';
        } catch (e) {
            wadah.innerHTML = '<span class="text-danger small"><%=Common.getBahasaConfigJS("Gagal memuat katalog unit usaha.")%></span>';
        }
    };
    const setUnitUsahaTerpilih<%=rnd%> = (daftarKode) => {
        document.querySelectorAll('.cek-unit-usaha-<%=rnd%>').forEach((c) => {
            c.checked = (daftarKode || []).includes(c.value);
        });
    };
    const ambilUnitUsahaTerpilih<%=rnd%> = () =>
        [...document.querySelectorAll('.cek-unit-usaha-<%=rnd%>:checked')].map((c) => c.value);

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
        const keyword = document.getElementById('searchToko<%=rnd%>').value.trim().replace(/'/g, "''");
        let filters = " WHERE 1=1 ";
        
        if (keyword !== '') {
            filters += " AND (nama ILIKE '%" + keyword + "%' OR kode ILIKE '%" + keyword + "%' OR keterangan ILIKE '%" + keyword + "%') ";
        }

        // Filter ketat: Jika pedagang, hanya boleh lihat tokonya sendiri
        if (!isAdmin<%=rnd%> && idTokoLogin<%=rnd%> !== '') {
            filters += " AND id = " + idTokoLogin<%=rnd%> + " ";
        }

        return filters;
    };

    const resetAndLoadData<%=rnd%> = () => {
        currentPage<%=rnd%> = 1; 
        loadDataToko<%=rnd%>();
    };

    const loadDataToko<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataToko<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Mengambil data...")%></span></td></tr>';

        const sqlFilters = buildFilterSQL<%=rnd%>();
        const offset = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%>;

        // Query Total Baris untuk Paginasi
        const sqlCount = "SELECT COUNT(*) AS jumlah FROM koperasi.toko " + sqlFilters + ";";
        
        const sqlData = "SELECT id, kode, nama, keterangan, bolehmelihattokolain, aktif, toko_demo, unit_usaha_json FROM koperasi.toko " + sqlFilters + " ORDER BY nama ASC LIMIT " + limitPerPage<%=rnd%> + " OFFSET " + offset + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetchData<%=rnd%>(sqlCount),
                fetchData<%=rnd%>(sqlData)
            ]);

            totalRecords<%=rnd%> = (resCount && resCount[0]) ? parseInt(resCount[0].jumlah || 0) : 0;
            const recordList = resData || [];

            let htmlList = '';

            if (recordList.length === 0) {
                htmlList = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center text-muted py-5"><i class="fas fa-store-slash fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Tidak ada toko ditemukan.")%></td></tr>';
            } else {
                let no = offset + 1; 
                recordList.forEach((row) => {

                    const isStatusAktif = (row.aktif === null || row.aktif === true || row.aktif === 't' || row.aktif === 'true');
                    const badgeStatus = isStatusAktif ? '<span class="badge bg-success bg-opacity-10 text-success border border-success px-2 py-1"><i class="fas fa-check-circle me-1"></i>Aktif</span>' : '<span class="badge bg-danger bg-opacity-10 text-danger border border-danger px-2 py-1"><i class="fas fa-times-circle me-1"></i>Non-Aktif</span>';

                    const isBolehMelihat = (row.bolehmelihattokolain === true || row.bolehmelihattokolain === 't' || row.bolehmelihattokolain === 'true');
                    const badgeBolehMelihat = isBolehMelihat ? '<span class="text-primary fw-bold"><i class="fas fa-check-double me-1"></i>Ya</span>' : '<span class="text-muted"><i class="fas fa-minus me-1"></i>Tidak</span>';

                    const safeNama = (row.nama || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');

                    htmlList += '<tr>' +
                            '<td class="text-center text-muted fw-medium">' + (no++) + '</td>' +
                            '<td class="text-start fw-bold text-secondary">' + (row.kode || '-') + '</td>' +
                            '<td class="text-start fw-bold text-dark">' + (row.nama || '-') + '</td>' +
                            '<td class="text-start text-muted small">' + (row.keterangan || '-') + '</td>' +
                            '<td class="text-center">' + badgeBolehMelihat + '</td>' +
                            '<td class="text-center">' + badgeStatus + '</td>';
                    
                    // Render kolom aksi hanya jika user adalah admin
                    if (isAdmin<%=rnd%>) {
                        const isTokoDemo = (row.toko_demo === true || row.toko_demo === 't' || row.toko_demo === 'true');
                        petaUnitUsahaToko<%=rnd%>[row.id] = row.unit_usaha_json || '[]';
                        htmlList += '<td class="text-center text-nowrap">' +
                                        (isTokoDemo ? '<button class="btn btn-sm btn-outline-success shadow-sm px-2 me-1 fw-bold" onclick="bukaGenerateProduk<%=rnd%>(' + row.id + ', \'' + safeNama + '\')" title="<%=Common.getBahasaConfig("Generate produk contoh sesuai unit usaha")%>"><i class="fas fa-magic"></i></button>' : '') +
                                        '<button class="btn btn-sm btn-outline-warning text-dark shadow-sm px-2 me-1 fw-bold" onclick="editToko<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Ubah Data")%>"><i class="fas fa-edit"></i></button>' +
                                        '<button class="btn btn-sm btn-outline-danger shadow-sm px-2 fw-bold" onclick="hapusToko<%=rnd%>(' + row.id + ', \'' + safeNama + '\')" title="<%=Common.getBahasaConfig("Hapus Data")%>"><i class="fas fa-trash-alt"></i></button>' +
                                    '</td>';
                    }
                            
                    htmlList += '</tr>';
                });
            }
            tbody.innerHTML = htmlList;
            updatePaginationUI<%=rnd%>();

        } catch (error) {
            console.error("Gagal load tabel toko:", error);
            tbody.innerHTML = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal menarik data. Silakan muat ulang.")%></td></tr>';
        }
    };

    const updatePaginationUI<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumberDisplay<%=rnd%>').innerText = currentPage<%=rnd%> + " / " + totalPages;
        
        const startIdx = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%> + 1;
        const endIdx = Math.min(currentPage<%=rnd%> * limitPerPage<%=rnd%>, totalRecords<%=rnd%>);
        const startInfo = totalRecords<%=rnd%> > 0 ? startIdx : 0;
        
        document.getElementById('pagingInfoToko<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startInfo + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari")%> <b class="text-dark">' + totalRecords<%=rnd%> + '</b> <%=Common.getBahasaConfig("data")%>';
        
        document.getElementById('btnPrevPage<%=rnd%>').disabled = (currentPage<%=rnd%> === 1);
        document.getElementById('btnNextPage<%=rnd%>').disabled = (currentPage<%=rnd%> === totalPages);
    };

    const changePageToko<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if (direction === -1 && currentPage<%=rnd%> > 1) {
            currentPage<%=rnd%>--;
            loadDataToko<%=rnd%>();
        } else if (direction === 1 && currentPage<%=rnd%> < totalPages) {
            currentPage<%=rnd%>++;
            loadDataToko<%=rnd%>();
        }
    };


    // ==========================================
    // BAGIAN 3: LOGIKA FORM & DINAMIS UI (ADMIN ONLY)
    // ==========================================
    const bukaFormTambah<%=rnd%> = () => {
        if (!isAdmin<%=rnd%>) return; 
        
        document.getElementById('inputIdToko<%=rnd%>').value = '';
        document.getElementById('formToko<%=rnd%>').reset();
        
        // Reset defaults
        document.getElementById('inputStatusAktif<%=rnd%>').checked = true;
        document.getElementById('inputStatusAktif<%=rnd%>').dispatchEvent(new Event('change'));
        document.getElementById('inputBolehMelihatTokoLain<%=rnd%>').checked = false;
        document.getElementById('inputBolehTransaksiStokHabis<%=rnd%>').checked = false;
        setUnitUsahaTerpilih<%=rnd%>([]);
        
        document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-store text-primary me-2"></i><%=Common.getBahasaConfig("Tambah Toko Baru")%>';
        
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    };

    const tutupForm<%=rnd%> = () => {
        if (!isAdmin<%=rnd%>) return;
        document.getElementById('viewForm<%=rnd%>').style.display = 'none';
        document.getElementById('viewList<%=rnd%>').style.display = 'block';
        loadDataToko<%=rnd%>();
    };


    // ==========================================
    // BAGIAN 4: SIMPAN, EDIT, & HAPUS
    // ==========================================
    const simpanToko<%=rnd%> = async () => {
        if (!isAdmin<%=rnd%>) return;
        
        const idToko = document.getElementById('inputIdToko<%=rnd%>').value;
        const btnSimpan = document.getElementById('btnSimpan<%=rnd%>');
        
        // Object utama Toko.java
        const dataObj = {
            kode: document.getElementById('inputKode<%=rnd%>').value.trim(),
            nama: document.getElementById('inputNama<%=rnd%>').value.trim(),
            keterangan: document.getElementById('inputKeterangan<%=rnd%>').value.trim(),
            bolehMelihatTokolain: document.getElementById('inputBolehMelihatTokoLain<%=rnd%>').checked,
            bolehTransaksiStokHabis: document.getElementById('inputBolehTransaksiStokHabis<%=rnd%>').checked,
            unitUsahaJson: JSON.stringify(ambilUnitUsahaTerpilih<%=rnd%>()),
            aktif: document.getElementById('inputStatusAktif<%=rnd%>').checked
        };
        
        const payload = { 
            action: "simpanDataRinci", 
            class: masterModelClass<%=rnd%>, 
            data: dataObj
        };

        if (idToko !== '') {
            payload.id = idToko;
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
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data toko berhasil disimpan!")%>', 'bg-success text-white');
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

    const editToko<%=rnd%> = async (id) => {
        if (!isAdmin<%=rnd%>) return;
        
        const sql = 'SELECT id, kode, nama, keterangan, bolehmelihattokolain, boleh_transaksi_stok_habis, unit_usaha_json, aktif FROM koperasi.toko WHERE id = ' + id;
        const res = await fetchData<%=rnd%>(sql);

        if (res.length > 0) {
            const data = res[0];

            // Set Form Data
            document.getElementById('inputIdToko<%=rnd%>').value = data.id;
            document.getElementById('inputKode<%=rnd%>').value = data.kode || '';
            document.getElementById('inputNama<%=rnd%>').value = data.nama || '';
            document.getElementById('inputKeterangan<%=rnd%>').value = data.keterangan || '';

            const isBolehMelihat = (data.bolehmelihattokolain === true || data.bolehmelihattokolain === 'true' || data.bolehmelihattokolain === 't');
            document.getElementById('inputBolehMelihatTokoLain<%=rnd%>').checked = isBolehMelihat;

            const isStokHabis = (data.boleh_transaksi_stok_habis === true || data.boleh_transaksi_stok_habis === 'true' || data.boleh_transaksi_stok_habis === 't');
            document.getElementById('inputBolehTransaksiStokHabis<%=rnd%>').checked = isStokHabis;

            let unitTerpilih = [];
            try { unitTerpilih = JSON.parse(data.unit_usaha_json || '[]'); } catch (e) { unitTerpilih = []; }
            setUnitUsahaTerpilih<%=rnd%>(unitTerpilih);

            const isAktif = (data.aktif === null || data.aktif === true || data.aktif === 'true' || data.aktif === 't');
            const chkAktif = document.getElementById('inputStatusAktif<%=rnd%>');
            chkAktif.checked = isAktif;
            chkAktif.dispatchEvent(new Event('change')); 
            
            document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-edit text-warning me-2"></i><%=Common.getBahasaConfig("Ubah Data Toko")%>';
            
            document.getElementById('viewList<%=rnd%>').style.display = 'none';
            document.getElementById('viewForm<%=rnd%>').style.display = 'block';
        }
    };

    const hapusToko<%=rnd%> = async (id, nama) => {
        if (!isAdmin<%=rnd%>) return;

        // Menggunakan standar fungsi prosesDeleteData 
        if (typeof prosesDeleteData === 'function') {
            const confirmMsg = '<%=Common.getBahasaConfigJS("Hapus Toko")%> ' + nama + '?';
            prosesDeleteData(masterModelClass<%=rnd%>, id, function(){ 
                 if (document.querySelectorAll('#tabelDataToko<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) {
                     currentPage<%=rnd%>--;
                 }
                 loadDataToko<%=rnd%>();
            });
        } else {
            // Fallback manual fetch delete
            const isConfirm = confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus toko")%> ' + nama + '?');
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
                    if (document.querySelectorAll('#tabelDataToko<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) {
                        currentPage<%=rnd%>--;
                    }
                    loadDataToko<%=rnd%>();
                } else {
                    showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Data gagal dihapus. Pastikan toko tidak memiliki relasi produk/pengadaan.")%>', 'bg-danger text-white');
                }
            } catch (error) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal terhubung ke peladen.")%>', 'bg-danger text-white');
            }
        }
    };

    // ==========================================
    // BAGIAN 5: GENERATE PRODUK CONTOH PER UNIT USAHA (ADMIN + TOKO DEMO)
    // ==========================================
    // Popup checkbox unit usaha (pre-check dari unit_usaha_json toko; bila toko
    // belum memilih, admin WAJIB mencentang di sini -- padanan respons
    // perlu_pilih_unit_usaha server) + jumlah per unit (clamp server 250..100000),
    // lalu poll pos_demo_status sampai job latar selesai.
    let petaUnitUsahaToko<%=rnd%> = {};
    let pollGenerate<%=rnd%> = null;

    const tutupGenerateProduk<%=rnd%> = () => {
        if (pollGenerate<%=rnd%>) { clearInterval(pollGenerate<%=rnd%>); pollGenerate<%=rnd%> = null; }
        const ov = document.getElementById('overlayGenerate<%=rnd%>');
        if (ov) ov.remove();
    };

    const bukaGenerateProduk<%=rnd%> = (idToko, namaToko) => {
        if (!isAdmin<%=rnd%>) return;
        tutupGenerateProduk<%=rnd%>();
        let terpilih = [];
        try { terpilih = JSON.parse(petaUnitUsahaToko<%=rnd%>[idToko] || '[]'); } catch (e) { terpilih = []; }

        let cek = '';
        let grupTerakhir = null;
        katalogUnitUsaha<%=rnd%>.forEach((u) => {
            if (u.grup !== grupTerakhir) {
                grupTerakhir = u.grup;
                cek += '<div class="fw-bold small text-secondary mt-2 mb-1">' + u.grup + '</div>';
            }
            const checked = terpilih.indexOf(u.kode) >= 0 ? ' checked' : '';
            cek += '<div class="form-check form-check-inline">' +
                   '<input class="form-check-input cek-gen-unit-<%=rnd%>" type="checkbox" value="' + u.kode + '" id="cekGen<%=rnd%>' + u.kode + '"' + checked + '>' +
                   '<label class="form-check-label small" for="cekGen<%=rnd%>' + u.kode + '">' + u.label + '</label>' +
                   '</div>';
        });

        const ov = document.createElement('div');
        ov.id = 'overlayGenerate<%=rnd%>';
        ov.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,.45);z-index:1055;display:flex;align-items:center;justify-content:center;';
        ov.innerHTML =
            '<div class="bg-white rounded-4 shadow-lg p-4" style="width:640px;max-width:95vw;max-height:90vh;overflow-y:auto;">' +
            '  <h5 class="fw-bold mb-1"><i class="fas fa-magic text-success me-2"></i><%=Common.getBahasaConfigJS("Generate Produk Contoh")%> &mdash; ' + namaToko + '</h5>' +
            (terpilih.length === 0
                ? '  <div class="alert alert-warning py-2 small mb-2"><%=Common.getBahasaConfigJS("Toko ini belum memiliki unit usaha. Centang jenis usaha yang produk contohnya akan diimpor.")%></div>'
                : '  <div class="text-muted small mb-2"><%=Common.getBahasaConfigJS("Produk contoh dibuat sesuai unit usaha terpilih.")%></div>') +
            '  <div class="mb-2">' +
            '    <label class="form-label small fw-semibold mb-1"><%=Common.getBahasaConfigJS("Jumlah produk per unit usaha (250 - 100.000)")%></label>' +
            '    <input type="number" min="250" max="100000" value="250" class="form-control form-control-sm" id="inputJumlahGen<%=rnd%>" style="width:180px;">' +
            '  </div>' +
            '  <div class="border rounded-3 p-2 mb-3" style="max-height:280px;overflow-y:auto;" id="isiGenerate<%=rnd%>">' + cek + '</div>' +
            '  <div id="progressGenerate<%=rnd%>" class="d-none mb-3">' +
            '    <div class="small fw-semibold mb-1" id="tahapGenerate<%=rnd%>">...</div>' +
            '    <div class="progress" style="height:10px;"><div class="progress-bar progress-bar-striped progress-bar-animated" id="barGenerate<%=rnd%>" style="width:0%"></div></div>' +
            '    <div class="small text-muted mt-1" id="angkaGenerate<%=rnd%>"></div>' +
            '  </div>' +
            '  <div class="text-end">' +
            '    <button class="btn btn-light border rounded-pill px-4 me-2 fw-semibold" id="btnBatalGen<%=rnd%>" onclick="tutupGenerateProduk<%=rnd%>()"><%=Common.getBahasaConfigJS("Batal")%></button>' +
            '    <button class="btn btn-success rounded-pill px-4 fw-bold" id="btnMulaiGen<%=rnd%>" onclick="mulaiGenerateProduk<%=rnd%>(' + idToko + ')"><i class="fas fa-play me-2"></i><%=Common.getBahasaConfigJS("Generate")%></button>' +
            '  </div>' +
            '</div>';
        document.body.appendChild(ov);
    };

    const mulaiGenerateProduk<%=rnd%> = async (idToko) => {
        if (!isAdmin<%=rnd%>) return;
        const unit = Array.from(document.querySelectorAll('.cek-gen-unit-<%=rnd%>:checked')).map((c) => c.value);
        if (unit.length === 0) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Pilih minimal satu unit usaha.")%>', 'bg-danger text-white');
            return;
        }
        let jumlah = parseInt(document.getElementById('inputJumlahGen<%=rnd%>').value, 10) || 250;
        jumlah = Math.min(100000, Math.max(250, jumlah));
        const btn = document.getElementById('btnMulaiGen<%=rnd%>');
        btn.disabled = true;
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    action: 'pos_demo_seed_products_unit_usaha',
                    toko_id: idToko,
                    unit_usaha: unit,
                    jumlah_per_unit: jumlah,
                    konfirmasi: 'SEED-DEMO-PRODUK-UNIT-USAHA'
                })
            });
            const hasil = await res.json();
            if (hasil.perlu_pilih_unit_usaha === true) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Pilih minimal satu unit usaha dahulu.")%>', 'bg-danger text-white');
                btn.disabled = false;
                return;
            }
            if (hasil.status !== '00') {
                showToast<%=rnd%>(hasil.description || '<%=Common.getBahasaConfigJS("Gagal memulai generate.")%>', 'bg-danger text-white');
                btn.disabled = false;
                return;
            }
            document.getElementById('progressGenerate<%=rnd%>').classList.remove('d-none');
            document.getElementById('btnBatalGen<%=rnd%>').classList.add('d-none');
            pollGenerate<%=rnd%> = setInterval(async () => {
                try {
                    const r = await fetch('<%=Common.ROOT%>/Data', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ action: 'pos_demo_status', toko_id: idToko })
                    });
                    const st = await r.json();
                    const target = parseInt(st.target, 10) || 0;
                    const selesai = parseInt(st.selesai, 10) || 0;
                    document.getElementById('tahapGenerate<%=rnd%>').innerText = st.tahap || '...';
                    document.getElementById('angkaGenerate<%=rnd%>').innerText = selesai + ' / ' + (target || '...');
                    document.getElementById('barGenerate<%=rnd%>').style.width = (target ? Math.min(100, Math.round(selesai * 100 / target)) : 0) + '%';
                    if (st.berjalan === false) {
                        clearInterval(pollGenerate<%=rnd%>); pollGenerate<%=rnd%> = null;
                        document.getElementById('tahapGenerate<%=rnd%>').innerText = st.ringkasan || '<%=Common.getBahasaConfigJS("Selesai.")%>';
                        document.getElementById('barGenerate<%=rnd%>').style.width = '100%';
                        document.getElementById('barGenerate<%=rnd%>').classList.remove('progress-bar-animated');
                        btn.innerHTML = '<i class="fas fa-check me-2"></i><%=Common.getBahasaConfigJS("Tutup")%>';
                        btn.disabled = false;
                        btn.onclick = () => tutupGenerateProduk<%=rnd%>();
                    }
                } catch (e) {
                    // Sekali gagal poll bukan kegagalan job (job jalan di server).
                }
            }, 2000);
        } catch (e) {
            console.error(e);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi.")%>', 'bg-danger text-white');
            btn.disabled = false;
        }
    };

    // ==========================================
    // INISIALISASI HALAMAN
    // ==========================================
    document.addEventListener("DOMContentLoaded", () => {
        document.getElementById('searchToko<%=rnd%>').addEventListener("keydown", (e) => {
            if (e.key === "Enter") {
                e.preventDefault();
                resetAndLoadData<%=rnd%>(); 
            }
        });
        loadDataToko<%=rnd%>();
        muatKatalogUnitUsaha<%=rnd%>();
    });
</script>