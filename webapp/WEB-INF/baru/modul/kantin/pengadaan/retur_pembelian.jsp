<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// Retur Pembelian: catat barang yang dikembalikan ke supplier (mis. rusak/tidak sesuai pesanan)
// atas Kulakan yang sudah tercatat. Menyusul pola & gerbang akses yang sama dengan
// pengadaan/index.jsp (Kulakan per-Faktur) -- endpoint backend: KantinHelper.returPembelianSimpan/
// returPembelianList/returPembelianHapus (aksi retur_pembelian_simpan/list/hapus).
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

boolean isAdmin = (toko == null);
String idTokoAktif = toko != null ? String.valueOf(toko.getId()) : "";
String namaTokoAktif = toko != null ? toko.getNama() : "";
// Gerbang SAMA PERSIS dengan KantinHelper.returPembelianSimpan/Hapus -- admin global ATAU
// pedagang berstatus Supervisor. Pedagang toko biasa hanya boleh melihat riwayat.
boolean bolehEntryRetur = isAdmin || (pedagang != null && Boolean.TRUE.equals(pedagang.getSupervisor()));
%>

<div id="viewListReturPembelian<%=rnd%>" class="animate__animated animate__fadeInUp">
	<div class="row mb-4">
		<div class="col-12">
			<div class="card border-0 shadow-sm rounded-4 border-top border-warning border-4">

				<div class="card-header bg-white border-0 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-3 border-bottom border-light">
					<div>
						<h5 class="fw-bold text-dark mb-1">
							<i class="fas fa-undo text-warning me-2"></i>
							<% if (isAdmin) { %>
								<%=Common.getBahasaConfig("Retur Pembelian (Semua Toko)")%>
							<% } else { %>
								<%=Common.getBahasaConfig("Retur Pembelian - ")%> <span class="text-warning"><%=namaTokoAktif%></span>
							<% } %>
						</h5>
						<small class="text-muted"><%=Common.getBahasaConfig("Catat barang yang dikembalikan ke supplier (rusak/tidak sesuai) dari Kulakan yang sudah tercatat.")%></small>
					</div>

					<% if (!bolehEntryRetur) { %>
					<span class="badge bg-secondary bg-opacity-10 text-secondary border px-3 py-2 fw-normal" title="<%=Common.getBahasaConfig("Hanya admin atau supervisor toko yang boleh mencatat Retur Pembelian.")%>">
						<i class="fas fa-lock me-1"></i><%=Common.getBahasaConfig("Lihat Saja (Non-Supervisor)")%>
					</span>
					<% } %>
				</div>

				<div class="card-body p-4">

					<% if (bolehEntryRetur) { %>
					<div class="bg-light p-3 rounded-3 border shadow-sm mb-4">
						<h6 class="fw-bold text-dark mb-3"><i class="fas fa-plus-circle text-warning me-2"></i><%=Common.getBahasaConfig("Tambah Retur Pembelian")%></h6>
						<div class="row g-3">
							<% if (isAdmin) { %>
							<div class="col-md-3">
								<label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Toko / Pedagang")%> <span class="text-danger">*</span></label>
								<select class="form-select fw-semibold" id="formTokoRetur<%=rnd%>" onchange="gantiTokoFormRetur<%=rnd%>()">
									<option value=""><%=Common.getBahasaConfig("-- Pilih Toko --")%></option>
								</select>
							</div>
							<% } else { %>
								<input type="hidden" id="formTokoRetur<%=rnd%>" value="<%=idTokoAktif%>">
							<% } %>

							<div class="col-md-<%=isAdmin ? "4" : "5"%>">
								<label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("No. Faktur Asal (opsional)")%></label>
								<input type="text" class="form-control" id="formKodeFakturAsalRetur<%=rnd%>" placeholder="<%=Common.getBahasaConfig("mis. INV-2026-001")%>">
							</div>

							<div class="col-md-<%=isAdmin ? "5" : "7"%>">
								<label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Supplier (opsional)")%></label>
								<div class="input-group shadow-sm">
									<span class="input-group-text bg-white border-end-0"><i class="fas fa-building text-warning"></i></span>
									<input type="text" class="form-control border-start-0 fw-bold" list="listSupplierRetur<%=rnd%>" id="inputCariSupplierRetur<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik nama supplier...")%>" autocomplete="off">
								</div>
								<datalist id="listSupplierRetur<%=rnd%>"></datalist>
								<input type="hidden" id="idSupplierSelectedRetur<%=rnd%>" value="">
							</div>
						</div>

						<hr class="my-3">

						<div class="row g-2 align-items-end">
							<div class="col-md-4">
								<label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Produk / Barang")%></label>
								<div class="input-group shadow-sm">
									<span class="input-group-text bg-white border-end-0"><i class="fas fa-box text-warning"></i></span>
									<input type="text" class="form-control border-start-0 fw-bold" list="listProdukRetur<%=rnd%>" id="inputCariProdukRetur<%=rnd%>" placeholder="<%=isAdmin ? Common.getBahasaConfig("Pilih Toko dulu...") : Common.getBahasaConfig("Ketik nama / kode produk...")%>" autocomplete="off" <%=isAdmin ? "disabled" : ""%>>
								</div>
								<datalist id="listProdukRetur<%=rnd%>"></datalist>
								<input type="hidden" id="idProdukSelectedRetur<%=rnd%>" value="">
							</div>
							<div class="col-md-1">
								<label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Qty")%></label>
								<input type="number" class="form-control fw-bold" id="itemQtyRetur<%=rnd%>" placeholder="0" min="0.01" step="0.01">
							</div>
							<div class="col-md-2">
								<label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Harga Satuan")%></label>
								<input type="number" class="form-control" id="itemHargaRetur<%=rnd%>" placeholder="0" min="0">
							</div>
							<div class="col-md-2">
								<label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Alasan")%></label>
								<input type="text" class="form-control" id="itemAlasanRetur<%=rnd%>" placeholder="<%=Common.getBahasaConfig("mis. Rusak")%>">
							</div>
							<div class="col-md-2">
								<label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Ket. (opsional)")%></label>
								<input type="text" class="form-control" id="itemKeteranganRetur<%=rnd%>" placeholder="-">
							</div>
							<div class="col-md-1">
								<button type="button" class="btn btn-warning text-white w-100 fw-bold" onclick="tambahItemRetur<%=rnd%>()" title="<%=Common.getBahasaConfig("Tambah ke Daftar")%>"><i class="fas fa-plus"></i></button>
							</div>
						</div>

						<div class="table-responsive border rounded-3 shadow-sm bg-white mt-3 mb-2">
							<table class="table table-sm table-striped align-middle mb-0">
								<thead class="table-light">
									<tr>
										<th class="text-start small"><%=Common.getBahasaConfig("Produk")%></th>
										<th class="text-end small"><%=Common.getBahasaConfig("Qty")%></th>
										<th class="text-end small"><%=Common.getBahasaConfig("Harga Satuan")%></th>
										<th class="text-end small"><%=Common.getBahasaConfig("Subtotal")%></th>
										<th class="text-start small"><%=Common.getBahasaConfig("Alasan")%></th>
										<th style="width:40px;"></th>
									</tr>
								</thead>
								<tbody id="tabelItemRetur<%=rnd%>">
									<tr><td colspan="6" class="text-center text-muted py-3 small"><%=Common.getBahasaConfig("Belum ada barang ditambahkan.")%></td></tr>
								</tbody>
								<tfoot>
									<tr class="table-light">
										<td colspan="3" class="text-end fw-bold"><%=Common.getBahasaConfig("Total Nilai Retur")%></td>
										<td class="text-end fw-bold text-danger" id="totalHitungRetur<%=rnd%>">Rp 0</td>
										<td colspan="2"></td>
									</tr>
								</tfoot>
							</table>
						</div>

						<div class="text-end">
							<button type="button" class="btn btn-warning text-white px-4 rounded-pill fw-bold shadow-sm" id="btnSimpanRetur<%=rnd%>" onclick="simpanRetur<%=rnd%>()">
								<i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Retur Pembelian")%>
							</button>
						</div>
					</div>
					<% } %>

					<h6 class="fw-bold text-dark mb-3"><i class="fas fa-history text-muted me-2"></i><%=Common.getBahasaConfig("Riwayat Retur Pembelian")%></h6>

					<div class="bg-light p-3 rounded-3 border shadow-sm mb-3">
						<div class="row g-2 align-items-end">
							<% if (isAdmin) { %>
							<div class="col-md-3">
								<label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Toko / Pedagang")%></label>
								<select class="form-select form-select-sm border-0 shadow-sm filter-input-retur-<%=rnd%>" id="filterTokoRetur<%=rnd%>">
									<option value=""><%=Common.getBahasaConfig("-- Pilih Toko --")%></option>
								</select>
							</div>
							<% } else { %>
								<input type="hidden" id="filterTokoRetur<%=rnd%>" value="<%=idTokoAktif%>">
							<% } %>
							<div class="col-md-4">
								<label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Cari (Produk / No. Faktur)")%></label>
								<input type="text" class="form-control form-control-sm border-0 shadow-sm filter-input-retur-<%=rnd%>" id="filterKeywordRetur<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik untuk mencari...")%>">
							</div>
							<div class="col-md-auto">
								<button class="btn btn-sm btn-secondary fw-bold px-3 shadow-sm" onclick="triggerSearchRetur<%=rnd%>()">
									<i class="fas fa-search me-1"></i> <%=Common.getBahasaConfig("Saring")%>
								</button>
							</div>
						</div>
					</div>

					<div class="table-responsive border rounded-3 shadow-sm bg-white">
						<table class="table table-hover table-striped align-middle mb-0">
							<thead class="table-dark text-center align-middle">
								<tr>
									<th class="fw-semibold text-uppercase small py-3" style="width: 50px;">#</th>
									<th class="fw-semibold text-uppercase small text-start"><%=Common.getBahasaConfig("Waktu")%></th>
									<th class="fw-semibold text-uppercase small text-start"><%=Common.getBahasaConfig("No. Faktur Asal")%></th>
									<th class="fw-semibold text-uppercase small text-start"><%=Common.getBahasaConfig("Produk")%></th>
									<th class="fw-semibold text-uppercase small text-end"><%=Common.getBahasaConfig("Qty")%></th>
									<th class="fw-semibold text-uppercase small text-end"><%=Common.getBahasaConfig("Total Nilai")%></th>
									<th class="fw-semibold text-uppercase small text-start"><%=Common.getBahasaConfig("Alasan")%></th>
									<th class="fw-semibold text-uppercase small text-start"><%=Common.getBahasaConfig("Oleh")%></th>
									<th class="fw-semibold text-uppercase small text-center" style="width: 60px;"><%=Common.getBahasaConfig("Aksi")%></th>
								</tr>
							</thead>
							<tbody id="tableRetur<%=rnd%>">
								<tr><td colspan="9" class="text-center py-5 text-muted"><%=isAdmin ? Common.getBahasaConfig("Pilih Toko di atas untuk melihat riwayat.") : Common.getBahasaConfig("Memuat...")%></td></tr>
							</tbody>
						</table>
					</div>

					<div class="d-flex justify-content-between align-items-center mt-4 pt-2">
						<span class="text-muted small fw-medium bg-light px-3 py-1 rounded-pill border" id="pagingInfoRetur<%=rnd%>"></span>
						<nav>
							<ul class="pagination pagination-sm mb-0 shadow-sm">
								<li class="page-item">
									<button class="page-link text-warning rounded-start-pill px-3" id="btnPrevRetur<%=rnd%>" onclick="changePageRetur<%=rnd%>(-1)"><i class="fas fa-chevron-left"></i></button>
								</li>
								<li class="page-item disabled">
									<span class="page-link fw-bold bg-light text-dark px-3" id="pageNumberRetur<%=rnd%>">1</span>
								</li>
								<li class="page-item">
									<button class="page-link text-warning rounded-end-pill px-3" id="btnNextRetur<%=rnd%>" onclick="changePageRetur<%=rnd%>(1)"><i class="fas fa-chevron-right"></i></button>
								</li>
							</ul>
						</nav>
					</div>

				</div>
			</div>
		</div>
	</div>
