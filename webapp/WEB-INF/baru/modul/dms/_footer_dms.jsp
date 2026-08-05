<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    Boolean loggedObjFooter = (Boolean) request.getAttribute("DMS_LOGGED_IN");
    boolean loggedFooter = loggedObjFooter != null && loggedObjFooter.booleanValue();
%>
<footer class="dms-footer py-5">
    <div class="container">
        <div class="row g-4 align-items-center">
            <div class="col-lg-8">
                <h5 class="fw-bold text-white mb-2"><i class="fa fa-folder-tree me-2"></i>Portal Dokumen Manajemen Sistem</h5>
                <p class="mb-0">Menampilkan katalog dokumen resmi dari database agar mudah dicari, mudah dipahami, dan tetap aman saat proses download.</p>
            </div>
            <div class="col-lg-4 text-lg-end">
                <a href="#dokumen" class="btn btn-outline-light rounded-pill px-4"><i class="fa fa-arrow-up me-2"></i>Kembali ke Katalog</a>
            </div>
        </div>
    </div>
</footer>

<% if (!loggedFooter) { %>
<div class="modal fade dms-login-modal" id="dmsLoginModal" tabindex="-1" aria-labelledby="dmsLoginModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-lg">
        <div class="modal-content">
            <div class="row g-0">
                <div class="col-lg-5 dms-login-side p-4 p-lg-5 d-flex flex-column justify-content-between">
                    <div>
                        <div class="dms-login-icon mb-3"><i class="fa fa-right-to-bracket fa-xl"></i></div>
                        <h4 class="fw-bold mb-2" id="dmsLoginModalLabel">Login Portal Dokumen</h4>
                        <p class="mb-0 opacity-75">Masuk untuk menampilkan tombol download pada dokumen yang sudah memiliki lampiran.</p>
                    </div>
                    <div class="small opacity-75 mt-4"><i class="fa fa-shield-halved me-1"></i>Proses login mengikuti AJAX login sistem seperti login2.jsp.</div>
                </div>
                <div class="col-lg-7 p-4 p-lg-5 bg-white">
                    <div class="d-flex justify-content-between align-items-start mb-3">
                        <div>
                            <h5 class="fw-bold mb-1">Masukkan Akun</h5>
                            <div class="text-muted small">Gunakan akun mahasiswa, siswa, pegawai, dosen, atau pengguna sistem.</div>
                        </div>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div id="dmsLoginAlert" class="alert alert-warning d-none rounded-4"></div>
                    <form id="dmsLoginForm" autocomplete="off">
                        <div class="mb-3">
                            <label class="form-label fw-semibold"><%=Common.getBahasaConfig("Username")%></label>
                            <div class="input-group input-group-lg">
                                <span class="input-group-text bg-light"><i class="fa fa-user"></i></span>
                                <input type="text" class="form-control" name="username" id="dmsLoginUsername" required>
                            </div>
                        </div>
                        <div class="mb-3">
                            <label class="form-label fw-semibold"><%=Common.getBahasaConfig("Password")%></label>
                            <div class="input-group input-group-lg">
                                <span class="input-group-text bg-light"><i class="fa fa-key"></i></span>
                                <input type="password" class="form-control" name="password" id="dmsLoginPassword" required>
                            </div>
                        </div>
                        <div class="form-check mb-4">
                            <input class="form-check-input" type="checkbox" value="true" id="dmsLoginRemember" name="rememberMe">
                            <label class="form-check-label" for="dmsLoginRemember"><%=Common.getBahasaConfig("Ingat saya")%></label>
                        </div>
                        <button type="submit" id="dmsLoginSubmit" class="btn btn-dms-gradient btn-lg rounded-pill px-4 w-100">
                            <i class="fa fa-lock-open me-2"></i>Login dan Aktifkan Download
                        </button>
                    </form>
                    <div class="text-muted small mt-3">Jika login berhasil, halaman akan dimuat ulang dan tombol download akan muncul pada dokumen yang memiliki lampiran.</div>
                </div>
            </div>
        </div>
    </div>
</div>
<% } %>

