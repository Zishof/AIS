<%@page import="ais.database.model.koperasi.AnggotaKoperasi"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// Pengecekan Sesi Pengguna
Tbmuser tbmuser = Common.getCurrentUser(request);
AnggotaKoperasi curentAnggota = tbmuser == null ? null : tbmuser.getAnggotaKoperasi();

if(curentAnggota == null) {
    out.print("<div class='text-center p-5'><i class='fas fa-exclamation-triangle fa-3x text-warning mb-3'></i><br><h5 class='text-muted'>" + Common.getBahasaConfig("Akses Ditolak") + "</h5><p>" + Common.getBahasaConfig("Halaman ini hanya dapat diakses oleh Anggota Koperasi yang sah.") + "</p></div>");
    return;
}

String rnd = Common.getGeneratedBarCode(7);
Long idMemberAktif = curentAnggota.getId();
%>


<div class="container-fluid py-2 animate__animated animate__fadeIn" id="dashboardMemberContent<%=rnd%>">

    <!-- HEADER DASHBOARD -->
    <div class="mb-4">
        <h5 class="fw-bolder text-primary mb-1">
            <i class="fas fa-chart-pie me-2"></i><%=Common.getBahasaConfig("Dashboard Finansial Saya")%>
        </h5>
        <p class="text-muted small mb-0"><%=Common.getBahasaConfig("Ringkasan aktivitas pembelanjaan, penghematan, dan riwayat isi saldo Anda.")%></p>
    </div>

    <!-- WIDGET METRIK UTAMA -->
    <div class="row g-3 mb-4">
        <!-- Widget Pengeluaran -->
        <div class="col-md-6 col-lg-3">
            <div class="bg-primary bg-opacity-10 p-3 rounded-4 border border-primary border-opacity-25 h-100 d-flex flex-column justify-content-center shadow-sm" style="transition: transform 0.2s; cursor: default;" onmouseover="this.style.transform='translateY(-3px)'" onmouseout="this.style.transform='translateY(0)'">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <div class="text-muted small fw-bold"><%=Common.getBahasaConfig("Total Pengeluaran")%></div>
                    <i class="fas fa-shopping-basket text-primary opacity-50"></i>
                </div>
                <h4 class="fw-bolder text-primary mb-0 text-truncate" id="dashTotalPengeluaran<%=rnd%>">
                    <i class="fas fa-spinner fa-spin fs-6"></i>
                </h4>
            </div>
        </div>
        
        <!-- Widget Transaksi -->
        <div class="col-md-6 col-lg-3">
            <div class="bg-success bg-opacity-10 p-3 rounded-4 border border-success border-opacity-25 h-100 d-flex flex-column justify-content-center shadow-sm" style="transition: transform 0.2s; cursor: default;" onmouseover="this.style.transform='translateY(-3px)'" onmouseout="this.style.transform='translateY(0)'">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <div class="text-muted small fw-bold"><%=Common.getBahasaConfig("Jumlah Transaksi")%></div>
                    <i class="fas fa-receipt text-success opacity-50"></i>
                </div>
                <h4 class="fw-bolder text-success mb-0 text-truncate" id="dashJmlTransaksi<%=rnd%>">
                    <i class="fas fa-spinner fa-spin fs-6"></i>
                </h4>
            </div>
        </div>
        
        <!-- Widget Cashback / Diskon -->
        <div class="col-md-6 col-lg-3">
            <div class="bg-warning bg-opacity-10 p-3 rounded-4 border border-warning border-opacity-50 h-100 d-flex flex-column justify-content-center shadow-sm" style="transition: transform 0.2s; cursor: default;" onmouseover="this.style.transform='translateY(-3px)'" onmouseout="this.style.transform='translateY(0)'">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <div class="text-dark small fw-bold opacity-75"><%=Common.getBahasaConfig("Total Hemat (Diskon/CB)")%></div>
                    <i class="fas fa-piggy-bank text-warning opacity-75"></i>
                </div>
                <h4 class="fw-bolder text-dark mb-0 text-truncate" id="dashTotalHemat<%=rnd%>">
                    <i class="fas fa-spinner fa-spin fs-6"></i>
                </h4>
            </div>
        </div>

        <!-- Widget Top-Up VA -->
        <div class="col-md-6 col-lg-3">
            <div class="bg-info bg-opacity-10 p-3 rounded-4 border border-info border-opacity-25 h-100 d-flex flex-column justify-content-center shadow-sm" style="transition: transform 0.2s; cursor: default;" onmouseover="this.style.transform='translateY(-3px)'" onmouseout="this.style.transform='translateY(0)'">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <div class="text-muted small fw-bold"><%=Common.getBahasaConfig("Total Top-Up Berhasil")%></div>
                    <i class="fas fa-wallet text-info opacity-75"></i>
                </div>
                <h4 class="fw-bolder text-info text-dark mb-0 text-truncate" id="dashTotalTopup<%=rnd%>">
                    <i class="fas fa-spinner fa-spin fs-6"></i>
                </h4>
            </div>
        </div>
    </div>

    <!-- AREA GRAFIK (CHARTS) -->
    <div class="row g-4">
        <!-- Grafik Tren Pembelanjaan (Bar Chart) -->
        <div class="col-lg-8">
            <div class="card border-0 shadow-sm rounded-4 h-100">
                <div class="card-header bg-white border-bottom-0 pt-4 pb-0 px-4">
                    <h6 class="fw-bold text-dark mb-1"><i class="fas fa-chart-bar text-primary me-2"></i><%=Common.getBahasaConfig("Tren Pengeluaran (6 Bulan Terakhir)")%></h6>
                    <small class="text-muted"><%=Common.getBahasaConfig("Berapa yang sudah Anda belanjakan tiap bulan, supaya mudah mengatur pengeluaran.")%></small>
                </div>
                <div class="card-body p-4">
                    <div style="height: 300px; width: 100%;">
                        <canvas id="chartTrendBelanja<%=rnd%>"></canvas>
                    </div>
                </div>
            </div>
        </div>

        <!-- Grafik Distribusi Toko (Doughnut Chart) -->
        <div class="col-lg-4">
            <div class="card border-0 shadow-sm rounded-4 h-100">
                <div class="card-header bg-white border-bottom-0 pt-4 pb-0 px-4">
                    <h6 class="fw-bold text-dark mb-1"><i class="fas fa-store text-success me-2"></i><%=Common.getBahasaConfig("Distribusi Toko Favorit")%></h6>
                    <small class="text-muted"><%=Common.getBahasaConfig("Toko mana yang paling sering Anda kunjungi untuk belanja.")%></small>
                </div>
                <div class="card-body p-4 d-flex justify-content-center align-items-center">
                    <div style="height: 250px; width: 100%; position: relative;">
                        <canvas id="chartTokoFavorit<%=rnd%>"></canvas>
                        <!-- Pesan jika data kosong -->
                        <div id="emptyTokoMsg<%=rnd%>" class="position-absolute top-50 start-50 translate-middle text-muted small fw-bold d-none text-center">
                            <i class="fas fa-box-open fa-2x mb-2 opacity-25"></i><br>
                            <%=Common.getBahasaConfig("Belum ada data")%>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

