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
        <div class="card border-0 shadow-sm rounded-4 border-top border-success border-4">
            
            <div class="card-header bg-white border-0 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-3 border-bottom border-light">
                <div>
                    <h5 class="fw-bold text-dark mb-1">
                        <i class="fas fa-handshake text-success me-2"></i>
                        <% if (toko != null) { %>
                            <%=Common.getBahasaConfig("Rekap Pelanggan Terloyal - ")%> <span class="text-success"><%=toko.getNama()%></span>
                        <% } else { %>
                            <%=Common.getBahasaConfig("Rekap Pelanggan Terloyal Keseluruhan")%>
                        <% } %>
                    </h5>
                    <small class="text-muted"><%=Common.getBahasaConfig("Daftar pelanggan setia dengan jumlah frekuensi dan nominal transaksi terbanyak.")%></small>
                </div>
                
                <div class="d-flex align-items-center gap-2 flex-wrap">
                    <select class="form-select form-select-sm rounded-3 shadow-sm bg-light" id="filterPeriodePelanggan<%=rnd%>" style="width: 160px; cursor: pointer;">
                        <option value="harian"><%=Common.getBahasaConfig("Harian (Hari Ini)")%></option>
                        <option value="mingguan"><%=Common.getBahasaConfig("Mingguan (7 Hari)")%></option>
                        <option value="bulanan" selected><%=Common.getBahasaConfig("Bulanan (30 Hari)")%></option>
                        <option value="semester"><%=Common.getBahasaConfig("6 Bulanan")%></option>
                        <option value="tahunan"><%=Common.getBahasaConfig("1 Tahunan")%></option>
                    </select>

                    <% if (toko == null) { %>
                    <div class="input-group input-group-sm shadow-sm" style="width: 180px;">
                        <span class="input-group-text bg-light border-end-0">
                            <i class="fas fa-store text-muted"></i>
                        </span>
                        <input type="text" class="form-control border-start-0 ps-0 bg-light" id="searchPedagangPelanggan<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Pedagang...")%>">
                    </div>
                    <% } else { %>
                        <input type="hidden" id="searchPedagangPelanggan<%=rnd%>" value="">
                    <% } %>

                    <div class="input-group input-group-sm shadow-sm" style="width: 180px;">
                        <span class="input-group-text bg-light border-end-0">
                            <i class="fas fa-user text-muted"></i>
                        </span>
                        <input type="text" class="form-control border-start-0 ps-0 bg-light" id="searchNamaPelanggan<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Pelanggan...")%>">
                    </div>

                    <button class="btn btn-sm btn-outline-primary rounded-pill px-3 fw-semibold shadow-sm text-nowrap" onclick="loadPelangganTerloyal<%=rnd%>()" id="btnRefreshPelanggan<%=rnd%>">
                        <i class="fas fa-search me-1"></i><%=Common.getBahasaConfig("Terapkan")%>
                    </button>
                    <button class="btn btn-sm btn-success rounded-pill px-3 fw-semibold shadow-sm text-nowrap" onclick="downloadPelangganExcel<%=rnd%>()" id="btnExcelPelanggan<%=rnd%>" disabled>
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
                                <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-user-check me-1"></i><%=Common.getBahasaConfig("Nama Pelanggan")%></th>
                                <th class="fw-semibold text-uppercase small px-3"><i class="fas fa-shopping-cart me-1"></i><%=Common.getBahasaConfig("Frekuensi Belanja")%></th>
                                <th class="text-end fw-semibold text-uppercase small px-3"><i class="fas fa-coins me-1"></i><%=Common.getBahasaConfig("Total Belanja (Rp)")%></th>
                            </tr>
                        </thead>
                        <tbody id="tabelPelangganTerloyal<%=rnd%>">
                            <tr>
                                <td colspan="<%= toko == null ? "5" : "4" %>" class="text-center py-5 text-muted">
                                    <div class="spinner-border text-success mb-3" role="status"></div><br>
                                    <span class="fw-medium"><%=Common.getBahasaConfig("Memuat data pelanggan...")%></span>
                                </td>
                            </tr>
                        </tbody>
                        <tfoot class="table-dark fw-bold" id="footerPelangganTerloyal<%=rnd%>" style="display: none;">
                            <tr>
                                <td colspan="<%= toko == null ? "3" : "2" %>" class="text-end px-3 text-uppercase"><%=Common.getBahasaConfig("Grand Total")%></td>
                                <td class="text-center bg-light text-primary" id="totFrekuensiPelanggan<%=rnd%>">0</td>
                                <td class="text-end bg-light text-success" id="totRpPelanggan<%=rnd%>">0</td>
                            </tr>
                        </tfoot>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    const langPelanggan<%=rnd%> = {
        emptyData: '<%=Common.getBahasaConfigJS("Tidak ada data pelanggan pada periode ini.")%>',
        errorFetch: '<%=Common.getBahasaConfigJS("Gagal mengambil data dari peladen. Silakan coba lagi.")%>',
        loading: '<%=Common.getBahasaConfigJS("Memuat...")%>',
        downloading: '<%=Common.getBahasaConfigJS("Mengunduh...")%>',
        subTotal: '<%=Common.getBahasaConfigJS("Sub-Total")%>'
    };

    const isLoginPedagangPelanggan<%=rnd%> = <%= toko != null %>;
    const filterTokoSQLPelanggan<%=rnd%> = "<%=sqlFilterToko%>";
    const baseColSpanPelanggan<%=rnd%> = isLoginPedagangPelanggan<%=rnd%> ? 4 : 5;

    const formatUang<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);
    };
    const formatAngkaBiasa<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID').format(angka || 0);
    };

    let rawDataPelanggan<%=rnd%> = [];

    const loadPelangganTerloyal<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelPelangganTerloyal<%=rnd%>');
        const tfoot = document.getElementById('footerPelangganTerloyal<%=rnd%>');
        const btnRefresh = document.getElementById('btnRefreshPelanggan<%=rnd%>');
        const btnExcel = document.getElementById('btnExcelPelanggan<%=rnd%>');
        
        const periode = document.getElementById('filterPeriodePelanggan<%=rnd%>').value;
        const searchInputPelanggan = document.getElementById('searchNamaPelanggan<%=rnd%>').value.trim();
        
        let searchInputPedagang = "";
        const elSearchPedagang = document.getElementById('searchPedagangPelanggan<%=rnd%>');
        if (elSearchPedagang) {
             searchInputPedagang = elSearchPedagang.value.trim();
        }
        
        tbody.innerHTML = '<tr><td colspan="' + baseColSpanPelanggan<%=rnd%> + '" class="text-center py-5 text-muted"><div class="spinner-border text-success mb-3" role="status"></div><br><span class="fw-medium">' + langPelanggan<%=rnd%>.loading + '</span></td></tr>';
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
        if (searchInputPedagang !== "" && !isLoginPedagangPelanggan<%=rnd%>) {
            const safeSearchPedagang = searchInputPedagang.replace(/'/g, "''");
            filterPencarianSQL += " AND t.nama ILIKE '%" + safeSearchPedagang + "%' ";
        }
        if (searchInputPelanggan !== "") {
            const safeSearchPelanggan = searchInputPelanggan.replace(/'/g, "''");
            filterPencarianSQL += " AND ak.nama ILIKE '%" + safeSearchPelanggan + "%' ";
        }

        // Terapkan filterTokoSQLPelanggan
        const sqlQuery = "SELECT " +
            "t.nama AS nama_pedagang, " +
            "ak.nama AS nama_pelanggan, " +
            "COUNT(a.id) AS frekuensi_belanja, " +
            "SUM(a.total) AS total_rp " +
            "FROM (SELECT * FROM (SELECT * FROM koperasi.pembelian WHERE COALESCE(aktif,true)=true) a WHERE COALESCE(aktif,true)=true) a " +
            "INNER JOIN koperasi.toko t ON a.toko = t.id " +
            "INNER JOIN koperasi.anggota_koperasi ak ON a.anggota_koperasi = ak.id " +
            "WHERE " + filterWaktuSQL + filterPencarianSQL + filterTokoSQLPelanggan<%=rnd%> +
            " GROUP BY t.id, t.nama, ak.id, ak.nama " +
            "ORDER BY t.nama ASC, total_rp DESC;";

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sql: sqlQuery, action: "sql" })
            });
            
            const result = await response.json();
            rawDataPelanggan<%=rnd%> = result.data || [];
            
            let htmlTbody = '';
            let grandTotalFrekuensi = 0;
            let grandTotalRp = 0;

            if (rawDataPelanggan<%=rnd%>.length === 0) {
                htmlTbody = '<tr><td colspan="' + baseColSpanPelanggan<%=rnd%> + '" class="text-center text-muted py-5"><i class="fas fa-users-slash fa-3x mb-3 text-secondary opacity-50 d-block"></i><span class="fw-medium">' + langPelanggan<%=rnd%>.emptyData + '</span></td></tr>';
            } else {
                let currentPedagang = '';
                let subTotalFrekuensi = 0;
                let subTotalRp = 0;
                let no = 1;

                rawDataPelanggan<%=rnd%>.forEach((row, index) => {
                    const frekuensi = parseInt(row.frekuensi_belanja || 0);
                    const rp = parseFloat(row.total_rp || 0);
                    
                    grandTotalFrekuensi += frekuensi;
                    grandTotalRp += rp;

                    if (!isLoginPedagangPelanggan<%=rnd%> && currentPedagang !== row.nama_pedagang && currentPedagang !== '') {
                        htmlTbody += 
                            '<tr class="bg-secondary bg-opacity-10 fw-bold border-bottom border-dark">' +
                                '<td colspan="3" class="text-end px-3 text-dark">' + langPelanggan<%=rnd%>.subTotal + ' ' + currentPedagang + '</td>' +
                                '<td class="text-center text-primary">' + formatAngkaBiasa<%=rnd%>(subTotalFrekuensi) + '</td>' +
                                '<td class="text-end text-success px-3">' + formatUang<%=rnd%>(subTotalRp) + '</td>' +
                            '</tr>';
                        
                        subTotalFrekuensi = 0;
                        subTotalRp = 0;
                        no = 1;
                    }

                    if (currentPedagang !== row.nama_pedagang) {
                        currentPedagang = row.nama_pedagang;
                    }

                    subTotalFrekuensi += frekuensi;
                    subTotalRp += rp;

                    // Hilangkan efek pudar jika yang login pedagang 
                    const bgRow = (!isLoginPedagangPelanggan<%=rnd%> && currentPedagang === row.nama_pedagang && no === 1) ? 'bg-light fw-bold text-dark' : '';
                    const namaPedagangTampil = (!isLoginPedagangPelanggan<%=rnd%> && no > 1) ? '<span class="text-muted opacity-50">' + row.nama_pedagang + '</span>' : row.nama_pedagang;

                    htmlTbody += '<tr class="' + bgRow + '">';
                    htmlTbody += '<td class="text-center text-muted border-end-0">' + no++ + '</td>';
                    
                    if (!isLoginPedagangPelanggan<%=rnd%>) {
                         htmlTbody += '<td class="text-start px-3 border-start-0 border-end-0">' + namaPedagangTampil + '</td>';
                    }
                    
                    htmlTbody += '<td class="text-start px-3 fw-medium text-dark border-start-0 border-end-0">' + (row.nama_pelanggan || '-') + '</td>';
                    htmlTbody += '<td class="text-center border-start-0 border-end-0"><span class="badge bg-primary rounded-pill shadow-sm px-2">' + formatAngkaBiasa<%=rnd%>(frekuensi) + 'x</span></td>';
                    htmlTbody += '<td class="text-end px-3 border-start-0 fw-semibold">' + formatUang<%=rnd%>(rp) + '</td>';
                    htmlTbody += '</tr>';

                    if (!isLoginPedagangPelanggan<%=rnd%> && index === rawDataPelanggan<%=rnd%>.length - 1) {
                        htmlTbody += 
                            '<tr class="bg-secondary bg-opacity-10 fw-bold border-bottom border-dark">' +
                                '<td colspan="3" class="text-end px-3 text-dark">' + langPelanggan<%=rnd%>.subTotal + ' ' + currentPedagang + '</td>' +
                                '<td class="text-center text-primary">' + formatAngkaBiasa<%=rnd%>(subTotalFrekuensi) + '</td>' +
                                '<td class="text-end text-success px-3">' + formatUang<%=rnd%>(subTotalRp) + '</td>' +
                            '</tr>';
                    }
                });

                document.getElementById('totFrekuensiPelanggan<%=rnd%>').innerText = formatAngkaBiasa<%=rnd%>(grandTotalFrekuensi);
                document.getElementById('totRpPelanggan<%=rnd%>').innerText = formatUang<%=rnd%>(grandTotalRp);
                
                tfoot.style.display = 'table-footer-group';
                btnExcel.disabled = false;
            }
            
            tbody.innerHTML = htmlTbody;

        } catch (error) {
            console.error("Gagal mengambil data pelanggan:", error);
            tbody.innerHTML = '<tr><td colspan="' + baseColSpanPelanggan<%=rnd%> + '" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i>' + langPelanggan<%=rnd%>.errorFetch + '</td></tr>';
        } finally {
            btnRefresh.disabled = false;
        }
    };

    const downloadPelangganExcel<%=rnd%> = () => {
        if (!rawDataPelanggan<%=rnd%> || rawDataPelanggan<%=rnd%>.length === 0) return;

        const periodeTeks = document.getElementById('filterPeriodePelanggan<%=rnd%>').options[document.getElementById('filterPeriodePelanggan<%=rnd%>').selectedIndex].text;

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
        
        tableHtml += '<h4>Laporan Pelanggan Terloyal</h4>';
        tableHtml += '<p>Periode: <b>' + periodeTeks + '</b></p>';
        <% if (toko != null) { %>
            tableHtml += '<p>Pedagang: <b><%=toko.getNama()%></b></p>';
        <% } %>

        tableHtml += '<table border="1">';
        tableHtml += '<thead>';
        tableHtml += '<tr>';
        tableHtml += '<th>No</th>';
        if (!isLoginPedagangPelanggan<%=rnd%>) {
            tableHtml += '<th>Nama Pedagang</th>';
        }
        tableHtml += '<th>Nama Pelanggan</th>';
        tableHtml += '<th>Frekuensi Belanja (Kali)</th>';
        tableHtml += '<th>Total Belanja (Rp)</th>';
        tableHtml += '</tr>';
        tableHtml += '</thead><tbody>';

        let grandTotalFrekuensi = 0;
        let grandTotalRp = 0;
        let subTotalFrekuensi = 0;
        let subTotalRp = 0;
        let currentPedagang = '';
        let no = 1;

        rawDataPelanggan<%=rnd%>.forEach((row, index) => {
            const frekuensi = parseInt(row.frekuensi_belanja || 0);
            const rp = parseFloat(row.total_rp || 0);
            
            grandTotalFrekuensi += frekuensi;
            grandTotalRp += rp;

            if (!isLoginPedagangPelanggan<%=rnd%> && currentPedagang !== row.nama_pedagang && currentPedagang !== '') {
                tableHtml += '<tr class="sub-total">';
                tableHtml += '<td colspan="3" style="text-align: right;">' + langPelanggan<%=rnd%>.subTotal + ' ' + currentPedagang + '</td>';
                tableHtml += '<td class="num-format">' + subTotalFrekuensi + '</td>';
                tableHtml += '<td class="rp-format">' + subTotalRp + '</td>';
                tableHtml += '</tr>';
                
                subTotalFrekuensi = 0;
                subTotalRp = 0;
                no = 1;
            }

            if (currentPedagang !== row.nama_pedagang) {
                currentPedagang = row.nama_pedagang;
            }

            subTotalFrekuensi += frekuensi;
            subTotalRp += rp;

            tableHtml += '<tr>';
            tableHtml += '<td style="text-align: center;">' + no++ + '</td>';
            if (!isLoginPedagangPelanggan<%=rnd%>) {
                tableHtml += '<td>' + (row.nama_pedagang || '-') + '</td>';
            }
            tableHtml += '<td>' + (row.nama_pelanggan || '-') + '</td>';
            tableHtml += '<td class="num-format">' + frekuensi + '</td>';
            tableHtml += '<td class="rp-format">' + rp + '</td>';
            tableHtml += '</tr>';

            if (!isLoginPedagangPelanggan<%=rnd%> && index === rawDataPelanggan<%=rnd%>.length - 1) {
                tableHtml += '<tr class="sub-total">';
                tableHtml += '<td colspan="3" style="text-align: right;">' + langPelanggan<%=rnd%>.subTotal + ' ' + currentPedagang + '</td>';
                tableHtml += '<td class="num-format">' + subTotalFrekuensi + '</td>';
                tableHtml += '<td class="rp-format">' + subTotalRp + '</td>';
                tableHtml += '</tr>';
            }
        });

        tableHtml += '</tbody>';
        tableHtml += '<tfoot style="font-weight: bold; background-color: #343a40; color: white;">';
        tableHtml += '<tr>';
        
        const footColSpan = isLoginPedagangPelanggan<%=rnd%> ? 2 : 3;
        tableHtml += '<td colspan="' + footColSpan + '" style="text-align: right;">Grand Total Keseluruhan</td>';
        
        tableHtml += '<td class="num-format">' + grandTotalFrekuensi + '</td>';
        tableHtml += '<td class="rp-format">' + grandTotalRp + '</td>';
        tableHtml += '</tr>';
        tableHtml += '</tfoot></table></body></html>';

        const blob = new Blob([tableHtml], { type: 'application/vnd.ms-excel' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        
        const timestamp = new Date().toISOString().slice(0, 10).replace(/-/g, "");
        const fileName = isLoginPedagangPelanggan<%=rnd%> ? "Pelanggan_Toko_<%=toko != null ? toko.getId() : ""%>_" : "Pelanggan_Semua_";
        link.download = fileName + timestamp + ".xls";
        link.href = url;
        
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        setTimeout(() => URL.revokeObjectURL(url), 100);
    };

    document.addEventListener("DOMContentLoaded", () => {
        const inputSearchPelanggan = document.getElementById('searchNamaPelanggan<%=rnd%>');
        const filterDropdown = document.getElementById('filterPeriodePelanggan<%=rnd%>');
        
        if(inputSearchPelanggan) {
            inputSearchPelanggan.addEventListener("keydown", (event) => {
                if (event.key === "Enter") {
                    event.preventDefault();
                    loadPelangganTerloyal<%=rnd%>();
                }
            });
        }
        
        const inputSearchPedagang = document.getElementById('searchPedagangPelanggan<%=rnd%>');
        if(inputSearchPedagang) {
             inputSearchPedagang.addEventListener("keydown", (event) => {
                if (event.key === "Enter") {
                    event.preventDefault();
                    loadPelangganTerloyal<%=rnd%>();
                }
            });
        }

        filterDropdown.addEventListener("change", () => {
            loadPelangganTerloyal<%=rnd%>();
        });

        loadPelangganTerloyal<%=rnd%>();
    });
</script>