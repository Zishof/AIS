<%@page import="ais.database.model.koperasi.PembelianAnggotaKoperasi"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%--
  Tab "Sinkronisasi POS" -- laporan server-side yang membedakan transaksi yang tersimpan lewat
  jalur ONLINE biasa (KantinHelper.bayar()) dari yang awalnya tertahan offline di perangkat kasir
  (IndexedDB) lalu baru sampai ke server belakangan lewat pos_offline_service.jsp (aksi=sync).

  PENTING soal cakupan: transaksi yang MASIH tertahan di suatu perangkat (belum pernah berhasil
  sinkron sama sekali) TIDAK bisa muncul di sini -- server memang belum pernah menerimanya. Untuk
  melihat itu, buka panel "Status Sinkronisasi" di tab POS Transaksi pada PERANGKAT KASIR yang
  bersangkutan (klik badge Online/Offline di pojok kanan atas). Laporan di sini hanya menjawab
  "dari transaksi yang SUDAH sampai ke server, mana yang lewat jalur offline dan berapa lama
  jedanya" -- berguna untuk audit lintas-kasir/lintas-perangkat yang tidak bisa dilihat dari satu
  browser saja.
--%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	out.print("{\"status\":\"error\", \"message\":\""
	+ Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
	return;
}
Pedagang pedagang = tbmuser.getPedagang();
Toko toko = pedagang == null ? null : pedagang.getToko();
String rnd = request.getParameter("rnd");
if (rnd == null) {
	rnd = Common.getGeneratedBarCode(7);
}
boolean isAdminSk = (toko == null);
String sqlFilterTokoSk = "";
if (toko != null) {
	sqlFilterTokoSk = " AND a.toko = " + toko.getId() + " ";
}
%>

