<%@ page isELIgnored="true" %>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// Pengecekan Sesi & Keamanan Akses
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	out.print("<div class='text-center p-5'><i class='fas fa-exclamation-triangle fa-3x text-warning mb-3'></i><br><h5 class='text-muted'>" + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "</h5></div>");
	return;
}

Pedagang pedagang = tbmuser.getPedagang();
Toko toko = pedagang == null ? null : pedagang.getToko();
String rnd = Common.getGeneratedBarCode(7);

boolean isAdmin = (toko == null);
String sqlFilterToko = "";
String namaTokoAktif = "";

if (toko != null) {
    sqlFilterToko = " AND a.toko = " + toko.getId() + " ";
    namaTokoAktif = toko.getNama();
}

boolean adminBolehVerifikasiBayar = Common.getKonfigurasi("aktifkan_admin_boleh_verifikasi_bayar", Konfigurasi.TIDAK_AKTIF).getNilai().equalsIgnoreCase(Konfigurasi.AKTIF);
// Pengaturan per toko MENGALAHKAN global (lihat OtomatisPesananUtil).
// Sebelumnya hanya global yang dibaca, sehingga satu toko tidak dapat
// mematikan proses otomatis yang dinyalakan untuk seluruh unit.
boolean otomatisVerifikasiBayarSetelahJam24 = ais.action.master.koperasi.OtomatisPesananUtil.bayarOtomatis(toko);
boolean otomatisLayaniSetelahJam24 = ais.action.master.koperasi.OtomatisPesananUtil.layaniOtomatis(toko);

%>

