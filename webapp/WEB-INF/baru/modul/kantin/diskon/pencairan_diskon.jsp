<%@page import="java.net.URLEncoder"%>
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

// Aturan: Admin bisa semua toko. Pedagang HANYA tokonya sendiri.
boolean isAdmin = (tokoLogin == null);
String idTokoAktif = tokoLogin != null ? String.valueOf(tokoLogin.getId()) : "";
String namaTokoAktif = tokoLogin != null ? tokoLogin.getNama() : "";
%>

<div id="viewList<%=rnd%>" class="animate__animated animate__fadeInUp">
    <div class="row mb-4">
        <div class="col-12">
            <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
                
                <div class="card-header bg-white border-0 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-3 border-bottom border-light">
                    <div>
                        <h5 class="fw-bold text-dark mb-1">
                            <i class="fas fa-hand-holding-usd text-primary me-2"></i>
                            <% if (isAdmin) { %>
                                <%=Common.getBahasaConfig("Pencairan Saldo Diskon (Semua Toko)")%>
                            <% } else { %>
                                <%=Common.getBahasaConfig("Pencairan Diskon - ")%> <span class="text-primary"><%=namaTokoAktif%></span>
                            <% } %>
                        </h5>
                        <small class="text-muted"><%=Common.getBahasaConfig("Catatan riwayat pencairan atau penarikan saldo diskon (cashback) milik anggota koperasi.")%></small>
                    </div>
                    
                    <div class="d-flex align-items-center gap-2 flex-wrap">
                        <button class="btn btn-success rounded-pill px-3 shadow-sm fw-bold" onclick="downloadExcelPencairan<%=rnd%>()" title="<%=Common.getBahasaConfig("Unduh file Excel")%>">
                            <i class="fas fa-file-excel me-1"></i><%=Common.getBahasaConfig("Unduh Excel")%>
                        </button>
                        <button class="btn btn-primary rounded-pill px-4 shadow-sm fw-bold text-nowrap" onclick="bukaFormTambah<%=rnd%>()">
                            <i class="fas fa-plus-circle me-1"></i><%=Common.getBahasaConfig("Catat Pencairan")%>
                        </button>
                    </div>
                </div>
                
                <div class="card-body p-4 pt-3">
                    
                    <div class="bg-light p-3 rounded-3 border shadow-sm mb-4">
                        <div class="row g-2 align-items-end">
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Pencarian")%></label>
                                <input type="text" class="form-control form-control-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterKeyword<%=rnd%>" placeholder="<%=Common.getBahasaConfig("No. Ref atau Nama Member...")%>">
                            </div>
                            
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Status Pencairan")%></label>
                                <select class="form-select form-select-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterStatus<%=rnd%>">
                                    <option value=""><%=Common.getBahasaConfig("- Semua Status -")%></option>
                                    <option value="BERHASIL"><%=Common.getBahasaConfig("BERHASIL")%></option>
                                    <option value="PENDING"><%=Common.getBahasaConfig("PENDING")%></option>
                                    <option value="DITOLAK"><%=Common.getBahasaConfig("DITOLAK")%></option>
                                </select>
                            </div>

                            <% if (isAdmin) { %>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Filter Toko")%></label>
                                <select class="form-select form-select-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterToko<%=rnd%>">
                                    <option value=""><%=Common.getBahasaConfig("- Semua Toko -")%></option>
                                </select>
                            </div>
                            <% } else { %>
                                <input type="hidden" id="filterToko<%=rnd%>" value="<%=idTokoAktif%>">
                            <% } %>

                            <div class="col-md-auto ms-auto">
                                <button class="btn btn-sm btn-secondary fw-bold px-3 shadow-sm" onclick="triggerSearchPencairan<%=rnd%>()">
                                    <i class="fas fa-search me-1"></i> <%=Common.getBahasaConfig("Saring")%>
                                </button>
                            </div>
                        </div>
                    </div>

                    <div class="table-responsive border rounded-3 shadow-sm bg-white">
                        <table class="table table-hover table-striped align-middle mb-0">
                            <thead class="table-dark text-center align-middle">
                                <tr>
                                    <th class="fw-semibold text-uppercase small px-3 py-3" style="width: 50px;">#</th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Waktu & No. Ref")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Anggota (Member)")%></th>
                                    <% if (isAdmin) { %>
                                        <th class="text-start fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Toko")%></th>
                                    <% } %>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Cara Pencairan")%></th>
                                    <th class="text-end fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Nominal (Rp)")%></th>
                                    <th class="fw-semibold text-uppercase small px-3 text-center"><%=Common.getBahasaConfig("Status")%></th>
                                    <th class="fw-semibold text-uppercase small px-3 text-center" style="width: 100px;"><i class="fas fa-cogs"></i></th>
                                </tr>
                            </thead>
                            <tbody id="tabelDataPencairan<%=rnd%>">
                                <tr><td colspan="<%= isAdmin ? 8 : 7 %>" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Memuat data...")%></span></td></tr>
                            </tbody>
                        </table>
                    </div>

                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mt-4">
                        <div class="small fw-medium text-muted bg-light px-3 py-1 rounded-pill border" id="pagingInfoPencairan<%=rnd%>"></div>
                        <nav>
                            <ul class="pagination pagination-sm mb-0 shadow-sm">
                                <li class="page-item"><button class="page-link text-primary rounded-start-pill px-3 fw-bold" id="btnPrevPage<%=rnd%>" onclick="changePagePencairan<%=rnd%>(-1)"><i class="fas fa-chevron-left"></i></button></li>
                                <li class="page-item disabled"><span class="page-link bg-light text-dark px-3 fw-bold" id="pageNumberDisplay<%=rnd%>">1</span></li>
                                <li class="page-item"><button class="page-link text-primary rounded-end-pill px-3 fw-bold" id="btnNextPage<%=rnd%>" onclick="changePagePencairan<%=rnd%>(1)"><i class="fas fa-chevron-right"></i></button></li>
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
                            <i class="fas fa-hand-holding-usd text-success me-2"></i><%=Common.getBahasaConfig("Form Pencairan Diskon")%>
                        </h5>
                    </div>
                </div>
                <div class="card-body p-4">
                    <form id="formPencairan<%=rnd%>" onsubmit="event.preventDefault(); savePencairan<%=rnd%>();">
                        <input type="hidden" id="inputIdPencairan<%=rnd%>" value="">
                        
                        <div class="row g-4 mt-1">
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("No. Referensi")%></label>
                                <input type="text" class="form-control fw-bold bg-light" id="inputKodePencairan<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Otomatis jika kosong...")%>">
                            </div>

                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Waktu Pencairan")%> <span class="text-danger">*</span></label>
                                <input type="datetime-local" class="form-control" id="inputWaktuPencairan<%=rnd%>" required>
                            </div>

                            <div class="col-md-12">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Anggota Koperasi / Member")%> <span class="text-danger">*</span></label>
                                <div class="input-group shadow-sm">
                                    <span class="input-group-text bg-white border-end-0"><i class="fas fa-user-tag text-primary"></i></span>
                                    <input type="text" class="form-control border-start-0 fw-bold" list="listAnggota<%=rnd%>" id="inputCariAnggota<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik nama atau kode anggota...")%>" autocomplete="off" required>
                                </div>
                                <datalist id="listAnggota<%=rnd%>"></datalist>
                                <input type="hidden" id="idAnggotaSelected<%=rnd%>" value="">
                                <div class="form-text text-info fw-bold mt-1" id="infoSaldoCashback<%=rnd%>" style="display: none;">
                                    <i class="fas fa-wallet me-1"></i>Sisa Saldo Cashback: <span id="labelSaldoCashback<%=rnd%>">Rp 0</span>
                                </div>
                                <input type="hidden" id="hiddenSaldoCashback<%=rnd%>" value="0">
                            </div>

                            <% if (isAdmin) { %>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Toko / Kasir Eksekutor")%></label>
                                <select class="form-select fw-semibold" id="inputTokoForm<%=rnd%>">
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Toko (Opsional) --")%></option>
                                </select>
                            </div>
                            <% } else { %>
                                <input type="hidden" id="inputTokoForm<%=rnd%>" value="<%=idTokoAktif%>">
                            <% } %>

                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Cara Pencairan")%> <span class="text-danger">*</span></label>
                                <select class="form-select fw-semibold shadow-sm" id="inputCaraBayar<%=rnd%>" required>
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Cara --")%></option>
                                </select>
                            </div>

                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tgl. Kedaluwarsa (Jika Berupa Topup)")%></label>
                                <div class="input-group shadow-sm">
                                    <span class="input-group-text bg-white border-end-0"><i class="far fa-calendar-times text-danger"></i></span>
                                    <input type="date" class="form-control border-start-0 fw-bold" id="inputTanggalExpired<%=rnd%>">
                                </div>
                            </div>

                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nominal (Rp)")%> <span class="text-danger">*</span></label>
                                <div class="input-group shadow-sm">
                                    <span class="input-group-text bg-light border-end-0">Rp</span>
                                    <input type="number" class="form-control border-start-0 text-success fw-bold fs-5" id="inputNominal<%=rnd%>" min="1" required placeholder="0">
                                </div>
                            </div>

                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Status Transaksi")%> <span class="text-danger">*</span></label>
                                <select class="form-select fw-bold border-secondary shadow-sm" id="inputStatus<%=rnd%>" required>
                                    <option value="BERHASIL" class="text-success"><%=Common.getBahasaConfig("BERHASIL")%></option>
                                    <option value="PENDING" class="text-warning"><%=Common.getBahasaConfig("PENDING")%></option>
                                    <option value="DITOLAK" class="text-danger"><%=Common.getBahasaConfig("DITOLAK")%></option>
                                </select>
                            </div>

                            <div class="col-12 mt-3">
                                <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Keterangan / Catatan")%></label>
                                <textarea class="form-control" id="inputKeterangan<%=rnd%>" rows="2" placeholder="<%=Common.getBahasaConfig("Contoh: Dicairkan tunai oleh kasir depan...")%>"></textarea>
                            </div>

                            <div class="col-12 mt-4 text-end border-top pt-3">
                                <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold border shadow-sm" onclick="tutupForm<%=rnd%>()"><i class="fas fa-times text-danger me-2"></i><%=Common.getBahasaConfig("Batal")%></button>
                                <button type="submit" class="btn btn-success px-4 rounded-pill shadow-sm fw-bold" id="btnSimpan<%=rnd%>">
                                    <i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Pencairan")%>
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
    const masterModelClass<%=rnd%> = "ais.database.model.koperasi.PencairanDiskon";
    const isAdmin<%=rnd%> = <%=isAdmin%>;
    const idTokoLogin<%=rnd%> = '<%=idTokoAktif%>';
    const colSpan<%=rnd%> = isAdmin<%=rnd%> ? 8 : 7;

    let arrDataAnggota<%=rnd%> = [];
    let currentPage<%=rnd%> = 1;
    const limitPerPage<%=rnd%> = 10;
    let totalRecords<%=rnd%> = 0;

    const formatRp<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);
    };

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

    // ==========================================
    // FUNGSI CEK SALDO MEMBER
    // ==========================================
    const cekSaldoMember<%=rnd%> = async (anggotaId) => {
        if (!anggotaId || anggotaId === "") {
            document.getElementById('infoSaldoCashback<%=rnd%>').style.display = 'none';
            document.getElementById('hiddenSaldoCashback<%=rnd%>').value = 0;
            return;
        }
        
        const idPencairan = document.getElementById('inputIdPencairan<%=rnd%>').value;
        let exceptCondition = "";
        if (idPencairan !== "") {
            exceptCondition = " AND id != " + idPencairan;
        }

        // Kalkulasi: Total Pendapatan Cashback dikurangi Total Penarikan
        const sqlCekSaldo = "SELECT " +
            "(COALESCE((SELECT SUM(totalcashback) FROM koperasi.pembelian_anggota_koperasi WHERE anggota_koperasi = " + anggotaId + "), 0) - " +
            "COALESCE((SELECT SUM(nominal_cair) FROM koperasi.pencairan_diskon WHERE anggota_koperasi = " + anggotaId + " AND status IN ('BERHASIL', 'PENDING')" + exceptCondition + "), 0)) as sisa_saldo";
        
        try {
            const resSaldo = await fetchData<%=rnd%>(sqlCekSaldo);
            let sisaSaldo = 0;
            if (resSaldo && resSaldo.length > 0 && resSaldo[0].sisa_saldo) {
                sisaSaldo = parseFloat(resSaldo[0].sisa_saldo);
            }
            
            document.getElementById('hiddenSaldoCashback<%=rnd%>').value = sisaSaldo;
            document.getElementById('labelSaldoCashback<%=rnd%>').innerText = formatRp<%=rnd%>(sisaSaldo);
            
            const infoEl = document.getElementById('infoSaldoCashback<%=rnd%>');
            infoEl.style.display = 'block';
            
            if(sisaSaldo <= 0) {
                infoEl.className = "form-text text-danger fw-bold mt-1";
            } else {
                infoEl.className = "form-text text-info fw-bold mt-1";
            }
        } catch(e) {
            console.error(e);
        }
    };

    // ==========================================
    // INIT COMBO BOX & DATALIST
    // ==========================================
    const loadMasterDataCombo<%=rnd%> = async () => {
        // Combo Toko (Jika Admin)
        if (isAdmin<%=rnd%>) {
            const sqlToko = "SELECT id, nama FROM koperasi.toko WHERE aktif = true ORDER BY nama ASC";
            const resToko = await fetchData<%=rnd%>(sqlToko);
            let optFilterToko = '<option value=""><%=Common.getBahasaConfig("- Semua Toko -")%></option>';
            let optFormToko = '<option value=""><%=Common.getBahasaConfig("-- Pilih Toko (Opsional) --")%></option>';
            resToko.forEach(item => {
                const opt = '<option value="' + item.id + '">' + item.nama + '</option>';
                optFilterToko += opt;
                optFormToko += opt;
            });
            document.getElementById('filterToko<%=rnd%>').innerHTML = optFilterToko;
            const formToko = document.getElementById('inputTokoForm<%=rnd%>');
            if (formToko) formToko.innerHTML = optFormToko;
        }

        // Combo Cara Pembayaran
        const sqlCara = "SELECT id, nama FROM koperasi.cara_pembayaran_koperasi WHERE aktif = true ORDER BY nama ASC";
        fetchData<%=rnd%>(sqlCara).then(res => {
            let optHtml = '<option value=""><%=Common.getBahasaConfig("-- Pilih Cara Pencairan --")%></option>';
            res.forEach(item => { optHtml += '<option value="' + item.id + '">' + item.nama + '</option>'; });
            document.getElementById('inputCaraBayar<%=rnd%>').innerHTML = optHtml;
        });

        // Load Data Anggota Koperasi untuk Datalist Searchbox
        const sqlAnggota = "SELECT id, nama, kode_identitas FROM koperasi.anggota_koperasi WHERE aktif = true ORDER BY nama ASC";
        fetchData<%=rnd%>(sqlAnggota).then(res => {
            let listHtml = '';
            arrDataAnggota<%=rnd%> = [];
            res.forEach(item => { 
                const iden = item.kode_identitas ? item.kode_identitas : '-';
                const label = item.nama + ' (' + iden + ')';
                arrDataAnggota<%=rnd%>.push({ id: item.id, label: label });
                listHtml += '<option value="' + label + '" data-id="' + item.id + '">'; 
            });
            document.getElementById('listAnggota<%=rnd%>').innerHTML = listHtml;
        });
    };

    // Auto-select Anggota Koperasi & Check Saldo
    document.getElementById('inputCariAnggota<%=rnd%>').addEventListener('change', function() {
        const val = this.value;
        const match = arrDataAnggota<%=rnd%>.find(a => a.label === val);
        const memberId = match ? match.id : "";
        document.getElementById('idAnggotaSelected<%=rnd%>').value = memberId;
        
        cekSaldoMember<%=rnd%>(memberId);
    });


    // ==========================================
    // READ (TAMPILKAN TABEL)
    // ==========================================
    const buildFilterSQL<%=rnd%> = () => {
        const keyword = document.getElementById('filterKeyword<%=rnd%>').value.trim().replace(/'/g, "''");
        const status = document.getElementById('filterStatus<%=rnd%>').value;
        const elToko = document.getElementById('filterToko<%=rnd%>');
        const toko = elToko ? elToko.value : "";

        let filters = " WHERE 1=1 ";
        
        if (keyword !== '') {
            filters += " AND (a.kode_pencairan ILIKE '%" + keyword + "%' OR ak.nama ILIKE '%" + keyword + "%' OR ak.kode_identitas ILIKE '%" + keyword + "%') ";
        }
        if (status !== '') {
            filters += " AND a.status = '" + status + "' ";
        }

        if (!isAdmin<%=rnd%>) {
            filters += " AND a.toko = " + idTokoLogin<%=rnd%> + " ";
        } else if (toko !== "") {
            filters += " AND a.toko = " + toko + " ";
        }

        return filters;
    };

    const triggerSearchPencairan<%=rnd%> = () => {
        currentPage<%=rnd%> = 1; 
        loadDataPencairan<%=rnd%>();
    };

    const loadDataPencairan<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataPencairan<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Mengambil data...")%></span></td></tr>';

        const sqlFilters = buildFilterSQL<%=rnd%>();
        const offset = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%>;

        const sqlCount = "SELECT COUNT(*) AS jumlah FROM koperasi.pencairan_diskon a LEFT JOIN koperasi.anggota_koperasi ak ON a.anggota_koperasi = ak.id " + sqlFilters + ";";
        
        const sqlData = "SELECT a.id, a.kode_pencairan, TO_CHAR(a.waktu_pencairan, 'YYYY-MM-DD HH24:MI') as waktu, " +
                        "TO_CHAR(a.waktu_pencairan, 'YYYY-MM-DD\"T\"HH24:MI') as w_raw, a.nominal_cair, a.status, a.keterangan, " +
                        "TO_CHAR(a.tanggal_expired_jika_berupa_topup, 'YYYY-MM-DD') as tgl_expired, " +
                        "t.id as id_toko, t.nama as nama_toko, " +
                        "ak.id as id_anggota, ak.nama as nama_anggota, ak.kode_identitas as kode_anggota, " +
                        "cb.id as id_carabayar, cb.nama as nama_carabayar " +
                        "FROM koperasi.pencairan_diskon a " +
                        "LEFT JOIN koperasi.toko t ON a.toko = t.id " +
                        "LEFT JOIN koperasi.anggota_koperasi ak ON a.anggota_koperasi = ak.id " +
                        "LEFT JOIN koperasi.cara_pembayaran_koperasi cb ON a.cara_pembayaran = cb.id " +
                        sqlFilters + " ORDER BY a.waktu_pencairan DESC, a.id DESC LIMIT " + limitPerPage<%=rnd%> + " OFFSET " + offset + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetchData<%=rnd%>(sqlCount),
                fetchData<%=rnd%>(sqlData)
            ]);

            totalRecords<%=rnd%> = (resCount && resCount[0]) ? parseInt(resCount[0].jumlah || 0) : 0;
            const recordList = resData || [];

            let htmlList = '';

            if (recordList.length === 0) {
                htmlList = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center text-muted py-5"><i class="fas fa-hand-holding-usd fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Tidak ada data pencairan ditemukan.")%></td></tr>';
            } else {
                let no = offset + 1; 
                recordList.forEach((row) => {

                    // Render Status Badge
                    let badgeStatus = '';
                    if (row.status === 'BERHASIL') {
                        badgeStatus = '<span class="badge bg-success shadow-sm px-3"><i class="fas fa-check me-1"></i>Berhasil</span>';
                    } else if (row.status === 'PENDING') {
                        badgeStatus = '<span class="badge bg-warning text-dark shadow-sm px-3"><i class="fas fa-clock me-1"></i>Pending</span>';
                    } else if (row.status === 'DITOLAK') {
                        badgeStatus = '<span class="badge bg-danger shadow-sm px-3"><i class="fas fa-times me-1"></i>Ditolak</span>';
                    } else {
                        badgeStatus = '<span class="badge bg-secondary shadow-sm px-3">' + (row.status || '-') + '</span>';
                    }

                    const safeNoRef = (row.kode_pencairan || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    const safeKet = (row.keterangan || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    
                    const strAnggota = (row.nama_anggota || 'NN') + ' <small class="text-muted">(' + (row.kode_anggota || '-') + ')</small>';
                    const strLabelAnggota = (row.nama_anggota || 'NN') + ' (' + (row.kode_anggota || '-') + ')'; // Untuk passing ke Form
                    const safeLabelAnggota = strLabelAnggota.replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    
                    let infoExpired = '';
                    if (row.tgl_expired) {
                        infoExpired = '<br><small class="text-danger fw-bold"><i class="far fa-calendar-times me-1"></i>Exp: ' + row.tgl_expired + '</small>';
                    }

                    htmlList += '<tr>' +
                            '<td class="text-center text-muted fw-medium">' + (no++) + '</td>' +
                            '<td class="text-start">' +
                                '<div class="fw-bold text-dark">' + safeNoRef + '</div>' +
                                '<small class="text-secondary"><i class="far fa-clock me-1"></i>' + (row.waktu || '-') + '</small>' +
                            '</td>' +
                            '<td class="text-start fw-bold text-primary">' + strAnggota + '</td>';
                    
                    if (isAdmin<%=rnd%>) {
                        htmlList += '<td><span class="text-muted">' + (row.nama_toko || 'General') + '</span></td>';
                    }        
                            
                    htmlList += '<td class="text-start text-dark fw-medium">' + (row.nama_carabayar || '-') + infoExpired + '</td>' +
                            '<td class="text-end fw-bold text-success fs-6">' + formatRp<%=rnd%>(row.nominal_cair) + '</td>' +
                            '<td class="text-center">' + badgeStatus + '</td>' +
                            '<td class="text-center text-nowrap">' + aksiBarisMenu([
                                { ikon: 'fa-edit', label: '<%=Common.getBahasaConfig("Ubah Data")%>', onclick: 'editPencairan<%=rnd%>(' + row.id + ', \'' + safeNoRef + '\', \'' + row.w_raw + '\', ' + (row.id_toko || 'null') + ', ' + (row.id_anggota || 'null') + ', \'' + safeLabelAnggota + '\', ' + (row.id_carabayar || 'null') + ', ' + (row.nominal_cair || 0) + ', \'' + (row.status || 'PENDING') + '\', \'' + safeKet + '\', \'' + (row.tgl_expired || '') + '\')' },
                                { ikon: 'fa-trash-alt', label: '<%=Common.getBahasaConfig("Hapus Data")%>', onclick: 'hapusPencairan<%=rnd%>(' + row.id + ')', merusak: true }
                            ]) + '</td>' +
                        '</tr>';
                });
            }
            tbody.innerHTML = htmlList;
            updatePaginationUI<%=rnd%>();

        } catch (error) {
            console.error("Gagal load tabel pencairan:", error);
            tbody.innerHTML = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal menarik data. Silakan muat ulang.")%></td></tr>';
        }
    };

    const updatePaginationUI<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumberDisplay<%=rnd%>').innerText = currentPage<%=rnd%> + " / " + totalPages;
        
        const startIdx = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%> + 1;
        const endIdx = Math.min(currentPage<%=rnd%> * limitPerPage<%=rnd%>, totalRecords<%=rnd%>);
        const startInfo = totalRecords<%=rnd%> > 0 ? startIdx : 0;
        
        document.getElementById('pagingInfoPencairan<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startInfo + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari")%> <b class="text-dark">' + totalRecords<%=rnd%> + '</b> <%=Common.getBahasaConfig("data")%>';
        
        document.getElementById('btnPrevPage<%=rnd%>').disabled = (currentPage<%=rnd%> === 1);
        document.getElementById('btnNextPage<%=rnd%>').disabled = (currentPage<%=rnd%> === totalPages);
    };

    const changePagePencairan<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if (direction === -1 && currentPage<%=rnd%> > 1) {
            currentPage<%=rnd%>--;
            loadDataPencairan<%=rnd%>();
        } else if (direction === 1 && currentPage<%=rnd%> < totalPages) {
            currentPage<%=rnd%>++;
            loadDataPencairan<%=rnd%>();
        }
    };


    // ==========================================
    // BAGIAN 3: LOGIKA FORM & DINAMIS UI 
    // ==========================================
    const bukaFormTambah<%=rnd%> = () => {
        
        document.getElementById('inputIdPencairan<%=rnd%>').value = '';
        document.getElementById('formPencairan<%=rnd%>').reset();
        
        document.getElementById('idAnggotaSelected<%=rnd%>').value = "";
        document.getElementById('inputCariAnggota<%=rnd%>').value = "";
        document.getElementById('inputTanggalExpired<%=rnd%>').value = "";
        document.getElementById('infoSaldoCashback<%=rnd%>').style.display = 'none';
        document.getElementById('hiddenSaldoCashback<%=rnd%>').value = 0;
        
        if (isAdmin<%=rnd%>) {
            document.getElementById('inputTokoForm<%=rnd%>').value = "";
        }
        
        // Auto Set Time to Now
        const now = new Date();
        now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
        document.getElementById('inputWaktuPencairan<%=rnd%>').value = now.toISOString().slice(0,16);
        
        // Generate Temporary No Ref
        document.getElementById('inputKodePencairan<%=rnd%>').value = "WD-" + new Date().getTime().toString().substring(5);

        document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-plus-circle text-primary me-2"></i><%=Common.getBahasaConfig("Catat Pencairan Baru")%>';
        
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    };

    const tutupForm<%=rnd%> = () => {
        document.getElementById('viewForm<%=rnd%>').style.display = 'none';
        document.getElementById('viewList<%=rnd%>').style.display = 'block';
        loadDataPencairan<%=rnd%>();
    };


    // ==========================================
    // BAGIAN 4: SIMPAN, EDIT, & HAPUS
    // ==========================================
    const savePencairan<%=rnd%> = async () => {
        const idPencairan = document.getElementById('inputIdPencairan<%=rnd%>').value;
        const anggotaId = document.getElementById('idAnggotaSelected<%=rnd%>').value;
        const caraId = document.getElementById('inputCaraBayar<%=rnd%>').value;
        const tokoId = isAdmin<%=rnd%> ? document.getElementById('inputTokoForm<%=rnd%>').value : idTokoLogin<%=rnd%>;
        const statusVal = document.getElementById('inputStatus<%=rnd%>').value;
        const tglExpired = document.getElementById('inputTanggalExpired<%=rnd%>').value;
        
        const noRef = document.getElementById('inputKodePencairan<%=rnd%>').value.trim();
        const nominal = parseFloat(document.getElementById('inputNominal<%=rnd%>').value) || 0;
        const sisaSaldo = parseFloat(document.getElementById('hiddenSaldoCashback<%=rnd%>').value) || 0;
        const waktuRaw = document.getElementById('inputWaktuPencairan<%=rnd%>').value; 
        const ket = document.getElementById('inputKeterangan<%=rnd%>').value.trim();

        if(anggotaId === "") {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Pilih Anggota Koperasi yang valid dari daftar!")%>', 'bg-warning text-dark');
            document.getElementById('inputCariAnggota<%=rnd%>').focus();
            return;
        }
        if(caraId === "") {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Cara Pembayaran tidak boleh kosong!")%>', 'bg-warning text-dark');
            return;
        }
        if(nominal <= 0) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Nominal harus lebih dari 0!")%>', 'bg-warning text-dark');
            return;
        }

        // VALIDASI OVSER-WITHDRAW (SISA SALDO)
        if(nominal > sisaSaldo && statusVal !== 'DITOLAK') {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal! Nominal pencairan (")%>' + formatRp<%=rnd%>(nominal) + ') melebihi saldo cashback yang tersedia (' + formatRp<%=rnd%>(sisaSaldo) + ').', 'bg-danger text-white');
            document.getElementById('inputNominal<%=rnd%>').focus();
            return;
        }

        const btnSimpan = document.getElementById('btnSimpan<%=rnd%>');
        const oriBtn = btnSimpan.innerHTML;
        btnSimpan.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyimpan...")%>';
        btnSimpan.disabled = true;

        const dataObj = {
            kodePencairan: noRef,
            nominalCair: nominal,
            waktuPencairan: waktuRaw,
            status: statusVal,
            keterangan: ket,
            anggotaKoperasi: anggotaId,
            caraPembayaran: caraId
        };
        if (tokoId !== "") {
            dataObj.toko = tokoId;
        }
        if (tglExpired !== "") {
            dataObj.tanggalExpiredJikaBerupaTopup = tglExpired;
        } else {
            dataObj.tanggalExpiredJikaBerupaTopup = null;
        }

        const payload = { 
            action: "simpanDataRinci", 
            class: masterModelClass<%=rnd%>, 
            data: dataObj
        };

        if (idPencairan !== '') {
            payload.id = idPencairan;
        }

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const result = await response.json();
            if (result.status === '00' || result.status === 'success' || result.id) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Pencairan diskon berhasil disimpan!")%>', 'bg-success text-white');
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

    const editPencairan<%=rnd%> = (id, noRef, waktu, idToko, idAnggota, labelAnggota, idCaraBayar, nominal, status, ket, tglExpired) => {
        document.getElementById('inputIdPencairan<%=rnd%>').value = id;
        document.getElementById('inputKodePencairan<%=rnd%>').value = noRef;
        document.getElementById('inputWaktuPencairan<%=rnd%>').value = waktu;
        
        if (isAdmin<%=rnd%>) {
            document.getElementById('inputTokoForm<%=rnd%>').value = idToko || '';
        }
        
        // Populate Datalist Anggota
        document.getElementById('idAnggotaSelected<%=rnd%>').value = idAnggota || '';
        document.getElementById('inputCariAnggota<%=rnd%>').value = labelAnggota || '';
        
        cekSaldoMember<%=rnd%>(idAnggota);
        
        document.getElementById('inputCaraBayar<%=rnd%>').value = idCaraBayar || '';
        document.getElementById('inputTanggalExpired<%=rnd%>').value = tglExpired || '';
        document.getElementById('inputNominal<%=rnd%>').value = nominal || 0;
        document.getElementById('inputStatus<%=rnd%>').value = status || 'PENDING';
        document.getElementById('inputKeterangan<%=rnd%>').value = ket || '';

        document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-edit text-warning me-2"></i><%=Common.getBahasaConfig("Ubah Data Pencairan")%>';
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    };

    const hapusPencairan<%=rnd%> = async (id) => {
        if (typeof prosesDeleteData === 'function') {
            prosesDeleteData(masterModelClass<%=rnd%>, id, function(){ 
                 if (document.querySelectorAll('#tabelDataPencairan<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) {
                     currentPage<%=rnd%>--;
                 }
                 loadDataPencairan<%=rnd%>();
            });
        } else {
            const isConfirm = confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus data riwayat ini?")%>');
            if (!isConfirm) return;

            const payload = { action: "deleteData", class: masterModelClass<%=rnd%>, id: id };
            try {
                const response = await fetch('<%=Common.ROOT%>/Data', {
                    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
                });
                const result = await response.json();
                if (result.status === '00' || result.status === 'success') {
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Berhasil dihapus!")%>', 'bg-success text-white');
                    if (document.querySelectorAll('#tabelDataPencairan<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) currentPage<%=rnd%>--;
                    loadDataPencairan<%=rnd%>();
                } else {
                    showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Data gagal dihapus.")%>', 'bg-danger text-white');
                }
            } catch (error) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal terhubung ke server.")%>', 'bg-danger text-white');
            }
        }
    };


    // ==========================================
    // DOWNLOAD EXCEL FEATURE
    // ==========================================
    const downloadExcelPencairan<%=rnd%> = async () => {
        showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Menyiapkan file Excel...")%>', 'bg-info text-dark');
        
        const sqlFilters = buildFilterSQL<%=rnd%>();
        const sqlQuery = "SELECT a.id, a.kode_pencairan, TO_CHAR(a.waktu_pencairan, 'YYYY-MM-DD HH24:MI:SS') as waktu, " +
                         "ak.kode_identitas as kode_anggota, ak.nama as nama_anggota, " +
                         "t.nama as nama_toko, cb.nama as cara_pembayaran, " +
                         "TO_CHAR(a.tanggal_expired_jika_berupa_topup, 'YYYY-MM-DD') as tgl_expired, " +
                         "a.nominal_cair, a.status, a.keterangan " +
                         "FROM koperasi.pencairan_diskon a " +
                         "LEFT JOIN koperasi.toko t ON a.toko = t.id " +
                         "LEFT JOIN koperasi.anggota_koperasi ak ON a.anggota_koperasi = ak.id " +
                         "LEFT JOIN koperasi.cara_pembayaran_koperasi cb ON a.cara_pembayaran = cb.id " +
                         sqlFilters + " ORDER BY a.waktu_pencairan DESC;";

        try {
            const result = await fetchData<%=rnd%>(sqlQuery);
            if (!result || result.length === 0) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Tidak ada data untuk diunduh.")%>', 'bg-warning text-dark');
                return;
            }

            let tableHtml = '<html xmlns:x="urn:schemas-microsoft-com:office:excel">';
            tableHtml += '<head><meta charset="UTF-8"><style>th { background-color: #f8f9fa; font-weight: bold; border: 1px solid black; } td { border: 1px solid black; }</style></head><body>';
            
            tableHtml += '<table><thead><tr>';
            tableHtml += '<th>ID_SISTEM</th><th>NO_REFERENSI</th><th>WAKTU_PENCAIRAN</th>';
            tableHtml += '<th>KODE_MEMBER</th><th>NAMA_MEMBER</th><th>NAMA_TOKO</th>';
            tableHtml += '<th>CARA_PEMBAYARAN</th><th>TGL_KADALUARSA</th><th>NOMINAL_CAIR</th><th>STATUS</th><th>KETERANGAN</th>';
            tableHtml += '</tr></thead><tbody>';

            result.forEach(r => {
                tableHtml += '<tr>';
                tableHtml += '<td>' + (r.id || '') + '</td><td>' + (r.kode_pencairan || '') + '</td><td>' + (r.waktu || '') + '</td>';
                tableHtml += '<td>' + (r.kode_anggota || '') + '</td><td>' + (r.nama_anggota || '') + '</td><td>' + (r.nama_toko || '') + '</td>';
                tableHtml += '<td>' + (r.cara_pembayaran || '') + '</td><td>' + (r.tgl_expired || '') + '</td><td>' + (r.nominal_cair || 0) + '</td><td>' + (r.status || '') + '</td><td>' + (r.keterangan || '') + '</td>';
                tableHtml += '</tr>';
            });

            tableHtml += '</tbody></table></body></html>';

            const blob = new Blob([tableHtml], { type: 'application/vnd.ms-excel' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement("a");
            const timestamp = new Date().toISOString().slice(0,10).replace(/-/g,"");
            link.download = "Pencairan_Diskon_" + timestamp + ".xls";
            link.href = url;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            setTimeout(() => URL.revokeObjectURL(url), 100);

        } catch (error) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal mengunduh data.")%>', 'bg-danger text-white');
        }
    };


    // ==========================================
    // INISIALISASI HALAMAN
    // ==========================================
    document.addEventListener("DOMContentLoaded", () => {
        loadMasterDataCombo<%=rnd%>();
        const filterInputs = document.querySelectorAll('.filter-input-<%=rnd%>');
        filterInputs.forEach(input => {
            if (input.tagName === 'SELECT') {
                input.addEventListener("change", () => triggerSearchPencairan<%=rnd%>());
            } else {
                input.addEventListener("keydown", (event) => {
                    if (event.key === "Enter") { event.preventDefault(); triggerSearchPencairan<%=rnd%>(); }
                });
            }
        });
        loadDataPencairan<%=rnd%>();
    });
</script>