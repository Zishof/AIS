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
                <i class="fas fa-history me-2"></i><%=Common.getBahasaConfig("Riwayat Transaksi Saya")%>
            </h4>
            <p class="text-muted small mb-0"><%=Common.getBahasaConfig("Pantau seluruh aktivitas pembelanjaan Anda di koperasi dan kantin.")%></p>
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
                    <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Pencarian Toko")%></label>
                    <div class="input-group shadow-sm">
                        <span class="input-group-text bg-light border-end-0 text-muted"><i class="fas fa-search"></i></span>
                        <input type="text" class="form-control border-start-0 filter-input-<%=rnd%>" id="searchKeyword<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik nama toko...")%>">
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
                            <th class="fw-bold text-uppercase small py-3 text-center"><%=Common.getBahasaConfig("Waktu")%></th>
                            <th class="fw-bold text-uppercase small text-start"><%=Common.getBahasaConfig("Toko Penyedia")%></th>
                            <th class="fw-bold text-uppercase small text-center"><%=Common.getBahasaConfig("Metode Pembayaran")%></th>
                            <th class="text-end fw-bold text-uppercase small"><%=Common.getBahasaConfig("Total Diskon")%></th>
                            <th class="text-end fw-bold text-uppercase small"><%=Common.getBahasaConfig("Cashback")%></th>
                            <th class="text-end fw-bold text-uppercase small"><%=Common.getBahasaConfig("Total Biaya")%></th>
                            <th class="text-center fw-bold text-uppercase small" style="width: 100px;"><%=Common.getBahasaConfig("Aksi")%></th>
                        </tr>
                    </thead>
                    <tbody id="tableRiwayat<%=rnd%>">
                        <tr>
                            <td colspan="7" class="text-center py-5">
                                <div class="spinner-border text-info mb-3"></div>
                                <br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Menyiapkan data riwayat transaksi...")%></span>
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
            filters += " AND a.tanggal_pembayaran BETWEEN '" + tglAwal + " 00:00:00' AND '" + tglAkhir + " 23:59:59' ";
        }
        
        if (keyword !== '') {
            filters += " AND b.nama ILIKE '%" + keyword + "%' ";
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
        
        tbody.innerHTML = '<tr><td colspan="' + colSpan + '" class="text-center py-5 text-muted"><div class="spinner-border text-info mb-3" role="status"></div><br><span class="fw-medium"><%=Common.getBahasaConfig("Memuat data riwayat...")%></span></td></tr>';

        const offset = (currentPageRw<%=rnd%> - 1) * limitPerPageRw<%=rnd%>;
        const sqlFilters = buildFiltersRiwayat<%=rnd%>();

        const sqlQueryData = "SELECT a.id, TO_CHAR(a.tanggal_pembayaran, 'DD Mon YYYY HH24:MI') as tanggal_pembayaran, c.nama AS nama_pembeli, b.nama AS pedagang, d.nama as carabayar, a.total_diskon, a.totalcashback, a.total_biaya " +
                             "FROM koperasi.pembelian_anggota_koperasi a " +
                             "LEFT JOIN koperasi.toko b ON (a.toko = b.id) " +
                             "LEFT JOIN koperasi.anggota_koperasi c ON (c.id = a.anggota_koperasi) " +
                             "LEFT JOIN koperasi.cara_pembayaran_koperasi d ON (d.id = a.cara_pembayaran_koperasi) " +
                             sqlFilters +
                             " ORDER BY a.tanggal_pembayaran DESC, a.id DESC LIMIT " + limitPerPageRw<%=rnd%> + " OFFSET " + offset + ";";

        const sqlQueryCount = "SELECT COUNT(*) AS jumlah " +
                              "FROM koperasi.pembelian_anggota_koperasi a " +
                              "LEFT JOIN koperasi.toko b ON (a.toko = b.id) " +
                              "LEFT JOIN koperasi.anggota_koperasi c ON (c.id = a.anggota_koperasi) " +
                              "LEFT JOIN koperasi.cara_pembayaran_koperasi d ON (d.id = a.cara_pembayaran_koperasi) " +
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
                htmlTbody = '<tr><td colspan="' + colSpan + '" class="text-center text-muted py-5"><i class="fas fa-folder-open fa-3x mb-3 text-secondary opacity-50 d-block"></i><span class="fw-medium"><%=Common.getBahasaConfig("Tidak ada data transaksi yang ditemukan.")%></span></td></tr>';
            } else {
                recordList.forEach(row => {
                    const badgeColor = (row.carabayar || '').toLowerCase().includes('tunai') ? 'bg-secondary' : 'bg-info text-dark';
                    
                    const safeTgl = (row.tanggal_pembayaran || '-');
                    const safePembeli = (row.nama_pembeli || '-').replace(/'/g, "\\'");
                    const safePedagang = (row.pedagang || '<%=Common.getBahasaConfigJS("Koperasi Utama")%>').replace(/'/g, "\\'");

                    htmlTbody += '<tr class="border-bottom">';
                    htmlTbody += '<td class="text-center"><span class="text-muted small fw-medium">' + safeTgl + '</span></td>';
                    htmlTbody += '<td class="text-start fw-bold text-dark">' + safePedagang + '</td>';
                    htmlTbody += '<td class="text-center"><span class="badge ' + badgeColor + ' bg-opacity-10 border px-3 py-1 shadow-sm">' + (row.carabayar || '-') + '</span></td>';
                    
                    htmlTbody += '<td class="text-end fw-bold text-danger px-3">' + formatRp<%=rnd%>(row.total_diskon) + '</td>';
                    htmlTbody += '<td class="text-end fw-bold text-info px-3">' + formatRp<%=rnd%>(row.totalcashback) + '</td>';
                    htmlTbody += '<td class="text-end fw-bolder text-success fs-6 px-3">' + formatRp<%=rnd%>(row.total_biaya) + '</td>';
                    
                    // Kolom Aksi HANYA CETAK (Karena ini member)
                    htmlTbody += '<td class="text-center text-nowrap">' +
                                    '<button id="btnCetak_' + row.id + '" class="btn btn-sm btn-outline-primary rounded-pill px-3 shadow-sm fw-bold" onclick="cetakUlangStruk<%=rnd%>(' + row.id + ', \'' + safeTgl + '\', ' + (row.total_biaya || 0) + ', ' + (row.total_diskon || 0) + ', ' + (row.totalcashback || 0) + ', \'' + safePembeli + '\', \'' + safePedagang + '\')" title="<%=Common.getBahasaConfig("Cetak Struk")%>"><i class="fas fa-print me-1"></i><%=Common.getBahasaConfig("Cetak")%></button>' +
                                 '</td>';
                    
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
        
        document.getElementById('pagingInfo<%=rnd%>').innerHTML = '<%=Common.getBahasaConfigJS("Menampilkan")%> <span class="text-dark">' + startIdx + ' - ' + endIdx + '</span> <%=Common.getBahasaConfig("dari total")%> <span class="text-dark">' + totalRecordsRw<%=rnd%> + '</span> <%=Common.getBahasaConfig("transaksi")%>';
        
        document.getElementById('btnPrevPage<%=rnd%>').disabled = (currentPageRw<%=rnd%> === 1);
        document.getElementById('btnNextPage<%=rnd%>').disabled = (currentPageRw<%=rnd%> === totalPages);
    };

    // ==========================================
    // FUNGSI KHUSUS MENCETAK ULANG STRUK
    // ==========================================
    const cetakUlangStruk<%=rnd%> = async (idTransaksi, waktuT, totalBiaya, totalDiskon, totalCashback, namaPembeli, pedagang) => {
        const btn = document.getElementById('btnCetak_' + idTransaksi);
        const originalHtml = btn.innerHTML;
        if(btn) btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        
        try {
            const sqlDetail = "SELECT c.nama, a.hargajual as harga, a.qty as jumlah, COALESCE(a.diskon, 0) as diskon, COALESCE(a.cashback, 0) as cashback " +
                              "FROM koperasi.pembelian a " +
                              "INNER JOIN koperasi.produk c ON c.id = a.produk " +
                              "WHERE a.pembelian_anggota_koperasi = " + idTransaksi;
                              
            const details = await fetchData<%=rnd%>(sqlDetail);
            
            const payload = {
                waktu: waktuT,
                kodeUnik: "R-" + idTransaksi,
                namaPembeli: namaPembeli,
                namaToko: pedagang && pedagang !== '-' ? pedagang : '<%=Common.getBahasaConfigJS("Koperasi Utama")%>',
                transaksi: details
            };
            
            cetakStrukRiwayat<%=rnd%>(payload, totalBiaya, totalDiskon, totalCashback);
            
        } catch (e) {
            alert("<%=Common.getBahasaConfig("Gagal mengambil detail transaksi untuk dicetak.")%>");
        } finally {
            if(btn) btn.innerHTML = originalHtml;
        }
    };

    const cetakStrukRiwayat<%=rnd%> = (payload, totalBiaya, totalDiskon, totalCashback) => {
        let htmlStruk = '<html><head><title><%=Common.getBahasaConfig("Cetak Salinan Struk")%></title>';
        htmlStruk += '<style>';
        htmlStruk += 'body { font-family: "Courier New", Courier, monospace; width: 300px; font-size: 12px; margin: 0 auto; padding: 10px; color: #000; } ';
        htmlStruk += '.text-center { text-align: center; } ';
        htmlStruk += '.text-right { text-align: right; } ';
        htmlStruk += '.bold { font-weight: bold; } ';
        htmlStruk += '.border-top { border-top: 1px dashed #000; margin-top: 5px; padding-top: 5px; } ';
        htmlStruk += '.border-bottom { border-bottom: 1px dashed #000; margin-bottom: 5px; padding-bottom: 5px; } ';
        htmlStruk += 'table { width: 100%; border-collapse: collapse; } ';
        htmlStruk += 'td { vertical-align: top; } ';
        htmlStruk += '</style></head><body>';
        
        htmlStruk += '<div class="text-center bold border-bottom">';
        htmlStruk += '<h3 style="margin:5px 0;">' + payload.namaToko + '</h3>';
        htmlStruk += '<div style="margin-bottom:5px;"><%=Common.getBahasaConfig("Salinan Struk Transaksi")%></div>';
        htmlStruk += '</div>';
        
        htmlStruk += '<div style="margin-bottom:5px;">';
        htmlStruk += '<%=Common.getBahasaConfigJS("Waktu")%>  : ' + payload.waktu + '<br>';
        htmlStruk += '<%=Common.getBahasaConfigJS("No Ref")%> : ' + payload.kodeUnik + '<br>';
        if(payload.namaPembeli && payload.namaPembeli !== '-') {
            htmlStruk += '<%=Common.getBahasaConfigJS("Member")%> : ' + payload.namaPembeli + '<br>';
        }
        htmlStruk += '</div>';
        
        htmlStruk += '<div class="border-top border-bottom">';
        htmlStruk += '<table>';
        
        let subtotalAwal = 0;
        payload.transaksi.forEach(item => {
            const sub = (item.harga * item.jumlah);
            subtotalAwal += sub;
            const subAfterDisc = sub - item.diskon; 

            htmlStruk += '<tr><td colspan="2" class="bold">' + item.nama + '</td></tr>';
            htmlStruk += '<tr><td style="padding-bottom:5px;">' + item.jumlah + ' x ' + formatRp<%=rnd%>(item.harga) + '</td><td class="text-right" style="padding-bottom:5px;">' + formatRp<%=rnd%>(subAfterDisc) + '</td></tr>';
            
            if(item.diskon > 0) {
                htmlStruk += '<tr><td colspan="2" style="padding-bottom:5px;">&nbsp;&nbsp;<i><%=Common.getBahasaConfig("Diskon Promo")%></i></td><td class="text-right">-' + formatRp<%=rnd%>(item.diskon) + '</td></tr>';
            }
            if(item.cashback > 0) {
                htmlStruk += '<tr><td colspan="2" style="padding-bottom:5px;">&nbsp;&nbsp;<i><%=Common.getBahasaConfig("Cashback Didapat")%></i></td><td class="text-right">+' + formatRp<%=rnd%>(item.cashback) + '</td></tr>';
            }
        });
        
        htmlStruk += '</table></div>';
        
        htmlStruk += '<table>';
        htmlStruk += '<tr><td><%=Common.getBahasaConfig("Subtotal")%></td><td class="text-right">' + formatRp<%=rnd%>(subtotalAwal) + '</td></tr>';
        
        if (totalDiskon > 0) {
            htmlStruk += '<tr><td><%=Common.getBahasaConfig("Total Diskon")%></td><td class="text-right">-' + formatRp<%=rnd%>(totalDiskon) + '</td></tr>';
        }
        
        htmlStruk += '<tr><td class="bold" style="font-size:14px;"><%=Common.getBahasaConfig("Total Bayar")%></td><td class="text-right bold" style="font-size:14px;">' + formatRp<%=rnd%>(totalBiaya) + '</td></tr>';
        
        if (totalCashback > 0) {
            htmlStruk += '<tr><td class="bold text-right" colspan="2" style="padding-top:10px;"><%=Common.getBahasaConfig("Total Cashback")%>: +' + formatRp<%=rnd%>(totalCashback) + '</td></tr>';
        }
        
        htmlStruk += '</table>';
        htmlStruk += '<div class="text-center border-top" style="margin-top: 15px;"><%=Common.getBahasaConfig("Terima Kasih")%><br><%=Common.getBahasaConfig("Selamat Belanja Kembali")%></div>';
        htmlStruk += '</body></html>';

        try{
            const printWindow = window.open('', '', 'height=600,width=400');
            printWindow.document.write(htmlStruk);
            printWindow.document.close();
            printWindow.focus();
            setTimeout(() => {
                printWindow.print();
            }, 500);
        }catch(e){}
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