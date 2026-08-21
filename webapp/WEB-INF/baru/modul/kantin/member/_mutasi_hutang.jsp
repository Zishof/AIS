<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// Layar Mutasi Hutang (Piutang Toko ke Anggota) -- pola query PERSIS mengikuti _mutasi_tabungan.jsp
// (raw SQL client-side via /Data, saldo berjalan pakai window function), bedanya sumber datanya:
// DEBIT ("Hutang Bertambah") dihitung dari HEADER koperasi.pembelian_anggota_koperasi (5 slot split
// pembayaran, LIHAT JavaDoc PembelianAnggotaKoperasi.getNominalBayar1() & DepositHelper.hitungDeposit)
// yang slot cara-bayarnya ditandai CaraPembayaranKoperasi.masukSebagaiHutang = true -- SETIAP slot
// dicek independen (pola sama dgn memotong_deposit: per-SLOT, bukan per-transaksi keseluruhan).
// KREDIT ("Pembayaran/Pelunasan") murni entri manual di koperasi.pembayaran_hutang (form di bawah),
// polanya disamakan dgn public.deposit (Topup) -- satu baris = satu pembayaran, tidak ada status
// "sudah dialokasikan ke transaksi mana".
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    out.print("<div class='text-center p-5'><i class='fas fa-exclamation-triangle fa-3x text-warning mb-3'></i><br><h5 class='text-muted'>" + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "</h5></div>");
    return;
}
String rnd = Common.getGeneratedBarCode(7);
%>

<div class="container-fluid py-3 py-md-4 animate__animated animate__fadeIn" id="mutasiHutangContent<%=rnd%>">

    <!-- HEADER -->
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-center mb-4 gap-3">
        <div class="text-center text-md-start">
            <h4 class="fw-bolder text-danger mb-1">
                <i class="fas fa-hand-holding-usd me-2"></i><%=Common.getBahasaConfig("Mutasi Hutang (Piutang Toko)")%>
            </h4>
            <p class="text-muted small mb-0"><%=Common.getBahasaConfig("Buku besar hutang anggota: transaksi berbayar cara \"masuk sebagai hutang\" (bertambah) dan pelunasan/cicilan manual (berkurang).")%></p>
        </div>
        <div class="d-flex gap-2 flex-wrap justify-content-center">
            <button class="btn btn-outline-success rounded-pill px-3 shadow-sm fw-bold" onclick="downloadExcelHutang<%=rnd%>()" title="<%=Common.getBahasaConfig("Unduh Excel")%>">
                <i class="fas fa-file-excel me-1"></i><%=Common.getBahasaConfig("Download")%>
            </button>
            <button class="btn btn-outline-warning rounded-pill px-3 shadow-sm fw-bold" onclick="showUploadModalHutang<%=rnd%>()" title="<%=Common.getBahasaConfig("Unggah Excel Pembayaran Hutang")%>">
                <i class="fas fa-file-upload me-1"></i><%=Common.getBahasaConfig("Upload")%>
            </button>
            <button class="btn btn-outline-secondary rounded-pill px-3 shadow-sm fw-bold" onclick="cetakPdfHutang<%=rnd%>()" title="<%=Common.getBahasaConfig("Cetak PDF")%>">
                <i class="fas fa-file-pdf me-1"></i><%=Common.getBahasaConfig("Cetak PDF")%>
            </button>
            <button class="btn btn-danger rounded-pill px-3 shadow-sm fw-bold" onclick="bukaFormBayarHutang<%=rnd%>()">
                <i class="fas fa-money-bill-wave me-1"></i><%=Common.getBahasaConfig("Bayar Hutang")%>
            </button>
        </div>
    </div>

    <!-- FILTER SECTION -->
    <div class="card border-0 shadow-sm rounded-4 mb-4 border-top border-danger border-3">
        <div class="card-body p-3 p-md-4">
            <div class="row g-3 align-items-end">
                <div class="col-md-4">
                    <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Anggota (Opsional, kosongkan utk semua)")%></label>
                    <div class="input-group shadow-sm">
                        <span class="input-group-text bg-white border-end-0 text-muted"><i class="fas fa-user"></i></span>
                        <input type="text" class="form-control border-start-0 fw-medium filter-hutang-<%=rnd%>" list="listAnggotaHutang<%=rnd%>" id="inputCariAnggotaHutang<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik kode / nama anggota...")%>" autocomplete="off">
                    </div>
                    <datalist id="listAnggotaHutang<%=rnd%>"></datalist>
                    <input type="hidden" id="idAnggotaHutangTerpilih<%=rnd%>" value="">
                </div>
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Tanggal Mulai")%></label>
                    <input type="date" class="form-control fw-medium shadow-sm filter-hutang-<%=rnd%>" id="searchDateStartHutang<%=rnd%>">
                </div>
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Tanggal Akhir")%></label>
                    <input type="date" class="form-control fw-medium shadow-sm filter-hutang-<%=rnd%>" id="searchDateEndHutang<%=rnd%>">
                </div>
                <div class="col-md-2 d-grid">
                    <button class="btn btn-danger rounded-3 shadow-sm fw-bold" onclick="resetAndLoadHutang<%=rnd%>()">
                        <i class="fas fa-filter me-2"></i><%=Common.getBahasaConfig("Saring")%>
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- DETAIL LEDGER -->
    <div class="card border-0 shadow-sm rounded-4 overflow-hidden mb-4" id="detailMutasiHutang<%=rnd%>">
        <div class="card-header bg-white border-0 pt-4 pb-2 px-4">
            <h6 class="fw-bold text-dark mb-0"><%=Common.getBahasaConfig("Buku Besar Mutasi Hutang")%></h6>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive px-3 py-2" style="min-height: 300px;">
                <table class="table table-hover align-middle mb-0 small" id="tabelCetakHutang<%=rnd%>">
                    <thead class="table-light text-secondary">
                        <tr>
                            <th class="fw-bold text-uppercase small"><%=Common.getBahasaConfig("Nama")%></th>
                            <th class="fw-bold text-uppercase small"><%=Common.getBahasaConfig("Tanggal")%></th>
                            <th class="fw-bold text-uppercase small"><%=Common.getBahasaConfig("Jenis Mutasi")%></th>
                            <th class="fw-bold text-uppercase small"><%=Common.getBahasaConfig("Keterangan")%></th>
                            <th class="fw-bold text-uppercase small text-end"><%=Common.getBahasaConfig("Hutang Bertambah")%></th>
                            <th class="fw-bold text-uppercase small text-end"><%=Common.getBahasaConfig("Pembayaran")%></th>
                            <th class="fw-bold text-uppercase small text-end"><%=Common.getBahasaConfig("Saldo Hutang Anggota")%></th>
                            <th class="fw-bold text-uppercase small text-end"><%=Common.getBahasaConfig("Saldo Hutang Total")%></th>
                        </tr>
                    </thead>
                    <tbody id="tabelDataHutang<%=rnd%>">
                        <tr>
                            <td colspan="8" class="text-center py-5">
                                <div class="spinner-border text-danger mb-3"></div>
                                <br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Sistem sedang memuat data mutasi hutang...")%></span>
                            </td>
                        </tr>
                    </tbody>
                    <tfoot id="tabelFooterHutang<%=rnd%>"></tfoot>
                </table>
            </div>

            <!-- PAGING -->
            <div class="bg-light p-3 border-top d-flex flex-column flex-md-row justify-content-between align-items-center gap-3">
                <div class="small fw-bold text-muted" id="pagingInfoHutang<%=rnd%>"></div>
                <div class="d-flex align-items-center bg-white rounded-pill shadow-sm border p-1" id="pagingContainerHutang<%=rnd%>">
                    <button class="btn btn-sm btn-light rounded-pill px-3 fw-bold text-danger" id="btnPrevPageHutang<%=rnd%>" onclick="changePageHutang<%=rnd%>(-1)">
                        <i class="fas fa-chevron-left me-1"></i><span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Sebelumnya")%></span>
                    </button>
                    <div class="px-3 fw-bold text-dark small">
                        <span id="lblCurrentPageHutang<%=rnd%>">1</span> / <span id="lblTotalPageHutang<%=rnd%>">1</span>
                    </div>
                    <button class="btn btn-sm btn-light rounded-pill px-3 fw-bold text-danger" id="btnNextPageHutang<%=rnd%>" onclick="changePageHutang<%=rnd%>(1)">
                        <span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Selanjutnya")%></span><i class="fas fa-chevron-right ms-1"></i>
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- SUMMARY PER ANGGOTA -->
    <div class="card border-0 shadow-sm rounded-4 overflow-hidden mb-4" id="rekapMutasiHutang<%=rnd%>">
        <div class="card-header bg-white border-0 pt-4 pb-2 px-4">
            <h6 class="fw-bold text-dark mb-0"><%=Common.getBahasaConfig("Rekap Hutang per Anggota")%></h6>
            <small class="text-muted"><%=Common.getBahasaConfig("Saldo awal dihitung dari seluruh transaksi sebelum tanggal mulai; saldo akhir adalah sisa hutang.")%></small>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive px-3 py-2">
                <table class="table table-hover align-middle mb-0 small">
                    <thead class="table-light text-secondary">
                        <tr>
                            <th class="fw-bold text-uppercase small"><%=Common.getBahasaConfig("Nama")%></th>
                            <th class="fw-bold text-uppercase small text-end"><%=Common.getBahasaConfig("Saldo Awal")%></th>
                            <th class="fw-bold text-uppercase small text-end"><%=Common.getBahasaConfig("Hutang Bertambah")%></th>
                            <th class="fw-bold text-uppercase small text-end"><%=Common.getBahasaConfig("Pembayaran")%></th>
                            <th class="fw-bold text-uppercase small text-end"><%=Common.getBahasaConfig("Saldo Akhir")%></th>
                        </tr>
                    </thead>
                    <tbody id="tabelSummaryHutang<%=rnd%>">
                        <tr><td colspan="5" class="text-center py-4 text-muted"><%=Common.getBahasaConfig("Memuat...")%></td></tr>
                    </tbody>
                    <tfoot id="tabelSummaryFooterHutang<%=rnd%>"></tfoot>
                </table>
            </div>
        </div>
    </div>

