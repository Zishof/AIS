<%@page import="ais.common.Common"%>
<%
    String rndTab = Common.getGeneratedBarCode(6);
    // Menangkap parameter dari pemanggil URL (jika ada), default = true (Siswa)
    String paramHanyaSiswa = request.getParameter("hanya_untuk_siswa");
    if (paramHanyaSiswa == null || paramHanyaSiswa.trim().isEmpty()) {
        paramHanyaSiswa = "true"; 
    }
%>

<div class="container-fluid py-3">
    <ul class="nav nav-pills nav-fill mb-4 shadow-sm bg-white p-2 rounded-4" id="mainTabs<%=rndTab%>" role="tablist">
        <li class="nav-item" role="presentation">
            <button class="nav-link active fw-bold fs-6 rounded-pill" id="tab-transaksi-<%=rndTab%>" data-bs-toggle="tab" data-bs-target="#pane-transaksi-<%=rndTab%>" type="button" role="tab">
                <i class="fas fa-cash-register me-2"></i><%=Common.getBahasaConfig("Transaksi Tagihan")%>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link fw-bold fs-6 rounded-pill" id="tab-riwayat-global-<%=rndTab%>" data-bs-toggle="tab" data-bs-target="#pane-riwayat-global-<%=rndTab%>" type="button" role="tab">
                <i class="fas fa-list-alt me-2"></i><%=Common.getBahasaConfig("Riwayat Global")%>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link fw-bold fs-6 rounded-pill" id="tab-dashboard-global-<%=rndTab%>" data-bs-toggle="tab" data-bs-target="#pane-dashboard-global-<%=rndTab%>" type="button" role="tab">
                <i class="fas fa-chart-line me-2"></i><%=Common.getBahasaConfig("Dashboard Pembayaran")%>
            </button>
        </li>
    </ul>

    <div class="tab-content" id="mainTabsContent<%=rndTab%>">
        <div class="tab-pane fade show active" id="pane-transaksi-<%=rndTab%>" role="tabpanel">
            <jsp:include page="/WEB-INF/baru/modul/bayar/pembayaran_online.jsp">
                <jsp:param value="<%=paramHanyaSiswa%>" name="hanya_untuk_siswa" />
            </jsp:include>
        </div>

        <div class="tab-pane fade" id="pane-riwayat-global-<%=rndTab%>" role="tabpanel">
            <jsp:include page="/WEB-INF/baru/modul/bayar/riwayat_pembayaran.jsp">
                <jsp:param value="<%=paramHanyaSiswa%>" name="hanya_untuk_siswa" />
            </jsp:include>
        </div>

        <div class="tab-pane fade" id="pane-dashboard-global-<%=rndTab%>" role="tabpanel">
            <jsp:include page="/WEB-INF/baru/modul/bayar/dashboard_pembayaran.jsp">
                <jsp:param value="<%=paramHanyaSiswa%>" name="hanya_untuk_siswa" />
            </jsp:include>
        </div>
    </div>
</div>

<script>
    // Event Listener: Dipicu setiap kali Tab Riwayat Global mulai ditampilkan di layar
    document.getElementById('tab-riwayat-global-<%=rndTab%>').addEventListener('shown.bs.tab', function (e) {
        // triggerLazyLoadRiwayatGlobal adalah fungsi yang kita buat di dalam riwayat_pembayaran.jsp
        if (typeof window.triggerLazyLoadRiwayatGlobal === 'function') {
            window.triggerLazyLoadRiwayatGlobal();
        }
    });

    // Event Listener: Dipicu setiap kali Tab Dashboard Pembayaran mulai ditampilkan di layar
    document.getElementById('tab-dashboard-global-<%=rndTab%>').addEventListener('shown.bs.tab', function (e) {
        // Sama seperti riwayat, Anda bisa mendefinisikan window.triggerLazyLoadDashboardGlobal 
        // di dalam file dashboard_pembayaran.jsp jika ingin datanya di-load secara dinamis.
        if (typeof window.triggerLazyLoadDashboardGlobal === 'function') {
            window.triggerLazyLoadDashboardGlobal();
        }
    });
</script>