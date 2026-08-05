<%@page import="java.net.URLEncoder"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.JenisSeleksi"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
	String paramAfiliasiKode = request.getParameter("afiliasi"); // Menangkap parameter kode afiliasi dari URL
    String rnd = Common.getGeneratedBarCode(7);
    String linkGelombang = Common.ROOT + "/pmb?hanya_tampil_jsp=true&p=pmb&s=_gelombang&baru=true";
    String linkPengumuman = Common.ROOT + "/pmb?hanya_tampil_jsp=true&p=pmb&s=_pengumuman&baru=true";
    String linkPengumumanRinci = Common.ROOT + "/pmb?hanya_tampil_jsp=true&p=home&s=pengumuman_rinci&baru=true";
    String linkApiProdi = Common.ROOT + "/pmb?hanya_tampil_jsp=true&p=pmb&s=_pilihan_prodi&baru=true";
    String linkPendaftaran = Common.ROOT + "/pmb?hanya_tampil_jsp=true&p=pmb&s=_pendaftaran_mahasiswa&baru=true";
    
    String kodeafiliasi = "------";
    if(paramAfiliasiKode != null && !paramAfiliasiKode.trim().isEmpty()){
    	kodeafiliasi = URLEncoder.encode(paramAfiliasiKode, "UTF-8");
    	linkPendaftaran += "&afiliasi="+kodeafiliasi;
    }
    
    String Telepon = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getTelepon();
    String Alamat1 = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getAlamat1();
    String Email = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getEmail();
    String judulNamaPerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama();
    String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");

    // LOGIKA KONFIGURASI LOGIN
    boolean tampilkanLogin = Common.getKonfigurasi("tampilkan_login_pmb", Konfigurasi.AKTIF).getNilai().equals(Konfigurasi.AKTIF);
    String headerLogin = Common.getKonfigurasi("header_login_pmb", "Login").getNilai();
    String tombolLogin = Common.getKonfigurasi("tombol_login_pmb", "Login Sekarang").getNilai();
    String bodyTxtLengkap = "Jika Anda telah melakukan pendaftaran,<br>pilih login untuk melengkapi data,<br> informasi ujian dan pembayaran,<br>serta informasi kelulusan.";
    if (Common.currentLang().equals(Tbmuser.INDONESIA)) {
        bodyTxtLengkap = Common.getKonfigurasi("body_login_pmb", bodyTxtLengkap).getNilai();
    }
%>

