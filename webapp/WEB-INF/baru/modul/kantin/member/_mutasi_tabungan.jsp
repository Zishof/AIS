<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// Layar READ-ONLY (tidak ada create/edit/delete) -- murni laporan buku besar hasil UNION 3 sumber
// mutasi saldo anggota koperasi: (1) public.deposit (topup & koreksi manual -- nominal boleh negatif,
// lihat _manajemen_topup.jsp), (2) koperasi.pembelian yang caraPembayaranKoperasi-nya memotong saldo
// (manual=false ATAU memotong_deposit=true, PERSIS syarat yg dipakai DepositHelper.hitungDeposit), dan
// (3) koperasi.pencairan_diskon berstatus BERHASIL (cashback). Saldo berjalan dihitung window function
// SQL, BUKAN dihitung ulang di Java/JS -- supaya angka "Saldo Per Penabung"/"Saldo Total" identik dgn
// urutan baris yg ditampilkan. CATATAN: ini APROKSIMASI histori (spt "gl_rincian" di laporan_laporan.jsp)
// -- penyesuaian voucher/cashback kedaluwarsa (lihat "totalHangus" di DepositHelper) bukan baris
// transaksi diskrit, jadi tidak ikut muncul sbg baris mutasi tersendiri di sini.
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    out.print("<div class='text-center p-5'><i class='fas fa-exclamation-triangle fa-3x text-warning mb-3'></i><br><h5 class='text-muted'>" + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "</h5></div>");
    return;
}
String rnd = Common.getGeneratedBarCode(7);
%>

