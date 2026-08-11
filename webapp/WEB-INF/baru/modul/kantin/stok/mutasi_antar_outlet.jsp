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
Pedagang pedagangLogin = tbmuser.getPedagang();
Toko tokoLogin = pedagangLogin == null ? null : pedagangLogin.getToko();
String rnd = Common.getGeneratedBarCode(7);

boolean isAdmin = (tokoLogin == null);
boolean supervisor = pedagangLogin != null && Boolean.TRUE.equals(pedagangLogin.getSupervisor());
String idTokoAktif = tokoLogin != null ? String.valueOf(tokoLogin.getId()) : "";
String namaTokoAktif = tokoLogin != null ? tokoLogin.getNama() : "";
%>

<div class="row mb-4">
    <div class="col-12">
        <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">

            <div class="card-header bg-white border-0 pt-4 pb-3 px-4">
                <h5 class="fw-bold text-dark mb-1">
                    <i class="fas fa-truck-loading text-primary me-2"></i><%=Common.getBahasaConfig("Mutasi Stok Antar Outlet")%>
                </h5>
                <small class="text-muted"><%=Common.getBahasaConfig("Kirim/transfer stok produk dari toko/kios ini ke toko/kios lain. Stok kedua sisi disesuaikan otomatis.")%></small>
            </div>

            <div class="card-body p-4 pt-3">

                <% if (!isAdmin && !supervisor) { %>
                <div class="text-center text-muted py-5">
                    <i class="fas fa-lock fa-2x mb-3 d-block opacity-50"></i>
                    <%=Common.getBahasaConfig("Hanya admin/manager atau supervisor toko yang dapat mencatat Mutasi Stok Antar Outlet.")%>
                </div>
                <% } else { %>

                <form id="formMutasi<%=rnd%>" onsubmit="event.preventDefault(); simpanMutasi<%=rnd%>();">
                    <div class="row g-3">
                        <% if (isAdmin) { %>
                        <div class="col-md-4">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Toko Asal")%> <span class="text-danger">*</span></label>
                            <select class="form-select shadow-sm fw-semibold" id="inputTokoAsal<%=rnd%>" onchange="loadComboProdukAsal<%=rnd%>(); loadRiwayatMutasi<%=rnd%>();" required>
                                <option value=""><%=Common.getBahasaConfig("-- Pilih Toko Asal (juga jadi filter riwayat) --")%></option>
                            </select>
                        </div>
                        <% } else { %>
                            <input type="hidden" id="inputTokoAsal<%=rnd%>" value="<%=idTokoAktif%>">
                        <% } %>

                        <div class="col-md-<%=isAdmin ? "4" : "6"%>">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Toko Tujuan")%> <span class="text-danger">*</span></label>
                            <select class="form-select shadow-sm fw-semibold" id="inputTokoTujuan<%=rnd%>" onchange="resetPilihManual<%=rnd%>()" required>
                                <option value=""><%=Common.getBahasaConfig("-- Pilih Toko Tujuan --")%></option>
                            </select>
                        </div>

                        <div class="col-md-<%=isAdmin ? "4" : "6"%>">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Produk / Barang (Toko Asal)")%> <span class="text-danger">*</span></label>
                            <div class="input-group shadow-sm">
                                <span class="input-group-text bg-white border-end-0"><i class="fas fa-box text-primary"></i></span>
                                <input type="text" class="form-control border-start-0 fw-bold" list="listProdukAsal<%=rnd%>" id="inputCariProdukAsal<%=rnd%>" placeholder="<%=isAdmin ? Common.getBahasaConfig("Pilih Toko Asal dulu...") : Common.getBahasaConfig("Ketik nama/kode produk...")%>" autocomplete="off" <%=isAdmin ? "disabled" : ""%> required>
                            </div>
                            <datalist id="listProdukAsal<%=rnd%>"></datalist>
                            <input type="hidden" id="idProdukAsalSelected<%=rnd%>" value="">
                            <small class="text-muted" id="infoStokAsal<%=rnd%>"></small>
                        </div>

                        <div class="col-md-3">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jumlah (Qty)")%> <span class="text-danger">*</span></label>
                            <input type="number" step="0.01" min="0.01" class="form-control shadow-sm fw-bold" id="inputQty<%=rnd%>" placeholder="0" required>
                        </div>

                        <div class="col-md-9">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Keterangan (opsional)")%></label>
                            <input type="text" class="form-control shadow-sm" id="inputKeterangan<%=rnd%>" placeholder="<%=Common.getBahasaConfig("mis. Restok kios cabang, permintaan mendadak, dsb.")%>">
                        </div>
                    </div>

                    <div class="alert alert-warning py-2 px-3 mt-3 small" id="boxPilihManual<%=rnd%>" style="display:none;">
                        <div class="fw-bold mb-2"><i class="fas fa-exclamation-triangle me-1"></i><%=Common.getBahasaConfig("Produk yang sama tidak ditemukan otomatis di toko tujuan -- pilih produk tujuan secara manual:")%></div>
                        <div class="input-group input-group-sm">
                            <input type="text" class="form-control" list="listProdukTujuan<%=rnd%>" id="inputCariProdukTujuan<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik nama/kode produk di toko tujuan...")%>" autocomplete="off">
                        </div>
                        <datalist id="listProdukTujuan<%=rnd%>"></datalist>
                        <input type="hidden" id="idProdukTujuanSelected<%=rnd%>" value="">
                    </div>

                    <div class="mt-3 text-end">
                        <button type="submit" class="btn btn-primary px-4 rounded-pill shadow-sm fw-bold" id="btnSimpanMutasi<%=rnd%>">
                            <i class="fas fa-paper-plane me-2"></i><%=Common.getBahasaConfig("Kirim Stok")%>
                        </button>
                    </div>
                </form>
                <% } %>

                <hr class="my-4">

                <h6 class="fw-bold text-dark mb-3"><i class="fas fa-history text-muted me-2"></i><%=Common.getBahasaConfig("Riwayat Mutasi Stok")%></h6>
                <div class="table-responsive border rounded-3 shadow-sm bg-white">
                    <table class="table table-hover table-striped align-middle mb-0">
                        <thead class="table-dark text-center align-middle">
                            <tr>
                                <th class="text-start fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Waktu")%></th>
                                <th class="fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Arah")%></th>
                                <th class="text-start fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Produk Asal")%></th>
                                <th class="text-start fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Produk Tujuan")%></th>
                                <th class="text-start fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Toko Asal")%></th>
                                <th class="text-start fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Toko Tujuan")%></th>
                                <th class="text-end fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Qty")%></th>
                                <th class="text-start fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Keterangan")%></th>
                            </tr>
                        </thead>
                        <tbody id="tabelRiwayatMutasi<%=rnd%>">
                            <tr><td colspan="8" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div></td></tr>
                        </tbody>
                    </table>
                </div>

            </div>
        </div>
    </div>
