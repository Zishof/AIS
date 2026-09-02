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
    sqlFilterToko = " AND a.toko = " + toko.getId() + " ";
}
%>

<div class="row mb-4 animate__animated animate__fadeInUp">
    <div class="col-12">
        <div class="card border-0 shadow-sm rounded-4 border-top border-danger border-4">
            
            <div class="card-header bg-white border-0 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-3 border-bottom border-light">
                <div>
                    <h5 class="fw-bold text-dark mb-1">
                        <i class="fas fa-fire text-danger me-2"></i>
                        <% if (toko != null) { %>
                            <%=Common.getBahasaConfig("Rekap Produk Terlaris - ")%> <span class="text-danger"><%=toko.getNama()%></span>
                        <% } else { %>
                            <%=Common.getBahasaConfig("Rekap Produk Terlaris Keseluruhan")%>
                        <% } %>
                    </h5>
                    <small class="text-muted"><%=Common.getBahasaConfig("Lihat daftar produk dengan penjualan terbaik berdasarkan periode tertentu.")%></small>
                </div>
                
                <div class="d-flex align-items-center gap-2 flex-wrap">
                    <select class="form-select form-select-sm rounded-3 shadow-sm bg-light" id="filterPeriode<%=rnd%>" style="width: 160px; cursor: pointer;">
                        <option value="harian"><%=Common.getBahasaConfig("Harian (Hari Ini)")%></option>
                        <option value="mingguan"><%=Common.getBahasaConfig("Mingguan (7 Hari)")%></option>
                        <option value="bulanan" selected><%=Common.getBahasaConfig("Bulanan (30 Hari)")%></option>
                        <option value="semester"><%=Common.getBahasaConfig("6 Bulanan")%></option>
                        <option value="tahunan"><%=Common.getBahasaConfig("1 Tahunan")%></option>
                    </select>

                    <% if (toko == null) { %>
                    <div class="input-group input-group-sm shadow-sm" style="width: 180px;">
                        <span class="input-group-text bg-light border-end-0" id="search-addon-produk<%=rnd%>">
                            <i class="fas fa-store text-muted"></i>
                        </span>
                        <input type="text" class="form-control border-start-0 ps-0 bg-light" id="searchPedagangProduk<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Pedagang...")%>">
                    </div>
                    <% } else { %>
                        <input type="hidden" id="searchPedagangProduk<%=rnd%>" value="">
                    <% } %>

                    <div class="input-group input-group-sm shadow-sm" style="width: 180px;">
                        <span class="input-group-text bg-light border-end-0">
                            <i class="fas fa-box text-muted"></i>
                        </span>
                        <input type="text" class="form-control border-start-0 ps-0 bg-light" id="searchNamaProduk<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Produk...")%>">
                    </div>

                    <button class="btn btn-sm btn-outline-primary rounded-pill px-3 fw-semibold shadow-sm text-nowrap" onclick="loadProdukTerlaris<%=rnd%>()" id="btnRefreshProduk<%=rnd%>">
                        <i class="fas fa-search me-1"></i><%=Common.getBahasaConfig("Terapkan")%>
                    </button>
                    <button class="btn btn-sm btn-success rounded-pill px-3 fw-semibold shadow-sm text-nowrap" onclick="downloadProdukExcel<%=rnd%>()" id="btnExcelProduk<%=rnd%>" disabled>
                        <i class="fas fa-file-excel me-1"></i><%=Common.getBahasaConfig("Unduh")%>
                    </button>
                </div>
            </div>
            
            <div class="card-body p-4 pt-0">
                <div class="table-responsive border rounded-3 shadow-sm mt-3">
                    <table class="table table-hover table-bordered align-middle mb-0">
                        <thead class="table-dark align-middle text-center">
                            <tr>
                                <th class="fw-semibold text-uppercase small px-3" style="width: 50px;">#</th>
                                <% if (toko == null) { %>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-store me-1"></i><%=Common.getBahasaConfig("Nama Pedagang")%></th>
                                <% } %>
                                <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-box-open me-1"></i><%=Common.getBahasaConfig("Nama Produk")%></th>
                                <th class="fw-semibold text-uppercase small px-3"><i class="fas fa-layer-group me-1"></i><%=Common.getBahasaConfig("Total Terjual (Qty)")%></th>
                                <th class="text-end fw-semibold text-uppercase small px-3"><i class="fas fa-coins me-1"></i><%=Common.getBahasaConfig("Total Pendapatan (Rp)")%></th>
                            </tr>
                        </thead>
                        <tbody id="tabelProdukTerlaris<%=rnd%>">
                            <tr>
                                <td colspan="<%= toko == null ? "5" : "4" %>" class="text-center py-5 text-muted">
                                    <div class="spinner-border text-danger mb-3" role="status"></div><br>
                                    <span class="fw-medium"><%=Common.getBahasaConfig("Memuat data produk terlaris...")%></span>
                                </td>
                            </tr>
                        </tbody>
                        <tfoot class="table-dark fw-bold" id="footerProdukTerlaris<%=rnd%>" style="display: none;">
                            <tr>
                                <td colspan="<%= toko == null ? "3" : "2" %>" class="text-end px-3 text-uppercase"><%=Common.getBahasaConfig("Grand Total")%></td>
                                <td class="text-center bg-light text-primary" id="totQtyProduk<%=rnd%>">0</td>
                                <td class="text-end bg-light text-success" id="totRpProduk<%=rnd%>">0</td>
                            </tr>
                        </tfoot>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    const langProduk<%=rnd%> = {
        emptyData: '<%=Common.getBahasaConfigJS("Tidak ada data penjualan pada periode ini.")%>',
        errorFetch: '<%=Common.getBahasaConfigJS("Gagal mengambil data dari peladen. Silakan coba lagi.")%>',
        loading: '<%=Common.getBahasaConfigJS("Memuat...")%>',
        downloading: '<%=Common.getBahasaConfigJS("Mengunduh...")%>',
        subTotal: '<%=Common.getBahasaConfigJS("Sub-Total")%>'
    };

    const isLoginPedagangProduk<%=rnd%> = <%= toko != null %>;
    const filterTokoSQLProduk<%=rnd%> = "<%=sqlFilterToko%>";
    const baseColSpanProduk<%=rnd%> = isLoginPedagangProduk<%=rnd%> ? 4 : 5;

    const formatUang<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);
    };
    const formatAngkaBiasa<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID').format(angka || 0);
    };

    let rawDataProduk<%=rnd%> = [];

    const loadProdukTerlaris<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelProdukTerlaris<%=rnd%>');
        const tfoot = document.getElementById('footerProdukTerlaris<%=rnd%>');
        const btnRefresh = document.getElementById('btnRefreshProduk<%=rnd%>');
        const btnExcel = document.getElementById('btnExcelProduk<%=rnd%>');
        
        const periode = document.getElementById('filterPeriode<%=rnd%>').value;
        const searchInputProduk = document.getElementById('searchNamaProduk<%=rnd%>').value.trim();
        
        let searchInputPedagang = "";
        const elSearchPedagang = document.getElementById('searchPedagangProduk<%=rnd%>');
        if (elSearchPedagang) {
            searchInputPedagang = elSearchPedagang.value.trim();
        }
        
        tbody.innerHTML = '<tr><td colspan="' + baseColSpanProduk<%=rnd%> + '" class="text-center py-5 text-muted"><div class="spinner-border text-danger mb-3" role="status"></div><br><span class="fw-medium">' + langProduk<%=rnd%>.loading + '</span></td></tr>';
        tfoot.style.display = 'none';
        btnRefresh.disabled = true;
        btnExcel.disabled = true;

        let filterWaktuSQL = "";
        if (periode === 'harian') {
            filterWaktuSQL = "DATE(a.waktu) = CURRENT_DATE";
        } else if (periode === 'mingguan') {
            filterWaktuSQL = "DATE(a.waktu) >= CURRENT_DATE - INTERVAL '7 days'";
        } else if (periode === 'bulanan') {
            filterWaktuSQL = "DATE(a.waktu) >= CURRENT_DATE - INTERVAL '1 month'";
        } else if (periode === 'semester') {
            filterWaktuSQL = "DATE(a.waktu) >= CURRENT_DATE - INTERVAL '6 months'";
        } else if (periode === 'tahunan') {
            filterWaktuSQL = "DATE(a.waktu) >= CURRENT_DATE - INTERVAL '1 year'";
        }

        let filterPencarianSQL = "";
        if (searchInputPedagang !== "" && !isLoginPedagangProduk<%=rnd%>) {
            const safeSearchPedagang = searchInputPedagang.replace(/'/g, "''");
            filterPencarianSQL += " AND b.nama ILIKE '%" + safeSearchPedagang + "%' ";
        }
        if (searchInputProduk !== "") {
            const safeSearchProduk = searchInputProduk.replace(/'/g, "''");
            filterPencarianSQL += " AND c.nama ILIKE '%" + safeSearchProduk + "%' ";
        }

        // Terapkan filterTokoSQLProduk pada klausa WHERE
        const sqlQuery = "SELECT " +
            "b.nama AS nama_pedagang, " +
            "c.nama AS nama_produk, " +
            "SUM(a.qty) AS total_qty, " +
            "SUM(a.total) AS total_rp " +
            "FROM (SELECT * FROM (SELECT * FROM koperasi.pembelian WHERE COALESCE(aktif,true)=true) a WHERE COALESCE(aktif,true)=true) a " +
            "INNER JOIN koperasi.toko b ON a.toko = b.id " +
            "INNER JOIN koperasi.produk c ON a.produk = c.id " +
            "WHERE " + filterWaktuSQL + filterPencarianSQL + filterTokoSQLProduk<%=rnd%> +
            " GROUP BY b.id, b.nama, c.id, c.nama " +
            "ORDER BY b.nama ASC, total_qty DESC;";

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sql: sqlQuery, action: "sql" })
            });
            
            const result = await response.json();
            rawDataProduk<%=rnd%> = result.data || [];
            
            let htmlTbody = '';
            let grandTotalQty = 0;
            let grandTotalRp = 0;

            if (rawDataProduk<%=rnd%>.length === 0) {
                htmlTbody = '<tr><td colspan="' + baseColSpanProduk<%=rnd%> + '" class="text-center text-muted py-5"><i class="fas fa-box-open fa-3x mb-3 text-secondary opacity-50 d-block"></i><span class="fw-medium">' + langProduk<%=rnd%>.emptyData + '</span></td></tr>';
            } else {
                let currentPedagang = '';
                let subTotalQty = 0;
                let subTotalRp = 0;
                let no = 1;

                rawDataProduk<%=rnd%>.forEach((row, index) => {
                    const qty = parseInt(row.total_qty || 0);
                    const rp = parseFloat(row.total_rp || 0);
                    
                    grandTotalQty += qty;
                    grandTotalRp += rp;

                    // Logika Sub-Total (hanya jika yang login BUKAN pedagang)
                    if (!isLoginPedagangProduk<%=rnd%> && currentPedagang !== row.nama_pedagang && currentPedagang !== '') {
                        htmlTbody += 
                            '<tr class="bg-secondary bg-opacity-10 fw-bold border-bottom border-dark">' +
                                '<td colspan="3" class="text-end px-3 text-dark">' + langProduk<%=rnd%>.subTotal + ' ' + currentPedagang + '</td>' +
                                '<td class="text-center text-primary">' + formatAngkaBiasa<%=rnd%>(subTotalQty) + '</td>' +
                                '<td class="text-end text-success px-3">' + formatUang<%=rnd%>(subTotalRp) + '</td>' +
                            '</tr>';
                        
                        subTotalQty = 0;
                        subTotalRp = 0;
                        no = 1;
                    }

                    if (currentPedagang !== row.nama_pedagang) {
                        currentPedagang = row.nama_pedagang;
                    }

                    subTotalQty += qty;
                    subTotalRp += rp;

                    // Hilangkan efek pudar jika yang login pedagang (karena cuma 1 pedagang, aneh kalau baris ke 2 dst memudar namanya)
                    const bgRow = (!isLoginPedagangProduk<%=rnd%> && currentPedagang === row.nama_pedagang && no === 1) ? 'bg-light fw-bold text-dark' : '';
                    const namaPedagangTampil = (!isLoginPedagangProduk<%=rnd%> && no > 1) ? '<span class="text-muted opacity-50">' + row.nama_pedagang + '</span>' : row.nama_pedagang;

                    htmlTbody += '<tr class="' + bgRow + '">';
                    htmlTbody += '<td class="text-center text-muted border-end-0">' + no++ + '</td>';
                    
                    if (!isLoginPedagangProduk<%=rnd%>) {
                         htmlTbody += '<td class="text-start px-3 border-start-0 border-end-0">' + namaPedagangTampil + '</td>';
                    }
                    
                    htmlTbody += '<td class="text-start px-3 fw-medium text-dark border-start-0 border-end-0">' + (row.nama_produk || '-') + '</td>';
                    htmlTbody += '<td class="text-center border-start-0 border-end-0">' + formatAngkaBiasa<%=rnd%>(qty) + '</td>';
                    htmlTbody += '<td class="text-end px-3 border-start-0">' + formatUang<%=rnd%>(rp) + '</td>';
                    htmlTbody += '</tr>';

                    // Sub-total baris terakhir (hanya jika admin)
                    if (!isLoginPedagangProduk<%=rnd%> && index === rawDataProduk<%=rnd%>.length - 1) {
                        htmlTbody += 
                            '<tr class="bg-secondary bg-opacity-10 fw-bold border-bottom border-dark">' +
                                '<td colspan="3" class="text-end px-3 text-dark">' + langProduk<%=rnd%>.subTotal + ' ' + currentPedagang + '</td>' +
                                '<td class="text-center text-primary">' + formatAngkaBiasa<%=rnd%>(subTotalQty) + '</td>' +
                                '<td class="text-end text-success px-3">' + formatUang<%=rnd%>(subTotalRp) + '</td>' +
                            '</tr>';
                    }
                });

                document.getElementById('totQtyProduk<%=rnd%>').innerText = formatAngkaBiasa<%=rnd%>(grandTotalQty);
                document.getElementById('totRpProduk<%=rnd%>').innerText = formatUang<%=rnd%>(grandTotalRp);
                
                tfoot.style.display = 'table-footer-group';
                btnExcel.disabled = false;
            }
            
            tbody.innerHTML = htmlTbody;

        } catch (error) {
            console.error("Gagal mengambil data produk:", error);
            tbody.innerHTML = '<tr><td colspan="' + baseColSpanProduk<%=rnd%> + '" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i>' + langProduk<%=rnd%>.errorFetch + '</td></tr>';
        } finally {
            btnRefresh.disabled = false;
        }
    };

    const downloadProdukExcel<%=rnd%> = () => {
        if (!rawDataProduk<%=rnd%> || rawDataProduk<%=rnd%>.length === 0) return;

        const periodeTeks = document.getElementById('filterPeriode<%=rnd%>').options[document.getElementById('filterPeriode<%=rnd%>').selectedIndex].text;

        let tableHtml = '<html xmlns:x="urn:schemas-microsoft-com:office:excel">';
        tableHtml += '<head>';
        tableHtml += '<meta charset="UTF-8">';
        tableHtml += '<style>';
        tableHtml += '.num-format { mso-number-format:"\\#\\,\\#\\#0"; text-align: center; }';
        tableHtml += '.rp-format { mso-number-format:"\\#\\,\\#\\#0"; text-align: right; }';
        tableHtml += '.sub-total { background-color: #d1ecf1; font-weight: bold; }';
        tableHtml += 'th { background-color: #f8f9fa; font-weight: bold; }';
        tableHtml += '</style>';
        tableHtml += '</head><body>';
        
        tableHtml += '<h4>Laporan Produk Terlaris</h4>';
        tableHtml += '<p>Periode: <b>' + periodeTeks + '</b></p>';
        <% if (toko != null) { %>
            tableHtml += '<p>Pedagang: <b><%=toko.getNama()%></b></p>';
        <% } %>

        tableHtml += '<table border="1">';
        tableHtml += '<thead>';
        tableHtml += '<tr>';
        tableHtml += '<th>No</th>';
        if (!isLoginPedagangProduk<%=rnd%>) {
            tableHtml += '<th>Nama Pedagang</th>';
        }
        tableHtml += '<th>Nama Produk</th>';
        tableHtml += '<th>Total Terjual (Qty)</th>';
        tableHtml += '<th>Total Pendapatan (Rp)</th>';
        tableHtml += '</tr>';
        tableHtml += '</thead><tbody>';

        let grandTotalQty = 0;
        let grandTotalRp = 0;
        let subTotalQty = 0;
        let subTotalRp = 0;
        let currentPedagang = '';
        let no = 1;

        rawDataProduk<%=rnd%>.forEach((row, index) => {
            const qty = parseInt(row.total_qty || 0);
            const rp = parseFloat(row.total_rp || 0);
            
            grandTotalQty += qty;
            grandTotalRp += rp;

            if (!isLoginPedagangProduk<%=rnd%> && currentPedagang !== row.nama_pedagang && currentPedagang !== '') {
                tableHtml += '<tr class="sub-total">';
                tableHtml += '<td colspan="3" style="text-align: right;">' + langProduk<%=rnd%>.subTotal + ' ' + currentPedagang + '</td>';
                tableHtml += '<td class="num-format">' + subTotalQty + '</td>';
                tableHtml += '<td class="rp-format">' + subTotalRp + '</td>';
                tableHtml += '</tr>';
                
                subTotalQty = 0;
                subTotalRp = 0;
                no = 1;
            }

            if (currentPedagang !== row.nama_pedagang) {
                currentPedagang = row.nama_pedagang;
            }

            subTotalQty += qty;
            subTotalRp += rp;

            tableHtml += '<tr>';
            tableHtml += '<td style="text-align: center;">' + no++ + '</td>';
            if (!isLoginPedagangProduk<%=rnd%>) {
                tableHtml += '<td>' + (row.nama_pedagang || '-') + '</td>';
            }
            tableHtml += '<td>' + (row.nama_produk || '-') + '</td>';
            tableHtml += '<td class="num-format">' + qty + '</td>';
            tableHtml += '<td class="rp-format">' + rp + '</td>';
            tableHtml += '</tr>';

            if (!isLoginPedagangProduk<%=rnd%> && index === rawDataProduk<%=rnd%>.length - 1) {
                tableHtml += '<tr class="sub-total">';
                tableHtml += '<td colspan="3" style="text-align: right;">' + langProduk<%=rnd%>.subTotal + ' ' + currentPedagang + '</td>';
                tableHtml += '<td class="num-format">' + subTotalQty + '</td>';
                tableHtml += '<td class="rp-format">' + subTotalRp + '</td>';
                tableHtml += '</tr>';
            }
        });

        tableHtml += '</tbody>';
        tableHtml += '<tfoot style="font-weight: bold; background-color: #343a40; color: white;">';
        tableHtml += '<tr>';
        
        const footColSpan = isLoginPedagangProduk<%=rnd%> ? 2 : 3;
        tableHtml += '<td colspan="' + footColSpan + '" style="text-align: right;">Grand Total Keseluruhan</td>';
        
        tableHtml += '<td class="num-format">' + grandTotalQty + '</td>';
        tableHtml += '<td class="rp-format">' + grandTotalRp + '</td>';
        tableHtml += '</tr>';
        tableHtml += '</tfoot></table></body></html>';

        const blob = new Blob([tableHtml], { type: 'application/vnd.ms-excel' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        
        const timestamp = new Date().toISOString().slice(0, 10).replace(/-/g, "");
        const fileName = isLoginPedagangProduk<%=rnd%> ? "Produk_Toko_<%=toko != null ? toko.getId() : ""%>_" : "Produk_Semua_";
        link.download = fileName + timestamp + ".xls";
        link.href = url;
        
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        setTimeout(() => URL.revokeObjectURL(url), 100);
    };

    document.addEventListener("DOMContentLoaded", () => {
        const inputSearchProduk = document.getElementById('searchNamaProduk<%=rnd%>');
        const filterDropdown = document.getElementById('filterPeriode<%=rnd%>');
        
        if(inputSearchProduk) {
            inputSearchProduk.addEventListener("keydown", (event) => {
                if (event.key === "Enter") {
                    event.preventDefault();
                    loadProdukTerlaris<%=rnd%>();
                }
            });
        }
        
        const inputSearchPedagang = document.getElementById('searchPedagangProduk<%=rnd%>');
        if(inputSearchPedagang) {
             inputSearchPedagang.addEventListener("keydown", (event) => {
                if (event.key === "Enter") {
                    event.preventDefault();
                    loadProdukTerlaris<%=rnd%>();
                }
            });
        }

        filterDropdown.addEventListener("change", () => {
            loadProdukTerlaris<%=rnd%>();
        });

        loadProdukTerlaris<%=rnd%>();
    });
</script>