<div class="container-fluid py-3 py-md-4 animate__animated animate__fadeIn" id="mutasiTabunganContent<%=rnd%>">

    <!-- HEADER -->
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-center mb-4 gap-3">
        <div class="text-center text-md-start">
            <h4 class="fw-bolder text-primary mb-1">
                <i class="fas fa-book me-2"></i><%=Common.getBahasaConfig("Mutasi Tabungan (Buku Besar)")%>
            </h4>
            <p class="text-muted small mb-0"><%=Common.getBahasaConfig("Urutan mutasi tabungan anggota: uang masuk, uang keluar, saldo per penabung, dan saldo total berjalan.")%></p>
        </div>
    </div>

    <!-- FILTER SECTION -->
    <div class="card border-0 shadow-sm rounded-4 mb-4 border-top border-primary border-3">
        <div class="card-body p-3 p-md-4">
            <div class="row g-3 align-items-end">
                <div class="col-md-4">
                    <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Anggota (Opsional, kosongkan utk semua)")%></label>
                    <div class="input-group shadow-sm">
                        <span class="input-group-text bg-white border-end-0 text-muted"><i class="fas fa-user"></i></span>
                        <input type="text" class="form-control border-start-0 fw-medium filter-mutasi-<%=rnd%>" list="listAnggotaMutasi<%=rnd%>" id="inputCariAnggotaMutasi<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik kode / nama anggota...")%>" autocomplete="off">
                    </div>
                    <datalist id="listAnggotaMutasi<%=rnd%>"></datalist>
                    <input type="hidden" id="idAnggotaMutasiTerpilih<%=rnd%>" value="">
                </div>
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Tanggal Mulai")%></label>
                    <input type="date" class="form-control fw-medium shadow-sm filter-mutasi-<%=rnd%>" id="searchDateStartMutasi<%=rnd%>">
                </div>
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Tanggal Akhir")%></label>
                    <input type="date" class="form-control fw-medium shadow-sm filter-mutasi-<%=rnd%>" id="searchDateEndMutasi<%=rnd%>">
                </div>
                <div class="col-md-2 d-grid">
                    <button class="btn btn-primary rounded-3 shadow-sm fw-bold" onclick="resetAndLoadMutasi<%=rnd%>()">
                        <i class="fas fa-filter me-2"></i><%=Common.getBahasaConfig("Saring")%>
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- DETAIL LEDGER -->
    <div class="card border-0 shadow-sm rounded-4 overflow-hidden mb-4">
        <div class="card-header bg-white border-0 pt-4 pb-2 px-4">
            <h6 class="fw-bold text-dark mb-0"><%=Common.getBahasaConfig("Buku Besar Mutasi Tabungan")%></h6>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive px-3 py-2" style="min-height: 300px;">
                <table class="table table-hover align-middle mb-0 small">
                    <thead class="table-light text-secondary">
                        <tr>
                            <th class="fw-bold text-uppercase small"><%=Common.getBahasaConfig("Nama")%></th>
                            <th class="fw-bold text-uppercase small"><%=Common.getBahasaConfig("Tanggal")%></th>
                            <th class="fw-bold text-uppercase small"><%=Common.getBahasaConfig("Jenis Mutasi")%></th>
                            <th class="fw-bold text-uppercase small"><%=Common.getBahasaConfig("Keterangan")%></th>
                            <th class="fw-bold text-uppercase small text-end"><%=Common.getBahasaConfig("Masuk (Debit)")%></th>
                            <th class="fw-bold text-uppercase small text-end"><%=Common.getBahasaConfig("Keluar (Kredit)")%></th>
                            <th class="fw-bold text-uppercase small text-end"><%=Common.getBahasaConfig("Saldo Per Penabung")%></th>
                            <th class="fw-bold text-uppercase small text-end"><%=Common.getBahasaConfig("Saldo Total")%></th>
                        </tr>
                    </thead>
                    <tbody id="tabelDataMutasi<%=rnd%>">
                        <tr>
                            <td colspan="8" class="text-center py-5">
                                <div class="spinner-border text-primary mb-3"></div>
                                <br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Sistem sedang memuat data mutasi...")%></span>
                            </td>
                        </tr>
                    </tbody>
                    <tfoot id="tabelFooterMutasi<%=rnd%>"></tfoot>
                </table>
            </div>

            <!-- PAGING -->
            <div class="bg-light p-3 border-top d-flex flex-column flex-md-row justify-content-between align-items-center gap-3">
                <div class="small fw-bold text-muted" id="pagingInfoMutasi<%=rnd%>"></div>
                <div class="d-flex align-items-center bg-white rounded-pill shadow-sm border p-1" id="pagingContainerMutasi<%=rnd%>">
                    <button class="btn btn-sm btn-light rounded-pill px-3 fw-bold text-primary" id="btnPrevPageMutasi<%=rnd%>" onclick="changePageMutasi<%=rnd%>(-1)">
                        <i class="fas fa-chevron-left me-1"></i><span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Sebelumnya")%></span>
                    </button>
                    <div class="px-3 fw-bold text-dark small">
                        <span id="lblCurrentPageMutasi<%=rnd%>">1</span> / <span id="lblTotalPageMutasi<%=rnd%>">1</span>
                    </div>
                    <button class="btn btn-sm btn-light rounded-pill px-3 fw-bold text-primary" id="btnNextPageMutasi<%=rnd%>" onclick="changePageMutasi<%=rnd%>(1)">
                        <span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Selanjutnya")%></span><i class="fas fa-chevron-right ms-1"></i>
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- SUMMARY PER ANGGOTA -->
    <div class="card border-0 shadow-sm rounded-4 overflow-hidden">
        <div class="card-header bg-white border-0 pt-4 pb-2 px-4">
            <h6 class="fw-bold text-dark mb-0"><%=Common.getBahasaConfig("Summary Buku Besar Mutasi Tabungan")%></h6>
            <small class="text-muted"><%=Common.getBahasaConfig("Mutasi digabung per penabung agar saldo setiap orang lebih cepat diketahui.")%></small>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive px-3 py-2">
                <table class="table table-hover align-middle mb-0 small">
                    <thead class="table-light text-secondary">
                        <tr>
                            <th class="fw-bold text-uppercase small"><%=Common.getBahasaConfig("Nama")%></th>
                            <th class="fw-bold text-uppercase small text-end"><%=Common.getBahasaConfig("Sum Debit")%></th>
                            <th class="fw-bold text-uppercase small text-end"><%=Common.getBahasaConfig("Sum Kredit")%></th>
                            <th class="fw-bold text-uppercase small text-end"><%=Common.getBahasaConfig("Sum Saldo")%></th>
                        </tr>
                    </thead>
                    <tbody id="tabelSummaryMutasi<%=rnd%>">
                        <tr><td colspan="4" class="text-center py-4 text-muted"><%=Common.getBahasaConfig("Memuat...")%></td></tr>
                    </tbody>
                    <tfoot id="tabelSummaryFooterMutasi<%=rnd%>"></tfoot>
                </table>
            </div>
        </div>
    </div>

</div>

