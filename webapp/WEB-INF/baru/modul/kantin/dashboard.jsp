<%@page import="ais.common.Common"%>
<%
String rndDash = Common.getGeneratedBarCode(5);
%>

<div class="mb-4">
    <ul class="nav nav-pills bg-white p-2 rounded-4 shadow-sm border" id="dashboardSubTabs<%=rndDash%>" role="tablist">
        <li class="nav-item flex-fill text-center" role="presentation">
            <button class="nav-link active w-100 rounded-pill fw-bold" id="tab-overview<%=rndDash%>" data-bs-toggle="pill" data-bs-target="#pane-overview<%=rndDash%>" type="button" role="tab" aria-selected="true">
                <i class="fas fa-home me-2"></i><%=Common.getBahasaConfig("Ringkasan Umum")%>
            </button>
        </li>
        <li class="nav-item flex-fill text-center" role="presentation">
            <button class="nav-link w-100 rounded-pill fw-bold" id="tab-finance<%=rndDash%>" data-bs-toggle="pill" data-bs-target="#pane-finance<%=rndDash%>" type="button" role="tab" aria-selected="false">
                <i class="fas fa-wallet me-2"></i><%=Common.getBahasaConfig("Keuangan & Kinerja")%>
            </button>
        </li>
        <li class="nav-item flex-fill text-center" role="presentation">
            <button class="nav-link w-100 rounded-pill fw-bold" id="tab-inventory<%=rndDash%>" data-bs-toggle="pill" data-bs-target="#pane-inventory<%=rndDash%>" type="button" role="tab" aria-selected="false">
                <i class="fas fa-box-open me-2"></i><%=Common.getBahasaConfig("Produk & Inventaris")%>
            </button>
        </li>
        <li class="nav-item flex-fill text-center" role="presentation">
            <button class="nav-link w-100 rounded-pill fw-bold" id="tab-customer<%=rndDash%>" data-bs-toggle="pill" data-bs-target="#pane-customer<%=rndDash%>" type="button" role="tab" aria-selected="false">
                <i class="fas fa-users me-2"></i><%=Common.getBahasaConfig("Perilaku Pelanggan")%>
            </button>
        </li>
        <li class="nav-item flex-fill text-center" role="presentation">
            <button class="nav-link w-100 rounded-pill fw-bold" id="tab-sales-analysis<%=rndDash%>" data-bs-toggle="pill" data-bs-target="#pane-sales-analysis<%=rndDash%>" type="button" role="tab" aria-selected="false">
                <i class="fas fa-chart-line me-2"></i><%=Common.getBahasaConfig("Analisis Penjualan")%>
            </button>
        </li>
    </ul>
</div>

<div class="tab-content" id="dashboardSubTabsContent<%=rndDash%>">

    <div class="tab-pane fade show active" id="pane-overview<%=rndDash%>" role="tabpanel" aria-labelledby="tab-overview<%=rndDash%>">
        <jsp:include page="/WEB-INF/baru/modul/kantin/_info_transaksi_header.jsp" />
        
        <jsp:include page="/WEB-INF/baru/modul/kantin/_forecast.jsp" />
        
        <jsp:include page="/WEB-INF/baru/modul/kantin/_riwayat_transaksi_terbaru.jsp" />
        
    </div>

    <div class="tab-pane fade" id="pane-finance<%=rndDash%>" role="tabpanel" aria-labelledby="tab-finance<%=rndDash%>">
        <jsp:include page="/WEB-INF/baru/modul/kantin/_analisis_laba.jsp" />

        <jsp:include page="/WEB-INF/baru/modul/kantin/_resep_hpp.jsp" />

        <jsp:include page="/WEB-INF/baru/modul/kantin/_leaderboard.jsp" />
    </div>

    <div class="tab-pane fade" id="pane-inventory<%=rndDash%>" role="tabpanel" aria-labelledby="tab-inventory<%=rndDash%>">
        <jsp:include page="/WEB-INF/baru/modul/kantin/_stok.jsp" />

        <jsp:include page="/WEB-INF/baru/modul/kantin/_bahan_baku.jsp" />

        <jsp:include page="/WEB-INF/baru/modul/kantin/_rekonsiliasi_aset.jsp" />

        <jsp:include page="/WEB-INF/baru/modul/kantin/_produk_terlaris.jsp" />
        
        <div class="row">
            <div class="col-lg-7">
                <jsp:include page="/WEB-INF/baru/modul/kantin/_rekap_produk_terlaris.jsp" />
            </div>
            <div class="col-lg-5">
                <jsp:include page="/WEB-INF/baru/modul/kantin/_produk_paling_kurang_laku.jsp" />
            </div>
        </div>
    </div>

    <div class="tab-pane fade" id="pane-customer<%=rndDash%>" role="tabpanel" aria-labelledby="tab-customer<%=rndDash%>">
        <jsp:include page="/WEB-INF/baru/modul/kantin/_peak_hour.jsp" />
        
        <jsp:include page="/WEB-INF/baru/modul/kantin/_rekap_pelanggan_terloyal.jsp" />
    </div>

    <div class="tab-pane fade" id="pane-sales-analysis<%=rndDash%>" role="tabpanel" aria-labelledby="tab-sales-analysis<%=rndDash%>">
        <jsp:include page="/WEB-INF/baru/modul/kantin/_analisis_riwayat_penjualan.jsp" />
    </div>

</div>
