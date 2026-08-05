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
        <div class="card border-0 shadow-sm rounded-4 border-top border-warning border-4">
            
            <div class="card-header bg-white border-0 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-3 border-bottom border-light">
                <div>
                    <h5 class="fw-bold text-dark mb-1">
                        <i class="fas fa-trophy text-warning me-2"></i>
                        <% if (toko != null) { %>
                            <%=Common.getBahasaConfig("Performa Penjualan - ")%> <span class="text-warning"><%=toko.getNama()%></span>
                        <% } else { %>
                            <%=Common.getBahasaConfig("Performa Pedagang Keseluruhan (Rekapitulasi)")%>
                        <% } %>
                    </h5>
                    <small class="text-muted"><%=Common.getBahasaConfig("Pantau perbandingan total kuantitas, pelanggan, dan pendapatan di berbagai periode.")%></small>
                </div>
                
                <div class="d-flex align-items-center gap-2">
                    <% if (toko == null) { %>
                    <div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;">
                        <span class="input-group-text bg-light border-end-0" id="search-addon<%=rnd%>">
                            <i class="fas fa-search text-muted"></i>
                        </span>
                        <input type="text" class="form-control border-start-0 ps-0 bg-light" id="searchPedagang<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Nama Pedagang...")%>" aria-label="Cari Pedagang">
                    </div>
                    <% } else { %>
                        <input type="hidden" id="searchPedagang<%=rnd%>" value="">
                    <% } %>

                    <button class="btn btn-sm btn-outline-primary rounded-pill px-3 fw-semibold shadow-sm text-nowrap" onclick="loadRekapPedagang<%=rnd%>()" id="btnRefreshRekap<%=rnd%>">
                        <i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Segarkan")%>
                    </button>
                    <button class="btn btn-sm btn-success rounded-pill px-3 fw-semibold shadow-sm text-nowrap" onclick="downloadRekapExcel<%=rnd%>()" id="btnExcelRekap<%=rnd%>" disabled>
                        <i class="fas fa-file-excel me-1"></i><%=Common.getBahasaConfig("Unduh Excel")%>
                    </button>
                </div>
            </div>
            
            <div class="card-body p-4 pt-0">
                <div class="table-responsive border rounded-3 shadow-sm mt-3">
                    <table class="table table-hover table-bordered align-middle mb-0 text-center" id="tabelRekapitulasiData<%=rnd%>">
                        <thead class="table-dark align-middle">
                            <tr>
                                <% if (toko == null) { %>
                                    <th rowspan="2" class="text-start fw-semibold text-uppercase small px-3" style="min-width: 180px;">
                                        <i class="fas fa-store me-1"></i><%=Common.getBahasaConfig("Nama Pedagang")%>
                                    </th>
                                <% } %>
                                <th colspan="3" class="fw-semibold text-uppercase small bg-primary bg-gradient border-bottom-0">
                                    <%=Common.getBahasaConfig("Harian (Hari Ini)")%>
                                </th>
                                <th colspan="3" class="fw-semibold text-uppercase small bg-success bg-gradient border-bottom-0">
                                    <%=Common.getBahasaConfig("Mingguan (7 Hari)")%>
                                </th>
                                <th colspan="3" class="fw-semibold text-uppercase small bg-warning text-dark bg-gradient border-bottom-0">
                                    <%=Common.getBahasaConfig("Bulanan (30 Hari)")%>
                                </th>
                            </tr>
                            <tr>
                                <th class="small bg-light text-dark border-top-0"><%=Common.getBahasaConfig("Qty")%></th>
                                <th class="small bg-light text-dark border-top-0"><%=Common.getBahasaConfig("Pembeli")%></th>
                                <th class="small bg-light text-dark border-top-0"><%=Common.getBahasaConfig("Total (Rp)")%></th>
                                
                                <th class="small bg-light text-dark border-top-0"><%=Common.getBahasaConfig("Qty")%></th>
                                <th class="small bg-light text-dark border-top-0"><%=Common.getBahasaConfig("Pembeli")%></th>
                                <th class="small bg-light text-dark border-top-0"><%=Common.getBahasaConfig("Total (Rp)")%></th>
                                
                                <th class="small bg-light text-dark border-top-0"><%=Common.getBahasaConfig("Qty")%></th>
                                <th class="small bg-light text-dark border-top-0"><%=Common.getBahasaConfig("Pembeli")%></th>
                                <th class="small bg-light text-dark border-top-0"><%=Common.getBahasaConfig("Total (Rp)")%></th>
                            </tr>
                        </thead>
                        <tbody id="tabelRekapPedagang<%=rnd%>">
                            <tr>
                                <td colspan="<%= toko == null ? "10" : "9" %>" class="text-center py-5 text-muted">
                                    <div class="spinner-border text-warning mb-3" role="status"></div><br>
                                    <span class="fw-medium"><%=Common.getBahasaConfig("Memuat data performa penjualan...")%></span>
                                </td>
                            </tr>
                        </tbody>
                        <tfoot class="table-dark fw-bold" id="tabelRekapFooter<%=rnd%>" style="display: none;">
                            <tr>
                                <% if (toko == null) { %>
                                    <td class="text-start px-3 text-uppercase"><%=Common.getBahasaConfig("Total Keseluruhan")%></td>
                                <% } %>
                                <td id="totHariQty<%=rnd%>">0</td>
                                <td id="totHariPembeli<%=rnd%>">0</td>
                                <td id="totHariRp<%=rnd%>" class="text-end text-primary bg-light">0</td>
                                <td id="totMingguQty<%=rnd%>">0</td>
                                <td id="totMingguPembeli<%=rnd%>">0</td>
                                <td id="totMingguRp<%=rnd%>" class="text-end text-success bg-light">0</td>
                                <td id="totBulanQty<%=rnd%>">0</td>
                                <td id="totBulanPembeli<%=rnd%>">0</td>
                                <td id="totBulanRp<%=rnd%>" class="text-end text-warning bg-light">0</td>
                            </tr>
                        </tfoot>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    const langRekap<%=rnd%> = {
        emptyData: '<%=Common.getBahasaConfigJS("Belum ada data transaksi atau pedagang tidak ditemukan.")%>',
        errorFetch: '<%=Common.getBahasaConfigJS("Gagal mengambil data dari peladen. Silakan coba lagi.")%>'
    };

    const isLoginPedagang<%=rnd%> = <%= toko != null %>;
    const filterTokoSQL<%=rnd%> = "<%=sqlFilterToko%>";
    const baseColSpan<%=rnd%> = isLoginPedagang<%=rnd%> ? 9 : 10;

    const formatUangRekap<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);
    };

    const formatAngkaBiasaRekap<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID').format(angka || 0);
    };

    let rawDataRekap<%=rnd%> = [];

    const loadRekapPedagang<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelRekapPedagang<%=rnd%>');
        const tfoot = document.getElementById('tabelRekapFooter<%=rnd%>');
        const btnRefresh = document.getElementById('btnRefreshRekap<%=rnd%>');
        const btnExcel = document.getElementById('btnExcelRekap<%=rnd%>');
        
        let searchInput = "";
        const elSearch = document.getElementById('searchPedagang<%=rnd%>');
        if (elSearch) {
            searchInput = elSearch.value.trim().replace(/'/g, "''");
        }
        
        tbody.innerHTML = '<tr><td colspan="' + baseColSpan<%=rnd%> + '" class="text-center py-5 text-muted"><div class="spinner-border text-warning mb-3" role="status"></div><br><span class="fw-medium"><%=Common.getBahasaConfig("Memuat data performa penjualan...")%></span></td></tr>';
        tfoot.style.display = 'none';
        btnRefresh.disabled = true;
        btnExcel.disabled = true;

        let filterNamaSQL = "";
        if (searchInput !== "" && !isLoginPedagang<%=rnd%>) {
            filterNamaSQL = " AND b.nama ILIKE '%" + searchInput + "%' ";
        }

        const sqlQuery = "SELECT " +
            "b.nama AS nama_pedagang, " +
            
            "COALESCE(SUM(CASE WHEN DATE(a.waktu) = CURRENT_DATE THEN a.qty ELSE 0 END), 0) AS harian_qty, " +
            "COUNT(DISTINCT CASE WHEN DATE(a.waktu) = CURRENT_DATE THEN a.anggota_koperasi END) AS harian_pembeli, " +
            "COALESCE(SUM(CASE WHEN DATE(a.waktu) = CURRENT_DATE THEN a.total ELSE 0 END), 0) AS harian_total, " +
            
            "COALESCE(SUM(CASE WHEN DATE(a.waktu) BETWEEN CURRENT_DATE - INTERVAL '7 days' AND CURRENT_DATE THEN a.qty ELSE 0 END), 0) AS mingguan_qty, " +
            "COUNT(DISTINCT CASE WHEN DATE(a.waktu) BETWEEN CURRENT_DATE - INTERVAL '7 days' AND CURRENT_DATE THEN a.anggota_koperasi END) AS mingguan_pembeli, " +
            "COALESCE(SUM(CASE WHEN DATE(a.waktu) BETWEEN CURRENT_DATE - INTERVAL '7 days' AND CURRENT_DATE THEN a.total ELSE 0 END), 0) AS mingguan_total, " +
            
            "COALESCE(SUM(CASE WHEN DATE(a.waktu) BETWEEN CURRENT_DATE - INTERVAL '1 month' AND CURRENT_DATE THEN a.qty ELSE 0 END), 0) AS bulan_qty, " +
            "COUNT(DISTINCT CASE WHEN DATE(a.waktu) BETWEEN CURRENT_DATE - INTERVAL '1 month' AND CURRENT_DATE THEN a.anggota_koperasi END) AS bulan_pembeli, " +
            "COALESCE(SUM(CASE WHEN DATE(a.waktu) BETWEEN CURRENT_DATE - INTERVAL '1 month' AND CURRENT_DATE THEN a.total ELSE 0 END), 0) AS bulan_total " +
            
            "FROM koperasi.pembelian a " +
            "INNER JOIN koperasi.toko b ON (a.toko = b.id and b.aktif) " +
            "WHERE DATE(a.waktu) >= CURRENT_DATE - INTERVAL '1 month' " +
            filterTokoSQL<%=rnd%> + filterNamaSQL + 
            "GROUP BY b.id, b.nama " +
            "ORDER BY bulan_total DESC;";

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sql: sqlQuery, action: "sql" })
            });
            
            const result = await response.json();
            rawDataRekap<%=rnd%> = result.data || [];
            
            let htmlTbody = '';

            let sumHariQty = 0, sumHariPembeli = 0, sumHariRp = 0;
            let sumMingguQty = 0, sumMingguPembeli = 0, sumMingguRp = 0;
            let sumBulanQty = 0, sumBulanPembeli = 0, sumBulanRp = 0;

            if (rawDataRekap<%=rnd%>.length === 0) {
                htmlTbody = '<tr><td colspan="' + baseColSpan<%=rnd%> + '" class="text-center text-muted py-5"><i class="fas fa-store-slash fa-3x mb-3 text-secondary opacity-50 d-block"></i><span class="fw-medium">' + langRekap<%=rnd%>.emptyData + '</span></td></tr>';
            } else {
                rawDataRekap<%=rnd%>.forEach((row, index) => {
                    sumHariQty += parseInt(row.harian_qty || 0);
                    sumHariPembeli += parseInt(row.harian_pembeli || 0);
                    sumHariRp += parseFloat(row.harian_total || 0);
                    
                    sumMingguQty += parseInt(row.mingguan_qty || 0);
                    sumMingguPembeli += parseInt(row.mingguan_pembeli || 0);
                    sumMingguRp += parseFloat(row.mingguan_total || 0);
                    
                    sumBulanQty += parseInt(row.bulan_qty || 0);
                    sumBulanPembeli += parseInt(row.bulan_pembeli || 0);
                    sumBulanRp += parseFloat(row.bulan_total || 0);

                    let rowClass = '';
                    let iconTrophy = '';
                    
                    if (searchInput === "" && !isLoginPedagang<%=rnd%>) {
                        if (index === 0) {
                            rowClass = 'bg-warning bg-opacity-10 fw-bold';
                            iconTrophy = '<i class="fas fa-medal text-warning me-2" title="Peringkat 1"></i>';
                        } else if (index === 1) {
                            rowClass = 'bg-secondary bg-opacity-10 fw-bold';
                            iconTrophy = '<i class="fas fa-medal text-secondary me-2" title="Peringkat 2"></i>';
                        } else if (index === 2) {
                            rowClass = 'bg-danger bg-opacity-10 fw-bold';
                            iconTrophy = '<i class="fas fa-medal text-danger me-2" title="Peringkat 3" style="color: #cd7f32 !important;"></i>';
                        }
                    }

                    htmlTbody += '<tr class="' + rowClass + '">';
                    
                    if (!isLoginPedagang<%=rnd%>) {
                        htmlTbody += '<td class="text-start fw-bold text-dark px-3 text-nowrap">' + iconTrophy + (row.nama_pedagang || '-') + '</td>';
                    }
                    
                    htmlTbody += 
                            '<td>' + formatAngkaBiasaRekap<%=rnd%>(row.harian_qty) + '</td>' +
                            '<td>' + formatAngkaBiasaRekap<%=rnd%>(row.harian_pembeli) + '</td>' +
                            '<td class="text-end text-primary fw-semibold bg-light">' + formatUangRekap<%=rnd%>(row.harian_total) + '</td>' +
                            
                            '<td>' + formatAngkaBiasaRekap<%=rnd%>(row.mingguan_qty) + '</td>' +
                            '<td>' + formatAngkaBiasaRekap<%=rnd%>(row.mingguan_pembeli) + '</td>' +
                            '<td class="text-end text-success fw-semibold bg-light">' + formatUangRekap<%=rnd%>(row.mingguan_total) + '</td>' +
                            
                            '<td>' + formatAngkaBiasaRekap<%=rnd%>(row.bulan_qty) + '</td>' +
                            '<td>' + formatAngkaBiasaRekap<%=rnd%>(row.bulan_pembeli) + '</td>' +
                            '<td class="text-end text-dark fw-bold bg-light">' + formatUangRekap<%=rnd%>(row.bulan_total) + '</td>' +
                        '</tr>';
                });

                document.getElementById('totHariQty<%=rnd%>').innerText = formatAngkaBiasaRekap<%=rnd%>(sumHariQty);
                document.getElementById('totHariPembeli<%=rnd%>').innerText = formatAngkaBiasaRekap<%=rnd%>(sumHariPembeli);
                document.getElementById('totHariRp<%=rnd%>').innerText = formatUangRekap<%=rnd%>(sumHariRp);
                
                document.getElementById('totMingguQty<%=rnd%>').innerText = formatAngkaBiasaRekap<%=rnd%>(sumMingguQty);
                document.getElementById('totMingguPembeli<%=rnd%>').innerText = formatAngkaBiasaRekap<%=rnd%>(sumMingguPembeli);
                document.getElementById('totMingguRp<%=rnd%>').innerText = formatUangRekap<%=rnd%>(sumMingguRp);
                
                document.getElementById('totBulanQty<%=rnd%>').innerText = formatAngkaBiasaRekap<%=rnd%>(sumBulanQty);
                document.getElementById('totBulanPembeli<%=rnd%>').innerText = formatAngkaBiasaRekap<%=rnd%>(sumBulanPembeli);
                document.getElementById('totBulanRp<%=rnd%>').innerText = formatUangRekap<%=rnd%>(sumBulanRp);
                
                // Jika login sebagai pedagang, tidak perlu grand total footer (karena hanya 1 baris)
                if(!isLoginPedagang<%=rnd%>) {
                     tfoot.style.display = 'table-footer-group';
                }
               
                btnExcel.disabled = false;
            }
            
            tbody.innerHTML = htmlTbody;

        } catch (error) {
            console.error("Gagal mengambil data rekap:", error);
            tbody.innerHTML = '<tr><td colspan="' + baseColSpan<%=rnd%> + '" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i>' + langRekap<%=rnd%>.errorFetch + '</td></tr>';
        } finally {
            btnRefresh.disabled = false;
        }
    };

    const downloadRekapExcel<%=rnd%> = () => {
        if (!rawDataRekap<%=rnd%> || rawDataRekap<%=rnd%>.length === 0) return;

        let tableHtml = '<html xmlns:x="urn:schemas-microsoft-com:office:excel">';
        tableHtml += '<head>';
        tableHtml += '<meta charset="UTF-8">';
        tableHtml += '<style>';
        tableHtml += '.num-format { mso-number-format:"\\#\\,\\#\\#0"; text-align: right; }'; 
        tableHtml += '.rp-format { mso-number-format:"\\#\\,\\#\\#0"; text-align: right; font-weight: bold; }'; 
        tableHtml += 'th { background-color: #f8f9fa; text-align: center; font-weight: bold; }';
        tableHtml += '</style>';
        tableHtml += '</head><body>';
        
        tableHtml += '<h4>Laporan Performa Penjualan</h4>';
        <% if (toko != null) { %>
            tableHtml += '<p>Pedagang: <b><%=toko.getNama()%></b></p>';
        <% } %>

        tableHtml += '<table border="1">';
        tableHtml += '<thead>';
        tableHtml += '<tr>';
        
        if (!isLoginPedagang<%=rnd%>) {
            tableHtml += '<th rowspan="2">Nama Pedagang</th>';
        }
        
        tableHtml += '<th colspan="3" style="background-color: #cce5ff;">Harian (Hari Ini)</th>';
        tableHtml += '<th colspan="3" style="background-color: #d4edda;">Mingguan (7 Hari)</th>';
        tableHtml += '<th colspan="3" style="background-color: #fff3cd;">Bulanan (30 Hari)</th>';
        tableHtml += '</tr><tr>';
        
        for(let i=0; i<3; i++) {
            tableHtml += '<th>Kuantitas</th><th>Pembeli</th><th>Total (Rp)</th>';
        }
        tableHtml += '</tr></thead><tbody>';

        let sumHariQty = 0, sumHariPembeli = 0, sumHariRp = 0;
        let sumMingguQty = 0, sumMingguPembeli = 0, sumMingguRp = 0;
        let sumBulanQty = 0, sumBulanPembeli = 0, sumBulanRp = 0;

        rawDataRekap<%=rnd%>.forEach(row => {
            sumHariQty += parseInt(row.harian_qty || 0);
            sumHariPembeli += parseInt(row.harian_pembeli || 0);
            sumHariRp += parseFloat(row.harian_total || 0);
            
            sumMingguQty += parseInt(row.mingguan_qty || 0);
            sumMingguPembeli += parseInt(row.mingguan_pembeli || 0);
            sumMingguRp += parseFloat(row.mingguan_total || 0);
            
            sumBulanQty += parseInt(row.bulan_qty || 0);
            sumBulanPembeli += parseInt(row.bulan_pembeli || 0);
            sumBulanRp += parseFloat(row.bulan_total || 0);

            tableHtml += '<tr>';
            if (!isLoginPedagang<%=rnd%>) {
                tableHtml += '<td>' + (row.nama_pedagang || '-') + '</td>';
            }
            tableHtml += '<td class="num-format">' + (row.harian_qty || 0) + '</td>';
            tableHtml += '<td class="num-format">' + (row.harian_pembeli || 0) + '</td>';
            tableHtml += '<td class="rp-format">' + (row.harian_total || 0) + '</td>';
            
            tableHtml += '<td class="num-format">' + (row.mingguan_qty || 0) + '</td>';
            tableHtml += '<td class="num-format">' + (row.mingguan_pembeli || 0) + '</td>';
            tableHtml += '<td class="rp-format">' + (row.mingguan_total || 0) + '</td>';
            
            tableHtml += '<td class="num-format">' + (row.bulan_qty || 0) + '</td>';
            tableHtml += '<td class="num-format">' + (row.bulan_pembeli || 0) + '</td>';
            tableHtml += '<td class="rp-format">' + (row.bulan_total || 0) + '</td>';
            tableHtml += '</tr>';
        });

        if (!isLoginPedagang<%=rnd%>) {
            tableHtml += '</tbody><tfoot style="font-weight: bold; background-color: #343a40; color: white;"><tr>';
            tableHtml += '<td>Grand Total Keseluruhan</td>';
            tableHtml += '<td class="num-format">' + sumHariQty + '</td><td class="num-format">' + sumHariPembeli + '</td><td class="rp-format">' + sumHariRp + '</td>';
            tableHtml += '<td class="num-format">' + sumMingguQty + '</td><td class="num-format">' + sumMingguPembeli + '</td><td class="rp-format">' + sumMingguRp + '</td>';
            tableHtml += '<td class="num-format">' + sumBulanQty + '</td><td class="num-format">' + sumBulanPembeli + '</td><td class="rp-format">' + sumBulanRp + '</td>';
            tableHtml += '</tr></tfoot></table></body></html>';
        } else {
             tableHtml += '</tbody></table></body></html>';
        }

        const blob = new Blob([tableHtml], { type: 'application/vnd.ms-excel' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        
        const timestamp = new Date().toISOString().slice(0, 10).replace(/-/g, "");
        const fileName = isLoginPedagang<%=rnd%> ? "Performa_Toko_<%=toko != null ? toko.getId() : ""%>_" : "Performa_Semua_Toko_";
        link.download = fileName + timestamp + ".xls";
        link.href = url;
        
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        setTimeout(() => URL.revokeObjectURL(url), 100);
    };

    document.addEventListener("DOMContentLoaded", () => {
        const elSearch = document.getElementById('searchPedagang<%=rnd%>');
        if(elSearch) {
            elSearch.addEventListener("keydown", (event) => {
                if (event.key === "Enter") {
                    event.preventDefault();
                    loadRekapPedagang<%=rnd%>();
                }
            });
        }
        loadRekapPedagang<%=rnd%>();
    });
</script>