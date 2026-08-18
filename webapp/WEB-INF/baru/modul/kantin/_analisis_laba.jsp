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
String sqlFilterTokoProduk = "";
if (toko != null) {
    // Filter ke toko spesifik
    sqlFilterToko = " AND a.toko = " + toko.getId() + " ";
    sqlFilterTokoProduk = " AND toko = " + toko.getId() + " ";
}
%>

<div class="row mb-4 animate__animated animate__fadeInUp">
    <div class="col-12">
        <div class="card border-0 shadow-sm rounded-4 border-top border-success border-4">
            
            <div class="card-header bg-white border-0 pt-4 pb-3 px-4 border-bottom border-light">
                <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-3">
                    <div>
                        <h5 class="fw-bold text-dark mb-1">
                            <i class="fas fa-hand-holding-usd text-success me-2"></i>
                            <% if (toko != null) { %>
                                <%=Common.getBahasaConfig("Analisis Laba Kotor - ")%> <span class="text-success"><%=toko.getNama()%></span>
                            <% } else { %>
                                <%=Common.getBahasaConfig("Analisis Laba Kotor Keseluruhan")%>
                            <% } %>
                        </h5>
                        <small class="text-muted"><%=Common.getBahasaConfig("Pantau estimasi keuntungan bersih (Omzet dikurangi Harga Pokok Pembelian) berdasarkan filter yang dipilih.")%></small>
                    </div>
                </div>
                
                <div class="d-flex align-items-center gap-2 flex-wrap bg-light p-2 rounded-3 border">
                    <select class="form-select form-select-sm rounded-3 shadow-sm border-0" id="filterPeriodeLaba<%=rnd%>" style="width: 160px; cursor: pointer;">
                        <option value="7 days"><%=Common.getBahasaConfig("7 Hari Terakhir")%></option>
                        <option value="14 days" selected><%=Common.getBahasaConfig("14 Hari Terakhir")%></option>
                        <option value="1 month"><%=Common.getBahasaConfig("30 Hari Terakhir")%></option>
                        <option value="3 months"><%=Common.getBahasaConfig("3 Bulan Terakhir")%></option>
                    </select>

                    <% if (toko == null) { %>
                    <div class="input-group input-group-sm shadow-sm" style="width: 180px;">
                        <span class="input-group-text bg-white border-end-0">
                            <i class="fas fa-store text-muted"></i>
                        </span>
                        <input type="text" class="form-control border-start-0 ps-0 bg-white" id="searchPedagangLaba<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Nama Pedagang...")%>">
                    </div>
                    <% } else { %>
                        <input type="hidden" id="searchPedagangLaba<%=rnd%>" value="">
                    <% } %>

                    <div class="input-group input-group-sm shadow-sm" style="width: 180px;">
                        <span class="input-group-text bg-white border-end-0">
                            <i class="fas fa-box text-muted"></i>
                        </span>
                        <input type="text" class="form-control border-start-0 ps-0 bg-white" id="searchProdukLaba<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Nama Barang...")%>">
                    </div>

                    <button class="btn btn-sm btn-success rounded-pill px-4 fw-bold shadow-sm text-nowrap" onclick="loadAnalisisLaba<%=rnd%>()" id="btnRefreshLaba<%=rnd%>">
                        <i class="fas fa-search me-1"></i><%=Common.getBahasaConfig("Saring Data")%>
                    </button>
                </div>
            </div>
            
            <div class="card-body p-4 pt-4 bg-light bg-opacity-50">
                
                <div class="row g-3 mb-4">
                    <div class="col-md-3 col-sm-6">
                        <div class="card border-0 shadow-sm rounded-3 h-100">
                            <div class="card-body p-3">
                                <div class="d-flex justify-content-between align-items-center mb-2">
                                    <span class="text-muted small fw-bold text-uppercase"><%=Common.getBahasaConfig("Total Omzet")%></span>
                                    <div class="bg-primary bg-opacity-10 text-primary rounded-circle p-2"><i class="fas fa-cash-register fa-fw"></i></div>
                                </div>
                                <h4 class="fw-bold text-dark mb-0" id="labaOmzet<%=rnd%>">Rp 0</h4>
                            </div>
                        </div>
                    </div>
                    
                    <div class="col-md-3 col-sm-6">
                        <div class="card border-0 shadow-sm rounded-3 h-100">
                            <div class="card-body p-3">
                                <div class="d-flex justify-content-between align-items-center mb-2">
                                    <span class="text-muted small fw-bold text-uppercase"><%=Common.getBahasaConfig("Total Modal (HPP)")%></span>
                                    <div class="bg-danger bg-opacity-10 text-danger rounded-circle p-2"><i class="fas fa-file-invoice-dollar fa-fw"></i></div>
                                </div>
                                <h4 class="fw-bold text-dark mb-0" id="labaModal<%=rnd%>">Rp 0</h4>
                            </div>
                        </div>
                    </div>
                    
                    <div class="col-md-3 col-sm-6">
                        <div class="card border-0 shadow-sm rounded-3 h-100 bg-success bg-gradient text-white">
                            <div class="card-body p-3">
                                <div class="d-flex justify-content-between align-items-center mb-2">
                                    <span class="text-white-50 small fw-bold text-uppercase"><%=Common.getBahasaConfig("Laba Kotor")%></span>
                                    <div class="bg-white bg-opacity-25 text-white rounded-circle p-2"><i class="fas fa-wallet fa-fw"></i></div>
                                </div>
                                <h4 class="fw-bold text-white mb-0" id="labaKotor<%=rnd%>">Rp 0</h4>
                            </div>
                        </div>
                    </div>
                    
                    <div class="col-md-3 col-sm-6">
                        <div class="card border-0 shadow-sm rounded-3 h-100">
                            <div class="card-body p-3">
                                <div class="d-flex justify-content-between align-items-center mb-2">
                                    <span class="text-muted small fw-bold text-uppercase"><%=Common.getBahasaConfig("Margin Laba")%></span>
                                    <div class="bg-info bg-opacity-10 text-info rounded-circle p-2"><i class="fas fa-percentage fa-fw"></i></div>
                                </div>
                                <h4 class="fw-bold text-dark mb-0" id="labaMargin<%=rnd%>">0%</h4>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="card border-0 shadow-sm rounded-3">
                    <div class="card-body p-4">
                        <h6 class="fw-bold text-secondary mb-3"><i class="fas fa-chart-bar me-2"></i><%=Common.getBahasaConfig("Tren Fluktuasi Laba Harian")%></h6>
                        <div id="loadingChartLaba<%=rnd%>" class="text-center py-5">
                            <div class="spinner-border text-success mb-3" role="status"></div><br>
                            <span class="fw-medium text-muted"><%=Common.getBahasaConfig("Menyiapkan grafik laba...")%></span>
                        </div>
                        <div id="containerChartLaba<%=rnd%>" style="position: relative; height: 300px; display: none;">
                            <canvas id="chartLabaKotor<%=rnd%>"></canvas>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    </div>
