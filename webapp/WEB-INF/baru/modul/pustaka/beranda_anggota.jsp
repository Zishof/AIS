<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.library.Anggota"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    String rnd = Common.getGeneratedBarCode(7);
    Tbmuser tbmuser = Common.getCurrentUser(request);
    
    if (tbmuser == null) {
        out.print("<div class='alert alert-danger m-5 shadow-sm border-0 rounded-4 text-center'><i class='fas fa-lock fa-3x mb-3 d-block text-danger opacity-75'></i><h5 class='fw-bold'>" + Common.getBahasaConfig("Akses Ditolak") + "</h5><p class='mb-4'>" + Common.getBahasaConfig("Anda harus masuk (login) sebagai anggota perpustakaan terlebih dahulu.") + "</p><a href='" + Common.ROOT + "/pustaka?hanya_tampil_jsp=true&p=pustaka&s=login_pustaka' class='btn btn-primary rounded-pill px-4 py-2 fw-bold shadow-sm'><i class='fas fa-sign-in-alt me-2'></i>" + Common.getBahasaConfig("Ke Halaman Login") + "</a></div>");
        return;
    }

    Anggota currentAnggota = Anggota.buatAtauAmbilAnggota(tbmuser, true);
    if (currentAnggota == null) {
        out.print("<div class='alert alert-danger m-5 shadow-sm border-0 rounded-4 text-center'><i class='fas fa-user-times fa-3x mb-3 d-block text-danger opacity-75'></i><h5 class='fw-bold'>" + Common.getBahasaConfig("Kesalahan Identitas") + "</h5>" + Common.getBahasaConfig("Data keanggotaan Anda tidak terdaftar di sistem perpustakaan.") + "</div>");
        return;
    }
%>

