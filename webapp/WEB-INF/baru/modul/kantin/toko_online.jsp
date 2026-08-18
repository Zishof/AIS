<%@page import="org.hibernate.criterion.Criterion"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.model.koperasi.AnggotaKoperasi"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.common.Common"%>
<%
// ==========================================
// 1. IDENTIFIKASI PENGGUNA (PUBLIK / MEMBER)
// ==========================================
Tbmuser tbmuser = null;
AnggotaKoperasi curentAnggota = null;
boolean aktifkanPilihanMeja = false;
boolean aktifkan_integrasi_google = false;

try {
    tbmuser = Common.getCurrentUser(request);
    if (tbmuser != null) {
        curentAnggota = tbmuser.getAnggotaKoperasi();
    }
    
    // Mengambil Konfigurasi
    aktifkanPilihanMeja = Common.getKonfigurasi("aktifkan_pilihan_meja", Konfigurasi.AKTIF).getNilai().equalsIgnoreCase(Konfigurasi.AKTIF);
    aktifkan_integrasi_google = Common.getKonfigurasi("aktifkan_integrasi_google", Konfigurasi.AKTIF).getNilai().equalsIgnoreCase(Konfigurasi.AKTIF);

} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/toko_online.jsp:29");
} finally {
    HibernateUtil.closeSession();
}

boolean isLogin = (curentAnggota != null);
String rnd = Common.getGeneratedBarCode(7);
String idMember = isLogin ? String.valueOf(curentAnggota.getId()) : "";
String namaMember = isLogin ? curentAnggota.getNama() : "";
String kodeMember = isLogin ? curentAnggota.getKode() : "";

String idJenisAnggota = (isLogin && curentAnggota.getJenisAnggotaKoperasi() != null) ? String.valueOf(curentAnggota.getJenisAnggotaKoperasi().getId()) : "0";
String idTipeAnggota = (isLogin && curentAnggota.getTipeAnggotaKoperasi() != null) ? String.valueOf(curentAnggota.getTipeAnggotaKoperasi().getId()) : "0";

// Pengambilan Konfigurasi Istilah
String istilahSisaSaldo = (isLogin && curentAnggota.getJenisAnggotaKoperasi() != null) ? curentAnggota.getJenisAnggotaKoperasi().getIstilahSisaSaldo() : Common.getBahasaConfig("Saldo Kas");
String istilahCashback = (isLogin && curentAnggota.getJenisAnggotaKoperasi() != null) ? curentAnggota.getJenisAnggotaKoperasi().getIstilahCashback() : Common.getBahasaConfig("Cashback");

// URL Login & Integrasi Google
String linkLoginViaGoogle = Common.ROOT+"/google.zul"; 
%>

<!-- Library QR & Excel (Bila diperlukan) -->
<script data-cfasync="false" src="https://unpkg.com/html5-qrcode" type="text/javascript"></script>

<style>
    body { background-color: #f4f7f6; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; }
    
    /* Navbar Kustom */
    .navbar-marketplace-<%=rnd%> {
        background-color: #ffffff;
        box-shadow: 0 4px 12px rgba(0,0,0,0.05);
        position: sticky;
        top: 0;
        z-index: 1030;
    }
    
    /* Product Card E-Commerce */
    .product-card-<%=rnd%> { 
        transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out; 
        border-radius: 16px; 
        border: 1px solid #e2e8f0;
        background: #fff;
        height: 100%;
        display: flex;
        flex-direction: column;
    }
    .product-card-<%=rnd%>:hover { 
        transform: translateY(-5px); 
        box-shadow: 0 12px 20px rgba(0,0,0,0.08) !important; 
        border-color: #3b82f6;
    }
    .product-img-wrapper-<%=rnd%> {
        height: 160px;
        border-radius: 16px 16px 0 0;
        overflow: hidden;
        background-color: #f8fafc;
        display: flex;
        align-items: center;
        justify-content: center;
        position: relative;
    }
    .product-img-<%=rnd%> {
        width: 100%;
        height: 100%;
        object-fit: cover;
    }
    
    /* Floating Cart Badge */
    .cart-badge-<%=rnd%> {
        position: absolute;
        top: -5px;
        right: -5px;
        font-size: 0.65rem;
    }

    /* Modal autentikasi milik halaman ini (tanpa iframe). */
    .modal-auth-height {
        height: 80vh;
        min-height: 500px;
        overflow-y: auto;
    }

    /* Divider style for Google Login */
    .separator-content {
        display: flex;
        align-items: center;
        text-align: center;
    }
    .separator-content::before,
    .separator-content::after {
        content: '';
        flex: 1;
        border-bottom: 1px solid #e2e8f0;
    }
    .separator-content:not(:empty)::before { margin-right: .5em; }
    .separator-content:not(:empty)::after { margin-left: .5em; }

    @media (max-width: 768px) {
        .row.g-3 { --bs-gutter-x: 0.75rem; margin-left: 0; margin-right: 0; }
        .row.g-3 > [class*="col-"] { padding-left: 0.375rem; padding-right: 0.375rem; }
        .product-img-wrapper-<%=rnd%> { height: 130px; }
        .modal-auth-height { height: 90vh; }
    }
</style>

<!-- NAVBAR MARKETPLACE -->
<nav class="navbar navbar-expand-lg navbar-marketplace-<%=rnd%> py-3">
    <div class="container">
        <!-- Brand -->
        <a class="navbar-brand fw-bolder text-primary d-flex align-items-center gap-2" href="#">
            <i class="fas fa-shopping-bag fs-3"></i>
            <span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Koperasi")%> <span class="text-dark">Online</span></span>
        </a>

        <!-- Search Bar (Center) -->
        <div class="flex-grow-1 mx-3 mx-lg-5" style="max-width: 600px;">
            <div class="input-group shadow-sm rounded-pill overflow-hidden border">
                <span class="input-group-text bg-white border-0 text-muted ps-3"><i class="fas fa-search"></i></span>
                <input type="text" class="form-control border-0 py-2 shadow-none" id="searchKatalog<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari barang atau toko...")%>" style="font-size: 0.9rem;">
                <button class="btn btn-primary fw-bold px-4" type="button" onclick="currentPage<%=rnd%>=1; loadKatalogProduk<%=rnd%>();">
                    <span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Cari")%></span><i class="fas fa-search d-inline d-sm-none"></i>
                </button>
            </div>
        </div>

        <!-- Right Menu (Cart & User) -->
        <div class="d-flex align-items-center gap-3">
            <!-- Cart Icon -->
            <button class="btn btn-light position-relative rounded-circle shadow-sm border p-2" style="width: 42px; height: 42px;" type="button" data-bs-toggle="offcanvas" data-bs-target="#cartOffcanvas<%=rnd%>" aria-controls="cartOffcanvas<%=rnd%>">
                <i class="fas fa-shopping-cart text-primary"></i>
                <span class="position-absolute translate-middle badge rounded-pill bg-danger cart-badge-<%=rnd%>" id="headerCartCount<%=rnd%>">0</span>
            </button>

            <!-- User Menu -->
            <% if (isLogin) { %>
                <div class="dropdown">
                    <a href="#" class="d-flex align-items-center text-decoration-none dropdown-toggle" id="userMenu<%=rnd%>" data-bs-toggle="dropdown" aria-expanded="false">
                        <div class="bg-primary text-white rounded-circle d-flex justify-content-center align-items-center shadow-sm" style="width: 42px; height: 42px;">
                            <i class="fas fa-user"></i>
                        </div>
                        <div class="ms-2 d-none d-md-block text-dark">
                            <div class="fw-bold lh-1" style="font-size: 0.85rem;"><%=namaMember%></div>
                            <small class="text-muted" style="font-size: 0.7rem;"><%=istilahSisaSaldo%>: <span id="lblHeaderSaldo<%=rnd%>"><i class="fas fa-spinner fa-spin"></i></span></small>
                        </div>
                    </a>
                    <ul class="dropdown-menu dropdown-menu-end shadow border-0 mt-2 rounded-3" aria-labelledby="userMenu<%=rnd%>">
                        <li><a class="dropdown-item fw-medium py-2" href="<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=kantin%2Fmember&s=_beranda_anggota"><i class="fas fa-chart-pie me-2 text-primary"></i><%=Common.getBahasaConfig("Dashboard Anggota")%></a></li>
                        <li><hr class="dropdown-divider"></li>
                        <li><a class="dropdown-item fw-medium py-2 text-danger" href="javascript:void(0);" onclick="tampilkanPopupKeluar()"><i class="fas fa-sign-out-alt me-2"></i><%=Common.getBahasaConfig("Keluar")%></a></li>
                    </ul>
                </div>
            <% } else { %>
                <button class="btn btn-outline-primary rounded-pill fw-bold px-4 shadow-sm d-none d-md-inline-block" onclick="tampilkanModalLogin<%=rnd%>()"><%=Common.getBahasaConfig("Masuk")%></button>
                <button class="btn btn-primary rounded-pill fw-bold px-4 shadow-sm d-none d-md-inline-block" onclick="tampilkanModalDaftar<%=rnd%>()"><%=Common.getBahasaConfig("Daftar")%></button>
                
                <!-- Versi Mobile -->
                <button class="btn btn-outline-primary rounded-circle shadow-sm d-inline-block d-md-none p-2 me-1" style="width: 42px; height: 42px;" onclick="tampilkanModalLogin<%=rnd%>()" title="<%=Common.getBahasaConfig("Masuk")%>"><i class="fas fa-sign-in-alt"></i></button>
                <button class="btn btn-primary rounded-circle shadow-sm d-inline-block d-md-none p-2" style="width: 42px; height: 42px;" onclick="tampilkanModalDaftar<%=rnd%>()" title="<%=Common.getBahasaConfig("Daftar")%>"><i class="fas fa-user-plus"></i></button>
            <% } %>
        </div>
    </div>
