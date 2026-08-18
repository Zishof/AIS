<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // 1. SECURITY CHECK & PRIVILEGES
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        out.print("{\"status\":\"error\", \"message\":\""
                + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
        return;
    }
    String rnd = Common.getGeneratedBarCode(7);
    
    boolean edit = false;
    boolean add = false;
    boolean delete = false;
    boolean isAdmin = false;
    
    if(tbmuser != null && tbmuser.getUserId() != null){
        edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser);
        delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE, tbmuser);
        add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE, tbmuser);
        
        if(Common.getApakahAdminLain(tbmuser)){
            edit = true;
            delete = true;
            add = true;
            isAdmin = true;
        }
    }
%>

<style>
    /* CSS KUSTOM UNTUK MODAL 90% DESKTOP & 100% MOBILE */
    @media (min-width: 992px) {
        .modal-xl-custom-<%=rnd%> {
            max-width: 90%;
        }
    }
    @media (max-width: 991px) {
        .modal-xl-custom-<%=rnd%> {
            max-width: 100%;
            margin: 0;
        }
        .modal-xl-custom-<%=rnd%> .modal-content {
            min-height: 100vh;
            border-radius: 0;
        }
    }
    .table-hover tbody tr:hover { background-color: #f8f9fa; }
    .filter-label-<%=rnd%> { font-size: 0.8rem; font-weight: 600; color: #6c757d; margin-bottom: 0.2rem; }
    
    /* Efek hover dan ukuran foto diperbesar */
    .foto-avatar-<%=rnd%> {
        width: 75px; 
        height: 75px; 
        object-fit: cover;
        transition: transform 0.3s ease-in-out;
        cursor: pointer;
    }
    .foto-avatar-<%=rnd%>:hover {
        transform: scale(1.8); 
        z-index: 10;
        position: relative;
        box-shadow: 0 10px 20px rgba(0,0,0,0.3) !important;
    }
</style>

<div class="container-fluid py-4">
    <div class="row mb-4 align-items-center">
        <div class="col-md-6">
            <h4 class="fw-bold text-primary mb-1">
                <i class="fas fa-users me-2"></i><%= Common.getBahasaConfig("Manajemen Calon Mahasiswa") %>
            </h4>
            <p class="text-muted small mb-0"><%= Common.getBahasaConfig("Kelola biodata, pencarian, dan pendaftaran calon mahasiswa baru.") %></p>
        </div>
        <div class="col-md-6 text-md-end mt-3 mt-md-0">
            <% if(add) { %>
            <button type="button" class="btn btn-primary rounded-pill px-4 shadow-sm fw-bold" onclick="window.bukaModalFormCalonMahasiswa<%=rnd%>('')">
                <i class="fas fa-user-plus me-2"></i><%= Common.getBahasaConfig("Tambah Pendaftar Baru") %>
            </button>
            <% } %>
        </div>
    </div>

    <div class="card border-0 shadow-sm rounded-4 mb-4">
        <div class="card-header bg-white border-bottom py-3">
            <h6 class="mb-0 fw-bold text-secondary"><i class="fas fa-filter me-2 text-warning"></i><%= Common.getBahasaConfig("Filter Pencarian Lanjutan") %></h6>
        </div>
        <div class="card-body p-4 bg-light rounded-bottom-4">
            <div class="row g-3">
                <div class="col-md-3 col-sm-6">
                    <div class="filter-label-<%=rnd%>"><%= Common.getBahasaConfig("Tahun Akademik") %></div>
                    <select id="filterTA<%=rnd%>" class="form-select form-select-sm shadow-none border-secondary" onchange="window.resetAndLoadData<%=rnd%>()">
                        <option value=""><%= Common.getBahasaConfig("Semua TA") %></option>
                        <% if(Common.tahunAngkatans != null) { 
                            for(String ta : Common.tahunAngkatans) { %>
                            <option value="<%= ta %>" <%= ta.equals(Common.getCurrentTahunAkademik()) ? "selected" : "" %>><%= ta %></option>
                        <% }} %>
                    </select>
                </div>
                <div class="col-md-3 col-sm-6">
                    <div class="filter-label-<%=rnd%>"><%= Common.getBahasaConfig("Gelombang Pendaftaran") %></div>
                    <select id="filterGelombang<%=rnd%>" class="form-select form-select-sm shadow-none border-secondary" onchange="window.resetAndLoadData<%=rnd%>()">
                        <option value=""><%= Common.getBahasaConfig("Semua Gelombang") %></option>
                    </select>
                </div>
                <div class="col-md-3 col-sm-6">
                    <div class="filter-label-<%=rnd%>"><%= Common.getBahasaConfig("Pilihan Paket") %></div>
                    <select id="filterPaket<%=rnd%>" class="form-select form-select-sm shadow-none border-secondary" onchange="window.resetAndLoadData<%=rnd%>()">
                        <option value=""><%= Common.getBahasaConfig("Semua Paket") %></option>
                    </select>
                </div>
                <div class="col-md-3 col-sm-6">
                    <div class="filter-label-<%=rnd%>"><%= Common.getBahasaConfig("Jalur Seleksi") %></div>
                    <select id="filterSeleksi<%=rnd%>" class="form-select form-select-sm shadow-none border-secondary" onchange="window.resetAndLoadData<%=rnd%>()">
                        <option value=""><%= Common.getBahasaConfig("Semua Jalur Seleksi") %></option>
                    </select>
                </div>
                <div class="col-md-4 col-sm-6">
                    <div class="filter-label-<%=rnd%>"><%= Common.getBahasaConfig("Pilihan Program Studi") %></div>
                    <select id="filterProdi<%=rnd%>" class="form-select form-select-sm shadow-none border-secondary" onchange="window.resetAndLoadData<%=rnd%>()">
                        <option value=""><%= Common.getBahasaConfig("Semua Prodi Pilihan") %></option>
                    </select>
                </div>
                <div class="col-md-2 col-sm-6">
                    <div class="filter-label-<%=rnd%>"><%= Common.getBahasaConfig("Status Aktif") %></div>
                    <select id="filterAktif<%=rnd%>" class="form-select form-select-sm shadow-none border-secondary" onchange="window.resetAndLoadData<%=rnd%>()">
                        <option value="Y" selected><%= Common.getBahasaConfig("Aktif") %></option>
                        <option value="N"><%= Common.getBahasaConfig("Tidak Aktif / Batal") %></option>
                        <option value=""><%= Common.getBahasaConfig("Semua Status") %></option>
                    </select>
                </div>
                <div class="col-md-6 col-sm-12">
                    <div class="filter-label-<%=rnd%>"><%= Common.getBahasaConfig("Kata Kunci (Nama, No. Daftar, KTP, NISN)") %></div>
                    <div class="input-group input-group-sm">
                        <input type="text" id="searchKeyword<%=rnd%>" class="form-control shadow-none border-secondary" placeholder="<%= Common.getBahasaConfig("Ketik kata kunci lalu tekan enter...") %>">
                        <button type="button" class="btn btn-secondary px-3" onclick="window.resetAndLoadData<%=rnd%>()"><i class="fas fa-search"></i></button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="card border-0 shadow-sm rounded-4">
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover table-striped align-middle mb-0" style="min-width: 1200px;">
                    <thead class="table-light text-secondary border-bottom">
                        <tr>
                            <th width="4%" class="text-center py-3">No</th>
                            <th width="8%" class="text-center py-3"><i class="fas fa-camera text-primary"></i> <%= Common.getBahasaConfig("Foto") %></th>
                            <th width="14%" class="py-3"><%= Common.getBahasaConfig("No Daftar & Tanggal") %></th>
                            <th width="20%" class="py-3"><%= Common.getBahasaConfig("Nama Calon Mahasiswa") %></th>
                            <th width="13%" class="py-3"><%= Common.getBahasaConfig("Asal Sekolah") %></th>
                            <th width="13%" class="py-3"><%= Common.getBahasaConfig("Gel / Paket / Seleksi") %></th>
                            <th width="13%" class="py-3"><%= Common.getBahasaConfig("Pilihan Prodi") %></th>
                            <th width="5%" class="text-center py-3"><%= Common.getBahasaConfig("Status") %></th>
                            <th width="10%" class="text-center py-3"><i class="fas fa-cog text-primary"></i> <%= Common.getBahasaConfig("Aksi") %></th>
                        </tr>
                    </thead>
                    <tbody id="tabelDataCalonMhs<%=rnd%>">
                        <tr>
                            <td colspan="9" class="text-center py-5 text-muted">
                                <div class="spinner-border spinner-border-sm text-primary mb-2"></div><br>
                                <%= Common.getBahasaConfig("Memuat data calon mahasiswa...") %>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
        <div class="card-footer bg-white border-top py-3 d-flex justify-content-between align-items-center rounded-bottom-4">
            <small class="text-muted fw-semibold" id="infoPaging<%=rnd%>"><%= Common.getBahasaConfig("Menampilkan 0 data") %></small>
            <nav><ul class="pagination pagination-sm mb-0 shadow-sm" id="paginationData<%=rnd%>"></ul></nav>
        </div>
    </div>
</div>

<script>
window.masterModelClass<%=rnd%> = "<%=BiodataCalonMahasiswa.class.getName()%>";
window.currentPage<%=rnd%> = 1;
window.maxResult<%=rnd%> = 10;
window.totalData<%=rnd%> = 0;

/**
 * INIT DROPDOWN REFERENSI UNTUK FILTER
 */
window.loadReferensiFilter<%=rnd%> = function() {
    try {
        fetch('<%=Common.ROOT%>/Data?baru=true', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: "daftar", class: "ais.database.model.GelombangPendaftaran", where1: "(aktif = true or aktif is null)", order1: "asc", sort1: "nama", tanpaLogin: "true" })
        }).then(function(res){ return res.json(); }).then(function(res){ window.injectSelect<%=rnd%>('filterGelombang<%=rnd%>', res.data); });

        fetch('<%=Common.ROOT%>/Data?baru=true', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: "daftar", class: "ais.database.model.Paket", where1: "(aktif = true or aktif is null)", order1: "asc", sort1: "nama", tanpaLogin: "true" })
        }).then(function(res){ return res.json(); }).then(function(res){ window.injectSelect<%=rnd%>('filterPaket<%=rnd%>', res.data); });

        fetch('<%=Common.ROOT%>/Data?baru=true', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: "daftar", class: "ais.database.model.JenisSeleksi", where1: "(aktif = true or aktif is null)", order1: "asc", sort1: "nama", tanpaLogin: "true" })
        }).then(function(res){ return res.json(); }).then(function(res){ window.injectSelect<%=rnd%>('filterSeleksi<%=rnd%>', res.data); });

        fetch('<%=Common.ROOT%>/Data?baru=true', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: "daftar", class: "ais.database.model.Jurusan", where1: "(aktif = true or aktif is null)", order1: "asc", sort1: "nama", tanpaLogin: "true" })
        }).then(function(res){ return res.json(); }).then(function(res){ window.injectSelect<%=rnd%>('filterProdi<%=rnd%>', res.data); });

    } catch (e) { console.error(e); }
};

