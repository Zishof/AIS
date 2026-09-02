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
    sqlFilterToko = " AND toko = " + toko.getId() + " ";
}
%>

<div class="row mb-4 animate__animated animate__fadeInUp">
    <div class="col-12">
        <div class="card border-0 shadow-sm rounded-4 border-top border-info border-4">
            <div class="card-header bg-white border-0 pt-4 pb-0 px-4 d-flex justify-content-between align-items-center flex-wrap gap-3">
                <div>
                    <h5 class="fw-bold text-dark mb-1">
                        <i class="fas fa-chart-area text-info me-2"></i>
                        <% if (toko != null) { %>
                            <%=Common.getBahasaConfig("Tren Transaksi - ")%> <span class="text-info"><%=toko.getNama()%></span>
                        <% } else { %>
                            <%=Common.getBahasaConfig("Grafik Tren Transaksi Keseluruhan")%>
                        <% } %>
                    </h5>
                    <small class="text-muted"><%=Common.getBahasaConfig("Analisis fluktuasi jumlah transaksi untuk keperluan peramalan (forecasting).")%></small>
                </div>
                
                <ul class="nav nav-pills nav-pills-sm bg-light p-1 rounded-pill shadow-sm" id="chartTabs<%=rnd%>" role="tablist">
                    <li class="nav-item" role="presentation">
                        <button class="nav-link active rounded-pill px-4 py-1 small fw-semibold transition-all" id="tab-harian-<%=rnd%>" data-periode="harian" data-bs-toggle="pill" type="button" role="tab" aria-selected="true"><%=Common.getBahasaConfig("Harian")%></button>
                    </li>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link rounded-pill px-4 py-1 small fw-semibold transition-all" id="tab-mingguan-<%=rnd%>" data-periode="mingguan" data-bs-toggle="pill" type="button" role="tab" aria-selected="false"><%=Common.getBahasaConfig("Mingguan")%></button>
                    </li>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link rounded-pill px-4 py-1 small fw-semibold transition-all" id="tab-bulanan-<%=rnd%>" data-periode="bulanan" data-bs-toggle="pill" type="button" role="tab" aria-selected="false"><%=Common.getBahasaConfig("Bulanan")%></button>
                    </li>
                </ul>
            </div>
            
            <div class="card-body p-4 mt-2">
                <div id="loadingChart<%=rnd%>" class="text-center py-5">
                    <div class="spinner-border text-info mb-3" role="status"></div>
                    <h6 class="text-muted fw-medium"><%=Common.getBahasaConfig("Menyiapkan Grafik Analitik...")%></h6>
                </div>
                
                <div id="chartContainer<%=rnd%>" style="position: relative; height: 380px; display: none;">
                    <canvas id="forecastChart<%=rnd%>"></canvas>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    let myChart<%=rnd%> = null;
    
    const labelTransaksi<%=rnd%> = '<%=Common.getBahasaConfigJS("Jumlah Transaksi")%>';
    const labelGagal<%=rnd%> = '<%=Common.getBahasaConfigJS("Gagal Memuat Data Grafik")%>';
    const filterTokoSQL<%=rnd%> = "<%=sqlFilterToko%>";

    const loadChartData<%=rnd%> = async (periode) => {
        document.getElementById('loadingChart<%=rnd%>').style.display = 'block';
        document.getElementById('chartContainer<%=rnd%>').style.display = 'none';
        
        let sqlQuery = "";
        
        // Penentuan Query berdasarkan Tab yang dipilih (Disisipkan filter toko)
        if (periode === 'harian') {
            sqlQuery = "SELECT TO_CHAR(DATE(waktu), 'DD Mon YYYY') AS label_periode, COUNT(*) AS jumlah " +
                       "FROM (SELECT * FROM koperasi.pembelian WHERE COALESCE(aktif,true)=true) a " +
                       "WHERE waktu >= CURRENT_DATE - INTERVAL '14 days' " + filterTokoSQL<%=rnd%> +
                       "GROUP BY DATE(waktu) " +
                       "ORDER BY DATE(waktu) ASC;";
        } else if (periode === 'mingguan') {
            sqlQuery = "SELECT TO_CHAR(DATE_TRUNC('week', waktu), 'DD Mon YYYY') AS label_periode, COUNT(*) AS jumlah " +
                       "FROM (SELECT * FROM koperasi.pembelian WHERE COALESCE(aktif,true)=true) a " +
                       "WHERE waktu >= CURRENT_DATE - INTERVAL '8 weeks' " + filterTokoSQL<%=rnd%> +
                       "GROUP BY DATE_TRUNC('week', waktu) " +
                       "ORDER BY DATE_TRUNC('week', waktu) ASC;";
        } else if (periode === 'bulanan') {
            sqlQuery = "SELECT TO_CHAR(DATE_TRUNC('month', waktu), 'Mon YYYY') AS label_periode, COUNT(*) AS jumlah " +
                       "FROM (SELECT * FROM koperasi.pembelian WHERE COALESCE(aktif,true)=true) a " +
                       "WHERE waktu >= CURRENT_DATE - INTERVAL '12 months' " + filterTokoSQL<%=rnd%> +
                       "GROUP BY DATE_TRUNC('month', waktu) " +
                       "ORDER BY DATE_TRUNC('month', waktu) ASC;";
        }

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
            
            recordList.forEach(row => {
                labels.push(row.label_periode);
                dataPoin.push(parseInt(row.jumlah));
            });
            
            renderChart<%=rnd%>(labels, dataPoin, periode);
            
        } catch (error) {
            console.error("Chart Error:", error);
            document.getElementById('loadingChart<%=rnd%>').innerHTML = '<div class="text-danger py-5"><i class="fas fa-exclamation-triangle fa-3x mb-3"></i><br>' + labelGagal<%=rnd%> + '</div>';
        } finally {
            if(myChart<%=rnd%> !== null) {
                document.getElementById('loadingChart<%=rnd%>').style.display = 'none';
                document.getElementById('chartContainer<%=rnd%>').style.display = 'block';
            }
        }
    };

    const renderChart<%=rnd%> = (labels, data, periode) => {
        const ctx = document.getElementById('forecastChart<%=rnd%>').getContext('2d');
        
        if (myChart<%=rnd%> !== null) {
            myChart<%=rnd%>.destroy();
        }
        
        // Mengubah warna gradien menjadi selaras dengan tema "Info"
        let gradient = ctx.createLinearGradient(0, 0, 0, 400);
        gradient.addColorStop(0, 'rgba(23, 162, 184, 0.5)'); // Info color with opacity
        gradient.addColorStop(1, 'rgba(23, 162, 184, 0.0)');

        myChart<%=rnd%> = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: labelTransaksi<%=rnd%>,
                    data: data,
                    backgroundColor: gradient,
                    borderColor: 'rgba(23, 162, 184, 1)', // Solid Info color
                    borderWidth: 3,
                    pointBackgroundColor: '#ffffff',
                    pointBorderColor: 'rgba(23, 162, 184, 1)',
                    pointBorderWidth: 2,
                    pointRadius: 4,
                    pointHoverRadius: 6,
                    fill: true,
                    tension: 0.4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false 
                    },
                    tooltip: {
                        backgroundColor: 'rgba(33, 37, 41, 0.9)', // Darker tooltip
                        titleFont: { size: 13, family: "'Nunito', sans-serif" },
                        bodyFont: { size: 14, weight: 'bold', family: "'Nunito', sans-serif" },
                        padding: 12,
                        displayColors: false,
                        cornerRadius: 8
                    }
                },
                scales: {
                    x: {
                        grid: {
                            display: false 
                        },
                        ticks: {
                            color: '#6c757d',
                            font: { size: 12, family: "'Nunito', sans-serif" }
                        }
                    },
                    y: {
                        beginAtZero: true,
                        grid: {
                            borderDash: [5, 5],
                            color: '#e9ecef'
                        },
                        ticks: {
                            color: '#6c757d',
                            font: { size: 12, family: "'Nunito', sans-serif" },
                            stepSize: 1 
                        }
                    }
                },
                interaction: {
                    intersect: false,
                    mode: 'index',
                },
            }
        });
    };

    document.addEventListener("DOMContentLoaded", () => {
        const tabs = document.querySelectorAll('#chartTabs<%=rnd%> .nav-link');
        
        tabs.forEach(tab => {
            tab.addEventListener('click', (e) => {
                const periode = e.target.getAttribute('data-periode');
                loadChartData<%=rnd%>(periode);
            });
        });

        loadChartData<%=rnd%>('harian');
    });
</script>