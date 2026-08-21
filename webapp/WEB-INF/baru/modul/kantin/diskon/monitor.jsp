<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// SECURITY CHECK
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	out.print("{\"status\":\"error\", \"message\":\"Sesi Anda telah berakhir. Silakan masuk kembali.\"}");
	return;
}
Pedagang pedagang = tbmuser.getPedagang();
Toko toko = pedagang == null ? null : pedagang.getToko();
String rnd = Common.getGeneratedBarCode(7);

boolean isAdmin = (toko == null);
String idTokoAktif = toko != null ? String.valueOf(toko.getId()) : "";
String namaTokoAktif = toko != null ? toko.getNama() : "Semua Toko Koperasi";
%>

<script data-cfasync="false" src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/3.9.1/chart.min.js"></script>

<style>
    .kpi-card-link { transition: transform 0.2s, box-shadow 0.2s; cursor: pointer; }
    .kpi-card-link:hover { transform: translateY(-5px); box-shadow: 0 .5rem 1rem rgba(0,0,0,.15)!important; }
</style>

<div class="container-fluid px-0 animate__animated animate__fadeIn">
    
    <div class="row mb-4 align-items-end">
        <div class="col-md-4">
            <h4 class="fw-bold text-dark mb-1">
                <i class="fas fa-gift text-primary me-2"></i><%=Common.getBahasaConfig("Dashboard Promo & Cashback")%>
            </h4>
            <span class="text-muted small"><%=Common.getBahasaConfig("Monitoring pengeluaran diskon, pemberian cashback, pencairan, dan dampak aturan promo.")%></span>
        </div>
        <div class="col-md-8 text-md-end mt-3 mt-md-0">
            <div class="d-flex justify-content-md-end gap-2 flex-wrap align-items-center">
                
                <div class="input-group input-group-sm shadow-sm" style="width: 170px;">
                    <span class="input-group-text bg-white border-end-0"><i class="fas fa-tags text-muted"></i></span>
                    <select class="form-select border-start-0 fw-bold text-dark" id="filterJenisAnggota<%=rnd%>" onchange="loadDashboardPromo<%=rnd%>()">
                        <option value=""><%=Common.getBahasaConfig("- Semua Anggota -")%></option>
                    </select>
                </div>

                <% if (isAdmin) { %>
                <div class="input-group input-group-sm shadow-sm" style="width: 170px;">
                    <span class="input-group-text bg-white border-end-0"><i class="fas fa-store text-muted"></i></span>
                    <select class="form-select border-start-0 fw-bold text-dark" id="filterToko<%=rnd%>" onchange="loadDashboardPromo<%=rnd%>()">
                        <option value=""><%=Common.getBahasaConfig("- Semua Toko -")%></option>
                    </select>
                </div>
                <% } else { %>
                    <input type="hidden" id="filterToko<%=rnd%>" value="<%=idTokoAktif%>">
                <% } %>

                <!-- Filter Periode Utama -->
                <div class="input-group input-group-sm shadow-sm" style="width: 220px;">
                    <span class="input-group-text bg-white border-end-0"><i class="fas fa-calendar-alt text-muted"></i></span>
                    <select class="form-select border-start-0 fw-bold text-primary" id="filterPeriode<%=rnd%>" onchange="toggleCustomDate<%=rnd%>(); loadDashboardPromo<%=rnd%>();">
                        <option value="today"><%=Common.getBahasaConfig("Hari Ini")%></option>
                        <option value="week"><%=Common.getBahasaConfig("Minggu Ini")%></option>
                        <option value="month" selected><%=Common.getBahasaConfig("Bulan Ini")%></option>
                        <option value="semester"><%=Common.getBahasaConfig("Semester Ini (6 Bln)")%></option>
                        <option value="year"><%=Common.getBahasaConfig("Tahun Ini")%></option>
                        <option value="3years"><%=Common.getBahasaConfig("3 Tahun Ini")%></option>
                        <option value="custom"><%=Common.getBahasaConfig("Tanggal Custom")%></option>
                    </select>
                    <button class="btn btn-primary fw-bold" onclick="loadDashboardPromo<%=rnd%>()" id="btnSync<%=rnd%>">
                        <i class="fas fa-sync-alt"></i>
                    </button>
                </div>

                <!-- Input Tanggal Custom (Hidden by default) -->
                <div class="d-flex align-items-center gap-2 d-none" id="wrapperCustomDate<%=rnd%>">
                    <input type="date" class="form-control form-control-sm shadow-sm fw-bold text-primary" id="customStartDate<%=rnd%>" onchange="loadDashboardPromo<%=rnd%>()" title="Tanggal Mulai">
                    <span class="text-muted fw-bold">-</span>
                    <input type="date" class="form-control form-control-sm shadow-sm fw-bold text-primary" id="customEndDate<%=rnd%>" onchange="loadDashboardPromo<%=rnd%>()" title="Tanggal Selesai">
                </div>

            </div>
        </div>
    </div>

    <div class="row g-3 mb-4">
        
        <div class="col-xl-3 col-sm-6" onclick="downloadDataKPI<%=rnd%>('DISKON')" title="<%=Common.getBahasaConfig("Unduh Data Rincian Diskon")%>">
            <div class="card border-0 shadow-sm rounded-4 h-100 position-relative overflow-hidden kpi-card-link">
                <div class="card-body p-4">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <div>
                            <p class="text-muted fw-bold mb-0 small text-uppercase"><%=Common.getBahasaConfig("Diskon Diberikan")%></p>
                            <h3 class="fw-bold text-dark mb-0 mt-1" id="kpiDiskon<%=rnd%>">Rp 0</h3>
                        </div>
                        <div class="bg-danger bg-opacity-10 text-danger rounded-circle d-flex align-items-center justify-content-center" style="width: 50px; height: 50px;">
                            <i class="fas fa-cut fa-lg"></i>
                        </div>
                    </div>
                    <span class="badge bg-danger bg-opacity-10 text-danger fw-bold border border-danger border-opacity-25 rounded-pill"><i class="fas fa-file-excel me-1"></i>Unduh Rincian</span>
                </div>
                <div class="position-absolute bottom-0 start-0 w-100 bg-danger" style="height: 4px;"></div>
            </div>
        </div>

        <div class="col-xl-3 col-sm-6" onclick="downloadDataKPI<%=rnd%>('CASHBACK_IN')" title="<%=Common.getBahasaConfig("Unduh Data Cashback Member")%>">
            <div class="card border-0 shadow-sm rounded-4 h-100 position-relative overflow-hidden kpi-card-link">
                <div class="card-body p-4">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <div>
                            <p class="text-muted fw-bold mb-0 small text-uppercase"><%=Common.getBahasaConfig("Cashback Diberikan")%></p>
                            <h3 class="fw-bold text-dark mb-0 mt-1" id="kpiCashbackIn<%=rnd%>">Rp 0</h3>
                        </div>
                        <div class="bg-success bg-opacity-10 text-success rounded-circle d-flex align-items-center justify-content-center" style="width: 50px; height: 50px;">
                            <i class="fas fa-donate fa-lg"></i>
                        </div>
                    </div>
                    <span class="badge bg-success bg-opacity-10 text-success fw-bold border border-success border-opacity-25 rounded-pill"><i class="fas fa-file-excel me-1"></i>Unduh Rincian</span>
                </div>
                <div class="position-absolute bottom-0 start-0 w-100 bg-success" style="height: 4px;"></div>
            </div>
        </div>

        <div class="col-xl-3 col-sm-6" onclick="downloadDataKPI<%=rnd%>('CASHBACK_OUT')" title="<%=Common.getBahasaConfig("Unduh Data Pencairan")%>">
            <div class="card border-0 shadow-sm rounded-4 h-100 position-relative overflow-hidden kpi-card-link">
                <div class="card-body p-4">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <div>
                            <p class="text-muted fw-bold mb-0 small text-uppercase"><%=Common.getBahasaConfig("Cashback Dicairkan")%></p>
                            <h3 class="fw-bold text-dark mb-0 mt-1" id="kpiCashbackOut<%=rnd%>">Rp 0</h3>
                        </div>
                        <div class="bg-info bg-opacity-10 text-info rounded-circle d-flex align-items-center justify-content-center" style="width: 50px; height: 50px;">
                            <i class="fas fa-hand-holding-usd fa-lg"></i>
                        </div>
                    </div>
                    <span class="badge bg-info bg-opacity-10 text-info fw-bold border border-info border-opacity-25 rounded-pill"><i class="fas fa-file-excel me-1"></i>Unduh Riwayat</span>
                </div>
                <div class="position-absolute bottom-0 start-0 w-100 bg-info" style="height: 4px;"></div>
            </div>
        </div>

        <div class="col-xl-3 col-sm-6" onclick="downloadDataKPI<%=rnd%>('SALDO')" title="<%=Common.getBahasaConfig("Unduh Sisa Saldo Semua Member")%>">
            <div class="card border-0 shadow-sm rounded-4 h-100 position-relative overflow-hidden bg-primary bg-opacity-10 kpi-card-link">
                <div class="card-body p-4">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <div>
                            <p class="text-primary-emphasis fw-bold mb-0 small text-uppercase"><%=Common.getBahasaConfig("Total Saldo Mengendap")%></p>
                            <h3 class="fw-bold text-primary-emphasis mb-0 mt-1" id="kpiSisaSaldo<%=rnd%>">Rp 0</h3>
                        </div>
                        <div class="bg-primary text-white rounded-circle d-flex align-items-center justify-content-center shadow-sm" style="width: 50px; height: 50px;">
                            <i class="fas fa-wallet fa-lg"></i>
                        </div>
                    </div>
                    <span class="badge bg-primary text-white fw-bold rounded-pill shadow-sm"><i class="fas fa-file-excel me-1"></i>Unduh Rekap Saldo</span>
                </div>
                <div class="position-absolute bottom-0 start-0 w-100 bg-primary" style="height: 4px;"></div>
            </div>
        </div>
    </div>

    <div class="row g-3 mb-4">
        <div class="col-lg-8">
            <div class="card border-0 shadow-sm rounded-4 h-100">
                <div class="card-header bg-white border-bottom-0 pt-4 pb-2 px-4 d-flex justify-content-between align-items-center">
                    <div>
                        <h6 class="fw-bold text-dark mb-1"><i class="fas fa-chart-area text-primary me-2"></i><%=Common.getBahasaConfig("Tren Pemberian Promo & Diskon")%></h6>
                        <small class="text-muted"><%=Common.getBahasaConfig("Seberapa sering promo dipakai dari hari ke hari, supaya terlihat kapan diskon paling laris ditawarkan.")%></small>
                    </div>
                </div>
                <div class="card-body p-4">
                    <canvas id="chartTrenPromo<%=rnd%>" height="100"></canvas>
                </div>
            </div>
        </div>

        <div class="col-lg-4">
            <div class="card border-0 shadow-sm rounded-4 h-100">
                <div class="card-header bg-white border-bottom-0 pt-4 pb-2 px-4">
                    <h6 class="fw-bold text-dark mb-1"><i class="fas fa-box-open text-warning me-2"></i><%=Common.getBahasaConfig("Top 5 Produk Promo Terbesar")%></h6>
                    <small class="text-muted"><%=Common.getBahasaConfig("Produk mana yang paling banyak dijual pakai diskon.")%></small>
                </div>
                <div class="card-body p-4 d-flex justify-content-center align-items-center">
                    <canvas id="chartTopProdukPromo<%=rnd%>"></canvas>
                </div>
            </div>
        </div>
    </div>

    <div class="row g-3 mb-4">
        
        <div class="col-lg-6">
            <div class="card border-0 shadow-sm rounded-4 h-100 d-flex flex-column">
                <div class="card-header bg-white border-bottom-0 pt-3 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-2">
                    <h6 class="fw-bold text-dark mb-0"><i class="fas fa-users text-success me-2"></i><%=Common.getBahasaConfig("Rekap Penerima Promo")%></h6>
                    <div class="d-flex gap-2">
                        <div class="input-group input-group-sm" style="width: 150px;">
                            <span class="input-group-text bg-light border-end-0"><i class="fas fa-search text-muted"></i></span>
                            <input type="text" class="form-control border-start-0 bg-light" id="searchMember<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Member...")%>" onkeyup="filterTabelMember<%=rnd%>()">
                        </div>
                        <button class="btn btn-sm btn-success fw-bold px-2 shadow-sm" onclick="downloadTabelMemberExcel<%=rnd%>()" title="Unduh Tabel Ini">
                            <i class="fas fa-file-excel"></i>
                        </button>
                    </div>
                </div>
                <div class="card-body p-0 flex-grow-1">
                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0">
                            <thead class="table-light text-muted small position-sticky top-0" style="z-index: 1;">
                                <tr>
                                    <th class="px-4"><%=Common.getBahasaConfig("Nama Member")%></th>
                                    <th class="text-end"><%=Common.getBahasaConfig("Dapat Diskon")%></th>
                                    <th class="text-end"><%=Common.getBahasaConfig("Dapat Cashback")%></th>
                                    <th class="text-end px-4"><%=Common.getBahasaConfig("Sisa Saldo")%></th>
                                </tr>
                            </thead>
                            <tbody id="tabelMemberPromo<%=rnd%>">
                                </tbody>
                        </table>
                    </div>
                </div>
                <div class="card-footer bg-white border-top-0 d-flex justify-content-between align-items-center py-3 px-4">
                    <small class="text-muted fw-medium" id="pagingInfoMember<%=rnd%>"></small>
                    <div class="btn-group btn-group-sm shadow-sm">
                        <button class="btn btn-outline-success fw-bold" id="btnPrevMember<%=rnd%>" onclick="changePageMember<%=rnd%>(-1)"><i class="fas fa-chevron-left"></i></button>
                        <button class="btn btn-success fw-bold disabled" id="pageNumberMember<%=rnd%>" style="opacity: 1;">1</button>
                        <button class="btn btn-outline-success fw-bold" id="btnNextMember<%=rnd%>" onclick="changePageMember<%=rnd%>(1)"><i class="fas fa-chevron-right"></i></button>
                    </div>
                </div>
            </div>
        </div>

        <div class="col-lg-6">
            <div class="card border-0 shadow-sm rounded-4 h-100 d-flex flex-column">
                <div class="card-header bg-white border-bottom-0 pt-3 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-2">
                    <h6 class="fw-bold text-dark mb-0"><i class="fas fa-boxes text-danger me-2"></i><%=Common.getBahasaConfig("Rincian Produk Promo")%></h6>
                    <div class="d-flex gap-2">
                        <div class="input-group input-group-sm" style="width: 150px;">
                            <span class="input-group-text bg-light border-end-0"><i class="fas fa-search text-muted"></i></span>
                            <input type="text" class="form-control border-start-0 bg-light" id="searchProduk<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Produk...")%>" onkeyup="filterTabelProduk<%=rnd%>()">
                        </div>
                        <button class="btn btn-sm btn-danger fw-bold px-2 shadow-sm" onclick="downloadTabelProdukExcel<%=rnd%>()" title="Unduh Tabel Ini">
                            <i class="fas fa-file-excel"></i>
                        </button>
                    </div>
                </div>
                <div class="card-body p-0 flex-grow-1">
                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0">
                            <thead class="table-light text-muted small position-sticky top-0" style="z-index: 1;">
                                <tr>
                                    <th class="px-4"><%=Common.getBahasaConfig("Kode")%></th>
                                    <th><%=Common.getBahasaConfig("Produk")%></th>
                                    <th class="text-end"><%=Common.getBahasaConfig("Total Diskon")%></th>
                                    <th class="text-end px-4"><%=Common.getBahasaConfig("Total Cashback")%></th>
                                </tr>
                            </thead>
                            <tbody id="tabelProdukPromo<%=rnd%>">
                                </tbody>
                        </table>
                    </div>
                </div>
                <div class="card-footer bg-white border-top-0 d-flex justify-content-between align-items-center py-3 px-4">
                    <small class="text-muted fw-medium" id="pagingInfoProduk<%=rnd%>"></small>
                    <div class="btn-group btn-group-sm shadow-sm">
                        <button class="btn btn-outline-danger fw-bold" id="btnPrevProduk<%=rnd%>" onclick="changePageProduk<%=rnd%>(-1)"><i class="fas fa-chevron-left"></i></button>
                        <button class="btn btn-danger fw-bold disabled" id="pageNumberProduk<%=rnd%>" style="opacity: 1;">1</button>
                        <button class="btn btn-outline-danger fw-bold" id="btnNextProduk<%=rnd%>" onclick="changePageProduk<%=rnd%>(1)"><i class="fas fa-chevron-right"></i></button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="row g-3">
        <div class="col-12">
            <div class="card border-0 shadow-sm rounded-4 h-100 d-flex flex-column border-top border-primary border-3">
                <div class="card-header bg-white border-bottom-0 pt-3 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-2">
                    <h6 class="fw-bold text-dark mb-0"><i class="fas fa-tags text-primary me-2"></i><%=Common.getBahasaConfig("Dampak Hasil Aturan Diskon Aktif")%></h6>
                    <div class="d-flex gap-2">
                        <div class="input-group input-group-sm" style="min-width:180px;flex:1 1 200px;">
                            <span class="input-group-text bg-light border-end-0"><i class="fas fa-search text-muted"></i></span>
                            <input type="text" class="form-control border-start-0 bg-light" id="searchAturan<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Aturan / Produk...")%>" onkeyup="filterTabelAturan<%=rnd%>()">
                        </div>
                        <button class="btn btn-sm btn-primary fw-bold px-3 shadow-sm" onclick="downloadTabelAturanExcel<%=rnd%>()" title="Unduh Laporan Aturan Diskon">
                            <i class="fas fa-file-excel me-1"></i> Unduh
                        </button>
                    </div>
                </div>
                <div class="card-body p-0 flex-grow-1">
                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0">
                            <thead class="table-light text-muted small position-sticky top-0" style="z-index: 1;">
                                <tr>
                                    <th class="px-4"><%=Common.getBahasaConfig("Nama Aturan Promo")%></th>
                                    <th><%=Common.getBahasaConfig("Tipe Eksekusi")%></th>
                                    <th><%=Common.getBahasaConfig("Target Produk")%></th>
                                    <th class="text-center"><%=Common.getBahasaConfig("Total Digunakan")%></th>
                                    <th class="text-end px-4"><%=Common.getBahasaConfig("Total Biaya Dikeluarkan (Rp)")%></th>
                                </tr>
                            </thead>
                            <tbody id="tabelAturanPromo<%=rnd%>">
                                </tbody>
                        </table>
                    </div>
                </div>
                <div class="card-footer bg-white border-top-0 d-flex justify-content-between align-items-center py-3 px-4">
                    <small class="text-muted fw-medium" id="pagingInfoAturan<%=rnd%>"></small>
                    <div class="btn-group btn-group-sm shadow-sm">
                        <button class="btn btn-outline-primary fw-bold" id="btnPrevAturan<%=rnd%>" onclick="changePageAturan<%=rnd%>(-1)"><i class="fas fa-chevron-left"></i></button>
                        <button class="btn btn-primary fw-bold disabled" id="pageNumberAturan<%=rnd%>" style="opacity: 1;">1</button>
                        <button class="btn btn-outline-primary fw-bold" id="btnNextAturan<%=rnd%>" onclick="changePageAturan<%=rnd%>(1)"><i class="fas fa-chevron-right"></i></button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- ================= Data Saldo Member Reguler Special (MRS) ================= -->
    <div class="row g-3 mt-1">
        <div class="col-12">
            <div class="card border-0 shadow-sm rounded-4 h-100 d-flex flex-column border-top border-success border-3">
                <div class="card-header bg-white border-bottom-0 pt-3 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-2">
                    <h6 class="fw-bold text-dark mb-0"><i class="fas fa-wallet text-success me-2"></i><%=Common.getBahasaConfig("Data Saldo Member Reguler Special (MRS)")%></h6>
                    <div class="d-flex gap-2">
                        <div class="input-group input-group-sm" style="width: 200px;">
                            <span class="input-group-text bg-light border-end-0"><i class="fas fa-search text-muted"></i></span>
                            <input type="text" class="form-control border-start-0 bg-light" id="searchMrs<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Member...")%>" onkeyup="filterTabelMrs<%=rnd%>()">
                        </div>
                        <button class="btn btn-sm btn-success fw-bold px-3 shadow-sm" onclick="downloadTabelMrsExcel<%=rnd%>()" title="<%=Common.getBahasaConfig("Unduh")%>"><i class="fas fa-file-excel me-1"></i> <%=Common.getBahasaConfig("Unduh")%></button>
                    </div>
                </div>
                <div class="card-body p-0 flex-grow-1">
                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0">
                            <thead class="table-light text-muted small position-sticky top-0" style="z-index: 1;">
                                <tr>
                                    <th class="px-4"><%=Common.getBahasaConfig("Nama Member")%></th>
                                    <th class="text-end"><%=Common.getBahasaConfig("Total Saldo Awal (Top-Up)")%></th>
                                    <th class="text-end"><%=Common.getBahasaConfig("Total Terpakai (Belanja)")%></th>
                                    <th class="text-end px-4"><%=Common.getBahasaConfig("Sisa Saldo")%></th>
                                </tr>
                            </thead>
                            <tbody id="tabelMrs<%=rnd%>"></tbody>
                        </table>
                    </div>
                </div>
                <div class="card-footer bg-white border-top-0 d-flex justify-content-between align-items-center py-3 px-4">
                    <small class="text-muted fw-medium" id="pagingInfoMrs<%=rnd%>"></small>
                    <div class="btn-group btn-group-sm shadow-sm">
                        <button class="btn btn-outline-success fw-bold" id="btnPrevMrs<%=rnd%>" onclick="changePageMrs<%=rnd%>(-1)"><i class="fas fa-chevron-left"></i></button>
                        <button class="btn btn-success fw-bold disabled" id="pageNumberMrs<%=rnd%>" style="opacity: 1;">1</button>
                        <button class="btn btn-outline-success fw-bold" id="btnNextMrs<%=rnd%>" onclick="changePageMrs<%=rnd%>(1)"><i class="fas fa-chevron-right"></i></button>
                    </div>
                </div>
            </div>
        </div>
    </div>

