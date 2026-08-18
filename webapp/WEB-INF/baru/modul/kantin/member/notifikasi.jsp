<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// ==========================================
// 1. Pengecekan Sesi & Keamanan Akses
// ==========================================
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
    return;
}

Pedagang pedagang = tbmuser.getPedagang();
Toko toko = pedagang == null ? null : pedagang.getToko();
boolean isAdmin = (toko == null);

// Halaman ini dikhususkan untuk Administrator Koperasi / Sistem
if (!isAdmin) {
    out.print("<div class='text-center p-5'><i class='fas fa-lock fa-3x text-danger mb-3'></i><br><h5 class='text-muted'>" + Common.getBahasaConfig("Akses Ditolak") + "</h5><p>" + Common.getBahasaConfig("Halaman ini khusus untuk Administrator.") + "</p></div>");
    return;
}

String rnd = Common.getGeneratedBarCode(7);
%>

<!-- Library untuk Export Excel -->
<script data-cfasync="false" src="https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js"></script>

<div class="container-fluid py-3 py-md-4 animate__animated animate__fadeIn" style="background-color: #f4f7f6; min-height: 100vh;">

    <!-- HEADER KONTROL -->
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-center mb-4 gap-3">
        <div class="text-center text-md-start">
            <h4 class="fw-bolder text-primary mb-1">
                <i class="fas fa-bell me-2"></i><%=Common.getBahasaConfig("Monitor Notifikasi Anggota")%>
            </h4>
            <p class="text-muted small mb-0"><%=Common.getBahasaConfig("Pantau log pesan, peringatan (SP), dan informasi sistem yang terkirim ke anggota.")%></p>
        </div>
        
        <div class="d-flex flex-wrap gap-2 justify-content-center">
            <button class="btn btn-danger rounded-pill px-4 shadow-sm fw-bold text-white" onclick="downloadPDFNotif<%=rnd%>()" title="<%=Common.getBahasaConfig("Cetak / Simpan PDF")%>">
                <i class="fas fa-file-pdf me-1"></i><%=Common.getBahasaConfig("Unduh PDF")%>
            </button>
            <button class="btn btn-success rounded-pill px-4 shadow-sm fw-bold" onclick="downloadExcelNotif<%=rnd%>()" title="<%=Common.getBahasaConfig("Ekspor Excel")%>">
                <i class="fas fa-file-excel me-1"></i><%=Common.getBahasaConfig("Unduh Excel")%>
            </button>
            <button class="btn btn-light border rounded-pill px-4 shadow-sm fw-bold text-primary" onclick="resetAndLoadData<%=rnd%>()">
                <i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Segarkan")%>
            </button>
        </div>
    </div>

    <!-- AREA PENYARINGAN (FILTER) -->
    <div class="card border-0 shadow-sm rounded-4 mb-4 border-top border-primary border-3">
        <div class="card-body p-3 p-md-4">
            <div class="row g-3 align-items-end">
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary mb-1"><i class="far fa-calendar-alt me-1 text-primary"></i><%=Common.getBahasaConfig("Mulai (Rentang 6 Bulan)")%></label>
                    <input type="date" class="form-control border-0 shadow-sm bg-light fw-medium filter-notif-<%=rnd%>" id="filterStart<%=rnd%>">
                </div>
                
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary mb-1"><i class="far fa-calendar-check me-1 text-primary"></i><%=Common.getBahasaConfig("Sampai")%></label>
                    <input type="date" class="form-control border-0 shadow-sm bg-light fw-medium filter-notif-<%=rnd%>" id="filterEnd<%=rnd%>">
                </div>

                <div class="col-md-4">
                    <label class="form-label small fw-bold text-secondary mb-1"><i class="fas fa-search me-1 text-primary"></i><%=Common.getBahasaConfig("Pencarian Spesifik")%></label>
                    <div class="input-group shadow-sm">
                        <span class="input-group-text bg-light border-end-0 text-muted"><i class="fas fa-keyboard"></i></span>
                        <input type="text" class="form-control border-start-0 bg-light fw-medium filter-notif-<%=rnd%>" id="searchKeyword<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari UserID, Nama, Telp, atau Isi Pesan...")%>">
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
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light text-secondary border-bottom">
                        <tr>
                            <th class="fw-bold text-uppercase small py-3 text-center" style="width: 50px;">#</th>
                            <th class="fw-bold text-uppercase small text-center" style="width: 160px;"><i class="far fa-clock me-1"></i><%=Common.getBahasaConfig("Waktu Kirim")%></th>
                            <th class="text-start fw-bold text-uppercase small" style="width: 250px;"><i class="fas fa-user me-1"></i><%=Common.getBahasaConfig("Penerima (Anggota)")%></th>
                            <th class="text-start fw-bold text-uppercase small"><i class="fas fa-envelope-open-text me-1"></i><%=Common.getBahasaConfig("Isi Notifikasi")%></th>
                            <th class="text-center fw-bold text-uppercase small" style="width: 120px;"><i class="fas fa-tag me-1"></i><%=Common.getBahasaConfig("Tipe")%></th>
                            <th class="text-center fw-bold text-uppercase small" style="width: 120px;"><i class="fas fa-eye me-1"></i><%=Common.getBahasaConfig("Status")%></th>
                            <th class="text-center fw-bold text-uppercase small" style="width: 80px;"><i class="fas fa-cogs"></i></th>
                        </tr>
                    </thead>
                    <tbody id="tabelDataNotifikasi<%=rnd%>">
                        <tr>
                            <td colspan="7" class="text-center py-5">
                                <div class="spinner-border text-primary mb-3"></div>
                                <br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Sistem sedang memuat data notifikasi...")%></span>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- PAGING -->
            <div class="bg-light p-3 border-top d-flex flex-column flex-md-row justify-content-between align-items-center gap-3">
                <div class="small fw-bold text-muted" id="pagingInfoNotif<%=rnd%>"></div>
                <div class="d-flex align-items-center bg-white rounded-pill shadow-sm border p-1">
                    <button class="btn btn-sm btn-light rounded-pill px-3 fw-bold text-primary" id="btnPrevPage<%=rnd%>" onclick="changePageNotif<%=rnd%>(-1)">
                        <i class="fas fa-chevron-left me-1"></i><span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Sebelumnya")%></span>
                    </button>
                    <div class="px-3 fw-bold text-dark small">
                        <span id="pageNumberDisplay<%=rnd%>">1 / 1</span>
                    </div>
                    <button class="btn btn-sm btn-light rounded-pill px-3 fw-bold text-primary" id="btnNextPage<%=rnd%>" onclick="changePageNotif<%=rnd%>(1)">
                        <span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Selanjutnya")%></span><i class="fas fa-chevron-right ms-1"></i>
                    </button>
                </div>
            </div>

        </div>
    </div>
