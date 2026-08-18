<%@page import="org.hibernate.criterion.Criterion"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.model.koperasi.AnggotaKoperasi"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.common.Common"%>
<%
// ==========================================
// 1. Pengecekan Sesi & Keamanan Akses
// ==========================================
Tbmuser tbmuser = null;
AnggotaKoperasi curentAnggota = null;
boolean aktifkanPilihanMeja = false;
boolean aktifkanBayarViaQrTopup = false;
boolean aktifkanTopupDiAnggota = false;
try {
    tbmuser = Common.getCurrentUser(request);
    curentAnggota = tbmuser == null ? null : tbmuser.getAnggotaKoperasi();
    
    if(curentAnggota == null) {
        out.print("<div class='text-center p-5 animate__animated animate__fadeIn'><i class='fas fa-exclamation-triangle fa-3x text-warning mb-3'></i><br><h5 class='text-dark fw-bold'>" + Common.getBahasaConfig("Akses Ditolak") + "</h5><p class='text-muted'>" + Common.getBahasaConfig("Halaman ini hanya dapat diakses oleh Anggota Koperasi yang sah.") + "</p></div>");
        return;
    }
    if(tbmuser != null && tbmuser.getPedagang() != null) {
        out.print("<div class='text-center p-5 animate__animated animate__fadeIn'><i class='fas fa-user-lock fa-3x text-danger mb-3'></i><br><h5 class='text-dark fw-bold'>" + Common.getBahasaConfig("Akses Ditolak") + "</h5><p class='text-muted'>" + Common.getBahasaConfig("Halaman ini tidak boleh diakses oleh akun mitra/pedagang.") + "</p></div>");
        return;
    }

    // Mengambil Konfigurasi
    aktifkanPilihanMeja = Common.getKonfigurasi("aktifkan_pilihan_meja", Konfigurasi.AKTIF).getNilai().equalsIgnoreCase(Konfigurasi.AKTIF);
    aktifkanBayarViaQrTopup = Common.getKonfigurasi("aktifkan_bayar_via_qr_topup", Konfigurasi.AKTIF).getNilai().equalsIgnoreCase(Konfigurasi.AKTIF);
    aktifkanTopupDiAnggota = Common.getKonfigurasi("aktifkan_topup_di_anggota", Konfigurasi.AKTIF).getNilai().equalsIgnoreCase(Konfigurasi.AKTIF);
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/_beranda_anggota.jsp:36");
} finally {
}

boolean isLogin = (curentAnggota != null);
String istilahSisaSaldo = curentAnggota == null || curentAnggota.getJenisAnggotaKoperasi() == null ? Common.getBahasaConfig("Saldo Kas") : curentAnggota.getJenisAnggotaKoperasi().getIstilahSisaSaldo();
String istilahCashback = curentAnggota == null || curentAnggota.getJenisAnggotaKoperasi() == null ? Common.getBahasaConfig("Cashback") : curentAnggota.getJenisAnggotaKoperasi().getIstilahCashback();

String rnd = Common.getGeneratedBarCode(7);
// Tanggal hari ini (format Indonesia) untuk ditampilkan di atas keranjang belanja
String tanggalHariIniDisplay = new java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", new java.util.Locale("id","ID")).format(new java.util.Date());
String idMember = isLogin ? String.valueOf(curentAnggota.getId()) : "";
String namaMember = isLogin ? curentAnggota.getNama() : "";
String kodeMember = isLogin ? curentAnggota.getKode() : "";

// Variabel untuk menyimpan ID jenis anggota yang aktif agar bisa dibaca SQL
String idJenisAnggota = (curentAnggota != null && curentAnggota.getJenisAnggotaKoperasi() != null) ? String.valueOf(curentAnggota.getJenisAnggotaKoperasi().getId()) : "0";
String idTipeAnggota = (curentAnggota != null && curentAnggota.getTipeAnggotaKoperasi() != null) ? String.valueOf(curentAnggota.getTipeAnggotaKoperasi().getId()) : "0";

boolean aktifkanTampilCashback = curentAnggota != null && curentAnggota.getJenisAnggotaKoperasi() != null && curentAnggota.getJenisAnggotaKoperasi().getTampilkanCashback();
boolean aktifkanTampilSisaSaldo = curentAnggota != null && curentAnggota.getJenisAnggotaKoperasi() != null && curentAnggota.getJenisAnggotaKoperasi().getTampilkanSisaSaldo();

// Perbaikan logika: jika null, kembalikan 0.0, jika tidak null ambil minimal saldo.
Double minimalDeposit = curentAnggota == null || curentAnggota.getJenisAnggotaKoperasi() == null ? 0.0 : curentAnggota.getJenisAnggotaKoperasi().getMinimalSaldo();

String urlTopupService = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Fmember&s=_topup_service";
String channelTopup = Common.getKonfigurasi("cannel_va_e_smartlink", "VA_BNI:2500:BNI;VA_BRI:2500:BRI;VA_BCA:3500:BCA;VA_BNC:3500:BNC(Bank Neo Commerce);VA_CIMB:2500:CIMB Niaga;VA_MANDIRI:3500:Bank Mandiri;VA_PERMATA:2500:Bank Permata;VA_BSI:3000:BSI;VA_DANAMON:3000:Danamon;OTC_ALFAMART:3000:Alfamart;OTC_INDOMARET:3000:Indomart").getNilai();
%>

<!-- Library untuk membaca QR Code via Kamera (HTML5-QRCode) -->
<script data-cfasync="false" src="https://unpkg.com/html5-qrcode" type="text/javascript"></script>

<style>
    /* Kustomisasi UI agar Eye-Catching & Mobile Friendly */
    body { background-color: #f4f7f6; }
    
    .product-card-<%=rnd%> { 
        transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out; 
        border-radius: 12px; 
        border: 1px solid #e9ecef;
    }
    .product-card-<%=rnd%>:hover { 
        transform: translateY(-3px); 
        box-shadow: 0 8px 15px rgba(0,0,0,0.08) !important; 
    }
    
    /* Styling khusus scanner */
    #reader_<%=rnd%>, #reader_bayar_<%=rnd%> {
        width: 100%;
        border-radius: 12px;
        overflow: hidden;
        border: none !important;
    }
    #reader_<%=rnd%> video, #reader_bayar_<%=rnd%> video {
        border-radius: 12px;
        object-fit: cover;
    }

    /* Kustomisasi Header Pill */
    .header-pill-group-<%=rnd%> {
        background-color: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 50px;
        padding: 6px;
        display: flex;
        align-items: center;
        box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
        width: 100%;
    }
    
    /* Menghilangkan panah atas-bawah (spinners) pada input number di keranjang */
    .no-spinners::-webkit-outer-spin-button,
    .no-spinners::-webkit-inner-spin-button {
        -webkit-appearance: none;
        margin: 0;
    }
    .no-spinners[type=number] {
        -moz-appearance: textfield;
    }
    
    /* Penyesuaian Jarak Penuh pada Mobile agar konten membesar ke tepi */
    @media (max-width: 768px) {
        .mobile-stretch-<%=rnd%> {
            padding-left: 0 !important;
            padding-right: 0 !important;
            overflow-x: hidden;
        }
        .header-pill-group-<%=rnd%> {
            border-radius: 16px;
            flex-wrap: wrap;
            padding: 8px;
            gap: 6px;
            justify-content: space-between;
        }
        .header-pill-group-<%=rnd%> > div, 
        .header-pill-group-<%=rnd%> > button {
            flex-grow: 1; /* Memaksa elemen meregang penuh ke samping */
            justify-content: center;
            margin: 0 !important;
        }
        .profile-box-<%=rnd%> {
            max-width: 100% !important;
            border-right: none !important; 
            padding-bottom: 4px;
            border-bottom: 1px solid #f1f5f9;
        }
        .profile-text-<%=rnd%> {
            max-width: 100% !important;
        }
        
        /* HACK: Menghilangkan padding dari elemen parent (Tab Content & Card Body) agar full width */
        .tab-content .card-body {
            padding-left: 0 !important;
            padding-right: 0 !important;
        }
        .tab-content .card {
            border-radius: 0 !important;
        }
        
        /* Mengurangi jarak antar produk agar katalog terlihat lebih besar & muat */
        .row.g-3 {
            --bs-gutter-x: 0.5rem;
            margin-left: 0;
            margin-right: 0;
        }
        .row.g-3 > [class*="col-"] {
            padding-left: 0.05rem;
            padding-right: 0.05rem;
        }
    }

    /* Keranjang Belanja Sticky khusus Desktop */
    .cart-sticky-<%=rnd%> {
        z-index: 10;
    }
    @media (min-width: 992px) {
        .cart-sticky-<%=rnd%> {
            position: sticky;
            top: 80px; /* Menghindari agar tidak tertutup navbar */
        }
    }
</style>

