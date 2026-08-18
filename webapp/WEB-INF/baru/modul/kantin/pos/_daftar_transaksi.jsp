<%@page import="ais.database.model.koperasi.PembelianAnggotaKoperasi"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
//2. SECURITY CHECK
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	out.print("{\"status\":\"error\", \"message\":\""
	+ Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
	return;
}
Pedagang pedagang = tbmuser.getPedagang();
Toko toko = pedagang == null ? null : pedagang.getToko();
String rnd = request.getParameter("rnd");
if(rnd == null) {
    rnd = Common.getGeneratedBarCode(7);
}

// Menyiapkan flag dan variabel filter untuk JavaScript
boolean isAdmin = (toko == null);
String idTokoAktif = toko != null ? String.valueOf(toko.getId()) : "";
String namaTokoAktif = toko != null ? toko.getNama() : "";

// Filter SQL khusus untuk riwayat dan katalog
String sqlFilterToko = "";
if (toko != null) {
	sqlFilterToko = " AND a.toko = " + toko.getId() + " ";
}
%>

<div class="row g-4 mb-4 animate__animated animate__fadeInUp">
    <div class="col-lg-12">
        <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
            <div class="card-body p-4">

                <div class="d-flex flex-column flex-md-row justify-content-between align-items-center mb-4 pb-3 border-bottom border-light gap-3">
                    <div class="text-center text-md-start">
                        <h5 class="fw-bold text-dark mb-1">
                            <i class="fas fa-history me-2 text-primary"></i>
                            <% if (toko != null) { %>
                                <%=Common.getBahasaConfig("Daftar Transaksi - ")%> <span class="text-primary"><%=toko.getNama()%></span>
                            <% } else { %>
                                <%=Common.getBahasaConfig("Daftar Transaksi Keseluruhan")%>
                            <% } %>
                        </h5>
                        <small class="text-muted"><%=Common.getBahasaConfig("Pantau detail setiap transaksi yang masuk ke dalam sistem.")%></small>
                    </div>
                    <div class="d-flex flex-wrap gap-2 justify-content-center">
                        <button class="btn btn-primary rounded-pill px-4 fw-semibold shadow-sm text-nowrap"
                            type="button" data-bs-toggle="collapse"
                            data-bs-target="#collapseFilterRw<%=rnd%>" aria-expanded="false"
                            aria-controls="collapseFilterRw<%=rnd%>">
                            <i class="fas fa-filter me-2"></i><%=Common.getBahasaConfig("Saring Data")%>
                        </button>
                    </div>
                </div>

                <div class="collapse mb-4" id="collapseFilterRw<%=rnd%>">
                    <div class="card card-body bg-light border-0 rounded-4 p-4 shadow-sm">
                        <div class="row g-3">
                            <div class="col-md-3">
                                <label class="form-label small fw-semibold text-secondary mb-1"><i class="far fa-calendar-alt me-1"></i><%=Common.getBahasaConfig("Tanggal Mulai")%></label>
                                <input type="date" class="form-control form-control-sm rounded-3 filter-rw-<%=rnd%> border-0 shadow-sm" id="filterStartRw<%=rnd%>">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-semibold text-secondary mb-1"><i class="far fa-calendar-check me-1"></i><%=Common.getBahasaConfig("Tanggal Akhir")%></label>
                                <input type="date" class="form-control form-control-sm rounded-3 filter-rw-<%=rnd%> border-0 shadow-sm" id="filterEndRw<%=rnd%>">
                            </div>
                            
                            <% if (toko == null) { %>
                            <div class="col-md-3">
                                <label class="form-label small fw-semibold text-secondary mb-1"><i class="fas fa-store me-1"></i><%=Common.getBahasaConfig("Nama Pedagang")%></label>
                                <input type="text" class="form-control form-control-sm rounded-3 filter-rw-<%=rnd%> border-0 shadow-sm" id="filterMerchantNameRw<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik nama pedagang")%>">
                            </div>
                            <% } %>
                            
                            <div class="col-md-3">
                                <label class="form-label small fw-semibold text-secondary mb-1"><i class="fas fa-user-tag me-1"></i><%=Common.getBahasaConfig("Nama Pembeli")%></label>
                                <input type="text" class="form-control form-control-sm rounded-3 filter-rw-<%=rnd%> border-0 shadow-sm" id="filterBuyerNameRw<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik nama pembeli")%>">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-semibold text-secondary mb-1"><i class="fas fa-box-open me-1"></i><%=Common.getBahasaConfig("Nama Barang")%></label>
                                <input type="text" class="form-control form-control-sm rounded-3 filter-rw-<%=rnd%> border-0 shadow-sm" id="filterItemNameRw<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik nama barang")%>">
                            </div>
                            
                            <div class="col-md-12 d-flex align-items-end gap-2 mt-3 justify-content-end">
                                <button class="btn btn-primary btn-sm rounded-pill px-4 shadow-sm fw-bold" type="button" id="btnSearchRw<%=rnd%>" onclick="triggerSearchRiwayat<%=rnd%>()">
                                    <i class="fas fa-search me-2"></i><%=Common.getBahasaConfig("Terapkan Filter")%>
                                </button>
                                <button class="btn btn-success btn-sm rounded-pill px-4 shadow-sm fw-bold" type="button" id="btnExportExcel<%=rnd%>" onclick="downloadExcelAPI<%=rnd%>()" disabled>
                                    <i class="fas fa-file-excel me-2"></i><%=Common.getBahasaConfig("Unduh Excel")%>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="table-responsive border rounded-3 shadow-sm bg-white">
                    <table class="table table-hover table-striped align-middle mb-0">
                        <thead class="table-dark text-center align-middle">
                            <tr>
                                <th class="fw-semibold text-uppercase small py-3" style="width: 50px;">#</th>
                                <th class="fw-semibold text-uppercase small"><i class="far fa-clock me-1"></i><%=Common.getBahasaConfig("Waktu")%></th>
                                <% if (toko == null) { %>
                                    <th class="fw-semibold text-uppercase small text-start"><i class="fas fa-store me-1"></i><%=Common.getBahasaConfig("Pedagang")%></th>
                                <% } %>
                                <th class="fw-semibold text-uppercase small text-start"><i class="fas fa-box me-1"></i><%=Common.getBahasaConfig("Rincian Barang")%></th>
                                <th class="fw-semibold text-uppercase small text-start"><i class="fas fa-user me-1"></i><%=Common.getBahasaConfig("Pembeli")%></th>
                                <th class="fw-semibold text-uppercase small"><i class="fas fa-wallet me-1"></i><%=Common.getBahasaConfig("Metode")%></th>
                                <th class="fw-semibold text-uppercase small"><i class="fas fa-layer-group me-1"></i><%=Common.getBahasaConfig("Qty")%></th>
                                <th class="fw-semibold text-uppercase small text-end px-3"><i class="fas fa-coins me-1"></i><%=Common.getBahasaConfig("Total Harga")%></th>
                                <th class="fw-semibold text-uppercase small text-center px-3" style="width: 170px;"><i class="fas fa-cogs me-1"></i><%=Common.getBahasaConfig("Aksi")%></th>
                            </tr>
                        </thead>
                        <tbody id="tableRiwayat<%=rnd%>">
                        </tbody>
                    </table>
                </div>

                <div class="d-flex justify-content-between align-items-center mt-4 border-top pt-3">
                    <span class="text-muted small fw-medium bg-light px-3 py-1 rounded-pill border" id="pagingInfoRw<%=rnd%>"></span>
                    <nav>
                        <ul class="pagination pagination-sm mb-0 shadow-sm">
                            <li class="page-item">
                                <button class="page-link text-primary rounded-start-pill px-3" id="btnPrevRw<%=rnd%>" onclick="changePageRw<%=rnd%>(-1)">
                                    <i class="fas fa-chevron-left"></i>
                                </button>
                            </li>
                            <li class="page-item disabled">
                                <span class="page-link fw-bold bg-light text-dark px-3" id="pageNumberRw<%=rnd%>">1</span>
                            </li>
                            <li class="page-item">
                                <button class="page-link text-primary rounded-end-pill px-3" id="btnNextRw<%=rnd%>" onclick="changePageRw<%=rnd%>(1)">
                                    <i class="fas fa-chevron-right"></i>
                                </button>
                            </li>
                        </ul>
                    </nav>
                </div>

            </div>
        </div>
    </div>