</div>

<!-- MODAL: BAYAR HUTANG (entri manual pelunasan/cicilan) -->
<div class="modal fade" id="modalBayarHutang<%=rnd%>" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 rounded-4 shadow-lg">
            <div class="modal-header bg-light border-bottom-0 pb-3">
                <h5 class="modal-title fw-bold text-dark">
                    <i class="fas fa-money-bill-wave text-danger me-2"></i><%=Common.getBahasaConfig("Entri Pembayaran Hutang")%>
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4 pt-2">
                <div class="mb-3">
                    <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Anggota")%> <span class="text-danger">*</span></label>
                    <div class="input-group shadow-sm">
                        <span class="input-group-text bg-white border-end-0 text-muted"><i class="fas fa-user"></i></span>
                        <input type="text" class="form-control border-start-0 fw-bold" list="listAnggotaBayarHutang<%=rnd%>" id="inputCariAnggotaBayarHutang<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik kode / nama anggota...")%>" autocomplete="off">
                    </div>
                    <datalist id="listAnggotaBayarHutang<%=rnd%>"></datalist>
                    <input type="hidden" id="idAnggotaBayarHutangTerpilih<%=rnd%>" value="">
                </div>
                <div class="mb-3">
                    <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nominal Pembayaran")%> <span class="text-danger">*</span></label>
                    <div class="input-group shadow-sm">
                        <span class="input-group-text bg-light border-end-0">Rp</span>
                        <input type="number" min="0" step="1" class="form-control fw-bold border-start-0" id="inputNominalBayarHutang<%=rnd%>" placeholder="0">
                    </div>
                </div>
                <div class="mb-3">
                    <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Waktu")%></label>
                    <input type="datetime-local" class="form-control fw-medium shadow-sm" id="inputWaktuBayarHutang<%=rnd%>">
                </div>
                <div class="mb-1">
                    <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Keterangan")%></label>
                    <textarea class="form-control shadow-sm" id="inputKeteranganBayarHutang<%=rnd%>" rows="2" placeholder="<%=Common.getBahasaConfig("Contoh: Cicilan bulan Agustus")%>"></textarea>
                </div>
            </div>
            <div class="modal-footer border-top-0 pt-0">
                <button type="button" class="btn btn-light px-4 rounded-pill fw-semibold" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
                <button type="button" class="btn btn-danger px-4 rounded-pill fw-bold shadow-sm" id="btnSimpanBayarHutang<%=rnd%>" onclick="simpanBayarHutang<%=rnd%>()">
                    <i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan")%>
                </button>
            </div>
        </div>
    </div>
