<%@page import="java.net.URLEncoder"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// 1. SECURITY CHECK & ROLE IDENTIFICATION
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	out.print("{\"status\":\"error\", \"message\":\""
	+ Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
	return;
}
Pedagang pedagangLogin = tbmuser.getPedagang();
Toko tokoLogin = pedagangLogin == null ? null : pedagangLogin.getToko();
String rnd = Common.getGeneratedBarCode(7);

// Aturan: Admin bisa semua toko/general. Pedagang HANYA tokonya sendiri.
boolean isAdmin = (tokoLogin == null);
String idTokoAktif = tokoLogin != null ? String.valueOf(tokoLogin.getId()) : "";
String namaTokoAktif = tokoLogin != null ? tokoLogin.getNama() : "";
%>

<script data-cfasync="false" src="https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js"></script>

<div id="viewList<%=rnd%>" class="animate__animated animate__fadeInUp">
    <div class="row mb-4">
        <div class="col-12">
            <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
                
                <div class="card-header bg-white border-0 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-3 border-bottom border-light">
                    <div>
                        <h5 class="fw-bold text-dark mb-1">
                            <i class="fas fa-tags text-primary me-2"></i>
                            <% if (isAdmin) { %>
                                <%=Common.getBahasaConfig("Aturan Diskon & Promo (Semua Toko)")%>
                            <% } else { %>
                                <%=Common.getBahasaConfig("Aturan Diskon - ")%> <span class="text-primary"><%=namaTokoAktif%></span>
                            <% } %>
                        </h5>
                        <small class="text-muted"><%=Common.getBahasaConfig("Kelola promo potongan harga langsung atau saldo diskon member.")%></small>
                    </div>
                    
                    <div class="d-flex align-items-center gap-2 flex-wrap">
                    	<% if (isAdmin) { %>
                        <button class="btn btn-warning rounded-pill px-3 shadow-sm fw-bold text-dark" onclick="showUploadModal<%=rnd%>()" title="<%=Common.getBahasaConfig("Unggah file Excel")%>">
                            <i class="fas fa-file-upload me-1"></i><%=Common.getBahasaConfig("Unggah")%>
                        </button>
                        <% } %>
                        <button class="btn btn-success rounded-pill px-3 shadow-sm fw-bold" onclick="downloadExcelDiskon<%=rnd%>()" title="<%=Common.getBahasaConfig("Unduh file Excel")%>">
                            <i class="fas fa-file-download me-1"></i><%=Common.getBahasaConfig("Unduh")%>
                        </button>
                        <button class="btn btn-primary rounded-pill px-4 shadow-sm fw-bold text-nowrap" onclick="bukaFormTambah<%=rnd%>()">
                            <i class="fas fa-plus-circle me-1"></i><%=Common.getBahasaConfig("Tambah Aturan")%>
                        </button>
                    </div>
                </div>
                
                <div class="card-body p-4 pt-3">
                    
                    <div class="bg-light p-3 rounded-3 border shadow-sm mb-4">
                        <div class="row g-2 align-items-end">
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Pencarian")%></label>
                                <input type="text" class="form-control form-control-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterKeyword<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Nama aturan atau nama produk...")%>">
                            </div>
                            
                            <% if (isAdmin) { %>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Filter Toko")%></label>
                                <select class="form-select form-select-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterToko<%=rnd%>">
                                    <option value=""><%=Common.getBahasaConfig("- Semua Toko -")%></option>
                                </select>
                            </div>
                            <% } else { %>
                                <input type="hidden" id="filterToko<%=rnd%>" value="<%=idTokoAktif%>">
                            <% } %>

                            <div class="col-md-auto ms-auto">
                                <button class="btn btn-sm btn-secondary fw-bold px-3 shadow-sm" onclick="triggerSearchDiskon<%=rnd%>()">
                                    <i class="fas fa-search me-1"></i> <%=Common.getBahasaConfig("Saring")%>
                                </button>
                            </div>
                        </div>
                    </div>

                    <div class="table-responsive border rounded-3 shadow-sm bg-white">
                        <table class="table table-hover table-striped align-middle mb-0">
                            <thead class="table-dark text-center align-middle">
                                <tr>
                                    <th class="fw-semibold text-uppercase small px-3 py-3" style="width: 50px;">#</th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Aturan Promo")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Target Produk")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Target Member")%></th>
                                    <th class="text-end fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Nilai Diskon")%></th>
                                    <th class="text-center fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Limit 1x/Hari/Toko")%></th>
                                    <th class="text-center fw-semibold text-uppercase small px-3"><%=Common.getBahasaConfig("Masa Berlaku")%></th>
                                    <th class="fw-semibold text-uppercase small px-3 text-center"><%=Common.getBahasaConfig("Status")%></th>
                                    <th class="fw-semibold text-uppercase small px-3 text-center" style="width: 140px;"><i class="fas fa-cogs"></i></th>
                                </tr>
                            </thead>
                            <tbody id="tabelDataDiskon<%=rnd%>">
                                <tr><td colspan="9" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Memuat data...")%></span></td></tr>
                            </tbody>
                        </table>
                    </div>

                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mt-4">
                        <div class="small fw-medium text-muted bg-light px-3 py-1 rounded-pill border" id="pagingInfoDiskon<%=rnd%>"></div>
                        <nav>
                            <ul class="pagination pagination-sm mb-0 shadow-sm">
                                <li class="page-item"><button class="page-link text-primary rounded-start-pill px-3 fw-bold" id="btnPrevPage<%=rnd%>" onclick="changePageDiskon<%=rnd%>(-1)"><i class="fas fa-chevron-left"></i></button></li>
                                <li class="page-item disabled"><span class="page-link bg-light text-dark px-3 fw-bold" id="pageNumberDisplay<%=rnd%>">1</span></li>
                                <li class="page-item"><button class="page-link text-primary rounded-end-pill px-3 fw-bold" id="btnNextPage<%=rnd%>" onclick="changePageDiskon<%=rnd%>(1)"><i class="fas fa-chevron-right"></i></button></li>
                            </ul>
                        </nav>
                    </div>

                </div>
            </div>
        </div>
    </div>
</div>

