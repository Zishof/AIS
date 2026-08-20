<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.sekolah.CalonSiswa"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="org.hibernate.Session"%>
<%@page import="java.util.Calendar"%>
<%@page import="ais.ui.util.WaktuUtil"%>

<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
        return;
    }

    boolean edit = false;
    boolean add = false;
    boolean delete = false;
    boolean isAdmin = false;
    if(tbmuser != null && tbmuser.getUserId() != null){
        edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser);
        delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE, tbmuser);
        add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE, tbmuser);
        
        if(Common.getApakahAdminLain(tbmuser)){
            edit = true;
            delete = true; add = true; isAdmin = true;
        }
    }

    String paramHanyaSiswa = request.getParameter("hanya_untuk_siswa");
    boolean isParamHanyaSiswaSet = paramHanyaSiswa != null && !paramHanyaSiswa.trim().isEmpty();
    boolean valHanyaSiswa = "true".equalsIgnoreCase(paramHanyaSiswa);

    String rnd = request.getParameter("rnd");
    if(rnd == null) rnd = Common.getGeneratedBarCode(7);

    Calendar cal = WaktuUtil.getCalendar();
    int currBulan = cal.get(Calendar.MONTH) + 1;
    int currTahun = cal.get(Calendar.YEAR);
    String pSiswa = request.getParameter("siswa");
    String pCalonSiswa = request.getParameter("calon_siswa");
    
    String lockedId = "";
    String lockedName = "";
    boolean lockedKhususSiswa = true;
    boolean isLocked = false;
    
    Session sess = null;
    try {
        sess = HibernateUtil.openSession();
        if (tbmuser.getSiswa() != null) {
            lockedId = String.valueOf(tbmuser.getSiswa().getId());
            lockedName = tbmuser.getSiswa().getNama();
            lockedKhususSiswa = true;
            isLocked = true;
        } else if (tbmuser.getCalonSiswa() != null) {
            lockedId = String.valueOf(tbmuser.getCalonSiswa().getId());
            lockedName = tbmuser.getCalonSiswa().getNama();
            lockedKhususSiswa = false;
            isLocked = true;
        } else if (pSiswa != null && !pSiswa.isEmpty()) {
            Siswa sw = (Siswa) ConstantValues.ambil(Siswa.class.getName(), Long.parseLong(pSiswa), true);
            if (sw != null) { lockedId = pSiswa; lockedName = sw.getNama(); lockedKhususSiswa = true; isLocked = true; }
        } else if (pCalonSiswa != null && !pCalonSiswa.isEmpty()) {
            CalonSiswa cw = (CalonSiswa) ConstantValues.ambil(CalonSiswa.class.getName(), Long.parseLong(pCalonSiswa), true);
            if (cw != null) { lockedId = pCalonSiswa; lockedName = cw.getNama(); lockedKhususSiswa = false; isLocked = true; }
        }
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/bayar/pembayaran_online.jsp:74");
    } finally {
        if(sess != null && sess.isOpen()) {
            sess.disconnect();
            sess.close();
        }
    }
%>