window.injectSelect<%=rnd%> = function(id, data) {
    var el = document.getElementById(id);
    if(!el || !data) return;
    var opt = el.innerHTML;
    for(var i=0; i<data.length; i++){
        opt += '<option value="' + data[i].id + '">' + data[i].nama + '</option>';
    }
    el.innerHTML = opt;
};

/**
 * BUKA POPUP LIHAT RINCI (HALAMAN _SUKSES_LOGIN)
 */
window.bukaModalLihatRinci<%=rnd%> = async function(idCama) {
    var modalId = 'modalRinciCama<%=rnd%>';
    var existingModal = document.getElementById(modalId);
    if(existingModal) existingModal.remove();

    // Endpoint menuju _sukses_login
    var endpoint = '<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_sukses_login&id=' + idCama;
    
    var titleText = '<%= Common.getBahasaConfigJS("Profil & Status Pendaftaran Calon Mahasiswa") %>';

    var modalHtml = 
        '<div class="modal fade" id="' + modalId + '" tabindex="-1" data-bs-backdrop="static" aria-hidden="true" style="z-index: 1055;">' +
            '<div class="modal-dialog modal-dialog-centered modal-dialog-scrollable modal-xl-custom-<%=rnd%>">' +
                '<div class="modal-content shadow-lg border-0 rounded-4">' +
                    '<div class="modal-header bg-light border-0 py-3 px-4">' +
                        '<h5 class="modal-title fw-bold text-dark">' +
                            '<i class="fas fa-user-circle text-primary me-2"></i>' + titleText +
                        '</h5>' +
                        '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>' +
                    '</div>' +
                    '<div class="modal-body p-0 bg-light" id="bodyModalRinci<%=rnd%>">' +
                        '<div class="text-center py-5">' +
                            '<div class="spinner-border text-primary" role="status"></div>' +
                            '<div class="mt-2 text-muted fw-bold"><%= Common.getBahasaConfig("Sedang Memuat Profil...") %></div>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>' +
        '</div>';
    
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    var modalElement = document.getElementById(modalId);
    new bootstrap.Modal(modalElement).show();

    try {
        var response = await fetch(endpoint);
        if(!response.ok) throw new Error("Gagal load profil");
        var htmlContent = await response.text();
        
        var bodyEl = document.getElementById('bodyModalRinci<%=rnd%>');
        bodyEl.innerHTML = htmlContent;

        // Mengeksekusi script bawaan halaman _sukses_login.jsp
        var scriptsArray = Array.from(bodyEl.getElementsByTagName('script')); 
        for (var i = 0; i < scriptsArray.length; i++) {
            var oldScript = scriptsArray[i];
            if (oldScript.src && oldScript.src.includes('email-decode')) continue; 
            var scriptNode = document.createElement('script');
            var srcEff = oldScript.src || oldScript.getAttribute('data-rocketlazyloadscript') || '';
            Array.from(oldScript.attributes).forEach(function(attr) {
                if (attr.name.toLowerCase() !== 'type') scriptNode.setAttribute(attr.name, attr.value);
            });
            scriptNode.type = 'text/javascript';
            if (srcEff) {
                scriptNode.src = srcEff;
                document.body.appendChild(scriptNode);
            } else {
                scriptNode.text = oldScript.innerHTML;
                document.body.appendChild(scriptNode).parentNode.removeChild(scriptNode);
            }
        }
    } catch (e) {
        document.getElementById('bodyModalRinci<%=rnd%>').innerHTML = '<div class="text-center py-5 text-danger"><i class="fas fa-exclamation-triangle fa-3x mb-3"></i><h5><%= Common.getBahasaConfig("Gagal memuat profil calon mahasiswa.") %></h5></div>';
    }

    modalElement.addEventListener('hidden.bs.modal', function () { 
        this.remove(); 
    });
};

