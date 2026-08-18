<%@page import="ais.common.Common"%>
<%
// Grup Produk (harga terpusat lintas toko) -- UI JSP untuk aksi grup_produk_daftar/simpan/hapus
// (GrupProdukApiHelper; permission menu+CRUD dicek SERVER-SIDE di sana, UI ini hanya kenyamanan).
String rnd = Common.getGeneratedBarCode(7);
%>

<div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
    <div class="card-header bg-white border-0 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-3 border-bottom border-light">
        <div>
            <h5 class="fw-bold text-dark mb-1"><i class="fas fa-layer-group text-primary me-2"></i><%=Common.getBahasaConfig("Grup Produk (Harga Terpusat)")%></h5>
            <small class="text-muted"><%=Common.getBahasaConfig("Ubah HPP/harga jual sekali di grup -- diterapkan ke seluruh produk anggota di semua toko/outlet.")%></small>
        </div>
        <div class="d-flex align-items-center gap-2 flex-wrap">
            <div class="input-group input-group-sm shadow-sm" style="min-width:180px;">
                <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>
                <input type="text" class="form-control border-start-0 ps-0" id="cariGrup<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari kode/nama grup...")%>">
            </div>
            <button class="btn btn-sm btn-outline-secondary rounded-pill px-3 fw-bold" onclick="muatGrup<%=rnd%>()"><i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Segarkan")%></button>
            <button class="btn btn-sm btn-primary rounded-pill px-3 fw-bold" onclick="bukaFormGrup<%=rnd%>(null)"><i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Tambah Grup")%></button>
        </div>
    </div>
    <div class="card-body p-4 pt-3">
        <div class="table-responsive border rounded-3 shadow-sm">
            <table class="table table-hover table-striped align-middle mb-0">
                <thead class="table-dark text-center align-middle">
                    <tr>
                        <th class="text-start px-3"><%=Common.getBahasaConfig("Kode")%></th>
                        <th class="text-start px-3"><%=Common.getBahasaConfig("Nama Grup")%></th>
                        <th class="px-3"><%=Common.getBahasaConfig("HPP / Harga Beli")%></th>
                        <th class="px-3"><%=Common.getBahasaConfig("Harga Jual")%></th>
                        <th class="px-3"><%=Common.getBahasaConfig("Produk Anggota")%></th>
                        <th class="px-3"><%=Common.getBahasaConfig("Status")%></th>
                        <th class="px-3" style="width:110px;"><i class="fas fa-cogs"></i></th>
                    </tr>
                </thead>
                <tbody id="tabelGrup<%=rnd%>">
                    <tr><td colspan="7" class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm me-2"></div><%=Common.getBahasaConfig("Memuat data...")%></td></tr>
                </tbody>
            </table>
        </div>
    </div>
</div>