<div class="container-fluid py-4 print-area">
    
    <div class="row mb-4 align-items-center">
        <div class="col-md-8">
            <h3 class="fw-bold text-dark mb-1"><i class="far fa-id-card text-theme-primary me-2"></i><%=Common.getBahasaConfig("Portal Keanggotaan")%></h3>
            <p class="text-secondary mb-0"><%=Common.getBahasaConfig("Selamat datang,")%> <strong class="text-dark"><%=currentAnggota.getNama()%></strong> (<%=Common.getBahasaConfig("ID")%>: <%=currentAnggota.getKode()%>)</p>
        </div>
        <div class="col-md-4 text-md-end mt-3 mt-md-0 no-print">
            <button class="btn btn-outline-secondary rounded-pill fw-bold shadow-sm transition-all" onclick="window.print()">
                <i class="fas fa-print me-2"></i><%=Common.getBahasaConfig("Cetak Laporan (PDF)")%>
            </button>
        </div>
    </div>

    <div class="row g-4 mb-5">
        <div class="col-sm-6 col-xl-3">
            <div class="card card-metric bg-gradient-theme-primary shadow">
                <div class="card-body p-4">
                    <h6 class="text-white-50 text-uppercase fw-bold mb-1"><%=Common.getBahasaConfig("Total Peminjaman")%></h6>
                    <h2 class="display-5 fw-bold mb-0" id="valPinjam<%=rnd%>">0</h2>
                    <i class="fas fa-book metric-icon"></i>
                </div>
            </div>
        </div>
        <div class="col-sm-6 col-xl-3">
            <div class="card card-metric bg-gradient-theme-success shadow">
                <div class="card-body p-4">
                    <h6 class="text-white-50 text-uppercase fw-bold mb-1"><%=Common.getBahasaConfig("Total Pengembalian")%></h6>
                    <h2 class="display-5 fw-bold mb-0" id="valKembali<%=rnd%>">0</h2>
                    <i class="fas fa-undo metric-icon"></i>
                </div>
            </div>
        </div>
        <div class="col-sm-6 col-xl-3">
            <div class="card card-metric bg-gradient-theme-warning shadow">
                <div class="card-body p-4">
                    <h6 class="text-white-50 text-uppercase fw-bold mb-1"><%=Common.getBahasaConfig("Total Pemesanan")%></h6>
                    <h2 class="display-5 fw-bold mb-0" id="valPesan<%=rnd%>">0</h2>
                    <i class="fas fa-bookmark metric-icon"></i>
                </div>
            </div>
        </div>
        <div class="col-sm-6 col-xl-3">
            <div class="card card-metric bg-gradient-theme-info shadow">
                <div class="card-body p-4">
                    <h6 class="text-white-50 text-uppercase fw-bold mb-1"><%=Common.getBahasaConfig("Total Kunjungan")%></h6>
                    <h2 class="display-5 fw-bold mb-0" id="valKunjung<%=rnd%>">0</h2>
                    <i class="fas fa-shoe-prints metric-icon"></i>
                </div>
            </div>
        </div>
    </div>

    <div class="row g-4">
        <div class="col-xl-4 mb-4">
            <div class="card border-0 shadow-sm rounded-4 h-100">
                <div class="card-header bg-white border-bottom-0 pt-4 pb-0">
                    <h6 class="fw-bold text-dark mb-0"><i class="fas fa-chart-pie text-theme-primary me-2"></i><%=Common.getBahasaConfig("Visualisasi Aktivitas")%></h6>
                </div>
                <div class="card-body d-flex align-items-center justify-content-center p-4">
                    <canvas id="chartAktivitas<%=rnd%>" style="max-height: 300px; width: 100%;"></canvas>
                </div>
            </div>
        </div>

        <div class="col-xl-8 mb-4">
            <div class="card border-0 shadow-sm rounded-4 h-100">
                <div class="card-header bg-white border-bottom pt-4 pb-3">
                    <ul class="nav nav-pills nav-pills-theme nav-fill gap-2" id="tabAktivitas<%=rnd%>" role="tablist">
                        <li class="nav-item" role="presentation">
                            <button class="nav-link active fw-bold rounded-pill shadow-sm" id="tab-pinjam-<%=rnd%>" data-bs-toggle="pill" data-bs-target="#pane-pinjam-<%=rnd%>" type="button" role="tab" onclick="ubahTabAktif<%=rnd%>('pinjam')">
                                <i class="fas fa-exchange-alt me-1"></i> <%=Common.getBahasaConfig("Sirkulasi")%>
                            </button>
                        </li>
                        <li class="nav-item" role="presentation">
                            <button class="nav-link fw-bold rounded-pill shadow-sm" id="tab-pesan-<%=rnd%>" data-bs-toggle="pill" data-bs-target="#pane-pesan-<%=rnd%>" type="button" role="tab" onclick="ubahTabAktif<%=rnd%>('pesan')">
                                <i class="fas fa-bookmark me-1"></i> <%=Common.getBahasaConfig("Pemesanan")%>
                            </button>
                        </li>
                        <li class="nav-item" role="presentation">
                            <button class="nav-link fw-bold rounded-pill shadow-sm" id="tab-kunjung-<%=rnd%>" data-bs-toggle="pill" data-bs-target="#pane-kunjung-<%=rnd%>" type="button" role="tab" onclick="ubahTabAktif<%=rnd%>('kunjung')">
                                <i class="far fa-building me-1"></i> <%=Common.getBahasaConfig("Kunjungan")%>
                            </button>
                        </li>
                    </ul>
                </div>
                
                <div class="card-body p-4">
                    <div class="bg-light p-3 rounded-3 mb-4 border no-print">
                        <div class="row g-2 align-items-end">
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Kata Kunci (Judul/ISBN)")%></label>
                                <div class="input-group input-group-sm">
                                    <span class="input-group-text bg-white"><i class="fas fa-search text-muted"></i></span>
                                    <input type="text" id="filterKeyword<%=rnd%>" class="form-control" placeholder="<%=Common.getBahasaConfig("Ketik pencarian...")%>">
                                </div>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Tanggal Mulai")%></label>
                                <input type="date" id="filterStartDate<%=rnd%>" class="form-control form-control-sm">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Tanggal Akhir")%></label>
                                <input type="date" id="filterEndDate<%=rnd%>" class="form-control form-control-sm">
                            </div>
                            <div class="col-md-2 d-flex gap-1">
                                <button class="btn btn-primary btn-sm w-100 fw-bold shadow-sm" onclick="terapkanFilter<%=rnd%>()"><%=Common.getBahasaConfig("Cari")%></button>
                                <button class="btn btn-outline-secondary btn-sm fw-bold shadow-sm" onclick="resetFilter<%=rnd%>()" title="<%=Common.getBahasaConfig("Atur Ulang")%>"><i class="fas fa-sync-alt"></i></button>
                            </div>
                        </div>
                    </div>

                    <div class="tab-content" id="tabContent<%=rnd%>">
                        <div class="tab-pane fade show active" id="pane-pinjam-<%=rnd%>" role="tabpanel">
                            <div class="d-flex justify-content-between align-items-center mb-3 no-print">
                                <h6 class="fw-bold text-secondary m-0"><%=Common.getBahasaConfig("Riwayat Peminjaman & Pengembalian")%></h6>
                                <button class="btn btn-sm btn-success rounded-pill px-3 shadow-sm transition-all" onclick="unduhExcel<%=rnd%>('tabelPinjam<%=rnd%>', 'Riwayat_Sirkulasi')"><i class="far fa-file-excel me-2"></i><%=Common.getBahasaConfig("Unduh Excel")%></button>
                            </div>
                            <div class="table-responsive mb-3">
                                <table class="table table-hover table-custom align-middle" id="tabelPinjam<%=rnd%>">
                                    <thead><tr>
                                        <th><%=Common.getBahasaConfig("Kode / Keterangan")%></th>
                                        <th><%=Common.getBahasaConfig("Waktu Peminjaman")%></th>
                                        <th><%=Common.getBahasaConfig("Status Pengembalian")%></th>
                                        <th><%=Common.getBahasaConfig("Waktu Dikembalikan")%></th>
                                    </tr></thead>
                                    <tbody id="tbodyPinjam<%=rnd%>"></tbody>
                                </table>
                            </div>
                            <ul class="pagination pagination-sm justify-content-center shadow-sm rounded-pill bg-white d-inline-flex px-2 py-1 mb-0" id="pgPinjam<%=rnd%>"></ul>
                        </div>

                        <div class="tab-pane fade" id="pane-pesan-<%=rnd%>" role="tabpanel">
                            <div class="d-flex justify-content-between align-items-center mb-3 no-print">
                                <h6 class="fw-bold text-secondary m-0"><%=Common.getBahasaConfig("Daftar Pemesanan Reservasi")%></h6>
                                <button class="btn btn-sm btn-success rounded-pill px-3 shadow-sm transition-all" onclick="unduhExcel<%=rnd%>('tabelPesan<%=rnd%>', 'Riwayat_Pemesanan')"><i class="far fa-file-excel me-2"></i><%=Common.getBahasaConfig("Unduh Excel")%></button>
                            </div>
                            <div class="table-responsive mb-3">
                                <table class="table table-hover table-custom align-middle" id="tabelPesan<%=rnd%>">
                                    <thead><tr>
                                        <th><%=Common.getBahasaConfig("Kode Reservasi")%></th>
                                        <th><%=Common.getBahasaConfig("Referensi Pustaka")%></th>
                                        <th><%=Common.getBahasaConfig("Waktu Pemesanan")%></th>
                                        <th><%=Common.getBahasaConfig("Status Saat Ini")%></th>
                                    </tr></thead>
                                    <tbody id="tbodyPesan<%=rnd%>"></tbody>
                                </table>
                            </div>
                            <ul class="pagination pagination-sm justify-content-center shadow-sm rounded-pill bg-white d-inline-flex px-2 py-1 mb-0" id="pgPesan<%=rnd%>"></ul>
                        </div>

                        <div class="tab-pane fade" id="pane-kunjung-<%=rnd%>" role="tabpanel">
                            <div class="d-flex justify-content-between align-items-center mb-3 no-print">
                                <h6 class="fw-bold text-secondary m-0"><%=Common.getBahasaConfig("Riwayat Kunjungan Fisik")%></h6>
                                <button class="btn btn-sm btn-success rounded-pill px-3 shadow-sm transition-all" onclick="unduhExcel<%=rnd%>('tabelKunjung<%=rnd%>', 'Riwayat_Kunjungan')"><i class="far fa-file-excel me-2"></i><%=Common.getBahasaConfig("Unduh Excel")%></button>
                            </div>
                            <div class="table-responsive mb-3">
                                <table class="table table-hover table-custom align-middle" id="tabelKunjung<%=rnd%>">
                                    <thead><tr>
                                        <th><%=Common.getBahasaConfig("Waktu Kunjungan")%></th>
                                        <th><%=Common.getBahasaConfig("Lokasi Perpustakaan")%></th>
                                        <th><%=Common.getBahasaConfig("Tujuan / Keterangan")%></th>
                                    </tr></thead>
                                    <tbody id="tbodyKunjung<%=rnd%>"></tbody>
                                </table>
                            </div>
                            <ul class="pagination pagination-sm justify-content-center shadow-sm rounded-pill bg-white d-inline-flex px-2 py-1 mb-0" id="pgKunjung<%=rnd%>"></ul>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    const apiSvc<%=rnd%> = '<%=Common.ROOT%>/pustaka?hanya_tampil_jsp=true&p=pustaka&s=_beranda_anggota_service';
    const limitPerPage<%=rnd%> = 10;
    let chartInstance<%=rnd%> = null;
    let currentTab<%=rnd%> = 'pinjam'; // Lacak tab aktif

    const themePrimaryColor = getComputedStyle(document.documentElement).getPropertyValue('--theme-primary').trim() || '#0d6efd';

    // Inisialisasi Tanggal (Hari ini s/d 5 Tahun Lalu)
    function initDateFilter<%=rnd%>() {
        var d = new Date();
        var y = d.getFullYear();
        var m = ('0' + (d.getMonth() + 1)).slice(-2);
        var day = ('0' + d.getDate()).slice(-2);
        var endStr = y + '-' + m + '-' + day;
        
        var startStr = (y - 5) + '-' + m + '-' + day;
        
        document.getElementById('filterStartDate<%=rnd%>').value = startStr;
        document.getElementById('filterEndDate<%=rnd%>').value = endStr;
    }

    function renderGrafik<%=rnd%>(pinjam, kembali, pesan, kunjung) {
        const ctx = document.getElementById('chartAktivitas<%=rnd%>').getContext('2d');
        if (chartInstance<%=rnd%>) chartInstance<%=rnd%>.destroy();
        chartInstance<%=rnd%> = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: ['<%=Common.getBahasaConfigJS("Peminjaman")%>', '<%=Common.getBahasaConfigJS("Pengembalian")%>', '<%=Common.getBahasaConfigJS("Pemesanan")%>', '<%=Common.getBahasaConfigJS("Kunjungan")%>'],
                datasets: [{
                    data: [pinjam, kembali, pesan, kunjung],
                    backgroundColor: [themePrimaryColor, '#198754', '#ffc107', '#0dcaf0'],
                    borderWidth: 0, hoverOffset: 5
                }]
            },
            options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom', labels: { usePointStyle: true, padding: 20 } } } }
        });
    }

    async function loadSummary<%=rnd%>() {
        try {
            const res = await fetch(apiSvc<%=rnd%> + "&action=summary");
            const result = await res.json();
            if (result.status === 'success') {
                document.getElementById('valPinjam<%=rnd%>').innerText = result.data.pinjam;
                document.getElementById('valKembali<%=rnd%>').innerText = result.data.kembali;
                document.getElementById('valPesan<%=rnd%>').innerText = result.data.pesan;
                document.getElementById('valKunjung<%=rnd%>').innerText = result.data.kunjung;
                renderGrafik<%=rnd%>(result.data.pinjam, result.data.kembali, result.data.pesan, result.data.kunjung);
            }
        } catch (err) {}
    }

    // Fungsi Pengambilan Data Historis (Dengan Parameter Filter)
    async function loadHistoriData<%=rnd%>(tipe, page) {
        const tbodyName = tipe.charAt(0).toUpperCase() + tipe.slice(1);
        const tbody = document.getElementById('tbody' + tbodyName + '<%=rnd%>');
        const colSpan = tipe === 'kunjung' ? 3 : 4;
        
        tbody.innerHTML = '<tr><td colspan="' + colSpan + '" class="text-center text-theme-primary py-4"><i class="fas fa-spinner fa-spin me-2"></i><%=Common.getBahasaConfig("Sedang memuat data...")%></td></tr>';
        
        // Ambil Nilai dari Input Filter
        const keyword = encodeURIComponent(document.getElementById('filterKeyword<%=rnd%>').value);
        const startDate = document.getElementById('filterStartDate<%=rnd%>').value;
        const endDate = document.getElementById('filterEndDate<%=rnd%>').value;
        
        const urlReq = apiSvc<%=rnd%> + "&action=history_" + tipe + "&page=" + page + "&limit=" + limitPerPage<%=rnd%> + "&keyword=" + keyword + "&start_date=" + startDate + "&end_date=" + endDate;

        try {
            const res = await fetch(urlReq);
            const result = await res.json();
            
            if (result.status === 'success') {
                let html = "";
                if(result.data.length === 0) {
                    html = '<tr><td colspan="' + colSpan + '" class="text-center text-muted py-4"><%=Common.getBahasaConfig("Tidak ada catatan aktivitas yang sesuai dengan filter.")%></td></tr>';
                } else {
                    result.data.forEach(p => {
                        if (tipe === 'pinjam') {
                            let badge = p.status.includes('Di') && !p.status.includes('Sedang') ? 'bg-success' : 'bg-primary';
                            html += '<tr><td><span class="fw-bold text-dark d-block">' + p.kode + '</span><small class="text-muted">' + p.keterangan + '</small></td><td>' + p.tanggal_pinjam + '</td><td><span class="badge ' + badge + '">' + p.status + '</span></td><td>' + p.tanggal_kembali + '</td></tr>';
                        } 
                        else if (tipe === 'pesan') {
                            html += '<tr><td><span class="fw-bold text-dark">' + p.kode + '</span></td><td><i class="fas fa-book text-muted me-2"></i>' + p.buku + '</td><td>' + p.tanggal + '</td><td><span class="badge bg-secondary">' + p.status + '</span></td></tr>';
                        } 
                        else if (tipe === 'kunjung') {
                            html += '<tr><td><i class="far fa-calendar-alt text-muted me-2"></i>' + p.tanggal + '</td><td>' + p.perpustakaan + '</td><td>' + p.keterangan + '</td></tr>';
                        }
                    });
                }
                tbody.innerHTML = html;
                renderPaginationUI<%=rnd%>(result.total_data, page, tipe);
            }
        } catch (err) {
            tbody.innerHTML = '<tr><td colspan="' + colSpan + '" class="text-center text-danger py-4"><i class="fas fa-exclamation-triangle me-2"></i><%=Common.getBahasaConfig("Gagal memuat koneksi data.")%></td></tr>';
        }
    }

    function ubahTabAktif<%=rnd%>(tipe) {
        currentTab<%=rnd%> = tipe;
        loadHistoriData<%=rnd%>(tipe, 1);
    }

    function terapkanFilter<%=rnd%>() {
        // Terapkan filter hanya pada tab yang sedang aktif
        loadHistoriData<%=rnd%>(currentTab<%=rnd%>, 1);
    }

    function resetFilter<%=rnd%>() {
        document.getElementById('filterKeyword<%=rnd%>').value = '';
        initDateFilter<%=rnd%>();
        loadHistoriData<%=rnd%>(currentTab<%=rnd%>, 1);
    }

    function renderPaginationUI<%=rnd%>(totalRecords, currentPage, tipe) {
        const totalPages = Math.ceil(totalRecords / limitPerPage<%=rnd%>);
        const pgName = tipe.charAt(0).toUpperCase() + tipe.slice(1);
        const paginationEl = document.getElementById("pg" + pgName + "<%=rnd%>");
        paginationEl.innerHTML = "";
        if (totalPages <= 1) return;
        
        let html = '<li class="page-item ' + (currentPage === 1 ? 'disabled' : '') + '"><a class="page-link border-0 text-secondary fw-bold px-2 rounded-circle" href="#" onclick="event.preventDefault(); loadHistoriData<%=rnd%>(\'' + tipe + '\', ' + (currentPage - 1) + ')"><i class="fas fa-chevron-left"></i></a></li>';
        let startPage = Math.max(1, currentPage - 2); 
        let endPage = Math.min(totalPages, currentPage + 2);
        
        for (let i = startPage; i <= endPage; i++) {
            let activeClass = (i === currentPage) ? "active shadow-sm bg-primary text-white" : "text-secondary"; 
            let styleColor = (i === currentPage) ? 'background-color: var(--theme-primary) !important; border-color: var(--theme-primary) !important;' : '';
            html += '<li class="page-item mx-1 rounded-circle"><a class="page-link border-0 rounded-circle fw-bold text-center ' + activeClass + '" style="width: 32px; height: 32px; padding: 0.3rem; ' + styleColor + '" href="#" onclick="event.preventDefault(); loadHistoriData<%=rnd%>(\'' + tipe + '\', ' + i + ')">' + i + '</a></li>';
        }
        
        html += '<li class="page-item ' + (currentPage === totalPages ? 'disabled' : '') + '"><a class="page-link border-0 text-secondary fw-bold px-2 rounded-circle" href="#" onclick="event.preventDefault(); loadHistoriData<%=rnd%>(\'' + tipe + '\', ' + (currentPage + 1) + ')"><i class="fas fa-chevron-right"></i></a></li>';
        paginationEl.innerHTML = html;
    }

    function unduhExcel<%=rnd%>(tableID, filename) {
        let table = document.getElementById(tableID);
        let rows = table.querySelectorAll('tr');
        let csvContent = [];
        for (let i = 0; i < rows.length; i++) {
            let row = [], cols = rows[i].querySelectorAll('td, th');
            for (let j = 0; j < cols.length; j++) {
                let data = cols[j].innerText.replace(/(\r\n|\n|\r)/gm, '').replace(/(\s\s)/gm, ' ');
                data = data.replace(/"/g, '""');
                row.push('"' + data + '"');
            }
            csvContent.push(row.join(','));
        }
        let csvString = csvContent.join('\n');
        let blob = new Blob([csvString], { type: 'text/csv;charset=utf-8;' });
        let url = URL.createObjectURL(blob);
        let link = document.createElement("a");
        link.setAttribute("href", url);
        link.setAttribute("download", filename + "_" + new Date().getTime() + ".csv");
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }

 // =========================================================================
    // INISIALISASI AWAL (LAZY LOAD SAFE)
    // Dieksekusi langsung saat skrip ini dimuat/disuntikkan ke halaman
    // =========================================================================
    initDateFilter<%=rnd%>();
    loadSummary<%=rnd%>();
    loadHistoriData<%=rnd%>('pinjam', 1); // Load default tab
</script>