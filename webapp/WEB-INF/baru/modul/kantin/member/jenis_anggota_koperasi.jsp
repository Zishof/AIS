<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// ==========================================
// 1. Pengecekan Sesi & Keamanan Akses
// ==========================================
Tbmuser tbmuser = null;
Toko toko = null;
boolean isAdmin = false;

try {
    tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
        return;
    }
    
    Pedagang pedagang = tbmuser.getPedagang();
    toko = pedagang == null ? null : pedagang.getToko();
    isAdmin = (toko == null);

} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/member/jenis_anggota_koperasi.jsp:27");
} finally {
    // Memastikan koneksi / session hibernate tertutup agar memori efisien dan tidak bocor
    HibernateUtil.closeSession();
}

String rnd = Common.getGeneratedBarCode(7);
%>

<div class="container-fluid py-3 py-md-4" style="background-color: #f4f7f6; min-height: 100vh;">

    <!-- TAMPILAN DAFTAR DATA -->
    <div id="viewList<%=rnd%>" class="animate__animated animate__fadeIn">
        
        <!-- HEADER KONTROL -->
        <div class="d-flex flex-column flex-md-row justify-content-between align-items-center mb-4 gap-3">
            <div class="text-center text-md-start">
                <h4 class="fw-bolder text-primary mb-1">
                    <i class="fas fa-tags me-2"></i><%=Common.getBahasaConfig("Manajemen Jenis Anggota")%>
                </h4>
                <p class="text-muted small mb-0"><%=Common.getBahasaConfig("Kelola data induk klasifikasi, hak akses, dan pengaturan kategori anggota koperasi.")%></p>
            </div>
            
            <div class="d-flex flex-wrap gap-2 justify-content-center">
                <button class="btn btn-light rounded-pill px-4 shadow-sm fw-bold border text-primary" onclick="resetAndLoadData<%=rnd%>()" title="<%=Common.getBahasaConfig("Muat Ulang Data")%>">
                    <i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Segarkan")%>
                </button>
                
                <% if (isAdmin) { %>
                <button class="btn btn-primary rounded-pill px-4 shadow-sm fw-bold text-nowrap" onclick="bukaFormTambah<%=rnd%>()">
                    <i class="fas fa-plus-circle me-1"></i><%=Common.getBahasaConfig("Tambah Jenis Baru")%>
                </button>
                <% } %>
            </div>
        </div>

        <!-- AREA PENYARINGAN (FILTER) -->
        <div class="card border-0 shadow-sm rounded-4 mb-4 border-top border-primary border-3">
            <div class="card-body p-3 p-md-4">
                <div class="row g-3 align-items-end">
                    <div class="col-md-10">
                        <label class="form-label small fw-bold text-secondary mb-1"><i class="fas fa-search me-1 text-primary"></i><%=Common.getBahasaConfig("Pencarian Data")%></label>
                        <div class="input-group shadow-sm">
                            <span class="input-group-text bg-light border-end-0 text-muted"><i class="fas fa-keyboard"></i></span>
                            <input type="text" class="form-control border-start-0 bg-light fw-medium" id="searchJenis<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik Kode atau Nama Jenis Anggota...")%>">
                        </div>
                    </div>

                    <div class="col-md-2 d-grid">
                        <button class="btn btn-primary rounded-3 shadow-sm fw-bold" onclick="resetAndLoadData<%=rnd%>()">
                            <i class="fas fa-filter me-2"></i><%=Common.getBahasaConfig("Saring")%>
                        </button>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- AREA TABEL DATA -->
        <div class="card border-0 shadow-sm rounded-4 overflow-hidden">
            <div class="card-body p-0 pt-1">
                <div class="table-responsive px-3 py-2" style="min-height: 400px;">
                    <table class="table table-hover table-striped align-middle mb-0">
                        <thead class="table-light text-secondary">
                            <tr>
                                <th class="fw-bold text-uppercase small py-3 text-center" style="width: 50px;">#</th>
                                <th class="text-start fw-bold text-uppercase small" style="width: 120px;"><i class="fas fa-barcode me-1"></i><%=Common.getBahasaConfig("Kode")%></th>
                                <th class="text-start fw-bold text-uppercase small"><i class="fas fa-font me-1"></i><%=Common.getBahasaConfig("Jenis & Istilah (Saldo/CB)")%></th>
                                <th class="text-start fw-bold text-uppercase small"><i class="fas fa-align-left me-1"></i><%=Common.getBahasaConfig("Keterangan")%></th>
                                <th class="fw-bold text-uppercase small text-center" style="width: 110px;"><i class="fas fa-wallet me-1"></i><%=Common.getBahasaConfig("Tampil Saldo")%></th>
                                <th class="fw-bold text-uppercase small text-center" style="width: 110px;"><i class="fas fa-piggy-bank me-1"></i><%=Common.getBahasaConfig("Tampil CB")%></th>
                                <th class="fw-bold text-uppercase small text-center" style="width: 110px;"><i class="fas fa-check-square me-1"></i><%=Common.getBahasaConfig("Dipilih")%></th>
                                <th class="fw-bold text-uppercase small text-center" style="width: 130px;"><i class="fas fa-money-bill-wave me-1"></i><%=Common.getBahasaConfig("Boleh Topup")%></th>
                                <th class="fw-bold text-uppercase small text-center" style="width: 82px;"><i class="fas fa-key me-1"></i>PIN</th>
                                <th class="fw-bold text-uppercase small text-center" style="width: 92px;"><i class="fas fa-camera me-1"></i><%=Common.getBahasaConfig("Wajah")%></th>
                                <th class="fw-bold text-uppercase small text-center" style="width: 110px;"><i class="fas fa-fingerprint me-1"></i>Fingerprint</th>
                                <th class="fw-bold text-uppercase small text-center" style="width: 100px;"><i class="fas fa-toggle-on me-1"></i><%=Common.getBahasaConfig("Status")%></th>
                                <th class="fw-bold text-uppercase small text-center" style="width: 100px;"><i class="fas fa-cogs"></i></th>
                            </tr>
                        </thead>
                        <tbody id="tabelDataJenis<%=rnd%>">
                            <tr>
                                <td colspan="13" class="text-center py-5">
                                    <div class="spinner-border text-primary mb-3"></div>
                                    <br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Sistem sedang memuat data...")%></span>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <!-- PAGING -->
                <div class="bg-light p-3 border-top d-flex flex-column flex-md-row justify-content-between align-items-center gap-3">
                    <div class="small fw-bold text-muted" id="pagingInfoJenis<%=rnd%>"></div>
                    <div class="d-flex align-items-center bg-white rounded-pill shadow-sm border p-1">
                        <button class="btn btn-sm btn-light rounded-pill px-3 fw-bold text-primary" id="btnPrevPage<%=rnd%>" onclick="changePageJenis<%=rnd%>(-1)">
                            <i class="fas fa-chevron-left me-1"></i><span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Sebelumnya")%></span>
                        </button>
                        <div class="px-3 fw-bold text-dark small">
                            <span id="pageNumberDisplay<%=rnd%>">1 / 1</span>
                        </div>
                        <button class="btn btn-sm btn-light rounded-pill px-3 fw-bold text-primary" id="btnNextPage<%=rnd%>" onclick="changePageJenis<%=rnd%>(1)">
                            <span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Selanjutnya")%></span><i class="fas fa-chevron-right ms-1"></i>
                        </button>
                    </div>
                </div>

            </div>
        </div>
    </div>

    <!-- TAMPILAN FORMULIR DATA -->
    <div id="viewForm<%=rnd%>" class="animate__animated animate__fadeIn" style="display: none;">
        <div class="row justify-content-center">
            <div class="col-lg-8">
                <div class="card border-0 shadow-lg rounded-4 border-top border-success border-4">
                    <div class="card-header bg-white border-bottom-0 pt-4 pb-0 px-4">
                        <div class="d-flex justify-content-between align-items-center">
                            <h5 class="fw-bold text-dark mb-0" id="formTitle<%=rnd%>">
                                <i class="fas fa-tag text-success me-2"></i><%=Common.getBahasaConfig("Formulir Jenis Anggota")%>
                            </h5>
                            <div class="form-check form-switch fs-5 mb-0">
                                <input class="form-check-input shadow-sm" type="checkbox" id="inputStatusAktif<%=rnd%>" checked style="cursor: pointer;">
                                <label class="form-check-label fs-6 fw-bold text-success" for="inputStatusAktif<%=rnd%>" id="labelStatusAktif<%=rnd%>"><%=Common.getBahasaConfig("Aktif")%></label>
                            </div>
                        </div>
                    </div>
                    
                    <div class="card-body p-4">
                        <form id="formJenisAnggota<%=rnd%>" onsubmit="event.preventDefault(); simpanJenisAnggota<%=rnd%>();">
                            <input type="hidden" id="inputIdJenis<%=rnd%>" value="">
                            
                            <div class="row g-4 mt-1">
                                
                                <div class="col-md-4">
                                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-barcode me-1 text-muted"></i><%=Common.getBahasaConfig("Kode Singkat")%></label>
                                    <input type="text" class="form-control fw-bold shadow-sm" id="inputKode<%=rnd%>" placeholder="<%=Common.getBahasaConfig("MHS, GURU, UMUM")%>">
                                </div>
                                
                                <div class="col-md-8">
                                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-font me-1 text-muted"></i><%=Common.getBahasaConfig("Nama Jenis Anggota")%> <span class="text-danger">*</span></label>
                                    <input type="text" class="form-control fw-bold shadow-sm" id="inputNama<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: Mahasiswa Aktif")%>" required>
                                </div>

                                <div class="col-md-6">
                                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-wallet me-1 text-muted"></i><%=Common.getBahasaConfig("Istilah Tampilan Sisa Saldo")%></label>
                                    <input type="text" class="form-control fw-bold shadow-sm" id="inputIstilahSaldo<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: Saldo Kas, E-Money, Titipan")%>" value="Saldo Kas">
                                    <small class="text-muted d-block mt-1" style="font-size: 11px;"><i class="fas fa-info-circle me-1"></i><%=Common.getBahasaConfig("Teks ini akan muncul sebagai keterangan saldo utama.")%></small>
                                </div>

                                <div class="col-md-6">
                                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-hand-holding-usd me-1 text-muted"></i><%=Common.getBahasaConfig("Istilah Tampilan Cashback")%></label>
                                    <input type="text" class="form-control fw-bold shadow-sm" id="inputIstilahCashback<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: Cashback, Poin, Koin")%>" value="Cashback">
                                    <small class="text-muted d-block mt-1" style="font-size: 11px;"><i class="fas fa-info-circle me-1"></i><%=Common.getBahasaConfig("Teks ini akan muncul sebagai keterangan saldo hadiah/promo.")%></small>
                                </div>
                                
                                <div class="col-md-12">
                                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-money-bill-wave me-1 text-muted"></i><%=Common.getBahasaConfig("Minimal Saldo Mengendap")%></label>
                                    <div class="input-group shadow-sm">
                                        <span class="input-group-text bg-light border-end-0 fw-bold">Rp</span>
                                        <input type="number" class="form-control fw-bold border-start-0" id="inputMinimalSaldo<%=rnd%>" placeholder="0" min="0" step="any">
                                    </div>
                                    <small class="text-muted d-block mt-1" style="font-size: 11px;"><i class="fas fa-info-circle me-1"></i><%=Common.getBahasaConfig("Batas saldo minimum yang tertahan dan tidak dapat digunakan untuk transaksi.")%></small>
                                </div>
                                
                                <div class="col-md-12 mt-4 border-top pt-3">
                                    <label class="form-label small fw-bold text-primary mb-2"><i class="fas fa-shopping-cart me-1"></i><%=Common.getBahasaConfig("Aturan Kewajiban Belanja Member")%></label>
                                    
                                    <div class="bg-light p-3 rounded-3 border shadow-sm">
                                        <!-- Pengaturan Wajib Belanja -->
                                        <div class="form-check form-switch fs-6 mb-3 border-bottom pb-3">
                                            <input class="form-check-input ms-0 me-2 shadow-sm border border-primary" type="checkbox" id="inputWajibBelanja<%=rnd%>" style="cursor: pointer;" onchange="toggleAturanBelanja<%=rnd%>()">
                                            <label class="form-check-label fw-bold text-primary" for="inputWajibBelanja<%=rnd%>"><%=Common.getBahasaConfig("Aktifkan Aturan Wajib Belanja Mingguan")%></label>
                                            <small class="d-block text-muted mt-1" style="font-size: 11px;"><%=Common.getBahasaConfig("Jika diaktifkan, sistem akan memantau jumlah transaksi member per minggu. Bisa dimatikan saat libur akademik.")%></small>
                                        </div>

                                        <div class="row g-3" id="areaAturanBelanja<%=rnd%>" style="display: none;">
                                            <div class="col-md-6">
                                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Target Belanja Per Minggu")%> <span class="text-danger">*</span></label>
                                                <div class="input-group shadow-sm">
                                                    <input type="number" class="form-control fw-bold text-end" id="inputTargetBelanja<%=rnd%>" placeholder="0" min="0">
                                                    <span class="input-group-text bg-white text-muted"><%=Common.getBahasaConfig("Kali")%></span>
                                                </div>
                                                <small class="text-muted d-block mt-1" style="font-size: 11px;"><%=Common.getBahasaConfig("Contoh: 5 (Member harus belanja minimal 5x seminggu).")%></small>
                                            </div>
                                            
                                            <div class="col-md-6">
                                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Batas Maksimal SP (Hangus)")%> <span class="text-danger">*</span></label>
                                                <div class="input-group shadow-sm">
                                                    <input type="number" class="form-control fw-bold text-end" id="inputMaksSP<%=rnd%>" placeholder="0" min="0">
                                                    <span class="input-group-text bg-white text-muted"><%=Common.getBahasaConfig("Peringatan")%></span>
                                                </div>
                                                <small class="text-muted d-block mt-1" style="font-size: 11px;"><%=Common.getBahasaConfig("Jika pelanggaran mencapai batas ini, status member otomatis non-aktif.")%></small>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                
                                <div class="col-md-12 mt-4 border-top pt-3">
                                    <label class="form-label small fw-bold text-primary mb-2"><i class="fas fa-sliders-h me-1"></i><%=Common.getBahasaConfig("Pengaturan Fitur & Izin")%></label>
                                    
                                    <div class="bg-light p-3 rounded-3 border shadow-sm d-flex flex-column gap-3">
                                        <!-- Pengaturan Tampilkan Saldo -->
                                        <div class="form-check form-switch fs-6 mb-0 border-bottom pb-2">
                                            <input class="form-check-input ms-0 me-2 shadow-sm border border-primary" type="checkbox" id="inputTampilkanSaldo<%=rnd%>" checked style="cursor: pointer;">
                                            <label class="form-check-label fw-bold text-primary small" for="inputTampilkanSaldo<%=rnd%>"><%=Common.getBahasaConfig("Tampilkan Nominal Saldo Utama di Beranda Anggota")%></label>
                                            <small class="d-block text-muted mt-1" style="font-size: 11px;"><%=Common.getBahasaConfig("Jika dinonaktifkan, anggota tidak akan dapat melihat informasi sisa saldo utamanya.")%></small>
                                        </div>

                                        <!-- Pengaturan Tampilkan Cashback -->
                                        <div class="form-check form-switch fs-6 mb-0 border-bottom pb-2">
                                            <input class="form-check-input ms-0 me-2 shadow-sm border" type="checkbox" id="inputTampilkanCashback<%=rnd%>" checked style="cursor: pointer;">
                                            <label class="form-check-label fw-bold text-dark small" for="inputTampilkanCashback<%=rnd%>"><%=Common.getBahasaConfig("Tampilkan Menu & Saldo Cashback di Beranda Anggota")%></label>
                                            <small class="d-block text-muted mt-1" style="font-size: 11px;"><%=Common.getBahasaConfig("Jika dinonaktifkan, anggota tidak akan melihat informasi saldo cashback yang mereka miliki.")%></small>
                                        </div>
                                        
                                        <!-- Pengaturan Bisa Dipilih -->
                                        <div class="form-check form-switch fs-6 mb-0 border-bottom pb-2">
                                            <input class="form-check-input ms-0 me-2 shadow-sm border" type="checkbox" id="inputDipilih<%=rnd%>" style="cursor: pointer;">
                                            <label class="form-check-label fw-bold text-dark small" for="inputDipilih<%=rnd%>"><%=Common.getBahasaConfig("Bisa Dipilih Saat Pendaftaran (Form Calon Anggota)")%></label>
                                        </div>

                                        <div class="border rounded-3 p-3 bg-white">
                                            <div class="fw-bold text-primary small mb-2"><i class="fas fa-shield-alt me-1"></i><%=Common.getBahasaConfig("Verifikasi sebelum memotong saldo")%></div>
                                            <div class="form-check form-switch mb-2">
                                                <input class="form-check-input" type="checkbox" id="inputWajibPin<%=rnd%>">
                                                <label class="form-check-label fw-bold small" for="inputWajibPin<%=rnd%>"><%=Common.getBahasaConfig("Wajib Verifikasi PIN numerik")%></label>
                                            </div>
                                            <div class="form-check form-switch mb-2">
                                                <input class="form-check-input" type="checkbox" id="inputWajibBiometricWajah<%=rnd%>">
                                                <label class="form-check-label fw-bold small" for="inputWajibBiometricWajah<%=rnd%>"><%=Common.getBahasaConfig("Wajib Verifikasi Biometric wajah (kamera + liveness)")%></label>
                                            </div>
                                            <div class="form-check form-switch">
                                                <input class="form-check-input" type="checkbox" id="inputWajibBiometricFingerprint<%=rnd%>">
                                                <label class="form-check-label fw-bold small" for="inputWajibBiometricFingerprint<%=rnd%>"><%=Common.getBahasaConfig("Wajib Verifikasi Fingerprint (scanner USB/OTG + SDK)")%></label>
                                            </div>
                                            <small class="text-muted d-block mt-2"><%=Common.getBahasaConfig("Semua metode yang dicentang wajib berhasil dan diakui server sebelum saldo member dipotong.")%></small>
                                        </div>

                                        <!-- Pengaturan Boleh Topup -->
                                        <div class="form-check form-switch fs-6 mb-0">
                                            <input class="form-check-input ms-0 me-2 shadow-sm border" type="checkbox" id="inputBolehTopup<%=rnd%>" style="cursor: pointer;">
                                            <label class="form-check-label fw-bold text-dark small" for="inputBolehTopup<%=rnd%>"><%=Common.getBahasaConfig("Mengizinkan Top-Up Saldo Secara Manual oleh Admin")%></label>
                                        </div>
                                    </div>
                                </div>

                                <div class="col-12">
                                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-align-left me-1 text-muted"></i><%=Common.getBahasaConfig("Keterangan Tambahan")%></label>
                                    <textarea class="form-control text-muted shadow-sm" id="inputKeterangan<%=rnd%>" rows="2" placeholder="<%=Common.getBahasaConfig("Masukkan penjelasan mengenai jenis keanggotaan ini jika diperlukan...")%>"></textarea>
                                </div>

                                <div class="col-12 mt-2 border-top pt-4">
                                    <label class="form-label small fw-bold text-primary mb-2"><i class="fas fa-credit-card me-2"></i><%=Common.getBahasaConfig("Daftar Saluran Pembayaran yang Diizinkan")%></label>
                                    <div class="bg-white p-3 border rounded-3 shadow-sm">
                                        <div class="row g-3" id="containerCaraBayar<%=rnd%>">
                                            <div class="col-12 text-muted small"><i class="fas fa-spinner fa-spin me-2"></i><%=Common.getBahasaConfig("Memuat metode pembayaran...")%></div>
                                        </div>
                                        <small class="text-muted d-block mt-3 border-top pt-2" style="font-size: 11px;"><i class="fas fa-info-circle me-1"></i><%=Common.getBahasaConfig("Centang saluran pembayaran yang dapat digunakan oleh jenis anggota ini untuk bertransaksi.")%></small>
                                    </div>
                                </div>

                                <div class="col-12 mt-5 text-end border-top pt-4">
                                    <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-bold border shadow-sm" onclick="tutupForm<%=rnd%>()">
                                        <i class="fas fa-times text-danger me-2"></i><%=Common.getBahasaConfig("Kembali")%>
                                    </button>
                                    <button type="submit" class="btn btn-success px-5 rounded-pill shadow-sm fw-bold" id="btnSimpan<%=rnd%>">
                                        <i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Data")%>
                                    </button>
                                </div>
                            </div>
                        </form>
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
    const masterModelClass<%=rnd%> = "ais.database.model.koperasi.JenisAnggotaKoperasi";
    let isEditMode<%=rnd%> = false; 
    
    // Inject status Admin dari Java ke JavaScript
    const isAdmin<%=rnd%> = <%=isAdmin%>;
    const colSpan<%=rnd%> = 13;

    // Variabel Paging & Cache
    let currentPage<%=rnd%> = 1;
    const limitPerPage<%=rnd%> = 10;
    let totalRecords<%=rnd%> = 0;
    let listCaraBayar<%=rnd%> = [];

    const fetchData<%=rnd%> = async (sql) => {
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sql: sql, action: "sql" })
            });
            const result = await res.json();
            return result.data || [];
        } catch (e) { 
            console.error("Fetch SQL Error:", e); 
            return []; 
        }
    };

    const showToast<%=rnd%> = (msg, colorClass) => {
        if(typeof tampilkanToast === "function") {
            tampilkanToast(msg, colorClass);
        } else {
            alert(msg);
        }
    };

    // Memuat Opsi Cara Pembayaran (Render Checkbox)
    const loadMasterCaraBayar<%=rnd%> = async () => {
        const sql = "SELECT id, nama FROM koperasi.cara_pembayaran_koperasi WHERE aktif = true ORDER BY nama ASC";
        const res = await fetchData<%=rnd%>(sql);
        listCaraBayar<%=rnd%> = res;
        
        let htmlCb = '';
        if(res.length === 0) {
            htmlCb = '<div class="col-12 text-muted small"><i class="fas fa-exclamation-triangle text-warning me-1"></i><%=Common.getBahasaConfig("Tidak ada metode pembayaran yang aktif di sistem.")%></div>';
        } else {
            res.forEach(item => {
                htmlCb += '<div class="col-md-6">' +
                            '<div class="form-check bg-light border rounded p-2 ps-4">' +
                                '<input class="form-check-input chk-carabayar-<%=rnd%>" type="checkbox" value="' + item.id + '" id="cbBayar_<%=rnd%>_' + item.id + '">' +
                                '<label class="form-check-label small fw-bold text-dark ms-1" style="cursor:pointer;" for="cbBayar_<%=rnd%>_' + item.id + '">' + item.nama + '</label>' +
                            '</div>' +
                          '</div>';
            });
        }
        document.getElementById('containerCaraBayar<%=rnd%>').innerHTML = htmlCb;
    };

    // Fungsi Penguncian Form (Readonly untuk selain Admin)
    const setFormState<%=rnd%> = (isReadonly) => {
        document.getElementById('inputKode<%=rnd%>').disabled = isReadonly;
        document.getElementById('inputNama<%=rnd%>').disabled = isReadonly;
        document.getElementById('inputIstilahSaldo<%=rnd%>').disabled = isReadonly;
        document.getElementById('inputIstilahCashback<%=rnd%>').disabled = isReadonly;
        document.getElementById('inputMinimalSaldo<%=rnd%>').disabled = isReadonly;
        document.getElementById('inputKeterangan<%=rnd%>').disabled = isReadonly;
        document.getElementById('inputDipilih<%=rnd%>').disabled = isReadonly;
        document.getElementById('inputBolehTopup<%=rnd%>').disabled = isReadonly;
        document.getElementById('inputTampilkanCashback<%=rnd%>').disabled = isReadonly;
        document.getElementById('inputTampilkanSaldo<%=rnd%>').disabled = isReadonly;
        document.getElementById('inputWajibPin<%=rnd%>').disabled = isReadonly;
        document.getElementById('inputWajibBiometricWajah<%=rnd%>').disabled = isReadonly;
        document.getElementById('inputWajibBiometricFingerprint<%=rnd%>').disabled = isReadonly;
        document.getElementById('inputStatusAktif<%=rnd%>').disabled = isReadonly;
        
        document.getElementById('inputWajibBelanja<%=rnd%>').disabled = isReadonly;
        document.getElementById('inputTargetBelanja<%=rnd%>').disabled = isReadonly;
        document.getElementById('inputMaksSP<%=rnd%>').disabled = isReadonly;
        
        // Kunci Checkbox Pembayaran
        const cbs = document.querySelectorAll('.chk-carabayar-<%=rnd%>');
        cbs.forEach(cb => cb.disabled = isReadonly);

        const btnSimpan = document.getElementById('btnSimpan<%=rnd%>');
        if (isReadonly) {
            btnSimpan.style.display = 'none';
        } else {
            btnSimpan.style.display = 'inline-block';
        }
    };

    // Toggle Area Aturan Belanja
    const toggleAturanBelanja<%=rnd%> = () => {
        const isWajib = document.getElementById('inputWajibBelanja<%=rnd%>').checked;
        const areaAturan = document.getElementById('areaAturanBelanja<%=rnd%>');
        const inTarget = document.getElementById('inputTargetBelanja<%=rnd%>');
        const inSP = document.getElementById('inputMaksSP<%=rnd%>');
        
        if(isWajib) {
            areaAturan.style.display = 'flex';
            inTarget.required = true;
            inSP.required = true;
        } else {
            areaAturan.style.display = 'none';
            inTarget.required = false;
            inSP.required = false;
            inTarget.value = '0';
            inSP.value = '0';
        }
    };

    // Label UI Switch Status Aktif
    document.getElementById('inputStatusAktif<%=rnd%>').addEventListener('change', function() {
        const lbl = document.getElementById('labelStatusAktif<%=rnd%>');
        if(this.checked) {
            lbl.innerText = '<%=Common.getBahasaConfigJS("Aktif")%>';
            lbl.className = 'form-check-label fs-6 fw-bold text-success';
        } else {
            lbl.innerText = '<%=Common.getBahasaConfigJS("Tidak Aktif")%>';
            lbl.className = 'form-check-label fs-6 fw-bold text-danger';
        }
    });

    // ==========================================
    // BAGIAN 2: LOGIKA LIST, PAGING & SEARCH
    // ==========================================
    const buildFilterSQL<%=rnd%> = () => {
        const keyword = document.getElementById('searchJenis<%=rnd%>').value.trim().replace(/'/g, "''");
        let filters = " WHERE 1=1 ";
        if (keyword !== '') {
            filters += " AND (nama ILIKE '%" + keyword + "%' OR kode ILIKE '%" + keyword + "%') ";
        }
        return filters;
    };

    const resetAndLoadData<%=rnd%> = () => {
        currentPage<%=rnd%> = 1; 
        loadDataJenisAnggota<%=rnd%>();
    };

    const loadDataJenisAnggota<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataJenis<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Mengambil data dari server...")%></span></td></tr>';

        const sqlFilters = buildFilterSQL<%=rnd%>();
        const offset = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%>;

        const sqlCount = "SELECT COUNT(*) AS jumlah FROM koperasi.jenis_anggota_koperasi " + sqlFilters + ";";
        
        // Penambahan kolom tampilkan_sisa_saldo dan istilah_cashback pada query utama
        const sqlData = "SELECT id, kode, nama, keterangan, dipilih, boleh_entry_topup_oleh_admin, aktif, tampilkan_cashback, tampilkan_sisa_saldo, istilah_sisa_saldo, istilah_cashback, daftar_cara_pembayaran_yang_boleh_di_pilih, wajib_belanja_rutin, target_frekuensi_belanja, maksimal_pelanggaran, COALESCE(wajib_pin,false) wajib_pin, COALESCE(wajib_verifikasi_biometric_wajah,false) wajib_verifikasi_biometric_wajah, COALESCE(wajib_verifikasi_biometric_fingerprint,false) wajib_verifikasi_biometric_fingerprint FROM koperasi.jenis_anggota_koperasi " + sqlFilters + " ORDER BY id DESC LIMIT " + limitPerPage<%=rnd%> + " OFFSET " + offset + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetchData<%=rnd%>(sqlCount),
                fetchData<%=rnd%>(sqlData)
            ]);

            totalRecords<%=rnd%> = (resCount && resCount[0]) ? parseInt(resCount[0].jumlah || 0) : 0;
            const recordList = resData || [];

            let htmlList = '';

            if (recordList.length === 0) {
                htmlList = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center text-muted py-5"><i class="fas fa-tags fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Tidak ada data jenis anggota yang ditemukan.")%></td></tr>';
            } else {
                let no = offset + 1; 
                recordList.forEach((row) => {

                    const isStatusAktif = (row.aktif === null || row.aktif === true || row.aktif === 'true' || row.aktif === 't');
                    const badgeStatus = isStatusAktif ? '<span class="badge bg-success bg-opacity-10 text-success border border-success px-2 py-1"><i class="fas fa-check-circle me-1"></i><%=Common.getBahasaConfig("Aktif")%></span>' : '<span class="badge bg-danger bg-opacity-10 text-danger border border-danger px-2 py-1"><i class="fas fa-times-circle me-1"></i><%=Common.getBahasaConfig("Non-Aktif")%></span>';

                    const isDipilih = (row.dipilih === true || row.dipilih === 'true' || row.dipilih === 't');
                    const badgeDipilih = isDipilih ? '<span class="text-success fw-bold"><i class="fas fa-check-circle"></i></span>' : '<span class="text-secondary fw-bold"><i class="fas fa-minus"></i></span>';

                    const isBolehTopup = (row.boleh_entry_topup_oleh_admin === true || row.boleh_entry_topup_oleh_admin === 'true' || row.boleh_entry_topup_oleh_admin === 't');
                    const badgeBolehTopup = isBolehTopup ? '<span class="text-success fw-bold"><i class="fas fa-check-circle"></i></span>' : '<span class="text-secondary fw-bold"><i class="fas fa-minus"></i></span>';

                    // Kolom Tampilkan Saldo
                    const isTampilSaldo = (row.tampilkan_sisa_saldo === null || row.tampilkan_sisa_saldo === true || row.tampilkan_sisa_saldo === 'true' || row.tampilkan_sisa_saldo === 't');
                    const badgeTampilSaldo = isTampilSaldo ? '<span class="text-primary fw-bold"><i class="fas fa-check-circle"></i></span>' : '<span class="text-secondary fw-bold"><i class="fas fa-minus"></i></span>';

                    // Kolom Tampilkan Cashback
                    const isTampilCb = (row.tampilkan_cashback === null || row.tampilkan_cashback === true || row.tampilkan_cashback === 'true' || row.tampilkan_cashback === 't');
                    const badgeTampilCb = isTampilCb ? '<span class="text-success fw-bold"><i class="fas fa-check-circle"></i></span>' : '<span class="text-secondary fw-bold"><i class="fas fa-minus"></i></span>';
                    const badgeWajibPin = (row.wajib_pin === true || row.wajib_pin === 'true' || row.wajib_pin === 't') ? '<span class="badge bg-warning text-dark">PIN</span>' : '<span class="text-muted">-</span>';
                    const badgeWajibWajah = (row.wajib_verifikasi_biometric_wajah === true || row.wajib_verifikasi_biometric_wajah === 'true' || row.wajib_verifikasi_biometric_wajah === 't') ? '<span class="badge bg-info text-dark"><i class="fas fa-camera"></i></span>' : '<span class="text-muted">-</span>';
                    const badgeWajibFingerprint = (row.wajib_verifikasi_biometric_fingerprint === true || row.wajib_verifikasi_biometric_fingerprint === 'true' || row.wajib_verifikasi_biometric_fingerprint === 't') ? '<span class="badge bg-primary"><i class="fas fa-fingerprint"></i></span>' : '<span class="text-muted">-</span>';

                    // Istilah Saldo & Cashback
                    const istilahSaldo = row.istilah_sisa_saldo || 'Saldo Kas';
                    const istilahCashback = row.istilah_cashback || 'Cashback';

                    let actionBtn = '';
                    if (isAdmin<%=rnd%>) {
                        actionBtn = '<button class="btn btn-sm btn-outline-warning text-dark shadow-sm px-3 fw-bold" onclick="editJenisAnggota<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Ubah Data")%>"><i class="fas fa-edit me-1"></i><%=Common.getBahasaConfig("Ubah")%></button>';
                    } else {
                        actionBtn = '<button class="btn btn-sm btn-outline-info text-dark shadow-sm px-3 fw-bold" onclick="lihatJenisAnggota<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Lihat Rincian")%>"><i class="fas fa-eye me-1"></i><%=Common.getBahasaConfig("Lihat")%></button>';
                    }

                    htmlList += '<tr>' +
                            '<td class="text-center text-muted fw-medium">' + (no++) + '</td>' +
                            '<td class="text-start fw-bold text-secondary">' + (row.kode || '-') + '</td>' +
                            '<td class="text-start fw-bold text-dark">' + (row.nama || '-') + '<br>' +
                            	'<span class="badge bg-light text-dark border mt-1 me-1" title="<%=Common.getBahasaConfig("Istilah Saldo")%>"><i class="fas fa-wallet text-primary me-1"></i>' + istilahSaldo + '</span>' +
                            	'<span class="badge bg-light text-dark border mt-1" title="<%=Common.getBahasaConfig("Istilah Cashback")%>"><i class="fas fa-hand-holding-usd text-success me-1"></i>' + istilahCashback + '</span>' +
                            '</td>' +
                            '<td class="text-start text-muted small">' + (row.keterangan || '-') + '</td>' +
                            '<td class="text-center">' + badgeTampilSaldo + '</td>' +
                            '<td class="text-center">' + badgeTampilCb + '</td>' +
                            '<td class="text-center">' + badgeDipilih + '</td>' +
                            '<td class="text-center">' + badgeBolehTopup + '</td>' +
                            '<td class="text-center">' + badgeWajibPin + '</td>' +
                            '<td class="text-center">' + badgeWajibWajah + '</td>' +
                            '<td class="text-center">' + badgeWajibFingerprint + '</td>' +
                            '<td class="text-center">' + badgeStatus + '</td>' +
                            '<td class="text-center text-nowrap">' + actionBtn + '</td>' +
                        '</tr>';
                });
            }
            tbody.innerHTML = htmlList;
            updatePaginationUI<%=rnd%>();

        } catch (error) {
            console.error("Gagal load tabel jenis anggota:", error);
            tbody.innerHTML = '<tr><td colspan="' + colSpan<%=rnd%> + '" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Terjadi kesalahan sistem saat menarik data. Silakan muat ulang.")%></td></tr>';
        }
    };

    const updatePaginationUI<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumberDisplay<%=rnd%>').innerText = currentPage<%=rnd%> + " / " + totalPages;
        
        const startIdx = totalRecords<%=rnd%> > 0 ? ((currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%> + 1) : 0;
        const endIdx = Math.min(currentPage<%=rnd%> * limitPerPage<%=rnd%>, totalRecords<%=rnd%>);
        const startInfo = totalRecords<%=rnd%> > 0 ? startIdx : 0;
        
        document.getElementById('pagingInfoJenis<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startInfo + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari total")%> <b class="text-dark">' + totalRecords<%=rnd%> + '</b> <%=Common.getBahasaConfig("data")%>';
        
        document.getElementById('btnPrevPage<%=rnd%>').disabled = (currentPage<%=rnd%> === 1);
        document.getElementById('btnNextPage<%=rnd%>').disabled = (currentPage<%=rnd%> === totalPages);
    };

    const changePageJenis<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if (direction === -1 && currentPage<%=rnd%> > 1) {
            currentPage<%=rnd%>--;
            loadDataJenisAnggota<%=rnd%>();
        } else if (direction === 1 && currentPage<%=rnd%> < totalPages) {
            currentPage<%=rnd%>++;
            loadDataJenisAnggota<%=rnd%>();
        }
    };


    // ==========================================
    // BAGIAN 3: LOGIKA FORM & DINAMIS UI
    // ==========================================
    const bukaFormTambah<%=rnd%> = () => {
        if (!isAdmin<%=rnd%>) return; 
        
        isEditMode<%=rnd%> = false; 
        
        document.getElementById('inputIdJenis<%=rnd%>').value = '';
        document.getElementById('formJenisAnggota<%=rnd%>').reset();
        
        const chkStatus = document.getElementById('inputStatusAktif<%=rnd%>');
        chkStatus.checked = true;
        chkStatus.dispatchEvent(new Event('change'));

        document.getElementById('inputDipilih<%=rnd%>').checked = false;
        document.getElementById('inputBolehTopup<%=rnd%>').checked = false; 
        document.getElementById('inputTampilkanCashback<%=rnd%>').checked = true; 
        document.getElementById('inputTampilkanSaldo<%=rnd%>').checked = true; 
        document.getElementById('inputIstilahSaldo<%=rnd%>').value = 'Saldo Kas';
        document.getElementById('inputIstilahCashback<%=rnd%>').value = 'Cashback';
        document.getElementById('inputMinimalSaldo<%=rnd%>').value = '0';
        document.getElementById('inputWajibPin<%=rnd%>').checked = false;
        document.getElementById('inputWajibBiometricWajah<%=rnd%>').checked = false;
        document.getElementById('inputWajibBiometricFingerprint<%=rnd%>').checked = false;
        
        document.getElementById('inputWajibBelanja<%=rnd%>').checked = false;
        document.getElementById('inputTargetBelanja<%=rnd%>').value = '0';
        document.getElementById('inputMaksSP<%=rnd%>').value = '0';
        toggleAturanBelanja<%=rnd%>();
        
        // Uncheck all metode pembayaran checkboxes
        const cbs = document.querySelectorAll('.chk-carabayar-<%=rnd%>');
        cbs.forEach(cb => cb.checked = false);

        setFormState<%=rnd%>(false);
        document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-plus-circle text-primary me-2"></i><%=Common.getBahasaConfig("Tambah Jenis Baru")%>';
        
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    };

    const tutupForm<%=rnd%> = () => {
        document.getElementById('viewForm<%=rnd%>').style.display = 'none';
        document.getElementById('viewList<%=rnd%>').style.display = 'block';
        loadDataJenisAnggota<%=rnd%>();
    };


    // ==========================================
    // BAGIAN 4: SIMPAN & EDIT (API SIMPANDATARINCI)
    // ==========================================
    const simpanJenisAnggota<%=rnd%> = async () => {
        if (!isAdmin<%=rnd%>) return; 
        
        const idJenis = document.getElementById('inputIdJenis<%=rnd%>').value;
        const btnSimpan = document.getElementById('btnSimpan<%=rnd%>');

        // Penggabungan Array Checkbox menjadi String Dipisah Koma
        const cbsChecked = document.querySelectorAll('.chk-carabayar-<%=rnd%>:checked');
        let stringCaraBayar = "";
        if (cbsChecked.length > 0) {
            let idArray = [];
            cbsChecked.forEach(cb => idArray.push(cb.value));
            // Format yang dihasilkan misal: ,1,4,5,
            stringCaraBayar = "," + idArray.join(",") + ","; 
        }
        
        // Object utama JenisAnggotaKoperasi
        const dataObj = {
            kode: document.getElementById('inputKode<%=rnd%>').value,
            nama: document.getElementById('inputNama<%=rnd%>').value,
            istilahSisaSaldo: document.getElementById('inputIstilahSaldo<%=rnd%>').value.trim() || 'Saldo Kas',
            istilahCashback: document.getElementById('inputIstilahCashback<%=rnd%>').value.trim() || 'Cashback',
            minimalSaldo: parseFloat(document.getElementById('inputMinimalSaldo<%=rnd%>').value) || 0,
            keterangan: document.getElementById('inputKeterangan<%=rnd%>').value,
            dipilih: document.getElementById('inputDipilih<%=rnd%>').checked,
            bolehEntryTopupOlehAdmin: document.getElementById('inputBolehTopup<%=rnd%>').checked,
            tampilkanCashback: document.getElementById('inputTampilkanCashback<%=rnd%>').checked,
            tampilkanSisaSaldo: document.getElementById('inputTampilkanSaldo<%=rnd%>').checked,
            wajibPin: document.getElementById('inputWajibPin<%=rnd%>').checked,
            wajibVerifikasiBiometricWajah: document.getElementById('inputWajibBiometricWajah<%=rnd%>').checked,
            wajibVerifikasiBiometricFingerprint: document.getElementById('inputWajibBiometricFingerprint<%=rnd%>').checked,
            wajibBelanjaRutin: document.getElementById('inputWajibBelanja<%=rnd%>').checked,
            targetFrekuensiBelanja: parseInt(document.getElementById('inputTargetBelanja<%=rnd%>').value) || 0,
            maksimalPelanggaran: parseInt(document.getElementById('inputMaksSP<%=rnd%>').value) || 0,
            aktif: document.getElementById('inputStatusAktif<%=rnd%>').checked,
            daftarCaraPembayaranYangBolehDiPilih: stringCaraBayar
        };
        
        const payload = { 
            action: "simpanDataRinci", 
            log: "true",
            class: masterModelClass<%=rnd%>, 
            data: dataObj
        };

        if (idJenis !== '') {
            payload.id = idJenis;
        }

        const oriBtn = btnSimpan.innerHTML;
        btnSimpan.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Sedang Menyimpan...")%>';
        btnSimpan.disabled = true;

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const result = await response.json();
            
            if (result.status === '00' || result.status === 'success' || result.id) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data jenis anggota berhasil disimpan dengan aman!")%>', 'bg-success text-white');
                tutupForm<%=rnd%>(); 
            } else {
                showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Sistem gagal menyimpan perubahan data ke database.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            console.error(e);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan komunikasi jaringan saat mengirim data.")%>', 'bg-danger text-white');
        } finally {
            btnSimpan.innerHTML = oriBtn;
            btnSimpan.disabled = false;
        }
    };

    const editJenisAnggota<%=rnd%> = async (id, isReadonly = false) => {
        if (!isAdmin<%=rnd%> && !isReadonly) return; 
        
        isEditMode<%=rnd%> = true; 
        
        const sql = 'SELECT id, kode, nama, keterangan, dipilih, boleh_entry_topup_oleh_admin, aktif, tampilkan_cashback, tampilkan_sisa_saldo, istilah_sisa_saldo, istilah_cashback, COALESCE(minimal_saldo, 0) as minimal_saldo, daftar_cara_pembayaran_yang_boleh_di_pilih, wajib_belanja_rutin, target_frekuensi_belanja, maksimal_pelanggaran, COALESCE(wajib_pin,false) wajib_pin, COALESCE(wajib_verifikasi_biometric_wajah,false) wajib_verifikasi_biometric_wajah, COALESCE(wajib_verifikasi_biometric_fingerprint,false) wajib_verifikasi_biometric_fingerprint FROM koperasi.jenis_anggota_koperasi WHERE id = ' + id;
        const res = await fetchData<%=rnd%>(sql);
        
        if (res.length > 0) {
            const data = res[0];
            
            // Set Form Data
            document.getElementById('inputIdJenis<%=rnd%>').value = data.id;
            document.getElementById('inputKode<%=rnd%>').value = data.kode || '';
            document.getElementById('inputNama<%=rnd%>').value = data.nama || '';
            document.getElementById('inputIstilahSaldo<%=rnd%>').value = data.istilah_sisa_saldo || 'Saldo Kas';
            document.getElementById('inputIstilahCashback<%=rnd%>').value = data.istilah_cashback || 'Cashback';
            document.getElementById('inputMinimalSaldo<%=rnd%>').value = data.minimal_saldo || 0;
            document.getElementById('inputWajibPin<%=rnd%>').checked = (data.wajib_pin === true || data.wajib_pin === 'true' || data.wajib_pin === 't');
            document.getElementById('inputWajibBiometricWajah<%=rnd%>').checked = (data.wajib_verifikasi_biometric_wajah === true || data.wajib_verifikasi_biometric_wajah === 'true' || data.wajib_verifikasi_biometric_wajah === 't');
            document.getElementById('inputWajibBiometricFingerprint<%=rnd%>').checked = (data.wajib_verifikasi_biometric_fingerprint === true || data.wajib_verifikasi_biometric_fingerprint === 'true' || data.wajib_verifikasi_biometric_fingerprint === 't');
            document.getElementById('inputKeterangan<%=rnd%>').value = data.keterangan || '';
            
            const isWajib = (data.wajib_belanja_rutin === true || data.wajib_belanja_rutin === 'true' || data.wajib_belanja_rutin === 't');
            document.getElementById('inputWajibBelanja<%=rnd%>').checked = isWajib;
            document.getElementById('inputTargetBelanja<%=rnd%>').value = data.target_frekuensi_belanja || 0;
            document.getElementById('inputMaksSP<%=rnd%>').value = data.maksimal_pelanggaran || 0;
            toggleAturanBelanja<%=rnd%>();

            const isDipilih = (data.dipilih === true || data.dipilih === 'true' || data.dipilih === 't');
            document.getElementById('inputDipilih<%=rnd%>').checked = isDipilih;

            const isBolehTopup = (data.boleh_entry_topup_oleh_admin === true || data.boleh_entry_topup_oleh_admin === 'true' || data.boleh_entry_topup_oleh_admin === 't');
            document.getElementById('inputBolehTopup<%=rnd%>').checked = isBolehTopup;

            const isTampilSaldo = (data.tampilkan_sisa_saldo === null || data.tampilkan_sisa_saldo === true || data.tampilkan_sisa_saldo === 'true' || data.tampilkan_sisa_saldo === 't');
            document.getElementById('inputTampilkanSaldo<%=rnd%>').checked = isTampilSaldo;

            const isTampilCb = (data.tampilkan_cashback === null || data.tampilkan_cashback === true || data.tampilkan_cashback === 'true' || data.tampilkan_cashback === 't');
            document.getElementById('inputTampilkanCashback<%=rnd%>').checked = isTampilCb;

            const isAktif = (data.aktif === null || data.aktif === true || data.aktif === 'true' || data.aktif === 't');
            const chkAktif = document.getElementById('inputStatusAktif<%=rnd%>');
            chkAktif.checked = isAktif;
            chkAktif.dispatchEvent(new Event('change')); 

            // Atur Centang Checkbox Cara Pembayaran
            const daftarBayarStr = data.daftar_cara_pembayaran_yang_boleh_di_pilih || "";
            const cbs = document.querySelectorAll('.chk-carabayar-<%=rnd%>');
            cbs.forEach(cb => {
                // Pengecekan ,ID, untuk memastikan tidak keliru ID parsial (contoh: cari ID 2 dlm string ",22,")
                if (daftarBayarStr.indexOf("," + cb.value + ",") !== -1) {
                    cb.checked = true;
                } else {
                    cb.checked = false;
                }
            });

            // Atur Form State Terkunci (Jika Mode Pedagang / Lihat Detail)
            setFormState<%=rnd%>(isReadonly);
            
            if (isReadonly) {
                document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-eye text-info me-2"></i><%=Common.getBahasaConfig("Rincian Jenis Anggota")%>';
            } else {
                document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-edit text-warning me-2"></i><%=Common.getBahasaConfig("Ubah Konfigurasi Jenis Anggota")%>';
            }
            
            document.getElementById('viewList<%=rnd%>').style.display = 'none';
            document.getElementById('viewForm<%=rnd%>').style.display = 'block';
        }
    };

    // Fungsi Helper Khusus Untuk Pedagang Melihat (Read-Only)
    const lihatJenisAnggota<%=rnd%> = (id) => {
        editJenisAnggota<%=rnd%>(id, true);
    };

    // INISIALISASI
    document.addEventListener("DOMContentLoaded", () => {
        document.getElementById('searchJenis<%=rnd%>').addEventListener("keydown", (e) => {
            if (e.key === "Enter") {
                e.preventDefault();
                resetAndLoadData<%=rnd%>(); 
            }
        });
        
        loadMasterCaraBayar<%=rnd%>();
        loadDataJenisAnggota<%=rnd%>();
    });
</script>
