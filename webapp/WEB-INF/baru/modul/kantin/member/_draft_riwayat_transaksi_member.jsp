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

<div class="container-fluid py-3 py-md-4 animate__animated animate__fadeIn" style="background-color: #f4f7f6; min-height: 100vh;">

    <div class="d-flex flex-column flex-md-row justify-content-between align-items-center mb-4 gap-2">
        <div class="text-center text-md-start">
            <h4 class="fw-bolder text-primary mb-1">
                <i class="fas fa-clipboard-list me-2"></i><%=Common.getBahasaConfig("Draft & Status Pesanan Saya")%>
            </h4>
            <p class="text-muted small mb-0"><%=Common.getBahasaConfig("Pantau pesanan Anda yang sedang diproses atau belum dibayarkan.")%></p>
        </div>
    </div>

    <div class="card border-0 shadow-sm rounded-4 mb-4 border-top border-primary border-3">
        <div class="card-body p-3 p-md-4">
            <div class="row g-3 align-items-end">
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Tanggal Awal")%></label>
                    <div class="input-group shadow-sm">
                        <span class="input-group-text bg-light border-end-0 text-primary"><i class="fas fa-calendar-alt"></i></span>
                        <input type="date" class="form-control border-start-0 fw-bold text-dark filter-input-<%=rnd%>" id="startDate<%=rnd%>">
                    </div>
                </div>
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Tanggal Akhir")%></label>
                    <div class="input-group shadow-sm">
                        <span class="input-group-text bg-light border-end-0 text-primary"><i class="fas fa-calendar-check"></i></span>
                        <input type="date" class="form-control border-start-0 fw-bold text-dark filter-input-<%=rnd%>" id="endDate<%=rnd%>">
                    </div>
                </div>
                <div class="col-md-4">
                    <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Pencarian Toko / Kode")%></label>
                    <div class="input-group shadow-sm">
                        <span class="input-group-text bg-light border-end-0 text-muted"><i class="fas fa-search"></i></span>
                        <input type="text" class="form-control border-start-0 filter-input-<%=rnd%>" id="searchKeyword<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik nama toko atau kode...")%>">
                    </div>
                </div>
                <div class="col-md-2 d-grid">
                    <button class="btn btn-primary rounded-3 shadow-sm fw-bold" onclick="resetAndLoadRiwayat<%=rnd%>()">
                        <i class="fas fa-filter me-2"></i><%=Common.getBahasaConfig("Terapkan")%>
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
                            <th class="fw-bold text-uppercase small py-3 text-center"><%=Common.getBahasaConfig("Waktu Pesan")%></th>
                            <th class="fw-bold text-uppercase small text-start"><%=Common.getBahasaConfig("Toko Penyedia")%></th>
                            <th class="fw-bold text-uppercase small text-start"><%=Common.getBahasaConfig("Cara Bayar & Ket.")%></th>
                            <th class="text-end fw-bold text-uppercase small"><%=Common.getBahasaConfig("Total Diskon")%></th>
                            <th class="text-end fw-bold text-uppercase small"><%=Common.getBahasaConfig("Cashback")%></th>
                            <th class="text-end fw-bold text-uppercase small"><%=Common.getBahasaConfig("Total Tagihan")%></th>
                            <th class="text-center fw-bold text-uppercase small" style="width: 150px;"><%=Common.getBahasaConfig("Status / Aksi")%></th>
                        </tr>
                    </thead>
                    <tbody id="tableRiwayat<%=rnd%>">
                        <tr>
                            <td colspan="7" class="text-center py-5">
                                <div class="spinner-border text-info mb-3"></div>
                                <br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Menyiapkan data pesanan Anda...")%></span>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div class="bg-light p-3 border-top d-flex flex-column flex-md-row justify-content-between align-items-center gap-3">
                <div class="small fw-bold text-muted" id="pagingInfo<%=rnd%>"></div>
                <div class="d-flex align-items-center bg-white rounded-pill shadow-sm border p-1" id="pagingContainer<%=rnd%>">
                    <button class="btn btn-sm btn-light rounded-pill px-3 fw-bold text-primary" id="btnPrevPage<%=rnd%>" onclick="changePage<%=rnd%>(-1)">
                        <i class="fas fa-chevron-left me-1"></i><span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Sebelumnya")%></span>
                    </button>
                    <div class="px-3 fw-bold text-dark small">
                        <span id="lblCurrentPage<%=rnd%>">1</span> / <span id="lblTotalPage<%=rnd%>">1</span>
                    </div>
                    <button class="btn btn-sm btn-light rounded-pill px-3 fw-bold text-primary" id="btnNextPage<%=rnd%>" onclick="changePage<%=rnd%>(1)">
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
    const masterModelClass<%=rnd%> = "ais.database.model.koperasi.DraftPembelianAnggotaKoperasi";
    const idMemberAktif<%=rnd%> = <%=idMemberAktif%>;
    let currentPageRw<%=rnd%> = 1;
    const limitPerPageRw<%=rnd%> = 10;
    let totalRecordsRw<%=rnd%> = 0;

    const formatRp<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);
    };

    const fetchData<%=rnd%> = async (sql) => {
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: "sql", sql: sql })
            });
            const result = await res.json();
            return result.data || [];
        } catch (e) { 
            console.error(e); 
            return []; 
        }
    };

    const setDefaultDates<%=rnd%> = () => {
        const today = new Date();
        const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
        
        const todayStr = today.getFullYear() + '-' + String(today.getMonth() + 1).padStart(2, '0') + '-' + String(today.getDate()).padStart(2, '0');
        const firstDayStr = firstDay.getFullYear() + '-' + String(firstDay.getMonth() + 1).padStart(2, '0') + '-01';
        
        document.getElementById('endDate<%=rnd%>').value = todayStr;
        document.getElementById('startDate<%=rnd%>').value = firstDayStr;
    };

    // ==========================================
    // LOGIKA PENCARIAN & PAGINASI
    // ==========================================
    const buildFiltersRiwayat<%=rnd%> = () => {
        const tglAwal = document.getElementById('startDate<%=rnd%>').value;
        const tglAkhir = document.getElementById('endDate<%=rnd%>').value;
        const keyword = document.getElementById('searchKeyword<%=rnd%>').value.trim().replace(/'/g, "''");

        // Wajib: Hanya tampilkan transaksi milik member yang sedang login
        let filters = " WHERE a.anggota_koperasi = " + idMemberAktif<%=rnd%> + " ";

        if (tglAwal !== "" && tglAkhir !== "") {
            filters += " AND DATE(a.tanggal_pembayaran) BETWEEN DATE('" + tglAwal + "') AND DATE('" + tglAkhir + "') ";
        }
        
        if (keyword !== '') {
            filters += " AND (b.nama ILIKE '%" + keyword + "%' OR a.kode ILIKE '%" + keyword + "%') ";
        }
        return filters;
    };

    const resetAndLoadRiwayat<%=rnd%> = () => {
        currentPageRw<%=rnd%> = 1;
        loadRiwayatAPI<%=rnd%>();
    };

    const changePage<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecordsRw<%=rnd%> / limitPerPageRw<%=rnd%>);
        if (direction === -1 && currentPageRw<%=rnd%> > 1) {
            currentPageRw<%=rnd%>--;
            loadRiwayatAPI<%=rnd%>();
        } else if (direction === 1 && currentPageRw<%=rnd%> < totalPages) {
            currentPageRw<%=rnd%>++;
            loadRiwayatAPI<%=rnd%>();
        }
    };

    const loadRiwayatAPI<%=rnd%> = async () => {
        const tbody = document.getElementById('tableRiwayat<%=rnd%>');
        const colSpan = 7; 
        
        tbody.innerHTML = '<tr><td colspan="' + colSpan + '" class="text-center py-5 text-muted"><div class="spinner-border text-info mb-3" role="status"></div><br><span class="fw-medium"><%=Common.getBahasaConfig("Memuat data pesanan...")%></span></td></tr>';

        const offset = (currentPageRw<%=rnd%> - 1) * limitPerPageRw<%=rnd%>;
        const sqlFilters = buildFiltersRiwayat<%=rnd%>();

        // Mengambil data dari draft_pembelian_anggota_koperasi
        const sqlQueryData = "SELECT a.id, TO_CHAR(a.tanggal_pembayaran, 'DD Mon YYYY HH24:MI') as tanggal_pembayaran, d.nama as kode, a.keterangan, b.nama AS pedagang, COALESCE(a.total_diskon, 0) as total_diskon, COALESCE(a.totalcashback, 0) as totalcashback, COALESCE(a.total_biaya, 0) as total_biaya, a.lunas " +
                             "FROM koperasi.draft_pembelian_anggota_koperasi a " +
                             "LEFT JOIN koperasi.toko b ON (a.toko = b.id) " +
                             "LEFT JOIN koperasi.cara_pembayaran_koperasi d ON (d.id = a.cara_pembayaran_koperasi) " +
                             sqlFilters +
                             " ORDER BY a.id DESC LIMIT " + limitPerPageRw<%=rnd%> + " OFFSET " + offset + ";";

        const sqlQueryCount = "SELECT COUNT(*) AS jumlah " +
                              "FROM koperasi.draft_pembelian_anggota_koperasi a " +
                              "LEFT JOIN koperasi.toko b ON (a.toko = b.id) " +
                              sqlFilters + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetchData<%=rnd%>(sqlQueryCount),
                fetchData<%=rnd%>(sqlQueryData)
            ]);

            totalRecordsRw<%=rnd%> = (resCount && resCount[0]) ? parseInt(resCount[0].jumlah || 0) : 0;
            const recordList = resData || [];

            let htmlTbody = '';
            if (recordList.length === 0) {
                htmlTbody = '<tr><td colspan="' + colSpan + '" class="text-center text-muted py-5"><i class="fas fa-clipboard-check fa-3x mb-3 text-secondary opacity-50 d-block"></i><span class="fw-medium"><%=Common.getBahasaConfig("Tidak ada data pesanan yang ditemukan.")%></span></td></tr>';
            } else {
                recordList.forEach(row => {
                    const safeTgl = (row.tanggal_pembayaran || '-');
                    const safePedagang = (row.pedagang || '<%=Common.getBahasaConfigJS("Kantin Utama")%>').replace(/'/g, "\\'");
                    const safeKode = (row.kode || '-');
                    const safeKet = (row.keterangan || '-');
                    
                    const isLunas = row.lunas !== null && row.lunas !== '';

                    let actBtn = '';
                    if (!isLunas) {
                        // Jika belum lunas, munculkan tombol Batal (Hapus Draft)
                        actBtn = '<button class="btn btn-sm btn-outline-danger rounded-pill px-3 shadow-sm fw-bold w-100" onclick="batalkanPesanan<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Batalkan Pesanan")%>"><i class="fas fa-times-circle me-1"></i><%=Common.getBahasaConfig("Batal")%></button>';
                    } else {
                        // Jika sudah lunas, tombol Batal hilang dan diganti status Lunas/Selesai
                        actBtn = '<span class="badge bg-success w-100 py-2"><i class="fas fa-check-double me-1"></i><%=Common.getBahasaConfig("Lunas")%></span>';
                    }

                    htmlTbody += '<tr class="border-bottom ' + (!isLunas ? 'table-warning' : '') + '">';
                    htmlTbody += '<td class="text-center"><span class="text-muted small fw-medium">' + safeTgl + '</span></td>';
                    htmlTbody += '<td class="text-start fw-bold text-dark">' + safePedagang + '</td>';
                    htmlTbody += '<td class="text-start"><span class="text-primary fw-bold">' + safeKode + '</span><br><small class="text-muted">' + safeKet + '</small></td>';
                    
                    htmlTbody += '<td class="text-end fw-bold text-danger px-3">' + formatRp<%=rnd%>(row.total_diskon) + '</td>';
                    htmlTbody += '<td class="text-end fw-bold text-info px-3">' + formatRp<%=rnd%>(row.totalcashback) + '</td>';
                    htmlTbody += '<td class="text-end fw-bolder text-success fs-6 px-3">' + formatRp<%=rnd%>(row.total_biaya) + '</td>';
                    
                    htmlTbody += '<td class="text-center text-nowrap">' + actBtn + '</td>';
                    
                    htmlTbody += '</tr>';
                });
            }
            tbody.innerHTML = htmlTbody;
            updatePaginationUIRiwayat<%=rnd%>();

        } catch (error) {
            console.error("API Error:", error);
            tbody.innerHTML = '<tr><td colspan="' + colSpan + '" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal mengambil data riwayat.")%></td></tr>';
        }
    };

    const updatePaginationUIRiwayat<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecordsRw<%=rnd%> / limitPerPageRw<%=rnd%>) || 1;
        document.getElementById('lblCurrentPage<%=rnd%>').innerText = currentPageRw<%=rnd%>;
        document.getElementById('lblTotalPage<%=rnd%>').innerText = totalPages;
        
        const startIdx = totalRecordsRw<%=rnd%> > 0 ? ((currentPageRw<%=rnd%> - 1) * limitPerPageRw<%=rnd%> + 1) : 0;
        const endIdx = Math.min(currentPageRw<%=rnd%> * limitPerPageRw<%=rnd%>, totalRecordsRw<%=rnd%>);
        
        document.getElementById('pagingInfo<%=rnd%>').innerHTML = '<%=Common.getBahasaConfigJS("Menampilkan")%> <span class="text-dark">' + startIdx + ' - ' + endIdx + '</span> <%=Common.getBahasaConfig("dari total")%> <span class="text-dark">' + totalRecordsRw<%=rnd%> + '</span> <%=Common.getBahasaConfig("pesanan")%>';
        
        document.getElementById('btnPrevPage<%=rnd%>').disabled = (currentPageRw<%=rnd%> === 1);
        document.getElementById('btnNextPage<%=rnd%>').disabled = (currentPageRw<%=rnd%> === totalPages);
    };

   //==========================================
        // LOGIKA HAPUS / BATALKAN PESANAN DRAFT
        // ==========================================
        const batalkanPesanan<%=rnd%> = async (id) => {
            // Cek status lunas terbaru di database untuk mencegah bentrok data (race condition)
            const sqlCek = "SELECT lunas FROM koperasi.draft_pembelian_anggota_koperasi WHERE id = " + id;
            try {
                const resCek = await fetchData<%=rnd%>(sqlCek);
                if (resCek && resCek.length > 0) {
                    const lunas = resCek[0].lunas;
                    if (lunas !== null && lunas !== "") {
                        tampilkanToast('<%=Common.getBahasaConfigJS("Pesanan tidak dapat dibatalkan karena baru saja diproses/dibayar oleh pihak toko. Tabel akan dimuat ulang.")%>', 'bg-warning text-dark');
                        loadRiwayatAPI<%=rnd%>(); // Reload data untuk menampilkan status terbarunya
                        return;
                    }
                }
            } catch (e) {
                console.error("Gagal mengecek status pesanan:", e);
                tampilkanToast('<%=Common.getBahasaConfigJS("Gagal memverifikasi status pesanan. Silakan periksa koneksi Anda.")%>', 'bg-danger text-white');
                return;
            }

            // Jika masih null (belum lunas), lanjutkan proses hapus
            if (typeof prosesDeleteData === 'function') {
                prosesDeleteData(masterModelClass<%=rnd%>, id, function(){ 
                     if (document.querySelectorAll('#tableRiwayat<%=rnd%> tr').length === 1 && currentPageRw<%=rnd%> > 1) {
                         currentPageRw<%=rnd%>--;
                     }
                     loadRiwayatAPI<%=rnd%>();
                });
            } else {
                 // Fallback apabila fungsi JS bawaan tidak ter-load
                 if(confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin membatalkan pesanan ini?")%>')) {
                     const payload = { action: "deleteData", class: masterModelClass<%=rnd%>, id: id };
                     fetch('<%=Common.ROOT%>/Data', {
                         method: 'POST',
                         headers: { 'Content-Type': 'application/json' },
                         body: JSON.stringify(payload)
                     }).then(res => res.json()).then(res => {
                         if (res.status === '00' || res.status === 'success') {
                             if (document.querySelectorAll('#tableRiwayat<%=rnd%> tr').length === 1 && currentPageRw<%=rnd%> > 1) currentPageRw<%=rnd%>--;
                             loadRiwayatAPI<%=rnd%>();
                             tampilkanToast('<%=Common.getBahasaConfigJS("Berhasil dibatalkan")%>', 'bg-success text-white');
                         } else {
                             tampilkanToast(res.description || '<%=Common.getBahasaConfigJS("Gagal membatalkan pesanan.")%>', 'bg-danger text-white');
                         }
                     });
                 }
            }
        };

    // ==========================================
    // INISIALISASI (MENYELESAIKAN ISU LOAD AWAL)
    // ==========================================
    const initRiwayatTransaksi<%=rnd%> = () => {
        setDefaultDates<%=rnd%>();
        
        const filterInputs = document.querySelectorAll('.filter-input-<%=rnd%>');
        filterInputs.forEach(input => {
            input.addEventListener("keydown", (event) => {
                if (event.key === "Enter") {
                    event.preventDefault(); 
                    resetAndLoadRiwayat<%=rnd%>();
                }
            });
        });

        loadRiwayatAPI<%=rnd%>();
    };

    setTimeout(initRiwayatTransaksi<%=rnd%>, 150);

</script>