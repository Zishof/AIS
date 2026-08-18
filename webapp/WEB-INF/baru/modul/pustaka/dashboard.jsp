<%@page import="ais.common.ConstantValues"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common" %>
<%@ page import="ais.database.model.Tbmuser" %>
<%@ page import="ais.database.model.library.Anggota" %>
<%
String rnd = Common.getGeneratedBarCode(7); 
Tbmuser tbmuser = Common.getCurrentUser(request);
Anggota anggota = tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null) ? Anggota.buatAtauAmbilAnggota(tbmuser, true) : (tbmuser != null ? Anggota.buatAtauAmbilAnggota(tbmuser, false) : null);
Long currentAnggotaId = (anggota != null) ? anggota.getId() : null;
%>

<script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/chart.js"></script>

<style>
    .dashboard-wrapper<%=rnd%> { background-color: #f8f9fa; padding: 2.5rem 1.5rem; border-radius: 1.25rem; min-height: 100vh; }
    .summary-card<%=rnd%> { border: none; border-radius: 1.25rem; color: #ffffff; transition: transform 0.3s ease, box-shadow 0.3s ease; position: relative; overflow: hidden; }
    .summary-card<%=rnd%>:hover { transform: translateY(-8px); box-shadow: 0 15px 30px rgba(0, 0, 0, 0.15); }
    .summary-card<%=rnd%>::after { content: '\f02d'; font-family: 'Font Awesome 5 Free'; font-weight: 900; position: absolute; right: -10px; bottom: -25px; font-size: 8rem; opacity: 0.15; transform: rotate(-15deg); }
    .summary-card<%=rnd%>.icon-users::after { content: '\f0c0'; } .summary-card<%=rnd%>.icon-out::after { content: '\f071'; } .summary-card<%=rnd%>.icon-in::after { content: '\f064'; }
    .bg-gradient-blue<%=rnd%> { background: linear-gradient(135deg, #007bff 0%, #00d2ff 100%); }
    .bg-gradient-purple<%=rnd%> { background: linear-gradient(135deg, #6f42c1 0%, #a88be8 100%); }
    .bg-gradient-orange<%=rnd%> { background: linear-gradient(135deg, #fd7e14 0%, #ffc107 100%); }
    .bg-gradient-green<%=rnd%> { background: linear-gradient(135deg, #198754 0%, #20c997 100%); }
    .content-container<%=rnd%> { background: #ffffff; border-radius: 1.25rem; padding: 1.75rem; box-shadow: 0 5px 20px rgba(0, 0, 0, 0.03); height: 100%; border: 1px solid rgba(0, 0, 0, 0.05); }
    .section-title<%=rnd%> { font-size: 1.15rem; font-weight: 700; color: #2c3e50; margin-bottom: 1.5rem; border-bottom: 2px solid #f1f3f5; padding-bottom: 0.75rem; }
    .number-display<%=rnd%> { font-size: 2.75rem; font-weight: 800; line-height: 1.1; margin: 12px 0; text-shadow: 1px 1px 2px rgba(0,0,0,0.1); }
    .nav-tabs-custom<%=rnd%> .nav-link { border: none; color: #6c757d; font-weight: 600; padding: 0.75rem 1.5rem; border-radius: 50rem; margin-right: 0.5rem; transition: all 0.3s ease; background-color: #f8f9fa; }
    .nav-tabs-custom<%=rnd%> .nav-link.active { background-color: #0d6efd; color: #ffffff; box-shadow: 0 4px 12px rgba(13, 110, 253, 0.3); }
    .nav-tabs-custom<%=rnd%> .nav-link:hover:not(.active) { background-color: #e9ecef; color: #0d6efd; }
    .table-custom<%=rnd%> thead th { background-color: #f8f9fa; color: #495057; font-weight: 700; border-bottom: 2px solid #dee2e6; text-transform: uppercase; font-size: 0.85rem; letter-spacing: 0.5px; }
    @media (max-width: 767.98px) {
        .number-display<%=rnd%> { font-size: 1.75rem; }
        .nav-tabs-custom<%=rnd%> { flex-wrap: nowrap; overflow-x: auto; -webkit-overflow-scrolling: touch; padding-bottom: 4px; }
        .nav-tabs-custom<%=rnd%> .nav-link { white-space: nowrap; padding: 0.5rem 0.85rem; font-size: 0.8rem; }
    }
</style>

<div class="dashboard-wrapper<%=rnd%>">
    <div class="container-fluid px-0">
        
        <div class="d-flex align-items-center mb-4 pb-3 border-bottom border-secondary border-opacity-10">
            <div class="bg-primary text-white rounded-circle d-flex align-items-center justify-content-center me-3 shadow-sm" style="width: 60px; height: 60px;"><i class="fas fa-chart-bar fa-2x"></i></div>
            <div>
                <h3 class="fw-bold text-dark mb-1"><%=Common.getBahasaConfig("Dasbor Perpustakaan")%></h3>
                <span class="text-secondary"><%=Common.getBahasaConfig("Ringkasan aktivitas sirkulasi dan data perpustakaan")%></span>
            </div>
        </div>

        <div class="row g-4 mb-4">
            <div class="col-xl-3 col-md-6">
                <div class="card summary-card<%=rnd%> bg-gradient-blue<%=rnd%> h-100 p-4">
                    <div class="d-flex justify-content-between align-items-start">
                        <div class="z-1">
                            <p class="mb-1 text-white-50 fw-bold text-uppercase" style="font-size: 0.85rem;"><%=Common.getBahasaConfig("Total Pustaka")%></p>
                            <h2 class="number-display<%=rnd%>" id="countKatalog<%=rnd%>"><i class="fas fa-circle-notch fa-spin fa-xs"></i></h2>
                            <span class="small fw-medium"><i class="fas fa-book me-1"></i><%=Common.getBahasaConfig("Judul Terdaftar")%></span>
                        </div>
                        <div class="bg-white bg-opacity-25 rounded-circle p-3 z-1"><i class="fas fa-swatchbook fa-2x"></i></div>
                    </div>
                </div>
            </div>

            <div class="col-xl-3 col-md-6">
                <div class="card summary-card<%=rnd%> icon-users bg-gradient-purple<%=rnd%> h-100 p-4">
                    <div class="d-flex justify-content-between align-items-start">
                        <div class="z-1">
                            <p class="mb-1 text-white-50 fw-bold text-uppercase" style="font-size: 0.85rem;"><%=Common.getBahasaConfig("Total Kunjungan")%></p>
                            <h2 class="number-display<%=rnd%>" id="countKunjungan<%=rnd%>"><i class="fas fa-circle-notch fa-spin fa-xs"></i></h2>
                            <span class="small fw-medium"><i class="fas fa-users me-1"></i><%=Common.getBahasaConfig("Aktivitas Kedatangan")%></span>
                        </div>
                        <div class="bg-white bg-opacity-25 rounded-circle p-3 z-1"><i class="fas fa-walking fa-2x"></i></div>
                    </div>
                </div>
            </div>

            <div class="col-xl-3 col-md-6">
                <div class="card summary-card<%=rnd%> icon-out bg-gradient-orange<%=rnd%> h-100 p-4">
                    <div class="d-flex justify-content-between align-items-start">
                        <div class="z-1">
                            <p class="mb-1 text-white-50 fw-bold text-uppercase" style="font-size: 0.85rem;"><%=Common.getBahasaConfig("Total Peminjaman")%></p>
                            <h2 class="number-display<%=rnd%>" id="countPinjam<%=rnd%>"><i class="fas fa-circle-notch fa-spin fa-xs"></i></h2>
                            <span class="small fw-medium"><i class="fas fa-sign-out-alt me-1"></i><%=Common.getBahasaConfig("Sirkulasi Keluar")%></span>
                        </div>
                        <div class="bg-white bg-opacity-25 rounded-circle p-3 z-1"><i class="fas fa-hand-holding fa-2x"></i></div>
                    </div>
                </div>
            </div>

            <div class="col-xl-3 col-md-6">
                <div class="card summary-card<%=rnd%> icon-in bg-gradient-green<%=rnd%> h-100 p-4">
                    <div class="d-flex justify-content-between align-items-start">
                        <div class="z-1">
                            <p class="mb-1 text-white-50 fw-bold text-uppercase" style="font-size: 0.85rem;"><%=Common.getBahasaConfig("Total Pengembalian")%></p>
                            <h2 class="number-display<%=rnd%>" id="countKembali<%=rnd%>"><i class="fas fa-circle-notch fa-spin fa-xs"></i></h2>
                            <span class="small fw-medium"><i class="fas fa-sign-in-alt me-1"></i><%=Common.getBahasaConfig("Sirkulasi Masuk")%></span>
                        </div>
                        <div class="bg-white bg-opacity-25 rounded-circle p-3 z-1"><i class="fas fa-box-open fa-2x"></i></div>
                    </div>
                </div>
            </div>
        </div>

        <div class="row g-4 mb-4">
            <div class="col-lg-8">
                <div class="content-container<%=rnd%>">
                    <h5 class="section-title<%=rnd%>"><i class="fas fa-chart-area text-primary me-2"></i><%=Common.getBahasaConfig("Tren Kunjungan (7 Hari Terakhir)")%></h5>
                    <div style="position: relative; height: 320px; width: 100%;"><canvas id="chartKunjungan<%=rnd%>"></canvas></div>
                </div>
            </div>

            <div class="col-lg-4">
                <div class="content-container<%=rnd%>">
                    <h5 class="section-title<%=rnd%>"><i class="fas fa-chart-pie text-warning me-2"></i><%=Common.getBahasaConfig("5 Kategori Pustaka Teratas")%></h5>
                    <div style="position: relative; height: 320px; width: 100%; display: flex; justify-content: center;"><canvas id="chartKategori<%=rnd%>"></canvas></div>
                </div>
            </div>
        </div>

        <div class="row">
            <div class="col-12">
                <div class="content-container<%=rnd%>">
                    
                    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center mb-4 pb-3 border-bottom">
                        <h5 class="section-title<%=rnd%> m-0 border-0 pb-0"><i class="fas fa-table text-success me-2"></i><%=Common.getBahasaConfig("Detail Rekapitulasi Data")%></h5>
                        
                        <form id="formCariRekap<%=rnd%>" onsubmit="event.preventDefault(); triggerSearchRekap<%=rnd%>();" class="d-flex mt-3 mt-md-0">
                            <div class="input-group shadow-sm">
                                <span class="input-group-text bg-white border-end-0 text-primary"><i class="fas fa-search"></i></span>
                                <input type="text" id="keywordRekap<%=rnd%>" class="form-control border-start-0" placeholder="<%=Common.getBahasaConfig("Cari (Judul / ISBN / Anggota)...")%>">
                                <button class="btn btn-primary fw-bold px-4" type="submit"><%=Common.getBahasaConfig("Cari")%></button>
                                <button class="btn btn-light border fw-bold text-secondary" type="button" onclick="resetSearchRekap<%=rnd%>()" title="<%=Common.getBahasaConfig("Atur Ulang")%>"><i class="fas fa-sync-alt"></i></button>
                            </div>
                        </form>
                    </div>
                    
                    <ul class="nav nav-tabs nav-tabs-custom<%=rnd%> mb-4" id="rekapTabs<%=rnd%>" role="tablist">
                        <li class="nav-item" role="presentation"><button class="nav-link active" id="tab-eksemplar-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#pane-eksemplar-<%=rnd%>" type="button" role="tab" onclick="loadTableEksemplar<%=rnd%>(1)"><i class="fas fa-barcode me-2"></i><%=Common.getBahasaConfig("Rekapitulasi Eksemplar Pustaka")%></button></li>
                        <li class="nav-item" role="presentation"><button class="nav-link" id="tab-kategori-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#pane-kategori-<%=rnd%>" type="button" role="tab" onclick="loadTableKategori<%=rnd%>(1)"><i class="fas fa-layer-group me-2"></i><%=Common.getBahasaConfig("Rekapitulasi Kategori")%></button></li>
                        <li class="nav-item" role="presentation"><button class="nav-link" id="tab-kunjungan-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#pane-kunjungan-<%=rnd%>" type="button" role="tab" onclick="loadTableKunjungan<%=rnd%>(1)"><i class="far fa-calendar-check me-2"></i><%=Common.getBahasaConfig("Rekapitulasi Kunjungan Harian")%></button></li>
                    </ul>

                    <div class="tab-content" id="rekapTabsContent<%=rnd%>">
                        <div class="tab-pane fade show active" id="pane-eksemplar-<%=rnd%>" role="tabpanel">
                            <div class="d-flex justify-content-end mb-3"><button class="btn btn-success rounded-pill fw-bold shadow-sm px-4" onclick="exportExcelEksemplar<%=rnd%>()"><i class="fas fa-file-excel me-2"></i><%=Common.getBahasaConfig("Unduh Excel")%></button></div>
                            <div class="table-responsive">
                                <table class="table table-hover table-custom<%=rnd%> align-middle border">
                                    <thead><tr><th width="5%" class="text-center"><%=Common.getBahasaConfig("No")%></th><th><%=Common.getBahasaConfig("Judul Pustaka")%></th><th><%=Common.getBahasaConfig("Lokasi Perpustakaan")%></th><th class="text-center" width="20%"><%=Common.getBahasaConfig("Total Eksemplar")%></th></tr></thead>
                                    <tbody id="tbody-eksemplar-<%=rnd%>"><tr><td colspan="4" class="text-center py-5"><div class="spinner-border text-primary" role="status"></div></td></tr></tbody>
                                </table>
                            </div>
                            <nav><ul class="pagination pagination-sm justify-content-end mt-3" id="pag-eksemplar-<%=rnd%>"></ul></nav>
                        </div>

                        <div class="tab-pane fade" id="pane-kategori-<%=rnd%>" role="tabpanel">
                            <div class="d-flex justify-content-end mb-3"><button class="btn btn-success rounded-pill fw-bold shadow-sm px-4" onclick="exportExcelKategori<%=rnd%>()"><i class="fas fa-file-excel me-2"></i><%=Common.getBahasaConfig("Unduh Excel")%></button></div>
                            <div class="table-responsive">
                                <table class="table table-hover table-custom<%=rnd%> align-middle border">
                                    <thead><tr><th width="5%" class="text-center"><%=Common.getBahasaConfig("No")%></th><th><%=Common.getBahasaConfig("Kategori Pustaka")%></th><th class="text-center" width="25%"><%=Common.getBahasaConfig("Jumlah Judul Terdaftar")%></th></tr></thead>
                                    <tbody id="tbody-kategori-<%=rnd%>"></tbody>
                                </table>
                            </div>
                            <nav><ul class="pagination pagination-sm justify-content-end mt-3" id="pag-kategori-<%=rnd%>"></ul></nav>
                        </div>

                        <div class="tab-pane fade" id="pane-kunjungan-<%=rnd%>" role="tabpanel">
                            <div class="d-flex justify-content-end mb-3"><button class="btn btn-success rounded-pill fw-bold shadow-sm px-4" onclick="exportExcelKunjungan<%=rnd%>()"><i class="fas fa-file-excel me-2"></i><%=Common.getBahasaConfig("Unduh Excel")%></button></div>
                            <div class="table-responsive">
                                <table class="table table-hover table-custom<%=rnd%> align-middle border">
                                    <thead><tr><th width="5%" class="text-center"><%=Common.getBahasaConfig("No")%></th><th><%=Common.getBahasaConfig("Tanggal Kunjungan")%></th><th class="text-center" width="25%"><%=Common.getBahasaConfig("Total Pengunjung")%></th></tr></thead>
                                    <tbody id="tbody-kunjungan-<%=rnd%>"></tbody>
                                </table>
                            </div>
                            <nav><ul class="pagination pagination-sm justify-content-end mt-3" id="pag-kunjungan-<%=rnd%>"></ul></nav>
                        </div>

                    </div>
                </div>
            </div>
        </div>

    </div>
</div>

<script>
    const apiUrl<%=rnd%> = '<%=Common.ROOT%>/Data';
    const currentAnggotaId<%=rnd%> = <%= currentAnggotaId == null ? "null" : currentAnggotaId %>;
    const limitPerPage<%=rnd%> = 10;
    
    const txtDataKosong<%=rnd%> = '<%=Common.getBahasaConfigJS("Data tidak tersedia")%>';
    const txtEksemplar<%=rnd%> = '<%=Common.getBahasaConfigJS("Eksemplar")%>';
    const txtJudul<%=rnd%> = '<%=Common.getBahasaConfigJS("Judul")%>';
    const txtKunjungan<%=rnd%> = '<%=Common.getBahasaConfigJS("Kunjungan")%>';

    const anggotaFilterKunjungan = currentAnggotaId<%=rnd%> ? " AND k.anggota = " + currentAnggotaId<%=rnd%> : "";
    const anggotaFilterPeminjaman = currentAnggotaId<%=rnd%> ? " AND anggota = " + currentAnggotaId<%=rnd%> : "";

    const fetchSql<%=rnd%> = async (sql) => {
        try {
            const res = await fetch(apiUrl<%=rnd%>, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ sql: sql, action: "sql", tanpaLogin:"true" }) });
            const result = await res.json(); return result.data || [];
        } catch (e) { console.error(e); return []; }
    };

    const formatNumber<%=rnd%> = (num) => num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".");

    const getCount<%=rnd%> = (res) => {
        if (res && res.length > 0) {
            let row = res[0]; let val = Array.isArray(row) ? row[0] : (row.jml !== undefined ? row.jml : Object.values(row)[0]); return { jml: val || 0 };
        } return { jml: 0 };
    };

    const buildPagination<%=rnd%> = (totalRecords, currentPage, targetElId, functionName) => {
        const totalPages = Math.ceil(totalRecords / limitPerPage<%=rnd%>);
        const pagEl = document.getElementById(targetElId); pagEl.innerHTML = "";
        if (totalPages <= 1) return;
        let html = "<li class='page-item " + (currentPage === 1 ? "disabled" : "") + "'><a class='page-link' href='#' onclick='event.preventDefault(); " + functionName + "(" + (currentPage - 1) + ")'>&laquo;</a></li>";
        let startPage = Math.max(1, currentPage - 2); let endPage = Math.min(totalPages, currentPage + 2);
        for (let i = startPage; i <= endPage; i++) {
            html += "<li class='page-item " + (i === currentPage ? "active" : "") + "'><a class='page-link' href='#' onclick='event.preventDefault(); " + functionName + "(" + i + ")'>" + i + "</a></li>";
        }
        html += "<li class='page-item " + (currentPage === totalPages ? "disabled" : "") + "'><a class='page-link' href='#' onclick='event.preventDefault(); " + functionName + "(" + (currentPage + 1) + ")'>&raquo;</a></li>";
        pagEl.innerHTML = html;
    };

    const downloadCSV<%=rnd%> = (dataArray, headers, filename) => {
        let csvContent = "data:text/csv;charset=utf-8," + headers.join(",") + "\n";
        dataArray.forEach(row => {
            let rowData = Array.isArray(row) ? row : Object.values(row);
            let rowStr = rowData.map(val => "\"" + (val || '').toString().replace(/"/g, '""') + "\"").join(",");
            csvContent += rowStr + "\n";
        });
        const encodedUri = encodeURI(csvContent);
        const link = document.createElement("a"); link.setAttribute("href", encodedUri); link.setAttribute("download", filename + ".csv");
        document.body.appendChild(link); link.click(); document.body.removeChild(link);
    };

    async function loadSummaryCards<%=rnd%>() {
        const [resKatalog, resKunjungan, resPinjam, resKembali] = await Promise.all([
            fetchSql<%=rnd%>("SELECT COUNT(id) as jml FROM library.item WHERE aktif = true"),
            fetchSql<%=rnd%>("SELECT COUNT(k.id) as jml FROM library.kunjungan_anggota k WHERE 1=1" + anggotaFilterKunjungan),
            fetchSql<%=rnd%>("SELECT COUNT(id) as jml FROM library.peminjaman_pengadaan_item WHERE tanggal_persetujuan IS NOT NULL" + anggotaFilterPeminjaman),
            fetchSql<%=rnd%>("SELECT COUNT(id) as jml FROM library.kembali_pengadaan_item WHERE tanggal_persetujuan IS NOT NULL")
        ]);
        document.getElementById("countKatalog<%=rnd%>").innerText = formatNumber<%=rnd%>(getCount<%=rnd%>(resKatalog)['jml']);
        document.getElementById("countKunjungan<%=rnd%>").innerText = formatNumber<%=rnd%>(getCount<%=rnd%>(resKunjungan)['jml']);
        document.getElementById("countPinjam<%=rnd%>").innerText = formatNumber<%=rnd%>(getCount<%=rnd%>(resPinjam)['jml']);
        document.getElementById("countKembali<%=rnd%>").innerText = formatNumber<%=rnd%>(getCount<%=rnd%>(resKembali)['jml']);
    }

    async function loadChartKunjungan<%=rnd%>() {
        const sql = "SELECT to_char(k.tanggal, 'YYYY-MM-DD') as tgl, COUNT(k.id) as jml FROM library.kunjungan_anggota k WHERE k.tanggal >= current_date - interval '7 days' " + anggotaFilterKunjungan + " GROUP BY to_char(k.tanggal, 'YYYY-MM-DD') ORDER BY tgl ASC";
        const resData = await fetchSql<%=rnd%>(sql);
        let labels = [], data = [];
        resData.forEach(row => { labels.push(Array.isArray(row) ? row[0] : row.tgl); data.push(Array.isArray(row) ? row[1] : row.jml); });
        new Chart(document.getElementById('chartKunjungan<%=rnd%>').getContext('2d'), {
            type: 'line', data: { labels: labels.length ? labels : [txtDataKosong<%=rnd%>], datasets: [{ label: txtKunjungan<%=rnd%>, data: data.length ? data : [0], borderColor: '#007bff', backgroundColor: 'rgba(0, 123, 255, 0.1)', borderWidth: 3, pointBackgroundColor: '#00d2ff', pointRadius: 5, fill: true, tension: 0.4 }] },
            options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } }
        });
    }

    async function loadChartKategori<%=rnd%>() {
        const sql = "SELECT kategories, COUNT(id) as jml FROM library.item WHERE kategories IS NOT NULL AND kategories != '' AND aktif = true GROUP BY kategories ORDER BY jml DESC LIMIT 5";
        const resData = await fetchSql<%=rnd%>(sql);
        let labels = [], data = [];
        resData.forEach(row => { labels.push(Array.isArray(row) ? row[0] : row.kategories); data.push(Array.isArray(row) ? row[1] : row.jml); });
        new Chart(document.getElementById('chartKategori<%=rnd%>').getContext('2d'), {
            type: 'doughnut', data: { labels: labels.length ? labels : [txtDataKosong<%=rnd%>], datasets: [{ data: data.length ? data : [1], backgroundColor: ['#007bff', '#6f42c1', '#fd7e14', '#198754', '#ffc107'], borderWidth: 0, hoverOffset: 5 }] },
            options: { responsive: true, maintainAspectRatio: false, cutout: '70%', plugins: { legend: { position: 'bottom', labels: { font: { size: 12 } } } } }
        });
    }

    function triggerSearchRekap<%=rnd%>() {
        if (document.getElementById("tab-eksemplar-<%=rnd%>").classList.contains("active")) loadTableEksemplar<%=rnd%>(1);
        else if (document.getElementById("tab-kategori-<%=rnd%>").classList.contains("active")) loadTableKategori<%=rnd%>(1);
        else if (document.getElementById("tab-kunjungan-<%=rnd%>").classList.contains("active")) loadTableKunjungan<%=rnd%>(1);
    }
    function resetSearchRekap<%=rnd%>() { document.getElementById("keywordRekap<%=rnd%>").value = ""; triggerSearchRekap<%=rnd%>(); }
    function getSearchKeyword<%=rnd%>() { const input = document.getElementById("keywordRekap<%=rnd%>"); return input ? input.value.trim().replace(/'/g, "''") : ""; }

    function buildSqlEksemplar<%=rnd%>() {
        let sql = "FROM library.item_punya_barcode b LEFT JOIN library.item i ON b.item = i.id LEFT JOIN library.perpustakaan p ON b.perpustakaan = p.id WHERE i.aktif = true ";
        const kw = getSearchKeyword<%=rnd%>();
        if (kw) sql += " AND (i.nama ILIKE '%" + kw + "%' OR i.isbn ILIKE '%" + kw + "%' OR i.isbn10 ILIKE '%" + kw + "%' OR p.nama ILIKE '%" + kw + "%') ";
        sql += " GROUP BY i.nama, p.nama "; return sql;
    }
    
    async function loadTableEksemplar<%=rnd%>(page) {
        document.getElementById("tbody-eksemplar-<%=rnd%>").innerHTML = "<tr><td colspan='4' class='text-center py-5'><div class='spinner-border text-primary' role='status'></div></td></tr>";
        const sqlBase = buildSqlEksemplar<%=rnd%>();
        const countData = await fetchSql<%=rnd%>("SELECT COUNT(*) as jml FROM (SELECT i.nama " + sqlBase + ") as temp");
        const total = getCount<%=rnd%>(countData)['jml'];
        const offset = (page - 1) * limitPerPage<%=rnd%>;
        const data = await fetchSql<%=rnd%>("SELECT i.nama, p.nama, count(b.id) " + sqlBase + " ORDER BY i.nama ASC LIMIT " + limitPerPage<%=rnd%> + " OFFSET " + offset);
        
        let html = ""; let no = offset + 1;
        data.forEach(row => {
            let arr = Array.isArray(row) ? row : [row.nama, row.p_nama || row.nama_1, row.count];
            html += "<tr><td class='text-center'>" + no++ + "</td><td class='fw-bold text-dark'>" + (arr[0]||'-') + "</td><td>" + (arr[1]||'-') + "</td><td class='text-center'><span class='badge bg-primary rounded-pill px-3 py-2 shadow-sm'>" + arr[2] + " " + txtEksemplar<%=rnd%> + "</span></td></tr>";
        });
        document.getElementById("tbody-eksemplar-<%=rnd%>").innerHTML = html || "<tr><td colspan='4' class='text-center text-muted py-4'><i class='fas fa-folder-open fa-2x mb-2 opacity-50'></i><br>" + txtDataKosong<%=rnd%> + "</td></tr>";
        buildPagination<%=rnd%>(total, page, "pag-eksemplar-<%=rnd%>", "loadTableEksemplar<%=rnd%>");
    }
    async function exportExcelEksemplar<%=rnd%>() { const data = await fetchSql<%=rnd%>("SELECT i.nama, p.nama, count(b.id) " + buildSqlEksemplar<%=rnd%>() + " ORDER BY i.nama ASC"); downloadCSV<%=rnd%>(data, ["<%=Common.getBahasaConfig("Judul Pustaka")%>", "<%=Common.getBahasaConfig("Lokasi Perpustakaan")%>", "<%=Common.getBahasaConfig("Total Eksemplar")%>"], "Rekapitulasi_Eksemplar_Pustaka"); }

    function buildSqlKategori<%=rnd%>() {
        let sql = "FROM library.item WHERE kategories IS NOT NULL AND kategories != '' AND aktif = true ";
        const kw = getSearchKeyword<%=rnd%>(); if (kw) sql += " AND kategories ILIKE '%" + kw + "%' ";
        sql += " GROUP BY kategories "; return sql;
    }
    async function loadTableKategori<%=rnd%>(page) {
        document.getElementById("tbody-kategori-<%=rnd%>").innerHTML = "<tr><td colspan='3' class='text-center py-5'><div class='spinner-border text-primary' role='status'></div></td></tr>";
        const sqlBase = buildSqlKategori<%=rnd%>();
        const countData = await fetchSql<%=rnd%>("SELECT COUNT(*) as jml FROM (SELECT kategories " + sqlBase + ") as temp");
        const total = getCount<%=rnd%>(countData)['jml'];
        const offset = (page - 1) * limitPerPage<%=rnd%>;
        const data = await fetchSql<%=rnd%>("SELECT kategories, COUNT(id) " + sqlBase + " ORDER BY COUNT(id) DESC LIMIT " + limitPerPage<%=rnd%> + " OFFSET " + offset);
        
        let html = ""; let no = offset + 1;
        data.forEach(row => {
            let arr = Array.isArray(row) ? row : Object.values(row);
            html += "<tr><td class='text-center'>" + no++ + "</td><td class='fw-bold text-dark'>" + (arr[0]||'-') + "</td><td class='text-center'><span class='badge bg-warning text-dark rounded-pill px-3 py-2 shadow-sm'>" + arr[1] + " " + txtJudul<%=rnd%> + "</span></td></tr>";
        });
        document.getElementById("tbody-kategori-<%=rnd%>").innerHTML = html || "<tr><td colspan='3' class='text-center text-muted py-4'><i class='fas fa-folder-open fa-2x mb-2 opacity-50'></i><br>" + txtDataKosong<%=rnd%> + "</td></tr>";
        buildPagination<%=rnd%>(total, page, "pag-kategori-<%=rnd%>", "loadTableKategori<%=rnd%>");
    }
    async function exportExcelKategori<%=rnd%>() { const data = await fetchSql<%=rnd%>("SELECT kategories, COUNT(id) " + buildSqlKategori<%=rnd%>() + " ORDER BY COUNT(id) DESC"); downloadCSV<%=rnd%>(data, ["<%=Common.getBahasaConfig("Kategori Pustaka")%>", "<%=Common.getBahasaConfig("Jumlah Judul Terdaftar")%>"], "Rekapitulasi_Kategori_Pustaka"); }

    function buildSqlKunjungan<%=rnd%>() {
        let sql = "FROM library.kunjungan_anggota k LEFT JOIN library.anggota a ON k.anggota = a.id WHERE 1=1 " + anggotaFilterKunjungan;
        const kw = getSearchKeyword<%=rnd%>(); if (kw) sql += " AND (a.nama ILIKE '%" + kw + "%' OR a.kode ILIKE '%" + kw + "%' OR k.keterangan ILIKE '%" + kw + "%') ";
        sql += " GROUP BY to_char(k.tanggal, 'YYYY-MM-DD') "; return sql;
    }
    async function loadTableKunjungan<%=rnd%>(page) {
        document.getElementById("tbody-kunjungan-<%=rnd%>").innerHTML = "<tr><td colspan='3' class='text-center py-5'><div class='spinner-border text-primary' role='status'></div></td></tr>";
        const sqlBase = buildSqlKunjungan<%=rnd%>();
        const countData = await fetchSql<%=rnd%>("SELECT COUNT(*) as jml FROM (SELECT to_char(k.tanggal, 'YYYY-MM-DD') " + sqlBase + ") as temp");
        const total = getCount<%=rnd%>(countData)['jml'];
        const offset = (page - 1) * limitPerPage<%=rnd%>;
        const data = await fetchSql<%=rnd%>("SELECT to_char(k.tanggal, 'YYYY-MM-DD') as tgl, count(k.id) " + sqlBase + " ORDER BY tgl DESC LIMIT " + limitPerPage<%=rnd%> + " OFFSET " + offset);
        
        let html = ""; let no = offset + 1;
        data.forEach(row => {
            let arr = Array.isArray(row) ? row : Object.values(row);
            html += "<tr><td class='text-center'>" + no++ + "</td><td class='fw-bold text-dark'><i class='far fa-calendar-alt text-secondary me-2'></i>" + (arr[0]||'-') + "</td><td class='text-center'><span class='badge bg-success rounded-pill px-3 py-2 shadow-sm'>" + arr[1] + " " + txtKunjungan<%=rnd%> + "</span></td></tr>";
        });
        document.getElementById("tbody-kunjungan-<%=rnd%>").innerHTML = html || "<tr><td colspan='3' class='text-center text-muted py-4'><i class='fas fa-folder-open fa-2x mb-2 opacity-50'></i><br>" + txtDataKosong<%=rnd%> + "</td></tr>";
        buildPagination<%=rnd%>(total, page, "pag-kunjungan-<%=rnd%>", "loadTableKunjungan<%=rnd%>");
    }
    async function exportExcelKunjungan<%=rnd%>() { const data = await fetchSql<%=rnd%>("SELECT to_char(k.tanggal, 'YYYY-MM-DD') as tgl, count(k.id) " + buildSqlKunjungan<%=rnd%>() + " ORDER BY tgl DESC"); downloadCSV<%=rnd%>(data, ["<%=Common.getBahasaConfig("Tanggal Kunjungan")%>", "<%=Common.getBahasaConfig("Total Pengunjung")%>"], "Rekapitulasi_Kunjungan_Harian"); }

    loadSummaryCards<%=rnd%>(); loadChartKunjungan<%=rnd%>(); loadChartKategori<%=rnd%>(); loadTableEksemplar<%=rnd%>(1);
</script>