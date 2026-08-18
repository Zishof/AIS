<%@page import="ais.database.model.koperasi.AnggotaKoperasi"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// 1. Pengecekan Sesi Pengguna
Tbmuser tbmuser = Common.getCurrentUser(request);
AnggotaKoperasi curentAnggota = tbmuser == null ? null : tbmuser.getAnggotaKoperasi();

if(curentAnggota == null) {
    out.print("<div class='text-center p-5'><i class='fas fa-exclamation-triangle fa-3x text-warning mb-3'></i><br><h5 class='text-muted'>" + Common.getBahasaConfig("Akses Ditolak") + "</h5><p>" + Common.getBahasaConfig("Halaman ini hanya dapat diakses oleh Anggota Koperasi yang sah.") + "</p></div>");
    return;
}

String rnd = Common.getGeneratedBarCode(7);
Long idMemberAktif = curentAnggota.getId();
%>

<div class="container-fluid py-2 animate__animated animate__fadeIn" id="riwayatBarangContent<%=rnd%>">

    <div class="mb-3">
        <h5 class="fw-bolder text-primary mb-1">
            <i class="fas fa-box-open me-2"></i><%=Common.getBahasaConfig("Rincian Barang yang Dibeli")%>
        </h5>
        <p class="text-muted small mb-0"><%=Common.getBahasaConfig("Daftar lengkap produk atau layanan yang pernah Anda beli melalui koperasi.")%></p>
    </div>

    <div class="card border-0 shadow-sm rounded-4 mb-4 border-top border-info border-3">
        <div class="card-body p-3">
            <div class="row g-3 align-items-end">
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Tanggal Awal")%></label>
                    <div class="input-group input-group-sm shadow-sm">
                        <span class="input-group-text bg-light border-end-0 text-info"><i class="fas fa-calendar-alt"></i></span>
                        <input type="date" class="form-control border-start-0 fw-bold text-dark filter-brg-<%=rnd%>" id="startDateBrg<%=rnd%>">
                    </div>
                </div>
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Tanggal Akhir")%></label>
                    <div class="input-group input-group-sm shadow-sm">
                        <span class="input-group-text bg-light border-end-0 text-info"><i class="fas fa-calendar-check"></i></span>
                        <input type="date" class="form-control border-start-0 fw-bold text-dark filter-brg-<%=rnd%>" id="endDateBrg<%=rnd%>">
                    </div>
                </div>
                <div class="col-md-4">
                    <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Pencarian Produk / Toko")%></label>
                    <div class="input-group input-group-sm shadow-sm">
                        <span class="input-group-text bg-light border-end-0 text-muted"><i class="fas fa-search"></i></span>
                        <input type="text" class="form-control border-start-0 filter-brg-<%=rnd%>" id="searchKeywordBrg<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik nama/kode barang atau toko...")%>">
                    </div>
                </div>
                <div class="col-md-2 d-grid">
                    <button class="btn btn-sm btn-info text-white rounded-3 shadow-sm fw-bold" onclick="resetAndLoadBarang<%=rnd%>()">
                        <i class="fas fa-filter me-1"></i><%=Common.getBahasaConfig("Saring")%>
                    </button>
                </div>
            </div>
        </div>
    </div>

    <div class="card border-0 shadow-sm rounded-4 overflow-hidden">
        <div class="card-body p-0">
            <div class="table-responsive px-3 py-2" style="min-height: 400px;">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light text-secondary">
                        <tr>
                            <th class="fw-bold text-uppercase small py-3 text-center" style="width: 50px;">#</th>
                            <th class="fw-bold text-uppercase small"><%=Common.getBahasaConfig("Waktu Transaksi")%></th>
                            <th class="fw-bold text-uppercase small"><%=Common.getBahasaConfig("Nama Barang")%></th>
                            <th class="fw-bold text-uppercase small"><%=Common.getBahasaConfig("Toko Penyedia")%></th>
                            <th class="fw-bold text-uppercase small text-center"><%=Common.getBahasaConfig("Kuantitas")%></th>
                            <th class="text-end fw-bold text-uppercase small"><%=Common.getBahasaConfig("Total Pembayaran")%></th>
                        </tr>
                    </thead>
                    <tbody id="tabelBarang<%=rnd%>">
                        <tr>
                            <td colspan="6" class="text-center py-5">
                                <div class="spinner-border text-info mb-3"></div>
                                <br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Menyiapkan data riwayat barang...")%></span>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div class="bg-light p-3 border-top d-flex flex-column flex-md-row justify-content-between align-items-center gap-3">
                <div class="small fw-bold text-muted" id="pagingInfoBrg<%=rnd%>"></div>
                <div class="d-flex align-items-center bg-white rounded-pill shadow-sm border p-1" id="pagingContainerBrg<%=rnd%>">
                    <button class="btn btn-sm btn-light rounded-pill px-3 fw-bold text-info" id="btnPrevPageBrg<%=rnd%>" onclick="changePageBarang<%=rnd%>(-1)">
                        <i class="fas fa-chevron-left me-1"></i><span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Sebelumnya")%></span>
                    </button>
                    <div class="px-3 fw-bold text-dark small">
                        <span id="lblCurrentPageBrg<%=rnd%>">1</span> / <span id="lblTotalPageBrg<%=rnd%>">1</span>
                    </div>
                    <button class="btn btn-sm btn-light rounded-pill px-3 fw-bold text-info" id="btnNextPageBrg<%=rnd%>" onclick="changePageBarang<%=rnd%>(1)">
                        <span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Selanjutnya")%></span><i class="fas fa-chevron-right ms-1"></i>
                    </button>
                </div>
            </div>
        </div>
    </div>

