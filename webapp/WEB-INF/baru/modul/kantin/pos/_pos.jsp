<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%
//2. SECURITY CHECK

Boolean posTanpaLogin = Common.getKonfigurasi("pos_tanpa_login", Konfigurasi.TIDAK_AKTIF).getNilai().equalsIgnoreCase(Konfigurasi.AKTIF);

Tbmuser tbmuser = Common.getCurrentUser(request);
if(!posTanpaLogin){
	if (tbmuser == null || tbmuser.getUserId() == null) {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		out.print("{\"status\":\"error\", \"message\":\""
		+ Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
		return;
	}
}

// Fitur "Top Up Saldo lewat POS" -- tombol hanya tampil bila kasir punya hak Tbmrole.getBolehEntryTopup(),
// gerbang YANG SAMA dgn menu "Manajemen Saldo (Deposit)" terpisah (_manajemen_topup.jsp).
ais.database.model.Tbmrole roleTopupPOS = tbmuser == null ? null : tbmuser.hakAkses();
boolean bolehTopupPOS = roleTopupPOS != null && roleTopupPOS.getBolehEntryTopup() != null
		&& roleTopupPOS.getBolehEntryTopup().booleanValue();

// PENGAMANAN: Cek null pada tbmuser agar tidak terjadi NullPointerException saat mode posTanpaLogin aktif
Pedagang pedagang = tbmuser == null ? null : tbmuser.getPedagang();
Toko toko = pedagang == null ? null : pedagang.getToko();
String rnd = request.getParameter("rnd");
if(rnd != null){
	rnd = Common.getGeneratedBarCode(7);
}
// Menyiapkan flag dan variabel filter untuk JavaScript
boolean isAdmin = (toko == null);
String idTokoAktif = toko != null ? String.valueOf(toko.getId()) : "";
String namaTokoAktif = toko != null ? toko.getNama() : "";

// Filter SQL khusus untuk riwayat dan katalog
String sqlFilterToko = "";
if (toko != null) {
    sqlFilterToko = " AND a.toko = " + toko.getId() + " ";
}

// Persentase pajak/PPN kantin -- SAMA PERSIS dgn sumber & cara baca versi ZK (PosKantinAction.
// bacaPajakPersen()) agar kedua versi POS konsisten mengenakan pajak yg sama. Nilai ini nominal
// PERSEN (bukan nominal rupiah) -- nominal rupiah dihitung di JS saat render keranjang.
double pajakPersenPOS = 0;
try {
    String vPajak = Common.getKonfigurasi("pajak_persen_kantin", "0").getNilai();
    if (vPajak != null) {
        vPajak = vPajak.replaceAll("[^0-9.]", "");
    }
    if (vPajak != null && vPajak.length() > 0) {
        pajakPersenPOS = Double.parseDouble(vPajak);
    }
} catch (Exception exPajak) { pajakPersenPOS = 0; }
if (pajakPersenPOS < 0) { pajakPersenPOS = 0; }

// Mengambil Informasi Perguruan Tinggi
PerguruanTinggi pt = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request);
String judulNamaPerguruanTinggi = pt != null && pt.getNama() != null ? pt.getNama() : "";
String Telepon = pt != null && pt.getTelepon() != null ? pt.getTelepon() : "";
String Motto = pt != null && pt.getMotto() != null ? pt.getMotto() : "";
String Alamat1 = pt != null && pt.getAlamat1() != null ? pt.getAlamat1() : "";
String Email = pt != null && pt.getEmail() != null ? pt.getEmail() : "";
String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_"); 
%>