</nav>

<!-- MAIN CONTENT -->
<div class="container py-4">
    
    <!-- Hero Banner -->
    <div class="card border-0 shadow-sm rounded-4 mb-4 overflow-hidden bg-primary text-white position-relative" style="background: var(--theme-gradient);">
        <div class="position-absolute top-0 end-0 opacity-25" style="transform: translate(10%, -20%);">
            <i class="fas fa-shopping-bag" style="font-size: 15rem;"></i>
        </div>
        <div class="card-body p-4 p-md-5 position-relative z-1 d-flex flex-column justify-content-center" style="min-height: 200px;">
            <h2 class="fw-bolder mb-2 display-6 shadow-sm d-inline-block" style="text-shadow: 2px 2px 4px rgba(0,0,0,0.3);"><%=Common.getBahasaConfig("Belanja Praktis, Harga Ekonomis")%></h2>
            <p class="fs-6 opacity-75 mb-0" style="max-width: 600px;"><%=Common.getBahasaConfig("Nikmati kemudahan berbelanja produk koperasi dan kantin secara daring. Semua pesanan terintegrasi langsung dengan saldo keanggotaan Anda.")%></p>
        </div>
    </div>

    <!-- Filter & Kategori -->
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h5 class="fw-bold text-dark mb-0"><i class="fas fa-box-open text-primary me-2"></i><%=Common.getBahasaConfig("Semua Produk")%></h5>
        
        <select class="form-select form-select-sm shadow-sm border-0 bg-white fw-bold text-primary w-auto rounded-pill px-3" id="selectTokoKatalog<%=rnd%>" onchange="handleTokoChange<%=rnd%>()">
            <option value=""><%=Common.getBahasaConfig("Semua Toko / Pedagang")%></option>
        </select>
    </div>

    <!-- Grid Produk -->
    <div class="row g-3" id="gridProduk<%=rnd%>">
        <!-- Indikator Loading -->
        <div class="col-12 text-center py-5">
            <div class="spinner-border text-primary" style="width: 3rem; height: 3rem;" role="status"></div>
            <div class="mt-3 fw-bold text-muted"><%=Common.getBahasaConfig("Menyiapkan etalase produk...")%></div>
        </div>
    </div>

    <!-- Paginasi -->
    <div class="d-flex justify-content-center mt-5 d-none" id="pagingContainer<%=rnd%>">
        <div class="bg-white p-2 rounded-pill shadow-sm border d-inline-flex align-items-center">
            <button class="btn btn-sm btn-light rounded-pill px-4 fw-bold text-primary" id="btnPrevPage<%=rnd%>" onclick="changePageKatalog<%=rnd%>(-1)">
                <i class="fas fa-chevron-left me-1"></i><%=Common.getBahasaConfig("Sebelumnya")%>
            </button>
            <div class="fw-bold text-dark px-4" style="font-size: 0.9rem;">
                <span id="lblCurrentPage<%=rnd%>">1</span> / <span id="lblTotalPage<%=rnd%>">1</span>
            </div>
            <button class="btn btn-sm btn-light rounded-pill px-4 fw-bold text-primary" id="btnNextPage<%=rnd%>" onclick="changePageKatalog<%=rnd%>(1)">
                <%=Common.getBahasaConfig("Selanjutnya")%><i class="fas fa-chevron-right ms-1"></i>
            </button>
        </div>
    </div>

</div>

<!-- OFFCANVAS KERANJANG BELANJA (CART SIDEBAR) -->
<div class="offcanvas offcanvas-end border-0 shadow-lg" tabindex="-1" id="cartOffcanvas<%=rnd%>" aria-labelledby="cartOffcanvasLabel<%=rnd%>" style="width: 400px; max-width: 100vw;">
    <div class="offcanvas-header bg-light border-bottom p-4">
        <h5 class="offcanvas-title fw-bold text-dark" id="cartOffcanvasLabel<%=rnd%>">
            <i class="fas fa-shopping-cart text-primary me-2"></i><%=Common.getBahasaConfig("Keranjang Saya")%>
        </h5>
        <button type="button" class="btn-close shadow-none" data-bs-dismiss="offcanvas" aria-label="Close"></button>
    </div>
    
    <div class="offcanvas-body p-0 d-flex flex-column bg-white">
        <!-- Area List Barang -->
        <div class="flex-grow-1 overflow-auto p-3" id="cartContainer<%=rnd%>">
            <div class="text-center text-muted py-5 mt-5">
                <i class="fas fa-cart-arrow-down fa-4x mb-3 opacity-25"></i>
                <br><h6 class="fw-bold"><%=Common.getBahasaConfig("Keranjang masih kosong")%></h6>
                <p class="small"><%=Common.getBahasaConfig("Silakan pilih produk dari etalase.")%></p>
            </div>
        </div>

        <!-- Area Checkout Summary -->
        <div class="p-4 bg-light border-top mt-auto">
            <div class="d-flex justify-content-between mb-2">
                <span class="text-secondary fw-semibold"><%=Common.getBahasaConfig("Subtotal")%></span>
                <span class="text-dark fw-bold" id="cartSubtotal<%=rnd%>">Rp 0</span>
            </div>
            <div class="d-flex justify-content-between mb-2">
                <span class="text-secondary fw-semibold"><%=Common.getBahasaConfig("Diskon Promo")%></span>
                <span class="text-danger fw-bold" id="cartDiskon<%=rnd%>">- Rp 0</span>
            </div>
            <div class="d-flex justify-content-between mb-3 border-bottom pb-3">
                <span class="text-secondary fw-semibold"><%=istilahCashback%></span>
                <span class="text-info fw-bold" id="cartCashback<%=rnd%>">+ Rp 0</span>
            </div>
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h5 class="fw-bolder text-dark mb-0"><%=Common.getBahasaConfig("Total Bayar")%></h5>
                <h4 class="fw-bolder text-success mb-0" id="cartGrandTotal<%=rnd%>">Rp 0</h4>
            </div>

            <button class="btn btn-primary btn-lg w-100 fw-bold rounded-pill shadow-sm" onclick="prosesCheckoutAwal<%=rnd%>()" id="btnProsesCheckoutAwal<%=rnd%>">
                <%=Common.getBahasaConfig("Lanjut Pembayaran")%> <i class="fas fa-arrow-right ms-1"></i>
            </button>
        </div>
    </div>
</div>