<script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<style>
    .bg-gradient-primary-<%=rnd%> { background: linear-gradient(135deg, #0d6efd, #0a58ca); color: white; }
    .card-tagihan-<%=rnd%> { transition: all 0.2s ease; border-left: 4px solid #0d6efd !important; }
    .card-tagihan-<%=rnd%>:hover { transform: translateY(-3px); box-shadow: 0 .5rem 1rem rgba(0,0,0,.15)!important; }
    .nav-tabs-custom-<%=rnd%> .nav-link { font-weight: bold; color: #6c757d; border: none; border-bottom: 3px solid transparent; padding: 1rem 1.5rem; }
    .nav-tabs-custom-<%=rnd%> .nav-link.active { color: #0d6efd; border-bottom-color: #0d6efd; background: transparent; }
    .nav-tabs-custom-<%=rnd%> .nav-link:hover:not(.active) { border-bottom-color: #dee2e6; }
</style>

<div class="row g-3 mb-4 animate__animated animate__fadeInDown">
    <div class="col-12">
        <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4 bg-light">
            <div class="card-body p-4">
                <div class="row g-3 align-items-end">
                    
                    <% if (isLocked) { %>
                    <div class="col-md-5">
                        <label class="form-label small fw-bold text-secondary mb-1"><i class="fas fa-user-graduate me-1"></i><%=Common.getBahasaConfig("Data Terkunci")%></label>
                        <input type="text" class="form-control border-0 shadow-sm fw-bold bg-white text-primary" value="<%=lockedName%> (<%=lockedKhususSiswa ? "Siswa" : "Calon Siswa"%>)" readonly>
                        <input type="hidden" id="idSiswaSelected<%=rnd%>" value="<%=lockedId%>">
                        <input type="hidden" id="khususUntukSiswa<%=rnd%>" value="<%=lockedKhususSiswa%>">
                    </div>
                    <% } else { %>
                        <% if(isParamHanyaSiswaSet) { %>
                            <input type="hidden" id="khususUntukSiswa<%=rnd%>" value="<%=valHanyaSiswa%>">
                            <div class="col-md-5">
                                <label class="form-label small fw-bold text-secondary mb-1"><i class="fas fa-user-graduate me-1"></i><%=Common.getBahasaConfig(valHanyaSiswa ? "Data Pelanggan (Siswa)" : "Data Pelanggan (Calon Siswa)")%></label>
                                <div class="input-group shadow-sm">
                                    <input type="text" class="form-control border-0 fw-semibold bg-white text-dark" id="inputSiswa<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Pilih dari Data Picker...")%>" readonly>
                                    <button class="btn btn-primary border-0 fw-bold px-3" type="button" onclick="bukaDataPicker<%=rnd%>()"><i class="fas fa-search"></i></button>
                                </div>
                                <input type="hidden" id="idSiswaSelected<%=rnd%>">
                            </div>
                        <% } else { %>
                            <div class="col-md-2">
                                <label class="form-label small fw-bold text-secondary mb-1"><i class="fas fa-users me-1"></i><%=Common.getBahasaConfig("Kategori")%></label>
                                <select class="form-select border-0 shadow-sm fw-semibold" id="khususUntukSiswa<%=rnd%>" onchange="resetDataSiswa<%=rnd%>()">
                                    <option value="true"><%=Common.getBahasaConfig("Siswa Aktif")%></option>
                                    <option value="false"><%=Common.getBahasaConfig("Calon Siswa")%></option>
                                </select>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary mb-1"><i class="fas fa-user-graduate me-1"></i><%=Common.getBahasaConfig("Data Pelanggan")%></label>
                                <div class="input-group shadow-sm">
                                    <input type="text" class="form-control border-0 fw-semibold bg-white text-dark" id="inputSiswa<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Pilih dari Data Picker...")%>" readonly>
                                    <button class="btn btn-primary border-0 fw-bold px-3" type="button" onclick="bukaDataPicker<%=rnd%>()"><i class="fas fa-search"></i></button>
                                </div>
                                <input type="hidden" id="idSiswaSelected<%=rnd%>">
                            </div>
                        <% } %>
                    <% } %>

                    <div class="col-md-2">
                        <label class="form-label small fw-bold text-secondary mb-1"><i class="far fa-calendar-alt me-1"></i><%=Common.getBahasaConfig("Bulan")%></label>
                        <select class="form-select border-0 shadow-sm fw-semibold" id="selectBulan<%=rnd%>" onchange="loadTagihan<%=rnd%>()">
                            <% for(int i=1; i<=12; i++) { %>
                                <option value="<%=i%>" <%= (i == currBulan) ? "selected" : "" %>><%=Common.BULAN[i-1]%></option>
                            <% } %>
                        </select>
                    </div>
                    
                    <div class="col-md-2">
                        <label class="form-label small fw-bold text-secondary mb-1"><i class="far fa-calendar me-1"></i><%=Common.getBahasaConfig("Tahun")%></label>
                        <select class="form-select border-0 shadow-sm fw-semibold" id="selectTahun<%=rnd%>" onchange="loadTagihan<%=rnd%>()">
                            <% for(int i=currTahun-5; i<=currTahun+5; i++) { %>
                                <option value="<%=i%>" <%= (i == currTahun) ? "selected" : "" %>><%=i%></option>
                            <% } %>
                        </select>
                    </div>

                    <div class="col-md-3 text-end d-flex gap-2 justify-content-end">
                        <button class="btn btn-primary shadow-sm px-4 fw-bold" onclick="loadTagihan<%=rnd%>()"><i class="fas fa-search me-2"></i><%=Common.getBahasaConfig("Tampilkan")%></button>
                        <button class="btn btn-outline-secondary shadow-sm" onclick="refreshDataSiswa<%=rnd%>()" title="<%=Common.getBahasaConfig("Segarkan Data Tagihan Pelanggan")%>"><i class="fas fa-sync-alt"></i></button>
                    </div>
                </div>
                
                <div class="row mt-4 pt-3 border-top d-flex justify-content-between align-items-center">
                    <div class="col-md-8 d-flex gap-4 align-items-center">
                        <% if (!isLocked || (tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null && tbmuser.getOrangTua() == null)) { %>
                        <div class="form-check form-switch">
                            <input class="form-check-input shadow-sm" type="checkbox" id="cbKustom<%=rnd%>" onchange="loadTagihan<%=rnd%>()">
                            <label class="form-check-label small fw-bold text-secondary" for="cbKustom<%=rnd%>"><%=Common.getBahasaConfig("Izinkan Pilihan Kustom Per Item Biaya")%></label>
                        </div>
                        <div class="form-check form-switch">
                            <input class="form-check-input shadow-sm" type="checkbox" id="cbBukanTagihan<%=rnd%>" onchange="loadTagihan<%=rnd%>()">
                            <label class="form-check-label small fw-bold text-secondary" for="cbBukanTagihan<%=rnd%>"><%=Common.getBahasaConfig("Tampilkan Pilihan Bukan Tagihan")%></label>
                        </div>
                        <% } %>
                    </div>
                    <% if (tbmuser.getCalonSiswa() == null && (!isParamHanyaSiswaSet || valHanyaSiswa)) { %>
                    <div class="col-md-4 text-end">
                        <button class="btn btn-outline-primary btn-sm fw-bold shadow-sm" onclick="bukaModalSuratTagihan<%=rnd%>()">
                            <i class="fas fa-file-invoice me-2"></i><%=Common.getBahasaConfig("Surat Tagihan")%>
                        </button>
                    </div>
                    <% } %>
                </div>
            </div>
        </div>
    </div>
</div>

<div class="row g-4 animate__animated animate__fadeInUp">
    <div class="col-lg-8">
        <div class="card border-0 shadow-sm rounded-4 bg-white overflow-hidden">
            <ul class="nav nav-tabs nav-tabs-custom-<%=rnd%> bg-light border-bottom-0" role="tablist">
                <li class="nav-item" role="presentation">
                    <button class="nav-link active" id="tab-tagihan-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#pane-tagihan-<%=rnd%>" type="button" role="tab">
                        <i class="fas fa-file-invoice-dollar me-2"></i><%=Common.getBahasaConfig("Daftar Tagihan")%>
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" id="tab-riwayat-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#pane-riwayat-<%=rnd%>" type="button" role="tab">
                        <i class="fas fa-history me-2"></i><%=Common.getBahasaConfig("Riwayat Pembayaran")%>
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" id="tab-dasbor-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#pane-dasbor-<%=rnd%>" type="button" role="tab">
                        <i class="fas fa-chart-pie me-2"></i><%=Common.getBahasaConfig("Dasbor Pembayaran")%>
                    </button>
                </li>
            </ul>

            <div class="tab-content">
                <div class="tab-pane fade show active" id="pane-tagihan-<%=rnd%>" role="tabpanel">
                    <div class="card-body p-4 bg-light" id="containerTagihan<%=rnd%>">
                        <div class="text-center py-5 text-muted">
                            <i class="fas fa-search-dollar fa-3x mb-3 opacity-50"></i><br>
                            <%=Common.getBahasaConfig("Silakan lengkapi pencarian untuk melihat rincian tagihan.")%>
                        </div>
                    </div>
                </div>
                <div class="tab-pane fade" id="pane-riwayat-<%=rnd%>" role="tabpanel">
                    <div class="card-body p-4 bg-white" id="wrapperRiwayat<%=rnd%>">
                        <div class="row g-2 mb-3 bg-light p-3 rounded-3 border">
                            <div class="col-md-2">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Mulai Tgl")%></label>
                                <input type="date" class="form-control form-control-sm shadow-sm" id="filterRiwayatStart<%=rnd%>">
                            </div>
                            <div class="col-md-2">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Sampai Tgl")%></label>
                                <input type="date" class="form-control form-control-sm shadow-sm" id="filterRiwayatEnd<%=rnd%>">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Item Biaya")%></label>
                                <input type="text" class="form-control form-control-sm shadow-sm" id="filterRiwayatKeyword<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari nama item...")%>">
                            </div>
                            <div class="col-md-2">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Bulan")%></label>
                                <select class="form-select form-select-sm shadow-sm" id="filterRiwayatBulan<%=rnd%>">
                                    <option value="0"><%=Common.getBahasaConfig("Semua")%></option>
                                    <% for(int i=1; i<=12; i++) { %>
                                        <option value="<%=i%>"><%=Common.BULAN[i-1]%></option>
                                    <% } %>
                                </select>
                            </div>
                            <div class="col-md-2">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Tahun")%></label>
                                <select class="form-select form-select-sm shadow-sm" id="filterRiwayatTahun<%=rnd%>">
                                    <option value="0"><%=Common.getBahasaConfig("Semua")%></option>
                                    <% for(int i=currTahun-5; i<=currTahun+5; i++) { %>
                                        <option value="<%=i%>"><%=i%></option>
                                    <% } %>
                                </select>
                            </div>
                            <div class="col-md-1 d-flex align-items-end">
                                <button class="btn btn-primary btn-sm w-100 fw-bold shadow-sm" onclick="applyFilterRiwayat<%=rnd%>()"><i class="fas fa-search"></i></button>
                            </div>
                        </div>
                        
                        <div id="containerRiwayat<%=rnd%>">
                            <div class="text-center py-5 text-muted">
                                <i class="fas fa-receipt fa-3x mb-3 opacity-50"></i><br>
                                <%=Common.getBahasaConfig("Belum ada riwayat pembayaran yang dimuat.")%>
                            </div>
                        </div>
                        
                        <div class="d-flex justify-content-between align-items-center mt-3 pt-3 border-top" id="paginationRiwayat<%=rnd%>" style="display:none !important;">
                            <span class="small fw-bold text-secondary" id="riwayatPageInfo<%=rnd%>"></span>
                            <div class="btn-group shadow-sm">
                                <button class="btn btn-sm btn-outline-primary fw-bold px-3" id="btnPrevRiwayat<%=rnd%>" onclick="changePageRiwayat<%=rnd%>(-1)"><i class="fas fa-chevron-left me-1"></i><%=Common.getBahasaConfig("Sebelumnya")%></button>
                                <button class="btn btn-sm btn-outline-primary fw-bold px-3" id="btnNextRiwayat<%=rnd%>" onclick="changePageRiwayat<%=rnd%>(1)"><%=Common.getBahasaConfig("Selanjutnya")%><i class="fas fa-chevron-right ms-1"></i></button>
                            </div>
                        </div>
                        <button class="btn btn-success btn-sm fw-bold shadow-sm mb-3 float-end" onclick="exportRiwayatLokalCSV()"><i class="fas fa-file-excel me-2"></i>Export Excel</button>
                    </div>
                </div>
                <div class="tab-pane fade" id="pane-dasbor-<%=rnd%>" role="tabpanel">
                    <div class="card-body p-4 bg-white" id="containerDasbor<%=rnd%>">
                        <div class="row g-3 mb-4">
                            <div class="col-md-4">
                                <div class="card bg-danger bg-opacity-10 border-0 shadow-sm rounded-4 h-100">
                                    <div class="card-body text-center">
                                        <h6 class="fw-bold text-danger"><i class="fas fa-exclamation-circle me-2"></i><%=Common.getBahasaConfig("Sisa Tagihan")%></h6>
                                        <h4 id="dashSisaTagihan<%=rnd%>" class="text-danger mb-0 fw-bold">Rp 0</h4>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-4">
                                <div class="card bg-success bg-opacity-10 border-0 shadow-sm rounded-4 h-100">
                                    <div class="card-body text-center">
                                        <h6 class="fw-bold text-success"><i class="fas fa-check-circle me-2"></i><%=Common.getBahasaConfig("Total Terbayar")%></h6>
                                        <h4 id="dashTotalBayar<%=rnd%>" class="text-success mb-0 fw-bold">Rp 0</h4>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-4">
                                <div class="card bg-info bg-opacity-10 border-0 shadow-sm rounded-4 h-100">
                                    <div class="card-body text-center">
                                        <h6 class="fw-bold text-info"><i class="fas fa-wallet me-2"></i><%=Common.getBahasaConfig("Sisa Tabungan")%></h6>
                                        <h4 id="dashSisaTabungan<%=rnd%>" class="text-info mb-0 fw-bold">Rp 0</h4>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="row g-3">
                            <div class="col-md-6">
                                <div class="card border border-light shadow-sm rounded-4 h-100">
                                    <div class="card-body">
                                        <h6 class="fw-bold text-secondary mb-3 text-center"><%=Common.getBahasaConfig("Komposisi Metode Pembayaran")%></h6>
                                        <canvas id="chartPie<%=rnd%>" style="max-height:250px;"></canvas>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="card border border-light shadow-sm rounded-4 h-100">
                                    <div class="card-body">
                                        <h6 class="fw-bold text-secondary mb-3 text-center"><%=Common.getBahasaConfig("Tren 6 Pembayaran Terakhir")%></h6>
                                        <canvas id="chartBar<%=rnd%>" style="max-height:250px;"></canvas>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="col-lg-4">
        <div class="card border-0 shadow-sm rounded-4 d-flex flex-column h-100">
            <div class="card-header bg-gradient-primary-<%=rnd%> border-bottom-0 pt-4 pb-3 px-4">
                <h5 class="fw-bold text-white mb-0"><i class="fas fa-calculator me-2"></i><%=Common.getBahasaConfig("Ringkasan Transaksi")%></h5>
            </div>
            <div class="card-body bg-white p-4 d-flex flex-column">
                
                <div id="panelTabungan<%=rnd%>" style="display:none;" class="mb-4 p-3 bg-light border border-success border-opacity-25 rounded-3 shadow-sm">
                    <h6 class="fw-bold text-success mb-2"><i class="fas fa-piggy-bank me-2"></i><%=Common.getBahasaConfig("Informasi Tabungan")%></h6>
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <span class="text-secondary small fw-bold"><%=Common.getBahasaConfig("Saldo Deposit Saat Ini:")%></span>
                        <span class="fw-bold fs-5 text-dark" id="lblSaldoDeposit<%=rnd%>">Rp 0</span>
                    </div>
                    <div class="form-check form-switch border-top pt-2">
                        <input class="form-check-input border-success" type="checkbox" id="cbGunakanTabungan<%=rnd%>" onchange="hitungTotalBayar<%=rnd%>()">
                        <label class="form-check-label small fw-bold text-success" for="cbGunakanTabungan<%=rnd%>"><%=Common.getBahasaConfig("Potong Saldo Tabungan")%></label>
                    </div>
                </div>

                <div class="d-flex justify-content-between mb-2">
                    <span class="text-secondary fw-semibold"><%=Common.getBahasaConfig("Subtotal Tagihan")%></span>
                    <span class="text-dark fw-bold fs-6" id="lblTotalTagihan<%=rnd%>">Rp 0</span>
                </div>
                <div class="d-flex justify-content-between mb-3 border-bottom pb-3">
                    <span class="text-secondary fw-semibold"><%=Common.getBahasaConfig("Pengurangan Tabungan")%></span>
                    <span class="text-danger fw-bold fs-6" id="lblPotonganTabungan<%=rnd%>">- Rp 0</span>
                </div>
                
                <div class="d-flex justify-content-between align-items-center mt-2 mb-1">
                    <h5 class="fw-bold text-dark mb-0"><%=Common.getBahasaConfig("Total Bayar")%></h5>
                    <h3 class="fw-bold text-primary mb-0" id="lblGrandTotal<%=rnd%>">Rp 0</h3>
                </div>

                <div id="containerTombolBayar<%=rnd%>" class="mt-4 d-grid gap-3">
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    var pEdit<%=rnd%> = <%=edit%>;
    var pAdd<%=rnd%> = <%=add%>;
    var pDelete<%=rnd%> = <%=delete%>;

    var arrTagihan<%=rnd%> = [];
    var arrRiwayat<%=rnd%> = [];
    var arrJenisPembayaran<%=rnd%> = [];
    var saldoTabungan<%=rnd%> = 0;
    var objChartPie<%=rnd%> = null;
    var objChartBar<%=rnd%> = null;
    
    var riwayatPage<%=rnd%> = 1;
    var riwayatPageSize<%=rnd%> = 15;

    var isRiwayatLoaded<%=rnd%> = false;
    var isDasborLoaded<%=rnd%> = false;
    var formatRp<%=rnd%> = function(angka) {
        return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);
    };

    var fetchData<%=rnd%> = async function(action, payload) {
        try {
            payload.action = action;
            const res = await fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=bayar&s=_pembayaran_online_services', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const textData = await res.text();
            try {
                return JSON.parse(textData);
            } catch(e) {
                return { status: 'HTML_SCRIPT', html: textData };
            }
        } catch (e) {
            console.error(e);
            tampilkanToast('<%=Common.getBahasaConfigJS("Koneksi ke peladen terputus.")%>', 'bg-danger text-white');
            return null;
        }
    };
    var injectModalPicker<%=rnd%> = function() {
        if (!document.getElementById('modalPicker<%=rnd%>')) {
            const modalHtml = 
            '<div class="modal fade" id="modalPicker<%=rnd%>" tabindex="-1" data-bs-backdrop="static">' +
                '<div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">' +
                    '<div class="modal-content border-0 shadow">' +
                        '<div class="modal-header bg-gradient-primary-<%=rnd%> text-white">' +
                            '<h5 class="modal-title fw-bold"><i class="fas fa-search me-2"></i><%=Common.getBahasaConfig("Pencarian Data")%></h5>' +
                            '<button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>' +
                        '</div>' +
                        '<div class="modal-body p-0" id="modalBodyPicker<%=rnd%>"></div>' +
                    '</div>' +
                '</div>' +
            '</div>';
            document.body.insertAdjacentHTML('beforeend', modalHtml);
        }
    };

    var injectModals<%=rnd%> = function() {
        if (!document.getElementById('modalAngsuran<%=rnd%>')) {
            const modalHtml = 
            '<div class="modal fade" id="modalAngsuran<%=rnd%>" data-bs-backdrop="static" tabindex="-1">' +
              '<div class="modal-dialog modal-dialog-centered">' +
                '<div class="modal-content border-0 shadow">' +
                  '<div class="modal-header bg-gradient-primary-<%=rnd%> text-white">' +
                    '<h5 class="modal-title fw-bold"><i class="fas fa-hand-holding-usd me-2"></i><%=Common.getBahasaConfig("Pendaftaran Angsuran")%></h5>' +
                    '<button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>' +
                  '</div>' +
                  '<div class="modal-body p-4">' +
                    '<input type="hidden" id="idTagihanAngsuran<%=rnd%>">' +
                    '<div class="mb-3">' +
                        '<label class="form-label text-secondary small fw-bold"><%=Common.getBahasaConfig("Komponen Biaya")%></label>' +
                        '<input type="text" class="form-control fw-bold bg-light border-0 shadow-sm" id="angsuranNamaBiaya<%=rnd%>" readonly>' +
                    '</div>' +
                    '<div class="mb-3">' +
                        '<label class="form-label text-secondary small fw-bold"><%=Common.getBahasaConfig("Maksimal Angsuran")%></label>' +
                        '<input type="text" class="form-control fw-bold text-danger bg-light border-0 shadow-sm" id="angsuranMaksimal<%=rnd%>" readonly>' +
                        '<input type="hidden" id="angsuranMaksimalVal<%=rnd%>">' +
                    '</div>' +
                    '<div class="mb-3">' +
                        '<label class="form-label text-primary small fw-bold"><%=Common.getBahasaConfig("Nominal Angsuran *")%></label>' +
                        '<div class="input-group shadow-sm">' +
                            '<span class="input-group-text fw-bold bg-white text-dark">Rp</span>' +
                            '<input type="number" class="form-control fw-bold fs-5 text-primary" id="angsuranNominalBayar<%=rnd%>" placeholder="0">' +
                        '</div>' +
                    '</div>' +
                    '<div class="mb-1">' +
                        '<label class="form-label text-secondary small fw-bold"><%=Common.getBahasaConfig("Keterangan Tambahan")%></label>' +
                        '<textarea class="form-control shadow-sm border-light" id="angsuranInformasi<%=rnd%>" rows="2" placeholder="<%=Common.getBahasaConfig("Catatan opsional...")%>"></textarea>' +
                    '</div>' +
                  '</div>' +
                  '<div class="modal-footer bg-light border-0">' +
                    '<button type="button" class="btn btn-outline-secondary fw-bold px-4" data-bs-dismiss="modal"><i class="fas fa-times me-2"></i><%=Common.getBahasaConfig("Batal")%></button>' +
                    '<button type="button" class="btn btn-primary fw-bold px-4 shadow-sm" onclick="simpanAngsuran<%=rnd%>()"><i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Angsuran")%></button>' +
                  '</div>' +
                '</div>' +
              '</div>' +
            '</div>' +
            '<div class="modal fade" id="modalConfirm<%=rnd%>" tabindex="-1" data-bs-backdrop="static">' +
                '<div class="modal-dialog modal-dialog-centered modal-sm">' +
                    '<div class="modal-content border-0 shadow">' +
                        '<div class="modal-body p-4 text-center">' +
                            '<i class="fas fa-question-circle fa-4x text-warning mb-3"></i>' +
                            '<h6 class="fw-bold mb-4 text-dark" id="confirmMsg<%=rnd%>"></h6>' +
                            '<div class="d-flex gap-2 justify-content-center">' +
                                '<button type="button" class="btn btn-light fw-bold px-4 shadow-sm" data-bs-dismiss="modal"><i class="fas fa-times me-2"></i><%=Common.getBahasaConfig("Batal")%></button>' +
                                '<button type="button" class="btn btn-primary fw-bold px-4 shadow-sm" id="btnConfirmYes<%=rnd%>"><i class="fas fa-check me-2"></i><%=Common.getBahasaConfig("Ya")%></button>' +
                            '</div>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>' +
            '<div class="modal fade" id="modalVAInfo<%=rnd%>" data-bs-backdrop="static" tabindex="-1">' +
                '<div class="modal-dialog modal-lg modal-dialog-centered">' +
                    '<div class="modal-content border-0 shadow">' +
                        '<div class="modal-header bg-gradient-primary-<%=rnd%> text-white">' +
                            '<h5 class="modal-title fw-bold"><i class="fas fa-file-invoice me-2"></i><%=Common.getBahasaConfig("Instruksi Pembayaran")%></h5>' +
                            '<button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>' +
                        '</div>' +
                        '<div class="modal-body p-4">' +
                            '<table class="table table-borderless align-middle">' +
                                '<tr><td class="text-secondary fw-bold" style="width: 35%;"><%=Common.getBahasaConfig("Kode Pembayaran")%></td>' +
                                    '<td><span class="fs-4 fw-bold text-primary" id="vaKode<%=rnd%>"></span></td></tr>' +
                                '<tr><td class="text-secondary fw-bold"><%=Common.getBahasaConfig("Atas Nama")%></td>' +
                                    '<td><span class="fs-5 fw-bold text-dark" id="vaNama<%=rnd%>"></span></td></tr>' +
                                '<tr><td class="text-secondary fw-bold"><%=Common.getBahasaConfig("Nominal Pembayaran")%></td>' +
                                    '<td><span class="fs-5 fw-bold text-dark" id="vaNominal<%=rnd%>"></span></td></tr>' +
                                '<tr id="rowVaBiayaAdmin<%=rnd%>"><td class="text-secondary fw-bold"><%=Common.getBahasaConfig("Biaya Administrasi")%></td>' +
                                    '<td><span class="fs-5 fw-bold text-dark" id="vaBiayaAdmin<%=rnd%>"></span></td></tr>' +
                                '<tr id="rowVaTotal<%=rnd%>"><td class="text-secondary fw-bold"><%=Common.getBahasaConfig("Total Pembayaran")%></td>' +
                                    '<td><span class="fs-4 fw-bold text-danger" id="vaTotal<%=rnd%>"></span></td></tr>' +
                                '<tr><td class="text-secondary fw-bold"><%=Common.getBahasaConfig("Terbilang")%></td>' +
                                    '<td><span class="fst-italic text-muted" id="vaTerbilang<%=rnd%>"></span></td></tr>' +
                                '<tr><td class="text-secondary fw-bold"><%=Common.getBahasaConfig("Tanggal Kadaluarsa")%></td>' +
                                    '<td><span class="fs-5 fw-bold text-dark" id="vaKadaluarsa<%=rnd%>"></span></td></tr>' +
                            '</table>' +
                            '<hr>' +
                            '<div class="alert alert-info small">' +
                                '<i class="fas fa-info-circle me-2"></i><%=Common.getBahasaConfig("Catatlah kode pembayaran di atas, Anda bisa melakukan pembayaran via ATM, Teller atau Mobile Banking.")%><br>' +
                                '<i class="fas fa-info-circle me-2 mt-2"></i><span id="vaNotes<%=rnd%>"></span>' +
                            '</div>' +
                            '<div class="text-center mt-4" id="rowVaQR<%=rnd%>">' +
                                '<p class="fw-bold text-secondary mb-2"><%=Common.getBahasaConfig("QR-Code")%></p>' +
                                '<img id="vaQRImg<%=rnd%>" src="" alt="QR Code" style="width: 150px; height: 150px; object-fit: contain; border: 1px solid #ddd; padding: 5px; border-radius: 8px;">' +
                            '</div>' +
                        '</div>' +
                        '<div class="modal-footer bg-light border-0">' +
                            '<button type="button" class="btn btn-outline-secondary fw-bold px-4" data-bs-dismiss="modal"><i class="fas fa-times me-2"></i><%=Common.getBahasaConfig("Tutup")%></button>' +
                            '<button type="button" class="btn btn-primary fw-bold px-4 shadow-sm" onclick="cetakVa<%=rnd%>()"><i class="fas fa-print me-2"></i><%=Common.getBahasaConfig("Cetak")%></button>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>' +
            '<div class="modal fade" id="modalSuratTagihan<%=rnd%>" data-bs-backdrop="static" tabindex="-1">' +
                '<div class="modal-dialog modal-dialog-centered">' +
                    '<div class="modal-content border-0 shadow">' +
                        '<div class="modal-header bg-gradient-primary-<%=rnd%> text-white">' +
                            '<h5 class="modal-title fw-bold"><i class="fas fa-file-invoice me-2"></i><%=Common.getBahasaConfig("Pengaturan Surat Tagihan")%></h5>' +
                            '<button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>' +
                        '</div>' +
                        '<div class="modal-body p-4">' +
                            '<div class="mb-3">' +
                                '<label class="form-label text-secondary small fw-bold"><%=Common.getBahasaConfig("Tanggal Surat")%></label>' +
                                '<input type="date" class="form-control shadow-sm" id="stTanggalSurat<%=rnd%>">' +
                            '</div>' +
                            '<div class="mb-3">' +
                                '<label class="form-label text-secondary small fw-bold"><%=Common.getBahasaConfig("Tanggal Jatuh Tempo")%></label>' +
                                '<input type="date" class="form-control shadow-sm" id="stTanggalJatuhTempo<%=rnd%>">' +
                            '</div>' +
                            '<div class="mb-3">' +
                                '<label class="form-label text-secondary small fw-bold"><%=Common.getBahasaConfig("Nomor Surat")%></label>' +
                                '<input type="text" class="form-control shadow-sm" id="stNomorSurat<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Masukkan Nomor Surat...")%>">' +
                            '</div>' +
                            '<div class="mb-3">' +
                                '<label class="form-label text-secondary small fw-bold"><%=Common.getBahasaConfig("Cara Pembayaran")%></label>' +
                                '<select class="form-select shadow-sm" id="stCaraPembayaran<%=rnd%>"></select>' +
                            '</div>' +
                            '<div class="mb-3">' +
                                '<label class="form-label text-secondary small fw-bold"><%=Common.getBahasaConfig("Prosentase Denda (%)")%></label>' +
                                '<input type="number" class="form-control shadow-sm" id="stDenda<%=rnd%>" value="0.0" step="0.1">' +
                            '</div>' +
                            '<div class="mb-1">' +
                                '<label class="form-label text-secondary small fw-bold"><%=Common.getBahasaConfig("Catatan Tambahan")%></label>' +
                                '<textarea class="form-control shadow-sm" id="stCatatan<%=rnd%>" rows="3" placeholder="<%=Common.getBahasaConfig("Catatan di akhir surat...")%>"></textarea>' +
                            '</div>' +
                        '</div>' +
                        '<div class="modal-footer bg-light border-0 d-flex justify-content-between">' +
                            '<button type="button" class="btn btn-outline-secondary fw-bold px-3" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>' +
                            '<div>' +
                                '<button type="button" class="btn btn-success fw-bold px-3 shadow-sm me-2" onclick="eksekusiSuratTagihan<%=rnd%>(true)"><i class="fab fa-whatsapp me-2"></i><%=Common.getBahasaConfig("Kirim WA")%></button>' +
                                '<button type="button" class="btn btn-primary fw-bold px-4 shadow-sm" onclick="eksekusiSuratTagihan<%=rnd%>(false)"><i class="fas fa-print me-2"></i><%=Common.getBahasaConfig("Cetak PDF")%></button>' +
                            '</div>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
            document.body.insertAdjacentHTML('beforeend', modalHtml);
            
            document.getElementById('stTanggalSurat<%=rnd%>').valueAsDate = new Date();
            let calJT = new Date();
            calJT.setMonth(calJT.getMonth() + 1);
            document.getElementById('stTanggalJatuhTempo<%=rnd%>').valueAsDate = calJT;
            let dEnd = new Date();
            let dStart = new Date();
            dStart.setFullYear(dStart.getFullYear() - 3);
            document.getElementById('filterRiwayatStart<%=rnd%>').valueAsDate = dStart;
            document.getElementById('filterRiwayatEnd<%=rnd%>').valueAsDate = dEnd;
        }
    };

    var showConfirmModal<%=rnd%> = function(msg, callbackYes) {
        const formattedMsg = msg.replace(/\n/g, '<br>');
        document.getElementById('confirmMsg<%=rnd%>').innerHTML = formattedMsg;
        const modalEl = document.getElementById('modalConfirm<%=rnd%>');
        const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
        
        const btnYes = document.getElementById('btnConfirmYes<%=rnd%>');
        btnYes.onclick = () => {
            modal.hide();
            if(callbackYes) callbackYes();
        };
        modal.show();
    };

    var resetDataSiswa<%=rnd%> = function() {
        document.getElementById('idSiswaSelected<%=rnd%>').value = '';
        document.getElementById('inputSiswa<%=rnd%>').value = '';
        document.getElementById('containerTagihan<%=rnd%>').innerHTML = '<div class="text-center py-5 text-muted"><i class="fas fa-search-dollar fa-3x mb-3 opacity-50"></i><br><%=Common.getBahasaConfig("Silakan lengkapi pencarian untuk melihat rincian tagihan.")%></div>';
        document.getElementById('containerRiwayat<%=rnd%>').innerHTML = '<div class="text-center py-5 text-muted"><i class="fas fa-receipt fa-3x mb-3 opacity-50"></i><br><%=Common.getBahasaConfig("Belum ada riwayat pembayaran yang dimuat.")%></div>';
        document.getElementById('containerDasbor<%=rnd%>').style.opacity = '0.3';
        document.getElementById('containerTombolBayar<%=rnd%>').innerHTML = '';
        
        isRiwayatLoaded<%=rnd%> = false;
        isDasborLoaded<%=rnd%> = false;
        hitungTotalBayar<%=rnd%>();
    };
    
    var bukaDataPicker<%=rnd%> = function() {
        injectModalPicker<%=rnd%>();

        const isSiswaStr = document.getElementById('khususUntukSiswa<%=rnd%>').value;
        const isSiswa = isSiswaStr === 'true';
        
        const paramClass = isSiswa ? '<%=ais.database.model.sekolah.Siswa.class.getName()%>' : '<%=ais.database.model.sekolah.CalonSiswa.class.getName()%>';
        const paramCols = isSiswa ? 'id,nis,nama' : 'id,nopendaftaran,nama';
        const paramOrder = 'nama';
        const paramTitle = encodeURIComponent(isSiswa ? '<%=Common.getBahasaConfigJS("Pilih Pelanggan")%>' : '<%=Common.getBahasaConfigJS("Pilih Calon Pelanggan")%>');
        const cbName = 'callbackPicker<%=rnd%>';
        const paramIdsTerpilih = '';
        
        // --- BAGIAN YANG DIPERBAIKI (Penambahan &rnd=<%=rnd%>) ---
        const urlPicker = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&radio=true&p=common&s=_data_picker&class=' + paramClass + '&cols=' + paramCols + '&aktif=true&order=' + paramOrder + '&title=' + paramTitle + '&idsTerpilih=' + paramIdsTerpilih + '&callback=' + cbName + '&rnd=<%=rnd%>';
        // ---------------------------------------------------------

        const container = document.getElementById('modalBodyPicker<%=rnd%>');
        if (!container) {
            tampilkanToast('<%=Common.getBahasaConfigJS("Sistem sedang memuat komponen, silakan coba lagi.")%>', 'bg-warning text-dark');
            return;
        }

        container.innerHTML = '<div class="text-center p-5"><div class="spinner-border text-primary"></div><p class="mt-3 fw-bold text-secondary"><%=Common.getBahasaConfig("Mempersiapkan Papan Pilihan...")%></p></div>';
        const modalEl = document.getElementById('modalPicker<%=rnd%>');
        const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
        modal.show();
        
        fetch(urlPicker).then(function(response) { return response.text(); }).then(function(html) {
            container.innerHTML = html;
            Array.from(container.querySelectorAll("script")).forEach(function(oldScript) {
                var newScript = document.createElement("script");
                Array.from(oldScript.attributes).forEach(function(attr) { if (attr.name.toLowerCase() !== 'type') newScript.setAttribute(attr.name, attr.value); });
                newScript.type = 'text/javascript';
                newScript.appendChild(document.createTextNode(oldScript.innerHTML));
                oldScript.parentNode.replaceChild(newScript, oldScript);
            });
        }).catch(function(err) {
            container.innerHTML = '<div class="text-center p-5 text-danger"><i class="fas fa-exclamation-triangle fa-2x mb-3"></i><br><%=Common.getBahasaConfig("Gagal memuat Data Picker.")%></div>';
        });
    };

    window['callbackPicker<%=rnd%>'] = function(selectedArray) {
        if(selectedArray && selectedArray.length > 0) {
            const data = selectedArray[0];
            const id = data.id || data._id;
            
            const isSiswaStr = document.getElementById('khususUntukSiswa<%=rnd%>').value;
            const isSiswa = isSiswaStr === 'true';
            
            let kodeStr = '-';
            if (isSiswa && data.nis && data.nis !== 'null' && data.nis !== '-') {
                kodeStr = data.nis;
            } else if (!isSiswa && data.nopendaftaran && data.nopendaftaran !== 'null' && data.nopendaftaran !== '-') {
                kodeStr = data.nopendaftaran;
            }
            
            const nama = (data.nama && data.nama !== 'null') ? data.nama : '-';
            
            document.getElementById('idSiswaSelected<%=rnd%>').value = id;
            document.getElementById('inputSiswa<%=rnd%>').value = nama + ' (' + kodeStr + ')';
            
            const modalEl = document.getElementById('modalPicker<%=rnd%>');
            const modal = bootstrap.Modal.getInstance(modalEl);
            if(modal) modal.hide();
            
            loadTagihan<%=rnd%>();
        }
    };
    document.getElementById('tab-riwayat-<%=rnd%>').addEventListener('shown.bs.tab', function () {
        if(!isRiwayatLoaded<%=rnd%> && document.getElementById('idSiswaSelected<%=rnd%>').value) {
            applyFilterRiwayat<%=rnd%>();
        }
    });
    document.getElementById('tab-dasbor-<%=rnd%>').addEventListener('shown.bs.tab', function () {
        if(!isDasborLoaded<%=rnd%> && document.getElementById('idSiswaSelected<%=rnd%>').value) {
            loadDasborData<%=rnd%>();
        }
    });
    var loadTagihan<%=rnd%> = async function() {
        const idSiswa = document.getElementById('idSiswaSelected<%=rnd%>').value;
        const khususUntukSiswa = document.getElementById('khususUntukSiswa<%=rnd%>').value;
        
        if(!idSiswa) return;

        isRiwayatLoaded<%=rnd%> = false;
        isDasborLoaded<%=rnd%> = false;

        const container = document.getElementById('containerTagihan<%=rnd%>');
        container.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-primary" style="width: 3rem; height: 3rem;"></div><p class="mt-3 text-secondary fw-bold"><%=Common.getBahasaConfig("Mengambil Data Tagihan...")%></p></div>';
        const payload = {
            idSiswa: idSiswa,
            khususUntukSiswa: (khususUntukSiswa === 'true'),
            bulan: document.getElementById('selectBulan<%=rnd%>').value,
            tahun: document.getElementById('selectTahun<%=rnd%>').value,
            kustom: document.getElementById('cbKustom<%=rnd%>') ? document.getElementById('cbKustom<%=rnd%>').checked : false,
            bukanTagihan: document.getElementById('cbBukanTagihan<%=rnd%>') ? document.getElementById('cbBukanTagihan<%=rnd%>').checked : false,
            refresh: false
        };
        const res = await fetchData<%=rnd%>('loadTagihan', payload);
        
        if(res && res.status === '00') {
            arrTagihan<%=rnd%> = res.tagihan;
            arrJenisPembayaran<%=rnd%> = res.jenisPembayaran || [];
            saldoTabungan<%=rnd%> = res.tabungan || 0;
            
            renderTabunganUI<%=rnd%>();
            renderTagihanHTML<%=rnd%>();
            renderTombolBayar<%=rnd%>(res.metodeBayar);
            
            var activeTab = document.querySelector('.nav-tabs-custom-<%=rnd%> .nav-link.active').id;
            if (activeTab === 'tab-riwayat-<%=rnd%>') applyFilterRiwayat<%=rnd%>();
            if (activeTab === 'tab-dasbor-<%=rnd%>') loadDasborData<%=rnd%>();
        } else {
            container.innerHTML = '<div class="alert alert-danger shadow-sm border-0"><i class="fas fa-exclamation-triangle fa-2x mb-2"></i><br>' + (res ? res.message : '<%=Common.getBahasaConfigJS("Gagal memuat data.")%>') + '</div>';
        }
    };
    
    var applyFilterRiwayat<%=rnd%> = async function() {
        riwayatPage<%=rnd%> = 1;
        const idSiswa = document.getElementById('idSiswaSelected<%=rnd%>').value;
        if(!idSiswa) return;
        
        document.getElementById('containerRiwayat<%=rnd%>').innerHTML = '<div class="text-center py-5"><div class="spinner-border text-primary" style="width: 3rem; height: 3rem;"></div></div>';
        document.getElementById('paginationRiwayat<%=rnd%>').setAttribute('style', 'display: none !important;');
        
        const payload = {
            idSiswa: idSiswa,
            khususUntukSiswa: (document.getElementById('khususUntukSiswa<%=rnd%>').value === 'true'),
            riwayatStartDate: document.getElementById('filterRiwayatStart<%=rnd%>').value,
            riwayatEndDate: document.getElementById('filterRiwayatEnd<%=rnd%>').value,
            riwayatBulan: document.getElementById('filterRiwayatBulan<%=rnd%>').value,
            riwayatTahun: document.getElementById('filterRiwayatTahun<%=rnd%>').value,
            riwayatKeyword: document.getElementById('filterRiwayatKeyword<%=rnd%>').value,
            bukanTagihan: document.getElementById('cbBukanTagihan<%=rnd%>') ? document.getElementById('cbBukanTagihan<%=rnd%>').checked : false
        };

        const res = await fetchData<%=rnd%>('loadRiwayat', payload);
        if(res && res.status === '00') {
            arrRiwayat<%=rnd%> = res.riwayat || [];
            renderRiwayatHTML<%=rnd%>();
            isRiwayatLoaded<%=rnd%> = true;
        } else {
            document.getElementById('containerRiwayat<%=rnd%>').innerHTML = '<div class="alert alert-danger text-center"><i class="fas fa-exclamation-triangle fa-2x mb-2"></i><br>Gagal memuat riwayat.</div>';
        }
    };
    
    var changePageRiwayat<%=rnd%> = function(dir) {
        riwayatPage<%=rnd%> += dir;
        renderRiwayatHTML<%=rnd%>();
    };

    var loadDasborData<%=rnd%> = async function() {
        const idSiswa = document.getElementById('idSiswaSelected<%=rnd%>').value;
        if(!idSiswa) return;
        
        document.getElementById('containerDasbor<%=rnd%>').style.opacity = '0.3';
        const payload = {
            idSiswa: idSiswa,
            khususUntukSiswa: (document.getElementById('khususUntukSiswa<%=rnd%>').value === 'true')
        };
        const res = await fetchData<%=rnd%>('loadDasbor', payload);
        if(res && res.status === '00') {
            renderDasbor<%=rnd%>(res.totalBayar, res.komposisiMetode);
            isDasborLoaded<%=rnd%> = true;
            document.getElementById('containerDasbor<%=rnd%>').style.opacity = '1';
        }
    };
    var renderDasbor<%=rnd%> = function(totalBayar, komposisiMetode) {
        var totalTunggakan = 0;
        for(var i=0; i<arrTagihan<%=rnd%>.length; i++){
            totalTunggakan += parseFloat(arrTagihan<%=rnd%>[i].totalTampil);
        }
        
        document.getElementById('dashSisaTagihan<%=rnd%>').innerText = formatRp<%=rnd%>(totalTunggakan);
        document.getElementById('dashSisaTabungan<%=rnd%>').innerText = formatRp<%=rnd%>(saldoTabungan<%=rnd%>);
        document.getElementById('dashTotalBayar<%=rnd%>').innerText = formatRp<%=rnd%>(totalBayar || 0);

        if (typeof Chart !== 'undefined') {
            drawCharts<%=rnd%>(komposisiMetode);
        } else {
            setTimeout(function(){ 
                if(typeof Chart !== 'undefined') drawCharts<%=rnd%>(komposisiMetode); 
            }, 800);
        }
    };

    var drawCharts<%=rnd%> = function(dbData) {
        var labelsPie = [];
        var dataPie = [];
        
        if (dbData && dbData.length > 0 && dbData[0].label) {
            for(var i=0; i<dbData.length; i++) {
                labelsPie.push(dbData[i].label);
                dataPie.push(parseFloat(dbData[i].val || 0));
            }
        }

        var ctxPie = document.getElementById('chartPie<%=rnd%>');
        if (ctxPie) {
            if (objChartPie<%=rnd%>) objChartPie<%=rnd%>.destroy();
            objChartPie<%=rnd%> = new Chart(ctxPie, {
                type: 'doughnut',
                data: {
                    labels: labelsPie.length > 0 ? labelsPie : ['Kosong'],
                    datasets: [{
                        data: dataPie.length > 0 ? dataPie : [1],
                        backgroundColor: ['#0d6efd', '#198754', '#ffc107', '#dc3545', '#6c757d', '#0dcaf0'],
                        borderWidth: 0
                    }]
                },
                options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'right' } } }
            });
        }

        var labelsBar = [];
        var dataBar = [];
        var limitBar = Math.min(arrRiwayat<%=rnd%>.length, 6);
        
        for (var i = 0; i < limitBar; i++) {
            if(arrRiwayat<%=rnd%>[i]){
                labelsBar.unshift(arrRiwayat<%=rnd%>[i].waktu.substring(0, 10));
                dataBar.unshift(parseFloat(arrRiwayat<%=rnd%>[i].nominal));
            }
        }

        var ctxBar = document.getElementById('chartBar<%=rnd%>');
        if (ctxBar) {
            if (objChartBar<%=rnd%>) objChartBar<%=rnd%>.destroy();
            objChartBar<%=rnd%> = new Chart(ctxBar, {
                type: 'bar',
                data: {
                    labels: labelsBar.length > 0 ? labelsBar : ['Kosong'],
                    datasets: [{
                        label: 'Nominal Bayar',
                        data: dataBar.length > 0 ? dataBar : [0],
                        backgroundColor: '#0d6efd',
                        borderRadius: 4
                     }]
                },
                options: { 
                    responsive: true, 
                    maintainAspectRatio: false, 
                    plugins: { legend: { display: false } },
                    scales: { y: { beginAtZero: true } }
                }
            });
        }
    };


    var renderRiwayatHTML<%=rnd%> = function() {
        const container = document.getElementById('containerRiwayat<%=rnd%>');
        const paginator = document.getElementById('paginationRiwayat<%=rnd%>');
        
        if(!arrRiwayat<%=rnd%> || arrRiwayat<%=rnd%>.length === 0) {
            container.innerHTML = '<div class="text-center py-5 text-muted"><i class="fas fa-history fa-4x mb-3 opacity-50"></i><h5 class="fw-bold mt-2"><%=Common.getBahasaConfig("Belum Ada Riwayat")%></h5><p><%=Common.getBahasaConfig("Tidak ditemukan transaksi untuk filter ini.")%></p></div>';
            if(paginator) paginator.setAttribute('style', 'display: none !important;');
            return;
        }

        let groupedRiwayat = [];
        let currentGroupId = null;
        let currentGroupObj = null;

        arrRiwayat<%=rnd%>.forEach(r => {
            if(r.groupId !== currentGroupId) {
                currentGroupObj = {
                    groupId: r.groupId, // Nama Pengaturan Biaya
                    items: []
                };
                groupedRiwayat.push(currentGroupObj);
                currentGroupId = r.groupId;
            }
            currentGroupObj.items.push(r);
        });

        var totalData = arrRiwayat<%=rnd%>.length;
        var totalPages = Math.ceil(groupedRiwayat.length / riwayatPageSize<%=rnd%>);
        if (riwayatPage<%=rnd%> < 1) riwayatPage<%=rnd%> = 1;
        if (riwayatPage<%=rnd%> > totalPages) riwayatPage<%=rnd%> = totalPages;
        
        var startIdx = (riwayatPage<%=rnd%> - 1) * riwayatPageSize<%=rnd%>;
        var endIdx = startIdx + riwayatPageSize<%=rnd%>;
        var pagedGroups = groupedRiwayat.slice(startIdx, endIdx);

        // Header Menyerupai Kolom ZK Rows yang diminta
        let html = '<div class="table-responsive"><table class="table table-hover align-middle mb-0"><thead class="table-light shadow-sm" style="z-index: 1;"><tr>' +
                   '<th style="width: 16%;">' + '<%=Common.getBahasaConfigJS("Waktu")%>' + '</th>' +
                   '<th style="width: 16%;">' + '<%=Common.getBahasaConfigJS("Bln")%>' + '</th>' +
                   '<th style="width: 35%;">' + '<%=Common.getBahasaConfigJS("Item")%>' + '</th>' +
                   '<th>' + '<%=Common.getBahasaConfigJS("Via")%>' + '</th>' +
                   '<th class="text-end" style="width: 12%;">' + '<%=Common.getBahasaConfigJS("Nominal")%>' + '</th>' +
                   '<th class="text-center" style="width: 10%;"><i class="fas fa-cogs"></i></th>' +
                   '</tr></thead><tbody>';

        pagedGroups.forEach(g => {
            // Group Pengaturan Biaya Header Row
            html += '<tr class="bg-primary bg-opacity-10 border-bottom border-primary border-opacity-25">' +
                '<td colspan="6" class="fw-bold text-dark"><i class="fas fa-layer-group text-primary me-2"></i>' + g.groupId + '</td>' +
                '</tr>';
                
            g.items.forEach(r => {
                let actionHtml = '<button class="btn btn-sm btn-outline-secondary rounded-circle shadow-sm" onclick="cetakStrukUlang<%=rnd%>(' + r.idPembayaran + ')" title="' + '<%=Common.getBahasaConfigJS("Cetak Struk Transaksi")%>' + '"><i class="fas fa-print"></i></button>';
                if (pDelete<%=rnd%>) {
                    actionHtml += ' <button class="btn btn-sm btn-outline-danger rounded-circle shadow-sm ms-1" onclick="hapusRiwayat<%=rnd%>(' + r.idPembayaran + ')" title="' + '<%=Common.getBahasaConfigJS("Hapus Riwayat Pembayaran")%>' + '"><i class="fas fa-trash-alt"></i></button>';
                }

                let nominalHtml = '<div class="fw-bold text-success text-nowrap">' + formatRp<%=rnd%>(r.nominal) + '</div>';
                if(r.nominalCoret > 0.01) {
                    nominalHtml += '<div class="small text-muted text-decoration-line-through text-nowrap">' + formatRp<%=rnd%>(r.nominalCoret) + '</div>';
                }

                html += '<tr class="bg-white">' +
                    '<td class="text-secondary fw-medium">' + r.waktu + '</td>' +
                    '<td class="text-secondary fw-medium">' + r.bln + '</td>' +
                    '<td><span class="fw-bold text-dark">' + r.item + '</span></td>' +
                    '<td class="text-secondary">' + r.via + '</td>' +
                    '<td class="text-end">' + nominalHtml + '</td>' +
                    '<td class="text-center text-nowrap">' + actionHtml + '</td>' +
                    '</tr>';
            });
        });

        html += '</tbody></table></div>';
        container.innerHTML = html;
        
        if(paginator) {
            paginator.setAttribute('style', 'display: flex !important;');
            document.getElementById('riwayatPageInfo<%=rnd%>').innerText = 'Halaman ' + riwayatPage<%=rnd%> + ' dari ' + totalPages + ' (' + groupedRiwayat.length + ' Grup)';
            document.getElementById('btnPrevRiwayat<%=rnd%>').disabled = riwayatPage<%=rnd%> === 1;
            document.getElementById('btnNextRiwayat<%=rnd%>').disabled = riwayatPage<%=rnd%> === totalPages;
        }
    };
    
    var renderTabunganUI<%=rnd%> = function() {
        const panel = document.getElementById('panelTabungan<%=rnd%>');
        if(saldoTabungan<%=rnd%> > 0.1) {
            panel.style.display = 'block';
            document.getElementById('lblSaldoDeposit<%=rnd%>').innerText = formatRp<%=rnd%>(saldoTabungan<%=rnd%>);
        } else {
            panel.style.display = 'none';
            document.getElementById('cbGunakanTabungan<%=rnd%>').checked = false;
        }
    };

    var renderTagihanHTML<%=rnd%> = function() {
        const container = document.getElementById('containerTagihan<%=rnd%>');
        
        // Atur status centang default berdasarkan aturan wajibPilih dan wajibPilihJikaBulanDipilih
        arrTagihan<%=rnd%>.forEach(t => {
            let isWajib = t.wajibPilih === true || t.wajibPilihJikaBulanDipilih === true;
            t.checked = isWajib;
            t.disabled = isWajib;
        });

        if(!arrTagihan<%=rnd%> || arrTagihan<%=rnd%>.length === 0) {
            container.innerHTML = '<div class="text-center py-5 text-muted"><i class="fas fa-check-circle fa-4x mb-3 text-success opacity-75"></i><h5 class="fw-bold mt-2"><%=Common.getBahasaConfig("Seluruh Tagihan Lunas!")%></h5><p><%=Common.getBahasaConfig("Tidak ditemukan tagihan tertunggak untuk parameter ini.")%></p></div>';
            hitungTotalBayar<%=rnd%>();
            return;
        }

        let html = '';
        let currentGroup = '';
        arrTagihan<%=rnd%>.forEach(t => {
            if(t.group !== currentGroup) {
                html += '<h6 class="fw-bold text-secondary border-bottom border-2 pb-2 mt-4 mb-3"><i class="fas fa-layer-group me-2 text-primary"></i>' + t.group + '</h6>';
                currentGroup = t.group;
            }

            const checkedHtml = t.checked ? 'checked' : '';
            const disabledHtml = t.disabled ? 'disabled' : '';
            const showHapusBtn = t.bisaHapusAngsuran && !t.bisaDiangsur;

            html += '<div class="card card-tagihan-<%=rnd%> bg-white border-0 shadow-sm mb-3 rounded-4 overflow-hidden">' +
                '<div class="card-body p-3 d-flex align-items-center justify-content-between">' +
                    '<div class="form-check d-flex align-items-center gap-3 w-75 m-0 p-0 ps-2">' +
                        '<input class="form-check-input border-secondary shadow-sm m-0" type="checkbox" style="transform: scale(1.4); cursor:pointer;" ' +
                               'id="cbTagihan_<%=rnd%>_' + t.id + '" value="' + t.id + '" ' +
                               'onchange="handleCheckboxLogic<%=rnd%>(\'' + t.id + '\')" ' + checkedHtml + ' ' + disabledHtml + '>' +
                        '<label class="form-check-label w-100 ps-2" for="cbTagihan_<%=rnd%>_' + t.id + '" style="cursor:pointer;">' +
                            '<div class="fw-bold text-dark fs-6">' + t.namaBiaya + '</div>' +
                            '<div class="mt-1">' +
                                '<span class="badge bg-light text-secondary border"><i class="far fa-calendar-alt me-1"></i> ' + t.periodeStr + '</span>' +
                                (t.denda > 0 ? '<span class="badge bg-danger text-white ms-1 shadow-sm"><i class="fas fa-exclamation-circle me-1"></i>Denda ' + formatRp<%=rnd%>(t.denda) + '</span>' : '') +
                                (t.tglDeadline ? '<span class="badge bg-warning text-dark ms-1 shadow-sm"><i class="fas fa-clock me-1"></i>DL: ' + t.tglDeadline + '</span>' : '') +
                                (t.va ? '<div class="text-danger fw-bold small mt-1"><i class="fas fa-qrcode me-1"></i>VA: ' + t.va + '</div>' : '') +
                            '</div>' +
                        '</label>' +
                    '</div>' +
                    '<div class="text-end w-25">' +
                        (t.bisaUbahNominal ? 
                            '<div class="input-group input-group-sm shadow-sm justify-content-end">' +
                                '<span class="input-group-text bg-white border-end-0 text-muted">Rp</span>' +
                                '<input type="number" class="form-control fw-bold text-primary border-start-0 text-end" style="max-width: 130px;" ' +
                                    'id="nomTagihan_<%=rnd%>_' + t.id + '" value="' + t.nominal + '" onchange="updateNominalTagihan<%=rnd%>(\'' + t.id + '\', this.value)">' +
                            '</div>' 
                            : 
                            '<div class="fw-bold fs-5 text-success">' + formatRp<%=rnd%>(t.totalTampil) + '</div>'
                        ) +
                        '<div class="mt-2 d-flex gap-1 justify-content-end">' +
                            (t.bisaDiangsur ? '<button class="btn btn-sm btn-outline-primary rounded-pill px-3 shadow-sm" onclick="bukaModalAngsuran<%=rnd%>(\'' + t.id + '\')"><i class="fas fa-plus me-1"></i>Angsur</button>' : '') +
                            (showHapusBtn ? '<button class="btn btn-sm btn-outline-danger rounded-pill px-3 shadow-sm" onclick="hapusAngsuran<%=rnd%>(\'' + t.id + '\')"><i class="fas fa-trash-alt me-1"></i>Hapus</button>' : '') +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
        });

        container.innerHTML = html;

        // Picu pendaftaran dependensi layaknya logic 'handleDependencies' pada sistem asli
        // Untuk item yang memiliki state default checked (karena wajibPilih atau wajibPilihJikaBulanDipilih)
        arrTagihan<%=rnd%>.forEach(t => {
            let isWajib = t.wajibPilih === true || t.wajibPilihJikaBulanDipilih === true;
            if (isWajib) {
                handleCheckboxLogic<%=rnd%>(t.id);
            }
        });

        hitungTotalBayar<%=rnd%>();
    };

    var checkUlangHarusBayar<%=rnd%> = function(harusBayars, tagihanObj) {
        arrTagihan<%=rnd%>.forEach(tag => {
            if (tagihanObj.harusBayarId && tag.idJenisBiaya == tagihanObj.harusBayarId) {
                if (tag.harusBayarId) {
                    harusBayars.push(tag.harusBayarId);
                    checkUlangHarusBayar<%=rnd%>(harusBayars, tag);
                }
            }
        });
    };

    var handleCheckboxLogic<%=rnd%> = function(idTrigger) {
        const isKustom = document.getElementById('cbKustom<%=rnd%>') ? document.getElementById('cbKustom<%=rnd%>').checked : false;
        if(isKustom) { hitungTotalBayar<%=rnd%>(); return; }

        const triggerCb = document.getElementById('cbTagihan_<%=rnd%>_' + idTrigger);
        if(!triggerCb) return;
        const pilih = triggerCb.checked;
        const tagihan = arrTagihan<%=rnd%>.find(t => t.id == idTrigger);

        if(!tagihan) return;
        if (tagihan.wajibDibayarSebelumnya && tagihan.wajibDibayarSebelumnya.trim() !== '') {
            arrTagihan<%=rnd%>.forEach(t => {
                const cb = document.getElementById('cbTagihan_<%=rnd%>_' + t.id);
                if (cb && tagihan.wajibDibayarSebelumnya.includes("," + t.id + ",")) {
                    cb.disabled = false;
                    cb.checked = false;
                }
            });
            if (pilih) {
                arrTagihan<%=rnd%>.forEach(t => {
                    const cb = document.getElementById('cbTagihan_<%=rnd%>_' + t.id);
                    if (cb && tagihan.wajibDibayarSebelumnya.includes("," + t.id + ",")) {
                        cb.checked = true;
                        if (t.id != tagihan.id) cb.disabled = true;
                    }
                });
            }
        }

        if (tagihan.dibayarSebanyak > 1) {
            arrTagihan<%=rnd%>.forEach(t => {
                const cb = document.getElementById('cbTagihan_<%=rnd%>_' + t.id);
                if (!cb) return;

                if (cb.checked && cb.disabled && (t.isTerakumulasiBulanan || t.idJenisBiaya == tagihan.idJenisBiaya)) {
                    cb.disabled = false;
                    cb.checked = false;
                }
            });
            if (pilih) {
                arrTagihan<%=rnd%>.forEach(t => {
                    const cb = document.getElementById('cbTagihan_<%=rnd%>_' + t.id);
                    if (!cb) return;

                    if (t.idJenisBiaya == tagihan.idJenisBiaya) {
                        let splitTA1 = t.tahunAjaran ? t.tahunAjaran.split("/")[0] : "";
                        let digitEmpat1 = ("000000000000" + splitTA1 + t.bayarKe).slice(-4);
                        
                        let splitTA2 = tagihan.tahunAjaran ? tagihan.tahunAjaran.split("/")[0] : "";
                        let digitEmpat2 = ("000000000000" + splitTA2 + tagihan.bayarKe).slice(-4);
                        
                        let tb1 = t.tahunBulanInt ? t.tahunBulanInt.toString() : "";
                        let tb2 = tagihan.tahunBulanInt ? tagihan.tahunBulanInt.toString() : "";
                        
                        let tag1 = parseInt(tb1 + digitEmpat1) || 0;
                        let tag2 = parseInt(tb2 + digitEmpat2) || 0;
                        if (tag1 <= tag2) {
                            cb.checked = true;
                            if (t.id != tagihan.id) cb.disabled = true;
                        }
                    }
                });
            }
        } 
        else if (tagihan.periode && tagihan.periode.toLowerCase() === 'bulanan') {
            arrTagihan<%=rnd%>.forEach(t => {
                const cb = document.getElementById('cbTagihan_<%=rnd%>_' + t.id);
                if (!cb) return;

                if (cb.checked && cb.disabled && (t.isTerakumulasiBulanan || t.idJenisBiaya == tagihan.idJenisBiaya)) {
                    cb.disabled = false;
                    cb.checked = false;
                }
            });
            if (pilih) {
                arrTagihan<%=rnd%>.forEach(t => {
                    const cb = document.getElementById('cbTagihan_<%=rnd%>_' + t.id);
                    if (!cb) return;

                    let accumulatedCondition = tagihan.isTerakumulasiBulanan && t.isTerakumulasiBulanan && 
                                               tagihan.tahunBulanInt > 0 && t.tahunBulanInt > 0 && 
                                               tagihan.tahunBulanInt === t.tahunBulanInt;

                    if (accumulatedCondition || t.idJenisBiaya == tagihan.idJenisBiaya) {
                        let tag1 = t.tahunBulanInt ? parseInt(t.tahunBulanInt) : 0;
                        let tag2 = tagihan.tahunBulanInt ? parseInt(tagihan.tahunBulanInt) : 0;

                        if (tag1 <= tag2 || accumulatedCondition) {
                            cb.checked = true;
                            if (t.id != tagihan.id) cb.disabled = true;

                            arrTagihan<%=rnd%>.forEach(tData => {
                                const cbData = document.getElementById('cbTagihan_<%=rnd%>_' + tData.id);
                                if(cbData) {
                                    if (tData.isTerakumulasiBulanan && t.isTerakumulasiBulanan && 
                                        tData.tahunBulanInt > 0 && t.tahunBulanInt > 0 && 
                                        tData.tahunBulanInt === t.tahunBulanInt) {
                                        cbData.checked = true;
                                        if (tData.id != tagihan.id) cbData.disabled = true;
                                    }
                                }
                            });
                        }
                    }
                });
            }
        } 
        else {
            let ada = tagihan.harusBayarId ? true : false;
            if (ada) {
                let harusBayars = [tagihan.harusBayarId];
                checkUlangHarusBayar<%=rnd%>(harusBayars, tagihan);

                arrTagihan<%=rnd%>.forEach(t => {
                    const cb = document.getElementById('cbTagihan_<%=rnd%>_' + t.id);
                    if (!cb) return;

                    if (cb.checked && cb.disabled && harusBayars.includes(t.idJenisBiaya)) {
                        cb.disabled = false;
                        cb.checked = false;
                    }
                });
                if (pilih) {
                    arrTagihan<%=rnd%>.forEach(t => {
                        const cb = document.getElementById('cbTagihan_<%=rnd%>_' + t.id);
                        if (!cb) return;

                        if (harusBayars.includes(t.idJenisBiaya)) {
                            cb.checked = true;
                            if (t.id != tagihan.id) cb.disabled = true;
                        }
                    });
                }
            }
        }

        arrTagihan<%=rnd%>.forEach(t => {
            const cb = document.getElementById('cbTagihan_<%=rnd%>_' + t.id);
            let isWajib = t.wajibPilih === true || t.wajibPilihJikaBulanDipilih === true;
            if(cb && isWajib) { cb.checked = true; cb.disabled = true; }
        });
        
        triggerCb.disabled = false;
        hitungTotalBayar<%=rnd%>();
    };

    var hitungTotalBayar<%=rnd%> = function() {
        let total = 0;
        let idTerpilih = [];
        
        arrTagihan<%=rnd%>.forEach(t => {
            const cb = document.getElementById('cbTagihan_<%=rnd%>_' + t.id);
            if(cb && cb.checked) {
                total += parseFloat(t.totalTampil);
                idTerpilih.push(t.id);
            }
        });
        const cbTabungan = document.getElementById('cbGunakanTabungan<%=rnd%>');
        let potonganTabungan = 0;
        
        if(cbTabungan && cbTabungan.checked && saldoTabungan<%=rnd%> > 0) {
            if(saldoTabungan<%=rnd%> >= total) { potonganTabungan = total; total = 0; } 
            else { potonganTabungan = saldoTabungan<%=rnd%>; total = total - saldoTabungan<%=rnd%>; }
        }

        document.getElementById('lblTotalTagihan<%=rnd%>').innerText = formatRp<%=rnd%>(total + potonganTabungan);
        document.getElementById('lblPotonganTabungan<%=rnd%>').innerText = "- " + formatRp<%=rnd%>(potonganTabungan);
        document.getElementById('lblGrandTotal<%=rnd%>').innerText = formatRp<%=rnd%>(total);
        
        window['tagihanDipilih<%=rnd%>'] = idTerpilih;
        window['grandTotalValue<%=rnd%>'] = total;
        window['pakaiTabungan<%=rnd%>'] = (cbTabungan && cbTabungan.checked);
    };

    var renderTombolBayar<%=rnd%> = function(metodeBayar) {
        const container = document.getElementById('containerTombolBayar<%=rnd%>');
        let html = '';
        
        if (!pAdd<%=rnd%> || !pEdit<%=rnd%>) {
            html = '<div class="alert alert-danger small text-center shadow-sm border-0"><i class="fas fa-lock fa-2x mb-2"></i><br><%=Common.getBahasaConfig("Anda tidak memiliki hak akses untuk melakukan proses pembayaran.")%></div>';
            container.innerHTML = html;
            return;
        }

        if(metodeBayar && metodeBayar.length > 0) {
            metodeBayar.forEach(m => {
                let icon = 'fa-money-bill-wave';
                let btnClass = 'btn-primary';
                
                const namaLow = m.nama.toLowerCase();
                if(namaLow.includes('bri')) { icon = 'fa-building'; btnClass = 'btn-primary'; }
                else if(namaLow.includes('bni')) { icon = 'fa-building'; btnClass = 'btn-warning text-dark'; }
                else if(namaLow.includes('bsi')) { icon = 'fa-building'; btnClass = 'btn-info text-white'; }
                else if(namaLow.includes('btn')) { icon = 'fa-building'; btnClass = 'btn-secondary text-white'; }
                else if(namaLow.includes('tunai')) { icon = 'fa-cash-register'; btnClass = 'btn-success'; }
                else if(namaLow.includes('online') || namaLow.includes('flip') || namaLow.includes('smart')) { icon = 'fa-globe'; btnClass = 'btn-dark'; }

                html += '<button class="btn ' + btnClass + ' shadow-sm py-3 fw-bold fs-6 text-start position-relative overflow-hidden rounded-3" ' + 
                            'onclick="prosesBayar<%=rnd%>(\'' + m.id + '\', \'' + m.type + '\', ' + m.fee + ', \'' + m.nama + '\')">' +
                            '<i class="fas ' + icon + ' fa-fw me-2 fs-5 align-middle"></i> ' + m.nama.toUpperCase() +
                            '<i class="fas fa-chevron-right position-absolute top-50 end-0 translate-middle-y pe-3 opacity-50"></i>' +
                         '</button>';
            });
        } else {
            html = '<div class="alert alert-warning small text-center shadow-sm border-0"><i class="fas fa-info-circle fa-2x mb-2"></i><br><%=Common.getBahasaConfig("Tidak ada metode pembayaran yang aktif.")%></div>';
        }
        container.innerHTML = html;
    };
    var prosesBayar<%=rnd%> = function(idMetode, typeMetode, fee, namaMetode) {
        const ids = window['tagihanDipilih<%=rnd%>'];
        if(!ids || ids.length === 0) {
            tampilkanToast('<%=Common.getBahasaConfigJS("Silakan pilih tagihan terlebih dahulu.")%>', 'bg-warning text-dark');
            return;
        }

        const gt = window['grandTotalValue<%=rnd%>'];
        const isTunai = typeMetode === 'TUNAI';
        if(isTunai && window['pakaiTabungan<%=rnd%>'] && (gt + fee) > saldoTabungan<%=rnd%>) {
            tampilkanToast('<%=Common.getBahasaConfigJS("Saldo tabungan tidak mencukupi untuk melakukan pembayaran secara penuh.")%>', 'bg-danger text-white');
            return;
        }

        const msg = '<%=Common.getBahasaConfigJS("Konfirmasi Pembayaran")%>:\n\n' +
                    'Metode: ' + namaMetode + '\n' +
                    '<%=Common.getBahasaConfigJS("Total Tagihan")%>: ' + formatRp<%=rnd%>(gt) + '\n' + 
                    (fee > 0 ? '<%=Common.getBahasaConfigJS("Biaya Admin")%>: ' + formatRp<%=rnd%>(fee) + '\n' : '') + '\n' + 
                    '<%=Common.getBahasaConfigJS("TOTAL AKHIR")%>: ' + formatRp<%=rnd%>(gt + fee) + '\n\n' + 
                    '<%=Common.getBahasaConfigJS("Lanjutkan transaksi?")%>';
        showConfirmModal<%=rnd%>(msg, async () => {
            const payload = {
                idSiswa: document.getElementById('idSiswaSelected<%=rnd%>').value,
                khususUntukSiswa: (document.getElementById('khususUntukSiswa<%=rnd%>').value === 'true'),
                idMetode: idMetode,
                typeMetode: typeMetode,
                tagihanIds: ids,
                pakaiTabungan: window['pakaiTabungan<%=rnd%>'],
                fee: fee
            };

            const res = await fetchData<%=rnd%>('prosesBayar', payload);
            
            if (res && res.status === 'HTML_SCRIPT') {
                tampilkanToast('<%=Common.getBahasaConfigJS("Transaksi Berhasil Diproses. Menyiapkan Struk...")%>', 'bg-success text-white');
                const scriptContent = res.html.replace(/<script>/gi, '').replace(/<\/script>/gi, '');
                const scriptEl = document.createElement('script');
                scriptEl.text = scriptContent;
                document.body.appendChild(scriptEl);
                loadTagihan<%=rnd%>();
                
            } else if (res && res.status === '00') {
                if (res.tipe === 'SHOW_VA_INFO' && res.vaData) {
                    tampilkanToast('<%=Common.getBahasaConfigJS("Instruksi Pembayaran Berhasil Dibuat.")%>', 'bg-success text-white');
                    showVaInfoModal<%=rnd%>(res.vaData);
                } else if(res.tipe === 'POPUP_URL' && res.url) {
                    tampilkanToast('<%=Common.getBahasaConfigJS("Membuka halaman pembayaran...")%>', 'bg-info text-white');
                    window.open(res.url, '_blank', 'width=800,height=600');
                } else {
                    tampilkanToast('<%=Common.getBahasaConfigJS("Transaksi Berhasil Diproses.")%>', 'bg-success text-white');
                }
                loadTagihan<%=rnd%>();
            } else {
                tampilkanToast(res ? res.message : '<%=Common.getBahasaConfigJS("Kesalahan memproses transaksi.")%>', 'bg-danger text-white');
            }
        });
    };
    var showVaInfoModal<%=rnd%> = function(vaData) {
        document.getElementById('vaKode<%=rnd%>').innerText = vaData.kode;
        document.getElementById('vaNama<%=rnd%>').innerText = vaData.nama;
        document.getElementById('vaNominal<%=rnd%>').innerText = formatRp<%=rnd%>(vaData.nominal);
        
        if (vaData.biayaAdministrasi > 0) {
            document.getElementById('rowVaBiayaAdmin<%=rnd%>').style.display = '';
            document.getElementById('rowVaTotal<%=rnd%>').style.display = '';
            document.getElementById('vaBiayaAdmin<%=rnd%>').innerText = formatRp<%=rnd%>(vaData.biayaAdministrasi);
            document.getElementById('vaTotal<%=rnd%>').innerText = formatRp<%=rnd%>(vaData.biayaTotal);
        } else {
            document.getElementById('rowVaBiayaAdmin<%=rnd%>').style.display = 'none';
            document.getElementById('rowVaTotal<%=rnd%>').style.display = 'none';
        }
        
        document.getElementById('vaTerbilang<%=rnd%>').innerText = vaData.terbilang;
        document.getElementById('vaKadaluarsa<%=rnd%>').innerText = vaData.kadaluarsa;
        
        document.getElementById('vaNotes<%=rnd%>').innerText = '<%=Common.getBahasaConfigJS("Pada saat Anda melakukan pembayaran, pastikan nominal atau nilai pembayaran yang Anda masukkan sama dengan total tagihan, yaitu")%> ' + formatRp<%=rnd%>(vaData.biayaTotal);
        if (vaData.qr && vaData.qr !== '') {
            document.getElementById('rowVaQR<%=rnd%>').style.display = '';
            document.getElementById('vaQRImg<%=rnd%>').src = vaData.qr;
        } else {
            document.getElementById('rowVaQR<%=rnd%>').style.display = 'none';
        }

        const modalEl = document.getElementById('modalVAInfo<%=rnd%>');
        const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
        modal.show();
    };

    var cetakVa<%=rnd%> = function() {
        const printWindow = window.open('', '_blank');
        printWindow.document.write('<html><head><title><%=Common.getBahasaConfig("Cetak Nomor Pembayaran")%></title>');
        printWindow.document.write('<style>body{font-family:sans-serif; padding:20px;} table{width:100%; border-collapse:collapse; margin-bottom:20px;} td{padding:8px; border-bottom:1px solid #ddd;}</style></head><body>');
        printWindow.document.write('<h2><%=Common.getBahasaConfig("Instruksi Pembayaran")%></h2>');
        printWindow.document.write(document.querySelector('#modalVAInfo<%=rnd%> .modal-body').innerHTML);
        printWindow.document.write('<jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp"/>
</body></html>');
        printWindow.document.close();
        printWindow.focus();
        setTimeout(function(){ printWindow.print(); printWindow.close(); }, 500);
    };

    var cetakStrukUlang<%=rnd%> = async function(idPembayaran) {
        const payload = { action: 'cetakStrukUlang', idPembayaran: idPembayaran };
        const res = await fetchData<%=rnd%>('cetakStrukUlang', payload);
        
        if (res && res.status === 'HTML_SCRIPT') {
            tampilkanToast('<%=Common.getBahasaConfigJS("Menyiapkan dokumen struk...")%>', 'bg-info text-white');
            const scriptContent = res.html.replace(/<script>/gi, '').replace(/<\/script>/gi, '');
            const scriptEl = document.createElement('script');
            scriptEl.text = scriptContent;
            document.body.appendChild(scriptEl);
        } else {
            tampilkanToast('<%=Common.getBahasaConfigJS("Gagal mencetak ulang struk.")%>', 'bg-danger text-white');
        }
    };

    var hapusRiwayat<%=rnd%> = function(idPembayaran) {
        if(typeof prosesDeleteData === 'function') {
            prosesDeleteData('ais.database.model.sekolah.PembayaranSiswa', idPembayaran, function() {
                tampilkanToast('<%=Common.getBahasaConfigJS("Riwayat pembayaran berhasil dihapus.")%>', 'bg-success text-white');
                applyFilterRiwayat<%=rnd%>();
                loadTagihan<%=rnd%>();
             });
        } else {
            tampilkanToast('<%=Common.getBahasaConfigJS("Fungsi prosesDeleteData tidak ditemukan dalam sistem.")%>', 'bg-danger text-white');
        }
    };

    var updateNominalTagihan<%=rnd%> = async function(idTagihan, val) {
        const payload = { idTagihan: idTagihan, nominal: val };
        const res = await fetchData<%=rnd%>('updateNominal', payload);
        if(res && res.status === '00') {
            loadTagihan<%=rnd%>();
        } else {
            tampilkanToast('<%=Common.getBahasaConfigJS("Gagal memperbarui nominal biaya.")%>', 'bg-danger text-white');
        }
    };

    var bukaModalAngsuran<%=rnd%> = function(idTagihan) {
        const t = arrTagihan<%=rnd%>.find(x => x.id == idTagihan);
        if(!t) return;
        document.getElementById('idTagihanAngsuran<%=rnd%>').value = t.id;
        document.getElementById('angsuranNamaBiaya<%=rnd%>').value = t.namaBiaya;
        document.getElementById('angsuranMaksimal<%=rnd%>').value = formatRp<%=rnd%>(t.maksAngsuran);
        document.getElementById('angsuranMaksimalVal<%=rnd%>').value = t.maksAngsuran;
        document.getElementById('angsuranNominalBayar<%=rnd%>').value = '';
        document.getElementById('angsuranInformasi<%=rnd%>').value = '';
        const modalEl = document.getElementById('modalAngsuran<%=rnd%>');
        const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
        modal.show();
    };
    var simpanAngsuran<%=rnd%> = async function() {
        const idTagihan = document.getElementById('idTagihanAngsuran<%=rnd%>').value;
        const bayar = parseFloat(document.getElementById('angsuranNominalBayar<%=rnd%>').value) || 0;
        const maks = parseFloat(document.getElementById('angsuranMaksimalVal<%=rnd%>').value) || 0;
        const info = document.getElementById('angsuranInformasi<%=rnd%>').value;
        if(bayar <= 0 || bayar >= maks) {
            tampilkanToast('<%=Common.getBahasaConfigJS("Nominal angsuran tidak valid. Harus lebih dari 0 dan kurang dari sisa maksimal tagihan.")%>', 'bg-warning text-dark');
            return;
        }

        const payload = { idTagihan: idTagihan, nominalBayar: bayar, informasi: info };
        const res = await fetchData<%=rnd%>('simpanAngsuran', payload);
        
        if(res && res.status === '00') {
            const modalEl = document.getElementById('modalAngsuran<%=rnd%>');
            const modal = bootstrap.Modal.getInstance(modalEl);
            if(modal) modal.hide();
            
            tampilkanToast('<%=Common.getBahasaConfigJS("Angsuran berhasil disimpan.")%>', 'bg-success text-white');
            loadTagihan<%=rnd%>();
        } else {
            tampilkanToast('<%=Common.getBahasaConfigJS("Kegagalan saat menyimpan data angsuran.")%>', 'bg-danger text-white');
        }
    };

    var hapusAngsuran<%=rnd%> = function(idTagihan) {
        showConfirmModal<%=rnd%>('<%=Common.getBahasaConfigJS("Konfirmasi: Apakah Anda yakin ingin membatalkan angsuran ini?")%>', async () => {
            const res = await fetchData<%=rnd%>('hapusAngsuran', { idTagihan: idTagihan });
            if(res && res.status === '00') {
                tampilkanToast('<%=Common.getBahasaConfigJS("Angsuran berhasil dibatalkan.")%>', 'bg-success text-white');
                 loadTagihan<%=rnd%>();
            } else {
                tampilkanToast(res ? res.message : '<%=Common.getBahasaConfigJS("Proses penghapusan gagal.")%>', 'bg-danger text-white');
            }
        });
    };

    var refreshDataSiswa<%=rnd%> = async function() {
        const id = document.getElementById('idSiswaSelected<%=rnd%>').value;
        const khususUntukSiswa = document.getElementById('khususUntukSiswa<%=rnd%>').value;
        if(!id) return;
        
        await fetchData<%=rnd%>('refreshDataSiswa', { idSiswa: id, khususUntukSiswa: (khususUntukSiswa === 'true') });
        tampilkanToast('<%=Common.getBahasaConfigJS("Data tagihan berhasil disegarkan.")%>', 'bg-info text-dark');
        loadTagihan<%=rnd%>();
    };

    var bukaModalSuratTagihan<%=rnd%> = function() {
        const idSiswa = document.getElementById('idSiswaSelected<%=rnd%>').value;
        if(!idSiswa) {
            tampilkanToast('<%=Common.getBahasaConfigJS("Pilih Pelanggan (Siswa) terlebih dahulu.")%>', 'bg-warning text-dark');
            return;
        }
        
        let optHtml = '<option value=""><%=Common.getBahasaConfig("== Tidak menggunakan cara pembayaran ==")%></option>';
        if(typeof arrJenisPembayaran<%=rnd%> !== 'undefined') {
            arrJenisPembayaran<%=rnd%>.forEach(jp => {
                optHtml += '<option value="' + jp.id + '">' + jp.nama + '</option>';
            });
        }
        document.getElementById('stCaraPembayaran<%=rnd%>').innerHTML = optHtml;

        const modalEl = document.getElementById('modalSuratTagihan<%=rnd%>');
        const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
        modal.show();
    };

    var eksekusiSuratTagihan<%=rnd%> = async function(isWA) {
        const idSiswa = document.getElementById('idSiswaSelected<%=rnd%>').value;
        const ids = arrTagihan<%=rnd%>.map(t => t.id);
        
        if(!idSiswa) return;
        if(ids.length === 0) {
            tampilkanToast('<%=Common.getBahasaConfigJS("Tidak ada tagihan yang bisa dicetak pada daftar saat ini.")%>', 'bg-warning text-dark');
            return;
        }

        const payload = {
            action: 'prosesSuratTagihan',
            idSiswa: idSiswa,
            khususUntukSiswa: (document.getElementById('khususUntukSiswa<%=rnd%>').value === 'true'),
            tagihanIds: ids,
            tanggalSurat: document.getElementById('stTanggalSurat<%=rnd%>').value,
            tanggalJatuhTempo: document.getElementById('stTanggalJatuhTempo<%=rnd%>').value,
            nomorSurat: document.getElementById('stNomorSurat<%=rnd%>').value,
            caraPembayaranId: document.getElementById('stCaraPembayaran<%=rnd%>').value,
            denda: document.getElementById('stDenda<%=rnd%>').value,
            catatan: document.getElementById('stCatatan<%=rnd%>').value,
            kirimWA: isWA,
            bulan: document.getElementById('selectBulan<%=rnd%>').value,
            tahun: document.getElementById('selectTahun<%=rnd%>').value
        };
        const modalEl = document.getElementById('modalSuratTagihan<%=rnd%>');
        bootstrap.Modal.getInstance(modalEl).hide();

        tampilkanToast('<%=Common.getBahasaConfigJS("Menyiapkan dokumen...")%>', 'bg-info text-white');
        
        const res = await fetchData<%=rnd%>('prosesSuratTagihan', payload);
        if (res && res.status === 'HTML_SCRIPT') {
            const scriptContent = res.html.replace(/<script>/gi, '').replace(/<\/script>/gi, '');
            const scriptEl = document.createElement('script');
            scriptEl.text = scriptContent;
            document.body.appendChild(scriptEl);
            tampilkanToast(isWA ? '<%=Common.getBahasaConfigJS("Surat tagihan berhasil dikirim via WA.")%>' : '<%=Common.getBahasaConfigJS("Surat tagihan siap dicetak.")%>', 'bg-success text-white');
        } else if(res && res.status === '00') {
            tampilkanToast('<%=Common.getBahasaConfigJS("Surat tagihan berhasil diproses.")%>', 'bg-success text-white');
        } else {
            tampilkanToast(res ? res.message : '<%=Common.getBahasaConfigJS("Gagal memproses surat tagihan.")%>', 'bg-danger text-white');
        }
    };

    var exportRiwayatLokalCSV = function() {
        if (!arrRiwayat<%=rnd%> || arrRiwayat<%=rnd%>.length === 0) {
            tampilkanToast('<%=Common.getBahasaConfigJS("Tidak ada data untuk di-export.")%>', 'bg-warning text-dark');
            return;
        }

        var csv = '\uFEFF'; 
        csv += 'Tanggal,Bulan Tagihan,Item Biaya,Metode,Nominal\n';
        arrRiwayat<%=rnd%>.forEach(function(row) {
            var itm = '"' + String(row.item || '').replace(/"/g, '""') + '"';
            csv += row.waktu + ',' + row.bln + ',' + itm + ',' + row.via + ',' + row.nominal + '\n';
        });

        var blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        var link = document.createElement("a");
        var url = URL.createObjectURL(blob);
        link.setAttribute("href", url);
        link.setAttribute("download", "Riwayat_Pembayaran_Lokal.csv");
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    };

    var initPembayaranOnline<%=rnd%> = function() {
        injectModals<%=rnd%>();
        const idTerpilih = document.getElementById('idSiswaSelected<%=rnd%>');
        if(idTerpilih && idTerpilih.value) {
             loadTagihan<%=rnd%>(); 
        }

        // Pemasangan Event Listener ENTER khusus untuk Tab Riwayat Lokal
        var inputs = document.querySelectorAll('#wrapperRiwayat<%=rnd%> .form-control');
        inputs.forEach(function(el) {
            el.addEventListener('keypress', function (e) {
                if (e.key === 'Enter') { applyFilterRiwayat<%=rnd%>(); }
            });
        });
    };

    setTimeout(() => {
        initPembayaranOnline<%=rnd%>();
    }, 150);

</script>