</div>

<script>
    var isLoginPedagang<%=rnd%> = <%= toko != null %>;
    var filterTokoSQLTransaksi<%=rnd%> = "<%=sqlFilterToko%>";
    
    var currentPageRw<%=rnd%> = 1;
    var limitPerPageRw<%=rnd%> = 10;
    var totalRecordsRw<%=rnd%> = 0;

    var formatRpRw<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);
    };

    var setDefaultDatesRiwayat<%=rnd%> = () => {
        const today = new Date();
        today.setDate(today.getDate() + 2);
        const threeMonthsAgo = new Date();
        threeMonthsAgo.setMonth(today.getMonth() - 3);
        document.getElementById('filterEndRw<%=rnd%>').value = today.toISOString().split('T')[0];
        document.getElementById('filterStartRw<%=rnd%>').value = threeMonthsAgo.toISOString().split('T')[0];
    };

    var buildSqlFiltersRiwayat<%=rnd%> = () => {
        const tglMulai = document.getElementById('filterStartRw<%=rnd%>').value;
        const tglSampai = document.getElementById('filterEndRw<%=rnd%>').value;
        const member = document.getElementById('filterBuyerNameRw<%=rnd%>').value.trim().replace(/'/g, "''");
        const barang = document.getElementById('filterItemNameRw<%=rnd%>').value.trim().replace(/'/g, "''");

        let filters = " WHERE DATE(a.waktu) BETWEEN DATE('" + tglMulai + "') AND DATE('" + tglSampai + "') " + filterTokoSQLTransaksi<%=rnd%>;
        
        if (!isLoginPedagang<%=rnd%>) {
            const elPedagang = document.getElementById('filterMerchantNameRw<%=rnd%>');
            if (elPedagang) {
                const pedagang = elPedagang.value.trim().replace(/'/g, "''");
                if (pedagang) filters += " AND b.nama ILIKE '%" + pedagang + "%' ";
            }
        }
        
        if (member) filters += " AND a.member ILIKE '%" + member + "%' ";
        if (barang) filters += " AND c.nama ILIKE '%" + barang + "%' ";

        return filters;
    };

    var triggerSearchRiwayat<%=rnd%> = () => {
        currentPageRw<%=rnd%> = 1;
        loadRiwayatAPI<%=rnd%>();
    };

    var loadRiwayatAPI<%=rnd%> = async () => {
        const tbody = document.getElementById('tableRiwayat<%=rnd%>');
        const btnSearch = document.getElementById('btnSearchRw<%=rnd%>');
        const btnExport = document.getElementById('btnExportExcel<%=rnd%>');
        
        const colSpan = isLoginPedagang<%=rnd%> ? 8 : 9;
        
        tbody.innerHTML = '<tr><td colspan="' + colSpan + '" class="text-center py-5 text-muted"><div class="spinner-border text-primary mb-3" role="status"></div><br><span class="fw-medium"><%=Common.getBahasaConfig("Memuat data...")%></span></td></tr>';
        btnSearch.disabled = true;
        btnExport.disabled = true;

        const offset = (currentPageRw<%=rnd%> - 1) * limitPerPageRw<%=rnd%>;
        const sqlFilters = buildSqlFiltersRiwayat<%=rnd%>();

        // Menggabungkan nama barang menggunakan STRING_AGG dan Grouping berdasarkan transaksi
        const sqlQueryData = "SELECT COALESCE(a.pembelian_anggota_koperasi, a.id) AS id_transaksi, " +
                             "TO_CHAR(MAX(a.waktu), 'DD-MM-YYYY HH24:MI') AS waktu_trx, " +
                             "STRING_AGG(c.nama || ' (' || a.qty || ')', ', ') AS namabarang, " +
                             "MAX(a.member) AS member, " +
                             "MAX(b.nama) AS pedagang, " +
                             "MAX(a.jenismember) AS jenismember, " +
                             "MAX(a.carabayar) AS carabayar, " +
                             "SUM(a.qty) AS qty, " +
                             "SUM(a.total) AS total, " +
                             "BOOL_AND(COALESCE(a.terlayani, false)) AS terlayani " +
                             "FROM koperasi.pembelian a " +
                             "INNER JOIN koperasi.toko b ON (a.toko = b.id and b.aktif = true) " +
                             "INNER JOIN koperasi.produk c ON (c.id = a.produk) " +
                             sqlFilters +
                             " GROUP BY COALESCE(a.pembelian_anggota_koperasi, a.id) " +
                             " ORDER BY MAX(a.waktu) DESC, id_transaksi DESC LIMIT " + limitPerPageRw<%=rnd%> + " OFFSET " + offset + ";";

        const sqlQueryCount = "SELECT COUNT(DISTINCT COALESCE(a.pembelian_anggota_koperasi, a.id)) AS jumlah " +
                              "FROM koperasi.pembelian a " +
                              "INNER JOIN koperasi.toko b ON (a.toko = b.id and b.aktif = true) " +
                              "INNER JOIN koperasi.produk c ON (c.id = a.produk) " +
                              sqlFilters + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetch('<%=Common.ROOT%>/Data', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ sql: sqlQueryCount, action: "sql" })
                }),
                fetch('<%=Common.ROOT%>/Data', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ sql: sqlQueryData, action: "sql" })
                })
            ]);

            const dataCount = await resCount.json();
            const dataResponse = await resData.json();

            totalRecordsRw<%=rnd%> = (dataCount.data && dataCount.data[0]) ? parseInt(dataCount.data[0].jumlah || 0) : 0;
            const recordList = dataResponse.data || [];

            let htmlTbody = '';
            if (recordList.length === 0) {
                htmlTbody = '<tr><td colspan="' + colSpan + '" class="text-center text-muted py-5"><i class="fas fa-folder-open fa-3x mb-3 text-secondary opacity-50 d-block"></i><span class="fw-medium"><%=Common.getBahasaConfig("Tidak ada data transaksi ditemukan.")%></span></td></tr>';
            } else {
                let no = offset + 1;
                recordList.forEach(row => {
                    const carabayar = (row.carabayar || '').toLowerCase();
                    const badgeColor = carabayar.includes('tunai') ? 'bg-secondary' : 'bg-info text-dark';
                    
                    htmlTbody += '<tr>';
                    htmlTbody += '<td class="text-center text-muted fw-medium">' + (no++) + '</td>';
                    htmlTbody += '<td class="text-center"><span class="text-muted small fw-medium">' + (row.waktu_trx || '-') + '</span></td>';
                    
                    if (!isLoginPedagang<%=rnd%>) {
                        htmlTbody += '<td class="text-start fw-bold text-dark">' + (row.pedagang || '-') + '</td>';
                    }
                    
                    htmlTbody += '<td class="text-start fw-bold text-primary" style="max-width: 250px;">' + (row.namabarang || '-') + '</td>';
                    htmlTbody += '<td class="text-start fw-medium">' + (row.member || '-') + '</td>';
                    htmlTbody += '<td class="text-center"><span class="badge ' + badgeColor + ' rounded-pill px-3 py-1 shadow-sm">' + (row.carabayar || '-') + '</span></td>';
                    htmlTbody += '<td class="text-center fw-bold text-dark">' + (row.qty || 0) + '</td>';
                    htmlTbody += '<td class="text-end fw-bold text-success px-3">' + formatRpRw<%=rnd%>(row.total) + '</td>';
                    
                    htmlTbody += '<td class="text-center text-nowrap">' +
                                    '<button class="btn btn-sm btn-outline-info shadow-sm px-3 fw-bold me-1" onclick="cetakStrukPOS<%=rnd%>(' + row.id_transaksi + ')" title="<%=Common.getBahasaConfig("Cetak Struk")%>"><i class="fas fa-print me-1"></i>Cetak</button>' +
                                    '<button class="btn btn-sm btn-outline-danger shadow-sm px-3 fw-bold" onclick="hapusTransaksi<%=rnd%>(' + row.id_transaksi + ')" title="<%=Common.getBahasaConfig("Hapus Transaksi")%>"><i class="fas fa-trash-alt me-1"></i>Hapus</button>' +
                                 '</td>';
                    htmlTbody += '</tr>';
                });
            }
            tbody.innerHTML = htmlTbody;
            updatePaginationUIRiwayat<%=rnd%>();

        } catch (error) {
            console.error("API Error:", error);
            tbody.innerHTML = '<tr><td colspan="' + colSpan + '" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal mengambil data riwayat.")%></td></tr>';
        } finally {
            btnSearch.disabled = false;
            btnExport.disabled = totalRecordsRw<%=rnd%> === 0;
        }
    };

    var updatePaginationUIRiwayat<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecordsRw<%=rnd%> / limitPerPageRw<%=rnd%>) || 1;
        document.getElementById('pageNumberRw<%=rnd%>').innerText = currentPageRw<%=rnd%> + " / " + totalPages;
        
        const startIdx = totalRecordsRw<%=rnd%> > 0 ? ((currentPageRw<%=rnd%> - 1) * limitPerPageRw<%=rnd%> + 1) : 0;
        const endIdx = Math.min(currentPageRw<%=rnd%> * limitPerPageRw<%=rnd%>, totalRecordsRw<%=rnd%>);
        
        document.getElementById('pagingInfoRw<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startIdx + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari total")%> <b class="text-dark">' + totalRecordsRw<%=rnd%> + '</b> <%=Common.getBahasaConfig("transaksi")%>';
        
        document.getElementById('btnPrevRw<%=rnd%>').disabled = (currentPageRw<%=rnd%> === 1);
        document.getElementById('btnNextRw<%=rnd%>').disabled = (currentPageRw<%=rnd%> === totalPages);
    };

    var changePageRw<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecordsRw<%=rnd%> / limitPerPageRw<%=rnd%>);
        if (direction === -1 && currentPageRw<%=rnd%> > 1) {
            currentPageRw<%=rnd%>--;
            loadRiwayatAPI<%=rnd%>();
        } else if (direction === 1 && currentPageRw<%=rnd%> < totalPages) {
            currentPageRw<%=rnd%>++;
            loadRiwayatAPI<%=rnd%>();
        }
    };

    // ==========================================
    // EXCEL DOWNLOAD 
    // ==========================================
    var downloadExcelAPI<%=rnd%> = async () => {
        const btnExport = document.getElementById('btnExportExcel<%=rnd%>');
        const originalHtml = btnExport.innerHTML;
        
        btnExport.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span><%=Common.getBahasaConfig("Mengunduh...")%>';
        btnExport.disabled = true;

        const sqlFilters = buildSqlFiltersRiwayat<%=rnd%>();
        
        const sqlQueryExport = "SELECT COALESCE(a.pembelian_anggota_koperasi, a.id) AS id_transaksi, " +
                               "TO_CHAR(MAX(a.waktu), 'DD-MM-YYYY HH24:MI') AS waktu_trx, " +
                               "STRING_AGG(c.nama || ' (' || a.qty || ')', ', ') AS namabarang, " +
                               "MAX(a.member) AS member, " +
                               "MAX(b.nama) AS pedagang, " +
                               "MAX(a.jenismember) AS jenismember, " +
                               "MAX(a.carabayar) AS carabayar, " +
                               "SUM(a.qty) AS qty, " +
                               "SUM(a.total) AS total, " +
                               "BOOL_AND(COALESCE(a.terlayani, false)) AS terlayani " +
                               "FROM koperasi.pembelian a " +
                               "INNER JOIN koperasi.toko b ON (a.toko = b.id and b.aktif = true) " +
                               "INNER JOIN koperasi.produk c ON (c.id = a.produk) " +
                               sqlFilters +
                               " GROUP BY COALESCE(a.pembelian_anggota_koperasi, a.id) " +
                               " ORDER BY MAX(a.waktu) DESC, id_transaksi DESC;";

        try {
            const resData = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sql: sqlQueryExport, action: "sql" })
            });
            const dataResponse = await resData.json();
            const recordList = dataResponse.data || [];

            if (recordList.length === 0) {
                alert('<%=Common.getBahasaConfigJS("Tidak ada data untuk diunduh.")%>');
                return;
            }

            let tableHtml = '<html xmlns:x="urn:schemas-microsoft-com:office:excel">';
            tableHtml += '<head><meta charset="UTF-8"><style>';
            tableHtml += '.num-format { mso-number-format:"\\#\\,\\#\\#0"; text-align: center; }';
            tableHtml += '.rp-format { mso-number-format:"\\#\\,\\#\\#0"; text-align: right; }';
            tableHtml += 'th { background-color: #f8f9fa; font-weight: bold; border: 1px solid black; padding: 5px; }';
            tableHtml += 'td { border: 1px solid black; padding: 5px; }';
            tableHtml += '</style></head><body>';
            
            tableHtml += '<h4>Laporan Daftar Transaksi</h4>';
            <% if (toko != null) { %>
                tableHtml += '<p>Pedagang: <b><%=toko.getNama()%></b></p>';
            <% } %>

            tableHtml += '<table><thead><tr>';
            tableHtml += '<th>Waktu Transaksi</th>';
            if (!isLoginPedagang<%=rnd%>) {
                tableHtml += '<th>Nama Pedagang</th>';
            }
            tableHtml += '<th>Rincian Barang</th><th>Nama Pembeli</th><th>Tipe Anggota</th>';
            tableHtml += '<th>Metode Pembayaran</th><th>Kuantitas</th><th>Total Harga (Rp)</th><th>Status Dilayani</th>';
            tableHtml += '</tr></thead><tbody>';

            recordList.forEach(row => {
                const isTerlayaniTxt = (row.terlayani === true || row.terlayani === 't' || row.terlayani === 'true') ? 'Selesai' : 'Belum';
                tableHtml += '<tr>';
                tableHtml += '<td style="text-align: center;">' + (row.waktu_trx || '-') + '</td>';
                if (!isLoginPedagang<%=rnd%>) {
                    tableHtml += '<td>' + (row.pedagang || '-') + '</td>';
                }
                tableHtml += '<td>' + (row.namabarang || '-') + '</td>';
                tableHtml += '<td>' + (row.member || '-') + '</td>';
                tableHtml += '<td style="text-align: center;">' + (row.jenismember || '-') + '</td>';
                tableHtml += '<td style="text-align: center;">' + (row.carabayar || '-') + '</td>';
                tableHtml += '<td class="num-format">' + (row.qty || 0) + '</td>';
                tableHtml += '<td class="rp-format">' + (row.total || 0) + '</td>';
                tableHtml += '<td style="text-align: center;">' + isTerlayaniTxt + '</td>';
                tableHtml += '</tr>';
            });
            
            tableHtml += '</tbody></table></body></html>';

            const blob = new Blob([tableHtml], { type: 'application/vnd.ms-excel' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement("a");
            
            const timestamp = new Date().toISOString().slice(0, 10).replace(/-/g, "");
            const fileName = isLoginPedagang<%=rnd%> ? "Trans_Toko_<%=toko != null ? toko.getId() : ""%>_" : "Trans_Semua_";
            
            link.download = fileName + timestamp + ".xls";
            link.href = url;
            
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            setTimeout(() => URL.revokeObjectURL(url), 100);

        } catch (error) {
            console.error("Export Error:", error);
            alert('<%=Common.getBahasaConfigJS("Gagal mengunduh data dari server.")%>');
        } finally {
            btnExport.innerHTML = originalHtml;
            btnExport.disabled = false;
        }
    };

    // Fungsi Hapus (Membutuhkan End-Point Backend Delete)
    var hapusTransaksi<%=rnd%> = (idTransaksi) => {
    	prosesDeleteData('<%=PembelianAnggotaKoperasi.class.getName()%>',idTransaksi, function(){ loadRiwayatAPI<%=rnd%>(); } );
    };

    // 5. Cetak Struk (Metode Tambahan Jika Ingin Akses ID Pembelian Langsung)
    const cetakStrukPOS<%=rnd%> = (idPembelian) => {
        // Asumsi URL cetak struk bawaan AIS
        const printUrl = "<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=kantin%2Fpos&s=cetak_struk&id=" + idPembelian;
        
        // Buka PopUp Print
        const printWindow = window.open(printUrl, 'Cetak Struk Koperasi', 'height=600,width=400');
        if (window.focus && printWindow) {
            printWindow.focus();
        }
    };

    // ==========================================
    // INITIALIZATION LISTENER MAIN
    // ==========================================
    document.addEventListener("DOMContentLoaded", () => {
        // Init Tab 2 (Riwayat)
        setDefaultDatesRiwayat<%=rnd%>();
        const filterInputsRw = document.querySelectorAll('.filter-rw-<%=rnd%>');
        filterInputsRw.forEach(input => {
            input.addEventListener("keydown", (event) => {
                if (event.key === "Enter") {
                    event.preventDefault(); 
                    triggerSearchRiwayat<%=rnd%>();
                }
            });
        });
        loadRiwayatAPI<%=rnd%>();
    });
</script>