<style>
    .page-bg-<%=rnd%> { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: url('<%=Common.ROOT%>/img/pmb_bg.jpg') center/cover no-repeat; opacity: 0.1; z-index: -999; pointer-events: none; }
    .hero-section-<%=rnd%> { background-color: #0d6efd; background: linear-gradient(rgba(13, 110, 253, 0.85), rgba(10, 88, 202, 0.85)), url('<%=Common.ROOT%>/img/pmb_bg.jpg') center/cover no-repeat; color: white; border-radius: 0 0 2rem 2rem; box-shadow: 0 4px 15px rgba(0,0,0,0.1); position: relative; z-index: 2; padding: 2rem; margin-bottom: 1.5rem; }

    /* ===== Redesain gaya "NexaPOS" (kategori pill, kartu produk, cart, modal bayar) ===== */
    .pill-kat-<%=rnd%> { border: 1px solid #e2e8f0; background: #fff; color: #334155; border-radius: 999px; padding: 7px 16px; font-weight: 700; font-size: 12.5px; cursor: pointer; transition: .12s; white-space: nowrap; flex-shrink: 0; }
    #kategoriPills<%=rnd%>::-webkit-scrollbar { height: 5px; }
    #kategoriPills<%=rnd%>::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 999px; }
    #kategoriPills<%=rnd%>::-webkit-scrollbar-track { background: transparent; }
    .pill-kat-<%=rnd%>:hover { border-color: #0d6efd; }
    .pill-kat-<%=rnd%>.active { background: #0d6efd; border-color: #0d6efd; color: #fff; box-shadow: 0 4px 10px rgba(13,110,253,.3); }
    .prod-card-<%=rnd%> { background: #fff; border: 1px solid #eef2f7; border-radius: 16px; overflow: hidden; cursor: pointer; transition: transform .12s, box-shadow .12s, border-color .12s; height: 100%; }
    .prod-card-<%=rnd%>:hover { transform: translateY(-3px); box-shadow: 0 10px 22px rgba(15,23,42,.12); border-color: #bfdbfe; }
    .prod-thumb-<%=rnd%> { position: relative; width: 100%; height: 96px; background: rgba(13,110,253,.07); color: #0d6efd; display: flex; align-items: center; justify-content: center; font-size: 30px; overflow: hidden; }
    .prod-thumb-<%=rnd%> img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
    .prod-badge-diskon-<%=rnd%> { position: absolute; top: 6px; left: 6px; background: #dc3545; color: #fff; font-size: 10px; font-weight: 800; padding: 2px 7px; border-radius: 999px; z-index: 2; }
    .prod-body-<%=rnd%> { padding: 10px 10px 12px; }
    .prod-nm-<%=rnd%> { font-weight: 700; font-size: 12.5px; color: #0f172a; min-height: 32px; line-height: 1.3; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
    .prod-foot-<%=rnd%> { display: flex; align-items: center; justify-content: space-between; margin-top: 6px; }
    .prod-pr-<%=rnd%> { font-weight: 800; color: #0d6efd; font-size: 13.5px; }
    .prod-pr-wrap-<%=rnd%> { display: flex; flex-direction: column; line-height: 1.15; }
    .prod-pr-coret-<%=rnd%> { font-size: 10.5px; color: #94a3b8; text-decoration: line-through; font-weight: 600; }
    .prod-pr-promo-<%=rnd%> { font-weight: 800; color: #dc3545; font-size: 13.5px; }
    .prod-add-<%=rnd%> { width: 26px; height: 26px; border-radius: 8px; background: #0d6efd; color: #fff; font-weight: 900; display: flex; align-items: center; justify-content: center; font-size: 16px; box-shadow: 0 4px 10px rgba(13,110,253,.35); flex-shrink: 0; }
    .prod-card-<%=rnd%>:hover .prod-add-<%=rnd%> { background: #0b5ed7; }

    .cart-item-<%=rnd%> { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-bottom: 1px solid #f1f5f9; }
    .cart-item-<%=rnd%>:last-child { border-bottom: none; }
    .cart-thumb-<%=rnd%> { width: 40px; height: 40px; border-radius: 10px; background: rgba(13,110,253,.08); color: #0d6efd; display: flex; align-items: center; justify-content: center; font-size: 15px; flex-shrink: 0; }
    .qty-pill-<%=rnd%> { display: flex; align-items: center; gap: 6px; background: #f1f5f9; border-radius: 999px; padding: 3px; }
    .qty-btn-<%=rnd%> { width: 22px; height: 22px; border-radius: 50%; background: #fff; border: 1px solid #e2e8f0; font-weight: 800; display: flex; align-items: center; justify-content: center; cursor: pointer; font-size: 13px; line-height: 1; }
    .qty-btn-<%=rnd%>:hover { background: #0d6efd; color: #fff; border-color: #0d6efd; }

    .paymethod-card-<%=rnd%> { position: relative; border: 1px solid #e2e8f0; border-radius: 14px; padding: 14px; cursor: pointer; transition: .12s; background: #fff; height: 100%; }
    .paymethod-card-<%=rnd%>:hover { border-color: #0d6efd; box-shadow: 0 4px 12px rgba(13,110,253,.12); }
    .paymethod-card-<%=rnd%>.sel { border-color: #0d6efd; background: rgba(13,110,253,.06); box-shadow: 0 0 0 2px rgba(13,110,253,.2) inset; }
    .paymethod-card-<%=rnd%>.split-sel { border-color: #198754; box-shadow: 0 0 0 2px rgba(25,135,84,.25) inset; }
    .paymethod-ic-<%=rnd%> { width: 38px; height: 38px; border-radius: 10px; background: rgba(13,110,253,.1); color: #0d6efd; display: flex; align-items: center; justify-content: center; font-size: 17px; margin-bottom: 8px; }
    .paymethod-split-chk-<%=rnd%> { position: absolute; top: 8px; right: 8px; width: 22px; height: 22px; border-radius: 6px; border: 1.5px solid #cbd5e1; background: #fff; display: flex; align-items: center; justify-content: center; font-size: 11px; color: transparent; }
    .paymethod-split-chk-<%=rnd%>:hover { border-color: #198754; }
    .paymethod-split-chk-<%=rnd%>.checked { background: #198754; border-color: #198754; color: #fff; }

    /* ===== Responsif ponsel: kartu produk lebih rapat, teks/khusus komponen dikecilkan sedikit ===== */
    @media (max-width: 576px) {
        .prod-thumb-<%=rnd%> { height: 76px; font-size: 24px; }
        .prod-nm-<%=rnd%> { font-size: 11.5px; min-height: 28px; }
        .prod-pr-<%=rnd%> { font-size: 12px; }
        .pill-kat-<%=rnd%> { padding: 6px 12px; font-size: 11.5px; }
        .cart-thumb-<%=rnd%> { width: 34px; height: 34px; font-size: 13px; }
        .paymethod-card-<%=rnd%> { padding: 10px; }
    }
</style>

<% if (tbmuser == null || tbmuser.getUserId() == null) { %>
<!-- Tampilan Header Saat Mode Demo (Belum Login) -->
<div class="page-bg-<%=rnd%>"></div>
<div class="hero-section-<%=rnd%> text-center animate__animated animate__fadeInDown">
    <img src="<%=logo_PerguruanTinggi%>" onerror="this.onerror=null; this.src='<%=Common.ROOT%>/img/default_logo.png';" alt="Logo" style="height: 80px; margin-bottom: 1rem; filter: drop-shadow(0px 4px 6px rgba(0,0,0,0.3));">
    <h2 class="fw-bold mb-1"><%=judulNamaPerguruanTinggi%></h2>
    <p class="fst-italic mb-3 opacity-75"><%=Motto%></p>
    <div class="d-flex justify-content-center flex-wrap gap-3 small">
        <span><i class="fas fa-map-marker-alt me-1"></i><%=Alamat1%></span>
        <span><i class="fas fa-phone me-1"></i><%=Telepon%></span>
        <span><i class="fas fa-envelope me-1"></i><%=Email%></span>
    </div>
</div>
<% } %>

<div class="row g-3 mb-4 animate__animated animate__fadeInUp">
	<div class="col-12">
		<div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4 bg-light">
			<div class="card-body p-3">
				<div class="row g-3">

					<% if (isAdmin) { %>
					<div class="col-md-4">
						<label class="form-label small fw-bold text-secondary mb-1"><i class="fas fa-store me-1"></i><%=Common.getBahasaConfig("Pilih Toko/Pedagang")%></label>
						<input class="form-control form-control-sm border-0 shadow-sm fw-semibold" list="listToko<%=rnd%>" id="inputToko<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik nama toko...")%>" autocomplete="off">
						<datalist id="listToko<%=rnd%>"></datalist>
						<input type="hidden" id="idTokoSelected<%=rnd%>">
					</div>
					<% } else { %>
					    <input type="hidden" id="idTokoSelected<%=rnd%>" value="<%=idTokoAktif%>">
					<% } %>

					<div class="col-md-4">
						<label class="form-label small fw-bold text-secondary mb-1"><i class="fas fa-user-tag me-1"></i><%=Common.getBahasaConfig("Pilih Member (Pelanggan)")%></label>
						<input class="form-control form-control-sm border-0 shadow-sm fw-semibold" list="listMember<%=rnd%>" id="inputMember<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik nama atau ID...")%>" autocomplete="off">
						<datalist id="listMember<%=rnd%>"></datalist>
						<input type="hidden" id="idMemberSelected<%=rnd%>">
                        <!-- Ditambahkan input tersembunyi untuk menyimpan minimal saldo -->
                        <input type="hidden" id="minSaldoMemberSelected<%=rnd%>" value="0">
                        <!-- Wajib PIN (dari jenis anggota) -- gate verifikasi PIN pembeli di Layar Pelanggan -->
                        <input type="hidden" id="wajibPinMemberSelected<%=rnd%>" value="false">
                        <!-- Fitur "Cek Saldo": tampilkan saldo member LANGSUNG saat dipilih, tanpa perlu mulai checkout dulu -->
                        <small class="fw-bold text-success d-none" id="infoSaldoMember<%=rnd%>"></small>
                        <% if (bolehTopupPOS) { %>
                        <!-- Fitur "Top Up Saldo lewat POS": hanya utk kasir dgn hak Boleh Entry Topup, tampil hanya saat member dipilih -->
                        <div>
                            <a href="javascript:void(0)" class="small fw-bold text-primary d-none" id="lnkTopupMember<%=rnd%>" onclick="toggleFormTopupMember<%=rnd%>()">+ <%=Common.getBahasaConfig("Top Up Saldo")%></a>
                        </div>
                        <div class="d-none border rounded-3 p-2 mt-1 bg-white" id="formTopupMemberWrap<%=rnd%>">
                            <div class="row g-2 align-items-end">
                                <div class="col-6">
                                    <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Nominal (Rp)")%></label>
                                    <input type="number" min="1" class="form-control form-control-sm" id="inputNominalTopupMember<%=rnd%>">
                                </div>
                                <div class="col-6">
                                    <button type="button" class="btn btn-success btn-sm w-100" id="btnSubmitTopupMember<%=rnd%>" onclick="submitTopupMember<%=rnd%>()"><%=Common.getBahasaConfig("Top Up")%></button>
                                </div>
                                <div class="col-12">
                                    <input type="text" class="form-control form-control-sm" id="inputKetTopupMember<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Keterangan (opsional)")%>">
                                </div>
                            </div>
                        </div>
                        <% } %>
					</div>

					<div class="col-md-4">
						<label class="form-label small fw-bold text-secondary mb-1"><i class="fas fa-wallet me-1"></i><%=Common.getBahasaConfig("Metode Pembayaran")%></label>
						<select class="d-none" id="selectCaraBayar<%=rnd%>" onchange="handleCaraBayarChange<%=rnd%>()">
							<option value=""><%=Common.getBahasaConfig("Memuat data...")%></option>
						</select>
						<button type="button" class="form-control form-control-sm border-0 shadow-sm fw-semibold text-start bg-white d-flex justify-content-between align-items-center" id="btnPilihMetodeBayar<%=rnd%>" data-bs-toggle="modal" data-bs-target="#modalMetodeBayar<%=rnd%>">
							<span id="lblMetodeBayarTerpilih<%=rnd%>" class="text-muted"><%=Common.getBahasaConfig("Memuat data...")%></span>
							<i class="fas fa-chevron-down small text-muted"></i>
						</button>
					</div>

				</div>
			</div>
		</div>
	</div>
</div>

<!-- Fitur "Sesi Kasir": kasir wajib membuka kas (modal awal) sebelum bertransaksi (bila toko mengaktifkan
     gerbangnya), lalu menutup kas di akhir shift. Chip status + tombol Buka/Tutup Kas ada di sini supaya
     kasir tak perlu berpindah menu; mesin perhitungan (SesiKasUtil) sama persis dgn versi ZK/menu Kas Kasir. -->
<div class="row g-3 mb-4 animate__animated animate__fadeInUp">
	<div class="col-12">
		<div class="card border-0 shadow-sm rounded-4">
			<div class="card-body p-3">
				<div class="d-flex justify-content-between align-items-center flex-wrap gap-2">
					<div class="d-flex align-items-center gap-2">
						<i class="fas fa-cash-register text-primary"></i>
						<span class="fw-bold small" id="lblStatusSesiKas<%=rnd%>"><%=Common.getBahasaConfig("Memeriksa sesi kas...")%></span>
					</div>
					<button type="button" class="btn btn-sm btn-primary" id="btnToggleSesiKas<%=rnd%>" onclick="toggleFormSesiKas<%=rnd%>()"><%=Common.getBahasaConfig("Memuat...")%></button>
				</div>
				<div class="border-top mt-3 pt-3 d-none" id="formSesiKasWrap<%=rnd%>"></div>
			</div>
		</div>
	</div>
</div>

<div class="row g-4 animate__animated animate__fadeInUp">

	<div class="col-lg-7">
		<div class="card border-0 shadow-sm rounded-4 h-100 bg-light">
			<div class="card-body p-3">
				<div class="input-group input-group-lg mb-3 shadow-sm rounded-pill overflow-hidden">
					<span class="input-group-text bg-white border-0"><i class="fas fa-barcode text-primary"></i></span>
                    <input type="text" class="form-control border-0 bg-white shadow-none fs-6" id="searchProduk<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Scan Barcode atau Cari Nama Produk...")%>" autofocus>
					<span class="input-group-text bg-white border-0 text-muted small d-none d-md-flex" title="<%=Common.getBahasaConfig("Tekan F2 dari mana saja utk fokus ke sini")%>">F2</span>
					<button class="btn btn-primary px-4 fw-bold" onclick="loadKatalogProduk<%=rnd%>()"><i class="fas fa-search me-2"></i><%=Common.getBahasaConfig("Cari")%></button>
				</div>

				<div class="d-flex flex-nowrap gap-2 mb-3 pb-1" id="kategoriPills<%=rnd%>" style="overflow-x:auto;overflow-y:hidden;-ms-overflow-style:none;scrollbar-width:thin;">
					<span class="text-muted small py-2"><%=Common.getBahasaConfig("Memuat kategori...")%></span>
				</div>

				<div class="row g-3" id="gridProduk<%=rnd%>" style="max-height: 500px; overflow-y: auto;">
					<div class="col-12 text-center py-5 text-muted">
						<div class="spinner-border text-primary mb-3"></div><br>
                        <span><%=Common.getBahasaConfig("Memuat katalog produk...")%></span>
					</div>
				</div>
			</div>
		</div>
	</div>

	<div class="col-lg-5">
		<div class="card border-0 shadow-sm rounded-4 h-100 d-flex flex-column">
			<div class="card-header bg-white border-bottom-0 pt-4 pb-2 px-4 d-flex justify-content-between align-items-center flex-nowrap">
				<h5 class="fw-bold text-dark mb-0 text-truncate"><i class="fas fa-shopping-cart text-primary me-2"></i><%=Common.getBahasaConfig("Keranjang")%></h5>
				<div class="d-flex gap-2 flex-nowrap flex-shrink-0">
					<button class="btn btn-sm btn-outline-info rounded-circle d-flex align-items-center justify-content-center" style="width:34px;height:34px;padding:0;" data-bs-toggle="modal" data-bs-target="#modalBantuanPOS<%=rnd%>" title="<%=Common.getBahasaConfig("Bantuan POS lengkap (F1)")%>">
						<i class="fas fa-question"></i>
					</button>
					<button class="btn btn-sm btn-outline-success rounded-circle d-flex align-items-center justify-content-center" style="width:34px;height:34px;padding:0;" data-bs-toggle="modal" data-bs-target="#modalQAPOS<%=rnd%>" onclick="siapkanQAPOS<%=rnd%>()" title="<%=Common.getBahasaConfig("Tanya Jawab sesuai halaman POS")%>">
						<i class="fas fa-comments"></i>
					</button>
					<button class="btn btn-sm btn-outline-secondary rounded-circle position-relative d-flex align-items-center justify-content-center" style="width:34px;height:34px;padding:0;" data-bs-toggle="modal" data-bs-target="#modalKeranjangTertahan<%=rnd%>" onclick="loadDaftarTertahan<%=rnd%>()" title="<%=Common.getBahasaConfig("Keranjang Tertahan")%>">
						<i class="fas fa-inbox"></i>
						<span class="badge bg-danger rounded-pill d-none" id="badgeTertahan<%=rnd%>" style="font-size:8px;position:absolute;top:-3px;right:-3px;">0</span>
					</button>
					<button class="btn btn-sm btn-outline-warning rounded-circle d-flex align-items-center justify-content-center" style="width:34px;height:34px;padding:0;" data-bs-toggle="modal" data-bs-target="#modalPromoManual<%=rnd%>" onclick="bukaPickerPromoManual<%=rnd%>()" title="<%=Common.getBahasaConfig("Pilih Promo")%>">
						<i class="fas fa-tags"></i>
					</button>
					<button class="btn btn-sm btn-outline-primary rounded-circle d-flex align-items-center justify-content-center" style="width:34px;height:34px;padding:0;" onclick="bukaLayarPelanggan<%=rnd%>()" title="<%=Common.getBahasaConfig("Layar Pelanggan")%>">
						<i class="fas fa-tv"></i>
					</button>
					<button class="btn btn-sm btn-outline-danger rounded-circle d-flex align-items-center justify-content-center" style="width:34px;height:34px;padding:0;" onclick="kosongkanKeranjang<%=rnd%>()" title="<%=Common.getBahasaConfig("Kosongkan Keranjang")%>">
						<i class="fas fa-trash-alt"></i>
					</button>
				</div>
			</div>

			<div class="card-body p-0 d-flex flex-column flex-grow-1">
				<div class="px-4" style="flex-grow: 1; min-height: 200px; max-height: 320px; overflow-y: auto;" id="cartContainer<%=rnd%>">
					<div class="text-center text-muted py-4"><i class="fas fa-shopping-basket fa-2x mb-2 opacity-50"></i><br><%=Common.getBahasaConfig("Keranjang masih kosong")%></div>
				</div>

				<div class="bg-light p-4 rounded-bottom-4 mt-auto border-top border-2">
					<div class="d-flex justify-content-between mb-1">
						<span class="text-secondary fw-semibold"><%=Common.getBahasaConfig("Subtotal")%></span>
						<span class="text-dark fw-bold" id="cartSubtotal<%=rnd%>">Rp 0</span>
					</div>
					<div class="d-flex justify-content-between mb-1">
						<span class="text-secondary fw-semibold"><%=Common.getBahasaConfig("Diskon Promo")%></span>
						<span class="text-danger fw-bold" id="cartDiskon<%=rnd%>">- Rp 0</span>
					</div>
                    <div class="d-flex justify-content-between mb-1">
						<span class="text-secondary fw-semibold"><%=Common.getBahasaConfig("Cashback Didapat")%></span>
						<span class="text-info fw-bold" id="cartCashback<%=rnd%>">+ Rp 0</span>
					</div>
					<div class="d-flex justify-content-between mb-3 border-bottom pb-3" id="rowPajak<%=rnd%>" style="display:none;">
						<span class="text-secondary fw-semibold"><%=Common.getBahasaConfig("Pajak")%> (<span id="lblPajakPersen<%=rnd%>"><%=pajakPersenPOS%></span>%)</span>
						<span class="text-dark fw-bold" id="cartPajak<%=rnd%>">Rp 0</span>
					</div>
					<div class="d-flex justify-content-between mb-3 align-items-center">
						<h5 class="fw-bold text-dark mb-0"><%=Common.getBahasaConfig("Total Bayar")%></h5>
						<h3 class="fw-bold text-success mb-0" id="cartGrandTotal<%=rnd%>">Rp 0</h3>
					</div>

                    <div id="panelUangTunai<%=rnd%>" class="bg-white p-3 rounded-3 border shadow-sm mb-4" style="display: none;">
						<div class="row g-2 align-items-center mb-2">
							<div class="col-5">
								<label class="small fw-bold text-secondary mb-0"><%=Common.getBahasaConfig("Uang Diterima")%></label>
							</div>
							<div class="col-7">
								<div class="input-group input-group-sm">
									<span class="input-group-text bg-light border-end-0">Rp</span>
									<input type="number" class="form-control border-start-0 fw-bold text-primary" id="inputUangTunai<%=rnd%>" placeholder="0" onkeyup="hitungKembalian<%=rnd%>()">
								</div>
							</div>
						</div>
						<div class="row g-2 align-items-center">
							<div class="col-5">
								<label class="small fw-bold text-secondary mb-0"><%=Common.getBahasaConfig("Kembalian")%></label>
							</div>
							<div class="col-7">
								<input type="text" class="form-control form-control-sm border-0 bg-transparent fw-bold text-danger fs-6 text-end px-1" id="labelKembalian<%=rnd%>" value="Rp 0" readonly>
							</div>
						</div>
					</div>

                    <div class="d-flex gap-2">
                        <button class="btn btn-outline-primary py-3 fw-bold rounded-3" onclick="simpanKeranjangTertahan<%=rnd%>()" id="btnSimpanTertahan<%=rnd%>" style="flex: 0 0 auto;" title="<%=Common.getBahasaConfig("Simpan keranjang ini utk dilanjutkan nanti, tanpa membayar sekarang")%>">
                            <i class="fas fa-bookmark"></i> <span class="d-none d-lg-inline"><%=Common.getBahasaConfig("Simpan")%></span>
                        </button>
                        <button class="btn btn-primary flex-grow-1 py-3 fw-bold rounded-3 shadow-sm fs-5" onclick="initiatePembayaran<%=rnd%>()" id="btnBayar<%=rnd%>">
                            <i class="fas fa-check-circle me-2"></i><%=Common.getBahasaConfig("Bayar")%> <span class="badge bg-white text-primary ms-1 d-none d-md-inline">F12</span>
                        </button>
                    </div>
				</div>
			</div>
		</div>
	</div>
</div>

<div class="row g-4 mt-1 animate__animated animate__fadeInUp">
	<div class="col-lg-5">
		<div class="card border-0 shadow-sm rounded-4 h-100">
			<div class="card-body p-4">
				<div class="d-flex justify-content-between align-items-center mb-1">
					<h6 class="fw-bold text-dark mb-0"><i class="fas fa-boxes-stacked text-primary me-2"></i><%=Common.getBahasaConfig("Ringkasan Inventori")%></h6>
					<a href="<%=Common.ROOT%>/baru?p=kantin&s=barang%2Findex" class="small fw-semibold text-decoration-none"><%=Common.getBahasaConfig("Lihat Semua")%></a>
				</div>
				<div class="text-muted small mb-3"><%=Common.getBahasaConfig("Menunjukkan berapa banyak barang yang tersedia di toko dan barang mana yang stoknya perlu segera ditambah.")%></div>
				<div class="row g-2 mb-3" id="invStatRow<%=rnd%>">
					<div class="col-12 text-center text-muted py-3"><div class="spinner-border spinner-border-sm text-primary"></div></div>
				</div>
				<div class="fw-bold small text-secondary mb-2"><%=Common.getBahasaConfig("Produk Stok Rendah")%></div>
				<div id="invStokRendahList<%=rnd%>"></div>
			</div>
		</div>
	</div>
	<div class="col-lg-7">
		<div class="card border-0 shadow-sm rounded-4 h-100">
			<div class="card-body p-4">
				<div class="d-flex justify-content-between align-items-center mb-1">
					<h6 class="fw-bold text-dark mb-0"><i class="fas fa-history text-primary me-2"></i><%=Common.getBahasaConfig("Riwayat Transaksi")%></h6>
					<span class="small text-muted"><%=Common.getBahasaConfig("5 transaksi terakhir")%></span>
				</div>
				<div class="text-muted small mb-3"><%=Common.getBahasaConfig("Menampilkan penjualan yang baru saja terjadi, supaya kasir bisa cepat memeriksa transaksi terakhir tanpa perlu membuka halaman lain.")%></div>
				<div class="table-responsive">
					<table class="table table-sm table-hover align-middle mb-0">
						<thead class="small text-muted text-uppercase">
							<tr><th><%=Common.getBahasaConfig("Waktu")%></th><th><%=Common.getBahasaConfig("Pembeli")%></th><th class="text-end"><%=Common.getBahasaConfig("Total")%></th><th class="text-center"><%=Common.getBahasaConfig("Metode")%></th></tr>
						</thead>
						<tbody id="miniRiwayatBody<%=rnd%>">
							<tr><td colspan="4" class="text-center text-muted py-4"><div class="spinner-border spinner-border-sm text-primary"></div></td></tr>
						</tbody>
					</table>
				</div>
			</div>
		</div>
	</div>
</div>

<div class="row g-4 mt-1 mb-2 animate__animated animate__fadeInUp">
	<div class="col-12">
		<div class="card border-0 shadow-sm rounded-4">
			<div class="card-body p-4">
				<h6 class="fw-bold text-dark mb-1"><i class="fas fa-chart-line text-primary me-2"></i><%=Common.getBahasaConfig("Analitik Penjualan")%> <span class="small text-muted fw-normal">(<%=Common.getBahasaConfig("7 Hari Terakhir")%>)</span></h6>
				<div class="text-muted small mb-3"><%=Common.getBahasaConfig("Membandingkan penjualan toko minggu ini dengan minggu lalu, supaya terlihat jelas apakah usaha sedang naik atau turun.")%></div>
				<div class="row g-2 mb-4" id="analitikStatRow<%=rnd%>">
					<div class="col-12 text-center text-muted py-3"><div class="spinner-border spinner-border-sm text-primary"></div></div>
				</div>
				<div class="row g-4">
					<div class="col-lg-5">
						<div class="fw-semibold small text-secondary mb-1"><%=Common.getBahasaConfig("Penjualan Harian")%></div>
						<div class="text-muted small mb-2" style="font-size:11.5px;"><%=Common.getBahasaConfig("Naik-turunnya total penjualan setiap hari dalam seminggu terakhir.")%></div>
						<div style="position: relative; height: 240px;"><canvas id="chartAnalitikLine<%=rnd%>"></canvas></div>
					</div>
					<div class="col-lg-3">
						<div class="fw-semibold small text-secondary mb-1"><%=Common.getBahasaConfig("Penjualan per Kategori")%></div>
						<div class="text-muted small mb-2" style="font-size:11.5px;"><%=Common.getBahasaConfig("Jenis barang apa yang paling banyak menyumbang penjualan minggu ini.")%></div>
						<div style="position: relative; height: 240px;"><canvas id="chartAnalitikDonut<%=rnd%>"></canvas></div>
					</div>
					<div class="col-lg-4">
						<div class="fw-semibold small text-secondary mb-1"><%=Common.getBahasaConfig("Perbandingan Kinerja")%></div>
						<div class="text-muted small mb-2" style="font-size:11.5px;"><%=Common.getBahasaConfig("Membandingkan 4 ukuran keberhasilan toko minggu ini vs minggu lalu dalam satu gambar -- makin lebar bentuknya, makin baik performanya.")%></div>
						<div style="position: relative; height: 240px;"><canvas id="chartAnalitikRadar<%=rnd%>"></canvas></div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>

<%@ include file="_bantuan_pos.jsp" %>
<script src="<%=Common.ROOT%>/js/pesan-formal.js"></script>
<script src="<%=Common.ROOT%>/js/ais_pos_offline.js"></script>
<script src="<%=Common.ROOT%>/js/ais_image_compress.js"></script>
<script src="<%=Common.ROOT%>/js/ais_pos_pwa.js" data-root="<%=Common.ROOT%>" data-manifest="<%=Common.ROOT%>/manifest.webmanifest" data-sw="<%=Common.ROOT%>/ais_pos_sw.js"></script>
<script>
    // ==========================================
    // VARIABEL & KONFIGURASI GLOBAL
    // ==========================================
    const posTanpaLogin<%=rnd%> = <%=posTanpaLogin%>;
    let cart<%=rnd%> = [];
    let grandTotalValue<%=rnd%> = 0;
    
    let arrDataToko<%=rnd%> = [];
    let arrDataMember<%=rnd%> = [];
    let arrAturanDiskon<%=rnd%> = []; // Menampung list Aturan Diskon Aktif

    const isAdmin<%=rnd%> = <%=isAdmin%>;
    let currentTokoId<%=rnd%> = '<%=idTokoAktif%>';
    const filterTokoSQL<%=rnd%> = "<%=sqlFilterToko%>";
    const pajakPersenPOS<%=rnd%> = <%=pajakPersenPOS%>;
    let grandPajakValue<%=rnd%> = 0; // nominal Rp pajak keranjang saat ini (diisi ulang tiap renderCart)
    let currentDraftId<%=rnd%> = null; // id koperasi.draft_pembelian_anggota_koperasi bila keranjang ini hasil "Muat" dr Tertahan

    // Split pembayaran (s/d 5 metode/transaksi, mis. separuh Transfer + separuh Tunai): array of
    // {id, nama, manual, nominal}. Kosong = mode lama satu-metode (select#selectCaraBayar<%=rnd%> tetap
    // sumber kebenaran, TIDAK ada perubahan perilaku). Terisi >=2 = mode split aktif, panel QR/tunai
    // besar disembunyikan (lihat lanjutkanPembayaranSetelahPin) karena tidak jelas dibayar via metode
    // mana yang harusnya menampilkan QR/kembalian saat displit.
    let splitSelectedMetodeBayar<%=rnd%> = [];
    
    // Variabel state untuk QR Code Polling
    let qrCheckInterval<%=rnd%> = null;
    let isQRCancelled<%=rnd%> = false;
    let qrGeneratorInstance<%=rnd%> = null; // Menyimpan instance QRCode.js

    // ==========================================
    // LAYAR PELANGGAN (layar kedua, mesin POS dual-monitor)
    // Komunikasi murni client-side via BroadcastChannel (nama channel = rnd halaman ini, unik
    // per sesi kasir) -- tanpa polling server. Lihat modul/kantin/pos/layar_pelanggan.jsp.
    // ==========================================
    let custWin<%=rnd%> = null;
    let custChannel<%=rnd%> = null;
    let lastKeranjangBroadcast<%=rnd%> = null; // cache utk balas permintaan "minta_status"
    let pinPendingResolve<%=rnd%> = null; // fungsi lanjut yg menunggu hasil verifikasi PIN pembeli
    try { if ('BroadcastChannel' in window) custChannel<%=rnd%> = new BroadcastChannel('pos_layar_pelanggan_<%=rnd%>'); } catch(e) { custChannel<%=rnd%> = null; }

    const kirimKeLayarPelanggan<%=rnd%> = (data) => {
        if (data && data.tipe === 'keranjang') lastKeranjangBroadcast<%=rnd%> = data;
        if (custChannel<%=rnd%>) { try { custChannel<%=rnd%>.postMessage(data); } catch(e) {} }
    };

    if (custChannel<%=rnd%>) {
        custChannel<%=rnd%>.onmessage = (ev) => {
            const d = ev.data || {};
            if (d.tipe === 'minta_status' && lastKeranjangBroadcast<%=rnd%>) {
                kirimKeLayarPelanggan<%=rnd%>(lastKeranjangBroadcast<%=rnd%>);
            } else if (d.tipe === 'survey' && typeof tampilkanToast === 'function') {
                tampilkanToast('<%=Common.getBahasaConfigJS("Pembeli memberi rating")%>: ' + '★'.repeat(Number(d.rating)||0), 'bg-info text-dark');
            } else if (d.tipe === 'pin_hasil') {
                if (d.ok && pinPendingResolve<%=rnd%>) {
                    const lanjut = pinPendingResolve<%=rnd%>;
                    pinPendingResolve<%=rnd%> = null;
                    lanjut();
                } else if (!d.ok) {
                    pinPendingResolve<%=rnd%> = null;
                    if (typeof tampilkanToast === "function") {
                        tampilkanToast(d.batal ? '<%=Common.getBahasaConfigJS("Transaksi dibatalkan oleh pembeli.")%>' : '<%=Common.getBahasaConfigJS("PIN salah. Transaksi dibatalkan.")%>', 'bg-danger text-white');
                    } else {
                        alert(d.batal ? '<%=Common.getBahasaConfigJS("Transaksi dibatalkan oleh pembeli.")%>' : '<%=Common.getBahasaConfigJS("PIN salah. Transaksi dibatalkan.")%>');
                    }
                }
            }
        };
    }

    // TOMBOL LAYAR PELANGGAN BERSIFAT TOGGLE (buka bila tertutup, tutup bila terbuka) -- sebelumnya
    // hanya bisa membuka/fokus, menutup harus lewat kontrol jendela popup itu sendiri (tombol X).
    // Di dalam shell aplikasi desktop (Electron, lihat desktop-pos-electron/), layar kedua adalah
    // BrowserWindow NATIVE yang diposisikan otomatis ke monitor kedua bila terdeteksi (bukan sekadar
    // popup browser) -- dijembatani lewat window.electronAPI yang diinjeksi preload script; di browser
    // biasa (bukan shell desktop), tetap jatuh ke window.open() seperti sebelumnya.
    window.bukaLayarPelanggan<%=rnd%> = () => {
        const idToko = document.getElementById('idTokoSelected<%=rnd%>') ? document.getElementById('idTokoSelected<%=rnd%>').value : '';
        const url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=kantin%2Fpos&s=layar_pelanggan&ch=pos_layar_pelanggan_<%=rnd%>' + (idToko ? ('&toko=' + encodeURIComponent(idToko)) : '');

        if (window.electronAPI && window.electronAPI.isElectron) {
            window.electronAPI.isCustomerScreenOpen().then(sudahTerbuka => {
                if (sudahTerbuka) { window.electronAPI.closeCustomerScreen(); }
                else { window.electronAPI.openCustomerScreen(url); }
            });
            return;
        }

        if (custWin<%=rnd%> && !custWin<%=rnd%>.closed) { custWin<%=rnd%>.close(); custWin<%=rnd%>= null; return; }
        // Geser default ke kanan lebar layar utama -- pada desktop extended (dua monitor) jendela
        // baru cenderung terbuka di monitor kedua; operator tinggal drag bila posisinya belum pas.
        const left = (window.screen && window.screen.availWidth) ? window.screen.availWidth : 0;
        custWin<%=rnd%> = window.open(url, 'posLayarPelanggan<%=rnd%>', 'width=1280,height=800,left=' + left + ',top=0,menubar=no,toolbar=no,location=no,status=no');
    };

    const formatRp<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);
    };

    const getFormattedDate<%=rnd%> = () => {
        const now = new Date();
        const pad = (n) => n.toString().padStart(2, '0');
        return pad(now.getDate()) + '-' + pad(now.getMonth()+1) + '-' + now.getFullYear() + ' ' + pad(now.getHours()) + ':' + pad(now.getMinutes()) + ':' + pad(now.getSeconds());
    };

    // Fungsi Generate Kode Unik 50 Karakter
    const generateUniqueCode50 = () => {
        const timePart = new Date().getTime().toString(); // 13 digit
        const randomPart = Array.from(window.crypto.getRandomValues(new Uint8Array(37)))
            .map(b => b.toString(36).padStart(2, '0')).join('').substring(0, 37).toUpperCase();
        return timePart + randomPart;
    };

    const fetchData<%=rnd%> = async (sql) => {
        try {
            let payload = { sql: sql, action: "sql" };
            if (posTanpaLogin<%=rnd%>) payload.tanpaLogin = "true";

            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const result = await res.json();
            return result.data || [];
        } catch (e) { console.error(e); return []; }
    };

    // ==========================================
    // INJEKSI MODAL VIA JAVASCRIPT
    // ==========================================
    const injectModals<%=rnd%> = () => {
        const modalHtml = `
        <div class="modal fade" id="modalQRCode<%=rnd%>" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" aria-hidden="true">
          <div class="modal-dialog modal-dialog-centered modal-sm">
            <div class="modal-content border-0 rounded-4 shadow">
              <div class="modal-header border-bottom-0 pb-0">
                <h5 class="modal-title fw-bold text-primary"><i class="fas fa-qrcode me-2"></i><%=Common.getBahasaConfig("Pindai QR Code")%></h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close" onclick="batalkanQR<%=rnd%>()"></button>
              </div>
              <div class="modal-body text-center pt-2 pb-4">
                <p class="text-muted small mb-3"><%=Common.getBahasaConfig("Arahkan kamera atau aplikasi pembayaran pelanggan ke kode ini.")%></p>
                
                <div id="qrcodeWrapper<%=rnd%>" class="d-inline-block bg-white p-3 border rounded-3 shadow-sm mb-3"></div>
                
                <h4 class="fw-bold text-dark mb-1" id="qrTotalBayar<%=rnd%>">Rp 0</h4>
                <div class="text-secondary small fw-medium mt-3" id="qrStatusLoading<%=rnd%>">
                    <span class="spinner-grow spinner-grow-sm text-primary me-2" role="status" aria-hidden="true"></span>
                    <%=Common.getBahasaConfig("Menunggu konfirmasi pembayaran...")%>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="modal fade" id="modalMetodeBayar<%=rnd%>" tabindex="-1" aria-hidden="true">
          <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 rounded-4 shadow">
              <div class="modal-header border-bottom-0 pb-1">
                <h5 class="modal-title fw-bold text-dark"><%=Common.getBahasaConfig("Pilih Metode Pembayaran")%></h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
              </div>
              <div class="modal-body pt-2 pb-4">
                <p class="text-muted small mb-2"><%=Common.getBahasaConfig("Klik kartu utk bayar penuh 1 metode, atau centang ikon di pojok kartu utk gabungkan s/d 5 metode (split bayar).")%></p>
                <div class="row g-2" id="gridMetodeBayar<%=rnd%>">
                    <div class="col-12 text-center text-muted py-3"><%=Common.getBahasaConfig("Memuat data...")%></div>
                </div>
                <div class="d-none mt-3 pt-3 border-top" id="panelSplitBayar<%=rnd%>">
                    <div class="fw-bold small text-dark mb-2"><i class="fas fa-layer-group me-1"></i><%=Common.getBahasaConfig("Bagi Nominal per Metode")%></div>
                    <div id="listSplitBayar<%=rnd%>"></div>
                    <div class="d-flex justify-content-between align-items-center mt-2 mb-3 px-1">
                        <span class="small text-muted"><%=Common.getBahasaConfig("Sisa belum dialokasikan")%></span>
                        <span class="fw-bold" id="sisaSplitBayar<%=rnd%>">Rp 0</span>
                    </div>
                    <button type="button" class="btn btn-primary w-100 rounded-pill fw-bold" id="btnTerapkanSplitBayar<%=rnd%>" onclick="terapkanSplitPembayaran<%=rnd%>()">
                        <i class="fas fa-check me-1"></i><%=Common.getBahasaConfig("Terapkan Split Pembayaran")%>
                    </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="modal fade" id="modalTransaksiSukses<%=rnd%>" data-bs-backdrop="static" tabindex="-1" aria-hidden="true">
          <div class="modal-dialog modal-dialog-centered modal-sm">
            <div class="modal-content border-0 rounded-4 shadow">
              <div class="modal-body text-center p-4">
                <div class="mb-2" style="font-size:52px;color:#198754;"><i class="fas fa-check-circle"></i></div>
                <h5 class="fw-bold text-dark mb-1"><%=Common.getBahasaConfig("Transaksi Berhasil")%></h5>
                <div class="fw-bold text-success mb-1" style="font-size:26px;" id="suksesTotalBayar<%=rnd%>">Rp 0</div>
                <div class="text-muted small mb-3" id="suksesInfoTrx<%=rnd%>"></div>
                <div class="d-flex gap-2 justify-content-center">
                    <button type="button" class="btn btn-outline-primary rounded-pill px-3" id="btnCetakUlangSukses<%=rnd%>"><i class="fas fa-print me-1"></i><%=Common.getBahasaConfig("Cetak Struk")%></button>
                    <button type="button" class="btn btn-primary rounded-pill px-4" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Selesai")%></button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="modal fade" id="modalKeranjangTertahan<%=rnd%>" tabindex="-1" aria-hidden="true">
          <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 rounded-4 shadow">
              <div class="modal-header border-bottom-0 pb-1">
                <h5 class="modal-title fw-bold text-dark"><i class="fas fa-inbox me-2"></i><%=Common.getBahasaConfig("Keranjang Tertahan")%></h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
              </div>
              <div class="modal-body pt-2 pb-4">
                <p class="text-muted small mb-3"><%=Common.getBahasaConfig("Keranjang yang disimpan sebelumnya (belum dibayar). Pilih salah satu utk melanjutkan transaksinya.")%></p>
                <div id="listKeranjangTertahan<%=rnd%>">
                    <div class="text-center text-muted py-3"><div class="spinner-border spinner-border-sm text-primary"></div></div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Gap-closure "Produk Ekstra" -- modal picker topping/tambahan, dibuka dari addToCart<%=rnd%> bila produk yg ditap punya ekstra_pilihan terisi. -->
        <div class="modal fade" id="modalEkstraPilihan<%=rnd%>" tabindex="-1" aria-hidden="true">
          <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 rounded-4 shadow">
              <div class="modal-header border-bottom-0 pb-1">
                <h5 class="modal-title fw-bold text-dark"><i class="fas fa-mug-hot me-2"></i><%=Common.getBahasaConfig("Pilih Ekstra")%> - <span id="judulModalEkstra<%=rnd%>"></span></h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
              </div>
              <div class="modal-body pt-2 pb-3">
                <p class="text-muted small mb-2"><%=Common.getBahasaConfig("Centang tambahan/topping yang dipesan (opsional).")%></p>
                <div id="bodyModalEkstra<%=rnd%>">
                    <div class="text-center text-muted py-3"><div class="spinner-border spinner-border-sm text-primary"></div></div>
                </div>
              </div>
              <div class="modal-footer border-top-0 pt-0">
                <button type="button" class="btn btn-light rounded-pill" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
                <button type="button" class="btn btn-primary rounded-pill fw-bold px-4" onclick="konfirmasiModalEkstra<%=rnd%>()"><i class="fas fa-cart-plus me-1"></i><%=Common.getBahasaConfig("Tambah ke Keranjang")%></button>
              </div>
            </div>
          </div>
        </div>

        <!-- Gap-closure "Aktivasi Manual" -- promo dgn aktivasi_manual=true TIDAK auto-terapkan (lihat
             evaluateDiscount<%=rnd%>), kasir wajib memilihnya di sini dulu. -->
        <div class="modal fade" id="modalPromoManual<%=rnd%>" tabindex="-1" aria-hidden="true">
          <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 rounded-4 shadow">
              <div class="modal-header border-bottom-0 pb-1">
                <h5 class="modal-title fw-bold text-dark"><i class="fas fa-tags me-2"></i><%=Common.getBahasaConfig("Pilih Promo")%></h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
              </div>
              <div class="modal-body pt-2 pb-3">
                <p class="text-muted small mb-2"><%=Common.getBahasaConfig("Promo berikut tersedia untuk keranjang saat ini, tapi harus dipilih manual.")%></p>
                <div id="bodyModalPromoManual<%=rnd%>">
                    <div class="text-center text-muted py-3"><div class="spinner-border spinner-border-sm text-primary"></div></div>
                </div>
              </div>
              <div class="modal-footer border-top-0 pt-0">
                <button type="button" class="btn btn-light rounded-pill" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Tutup")%></button>
              </div>
            </div>
          </div>
        </div>

        <div class="modal fade" id="modalStatusSinkron<%=rnd%>" tabindex="-1" aria-hidden="true">
          <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content border-0 rounded-4 shadow">
              <div class="modal-header border-bottom-0 pb-1">
                <h5 class="modal-title fw-bold text-dark"><i class="fas fa-rotate me-2"></i><%=Common.getBahasaConfig("Status Sinkronisasi")%></h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
              </div>
              <div class="modal-body pt-1 pb-4">
                <p class="text-muted small mb-3"><%=Common.getBahasaConfig("Transaksi yang tersimpan saat internet terputus akan otomatis terkirim begitu koneksi kembali. Di sini Anda bisa memeriksa transaksi mana saja yang masih menunggu, dan memaksa pengiriman sekarang juga.")%></p>
                <div class="row g-2 mb-3">
                  <div class="col-6 col-md-4">
                    <div class="border rounded-3 p-2 text-center h-100">
                      <div class="text-muted small"><%=Common.getBahasaConfig("Koneksi Saat Ini")%></div>
                      <div class="fw-bold" id="ssKoneksi<%=rnd%>">-</div>
                    </div>
                  </div>
                  <div class="col-6 col-md-4">
                    <div class="border rounded-3 p-2 text-center h-100">
                      <div class="text-muted small"><%=Common.getBahasaConfig("Menunggu Sinkron")%></div>
                      <div class="fw-bold" id="ssPending<%=rnd%>">-</div>
                    </div>
                  </div>
                  <div class="col-12 col-md-4">
                    <div class="border rounded-3 p-2 text-center h-100">
                      <div class="text-muted small"><%=Common.getBahasaConfig("Terakhir Sinkron")%></div>
                      <div class="fw-bold" id="ssTerakhir<%=rnd%>" style="font-size:.85rem;">-</div>
                    </div>
                  </div>
                </div>
                <div class="d-flex flex-wrap gap-2 mb-3">
                  <button type="button" class="btn btn-primary rounded-pill px-3" id="btnSyncSekarang<%=rnd%>"><i class="fas fa-rotate me-1"></i><%=Common.getBahasaConfig("Sinkronkan Sekarang")%></button>
                  <button type="button" class="btn btn-outline-secondary rounded-pill px-3" id="btnLihatRiwayatSinkron<%=rnd%>"><i class="fas fa-clock-rotate-left me-1"></i><%=Common.getBahasaConfig("Riwayat Sinkronisasi (Server)")%></button>
                </div>
                <div class="text-muted small mb-2"><%=Common.getBahasaConfig("Transaksi yang masih tersimpan di perangkat ini (belum terkirim):")%></div>
                <div class="table-responsive" style="max-height:320px;overflow-y:auto;">
                  <table class="table table-sm align-middle mb-0">
                    <thead><tr>
                      <th><%=Common.getBahasaConfig("Waktu")%></th>
                      <th><%=Common.getBahasaConfig("Kode")%></th>
                      <th class="text-end"><%=Common.getBahasaConfig("Total")%></th>
                      <th><%=Common.getBahasaConfig("Status")%></th>
                    </tr></thead>
                    <tbody id="ssTabelPending<%=rnd%>"><tr><td colspan="4" class="text-center text-muted py-3"><%=Common.getBahasaConfig("Memuat...")%></td></tr></tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="modal fade" id="modalPesananBaru<%=rnd%>" tabindex="-1" aria-hidden="true">
          <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 rounded-4 shadow">
              <div class="modal-header border-bottom-0 pb-1 bg-primary bg-opacity-10">
                <h5 class="modal-title fw-bold text-primary"><i class="fas fa-bell me-2"></i><%=Common.getBahasaConfig("Pesanan Online Baru")%></h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
              </div>
              <div class="modal-body pt-2 pb-3" id="isiPesananBaru<%=rnd%>" style="max-height:60vh;overflow-y:auto;"></div>
              <div class="modal-footer border-top-0 pt-0">
                <button type="button" class="btn btn-outline-secondary rounded-pill px-3" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Tutup")%></button>
                <button type="button" class="btn btn-primary rounded-pill px-3" id="btnLihatTabPesananBaru<%=rnd%>"><i class="fas fa-list me-1"></i><%=Common.getBahasaConfig("Lihat Daftar Pesanan")%></button>
              </div>
            </div>
          </div>
        </div>`;
        document.body.insertAdjacentHTML('beforeend', modalHtml);
    };

    /** Tampilkan kartu konfirmasi in-page (checkmark+ringkasan) SETELAH cetakStruk() -- pelengkap
     *  visual, TIDAK menggantikan alur cetak popup yg sudah berfungsi. */
    const tampilkanTransaksiSukses<%=rnd%> = (payload, totalBayar) => {
        const el = document.getElementById('modalTransaksiSukses<%=rnd%>');
        if (!el) return;
        document.getElementById('suksesTotalBayar<%=rnd%>').textContent = formatRp<%=rnd%>(totalBayar);
        document.getElementById('suksesInfoTrx<%=rnd%>').textContent = (payload.waktu || '') + ' • ' + (payload.kodeUnik || '');
        const btnCetak = document.getElementById('btnCetakUlangSukses<%=rnd%>');
        btnCetak.onclick = () => cetakStruk<%=rnd%>(payload, totalBayar, 0, 0);
        try { new bootstrap.Modal(el).show(); } catch (e) { /* Bootstrap blm siap -- abaikan, print popup ttp jalan */ }
    };

    // ==========================================
    // LOGIKA METODE PEMBAYARAN DINAMIS
    // ==========================================
    const ikonMetodeBayar<%=rnd%> = (nama) => {
        const n = (nama || '').toLowerCase();
        if (n.indexOf('tunai') >= 0 || n.indexOf('cash') >= 0) return 'fa-money-bill-wave';
        if (n.indexOf('qris') >= 0 || n.indexOf('qr') >= 0) return 'fa-qrcode';
        if (n.indexOf('kartu') >= 0 || n.indexOf('debit') >= 0 || n.indexOf('kredit') >= 0 || n.indexOf('card') >= 0) return 'fa-credit-card';
        return 'fa-wallet';
    };

    // Render ulang grid kartu metode bayar berdasarkan <option> yg sudah dimuat ke <select> asli
    // (select#selectCaraBayar tetap sumber kebenaran -- kartu ini cuma lapisan visual di atasnya,
    // supaya SELURUH logika checkout/PIN/QRIS existing yg baca select.value tak perlu diubah sama sekali).
    const renderMetodeBayarCards<%=rnd%> = () => {
        const select = document.getElementById('selectCaraBayar<%=rnd%>');
        const grid = document.getElementById('gridMetodeBayar<%=rnd%>');
        if (!select || !grid) return;

        let html = '';
        let ada = false;
        for (let i = 0; i < select.options.length; i++) {
            const opt = select.options[i];
            if (opt.value === '') continue;
            ada = true;
            const nama = opt.getAttribute('data-nama') || opt.text;
            const isManual = opt.getAttribute('data-manual') === 'true';
            const isSel = opt.selected ? 'sel' : '';
            const dalamSplit = splitSelectedMetodeBayar<%=rnd%>.some(s => String(s.id) === String(opt.value));
            html += '<div class="col-6">' +
                        '<div class="paymethod-card-<%=rnd%> ' + isSel + (dalamSplit ? ' split-sel' : '') + '" id="paymethod-<%=rnd%>-' + opt.value + '" onclick="pilihMetodeBayar<%=rnd%>(\'' + opt.value + '\')">' +
                            '<div class="paymethod-split-chk-<%=rnd%>' + (dalamSplit ? ' checked' : '') + '" onclick="event.stopPropagation(); toggleSplitMetodeBayar<%=rnd%>(\'' + opt.value + '\', ' + JSON.stringify(nama) + ', ' + isManual + ');" title="<%=Common.getBahasaConfig("Sertakan dalam split pembayaran")%>"><i class="fas fa-check"></i></div>' +
                            '<div class="paymethod-ic-<%=rnd%>"><i class="fas ' + ikonMetodeBayar<%=rnd%>(nama) + '"></i></div>' +
                            '<div class="fw-bold text-dark" style="font-size:13px;">' + nama + '</div>' +
                        '</div>' +
                    '</div>';
        }
        grid.innerHTML = ada ? html : '<div class="col-12 text-center text-muted py-3"><%=Common.getBahasaConfig("Tidak ada metode pembayaran tersedia.")%></div>';
        perbaruiLabelMetodeBayar<%=rnd%>();
    };

    // ==========================================
    // SPLIT PEMBAYARAN (s/d 5 metode/transaksi)
    // ==========================================
    window.toggleSplitMetodeBayar<%=rnd%> = (id, nama, manual) => {
        const idx = splitSelectedMetodeBayar<%=rnd%>.findIndex(s => String(s.id) === String(id));
        if (idx >= 0) {
            splitSelectedMetodeBayar<%=rnd%>.splice(idx, 1);
        } else {
            if (splitSelectedMetodeBayar<%=rnd%>.length >= 5) {
                if (typeof tampilkanToast === "function") tampilkanToast('<%=Common.getBahasaConfigJS("Maksimal 5 metode pembayaran per transaksi.")%>', 'bg-warning text-dark');
                return;
            }
            splitSelectedMetodeBayar<%=rnd%>.push({ id: id, nama: nama, manual: manual, nominal: 0 });
        }
        renderMetodeBayarCards<%=rnd%>();
        renderSplitPanel<%=rnd%>();
    };

    const renderSplitPanel<%=rnd%> = () => {
        const panel = document.getElementById('panelSplitBayar<%=rnd%>');
        const list = document.getElementById('listSplitBayar<%=rnd%>');
        if (!panel || !list) return;

        if (splitSelectedMetodeBayar<%=rnd%>.length < 2) {
            panel.classList.add('d-none');
            return;
        }
        panel.classList.remove('d-none');

        // Default: bagi rata, sisa pembulatan dibebankan ke slot terakhir. HANYA dijalankan sekali
        // per perubahan jumlah slot (dideteksi via seluruh nominal masih 0) -- supaya angka yg sudah
        // diedit manual oleh kasir tidak direset ulang tiap render.
        const semuaNol = splitSelectedMetodeBayar<%=rnd%>.every(s => !s.nominal);
        if (semuaNol && grandTotalValue<%=rnd%> > 0) {
            const n = splitSelectedMetodeBayar<%=rnd%>.length;
            const rata = Math.floor(grandTotalValue<%=rnd%> / n);
            splitSelectedMetodeBayar<%=rnd%>.forEach((s, i) => {
                s.nominal = (i === n - 1) ? (grandTotalValue<%=rnd%> - rata * (n - 1)) : rata;
            });
        }

        let html = '';
        splitSelectedMetodeBayar<%=rnd%>.forEach(s => {
            html += '<div class="d-flex align-items-center gap-2 mb-2">' +
                        '<div class="small fw-semibold text-dark" style="width:38%;"><i class="fas ' + ikonMetodeBayar<%=rnd%>(s.nama) + ' me-1 text-muted"></i>' + s.nama + '</div>' +
                        '<div class="input-group input-group-sm">' +
                            '<span class="input-group-text">Rp</span>' +
                            '<input type="number" min="0" step="any" class="form-control text-end" value="' + s.nominal + '" oninput="updateSplitNominal<%=rnd%>(\'' + s.id + '\', this.value)">' +
                        '</div>' +
                    '</div>';
        });
        list.innerHTML = html;
        updateSisaSplitLabel<%=rnd%>();
    };

    window.updateSplitNominal<%=rnd%> = (id, value) => {
        const s = splitSelectedMetodeBayar<%=rnd%>.find(s => String(s.id) === String(id));
        if (s) s.nominal = parseFloat(value) || 0;
        updateSisaSplitLabel<%=rnd%>();
    };

    const updateSisaSplitLabel<%=rnd%> = () => {
        const totalDialokasikan = splitSelectedMetodeBayar<%=rnd%>.reduce((sum, s) => sum + (parseFloat(s.nominal) || 0), 0);
        const sisa = grandTotalValue<%=rnd%> - totalDialokasikan;
        const lbl = document.getElementById('sisaSplitBayar<%=rnd%>');
        const btn = document.getElementById('btnTerapkanSplitBayar<%=rnd%>');
        if (!lbl || !btn) return;
        lbl.textContent = formatRp<%=rnd%>(sisa);
        const balance = Math.abs(sisa) < 1; // toleransi pembulatan
        lbl.className = balance ? 'fw-bold text-success' : 'fw-bold text-danger';
        btn.disabled = !balance;
    };

    window.terapkanSplitPembayaran<%=rnd%> = () => {
        if (splitSelectedMetodeBayar<%=rnd%>.length < 2) return;
        // Slot 1 (select#selectCaraBayar) tetap diisi metode PERTAMA yg dicentang -- dibutuhkan
        // beberapa jalur lama yg masih membaca select.value (mis. label tombol pemicu modal).
        const select = document.getElementById('selectCaraBayar<%=rnd%>');
        select.value = splitSelectedMetodeBayar<%=rnd%>[0].id;
        document.getElementById('lblMetodeBayarTerpilih<%=rnd%>').innerHTML =
            '<i class="fas fa-layer-group me-1"></i>' + splitSelectedMetodeBayar<%=rnd%>.length + ' <%=Common.getBahasaConfigJS("Metode (Split)")%>';
        document.getElementById('lblMetodeBayarTerpilih<%=rnd%>').classList.remove('text-muted');
        document.getElementById('lblMetodeBayarTerpilih<%=rnd%>').classList.add('fw-bold', 'text-dark');
        document.getElementById('panelUangTunai<%=rnd%>').style.display = 'none'; // ambigu saat split, lihat JavaDoc lanjutkanPembayaranSetelahPin
        const modalEl = document.getElementById('modalMetodeBayar<%=rnd%>');
        const inst = modalEl && window.bootstrap ? bootstrap.Modal.getInstance(modalEl) : null;
        if (inst) inst.hide();
    };

    const perbaruiLabelMetodeBayar<%=rnd%> = () => {
        const select = document.getElementById('selectCaraBayar<%=rnd%>');
        const lbl = document.getElementById('lblMetodeBayarTerpilih<%=rnd%>');
        if (!select || !lbl) return;
        if (select.selectedIndex > 0) {
            lbl.textContent = select.options[select.selectedIndex].getAttribute('data-nama') || select.options[select.selectedIndex].text;
            lbl.classList.remove('text-muted');
            lbl.classList.add('fw-bold', 'text-dark');
        } else {
            lbl.textContent = '<%=Common.getBahasaConfigJS("-- Pilih Metode --")%>';
            lbl.classList.add('text-muted');
            lbl.classList.remove('fw-bold', 'text-dark');
        }
    };

    window.pilihMetodeBayar<%=rnd%> = (value) => {
        const select = document.getElementById('selectCaraBayar<%=rnd%>');
        if (!select) return;
        // Klik badan kartu = pilih SATU metode utk bayar penuh, spt sebelum fitur split ada -- selalu
        // membatalkan/mereset split yg sedang disusun (kalau kasir berubah pikiran dari split ke satu
        // metode saja), supaya tak ada state split "nyangkut" saat checkout.
        splitSelectedMetodeBayar<%=rnd%> = [];
        select.value = value;
        select.dispatchEvent(new Event('change'));
        document.querySelectorAll('#gridMetodeBayar<%=rnd%> .paymethod-card-<%=rnd%>').forEach(c => c.classList.remove('sel', 'split-sel'));
        document.querySelectorAll('#gridMetodeBayar<%=rnd%> .paymethod-split-chk-<%=rnd%>').forEach(c => c.classList.remove('checked'));
        const el = document.getElementById('paymethod-<%=rnd%>-' + value);
        if (el) el.classList.add('sel');
        renderSplitPanel<%=rnd%>();
        perbaruiLabelMetodeBayar<%=rnd%>();
        const modalEl = document.getElementById('modalMetodeBayar<%=rnd%>');
        const inst = modalEl && window.bootstrap ? bootstrap.Modal.getInstance(modalEl) : null;
        if (inst) inst.hide();
    };

    const loadMetodePembayaranPOS<%=rnd%> = async (idJenisAnggota) => {
        // Gap-closure "Ada Kembalian" -- dulu panel Uang Tunai/Kembalian dinyalakan lewat tebak-nama
        // ("tunai"/"cash" di nama metode). Sekarang pakai kolom sungguhan `ada_kembalian`, dgn fallback
        // SAMA PERSIS spt getter entity (COALESCE ke nama ILIKE tunai) utk metode lama yg kolomnya
        // masih NULL -- tidak perlu migrasi data.
        let sql = "SELECT id, nama, manual, COALESCE(ada_kembalian, nama ILIKE '%tunai%') AS ada_kembalian " +
                   "FROM koperasi.cara_pembayaran_koperasi WHERE aktif = true ORDER BY nama ASC";

        // Jika ada ID Jenis Anggota, filter berdasarkan daftar_cara_pembayaran_yang_boleh_di_pilih
        if (idJenisAnggota) {
            sql = "SELECT cpk.id, cpk.nama, cpk.manual, COALESCE(cpk.ada_kembalian, cpk.nama ILIKE '%tunai%') AS ada_kembalian " +
                  "FROM koperasi.cara_pembayaran_koperasi cpk " +
                  "WHERE cpk.aktif = true " +
                  "AND (SELECT jak.daftar_cara_pembayaran_yang_boleh_di_pilih FROM koperasi.jenis_anggota_koperasi jak WHERE jak.id = " + idJenisAnggota + ") LIKE '%,' || cpk.id || ',%' " +
                  "ORDER BY cpk.nama ASC";
        }

        const res = await fetchData<%=rnd%>(sql);

        let optHtml = '<option value=""><%=Common.getBahasaConfig("-- Pilih Metode --")%></option>';
        if(res && res.length > 0) {
            res.forEach(item => {
                const isSelected = (res.length === 1) ? 'selected' : '';
                const isManual = item.manual === true || item.manual === 't' || item.manual === 'true' ? 'true' : 'false';
                const adaKembalian = item.ada_kembalian === true || item.ada_kembalian === 't' || item.ada_kembalian === 'true' ? 'true' : 'false';
                optHtml += '<option value="' + item.id + '" data-nama="' + item.nama + '" data-manual="' + isManual + '" data-ada-kembalian="' + adaKembalian + '" ' + isSelected + '>' + item.nama + '</option>';
            });
        } else if (idJenisAnggota) {
            optHtml = '<option value=""><%=Common.getBahasaConfig("-- Tidak Ada Metode Pembayaran Diizinkan --")%></option>';
        }

        const selectCara = document.getElementById('selectCaraBayar<%=rnd%>');
        if(selectCara) {
            selectCara.innerHTML = optHtml;
            handleCaraBayarChange<%=rnd%>(); // Memicu UI form tunai
            renderMetodeBayarCards<%=rnd%>(); // Sinkron kartu visual modal
        }
    };

    /** Metode terpilih saat ini punya kembalian (true) atau pembayaran wajib pas (false)? Default
     * true saat belum ada metode terpilih (biar tidak keliru memblokir sebelum kasir memilih apa pun). */
    const metodeBayarAdaKembalian<%=rnd%> = () => {
        const select = document.getElementById('selectCaraBayar<%=rnd%>');
        if (!select || select.selectedIndex <= 0) return true;
        return select.options[select.selectedIndex].getAttribute('data-ada-kembalian') === 'true';
    };

    const handleCaraBayarChange<%=rnd%> = () => {
        const select = document.getElementById('selectCaraBayar<%=rnd%>');
        const panelTunai = document.getElementById('panelUangTunai<%=rnd%>');
        const inputUangTunai = document.getElementById('inputUangTunai<%=rnd%>');

        // Gap-closure "Ada Kembalian" -- panel Uang Diterima/Kembalian dulu ditebak dari nama metode
        // ("tunai"/"cash"), sekarang dari kolom sungguhan `ada_kembalian`. Metode non-tunai (QRIS/
        // Transfer/Voucher/Kasbon dst) tidak menampilkan panel sama sekali -- pembayarannya WAJIB pas
        // dgn total, dikosongkan supaya tak ada input "Uang Diterima" yang membingungkan/keliru dipakai.
        if (select.selectedIndex > 0 && metodeBayarAdaKembalian<%=rnd%>()) {
            panelTunai.style.display = 'block';
        } else {
            panelTunai.style.display = 'none';
            if (inputUangTunai) inputUangTunai.value = '';
        }
        hitungKembalian<%=rnd%>();
    };


    // ==========================================
    // LOGIKA POS KASIR & ATURAN DISKON
    // ==========================================
    const loadMasterData<%=rnd%> = async () => {
        if (isAdmin<%=rnd%>) {
            const sqlToko = "SELECT id, nama FROM koperasi.toko WHERE aktif = true ORDER BY nama ASC";
            fetchData<%=rnd%>(sqlToko).then(res => {
                arrDataToko<%=rnd%> = res;
                let listHtml = '';
                res.forEach(item => { listHtml += '<option value="' + item.nama + '" data-id="' + item.id + '">'; });
                document.getElementById('listToko<%=rnd%>').innerHTML = listHtml;
            });
            
            document.getElementById('inputToko<%=rnd%>').addEventListener('change', async function() {
                const val = this.value;
                const match = arrDataToko<%=rnd%>.find(t => t.nama === val);
                currentTokoId<%=rnd%> = match ? match.id : "";
                document.getElementById('idTokoSelected<%=rnd%>').value = currentTokoId<%=rnd%>;
                if (window.AisPosOffline) AisPosOffline.setSession({ tokoId: currentTokoId<%=rnd%> }); // cache produk toko ini utk mode offline
                loadKatalogProduk<%=rnd%>();
                await updateUsageDiskonMember<%=rnd%>(); // Refresh limit toko yang baru dipilih
                recalculateCart<%=rnd%>(); // Re-evaluasi diskon per toko
                muatUlangPanelDashboard<%=rnd%>();
                loadDaftarTertahan<%=rnd%>();
                muatStatusSesiKas<%=rnd%>();
            });
        }

        // Ambil Data Anggota Koperasi (beserta relasi tipe, jenisnya, dan minimal saldo)
        const sqlMember = "SELECT a.id, a.nama, a.kode_identitas, a.jenis_anggota_koperasi as jenis_anggota, a.tipe_anggota_koperasi as tipe_anggota, COALESCE(j.minimal_saldo, 0) as min_saldo, COALESCE(j.wajib_pin, false) as wajib_pin " +
                          "FROM koperasi.anggota_koperasi a " +
                          "LEFT JOIN koperasi.jenis_anggota_koperasi j ON a.jenis_anggota_koperasi = j.id " +
                          "WHERE a.aktif = true ORDER BY a.nama ASC";
        fetchData<%=rnd%>(sqlMember).then(res => {
            arrDataMember<%=rnd%> = res;
            let listHtml = '';
            res.forEach(item => { 
                const iden = item.kode_identitas ? item.kode_identitas : '-';
                const label = item.nama + ' (' + iden + ')';
                listHtml += '<option value="' + label + '" data-id="' + item.id + '">'; 
            });
            document.getElementById('listMember<%=rnd%>').innerHTML = listHtml;
        });

        document.getElementById('inputMember<%=rnd%>').addEventListener('change', async function() {
            const val = this.value;
            const match = arrDataMember<%=rnd%>.find(m => {
                const iden = m.kode_identitas ? m.kode_identitas : '-';
                return (m.nama + ' (' + iden + ')') === val;
            });
            document.getElementById('idMemberSelected<%=rnd%>').value = match ? match.id : "";
            document.getElementById('minSaldoMemberSelected<%=rnd%>').value = match ? match.min_saldo : 0;
            const wajibPinVal = match && (match.wajib_pin === true || match.wajib_pin === 't' || match.wajib_pin === 'true');
            document.getElementById('wajibPinMemberSelected<%=rnd%>').value = wajibPinVal ? 'true' : 'false';

            // Reload cara bayar sesuai jenis anggota yang dipilih
            const idJenisAnggota = match ? match.jenis_anggota : null;
            loadMetodePembayaranPOS<%=rnd%>(idJenisAnggota);

            // Fitur "Cek Saldo": tampilkan saldo member LANGSUNG saat dipilih (info saja, bukan validasi checkout)
            tampilkanSaldoMemberTerpilih<%=rnd%>(match);

            // Re-kalkulasi daily limit (jika dibatasi per hari) dan seluruh keranjang
            await updateUsageDiskonMember<%=rnd%>();
            recalculateCart<%=rnd%>();
        });

        const tampilkanSaldoMemberTerpilih<%=rnd%> = async (match) => {
            const elInfoSaldo = document.getElementById('infoSaldoMember<%=rnd%>');
            const elLnkTopup = document.getElementById('lnkTopupMember<%=rnd%>');
            const elFormTopup = document.getElementById('formTopupMemberWrap<%=rnd%>');
            if (!match) {
                elInfoSaldo.classList.add('d-none');
                if (elLnkTopup) elLnkTopup.classList.add('d-none');
                if (elFormTopup) elFormTopup.classList.add('d-none');
                return;
            }
            elInfoSaldo.classList.remove('d-none');
            elInfoSaldo.textContent = '<%=Common.getBahasaConfigJS("Memeriksa saldo...")%>';
            if (elLnkTopup) elLnkTopup.classList.remove('d-none');
            const idMemberSaatDicek = match.id;
            const saldo = await checkSaldoTerbaru<%=rnd%>(idMemberSaatDicek);
            // Batalkan bila member sudah berganti selagi menunggu balasan server
            if (document.getElementById('idMemberSelected<%=rnd%>').value != idMemberSaatDicek) return;
            let teks = '<%=Common.getBahasaConfigJS("Saldo")%>: ' + formatRp<%=rnd%>(saldo);
            if (match.min_saldo && Number(match.min_saldo) > 0) {
                teks += ' · <%=Common.getBahasaConfigJS("min. mengendap")%> ' + formatRp<%=rnd%>(match.min_saldo);
            }
            elInfoSaldo.textContent = teks;
        };

        // Fitur "Top Up Saldo lewat POS" (hanya dirender bila bolehTopupPOS -- elemen mungkin tak ada)
        const toggleFormTopupMember<%=rnd%> = () => {
            const wrap = document.getElementById('formTopupMemberWrap<%=rnd%>');
            if (!wrap) return;
            wrap.classList.toggle('d-none');
        };

        const submitTopupMember<%=rnd%> = async () => {
            const idMember = document.getElementById('idMemberSelected<%=rnd%>').value;
            if (!idMember) return;
            const nominal = parseFloat(document.getElementById('inputNominalTopupMember<%=rnd%>').value) || 0;
            if (nominal <= 0) {
                if (typeof tampilkanToast === "function") tampilkanToast('<%=Common.getBahasaConfigJS("Nominal top up harus lebih dari 0.")%>', 'bg-warning text-dark');
                else alert('<%=Common.getBahasaConfigJS("Nominal top up harus lebih dari 0.")%>');
                return;
            }
            const keterangan = document.getElementById('inputKetTopupMember<%=rnd%>').value || '';
            const btn = document.getElementById('btnSubmitTopupMember<%=rnd%>');
            btn.disabled = true;
            try {
                let payload = { action: "topup_saldo", id_member: idMember, nominal: nominal, keterangan: keterangan };
                if (posTanpaLogin<%=rnd%>) payload.tanpaLogin = "true";
                const res = await fetch('<%=Common.ROOT%>/Data', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
                const result = await res.json();
                if (result.status === '00') {
                    document.getElementById('formTopupMemberWrap<%=rnd%>').classList.add('d-none');
                    document.getElementById('inputNominalTopupMember<%=rnd%>').value = '';
                    document.getElementById('inputKetTopupMember<%=rnd%>').value = '';
                    const matchTopup = arrDataMember<%=rnd%>.find(m => String(m.id) === String(idMember));
                    await tampilkanSaldoMemberTerpilih<%=rnd%>(matchTopup);
                    if (typeof tampilkanToast === "function") tampilkanToast('<%=Common.getBahasaConfigJS("Top up berhasil.")%>', 'bg-success text-white');
                    else alert('<%=Common.getBahasaConfigJS("Top up berhasil.")%>');
                } else {
                    const pesan = result.description || '<%=Common.getBahasaConfigJS("Gagal melakukan top up.")%>';
                    if (typeof tampilkanToast === "function") tampilkanToast(pesan, 'bg-danger text-white'); else alert(pesan);
                }
            } finally {
                btn.disabled = false;
            }
        };

        // Load Default Cara Bayar (Semua yang aktif) sebelum member dipilih
        loadMetodePembayaranPOS<%=rnd%>(null);
    };

    // Fungsi Pembantu Pengecekan Saldo
    const checkSaldoTerbaru<%=rnd%> = async (idMember) => {
        if (!idMember) return 0;
        try {
            let payload = { action: "tabungan", id_member: idMember, refresh: true };
            if (posTanpaLogin<%=rnd%>) payload.tanpaLogin = "true";

            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const result = await res.json();
            return (result && result.data !== undefined) ? parseFloat(result.data) : 0;
        } catch (e) {
            console.error("Gagal menarik saldo", e);
            return 0;
        }
    };

    // ==========================================
    // FITUR "SESI KASIR" (Buka/Tutup Kas) -- mesin sama dgn versi ZK/menu Kas Kasir (SesiKasUtil di
    // server), dipanggil lewat 3 aksi baru pada endpoint /Data yang sudah dipakai method2 POS lain di
    // atas: sesi_kas_status (dipanggil saat halaman dimuat & tiap toko berpindah), sesi_kas_buka,
    // sesi_kas_tutup. Status TERAKHIR disimpan di sesiKasInfo<%=rnd%> supaya form Buka/Tutup bisa
    // dibangun tanpa panggilan server tambahan tiap kali toggle dibuka.
    // ==========================================
	let sesiKasInfo<%=rnd%> = { terbuka: false };
	// Identitas instalasi browser stabil. Disimpan lokal agar sesi kas tetap terikat
	// ke mesin/browser yang sama setelah halaman dimuat ulang atau pengguna login lagi.
	const idPerangkatSesiKas<%=rnd%> = (() => {
		const kunci = 'ais-pos-id-perangkat';
		let nilai = localStorage.getItem(kunci);
		if (!nilai) {
			nilai = (window.crypto && window.crypto.randomUUID)
				? window.crypto.randomUUID()
				: 'web-' + Date.now() + '-' + Math.random().toString(36).substring(2, 14);
			localStorage.setItem(kunci, nilai);
		}
		return nilai;
	})();
	const namaPerangkatSesiKas<%=rnd%> = 'Browser ' + (navigator.platform || 'Web');

	const panggilSesiKas<%=rnd%> = async (action, extra) => {
		let payload = Object.assign({
			action: action,
			id_toko: document.getElementById('idTokoSelected<%=rnd%>').value || null,
			id_perangkat: idPerangkatSesiKas<%=rnd%>,
			nama_perangkat: namaPerangkatSesiKas<%=rnd%>
		}, extra || {});
        if (posTanpaLogin<%=rnd%>) payload.tanpaLogin = "true";
        const res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        return await res.json();
    };

    const muatStatusSesiKas<%=rnd%> = async () => {
        const lbl = document.getElementById('lblStatusSesiKas<%=rnd%>');
        const btn = document.getElementById('btnToggleSesiKas<%=rnd%>');
        try {
            const r = await panggilSesiKas<%=rnd%>('sesi_kas_status');
            sesiKasInfo<%=rnd%> = (r && r.status === '00') ? r : { terbuka: false };
        } catch (e) {
            sesiKasInfo<%=rnd%> = { terbuka: false };
        }
        if (sesiKasInfo<%=rnd%>.terbuka) {
            const waktu = new Date(sesiKasInfo<%=rnd%>.waktuBuka);
            const waktuStr = isNaN(waktu.getTime()) ? '' : (' <%=Common.getBahasaConfigJS("sejak")%> ' + waktu.toLocaleString('id-ID', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }));
            lbl.className = 'fw-bold small text-success';
            lbl.textContent = '<%=Common.getBahasaConfigJS("Kas")%>: ' + formatRp<%=rnd%>(sesiKasInfo<%=rnd%>.kasSaatIni) + waktuStr;
            btn.textContent = '<%=Common.getBahasaConfigJS("Tutup Kas")%>';
            btn.className = 'btn btn-sm btn-danger';
        } else {
            lbl.className = 'fw-bold small text-danger';
            lbl.textContent = '<%=Common.getBahasaConfigJS("Kas")%>: <%=Common.getBahasaConfigJS("Tertutup")%>';
            btn.textContent = '<%=Common.getBahasaConfigJS("Buka Kas")%>';
            btn.className = 'btn btn-sm btn-success';
        }
        // Form yang sedang terbuka (bila ada) dibangun ulang supaya konsisten dgn status terbaru.
        const wrap = document.getElementById('formSesiKasWrap<%=rnd%>');
        if (wrap && !wrap.classList.contains('d-none')) renderFormSesiKas<%=rnd%>();
    };

    const toggleFormSesiKas<%=rnd%> = () => {
        const wrap = document.getElementById('formSesiKasWrap<%=rnd%>');
        if (wrap.classList.contains('d-none')) {
            renderFormSesiKas<%=rnd%>();
            wrap.classList.remove('d-none');
        } else {
            wrap.classList.add('d-none');
        }
    };

    const renderFormSesiKas<%=rnd%> = () => {
        const wrap = document.getElementById('formSesiKasWrap<%=rnd%>');
        if (!sesiKasInfo<%=rnd%>.terbuka) {
            wrap.innerHTML =
                '<div class="row g-2 align-items-end">' +
                '<div class="col-sm-4"><label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfigJS("Modal Awal (Rp)")%></label>' +
                '<input type="number" min="0" class="form-control form-control-sm" id="inputModalAwalSesiKas<%=rnd%>" value="0"></div>' +
                '<div class="col-sm-5"><label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfigJS("Keterangan")%></label>' +
                '<input type="text" class="form-control form-control-sm" id="inputKetSesiKas<%=rnd%>"></div>' +
                '<div class="col-sm-3"><button type="button" class="btn btn-success btn-sm w-100" id="btnSubmitSesiKas<%=rnd%>"><%=Common.getBahasaConfigJS("Buka Kas")%></button></div>' +
                '</div>';
            document.getElementById('btnSubmitSesiKas<%=rnd%>').addEventListener('click', submitBukaKas<%=rnd%>);
        } else {
            const seharusnya = (sesiKasInfo<%=rnd%>.modalAwal || 0) + (sesiKasInfo<%=rnd%>.totalTunai || 0);
            wrap.innerHTML =
                '<div class="row g-2 mb-2 small">' +
                '<div class="col-6 col-md-3"><div class="text-muted"><%=Common.getBahasaConfigJS("Modal Awal")%></div><div class="fw-bold">' + formatRp<%=rnd%>(sesiKasInfo<%=rnd%>.modalAwal) + '</div></div>' +
                '<div class="col-6 col-md-3"><div class="text-muted"><%=Common.getBahasaConfigJS("Penjualan Tunai")%></div><div class="fw-bold text-success">' + formatRp<%=rnd%>(sesiKasInfo<%=rnd%>.totalTunai) + '</div></div>' +
                '<div class="col-6 col-md-3"><div class="text-muted"><%=Common.getBahasaConfigJS("Non Tunai")%></div><div class="fw-bold text-primary">' + formatRp<%=rnd%>(sesiKasInfo<%=rnd%>.totalNonTunai) + '</div></div>' +
                '<div class="col-6 col-md-3"><div class="text-muted"><%=Common.getBahasaConfigJS("Kas Seharusnya")%></div><div class="fw-bold">' + formatRp<%=rnd%>(seharusnya) + '</div></div>' +
                '</div>' +
                '<div class="row g-2 align-items-end">' +
                '<div class="col-sm-4"><label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfigJS("Uang Fisik (Rp)")%></label>' +
                '<input type="number" min="0" class="form-control form-control-sm" id="inputUangFisikSesiKas<%=rnd%>" value="' + Math.round(seharusnya) + '"></div>' +
                '<div class="col-sm-5"><label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfigJS("Keterangan")%></label>' +
                '<input type="text" class="form-control form-control-sm" id="inputKetSesiKas<%=rnd%>"></div>' +
                '<div class="col-sm-3"><button type="button" class="btn btn-danger btn-sm w-100" id="btnSubmitSesiKas<%=rnd%>"><%=Common.getBahasaConfigJS("Tutup Kas")%></button></div>' +
                '</div>';
            document.getElementById('btnSubmitSesiKas<%=rnd%>').addEventListener('click', submitTutupKas<%=rnd%>);
        }
    };

    const submitBukaKas<%=rnd%> = async () => {
        const idToko = document.getElementById('idTokoSelected<%=rnd%>').value;
        if (!idToko) {
            if (typeof tampilkanToast === "function") tampilkanToast('<%=Common.getBahasaConfigJS("Pilih toko/pedagang terlebih dahulu sebelum membuka kas.")%>', 'bg-warning text-dark');
            else alert('<%=Common.getBahasaConfigJS("Pilih toko/pedagang terlebih dahulu sebelum membuka kas.")%>');
            return;
        }
        const modalAwal = parseFloat(document.getElementById('inputModalAwalSesiKas<%=rnd%>').value) || 0;
        const keterangan = document.getElementById('inputKetSesiKas<%=rnd%>').value || '';
        const btn = document.getElementById('btnSubmitSesiKas<%=rnd%>');
        btn.disabled = true;
        try {
            const r = await panggilSesiKas<%=rnd%>('sesi_kas_buka', { modal_awal: modalAwal, keterangan: keterangan });
            if (r && r.status === '00') {
                document.getElementById('formSesiKasWrap<%=rnd%>').classList.add('d-none');
                await muatStatusSesiKas<%=rnd%>();
                if (typeof tampilkanToast === "function") tampilkanToast('<%=Common.getBahasaConfigJS("Kas berhasil dibuka.")%>', 'bg-success text-white');
            } else {
                const pesan = (r && r.description) || '<%=Common.getBahasaConfigJS("Gagal membuka kas.")%>';
                if (typeof tampilkanToast === "function") tampilkanToast(pesan, 'bg-danger text-white'); else alert(pesan);
            }
        } finally {
            btn.disabled = false;
        }
    };

    const submitTutupKas<%=rnd%> = async () => {
        const yakin = confirm('<%=Common.getBahasaConfigJS("Apakah Bapak/Ibu yakin ingin menutup kas kasir sekarang? Selisih terhadap kas seharusnya akan dicatat secara permanen.")%>');
        if (!yakin) return;
        const uangFisik = parseFloat(document.getElementById('inputUangFisikSesiKas<%=rnd%>').value) || 0;
        const keterangan = document.getElementById('inputKetSesiKas<%=rnd%>').value || '';
        const btn = document.getElementById('btnSubmitSesiKas<%=rnd%>');
        btn.disabled = true;
        try {
            const r = await panggilSesiKas<%=rnd%>('sesi_kas_tutup', { uang_fisik: uangFisik, keterangan: keterangan });
            if (r && r.status === '00') {
                document.getElementById('formSesiKasWrap<%=rnd%>').classList.add('d-none');
                await muatStatusSesiKas<%=rnd%>();
                const selisih = Number(r.selisih) || 0;
                if (typeof tampilkanToast === "function") tampilkanToast('<%=Common.getBahasaConfigJS("Kas ditutup. Selisih")%>: ' + formatRp<%=rnd%>(selisih), selisih < 0 ? 'bg-danger text-white' : 'bg-success text-white');
            } else {
                const pesan = (r && r.description) || '<%=Common.getBahasaConfigJS("Gagal menutup kas.")%>';
                if (typeof tampilkanToast === "function") tampilkanToast(pesan, 'bg-danger text-white'); else alert(pesan);
            }
        } finally {
            btn.disabled = false;
        }
    };

    // ==========================================
    // FITUR "POPUP PESANAN ONLINE BARU" -- dipoll berkala (bukan real-time push) supaya kasir
    // langsung tahu begitu pembeli checkout lewat toko_online.jsp, tanpa harus manual buka tab
    // "Pesanan". Panggilan PERTAMA hanya mengambil baseline (id tertinggi SAAT INI) -- TIDAK
    // menampilkan popup utk pesanan lama yang sudah menumpuk sebelum kasir membuka halaman ini,
    // supaya tidak membanjiri kasir dgn popup pesanan yang sebenarnya sudah lama menunggu.
    // ==========================================
    let sejakIdPesananBaru<%=rnd%> = null; // null = belum pernah polling (panggilan pertama = baseline)
    let intervalPesananBaru<%=rnd%>;

    const cekPesananBaru<%=rnd%> = async () => {
        try {
            const idToko = document.getElementById('idTokoSelected<%=rnd%>').value;
            let payload = { action: "pesanan_online_baru", id_toko: idToko || null, sejak_id: sejakIdPesananBaru<%=rnd%> || 0 };
            if (posTanpaLogin<%=rnd%>) payload.tanpaLogin = "true";
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const result = await res.json();
            if (result.status !== '00') return;

            const adalahBaseline = (sejakIdPesananBaru<%=rnd%> === null);
            sejakIdPesananBaru<%=rnd%> = result.maksId;
            if (adalahBaseline) return; // panggilan pertama: rekam baseline saja, jangan tampilkan popup

            const daftar = result.pesanan || [];
            if (daftar.length === 0) return;

            let html = '';
            daftar.forEach(p => {
                html += '<div class="border rounded-3 p-3 mb-2 bg-light">' +
                    '<div class="d-flex justify-content-between align-items-start mb-1">' +
                    '<span class="fw-bold text-dark"><i class="fas fa-user me-1 text-primary"></i>' + (p.pembeli || '-') + '</span>' +
                    '<span class="badge bg-primary bg-opacity-10 text-primary">' + (p.waktu || '') + '</span>' +
                    '</div>' +
                    '<div class="small text-muted mb-1"><%=Common.getBahasaConfigJS("Kode")%>: ' + (p.kode || '-') + '</div>' +
                    '<div class="small text-dark"><i class="fas fa-box me-1"></i>' + (p.barang || '-') + '</div>' +
                    '</div>';
            });
            document.getElementById('isiPesananBaru<%=rnd%>').innerHTML = html;
            try {
                new bootstrap.Modal(document.getElementById('modalPesananBaru<%=rnd%>')).show();
            } catch (e) { /* Bootstrap blm siap -- abaikan, tetap coba toast di bawah */ }
            if (typeof tampilkanToast === "function") {
                tampilkanToast('<%=Common.getBahasaConfigJS("Ada")%> ' + daftar.length + ' <%=Common.getBahasaConfigJS("pesanan online baru!")%>', 'bg-primary text-white');
            }
        } catch (e) { /* Polling diam-diam gagal (mis. sedang offline) -- coba lagi di siklus berikutnya */ }
    };

    const mulaiPollingPesananBaru<%=rnd%> = () => {
        cekPesananBaru<%=rnd%>();
        if (intervalPesananBaru<%=rnd%>) clearInterval(intervalPesananBaru<%=rnd%>);
        intervalPesananBaru<%=rnd%> = setInterval(cekPesananBaru<%=rnd%>, 20000);
    };

    const updateUsageDiskonMember<%=rnd%> = async () => {
        const idMember = document.getElementById('idMemberSelected<%=rnd%>').value;
        const currentToko = document.getElementById('idTokoSelected<%=rnd%>').value;
        
        if (!arrAturanDiskon<%=rnd%>) return;

        for (let i = 0; i < arrAturanDiskon<%=rnd%>.length; i++) {
            let rule = arrAturanDiskon<%=rnd%>[i];
            if (rule.berlaku_per_hari_dan_per_toko === 'true' || rule.berlaku_per_hari_dan_per_toko === true || rule.berlaku_per_hari_dan_per_toko === 't') {
                if (idMember !== "" && currentToko !== "") {
                    // Cek riwayat belanja dan draft hari ini untuk idMember (Filter SPESIFIK per TOKO)
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
                    const resUsage = await fetchData<%=rnd%>(sqlUsage);
                    rule.terpakai_hari_ini = (resUsage && resUsage.length > 0) ? parseFloat(resUsage[0].terpakai) : 0;
                } else {
                    rule.terpakai_hari_ini = 0;
                }
            } else {
                rule.terpakai_hari_ini = 0;
            }
            rule.terpakai_di_keranjang = 0; // State bantuan lokal
        }
    };

    /**
     * Cek CSV hari aktif ("1,3,5", 1=Senin..7=Minggu ISO weekday) vs HARI INI -- SAMA konvensi
     * dgn ais.common.HariAktifUtil.aktifPadaHari (Java, ZK+API); kosong/null = berlaku semua hari.
     */
    const hariAktifSekarang<%=rnd%> = (csv) => {
        if (!csv || (csv + '').trim() === '') return true;
        const isoDow = ((new Date().getDay() + 6) % 7) + 1; // JS getDay(): 0=Minggu..6=Sabtu -> ISO 1..7
        return (',' + csv + ',').indexOf(',' + isoDow + ',') >= 0;
    };

    const loadAturanDiskon<%=rnd%> = async () => {
        // Jangan ubah type date jadi _iso langsung di sql, biar action:"sql" yang tambahin otomatis
        const sqlAturan = "SELECT id, produk, toko, berlaku_semua_member, jenis_anggota, tipe_anggota, persentase, maksimal_potongan, nominal, potongan_langsung, berlaku_per_hari_dan_per_toko, tanggal_mulai, tanggal_selesai, hari_aktif, COALESCE(aktivasi_manual,false) AS aktivasi_manual, nama_aturan, keterangan FROM koperasi.aturan_diskon WHERE aktif = true";
        const res = await fetchData<%=rnd%>(sqlAturan);
        if(res) {
            arrAturanDiskon<%=rnd%> = res;
            await updateUsageDiskonMember<%=rnd%>();
        }
    };

    const hitungKembalian<%=rnd%> = () => {
        const inputTunai = parseFloat(document.getElementById('inputUangTunai<%=rnd%>').value) || 0;
        const kembalian = inputTunai - grandTotalValue<%=rnd%>;
        const labelKembalian = document.getElementById('labelKembalian<%=rnd%>');
        
        if (kembalian < 0) {
            labelKembalian.value = "Kurang " + formatRp<%=rnd%>(Math.abs(kembalian));
            labelKembalian.classList.replace('text-success', 'text-danger');
        } else {
            labelKembalian.value = formatRp<%=rnd%>(kembalian);
            labelKembalian.classList.replace('text-danger', 'text-success');
        }
    };
    
    // --- ASYNC FUNCTION UTK CEK DAN LOAD GAMBAR BILA ADA ---
    const checkAndLoadImage<%=rnd%> = async (pId) => {
        try {
            const reqObj = { 
                action: "file", 
                class: "ais.database.model.file.LampiranLain", 
                ref: pId.toString(), 
                jenis: "ais.database.model.inventory.Produk",
                refresh: "false"
            };
            if (posTanpaLogin<%=rnd%>) reqObj.tanpaLogin = "true";

            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(reqObj)
            });
            const dataResponse = await response.json();
            
            if(dataResponse.status === '00' && dataResponse.data && dataResponse.data.nama){ 
                const fileName = dataResponse.data.nama;
                
                if(fileName !== 'administrator-icon_default.png') {
                    let imgUrl = "<%=Common.ROOT%>/Data?action=file&class=ais.database.model.file.LampiranLain&ref=" + pId + "&jenis=ais.database.model.inventory.Produk&render=true";
                    if (posTanpaLogin<%=rnd%>) imgUrl += "&tanpaLogin=true";

                    const wrapper = document.getElementById('prod-icon-wrapper-<%=rnd%>-' + pId);

                    if(wrapper) {
                        wrapper.innerHTML = '<img src="' + imgUrl + '" alt="Produk">';
                    }
                }
            }
        } catch (error) {
            console.error("Gagal menarik metadata gambar POS:", error);
        }
    };

    // ==========================================
    // KATEGORI PRODUK (jenis_produk) -- pill filter di atas grid
    // ==========================================
    let currentKategoriId<%=rnd%> = '';
    const ikonKategori<%=rnd%> = (nama) => {
        const n = (nama || '').toLowerCase();
        if (n.indexOf('minum') >= 0) return 'fa-mug-hot';
        if (n.indexOf('kopi') >= 0) return 'fa-coffee';
        if (n.indexOf('makan') >= 0) return 'fa-utensils';
        if (n.indexOf('snack') >= 0 || n.indexOf('camilan') >= 0) return 'fa-cookie-bite';
        if (n.indexOf('roti') >= 0 || n.indexOf('kue') >= 0) return 'fa-bread-slice';
        if (n.indexOf('alat') >= 0 || n.indexOf('peralatan') >= 0) return 'fa-toolbox';
        return 'fa-ellipsis-h';
    };

    const loadKategoriProduk<%=rnd%> = async () => {
        const wrap = document.getElementById('kategoriPills<%=rnd%>');
        const sql = "SELECT id, nama FROM koperasi.jenis_produk WHERE aktif = true ORDER BY nama ASC";
        const list = await fetchData<%=rnd%>(sql);

        let html = '<span class="pill-kat-<%=rnd%> active" id="pillKat<%=rnd%>-semua" onclick="pilihKategori<%=rnd%>(\'\', this)"><i class="fas fa-th-large me-1"></i><%=Common.getBahasaConfig("Semua")%></span>';
        list.forEach(k => {
            const namaEsc = (k.nama || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
            html += '<span class="pill-kat-<%=rnd%>" onclick="pilihKategori<%=rnd%>(' + k.id + ', this)"><i class="fas ' + ikonKategori<%=rnd%>(k.nama) + ' me-1"></i>' + namaEsc + '</span>';
        });
        wrap.innerHTML = html;
    };

    window.pilihKategori<%=rnd%> = (idKategori, el) => {
        currentKategoriId<%=rnd%> = idKategori;
        document.querySelectorAll('#kategoriPills<%=rnd%> .pill-kat-<%=rnd%>').forEach(p => p.classList.remove('active'));
        if (el) el.classList.add('active');
        loadKatalogProduk<%=rnd%>();
    };

    const loadKatalogProduk<%=rnd%> = async () => {
        const grid = document.getElementById('gridProduk<%=rnd%>');
        const keyword = document.getElementById('searchProduk<%=rnd%>').value.trim().replace(/'/g, "''");

        if (isAdmin<%=rnd%> && currentTokoId<%=rnd%> === "") {
            grid.innerHTML = '<div class="col-12 text-center py-5 text-danger"><i class="fas fa-store-slash fa-3x mb-3"></i><br><%=Common.getBahasaConfig("Silakan pilih Toko/Pedagang terlebih dahulu.")%></div>';
            return;
        }

        grid.innerHTML = '<div class="col-12 text-center py-5"><div class="spinner-border text-primary"></div></div>';

        // Gap-closure "Jenis Item" (Produk vs Bahan Baku/Ekstra) -- bahan baku & ekstra TIDAK boleh
        // dijual langsung lewat Kasir sbg baris mandiri, hanya dipakai via resep/picker ekstra produk
        // lain. "<>" polos TIDAK match NULL di Postgres (produk lama sebelum kolom ini ada), jadi
        // WAJIB pola OR IS NULL. `ekstra_pilihan` ikut diambil supaya addToCart tahu apa produk ini
        // punya ekstra yg bisa dipilih (buka modal picker) sebelum push ke keranjang.
        // Gap-closure "Harga Coret" -- preview diskon di level katalog (sebelum masuk keranjang).
        // Hanya promo PUBLIK (berlaku_semua_member) yg dipreview di sini krn belum ada member terpilih
        // di titik ini (sama spt evaluateDiscount<%=rnd%> di bawah); hanya potongan_langsung=true krn
        // cashback tidak mengubah harga struk; aktivasi_manual DIKECUALIKAN krn baru aktif lewat picker
        // "Promo" manual, bukan preview otomatis. hari_aktif dicek via CSV-membership (ISODOW Postgres
        // = 1..7 Senin..Minggu, SAMA konvensi dgn ais.common.HariAktifUtil).
        const kondisiDiskonAktif<%=rnd%> = "d.aktif = true AND (d.produk IS NULL OR d.produk = a.id) AND (d.toko IS NULL OR d.toko = a.toko) " +
            "AND (d.tanggal_mulai IS NULL OR d.tanggal_mulai <= now()) AND (d.tanggal_selesai IS NULL OR d.tanggal_selesai >= now()) " +
            "AND COALESCE(d.berlaku_semua_member,false) = true AND COALESCE(d.potongan_langsung,true) = true " +
            "AND COALESCE(d.aktivasi_manual,false) = false " +
            "AND (d.hari_aktif IS NULL OR d.hari_aktif = '' OR (',' || d.hari_aktif || ',') LIKE ('%,' || EXTRACT(ISODOW FROM now())::int || ',%'))";
        let sqlQuery = "SELECT a.id, a.kode, a.nama, a.hargajual, COALESCE(a.stok,0) AS stok, a.ekstra_pilihan, " +
                       "(SELECT MAX(d.persentase) FROM koperasi.aturan_diskon d WHERE " + kondisiDiskonAktif<%=rnd%> + " AND COALESCE(d.persentase,0) > 0) AS diskon_persen, " +
                       "(SELECT MAX(d.maksimal_potongan) FROM koperasi.aturan_diskon d WHERE " + kondisiDiskonAktif<%=rnd%> + " AND COALESCE(d.persentase,0) > 0) AS diskon_maks_potongan, " +
                       "(SELECT MAX(d.nominal) FROM koperasi.aturan_diskon d WHERE " + kondisiDiskonAktif<%=rnd%> + " AND COALESCE(d.persentase,0) = 0 AND COALESCE(d.nominal,0) > 0) AS diskon_nominal " +
                       "FROM koperasi.produk a WHERE a.aktif = true AND a.toko = " + currentTokoId<%=rnd%> +
                       " AND (a.jenis_item IS NULL OR a.jenis_item NOT IN ('BAHAN','EKSTRA')) ";
        if (currentKategoriId<%=rnd%> !== '') {
            sqlQuery += " AND a.jenis_produk = " + currentKategoriId<%=rnd%> + " ";
        }
        if (keyword !== "") {
            // OR ke barcode jg (gap-closure) -- kotak cari yg sama dipakai kasir utk ketik nama/kode
            // MAUPUN scan barcode fisik produk.
            sqlQuery += " AND (a.nama ILIKE '%" + keyword + "%' OR a.kode = '" + keyword + "' OR a.barcode = '" + keyword + "') ";
        }
        sqlQuery += " ORDER BY a.nama ASC LIMIT 60;";

        const produkList = await fetchData<%=rnd%>(sqlQuery);
        let htmlGrid = '';

        if (produkList.length === 0) {
            htmlGrid = '<div class="col-12 text-center py-5 text-muted"><i class="fas fa-box-open fa-3x mb-3 opacity-50"></i><br><%=Common.getBahasaConfig("Produk tidak ditemukan.")%></div>';
        } else {
            const icons = ['fa-box', 'fa-coffee', 'fa-hamburger', 'fa-ice-cream', 'fa-pizza-slice', 'fa-cookie'];

            produkList.forEach((p, idx) => {
                const icon = icons[idx % icons.length];
                const pId = p.id;
                const pKode = p.kode || '';
                const pNama = p.nama.replace(/'/g, "\\'").replace(/"/g, '&quot;');
                const pHarga = parseFloat(p.hargajual || 0);
                const pStok = parseFloat(p.stok || 0);
                const diskonPersen = parseFloat(p.diskon_persen || 0);
                const diskonMaksPotongan = parseFloat(p.diskon_maks_potongan || 0);
                const diskonNominal = parseFloat(p.diskon_nominal || 0);
                // Gap-closure "Harga Coret" -- persentase diprioritaskan drpd nominal (SAMA urutan dgn
                // evaluateDiscount<%=rnd%>/mesin server), lalu dibatasi maksimal_potongan bila ada.
                let hargaPromo<%=rnd%> = null;
                if (diskonPersen > 0) {
                    let potongan = pHarga * (diskonPersen / 100);
                    if (diskonMaksPotongan > 0 && potongan > diskonMaksPotongan) potongan = diskonMaksPotongan;
                    hargaPromo<%=rnd%> = pHarga - potongan;
                } else if (diskonNominal > 0) {
                    hargaPromo<%=rnd%> = pHarga - Math.min(diskonNominal, pHarga);
                }
                const adaPromo<%=rnd%> = hargaPromo<%=rnd%> !== null && hargaPromo<%=rnd%> < pHarga;
                // Gap-closure "Produk Ekstra" -- diteruskan sbg 5th arg (string JSON id array, mis.
                // "[601,602]", atau string kosong bila produk ini tak punya ekstra) ke addToCart supaya
                // ia tahu perlu buka modal picker sebelum push ke keranjang atau tidak.
                const pEkstraPilihan = (p.ekstra_pilihan || '').replace(/'/g, "\\'");
                const stokBadge = pStok <= 0 ? '<span class="badge bg-danger position-absolute top-0 end-0 m-1" style="font-size:9px;z-index:2;"><%=Common.getBahasaConfig("Habis")%></span>' : (pStok <= 5 ? '<span class="badge bg-warning text-dark position-absolute top-0 end-0 m-1" style="font-size:9px;z-index:2;">Stok ' + pStok + '</span>' : '');
                const hargaHtml<%=rnd%> = adaPromo<%=rnd%>
                    ? '<span class="prod-pr-wrap-<%=rnd%>"><span class="prod-pr-coret-<%=rnd%>">' + formatRp<%=rnd%>(pHarga) + '</span><span class="prod-pr-promo-<%=rnd%>">' + formatRp<%=rnd%>(hargaPromo<%=rnd%>) + '</span></span>'
                    : '<span class="prod-pr-<%=rnd%>">' + formatRp<%=rnd%>(pHarga) + '</span>';

                htmlGrid += '<div class="col-lg-4 col-md-6 col-sm-6">' +
                        '<div class="prod-card-<%=rnd%>" onclick="addToCart<%=rnd%>(' + pId + ', \'' + pKode + '\', \'' + pNama + '\', ' + pHarga + ', \'' + pEkstraPilihan + '\')">' +
                            '<div class="prod-thumb-<%=rnd%>" id="prod-icon-wrapper-<%=rnd%>-' + pId + '">' +
                                (diskonPersen > 0 ? '<span class="prod-badge-diskon-<%=rnd%>">' + Math.round(diskonPersen) + '%</span>' : (adaPromo<%=rnd%> ? '<span class="prod-badge-diskon-<%=rnd%>">Promo</span>' : '')) +
                                stokBadge +
                                '<i class="fas ' + icon + '"></i>' +
                            '</div>' +
                            '<div class="prod-body-<%=rnd%>">' +
                                '<div class="prod-nm-<%=rnd%>" title="' + p.nama + '">' + p.nama + '</div>' +
                                '<div class="prod-foot-<%=rnd%>">' +
                                    hargaHtml<%=rnd%> +
                                    '<span class="prod-add-<%=rnd%>">+</span>' +
                                '</div>' +
                            '</div>' +
                        '</div>' +
                    '</div>';
            });
        }

        grid.innerHTML = htmlGrid;

        if(produkList.length > 0) {
            produkList.forEach(p => { checkAndLoadImage<%=rnd%>(p.id); });
        }
    };

    // ==========================================
    // LOGIKA ATURAN DISKON & KERANJANG
    // ==========================================
    /** Member yg dipilih kasir saat ini, atau null (dipakai evaluateDiscount + picker Promo manual). */
    const memberTerpilihSaatIni<%=rnd%> = () => {
        const idMember = document.getElementById('idMemberSelected<%=rnd%>').value;
        if (idMember && arrDataMember<%=rnd%>.length > 0) {
            return arrDataMember<%=rnd%>.find(m => m.id == idMember) || null;
        }
        return null;
    };

    /**
     * Cek kelayakan SATU aturan (produk/toko/tanggal/hari/member) utk satu baris keranjang -- TIDAK
     * termasuk cek aktivasi_manual (itu keputusan PEMANGGIL). Dipakai bersama oleh evaluateDiscount
     * (auto-apply &amp; apply-1-aturan-terpilih) dan bukaPickerPromoManual (listing).
     */
    const aturanEligibleUntukItem<%=rnd%> = (rule, item, selectedMember) => {
        const now = new Date();
        const isSpecificProduct = (rule.produk !== null && rule.produk !== '' && rule.produk !== undefined);
        if ((isSpecificProduct == true || isSpecificProduct == 'true') && rule.produk != item.id) return false;

        const isSpecificToko = (rule.toko !== null && rule.toko !== '' && rule.toko !== undefined);
        if ((isSpecificToko == true || isSpecificToko == 'true') && rule.toko != currentTokoId<%=rnd%>) return false;

        if (rule.tanggal_mulai_iso && rule.tanggal_mulai_iso !== '' && new Date(rule.tanggal_mulai_iso) > now) return false;
        if (rule.tanggal_selesai_iso && rule.tanggal_selesai_iso !== '' && new Date(rule.tanggal_selesai_iso) < now) return false;
        if (!hariAktifSekarang<%=rnd%>(rule.hari_aktif)) return false;

        const isAllMember = (rule.berlaku_semua_member === 'true' || rule.berlaku_semua_member === true || rule.berlaku_semua_member === 't');
        if (!isAllMember) {
            if (!selectedMember) return false; // Butuh member spesifik tapi kasir belum pilih member
            if (rule.jenis_anggota && rule.jenis_anggota !== '' && rule.jenis_anggota != selectedMember.jenis_anggota) return false;
            if (rule.tipe_anggota && rule.tipe_anggota !== '' && rule.tipe_anggota != selectedMember.tipe_anggota) return false;
        }
        return true;
    };

    /**
     * @param onlyRuleId bila diisi (kasir sudah pilih 1 promo manual lewat picker "Promo", lihat
     *                   {@code bukaPickerPromoManual}/{@code pilihPromoManual}): HANYA hitung aturan
     *                   dgn id itu (aktivasi_manual DIABAIKAN krn kasir sudah pilih eksplisit), pakai
     *                   rumus yg SAMA PERSIS dgn auto-apply. Kosong/null (default): perilaku auto-apply
     *                   biasa, aturan aktivasi_manual=true DIKECUALIKAN.
     */
    const evaluateDiscount<%=rnd%> = (item, onlyRuleId) => {
        let appliedRule = null;
        const selectedMember = memberTerpilihSaatIni<%=rnd%>();

        for (let rule of arrAturanDiskon<%=rnd%>) {
            if (onlyRuleId != null) {
                if (rule.id != onlyRuleId) continue;
            } else {
                const isManual = (rule.aktivasi_manual === true || rule.aktivasi_manual === 'true' || rule.aktivasi_manual === 't');
                if (isManual) continue; // dikecualikan dari auto-apply -- hanya lewat picker "Promo" manual
            }

            if (!aturanEligibleUntukItem<%=rnd%>(rule, item, selectedMember)) continue;

            appliedRule = rule;
            break;
        }

        item.diskon = 0;
        item.cashback = 0;
        item.aturanDiskon = null;
        item.berlakuPerHariDanPerToko = false;

        if (appliedRule) {
            let discountValue = 0;
            const itemTotalBeforeDisc = item.harga * item.jumlah;
            const persen = parseFloat(appliedRule.persentase) || 0;
            const maxPot = parseFloat(appliedRule.maksimal_potongan) || 0;
            const nom = parseFloat(appliedRule.nominal) || 0;

            if (persen > 0) {
                discountValue = itemTotalBeforeDisc * (persen / 100);
            } else if (nom > 0) {
                discountValue = nom * item.jumlah;
                if(discountValue > itemTotalBeforeDisc) discountValue = itemTotalBeforeDisc; 
            }

            const isBerlaku1x = (appliedRule.berlaku_per_hari_dan_per_toko === 'true' || appliedRule.berlaku_per_hari_dan_per_toko === true || appliedRule.berlaku_per_hari_dan_per_toko === 't');

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
            item.berlakuPerHariDanPerToko = isBerlaku1x;
        }
    };

    const recalculateCart<%=rnd%> = () => {
        // Reset akumulator keranjang setiap kali dihitung ulang
        if(arrAturanDiskon<%=rnd%>) {
            arrAturanDiskon<%=rnd%>.forEach(r => r.terpakai_di_keranjang = 0);
        }
        cart<%=rnd%>.forEach(item => {
            // Gap-closure "Aktivasi Manual" -- baris yg promonya dipilih manual kasir (item.promoManual)
            // TETAP dihitung ulang (bukan dibekukan) supaya batas maksimal_potongan tetap akurat, tapi
            // dgn onlyRuleId terkunci ke promo itu saja -- bukan ikut auto-apply biasa.
            evaluateDiscount<%=rnd%>(item, item.promoManual ? item.aturanDiskon : null);
        });
        renderCart<%=rnd%>();
    };

    /**
     * Gap-closure "Aktivasi Manual" -- buka picker daftar promo `aktivasi_manual=true` yang eligible
     * utk MINIMAL SATU baris keranjang saat ini (reuse aturanEligibleUntukItem, sama predikat yg
     * dipakai auto-apply, cuma dibalik: hanya ambil yang manual, bukan yang di-skip).
     */
    const bukaPickerPromoManual<%=rnd%> = () => {
        const body = document.getElementById('bodyModalPromoManual<%=rnd%>');
        if (cart<%=rnd%>.length === 0) {
            body.innerHTML = '<p class="text-muted text-center py-3 mb-0"><%=Common.getBahasaConfig("Keranjang masih kosong.")%></p>';
            return;
        }
        const selectedMember = memberTerpilihSaatIni<%=rnd%>();
        const promoEligible = arrAturanDiskon<%=rnd%>.filter(rule => {
            const isManual = (rule.aktivasi_manual === true || rule.aktivasi_manual === 'true' || rule.aktivasi_manual === 't');
            if (!isManual) return false;
            return cart<%=rnd%>.some(item => aturanEligibleUntukItem<%=rnd%>(rule, item, selectedMember));
        });

        if (promoEligible.length === 0) {
            body.innerHTML = '<p class="text-muted text-center py-3 mb-0"><%=Common.getBahasaConfig("Tidak ada promo manual yang tersedia untuk keranjang saat ini.")%></p>';
            return;
        }

        let html = '<div class="list-group">';
        promoEligible.forEach(rule => {
            const aktifSekarang = cart<%=rnd%>.some(item => item.promoManual && item.aturanDiskon == rule.id);
            const persen = parseFloat(rule.persentase) || 0;
            const nominal = parseFloat(rule.nominal) || 0;
            const isPotongLsg = (rule.potongan_langsung === 'true' || rule.potongan_langsung === true || rule.potongan_langsung === 't');
            const deskripsiNilai = persen > 0 ? ('Potongan ' + persen + '%') : (nominal > 0 ? ('Potongan ' + formatRp<%=rnd%>(nominal)) : 'Promo');
            const namaAturan = (rule.nama_aturan || 'Promo').replace(/'/g, "\\'").replace(/"/g, '&quot;');
            html += '<button type="button" class="list-group-item list-group-item-action' + (aktifSekarang ? ' active' : '') + '" onclick="pilihPromoManual<%=rnd%>(' + rule.id + ')">' +
                '<div class="d-flex justify-content-between align-items-center">' +
                    '<div><div class="fw-bold">' + namaAturan + '</div><div class="small' + (aktifSekarang ? '' : ' text-muted') + '">' + deskripsiNilai + (isPotongLsg ? '' : ' (cashback)') + '</div></div>' +
                    (aktifSekarang ? '<i class="fas fa-check-circle"></i>' : '') +
                '</div>' +
            '</button>';
        });
        html += '</div>';
        body.innerHTML = html;
    };

    /** Kasir memilih 1 promo manual dari picker -- terapkan ke SEMUA baris keranjang yang eligible. */
    window.pilihPromoManual<%=rnd%> = (aturanId) => {
        const selectedMember = memberTerpilihSaatIni<%=rnd%>();
        let adaYangDapat = false;
        cart<%=rnd%>.forEach(item => {
            const rule = arrAturanDiskon<%=rnd%>.find(r => r.id == aturanId);
            if (rule && aturanEligibleUntukItem<%=rnd%>(rule, item, selectedMember)) {
                item.promoManual = true;
                adaYangDapat = true;
            }
        });
        if (!adaYangDapat) {
            const pesan = '<%=Common.getBahasaConfigJS("Promo ini tidak berlaku untuk produk di keranjang saat ini.")%>';
            if (typeof tampilkanToast === "function") tampilkanToast(pesan, 'bg-warning text-dark'); else alert(pesan);
            return;
        }
        recalculateCart<%=rnd%>();
        const modalEl = document.getElementById('modalPromoManual<%=rnd%>');
        const modal = modalEl && window.bootstrap ? bootstrap.Modal.getInstance(modalEl) : null;
        if (modal) modal.hide();
        const pesanSukses = '<%=Common.getBahasaConfigJS("Promo diterapkan.")%>';
        if (typeof tampilkanToast === "function") tampilkanToast(pesanSukses, 'bg-success text-white');
    };

    const addToCart<%=rnd%> = async (id, kode, nama, harga, ekstraPilihanJson) => {
        const idMember = document.getElementById('idMemberSelected<%=rnd%>').value;
        if(idMember) {
             // Selalu refresh saldo saat memilih barang
             await checkSaldoTerbaru<%=rnd%>(idMember);
        }

        // Gap-closure "Produk Ekstra" -- produk dgn ekstra_pilihan terisi WAJIB lewat modal picker
        // dulu (kasir centang topping/tambahan apa saja) sebelum push ke keranjang. Produk tanpa
        // ekstra (mayoritas) tetap langsung masuk spt sebelumnya lewat pushKeKeranjang dgn ekstra=[].
        let idsEkstra = [];
        if (ekstraPilihanJson) {
            try {
                const arr = JSON.parse(ekstraPilihanJson);
                if (Array.isArray(arr) && arr.length > 0) idsEkstra = arr;
            } catch(e) { /* produk lama/rusak tanpa ekstra valid -- perlakukan spt tanpa ekstra */ }
        }

        if (idsEkstra.length > 0) {
            bukaModalEkstra<%=rnd%>(id, kode, nama, harga, idsEkstra);
            return;
        }

        pushKeKeranjang<%=rnd%>(id, kode, nama, harga, []);
    };

    /**
     * Bandingkan dua pilihan ekstra (array of {id,...} ATAU array of id polos) apakah SAMA PERSIS
     * (set id identik, urutan tak penting). Dipakai sbg bagian kunci merge baris keranjang -- baris
     * produk yg sama TAPI beda pilihan ekstra harus jadi baris keranjang TERPISAH.
     */
    const sameEkstraSelection<%=rnd%> = (a, b) => {
        const idsA = (a || []).map(x => (x && typeof x === 'object' ? x.id : x)).map(String).sort();
        const idsB = (b || []).map(x => (x && typeof x === 'object' ? x.id : x)).map(String).sort();
        if (idsA.length !== idsB.length) return false;
        return idsA.every((v, i) => v === idsB[i]);
    };

    /** Push/merge aktual ke keranjang -- dipakai baik oleh jalur langsung (tanpa ekstra) maupun oleh konfirmasiModalEkstra (dgn ekstra). */
    const pushKeKeranjang<%=rnd%> = (id, kode, nama, harga, ekstraArr) => {
        const ekstra = Array.isArray(ekstraArr) ? ekstraArr : [];
        const existingItem = cart<%=rnd%>.find(item => item.id === id && sameEkstraSelection<%=rnd%>(item.ekstra, ekstra));
        if (existingItem) {
            existingItem.jumlah += 1;
        } else {
            let newItem = { id: id, kode: kode, nama: nama, harga: harga, jumlah: 1, diskon: 0, cashback: 0, aturanDiskon: null, berlakuPerHariDanPerToko: false, ekstra: ekstra };
            cart<%=rnd%>.push(newItem);
        }
        recalculateCart<%=rnd%>();
    };

    // ==========================================
    // MODAL PILIH EKSTRA (MODIFIER/ADD-ON)
    // ==========================================
    let ekstraModalPending<%=rnd%> = null; // {id, kode, nama, harga} produk dasar yg sedang menunggu pilihan ekstra di modal

    const bukaModalEkstra<%=rnd%> = async (id, kode, nama, harga, idsEkstra) => {
        ekstraModalPending<%=rnd%> = { id: id, kode: kode, nama: nama, harga: harga };
        document.getElementById('judulModalEkstra<%=rnd%>').innerText = nama;
        const body = document.getElementById('bodyModalEkstra<%=rnd%>');
        body.innerHTML = '<div class="text-center text-muted py-3"><div class="spinner-border spinner-border-sm text-primary"></div></div>';

        const modalEl = document.getElementById('modalEkstraPilihan<%=rnd%>');
        new bootstrap.Modal(modalEl).show();

        // Resolve id->{nama,harga} LIVE dari DB (bukan cache) -- JSP ini selalu online, tidak perlu
        // dijaga terhadap offline-cache spt Electron/Flutter. Filter numerik dulu (defensif thd data
        // ekstra_pilihan yg rusak/bukan angka) sebelum dirangkai jadi klausa SQL IN (...).
        const idsEkstraAman = idsEkstra.map(x => parseInt(x)).filter(x => !isNaN(x));
        if (idsEkstraAman.length === 0) {
            body.innerHTML = '<div class="text-center text-muted py-3"><%=Common.getBahasaConfig("Data ekstra tidak ditemukan.")%></div>';
            return;
        }
        const sql = "SELECT id, kode, nama, COALESCE(hargajual,0) AS hargajual FROM koperasi.produk WHERE id IN (" + idsEkstraAman.join(',') + ") ORDER BY nama ASC";
        const opsi = await fetchData<%=rnd%>(sql);

        if (!opsi || opsi.length === 0) {
            body.innerHTML = '<div class="text-center text-muted py-3"><%=Common.getBahasaConfig("Data ekstra tidak ditemukan.")%></div>';
            return;
        }

        let html = '';
        opsi.forEach(p => {
            const namaEks = (p.nama || '').replace(/"/g, '&quot;');
            const hargaEks = parseFloat(p.hargajual || 0);
            html += '<div class="form-check d-flex justify-content-between align-items-center border-bottom py-2">' +
                        '<div class="form-check">' +
                            '<input class="form-check-input" type="checkbox" value="' + p.id + '" id="ckEkstra<%=rnd%>-' + p.id + '" ' +
                                'data-kode="' + (p.kode || '') + '" data-nama="' + namaEks + '" data-harga="' + hargaEks + '">' +
                            '<label class="form-check-label" for="ckEkstra<%=rnd%>-' + p.id + '">' + (p.nama || '') + '</label>' +
                        '</div>' +
                        '<span class="text-muted small">' + formatRp<%=rnd%>(hargaEks) + '</span>' +
                    '</div>';
        });
        body.innerHTML = html;
    };

    const konfirmasiModalEkstra<%=rnd%> = () => {
        if (!ekstraModalPending<%=rnd%>) return;
        const body = document.getElementById('bodyModalEkstra<%=rnd%>');
        const ekstraArr = [];
        body.querySelectorAll('input[type="checkbox"]:checked').forEach(ck => {
            ekstraArr.push({
                id: parseInt(ck.value),
                kode: ck.getAttribute('data-kode') || '',
                nama: ck.getAttribute('data-nama') || '',
                harga: parseFloat(ck.getAttribute('data-harga')) || 0,
                jumlah: 1
            });
        });
        const p = ekstraModalPending<%=rnd%>;
        ekstraModalPending<%=rnd%> = null;

        const modalEl = document.getElementById('modalEkstraPilihan<%=rnd%>');
        const inst = window.bootstrap ? bootstrap.Modal.getInstance(modalEl) : null;
        if (inst) inst.hide();

        pushKeKeranjang<%=rnd%>(p.id, p.kode, p.nama, p.harga, ekstraArr);
    };

    const updateQty<%=rnd%> = async (index, delta) => {
        const idMember = document.getElementById('idMemberSelected<%=rnd%>').value;
        if(idMember) {
             await checkSaldoTerbaru<%=rnd%>(idMember);
        }

        cart<%=rnd%>[index].jumlah += delta;
        if (cart<%=rnd%>[index].jumlah <= 0) {
            cart<%=rnd%>.splice(index, 1);
        }
        recalculateCart<%=rnd%>();
    };

    const kosongkanKeranjang<%=rnd%> = () => {
        cart<%=rnd%> = [];
        currentDraftId<%=rnd%> = null;
        splitSelectedMetodeBayar<%=rnd%> = [];
        renderSplitPanel<%=rnd%>();
        recalculateCart<%=rnd%>();
        document.getElementById('inputUangTunai<%=rnd%>').value = '';
        hitungKembalian<%=rnd%>();
    };

    // ==========================================
    // KERANJANG TERTAHAN (hold/park sale) -- reuse KantinHelper.draft_bayar (koperasi.
    // draft_pembelian_anggota_koperasi/draft_pembelian) yg SUDAH dipakai & teruji di modul
    // Pesanan (_draft_pesanan_anggota.jsp). Kolom native SQL diambil PERSIS dari file itu
    // (hargasatuan/totalcashback tanpa underscore -- BUKAN tebakan konvensi).
    // ==========================================
    const loadDaftarTertahan<%=rnd%> = async () => {
        const idToko = document.getElementById('idTokoSelected<%=rnd%>').value;
        const listEl = document.getElementById('listKeranjangTertahan<%=rnd%>');
        const badge = document.getElementById('badgeTertahan<%=rnd%>');
        if (!idToko) {
            listEl.innerHTML = '<div class="text-center text-muted small py-3"><%=Common.getBahasaConfig("Pilih Toko/Pedagang dahulu.")%></div>';
            badge.classList.add('d-none');
            return;
        }
        const sql = "SELECT a.id, TO_CHAR(a.tanggal_pembayaran,'DD-MM HH24:MI') as waktu, a.kode, " +
                    "b.nama as pemesan, COALESCE(a.total_biaya,0) as total, " +
                    "(SELECT COUNT(*) FROM koperasi.draft_pembelian d WHERE d.draft_pembelian_anggota_koperasi = a.id) as jml_item " +
                    "FROM koperasi.draft_pembelian_anggota_koperasi a " +
                    "LEFT JOIN koperasi.anggota_koperasi b ON a.anggota_koperasi = b.id " +
                    "WHERE a.lunas IS NULL AND a.toko = " + idToko + " ORDER BY a.tanggal_pembayaran DESC LIMIT 30;";
        const res = await fetchData<%=rnd%>(sql);

        if (!res || res.length === 0) {
            listEl.innerHTML = '<div class="text-center text-muted small py-4"><i class="fas fa-inbox fa-2x mb-2 opacity-50 d-block"></i><%=Common.getBahasaConfig("Belum ada keranjang yang ditahan.")%></div>';
            badge.classList.add('d-none');
            return;
        }
        badge.textContent = res.length;
        badge.classList.remove('d-none');

        let html = '';
        res.forEach(r => {
            html += '<div class="d-flex justify-content-between align-items-center border rounded-3 p-3 mb-2">' +
                        '<div>' +
                            '<div class="fw-bold text-dark" style="font-size:13px;">' + (r.pemesan || '<%=Common.getBahasaConfigJS("Walk-in Customer")%>') + '</div>' +
                            '<div class="text-muted small">' + r.waktu + ' &bull; ' + r.jml_item + ' <%=Common.getBahasaConfig("item")%> &bull; ' + formatRp<%=rnd%>(r.total) + '</div>' +
                        '</div>' +
                        '<button class="btn btn-sm btn-primary rounded-pill" onclick="muatKeranjangTertahan<%=rnd%>(' + r.id + ')" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Muat")%></button>' +
                    '</div>';
        });
        listEl.innerHTML = html;
    };

    window.muatKeranjangTertahan<%=rnd%> = async (idDraft) => {
        // Gap-closure "Produk Ekstra" -- d.id (draft_item_id) & d.induk_id ikut diambil supaya baris
        // flat draft_pembelian bisa diregroup balik jadi cart bersarang (baris dasar + .ekstra[]) di
        // bawah. Baris lama (sebelum fitur ini ada) punya induk_id NULL utk semuanya -- tetap
        // ter-flatten jadi baris dasar biasa, TIDAK ada perubahan perilaku.
        const sqlItems = "SELECT p.id as produk_id, p.kode, COALESCE(p.nama, d.nama) as nama, " +
            "d.hargasatuan as harga, d.qty as jumlah, COALESCE(d.diskon,0) as diskon, COALESCE(d.cashback,0) as cashback, d.aturan_diskon, " +
            "d.id AS draft_item_id, d.induk_id " +
            "FROM koperasi.draft_pembelian d LEFT JOIN koperasi.produk p ON d.produk = p.id " +
            "WHERE d.draft_pembelian_anggota_koperasi = " + idDraft + ";";
        const resItems = await fetchData<%=rnd%>(sqlItems);
        if (!resItems || resItems.length === 0) {
            if (typeof tampilkanToast === "function") tampilkanToast('<%=Common.getBahasaConfigJS("Keranjang tertahan ini kosong/sudah tak valid.")%>', 'bg-warning text-dark');
            return;
        }

        // Pass 1: baris dasar (induk_id kosong) -- jadi baris cart, kunci sementara _draftItemId
        // dipakai pass 2 utk mencocokkan ekstra ke induknya, lalu dibuang.
        const barisDasar = resItems.filter(it => !it.induk_id);
        const barisEkstra = resItems.filter(it => !!it.induk_id);

        const cartBaru = barisDasar.map(it => ({
            id: it.produk_id, kode: it.kode || '', nama: it.nama, harga: parseFloat(it.harga || 0),
            jumlah: parseFloat(it.jumlah || 0), diskon: parseFloat(it.diskon || 0), cashback: parseFloat(it.cashback || 0),
            aturanDiskon: it.aturan_diskon || null, berlakuPerHariDanPerToko: false,
            ekstra: [], _draftItemId: it.draft_item_id
        }));

        // Pass 2: tempelkan tiap baris ekstra ke baris dasar yg draft_item_id-nya = induk_id baris ini.
        barisEkstra.forEach(it => {
            const induk = cartBaru.find(c => String(c._draftItemId) === String(it.induk_id));
            if (!induk) return; // induk sudah tak ada/tak valid -- lewati diam2 drpd error
            induk.ekstra.push({ id: it.produk_id, kode: it.kode || '', nama: it.nama, harga: parseFloat(it.harga || 0), jumlah: 1 });
        });

        cartBaru.forEach(c => { delete c._draftItemId; });
        cart<%=rnd%> = cartBaru;
        currentDraftId<%=rnd%> = idDraft;

        // Muat balik header (member+metode bayar) supaya kasir tinggal lanjut Bayar
        const sqlHeader = "SELECT anggota_koperasi, cara_pembayaran_koperasi FROM koperasi.draft_pembelian_anggota_koperasi WHERE id = " + idDraft + ";";
        const resHeader = await fetchData<%=rnd%>(sqlHeader);
        if (resHeader && resHeader[0] && resHeader[0].anggota_koperasi) {
            const idM = resHeader[0].anggota_koperasi;
            const match = arrDataMember<%=rnd%>.find(m => String(m.id) === String(idM));
            if (match) {
                const iden = match.kode_identitas ? match.kode_identitas : '-';
                document.getElementById('inputMember<%=rnd%>').value = match.nama + ' (' + iden + ')';
                document.getElementById('idMemberSelected<%=rnd%>').value = match.id;
                document.getElementById('minSaldoMemberSelected<%=rnd%>').value = match.min_saldo || 0;
                await loadMetodePembayaranPOS<%=rnd%>(match.jenis_anggota);
            }
        }

        recalculateCart<%=rnd%>();
        if (typeof tampilkanToast === "function") tampilkanToast('<%=Common.getBahasaConfigJS("Keranjang tertahan dimuat, silakan lanjutkan.")%>', 'bg-success text-white');
    };

    const simpanKeranjangTertahan<%=rnd%> = async () => {
        if (cart<%=rnd%>.length === 0) {
            if (typeof tampilkanToast === "function") tampilkanToast('<%=Common.getBahasaConfigJS("Keranjang masih kosong, tidak ada yang disimpan.")%>', 'bg-warning text-dark');
            return;
        }
        const idToko = document.getElementById('idTokoSelected<%=rnd%>').value;
        if (!idToko) {
            if (typeof tampilkanToast === "function") tampilkanToast('<%=Common.getBahasaConfigJS("Silakan pilih Toko/Pedagang.")%>', 'bg-warning text-dark');
            return;
        }
        const selectBayar = document.getElementById('selectCaraBayar<%=rnd%>');
        // caraBayar WAJIB diisi backend (draft_bayar) -- pakai yg sedang dipilih, atau opsi
        // pertama yg tersedia bila kasir belum memilih (murni penanda administratif, tak
        // menagih siapa pun sampai keranjang ini benar2 di-"Bayar").
        let idCaraBayar = selectBayar.value;
        if (!idCaraBayar && selectBayar.options.length > 1) {
            idCaraBayar = selectBayar.options[1].value;
        }
        if (!idCaraBayar) {
            if (typeof tampilkanToast === "function") tampilkanToast('<%=Common.getBahasaConfigJS("Belum ada metode pembayaran yang bisa dipakai utk menyimpan.")%>', 'bg-danger text-white');
            return;
        }

        const btn = document.getElementById('btnSimpanTertahan<%=rnd%>');
        const oriHtml = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm"></span>';

		const payload = {
            action: "draft_bayar",
            id: currentDraftId<%=rnd%>,
            kodeUnik: "DRAFT-" + generateUniqueCode50(),
            idToko: idToko,
            waktu: getFormattedDate<%=rnd%>(),
            id_member: document.getElementById('idMemberSelected<%=rnd%>').value,
			caraBayar: idCaraBayar,
			id_perangkat: idPerangkatSesiKas<%=rnd%>,
			nama_perangkat: namaPerangkatSesiKas<%=rnd%>,
			nama_mesin: namaPerangkatSesiKas<%=rnd%>,
			kode_sesi_kas: sesiKasInfo<%=rnd%>.kodeSesiKas || null,
            transaksi: cart<%=rnd%>.map(item => ({
                id: item.id, kode: item.kode, nama: item.nama, harga: item.harga, jumlah: item.jumlah,
                diskon: item.diskon, aturanDiskon: item.aturanDiskon, cashback: item.cashback,
                ekstra: item.ekstra || [] // Gap-closure "Produk Ekstra" -- kosong utk baris tanpa ekstra, byte-identical spt sebelumnya
            }))
        };
        if (posTanpaLogin<%=rnd%>) payload.tanpaLogin = "true";

        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
            });
            const hasil = await res.json();
            if (hasil.status === '00' || hasil.status === 'success') {
                if (typeof tampilkanToast === "function") tampilkanToast('<%=Common.getBahasaConfigJS("Keranjang disimpan, bisa dilanjutkan lewat tombol Tertahan.")%>', 'bg-success text-white');
                kosongkanKeranjang<%=rnd%>();
                document.getElementById('inputMember<%=rnd%>').value = '';
                document.getElementById('idMemberSelected<%=rnd%>').value = '';
                loadDaftarTertahan<%=rnd%>();
            } else {
                if (typeof tampilkanToast === "function") tampilkanToast(hasil.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan keranjang.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            console.error(e);
            if (typeof tampilkanToast === "function") tampilkanToast('<%=Common.getBahasaConfigJS("Gagal menyimpan. Periksa jaringan Anda.")%>', 'bg-danger text-white');
        } finally {
            btn.disabled = false;
            btn.innerHTML = oriHtml;
        }
    };

    const renderCart<%=rnd%> = () => {
        const container = document.getElementById('cartContainer<%=rnd%>');
        let subtotal = 0;
        let totalDiskon = 0;
        let totalCashback = 0;

        if (cart<%=rnd%>.length === 0) {
            container.innerHTML = '<div class="text-center text-muted py-4"><i class="fas fa-shopping-basket fa-2x mb-2 opacity-50"></i><br><%=Common.getBahasaConfig("Keranjang masih kosong")%></div>';
            grandTotalValue<%=rnd%> = 0;
        } else {
            let htmlCart = '';
            cart<%=rnd%>.forEach((item, index) => {
                // Gap-closure "Produk Ekstra" -- ekstra ikut menambah nilai baris ini. Server
                // mengalikan harga tiap ekstra dgn jumlah INDUK (lihat KantinHelper.
                // ratakanTransaksiDenganEkstra, jumlahEkstra selalu 1 dari sisi kita), jadi
                // kontribusinya ke subtotal HARUS dihitung persis sama di sini supaya total yg
                // ditampilkan/divalidasi (tunai, QR, potong saldo, struk) SELALU sinkron dgn total
                // yang sebenarnya tersimpan di server.
                const ekstraList = item.ekstra || [];
                const ekstraHargaPerParent = ekstraList.reduce((s, ek) => s + ((parseFloat(ek.harga) || 0) * (parseFloat(ek.jumlah) || 1)), 0);
                const itemTotal = (item.harga * item.jumlah) - item.diskon + (ekstraHargaPerParent * item.jumlah);
                subtotal += (item.harga * item.jumlah) + (ekstraHargaPerParent * item.jumlah);
                totalDiskon += item.diskon;
                totalCashback += item.cashback;

                // Indikator Promo UI
                let notePromo = '';
                if(item.diskon > 0) notePromo += '<span class="badge bg-danger text-white rounded-pill mt-1 px-2" style="font-size: 9.5px;"><i class="fas fa-cut me-1"></i>Potong ' + formatRp<%=rnd%>(item.diskon) + '</span> ';
                if(item.cashback > 0) notePromo += '<span class="badge bg-info text-dark rounded-pill mt-1 px-2" style="font-size: 9.5px;"><i class="fas fa-wallet me-1"></i>Cashback ' + formatRp<%=rnd%>(item.cashback) + '</span>';

                // Sub-list ekstra (topping/tambahan) terpilih, terindentasi di bawah nama produk induk
                let ekstraHtmlBaris = '';
                if (ekstraList.length > 0) {
                    ekstraHtmlBaris = '<div class="mt-1">' + ekstraList.map(ek =>
                        '<div class="text-muted" style="font-size:10.5px;padding-left:6px;">+ ' + ek.nama + (ek.harga ? ' <span class="text-secondary">(' + formatRp<%=rnd%>(ek.harga) + ')</span>' : '') + '</div>'
                    ).join('') + '</div>';
                }

                htmlCart += '<div class="cart-item-<%=rnd%>">' +
                        '<div class="cart-thumb-<%=rnd%>"><i class="fas fa-box"></i></div>' +
                        '<div class="flex-grow-1">' +
                            '<div class="fw-bold text-dark" style="font-size:13px;">' + item.nama + '</div>' +
                            '<div class="text-muted" style="font-size:11px;">' + formatRp<%=rnd%>(item.harga) + '</div>' +
                            ekstraHtmlBaris +
                            (notePromo ? '<div class="mt-1">' + notePromo + '</div>' : '') +
                        '</div>' +
                        '<div class="qty-pill-<%=rnd%>">' +
                            '<span class="qty-btn-<%=rnd%>" onclick="updateQty<%=rnd%>(' + index + ', -1)">-</span>' +
                            '<span style="min-width:16px;text-align:center;font-weight:800;font-size:12px;">' + item.jumlah + '</span>' +
                            '<span class="qty-btn-<%=rnd%>" onclick="updateQty<%=rnd%>(' + index + ', 1)">+</span>' +
                        '</div>' +
                        '<div class="text-end fw-bold text-dark" style="font-size:12.5px;min-width:78px;">' + formatRp<%=rnd%>(itemTotal) +
                            '<div class="text-danger" style="font-size:15px;line-height:1;cursor:pointer;margin-top:2px;" onclick="updateQty<%=rnd%>(' + index + ', -' + item.jumlah + ')" title="<%=Common.getBahasaConfig("Hapus")%>"><i class="fas fa-times"></i></div>' +
                        '</div>' +
                    '</div>';
            });
            container.innerHTML = htmlCart;
            const basePajak = subtotal - totalDiskon;
            grandPajakValue<%=rnd%> = pajakPersenPOS<%=rnd%> > 0 ? (basePajak * pajakPersenPOS<%=rnd%> / 100) : 0;
            grandTotalValue<%=rnd%> = basePajak + grandPajakValue<%=rnd%>;
        }
        if (cart<%=rnd%>.length === 0) { grandPajakValue<%=rnd%> = 0; }

        document.getElementById('cartSubtotal<%=rnd%>').innerText = formatRp<%=rnd%>(subtotal);
        document.getElementById('cartDiskon<%=rnd%>').innerText = "- " + formatRp<%=rnd%>(totalDiskon);
        document.getElementById('cartCashback<%=rnd%>').innerText = "+ " + formatRp<%=rnd%>(totalCashback);
        const rowPajakEl = document.getElementById('rowPajak<%=rnd%>');
        if (pajakPersenPOS<%=rnd%> > 0) {
            rowPajakEl.style.display = 'flex';
            document.getElementById('cartPajak<%=rnd%>').innerText = formatRp<%=rnd%>(grandPajakValue<%=rnd%>);
        } else {
            rowPajakEl.style.display = 'none';
        }
        document.getElementById('cartGrandTotal<%=rnd%>').innerText = formatRp<%=rnd%>(grandTotalValue<%=rnd%>);

        const elTokoLp = document.getElementById('inputToko<%=rnd%>');
        const elMemberLp = document.getElementById('inputMember<%=rnd%>');
        kirimKeLayarPelanggan<%=rnd%>({
            tipe: 'keranjang',
            namaToko: (isAdmin<%=rnd%> && elTokoLp && elTokoLp.value) ? elTokoLp.value : '<%=namaTokoAktif%>',
            namaMember: elMemberLp ? elMemberLp.value : '',
            items: cart<%=rnd%>.map(it => ({ nama: it.nama, harga: it.harga, jumlah: it.jumlah, diskon: it.diskon })),
            subtotal: subtotal, totalDiskon: totalDiskon, totalCashback: totalCashback, totalPajak: grandPajakValue<%=rnd%>,
            grandTotal: grandTotalValue<%=rnd%>
        });

        hitungKembalian<%=rnd%>();
    };

    // Fungsi Cetak Struk
    const cetakStruk<%=rnd%> = (payload, totalBayar, uangTunai, kembalian) => {
        const elToko = document.getElementById('inputToko<%=rnd%>');
        const namaToko = isAdmin<%=rnd%> && elToko && elToko.value ? elToko.value : '<%=namaTokoAktif%>';
        
        let htmlStruk = '<html><head><title>Cetak Struk</title>';
        htmlStruk += '<style>';
        // Font tebal (700) + text-stroke + print-color-adjust: cetakan thermal keluar tipis/buram
        // kalau font default dipakai apa adanya (lihat catatan sama di cetak_struk.jsp/struk.js).
        htmlStruk += 'body { font-family: "Courier New", Courier, monospace; width: 300px; font-size: 14px; font-weight: 700; -webkit-text-stroke: 0.4px #000; margin: 0 auto; padding: 10px; color: #000; -webkit-print-color-adjust: exact; print-color-adjust: exact; } ';
        htmlStruk += '.text-center { text-align: center; } ';
        htmlStruk += '.text-right { text-align: right; } ';
        htmlStruk += '.bold { font-weight: 900; -webkit-text-stroke: 0.6px #000; } ';
        htmlStruk += '.border-top { border-top: 2px dashed #000; margin-top: 5px; padding-top: 5px; } ';
        htmlStruk += '.border-bottom { border-bottom: 2px dashed #000; margin-bottom: 5px; padding-bottom: 5px; } ';
        htmlStruk += 'table { width: 100%; border-collapse: collapse; } ';
        htmlStruk += 'td { vertical-align: top; } ';
        htmlStruk += '</style></head><body>';
        
        htmlStruk += '<div class="text-center bold border-bottom">';
        htmlStruk += '<h3 style="margin:5px 0;">' + (namaToko || 'Koperasi Utama') + '</h3>';
        htmlStruk += '<div style="margin-bottom:5px;">Struk Pembayaran</div>';
        htmlStruk += '</div>';
        
        htmlStruk += '<div style="margin-bottom:5px;">';
        htmlStruk += 'Waktu : ' + payload.waktu + '<br>';
        htmlStruk += 'No    : ' + payload.kodeUnik + '<br>';
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
                htmlStruk += '<tr><td colspan="2" style="padding-bottom:5px;">&nbsp;&nbsp;<i>Diskon Promo</i></td><td class="text-right">-' + formatRp<%=rnd%>(itemDiskon) + '</td></tr>';
            }
            if(itemCashback > 0) {
                htmlStruk += '<tr><td colspan="2" style="padding-bottom:5px;">&nbsp;&nbsp;<i>Cashback Didapat</i></td><td class="text-right">+' + formatRp<%=rnd%>(itemCashback) + '</td></tr>';
            }

            // Gap-closure "Produk Ekstra" -- flatten baris ekstra langsung di bawah baris induknya,
            // diindentasi spt tambahan/topping. Harga per baris = harga ekstra x jumlah INDUK (server
            // mengalikan persis sama, lihat KantinHelper.ratakanTransaksiDenganEkstra).
            const ekstraStruk = item.ekstra || [];
            ekstraStruk.forEach(ek => {
                const hargaEk = parseFloat(ek.harga) || 0;
                const subEk = hargaEk * item.jumlah;
                subtotalAwal += subEk;
                htmlStruk += '<tr><td colspan="2" style="padding-bottom:5px;">&nbsp;&nbsp;+ ' + ek.nama + '</td><td class="text-right" style="padding-bottom:5px;">' + formatRp<%=rnd%>(subEk) + '</td></tr>';
            });
        });
        
        htmlStruk += '</table></div>';
        
        htmlStruk += '<table>';
        htmlStruk += '<tr><td>Subtotal</td><td class="text-right">' + formatRp<%=rnd%>(subtotalAwal) + '</td></tr>';
        
        if (totalDiskonStruk > 0) {
            htmlStruk += '<tr><td>Total Diskon</td><td class="text-right">-' + formatRp<%=rnd%>(totalDiskonStruk) + '</td></tr>';
        }

        if (payload.pajak > 0) {
            htmlStruk += '<tr><td>Pajak (' + pajakPersenPOS<%=rnd%> + '%)</td><td class="text-right">' + formatRp<%=rnd%>(payload.pajak) + '</td></tr>';
        }

        htmlStruk += '<tr><td class="bold">Total Bayar</td><td class="text-right bold">' + formatRp<%=rnd%>(totalBayar) + '</td></tr>';
        
        if (totalCashbackStruk > 0) {
            htmlStruk += '<tr><td class="bold">Total Cashback</td><td class="text-right bold">+' + formatRp<%=rnd%>(totalCashbackStruk) + '</td></tr>';
        }
        
        if (uangTunai > 0) {
            htmlStruk += '<tr><td>Uang Tunai</td><td class="text-right">' + formatRp<%=rnd%>(uangTunai) + '</td></tr>';
            htmlStruk += '<tr><td>Kembalian</td><td class="text-right">' + formatRp<%=rnd%>(kembalian) + '</td></tr>';
        }
        
        htmlStruk += '</table>';
        htmlStruk += '<div class="text-center border-top" style="margin-top: 15px;">Terima Kasih<br>Selamat Belanja Kembali</div>';
        htmlStruk += '</body></html>';

        try{
	        const printWindow = window.open('', '', 'height=600,width=400');
	        printWindow.document.write(htmlStruk);
	        printWindow.document.close();
	        printWindow.focus();
	        setTimeout(() => {
	            printWindow.print();
	            //printWindow.close();
	        }, 500);
        }catch(e){}
    };

    // ==========================================
    // LOGIKA PEMBAYARAN & QR CODE
    // ==========================================
    const initiatePembayaran<%=rnd%> = async () => {
        if (cart<%=rnd%>.length === 0) {
            if (typeof tampilkanToast === "function") {
                tampilkanToast('<%=Common.getBahasaConfigJS("Keranjang belanja kosong! Silakan pilih produk.")%>', 'bg-warning text-dark');
            } else {
                alert('<%=Common.getBahasaConfigJS("Keranjang belanja kosong! Silakan pilih produk.")%>');
            }
            return;
        }

        const idToko = document.getElementById('idTokoSelected<%=rnd%>').value;
        const idMember = document.getElementById('idMemberSelected<%=rnd%>').value;
        const selectBayar = document.getElementById('selectCaraBayar<%=rnd%>');
        const idCaraBayar = selectBayar.value;

        if (idToko === "") {
            if (typeof tampilkanToast === "function") {
                tampilkanToast('<%=Common.getBahasaConfigJS("Silakan pilih Toko/Pedagang.")%>', 'bg-warning text-dark');
            } else {
                alert('<%=Common.getBahasaConfigJS("Silakan pilih Toko/Pedagang.")%>');
            }
            return;
        }
        if (idCaraBayar === "") {
            if (typeof tampilkanToast === "function") {
                tampilkanToast('<%=Common.getBahasaConfigJS("Pilih Metode Pembayaran.")%>', 'bg-warning text-dark');
            } else {
                alert('<%=Common.getBahasaConfigJS("Pilih Metode Pembayaran.")%>');
            }
            return;
        }

        const namaCaraBayar = selectBayar.options[selectBayar.selectedIndex].getAttribute('data-nama').toLowerCase();
        const optCaraBayar = selectBayar.options[selectBayar.selectedIndex];
        const isSplitAktif = splitSelectedMetodeBayar<%=rnd%>.length >= 2;

        // --- VALIDASI SALDO MENGENDAP (HANYA UTK NOMINAL YG BENAR2 MEMOTONG SALDO) ---
        // Mode split: jumlahkan HANYA slot yg metodenya non-manual (potong saldo), BUKAN seluruh
        // grandTotal -- persis permintaan "nominal yg memotong deposit bisa ada di salah satu dari
        // 5 kolom". Mode lama (1 metode): identik spt sebelumnya (isManual dari satu-satunya slot).
        const nominalPotongSaldo = isSplitAktif
            ? splitSelectedMetodeBayar<%=rnd%>.filter(s => !s.manual).reduce((sum, s) => sum + (parseFloat(s.nominal) || 0), 0)
            : ((optCaraBayar && optCaraBayar.getAttribute('data-manual') === 'true') ? 0 : grandTotalValue<%=rnd%>);

        if (nominalPotongSaldo > 0) {
            if (idMember === "") {
                if (typeof tampilkanToast === "function") {
                    tampilkanToast('<%=Common.getBahasaConfigJS("Silakan pilih Member (Pelanggan) untuk metode potong saldo.")%>', 'bg-warning text-dark');
                } else {
                    alert('<%=Common.getBahasaConfigJS("Silakan pilih Member (Pelanggan) untuk metode potong saldo.")%>');
                }
                return;
            }

            // Ambil saldo ter-update
            const currentSaldo = await checkSaldoTerbaru<%=rnd%>(idMember);
            const minimalDeposit = parseFloat(document.getElementById('minSaldoMemberSelected<%=rnd%>').value) || 0;
            const memberSaatIni = arrDataMember<%=rnd%>.find(m => String(m.id) === String(idMember));
            const namaMemberSaatIni = memberSaatIni ? memberSaatIni.nama : '<%=Common.getBahasaConfigJS("pelanggan ini")%>';

            if (currentSaldo < nominalPotongSaldo) {
                const pesanSaldoKurang = '<%=Common.getBahasaConfigJS("Maaf, saldo")%> ' + namaMemberSaatIni + ' <%=Common.getBahasaConfigJS("tidak mencukupi untuk transaksi ini")%> (<%=Common.getBahasaConfigJS("saldo")%> ' + formatRp<%=rnd%>(currentSaldo) + ', <%=Common.getBahasaConfigJS("nominal yg memotong saldo")%> ' + formatRp<%=rnd%>(nominalPotongSaldo) + ').';
                if (typeof tampilkanToast === "function") {
                    tampilkanToast(pesanSaldoKurang, 'bg-danger text-white');
                } else {
                    alert(pesanSaldoKurang);
                }
                return;
            }

            // Validasi Minimal Saldo Mengendap
            if ((currentSaldo - nominalPotongSaldo) < minimalDeposit) {
                const pesanMengendapKurang = '<%=Common.getBahasaConfigJS("Transaksi ditolak! Sisa saldo")%> ' + namaMemberSaatIni + ' <%=Common.getBahasaConfigJS("setelah transaksi kurang dari batas saldo mengendap yang diizinkan")%> (' + formatRp<%=rnd%>(minimalDeposit) + ').';
                if (typeof tampilkanToast === "function") {
                    tampilkanToast(pesanMengendapKurang, 'bg-danger text-white');
                } else {
                    alert(pesanMengendapKurang);
                }
                return;
            }
        }

        // --- GATE PIN PEMBELI (hanya bila jenis anggota member terpilih "Wajib PIN") ---
        // Verifikasi dilakukan PEMBELI SENDIRI di Layar Pelanggan (layar kedua); transaksi baru
        // lanjut disimpan setelah kasir menerima balasan 'pin_hasil' ok=true lewat BroadcastChannel.
        const wajibPin = document.getElementById('wajibPinMemberSelected<%=rnd%>').value === 'true';
        if (idMember !== "" && wajibPin) {
            if (!custWin<%=rnd%> || custWin<%=rnd%>.closed) {
                if (typeof tampilkanToast === "function") {
                    tampilkanToast('<%=Common.getBahasaConfigJS("Buka Layar Pelanggan terlebih dahulu untuk verifikasi PIN pembeli.")%>', 'bg-warning text-dark');
                } else {
                    alert('<%=Common.getBahasaConfigJS("Buka Layar Pelanggan terlebih dahulu untuk verifikasi PIN pembeli.")%>');
                }
                return;
            }
            const memberNamaPin = document.getElementById('inputMember<%=rnd%>').value || '';
            pinPendingResolve<%=rnd%> = () => lanjutkanPembayaranSetelahPin<%=rnd%>(idToko, idMember, idCaraBayar, namaCaraBayar);
            kirimKeLayarPelanggan<%=rnd%>({ tipe: 'minta_pin', memberId: idMember, memberNama: memberNamaPin });
            if (typeof tampilkanToast === "function") {
                tampilkanToast('<%=Common.getBahasaConfigJS("Menunggu pembeli memasukkan PIN di Layar Pelanggan...")%>', 'bg-info text-dark');
            }
            return;
        }

        lanjutkanPembayaranSetelahPin<%=rnd%>(idToko, idMember, idCaraBayar, namaCaraBayar);
    };

    /** Lanjutan initiatePembayaran setelah lolos (atau tak butuh) verifikasi PIN pembeli. */
    const lanjutkanPembayaranSetelahPin<%=rnd%> = (idToko, idMember, idCaraBayar, namaCaraBayar) => {
        const btnBayar = document.getElementById('btnBayar<%=rnd%>');
        const oriBtnHtml = btnBayar.innerHTML;
        btnBayar.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyimpan...")%>';
        btnBayar.disabled = true;

        const kodeTransaksi = "POS-" + generateUniqueCode50();
        const curWaktu = getFormattedDate<%=rnd%>();

        // Mengirimkan Transaksi dan Ekstra Atribut Diskon & Cashback
        const payload = {
            action: "bayar",
            kodeUnik: kodeTransaksi,
            idToko: idToko,
            waktu: curWaktu,
            id_member: idMember,
            caraBayar: idCaraBayar,
            log:"true",
            draftPembelianAnggotaKoperasi: currentDraftId<%=rnd%>, // bila keranjang ini hasil "Muat" dr Tertahan, tuntaskan draft yg SAMA (bukan duplikat)
            pajak: grandPajakValue<%=rnd%>, // nominal Rp -- KantinHelper.bayar() menjumlahkan field ini LANGSUNG ke total tersimpan
            transaksi: cart<%=rnd%>.map(item => ({
            	id: item.id,
                kode: item.kode,
                nama: item.nama,
                harga: item.harga,
                jumlah: item.jumlah,
                diskon: item.diskon,
                aturanDiskon: item.aturanDiskon, // Inject ID Promo ke backend
                cashback: item.cashback,          // Inject Cashback ke backend
                berlakuPerHariDanPerToko: item.berlakuPerHariDanPerToko,
                ekstra: item.ekstra || [] // Gap-closure "Produk Ekstra" -- kosong utk baris tanpa ekstra, byte-identical spt sebelumnya
            }))
        };

        if (posTanpaLogin<%=rnd%>) payload.tanpaLogin = "true";

        const isSplitAktif = splitSelectedMetodeBayar<%=rnd%>.length >= 2;
        if (isSplitAktif) {
            // Slot 1 (payload.caraBayar di atas) SUDAH metode pertama yg dicentang -- kirim sisanya
            // (slot 2-5) sbg caraBayarTambahan. Nominal slot 1 TIDAK dikirim eksplisit -- server
            // menghitungnya implisit dari totalBiaya dikurangi slot 2-5 (lihat
            // PembelianAnggotaKoperasi.getNominalBayar1()), jadi harus SELALU konsisten dgn total di sini.
            payload.caraBayarTambahan = splitSelectedMetodeBayar<%=rnd%>.slice(1).map(s => ({ caraBayar: s.id, nominal: s.nominal }));
            // Split: QR-code besar & panel kembalian tunai diambil dari model "satu metode = seluruh
            // total", tidak berlaku saat displit ke banyak metode -- langsung eksekusi seperti metode
            // non-tunai/non-QR biasa (nominal per metode sudah pasti dari panel split, bukan dihitung ulang).
            eksekusiPembayaranBackend<%=rnd%>(payload, 0, 0);
            return;
        }

        if (namaCaraBayar.includes('online') || namaCaraBayar.includes('qris') || namaCaraBayar.includes('topup')) {
            tampilkanModalQR<%=rnd%>(payload, grandTotalValue<%=rnd%>);
            btnBayar.innerHTML = oriBtnHtml;
            btnBayar.disabled = false;
        } else {
            let uangTunai = 0;
            let kembalian = 0;
            // Gap-closure "Ada Kembalian" -- dulu ditebak dari nama ('tunai'/'cash'), sekarang dari
            // kolom sungguhan (lihat handleCaraBayarChange<%=rnd%>/metodeBayarAdaKembalian<%=rnd%>).
            // Metode TANPA kembalian (QRIS/Transfer/Voucher/Kasbon dst) WAJIB bayar pas -- uangTunai
            // dikunci = total persis, tidak dibaca dari input (panelnya memang disembunyikan).
            if (metodeBayarAdaKembalian<%=rnd%>()) {
                uangTunai = parseFloat(document.getElementById('inputUangTunai<%=rnd%>').value) || 0;
                kembalian = uangTunai - grandTotalValue<%=rnd%>;
                if (uangTunai < grandTotalValue<%=rnd%>) {
                    if (typeof tampilkanToast === "function") {
                        tampilkanToast('<%=Common.getBahasaConfigJS("Uang tunai kurang!")%>', 'bg-danger text-white');
                    } else {
                        alert('<%=Common.getBahasaConfigJS("Uang tunai kurang!")%>');
                    }
                    document.getElementById('inputUangTunai<%=rnd%>').focus();
                    btnBayar.innerHTML = oriBtnHtml;
                    btnBayar.disabled = false;
                    return;
                }
            } else {
                uangTunai = grandTotalValue<%=rnd%>;
                kembalian = 0;
            }
            eksekusiPembayaranBackend<%=rnd%>(payload, uangTunai, kembalian);
        }
    };

    const tampilkanModalQR<%=rnd%> = (payload, totalBayar) => {
        const modalEl = document.getElementById('modalQRCode<%=rnd%>');
        const modalInstance = new bootstrap.Modal(modalEl);
        
        document.getElementById('qrTotalBayar<%=rnd%>').innerText = formatRp<%=rnd%>(totalBayar);
        
        const qrDataObj = {
            nominal: totalBayar,
            kodeToko: payload.idToko,
            waktu: payload.waktu,
            kodeUnik: payload.kodeUnik
        };
        const qrString = JSON.stringify(qrDataObj);
        
        const qrWrapper = document.getElementById('qrcodeWrapper<%=rnd%>');
        qrWrapper.innerHTML = ''; 
        
        qrGeneratorInstance<%=rnd%> = new QRCode(qrWrapper, {
            text: qrString,
            width: 200,
            height: 200,
            colorDark : "#000000",
            colorLight : "#ffffff",
            correctLevel : QRCode.CorrectLevel.H
        });

        modalInstance.show();
        isQRCancelled<%=rnd%> = false;
        kirimKeLayarPelanggan<%=rnd%>({ tipe: 'qris', qrString: qrString, totalBayar: totalBayar });
        mulaiPollingQR<%=rnd%>(payload.kodeUnik, payload, totalBayar);
    };

    const batalkanQR<%=rnd%> = () => {
        isQRCancelled<%=rnd%> = true;
        kirimKeLayarPelanggan<%=rnd%>({ tipe: 'batal_qris' });
        if(qrCheckInterval<%=rnd%>) clearInterval(qrCheckInterval<%=rnd%>);
        if (typeof tampilkanToast === "function") {
            tampilkanToast('<%=Common.getBahasaConfigJS("Pembayaran Online dibatalkan oleh kasir.")%>', 'bg-secondary text-white');
        } else {
            alert('<%=Common.getBahasaConfigJS("Pembayaran Online dibatalkan oleh kasir.")%>');
        }
    };

    const mulaiPollingQR<%=rnd%> = (kodeUnik, payloadUtama, totalBayar) => {
        if(qrCheckInterval<%=rnd%>) clearInterval(qrCheckInterval<%=rnd%>);
        
        qrCheckInterval<%=rnd%> = setInterval(async () => {
            if (isQRCancelled<%=rnd%>) {
                clearInterval(qrCheckInterval<%=rnd%>);
                return;
            }

            try {
                let pollPayload = { 
                    action: "checkBayar", 
                    kodeUnik: kodeUnik 
                };
                if (posTanpaLogin<%=rnd%>) pollPayload.tanpaLogin = "true";

                const response = await fetch('<%=Common.ROOT%>/Data', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(pollPayload)
                });

                const data = await response.json();
				
                if (data && data.status === '00') {
                    clearInterval(qrCheckInterval<%=rnd%>);
                    
                    const modalEl = document.getElementById('modalQRCode<%=rnd%>');
                    const modalInstance = bootstrap.Modal.getInstance(modalEl);
                    modalInstance.hide();
                    
                    if (typeof tampilkanToast === "function") {
                        tampilkanToast('<%=Common.getBahasaConfigJS("Pembayaran Online Diterima!")%>', 'bg-success text-white');
                    } else {
                        alert('<%=Common.getBahasaConfigJS("Pembayaran Online Diterima!")%>');
                    }
                    
                    payloadUtama.kodePembayaranOnline = data.data;
                    payloadUtama.id_member = data.member;
					
                    eksekusiPembayaranBackend<%=rnd%>(payloadUtama, 0, 0);
                }
            } catch (err) {
                console.error("Polling QR Error:", err);
            }

        }, 2000); 
    };

    // === POS Offline-First: bangun objek transaksi + badge status (dipakai mode offline). ===
    // Fase 1 (perbaikan celah lama): sebelumnya field caraBayar/member SUDAH dikirim di objek ini,
    // tapi diskon/cashback per-item TIDAK ikut disalin ke `items` -- sehingga hilang begitu transaksi
    // ini disinkronkan lewat pos_offline_service.jsp (yang juga sudah diperbaiki agar memetakan
    // seluruh field ini, bukan cuma qty/harga). caraBayar/member sendiri sudah benar dikirim di sini;
    // gap-nya ada di sisi server yang sebelumnya tidak membacanya -- lihat pos_offline_service.jsp.
    const posTrxOffline<%=rnd%> = (payload, uangTunai, kembalian) => ({
        clientTrxId: payload.kodeUnik, tokoId: payload.idToko, waktu: payload.waktu,
        caraBayar: payload.caraBayar, caraBayarTambahan: payload.caraBayarTambahan, member: payload.id_member,
        total: grandTotalValue<%=rnd%>, bayar: uangTunai, kembalian: kembalian,
        items: (payload.transaksi || []).map(t => ({
            produkId: t.id, qty: t.jumlah, harga: t.harga,
            diskon: t.diskon || 0, cashback: t.cashback || 0
        }))
    });
    const posBadge<%=rnd%> = (st) => {
        var b = document.getElementById('posNetBadge<%=rnd%>');
        if (!b) {
            b = document.createElement('div'); b.id = 'posNetBadge<%=rnd%>';
            b.style.cssText = 'position:fixed;top:74px;right:16px;z-index:9999;font-weight:700;padding:6px 12px;border-radius:999px;box-shadow:0 2px 8px rgba(0,0,0,.18);font-size:.8rem;cursor:pointer;';
            b.title = '<%=Common.getBahasaConfigJS("Klik untuk lihat status sinkronisasi & sinkronkan manual")%>';
            b.onclick = () => bukaStatusSinkron<%=rnd%>();
            document.body.appendChild(b);
        }
        var pend = st && st.pending ? (' • ' + st.pending + ' tertunda') : '';
        if (st && st.online) { b.style.background = '#16a34a'; b.style.color = '#fff'; b.textContent = '● Online' + pend; }
        else { b.style.background = '#dc2626'; b.style.color = '#fff'; b.textContent = '● OFFLINE' + pend; }
    };

    // === Panel "Status Sinkronisasi": lihat transaksi yang masih tertunda di perangkat ini + tombol
    // sinkron manual. Sebelumnya badge di atas hanya menampilkan ANGKA jumlah tertunda tanpa cara
    // melihat detail/memaksa kirim -- kasir harus menunggu timer 30 detik atau reload halaman. ===
    const formatWaktuSinkron<%=rnd%> = (epochMillis) => {
        if (!epochMillis) return '<%=Common.getBahasaConfig("Belum pernah")%>';
        try { return new Date(epochMillis).toLocaleString('id-ID', { dateStyle: 'medium', timeStyle: 'medium' }); }
        catch (e) { return new Date(epochMillis).toLocaleString(); }
    };
    const muatStatusSinkron<%=rnd%> = async () => {
        if (!window.AisPosOffline) return;
        const tbody = document.getElementById('ssTabelPending<%=rnd%>');
        document.getElementById('ssKoneksi<%=rnd%>').innerHTML = AisPosOffline.isOnline()
            ? '<span class="text-success"><i class="fas fa-wifi me-1"></i><%=Common.getBahasaConfig("Online")%></span>'
            : '<span class="text-danger"><i class="fas fa-wifi-slash me-1"></i><%=Common.getBahasaConfig("Offline")%></span>';
        try {
            const [daftar, terakhir] = await Promise.all([AisPosOffline.listPending(), AisPosOffline.getLastSyncAt()]);
            document.getElementById('ssPending<%=rnd%>').textContent = daftar.length;
            document.getElementById('ssTerakhir<%=rnd%>').textContent = formatWaktuSinkron<%=rnd%>(terakhir);
            if (!daftar.length) {
                tbody.innerHTML = '<tr><td colspan="4" class="text-center text-success py-3"><i class="fas fa-check-circle me-1"></i><%=Common.getBahasaConfig("Semua transaksi sudah terkirim ke server.")%></td></tr>';
                return;
            }
            tbody.innerHTML = daftar.map(t => {
                const waktu = t.waktu ? new Date(t.waktu).toLocaleString('id-ID', { dateStyle: 'short', timeStyle: 'short' }) : '-';
                const status = AisPosOffline.isOnline()
                    ? '<span class="badge bg-warning text-dark"><%=Common.getBahasaConfig("Sedang dicoba")%></span>'
                    : '<span class="badge bg-secondary"><%=Common.getBahasaConfig("Menunggu koneksi")%></span>';
                return '<tr><td>' + waktu + '</td><td class="text-monospace small">' + (t.clientTrxId || '-') + '</td>'
                    + '<td class="text-end">' + formatRp<%=rnd%>(t.total || 0) + '</td><td>' + status + '</td></tr>';
            }).join('');
        } catch (e) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center text-danger py-3"><%=Common.getBahasaConfig("Gagal membaca data lokal.")%></td></tr>';
        }
    };
    const bukaStatusSinkron<%=rnd%> = () => {
        const el = document.getElementById('modalStatusSinkron<%=rnd%>');
        if (!el) return;
        muatStatusSinkron<%=rnd%>();
        try { new bootstrap.Modal(el).show(); } catch (e) { /* Bootstrap belum siap */ }
    };
    // Halaman "Sinkronisasi POS" adalah tab ketiga di pos.jsp (sibling dari tab POS/Riwayat ini,
    // sama-sama di-include dgn rnd yg sama) -- pindah ke tab itu drpd buka jendela baru. Bila
    // _pos.jsp ini kebetulan ditampilkan berdiri sendiri (tanpa pembungkus 3-tab), tab itu tidak
    // ada -- jangan error, cukup beri tahu lewat toast.
    const bukaRiwayatSinkron<%=rnd%> = () => {
        const tabTujuan = document.getElementById('tab-sinkron-<%=rnd%>');
        if (tabTujuan) {
            try { new bootstrap.Tab(tabTujuan).show(); } catch (e) { tabTujuan.click(); }
            const modalEl = document.getElementById('modalStatusSinkron<%=rnd%>');
            if (modalEl) { try { bootstrap.Modal.getOrCreateInstance(modalEl).hide(); } catch (e) { /* abaikan */ } }
        } else if (typeof tampilkanToast === "function") {
            tampilkanToast('<%=Common.getBahasaConfigJS("Buka tab \"Sinkronisasi POS\" pada menu utama Kantin/POS untuk melihat riwayat lengkap.")%>', 'bg-info text-white');
        }
    };

    const eksekusiPembayaranBackend<%=rnd%> = async (payload, uangTunai, kembalian) => {
        const btnBayar = document.getElementById('btnBayar<%=rnd%>');
        // Label awal tombol DIPATOK tetap (bukan ditangkap dari innerHTML saat ini), karena fungsi
        // ini bisa dipanggil saat tombol sudah berstatus "Menyimpan..." dari initiatePembayaran —
        // kalau ditangkap, finally akan mengembalikan ke "Menyimpan..." sehingga tombol macet.
        const oriBtnHtml = '<i class="fas fa-check-circle me-2"></i><%=Common.getBahasaConfig("Bayar")%>';
        btnBayar.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyimpan...")%>';
        btnBayar.disabled = true;

        // OFFLINE-FIRST: bila browser sedang offline, simpan ke antrian lokal + cetak struk; server
        // menerima saat online kembali (idempoten via kodeUnik). Jalur online di bawah tidak tersentuh.
        if (window.AisPosOffline && !navigator.onLine) {
            try {
                await AisPosOffline.submit(posTrxOffline<%=rnd%>(payload, uangTunai, kembalian));
                if (typeof tampilkanToast === "function") tampilkanToast('<%=Common.getBahasaConfigJS("Transaksi disimpan OFFLINE — otomatis dikirim ke server saat online kembali.")%>', 'bg-warning text-dark');
                else alert('<%=Common.getBahasaConfigJS("Transaksi disimpan OFFLINE.")%>');
                cetakStruk<%=rnd%>(payload, grandTotalValue<%=rnd%>, uangTunai, kembalian);
                tampilkanTransaksiSukses<%=rnd%>(payload, grandTotalValue<%=rnd%>);
                kirimKeLayarPelanggan<%=rnd%>({ tipe: 'sukses', totalBayar: grandTotalValue<%=rnd%>, uangTunai: uangTunai, kembalian: kembalian, namaToko: (isAdmin<%=rnd%> && document.getElementById('inputToko<%=rnd%>') && document.getElementById('inputToko<%=rnd%>').value) ? document.getElementById('inputToko<%=rnd%>').value : '<%=namaTokoAktif%>' });
                kosongkanKeranjang<%=rnd%>();
                document.getElementById('inputMember<%=rnd%>').value = '';
                document.getElementById('idMemberSelected<%=rnd%>').value = '';
                document.getElementById('minSaldoMemberSelected<%=rnd%>').value = '0';
                muatUlangPanelDashboard<%=rnd%>();
            } catch (e) {
                tampilkanPesanGagalFormal(
                    "penyimpanan transaksi POS secara luring (offline)",
                    "Rincian teknis: " + (e && e.message ? e.message : e),
                    ["Periksa apakah ruang penyimpanan lokal (local storage) peramban Bapak/Ibu masih tersedia/tidak penuh.", "Catat item transaksi ini secara manual sebagai cadangan, lalu coba ulangi transaksi.", "Bila kendala berulang, segera hubungi Administrator Sistem agar transaksi tidak hilang."]
                );
            } finally { btnBayar.innerHTML = oriBtnHtml; btnBayar.disabled = false; }
            return;
        }

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const result = await response.json();
            
            if(result.status === '00' || result.status === 'success'){
                if (typeof tampilkanToast === "function") {
            	    tampilkanToast('<%=Common.getBahasaConfigJS("Transaksi Berhasil Disimpan!")%>', 'bg-success text-white');
                } else {
                    alert('<%=Common.getBahasaConfigJS("Transaksi Berhasil Disimpan!")%>');
                }
                cetakStruk<%=rnd%>(payload, grandTotalValue<%=rnd%>, uangTunai, kembalian);
                tampilkanTransaksiSukses<%=rnd%>(payload, grandTotalValue<%=rnd%>);
                kirimKeLayarPelanggan<%=rnd%>({ tipe: 'sukses', totalBayar: grandTotalValue<%=rnd%>, uangTunai: uangTunai, kembalian: kembalian, namaToko: (isAdmin<%=rnd%> && document.getElementById('inputToko<%=rnd%>') && document.getElementById('inputToko<%=rnd%>').value) ? document.getElementById('inputToko<%=rnd%>').value : '<%=namaTokoAktif%>' });
                kosongkanKeranjang<%=rnd%>();
                document.getElementById('inputMember<%=rnd%>').value = ''; 
                document.getElementById('idMemberSelected<%=rnd%>').value = '';
                document.getElementById('minSaldoMemberSelected<%=rnd%>').value = '0';
            } else {
                if (typeof tampilkanToast === "function") {
            	    tampilkanToast(result.description || "Gagal menyimpan transaksi.", 'bg-danger text-white');
                } else {
                    alert(result.description || "Gagal menyimpan transaksi.");
                }
            }
            
        } catch (error) {
            console.error("Save Tx Error:", error);
            if (typeof tampilkanToast === "function") {
                tampilkanToast('<%=Common.getBahasaConfigJS("Koneksi terputus saat menyimpan.")%>', 'bg-danger text-white');
            } else {
                alert('<%=Common.getBahasaConfigJS("Koneksi terputus saat menyimpan.")%>');
            }
        } finally {
            btnBayar.innerHTML = oriBtnHtml;
            btnBayar.disabled = false;
        }
    };

    // ==========================================
    // INISIALISASI HALAMAN
    // ==========================================
    // ==========================================
    // PANEL: RINGKASAN INVENTORI, RIWAYAT MINI, ANALITIK PENJUALAN
    // (murni aditif/read-only -- tak menyentuh state cart/checkout sama sekali)
    // ==========================================
    let chartAnalitikLine<%=rnd%> = null, chartAnalitikDonut<%=rnd%> = null, chartAnalitikRadar<%=rnd%> = null;

    const scopeTokoAktif<%=rnd%> = () => (isAdmin<%=rnd%> && currentTokoId<%=rnd%> === "") ? null : currentTokoId<%=rnd%>;

    const loadRingkasanInventori<%=rnd%> = async () => {
        const idToko = scopeTokoAktif<%=rnd%>();
        const statRow = document.getElementById('invStatRow<%=rnd%>');
        const listEl = document.getElementById('invStokRendahList<%=rnd%>');
        if (idToko === null) {
            statRow.innerHTML = '<div class="col-12 text-center text-muted small py-3"><%=Common.getBahasaConfig("Pilih Toko/Pedagang dahulu.")%></div>';
            listEl.innerHTML = '';
            return;
        }
        const cond = " WHERE aktif = true AND toko = " + idToko + " ";
        const sqlStat = "SELECT COUNT(*) AS total, " +
            "SUM(CASE WHEN COALESCE(stok,0) > 0 AND COALESCE(stok,0) <= 5 THEN 1 ELSE 0 END) AS rendah, " +
            "SUM(CASE WHEN COALESCE(stok,0) <= 0 THEN 1 ELSE 0 END) AS habis, " +
            "COALESCE(SUM(COALESCE(stok,0) * COALESCE(hargabeli, hargajual, 0)), 0) AS nilai " +
            "FROM koperasi.produk" + cond + ";";
        const resStat = await fetchData<%=rnd%>(sqlStat);
        const s = (resStat && resStat[0]) ? resStat[0] : { total: 0, rendah: 0, habis: 0, nilai: 0 };

        const tile = (label, value, cls) => '<div class="col-6"><div class="border rounded-3 p-2 h-100"><div class="text-muted text-uppercase" style="font-size:9.5px;font-weight:700;">' + label + '</div><div class="fw-bold ' + cls + '" style="font-size:16px;">' + value + '</div></div></div>';
        statRow.innerHTML = tile('<%=Common.getBahasaConfigJS("Total Produk")%>', parseInt(s.total || 0), 'text-dark')
            + tile('<%=Common.getBahasaConfigJS("Stok Rendah")%>', parseInt(s.rendah || 0), 'text-warning')
            + tile('<%=Common.getBahasaConfigJS("Stok Habis")%>', parseInt(s.habis || 0), 'text-danger')
            + tile('<%=Common.getBahasaConfigJS("Nilai Stok")%>', formatRp<%=rnd%>(s.nilai || 0), 'text-success');

        const sqlList = "SELECT nama, COALESCE(stok,0) AS stok FROM koperasi.produk" + cond + " AND COALESCE(stok,0) <= 5 ORDER BY stok ASC LIMIT 4;";
        const resList = await fetchData<%=rnd%>(sqlList);
        if (!resList || resList.length === 0) {
            listEl.innerHTML = '<div class="text-center text-success small py-2"><i class="fas fa-check-circle me-1"></i><%=Common.getBahasaConfig("Semua stok aman.")%></div>';
        } else {
            let html = '';
            resList.forEach(r => {
                const stok = parseFloat(r.stok || 0);
                const badge = stok <= 0 ? '<span class="badge bg-danger">Stok ' + stok + '</span>' : '<span class="badge bg-warning text-dark">Stok ' + stok + '</span>';
                html += '<div class="d-flex justify-content-between align-items-center py-1 border-bottom small"><span class="text-truncate" style="max-width:180px;">' + r.nama + '</span>' + badge + '</div>';
            });
            listEl.innerHTML = html;
        }
    };

    const loadMiniRiwayat<%=rnd%> = async () => {
        const idToko = scopeTokoAktif<%=rnd%>();
        const body = document.getElementById('miniRiwayatBody<%=rnd%>');
        if (idToko === null) {
            body.innerHTML = '<tr><td colspan="4" class="text-center text-muted small py-3"><%=Common.getBahasaConfig("Pilih Toko/Pedagang dahulu.")%></td></tr>';
            return;
        }
        const sql = "SELECT COALESCE(a.pembelian_anggota_koperasi, a.id) AS id_transaksi, MAX(a.waktu) AS waktu, " +
            "MAX(a.member) AS member, MAX(a.carabayar) AS carabayar, SUM(a.total) AS total " +
            "FROM koperasi.pembelian a WHERE a.toko = " + idToko + " " +
            "GROUP BY COALESCE(a.pembelian_anggota_koperasi, a.id) ORDER BY MAX(a.waktu) DESC LIMIT 5;";
        const res = await fetchData<%=rnd%>(sql);
        if (!res || res.length === 0) {
            body.innerHTML = '<tr><td colspan="4" class="text-center text-muted small py-3"><%=Common.getBahasaConfig("Belum ada transaksi.")%></td></tr>';
            return;
        }
        let html = '';
        res.forEach(r => {
            html += '<tr><td class="small text-muted">' + (r.waktu || '-') + '</td>' +
                '<td class="small">' + (r.member || 'Walk-in Customer') + '</td>' +
                '<td class="text-end small fw-bold text-success">' + formatRp<%=rnd%>(r.total) + '</td>' +
                '<td class="text-center"><span class="badge bg-light text-dark border">' + (r.carabayar || '-') + '</span></td></tr>';
        });
        body.innerHTML = html;
    };

    const loadAnalitikPenjualan<%=rnd%> = async () => {
        const idToko = scopeTokoAktif<%=rnd%>();
        const statRow = document.getElementById('analitikStatRow<%=rnd%>');
        if (idToko === null) {
            statRow.innerHTML = '<div class="col-12 text-center text-muted small py-3"><%=Common.getBahasaConfig("Pilih Toko/Pedagang dahulu.")%></div>';
            if (chartAnalitikLine<%=rnd%>) { chartAnalitikLine<%=rnd%>.destroy(); chartAnalitikLine<%=rnd%> = null; }
            if (chartAnalitikDonut<%=rnd%>) { chartAnalitikDonut<%=rnd%>.destroy(); chartAnalitikDonut<%=rnd%> = null; }
            if (chartAnalitikRadar<%=rnd%>) { chartAnalitikRadar<%=rnd%>.destroy(); chartAnalitikRadar<%=rnd%> = null; }
            return;
        }
        const condToko = " AND toko = " + idToko + " ";
        const condTokoA = " AND a.toko = " + idToko + " ";

        // Statistik 7 hari terakhir vs 7 hari sebelumnya (dipakai jg utk radar "Perbandingan Kinerja")
        const sqlStat = "SELECT " +
            "COALESCE(SUM(CASE WHEN waktu >= CURRENT_DATE - INTERVAL '6 days' THEN total ELSE 0 END),0) AS total_now, " +
            "COALESCE(SUM(CASE WHEN waktu < CURRENT_DATE - INTERVAL '6 days' AND waktu >= CURRENT_DATE - INTERVAL '13 days' THEN total ELSE 0 END),0) AS total_prev, " +
            "COUNT(DISTINCT CASE WHEN waktu >= CURRENT_DATE - INTERVAL '6 days' THEN COALESCE(pembelian_anggota_koperasi, id) END) AS trx_now, " +
            "COUNT(DISTINCT CASE WHEN waktu < CURRENT_DATE - INTERVAL '6 days' AND waktu >= CURRENT_DATE - INTERVAL '13 days' THEN COALESCE(pembelian_anggota_koperasi, id) END) AS trx_prev, " +
            "COALESCE(SUM(CASE WHEN waktu >= CURRENT_DATE - INTERVAL '6 days' THEN qty ELSE 0 END),0) AS qty_now, " +
            "COALESCE(SUM(CASE WHEN waktu < CURRENT_DATE - INTERVAL '6 days' AND waktu >= CURRENT_DATE - INTERVAL '13 days' THEN qty ELSE 0 END),0) AS qty_prev " +
            "FROM koperasi.pembelian WHERE waktu >= CURRENT_DATE - INTERVAL '13 days'" + condToko + ";";
        const resStat = await fetchData<%=rnd%>(sqlStat);
        const st = (resStat && resStat[0]) ? resStat[0] : {};
        const totalNow = parseFloat(st.total_now || 0), totalPrev = parseFloat(st.total_prev || 0);
        const trxNow = parseInt(st.trx_now || 0), qtyNow = parseFloat(st.qty_now || 0);
        const trxPrev = parseInt(st.trx_prev || 0), qtyPrev = parseFloat(st.qty_prev || 0);
        const rata = trxNow > 0 ? totalNow / trxNow : 0;
        const rataPrev = trxPrev > 0 ? totalPrev / trxPrev : 0;
        const pct = totalPrev > 0 ? ((totalNow - totalPrev) / totalPrev * 100) : (totalNow > 0 ? 100 : 0);
        const pctTxt = (pct >= 0 ? '+' : '') + pct.toFixed(1) + '%';
        const pctCls = pct >= 0 ? 'text-success' : 'text-danger';

        const tileA = (label, value, delta) => '<div class="col-md-3 col-6"><div class="border rounded-3 p-3 h-100"><div class="text-muted text-uppercase" style="font-size:9.5px;font-weight:700;">' + label + '</div><div class="fw-bold text-dark" style="font-size:18px;">' + value + '</div>' + (delta || '') + '</div></div>';
        statRow.innerHTML = tileA('<%=Common.getBahasaConfigJS("Total Penjualan")%>', formatRp<%=rnd%>(totalNow), '<div class="small ' + pctCls + '">' + pctTxt + ' <%=Common.getBahasaConfig("vs 7 hari sebelumnya")%></div>')
            + tileA('<%=Common.getBahasaConfigJS("Transaksi")%>', trxNow, '')
            + tileA('<%=Common.getBahasaConfigJS("Rata-rata per Transaksi")%>', formatRp<%=rnd%>(rata), '')
            + tileA('<%=Common.getBahasaConfigJS("Item Terjual")%>', qtyNow, '');

        // Line chart: penjualan harian 7 hari
        const sqlLine = "SELECT TO_CHAR(DATE(waktu), 'DD Mon') AS label_periode, COALESCE(SUM(total),0) AS jumlah " +
            "FROM koperasi.pembelian WHERE waktu >= CURRENT_DATE - INTERVAL '6 days'" + condToko +
            " GROUP BY DATE(waktu) ORDER BY DATE(waktu) ASC;";
        const resLine = await fetchData<%=rnd%>(sqlLine);
        const labelsLine = resLine.map(r => r.label_periode);
        const dataLine = resLine.map(r => parseFloat(r.jumlah || 0));

        const ctxLine = document.getElementById('chartAnalitikLine<%=rnd%>').getContext('2d');
        if (chartAnalitikLine<%=rnd%>) chartAnalitikLine<%=rnd%>.destroy();
        let gradLine = ctxLine.createLinearGradient(0, 0, 0, 260);
        gradLine.addColorStop(0, 'rgba(13,110,253,.35)'); gradLine.addColorStop(1, 'rgba(13,110,253,0)');
        chartAnalitikLine<%=rnd%> = new Chart(ctxLine, {
            type: 'line',
            data: { labels: labelsLine, datasets: [{ label: '<%=Common.getBahasaConfigJS("Penjualan")%>', data: dataLine, borderColor: '#0d6efd', backgroundColor: gradLine, borderWidth: 3, fill: true, tension: .4, pointBackgroundColor: '#fff', pointBorderColor: '#0d6efd', pointBorderWidth: 2, pointRadius: 4 }] },
            options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { callback: (v) => formatRp<%=rnd%>(v) } } } }
        });

        // Donut chart: penjualan per kategori (jenis_produk)
        const sqlDonut = "SELECT COALESCE(jp.nama, '<%=Common.getBahasaConfigJS("Lainnya")%>') AS kategori, COALESCE(SUM(a.total),0) AS jumlah " +
            "FROM koperasi.pembelian a LEFT JOIN koperasi.produk p ON p.id = a.produk LEFT JOIN koperasi.jenis_produk jp ON jp.id = p.jenis_produk " +
            "WHERE a.waktu >= CURRENT_DATE - INTERVAL '6 days'" + condTokoA +
            " GROUP BY jp.nama ORDER BY jumlah DESC LIMIT 6;";
        const resDonut = await fetchData<%=rnd%>(sqlDonut);
        const labelsDonut = resDonut.map(r => r.kategori);
        const dataDonut = resDonut.map(r => parseFloat(r.jumlah || 0));
        const paletteDonut = ['#0d6efd', '#20c997', '#ffc107', '#fd7e14', '#6f42c1', '#dc3545'];

        const ctxDonut = document.getElementById('chartAnalitikDonut<%=rnd%>').getContext('2d');
        if (chartAnalitikDonut<%=rnd%>) chartAnalitikDonut<%=rnd%>.destroy();
        chartAnalitikDonut<%=rnd%> = new Chart(ctxDonut, {
            type: 'doughnut',
            data: { labels: labelsDonut, datasets: [{ data: dataDonut, backgroundColor: paletteDonut, borderWidth: 2, borderColor: '#fff' }] },
            options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom', labels: { boxWidth: 10, font: { size: 10.5 } } } } }
        });

        // Radar/spider "Perbandingan Kinerja": 4 ukuran minggu ini vs minggu lalu, dinormalisasi
        // 0-100 per sumbu (nilai mentah beda satuan -- Rupiah vs jumlah -- jadi dibuat sebanding
        // dgn membagi thd nilai terbesar di sumbu itu) supaya bentuk jaringnya bermakna dibaca.
        const skala = (now, prev) => { const m = Math.max(now, prev, 1); return [Math.round(now / m * 100), Math.round(prev / m * 100)]; };
        const [sTotalNow, sTotalPrev] = skala(totalNow, totalPrev);
        const [sTrxNow, sTrxPrev] = skala(trxNow, trxPrev);
        const [sRataNow, sRataPrev] = skala(rata, rataPrev);
        const [sQtyNow, sQtyPrev] = skala(qtyNow, qtyPrev);

        const ctxRadar = document.getElementById('chartAnalitikRadar<%=rnd%>').getContext('2d');
        if (chartAnalitikRadar<%=rnd%>) chartAnalitikRadar<%=rnd%>.destroy();
        chartAnalitikRadar<%=rnd%> = new Chart(ctxRadar, {
            type: 'radar',
            data: {
                labels: ['<%=Common.getBahasaConfigJS("Penjualan")%>', '<%=Common.getBahasaConfigJS("Transaksi")%>', '<%=Common.getBahasaConfigJS("Rata-rata")%>', '<%=Common.getBahasaConfigJS("Item Terjual")%>'],
                datasets: [
                    { label: '<%=Common.getBahasaConfigJS("Minggu Ini")%>', data: [sTotalNow, sTrxNow, sRataNow, sQtyNow], borderColor: '#0d6efd', backgroundColor: 'rgba(13,110,253,.2)', borderWidth: 2, pointRadius: 3 },
                    { label: '<%=Common.getBahasaConfigJS("Minggu Lalu")%>', data: [sTotalPrev, sTrxPrev, sRataPrev, sQtyPrev], borderColor: '#94a3b8', backgroundColor: 'rgba(148,163,184,.15)', borderWidth: 2, pointRadius: 3 }
                ]
            },
            options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom', labels: { boxWidth: 10, font: { size: 10.5 } } } }, scales: { r: { beginAtZero: true, max: 100, ticks: { display: false }, pointLabels: { font: { size: 10.5 } } } } }
        });
    };

    const muatUlangPanelDashboard<%=rnd%> = () => {
        loadRingkasanInventori<%=rnd%>();
        loadMiniRiwayat<%=rnd%>();
        loadAnalitikPenjualan<%=rnd%>();
    };

    document.addEventListener("DOMContentLoaded", async () => {
        injectModals<%=rnd%>();
        const btnLihatTabPesananBaru<%=rnd%> = document.getElementById('btnLihatTabPesananBaru<%=rnd%>');
        if (btnLihatTabPesananBaru<%=rnd%>) {
            btnLihatTabPesananBaru<%=rnd%>.addEventListener('click', () => {
                window.open('<%=Common.ROOT%>/baru?p=kantin%2Fpesanan&s=draft_pesanan_anggota', '_blank');
            });
        }
        mulaiPollingPesananBaru<%=rnd%>();
        const btnLihatRiwayatSinkron<%=rnd%> = document.getElementById('btnLihatRiwayatSinkron<%=rnd%>');
        if (btnLihatRiwayatSinkron<%=rnd%>) btnLihatRiwayatSinkron<%=rnd%>.addEventListener('click', bukaRiwayatSinkron<%=rnd%>);
        const btnSyncSekarang<%=rnd%> = document.getElementById('btnSyncSekarang<%=rnd%>');
        if (btnSyncSekarang<%=rnd%>) {
            btnSyncSekarang<%=rnd%>.addEventListener('click', async () => {
                if (!window.AisPosOffline) return;
                if (!AisPosOffline.isOnline()) {
                    if (typeof tampilkanToast === "function") tampilkanToast('<%=Common.getBahasaConfigJS("Tidak ada koneksi internet -- transaksi akan terkirim otomatis begitu koneksi kembali.")%>', 'bg-warning text-dark');
                    return;
                }
                const oriHtml<%=rnd%> = btnSyncSekarang<%=rnd%>.innerHTML;
                btnSyncSekarang<%=rnd%>.disabled = true;
                btnSyncSekarang<%=rnd%>.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyinkronkan...")%>';
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
                    btnSyncSekarang<%=rnd%>.disabled = false;
                    btnSyncSekarang<%=rnd%>.innerHTML = oriHtml<%=rnd%>;
                    muatStatusSinkron<%=rnd%>();
                }
            });
        }
        loadKategoriProduk<%=rnd%>();
        muatStatusSesiKas<%=rnd%>();
        await loadMasterData<%=rnd%>();
        // Aktifkan mesin POS Offline-First (cache produk + antrian transaksi + auto-sync online/offline).
        try {
            if (window.AisPosOffline) {
                var posSvc<%=rnd%> = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=kantin%2Fpos&s=pos_offline_service';
                AisPosOffline.init({ kasir: '<%= (tbmuser != null && tbmuser.getUserId() != null) ? tbmuser.getUserId() : "" %>', dataUrl: posSvc<%=rnd%>, syncUrl: posSvc<%=rnd%>, onStatus: posBadge<%=rnd%> });
                var _tkInit<%=rnd%> = document.getElementById('idTokoSelected<%=rnd%>');
                if (_tkInit<%=rnd%> && _tkInit<%=rnd%>.value) AisPosOffline.setSession({ tokoId: _tkInit<%=rnd%>.value });
            }
        } catch (e) { console.warn('POS offline init:', e); }
        await loadAturanDiskon<%=rnd%>();
        document.getElementById('searchProduk<%=rnd%>').addEventListener("keydown", (event) => {
            if (event.key === "Enter") {
                event.preventDefault();
                loadKatalogProduk<%=rnd%>();
            }
        });

        // Pintasan keyboard kasir (F2 fokus cari/scan, F12 bayar) -- pola umum aplikasi POS fisik,
        // mempercepat kasir yg terbiasa tanpa mouse. Diabaikan bila fokus ada di textarea/uang tunai
        // agar tak mengganggu pengetikan biasa.
        document.addEventListener('keydown', (event) => {
            if (event.key === 'F1') {
                event.preventDefault();
                const bantuan = document.getElementById('modalBantuanPOS<%=rnd%>');
                if (bantuan && window.bootstrap) bootstrap.Modal.getOrCreateInstance(bantuan).show();
            } else if (event.key === 'F2' || event.key === 'F3') {
                event.preventDefault();
                document.getElementById('searchProduk<%=rnd%>').focus();
            } else if (event.key === 'F12') {
                event.preventDefault();
                const btn = document.getElementById('btnBayar<%=rnd%>');
                if (btn && !btn.disabled) btn.click();
            }
        });
        
        loadKatalogProduk<%=rnd%>();
        muatUlangPanelDashboard<%=rnd%>();
        loadDaftarTertahan<%=rnd%>();
    });
</script>
