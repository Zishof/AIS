<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// 1. Pengecekan Sesi & Keamanan Akses
Tbmuser tbmuser = Common.getCurrentUser(request);

if(tbmuser == null) {
    out.print("<div class='text-center p-5'><i class='fas fa-exclamation-triangle fa-3x text-warning mb-3'></i><br><h5 class='text-muted'>" + Common.getBahasaConfig("Akses Ditolak") + "</h5><p>" + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "</p></div>");
    return;
}

String rnd = Common.getGeneratedBarCode(7);
%>

<!-- Impor SheetJS untuk Ekspor/Impor Excel -->
<script data-cfasync="false" src="https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js"></script>

<style>
    /* Kustomisasi UI agar Eye-Catching */
    .qr-preview-<%=rnd%> {
        width: 50px;
        height: 50px;
        object-fit: contain;
        border-radius: 8px;
        border: 1px solid #dee2e6;
        padding: 2px;
        background: #fff;
        transition: transform 0.2s;
    }
    .qr-preview-<%=rnd%>:hover {
        transform: scale(1.5);
        z-index: 100;
        position: relative;
        box-shadow: 0 10px 20px rgba(0,0,0,0.1);
    }
    .upload-box-<%=rnd%> {
        border: 2px dashed #0d6efd;
        border-radius: 12px;
        padding: 30px 20px;
        text-align: center;
        background-color: #f8f9fa;
        cursor: pointer;
        transition: all 0.3s;
    }
    .upload-box-<%=rnd%>:hover {
        background-color: #e9ecef;
        border-color: #0b5ed7;
    }
</style>