</div>

<script>
    // ==========================================
    // VARIABEL GLOBAL & INISIALISASI
    // ==========================================
    const memberIdDash<%=rnd%> = <%=idMemberAktif%>;
    let chartTrendInstance<%=rnd%> = null;
    let chartTokoInstance<%=rnd%> = null;

    // Helper Format Rupiah
    const formatRpDash<%=rnd%> = (angka) => {
        return "Rp " + new Intl.NumberFormat('id-ID').format(angka || 0);
    };

    // Helper Panggil API Data SQL
    const fetchSqlAPI<%=rnd%> = async (sql) => {
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: "sql", sql: sql })
            });
            const result = await res.json();
            return result.data || [];
        } catch (e) { 
            console.error(e); 
            return []; 
        }
    };

    // ==========================================
    // FUNGSI MUAT WIDGET METRIK UTAMA
    // ==========================================
    const loadDashboardMetrics<%=rnd%> = async () => {
        // 1. Kueri Metrik Belanja (Pengeluaran, Transaksi, Cashback/Diskon)
        const sqlBelanja = "SELECT COUNT(id) AS jml_trx, " +
                           "COALESCE(SUM(total_biaya), 0) AS total_pengeluaran, " +
                           "COALESCE(SUM(COALESCE(total_diskon, 0) + COALESCE(totalcashback, 0)), 0) AS total_hemat " +
                           "FROM koperasi.pembelian_anggota_koperasi " +
                           "WHERE anggota_koperasi = " + memberIdDash<%=rnd%> + ";";

        // 2. Kueri Metrik Top-Up — ambil dari public.deposit (sumber saldo resmi: mencakup
        //    top-up manual oleh admin MAUPUN top-up online via VA, sama dengan DepositHelper).
        const sqlTopup = "SELECT COALESCE(SUM(nominal), 0) AS total_topup " +
                         "FROM public.deposit " +
                         "WHERE anggota_koperasi = " + memberIdDash<%=rnd%> + ";";

        try {
            const [resBelanja, resTopup] = await Promise.all([
                fetchSqlAPI<%=rnd%>(sqlBelanja),
                fetchSqlAPI<%=rnd%>(sqlTopup)
            ]);

            // Set Data Belanja
            if (resBelanja && resBelanja.length > 0) {
                const data = resBelanja[0];
                document.getElementById('dashJmlTransaksi<%=rnd%>').innerText = (data.jml_trx || 0) + ' <%=Common.getBahasaConfig("Kali")%>';
                document.getElementById('dashTotalPengeluaran<%=rnd%>').innerText = formatRpDash<%=rnd%>(data.total_pengeluaran);
                document.getElementById('dashTotalHemat<%=rnd%>').innerText = formatRpDash<%=rnd%>(data.total_hemat);
            }

            // Set Data Topup
            if (resTopup && resTopup.length > 0) {
                document.getElementById('dashTotalTopup<%=rnd%>').innerText = formatRpDash<%=rnd%>(resTopup[0].total_topup);
            }

        } catch (error) {
            console.error("Gagal memuat metrik dashboard:", error);
            document.getElementById('dashJmlTransaksi<%=rnd%>').innerText = "0";
            document.getElementById('dashTotalPengeluaran<%=rnd%>').innerText = "Rp 0";
            document.getElementById('dashTotalHemat<%=rnd%>').innerText = "Rp 0";
            document.getElementById('dashTotalTopup<%=rnd%>').innerText = "Rp 0";
        }
    };

    // ==========================================
    // FUNGSI MUAT GRAFIK (CHART.JS)
    // ==========================================
    const loadDashboardCharts<%=rnd%> = async () => {
        // --- GRAFIK 1: Tren Pembelanjaan (6 Bulan Terakhir) ---
        const sqlTrend = "SELECT TO_CHAR(tanggal_pembayaran, 'Mon YYYY') as bulan_label, " +
                         "TO_CHAR(tanggal_pembayaran, 'YYYY-MM') as bulan_urut, " +
                         "COALESCE(SUM(total_biaya), 0) as total_nominal " +
                         "FROM koperasi.pembelian_anggota_koperasi " +
                         "WHERE anggota_koperasi = " + memberIdDash<%=rnd%> + " " +
                         "GROUP BY TO_CHAR(tanggal_pembayaran, 'Mon YYYY'), TO_CHAR(tanggal_pembayaran, 'YYYY-MM') " +
                         "ORDER BY bulan_urut ASC LIMIT 6;";

        // --- GRAFIK 2: Distribusi Pembelanjaan Per Toko ---
        const sqlToko = "SELECT COALESCE(t.nama, 'Koperasi Utama') as nama_toko, " +
                        "COALESCE(SUM(a.total_biaya), 0) as total_nominal " +
                        "FROM koperasi.pembelian_anggota_koperasi a " +
                        "LEFT JOIN koperasi.toko t ON a.toko = t.id " +
                        "WHERE a.anggota_koperasi = " + memberIdDash<%=rnd%> + " " +
                        "GROUP BY t.nama " +
                        "ORDER BY total_nominal DESC LIMIT 5;";

        try {
            const [resTrend, resToko] = await Promise.all([
                fetchSqlAPI<%=rnd%>(sqlTrend),
                fetchSqlAPI<%=rnd%>(sqlToko)
            ]);

            // Render Chart Tren
            const ctxTrend = document.getElementById('chartTrendBelanja<%=rnd%>').getContext('2d');
            const labelsTrend = resTrend.map(r => r.bulan_label);
            const dataTrend = resTrend.map(r => parseFloat(r.total_nominal));

            if (chartTrendInstance<%=rnd%>) chartTrendInstance<%=rnd%>.destroy();
            chartTrendInstance<%=rnd%> = new Chart(ctxTrend, {
                type: 'bar',
                data: {
                    labels: labelsTrend.length > 0 ? labelsTrend : ['Belum Ada Data'],
                    datasets: [{
                        label: '<%=Common.getBahasaConfigJS("Total Pengeluaran (Rp)")%>',
                        data: dataTrend.length > 0 ? dataTrend : [0],
                        backgroundColor: 'rgba(59, 130, 246, 0.8)', // Primary color
                        borderColor: 'rgba(59, 130, 246, 1)',
                        borderWidth: 1,
                        borderRadius: 6,
                        barPercentage: 0.6
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            callbacks: {
                                label: function(context) { return formatRpDash<%=rnd%>(context.raw); }
                            }
                        }
                    },
                    scales: {
                        y: { beginAtZero: true, ticks: { callback: function(value) { return 'Rp ' + (value/1000) + 'K'; } } },
                        x: { grid: { display: false } }
                    }
                }
            });

            // Render Chart Toko (Doughnut)
            const ctxToko = document.getElementById('chartTokoFavorit<%=rnd%>').getContext('2d');
            
            if (resToko.length === 0) {
                document.getElementById('emptyTokoMsg<%=rnd%>').classList.remove('d-none');
            } else {
                document.getElementById('emptyTokoMsg<%=rnd%>').classList.add('d-none');
            }

            const labelsToko = resToko.map(r => r.nama_toko);
            const dataToko = resToko.map(r => parseFloat(r.total_nominal));
            const colorPalette = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];

            if (chartTokoInstance<%=rnd%>) chartTokoInstance<%=rnd%>.destroy();
            chartTokoInstance<%=rnd%> = new Chart(ctxToko, {
                type: 'doughnut',
                data: {
                    labels: labelsToko,
                    datasets: [{
                        data: dataToko,
                        backgroundColor: colorPalette,
                        borderWidth: 2,
                        hoverOffset: 4
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    cutout: '65%',
                    plugins: {
                        legend: { position: 'bottom', labels: { usePointStyle: true, padding: 20, font: { size: 11 } } },
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    const label = context.label || '';
                                    const value = context.raw || 0;
                                    return ' ' + label + ': ' + formatRpDash<%=rnd%>(value);
                                }
                            }
                        }
                    }
                }
            });

        } catch (error) {
            console.error("Gagal merender grafik dashboard:", error);
        }
    };

    // ==========================================
    // TRIGGER INITIALIZATION
    // ==========================================
    const initDashboardMember<%=rnd%> = () => {
        loadDashboardMetrics<%=rnd%>();
        loadDashboardCharts<%=rnd%>();
    };

    // Memuat saat DOM dirender (gunakan setTimeout untuk transisi tab agar Chart responsif)
    setTimeout(initDashboardMember<%=rnd%>, 250);

</script>