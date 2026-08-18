<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.sekolah.Yayasan"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.*"%>
<%@page import="java.util.*"%>

<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        out.print("<div class='alert alert-danger'>Sesi Anda telah berakhir.</div>");
        return;
    }

    String paramHanyaSiswa = request.getParameter("hanya_untuk_siswa");
    boolean isParamHanyaSiswaSet = paramHanyaSiswa != null && !paramHanyaSiswa.trim().isEmpty();
    boolean valHanyaSiswa = "true".equalsIgnoreCase(paramHanyaSiswa);

    String rndDsh = Common.getGeneratedBarCode(8);

    List<Sekolah> listSekolahDsh = new ArrayList<Sekolah>();
    List<Yayasan> listYayasanDsh = new ArrayList<Yayasan>();
    Session sessFilterDsh = HibernateUtil.openSession();
    try {
        org.hibernate.Criteria cSek = sessFilterDsh.createCriteria(Sekolah.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama"));
        if(tbmuser.ambilSekolah() != null) cSek.add(Restrictions.eq("id", tbmuser.ambilSekolah().getId()));
        else if(tbmuser.ambilYayasan() != null) cSek.add(Restrictions.eq("yayasan.id", tbmuser.ambilYayasan().getId()));
        listSekolahDsh = ConstantValues.simpleList(cSek, Sekolah.class);
        org.hibernate.Criteria cYay = sessFilterDsh.createCriteria(Yayasan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama"));
        if(tbmuser.ambilYayasan() != null) cYay.add(Restrictions.eq("id", tbmuser.ambilYayasan().getId()));
        listYayasanDsh = ConstantValues.simpleList(cYay, Yayasan.class);
    } finally {
        if(sessFilterDsh != null) sessFilterDsh.close();
    }
%>

<div class="card border-0 shadow-sm rounded-4 bg-white animate__animated animate__fadeIn">
    <div class="card-header bg-white border-bottom p-4 d-flex justify-content-between align-items-center">
        <h5 class="fw-bold text-dark mb-0"><i class="fas fa-chart-pie me-2 text-primary"></i><%=Common.getBahasaConfig("Dasbor & Tren Pembayaran")%></h5>
        <button class="btn btn-success btn-sm fw-bold shadow-sm" onclick="exportDasborExcel<%=rndDsh%>()">
            <i class="fas fa-file-excel me-2"></i><%=Common.getBahasaConfig("Export Rekap Excel")%>
        </button>
    </div>
    
    <div class="card-body bg-light border-bottom p-4">
        <div class="row g-3">
            <div class="col-md-3">
                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Yayasan")%></label>
                <select class="form-select shadow-sm" id="dshYayasan<%=rndDsh%>" onchange="filterDshYayasan<%=rndDsh%>()">
                    <option value="0"><%=Common.getBahasaConfig("-- Semua Yayasan --")%></option>
                    <% for(Yayasan y : listYayasanDsh) { %>
                        <option value="<%=y.getId()%>"><%=y.getNama()%></option>
                    <% } %>
                </select>
            </div>
            <div class="col-md-3">
                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Sekolah")%></label>
                <select class="form-select shadow-sm" id="dshSekolah<%=rndDsh%>">
                    <option value="0"><%=Common.getBahasaConfig("-- Semua Sekolah --")%></option>
                    <% for(Sekolah s : listSekolahDsh) { %>
                        <option value="<%=s.getId()%>"><%=s.getNama()%></option>
                    <% } %>
                </select>
            </div>
            
            <% if(isParamHanyaSiswaSet) { %>
                <input type="hidden" id="dshKategori<%=rndDsh%>" value="<%=valHanyaSiswa%>">
            <% } else { %>
                <div class="col-md-2">
                    <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Kategori")%></label>
                    <select class="form-select shadow-sm" id="dshKategori<%=rndDsh%>">
                        <option value="true"><%=Common.getBahasaConfig("Siswa Aktif")%></option>
                        <option value="false"><%=Common.getBahasaConfig("Calon Siswa")%></option>
                    </select>
                </div>
            <% } %>
            
            <div class="col-md-4">
                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Cari Pelanggan (NIS / Nama)")%></label>
                <input type="text" class="form-control shadow-sm filter-enter-<%=rndDsh%>" id="dshPelanggan<%=rndDsh%>" placeholder="<%=Common.getBahasaConfig("Ketik di sini...")%>">
            </div>
            
            <div class="col-md-3">
                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Cari Item Biaya")%></label>
                <input type="text" class="form-control shadow-sm filter-enter-<%=rndDsh%>" id="dshItem<%=rndDsh%>" placeholder="<%=Common.getBahasaConfig("Nama / Kode Biaya...")%>">
            </div>
            <div class="col-md-3">
                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Mulai Transaksi")%></label>
                <input type="date" class="form-control shadow-sm filter-enter-<%=rndDsh%>" id="dshStart<%=rndDsh%>">
            </div>
            <div class="col-md-3">
                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Sampai Transaksi")%></label>
                <input type="date" class="form-control shadow-sm filter-enter-<%=rndDsh%>" id="dshEnd<%=rndDsh%>">
            </div>
            <div class="col-md-3 d-flex align-items-end gap-2">
                <button class="btn btn-light border fw-bold shadow-sm w-50" onclick="resetFilterDsh<%=rndDsh%>()"><i class="fas fa-sync-alt"></i></button>
                <button class="btn btn-primary fw-bold shadow-sm w-100" onclick="applyFilterDsh<%=rndDsh%>()"><i class="fas fa-search me-2"></i>Cari</button>
            </div>
        </div>
    </div>

    <div class="card-body p-4" id="containerDasborGlobal<%=rndDsh%>">
        <div class="row mb-4">
            <div class="col-12">
                <div class="card bg-primary bg-opacity-10 border-0 shadow-sm rounded-4 p-4 text-center">
                    <h5 class="fw-bold text-primary mb-2"><i class="fas fa-money-check-alt me-2"></i><%=Common.getBahasaConfig("Total Pendapatan Terfilter")%></h5>
                    <h2 class="fw-bolder text-dark mb-0" id="dshTotalNominal<%=rndDsh%>">Rp 0</h2>
                </div>
            </div>
        </div>

        <div class="row g-4">
            <div class="col-lg-12">
                <div class="card border shadow-sm rounded-4 h-100">
                    <div class="card-header bg-white border-bottom-0 pt-3 pb-0">
                        <h6 class="fw-bold text-secondary text-center"><i class="fas fa-chart-line me-2"></i><%=Common.getBahasaConfig("Tren Pendapatan Bulanan")%></h6>
                    </div>
                    <div class="card-body p-4" style="height: 300px;">
                        <canvas id="chartDshTrend<%=rndDsh%>"></canvas>
                    </div>
                </div>
            </div>
            <div class="col-lg-6">
                <div class="card border shadow-sm rounded-4 h-100">
                    <div class="card-header bg-white border-bottom-0 pt-3 pb-0">
                        <h6 class="fw-bold text-secondary text-center"><i class="fas fa-wallet me-2"></i><%=Common.getBahasaConfig("Komposisi Metode Pembayaran")%></h6>
                    </div>
                    <div class="card-body p-4" style="height: 300px;">
                        <canvas id="chartDshMetode<%=rndDsh%>"></canvas>
                    </div>
                </div>
            </div>
            <div class="col-lg-6">
                <div class="card border shadow-sm rounded-4 h-100">
                    <div class="card-header bg-white border-bottom-0 pt-3 pb-0">
                        <h6 class="fw-bold text-secondary text-center"><i class="fas fa-school me-2"></i><%=Common.getBahasaConfig("Kontribusi Pendapatan per Sekolah")%></h6>
                    </div>
                    <div class="card-body p-4" style="height: 300px;">
                        <canvas id="chartDshSekolah<%=rndDsh%>"></canvas>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    var isDasborGlobalLoaded = false;
    var dshDataExportCache<%=rndDsh%> = [];
    var chartObjTrend<%=rndDsh%> = null;
    var chartObjMetode<%=rndDsh%> = null;
    var chartObjSekolah<%=rndDsh%> = null;
    var listSekolahLengkapDsh<%=rndDsh%> = [
        <% for(Sekolah s : listSekolahDsh) { %>
            {id: <%=s.getId()%>, nama: '<%=s.getNama().replace("'", "\\'")%>', yayasanId: <%=s.getYayasan() != null ? s.getYayasan().getId() : "null"%>},
        <% } %>
    ];
    var formatRpDsh<%=rndDsh%> = function(angka) {
        return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);
    };

    var filterDshYayasan<%=rndDsh%> = function() {
        const yayasanId = document.getElementById('dshYayasan<%=rndDsh%>').value;
        const selSekolah = document.getElementById('dshSekolah<%=rndDsh%>');
        selSekolah.innerHTML = '<option value="0"><%=Common.getBahasaConfig("-- Semua Sekolah --")%></option>';
        listSekolahLengkapDsh<%=rndDsh%>.forEach(s => {
            if(yayasanId == "0" || s.yayasanId == yayasanId) {
                selSekolah.innerHTML += '<option value="' + s.id + '">' + s.nama + '</option>';
            }
        });
    };

    var initFiltersDsh<%=rndDsh%> = function() {
        let dEnd = new Date();
        let dStart = new Date();
        dStart.setFullYear(dStart.getFullYear() - 1); // Default 1 Tahun Terakhir utk Dashboard agar grafik cantik
        document.getElementById('dshStart<%=rndDsh%>').valueAsDate = dStart;
        document.getElementById('dshEnd<%=rndDsh%>').valueAsDate = dEnd;

        var selYayasan = document.getElementById('dshYayasan<%=rndDsh%>');
        var selSekolah = document.getElementById('dshSekolah<%=rndDsh%>');
        if (selYayasan && selYayasan.options.length === 2) {
            selYayasan.selectedIndex = 1;
            selYayasan.disabled = true;
            filterDshYayasan<%=rndDsh%>();
        }
        if (selSekolah && selSekolah.options.length === 2) {
            selSekolah.selectedIndex = 1;
            selSekolah.disabled = true;
        }
        
        // Setup Enter event
        var inputs = document.querySelectorAll('.filter-enter-<%=rndDsh%>');
        inputs.forEach(function(el) {
            el.addEventListener('keypress', function(e) {
                if (e.key === 'Enter') applyFilterDsh<%=rndDsh%>();
            });
        });
        applyFilterDsh<%=rndDsh%>();
    };

    var applyFilterDsh<%=rndDsh%> = async function() {
        const payload = {
            action: 'loadDashboardGlobal',
            idYayasan: document.getElementById('dshYayasan<%=rndDsh%>').value,
            idSekolah: document.getElementById('dshSekolah<%=rndDsh%>').value,
            isSiswa: (document.getElementById('dshKategori<%=rndDsh%>').value === 'true'),
            keywordPelanggan: document.getElementById('dshPelanggan<%=rndDsh%>').value,
            keywordItem: document.getElementById('dshItem<%=rndDsh%>').value, 
            startDate: document.getElementById('dshStart<%=rndDsh%>').value,
            endDate: document.getElementById('dshEnd<%=rndDsh%>').value
        };
        try {
            const res = await fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=bayar&s=_pembayaran_online_services', {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
            });
            const result = await res.json();
            
            if(result && result.status === '00') {
                dshDataExportCache<%=rndDsh%> = result.exportData || [];
                document.getElementById('dshTotalNominal<%=rndDsh%>').innerText = formatRpDsh<%=rndDsh%>(result.totalNominal);
                renderDashboardCharts<%=rndDsh%>(result.chartTrend, result.chartMetode, result.chartSekolah);
            } else {
                tampilkanToast(result ? result.message : 'Gagal memuat dasbor.', 'bg-danger text-white');
            }
        } catch (e) {
            tampilkanToast('Koneksi terputus.', 'bg-danger text-white');
        }
    };

    var resetFilterDsh<%=rndDsh%> = function() {
        document.getElementById('dshPelanggan<%=rndDsh%>').value = '';
        document.getElementById('dshItem<%=rndDsh%>').value = '';
        
        var selYayasan = document.getElementById('dshYayasan<%=rndDsh%>');
        if(!selYayasan.disabled) selYayasan.value = '0';
        
        var selSekolah = document.getElementById('dshSekolah<%=rndDsh%>');
        if(!selSekolah.disabled) {
            filterDshYayasan<%=rndDsh%>();
            selSekolah.value = '0';
        }
        
        let dEnd = new Date();
        let dStart = new Date();
        dStart.setFullYear(dStart.getFullYear() - 1);
        document.getElementById('dshStart<%=rndDsh%>').valueAsDate = dStart;
        document.getElementById('dshEnd<%=rndDsh%>').valueAsDate = dEnd;

        applyFilterDsh<%=rndDsh%>();
    };
    
    var renderDashboardCharts<%=rndDsh%> = function(trendData, metodeData, sekolahData) {
        if (typeof Chart === 'undefined') return;
        // 1. CHART TREND (BAR)
        var trendLabels = []; var trendVals = [];
        (trendData || []).forEach(function(d){ trendLabels.push(d.label); trendVals.push(d.val); });
        
        if (chartObjTrend<%=rndDsh%>) chartObjTrend<%=rndDsh%>.destroy();
        chartObjTrend<%=rndDsh%> = new Chart(document.getElementById('chartDshTrend<%=rndDsh%>'), {
            type: 'bar',
            data: {
                labels: trendLabels,
                datasets: [{
                    label: 'Pendapatan',
                    data: trendVals,
                    backgroundColor: '#0d6efd',
                    borderRadius: 4
                }]
            },
            options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } }
        });
        
        // 2. CHART METODE (DOUGHNUT)
        var metLabels = []; var metVals = [];
        (metodeData || []).forEach(function(d){ metLabels.push(d.label); metVals.push(d.val); });

        if (chartObjMetode<%=rndDsh%>) chartObjMetode<%=rndDsh%>.destroy();
        chartObjMetode<%=rndDsh%> = new Chart(document.getElementById('chartDshMetode<%=rndDsh%>'), {
            type: 'doughnut',
            data: {
                labels: metLabels,
                datasets: [{
                    data: metVals,
                    backgroundColor: ['#198754', '#ffc107', '#0dcaf0', '#dc3545', '#6c757d', '#0d6efd'],
                    borderWidth: 0
                }]
            },
            options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'right' } } }
        });

        // 3. CHART SEKOLAH (PIE)
        var sekLabels = [];
        var sekVals = [];
        (sekolahData || []).forEach(function(d){ sekLabels.push(d.label); sekVals.push(d.val); });

        if (chartObjSekolah<%=rndDsh%>) chartObjSekolah<%=rndDsh%>.destroy();
        chartObjSekolah<%=rndDsh%> = new Chart(document.getElementById('chartDshSekolah<%=rndDsh%>'), {
            type: 'pie',
            data: {
                labels: sekLabels,
                datasets: [{
                    data: sekVals,
                    backgroundColor: ['#ffc107', '#0d6efd', '#dc3545', '#198754', '#0dcaf0', '#6c757d'],
                    borderWidth: 0
                }]
            },
            options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'right' } } }
        });
    };

    var exportDasborExcel<%=rndDsh%> = function() {
        if (!dshDataExportCache<%=rndDsh%> || dshDataExportCache<%=rndDsh%>.length === 0) {
            tampilkanToast('Tidak ada data untuk diekspor.', 'bg-warning text-dark');
            return;
        }
        
        var csv = '\uFEFF';
        csv += 'Tanggal,Sekolah,Siswa/Pelanggan,Metode,Item Biaya,Nominal\n';
        
        dshDataExportCache<%=rndDsh%>.forEach(function(row) {
            var mtd = '"' + String(row.Metode || '').replace(/"/g, '""') + '"';
            var skl = '"' + String(row.Sekolah || '').replace(/"/g, '""') + '"';
            var itm = '"' + String(row['Item Biaya'] || '').replace(/"/g, '""') + '"';
            
            csv += row.Tanggal + ',' + skl + ',-,' + mtd + ',' + itm + ',' + row.Nominal + '\n';
        });

        var blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        var link = document.createElement("a");
        var url = URL.createObjectURL(blob);
        link.setAttribute("href", url);
        link.setAttribute("download", "Dasbor_Rekap_Pembayaran.csv");
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    };

    // FUNGSI INI TELAH DISESUAIKAN NAMANYA DENGAN YANG DIPANGGIL DI index.jsp
    window.triggerLazyLoadDashboardGlobal = function() {
        if(!isDasborGlobalLoaded) {
            initFiltersDsh<%=rndDsh%>();
            isDasborGlobalLoaded = true;
        }
    };

    // Fallback: Jika tab ini secara bawaan (default) sudah dalam keadaan aktif saat pertama kali dimuat,
    // maka kita eksekusi fungsinya secara otomatis tanpa menunggu event klik tab.
    setTimeout(function() {
        var myContainer = document.getElementById('containerDasborGlobal<%=rndDsh%>');
        if (myContainer) {
            var pane = myContainer.closest('.tab-pane');
            if (!pane || pane.classList.contains('active') || pane.classList.contains('show')) {
                if (typeof window.triggerLazyLoadDashboardGlobal === 'function') {
                    window.triggerLazyLoadDashboardGlobal();
                }
            }
        }
    }, 300);

</script>