<style>
    /* CSS Khusus Komponen */
    .panel-bg-white-<%=rnd%> { background-color: rgba(255, 255, 255, 0.98) !important; backdrop-filter: blur(8px); }
    .icon-circle-<%=rnd%> { width: 45px; height: 45px; display: flex; align-items: center; justify-content: center; border-radius: 50%; font-size: 1.25rem; }
    .card-custom-<%=rnd%> { border: none; border-radius: 1.2rem; box-shadow: 0 4px 20px rgba(0,0,0,0.06); transition: transform 0.2s ease, box-shadow 0.2s ease; }
    .card-header-custom-<%=rnd%> { background-color: transparent; border-bottom: 1px solid rgba(0,0,0,0.05); padding: 1.25rem 1.5rem; }
    
    /* CSS Katalog Prodi */
    .prodi-card-<%=rnd%> { transition: all 0.3s ease; border: 1px solid rgba(0,0,0,0.08) !important; border-radius: 1rem; }
    .prodi-card-<%=rnd%>:hover { transform: translateY(-5px); box-shadow: 0 12px 24px rgba(0,0,0,0.12) !important; border-color: rgba(13, 110, 253, 0.3) !important; }
    .accordion-button:not(.collapsed) { background-color: rgba(13, 110, 253, 0.05); color: #0d6efd; box-shadow: none; border-bottom: 1px solid rgba(13, 110, 253, 0.1); }
    .accordion-button:focus { box-shadow: none; }

    /* CSS Kotak Login ala Groupbox */
    .login-groupbox-<%=rnd%> { border: 1px solid #bdbbbb; background-color: rgba(255,255,255,0.7); border-radius: 0.75rem; box-shadow: 1px 1px 5px rgba(0,0,0,0.08); transition: all 0.3s ease; }
    .login-groupbox-<%=rnd%>:hover { background-color: rgba(255,255,255,1); box-shadow: 0 8px 20px rgba(0,0,0,0.12); }
</style>

<div class="container mt-4 mb-5">
    
    <div class="row g-4 mb-4">
        
        <div class="col-lg-8">
            <div class="card card-custom-<%=rnd%> panel-bg-white-<%=rnd%> mb-4">
                <div class="card-header-custom-<%=rnd%>">
                    <h5 class="mb-0 fw-bold text-primary">
                        <i class="fas fa-magnifying-glass me-2 text-warning"></i><%= Common.getBahasaConfig("Pencarian Jadwal Pendaftaran") %>
                    </h5>
                </div>
                <div class="card-body rounded-bottom-4 bg-light bg-opacity-50 p-4">
                    <div class="row g-3">
                        <div class="col-md-4">
                            <label class="form-label fw-semibold small text-muted"><i class="far fa-calendar-days me-2"></i><%= Common.getBahasaConfig("Tahun Akademik") %></label>
                            <select class="form-select border-primary rounded-pill shadow-none fw-semibold text-dark" 
                                    onchange="loadGelombang<%=rnd%>(1); if(typeof window.loadKatalogProdiAjax<%=rnd%> === 'function') window.loadKatalogProdiAjax<%=rnd%>();" 
                                    id="filterTA<%=rnd%>">
                                <option value="" selected><%= Common.getBahasaConfig("Semua Tahun Akademik") %></option>
                                <% if (Common.tahunAngkatans != null) { 
                                    for (String t : Common.tahunAngkatans) { %>
                                        <option value="<%= t %>"><%= t %></option>
                                <% } } %>
                            </select>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label fw-semibold small text-muted"><i class="fas fa-list-ul me-2"></i><%= Common.getBahasaConfig("Jalur Seleksi") %></label>
                            <select class="form-select border-primary rounded-pill shadow-none fw-semibold text-dark" onchange="loadGelombang<%=rnd%>(1)" id="filterSeleksi<%=rnd%>">
                                <option value=""><%= Common.getBahasaConfig("Semua Jalur Seleksi") %></option>
                            </select>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label fw-semibold small text-muted"><i class="fas fa-door-open me-2"></i><%= Common.getBahasaConfig("Status Pendaftaran") %></label>
                            <select class="form-select border-primary rounded-pill shadow-none fw-semibold text-dark" onchange="loadGelombang<%=rnd%>(1)" id="filterStatus<%=rnd%>">
                                <option value="semua"><%= Common.getBahasaConfig("Semua Status") %></option>
                                <option value="buka" selected><%= Common.getBahasaConfig("Periode Aktif") %></option>
                                <option value="tutup"><%= Common.getBahasaConfig("Periode Berakhir") %></option>
                            </select>
                        </div>
                    </div>
                </div>
            </div>

            <div id="containerGelombang<%=rnd%>">
                <div class="card card-custom-<%=rnd%> panel-bg-white-<%=rnd%> p-5 text-center">
                    <div class="spinner-border text-primary" style="width: 3rem; height: 3rem;" role="status"></div>
                    <div class="mt-3 text-muted fw-bold"><%= Common.getBahasaConfig("Sistem sedang memuat data jadwal pendaftaran...") %></div>
                </div>
            </div>
        </div>

        <div class="col-lg-4">
            
            <% if (tampilkanLogin) { %>
            <div class="card login-groupbox-<%=rnd%> mb-4 p-3">
                <div class="d-flex align-items-center mb-3 border-bottom pb-2">
                    <i class="fas fa-right-to-bracket fa-2x text-primary me-3"></i>
                    <h5 class="fw-bold text-dark mb-0"><%= Common.getBahasaConfig(headerLogin) %></h5>
                </div>
                <div class="small text-muted mb-3 lh-base">
                    <%= Common.getBahasaConfig(bodyTxtLengkap) %>
                </div>
                <button type="button" class="btn btn-primary w-100 rounded-pill fw-bold shadow-sm" style="font-size: 15px;" onclick="if(typeof window.bukaModalLoginPMB === 'function') window.bukaModalLoginPMB();">
                    <i class="fas fa-right-to-bracket me-2"></i><%= Common.getBahasaConfig(tombolLogin) %>
                </button>
            </div>
            <% } %>

            <div class="card card-custom-<%=rnd%> panel-bg-white-<%=rnd%> mb-4">
                <div class="card-header-custom-<%=rnd%> d-flex justify-content-between align-items-center">
                    <h5 class="mb-0 fw-bold text-danger">
                        <i class="fas fa-bullhorn me-2"></i><%= Common.getBahasaConfig("Pengumuman Terkini") %>
                    </h5>
                    <button class="btn btn-sm btn-light border rounded-circle shadow-sm" onclick="loadPengumuman<%=rnd%>(1)" title="<%= Common.getBahasaConfig("Muat Ulang Informasi") %>">
                        <i class="fas fa-arrows-rotate text-secondary"></i>
                    </button>
                </div>
                <div class="card-body p-3 border-bottom bg-light bg-opacity-50">
                    <div class="input-group">
                        <input type="text" id="cariPengumuman<%=rnd%>" class="form-control border-danger rounded-start-pill shadow-none" 
                               placeholder="<%= Common.getBahasaConfig("Cari informasi...") %>" 
                               onkeydown="if(event.key === 'Enter') loadPengumuman<%=rnd%>(1)">
                        <button class="btn btn-danger text-white rounded-end-pill px-4 shadow-none" type="button" onclick="loadPengumuman<%=rnd%>(1)">
                            <i class="fas fa-magnifying-glass"></i>
                        </button>
                    </div>
                </div>
                <div class="card-body p-0">
                    <div id="listPengumuman<%=rnd%>" class="rounded-bottom-4">
                        <div class="p-5 text-center text-muted fw-bold"><%= Common.getBahasaConfig("Memuat pengumuman...") %></div>
                    </div>
                </div>
            </div>

            <div class="card card-custom-<%=rnd%> panel-bg-white-<%=rnd%>">
                <div class="card-header-custom-<%=rnd%>">
                    <h5 class="mb-0 fw-bold text-success text-center">
                        <i class="fas fa-headset me-2"></i><%= Common.getBahasaConfig("Layanan Informasi") %>
                    </h5>
                </div>
                <div class="card-body p-4">
                    <ul class="list-unstyled mb-0 text-secondary small">
                        <li class="d-flex align-items-start mb-4">
                            <div class="icon-circle-<%=rnd%> bg-danger bg-opacity-10 text-danger me-3 flex-shrink-0"><i class="fas fa-location-dot"></i></div>
                            <div class="mt-1"><strong class="d-block text-dark mb-1"><%= Common.getBahasaConfig("Alamat Resmi Kampus") %></strong><%= Alamat1 != null ? Alamat1 : "-" %></div>
                        </li>
                        <li class="d-flex align-items-center mb-4">
                            <div class="icon-circle-<%=rnd%> bg-success bg-opacity-10 text-success me-3 flex-shrink-0"><i class="fas fa-phone"></i></div>
                            <div><strong class="d-block text-dark mb-1"><%= Common.getBahasaConfig("Telepon Resmi") %></strong><%= Telepon != null ? Telepon : "-" %></div>
                        </li>
                        <li class="d-flex align-items-center">
                            <div class="icon-circle-<%=rnd%> bg-primary bg-opacity-10 text-primary me-3 flex-shrink-0"><i class="fas fa-envelope"></i></div>
                            <div><strong class="d-block text-dark mb-1"><%= Common.getBahasaConfig("Surat Elektronik (Email)") %></strong><%= Email != null ? Email : "-" %></div>
                        </li>
                    </ul>
                </div>
            </div>

        </div>
    </div>
    
    <div class="row g-4 mt-2">
        <div class="col-12">
            <div class="card card-custom-<%=rnd%> panel-bg-white-<%=rnd%>">
                <div class="card-header-custom-<%=rnd%> d-flex flex-column flex-md-row justify-content-between align-items-center bg-transparent py-4">
                    <div>
                        <h4 class="fw-bold text-dark mb-2"><i class="fas fa-rectangle-list text-primary me-2"></i><%= Common.getBahasaConfig("Katalog Program Studi") %></h4>
                        <p class="text-muted small mb-0"><%= Common.getBahasaConfig("Silakan tinjau daftar program studi yang dibuka, lalu tekan tombol daftar pada pilihan yang Anda minati.") %></p>
                    </div>
                </div>
                
                <div class="card-body bg-light bg-opacity-50 p-4 p-md-5 rounded-bottom-4" id="containerLazyProdi<%=rnd%>">
                    <div class="text-center py-5">
                        <div class="spinner-border text-success" style="width: 3.5rem; height: 3.5rem;" role="status"></div>
                        <div class="mt-4 text-muted fw-bold fs-6"><%= Common.getBahasaConfig("Memuat direktori Program Studi...") %></div>
                    </div>
                </div>
            </div>
        </div>
    </div>

</div>
<%
// Teks terjemahan untuk halaman sebelum login — di-escape agar aman di dalam string JS
String _slBatal       = Common.getBahasaConfig("Batal").replace("\\", "\\\\").replace("'", "\\'").replace("</", "<\\/");
String _slYaLanjutkan = Common.getBahasaConfig("Ya, Lanjutkan").replace("\\", "\\\\").replace("'", "\\'").replace("</", "<\\/");
String _slMemverif    = Common.getBahasaConfig("Memverifikasi Data...").replace("\\", "\\\\").replace("'", "\\'").replace("</", "<\\/");
String _slLoginPmb    = Common.getBahasaConfig("Akses Dasbor Peserta").replace("\\", "\\\\").replace("'", "\\'").replace("</", "<\\/");
String _slPengingat   = Common.getBahasaConfig("Pengingat Batas Waktu").replace("\\", "\\\\").replace("'", "\\'").replace("</", "<\\/");
String _slPesanBatas  = Common.getBahasaConfig("Pastikan Anda login sebelum batas waktu Gelombang Anda berakhir untuk melengkapi kelengkapan berkas dan tagihan pembayaran.").replace("\\", "\\\\").replace("'", "\\'").replace("</", "<\\/");
String _slMasukSkrg       = Common.getBahasaConfig("Masuk Sekarang").replace("\\", "\\\\").replace("'", "\\'").replace("</", "<\\/");
String _slSilakan         = Common.getBahasaConfig("Silakan masukkan <strong>No. Registrasi / Email / Nama Lengkap</strong> Anda sebagai Nama Pengguna dan <strong>Tanggal Lahir</strong> sebagai Kata Sandi.").replace("\\", "\\\\").replace("'", "\\'").replace("</", "<\\/");
String _slNoReg           = Common.getBahasaConfig("No. Registrasi / Email / Nama").replace("\\", "\\\\").replace("'", "\\'").replace("</", "<\\/");
String _slKetikNoReg      = Common.getBahasaConfig("Ketik No. Registrasi / Email / Nama Lengkap").replace("\\", "\\\\").replace("'", "\\'").replace("</", "<\\/");
String _slTglLahir        = Common.getBahasaConfig("Tanggal Lahir (Sebagai Sandi)").replace("\\", "\\\\").replace("'", "\\'").replace("</", "<\\/");
String _slAksesDitolak    = Common.getBahasaConfig("Akses ditolak. Gagal membuat sesi pengguna yang valid. Silakan hubungi admin.").replace("\\", "\\\\").replace("'", "\\'").replace("</", "<\\/");
String _slKombinasiSalah  = Common.getBahasaConfig("Kombinasi No. Registrasi/Email/Nama dan Tanggal Lahir tidak cocok. Mohon periksa kembali data Anda.").replace("\\", "\\\\").replace("'", "\\'").replace("</", "<\\/");
String _slKendalaJaringan = Common.getBahasaConfig("Terjadi kendala jaringan saat melakukan proses verifikasi ke peladen. Silakan coba kembali.").replace("\\", "\\\\").replace("'", "\\'").replace("</", "<\\/");
%>
<script>
// -----------------------------------------------------------------------------
// 0. UTILITAS GLOBAL (TOAST & CONFIRM MODAL)
// -----------------------------------------------------------------------------
if (typeof window.tampilkanToast !== 'function') {
    window.tampilkanToast = function(msg, bgClass) {
        var toastContainer = document.getElementById('toastContainerGlobal');
        if (!toastContainer) {
            document.body.insertAdjacentHTML('beforeend', '<div id="toastContainerGlobal" class="toast-container position-fixed top-0 end-0 p-4" style="z-index: 1090;"></div>');
            toastContainer = document.getElementById('toastContainerGlobal');
        }
        var toastId = 'toast_' + Date.now();
        var icon = 'fa-circle-info';
        var cls = bgClass || 'bg-primary';
        if(cls.indexOf('bg-danger') >= 0) icon = 'fa-triangle-exclamation';
        else if(cls.indexOf('bg-success') >= 0) icon = 'fa-circle-check';
        else if(cls.indexOf('bg-warning') >= 0) icon = 'fa-circle-exclamation';

        var safeMsg = (typeof window.pmbEscapeHtml === 'function') ? window.pmbEscapeHtml(msg) : String(msg || '');
        var safeClass = String(cls).replace(/[^a-zA-Z0-9_\-\s]/g, '');
        var html = '' +
            '<div id="' + toastId + '" class="toast align-items-center text-white border-0 shadow-lg ' + safeClass + ' rounded-4 mb-2" role="alert" aria-live="assertive" aria-atomic="true">' +
              '<div class="d-flex px-2 py-1">' +
                '<div class="toast-body fw-semibold fs-6 d-flex align-items-center">' +
                    '<i class="fas ' + icon + ' fs-4 me-3 opacity-75"></i> <span>' + safeMsg + '</span>' +
                '</div>' +
                '<button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>' +
              '</div>' +
            '</div>';
        toastContainer.insertAdjacentHTML('beforeend', html);
        var toastEl = document.getElementById(toastId);
        if (window.bootstrap && bootstrap.Toast) {
            var bsToast = new bootstrap.Toast(toastEl, { delay: 5000 });
            bsToast.show();
            toastEl.addEventListener('hidden.bs.toast', function() { toastEl.remove(); });
        } else {
            setTimeout(function() { if (toastEl && toastEl.parentNode) toastEl.parentNode.removeChild(toastEl); }, 5000);
        }
    };
}

if (typeof window.showConfirmModal !== 'function') {
    window.showConfirmModal = function(msg, callbackYes) {
        var modalId = 'globalConfirmModal<%=rnd%>';
        var modalEl = document.getElementById(modalId);
        if(modalEl) modalEl.remove();

        var safeMsg = (typeof window.pmbEscapeHtml === 'function') ? window.pmbEscapeHtml(msg) : String(msg || '');
        var btnId = 'btnConfirmYes' + modalId;
        var html = '' +
        '<div class="modal fade" id="' + modalId + '" tabindex="-1" style="z-index: 1085;">' +
            '<div class="modal-dialog modal-dialog-centered">' +
                '<div class="modal-content rounded-4 border-0 shadow-lg">' +
                    '<div class="modal-body p-5 text-center">' +
                        '<i class="fas fa-circle-question fa-4x text-primary mb-4"></i>' +
                        '<h5 class="fw-bold text-dark mb-4 lh-base">' + safeMsg + '</h5>' +
                        '<div class="d-flex justify-content-center gap-3">' +
                            '<button type="button" class="btn btn-light border rounded-pill px-4 fw-bold shadow-sm" data-bs-dismiss="modal"><%= _slBatal %></button>' +
                            '<button type="button" class="btn btn-primary rounded-pill px-5 fw-bold shadow-sm" id="' + btnId + '"><%= _slYaLanjutkan %></button>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>' +
        '</div>';
        
        document.body.insertAdjacentHTML('beforeend', html);
        modalEl = document.getElementById(modalId);
        var yesBtn = document.getElementById(btnId);
        var modalObj = (window.bootstrap && bootstrap.Modal) ? new bootstrap.Modal(modalEl) : null;
        if (yesBtn) {
            yesBtn.addEventListener('click', function() {
                if (modalObj) modalObj.hide();
                if(typeof callbackYes === 'function') callbackYes();
            });
        }
        modalEl.addEventListener('hidden.bs.modal', function() { modalEl.remove(); });
        if (modalObj) modalObj.show();
    };
}

// -----------------------------------------------------------------------------
// 1. LAZY LOADING RENDERER UNTUK KATALOG PRODI (AJAX -> JSON -> HTML)
// -----------------------------------------------------------------------------
window.loadKatalogProdiAjax<%=rnd%> = async () => {
    const container = document.getElementById('containerLazyProdi<%=rnd%>');
    if (!container) return;
    const taVal = document.getElementById('filterTA<%=rnd%>') ? encodeURIComponent(document.getElementById('filterTA<%=rnd%>').value) : '';

    try {
        const response = await fetch('<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_pilihan_prodi&baru=true&ta=' + taVal);
        const resJson = await response.json();
        if (resJson.status === 'success' && resJson.fakultasList) {
            let html = '<div class="accordion accordion-flush" id="accordionFakultas<%=rnd%>">';
            if (resJson.fakultasList.length === 0) {
                html += '<div class="text-center py-5 text-muted small"><i class="fas fa-folder-open fa-3x mb-3 opacity-20"></i><br><%= Common.getBahasaConfig("Tidak ada data Program Studi.") %></div>';
            }
            
            resJson.fakultasList.forEach((fakultas, fIdx) => {
                html += '<div class="accordion-item border-0 mb-3 rounded-4 shadow-sm overflow-hidden bg-white">' +
                        '<h2 class="accordion-header">' +
                            '<button class="accordion-button collapsed fw-bold py-3 px-4" type="button" data-bs-toggle="collapse" data-bs-target="#coll_f_' + fIdx + '">' +
                                '<i class="fas fa-building-columns me-3 text-primary small"></i>' + fakultas.nama +
                            '</button>' +
                        '</h2>' +
                        '<div id="coll_f_' + fIdx + '" class="accordion-collapse collapse" data-bs-parent="#accordionFakultas<%=rnd%>">' +
                          '<div class="accordion-body p-3 p-md-4 bg-light bg-opacity-25">';
                
                fakultas.jurusans.forEach((prodi) => {
                    html += '<div class="card prodi-card-<%=rnd%> mb-3 shadow-none border-light">' +
                                '<div class="card-body p-4">' +
                                    '<div class="d-flex justify-content-between align-items-center mb-3 border-bottom pb-2">' +
                                        '<h6 class="fw-bold text-dark mb-0"><i class="fas fa-graduation-cap text-success me-2 small"></i>' + prodi.jenjang + ' ' + prodi.nama + '</h6>' +
                                        '<span class="badge bg-primary bg-opacity-10 text-primary rounded-pill badge-custom"><%= Common.getBahasaConfig("Tersedia") %></span>' +
                                    '</div>' +
                                    '<div class="text-prodi-desc mb-4">' + prodi.deskripsi + '</div>' +
                                    '<div class="row g-3 small text-muted">' +
                                        '<div class="col-md-6"><i class="fas fa-calendar-check text-warning me-2 opacity-75"></i><strong><%= Common.getBahasaConfig("Gelombang") %>:</strong><br><span class="text-dark">' + (prodi.gelombangs.join(', ') || '-') + '</span></div>' +
                                        '<div class="col-md-6"><i class="fas fa-clock text-info me-2 opacity-75"></i><strong><%= Common.getBahasaConfig("Program") %>:</strong><br><span class="text-dark">' + (prodi.programs.join(', ') || '-') + '</span></div>' +
                                    '</div>' +
                                '</div>';
                    if (prodi.buttons.length > 0) {
                        html += '<div class="card-footer bg-light border-0 p-3 text-end">';
                        prodi.buttons.forEach(btn => {
                            let lnk = '<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&afiliasi=<%=kodeafiliasi%>&s=_pendaftaran_mahasiswa&baru=true&gelombangId=' + btn.gelId + '&jurusanId=' + prodi.id + '&paketId=' + btn.pktId;
                            html += '<button type="button" onclick="window.daftarGelombangByUrl(\'' + lnk + '\');" class="btn btn-primary btn-sm rounded-pill px-4 fw-bold shadow-sm me-2"><i class="fas fa-pen-to-square me-1"></i> ' + btn.label + '</button>';
                        });
                        html += '</div>';
                    }
                    html += '</div>';
                });
                html += '</div></div></div>'; 
            });
            container.innerHTML = html + '</div>';
        }
    } catch (e) { 
        console.error("Error Katalog:", e);
        container.innerHTML = '<div class="text-center p-5 text-danger small"><i class="fas fa-triangle-exclamation me-2"></i><%= Common.getBahasaConfig("Gagal memuat katalog Program Studi.") %></div>'; 
    }
};

// -----------------------------------------------------------------------------
// 2. FUNGSI MEMBUKA MODAL PENDAFTARAN (DARI KARTU PRODI / GELOMBANG)
// -----------------------------------------------------------------------------
window.daftarGelombangByUrl = async (fullUrl) => {
    if(typeof window.showLoadingPMB === 'function') window.showLoadingPMB();
    try {
        const response = await fetch(fullUrl);
        if (!response.ok) throw new Error("Gagal mengambil formulir");
        const content = await response.text();
        
        const modalId = 'modalDaftarPmbDinamis';
        if(document.getElementById(modalId)) document.getElementById(modalId).remove();
        const modalHtml = 
            '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">' +
                '<div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">' +
                    '<div class="modal-content shadow-lg border-0 rounded-4">' +
                        '<div class="modal-header bg-light border-0 py-3 px-4">' +
                            '<h5 class="modal-title fw-bold text-dark"><i class="fas fa-file-signature text-primary me-2"></i><%= Common.getBahasaConfig("Formulir Pendaftaran Mahasiswa Baru") %></h5>' +
                            '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>' +
                        '</div>' +
                        '<div class="modal-body p-0 bg-light">' + content + '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        const modalElement = document.getElementById(modalId);
        
        // Evaluasi ulang script JSP yang dikembalikan dari fetch
        const scriptsArray = Array.from(modalElement.getElementsByTagName('script'));
        for (let i = 0; i < scriptsArray.length; i++) {
            const oldScript = scriptsArray[i];
            if (oldScript.src && oldScript.src.includes('email-decode')) continue; 
            const scriptNode = document.createElement('script');
            var srcEff = oldScript.src || oldScript.getAttribute('data-rocketlazyloadscript') || '';
            Array.from(oldScript.attributes).forEach(attr => { if (attr.name.toLowerCase() !== 'type') scriptNode.setAttribute(attr.name, attr.value); });
            scriptNode.type = 'text/javascript';
            if (srcEff) { scriptNode.src = srcEff; document.body.appendChild(scriptNode); }
            else { scriptNode.text = oldScript.innerHTML; document.body.appendChild(scriptNode).parentNode.removeChild(scriptNode); }
        }

        if(typeof window.hideLoadingPMB === 'function') window.hideLoadingPMB();
        new bootstrap.Modal(modalElement).show();
        modalElement.addEventListener('hidden.bs.modal', function () { this.remove(); });
    } catch (error) {
        if(typeof window.hideLoadingPMB === 'function') window.hideLoadingPMB();
        tampilkanToast('<%= Common.getBahasaConfigJS("Terjadi kesalahan sistem saat memuat formulir pendaftaran. Silakan coba beberapa saat lagi.") %>', 'bg-danger text-white');
    }
};

window.daftarGelombang<%=rnd%> = async (idGelombang) => {
    const directUrl = '<%=linkPendaftaran%>&gelombangId=' + idGelombang;
    window.daftarGelombangByUrl(directUrl);
};

// -----------------------------------------------------------------------------
// 3. FUNGSI BAWAAN HALAMAN UTAMA (GELOMBANG & PENGUMUMAN)
// -----------------------------------------------------------------------------
const loadJenisSeleksi<%=rnd%> = async () => {
    const elSeleksi = document.getElementById('filterSeleksi<%=rnd%>');
    if (!elSeleksi) return;

    var reqObj = { "action": "daftar", "class": "<%=JenisSeleksi.class.getName()%>", "where1": "aktif = true or aktif is null", "order1": "asc", "sort1": "nama", "tanpaLogin": "true" };
    try {
        const res = await fetch('<%=Common.ROOT%>/Data?baru=true', {
            method: 'POST', headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' }, body: JSON.stringify(reqObj)
        });
        const textResponse = await res.text();
        if (textResponse.trim().startsWith("<")) throw new Error("Terblokir Firewall/Filter");
        
        const dataResponse = JSON.parse(textResponse);
        const data = dataResponse.data || [];
        let options = '<option value=""><%= Common.getBahasaConfig("Semua Jalur Seleksi") %></option>';
        data.forEach((row) => { options += '<option value="' + row.id + '">' + row.nama + '</option>'; });
        elSeleksi.innerHTML = options;
    } catch (e) { 
        elSeleksi.innerHTML = '<option value="">-- <%= Common.getBahasaConfig("Gagal memuat data seleksi") %> --</option>';
    }
};

const loadPengumuman<%=rnd%> = async (page = 1) => {
    const container = document.getElementById('listPengumuman<%=rnd%>');
    const inputCari = document.getElementById('cariPengumuman<%=rnd%>');
    if (!container) return;

    let keywordParam = '';
    if (inputCari && inputCari.value.trim() !== '') keywordParam = '&keyword=' + encodeURIComponent(inputCari.value.trim());
    container.innerHTML = '<div class="p-5 text-center text-muted small"><div class="spinner-border text-danger mb-3" style="width: 2rem; height: 2rem;" role="status"></div><br><span class="fw-bold"><%= Common.getBahasaConfig("Memuat informasi terkini...") %></span></div>';
    try {
        const url = '<%=linkPengumuman%>&target=calon_mahasiswa&page=' + page + '&rnd=<%=rnd%>' + keywordParam;
        const response = await fetch(url);
        if (response.ok) container.innerHTML = await response.text(); 
        else throw new Error("Gagal load pengumuman");
    } catch (error) { 
        container.innerHTML = '<div class="p-5 text-center text-danger small"><i class="fas fa-triangle-exclamation fa-3x mb-3 d-block opacity-50"></i> <strong class="d-block"><%= Common.getBahasaConfig("Koneksi Terputus") %></strong> <%= Common.getBahasaConfig("Gagal memuat pengumuman.") %></div>';
    }
};

const loadGelombang<%=rnd%> = async (page = 1) => {
    const container = document.getElementById('containerGelombang<%=rnd%>');
    if (!container) return;
    const ta = encodeURIComponent(document.getElementById('filterTA<%=rnd%>') ? document.getElementById('filterTA<%=rnd%>').value : '');
    const seleksiId = encodeURIComponent(document.getElementById('filterSeleksi<%=rnd%>') ? document.getElementById('filterSeleksi<%=rnd%>').value : '');
    const statusVal = encodeURIComponent(document.getElementById('filterStatus<%=rnd%>') ? document.getElementById('filterStatus<%=rnd%>').value : 'semua');
    
    container.innerHTML = '<div class="card card-custom-<%=rnd%> panel-bg-white-<%=rnd%> p-5 text-center"><div class="spinner-border text-primary mb-3" style="width: 3rem; height: 3rem;"></div><div class="text-muted fw-bold"><%= Common.getBahasaConfig("Sistem sedang memuat data jadwal pendaftaran...") %></div></div>';
    try {
        const url = '<%=linkGelombang%>&ta=' + ta + '&seleksi=' + seleksiId + '&status=' + statusVal + '&page=' + page + '&rnd=<%=rnd%>';
        const response = await fetch(url);
        if (response.ok) container.innerHTML = await response.text();
        else throw new Error("Gagal load gelombang");
    } catch (error) { 
        container.innerHTML = '<div class="card card-custom-<%=rnd%> panel-bg-white-<%=rnd%> p-5 text-center text-danger"><i class="fas fa-triangle-exclamation fa-3x mb-3 d-block opacity-50"></i><strong class="d-block mb-1"><%= Common.getBahasaConfig("Kesalahan Jaringan") %></strong><%= Common.getBahasaConfig("Gagal memuat jadwal gelombang pendaftaran.") %></div>';
    }
};

window.detailPengumuman<%=rnd%> = async (idPengumuman) => {
    if(typeof window.showLoadingPMB === 'function') window.showLoadingPMB();
    try {
        const url = '<%=linkPengumumanRinci%>&id=' + idPengumuman;
        const response = await fetch(url);
        const content = await response.text();
        
        const modalId = 'modalDetailPengumuman<%=rnd%>';
        if(document.getElementById(modalId)) document.getElementById(modalId).remove();
        const modalHtml = 
            '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true">' +
                '<div class="modal-dialog modal-dialog-centered modal-dialog-scrollable modal-lg">' +
                    '<div class="modal-content shadow-lg border-0 rounded-4">' +
                        '<div class="modal-header bg-light border-0 py-3 px-4">' +
                            '<h5 class="modal-title fw-bold text-dark"><i class="fas fa-circle-info text-primary me-2"></i><%= Common.getBahasaConfig("Rincian Pengumuman") %></h5>' +
                            '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>' +
                        '</div>' +
                        '<div class="modal-body p-4 p-md-5 bg-white">' + content + '</div>' +
                        '<div class="modal-footer border-0 bg-light px-4 py-3">' +
                            '<button type="button" class="btn btn-secondary rounded-pill fw-bold shadow-sm px-5" data-bs-dismiss="modal"><i class="fas fa-xmark me-2"></i><%= Common.getBahasaConfig("Tutup") %></button>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        if(typeof window.hideLoadingPMB === 'function') window.hideLoadingPMB();
        new bootstrap.Modal(document.getElementById(modalId)).show();
        document.getElementById(modalId).addEventListener('hidden.bs.modal', function () { this.remove(); });
    } catch (error) { 
        if(typeof window.hideLoadingPMB === 'function') window.hideLoadingPMB();
        tampilkanToast('<%= Common.getBahasaConfigJS("Gagal memuat detail pengumuman.") %>', 'bg-danger text-white');
    }
};

window.bukaModalLoginPMB = () => {
    const modalId = 'modalLoginPmbGlobal';
    if(document.getElementById(modalId)) document.getElementById(modalId).remove();
    
    let optTgl = '<option value=""><%= Common.getBahasaConfig("Tgl") %></option>';
    for(let i=1; i<=31; i++) optTgl += '<option value="' + (i<10?'0'+i:i) + '">' + i + '</option>';
    let optBln = '<option value=""><%= Common.getBahasaConfig("Bulan") %></option>';
    for(let i=1; i<=12; i++) optBln += '<option value="' + (i<10?'0'+i:i) + '">' + i + '</option>';
    let optThn = '<option value=""><%= Common.getBahasaConfig("Tahun") %></option>';
    const yr = new Date().getFullYear();
    for(let i=yr; i>=(yr-60); i--) optThn += '<option value="' + i + '">' + i + '</option>';

    const modalHtml = 
        '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true" style="z-index: 1060;">' +
            '<div class="modal-dialog modal-dialog-centered">' +
                '<div class="modal-content shadow-lg border-0 rounded-4 overflow-hidden">' +
                    '<div class="modal-header bg-primary text-white border-0 py-4 px-4 px-md-5">' +
                        '<h5 class="modal-title fw-bold fs-8"><i class="fas fa-user-circle me-3 text-warning"></i><%= _slLoginPmb %></h5>' +
                        '<button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>' +
                    '</div>' +
                    '<div class="modal-body p-4 p-md-5 bg-white">' +
                    
                        // SATU PENGINGAT UMUM YANG RAPI DAN BERSIH
                        '<div class="alert alert-warning p-3 mb-4 rounded-4 border-warning bg-warning bg-opacity-10">' +
                            '<div class="d-flex">' +
                                '<i class="fas fa-circle-exclamation text-warning fs-3 me-3 mt-1"></i>' +
                                '<div>' +
                                    '<h6 class="fw-bold text-dark mb-1"><%= _slPengingat %></h6>' +
                                    '<p class="small text-muted mb-0"><%= _slPesanBatas %></p>' +
                                '</div>' +
                            '</div>' +
                        '</div>' +
                        
                        '<p class="text-muted small mb-4 text-center lh-lg"><%= _slSilakan %></p>' +
                        '<form id="formLoginPmbGlobal" onsubmit="event.preventDefault(); window.prosesLoginPMBGlobal();">' +
                            '<div class="mb-4">' +
                                '<label class="form-label fw-bold small text-secondary"><i class="fas fa-id-card me-2 text-primary"></i><%= _slNoReg %></label>' +
                                '<input type="text" class="form-control form-control-lg shadow-none border-primary bg-light fw-bold text-center" id="loginNoRegGlobal" placeholder="<%= _slKetikNoReg %>" required>' +
                            '</div>' +
                            '<div class="mb-4">' +
                                '<label class="form-label fw-bold small text-secondary"><i class="fas fa-calendar-days me-2 text-primary"></i><%= _slTglLahir %></label>' +
                                '<div class="row g-2">' +
                                    '<div class="col-3"><select class="form-select form-select-lg shadow-none border-primary bg-light" id="loginTglGlobal" required>' + optTgl + '</select></div>' +
                                    '<div class="col-4"><select class="form-select form-select-lg shadow-none border-primary bg-light" id="loginBlnGlobal" required>' + optBln + '</select></div>' +
                                    '<div class="col-5"><select class="form-select form-select-lg shadow-none border-primary bg-light" id="loginThnGlobal" required>' + optThn + '</select></div>' +
                                '</div>' +
                            '</div>' +
                            '<div class="d-grid mt-5">' +
                                '<button type="submit" class="btn btn-primary btn-lg rounded-pill fw-bold shadow-sm py-3" id="btnSubmitLoginGlobal"><i class="fas fa-right-to-bracket me-2"></i><%= _slMasukSkrg %></button>' +
                            '</div>' +
                        '</form>' +
                    '</div>' +
                '</div>' +
            '</div>' +
        '</div>';
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    new bootstrap.Modal(document.getElementById(modalId)).show();
    document.getElementById(modalId).addEventListener('hidden.bs.modal', function () { this.remove(); });
};

window.prosesLoginPMBGlobal = async () => { 
    const btn = document.getElementById('btnSubmitLoginGlobal');
    const oriText = btn.innerHTML;
    
    // PERUBAHAN: Tangkap input asli, lalu paksa menjadi huruf kecil (lowercase)
    const noRegInput = document.getElementById('loginNoRegGlobal').value.trim();
    const noRegLower = noRegInput.toLowerCase();
    
    const tgl = document.getElementById('loginTglGlobal').value;
    const bln = document.getElementById('loginBlnGlobal').value;
    const thn = document.getElementById('loginThnGlobal').value;
    const dob = thn + "-" + bln + "-" + tgl;

    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status"></span><%= _slMemverif %>';
    btn.disabled = true;

    // PERUBAHAN: Gunakan fungsi SQL lower() pada nama kolom agar cocok dengan input lowercase
    const reqObj = { 
        action: "daftar", 
        class: "<%=BiodataCalonMahasiswa.class.getName()%>", 
        where1: "(lower(no_registrasi) = '" + noRegLower + "' OR lower(email) = '" + noRegLower + "' OR lower(nama) = '" + noRegLower + "') AND tanggal_lahir = '" + dob + "'",
        tanpaLogin: "true" 
    };

    try {
        const res = await fetch('<%=Common.ROOT%>/Data?baru=true', {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(reqObj)
        });
        const result = await res.json();
        
        if (result.data && result.data.length > 0) {
            const pmbId = result.data[0].id;
            const formBody = new URLSearchParams();
            formBody.append('auth_action', 'login');
            formBody.append('cama_id', pmbId);
            
            const loginRes = await fetch('<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=landing_page', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: formBody.toString()
            });
            
            const loginData = await loginRes.json();
            if (loginData.status === 'success') {
                window.location.reload();
            } else {
                tampilkanToast('<%= _slAksesDitolak %>', 'bg-danger text-white');
                btn.innerHTML = oriText; btn.disabled = false;
            }
        } else {
            tampilkanToast('<%= _slKombinasiSalah %>', 'bg-danger text-white');
            btn.innerHTML = oriText; btn.disabled = false;
        }
    } catch(e) {
        tampilkanToast('<%= _slKendalaJaringan %>', 'bg-danger text-white');
        btn.innerHTML = oriText; btn.disabled = false;
    }
};

// -----------------------------------------------------------------------------
// 4. PEMICU EVENT (TRIGGER) SAAT HALAMAN SELESAI DIMUAT
// -----------------------------------------------------------------------------
document.addEventListener("DOMContentLoaded", async () => {
    // Jalankan pemuatan filter Gelombang secara asynchronous
    if (typeof loadJenisSeleksi<%=rnd%> === 'function') await loadJenisSeleksi<%=rnd%>(); 
    
    // Mulai Fetching Konten Paralel
    if (typeof loadPengumuman<%=rnd%> === 'function') loadPengumuman<%=rnd%>(1);
    if (typeof loadGelombang<%=rnd%> === 'function') loadGelombang<%=rnd%>(1); 
    
    // Lazy Load Komponen Katalog Prodi (JSON -> HTML DOM)
    if (typeof window.loadKatalogProdiAjax<%=rnd%> === 'function') {
        window.loadKatalogProdiAjax<%=rnd%>();
    }
});
</script>