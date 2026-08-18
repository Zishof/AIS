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
boolean bolehSupervisorRiwayat = pedagang == null || Boolean.TRUE.equals(pedagang.getSupervisor());
String rnd = Common.getGeneratedBarCode(7);

// Menyiapkan variabel filter SQL jika user adalah pedagang
String sqlFilterToko = "";
if (toko != null) {
    sqlFilterToko = " AND a.toko = " + toko.getId() + " ";
}

// Gate "b.aktif" HANYA utk view admin (toko==null, lihat SEMUA toko -- sembunyikan yg non-aktif
// biar tak berantakan). JANGAN diterapkan pada view pedagang sendiri (toko!=null): tenant yg
// storenya di-nonaktifkan admin (mis. suspend sementara) TETAP harus bisa lihat riwayat & pesanan
// yg belum dilayani miliknya sendiri -- kalau tidak, akun tenant tsb tampak "error"/riwayat kosong
// padahal datanya ada (lihat juga grafik Tren Transaksi yg TAK pakai join b.aktif sama sekali).
String joinTokoAktif = (toko != null) ? "" : " and b.aktif";
%>

<div class="row g-4 mb-4 animate__animated animate__fadeInUp">
    <div class="col-lg-12">
        <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
            <div class="card-body p-4">

                <div class="d-flex flex-column flex-md-row justify-content-between align-items-center mb-4 pb-3 border-bottom border-light gap-3">
                    <div class="text-center text-md-start">
                        <h5 class="fw-bold text-dark mb-1">
                            <i class="fas fa-history me-2 text-primary"></i>
                            <% if (toko != null) { %>
                                <%=Common.getBahasaConfig("Riwayat Transaksi - ")%> <span class="text-primary"><%=toko.getNama()%></span>
                            <% } else { %>
                                <%=Common.getBahasaConfig("Riwayat Transaksi Keseluruhan")%>
                            <% } %>
                        </h5>
                        <small class="text-muted"><%=Common.getBahasaConfig("Pantau detail setiap transaksi yang masuk ke dalam sistem.")%></small>
                    </div>
                    <div class="d-flex flex-wrap gap-2 justify-content-center">
                        
                        <% if (toko != null) { %>
                        <!-- Tombol Layani Semua (Hanya untuk Pedagang) -->
                        <button class="btn btn-success rounded-pill px-4 fw-semibold shadow-sm text-nowrap" type="button" onclick="konfirmasiLayaniSemua<%=rnd%>()">
                            <i class="fas fa-check-double me-2"></i><%=Common.getBahasaConfig("Layani Semua")%>
                        </button>
                        <% } %>

                        <button class="btn btn-primary rounded-pill px-4 fw-semibold shadow-sm text-nowrap"
                            type="button" data-bs-toggle="collapse"
                            data-bs-target="#collapseFilter<%=rnd%>" aria-expanded="false"
                            aria-controls="collapseFilter<%=rnd%>" id="btnToggleFilter<%=rnd%>">
                            <i class="fas fa-filter me-2"></i><%=Common.getBahasaConfig("Saring Data")%>
                        </button>
                    </div>
                </div>

                <div class="collapse mb-4" id="collapseFilter<%=rnd%>">
                    <div class="card card-body bg-light border-0 rounded-4 p-4 shadow-sm">
                        <div class="row g-3">
                            <div class="col-md-3">
                                <label class="form-label small fw-semibold text-secondary mb-1"><i class="far fa-calendar-alt me-1"></i><%=Common.getBahasaConfig("Tanggal Mulai")%></label>
                                <input type="date" class="form-control form-control-sm rounded-3 tx-filter<%=rnd%> border-0 shadow-sm" id="filterStart<%=rnd%>">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-semibold text-secondary mb-1"><i class="far fa-calendar-check me-1"></i><%=Common.getBahasaConfig("Tanggal Akhir")%></label>
                                <input type="date" class="form-control form-control-sm rounded-3 tx-filter<%=rnd%> border-0 shadow-sm" id="filterEnd<%=rnd%>">
                            </div>
                            
                            <% if (toko == null) { %>
                            <div class="col-md-3">
                                <label class="form-label small fw-semibold text-secondary mb-1"><i class="fas fa-store me-1"></i><%=Common.getBahasaConfig("Nama Pedagang")%></label>
                                <input type="text" class="form-control form-control-sm rounded-3 tx-filter<%=rnd%> border-0 shadow-sm" id="filterMerchantName<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik nama pedagang")%>">
                            </div>
                            <% } %>
                            
                            <div class="col-md-3">
                                <label class="form-label small fw-semibold text-secondary mb-1"><i class="fas fa-user-tag me-1"></i><%=Common.getBahasaConfig("Nama Pembeli")%></label>
                                <input type="text" class="form-control form-control-sm rounded-3 tx-filter<%=rnd%> border-0 shadow-sm" id="filterBuyerName<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik nama pembeli")%>">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-semibold text-secondary mb-1"><i class="fas fa-id-card me-1"></i><%=Common.getBahasaConfig("Tipe / Jenis Member")%></label>
                                <input type="text" class="form-control form-control-sm rounded-3 tx-filter<%=rnd%> border-0 shadow-sm" id="filterTipeJenisMember<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: Mahasiswa / Reguler")%>">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-semibold text-secondary mb-1"><i class="fas fa-box-open me-1"></i><%=Common.getBahasaConfig("Nama Barang")%></label>
                                <input type="text" class="form-control form-control-sm rounded-3 tx-filter<%=rnd%> border-0 shadow-sm" id="filterItemName<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik nama barang")%>">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-semibold text-secondary mb-1"><i class="fas fa-wallet me-1"></i><%=Common.getBahasaConfig("Metode Bayar")%></label>
                                <input type="text" class="form-control form-control-sm rounded-3 tx-filter<%=rnd%> border-0 shadow-sm" id="filterPaymentMethod<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: Tunai / QRIS")%>">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-semibold text-secondary mb-1"><i class="fas fa-sort-amount-down me-1"></i><%=Common.getBahasaConfig("Kuantitas Min")%></label>
                                <input type="number" class="form-control form-control-sm rounded-3 tx-filter<%=rnd%> border-0 shadow-sm" id="filterQtyMulai<%=rnd%>" placeholder="0">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-semibold text-secondary mb-1"><i class="fas fa-sort-amount-up me-1"></i><%=Common.getBahasaConfig("Kuantitas Max")%></label>
                                <input type="number" class="form-control form-control-sm rounded-3 tx-filter<%=rnd%> border-0 shadow-sm" id="filterQtySampai<%=rnd%>" placeholder="0">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-semibold text-secondary mb-1"><i class="fas fa-tag me-1"></i><%=Common.getBahasaConfig("Harga Terendah")%></label>
                                <input type="number" class="form-control form-control-sm rounded-3 tx-filter<%=rnd%> border-0 shadow-sm" id="filterHargaMulai<%=rnd%>" placeholder="Rp 0">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-semibold text-secondary mb-1"><i class="fas fa-tags me-1"></i><%=Common.getBahasaConfig("Harga Tertinggi")%></label>
                                <input type="number" class="form-control form-control-sm rounded-3 tx-filter<%=rnd%> border-0 shadow-sm" id="filterHargaSampai<%=rnd%>" placeholder="Rp 0">
                            </div>
                            
                            <div class="col-md-6 d-flex align-items-end gap-2 mt-3">
                                <button class="btn btn-primary btn-sm rounded-pill px-4 shadow-sm flex-grow-1 fw-bold" type="button" id="btnSearch<%=rnd%>" onclick="triggerSearch<%=rnd%>()">
                                    <i class="fas fa-search me-2"></i><%=Common.getBahasaConfig("Terapkan Filter")%>
                                </button>
                                <button class="btn btn-success btn-sm rounded-pill px-4 shadow-sm fw-bold" type="button" id="btnExportExcel<%=rnd%>" onclick="downloadExcelAPI<%=rnd%>()">
                                    <i class="fas fa-file-excel me-2"></i><%=Common.getBahasaConfig("Unduh Excel")%>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="table-responsive border rounded-3 shadow-sm bg-white">
                    <table class="table table-hover table-striped align-middle mb-0">
                        <thead class="table-dark text-center align-middle">
                            <tr>
                                <th class="fw-semibold text-uppercase small py-3"><i class="far fa-clock me-1"></i><%=Common.getBahasaConfig("Waktu")%></th>
                                <% if (toko == null) { %>
                                    <th class="fw-semibold text-uppercase small text-start"><i class="fas fa-store me-1"></i><%=Common.getBahasaConfig("Pedagang")%></th>
                                <% } %>
                                <th class="fw-semibold text-uppercase small text-start"><i class="fas fa-box me-1"></i><%=Common.getBahasaConfig("Barang")%></th>
                                <th class="fw-semibold text-uppercase small text-start"><i class="fas fa-user me-1"></i><%=Common.getBahasaConfig("Pembeli")%></th>
                                <th class="fw-semibold text-uppercase small"><i class="fas fa-id-card me-1"></i><%=Common.getBahasaConfig("Tipe Anggota")%></th>
                                <th class="fw-semibold text-uppercase small"><i class="fas fa-wallet me-1"></i><%=Common.getBahasaConfig("Metode")%></th>
                                <th class="fw-semibold text-uppercase small"><i class="fas fa-layer-group me-1"></i><%=Common.getBahasaConfig("Qty")%></th>
                                <th class="fw-semibold text-uppercase small text-end px-3"><i class="fas fa-coins me-1"></i><%=Common.getBahasaConfig("Total Harga")%></th>
                                <th class="fw-semibold text-uppercase small text-center px-3" style="width: 140px;"><i class="fas fa-concierge-bell me-1"></i><%=Common.getBahasaConfig("Status Layanan")%></th>
                                <% if (bolehSupervisorRiwayat) { %>
                                    <th class="fw-semibold text-uppercase small text-center px-3" style="width: 110px;"><i class="fas fa-tools me-1"></i><%=Common.getBahasaConfig("Aksi")%></th>
                                <% } %>
                            </tr>
                        </thead>
                        <tbody id="latestTransactionsTable<%=rnd%>">
                            </tbody>
                    </table>
                </div>

                <div class="d-flex justify-content-between align-items-center mt-4 border-top pt-3">
                    <span class="text-muted small fw-medium bg-light px-3 py-1 rounded-pill border" id="pagingInfo<%=rnd%>"></span>
                    <nav>
                        <ul class="pagination pagination-sm mb-0 shadow-sm">
                            <li class="page-item">
                                <button class="page-link text-primary rounded-start-pill px-3" id="btnPrev<%=rnd%>" onclick="changePage<%=rnd%>(-1)">
                                    <i class="fas fa-chevron-left"></i>
                                </button>
                            </li>
                            <li class="page-item disabled">
                                <span class="page-link fw-bold bg-light text-dark px-3" id="pageNumber<%=rnd%>">1</span>
                            </li>
                            <li class="page-item">
                                <button class="page-link text-primary rounded-end-pill px-3" id="btnNext<%=rnd%>" onclick="changePage<%=rnd%>(1)">
                                    <i class="fas fa-chevron-right"></i>
                                </button>
                            </li>
                        </ul>
                    </nav>
                </div>

            </div>
        </div>
    </div>