<div class="container-fluid py-3 py-md-4 animate__animated animate__fadeIn" style="background-color: #f4f7f6; min-height: 100vh;">

    <!-- VIEW: LIST DATA -->
    <div id="viewListMeja<%=rnd%>" class="d-block">
        
        <!-- HEADER -->
        <div class="d-flex flex-column flex-md-row justify-content-between align-items-center mb-4 gap-3">
            <div class="text-center text-md-start">
                <h4 class="fw-bolder text-primary mb-1">
                    <i class="fas fa-chair me-2"></i><%=Common.getBahasaConfig("Manajemen Meja Kantin")%>
                </h4>
                <p class="text-muted small mb-0"><%=Common.getBahasaConfig("Kelola data meja dan hasilkan QR Code untuk pemesanan member.")%></p>
            </div>
            
            <div class="d-flex flex-wrap gap-2 justify-content-center">
                <button class="btn btn-warning text-dark rounded-pill px-3 shadow-sm fw-bold" onclick="showUploadModalMeja<%=rnd%>()">
                    <i class="fas fa-file-upload me-1"></i><span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Unggah Excel")%></span>
                </button>
                <button class="btn btn-success rounded-pill px-3 shadow-sm fw-bold" onclick="downloadExcelMeja<%=rnd%>()">
                    <i class="fas fa-file-download me-1"></i><span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Unduh Excel")%></span>
                </button>
                <button class="btn btn-info text-white rounded-pill px-3 shadow-sm fw-bold" onclick="cetakQRMassal<%=rnd%>()">
                    <i class="fas fa-qrcode me-1"></i><span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Cetak QR Massal")%></span>
                </button>
                <button class="btn btn-primary rounded-pill px-4 shadow-sm fw-bold" onclick="bukaFormMeja<%=rnd%>()">
                    <i class="fas fa-plus-circle me-1"></i><%=Common.getBahasaConfig("Tambah Meja")%>
                </button>
            </div>
        </div>

        <!-- FILTER SECTION -->
        <div class="card border-0 shadow-sm rounded-4 mb-4 border-top border-primary border-3">
            <div class="card-body p-3 p-md-4">
                <div class="row g-3 align-items-end">
                    <div class="col-md-10">
                        <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Pencarian Data Meja")%></label>
                        <div class="input-group shadow-sm">
                            <span class="input-group-text bg-light border-end-0 text-muted"><i class="fas fa-search"></i></span>
                            <input type="text" class="form-control border-start-0 fw-medium filter-meja-<%=rnd%>" id="searchKeywordMeja<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik kode meja, nama meja, atau keterangan...")%>">
                        </div>
                    </div>
                    <div class="col-md-2 d-grid">
                        <button class="btn btn-primary rounded-3 shadow-sm fw-bold" onclick="resetAndLoadMeja<%=rnd%>()">
                            <i class="fas fa-filter me-2"></i><%=Common.getBahasaConfig("Saring")%>
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
                                <th class="fw-bold text-uppercase small text-center" style="width: 80px;"><%=Common.getBahasaConfig("QR Code")%></th>
                                <th class="fw-bold text-uppercase small"><%=Common.getBahasaConfig("Kode Meja")%></th>
                                <th class="fw-bold text-uppercase small"><%=Common.getBahasaConfig("Nama / Alias Meja")%></th>
                                <th class="fw-bold text-uppercase small"><%=Common.getBahasaConfig("Keterangan")%></th>
                                <th class="text-center fw-bold text-uppercase small" style="width: 100px;"><%=Common.getBahasaConfig("Status")%></th>
                                <th class="text-center fw-bold text-uppercase small" style="width: 160px;"><%=Common.getBahasaConfig("Aksi")%></th>
                            </tr>
                        </thead>
                        <tbody id="tabelDataMeja<%=rnd%>">
                            <tr>
                                <td colspan="7" class="text-center py-5">
                                    <div class="spinner-border text-primary mb-3"></div>
                                    <br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Sistem sedang memuat data meja...")%></span>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <!-- PAGING -->
                <div class="bg-light p-3 border-top d-flex flex-column flex-md-row justify-content-between align-items-center gap-3">
                    <div class="small fw-bold text-muted" id="pagingInfoMeja<%=rnd%>"></div>
                    <div class="d-flex align-items-center bg-white rounded-pill shadow-sm border p-1" id="pagingContainerMeja<%=rnd%>">
                        <button class="btn btn-sm btn-light rounded-pill px-3 fw-bold text-primary" id="btnPrevPageMeja<%=rnd%>" onclick="changePageMeja<%=rnd%>(-1)">
                            <i class="fas fa-chevron-left me-1"></i><span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Sebelumnya")%></span>
                        </button>
                        <div class="px-3 fw-bold text-dark small">
                            <span id="lblCurrentPageMeja<%=rnd%>">1</span> / <span id="lblTotalPageMeja<%=rnd%>">1</span>
                        </div>
                        <button class="btn btn-sm btn-light rounded-pill px-3 fw-bold text-primary" id="btnNextPageMeja<%=rnd%>" onclick="changePageMeja<%=rnd%>(1)">
                            <span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Selanjutnya")%></span><i class="fas fa-chevron-right ms-1"></i>
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- VIEW: FORM DATA -->
    <div id="viewFormMeja<%=rnd%>" class="d-none">
        <div class="row justify-content-center">
            <div class="col-lg-8">
                <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
                    <div class="card-header bg-white border-0 pt-4 pb-0 px-4">
                        <div class="d-flex justify-content-between align-items-center">
                            <h5 class="fw-bold text-dark mb-0" id="formTitleMeja<%=rnd%>">
                                <i class="fas fa-plus-circle text-primary me-2"></i><%=Common.getBahasaConfig("Formulir Meja Kantin")%>
                            </h5>
                            <div class="form-check form-switch fs-5 mb-0">
                                <input class="form-check-input" type="checkbox" id="inputAktifMeja<%=rnd%>" checked style="cursor: pointer;">
                                <label class="form-check-label fs-6 fw-bold text-success" for="inputAktifMeja<%=rnd%>" id="labelAktifMeja<%=rnd%>"><%=Common.getBahasaConfig("Aktif")%></label>
                            </div>
                        </div>
                    </div>
                    <div class="card-body p-4">
                        <form id="formMeja<%=rnd%>" onsubmit="event.preventDefault(); simpanDataMeja<%=rnd%>();">
                            <input type="hidden" id="inputIdMeja<%=rnd%>" value="">
                            
                            <div class="row g-4 mt-1">
                                <div class="col-md-6">
                                    <div class="d-flex justify-content-between align-items-center mb-1">
                                        <label class="form-label small fw-bold text-secondary mb-0"><%=Common.getBahasaConfig("Kode Meja (QR)")%> <span class="text-danger">*</span></label>
                                        <div class="form-check form-switch fs-6 mb-0">
                                            <input class="form-check-input ms-0 me-2" type="checkbox" id="chkManualKodeMeja<%=rnd%>" onchange="toggleManualKodeMeja<%=rnd%>()" style="cursor: pointer;">
                                            <label class="form-check-label fw-bold text-primary small" for="chkManualKodeMeja<%=rnd%>" style="cursor: pointer;"><%=Common.getBahasaConfig("Input Manual")%></label>
                                        </div>
                                    </div>
                                    <input type="text" class="form-control fw-bold text-uppercase bg-light" id="inputKodeMeja<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: TBL-01")%>" required readonly>
                                    <small class="text-muted" style="font-size: 11px;"><%=Common.getBahasaConfig("Kode unik yang akan di-generate menjadi QR Code.")%></small>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label small fw-bold text-secondary mt-1"><%=Common.getBahasaConfig("Nama / Alias Meja")%></label>
                                    <input type="text" class="form-control fw-medium" id="inputNamaMeja<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: Meja Pojok Kaca")%>">
                                </div>
                                <div class="col-12">
                                    <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Keterangan")%></label>
                                    <textarea class="form-control" id="inputKeteranganMeja<%=rnd%>" rows="3" placeholder="<%=Common.getBahasaConfig("Informasi tambahan mengenai meja ini...")%>"></textarea>
                                </div>

                                <div class="col-12 mt-5 text-end border-top pt-4">
                                    <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-bold border shadow-sm" onclick="tutupFormMeja<%=rnd%>()">
                                        <i class="fas fa-times text-danger me-2"></i><%=Common.getBahasaConfig("Batal")%>
                                    </button>
                                    <button type="submit" class="btn btn-primary px-5 rounded-pill shadow-sm fw-bold" id="btnSimpanMeja<%=rnd%>">
                                        <i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Data")%>
                                    </button>
                                </div>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>