</div>

<script>
    // ==========================================
    // VARIABEL GLOBAL & UTILITIES
    // ==========================================
    const masterModelClass<%=rnd%> = "ais.database.model.Notifikasi";
    let currentPage<%=rnd%> = 1;
    const limitPerPage<%=rnd%> = 10;
    let totalRecords<%=rnd%> = 0;

    const setFilterDefaultDates<%=rnd%> = () => {
        const today = new Date();
        today.setMonth(today.getMonth() + 1);
        const sixMonthsAgo = new Date();
        sixMonthsAgo.setMonth(today.getMonth() - 7);
        
        document.getElementById('filterEnd<%=rnd%>').value = today.toISOString().split('T')[0];
        document.getElementById('filterStart<%=rnd%>').value = sixMonthsAgo.toISOString().split('T')[0];
    };

    const fetchDataAPI<%=rnd%> = async (sql) => {
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

    // ==========================================
    // LOGIKA FILTER & PAGING
    // ==========================================
    const buildFilterSQL<%=rnd%> = () => {
        const tglMulai = document.getElementById('filterStart<%=rnd%>').value;
        const tglSampai = document.getElementById('filterEnd<%=rnd%>').value;
        const keyword = document.getElementById('searchKeyword<%=rnd%>').value.trim().replace(/'/g, "''");

        let filters = " WHERE 1=1 ";
        
        if (tglMulai !== "" && tglSampai !== "") {
            filters += " AND DATE(n.waktu) BETWEEN DATE('" + tglMulai + "') AND DATE('" + tglSampai + "') ";
        }
        
        if (keyword !== '') {
            filters += " AND (n.nama ILIKE '%" + keyword + "%' OR n.keterangan ILIKE '%" + keyword + "%' OR ak.nama ILIKE '%" + keyword + "%' OR ak.hp ILIKE '%" + keyword + "%' OR n.emails ILIKE '%" + keyword + "%') ";
        }
        return filters;
    };

    const resetAndLoadData<%=rnd%> = () => {
        currentPage<%=rnd%> = 1; 
        loadDataNotifikasi<%=rnd%>();
    };

    const loadDataNotifikasi<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataNotifikasi<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Mengambil data notifikasi dari server...")%></span></td></tr>';

        const sqlFilters = buildFilterSQL<%=rnd%>();
        const offset = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%>;

        const sqlCount = "SELECT COUNT(n.id) AS jumlah FROM public.notifikasi n LEFT JOIN koperasi.anggota_koperasi ak ON n.nama = ak.userid " + sqlFilters + ";";
        
        const sqlData = "SELECT n.id, TO_CHAR(n.waktu, 'DD Mon YYYY HH24:MI:SS') as waktu_str, n.waktu, n.nama as userid, n.emails, n.keterangan, n.hasil, n.buka, " +
                        "ak.nama as nama_member, ak.hp as telp_member " + 
                        "FROM public.notifikasi n " +
                        "LEFT JOIN koperasi.anggota_koperasi ak ON n.nama = ak.userid " +
                        sqlFilters + " ORDER BY n.waktu DESC, n.id DESC LIMIT " + limitPerPage<%=rnd%> + " OFFSET " + offset + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetchDataAPI<%=rnd%>(sqlCount),
                fetchDataAPI<%=rnd%>(sqlData)
            ]);

            totalRecords<%=rnd%> = (resCount && resCount[0]) ? parseInt(resCount[0].jumlah || 0) : 0;
            const recordList = resData || [];

            let htmlList = '';

            if (recordList.length === 0) {
                htmlList = '<tr><td colspan="7" class="text-center text-muted py-5"><i class="fas fa-bell-slash fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Tidak ada log notifikasi yang ditemukan pada rentang waktu ini.")%></td></tr>';
            } else {
                let no = offset + 1; 
                recordList.forEach((row) => {
                    // Badge Tipe/Hasil
                    let typeResult = row.hasil ? row.hasil.toUpperCase() : 'INFO';
                    let badgeType = '';
                    if (typeResult === 'WARNING') badgeType = '<span class="badge bg-warning text-dark px-3 py-1"><i class="fas fa-exclamation-triangle me-1"></i>Peringatan</span>';
                    else if (typeResult === 'DANGER') badgeType = '<span class="badge bg-danger px-3 py-1"><i class="fas fa-ban me-1"></i>Kritis</span>';
                    else if (typeResult === 'SUCCESS') badgeType = '<span class="badge bg-success px-3 py-1"><i class="fas fa-check-circle me-1"></i>Sukses</span>';
                    else badgeType = '<span class="badge bg-info text-dark px-3 py-1"><i class="fas fa-info-circle me-1"></i>Informasi</span>';

                    // Status Dibaca
                    const isBuka = (row.buka === true || row.buka === 'true' || row.buka === 't');
                    const badgeBuka = isBuka ? '<span class="text-success fw-bold small"><i class="fas fa-envelope-open me-1"></i>Dibaca</span>' : '<span class="text-danger fw-bold small"><i class="fas fa-envelope me-1"></i>Belum</span>';

                    // Info Member
                    const uid = row.userid || '-';
                    const nama = row.nama_member || '<%=Common.getBahasaConfigJS("Anonim")%>';
                    const telp = row.telp_member || row.emails || '-';

                    htmlList += '<tr class="border-bottom">' +
                            '<td class="text-center text-muted fw-medium">' + (no++) + '</td>' +
                            '<td class="text-center text-secondary small fw-bold">' + (row.waktu_str || '-') + '</td>' +
                            '<td class="text-start">' +
                                '<div class="fw-bold text-primary">' + nama + '</div>' +
                                '<small class="text-muted"><i class="fas fa-user-tag me-1"></i>' + uid + '</small><br>' +
                                '<small class="text-muted"><i class="fas fa-phone-alt me-1"></i>' + telp + '</small>' +
                            '</td>' +
                            '<td class="text-start text-dark fw-medium" style="max-width: 400px; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; text-overflow: ellipsis;" title="' + (row.keterangan || '').replace(/"/g, '&quot;') + '">' + 
                                (row.keterangan || '-') + 
                            '</td>' +
                            '<td class="text-center">' + badgeType + '</td>' +
                            '<td class="text-center">' + badgeBuka + '</td>' +
                            '<td class="text-center"><button class="btn btn-sm btn-outline-danger shadow-sm px-2 fw-bold" onclick="hapusNotifikasi<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Hapus Notifikasi")%>"><i class="fas fa-trash-alt"></i></button></td>' +
                        '</tr>';
                });
            }
            tbody.innerHTML = htmlList;
            updatePaginationUI<%=rnd%>();

        } catch (error) {
            console.error("Gagal load tabel notifikasi:", error);
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Terjadi kesalahan sistem saat menarik data. Silakan muat ulang.")%></td></tr>';
        }
    };

    const updatePaginationUI<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumberDisplay<%=rnd%>').innerText = currentPage<%=rnd%> + " / " + totalPages;
        
        const startIdx = totalRecords<%=rnd%> > 0 ? ((currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%> + 1) : 0;
        const endIdx = Math.min(currentPage<%=rnd%> * limitPerPage<%=rnd%>, totalRecords<%=rnd%>);
        
        document.getElementById('pagingInfoNotif<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startIdx + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari total")%> <b class="text-dark">' + totalRecords<%=rnd%> + '</b> <%=Common.getBahasaConfig("notifikasi")%>';
        
        document.getElementById('btnPrevPage<%=rnd%>').disabled = (currentPage<%=rnd%> === 1);
        document.getElementById('btnNextPage<%=rnd%>').disabled = (currentPage<%=rnd%> === totalPages);
    };

    const changePageNotif<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if (direction === -1 && currentPage<%=rnd%> > 1) {
            currentPage<%=rnd%>--;
            loadDataNotifikasi<%=rnd%>();
        } else if (direction === 1 && currentPage<%=rnd%> < totalPages) {
            currentPage<%=rnd%>++;
            loadDataNotifikasi<%=rnd%>();
        }
    };

    // ==========================================
    // LOGIKA HAPUS DATA
    // ==========================================
    const hapusNotifikasi<%=rnd%> = async (id) => {
        if (typeof prosesDeleteData === 'function') {
            prosesDeleteData(masterModelClass<%=rnd%>, id, function(){ 
                 if (document.querySelectorAll('#tabelDataNotifikasi<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) {
                     currentPage<%=rnd%>--;
                 }
                 loadDataNotifikasi<%=rnd%>();
            });
        } else {
            const isConfirm = confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus notifikasi ini?")%>');
            if (!isConfirm) return;

            const payload = {
                action: "deleteData",
                class: masterModelClass<%=rnd%>,
                id: id
            };

            try {
                const response = await fetch('<%=Common.ROOT%>/Data', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });

                const result = await response.json();
                if (result.status === '00' || result.status === 'success') {
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data notifikasi berhasil dihapus!")%>', 'bg-success text-white');
                    if (document.querySelectorAll('#tabelDataNotifikasi<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) {
                        currentPage<%=rnd%>--;
                    }
                    loadDataNotifikasi<%=rnd%>();
                } else {
                    showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Data gagal dihapus.")%>', 'bg-danger text-white');
                }
            } catch (error) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Koneksi jaringan terputus. Penghapusan dibatalkan.")%>', 'bg-danger text-white');
            }
        }
    };

    // ==========================================
    // EXPORT EXCEL & PDF
    // ==========================================
    const pullAllDataForExport<%=rnd%> = async () => {
        const sqlFilters = buildFilterSQL<%=rnd%>();
        const sqlData = "SELECT n.id, TO_CHAR(n.waktu, 'DD-MM-YYYY HH24:MI:SS') as waktu, n.nama as userid, n.emails as kontak, n.keterangan, n.hasil, n.buka, " +
                        "ak.nama as nama_member, ak.hp as telp_member " + 
                        "FROM public.notifikasi n " +
                        "LEFT JOIN koperasi.anggota_koperasi ak ON n.nama = ak.userid " +
                        sqlFilters + " ORDER BY n.waktu DESC;";
        return await fetchDataAPI<%=rnd%>(sqlData);
    };

    const downloadExcelNotif<%=rnd%> = async () => {
        showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Sedang menyiapkan file Excel, mohon tunggu...")%>', 'bg-info text-dark');
        
        try {
            const result = await pullAllDataForExport<%=rnd%>();
            if (!result || result.length === 0) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Tidak ada data untuk diunduh pada rentang ini.")%>', 'bg-warning text-dark');
                return;
            }

            let tableHtml = '<html xmlns:x="urn:schemas-microsoft-com:office:excel">';
            tableHtml += '<head><meta charset="UTF-8"><style>';
            tableHtml += 'th { background-color: #3b82f6; color: white; font-weight: bold; border: 1px solid black; }';
            tableHtml += 'td { border: 1px solid black; vertical-align: top; }';
            tableHtml += '</style></head><body>';
            
            tableHtml += '<h4>Laporan Riwayat Notifikasi Sistem</h4>';
            tableHtml += '<p>Filter Rentang: <b>' + document.getElementById('filterStart<%=rnd%>').value + ' s/d ' + document.getElementById('filterEnd<%=rnd%>').value + '</b></p>';

            tableHtml += '<table><thead><tr>';
            tableHtml += '<th>NO</th><th>WAKTU</th><th>USER_ID</th><th>NAMA_MEMBER</th><th>KONTAK</th><th>ISI_PESAN</th><th>TIPE_PESAN</th><th>STATUS_DIBACA</th>';
            tableHtml += '</tr></thead><tbody>';

            let no = 1;
            result.forEach(r => {
                tableHtml += '<tr>';
                tableHtml += '<td style="text-align: center;">' + (no++) + '</td>';
                tableHtml += '<td>' + (r.waktu || '') + '</td>';
                tableHtml += '<td>' + (r.userid || '') + '</td>';
                tableHtml += '<td>' + (r.nama_member || 'Anonim') + '</td>';
                tableHtml += '<td>' + (r.telp_member || r.kontak || '') + '</td>';
                tableHtml += '<td>' + (r.keterangan || '') + '</td>';
                tableHtml += '<td>' + (r.hasil || 'INFO') + '</td>';
                tableHtml += '<td style="text-align: center;">' + ((r.buka === 'true' || r.buka === true || r.buka === 't') ? 'SUDAH' : 'BELUM') + '</td>';
                tableHtml += '</tr>';
            });

            tableHtml += '</tbody></table></body></html>';

            const blob = new Blob([tableHtml], { type: 'application/vnd.ms-excel' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement("a");
            const timestamp = new Date().toISOString().slice(0,10).replace(/-/g,"");
            link.download = "Laporan_Notifikasi_" + timestamp + ".xls";
            link.href = url;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            setTimeout(() => URL.revokeObjectURL(url), 100);

        } catch (error) {
            console.error(error);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal mengekspor data Excel.")%>', 'bg-danger text-white');
        }
    };

    const downloadPDFNotif<%=rnd%> = async () => {
        showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Menyiapkan dokumen cetak/PDF...")%>', 'bg-info text-dark');
        
        try {
            const result = await pullAllDataForExport<%=rnd%>();
            if (!result || result.length === 0) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Tidak ada data untuk dicetak pada rentang ini.")%>', 'bg-warning text-dark');
                return;
            }

            let html = '<!DOCTYPE html><html><head><title><%=Common.getBahasaConfig("Laporan Notifikasi")%></title>';
            html += '<style>';
            html += 'body { font-family: Arial, sans-serif; font-size: 12px; margin: 20px; }';
            html += 'h2 { text-align: center; margin-bottom: 5px; }';
            html += 'p { text-align: center; color: #555; margin-bottom: 20px; }';
            html += 'table { width: 100%; border-collapse: collapse; margin-top: 10px; }';
            html += 'th, td { border: 1px solid #ddd; padding: 8px; text-align: left; vertical-align: top; }';
            html += 'th { background-color: #f2f2f2; font-weight: bold; text-align: center; }';
            html += '.center { text-align: center; }';
            html += '.badge { padding: 3px 6px; border-radius: 4px; font-size: 10px; font-weight: bold; color: white; }';
            html += '.bg-warning { background-color: #ffc107; color: #000; }';
            html += '.bg-danger { background-color: #dc3545; }';
            html += '.bg-info { background-color: #0dcaf0; color: #000; }';
            html += '.bg-success { background-color: #198754; }';
            html += '.bg-secondary { background-color: #6c757d; }';
            html += '</style></head><body>';
            
            html += '<h2>LAPORAN RIWAYAT NOTIFIKASI SISTEM</h2>';
            html += '<p>Periode: ' + document.getElementById('filterStart<%=rnd%>').value + ' s/d ' + document.getElementById('filterEnd<%=rnd%>').value + '</p>';
            
            html += '<table><thead><tr>';
            html += '<th style="width: 30px;">No</th>';
            html += '<th style="width: 120px;">Waktu</th>';
            html += '<th style="width: 150px;">Anggota (ID)</th>';
            html += '<th>Pesan / Keterangan</th>';
            html += '<th style="width: 80px;">Tipe</th>';
            html += '<th style="width: 80px;">Status</th>';
            html += '</tr></thead><tbody>';

            let no = 1;
            result.forEach(r => {
                const tipe = r.hasil ? r.hasil.toUpperCase() : 'INFO';
                let classTipe = 'bg-info';
                if(tipe === 'WARNING') classTipe = 'bg-warning';
                else if(tipe === 'DANGER') classTipe = 'bg-danger';
                else if(tipe === 'SUCCESS') classTipe = 'bg-success';

                const isDibaca = (r.buka === 'true' || r.buka === true || r.buka === 't');
                const statusHtml = isDibaca ? '<span class="badge bg-success">Dibaca</span>' : '<span class="badge bg-secondary">Belum</span>';

                html += '<tr>';
                html += '<td class="center">' + (no++) + '</td>';
                html += '<td class="center">' + (r.waktu || '') + '</td>';
                html += '<td><b>' + (r.nama_member || 'Anonim') + '</b><br><span style="font-size:10px; color:#666;">ID: ' + (r.userid || '') + '</span></td>';
                html += '<td>' + (r.keterangan || '') + '</td>';
                html += '<td class="center"><span class="badge ' + classTipe + '">' + tipe + '</span></td>';
                html += '<td class="center">' + statusHtml + '</td>';
                html += '</tr>';
            });

            html += '</tbody></table></body></html>';

            const printWindow = window.open('', '', 'height=800,width=1000');
            printWindow.document.write(html);
            printWindow.document.close();
            printWindow.focus();
            
            // Jeda agar browser memuat CSS sebelum jendela cetak muncul
            setTimeout(() => { printWindow.print(); }, 500);

        } catch (error) {
            console.error(error);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal menyiapkan dokumen cetak.")%>', 'bg-danger text-white');
        }
    };


    // ==========================================
    // INISIALISASI
    // ==========================================
    document.addEventListener("DOMContentLoaded", () => {
        setFilterDefaultDates<%=rnd%>();

        // Trigger Pencarian Saat Tekan Enter
        const filterInputs = document.querySelectorAll('.filter-notif-<%=rnd%>');
        filterInputs.forEach(input => {
            input.addEventListener("keydown", (e) => {
                if (e.key === "Enter") {
                    e.preventDefault();
                    resetAndLoadData<%=rnd%>();
                }
            });
        });
        
        loadDataNotifikasi<%=rnd%>();
    });
</script>