</div>

<script>
	// ==========================================
	// VARIABEL GLOBAL & UTILITIES
	// ==========================================
	const isAdminRetur<%=rnd%> = <%=isAdmin%>;
	const bolehEntryRetur<%=rnd%> = <%=bolehEntryRetur%>;
	const idTokoLoginRetur<%=rnd%> = '<%=idTokoAktif%>';

	let arrDataSupplierRetur<%=rnd%> = [];
	let arrDataProdukRetur<%=rnd%> = [];
	let itemsRetur<%=rnd%> = [];

	let currentPageRetur<%=rnd%> = 1;
	const limitPerPageRetur<%=rnd%> = 20;
	let totalRecordsRetur<%=rnd%> = 0;

	const formatRpRetur<%=rnd%> = (angka) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);

	const fetchDataRetur<%=rnd%> = async (payload) => {
		const res = await fetch('<%=Common.ROOT%>/Data', {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify(payload)
		});
		return await res.json();
	};

	const fetchSqlRetur<%=rnd%> = async (sql) => {
		try {
			const res = await fetchDataRetur<%=rnd%>({ sql: sql, action: "sql" });
			return res.data || [];
		} catch (e) {
			console.error("Fetch Error: ", e);
			return [];
		}
	};

	const showToastRetur<%=rnd%> = (msg, colorClass) => {
		if (typeof tampilkanToast === "function") { tampilkanToast(msg, colorClass); } else { alert(msg); }
	};

	<% if (bolehEntryRetur) { %>
	// ==========================================
	// COMBO TOKO (form entri, admin saja)
	// ==========================================
	const loadComboTokoFormRetur<%=rnd%> = async () => {
		if (!isAdminRetur<%=rnd%>) return;
		const rows = await fetchSqlRetur<%=rnd%>("SELECT id, nama FROM koperasi.toko WHERE aktif = true ORDER BY nama ASC");
		let opt = '<option value=""><%=Common.getBahasaConfigJS("-- Pilih Toko --")%></option>';
		rows.forEach(r => { opt += '<option value="' + r.id + '">' + r.nama + '</option>'; });
		const elForm = document.getElementById('formTokoRetur<%=rnd%>');
		if (elForm) elForm.innerHTML = opt;
	};

	// ==========================================
	// COMBO PRODUK (dinamis per Toko)
	// ==========================================
	const loadComboProdukRetur<%=rnd%> = async () => {
		const tokoId = isAdminRetur<%=rnd%> ? document.getElementById('formTokoRetur<%=rnd%>').value : idTokoLoginRetur<%=rnd%>;
		const input = document.getElementById('inputCariProdukRetur<%=rnd%>');
		const dt = document.getElementById('listProdukRetur<%=rnd%>');
		arrDataProdukRetur<%=rnd%> = [];
		document.getElementById('idProdukSelectedRetur<%=rnd%>').value = '';
		if (!tokoId) {
			input.value = '';
			input.disabled = true;
			input.placeholder = '<%=Common.getBahasaConfigJS("Pilih Toko dulu...")%>';
			return;
		}
		input.disabled = true;
		input.placeholder = '<%=Common.getBahasaConfigJS("Memuat data...")%>';
		const rows = await fetchSqlRetur<%=rnd%>("SELECT id, nama, kode FROM koperasi.produk WHERE aktif = true AND toko = " + tokoId + " ORDER BY nama ASC");
		let html = '';
		rows.forEach(item => {
			const kode = item.kode ? ' [' + item.kode + ']' : '';
			const namaLengkap = item.nama + kode;
			arrDataProdukRetur<%=rnd%>.push({ id: item.id, nama_lengkap: namaLengkap });
			html += '<option value="' + namaLengkap + '">';
		});
		dt.innerHTML = html;
		input.value = '';
		input.disabled = false;
		input.placeholder = '<%=Common.getBahasaConfigJS("Ketik nama / kode produk...")%>';
	};

	const gantiTokoFormRetur<%=rnd%> = () => {
		loadComboProdukRetur<%=rnd%>();
	};

	document.getElementById('inputCariProdukRetur<%=rnd%>').addEventListener('change', function () {
		const match = arrDataProdukRetur<%=rnd%>.find(p => p.nama_lengkap === this.value);
		document.getElementById('idProdukSelectedRetur<%=rnd%>').value = match ? match.id : '';
	});

	// ==========================================
	// SUPPLIER (search debounced, tanpa quick-add -- pola sederhana krn retur bukan pencatatan
	// supplier baru, cukup pilih dari yg sudah ada)
	// ==========================================
	let timerCariSupplierRetur<%=rnd%> = null;
	document.getElementById('inputCariSupplierRetur<%=rnd%>').addEventListener('input', function () {
		document.getElementById('idSupplierSelectedRetur<%=rnd%>').value = '';
		const kw = this.value.trim();
		clearTimeout(timerCariSupplierRetur<%=rnd%>);
		timerCariSupplierRetur<%=rnd%> = setTimeout(async () => {
			const res = await fetchDataRetur<%=rnd%>({ action: 'penyedia_list', keyword: kw });
			arrDataSupplierRetur<%=rnd%> = res.data || [];
			let html = '';
			arrDataSupplierRetur<%=rnd%>.forEach(s => { html += '<option value="' + s.nama + '">'; });
			document.getElementById('listSupplierRetur<%=rnd%>').innerHTML = html;
		}, 300);
	});
	document.getElementById('inputCariSupplierRetur<%=rnd%>').addEventListener('change', function () {
		const match = arrDataSupplierRetur<%=rnd%>.find(s => s.nama === this.value);
		document.getElementById('idSupplierSelectedRetur<%=rnd%>').value = match ? match.id : '';
	});

	// ==========================================
	// ITEM RETUR (lokal, sebelum Simpan)
	// ==========================================
	const renderItemRetur<%=rnd%> = () => {
		const tbody = document.getElementById('tabelItemRetur<%=rnd%>');
		if (itemsRetur<%=rnd%>.length === 0) {
			tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-3 small"><%=Common.getBahasaConfig("Belum ada barang ditambahkan.")%></td></tr>';
		} else {
			let html = '';
			itemsRetur<%=rnd%>.forEach((it, idx) => {
				const subtotal = it.qty * it.hargaSatuan;
				html += '<tr>' +
					'<td class="text-start fw-semibold">' + it.namaProduk + '</td>' +
					'<td class="text-end">' + it.qty + '</td>' +
					'<td class="text-end">' + formatRpRetur<%=rnd%>(it.hargaSatuan) + '</td>' +
					'<td class="text-end fw-bold text-danger">' + formatRpRetur<%=rnd%>(subtotal) + '</td>' +
					'<td class="text-start text-muted small">' + (it.alasan || '-') + '</td>' +
					'<td class="text-center"><button type="button" class="btn btn-sm btn-outline-danger" onclick="hapusItemRetur<%=rnd%>(' + idx + ')"><i class="fas fa-times"></i></button></td>' +
					'</tr>';
			});
			tbody.innerHTML = html;
		}
		const total = itemsRetur<%=rnd%>.reduce((a, it) => a + (it.qty * it.hargaSatuan), 0);
		document.getElementById('totalHitungRetur<%=rnd%>').textContent = formatRpRetur<%=rnd%>(total);
	};

	const tambahItemRetur<%=rnd%> = () => {
		const produkId = document.getElementById('idProdukSelectedRetur<%=rnd%>').value;
		const namaProduk = document.getElementById('inputCariProdukRetur<%=rnd%>').value.trim();
		const qty = parseFloat(document.getElementById('itemQtyRetur<%=rnd%>').value) || 0;
		const harga = parseFloat(document.getElementById('itemHargaRetur<%=rnd%>').value) || 0;
		const alasan = document.getElementById('itemAlasanRetur<%=rnd%>').value.trim();
		const ket = document.getElementById('itemKeteranganRetur<%=rnd%>').value.trim();

		if (!produkId) {
			showToastRetur<%=rnd%>('<%=Common.getBahasaConfigJS("Pilih produk yang valid dari daftar terlebih dahulu.")%>', 'bg-warning text-dark');
			return;
		}
		if (qty <= 0) {
			showToastRetur<%=rnd%>('<%=Common.getBahasaConfigJS("Jumlah (qty) harus lebih dari 0.")%>', 'bg-warning text-dark');
			return;
		}

		itemsRetur<%=rnd%>.push({ produkId: produkId, namaProduk: namaProduk, qty: qty, hargaSatuan: harga, alasan: alasan, keterangan: ket });
		renderItemRetur<%=rnd%>();

		document.getElementById('inputCariProdukRetur<%=rnd%>').value = '';
		document.getElementById('idProdukSelectedRetur<%=rnd%>').value = '';
		document.getElementById('itemQtyRetur<%=rnd%>').value = '';
		document.getElementById('itemHargaRetur<%=rnd%>').value = '';
		document.getElementById('itemAlasanRetur<%=rnd%>').value = '';
		document.getElementById('itemKeteranganRetur<%=rnd%>').value = '';
		document.getElementById('inputCariProdukRetur<%=rnd%>').focus();
	};

	const hapusItemRetur<%=rnd%> = (idx) => {
		itemsRetur<%=rnd%>.splice(idx, 1);
		renderItemRetur<%=rnd%>();
	};

	// ==========================================
	// SIMPAN RETUR
	// ==========================================
	const simpanRetur<%=rnd%> = async () => {
		if (itemsRetur<%=rnd%>.length === 0) {
			showToastRetur<%=rnd%>('<%=Common.getBahasaConfigJS("Tambahkan minimal satu barang ke daftar retur.")%>', 'bg-warning text-dark');
			return;
		}

		const kodeFakturAsal = document.getElementById('formKodeFakturAsalRetur<%=rnd%>').value.trim();
		const supplierId = document.getElementById('idSupplierSelectedRetur<%=rnd%>').value;

		const btn = document.getElementById('btnSimpanRetur<%=rnd%>');
		const asliBtn = btn.innerHTML;
		btn.disabled = true;
		btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyimpan...")%>';

		const payload = {
			action: 'retur_pembelian_simpan',
			kode_faktur_asal: kodeFakturAsal,
			items: itemsRetur<%=rnd%>.map(it => ({
				produk_id: it.produkId,
				qty: it.qty,
				harga_satuan: it.hargaSatuan,
				alasan: it.alasan,
				keterangan: it.keterangan
			}))
		};
		if (supplierId) payload.supplier_id = supplierId;

		try {
			const res = await fetchDataRetur<%=rnd%>(payload);
			if (res.status === '00') {
				showToastRetur<%=rnd%>('<%=Common.getBahasaConfigJS("Retur Pembelian berhasil disimpan.")%>', 'bg-success text-white');
				document.getElementById('formKodeFakturAsalRetur<%=rnd%>').value = '';
				document.getElementById('inputCariSupplierRetur<%=rnd%>').value = '';
				document.getElementById('idSupplierSelectedRetur<%=rnd%>').value = '';
				itemsRetur<%=rnd%> = [];
				renderItemRetur<%=rnd%>();
				const tokoAktifRiwayat = document.getElementById('filterTokoRetur<%=rnd%>').value;
				const tokoDipakai = isAdminRetur<%=rnd%> ? document.getElementById('formTokoRetur<%=rnd%>').value : idTokoLoginRetur<%=rnd%>;
				if (isAdminRetur<%=rnd%> && !tokoAktifRiwayat && tokoDipakai) {
					document.getElementById('filterTokoRetur<%=rnd%>').value = tokoDipakai;
				}
				loadDataReturAPI<%=rnd%>();
			} else {
				showToastRetur<%=rnd%>(res.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan retur pembelian.")%>', 'bg-danger text-white');
			}
		} catch (e) {
			showToastRetur<%=rnd%>('<%=Common.getBahasaConfigJS("Koneksi gagal. Silakan coba lagi.")%>', 'bg-danger text-white');
		} finally {
			btn.disabled = false;
			btn.innerHTML = asliBtn;
		}
	};
	<% } %>

	// ==========================================
	// COMBO TOKO (filter riwayat, admin saja)
	// ==========================================
	const loadComboTokoFilterRetur<%=rnd%> = async () => {
		if (!isAdminRetur<%=rnd%>) return;
		const rows = await fetchSqlRetur<%=rnd%>("SELECT id, nama FROM koperasi.toko WHERE aktif = true ORDER BY nama ASC");
		let opt = '<option value=""><%=Common.getBahasaConfigJS("-- Pilih Toko --")%></option>';
		rows.forEach(r => { opt += '<option value="' + r.id + '">' + r.nama + '</option>'; });
		const elFilter = document.getElementById('filterTokoRetur<%=rnd%>');
		if (elFilter) elFilter.innerHTML = opt;
	};

	// ==========================================
	// RIWAYAT (paginated)
	// ==========================================
	const triggerSearchRetur<%=rnd%> = () => {
		currentPageRetur<%=rnd%> = 1;
		loadDataReturAPI<%=rnd%>();
	};

	const loadDataReturAPI<%=rnd%> = async () => {
		const tbody = document.getElementById('tableRetur<%=rnd%>');
		const tokoId = document.getElementById('filterTokoRetur<%=rnd%>').value;
		if (isAdminRetur<%=rnd%> && !tokoId) {
			tbody.innerHTML = '<tr><td colspan="9" class="text-center py-5 text-muted"><%=Common.getBahasaConfig("Pilih Toko di atas untuk melihat riwayat.")%></td></tr>';
			document.getElementById('pagingInfoRetur<%=rnd%>').innerHTML = '';
			return;
		}
		tbody.innerHTML = '<tr><td colspan="9" class="text-center py-5 text-muted"><div class="spinner-border text-warning mb-3"></div><br><%=Common.getBahasaConfig("Memuat riwayat...")%></td></tr>';

		const keyword = document.getElementById('filterKeywordRetur<%=rnd%>').value.trim();

		const payload = {
			action: 'retur_pembelian_list',
			keyword: keyword,
			page: currentPageRetur<%=rnd%>,
			page_size: limitPerPageRetur<%=rnd%>
		};
		if (tokoId) payload.toko_id = tokoId;

		try {
			const res = await fetchDataRetur<%=rnd%>(payload);
			const rows = res.data || [];
			totalRecordsRetur<%=rnd%> = res.total || 0;

			if (rows.length === 0) {
				tbody.innerHTML = '<tr><td colspan="9" class="text-center text-muted py-5"><i class="fas fa-undo fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Belum ada data retur pembelian.")%></td></tr>';
			} else {
				let no = (currentPageRetur<%=rnd%> - 1) * limitPerPageRetur<%=rnd%> + 1;
				let html = '';
				rows.forEach(r => {
					html += '<tr>' +
						'<td class="text-center text-muted">' + no++ + '</td>' +
						'<td class="fw-medium text-secondary">' + (r.waktu || '-') + '</td>' +
						'<td class="fw-bold">' + (r.kodeFakturAsal || '-') + '</td>' +
						'<td class="text-dark">' + (r.namaProduk || '-') + '</td>' +
						'<td class="text-end fw-bold">' + (r.qty || 0) + '</td>' +
						'<td class="text-end fw-bold text-danger">' + formatRpRetur<%=rnd%>(r.totalNilai) + '</td>' +
						'<td class="text-start text-muted small">' + (r.alasan || '-') + '</td>' +
						'<td class="text-start text-muted small">' + (r.oleh || '-') + '</td>' +
						'<td class="text-center">' + (bolehEntryRetur<%=rnd%> ?
							'<button class="btn btn-sm btn-outline-danger shadow-sm" onclick="hapusReturRiwayat<%=rnd%>(' + r.id + ')" title="<%=Common.getBahasaConfig("Hapus")%>"><i class="fas fa-trash"></i></button>' : '-') +
						'</td>' +
						'</tr>';
				});
				tbody.innerHTML = html;
			}
			updatePaginationRetur<%=rnd%>();
		} catch (error) {
			console.error(error);
			tbody.innerHTML = '<tr><td colspan="9" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal mengambil data.")%></td></tr>';
		}
	};

	const updatePaginationRetur<%=rnd%> = () => {
		const totalPages = Math.ceil(totalRecordsRetur<%=rnd%> / limitPerPageRetur<%=rnd%>) || 1;
		document.getElementById('pageNumberRetur<%=rnd%>').innerText = currentPageRetur<%=rnd%> + " / " + totalPages;

		const startIdx = (currentPageRetur<%=rnd%> - 1) * limitPerPageRetur<%=rnd%> + 1;
		const endIdx = Math.min(currentPageRetur<%=rnd%> * limitPerPageRetur<%=rnd%>, totalRecordsRetur<%=rnd%>);
		const startInfo = totalRecordsRetur<%=rnd%> > 0 ? startIdx : 0;

		document.getElementById('pagingInfoRetur<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-warning me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startInfo + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari")%> <b class="text-dark">' + totalRecordsRetur<%=rnd%> + '</b> <%=Common.getBahasaConfig("data")%>';

		document.getElementById('btnPrevRetur<%=rnd%>').disabled = (currentPageRetur<%=rnd%> === 1);
		document.getElementById('btnNextRetur<%=rnd%>').disabled = (currentPageRetur<%=rnd%> === totalPages);
	};

	const changePageRetur<%=rnd%> = (direction) => {
		const totalPages = Math.ceil(totalRecordsRetur<%=rnd%> / limitPerPageRetur<%=rnd%>);
		if (direction === -1 && currentPageRetur<%=rnd%> > 1) {
			currentPageRetur<%=rnd%>--;
			loadDataReturAPI<%=rnd%>();
		} else if (direction === 1 && currentPageRetur<%=rnd%> < totalPages) {
			currentPageRetur<%=rnd%>++;
			loadDataReturAPI<%=rnd%>();
		}
	};

	<% if (bolehEntryRetur) { %>
	const hapusReturRiwayat<%=rnd%> = async (id) => {
		if (!confirm('<%=Common.getBahasaConfigJS("Hapus data retur pembelian ini? Stok produk akan disesuaikan kembali.")%>')) return;
		try {
			const res = await fetchDataRetur<%=rnd%>({ action: 'retur_pembelian_hapus', id: id });
			if (res.status === '00') {
				showToastRetur<%=rnd%>('<%=Common.getBahasaConfigJS("Data retur berhasil dihapus.")%>', 'bg-success text-white');
				loadDataReturAPI<%=rnd%>();
			} else {
				showToastRetur<%=rnd%>(res.description || '<%=Common.getBahasaConfigJS("Gagal menghapus data retur.")%>', 'bg-danger text-white');
			}
		} catch (e) {
			showToastRetur<%=rnd%>('<%=Common.getBahasaConfigJS("Koneksi gagal. Silakan coba lagi.")%>', 'bg-danger text-white');
		}
	};
	<% } %>

	// ==========================================
	// INISIALISASI HALAMAN
	// ==========================================
	document.addEventListener("DOMContentLoaded", () => {
		<% if (bolehEntryRetur) { %>
		loadComboTokoFormRetur<%=rnd%>();
		if (!isAdminRetur<%=rnd%>) {
			loadComboProdukRetur<%=rnd%>();
		}
		<% } %>
		loadComboTokoFilterRetur<%=rnd%>();

		const filterInputs = document.querySelectorAll('.filter-input-retur-<%=rnd%>');
		filterInputs.forEach(input => {
			if (input.tagName === 'SELECT') {
				input.addEventListener("change", () => triggerSearchRetur<%=rnd%>());
			} else {
				input.addEventListener("keydown", (event) => {
					if (event.key === "Enter") {
						event.preventDefault();
						triggerSearchRetur<%=rnd%>();
					}
				});
			}
		});

		if (!isAdminRetur<%=rnd%>) {
			loadDataReturAPI<%=rnd%>();
		}
	});
</script>
