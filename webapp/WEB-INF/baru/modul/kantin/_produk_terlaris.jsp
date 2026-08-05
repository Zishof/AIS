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
    
    <div class="col-lg-7">
        <div class="card border-0 shadow-sm rounded-4 h-100 border-top border-primary border-4">
            <div class="card-header bg-white border-0 pt-4 pb-0 px-4 d-flex justify-content-between align-items-center">
                <div>
                    <h6 class="fw-bold text-dark mb-1">
                        <i class="fas fa-box-open text-primary me-2"></i>
                        <% if (toko != null) { %>
                            <%=Common.getBahasaConfig("10 Produk Terlaris - ")%> <span class="text-primary"><%=toko.getNama()%></span>
                        <% } else { %>
                            <%=Common.getBahasaConfig("10 Produk Terlaris Keseluruhan")%>
                        <% } %>
                    </h6>
                    <small class="text-muted"><%=Common.getBahasaConfig("(Berdasarkan kuantitas 30 Hari Terakhir)")%></small>
                </div>
                <button class="btn btn-sm btn-light rounded-circle shadow-sm" onclick="loadChartProduk<%=rnd%>()" title="<%=Common.getBahasaConfig("Segarkan")%>">
                    <i class="fas fa-sync-alt text-primary"></i>
                </button>
            </div>
            <div class="card-body p-4 mt-2">
                <div id="loadingProduk<%=rnd%>" class="text-center py-5">
                    <div class="spinner-border text-primary mb-3" role="status"></div>
                    <h6 class="text-muted fw-medium"><%=Common.getBahasaConfig("Menyiapkan Grafik Produk...")%></h6>
                </div>
                <div id="containerProduk<%=rnd%>" style="position: relative; height: 320px; display: none;">
                    <canvas id="chartProduk<%=rnd%>"></canvas>
                </div>
            </div>
        </div>
    </div>

    <div class="col-lg-5">
        <div class="card border-0 shadow-sm rounded-4 h-100 border-top border-success border-4">
            <div class="card-header bg-white border-0 pt-4 pb-0 px-4 d-flex justify-content-between align-items-center">
                <div>
                    <h6 class="fw-bold text-dark mb-1">
                        <i class="fas fa-wallet text-success me-2"></i>
                        <% if (toko != null) { %>
                            <%=Common.getBahasaConfig("Metode Pembayaran - ")%> <span class="text-success"><%=toko.getNama()%></span>
                        <% } else { %>
                            <%=Common.getBahasaConfig("Sebaran Metode Pembayaran")%>
                        <% } %>
                    </h6>
                    <small class="text-muted"><%=Common.getBahasaConfig("(Berdasarkan nominal 30 Hari Terakhir)")%></small>
                </div>
                <button class="btn btn-sm btn-light rounded-circle shadow-sm" onclick="loadChartMetode<%=rnd%>()" title="<%=Common.getBahasaConfig("Segarkan")%>">
                    <i class="fas fa-sync-alt text-success"></i>
                </button>
            </div>
            <div class="card-body p-4 d-flex flex-column justify-content-center mt-2">
                <div id="loadingMetode<%=rnd%>" class="text-center py-5">
                    <div class="spinner-border text-success mb-3" role="status"></div>
                    <h6 class="text-muted fw-medium"><%=Common.getBahasaConfig("Menyiapkan Grafik Pembayaran...")%></h6>
                </div>
                <div id="containerMetode<%=rnd%>" style="position: relative; height: 280px; display: none;">
                    <canvas id="chartMetode<%=rnd%>"></canvas>
                </div>
            </div>
        </div>
    </div>

</div>