<div class="container-fluid px-0 px-md-3 py-3 py-md-4 pb-5 mobile-stretch-<%=rnd%> animate__animated animate__fadeIn" style="background-color: #f4f7f6; min-height: 100vh; padding-bottom: 40px !important;">

    <!-- HEADER / INFO PENGGUNA -->
    <div class="d-flex flex-column flex-lg-row justify-content-between align-items-center mb-3 mb-md-4 gap-3">
        
        <div class="w-100 d-flex justify-content-center justify-content-lg-end">
            <% if (isLogin) { %>
                <div class="header-pill-group-<%=rnd%>">
                    
                    <!-- Kotak Profil -->
                    <div class="d-flex align-items-center pe-3 border-end border-light">
                        <div class="bg-primary text-white rounded-circle d-flex justify-content-center align-items-center me-2 flex-shrink-0" style="width: 36px; height: 36px;">
                            <i class="fas fa-user small"></i>
                        </div>
                        <div class="text-start text-truncate" style="max-width: 130px;">
                            <div class="fw-bold text-dark lh-1 text-truncate" style="font-size: 0.85rem;" title="<%=namaMember%>"><%=namaMember%></div>
                            <small class="text-muted" style="font-size: 0.65rem;"><%=kodeMember%></small>
                        </div>
                    </div>
                    
                    <%
					if(aktifkanTampilSisaSaldo){
					%>
                    <!-- Indikator Saldo Kas -->
                    <div class="d-flex align-items-center bg-success bg-opacity-10 text-success px-3 py-1 rounded-pill border border-success border-opacity-25 mx-1 my-1 my-lg-0">
                        <div class="text-end me-2">
                            <small class="fw-bold d-block lh-1" style="font-size: 0.65rem;"><%=istilahSisaSaldo%></small>
                            <span class="fw-bolder" style="font-size: 0.85rem;" id="labelSaldoUtama<%=rnd%>">Rp 0</span>
                        </div>
                        <button class="btn btn-sm btn-success rounded-circle p-1 d-flex align-items-center justify-content-center shadow-sm flex-shrink-0" style="width: 24px; height: 24px;" onclick="refreshSaldoTerkini<%=rnd%>(this)" id="btnRefreshSaldo<%=rnd%>" title="<%=Common.getBahasaConfig("Perbarui Saldo")%>">
                            <i class="fas fa-sync-alt" style="font-size: 0.6rem;"></i>
                        </button>
                    </div>
                    <%
					}
                    %>
					
					<%
					if(aktifkanTampilCashback){
					%>
                    <!-- Indikator Sisa Cashback -->
                    <div class="d-flex align-items-center bg-info bg-opacity-10 text-primary px-3 py-1 rounded-pill border border-info border-opacity-25 mx-1 my-1 my-lg-0">
                        <div class="text-end me-2">
                            <small class="fw-bold d-block lh-1 text-muted" style="font-size: 0.65rem;"><%=istilahCashback%></small>
                            <span class="fw-bolder text-info text-darken-3" style="font-size: 0.85rem;" id="labelSisaCashback<%=rnd%>">Rp 0</span>
                        </div>
                        <button class="btn btn-sm btn-info text-white rounded-circle p-1 d-flex align-items-center justify-content-center shadow-sm flex-shrink-0" style="width: 24px; height: 24px;" onclick="refreshSisaCashback<%=rnd%>(this)" id="btnRefreshCashback<%=rnd%>" title="<%=Common.getBahasaConfig("Perbarui")%> <%=istilahCashback%>">
                            <i class="fas fa-sync-alt" style="font-size: 0.6rem;"></i>
                        </button>
                    </div>
                    <%
					}
                    %>

					<%
					if(aktifkanBayarViaQrTopup){
					%>
                    <!-- Tombol QR Bayar -->
                    <button class="btn btn-dark text-white fw-bold rounded-pill shadow-sm px-3 mx-1 my-1 my-lg-0 d-flex align-items-center justify-content-center" onclick="bukaModalScannerBayar<%=rnd%>()" style="font-size: 0.85rem; height: 36px;">
                        <i class="fas fa-qrcode me-2"></i><%=Common.getBahasaConfig("Bayar QR")%>
                    </button>
                    <%
					}
                    %>
                    
                    <%
					if(aktifkanTopupDiAnggota){
					%>
                    <!-- Tombol Topup / Isi Saldo -->
                    <button class="btn btn-warning text-dark fw-bold rounded-pill shadow-sm px-3 ms-1 my-1 my-lg-0 d-flex align-items-center justify-content-center" onclick="verifyAndShowTopupModal<%=rnd%>(this)" style="font-size: 0.85rem; height: 36px;">
                        <i class="fas fa-plus-circle me-2"></i><%=Common.getBahasaConfig("Isi Saldo")%>
                    </button>
 					<%
					}
                    %>
                </div>
            <% } else { %>
                <div class="d-flex gap-2 w-100 justify-content-center justify-content-md-end px-2">
                    <a href="<%=Common.ROOT%>/login" class="btn btn-sm btn-outline-primary rounded-pill px-3 shadow-sm fw-bold flex-grow-1 flex-md-grow-0">
                        <i class="fas fa-sign-in-alt me-1"></i><%=Common.getBahasaConfig("Masuk")%>
                    </a>
                    <a href="<%=Common.ROOT%>/daftarAnggota" class="btn btn-sm btn-primary rounded-pill px-3 shadow-sm fw-bold flex-grow-1 flex-md-grow-0">
                        <i class="fas fa-user-plus me-1"></i><%=Common.getBahasaConfig("Daftar")%>
                    </a>
                </div>
            <% } %>
        </div>
    </div>

    <!-- HERO BANNER -->
    <div class="p-3 p-md-4 mb-3 text-start d-flex flex-column flex-md-row align-items-center justify-content-between gap-3 text-white rounded-4 shadow-sm position-relative overflow-hidden" style="background: var(--theme-gradient);">
        <!-- Dekorasi Background -->
        <div class="position-absolute top-0 end-0 opacity-25" style="transform: translate(15%, -15%);">
            <i class="fas fa-shopping-cart" style="font-size: 10rem;"></i>
        </div>
        
        <div class="me-md-3 position-relative" style="z-index: 2;">
            <h4 class="fw-bolder mb-1 fs-8 text-white shadow-sm" style="text-shadow: 1px 1px 2px rgba(0,0,0,0.2);"><i class="fas fa-store me-2"></i><%=Common.getBahasaConfig("Pusat Belanja")%></h4>
            <p class="mb-0 opacity-100 small text-white-50"><%=Common.getBahasaConfig("Temukan beragam produk dengan harga terbaik dan nikmati promo diskon serta "+istilahCashback+" harian.")%></p>
        </div>
        
        <div class="w-100 flex-shrink-0 mt-2 mt-md-0 position-relative" style="z-index: 2; max-width: 360px;">
            <div class="input-group shadow-sm rounded-pill overflow-hidden bg-white p-1 border border-2 border-white">
                <span class="input-group-text bg-transparent border-0 text-primary ps-3 pe-2"><i class="fas fa-search"></i></span>
                <input type="text" class="form-control border-0 py-2 fw-medium shadow-none bg-transparent" id="searchKatalog<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik nama produk...")%>" style="font-size: 14px;">
                <button class="btn btn-primary rounded-pill fw-bold px-4 shadow-sm" type="button" onclick="currentPage<%=rnd%>=1; loadKatalogProduk<%=rnd%>();" style="font-size: 13px;">
                    <%=Common.getBahasaConfig("Cari")%>
                </button>
            </div>
        </div>
    </div>

    <div class="row g-3 px-1 px-md-0">
        
        <!-- KIRI: KATALOG PRODUK -->
        <div class="col-xl-8 col-lg-7">
            
            <!-- SELEKSI TOKO / PEDAGANG WAJIB -->
            <div class="card border-0 shadow-sm rounded-4 mb-3 border-top border-info border-3">
                <div class="card-body p-3">
                    <label class="form-label small fw-bold text-secondary mb-1"><i class="fas fa-store text-info me-1"></i><%=Common.getBahasaConfig("Pilih Pedagang / Toko")%> <span class="text-danger">*</span></label>
                    <select class="form-select form-select-lg shadow-sm fw-bold border-0 bg-light" id="selectTokoKatalog<%=rnd%>" onchange="handleTokoChange<%=rnd%>()">
                        <option value=""><%=Common.getBahasaConfig("Memuat data toko...")%></option>
                    </select>
                    <div class="form-text small text-muted mt-1">
                        <i class="fas fa-info-circle text-info me-1"></i><%=Common.getBahasaConfig("Anda dapat berbelanja dari beberapa tenant sekaligus — isi keranjang dari satu tenant, lalu ganti tenant untuk menambah produk lainnya. Keranjang dikelompokkan otomatis per tenant.")%>
                    </div>
                </div>
            </div>

            <div class="d-flex justify-content-between align-items-center mb-2 border-bottom pb-2 mx-1 mx-md-0">
                <h6 class="fw-bold text-dark mb-0 small"><i class="fas fa-box-open me-2 text-primary"></i><%=Common.getBahasaConfig("Katalog Produk")%></h6>
                <span class="badge bg-primary bg-opacity-10 text-primary rounded-pill px-2 py-1 border border-primary border-opacity-25" style="font-size: 0.7rem;" id="totalKatalog<%=rnd%>">0 <%=Common.getBahasaConfig("Produk")%></span>
            </div>
            
            <div class="row g-2 g-md-3" id="gridProduk<%=rnd%>">
                <div class="col-12 text-center py-5">
                    <div class="spinner-border text-primary" role="status"></div>
                    <div class="mt-2 small fw-medium text-muted"><%=Common.getBahasaConfig("Sistem sedang memuat etalase, mohon tunggu...")%></div>
                </div>
            </div>
            
            <div class="d-flex justify-content-between align-items-center mt-3 bg-white p-2 rounded-4 shadow-sm border d-none mx-1 mx-md-0" id="pagingContainer<%=rnd%>">
                <button class="btn btn-sm btn-outline-primary rounded-pill px-3 fw-bold" style="font-size: 0.8rem;" id="btnPrevPage<%=rnd%>" onclick="changePageKatalog<%=rnd%>(-1)">
                    <i class="fas fa-chevron-left me-1"></i><span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Sebelumnya")%></span>
                </button>
                <div class="fw-bold text-muted text-center" style="font-size: 0.8rem;">
                    <%=Common.getBahasaConfig("Hal.")%> <span id="lblCurrentPage<%=rnd%>" class="text-primary fs-6">1</span> / <span id="lblTotalPage<%=rnd%>">1</span>
                </div>
                <button class="btn btn-sm btn-outline-primary rounded-pill px-3 fw-bold" style="font-size: 0.8rem;" id="btnNextPage<%=rnd%>" onclick="changePageKatalog<%=rnd%>(1)">
                    <span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Selanjutnya")%></span><i class="fas fa-chevron-right ms-1"></i>
                </button>
            </div>
        </div>

        <!-- KANAN: KERANJANG BELANJA (CART) -->
        <div class="col-xl-4 col-lg-5 pb-5 mb-5" id="cartSection<%=rnd%>">
            
            <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-3 mx-1 mx-md-0 mb-4 cart-sticky-<%=rnd%>" id="mainCartWrapper<%=rnd%>">
                
                <div class="card-header bg-white border-bottom-0 pt-3 pb-2 px-3">

                    <!-- DETAIL TANGGAL HARI INI (DI ATAS KERANJANG BELANJA) -->
                    <div class="d-flex align-items-center justify-content-between bg-primary bg-opacity-10 text-primary rounded-3 px-3 py-2 mb-3 border border-primary border-opacity-25">
                        <span class="fw-bold text-truncate" style="font-size: 0.78rem;">
                            <i class="far fa-calendar-alt me-2"></i><%=tanggalHariIniDisplay%>
                        </span>
                        <span class="fw-bolder badge bg-primary text-white rounded-pill ms-2 flex-shrink-0" id="jamHariIni<%=rnd%>" style="font-size: 0.72rem;">--:--:--</span>
                    </div>

                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <h6 class="fw-bold text-dark mb-0 small">
                            <i class="fas fa-shopping-basket text-primary me-2"></i><%=Common.getBahasaConfig("Keranjang Belanja")%>
                        </h6>
                        <span class="badge bg-danger rounded-circle shadow-sm" id="badgeCartCount<%=rnd%>">0</span>
                    </div>

                    <!-- AREA PEMILIHAN MEJA (DITAMPILKAN JIKA KONFIGURASI AKTIF) -->
                    <div class="bg-light p-2 rounded-3 border border-secondary border-opacity-25 mb-2 <%= !aktifkanPilihanMeja ? "d-none" : "" %>" id="areaPemilihanMeja<%=rnd%>">
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <span class="small fw-bold text-secondary"><i class="fas fa-chair me-1"></i><%=Common.getBahasaConfig("Meja Pemesan")%> <span class="text-danger">*</span></span>
                            <!-- Opsi Tanpa Meja (Bawa Pulang) -->
                            <div class="form-check form-switch fs-6 mb-0">
                                <input class="form-check-input ms-0 me-2" type="checkbox" id="chkTanpaMeja<%=rnd%>" onchange="toggleTanpaMeja<%=rnd%>()" style="cursor: pointer;">
                                <label class="form-check-label fw-bold text-primary small" for="chkTanpaMeja<%=rnd%>" style="font-size: 11px; cursor: pointer;"><%=Common.getBahasaConfig("Bawa Pulang")%></label>
                            </div>
                        </div>

                        <!-- TOMBOL SCAN MEJA KANTIN -->
                        <button class="btn btn-sm btn-outline-info w-100 rounded-pill fw-bold border-dashed" onclick="bukaModalScannerMeja<%=rnd%>()" id="btnPilihMeja<%=rnd%>">
                            <i class="fas fa-qrcode me-2"></i><%=Common.getBahasaConfig("Scan QR Meja")%>
                        </button>
                        
                        <!-- Indikator Meja Terpilih -->
                        <div id="infoMejaTerpilih<%=rnd%>" class="d-none bg-info bg-opacity-10 text-dark border border-info rounded-pill px-3 py-1 mt-2 text-center shadow-sm">
                            <small class="fw-bold"><i class="fas fa-check-circle text-info me-2"></i><span id="lblNamaMeja<%=rnd%>"></span></small>
                            <button class="btn btn-link btn-sm text-danger p-0 ms-2" onclick="hapusMejaTerpilih<%=rnd%>()" title="<%=Common.getBahasaConfig("Batal Pilih Meja")%>"><i class="fas fa-times-circle"></i></button>
                        </div>
                    </div>
                </div>
                
                <div class="card-body p-0">
                    <div class="table-responsive px-2" style="overflow-y: visible; min-height: 150px;">
                        <table class="table table-borderless align-middle mb-0">
                            <tbody id="cartContainer<%=rnd%>">
                                <tr>
                                    <td class="text-center text-muted py-5">
                                        <i class="fas fa-cart-arrow-down fa-3x mb-3 opacity-25 text-secondary"></i>
                                        <br><small class="fw-medium"><%=Common.getBahasaConfig("Keranjang Anda masih kosong.")%></small>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>

                    <div class="bg-light p-3 rounded-bottom-4 border-top border-2">
                        <div class="mb-3" <%= !isLogin ? "style='display:none;'" : "" %>>
                            <label class="form-label fw-bold text-secondary mb-1" style="font-size: 0.75rem;"><%=Common.getBahasaConfig("Metode Pembayaran")%></label>
                            <select class="form-select form-select-sm fw-bold shadow-sm border-0" id="selectCaraBayar<%=rnd%>" style="font-size: 0.8rem;">
                                <option value=""><%=Common.getBahasaConfig("Memuat saluran...")%></option>
                            </select>
                        </div>

                        <!-- OPSI TAMBAH KOMENTAR / CATATAN (DI BAWAH METODE PEMBAYARAN) -->
                        <div class="mb-3" <%= !isLogin ? "style='display:none;'" : "" %>>
                            <label class="form-label fw-bold text-secondary mb-1" style="font-size: 0.75rem;" for="inputKomentar<%=rnd%>">
                                <i class="far fa-comment-dots me-1"></i><%=Common.getBahasaConfig("Catatan / Komentar")%>
                                <span class="text-muted fw-normal">(<%=Common.getBahasaConfig("opsional")%>)</span>
                            </label>
                            <textarea class="form-control form-control-sm shadow-sm border-0 bg-white" id="inputKomentar<%=rnd%>" rows="2" maxlength="250" style="font-size: 0.8rem; resize: none;" placeholder="<%=Common.getBahasaConfig("Contoh: Tidak pakai sambal, antar ke meja, dan lain-lain.")%>"></textarea>
                        </div>

                        <div class="d-flex justify-content-between mb-1" style="font-size: 0.8rem;">
                            <span class="text-secondary fw-semibold"><%=Common.getBahasaConfig("Subtotal")%></span>
                            <span class="text-dark fw-bold" id="cartSubtotal<%=rnd%>">Rp 0</span>
                        </div>
                        <div class="d-flex justify-content-between mb-1" style="font-size: 0.8rem;">
                            <span class="text-secondary fw-semibold"><%=Common.getBahasaConfig("Diskon Promosi")%></span>
                            <span class="text-danger fw-bold" id="cartDiskon<%=rnd%>">- Rp 0</span>
                        </div>
                        <div class="d-flex justify-content-between mb-2 border-bottom pb-2" style="font-size: 0.8rem;<%=aktifkanTampilCashback ? "" : "display: none !important;"%>">
                            <span class="text-secondary fw-semibold"><%=istilahCashback%></span>
                            <span class="text-info fw-bold" id="cartCashback<%=rnd%>">+ Rp 0</span>
                        </div>
                        <div class="d-flex justify-content-between mb-3 align-items-center mt-2">
                            <h6 class="fw-bold text-dark mb-0 small"><%=Common.getBahasaConfig("Total Tagihan")%></h6>
                            <h5 class="fw-bolder text-success mb-0" id="cartGrandTotal<%=rnd%>">Rp 0</h5>
                        </div>

                        <% if (isLogin) { %>
                            <button class="btn btn-primary w-100 py-2 py-md-3 mb-2 fw-bold rounded-pill shadow-sm" style="font-size: 0.95rem;" onclick="initiateCheckout<%=rnd%>()" id="btnCheckout<%=rnd%>">
                                <i class="fas fa-check-circle me-1"></i><%=Common.getBahasaConfig("Konfirmasi Pembayaran")%>
                            </button>
                        <% } else { %>
                            <a href="<%=Common.ROOT%>/login" class="btn btn-secondary w-100 py-2 py-md-3 mb-2 fw-bold rounded-pill shadow-sm d-block text-center text-decoration-none" style="font-size: 0.95rem;">
                                <i class="fas fa-lock me-1"></i><%=Common.getBahasaConfig("Masuk untuk Membayar")%>
                            </a>
                        <% } %>
                    </div>
                </div>
            </div>

            <!-- Kontainer Hasil Checkout -->
            <div class="card border-0 shadow-sm rounded-4 mx-1 mx-md-0 mb-5 d-none animate__animated animate__fadeIn" id="checkoutResultContainer<%=rnd%>">
            </div>

        </div>
    </div>