/**
 * BUKA POPUP FORM PENDAFTARAN MAHASISWA (TAMBAH / EDIT BIODATA)
 */
window.bukaModalFormCalonMahasiswa<%=rnd%> = async function(idCama) {
    var modalId = 'modalFormCama<%=rnd%>';
    var existingModal = document.getElementById(modalId);
    if(existingModal) existingModal.remove();

    var endpoint = '<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_pendaftaran_mahasiswa&baru=true';
    var titleText = '<%= Common.getBahasaConfigJS("Pendaftaran Mahasiswa Baru") %>';
    
    if(idCama && idCama !== '') {
        endpoint += '&id=' + idCama;
        titleText = '<%= Common.getBahasaConfigJS("Ubah Data Calon Mahasiswa") %>';
    }

    var modalHtml = 
        '<div class="modal fade" id="' + modalId + '" tabindex="-1" data-bs-backdrop="static" aria-hidden="true" style="z-index: 1055;">' +
            '<div class="modal-dialog modal-dialog-centered modal-dialog-scrollable modal-xl-custom-<%=rnd%>">' +
                '<div class="modal-content shadow-lg border-0 rounded-4">' +
                    '<div class="modal-header bg-light border-0 py-3 px-4">' +
                        '<h5 class="modal-title fw-bold text-dark">' +
                            '<i class="fas fa-file-signature text-primary me-2"></i>' + titleText +
                        '</h5>' +
                        '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close" onclick="window.loadDataCalonMahasiswa<%=rnd%>()"></button>' +
                    '</div>' +
                    '<div class="modal-body p-0 bg-light" id="bodyModalCama<%=rnd%>">' +
                        '<div class="text-center py-5">' +
                            '<div class="spinner-border text-primary" role="status"></div>' +
                            '<div class="mt-2 text-muted fw-bold"><%= Common.getBahasaConfig("Sedang Memuat Formulir...") %></div>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>' +
        '</div>';
    
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    var modalElement = document.getElementById(modalId);
    new bootstrap.Modal(modalElement).show();

    try {
        var response = await fetch(endpoint);
        if(!response.ok) throw new Error("Gagal load formulir");
        var htmlContent = await response.text();
        
        var bodyEl = document.getElementById('bodyModalCama<%=rnd%>');
        bodyEl.innerHTML = htmlContent;

        var scriptsArray = Array.from(bodyEl.getElementsByTagName('script')); 
        for (var i = 0; i < scriptsArray.length; i++) {
            var oldScript = scriptsArray[i];
            if (oldScript.src && oldScript.src.includes('email-decode')) continue; 
            var scriptNode = document.createElement('script');
            var srcEff = oldScript.src || oldScript.getAttribute('data-rocketlazyloadscript') || '';
            Array.from(oldScript.attributes).forEach(function(attr) {
                if (attr.name.toLowerCase() !== 'type') scriptNode.setAttribute(attr.name, attr.value);
            });
            scriptNode.type = 'text/javascript';
            if (srcEff) {
                scriptNode.src = srcEff;
                document.body.appendChild(scriptNode);
            } else {
                scriptNode.text = oldScript.innerHTML;
                document.body.appendChild(scriptNode).parentNode.removeChild(scriptNode);
            }
        }
    } catch (e) {
        document.getElementById('bodyModalCama<%=rnd%>').innerHTML = '<div class="text-center py-5 text-danger"><i class="fas fa-exclamation-triangle fa-3x mb-3"></i><h5><%= Common.getBahasaConfig("Gagal memuat formulir pendaftaran.") %></h5></div>';
    }

    modalElement.addEventListener('hidden.bs.modal', function () { 
        this.remove(); 
        window.loadDataCalonMahasiswa<%=rnd%>(); 
    });
};