</div>

<!-- MODAL UPLOAD EXCEL -->
<div class="modal fade" id="modalUploadExcelMeja<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 rounded-4 shadow-lg">
            <div class="modal-header bg-light border-bottom-0 pb-3">
                <h5 class="modal-title fw-bold text-dark">
                    <i class="fas fa-file-excel text-success me-2"></i><%=Common.getBahasaConfig("Unggah Data Meja (Excel)")%>
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4 text-center">
                <p class="text-muted small mb-4"><%=Common.getBahasaConfig("Pastikan format Excel Anda memiliki kolom: KODE_MEJA, NAMA_MEJA, KETERANGAN, dan AKTIF (TRUE/FALSE).")%></p>
                
                <div class="upload-box-<%=rnd%> shadow-sm mb-3" onclick="document.getElementById('fileExcelMeja<%=rnd%>').click()">
                    <i class="fas fa-cloud-upload-alt fa-3x text-primary mb-2"></i>
                    <h6 class="fw-bold text-dark"><%=Common.getBahasaConfig("Pilih File Excel")%></h6>
                    <p class="small text-success fw-bold mb-0 mt-2" id="namaFileExcelTerpilih<%=rnd%>" style="display: none;"></p>
                </div>
                <input class="form-control d-none" type="file" id="fileExcelMeja<%=rnd%>" accept=".xls,.xlsx" onchange="tampilkanNamaFileExcel<%=rnd%>(this)">
                
                <div id="uploadStatusBoxMeja<%=rnd%>" class="alert alert-info py-2 text-start mt-3" style="display: none;"></div>
                
                <div class="mt-4">
                    <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-bold" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
                    <button type="button" class="btn btn-primary px-4 rounded-pill fw-bold shadow-sm" id="btnProsesExcelMeja<%=rnd%>" onclick="prosesUploadExcelMeja<%=rnd%>()">
                        <i class="fas fa-cogs me-2"></i><%=Common.getBahasaConfig("Proses Data")%>
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    // ==========================================
    // VARIABEL GLOBAL & KONFIGURASI
    // ==========================================
    const masterClassMeja<%=rnd%> = "ais.database.model.koperasi.MejaKantin";
    let currentPageMeja<%=rnd%> = 1;
    const limitPerPageMeja<%=rnd%> = 10;
    let totalRecordsMeja<%=rnd%> = 0;
    let modalUploadMejaInstance<%=rnd%> = null;

    // Helper: Toast Notifikasi
    const showToastUI<%=rnd%> = (msg, colorClass) => {
        if(typeof tampilkanToast === "function") {
            tampilkanToast(msg, colorClass);
        } else {
            alert(msg);
        }
    };

    // Helper: Fetch API
    const fetchApiMeja<%=rnd%> = async (payload) => {
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

    // ==========================================
    // NAVIGASI TAMPILAN
    // ==========================================
    const switchViewMeja<%=rnd%> = (viewId) => {
        document.getElementById('viewListMeja<%=rnd%>').classList.add('d-none');
        document.getElementById('viewListMeja<%=rnd%>').classList.remove('d-block');
        document.getElementById('viewFormMeja<%=rnd%>').classList.add('d-none');
        document.getElementById('viewFormMeja<%=rnd%>').classList.remove('d-block');
        
        document.getElementById(viewId).classList.remove('d-none');
        document.getElementById(viewId).classList.add('d-block');
    };

    // Input Switch Label Aktif
    const uiStatusAktifMeja<%=rnd%> = document.getElementById('inputAktifMeja<%=rnd%>');
    if (uiStatusAktifMeja<%=rnd%>) {
        uiStatusAktifMeja<%=rnd%>.addEventListener('change', function() {
            const lbl = document.getElementById('labelAktifMeja<%=rnd%>');
            if(this.checked) {
                lbl.innerText = '<%=Common.getBahasaConfigJS("Aktif")%>';
                lbl.className = 'form-check-label fs-6 fw-bold text-success';
            } else {
                lbl.innerText = '<%=Common.getBahasaConfigJS("Tidak Aktif")%>';
                lbl.className = 'form-check-label fs-6 fw-bold text-danger';
            }
        });
    }

    // ==========================================
    // READ (TAMPILKAN TABEL & FILTER)
    // ==========================================
    const getFilterQueryMeja<%=rnd%> = () => {
        const keyword = document.getElementById('searchKeywordMeja<%=rnd%>').value.trim().replace(/'/g, "''");
        let filters = " WHERE 1=1 ";
        
        if (keyword !== '') {
            filters += " AND (kode ILIKE '%" + keyword + "%' OR nama ILIKE '%" + keyword + "%' OR keterangan ILIKE '%" + keyword + "%') ";
        }
        return filters;
    };

    const resetAndLoadMeja<%=rnd%> = () => {
        currentPageMeja<%=rnd%> = 1;
        loadDataMeja<%=rnd%>();
    };

    const changePageMeja<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecordsMeja<%=rnd%> / limitPerPageMeja<%=rnd%>);
        if (direction === -1 && currentPageMeja<%=rnd%> > 1) {
            currentPageMeja<%=rnd%>--;
            loadDataMeja<%=rnd%>();
        } else if (direction === 1 && currentPageMeja<%=rnd%> < totalPages) {
            currentPageMeja<%=rnd%>++;
            loadDataMeja<%=rnd%>();
        }
    };

    const loadDataMeja<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataMeja<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Sistem sedang memuat data meja...")%></span></td></tr>';

        const sqlFilters = getFilterQueryMeja<%=rnd%>();
        const offset = (currentPageMeja<%=rnd%> - 1) * limitPerPageMeja<%=rnd%>;

        const sqlCount = "SELECT COUNT(id) AS jumlah FROM koperasi.meja_kantin " + sqlFilters + ";";
        const sqlData = "SELECT id, kode, nama, keterangan, aktif " +
                        "FROM koperasi.meja_kantin " +
                        sqlFilters + " ORDER BY kode ASC LIMIT " + limitPerPageMeja<%=rnd%> + " OFFSET " + offset + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetchApiMeja<%=rnd%>({ action: "sql", sql: sqlCount }),
                fetchApiMeja<%=rnd%>({ action: "sql", sql: sqlData })
            ]);

            totalRecordsMeja<%=rnd%> = (resCount.data && resCount.data[0]) ? parseInt(resCount.data[0].jumlah || 0) : 0;
            const recordList = resData.data || [];

            let htmlList = '';

            if (recordList.length === 0) {
                htmlList = '<tr><td colspan="7" class="text-center text-muted py-5"><i class="fas fa-folder-open fa-3x mb-3 opacity-25"></i><br><%=Common.getBahasaConfig("Tidak ada data meja yang ditemukan.")%></td></tr>';
            } else {
                let no = offset + 1; 
                recordList.forEach((row) => {
                    const isAktif = (row.aktif === null || row.aktif === true || row.aktif === 't' || row.aktif === 'true');
                    const badgeStatus = isAktif ? '<span class="badge bg-success bg-opacity-10 text-success border border-success px-2 py-1"><i class="fas fa-check-circle me-1"></i><%=Common.getBahasaConfig("Aktif")%></span>' : '<span class="badge bg-secondary bg-opacity-10 text-secondary border border-secondary px-2 py-1"><i class="fas fa-times-circle me-1"></i><%=Common.getBahasaConfig("Nonaktif")%></span>';

                    const safeKode = (row.kode || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    const safeNama = (row.nama || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    const safeKet = (row.keterangan || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    
                    // URL QR Code dari API Eksternal Terpercaya (Bisa di-scan)
                    const qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" + encodeURIComponent(row.kode);

                    htmlList += '<tr class="border-bottom">' +
                            '<td class="text-center text-muted fw-medium">' + (no++) + '</td>' +
                            '<td class="text-center"><img src="' + qrUrl + '" class="qr-preview-<%=rnd%> shadow-sm" alt="QR Code"></td>' +
                            '<td class="fw-bolder text-primary fs-6">' + (row.kode || '-') + '</td>' +
                            '<td class="fw-bold text-dark">' + (row.nama || '-') + '</td>' +
                            '<td class="text-muted small">' + (row.keterangan || '-') + '</td>' +
                            '<td class="text-center">' + badgeStatus + '</td>' +
                            '<td class="text-center text-nowrap">' +
                                '<button class="btn btn-sm btn-outline-warning text-dark shadow-sm px-2 me-1 mb-1 fw-bold" onclick="editMeja<%=rnd%>(' + row.id + ', \'' + safeKode + '\', \'' + safeNama + '\', \'' + safeKet + '\', ' + isAktif + ')" title="<%=Common.getBahasaConfig("Ubah Data")%>"><i class="fas fa-edit"></i></button>' +
                                '<button class="btn btn-sm btn-outline-info shadow-sm px-2 me-1 mb-1 fw-bold" onclick="cetakQRSingle<%=rnd%>(\'' + safeKode + '\', \'' + safeNama + '\')" title="<%=Common.getBahasaConfig("Cetak QR Code Ini")%>"><i class="fas fa-print"></i></button>' +
                                '<button class="btn btn-sm btn-outline-danger shadow-sm px-2 mb-1 fw-bold" onclick="hapusMeja<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Hapus Data")%>"><i class="fas fa-trash-alt"></i></button>' +
                            '</td>' +
                        '</tr>';
                });
            }
            
            tbody.innerHTML = htmlList;
            updatePagingUIMeja<%=rnd%>();

        } catch (error) {
            console.error("Error loading table:", error);
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3"></i><br><%=Common.getBahasaConfig("Gagal menarik data dari peladen.")%></td></tr>';
        }
    };

    const updatePagingUIMeja<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecordsMeja<%=rnd%> / limitPerPageMeja<%=rnd%>) || 1;
        document.getElementById('lblCurrentPageMeja<%=rnd%>').innerText = currentPageMeja<%=rnd%>;
        document.getElementById('lblTotalPageMeja<%=rnd%>').innerText = totalPages;
        
        const startIdx = totalRecordsMeja<%=rnd%> > 0 ? ((currentPageMeja<%=rnd%> - 1) * limitPerPageMeja<%=rnd%> + 1) : 0;
        const endIdx = Math.min(currentPageMeja<%=rnd%> * limitPerPageMeja<%=rnd%>, totalRecordsMeja<%=rnd%>);
        
        document.getElementById('pagingInfoMeja<%=rnd%>').innerHTML = '<%=Common.getBahasaConfigJS("Menampilkan")%> <span class="text-dark">' + startIdx + ' - ' + endIdx + '</span> <%=Common.getBahasaConfig("dari")%> <span class="text-dark">' + totalRecordsMeja<%=rnd%> + '</span> <%=Common.getBahasaConfig("meja")%>';
        
        document.getElementById('btnPrevPageMeja<%=rnd%>').disabled = (currentPageMeja<%=rnd%> === 1);
        document.getElementById('btnNextPageMeja<%=rnd%>').disabled = (currentPageMeja<%=rnd%> === totalPages);
    };

    // ==========================================
    // CREATE / UPDATE / DELETE & AUTO-GENERATE
    // ==========================================
    
    const generateKodeMejaOtomatis<%=rnd%> = () => {
        const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
        let result = 'TBL-';
        for (let i = 0; i < 5; i++) {
            result += chars.charAt(Math.floor(Math.random() * chars.length));
        }
        return result;
    };

    const toggleManualKodeMeja<%=rnd%> = () => {
        const isManual = document.getElementById('chkManualKodeMeja<%=rnd%>').checked;
        const inputKode = document.getElementById('inputKodeMeja<%=rnd%>');
        const idMeja = document.getElementById('inputIdMeja<%=rnd%>').value;

        if (isManual) {
            inputKode.readOnly = false;
            inputKode.classList.remove('bg-light');
            inputKode.focus();
        } else {
            inputKode.readOnly = true;
            inputKode.classList.add('bg-light');
            // Hanya perbarui kode otomatis jika sedang menambah data baru (idMeja kosong)
            if (idMeja === '') {
                inputKode.value = generateKodeMejaOtomatis<%=rnd%>();
            }
        }
    };

    const bukaFormMeja<%=rnd%> = () => {
        document.getElementById('formMeja<%=rnd%>').reset();
        document.getElementById('inputIdMeja<%=rnd%>').value = '';
        
        document.getElementById('inputAktifMeja<%=rnd%>').checked = true;
        document.getElementById('inputAktifMeja<%=rnd%>').dispatchEvent(new Event('change'));

        // Reset Input Kode & Set Auto-Generate
        document.getElementById('chkManualKodeMeja<%=rnd%>').checked = false;
        const inputKode = document.getElementById('inputKodeMeja<%=rnd%>');
        inputKode.value = generateKodeMejaOtomatis<%=rnd%>();
        inputKode.readOnly = true;
        inputKode.classList.add('bg-light');

        document.getElementById('formTitleMeja<%=rnd%>').innerHTML = '<i class="fas fa-plus-circle text-primary me-2"></i><%=Common.getBahasaConfig("Tambah Meja Baru")%>';
        switchViewMeja<%=rnd%>('viewFormMeja<%=rnd%>');
        setTimeout(() => { document.getElementById('inputNamaMeja<%=rnd%>').focus(); }, 200);
    };

    const tutupFormMeja<%=rnd%> = () => {
        switchViewMeja<%=rnd%>('viewListMeja<%=rnd%>');
    };

    const simpanDataMeja<%=rnd%> = async () => {
        const btnSimpan = document.getElementById('btnSimpanMeja<%=rnd%>');
        const oriBtn = btnSimpan.innerHTML;
        btnSimpan.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyimpan...")%>';
        btnSimpan.disabled = true;

        const idMeja = document.getElementById('inputIdMeja<%=rnd%>').value;
        const dataObj = {
            kode: document.getElementById('inputKodeMeja<%=rnd%>').value.trim().toUpperCase(),
            nama: document.getElementById('inputNamaMeja<%=rnd%>').value.trim(),
            keterangan: document.getElementById('inputKeteranganMeja<%=rnd%>').value.trim(),
            aktif: document.getElementById('inputAktifMeja<%=rnd%>').checked
        };

        const payload = { action: "simpanDataRinci", class: masterClassMeja<%=rnd%>, data: dataObj };
        if (idMeja !== '') payload.id = idMeja;

        try {
            const res = await fetchApiMeja<%=rnd%>(payload);
            if (res.status === '00' || res.status === 'success' || res.id) {
                showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Data meja berhasil disimpan.")%>', 'bg-success text-white');
                tutupFormMeja<%=rnd%>();
                loadDataMeja<%=rnd%>();
            } else {
                showToastUI<%=rnd%>(res.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan data. Pastikan Kode Meja unik.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi.")%>', 'bg-danger text-white');
        } finally {
            btnSimpan.innerHTML = oriBtn;
            btnSimpan.disabled = false;
        }
    };

    const editMeja<%=rnd%> = (id, kode, nama, ket, isAktif) => {
        document.getElementById('inputIdMeja<%=rnd%>').value = id;
        document.getElementById('inputKodeMeja<%=rnd%>').value = kode;
        document.getElementById('inputNamaMeja<%=rnd%>').value = nama;
        document.getElementById('inputKeteranganMeja<%=rnd%>').value = ket;

        const chkAktif = document.getElementById('inputAktifMeja<%=rnd%>');
        chkAktif.checked = isAktif;
        chkAktif.dispatchEvent(new Event('change'));

        // Set Input Kode menjadi read-only secara default saat mode edit
        document.getElementById('chkManualKodeMeja<%=rnd%>').checked = false;
        const inputKode = document.getElementById('inputKodeMeja<%=rnd%>');
        inputKode.readOnly = true;
        inputKode.classList.add('bg-light');

        document.getElementById('formTitleMeja<%=rnd%>').innerHTML = '<i class="fas fa-edit text-warning me-2"></i><%=Common.getBahasaConfig("Ubah Data Meja")%>';
        switchViewMeja<%=rnd%>('viewFormMeja<%=rnd%>');
    };

    const hapusMeja<%=rnd%> = async (id) => {
        if(confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus data meja ini?")%>')) {
            const payload = { action: "deleteData", class: masterClassMeja<%=rnd%>, id: id };
            const res = await fetchApiMeja<%=rnd%>(payload);
            if (res.status === '00' || res.status === 'success') {
                showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Data berhasil dihapus.")%>', 'bg-success text-white');
                if (document.querySelectorAll('#tabelDataMeja<%=rnd%> tr').length === 1 && currentPageMeja<%=rnd%> > 1) currentPageMeja<%=rnd%>--;
                loadDataMeja<%=rnd%>();
            } else {
                showToastUI<%=rnd%>(res.description || '<%=Common.getBahasaConfigJS("Data gagal dihapus karena terhubung dengan transaksi lain.")%>', 'bg-danger text-white');
            }
        }
    };

    // ==========================================
    // EXCEL UPLOAD & DOWNLOAD
    // ==========================================
    const downloadExcelMeja<%=rnd%> = async () => {
        showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Menyiapkan file Excel...")%>', 'bg-info text-dark');
        
        const sqlFilters = getFilterQueryMeja<%=rnd%>();
        const sqlQuery = "SELECT kode, nama, keterangan, aktif FROM koperasi.meja_kantin " + sqlFilters + " ORDER BY kode ASC;";

        try {
            const result = await fetchApiMeja<%=rnd%>({ action: "sql", sql: sqlQuery });
            if (!result.data || result.data.length === 0) {
                showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Tidak ada data untuk diunduh.")%>', 'bg-warning text-dark');
                return;
            }

            let tableHtml = '<html xmlns:x="urn:schemas-microsoft-com:office:excel">';
            tableHtml += '<head><meta charset="UTF-8"><style>th { background-color: #3b82f6; color: white; font-weight: bold; border: 1px solid black; padding: 5px; } td { border: 1px solid black; padding: 5px; }</style></head><body>';
            tableHtml += '<table><thead><tr><th>KODE_MEJA</th><th>NAMA_MEJA</th><th>KETERANGAN</th><th>AKTIF</th></tr></thead><tbody>';

            result.data.forEach(r => {
                tableHtml += '<tr>';
                tableHtml += '<td>' + (r.kode || '') + '</td><td>' + (r.nama || '') + '</td><td>' + (r.keterangan || '') + '</td>';
                tableHtml += '<td>' + (r.aktif === true || r.aktif === 't' ? 'TRUE' : 'FALSE') + '</td>';
                tableHtml += '</tr>';
            });
            tableHtml += '</tbody></table></body></html>';

            const blob = new Blob([tableHtml], { type: 'application/vnd.ms-excel' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement("a");
            link.download = "Format_MejaKantin_" + new Date().getTime() + ".xls";
            link.href = url;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            setTimeout(() => URL.revokeObjectURL(url), 100);

        } catch (error) {
            showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal mengunduh data.")%>', 'bg-danger text-white');
        }
    };

    const showUploadModalMeja<%=rnd%> = () => {
        document.getElementById('fileExcelMeja<%=rnd%>').value = '';
        document.getElementById('namaFileExcelTerpilih<%=rnd%>').style.display = 'none';
        document.getElementById('uploadStatusBoxMeja<%=rnd%>').style.display = 'none';
        
        if(!modalUploadMejaInstance<%=rnd%>) {
            modalUploadMejaInstance<%=rnd%> = new bootstrap.Modal(document.getElementById('modalUploadExcelMeja<%=rnd%>'));
        }
        modalUploadMejaInstance<%=rnd%>.show();
    };

    const tampilkanNamaFileExcel<%=rnd%> = (input) => {
        const lbl = document.getElementById('namaFileExcelTerpilih<%=rnd%>');
        if (input.files && input.files[0]) {
            lbl.innerHTML = '<i class="fas fa-check-circle me-1"></i> ' + input.files[0].name;
            lbl.style.display = 'block';
        } else {
            lbl.style.display = 'none';
        }
    };

    const prosesUploadExcelMeja<%=rnd%> = () => {
        const fileInput = document.getElementById('fileExcelMeja<%=rnd%>');
        const file = fileInput.files[0];
        if (!file) return showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Pilih file Excel terlebih dahulu!")%>', 'bg-warning text-dark');
        
        const btnProcess = document.getElementById('btnProsesExcelMeja<%=rnd%>');
        const statusBox = document.getElementById('uploadStatusBoxMeja<%=rnd%>');
        
        btnProcess.disabled = true;
        btnProcess.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Membaca File...")%>';
        statusBox.style.display = 'block';
        statusBox.className = 'alert alert-info py-2 small fw-bold';
        statusBox.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Mengekstrak data baris demi baris...';

        const reader = new FileReader();
        reader.onload = async (e) => {
            try {
                const data = new Uint8Array(e.target.result);
                const workbook = XLSX.read(data, {type: 'array'});
                const excelRows = XLSX.utils.sheet_to_json(workbook.Sheets[workbook.SheetNames[0]], {defval: ""});
                
                if (excelRows.length === 0) throw new Error("File Excel Kosong.");

                statusBox.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Mengunggah ' + excelRows.length + ' baris data ke server...';

                const batchData = excelRows.map(row => {
                    const obj = {
                        kode: row['KODE_MEJA'] ? row['KODE_MEJA'].toString().trim().toUpperCase() : "",
                        nama: row['NAMA_MEJA'] || "",
                        keterangan: row['KETERANGAN'] || "",
                        aktif: (row['AKTIF'] && row['AKTIF'].toString().toUpperCase() === 'TRUE')
                    };
                    return obj;
                });

                // Filter out empty rows
                const validBatch = batchData.filter(d => d.kode !== "");

                const payload = { action: "simpanBatchDataRinci", class: masterClassMeja<%=rnd%>, dataBatch: validBatch };
                const res = await fetchApiMeja<%=rnd%>(payload);

                if (res.status === '00' || res.status === 'success') {
                    statusBox.className = 'alert alert-success py-2 small fw-bold';
                    statusBox.innerHTML = '<i class="fas fa-check-circle me-2"></i>Data berhasil diunggah!';
                    setTimeout(() => { modalUploadMejaInstance<%=rnd%>.hide(); loadDataMeja<%=rnd%>(); }, 1500);
                } else throw new Error(res.description || "Gagal menyimpan di sisi server. Pastikan tidak ada duplikasi kode.");
            } catch (error) {
                statusBox.className = 'alert alert-danger py-2 small fw-bold';
                statusBox.innerHTML = '<i class="fas fa-exclamation-triangle me-2"></i>Kesalahan: ' + error.message;
            } finally {
                btnProcess.disabled = false;
                btnProcess.innerHTML = '<i class="fas fa-cogs me-2"></i><%=Common.getBahasaConfig("Proses Data")%>';
            }
        };
        reader.readAsArrayBuffer(file);
    };

    // ==========================================
    // LOGIKA CETAK QR CODE
    // ==========================================
    const renderHTMLQRPrint = (itemsArray) => {
        let html = '<!DOCTYPE html><html><head><title><%=Common.getBahasaConfig("Cetak QR Code Meja")%></title>';
        html += '<style>';
        html += 'body { font-family: Arial, sans-serif; margin: 0; padding: 20px; text-align: center; }';
        html += '.grid-container { display: flex; flex-wrap: wrap; justify-content: center; gap: 20px; }';
        html += '.qr-card { border: 2px dashed #000; border-radius: 15px; padding: 20px; width: 220px; text-align: center; page-break-inside: avoid; margin-bottom: 20px; }';
        html += '.qr-img { width: 180px; height: 180px; margin-bottom: 10px; }';
        html += '.qr-code-txt { font-size: 24px; font-weight: bold; margin: 0; color: #000; }';
        html += '.qr-name-txt { font-size: 14px; margin: 5px 0 0 0; color: #555; }';
        html += '</style></head><body>';
        html += '<h2 style="margin-bottom:30px;"><%=Common.getBahasaConfig("Koleksi QR Code Meja Kantin")%></h2>';
        html += '<div class="grid-container">';
        
        itemsArray.forEach(item => {
            const qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" + encodeURIComponent(item.kode);
            html += '<div class="qr-card">';
            html += '<img src="' + qrUrl + '" class="qr-img" alt="QR">';
            html += '<p class="qr-code-txt">' + item.kode + '</p>';
            if(item.nama) html += '<p class="qr-name-txt">' + item.nama + '</p>';
            html += '</div>';
        });

        html += '</div></body></html>';
        return html;
    };

    const cetakQRSingle<%=rnd%> = (kode, nama) => {
        const item = [{ kode: kode, nama: nama }];
        const htmlToPrint = renderHTMLQRPrint(item);
        
        const printWindow = window.open('', '', 'height=800,width=800');
        printWindow.document.write(htmlToPrint);
        printWindow.document.close();
        printWindow.focus();
        // Beri waktu sejenak agar gambar QR ter-load dari API
        setTimeout(() => { printWindow.print(); }, 800);
    };

    const cetakQRMassal<%=rnd%> = async () => {
        const btnCetak = document.querySelector('button[onclick="cetakQRMassal<%=rnd%>()"]');
        const oriBtn = btnCetak.innerHTML;
        btnCetak.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i><%=Common.getBahasaConfig("Menyiapkan...")%>';
        btnCetak.disabled = true;

        const sqlFilters = getFilterQueryMeja<%=rnd%>();
        // Batasi 100 meja sekali cetak agar tidak nge-hang browser
        const sqlQuery = "SELECT kode, nama FROM koperasi.meja_kantin " + sqlFilters + " ORDER BY kode ASC LIMIT 100;";

        try {
            const result = await fetchApiMeja<%=rnd%>({ action: "sql", sql: sqlQuery });
            if (!result.data || result.data.length === 0) {
                showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Tidak ada data meja untuk dicetak.")%>', 'bg-warning text-dark');
                return;
            }

            const htmlToPrint = renderHTMLQRPrint(result.data);
            const printWindow = window.open('', '', 'height=800,width=800');
            printWindow.document.write(htmlToPrint);
            printWindow.document.close();
            printWindow.focus();
            
            // Jeda lebih lama untuk massal agar gambar termuat sempurna
            setTimeout(() => { printWindow.print(); }, 1500);

        } catch (error) {
            showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal menyiapkan data cetak massal.")%>', 'bg-danger text-white');
        } finally {
            btnCetak.innerHTML = oriBtn;
            btnCetak.disabled = false;
        }
    };

    // ==========================================
    // INISIALISASI HALAMAN
    // ==========================================
    document.addEventListener("DOMContentLoaded", () => {
        const filterInputs = document.querySelectorAll('.filter-meja-<%=rnd%>');
        filterInputs.forEach(input => {
            input.addEventListener("keydown", (event) => {
                if (event.key === "Enter") {
                    event.preventDefault(); 
                    resetAndLoadMeja<%=rnd%>();
                }
            });
        });
        loadDataMeja<%=rnd%>();
    });

</script>