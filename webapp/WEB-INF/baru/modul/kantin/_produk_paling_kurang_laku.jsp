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
String rnd = Common.getGeneratedBarCode(7);

// Menyiapkan variabel filter SQL jika user adalah pedagang
String sqlFilterToko = "";
if (toko != null) {
    // Memfilter dari tabel master produk
    sqlFilterToko = " AND c.toko = " + toko.getId() + " ";
}
%>

<div class="row mb-4 animate__animated animate__fadeInUp">
    <div class="col-12">
        <div class="card border-0 shadow-sm rounded-4 border-top border-secondary border-4">
            
            <div class="card-header bg-white border-0 pt-4 pb-3 px-4 border-bottom border-light">
                <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-3">
                    <div>
                        <h5 class="fw-bold text-dark mb-1">
                            <i class="fas fa-hourglass-half text-secondary me-2"></i>
                            <% if (toko != null) { %>
                                <%=Common.getBahasaConfig("Produk Kurang Laku - ")%> <span class="text-secondary"><%=toko.getNama()%></span>
                            <% } else { %>
                                <%=Common.getBahasaConfig("Produk Kurang Laku (Slow-Moving)")%>
                            <% } %>
                        </h5>
                        <small class="text-muted"><%=Common.getBahasaConfig("Daftar barang dengan penjualan paling sedikit (atau 0) untuk evaluasi menu/stok.")%></small>
                    </div>
                </div>
                
                <div class="d-flex align-items-center gap-2 flex-wrap bg-light p-2 rounded-3 border">
                    <select class="form-select form-select-sm rounded-3 shadow-sm" id="filterPeriodeLambat<%=rnd%>" style="width: 150px; cursor: pointer;">
                        <option value="14 days"><%=Common.getBahasaConfig("14 Hari Terakhir")%></option>
                        <option value="1 month" selected><%=Common.getBahasaConfig("30 Hari Terakhir")%></option>
                        <option value="3 months"><%=Common.getBahasaConfig("3 Bulan Terakhir")%></option>
                    </select>

                    <% if (toko == null) { %>
                    <div class="input-group input-group-sm shadow-sm" style="width: 160px;">
                        <span class="input-group-text bg-white border-end-0">
                            <i class="fas fa-store text-muted"></i>
                        </span>
                        <input type="text" class="form-control border-start-0 ps-0 bg-white" id="searchPedagangLambat<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Pedagang...")%>">
                    </div>
                    <% } else { %>
                        <input type="hidden" id="searchPedagangLambat<%=rnd%>" value="">
                    <% } %>

                    <div class="input-group input-group-sm shadow-sm" style="width: 160px;">
                        <span class="input-group-text bg-white border-end-0">
                            <i class="fas fa-box-open text-muted"></i>
                        </span>
                        <input type="text" class="form-control border-start-0 ps-0 bg-white" id="searchProdukLambat<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Nama Barang...")%>">
                    </div>

                    <div class="input-group input-group-sm shadow-sm" style="width: 190px;">
                        <span class="input-group-text bg-white border-end-0 small fw-medium text-muted" title="<%=Common.getBahasaConfig("Jumlah Terjual")%>"><i class="fas fa-shopping-cart me-1"></i> Qty</span>
                        <input type="number" class="form-control text-center px-1 border-start-0" id="searchQtyMinLambat<%=rnd%>" placeholder="Min" value="0">
                        <span class="input-group-text bg-white border-start-0 border-end-0 px-1">-</span>
                        <input type="number" class="form-control text-center px-1 border-start-0" id="searchQtyMaxLambat<%=rnd%>" placeholder="Max" value="5">
                    </div>

                    <button class="btn btn-sm btn-secondary rounded-pill px-4 fw-bold shadow-sm text-nowrap" onclick="loadProdukLambat<%=rnd%>()" id="btnRefreshLambat<%=rnd%>">
                        <i class="fas fa-search me-1"></i><%=Common.getBahasaConfig("Saring")%>
                    </button>
                </div>
            </div>
            
            <div class="card-body p-4 pt-2">
                <div id="loadingLambat<%=rnd%>" class="text-center py-5" style="display: none;">
                    <div class="spinner-border text-secondary mb-3" role="status"></div><br>
                    <span class="fw-medium text-muted"><%=Common.getBahasaConfig("Menganalisis pergerakan produk...")%></span>
                </div>
                
                <div class="row" id="listProdukLambatContainer<%=rnd%>">
                    <div class="col-12">
                        <div style="max-height: 400px; overflow-y: auto;" class="pe-2">
                            <ul class="list-group list-group-flush" id="listProdukLambat<%=rnd%>">
                                </ul>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    const langLambat<%=rnd%> = {
        emptyData: '<%=Common.getBahasaConfigJS("Hebat! Semua produk di luar rentang saringan ini terjual dengan baik, atau tidak ada data yang cocok.")%>',
        errorFetch: '<%=Common.getBahasaConfigJS("Gagal mengambil data dari peladen.")%>',
        terjual: '<%=Common.getBahasaConfigJS("Terjual")%>',
        qty: '<%=Common.getBahasaConfigJS("Qty")%>'
    };

    const isLoginPedagangLambat<%=rnd%> = <%= toko != null %>;
    const filterTokoSQLLambat<%=rnd%> = "<%=sqlFilterToko%>";

    const loadProdukLambat<%=rnd%> = async () => {
        const listContainer = document.getElementById('listProdukLambat<%=rnd%>');
        const loadingBox = document.getElementById('loadingLambat<%=rnd%>');
        const btnRefresh = document.getElementById('btnRefreshLambat<%=rnd%>');
        
        // Mengambil nilai filter
        const periodeInterval = document.getElementById('filterPeriodeLambat<%=rnd%>').value;
        const searchProduk = document.getElementById('searchProdukLambat<%=rnd%>').value.trim();
        const qtyMin = document.getElementById('searchQtyMinLambat<%=rnd%>').value;
        const qtyMax = document.getElementById('searchQtyMaxLambat<%=rnd%>').value;
        
        let searchPedagang = "";
        const elPedagang = document.getElementById('searchPedagangLambat<%=rnd%>');
        if (elPedagang) {
            searchPedagang = elPedagang.value.trim();
        }
        
        // Reset UI
        listContainer.innerHTML = '';
        loadingBox.style.display = 'block';
        btnRefresh.disabled = true;

        // Membangun Filter SQL
        let filterPencarianSQL = " "; 
        if (searchPedagang !== "" && !isLoginPedagangLambat<%=rnd%>) {
            const safeSearchPedagang = searchPedagang.replace(/'/g, "''");
            filterPencarianSQL += " AND b.nama ILIKE '%" + safeSearchPedagang + "%' ";
        }
        if (searchProduk !== "") {
            const safeSearchProduk = searchProduk.replace(/'/g, "''");
            filterPencarianSQL += " AND c.nama ILIKE '%" + safeSearchProduk + "%' ";
        }

        // Membangun HAVING SQL untuk filter 'Qty Terjual (Between)'
        let minVal = qtyMin !== "" ? parseInt(qtyMin) : 0;
        let maxVal = qtyMax !== "" ? parseInt(qtyMax) : 999999;
        let havingSQL = " HAVING COALESCE(SUM(a.qty), 0) BETWEEN " + minVal + " AND " + maxVal + " ";

        // Kueri SQL: Menggunakan LEFT JOIN agar produk dengan 0 transaksi tetap muncul
        // Asumsi: Hanya mengecek produk yang statusnya 'aktif' (agar produk lama yg sudah dihapus tidak muncul terus)
        const sqlQuery = "SELECT " +
            "b.nama AS nama_pedagang, " +
            "c.nama AS nama_produk, " +
            "COALESCE(SUM(a.qty), 0) AS total_terjual " +
            "FROM koperasi.produk c " +
            "INNER JOIN koperasi.toko b ON c.toko = b.id " +
            // Perhatikan kurung buka-tutup pada relasi ON agar filter tanggal hanya berlaku untuk join pembelian
            "LEFT JOIN koperasi.pembelian a ON (a.produk = c.id AND DATE(a.waktu) >= CURRENT_DATE - INTERVAL '" + periodeInterval + "') " +
            "WHERE (c.aktif = true OR c.aktif IS NULL) " + filterTokoSQLLambat<%=rnd%> + filterPencarianSQL +
            "GROUP BY b.id, b.nama, c.id, c.nama " +
            havingSQL +
            "ORDER BY total_terjual ASC, c.nama ASC " +
            "LIMIT 20;"; // Tampilkan lebih banyak (20 items) karena format List lebih ringkas dari Tabel

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sql: sqlQuery, action: "sql" })
            });
            
            const result = await response.json();
            const recordList = result.data || [];
            
            let htmlList = '';

            if (recordList.length === 0) {
                htmlList = '<div class="text-center py-5 text-success"><i class="fas fa-thumbs-up fa-3x mb-3 opacity-75 d-block"></i><span class="fw-bold fs-5">' + langLambat<%=rnd%>.emptyData + '</span></div>';
            } else {
                recordList.forEach((row) => {
                    const terjual = parseFloat(row.total_terjual || 0);
                    
                    // Logika Visual
                    let badgeClass = '';
                    if (terjual === 0) {
                        badgeClass = 'bg-danger text-white shadow-sm'; // Merah mencolok jika sama sekali tidak laku
                    } else if (terjual <= 5) {
                        badgeClass = 'bg-warning text-dark'; // Kuning jika lakunya sangat sedikit
                    } else {
                        badgeClass = 'bg-light text-secondary border'; // Abu-abu jika lumayan
                    }

                    // Tampilan Toko (Sembunyikan jika login sebagai Pedagang)
                    const labelToko = !isLoginPedagangLambat<%=rnd%> 
                        ? '<small class="text-muted d-block mt-1"><i class="fas fa-store me-1 opacity-50"></i>' + (row.nama_pedagang || '-') + '</small>' 
                        : '';

                    htmlList += 
                        '<li class="list-group-item d-flex justify-content-between align-items-center px-3 py-3 mb-2 rounded-3 border transition-all table-hover bg-white shadow-sm">' +
                            '<div class="d-flex align-items-center">' +
                                '<div class="bg-secondary bg-opacity-10 p-3 rounded-circle me-3 text-secondary shadow-sm">' +
                                    '<i class="fas fa-box fa-lg"></i>' +
                                '</div>' +
                                '<div>' +
                                    '<h6 class="mb-0 fw-bold text-dark">' + (row.nama_produk || '-') + '</h6>' +
                                    labelToko +
                                '</div>' +
                            '</div>' +
                            '<span class="badge ' + badgeClass + ' rounded-pill px-3 py-2 fs-6">' +
                                terjual + ' <small class="fw-normal">' + langLambat<%=rnd%>.qty + ' ' + langLambat<%=rnd%>.terjual + '</small>' +
                            '</span>' +
                        '</li>';
                });
            }
            
            listContainer.innerHTML = htmlList;

        } catch (error) {
            console.error("Gagal mengambil data lambat:", error);
            listContainer.innerHTML = '<div class="text-center text-danger py-4"><i class="fas fa-exclamation-triangle fa-2x mb-2 d-block"></i>' + langLambat<%=rnd%>.errorFetch + '</div>';
        } finally {
            loadingBox.style.display = 'none';
            btnRefresh.disabled = false;
        }
    };

    // Auto-load saat halaman siap
    document.addEventListener("DOMContentLoaded", () => {
        const filterDropdown = document.getElementById('filterPeriodeLambat<%=rnd%>');
        
        // Auto-refresh saat ganti dropdown hari
        filterDropdown.addEventListener("change", () => {
            loadProdukLambat<%=rnd%>();
        });

        // Trigger Pencarian dengan tombol Enter
        const filterInputs = [
            document.getElementById('searchProdukLambat<%=rnd%>'),
            document.getElementById('searchPedagangLambat<%=rnd%>'),
            document.getElementById('searchQtyMinLambat<%=rnd%>'),
            document.getElementById('searchQtyMaxLambat<%=rnd%>')
        ];
        
        filterInputs.forEach(input => {
            if(input) {
                input.addEventListener("keydown", (event) => {
                    if (event.key === "Enter") {
                        event.preventDefault();
                        loadProdukLambat<%=rnd%>();
                    }
                });
            }
        });

        loadProdukLambat<%=rnd%>();
    });
</script>