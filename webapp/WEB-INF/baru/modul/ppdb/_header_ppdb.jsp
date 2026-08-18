<%@page import="ais.database.model.sekolah.CalonSiswa"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    String rnd = Common.getGeneratedBarCode(7);
    CalonSiswa siswaLogged = Common.isLoginCalonSiswa(request);
    boolean isLoggedIn = (siswaLogged != null);

    // Ambil Data Konfigurasi Master Sekolah
    String judulNamaSekolah = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama();
    String Motto = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getMotto();
    String logo_Sekolah = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
%>

<style>
    /* Latar Belakang & Hero Banner */
    .page-bg-ppdb<%=rnd%> { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: url('<%=Common.ROOT%>/img/pmb_bg.jpg') center/cover no-repeat; opacity: 0.08; z-index: -999; pointer-events: none; }
    .hero-section-ppdb<%=rnd%> { background: linear-gradient(135deg, rgba(13, 110, 253, 0.9), rgba(10, 88, 202, 0.95)), url('<%=Common.ROOT%>/img/pmb_bg.jpg') center/cover no-repeat; color: white; border-radius: 0 0 2.5rem 2.5rem; box-shadow: 0 10px 30px rgba(0,0,0,0.15); position: relative; z-index: 2; border-bottom: 5px solid #ffc107; }
    
    /* Efek Gambar & Teks */
    .logo-shadow-ppdb<%=rnd%> { filter: drop-shadow(0px 8px 12px rgba(0,0,0,0.4)); transition: transform 0.3s ease; }
    .logo-shadow-ppdb<%=rnd%>:hover { transform: scale(1.05); }
    .text-shadow-ppdb<%=rnd%> { text-shadow: 2px 2px 8px rgba(0,0,0,0.5); }
    .badge-ppdb-custom<%=rnd%> { background: rgba(255, 255, 255, 0.15); backdrop-filter: blur(10px); border: 1px solid rgba(255, 255, 255, 0.3); letter-spacing: 1px; }
    
    /* Tombol Login */
    .btn-login-ppdb<%=rnd%> { transition: all 0.3s ease; font-weight: 700; letter-spacing: 0.5px; }
    .btn-login-ppdb<%=rnd%>:hover { transform: translateY(-3px); box-shadow: 0 8px 25px rgba(0,0,0,0.2) !important; background-color: #f8f9fa !important; color: #0d6efd !important; }
</style>

<div id="globalLoaderPPDB" class="d-none align-items-center justify-content-center" style="position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(0,0,0,0.7); z-index: 9999; backdrop-filter: blur(5px);">
    <div class="text-center text-white p-4 bg-dark bg-opacity-50 rounded-4 shadow-lg border border-secondary border-opacity-50">
        <div class="spinner-border mb-3 text-warning" style="width: 3.5rem; height: 3.5rem;" role="status"></div>
        <h5 class="fw-bold tracking-wide"><%= Common.getBahasaConfig("Mohon Tunggu Sebentar...") %></h5>
        <p class="small text-white-50 mb-0"><%= Common.getBahasaConfig("Sistem sedang memproses data Anda.") %></p>
    </div>
</div>

<div class="page-bg-ppdb<%=rnd%>"></div>

<div class="hero-section-ppdb<%=rnd%> pt-3 pb-5 mb-5 text-center">
    
    <div class="container text-end pt-2 pb-2" style="min-height: 40px;">
        <% if (!isLoggedIn) { %>
            <button class="btn btn-light text-primary rounded-pill shadow px-4 border-0 btn-login-ppdb<%=rnd%>" onclick="if(typeof window.bukaModalLoginPPDB === 'function') window.bukaModalLoginPPDB();">
                <i class="fas fa-sign-in-alt me-2"></i><%= Common.getBahasaConfig("Masuk Calon Siswa") %>
            </button>
        <% } %>
    </div>

    <div class="container pb-4 pt-2">
        <% if (logo_Sekolah != null && !logo_Sekolah.trim().isEmpty()) { %>
            <img src="<%= logo_Sekolah %>" alt="Logo Institusi" class="img-fluid mb-3 logo-shadow-ppdb<%=rnd%>" style="max-height: 140px; object-fit: contain;">
        <% } %>

        <h2 class="fw-bold text-white text-uppercase text-shadow-ppdb<%=rnd%> mb-2" style="letter-spacing: 1.5px;">
            <%= judulNamaSekolah != null ? judulNamaSekolah : "" %>
        </h2>
        
        <% if (Motto != null && !Motto.trim().isEmpty()) { %>
            <h5 class="fw-light text-warning fst-italic mb-4 text-shadow-ppdb<%=rnd%>">
                "<%= Motto %>"
            </h5>
        <% } else { %>
             <div class="mb-4"></div>
        <% } %>

        <div class="d-inline-block badge-ppdb-custom<%=rnd%> rounded-pill px-4 py-2 mb-3 shadow">
            <h3 class="fw-bold mb-0 text-white text-shadow-ppdb<%=rnd%>">
                <i class="fas fa-user-graduate me-2 text-warning"></i><%= Common.getBahasaConfig("Penerimaan Peserta Didik Baru (PPDB)") %>
            </h3>
        </div>

        <p class="lead fw-normal mb-2 text-white opacity-75 mx-auto" style="max-width: 850px; font-size: 1.15rem; line-height: 1.6;">
            <%= Common.getBahasaConfig("Mari mewujudkan masa depan gemilang bersama kami. Silakan telusuri informasi dan pilih jalur pendaftaran yang sesuai dengan Anda di bawah ini.") %>
        </p>
    </div>
</div>

<script>
window.showLoadingPPDB = () => {
    const loader = document.getElementById('globalLoaderPPDB');
    if(loader) { loader.classList.remove('d-none'); loader.classList.add('d-flex'); }
};

window.hideLoadingPPDB = () => {
    const loader = document.getElementById('globalLoaderPPDB');
    if(loader) { loader.classList.remove('d-flex'); loader.classList.add('d-none'); }
};

// =========================================================================
// FUNGSI POPUP LOGIN (MENGGUNAKAN NO REGISTRASI DAN TANGGAL LAHIR)
// =========================================================================
window.bukaModalLoginPPDB = () => {
    const modalId = 'modalLoginPPDBGlobal';
    if(document.getElementById(modalId)) document.getElementById(modalId).remove();
    
    let optTgl = '<option value=""><%= Common.getBahasaConfig("Tgl") %></option>';
    for(let i=1; i<=31; i++) optTgl += '<option value="' + (i<10?'0'+i:i) + '">' + i + '</option>';
    
    let optBln = '<option value=""><%= Common.getBahasaConfig("Bulan") %></option>';
    for(let i=1; i<=12; i++) optBln += '<option value="' + (i<10?'0'+i:i) + '">' + i + '</option>';
    
    let optThn = '<option value=""><%= Common.getBahasaConfig("Tahun") %></option>';
    const yr = new Date().getFullYear();
    for(let i=yr; i>=(yr-40); i--) optThn += '<option value="' + i + '">' + i + '</option>';
    
    const modalHtml = 
        '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true" style="z-index: 1060;">' +
            '<div class="modal-dialog modal-dialog-centered">' +
                '<div class="modal-content shadow-lg border-0 rounded-4 overflow-hidden">' +
                    '<div class="modal-header bg-primary text-white border-0 py-3 px-4">' +
                        '<h5 class="modal-title fw-bold"><i class="fas fa-sign-in-alt me-2 text-warning"></i><%= Common.getBahasaConfig("Masuk ke Dasbor Calon Siswa") %></h5>' +
                        '<button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>' +
                    '</div>' +
                    '<div class="modal-body p-4 p-md-5 bg-white">' +
                        '<p class="text-muted small mb-4 text-center"><%= Common.getBahasaConfig("Silakan masukkan Nomor Registrasi Anda sebagai Nama Pengguna (Username) dan Tanggal Lahir sebagai Kata Sandi (Password).") %></p>' +
                        '<form id="formLoginPPDBGlobal" onsubmit="event.preventDefault(); window.prosesLoginPPDBGlobal();">' +
                            '<div class="mb-4">' +
                                '<label class="form-label fw-bold small text-secondary"><i class="fas fa-id-card me-2"></i><%= Common.getBahasaConfig("Nomor Registrasi") %></label>' +
                                '<input type="text" class="form-control form-control-lg shadow-none border-primary bg-light" id="loginNoRegGlobal" placeholder="<%= Common.getBahasaConfig("Ketik Nomor Registrasi...") %>" required>' +
                            '</div>' +
                            '<div class="mb-4">' +
                                '<label class="form-label fw-bold small text-secondary"><i class="fas fa-birthday-cake me-2"></i><%= Common.getBahasaConfig("Tanggal Lahir (Sebagai Sandi)") %></label>' +
                                '<div class="row g-2">' +
                                    '<div class="col-3"><select class="form-select form-select-lg shadow-none border-primary bg-light" id="loginTglGlobal" required>' + optTgl + '</select></div>' +
                                    '<div class="col-4"><select class="form-select form-select-lg shadow-none border-primary bg-light" id="loginBlnGlobal" required>' + optBln + '</select></div>' +
                                    '<div class="col-5"><select class="form-select form-select-lg shadow-none border-primary bg-light" id="loginThnGlobal" required>' + optThn + '</select></div>' +
                                '</div>' +
                            '</div>' +
                            '<div class="d-grid mt-5">' +
                                '<button type="submit" class="btn btn-primary btn-lg rounded-pill fw-bold shadow-sm" id="btnSubmitLoginGlobal"><i class="fas fa-sign-in-alt me-2"></i><%= Common.getBahasaConfig("Masuk Sekarang") %></button>' +
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

window.prosesLoginPPDBGlobal = async () => {
    const btn = document.getElementById('btnSubmitLoginGlobal');
    const oriText = btn.innerHTML;
    
    const noReg = document.getElementById('loginNoRegGlobal').value.trim();
    const tgl = document.getElementById('loginTglGlobal').value;
    const bln = document.getElementById('loginBlnGlobal').value;
    const thn = document.getElementById('loginThnGlobal').value;
    const dob = thn + "-" + bln + "-" + tgl;
    
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status"></span><%= Common.getBahasaConfig("Memverifikasi Data...") %>';
    btn.disabled = true;
    
    const reqObj = { 
        action: "daftar", 
        class: "<%=CalonSiswa.class.getName()%>", 
        where1: "no_registrasi = '" + noReg + "' AND tanggal_lahir = '" + dob + "'",
        tanpaLogin: "true" 
    };
    
    try {
        const res = await fetch('<%=Common.ROOT%>/Data?baru=true', {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(reqObj)
        });
        const result = await res.json();
        
        if (result.data && result.data.length > 0) {
            const ppdbId = result.data[0].id;
            
            // Memanggil API Lokal di landing_page.jsp untuk mengatur Session
            const formBody = new URLSearchParams();
            formBody.append('auth_action', 'login');
            formBody.append('siswa_id', ppdbId);
            
            const loginRes = await fetch('<%=Common.ROOT%>/ppdb?hanya_tampil_jsp=true&p=ppdb&s=landing_page', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: formBody.toString()
            });
            
            const loginData = await loginRes.json();
            if (loginData.status === 'success') {
                window.location.reload(); 
            } else {
                if (typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Gagal membuat sesi pengguna. Silakan coba kembali.") %>', 'bg-danger text-white');
                btn.innerHTML = oriText;
                btn.disabled = false;
            }
        } else {
            if (typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Kombinasi Nomor Registrasi dan Tanggal Lahir tidak ditemukan di sistem. Mohon periksa kembali.") %>', 'bg-warning text-dark');
            btn.innerHTML = oriText;
            btn.disabled = false;
        }
    } catch(e) {
        if (typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Terjadi kendala jaringan saat melakukan proses verifikasi. Silakan coba kembali.") %>', 'bg-danger text-white');
        btn.innerHTML = oriText;
        btn.disabled = false;
    }
};

window.konfirmasiLogoutPPDB = () => {
    const modalId = 'modalLogoutPPDBGlobal';
    const exist = document.getElementById(modalId);
    if(exist) exist.remove();
    
    const modalHtml = 
    '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true" style="z-index: 1080;">' +
        '<div class="modal-dialog modal-dialog-centered">' +
            '<div class="modal-content border-0 rounded-4 shadow-lg">' +
                '<div class="modal-body p-4 text-center">' +
                    '<i class="fas fa-sign-out-alt fa-3x text-danger mb-3 mt-2"></i>' +
                    '<h5 class="fw-bold text-dark"><%= Common.getBahasaConfig("Konfirmasi Keluar") %></h5>' +
                    '<p class="text-muted mb-4"><%= Common.getBahasaConfig("Apakah Anda yakin ingin keluar dari sesi saat ini?") %></p>' +
                    '<div class="d-flex justify-content-center gap-2">' +
                        '<button type="button" class="btn btn-light rounded-pill px-4 fw-bold shadow-sm" data-bs-dismiss="modal"><%= Common.getBahasaConfig("Batal") %></button>' +
                        '<button type="button" class="btn btn-danger rounded-pill px-4 fw-bold shadow-sm" onclick="window.location.href=\'<%=Common.ROOT%>/ppdb?hanya_tampil_jsp=true&p=ppdb&s=landing_page&auth_action=logout\'"><%= Common.getBahasaConfig("Ya, Keluar") %></button>' +
                    '</div>' +
                '</div>' +
            '</div>' +
        '</div>' +
    '</div>';
    
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    const modalEl = document.getElementById(modalId);
    new bootstrap.Modal(modalEl).show();
    modalEl.addEventListener('hidden.bs.modal', function () { this.remove(); });
};
</script>