</div>

<script>
    const isAdmin<%=rnd%> = <%=isAdmin%>;
    const idTokoLogin<%=rnd%> = '<%=idTokoAktif%>';
    let arrDataProdukAsal<%=rnd%> = [];
    let arrDataProdukTujuan<%=rnd%> = [];

    const fetchData<%=rnd%> = async (payload) => {
        const res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        return await res.json();
    };

    const showToast<%=rnd%> = (msg, colorClass) => {
        if (typeof tampilkanToast === "function") { tampilkanToast(msg, colorClass); } else { alert(msg); }
    };

    // ==== Combo Toko (asal utk admin, tujuan utk semua) ====
    const loadComboToko<%=rnd%> = async () => {
        const res = await fetchData<%=rnd%>({ action: "sql", sql: "SELECT id, nama FROM koperasi.toko WHERE aktif = true ORDER BY nama ASC" });
        const rows = res.data || [];
        let opt = '<option value=""><%=Common.getBahasaConfigJS("-- Pilih Toko --")%></option>';
        rows.forEach(r => { opt += '<option value="' + r.id + '">' + r.nama + '</option>'; });

        const elTujuan = document.getElementById('inputTokoTujuan<%=rnd%>');
        if (elTujuan) elTujuan.innerHTML = opt;

        if (isAdmin<%=rnd%>) {
            const elAsal = document.getElementById('inputTokoAsal<%=rnd%>');
            if (elAsal) elAsal.innerHTML = opt;
        }
    };

    // ==== Produk toko asal (dgn info stok) ====
    const loadComboProdukAsal<%=rnd%> = async () => {
        const tokoId = isAdmin<%=rnd%> ? document.getElementById('inputTokoAsal<%=rnd%>').value : idTokoLogin<%=rnd%>;
        const input = document.getElementById('inputCariProdukAsal<%=rnd%>');
        const dt = document.getElementById('listProdukAsal<%=rnd%>');
        document.getElementById('idProdukAsalSelected<%=rnd%>').value = '';
        document.getElementById('infoStokAsal<%=rnd%>').textContent = '';
        if (!tokoId) { input.value = ''; input.disabled = true; return; }

        input.disabled = true;
        input.placeholder = '<%=Common.getBahasaConfigJS("Memuat produk...")%>';
        const res = await fetchData<%=rnd%>({ action: "sql", sql: "SELECT id, nama, kode, stok FROM koperasi.produk WHERE toko = " + tokoId + " AND aktif = true AND (jenis_item IS NULL OR jenis_item = 'JUAL') ORDER BY nama ASC" });
        const rows = res.data || [];
        arrDataProdukAsal<%=rnd%> = [];
        let html = '';
        rows.forEach(r => {
            const kode = r.kode ? ' [' + r.kode + ']' : '';
            const namaLengkap = r.nama + kode;
            arrDataProdukAsal<%=rnd%>.push({ id: r.id, nama_lengkap: namaLengkap, stok: r.stok || 0 });
            html += '<option value="' + namaLengkap + '">';
        });
        dt.innerHTML = html;
        input.disabled = false;
        input.placeholder = '<%=Common.getBahasaConfigJS("Ketik nama/kode produk...")%>';
    };

    document.addEventListener("DOMContentLoaded", () => {
        const inputAsal = document.getElementById('inputCariProdukAsal<%=rnd%>');
        if (inputAsal) {
            inputAsal.addEventListener('change', function () {
                const match = arrDataProdukAsal<%=rnd%>.find(p => p.nama_lengkap === this.value);
                document.getElementById('idProdukAsalSelected<%=rnd%>').value = match ? match.id : '';
                document.getElementById('infoStokAsal<%=rnd%>').textContent = match
                    ? ('<%=Common.getBahasaConfigJS("Stok saat ini: ")%>' + match.stok)
                    : '';
            });
        }
        const inputTujuanManual = document.getElementById('inputCariProdukTujuan<%=rnd%>');
        if (inputTujuanManual) {
            inputTujuanManual.addEventListener('change', function () {
                const match = arrDataProdukTujuan<%=rnd%>.find(p => p.nama_lengkap === this.value);
                document.getElementById('idProdukTujuanSelected<%=rnd%>').value = match ? match.id : '';
            });
        }
        loadComboToko<%=rnd%>();
        if (!isAdmin<%=rnd%>) { loadComboProdukAsal<%=rnd%>(); loadRiwayatMutasi<%=rnd%>(); }
    });

    const resetPilihManual<%=rnd%> = () => {
        document.getElementById('boxPilihManual<%=rnd%>').style.display = 'none';
        document.getElementById('idProdukTujuanSelected<%=rnd%>').value = '';
        const el = document.getElementById('inputCariProdukTujuan<%=rnd%>');
        if (el) el.value = '';
    };

    // ==== Muat daftar produk toko tujuan (utk picker manual, dipicu saat auto-match gagal) ====
    const muatProdukTujuanManual<%=rnd%> = async () => {
        const tokoTujuanId = document.getElementById('inputTokoTujuan<%=rnd%>').value;
        if (!tokoTujuanId) return;
        const res = await fetchData<%=rnd%>({ action: "sql", sql: "SELECT id, nama, kode FROM koperasi.produk WHERE toko = " + tokoTujuanId + " AND aktif = true AND (jenis_item IS NULL OR jenis_item = 'JUAL') ORDER BY nama ASC" });
        const rows = res.data || [];
        arrDataProdukTujuan<%=rnd%> = [];
        let html = '';
        rows.forEach(r => {
            const kode = r.kode ? ' [' + r.kode + ']' : '';
            const namaLengkap = r.nama + kode;
            arrDataProdukTujuan<%=rnd%>.push({ id: r.id, nama_lengkap: namaLengkap });
            html += '<option value="' + namaLengkap + '">';
        });
        document.getElementById('listProdukTujuan<%=rnd%>').innerHTML = html;
    };

    // ==== Simpan (kirim stok) ====
    const simpanMutasi<%=rnd%> = async (produkTujuanIdEksplisit) => {
        const tokoAsalId = document.getElementById('inputTokoAsal<%=rnd%>').value;
        const tokoTujuanId = document.getElementById('inputTokoTujuan<%=rnd%>').value;
        const produkAsalId = document.getElementById('idProdukAsalSelected<%=rnd%>').value;
        const qty = parseFloat(document.getElementById('inputQty<%=rnd%>').value);
        const keterangan = document.getElementById('inputKeterangan<%=rnd%>').value.trim();

        if (!tokoAsalId || !tokoTujuanId || !produkAsalId) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Toko asal, toko tujuan, dan produk wajib diisi!")%>', 'bg-warning text-dark');
            return;
        }
        if (!qty || qty <= 0) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Jumlah (qty) wajib diisi lebih dari 0!")%>', 'bg-warning text-dark');
            return;
        }

        const btn = document.getElementById('btnSimpanMutasi<%=rnd%>');
        const asliBtn = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Mengirim...")%>';

        const payload = {
            action: "mutasi_stok_simpan",
            toko_id: tokoAsalId,
            produk_asal_id: produkAsalId,
            toko_tujuan_id: tokoTujuanId,
            qty: qty,
            keterangan: keterangan
        };
        if (produkTujuanIdEksplisit) payload.produk_tujuan_id = produkTujuanIdEksplisit;

        try {
            const result = await fetchData<%=rnd%>(payload);
            if (result.status === '00') {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Stok berhasil dikirim ke ")%>' + (result.produkTujuanNama || '') + '.', 'bg-success text-white');
                document.getElementById('formMutasi<%=rnd%>').reset();
                resetPilihManual<%=rnd%>();
                document.getElementById('idProdukAsalSelected<%=rnd%>').value = '';
                if (isAdmin<%=rnd%>) document.getElementById('inputCariProdukAsal<%=rnd%>').value = '';
                loadRiwayatMutasi<%=rnd%>();
                if (!isAdmin<%=rnd%>) loadComboProdukAsal<%=rnd%>();
            } else if (result.status === '92') {
                document.getElementById('boxPilihManual<%=rnd%>').style.display = 'block';
                await muatProdukTujuanManual<%=rnd%>();
                showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Pilih produk tujuan secara manual di bawah, lalu kirim ulang.")%>', 'bg-warning text-dark');
            } else {
                showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal mengirim stok.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal terhubung ke server.")%>', 'bg-danger text-white');
        } finally {
            btn.disabled = false;
            btn.innerHTML = asliBtn;
        }
    };

    // Kalau kotak pilih-manual sedang tampil dan user sudah pilih produk tujuan, tombol "Kirim Stok"
    // (submit form yang sama) otomatis menyertakan produk_tujuan_id eksplisit.
    document.getElementById('formMutasi<%=rnd%>') && document.getElementById('formMutasi<%=rnd%>').addEventListener('submit', function (ev) {
        const idTujuanManual = document.getElementById('idProdukTujuanSelected<%=rnd%>');
        if (idTujuanManual && idTujuanManual.value) {
            ev.preventDefault();
            simpanMutasi<%=rnd%>(idTujuanManual.value);
        }
    }, true);

    // ==== Riwayat (dilihat dari kacamata Toko Asal -- utk admin, pilih dulu di combo Toko Asal) ====
    const loadRiwayatMutasi<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelRiwayatMutasi<%=rnd%>');
        const tokoId = document.getElementById('inputTokoAsal<%=rnd%>').value;
        if (!tokoId) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-5"><%=Common.getBahasaConfig("Pilih Toko Asal di atas untuk melihat riwayat mutasi toko itu.")%></td></tr>';
            return;
        }
        tbody.innerHTML = '<tr><td colspan="8" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div></td></tr>';
        const result = await fetchData<%=rnd%>({ action: "mutasi_stok_list", toko_id: tokoId, limit: 100 });
        const rows = result.data || [];
        if (rows.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-5"><%=Common.getBahasaConfig("Belum ada riwayat mutasi stok.")%></td></tr>';
            return;
        }
        let html = '';
        rows.forEach(r => {
            const badge = r.arah === 'masuk'
                ? '<span class="badge bg-success-subtle text-success border border-success-subtle"><i class="fas fa-arrow-down me-1"></i><%=Common.getBahasaConfigJS("Masuk")%></span>'
                : '<span class="badge bg-danger-subtle text-danger border border-danger-subtle"><i class="fas fa-arrow-up me-1"></i><%=Common.getBahasaConfigJS("Keluar")%></span>';
            html += '<tr>' +
                '<td class="text-start text-muted small">' + (r.waktu || '-') + '</td>' +
                '<td class="text-center">' + badge + '</td>' +
                '<td class="text-start">' + (r.produkAsalNama || '-') + '</td>' +
                '<td class="text-start">' + (r.produkTujuanNama || '-') + '</td>' +
                '<td class="text-start text-muted small">' + (r.tokoAsalNama || '-') + '</td>' +
                '<td class="text-start text-muted small">' + (r.tokoTujuanNama || '-') + '</td>' +
                '<td class="text-end fw-bold">' + (r.qty || 0) + '</td>' +
                '<td class="text-start text-muted small">' + (r.keterangan || '-') + '</td>' +
                '</tr>';
        });
        tbody.innerHTML = html;
    };
</script>
