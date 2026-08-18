<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// SECURITY CHECK
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	out.print("{\"status\":\"error\", \"message\":\"Sesi Anda telah berakhir. Silakan masuk kembali.\"}");
	return;
}
Pedagang pedagang = tbmuser.getPedagang();
Toko toko = pedagang == null ? null : pedagang.getToko();
String rnd = Common.getGeneratedBarCode(7);

boolean isAdmin = (toko == null);
String idTokoAktif = toko != null ? String.valueOf(toko.getId()) : "";
String namaTokoAktif = toko != null ? toko.getNama() : "Semua Toko Koperasi";
%>

<script data-cfasync="false" src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/3.9.1/chart.min.js"></script>
<script data-cfasync="false" src="https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js"></script>

<style>
    .kpi-card-link {
        transition: transform 0.2s, box-shadow 0.2s;
        cursor: pointer;
    }
    .kpi-card-link:hover {
        transform: translateY(-5px);
        box-shadow: 0 .5rem 1rem rgba(0,0,0,.15)!important;
    }
</style>

<div class="container-fluid px-0 animate__animated animate__fadeIn">
    
    <div class="row mb-4 align-items-end">
        <div class="col-md-5">
            <h4 class="fw-bold text-dark mb-1">
                <i class="fas fa-chart-line text-primary me-2"></i><%=Common.getBahasaConfig("Dashboard Mutasi Barang")%>
            </h4>
            <span class="text-muted small"><%=Common.getBahasaConfig("Monitoring pergerakan barang masuk (Pengadaan) dan keluar (Penjualan/Opname).")%></span>
        </div>
        <div class="col-md-7 text-md-end mt-3 mt-md-0">
            <div class="d-flex justify-content-md-end gap-2 flex-wrap">
                
                <% if (isAdmin) { %>
                <div class="input-group input-group-sm shadow-sm" style="width: 200px;">
                    <span class="input-group-text bg-white border-end-0"><i class="fas fa-store text-muted"></i></span>
                    <select class="form-select border-start-0 fw-bold text-dark" id="filterToko<%=rnd%>" onchange="loadDashboardData<%=rnd%>()">
                        <option value=""><%=Common.getBahasaConfig("- Semua Toko -")%></option>
                    </select>
                </div>
                <% } else { %>
                    <input type="hidden" id="filterToko<%=rnd%>" value="<%=idTokoAktif%>">
                <% } %>

                <div class="input-group input-group-sm shadow-sm" style="min-width:140px;flex:1 1 160px;">
                    <span class="input-group-text bg-white border-end-0"><i class="fas fa-calendar-alt text-muted"></i></span>
                    <select class="form-select border-start-0 fw-bold text-primary" id="filterPeriode<%=rnd%>" onchange="loadDashboardData<%=rnd%>()">
                        <option value="today"><%=Common.getBahasaConfig("Hari Ini")%></option>
                        <option value="week"><%=Common.getBahasaConfig("Minggu Ini")%></option>
                        <option value="month" selected><%=Common.getBahasaConfig("Bulan Ini")%></option>
                        <option value="semester"><%=Common.getBahasaConfig("Semester Ini (6 Bln)")%></option>
                        <option value="year"><%=Common.getBahasaConfig("Tahun Ini")%></option>
                        <option value="3years"><%=Common.getBahasaConfig("3 Tahun Ini")%></option>
                    </select>
                    <button class="btn btn-primary fw-bold" onclick="loadDashboardData<%=rnd%>()" id="btnSync<%=rnd%>">
                        <i class="fas fa-sync-alt"></i>
                    </button>
                </div>
            </div>
        </div>
    </div>

    <div class="row g-3 mb-4">
        
        <div class="col-xl-3 col-sm-6" onclick="downloadDataKPI<%=rnd%>('MASUK')" title="<%=Common.getBahasaConfig("Klik untuk mengunduh rincian barang masuk")%>">
            <div class="card border-0 shadow-sm rounded-4 h-100 position-relative overflow-hidden kpi-card-link">
                <div class="card-body p-4">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <div>
                            <p class="text-muted fw-bold mb-0 small text-uppercase"><%=Common.getBahasaConfig("Barang Masuk")%></p>
                            <h3 class="fw-bold text-dark mb-0 mt-1" id="kpiBarangMasuk<%=rnd%>">0</h3>
                        </div>
                        <div class="bg-success bg-opacity-10 text-success rounded-circle d-flex align-items-center justify-content-center" style="width: 50px; height: 50px;">
                            <i class="fas fa-arrow-down fa-lg"></i>
                        </div>
                    </div>
                    <span class="badge bg-success bg-opacity-10 text-success fw-bold border border-success border-opacity-25 rounded-pill"><i class="fas fa-file-excel me-1"></i>Unduh Rincian</span>
                </div>
                <div class="position-absolute bottom-0 start-0 w-100 bg-success" style="height: 4px;"></div>
            </div>
        </div>

        <div class="col-xl-3 col-sm-6" onclick="downloadDataKPI<%=rnd%>('KELUAR')" title="<%=Common.getBahasaConfig("Klik untuk mengunduh rincian barang keluar")%>">
            <div class="card border-0 shadow-sm rounded-4 h-100 position-relative overflow-hidden kpi-card-link">
                <div class="card-body p-4">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <div>
                            <p class="text-muted fw-bold mb-0 small text-uppercase"><%=Common.getBahasaConfig("Barang Keluar")%></p>
                            <h3 class="fw-bold text-dark mb-0 mt-1" id="kpiBarangKeluar<%=rnd%>">0</h3>
                        </div>
                        <div class="bg-danger bg-opacity-10 text-danger rounded-circle d-flex align-items-center justify-content-center" style="width: 50px; height: 50px;">
                            <i class="fas fa-arrow-up fa-lg"></i>
                        </div>
                    </div>
                    <span class="badge bg-danger bg-opacity-10 text-danger fw-bold border border-danger border-opacity-25 rounded-pill"><i class="fas fa-file-excel me-1"></i>Unduh Rincian</span>
                </div>
                <div class="position-absolute bottom-0 start-0 w-100 bg-danger" style="height: 4px;"></div>
            </div>
        </div>

        <div class="col-xl-3 col-sm-6" onclick="downloadDataKPI<%=rnd%>('STOK')" title="<%=Common.getBahasaConfig("Klik untuk mengunduh daftar semua stok produk")%>">
            <div class="card border-0 shadow-sm rounded-4 h-100 position-relative overflow-hidden kpi-card-link">
                <div class="card-body p-4">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <div>
                            <p class="text-muted fw-bold mb-0 small text-uppercase"><%=Common.getBahasaConfig("Total Stok Saat Ini")%></p>
                            <h3 class="fw-bold text-dark mb-0 mt-1" id="kpiTotalStok<%=rnd%>">0</h3>
                        </div>
                        <div class="bg-primary bg-opacity-10 text-primary rounded-circle d-flex align-items-center justify-content-center" style="width: 50px; height: 50px;">
                            <i class="fas fa-boxes fa-lg"></i>
                        </div>
                    </div>
                    <span class="badge bg-primary bg-opacity-10 text-primary fw-bold border border-primary border-opacity-25 rounded-pill"><i class="fas fa-file-excel me-1"></i>Daftar Produk</span>
                </div>
                <div class="position-absolute bottom-0 start-0 w-100 bg-primary" style="height: 4px;"></div>
            </div>
        </div>

        <div class="col-xl-3 col-sm-6" onclick="downloadDataKPI<%=rnd%>('KRITIS')" title="<%=Common.getBahasaConfig("Klik untuk mengunduh daftar produk yang harus di-restock")%>">
            <div class="card border-0 shadow-sm rounded-4 h-100 position-relative overflow-hidden bg-warning bg-opacity-10 kpi-card-link">
                <div class="card-body p-4">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <div>
                            <p class="text-warning-emphasis fw-bold mb-0 small text-uppercase"><%=Common.getBahasaConfig("Peringatan Stok < 10")%></p>
                            <h3 class="fw-bold text-warning-emphasis mb-0 mt-1" id="kpiStokKritis<%=rnd%>">0</h3>
                        </div>
                        <div class="bg-warning text-dark rounded-circle d-flex align-items-center justify-content-center shadow-sm" style="width: 50px; height: 50px;">
                            <i class="fas fa-exclamation-triangle fa-lg"></i>
                        </div>
                    </div>
                    <span class="badge bg-warning text-dark fw-bold rounded-pill shadow-sm"><i class="fas fa-file-excel me-1"></i>List Warning</span>
                </div>
                <div class="position-absolute bottom-0 start-0 w-100 bg-warning" style="height: 4px;"></div>
            </div>
        </div>
    </div>

    <div class="row g-3 mb-4">
        <div class="col-lg-8">
            <div class="card border-0 shadow-sm rounded-4 h-100">
                <div class="card-header bg-white border-bottom-0 pt-4 pb-2 px-4 d-flex justify-content-between align-items-center">
                    <div>
                        <h6 class="fw-bold text-dark mb-1"><i class="fas fa-chart-area text-primary me-2"></i><%=Common.getBahasaConfig("Tren Pergerakan Barang")%></h6>
                        <small class="text-muted"><%=Common.getBahasaConfig("Berapa banyak barang masuk dan keluar tiap hari, supaya terlihat naik-turunnya.")%></small>
                    </div>
                </div>
                <div class="card-body p-4">
                    <canvas id="chartTrenMutasi<%=rnd%>" height="100"></canvas>
                </div>
            </div>
        </div>

        <div class="col-lg-4">
            <div class="card border-0 shadow-sm rounded-4 h-100">
                <div class="card-header bg-white border-bottom-0 pt-4 pb-2 px-4">
                    <h6 class="fw-bold text-dark mb-1"><i class="fas fa-trophy text-warning me-2"></i><%=Common.getBahasaConfig("Top 5 Barang Keluar")%></h6>
                    <small class="text-muted"><%=Common.getBahasaConfig("Barang apa saja yang paling cepat berkurang stoknya.")%></small>
                </div>
                <div class="card-body p-4 d-flex justify-content-center align-items-center">
                    <canvas id="chartTopProduk<%=rnd%>"></canvas>
                </div>
            </div>
        </div>
    </div>

    <div class="row g-3">
        <div class="col-lg-7">
            <div class="card border-0 shadow-sm rounded-4 h-100 d-flex flex-column">
                <div class="card-header bg-white border-bottom-0 pt-3 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-2">
                    <h6 class="fw-bold text-dark mb-0"><i class="fas fa-history text-secondary me-2"></i><%=Common.getBahasaConfig("Log Transaksi Terkini")%></h6>
                    
                    <div class="d-flex gap-2">
                        <div class="input-group input-group-sm" style="width: 180px;">
                            <span class="input-group-text bg-light border-end-0"><i class="fas fa-search text-muted"></i></span>
                            <input type="text" class="form-control border-start-0 bg-light" id="inputSearchLog<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Kode/Nama...")%>" onkeyup="filterTabelLog<%=rnd%>()">
                        </div>
                        <button class="btn btn-sm btn-success fw-bold px-3 shadow-sm" onclick="downloadLogExcel<%=rnd%>()" title="<%=Common.getBahasaConfig("Unduh Log Transaksi ke Excel")%>">
                            <i class="fas fa-file-excel"></i>
                        </button>
                    </div>
                </div>
                
                <div class="card-body p-0 flex-grow-1">
                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0">
                            <thead class="table-light text-muted small position-sticky top-0" style="z-index: 1;">
                                <tr>
                                    <th class="px-4"><%=Common.getBahasaConfig("Waktu")%></th>
                                    <th><%=Common.getBahasaConfig("Tipe Mutasi")%></th>
                                    <th><%=Common.getBahasaConfig("Produk")%></th>
                                    <th class="text-end px-4"><%=Common.getBahasaConfig("Qty")%></th>
                                </tr>
                            </thead>
                            <tbody id="tabelLogMutasi<%=rnd%>">
                                </tbody>
                        </table>
                    </div>
                </div>
                <div class="card-footer bg-white border-top-0 d-flex justify-content-between align-items-center py-3 px-4">
                    <small class="text-muted fw-medium" id="pagingInfoLog<%=rnd%>"></small>
                    <div class="btn-group btn-group-sm shadow-sm">
                        <button class="btn btn-outline-primary fw-bold" id="btnPrevLog<%=rnd%>" onclick="changePageLogMutasi<%=rnd%>(-1)"><i class="fas fa-chevron-left"></i></button>
                        <button class="btn btn-primary fw-bold disabled" id="pageNumberLog<%=rnd%>" style="opacity: 1;">1</button>
                        <button class="btn btn-outline-primary fw-bold" id="btnNextLog<%=rnd%>" onclick="changePageLogMutasi<%=rnd%>(1)"><i class="fas fa-chevron-right"></i></button>
                    </div>
                </div>
            </div>
        </div>

        <div class="col-lg-5">
            <div class="card border-0 shadow-sm rounded-4 h-100 border-top border-warning border-3 d-flex flex-column">
                <div class="card-header bg-white border-bottom-0 pt-3 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-2">
                    <div class="d-flex align-items-center">
                        <h6 class="fw-bold text-dark mb-0"><i class="fas fa-battery-quarter text-danger me-2"></i><%=Common.getBahasaConfig("Stok Menipis")%></h6>
                        <span class="badge bg-danger rounded-pill px-2 ms-2" id="badgeStokHabis<%=rnd%>">0</span>
                    </div>
                    
                    <div class="d-flex gap-2">
                        <div class="input-group input-group-sm" style="width: 150px;">
                            <span class="input-group-text bg-light border-end-0"><i class="fas fa-search text-muted"></i></span>
                            <input type="text" class="form-control border-start-0 bg-light" id="inputSearchKritis<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari...")%>" onkeyup="filterTabelKritis<%=rnd%>()">
                        </div>
                        <button class="btn btn-sm btn-success fw-bold px-3 shadow-sm" onclick="downloadKritisExcel<%=rnd%>()" title="<%=Common.getBahasaConfig("Unduh Stok Menipis ke Excel")%>">
                            <i class="fas fa-file-excel"></i>
                        </button>
                    </div>
                </div>
                <div class="card-body p-0 flex-grow-1">
                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0">
                            <thead class="table-light text-muted small position-sticky top-0" style="z-index: 1;">
                                <tr>
                                    <th class="px-4"><%=Common.getBahasaConfig("Kode")%></th>
                                    <th><%=Common.getBahasaConfig("Produk")%></th>
                                    <th class="text-center px-4"><%=Common.getBahasaConfig("Sisa Stok")%></th>
                                </tr>
                            </thead>
                            <tbody id="tabelStokKritis<%=rnd%>">
                                </tbody>
                        </table>
                    </div>
                </div>
                <div class="card-footer bg-white border-top-0 d-flex justify-content-between align-items-center py-3 px-4">
                    <small class="text-muted fw-medium" id="pagingInfoKritis<%=rnd%>"></small>
                    <div class="btn-group btn-group-sm shadow-sm">
                        <button class="btn btn-outline-warning text-dark fw-bold" id="btnPrevKritis<%=rnd%>" onclick="changePageStokKritis<%=rnd%>(-1)"><i class="fas fa-chevron-left"></i></button>
                        <button class="btn btn-warning text-dark fw-bold disabled" id="pageNumberKritis<%=rnd%>" style="opacity: 1;">1</button>
                        <button class="btn btn-outline-warning text-dark fw-bold" id="btnNextKritis<%=rnd%>" onclick="changePageStokKritis<%=rnd%>(1)"><i class="fas fa-chevron-right"></i></button>
                    </div>
                </div>
            </div>
        </div>
    </div>