<script>
    // ==========================================
    // VARIABEL GLOBAL
    // ==========================================
    let currentPageMutasi<%=rnd%> = 1;
    const limitPerPageMutasi<%=rnd%> = 10;
    let dataMutasiLengkap<%=rnd%> = []; // seluruh baris hasil filter (paging dilakukan di client, spt Pesanan/Produk)
    let timerPencarianAnggotaMutasi<%=rnd%> = null;

    const formatRpMutasi<%=rnd%> = (angka) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);

    const fetchApiMutasi<%=rnd%> = async (payload) => {
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

    const setDefaultDatesMutasi<%=rnd%> = () => {
        const today = new Date();
        const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
        const todayStr = today.getFullYear() + '-' + String(today.getMonth() + 1).padStart(2, '0') + '-' + String(today.getDate()).padStart(2, '0');
        const firstDayStr = firstDay.getFullYear() + '-' + String(firstDay.getMonth() + 1).padStart(2, '0') + '-01';
        document.getElementById('searchDateEndMutasi<%=rnd%>').value = todayStr;
        document.getElementById('searchDateStartMutasi<%=rnd%>').value = firstDayStr;
    };

    // ==========================================
    // LIVE SEARCH ANGGOTA (opsional, filter satu penabung)
    // ==========================================
    const tarikDataAnggotaMutasi<%=rnd%> = async (keyword) => {
        const safeKeyword = keyword.replace(/'/g, "''");
        const sql = "SELECT id, kode, nama FROM koperasi.anggota_koperasi " +
                    "WHERE (nama ILIKE '%" + safeKeyword + "%' OR kode ILIKE '%" + safeKeyword + "%') " +
                    "ORDER BY nama ASC LIMIT 50";
        const res = await fetchApiMutasi<%=rnd%>({ action: "sql", sql: sql });
        let html = '';
        (res.data || []).forEach(item => {
            html += '<option value="' + item.kode + ' - ' + item.nama + '" data-id="' + item.id + '">';
        });
        document.getElementById('listAnggotaMutasi<%=rnd%>').innerHTML = html;
    };

    const inputCariMutasi<%=rnd%> = document.getElementById('inputCariAnggotaMutasi<%=rnd%>');
    inputCariMutasi<%=rnd%>.addEventListener('input', function() {
        const keyword = this.value.trim();
        if (keyword === '') { document.getElementById('idAnggotaMutasiTerpilih<%=rnd%>').value = ''; return; }
        const isMatch = Array.from(document.getElementById('listAnggotaMutasi<%=rnd%>').options).some(opt => opt.value === keyword);
        if (!isMatch) {
            clearTimeout(timerPencarianAnggotaMutasi<%=rnd%>);
            timerPencarianAnggotaMutasi<%=rnd%> = setTimeout(() => tarikDataAnggotaMutasi<%=rnd%>(keyword), 500);
        }
    });
    inputCariMutasi<%=rnd%>.addEventListener('change', function() {
        const val = this.value;
        let matchId = '';
        Array.from(document.getElementById('listAnggotaMutasi<%=rnd%>').childNodes).forEach(opt => {
            if (opt.value === val) matchId = opt.getAttribute('data-id');
        });
        document.getElementById('idAnggotaMutasiTerpilih<%=rnd%>').value = matchId;
    });

    // ==========================================
    // BUILD & JALANKAN QUERY LEDGER (UNION 3 sumber + window function saldo berjalan)
    // ==========================================
    const resetAndLoadMutasi<%=rnd%> = () => {
        currentPageMutasi<%=rnd%> = 1;
        loadDataMutasi<%=rnd%>();
    };

    const loadDataMutasi<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataMutasi<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="8" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Sistem sedang memuat data mutasi...")%></span></td></tr>';
        document.getElementById('tabelFooterMutasi<%=rnd%>').innerHTML = '';
        document.getElementById('tabelSummaryMutasi<%=rnd%>').innerHTML = '<tr><td colspan="4" class="text-center py-4 text-muted"><%=Common.getBahasaConfig("Memuat...")%></td></tr>';
        document.getElementById('tabelSummaryFooterMutasi<%=rnd%>').innerHTML = '';

        const tglAwal = document.getElementById('searchDateStartMutasi<%=rnd%>').value;
        const tglAkhir = document.getElementById('searchDateEndMutasi<%=rnd%>').value;
        const idAnggota = document.getElementById('idAnggotaMutasiTerpilih<%=rnd%>').value;

        if (tglAwal === '' || tglAkhir === '') {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-warning py-5"><i class="fas fa-calendar-times fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Tanggal Mulai dan Tanggal Akhir wajib diisi.")%></td></tr>';
            return;
        }

        const rentang = "'" + tglAwal + " 00:00:00' AND '" + tglAkhir + " 23:59:59'";
        const filterAnggota = idAnggota !== '' ? (" AND anggota_koperasi = " + parseInt(idAnggota, 10) + " ") : "";

        const sql =
            "WITH mutasi AS ( " +
            "  SELECT d.waktu AS waktu, ('D' || d.id) AS baris_id, (a.kode || ' - ' || a.nama) AS nama_anggota, d.anggota_koperasi AS id_anggota, " +
            "         CASE WHEN d.nominal >= 0 THEN 'Topup Tabungan' ELSE 'Pengeluaran Manual' END AS jenis_mutasi, " +
            "         COALESCE(d.keterangan, '') AS keterangan, GREATEST(d.nominal, 0) AS masuk, GREATEST(-d.nominal, 0) AS keluar " +
            "  FROM public.deposit d JOIN koperasi.anggota_koperasi a ON d.anggota_koperasi = a.id " +
            "  WHERE d.anggota_koperasi IS NOT NULL AND d.waktu BETWEEN " + rentang + filterAnggota.replace(/anggota_koperasi/g, "d.anggota_koperasi") +
            "  UNION ALL " +
            "  SELECT p.waktu, ('P' || p.id), (a.kode || ' - ' || a.nama), p.anggota_koperasi, " +
            "         'Pembelian/Belanja' AS jenis_mutasi, COALESCE(p.kode, '') AS keterangan, 0 AS masuk, COALESCE(p.total, 0) AS keluar " +
            "  FROM koperasi.pembelian p " +
            "  JOIN koperasi.anggota_koperasi a ON p.anggota_koperasi = a.id " +
            "  JOIN koperasi.cara_pembayaran_koperasi cpk ON p.cara_pembayaran_koperasi = cpk.id " +
            "  WHERE p.anggota_koperasi IS NOT NULL AND (cpk.manual = false OR cpk.memotong_deposit = true) " +
            "  AND p.waktu BETWEEN " + rentang + filterAnggota.replace(/anggota_koperasi/g, "p.anggota_koperasi") +
            "  UNION ALL " +
            "  SELECT pc.waktu_pencairan, ('C' || pc.id), (a.kode || ' - ' || a.nama), pc.anggota_koperasi, " +
            "         'Cashback Diskon' AS jenis_mutasi, 'Pencairan diskon' AS keterangan, COALESCE(pc.nominal_cair, 0) AS masuk, 0 AS keluar " +
            "  FROM koperasi.pencairan_diskon pc " +
            "  JOIN koperasi.anggota_koperasi a ON pc.anggota_koperasi = a.id " +
            "  JOIN koperasi.cara_pembayaran_koperasi cpk2 ON pc.cara_pembayaran = cpk2.id " +
            "  WHERE pc.status = 'BERHASIL' AND cpk2.manual = false " +
            "  AND pc.waktu_pencairan BETWEEN " + rentang + filterAnggota.replace(/anggota_koperasi/g, "pc.anggota_koperasi") +
            ") " +
            "SELECT *, " +
            "  SUM(masuk - keluar) OVER (PARTITION BY id_anggota ORDER BY waktu, baris_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS saldo_per_penabung, " +
            "  SUM(masuk - keluar) OVER (ORDER BY waktu, baris_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS saldo_total " +
            "FROM mutasi ORDER BY waktu ASC, baris_id ASC;";

        try {
            const res = await fetchApiMutasi<%=rnd%>({ action: "sql", sql: sql });
            dataMutasiLengkap<%=rnd%> = res.data || [];
            renderTabelMutasi<%=rnd%>();
            renderSummaryMutasi<%=rnd%>();
        } catch (error) {
            console.error("Gagal load mutasi tabungan:", error);
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3"></i><br><%=Common.getBahasaConfig("Gagal menarik data dari peladen.")%></td></tr>';
        }
    };

    const badgeJenisMutasi<%=rnd%> = (jenis) => {
        const peta = {
            'Topup Tabungan': 'success',
            'Pengeluaran Manual': 'danger',
            'Pembelian/Belanja': 'warning',
            'Cashback Diskon': 'info'
        };
        const warna = peta[jenis] || 'secondary';
        return '<span class="badge bg-' + warna + ' bg-opacity-25 text-' + warna + ' border border-' + warna + ' px-2 py-1">' + jenis + '</span>';
    };

    const formatTanggalMutasi<%=rnd%> = (iso) => {
        if (!iso) return '-';
        const d = new Date(iso);
        if (isNaN(d.getTime())) return iso;
        const bulan = ['Jan','Feb','Mar','Apr','Mei','Jun','Jul','Agu','Sep','Okt','Nov','Des'];
        const pad = (n) => String(n).padStart(2, '0');
        return pad(d.getDate()) + ' ' + bulan[d.getMonth()] + ' ' + d.getFullYear() + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
    };

    const renderTabelMutasi<%=rnd%> = () => {
        const tbody = document.getElementById('tabelDataMutasi<%=rnd%>');
        const tfoot = document.getElementById('tabelFooterMutasi<%=rnd%>');
        const total = dataMutasiLengkap<%=rnd%>.length;

        if (total === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-5"><i class="fas fa-book fa-3x mb-3 opacity-25"></i><br><%=Common.getBahasaConfig("Tidak ada mutasi pada rentang ini.")%></td></tr>';
            updatePagingUIMutasi<%=rnd%>();
            return;
        }

        const startIdx = (currentPageMutasi<%=rnd%> - 1) * limitPerPageMutasi<%=rnd%>;
        const pageRows = dataMutasiLengkap<%=rnd%>.slice(startIdx, startIdx + limitPerPageMutasi<%=rnd%>);

        let html = '';
        pageRows.forEach((row, idx) => {
            html += '<tr>' +
                '<td class="fw-bold text-dark">' + (row.nama_anggota || '-') + '</td>' +
                '<td class="text-secondary">' + formatTanggalMutasi<%=rnd%>(row.waktu) + '</td>' +
                '<td>' + badgeJenisMutasi<%=rnd%>(row.jenis_mutasi) + '</td>' +
                '<td class="text-muted">' + (row.keterangan || '') + '</td>' +
                '<td class="text-end fw-bold' + (row.masuk > 0 ? ' text-success' : ' text-muted') + '">' + (row.masuk > 0 ? formatRpMutasi<%=rnd%>(row.masuk) : '-') + '</td>' +
                '<td class="text-end fw-bold' + (row.keluar > 0 ? ' text-danger' : ' text-muted') + '">' + (row.keluar > 0 ? formatRpMutasi<%=rnd%>(row.keluar) : '-') + '</td>' +
                '<td class="text-end fw-bolder text-primary">' + formatRpMutasi<%=rnd%>(row.saldo_per_penabung) + '</td>' +
                '<td class="text-end fw-bold text-dark">' + formatRpMutasi<%=rnd%>(row.saldo_total) + '</td>' +
                '</tr>';
        });
        tbody.innerHTML = html;

        const totalMasuk = dataMutasiLengkap<%=rnd%>.reduce((s, r) => s + (parseFloat(r.masuk) || 0), 0);
        const totalKeluar = dataMutasiLengkap<%=rnd%>.reduce((s, r) => s + (parseFloat(r.keluar) || 0), 0);
        const saldoAkhir = dataMutasiLengkap<%=rnd%>[total - 1].saldo_total;
        tfoot.innerHTML = '<tr class="table-light fw-bold">' +
            '<td colspan="4" class="text-end"><%=Common.getBahasaConfig("TOTAL")%> :</td>' +
            '<td class="text-end text-success">' + formatRpMutasi<%=rnd%>(totalMasuk) + '</td>' +
            '<td class="text-end text-danger">' + formatRpMutasi<%=rnd%>(totalKeluar) + '</td>' +
            '<td class="text-end text-muted">&ndash;</td>' +
            '<td class="text-end text-dark">' + formatRpMutasi<%=rnd%>(saldoAkhir) + '</td>' +
            '</tr>';

        updatePagingUIMutasi<%=rnd%>();
    };

    const renderSummaryMutasi<%=rnd%> = () => {
        const tbody = document.getElementById('tabelSummaryMutasi<%=rnd%>');
        const tfoot = document.getElementById('tabelSummaryFooterMutasi<%=rnd%>');

        if (dataMutasiLengkap<%=rnd%>.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-4"><%=Common.getBahasaConfig("Tidak ada data.")%></td></tr>';
            return;
        }

        // Group per anggota, urutan kemunculan pertama (spt Screenshot: kronologis, bukan alfabet)
        const peta = new Map();
        dataMutasiLengkap<%=rnd%>.forEach(row => {
            const key = row.id_anggota;
            if (!peta.has(key)) peta.set(key, { nama: row.nama_anggota, debit: 0, kredit: 0 });
            const rec = peta.get(key);
            rec.debit += parseFloat(row.masuk) || 0;
            rec.kredit += parseFloat(row.keluar) || 0;
        });

        let html = '';
        let totalDebit = 0, totalKredit = 0;
        peta.forEach(rec => {
            const saldo = rec.debit - rec.kredit;
            totalDebit += rec.debit;
            totalKredit += rec.kredit;
            html += '<tr>' +
                '<td class="fw-bold text-dark">' + rec.nama + '</td>' +
                '<td class="text-end text-success">' + formatRpMutasi<%=rnd%>(rec.debit) + '</td>' +
                '<td class="text-end text-danger">' + formatRpMutasi<%=rnd%>(rec.kredit) + '</td>' +
                '<td class="text-end fw-bolder ' + (saldo >= 0 ? 'text-primary' : 'text-danger') + '">' + formatRpMutasi<%=rnd%>(saldo) + '</td>' +
                '</tr>';
        });
        tbody.innerHTML = html;

        tfoot.innerHTML = '<tr class="table-light fw-bold">' +
            '<td class="text-end"><%=Common.getBahasaConfig("TOTAL")%> :</td>' +
            '<td class="text-end text-success">' + formatRpMutasi<%=rnd%>(totalDebit) + '</td>' +
            '<td class="text-end text-danger">' + formatRpMutasi<%=rnd%>(totalKredit) + '</td>' +
            '<td class="text-end text-dark">' + formatRpMutasi<%=rnd%>(totalDebit - totalKredit) + '</td>' +
            '</tr>';
    };

    const updatePagingUIMutasi<%=rnd%> = () => {
        const total = dataMutasiLengkap<%=rnd%>.length;
        const totalPages = Math.ceil(total / limitPerPageMutasi<%=rnd%>) || 1;
        document.getElementById('lblCurrentPageMutasi<%=rnd%>').innerText = currentPageMutasi<%=rnd%>;
        document.getElementById('lblTotalPageMutasi<%=rnd%>').innerText = totalPages;

        const startIdx = total > 0 ? ((currentPageMutasi<%=rnd%> - 1) * limitPerPageMutasi<%=rnd%> + 1) : 0;
        const endIdx = Math.min(currentPageMutasi<%=rnd%> * limitPerPageMutasi<%=rnd%>, total);
        document.getElementById('pagingInfoMutasi<%=rnd%>').innerHTML = '<%=Common.getBahasaConfigJS("Menampilkan")%> <span class="text-dark">' + startIdx + ' - ' + endIdx + '</span> <%=Common.getBahasaConfig("dari")%> <span class="text-dark">' + total + '</span> <%=Common.getBahasaConfig("mutasi")%>';

        document.getElementById('btnPrevPageMutasi<%=rnd%>').disabled = (currentPageMutasi<%=rnd%> === 1);
        document.getElementById('btnNextPageMutasi<%=rnd%>').disabled = (currentPageMutasi<%=rnd%> === totalPages);
    };

    const changePageMutasi<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(dataMutasiLengkap<%=rnd%>.length / limitPerPageMutasi<%=rnd%>);
        if (direction === -1 && currentPageMutasi<%=rnd%> > 1) {
            currentPageMutasi<%=rnd%>--;
            renderTabelMutasi<%=rnd%>();
        } else if (direction === 1 && currentPageMutasi<%=rnd%> < totalPages) {
            currentPageMutasi<%=rnd%>++;
            renderTabelMutasi<%=rnd%>();
        }
    };

    // ==========================================
    // INISIALISASI
    // ==========================================
    document.addEventListener("DOMContentLoaded", () => {
        setDefaultDatesMutasi<%=rnd%>();
        const filterInputs = document.querySelectorAll('.filter-mutasi-<%=rnd%>');
        filterInputs.forEach(input => {
            input.addEventListener("keydown", (event) => {
                if (event.key === "Enter") {
                    event.preventDefault();
                    resetAndLoadMutasi<%=rnd%>();
                }
            });
        });
        loadDataMutasi<%=rnd%>();
    });
</script>
