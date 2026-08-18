<%@page import="ais.database.model.Konfigurasi"%>
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

Boolean tampilkanRingkasanPenjualan = Common.getKonfigurasi("tampilkan_ringkasan_penjualan_ekantin", Konfigurasi.AKTIF).getNilai().equalsIgnoreCase(Konfigurasi.AKTIF);  

// Menyiapkan variabel filter SQL jika user adalah pedagang
String sqlFilterToko = "";
if (toko != null) {
    // Karena kolom toko di tabel pembelian diasumsikan menggunakan ID toko
    sqlFilterToko = " AND toko = " + toko.getId() + " ";
}
%>

<% if (tampilkanRingkasanPenjualan) { %>
<div class="row g-3 mb-4 animate__animated animate__fadeInDown" id="ringkasanPenjualanContainer<%=rnd%>">
    
    <div class="col-12 d-flex justify-content-between align-items-end mb-1">
        <h5 class="fw-bolder text-dark mb-0">
            <i class="fas fa-chart-line text-primary me-2"></i><%=Common.getBahasaConfig("Ringkasan Penjualan")%>
        </h5>
        <small class="text-muted fw-semibold">
            <i class="fas fa-clock me-1"></i><%=Common.getBahasaConfig("Diperbarui:")%> <span id="lastUpdate<%=rnd%>"><i class="fas fa-spinner fa-spin"></i></span>
        </small>
    </div>

    <!-- Hari Ini -->
    <div class="col-sm-6 col-xl-3">
        <div class="card border-0 shadow-sm rounded-4 h-100 border-start border-primary border-4">
            <div class="card-body p-3">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <span class="text-muted fw-bold small text-uppercase"><%=Common.getBahasaConfig("Hari Ini")%></span>
                    <div class="bg-primary bg-opacity-10 text-primary rounded-circle d-flex align-items-center justify-content-center" style="width: 32px; height: 32px;">
                        <i class="fas fa-calendar-day"></i>
                    </div>
                </div>
                <h4 class="fw-bolder text-dark mb-1" id="dailyTotalRp<%=rnd%>"><i class="fas fa-spinner fa-spin fs-6 text-muted"></i></h4>
                <div class="d-flex align-items-center gap-2 mt-2">
                    <span class="badge bg-light text-dark border shadow-sm"><i class="fas fa-receipt text-secondary me-1"></i><span id="dailyTrx<%=rnd%>">0</span> <%=Common.getBahasaConfig("Trx")%></span>
                    <% if (toko == null) { %><span class="badge bg-light text-dark border shadow-sm"><i class="fas fa-store text-secondary me-1"></i><span id="dailyPedagang<%=rnd%>">0</span> <%=Common.getBahasaConfig("Toko")%></span><% } %>
                </div>
            </div>
        </div>
    </div>

    <!-- Minggu Ini -->
    <div class="col-sm-6 col-xl-3">
        <div class="card border-0 shadow-sm rounded-4 h-100 border-start border-success border-4">
            <div class="card-body p-3">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <span class="text-muted fw-bold small text-uppercase"><%=Common.getBahasaConfig("Minggu Ini")%></span>
                    <div class="bg-success bg-opacity-10 text-success rounded-circle d-flex align-items-center justify-content-center" style="width: 32px; height: 32px;">
                        <i class="fas fa-calendar-week"></i>
                    </div>
                </div>
                <h4 class="fw-bolder text-dark mb-1" id="weeklyTotalRp<%=rnd%>"><i class="fas fa-spinner fa-spin fs-6 text-muted"></i></h4>
                <div class="d-flex align-items-center gap-2 mt-2">
                    <span class="badge bg-light text-dark border shadow-sm"><i class="fas fa-receipt text-secondary me-1"></i><span id="weeklyTrx<%=rnd%>">0</span> <%=Common.getBahasaConfig("Trx")%></span>
                    <% if (toko == null) { %><span class="badge bg-light text-dark border shadow-sm"><i class="fas fa-store text-secondary me-1"></i><span id="weeklyPedagang<%=rnd%>">0</span> <%=Common.getBahasaConfig("Toko")%></span><% } %>
                </div>
            </div>
        </div>
    </div>

    <!-- Bulan Ini -->
    <div class="col-sm-6 col-xl-3">
        <div class="card border-0 shadow-sm rounded-4 h-100 border-start border-warning border-4">
            <div class="card-body p-3">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <span class="text-muted fw-bold small text-uppercase"><%=Common.getBahasaConfig("Bulan Ini")%></span>
                    <div class="bg-warning bg-opacity-10 text-warning rounded-circle d-flex align-items-center justify-content-center" style="width: 32px; height: 32px;">
                        <i class="fas fa-calendar-alt"></i>
                    </div>
                </div>
                <h4 class="fw-bolder text-dark mb-1" id="monthlyTotalRp<%=rnd%>"><i class="fas fa-spinner fa-spin fs-6 text-muted"></i></h4>
                <div class="d-flex align-items-center gap-2 mt-2">
                    <span class="badge bg-light text-dark border shadow-sm"><i class="fas fa-receipt text-secondary me-1"></i><span id="monthlyTrx<%=rnd%>">0</span> <%=Common.getBahasaConfig("Trx")%></span>
                    <% if (toko == null) { %><span class="badge bg-light text-dark border shadow-sm"><i class="fas fa-store text-secondary me-1"></i><span id="monthlyPedagang<%=rnd%>">0</span> <%=Common.getBahasaConfig("Toko")%></span><% } %>
                </div>
            </div>
        </div>
    </div>

    <!-- Semester Ini -->
    <div class="col-sm-6 col-xl-3">
        <div class="card border-0 shadow-sm rounded-4 h-100 border-start border-danger border-4">
            <div class="card-body p-3">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <span class="text-muted fw-bold small text-uppercase"><%=Common.getBahasaConfig("Semester Ini")%></span>
                    <div class="bg-danger bg-opacity-10 text-danger rounded-circle d-flex align-items-center justify-content-center" style="width: 32px; height: 32px;">
                        <i class="fas fa-calendar"></i>
                    </div>
                </div>
                <h4 class="fw-bolder text-dark mb-1" id="semesterTotalRp<%=rnd%>"><i class="fas fa-spinner fa-spin fs-6 text-muted"></i></h4>
                <div class="d-flex align-items-center gap-2 mt-2">
                    <span class="badge bg-light text-dark border shadow-sm"><i class="fas fa-receipt text-secondary me-1"></i><span id="semesterTrx<%=rnd%>">0</span> <%=Common.getBahasaConfig("Trx")%></span>
                    <% if (toko == null) { %><span class="badge bg-light text-dark border shadow-sm"><i class="fas fa-store text-secondary me-1"></i><span id="semesterPedagang<%=rnd%>">0</span> <%=Common.getBahasaConfig("Toko")%></span><% } %>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    const formatRupiahHeader<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);
    };
    
    const formatAngkaBiasa<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID').format(angka || 0);
    };

    const loadRingkasanPenjualan<%=rnd%> = async () => {
        let updateText = '<%=Common.getBahasaConfigJS("Memperbarui...")%>';
        document.getElementById('lastUpdate<%=rnd%>').innerHTML = '<i class="fas fa-spinner fa-spin"></i>';

        const sqlQuery = "WITH stats AS ( " +
            "SELECT " +
            "  SUM(CASE WHEN DATE(waktu) = CURRENT_DATE THEN total ELSE 0 END) AS hari_ini_rp, " +
            "  COUNT(DISTINCT CASE WHEN DATE(waktu) = CURRENT_DATE THEN id END) AS hari_ini_trx, " +
            "  COUNT(DISTINCT CASE WHEN DATE(waktu) = CURRENT_DATE THEN toko END) AS hari_ini_pedagang, " +
            
            "  SUM(CASE WHEN DATE(waktu) >= DATE_TRUNC('week', CURRENT_DATE) THEN total ELSE 0 END) AS minggu_ini_rp, " +
            "  COUNT(DISTINCT CASE WHEN DATE(waktu) >= DATE_TRUNC('week', CURRENT_DATE) THEN id END) AS minggu_ini_trx, " +
            "  COUNT(DISTINCT CASE WHEN DATE(waktu) >= DATE_TRUNC('week', CURRENT_DATE) THEN toko END) AS minggu_ini_pedagang, " +
            
            "  SUM(CASE WHEN DATE(waktu) >= DATE_TRUNC('month', CURRENT_DATE) THEN total ELSE 0 END) AS bulan_ini_rp, " +
            "  COUNT(DISTINCT CASE WHEN DATE(waktu) >= DATE_TRUNC('month', CURRENT_DATE) THEN id END) AS bulan_ini_trx, " +
            "  COUNT(DISTINCT CASE WHEN DATE(waktu) >= DATE_TRUNC('month', CURRENT_DATE) THEN toko END) AS bulan_ini_pedagang, " +
            
            "  SUM(CASE WHEN waktu >= NOW() - INTERVAL '6 months' THEN total ELSE 0 END) AS semester_ini_rp, " +
            "  COUNT(DISTINCT CASE WHEN waktu >= NOW() - INTERVAL '6 months' THEN id END) AS semester_ini_trx, " +
            "  COUNT(DISTINCT CASE WHEN waktu >= NOW() - INTERVAL '6 months' THEN toko END) AS semester_ini_pedagang, " +
            
            "  MAX(waktu) AS waktu_terakhir " +
            "FROM koperasi.pembelian WHERE aktif = true <%=sqlFilterToko%> " +
        ") SELECT * FROM stats;";

        try {
            // Kita gunakan blok ini karena posTanpaLogin umumnya di defined di form utama kasir (jika ada),
            // Jika tidak ada parameter tersebut di scope global, kita abaikan pengirimannya
            let payloadData = { sql: sqlQuery, action: "sql" };
            if (typeof posTanpaLogin<%=rnd%> !== 'undefined' && posTanpaLogin<%=rnd%>) {
                payloadData.tanpaLogin = "true";
            }

            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payloadData)
            });
            
            const result = await response.json();
            
            if (result.data && result.data.length > 0) {
                const data = result.data[0];
                
                document.getElementById('dailyTotalRp<%=rnd%>').innerText = formatRupiahHeader<%=rnd%>(data.hari_ini_rp);
                document.getElementById('weeklyTotalRp<%=rnd%>').innerText = formatRupiahHeader<%=rnd%>(data.minggu_ini_rp);
                document.getElementById('monthlyTotalRp<%=rnd%>').innerText = formatRupiahHeader<%=rnd%>(data.bulan_ini_rp);
                document.getElementById('semesterTotalRp<%=rnd%>').innerText = formatRupiahHeader<%=rnd%>(data.semester_ini_rp);
                
                document.getElementById('dailyTrx<%=rnd%>').innerText = formatAngkaBiasa<%=rnd%>(data.hari_ini_trx);
                document.getElementById('weeklyTrx<%=rnd%>').innerText = formatAngkaBiasa<%=rnd%>(data.minggu_ini_trx);
                document.getElementById('monthlyTrx<%=rnd%>').innerText = formatAngkaBiasa<%=rnd%>(data.bulan_ini_trx);
                document.getElementById('semesterTrx<%=rnd%>').innerText = formatAngkaBiasa<%=rnd%>(data.semester_ini_trx);
                
                <% if (toko == null) { %>
                document.getElementById('dailyPedagang<%=rnd%>').innerText = formatAngkaBiasa<%=rnd%>(data.hari_ini_pedagang);
                document.getElementById('weeklyPedagang<%=rnd%>').innerText = formatAngkaBiasa<%=rnd%>(data.minggu_ini_pedagang);
                document.getElementById('monthlyPedagang<%=rnd%>').innerText = formatAngkaBiasa<%=rnd%>(data.bulan_ini_pedagang);
                document.getElementById('semesterPedagang<%=rnd%>').innerText = formatAngkaBiasa<%=rnd%>(data.semester_ini_pedagang);
                <% } %>
                
                if (data.waktu_terakhir) {
                    const dateObj = new Date(data.waktu_terakhir);
                    updateText = dateObj.toLocaleString('id-ID', { 
                        year: 'numeric', month: '2-digit', day: '2-digit', 
                        hour: '2-digit', minute: '2-digit', second: '2-digit' 
                    });
                } else {
                    updateText = '<%=Common.getBahasaConfigJS("Belum Ada Transaksi")%>';
                }
            }
        } catch (error) {
            console.error("Gagal mengambil data ringkasan:", error);
            document.getElementById('dailyTotalRp<%=rnd%>').innerText = 'Rp 0';
            document.getElementById('weeklyTotalRp<%=rnd%>').innerText = 'Rp 0';
            document.getElementById('monthlyTotalRp<%=rnd%>').innerText = 'Rp 0';
            document.getElementById('semesterTotalRp<%=rnd%>').innerText = 'Rp 0';
            updateText = '<%=Common.getBahasaConfigJS("Gagal Memuat")%>';
        } finally {
            document.getElementById('lastUpdate<%=rnd%>').innerText = updateText;
        }
    };

    document.addEventListener("DOMContentLoaded", () => {
        loadRingkasanPenjualan<%=rnd%>();
    });
</script>
<% } %>