</div>

<!-- MODAL: UPLOAD EXCEL PEMBAYARAN HUTANG -->
<div class="modal fade" id="modalUploadExcelHutang<%=rnd%>" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 rounded-4 shadow-lg">
            <div class="modal-header bg-light border-bottom-0 pb-3">
                <h5 class="modal-title fw-bold text-dark">
                    <i class="fas fa-file-upload text-warning me-2"></i><%=Common.getBahasaConfig("Unggah Data Pembayaran Hutang")%>
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4 pt-2 text-center">
                <p class="text-muted small mb-4"><%=Common.getBahasaConfig("Kolom wajib: KODE_ANGGOTA, NOMINAL. Opsional: WAKTU (YYYY-MM-DD HH:mm:ss), KETERANGAN. Ekstensi yang didukung: .xls, .xlsx")%></p>
                <input class="form-control mb-3" type="file" id="fileUploadExcelHutang<%=rnd%>" accept=".xls,.xlsx">
                <div id="uploadStatusBoxHutang<%=rnd%>" class="alert alert-info py-2" style="display: none;"></div>
                <div class="mt-4">
                    <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
                    <button type="button" class="btn btn-warning text-dark px-4 rounded-pill fw-bold shadow-sm" id="btnProsesUploadHutang<%=rnd%>" onclick="prosesUploadExcelHutang<%=rnd%>()">
                        <i class="fas fa-cogs me-2"></i><%=Common.getBahasaConfig("Proses Data")%>
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    // ==========================================
    // VARIABEL GLOBAL
    // ==========================================
    const masterClassBayarHutang<%=rnd%> = "ais.database.model.koperasi.PembayaranHutang";
    let currentPageHutang<%=rnd%> = 1;
    const limitPerPageHutang<%=rnd%> = 10;
    let dataMutasiHutangLengkap<%=rnd%> = [];
    let timerPencarianAnggotaHutang<%=rnd%> = null;
    let modalInstanceBayarHutang<%=rnd%> = null;
    let modalInstanceUploadHutang<%=rnd%> = null;

    const formatRpHutang<%=rnd%> = (angka) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);

    const fetchApiHutang<%=rnd%> = async (payload) => {
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            return await res.json();
        } catch (e) {
            console.error(e);
            return { status: "error" };
        }
    };

    const showToastHutang<%=rnd%> = (msg, colorClass) => {
        if (typeof tampilkanToast === "function") {
            tampilkanToast(msg, colorClass);
        } else {
            alert(msg);
        }
    };

    const setDefaultDatesHutang<%=rnd%> = () => {
        const today = new Date();
        const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
        const todayStr = today.getFullYear() + '-' + String(today.getMonth() + 1).padStart(2, '0') + '-' + String(today.getDate()).padStart(2, '0');
        const firstDayStr = firstDay.getFullYear() + '-' + String(firstDay.getMonth() + 1).padStart(2, '0') + '-01';
        document.getElementById('searchDateEndHutang<%=rnd%>').value = todayStr;
        document.getElementById('searchDateStartHutang<%=rnd%>').value = firstDayStr;
    };

    // ==========================================
    // LIVE SEARCH ANGGOTA (filter ledger)
    // ==========================================
    const tarikDataAnggotaHutang<%=rnd%> = async (keyword, datalistId) => {
        const safeKeyword = keyword.replace(/'/g, "''");
        const sql = "SELECT id, kode, nama FROM koperasi.anggota_koperasi " +
                    "WHERE (nama ILIKE '%" + safeKeyword + "%' OR kode ILIKE '%" + safeKeyword + "%') " +
                    "ORDER BY nama ASC LIMIT 50";
        const res = await fetchApiHutang<%=rnd%>({ action: "sql", sql: sql });
        let html = '';
        (res.data || []).forEach(item => {
            html += '<option value="' + item.kode + ' - ' + item.nama + '" data-id="' + item.id + '">';
        });
        document.getElementById(datalistId).innerHTML = html;
    };

    const pasangSearchAnggota<%=rnd%> = (inputId, datalistId, hiddenId) => {
        const input = document.getElementById(inputId);
        input.addEventListener('input', function() {
            const keyword = this.value.trim();
            if (keyword === '') { document.getElementById(hiddenId).value = ''; return; }
            const isMatch = Array.from(document.getElementById(datalistId).options).some(opt => opt.value === keyword);
            if (!isMatch) {
                clearTimeout(timerPencarianAnggotaHutang<%=rnd%>);
                timerPencarianAnggotaHutang<%=rnd%> = setTimeout(() => tarikDataAnggotaHutang<%=rnd%>(keyword, datalistId), 500);
            }
        });
        input.addEventListener('change', function() {
            const val = this.value;
            let matchId = '';
            Array.from(document.getElementById(datalistId).childNodes).forEach(opt => {
                if (opt.value === val) matchId = opt.getAttribute('data-id');
            });
            document.getElementById(hiddenId).value = matchId;
        });
    };

    // ==========================================
    // BUILD & JALANKAN QUERY LEDGER
    // ==========================================
    const resetAndLoadHutang<%=rnd%> = () => {
        currentPageHutang<%=rnd%> = 1;
        loadDataHutang<%=rnd%>();
    };

    const loadDataHutang<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataHutang<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="8" class="text-center py-5"><div class="spinner-border text-danger mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Sistem sedang memuat data mutasi hutang...")%></span></td></tr>';
        document.getElementById('tabelFooterHutang<%=rnd%>').innerHTML = '';
        document.getElementById('tabelSummaryHutang<%=rnd%>').innerHTML = '<tr><td colspan="5" class="text-center py-4 text-muted"><%=Common.getBahasaConfig("Memuat...")%></td></tr>';
        document.getElementById('tabelSummaryFooterHutang<%=rnd%>').innerHTML = '';

        const tglAwal = document.getElementById('searchDateStartHutang<%=rnd%>').value;
        const tglAkhir = document.getElementById('searchDateEndHutang<%=rnd%>').value;
        const idAnggota = document.getElementById('idAnggotaHutangTerpilih<%=rnd%>').value;

        if (tglAwal === '' || tglAkhir === '') {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-warning py-5"><i class="fas fa-calendar-times fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Tanggal Mulai dan Tanggal Akhir wajib diisi.")%></td></tr>';
            return;
        }

        const batasAwal = "CAST('" + tglAwal + "' AS date)";
        const batasAkhir = "(CAST('" + tglAkhir + "' AS date) + interval '1 day')";
        const filterAnggota = idAnggota !== '' ? (" AND anggota_koperasi = " + parseInt(idAnggota, 10) + " ") : "";

        // Ekspresi nominal slot-1 (implisit, lihat JavaDoc PembelianAnggotaKoperasi.getNominalBayar1())
        const n1 = "GREATEST(0, COALESCE(h.total_biaya,0) - COALESCE(h.nominal_bayar_2,0) - COALESCE(h.nominal_bayar_3,0) - COALESCE(h.nominal_bayar_4,0) - COALESCE(h.nominal_bayar_5,0))";

        const sql =
            "WITH semua_mutasi AS ( " +
            "  SELECT h.tanggal_pembayaran AS waktu, ('H1' || h.id) AS baris_id, (a.kode || ' - ' || a.nama) AS nama_anggota, h.anggota_koperasi AS id_anggota, " +
            "         'Belanja (Hutang)' AS jenis_mutasi, COALESCE(h.kode, '') AS keterangan, " + n1 + " AS bertambah, 0 AS berkurang " +
            "  FROM koperasi.pembelian_anggota_koperasi h " +
            "  JOIN koperasi.anggota_koperasi a ON h.anggota_koperasi = a.id " +
            "  JOIN koperasi.cara_pembayaran_koperasi cpk1 ON h.cara_pembayaran_koperasi = cpk1.id " +
            "  WHERE cpk1.masuk_sebagai_hutang = true AND h.anggota_koperasi IS NOT NULL AND " + n1 + " > 0 " +
            "  AND h.tanggal_pembayaran < " + batasAkhir + filterAnggota.replace(/anggota_koperasi/g, "h.anggota_koperasi") +
            "  UNION ALL " +
            "  SELECT h.tanggal_pembayaran, ('H2' || h.id), (a.kode || ' - ' || a.nama), h.anggota_koperasi, " +
            "         'Belanja (Hutang)', COALESCE(h.kode, ''), COALESCE(h.nominal_bayar_2,0), 0 " +
            "  FROM koperasi.pembelian_anggota_koperasi h " +
            "  JOIN koperasi.anggota_koperasi a ON h.anggota_koperasi = a.id " +
            "  JOIN koperasi.cara_pembayaran_koperasi cpk2 ON h.cara_pembayaran_koperasi_2 = cpk2.id " +
            "  WHERE cpk2.masuk_sebagai_hutang = true AND h.anggota_koperasi IS NOT NULL AND COALESCE(h.nominal_bayar_2,0) > 0 " +
            "  AND h.tanggal_pembayaran < " + batasAkhir + filterAnggota.replace(/anggota_koperasi/g, "h.anggota_koperasi") +
            "  UNION ALL " +
            "  SELECT h.tanggal_pembayaran, ('H3' || h.id), (a.kode || ' - ' || a.nama), h.anggota_koperasi, " +
            "         'Belanja (Hutang)', COALESCE(h.kode, ''), COALESCE(h.nominal_bayar_3,0), 0 " +
            "  FROM koperasi.pembelian_anggota_koperasi h " +
            "  JOIN koperasi.anggota_koperasi a ON h.anggota_koperasi = a.id " +
            "  JOIN koperasi.cara_pembayaran_koperasi cpk3 ON h.cara_pembayaran_koperasi_3 = cpk3.id " +
            "  WHERE cpk3.masuk_sebagai_hutang = true AND h.anggota_koperasi IS NOT NULL AND COALESCE(h.nominal_bayar_3,0) > 0 " +
            "  AND h.tanggal_pembayaran < " + batasAkhir + filterAnggota.replace(/anggota_koperasi/g, "h.anggota_koperasi") +
            "  UNION ALL " +
            "  SELECT h.tanggal_pembayaran, ('H4' || h.id), (a.kode || ' - ' || a.nama), h.anggota_koperasi, " +
            "         'Belanja (Hutang)', COALESCE(h.kode, ''), COALESCE(h.nominal_bayar_4,0), 0 " +
            "  FROM koperasi.pembelian_anggota_koperasi h " +
            "  JOIN koperasi.anggota_koperasi a ON h.anggota_koperasi = a.id " +
            "  JOIN koperasi.cara_pembayaran_koperasi cpk4 ON h.cara_pembayaran_koperasi_4 = cpk4.id " +
            "  WHERE cpk4.masuk_sebagai_hutang = true AND h.anggota_koperasi IS NOT NULL AND COALESCE(h.nominal_bayar_4,0) > 0 " +
            "  AND h.tanggal_pembayaran < " + batasAkhir + filterAnggota.replace(/anggota_koperasi/g, "h.anggota_koperasi") +
            "  UNION ALL " +
            "  SELECT h.tanggal_pembayaran, ('H5' || h.id), (a.kode || ' - ' || a.nama), h.anggota_koperasi, " +
            "         'Belanja (Hutang)', COALESCE(h.kode, ''), COALESCE(h.nominal_bayar_5,0), 0 " +
            "  FROM koperasi.pembelian_anggota_koperasi h " +
            "  JOIN koperasi.anggota_koperasi a ON h.anggota_koperasi = a.id " +
            "  JOIN koperasi.cara_pembayaran_koperasi cpk5 ON h.cara_pembayaran_koperasi_5 = cpk5.id " +
            "  WHERE cpk5.masuk_sebagai_hutang = true AND h.anggota_koperasi IS NOT NULL AND COALESCE(h.nominal_bayar_5,0) > 0 " +
            "  AND h.tanggal_pembayaran < " + batasAkhir + filterAnggota.replace(/anggota_koperasi/g, "h.anggota_koperasi") +
            "  UNION ALL " +
            "  SELECT ph.waktu, ('C' || ph.id), (a.kode || ' - ' || a.nama), ph.anggota_koperasi, " +
            "         'Pembayaran Hutang', COALESCE(ph.keterangan, ''), 0, COALESCE(ph.nominal, 0) " +
            "  FROM koperasi.pembayaran_hutang ph " +
            "  JOIN koperasi.anggota_koperasi a ON ph.anggota_koperasi = a.id " +
            "  WHERE ph.waktu < " + batasAkhir + filterAnggota.replace(/anggota_koperasi/g, "ph.anggota_koperasi") +
            "), saldo_awal AS (SELECT id_anggota, SUM(bertambah-berkurang) AS saldo_awal FROM semua_mutasi WHERE waktu < " + batasAwal + " GROUP BY id_anggota), " +
            "mutasi AS (SELECT * FROM semua_mutasi WHERE waktu >= " + batasAwal + ") " +
            "SELECT m.*, COALESCE(sa.saldo_awal,0) AS saldo_awal, " +
            "  COALESCE(sa.saldo_awal,0) + SUM(bertambah - berkurang) OVER (PARTITION BY m.id_anggota ORDER BY waktu, baris_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS saldo_per_anggota, " +
            "  (SELECT COALESCE(SUM(saldo_awal),0) FROM saldo_awal) + SUM(bertambah - berkurang) OVER (ORDER BY waktu, baris_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS saldo_total " +
            "FROM mutasi m LEFT JOIN saldo_awal sa ON sa.id_anggota=m.id_anggota ORDER BY waktu ASC, baris_id ASC;";

        try {
            const res = await fetchApiHutang<%=rnd%>({ action: "sql", sql: sql });
            dataMutasiHutangLengkap<%=rnd%> = res.data || [];
            renderTabelHutang<%=rnd%>();
            renderSummaryHutang<%=rnd%>();
        } catch (error) {
            console.error("Gagal load mutasi hutang:", error);
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3"></i><br><%=Common.getBahasaConfig("Gagal menarik data dari peladen.")%></td></tr>';
        }
    };

    const badgeJenisHutang<%=rnd%> = (jenis) => {
        const peta = { 'Belanja (Hutang)': 'danger', 'Pembayaran Hutang': 'success' };
        const warna = peta[jenis] || 'secondary';
        return '<span class="badge bg-' + warna + ' bg-opacity-25 text-' + warna + ' border border-' + warna + ' px-2 py-1">' + jenis + '</span>';
    };

    const formatTanggalHutang<%=rnd%> = (iso) => {
        if (!iso) return '-';
        const d = new Date(iso);
        if (isNaN(d.getTime())) return iso;
        const bulan = ['Jan','Feb','Mar','Apr','Mei','Jun','Jul','Agu','Sep','Okt','Nov','Des'];
        const pad = (n) => String(n).padStart(2, '0');
        return pad(d.getDate()) + ' ' + bulan[d.getMonth()] + ' ' + d.getFullYear() + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
    };

    const renderTabelHutang<%=rnd%> = () => {
        const tbody = document.getElementById('tabelDataHutang<%=rnd%>');
        const tfoot = document.getElementById('tabelFooterHutang<%=rnd%>');
        const total = dataMutasiHutangLengkap<%=rnd%>.length;

        if (total === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-5"><i class="fas fa-hand-holding-usd fa-3x mb-3 opacity-25"></i><br><%=Common.getBahasaConfig("Tidak ada mutasi hutang pada rentang ini.")%></td></tr>';
            updatePagingUIHutang<%=rnd%>();
            return;
        }

        const startIdx = (currentPageHutang<%=rnd%> - 1) * limitPerPageHutang<%=rnd%>;
        const pageRows = dataMutasiHutangLengkap<%=rnd%>.slice(startIdx, startIdx + limitPerPageHutang<%=rnd%>);

        let html = '';
        pageRows.forEach((row) => {
            html += '<tr>' +
                '<td class="fw-bold text-dark">' + (row.nama_anggota || '-') + '</td>' +
                '<td class="text-secondary">' + formatTanggalHutang<%=rnd%>(row.waktu) + '</td>' +
                '<td>' + badgeJenisHutang<%=rnd%>(row.jenis_mutasi) + '</td>' +
                '<td class="text-muted">' + (row.keterangan || '') + '</td>' +
                '<td class="text-end fw-bold' + (row.bertambah > 0 ? ' text-danger' : ' text-muted') + '">' + (row.bertambah > 0 ? formatRpHutang<%=rnd%>(row.bertambah) : '-') + '</td>' +
                '<td class="text-end fw-bold' + (row.berkurang > 0 ? ' text-success' : ' text-muted') + '">' + (row.berkurang > 0 ? formatRpHutang<%=rnd%>(row.berkurang) : '-') + '</td>' +
                '<td class="text-end fw-bolder text-danger">' + formatRpHutang<%=rnd%>(row.saldo_per_anggota) + '</td>' +
                '<td class="text-end fw-bold text-dark">' + formatRpHutang<%=rnd%>(row.saldo_total) + '</td>' +
                '</tr>';
        });
        tbody.innerHTML = html;

        const totalBertambah = dataMutasiHutangLengkap<%=rnd%>.reduce((s, r) => s + (parseFloat(r.bertambah) || 0), 0);
        const totalBerkurang = dataMutasiHutangLengkap<%=rnd%>.reduce((s, r) => s + (parseFloat(r.berkurang) || 0), 0);
        const saldoAkhir = dataMutasiHutangLengkap<%=rnd%>[total - 1].saldo_total;
        tfoot.innerHTML = '<tr class="table-light fw-bold">' +
            '<td colspan="4" class="text-end"><%=Common.getBahasaConfig("TOTAL")%> :</td>' +
            '<td class="text-end text-danger">' + formatRpHutang<%=rnd%>(totalBertambah) + '</td>' +
            '<td class="text-end text-success">' + formatRpHutang<%=rnd%>(totalBerkurang) + '</td>' +
            '<td class="text-end text-muted">&ndash;</td>' +
            '<td class="text-end text-dark">' + formatRpHutang<%=rnd%>(saldoAkhir) + '</td>' +
            '</tr>';

        updatePagingUIHutang<%=rnd%>();
    };

    const renderSummaryHutang<%=rnd%> = () => {
        const tbody = document.getElementById('tabelSummaryHutang<%=rnd%>');
        const tfoot = document.getElementById('tabelSummaryFooterHutang<%=rnd%>');

        if (dataMutasiHutangLengkap<%=rnd%>.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-4"><%=Common.getBahasaConfig("Tidak ada data.")%></td></tr>';
            return;
        }

        const peta = new Map();
        dataMutasiHutangLengkap<%=rnd%>.forEach(row => {
            const key = row.id_anggota;
            if (!peta.has(key)) peta.set(key, { nama: row.nama_anggota, awal: parseFloat(row.saldo_awal) || 0, bertambah: 0, berkurang: 0 });
            const rec = peta.get(key);
            rec.bertambah += parseFloat(row.bertambah) || 0;
            rec.berkurang += parseFloat(row.berkurang) || 0;
        });

        let html = '';
        let totalAwal = 0, totalBertambah = 0, totalBerkurang = 0;
        peta.forEach(rec => {
            const sisa = rec.awal + rec.bertambah - rec.berkurang;
            totalAwal += rec.awal;
            totalBertambah += rec.bertambah;
            totalBerkurang += rec.berkurang;
            html += '<tr>' +
                '<td class="fw-bold text-dark">' + rec.nama + '</td>' +
                '<td class="text-end text-secondary">' + formatRpHutang<%=rnd%>(rec.awal) + '</td>' +
                '<td class="text-end text-danger">' + formatRpHutang<%=rnd%>(rec.bertambah) + '</td>' +
                '<td class="text-end text-success">' + formatRpHutang<%=rnd%>(rec.berkurang) + '</td>' +
                '<td class="text-end fw-bolder ' + (sisa > 0 ? 'text-danger' : 'text-success') + '">' + formatRpHutang<%=rnd%>(sisa) + '</td>' +
                '</tr>';
        });
        tbody.innerHTML = html;

        tfoot.innerHTML = '<tr class="table-light fw-bold">' +
            '<td class="text-end"><%=Common.getBahasaConfig("TOTAL")%> :</td>' +
            '<td class="text-end text-secondary">' + formatRpHutang<%=rnd%>(totalAwal) + '</td>' +
            '<td class="text-end text-danger">' + formatRpHutang<%=rnd%>(totalBertambah) + '</td>' +
            '<td class="text-end text-success">' + formatRpHutang<%=rnd%>(totalBerkurang) + '</td>' +
            '<td class="text-end text-dark">' + formatRpHutang<%=rnd%>(totalAwal + totalBertambah - totalBerkurang) + '</td>' +
            '</tr>';
    };

    const updatePagingUIHutang<%=rnd%> = () => {
        const total = dataMutasiHutangLengkap<%=rnd%>.length;
        const totalPages = Math.ceil(total / limitPerPageHutang<%=rnd%>) || 1;
        document.getElementById('lblCurrentPageHutang<%=rnd%>').innerText = currentPageHutang<%=rnd%>;
        document.getElementById('lblTotalPageHutang<%=rnd%>').innerText = totalPages;

        const startIdx = total > 0 ? ((currentPageHutang<%=rnd%> - 1) * limitPerPageHutang<%=rnd%> + 1) : 0;
        const endIdx = Math.min(currentPageHutang<%=rnd%> * limitPerPageHutang<%=rnd%>, total);
        document.getElementById('pagingInfoHutang<%=rnd%>').innerHTML = '<%=Common.getBahasaConfigJS("Menampilkan")%> <span class="text-dark">' + startIdx + ' - ' + endIdx + '</span> <%=Common.getBahasaConfig("dari")%> <span class="text-dark">' + total + '</span> <%=Common.getBahasaConfig("mutasi")%>';

        document.getElementById('btnPrevPageHutang<%=rnd%>').disabled = (currentPageHutang<%=rnd%> === 1);
        document.getElementById('btnNextPageHutang<%=rnd%>').disabled = (currentPageHutang<%=rnd%> === totalPages);
    };

    const changePageHutang<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(dataMutasiHutangLengkap<%=rnd%>.length / limitPerPageHutang<%=rnd%>);
        if (direction === -1 && currentPageHutang<%=rnd%> > 1) {
            currentPageHutang<%=rnd%>--;
            renderTabelHutang<%=rnd%>();
        } else if (direction === 1 && currentPageHutang<%=rnd%> < totalPages) {
            currentPageHutang<%=rnd%>++;
            renderTabelHutang<%=rnd%>();
        }
    };

    // ==========================================
    // FORM BAYAR HUTANG (INSERT koperasi.pembayaran_hutang)
    // ==========================================
    const bukaFormBayarHutang<%=rnd%> = () => {
        document.getElementById('inputCariAnggotaBayarHutang<%=rnd%>').value = '';
        document.getElementById('idAnggotaBayarHutangTerpilih<%=rnd%>').value = '';
        document.getElementById('inputNominalBayarHutang<%=rnd%>').value = '';
        document.getElementById('inputKeteranganBayarHutang<%=rnd%>').value = '';
        const now = new Date();
        document.getElementById('inputWaktuBayarHutang<%=rnd%>').value = new Date(now.getTime() - (now.getTimezoneOffset() * 60000)).toISOString().slice(0, 16);

        if (modalInstanceBayarHutang<%=rnd%> === null) {
            modalInstanceBayarHutang<%=rnd%> = new bootstrap.Modal(document.getElementById('modalBayarHutang<%=rnd%>'));
        }
        modalInstanceBayarHutang<%=rnd%>.show();
    };

    const simpanBayarHutang<%=rnd%> = async () => {
        const idAnggota = document.getElementById('idAnggotaBayarHutangTerpilih<%=rnd%>').value;
        const nominal = parseFloat(document.getElementById('inputNominalBayarHutang<%=rnd%>').value || 0);

        if (idAnggota === '') {
            showToastHutang<%=rnd%>('<%=Common.getBahasaConfigJS("Silakan pilih Anggota Koperasi yang valid dari daftar pencarian.")%>', 'bg-warning text-dark');
            return;
        }
        if (nominal <= 0) {
            showToastHutang<%=rnd%>('<%=Common.getBahasaConfigJS("Nominal pembayaran harus lebih dari 0.")%>', 'bg-warning text-dark');
            return;
        }

        const rawWaktu = document.getElementById('inputWaktuBayarHutang<%=rnd%>').value;
        const formattedWaktu = rawWaktu ? (rawWaktu.replace('T', ' ') + ":00") : "";

        const dataObj = {
            anggotaKoperasi: idAnggota,
            nominal: nominal,
            waktu: formattedWaktu || undefined,
            keterangan: document.getElementById('inputKeteranganBayarHutang<%=rnd%>').value.trim()
        };

        const btnSimpan = document.getElementById('btnSimpanBayarHutang<%=rnd%>');
        const oriBtn = btnSimpan.innerHTML;
        btnSimpan.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyimpan...")%>';
        btnSimpan.disabled = true;

        try {
            const result = await fetchApiHutang<%=rnd%>({ action: "simpanDataRinci", class: masterClassBayarHutang<%=rnd%>, data: dataObj });
            if (result.status === '00' || result.status === 'success' || result.id) {
                showToastHutang<%=rnd%>('<%=Common.getBahasaConfigJS("Pembayaran hutang berhasil disimpan.")%>', 'bg-success text-white');
                modalInstanceBayarHutang<%=rnd%>.hide();
                resetAndLoadHutang<%=rnd%>();
            } else {
                showToastHutang<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan data.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            console.error(e);
            showToastHutang<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi saat menyimpan.")%>', 'bg-danger text-white');
        } finally {
            btnSimpan.innerHTML = oriBtn;
            btnSimpan.disabled = false;
        }
    };

    // ==========================================
    // DOWNLOAD EXCEL (ekspor ledger yg sedang ditampilkan)
    // ==========================================
    const downloadExcelHutang<%=rnd%> = () => {
        if (dataMutasiHutangLengkap<%=rnd%>.length === 0) {
            showToastHutang<%=rnd%>('<%=Common.getBahasaConfigJS("Tidak ada data untuk diunduh. Silakan saring data terlebih dahulu.")%>', 'bg-warning text-dark');
            return;
        }
        const rows = dataMutasiHutangLengkap<%=rnd%>.map(r => ({
            NAMA_ANGGOTA: r.nama_anggota || '',
            TANGGAL: formatTanggalHutang<%=rnd%>(r.waktu),
            JENIS_MUTASI: r.jenis_mutasi || '',
            KETERANGAN: r.keterangan || '',
            HUTANG_BERTAMBAH: parseFloat(r.bertambah) || 0,
            PEMBAYARAN: parseFloat(r.berkurang) || 0,
            SALDO_HUTANG_ANGGOTA: parseFloat(r.saldo_per_anggota) || 0,
            SALDO_HUTANG_TOTAL: parseFloat(r.saldo_total) || 0
        }));
        const ws = XLSX.utils.json_to_sheet(rows);
        const wb = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(wb, ws, "Mutasi Hutang");
        const tglAwal = document.getElementById('searchDateStartHutang<%=rnd%>').value;
        const tglAkhir = document.getElementById('searchDateEndHutang<%=rnd%>').value;
        XLSX.writeFile(wb, "Mutasi_Hutang_" + tglAwal + "_sd_" + tglAkhir + ".xlsx");
    };

    // ==========================================
    // UPLOAD EXCEL (bulk entri pembayaran/pelunasan hutang)
    // ==========================================
    const showUploadModalHutang<%=rnd%> = () => {
        document.getElementById('fileUploadExcelHutang<%=rnd%>').value = '';
        const statusBox = document.getElementById('uploadStatusBoxHutang<%=rnd%>');
        statusBox.style.display = 'none';
        statusBox.innerHTML = '';
        if (modalInstanceUploadHutang<%=rnd%> === null) {
            modalInstanceUploadHutang<%=rnd%> = new bootstrap.Modal(document.getElementById('modalUploadExcelHutang<%=rnd%>'));
        }
        modalInstanceUploadHutang<%=rnd%>.show();
    };

    const cariIdAnggotaByKodeHutang<%=rnd%> = async (kode) => {
        if (!kode) return null;
        const safeKode = String(kode).trim().replace(/'/g, "''");
        const sql = "SELECT id FROM koperasi.anggota_koperasi WHERE kode = '" + safeKode + "' LIMIT 1";
        const res = await fetchApiHutang<%=rnd%>({ action: "sql", sql: sql });
        return (res.data && res.data[0]) ? res.data[0].id : null;
    };

    const prosesUploadExcelHutang<%=rnd%> = () => {
        const fileInput = document.getElementById('fileUploadExcelHutang<%=rnd%>');
        const file = fileInput.files[0];
        if (!file) {
            showToastHutang<%=rnd%>('<%=Common.getBahasaConfigJS("Pilih file Excel terlebih dahulu!")%>', 'bg-warning text-dark');
            return;
        }

        const btnProcess = document.getElementById('btnProsesUploadHutang<%=rnd%>');
        const statusBox = document.getElementById('uploadStatusBoxHutang<%=rnd%>');
        btnProcess.disabled = true;
        btnProcess.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Membaca File...")%>';
        statusBox.style.display = 'block';
        statusBox.className = 'alert alert-info py-2 small';
        statusBox.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Mengekstrak data baris demi baris...';

        const reader = new FileReader();
        reader.onload = async (e) => {
            try {
                const data = new Uint8Array(e.target.result);
                const workbook = XLSX.read(data, {type: 'array'});
                const firstSheet = workbook.Sheets[workbook.SheetNames[0]];
                const excelRows = XLSX.utils.sheet_to_json(firstSheet, {defval: ""});

                if (excelRows.length === 0) {
                    throw new Error("File Excel Kosong atau format tidak sesuai.");
                }

                let berhasil = 0;
                let gagal = 0;
                const pesanGagal = [];

                for (let i = 0; i < excelRows.length; i++) {
                    const row = excelRows[i];
                    statusBox.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Memproses baris ' + (i + 1) + ' dari ' + excelRows.length + '...';

                    const kodeAnggota = row['KODE_ANGGOTA'] ? String(row['KODE_ANGGOTA']).trim() : '';
                    const nominal = parseFloat(row['NOMINAL']) || 0;
                    const waktu = row['WAKTU'] ? String(row['WAKTU']).trim() : '';

                    const idAnggota = await cariIdAnggotaByKodeHutang<%=rnd%>(kodeAnggota);
                    if (!idAnggota) {
                        gagal++;
                        pesanGagal.push('Baris ' + (i + 2) + ': kode anggota "' + kodeAnggota + '" tidak ditemukan.');
                        continue;
                    }
                    if (nominal <= 0) {
                        gagal++;
                        pesanGagal.push('Baris ' + (i + 2) + ': nominal kosong/nol.');
                        continue;
                    }

                    const dataObj = {
                        anggotaKoperasi: idAnggota,
                        nominal: nominal,
                        waktu: waktu ? waktu : undefined,
                        keterangan: row['KETERANGAN'] || ''
                    };

                    try {
                        const result = await fetchApiHutang<%=rnd%>({ action: "simpanDataRinci", class: masterClassBayarHutang<%=rnd%>, data: dataObj });
                        if (result.status === '00' || result.status === 'success' || result.id) {
                            berhasil++;
                        } else {
                            gagal++;
                            pesanGagal.push('Baris ' + (i + 2) + ': ' + (result.description || 'gagal disimpan.'));
                        }
                    } catch (errBaris) {
                        gagal++;
                        pesanGagal.push('Baris ' + (i + 2) + ': kesalahan koneksi.');
                    }
                }

                if (gagal === 0) {
                    statusBox.className = 'alert alert-success py-2 small fw-bold';
                    statusBox.innerHTML = '<i class="fas fa-check-circle me-2"></i>' + berhasil + ' baris berhasil disimpan!';
                } else {
                    statusBox.className = 'alert alert-warning py-2 small';
                    statusBox.innerHTML = '<i class="fas fa-exclamation-triangle me-2"></i>' + berhasil + ' berhasil, ' + gagal + ' gagal.<br>' +
                        pesanGagal.slice(0, 5).join('<br>') + (pesanGagal.length > 5 ? '<br>... dan ' + (pesanGagal.length - 5) + ' lainnya.' : '');
                }

                showToastHutang<%=rnd%>('<%=Common.getBahasaConfigJS("Upload selesai.")%>', gagal === 0 ? 'bg-success text-white' : 'bg-warning text-dark');
                resetAndLoadHutang<%=rnd%>();
                if (gagal === 0) {
                    setTimeout(() => { modalInstanceUploadHutang<%=rnd%>.hide(); }, 1500);
                }
            } catch (error) {
                console.error("Upload Error:", error);
                statusBox.className = 'alert alert-danger py-2 small';
                statusBox.innerHTML = '<i class="fas fa-exclamation-triangle me-2"></i>Terjadi Kesalahan: ' + error.message;
            } finally {
                btnProcess.disabled = false;
                btnProcess.innerHTML = '<i class="fas fa-cogs me-2"></i><%=Common.getBahasaConfig("Proses Data")%>';
            }
        };
        reader.readAsArrayBuffer(file);
    };

    // ==========================================
    // CETAK PDF (print-friendly popup window)
    // ==========================================
    const cetakPdfHutang<%=rnd%> = () => {
        if (dataMutasiHutangLengkap<%=rnd%>.length === 0) {
            showToastHutang<%=rnd%>('<%=Common.getBahasaConfigJS("Tidak ada data untuk dicetak. Silakan saring data terlebih dahulu.")%>', 'bg-warning text-dark');
            return;
        }
        const tglAwal = document.getElementById('searchDateStartHutang<%=rnd%>').value;
        const tglAkhir = document.getElementById('searchDateEndHutang<%=rnd%>').value;

        let baris = '';
        dataMutasiHutangLengkap<%=rnd%>.forEach(r => {
            baris += '<tr>' +
                '<td>' + (r.nama_anggota || '-') + '</td>' +
                '<td>' + formatTanggalHutang<%=rnd%>(r.waktu) + '</td>' +
                '<td>' + (r.jenis_mutasi || '') + '</td>' +
                '<td>' + (r.keterangan || '') + '</td>' +
                '<td style="text-align:right;">' + (r.bertambah > 0 ? formatRpHutang<%=rnd%>(r.bertambah) : '-') + '</td>' +
                '<td style="text-align:right;">' + (r.berkurang > 0 ? formatRpHutang<%=rnd%>(r.berkurang) : '-') + '</td>' +
                '<td style="text-align:right;">' + formatRpHutang<%=rnd%>(r.saldo_per_anggota) + '</td>' +
                '</tr>';
        });

        const html = '<!DOCTYPE html><html><head><meta charset="utf-8"><title>Mutasi Hutang</title>' +
            '<style>body{font-family:Arial,sans-serif;font-size:11px;padding:16px;} h3{margin-bottom:4px;} p{margin-top:0;color:#555;} ' +
            'table{width:100%;border-collapse:collapse;} th,td{border:1px solid #ccc;padding:5px 7px;} th{background:#f0f0f0;text-align:left;}</style>' +
            '</head><body>' +
            '<h3><%=Common.getBahasaConfigJS("Mutasi Hutang (Piutang Toko)")%></h3>' +
            '<p>' + tglAwal + ' s/d ' + tglAkhir + '</p>' +
            '<table><thead><tr>' +
            '<th><%=Common.getBahasaConfigJS("Nama")%></th><th><%=Common.getBahasaConfigJS("Tanggal")%></th>' +
            '<th><%=Common.getBahasaConfigJS("Jenis Mutasi")%></th><th><%=Common.getBahasaConfigJS("Keterangan")%></th>' +
            '<th><%=Common.getBahasaConfigJS("Hutang Bertambah")%></th><th><%=Common.getBahasaConfigJS("Pembayaran")%></th>' +
            '<th><%=Common.getBahasaConfigJS("Saldo Anggota")%></th>' +
            '</tr></thead><tbody>' + baris + '</tbody></table>' +
            '<script>window.onload = function(){ window.print(); };<' + '/script>' +
            '</body></html>';

        const jendela = window.open('', '_blank');
        jendela.document.open();
        jendela.document.write(html);
        jendela.document.close();
    };

    // ==========================================
    // INISIALISASI
    // ==========================================
    document.addEventListener("DOMContentLoaded", () => {
        const rekap = document.getElementById('rekapMutasiHutang<%=rnd%>');
        const detail = document.getElementById('detailMutasiHutang<%=rnd%>');
        detail.parentNode.insertBefore(rekap, detail);
        setDefaultDatesHutang<%=rnd%>();
        pasangSearchAnggota<%=rnd%>('inputCariAnggotaHutang<%=rnd%>', 'listAnggotaHutang<%=rnd%>', 'idAnggotaHutangTerpilih<%=rnd%>');
        pasangSearchAnggota<%=rnd%>('inputCariAnggotaBayarHutang<%=rnd%>', 'listAnggotaBayarHutang<%=rnd%>', 'idAnggotaBayarHutangTerpilih<%=rnd%>');

        const filterInputs = document.querySelectorAll('.filter-hutang-<%=rnd%>');
        filterInputs.forEach(input => {
            input.addEventListener("keydown", (event) => {
                if (event.key === "Enter") {
                    event.preventDefault();
                    resetAndLoadHutang<%=rnd%>();
                }
            });
        });
        loadDataHutang<%=rnd%>();
    });
</script>
