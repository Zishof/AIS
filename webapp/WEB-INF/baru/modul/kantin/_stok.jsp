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
    // Memfilter data produk berdasarkan ID toko pedagang tersebut
    sqlFilterToko = " AND c.toko = " + toko.getId() + " ";
}
%>

<div class="row mb-4 animate__animated animate__fadeInUp">
    <div class="col-12">
        <div class="card border-0 shadow-sm rounded-4 border-top border-warning border-4">
            
            <div class="card-header bg-white border-0 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-3 border-bottom border-light">
                <div>
                    <h5 class="fw-bold text-dark mb-1">
                        <i class="fas fa-exclamation-triangle text-warning me-2"></i>
                        <% if (toko != null) { %>
                            <%=Common.getBahasaConfig("Peringatan Stok - ")%> <span class="text-warning"><%=toko.getNama()%></span>
                        <% } else { %>
                            <%=Common.getBahasaConfig("Peringatan Stok Keseluruhan")%>
                        <% } %>
                    </h5>
                    <small class="text-muted"><%=Common.getBahasaConfig("Daftar produk dengan stok yang perlu segera dilakukan pengadaan ulang/kulakan.")%></small>
                </div>
                
                <div class="d-flex align-items-center gap-2 flex-wrap">
                    
                    <% if (toko == null) { %>
                    <div class="input-group input-group-sm shadow-sm" style="width: 160px;">
                        <span class="input-group-text bg-light border-end-0">
                            <i class="fas fa-store text-muted"></i>
                        </span>
                        <input type="text" class="form-control border-start-0 ps-0 bg-light" id="searchPedagangStok<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Pedagang...")%>">
                    </div>
                    <% } else { %>
                        <input type="hidden" id="searchPedagangStok<%=rnd%>" value="">
                    <% } %>

                    <div class="input-group input-group-sm shadow-sm" style="width: 160px;">
                        <span class="input-group-text bg-light border-end-0">
                            <i class="fas fa-box-open text-muted"></i>
                        </span>
                        <input type="text" class="form-control border-start-0 ps-0 bg-light" id="searchProdukStok<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Produk...")%>">
                    </div>

                    <div class="input-group input-group-sm shadow-sm" style="width: 200px;">
                        <span class="input-group-text bg-light border-end-0 small"><%=Common.getBahasaConfig("Stok")%></span>
                        <input type="number" class="form-control text-center" id="searchStokMin<%=rnd%>" placeholder="Min" value="0">
                        <span class="input-group-text bg-light border-start-0 border-end-0">-</span>
                        <input type="number" class="form-control text-center" id="searchStokMax<%=rnd%>" placeholder="Max" value="10">
                    </div>

                    <button class="btn btn-sm btn-outline-warning text-dark rounded-pill px-3 fw-semibold shadow-sm text-nowrap" onclick="loadStokMenipis<%=rnd%>()" id="btnRefreshStok<%=rnd%>">
                        <i class="fas fa-search me-1"></i><%=Common.getBahasaConfig("Cari")%>
                    </button>
                    
                    <button class="btn btn-sm btn-success rounded-pill px-3 fw-semibold shadow-sm text-nowrap" onclick="downloadStokExcel<%=rnd%>()" id="btnExcelStok<%=rnd%>" disabled>
                        <i class="fas fa-file-excel me-1"></i><%=Common.getBahasaConfig("Unduh")%>
                    </button>
                </div>
            </div>
            
            <div class="card-body p-4 pt-0">
                <div class="table-responsive border rounded-3 shadow-sm mt-3" style="max-height: 400px; overflow-y: auto;">
                    <table class="table table-hover align-middle mb-0 text-center">
                        <thead class="table-dark align-middle sticky-top">
                            <tr>
                                <th class="fw-bold text-uppercase small px-3 text-white" style="width: 60px;">#</th>
                                <% if (toko == null) { %>
                                    <th class="text-start fw-bold text-uppercase small px-3 text-white"><i class="fas fa-store me-1"></i><%=Common.getBahasaConfig("Nama Pedagang")%></th>
                                <% } %>
                                <th class="text-start fw-bold text-uppercase small px-3 text-white"><i class="fas fa-box-open me-1"></i><%=Common.getBahasaConfig("Nama Produk")%></th>
                                <th class="fw-bold text-uppercase small px-3 text-white"><i class="fas fa-cubes me-1"></i><%=Common.getBahasaConfig("Sisa Stok")%></th>
                                <th class="fw-bold text-uppercase small px-3 text-white"><i class="fas fa-info-circle me-1"></i><%=Common.getBahasaConfig("Status")%></th>
                            </tr>
                        </thead>
                        <tbody id="tabelStokMenipis<%=rnd%>">
                            <tr>
                                <td colspan="<%= toko == null ? "5" : "4" %>" class="text-center py-5 text-muted">
                                    <div class="spinner-border text-warning mb-3" role="status"></div><br>
                                    <span class="fw-medium"><%=Common.getBahasaConfig("Memeriksa data stok...")%></span>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    const langStok<%=rnd%> = {
        emptyData: '<%=Common.getBahasaConfigJS("Luar biasa! Stok semua produk masih dalam kondisi aman atau tidak ditemukan berdasarkan filter.")%>',
        errorFetch: '<%=Common.getBahasaConfigJS("Gagal mengambil data stok dari peladen. Silakan coba lagi.")%>',
        loading: '<%=Common.getBahasaConfigJS("Memeriksa data stok...")%>',
        statusHabis: '<%=Common.getBahasaConfigJS("HABIS (Kosong)")%>',
        statusKritis: '<%=Common.getBahasaConfigJS("KRITIS (<= 5)")%>',
        statusMenipis: '<%=Common.getBahasaConfigJS("MENIPIS")%>'
    };

    const isLoginPedagangStok<%=rnd%> = <%= toko != null %>;
    const filterTokoSQLStok<%=rnd%> = "<%=sqlFilterToko%>";
    const baseColSpanStok<%=rnd%> = isLoginPedagangStok<%=rnd%> ? 4 : 5;

    const formatAngkaStok<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID').format(angka || 0);
    };

    let rawDataStok<%=rnd%> = [];

    const loadStokMenipis<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelStokMenipis<%=rnd%>');
        const btnRefresh = document.getElementById('btnRefreshStok<%=rnd%>');
        const btnExcel = document.getElementById('btnExcelStok<%=rnd%>');
        
        // Mengambil nilai filter pencarian
        const searchProduk = document.getElementById('searchProdukStok<%=rnd%>').value.trim();
        const stokMin = document.getElementById('searchStokMin<%=rnd%>').value;
        const stokMax = document.getElementById('searchStokMax<%=rnd%>').value;
        
        let searchPedagang = "";
        const elSearchPedagang = document.getElementById('searchPedagangStok<%=rnd%>');
        if (elSearchPedagang) {
             searchPedagang = elSearchPedagang.value.trim();
        }

        // Status Loading
        tbody.innerHTML = '<tr><td colspan="' + baseColSpanStok<%=rnd%> + '" class="text-center py-5 text-muted"><div class="spinner-border text-warning mb-3" role="status"></div><br><span class="fw-medium">' + langStok<%=rnd%>.loading + '</span></td></tr>';
        btnRefresh.disabled = true;
        btnExcel.disabled = true;

        // Membangun Filter SQL
        let filterPencarianSQL = " WHERE 1=1 "; 
        
        // Filter Stok Default
        let valStokMin = stokMin !== "" ? parseInt(stokMin) : -9999;
        let valStokMax = stokMax !== "" ? parseInt(stokMax) : 10;
        filterPencarianSQL += " AND COALESCE(c.stok, 0) BETWEEN " + valStokMin + " AND " + valStokMax + " ";

        if (searchPedagang !== "" && !isLoginPedagangStok<%=rnd%>) {
            const safeSearchPedagang = searchPedagang.replace(/'/g, "''");
            filterPencarianSQL += " AND b.nama ILIKE '%" + safeSearchPedagang + "%' ";
        }
        if (searchProduk !== "") {
            const safeSearchProduk = searchProduk.replace(/'/g, "''");
            filterPencarianSQL += " AND c.nama ILIKE '%" + safeSearchProduk + "%' ";
        }

        // Kueri SQL
        const sqlQuery = "SELECT " +
            "b.nama AS nama_pedagang, " +
            "c.nama AS nama_produk, " +
            "COALESCE(c.stok, 0) AS sisa_stok " +
            "FROM koperasi.produk c " +
            "INNER JOIN koperasi.toko b ON c.toko = b.id " +
            filterPencarianSQL + filterTokoSQLStok<%=rnd%> +
            "ORDER BY b.nama ASC, c.stok ASC;"; // Diurutkan berdasarkan toko lalu stok paling menipis

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sql: sqlQuery, action: "sql" })
            });
            
            const result = await response.json();
            rawDataStok<%=rnd%> = result.data || [];
            
            let htmlTbody = '';

            if (rawDataStok<%=rnd%>.length === 0) {
                htmlTbody = '<tr><td colspan="' + baseColSpanStok<%=rnd%> + '" class="text-center text-success py-5"><i class="fas fa-check-circle fa-3x mb-3 opacity-75 d-block"></i><span class="fw-bold">' + langStok<%=rnd%>.emptyData + '</span></td></tr>';
            } else {
                let no = 1;
                let currentPedagang = '';

                rawDataStok<%=rnd%>.forEach((row) => {
                    const stok = parseFloat(row.sisa_stok || 0);
                    
                    // Logika Visual Grouping Pedagang Baru
                    let isNewPedagang = (currentPedagang !== row.nama_pedagang);
                    if (isNewPedagang) {
                        currentPedagang = row.nama_pedagang;
                        if(!isLoginPedagangStok<%=rnd%>) { no = 1; } // Reset nomor jika ganti pedagang dan login sbg admin
                    }
                    
                    // Logika Visual Status Stok
                    let badgeClass = '';
                    let statusText = '';
                    let rowClass = '';
                    
                    if (stok <= 0) {
                        badgeClass = 'bg-danger';
                        statusText = '<i class="fas fa-times-circle me-1"></i>' + langStok<%=rnd%>.statusHabis;
                        rowClass = 'table-danger border-danger'; 
                    } else if (stok <= 5) {
                        badgeClass = 'bg-warning text-dark';
                        statusText = '<i class="fas fa-exclamation-triangle me-1"></i>' + langStok<%=rnd%>.statusKritis;
                        rowClass = 'table-warning border-warning'; 
                    } else {
                        badgeClass = 'bg-info text-dark';
                        statusText = '<i class="fas fa-info-circle me-1"></i>' + langStok<%=rnd%>.statusMenipis;
                    }
                    
                    // Background styling untuk grouping (Hanya untuk Admin)
                    const bgGroupClass = (!isLoginPedagangStok<%=rnd%> && isNewPedagang) ? 'border-top border-dark border-2' : '';
                    const finalRowClass = rowClass !== '' ? rowClass + ' ' + bgGroupClass : bgGroupClass;

                    htmlTbody += '<tr class="' + finalRowClass + '">';
                    htmlTbody += '<td class="text-center text-muted fw-semibold border-end-0">' + no++ + '</td>';
                    
                    if (!isLoginPedagangStok<%=rnd%>) {
                         const namaPedagangTampil = isNewPedagang ? '<span class="fw-bold text-dark">' + row.nama_pedagang + '</span>' : '<span class="text-muted opacity-50">' + row.nama_pedagang + '</span>';
                         htmlTbody += '<td class="text-start px-3 border-start-0 border-end-0">' + namaPedagangTampil + '</td>';
                    }
                    
                    htmlTbody += '<td class="text-start px-3 fw-semibold border-start-0 border-end-0">' + (row.nama_produk || '-') + '</td>';
                    htmlTbody += '<td class="text-center fs-5 fw-bold border-start-0 border-end-0">' + formatAngkaStok<%=rnd%>(stok) + '</td>';
                    htmlTbody += '<td class="text-center px-3 border-start-0"><span class="badge ' + badgeClass + ' rounded-pill px-3 py-2 shadow-sm w-100">' + statusText + '</span></td>';
                    
                    htmlTbody += '</tr>';
                });
                
                btnExcel.disabled = false;
            }
            
            tbody.innerHTML = htmlTbody;

        } catch (error) {
            console.error("Gagal mengambil data stok:", error);
            tbody.innerHTML = '<tr><td colspan="' + baseColSpanStok<%=rnd%> + '" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i>' + langStok<%=rnd%>.errorFetch + '</td></tr>';
        } finally {
            btnRefresh.disabled = false;
        }
    };

    // Fungsi Unduh Excel untuk Daftar Belanja (Kulakan)
    const downloadStokExcel<%=rnd%> = () => {
        if (!rawDataStok<%=rnd%> || rawDataStok<%=rnd%>.length === 0) return;

        let tableHtml = '<html xmlns:x="urn:schemas-microsoft-com:office:excel">';
        tableHtml += '<head><meta charset="UTF-8"><style>';
        tableHtml += '.num-format { mso-number-format:"\\#\\,\\#\\#0"; text-align: center; }';
        tableHtml += 'th { background-color: #f8f9fa; font-weight: bold; }';
        tableHtml += '</style></head><body>';
        
        tableHtml += '<h4>Daftar Barang Untuk Kulakan (Stok Menipis)</h4>';
        <% if (toko != null) { %>
            tableHtml += '<p>Toko: <b><%=toko.getNama()%></b></p>';
        <% } %>

        tableHtml += '<table border="1"><thead><tr>';
        tableHtml += '<th>No</th>';
        if (!isLoginPedagangStok<%=rnd%>) {
            tableHtml += '<th>Nama Pedagang</th>';
        }
        tableHtml += '<th>Nama Produk</th>';
        tableHtml += '<th>Sisa Stok Saat Ini</th>';
        tableHtml += '<th>Rencana Jumlah Beli (Ceklis)</th>'; // Kolom kosong tambahan untuk diisi manual setelah dicetak
        tableHtml += '</tr></thead><tbody>';

        let no = 1;
        let currentPedagang = '';

        rawDataStok<%=rnd%>.forEach((row) => {
            if (!isLoginPedagangStok<%=rnd%> && currentPedagang !== row.nama_pedagang) {
                currentPedagang = row.nama_pedagang;
                no = 1;
            }

            tableHtml += '<tr>';
            tableHtml += '<td style="text-align: center;">' + no++ + '</td>';
            if (!isLoginPedagangStok<%=rnd%>) {
                tableHtml += '<td>' + (row.nama_pedagang || '-') + '</td>';
            }
            tableHtml += '<td>' + (row.nama_produk || '-') + '</td>';
            tableHtml += '<td class="num-format">' + parseFloat(row.sisa_stok || 0) + '</td>';
            tableHtml += '<td></td>'; // Kolom kosong
            tableHtml += '</tr>';
        });

        tableHtml += '</tbody></table></body></html>';

        const blob = new Blob([tableHtml], { type: 'application/vnd.ms-excel' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        
        const timestamp = new Date().toISOString().slice(0, 10).replace(/-/g, "");
        const fileName = isLoginPedagangStok<%=rnd%> ? "Daftar_Kulakan_Toko_<%=toko != null ? toko.getId() : ""%>_" : "Daftar_Kulakan_Semua_";
        link.download = fileName + timestamp + ".xls";
        link.href = url;
        
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        setTimeout(() => URL.revokeObjectURL(url), 100);
    };

    document.addEventListener("DOMContentLoaded", () => {
        // Listener Enter untuk Filter
        const filterInputs = [
            document.getElementById('searchProdukStok<%=rnd%>'),
            document.getElementById('searchPedagangStok<%=rnd%>'),
            document.getElementById('searchStokMin<%=rnd%>'),
            document.getElementById('searchStokMax<%=rnd%>')
        ];
        
        filterInputs.forEach(input => {
            if(input) {
                input.addEventListener("keydown", (event) => {
                    if (event.key === "Enter") {
                        event.preventDefault();
                        loadStokMenipis<%=rnd%>();
                    }
                });
            }
        });

        loadStokMenipis<%=rnd%>();
    });
</script>