</div>

<!-- Floating Action Button for Mobile Cart, posisinya ditinggikan agar di atas footer -->
<button class="btn btn-primary d-lg-none position-fixed rounded-circle shadow-lg d-flex align-items-center justify-content-center floating-cart-btn-<%=rnd%>" 
        style="width: 60px; height: 60px; z-index: 9999; bottom: 135px; right: 20px; transition: opacity 0.3s ease, visibility 0.3s ease;" 
        onclick="document.getElementById('cartSection<%=rnd%>').scrollIntoView({behavior: 'smooth', block: 'start'});">
    <i class="fas fa-shopping-cart fa-lg"></i>
    <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger shadow-sm border border-light" id="floatingCartCount<%=rnd%>" style="font-size: 11px;">0</span>
</button>

<script>
    // ==========================================
    // VARIABEL GLOBAL & STATE
    // ==========================================
    const isLogin<%=rnd%> = <%=isLogin%>;
    const idMemberAktif<%=rnd%> = '<%=idMember%>';
    const urlTopupService<%=rnd%> = '<%=urlTopupService%>';
    const reqPilihMeja<%=rnd%> = <%=aktifkanPilihanMeja%>;
    const minimalDeposit<%=rnd%> = <%=minimalDeposit%>; // Variable Minimal Saldo Mengendap
    let saldoMember<%=rnd%> = 0;
    
    let cart<%=rnd%> = [];
    let grandTotalValue<%=rnd%> = 0;
    let arrAturanDiskon<%=rnd%> = [];
    
    // Variabel Scanner & Meja
    let html5QrcodeScanner<%=rnd%> = null;
    let html5QrcodeScannerBayar<%=rnd%> = null;
    let scannerBayarModalInstance<%=rnd%> = null;
    let idMejaTerpilih<%=rnd%> = null;
    let isBawaPulang<%=rnd%> = false;
    
    // Variabel Paginasi Katalog Produk
    let currentPage<%=rnd%> = 1;
    const limitPerPage<%=rnd%> = 10;
    let totalRecords<%=rnd%> = 0;
    let topupModalInstance<%=rnd%> = null;
    let modalPilihTopupInstance<%=rnd%> = null;
    let idCaraBayarTopup<%=rnd%> = '';
    let scannerModalInstance<%=rnd%> = null;

    // ==========================================
    // INJEKSI MODAL & DOM KE BODY
    // ==========================================
    const injectModals<%=rnd%> = () => {
        const modalsHtml = `
            <!-- Modal Scanner QR Meja -->
            <div class="modal fade" id="modalScannerMeja<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content rounded-4 border-0 shadow-lg">
                        <div class="modal-header bg-light border-bottom-0 pb-2 pt-3 px-4">
                            <h5 class="modal-title fw-bold text-dark">
                                <i class="fas fa-camera text-info me-2"></i><%=Common.getBahasaConfig("Pindai QR Meja")%>
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close" onclick="tutupScannerMeja<%=rnd%>()"></button>
                        </div>
                        <div class="modal-body p-3 text-center">
                            <p class="text-muted small mb-3"><%=Common.getBahasaConfig("Arahkan kamera Anda ke QR Code yang terdapat pada meja Kantin.")%></p>
                            <div id="reader_<%=rnd%>" class="shadow-sm mb-3"></div>
                            <div id="scannerStatus<%=rnd%>" class="alert alert-warning py-2 small fw-bold d-none"></div>
                        </div>
                        <div class="modal-footer border-top-0 justify-content-center pt-0">
                            <button type="button" class="btn btn-light rounded-pill px-4 fw-bold shadow-sm" data-bs-dismiss="modal" onclick="tutupScannerMeja<%=rnd%>()"><%=Common.getBahasaConfig("Batal")%></button>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Modal Scanner QR Pembayaran -->
            <div class="modal fade" id="modalScannerBayar<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content rounded-4 border-0 shadow-lg">
                        <div class="modal-header bg-dark border-bottom-0 pb-2 pt-3 px-4">
                            <h5 class="modal-title fw-bold text-white">
                                <i class="fas fa-qrcode text-warning me-2"></i><%=Common.getBahasaConfig("Pindai QR Pembayaran")%>
                            </h5>
                            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close" onclick="tutupScannerBayar<%=rnd%>()"></button>
                        </div>
                        <div class="modal-body p-3 text-center">
                            <p class="text-muted small mb-3"><%=Common.getBahasaConfig("Arahkan kamera ke QR Code yang tampil di layar Kasir.")%></p>
                            <div id="reader_bayar_<%=rnd%>" class="shadow-sm mb-3"></div>
                            <div id="scannerBayarStatus<%=rnd%>" class="alert alert-warning py-2 small fw-bold d-none"></div>
                        </div>
                        <div class="modal-footer border-top-0 justify-content-center pt-0">
                            <button type="button" class="btn btn-light rounded-pill px-4 fw-bold shadow-sm" data-bs-dismiss="modal" onclick="tutupScannerBayar<%=rnd%>()"><%=Common.getBahasaConfig("Batal")%></button>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Modal Pilih Metode Topup -->
            <div class="modal fade" id="modalPilihTopup<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
                <div class="modal-dialog modal-dialog-centered modal-sm">
                    <div class="modal-content rounded-4 border-0 shadow-lg">
                        <div class="modal-header bg-light border-bottom-0 pb-2 pt-3 px-4">
                            <h6 class="modal-title fw-bold text-dark"><i class="fas fa-wallet text-warning me-2"></i><%=Common.getBahasaConfig("Pilih Saluran Top-Up")%></h6>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body p-4 pt-2">
                            <label class="form-label small fw-bold text-secondary mb-2"><%=Common.getBahasaConfig("Pilih metode pembayaran:")%></label>
                            <select class="form-select mb-3 fw-medium shadow-sm" id="selectMetodeTopup<%=rnd%>"></select>
                            <button class="btn btn-warning w-100 fw-bold rounded-pill shadow-sm" onclick="lanjutkanPilihTopup<%=rnd%>()">
                                <%=Common.getBahasaConfig("Lanjut")%> <i class="fas fa-arrow-right ms-1"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Modal top-up milik halaman ini; transaksi dilakukan lewat API. -->
            <div class="modal fade" id="modalTopup<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
                <div class="modal-dialog modal-dialog-centered modal-lg modal-fullscreen-sm-down">
                    <div class="modal-content rounded-4 border-0 shadow-lg">
                        <div class="modal-header bg-light border-bottom-0 pb-2 pt-3 px-4">
                            <h5 class="modal-title fw-bold text-dark"><i class="fas fa-wallet text-warning me-2"></i><%=Common.getBahasaConfig("Isi Saldo (Top-Up)")%></h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close" onclick="closeTopupModal<%=rnd%>()"></button>
                        </div>
                        <div class="modal-body p-4 bg-light">
                            <form id="formTopupLangsung<%=rnd%>" onsubmit="event.preventDefault(); eksekusiTopupLangsung<%=rnd%>();">
                                <div id="pesanTopupLangsung<%=rnd%>" class="alert d-none"></div>
                                <label class="form-label fw-bold"><%=Common.getBahasaConfig("Nominal Isi Saldo")%></label>
                                <div class="input-group input-group-lg mb-3 shadow-sm">
                                    <span class="input-group-text">Rp</span>
                                    <input type="number" min="10000" step="1000" class="form-control fw-bold" id="nominalTopupLangsung<%=rnd%>" placeholder="10000" required>
                                </div>
                                <div class="d-flex flex-wrap gap-2 mb-4">
                                    <button type="button" class="btn btn-outline-primary" onclick="document.getElementById('nominalTopupLangsung<%=rnd%>').value=10000">10.000</button>
                                    <button type="button" class="btn btn-outline-primary" onclick="document.getElementById('nominalTopupLangsung<%=rnd%>').value=50000">50.000</button>
                                    <button type="button" class="btn btn-outline-primary" onclick="document.getElementById('nominalTopupLangsung<%=rnd%>').value=100000">100.000</button>
                                    <button type="button" class="btn btn-outline-primary" onclick="document.getElementById('nominalTopupLangsung<%=rnd%>').value=500000">500.000</button>
                                </div>
                                <label class="form-label fw-bold"><%=Common.getBahasaConfig("Saluran Pembayaran")%></label>
                                <select class="form-select form-select-lg mb-4 shadow-sm" id="channelTopupLangsung<%=rnd%>" required>
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Bank / Saluran --")%></option>
                                    <%
                                        for (String channel : channelTopup.split(";")) {
                                            if (channel.trim().isEmpty()) continue;
                                            String[] detailChannel = StringUtils.split(channel.trim(), ":");
                                            if (detailChannel == null || detailChannel.length < 3) continue;
                                    %>
                                        <option value="<%=detailChannel[0]%>|<%=detailChannel[1]%>"><%=detailChannel[2]%> — Admin Rp <%=Common.numberFormat.get().format(Double.parseDouble(detailChannel[1]))%></option>
                                    <% } %>
                                </select>
                                <button class="btn btn-success btn-lg w-100 rounded-pill fw-bold shadow-sm" id="btnTopupLangsung<%=rnd%>" type="submit">
                                    <i class="fas fa-lock me-2"></i><%=Common.getBahasaConfig("Buat Tagihan Pembayaran")%>
                                </button>
                                <small class="text-muted d-block text-center mt-3"><%=Common.getBahasaConfig("Setelah tagihan dibuat, Anda akan dialihkan ke halaman pembayaran.")%></small>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        `;
        document.body.insertAdjacentHTML('beforeend', modalsHtml);
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

    // ==========================================
    // LOGIKA SCANNER BAYAR QR (ONLINE)
    // ==========================================
    const bukaModalScannerBayar<%=rnd%> = () => {
        let modalElement = document.getElementById('modalScannerBayar<%=rnd%>');
        
        if (!scannerBayarModalInstance<%=rnd%>) {
            scannerBayarModalInstance<%=rnd%> = new bootstrap.Modal(modalElement);
        }
        
        document.getElementById('scannerBayarStatus<%=rnd%>').classList.add('d-none');
        scannerBayarModalInstance<%=rnd%>.show();

        if (!html5QrcodeScannerBayar<%=rnd%>) {
            html5QrcodeScannerBayar<%=rnd%> = new Html5QrcodeScanner(
                "reader_bayar_<%=rnd%>", 
                { fps: 10, qrbox: { width: 250, height: 250 }, aspectRatio: 1.0 }
            );
        }
        html5QrcodeScannerBayar<%=rnd%>.render(onScanSuccessBayar<%=rnd%>, onScanFailure<%=rnd%>);
    };

    const tutupScannerBayar<%=rnd%> = () => {
        if (html5QrcodeScannerBayar<%=rnd%>) {
            html5QrcodeScannerBayar<%=rnd%>.clear().catch(error => {
                console.error("Failed to clear html5QrcodeScannerBayar. ", error);
            });
            html5QrcodeScannerBayar<%=rnd%> = null;
        }
        if(scannerBayarModalInstance<%=rnd%>) {
            scannerBayarModalInstance<%=rnd%>.hide();
        }
    };

    const onScanSuccessBayar<%=rnd%> = async (decodedText, decodedResult) => {
        html5QrcodeScannerBayar<%=rnd%>.pause(true);
        const sts = document.getElementById('scannerBayarStatus<%=rnd%>');
        sts.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i><%=Common.getBahasaConfig("Memproses Pembayaran...")%>';
        sts.className = 'alert alert-info py-2 small fw-bold';
        sts.classList.remove('d-none');

        try {
            // Validasi string JSON dari QR Code
            const qrData = JSON.parse(decodedText);
            if(!qrData.nominal || !qrData.kodeToko || !qrData.kodeUnik) {
                throw new Error("Format QR tidak valid");
            }

            // Segarkan data saldo terlebih dahulu dari server
            await loadSaldoMember<%=rnd%>(true);

            // Pengecekan kecukupan saldo member sebelum menembak API (Dengan Minimal Deposit)
            const nominalBayar = parseFloat(qrData.nominal);
            if (saldoMember<%=rnd%> < nominalBayar) {
                sts.innerHTML = '<i class="fas fa-exclamation-triangle me-2"></i><%=Common.getBahasaConfig("Saldo Anda tidak mencukupi untuk pembayaran ini.")%>';
                sts.className = 'alert alert-danger py-2 small fw-bold';
                showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Saldo Kas Anda tidak mencukupi untuk memproses pembayaran Online!")%>', 'bg-danger text-white');
                
                setTimeout(() => { html5QrcodeScannerBayar<%=rnd%>.resume(); sts.classList.add('d-none'); }, 3000);
                return;
            }

            if ((saldoMember<%=rnd%> - nominalBayar) < minimalDeposit<%=rnd%>) {
                sts.innerHTML = '<i class="fas fa-exclamation-triangle me-2"></i><%=Common.getBahasaConfig("Batas minimal saldo mengendap tidak terpenuhi.")%>';
                sts.className = 'alert alert-danger py-2 small fw-bold';
                showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Transaksi QR gagal! Sisa saldo Anda setelah transaksi ini kurang dari batas minimal yang diizinkan (Rp ")%>' + formatRp<%=rnd%>(minimalDeposit<%=rnd%>) + ').', 'bg-danger text-white');
                
                setTimeout(() => { html5QrcodeScannerBayar<%=rnd%>.resume(); sts.classList.add('d-none'); }, 3000);
                return;
            }

            // Payload sesuai permintaan: action bayarOnline dan field kode diisi string JSON
            const payload = {
                action: "bayarOnline",
                idMember: idMemberAktif<%=rnd%>,
                kode: decodedText
            };

            const res = await fetchDataAPI<%=rnd%>(payload);
            if(res.status === '00' || res.status === 'success') {
                showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Pembayaran Online Berhasil Diproses!")%>', 'bg-success text-white');
                tutupScannerBayar<%=rnd%>();
                // Segarkan saldo untuk melihat pengurangan
                loadSaldoMember<%=rnd%>(true);
            } else {
                sts.innerHTML = '<i class="fas fa-times-circle me-2"></i>' + (res.description || '<%=Common.getBahasaConfigJS("Gagal memproses pembayaran.")%>');
                sts.className = 'alert alert-danger py-2 small fw-bold';
                setTimeout(() => { html5QrcodeScannerBayar<%=rnd%>.resume(); }, 3000);
            }
        } catch(e) {
            console.error("QR Scan Error:", e);
            sts.innerHTML = '<i class="fas fa-exclamation-triangle me-2"></i><%=Common.getBahasaConfig("QR Code tidak dikenali sebagai instruksi pembayaran valid.")%>';
            sts.className = 'alert alert-danger py-2 small fw-bold';
            setTimeout(() => { html5QrcodeScannerBayar<%=rnd%>.resume(); }, 3000);
        }
    };


    // ==========================================
    // LOGIKA MEJA KANTIN (SCAN & BAWA PULANG)
    // ==========================================
    const toggleTanpaMeja<%=rnd%> = () => {
        isBawaPulang<%=rnd%> = document.getElementById('chkTanpaMeja<%=rnd%>').checked;
        const btnPilihMeja = document.getElementById('btnPilihMeja<%=rnd%>');
        const infoMeja = document.getElementById('infoMejaTerpilih<%=rnd%>');
        
        if (isBawaPulang<%=rnd%>) {
            // Hapus pilihan meja sebelumnya (jika ada) dan sembunyikan tombol scan
            idMejaTerpilih<%=rnd%> = null;
            infoMeja.classList.add('d-none');
            btnPilihMeja.classList.add('d-none');
        } else {
            // Munculkan kembali tombol scan jika checkbox di-uncheck
            btnPilihMeja.classList.remove('d-none');
        }
    };

    const bukaModalScannerMeja<%=rnd%> = () => {
        let modalElement = document.getElementById('modalScannerMeja<%=rnd%>');
        
        // Pengecekan aman agar Bootstrap Modal selalu diinisialisasi
        if (!scannerModalInstance<%=rnd%>) {
            scannerModalInstance<%=rnd%> = new bootstrap.Modal(modalElement);
        }
        
        document.getElementById('scannerStatus<%=rnd%>').classList.add('d-none');
        scannerModalInstance<%=rnd%>.show();

        // Inisialisasi HTML5-QRCode dengan reset aman
        if (!html5QrcodeScanner<%=rnd%>) {
            html5QrcodeScanner<%=rnd%> = new Html5QrcodeScanner(
                "reader_<%=rnd%>", 
                { fps: 10, qrbox: { width: 250, height: 250 }, aspectRatio: 1.0 }
            );
        }
        html5QrcodeScanner<%=rnd%>.render(onScanSuccess<%=rnd%>, onScanFailure<%=rnd%>);
    };

    const tutupScannerMeja<%=rnd%> = () => {
        if (html5QrcodeScanner<%=rnd%>) {
            html5QrcodeScanner<%=rnd%>.clear().catch(error => {
                console.error("Failed to clear html5QrcodeScanner. ", error);
            });
            // Hapus referensi scanner agar bisa di-render ulang dengan bersih
            html5QrcodeScanner<%=rnd%> = null;
        }
        if(scannerModalInstance<%=rnd%>) {
            scannerModalInstance<%=rnd%>.hide();
        }
    };

    const onScanFailure<%=rnd%> = (error) => {
        // Abaikan peringatan berkelanjutan saat kamera mencari QR
        // console.warn(error);
    };

    const onScanSuccess<%=rnd%> = async (decodedText, decodedResult) => {
        // Hentikan scanner sementara agar tidak berulang kali tertembak
        html5QrcodeScanner<%=rnd%>.pause(true);
        
        const sts = document.getElementById('scannerStatus<%=rnd%>');
        sts.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i><%=Common.getBahasaConfig("Memverifikasi Kode Meja...")%>';
        sts.className = 'alert alert-info py-2 small fw-bold';
        sts.classList.remove('d-none');

        // Verifikasi ke database meja_kantin berdasarkan kode QR
        const sqlVerifikasi = "SELECT id, nama FROM koperasi.meja_kantin WHERE kode = '" + decodedText.replace(/'/g, "''") + "' AND aktif = true LIMIT 1";

        try {
            const res = await fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlVerifikasi });
            
            if (res.data && res.data.length > 0) {
                const meja = res.data[0];
                idMejaTerpilih<%=rnd%> = meja.id;
                
                // Update UI Keranjang
                document.getElementById('btnPilihMeja<%=rnd%>').classList.add('d-none');
                const infoBox = document.getElementById('infoMejaTerpilih<%=rnd%>');
                document.getElementById('lblNamaMeja<%=rnd%>').innerText = meja.nama || decodedText;
                infoBox.classList.remove('d-none');

                showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Meja berhasil dipilih.")%>', 'bg-success text-white');
                tutupScannerMeja<%=rnd%>();
            } else {
                sts.innerHTML = '<i class="fas fa-times-circle me-2"></i><%=Common.getBahasaConfig("QR Code tidak dikenali atau meja tidak aktif.")%>';
                sts.className = 'alert alert-danger py-2 small fw-bold';
                // Resume pencarian setelah beberapa detik
                setTimeout(() => { html5QrcodeScanner<%=rnd%>.resume(); }, 2000);
            }
        } catch (error) {
            sts.innerHTML = '<i class="fas fa-exclamation-triangle me-2"></i><%=Common.getBahasaConfig("Koneksi gagal. Coba lagi.")%>';
            sts.className = 'alert alert-danger py-2 small fw-bold';
            setTimeout(() => { html5QrcodeScanner<%=rnd%>.resume(); }, 2000);
        }
    };

    const hapusMejaTerpilih<%=rnd%> = () => {
        idMejaTerpilih<%=rnd%> = null;
        document.getElementById('infoMejaTerpilih<%=rnd%>').classList.add('d-none');
        document.getElementById('btnPilihMeja<%=rnd%>').classList.remove('d-none');
    };

    // ==========================================
    // LOGIKA REFRESH SALDO
    // ==========================================
    const refreshSaldoTerkini<%=rnd%> = async (element) => {
        const btn = element || document.getElementById('btnRefreshSaldo<%=rnd%>');
        if (!btn) return;
        
        const icon = btn.querySelector('i');
        
        if (icon) icon.classList.add('fa-spin');
        btn.disabled = true;
        
        await loadSaldoMember<%=rnd%>(true);
        
        if (icon) icon.classList.remove('fa-spin');
        btn.disabled = false;
        
        showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Informasi saldo Anda berhasil diperbarui.")%>', 'bg-success text-white');
    };

    const refreshSisaCashback<%=rnd%> = async (element) => {
        const btn = element || document.getElementById('btnRefreshCashback<%=rnd%>');
        if (!btn) return;
        
        const icon = btn.querySelector('i');
        if (icon) icon.classList.add('fa-spin');
        btn.disabled = true;
        
        await loadSisaCashback<%=rnd%>();
        
        if (icon) icon.classList.remove('fa-spin');
        btn.disabled = false;
        
        showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Informasi Sisa "+istilahCashback+" Anda berhasil diperbarui.")%>', 'bg-info text-white');
    };

    // ==========================================
    // LOGIKA MODAL TOPUP (DYNAMIC SELECTION)
    // ==========================================
    const verifyAndShowTopupModal<%=rnd%> = async (btnElement) => {
        let oriContent = "";
        if (btnElement) {
            oriContent = btnElement.innerHTML;
            btnElement.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Memuat...")%>';
            btnElement.disabled = true;
        }

        const sql = "SELECT cpk.id, cpk.nama " +
                    "FROM koperasi.cara_pembayaran_koperasi cpk " +
                    "WHERE cpk.aktif = true AND cpk.manual = false " +
                    "AND (SELECT jak.daftar_cara_pembayaran_yang_boleh_di_pilih FROM koperasi.jenis_anggota_koperasi jak WHERE jak.id = <%=idJenisAnggota%>) LIKE '%,' || cpk.id || ',%' " +
                    "ORDER BY cpk.nama ASC";

        try {
            const res = await fetchDataAPI<%=rnd%>({ action: "sql", sql: sql });
            if (res.data && res.data.length === 1) {
                bukaTopupLangsung<%=rnd%>(res.data[0].id);
            } else if (res.data && res.data.length > 1) {
                const selectEl = document.getElementById('selectMetodeTopup<%=rnd%>');
                selectEl.innerHTML = '';
                res.data.forEach(item => {
                    selectEl.innerHTML += '<option value="' + item.id + '">' + item.nama + '</option>';
                });

                let modalElement = document.getElementById('modalPilihTopup<%=rnd%>');
                if (!modalPilihTopupInstance<%=rnd%>) {
                    modalPilihTopupInstance<%=rnd%> = new bootstrap.Modal(modalElement);
                }
                modalPilihTopupInstance<%=rnd%>.show();
            } else {
                showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Tidak ada saluran pembayaran online yang tersedia untuk Anda.")%>', 'bg-warning text-dark');
            }
        } catch (e) {
            console.error("Topup check error:", e);
            showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal memuat saluran pembayaran.")%>', 'bg-danger text-white');
        } finally {
            if (btnElement) {
                btnElement.innerHTML = oriContent;
                btnElement.disabled = false;
            }
        }
    };

    const lanjutkanPilihTopup<%=rnd%> = () => {
        const idCaraBayar = document.getElementById('selectMetodeTopup<%=rnd%>').value;
        if (modalPilihTopupInstance<%=rnd%>) {
            modalPilihTopupInstance<%=rnd%>.hide();
        }
        bukaTopupLangsung<%=rnd%>(idCaraBayar);
    };

    const bukaTopupLangsung<%=rnd%> = (idCaraBayar) => {
        let modalElement = document.getElementById('modalTopup<%=rnd%>');
        if (!topupModalInstance<%=rnd%>) {
            topupModalInstance<%=rnd%> = new bootstrap.Modal(modalElement);
        }
        idCaraBayarTopup<%=rnd%> = idCaraBayar;
        document.getElementById('formTopupLangsung<%=rnd%>').reset();
        document.getElementById('pesanTopupLangsung<%=rnd%>').classList.add('d-none');
        topupModalInstance<%=rnd%>.show();
    };

    const closeTopupModal<%=rnd%> = () => {
        idCaraBayarTopup<%=rnd%> = '';
        loadSaldoMember<%=rnd%>(true); 
    };

    const tampilkanPesanTopupLangsung<%=rnd%> = (message, success) => {
        const element = document.getElementById('pesanTopupLangsung<%=rnd%>');
        element.textContent = message || '';
        element.className = 'alert ' + (success ? 'alert-success' : 'alert-danger');
        element.classList.toggle('d-none', !message);
    };

    const eksekusiTopupLangsung<%=rnd%> = async () => {
        const nominal = Number(document.getElementById('nominalTopupLangsung<%=rnd%>').value || 0);
        const channelValue = document.getElementById('channelTopupLangsung<%=rnd%>').value;
        const channelParts = channelValue.split('|');
        if (nominal < 10000 || channelParts.length < 2 || !idCaraBayarTopup<%=rnd%>) {
            tampilkanPesanTopupLangsung<%=rnd%>('<%=Common.getBahasaConfigJS("Isi nominal minimal Rp 10.000 dan pilih saluran pembayaran.")%>', false);
            return;
        }

        const channelCode = channelParts[0];
        const admin = Number(channelParts[1] || 0);
        const button = document.getElementById('btnTopupLangsung<%=rnd%>');
        button.disabled = true;
        button.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Memproses...")%>';
        tampilkanPesanTopupLangsung<%=rnd%>('', false);

        const endpoint = urlTopupService<%=rnd%> + '&selectedChannelCode=' + encodeURIComponent(channelCode) + '&cara_pembayaran_id=' + encodeURIComponent(idCaraBayarTopup<%=rnd%>);
        const payload = {
            action: 'topup',
            id_member: idMemberAktif<%=rnd%>,
            nominal: nominal,
            selectedChannelCode: channelCode,
            cara_pembayaran_id: idCaraBayarTopup<%=rnd%>,
            admin: admin
        };

        try {
            const response = await fetch(endpoint, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify(payload)
            });
            const result = await response.json();
            let paymentUrl = null;
            if (result.data && Array.isArray(result.data) && result.data.length > 0) paymentUrl = result.data[0].link;
            if (!paymentUrl && result.url) paymentUrl = result.url;
            if (response.ok && (result.status === '00' || result.status === 'success') && paymentUrl) {
                tampilkanPesanTopupLangsung<%=rnd%>('<%=Common.getBahasaConfigJS("Tagihan berhasil dibuat. Mengalihkan ke pembayaran...")%>', true);
                window.location.href = paymentUrl;
                return;
            }
            tampilkanPesanTopupLangsung<%=rnd%>(result.message || result.description || '<%=Common.getBahasaConfigJS("Tagihan pembayaran gagal dibuat.")%>', false);
        } catch (error) {
            console.error('Topup API error:', error);
            tampilkanPesanTopupLangsung<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal terhubung ke layanan pembayaran.")%>', false);
        } finally {
            button.disabled = false;
            button.innerHTML = '<i class="fas fa-lock me-2"></i><%=Common.getBahasaConfig("Buat Tagihan Pembayaran")%>';
        }
    };

    // ==========================================
    // PENGAMBILAN DATA (PRODUK, SALDO, DISKON, TOKO)
    // ==========================================
    
    const loadSisaCashback<%=rnd%> = async () => {
        // Panggil dan Hitung Sisa Cashback Member
        const sqlCb = "SELECT " +
                      "COALESCE((SELECT SUM(p.cashback) FROM koperasi.pembelian p LEFT JOIN koperasi.pembelian_anggota_koperasi pak ON p.pembelian_anggota_koperasi = pak.id WHERE pak.anggota_koperasi = " + idMemberAktif<%=rnd%> + "), 0) - " +
                      "COALESCE((SELECT SUM(pd.nominal_cair) FROM koperasi.pencairan_diskon pd WHERE pd.anggota_koperasi = " + idMemberAktif<%=rnd%> + " AND pd.status = 'BERHASIL'), 0) " +
                      "AS sisa_cashback";
        const resCb = await fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlCb });
        if (resCb.data && resCb.data.length > 0) {
            const sisaCb = parseFloat(resCb.data[0].sisa_cashback || 0);
            const elCb = document.getElementById('labelSisaCashback<%=rnd%>');
            if(elCb) elCb.innerText = formatRp<%=rnd%>(sisaCb);
        }
    };

    const loadSaldoMember<%=rnd%> = async (refresh) => {
        if(!isLogin<%=rnd%>) return;
        
        // Panggil Tabungan Biasa
        const payload = { action: "tabungan", id_member: idMemberAktif<%=rnd%>, refresh: refresh };
        const res = await fetchDataAPI<%=rnd%>(payload);
        if (res && res.data !== undefined) {
            saldoMember<%=rnd%> = parseFloat(res.data);
            const elSaldo = document.getElementById('labelSaldoUtama<%=rnd%>');
            if (elSaldo) elSaldo.innerText = formatRp<%=rnd%>(saldoMember<%=rnd%>);
        }
        
        loadSisaCashback<%=rnd%>();
    };

    const loadTokoKatalog<%=rnd%> = async () => {
        const sql = "SELECT id, nama FROM koperasi.toko WHERE aktif = true ORDER BY nama ASC";
        const res = await fetchDataAPI<%=rnd%>({action: "sql", sql: sql});
        let opt = '<option value=""><%=Common.getBahasaConfig("-- Pilih Pedagang / Toko Terlebih Dahulu --")%></option>';
        if (res.data) {
            res.data.forEach(t => { 
                opt += '<option value="' + t.id + '">' + t.nama + '</option>'; 
            });
        }
        document.getElementById('selectTokoKatalog<%=rnd%>').innerHTML = opt;
    };

    const loadMetodePembayaran<%=rnd%> = async () => {
        if(!isLogin<%=rnd%>) return;
        
        // Menggunakan daftar_cara_pembayaran_yang_boleh_di_pilih dari JenisAnggotaKoperasi (via Subquery)
        const sql = "SELECT cpk.id, cpk.nama, cpk.manual " +
                    "FROM koperasi.cara_pembayaran_koperasi cpk " +
                    "WHERE cpk.aktif = true AND cpk.online = true " +
                    "AND (SELECT jak.daftar_cara_pembayaran_yang_boleh_di_pilih FROM koperasi.jenis_anggota_koperasi jak WHERE jak.id = <%=idJenisAnggota%>) LIKE '%,' || cpk.id || ',%' " +
                    "ORDER BY cpk.nama ASC";

        const res = await fetchDataAPI<%=rnd%>({ action: "sql", sql: sql, log:"true" });
        
        let optHtml = '';
        
        if (!res.data || res.data.length !== 1) {
            optHtml += '<option value=""><%=Common.getBahasaConfig("-- Pilih Saluran Pembayaran --")%></option>';
        }
        
        if(res.data && res.data.length > 0) {
            res.data.forEach(item => { 
                const isSelected = (res.data.length === 1) ? 'selected' : '';
                const isManual = item.manual === true || item.manual === 't' || item.manual === 'true' ? 'true' : 'false';
                optHtml += '<option value="' + item.id + '" data-manual="' + isManual + '" ' + isSelected + '>' + item.nama + '</option>'; 
            });
        } else {
            optHtml = '<option value=""><%=Common.getBahasaConfig("-- Tidak Ada Metode Pembayaran --")%></option>';
        }
        const selectCara = document.getElementById('selectCaraBayar<%=rnd%>');
        if(selectCara) selectCara.innerHTML = optHtml;
    };

    const handleTokoChange<%=rnd%> = async () => {
        currentPage<%=rnd%> = 1;
        loadKatalogProduk<%=rnd%>();
        await updateUsageDiskonMember<%=rnd%>();
        recalculateCart<%=rnd%>();
    };

    const updateUsageDiskonMember<%=rnd%> = async () => {
        const idMember = idMemberAktif<%=rnd%>;
        const elToko = document.getElementById('selectTokoKatalog<%=rnd%>');
        const currentToko = elToko ? elToko.value : "";
        
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
                    const resUsage = await fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlUsage });
                    rule.terpakai_hari_ini = (resUsage.data && resUsage.data.length > 0) ? parseFloat(resUsage.data[0].terpakai) : 0;
                } else {
                    rule.terpakai_hari_ini = 0;
                }
            } else {
                rule.terpakai_hari_ini = 0;
            }
            rule.terpakai_di_keranjang = 0; // State bantuan lokal
        }
    };

    const loadAturanDiskon<%=rnd%> = async () => {
        // Jangan ubah type date jadi _iso langsung di sql, biar action:"sql" yang tambahin otomatis
        const sqlAturan = "SELECT id,produk,toko,berlaku_semua_member,jenis_anggota,tipe_anggota,persentase,maksimal_potongan,nominal,potongan_langsung,berlaku_per_hari_dan_per_toko,tanggal_mulai,tanggal_selesai FROM (" +
                "SELECT id,produk,toko,berlaku_semua_member,jenis_anggota,tipe_anggota,persentase,maksimal_potongan,nominal,potongan_langsung,berlaku_per_hari_dan_per_toko,tanggal_mulai,tanggal_selesai,CASE WHEN produk IS NULL THEN 2 ELSE 0 END prioritas FROM koperasi.aturan_diskon WHERE aktif=true " +
                "UNION ALL SELECT NULL::bigint,d.produk,g.toko,COALESCE(g.berlaku_semua_member,true),g.jenis_anggota,g.tipe_anggota,COALESCE(g.persentase,0),COALESCE(g.maksimal_potongan,0),COALESCE(g.nominal,0),COALESCE(g.potongan_langsung,true),false,g.tanggal_mulai,g.tanggal_selesai,1 FROM koperasi.grup_aturan_diskon g JOIN koperasi.grup_aturan_diskon_detail d ON d.grup_aturan_diskon=g.id WHERE g.aktif=true AND d.aktif=true) semua_aturan ORDER BY prioritas,id NULLS LAST";
        const res = await fetchDataAPI<%=rnd%>({ action: "sql", sql: sqlAturan });
        if(res.data) {
            arrAturanDiskon<%=rnd%> = res.data;
            if (isLogin<%=rnd%>) {
                await updateUsageDiskonMember<%=rnd%>();
            }
        }
    };

    const checkAndLoadImage<%=rnd%> = async (pId) => {
        try {
            const reqObj = { 
                action: "file", 
                class: "ais.database.model.file.LampiranLain", 
                ref: pId.toString(), 
                jenis: "ais.database.model.inventory.Produk",
                refresh: "false"
            };
            const dataResponse = await fetchDataAPI<%=rnd%>(reqObj);
            
            if(dataResponse.status === '00' && dataResponse.data && dataResponse.data.nama){ 
                if(dataResponse.data.nama !== 'administrator-icon_default.png') {
                    const imgUrl = "<%=Common.ROOT%>/Data?action=file&class=ais.database.model.file.LampiranLain&ref=" + pId + "&jenis=ais.database.model.inventory.Produk&render=true";
                    const wrapper = document.getElementById('prod-icon-wrapper-<%=rnd%>-' + pId);
                    if(wrapper) {
                        wrapper.classList.remove('p-2', 'bg-opacity-10');
                        wrapper.style.padding = '0px';
                        wrapper.innerHTML = '<img src="' + imgUrl + '" class="w-100 h-100 object-fit-cover" alt="<%=Common.getBahasaConfig("Gambar Produk")%>">';
                    }
                }
            }
        } catch (error) { console.error(error); }
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

        const elToko = document.getElementById('selectTokoKatalog<%=rnd%>');
        const idToko = elToko ? elToko.value : "";

        if(!idToko || idToko === "") {
            grid.innerHTML = '<div class="col-12 text-center py-5 text-muted"><i class="fas fa-store-slash fa-3x mb-3 opacity-25"></i><br><span class="fw-medium"><%=Common.getBahasaConfig("Silakan pilih pedagang/toko terlebih dahulu untuk melihat produk.")%></span></div>';
            if(pagingContainer) pagingContainer.classList.add('d-none');
            const lblTotal = document.getElementById('totalKatalog<%=rnd%>');
            if(lblTotal) lblTotal.innerText = '0 <%=Common.getBahasaConfig("Produk")%>';
            return;
        }
        
        const searchInput = document.getElementById('searchKatalog<%=rnd%>');
        const keyword = searchInput ? searchInput.value.trim().replace(/'/g, "''") : "";
        
        grid.innerHTML = '<div class="col-12 text-center py-4"><div class="spinner-border text-primary spinner-border-sm"></div><div class="mt-2 small text-muted"><%=Common.getBahasaConfig("Sistem sedang memuat katalog, mohon tunggu...")%></div></div>';
        if(pagingContainer) pagingContainer.classList.add('d-none');

        let filterSql = "WHERE p.aktif = true AND p.toko = " + idToko + " ";
        if (keyword !== "") {
            filterSql += " AND (p.nama ILIKE '%" + keyword + "%' OR p.kode = '" + keyword + "') ";
        }

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

            const lblTotal = document.getElementById('totalKatalog<%=rnd%>');
            if(lblTotal) lblTotal.innerText = totalRecords<%=rnd%> + ' <%=Common.getBahasaConfig("Produk")%>';

            if (produkList.length === 0) {
                grid.innerHTML = '<div class="col-12 text-center py-4 text-muted"><i class="fas fa-box-open fa-3x mb-3 opacity-25"></i><br><span class="fw-medium"><%=Common.getBahasaConfig("Tidak ada produk yang sesuai dengan pencarian Anda.")%></span></div>';
                return;
            }

            let htmlGrid = '';
            const icons = ['fa-box', 'fa-coffee', 'fa-hamburger', 'fa-ice-cream', 'fa-pizza-slice'];
            const colors = ['primary', 'success', 'danger', 'warning', 'info'];

            produkList.forEach((p, idx) => {
                const icon = icons[idx % icons.length];
                const color = colors[idx % colors.length];
                const pId = p.id;
                
                // Mencegah error Javascript karena tanda petik satu (single quote) pada string
                const pKode = p.kode || '';
                const pKodeEsc = pKode.replace(/'/g, "\\'").replace(/"/g, '&quot;');
                const pNamaDisp = p.nama;
                const pNamaEsc = p.nama.replace(/'/g, "\\'").replace(/"/g, '&quot;');
                const pHarga = parseFloat(p.hargajual || 0);
                const pIdToko = p.id_toko;
                const nmTokoDisp = p.nama_toko || '<%=Common.getBahasaConfigJS("Koperasi Utama")%>';
                const nmTokoEsc = nmTokoDisp.replace(/'/g, "\\'").replace(/"/g, '&quot;');

                htmlGrid += '<div class="col-12 col-sm-6 col-xxl-6">' +
                        '<div class="card product-card-<%=rnd%> h-100 shadow-sm">' +
                            '<div class="d-flex align-items-center p-2">' +
                                '<div id="prod-icon-wrapper-<%=rnd%>-' + pId + '" class="bg-' + color + ' bg-opacity-10 text-' + color + ' rounded-3 d-flex align-items-center justify-content-center overflow-hidden flex-shrink-0" style="width: 70px; height: 70px;">' +
                                    '<i class="fas ' + icon + ' fa-lg"></i>' +
                                '</div>' +
                                '<div class="ms-2 flex-grow-1 min-width-0">' +
                                    '<small class="text-muted fw-bold d-block text-truncate" style="font-size:10px;"><i class="fas fa-store text-primary me-1"></i>' + nmTokoDisp + '</small>' +
                                    '<div class="fw-bolder text-dark text-truncate" title="' + pNamaDisp.replace(/"/g, '&quot;') + '" style="font-size: 0.95rem;">' + pNamaDisp + '</div>' +
                                    '<div class="text-success fw-bold" style="font-size: 0.85rem;">' + formatRp<%=rnd%>(pHarga) + '</div>' +
                                '</div>' +
                            '</div>' +
                            '<div class="card-footer bg-transparent border-top-0 pt-0 pb-2 px-2">' +
                                '<button class="btn btn-outline-primary btn-sm w-100 fw-bold rounded-pill shadow-sm" style="font-size: 0.75rem;" onclick="addToCart<%=rnd%>(' + pId + ', \'' + pKodeEsc + '\', \'' + pNamaEsc + '\', ' + pHarga + ', ' + pIdToko + ', \'' + nmTokoEsc + '\')">' +
                                    '<i class="fas fa-cart-plus me-1"></i><%=Common.getBahasaConfig("Tambahkan ke Keranjang")%>' +
                                '</button>' +
                            '</div>' +
                        '</div>' +
                    '</div>';
            });
            
            grid.innerHTML = htmlGrid;
            produkList.forEach(p => checkAndLoadImage<%=rnd%>(p.id));

            if (totalRecords<%=rnd%> > limitPerPage<%=rnd%>) {
                const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
                const lblPage = document.getElementById('lblCurrentPage<%=rnd%>');
                const lblTotalPage = document.getElementById('lblTotalPage<%=rnd%>');
                const btnPrev = document.getElementById('btnPrevPage<%=rnd%>');
                const btnNext = document.getElementById('btnNextPage<%=rnd%>');
                
                if(lblPage) lblPage.innerText = currentPage<%=rnd%>;
                if(lblTotalPage) lblTotalPage.innerText = totalPages;
                if(btnPrev) btnPrev.disabled = (currentPage<%=rnd%> === 1);
                if(btnNext) btnNext.disabled = (currentPage<%=rnd%> === totalPages);
                
                if(pagingContainer) pagingContainer.classList.remove('d-none');
            }

        } catch (error) {
            console.error("Gagal load katalog:", error);
            grid.innerHTML = '<div class="col-12 text-center text-danger py-4"><i class="fas fa-exclamation-triangle mb-2"></i><br><small><%=Common.getBahasaConfig("Sistem gagal menarik data katalog. Silakan muat ulang halaman.")%></small></div>';
        }
    };


    const evaluateDiscount<%=rnd%> = (item) => {
        if(!isLogin<%=rnd%>) return; 

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

            // Evaluasi Member
            const isAllMember = (rule.berlaku_semua_member === 'true' || rule.berlaku_semua_member === true || rule.berlaku_semua_member === 't');
            if (!isAllMember) {
                if (!selectedMember) continue; // Butuh member spesifik tapi kasir belum pilih member
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
        // Reset akumulator keranjang setiap kali dihitung ulang
        if(arrAturanDiskon<%=rnd%>) {
            arrAturanDiskon<%=rnd%>.forEach(r => r.terpakai_di_keranjang = 0);
        }
        cart<%=rnd%>.forEach(item => {
            evaluateDiscount<%=rnd%>(item);
        });
        renderCart<%=rnd%>();
    };

    const addToCart<%=rnd%> = async (id, kode, nama, harga, idToko, namaToko) => {
        if (!isLogin<%=rnd%>) {
            showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Silakan masuk (login) terlebih dahulu untuk mulai berbelanja.")%>', 'bg-warning text-dark');
            return;
        }

        // Segarkan saldo saat ini dari server untuk memastikan datanya mutakhir setiap memilih barang
        await loadSaldoMember<%=rnd%>(true);

        const existingItem = cart<%=rnd%>.find(item => item.id === id);
        if (existingItem) {
            existingItem.jumlah += 1;
        } else {
            let newItem = { id: id, kode: kode, nama: nama, harga: harga, jumlah: 1, idToko: idToko, namaToko: namaToko, diskon: 0, cashback: 0, aturanDiskon: null, berlakuPerHariDanPerToko: false };
            cart<%=rnd%>.push(newItem);
        }
        recalculateCart<%=rnd%>();
    };

    const updateQty<%=rnd%> = async (index, delta) => {
        // Segarkan saldo saat ini dari server untuk memastikan datanya mutakhir
        await loadSaldoMember<%=rnd%>(true);
        
        cart<%=rnd%>[index].jumlah += delta;
        if (cart<%=rnd%>[index].jumlah <= 0) {
            cart<%=rnd%>.splice(index, 1);
        }
        recalculateCart<%=rnd%>();
    };

    // Fungsi baru untuk menangani perubahan jumlah dari input ketikan (type="number")
    const setQty<%=rnd%> = async (index, val) => {
        let newVal = parseInt(val);
        // Segarkan saldo dari server agar mutakhir
        await loadSaldoMember<%=rnd%>(true);
        
        if (isNaN(newVal) || newVal <= 0) {
            // Jika diketik 0 atau negatif/bukan angka, hapus item dari keranjang
            cart<%=rnd%>.splice(index, 1);
        } else {
            cart<%=rnd%>[index].jumlah = newVal;
        }
        recalculateCart<%=rnd%>();
    };

    const kosongkanKeranjang<%=rnd%> = () => {
        cart<%=rnd%> = [];
        recalculateCart<%=rnd%>();
    };

    const renderCart<%=rnd%> = () => {
        const container = document.getElementById('cartContainer<%=rnd%>');
        const badgeCount = document.getElementById('badgeCartCount<%=rnd%>');
        const floatingBadge = document.getElementById('floatingCartCount<%=rnd%>');
        const elTokoSelect = document.getElementById('selectTokoKatalog<%=rnd%>');
        
        if(!container) return;

        let subtotal = 0;
        let totalDiskon = 0;
        let totalCashback = 0;
        let totalItems = 0;

        if (cart<%=rnd%>.length === 0) {
            container.innerHTML = '<tr><td class="text-center text-muted py-5"><i class="fas fa-cart-arrow-down fa-3x mb-3 opacity-25 text-secondary"></i><br><span class="fw-medium"><%=Common.getBahasaConfig("Keranjang Anda masih kosong.")%></span></td></tr>';
            grandTotalValue<%=rnd%> = 0;
            if(badgeCount) badgeCount.innerText = "0";
            if(floatingBadge) floatingBadge.innerText = "0";
            
            // Re-enable Toko Select
            if(elTokoSelect) elTokoSelect.disabled = false;
        } else {
            // MULTI-TENANT: pemilihan toko sengaja TIDAK dikunci, agar member dapat
            // menambah produk dari beberapa tenant sekaligus ke dalam satu keranjang.
            if(elTokoSelect) elTokoSelect.disabled = false;

            const cartByToko = cart<%=rnd%>.reduce((acc, item) => {
                if (!acc[item.idToko]) acc[item.idToko] = { namaToko: item.namaToko, items: [] };
                acc[item.idToko].items.push(item);
                return acc;
            }, {});

            let htmlCart = '';
            
            for (const tId in cartByToko) {
                const toko = cartByToko[tId];
                htmlCart += '<tr><td colspan="3" class="px-2 py-2 border-bottom" style="background-color: #eef4ff;"><div class="text-center fw-bold text-primary text-uppercase" style="font-size: 11px; letter-spacing: .3px;"><i class="fas fa-store me-1"></i>' + toko.namaToko + '</div></td></tr>';
                
                toko.items.forEach((item) => {
                    const idx = cart<%=rnd%>.findIndex(i => i.id === item.id); 
                    const itemTotal = (item.harga * item.jumlah) - item.diskon;
                    subtotal += (item.harga * item.jumlah);
                    totalDiskon += item.diskon;
                    totalCashback += item.cashback;
                    totalItems += item.jumlah;
                    
                    let notePromo = '';
                    if(item.diskon > 0) notePromo += '<span class="badge bg-danger text-white rounded-pill mt-1 px-1 d-inline-block" style="font-size: 9px;"><i class="fas fa-cut me-1"></i>Potong ' + formatRp<%=rnd%>(item.diskon) + '</span><br>';
                    
                    <%if(aktifkanTampilCashback){ %>
                    if(item.cashback > 0) notePromo += '<span class="badge bg-info text-dark rounded-pill mt-1 px-1 d-inline-block" style="font-size: 9px;"><i class="fas fa-wallet me-1"></i><%=istilahCashback%> ' + formatRp<%=rnd%>(item.cashback) + '</span>';
					<% } %>
                    htmlCart += '<tr class="border-bottom">' +
                            '<td class="px-2 py-2">' +
                                '<div class="fw-bold text-dark lh-sm" style="font-size: 0.85rem;">' + item.nama + '</div>' + 
                                '<small class="text-muted fw-semibold" style="font-size: 0.75rem;">' + formatRp<%=rnd%>(item.harga) + '</small><br>' +
                                notePromo +
                            '</td>' +
                            '<td style="width: 90px;" class="px-1 py-2">' +
                                '<div class="input-group input-group-sm shadow-sm rounded-pill overflow-hidden border">' +
                                    '<button class="btn btn-light text-primary border-0 px-2 fw-bold py-1" type="button" onclick="updateQty<%=rnd%>(' + idx + ', -1)">-</button>' +
                                    '<input type="number" min="1" class="form-control text-center px-0 border-0 fw-bold bg-white py-1 no-spinners" style="font-size: 0.8rem;" value="' + item.jumlah + '" onchange="setQty<%=rnd%>(' + idx + ', this.value)">' +
                                    '<button class="btn btn-light text-primary border-0 px-2 fw-bold py-1" type="button" onclick="updateQty<%=rnd%>(' + idx + ', 1)">+</button>' +
                                '</div>' +
                            '</td>' +
                            '<td class="text-end fw-bolder text-success px-2 py-2" style="font-size: 0.85rem;">' + formatRp<%=rnd%>(itemTotal) + '</td>' +
                        '</tr>';
                });
            }

            container.innerHTML = htmlCart;
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
        if(elCashback) elCashback.innerText = "+ " + formatRp<%=rnd%>(totalCashback);
        if(elGrandTotal) elGrandTotal.innerText = formatRp<%=rnd%>(grandTotalValue<%=rnd%>);
    };

    // ==========================================
    // LOGIKA PEMBAYARAN (CHECKOUT MULTI-TOKO)
    // ==========================================
    const initiateCheckout<%=rnd%> = async () => {
        if (cart<%=rnd%>.length === 0) {
            showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Keranjang belanja masih kosong! Silakan tambah produk terlebih dahulu.")%>', 'bg-warning text-dark');
            return;
        }

        const btnBayar = document.getElementById('btnCheckout<%=rnd%>');
        if (!btnBayar) return;
        
        const oriBtnHtml = btnBayar.innerHTML;
        btnBayar.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Memproses Transaksi...")%>';
        btnBayar.disabled = true;

        // Tarik saldo member paling mutakhir dari server sebelum validasi
        await loadSaldoMember<%=rnd%>(true);

        const cboCaraBayar = document.getElementById('selectCaraBayar<%=rnd%>');
        if (!cboCaraBayar || cboCaraBayar.value === "") {
            showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Silakan pilih Saluran Pembayaran di keranjang Anda.")%>', 'bg-warning text-dark');
            if(cboCaraBayar) {
                cboCaraBayar.focus();
                cboCaraBayar.classList.add('is-invalid');
                setTimeout(() => cboCaraBayar.classList.remove('is-invalid'), 3000);
            }
            btnBayar.innerHTML = oriBtnHtml;
            btnBayar.disabled = false;
            return;
        }

        const optCaraBayar = cboCaraBayar.options[cboCaraBayar.selectedIndex];
        const idCaraBayar = cboCaraBayar.value;
        const isManual = optCaraBayar ? (optCaraBayar.getAttribute('data-manual') === 'true') : false;

        // Jika bukan pembayaran manual, pastikan saldo cukup & memenuhi kriteria minimal deposit
        if (!isManual) {
            if (saldoMember<%=rnd%> < grandTotalValue<%=rnd%>) {
                showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Maaf, Saldo Anda tidak mencukupi untuk transaksi ini. Silakan isi ulang saldo Anda terlebih dahulu.")%>', 'bg-danger text-white');
                btnBayar.innerHTML = oriBtnHtml;
                btnBayar.disabled = false;
                return;
            }
            
            // Validasi Minimal Saldo Mengendap
            if ((saldoMember<%=rnd%> - grandTotalValue<%=rnd%>) < minimalDeposit<%=rnd%>) {
                showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Transaksi gagal! Sisa saldo Anda setelah transaksi kurang dari batas saldo mengendap yang diizinkan (Rp ")%>' + formatRp<%=rnd%>(minimalDeposit<%=rnd%>) + ').', 'bg-danger text-white');
                btnBayar.innerHTML = oriBtnHtml;
                btnBayar.disabled = false;
                return;
            }
        }

        // VALIDASI MEJA KANTIN BERDASARKAN KONFIGURASI
        if (reqPilihMeja<%=rnd%>) {
            if (!isBawaPulang<%=rnd%> && idMejaTerpilih<%=rnd%> === null) {
                showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Silakan Scan QR Meja tempat Anda berada, atau pilih opsi Bawa Pulang.")%>', 'bg-danger text-white');
                // Menyorot area meja agar member sadar
                const btnMeja = document.getElementById('btnPilihMeja<%=rnd%>');
                if (btnMeja) {
                    btnMeja.classList.remove('btn-outline-info');
                    btnMeja.classList.add('btn-outline-danger');
                    setTimeout(() => {
                        btnMeja.classList.remove('btn-outline-danger');
                        btnMeja.classList.add('btn-outline-info');
                    }, 3000);
                }
                btnBayar.innerHTML = oriBtnHtml;
                btnBayar.disabled = false;
                return;
            }
        }

        const curWaktu = getFormattedDate<%=rnd%>();

        // Ambil catatan/komentar opsional yang diisi member (di bawah Metode Pembayaran)
        const elKomentar = document.getElementById('inputKomentar<%=rnd%>');
        const komentarKeranjang = elKomentar ? elKomentar.value.trim() : "";

        const groupedCart = cart<%=rnd%>.reduce((acc, item) => {
            if (!acc[item.idToko]) {
                acc[item.idToko] = [];
            }
            acc[item.idToko].push(item);
            return acc;
        }, {});

        let isAllSuccess = true;
        let errorMessage = "";

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

            const kodeTransaksi = "ONL-" + generateUniqueCode50();

            // Ubah action menjadi draft_bayar jika manual
            const targetAction = isManual ? "draft_bayar" : "bayar";

            const payload = {
                action: targetAction,
                kodeUnik: kodeTransaksi,
                idToko: tId,
                waktu: curWaktu,
                id_member: idMemberAktif<%=rnd%>,
                kanalCheckout: "anggota_online",
                caraBayar: idCaraBayar,
                keterangan: komentarKeranjang,
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

            // Injeksi parameter Meja Kantin
            if (reqPilihMeja<%=rnd%> && !isBawaPulang<%=rnd%> && idMejaTerpilih<%=rnd%> !== null) {
                payload.mejaKantin = idMejaTerpilih<%=rnd%>;
            }

            try {
                const res = await fetchDataAPI<%=rnd%>(payload);
                if (res.status !== '00' && res.status !== 'success') {
                    isAllSuccess = false;
                    errorMessage = res.description || "<%=Common.getBahasaConfig("Gagal memproses pembayaran untuk salah satu toko.")%>";
                    break; 
                }
            } catch (error) {
                console.error("Payment Error:", error);
                isAllSuccess = false;
                errorMessage = "<%=Common.getBahasaConfig("Koneksi jaringan terputus saat memproses transaksi.")%>";
                break;
            }
        }

        if (isAllSuccess) {
            let infoSuksesHtml = '';
            if (isManual) {
                infoSuksesHtml = '<p class="text-muted small"><%=Common.getBahasaConfig("Pesanan telah dibuat. Silakan selesaikan pembayaran Anda di Kasir/Toko yang bersangkutan.")%></p>';
            } else {
                infoSuksesHtml = '<p class="text-muted small"><%=Common.getBahasaConfig("Pesanan Anda telah diteruskan ke pihak penyedia dan saldo Anda telah disesuaikan.")%></p>';
            }

            const htmlSukses = '<div class="card-body p-4 p-md-5 text-center animate__animated animate__zoomIn">' +
                               '<i class="fas fa-check-circle fa-4x text-success mb-3"></i>' +
                               '<h5 class="fw-bolder text-dark mb-2"><%=Common.getBahasaConfig("Transaksi Berhasil!")%></h5>' +
                               infoSuksesHtml +
                               '<div class="bg-light p-3 rounded-4 d-inline-block mt-2 mb-4 text-center shadow-sm border w-100" style="max-width: 250px;">' +
                                    '<small class="text-muted d-block mb-1 fw-bold"><%=Common.getBahasaConfig("Total Tagihan")%></small>' + 
                                    '<h4 class="fw-bolder text-success mb-0">' + formatRp<%=rnd%>(grandTotalValue<%=rnd%>) + '</h4>' +
                               '</div><br>' +
                               '<button class="btn btn-primary rounded-pill px-5 fw-bold shadow-sm" onclick="location.reload()"><i class="fas fa-home me-2"></i><%=Common.getBahasaConfig("Selesai & Ke Beranda")%></button>' +
                               '</div>';
            
            // Ganti Container
            const wrapper = document.getElementById('mainCartWrapper<%=rnd%>');
            if (wrapper) wrapper.classList.add('d-none');
            
            const resultContainer = document.getElementById('checkoutResultContainer<%=rnd%>');
            if (resultContainer) {
                resultContainer.innerHTML = htmlSukses;
                resultContainer.classList.remove('d-none');
            }
            
            const floatingBtn = document.querySelector('.floating-cart-btn-<%=rnd%>');
            if (floatingBtn) floatingBtn.style.display = 'none';
            
            loadSaldoMember<%=rnd%>(true);
            kosongkanKeranjang<%=rnd%>();

        } else {
            showToastUI<%=rnd%>(errorMessage, 'bg-danger text-white');
        }

        if(document.body.contains(btnBayar)) {
            btnBayar.innerHTML = oriBtnHtml;
            btnBayar.disabled = false;
        }
    };

    // ==========================================
    // INISIALISASI HALAMAN
    // ==========================================
    document.addEventListener("DOMContentLoaded", async () => {
        injectModals<%=rnd%>();

        // Jam berjalan pada detail tanggal di atas keranjang belanja
        const elJamHariIni = document.getElementById('jamHariIni<%=rnd%>');
        if (elJamHariIni) {
            const tickJamHariIni = () => {
                const n = new Date();
                const p = (x) => x.toString().padStart(2, '0');
                elJamHariIni.innerText = p(n.getHours()) + ':' + p(n.getMinutes()) + ':' + p(n.getSeconds());
            };
            tickJamHariIni();
            setInterval(tickJamHariIni, 1000);
        }

        await loadTokoKatalog<%=rnd%>(); // Muat dropdown toko terlebih dahulu
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
            loadSaldoMember<%=rnd%>(false);
            loadMetodePembayaran<%=rnd%>();
        }

        // ==========================================
        // OBSERVER: HIDE FLOATING CART BUTTON SAAT SCROLL KE KERANJANG
        // ==========================================
        const cartSectionEl = document.getElementById('cartSection<%=rnd%>');
        const floatingCartBtnEl = document.querySelector('.floating-cart-btn-<%=rnd%>');
        
        if (cartSectionEl && floatingCartBtnEl) {
            const observer = new IntersectionObserver((entries) => {
                if (entries[0].isIntersecting) {
                    floatingCartBtnEl.style.opacity = '0';
                    floatingCartBtnEl.style.visibility = 'hidden';
                } else {
                    floatingCartBtnEl.style.opacity = '1';
                    floatingCartBtnEl.style.visibility = 'visible';
                }
            }, { threshold: 0.1 }); 
            observer.observe(cartSectionEl);
        }
    });
</script>
