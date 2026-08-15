<%@page import="ais.database.model.Pendaftar"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.action.servlet.EbisnisPublicServlet"%>
<%@page import="ais.common.ebisnis.EBisnisCsrf"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
// ============================================================================================
// dashboard_ebisnis.jsp -- dashboard self-service Pendaftar ebisnis.id SETELAH login (kelola
// Brand, Toko & Mesin POS, Investor, Manajemen). Forward tunggal dari EbisnisPublicServlet.doGet
// (bukan URL langsung -- WEB-INF tidak bisa diakses langsung dari luar), yang sudah memastikan
// sesi Pendaftar ada sebelum forward ke sini; guard di bawah murni jaga-jaga kalau JSP ini
// ter-include/forward dari jalur lain di kemudian hari.
// ============================================================================================
Pendaftar pendaftar = (Pendaftar) session.getAttribute(EbisnisPublicServlet.SESSION_PENDAFTAR);
if (pendaftar == null) {
	response.sendRedirect(request.getContextPath() + "/ebisnis/");
	return;
}
String namaBisnis = StringEscapeUtils.escapeHtml(pendaftar.getNama());
String ebisnisCsrfToken = EBisnisCsrf.ensure(session);
%>
<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Dashboard - <%=namaBisnis%> | ebisnis.id</title>
<link rel="icon" href="<%=request.getContextPath()%>/img/logo_ebisnis.png">
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
<style>
    :root { --primary-color: #1E3A5F; --accent-color: #C0563D; }
    body { background: #f4f6f9; font-family: 'Segoe UI', Arial, sans-serif; }
    .navbar-ebisnis { background: linear-gradient(135deg, var(--primary-color), #13293D); }
    .stat-card { border: none; border-radius: 14px; box-shadow: 0 2px 10px rgba(0,0,0,0.06); }
    .stat-card .stat-icon { width: 52px; height: 52px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 1.4rem; color: #fff; }
    .stat-value { font-size: 1.8rem; font-weight: 800; color: var(--primary-color); }
    .nav-pills .nav-link.active { background-color: var(--primary-color); }
    .nav-pills .nav-link { color: var(--primary-color); font-weight: 600; }
    .panel-card { background: #fff; border-radius: 14px; box-shadow: 0 2px 10px rgba(0,0,0,0.06); padding: 1.75rem; }
    .kredensial-box { background: #fff8e6; border: 1px dashed var(--accent-color); border-radius: 10px; padding: 1rem; }
    table.table-ebisnis thead { background: #eef2f6; }
</style>
</head>
<body>

    <nav class="navbar navbar-dark navbar-ebisnis px-3 py-3 mb-4">
        <span class="navbar-brand fw-bold">
            <img src="<%=request.getContextPath()%>/img/logo_ebisnis.png" style="height:28px;width:28px;border-radius:8px;vertical-align:middle;margin-right:8px;">
            ebisnis.id -- <%=namaBisnis%>
        </span>
        <div>
            <a href="<%=request.getContextPath()%>/ebisnis/" class="btn btn-sm btn-outline-light me-2"><i class="fas fa-house me-1"></i><%= Common.getBahasaConfig("Beranda") %></a>
            <form method="post" action="<%=request.getContextPath()%>/ebisnis/auth/logout" class="d-inline">
                <input type="hidden" name="aksi" value="logout">
                <input type="hidden" name="_csrf" value="<%=ebisnisCsrfToken%>">
                <button type="submit" class="btn btn-sm btn-outline-light"><i class="fas fa-right-from-bracket me-1"></i><%= Common.getBahasaConfig("Keluar") %></button>
            </form>
        </div>
    </nav>

    <div class="container pb-5">

        <%-- Panel tenant program pendaftaran /pendaftaran: status READY/ACTIVE + trial + modul.
             HANYA tampil bila akun ini punya tenant (s=tenant_list) -- akun legacy tidak berubah. --%>
        <div id="panelTenant" class="alert d-none mb-4" role="status" style="border-left:4px solid #0b5ed7;background:#eff6ff">
            <div class="d-flex justify-content-between align-items-center flex-wrap gap-2">
                <div>
                    <b id="tenantNama">-</b> <span class="badge bg-primary" id="tenantStatus">-</span>
                    <div class="small text-secondary" id="tenantTrial"></div>
                    <div class="small" id="tenantModul"></div>
                    <div class="small text-warning" id="tenantModulRencana"></div>
                </div>
                <a class="btn btn-outline-primary btn-sm" href="<%= request.getContextPath() %>/ebisnis/auth/daftar?mode=tenant-baru">
                    <i class="fas fa-plus me-1"></i><%= Common.getBahasaConfig("Buat Tenant Baru") %>
                </a>
            </div>
        </div>

        <div class="row g-3 mb-4" id="ringkasanCards">
            <div class="col-6 col-md-3">
                <div class="stat-card p-3 d-flex align-items-center gap-3">
                    <div class="stat-icon" style="background:#1E3A5F;"><i class="fas fa-tags"></i></div>
                    <div><div class="stat-value" id="statBrand">-</div><div class="text-secondary small"><%= Common.getBahasaConfig("Brand") %></div></div>
                </div>
            </div>
            <div class="col-6 col-md-3">
                <div class="stat-card p-3 d-flex align-items-center gap-3">
                    <div class="stat-icon" style="background:#2E6F63;"><i class="fas fa-store"></i></div>
                    <div><div class="stat-value" id="statToko">-</div><div class="text-secondary small"><%= Common.getBahasaConfig("Toko / Gerai") %></div></div>
                </div>
            </div>
            <div class="col-6 col-md-3">
                <div class="stat-card p-3 d-flex align-items-center gap-3">
                    <div class="stat-icon" style="background:#B8860B;"><i class="fas fa-cash-register"></i></div>
                    <div><div class="stat-value" id="statMesin">-</div><div class="text-secondary small"><%= Common.getBahasaConfig("Mesin POS") %></div></div>
                </div>
            </div>
            <div class="col-6 col-md-3">
                <div class="stat-card p-3 d-flex align-items-center gap-3">
                    <div class="stat-icon" style="background:#C0563D;"><i class="fas fa-hand-holding-dollar"></i></div>
                    <div><div class="stat-value" id="statInvestor">-</div><div class="text-secondary small"><%= Common.getBahasaConfig("Investor") %></div></div>
                </div>
            </div>
        </div>

        <ul class="nav nav-pills mb-4 gap-2" id="ebisnisTab">
            <li class="nav-item"><button class="nav-link active" data-tab="brand"><i class="fas fa-tags me-1"></i><%= Common.getBahasaConfig("Brand") %></button></li>
            <li class="nav-item"><button class="nav-link" data-tab="toko"><i class="fas fa-store me-1"></i><%= Common.getBahasaConfig("Toko & Mesin POS") %></button></li>
            <li class="nav-item"><button class="nav-link" data-tab="investor"><i class="fas fa-hand-holding-dollar me-1"></i><%= Common.getBahasaConfig("Investor") %></button></li>
            <li class="nav-item"><button class="nav-link" data-tab="manajemen"><i class="fas fa-user-tie me-1"></i><%= Common.getBahasaConfig("Manajemen") %></button></li>
        </ul>

        <!-- ==================== TAB: BRAND ==================== -->
        <div class="panel-card ebisnis-panel" data-panel="brand">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h5 class="fw-bold mb-0"><%= Common.getBahasaConfig("Daftar Brand / Sub-Merek") %></h5>
                <button class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#modalBrand"><i class="fas fa-plus me-1"></i><%= Common.getBahasaConfig("Tambah Brand") %></button>
            </div>
            <div class="table-responsive">
                <table class="table table-ebisnis align-middle">
                    <thead><tr><th><%= Common.getBahasaConfig("Nama Brand") %></th><th><%= Common.getBahasaConfig("Status") %></th></tr></thead>
                    <tbody id="tabelBrand"><tr><td colspan="2" class="text-center text-secondary"><%= Common.getBahasaConfig("Memuat...") %></td></tr></tbody>
                </table>
            </div>
        </div>

        <!-- ==================== TAB: TOKO & MESIN POS ==================== -->
        <div class="panel-card ebisnis-panel d-none" data-panel="toko">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h5 class="fw-bold mb-0"><%= Common.getBahasaConfig("Daftar Toko / Gerai / Cafe") %></h5>
                <button class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#modalToko"><i class="fas fa-plus me-1"></i><%= Common.getBahasaConfig("Tambah Toko") %></button>
            </div>
            <div class="table-responsive">
                <table class="table table-ebisnis align-middle">
                    <thead><tr><th><%= Common.getBahasaConfig("Nama Toko") %></th><th><%= Common.getBahasaConfig("Brand") %></th><th><%= Common.getBahasaConfig("Kota") %></th><th><%= Common.getBahasaConfig("Mesin POS") %></th><th></th></tr></thead>
                    <tbody id="tabelToko"><tr><td colspan="5" class="text-center text-secondary"><%= Common.getBahasaConfig("Memuat...") %></td></tr></tbody>
                </table>
            </div>
        </div>

        <!-- ==================== TAB: INVESTOR ==================== -->
        <div class="panel-card ebisnis-panel d-none" data-panel="investor">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h5 class="fw-bold mb-0"><%= Common.getBahasaConfig("Daftar Investor") %></h5>
                <button class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#modalInvestor"><i class="fas fa-plus me-1"></i><%= Common.getBahasaConfig("Tambah Investor") %></button>
            </div>
            <div class="table-responsive">
                <table class="table table-ebisnis align-middle">
                    <thead><tr><th><%= Common.getBahasaConfig("Nama") %></th><th><%= Common.getBahasaConfig("Email") %></th><th><%= Common.getBahasaConfig("Userid Login") %></th><th></th></tr></thead>
                    <tbody id="tabelInvestor"><tr><td colspan="4" class="text-center text-secondary"><%= Common.getBahasaConfig("Memuat...") %></td></tr></tbody>
                </table>
            </div>
        </div>

        <!-- ==================== TAB: MANAJEMEN ==================== -->
        <div class="panel-card ebisnis-panel d-none" data-panel="manajemen">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h5 class="fw-bold mb-0"><%= Common.getBahasaConfig("Akun Manajemen (SDM/Payroll/Logistik/dst)") %></h5>
                <button class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#modalManajemen"><i class="fas fa-plus me-1"></i><%= Common.getBahasaConfig("Tambah Akun") %></button>
            </div>
            <div class="table-responsive">
                <table class="table table-ebisnis align-middle">
                    <thead><tr><th><%= Common.getBahasaConfig("Nama") %></th><th><%= Common.getBahasaConfig("Jabatan") %></th><th><%= Common.getBahasaConfig("Userid Login") %></th><th></th></tr></thead>
                    <tbody id="tabelManajemen"><tr><td colspan="4" class="text-center text-secondary"><%= Common.getBahasaConfig("Memuat...") %></td></tr></tbody>
                </table>
            </div>
        </div>

    </div>

    <!-- ==================== MODAL: Tambah Brand ==================== -->
    <div class="modal fade" id="modalBrand" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
        <div class="modal-header"><h5 class="modal-title fw-bold"><%= Common.getBahasaConfig("Tambah Brand") %></h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
        <div class="modal-body">
            <div class="ebisnis-modal-alert"></div>
            <form id="formBrand">
                <label class="form-label"><%= Common.getBahasaConfig("Nama Brand / Merek") %> *</label>
                <input type="text" name="nama" class="form-control mb-3" required maxlength="255">
                <button type="submit" class="btn btn-primary w-100"><i class="fas fa-plus me-2"></i><%= Common.getBahasaConfig("Simpan") %></button>
            </form>
        </div>
    </div></div></div>

    <!-- ==================== MODAL: Tambah Toko ==================== -->
    <div class="modal fade" id="modalToko" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
        <div class="modal-header"><h5 class="modal-title fw-bold"><%= Common.getBahasaConfig("Tambah Toko / Gerai / Cafe") %></h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
        <div class="modal-body">
            <div class="ebisnis-modal-alert"></div>
            <form id="formToko">
                <label class="form-label"><%= Common.getBahasaConfig("Nama Toko") %> *</label>
                <input type="text" name="nama" class="form-control mb-3" required maxlength="255">
                <label class="form-label"><%= Common.getBahasaConfig("Brand (opsional)") %></label>
                <select name="brandId" class="form-select mb-3" id="selectBrandToko">
                    <option value=""><%= Common.getBahasaConfig("-- Tanpa Brand --") %></option>
                </select>
                <label class="form-label"><%= Common.getBahasaConfig("Kota") %></label>
                <input type="text" name="kota" class="form-control mb-3" maxlength="100">
                <label class="form-label"><%= Common.getBahasaConfig("Alamat") %></label>
                <input type="text" name="alamat" class="form-control mb-3" maxlength="255">
                <label class="form-label"><%= Common.getBahasaConfig("Telepon") %></label>
                <input type="text" name="telp" class="form-control mb-3" maxlength="50">
                <button type="submit" class="btn btn-primary w-100"><i class="fas fa-plus me-2"></i><%= Common.getBahasaConfig("Simpan") %></button>
            </form>
        </div>
    </div></div></div>

    <!-- ==================== MODAL: Mesin POS per Toko ==================== -->
    <div class="modal fade" id="modalMesinPos" tabindex="-1"><div class="modal-dialog modal-lg"><div class="modal-content">
        <div class="modal-header"><h5 class="modal-title fw-bold"><i class="fas fa-cash-register me-2"></i><%= Common.getBahasaConfig("Mesin POS Toko:") %> <span id="mesinPosTokoNama"></span></h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
        <div class="modal-body">
            <div class="ebisnis-modal-alert"></div>
            <div class="table-responsive mb-3">
                <table class="table table-sm table-ebisnis align-middle">
                    <thead><tr><th><%= Common.getBahasaConfig("Nama Mesin") %></th><th><%= Common.getBahasaConfig("Userid") %></th><th><%= Common.getBahasaConfig("Status") %></th></tr></thead>
                    <tbody id="tabelMesinPos"></tbody>
                </table>
            </div>
            <form id="formMesinPos" class="border-top pt-3">
                <input type="hidden" name="tokoId" id="mesinPosTokoId">
                <label class="form-label"><%= Common.getBahasaConfig("Nama Mesin POS Baru") %> *</label>
                <div class="input-group">
                    <input type="text" name="nama" class="form-control" required maxlength="255" placeholder="<%= Common.getBahasaConfig("mis. Kasir 1, Kasir Depan") %>">
                    <button type="submit" class="btn btn-primary"><i class="fas fa-plus me-1"></i><%= Common.getBahasaConfig("Tambah Mesin") %></button>
                </div>
            </form>
            <div id="mesinPosKredensialBaru" class="kredensial-box mt-3 d-none"></div>
        </div>
    </div></div></div>

    <!-- ==================== MODAL: Tambah Investor ==================== -->
    <div class="modal fade" id="modalInvestor" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
        <div class="modal-header"><h5 class="modal-title fw-bold"><%= Common.getBahasaConfig("Tambah Investor") %></h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
        <div class="modal-body">
            <div class="ebisnis-modal-alert"></div>
            <form id="formInvestor">
                <label class="form-label"><%= Common.getBahasaConfig("Nama Investor") %> *</label>
                <input type="text" name="nama" class="form-control mb-3" required maxlength="255">
                <label class="form-label"><%= Common.getBahasaConfig("Email") %></label>
                <input type="email" name="email" class="form-control mb-3" maxlength="255">
                <label class="form-label"><%= Common.getBahasaConfig("Telepon") %></label>
                <input type="text" name="telp" class="form-control mb-3" maxlength="50">
                <button type="submit" class="btn btn-primary w-100"><i class="fas fa-plus me-2"></i><%= Common.getBahasaConfig("Simpan & Buat Akun Login") %></button>
            </form>
            <div id="investorKredensialBaru" class="kredensial-box mt-3 d-none"></div>
        </div>
    </div></div></div>

    <!-- ==================== MODAL: Tambah Akun Manajemen ==================== -->
    <div class="modal fade" id="modalManajemen" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
        <div class="modal-header"><h5 class="modal-title fw-bold"><%= Common.getBahasaConfig("Tambah Akun Manajemen") %></h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
        <div class="modal-body">
            <div class="ebisnis-modal-alert"></div>
            <form id="formManajemen">
                <label class="form-label"><%= Common.getBahasaConfig("Nama") %> *</label>
                <input type="text" name="nama" class="form-control mb-3" required maxlength="255">
                <label class="form-label"><%= Common.getBahasaConfig("Jabatan / Modul") %></label>
                <input type="text" name="jabatan" class="form-control mb-3" maxlength="100" placeholder="<%= Common.getBahasaConfig("mis. HRD, Finance, Logistik") %>">
                <button type="submit" class="btn btn-primary w-100"><i class="fas fa-plus me-2"></i><%= Common.getBahasaConfig("Simpan & Buat Akun Login") %></button>
            </form>
            <div id="manajemenKredensialBaru" class="kredensial-box mt-3 d-none"></div>
        </div>
    </div></div></div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
    <script>
    (function () {
        var CTX = '<%=request.getContextPath()%>';
        var CSRF = '<%=ebisnisCsrfToken%>';

        function escapeHtml(s) {
            var d = document.createElement('div');
            d.textContent = (s === null || s === undefined) ? '' : String(s);
            return d.innerHTML;
        }

        function panggil(s, data) {
            var payload = new URLSearchParams(data || {});
            payload.append('s', s);
            return fetch(CTX + '/ebisnis/auth/session', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8', 'X-CSRF-Token': CSRF },
                credentials: 'include',
                body: payload.toString()
            }).then(function (r) { return r.json(); });
        }

        function tampilkanAlert(container, jenis, pesan) {
            var area = container.querySelector('.ebisnis-modal-alert');
            if (area) { area.innerHTML = '<div class="alert alert-' + (jenis === 'sukses' ? 'success' : 'danger') + ' text-center py-2">' + escapeHtml(pesan) + '</div>'; }
        }

        function kredensialHtml(hasil) {
            return '<div class="fw-bold mb-2"><i class="fas fa-key me-2"></i><%= Common.getBahasaConfig("Kredensial login (tampil sekali, catat sekarang):") %></div>' +
                '<div>Userid: <b>' + escapeHtml(hasil.userid) + '</b></div>' +
                '<div class="mb-2">Password: <b>' + escapeHtml(hasil.password) + '</b></div>' +
                '<img src="https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=' + encodeURIComponent(hasil.qrData) + '" alt="QR Login" class="img-fluid" style="max-width:180px;">' +
                '<div class="small text-secondary mt-1"><%= Common.getBahasaConfig("Scan QR ini pada layar login untuk masuk otomatis.") %></div>';
        }

        // ---------------- RINGKASAN ----------------
        function muatRingkasan() {
            panggil('ringkasan').then(function (h) {
                if (h.status !== '00') { return; }
                document.getElementById('statBrand').textContent = h.jumlahBrand;
                document.getElementById('statToko').textContent = h.jumlahToko;
                document.getElementById('statMesin').textContent = h.jumlahMesinPos;
                document.getElementById('statInvestor').textContent = h.jumlahInvestor;
            });
        }

        // ---------------- TENANT (program /pendaftaran) ----------------
        function muatTenant() {
            panggil('tenant_list').then(function (h) {
                if (h.status !== '00' || !h.data || !h.data.length) { return; }
                var t = h.data[0]; // tenant pertama; multi-tenant memakai daftar yang sama
                var panel = document.getElementById('panelTenant');
                panel.classList.remove('d-none');
                document.getElementById('tenantNama').textContent = t.nama + ' (' + (t.slug || '-') + ')';
                document.getElementById('tenantStatus').textContent = t.status;
                document.getElementById('tenantTrial').textContent =
                    t.trialEnd ? ('Masa coba hingga: ' + t.trialEnd) : '';
                document.getElementById('tenantModul').textContent =
                    (t.modulAktif && t.modulAktif.length) ? ('Modul aktif: ' + t.modulAktif.join(', ')) : '';
                // JUJUR: modul PLANNED ditampilkan sbg "belum tersedia", TANPA tombol/link.
                document.getElementById('tenantModulRencana').textContent =
                    (t.modulBelumTersedia && t.modulBelumTersedia.length)
                        ? ('Dalam roadmap (belum tersedia): ' + t.modulBelumTersedia.join(', ')) : '';
            }).catch(function () { /* akun legacy tanpa tenant: panel tetap tersembunyi */ });
        }

        // ---------------- TAB SWITCH ----------------
        document.querySelectorAll('#ebisnisTab .nav-link').forEach(function (btn) {
            btn.addEventListener('click', function () {
                document.querySelectorAll('#ebisnisTab .nav-link').forEach(function (b) { b.classList.remove('active'); });
                btn.classList.add('active');
                var tab = btn.getAttribute('data-tab');
                document.querySelectorAll('.ebisnis-panel').forEach(function (p) {
                    p.classList.toggle('d-none', p.getAttribute('data-panel') !== tab);
                });
                if (tab === 'brand') { muatBrand(); }
                if (tab === 'toko') { muatToko(); }
                if (tab === 'investor') { muatInvestor(); }
                if (tab === 'manajemen') { muatManajemen(); }
            });
        });

        // ---------------- BRAND ----------------
        var daftarBrandCache = [];
        function muatBrand() {
            panggil('brand_list').then(function (h) {
                if (h.status !== '00') { return; }
                daftarBrandCache = h.data;
                var tbody = document.getElementById('tabelBrand');
                if (!h.data.length) { tbody.innerHTML = '<tr><td colspan="2" class="text-center text-secondary"><%= Common.getBahasaConfig("Belum ada brand.") %></td></tr>'; }
                else {
                    tbody.innerHTML = h.data.map(function (b) {
                        return '<tr><td>' + escapeHtml(b.nama) + '</td><td><span class="badge bg-' + (b.aktif ? 'success' : 'secondary') + '">' + (b.aktif ? 'Aktif' : 'Nonaktif') + '</span></td></tr>';
                    }).join('');
                }
                var sel = document.getElementById('selectBrandToko');
                sel.innerHTML = '<option value=""><%= Common.getBahasaConfig("-- Tanpa Brand --") %></option>' +
                    h.data.map(function (b) { return '<option value="' + b.id + '">' + escapeHtml(b.nama) + '</option>'; }).join('');
            });
        }
        document.getElementById('formBrand').addEventListener('submit', function (e) {
            e.preventDefault();
            var form = e.target;
            panggil('brand_tambah', new FormData(form)).then(function (h) {
                if (h.status === '00') {
                    bootstrap.Modal.getInstance(document.getElementById('modalBrand')).hide();
                    form.reset();
                    muatBrand(); muatRingkasan();
                } else {
                    tampilkanAlert(form.closest('.modal-body'), 'error', h.description);
                }
            });
        });

        // ---------------- TOKO ----------------
        function muatToko() {
            panggil('toko_list').then(function (h) {
                if (h.status !== '00') { return; }
                var tbody = document.getElementById('tabelToko');
                if (!h.data.length) { tbody.innerHTML = '<tr><td colspan="5" class="text-center text-secondary"><%= Common.getBahasaConfig("Belum ada toko.") %></td></tr>'; return; }
                tbody.innerHTML = h.data.map(function (t) {
                    return '<tr><td>' + escapeHtml(t.nama) + '</td><td>' + escapeHtml(t.brandNama || '-') + '</td><td>' + escapeHtml(t.kota || '-') + '</td>' +
                        '<td><span class="badge bg-info text-dark">' + t.jumlahMesinPos + ' mesin</span></td>' +
                        '<td><button class="btn btn-sm btn-outline-primary btn-kelola-mesin" data-id="' + t.id + '" data-nama="' + escapeHtml(t.nama) + '"><i class="fas fa-cash-register me-1"></i><%= Common.getBahasaConfig("Kelola Mesin POS") %></button></td></tr>';
                }).join('');
                tbody.querySelectorAll('.btn-kelola-mesin').forEach(function (btn) {
                    btn.addEventListener('click', function () { bukaMesinPos(btn.getAttribute('data-id'), btn.getAttribute('data-nama')); });
                });
            });
        }
        document.getElementById('formToko').addEventListener('submit', function (e) {
            e.preventDefault();
            var form = e.target;
            panggil('toko_tambah', new FormData(form)).then(function (h) {
                if (h.status === '00') {
                    bootstrap.Modal.getInstance(document.getElementById('modalToko')).hide();
                    form.reset();
                    muatToko(); muatRingkasan();
                } else {
                    tampilkanAlert(form.closest('.modal-body'), 'error', h.description);
                }
            });
        });

        // ---------------- MESIN POS ----------------
        function bukaMesinPos(tokoId, tokoNama) {
            document.getElementById('mesinPosTokoId').value = tokoId;
            document.getElementById('mesinPosTokoNama').textContent = tokoNama;
            document.getElementById('mesinPosKredensialBaru').classList.add('d-none');
            document.getElementById('formMesinPos').reset();
            document.getElementById('mesinPosTokoId').value = tokoId;
            muatMesinPos(tokoId);
            new bootstrap.Modal(document.getElementById('modalMesinPos')).show();
        }
        function muatMesinPos(tokoId) {
            panggil('mesin_pos_list', { tokoId: tokoId }).then(function (h) {
                var tbody = document.getElementById('tabelMesinPos');
                if (h.status !== '00' || !h.data.length) { tbody.innerHTML = '<tr><td colspan="3" class="text-center text-secondary"><%= Common.getBahasaConfig("Belum ada mesin POS.") %></td></tr>'; return; }
                tbody.innerHTML = h.data.map(function (p) {
                    return '<tr><td>' + escapeHtml(p.nama) + '</td><td>' + escapeHtml(p.userid) + '</td><td><span class="badge bg-' + (p.aktif ? 'success' : 'secondary') + '">' + (p.aktif ? 'Aktif' : 'Nonaktif') + '</span></td></tr>';
                }).join('');
            });
        }
        document.getElementById('formMesinPos').addEventListener('submit', function (e) {
            e.preventDefault();
            var form = e.target;
            panggil('mesin_pos_tambah', new FormData(form)).then(function (h) {
                if (h.status === '00') {
                    var box = document.getElementById('mesinPosKredensialBaru');
                    box.classList.remove('d-none');
                    box.innerHTML = kredensialHtml(h);
                    form.reset();
                    document.getElementById('mesinPosTokoId').value = form.querySelector('[name=tokoId]') ? form.querySelector('[name=tokoId]').value : '';
                    muatMesinPos(document.getElementById('mesinPosTokoId').value);
                    muatToko(); muatRingkasan();
                } else {
                    tampilkanAlert(form.closest('.modal-body'), 'error', h.description);
                }
            });
        });

        // ---------------- INVESTOR ----------------
        function muatInvestor() {
            panggil('investor_list').then(function (h) {
                if (h.status !== '00') { return; }
                var tbody = document.getElementById('tabelInvestor');
                if (!h.data.length) { tbody.innerHTML = '<tr><td colspan="4" class="text-center text-secondary"><%= Common.getBahasaConfig("Belum ada investor.") %></td></tr>'; return; }
                tbody.innerHTML = h.data.map(function (inv) {
                    return '<tr><td>' + escapeHtml(inv.nama) + '</td><td>' + escapeHtml(inv.email || '-') + '</td><td>' + escapeHtml(inv.userid) + '</td>' +
                        '<td><span class="badge bg-' + (inv.aktif ? 'success' : 'secondary') + '">' + (inv.aktif ? 'Aktif' : 'Nonaktif') + '</span></td></tr>';
                }).join('');
            });
        }
        document.getElementById('formInvestor').addEventListener('submit', function (e) {
            e.preventDefault();
            var form = e.target;
            panggil('investor_tambah', new FormData(form)).then(function (h) {
                if (h.status === '00') {
                    var box = document.getElementById('investorKredensialBaru');
                    box.classList.remove('d-none');
                    box.innerHTML = kredensialHtml(h);
                    form.reset();
                    muatInvestor(); muatRingkasan();
                } else {
                    tampilkanAlert(form.closest('.modal-body'), 'error', h.description);
                }
            });
        });

        // ---------------- MANAJEMEN ----------------
        function muatManajemen() {
            panggil('manajemen_list').then(function (h) {
                if (h.status !== '00') { return; }
                var tbody = document.getElementById('tabelManajemen');
                if (!h.data.length) { tbody.innerHTML = '<tr><td colspan="4" class="text-center text-secondary"><%= Common.getBahasaConfig("Belum ada akun manajemen.") %></td></tr>'; return; }
                tbody.innerHTML = h.data.map(function (a) {
                    return '<tr><td>' + escapeHtml(a.nama) + '</td><td>' + escapeHtml(a.jabatan || '-') + '</td><td>' + escapeHtml(a.userid) + '</td>' +
                        '<td><span class="badge bg-' + (a.aktif ? 'success' : 'secondary') + '">' + (a.aktif ? 'Aktif' : 'Nonaktif') + '</span></td></tr>';
                }).join('');
            });
        }
        document.getElementById('formManajemen').addEventListener('submit', function (e) {
            e.preventDefault();
            var form = e.target;
            panggil('manajemen_tambah', new FormData(form)).then(function (h) {
                if (h.status === '00') {
                    var box = document.getElementById('manajemenKredensialBaru');
                    box.classList.remove('d-none');
                    box.innerHTML = kredensialHtml(h);
                    form.reset();
                    muatManajemen(); muatRingkasan();
                } else {
                    tampilkanAlert(form.closest('.modal-body'), 'error', h.description);
                }
            });
        });

        muatRingkasan();
        muatTenant();
        muatBrand();
    })();
    </script>
</body>
</html>