<!-- Modal form -->
<div class="modal fade" id="modalGrup<%=rnd%>" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-4 border-0 shadow-lg">
            <div class="modal-header border-0 pb-0">
                <h5 class="fw-bold mb-0" id="judulFormGrup<%=rnd%>"><%=Common.getBahasaConfig("Grup Produk")%></h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body p-4">
                <form id="formGrup<%=rnd%>" onsubmit="event.preventDefault(); simpanGrup<%=rnd%>();">
                    <input type="hidden" id="idGrup<%=rnd%>" value="">
                    <div class="mb-3">
                        <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Kode Grup")%></label>
                        <input type="text" class="form-control form-control-sm" id="kodeGrup<%=rnd%>" placeholder="AYAM-MRN">
                    </div>
                    <div class="mb-3">
                        <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Nama Grup")%> <span class="text-danger">*</span></label>
                        <input type="text" class="form-control form-control-sm" id="namaGrup<%=rnd%>" required placeholder="<%=Common.getBahasaConfig("Contoh: Ayam Marinasi")%>">
                    </div>
                    <div class="mb-3">
                        <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Keterangan")%></label>
                        <textarea class="form-control form-control-sm" id="ketGrup<%=rnd%>" rows="2"></textarea>
                    </div>
                    <div class="row g-2 mb-3">
                        <div class="col-6">
                            <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("HPP / Harga Beli")%></label>
                            <input type="number" step="any" min="0" class="form-control form-control-sm" id="hargaBeliGrup<%=rnd%>" placeholder="<%=Common.getBahasaConfig("kosongkan bila tidak dikendalikan grup")%>">
                        </div>
                        <div class="col-6">
                            <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Harga Jual")%></label>
                            <input type="number" step="any" min="0" class="form-control form-control-sm" id="hargaJualGrup<%=rnd%>" placeholder="<%=Common.getBahasaConfig("kosongkan bila tidak dikendalikan grup")%>">
                        </div>
                    </div>
                    <div class="form-check form-switch mb-3">
                        <input class="form-check-input" type="checkbox" id="aktifGrup<%=rnd%>" checked>
                        <label class="form-check-label fw-semibold" for="aktifGrup<%=rnd%>"><%=Common.getBahasaConfig("Aktif")%></label>
                    </div>
                    <div class="alert alert-warning small py-2 mb-3">
                        <i class="fas fa-triangle-exclamation me-1"></i><%=Common.getBahasaConfig("Menyimpan grup akan MENIMPA harga seluruh produk anggota di semua outlet (kolom harga yang kosong tidak disalin). Setiap perubahan tercatat pada jejak audit per produk.")%>
                    </div>
                    <div class="text-end">
                        <button type="button" class="btn btn-light border rounded-pill px-4 me-2" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
                        <button type="submit" class="btn btn-primary rounded-pill px-4 fw-bold" id="btnSimpanGrup<%=rnd%>"><i class="fas fa-save me-1"></i><%=Common.getBahasaConfig("Simpan & Terapkan ke Semua Outlet")%></button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<script>
    var daftarGrup<%=rnd%> = [];

    var panggilGrupApi<%=rnd%> = async (payload) => {
        const res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        return res.json();
    };

    var toastGrup<%=rnd%> = (msg, cls) => {
        if (typeof tampilkanToast === "function") tampilkanToast(msg, cls); else alert(msg);
    };

    var fmtRpGrup<%=rnd%> = (v) => (v === null || v === undefined || v === '')
        ? '<span class="text-muted">-</span>'
        : new Intl.NumberFormat('id-ID').format(v);

    var muatGrup<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelGrup<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm me-2"></div><%=Common.getBahasaConfigJS("Memuat data...")%></td></tr>';
        const hasil = await panggilGrupApi<%=rnd%>({
            action: 'grup_produk_daftar',
            hanya_aktif: false,
            cari: document.getElementById('cariGrup<%=rnd%>').value.trim()
        });
        if (hasil.status !== '00') {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger py-4">' + (hasil.description || '<%=Common.getBahasaConfigJS("Gagal memuat data.")%>') + '</td></tr>';
            return;
        }
        daftarGrup<%=rnd%> = hasil.data || [];
        if (daftarGrup<%=rnd%>.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4"><i class="fas fa-layer-group fa-2x mb-2 opacity-50 d-block"></i><%=Common.getBahasaConfigJS("Belum ada grup produk.")%></td></tr>';
            return;
        }
        let html = '';
        daftarGrup<%=rnd%>.forEach((g) => {
            const badge = g.aktif
                ? '<span class="badge bg-success bg-opacity-10 text-success border border-success px-2">Aktif</span>'
                : '<span class="badge bg-danger bg-opacity-10 text-danger border border-danger px-2">Non-Aktif</span>';
            html += '<tr>' +
                '<td class="text-start fw-bold text-secondary px-3">' + (g.kode || '-') + '</td>' +
                '<td class="text-start fw-bold text-dark px-3">' + g.nama + '</td>' +
                '<td class="text-end px-3">' + fmtRpGrup<%=rnd%>(g.harga_beli) + '</td>' +
                '<td class="text-end px-3">' + fmtRpGrup<%=rnd%>(g.harga_jual) + '</td>' +
                '<td class="text-center px-3"><span class="badge bg-primary bg-opacity-10 text-primary border border-primary px-2">' + g.jumlah_anggota + '</span></td>' +
                '<td class="text-center px-3">' + badge + '</td>' +
                '<td class="text-center text-nowrap px-3">' +
                    '<button class="btn btn-sm btn-outline-warning text-dark px-2 me-1" onclick="bukaFormGrup<%=rnd%>(' + g.id + ')" title="<%=Common.getBahasaConfigJS("Ubah")%>"><i class="fas fa-edit"></i></button>' +
                    '<button class="btn btn-sm btn-outline-danger px-2" onclick="hapusGrup<%=rnd%>(' + g.id + ', \'' + (g.nama || '').replace(/'/g, "\\'") + '\')" title="<%=Common.getBahasaConfigJS("Hapus")%>"><i class="fas fa-trash-alt"></i></button>' +
                '</td></tr>';
        });
        tbody.innerHTML = html;
    };

    var bukaFormGrup<%=rnd%> = (id) => {
        document.getElementById('formGrup<%=rnd%>').reset();
        document.getElementById('idGrup<%=rnd%>').value = '';
        document.getElementById('aktifGrup<%=rnd%>').checked = true;
        document.getElementById('judulFormGrup<%=rnd%>').innerText = id === null
            ? '<%=Common.getBahasaConfigJS("Tambah Grup Produk")%>' : '<%=Common.getBahasaConfigJS("Ubah Grup Produk")%>';
        if (id !== null) {
            const g = daftarGrup<%=rnd%>.find(x => x.id === id);
            if (g) {
                document.getElementById('idGrup<%=rnd%>').value = g.id;
                document.getElementById('kodeGrup<%=rnd%>').value = g.kode || '';
                document.getElementById('namaGrup<%=rnd%>').value = g.nama || '';
                document.getElementById('ketGrup<%=rnd%>').value = g.keterangan || '';
                document.getElementById('hargaBeliGrup<%=rnd%>').value = g.harga_beli === null ? '' : g.harga_beli;
                document.getElementById('hargaJualGrup<%=rnd%>').value = g.harga_jual === null ? '' : g.harga_jual;
                document.getElementById('aktifGrup<%=rnd%>').checked = g.aktif;
            }
        }
        new bootstrap.Modal(document.getElementById('modalGrup<%=rnd%>')).show();
    };

    var simpanGrup<%=rnd%> = async () => {
        const btn = document.getElementById('btnSimpanGrup<%=rnd%>');
        const ori = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span><%=Common.getBahasaConfigJS("Menyimpan...")%>';
        try {
            const hb = document.getElementById('hargaBeliGrup<%=rnd%>').value.trim();
            const hj = document.getElementById('hargaJualGrup<%=rnd%>').value.trim();
            const payload = {
                action: 'grup_produk_simpan',
                nama: document.getElementById('namaGrup<%=rnd%>').value.trim(),
                kode: document.getElementById('kodeGrup<%=rnd%>').value.trim(),
                keterangan: document.getElementById('ketGrup<%=rnd%>').value.trim(),
                aktif: document.getElementById('aktifGrup<%=rnd%>').checked
            };
            if (hb !== '') payload.harga_beli = parseFloat(hb);
            if (hj !== '') payload.harga_jual = parseFloat(hj);
            const id = document.getElementById('idGrup<%=rnd%>').value;
            if (id !== '') payload.id = id;
            const hasil = await panggilGrupApi<%=rnd%>(payload);
            if (hasil.status === '00') {
                bootstrap.Modal.getInstance(document.getElementById('modalGrup<%=rnd%>')).hide();
                toastGrup<%=rnd%>('<%=Common.getBahasaConfigJS("Grup tersimpan. Harga diterapkan ke")%> ' + (hasil.diterapkan || 0) + ' <%=Common.getBahasaConfigJS("produk di seluruh outlet.")%>', 'bg-success text-white');
                muatGrup<%=rnd%>();
            } else {
                toastGrup<%=rnd%>(hasil.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan grup.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            console.error(e);
            toastGrup<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi saat menyimpan.")%>', 'bg-danger text-white');
        } finally {
            btn.disabled = false;
            btn.innerHTML = ori;
        }
    };

    var hapusGrup<%=rnd%> = async (id, nama) => {
        if (!confirm('<%=Common.getBahasaConfigJS("Hapus grup")%> ' + nama + '?')) return;
        const hasil = await panggilGrupApi<%=rnd%>({ action: 'grup_produk_hapus', id: id });
        if (hasil.status === '00') {
            toastGrup<%=rnd%>('<%=Common.getBahasaConfigJS("Grup dihapus.")%>', 'bg-success text-white');
            muatGrup<%=rnd%>();
        } else {
            toastGrup<%=rnd%>(hasil.description || '<%=Common.getBahasaConfigJS("Gagal menghapus grup.")%>', 'bg-danger text-white');
        }
    };

    document.addEventListener("DOMContentLoaded", () => {
        const cari = document.getElementById('cariGrup<%=rnd%>');
        if (cari) cari.addEventListener("keydown", (e) => { if (e.key === "Enter") { e.preventDefault(); muatGrup<%=rnd%>(); } });
        muatGrup<%=rnd%>();
    });
</script>