</div>

<script>
    // ==========================================
    // VARIABEL GLOBAL
    // ==========================================
    const memberIdBarang<%=rnd%> = <%=idMemberAktif%>;
    let currentPgBrg<%=rnd%> = 1;
    const limitPgBrg<%=rnd%> = 10;
    let totalRecBrg<%=rnd%> = 0;

    const fmtRupiah<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);
    };

    const fetchApiBrg<%=rnd%> = async (payload) => {
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            return await res.json();
        } catch (e) { 
            console.error(e); 
            return { status: "error", message: "<%=Common.getBahasaConfig("Terjadi kesalahan koneksi jaringan.")%>" }; 
        }
    };

    const initDateFiltersBrg<%=rnd%> = () => {
        const today = new Date();
        const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
        
        const todayStr = today.getFullYear() + '-' + String(today.getMonth() + 1).padStart(2, '0') + '-' + String(today.getDate()).padStart(2, '0');
        const firstDayStr = firstDay.getFullYear() + '-' + String(firstDay.getMonth() + 1).padStart(2, '0') + '-01';
        
        document.getElementById('endDateBrg<%=rnd%>').value = todayStr;
        document.getElementById('startDateBrg<%=rnd%>').value = firstDayStr;
    };

    // ==========================================
    // LOGIKA PENCARIAN & PAGINASI
    // ==========================================
    const getFilterQueryBrg<%=rnd%> = () => {
        const tglAwal = document.getElementById('startDateBrg<%=rnd%>').value;
        const tglAkhir = document.getElementById('endDateBrg<%=rnd%>').value;
        const keyword = document.getElementById('searchKeywordBrg<%=rnd%>').value.trim().replace(/'/g, "''");

        // Filter Wajib: Hanya transaksi dari member yang sedang login
        let filters = " WHERE a.anggota_koperasi = " + memberIdBarang<%=rnd%> + " ";

        if (tglAwal !== "" && tglAkhir !== "") {
            filters += " AND a.waktu BETWEEN '" + tglAwal + " 00:00:00' AND '" + tglAkhir + " 23:59:59' ";
        }
        
        if (keyword !== '') {
            // Pencarian ditambahkan untuk kode barang (c.kode), nama barang (c.nama), dan nama toko (b.nama)
            filters += " AND (c.nama ILIKE '%" + keyword + "%' OR c.kode ILIKE '%" + keyword + "%' OR b.nama ILIKE '%" + keyword + "%') ";
        }
        return filters;
    };

    const resetAndLoadBarang<%=rnd%> = () => {
        currentPgBrg<%=rnd%> = 1;
        loadDataBarang<%=rnd%>();
    };

    const changePageBarang<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecBrg<%=rnd%> / limitPgBrg<%=rnd%>);
        if (direction === -1 && currentPgBrg<%=rnd%> > 1) {
            currentPgBrg<%=rnd%>--;
            loadDataBarang<%=rnd%>();
        } else if (direction === 1 && currentPgBrg<%=rnd%> < totalPages) {
            currentPgBrg<%=rnd%>++;
            loadDataBarang<%=rnd%>();
        }
    };

    const loadDataBarang<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelBarang<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="6" class="text-center py-5"><div class="spinner-border text-info mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Sistem sedang memuat rincian barang...")%></span></td></tr>';

        const sqlFilters = getFilterQueryBrg<%=rnd%>();
        const offset = (currentPgBrg<%=rnd%> - 1) * limitPgBrg<%=rnd%>;

        // MENGGUNAKAN QUERY SESUAI PERMINTAAN ANDA
        const sqlCount = "SELECT COUNT(*) AS jumlah " +
                         "FROM koperasi.pembelian a " +
                         "INNER JOIN koperasi.toko b ON (a.toko = b.id and b.aktif = true) " +
                         "INNER JOIN koperasi.produk c ON (c.id = a.produk) " +
                         sqlFilters + ";";
        
        const sqlData = "SELECT TO_CHAR(a.waktu, 'DD Mon YYYY HH24:MI') as waktu_trx, c.nama AS namabarang, a.member, b.nama AS pedagang, a.jenismember, a.carabayar, a.qty, a.total " +
                        "FROM koperasi.pembelian a " +
                        "INNER JOIN koperasi.toko b ON (a.toko = b.id and b.aktif = true) " +
                        "INNER JOIN koperasi.produk c ON (c.id = a.produk) " +
                        sqlFilters +
                        " ORDER BY a.waktu DESC, a.id DESC LIMIT " + limitPgBrg<%=rnd%> + " OFFSET " + offset + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetchApiBrg<%=rnd%>({ action: "sql", sql: sqlCount }),
                fetchApiBrg<%=rnd%>({ action: "sql", sql: sqlData })
            ]);

            totalRecBrg<%=rnd%> = (resCount.data && resCount.data[0]) ? parseInt(resCount.data[0].jumlah || 0) : 0;
            const recordList = resData.data || [];

            let htmlList = '';

            if (recordList.length === 0) {
                htmlList = '<tr><td colspan="6" class="text-center text-muted py-5"><i class="fas fa-box-open fa-3x mb-3 opacity-25"></i><br><%=Common.getBahasaConfig("Tidak ada data produk yang ditemukan pada periode ini.")%></td></tr>';
            } else {
                let no = offset + 1; 
                recordList.forEach((row) => {
                    const toko = row.pedagang || '<%=Common.getBahasaConfigJS("Koperasi Utama")%>';
                    const produk = row.namabarang || '<%=Common.getBahasaConfigJS("Item Tidak Diketahui")%>';
                    const qty = parseInt(row.qty || 0);
                    const total = parseFloat(row.total || 0);

                    htmlList += '<tr class="border-bottom">' +
                            '<td class="text-center text-muted fw-medium">' + (no++) + '</td>' +
                            '<td>' +
                                '<div class="fw-bold text-dark mb-1" style="font-size: 13px;">' + (row.waktu_trx || '-') + '</div>' +
                                '<small class="text-muted" style="font-size: 11px;"><i class="fas fa-wallet me-1 text-info"></i>' + (row.carabayar || '-') + '</small>' +
                            '</td>' +
                            '<td>' +
                                '<div class="fw-bold text-primary">' + produk + '</div>' +
                            '</td>' +
                            '<td><span class="badge bg-light text-secondary border px-2 py-1"><i class="fas fa-store me-1"></i>' + toko + '</span></td>' +
                            '<td class="text-center fw-bold text-dark">' + qty + '</td>' +
                            '<td class="text-end fw-bolder text-success fs-6">' + fmtRupiah<%=rnd%>(total) + '</td>' +
                        '</tr>';
                });
            }
            
            tbody.innerHTML = htmlList;
            updatePagingUIBrg<%=rnd%>();

        } catch (error) {
            console.error("Error loading items:", error);
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3"></i><br><%=Common.getBahasaConfig("Gagal menarik data dari peladen.")%></td></tr>';
        }
    };

    const updatePagingUIBrg<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecBrg<%=rnd%> / limitPgBrg<%=rnd%>) || 1;
        document.getElementById('lblCurrentPageBrg<%=rnd%>').innerText = currentPgBrg<%=rnd%>;
        document.getElementById('lblTotalPageBrg<%=rnd%>').innerText = totalPages;
        
        const startIdx = totalRecBrg<%=rnd%> > 0 ? ((currentPgBrg<%=rnd%> - 1) * limitPgBrg<%=rnd%> + 1) : 0;
        const endIdx = Math.min(currentPgBrg<%=rnd%> * limitPgBrg<%=rnd%>, totalRecBrg<%=rnd%>);
        
        document.getElementById('pagingInfoBrg<%=rnd%>').innerHTML = '<%=Common.getBahasaConfigJS("Menampilkan")%> <span class="text-dark">' + startIdx + ' - ' + endIdx + '</span> <%=Common.getBahasaConfig("dari")%> <span class="text-dark">' + totalRecBrg<%=rnd%> + '</span> <%=Common.getBahasaConfig("item barang")%>';
        
        document.getElementById('btnPrevPageBrg<%=rnd%>').disabled = (currentPgBrg<%=rnd%> === 1);
        document.getElementById('btnNextPageBrg<%=rnd%>').disabled = (currentPgBrg<%=rnd%> === totalPages);
    };

    // ==========================================
    // INISIALISASI
    // ==========================================
    const initRiwayatBarang<%=rnd%> = () => {
        initDateFiltersBrg<%=rnd%>();
        
        const filterInputs = document.querySelectorAll('.filter-brg-<%=rnd%>');
        filterInputs.forEach(input => {
            input.addEventListener("keydown", (event) => {
                if (event.key === "Enter") {
                    event.preventDefault(); 
                    resetAndLoadBarang<%=rnd%>();
                }
            });
        });

        loadDataBarang<%=rnd%>();
    };

    // Timeout sedikit membantu kelancaran render jika ditaruh dalam Bootstrap Tabs
    setTimeout(initRiwayatBarang<%=rnd%>, 150);

</script>