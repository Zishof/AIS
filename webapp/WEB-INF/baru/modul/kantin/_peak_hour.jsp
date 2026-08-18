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
    // Karena tabel pembelian diasumsikan menggunakan ID toko
    sqlFilterToko = " AND a.toko = " + toko.getId() + " ";
}
%>

<div class="row g-4 mb-4 animate__animated animate__fadeInUp">
    <div class="col-lg-6">
        <div class="card border-0 shadow-sm rounded-4 h-100 border-top border-info border-4">
            <div class="card-header bg-white border-0 pt-4 pb-0 px-4 d-flex justify-content-between align-items-center">
                <div>
                    <h6 class="fw-bold text-dark mb-1">
                        <i class="fas fa-clock text-info me-2"></i>
                        <% if (toko != null) { %>
                            <%=Common.getBahasaConfig("Jam Sibuk Transaksi - ")%> <span class="text-info"><%=toko.getNama()%></span>
                        <% } else { %>
                            <%=Common.getBahasaConfig("Jam Sibuk Transaksi Keseluruhan")%>
                        <% } %>
                    </h6>
                    <small class="text-muted"><%=Common.getBahasaConfig("(Berdasarkan data 30 Hari Terakhir)")%></small>
                </div>
                <button class="btn btn-sm btn-light rounded-circle shadow-sm" onclick="loadChartJamSibuk<%=rnd%>()" title="<%=Common.getBahasaConfig("Segarkan")%>">
                    <i class="fas fa-sync-alt text-info"></i>
                </button>
            </div>
            <div class="card-body p-4 mt-2">
                <div id="loadingJam<%=rnd%>" class="text-center py-5">
                    <div class="spinner-border text-info mb-3" role="status"></div>
                    <h6 class="text-muted fw-medium"><%=Common.getBahasaConfig("Menyiapkan Data Jam Sibuk...")%></h6>
                </div>
                <div id="containerJam<%=rnd%>" style="position: relative; height: 320px; display: none;">
                    <canvas id="chartJamSibuk<%=rnd%>"></canvas>
                </div>
            </div>
        </div>
    </div>

    <div class="col-lg-6">
        <div class="card border-0 shadow-sm rounded-4 h-100 border-top border-warning border-4">
            <div class="card-header bg-white border-0 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-2 border-bottom border-light">
                <div>
                    <h6 class="fw-bold text-dark mb-1">
                        <i class="fas fa-crown text-warning me-2"></i>
                        <% if (toko != null) { %>
                            <%=Common.getBahasaConfig("10 Pembeli Terloyal - ")%> <span class="text-warning"><%=toko.getNama()%></span>
                        <% } else { %>
                            <%=Common.getBahasaConfig("10 Pembeli Terloyal Keseluruhan")%>
                        <% } %>
                    </h6>
                    <small class="text-muted"><%=Common.getBahasaConfig("(Berdasarkan nominal 30 Hari Terakhir)")%></small>
                </div>
                <div>
                    <button class="btn btn-sm btn-outline-primary rounded-pill px-3 fw-semibold shadow-sm text-nowrap" onclick="loadPembeliLoyal<%=rnd%>()" id="btnRefreshPembeli<%=rnd%>">
                        <i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Segarkan")%>
                    </button>
                    <button class="btn btn-sm btn-success rounded-pill px-3 fw-semibold shadow-sm text-nowrap" onclick="downloadPembeliExcel<%=rnd%>()" id="btnExcelPembeli<%=rnd%>" disabled>
                        <i class="fas fa-file-excel me-1"></i><%=Common.getBahasaConfig("Unduh")%>
                    </button>
                </div>
            </div>
            <div class="card-body p-4 pt-3">
                <div class="table-responsive border rounded-3 shadow-sm" style="max-height: 320px; overflow-y: auto;">
                    <table class="table table-hover table-bordered align-middle mb-0 text-center" style="font-size: 0.9rem;">
                        <thead class="table-dark align-middle sticky-top">
                            <tr>
                                <th class="fw-semibold px-2" style="width: 40px;">#</th>
                                <th class="text-start fw-semibold px-3"><i class="fas fa-user me-1"></i><%=Common.getBahasaConfig("Nama Pembeli")%></th>
                                <th class="fw-semibold px-2" title="<%=Common.getBahasaConfig("Frekuensi Belanja")%>"><i class="fas fa-shopping-cart"></i></th>
                                <th class="text-end fw-semibold px-3"><i class="fas fa-coins me-1"></i><%=Common.getBahasaConfig("Total (Rp)")%></th>
                            </tr>
                        </thead>
                        <tbody id="tabelPembeliLoyal<%=rnd%>">
                            <tr>
                                <td colspan="4" class="text-center py-5 text-muted">
                                    <div class="spinner-border text-warning mb-3" role="status"></div><br>
                                    <span class="fw-medium"><%=Common.getBahasaConfig("Memuat data pembeli...")%></span>
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
    const formatUang<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);
    };
    const formatAngkaBiasa<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID').format(angka || 0);
    };

    let myChartJamSibuk<%=rnd%> = null;
    let rawDataPembeli<%=rnd%> = [];
    
    // Variabel filter dinamis dari JSP
    const filterTokoSQL<%=rnd%> = "<%=sqlFilterToko%>";
    const isLoginPedagang<%=rnd%> = <%= toko != null %>;

    // --- FUNGSI CHART JAM SIBUK ---
    const loadChartJamSibuk<%=rnd%> = async () => {
        document.getElementById('loadingJam<%=rnd%>').style.display = 'block';
        document.getElementById('containerJam<%=rnd%>').style.display = 'none';

        // Query: Ekstrak JAM dari waktu transaksi, lalu hitung jumlahnya, disisipi filter toko
        const sqlQuery = "SELECT EXTRACT(HOUR FROM a.waktu) AS jam_transaksi, COUNT(a.id) AS jumlah_transaksi " +
                         "FROM koperasi.pembelian a " +
                         "WHERE DATE(a.waktu) >= CURRENT_DATE - INTERVAL '1 month' " + filterTokoSQL<%=rnd%> +
                         "GROUP BY EXTRACT(HOUR FROM a.waktu) " +
                         "ORDER BY jam_transaksi ASC;";

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sql: sqlQuery, action: "sql" })
            });
            
            const result = await response.json();
            const recordList = result.data || [];
            
            // Siapkan array 24 jam (00:00 - 23:00) agar grafik tidak bolong jika ada jam yang kosong
            const labels = Array.from({length: 24}, (_, i) => i.toString().padStart(2, '0') + ":00");
            const dataPoin = new Array(24).fill(0);
            
            recordList.forEach(row => {
                const jam = parseInt(row.jam_transaksi || 0);
                if(jam >= 0 && jam <= 23) {
                    dataPoin[jam] = parseInt(row.jumlah_transaksi || 0);
                }
            });
            
            renderChartJamSibuk<%=rnd%>(labels, dataPoin);
            
        } catch (error) {
            console.error("Chart Jam Error:", error);
            document.getElementById('loadingJam<%=rnd%>').innerHTML = '<div class="text-danger py-4"><i class="fas fa-exclamation-triangle fa-2x mb-2"></i><br><%=Common.getBahasaConfig("Gagal memuat data")%></div>';
        } finally {
            if(myChartJamSibuk<%=rnd%> !== null) {
                document.getElementById('loadingJam<%=rnd%>').style.display = 'none';
                document.getElementById('containerJam<%=rnd%>').style.display = 'block';
            }
        }
    };

    const renderChartJamSibuk<%=rnd%> = (labels, data) => {
        const ctx = document.getElementById('chartJamSibuk<%=rnd%>').getContext('2d');
        if (myChartJamSibuk<%=rnd%> !== null) myChartJamSibuk<%=rnd%>.destroy();

        // Cari nilai tertinggi untuk memberi warna beda pada bar tertinggi (puncak sibuk)
        const maxVal = Math.max(...data);
        const bgColors = data.map(val => val === maxVal && val > 0 ? 'rgba(220, 53, 69, 0.8)' : 'rgba(23, 162, 184, 0.6)');
        const borderColors = data.map(val => val === maxVal && val > 0 ? 'rgba(220, 53, 69, 1)' : 'rgba(23, 162, 184, 1)');

        myChartJamSibuk<%=rnd%> = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: '<%=Common.getBahasaConfigJS("Total Transaksi")%>',
                    data: data,
                    backgroundColor: bgColors,
                    borderColor: borderColors,
                    borderWidth: 1,
                    borderRadius: 4,
                    barPercentage: 0.8
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: { 
                        backgroundColor: 'rgba(33, 37, 41, 0.9)',
                        titleFont: { size: 13, family: "'Nunito', sans-serif" },
                        bodyFont: { size: 14, weight: 'bold', family: "'Nunito', sans-serif" },
                        padding: 12,
                        cornerRadius: 8
                    }
                },
                scales: {
                    x: {
                        grid: { display: false },
                        ticks: { font: { size: 11, family: "'Nunito', sans-serif" }, maxRotation: 45, minRotation: 45, color: '#6c757d' }
                    },
                    y: {
                        beginAtZero: true,
                        grid: { borderDash: [5, 5], color: '#e9ecef' },
                        ticks: { stepSize: 1, font: { family: "'Nunito', sans-serif" }, color: '#6c757d' }
                    }
                }
            }
        });
    };

    // --- FUNGSI TABEL PEMBELI LOYAL ---
    const loadPembeliLoyal<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelPembeliLoyal<%=rnd%>');
        const btnRefresh = document.getElementById('btnRefreshPembeli<%=rnd%>');
        const btnExcel = document.getElementById('btnExcelPembeli<%=rnd%>');
        
        tbody.innerHTML = '<tr><td colspan="4" class="text-center py-5 text-muted"><div class="spinner-border text-warning mb-3" role="status"></div><br><span class="fw-medium"><%=Common.getBahasaConfig("Memuat data pembeli...")%></span></td></tr>';
        btnRefresh.disabled = true;
        btnExcel.disabled = true;

        // Query: Hitung frekuensi belanja dan total rupiah per pembeli, disisipi filter toko
        const sqlQuery = "SELECT b.nama AS nama_pembeli, " +
                         "COUNT(a.id) AS frekuensi_belanja, " +
                         "SUM(a.total) AS total_rp " +
                         "FROM koperasi.pembelian a " +
                         "INNER JOIN koperasi.anggota_koperasi b on (a.anggota_koperasi = b.id) " +
                         "WHERE DATE(a.waktu) >= CURRENT_DATE - INTERVAL '1 month' " +
                         "AND a.anggota_koperasi IS NOT NULL " + filterTokoSQL<%=rnd%> +
                         "GROUP BY b.id, b.nama " + // Tambahkan b.nama di GROUP BY agar sesuai standar SQL
                         "ORDER BY total_rp DESC " +
                         "LIMIT 10;";

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sql: sqlQuery, action: "sql" })
            });
            
            const result = await response.json();
            rawDataPembeli<%=rnd%> = result.data || [];
            
            let htmlTbody = '';

            if (rawDataPembeli<%=rnd%>.length === 0) {
                htmlTbody = '<tr><td colspan="4" class="text-center text-muted py-5"><i class="fas fa-users-slash fa-3x mb-3 text-secondary opacity-50 d-block"></i><span class="fw-medium"><%=Common.getBahasaConfig("Belum ada data.")%></span></td></tr>';
            } else {
                let no = 1;
                rawDataPembeli<%=rnd%>.forEach((row, index) => {
                    // Beri highlight untuk Top 1, 2, dan 3
                    let rowClass = '';
                    let iconTrophy = '';
                    
                    if (index === 0) {
                        rowClass = 'bg-warning bg-opacity-10 fw-bold';
                        iconTrophy = '<i class="fas fa-crown text-warning me-2" title="Pelanggan Terbaik 1"></i>';
                    } else if (index === 1) {
                        rowClass = 'bg-secondary bg-opacity-10 fw-bold';
                        iconTrophy = '<i class="fas fa-medal text-secondary me-2" title="Pelanggan Terbaik 2"></i>';
                    } else if (index === 2) {
                        rowClass = 'bg-danger bg-opacity-10 fw-bold';
                        iconTrophy = '<i class="fas fa-medal text-danger me-2" title="Pelanggan Terbaik 3" style="color: #cd7f32 !important;"></i>';
                    }

                    htmlTbody += 
                        '<tr class="' + rowClass + '">' +
                            '<td class="text-muted">' + no++ + '</td>' +
                            '<td class="text-start px-3 text-dark">' + iconTrophy + (row.nama_pembeli || '-') + '</td>' +
                            '<td><span class="badge bg-primary rounded-pill px-2 py-1 shadow-sm">' + formatAngkaBiasa<%=rnd%>(row.frekuensi_belanja) + 'x</span></td>' +
                            '<td class="text-end text-success px-3 fw-semibold">' + formatUang<%=rnd%>(row.total_rp) + '</td>' +
                        '</tr>';
                });
                btnExcel.disabled = false;
            }
            tbody.innerHTML = htmlTbody;

        } catch (error) {
            console.error("Gagal mengambil data pembeli:", error);
            tbody.innerHTML = '<tr><td colspan="4" class="text-center text-danger py-4"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal memuat data.")%></td></tr>';
        } finally {
            btnRefresh.disabled = false;
        }
    };

    // --- FUNGSI EXCEL PEMBELI LOYAL ---
    const downloadPembeliExcel<%=rnd%> = () => {
        if (!rawDataPembeli<%=rnd%> || rawDataPembeli<%=rnd%>.length === 0) return;

        let tableHtml = '<html xmlns:x="urn:schemas-microsoft-com:office:excel"><head><meta charset="UTF-8"><style>';
        tableHtml += '.num-format { mso-number-format:"\\#\\,\\#\\#0"; text-align: center; }';
        tableHtml += '.rp-format { mso-number-format:"\\#\\,\\#\\#0"; text-align: right; }';
        tableHtml += 'th { background-color: #f8f9fa; font-weight: bold; }';
        tableHtml += '</style></head><body>';
        
        tableHtml += '<h4>Laporan 10 Pembeli Terloyal (30 Hari Terakhir)</h4>';
        <% if (toko != null) { %>
            tableHtml += '<p>Pedagang: <b><%=toko.getNama()%></b></p>';
        <% } %>
        tableHtml += '<table border="1"><thead><tr>';
        tableHtml += '<th>Peringkat</th><th>Nama Pembeli</th><th>Frekuensi Belanja (Kali)</th><th>Total Transaksi (Rp)</th>';
        tableHtml += '</tr></thead><tbody>';

        let no = 1;
        rawDataPembeli<%=rnd%>.forEach(row => {
            tableHtml += '<tr>';
            tableHtml += '<td style="text-align: center;">' + no++ + '</td>';
            tableHtml += '<td>' + (row.nama_pembeli || '-') + '</td>';
            tableHtml += '<td class="num-format">' + (row.frekuensi_belanja || 0) + '</td>';
            tableHtml += '<td class="rp-format">' + (row.total_rp || 0) + '</td>';
            tableHtml += '</tr>';
        });

        tableHtml += '</tbody></table></body></html>';

        const blob = new Blob([tableHtml], { type: 'application/vnd.ms-excel' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        const timestamp = new Date().toISOString().slice(0, 10).replace(/-/g, "");
        const fileName = isLoginPedagang<%=rnd%> ? "Top10_Pembeli_Toko_<%=toko != null ? toko.getId() : ""%>_" : "Top10_Pembeli_Semua_";
        link.download = fileName + timestamp + ".xls";
        link.href = url;
        
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        setTimeout(() => URL.revokeObjectURL(url), 100);
    };

    document.addEventListener("DOMContentLoaded", () => {
        loadChartJamSibuk<%=rnd%>();
        loadPembeliLoyal<%=rnd%>();
    });
</script>