</div>

<script>
    let myChartLaba<%=rnd%> = null;
    const filterTokoSQLLaba<%=rnd%> = "<%=sqlFilterToko%>";
    const isLoginPedagangLaba<%=rnd%> = <%= toko != null %>;

    const formatUangLaba<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);
    };

    const sqlLaba<%=rnd%> = async (sql) => {
        try {
            const r = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ sql: sql, action: "sql" }) });
            const j = await r.json();
            return j.data || [];
        } catch (e) { return []; }
    };

    // Peta HPP per unit tiap produk: kalau produk ber-resep (ada bahanbaku), HPP = rollup resep
    // (qty x harga tiap bahan baku, sama seperti panel Resep & HPP); kalau tidak, HPP = hargabeli
    // langsung (produk beli-jadi yang memang dibeli seharga itu, mis. minuman kemasan titipan).
    // Memakai hargabeli produk jadi utk SEMUA produk (kode lama) salah utk menu racikan karena
    // menu racikan umumnya tak punya hargabeli sendiri -- ia dibuat dari bahan, bukan dibeli jadi.
    const petaHargaPokok<%=rnd%> = async () => {
        const semua = await sqlLaba<%=rnd%>("SELECT id, COALESCE(bahanbaku,'') AS resep, COALESCE(hargabeli,0) AS beli "
            + "FROM koperasi.produk WHERE 1=1<%=sqlFilterTokoProduk%>");
        const perluResep = [];
        const idBahan = new Set();
        const peta = {};
        semua.forEach(r => {
            const id = parseInt(r.id);
            const beli = parseFloat(r.beli) || 0;
            let items = null;
            const resepTxt = (r.resep || '').trim();
            if (resepTxt !== '' && resepTxt !== '[]') {
                try { const parsedItems = JSON.parse(resepTxt); if (Array.isArray(parsedItems) && parsedItems.length > 0) items = parsedItems; } catch (e) { items = null; }
            }
            if (items) {
                perluResep.push({ id, items });
                items.forEach(it => { const pid = parseInt(it.produk); if (pid) idBahan.add(pid); });
            } else {
                peta[id] = beli;
            }
        });
        let hargaBahan = {};
        if (idBahan.size > 0) {
            const ids = Array.from(idBahan).filter(x => Number.isFinite(x)).join(',');
            if (ids) {
                const hb = await sqlLaba<%=rnd%>("SELECT id, COALESCE(hargabeli,0) AS hargabeli FROM koperasi.produk WHERE id IN (" + ids + ")");
                hb.forEach(r => { hargaBahan[parseInt(r.id)] = parseFloat(r.hargabeli) || 0; });
            }
        }
        perluResep.forEach(p => {
            let hpp = 0;
            p.items.forEach(it => { hpp += (parseFloat(it.qty) || 0) * (hargaBahan[parseInt(it.produk)] || 0); });
            peta[p.id] = hpp;
        });
        return peta;
    };

    const loadAnalisisLaba<%=rnd%> = async () => {
        const btnRefresh = document.getElementById('btnRefreshLaba<%=rnd%>');
        btnRefresh.disabled = true;
        
        document.getElementById('loadingChartLaba<%=rnd%>').style.display = 'block';
        document.getElementById('containerChartLaba<%=rnd%>').style.display = 'none';

        // MENGAMBIL NILAI FILTER
        const periodeInterval = document.getElementById('filterPeriodeLaba<%=rnd%>').value;
        const searchProduk = document.getElementById('searchProdukLaba<%=rnd%>').value.trim();
        
        let searchPedagang = "";
        const elPedagang = document.getElementById('searchPedagangLaba<%=rnd%>');
        if (elPedagang) {
            searchPedagang = elPedagang.value.trim();
        }

        // MEMBANGUN QUERY FILTER PENCARIAN
        let filterPencarianSQL = "";
        if (searchPedagang !== "" && !isLoginPedagangLaba<%=rnd%>) {
            const safeSearchPedagang = searchPedagang.replace(/'/g, "''");
            // INNER JOIN dengan tabel toko ada di 'b'
            filterPencarianSQL += " AND b.nama ILIKE '%" + safeSearchPedagang + "%' ";
        }
        if (searchProduk !== "") {
            const safeSearchProduk = searchProduk.replace(/'/g, "''");
            filterPencarianSQL += " AND c.nama ILIKE '%" + safeSearchProduk + "%' ";
        }

        // Kueri detail per (hari, produk) -- BUKAN pakai c.hargabeli langsung, sebab produk racikan
        // umumnya tak punya hargabeli sendiri. Modal dihitung di JS via petaHargaPokok<%=rnd%>()
        // (rollup resep utk produk ber-resep, hargabeli utk sisanya), lalu dijumlah per hari sekaligus
        // per total -- satu kueri melayani kartu ringkasan DAN grafik tren (tak perlu dua kueri terpisah).
        const sqlDetail = "SELECT DATE(a.waktu) AS tgl, TO_CHAR(DATE(a.waktu), 'DD Mon') AS tanggal, " +
            "a.produk AS produk_id, COALESCE(SUM(a.total), 0) AS omzet, COALESCE(SUM(a.qty), 0) AS qty " +
            "FROM koperasi.pembelian a " +
            "INNER JOIN koperasi.produk c ON a.produk = c.id " +
            "INNER JOIN koperasi.toko b ON a.toko = b.id " +
            "WHERE DATE(a.waktu) >= CURRENT_DATE - INTERVAL '" + periodeInterval + "' " +
            filterTokoSQLLaba<%=rnd%> + filterPencarianSQL +
            "GROUP BY DATE(a.waktu), a.produk " +
            "ORDER BY DATE(a.waktu) ASC;";

        try {
            const [peta, detailRows] = await Promise.all([
                petaHargaPokok<%=rnd%>(),
                sqlLaba<%=rnd%>(sqlDetail)
            ]);

            let totalOmzet = 0, totalModal = 0;
            const harian = new Map(); // key = tanggal ISO (unik walau lintas tahun) -> {label, omzet, modal}
            detailRows.forEach(r => {
                const tgl = r.tgl;
                const omzet = parseFloat(r.omzet) || 0;
                const qty = parseFloat(r.qty) || 0;
                const cost = peta[parseInt(r.produk_id)] || 0;
                const modal = qty * cost;
                totalOmzet += omzet;
                totalModal += modal;
                if (!harian.has(tgl)) harian.set(tgl, { label: r.tanggal, omzet: 0, modal: 0 });
                const acc = harian.get(tgl);
                acc.omzet += omzet;
                acc.modal += modal;
            });

            // -- SET DATA KARTU RINGKASAN --
            const laba = totalOmzet - totalModal;
            let margin = 0;
            if (totalOmzet > 0) {
                margin = (laba / totalOmzet) * 100;
            }

            document.getElementById('labaOmzet<%=rnd%>').innerText = formatUangLaba<%=rnd%>(totalOmzet);
            document.getElementById('labaModal<%=rnd%>').innerText = formatUangLaba<%=rnd%>(totalModal);
            document.getElementById('labaKotor<%=rnd%>').innerText = formatUangLaba<%=rnd%>(laba);

            // Beri warna merah jika margin minus (rugi)
            const elMargin = document.getElementById('labaMargin<%=rnd%>');
            elMargin.innerText = margin.toFixed(2) + '%';
            if (margin < 0) {
                elMargin.classList.replace('text-dark', 'text-danger');
            } else {
                elMargin.classList.replace('text-danger', 'text-dark');
            }

            // -- SET DATA GRAFIK BATANG --
            const labels = [];
            const dataPoin = [];
            harian.forEach(acc => {
                labels.push(acc.label);
                dataPoin.push(acc.omzet - acc.modal);
            });

            renderChartLaba<%=rnd%>(labels, dataPoin);

        } catch (error) {
            console.error("Gagal mengambil data laba:", error);
            document.getElementById('loadingChartLaba<%=rnd%>').innerHTML = '<div class="text-danger"><i class="fas fa-exclamation-triangle fa-2x mb-2"></i><br><%=Common.getBahasaConfig("Gagal memuat grafik laba. Pastikan kolom sudah ada di tabel Produk.")%></div>';
        } finally {
            btnRefresh.disabled = false;
        }
    };

    const renderChartLaba<%=rnd%> = (labels, data) => {
        document.getElementById('loadingChartLaba<%=rnd%>').style.display = 'none';
        document.getElementById('containerChartLaba<%=rnd%>').style.display = 'block';

        const ctx = document.getElementById('chartLabaKotor<%=rnd%>').getContext('2d');
        if (myChartLaba<%=rnd%> !== null) {
            myChartLaba<%=rnd%>.destroy();
        }

        // Logic Warna: Jika Laba Minus (Rugi) warna jadi merah, jika untung warna hijau
        const bgColors = data.map(val => val < 0 ? 'rgba(220, 53, 69, 0.8)' : 'rgba(25, 135, 84, 0.8)');
        const borderColors = data.map(val => val < 0 ? 'rgba(220, 53, 69, 1)' : 'rgba(25, 135, 84, 1)');

        myChartLaba<%=rnd%> = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: '<%=Common.getBahasaConfigJS("Laba Kotor (Rp)")%>',
                    data: data,
                    backgroundColor: bgColors,
                    borderColor: borderColors,
                    borderWidth: 1,
                    borderRadius: 6, // Ujung bar membulat (modern UI)
                    barPercentage: 0.6
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
                        cornerRadius: 8,
                        callbacks: {
                            label: function(context) {
                                return formatUangLaba<%=rnd%>(context.raw);
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        grid: { display: false },
                        ticks: { font: { size: 12, family: "'Nunito', sans-serif" }, color: '#6c757d' }
                    },
                    y: {
                        beginAtZero: true,
                        grid: { borderDash: [5, 5], color: '#e9ecef' },
                        ticks: {
                            font: { family: "'Nunito', sans-serif" },
                            color: '#6c757d',
                            callback: function(value) {
                                // Memformat sumbu Y menjadi format K / Juta agar tidak terlalu panjang
                                if (value >= 1000000 || value <= -1000000) {
                                    return (value / 1000000) + 'M';
                                } else if (value >= 1000 || value <= -1000) {
                                    return (value / 1000) + 'K';
                                }
                                return value;
                            }
                        }
                    }
                }
            }
        });
    };

    // Auto-load saat halaman siap
    document.addEventListener("DOMContentLoaded", () => {
        const filterDropdown = document.getElementById('filterPeriodeLaba<%=rnd%>');
        
        // Auto-refresh saat ganti dropdown hari
        filterDropdown.addEventListener("change", () => {
            loadAnalisisLaba<%=rnd%>();
        });

        // Trigger Pencarian dengan tombol Enter
        const filterInputs = [
            document.getElementById('searchProdukLaba<%=rnd%>'),
            document.getElementById('searchPedagangLaba<%=rnd%>')
        ];
        
        filterInputs.forEach(input => {
            if(input) {
                input.addEventListener("keydown", (event) => {
                    if (event.key === "Enter") {
                        event.preventDefault();
                        loadAnalisisLaba<%=rnd%>();
                    }
                });
            }
        });

        loadAnalisisLaba<%=rnd%>();
    });
</script>