<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.common.ProfileImageUtil"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common" %>
<%@ page import="ais.database.model.library.*" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.hibernate.Session" %>
<%@ page import="org.hibernate.criterion.Order" %>
<%@ page import="ais.database.hibernate.HibernateUtil" %>
<%
String rnd = Common.getGeneratedBarCode(7);
Tbmuser tbmuser = Common.getCurrentUser(request);
Anggota anggota = tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null) ? Anggota.buatAtauAmbilAnggota(tbmuser, true) : (tbmuser != null ? Anggota.buatAtauAmbilAnggota(tbmuser, false) : null);
Long currentAnggotaId = (anggota != null) ? anggota.getId() : null;
%>

<style>
    .main-body-bg<%=rnd%> { background-color: #f8f9fa; position: relative; }
    .main-body-bg<%=rnd%>::before { content: ""; position: fixed; top: 0; left: 0; right: 0; bottom: 0; background-image: url('<%=Common.ROOT%>/img/buku.jpeg'); background-size: cover; background-position: center; opacity: 0.25; z-index: -10; pointer-events: none; }
    .visit-card<%=rnd%> { transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1); background: #fff; border: 1px solid rgba(0,0,0,0.05); }
    .visit-card<%=rnd%>:hover { transform: translateY(-7px); box-shadow: 0 1.5rem 2.5rem rgba(0,0,0,0.1)!important; border-color: transparent; }
    .hero-section<%=rnd%> { background-image: url('<%=Common.ROOT%>/img/buku.jpeg'); background-size: cover; background-position: center; background-attachment: fixed; padding: 5rem 0 4rem 0; border-radius: 0 0 2.5rem 2.5rem; position: relative; overflow: hidden; }
    @media (max-width: 767.98px) { .hero-section<%=rnd%> { background-attachment: scroll; padding: 3rem 0 2rem 0; } }
    .hero-section<%=rnd%>::before { content: ''; position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0, 0, 0, 0.5); z-index: 1; }
    .hero-section<%=rnd%>::after { content: ''; position: absolute; bottom: 0; left: 0; right: 0; height: 50px; background: linear-gradient(to top, rgba(0,0,0,0.3), transparent); z-index: 1; }
    .hero-section<%=rnd%> .container { position: relative; z-index: 2; }
    .text-light-shadow<%=rnd%> { color: #ffffff !important; text-shadow: 2px 2px 6px rgba(0, 0, 0, 0.8), -1px -1px 4px rgba(0, 0, 0, 0.6); font-weight: 800; }
    .search-wrapper<%=rnd%> { position: relative; overflow: hidden; background-color: #fff; }
    .search-wrapper<%=rnd%>::before { content: ""; position: absolute; top: 0; left: 0; right: 0; bottom: 0; background-image: url('<%=Common.ROOT%>/img/buku.jpeg'); background-size: cover; background-position: center; opacity: 0.08; z-index: 1; }
    .search-content-inner<%=rnd%> { position: relative; z-index: 2; background: rgba(255, 255, 255, 0.85); border-radius: 1rem; box-shadow: 0 4px 15px rgba(0,0,0,0.05); }
    .text-truncate-custom { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: block; }
</style>

<div class="hero-section<%=rnd%> text-center shadow mb-5">
    <div class="container">
        <h1 class="display-5 mb-3 text-light-shadow<%=rnd%>"><i class="fas fa-users text-success me-3"></i><%=Common.getBahasaConfig("Riwayat Kunjungan")%></h1>
        <p class="lead mb-5 text-light-shadow<%=rnd%>" style="font-weight: 600;"><%=Common.getBahasaConfig("Pantau histori kedatangan dan kunjungan anggota di perpustakaan.")%></p>
    </div>
</div>

<div class="container mb-5">
    <div class="card shadow-sm border-0 rounded-4 mb-5 search-wrapper<%=rnd%>">
        <div class="card-body p-4 p-md-5 search-content-inner<%=rnd%>">
            <h5 class="card-title fw-bold mb-4 text-primary border-bottom pb-3"><i class="fas fa-search me-2"></i><%=Common.getBahasaConfig("Filter Riwayat Kunjungan")%></h5>
            <form id="formPencarian<%=rnd%>" onsubmit="event.preventDefault(); loadData<%=rnd%>(1);">
                <div class="row g-4">
                    <div class="col-md-6">
                        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Kata Kunci Pencarian")%></label>
                        <div class="input-group input-group-lg shadow-sm rounded-3">
                            <span class="input-group-text bg-light border-0 text-primary"><i class="fas fa-user"></i></span>
                            <input type="text" id="filterKeyword<%=rnd%>" class="form-control border-0 bg-light" placeholder="<%=Common.getBahasaConfig("Nama, Kode, atau Keterangan...")%>">
                        </div>
                    </div>
                    <div class="col-md-6">
                        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Lokasi Perpustakaan")%></label>
                        <div class="input-group input-group-lg shadow-sm rounded-3">
                            <span class="input-group-text bg-light border-0 text-danger"><i class="fas fa-map-marker-alt"></i></span>
                            <select id="filterPerpustakaan<%=rnd%>" class="form-select border-0 bg-light fw-medium">
                                <option value=""><%=Common.getBahasaConfig("-- Semua Perpustakaan --")%></option>
                            </select>
                        </div>
                    </div>
                    <div class="col-md-12 mt-4 d-flex align-items-end justify-content-end gap-3">
                        <button type="button" class="btn btn-light px-4 rounded-pill fw-bold text-secondary" style="background: #f1f2f3;" onclick="resetFilter<%=rnd%>()">
                            <i class="fas fa-sync-alt me-2"></i><%=Common.getBahasaConfig("Atur Ulang")%>
                        </button>
                        <button type="submit" class="btn btn-primary px-5 rounded-pill shadow-sm fw-bold">
                            <i class="fas fa-search me-2"></i><%=Common.getBahasaConfig("Mulai Pencarian")%>
                        </button>
                    </div>
                </div>
            </form>
        </div>
    </div>

    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-end mb-4 border-bottom pb-3">
        <h4 class="fw-bold text-dark m-0 mb-2 mb-md-0"><i class="fas fa-list-alt text-success me-2"></i><%=Common.getBahasaConfig("Daftar Kunjungan Terbaru")%></h4>
        <div id="infoHasil<%=rnd%>" class="text-muted small fw-medium bg-white px-3 py-1 rounded-pill border"><%=Common.getBahasaConfig("Menyiapkan data dari sistem...")%></div>
    </div>

    <div class="row row-cols-1 row-cols-md-2 row-cols-xl-3 g-4 main-body-bg<%=rnd%>" id="gridData<%=rnd%>"></div>

    <nav class="mt-5 pt-3" aria-label="<%=Common.getBahasaConfig("Navigasi Halaman Data")%>">
        <ul class="pagination pagination-lg justify-content-center shadow-sm rounded-pill bg-white d-inline-flex px-2 py-2" id="paginationData<%=rnd%>"></ul>
    </nav>
</div>

<script>
    const limitPerPage<%=rnd%> = 12;
    let currentPageGlobal<%=rnd%> = 1;
    const apiUrl<%=rnd%> = '<%=Common.ROOT%>/Data';
    const currentAnggotaId<%=rnd%> = <%= currentAnggotaId == null ? "null" : currentAnggotaId %>;

    const txtSedangMemuat<%=rnd%> = '<%=Common.getBahasaConfigJS("Sedang memuat data kunjungan...")%>';
    const txtGagalMemuat<%=rnd%> = '<%=Common.getBahasaConfigJS("Gagal menarik data dari server. Silakan muat ulang halaman.")%>';
    const txtTidakDitemukan<%=rnd%> = '<%=Common.getBahasaConfigJS("Maaf, tidak ada histori kunjungan yang sesuai pencarian.")%>';
    const txtSebelumnya<%=rnd%> = '<%=Common.getBahasaConfigJS("Sebelumnya")%>';
    const txtSelanjutnya<%=rnd%> = '<%=Common.getBahasaConfigJS("Selanjutnya")%>';
    
    const fetchData<%=rnd%> = async (sql) => {
        try {
            const res = await fetch(apiUrl<%=rnd%>, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ sql: sql, action: "sql", tanpaLogin:"true" }) });
            const result = await res.json(); return result.data || [];
        } catch (e) { console.error(e); return null; }
    };

    async function loadPerpustakaan<%=rnd%>() {
        try {
            const reqObj = { "action": "daftar", tanpaLogin:"true", "class": "<%=ais.database.model.library.Perpustakaan.class.getName()%>", "max": 100, "order1": "asc", "sort1": "nama" };
            const response = await fetch(apiUrl<%=rnd%>, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(reqObj) });
            const data = await response.json();
            const selectBox = document.getElementById("filterPerpustakaan<%=rnd%>");
            if (data && data.data) {
                data.data.forEach(p => {
                    let option = document.createElement("option"); option.value = p.id; option.text = p.nama; selectBox.appendChild(option);
                });
                if (data.data.length === 1) { selectBox.value = data.data[0].id; }
            }
        } catch (error) { console.error(error); }
    }

    function buildFilterSQL<%=rnd%>() {
        let condition = " WHERE 1=1 ";
        const cari = document.getElementById("filterKeyword<%=rnd%>").value.trim().replace(/'/g, "''");
        const perpustakaanId = document.getElementById("filterPerpustakaan<%=rnd%>").value;

        if (cari) condition += " AND (a.nama ILIKE '%" + cari + "%' OR a.kode ILIKE '%" + cari + "%' OR k.keterangan ILIKE '%" + cari + "%') ";
        if (perpustakaanId) condition += " AND k.perpustakaan = " + perpustakaanId + " ";
        if (currentAnggotaId<%=rnd%>) condition += " AND k.anggota = " + currentAnggotaId<%=rnd%> + " ";
        return condition;
    }

    async function loadData<%=rnd%>(currentPage) {
        currentPageGlobal<%=rnd%> = currentPage;
        const grid = document.getElementById("gridData<%=rnd%>");
        const infoText = document.getElementById("infoHasil<%=rnd%>");
        grid.innerHTML = "<div class='col-12 text-center py-5'><div class='spinner-border text-success' role='status' style='width: 3rem; height: 3rem;'></div><h5 class='mt-4 text-muted fw-bold'>" + txtSedangMemuat<%=rnd%> + "</h5></div>";
        document.getElementById("paginationData<%=rnd%>").innerHTML = ""; infoText.innerHTML = "";

        const baseCondition = buildFilterSQL<%=rnd%>();
        const sqlFrom = " FROM library.kunjungan_anggota k LEFT JOIN library.anggota a ON k.anggota = a.id LEFT JOIN library.perpustakaan p ON k.perpustakaan = p.id " + baseCondition;
        const sqlCount = "SELECT COUNT(k.id) AS total " + sqlFrom;
        const offset = (currentPage - 1) * limitPerPage<%=rnd%>;
        const sqlData = "SELECT k.id, to_char(k.tanggal, 'YYYY-MM-DD HH24:MI:SS') as tanggal, a.kode, a.nama, p.nama as perpustakaan_nama, k.keterangan, k.anggota as anggota_id " + sqlFrom + " ORDER BY k.tanggal DESC LIMIT " + limitPerPage<%=rnd%> + " OFFSET " + offset;

        const countResult = await fetchData<%=rnd%>(sqlCount);
        const dataResult = await fetchData<%=rnd%>(sqlData);

        if (countResult === null || dataResult === null) {
            grid.innerHTML = "<div class='col-12 text-center text-danger py-5'><div class='bg-danger bg-opacity-10 p-4 rounded-circle d-inline-block mb-3'><i class='fas fa-exclamation-triangle fa-3x text-danger'></i></div><h5 class='fw-bold'>" + txtGagalMemuat<%=rnd%> + "</h5></div>"; return;
        }

        let totalRecords = 0;
        if (countResult.length > 0) {
            totalRecords = parseInt(countResult[0].total || Object.values(countResult[0])[0] || 0);
        }

        let dataList = [];
        if (dataResult.length > 0) {
            dataList = dataResult.map(row => {
                return { 
                    id: row.id, 
                    tanggal: row.tanggal, 
                    kode: row.kode, 
                    nama: row.nama, 
                    perpustakaan: row.perpustakaan_nama || row.perpustakaan, 
                    keterangan: row.keterangan,
                    anggota_id: row.anggota_id
                };
            });
        }
        
        renderGrid<%=rnd%>(dataList); 
        updateInfoHasil<%=rnd%>(totalRecords, dataList.length); 
        renderPagination<%=rnd%>(totalRecords, currentPage);
    }

    function renderGrid<%=rnd%>(dataList) {
        const grid = document.getElementById("gridData<%=rnd%>"); grid.innerHTML = "";
        if (dataList.length === 0) { grid.innerHTML = "<div class='col-12 text-center py-5'><i class='far fa-id-card fa-5x text-secondary opacity-50 mb-4'></i><h4 class='text-secondary fw-bold'>" + txtTidakDitemukan<%=rnd%> + "</h4></div>"; return; }

        let htmlContent = "";
        dataList.forEach(item => {
            const nama = item.nama ? item.nama : (item.kode ? item.kode : "-");
            const kode = item.kode ? item.kode : "Tanpa ID";
            const perpus = item.perpustakaan ? item.perpustakaan : "-";
            const ket = (item.keterangan && item.keterangan.trim() !== "") ? item.keterangan : "Kunjungan Umum";
            
            let formattedDate = '-';
            if (item.tanggal) {
                const d = new Date(item.tanggal.replace(' ', 'T'));
                formattedDate = d.toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
            }

            // Memanggil endpoint action=getFoto yang sekarang sudah ada di Pustaka.java
            const photoUrl = '<%=Common.ROOT%>/pustaka?action=getFoto&anggotaId=' + (item.anggota_id || '');
            const fallbackAvatarUrl = 'https://ui-avatars.com/api/?name=' + encodeURIComponent(nama) + '&background=random';

            htmlContent += 
                "<div class='col'>" +
                    "<div class='card h-100 visit-card<%=rnd%> rounded-4 overflow-hidden p-4'>" +
                        "<div class='d-flex align-items-center mb-3'>" +
                            "<div class='me-3 flex-shrink-0'>" +
                                "<img src='" + photoUrl + "' class='rounded-circle object-fit-cover shadow-sm border border-2 border-light' style='width: 60px; height: 60px;' onerror=\"this.src='" + fallbackAvatarUrl + "'\">" +
                            "</div>" +
                            "<div style='width: calc(100% - 75px);'>" +
                                "<h6 class='fw-bold text-dark mb-1 text-truncate-custom' title='" + nama + "'>" + nama + "</h6>" +
                                "<span class='badge bg-light text-secondary border'>" + kode + "</span>" +
                            "</div>" +
                        "</div>" +
                        "<div class='mt-auto border-top pt-3'>" +
                            "<div class='small text-secondary mb-2 text-truncate-custom' title='" + formattedDate + "'><i class='far fa-clock text-warning me-2 w-15px'></i>" + formattedDate + "</div>" +
                            "<div class='small text-secondary mb-2 text-truncate-custom' title='" + perpus + "'><i class='fas fa-map-marker-alt text-danger me-2 w-15px'></i>" + perpus + "</div>" +
                            "<div class='small text-secondary text-truncate-custom' title='" + ket + "'><i class='fas fa-info-circle text-info me-2 w-15px'></i>" + ket + "</div>" +
                        "</div>" +
                    "</div>" +
                "</div>";
        });
        grid.innerHTML = htmlContent;
    }

    function renderPagination<%=rnd%>(totalRecords, currentPage) {
        const totalPages = Math.ceil(totalRecords / limitPerPage<%=rnd%>);
        const paginationEl = document.getElementById("paginationData<%=rnd%>"); paginationEl.innerHTML = "";
        if (totalPages <= 1) return;
        let html = "<li class='page-item " + (currentPage === 1 ? "disabled" : "") + "'><a class='page-link border-0 text-secondary fw-bold px-3 rounded-pill mx-1' href='#' onclick='event.preventDefault(); if(" + currentPage + " > 1) loadData<%=rnd%>(" + (currentPage - 1) + ")'><i class='fas fa-chevron-left me-2'></i>" + txtSebelumnya<%=rnd%> + "</a></li>";
        let startPage = Math.max(1, currentPage - 2); let endPage = Math.min(totalPages, currentPage + 2);
        for (let i = startPage; i <= endPage; i++) {
            let activeClass = (i === currentPage) ? "active shadow-sm" : ""; let btnStyle = (i === currentPage) ? "background-color: #198754; color: white;" : "color: #6c757d;";
            html += "<li class='page-item mx-1 " + activeClass + " rounded-circle'><a class='page-link border-0 rounded-circle fw-bold' style='width: 45px; height: 45px; display: flex; align-items: center; justify-content: center; " + btnStyle + "' href='#' onclick='event.preventDefault(); loadData<%=rnd%>(" + i + ")'>" + i + "</a></li>";
        }
        html += "<li class='page-item " + (currentPage === totalPages ? "disabled" : "") + "'><a class='page-link border-0 text-secondary fw-bold px-3 rounded-pill mx-1' href='#' onclick='event.preventDefault(); if(" + currentPage + " < " + totalPages + ") loadData<%=rnd%>(" + (currentPage + 1) + ")'>" + txtSelanjutnya<%=rnd%> + "<i class='fas fa-chevron-right ms-2'></i></a></li>";
        paginationEl.innerHTML = html;
    }

    function updateInfoHasil<%=rnd%>(totalRecords, currentCount) {
        const infoText = document.getElementById("infoHasil<%=rnd%>");
        if (totalRecords > 0) infoText.innerHTML = '<%=Common.getBahasaConfigJS("Menampilkan")%>' + " <span class='text-primary fw-bold mx-1'>" + currentCount + "</span> " + '<%=Common.getBahasaConfigJS("dari total")%>' + " <span class='text-primary fw-bold mx-1'>" + totalRecords + "</span> " + '<%=Common.getBahasaConfigJS("kunjungan")%>' + ".";
        else infoText.innerHTML = "";
    }

    function resetFilter<%=rnd%>() { 
        const form = document.getElementById("formPencarian<%=rnd%>");
        if(form) { form.reset(); loadPerpustakaan<%=rnd%>().then(() => loadData<%=rnd%>(1)); } 
        else { loadData<%=rnd%>(1); }
    }

    loadPerpustakaan<%=rnd%>().then(() => { loadData<%=rnd%>(1); });
</script>