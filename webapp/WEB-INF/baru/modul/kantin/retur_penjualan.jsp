<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// 1. SECURITY CHECK & ROLE IDENTIFICATION
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

// Aturan: Admin bisa semua toko. Pedagang HANYA tokonya sendiri. TIDAK digerbang Supervisor (beda
// dari Kulakan) -- retur pelanggan adalah tugas rutin kasir sehari-hari, lihat JavaDoc
// KantinHelper.returPenjualanSimpan.
boolean isAdmin = (toko == null);
String idTokoAktif = toko != null ? String.valueOf(toko.getId()) : "";
String namaTokoAktif = toko != null ? toko.getNama() : "";
%>

<div id="viewList<%=rnd%>" class="animate__animated animate__fadeInUp">
    <div class="row mb-4">
        <div class="col-12">
            <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
                <div class="card-header bg-white border-0 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-3 border-bottom border-light">
                    <div>
                        <h5 class="fw-bold text-dark mb-1">
                            <i class="fas fa-undo-alt text-primary me-2"></i>
                            <% if (isAdmin) { %>
                                <%=Common.getBahasaConfig("Retur Penjualan (Semua Toko)")%>
                            <% } else { %>
                                <%=Common.getBahasaConfig("Retur Penjualan - ")%> <span class="text-primary"><%=namaTokoAktif%></span>
                            <% } %>
                        </h5>
                        <small class="text-muted"><%=Common.getBahasaConfig("Catat barang yang dikembalikan pelanggan (rusak/salah/tidak sesuai) dari transaksi penjualan yang sudah dibayar.")%></small>
                    </div>
                    <button class="btn btn-primary rounded-pill px-4 fw-bold shadow-sm" onclick="bukaFormCariTransaksi<%=rnd%>()">
                        <i class="fas fa-search me-2"></i><%=Common.getBahasaConfig("Retur Baru (Cari Transaksi)")%>
                    </button>
                </div>

                <div class="card-body p-4">
                    <div class="bg-light p-3 rounded-3 border shadow-sm mb-4">
                        <div class="row g-2 align-items-end">
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Cari (Produk/Nomor Nota/Pembeli)")%></label>
                                <input type="text" class="form-control form-control-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterKeyword<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik lalu Enter...")%>">
                            </div>
                            <% if (isAdmin) { %>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Toko")%></label>
                                <select class="form-select form-select-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterToko<%=rnd%>">
                                    <option value=""><%=Common.getBahasaConfig("- Semua Toko -")%></option>
                                </select>
                            </div>
                            <% } else { %>
                                <input type="hidden" id="filterToko<%=rnd%>" value="<%=idTokoAktif%>">
                            <% } %>
                            <div class="col-md-auto ms-auto">
                                <button class="btn btn-sm btn-secondary fw-bold px-3 shadow-sm" onclick="triggerSearchRp<%=rnd%>()">
                                    <i class="fas fa-search me-1"></i> <%=Common.getBahasaConfig("Saring")%>
                                </button>
                            </div>
                        </div>
                    </div>

                    <div class="table-responsive border rounded-3 shadow-sm bg-white">
                        <table class="table table-hover table-striped align-middle mb-0">
                            <thead class="table-dark text-center align-middle">
                                <tr>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Waktu")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Nomor Nota Asal")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Produk")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Pembeli")%></th>
                                    <th class="fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Qty")%></th>
                                    <th class="fw-semibold text-uppercase small px-3 text-end"><%=Common.getBahasaConfig("Nilai Retur")%></th>
                                    <th class="fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Alasan")%></th>
                                    <th class="fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Balik ke Stok?")%></th>
                                </tr>
                            </thead>
                            <tbody id="tabelDataRp<%=rnd%>">
                                <tr><td colspan="8" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Memuat data retur...")%></span></td></tr>
                            </tbody>
                        </table>
                    </div>

                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mt-4">
                        <div class="small fw-medium text-muted bg-light px-3 py-1 rounded-pill border" id="pagingInfoRp<%=rnd%>"></div>
                        <nav>
                            <ul class="pagination pagination-sm mb-0 shadow-sm">
                                <li class="page-item"><button class="page-link text-primary rounded-start-pill px-3 fw-bold" id="btnPrevRp<%=rnd%>" onclick="changePageRp<%=rnd%>(-1)"><i class="fas fa-chevron-left me-1"></i><%=Common.getBahasaConfig("Sebelumnnya")%></button></li>
                                <li class="page-item disabled"><span class="page-link bg-light text-dark px-3 fw-bold" id="pageNumberRp<%=rnd%>">1</span></li>
                                <li class="page-item"><button class="page-link text-primary rounded-end-pill px-3 fw-bold" id="btnNextRp<%=rnd%>" onclick="changePageRp<%=rnd%>(1)"><%=Common.getBahasaConfig("Selanjutnya")%><i class="fas fa-chevron-right ms-1"></i></button></li>
                            </ul>
                        </nav>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- ==== Langkah 1: Cari Transaksi Asal ==== -->