<style>
    body { background-color: #f4f7f6; }
    .table-pesanan-<%=rnd%> th { background-color: #f8f9fa; font-size: 13px; text-transform: uppercase; color: #6c757d; }
    .item-row-<%=rnd%> { transition: background-color 0.2s; }
    .item-row-<%=rnd%>:hover { background-color: #f1f5f9; }
</style>

<div class="container-fluid py-3 py-md-4 animate__animated animate__fadeIn" id="draftManagerContent<%=rnd%>">

    <!-- HEADER -->
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-center mb-4 gap-3">
        <div class="text-center text-md-start">
            <h4 class="fw-bolder text-primary mb-1">
                <i class="fas fa-clipboard-list me-2"></i><%=Common.getBahasaConfig("Monitor Pesanan Online (Draft)")%>
            </h4>
            <p class="text-muted small mb-0">
                <% if (!isAdmin) { %>
                    <%=Common.getBahasaConfig("Daftar pesanan dari anggota untuk toko: ")%> <strong class="text-dark"><%=toko.getNama()%></strong>
                <% } else { %>
                    <%=Common.getBahasaConfig("Monitor seluruh pesanan anggota di semua toko (Akses Admin).")%>
                <% } %>
            </p>
        </div>
        
        <div class="d-flex flex-wrap gap-2 justify-content-center">
            <button class="btn btn-light rounded-pill px-4 shadow-sm fw-bold border" onclick="resetAndLoadDraft<%=rnd%>()">
                <i class="fas fa-sync-alt me-1 text-primary"></i><%=Common.getBahasaConfig("Segarkan Data")%>
            </button>
        </div>
    </div>

    <!-- FILTER SECTION -->
    <div class="card border-0 shadow-sm rounded-4 mb-4 border-top border-primary border-3">
        <div class="card-body p-3 p-md-4">
            <div class="row g-3 align-items-end">
                <div class="col-md-2">
                    <label class="form-label small fw-bold text-secondary mb-1"><i class="far fa-calendar-alt me-1"></i><%=Common.getBahasaConfig("Mulai")%></label>
                    <input type="date" class="form-control form-control-sm rounded-3 filter-draft-<%=rnd%> shadow-sm" id="filterStart<%=rnd%>">
                </div>
                <div class="col-md-2">
                    <label class="form-label small fw-bold text-secondary mb-1"><i class="far fa-calendar-check me-1"></i><%=Common.getBahasaConfig("Akhir")%></label>
                    <input type="date" class="form-control form-control-sm rounded-3 filter-draft-<%=rnd%> shadow-sm" id="filterEnd<%=rnd%>">
                </div>
                <div class="col-md-2">
                    <label class="form-label small fw-bold text-secondary mb-1"><i class="fas fa-barcode me-1"></i><%=Common.getBahasaConfig("Kode")%></label>
                    <input type="text" class="form-control form-control-sm rounded-3 filter-draft-<%=rnd%> shadow-sm" id="filterKode<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Kode Draft")%>">
                </div>
                <div class="col-md-2">
                    <label class="form-label small fw-bold text-secondary mb-1"><i class="fas fa-user me-1"></i><%=Common.getBahasaConfig("Pembeli")%></label>
                    <input type="text" class="form-control form-control-sm rounded-3 filter-draft-<%=rnd%> shadow-sm" id="filterPembeli<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Nama Pembeli")%>">
                </div>
                <% if (isAdmin) { %>
                <div class="col-md-2">
                    <label class="form-label small fw-bold text-secondary mb-1"><i class="fas fa-store me-1"></i><%=Common.getBahasaConfig("Pedagang")%></label>
                    <input type="text" class="form-control form-control-sm rounded-3 filter-draft-<%=rnd%> shadow-sm" id="filterPedagang<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Nama Pedagang")%>">
                </div>
                <% } %>
                <div class="<%= isAdmin ? "col-md-2" : "col-md-4" %> d-flex flex-column justify-content-end gap-2">
                    <button class="btn btn-sm btn-primary rounded-3 shadow-sm fw-bold w-100" onclick="resetAndLoadDraft<%=rnd%>()">
                        <i class="fas fa-filter me-2"></i><%=Common.getBahasaConfig("Saring")%>
                    </button>
                    <% if (isAdmin && adminBolehVerifikasiBayar) { %>
                    <button class="btn btn-sm btn-success rounded-3 shadow-sm fw-bold w-100" onclick="konfirmasiBayarSemua<%=rnd%>()">
                        <i class="fas fa-check-double me-1"></i><%=Common.getBahasaConfig("Bayar Semua")%>
                    </button>
                    <% } else if (!isAdmin) { %>
                    <button class="btn btn-sm btn-success rounded-3 shadow-sm fw-bold w-100" onclick="konfirmasiLayaniSemua<%=rnd%>()">
                        <i class="fas fa-check-double me-1"></i><%=Common.getBahasaConfig("Layani Semua")%>
                    </button>
                    <% } %>
                </div>
            </div>
        </div>
    </div>

    <!-- DATA TABLE SECTION -->
    <div class="card border-0 shadow-sm rounded-4 overflow-hidden">
        <div class="card-body p-0">
            <div class="table-responsive px-3 py-2" style="min-height: 400px;">
                <table class="table table-hover align-middle mb-0 table-pesanan-<%=rnd%>">
                    <thead>
                        <tr>
                            <th class="py-3 text-center" style="width: 50px;">#</th>
                            <th><%=Common.getBahasaConfig("Waktu Pesan")%></th>
                            <th><%=Common.getBahasaConfig("Nama Pemesan")%></th>
                            <% if (isAdmin) { %>
                                <th><%=Common.getBahasaConfig("Toko")%></th>
                            <% } %>
                            <th class="text-start"><%=Common.getBahasaConfig("Rincian Pesanan")%></th>
                            <th class="text-end"><%=Common.getBahasaConfig("Diskon")%></th>
                            <th class="text-end"><%=Common.getBahasaConfig("Cashback")%></th>
                            <th class="text-end"><%=Common.getBahasaConfig("Total Tagihan")%></th>
                            <th class="text-center"><%=Common.getBahasaConfig("Status")%></th>
                            <th class="text-center" style="width: 140px;"><%=Common.getBahasaConfig("Aksi")%></th>
                        </tr>
                    </thead>
                    <tbody id="tabelDataDraft<%=rnd%>">
                        <tr>
                            <td colspan="<%=isAdmin ? 10 : 9%>" class="text-center py-5">
                                <div class="spinner-border text-primary mb-3"></div>
                                <br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Sistem sedang memuat daftar pesanan...")%></span>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- PAGING -->
            <div class="bg-light p-3 border-top d-flex flex-column flex-md-row justify-content-between align-items-center gap-3">
                <div class="small fw-bold text-muted" id="pagingInfoDraft<%=rnd%>"></div>
                <div class="d-flex align-items-center bg-white rounded-pill shadow-sm border p-1">
                    <button class="btn btn-sm btn-light rounded-pill px-3 fw-bold text-primary" id="btnPrevPageDraft<%=rnd%>" onclick="changePageDraft<%=rnd%>(-1)">
                        <i class="fas fa-chevron-left me-1"></i><span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Sebelumnya")%></span>
                    </button>
                    <div class="px-3 fw-bold text-dark small">
                        <span id="lblCurrentPageDraft<%=rnd%>">1</span> / <span id="lblTotalPageDraft<%=rnd%>">1</span>
                    </div>
                    <button class="btn btn-sm btn-light rounded-pill px-3 fw-bold text-primary" id="btnNextPageDraft<%=rnd%>" onclick="changePageDraft<%=rnd%>(1)">
                        <span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Selanjutnya")%></span><i class="fas fa-chevron-right ms-1"></i>
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    // ==========================================
    // INJEKSI MODAL & TOAST VIA JAVASCRIPT
    // ==========================================
    var renderModalAndToastHTML<%=rnd%> = () => {
        const uiHtml = `
            <!-- POPUP / TOAST NOTIFIKASI PESANAN BARU (KIRI BAWAH) -->
            <div class="position-fixed bottom-0 start-0 p-4" style="z-index: 1080;">
                <div id="toastPesananBaru<%=rnd%>" class="toast shadow-lg border-0 rounded-4 d-none" role="alert" aria-live="assertive" aria-atomic="true" data-bs-autohide="false">
                    <div class="toast-header bg-danger text-white border-bottom-0 rounded-top-4 py-3">
                        <i class="fas fa-concierge-bell fa-shake me-2 fs-5"></i>
                        <strong class="me-auto fs-6"><%=Common.getBahasaConfig("Pesanan Baru Masuk!")%></strong>
                        <button type="button" class="btn-close btn-close-white" onclick="tutupToastPesanan<%=rnd%>()"></button>
                    </div>
                    <div class="toast-body bg-white rounded-bottom-4 p-4 text-center">
                        <p class="mb-2"><%=Common.getBahasaConfig("Terdapat")%> <b id="lblJmlPesananBaru<%=rnd%>" class="text-danger fs-4">0</b> <%=Common.getBahasaConfig("pesanan draft yang belum dibayar/diproses.")%></p>
                        <div class="mt-3 pt-3 border-top d-flex flex-column gap-2">
                            <button type="button" class="btn btn-outline-danger rounded-pill w-100 fw-bold shadow-sm" onclick="lihatPesananBaru<%=rnd%>()">
                                <i class="fas fa-eye me-2"></i><%=Common.getBahasaConfig("Cek Pesanan Sekarang")%>
                            </button>
                            <button type="button" class="btn btn-success rounded-pill w-100 fw-bold shadow-sm" onclick="konfirmasiLayaniSemua<%=rnd%>()">
                                <i class="fas fa-check-double me-2"></i><%=Common.getBahasaConfig("Layani Semua")%>
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            <!-- MODAL PEMBAYARAN & EDIT PESANAN (POS) -->
            <div class="modal fade" id="modalBayarPesanan<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
                <div class="modal-dialog modal-dialog-centered modal-lg">
                    <div class="modal-content rounded-4 border-0 shadow-lg">
                        <div class="modal-header bg-light border-bottom-0 pb-2 pt-3 px-4">
                            <h5 class="modal-title fw-bold text-dark" id="modalTitleBayar<%=rnd%>">
                                <i class="fas fa-cash-register text-success me-2"></i><%=Common.getBahasaConfig("Proses Pembayaran Pesanan")%>
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close" onclick="batalBayar<%=rnd%>()"></button>
                        </div>
                        
                        <div class="modal-body p-0 bg-light">
                            <div class="p-3 border-bottom bg-white d-flex justify-content-between align-items-center">
                                <div>
                                    <span class="small text-muted d-block"><%=Common.getBahasaConfig("Kode Draft:")%></span>
                                    <strong class="text-primary" id="lblModalKodeDraft<%=rnd%>">-</strong>
                                </div>
                                <div class="text-end">
                                    <span class="small text-muted d-block"><%=Common.getBahasaConfig("Pemesan:")%></span>
                                    <strong class="text-dark" id="lblModalPemesan<%=rnd%>">-</strong>
                                </div>
                            </div>

                            <div class="table-responsive px-2" style="max-height: 350px; overflow-y: auto;">
                                <table class="table table-borderless align-middle mb-0">
                                    <tbody id="cartDetailsContainer<%=rnd%>">
                                        <!-- Items dimuat via JS -->
                                    </tbody>
                                </table>
                            </div>

                            <div class="bg-white p-4 rounded-bottom-4 border-top border-2 shadow-sm">
                                <div class="row g-3 mb-3">
                                    <div class="col-md-6">
                                        <label class="form-label fw-bold text-secondary mb-1 small"><%=Common.getBahasaConfig("Metode Pembayaran")%></label>
                                        <select class="form-select form-select-sm fw-bold shadow-sm" id="selectCaraBayar<%=rnd%>">
                                            <option value=""><%=Common.getBahasaConfig("Memuat saluran...")%></option>
                                        </select>
                                    </div>
                                    <div class="col-md-6 text-end d-flex flex-column justify-content-end">
                                        <h5 class="fw-bolder text-success mb-0" id="lblModalGrandTotal<%=rnd%>">Rp 0</h5>
                                    </div>
                                </div>

                                <button class="btn btn-success w-100 py-3 fw-bold rounded-pill shadow" onclick="prosesBayarPesanan<%=rnd%>()" id="btnProsesBayar<%=rnd%>">
                                    <i class="fas fa-check-circle me-2"></i><span id="labelTombolAksiBayar<%=rnd%>"><%=Common.getBahasaConfig("Bayar & Cetak Struk")%></span>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- MODAL KONFIRMASI BAYAR SEMUA (MASSAL ADMIN) -->
            <div class="modal fade" id="modalBayarSemua<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content rounded-4 border-0 shadow-lg">
                        <div class="modal-header bg-success text-white border-bottom-0 pb-2 pt-3 px-4">
                            <h5 class="modal-title fw-bold">
                                <i class="fas fa-check-double me-2"></i><%=Common.getBahasaConfig("Verifikasi Pembayaran Massal")%>
                            </h5>
                            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body p-4 text-center">
                            <p class="text-muted mb-4" id="bayarSemuaStatus<%=rnd%>">
                                <%=Common.getBahasaConfig("Apakah Anda yakin ingin memproses pembayaran SEMUA pesanan yang belum dibayar berdasarkan filter pencarian saat ini?")%>
                            </p>
                            
                            <div id="progressBarContainer<%=rnd%>" style="display: none;">
                                <div class="progress shadow-sm" style="height: 25px; border-radius: 12px;">
                                    <div id="progressBarBayarSemua<%=rnd%>" class="progress-bar progress-bar-striped progress-bar-animated bg-success fw-bold" role="progressbar" style="width: 0%; font-size: 14px;">0%</div>
                                </div>
                            </div>

                            <div class="mt-4">
                                <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-bold border shadow-sm" data-bs-dismiss="modal" id="btnBatalBayarSemua<%=rnd%>"><%=Common.getBahasaConfig("Tutup")%></button>
                                <button type="button" class="btn btn-success px-4 rounded-pill fw-bold shadow-sm" id="btnEksekusiBayarSemua<%=rnd%>" onclick="prosesBayarkanSemua<%=rnd%>()">
                                    <i class="fas fa-play me-2"></i><%=Common.getBahasaConfig("Ya, Proses Semua")%>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- MODAL KONFIRMASI LAYANI SEMUA (MASSAL PEDAGANG) -->
            <div class="modal fade" id="modalLayaniSemua<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content rounded-4 border-0 shadow-lg">
                        <div class="modal-header bg-success text-white border-bottom-0 pb-2 pt-3 px-4">
                            <h5 class="modal-title fw-bold">
                                <i class="fas fa-check-double me-2"></i><%=Common.getBahasaConfig("Layani Semua Pesanan")%>
                            </h5>
                            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body p-4 text-center">
                            <p class="text-muted mb-4" id="layaniSemuaStatus<%=rnd%>">
                                <%=Common.getBahasaConfig("Apakah Anda yakin ingin memproses dan melayani SEMUA pesanan yang masuk berdasarkan filter saat ini?")%>
                            </p>
                            
                            <div id="progressBarContainerLayani<%=rnd%>" style="display: none;">
                                <div class="progress shadow-sm" style="height: 25px; border-radius: 12px;">
                                    <div id="progressBarLayaniSemua<%=rnd%>" class="progress-bar progress-bar-striped progress-bar-animated bg-success fw-bold" role="progressbar" style="width: 0%; font-size: 14px;">0%</div>
                                </div>
                            </div>

                            <div class="mt-4">
                                <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-bold border shadow-sm" data-bs-dismiss="modal" id="btnBatalLayaniSemua<%=rnd%>"><%=Common.getBahasaConfig("Tutup")%></button>
                                <button type="button" class="btn btn-success px-4 rounded-pill fw-bold shadow-sm" id="btnEksekusiLayaniSemua<%=rnd%>" onclick="prosesLayaniSemua<%=rnd%>()">
                                    <i class="fas fa-play me-2"></i><%=Common.getBahasaConfig("Ya, Layani Semua")%>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <!-- MODAL DETAIL LENGKAP PESANAN (READ-ONLY) -->
            <div class="modal fade" id="modalDetailPesanan<%=rnd%>" tabindex="-1" aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered modal-lg modal-dialog-scrollable">
                    <div class="modal-content rounded-4 border-0 shadow-lg">
                        <div class="modal-header bg-primary text-white border-bottom-0 pt-3 px-4">
                            <h5 class="modal-title fw-bold"><i class="fas fa-receipt me-2"></i><%=Common.getBahasaConfig("Detail Lengkap Pesanan")%></h5>
                            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body p-0 bg-light" id="detailPesananBody<%=rnd%>">
                            <!-- Diisi via JavaScript -->
                        </div>
                        <div class="modal-footer bg-white border-top">
                            <button type="button" class="btn btn-light rounded-pill px-4 fw-bold border" data-bs-dismiss="modal"><i class="fas fa-times me-1"></i><%=Common.getBahasaConfig("Tutup")%></button>
                            <button type="button" class="btn btn-outline-success rounded-pill px-4 fw-bold" onclick="excelDetailPesanan<%=rnd%>()"><i class="fas fa-file-excel me-2"></i><%=Common.getBahasaConfig("Download Excel")%></button>
                            <button type="button" class="btn btn-outline-primary rounded-pill px-4 fw-bold" onclick="cetakDetailPesanan<%=rnd%>()"><i class="fas fa-file-pdf me-2"></i><%=Common.getBahasaConfig("Download PDF")%></button>
                        </div>
                    </div>
                </div>
            </div>
        `;
        document.body.insertAdjacentHTML('beforeend', uiHtml);
    };

    // ==========================================
    // VARIABEL GLOBAL (MENGGUNAKAN VAR UNTUK MENCEGAH REFERENCE ERROR DI AJAX)
    // ==========================================
    var masterModelClass<%=rnd%> = "ais.database.model.koperasi.DraftPembelianAnggotaKoperasi";
    var isAdmin<%=rnd%> = <%=isAdmin%>;
    var adminBolehVerifikasiBayar<%=rnd%> = <%=adminBolehVerifikasiBayar%>;
    var isLoginPedagang<%=rnd%> = <%= !isAdmin %>;
    var currentTokoId<%=rnd%> = <%= toko != null ? toko.getId() : "null" %>;
    var filterTokoSQL<%=rnd%> = "<%=sqlFilterToko%>";
    
    var currentPageDraft<%=rnd%> = 1;
    var limitPerPageDraft<%=rnd%> = 10;
    var totalRecordsDraft<%=rnd%> = 0;
    
    var pollingInterval<%=rnd%> = null;
    var modalBayarInstance<%=rnd%> = null;
    var modalBayarSemuaInstance<%=rnd%> = null;
    var modalLayaniSemuaInstance<%=rnd%> = null;
    var modalDetailInstance<%=rnd%> = null;
    var detailPesananData<%=rnd%> = null; // Cache data terakhir utk tombol Cetak
    var detailClockInterval<%=rnd%> = null;
    
    var arrAturanDiskon<%=rnd%> = []; // Untuk Kalkulasi Diskon

    // State Pembayaran
    var activeDraftId<%=rnd%> = null;
    var activeDraftKode<%=rnd%> = "";
    var activeDraftIdToko<%=rnd%> = null;
    var activeDraftNamaToko<%=rnd%> = "";
    var isModalReadOnly<%=rnd%> = false; // Flag Monitor ReadOnly
    var cartDraftItems<%=rnd%> = [];
    var grandTotalModalValue<%=rnd%> = 0;

    // Helper: Format Rupiah
    var formatRp<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);
    };

    // Helper: Fetch API SQL Default
    var fetchDataAPI<%=rnd%> = async (payload) => {
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            return await res.json();
        } catch (e) { 
            console.error(e); 
            return { status: "error", message: "<%=Common.getBahasaConfig("Terjadi kesalahan jaringan.")%>" }; 
        }
    };
    
    // Helper: Fetch API SQL Update (Bisa Multiple Query)
    var updateDataAPI<%=rnd%> = async (sqlString) => {
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: "update_data", sql: sqlString })
            });
            return await res.json();
        } catch (e) {
            return { status: 'error' };
        }
    };

    // Mencegah error "not defined" akibat block-scoping dengan `var` / `window`
    window.showToastUI<%=rnd%> = function(msg, colorClass) {
        if(typeof tampilkanToast === "function") {
            tampilkanToast(msg, colorClass);
        } else {
            alert(msg);
        }
    };

    // Set Default Tanggal Filter (1 Bulan Terakhir)
    var setFilterDefaultDatesDraft<%=rnd%> = () => {
        const today = new Date();
        const lastMonth = new Date();
        lastMonth.setMonth(today.getMonth() - 1);
        
        document.getElementById('filterEnd<%=rnd%>').value = today.toISOString().split('T')[0];
        document.getElementById('filterStart<%=rnd%>').value = lastMonth.toISOString().split('T')[0];
    };


    // ==========================================
    // LOGIKA AUTO-VERIFIKASI BAYAR OVERDUE (> 12 MALAM)
    // ==========================================
    var autoVerifikasiOverdue<%=rnd%> = async () => {
        const isAutoVerifAktif = <%=otomatisVerifikasiBayarSetelahJam24%>;
        if (!isAutoVerifAktif) return;

        let filterTokoUpdate = "";
        <% if (toko != null) { %>
            filterTokoUpdate = " AND a.toko = <%=toko.getId()%> ";
        <% } %>

        const sqlUnpaid = "SELECT a.id, a.kode, a.toko as id_toko, a.anggota_koperasi, a.cara_pembayaran_koperasi " +
                          "FROM koperasi.draft_pembelian_anggota_koperasi a " +
                          "WHERE a.lunas IS NULL AND DATE(a.tanggal_pembayaran) < CURRENT_DATE " + filterTokoUpdate + " ORDER BY a.id ASC;";

        try {
            const resUnpaid = await fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlUnpaid, tanpaLoading: true });
            const listUnpaid = resUnpaid.data || [];

            if (listUnpaid.length > 0) {
                if (!document.getElementById('modalAutoVerifProses<%=rnd%>')) {
                    const modalProgress = `
                    <div class="modal fade" id="modalAutoVerifProses<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
                        <div class="modal-dialog modal-dialog-centered">
                            <div class="modal-content rounded-4 border-0 shadow-lg">
                                <div class="modal-body p-5 text-center">
                                    <i class="fas fa-magic fa-3x text-primary mb-3 animate__animated animate__bounce"></i>
                                    <h4 class="fw-bolder text-dark mb-2"><%=Common.getBahasaConfig("Verifikasi Otomatis")%></h4>
                                    <p class="text-muted small mb-4" id="autoVerifStatus<%=rnd%>"><%=Common.getBahasaConfig("Sistem memverifikasi pembayaran pesanan yang terlewat dari hari sebelumnya...")%></p>
                                    <div class="progress shadow-sm mb-3" style="height: 20px; border-radius: 10px;">
                                        <div id="autoVerifProgress<%=rnd%>" class="progress-bar progress-bar-striped progress-bar-animated bg-primary fw-bold" role="progressbar" style="width: 0%;">0%</div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>`;
                    document.body.insertAdjacentHTML('beforeend', modalProgress);
                }
                
                const modalVerif = new bootstrap.Modal(document.getElementById('modalAutoVerifProses<%=rnd%>'));
                modalVerif.show();

                let successCount = 0;
                let failCount = 0;
                let pesanGagalTerakhir = '';
                for (let i = 0; i < listUnpaid.length; i++) {
                    const draft = listUnpaid[i];
                    document.getElementById('autoVerifStatus<%=rnd%>').innerHTML = `<%=Common.getBahasaConfig("Memproses pesanan")%> ${i+1} / ${listUnpaid.length} <br> <span class="small fw-bold text-primary">(${draft.kode})</span>`;
                    
                    const sqlDetail = "SELECT d.id as draft_detail_id, p.id as produk_id, p.kode, d.nama, d.hargasatuan as harga, d.qty as jumlah, d.diskon, d.aturan_diskon, d.cashback " +
                                      "FROM koperasi.draft_pembelian d LEFT JOIN koperasi.produk p ON d.produk = p.id " +
                                      "WHERE d.draft_pembelian_anggota_koperasi = " + draft.id + ";";
                    const resDetail = await fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlDetail, tanpaLoading: true });
                    const items = resDetail.data || [];
                    
                    if (items.length > 0) {
                        const curWaktu = new Date().toISOString().slice(0, 19).replace('T', ' '); 
                        const kodeTransaksi = "POS-AUTO-" + new Date().getTime() + "-" + i;
                        const caraBayar = draft.cara_pembayaran_koperasi || '1'; 
                        
                        const payload = {
                            action: "bayar",
                            kodeUnik: kodeTransaksi,
                            idToko: draft.id_toko, 
                            waktu: curWaktu,
                            log: "true",
							kanalCheckout: "otomatis_halaman",
                            caraBayar: caraBayar,
                            draftPembelianAnggotaKoperasi: draft.id,
                            transaksi: items.map(item => ({
                                id: item.produk_id, kode: item.kode, nama: item.nama,
                                harga: parseFloat(item.harga||0), jumlah: parseFloat(item.jumlah||1),
                                diskon: parseFloat(item.diskon||0), aturanDiskon: item.aturan_diskon, 
                                cashback: parseFloat(item.cashback||0)
                            }))
                        };

                        const resBayar = await fetchDataAPI<%=rnd%>(payload);
                        if (resBayar.status === '00' || resBayar.status === 'success') {
                            successCount++;
                        } else {
                            // Kegagalan TIDAK boleh diam-diam: insiden 2026-08-18, seluruh verifikasi
                            // otomatis ditolak gerbang sesi kas (status 91) tetapi layar tetap
                            // menampilkan modal sukses -- pesanan macet "belum bayar" tanpa jejak.
                            failCount++;
                            pesanGagalTerakhir = resBayar.description || resBayar.message || ('status ' + resBayar.status);
                            console.error('Auto-verifikasi gagal utk draft ' + draft.kode + ':', resBayar);
                        }
                    }
                    
                    const percent = Math.round(((i + 1) / listUnpaid.length) * 100);
                    document.getElementById('autoVerifProgress<%=rnd%>').style.width = percent + '%';
                    document.getElementById('autoVerifProgress<%=rnd%>').innerHTML = percent + '%';
                }
                
                modalVerif.hide();
                
                // Show success info modal
                const modalHtml = `
                <div class="modal fade animate__animated animate__zoomIn" id="modalAutoVerifSukses<%=rnd%>" tabindex="-1" aria-hidden="true">
                    <div class="modal-dialog modal-dialog-centered">
                        <div class="modal-content rounded-4 border-0 shadow-lg">
                            <div class="modal-body p-5 text-center">
                                <div class="mb-4">
                                    <div class="d-inline-flex align-items-center justify-content-center bg-primary bg-opacity-10 text-primary rounded-circle" style="width: 80px; height: 80px;">
                                        <i class="fas fa-magic fa-3x"></i>
                                    </div>
                                </div>
                                <h4 class="fw-bolder text-dark mb-3"><%=Common.getBahasaConfig("Penyelesaian Otomatis")%></h4>
                                <p class="text-muted fs-6 mb-4"><%=Common.getBahasaConfig("Sistem telah otomatis memverifikasi pembayaran")%> <b class="text-primary fs-5">` + successCount + `</b> <%=Common.getBahasaConfig("pesanan dari hari sebelumnya menjadi terbayar.")%></p>` +
                                (failCount > 0 ? `<div class="alert alert-danger text-start small rounded-3 mb-4"><i class="fas fa-triangle-exclamation me-1"></i><b>` + failCount + ` <%=Common.getBahasaConfig("pesanan GAGAL diverifikasi otomatis.")%></b><br>` + pesanGagalTerakhir + `</div>` : '') + `
                                <button type="button" class="btn btn-primary px-5 rounded-pill fw-bold shadow-sm" data-bs-dismiss="modal"><i class="fas fa-check me-2"></i><%=Common.getBahasaConfig("Mengerti")%></button>
                            </div>
                        </div>
                    </div>
                </div>`;
                document.body.insertAdjacentHTML('beforeend', modalHtml);
                const autoModal = new bootstrap.Modal(document.getElementById('modalAutoVerifSukses<%=rnd%>'));
                autoModal.show();
                
                document.getElementById('modalAutoVerifSukses<%=rnd%>').addEventListener('hidden.bs.modal', function () {
                    this.remove(); 
                    loadDataDraft<%=rnd%>(); 
                });
            }
        } catch (e) {
            console.error("Gagal auto-verifikasi overdue", e);
        }
    };


    // ==========================================
    // LOGIKA PERHITUNGAN ULANG DISKON (DENGAN DEBUGGING)
    // ==========================================
    var loadAturanDiskon<%=rnd%> = async () => {
        console.log("[DEBUG] Mengeksekusi Query loadAturanDiskon...");
        const sqlAturan = "SELECT id, produk, toko, berlaku_semua_member, jenis_anggota, tipe_anggota, persentase, maksimal_potongan, nominal, potongan_langsung, TO_CHAR(tanggal_mulai, 'YYYY-MM-DD\"T\"HH24:MI:SS') as tanggal_mulai_iso, TO_CHAR(tanggal_selesai, 'YYYY-MM-DD\"T\"HH24:MI:SS') as tanggal_selesai_iso FROM koperasi.aturan_diskon WHERE aktif = true";
        const res = await fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlAturan });
        console.log("[DEBUG] Hasil Query loadAturanDiskon:", res);
        
        if(res && res.data && res.data.length > 0) {
            arrAturanDiskon<%=rnd%> = res.data;
        } else {
            arrAturanDiskon<%=rnd%> = [];
            console.warn("[DEBUG] Gagal mendapatkan data aturan diskon, atau tabel promo sedang kosong.");
        }
    };

    var updateUsageDiskonMember<%=rnd%> = async (idMember, currentToko) => {
        if (!arrAturanDiskon<%=rnd%> || arrAturanDiskon<%=rnd%>.length === 0) return;

        for (let i = 0; i < arrAturanDiskon<%=rnd%>.length; i++) {
            let rule = arrAturanDiskon<%=rnd%>[i];
            if (rule.berlaku_per_hari_dan_per_toko === 'true' || rule.berlaku_per_hari_dan_per_toko === true || rule.berlaku_per_hari_dan_per_toko === 't') {
                if (idMember !== "" && idMember !== null && currentToko !== "" && currentToko !== null) {
                    const sqlUsage = "SELECT COALESCE(SUM(terpakai), 0) as terpakai FROM (" +
                                     "SELECT COALESCE(SUM(COALESCE(p.diskon,0) + COALESCE(p.cashback,0)), 0) as terpakai " +
                                     "FROM koperasi.pembelian p " +
                                     "LEFT JOIN koperasi.pembelian_anggota_koperasi pak ON p.pembelian_anggota_koperasi = pak.id " +
                                     "WHERE p.aturan_diskon = " + rule.id + " " +
                                     "AND p.toko = " + currentToko + " " +
                                     "AND pak.anggota_koperasi = " + idMember + " " +
                                     "AND DATE(pak.tanggal_pembayaran) = CURRENT_DATE " +
                                     "UNION ALL " +
                                     "SELECT COALESCE(SUM(COALESCE(dp.diskon,0) + COALESCE(dp.cashback,0)), 0) as terpakai " +
                                     "FROM koperasi.draft_pembelian dp " +
                                     "LEFT JOIN koperasi.draft_pembelian_anggota_koperasi dpak ON dp.draft_pembelian_anggota_koperasi = dpak.id " +
                                     "WHERE dp.aturan_diskon = " + rule.id + " " +
                                     "AND dp.toko = " + currentToko + " " +
                                     "AND dpak.anggota_koperasi = " + idMember + " " +
                                     "AND DATE(dpak.tanggal_pembayaran) = CURRENT_DATE " +
                                     "AND dpak.lunas is null" +
                                     ") as gabungan";
                    const resUsage = await fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlUsage });
                    rule.terpakai_hari_ini = (resUsage && resUsage.data && resUsage.data.length > 0) ? parseFloat(resUsage.data[0].terpakai) : 0;
                } else {
                    rule.terpakai_hari_ini = 0;
                }
            } else {
                rule.terpakai_hari_ini = 0;
            }
            rule.terpakai_di_keranjang = 0; 
        }
    };

    window.hitungUlangDiskon<%=rnd%> = async function(idDraft, idToko, idMember, idLunas) {
        console.log("========== MEMULAI KALKULASI ULANG DISKON & CASHBACK ==========");
        console.log("Parameter Target -> ID Draft:", idDraft, "| ID Toko:", idToko, "| ID Member:", idMember, "| Lunas ID:", idLunas);
        
        showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Mengkalkulasi ulang diskon dan cashback...")%>', 'bg-info text-white');
        
        // Memaksa tarik ulang aturan promo terbaru untuk berjaga-jaga jika belum termuat
        console.log("Menarik data Aturan Promo terbaru dari database...");
        await loadAturanDiskon<%=rnd%>();
        
        // 1. Ambil data member untuk pengecekan aturan spesifik jenis & tipe
        let selectedMember = null;
        if (idMember && idMember !== 'null') {
            const sqlMember = "SELECT jenis_anggota_koperasi, tipe_anggota_koperasi FROM koperasi.anggota_koperasi WHERE id = " + idMember;
            const resMember = await fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlMember });
            if (resMember && resMember.data && resMember.data.length > 0) {
                selectedMember = {
                    jenis_anggota: resMember.data[0].jenis_anggota_koperasi,
                    tipe_anggota: resMember.data[0].tipe_anggota_koperasi
                };
            }
        }
        console.log("Data Member (Jenis/Tipe Terpilih):", selectedMember);

        // 2. Refresh usage harian untuk member & toko tersebut
        await updateUsageDiskonMember<%=rnd%>(idMember, idToko);
        console.log("Daftar Aturan Promo Tersedia (beserta pemakaian hari ini):", arrAturanDiskon<%=rnd%>);

        // 3. Fetch Items dalam Draft tersebut
        const sqlItems = "SELECT id, produk as produk_id, hargasatuan as harga, qty as jumlah FROM koperasi.draft_pembelian WHERE draft_pembelian_anggota_koperasi = " + idDraft;
        const resItems = await fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlItems });
        const items = resItems.data || [];

        console.log("Daftar Item Pesanan (Sebelum dihitung):", items);

        if (!items || items.length === 0) {
            console.warn("Dibatalkan: Tidak ada item pesanan yang ditemukan.");
            showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Tidak ada item pesanan untuk dihitung ulang.")%>', 'bg-warning text-dark');
            return;
        }

        let totalSubtotal = 0;
        let totalDiskonTrx = 0;
        let totalCashbackTrx = 0;
        let bulkUpdateSql = "";

        const now = new Date();

        // 4. Evaluasi ulang setiap item berdasarkan arrAturanDiskon
        items.forEach(item => {
            console.log("--> Mengevaluasi Produk:", item.produk_id, "| Harga:", item.harga, "| Qty:", item.jumlah);
            let appliedRule = null;
            
            for (let rule of arrAturanDiskon<%=rnd%>) {
                // Cek Kesesuaian Produk
                const isSpecificProduct = (rule.produk !== null && rule.produk !== '' && rule.produk !== undefined);
                if ((isSpecificProduct == true || isSpecificProduct == 'true') && rule.produk != item.produk_id) continue;
                
                // Cek Kesesuaian Toko
                const isSpecificToko = (rule.toko !== null && rule.toko !== '' && rule.toko !== undefined);
                if ((isSpecificToko == true || isSpecificToko == 'true') && rule.toko != idToko) continue;
                
                // Cek Waktu Berlaku Promo
                if (rule.tanggal_mulai_iso && rule.tanggal_mulai_iso !== '' && new Date(rule.tanggal_mulai_iso) > now) continue;
                if (rule.tanggal_selesai_iso && rule.tanggal_selesai_iso !== '' && new Date(rule.tanggal_selesai_iso) < now) continue;
                
                // Cek Kesesuaian Tipe Anggota
                const isAllMember = (rule.berlaku_semua_member === 'true' || rule.berlaku_semua_member === true || rule.berlaku_semua_member === 't');
                if (!isAllMember) {
                    if (!selectedMember) continue; 
                    if (rule.jenis_anggota && rule.jenis_anggota !== '' && rule.jenis_anggota != selectedMember.jenis_anggota) continue;
                    if (rule.tipe_anggota && rule.tipe_anggota !== '' && rule.tipe_anggota != selectedMember.tipe_anggota) continue;
                }

                appliedRule = rule;
                break;
            }

            item.diskon = 0;
            item.cashback = 0;
            item.aturanDiskon = "null";

            if (appliedRule) {
                console.log("    [MATCH] Aturan yang berlaku untuk produk ini:", appliedRule);
            } else {
                console.log("    [NO MATCH] Tidak ada aturan promo yang cocok untuk produk ini.");
            }

            // Terapkan Rumus Jika Ada Promo Cocok
            if (appliedRule) {
                let discountValue = 0;
                const itemTotalBeforeDisc = item.harga * item.jumlah;
                const persen = parseFloat(appliedRule.persentase) || 0;
                const maxPot = parseFloat(appliedRule.maksimal_potongan) || 0;
                const nom = parseFloat(appliedRule.nominal) || 0;

                if (persen > 0) {
                    discountValue = itemTotalBeforeDisc * (persen / 100);
                    if (maxPot > 0 && discountValue > maxPot) discountValue = maxPot;
                } else if (nom > 0) {
                    discountValue = nom * item.jumlah;
                    if(discountValue > itemTotalBeforeDisc) discountValue = itemTotalBeforeDisc; 
                }

                const isBerlaku1x = (appliedRule.berlaku_per_hari_dan_per_toko === 'true' || appliedRule.berlaku_per_hari_dan_per_toko === true || appliedRule.berlaku_per_hari_dan_per_toko === 't');

                // Potong kuota maksimal jika disetting 1x sehari
                if (isBerlaku1x && maxPot > 0) {
                    const terpakaiHariIni = parseFloat(appliedRule.terpakai_hari_ini) || 0;
                    const terpakaiDiKeranjang = parseFloat(appliedRule.terpakai_di_keranjang) || 0;
                    const sisaMaksimal = maxPot - terpakaiHariIni - terpakaiDiKeranjang;

                    if (sisaMaksimal <= 0) {
                        discountValue = 0;
                    } else if (discountValue > sisaMaksimal) {
                        discountValue = sisaMaksimal;
                    }
                    appliedRule.terpakai_di_keranjang = terpakaiDiKeranjang + discountValue;
                } else {
                    if (maxPot > 0 && discountValue > maxPot) {
                        discountValue = maxPot;
                    }
                }

                const isPotongLsg = (appliedRule.potongan_langsung === 'true' || appliedRule.potongan_langsung === true || appliedRule.potongan_langsung === 't');
                if (isPotongLsg) item.diskon = discountValue;
                else item.cashback = discountValue;
                
                item.aturanDiskon = appliedRule.id;
            }

            console.log("    Hasil Item -> Diskon:", item.diskon, "| Cashback:", item.cashback);

            totalSubtotal += (item.harga * item.jumlah);
            totalDiskonTrx += item.diskon;
            totalCashbackTrx += item.cashback;

            const itemTotalRow = (item.harga * item.jumlah) - item.diskon;

            // Generate string update detail draft
            bulkUpdateSql += "UPDATE koperasi.draft_pembelian SET diskon = " + item.diskon + ", cashback = " + item.cashback + ", aturan_diskon = " + item.aturanDiskon + ", total = " + itemTotalRow + " WHERE id = " + item.id + "; ";
            
            // JIKA TRANSAKSI SUDAH LUNAS (DIBAYAR), UPDATE JUGA TABEL PEMBELIAN UTAMANYA
            if (idLunas && idLunas !== 'null') {
                bulkUpdateSql += "UPDATE koperasi.pembelian SET diskon = " + item.diskon + ", cashback = " + item.cashback + ", aturan_diskon = " + item.aturanDiskon + ", total = " + itemTotalRow + " WHERE pembelian_anggota_koperasi = " + idLunas + " AND produk = " + item.produk_id + "; ";
            }
        });

        // Generate string update header draft
        const totalBiayaFinal = totalSubtotal - totalDiskonTrx;
        bulkUpdateSql += "UPDATE koperasi.draft_pembelian_anggota_koperasi SET total_diskon = " + totalDiskonTrx + ", totalcashback = " + totalCashbackTrx + ", total_biaya = " + totalBiayaFinal + " WHERE id = " + idDraft + ";";

        // JIKA TRANSAKSI SUDAH LUNAS, UPDATE HEADER PEMBELIAN UTAMANYA JUGA
        if (idLunas && idLunas !== 'null') {
             bulkUpdateSql += "UPDATE koperasi.pembelian_anggota_koperasi SET total_diskon = " + totalDiskonTrx + ", totalcashback = " + totalCashbackTrx + ", total_biaya = " + totalBiayaFinal + " WHERE id = " + idLunas + ";";
        }

        console.log("=== RINGKASAN AKHIR KALKULASI ULANG ===");
        console.log("Total Diskon Transaksi:", totalDiskonTrx);
        console.log("Total Cashback Transaksi:", totalCashbackTrx);
        console.log("Total Biaya Final Tagihan:", totalBiayaFinal);
        console.log("SQL Update yang digenerate untuk database:", bulkUpdateSql);

        // 5. Eksekusi Kueri Ke Backend
        try {
            const resJson = await updateDataAPI<%=rnd%>(bulkUpdateSql);
            console.log("Respon Eksekusi Update dari DB:", resJson);
            
            if (resJson.status === 'success' || resJson.status === '00') {
                showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Diskon dan Cashback berhasil dihitung ulang dan disimpan!")%>', 'bg-success text-white');
                loadDataDraft<%=rnd%>();
            } else {
                showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal menyimpan hasil perhitungan ke database.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            console.error("Terjadi error koneksi / syntax saat eksekusi SQL: ", e);
            showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Koneksi terputus saat mengeksekusi perhitungan.")%>', 'bg-danger text-white');
        }
        
        console.log("========== SELESAI KALKULASI ==========");
    };


    // ==========================================
    // LOGIKA TABEL DRAFT & PENCARIAN
    // ==========================================
    var getFilterQueryDraft<%=rnd%> = () => {
        const tglMulai = document.getElementById('filterStart<%=rnd%>').value;
        const tglSampai = document.getElementById('filterEnd<%=rnd%>').value;
        const kode = document.getElementById('filterKode<%=rnd%>').value.trim().replace(/'/g, "''");
        const pembeli = document.getElementById('filterPembeli<%=rnd%>').value.trim().replace(/'/g, "''");

        let filters = " WHERE DATE(a.tanggal_pembayaran) BETWEEN DATE('" + tglMulai + "') AND DATE('" + tglSampai + "') " + filterTokoSQL<%=rnd%>;
        
        if (kode !== '') filters += " AND a.kode ILIKE '%" + kode + "%' ";
        if (pembeli !== '') filters += " AND b.nama ILIKE '%" + pembeli + "%' ";
        
        if (isAdmin<%=rnd%>) {
            const pedagang = document.getElementById('filterPedagang<%=rnd%>').value.trim().replace(/'/g, "''");
            if (pedagang !== '') filters += " AND t.nama ILIKE '%" + pedagang + "%' ";
        }

        return filters;
    };

    window.resetAndLoadDraft<%=rnd%> = function() {
        currentPageDraft<%=rnd%> = 1;
        loadDataDraft<%=rnd%>();
    };

    window.changePageDraft<%=rnd%> = function(direction) {
        const totalPages = Math.ceil(totalRecordsDraft<%=rnd%> / limitPerPageDraft<%=rnd%>);
        if (direction === -1 && currentPageDraft<%=rnd%> > 1) {
            currentPageDraft<%=rnd%>--;
            loadDataDraft<%=rnd%>();
        } else if (direction === 1 && currentPageDraft<%=rnd%> < totalPages) {
            currentPageDraft<%=rnd%>++;
            loadDataDraft<%=rnd%>();
        }
    };

    var loadDataDraft<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataDraft<%=rnd%>');
        const colSpan = isAdmin<%=rnd%> ? 10 : 9;
        tbody.innerHTML = '<tr><td colspan="' + colSpan + '" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Sistem sedang memuat daftar pesanan...")%></span></td></tr>';

        const sqlFilters = getFilterQueryDraft<%=rnd%>();
        const offset = (currentPageDraft<%=rnd%> - 1) * limitPerPageDraft<%=rnd%>;

        const sqlCount = "SELECT COUNT(DISTINCT a.id) as jumlah FROM koperasi.draft_pembelian_anggota_koperasi a LEFT JOIN koperasi.anggota_koperasi b ON a.anggota_koperasi = b.id LEFT JOIN koperasi.toko t ON a.toko = t.id " + sqlFilters + ";";
        
        const sqlData = "SELECT a.id, TO_CHAR(a.tanggal_pembayaran, 'DD-MM-YYYY HH24:MI') as waktu, a.kode, " +
                        "b.nama as pemesan, a.total_biaya, a.lunas, a.keterangan, t.nama as nama_toko, a.toko as id_toko, " +
                        "COALESCE(a.total_diskon, 0) as total_diskon, COALESCE(a.totalcashback, 0) as totalcashback, " +
                        "a.cara_pembayaran_koperasi as id_cara_bayar, " +
                        "a.anggota_koperasi as id_member, " + 
                        "STRING_AGG(COALESCE(p.nama, dp.nama) || ' (' || dp.qty || ')', ', ') AS namabarang " +
                        "FROM koperasi.draft_pembelian_anggota_koperasi a " +
                        "LEFT JOIN koperasi.anggota_koperasi b ON a.anggota_koperasi = b.id " +
                        "LEFT JOIN koperasi.toko t ON a.toko = t.id " +
                        "LEFT JOIN koperasi.draft_pembelian dp ON dp.draft_pembelian_anggota_koperasi = a.id " +
                        "LEFT JOIN koperasi.produk p ON dp.produk = p.id " +
                        sqlFilters + " " +
                        "GROUP BY a.id, a.tanggal_pembayaran, a.kode, b.nama, a.total_biaya, a.lunas, a.keterangan, t.nama, a.toko, a.total_diskon, a.totalcashback, a.cara_pembayaran_koperasi " +
                        "ORDER BY (CASE WHEN a.lunas IS NULL THEN 0 ELSE 1 END) ASC, a.id DESC LIMIT " + limitPerPageDraft<%=rnd%> + " OFFSET " + offset + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlCount }),
                fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlData })
            ]);

            totalRecordsDraft<%=rnd%> = (resCount.data && resCount.data[0]) ? parseInt(resCount.data[0].jumlah || 0) : 0;
            const recordList = resData.data || [];

            let htmlList = '';

            if (recordList.length === 0) {
                htmlList = '<tr><td colspan="' + colSpan + '" class="text-center text-muted py-5"><i class="fas fa-clipboard-check fa-3x mb-3 opacity-25"></i><br><%=Common.getBahasaConfig("Tidak ada data pesanan yang sesuai kriteria.")%></td></tr>';
            } else {
                let no = offset + 1; 
                recordList.forEach((row) => {
                    const isLunas = row.lunas !== null && row.lunas !== '';
                    const badgeStatus = isLunas ? '<span class="badge bg-success"><i class="fas fa-check-double me-1"></i><%=Common.getBahasaConfig("Lunas & Selesai")%></span>' : '<span class="badge bg-warning text-dark animate__animated animate__pulse animate__infinite"><i class="fas fa-clock me-1"></i><%=Common.getBahasaConfig("Belum Dibayar")%></span>';
                    
                    const safePemesan = (row.pemesan || '<%=Common.getBahasaConfigJS("Tanpa Nama")%>').replace(/'/g, "\\'");
                    
                    let actionBtn = '';
                    let btnHitung = '<button class="btn btn-sm btn-outline-info rounded-pill px-3 shadow-sm fw-bold w-100 mb-2" onclick="hitungUlangDiskon<%=rnd%>(' + row.id + ', ' + (row.id_toko||'null') + ', ' + (row.id_member||'null') + ', ' + (isLunas ? row.lunas : 'null') + ')" title="<%=Common.getBahasaConfig("Hitung Ulang Diskon & Cashback")%>"><i class="fas fa-calculator me-1"></i><%=Common.getBahasaConfig("Hitung Ulang")%></button>';
                    let btnDetail = '<button class="btn btn-sm btn-outline-dark rounded-pill px-3 shadow-sm fw-bold w-100 mb-2" onclick="lihatDetailPesanan<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Lihat seluruh data pesanan ini")%>"><i class="fas fa-receipt me-1"></i><%=Common.getBahasaConfig("Detail")%></button>';
                    
                    if (isLunas) {
                        if (isAdmin<%=rnd%>) {
                            actionBtn += '<button class="btn btn-sm btn-outline-secondary rounded-pill px-3 shadow-sm fw-bold w-100 mb-2" onclick="cetakStrukPOS<%=rnd%>(' + row.lunas + ')"><i class="fas fa-print me-1"></i><%=Common.getBahasaConfig("Cetak Struk")%></button>';
                            actionBtn += btnHitung;
                            actionBtn += '<button class="btn btn-sm btn-light border text-secondary rounded-pill px-3 shadow-sm fw-bold w-100" onclick="bukaModalBayar<%=rnd%>(' + row.id + ', \'' + row.kode + '\', \'' + safePemesan + '\', ' + row.id_toko + ', \'' + (row.id_cara_bayar || '') + '\', \'' + (row.nama_toko || '').replace(/'/g, "\\'") + '\', true)"><i class="fas fa-eye me-1"></i><%=Common.getBahasaConfig("Monitor")%></button>';
                        } else {
                            actionBtn += '<button class="btn btn-sm btn-outline-primary rounded-pill px-3 shadow-sm fw-bold w-100 mb-2" onclick="bukaModalBayar<%=rnd%>(' + row.id + ', \'' + row.kode + '\', \'' + safePemesan + '\', ' + row.id_toko + ', \'' + (row.id_cara_bayar || '') + '\', \'' + (row.nama_toko || '').replace(/'/g, "\\'") + '\', false)"><i class="fas fa-edit me-1"></i><%=Common.getBahasaConfig("Ubah & Cetak")%></button>';
                            actionBtn += btnHitung;
                        }
                    } else {
                        // Belum Lunas
                        const canVerifyPayment = isLoginPedagang<%=rnd%> || (isAdmin<%=rnd%> && adminBolehVerifikasiBayar<%=rnd%>);

                        if (canVerifyPayment) {
                            let btnUtama = '<button class="btn btn-sm btn-primary rounded-pill px-3 shadow-sm fw-bold w-100 mb-2" onclick="bukaModalBayar<%=rnd%>(' + row.id + ', \'' + row.kode + '\', \'' + safePemesan + '\', ' + row.id_toko + ', \'' + (row.id_cara_bayar || '') + '\', \'' + (row.nama_toko || '').replace(/'/g, "\\'") + '\', false)"><i class="fas fa-cash-register me-1"></i><%=Common.getBahasaConfig("Bayar")%></button>';
                            let btnBatal = '<button class="btn btn-sm btn-danger rounded-pill px-3 shadow-sm fw-bold w-100" onclick="batalkanPesanan<%=rnd%>(' + row.id + ')"><i class="fas fa-times-circle me-1"></i><%=Common.getBahasaConfig("Batalkan")%></button>';
                            actionBtn = btnUtama + btnHitung + btnBatal;
                        } else {
                            // Admin yang tidak punya hak verifikasi bayar
                            let btnMonitor = '<button class="btn btn-sm btn-light border text-secondary rounded-pill px-3 shadow-sm fw-bold w-100 mb-2" onclick="bukaModalBayar<%=rnd%>(' + row.id + ', \'' + row.kode + '\', \'' + safePemesan + '\', ' + row.id_toko + ', \'' + (row.id_cara_bayar || '') + '\', \'' + (row.nama_toko || '').replace(/'/g, "\\'") + '\', true)"><i class="fas fa-eye me-1"></i><%=Common.getBahasaConfig("Monitor")%></button>';
                            let btnBatal = '<button class="btn btn-sm btn-outline-danger rounded-pill px-3 shadow-sm fw-bold w-100" onclick="batalkanPesanan<%=rnd%>(' + row.id + ')"><i class="fas fa-times-circle me-1"></i><%=Common.getBahasaConfig("Batalkan")%></button>';
                            actionBtn = btnMonitor + btnHitung + btnBatal;
                        }
                    }

                    // Tombol "Detail" selalu tersedia di semua baris (lunas / belum), tampil paling atas.
                    actionBtn = btnDetail + actionBtn;

                    // Format Rincian Barang dan Keterangan (seluruh sel bisa diklik untuk lihat lengkap)
                    const namaBarang = row.namabarang || '-';
                    let ketSingkat = row.keterangan || '';
                    if (ketSingkat.length > 50) ketSingkat = ketSingkat.substring(0, 47) + '...';

                    let rincianHtml = '<div style="cursor:pointer;" onclick="lihatDetailPesanan<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Klik untuk melihat seluruh data pesanan")%>">';
                    rincianHtml += '<div class="fw-bold text-primary mb-1" style="font-size: 0.85rem;">' + namaBarang + '</div>';
                    if (ketSingkat !== '') {
                        rincianHtml += '<small class="text-muted d-block"><i class="fas fa-comment-alt me-1"></i>' + ketSingkat + '</small>';
                    }
                    rincianHtml += '<span class="badge bg-light text-primary border mt-1"><i class="fas fa-search-plus me-1"></i><%=Common.getBahasaConfig("Lihat Detail Lengkap")%></span>';
                    rincianHtml += '</div>';

                    htmlList += '<tr class="border-bottom item-row-<%=rnd%> ' + (!isLunas ? 'table-warning' : '') + '">' +
                            '<td class="text-center text-muted fw-medium">' + (no++) + '</td>' +
                            '<td class="small fw-semibold text-secondary">' + (row.waktu || '-') + '</td>' +
                            '<td class="fw-bold text-dark">' + (row.pemesan || '-') + '</td>';
                            
                    if (isAdmin<%=rnd%>) {
                        htmlList += '<td><span class="badge bg-info text-dark"><i class="fas fa-store me-1"></i>' + (row.nama_toko || '-') + '</span></td>';
                    }
                    
                    htmlList += '<td class="text-start">' + rincianHtml + '</td>' +
                            '<td class="text-end text-danger fw-bold">' + formatRp<%=rnd%>(row.total_diskon) + '</td>' +
                            '<td class="text-end text-info fw-bold">' + formatRp<%=rnd%>(row.totalcashback) + '</td>' +
                            '<td class="text-end text-success fw-bolder">' + formatRp<%=rnd%>(row.total_biaya) + '</td>' +
                            '<td class="text-center">' + badgeStatus + '</td>' +
                            '<td class="text-center">' + actionBtn + '</td>' +
                        '</tr>';
                });
            }
            
            tbody.innerHTML = htmlList;
            updatePagingUIDraft<%=rnd%>();

            // Cek polling badge agar sinkron
            cekPesananDraftBaru<%=rnd%>();

        } catch (error) {
            console.error("Error loading table:", error);
            tbody.innerHTML = '<tr><td colspan="' + colSpan + '" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3"></i><br><%=Common.getBahasaConfig("Gagal menarik data dari peladen.")%></td></tr>';
        }
    };

    var updatePagingUIDraft<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecordsDraft<%=rnd%> / limitPerPageDraft<%=rnd%>) || 1;
        document.getElementById('lblCurrentPageDraft<%=rnd%>').innerText = currentPageDraft<%=rnd%>;
        document.getElementById('lblTotalPageDraft<%=rnd%>').innerText = totalPages;
        
        const startIdx = totalRecordsDraft<%=rnd%> > 0 ? ((currentPageDraft<%=rnd%> - 1) * limitPerPageDraft<%=rnd%> + 1) : 0;
        const endIdx = Math.min(currentPageDraft<%=rnd%> * limitPerPageDraft<%=rnd%>, totalRecordsDraft<%=rnd%>);
        
        document.getElementById('pagingInfoDraft<%=rnd%>').innerHTML = '<%=Common.getBahasaConfigJS("Menampilkan")%> <span class="text-dark">' + startIdx + ' - ' + endIdx + '</span> <%=Common.getBahasaConfig("dari")%> <span class="text-dark">' + totalRecordsDraft<%=rnd%> + '</span> <%=Common.getBahasaConfig("pesanan")%>';
        
        document.getElementById('btnPrevPageDraft<%=rnd%>').disabled = (currentPageDraft<%=rnd%> === 1);
        document.getElementById('btnNextPageDraft<%=rnd%>').disabled = (currentPageDraft<%=rnd%> === totalPages);
    };

    // ==========================================
    // LOGIKA DETAIL LENGKAP PESANAN (READ-ONLY)
    // Menampilkan SELURUH data 1 pesanan: header, catatan penuh, semua item,
    // beserta rincian diskon / cashback / subtotal dan total tagihan.
    // ==========================================
    var escapeHtmlDetail<%=rnd%> = (s) => {
        if (s === null || s === undefined) return '';
        return ('' + s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    };

    var buildDetailHtml<%=rnd%> = (h, items) => {
        const isLunas = h.lunas !== null && h.lunas !== '' && h.lunas !== undefined;
        const statusBadge = isLunas
            ? '<span class="badge bg-success px-3 py-2"><i class="fas fa-check-double me-1"></i><%=Common.getBahasaConfig("Lunas & Selesai")%></span>'
            : '<span class="badge bg-warning text-dark px-3 py-2"><i class="fas fa-clock me-1"></i><%=Common.getBahasaConfig("Belum Dibayar")%></span>';

        let subtotal = 0;
        let html = '';

        // --- BLOK INFORMASI HEADER ---
        html += '<div class="bg-white p-4 border-bottom">';
        html += '<div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-3">';
        html += '<div><div class="text-muted small text-uppercase fw-bold"><%=Common.getBahasaConfig("Kode Pesanan")%></div><div class="fs-5 fw-bolder text-primary">' + (h.kode || '-') + '</div></div>';
        html += '<div class="text-end">' + statusBadge + '</div>';
        html += '</div>';
        html += '<div class="row g-3">';
        const infoCell = (icon, label, val) => '<div class="col-6 col-md-4"><div class="text-muted small"><i class="fas ' + icon + ' me-1"></i>' + label + '</div><div class="fw-bold text-dark">' + val + '</div></div>';
        html += infoCell('fa-user', '<%=Common.getBahasaConfigJS("Pemesan")%>', (h.kode_member ? '<span class="text-secondary">' + h.kode_member + '</span> ' : '') + (h.pemesan || '-'));
        html += infoCell('fa-store', '<%=Common.getBahasaConfigJS("Toko / Pedagang")%>', h.nama_toko || '-');
        html += infoCell('fa-clock', '<%=Common.getBahasaConfigJS("Waktu Pesan")%>', h.waktu || '-');
        html += infoCell('fa-stopwatch', '<%=Common.getBahasaConfigJS("Waktu Sekarang")%>', '<span id="detailClock<%=rnd%>">-</span>');
        html += infoCell('fa-wallet', '<%=Common.getBahasaConfigJS("Metode Bayar")%>', h.cara_bayar || '-');
        html += '</div>';
        if (h.keterangan && h.keterangan.trim() !== '') {
            html += '<div class="mt-3 p-3 bg-light rounded-3 border"><div class="text-muted small fw-bold mb-1"><i class="fas fa-comment-alt me-1"></i><%=Common.getBahasaConfig("Catatan / Keterangan Pesanan")%></div><div class="text-dark" style="white-space:pre-wrap;">' + escapeHtmlDetail<%=rnd%>(h.keterangan) + '</div></div>';
        }
        html += '</div>';

        // --- TABEL RINCIAN ITEM ---
        html += '<div class="p-4">';
        html += '<h6 class="fw-bold text-dark mb-3"><i class="fas fa-list-ul me-2 text-primary"></i><%=Common.getBahasaConfig("Rincian Item Pesanan")%> (' + items.length + ')</h6>';
        html += '<div class="table-responsive"><table class="table table-sm align-middle mb-0">';
        html += '<thead><tr class="text-muted small text-uppercase">' +
                '<th style="width:36px;">#</th><th><%=Common.getBahasaConfig("Produk")%></th><th class="text-center"><%=Common.getBahasaConfig("Qty")%></th>' +
                '<th class="text-end"><%=Common.getBahasaConfig("Harga")%></th><th class="text-end"><%=Common.getBahasaConfig("Diskon")%></th><th class="text-end"><%=Common.getBahasaConfig("Cashback")%></th><th class="text-end"><%=Common.getBahasaConfig("Subtotal")%></th></tr></thead><tbody>';
        if (items.length === 0) {
            html += '<tr><td colspan="7" class="text-center text-muted py-4"><%=Common.getBahasaConfig("Tidak ada item pada pesanan ini.")%></td></tr>';
        } else {
            items.forEach((it, i) => {
                const harga = parseFloat(it.harga) || 0;
                const qty = parseFloat(it.jumlah) || 0;
                const disk = parseFloat(it.diskon) || 0;
                const cb = parseFloat(it.cashback) || 0;
                const sub = (harga * qty) - disk;
                subtotal += (harga * qty);
                html += '<tr>' +
                    '<td class="text-muted">' + (i + 1) + '</td>' +
                    '<td class="fw-semibold text-dark">' + (it.nama || '-') + (it.kode ? ' <span class="badge bg-light text-muted border ms-1" style="font-size:9px;">' + it.kode + '</span>' : '') + '</td>' +
                    '<td class="text-center fw-bold">' + qty + '</td>' +
                    '<td class="text-end">' + formatRp<%=rnd%>(harga) + '</td>' +
                    '<td class="text-end text-danger">' + (disk > 0 ? '-' + formatRp<%=rnd%>(disk) : '-') + '</td>' +
                    '<td class="text-end text-info">' + (cb > 0 ? '+' + formatRp<%=rnd%>(cb) : '-') + '</td>' +
                    '<td class="text-end fw-bolder text-dark">' + formatRp<%=rnd%>(sub) + '</td>' +
                '</tr>';
            });
        }
        html += '</tbody></table></div>';

        // --- RINGKASAN TOTAL ---
        html += '<div class="d-flex justify-content-end mt-3"><div style="min-width:280px;">';
        const totRow = (label, val, cls) => '<div class="d-flex justify-content-between py-1"><span class="text-muted">' + label + '</span><span class="fw-bold ' + (cls || '') + '">' + val + '</span></div>';
        html += totRow('<%=Common.getBahasaConfigJS("Subtotal")%>', formatRp<%=rnd%>(subtotal), 'text-dark');
        html += totRow('<%=Common.getBahasaConfigJS("Total Diskon")%>', '-' + formatRp<%=rnd%>(h.total_diskon), 'text-danger');
        html += totRow('<%=Common.getBahasaConfigJS("Total Cashback")%>', '+' + formatRp<%=rnd%>(h.totalcashback), 'text-info');
        html += '<div class="d-flex justify-content-between py-2 mt-1 border-top"><span class="fw-bolder text-dark"><%=Common.getBahasaConfig("TOTAL TAGIHAN")%></span><span class="fw-bolder text-success fs-5">' + formatRp<%=rnd%>(h.total_biaya) + '</span></div>';
        html += '</div></div>';
        html += '</div>';

        return html;
    };

    window.lihatDetailPesanan<%=rnd%> = async function(idDraft) {
        if (!modalDetailInstance<%=rnd%>) {
            modalDetailInstance<%=rnd%> = new bootstrap.Modal(document.getElementById('modalDetailPesanan<%=rnd%>'));
        }
        const body = document.getElementById('detailPesananBody<%=rnd%>');
        body.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Memuat rincian lengkap pesanan...")%></span></div>';
        detailPesananData<%=rnd%> = null;
        modalDetailInstance<%=rnd%>.show();

        const sqlHeader = "SELECT a.kode, TO_CHAR(a.tanggal_pembayaran, 'DD-MM-YYYY HH24:MI:SS') as waktu, " +
            "COALESCE(b.nama,'-') as pemesan, COALESCE(b.kode_identitas,'') as kode_member, COALESCE(t.nama,'-') as nama_toko, " +
            "a.lunas, COALESCE(a.keterangan,'') as keterangan, COALESCE(a.total_biaya,0) as total_biaya, " +
            "COALESCE(a.total_diskon,0) as total_diskon, COALESCE(a.totalcashback,0) as totalcashback, COALESCE(cpk.nama,'-') as cara_bayar " +
            "FROM koperasi.draft_pembelian_anggota_koperasi a " +
            "LEFT JOIN koperasi.anggota_koperasi b ON a.anggota_koperasi = b.id " +
            "LEFT JOIN koperasi.toko t ON a.toko = t.id " +
            "LEFT JOIN koperasi.cara_pembayaran_koperasi cpk ON a.cara_pembayaran_koperasi = cpk.id " +
            "WHERE a.id = " + idDraft + ";";

        const sqlItems = "SELECT COALESCE(p.nama, d.nama) as nama, COALESCE(p.kode,'') as kode, " +
            "COALESCE(d.hargasatuan,0) as harga, COALESCE(d.qty,0) as jumlah, COALESCE(d.diskon,0) as diskon, COALESCE(d.cashback,0) as cashback " +
            "FROM koperasi.draft_pembelian d LEFT JOIN koperasi.produk p ON d.produk = p.id " +
            "WHERE d.draft_pembelian_anggota_koperasi = " + idDraft + " ORDER BY d.id ASC;";

        try {
            const [resH, resI] = await Promise.all([
                fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlHeader }),
                fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlItems })
            ]);
            const h = (resH.data && resH.data[0]) ? resH.data[0] : null;
            const items = resI.data || [];
            if (!h) {
                body.innerHTML = '<div class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3"></i><br><%=Common.getBahasaConfig("Data pesanan tidak ditemukan.")%></div>';
                return;
            }
            detailPesananData<%=rnd%> = { header: h, items: items };
            body.innerHTML = buildDetailHtml<%=rnd%>(h, items);
            if (detailClockInterval<%=rnd%>) clearInterval(detailClockInterval<%=rnd%>);
            const tickDetailClock<%=rnd%> = () => {
                const el = document.getElementById('detailClock<%=rnd%>');
                if (el) el.textContent = new Intl.DateTimeFormat('id-ID', {day:'2-digit',month:'2-digit',year:'numeric',hour:'2-digit',minute:'2-digit',second:'2-digit',hour12:false}).format(new Date());
            };
            tickDetailClock<%=rnd%>();
            detailClockInterval<%=rnd%> = setInterval(tickDetailClock<%=rnd%>, 1000);
        } catch (e) {
            console.error("Gagal memuat detail pesanan:", e);
            body.innerHTML = '<div class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3"></i><br><%=Common.getBahasaConfig("Gagal memuat rincian pesanan.")%></div>';
        }
    };

    window.excelDetailPesanan<%=rnd%> = function() {
        if (!detailPesananData<%=rnd%>) return;
        const h = detailPesananData<%=rnd%>.header, items = detailPesananData<%=rnd%>.items || [];
        let table = '<table><thead><tr><th>Produk</th><th>Kode</th><th>Qty</th><th>Harga</th><th>Diskon</th><th>Cashback</th><th>Subtotal</th></tr></thead><tbody>';
        items.forEach(it => { const harga=Number(it.harga||0),qty=Number(it.jumlah||0),diskon=Number(it.diskon||0); table += '<tr><td>'+escapeHtmlDetail<%=rnd%>(it.nama||'')+'</td><td>'+escapeHtmlDetail<%=rnd%>(it.kode||'')+'</td><td>'+qty+'</td><td>'+harga+'</td><td>'+diskon+'</td><td>'+Number(it.cashback||0)+'</td><td>'+(harga*qty-diskon)+'</td></tr>'; });
        table += '</tbody></table>';
        const blob = new Blob(['<html><meta charset="UTF-8"><body><h2>Detail Pesanan '+escapeHtmlDetail<%=rnd%>(h.kode||'')+'</h2><p>Waktu transaksi: '+escapeHtmlDetail<%=rnd%>(h.waktu||'')+'</p>'+table+'</body></html>'], {type:'application/vnd.ms-excel;charset=utf-8'});
        const a=document.createElement('a');a.href=URL.createObjectURL(blob);a.download='detail-pesanan-'+String(h.kode||'').replace(/[^a-z0-9_-]+/gi,'-')+'.xls';a.click();setTimeout(()=>URL.revokeObjectURL(a.href),1000);
    };

    window.cetakDetailPesanan<%=rnd%> = function() {
        if (!detailPesananData<%=rnd%>) return;
        const h = detailPesananData<%=rnd%>.header;
        const items = detailPesananData<%=rnd%>.items;
        const isLunas = h.lunas !== null && h.lunas !== '' && h.lunas !== undefined;

        let subtotal = 0;
        let rowsHtml = '';
        items.forEach((it, i) => {
            const harga = parseFloat(it.harga) || 0, qty = parseFloat(it.jumlah) || 0, disk = parseFloat(it.diskon) || 0, cb = parseFloat(it.cashback) || 0;
            const sub = (harga * qty) - disk; subtotal += (harga * qty);
            rowsHtml += '<tr><td>' + (i + 1) + '</td><td>' + (it.nama || '-') + '</td><td style="text-align:center;">' + qty + '</td>' +
                '<td style="text-align:right;">' + formatRp<%=rnd%>(harga) + '</td><td style="text-align:right;">' + (disk > 0 ? '-' + formatRp<%=rnd%>(disk) : '-') + '</td>' +
                '<td style="text-align:right;">' + (cb > 0 ? '+' + formatRp<%=rnd%>(cb) : '-') + '</td><td style="text-align:right;">' + formatRp<%=rnd%>(sub) + '</td></tr>';
        });

        let doc = '<html><head><title><%=Common.getBahasaConfig("Detail Pesanan")%> ' + (h.kode || '') + '</title><style>' +
            'body{font-family:Arial,Helvetica,sans-serif;font-size:12px;color:#000;padding:24px;}' +
            'h2{margin:0 0 4px 0;} table{width:100%;border-collapse:collapse;margin-top:10px;}' +
            'th,td{border:1px solid #ccc;padding:6px 8px;} th{background:#f0f0f0;text-align:left;}' +
            '.info td{border:none;padding:2px 4px;} .totals{margin-top:10px;width:320px;float:right;} .totals td{border:none;padding:2px 6px;}' +
            '</style></head><body>';
        doc += '<h2><%=Common.getBahasaConfig("Detail Pesanan")%></h2><div><b><%=Common.getBahasaConfig("Status")%>:</b> ' + (isLunas ? '<%=Common.getBahasaConfigJS("LUNAS & SELESAI")%>' : '<%=Common.getBahasaConfigJS("BELUM DIBAYAR")%>') + '</div>';
        doc += '<table class="info" style="margin-top:8px;border:none;">' +
            '<tr><td><b><%=Common.getBahasaConfig("Kode")%></b></td><td>: ' + (h.kode || '-') + '</td><td><b><%=Common.getBahasaConfig("Waktu")%></b></td><td>: ' + (h.waktu || '-') + '</td></tr>' +
            '<tr><td><b><%=Common.getBahasaConfig("Pemesan")%></b></td><td>: ' + ((h.kode_member ? h.kode_member + ' ' : '') + (h.pemesan || '-')) + '</td><td><b><%=Common.getBahasaConfig("Toko")%></b></td><td>: ' + (h.nama_toko || '-') + '</td></tr>' +
            '<tr><td><b><%=Common.getBahasaConfig("Metode")%></b></td><td>: ' + (h.cara_bayar || '-') + '</td><td></td><td></td></tr></table>';
        if (h.keterangan && h.keterangan.trim() !== '') {
            doc += '<div style="margin-top:8px;"><b><%=Common.getBahasaConfig("Catatan")%>:</b> ' + escapeHtmlDetail<%=rnd%>(h.keterangan) + '</div>';
        }
        doc += '<table><thead><tr><th>#</th><th><%=Common.getBahasaConfig("Produk")%></th><th><%=Common.getBahasaConfig("Qty")%></th><th><%=Common.getBahasaConfig("Harga")%></th><th><%=Common.getBahasaConfig("Diskon")%></th><th><%=Common.getBahasaConfig("Cashback")%></th><th><%=Common.getBahasaConfig("Subtotal")%></th></tr></thead><tbody>' + rowsHtml + '</tbody></table>';
        doc += '<table class="totals">' +
            '<tr><td><%=Common.getBahasaConfig("Subtotal")%></td><td style="text-align:right;">' + formatRp<%=rnd%>(subtotal) + '</td></tr>' +
            '<tr><td><%=Common.getBahasaConfig("Total Diskon")%></td><td style="text-align:right;">-' + formatRp<%=rnd%>(h.total_diskon) + '</td></tr>' +
            '<tr><td><%=Common.getBahasaConfig("Total Cashback")%></td><td style="text-align:right;">+' + formatRp<%=rnd%>(h.totalcashback) + '</td></tr>' +
            '<tr><td><b><%=Common.getBahasaConfig("TOTAL TAGIHAN")%></b></td><td style="text-align:right;"><b>' + formatRp<%=rnd%>(h.total_biaya) + '</b></td></tr></table>';
        doc += '</body></html>';

        try {
            const w = window.open('', '', 'height=700,width=820');
            w.document.write(doc); w.document.close(); w.focus();
            setTimeout(() => { w.print(); }, 400);
        } catch (e) {}
    };

    // ==========================================
    // LOGIKA HAPUS / BATALKAN PESANAN DRAFT
    // ==========================================
    window.batalkanPesanan<%=rnd%> = function(id) {
        if (typeof prosesDeleteData === 'function') {
            prosesDeleteData(masterModelClass<%=rnd%>, id, function(){ 
                 if (document.querySelectorAll('#tabelDataDraft<%=rnd%> tr').length === 1 && currentPageDraft<%=rnd%> > 1) {
                     currentPageDraft<%=rnd%>--;
                 }
                 loadDataDraft<%=rnd%>();
            });
        } else {
             // Fallback apabila fungsi JS bawaan tidak ter-load
             if(confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin membatalkan dan menghapus pesanan ini?")%>')) {
                 const payload = { action: "deleteData", class: masterModelClass<%=rnd%>, id: id };
                 fetchDataAPI<%=rnd%>(payload).then(res => {
                     if (res.status === '00' || res.status === 'success') {
                         if (document.querySelectorAll('#tabelDataDraft<%=rnd%> tr').length === 1 && currentPageDraft<%=rnd%> > 1) currentPageDraft<%=rnd%>--;
                         loadDataDraft<%=rnd%>();
                     } else {
                         alert(res.description || '<%=Common.getBahasaConfigJS("Gagal membatalkan pesanan.")%>');
                     }
                 });
             }
        }
    };

    // ==========================================
    // LOGIKA MODAL PEMBAYARAN, MONITOR & EDIT CART
    // ==========================================
    
    // 1. Load Cara Pembayaran untuk POS Kantin
    var loadCaraBayarPOS<%=rnd%> = async () => {
        const sql = "SELECT id, nama FROM koperasi.cara_pembayaran_koperasi WHERE aktif = true ORDER BY nama ASC";
        const res = await fetchDataAPI<%=rnd%>({ action: "sql", sql: sql });
        let opt = '<option value=""><%=Common.getBahasaConfig("-- Pilih Metode Bayar --")%></option>';
        if (res.data) {
            res.data.forEach(item => {
                opt += '<option value="' + item.id + '">' + item.nama + '</option>';
            });
        }
        document.getElementById('selectCaraBayar<%=rnd%>').innerHTML = opt;
    };

    // 2. Buka Modal dan Tarik Rincian Items
    window.bukaModalBayar<%=rnd%> = async function(draftId, draftKode, pemesan, idToko, idCaraBayar, namaToko, isReadOnly = false) {
        activeDraftId<%=rnd%> = draftId;
        activeDraftKode<%=rnd%> = draftKode;
        activeDraftIdToko<%=rnd%> = idToko;
        activeDraftNamaToko<%=rnd%> = namaToko || "";
        isModalReadOnly<%=rnd%> = isReadOnly;

        document.getElementById('lblModalKodeDraft<%=rnd%>').innerText = draftKode;
        document.getElementById('lblModalPemesan<%=rnd%>').innerText = pemesan;
        document.getElementById('cartDetailsContainer<%=rnd%>').innerHTML = '<tr><td class="text-center py-4"><i class="fas fa-spinner fa-spin text-success fa-2x"></i></td></tr>';
        
        // Atur Status Readonly untuk Admin
        const titleModal = document.getElementById('modalTitleBayar<%=rnd%>');
        if (titleModal) {
             titleModal.innerHTML = isReadOnly ? '<i class="fas fa-eye text-primary me-2"></i><%=Common.getBahasaConfig("Monitor Rincian Pesanan")%>' : '<i class="fas fa-cash-register text-success me-2"></i><%=Common.getBahasaConfig("Proses Pembayaran Pesanan")%>';
        }

        // Atur Default Cara Bayar & Kunci Jika Admin
        const selectCara = document.getElementById('selectCaraBayar<%=rnd%>');
        const btnProses = document.getElementById('btnProsesBayar<%=rnd%>');
        const labelTombol = document.getElementById('labelTombolAksiBayar<%=rnd%>');

        if (isReadOnly) {
            selectCara.disabled = true;
            btnProses.style.display = 'none'; // Sembunyikan Tombol Bayar untuk Admin
            
            // Set value jika ada default
            if (idCaraBayar && idCaraBayar !== 'null' && idCaraBayar !== '') {
                selectCara.value = idCaraBayar;
            }
        } else {
            selectCara.disabled = false;
            btnProses.style.display = 'block'; // Tampilkan Tombol Bayar
            
            // Set Default Cara Pembayaran dari Data Draft
            if (idCaraBayar && idCaraBayar !== 'null' && idCaraBayar !== '') {
                selectCara.value = idCaraBayar;
                if(labelTombol) labelTombol.innerText = '<%=Common.getBahasaConfigJS("Ubah & Cetak Struk")%>';
            } else {
                selectCara.value = "";
                if(labelTombol) labelTombol.innerText = '<%=Common.getBahasaConfigJS("Bayar & Cetak Struk")%>';
            }
        }

        if (!modalBayarInstance<%=rnd%>) {
            modalBayarInstance<%=rnd%> = new bootstrap.Modal(document.getElementById('modalBayarPesanan<%=rnd%>'));
        }
        modalBayarInstance<%=rnd%>.show();

        // Tarik data detail item draft
        const sqlDetail = "SELECT d.id as draft_detail_id, p.id as produk_id, p.kode, d.nama, d.hargasatuan as harga, d.qty as jumlah, d.diskon, d.aturan_diskon, d.cashback " +
                          "FROM koperasi.draft_pembelian d " +
                          "LEFT JOIN koperasi.produk p ON d.produk = p.id " +
                          "WHERE d.draft_pembelian_anggota_koperasi = " + draftId + " ORDER BY d.id ASC;";
        
        try {
            const resDetail = await fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlDetail });
            cartDraftItems<%=rnd%> = resDetail.data || [];
            
            // Format ulang struktur agar mirip cart belanja di beranda
            cartDraftItems<%=rnd%> = cartDraftItems<%=rnd%>.map(item => ({
                id: item.produk_id,
                draft_detail_id: item.draft_detail_id,
                kode: item.kode,
                nama: item.nama,
                harga: parseFloat(item.harga || 0),
                jumlah: parseFloat(item.jumlah || 1),
                diskon: parseFloat(item.diskon || 0),
                aturanDiskon: item.aturan_diskon,
                cashback: parseFloat(item.cashback || 0)
            }));

            renderCartModal<%=rnd%>();

        } catch (e) {
            console.error(e);
            document.getElementById('cartDetailsContainer<%=rnd%>').innerHTML = '<tr><td class="text-center text-danger py-4"><i class="fas fa-exclamation-triangle"></i> <%=Common.getBahasaConfig("Gagal memuat rincian pesanan.")%></td></tr>';
        }
    };

    window.batalBayar<%=rnd%> = function() {
        if (modalBayarInstance<%=rnd%>) modalBayarInstance<%=rnd%>.hide();
        activeDraftId<%=rnd%> = null;
        cartDraftItems<%=rnd%> = [];
        isModalReadOnly<%=rnd%> = false;
    };

    // 3. Render Ulang Keranjang di Dalam Modal
    var renderCartModal<%=rnd%> = () => {
        const container = document.getElementById('cartDetailsContainer<%=rnd%>');
        const lblTotal = document.getElementById('lblModalGrandTotal<%=rnd%>');
        
        let htmlCart = '';
        grandTotalModalValue<%=rnd%> = 0;

        if (cartDraftItems<%=rnd%>.length === 0) {
            htmlCart = '<tr><td class="text-center text-muted py-4"><i class="fas fa-shopping-basket fa-2x mb-2 opacity-25"></i><br><%=Common.getBahasaConfig("Pesanan kosong / semua item dihapus.")%></td></tr>';
            lblTotal.innerText = 'Rp 0';
            if(!isModalReadOnly<%=rnd%>) document.getElementById('btnProsesBayar<%=rnd%>').disabled = true;
        } else {
            if(!isModalReadOnly<%=rnd%>) document.getElementById('btnProsesBayar<%=rnd%>').disabled = false;

            cartDraftItems<%=rnd%>.forEach((item, idx) => {
                const itemSubtotal = (item.harga * item.jumlah) - item.diskon;
                grandTotalModalValue<%=rnd%> += itemSubtotal;

                let promoBadge = '';
                if(item.diskon > 0) promoBadge += '<span class="badge bg-danger ms-2" style="font-size: 9px;">Disc ' + formatRp<%=rnd%>(item.diskon) + '</span>';

                let qtyUi = '';
                let deleteUi = '';

                // Jika Admin Monitor, hilangkan fungsi edit Cart
                if (isModalReadOnly<%=rnd%>) {
                    qtyUi = '<div class="text-center fw-bolder fs-6">' + item.jumlah + ' <span class="fw-normal small text-muted">x</span></div>';
                    deleteUi = ''; 
                } else {
                    qtyUi = '<div class="input-group input-group-sm shadow-sm rounded-pill overflow-hidden border border-success">' +
                                '<button class="btn btn-light text-success border-0 px-2 fw-bold" type="button" onclick="updateQtyModal<%=rnd%>(' + idx + ', -1)">-</button>' +
                                '<input type="text" class="form-control text-center px-0 border-0 fw-bolder bg-white" style="font-size: 0.9rem;" value="' + item.jumlah + '" readonly>' +
                                '<button class="btn btn-light text-success border-0 px-2 fw-bold" type="button" onclick="updateQtyModal<%=rnd%>(' + idx + ', 1)">+</button>' +
                            '</div>';
                    deleteUi = '<button class="btn btn-sm btn-outline-danger border-0" onclick="removeItemModal<%=rnd%>(' + idx + ')"><i class="fas fa-times"></i></button>';
                }

                htmlCart += '<tr class="border-bottom">' +
                            '<td class="px-3 py-3">' +
                                '<div class="fw-bold text-dark mb-1" style="font-size: 0.9rem;">' + item.nama + promoBadge + '</div>' + 
                                '<small class="text-muted fw-semibold">' + formatRp<%=rnd%>(item.harga) + '</small>' +
                            '</td>' +
                            '<td style="width: 120px;" class="px-2 py-3">' + qtyUi + '</td>' +
                            '<td class="text-end fw-bolder text-dark px-3 py-3 fs-6">' + formatRp<%=rnd%>(itemSubtotal) + '</td>' +
                            '<td class="text-center px-2 py-3" style="width: 50px;">' + deleteUi + '</td>' +
                        '</tr>';
            });
            lblTotal.innerText = formatRp<%=rnd%>(grandTotalModalValue<%=rnd%>);
        }
        container.innerHTML = htmlCart;
    };

    window.updateQtyModal<%=rnd%> = function(index, delta) {
        if(isModalReadOnly<%=rnd%>) return;
        cartDraftItems<%=rnd%>[index].jumlah += delta;
        if (cartDraftItems<%=rnd%>[index].jumlah <= 0) {
            cartDraftItems<%=rnd%>.splice(index, 1);
        }
        renderCartModal<%=rnd%>();
    };

    window.removeItemModal<%=rnd%> = function(index) {
        if(isModalReadOnly<%=rnd%>) return;
        cartDraftItems<%=rnd%>.splice(index, 1);
        renderCartModal<%=rnd%>();
    };

    // 4. Eksekusi Pembayaran via API
    window.prosesBayarPesanan<%=rnd%> = async function() {
        if (isModalReadOnly<%=rnd%>) return;

        const idCaraBayar = document.getElementById('selectCaraBayar<%=rnd%>').value;
        if (!idCaraBayar || idCaraBayar === "") {
            alert('<%=Common.getBahasaConfigJS("Silakan pilih Metode Pembayaran terlebih dahulu.")%>');
            document.getElementById('selectCaraBayar<%=rnd%>').focus();
            return;
        }

        if (cartDraftItems<%=rnd%>.length === 0) {
            alert('<%=Common.getBahasaConfigJS("Tidak ada barang untuk dibayar.")%>');
            return;
        }

        const btnBayar = document.getElementById('btnProsesBayar<%=rnd%>');
        const oriBtnHtml = btnBayar.innerHTML;
        btnBayar.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Memproses...")%>';
        btnBayar.disabled = true;

        const curWaktu = new Date().toISOString().slice(0, 19).replace('T', ' '); 
        const kodeTransaksi = "POS-" + new Date().getTime(); 

        // Susun Payload
        const payload = {
            action: "bayar",
            kodeUnik: kodeTransaksi,
            idToko: activeDraftIdToko<%=rnd%>, 
            waktu: curWaktu,
            log:"true",
            caraBayar: idCaraBayar,
            draftPembelianAnggotaKoperasi: activeDraftId<%=rnd%>, // INI PARAMETER KUNCI UNTUK MENUTUP / UBAH DRAFT
            transaksi: cartDraftItems<%=rnd%>.map(item => ({
                id: item.id, // ID Produk
                kode: item.kode,
                nama: item.nama,
                harga: item.harga,
                jumlah: item.jumlah,
                diskon: item.diskon,
                aturanDiskon: item.aturanDiskon, 
                cashback: item.cashback          
            }))
        };

        try {
            const res = await fetchDataAPI<%=rnd%>(payload);
            if (res.status === '00' || res.status === 'success') {
                
                // Tutup Modal
                batalBayar<%=rnd%>();
                
                // Segarkan Tabel Draft
                loadDataDraft<%=rnd%>();
                
                // Cetak Struk Custom seperti POS
                cetakStruk<%=rnd%>(payload, grandTotalModalValue<%=rnd%>, 0, 0);

            } else {
                alert(res.description || '<%=Common.getBahasaConfigJS("Gagal memproses pembayaran pesanan.")%>');
            }
        } catch (error) {
            console.error("Payment Error:", error);
            alert('<%=Common.getBahasaConfigJS("Koneksi terputus saat memproses transaksi.")%>');
        } finally {
            btnBayar.innerHTML = oriBtnHtml;
            btnBayar.disabled = false;
        }
    };

    // 5. Cetak Struk (Metode Tambahan Jika Ingin Akses ID Pembelian Langsung)
    window.cetakStrukPOS<%=rnd%> = function(idPembelian) {
        // Asumsi URL cetak struk bawaan AIS
        const printUrl = "<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=kantin%2Fpos&s=cetak_struk&id=" + idPembelian;
        
        // Buka PopUp Print
        const printWindow = window.open(printUrl, 'Cetak Struk Koperasi', 'height=600,width=400');
        if (window.focus && printWindow) {
            printWindow.focus();
        }
    };

    // Cetak Struk Custom HTML (Modul POS) yg dipanggil saat bayar selesai
    var cetakStruk<%=rnd%> = (payload, totalBayar, uangTunai, kembalian) => {
        const namaToko = activeDraftNamaToko<%=rnd%> || '<%=namaTokoAktif%>' || '<%=Common.getBahasaConfigJS("Koperasi Utama")%>';
        
        let htmlStruk = '<html><head><title><%=Common.getBahasaConfig("Cetak Struk")%></title>';
        htmlStruk += '<style>';
        htmlStruk += 'body { font-family: "Courier New", Courier, monospace; width: 300px; font-size: 12px; margin: 0 auto; padding: 10px; color: #000; } ';
        htmlStruk += '.text-center { text-align: center; } ';
        htmlStruk += '.text-right { text-align: right; } ';
        htmlStruk += '.bold { font-weight: bold; } ';
        htmlStruk += '.border-top { border-top: 1px dashed #000; margin-top: 5px; padding-top: 5px; } ';
        htmlStruk += '.border-bottom { border-bottom: 1px dashed #000; margin-bottom: 5px; padding-bottom: 5px; } ';
        htmlStruk += 'table { width: 100%; border-collapse: collapse; } ';
        htmlStruk += 'td { vertical-align: top; } ';
        htmlStruk += '</style></head><body>';
        
        htmlStruk += '<div class="text-center bold border-bottom">';
        htmlStruk += '<h3 style="margin:5px 0;">' + namaToko + '</h3>';
        htmlStruk += '<div style="margin-bottom:5px;"><%=Common.getBahasaConfig("Struk Pembayaran")%></div>';
        htmlStruk += '</div>';
        
        htmlStruk += '<div style="margin-bottom:5px;">';
        htmlStruk += '<%=Common.getBahasaConfigJS("Waktu")%> : ' + payload.waktu + '<br>';
        htmlStruk += '<%=Common.getBahasaConfigJS("No")%>    : ' + payload.kodeUnik + '<br>';
        htmlStruk += '</div>';
        
        htmlStruk += '<div class="border-top border-bottom">';
        htmlStruk += '<table>';
        
        let subtotalAwal = 0;
        let totalDiskonStruk = 0;
        let totalCashbackStruk = 0;

        payload.transaksi.forEach(item => {
            const sub = (item.harga * item.jumlah);
            subtotalAwal += sub;
            
            const itemDiskon = item.diskon || 0;
            const itemCashback = item.cashback || 0;
            
            totalDiskonStruk += itemDiskon;
            totalCashbackStruk += itemCashback;
            
            const subAfterDisc = sub - itemDiskon;
            
            htmlStruk += '<tr><td colspan="2">' + item.nama + '</td></tr>';
            htmlStruk += '<tr><td style="padding-bottom:5px;">' + item.jumlah + ' x ' + formatRp<%=rnd%>(item.harga) + '</td><td class="text-right" style="padding-bottom:5px;">' + formatRp<%=rnd%>(subAfterDisc) + '</td></tr>';
            
            // Tampilkan Note Promo di Struk Jika Ada
            if(itemDiskon > 0) {
                htmlStruk += '<tr><td colspan="2" style="padding-bottom:5px;">&nbsp;&nbsp;<i><%=Common.getBahasaConfig("Diskon Promo")%></i></td><td class="text-right">-' + formatRp<%=rnd%>(itemDiskon) + '</td></tr>';
            }
            if(itemCashback > 0) {
                htmlStruk += '<tr><td colspan="2" style="padding-bottom:5px;">&nbsp;&nbsp;<i><%=Common.getBahasaConfig("Cashback Didapat")%></i></td><td class="text-right">+' + formatRp<%=rnd%>(itemCashback) + '</td></tr>';
            }
        });
        
        htmlStruk += '</table></div>';
        
        htmlStruk += '<table>';
        htmlStruk += '<tr><td><%=Common.getBahasaConfig("Subtotal")%></td><td class="text-right">' + formatRp<%=rnd%>(subtotalAwal) + '</td></tr>';
        
        if (totalDiskonStruk > 0) {
            htmlStruk += '<tr><td><%=Common.getBahasaConfig("Total Diskon")%></td><td class="text-right">-' + formatRp<%=rnd%>(totalDiskonStruk) + '</td></tr>';
        }
        
        htmlStruk += '<tr><td class="bold"><%=Common.getBahasaConfig("Total Bayar")%></td><td class="text-right bold">' + formatRp<%=rnd%>(totalBayar) + '</td></tr>';
        
        if (totalCashbackStruk > 0) {
            htmlStruk += '<tr><td class="bold"><%=Common.getBahasaConfig("Total Cashback")%></td><td class="text-right bold">+' + formatRp<%=rnd%>(totalCashbackStruk) + '</td></tr>';
        }
        
        if (uangTunai > 0) {
            htmlStruk += '<tr><td><%=Common.getBahasaConfig("Uang Tunai")%></td><td class="text-right">' + formatRp<%=rnd%>(uangTunai) + '</td></tr>';
            htmlStruk += '<tr><td><%=Common.getBahasaConfig("Kembalian")%></td><td class="text-right">' + formatRp<%=rnd%>(kembalian) + '</td></tr>';
        }
        
        htmlStruk += '</table>';
        htmlStruk += '<div class="text-center border-top" style="margin-top: 15px;"><%=Common.getBahasaConfig("Terima Kasih")%><br><%=Common.getBahasaConfig("Selamat Belanja Kembali")%></div>';
        htmlStruk += '</body></html>';

        try{
	        const printWindow = window.open('', '', 'height=600,width=400');
	        printWindow.document.write(htmlStruk);
	        printWindow.document.close();
	        printWindow.focus();
	        setTimeout(() => {
	            printWindow.print();
	        }, 500);
        }catch(e){}
    };


    // ==========================================
    // LOGIKA PEMBAYARAN MASSAL (BAYAR SEMUA UNTUK ADMIN)
    // ==========================================
    window.konfirmasiBayarSemua<%=rnd%> = function() {
        if (!modalBayarSemuaInstance<%=rnd%>) {
            modalBayarSemuaInstance<%=rnd%> = new bootstrap.Modal(document.getElementById('modalBayarSemua<%=rnd%>'));
        }
        document.getElementById('progressBarContainer<%=rnd%>').style.display = 'none';
        document.getElementById('btnEksekusiBayarSemua<%=rnd%>').style.display = 'inline-block';
        document.getElementById('btnBatalBayarSemua<%=rnd%>').style.display = 'inline-block';
        document.getElementById('bayarSemuaStatus<%=rnd%>').innerHTML = '<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin memproses pembayaran SEMUA pesanan yang belum dibayar berdasarkan filter pencarian saat ini?")%>';
        
        modalBayarSemuaInstance<%=rnd%>.show();
    };

    window.prosesBayarkanSemua<%=rnd%> = async function() {
        const btnEksekusi = document.getElementById('btnEksekusiBayarSemua<%=rnd%>');
        const btnBatal = document.getElementById('btnBatalBayarSemua<%=rnd%>');
        const progressBarContainer = document.getElementById('progressBarContainer<%=rnd%>');
        const progressBar = document.getElementById('progressBarBayarSemua<%=rnd%>');
        const statusText = document.getElementById('bayarSemuaStatus<%=rnd%>');

        btnEksekusi.style.display = 'none';
        btnBatal.style.display = 'none';
        progressBarContainer.style.display = 'block';
        progressBar.style.width = '0%';
        progressBar.innerHTML = '0%';
        
        const sqlFilters = getFilterQueryDraft<%=rnd%>();
        const sqlUnpaid = "SELECT a.id, a.kode, a.toko as id_toko, a.anggota_koperasi, a.cara_pembayaran_koperasi " +
                          "FROM koperasi.draft_pembelian_anggota_koperasi a " +
                          "LEFT JOIN koperasi.anggota_koperasi b ON a.anggota_koperasi = b.id " +
                          "LEFT JOIN koperasi.toko t ON a.toko = t.id " +
                          sqlFilters + " AND a.lunas IS NULL ORDER BY a.id ASC;";

        try {
            statusText.innerHTML = '<%=Common.getBahasaConfigJS("Mengambil data pesanan yang belum dilunasi...")%>';
            const resUnpaid = await fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlUnpaid });
            const listUnpaid = resUnpaid.data || [];

            if (listUnpaid.length === 0) {
                statusText.innerHTML = '<span class="text-warning fw-bold"><i class="fas fa-exclamation-triangle me-1"></i><%=Common.getBahasaConfig("Tidak ada pesanan yang perlu dibayar pada filter saat ini.")%></span>';
                btnBatal.style.display = 'inline-block';
                btnBatal.innerHTML = '<%=Common.getBahasaConfigJS("Tutup")%>';
                return;
            }

            let successCount = 0;
            let failCount = 0;

            for (let i = 0; i < listUnpaid.length; i++) {
                const draft = listUnpaid[i];
                statusText.innerHTML = `<%=Common.getBahasaConfig("Memproses pesanan")%> ${i+1} / ${listUnpaid.length} <br> <span class="small fw-bold text-primary">(${draft.kode})</span>`;
                
                const sqlDetail = "SELECT d.id as draft_detail_id, p.id as produk_id, p.kode, d.nama, d.hargasatuan as harga, d.qty as jumlah, d.diskon, d.aturan_diskon, d.cashback " +
                                  "FROM koperasi.draft_pembelian d " +
                                  "LEFT JOIN koperasi.produk p ON d.produk = p.id " +
                                  "WHERE d.draft_pembelian_anggota_koperasi = " + draft.id + ";";
                                  
                const resDetail = await fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlDetail });
                const items = resDetail.data || [];

                if (items.length > 0) {
                    const curWaktu = new Date().toISOString().slice(0, 19).replace('T', ' '); 
                    const kodeTransaksi = "POS-MASSAL-" + new Date().getTime() + "-" + i;
                    const caraBayar = draft.cara_pembayaran_koperasi || '1'; // Default ke ID 1 jika null
                    
                    const payload = {
                        action: "bayar",
                        kodeUnik: kodeTransaksi,
                        idToko: draft.id_toko, 
                        waktu: curWaktu,
                        log: "true",
                        caraBayar: caraBayar,
                        draftPembelianAnggotaKoperasi: draft.id,
                        transaksi: items.map(item => ({
                            id: item.produk_id, 
                            kode: item.kode,
                            nama: item.nama,
                            harga: parseFloat(item.harga||0),
                            jumlah: parseFloat(item.jumlah||1),
                            diskon: parseFloat(item.diskon||0),
                            aturanDiskon: item.aturan_diskon, 
                            cashback: parseFloat(item.cashback||0)
                        }))
                    };

                    const resBayar = await fetchDataAPI<%=rnd%>(payload);
                    if (resBayar.status === '00' || resBayar.status === 'success') {
                        successCount++;
                    } else {
                        failCount++;
                    }
                } else {
                    failCount++;
                }

                // Update Progress Bar UI
                const percent = Math.round(((i + 1) / listUnpaid.length) * 100);
                progressBar.style.width = percent + '%';
                progressBar.innerHTML = percent + '%';
            }

            statusText.innerHTML = '<span class="text-success fw-bold"><i class="fas fa-check-circle me-1"></i><%=Common.getBahasaConfig("Selesai diproses!")%></span><br><small class="text-muted"><%=Common.getBahasaConfig("Berhasil:")%> ' + successCount + ', <%=Common.getBahasaConfig("Gagal:")%> ' + failCount + '</small>';
            btnBatal.style.display = 'inline-block';
            btnBatal.innerHTML = '<%=Common.getBahasaConfigJS("Selesai & Tutup")%>';
            loadDataDraft<%=rnd%>();

        } catch (e) {
            console.error(e);
            statusText.innerHTML = '<span class="text-danger fw-bold"><i class="fas fa-times-circle me-1"></i><%=Common.getBahasaConfig("Terjadi kesalahan jaringan.")%></span>';
            btnBatal.style.display = 'inline-block';
        }
    };
    
    // ==========================================
    // LOGIKA LAYANI SEMUA (MASSAL PEDAGANG)
    // ==========================================
    window.konfirmasiLayaniSemua<%=rnd%> = function() {
        if (!modalLayaniSemuaInstance<%=rnd%>) {
            modalLayaniSemuaInstance<%=rnd%> = new bootstrap.Modal(document.getElementById('modalLayaniSemua<%=rnd%>'));
        }
        document.getElementById('progressBarContainerLayani<%=rnd%>').style.display = 'none';
        document.getElementById('btnEksekusiLayaniSemua<%=rnd%>').style.display = 'inline-block';
        document.getElementById('btnBatalLayaniSemua<%=rnd%>').style.display = 'inline-block';
        document.getElementById('layaniSemuaStatus<%=rnd%>').innerHTML = '<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin memproses dan melayani SEMUA pesanan yang masuk berdasarkan filter saat ini?")%>';
        
        modalLayaniSemuaInstance<%=rnd%>.show();
    };

    window.prosesLayaniSemua<%=rnd%> = async function() {
        const btnEksekusi = document.getElementById('btnEksekusiLayaniSemua<%=rnd%>');
        const btnBatal = document.getElementById('btnBatalLayaniSemua<%=rnd%>');
        const progressBarContainer = document.getElementById('progressBarContainerLayani<%=rnd%>');
        const progressBar = document.getElementById('progressBarLayaniSemua<%=rnd%>');
        const statusText = document.getElementById('layaniSemuaStatus<%=rnd%>');

        btnEksekusi.style.display = 'none';
        btnBatal.style.display = 'none';
        progressBarContainer.style.display = 'block';
        progressBar.style.width = '0%';
        progressBar.innerHTML = '0%';
        
        const sqlFilters = getFilterQueryDraft<%=rnd%>();
        const sqlUnpaid = "SELECT a.id, a.kode, a.toko as id_toko, a.anggota_koperasi, a.cara_pembayaran_koperasi " +
                          "FROM koperasi.draft_pembelian_anggota_koperasi a " +
                          "LEFT JOIN koperasi.anggota_koperasi b ON a.anggota_koperasi = b.id " +
                          "LEFT JOIN koperasi.toko t ON a.toko = t.id " +
                          sqlFilters + " AND a.lunas IS NULL ORDER BY a.id ASC;";

        try {
            statusText.innerHTML = '<%=Common.getBahasaConfigJS("Mengambil data pesanan yang belum dilayani...")%>';
            const resUnpaid = await fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlUnpaid });
            const listUnpaid = resUnpaid.data || [];

            if (listUnpaid.length === 0) {
                statusText.innerHTML = '<span class="text-warning fw-bold"><i class="fas fa-exclamation-triangle me-1"></i><%=Common.getBahasaConfig("Tidak ada pesanan yang perlu dilayani pada filter saat ini.")%></span>';
                btnBatal.style.display = 'inline-block';
                btnBatal.innerHTML = '<%=Common.getBahasaConfigJS("Tutup")%>';
                return;
            }

            let successCount = 0;
            let failCount = 0;

            for (let i = 0; i < listUnpaid.length; i++) {
                const draft = listUnpaid[i];
                statusText.innerHTML = `<%=Common.getBahasaConfig("Memproses pesanan")%> ${i+1} / ${listUnpaid.length} <br> <span class="small fw-bold text-primary">(${draft.kode})</span>`;
                
                const sqlDetail = "SELECT d.id as draft_detail_id, p.id as produk_id, p.kode, d.nama, d.hargasatuan as harga, d.qty as jumlah, d.diskon, d.aturan_diskon, d.cashback " +
                                  "FROM koperasi.draft_pembelian d " +
                                  "LEFT JOIN koperasi.produk p ON d.produk = p.id " +
                                  "WHERE d.draft_pembelian_anggota_koperasi = " + draft.id + ";";
                                  
                const resDetail = await fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlDetail });
                const items = resDetail.data || [];

                if (items.length > 0) {
                    const curWaktu = new Date().toISOString().slice(0, 19).replace('T', ' '); 
                    const kodeTransaksi = "POS-MASSAL-" + new Date().getTime() + "-" + i;
                    const caraBayar = draft.cara_pembayaran_koperasi || '1'; 
                    
                    const payload = {
                        action: "bayar",
                        kodeUnik: kodeTransaksi,
                        idToko: draft.id_toko, 
                        waktu: curWaktu,
                        log: "true",
                        caraBayar: caraBayar,
                        draftPembelianAnggotaKoperasi: draft.id,
                        transaksi: items.map(item => ({
                            id: item.produk_id, 
                            kode: item.kode,
                            nama: item.nama,
                            harga: parseFloat(item.harga||0),
                            jumlah: parseFloat(item.jumlah||1),
                            diskon: parseFloat(item.diskon||0),
                            aturanDiskon: item.aturan_diskon, 
                            cashback: parseFloat(item.cashback||0)
                        }))
                    };

                    const resBayar = await fetchDataAPI<%=rnd%>(payload);
                    if (resBayar.status === '00' || resBayar.status === 'success') {
                        successCount++;
                    } else {
                        failCount++;
                    }
                } else {
                    failCount++;
                }

                // Update Progress Bar UI
                const percent = Math.round(((i + 1) / listUnpaid.length) * 100);
                progressBar.style.width = percent + '%';
                progressBar.innerHTML = percent + '%';
            }

            statusText.innerHTML = '<span class="text-success fw-bold"><i class="fas fa-check-circle me-1"></i><%=Common.getBahasaConfig("Seluruh pesanan berhasil dilayani!")%></span><br><small class="text-muted"><%=Common.getBahasaConfig("Berhasil:")%> ' + successCount + ', <%=Common.getBahasaConfig("Gagal:")%> ' + failCount + '</small>';
            btnBatal.style.display = 'inline-block';
            btnBatal.innerHTML = '<%=Common.getBahasaConfigJS("Selesai & Tutup")%>';
            loadDataDraft<%=rnd%>();
            
            // Sembunyikan notifikasi merah di bawah jika sudah terlayani semua
            tutupToastPesanan<%=rnd%>();

        } catch (e) {
            console.error(e);
            statusText.innerHTML = '<span class="text-danger fw-bold"><i class="fas fa-times-circle me-1"></i><%=Common.getBahasaConfig("Terjadi kesalahan jaringan.")%></span>';
            btnBatal.style.display = 'inline-block';
        }
    };


    // ==========================================
    // LOGIKA POLLING & TOAST KIRI BAWAH (PEDAGANG ONLY)
    // ==========================================
    var cekPesananDraftBaru<%=rnd%> = async () => {
        if (!isLoginPedagang<%=rnd%>) return; 
        
        if (!currentTokoId<%=rnd%>) return;

        // Hitung draft pembelian yang LUNAS-nya masih kosong/null
        const sqlCek = "SELECT COUNT(id) as jml FROM koperasi.draft_pembelian_anggota_koperasi WHERE toko = " + currentTokoId<%=rnd%> + " AND (lunas IS NULL);";
        
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: "sql", sql: sqlCek, tanpaLoading: true }) 
            });
            const dataResponse = await res.json();
            const count = (dataResponse.data && dataResponse.data[0]) ? parseInt(dataResponse.data[0].jml || 0) : 0;
            
            const toastEl = document.getElementById('toastPesananBaru<%=rnd%>');
            
            if (count > 0) {
                document.getElementById('lblJmlPesananBaru<%=rnd%>').innerText = count;
                toastEl.classList.remove('d-none');
                toastEl.classList.add('show');
            } else {
                toastEl.classList.add('d-none');
                toastEl.classList.remove('show');
            }
        } catch (e) {
            // Silently ignore polling errors
        }
    };

    window.tutupToastPesanan<%=rnd%> = function() {
        document.getElementById('toastPesananBaru<%=rnd%>').classList.remove('show');
        document.getElementById('toastPesananBaru<%=rnd%>').classList.add('d-none');
    };

    window.lihatPesananBaru<%=rnd%> = function() {
        // Arahkan ke tab riwayat transaksi jika perlu (asumsi pedagang melihat di tab terpisah)
        // Di sini kita trigger klik ke tab Riwayat (sesuaikan dengan ID tab Anda)
        const tabRiwayat = document.getElementById('tab-riwayat-<%=rnd%>');
        if (tabRiwayat) {
             tabRiwayat.click();
             setTimeout(() => {
                 const tabTransaksi = document.getElementById('subtab-transaksi-<%=rnd%>');
                 if (tabTransaksi) tabTransaksi.click();
             }, 200);
        } else {
            // Fallback, scroll saja
            document.getElementById('draftManagerContent<%=rnd%>').scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    };

    // ==========================================
    // LOGIKA AUTO-VERIFIKASI BAYAR OVERDUE (> 12 MALAM)
    // ==========================================
    var autoVerifikasiOverdue<%=rnd%> = async () => {
        const isAutoVerifAktif = <%=otomatisVerifikasiBayarSetelahJam24%>;
        if (!isAutoVerifAktif) return;

        let filterTokoUpdate = "";
        <% if (toko != null) { %>
            filterTokoUpdate = " AND a.toko = <%=toko.getId()%> ";
        <% } %>

        const sqlUnpaid = "SELECT a.id, a.kode, a.toko as id_toko, a.anggota_koperasi, a.cara_pembayaran_koperasi " +
                          "FROM koperasi.draft_pembelian_anggota_koperasi a " +
                          "WHERE a.lunas IS NULL AND DATE(a.tanggal_pembayaran) < CURRENT_DATE " + filterTokoUpdate + " ORDER BY a.id ASC;";

        try {
            const resUnpaid = await fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlUnpaid, tanpaLoading: true });
            const listUnpaid = resUnpaid.data || [];

            if (listUnpaid.length > 0) {
                if (!document.getElementById('modalAutoVerifProses<%=rnd%>')) {
                    const modalProgress = `
                    <div class="modal fade" id="modalAutoVerifProses<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
                        <div class="modal-dialog modal-dialog-centered">
                            <div class="modal-content rounded-4 border-0 shadow-lg">
                                <div class="modal-body p-5 text-center">
                                    <i class="fas fa-magic fa-3x text-primary mb-3 animate__animated animate__bounce"></i>
                                    <h4 class="fw-bolder text-dark mb-2"><%=Common.getBahasaConfig("Verifikasi Otomatis")%></h4>
                                    <p class="text-muted small mb-4" id="autoVerifStatus<%=rnd%>"><%=Common.getBahasaConfig("Sistem memverifikasi pembayaran pesanan yang terlewat dari hari sebelumnya...")%></p>
                                    <div class="progress shadow-sm mb-3" style="height: 20px; border-radius: 10px;">
                                        <div id="autoVerifProgress<%=rnd%>" class="progress-bar progress-bar-striped progress-bar-animated bg-primary fw-bold" role="progressbar" style="width: 0%;">0%</div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>`;
                    document.body.insertAdjacentHTML('beforeend', modalProgress);
                }
                
                const modalVerif = new bootstrap.Modal(document.getElementById('modalAutoVerifProses<%=rnd%>'));
                modalVerif.show();

                let successCount = 0;
                let failCount = 0;
                let pesanGagalTerakhir = '';
                for (let i = 0; i < listUnpaid.length; i++) {
                    const draft = listUnpaid[i];
                    document.getElementById('autoVerifStatus<%=rnd%>').innerHTML = `<%=Common.getBahasaConfig("Memproses pesanan")%> ${i+1} / ${listUnpaid.length} <br> <span class="small fw-bold text-primary">(${draft.kode})</span>`;
                    
                    const sqlDetail = "SELECT d.id as draft_detail_id, p.id as produk_id, p.kode, d.nama, d.hargasatuan as harga, d.qty as jumlah, d.diskon, d.aturan_diskon, d.cashback " +
                                      "FROM koperasi.draft_pembelian d LEFT JOIN koperasi.produk p ON d.produk = p.id " +
                                      "WHERE d.draft_pembelian_anggota_koperasi = " + draft.id + ";";
                    const resDetail = await fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlDetail, tanpaLoading: true });
                    const items = resDetail.data || [];
                    
                    if (items.length > 0) {
                        const curWaktu = new Date().toISOString().slice(0, 19).replace('T', ' '); 
                        const kodeTransaksi = "POS-AUTO-" + new Date().getTime() + "-" + i;
                        const caraBayar = draft.cara_pembayaran_koperasi || '1'; 
                        
                        const payload = {
                            action: "bayar",
                            kodeUnik: kodeTransaksi,
                            idToko: draft.id_toko, 
                            waktu: curWaktu,
                            log: "true",
                            kanalCheckout: "otomatis_halaman",
                            caraBayar: caraBayar,
                            draftPembelianAnggotaKoperasi: draft.id,
                            transaksi: items.map(item => ({
                                id: item.produk_id, kode: item.kode, nama: item.nama,
                                harga: parseFloat(item.harga||0), jumlah: parseFloat(item.jumlah||1),
                                diskon: parseFloat(item.diskon||0), aturanDiskon: item.aturan_diskon, 
                                cashback: parseFloat(item.cashback||0)
                            }))
                        };

                        const resBayar = await fetchDataAPI<%=rnd%>(payload);
                        if (resBayar.status === '00' || resBayar.status === 'success') {
                            successCount++;
                        } else {
                            // Kegagalan TIDAK boleh diam-diam: insiden 2026-08-18, seluruh verifikasi
                            // otomatis ditolak gerbang sesi kas (status 91) tetapi layar tetap
                            // menampilkan modal sukses -- pesanan macet "belum bayar" tanpa jejak.
                            failCount++;
                            pesanGagalTerakhir = resBayar.description || resBayar.message || ('status ' + resBayar.status);
                            console.error('Auto-verifikasi gagal utk draft ' + draft.kode + ':', resBayar);
                        }
                    }
                    
                    const percent = Math.round(((i + 1) / listUnpaid.length) * 100);
                    document.getElementById('autoVerifProgress<%=rnd%>').style.width = percent + '%';
                    document.getElementById('autoVerifProgress<%=rnd%>').innerHTML = percent + '%';
                }
                
                modalVerif.hide();
                
                // Show success info modal
                const modalHtml = `
                <div class="modal fade animate__animated animate__zoomIn" id="modalAutoVerifSukses<%=rnd%>" tabindex="-1" aria-hidden="true">
                    <div class="modal-dialog modal-dialog-centered">
                        <div class="modal-content rounded-4 border-0 shadow-lg">
                            <div class="modal-body p-5 text-center">
                                <div class="mb-4">
                                    <div class="d-inline-flex align-items-center justify-content-center bg-primary bg-opacity-10 text-primary rounded-circle" style="width: 80px; height: 80px;">
                                        <i class="fas fa-magic fa-3x"></i>
                                    </div>
                                </div>
                                <h4 class="fw-bolder text-dark mb-3"><%=Common.getBahasaConfig("Penyelesaian Otomatis")%></h4>
                                <p class="text-muted fs-6 mb-4"><%=Common.getBahasaConfig("Sistem telah otomatis memverifikasi pembayaran")%> <b class="text-primary fs-5">` + successCount + `</b> <%=Common.getBahasaConfig("pesanan dari hari sebelumnya menjadi terbayar.")%></p>` +
                                (failCount > 0 ? `<div class="alert alert-danger text-start small rounded-3 mb-4"><i class="fas fa-triangle-exclamation me-1"></i><b>` + failCount + ` <%=Common.getBahasaConfig("pesanan GAGAL diverifikasi otomatis.")%></b><br>` + pesanGagalTerakhir + `</div>` : '') + `
                                <button type="button" class="btn btn-primary px-5 rounded-pill fw-bold shadow-sm" data-bs-dismiss="modal"><i class="fas fa-check me-2"></i><%=Common.getBahasaConfig("Mengerti")%></button>
                            </div>
                        </div>
                    </div>
                </div>`;
                document.body.insertAdjacentHTML('beforeend', modalHtml);
                const autoModal = new bootstrap.Modal(document.getElementById('modalAutoVerifSukses<%=rnd%>'));
                autoModal.show();
                
                document.getElementById('modalAutoVerifSukses<%=rnd%>').addEventListener('hidden.bs.modal', function () {
                    this.remove(); 
                    loadDataDraft<%=rnd%>(); 
                });
            }
        } catch (e) {
            console.error("Gagal auto-verifikasi overdue", e);
        }
    };


    // ==========================================
    // INISIALISASI HALAMAN
    // ==========================================
    document.addEventListener("DOMContentLoaded", async () => {
        renderModalAndToastHTML<%=rnd%>();
        setFilterDefaultDatesDraft<%=rnd%>();
        
        const filterInputs = document.querySelectorAll('.filter-draft-<%=rnd%>');
        filterInputs.forEach(input => {
            input.addEventListener("keydown", (event) => {
                if (event.key === "Enter") {
                    event.preventDefault(); 
                    resetAndLoadDraft<%=rnd%>();
                }
            });
        });

        loadCaraBayarPOS<%=rnd%>();
        
        // Panggil auto verifikasi
        await autoVerifikasiOverdue<%=rnd%>();
        
        loadDataDraft<%=rnd%>();

        // Polling setiap 10 detik jika login sebagai Pedagang
        if (isLoginPedagang<%=rnd%>) {
            pollingInterval<%=rnd%> = setInterval(cekPesananDraftBaru<%=rnd%>, 10000); 
            setTimeout(cekPesananDraftBaru<%=rnd%>, 1500); // Trigger pertama
        }
    });

</script>