<div class="row g-4 mb-4 animate__animated animate__fadeInUp">
    <div class="col-lg-12">
        <div class="card border-0 shadow-sm rounded-4 border-top border-warning border-4">
            <div class="card-body p-4">

                <div class="d-flex flex-column flex-md-row justify-content-between align-items-center mb-3 pb-3 border-bottom border-light gap-3">
                    <div class="text-center text-md-start">
                        <h5 class="fw-bold text-dark mb-1">
                            <i class="fas fa-rotate me-2 text-warning"></i><%=Common.getBahasaConfig("Sinkronisasi POS")%>
                        </h5>
                        <small class="text-muted"><%=Common.getBahasaConfig("Pantau transaksi yang masih tersimpan offline (belum terkirim) maupun yang sudah berhasil sampai ke server, sekaligus bandingkan mana yang lewat jalur online biasa dan mana yang sempat tertunda.")%></small>
                    </div>
                    <div class="d-flex flex-wrap gap-2 justify-content-center">
                        <button class="btn btn-warning text-dark rounded-pill px-4 fw-semibold shadow-sm text-nowrap"
                            type="button" data-bs-toggle="collapse"
                            data-bs-target="#collapseFilterSk<%=rnd%>" aria-expanded="false"
                            aria-controls="collapseFilterSk<%=rnd%>">
                            <i class="fas fa-filter me-2"></i><%=Common.getBahasaConfig("Saring Data")%>
                        </button>
                        <button class="btn btn-outline-secondary rounded-pill px-4 fw-semibold shadow-sm text-nowrap" type="button" id="btnRefreshSk<%=rnd%>">
                            <i class="fas fa-arrows-rotate me-2"></i><%=Common.getBahasaConfig("Segarkan")%>
                        </button>
                    </div>
                </div>

                <p class="text-muted small mb-3">
                    <i class="fas fa-circle-info me-1"></i>
                    <%=Common.getBahasaConfig("Panel merah di bawah menampilkan transaksi di komputer INI yang belum terkirim (komputer kasir lain punya daftarnya sendiri-sendiri); tabel di bawahnya menampilkan transaksi yang sudah berhasil terkirim ke server.")%>
                </p>

                <div class="card border-0 shadow-sm rounded-4 mb-4" style="border-left: 5px solid #dc3545;">
                    <div class="card-body p-3 p-md-4">
                        <div class="d-flex flex-wrap justify-content-between align-items-center mb-2 gap-2">
                            <h6 class="fw-bold text-dark mb-0">
                                <i class="fas fa-mobile-screen-button me-2 text-danger"></i><%=Common.getBahasaConfig("Transaksi Belum Sinkron (Perangkat Ini)")%>
                            </h6>
                            <div class="d-flex align-items-center gap-2">
                                <span class="badge bg-light text-dark border" id="skKoneksiPerangkat<%=rnd%>">-</span>
                                <button type="button" class="btn btn-danger btn-sm rounded-pill px-3" id="btnSyncSekarangSk<%=rnd%>">
                                    <i class="fas fa-rotate me-1"></i><%=Common.getBahasaConfig("Sinkronkan Sekarang")%>
                                </button>
                            </div>
                        </div>
                        <p class="text-muted small mb-2">
                            <%=Common.getBahasaConfig("Terakhir perangkat ini berhasil bicara ke server:")%> <b id="skTerakhirSinkronSk<%=rnd%>">-</b>
                        </p>
                        <div class="table-responsive" style="max-height:260px;overflow-y:auto;">
                            <table class="table table-sm align-middle mb-0">
                                <thead><tr>
                                    <th><%=Common.getBahasaConfig("Waktu")%></th>
                                    <th><%=Common.getBahasaConfig("Kode")%></th>
                                    <th class="text-end"><%=Common.getBahasaConfig("Total")%></th>
                                    <th><%=Common.getBahasaConfig("Status")%></th>
                                </tr></thead>
                                <tbody id="tabelPendingSk<%=rnd%>"><tr><td colspan="4" class="text-center text-muted py-3"><%=Common.getBahasaConfig("Memuat...")%></td></tr></tbody>
                            </table>
                        </div>
                    </div>
                </div>

                <div class="row g-3 mb-4">
                    <div class="col-6 col-md-3">
                        <div class="border rounded-3 p-3 text-center h-100 bg-light">
                            <div class="text-muted small mb-1"><%=Common.getBahasaConfig("Total Transaksi")%></div>
                            <div class="fw-bold fs-5 text-dark" id="skTotalTrx<%=rnd%>">-</div>
                        </div>
                    </div>
                    <div class="col-6 col-md-3">
                        <div class="border rounded-3 p-3 text-center h-100 bg-light">
                            <div class="text-muted small mb-1"><%=Common.getBahasaConfig("Via Offline → Sinkron")%></div>
                            <div class="fw-bold fs-5 text-warning" id="skTotalOffline<%=rnd%>">-</div>
                        </div>
                    </div>
                    <div class="col-12 col-md-6">
                        <div class="border rounded-3 p-3 text-center h-100 bg-light">
                            <div class="text-muted small mb-1"><%=Common.getBahasaConfig("Rata-rata Keterlambatan Sinkron")%></div>
                            <div class="fw-bold fs-5 text-dark" id="skRataKeterlambatan<%=rnd%>">-</div>
                        </div>
                    </div>
                </div>

                <div class="collapse mb-4" id="collapseFilterSk<%=rnd%>">
                    <div class="card card-body bg-light border-0 rounded-4 p-4 shadow-sm">
                        <div class="row g-3">
                            <div class="col-md-3">
                                <label class="form-label small fw-semibold text-secondary mb-1"><i class="far fa-calendar-alt me-1"></i><%=Common.getBahasaConfig("Tanggal Mulai")%></label>
                                <input type="date" class="form-control form-control-sm rounded-3 filter-sk-<%=rnd%> border-0 shadow-sm" id="filterStartSk<%=rnd%>">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-semibold text-secondary mb-1"><i class="far fa-calendar-check me-1"></i><%=Common.getBahasaConfig("Tanggal Akhir")%></label>
                                <input type="date" class="form-control form-control-sm rounded-3 filter-sk-<%=rnd%> border-0 shadow-sm" id="filterEndSk<%=rnd%>">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-semibold text-secondary mb-1"><i class="fas fa-code-branch me-1"></i><%=Common.getBahasaConfig("Sumber")%></label>
                                <select class="form-select form-select-sm rounded-3 filter-sk-<%=rnd%> border-0 shadow-sm" id="filterSumberSk<%=rnd%>">
                                    <option value=""><%=Common.getBahasaConfig("Semua")%></option>
                                    <option value="OFFLINE_SYNC"><%=Common.getBahasaConfig("Hanya Offline → Sinkron")%></option>
                                    <option value="ONLINE"><%=Common.getBahasaConfig("Hanya Online")%></option>
                                </select>
                            </div>
                            <% if (isAdminSk) { %>
                            <div class="col-md-3">
                                <label class="form-label small fw-semibold text-secondary mb-1"><i class="fas fa-store me-1"></i><%=Common.getBahasaConfig("Nama Toko")%></label>
                                <input type="text" class="form-control form-control-sm rounded-3 filter-sk-<%=rnd%> border-0 shadow-sm" id="filterTokoSk<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik nama toko")%>">
                            </div>
                            <% } %>
                            <div class="col-md-12 d-flex align-items-end gap-2 mt-3 justify-content-end">
                                <button class="btn btn-warning text-dark btn-sm rounded-pill px-4 shadow-sm fw-bold" type="button" id="btnSearchSk<%=rnd%>">
                                    <i class="fas fa-search me-2"></i><%=Common.getBahasaConfig("Terapkan Filter")%>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="table-responsive border rounded-3 shadow-sm bg-white">
                    <table class="table table-hover table-striped align-middle mb-0">
                        <thead class="table-dark text-center align-middle">
                            <tr>
                                <th class="fw-semibold text-uppercase small py-3" style="width: 50px;">#</th>
                                <th class="fw-semibold text-uppercase small"><i class="far fa-clock me-1"></i><%=Common.getBahasaConfig("Waktu Transaksi")%></th>
                                <% if (isAdminSk) { %>
                                    <th class="fw-semibold text-uppercase small text-start"><i class="fas fa-store me-1"></i><%=Common.getBahasaConfig("Toko")%></th>
                                <% } %>
                                <th class="fw-semibold text-uppercase small"><i class="fas fa-hashtag me-1"></i><%=Common.getBahasaConfig("Kode")%></th>
                                <th class="fw-semibold text-uppercase small"><i class="fas fa-code-branch me-1"></i><%=Common.getBahasaConfig("Sumber")%></th>
                                <th class="fw-semibold text-uppercase small"><i class="fas fa-cloud-arrow-up me-1"></i><%=Common.getBahasaConfig("Waktu Sinkron")%></th>
                                <th class="fw-semibold text-uppercase small"><i class="fas fa-hourglass-half me-1"></i><%=Common.getBahasaConfig("Keterlambatan")%></th>
                                <th class="fw-semibold text-uppercase small text-start"><i class="fas fa-user me-1"></i><%=Common.getBahasaConfig("Kasir")%></th>
                                <th class="fw-semibold text-uppercase small text-end px-3"><i class="fas fa-coins me-1"></i><%=Common.getBahasaConfig("Total")%></th>
                            </tr>
                        </thead>
                        <tbody id="tableSinkron<%=rnd%>">
                        </tbody>
                    </table>
                </div>

                <div class="d-flex justify-content-between align-items-center mt-4 border-top pt-3">
                    <span class="text-muted small fw-medium bg-light px-3 py-1 rounded-pill border" id="pagingInfoSk<%=rnd%>"></span>
                    <nav>
                        <ul class="pagination pagination-sm mb-0 shadow-sm">
                            <li class="page-item">
                                <button class="page-link text-primary rounded-start-pill px-3" id="btnPrevSk<%=rnd%>">
                                    <i class="fas fa-chevron-left"></i>
                                </button>
                            </li>
                            <li class="page-item disabled">
                                <span class="page-link fw-bold bg-light text-dark px-3" id="pageNumberSk<%=rnd%>">1</span>
                            </li>
                            <li class="page-item">
                                <button class="page-link text-primary rounded-end-pill px-3" id="btnNextSk<%=rnd%>">
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