<div id="viewCari<%=rnd%>" class="animate__animated animate__fadeIn" style="display: none;">
    <div class="row justify-content-center">
        <div class="col-lg-8">
            <div class="card border-0 shadow-sm rounded-4 border-top border-info border-4">
                <div class="card-header bg-white border-0 pt-4 pb-3 px-4">
                    <h5 class="fw-bold text-dark mb-0"><i class="fas fa-receipt text-info me-2"></i><%=Common.getBahasaConfig("Langkah 1: Cari Transaksi Penjualan Asal")%></h5>
                    <small class="text-muted"><%=Common.getBahasaConfig("Cari struk/nota transaksi tempat barang yang akan diretur pernah dibeli.")%></small>
                </div>
                <div class="card-body p-4">
                    <div class="input-group shadow-sm mb-3">
                        <span class="input-group-text bg-light border-end-0"><i class="fas fa-search text-muted"></i></span>
                        <input type="text" class="form-control" id="cariNotaAsal<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Nomor nota / nama pembeli...")%>">
                        <button class="btn btn-primary fw-bold" onclick="cariTransaksiAsal<%=rnd%>()"><%=Common.getBahasaConfig("Cari")%></button>
                    </div>
                    <div id="hasilCariTransaksi<%=rnd%>"></div>
                    <div class="text-end border-top pt-3 mt-3">
                        <button type="button" class="btn btn-light px-4 rounded-pill fw-semibold border shadow-sm" onclick="tutupSemuaForm<%=rnd%>()"><i class="fas fa-times text-danger me-2"></i><%=Common.getBahasaConfig("Batal")%></button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- ==== Langkah 2: Pilih Barang & Detail Retur ==== -->
