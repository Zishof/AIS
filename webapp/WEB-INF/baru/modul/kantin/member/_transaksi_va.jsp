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
String baseUrlCekUlang = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Fmember&s=_check_ulang_pembayaran";
%>

<div class="container-fluid py-2 animate__animated animate__fadeIn" id="riwayatVAContent<%=rnd%>">

    <!-- HEADER -->
    <div class="mb-3">
        <h5 class="fw-bolder text-warning text-darken-2 mb-1">
            <i class="fas fa-file-invoice-dollar me-2"></i><%=Common.getBahasaConfig("Riwayat Tagihan & Top-Up")%>
        </h5>
        <p class="text-muted small mb-0"><%=Common.getBahasaConfig("Daftar tagihan Virtual Account dan status pengisian saldo Anda.")%></p>
    </div>

    <!-- FILTER SECTION -->
    <div class="card border-0 shadow-sm rounded-4 mb-4 border-top border-warning border-3">
        <div class="card-body p-3">
            <div class="row g-3 align-items-end">
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Jatuh Tempo Awal")%></label>
                    <div class="input-group input-group-sm shadow-sm">
                        <span class="input-group-text bg-light border-end-0 text-warning"><i class="fas fa-calendar-alt"></i></span>
                        <input type="date" class="form-control border-start-0 fw-bold text-dark filter-va-<%=rnd%>" id="startDateVA<%=rnd%>">
                    </div>
                </div>
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Jatuh Tempo Akhir")%></label>
                    <div class="input-group input-group-sm shadow-sm">
                        <span class="input-group-text bg-light border-end-0 text-warning"><i class="fas fa-calendar-check"></i></span>
                        <input type="date" class="form-control border-start-0 fw-bold text-dark filter-va-<%=rnd%>" id="endDateVA<%=rnd%>">
                    </div>
                </div>
                <div class="col-md-4">
                    <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Pencarian Bank / Kode VA")%></label>
                    <div class="input-group input-group-sm shadow-sm">
                        <span class="input-group-text bg-light border-end-0 text-muted"><i class="fas fa-search"></i></span>
                        <input type="text" class="form-control border-start-0 filter-va-<%=rnd%>" id="searchKeywordVA<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik nama bank, keterangan, atau kode VA...")%>">
                    </div>
                </div>
                <div class="col-md-2 d-grid">
                    <button class="btn btn-sm btn-warning text-dark rounded-3 shadow-sm fw-bold" onclick="resetAndLoadVA<%=rnd%>()">
                        <i class="fas fa-filter me-1"></i><%=Common.getBahasaConfig("Saring")%>
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- DATA TABLE SECTION -->
    <div class="card border-0 shadow-sm rounded-4 overflow-hidden">
        <div class="card-body p-0">
            <div class="table-responsive px-3 py-2" style="min-height: 400px;">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light text-secondary">
                        <tr>
                            <th class="fw-bold text-uppercase small py-3 text-center" style="width: 50px;">#</th>
                            <th class="fw-bold text-uppercase small"><%=Common.getBahasaConfig("Saluran & Bank")%></th>
                            <th class="fw-bold text-uppercase small"><%=Common.getBahasaConfig("Keterangan Tagihan")%></th>
                            <th class="text-end fw-bold text-uppercase small"><%=Common.getBahasaConfig("Total Tagihan")%></th>
                            <th class="text-center fw-bold text-uppercase small"><%=Common.getBahasaConfig("Batas Waktu (Jatuh Tempo)")%></th>
                            <th class="text-center fw-bold text-uppercase small" style="width: 140px;"><%=Common.getBahasaConfig("Status")%></th>
                        </tr>
                    </thead>
                    <tbody id="tabelDaftarVA<%=rnd%>">
                        <!-- Indikator Loading Default -->
                        <tr>
                            <td colspan="6" class="text-center py-5">
                                <div class="spinner-border text-warning mb-3"></div>
                                <br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Menyiapkan data tagihan VA...")%></span>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- PAGING -->
            <div class="bg-light p-3 border-top d-flex flex-column flex-md-row justify-content-between align-items-center gap-3">
                <div class="small fw-bold text-muted" id="pagingInfoVA<%=rnd%>"></div>
                <div class="d-flex align-items-center bg-white rounded-pill shadow-sm border p-1" id="pagingContainerVA<%=rnd%>">
                    <button class="btn btn-sm btn-light rounded-pill px-3 fw-bold text-warning text-darken-2" id="btnPrevPageVA<%=rnd%>" onclick="changePageVA<%=rnd%>(-1)">
                        <i class="fas fa-chevron-left me-1"></i><span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Sebelumnya")%></span>
                    </button>
                    <div class="px-3 fw-bold text-dark small">
                        <span id="lblCurrentPageVA<%=rnd%>">1</span> / <span id="lblTotalPageVA<%=rnd%>">1</span>
                    </div>
                    <button class="btn btn-sm btn-light rounded-pill px-3 fw-bold text-warning text-darken-2" id="btnNextPageVA<%=rnd%>" onclick="changePageVA<%=rnd%>(1)">
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
    const memberIdVA<%=rnd%> = <%=idMemberAktif%>;
    const baseUrlCekUlangAPI<%=rnd%> = '<%=baseUrlCekUlang%>';
    let currentPgVA<%=rnd%> = 1;
    const limitPgVA<%=rnd%> = 10;
    let totalRecVA<%=rnd%> = 0;

    const fmtRupiahVA<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);
    };

    const showToastUI<%=rnd%> = (msg, colorClass) => {
        if(typeof tampilkanToast === "function") {
            tampilkanToast(msg, colorClass);
        } else {
            alert(msg);
        }
    };

    const fetchApiVA<%=rnd%> = async (payload) => {
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

    const initDateFiltersVA<%=rnd%> = () => {
        const today = new Date();
        const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
        
        // Default menampilkan tagihan yang jatuh temponya mulai dari awal bulan ini sampai akhir bulan ini
        const lastDay = new Date(today.getFullYear(), today.getMonth() + 1, 0);
        
        const lastDayStr = lastDay.getFullYear() + '-' + String(lastDay.getMonth() + 1).padStart(2, '0') + '-' + String(lastDay.getDate()).padStart(2, '0');
        const firstDayStr = firstDay.getFullYear() + '-' + String(firstDay.getMonth() + 1).padStart(2, '0') + '-01';
        
        document.getElementById('endDateVA<%=rnd%>').value = lastDayStr;
        document.getElementById('startDateVA<%=rnd%>').value = firstDayStr;
    };

    // ==========================================
    // LOGIKA CEK ULANG PEMBAYARAN
    // ==========================================
    const cekUlangPembayaran<%=rnd%> = async (idVa) => {
        const btn = document.getElementById('btnCekUlang_' + idVa);
        const originalHtml = btn.innerHTML;
        
        btn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i><%=Common.getBahasaConfig("Mengecek...")%>';
        btn.disabled = true;

        try {
            const response = await fetch(baseUrlCekUlangAPI<%=rnd%> + "&id_va=" + idVa);
            const result = await response.json();

            // Evaluasi respon API dari _chek_ulang_pembayaran.jsp
            if (result.status === 'success') {
                showToastUI<%=rnd%>(result.message, 'bg-success text-white');
            } else if (result.status === 'pending') {
                showToastUI<%=rnd%>(result.message, 'bg-warning text-dark');
            } else {
                showToastUI<%=rnd%>(result.message || '<%=Common.getBahasaConfigJS("Gagal memverifikasi pembayaran.")%>', 'bg-danger text-white');
            }
            
            // Reload tabel otomatis untuk memperbarui status
            loadDataVA<%=rnd%>();

        } catch (error) {
            console.error(error);
            showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan saat mengecek pembayaran.")%>', 'bg-danger text-white');
            
            btn.innerHTML = originalHtml;
            btn.disabled = false;
        }
    };

    // ==========================================
    // LOGIKA PENCARIAN & PAGINASI
    // ==========================================
    const getFilterQueryVA<%=rnd%> = () => {
        const tglAwal = document.getElementById('startDateVA<%=rnd%>').value;
        const tglAkhir = document.getElementById('endDateVA<%=rnd%>').value;
        const keyword = document.getElementById('searchKeywordVA<%=rnd%>').value.trim().replace(/'/g, "''");

        // Filter Wajib: Hanya Virtual Account milik member yang sedang login
        let filters = " WHERE anggota_koperasi = " + memberIdVA<%=rnd%> + " ";

        if (tglAwal !== "" && tglAkhir !== "") {
            filters += " AND kadaluarsa BETWEEN '" + tglAwal + " 00:00:00' AND '" + tglAkhir + " 23:59:59' ";
        }
        
        if (keyword !== '') {
            filters += " AND (kode ILIKE '%" + keyword + "%' OR channel ILIKE '%" + keyword + "%' OR keterangan ILIKE '%" + keyword + "%') ";
        }
        return filters;
    };

    const resetAndLoadVA<%=rnd%> = () => {
        currentPgVA<%=rnd%> = 1;
        loadDataVA<%=rnd%>();
    };

    const changePageVA<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecVA<%=rnd%> / limitPgVA<%=rnd%>);
        if (direction === -1 && currentPgVA<%=rnd%> > 1) {
            currentPgVA<%=rnd%>--;
            loadDataVA<%=rnd%>();
        } else if (direction === 1 && currentPgVA<%=rnd%> < totalPages) {
            currentPgVA<%=rnd%>++;
            loadDataVA<%=rnd%>();
        }
    };

    const loadDataVA<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDaftarVA<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="6" class="text-center py-5"><div class="spinner-border text-warning mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Sistem sedang memuat data tagihan...")%></span></td></tr>';

        const sqlFilters = getFilterQueryVA<%=rnd%>();
        const offset = (currentPgVA<%=rnd%> - 1) * limitPgVA<%=rnd%>;

        const sqlCount = "SELECT COUNT(id) AS jumlah FROM public.virtual_account_bank " + sqlFilters + ";";
        
        const sqlData = "SELECT id, channel as bank, kode, total, keterangan, link, " +
                        "TO_CHAR(kadaluarsawaktu, 'DD Mon YYYY HH24:MI') as batas_waktu, " +
                        "CASE " +
                        "   WHEN waktubayar IS NOT NULL THEN 'LUNAS' " +
                        "   WHEN kadaluarsawaktu < NOW() THEN 'KEDALUWARSA' " +
                        "   ELSE 'MENUNGGU' " +
                        "END as status_bayar " +
                        "FROM public.virtual_account_bank " +
                        sqlFilters + " ORDER BY id DESC LIMIT " + limitPgVA<%=rnd%> + " OFFSET " + offset + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetchApiVA<%=rnd%>({ action: "sql", sql: sqlCount }),
                fetchApiVA<%=rnd%>({ action: "sql", sql: sqlData })
            ]);

            totalRecVA<%=rnd%> = (resCount.data && resCount.data[0]) ? parseInt(resCount.data[0].jumlah || 0) : 0;
            const recordList = resData.data || [];

            let htmlList = '';

            if (recordList.length === 0) {
                htmlList = '<tr><td colspan="6" class="text-center text-muted py-5"><i class="fas fa-check-circle fa-3x mb-3 opacity-25"></i><br><%=Common.getBahasaConfig("Tidak ada data tagihan Virtual Account yang ditemukan.")%></td></tr>';
            } else {
                let no = offset + 1; 
                recordList.forEach((row) => {
                    const bankName = row.bank || '-';
                    const kodeVA = row.kode || '-';
                    const keterangan = row.keterangan || '<%=Common.getBahasaConfigJS("Pengisian Saldo")%>';
                    const total = parseFloat(row.total || 0);

                    let badgeStatus = '';
                    let actionBtn = '';
                    
                    if (row.status_bayar === 'LUNAS') {
                        badgeStatus = '<span class="badge bg-success bg-opacity-10 text-success border border-success px-2 py-1 shadow-sm"><i class="fas fa-check-circle me-1"></i><%=Common.getBahasaConfig("Lunas / Selesai")%></span>';
                    } else if (row.status_bayar === 'KEDALUWARSA') {
                        badgeStatus = '<span class="badge bg-secondary bg-opacity-10 text-secondary border border-secondary px-2 py-1 shadow-sm"><i class="fas fa-times-circle me-1"></i><%=Common.getBahasaConfig("Kedaluwarsa")%></span>';
                    } else {
                        // Menunggu Pembayaran -> Munculkan Tombol Bayar & Cek Ulang
                        badgeStatus = '<span class="badge bg-warning text-dark border border-warning px-2 py-1 shadow-sm"><i class="fas fa-clock me-1"></i><%=Common.getBahasaConfig("Menunggu Pembayaran")%></span>';
                        
                        actionBtn += '<div class="d-flex flex-column gap-1 mt-2">';
                        if (row.link && row.link !== 'null') {
                            actionBtn += '<a href="' + row.link + '" target="_blank" class="btn btn-sm btn-primary rounded-pill fw-bold shadow-sm w-100" style="font-size: 11px;"><i class="fas fa-external-link-alt me-1"></i><%=Common.getBahasaConfig("Bayar")%></a>';
                        }
                        actionBtn += '<button class="btn btn-sm btn-outline-info rounded-pill fw-bold shadow-sm w-100" style="font-size: 11px;" onclick="cekUlangPembayaran<%=rnd%>(' + row.id + ')" id="btnCekUlang_' + row.id + '"><i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Cek Status")%></button>';
                        actionBtn += '</div>';
                    }

                    htmlList += '<tr class="border-bottom">' +
                            '<td class="text-center text-muted fw-medium">' + (no++) + '</td>' +
                            '<td>' +
                                '<div class="fw-bold text-dark mb-1"><i class="fas fa-university text-muted me-1"></i>' + bankName + '</div>' +
                            '</td>' +
                            '<td>' +
                                '<div class="fw-bolder text-primary mb-1">' + kodeVA + '</div>' +
                                '<small class="text-muted">' + keterangan + '</small>' +
                            '</td>' +
                            '<td class="text-end fw-bolder text-dark fs-6">' + fmtRupiahVA<%=rnd%>(total) + '</td>' +
                            '<td class="text-center text-danger fw-semibold small">' + (row.batas_waktu || '-') + '</td>' +
                            '<td class="text-center">' + badgeStatus + actionBtn + '</td>' +
                        '</tr>';
                });
            }
            
            tbody.innerHTML = htmlList;
            updatePagingUIVA<%=rnd%>();

        } catch (error) {
            console.error("Error loading VA:", error);
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3"></i><br><%=Common.getBahasaConfig("Gagal menarik data dari peladen.")%></td></tr>';
        }
    };

    const updatePagingUIVA<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecVA<%=rnd%> / limitPgVA<%=rnd%>) || 1;
        document.getElementById('lblCurrentPageVA<%=rnd%>').innerText = currentPgVA<%=rnd%>;
        document.getElementById('lblTotalPageVA<%=rnd%>').innerText = totalPages;
        
        const startIdx = totalRecVA<%=rnd%> > 0 ? ((currentPgVA<%=rnd%> - 1) * limitPgVA<%=rnd%> + 1) : 0;
        const endIdx = Math.min(currentPgVA<%=rnd%> * limitPgVA<%=rnd%>, totalRecVA<%=rnd%>);
        
        document.getElementById('pagingInfoVA<%=rnd%>').innerHTML = '<%=Common.getBahasaConfigJS("Menampilkan")%> <span class="text-dark">' + startIdx + ' - ' + endIdx + '</span> <%=Common.getBahasaConfig("dari")%> <span class="text-dark">' + totalRecVA<%=rnd%> + '</span> <%=Common.getBahasaConfig("tagihan")%>';
        
        document.getElementById('btnPrevPageVA<%=rnd%>').disabled = (currentPgVA<%=rnd%> === 1);
        document.getElementById('btnNextPageVA<%=rnd%>').disabled = (currentPgVA<%=rnd%> === totalPages);
    };

    // ==========================================
    // INISIALISASI
    // ==========================================
    const initRiwayatVA<%=rnd%> = () => {
        initDateFiltersVA<%=rnd%>();
        
        const filterInputs = document.querySelectorAll('.filter-va-<%=rnd%>');
        filterInputs.forEach(input => {
            input.addEventListener("keydown", (event) => {
                if (event.key === "Enter") {
                    event.preventDefault(); 
                    resetAndLoadVA<%=rnd%>();
                }
            });
        });

        loadDataVA<%=rnd%>();
    };

    // Timeout membantu kelancaran render jika di-include dalam Bootstrap Tabs
    setTimeout(initRiwayatVA<%=rnd%>, 200);

</script>