</div>

<!-- ==========================================
     POPUP OTOMATIS TERLAYANI (AUTO SERVE)
=========================================== -->
<div class="modal fade" id="modalAutoServe<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-4 border-0 shadow-lg">
            <div class="modal-header bg-info text-white border-bottom-0 pb-2 pt-3 px-4">
                <h5 class="modal-title fw-bold">
                    <i class="fas fa-info-circle me-2"></i><%=Common.getBahasaConfig("Informasi Sistem")%>
                </h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4 text-center">
                <div class="mb-3">
                    <i class="fas fa-magic fa-3x text-info"></i>
                </div>
                <p class="text-dark mb-0 fs-5" id="autoServeText<%=rnd%>"></p>
            </div>
            <div class="modal-footer border-top-0 justify-content-center">
                <button type="button" class="btn btn-info text-white px-5 rounded-pill fw-bold shadow-sm" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Mengerti")%></button>
            </div>
        </div>
    </div>
</div>

<!-- ==========================================
     POPUP / TOAST NOTIFIKASI PESANAN BARU
=========================================== -->
<div class="position-fixed bottom-0 start-0 p-4" style="z-index: 1080;">
    <div id="toastPesananBaru<%=rnd%>" class="toast shadow-lg border-0 rounded-4 d-none" role="alert" aria-live="assertive" aria-atomic="true" data-bs-autohide="false">
        <div class="toast-header bg-warning text-dark border-bottom-0 rounded-top-4 py-3">
            <i class="fas fa-bell fa-shake me-2 fs-5"></i>
            <strong class="me-auto fs-6"><%=Common.getBahasaConfig("Ada Pesanan Masuk!")%></strong>
            <button type="button" class="btn-close" onclick="tutupToastPesanan<%=rnd%>()"></button>
        </div>
        <div class="toast-body bg-white rounded-bottom-4 p-4 text-center">
            <p class="mb-2"><%=Common.getBahasaConfig("Terdapat")%> <b id="lblJmlPesananBaru<%=rnd%>" class="text-danger fs-4">0</b> <%=Common.getBahasaConfig("pesanan yang belum Anda layani/selesaikan.")%></p>
            <div class="mt-3 pt-3 border-top d-flex flex-column gap-2">
                <button type="button" class="btn btn-primary rounded-pill w-100 fw-bold shadow-sm" onclick="lihatPesananBaru<%=rnd%>()">
                    <i class="fas fa-eye me-2"></i><%=Common.getBahasaConfig("Lihat Pesanan")%>
                </button>
                <button type="button" class="btn btn-success rounded-pill w-100 fw-bold shadow-sm" onclick="konfirmasiLayaniSemua<%=rnd%>()">
                    <i class="fas fa-check-double me-2"></i><%=Common.getBahasaConfig("Layani Semua")%>
                </button>
            </div>
        </div>
    </div>