<script>
    var isLoginPedagangSk<%=rnd%> = <%= toko != null %>;
    var filterTokoSQLSinkron<%=rnd%> = "<%=sqlFilterTokoSk%>";

    var currentPageSk<%=rnd%> = 1;
    var limitPerPageSk<%=rnd%> = 15;
    var totalRecordsSk<%=rnd%> = 0;

    var formatRpSk<%=rnd%> = (angka) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);

    /** Format jeda detik jadi teks awam ("2 jam 14 menit", "45 detik", dst). */
    var formatKeterlambatanSk<%=rnd%> = (detik) => {
        if (detik == null) return '-';
        detik = Math.max(0, Math.round(detik));
        if (detik < 60) return detik + ' <%=Common.getBahasaConfig("detik")%>';
        var menit = Math.floor(detik / 60);
        if (menit < 60) return menit + ' <%=Common.getBahasaConfig("menit")%>';
        var jam = Math.floor(menit / 60);
        var sisaMenit = menit % 60;
        return jam + ' <%=Common.getBahasaConfig("jam")%>' + (sisaMenit > 0 ? (' ' + sisaMenit + ' <%=Common.getBahasaConfig("menit")%>') : '');
    };

    var setDefaultDatesSinkron<%=rnd%> = () => {
        const today = new Date();
        today.setDate(today.getDate() + 2);
        const threeMonthsAgo = new Date();
        threeMonthsAgo.setMonth(today.getMonth() - 3);
        document.getElementById('filterEndSk<%=rnd%>').value = today.toISOString().split('T')[0];
        document.getElementById('filterStartSk<%=rnd%>').value = threeMonthsAgo.toISOString().split('T')[0];
    };

    var buildSqlFiltersSinkron<%=rnd%> = () => {
        const tglMulai = document.getElementById('filterStartSk<%=rnd%>').value;
        const tglSampai = document.getElementById('filterEndSk<%=rnd%>').value;
        const sumber = document.getElementById('filterSumberSk<%=rnd%>').value;

        let filters = " WHERE DATE(a.tanggal_pembayaran) BETWEEN DATE('" + tglMulai + "') AND DATE('" + tglSampai + "') " + filterTokoSQLSinkron<%=rnd%>;

        if (sumber === 'OFFLINE_SYNC') filters += " AND a.sumber_transaksi = 'OFFLINE_SYNC' ";
        else if (sumber === 'ONLINE') filters += " AND (a.sumber_transaksi IS NULL OR a.sumber_transaksi = 'ONLINE') ";

        if (!isLoginPedagangSk<%=rnd%>) {
            const elToko = document.getElementById('filterTokoSk<%=rnd%>');
            if (elToko) {
                const tokoNama = elToko.value.trim().replace(/'/g, "''");
                if (tokoNama) filters += " AND b.nama ILIKE '%" + tokoNama + "%' ";
            }
        }
        return filters;
    };

    var triggerSearchSinkron<%=rnd%> = () => {
        currentPageSk<%=rnd%> = 1;
        loadRiwayatSinkronAPI<%=rnd%>();
    };

    var loadRiwayatSinkronAPI<%=rnd%> = async () => {
        const tbody = document.getElementById('tableSinkron<%=rnd%>');
        const btnSearch = document.getElementById('btnSearchSk<%=rnd%>');
        const colSpan = isLoginPedagangSk<%=rnd%> ? 7 : 8;

        tbody.innerHTML = '<tr><td colspan="' + colSpan + '" class="text-center py-5 text-muted"><div class="spinner-border text-warning mb-3" role="status"></div><br><span class="fw-medium"><%=Common.getBahasaConfig("Memuat data...")%></span></td></tr>';
        btnSearch.disabled = true;

        const offset = (currentPageSk<%=rnd%> - 1) * limitPerPageSk<%=rnd%>;
        const sqlFilters = buildSqlFiltersSinkron<%=rnd%>();

        const kolomToko = isLoginPedagangSk<%=rnd%> ? "" : " b.nama AS nama_toko, ";
        const sqlData = "SELECT a.id, a.kode, a.oleh, a.total_biaya, " +
                        "TO_CHAR(a.tanggal_pembayaran, 'DD-MM-YYYY HH24:MI') AS waktu_trx, " +
                        "COALESCE(a.sumber_transaksi, 'ONLINE') AS sumber, " +
                        "TO_CHAR(a.waktu_sinkron, 'DD-MM-YYYY HH24:MI') AS waktu_sinkron_fmt, " +
                        "EXTRACT(EPOCH FROM (a.waktu_sinkron - a.tanggal_pembayaran)) AS keterlambatan_detik, " +
                        kolomToko +
                        "'x' AS placeholder " +
                        "FROM koperasi.pembelian_anggota_koperasi a " +
                        "INNER JOIN koperasi.toko b ON (b.id = a.toko) " +
                        sqlFilters +
                        " ORDER BY a.tanggal_pembayaran DESC, a.id DESC LIMIT " + limitPerPageSk<%=rnd%> + " OFFSET " + offset + ";";

        const sqlCount = "SELECT COUNT(*) AS jumlah FROM koperasi.pembelian_anggota_koperasi a " +
                          "INNER JOIN koperasi.toko b ON (b.id = a.toko) " + sqlFilters + ";";

        const sqlRingkasan = "SELECT COUNT(*) AS total, " +
                              "COUNT(*) FILTER (WHERE COALESCE(a.sumber_transaksi,'ONLINE') = 'OFFLINE_SYNC') AS total_offline, " +
                              "AVG(EXTRACT(EPOCH FROM (a.waktu_sinkron - a.tanggal_pembayaran))) FILTER (WHERE COALESCE(a.sumber_transaksi,'ONLINE') = 'OFFLINE_SYNC') AS rata_keterlambatan " +
                              "FROM koperasi.pembelian_anggota_koperasi a " +
                              "INNER JOIN koperasi.toko b ON (b.id = a.toko) " + sqlFilters + ";";

        try {
            const [resCount, resData, resRingkasan] = await Promise.all([
                fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ sql: sqlCount, action: "sql" }) }),
                fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ sql: sqlData, action: "sql" }) }),
                fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ sql: sqlRingkasan, action: "sql" }) })
            ]);

            const dataCount = await resCount.json();
            const dataResponse = await resData.json();
            const dataRingkasan = await resRingkasan.json();

            totalRecordsSk<%=rnd%> = (dataCount.data && dataCount.data[0]) ? parseInt(dataCount.data[0].jumlah || 0) : 0;
            const recordList = dataResponse.data || [];
            const ringkasan = (dataRingkasan.data && dataRingkasan.data[0]) ? dataRingkasan.data[0] : {};

            document.getElementById('skTotalTrx<%=rnd%>').textContent = ringkasan.total || 0;
            document.getElementById('skTotalOffline<%=rnd%>').textContent = ringkasan.total_offline || 0;
            document.getElementById('skRataKeterlambatan<%=rnd%>').textContent = ringkasan.rata_keterlambatan != null
                ? formatKeterlambatanSk<%=rnd%>(parseFloat(ringkasan.rata_keterlambatan)) : '-';

            let htmlTbody = '';
            if (recordList.length === 0) {
                htmlTbody = '<tr><td colspan="' + colSpan + '" class="text-center text-muted py-5"><i class="fas fa-folder-open fa-3x mb-3 text-secondary opacity-50 d-block"></i><span class="fw-medium"><%=Common.getBahasaConfig("Tidak ada data transaksi ditemukan.")%></span></td></tr>';
            } else {
                let no = offset + 1;
                recordList.forEach(row => {
                    const isOffline = row.sumber === 'OFFLINE_SYNC';
                    const badgeSumber = isOffline
                        ? '<span class="badge bg-warning text-dark rounded-pill px-3 py-1 shadow-sm"><i class="fas fa-cloud-arrow-up me-1"></i><%=Common.getBahasaConfig("Offline → Sinkron")%></span>'
                        : '<span class="badge bg-success rounded-pill px-3 py-1 shadow-sm"><i class="fas fa-bolt me-1"></i><%=Common.getBahasaConfig("Online")%></span>';
                    const keterlambatan = isOffline ? formatKeterlambatanSk<%=rnd%>(row.keterlambatan_detik) : '-';
                    const waktuSinkron = isOffline ? (row.waktu_sinkron_fmt || '-') : '-';

                    htmlTbody += '<tr>';
                    htmlTbody += '<td class="text-center text-muted fw-medium">' + (no++) + '</td>';
                    htmlTbody += '<td class="text-center"><span class="text-muted small fw-medium">' + (row.waktu_trx || '-') + '</span></td>';
                    if (!isLoginPedagangSk<%=rnd%>) {
                        htmlTbody += '<td class="text-start fw-bold text-dark">' + (row.nama_toko || '-') + '</td>';
                    }
                    htmlTbody += '<td class="text-center text-monospace small">' + (row.kode || '-') + '</td>';
                    htmlTbody += '<td class="text-center">' + badgeSumber + '</td>';
                    htmlTbody += '<td class="text-center small">' + waktuSinkron + '</td>';
                    htmlTbody += '<td class="text-center small fw-medium ' + (isOffline ? 'text-warning' : 'text-muted') + '">' + keterlambatan + '</td>';
                    htmlTbody += '<td class="text-start fw-medium">' + (row.oleh || '-') + '</td>';
                    htmlTbody += '<td class="text-end fw-bold text-success px-3">' + formatRpSk<%=rnd%>(row.total_biaya) + '</td>';
                    htmlTbody += '</tr>';
                });
            }
            tbody.innerHTML = htmlTbody;
            updatePaginationUISinkron<%=rnd%>();

        } catch (error) {
            console.error("API Error:", error);
            tbody.innerHTML = '<tr><td colspan="' + colSpan + '" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal mengambil data riwayat sinkronisasi.")%></td></tr>';
        } finally {
            btnSearch.disabled = false;
        }
    };

    var updatePaginationUISinkron<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecordsSk<%=rnd%> / limitPerPageSk<%=rnd%>) || 1;
        document.getElementById('pageNumberSk<%=rnd%>').innerText = currentPageSk<%=rnd%> + " / " + totalPages;

        const startIdx = totalRecordsSk<%=rnd%> > 0 ? ((currentPageSk<%=rnd%> - 1) * limitPerPageSk<%=rnd%> + 1) : 0;
        const endIdx = Math.min(currentPageSk<%=rnd%> * limitPerPageSk<%=rnd%>, totalRecordsSk<%=rnd%>);

        document.getElementById('pagingInfoSk<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-warning me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startIdx + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari total")%> <b class="text-dark">' + totalRecordsSk<%=rnd%> + '</b> <%=Common.getBahasaConfig("transaksi")%>';

        document.getElementById('btnPrevSk<%=rnd%>').disabled = (currentPageSk<%=rnd%> === 1);
        document.getElementById('btnNextSk<%=rnd%>').disabled = (currentPageSk<%=rnd%> === totalPages);
    };

    var changePageSk<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecordsSk<%=rnd%> / limitPerPageSk<%=rnd%>);
        if (direction === -1 && currentPageSk<%=rnd%> > 1) { currentPageSk<%=rnd%>--; loadRiwayatSinkronAPI<%=rnd%>(); }
        else if (direction === 1 && currentPageSk<%=rnd%> < totalPages) { currentPageSk<%=rnd%>++; loadRiwayatSinkronAPI<%=rnd%>(); }
    };

    // === Panel "Transaksi Belum Sinkron (Perangkat Ini)" -- separuh lain dari halaman monitor ini.
    // Berbeda dari tabel riwayat di atas (dibaca dari SERVER lewat SQL), panel ini dibaca LANGSUNG
    // dari IndexedDB browser via AisPosOffline (mesin yang sama dipakai _pos.jsp) -- karena transaksi
    // yang benar-benar belum sinkron memang belum pernah sampai ke server, jadi TIDAK ADA query SQL
    // yang bisa menemukannya. window.AisPosOffline sudah pasti tersedia di sini (di-load & di-init
    // oleh <script> tab "POS Transaksi" yang sama-sama ada di halaman ini, dieksekusi lebih dulu
    // karena urutan include di pos.jsp). ===
    var formatWaktuSinkronTerakhirSk<%=rnd%> = (epochMillis) => {
        if (!epochMillis) return '<%=Common.getBahasaConfig("Belum pernah")%>';
        try { return new Date(epochMillis).toLocaleString('id-ID', { dateStyle: 'medium', timeStyle: 'medium' }); }
        catch (e) { return new Date(epochMillis).toLocaleString(); }
    };
    var muatPendingLokalSk<%=rnd%> = async () => {
        if (!window.AisPosOffline) {
            document.getElementById('skKoneksiPerangkat<%=rnd%>').textContent = '<%=Common.getBahasaConfig("Mesin offline tidak aktif di halaman ini")%>';
            return;
        }
        const tbody = document.getElementById('tabelPendingSk<%=rnd%>');
        const badge = document.getElementById('skKoneksiPerangkat<%=rnd%>');
        badge.innerHTML = AisPosOffline.isOnline()
            ? '<i class="fas fa-wifi me-1 text-success"></i><%=Common.getBahasaConfig("Online")%>'
            : '<i class="fas fa-wifi-slash me-1 text-danger"></i><%=Common.getBahasaConfig("Offline")%>';
        try {
            const [daftar, terakhir] = await Promise.all([AisPosOffline.listPending(), AisPosOffline.getLastSyncAt()]);
            document.getElementById('skTerakhirSinkronSk<%=rnd%>').textContent = formatWaktuSinkronTerakhirSk<%=rnd%>(terakhir);
            if (!daftar.length) {
                tbody.innerHTML = '<tr><td colspan="4" class="text-center text-success py-3"><i class="fas fa-check-circle me-1"></i><%=Common.getBahasaConfig("Semua transaksi di perangkat ini sudah terkirim ke server.")%></td></tr>';
                return;
            }
            tbody.innerHTML = daftar.map(t => {
                const waktu = t.waktu ? new Date(t.waktu).toLocaleString('id-ID', { dateStyle: 'short', timeStyle: 'short' }) : '-';
                const status = AisPosOffline.isOnline()
                    ? '<span class="badge bg-warning text-dark"><%=Common.getBahasaConfig("Sedang dicoba")%></span>'
                    : '<span class="badge bg-secondary"><%=Common.getBahasaConfig("Menunggu koneksi")%></span>';
                return '<tr><td>' + waktu + '</td><td class="text-monospace small">' + (t.clientTrxId || '-') + '</td>'
                    + '<td class="text-end">' + formatRpSk<%=rnd%>(t.total || 0) + '</td><td>' + status + '</td></tr>';
            }).join('');
        } catch (e) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center text-danger py-3"><%=Common.getBahasaConfig("Gagal membaca data lokal.")%></td></tr>';
        }
    };

    document.addEventListener("DOMContentLoaded", () => {
        setDefaultDatesSinkron<%=rnd%>();
        document.getElementById('btnSearchSk<%=rnd%>').addEventListener('click', triggerSearchSinkron<%=rnd%>);
        document.getElementById('btnRefreshSk<%=rnd%>').addEventListener('click', () => { loadRiwayatSinkronAPI<%=rnd%>(); muatPendingLokalSk<%=rnd%>(); });
        document.getElementById('btnPrevSk<%=rnd%>').addEventListener('click', () => changePageSk<%=rnd%>(-1));
        document.getElementById('btnNextSk<%=rnd%>').addEventListener('click', () => changePageSk<%=rnd%>(1));
        const filterInputsSk = document.querySelectorAll('.filter-sk-<%=rnd%>');
        filterInputsSk.forEach(input => {
            input.addEventListener("keydown", (event) => {
                if (event.key === "Enter") { event.preventDefault(); triggerSearchSinkron<%=rnd%>(); }
            });
        });

        const btnSyncSekarangSk<%=rnd%> = document.getElementById('btnSyncSekarangSk<%=rnd%>');
        if (btnSyncSekarangSk<%=rnd%>) {
            btnSyncSekarangSk<%=rnd%>.addEventListener('click', async () => {
                if (!window.AisPosOffline) return;
                if (!AisPosOffline.isOnline()) {
                    if (typeof tampilkanToast === "function") tampilkanToast('<%=Common.getBahasaConfigJS("Tidak ada koneksi internet -- transaksi akan terkirim otomatis begitu koneksi kembali.")%>', 'bg-warning text-dark');
                    return;
                }
                const oriHtml = btnSyncSekarangSk<%=rnd%>.innerHTML;
                btnSyncSekarangSk<%=rnd%>.disabled = true;
                btnSyncSekarangSk<%=rnd%>.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyinkronkan...")%>';
                try {
                    const jumlah = await AisPosOffline.syncNow();
                    if (typeof tampilkanToast === "function") {
                        tampilkanToast(jumlah > 0
                            ? ('<%=Common.getBahasaConfigJS("Berhasil menyinkronkan")%> ' + jumlah + ' <%=Common.getBahasaConfigJS("transaksi.")%>')
                            : '<%=Common.getBahasaConfigJS("Tidak ada transaksi tertunda yang perlu disinkronkan.")%>', 'bg-success text-white');
                    }
                } catch (e) {
                    if (typeof tampilkanToast === "function") tampilkanToast('<%=Common.getBahasaConfigJS("Gagal menyinkronkan, coba lagi.")%>', 'bg-danger text-white');
                } finally {
                    btnSyncSekarangSk<%=rnd%>.disabled = false;
                    btnSyncSekarangSk<%=rnd%>.innerHTML = oriHtml;
                    muatPendingLokalSk<%=rnd%>();
                    loadRiwayatSinkronAPI<%=rnd%>();
                }
            });
        }

        // Tab ini baru terlihat saat kasir/admin mengklik "Sinkronisasi POS" -- muat datanya begitu
        // tab tersebut benar-benar ditampilkan (bukan langsung saat halaman dibuka), supaya tidak
        // membebani query di setiap page-load POS padahal tab-nya belum tentu dibuka.
        const tabTrigger = document.getElementById('tab-sinkron-<%=rnd%>');
        if (tabTrigger) {
            let sudahDimuat = false;
            tabTrigger.addEventListener('shown.bs.tab', () => {
                if (!sudahDimuat) { sudahDimuat = true; loadRiwayatSinkronAPI<%=rnd%>(); }
                muatPendingLokalSk<%=rnd%>(); // selalu segarkan panel lokal tiap tab dibuka (murah, dari IndexedDB)
            });
        } else {
            // Tidak ada pembungkus tab (fragment dipakai berdiri sendiri) -- muat langsung.
            loadRiwayatSinkronAPI<%=rnd%>();
            muatPendingLokalSk<%=rnd%>();
        }
    });
</script>