</div>

<script>
    let chartTrenInstance<%=rnd%> = null;
    let chartTopInstance<%=rnd%> = null;
    const isAdmin<%=rnd%> = <%=isAdmin%>;
    const defaultTokoId<%=rnd%> = '<%=idTokoAktif%>';

    // State Variables for Pagination & Filtering
    let logDataList<%=rnd%> = [];
    let logPage<%=rnd%> = 1;
    const logLimit<%=rnd%> = 5;

    let kritisDataList<%=rnd%> = [];
    let kritisPage<%=rnd%> = 1;
    const kritisLimit<%=rnd%> = 5;

    // Utility Fetch JSON
    const fetchApiData<%=rnd%> = async (sql) => {
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sql: sql, action: "sql" })
            });
            const result = await res.json();
            return result.data || [];
        } catch (e) { 
            console.error("Fetch Data Error:", e); 
            return []; 
        }
    };

    const formatRp<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID', { minimumFractionDigits: 0 }).format(angka || 0);
    };

    const initFilterToko<%=rnd%> = async () => {
        if (isAdmin<%=rnd%>) {
            const sqlToko = "SELECT id, nama FROM koperasi.toko WHERE aktif = true ORDER BY nama ASC";
            const resToko = await fetchApiData<%=rnd%>(sqlToko);
            
            const elFilter = document.getElementById('filterToko<%=rnd%>');
            let optHtml = '<option value="">' + '<%=Common.getBahasaConfigJS("- Semua Toko -")%>' + '</option>';
            
            resToko.forEach(item => {
                optHtml += '<option value="' + item.id + '">' + item.nama + '</option>';
            });
            
            elFilter.innerHTML = optHtml;
        }
    };

    const getSqlTimeCondition<%=rnd%> = (colName, filterVal) => {
        if (filterVal === 'today') return " AND " + colName + " >= current_date ";
        if (filterVal === 'week') return " AND " + colName + " >= current_date - interval '7 days' ";
        if (filterVal === 'month') return " AND date_trunc('month', " + colName + ") = date_trunc('month', current_date) ";
        if (filterVal === 'semester') return " AND " + colName + " >= current_date - interval '6 months' ";
        if (filterVal === 'year') return " AND date_trunc('year', " + colName + ") = date_trunc('year', current_date) ";
        if (filterVal === '3years') return " AND " + colName + " >= current_date - interval '3 years' ";
        return ""; 
    };

    // =======================================================
    // EXPORT EXCEL BERDASARKAN KARTU KPI YANG DIKLIK
    // =======================================================
    const downloadDataKPI<%=rnd%> = async (kpiType) => {
        const periode = document.getElementById('filterPeriode<%=rnd%>').value;
        const elToko = document.getElementById('filterToko<%=rnd%>');
        const tokoId = elToko ? elToko.value : defaultTokoId<%=rnd%>;
        
        let sqlQuery = "";
        let fileName = "";
        
        if (kpiType === 'MASUK') {
            let sqlFilter = " WHERE 1=1 ";
            if (tokoId !== "") sqlFilter += " AND a.toko = " + tokoId + " ";
            sqlFilter += getSqlTimeCondition<%=rnd%>("a.waktupengadaan", periode);
            
            sqlQuery = "SELECT a.nomorfaktur AS \"Nomor Faktur\", TO_CHAR(a.waktupengadaan, 'YYYY-MM-DD HH24:MI:SS') AS \"Waktu Masuk\", " +
                       "p.kode AS \"Kode Produk\", p.nama AS \"Nama Produk\", a.namasupplier AS \"Supplier\", " +
                       "a.qty AS \"Jumlah Masuk\", a.hargabelisatuan AS \"Harga Beli\", a.totalharga AS \"Total Harga\" " +
                       "FROM koperasi.pengadaan_produk a " +
                       "LEFT JOIN koperasi.produk p ON a.produk = p.id " +
                       sqlFilter + " ORDER BY a.waktupengadaan DESC;";
            fileName = "Rekap_Barang_Masuk_" + periode;
        }
        else if (kpiType === 'KELUAR') {
            let sqlFilter = " WHERE 1=1 ";
            if (tokoId !== "") sqlFilter += " AND a.toko = " + tokoId + " ";
            sqlFilter += getSqlTimeCondition<%=rnd%>("a.waktu", periode);
            
            sqlQuery = "SELECT a.kode AS \"No Transaksi\", TO_CHAR(a.waktu, 'YYYY-MM-DD HH24:MI:SS') AS \"Waktu Keluar\", " +
                       "p.kode AS \"Kode Produk\", p.nama AS \"Nama Produk\", " +
                       "a.qty AS \"Jumlah Keluar\", a.hargajual AS \"Harga Jual\", (a.qty * a.hargajual) AS \"Subtotal\" " +
                       "FROM koperasi.pembelian a " +
                       "LEFT JOIN koperasi.produk p ON a.produk = p.id " +
                       sqlFilter + " ORDER BY a.waktu DESC;";
            fileName = "Rekap_Barang_Keluar_" + periode;
        }
        else if (kpiType === 'STOK') {
            let sqlFilter = " WHERE p.aktif = true ";
            if (tokoId !== "") sqlFilter += " AND p.toko = " + tokoId + " ";
            
            sqlQuery = "SELECT p.kode AS \"Kode Produk\", p.nama AS \"Nama Produk\", " +
                       "k.nama AS \"Kategori\", t.nama AS \"Toko\", " +
                       "p.hargabeli AS \"Harga Modal\", p.hargajual AS \"Harga Jual\", " +
                       "p.stok AS \"Total Stok Sistem\" " +
                       "FROM koperasi.produk p " +
                       "LEFT JOIN koperasi.jenis_produk k ON p.jenis_produk = k.id " +
                       "LEFT JOIN koperasi.toko t ON p.toko = t.id " +
                       sqlFilter + " ORDER BY p.nama ASC;";
            fileName = "Daftar_Total_Stok_Realtime";
        }
        else if (kpiType === 'KRITIS') {
            let sqlFilter = " WHERE p.aktif = true AND p.stok < 10 ";
            if (tokoId !== "") sqlFilter += " AND p.toko = " + tokoId + " ";
            
            sqlQuery = "SELECT p.kode AS \"Kode Produk\", p.nama AS \"Nama Produk\", " +
                       "t.nama AS \"Toko\", p.stok AS \"Sisa Stok Kritis\" " +
                       "FROM koperasi.produk p " +
                       "LEFT JOIN koperasi.toko t ON p.toko = t.id " +
                       sqlFilter + " ORDER BY p.stok ASC, p.nama ASC;";
            fileName = "Peringatan_Stok_Habis";
        }

        try {
            document.body.style.cursor = 'wait';
            const result = await fetchApiData<%=rnd%>(sqlQuery);
            if (!result || result.length === 0) {
                alert('<%=Common.getBahasaConfigJS("Tidak ada data untuk diunduh pada filter ini.")%>');
                document.body.style.cursor = 'default';
                return;
            }

            let tableHtml = '<html xmlns:x="urn:schemas-microsoft-com:office:excel">';
            tableHtml += '<head><meta charset="UTF-8"><style>';
            tableHtml += 'th { background-color: #f8f9fa; font-weight: bold; border: 1px solid black; }';
            tableHtml += 'td { border: 1px solid black; }';
            tableHtml += '</style></head><body>';
            
            const columns = Object.keys(result[0]);
            
            tableHtml += '<table><thead><tr>';
            columns.forEach(col => {
                tableHtml += '<th>' + col + '</th>';
            });
            tableHtml += '</tr></thead><tbody>';

            result.forEach(row => {
                tableHtml += '<tr>';
                columns.forEach(col => {
                    tableHtml += '<td>' + (row[col] !== null ? row[col] : '') + '</td>';
                });
                tableHtml += '</tr>';
            });

            tableHtml += '</tbody></table></body></html>';

            const blob = new Blob([tableHtml], { type: 'application/vnd.ms-excel' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement("a");
            const timestamp = new Date().toISOString().slice(0,10).replace(/-/g,"");
            link.download = fileName + "_" + timestamp + ".xls";
            link.href = url;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            setTimeout(() => URL.revokeObjectURL(url), 100);

        } catch (error) {
            console.error("Gagal Download KPI Excel:", error);
            alert('<%=Common.getBahasaConfigJS("Gagal mengunduh berkas Excel.")%>');
        } finally {
            document.body.style.cursor = 'default';
        }
    };


    const loadDashboardData<%=rnd%> = async () => {
        const periode = document.getElementById('filterPeriode<%=rnd%>').value;
        const elToko = document.getElementById('filterToko<%=rnd%>');
        const tokoId = elToko ? elToko.value : defaultTokoId<%=rnd%>;
        
        const btnSync = document.getElementById('btnSync<%=rnd%>');
        btnSync.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        btnSync.disabled = true;

        try {
            let sqlFilterToko = tokoId !== "" ? " AND a.toko = " + tokoId + " " : "";
            let sqlFilterProduk = tokoId !== "" ? " AND p.toko = " + tokoId + " " : "";

            let sqlIn = "SELECT SUM(a.qty) as total FROM koperasi.pengadaan_produk a WHERE 1=1 " + sqlFilterToko + getSqlTimeCondition<%=rnd%>("a.waktupengadaan", periode);
            let resIn = await fetchApiData<%=rnd%>(sqlIn);
            let totalMasuk = resIn && resIn[0] && resIn[0].total ? parseFloat(resIn[0].total) : 0;

            let sqlOut = "SELECT SUM(a.qty) as total FROM koperasi.pembelian a WHERE 1=1 " + sqlFilterToko + getSqlTimeCondition<%=rnd%>("a.waktu", periode);
            let resOut = await fetchApiData<%=rnd%>(sqlOut);
            let totalKeluar = resOut && resOut[0] && resOut[0].total ? parseFloat(resOut[0].total) : 0;

            let sqlStok = "SELECT SUM(p.stok) as total_stok, COUNT(CASE WHEN p.stok < 10 THEN 1 END) as stok_kritis FROM koperasi.produk p WHERE p.aktif = true " + sqlFilterProduk;
            let resStok = await fetchApiData<%=rnd%>(sqlStok);
            let totalStok = resStok && resStok[0] && resStok[0].total_stok ? parseFloat(resStok[0].total_stok) : 0;
            let stokKritis = resStok && resStok[0] && resStok[0].stok_kritis ? parseInt(resStok[0].stok_kritis) : 0;

            document.getElementById('kpiBarangMasuk<%=rnd%>').innerText = formatRp<%=rnd%>(totalMasuk);
            document.getElementById('kpiBarangKeluar<%=rnd%>').innerText = formatRp<%=rnd%>(totalKeluar);
            document.getElementById('kpiTotalStok<%=rnd%>').innerText = formatRp<%=rnd%>(totalStok);
            document.getElementById('kpiStokKritis<%=rnd%>').innerText = stokKritis;
            document.getElementById('badgeStokHabis<%=rnd%>').innerText = stokKritis;

            let timeGroupFormat = (periode === 'year' || periode === 'semester' || periode === '3years') ? 'YYYY-MM' : 'YYYY-MM-DD';
            
            let sqlChartIn = "SELECT TO_CHAR(a.waktupengadaan, '" + timeGroupFormat + "') as tanggal, SUM(a.qty) as total " +
                             "FROM koperasi.pengadaan_produk a WHERE 1=1 " + sqlFilterToko + getSqlTimeCondition<%=rnd%>("a.waktupengadaan", periode) + 
                             " GROUP BY TO_CHAR(a.waktupengadaan, '" + timeGroupFormat + "') ORDER BY tanggal ASC;";
            let resChartIn = await fetchApiData<%=rnd%>(sqlChartIn);

            let sqlChartOut = "SELECT TO_CHAR(a.waktu, '" + timeGroupFormat + "') as tanggal, SUM(a.qty) as total " +
                              "FROM koperasi.pembelian a WHERE 1=1 " + sqlFilterToko + getSqlTimeCondition<%=rnd%>("a.waktu", periode) + 
                              " GROUP BY TO_CHAR(a.waktu, '" + timeGroupFormat + "') ORDER BY tanggal ASC;";
            let resChartOut = await fetchApiData<%=rnd%>(sqlChartOut);

            let setTanggal = new Set();
            resChartIn.forEach(r => setTanggal.add(r.tanggal));
            resChartOut.forEach(r => setTanggal.add(r.tanggal));
            let labelsChart = Array.from(setTanggal).sort();

            let dataMasuk = labelsChart.map(lbl => {
                let match = resChartIn.find(r => r.tanggal === lbl);
                return match ? parseFloat(match.total) : 0;
            });
            let dataKeluar = labelsChart.map(lbl => {
                let match = resChartOut.find(r => r.tanggal === lbl);
                return match ? parseFloat(match.total) : 0;
            });

            renderChartTren<%=rnd%>({ labels: labelsChart, masuk: dataMasuk, keluar: dataKeluar });

            let sqlTopProduk = "SELECT p.nama as produk, SUM(a.qty) as total " +
                               "FROM koperasi.pembelian a LEFT JOIN koperasi.produk p ON a.produk = p.id " +
                               "WHERE 1=1 " + sqlFilterToko + getSqlTimeCondition<%=rnd%>("a.waktu", periode) +
                               " GROUP BY p.nama ORDER BY total DESC LIMIT 5;";
            let resTopProduk = await fetchApiData<%=rnd%>(sqlTopProduk);
            
            let lblTop = resTopProduk.map(r => r.produk);
            let valTop = resTopProduk.map(r => parseFloat(r.total));
            renderChartTopProduk<%=rnd%>({ labels: lblTop, values: valTop });

            // LOG TRANSAKSI MENGGUNAKAN UNION ALL
            let sqlLogUnion = 
                "SELECT TO_CHAR(waktu, 'YYYY-MM-DD HH24:MI') as waktu_str, waktu as raw_waktu, tipe, produk, kode, qty FROM (" +
                    "SELECT a.waktupengadaan as waktu, 'IN' as tipe, p.nama as produk, p.kode as kode, a.qty as qty " +
                    "FROM koperasi.pengadaan_produk a LEFT JOIN koperasi.produk p ON a.produk = p.id " +
                    "WHERE 1=1 " + sqlFilterToko + getSqlTimeCondition<%=rnd%>("a.waktupengadaan", periode) +
                    " UNION ALL " +
                    "SELECT a.waktu as waktu, 'OUT' as tipe, p.nama as produk, p.kode as kode, a.qty as qty " +
                    "FROM koperasi.pembelian a LEFT JOIN koperasi.produk p ON a.produk = p.id " +
                    "WHERE 1=1 " + sqlFilterToko + getSqlTimeCondition<%=rnd%>("a.waktu", periode) +
                    " UNION ALL " +
                    "SELECT a.waktuopname as waktu, 'OPNAME' as tipe, p.nama as produk, p.kode as kode, a.selisih as qty " +
                    "FROM koperasi.stok_opname a LEFT JOIN koperasi.produk p ON a.produk = p.id " +
                    "WHERE 1=1 " + sqlFilterToko + getSqlTimeCondition<%=rnd%>("a.waktuopname", periode) +
                    " UNION ALL " +
                    "SELECT a.waktu as waktu, 'PAKAI' as tipe, p.nama as produk, p.kode as kode, a.qty as qty " +
                    "FROM koperasi.pemakaian_bahan_baku a LEFT JOIN koperasi.produk p ON a.produk = p.id " +
                    "WHERE 1=1 " + sqlFilterToko + getSqlTimeCondition<%=rnd%>("a.waktu", periode) +
                ") t_all " +
                "ORDER BY raw_waktu DESC LIMIT 100;"; // Limit cache 100 untuk search local
                
            let resLogUnion = await fetchApiData<%=rnd%>(sqlLogUnion);
            logDataList<%=rnd%> = resLogUnion.map(r => ({
                waktu: r.waktu_str,
                tipe: r.tipe,
                namaProduk: r.produk || '',
                kode: r.kode || '',
                qty: parseFloat(r.qty)
            }));

            // TABEL STOK KRITIS
            let sqlStokKritis = "SELECT p.kode, p.nama, p.stok FROM koperasi.produk p " +
                                "WHERE p.aktif = true AND p.stok < 10 " + sqlFilterProduk +
                                "ORDER BY p.stok ASC LIMIT 100;"; // Limit cache 100 untuk search local
            let resStokKritis = await fetchApiData<%=rnd%>(sqlStokKritis);
            kritisDataList<%=rnd%> = resStokKritis.map(r => ({
                kode: r.kode || '',
                namaProduk: r.nama || '',
                stok: parseFloat(r.stok)
            }));

            // Bersihkan input pencarian
            document.getElementById('inputSearchLog<%=rnd%>').value = '';
            document.getElementById('inputSearchKritis<%=rnd%>').value = '';

            // Reset Halaman dan Render Tabel
            logPage<%=rnd%> = 1;
            kritisPage<%=rnd%> = 1;
            renderTabelLog<%=rnd%>();
            renderTabelStokKritis<%=rnd%>();

        } catch (error) {
            console.error("Gagal memuat dashboard database:", error);
            alert('<%=Common.getBahasaConfigJS("Terjadi kesalahan saat mengeksekusi kueri Dasbor.")%>');
        } finally {
            btnSync.innerHTML = '<i class="fas fa-sync-alt"></i>';
            btnSync.disabled = false;
        }
    };

    const renderChartTren<%=rnd%> = (data) => {
        const ctx = document.getElementById('chartTrenMutasi<%=rnd%>').getContext('2d');
        if (chartTrenInstance<%=rnd%>) chartTrenInstance<%=rnd%>.destroy();

        chartTrenInstance<%=rnd%> = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: data.labels, 
                datasets: [
                    {
                        label: 'Barang Masuk',
                        data: data.masuk,
                        backgroundColor: 'rgba(25, 135, 84, 0.8)', 
                        borderRadius: 4
                    },
                    {
                        label: 'Barang Keluar',
                        data: data.keluar,
                        backgroundColor: 'rgba(220, 53, 69, 0.8)', 
                        borderRadius: 4
                    }
                ]
            },
            options: {
                responsive: true,
                interaction: { intersect: false, mode: 'index' },
                plugins: { legend: { position: 'top' } },
                scales: {
                    y: { beginAtZero: true, grid: { borderDash: [2, 4] } },
                    x: { grid: { display: false } }
                }
            }
        });
    };

    const renderChartTopProduk<%=rnd%> = (data) => {
        const ctx = document.getElementById('chartTopProduk<%=rnd%>').getContext('2d');
        if (chartTopInstance<%=rnd%>) chartTopInstance<%=rnd%>.destroy();

        chartTopInstance<%=rnd%> = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: data.labels,
                datasets: [{
                    data: data.values,
                    backgroundColor: ['#0d6efd', '#ffc107', '#198754', '#dc3545', '#6c757d'],
                    borderWidth: 2,
                    borderColor: '#ffffff'
                }]
            },
            options: {
                responsive: true,
                cutout: '65%',
                plugins: { legend: { position: 'bottom', labels: { boxWidth: 12 } } }
            }
        });
    };

    // =======================================================
    // LOGIC TABEL & PAGING: LOG TRANSAKSI
    // =======================================================
    const filterTabelLog<%=rnd%> = () => {
        logPage<%=rnd%> = 1;
        renderTabelLog<%=rnd%>();
    };

    const getFilteredLogData<%=rnd%> = () => {
        const keyword = document.getElementById('inputSearchLog<%=rnd%>').value.toLowerCase();
        if (keyword === "") return logDataList<%=rnd%>;
        return logDataList<%=rnd%>.filter(item => 
            item.namaProduk.toLowerCase().includes(keyword) || 
            item.kode.toLowerCase().includes(keyword)
        );
    };

    const renderTabelLog<%=rnd%> = () => {
        const tbody = document.getElementById('tabelLogMutasi<%=rnd%>');
        let html = '';
        
        const filteredData = getFilteredLogData<%=rnd%>();
        const totalRecords = filteredData.length;
        const totalPages = Math.ceil(totalRecords / logLimit<%=rnd%>) || 1;
        
        if (logPage<%=rnd%> > totalPages) logPage<%=rnd%> = totalPages;
        if (logPage<%=rnd%> < 1) logPage<%=rnd%> = 1;

        const startIndex = (logPage<%=rnd%> - 1) * logLimit<%=rnd%>;
        const endIndex = Math.min(startIndex + logLimit<%=rnd%>, totalRecords);
        const currentData = filteredData.slice(startIndex, endIndex);

        if(currentData.length === 0) {
            html = '<tr><td colspan="4" class="text-center text-muted py-4"><i class="fas fa-history opacity-25 fa-2x mb-2 d-block"></i>Tidak ada transaksi ditemukan.</td></tr>';
        } else {
            currentData.forEach(item => {
                let badgeHtml = '';
                let textColor = '';
                let qtyPrefix = '';

                if(item.tipe === 'IN') {
                    badgeHtml = '<span class="badge bg-success bg-opacity-10 text-success border border-success border-opacity-25 px-2"><i class="fas fa-arrow-down me-1"></i>Masuk</span>';
                    textColor = 'text-success';
                    qtyPrefix = '+';
                } else if(item.tipe === 'OUT') {
                    badgeHtml = '<span class="badge bg-danger bg-opacity-10 text-danger border border-danger border-opacity-25 px-2"><i class="fas fa-arrow-up me-1"></i>Keluar</span>';
                    textColor = 'text-danger';
                    qtyPrefix = '-';
                } else if(item.tipe === 'PAKAI') {
                    badgeHtml = '<span class="badge bg-warning bg-opacity-10 text-warning border border-warning border-opacity-25 px-2"><i class="fas fa-utensils me-1"></i>Pakai Bahan</span>';
                    textColor = 'text-warning';
                    qtyPrefix = '-';
                } else {
                    badgeHtml = '<span class="badge bg-secondary bg-opacity-10 text-secondary border border-secondary border-opacity-25 px-2"><i class="fas fa-exchange-alt me-1"></i>Opname</span>';
                    textColor = 'text-dark';
                    qtyPrefix = '';
                }

                // Tampilkan Kode Produk jika ada untuk informatif tambahan
                let nmProd = item.namaProduk;
                if(item.kode && item.kode !== '') nmProd += ' <small class="text-muted">[' + item.kode + ']</small>';

                html += '<tr>' +
                            '<td class="px-4 text-muted small fw-medium">' + item.waktu + '</td>' +
                            '<td>' + badgeHtml + '</td>' +
                            '<td class="fw-bold text-dark">' + nmProd + '</td>' +
                            '<td class="text-end px-4 fw-bold ' + textColor + '">' + qtyPrefix + item.qty + '</td>' +
                        '</tr>';
            });
        }
        tbody.innerHTML = html;
        
        document.getElementById('pageNumberLog<%=rnd%>').innerText = logPage<%=rnd%> + " / " + totalPages;
        document.getElementById('pagingInfoLog<%=rnd%>').innerText = (totalRecords > 0 ? (startIndex + 1) : 0) + " - " + endIndex + " dari " + totalRecords;
        document.getElementById('btnPrevLog<%=rnd%>').disabled = (logPage<%=rnd%> === 1);
        document.getElementById('btnNextLog<%=rnd%>').disabled = (logPage<%=rnd%> === totalPages);
    };

    const changePageLogMutasi<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(getFilteredLogData<%=rnd%>().length / logLimit<%=rnd%>) || 1;
        if (direction === -1 && logPage<%=rnd%> > 1) {
            logPage<%=rnd%>--;
            renderTabelLog<%=rnd%>();
        } else if (direction === 1 && logPage<%=rnd%> < totalPages) {
            logPage<%=rnd%>++;
            renderTabelLog<%=rnd%>();
        }
    };

    // Download Khusus Tabel Log Transaksi Filtered
    const downloadLogExcel<%=rnd%> = () => {
        const filteredData = getFilteredLogData<%=rnd%>();
        if(filteredData.length === 0) {
            alert('<%=Common.getBahasaConfigJS("Tidak ada data log yang bisa diunduh.")%>');
            return;
        }

        let tableHtml = '<html xmlns:x="urn:schemas-microsoft-com:office:excel"><head><meta charset="UTF-8"><style>th{background-color:#f8f9fa;border:1px solid #000;}td{border:1px solid #000;}</style></head><body>';
        tableHtml += '<table><thead><tr><th>Waktu</th><th>Tipe Mutasi</th><th>Kode Produk</th><th>Nama Produk</th><th>Qty</th></tr></thead><tbody>';
        
        filteredData.forEach(item => {
            tableHtml += '<tr><td>' + item.waktu + '</td><td>' + item.tipe + '</td><td>' + item.kode + '</td><td>' + item.namaProduk + '</td><td>' + item.qty + '</td></tr>';
        });
        
        tableHtml += '</tbody></table></body></html>';

        const blob = new Blob([tableHtml], { type: 'application/vnd.ms-excel' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.download = "Tabel_Log_Transaksi_" + new Date().getTime() + ".xls";
        link.href = url;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    };

    // =======================================================
    // LOGIC TABEL & PAGING: PERINGATAN STOK
    // =======================================================
    const filterTabelKritis<%=rnd%> = () => {
        kritisPage<%=rnd%> = 1;
        renderTabelStokKritis<%=rnd%>();
    };

    const getFilteredKritisData<%=rnd%> = () => {
        const keyword = document.getElementById('inputSearchKritis<%=rnd%>').value.toLowerCase();
        if (keyword === "") return kritisDataList<%=rnd%>;
        return kritisDataList<%=rnd%>.filter(item => 
            item.namaProduk.toLowerCase().includes(keyword) || 
            item.kode.toLowerCase().includes(keyword)
        );
    };

    const renderTabelStokKritis<%=rnd%> = () => {
        const tbody = document.getElementById('tabelStokKritis<%=rnd%>');
        let html = '';

        const filteredData = getFilteredKritisData<%=rnd%>();
        const totalRecords = filteredData.length;
        const totalPages = Math.ceil(totalRecords / kritisLimit<%=rnd%>) || 1;
        
        if (kritisPage<%=rnd%> > totalPages) kritisPage<%=rnd%> = totalPages;
        if (kritisPage<%=rnd%> < 1) kritisPage<%=rnd%> = 1;

        const startIndex = (kritisPage<%=rnd%> - 1) * kritisLimit<%=rnd%>;
        const endIndex = Math.min(startIndex + kritisLimit<%=rnd%>, totalRecords);
        const currentData = filteredData.slice(startIndex, endIndex);

        if(currentData.length === 0) {
            html = '<tr><td colspan="3" class="text-center text-success py-4"><i class="fas fa-check-circle opacity-50 fa-2x mb-2 d-block"></i>Semua stok aman / tidak ada data.</td></tr>';
        } else {
            currentData.forEach(item => {
                let bgRow = item.stok <= 0 ? 'bg-danger bg-opacity-10' : '';
                let textQty = item.stok <= 0 ? 'text-danger' : 'text-warning-emphasis';

                html += '<tr class="' + bgRow + '">' +
                            '<td class="px-4 fw-medium text-muted">' + item.kode + '</td>' +
                            '<td class="fw-bold text-dark">' + item.namaProduk + '</td>' +
                            '<td class="text-center px-4 fw-bold fs-6 ' + textQty + '">' + item.stok + '</td>' +
                        '</tr>';
            });
        }
        tbody.innerHTML = html;
        
        document.getElementById('pageNumberKritis<%=rnd%>').innerText = kritisPage<%=rnd%> + " / " + totalPages;
        document.getElementById('pagingInfoKritis<%=rnd%>').innerText = (totalRecords > 0 ? (startIndex + 1) : 0) + " - " + endIndex + " dari " + totalRecords;
        document.getElementById('btnPrevKritis<%=rnd%>').disabled = (kritisPage<%=rnd%> === 1);
        document.getElementById('btnNextKritis<%=rnd%>').disabled = (kritisPage<%=rnd%> === totalPages);
    };
    
    const changePageStokKritis<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(getFilteredKritisData<%=rnd%>().length / kritisLimit<%=rnd%>) || 1;
        if (direction === -1 && kritisPage<%=rnd%> > 1) {
            kritisPage<%=rnd%>--;
            renderTabelStokKritis<%=rnd%>();
        } else if (direction === 1 && kritisPage<%=rnd%> < totalPages) {
            kritisPage<%=rnd%>++;
            renderTabelStokKritis<%=rnd%>();
        }
    };

    // Download Khusus Tabel Stok Kritis Filtered
    const downloadKritisExcel<%=rnd%> = () => {
        const filteredData = getFilteredKritisData<%=rnd%>();
        if(filteredData.length === 0) {
            alert('<%=Common.getBahasaConfigJS("Tidak ada data stok kritis yang bisa diunduh.")%>');
            return;
        }

        let tableHtml = '<html xmlns:x="urn:schemas-microsoft-com:office:excel"><head><meta charset="UTF-8"><style>th{background-color:#f8f9fa;border:1px solid #000;}td{border:1px solid #000;}</style></head><body>';
        tableHtml += '<table><thead><tr><th>Kode Produk</th><th>Nama Produk</th><th>Sisa Stok</th></tr></thead><tbody>';
        
        filteredData.forEach(item => {
            tableHtml += '<tr><td>' + item.kode + '</td><td>' + item.namaProduk + '</td><td>' + item.stok + '</td></tr>';
        });
        
        tableHtml += '</tbody></table></body></html>';

        const blob = new Blob([tableHtml], { type: 'application/vnd.ms-excel' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.download = "Tabel_Stok_Menipis_" + new Date().getTime() + ".xls";
        link.href = url;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    };


    document.addEventListener("DOMContentLoaded", async () => {
        await initFilterToko<%=rnd%>();
        loadDashboardData<%=rnd%>();
    });
</script>