<script>
    // Variabel Penampung Instansi Grafik
    let myChartProduk<%=rnd%> = null;
    let myChartMetode<%=rnd%> = null;
    
    // Variabel filter dinamis dari JSP
    const filterTokoSQL<%=rnd%> = "<%=sqlFilterToko%>";

    // --- FUNGSI GRAFIK PRODUK TERLARIS ---
    const loadChartProduk<%=rnd%> = async () => {
        document.getElementById('loadingProduk<%=rnd%>').style.display = 'block';
        document.getElementById('containerProduk<%=rnd%>').style.display = 'none';

        // Query: Ambil 10 produk dengan total kuantitas terbanyak dalam 1 bulan terakhir, disisipi filter toko
        const sqlQuery = "SELECT c.nama AS namabarang, SUM(a.qty) AS total_qty " +
                         "FROM koperasi.pembelian a " +
                         "INNER JOIN koperasi.produk c ON a.produk = c.id " +
                         "WHERE DATE(a.waktu) >= CURRENT_DATE - INTERVAL '1 month' " + filterTokoSQL<%=rnd%> +
                         "GROUP BY c.id, c.nama " +
                         "ORDER BY total_qty DESC " +
                         "LIMIT 10;";

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sql: sqlQuery, action: "sql" })
            });
            
            const result = await response.json();
            const recordList = result.data || [];
            
            const labels = [];
            const dataPoin = [];
            
            // Dibalik (reverse) agar yang paling besar ada di posisi atas pada Horizontal Bar Chart
            recordList.reverse().forEach(row => {
                labels.push(row.namabarang || '<%=Common.getBahasaConfigJS("Tidak Diketahui")%>');
                dataPoin.push(parseInt(row.total_qty || 0));
            });
            
            renderChartProduk<%=rnd%>(labels, dataPoin);
            
        } catch (error) {
            console.error("Chart Produk Error:", error);
            document.getElementById('loadingProduk<%=rnd%>').innerHTML = '<div class="text-danger py-4"><i class="fas fa-exclamation-triangle fa-2x mb-2"></i><br><%=Common.getBahasaConfig("Gagal memuat data")%></div>';
        } finally {
            if(myChartProduk<%=rnd%> !== null) {
                document.getElementById('loadingProduk<%=rnd%>').style.display = 'none';
                document.getElementById('containerProduk<%=rnd%>').style.display = 'block';
            }
        }
    };

    const renderChartProduk<%=rnd%> = (labels, data) => {
        const ctx = document.getElementById('chartProduk<%=rnd%>').getContext('2d');
        if (myChartProduk<%=rnd%> !== null) myChartProduk<%=rnd%>.destroy();

        // Mencari nilai tertinggi untuk memberikan warna aksen yang berbeda (Top 1)
        const maxVal = Math.max(...data);
        const bgColors = data.map(val => val === maxVal && val > 0 ? 'rgba(13, 110, 253, 0.8)' : 'rgba(13, 110, 253, 0.4)');
        const borderColors = data.map(val => val === maxVal && val > 0 ? 'rgba(13, 110, 253, 1)' : 'rgba(13, 110, 253, 0.8)');

        myChartProduk<%=rnd%> = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: '<%=Common.getBahasaConfigJS("Total Kuantitas Terjual")%>',
                    data: data,
                    backgroundColor: bgColors,
                    borderColor: borderColors,
                    borderWidth: 1,
                    borderRadius: 4,
                    barPercentage: 0.7
                }]
            },
            options: {
                indexAxis: 'y', 
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
                        beginAtZero: true,
                        grid: { borderDash: [5, 5], color: '#e9ecef' },
                        ticks: { font: { size: 12, family: "'Nunito', sans-serif" } }
                    },
                    y: {
                        grid: { display: false },
                        ticks: { font: { size: 12, family: "'Nunito', sans-serif", weight: '500' }, color: '#495057' }
                    }
                }
            }
        });
    };

    // --- FUNGSI GRAFIK METODE PEMBAYARAN ---
    const loadChartMetode<%=rnd%> = async () => {
        document.getElementById('loadingMetode<%=rnd%>').style.display = 'block';
        document.getElementById('containerMetode<%=rnd%>').style.display = 'none';

        // Query: Agregasi total Rupiah berdasarkan metode pembayaran dalam 1 bulan terakhir
        // Menggunakan filter 'WHERE a.toko =' pada tabel utama, perhatikan ALIAS 'a.' pada Query!
        // Modifikasi query untuk memastikan alias tabel digunakan jika filter memerlukan
        const sqlQuery = "SELECT a.carabayar, SUM(a.total) AS total_rp " +
                         "FROM koperasi.pembelian a " +
                         "WHERE DATE(a.waktu) >= CURRENT_DATE - INTERVAL '1 month' " + filterTokoSQL<%=rnd%> +
                         "GROUP BY a.carabayar " +
                         "ORDER BY total_rp DESC;";

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sql: sqlQuery, action: "sql" })
            });
            
            const result = await response.json();
            const recordList = result.data || [];
            
            const labels = [];
            const dataPoin = [];
            
            // Palet Warna yang lebih modern dan cerah
            const bgColors = [
                'rgba(25, 135, 84, 0.85)',   // Success (Hijau)
                'rgba(255, 193, 7, 0.85)',   // Warning (Kuning)
                'rgba(13, 202, 240, 0.85)',  // Info (Biru Muda)
                'rgba(220, 53, 69, 0.85)',   // Danger (Merah)
                'rgba(111, 66, 193, 0.85)',  // Purple
                'rgba(108, 117, 125, 0.85)'  // Secondary (Abu)
            ];
            
            recordList.forEach(row => {
                labels.push(row.carabayar || '<%=Common.getBahasaConfigJS("Lainnya")%>');
                dataPoin.push(parseFloat(row.total_rp || 0));
            });
            
            renderChartMetode<%=rnd%>(labels, dataPoin, bgColors);
            
        } catch (error) {
            console.error("Chart Metode Error:", error);
            document.getElementById('loadingMetode<%=rnd%>').innerHTML = '<div class="text-danger py-4"><i class="fas fa-exclamation-triangle fa-2x mb-2"></i><br><%=Common.getBahasaConfig("Gagal memuat data")%></div>';
        } finally {
            if(myChartMetode<%=rnd%> !== null) {
                document.getElementById('loadingMetode<%=rnd%>').style.display = 'none';
                document.getElementById('containerMetode<%=rnd%>').style.display = 'block';
            }
        }
    };

    const renderChartMetode<%=rnd%> = (labels, data, bgColors) => {
        const ctx = document.getElementById('chartMetode<%=rnd%>').getContext('2d');
        if (myChartMetode<%=rnd%> !== null) myChartMetode<%=rnd%>.destroy();

        myChartMetode<%=rnd%> = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: data,
                    backgroundColor: bgColors.slice(0, data.length),
                    borderColor: '#ffffff',
                    borderWidth: 3,
                    hoverOffset: 8
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '68%', 
                plugins: {
                    legend: {
                        position: 'right',
                        labels: {
                            usePointStyle: true,
                            padding: 25,
                            font: { size: 13, family: "'Nunito', sans-serif", weight: '600' },
                            color: '#495057'
                        }
                    },
                    tooltip: {
                        backgroundColor: 'rgba(33, 37, 41, 0.9)',
                        titleFont: { size: 13, family: "'Nunito', sans-serif" },
                        bodyFont: { size: 14, weight: 'bold', family: "'Nunito', sans-serif" },
                        padding: 12,
                        cornerRadius: 8,
                        callbacks: {
                            label: function(context) {
                                let label = context.label || '';
                                if (label) { label += ': '; }
                                label += new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(context.raw);
                                return label;
                            }
                        }
                    }
                }
            }
        });
    };

    // --- INISIALISASI ---
    document.addEventListener("DOMContentLoaded", () => {
        loadChartProduk<%=rnd%>();
        loadChartMetode<%=rnd%>();
    });
</script>