<div id="viewForm<%=rnd%>" class="animate__animated animate__fadeIn" style="display: none;">
    <div class="row justify-content-center">
        <div class="col-lg-10">
            <div class="card border-0 shadow-sm rounded-4 border-top border-success border-4">
                <div class="card-header bg-white border-0 pt-4 pb-0 px-4">
                    <div class="d-flex justify-content-between align-items-center">
                        <h5 class="fw-bold text-dark mb-0" id="formTitle<%=rnd%>">
                            <i class="fas fa-tags text-success me-2"></i><%=Common.getBahasaConfig("Formulir Aturan Diskon")%>
                        </h5>
                        <div class="form-check form-switch fs-5 mb-0">
                            <input class="form-check-input" type="checkbox" id="inputAktif<%=rnd%>" checked style="cursor: pointer;">
                            <label class="form-check-label fs-6 fw-bold text-success" for="inputAktif<%=rnd%>" id="labelAktif<%=rnd%>"><%=Common.getBahasaConfig("Aktif")%></label>
                        </div>
                    </div>
                </div>
                <div class="card-body p-4">
                    <form id="formDiskon<%=rnd%>" onsubmit="event.preventDefault(); saveDiskon<%=rnd%>();">
                        <input type="hidden" id="inputIdDiskon<%=rnd%>" value="">
                        
                        <div class="row g-4 mt-1">
                            <div class="col-md-12">
                                <h6 class="fw-bold text-primary border-bottom pb-2"><i class="fas fa-info-circle me-2"></i>Informasi Umum</h6>
                            </div>
                            
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Aturan Promo")%> <span class="text-danger">*</span></label>
                                <input type="text" class="form-control fw-bold" id="inputNamaAturan<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: Promo Merdeka 17an")%>" required>
                            </div>
                            
                            <% if (isAdmin) { %>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Berlaku di Toko")%></label>
                                <select class="form-select fw-semibold" id="inputTokoForm<%=rnd%>" onchange="loadComboProduk<%=rnd%>()">
                                    <option value=""><%=Common.getBahasaConfig("-- Berlaku General (Semua Toko) --")%></option>
                                </select>
                            </div>
                            <% } else { %>
                                <input type="hidden" id="inputTokoForm<%=rnd%>" value="<%=idTokoAktif%>">
                            <% } %>
                            
                            <div class="col-md-12">
                                <div class="d-flex justify-content-between align-items-center mb-1">
                                    <label class="form-label small fw-bold text-secondary mb-0"><%=Common.getBahasaConfig("Target Produk")%> <span class="text-danger">*</span></label>
                                    <div class="form-check form-switch fs-6 mb-0">
                                        <input class="form-check-input ms-0 me-2" type="checkbox" id="inputSemuaProduk<%=rnd%>" onchange="toggleProdukTarget<%=rnd%>()">
                                        <label class="form-check-label fw-bold text-primary small" for="inputSemuaProduk<%=rnd%>"><%=Common.getBahasaConfig("Berlaku Untuk Semua Produk")%></label>
                                    </div>
                                </div>
                                <div class="input-group shadow-sm" id="wrapperCariProduk<%=rnd%>">
                                    <span class="input-group-text bg-white border-end-0"><i class="fas fa-box text-primary"></i></span>
                                    <input type="text" class="form-control border-start-0 fw-bold" list="listProduk<%=rnd%>" id="inputCariProduk<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik nama produk spesifik...")%>" autocomplete="off" required>
                                </div>
                                <datalist id="listProduk<%=rnd%>"></datalist>
                                <input type="hidden" id="idProdukSelected<%=rnd%>" value="">
                            </div>

                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal & Waktu Mulai")%></label>
                                <input type="datetime-local" class="form-control" id="inputTglMulai<%=rnd%>">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal & Waktu Berakhir")%></label>
                                <input type="datetime-local" class="form-control" id="inputTglSelesai<%=rnd%>">
                            </div>

                            <div class="col-md-12">
                                <label class="form-label small fw-bold text-secondary d-block"><%=Common.getBahasaConfig("Pilih Hari (kosongkan = berlaku semua hari)")%></label>
                                <div class="d-flex flex-wrap gap-2 align-items-center">
                                    <button type="button" class="btn btn-sm btn-outline-secondary" onclick="pilihSemuaHariDiskon<%=rnd%>()"><%=Common.getBahasaConfig("Pilih Semua Hari")%></button>
                                    <div class="form-check form-check-inline bg-light px-2 py-1 rounded-pill border m-0">
                                        <input class="form-check-input" type="checkbox" value="1" id="inputHari1_<%=rnd%>"><label class="form-check-label small" for="inputHari1_<%=rnd%>"><%=Common.getBahasaConfig("Senin")%></label>
                                    </div>
                                    <div class="form-check form-check-inline bg-light px-2 py-1 rounded-pill border m-0">
                                        <input class="form-check-input" type="checkbox" value="2" id="inputHari2_<%=rnd%>"><label class="form-check-label small" for="inputHari2_<%=rnd%>"><%=Common.getBahasaConfig("Selasa")%></label>
                                    </div>
                                    <div class="form-check form-check-inline bg-light px-2 py-1 rounded-pill border m-0">
                                        <input class="form-check-input" type="checkbox" value="3" id="inputHari3_<%=rnd%>"><label class="form-check-label small" for="inputHari3_<%=rnd%>"><%=Common.getBahasaConfig("Rabu")%></label>
                                    </div>
                                    <div class="form-check form-check-inline bg-light px-2 py-1 rounded-pill border m-0">
                                        <input class="form-check-input" type="checkbox" value="4" id="inputHari4_<%=rnd%>"><label class="form-check-label small" for="inputHari4_<%=rnd%>"><%=Common.getBahasaConfig("Kamis")%></label>
                                    </div>
                                    <div class="form-check form-check-inline bg-light px-2 py-1 rounded-pill border m-0">
                                        <input class="form-check-input" type="checkbox" value="5" id="inputHari5_<%=rnd%>"><label class="form-check-label small" for="inputHari5_<%=rnd%>"><%=Common.getBahasaConfig("Jumat")%></label>
                                    </div>
                                    <div class="form-check form-check-inline bg-light px-2 py-1 rounded-pill border m-0">
                                        <input class="form-check-input" type="checkbox" value="6" id="inputHari6_<%=rnd%>"><label class="form-check-label small" for="inputHari6_<%=rnd%>"><%=Common.getBahasaConfig("Sabtu")%></label>
                                    </div>
                                    <div class="form-check form-check-inline bg-light px-2 py-1 rounded-pill border m-0">
                                        <input class="form-check-input" type="checkbox" value="7" id="inputHari7_<%=rnd%>"><label class="form-check-label small" for="inputHari7_<%=rnd%>"><%=Common.getBahasaConfig("Minggu")%></label>
                                    </div>
                                </div>
                            </div>

                            <div class="col-md-12 mt-4">
                                <h6 class="fw-bold text-primary border-bottom pb-2"><i class="fas fa-users me-2"></i>Target Member & Batasan</h6>
                            </div>

                            <div class="col-md-6">
                                <div class="form-check form-switch fs-6 bg-light p-3 rounded-3 border h-100">
                                    <input class="form-check-input ms-0 me-2" type="checkbox" id="inputBerlakuSemua<%=rnd%>" checked onchange="toggleMemberTarget<%=rnd%>()">
                                    <label class="form-check-label fw-bold text-dark" for="inputBerlakuSemua<%=rnd%>"><%=Common.getBahasaConfig("Berlaku Untuk Semua Member (Publik)")%></label>
                                </div>
                            </div>
                            
                            <div class="col-md-6">
                                <div class="form-check form-switch fs-6 bg-light p-3 rounded-3 border h-100">
                                    <input class="form-check-input ms-0 me-2" type="checkbox" id="inputBerlaku1xHari<%=rnd%>">
                                    <label class="form-check-label fw-bold text-dark" for="inputBerlaku1xHari<%=rnd%>"><%=Common.getBahasaConfig("Batas Penggunaan 1x Per Hari Per Toko")%></label>
                                </div>
                            </div>

                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jenis Anggota (Spesifik)")%></label>
                                <select class="form-select bg-light" id="inputJenisAnggota<%=rnd%>" disabled>
                                    <option value=""><%=Common.getBahasaConfig("-- Semua Jenis --")%></option>
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tipe Anggota (Spesifik)")%></label>
                                <select class="form-select bg-light" id="inputTipeAnggota<%=rnd%>" disabled>
                                    <option value=""><%=Common.getBahasaConfig("-- Semua Tipe --")%></option>
                                </select>
                            </div>

                            <div class="col-md-12 mt-4">
                                <h6 class="fw-bold text-primary border-bottom pb-2"><i class="fas fa-percent me-2"></i>Nilai & Logika Diskon</h6>
                            </div>

                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Diskon Persentase (%)")%></label>
                                <input type="number" step="0.01" class="form-control" id="inputPersentase<%=rnd%>" placeholder="Contoh: 10">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Maksimal Potongan (Rp)")%></label>
                                <input type="number" class="form-control" id="inputMaksPotongan<%=rnd%>" placeholder="Contoh: 15000">
                                <small class="text-muted" style="font-size: 10px;">*Jika pakai persentase</small>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Atau Diskon Nominal Fix (Rp)")%></label>
                                <input type="number" class="form-control" id="inputNominal<%=rnd%>" placeholder="Contoh: 5000">
                            </div>

                            <div class="col-md-12">
                                <div class="bg-primary bg-opacity-10 p-3 rounded-3 border border-primary border-opacity-25 mt-2">
                                    <label class="form-label small fw-bold text-dark mb-2"><%=Common.getBahasaConfig("Logika Eksekusi Kasir (Pilih salah satu)")%> <span class="text-danger">*</span></label>
                                    <div class="d-flex gap-4">
                                        <div class="form-check">
                                            <input class="form-check-input" type="radio" name="radioPotongan<%=rnd%>" id="radioPotongStruk<%=rnd%>" value="true" checked>
                                            <label class="form-check-label fw-semibold" for="radioPotongStruk<%=rnd%>"><i class="fas fa-cut text-danger me-1"></i> Potong Langsung (Harga Lebih Murah)</label>
                                        </div>
                                        <div class="form-check">
                                            <input class="form-check-input" type="radio" name="radioPotongan<%=rnd%>" id="radioCashback<%=rnd%>" value="false">
                                            <label class="form-check-label fw-semibold" for="radioCashback<%=rnd%>"><i class="fas fa-wallet text-success me-1"></i> Simpan Jadi Saldo Diskon (Cashback)</label>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div class="col-12 mt-3">
                                <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Keterangan Syarat & Ketentuan")%></label>
                                <textarea class="form-control" id="inputKeterangan<%=rnd%>" rows="2" placeholder="<%=Common.getBahasaConfig("Contoh: Hanya berlaku untuk makan di tempat...")%>"></textarea>
                            </div>

                            <div class="col-12 mt-4 text-end border-top pt-3">
                                <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold border shadow-sm" onclick="tutupForm<%=rnd%>()"><i class="fas fa-times text-danger me-2"></i><%=Common.getBahasaConfig("Batal")%></button>
                                <button type="submit" class="btn btn-success px-4 rounded-pill shadow-sm fw-bold" id="btnSimpan<%=rnd%>">
                                    <i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Aturan")%>
                                </button>
                            </div>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="modalUploadExcel<%=rnd%>" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 rounded-4 shadow-lg">
            <div class="modal-header bg-light border-bottom-0 pb-3">
                <h5 class="modal-title fw-bold text-dark">
                    <i class="fas fa-file-upload text-warning me-2"></i><%=Common.getBahasaConfig("Unggah Data Aturan Diskon")%>
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4 pt-2 text-center">
                <p class="text-muted small mb-4"><%=Common.getBahasaConfig("Format file Excel harus sama persis dengan format unduhan.")%></p>
                <input class="form-control mb-3" type="file" id="fileUploadExcel<%=rnd%>" accept=".xls,.xlsx">
                <div id="uploadStatusBox<%=rnd%>" class="alert alert-info py-2" style="display: none;"></div>
                <div class="mt-4">
                    <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
                    <button type="button" class="btn btn-warning text-dark px-4 rounded-pill fw-bold shadow-sm" id="btnProsesUpload<%=rnd%>" onclick="prosesUploadExcel<%=rnd%>()">
                        <i class="fas fa-cogs me-2"></i><%=Common.getBahasaConfig("Proses Data")%>
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    // ==========================================
    // BAGIAN 1: UTILITIES & STATE GLOBAL
    // ==========================================
    const masterModelClass<%=rnd%> = "ais.database.model.koperasi.AturanDiskon";
    const isAdmin<%=rnd%> = <%=isAdmin%>;
    const idTokoLogin<%=rnd%> = '<%=idTokoAktif%>';
    const idUserLogin<%=rnd%> = '<%=tbmuser.getUserId()%>';
    let modalInstanceUpload<%=rnd%> = null;

    let arrDataProduk<%=rnd%> = [];
    let currentPage<%=rnd%> = 1;
    const limitPerPage<%=rnd%> = 10;
    let totalRecords<%=rnd%> = 0;

    const fetchData<%=rnd%> = async (sql) => {
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

    // Label UI Switch Status Aktif
    const uiStatusAktif<%=rnd%> = document.getElementById('inputAktif<%=rnd%>');
    if (uiStatusAktif<%=rnd%>) {
        uiStatusAktif<%=rnd%>.addEventListener('change', function() {
            const lbl = document.getElementById('labelAktif<%=rnd%>');
            if(this.checked) {
                lbl.innerText = '<%=Common.getBahasaConfigJS("Aktif")%>';
                lbl.className = 'form-check-label fs-6 fw-bold text-success';
            } else {
                lbl.innerText = '<%=Common.getBahasaConfigJS("Tidak Aktif")%>';
                lbl.className = 'form-check-label fs-6 fw-bold text-danger';
            }
        });
    }

    // Toggle Jenis & Tipe Member
    const toggleMemberTarget<%=rnd%> = () => {
        const isAll = document.getElementById('inputBerlakuSemua<%=rnd%>').checked;
        const cbJenis = document.getElementById('inputJenisAnggota<%=rnd%>');
        const cbTipe = document.getElementById('inputTipeAnggota<%=rnd%>');
        
        if (isAll) {
            cbJenis.disabled = true;
            cbTipe.disabled = true;
            cbJenis.value = "";
            cbTipe.value = "";
            cbJenis.classList.add("bg-light");
            cbTipe.classList.add("bg-light");
        } else {
            cbJenis.disabled = false;
            cbTipe.disabled = false;
            cbJenis.classList.remove("bg-light");
            cbTipe.classList.remove("bg-light");
        }
    };

    // Toggle Produk Target (Semua vs Spesifik)
    const toggleProdukTarget<%=rnd%> = () => {
        const isAllProduk = document.getElementById('inputSemuaProduk<%=rnd%>').checked;
        const inputCari = document.getElementById('inputCariProduk<%=rnd%>');
        const idTersembunyi = document.getElementById('idProdukSelected<%=rnd%>');
        
        if (isAllProduk) {
            inputCari.disabled = true;
            inputCari.required = false;
            inputCari.value = "";
            idTersembunyi.value = "";
            inputCari.classList.add("bg-light");
            inputCari.placeholder = "<%=Common.getBahasaConfig("Berlaku untuk semua produk...")%>";
        } else {
            inputCari.disabled = false;
            inputCari.required = true;
            inputCari.classList.remove("bg-light");
            inputCari.placeholder = "<%=Common.getBahasaConfig("Ketik nama produk spesifik...")%>";
        }
    };

    // ==========================================
    // INIT COMBO BOX (TOKO, JENIS, TIPE, PRODUK)
    // ==========================================
    const loadMasterDataCombo<%=rnd%> = async () => {
        // Combo Toko untuk Filter dan Form (Jika Admin)
        if (isAdmin<%=rnd%>) {
            const sqlToko = "SELECT id, nama FROM koperasi.toko WHERE aktif = true ORDER BY nama ASC";
            const resToko = await fetchData<%=rnd%>(sqlToko);
            
            let optFilterToko = '<option value=""><%=Common.getBahasaConfig("- Semua Toko -")%></option>';
            let optFormToko = '<option value=""><%=Common.getBahasaConfig("-- Berlaku General (Semua Toko) --")%></option>';
            
            resToko.forEach(item => {
                const opt = '<option value="' + item.id + '">' + item.nama + '</option>';
                optFilterToko += opt;
                optFormToko += opt;
            });
            
            document.getElementById('filterToko<%=rnd%>').innerHTML = optFilterToko;
            const formToko = document.getElementById('inputTokoForm<%=rnd%>');
            if (formToko) formToko.innerHTML = optFormToko;
        }

        // Combo Jenis Anggota
        const sqlJenis = "SELECT id, nama FROM koperasi.jenis_anggota_koperasi WHERE aktif = true ORDER BY nama ASC";
        fetchData<%=rnd%>(sqlJenis).then(res => {
            let optHtml = '<option value=""><%=Common.getBahasaConfig("-- Semua Jenis --")%></option>';
            res.forEach(item => { optHtml += '<option value="' + item.id + '">' + item.nama + '</option>'; });
            document.getElementById('inputJenisAnggota<%=rnd%>').innerHTML = optHtml;
        });

        // Combo Tipe Anggota
        const sqlTipe = "SELECT id, nama FROM koperasi.tipe_anggota_koperasi WHERE aktif = true ORDER BY nama ASC";
        fetchData<%=rnd%>(sqlTipe).then(res => {
            let optHtml = '<option value=""><%=Common.getBahasaConfig("-- Semua Tipe --")%></option>';
            res.forEach(item => { optHtml += '<option value="' + item.id + '">' + item.nama + '</option>'; });
            document.getElementById('inputTipeAnggota<%=rnd%>').innerHTML = optHtml;
        });
    };

    // Auto-select Produk Datalist
    document.getElementById('inputCariProduk<%=rnd%>').addEventListener('change', function() {
        const val = this.value;
        const match = arrDataProduk<%=rnd%>.find(p => p.nama_lengkap === val);
        document.getElementById('idProdukSelected<%=rnd%>').value = match ? match.id : "";
    });

    const loadComboProduk<%=rnd%> = async (selectedProdukId = null, selectedProdukName = null) => {
        const tokoId = isAdmin<%=rnd%> ? document.getElementById('inputTokoForm<%=rnd%>').value : idTokoLogin<%=rnd%>;
        const inputProduk = document.getElementById('inputCariProduk<%=rnd%>');
        const dtListProduk = document.getElementById('listProduk<%=rnd%>');
        
        inputProduk.placeholder = '<%=Common.getBahasaConfigJS("Memuat data produk...")%>';
        inputProduk.disabled = true;

        // Jika toko kosong (General), tarik semua produk. Jika ada toko, tarik khusus toko tsb.
        let sqlProduk = "SELECT id, nama, kode FROM koperasi.produk WHERE aktif = true ";
        if (tokoId !== "") {
            sqlProduk += " AND toko = " + tokoId;
        }
        sqlProduk += " ORDER BY nama ASC";

        const resProduk = await fetchData<%=rnd%>(sqlProduk);
        
        let listProdukHtml = '';
        arrDataProduk<%=rnd%> = []; 
        
        resProduk.forEach(item => {
            const kode = item.kode ? ' [' + item.kode + ']' : "";
            const namaLengkap = item.nama + kode;
            
            arrDataProduk<%=rnd%>.push({
                id: item.id,
                nama_lengkap: namaLengkap
            });
            listProdukHtml += '<option value="' + namaLengkap + '" data-id="' + item.id + '">';
        });
        
        dtListProduk.innerHTML = listProdukHtml;
        
        // Kembalikan state input sesuai checkbox "semua produk"
        toggleProdukTarget<%=rnd%>();

        if (selectedProdukId) {
            document.getElementById('idProdukSelected<%=rnd%>').value = selectedProdukId;
            inputProduk.value = selectedProdukName || "";
        } else {
            inputProduk.value = "";
            document.getElementById('idProdukSelected<%=rnd%>').value = "";
        }
    };


    // ==========================================
    // READ (TAMPILKAN TABEL)
    // ==========================================
    const buildFilterSQL<%=rnd%> = () => {
        const keyword = document.getElementById('filterKeyword<%=rnd%>').value.trim().replace(/'/g, "''");
        const elToko = document.getElementById('filterToko<%=rnd%>');
        const toko = elToko ? elToko.value : "";

        let filters = " WHERE 1=1 ";
        
        if (keyword !== '') {
            filters += " AND (a.nama_aturan ILIKE '%" + keyword + "%' OR p.nama ILIKE '%" + keyword + "%' OR a.keterangan ILIKE '%" + keyword + "%') ";
        }

        if (!isAdmin<%=rnd%>) {
            // Pedagang hanya melihat diskon tokonya dan diskon general (toko is null)
            filters += " AND (a.toko = " + idTokoLogin<%=rnd%> + " OR a.toko IS NULL) ";
        } else if (toko !== "") {
            filters += " AND a.toko = " + toko + " ";
        }

        return filters;
    };

    const triggerSearchDiskon<%=rnd%> = () => {
        currentPage<%=rnd%> = 1; 
        loadDataDiskon<%=rnd%>();
    };

    const loadDataDiskon<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataDiskon<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="9" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Mengambil data...")%></span></td></tr>';

        const sqlFilters = buildFilterSQL<%=rnd%>();
        const offset = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%>;

        const sqlCount = "SELECT COUNT(*) AS jumlah FROM koperasi.aturan_diskon a LEFT JOIN koperasi.produk p ON a.produk = p.id " + sqlFilters + ";";
        
        const sqlData = "SELECT a.id, a.nama_aturan, a.keterangan, a.berlaku_semua_member, a.berlaku_per_hari_dan_per_toko, a.persentase, a.maksimal_potongan, a.nominal, a.potongan_langsung, a.aktif, " +
                        "TO_CHAR(a.tanggal_mulai, 'YYYY-MM-DD\"T\"HH24:MI') as tgl_mulai, " +
                        "TO_CHAR(a.tanggal_selesai, 'YYYY-MM-DD\"T\"HH24:MI') as tgl_selesai, " +
                        "TO_CHAR(a.tanggal_mulai, 'DD-MM-YYYY') as txt_mulai, TO_CHAR(a.tanggal_selesai, 'DD-MM-YYYY') as txt_selesai, " +
                        "p.id AS id_produk, p.nama AS nama_produk, " +
                        "t.id AS id_toko, t.nama AS nama_toko, " +
                        "ja.id AS id_jenis, ja.nama AS nama_jenis, " +
                        "ta.id AS id_tipe, ta.nama AS nama_tipe, " +
                        "a.dikunci, u.usernama as nama_pengunci, a.hari_aktif " +
                        "FROM koperasi.aturan_diskon a " +
                        "LEFT JOIN koperasi.produk p ON a.produk = p.id " +
                        "LEFT JOIN koperasi.toko t ON a.toko = t.id " + 
                        "LEFT JOIN koperasi.jenis_anggota_koperasi ja ON a.jenis_anggota = ja.id " +
                        "LEFT JOIN koperasi.tipe_anggota_koperasi ta ON a.tipe_anggota = ta.id " +
                        "LEFT JOIN public.tbmuser u ON a.dikunci = u.userid " +
                        sqlFilters + " ORDER BY a.id DESC LIMIT " + limitPerPage<%=rnd%> + " OFFSET " + offset + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetchData<%=rnd%>(sqlCount),
                fetchData<%=rnd%>(sqlData)
            ]);

            totalRecords<%=rnd%> = (resCount && resCount[0]) ? parseInt(resCount[0].jumlah || 0) : 0;
            const recordList = resData || [];

            let htmlList = '';

            if (recordList.length === 0) {
                htmlList = '<tr><td colspan="9" class="text-center text-muted py-5"><i class="fas fa-tags fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Tidak ada aturan diskon ditemukan.")%></td></tr>';
            } else {
                let no = offset + 1; 
                recordList.forEach((row) => {

                    const isAktif = (row.aktif === null || row.aktif === true || row.aktif === 't' || row.aktif === 'true');
                    const badgeStatus = isAktif ? '<span class="badge bg-success bg-opacity-10 text-success border border-success px-2"><i class="fas fa-check-circle"></i></span>' : '<span class="badge bg-secondary bg-opacity-10 text-secondary border border-secondary px-2"><i class="fas fa-times-circle"></i></span>';

                    const isAllMember = (row.berlaku_semua_member === true || row.berlaku_semua_member === 't' || row.berlaku_semua_member === 'true');
                    const isPotongLsg = (row.potongan_langsung === true || row.potongan_langsung === 't' || row.potongan_langsung === 'true');
                    const isLimit1x = (row.berlaku_per_hari_dan_per_toko === true || row.berlaku_per_hari_dan_per_toko === 't' || row.berlaku_per_hari_dan_per_toko === 'true');

                    const badgeLimit = isLimit1x ? '<span class="badge bg-warning text-dark"><i class="fas fa-exclamation-circle me-1"></i>Ya</span>' : '<span class="text-muted fw-bold">Tidak</span>';

                    let strTarget = isAllMember ? '<span class="text-success fw-bold"><i class="fas fa-globe me-1"></i>Semua Member</span>' : '<span class="text-primary fw-bold">Spesifik</span>';
                    if (!isAllMember) {
                        strTarget += '<br><small class="text-muted">' + (row.nama_jenis || 'Semua Jenis') + ' / ' + (row.nama_tipe || 'Semua Tipe') + '</small>';
                    }

                    // Target Produk (Semua vs Spesifik)
                    const strProduk = row.id_produk ? '<span class="fw-bold text-primary">' + row.nama_produk.replace(/'/g, "\\'").replace(/"/g, '&quot;') + '</span>' : '<span class="text-success fw-bold"><i class="fas fa-boxes me-1"></i>Semua Produk</span>';

                    let strNilai = '';
                    if (parseFloat(row.persentase) > 0) {
                        strNilai = '<span class="text-danger fw-bold">' + row.persentase + '%</span>';
                        if (parseFloat(row.maksimal_potongan) > 0) strNilai += '<br><small class="text-muted">Max: ' + row.maksimal_potongan + '</small>';
                    } else if (parseFloat(row.nominal) > 0) {
                        strNilai = '<span class="text-danger fw-bold">Rp ' + row.nominal + '</span>';
                    } else {
                        strNilai = '-';
                    }

                    let strEksekusi = isPotongLsg ? '<span class="badge bg-danger rounded-1" title="Potong Struk"><i class="fas fa-cut"></i></span>' : '<span class="badge bg-info text-dark rounded-1" title="Masuk Saldo"><i class="fas fa-wallet"></i></span>';

                    const safeNamaAturan = (row.nama_aturan || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    const safeKet = (row.keterangan || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    const safeProdukName = row.nama_produk ? row.nama_produk.replace(/'/g, "\\'").replace(/"/g, '&quot;') : '';

                    const tokoLabel = row.nama_toko ? '<span class="badge bg-light text-dark border">' + row.nama_toko + '</span>' : '<span class="badge bg-primary">General</span>';

                    // --- LOGIKA TOMBOL & KUNCI ---
                    const idPengunci = row.dikunci; 
                    
                    const isLocked = (idPengunci !== null && idPengunci !== '' && idPengunci !== undefined);
                    let lockBadge = '';
                    let actBtn = '';

                    if (isLocked) {
                        lockBadge = '<div class="mt-1"><span class="badge bg-warning text-dark"><i class="fas fa-lock me-1"></i>Dikunci oleh: ' + (row.nama_pengunci || 'Sistem') + '</span></div>';
                    }

                    if (isAdmin<%=rnd%> || row.id_toko == idTokoLogin<%=rnd%>) {
                        if (!isLocked) {
                            actBtn = '<button class="btn btn-sm btn-outline-warning text-dark shadow-sm px-2 me-1 mb-1 fw-bold" onclick="editDiskon<%=rnd%>(' + row.id + ', \'' + safeNamaAturan + '\', ' + (row.id_toko || 'null') + ', ' + (row.id_produk || 'null') + ', \'' + safeProdukName + '\', ' + isAllMember + ', ' + (row.id_jenis || 'null') + ', ' + (row.id_tipe || 'null') + ', ' + (row.persentase || 0) + ', ' + (row.maksimal_potongan || 0) + ', ' + (row.nominal || 0) + ', ' + isPotongLsg + ', \'' + (row.tgl_mulai || '') + '\', \'' + (row.tgl_selesai || '') + '\', ' + isAktif + ', \'' + safeKet + '\', ' + isLimit1x + ', \'' + (row.hari_aktif || '') + '\')" title="<%=Common.getBahasaConfig("Ubah Data")%>"><i class="fas fa-edit"></i></button>' +
                                     '<button class="btn btn-sm btn-outline-secondary text-dark shadow-sm px-2 me-1 mb-1 fw-bold" onclick="kunciDiskon<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Kunci Data Ini")%>"><i class="fas fa-lock"></i></button>' +
                                     '<button class="btn btn-sm btn-outline-danger shadow-sm px-2 mb-1 fw-bold" onclick="hapusDiskon<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Hapus Data")%>"><i class="fas fa-trash-alt"></i></button>';
                        } else {
                            if (isAdmin<%=rnd%> || idPengunci == idUserLogin<%=rnd%>) {
                                actBtn = '<button class="btn btn-sm btn-warning text-dark shadow-sm px-3 fw-bold" onclick="bukaKunciDiskon<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Buka Kunci")%>"><i class="fas fa-unlock me-1"></i>Buka</button>';
                            } else {
                                actBtn = '<span class="text-muted small"><i class="fas fa-lock"></i> Locked</span>';
                            }
                        }
                    } else {
                        actBtn = '<span class="text-muted small"><i class="fas fa-eye"></i> Readonly</span>';
                    }

                    htmlList += '<tr>' +
                            '<td class="text-center text-muted fw-medium">' + (no++) + '</td>' +
                            '<td class="text-start">' +
                                '<div class="fw-bold text-dark">' + (row.nama_aturan || '-') + '</div>' +
                                '<div>' + tokoLabel + '</div>' +
                                lockBadge +
                            '</td>' +
                            '<td class="text-start">' + strProduk + '</td>' +
                            '<td class="text-start">' + strTarget + '</td>' +
                            '<td class="text-end">' + strNilai + ' ' + strEksekusi + '</td>' +
                            '<td class="text-center">' + badgeLimit + '</td>' +
                            '<td class="text-center small text-muted">' + (row.txt_mulai || '-') + '<br>s/d<br>' + (row.txt_selesai || '-') + '</td>' +
                            '<td class="text-center">' + badgeStatus + '</td>' +
                            '<td class="text-center text-nowrap">' + actBtn + '</td>' +
                        '</tr>';
                });
            }
            tbody.innerHTML = htmlList;
            updatePaginationUI<%=rnd%>();

        } catch (error) {
            console.error("Gagal load tabel diskon:", error);
            tbody.innerHTML = '<tr><td colspan="9" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal menarik data. Silakan muat ulang.")%></td></tr>';
        }
    };

    const updatePaginationUI<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumberDisplay<%=rnd%>').innerText = currentPage<%=rnd%> + " / " + totalPages;
        
        const startIdx = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%> + 1;
        const endIdx = Math.min(currentPage<%=rnd%> * limitPerPage<%=rnd%>, totalRecords<%=rnd%>);
        const startInfo = totalRecords<%=rnd%> > 0 ? startIdx : 0;
        
        document.getElementById('pagingInfoDiskon<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startInfo + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari")%> <b class="text-dark">' + totalRecords<%=rnd%> + '</b> <%=Common.getBahasaConfig("data")%>';
        
        document.getElementById('btnPrevPage<%=rnd%>').disabled = (currentPage<%=rnd%> === 1);
        document.getElementById('btnNextPage<%=rnd%>').disabled = (currentPage<%=rnd%> === totalPages);
    };

    const changePageDiskon<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if (direction === -1 && currentPage<%=rnd%> > 1) {
            currentPage<%=rnd%>--;
            loadDataDiskon<%=rnd%>();
        } else if (direction === 1 && currentPage<%=rnd%> < totalPages) {
            currentPage<%=rnd%>++;
            loadDataDiskon<%=rnd%>();
        }
    };


    // ==========================================
    // BAGIAN 3: LOGIKA FORM & DINAMIS UI 
    // ==========================================
    const bukaFormTambah<%=rnd%> = () => {
        
        document.getElementById('inputIdDiskon<%=rnd%>').value = '';
        document.getElementById('formDiskon<%=rnd%>').reset();
        
        document.getElementById('idProdukSelected<%=rnd%>').value = "";
        
        if (isAdmin<%=rnd%>) {
            document.getElementById('inputTokoForm<%=rnd%>').value = "";
        }
        
        // Reset Checkbox & Toggle
        document.getElementById('inputAktif<%=rnd%>').checked = true;
        document.getElementById('inputAktif<%=rnd%>').dispatchEvent(new Event('change'));
        document.getElementById('inputBerlakuSemua<%=rnd%>').checked = true;
        document.getElementById('inputBerlaku1xHari<%=rnd%>').checked = false;
        toggleMemberTarget<%=rnd%>();
        document.getElementById('radioPotongStruk<%=rnd%>').checked = true;

        document.getElementById('inputSemuaProduk<%=rnd%>').checked = false;
        
        loadComboProduk<%=rnd%>(); 

        document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-plus-circle text-primary me-2"></i><%=Common.getBahasaConfig("Tambah Aturan Promo Baru")%>';
        
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    };

    const tutupForm<%=rnd%> = () => {
        document.getElementById('viewForm<%=rnd%>').style.display = 'none';
        document.getElementById('viewList<%=rnd%>').style.display = 'block';
        loadDataDiskon<%=rnd%>();
    };


    // ==========================================
    // BAGIAN 4: SIMPAN, EDIT, HAPUS, KUNCI
    // ==========================================
    const saveDiskon<%=rnd%> = async () => {
        const idDiskon = document.getElementById('inputIdDiskon<%=rnd%>').value;
        const tokoId = isAdmin<%=rnd%> ? document.getElementById('inputTokoForm<%=rnd%>').value : idTokoLogin<%=rnd%>;
        
        // Cek Produk
        const isAllProduk = document.getElementById('inputSemuaProduk<%=rnd%>').checked;
        let produkId = "";
        if (!isAllProduk) {
            produkId = document.getElementById('idProdukSelected<%=rnd%>').value;
            if(produkId === "") {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Mohon pilih Produk target promo, atau centang Berlaku Untuk Semua Produk!")%>', 'bg-warning text-dark');
                document.getElementById('inputCariProduk<%=rnd%>').focus();
                return;
            }
        }
        
        const isAllMember = document.getElementById('inputBerlakuSemua<%=rnd%>').checked;
        const isLimit1x = document.getElementById('inputBerlaku1xHari<%=rnd%>').checked;
        const jenisId = document.getElementById('inputJenisAnggota<%=rnd%>').value;
        const tipeId = document.getElementById('inputTipeAnggota<%=rnd%>').value;

        const valPersen = parseFloat(document.getElementById('inputPersentase<%=rnd%>').value) || 0;
        const valMax = parseFloat(document.getElementById('inputMaksPotongan<%=rnd%>').value) || 0;
        const valNominal = parseFloat(document.getElementById('inputNominal<%=rnd%>').value) || 0;

        const isPotongLangsung = document.getElementById('radioPotongStruk<%=rnd%>').checked;

        const btnSimpan = document.getElementById('btnSimpan<%=rnd%>');
        const oriBtn = btnSimpan.innerHTML;
        btnSimpan.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyimpan...")%>';
        btnSimpan.disabled = true;

        const dataObj = {
            namaAturan: document.getElementById('inputNamaAturan<%=rnd%>').value.trim(),
            keterangan: document.getElementById('inputKeterangan<%=rnd%>').value.trim(),
            berlakuSemuaMember: isAllMember,
            berlakuPerHariDanPerToko: isLimit1x,
            persentase: valPersen,
            maksimalPotongan: valMax,
            nominal: valNominal,
            potonganLangsung: isPotongLangsung,
            aktif: document.getElementById('inputAktif<%=rnd%>').checked,
            // Jika isAllProduk, biarkan produk null/kosong (jangan dikirim)
        };
        dataObj.produk = null;
        if (!isAllProduk && produkId !== "") {
            dataObj.produk = produkId;
        }

        if (tokoId !== "") dataObj.toko = tokoId;
        if (!isAllMember) {
            if (jenisId !== "") dataObj.jenisAnggota = jenisId;
            if (tipeId !== "") dataObj.tipeAnggota = tipeId;
        }

        const tglMulai = document.getElementById('inputTglMulai<%=rnd%>').value;
        const tglSelesai = document.getElementById('inputTglSelesai<%=rnd%>').value;
        if(tglMulai !== "") dataObj.tanggalMulai = tglMulai;
        if(tglSelesai !== "") dataObj.tanggalSelesai = tglSelesai;

        // Gap-closure "Promo Pilih Hari" -- CSV angka hari yg dicentang, kosong/tak ada yg dicentang =
        // berlaku semua hari (konsisten dgn null=tanpa batas di tanggalMulai/tanggalSelesai).
        const hariTerpilih<%=rnd%> = [];
        for (let hh = 1; hh <= 7; hh++) {
            const cb = document.getElementById('inputHari' + hh + '_<%=rnd%>');
            if (cb && cb.checked) hariTerpilih<%=rnd%>.push(hh);
        }
        dataObj.hariAktif = hariTerpilih<%=rnd%>.length > 0 ? hariTerpilih<%=rnd%>.join(',') : null;

        const payload = { 
            action: "simpanDataRinci", 
            class: masterModelClass<%=rnd%>, 
            data: dataObj
        };

        if (idDiskon !== '') payload.id = idDiskon;

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const result = await response.json();
            if (result.status === '00' || result.status === 'success' || result.id) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Aturan diskon berhasil disimpan!")%>', 'bg-success text-white');
                tutupForm<%=rnd%>(); 
            } else {
                showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan data.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            console.error(e);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi.")%>', 'bg-danger text-white');
        } finally {
            btnSimpan.innerHTML = oriBtn;
            btnSimpan.disabled = false;
        }
    };

    const pilihSemuaHariDiskon<%=rnd%> = () => {
        for (let hh = 1; hh <= 7; hh++) {
            const cb = document.getElementById('inputHari' + hh + '_<%=rnd%>');
            if (cb) cb.checked = false;
        }
    };

    const editDiskon<%=rnd%> = async (id, namaAturan, idToko, idProduk, namaProdukFull, isAllMember, idJenis, idTipe, persen, maxPotongan, nominal, isPotongLsg, tMulai, tSelesai, isAktif, ket, isLimit, hariAktif) => {
        document.getElementById('inputIdDiskon<%=rnd%>').value = id;
        // Gap-closure "Promo Pilih Hari" -- centang ulang checkbox sesuai CSV tersimpan, kosong = semua hari
        const hariAktifSet<%=rnd%> = (hariAktif || '').split(',').map(s => s.trim()).filter(s => s !== '');
        for (let hh = 1; hh <= 7; hh++) {
            const cb = document.getElementById('inputHari' + hh + '_<%=rnd%>');
            if (cb) cb.checked = hariAktifSet<%=rnd%>.indexOf(String(hh)) !== -1;
        }
        document.getElementById('inputNamaAturan<%=rnd%>').value = namaAturan;
        
        if (isAdmin<%=rnd%>) {
            document.getElementById('inputTokoForm<%=rnd%>').value = idToko || '';
        }
        
        // Logika Target Produk
        if (idProduk === null || idProduk === 'null' || idProduk === "") {
            document.getElementById('inputSemuaProduk<%=rnd%>').checked = true;
            await loadComboProduk<%=rnd%>(); 
        } else {
            document.getElementById('inputSemuaProduk<%=rnd%>').checked = false;
            await loadComboProduk<%=rnd%>(idProduk, namaProdukFull);
        }
        
        document.getElementById('inputBerlakuSemua<%=rnd%>').checked = isAllMember;
        document.getElementById('inputBerlaku1xHari<%=rnd%>').checked = isLimit;
        toggleMemberTarget<%=rnd%>();
        if (!isAllMember) {
            document.getElementById('inputJenisAnggota<%=rnd%>').value = idJenis || '';
            document.getElementById('inputTipeAnggota<%=rnd%>').value = idTipe || '';
        }

        document.getElementById('inputPersentase<%=rnd%>').value = persen;
        document.getElementById('inputMaksPotongan<%=rnd%>').value = maxPotongan;
        document.getElementById('inputNominal<%=rnd%>').value = nominal;

        if(isPotongLsg) document.getElementById('radioPotongStruk<%=rnd%>').checked = true;
        else document.getElementById('radioCashback<%=rnd%>').checked = true;

        document.getElementById('inputTglMulai<%=rnd%>').value = tMulai;
        document.getElementById('inputTglSelesai<%=rnd%>').value = tSelesai;
        document.getElementById('inputKeterangan<%=rnd%>').value = ket;

        const chkAktif = document.getElementById('inputAktif<%=rnd%>');
        chkAktif.checked = isAktif;
        chkAktif.dispatchEvent(new Event('change')); 

        document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-edit text-warning me-2"></i><%=Common.getBahasaConfig("Ubah Aturan Diskon")%>';
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    };

    const hapusDiskon<%=rnd%> = async (id) => {
        if (typeof prosesDeleteData === 'function') {
            prosesDeleteData(masterModelClass<%=rnd%>, id, function(){ 
                 if (document.querySelectorAll('#tabelDataDiskon<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) {
                     currentPage<%=rnd%>--;
                 }
                 loadDataDiskon<%=rnd%>();
            });
        } else {
            const isConfirm = confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus aturan ini?")%>');
            if (!isConfirm) return;

            const payload = { action: "deleteData", class: masterModelClass<%=rnd%>, id: id };
            try {
                const response = await fetch('<%=Common.ROOT%>/Data', {
                    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
                });
                const result = await response.json();
                if (result.status === '00' || result.status === 'success') {
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Berhasil dihapus!")%>', 'bg-success text-white');
                    if (document.querySelectorAll('#tabelDataDiskon<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) currentPage<%=rnd%>--;
                    loadDataDiskon<%=rnd%>();
                } else {
                    showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Data gagal dihapus.")%>', 'bg-danger text-white');
                }
            } catch (error) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal terhubung ke server.")%>', 'bg-danger text-white');
            }
        }
    };
    
    // --- FITUR KUNCI (LOCK / UNLOCK) ---
    const setKunciData<%=rnd%> = async (id, targetUserId) => {
        const payload = { 
            action: "simpanDataRinci", 
            class: masterModelClass<%=rnd%>, 
            id: id,
            data: {} // Hanya update field tertentu
        };

        console.log("targetUserId ", targetUserId);
        if (targetUserId === null) {
            payload.data.dikunci = null; // Unlock
        } else {
            payload.data.dikunci = targetUserId; // Lock
        }

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const result = await response.json();
            
            console.log("result ", result);
            
            if (result.status === '00' || result.status === 'success') {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Status kunci berhasil diubah!")%>', 'bg-success text-white');
                loadDataDiskon<%=rnd%>(); 
            } else {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal mengubah status kunci.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi.")%>', 'bg-danger text-white');
        }
    };

    const kunciDiskon<%=rnd%> = (id) => {
        if(confirm('<%=Common.getBahasaConfigJS("Kunci aturan ini? Setelah dikunci, data hanya bisa diubah oleh Anda.")%>')) {
            setKunciData<%=rnd%>(id, idUserLogin<%=rnd%>);
        }
    };

    const bukaKunciDiskon<%=rnd%> = (id) => {
        if(confirm('<%=Common.getBahasaConfigJS("Buka kunci aturan ini? Orang lain akan dapat mengubah data ini.")%>')) {
            setKunciData<%=rnd%>(id, null);
        }
    };


    // ==========================================
    // DOWNLOAD & UPLOAD EXCEL FEATURE
    // ==========================================
    const downloadExcelDiskon<%=rnd%> = async () => {
        showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Menyiapkan file Excel...")%>', 'bg-info text-dark');
        
        const sqlFilters = buildFilterSQL<%=rnd%>();
        const sqlQuery = "SELECT a.id, a.nama_aturan, " +
                         "p.id AS id_produk, p.nama AS nama_produk, a.toko AS id_toko, " +
                         "a.berlaku_semua_member, a.jenis_anggota, a.tipe_anggota, " +
                         "a.persentase, a.maksimal_potongan, a.nominal, a.potongan_langsung, a.berlaku_per_hari_dan_per_toko, " +
                         "TO_CHAR(a.tanggal_mulai, 'YYYY-MM-DD\"T\"HH24:MI') as tgl_mulai, " +
                         "TO_CHAR(a.tanggal_selesai, 'YYYY-MM-DD\"T\"HH24:MI') as tgl_selesai, " +
                         "a.keterangan, a.aktif " +
                         "FROM koperasi.aturan_diskon a " +
                         "LEFT JOIN koperasi.produk p ON a.produk = p.id " +
                         sqlFilters + " ORDER BY a.id DESC;";

        try {
            const result = await fetchData<%=rnd%>(sqlQuery);
            if (!result || result.length === 0) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Tidak ada data untuk diunduh.")%>', 'bg-warning text-dark');
                return;
            }

            let tableHtml = '<html xmlns:x="urn:schemas-microsoft-com:office:excel">';
            tableHtml += '<head><meta charset="UTF-8"><style>th { background-color: #f8f9fa; font-weight: bold; border: 1px solid black; } td { border: 1px solid black; }</style></head><body>';
            
            tableHtml += '<table><thead><tr>';
            tableHtml += '<th>ID_SISTEM</th><th>NAMA_ATURAN</th><th>ID_TOKO</th><th>ID_PRODUK</th><th>NAMA_PRODUK_INFO</th>';
            tableHtml += '<th>BERLAKU_SEMUA_MEMBER</th><th>ID_JENIS_ANGGOTA</th><th>ID_TIPE_ANGGOTA</th>';
            tableHtml += '<th>PERSENTASE</th><th>MAKS_POTONGAN</th><th>NOMINAL</th><th>POTONGAN_LANGSUNG</th><th>BERLAKU_1X_HARI_PER_TOKO</th>';
            tableHtml += '<th>TGL_MULAI</th><th>TGL_SELESAI</th><th>KETERANGAN</th><th>AKTIF</th>';
            tableHtml += '</tr></thead><tbody>';

            result.forEach(r => {
                tableHtml += '<tr>';
                tableHtml += '<td>' + (r.id || '') + '</td><td>' + (r.nama_aturan || '') + '</td><td>' + (r.id_toko || '') + '</td><td>' + (r.id_produk || '') + '</td><td>' + (r.nama_produk || '') + '</td>';
                tableHtml += '<td>' + (r.berlaku_semua_member === true || r.berlaku_semua_member === 't' ? 'TRUE' : 'FALSE') + '</td>';
                tableHtml += '<td>' + (r.jenis_anggota || '') + '</td><td>' + (r.tipe_anggota || '') + '</td>';
                tableHtml += '<td>' + (r.persentase || 0) + '</td><td>' + (r.maksimal_potongan || 0) + '</td><td>' + (r.nominal || 0) + '</td>';
                tableHtml += '<td>' + (r.potongan_langsung === true || r.potongan_langsung === 't' ? 'TRUE' : 'FALSE') + '</td>';
                tableHtml += '<td>' + (r.berlaku_per_hari_dan_per_toko === true || r.berlaku_per_hari_dan_per_toko === 't' ? 'TRUE' : 'FALSE') + '</td>';
                tableHtml += '<td>' + (r.tgl_mulai || '') + '</td><td>' + (r.tgl_selesai || '') + '</td><td>' + (r.keterangan || '') + '</td>';
                tableHtml += '<td>' + (r.aktif === true || r.aktif === 't' ? 'TRUE' : 'FALSE') + '</td>';
                tableHtml += '</tr>';
            });

            tableHtml += '</tbody></table><jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp"/>
</body></html>';

            const blob = new Blob([tableHtml], { type: 'application/vnd.ms-excel' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement("a");
            link.download = "Format_AturanDiskon_" + new Date().getTime() + ".xls";
            link.href = url;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            setTimeout(() => URL.revokeObjectURL(url), 100);

        } catch (error) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal mengunduh data.")%>', 'bg-danger text-white');
        }
    };

    const showUploadModal<%=rnd%> = () => {
        document.getElementById('fileUploadExcel<%=rnd%>').value = '';
        const statusBox = document.getElementById('uploadStatusBox<%=rnd%>');
        statusBox.style.display = 'none';
        
        if(modalInstanceUpload<%=rnd%> === null) {
            modalInstanceUpload<%=rnd%> = new bootstrap.Modal(document.getElementById('modalUploadExcel<%=rnd%>'));
        }
        modalInstanceUpload<%=rnd%>.show();
    };

    const prosesUploadExcel<%=rnd%> = () => {
        const fileInput = document.getElementById('fileUploadExcel<%=rnd%>');
        const file = fileInput.files[0];
        if (!file) return showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Pilih file Excel terlebih dahulu!")%>', 'bg-warning text-dark');
        
        const btnProcess = document.getElementById('btnProsesUpload<%=rnd%>');
        const statusBox = document.getElementById('uploadStatusBox<%=rnd%>');
        
        btnProcess.disabled = true;
        btnProcess.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Membaca File...")%>';
        statusBox.style.display = 'block';
        statusBox.className = 'alert alert-info py-2 small';
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
                        id: row['ID_SISTEM'] ? row['ID_SISTEM'].toString().trim() : "",
                        namaAturan: row['NAMA_ATURAN'] || "",
                        toko: isAdmin<%=rnd%> ? (row['ID_TOKO'] || "") : idTokoLogin<%=rnd%>,
                        produk: row['ID_PRODUK'] || null, // Convert empty string to null for Global Product
                        berlakuSemuaMember: (row['BERLAKU_SEMUA_MEMBER'] && row['BERLAKU_SEMUA_MEMBER'].toString().toUpperCase() === 'TRUE'),
                        jenisAnggota: row['ID_JENIS_ANGGOTA'] || null,
                        tipeAnggota: row['ID_TIPE_ANGGOTA'] || null,
                        persentase: parseFloat(row['PERSENTASE']) || 0,
                        maksimalPotongan: parseFloat(row['MAKS_POTONGAN']) || 0,
                        nominal: parseFloat(row['NOMINAL']) || 0,
                        potonganLangsung: (row['POTONGAN_LANGSUNG'] && row['POTONGAN_LANGSUNG'].toString().toUpperCase() === 'TRUE'),
                        berlakuPerHariDanPerToko: (row['BERLAKU_1X_HARI_PER_TOKO'] && row['BERLAKU_1X_HARI_PER_TOKO'].toString().toUpperCase() === 'TRUE'),
                        tanggalMulai: row['TGL_MULAI'] || "",
                        tanggalSelesai: row['TGL_SELESAI'] || "",
                        keterangan: row['KETERANGAN'] || "",
                        aktif: (row['AKTIF'] && row['AKTIF'].toString().toUpperCase() === 'TRUE')
                    };
                    return obj;
                });

                const payload = { action: "simpanBatchDataRinci", class: masterModelClass<%=rnd%>, dataBatch: batchData };
                const response = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
                const result = await response.json();

                if (result.status === '00' || result.status === 'success') {
                    statusBox.className = 'alert alert-success py-2 small fw-bold';
                    statusBox.innerHTML = '<i class="fas fa-check-circle me-2"></i>Data berhasil diunggah!';
                    setTimeout(() => { modalInstanceUpload<%=rnd%>.hide(); loadDataDiskon<%=rnd%>(); }, 1500);
                } else throw new Error(result.description || "Gagal menyimpan di sisi server.");
            } catch (error) {
                statusBox.className = 'alert alert-danger py-2 small';
                statusBox.innerHTML = '<i class="fas fa-exclamation-triangle me-2"></i>Kesalahan: ' + error.message;
            } finally {
                btnProcess.disabled = false;
                btnProcess.innerHTML = '<i class="fas fa-cogs me-2"></i><%=Common.getBahasaConfig("Proses Data")%>';
            }
        };
        reader.readAsArrayBuffer(file);
    };

    // ==========================================
    // INISIALISASI HALAMAN
    // ==========================================
    document.addEventListener("DOMContentLoaded", () => {
        loadMasterDataCombo<%=rnd%>();
        const filterInputs = document.querySelectorAll('.filter-input-<%=rnd%>');
        filterInputs.forEach(input => {
            if (input.tagName === 'SELECT') {
                input.addEventListener("change", () => triggerSearchDiskon<%=rnd%>());
            } else {
                input.addEventListener("keydown", (event) => {
                    if (event.key === "Enter") { event.preventDefault(); triggerSearchDiskon<%=rnd%>(); }
                });
            }
        });
        loadDataDiskon<%=rnd%>();
    });
</script>