</div>

<script>
    const lang<%=rnd%> = {
        emptyData: '<%=Common.getBahasaConfigJS("Tidak ada data transaksi yang ditemukan.")%>',
        showing: '<%=Common.getBahasaConfigJS("Menampilkan")%>',
        from: '<%=Common.getBahasaConfigJS("dari total")%>',
        data: '<%=Common.getBahasaConfigJS("data")%>',
        errorFetch: '<%=Common.getBahasaConfigJS("Gagal mengambil data dari server. Periksa koneksi internet Anda.")%>',
        loading: '<%=Common.getBahasaConfigJS("Memuat data transaksi...")%>',
        downloading: '<%=Common.getBahasaConfigJS("Menyiapkan Excel...")%>'
    };

    const isLoginPedagang<%=rnd%> = <%= toko != null %>;
    const bolehSupervisorRiwayat<%=rnd%> = <%= bolehSupervisorRiwayat %>;
    const serviceRiwayatTransaksi<%=rnd%> = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=kantin&s=riwayat_transaksi_service';
    const filterTokoSQL<%=rnd%> = "<%=sqlFilterToko%>";
    const joinTokoAktifSQL<%=rnd%> = "<%=joinTokoAktif%>";
    let pollingInterval<%=rnd%> = null;
    let modalLayaniSemuaInstance<%=rnd%> = null;

    const formatRp<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);
    };

    const setFilterDefaultDates<%=rnd%> = () => {
        const today = new Date();
        today.setDate(today.getDate() + 2);
        const threeMonthsAgo = new Date();
        threeMonthsAgo.setMonth(today.getMonth() - 3);
        document.getElementById('filterEnd<%=rnd%>').value = today.toISOString().split('T')[0];
        document.getElementById('filterStart<%=rnd%>').value = threeMonthsAgo.toISOString().split('T')[0];
    };
    setFilterDefaultDates<%=rnd%>();

    let currentPage<%=rnd%> = 1;
    const limitPerPage<%=rnd%> = 10;
    let totalRecords<%=rnd%> = 0;

    // ==========================================
    // FITUR AUTO SERVE UNTUK SEBELUM JAM 12 MALAM
    // ==========================================
    const autoServeMidnightTransactions<%=rnd%> = async () => {
        const idTokoAktif = <%= toko != null ? toko.getId() : "null" %>;
        // Jika login pedagang, hanya terapkan auto-serve pada tokonya. Jika admin, berlaku ke seluruh toko.
        const filterTokoAuto = isLoginPedagang<%=rnd%> ? " AND toko = " + idTokoAktif : "";
        
        // Ambil transaksi sebelum hari ini (jam 12 malam ke belakang)
        const sqlCount = "SELECT COUNT(DISTINCT COALESCE(pembelian_anggota_koperasi, id)) as jml FROM koperasi.pembelian WHERE (terlayani = false OR terlayani IS NULL) AND DATE(waktu) < CURRENT_DATE" + filterTokoAuto + ";";

        try {
            const resCount = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: "sql", sql: sqlCount, tanpaLoading: true })
            });
            const dataCount = await resCount.json();
            const count = (dataCount.data && dataCount.data[0]) ? parseInt(dataCount.data[0].jml || 0) : 0;

            if (count > 0) {
                // Update menjadi terlayani = true
                const sqlUpdate = "UPDATE koperasi.pembelian SET terlayani = true WHERE (terlayani = false OR terlayani IS NULL) AND DATE(waktu) < CURRENT_DATE" + filterTokoAuto + ";";
                await fetch('<%=Common.ROOT%>/Data', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ action: "update_data", sql: sqlUpdate, tanpaLoading: true })
                });

                // Tampilkan Popup Notifikasi
                document.getElementById('autoServeText<%=rnd%>').innerHTML = '<%=Common.getBahasaConfigJS("Terdapat")%> <b>' + count + '</b> <%=Common.getBahasaConfig("transaksi sebelum jam 12 malam yang telah otomatis ditandai sebagai terlayani.")%>';
                const modalAuto = new bootstrap.Modal(document.getElementById('modalAutoServe<%=rnd%>'));
                modalAuto.show();
            }
        } catch (e) {
            console.error("Gagal menjalankan Auto-Serve transaksi lewat tengah malam:", e);
        }
    };

    // ==========================================
    // INJEKSI MODAL JAVASCRIPT (LAYANI SEMUA)
    // ==========================================
    const renderModalHtml<%=rnd%> = () => {
        const uiHtml = `
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
                            <%=Common.getBahasaConfig("Apakah Anda yakin ingin memproses dan melayani SEMUA pesanan yang belum dilayani berdasarkan filter saat ini?")%>
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
        `;
        document.body.insertAdjacentHTML('beforeend', uiHtml);
    };

    const buildSqlFilters<%=rnd%> = () => {
        const tglMulai = document.getElementById('filterStart<%=rnd%>').value;
        const tglSampai = document.getElementById('filterEnd<%=rnd%>').value;
        const member = document.getElementById('filterBuyerName<%=rnd%>').value.trim().replace(/'/g, "''");
        const tipeJenisMember = document.getElementById('filterTipeJenisMember<%=rnd%>').value.trim().replace(/'/g, "''");
        const barang = document.getElementById('filterItemName<%=rnd%>').value.trim().replace(/'/g, "''");
        const carabayar = document.getElementById('filterPaymentMethod<%=rnd%>').value.trim().replace(/'/g, "''");
        const qtyMulai = document.getElementById('filterQtyMulai<%=rnd%>').value;
        const qtySampai = document.getElementById('filterQtySampai<%=rnd%>').value;
        const hargaMulai = document.getElementById('filterHargaMulai<%=rnd%>').value;
        const hargaSampai = document.getElementById('filterHargaSampai<%=rnd%>').value;

        let filters = " WHERE DATE(a.waktu) BETWEEN DATE('" + tglMulai + "') AND DATE('" + tglSampai + "') " + filterTokoSQL<%=rnd%>;
        
        // Ambil filter pedagang HANYA jika bukan login sebagai pedagang
        if (!isLoginPedagang<%=rnd%>) {
            const elPedagang = document.getElementById('filterMerchantName<%=rnd%>');
            if (elPedagang) {
                const pedagang = elPedagang.value.trim().replace(/'/g, "''");
                if (pedagang) filters += " AND b.nama ILIKE '%" + pedagang + "%' ";
            }
        }
        
        if (member) filters += " AND a.member ILIKE '%" + member + "%' ";
        if (tipeJenisMember) {
            // Cocokkan teks kolom denormalisasi a.jenismember, ATAU telusuri relasi:
            // pembelian -> anggota_koperasi (langsung atau via pembelian_anggota_koperasi)
            // -> tipe_anggota_koperasi / jenis_anggota_koperasi (cocokkan nama-nya).
            // Dipakai EXISTS agar query utama (group by / count / export) tidak perlu join tambahan.
            filters += " AND (a.jenismember ILIKE '%" + tipeJenisMember + "%' OR EXISTS (" +
                       "SELECT 1 FROM koperasi.anggota_koperasi ak " +
                       "LEFT JOIN koperasi.tipe_anggota_koperasi tak ON tak.id = ak.tipe_anggota_koperasi " +
                       "LEFT JOIN koperasi.jenis_anggota_koperasi jak ON jak.id = ak.jenis_anggota_koperasi " +
                       "WHERE ak.id = COALESCE(a.anggota_koperasi, (SELECT pak.anggota_koperasi FROM koperasi.pembelian_anggota_koperasi pak WHERE pak.id = a.pembelian_anggota_koperasi)) " +
                       "AND (tak.nama ILIKE '%" + tipeJenisMember + "%' OR jak.nama ILIKE '%" + tipeJenisMember + "%')" +
                       ")) ";
        }
        if (barang) filters += " AND c.nama ILIKE '%" + barang + "%' ";
        if (carabayar) filters += " AND a.carabayar ILIKE '%" + carabayar + "%' ";
        if (qtyMulai) filters += " AND a.qty >= " + qtyMulai + " ";
        if (qtySampai) filters += " AND a.qty <= " + qtySampai + " ";
        if (hargaMulai) filters += " AND a.total >= " + hargaMulai + " ";
        if (hargaSampai) filters += " AND a.total <= " + hargaSampai + " ";

        return filters;
    };

    const triggerSearch<%=rnd%> = () => {
        currentPage<%=rnd%> = 1;
        loadTransaksiAPI<%=rnd%>();
    };

    const loadTransaksiAPI<%=rnd%> = async () => {
        const tbody = document.getElementById('latestTransactionsTable<%=rnd%>');
        const btnSearch = document.getElementById('btnSearch<%=rnd%>');
        const btnExport = document.getElementById('btnExportExcel<%=rnd%>');
        
        // Menyesuaikan colspan (pedagang 8 kolom, admin 9 kolom, ditambah Aksi bila boleh supervisor).
        const colSpan = (isLoginPedagang<%=rnd%> ? 8 : 9) + (bolehSupervisorRiwayat<%=rnd%> ? 1 : 0);
        
        tbody.innerHTML = '<tr><td colspan="' + colSpan + '" class="text-center py-5 text-muted"><div class="spinner-border text-primary mb-3" role="status"></div><br><span class="fw-medium">' + lang<%=rnd%>.loading + '</span></td></tr>';
        btnSearch.disabled = true;
        btnExport.disabled = true;

        const offset = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%>;
        const sqlFilters = buildSqlFilters<%=rnd%>();

        // Modifikasi query: Group By Transaksi (pembelian_anggota_koperasi) agar tidak duplikat
        const sqlQueryData = "SELECT COALESCE(a.pembelian_anggota_koperasi, a.id) AS id_transaksi, " +
                             "MAX(a.waktu) AS waktu, " +
                             "STRING_AGG(COALESCE(c.nama, '(Produk Dihapus)') || ' (' || a.qty || ')', ', ') AS namabarang, " +
                             "MAX(a.member) AS member, " +
                             "MAX(b.nama) AS pedagang, " +
                             "MAX(a.jenismember) AS jenismember, " +
                             "MAX(a.carabayar) AS carabayar, " +
                             "SUM(a.qty) AS qty, " +
                             "SUM(a.total) AS total, " +
                             "BOOL_AND(COALESCE(a.terlayani, false)) AS terlayani " +
                             "FROM koperasi.pembelian a " +
                             "INNER JOIN koperasi.toko b ON (a.toko = b.id" + joinTokoAktifSQL<%=rnd%> + ") " +
                             "LEFT JOIN koperasi.produk c ON (c.id = a.produk) " +
                             sqlFilters +
                             " GROUP BY COALESCE(a.pembelian_anggota_koperasi, a.id) " +
                             " ORDER BY MAX(a.waktu) DESC, id_transaksi DESC LIMIT " + limitPerPage<%=rnd%> + " OFFSET " + offset + ";";

        // Query count juga distink berdasarkan transaksi
        const sqlQueryCount = "SELECT COUNT(DISTINCT COALESCE(a.pembelian_anggota_koperasi, a.id)) AS jumlah " +
                              "FROM koperasi.pembelian a " +
                              "INNER JOIN koperasi.toko b ON (a.toko = b.id" + joinTokoAktifSQL<%=rnd%> + ") " +
                              "LEFT JOIN koperasi.produk c ON (c.id = a.produk) " +
                              sqlFilters + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetch('<%=Common.ROOT%>/Data', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ sql: sqlQueryCount, action: "sql" })
                }),
                fetch('<%=Common.ROOT%>/Data', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ sql: sqlQueryData, action: "sql" })
                })
            ]);

            const dataCount = await resCount.json();
            const dataResponse = await resData.json();

            totalRecords<%=rnd%> = (dataCount.data && dataCount.data[0]) ? (dataCount.data[0].jumlah || 0) : 0;
            const recordList = dataResponse.data || [];

            let htmlTbody = '';
            if (recordList.length === 0) {
                htmlTbody = '<tr><td colspan="' + colSpan + '" class="text-center text-muted py-5"><i class="fas fa-folder-open fa-3x mb-3 text-secondary opacity-50 d-block"></i><span class="fw-medium">' + lang<%=rnd%>.emptyData + '</span></td></tr>';
            } else {
                recordList.forEach(row => {
                    const carabayar = (row.carabayar || '').toLowerCase();
                    const badgeColor = carabayar.includes('tunai') ? 'bg-secondary' : 'bg-info text-dark';
                    const memberBadgeColor = (row.jenismember || '').toLowerCase() === 'premium' ? 'bg-warning text-dark border-warning' : 'bg-light text-dark border';

                    // Menentukan UI untuk Status Terlayani
                    const isTerlayani = row.terlayani === true || row.terlayani === 't' || row.terlayani === 'true';
                    let btnLayanan = '';

                    if (isLoginPedagang<%=rnd%>) {
                        if (isTerlayani) {
                            btnLayanan = '<span class="badge bg-success bg-opacity-10 text-success border border-success px-2 py-1"><i class="fas fa-check-double me-1"></i>Selesai</span>';
                        } else {
                            btnLayanan = '<button class="btn btn-sm btn-warning text-dark fw-bold rounded-pill shadow-sm" onclick="tandaiTerlayani<%=rnd%>(' + row.id_transaksi + ')" title="<%=Common.getBahasaConfig("Klik jika pesanan ini sudah Anda proses/layani.")%>"><i class="fas fa-bell me-1"></i>Layani</button>';
                        }
                    } else {
                        // Jika Admin melihat, hanya tampilkan badge status
                        btnLayanan = isTerlayani ? '<span class="badge bg-success"><i class="fas fa-check me-1"></i>Selesai</span>' : '<span class="badge bg-secondary"><i class="fas fa-clock me-1"></i>Menunggu</span>';
                    }

                    htmlTbody += '<tr class="' + (!isTerlayani ? 'table-warning' : '') + '">'; // Beri warna kuning muda jika belum dilayani
                    htmlTbody += '<td class="text-center"><span class="text-muted small fw-medium">' + (row.waktu || '-') + '</span></td>';
                    
                    if (!isLoginPedagang<%=rnd%>) {
                        htmlTbody += '<td class="text-start fw-bold text-dark">' + (row.pedagang || '-') + '</td>';
                    }
                    
                    htmlTbody += '<td class="text-start">' + (row.namabarang || '-') + '</td>';
                    htmlTbody += '<td class="text-start fw-medium">' + (row.member || '-') + '</td>';
                    htmlTbody += '<td class="text-center"><span class="badge ' + memberBadgeColor + ' fw-semibold px-2 py-1">' + (row.jenismember || '-') + '</span></td>';
                    htmlTbody += '<td class="text-center"><span class="badge ' + badgeColor + ' rounded-pill px-3 py-1 shadow-sm">' + (row.carabayar || '-') + '</span></td>';
                    htmlTbody += '<td class="text-center fw-bold text-primary">' + (row.qty || 0) + '</td>';
                    htmlTbody += '<td class="text-end fw-bold text-success px-3">' + formatRp<%=rnd%>(row.total) + '</td>';
                    htmlTbody += '<td class="text-center">' + btnLayanan + '</td>';
                    if (bolehSupervisorRiwayat<%=rnd%>) {
                        htmlTbody += '<td class="text-center"><button class="btn btn-sm btn-outline-danger rounded-pill fw-bold shadow-sm" type="button" onclick="batalkanTransaksiRiwayat<%=rnd%>(' + row.id_transaksi + ')" title="<%=Common.getBahasaConfigJS("Batalkan transaksi ini")%>"><i class="fas fa-ban me-1"></i><%=Common.getBahasaConfigJS("Batal")%></button></td>';
                    }
                    htmlTbody += '</tr>';
                });
            }
            tbody.innerHTML = htmlTbody;
            updatePaginationUI<%=rnd%>();
            
            // Re-check jumlah pesanan jika baru memuat data
            cekPesananBelumTerlayani<%=rnd%>();

        } catch (error) {
            console.error("API Error:", error);
            tbody.innerHTML = '<tr><td colspan="' + colSpan + '" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i>' + lang<%=rnd%>.errorFetch + '</td></tr>';
        } finally {
            btnSearch.disabled = false;
            btnExport.disabled = totalRecords<%=rnd%> === 0;
        }
    };

    // ==========================================
    // LOGIKA UPDATE STATUS LAYANAN (SATUAN PER TRANSAKSI)
    // ==========================================
    const tandaiTerlayani<%=rnd%> = async (idTransaksi) => {
        if (typeof konfirmasiModal === 'function') {
            if (!await konfirmasiModal('<%=Common.getBahasaConfigJS("Apakah Anda yakin pesanan ini sudah dilayani/diberikan kepada pembeli?")%>')) return;
        } else if (!confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin pesanan ini sudah dilayani/diberikan kepada pembeli?")%>')) return;

        // Menggunakan update_data agar mengupdate semua barang (items) di transaksi tersebut sekaligus
        const payload = {
            action: "update_data",
            sql: "UPDATE koperasi.pembelian SET terlayani = true WHERE pembelian_anggota_koperasi = " + idTransaksi + " OR id = " + idTransaksi
        };
        
        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const result = await response.json();
            
            if (result.status === '00' || result.status === 'success') {
                // Muat ulang tabel setelah di-update berhasil
                loadTransaksiAPI<%=rnd%>();
            } else {
                alert(result.description || '<%=Common.getBahasaConfigJS("Gagal mengubah status pesanan pada database.")%>');
            }
        } catch (e) {
            console.error("Gagal update status layanan", e);
            alert('<%=Common.getBahasaConfigJS("Gagal mengubah status. Periksa jaringan Anda.")%>');
        }
    };

    // ==========================================

    const mintaAlasanPembatalanRiwayat<%=rnd%> = () => new Promise((resolve) => {
        const modalId = 'modalAlasanPembatalan<%=rnd%>';
        let modalEl = document.getElementById(modalId);
        if (!modalEl) {
            const wrap = document.createElement('div');
            wrap.innerHTML = '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true">'
                + '<div class="modal-dialog modal-dialog-centered">'
                + '<div class="modal-content border-0 shadow rounded-4">'
                + '<div class="modal-header border-0 pb-1"><h5 class="modal-title fw-bold"><i class="fas fa-ban text-danger me-2"></i><%=Common.getBahasaConfigJS("Alasan Pembatalan")%></h5><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div>'
                + '<div class="modal-body pt-2"><label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfigJS("Tuliskan alasan pembatalan transaksi ini")%></label><textarea class="form-control rounded-3" rows="4" id="' + modalId + 'Input" maxlength="500"></textarea><div class="form-text"><%=Common.getBahasaConfigJS("Alasan akan disimpan di arsip pembatalan transaksi.")%></div></div>'
                + '<div class="modal-footer border-0"><button type="button" class="btn btn-light rounded-pill px-4" data-bs-dismiss="modal"><%=Common.getBahasaConfigJS("Batal")%></button><button type="button" class="btn btn-danger rounded-pill px-4" id="' + modalId + 'Ok"><%=Common.getBahasaConfigJS("Lanjutkan")%></button></div>'
                + '</div></div></div>';
            document.body.appendChild(wrap.firstChild);
            modalEl = document.getElementById(modalId);
        }
        const input = document.getElementById(modalId + 'Input');
        const ok = document.getElementById(modalId + 'Ok');
        const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
        let settled = false;
        input.value = '';
        const cleanup = () => { ok.removeEventListener('click', onOk); modalEl.removeEventListener('hidden.bs.modal', onHidden); };
        const onOk = () => { settled = true; const val = input.value; cleanup(); modal.hide(); resolve(val); };
        const onHidden = () => { cleanup(); if (!settled) resolve(null); };
        ok.addEventListener('click', onOk);
        modalEl.addEventListener('hidden.bs.modal', onHidden);
        modal.show();
        setTimeout(() => input.focus(), 250);
    });

    // LOGIKA PEMBATALAN TRANSAKSI (SUPERVISOR / ADMIN)
    // ==========================================
    const batalkanTransaksiRiwayat<%=rnd%> = async (idTransaksi) => {
        if (!bolehSupervisorRiwayat<%=rnd%>) {
            alert('<%=Common.getBahasaConfigJS("Hanya supervisor/admin yang boleh membatalkan transaksi.")%>');
            return;
        }

        const alasan = await mintaAlasanPembatalanRiwayat<%=rnd%>();
        if (alasan === null) return;
        if (!alasan.trim()) {
            alert('<%=Common.getBahasaConfigJS("Alasan pembatalan wajib diisi.")%>');
            return;
        }
        if (typeof konfirmasiModal === 'function') {
            if (!await konfirmasiModal('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin membatalkan transaksi ini? Data transaksi akan dihapus dan dicatat pada arsip pembatalan.")%>')) return;
        } else if (!confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin membatalkan transaksi ini? Data transaksi akan dihapus dan dicatat pada arsip pembatalan.")%>')) return;

        const formData = new URLSearchParams();
        formData.append('aksi', 'batal_transaksi');
        formData.append('id', idTransaksi);
        formData.append('alasan', alasan.trim());

        try {
            const response = await fetch(serviceRiwayatTransaksi<%=rnd%>, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: formData.toString()
            });
            const result = await response.json();
            if (result.status === '00' || result.status === 'success') {
                if (typeof tampilkanPesanSuksesFormal === 'function') {
                    tampilkanPesanSuksesFormal('pembatalan transaksi', result.message || '<%=Common.getBahasaConfigJS("Transaksi berhasil dibatalkan.")%>');
                } else {
                    alert(result.message || '<%=Common.getBahasaConfigJS("Transaksi berhasil dibatalkan.")%>');
                }
                loadTransaksiAPI<%=rnd%>();
            } else {
                const pesan = result.message || result.description || '<%=Common.getBahasaConfigJS("Transaksi gagal dibatalkan.")%>';
                if (typeof tampilkanPesanGagalFormal === 'function') {
                    tampilkanPesanGagalFormal('pembatalan transaksi', pesan, ['<%=Common.getBahasaConfigJS("Pastikan akun memiliki hak supervisor/admin.")%>', '<%=Common.getBahasaConfigJS("Pastikan transaksi masih milik toko yang sedang login.")%>']);
                } else {
                    alert(pesan);
                }
            }
        } catch (e) {
            console.error("Gagal membatalkan transaksi", e);
            alert('<%=Common.getBahasaConfigJS("Gagal membatalkan transaksi. Periksa jaringan Anda.")%>');
        }
    };

    // ==========================================
    // LOGIKA UPDATE STATUS LAYANAN (MASSAL)
    // ==========================================
    const konfirmasiLayaniSemua<%=rnd%> = () => {
        if (!modalLayaniSemuaInstance<%=rnd%>) {
            modalLayaniSemuaInstance<%=rnd%> = new bootstrap.Modal(document.getElementById('modalLayaniSemua<%=rnd%>'));
        }
        document.getElementById('progressBarContainerLayani<%=rnd%>').style.display = 'none';
        document.getElementById('btnEksekusiLayaniSemua<%=rnd%>').style.display = 'inline-block';
        document.getElementById('btnBatalLayaniSemua<%=rnd%>').style.display = 'inline-block';
        document.getElementById('layaniSemuaStatus<%=rnd%>').innerHTML = '<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin memproses dan melayani SEMUA pesanan yang masuk berdasarkan filter saat ini?")%>';
        
        // Sembunyikan popup Toast jika aktif, agar tidak bertabrakan dengan modal
        tutupToastPesanan<%=rnd%>();

        modalLayaniSemuaInstance<%=rnd%>.show();
    };

    const prosesLayaniSemua<%=rnd%> = async () => {
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
        
        const sqlFilters = buildSqlFilters<%=rnd%>();
        
        // Tarik ID transaksi unik yang belum terlayani berdasarkan filter yang ada
        const sqlUnserved = "SELECT DISTINCT COALESCE(a.pembelian_anggota_koperasi, a.id) AS id_transaksi " +
                            "FROM koperasi.pembelian a " +
                            "INNER JOIN koperasi.toko b ON (a.toko = b.id" + joinTokoAktifSQL<%=rnd%> + ") " +
                            "LEFT JOIN koperasi.produk c ON (c.id = a.produk) " +
                            sqlFilters + " AND (a.terlayani IS NULL OR a.terlayani = false) ORDER BY id_transaksi ASC;";

        try {
            statusText.innerHTML = '<%=Common.getBahasaConfigJS("Mengambil data pesanan yang belum dilayani...")%>';
            
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: "sql", sql: sqlUnserved })
            });
            const resUnserved = await response.json();
            const listUnserved = resUnserved.data || [];

            if (listUnserved.length === 0) {
                statusText.innerHTML = '<span class="text-warning fw-bold"><i class="fas fa-exclamation-triangle me-1"></i><%=Common.getBahasaConfig("Tidak ada pesanan yang perlu dilayani pada filter saat ini.")%></span>';
                btnBatal.style.display = 'inline-block';
                btnBatal.innerHTML = '<%=Common.getBahasaConfigJS("Tutup")%>';
                return;
            }

            let successCount = 0;
            let failCount = 0;

            for (let i = 0; i < listUnserved.length; i++) {
                const itemTransaksi = listUnserved[i];
                statusText.innerHTML = `<%=Common.getBahasaConfig("Memproses pesanan")%> ${i+1} / ${listUnserved.length}...`;
                
                const payload = {
                    action: "update_data",
                    sql: "UPDATE koperasi.pembelian SET terlayani = true WHERE pembelian_anggota_koperasi = " + itemTransaksi.id_transaksi + " OR id = " + itemTransaksi.id_transaksi
                };

                const resUpdateRaw = await fetch('<%=Common.ROOT%>/Data', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
                
                const resUpdate = await resUpdateRaw.json();

                if (resUpdate.status === '00' || resUpdate.status === 'success') {
                    successCount++;
                } else {
                    failCount++;
                }

                // Update Progress Bar UI
                const percent = Math.round(((i + 1) / listUnserved.length) * 100);
                progressBar.style.width = percent + '%';
                progressBar.innerHTML = percent + '%';
            }

            statusText.innerHTML = '<span class="text-success fw-bold"><i class="fas fa-check-circle me-1"></i><%=Common.getBahasaConfig("Seluruh pesanan berhasil dilayani!")%></span><br><small class="text-muted"><%=Common.getBahasaConfig("Berhasil:")%> ' + successCount + ', <%=Common.getBahasaConfig("Gagal:")%> ' + failCount + '</small>';
            btnBatal.style.display = 'inline-block';
            btnBatal.innerHTML = '<%=Common.getBahasaConfigJS("Selesai & Tutup")%>';
            
            // Reload tabel riwayat 
            loadTransaksiAPI<%=rnd%>();

        } catch (e) {
            console.error(e);
            statusText.innerHTML = '<span class="text-danger fw-bold"><i class="fas fa-times-circle me-1"></i><%=Common.getBahasaConfig("Terjadi kesalahan jaringan.")%></span>';
            btnBatal.style.display = 'inline-block';
        }
    };


    // ==========================================
    // LOGIKA POLLING & POPUP TOAST (BACKGROUND)
    // ==========================================
    const cekPesananBelumTerlayani<%=rnd%> = async () => {
        if (!isLoginPedagang<%=rnd%>) return; // Polling hanya jalan jika login sebagai Pedagang (Toko)
        
        const idTokoAktif = <%=toko != null ? toko.getId() : "null"%>;
        if (!idTokoAktif) return;

        const sqlFilters = buildSqlFilters<%=rnd%>();
        const sqlCek = "SELECT COUNT(DISTINCT COALESCE(a.pembelian_anggota_koperasi, a.id)) as jml FROM koperasi.pembelian a INNER JOIN koperasi.toko b ON (a.toko = b.id" + joinTokoAktifSQL<%=rnd%> + ") LEFT JOIN koperasi.produk c ON (c.id = a.produk) " + sqlFilters + " and a.toko = " + idTokoAktif + " AND (a.terlayani = false OR a.terlayani IS NULL);";
        
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: "sql", sql: sqlCek, tanpaLoading: true }) // jika sistem punya handle loader global
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
            // Abaikan peringatan gagal tarik data di background agar tidak mengganggu console
        }
    };

    const tutupToastPesanan<%=rnd%> = () => {
        document.getElementById('toastPesananBaru<%=rnd%>').classList.remove('show');
        document.getElementById('toastPesananBaru<%=rnd%>').classList.add('d-none');
    };

    const lihatPesananBaru<%=rnd%> = () => {
        // Reset filter agar data baru terlihat, kemudian scrollTo
        
        const today = new Date();
        today.setDate(today.getDate() + 5);
        const threeMonthsAgo = new Date();
        threeMonthsAgo.setMonth(today.getMonth() - 3);
        document.getElementById('filterEnd<%=rnd%>').value = today.toISOString().split('T')[0];
        document.getElementById('filterStart<%=rnd%>').value = threeMonthsAgo.toISOString().split('T')[0];
        
        triggerSearch<%=rnd%>();
        
        // Scroll ke tabel
        document.getElementById('latestTransactionsTable<%=rnd%>').scrollIntoView({ behavior: 'smooth', block: 'center' });
    };

    // ==========================================
    // EXCEL DOWNLOAD & LAINNYA
    // ==========================================
    const downloadExcelAPI<%=rnd%> = async () => {
        const btnExport = document.getElementById('btnExportExcel<%=rnd%>');
        const originalHtml = btnExport.innerHTML;
        
        btnExport.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>' + lang<%=rnd%>.downloading;
        btnExport.disabled = true;

        const sqlFilters = buildSqlFilters<%=rnd%>();
        
        // Export query juga disamakan group-nya agar data sesuai tabel UI
        const sqlQueryExport = "SELECT MAX(a.waktu) AS waktu, " +
                               "STRING_AGG(COALESCE(c.nama, '(Produk Dihapus)') || ' (' || a.qty || ')', ', ') AS namabarang, " +
                               "MAX(a.member) AS member, MAX(b.nama) AS pedagang, " +
                               "MAX(a.jenismember) AS jenismember, MAX(a.carabayar) AS carabayar, " +
                               "SUM(a.qty) AS qty, SUM(a.total) AS total, " +
                               "BOOL_AND(COALESCE(a.terlayani, false)) AS terlayani " +
                               "FROM koperasi.pembelian a " +
                               "INNER JOIN koperasi.toko b ON (a.toko = b.id" + joinTokoAktifSQL<%=rnd%> + ") " +
                               "LEFT JOIN koperasi.produk c ON (c.id = a.produk) " +
                               sqlFilters +
                               " GROUP BY COALESCE(a.pembelian_anggota_koperasi, a.id) " +
                               " ORDER BY MAX(a.waktu) DESC, COALESCE(a.pembelian_anggota_koperasi, a.id) DESC;";

        try {
            const resData = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sql: sqlQueryExport, action: "sql" })
            });
            const dataResponse = await resData.json();
            const recordList = dataResponse.data || [];

            if (recordList.length === 0) return;

            let tableHtml = '<html xmlns:x="urn:schemas-microsoft-com:office:excel">';
            tableHtml += '<head><meta charset="UTF-8"><style>';
            tableHtml += '.num-format { mso-number-format:"\\#\\,\\#\\#0"; text-align: center; }';
            tableHtml += '.rp-format { mso-number-format:"\\#\\,\\#\\#0"; text-align: right; }';
            tableHtml += 'th { background-color: #f8f9fa; font-weight: bold; }';
            tableHtml += '</style></head><body>';
            
            // Header Judul Excel
            tableHtml += '<h4>Laporan Riwayat Transaksi</h4>';
            <% if (toko != null) { %>
                tableHtml += '<p>Pedagang: <b><%=toko.getNama()%></b></p>';
            <% } %>

            tableHtml += '<table border="1"><thead><tr>';
            tableHtml += '<th>Waktu Transaksi</th>';
            if (!isLoginPedagang<%=rnd%>) {
                tableHtml += '<th>Nama Pedagang</th>';
            }
            tableHtml += '<th>Rincian Barang</th><th>Nama Pembeli</th><th>Tipe Anggota</th>';
            tableHtml += '<th>Metode Pembayaran</th><th>Kuantitas</th><th>Total Harga (Rp)</th><th>Status Dilayani</th>';
            tableHtml += '</tr></thead><tbody>';

            recordList.forEach(row => {
                const isTerlayaniTxt = (row.terlayani === true || row.terlayani === 't' || row.terlayani === 'true') ? 'Selesai' : 'Belum';
                tableHtml += '<tr>';
                tableHtml += '<td style="text-align: center;">' + (row.waktu || '-') + '</td>';
                if (!isLoginPedagang<%=rnd%>) {
                    tableHtml += '<td>' + (row.pedagang || '-') + '</td>';
                }
                tableHtml += '<td>' + (row.namabarang || '-') + '</td>';
                tableHtml += '<td>' + (row.member || '-') + '</td>';
                tableHtml += '<td style="text-align: center;">' + (row.jenismember || '-') + '</td>';
                tableHtml += '<td style="text-align: center;">' + (row.carabayar || '-') + '</td>';
                tableHtml += '<td class="num-format">' + (row.qty || 0) + '</td>';
                tableHtml += '<td class="rp-format">' + (row.total || 0) + '</td>';
                tableHtml += '<td style="text-align: center;">' + isTerlayaniTxt + '</td>';
                tableHtml += '</tr>';
            });
            
            tableHtml += '</tbody></table></body></html>';

            const blob = new Blob([tableHtml], { type: 'application/vnd.ms-excel' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement("a");
            
            const timestamp = new Date().toISOString().slice(0, 10).replace(/-/g, "");
            const fileName = isLoginPedagang<%=rnd%> ? "Trans_Toko_<%=toko != null ? toko.getId() : ""%>_" : "Trans_Semua_";
            
            link.download = fileName + timestamp + ".xls";
            link.href = url;
            
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            setTimeout(() => URL.revokeObjectURL(url), 100);

        } catch (error) {
            console.error("Export Error:", error);
            alert(lang<%=rnd%>.errorFetch);
        } finally {
            btnExport.innerHTML = originalHtml;
            btnExport.disabled = false;
        }
    };

    const updatePaginationUI<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumber<%=rnd%>').innerText = currentPage<%=rnd%> + " / " + totalPages;
        
        const startIdx = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%> + 1;
        const endIdx = Math.min(currentPage<%=rnd%> * limitPerPage<%=rnd%>, totalRecords<%=rnd%>);
        const startInfo = totalRecords<%=rnd%> > 0 ? startIdx : 0;
        
        document.getElementById('pagingInfo<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i>' + lang<%=rnd%>.showing + ' <b class="text-dark">' + startInfo + ' - ' + endIdx + '</b> ' + lang<%=rnd%>.from + ' <b class="text-dark">' + totalRecords<%=rnd%> + '</b> ' + lang<%=rnd%>.data;
        
        document.getElementById('btnPrev<%=rnd%>').disabled = (currentPage<%=rnd%> === 1);
        document.getElementById('btnNext<%=rnd%>').disabled = (currentPage<%=rnd%> === totalPages);
    };

    const changePage<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if (direction === -1 && currentPage<%=rnd%> > 1) {
            currentPage<%=rnd%>--;
            loadTransaksiAPI<%=rnd%>();
        } else if (direction === 1 && currentPage<%=rnd%> < totalPages) {
            currentPage<%=rnd%>++;
            loadTransaksiAPI<%=rnd%>();
        }
    };

    document.addEventListener("DOMContentLoaded", async () => {
        renderModalHtml<%=rnd%>(); // Inject modal Layani Semua

        // --- EKSEKUSI AUTO SERVE SEBELUM DATA DILAPorkan KE TABEL ---
        await autoServeMidnightTransactions<%=rnd%>();
        
        const filterInputs = document.querySelectorAll('.tx-filter<%=rnd%>');
        filterInputs.forEach(input => {
            input.addEventListener("keydown", (event) => {
                if (event.key === "Enter") {
                    event.preventDefault(); 
                    triggerSearch<%=rnd%>();
                }
            });
        });
        loadTransaksiAPI<%=rnd%>();

        // Inisialisasi Polling hanya jika dia adalah pedagang
        if (isLoginPedagang<%=rnd%>) {
            pollingInterval<%=rnd%> = setInterval(cekPesananBelumTerlayani<%=rnd%>, 10000); // Polling tiap 10 detik
            // Panggil sekali di awal untuk langsung mengecek
            setTimeout(cekPesananBelumTerlayani<%=rnd%>, 2000);
        }
    });
</script>