/**
 * MUNCULKAN PDF KE DALAM MODAL
 */
window.bukaModalCetakPDF<%=rnd%> = async function(idCama) {
    const existingModal = document.getElementById('modalCetakPDF<%=rnd%>');
    if(existingModal) existingModal.remove();
    
    if(typeof tampilkanToast === 'function') {
        tampilkanToast('<%= Common.getBahasaConfigJS("Sedang menyiapkan dokumen PDF...") %>', 'bg-info text-white');
    }
    
    try {
        let urlService = '<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_cetak_kartu_pendaftaran&id=' + idCama;
        const response = await fetch(urlService);
        const data = await response.json();
        
        if(data.status === 'success' && data.url) {
            var modalHtml = 
            '<div class="modal fade" id="modalCetakPDF<%=rnd%>" tabindex="-1" aria-hidden="true" style="z-index: 1080;">' +
                '<div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">' +
                    '<div class="modal-content rounded-4 border-0 shadow">' +
                        '<div class="modal-header bg-light border-0 py-3">' +
                            '<h5 class="modal-title fw-bold text-primary"><i class="fas fa-file-pdf me-2"></i><%= Common.getBahasaConfig("Kartu Pendaftaran") %></h5>' +
                            '<button type="button" class="btn-close" data-bs-dismiss="modal"></button>' +
                        '</div>' +
                        '<div class="modal-body p-0" style="height: 80vh;">' +
                            '<iframe src="' + data.url + '" style="width:100%; height:100%; border:none;"></iframe>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
            
            document.body.insertAdjacentHTML('beforeend', modalHtml);
            let modalEl = document.getElementById('modalCetakPDF<%=rnd%>');
            let modalObj = new bootstrap.Modal(modalEl);
            modalObj.show();
            
            modalEl.addEventListener('hidden.bs.modal', function () { 
                this.remove(); 
            });
        } else {
            if(typeof tampilkanToast === 'function') tampilkanToast(data.message || '<%= Common.getBahasaConfigJS("Gagal membuat PDF") %>', 'bg-danger text-white');
        }
    } catch (e) {
        console.error(e);
        if(typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Terjadi kesalahan koneksi saat mencetak PDF") %>', 'bg-danger text-white');
    }
};

/**
 * LOGIKA LOAD DATA TABEL
 */
window.loadDataCalonMahasiswa<%=rnd%> = async function() {
    var tbody = document.getElementById('tabelDataCalonMhs<%=rnd%>');
    var infoPaging = document.getElementById('infoPaging<%=rnd%>');
    var pagination = document.getElementById('paginationData<%=rnd%>');

    tbody.innerHTML = '<tr><td colspan="9" class="text-center py-5 text-muted"><div class="spinner-border spinner-border-sm text-primary mb-2"></div><br><%= Common.getBahasaConfig("Mengambil data...") %></td></tr>';

    var whereClauses = [];
    
    var valTA = document.getElementById('filterTA<%=rnd%>').value;
    if(valTA) whereClauses.push("tahunAkademik = '" + valTA + "'");

    var valGel = document.getElementById('filterGelombang<%=rnd%>').value;
    if(valGel) whereClauses.push("gelombang_pendaftaran = " + valGel);

    var valPaket = document.getElementById('filterPaket<%=rnd%>').value;
    if(valPaket) whereClauses.push("paket_registrasi_mahasiswa = " + valPaket);

    var valSel = document.getElementById('filterSeleksi<%=rnd%>').value;
    if(valSel) whereClauses.push("jenis_seleksi = " + valSel);

    var valProdi = document.getElementById('filterProdi<%=rnd%>').value;
    if(valProdi) whereClauses.push("(prodi_1 = " + valProdi + " OR prodi_2 = " + valProdi + " OR prodi3 = " + valProdi + " OR prodi4 = " + valProdi + " OR prodi5 = " + valProdi + ")");

    var valAktif = document.getElementById('filterAktif<%=rnd%>').value;
    if(valAktif === 'Y') whereClauses.push("(aktif = true OR aktif IS NULL)");
    else if(valAktif === 'N') whereClauses.push("aktif = false");

    var keyword = document.getElementById('searchKeyword<%=rnd%>').value.trim();
    if(keyword) {
        var kw = keyword.toLowerCase().replace(/'/g, "''");
        whereClauses.push("(no_registrasi ILIKE '%" + kw + "%' OR nama ILIKE '%" + kw + "%' OR no_identitas ILIKE '%" + kw + "%' OR nisn ILIKE '%" + kw + "%')");
    }

    var where1Str = whereClauses.length > 0 ? whereClauses.join(' AND ') : '1=1';
    
    var offset = (window.currentPage<%=rnd%> - 1) * window.maxResult<%=rnd%>;

    var requestBody = {
        action: "daftar",
        class: window.masterModelClass<%=rnd%>,
        where1: where1Str,
        order1: "desc",
        sort1: "id", 
        max: window.maxResult<%=rnd%>,
        maxResults: window.maxResult<%=rnd%>,
        first: offset,
        firstResult: offset,
        count: "true",
        deep: 2
    };

    try {
        var res = await fetch('<%=Common.ROOT%>/Data?baru=true', {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(requestBody)
        });
        var dataRes = await res.json();
        var list = dataRes.data || [];
        
        // Pengecekan ketat untuk total row yang dikembalikan AIS
        window.totalData<%=rnd%> = parseInt(dataRes.totalData || dataRes.total || dataRes.count || dataRes.total_data) || 0;
        if(window.totalData<%=rnd%> === 0 && list.length > 0) {
            window.totalData<%=rnd%> = list.length; 
        }

        if (list.length === 0) {
            tbody.innerHTML = '<tr><td colspan="9" class="text-center py-5"><i class="fas fa-folder-open fa-3x text-muted opacity-50 mb-3 d-block"></i><h6 class="text-muted fw-bold"><%= Common.getBahasaConfig("Tidak ada data ditemukan") %></h6></td></tr>';
            infoPaging.innerHTML = "<%= Common.getBahasaConfig("Menampilkan 0 data") %>";
            pagination.innerHTML = '';
            return;
        }

        var html = '';
        for(var i=0; i<list.length; i++) {
            var item = list[i];
            var num = offset + i + 1;
            
            var noDaftar = item["noRegistrasi"] ? item["noRegistrasi"] : '-';
            var tglDaftar = item["tanggalPendaftaran.formated5"] ? item["tanggalPendaftaran.formated5"] : '-';
            var nama = item.nama ? item.nama : '-';
            
            var jk = item.jenisKelamin ? (item.jenisKelamin.toLowerCase().includes('laki') ? '<i class="fas fa-mars text-primary" title="Laki-laki"></i>' : '<i class="fas fa-venus text-danger" title="Perempuan"></i>') : '';
            var iden = item.noIdentitas ? '<br><small class="text-muted"><i class="fas fa-id-card me-1"></i> ' + item.noIdentitas + '</small>' : '';
            
            var sekolah = item["namaSekolahAsal.nama"] ? item["namaSekolahAsal.nama"] : '-';
            var namaGel = item["gelombangPendaftaran.nama"] ? item["gelombangPendaftaran.nama"] : '-';
            var namaPaket = item["paket.nama"] ? item["paket.nama"] : '-';
            var namaSel = item["jenisSeleksi.nama"] ? item["jenisSeleksi.nama"] : '-';
            var prodi1 = item["prodi1.nama"] ? item["prodi1.nama"] : '-';

            var statusBadge = (item.aktif !== false && item.aktif !== 'false') ? 
                '<span class="badge bg-success rounded-pill px-2 py-1"><i class="fas fa-check-circle me-1"></i>Aktif</span>' : 
                '<span class="badge bg-danger rounded-pill px-2 py-1"><i class="fas fa-times-circle me-1"></i>Batal</span>';

            // TOMBOL AKSI (WAJIB type="button")
            var aksiLihat = '<button type="button" class="btn btn-sm btn-success shadow-sm rounded-circle me-1" onclick="window.bukaModalLihatRinci<%=rnd%>(\'' + item.id + '\')" title="<%= Common.getBahasaConfig("Lihat Rincian Calon Mahasiswa") %>"><i class="fas fa-eye text-white"></i></button>';
            var aksiCetak = '<button type="button" class="btn btn-sm btn-outline-info shadow-sm rounded-circle me-1" onclick="window.bukaModalCetakPDF<%=rnd%>(\'' + item.id + '\')" title="<%= Common.getBahasaConfig("Cetak Kartu / Bukti") %>"><i class="fas fa-print"></i></button>';
            var aksiEdit = <%= edit %> ? '<button type="button" class="btn btn-sm btn-outline-primary shadow-sm rounded-circle me-1" onclick="window.bukaModalFormCalonMahasiswa<%=rnd%>(\'' + item.id + '\')" title="<%= Common.getBahasaConfig("Ubah Data") %>"><i class="fas fa-edit"></i></button>' : '';
            var aksiHapus = <%= delete %> ? '<button type="button" class="btn btn-sm btn-outline-danger shadow-sm rounded-circle" onclick="window.hapusCalonMahasiswa<%=rnd%>(\'' + item.id + '\')" title="<%= Common.getBahasaConfig("Hapus / Nonaktifkan") %>"><i class="fas fa-trash-alt"></i></button>' : '';

            var urlFoto = item.foto_kecil ? item.foto_kecil : '<%=Common.ROOT%>/img/default-avatar.png';
            var fotoHtml = '<div class="text-center">' +
                              '<img id="foto_mhs_' + item.id + '_<%=rnd%>" src="' + urlFoto + '" onerror="this.src=\'<%=Common.ROOT%>/img/default-avatar.png\'" onclick="if(typeof tampilGambar === \'function\') tampilGambar(this);" class="img-thumbnail rounded-circle shadow-sm foto-avatar-<%=rnd%>">' +
                           '</div>';

            html += '<tr>' +
                        '<td class="text-center fw-bold text-secondary">' + num + '</td>' +
                        '<td class="text-center">' + fotoHtml + '</td>' +
                        '<td>' +
                            '<span class="fw-bold text-primary">' + noDaftar + '</span><br>' +
                            '<small class="text-muted"><i class="far fa-calendar-alt me-1"></i>' + tglDaftar + '</small>' +
                        '</td>' +
                        '<td><span class="fw-semibold text-dark">' + nama + ' ' + jk + '</span>' + iden + '</td>' +
                        '<td><span class="small text-secondary">' + sekolah + '</span></td>' +
                        '<td>' +
                            '<div class="small mb-1"><strong>Gel:</strong> ' + namaGel + '</div>' +
                            '<div class="small mb-1 text-success"><strong>Pkt:</strong> ' + namaPaket + '</div>' +
                            '<div class="small text-info"><strong>Sel:</strong> ' + namaSel + '</div>' +
                        '</td>' +
                        '<td><span class="badge bg-light text-dark border border-secondary">' + prodi1 + '</span></td>' +
                        '<td class="text-center">' + statusBadge + '</td>' +
                        '<td class="text-center text-nowrap">' + aksiLihat + aksiCetak + aksiEdit + aksiHapus + '</td>' +
                    '</tr>';
        }
        
        tbody.innerHTML = html;
        window.renderPagination<%=rnd%>();
        
        var startRow = offset + 1;
        var endRow = Math.min(startRow + window.maxResult<%=rnd%> - 1, window.totalData<%=rnd%>);
        infoPaging.innerHTML = '<%= Common.getBahasaConfigJS("Menampilkan") %> ' + startRow + ' - ' + endRow + ' <%= Common.getBahasaConfig("dari total") %> ' + window.totalData<%=rnd%> + ' <%= Common.getBahasaConfig("data") %>';

    } catch (error) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center py-5 text-danger"><i class="fas fa-exclamation-triangle fa-2x mb-2 d-block"></i><%= Common.getBahasaConfig("Gagal memuat data dari peladen.") %></td></tr>';
    }
};

window.resetAndLoadData<%=rnd%> = function() {
    window.currentPage<%=rnd%> = 1;
    window.loadDataCalonMahasiswa<%=rnd%>();
};

// WAJIB menggunakan type="button" untuk mencegah form submit di pagination
window.renderPagination<%=rnd%> = function() {
    var pagination = document.getElementById('paginationData<%=rnd%>');
    var totalPages = Math.ceil(window.totalData<%=rnd%> / window.maxResult<%=rnd%>);
    if (totalPages <= 1) { pagination.innerHTML = ''; return; }

    var pHtml = '';
    var prevDisabled = window.currentPage<%=rnd%> === 1 ? 'disabled' : '';
    pHtml += '<li class="page-item ' + prevDisabled + '"><a href="javascript:void(0);" class="page-link rounded-start-pill" onclick="window.changePage<%=rnd%>(' + (window.currentPage<%=rnd%> - 1) + ')"><i class="fas fa-chevron-left"></i></a></li>';

    for (var i = 1; i <= totalPages; i++) {
        if (i === 1 || i === totalPages || Math.abs(i - window.currentPage<%=rnd%>) <= 2) {
            var activeClass = window.currentPage<%=rnd%> === i ? 'active' : '';
            var activeBtnClass = window.currentPage<%=rnd%> === i ? 'bg-primary text-white border-primary' : '';
            pHtml += '<li class="page-item ' + activeClass + '"><a href="javascript:void(0);" class="page-link ' + activeBtnClass + '" onclick="window.changePage<%=rnd%>(' + i + ')">' + i + '</a></li>';
        } else if (Math.abs(i - window.currentPage<%=rnd%>) === 3) {
            pHtml += '<li class="page-item disabled"><a href="javascript:void(0);" class="page-link">...</a></li>';
        }
    }
    
    var nextDisabled = window.currentPage<%=rnd%> === totalPages ? 'disabled' : '';
    pHtml += '<li class="page-item ' + nextDisabled + '"><a href="javascript:void(0);" class="page-link rounded-end-pill" onclick="window.changePage<%=rnd%>(' + (window.currentPage<%=rnd%> + 1) + ')"><i class="fas fa-chevron-right"></i></a></li>';
    
    pagination.innerHTML = pHtml;
};

window.changePage<%=rnd%> = function(page) {
    var totalPages = Math.ceil(window.totalData<%=rnd%> / window.maxResult<%=rnd%>);
    if (page < 1 || page > totalPages) return;
    window.currentPage<%=rnd%> = page;
    window.loadDataCalonMahasiswa<%=rnd%>();
};

window.hapusCalonMahasiswa<%=rnd%> = function(id) {
    if (typeof proseshapusDataRinci === 'function') {
        proseshapusDataRinci(window.masterModelClass<%=rnd%>, id, function() { 
             if (document.querySelectorAll('#tabelDataCalonMhs<%=rnd%> tr').length === 1 && window.currentPage<%=rnd%> > 1) {
                 window.currentPage<%=rnd%>--;
             }
             window.loadDataCalonMahasiswa<%=rnd%>();
        });
    } else {
        console.warn('<%= Common.getBahasaConfigJS("Fungsi proseshapusDataRinci tidak ditemukan di global script.") %>');
    }
};

document.addEventListener("DOMContentLoaded", function() {
    var searchInput = document.getElementById('searchKeyword<%=rnd%>');
    if(searchInput) {
        searchInput.addEventListener("keydown", function(e) {
            if (e.key === "Enter") { 
                e.preventDefault(); 
                window.resetAndLoadData<%=rnd%>(); 
            }
        });
    }
    window.loadReferensiFilter<%=rnd%>();
    window.loadDataCalonMahasiswa<%=rnd%>();
});

// Default Toast
if (typeof tampilkanToast !== 'function') {
    window.tampilkanToast = function(msg) {
        alert(msg.replace(/<\/?[^>]+(>|$)/g, ""));
    };
}
</script>