<script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<script>
(function(){
    var baseUrl = document.body.getAttribute('data-dms-base-url') || '<%=Common.ROOT%>/document';
    var explorer = document.getElementById('dmsExplorer');
    var currentAkreditasi = '';
    var currentInduk = '';

    function encode(v){ return encodeURIComponent(v || ''); }
    function escapeHtml(value){
        return String(value || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
    }
    function showLoading(){
        if (explorer) {
            explorer.innerHTML = '<div class="dms-loading"><i class="fa fa-spinner fa-spin me-2"></i>Mengambil daftar dokumen...</div>';
        }
    }
    function syncCurrentFromRendered(){
        if (!explorer) return;
        var rendered = explorer.querySelector('[data-dms-rendered]');
        if (rendered) {
            currentAkreditasi = rendered.getAttribute('data-akreditasi') || '';
            currentInduk = rendered.getAttribute('data-induk') || '';
        }
    }
    function loadDms(akreditasi, induk, q){
        if (!explorer) return;
        currentAkreditasi = akreditasi || '';
        currentInduk = induk || '';
        showLoading();
        var url = baseUrl + '?service=1&action=list&akreditasi=' + encode(currentAkreditasi) + '&induk=' + encode(currentInduk) + '&q=' + encode(q || '');
        fetch(url, { credentials: 'same-origin' })
            .then(function(r){ return r.text(); })
            .then(function(html){ explorer.innerHTML = html; syncCurrentFromRendered(); })
            .catch(function(){ explorer.innerHTML = '<div class="alert alert-danger rounded-4 mb-0"><i class="fa fa-triangle-exclamation me-2"></i>Daftar dokumen belum dapat dimuat. Silakan muat ulang halaman.</div>'; });
    }

    document.addEventListener('click', function(e){
        var openItem = e.target.closest('[data-dms-folder]');
        if (openItem) {
            e.preventDefault();
            loadDms(openItem.getAttribute('data-dms-akreditasi') || '', openItem.getAttribute('data-dms-induk') || '', '');
            return;
        }
        var crumb = e.target.closest('[data-dms-crumb]');
        if (crumb) {
            e.preventDefault();
            loadDms(crumb.getAttribute('data-dms-akreditasi') || '', crumb.getAttribute('data-dms-induk') || '', '');
            return;
        }
        var loginBtn = e.target.closest('[data-dms-login-open]');
        if (loginBtn) {
            e.preventDefault();
            openLoginModal();
        }
    });

    document.addEventListener('submit', function(e){
        var form = e.target.closest('[data-dms-search-form]');
        if (form) {
            e.preventDefault();
            var input = form.querySelector('[name="q"]');
            loadDms(form.getAttribute('data-current-akreditasi') || currentAkreditasi, form.getAttribute('data-current-induk') || currentInduk, input ? input.value : '');
        }
    });

    var loginModalEl = document.getElementById('dmsLoginModal');
    var loginModal = null;
    function openLoginModal(){
        if (!loginModalEl || !window.bootstrap) return;
        loginModal = bootstrap.Modal.getOrCreateInstance(loginModalEl);
        clearLoginAlert();
        loginModal.show();
        setTimeout(function(){ var u = document.getElementById('dmsLoginUsername'); if (u) u.focus(); }, 250);
    }
    function showLoginAlert(message){
        var alert = document.getElementById('dmsLoginAlert');
        if (alert) {
            alert.classList.remove('d-none');
            alert.innerHTML = '<i class="fa fa-circle-exclamation me-2"></i>' + escapeHtml(message || 'Login belum berhasil.');
        }
    }
    function clearLoginAlert(){
        var alert = document.getElementById('dmsLoginAlert');
        if (alert) {
            alert.classList.add('d-none');
            alert.innerHTML = '';
        }
    }
    function setLoginLoading(loading){
        var btn = document.getElementById('dmsLoginSubmit');
        if (!btn) return;
        btn.disabled = !!loading;
        btn.innerHTML = loading ? '<i class="fa fa-spinner fa-spin me-2"></i>Memverifikasi...' : '<i class="fa fa-lock-open me-2"></i>Login dan Aktifkan Download';
    }
    function extractLoginMessageFromHtml(text){
        try {
            var div = document.createElement('div');
            div.innerHTML = text || '';
            var plain = (div.textContent || div.innerText || '').replace(/\s+/g, ' ').trim();
            if (plain.length > 0) {
                return plain.substring(0, 260);
            }
        } catch (e) {}
        return '';
    }

    var loginForm = document.getElementById('dmsLoginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', function(e){
            e.preventDefault();
            clearLoginAlert();
            var username = document.getElementById('dmsLoginUsername');
            var password = document.getElementById('dmsLoginPassword');
            var remember = document.getElementById('dmsLoginRemember');
            if (!username || !username.value.trim()) { showLoginAlert('Username harus diisi.'); return; }
            if (!password || !password.value.trim()) { showLoginAlert('Password harus diisi.'); return; }

            var payload = new URLSearchParams();
            payload.append('username', username.value.trim());
            payload.append('password', password.value);
            if (remember && remember.checked) payload.append('rememberMe', 'true');

            setLoginLoading(true);
            fetch('<%=Common.ROOT%>/login?action=ajax_login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
                credentials: 'include',
                body: payload.toString()
            }).then(function(response){
                return response.text().then(function(text){ return { ok: response.ok, text: text }; });
            }).then(function(res){
                var result = null;
                try {
                    result = JSON.parse(res.text);
                } catch(ex) {
                    result = { status: 'error', message: extractLoginMessageFromHtml(res.text) || 'Gagal memproses respon login.' };
                }
                if (!res.ok && result.status === 'success') {
                    result.status = 'error';
                    result.message = 'Login gagal diproses oleh server. Silakan coba lagi.';
                }
                if (result && result.status === 'success') {
                    if (remember && remember.checked && result.cookie_val) {
                        var maxAge = 15552000;
                        document.cookie = 'userinfo=' + encodeURIComponent(result.cookie_val) + '; max-age=' + maxAge + '; path=/';
                        document.cookie = 'userid=' + encodeURIComponent(username.value.trim()) + '; max-age=' + maxAge + '; path=/';
                    }
                    setLoginLoading(false);
                    if (loginModal) loginModal.hide();
                    if (window.Swal) {
                        Swal.fire({ icon:'success', title:'Berhasil', text: result.message || 'Login berhasil. Tombol download akan diaktifkan.', timer: 1100, showConfirmButton:false }).then(function(){ window.location.replace(baseUrl + '#dokumen'); });
                    } else {
                        window.location.replace(baseUrl + '#dokumen');
                    }
                } else {
                    setLoginLoading(false);
                    showLoginAlert((result && result.message) ? result.message : 'Nama pengguna atau kata sandi tidak valid.');
                }
            }).catch(function(){
                setLoginLoading(false);
                showLoginAlert('Gagal terhubung ke server login. Silakan coba lagi.');
            });
        });
    }
    syncCurrentFromRendered();
})();
</script>
</body>
</html>