</div>

<script>
    let chartTrenInstance<%=rnd%> = null;
    let chartTopInstance<%=rnd%> = null;
    const isAdmin<%=rnd%> = <%=isAdmin%>;
    const defaultTokoId<%=rnd%> = '<%=idTokoAktif%>';

    // State Pagination
    let memberDataList<%=rnd%> = [];
    let memberPage<%=rnd%> = 1;
    const memberLimit<%=rnd%> = 5;

    let produkDataList<%=rnd%> = [];
    let produkPage<%=rnd%> = 1;
    const produkLimit<%=rnd%> = 5;

    let aturanDataList<%=rnd%> = [];
    let aturanPage<%=rnd%> = 1;
    const aturanLimit<%=rnd%> = 5;

    let mrsDataList<%=rnd%> = [];
    let mrsPage<%=rnd%> = 1;
    const mrsLimit<%=rnd%> = 8;

    // Menyiapkan default Tanggal Custom saat ini
    const setDefaultCustomDates<%=rnd%> = () => {
        const today = new Date();
        const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
        
        const todayStr = today.getFullYear() + '-' + String(today.getMonth() + 1).padStart(2, '0') + '-' + String(today.getDate()).padStart(2, '0');
        const firstDayStr = firstDay.getFullYear() + '-' + String(firstDay.getMonth() + 1).padStart(2, '0') + '-01';
        
        document.getElementById('customStartDate<%=rnd%>').value = firstDayStr;
        document.getElementById('customEndDate<%=rnd%>').value = todayStr;
    };

    // Fungsi Toggle Input Custom Date
    const toggleCustomDate<%=rnd%> = () => {
        const periodeVal = document.getElementById('filterPeriode<%=rnd%>').value;
        const wrapperDate = document.getElementById('wrapperCustomDate<%=rnd%>');
        
        if (periodeVal === 'custom') {
            wrapperDate.classList.remove('d-none');
            wrapperDate.classList.add('d-flex');
        } else {
            wrapperDate.classList.remove('d-flex');
            wrapperDate.classList.add('d-none');
        }
    };

    const fetchApiData<%=rnd%> = async (sql) => {
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sql: sql, action: "sql" })
            });
            const result = await res.json();
            return result.data || [];
        } catch (e) { 
            console.error("Fetch Data Error:", e); 
            return []; 
        }
    };

    const formatRp<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);
    };

    const initFilterToko<%=rnd%> = async () => {
        if (isAdmin<%=rnd%>) {
            const sqlToko = "SELECT id, nama FROM koperasi.toko WHERE aktif = true ORDER BY nama ASC";
            const resToko = await fetchApiData<%=rnd%>(sqlToko);
            
            const elFilter = document.getElementById('filterToko<%=rnd%>');
            let optHtml = '<option value="">' + '<%=Common.getBahasaConfigJS("- Semua Toko -")%>' + '</option>';
            resToko.forEach(item => {
                optHtml += '<option value="' + item.id + '">' + item.nama + '</option>';
            });
            elFilter.innerHTML = optHtml;
        }
    };

    const initFilterJenisAnggota<%=rnd%> = async () => {
        const sqlJenis = "SELECT id, nama FROM koperasi.jenis_anggota_koperasi WHERE aktif = true ORDER BY nama ASC";
        const resJenis = await fetchApiData<%=rnd%>(sqlJenis);
        
        const elFilter = document.getElementById('filterJenisAnggota<%=rnd%>');
        let optHtml = '<option value="">' + '<%=Common.getBahasaConfigJS("- Semua Anggota -")%>' + '</option>';
        resJenis.forEach(item => {
            optHtml += '<option value="' + item.id + '">' + item.nama + '</option>';
        });
        elFilter.innerHTML = optHtml;
    };

    // Modifikasi GetSqlTimeCondition untuk Support Custom Date
    const getSqlTimeCondition<%=rnd%> = (colName, filterVal) => {
        if (filterVal === 'today') return " AND " + colName + " >= current_date ";
        if (filterVal === 'week') return " AND " + colName + " >= current_date - interval '7 days' ";
        if (filterVal === 'month') return " AND date_trunc('month', " + colName + ") = date_trunc('month', current_date) ";
        if (filterVal === 'semester') return " AND " + colName + " >= current_date - interval '6 months' ";
        if (filterVal === 'year') return " AND date_trunc('year', " + colName + ") = date_trunc('year', current_date) ";
        if (filterVal === '3years') return " AND " + colName + " >= current_date - interval '3 years' ";
        
        if (filterVal === 'custom') {
            const startDate = document.getElementById('customStartDate<%=rnd%>').value;
            const endDate = document.getElementById('customEndDate<%=rnd%>').value;
            if (startDate !== "" && endDate !== "") {
                return " AND " + colName + " BETWEEN '" + startDate + " 00:00:00' AND '" + endDate + " 23:59:59' ";
            }
            return ""; // Diabaikan jika salah satu tanggal kosong
        }
        
        return ""; 
    };

    // =======================================================
    // EXPORT EXCEL GENERAL HELPER
    // =======================================================
    const generateExcelFile<%=rnd%> = (dataArray, columns, fileNamePrefix) => {
        if(dataArray.length === 0) {
            alert('<%=Common.getBahasaConfigJS("Tidak ada data untuk diunduh.")%>');
            return;
        }
        let tableHtml = '<html xmlns:x="urn:schemas-microsoft-com:office:excel"><head><meta charset="UTF-8"><style>th{background-color:#f8f9fa;border:1px solid #000;}td{border:1px solid #000;}</style></head><body>';
        tableHtml += '<table><thead><tr>';
        columns.forEach(col => { tableHtml += '<th>' + col.header + '</th>'; });
        tableHtml += '</tr></thead><tbody>';
        dataArray.forEach(row => {
            tableHtml += '<tr>';
            columns.forEach(col => { tableHtml += '<td>' + row[col.key] + '</td>'; });
            tableHtml += '</tr>';
        });
        tableHtml += '</tbody></table></body></html>';
        
        const blob = new Blob([tableHtml], { type: 'application/vnd.ms-excel' });
        const link = document.createElement("a");
        link.download = fileNamePrefix + "_" + new Date().getTime() + ".xls";
        link.href = URL.createObjectURL(blob);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    };

    // =======================================================
    // EXPORT EXCEL UNTUK KPI BOX (Raw Data)
    // =======================================================
    const downloadDataKPI<%=rnd%> = async (kpiType) => {
        const periode = document.getElementById('filterPeriode<%=rnd%>').value;
        const elToko = document.getElementById('filterToko<%=rnd%>');
        const tokoId = elToko ? elToko.value : defaultTokoId<%=rnd%>;
        const jenisId = document.getElementById('filterJenisAnggota<%=rnd%>').value;
        
        let sqlFilterToko = tokoId !== "" ? " AND a.toko = " + tokoId + " " : "";
        let condJenisAk = jenisId !== "" ? " AND ak.jenis_anggota_koperasi = " + jenisId + " " : "";
        let joinDet_PAK_C = jenisId !== "" ? " INNER JOIN koperasi.pembelian_anggota_koperasi pak ON a.pembelian_anggota_koperasi = pak.id INNER JOIN koperasi.anggota_koperasi c ON pak.anggota_koperasi = c.id " : "";
        let condJenis = jenisId !== "" ? " AND c.jenis_anggota_koperasi = " + jenisId + " " : "";

        let sqlQuery = "";
        let fileName = "";
        
        document.body.style.cursor = 'wait';
        
        try {
            if (kpiType === 'DISKON') {
                sqlQuery = "SELECT a.kode, TO_CHAR(a.waktu, 'YYYY-MM-DD HH24:MI:SS') as waktu, p.nama as produk, a.diskon " +
                           "FROM koperasi.pembelian a INNER JOIN koperasi.produk p ON a.produk = p.id " +
                           joinDet_PAK_C +
                           "WHERE a.diskon > 0 " + sqlFilterToko + getSqlTimeCondition<%=rnd%>("a.waktu", periode) + condJenis + " ORDER BY a.waktu DESC;";
                fileName = "Rincian_Diskon_Diberikan";
            }
            else if (kpiType === 'CASHBACK_IN') {
                sqlQuery = "SELECT a.kode, TO_CHAR(a.waktu, 'YYYY-MM-DD HH24:MI:SS') as waktu, p.nama as produk, a.cashback " +
                           "FROM koperasi.pembelian a INNER JOIN koperasi.produk p ON a.produk = p.id " +
                           joinDet_PAK_C +
                           "WHERE a.cashback > 0 " + sqlFilterToko + getSqlTimeCondition<%=rnd%>("a.waktu", periode) + condJenis + " ORDER BY a.waktu DESC;";
                fileName = "Rincian_Cashback_Diberikan";
            }
            else if (kpiType === 'CASHBACK_OUT') {
                sqlQuery = "SELECT a.kode_pencairan, TO_CHAR(a.waktu_pencairan, 'YYYY-MM-DD HH24:MI:SS') as waktu, ak.nama as member, a.nominal_cair " +
                           "FROM koperasi.pencairan_diskon a INNER JOIN koperasi.anggota_koperasi ak ON a.anggota_koperasi = ak.id " +
                           "WHERE a.status IN ('BERHASIL','PENDING') " + sqlFilterToko + getSqlTimeCondition<%=rnd%>("a.waktu_pencairan", periode) + condJenisAk + " ORDER BY a.waktu_pencairan DESC;";
                fileName = "Rincian_Pencairan_Cashback";
            }
            else if (kpiType === 'SALDO') {
                // Sisa Saldo Absolut tanpa filter periode waktu
                sqlQuery = "SELECT t.kode_identitas, t.nama_member, t.sisa_saldo FROM ( " +
                           "SELECT ak.kode_identitas, ak.nama as nama_member, " +
                           "(COALESCE((SELECT SUM(totalcashback) FROM koperasi.pembelian_anggota_koperasi p WHERE p.anggota_koperasi = ak.id), 0) - " +
                           "COALESCE((SELECT SUM(nominal_cair) FROM koperasi.pencairan_diskon d WHERE d.anggota_koperasi = ak.id AND d.status IN ('BERHASIL', 'PENDING')), 0)) as sisa_saldo " +
                           "FROM koperasi.anggota_koperasi ak WHERE 1=1 " + condJenisAk + ") t WHERE t.sisa_saldo > 0 ORDER BY t.sisa_saldo DESC;";
                fileName = "Rekap_Sisa_Saldo_Member";
            }

            const result = await fetchApiData<%=rnd%>(sqlQuery);
            if (!result || result.length === 0) {
                alert('<%=Common.getBahasaConfigJS("Tidak ada data untuk diunduh pada filter ini.")%>');
                return;
            }

            const columns = Object.keys(result[0]).map(k => ({ header: k.toUpperCase(), key: k }));
            generateExcelFile<%=rnd%>(result, columns, fileName);

        } catch (error) {
            alert('<%=Common.getBahasaConfigJS("Maaf, berkas Excel gagal diunduh. Silakan coba lagi.")%>');
        } finally {
            document.body.style.cursor = 'default';
        }
    };


    // =======================================================
    // MAIN FETCH DATA DASHBOARD
    // =======================================================
    const loadDashboardPromo<%=rnd%> = async () => {
        const periode = document.getElementById('filterPeriode<%=rnd%>').value;
        const elToko = document.getElementById('filterToko<%=rnd%>');
        const tokoId = elToko ? elToko.value : defaultTokoId<%=rnd%>;
        const jenisId = document.getElementById('filterJenisAnggota<%=rnd%>').value;
        
        const btnSync = document.getElementById('btnSync<%=rnd%>');
        btnSync.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        btnSync.disabled = true;

        try {
            let sqlFilterToko = tokoId !== "" ? " AND a.toko = " + tokoId + " " : "";
            let sqlFilterTokoDet = tokoId !== "" ? " AND det.toko = " + tokoId + " " : "";
            
            let joinA_C = jenisId !== "" ? " INNER JOIN koperasi.anggota_koperasi c ON a.anggota_koperasi = c.id " : "";
            let joinPA_C = jenisId !== "" ? " INNER JOIN koperasi.anggota_koperasi c ON pa.anggota_koperasi = c.id " : "";
            let joinD_C = jenisId !== "" ? " INNER JOIN koperasi.anggota_koperasi c ON d.anggota_koperasi = c.id " : "";
            let joinDet_PAK_C = jenisId !== "" ? " INNER JOIN koperasi.pembelian_anggota_koperasi pak ON det.pembelian_anggota_koperasi = pak.id INNER JOIN koperasi.anggota_koperasi c ON pak.anggota_koperasi = c.id " : "";
            let condJenis = jenisId !== "" ? " AND c.jenis_anggota_koperasi = " + jenisId + " " : "";
            let condJenisMember = jenisId !== "" ? " AND c.jenis_anggota_koperasi = " + jenisId + " " : "";

            // 1. KPI Data
            let sqlKPI = "SELECT " +
                "(SELECT COALESCE(SUM(a.total_diskon), 0) FROM koperasi.pembelian_anggota_koperasi a " + joinA_C + " WHERE 1=1 " + sqlFilterToko + getSqlTimeCondition<%=rnd%>("a.tanggal_pembayaran", periode) + condJenis + ") as t_diskon, " +
                "(SELECT COALESCE(SUM(a.totalcashback), 0) FROM koperasi.pembelian_anggota_koperasi a " + joinA_C + " WHERE 1=1 " + sqlFilterToko + getSqlTimeCondition<%=rnd%>("a.tanggal_pembayaran", periode) + condJenis + ") as t_cashback_in, " +
                "(SELECT COALESCE(SUM(a.nominal_cair), 0) FROM koperasi.pencairan_diskon a " + joinA_C + " WHERE a.status IN ('BERHASIL', 'PENDING') " + sqlFilterToko + getSqlTimeCondition<%=rnd%>("a.waktu_pencairan", periode) + condJenis + ") as t_cair, " +
                "(SELECT (COALESCE(SUM(pa.totalcashback),0) - (SELECT COALESCE(SUM(d.nominal_cair),0) FROM koperasi.pencairan_diskon d " + joinD_C + " WHERE d.status IN ('BERHASIL','PENDING') " + condJenis + ")) FROM koperasi.pembelian_anggota_koperasi pa " + joinPA_C + " WHERE 1=1 " + condJenis + ") as sisa_absolut";
                
            let resKPI = await fetchApiData<%=rnd%>(sqlKPI);
            if(resKPI && resKPI.length > 0) {
                document.getElementById('kpiDiskon<%=rnd%>').innerText = formatRp<%=rnd%>(resKPI[0].t_diskon);
                document.getElementById('kpiCashbackIn<%=rnd%>').innerText = formatRp<%=rnd%>(resKPI[0].t_cashback_in);
                document.getElementById('kpiCashbackOut<%=rnd%>').innerText = formatRp<%=rnd%>(resKPI[0].t_cair);
                document.getElementById('kpiSisaSaldo<%=rnd%>').innerText = formatRp<%=rnd%>(resKPI[0].sisa_absolut);
            }

            // 2. CHART TREN
            let timeGroupFormat = (periode === 'year' || periode === 'semester' || periode === '3years') ? 'YYYY-MM' : 'YYYY-MM-DD';
            let sqlChart = "SELECT TO_CHAR(a.tanggal_pembayaran, '" + timeGroupFormat + "') as tanggal, SUM(a.total_diskon) as diskon, SUM(a.totalcashback) as cashback " +
                           "FROM koperasi.pembelian_anggota_koperasi a " + joinA_C + " WHERE (a.total_diskon > 0 OR a.totalcashback > 0) " + sqlFilterToko + getSqlTimeCondition<%=rnd%>("a.tanggal_pembayaran", periode) + condJenis +
                           " GROUP BY TO_CHAR(a.tanggal_pembayaran, '" + timeGroupFormat + "') ORDER BY tanggal ASC;";
            let resChart = await fetchApiData<%=rnd%>(sqlChart);

            let labelsChart = resChart.map(r => r.tanggal);
            let dataDiskon = resChart.map(r => parseFloat(r.diskon));
            let dataCashback = resChart.map(r => parseFloat(r.cashback));
            renderChartTren<%=rnd%>({ labels: labelsChart, diskon: dataDiskon, cashback: dataCashback });

            // 3. TABEL PRODUK & CHART DOUGHNUT
            let sqlProduk = "SELECT p.kode, p.nama as produk, SUM(det.diskon) as t_diskon, SUM(det.cashback) as t_cashback " +
                            "FROM koperasi.pembelian det INNER JOIN koperasi.produk p ON det.produk = p.id " +
                            joinDet_PAK_C +
                            "WHERE (det.diskon > 0 OR det.cashback > 0) " + sqlFilterTokoDet + getSqlTimeCondition<%=rnd%>("det.waktu", periode) + condJenis +
                            " GROUP BY p.kode, p.nama ORDER BY (SUM(det.diskon) + SUM(det.cashback)) DESC LIMIT 100;";
            let resProduk = await fetchApiData<%=rnd%>(sqlProduk);
            
            produkDataList<%=rnd%> = resProduk.map(r => ({
                kode: r.kode || '-',
                nama: r.produk,
                diskon: parseFloat(r.t_diskon),
                cashback: parseFloat(r.t_cashback)
            }));

            let lblTop = produkDataList<%=rnd%>.slice(0,5).map(r => r.nama);
            let valTop = produkDataList<%=rnd%>.slice(0,5).map(r => r.diskon + r.cashback);
            renderChartTopProduk<%=rnd%>({ labels: lblTop, values: valTop });

            // 4. TABEL MEMBER
            let sqlMember = "SELECT c.kode_identitas, c.nama, " +
                    "SUM(a.total_diskon) as diskon_periode, SUM(a.totalcashback) as cashback_periode, " +
                    "(COALESCE((SELECT SUM(totalcashback) FROM koperasi.pembelian_anggota_koperasi p WHERE p.anggota_koperasi = c.id), 0) - " +
                    "COALESCE((SELECT SUM(nominal_cair) FROM koperasi.pencairan_diskon d WHERE d.anggota_koperasi = c.id AND d.status IN ('BERHASIL', 'PENDING')), 0)) as sisa_saldo " +
                    "FROM koperasi.pembelian_anggota_koperasi a INNER JOIN koperasi.anggota_koperasi c ON a.anggota_koperasi = c.id " +
                    "WHERE (a.total_diskon > 0 OR a.totalcashback > 0) " + sqlFilterToko + getSqlTimeCondition<%=rnd%>("a.tanggal_pembayaran", periode) + condJenisMember +
                    " GROUP BY c.id, c.kode_identitas, c.nama " +
                    " ORDER BY cashback_periode DESC, diskon_periode DESC LIMIT 100;";
            let resMember = await fetchApiData<%=rnd%>(sqlMember);

            memberDataList<%=rnd%> = resMember.map(r => ({
                nama: r.nama + (r.kode_identitas ? ' ('+r.kode_identitas+')' : ''),
                diskon_periode: parseFloat(r.diskon_periode),
                cashback_periode: parseFloat(r.cashback_periode),
                sisa_saldo: parseFloat(r.sisa_saldo)
            }));

            // 5. TABEL DAMPAK ATURAN DISKON
            let sqlAturan = "SELECT ad.nama_aturan, ad.potongan_langsung, p.nama as nama_produk, " +
                            "COUNT(det.id) as total_pakai, SUM(det.diskon + det.cashback) as total_nominal " +
                            "FROM koperasi.pembelian det " +
                            "INNER JOIN koperasi.aturan_diskon ad ON det.aturan_diskon = ad.id " +
                            "INNER JOIN koperasi.produk p ON det.produk = p.id " +
                            joinDet_PAK_C +
                            "WHERE 1=1 " + sqlFilterTokoDet + getSqlTimeCondition<%=rnd%>("det.waktu", periode) + condJenis +
                            " GROUP BY ad.id, ad.nama_aturan, ad.potongan_langsung, p.nama " +
                            "ORDER BY total_nominal DESC LIMIT 100;";
            let resAturan = await fetchApiData<%=rnd%>(sqlAturan);
            
            aturanDataList<%=rnd%> = resAturan.map(r => ({
                namaAturan: r.nama_aturan,
                tipeEksekusi: (r.potongan_langsung === true || r.potongan_langsung === 'true') ? 'Potong Struk' : 'Cashback Member',
                namaProduk: r.nama_produk,
                totalPakai: parseInt(r.total_pakai),
                totalNominal: parseFloat(r.total_nominal)
            }));

            // Bersihkan searchbox
            document.getElementById('searchMember<%=rnd%>').value = '';
            document.getElementById('searchProduk<%=rnd%>').value = '';
            document.getElementById('searchAturan<%=rnd%>').value = '';
            
            // Reset Halaman
            memberPage<%=rnd%> = 1;
            produkPage<%=rnd%> = 1;
            aturanPage<%=rnd%> = 1;
            
            // Render ulang semua tabel
            renderTabelMember<%=rnd%>();
            renderTabelProduk<%=rnd%>();
            renderTabelAturan<%=rnd%>();

        } catch (error) {
            console.error("Gagal memuat dashboard promo:", error);
            alert('<%=Common.getBahasaConfigJS("Maaf, terjadi kesalahan saat memuat data dasbor. Silakan coba lagi.")%>');
        } finally {
            btnSync.innerHTML = '<i class="fas fa-sync-alt"></i>';
            btnSync.disabled = false;
        }
    };

    const renderChartTren<%=rnd%> = (data) => {
        const ctx = document.getElementById('chartTrenPromo<%=rnd%>').getContext('2d');
        if (chartTrenInstance<%=rnd%>) chartTrenInstance<%=rnd%>.destroy();

        chartTrenInstance<%=rnd%> = new Chart(ctx, {
            type: 'line',
            data: {
                labels: data.labels, 
                datasets: [
                    {
                        label: 'Diskon Diberikan (Rp)',
                        data: data.diskon,
                        borderColor: '#dc3545',
                        backgroundColor: 'rgba(220, 53, 69, 0.1)', 
                        borderWidth: 2,
                        fill: true,
                        tension: 0.3
                    },
                    {
                        label: 'Cashback Diberikan (Rp)',
                        data: data.cashback,
                        borderColor: '#198754',
                        backgroundColor: 'rgba(25, 135, 84, 0.1)', 
                        borderWidth: 2,
                        fill: true,
                        tension: 0.3
                    }
                ]
            },
            options: {
                responsive: true,
                interaction: { intersect: false, mode: 'index' },
                plugins: { legend: { position: 'top' } },
                scales: {
                    y: { beginAtZero: true, grid: { borderDash: [2, 4] } },
                    x: { grid: { display: false } }
                }
            }
        });
    };

    const renderChartTopProduk<%=rnd%> = (data) => {
        const ctx = document.getElementById('chartTopProdukPromo<%=rnd%>').getContext('2d');
        if (chartTopInstance<%=rnd%>) chartTopInstance<%=rnd%>.destroy();

        chartTopInstance<%=rnd%> = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: data.labels,
                datasets: [{
                    data: data.values,
                    backgroundColor: ['#dc3545', '#fd7e14', '#ffc107', '#198754', '#0d6efd'],
                    borderWidth: 2,
                    borderColor: '#ffffff'
                }]
            },
            options: {
                responsive: true,
                cutout: '65%',
                plugins: { legend: { position: 'bottom', labels: { boxWidth: 12 } } }
            }
        });
    };


    // ==========================================
    // LOGIC TABEL & PAGING: MEMBER
    // ==========================================
    const filterTabelMember<%=rnd%> = () => { memberPage<%=rnd%> = 1; renderTabelMember<%=rnd%>(); };
    const getFilteredMemberData<%=rnd%> = () => {
        const keyword = document.getElementById('searchMember<%=rnd%>').value.toLowerCase();
        if (keyword === "") return memberDataList<%=rnd%>;
        return memberDataList<%=rnd%>.filter(item => item.nama.toLowerCase().includes(keyword));
    };

    const renderTabelMember<%=rnd%> = () => {
        const tbody = document.getElementById('tabelMemberPromo<%=rnd%>');
        let html = '';
        
        const filteredData = getFilteredMemberData<%=rnd%>();
        const totalRecords = filteredData.length;
        const totalPages = Math.ceil(totalRecords / memberLimit<%=rnd%>) || 1;
        if (memberPage<%=rnd%> > totalPages) memberPage<%=rnd%> = totalPages;
        if (memberPage<%=rnd%> < 1) memberPage<%=rnd%> = 1;

        const startIndex = (memberPage<%=rnd%> - 1) * memberLimit<%=rnd%>;
        const endIndex = Math.min(startIndex + memberLimit<%=rnd%>, totalRecords);
        const currentData = filteredData.slice(startIndex, endIndex);

        if(currentData.length === 0) {
            html = '<tr><td colspan="4" class="text-center text-muted py-4"><i class="fas fa-users-slash opacity-25 fa-2x mb-2 d-block"></i>Belum ada data member.</td></tr>';
        } else {
            currentData.forEach(item => {
                html += '<tr>' +
                            '<td class="px-4 fw-bold text-dark">' + item.nama + '</td>' +
                            '<td class="text-end fw-bold text-danger">' + formatRp<%=rnd%>(item.diskon_periode) + '</td>' +
                            '<td class="text-end fw-bold text-success">' + formatRp<%=rnd%>(item.cashback_periode) + '</td>' +
                            '<td class="text-end px-4 fw-bold text-primary">' + formatRp<%=rnd%>(item.sisa_saldo) + '</td>' +
                        '</tr>';
            });
        }
        tbody.innerHTML = html;
        document.getElementById('pageNumberMember<%=rnd%>').innerText = memberPage<%=rnd%> + " / " + totalPages;
        document.getElementById('pagingInfoMember<%=rnd%>').innerText = (totalRecords > 0 ? (startIndex + 1) : 0) + " - " + endIndex + " dari " + totalRecords;
        document.getElementById('btnPrevMember<%=rnd%>').disabled = (memberPage<%=rnd%> === 1);
        document.getElementById('btnNextMember<%=rnd%>').disabled = (memberPage<%=rnd%> === totalPages);
    };

    const changePageMember<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(getFilteredMemberData<%=rnd%>().length / memberLimit<%=rnd%>) || 1;
        if (direction === -1 && memberPage<%=rnd%> > 1) { memberPage<%=rnd%>--; renderTabelMember<%=rnd%>(); }
        else if (direction === 1 && memberPage<%=rnd%> < totalPages) { memberPage<%=rnd%>++; renderTabelMember<%=rnd%>(); }
    };

    const downloadTabelMemberExcel<%=rnd%> = () => {
        const columns = [
            { header: "Nama Member", key: "nama" },
            { header: "Dapat Diskon (Rp)", key: "diskon_periode" },
            { header: "Dapat Cashback (Rp)", key: "cashback_periode" },
            { header: "Sisa Saldo Aktual (Rp)", key: "sisa_saldo" }
        ];
        generateExcelFile<%=rnd%>(getFilteredMemberData<%=rnd%>(), columns, "Rekap_Member_Promo");
    };


    // ==========================================
    // LOGIC TABEL & PAGING: PRODUK
    // ==========================================
    const filterTabelProduk<%=rnd%> = () => { produkPage<%=rnd%> = 1; renderTabelProduk<%=rnd%>(); };
    const getFilteredProdukData<%=rnd%> = () => {
        const keyword = document.getElementById('searchProduk<%=rnd%>').value.toLowerCase();
        if (keyword === "") return produkDataList<%=rnd%>;
        return produkDataList<%=rnd%>.filter(item => item.nama.toLowerCase().includes(keyword) || item.kode.toLowerCase().includes(keyword));
    };

    const renderTabelProduk<%=rnd%> = () => {
        const tbody = document.getElementById('tabelProdukPromo<%=rnd%>');
        let html = '';
        
        const filteredData = getFilteredProdukData<%=rnd%>();
        const totalRecords = filteredData.length;
        const totalPages = Math.ceil(totalRecords / produkLimit<%=rnd%>) || 1;
        if (produkPage<%=rnd%> > totalPages) produkPage<%=rnd%> = totalPages;
        if (produkPage<%=rnd%> < 1) produkPage<%=rnd%> = 1;

        const startIndex = (produkPage<%=rnd%> - 1) * produkLimit<%=rnd%>;
        const endIndex = Math.min(startIndex + produkLimit<%=rnd%>, totalRecords);
        const currentData = filteredData.slice(startIndex, endIndex);

        if(currentData.length === 0) {
            html = '<tr><td colspan="4" class="text-center text-muted py-4"><i class="fas fa-box-open opacity-25 fa-2x mb-2 d-block"></i>Belum ada data produk promo.</td></tr>';
        } else {
            currentData.forEach(item => {
                html += '<tr>' +
                            '<td class="px-4 text-muted small fw-medium">' + item.kode + '</td>' +
                            '<td class="fw-bold text-dark">' + item.nama + '</td>' +
                            '<td class="text-end fw-bold text-danger">' + formatRp<%=rnd%>(item.diskon) + '</td>' +
                            '<td class="text-end px-4 fw-bold text-success">' + formatRp<%=rnd%>(item.cashback) + '</td>' +
                        '</tr>';
            });
        }
        tbody.innerHTML = html;
        document.getElementById('pageNumberProduk<%=rnd%>').innerText = produkPage<%=rnd%> + " / " + totalPages;
        document.getElementById('pagingInfoProduk<%=rnd%>').innerText = (totalRecords > 0 ? (startIndex + 1) : 0) + " - " + endIndex + " dari " + totalRecords;
        document.getElementById('btnPrevProduk<%=rnd%>').disabled = (produkPage<%=rnd%> === 1);
        document.getElementById('btnNextProduk<%=rnd%>').disabled = (produkPage<%=rnd%> === totalPages);
    };

    const changePageProduk<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(getFilteredProdukData<%=rnd%>().length / produkLimit<%=rnd%>) || 1;
        if (direction === -1 && produkPage<%=rnd%> > 1) { produkPage<%=rnd%>--; renderTabelProduk<%=rnd%>(); }
        else if (direction === 1 && produkPage<%=rnd%> < totalPages) { produkPage<%=rnd%>++; renderTabelProduk<%=rnd%>(); }
    };

    const downloadTabelProdukExcel<%=rnd%> = () => {
        const columns = [
            { header: "Kode", key: "kode" },
            { header: "Nama Produk", key: "nama" },
            { header: "Total Diskon (Rp)", key: "diskon" },
            { header: "Total Cashback (Rp)", key: "cashback" }
        ];
        generateExcelFile<%=rnd%>(getFilteredProdukData<%=rnd%>(), columns, "Rincian_Produk_Promo");
    };


    // ==========================================
    // LOGIC TABEL & PAGING: ATURAN DISKON
    // ==========================================
    const filterTabelAturan<%=rnd%> = () => { aturanPage<%=rnd%> = 1; renderTabelAturan<%=rnd%>(); };
    const getFilteredAturanData<%=rnd%> = () => {
        const keyword = document.getElementById('searchAturan<%=rnd%>').value.toLowerCase();
        if (keyword === "") return aturanDataList<%=rnd%>;
        return aturanDataList<%=rnd%>.filter(item => item.namaAturan.toLowerCase().includes(keyword) || item.namaProduk.toLowerCase().includes(keyword));
    };

    const renderTabelAturan<%=rnd%> = () => {
        const tbody = document.getElementById('tabelAturanPromo<%=rnd%>');
        let html = '';
        
        const filteredData = getFilteredAturanData<%=rnd%>();
        const totalRecords = filteredData.length;
        const totalPages = Math.ceil(totalRecords / aturanLimit<%=rnd%>) || 1;
        if (aturanPage<%=rnd%> > totalPages) aturanPage<%=rnd%> = totalPages;
        if (aturanPage<%=rnd%> < 1) aturanPage<%=rnd%> = 1;

        const startIndex = (aturanPage<%=rnd%> - 1) * aturanLimit<%=rnd%>;
        const endIndex = Math.min(startIndex + aturanLimit<%=rnd%>, totalRecords);
        const currentData = filteredData.slice(startIndex, endIndex);

        if(currentData.length === 0) {
            html = '<tr><td colspan="5" class="text-center text-muted py-4"><i class="fas fa-tags opacity-25 fa-2x mb-2 d-block"></i>Belum ada data aturan diskon yang digunakan.</td></tr>';
        } else {
            currentData.forEach(item => {
                let badgeTipe = item.tipeEksekusi === 'Potong Struk' 
                    ? '<span class="badge bg-danger bg-opacity-10 text-danger border border-danger border-opacity-25 px-2">Potong Struk</span>' 
                    : '<span class="badge bg-success bg-opacity-10 text-success border border-success border-opacity-25 px-2">Cashback Member</span>';

                html += '<tr>' +
                            '<td class="px-4 fw-bold text-dark">' + item.namaAturan + '</td>' +
                            '<td>' + badgeTipe + '</td>' +
                            '<td class="fw-medium text-secondary">' + item.namaProduk + '</td>' +
                            '<td class="text-center fw-bold">' + item.totalPakai + ' x</td>' +
                            '<td class="text-end px-4 fw-bold text-primary">' + formatRp<%=rnd%>(item.totalNominal) + '</td>' +
                        '</tr>';
            });
        }
        tbody.innerHTML = html;
        document.getElementById('pageNumberAturan<%=rnd%>').innerText = aturanPage<%=rnd%> + " / " + totalPages;
        document.getElementById('pagingInfoAturan<%=rnd%>').innerText = (totalRecords > 0 ? (startIndex + 1) : 0) + " - " + endIndex + " dari " + totalRecords;
        document.getElementById('btnPrevAturan<%=rnd%>').disabled = (aturanPage<%=rnd%> === 1);
        document.getElementById('btnNextAturan<%=rnd%>').disabled = (aturanPage<%=rnd%> === totalPages);
    };

    const changePageAturan<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(getFilteredAturanData<%=rnd%>().length / aturanLimit<%=rnd%>) || 1;
        if (direction === -1 && aturanPage<%=rnd%> > 1) { aturanPage<%=rnd%>--; renderTabelAturan<%=rnd%>(); }
        else if (direction === 1 && aturanPage<%=rnd%> < totalPages) { aturanPage<%=rnd%>++; renderTabelAturan<%=rnd%>(); }
    };

    const downloadTabelAturanExcel<%=rnd%> = () => {
        const columns = [
            { header: "Nama Aturan Promo", key: "namaAturan" },
            { header: "Tipe Eksekusi", key: "tipeEksekusi" },
            { header: "Target Produk", key: "namaProduk" },
            { header: "Total Digunakan (Kali)", key: "totalPakai" },
            { header: "Total Biaya Promo (Rp)", key: "totalNominal" }
        ];
        generateExcelFile<%=rnd%>(getFilteredAturanData<%=rnd%>(), columns, "Dampak_Aturan_Diskon");
    };


    // ==========================================
    // SALDO MEMBER REGULER SPECIAL (MRS)
    // Saldo Awal = total top-up; Terpakai = total belanja; Sisa = Awal - Terpakai.
    // ==========================================
    const loadMrs<%=rnd%> = async () => {
        const sqlMrs = "SELECT ak.kode_identitas, ak.nama, " +
            "COALESCE((SELECT SUM(dp.nominal) FROM public.deposit dp WHERE dp.anggota_koperasi = ak.id),0) AS saldo_awal, " +
            "COALESCE((SELECT SUM(pb.total) FROM koperasi.pembelian pb INNER JOIN koperasi.cara_pembayaran_koperasi cpk ON pb.cara_pembayaran_koperasi = cpk.id WHERE pb.anggota_koperasi = ak.id AND cpk.manual = false),0) AS terpakai " +
            "FROM koperasi.anggota_koperasi ak INNER JOIN koperasi.jenis_anggota_koperasi jak ON ak.jenis_anggota_koperasi = jak.id " +
            "WHERE (ak.aktif = true OR ak.aktif IS NULL) AND (LOWER(jak.nama) LIKE '%reguler%spe%' OR ak.kode_identitas ILIKE 'MRS%') " +
            "ORDER BY saldo_awal DESC LIMIT 500;";
        const res = await fetchApiData<%=rnd%>(sqlMrs);
        mrsDataList<%=rnd%> = (res || []).map(r => {
            const awal = parseFloat(r.saldo_awal) || 0;
            const terpakai = parseFloat(r.terpakai) || 0;
            return {
                nama: (r.kode_identitas ? r.kode_identitas + ' — ' : '') + (r.nama || '-'),
                awal: awal,
                terpakai: terpakai,
                sisa: awal - terpakai
            };
        });
        mrsPage<%=rnd%> = 1;
        const sb = document.getElementById('searchMrs<%=rnd%>');
        if (sb) sb.value = '';
        renderTabelMrs<%=rnd%>();
    };

    const getFilteredMrsData<%=rnd%> = () => {
        const keyword = document.getElementById('searchMrs<%=rnd%>').value.toLowerCase();
        if (keyword === "") return mrsDataList<%=rnd%>;
        return mrsDataList<%=rnd%>.filter(item => item.nama.toLowerCase().includes(keyword));
    };

    const filterTabelMrs<%=rnd%> = () => { mrsPage<%=rnd%> = 1; renderTabelMrs<%=rnd%>(); };

    const renderTabelMrs<%=rnd%> = () => {
        const tbody = document.getElementById('tabelMrs<%=rnd%>');
        let html = '';
        const filtered = getFilteredMrsData<%=rnd%>();
        const totalRecords = filtered.length;
        const totalPages = Math.ceil(totalRecords / mrsLimit<%=rnd%>) || 1;
        if (mrsPage<%=rnd%> > totalPages) mrsPage<%=rnd%> = totalPages;
        if (mrsPage<%=rnd%> < 1) mrsPage<%=rnd%> = 1;
        const startIndex = (mrsPage<%=rnd%> - 1) * mrsLimit<%=rnd%>;
        const endIndex = Math.min(startIndex + mrsLimit<%=rnd%>, totalRecords);
        const currentData = filtered.slice(startIndex, endIndex);
        if (currentData.length === 0) {
            html = '<tr><td colspan="4" class="text-center text-muted py-4"><i class="fas fa-wallet opacity-25 fa-2x mb-2 d-block"></i><%=Common.getBahasaConfig("Belum ada member Reguler Special (MRS) atau belum ada top-up.")%></td></tr>';
        } else {
            currentData.forEach(item => {
                html += '<tr>' +
                            '<td class="px-4 fw-bold text-dark">' + item.nama + '</td>' +
                            '<td class="text-end fw-bold text-primary">' + formatRp<%=rnd%>(item.awal) + '</td>' +
                            '<td class="text-end fw-bold text-danger">' + formatRp<%=rnd%>(item.terpakai) + '</td>' +
                            '<td class="text-end px-4 fw-bolder text-success">' + formatRp<%=rnd%>(item.sisa) + '</td>' +
                        '</tr>';
            });
        }
        tbody.innerHTML = html;
        document.getElementById('pageNumberMrs<%=rnd%>').innerText = mrsPage<%=rnd%> + " / " + totalPages;
        document.getElementById('pagingInfoMrs<%=rnd%>').innerText = (totalRecords > 0 ? (startIndex + 1) : 0) + " - " + endIndex + " <%=Common.getBahasaConfig("dari")%> " + totalRecords + " member";
        document.getElementById('btnPrevMrs<%=rnd%>').disabled = (mrsPage<%=rnd%> === 1);
        document.getElementById('btnNextMrs<%=rnd%>').disabled = (mrsPage<%=rnd%> === totalPages);
    };

    const changePageMrs<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(getFilteredMrsData<%=rnd%>().length / mrsLimit<%=rnd%>) || 1;
        if (direction === -1 && mrsPage<%=rnd%> > 1) { mrsPage<%=rnd%>--; renderTabelMrs<%=rnd%>(); }
        else if (direction === 1 && mrsPage<%=rnd%> < totalPages) { mrsPage<%=rnd%>++; renderTabelMrs<%=rnd%>(); }
    };

    const downloadTabelMrsExcel<%=rnd%> = () => {
        const columns = [
            { header: "Nama Member", key: "nama" },
            { header: "Total Saldo Awal (Top-Up)", key: "awal" },
            { header: "Total Terpakai (Belanja)", key: "terpakai" },
            { header: "Sisa Saldo", key: "sisa" }
        ];
        generateExcelFile<%=rnd%>(getFilteredMrsData<%=rnd%>(), columns, "Saldo_Member_MRS");
    };

    document.addEventListener("DOMContentLoaded", async () => {
        setDefaultCustomDates<%=rnd%>();
        await initFilterToko<%=rnd%>();
        await initFilterJenisAnggota<%=rnd%>();
        loadDashboardPromo<%=rnd%>();
        loadMrs<%=rnd%>();
    });
</script>