<script>
    // ==========================================
    // VARIABEL GLOBAL & STATE
    // ==========================================
    const isLogin<%=rnd%> = <%=isLogin%>;
    const idMemberAktif<%=rnd%> = '<%=idMember%>';
    let saldoMember<%=rnd%> = 0;
    let grandTotalValue<%=rnd%> = 0;
    
    let arrAturanDiskon<%=rnd%> = [];
    
    // State Paginasi Katalog Produk
    let currentPage<%=rnd%> = 1;
    const limitPerPage<%=rnd%> = 12; 
    let totalRecords<%=rnd%> = 0;
    
    // Instance Modals
    let offcanvasCartInstance<%=rnd%> = null;
    let modalCheckoutInstance<%=rnd%> = null;
    let modalSuksesInstance<%=rnd%> = null;
    let modalAuthInstance<%=rnd%> = null; 

    // --- MANAJEMEN KERANJANG VIA LOCAL STORAGE ---
    const CART_STORAGE_KEY = 'publicMarketplaceCart';
    let cart<%=rnd%> = [];

    const muatKeranjangDariStorage<%=rnd%> = () => {
        try {
            const stored = localStorage.getItem(CART_STORAGE_KEY);
            if (stored) {
                cart<%=rnd%> = JSON.parse(stored);
            }
        } catch (e) {
            console.error("Gagal membaca LocalStorage", e);
            cart<%=rnd%> = [];
        }
    };

    const simpanKeranjangKeStorage<%=rnd%> = () => {
        try {
            localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(cart<%=rnd%>));
        } catch (e) {
            console.error("Gagal menyimpan ke LocalStorage", e);
        }
    };

    const formatRp<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);
    };

    const getFormattedDate<%=rnd%> = () => {
        const now = new Date();
        const pad = (n) => n.toString().padStart(2, '0');
        return pad(now.getDate()) + '-' + pad(now.getMonth()+1) + '-' + now.getFullYear() + ' ' + pad(now.getHours()) + ':' + pad(now.getMinutes()) + ':' + pad(now.getSeconds());
    };

    const generateUniqueCode50 = () => {
        const timePart = new Date().getTime().toString();
        const randomPart = Array.from(window.crypto.getRandomValues(new Uint8Array(37)))
            .map(b => b.toString(36).padStart(2, '0')).join('').substring(0, 37).toUpperCase();
        return timePart + randomPart;
    };

    const fetchDataAPI<%=rnd%> = async (payload) => {
        try {
            payload.tanpaLogin = "true";
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            return await res.json();
        } catch (e) { 
            console.error(e); 
            return { status: "error", message: "<%=Common.getBahasaConfig("Terjadi kesalahan jaringan. Harap periksa koneksi internet Anda.")%>" }; 
        }
    };

    const showToastUI<%=rnd%> = (msg, colorClass) => {
        if(typeof tampilkanToast === "function") {
            tampilkanToast(msg, colorClass);
        } else {
            alert(msg);
        }
    };

    const injectModalsHTML<%=rnd%> = () => {
        // Modal Auth (Login & Daftar)
        const modalAuthHtml = `
        <div class="modal fade" id="modalAuth<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
            <div class="modal-dialog modal-dialog-centered modal-lg">
                <div class="modal-content rounded-4 border-0 shadow-lg">
                    <div class="modal-header bg-light border-bottom-0 pb-2 pt-3 px-4">
                        <h5 class="modal-title fw-bold text-dark" id="modalAuthTitle<%=rnd%>"><%=Common.getBahasaConfig("Autentikasi Pengguna")%></h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close" onclick="tutupModalAuth<%=rnd%>()"></button>
                    </div>
                    <div class="modal-body p-4 modal-auth-height bg-light">
                        <div id="authLoginPane<%=rnd%>">
                            <form id="formLoginMarketplace<%=rnd%>" onsubmit="event.preventDefault(); prosesLoginMarketplace<%=rnd%>();" class="mx-auto" style="max-width: 460px;">
                                <div class="text-center mb-4">
                                    <i class="fas fa-user-circle fa-4x text-primary mb-3"></i>
                                    <p class="text-muted mb-0"><%=Common.getBahasaConfig("Gunakan akun anggota Anda untuk melanjutkan.")%></p>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label fw-bold"><%=Common.getBahasaConfig("Nama Pengguna")%></label>
                                    <input class="form-control form-control-lg" id="authUsername<%=rnd%>" autocomplete="username" required>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label fw-bold"><%=Common.getBahasaConfig("Kata Sandi")%></label>
                                    <input type="password" class="form-control form-control-lg" id="authPassword<%=rnd%>" autocomplete="current-password" required>
                                </div>
                                <div id="authLoginMessage<%=rnd%>" class="alert alert-danger d-none"></div>
                                <button class="btn btn-primary btn-lg w-100 rounded-pill fw-bold" id="btnLoginMarketplace<%=rnd%>" type="submit">
                                    <i class="fas fa-sign-in-alt me-2"></i><%=Common.getBahasaConfig("Masuk")%>
                                </button>
                                <button class="btn btn-link w-100 mt-2" type="button" onclick="tampilkanModalDaftar<%=rnd%>()"><%=Common.getBahasaConfig("Belum punya akun? Daftar")%></button>
                            </form>
                        </div>
                        <div id="authRegisterPane<%=rnd%>" class="d-none">
                            <form id="formDaftarMarketplace<%=rnd%>" onsubmit="event.preventDefault(); prosesDaftarMarketplace<%=rnd%>();" class="mx-auto" style="max-width: 720px;">
                                <div class="row g-3">
                                    <div class="col-12"><div id="authRegisterMessage<%=rnd%>" class="alert d-none"></div></div>
                                    <div class="col-md-6"><label class="form-label fw-bold"><%=Common.getBahasaConfig("Nama Lengkap")%></label><input class="form-control" id="daftarNama<%=rnd%>" required></div>
                                    <div class="col-md-6"><label class="form-label fw-bold"><%=Common.getBahasaConfig("Nomor HP / WhatsApp")%></label><input class="form-control" id="daftarHp<%=rnd%>" required></div>
                                    <div class="col-md-6"><label class="form-label fw-bold"><%=Common.getBahasaConfig("Email")%></label><input type="email" class="form-control" id="daftarEmail<%=rnd%>" required></div>
                                    <div class="col-md-6"><label class="form-label fw-bold"><%=Common.getBahasaConfig("Jenis Keanggotaan")%></label><select class="form-select" id="daftarJenis<%=rnd%>" required><option value=""><%=Common.getBahasaConfig("Memuat data...")%></option></select></div>
                                    <div class="col-md-6"><label class="form-label fw-bold"><%=Common.getBahasaConfig("Nama Pengguna")%></label><input class="form-control" id="daftarUsername<%=rnd%>" pattern="[A-Za-z0-9._-]+" autocomplete="username" required><small class="text-muted"><%=Common.getBahasaConfig("Gunakan huruf, angka, titik, garis bawah, atau tanda hubung.")%></small></div>
                                    <div class="col-md-6"><label class="form-label fw-bold"><%=Common.getBahasaConfig("Kata Sandi")%></label><input type="password" minlength="6" class="form-control" id="daftarPassword<%=rnd%>" autocomplete="new-password" required></div>
                                    <div class="col-12 d-flex gap-2 justify-content-end mt-4">
                                        <button class="btn btn-light rounded-pill px-4" type="button" onclick="tampilkanModalLogin<%=rnd%>()"><%=Common.getBahasaConfig("Kembali ke Login")%></button>
                                        <button class="btn btn-success rounded-pill px-4 fw-bold" id="btnDaftarMarketplace<%=rnd%>" type="submit"><i class="fas fa-user-plus me-2"></i><%=Common.getBahasaConfig("Daftar")%></button>
                                    </div>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </div>`;

        // Modal Checkout
        const modalCheckoutHtml = `
        <div class="modal fade" id="modalCheckout<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content rounded-4 border-0 shadow-lg">
                    <div class="modal-header bg-primary text-white border-bottom-0 pb-3 pt-4 px-4">
                        <h5 class="modal-title fw-bold">
                            <i class="fas fa-cash-register me-2"></i><%=Common.getBahasaConfig("Selesaikan Pembayaran")%>
                        </h5>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    
                    <div class="modal-body p-4 bg-light">
                        
                        <div class="bg-white p-3 rounded-3 shadow-sm border mb-4 text-center">
                            <span class="text-muted small fw-bold text-uppercase d-block mb-1"><%=Common.getBahasaConfig("Total Tagihan Anda")%></span>
                            <h2 class="fw-bolder text-dark mb-0" id="lblModalTotalTagihan<%=rnd%>">Rp 0</h2>
                        </div>

                        <label class="form-label fw-bold text-secondary mb-2"><i class="fas fa-wallet me-2 text-primary"></i><%=Common.getBahasaConfig("Pilih Metode Pembayaran")%></label>
                        
                        <div class="form-check bg-white border rounded-3 p-3 mb-2 shadow-sm d-flex align-items-center" id="wrapperMetodeSaldo<%=rnd%>">
                            <input class="form-check-input ms-1 me-3 fs-5" type="radio" name="metodeBayar<%=rnd%>" id="radioBayarSaldo<%=rnd%>" value="SALDO">
                            <label class="form-check-label w-100 fw-bold text-dark d-flex justify-content-between align-items-center" for="radioBayarSaldo<%=rnd%>" style="cursor:pointer;">
                                <span>
                                    <%=istilahSisaSaldo%>
                                    <br><small class="text-success fw-bold" id="lblInfoSaldoModal<%=rnd%>">Tersedia: Rp 0</small>
                                </span>
                                <i class="fas fa-check-circle text-success d-none check-icon-metode"></i>
                            </label>
                        </div>

                        <div class="bg-white border rounded-3 p-3 shadow-sm">
                            <div class="form-check d-flex align-items-center mb-2">
                                <input class="form-check-input ms-1 me-3 fs-5" type="radio" name="metodeBayar<%=rnd%>" id="radioBayarOnline<%=rnd%>" value="ONLINE">
                                <label class="form-check-label w-100 fw-bold text-dark" for="radioBayarOnline<%=rnd%>" style="cursor:pointer;">
                                    <%=Common.getBahasaConfig("Pembayaran Online (VA / QRIS)")%>
                                </label>
                            </div>
                            <div class="ps-5 pe-2" id="wrapperDropdownOnline<%=rnd%>" style="opacity: 0.5; pointer-events: none;">
                                <select class="form-select form-select-sm fw-medium shadow-sm bg-light" id="selectMetodeOnline<%=rnd%>">
                                    <option value=""><%=Common.getBahasaConfig("Memuat saluran...")%></option>
                                </select>
                                <small class="text-danger fw-bold mt-1 d-none" id="errorMetodeOnline<%=rnd%>"><i class="fas fa-exclamation-circle me-1"></i><%=Common.getBahasaConfig("Silakan pilih bank/saluran")%></small>
                            </div>
                        </div>

                        <div class="mt-4">
                            <button class="btn btn-success w-100 py-3 fw-bold rounded-pill shadow-sm fs-6" onclick="eksekusiPembayaranFinal<%=rnd%>()" id="btnEksekusiBayar<%=rnd%>">
                                <i class="fas fa-lock me-2"></i><%=Common.getBahasaConfig("Bayar Sekarang")%>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>`;

        const modalSuksesHtml = `
        <div class="modal fade" id="modalSuksesTransaksi<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content rounded-4 border-0 shadow-lg">
                    <div class="modal-body p-5 text-center">
                        <i class="fas fa-check-circle fa-5x text-success mb-4 animate__animated animate__bounceIn"></i>
                        <h4 class="fw-bolder text-dark mb-2" id="titleSuksesTrx<%=rnd%>"><%=Common.getBahasaConfig("Pesanan Berhasil!")%></h4>
                        <p class="text-muted" id="descSuksesTrx<%=rnd%>"><%=Common.getBahasaConfig("Pembayaran telah berhasil. Pesanan Anda akan segera diproses.")%></p>
                        <button type="button" class="btn btn-primary rounded-pill px-5 mt-3 fw-bold shadow-sm" onclick="location.reload()">
                            <%=Common.getBahasaConfig("Selesai & Ke Beranda")%>
                        </button>
                    </div>
                </div>
            </div>
        </div>`;

        document.body.insertAdjacentHTML('beforeend', modalAuthHtml);
        document.body.insertAdjacentHTML('beforeend', modalCheckoutHtml);
        document.body.insertAdjacentHTML('beforeend', modalSuksesHtml);

        const elOffcanvas = document.getElementById('cartOffcanvas<%=rnd%>');
        if (elOffcanvas) offcanvasCartInstance<%=rnd%> = new bootstrap.Offcanvas(elOffcanvas);
        
        modalAuthInstance<%=rnd%> = new bootstrap.Modal(document.getElementById('modalAuth<%=rnd%>'));
        modalCheckoutInstance<%=rnd%> = new bootstrap.Modal(document.getElementById('modalCheckout<%=rnd%>'));
        modalSuksesInstance<%=rnd%> = new bootstrap.Modal(document.getElementById('modalSuksesTransaksi<%=rnd%>'));
    };

    // ==========================================
    // LOGIKA MODAL AUTH (LOGIN & DAFTAR)
    // ==========================================
    const tampilkanModalLogin<%=rnd%> = () => {
        if (!modalAuthInstance<%=rnd%>) return;
        document.getElementById('modalAuthTitle<%=rnd%>').innerHTML = '<i class="fas fa-sign-in-alt text-primary me-2"></i><%=Common.getBahasaConfig("Masuk Ke Sistem")%>';
        document.getElementById('authLoginPane<%=rnd%>').classList.remove('d-none');
        document.getElementById('authRegisterPane<%=rnd%>').classList.add('d-none');
        if(offcanvasCartInstance<%=rnd%>) offcanvasCartInstance<%=rnd%>.hide();
        modalAuthInstance<%=rnd%>.show();
        setTimeout(() => document.getElementById('authUsername<%=rnd%>').focus(), 250);
    };

    const tampilkanModalDaftar<%=rnd%> = async () => {
        if (!modalAuthInstance<%=rnd%>) return;
        document.getElementById('modalAuthTitle<%=rnd%>').innerHTML = '<i class="fas fa-user-plus text-success me-2"></i><%=Common.getBahasaConfig("Pendaftaran Anggota Baru")%>';
        document.getElementById('authLoginPane<%=rnd%>').classList.add('d-none');
        document.getElementById('authRegisterPane<%=rnd%>').classList.remove('d-none');
        if(offcanvasCartInstance<%=rnd%>) offcanvasCartInstance<%=rnd%>.hide();
        modalAuthInstance<%=rnd%>.show();
        await muatJenisDaftarMarketplace<%=rnd%>();
    };

    const tutupModalAuth<%=rnd%> = () => {
        const password = document.getElementById('authPassword<%=rnd%>');
        if (password) password.value = '';
    };

    const setAuthMessage<%=rnd%> = (id, message, success) => {
        const element = document.getElementById(id);
        element.textContent = message || '';
        element.className = 'alert ' + (success ? 'alert-success' : 'alert-danger');
        element.classList.toggle('d-none', !message);
    };

    const prosesLoginMarketplace<%=rnd%> = async () => {
        const button = document.getElementById('btnLoginMarketplace<%=rnd%>');
        const username = document.getElementById('authUsername<%=rnd%>').value.trim();
        const password = document.getElementById('authPassword<%=rnd%>').value;
        button.disabled = true;
        button.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Memproses...")%>';
        setAuthMessage<%=rnd%>('authLoginMessage<%=rnd%>', '', false);
        try {
            const payload = new URLSearchParams({ username: username, password: password });
            const response = await fetch('<%=Common.ROOT%>/login?action=ajax_login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
                credentials: 'include',
                body: payload.toString()
            });
            const result = await response.json();
            if (response.ok && result.status === 'success') {
                setAuthMessage<%=rnd%>('authLoginMessage<%=rnd%>', result.message || '<%=Common.getBahasaConfigJS("Login berhasil.")%>', true);
                window.setTimeout(() => window.location.reload(), 600);
                return;
            }
            setAuthMessage<%=rnd%>('authLoginMessage<%=rnd%>', result.message || '<%=Common.getBahasaConfigJS("Nama pengguna atau kata sandi tidak valid.")%>', false);
            document.getElementById('authPassword<%=rnd%>').focus();
        } catch (error) {
            console.error(error);
            setAuthMessage<%=rnd%>('authLoginMessage<%=rnd%>', '<%=Common.getBahasaConfigJS("Gagal terhubung ke peladen.")%>', false);
        } finally {
            button.disabled = false;
            button.innerHTML = '<i class="fas fa-sign-in-alt me-2"></i><%=Common.getBahasaConfig("Masuk")%>';
        }
    };

    let jenisDaftarSudahDimuat<%=rnd%> = false;
    const muatJenisDaftarMarketplace<%=rnd%> = async () => {
        if (jenisDaftarSudahDimuat<%=rnd%>) return;
        const result = await fetchDataAPI<%=rnd%>({
            action: 'sql',
            sql: 'SELECT id, nama, kode FROM koperasi.jenis_anggota_koperasi WHERE aktif = true AND dipilih = true ORDER BY nama ASC'
        });
        const select = document.getElementById('daftarJenis<%=rnd%>');
        select.innerHTML = '<option value=""><%=Common.getBahasaConfig("-- Pilih Jenis Keanggotaan --")%></option>';
        (result.data || []).forEach(item => {
            const option = document.createElement('option');
            option.value = item.id;
            option.dataset.kode = item.kode || 'MEM';
            option.textContent = item.nama;
            select.appendChild(option);
        });
        jenisDaftarSudahDimuat<%=rnd%> = true;
    };

    const prosesDaftarMarketplace<%=rnd%> = async () => {
        const button = document.getElementById('btnDaftarMarketplace<%=rnd%>');
        const username = document.getElementById('daftarUsername<%=rnd%>').value.trim();
        if (!/^[A-Za-z0-9._-]+$/.test(username)) {
            setAuthMessage<%=rnd%>('authRegisterMessage<%=rnd%>', '<%=Common.getBahasaConfigJS("Format nama pengguna tidak valid.")%>', false);
            return;
        }
        button.disabled = true;
        button.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Memproses...")%>';
        try {
            const cekUser = await fetchDataAPI<%=rnd%>({ action: 'sql', sql: "SELECT userid FROM public.tbmuser WHERE userid = '" + username + "' LIMIT 1" });
            if (cekUser.data && cekUser.data.length > 0) {
                setAuthMessage<%=rnd%>('authRegisterMessage<%=rnd%>', '<%=Common.getBahasaConfigJS("Nama pengguna sudah dipakai.")%>', false);
                return;
            }
            const jenis = document.getElementById('daftarJenis<%=rnd%>');
            const prefix = (jenis.options[jenis.selectedIndex].dataset.kode || 'MEM').toUpperCase().replace(/[^A-Z0-9_-]/g, '') || 'MEM';
            const now = new Date();
            const kode = prefix + '-' + now.getTime();
            const payload = {
                action: 'simpanDataRinci',
                class: '<%=AnggotaKoperasi.class.getName()%>',
                tanpaLogin: 'true',
                data: {
                    nama: document.getElementById('daftarNama<%=rnd%>').value.trim(),
                    hp: document.getElementById('daftarHp<%=rnd%>').value.trim(),
                    emailNasabah: document.getElementById('daftarEmail<%=rnd%>').value.trim(),
                    userid: username,
                    pass: document.getElementById('daftarPassword<%=rnd%>').value,
                    jenisAnggotaKoperasi: jenis.value,
                    kode: kode,
                    aktif: true
                }
            };
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify(payload)
            });
            const result = await response.json();
            if (response.ok && (result.status === '00' || result.status === 'success' || result.id)) {
                setAuthMessage<%=rnd%>('authRegisterMessage<%=rnd%>', '<%=Common.getBahasaConfigJS("Pendaftaran berhasil. Silakan masuk menggunakan akun baru Anda.")%>', true);
                document.getElementById('formDaftarMarketplace<%=rnd%>').reset();
                window.setTimeout(tampilkanModalLogin<%=rnd%>, 900);
                return;
            }
            setAuthMessage<%=rnd%>('authRegisterMessage<%=rnd%>', result.description || '<%=Common.getBahasaConfigJS("Pendaftaran gagal diproses.")%>', false);
        } catch (error) {
            console.error(error);
            setAuthMessage<%=rnd%>('authRegisterMessage<%=rnd%>', '<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi.")%>', false);
        } finally {
            button.disabled = false;
            button.innerHTML = '<i class="fas fa-user-plus me-2"></i><%=Common.getBahasaConfig("Daftar")%>';
        }
    };

    // ==========================================
    // PENGAMBILAN DATA AWAL (SALDO, DISKON, TOKO)
    // ==========================================
    const loadSaldoMember<%=rnd%> = async () => {
        if(!isLogin<%=rnd%>) return;
        const payload = { action: "tabungan", id_member: idMemberAktif<%=rnd%>, refresh: false };
        const res = await fetchDataAPI<%=rnd%>(payload);
        if (res && res.data !== undefined) {
            saldoMember<%=rnd%> = parseFloat(res.data);
            const elSaldo = document.getElementById('lblHeaderSaldo<%=rnd%>');
            if (elSaldo) elSaldo.innerText = formatRp<%=rnd%>(saldoMember<%=rnd%>);
            
            const elSaldoModal = document.getElementById('lblInfoSaldoModal<%=rnd%>');
            if (elSaldoModal) elSaldoModal.innerText = '<%=Common.getBahasaConfigJS("Tersedia:")%> ' + formatRp<%=rnd%>(saldoMember<%=rnd%>);
        }
    };

    const loadTokoKatalog<%=rnd%> = async () => {
        const sql = "SELECT id, nama FROM koperasi.toko WHERE aktif = true ORDER BY nama ASC";
        const res = await fetchDataAPI<%=rnd%>({action: "sql", sql: sql});
        let opt = '<option value=""><%=Common.getBahasaConfig("Semua Toko / Pedagang")%></option>';
        if (res.data) {
            res.data.forEach(t => { 
                opt += '<option value="' + t.id + '">' + t.nama + '</option>'; 
            });
        }
        document.getElementById('selectTokoKatalog<%=rnd%>').innerHTML = opt;
    };

    const loadMetodePembayaranOnline<%=rnd%> = async () => {
        const sql = "SELECT id, nama FROM koperasi.cara_pembayaran_koperasi WHERE aktif = true AND manual = false ORDER BY nama ASC";
        const res = await fetchDataAPI<%=rnd%>({ action: "sql", sql: sql });
        
        let optHtml = '<option value=""><%=Common.getBahasaConfig("-- Pilih Bank / Dompet Digital --")%></option>';
        
        if(res.data && res.data.length > 0) {
            res.data.forEach(item => { 
                optHtml += '<option value="' + item.id + '">' + item.nama + '</option>'; 
            });
        } else {
            optHtml = '<option value=""><%=Common.getBahasaConfig("-- Saluran Sedang Gangguan --")%></option>';
        }
        
        const selectCara = document.getElementById('selectMetodeOnline<%=rnd%>');
        if(selectCara) selectCara.innerHTML = optHtml;
    };

    const loadAturanDiskon<%=rnd%> = async () => {
        const sqlAturan = "SELECT id,produk,toko,berlaku_semua_member,jenis_anggota,tipe_anggota,persentase,maksimal_potongan,nominal,potongan_langsung,berlaku_per_hari_dan_per_toko,tanggal_mulai_iso,tanggal_selesai_iso FROM (" +
                "SELECT id,produk,toko,berlaku_semua_member,jenis_anggota,tipe_anggota,persentase,maksimal_potongan,nominal,potongan_langsung,berlaku_per_hari_dan_per_toko,TO_CHAR(tanggal_mulai, 'YYYY-MM-DD\"T\"HH24:MI:SS') tanggal_mulai_iso,TO_CHAR(tanggal_selesai, 'YYYY-MM-DD\"T\"HH24:MI:SS') tanggal_selesai_iso,CASE WHEN produk IS NULL THEN 2 ELSE 0 END prioritas FROM koperasi.aturan_diskon WHERE aktif=true " +
                "UNION ALL SELECT NULL::bigint,d.produk,g.toko,COALESCE(g.berlaku_semua_member,true),g.jenis_anggota,g.tipe_anggota,COALESCE(g.persentase,0),COALESCE(g.maksimal_potongan,0),COALESCE(g.nominal,0),COALESCE(g.potongan_langsung,true),false,TO_CHAR(g.tanggal_mulai, 'YYYY-MM-DD\"T\"HH24:MI:SS'),TO_CHAR(g.tanggal_selesai, 'YYYY-MM-DD\"T\"HH24:MI:SS'),1 FROM koperasi.grup_aturan_diskon g JOIN koperasi.grup_aturan_diskon_detail d ON d.grup_aturan_diskon=g.id WHERE g.aktif=true AND d.aktif=true) semua_aturan ORDER BY prioritas,id NULLS LAST";
        const res = await fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlAturan });
        if(res.data) {
            arrAturanDiskon<%=rnd%> = res.data;
        }
    };

    const updateUsageDiskonMember<%=rnd%> = async () => {
        if (!isLogin<%=rnd%> || !arrAturanDiskon<%=rnd%>) return;

        const idMember = idMemberAktif<%=rnd%>;
        const currentToko = document.getElementById('selectTokoKatalog<%=rnd%>').value; 

        for (let i = 0; i < arrAturanDiskon<%=rnd%>.length; i++) {
            let rule = arrAturanDiskon<%=rnd%>[i];
            if (rule.berlaku_per_hari_dan_per_toko === 'true' || rule.berlaku_per_hari_dan_per_toko === true || rule.berlaku_per_hari_dan_per_toko === 't') {
                if (idMember !== "" && currentToko !== "") {
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
                    rule.terpakai_hari_ini = (resUsage.data && resUsage.data.length > 0) ? parseFloat(resUsage.data[0].terpakai) : 0;
                } else {
                    rule.terpakai_hari_ini = 0;
                }
            } else {
                rule.terpakai_hari_ini = 0;
            }
            rule.terpakai_di_keranjang = 0;
        }
    };

    // ==========================================
    // LOGIKA KATALOG PRODUK
    // ==========================================
    const checkAndLoadImageMarket<%=rnd%> = async (pId, colorClass, iconClass) => {
        try {
            const reqObj = { 
                action: "file", 
                class: "ais.database.model.file.LampiranLain", 
                ref: pId.toString(), 
                jenis: "ais.database.model.inventory.Produk",
                refresh: "false",
                tanpaLogin: "true"
            };
            const dataResponse = await fetchDataAPI<%=rnd%>(reqObj);
            
            const wrapper = document.getElementById('prod-icon-wrapper-<%=rnd%>-' + pId);
            if (!wrapper) return;

            if(dataResponse.status === '00' && dataResponse.data && dataResponse.data.nama){ 
                if(dataResponse.data.nama !== 'administrator-icon_default.png') {
                    // Gambar Valid, timpa Icon menjadi Gambar Asli
                    const imgUrl = "<%=Common.ROOT%>/Data?action=file&class=<%=ais.database.model.file.LampiranLain.class.getName()%>&ref=" + pId + "&jenis=<%=java.net.URLEncoder.encode(ais.database.model.inventory.Produk.class.getName(), "UTF-8")%>&render=true&time=" + new Date().getTime(); 
                    wrapper.innerHTML = '<img src="' + imgUrl + '" class="product-img-<%=rnd%>" alt="<%=Common.getBahasaConfig("Gambar Produk")%>">';
                    return;
                }
            }
            
            // Fallback Ikon (Jika gambar tidak ada)
            wrapper.classList.add('bg-' + colorClass, 'bg-opacity-10', 'text-' + colorClass);
            wrapper.innerHTML = '<i class="fas ' + iconClass + ' fa-4x opacity-50"></i>';

        } catch (error) { 
            // Abaikan error jaringan untuk individual image load, biarkan fallback bekerja
        }
    };

    const handleTokoChange<%=rnd%> = async () => {
        currentPage<%=rnd%> = 1;
        loadKatalogProduk<%=rnd%>();
        if(isLogin<%=rnd%>) await updateUsageDiskonMember<%=rnd%>();
        recalculateCart<%=rnd%>();
    };

    const changePageKatalog<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if (direction === -1 && currentPage<%=rnd%> > 1) {
            currentPage<%=rnd%>--;
            loadKatalogProduk<%=rnd%>();
        } else if (direction === 1 && currentPage<%=rnd%> < totalPages) {
            currentPage<%=rnd%>++;
            loadKatalogProduk<%=rnd%>();
        }
    };

    const loadKatalogProduk<%=rnd%> = async () => {
        const grid = document.getElementById('gridProduk<%=rnd%>');
        const pagingContainer = document.getElementById('pagingContainer<%=rnd%>');
        if(!grid) return;

        const idToko = document.getElementById('selectTokoKatalog<%=rnd%>').value;
        const keyword = document.getElementById('searchKatalog<%=rnd%>').value.trim().replace(/'/g, "''");
        
        grid.innerHTML = '<div class="col-12 text-center py-5"><div class="spinner-border text-primary" style="width: 3rem; height: 3rem;"></div><div class="mt-3 fw-bold text-muted"><%=Common.getBahasaConfig("Sistem sedang memuat katalog...")%></div></div>';
        if(pagingContainer) pagingContainer.classList.add('d-none');

        let filterSql = "WHERE p.aktif = true ";
        if (idToko !== "") filterSql += " AND p.toko = " + idToko + " ";
        if (keyword !== "") filterSql += " AND (p.nama ILIKE '%" + keyword + "%' OR p.kode = '" + keyword + "' OR t.nama ILIKE '%" + keyword + "%') ";

        const sqlCount = "SELECT COUNT(p.id) as jumlah FROM koperasi.produk p LEFT JOIN koperasi.toko t ON p.toko = t.id " + filterSql;
        const offset = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%>;
        const sqlData = "SELECT p.id, p.kode, p.nama, p.hargajual, t.id as id_toko, t.nama as nama_toko " +
                        "FROM koperasi.produk p LEFT JOIN koperasi.toko t ON p.toko = t.id " +
                        filterSql + " ORDER BY p.nama ASC LIMIT " + limitPerPage<%=rnd%> + " OFFSET " + offset + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlCount }),
                fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlData })
            ]);

            totalRecords<%=rnd%> = (resCount.data && resCount.data[0]) ? parseInt(resCount.data[0].jumlah || 0) : 0;
            const produkList = resData.data || [];

            if (produkList.length === 0) {
                grid.innerHTML = '<div class="col-12 text-center py-5 text-muted"><i class="fas fa-box-open fa-4x mb-3 opacity-25"></i><br><h5 class="fw-bold"><%=Common.getBahasaConfig("Produk tidak ditemukan")%></h5><p class="small"><%=Common.getBahasaConfig("Coba gunakan kata kunci pencarian lain.")%></p></div>';
                return;
            }

            let htmlGrid = '';
            const icons = ['fa-box', 'fa-coffee', 'fa-hamburger', 'fa-ice-cream', 'fa-pizza-slice', 'fa-cookie'];
            const colors = ['primary', 'success', 'danger', 'warning', 'info', 'secondary'];

            produkList.forEach((p, idx) => {
                const icon = icons[idx % icons.length];
                const color = colors[idx % colors.length];
                
                const pId = p.id;
                const pKode = p.kode || '';
                const pNama = p.nama.replace(/'/g, "\\'").replace(/"/g, '&quot;');
                const pHarga = parseFloat(p.hargajual || 0);
                const pIdToko = p.id_toko;
                const nmToko = (p.nama_toko || '<%=Common.getBahasaConfigJS("Koperasi Utama")%>').replace(/'/g, "\\'");
                
                htmlGrid += '<div class="col-6 col-md-4 col-lg-3 col-xl-2">' +
                        '<div class="product-card-<%=rnd%>">' +
                            '<div id="prod-icon-wrapper-<%=rnd%>-' + pId + '" class="product-img-wrapper-<%=rnd%> p-0">' +
                                // Spinner sementara sebelum load gambar sesungguhnya/fallback icon
                                '<i class="fas fa-spinner fa-spin text-primary fs-3"></i>' +
                            '</div>' +
                            '<div class="p-3 d-flex flex-column flex-grow-1">' +
                                '<small class="text-muted fw-bold text-truncate mb-1" style="font-size: 10px;"><i class="fas fa-store text-primary me-1"></i>' + nmToko + '</small>' +
                                '<h6 class="fw-bolder text-dark mb-2" style="font-size: 0.9rem; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;" title="' + p.nama + '">' + p.nama + '</h6>' +
                                '<div class="text-success fw-bolder mb-3 mt-auto" style="font-size: 1rem;">' + formatRp<%=rnd%>(pHarga) + '</div>' +
                                '<button class="btn btn-outline-primary btn-sm w-100 fw-bold rounded-pill shadow-sm mt-auto" onclick="addToCart<%=rnd%>(' + pId + ', \'' + pKode + '\', \'' + pNama + '\', ' + pHarga + ', ' + pIdToko + ', \'' + nmToko + '\')">' +
                                    '<i class="fas fa-cart-plus me-1"></i><%=Common.getBahasaConfig("Tambah")%>' +
                                '</button>' +
                            '</div>' +
                        '</div>' +
                    '</div>';
            });
            
            grid.innerHTML = htmlGrid;

            // Trigger pencarian gambar untuk setiap produk
            produkList.forEach((p, idx) => {
                const icon = icons[idx % icons.length];
                const color = colors[idx % colors.length];
                checkAndLoadImageMarket<%=rnd%>(p.id, color, icon);
            });

            if (totalRecords<%=rnd%> > limitPerPage<%=rnd%>) {
                const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
                document.getElementById('lblCurrentPage<%=rnd%>').innerText = currentPage<%=rnd%>;
                document.getElementById('lblTotalPage<%=rnd%>').innerText = totalPages;
                document.getElementById('btnPrevPage<%=rnd%>').disabled = (currentPage<%=rnd%> === 1);
                document.getElementById('btnNextPage<%=rnd%>').disabled = (currentPage<%=rnd%> === totalPages);
                if(pagingContainer) pagingContainer.classList.remove('d-none');
            } else {
                if(pagingContainer) pagingContainer.classList.add('d-none');
            }

        } catch (error) {
            console.error("Gagal load katalog:", error);
            grid.innerHTML = '<div class="col-12 text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-3x mb-3"></i><br><h6 class="fw-bold"><%=Common.getBahasaConfig("Gagal menarik data katalog dari server.")%></h6></div>';
        }
    };

    // ==========================================
    // LOGIKA KERANJANG BELANJA (CART) & DISKON
    // ==========================================
    const evaluateDiscount<%=rnd%> = (item) => {
        // Diskon HANYA berlaku jika member sudah login
        if(!isLogin<%=rnd%>) {
            item.diskon = 0;
            item.cashback = 0;
            item.aturanDiskon = null;
            item.berlakuPerHariDanPerToko = false;
            return; 
        }
        
        let appliedRule = null;
        const now = new Date();
        const selectedMember = {
            jenis_anggota: '<%=idJenisAnggota%>',
            tipe_anggota: '<%=idTipeAnggota%>'
        };

        for (let rule of arrAturanDiskon<%=rnd%>) {
            const isSpecificProduct = (rule.produk !== null && rule.produk !== '' && rule.produk !== undefined);
            if ((isSpecificProduct == true || isSpecificProduct == 'true') && rule.produk != item.id) continue;
            
            const isSpecificToko = (rule.toko !== null && rule.toko !== '' && rule.toko !== undefined);
            if ((isSpecificToko == true || isSpecificToko == 'true') && rule.toko != item.idToko) continue;
            
            if (rule.tanggal_mulai_iso && rule.tanggal_mulai_iso !== '' && new Date(rule.tanggal_mulai_iso) > now) continue;
            if (rule.tanggal_selesai_iso && rule.tanggal_selesai_iso !== '') {
                const batasSelesai = new Date(rule.tanggal_selesai_iso);
                batasSelesai.setHours(23, 59, 59, 999);
                if (batasSelesai < now) continue;
            }
            
            // Evaluasi Hak Akses Member
            const isAllMember = (rule.berlaku_semua_member === 'true' || rule.berlaku_semua_member === true || rule.berlaku_semua_member === 't');
            if (!isAllMember) {
                if (!selectedMember) continue; 
                if (rule.jenis_anggota && rule.jenis_anggota !== '' && rule.jenis_anggota != selectedMember.jenis_anggota) continue;
                if (rule.tipe_anggota && rule.tipe_anggota !== '' && rule.tipe_anggota != selectedMember.tipe_anggota) continue;
            }

            if ((parseFloat(rule.persentase) || 0) <= 0 && (parseFloat(rule.nominal) || 0) <= 0) continue;

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
        if(arrAturanDiskon<%=rnd%>) {
            arrAturanDiskon<%=rnd%>.forEach(r => r.terpakai_di_keranjang = 0);
        }
        cart<%=rnd%>.forEach(item => {
            evaluateDiscount<%=rnd%>(item);
        });
        simpanKeranjangKeStorage<%=rnd%>(); // Simpan setiap ada perubahan
        renderCartUI<%=rnd%>();
    };

    const addToCart<%=rnd%> = async (id, kode, nama, harga, idToko, namaToko) => {
        const existingItem = cart<%=rnd%>.find(item => item.id === id);
        if (existingItem) {
            existingItem.jumlah += 1;
        } else {
            let newItem = { id: id, kode: kode, nama: nama, harga: harga, jumlah: 1, idToko: idToko, namaToko: namaToko, diskon: 0, cashback: 0, aturanDiskon: null, berlakuPerHariDanPerToko: false };
            cart<%=rnd%>.push(newItem);
        }
        
        recalculateCart<%=rnd%>();
        
        // Animasi Notifikasi Keranjang Bertambah
        const cartBtn = document.querySelector('.floating-cart-btn-<%=rnd%>');
        if(cartBtn) {
            cartBtn.classList.add('animate__animated', 'animate__rubberBand');
            setTimeout(() => cartBtn.classList.remove('animate__rubberBand'), 1000);
        }
        showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Berhasil memasukkan 1x ")%>' + nama + '<%=Common.getBahasaConfigJS(" ke keranjang.")%>', 'bg-success text-white');
    };

    const updateQty<%=rnd%> = (index, delta) => {
        cart<%=rnd%>[index].jumlah += delta;
        if (cart<%=rnd%>[index].jumlah <= 0) {
            cart<%=rnd%>.splice(index, 1);
        }
        recalculateCart<%=rnd%>();
    };

    const renderCartUI<%=rnd%> = () => {
        const container = document.getElementById('cartContainer<%=rnd%>');
        const badgeCount = document.getElementById('headerCartCount<%=rnd%>');
        const floatingBadge = document.getElementById('floatingCartCount<%=rnd%>');
        
        let subtotal = 0;
        let totalDiskon = 0;
        let totalCashback = 0;
        let totalItems = 0;

        if (cart<%=rnd%>.length === 0) {
            if(container) container.innerHTML = '<div class="text-center text-muted py-5 mt-5"><i class="fas fa-cart-arrow-down fa-4x mb-3 opacity-25"></i><br><h6 class="fw-bold"><%=Common.getBahasaConfig("Keranjang masih kosong")%></h6><p class="small"><%=Common.getBahasaConfig("Silakan pilih produk dari etalase.")%></p></div>';
            grandTotalValue<%=rnd%> = 0;
            if(badgeCount) badgeCount.innerText = "0";
            if(floatingBadge) floatingBadge.innerText = "0";
            document.getElementById('btnProsesCheckoutAwal<%=rnd%>').disabled = true;
        } else {
            document.getElementById('btnProsesCheckoutAwal<%=rnd%>').disabled = false;
            
            const cartByToko = cart<%=rnd%>.reduce((acc, item) => {
                if (!acc[item.idToko]) acc[item.idToko] = { namaToko: item.namaToko, items: [] };
                acc[item.idToko].items.push(item);
                return acc;
            }, {});

            let htmlCart = '';
            
            for (const tId in cartByToko) {
                const toko = cartByToko[tId];
                htmlCart += '<div class="bg-light px-3 py-2 fw-bold text-primary border-bottom border-top small"><i class="fas fa-store me-2"></i>' + toko.namaToko + '</div>';
                
                toko.items.forEach((item) => {
                    const idx = cart<%=rnd%>.findIndex(i => i.id === item.id); 
                    const itemTotal = (item.harga * item.jumlah) - item.diskon;
                    subtotal += (item.harga * item.jumlah);
                    totalDiskon += item.diskon;
                    totalCashback += item.cashback;
                    totalItems += item.jumlah;
                    
                    let notePromo = '';
                    if(item.diskon > 0) notePromo += '<span class="badge bg-danger text-white rounded-pill mt-1 px-2 py-1" style="font-size: 10px;"><i class="fas fa-cut me-1"></i><%=Common.getBahasaConfig("Potong")%> ' + formatRp<%=rnd%>(item.diskon) + '</span> ';
                    
                    // Hanya Tampilkan Cashback Jika Anggota
                    if(isLogin<%=rnd%> && item.cashback > 0) {
                        notePromo += '<span class="badge bg-info text-dark rounded-pill mt-1 px-2 py-1" style="font-size: 10px;"><i class="fas fa-wallet me-1"></i><%=istilahCashback%> ' + formatRp<%=rnd%>(item.cashback) + '</span>';
                    }

                    htmlCart += '<div class="d-flex align-items-center justify-content-between p-3 border-bottom">' +
                                    '<div class="flex-grow-1 pe-3">' +
                                        '<h6 class="fw-bold text-dark mb-1" style="font-size: 0.9rem;">' + item.nama + '</h6>' + 
                                        '<div class="text-success fw-bold small">' + formatRp<%=rnd%>(item.harga) + '</div>' +
                                        '<div>' + notePromo + '</div>' +
                                    '</div>' +
                                    '<div class="d-flex flex-column align-items-end gap-2">' +
                                        '<div class="fw-bolder text-dark">' + formatRp<%=rnd%>(itemTotal) + '</div>' +
                                        '<div class="input-group input-group-sm shadow-sm rounded-pill overflow-hidden border" style="width: 100px;">' +
                                            '<button class="btn btn-light text-primary border-0 px-2 fw-bold" type="button" onclick="updateQty<%=rnd%>(' + idx + ', -1)">-</button>' +
                                            '<input type="text" class="form-control text-center px-0 border-0 fw-bold bg-white" style="font-size: 0.8rem;" value="' + item.jumlah + '" readonly>' +
                                            '<button class="btn btn-light text-primary border-0 px-2 fw-bold" type="button" onclick="updateQty<%=rnd%>(' + idx + ', 1)">+</button>' +
                                        '</div>' +
                                    '</div>' +
                                '</div>';
                });
            }

            if(container) container.innerHTML = htmlCart;
            grandTotalValue<%=rnd%> = subtotal - totalDiskon;
            if(badgeCount) badgeCount.innerText = totalItems;
            if(floatingBadge) floatingBadge.innerText = totalItems;
        }

        const elSubtotal = document.getElementById('cartSubtotal<%=rnd%>');
        const elDiskon = document.getElementById('cartDiskon<%=rnd%>');
        const elCashback = document.getElementById('cartCashback<%=rnd%>');
        const elGrandTotal = document.getElementById('cartGrandTotal<%=rnd%>');

        if(elSubtotal) elSubtotal.innerText = formatRp<%=rnd%>(subtotal);
        if(elDiskon) elDiskon.innerText = "- " + formatRp<%=rnd%>(totalDiskon);
        
        // HANYA TAMPILKAN TOTAL CASHBACK JIKA MEMBER
        if(isLogin<%=rnd%>) {
            if(elCashback) elCashback.innerText = "+ " + formatRp<%=rnd%>(totalCashback);
        } else {
            // Jika pengunjung publik (belum login), hapus container potensi cashback agar tidak membingungkan
            const contCb = elCashback ? elCashback.parentElement : null;
            if (contCb) contCb.style.display = 'none';
        }

        if(elGrandTotal) elGrandTotal.innerText = formatRp<%=rnd%>(grandTotalValue<%=rnd%>);
    };


    // ==========================================
    // LOGIKA PEMBAYARAN (CHECKOUT)
    // ==========================================
    
    // Tahap 1: Membuka Checkout Modal (Wajib Login)
    const prosesCheckoutAwal<%=rnd%> = () => {
        if (!isLogin<%=rnd%>) {
            // Arahkan ke Modal Login/Daftar
            if (offcanvasCartInstance<%=rnd%>) offcanvasCartInstance<%=rnd%>.hide();
            tampilkanModalLogin<%=rnd%>();
            return;
        }

        if (cart<%=rnd%>.length === 0) return;

        // Tutup Sidebar Keranjang
        if (offcanvasCartInstance<%=rnd%>) offcanvasCartInstance<%=rnd%>.hide();

        // Update Informasi Modal
        document.getElementById('lblModalTotalTagihan<%=rnd%>').innerText = formatRp<%=rnd%>(grandTotalValue<%=rnd%>);

        // Atur Status Opsi Saldo
        const wrapSaldo = document.getElementById('wrapperMetodeSaldo<%=rnd%>');
        const radioSaldo = document.getElementById('radioBayarSaldo<%=rnd%>');
        
        if (saldoMember<%=rnd%> >= grandTotalValue<%=rnd%>) {
            wrapSaldo.classList.remove('opacity-50');
            radioSaldo.disabled = false;
            // Default pilih saldo jika cukup
            radioSaldo.checked = true;
            document.getElementById('wrapperDropdownOnline<%=rnd%>').style.opacity = '0.5';
            document.getElementById('wrapperDropdownOnline<%=rnd%>').style.pointerEvents = 'none';
            document.getElementById('selectMetodeOnline<%=rnd%>').required = false;
        } else {
            wrapSaldo.classList.add('opacity-50');
            radioSaldo.disabled = true;
            radioSaldo.checked = false;
            document.getElementById('lblInfoSaldoModal<%=rnd%>').innerHTML = '<i class="fas fa-exclamation-circle me-1"></i><%=Common.getBahasaConfig("Saldo Tidak Mencukupi")%>';
            document.getElementById('lblInfoSaldoModal<%=rnd%>').classList.replace('text-success', 'text-danger');
            
            // Paksa pilih online
            document.getElementById('radioBayarOnline<%=rnd%>').checked = true;
            document.getElementById('wrapperDropdownOnline<%=rnd%>').style.opacity = '1';
            document.getElementById('wrapperDropdownOnline<%=rnd%>').style.pointerEvents = 'auto';
        }

        // Buka Modal Checkout
        if (modalCheckoutInstance<%=rnd%>) modalCheckoutInstance<%=rnd%>.show();
    };

    // Event Listener Radio Button Pemilihan Metode di Modal
    document.querySelectorAll('input[name="metodeBayar<%=rnd%>"]').forEach(radio => {
        radio.addEventListener('change', function() {
            const wrapOnline = document.getElementById('wrapperDropdownOnline<%=rnd%>');
            const selectOnline = document.getElementById('selectMetodeOnline<%=rnd%>');
            const errorMsg = document.getElementById('errorMetodeOnline<%=rnd%>');
            
            if (this.value === 'ONLINE') {
                wrapOnline.style.opacity = '1';
                wrapOnline.style.pointerEvents = 'auto';
                selectOnline.required = true;
            } else {
                wrapOnline.style.opacity = '0.5';
                wrapOnline.style.pointerEvents = 'none';
                selectOnline.required = false;
                selectOnline.classList.remove('is-invalid');
                errorMsg.classList.add('d-none');
            }
        });
    });

    // Tahap 2: Eksekusi Final ke Backend
    const eksekusiPembayaranFinal<%=rnd%> = async () => {
        if (cart<%=rnd%>.length === 0) return;

        // Deteksi Pilihan Metode
        const metodeTerpilih = document.querySelector('input[name="metodeBayar<%=rnd%>"]:checked');
        if (!metodeTerpilih) {
            showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Silakan pilih salah satu metode pembayaran terlebih dahulu.")%>', 'bg-warning text-dark');
            return;
        }

        let idCaraBayar = null;
        let isOnline = false;

        if (metodeTerpilih.value === 'ONLINE') {
            isOnline = true;
            idCaraBayar = document.getElementById('selectMetodeOnline<%=rnd%>').value;
            
            if (!idCaraBayar) {
                document.getElementById('selectMetodeOnline<%=rnd%>').classList.add('is-invalid');
                document.getElementById('errorMetodeOnline<%=rnd%>').classList.remove('d-none');
                return;
            }
        } else {
            isOnline = false;
            if (saldoMember<%=rnd%> < grandTotalValue<%=rnd%>) {
                showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Sistem menolak, saldo Anda tidak mencukupi.")%>', 'bg-danger text-white');
                return;
            }
        }

        const btnBayar = document.getElementById('btnEksekusiBayar<%=rnd%>');
        const oriBtnHtml = btnBayar.innerHTML;
        btnBayar.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menghubungi Server...")%>';
        btnBayar.disabled = true;

        const curWaktu = getFormattedDate<%=rnd%>();

        // Mengelompokkan berdasarkan toko (Backend memproses per toko)
        const groupedCart = cart<%=rnd%>.reduce((acc, item) => {
            if (!acc[item.idToko]) acc[item.idToko] = [];
            acc[item.idToko].push(item);
            return acc;
        }, {});

        let isSemuaSukses = true;
        let pesanError = "";
        let paymentLink = null; // Menyimpan link jika metode online

        for (const tId in groupedCart) {
            const transaksiToko = groupedCart[tId];
            
            let subToko = 0;
            let discToko = 0;
            transaksiToko.forEach(ti => {
                subToko += (ti.harga * ti.jumlah);
                discToko += ti.diskon;
            });
            const totalBayarTokoIni = subToko - discToko;
            
            if (totalBayarTokoIni < 0) continue; 

            const kodeTransaksi = "MKT-" + generateUniqueCode50();

            // Jika Online = "draft_bayar" (bikin draft lalu bayar online), Jika Saldo = "bayar" (langsung potong)
            const targetAction = isOnline ? "draft_bayar" : "bayar";

            const payload = {
                action: targetAction,
                kodeUnik: kodeTransaksi,
                idToko: tId, 
                waktu: curWaktu,
                id_member: idMemberAktif<%=rnd%>,
                kanalCheckout: "anggota_online",
                transaksi: transaksiToko.map(item => ({
                    id: item.id,
                    kode: item.kode,
                    nama: item.nama,
                    harga: item.harga,
                    jumlah: item.jumlah,
                    diskon: item.diskon,
                    aturanDiskon: item.aturanDiskon, 
                    cashback: item.cashback,
                    berlakuPerHariDanPerToko: item.berlakuPerHariDanPerToko
                }))
            };

            if (isOnline) {
                payload.caraBayar = idCaraBayar;
            }

            try {
                const res = await fetchDataAPI<%=rnd%>(payload);
                if (res.status === '00' || res.status === 'success') {
                    if (isOnline) {
                        if (res.data && Array.isArray(res.data) && res.data.length > 0) {
                            paymentLink = res.data[0].link;
                        } else if (res.url) {
                            paymentLink = res.url;
                        }
                    }
                } else {
                    isSemuaSukses = false;
                    pesanError = res.description || "<%=Common.getBahasaConfig("Gagal memproses pembayaran untuk toko ")%>" + tId;
                    break; 
                }
            } catch (error) {
                console.error("Payment Error:", error);
                isSemuaSukses = false;
                pesanError = "<%=Common.getBahasaConfig("Koneksi jaringan terputus saat memproses transaksi.")%>";
                break;
            }
        }

        if (isSemuaSukses) {
            // BERSIHKAN CART LOKAL
            cart<%=rnd%> = [];
            localStorage.removeItem(CART_STORAGE_KEY);
            renderCartUI<%=rnd%>();

            if (modalCheckoutInstance<%=rnd%>) modalCheckoutInstance<%=rnd%>.hide();

            if (isOnline) {
                if (paymentLink) {
                    window.location.href = paymentLink;
                } else {
                    document.getElementById('titleSuksesTrx<%=rnd%>').innerText = '<%=Common.getBahasaConfigJS("Tagihan Dibuat")%>';
                    document.getElementById('descSuksesTrx<%=rnd%>').innerText = '<%=Common.getBahasaConfigJS("Pesanan Anda berhasil dicatat. Silakan cek menu Riwayat (Nomor VA) untuk menyelesaikan pembayaran.")%>';
                    if(modalSuksesInstance<%=rnd%>) modalSuksesInstance<%=rnd%>.show();
                }
            } else {
                if(modalSuksesInstance<%=rnd%>) modalSuksesInstance<%=rnd%>.show();
            }

        } else {
            showToastUI<%=rnd%>(pesanError, 'bg-danger text-white');
        }

        btnBayar.innerHTML = oriBtnHtml;
        btnBayar.disabled = false;
    };


    // ==========================================
    // INISIALISASI HALAMAN UTAMA
    // ==========================================
    document.addEventListener("DOMContentLoaded", async () => {
        injectModalsHTML<%=rnd%>();
        
        // Load data cart dari memori (jika user baru saja login ulang)
        muatKeranjangDariStorage<%=rnd%>();
        
        await loadTokoKatalog<%=rnd%>(); 
        await loadMetodePembayaranOnline<%=rnd%>();
        await loadAturanDiskon<%=rnd%>();
        
        const searchBox = document.getElementById('searchKatalog<%=rnd%>');
        if(searchBox) {
            searchBox.addEventListener("keydown", (event) => {
                if (event.key === "Enter") {
                    event.preventDefault();
                    currentPage<%=rnd%> = 1; 
                    loadKatalogProduk<%=rnd%>();
                }
            });
        }
        
        loadKatalogProduk<%=rnd%>();
        
        if (isLogin<%=rnd%>) {
            await loadSaldoMember<%=rnd%>();
            if (cart<%=rnd%>.length > 0) {
                // Rekalkulasi diskon karena status user sekarang sudah teridentifikasi (Login)
                recalculateCart<%=rnd%>();
            }
        } else {
            renderCartUI<%=rnd%>();
        }
    });
</script>