<div id="viewForm<%=rnd%>" class="animate__animated animate__fadeIn" style="display: none;">
    <div class="row justify-content-center">
        <div class="col-lg-9">
            <div class="card border-0 shadow-sm rounded-4 border-top border-success border-4">
                <div class="card-header bg-white border-0 pt-4 pb-3 px-4">
                    <h5 class="fw-bold text-dark mb-0"><i class="fas fa-box-open text-success me-2"></i><%=Common.getBahasaConfig("Langkah 2: Pilih Barang yang Diretur")%></h5>
                    <small class="text-muted"><%=Common.getBahasaConfig("Transaksi asal: ")%><b id="labelTransaksiAsal<%=rnd%>"></b></small>
                </div>
                <div class="card-body p-4">
                    <input type="hidden" id="idTransaksiAsal<%=rnd%>" value="">
                    <input type="hidden" id="kodeTransaksiAsal<%=rnd%>" value="">
                    <input type="hidden" id="namaPembeliAsal<%=rnd%>" value="">

                    <div class="table-responsive border rounded-3 shadow-sm bg-white mb-3">
                        <table class="table table-hover align-middle mb-0" id="tabelItemAsal<%=rnd%>">
                        </table>
                    </div>

                    <div class="row g-3">
                        <div class="col-md-6">
                            <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Metode Pengembalian")%></label>
                            <select class="form-select shadow-sm" id="metodePengembalian<%=rnd%>">
                                <option value="Tunai"><%=Common.getBahasaConfig("Tunai")%></option>
                                <option value="Saldo Member"><%=Common.getBahasaConfig("Saldo Member")%></option>
                                <option value="Tukar Barang"><%=Common.getBahasaConfig("Tukar Barang")%></option>
                                <option value="Tanpa Pengembalian"><%=Common.getBahasaConfig("Tanpa Pengembalian (Hanya Catatan)")%></option>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Total Nilai Retur")%></label>
                            <input type="text" class="form-control fw-bold text-success shadow-sm" id="totalNilaiRetur<%=rnd%>" value="Rp 0" readonly>
                        </div>
                    </div>

                    <div class="text-end border-top pt-3 mt-4">
                        <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold border shadow-sm" onclick="tutupSemuaForm<%=rnd%>()"><i class="fas fa-times text-danger me-2"></i><%=Common.getBahasaConfig("Batal")%></button>
                        <button type="button" class="btn btn-success px-4 rounded-pill shadow-sm fw-bold" id="btnSimpanRp<%=rnd%>" onclick="simpanRetur<%=rnd%>()">
                            <i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Retur")%>
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    // ==========================================
    // BAGIAN 1: UTILITIES & STATE GLOBAL
    // ==========================================
    const isAdminRp<%=rnd%> = <%=isAdmin%>;
    const idTokoLoginRp<%=rnd%> = '<%=idTokoAktif%>';

    let currentPageRp<%=rnd%> = 1;
    const limitPerPageRp<%=rnd%> = 15;
    let totalRecordsRp<%=rnd%> = 0;

    const ALASAN_RETUR<%=rnd%> = ['Rusak', 'Salah Ukuran/Varian', 'Tidak Sesuai Pesanan', 'Berubah Pikiran', 'Kadaluarsa', 'Lainnya'];
    const KONDISI_BARANG<%=rnd%> = ['Baik (Layak Jual Lagi)', 'Rusak (Tidak Layak Jual)'];

    const fetchData<%=rnd%> = async (sql) => {
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sql: sql, action: "sql" })
            });
            const result = await res.json();
            return result.data || [];
        } catch (e) { console.error("Fetch SQL Error:", e); return []; }
    };

    const panggilAksi<%=rnd%> = async (action, payload) => {
        const body = Object.assign({ action: action }, payload || {});
        const res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        return await res.json();
    };

    const showToast<%=rnd%> = (msg, colorClass) => {
        if (typeof tampilkanToast === "function") { tampilkanToast(msg, colorClass); } else { alert(msg); }
    };

    const formatRp<%=rnd%> = (n) => 'Rp ' + Math.round(Number(n) || 0).toLocaleString('id-ID');

    // ==========================================
    // INIT COMBO TOKO (admin saja)
    // ==========================================
    const loadComboTokoRp<%=rnd%> = async () => {
        if (isAdminRp<%=rnd%>) {
            const resToko = await fetchData<%=rnd%>("SELECT id, nama FROM koperasi.toko WHERE aktif = true ORDER BY nama ASC");
            let opt = '<option value=""><%=Common.getBahasaConfig("- Semua Toko -")%></option>';
            resToko.forEach(t => { opt += '<option value="' + t.id + '">' + t.nama + '</option>'; });
            document.getElementById('filterToko<%=rnd%>').innerHTML = opt;
        }
    };

    // ==========================================
    // BAGIAN 2: RIWAYAT RETUR (READ)
    // ==========================================
    const triggerSearchRp<%=rnd%> = () => { currentPageRp<%=rnd%> = 1; loadDataRp<%=rnd%>(); };

    const changePageRp<%=rnd%> = (dir) => {
        const totalPages = Math.max(1, Math.ceil(totalRecordsRp<%=rnd%> / limitPerPageRp<%=rnd%>));
        if (dir === -1 && currentPageRp<%=rnd%> > 1) { currentPageRp<%=rnd%>--; loadDataRp<%=rnd%>(); }
        else if (dir === 1 && currentPageRp<%=rnd%> < totalPages) { currentPageRp<%=rnd%>++; loadDataRp<%=rnd%>(); }
    };

    const loadDataRp<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataRp<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="8" class="text-center py-5"><div class="spinner-border text-primary"></div></td></tr>';
        try {
            const r = await panggilAksi<%=rnd%>('retur_penjualan_list', {
                keyword: document.getElementById('filterKeyword<%=rnd%>').value.trim(),
                toko_id: document.getElementById('filterToko<%=rnd%>').value,
                page: currentPageRp<%=rnd%>, page_size: limitPerPageRp<%=rnd%>
            });
            if (r.status !== '00' && r.status !== 'success') {
                tbody.innerHTML = '<tr><td colspan="8" class="text-center text-danger py-5">' + (r.description || '<%=Common.getBahasaConfigJS("Gagal memuat data.")%>') + '</td></tr>';
                return;
            }
            totalRecordsRp<%=rnd%> = r.total || 0;
            const data = r.data || [];
            if (data.length === 0) {
                tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-5"><i class="fas fa-inbox fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Belum ada retur penjualan tercatat.")%></td></tr>';
            } else {
                let html = '';
                data.forEach(row => {
                    const balik = row.kembalikanKeStok ? '<span class="badge bg-success bg-opacity-10 text-success border border-success"><i class="fas fa-check me-1"></i><%=Common.getBahasaConfig("Ya")%></span>'
                                                        : '<span class="badge bg-secondary bg-opacity-10 text-secondary border border-secondary"><%=Common.getBahasaConfig("Tidak (Rusak)")%></span>';
                    html += '<tr>' +
                        '<td class="text-start small">' + (row.waktu || '-') + '</td>' +
                        '<td class="text-start fw-bold small">' + (row.kodeTransaksiAsal || '-') + '</td>' +
                        '<td class="text-start fw-bold text-primary">' + (row.namaProduk || '-') + '</td>' +
                        '<td class="text-start small">' + (row.namaPembeli || '-') + '</td>' +
                        '<td class="text-center">' + (row.qty || 0) + '</td>' +
                        '<td class="text-end fw-bold text-danger">' + formatRp<%=rnd%>(row.totalNilai) + '</td>' +
                        '<td class="text-center small">' + (row.alasan || '-') + '</td>' +
                        '<td class="text-center">' + balik + '</td>' +
                    '</tr>';
                });
                tbody.innerHTML = html;
            }
            const totalPages = Math.max(1, Math.ceil(totalRecordsRp<%=rnd%> / limitPerPageRp<%=rnd%>));
            document.getElementById('pageNumberRp<%=rnd%>').innerText = currentPageRp<%=rnd%> + ' / ' + totalPages;
            document.getElementById('pagingInfoRp<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Total")%> <b>' + totalRecordsRp<%=rnd%> + '</b> <%=Common.getBahasaConfig("baris")%>';
            document.getElementById('btnPrevRp<%=rnd%>').disabled = (currentPageRp<%=rnd%> === 1);
            document.getElementById('btnNextRp<%=rnd%>').disabled = (currentPageRp<%=rnd%> >= totalPages);
        } catch (e) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-danger py-5"><%=Common.getBahasaConfig("Gagal menarik data.")%></td></tr>';
        }
    };

    // ==========================================
    // BAGIAN 3: LANGKAH 1 -- CARI TRANSAKSI ASAL
    // ==========================================
    const bukaFormCariTransaksi<%=rnd%> = () => {
        document.getElementById('cariNotaAsal<%=rnd%>').value = '';
        document.getElementById('hasilCariTransaksi<%=rnd%>').innerHTML = '';
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewCari<%=rnd%>').style.display = 'block';
        document.getElementById('viewForm<%=rnd%>').style.display = 'none';
    };

    const tutupSemuaForm<%=rnd%> = () => {
        document.getElementById('viewCari<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'none';
        document.getElementById('viewList<%=rnd%>').style.display = 'block';
        loadDataRp<%=rnd%>();
    };

    const cariTransaksiAsal<%=rnd%> = async () => {
        const kw = document.getElementById('cariNotaAsal<%=rnd%>').value.trim().replace(/'/g, "''");
        const hasilDiv = document.getElementById('hasilCariTransaksi<%=rnd%>');
        if (kw === '') { showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Ketik nomor nota atau nama pembeli dulu.")%>', 'bg-warning text-dark'); return; }
        hasilDiv.innerHTML = '<div class="text-center py-4"><div class="spinner-border text-primary"></div></div>';

        const tokoFilter = isAdminRp<%=rnd%> ? '' : (" AND pak.toko = " + idTokoLoginRp<%=rnd%>);
        const sql = "SELECT pak.id, pak.kode, pak.tanggal_pembayaran, pak.total_biaya, " +
            "COALESCE(ak.nama, '<%=Common.getBahasaConfigJS("Pelanggan Umum")%>') as nama_pembeli " +
            "FROM koperasi.pembelian_anggota_koperasi pak " +
            "LEFT JOIN koperasi.anggota_koperasi ak ON pak.anggota_koperasi = ak.id " +
            "WHERE (pak.kode ILIKE '%" + kw + "%' OR COALESCE(ak.nama,'') ILIKE '%" + kw + "%')" + tokoFilter +
            " ORDER BY pak.tanggal_pembayaran DESC LIMIT 25";
        const rows = await fetchData<%=rnd%>(sql);
        if (rows.length === 0) {
            hasilDiv.innerHTML = '<div class="daftar-kosong text-center text-muted py-4"><i class="fas fa-search fa-2x mb-2 d-block opacity-50"></i><%=Common.getBahasaConfig("Transaksi tidak ditemukan.")%></div>';
            return;
        }
        let html = '<div class="list-group shadow-sm">';
        rows.forEach(r => {
            const safeNama = (r.nama_pembeli || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
            html += '<button type="button" class="list-group-item list-group-item-action d-flex justify-content-between align-items-center" ' +
                'onclick="pilihTransaksiAsal<%=rnd%>(' + r.id + ', \'' + (r.kode || '') + '\', \'' + safeNama + '\')">' +
                '<span><b class="text-primary">' + (r.kode || '#' + r.id) + '</b> &middot; ' + (r.nama_pembeli || '-') + '<br>' +
                '<small class="text-muted">' + (r.tanggal_pembayaran || '-') + '</small></span>' +
                '<span class="fw-bold text-success">' + formatRp<%=rnd%>(r.total_biaya) + '</span>' +
            '</button>';
        });
        html += '</div>';
        hasilDiv.innerHTML = html;
    };

    // ==========================================
    // BAGIAN 4: LANGKAH 2 -- PILIH BARANG
    // ==========================================
    const pilihTransaksiAsal<%=rnd%> = async (idTransaksi, kode, namaPembeli) => {
        document.getElementById('idTransaksiAsal<%=rnd%>').value = idTransaksi;
        document.getElementById('kodeTransaksiAsal<%=rnd%>').value = kode;
        document.getElementById('namaPembeliAsal<%=rnd%>').value = namaPembeli;
        document.getElementById('labelTransaksiAsal<%=rnd%>').innerText = kode + ' -- ' + namaPembeli;

        const sql = "SELECT p.produk, pr.nama, p.qty, COALESCE(p.hargasatuan, (p.total / NULLIF(p.qty,0)), 0) as harga " +
            "FROM koperasi.pembelian p JOIN koperasi.produk pr ON p.produk = pr.id " +
            "WHERE p.pembelian_anggota_koperasi = " + idTransaksi + " ORDER BY p.id ASC";
        const items = await fetchData<%=rnd%>(sql);

        let optAlasan = ALASAN_RETUR<%=rnd%>.map(a => '<option value="' + a + '">' + a + '</option>').join('');
        let optKondisi = KONDISI_BARANG<%=rnd%>.map(k => '<option value="' + k + '">' + k + '</option>').join('');

        let html = '<thead class="table-light"><tr>' +
            '<th style="width:36px;"></th><th><%=Common.getBahasaConfig("Produk")%></th>' +
            '<th style="width:100px;"><%=Common.getBahasaConfig("Qty Beli")%></th>' +
            '<th style="width:110px;"><%=Common.getBahasaConfig("Qty Retur")%></th>' +
            '<th style="width:150px;"><%=Common.getBahasaConfig("Alasan")%></th>' +
            '<th style="width:190px;"><%=Common.getBahasaConfig("Kondisi Barang")%></th>' +
            '</tr></thead><tbody>';
        items.forEach((it, idx) => {
            html += '<tr class="baris-item-rp<%=rnd%>" data-produk="' + it.produk + '" data-harga="' + it.harga + '" data-qty-beli="' + it.qty + '">' +
                '<td class="text-center"><input type="checkbox" class="form-check-input chk-item-rp<%=rnd%>" data-idx="' + idx + '"></td>' +
                '<td class="fw-bold">' + it.nama + '<br><small class="text-muted"><%=Common.getBahasaConfig("Harga saat beli")%>: ' + formatRp<%=rnd%>(it.harga) + '</small></td>' +
                '<td class="text-center">' + it.qty + '</td>' +
                '<td><input type="number" class="form-control form-control-sm qty-retur-rp<%=rnd%>" min="0.01" max="' + it.qty + '" step="0.01" value="' + it.qty + '" disabled></td>' +
                '<td><select class="form-select form-select-sm alasan-rp<%=rnd%>" disabled>' + optAlasan + '</select></td>' +
                '<td><select class="form-select form-select-sm kondisi-rp<%=rnd%>" disabled>' + optKondisi + '</select></td>' +
            '</tr>';
        });
        html += '</tbody>';
        document.getElementById('tabelItemAsal<%=rnd%>').innerHTML = html;

        document.querySelectorAll('.chk-item-rp<%=rnd%>').forEach(chk => {
            chk.addEventListener('change', function () {
                const tr = this.closest('tr');
                tr.querySelectorAll('input, select').forEach(el => { if (el !== this) el.disabled = !this.checked; });
                hitungTotalRetur<%=rnd%>();
            });
        });
        document.querySelectorAll('.qty-retur-rp<%=rnd%>').forEach(inp => inp.addEventListener('input', hitungTotalRetur<%=rnd%>));

        document.getElementById('viewCari<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    };

    // "Kondisi: Rusak" otomatis membuat kembalikan_ke_stok=false saat simpan (lihat simpanRetur<%=rnd%>)
    // -- diturunkan dari pilihan dropdown Kondisi Barang, BUKAN checkbox terpisah, supaya kasir tak
    // perlu mengingat 2 kontrol yang sebetulnya saling terkait.
    const hitungTotalRetur<%=rnd%> = () => {
        let total = 0;
        document.querySelectorAll('.baris-item-rp<%=rnd%>').forEach(tr => {
            const chk = tr.querySelector('.chk-item-rp<%=rnd%>');
            if (chk && chk.checked) {
                const qty = parseFloat(tr.querySelector('.qty-retur-rp<%=rnd%>').value) || 0;
                const harga = parseFloat(tr.getAttribute('data-harga')) || 0;
                total += qty * harga;
            }
        });
        document.getElementById('totalNilaiRetur<%=rnd%>').value = formatRp<%=rnd%>(total);
    };

    // ==========================================
    // BAGIAN 5: SIMPAN
    // ==========================================
    const simpanRetur<%=rnd%> = async () => {
        const items = [];
        let adaError = false;
        document.querySelectorAll('.baris-item-rp<%=rnd%>').forEach(tr => {
            const chk = tr.querySelector('.chk-item-rp<%=rnd%>');
            if (!chk || !chk.checked) return;
            const qty = parseFloat(tr.querySelector('.qty-retur-rp<%=rnd%>').value) || 0;
            const qtyBeli = parseFloat(tr.getAttribute('data-qty-beli')) || 0;
            if (qty <= 0 || qty > qtyBeli) { adaError = true; return; }
            const kondisi = tr.querySelector('.kondisi-rp<%=rnd%>').value;
            items.push({
                produk_id: tr.getAttribute('data-produk'),
                qty: qty,
                harga_satuan: parseFloat(tr.getAttribute('data-harga')) || 0,
                alasan: tr.querySelector('.alasan-rp<%=rnd%>').value,
                kondisi_barang: kondisi,
                kembalikan_ke_stok: !kondisi.toLowerCase().includes('rusak')
            });
        });

        if (adaError) { showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Jumlah retur ada yang melebihi jumlah beli asal.")%>', 'bg-danger text-white'); return; }
        if (items.length === 0) { showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Pilih minimal satu barang untuk diretur.")%>', 'bg-warning text-dark'); return; }

        const btn = document.getElementById('btnSimpanRp<%=rnd%>');
        const oriHtml = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyimpan...")%>';
        try {
            const r = await panggilAksi<%=rnd%>('retur_penjualan_simpan', {
                pembelian_anggota_koperasi_id: document.getElementById('idTransaksiAsal<%=rnd%>').value,
                kode_transaksi_asal: document.getElementById('kodeTransaksiAsal<%=rnd%>').value,
                nama_pembeli: document.getElementById('namaPembeliAsal<%=rnd%>').value,
                metode_pengembalian: document.getElementById('metodePengembalian<%=rnd%>').value,
                items: items
            });
            if (r.status === '00' || r.status === 'success') {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Retur penjualan berhasil disimpan.")%>', 'bg-success text-white');
                tutupSemuaForm<%=rnd%>();
            } else {
                showToast<%=rnd%>(r.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan retur.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal terhubung ke peladen.")%>', 'bg-danger text-white');
        } finally {
            btn.disabled = false;
            btn.innerHTML = oriHtml;
        }
    };

    // ==========================================
    // INISIALISASI
    // ==========================================
    document.addEventListener('DOMContentLoaded', () => {
        loadComboTokoRp<%=rnd%>();
        loadDataRp<%=rnd%>();
        const filterInputs = document.querySelectorAll('.filter-input-<%=rnd%>');
        filterInputs.forEach(input => {
            if (input.tagName === 'SELECT') { input.addEventListener('change', () => triggerSearchRp<%=rnd%>()); }
            else { input.addEventListener('keydown', (e) => { if (e.key === 'Enter') { e.preventDefault(); triggerSearchRp<%=rnd%>(); } }); }
        });
    });
</script>
