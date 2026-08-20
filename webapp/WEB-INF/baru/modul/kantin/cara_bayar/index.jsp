<%@page import="ais.database.model.koperasi.CaraPembayaranKoperasi"%>
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

// Aturan: Pedagang HANYA BISA MELIHAT (Read-Only). Admin bisa semua.
boolean isAdmin = (toko == null);
%>

<div id="viewList<%=rnd%>" class="animate__animated animate__fadeInUp">
    <div class="row mb-4">
        <div class="col-12">
            <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
                
                <div class="card-header bg-white border-0 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-3 border-bottom border-light">
                    <div>
                        <h5 class="fw-bold text-dark mb-1">
                            <i class="fas fa-wallet text-primary me-2"></i><%=Common.getBahasaConfig("Manajemen Cara Pembayaran")%>
                        </h5>
                        <small class="text-muted"><%=Common.getBahasaConfig("Kelola master data metode atau cara pembayaran koperasi.")%></small>
                    </div>
                    
                    <div class="d-flex align-items-center gap-2 flex-wrap">
                        <div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;">
                            <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>
                            <input type="text" class="form-control border-start-0 ps-0 bg-white fw-medium" id="searchCaraBayar<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Kode atau Nama...")%>">
                        </div>
                        
                        <button class="btn btn-sm btn-outline-secondary rounded-pill px-3 shadow-sm fw-bold" onclick="resetAndLoadData<%=rnd%>()" title="<%=Common.getBahasaConfig("Muat Ulang Data")%>">
                            <i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Segarkan")%>
                        </button>
                        
                        <% if (isAdmin) { %>
                        <button class="btn btn-sm btn-primary rounded-pill px-4 shadow-sm fw-bold text-nowrap" onclick="bukaFormTambah<%=rnd%>()">
                            <i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Tambah Metode")%>
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
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-money-check-alt me-1"></i><%=Common.getBahasaConfig("Nama Pembayaran")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-align-left me-1"></i><%=Common.getBahasaConfig("Keterangan")%></th>
                                    <th class="fw-semibold text-uppercase small px-3" style="width: 120px;"><i class="fas fa-hand-holding-usd me-1"></i><%=Common.getBahasaConfig("Manual")%></th>
                                    <th class="fw-semibold text-uppercase small px-3" style="width: 120px;"><i class="fas fa-globe me-1"></i><%=Common.getBahasaConfig("Online")%></th>
                                    <th class="fw-semibold text-uppercase small px-3" style="width: 130px;"><i class="fas fa-wallet me-1"></i><%=Common.getBahasaConfig("Potong Saldo")%></th>
                                    <th class="fw-semibold text-uppercase small px-3" style="width: 120px;"><i class="fas fa-hand-holding-usd me-1"></i><%=Common.getBahasaConfig("Hutang")%></th>
                                    <th class="fw-semibold text-uppercase small px-3" style="width: 130px;"><i class="fas fa-coins me-1"></i><%=Common.getBahasaConfig("Kembalian")%></th>
                                    <th class="fw-semibold text-uppercase small px-3" style="width: 120px;"><i class="fas fa-toggle-on me-1"></i><%=Common.getBahasaConfig("Status")%></th>
                                    <% if (isAdmin) { %>
                                    <th class="fw-semibold text-uppercase small px-3 text-center" style="width: 100px;"><i class="fas fa-cogs"></i></th>
                                    <% } %>
                                </tr>
                            </thead>
                            <tbody id="tabelDataCaraBayar<%=rnd%>">
                                <tr><td colspan="<%= isAdmin ? 11 : 10 %>" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Memuat data...")%></span></td></tr>
                            </tbody>
                        </table>
                    </div>

                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3">
                        <div class="small fw-medium text-muted bg-light px-3 py-1 rounded-pill border" id="pagingInfoCaraBayar<%=rnd%>">
                        </div>
                        <nav>
                            <ul class="pagination pagination-sm mb-0 shadow-sm">
                                <li class="page-item">
                                    <button class="page-link text-primary rounded-start-pill px-3 fw-bold" id="btnPrevPage<%=rnd%>" onclick="changePageCaraBayar<%=rnd%>(-1)">
                                        <i class="fas fa-chevron-left me-1"></i><%=Common.getBahasaConfig("Sebelumnnya")%>
                                    </button>
                                </li>
                                <li class="page-item disabled">
                                    <span class="page-link bg-light text-dark px-3 fw-bold" id="pageNumberDisplay<%=rnd%>">1</span>
                                </li>
                                <li class="page-item">
                                    <button class="page-link text-primary rounded-end-pill px-3 fw-bold" id="btnNextPage<%=rnd%>" onclick="changePageCaraBayar<%=rnd%>(1)">
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
                            <i class="fas fa-wallet text-success me-2"></i><%=Common.getBahasaConfig("Formulir Cara Pembayaran")%>
                        </h5>
                        
                        <div class="form-check form-switch fs-5 mb-0">
                            <input class="form-check-input" type="checkbox" id="inputStatusAktif<%=rnd%>" checked style="cursor: pointer;">
                            <label class="form-check-label fs-6 fw-bold text-success" for="inputStatusAktif<%=rnd%>" id="labelStatusAktif<%=rnd%>"><%=Common.getBahasaConfig("Aktif")%></label>
                        </div>
                    </div>
                </div>
                <div class="card-body p-4">
                    <form id="formCaraBayar<%=rnd%>" onsubmit="event.preventDefault(); simpanCaraBayar<%=rnd%>();">
                        <input type="hidden" id="inputIdCaraBayar<%=rnd%>" value="">
                        
                        <div class="row g-3 mt-1">
                            
                            <div class="col-md-12">
                                <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Kode")%></label>
                                <div class="input-group input-group-sm shadow-sm">
                                    <span class="input-group-text bg-light border-end-0"><i class="fas fa-barcode text-muted"></i></span>
                                    <input type="text" class="form-control fw-bold border-start-0" id="inputKode<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: TUNAI, TRF_BCA, QRIS")%>">
                                </div>
                            </div>
                            
                            <div class="col-md-12">
                                <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Nama Pembayaran")%> <span class="text-danger">*</span></label>
                                <div class="input-group input-group-sm shadow-sm">
                                    <span class="input-group-text bg-light border-end-0"><i class="fas fa-font text-muted"></i></span>
                                    <input type="text" class="form-control fw-bold border-start-0" id="inputNama<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: Transfer Bank BCA")%>" required>
                                </div>
                            </div>
                            
                            <div class="col-md-6 pt-2">
                                <div class="form-check form-switch fs-6 border rounded-3 p-2 bg-light">
                                    <input class="form-check-input ms-1 me-2" type="checkbox" id="inputManual<%=rnd%>" checked style="cursor: pointer;">
                                    <label class="form-check-label fw-semibold text-dark small" for="inputManual<%=rnd%>"><%=Common.getBahasaConfig("Verifikasi Manual (Oleh Admin)")%></label>
                                </div>
                            </div>

                            <div class="col-md-6 pt-2">
                                <div class="form-check form-switch fs-6 border rounded-3 p-2 bg-light">
                                    <input class="form-check-input ms-1 me-2" type="checkbox" id="inputOnline<%=rnd%>" style="cursor: pointer;">
                                    <label class="form-check-label fw-semibold text-dark small" for="inputOnline<%=rnd%>"><%=Common.getBahasaConfig("Metode Pembayaran Online")%></label>
                                </div>
                            </div>

                            <!-- Ada Kembalian: gap-closure -- metode spt Tunai boleh dibayar LEBIH lalu
                                 dikembalikan sisanya; metode non-tunai (QRIS/Transfer/Voucher/Kasbon dst)
                                 harus PAS, tanpa kembalian. Default mengikuti nama (ilike "tunai") selama
                                 admin belum menyentuh checkbox ini secara manual -- lihat listener di bawah. -->
                            <div class="col-md-6 pt-2">
                                <div class="form-check form-switch fs-6 border rounded-3 p-2 bg-light">
                                    <input class="form-check-input ms-1 me-2" type="checkbox" id="inputAdaKembalian<%=rnd%>" style="cursor: pointer;">
                                    <label class="form-check-label fw-semibold text-dark small" for="inputAdaKembalian<%=rnd%>"><%=Common.getBahasaConfig("Ada Kembalian")%></label>
                                    <div class="small text-muted mt-1 ms-1"><%=Common.getBahasaConfig("Jika dicentang, kasir boleh menerima uang lebih dari total lalu mengembalikan sisanya. Tidak dicentang = pembayaran harus pas, tanpa kembalian.")%></div>
                                </div>
                            </div>

                            <!-- Memotong Deposit: dipisah dari "Verifikasi Manual" supaya metode bayar bisa
                                 SEKALIGUS diverifikasi admin DAN memotong saldo (mis. Voucher). Diberi warna
                                 peringatan karena efeknya langsung ke saldo anggota. -->
                            <div class="col-12 pt-2">
                                <div class="form-check form-switch fs-6 border border-warning rounded-3 p-2" style="background-color:#fff8e1;">
                                    <input class="form-check-input ms-1 me-2" type="checkbox" id="inputMemotongDeposit<%=rnd%>" style="cursor: pointer;">
                                    <label class="form-check-label fw-semibold text-dark small" for="inputMemotongDeposit<%=rnd%>"><%=Common.getBahasaConfig("Memotong Deposit / Saldo Anggota")%></label>
                                    <div class="small text-muted mt-1 ms-1"><%=Common.getBahasaConfig("Jika dicentang, belanja dengan metode ini mengurangi saldo anggota — walaupun Verifikasi Manual dinyalakan. Perubahan berlaku surut untuk transaksi yang sudah ada.")%></div>
                                </div>
                            </div>

                            <!-- Masuk Sebagai Hutang: metode pembayaran "ambil dari utang" -- transaksi yg
                                 memakainya TIDAK dianggap lunas, dicatat sbg piutang toko ke member (lihat
                                 tab "Mutasi Hutang" & batas "Maksimal Boleh Utang" di Tipe Member). -->
                            <div class="col-12 pt-2">
                                <div class="form-check form-switch fs-6 border border-danger rounded-3 p-2" style="background-color:#fdecea;">
                                    <input class="form-check-input ms-1 me-2" type="checkbox" id="inputMasukSebagaiHutang<%=rnd%>" style="cursor: pointer;">
                                    <label class="form-check-label fw-semibold text-dark small" for="inputMasukSebagaiHutang<%=rnd%>"><%=Common.getBahasaConfig("Masukkan sebagai Hutang")%></label>
                                    <div class="small text-muted mt-1 ms-1"><%=Common.getBahasaConfig("Jika dicentang, transaksi dengan metode ini dicatat sebagai HUTANG pelanggan (bukan lunas) — tercatat di tab \"Mutasi Hutang\" dan dibatasi oleh \"Maksimal Boleh Utang\" pada Tipe Member-nya.")%></div>
                                </div>
                            </div>

                            <div class="col-12 mt-3">
                                <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Keterangan Tambahan")%></label>
                                <textarea class="form-control text-muted shadow-sm" id="inputKeterangan<%=rnd%>" rows="3" placeholder="<%=Common.getBahasaConfig("Masukkan instruksi atau no. rekening jika ada...")%>"></textarea>
                            </div>


                            <div class="col-12 mt-3">
                                <div class="border rounded-3 p-3 bg-light-subtle">
                                    <div class="fw-bold text-secondary small mb-2"><i class="fas fa-book text-primary me-1"></i><%=Common.getBahasaConfig("Akuntansi")%></div>
                                    <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Akun Kas/Bank")%></label>
                                    <input type="text" class="form-control form-control-sm shadow-sm mb-1" id="cariAkunKas<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari kode atau nama akun...")%>" autocomplete="off">
                                    <select class="form-select form-select-sm shadow-sm" id="inputAkunKas<%=rnd%>"><option value=""><%=Common.getBahasaConfig("- Pilih Akun -")%></option></select>
                                    <div class="form-text small"><%=Common.getBahasaConfig("Akun yang didebet/dikredit saat metode ini dipakai pada jurnal. Kosongkan untuk memakai Akun Kas/Bank pada master Toko.")%></div>
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
    const masterModelClass<%=rnd%> = "ais.database.model.koperasi.CaraPembayaranKoperasi";
    
    // Inject status Admin
    const isAdmin<%=rnd%> = <%=isAdmin%>;
    const colSpan<%=rnd%> = isAdmin<%=rnd%> ? 11 : 10; // +1 "Potong Saldo", +1 "Hutang", +1 "Kembalian"
    // Gap-closure "Ada Kembalian" -- true selama admin BELUM menyentuh checkbox-nya sendiri di form
    // Tambah; sekali disentuh (atau saat form Ubah dibuka), berhenti auto-mengikuti field Nama.
    let kembalianDisentuhManual<%=rnd%> = false;

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

    // Gap-closure "Ada Kembalian" -- selama admin belum menyentuh checkbox-nya sendiri, ikuti field
    // Nama secara live (ilike "tunai" -> Ya). Sekali disentuh manual, berhenti auto-mengikuti.
    const uiAdaKembalian<%=rnd%> = document.getElementById('inputAdaKembalian<%=rnd%>');
    const uiNamaUntukKembalian<%=rnd%> = document.getElementById('inputNama<%=rnd%>');
    if (uiAdaKembalian<%=rnd%>) {
        uiAdaKembalian<%=rnd%>.addEventListener('change', function() {
            kembalianDisentuhManual<%=rnd%> = true;
        });
    }
    if (uiNamaUntukKembalian<%=rnd%>) {
        uiNamaUntukKembalian<%=rnd%>.addEventListener('input', function() {
            if (!kembalianDisentuhManual<%=rnd%> && uiAdaKembalian<%=rnd%>) {
                uiAdaKembalian<%=rnd%>.checked = this.value.trim().toLowerCase().indexOf('tunai') >= 0;
            }
        });
    }

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
        const keyword = document.getElementById('searchCaraBayar<%=rnd%>').value.trim().replace(/'/g, "''");
        let filters = " WHERE 1=1 ";
        if (keyword !== '') {
            filters += " AND (nama ILIKE '%" + keyword + "%' OR kode ILIKE '%" + keyword + "%' OR keterangan ILIKE '%" + keyword + "%') ";
        }
        return filters;
    };

    const resetAndLoadData<%=rnd%> = () => {
        currentPage<%=rnd%> = 1; 
        loadDataCaraBayar<%=rnd%>();
    };

    const loadDataCaraBayar<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataCaraBayar<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Mengambil data...")%></span></td></tr>';

        const sqlFilters = buildFilterSQL<%=rnd%>();
        const offset = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%>;

        // Query Total Baris untuk Paginasi
        const sqlCount = "SELECT COUNT(*) AS jumlah FROM koperasi.cara_pembayaran_koperasi " + sqlFilters + ";";
        
        // Asumsi struktur DB column names (berdasarkan Hibernate default naming)
        const sqlData = "SELECT id, kode, nama, keterangan, manual, online, memotong_deposit, masuk_sebagai_hutang, aktif, " +
            "COALESCE(ada_kembalian, nama ILIKE '%tunai%') AS ada_kembalian " +
            "FROM koperasi.cara_pembayaran_koperasi " + sqlFilters + " ORDER BY nama ASC LIMIT " + limitPerPage<%=rnd%> + " OFFSET " + offset + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetchData<%=rnd%>(sqlCount),
                fetchData<%=rnd%>(sqlData)
            ]);

            totalRecords<%=rnd%> = (resCount && resCount[0]) ? parseInt(resCount[0].jumlah || 0) : 0;
            const recordList = resData || [];

            let htmlList = '';

            if (recordList.length === 0) {
                htmlList = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center text-muted py-5"><i class="fas fa-wallet fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Tidak ada metode pembayaran ditemukan.")%></td></tr>';
            } else {
                let no = offset + 1; 
                recordList.forEach((row) => {

                    const isStatusAktif = (row.aktif === null || row.aktif === true || row.aktif === 't' || row.aktif === 'true');
                    const badgeStatus = isStatusAktif ? '<span class="badge bg-success bg-opacity-10 text-success border border-success px-2 py-1"><i class="fas fa-check-circle me-1"></i>Aktif</span>' : '<span class="badge bg-danger bg-opacity-10 text-danger border border-danger px-2 py-1"><i class="fas fa-times-circle me-1"></i>Non-Aktif</span>';

                    const isManual = (row.manual === null || row.manual === true || row.manual === 't' || row.manual === 'true');
                    const badgeManual = isManual ? '<span class="text-warning fw-bold"><i class="fas fa-hand-paper me-1"></i>Ya</span>' : '<span class="text-secondary fw-bold"><i class="fas fa-robot me-1"></i>Tidak</span>';

                    const isOnline = (row.online === true || row.online === 't' || row.online === 'true');
                    const badgeOnline = isOnline ? '<span class="text-primary fw-bold"><i class="fas fa-wifi me-1"></i>Ya</span>' : '<span class="text-secondary fw-bold"><i class="fas fa-store-alt me-1"></i>Tidak</span>';

                    // Efektif memotong saldo bila: TIDAK manual (perilaku lama) ATAU ditandai memotong.
                    // Ditampilkan apa adanya supaya admin melihat dampak nyata konfigurasinya, bukan
                    // sekadar nilai kolom barunya saja.
                    const isPotongFlag = (row.memotong_deposit === true || row.memotong_deposit === 't' || row.memotong_deposit === 'true');
                    const efektifPotong = (!isManual) || isPotongFlag;
                    const badgePotong = efektifPotong
                        ? '<span class="badge bg-warning bg-opacity-25 text-dark border border-warning px-2 py-1"><i class="fas fa-wallet me-1"></i>Ya</span>'
                        : '<span class="text-secondary fw-bold"><i class="fas fa-minus me-1"></i>Tidak</span>';

                    const isHutang = (row.masuk_sebagai_hutang === true || row.masuk_sebagai_hutang === 't' || row.masuk_sebagai_hutang === 'true');
                    const badgeHutang = isHutang
                        ? '<span class="badge bg-danger bg-opacity-25 text-danger border border-danger px-2 py-1"><i class="fas fa-hand-holding-usd me-1"></i>Ya</span>'
                        : '<span class="text-secondary fw-bold"><i class="fas fa-minus me-1"></i>Tidak</span>';

                    const isKembalian = (row.ada_kembalian === true || row.ada_kembalian === 't' || row.ada_kembalian === 'true');
                    const badgeKembalian = isKembalian
                        ? '<span class="text-success fw-bold"><i class="fas fa-coins me-1"></i>Ya</span>'
                        : '<span class="text-secondary fw-bold"><i class="fas fa-minus me-1"></i>Tidak</span>';

                    htmlList += '<tr>' +
                            '<td class="text-center text-muted fw-medium">' + (no++) + '</td>' +
                            '<td class="text-start fw-bold text-secondary">' + (row.kode || '-') + '</td>' +
                            '<td class="text-start fw-bold text-dark">' + (row.nama || '-') + '</td>' +
                            '<td class="text-start text-muted small">' + (row.keterangan || '-') + '</td>' +
                            '<td class="text-center">' + badgeManual + '</td>' +
                            '<td class="text-center">' + badgeOnline + '</td>' +
                            '<td class="text-center">' + badgePotong + '</td>' +
                            '<td class="text-center">' + badgeHutang + '</td>' +
                            '<td class="text-center">' + badgeKembalian + '</td>' +
                            '<td class="text-center">' + badgeStatus + '</td>';
                    
                    // Render kolom aksi hanya jika user adalah admin
                    if (isAdmin<%=rnd%>) {
                        htmlList += '<td class="text-center text-nowrap">' +
                                        '<button class="btn btn-sm btn-outline-warning text-dark shadow-sm px-2 me-1 fw-bold" onclick="editCaraBayar<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Ubah Data")%>"><i class="fas fa-edit"></i></button>' +
                                        '<button class="btn btn-sm btn-outline-danger shadow-sm px-2 fw-bold" onclick="hapusCaraBayar<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Hapus Data")%>"><i class="fas fa-trash-alt"></i></button>' +
                                    '</td>';
                    }
                            
                    htmlList += '</tr>';
                });
            }
            tbody.innerHTML = htmlList;
            updatePaginationUI<%=rnd%>();

        } catch (error) {
            console.error("Gagal load tabel metode pembayaran:", error);
            tbody.innerHTML = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal menarik data. Silakan muat ulang.")%></td></tr>';
        }
    };

    const updatePaginationUI<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumberDisplay<%=rnd%>').innerText = currentPage<%=rnd%> + " / " + totalPages;
        
        const startIdx = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%> + 1;
        const endIdx = Math.min(currentPage<%=rnd%> * limitPerPage<%=rnd%>, totalRecords<%=rnd%>);
        const startInfo = totalRecords<%=rnd%> > 0 ? startIdx : 0;
        
        document.getElementById('pagingInfoCaraBayar<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startInfo + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari")%> <b class="text-dark">' + totalRecords<%=rnd%> + '</b> <%=Common.getBahasaConfig("data")%>';
        
        document.getElementById('btnPrevPage<%=rnd%>').disabled = (currentPage<%=rnd%> === 1);
        document.getElementById('btnNextPage<%=rnd%>').disabled = (currentPage<%=rnd%> === totalPages);
    };

    const changePageCaraBayar<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if (direction === -1 && currentPage<%=rnd%> > 1) {
            currentPage<%=rnd%>--;
            loadDataCaraBayar<%=rnd%>();
        } else if (direction === 1 && currentPage<%=rnd%> < totalPages) {
            currentPage<%=rnd%>++;
            loadDataCaraBayar<%=rnd%>();
        }
    };


    // ==========================================
    // BAGIAN 3: LOGIKA FORM & DINAMIS UI (ADMIN ONLY)
    // ==========================================
    const bukaFormTambah<%=rnd%> = () => {
        if (!isAdmin<%=rnd%>) return; // Penjagaan ganda
        
        document.getElementById('inputIdCaraBayar<%=rnd%>').value = '';
        document.getElementById('formCaraBayar<%=rnd%>').reset();
        
        // Reset defaults
        document.getElementById('inputStatusAktif<%=rnd%>').checked = true;
        document.getElementById('inputStatusAktif<%=rnd%>').dispatchEvent(new Event('change'));
        document.getElementById('inputManual<%=rnd%>').checked = true;
        document.getElementById('inputOnline<%=rnd%>').checked = false; // Default online false
        document.getElementById('inputMemotongDeposit<%=rnd%>').checked = false; // Default TIDAK memotong saldo
        document.getElementById('inputMasukSebagaiHutang<%=rnd%>').checked = false; // Default BUKAN hutang
        document.getElementById('inputAdaKembalian<%=rnd%>').checked = false; // Nama masih kosong -- mengikuti field Nama begitu diketik
        kembalianDisentuhManual<%=rnd%> = false;

        document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-wallet text-primary me-2"></i><%=Common.getBahasaConfig("Tambah Metode Pembayaran")%>';
        
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    };

    const tutupForm<%=rnd%> = () => {
        if (!isAdmin<%=rnd%>) return;
        document.getElementById('viewForm<%=rnd%>').style.display = 'none';
        document.getElementById('viewList<%=rnd%>').style.display = 'block';
        loadDataCaraBayar<%=rnd%>();
    };


    // ==========================================
    // BAGIAN 4: SIMPAN, EDIT, & HAPUS
    // ==========================================
    const simpanCaraBayar<%=rnd%> = async () => {
        if (!isAdmin<%=rnd%>) return;
        
        const idBayar = document.getElementById('inputIdCaraBayar<%=rnd%>').value;
        const btnSimpan = document.getElementById('btnSimpan<%=rnd%>');
        
        // Object utama CaraPembayaranKoperasi.java
    // Pemilih akun bercari (kode & nama). Bagan akun bisa ratusan baris sehingga dropdown
    // polos sulit dipakai; akun yang sedang terpilih selalu ikut tampil saat daftar tersaring.
    const ID_AKUN<%=rnd%> = ['inputAkunKas<%=rnd%>'];
    let akunDaftar<%=rnd%> = [];
    const escAkun<%=rnd%> = (t) => (t + '').replace(/</g, '&lt;').replace(/"/g, '&quot;');
    const opsiAkunHtml<%=rnd%> = (kata, nilai) => {
        const kunci = (kata || '').trim().toLowerCase().split(/\s+/).filter((k) => k.length > 0);
        let html = '<option value=""><%=Common.getBahasaConfigJS("- Pilih Akun -")%></option>';
        (akunDaftar<%=rnd%> || []).forEach((a) => {
            const teks = ((a.kode || '') + ' ' + (a.nama || '')).toLowerCase();
            let cocok = true;
            kunci.forEach((k) => { if (teks.indexOf(k) < 0) { cocok = false; } });
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
        const box = document.getElementById(sid.replace('input', 'cari'));
        if (box) { box.value = ''; }
        const v = (val === null || val === undefined) ? '' : (val + '');
        el.innerHTML = opsiAkunHtml<%=rnd%>('', v);
        el.value = v;
    };
    loadAkunOptions<%=rnd%>();

        const dataObj = {
            kode: document.getElementById('inputKode<%=rnd%>').value.trim(),
            nama: document.getElementById('inputNama<%=rnd%>').value.trim(),
            keterangan: document.getElementById('inputKeterangan<%=rnd%>').value.trim(),
            akun: document.getElementById('inputAkunKas<%=rnd%>').value || null,
            manual: document.getElementById('inputManual<%=rnd%>').checked,
            online: document.getElementById('inputOnline<%=rnd%>').checked, // Parameter Online
            memotongDeposit: document.getElementById('inputMemotongDeposit<%=rnd%>').checked, // Kurangi saldo anggota
            masukSebagaiHutang: document.getElementById('inputMasukSebagaiHutang<%=rnd%>').checked, // Catat sbg hutang
            adaKembalian: document.getElementById('inputAdaKembalian<%=rnd%>').checked, // Boleh ada kembalian
            aktif: document.getElementById('inputStatusAktif<%=rnd%>').checked
        };
        
        const payload = { 
            action: "simpanDataRinci", 
            class: masterModelClass<%=rnd%>, 
            data: dataObj
        };

        if (idBayar !== '') {
            payload.id = idBayar;
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
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Cara pembayaran berhasil disimpan!")%>', 'bg-success text-white');
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

    const editCaraBayar<%=rnd%> = async (id) => {
        if (!isAdmin<%=rnd%>) return;
        
        const sql = 'SELECT id, kode, nama, keterangan, manual, online, memotong_deposit, masuk_sebagai_hutang, aktif, akun, ' +
            "COALESCE(ada_kembalian, nama ILIKE '%tunai%') AS ada_kembalian " +
            'FROM koperasi.cara_pembayaran_koperasi WHERE id = ' + id;
        const res = await fetchData<%=rnd%>(sql);
        
        if (res.length > 0) {
            const data = res[0];
            
            // Set Form Data
            document.getElementById('inputIdCaraBayar<%=rnd%>').value = data.id;
            document.getElementById('inputKode<%=rnd%>').value = data.kode || '';
            document.getElementById('inputNama<%=rnd%>').value = data.nama || '';
            setAkunSelect<%=rnd%>('inputAkunKas<%=rnd%>', data.akun);
            document.getElementById('inputKeterangan<%=rnd%>').value = data.keterangan || '';
            
            const isManual = (data.manual === null || data.manual === true || data.manual === 'true' || data.manual === 't');
            document.getElementById('inputManual<%=rnd%>').checked = isManual;

            const isOnline = (data.online === true || data.online === 'true' || data.online === 't');
            document.getElementById('inputOnline<%=rnd%>').checked = isOnline;

            // NULL sengaja dianggap FALSE (tidak memotong), selaras getMemotongDeposit() di entity:
            // metode bayar lama tidak boleh mendadak ikut memotong saldo.
            const isPotongDeposit = (data.memotong_deposit === true || data.memotong_deposit === 'true' || data.memotong_deposit === 't');
            document.getElementById('inputMemotongDeposit<%=rnd%>').checked = isPotongDeposit;

            const isMasukHutang = (data.masuk_sebagai_hutang === true || data.masuk_sebagai_hutang === 'true' || data.masuk_sebagai_hutang === 't');
            document.getElementById('inputMasukSebagaiHutang<%=rnd%>').checked = isMasukHutang;

            // SQL sudah pakai COALESCE(ada_kembalian, nama ILIKE '%tunai%') jadi nilainya sudah final
            // (tidak perlu fallback lagi di sini) -- begitu form Ubah dibuka, berhenti auto-mengikuti
            // field Nama (nilai yang dimuat inilah sumber kebenarannya, bukan diturunkan ulang).
            const isKembalian = (data.ada_kembalian === true || data.ada_kembalian === 'true' || data.ada_kembalian === 't');
            document.getElementById('inputAdaKembalian<%=rnd%>').checked = isKembalian;
            kembalianDisentuhManual<%=rnd%> = true;

            const isAktif = (data.aktif === null || data.aktif === true || data.aktif === 'true' || data.aktif === 't');
            const chkAktif = document.getElementById('inputStatusAktif<%=rnd%>');
            chkAktif.checked = isAktif;
            chkAktif.dispatchEvent(new Event('change')); 
            
            document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-edit text-warning me-2"></i><%=Common.getBahasaConfig("Ubah Metode Pembayaran")%>';
            
            document.getElementById('viewList<%=rnd%>').style.display = 'none';
            document.getElementById('viewForm<%=rnd%>').style.display = 'block';
        }
    };

    const hapusCaraBayar<%=rnd%> = async (id) => {
        if (!isAdmin<%=rnd%>) return;

        // Menggunakan standar fungsi prosesDeleteData jika ada, atau fetch custom
        if (typeof prosesDeleteData === 'function') {
            prosesDeleteData(masterModelClass<%=rnd%>, id, function(){ 
                 if (document.querySelectorAll('#tabelDataCaraBayar<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) {
                     currentPage<%=rnd%>--;
                 }
                 loadDataCaraBayar<%=rnd%>();
            });
        } else {
            // Fallback manual fetch delete
            const isConfirm = confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus metode pembayaran ini?")%>');
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
                    if (document.querySelectorAll('#tabelDataCaraBayar<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) {
                        currentPage<%=rnd%>--;
                    }
                    loadDataCaraBayar<%=rnd%>();
                } else {
                    showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Data gagal dihapus.")%>', 'bg-danger text-white');
                }
            } catch (error) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal terhubung ke peladen.")%>', 'bg-danger text-white');
            }
        }
    };

    // ==========================================
    // INISIALISASI HALAMAN
    // ==========================================
    document.addEventListener("DOMContentLoaded", () => {
        document.getElementById('searchCaraBayar<%=rnd%>').addEventListener("keydown", (e) => {
            if (e.key === "Enter") {
                e.preventDefault();
                resetAndLoadData<%=rnd%>(); 
            }
        });
        loadDataCaraBayar<%=rnd%>();
    });
</script>