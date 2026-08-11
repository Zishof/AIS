<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="ais.database.model.inventory.Produk"%>
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
Pedagang pedagang = tbmuser.getPedagang();
Toko toko = pedagang == null ? null : pedagang.getToko();
String rnd = Common.getGeneratedBarCode(7);

boolean isAdmin = (toko == null);
boolean pedagangBolehMengubahHarga = Common.getKonfigurasi("pedagang_boleh_mengubah_harga_barang", Konfigurasi.TIDAK_AKTIF).getNilai().equals(Konfigurasi.AKTIF);

String idTokoAktif = toko != null ? String.valueOf(toko.getId()) : "";
String namaTokoAktif = toko != null ? toko.getNama() : "";
%>

<script data-cfasync="false" src="https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js"></script>

<div class="row mb-4 animate__animated animate__fadeInUp" id="viewList<%=rnd%>">
    <div class="col-12">
        <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
            
            <div class="card-header bg-white border-0 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center flex-wrap gap-3 border-bottom border-light">
                <div>
                    <h5 class="fw-bold text-dark mb-1">
                        <i class="fas fa-box-open text-primary me-2"></i>
                        <% if (isAdmin) { %>
                            <%=Common.getBahasaConfig("Manajemen Katalog Produk (Semua Toko)")%>
                        <% } else { %>
                            <%=Common.getBahasaConfig("Katalog Produk - ")%> <span class="text-primary"><%=namaTokoAktif%></span>
                        <% } %>
                    </h5>
                    <small class="text-muted"><%=Common.getBahasaConfig("Kelola data barang/jasa, harga, stok, gambar, dan kategori produk Anda.")%></small>
                </div>
                <div class="d-flex flex-wrap gap-2">
                    
                    <button class="btn btn-info text-white rounded-pill px-3 fw-bold shadow-sm" onclick="sinkronisasiStok<%=rnd%>()" id="btnSyncStok<%=rnd%>" title="<%=Common.getBahasaConfig("Kalkulasi ulang stok berdasarkan histori pengadaan dan penjualan")%>">
                        <i class="fas fa-sync me-1"></i><%=Common.getBahasaConfig("Sinkronkan Stok")%>
                    </button>

                    <button class="btn btn-warning rounded-pill px-3 fw-bold shadow-sm" onclick="showUploadModal<%=rnd%>()" title="<%=Common.getBahasaConfig("Unggah file Excel")%>">
                        <i class="fas fa-file-upload me-1"></i><%=Common.getBahasaConfig("Unggah")%>
                    </button>
                    <button class="btn btn-success rounded-pill px-3 fw-bold shadow-sm" onclick="downloadExcelProduk<%=rnd%>()" title="<%=Common.getBahasaConfig("Unduh file Excel")%>">
                        <i class="fas fa-file-download me-1"></i><%=Common.getBahasaConfig("Unduh")%>
                    </button>
                    <button class="btn btn-primary rounded-pill px-4 fw-bold shadow-sm" onclick="showAddModal<%=rnd%>()">
                        <i class="fas fa-plus-circle me-2"></i><%=Common.getBahasaConfig("Tambah Produk")%>
                    </button>
                </div>
            </div>

            <div class="card-body p-4">

                <%-- Fitur "Dasbor Statistik Produk" -- kartu ringkasan + 3 rincian batang (kategori/
                     pemasok/rentang harga), dihitung SEKALI saat halaman dibuka (scope toko login bila
                     terkunci, else semua toko), TIDAK ikut berubah mengikuti filter Kode/Nama/Toko di
                     bawah -- angka "kondisi katalog saat ini". Query SAMA PERSIS dgn
                     KantinHelper.produkStatistik (aksi server dipakai Desktop/Android) supaya taat
                     asas lintas platform, dijalankan di sini lewat fetchSqlData (endpoint SQL generik
                     yg sudah dipakai seluruh halaman ini), BUKAN lewat PosApi. --%>
                <div class="row g-2 mb-4" id="statistikProdukKpi<%=rnd%>">
                    <div class="col-6 col-md-2"><div class="p-3 rounded-3 border bg-white shadow-sm h-100"><div class="small text-muted fw-bold text-uppercase" style="font-size:10px;">Total Produk</div><div class="fs-5 fw-bold" id="spTotal<%=rnd%>">0</div></div></div>
                    <div class="col-6 col-md-2"><div class="p-3 rounded-3 border bg-white shadow-sm h-100"><div class="small text-muted fw-bold text-uppercase" style="font-size:10px;">Aktif</div><div class="fs-5 fw-bold text-success" id="spAktif<%=rnd%>">0</div></div></div>
                    <div class="col-6 col-md-2"><div class="p-3 rounded-3 border bg-white shadow-sm h-100"><div class="small text-muted fw-bold text-uppercase" style="font-size:10px;">Non-Aktif</div><div class="fs-5 fw-bold text-secondary" id="spNonaktif<%=rnd%>">0</div></div></div>
                    <div class="col-6 col-md-2"><div class="p-3 rounded-3 border bg-white shadow-sm h-100"><div class="small text-muted fw-bold text-uppercase" style="font-size:10px;">Stok Habis</div><div class="fs-5 fw-bold text-danger" id="spHabis<%=rnd%>">0</div></div></div>
                    <div class="col-6 col-md-2"><div class="p-3 rounded-3 border bg-white shadow-sm h-100"><div class="small text-muted fw-bold text-uppercase" style="font-size:10px;">Stok &le;5</div><div class="fs-5 fw-bold text-warning" id="spRendah<%=rnd%>">0</div></div></div>
                    <div class="col-6 col-md-2"><div class="p-3 rounded-3 border bg-white shadow-sm h-100"><div class="small text-muted fw-bold text-uppercase" style="font-size:10px;">Nilai Stok</div><div class="fs-6 fw-bold" id="spNilaiStok<%=rnd%>">Rp 0</div></div></div>
                </div>
                <div class="row g-3 mb-4">
                    <div class="col-md-4"><div class="p-3 rounded-3 border bg-white shadow-sm h-100"><div class="small fw-bold mb-2">Per Kategori</div><div id="spBarKategori<%=rnd%>"></div></div></div>
                    <div class="col-md-4"><div class="p-3 rounded-3 border bg-white shadow-sm h-100"><div class="small fw-bold mb-2">Per Pemasok/Vendor</div><div id="spBarPemasok<%=rnd%>"></div></div></div>
                    <div class="col-md-4"><div class="p-3 rounded-3 border bg-white shadow-sm h-100"><div class="small fw-bold mb-2">Per Rentang Harga Jual</div><div id="spBarHarga<%=rnd%>"></div></div></div>
                </div>

                <div class="bg-light p-3 rounded-3 border shadow-sm mb-4">
                    <div class="row g-2 align-items-end">
                        <div class="col-md-2">
                            <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Kode/Barcode")%></label>
                            <input type="text" class="form-control form-control-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterKode<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Kode atau Barcode")%>">
                        </div>
                        <div class="col-md-3">
                            <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Nama Produk")%></label>
                            <input type="text" class="form-control form-control-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterNama<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari nama...")%>">
                        </div>
                        <div class="col-md-3">
                            <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Jenis Produk")%></label>
                            <select class="form-select form-select-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterJenis<%=rnd%>">
                                <option value=""><%=Common.getBahasaConfig("- Semua Jenis -")%></option>
                            </select>
                        </div>
                        <div class="col-md-auto">
                            <label class="form-label small fw-bold text-secondary mb-1 d-block"><%=Common.getBahasaConfig("Jenis Item")%></label>
                            <div class="btn-group btn-group-sm shadow-sm" role="group">
                                <button type="button" class="btn btn-outline-secondary active" id="btnFilterJenisItemSemua<%=rnd%>" onclick="setFilterJenisItem<%=rnd%>('')"><%=Common.getBahasaConfig("Semua")%></button>
                                <button type="button" class="btn btn-outline-secondary" id="btnFilterJenisItemJual<%=rnd%>" onclick="setFilterJenisItem<%=rnd%>('JUAL')"><%=Common.getBahasaConfig("Produk")%></button>
                                <button type="button" class="btn btn-outline-secondary" id="btnFilterJenisItemBahan<%=rnd%>" onclick="setFilterJenisItem<%=rnd%>('BAHAN')"><%=Common.getBahasaConfig("Bahan")%></button>
                            </div>
                        </div>

                        <% if (isAdmin) { %>
                        <div class="col-md-3">
                            <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Toko / Pedagang")%></label>
                            <select class="form-select form-select-sm border-0 shadow-sm filter-input-<%=rnd%>" id="filterToko<%=rnd%>">
                                <option value=""><%=Common.getBahasaConfig("- Semua Toko -")%></option>
                            </select>
                        </div>
                        <% } else { %>
                            <input type="hidden" id="filterToko<%=rnd%>" value="<%=idTokoAktif%>">
                        <% } %>

                        <div class="col-md-auto ms-auto">
                            <button class="btn btn-sm btn-secondary fw-bold px-3 shadow-sm" onclick="triggerSearchProduk<%=rnd%>()">
                                <i class="fas fa-search me-1"></i> <%=Common.getBahasaConfig("Saring")%>
                            </button>
                        </div>
                    </div>
                </div>

                <div class="table-responsive border rounded-3 shadow-sm bg-white">
                    <table class="table table-hover table-striped align-middle mb-0">
                        <thead class="table-dark text-center align-middle">
                            <tr>
                                <th class="fw-semibold text-uppercase small py-3" style="width: 50px;">#</th>
                                <th class="fw-semibold text-uppercase small" style="width: 70px;"><%=Common.getBahasaConfig("Gambar")%></th>
                                <th class="fw-semibold text-uppercase small text-start"><%=Common.getBahasaConfig("Kode")%></th>
                                <th class="fw-semibold text-uppercase small text-start"><%=Common.getBahasaConfig("Nama Produk")%></th>
                                <th class="fw-semibold text-uppercase small text-start"><%=Common.getBahasaConfig("Jenis")%></th>
                                <% if (isAdmin) { %>
                                    <th class="fw-semibold text-uppercase small text-start"><%=Common.getBahasaConfig("Toko")%></th>
                                <% } %>
                                <th class="fw-semibold text-uppercase small text-end"><%=Common.getBahasaConfig("Harga Beli")%></th>
                                <th class="fw-semibold text-uppercase small text-end"><%=Common.getBahasaConfig("Harga Jual")%></th>
                                <th class="fw-semibold text-uppercase small"><%=Common.getBahasaConfig("Stok")%></th>
                                <th class="fw-semibold text-uppercase small"><%=Common.getBahasaConfig("Status")%></th>
                                <th class="fw-semibold text-uppercase small text-center" style="width: 100px;"><%=Common.getBahasaConfig("Aksi")%></th>
                            </tr>
                        </thead>
                        <tbody id="tableProduk<%=rnd%>">
                        </tbody>
                    </table>
                </div>

                <div class="d-flex justify-content-between align-items-center mt-4 pt-2">
                    <span class="text-muted small fw-medium bg-light px-3 py-1 rounded-pill border" id="pagingInfoProduk<%=rnd%>"></span>
                    <nav>
                        <ul class="pagination pagination-sm mb-0 shadow-sm">
                            <li class="page-item">
                                <button class="page-link text-primary rounded-start-pill px-3" id="btnPrevProduk<%=rnd%>" onclick="changePageProduk<%=rnd%>(-1)"><i class="fas fa-chevron-left"></i></button>
                            </li>
                            <li class="page-item disabled">
                                <span class="page-link fw-bold bg-light text-dark px-3" id="pageNumberProduk<%=rnd%>">1</span>
                            </li>
                            <li class="page-item">
                                <button class="page-link text-primary rounded-end-pill px-3" id="btnNextProduk<%=rnd%>" onclick="changePageProduk<%=rnd%>(1)"><i class="fas fa-chevron-right"></i></button>
                            </li>
                        </ul>
                    </nav>
                </div>

            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="modalFormProduk<%=rnd%>" tabindex="-1" data-bs-backdrop="static" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered">
        <div class="modal-content border-0 rounded-4 shadow-lg">
            <div class="modal-header bg-light border-bottom-0 pb-3">
                <h5 class="modal-title fw-bold text-dark" id="modalTitleProduk<%=rnd%>">
                    <i class="fas fa-box text-primary me-2"></i><%=Common.getBahasaConfig("Form Produk")%>
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4 pt-2">
                <form id="formProduk<%=rnd%>" onsubmit="event.preventDefault(); saveProduk<%=rnd%>();">
                    <input type="hidden" id="formId<%=rnd%>" value="">
                    
                    <div class="row g-3">
                        
                        <div class="col-md-12 mb-3 mt-2 text-center" id="uploadPhotoArea<%=rnd%>" style="display: none;">
                            <div class="p-3 bg-light rounded-3 border">
                                <label class="form-label small fw-bold text-secondary d-block"><%=Common.getBahasaConfig("Gambar Produk")%></label>
                                
                                <div class="mb-2">
                                    <img id="previewImage<%=rnd%>" src="<%=Common.ROOT%>/img/empty_image.png" onerror="this.onerror=null; this.src='<%=Common.ROOT%>/img/empty_image.png';" alt="Produk" class="rounded-3 shadow-sm object-fit-cover border border-2 border-primary" style="width: 150px; height: 150px; background-color: #fff;">
                                </div>
                                
                                <div class="d-flex justify-content-center align-items-center gap-2 mt-3">
                                    <input type="file" id="input_dofile_foto_<%=rnd%>" class="d-none" accept="image/*" onchange="uploadFile<%=rnd%>()">
                                    <input type="hidden" id="fileNameDisplay<%=rnd%>" value="">
                                    
                                    <button type="button" class="btn btn-sm btn-outline-primary fw-bold rounded-pill shadow-sm px-4" onclick="document.getElementById('input_dofile_foto_<%=rnd%>').click();">
                                        <i class="fas fa-camera me-2"></i><%=Common.getBahasaConfig("Ganti Gambar")%>
                                    </button>
                                </div>

                                <div id="progress-wrapper-<%=rnd%>" class="mt-2" style="display: none; max-width: 300px; margin: 0 auto;">
                                    <div class="progress" style="height: 15px;">
                                        <div id="progress-bar-<%=rnd%>" class="progress-bar bg-primary progress-bar-striped progress-bar-animated fw-bold" role="progressbar" style="width: 0%; font-size: 10px;">0%</div>
                                    </div>
                                </div>
                                <small class="text-muted mt-2 d-block fst-italic"><%=Common.getBahasaConfig("Rekomendasi ukuran 1:1 (Kotak). Format JPG/PNG.")%></small>
                            </div>
                        </div>

                        <div class="col-md-3">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Kode Produk")%></label>
                            <input type="text" class="form-control fw-semibold" id="formKode<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Otomatis jika kosong")%>">
                        </div>
                        <div class="col-md-3">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("UPC/Barcode")%></label>
                            <input type="text" class="form-control" id="formBarcode<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Opsional")%>">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Produk")%> <span class="text-danger">*</span></label>
                            <input type="text" class="form-control fw-semibold" id="formNama<%=rnd%>" required placeholder="<%=Common.getBahasaConfig("Masukkan nama barang...")%>">
                        </div>

                        <div class="col-md-6">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jenis Produk")%> <span class="text-danger">*</span></label>
                            <select class="form-select" id="formJenis<%=rnd%>" required>
                                <option value=""><%=Common.getBahasaConfig("Memuat...")%></option>
                            </select>
                        </div>

                        <% if (isAdmin) { %>
                        <div class="col-md-6">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Toko / Pedagang")%> <span class="text-danger">*</span></label>
                            <select class="form-select" id="formToko<%=rnd%>" required>
                                <option value=""><%=Common.getBahasaConfig("Memuat...")%></option>
                            </select>
                        </div>
                        <% } else { %>
                            <input type="hidden" id="formToko<%=rnd%>" value="<%=idTokoAktif%>">
                        <% } %>

                        <div class="col-md-4">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Harga Modal/Beli")%></label>
                            <div class="input-group">
                                <span class="input-group-text bg-light border-end-0">Rp</span>
                                <input type="number" class="form-control border-start-0" id="formHargaBeli<%=rnd%>" placeholder="0" min="0">
                            </div>
                        </div>

                        <div class="col-md-4">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Harga Jual")%> <span class="text-danger">*</span></label>
                            <div class="input-group">
                                <span class="input-group-text bg-light border-end-0">Rp</span>
                                <input type="number" class="form-control border-start-0 text-success fw-bold" id="formHargaJual<%=rnd%>" required placeholder="0" min="0">
                            </div>
                            <% if (!isAdmin && !pedagangBolehMengubahHarga) { %>
                                <small class="text-danger mt-1 d-block" style="font-size: 10px;"><i class="fas fa-lock me-1"></i>Harga ditentukan oleh Admin.</small>
                            <% } %>
                        </div>

                        <div class="col-md-4">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Stok Awal")%></label>
                            <input type="number" class="form-control" id="formStok<%=rnd%>" placeholder="0" step="0.01">
                        </div>

                        <div class="col-md-6">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jenis Item")%></label>
                            <select class="form-select" id="formJenisItem<%=rnd%>">
                                <option value="JUAL"><%=Common.getBahasaConfig("Produk (bisa dijual di Kasir)")%></option>
                                <option value="BAHAN"><%=Common.getBahasaConfig("Bahan Baku (hanya dipakai via resep)")%></option>
                                <option value="EKSTRA"><%=Common.getBahasaConfig("Ekstra (hanya dipakai via picker ekstra produk lain)")%></option>
                            </select>
                            <small class="text-muted d-block mt-1"><%=Common.getBahasaConfig("Bahan Baku & Ekstra tidak muncul di katalog Kasir. Bahan Baku hanya bisa dipilih di editor Resep, Ekstra hanya bisa dipilih di editor Pilih Ekstra produk lain.")%></small>
                        </div>

                        <div class="col-md-6">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Aturan Jual Saat Stok Kurang")%></label>
                            <select class="form-select" id="formIzinkanJualMinusStok<%=rnd%>">
                                <option value=""><%=Common.getBahasaConfig("Ikut Pengaturan Toko (default)")%></option>
                                <option value="true"><%=Common.getBahasaConfig("Selalu Boleh Dijual Walau Stok Minus")%></option>
                                <option value="false"><%=Common.getBahasaConfig("Wajib Diblokir Jika Stok Tidak Cukup")%></option>
                            </select>
                        </div>

                        <div class="col-12">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Keterangan Tambahan")%></label>
                            <textarea class="form-control" id="formKeterangan<%=rnd%>" rows="2" placeholder="<%=Common.getBahasaConfig("Deskripsi singkat...")%>"></textarea>
                        </div>

                        <div class="col-12">
                            <label class="form-label small fw-bold text-secondary"><i class="fas fa-link me-1"></i><%=Common.getBahasaConfig("Barang Persediaan (Aset)")%> <span class="text-muted fw-normal">(<%=Common.getBahasaConfig("opsional")%>)</span></label>
                            <select class="form-select" id="formMasterAsset<%=rnd%>" onchange="updateRekonHint<%=rnd%>()">
                                <option value=""><%=Common.getBahasaConfig("-- Tidak ditautkan --")%></option>
                            </select>
                            <small class="text-muted d-block mt-1" id="rekonAsetForm<%=rnd%>"></small>
                        </div>

                        <!-- ============ BAHAN BAKU (RESEP) & HPP ============ -->
                        <div class="col-12 mt-2">
                            <div class="p-3 rounded-3 border" style="background:#f8fafc;">
                                <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-1">
                                    <label class="form-label fw-bold text-dark mb-0">
                                        <i class="fas fa-mortar-pestle me-2 text-primary"></i><%=Common.getBahasaConfig("Bahan Baku (Resep) & HPP")%>
                                    </label>
                                    <span class="badge bg-primary border" id="bbHppBadge<%=rnd%>"><%=Common.getBahasaConfig("HPP")%>: Rp 0</span>
                                </div>
                                <p class="text-muted small mb-2"><%=Common.getBahasaConfig("Susun produk ini dari bahan baku (produk lain). HPP (modal) dihitung otomatis dari total bahan baku yang diperlukan.")%></p>
                                <div class="row g-2 align-items-end mb-2">
                                    <div class="col-md-6">
                                        <label class="form-label small fw-semibold text-secondary mb-1"><%=Common.getBahasaConfig("Bahan Baku (produk lain)")%></label>
                                        <select class="form-select form-select-sm" id="bbSelectProduk<%=rnd%>"><option value=""><%=Common.getBahasaConfig("Memuat...")%></option></select>
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary mb-1"><%=Common.getBahasaConfig("Qty / Jumlah")%></label>
                                        <input type="number" class="form-control form-control-sm" id="bbQty<%=rnd%>" value="1" min="0" step="any">
                                    </div>
                                    <div class="col-md-3">
                                        <button type="button" class="btn btn-sm btn-primary w-100 fw-bold rounded-3" onclick="bbAdd<%=rnd%>()"><i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Tambah")%></button>
                                    </div>
                                </div>
                                <div class="table-responsive">
                                    <table class="table table-sm align-middle mb-1">
                                        <thead><tr class="small text-muted text-uppercase">
                                            <th><%=Common.getBahasaConfig("Bahan Baku")%></th>
                                            <th class="text-center" style="width:80px;"><%=Common.getBahasaConfig("Qty")%></th>
                                            <th class="text-end" style="width:120px;"><%=Common.getBahasaConfig("Harga")%></th>
                                            <th class="text-end" style="width:130px;"><%=Common.getBahasaConfig("Subtotal")%></th>
                                            <th style="width:38px;"></th>
                                        </tr></thead>
                                        <tbody id="bbList<%=rnd%>"></tbody>
                                        <tfoot><tr class="fw-bold border-top">
                                            <td colspan="3" class="text-end"><%=Common.getBahasaConfig("Total HPP")%></td>
                                            <td class="text-end text-primary" id="bbTotalHpp<%=rnd%>">Rp 0</td>
                                            <td></td>
                                        </tr></tfoot>
                                    </table>
                                </div>
                                <div class="text-end">
                                    <button type="button" class="btn btn-sm btn-outline-success rounded-pill fw-bold" onclick="bbSetAsHpp<%=rnd%>()"><i class="fas fa-equals me-1"></i><%=Common.getBahasaConfig("Jadikan Harga Modal/Beli")%></button>
                                </div>
                            </div>
                        </div>

                        <!-- ============ PILIH EKSTRA (MODIFIER/ADD-ON) ============ -->
                        <div class="col-12 mt-2">
                            <div class="p-3 rounded-3 border" style="background:#f8fafc;">
                                <label class="form-label fw-bold text-dark mb-1">
                                    <i class="fas fa-mug-hot me-2 text-primary"></i><%=Common.getBahasaConfig("Pilih Ekstra")%>
                                </label>
                                <p class="text-muted small mb-2"><%=Common.getBahasaConfig("Produk lain ber-Jenis Item \"Ekstra\" yang boleh dipilih pembeli sebagai tambahan/topping saat memesan produk ini (mis. Ekstra Topping Mesis, Ekstra Cruble Oreo).")%></p>
                                <div class="row g-2 align-items-end mb-2">
                                    <div class="col-md-9">
                                        <label class="form-label small fw-semibold text-secondary mb-1"><%=Common.getBahasaConfig("Ekstra (produk lain)")%></label>
                                        <select class="form-select form-select-sm" id="eksSelectProduk<%=rnd%>"><option value=""><%=Common.getBahasaConfig("Memuat...")%></option></select>
                                    </div>
                                    <div class="col-md-3">
                                        <button type="button" class="btn btn-sm btn-primary w-100 fw-bold rounded-3" onclick="eksAdd<%=rnd%>()"><i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Tambah")%></button>
                                    </div>
                                </div>
                                <div id="eksList<%=rnd%>"></div>
                            </div>
                        </div>

                        <div class="col-12 mt-3">
                            <div class="form-check form-switch bg-light p-3 rounded-3 border">
                                <input class="form-check-input ms-0 me-2" type="checkbox" id="formAktif<%=rnd%>" <%= isAdmin ? "" : "disabled" %>>
                                <label class="form-check-label fw-bold text-dark" for="formAktif<%=rnd%>">
                                    <%=Common.getBahasaConfig("Produk Aktif (Dapat dijual di Koperasi Online)")%>
                                    <% if (!isAdmin) { %>
                                        <br><small class="text-danger fw-normal" style="font-size: 11px;"><i class="fas fa-info-circle me-1"></i><%=Common.getBahasaConfig("Hanya Admin Koperasi yang dapat mengaktifkan atau menonaktifkan status produk.")%></small>
                                    <% } %>
                                </label>
                            </div>
                        </div>
                    </div>

                    <div class="mt-4 text-end border-top pt-3">
                        <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
                        <button type="submit" class="btn btn-primary px-4 rounded-pill fw-bold shadow-sm" id="btnSaveProduk<%=rnd%>">
                            <i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Produk")%>
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="modalUploadExcel<%=rnd%>" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 rounded-4 shadow-lg">
            <div class="modal-header bg-light border-bottom-0 pb-3">
                <h5 class="modal-title fw-bold text-dark">
                    <i class="fas fa-file-upload text-warning me-2"></i><%=Common.getBahasaConfig("Unggah Data Produk")%>
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4 pt-2 text-center">
                <p class="text-muted small mb-3"><%=Common.getBahasaConfig("Format Excel: \"Daftar Barang dan Jasa\" (Accurate) -- sama dgn format file Unduh di sini, atau file ekspor Accurate asli tanpa perlu diedit dulu. Setelah diproses, baris akan ditinjau dulu sebelum disimpan.")%></p>

                <input class="form-control mb-3" type="file" id="fileUploadExcel<%=rnd%>" accept=".xls,.xlsx">

                <div id="uploadStatusBox<%=rnd%>" class="alert alert-info py-2" style="display: none;">
                    </div>

                <div class="mt-4">
                    <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
                    <button type="button" class="btn btn-warning text-dark px-4 rounded-pill fw-bold shadow-sm" id="btnProsesUpload<%=rnd%>" onclick="prosesUploadExcel<%=rnd%>()">
                        <i class="fas fa-cogs me-2"></i><%=Common.getBahasaConfig("Proses & Tinjau")%>
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="modalTinjauImporExcel<%=rnd%>" data-bs-backdrop="static" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-xl">
        <div class="modal-content border-0 rounded-4 shadow-lg">
            <div class="modal-header bg-light border-bottom-0 pb-3">
                <h5 class="modal-title fw-bold text-dark">
                    <i class="fas fa-clipboard-check text-primary me-2"></i><%=Common.getBahasaConfig("Tinjau Sebelum Simpan")%>
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4 pt-2">
                <div id="tinjauPeringatanBox<%=rnd%>" class="alert alert-warning py-2 small d-none"></div>
                <p class="text-muted small mb-2" id="tinjauRingkasan<%=rnd%>"></p>
                <div class="table-responsive" style="max-height: 55vh; overflow-y: auto;">
                    <table class="table table-hover table-sm align-middle mb-0">
                        <thead class="table-dark text-center" style="position: sticky; top: 0; z-index: 1;">
                            <tr>
                                <th>#</th>
                                <th class="text-start">Kode</th>
                                <th class="text-start">Nama Barang</th>
                                <th class="text-start">Kategori</th>
                                <th>Stok Lama &rarr; Baru</th>
                                <th>Harga Jual</th>
                                <th>Harga Beli</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody id="tinjauTabelBody<%=rnd%>"></tbody>
                    </table>
                </div>
                <div id="tinjauStatusBox<%=rnd%>" class="alert alert-info py-2 mt-3 d-none"></div>
                <div class="mt-4 text-end">
                    <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
                    <button type="button" class="btn btn-primary px-4 rounded-pill fw-bold shadow-sm" id="btnKomitImporExcel<%=rnd%>" onclick="komitImporExcel<%=rnd%>()">
                        <i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Semua")%>
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
    const isAdminProduk<%=rnd%> = <%=isAdmin%>;
    const pedagangBolehUbahHarga<%=rnd%> = <%=pedagangBolehMengubahHarga%>;
    const idTokoLogin<%=rnd%> = '<%=idTokoAktif%>';
    let modalInstanceProduk<%=rnd%> = null;
    let modalInstanceUpload<%=rnd%> = null;

    let currentPageProduk<%=rnd%> = 1;
    const limitPerPageProduk<%=rnd%> = 20;
    let totalRecordsProduk<%=rnd%> = 0;
    let filterJenisItemAktif<%=rnd%> = ''; // '' = semua, 'JUAL' = produk saja, 'BAHAN' = bahan baku saja

    const setFilterJenisItem<%=rnd%> = (val) => {
        filterJenisItemAktif<%=rnd%> = val;
        ['Semua', 'Jual', 'Bahan'].forEach(k => {
            document.getElementById('btnFilterJenisItem' + k + '<%=rnd%>').classList.remove('active');
        });
        const key = val === '' ? 'Semua' : (val === 'JUAL' ? 'Jual' : 'Bahan');
        document.getElementById('btnFilterJenisItem' + key + '<%=rnd%>').classList.add('active');
        triggerSearchProduk<%=rnd%>();
    };
    
    // Variabel untuk menyimpan ID produk yang sedang diedit (untuk upload foto)
    let activeEditingProdukId<%=rnd%> = "";

    // Bahan baku (resep) & HPP
    let bahanBakuList<%=rnd%> = [];      // [{produk, nama, qty, harga}]
    let bahanBakuOptions<%=rnd%> = [];   // kandidat bahan baku (produk lain)

    // Pilih Ekstra (modifier/add-on) -- lebih sederhana dari Bahan Baku: hanya id mentah (tanpa
    // qty/harga snapshot), karena harga ekstra SELALU diambil live dari hargaJual produk ekstra
    // itu sendiri saat checkout (lihat JavaDoc Produk.getEkstraPilihan()).
    let ekstraPilihanList<%=rnd%> = [];  // [id, id, ...] id produk ber-jenisItem EKSTRA
    let ekstraPilihanOptions<%=rnd%> = []; // kandidat ekstra (produk lain ber-jenisItem EKSTRA)

    const formatRpProduk<%=rnd%> = (angka) => {
        return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);
    };

    const fetchSqlData<%=rnd%> = async (sql) => {
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                log: "true",
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sql: sql, action: "sql" })
            });
            const result = await res.json();
            return result.data || [];
        } catch (e) { 
            console.error("Fetch Error: ", e); 
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
    // INIT COMBO BOX (MASTER DATA)
    // ==========================================
    const loadMasterDataCombo<%=rnd%> = async () => {
        const sqlJenis = "SELECT id, nama FROM koperasi.jenis_produk WHERE aktif = true ORDER BY nama ASC";
        fetchSqlData<%=rnd%>(sqlJenis).then(res => {
            let optFilter = '<option value=""><%=Common.getBahasaConfig("- Semua Jenis -")%></option>';
            let optForm = '<option value=""><%=Common.getBahasaConfig("-- Pilih Jenis --")%></option>';
            res.forEach(item => {
                const opt = '<option value="' + item.id + '">' + item.nama + '</option>';
                optFilter += opt;
                optForm += opt;
            });
            document.getElementById('filterJenis<%=rnd%>').innerHTML = optFilter;
            document.getElementById('formJenis<%=rnd%>').innerHTML = optForm;
        });

        if (isAdminProduk<%=rnd%>) {
            const sqlToko = "SELECT id, nama FROM koperasi.toko WHERE aktif = true ORDER BY nama ASC";
            fetchSqlData<%=rnd%>(sqlToko).then(res => {
                let optFilter = '<option value=""><%=Common.getBahasaConfig("- Semua Toko -")%></option>';
                let optForm = '<option value=""><%=Common.getBahasaConfig("-- Pilih Toko --")%></option>';
                res.forEach(item => {
                    const opt = '<option value="' + item.id + '">' + item.nama + '</option>';
                    optFilter += opt;
                    optForm += opt;
                });
                document.getElementById('filterToko<%=rnd%>').innerHTML = optFilter;
                document.getElementById('formToko<%=rnd%>').innerHTML = optForm;
            });
        }
    };


    // ==========================================
    // BAHAN BAKU (RESEP) & HPP
    // ==========================================
    const loadBahanBakuOptions<%=rnd%> = async () => {
        // Gap-closure "Jenis Item" -- picker resep HANYA menampilkan item yg ditandai Bahan Baku,
        // bukan seluruh katalog produk jual seperti sebelumnya.
        let sql = "SELECT id, kode, nama, COALESCE(hargabeli,0) AS hargabeli FROM koperasi.produk WHERE jenis_item = 'BAHAN' ";
        if (!isAdminProduk<%=rnd%>) sql += " AND toko = " + idTokoLogin<%=rnd%> + " ";
        sql += " ORDER BY nama ASC LIMIT 2000";
        bahanBakuOptions<%=rnd%> = await fetchSqlData<%=rnd%>(sql);
        bbRenderOptions<%=rnd%>();
    };

    const bbRenderOptions<%=rnd%> = () => {
        const sel = document.getElementById('bbSelectProduk<%=rnd%>');
        if (!sel) return;
        const curId = (document.getElementById('formId<%=rnd%>').value || '').toString();
        let opt = '<option value=""><%=Common.getBahasaConfig("-- Pilih bahan baku --")%></option>';
        bahanBakuOptions<%=rnd%>.forEach(p => {
            if (curId !== '' && p.id.toString() === curId) return; // tidak boleh jadi bahan baku dirinya sendiri
            opt += '<option value="' + p.id + '">' + (p.kode ? p.kode + ' - ' : '') + p.nama + ' (' + formatRpProduk<%=rnd%>(p.hargabeli) + ')</option>';
        });
        sel.innerHTML = opt;
    };

    const bbHitungTotal<%=rnd%> = () => bahanBakuList<%=rnd%>.reduce((s, it) => s + (parseFloat(it.qty)||0) * (parseFloat(it.harga)||0), 0);

    const bbRender<%=rnd%> = () => {
        const tb = document.getElementById('bbList<%=rnd%>');
        if (!tb) return;
        if (bahanBakuList<%=rnd%>.length === 0) {
            tb.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-2 small"><%=Common.getBahasaConfig("Belum ada bahan baku. Produk dijual tanpa resep.")%></td></tr>';
        } else {
            let html = '';
            bahanBakuList<%=rnd%>.forEach((it, i) => {
                const sub = (parseFloat(it.qty)||0) * (parseFloat(it.harga)||0);
                html += '<tr>' +
                    '<td class="fw-semibold">' + (it.nama || '-') + '</td>' +
                    '<td class="text-center">' + it.qty + '</td>' +
                    '<td class="text-end">' + formatRpProduk<%=rnd%>(it.harga) + '</td>' +
                    '<td class="text-end fw-bold">' + formatRpProduk<%=rnd%>(sub) + '</td>' +
                    '<td class="text-center"><button type="button" class="btn btn-sm btn-outline-danger border-0 p-1" onclick="bbRemove<%=rnd%>(' + i + ')"><i class="fas fa-times"></i></button></td>' +
                '</tr>';
            });
            tb.innerHTML = html;
        }
        const total = bbHitungTotal<%=rnd%>();
        const elTot = document.getElementById('bbTotalHpp<%=rnd%>');
        if (elTot) elTot.innerText = formatRpProduk<%=rnd%>(total);
        const badge = document.getElementById('bbHppBadge<%=rnd%>');
        if (badge) badge.innerHTML = '<%=Common.getBahasaConfigJS("HPP")%>: ' + formatRpProduk<%=rnd%>(total);
    };

    const bbAdd<%=rnd%> = () => {
        const sel = document.getElementById('bbSelectProduk<%=rnd%>');
        const pid = sel.value;
        if (pid === '') { showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Pilih bahan baku terlebih dahulu.")%>', 'bg-warning text-dark'); return; }
        const qty = parseFloat(document.getElementById('bbQty<%=rnd%>').value) || 0;
        if (qty <= 0) { showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Qty harus lebih dari 0.")%>', 'bg-warning text-dark'); return; }
        const p = bahanBakuOptions<%=rnd%>.find(x => x.id.toString() === pid.toString());
        if (!p) return;
        const existing = bahanBakuList<%=rnd%>.find(x => x.produk.toString() === pid.toString());
        if (existing) { existing.qty = (parseFloat(existing.qty)||0) + qty; }
        else { bahanBakuList<%=rnd%>.push({ produk: parseInt(pid), nama: p.nama, qty: qty, harga: parseFloat(p.hargabeli) || 0 }); }
        document.getElementById('bbQty<%=rnd%>').value = '1';
        bbRender<%=rnd%>();
    };

    const bbRemove<%=rnd%> = (idx) => { bahanBakuList<%=rnd%>.splice(idx, 1); bbRender<%=rnd%>(); };

    const bbSetAsHpp<%=rnd%> = () => {
        const total = bbHitungTotal<%=rnd%>();
        if (total <= 0) { showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Belum ada bahan baku untuk dihitung.")%>', 'bg-warning text-dark'); return; }
        const el = document.getElementById('formHargaBeli<%=rnd%>');
        if (el.disabled) { showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Anda tidak memiliki hak mengubah harga.")%>', 'bg-warning text-dark'); return; }
        el.value = Math.round(total);
        showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Harga Modal/Beli diisi dari HPP bahan baku.")%>', 'bg-success text-white');
    };

    const bbLoadForEdit<%=rnd%> = async (id) => {
        bahanBakuList<%=rnd%> = [];
        try {
            const res = await fetchSqlData<%=rnd%>("SELECT bahanbaku, master_asset FROM koperasi.produk WHERE id = " + id);
            if (res && res[0]) {
                if (res[0].bahanbaku) {
                    const arr = JSON.parse(res[0].bahanbaku);
                    if (Array.isArray(arr)) bahanBakuList<%=rnd%> = arr.map(x => ({ produk: x.produk, nama: x.nama, qty: parseFloat(x.qty)||0, harga: parseFloat(x.harga)||0 }));
                }
                const selMa = document.getElementById('formMasterAsset<%=rnd%>');
                if (selMa) { selMa.value = res[0].master_asset || ''; updateRekonHint<%=rnd%>(); }
            }
        } catch (e) { console.error("bahanBaku/masterAsset parse error", e); }
        bbRenderOptions<%=rnd%>();
        bbRender<%=rnd%>();
    };


    // ==========================================
    // PILIH EKSTRA (MODIFIER/ADD-ON)
    // ==========================================
    const loadEkstraOptions<%=rnd%> = async () => {
        // Padanan loadBahanBakuOptions, tapi sumbernya produk ber-jenisItem EKSTRA (bukan BAHAN) --
        // dan harga yg ditampilkan hargajual (harga JUAL yg dibayar pembeli), BUKAN hargabeli spt
        // bahan baku (yg dicost pakai hargabeli krn itu utk hitung HPP).
        let sql = "SELECT id, kode, nama, COALESCE(hargajual,0) AS hargajual FROM koperasi.produk WHERE jenis_item = 'EKSTRA' ";
        if (!isAdminProduk<%=rnd%>) sql += " AND toko = " + idTokoLogin<%=rnd%> + " ";
        sql += " ORDER BY nama ASC LIMIT 2000";
        ekstraPilihanOptions<%=rnd%> = await fetchSqlData<%=rnd%>(sql);
        eksRenderOptions<%=rnd%>();
    };

    const eksRenderOptions<%=rnd%> = () => {
        const sel = document.getElementById('eksSelectProduk<%=rnd%>');
        if (!sel) return;
        const curId = (document.getElementById('formId<%=rnd%>').value || '').toString();
        let opt = '<option value=""><%=Common.getBahasaConfig("-- Pilih ekstra --")%></option>';
        ekstraPilihanOptions<%=rnd%>.forEach(p => {
            if (curId !== '' && p.id.toString() === curId) return; // tidak boleh jadi ekstra dirinya sendiri
            if (ekstraPilihanList<%=rnd%>.indexOf(parseInt(p.id)) !== -1) return; // sudah dipilih, jangan tampil lagi
            opt += '<option value="' + p.id + '">' + (p.kode ? p.kode + ' - ' : '') + p.nama + ' (' + formatRpProduk<%=rnd%>(p.hargajual) + ')</option>';
        });
        sel.innerHTML = opt;
    };

    const eksRender<%=rnd%> = () => {
        const wrap = document.getElementById('eksList<%=rnd%>');
        if (!wrap) return;
        if (ekstraPilihanList<%=rnd%>.length === 0) {
            wrap.innerHTML = '<div class="text-muted small py-1"><%=Common.getBahasaConfig("Belum ada ekstra dipilih. Produk dijual tanpa opsi tambahan.")%></div>';
            return;
        }
        let html = '<div class="list-group list-group-flush">';
        ekstraPilihanList<%=rnd%>.forEach((id, i) => {
            const p = ekstraPilihanOptions<%=rnd%>.find(x => x.id.toString() === id.toString());
            const nama = p ? p.nama : ('#' + id);
            const harga = p ? formatRpProduk<%=rnd%>(p.hargajual) : '';
            html += '<div class="list-group-item d-flex justify-content-between align-items-center px-0 py-1 bg-transparent">' +
                        '<span class="small"><i class="fas fa-mug-hot text-muted me-1"></i>' + nama + (harga ? ' <span class="text-muted">(' + harga + ')</span>' : '') + '</span>' +
                        '<button type="button" class="btn btn-sm btn-outline-danger border-0 p-1" onclick="eksRemove<%=rnd%>(' + i + ')"><i class="fas fa-times"></i></button>' +
                    '</div>';
        });
        html += '</div>';
        wrap.innerHTML = html;
    };

    const eksAdd<%=rnd%> = () => {
        const sel = document.getElementById('eksSelectProduk<%=rnd%>');
        const pid = sel.value;
        if (pid === '') { showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Pilih ekstra terlebih dahulu.")%>', 'bg-warning text-dark'); return; }
        const idInt = parseInt(pid);
        if (ekstraPilihanList<%=rnd%>.indexOf(idInt) === -1) { ekstraPilihanList<%=rnd%>.push(idInt); }
        sel.value = '';
        eksRender<%=rnd%>();
        eksRenderOptions<%=rnd%>();
    };

    const eksRemove<%=rnd%> = (idx) => {
        ekstraPilihanList<%=rnd%>.splice(idx, 1);
        eksRender<%=rnd%>();
        eksRenderOptions<%=rnd%>();
    };

    const eksLoadForEdit<%=rnd%> = async (id) => {
        ekstraPilihanList<%=rnd%> = [];
        try {
            const res = await fetchSqlData<%=rnd%>("SELECT ekstra_pilihan FROM koperasi.produk WHERE id = " + id);
            if (res && res[0] && res[0].ekstra_pilihan) {
                const arr = JSON.parse(res[0].ekstra_pilihan);
                if (Array.isArray(arr)) ekstraPilihanList<%=rnd%> = arr.map(x => parseInt(x));
            }
        } catch (e) { console.error("ekstraPilihan parse error", e); }
        eksRenderOptions<%=rnd%>();
        eksRender<%=rnd%>();
    };


    // ==========================================
    // TAUTAN BARANG PERSEDIAAN (ASET) + REKONSILIASI
    // ==========================================
    const loadMasterAssetOptions<%=rnd%> = async () => {
        const sel = document.getElementById('formMasterAsset<%=rnd%>');
        if (!sel) return;
        const res = await fetchSqlData<%=rnd%>("SELECT id, kode, nama FROM asset.master_asset WHERE tipe = 'Barang habis pakai' ORDER BY nama ASC LIMIT 3000");
        let opt = '<option value=""><%=Common.getBahasaConfig("-- Tidak ditautkan --")%></option>';
        (res || []).forEach(p => { opt += '<option value="' + p.id + '">' + (p.kode ? p.kode + ' - ' : '') + (p.nama || '') + '</option>'; });
        sel.innerHTML = opt;
    };

    const updateRekonHint<%=rnd%> = async () => {
        const el = document.getElementById('rekonAsetForm<%=rnd%>');
        if (!el) return;
        const ma = document.getElementById('formMasterAsset<%=rnd%>').value;
        if (!ma) { el.innerHTML = ''; return; }
        const res = await fetchSqlData<%=rnd%>("SELECT COALESCE(SUM((a.qty+a.qtybonus)*b.jenis),0) AS stok FROM asset.detail_transaksi_asset a INNER JOIN library.kode_transaksi b ON a.kode_transaksi = b.id WHERE a.master_asset = " + ma);
        const stokAset = (res && res[0]) ? (parseFloat(res[0].stok) || 0) : 0;
        const stokKantin = parseFloat(document.getElementById('formStok<%=rnd%>').value) || 0;
        const selisih = stokKantin - stokAset;
        const cls = Math.abs(selisih) > 0.001 ? 'text-danger' : 'text-success';
        el.innerHTML = '<i class="fas fa-balance-scale me-1"></i><%=Common.getBahasaConfig("Stok Aset")%>: <b>' + stokAset + '</b> &middot; <%=Common.getBahasaConfig("Stok Kantin")%>: <b>' + stokKantin + '</b> &middot; <%=Common.getBahasaConfig("Selisih")%>: <b class="' + cls + '">' + selisih + '</b>';
    };


    // ==========================================
    // READ (TAMPILKAN TABEL)
    // ==========================================
    const buildFiltersProduk<%=rnd%> = () => {
        const kode = document.getElementById('filterKode<%=rnd%>').value.trim().replace(/'/g, "''");
        const nama = document.getElementById('filterNama<%=rnd%>').value.trim().replace(/'/g, "''");
        const jenis = document.getElementById('filterJenis<%=rnd%>').value;
        const toko = document.getElementById('filterToko<%=rnd%>').value;

        let filters = " WHERE 1=1 ";
        
        if (kode !== "") filters += " AND (a.kode ILIKE '%" + kode + "%' OR a.barcode ILIKE '%" + kode + "%') ";
        if (nama !== "") filters += " AND a.nama ILIKE '%" + nama + "%' ";
        if (jenis !== "") filters += " AND a.jenis_produk = " + jenis + " ";
        // Gap-closure "Jenis Item" (Produk vs Bahan Baku) -- "<>" polos TIDAK match NULL di Postgres
        // (produk lama sebelum kolom ini ada), jadi WAJIB pola OR IS NULL utk filter "JUAL".
        if (filterJenisItemAktif<%=rnd%> === 'JUAL') filters += " AND (a.jenis_item IS NULL OR a.jenis_item <> 'BAHAN') ";
        else if (filterJenisItemAktif<%=rnd%> === 'BAHAN') filters += " AND a.jenis_item = 'BAHAN' ";

        if (!isAdminProduk<%=rnd%>) {
            filters += " AND a.toko = " + idTokoLogin<%=rnd%> + " ";
        } else if (toko !== "") {
            filters += " AND a.toko = " + toko + " ";
        }

        return filters;
    };

    const triggerSearchProduk<%=rnd%> = () => {
        currentPageProduk<%=rnd%> = 1;
        loadDataProdukAPI<%=rnd%>();
    };

    const loadDataProdukAPI<%=rnd%> = async () => {
        const tbody = document.getElementById('tableProduk<%=rnd%>');
        const colSpan = isAdminProduk<%=rnd%> ? 11 : 10; // Tambah 1 untuk kolom Gambar
        
        tbody.innerHTML = '<tr><td colspan="' + colSpan + '" class="text-center py-5 text-muted"><div class="spinner-border text-primary mb-3"></div><br><%=Common.getBahasaConfig("Memuat data produk...")%></td></tr>';

        const offset = (currentPageProduk<%=rnd%> - 1) * limitPerPageProduk<%=rnd%>;
        const sqlFilters = buildFiltersProduk<%=rnd%>();

        const sqlQueryData = "SELECT a.id, a.kode, a.barcode, a.nama, a.hargabeli, a.hargajual, a.stok, a.aktif, a.keterangan, " +
                             "a.izinkan_jual_minus_stok, a.jenis_item, " +
                             "b.id AS id_toko, b.nama AS nama_toko, " +
                             "c.id AS id_jenis, c.nama AS nama_jenis " +
                             "FROM koperasi.produk a " +
                             "INNER JOIN koperasi.toko b ON (a.toko = b.id and b.aktif) " +
                             "LEFT JOIN koperasi.jenis_produk c ON a.jenis_produk = c.id " +
                             sqlFilters +
                             " ORDER BY a.id DESC LIMIT " + limitPerPageProduk<%=rnd%> + " OFFSET " + offset + ";";

        const sqlQueryCount = "SELECT COUNT(*) AS jumlah FROM koperasi.produk a INNER JOIN koperasi.toko b ON (a.toko = b.id and b.aktif) " + sqlFilters + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetchSqlData<%=rnd%>(sqlQueryCount),
                fetchSqlData<%=rnd%>(sqlQueryData)
            ]);

            totalRecordsProduk<%=rnd%> = (resCount && resCount[0]) ? parseInt(resCount[0].jumlah || 0) : 0;
            const recordList = resData || [];

            let htmlTbody = '';
            if (recordList.length === 0) {
                htmlTbody = '<tr><td colspan="' + colSpan + '" class="text-center text-muted py-5"><i class="fas fa-box-open fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Belum ada data produk.")%></td></tr>';
            } else {
                let no = offset + 1;
                recordList.forEach(row => {
                    const isAktif = row.aktif === 'true' || row.aktif === true;
                    const badgeAktif = isAktif 
                        ? '<span class="badge bg-success rounded-pill px-3 shadow-sm"><i class="fas fa-check me-1"></i>Aktif</span>' 
                        : '<span class="badge bg-secondary rounded-pill px-3 shadow-sm"><i class="fas fa-times me-1"></i>Non-Aktif</span>';
                    
                    const stok = parseFloat(row.stok || 0);
                    const classStok = stok <= 0 ? 'text-danger fw-bold' : 'fw-semibold';

                    const safeName = (row.nama || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    const safeKet = (row.keterangan || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    
                    // Bangun URL Gambar
                    const imgUrl = "<%=Common.ROOT%>/Data?action=file&class=<%=ais.database.model.file.LampiranLain.class.getName()%>&ref=" + row.id + "&jenis=<%=URLEncoder.encode(Produk.class.getName(), "UTF-8")%>&render=true&rand="+(++variable);

                    htmlTbody += '<tr>';
                    htmlTbody += '<td class="text-center text-muted">' + no++ + '</td>';
                    
                    // Kolom Gambar Baru
                    htmlTbody += '<td class="text-center">' +
                                 '<img src="' + imgUrl + '" onerror="this.onerror=null; this.src=\'<%=Common.ROOT%>/img/empty_image.png\';" class="rounded-3 shadow-sm object-fit-cover border border-light" style="width: 45px; height: 45px; background-color: #f8f9fa;" alt="Img">' +
                                 '</td>';
                                 
                    htmlTbody += '<td class="fw-bold">' + (row.kode || '-') + '</td>';
                    htmlTbody += '<td class="fw-bold text-dark">' + (row.nama || '-') + '</td>';
                    const jenisItemRow = row.jenis_item || 'JUAL';
                    const badgeJenisItem = jenisItemRow === 'BAHAN'
                        ? '<span class="badge bg-warning text-dark rounded-pill">' + '<%=Common.getBahasaConfigJS("Bahan")%>' + '</span>'
                        : '<span class="badge bg-info text-dark rounded-pill">' + '<%=Common.getBahasaConfigJS("Produk")%>' + '</span>';
                    htmlTbody += '<td>' + (row.nama_jenis || '-') + '<br>' + badgeJenisItem + '</td>';
                    
                    if (isAdminProduk<%=rnd%>) {
                        htmlTbody += '<td><span class="text-muted">' + (row.nama_toko || '-') + '</span></td>';
                    }
                    
                    htmlTbody += '<td class="text-end text-muted">' + formatRpProduk<%=rnd%>(row.hargabeli) + '</td>';
                    htmlTbody += '<td class="text-end fw-bold text-success">' + formatRpProduk<%=rnd%>(row.hargajual) + '</td>';
                    htmlTbody += '<td class="text-center ' + classStok + '">' + stok + '</td>';
                    htmlTbody += '<td class="text-center">' + badgeAktif + '</td>';
                    
                    const izinkanMinusJs = (row.izinkan_jual_minus_stok === true) ? 'true' : ((row.izinkan_jual_minus_stok === false) ? 'false' : 'null');

                    const safeBarcode = (row.barcode || '').replace(/'/g, "\\'");
                    htmlTbody += '<td class="text-center text-nowrap">' +
                                    '<button class="btn btn-sm btn-outline-warning text-dark shadow-sm me-1" onclick="showEditModal<%=rnd%>(' + row.id + ', \'' + (row.kode || '') + '\', \'' + safeName + '\', ' + (row.id_jenis || 'null') + ', ' + (row.id_toko || 'null') + ', ' + (row.hargabeli || 0) + ', ' + (row.hargajual || 0) + ', ' + stok + ', \'' + safeKet + '\', ' + isAktif + ', ' + izinkanMinusJs + ', \'' + safeBarcode + '\', \'' + jenisItemRow + '\')" title="Edit Produk & Gambar"><i class="fas fa-edit"></i></button>' +
                                    '<button class="btn btn-sm btn-outline-danger shadow-sm" onclick="deleteProduk<%=rnd%>(' + row.id + ')" title="Hapus Produk"><i class="fas fa-trash-alt"></i></button>' +
                                 '</td>';
                    htmlTbody += '</tr>';
                });
            }
            tbody.innerHTML = htmlTbody;
            updatePaginationProduk<%=rnd%>();

        } catch (error) {
        	console.error(error);
            tbody.innerHTML = '<tr><td colspan="' + colSpan + '" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal mengambil data.")%></td></tr>';
        }
    };

    const updatePaginationProduk<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecordsProduk<%=rnd%> / limitPerPageProduk<%=rnd%>) || 1;
        document.getElementById('pageNumberProduk<%=rnd%>').innerText = currentPageProduk<%=rnd%> + " / " + totalPages;
        
        const startIdx = (currentPageProduk<%=rnd%> - 1) * limitPerPageProduk<%=rnd%> + 1;
        const endIdx = Math.min(currentPageProduk<%=rnd%> * limitPerPageProduk<%=rnd%>, totalRecordsProduk<%=rnd%>);
        const startInfo = totalRecordsProduk<%=rnd%> > 0 ? startIdx : 0;
        
        document.getElementById('pagingInfoProduk<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startInfo + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari")%> <b class="text-dark">' + totalRecordsProduk<%=rnd%> + '</b> <%=Common.getBahasaConfig("data")%>';
        
        document.getElementById('btnPrevProduk<%=rnd%>').disabled = (currentPageProduk<%=rnd%> === 1);
        document.getElementById('btnNextProduk<%=rnd%>').disabled = (currentPageProduk<%=rnd%> === totalPages);
    };

    const changePageProduk<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecordsProduk<%=rnd%> / limitPerPageProduk<%=rnd%>);
        if (direction === -1 && currentPageProduk<%=rnd%> > 1) {
            currentPageProduk<%=rnd%>--;
            loadDataProdukAPI<%=rnd%>();
        } else if (direction === 1 && currentPageProduk<%=rnd%> < totalPages) {
            currentPageProduk<%=rnd%>++;
            loadDataProdukAPI<%=rnd%>();
        }
    };


    // ==========================================
    // SINKRONISASI STOK
    // ==========================================
    const sinkronisasiStok<%=rnd%> = async () => {
        const isConfirm = confirm('<%=Common.getBahasaConfigJS("Proses ini akan mengkalkulasi ulang seluruh stok produk berdasarkan rekam jejak pengadaan, opname, dan penjualan. Proses ini mungkin memakan waktu. Lanjutkan?")%>');
        if (!isConfirm) return;

        const btnSync = document.getElementById('btnSyncStok<%=rnd%>');
        const originalBtnHtml = btnSync.innerHTML;

        btnSync.disabled = true;
        btnSync.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyinkronkan...")%>';
        
        try {
            const sqlFilters = buildFiltersProduk<%=rnd%>();
            
            const sqlUpdate = "UPDATE koperasi.produk a "+
            	"SET stok = ( "+
            	"	    COALESCE((SELECT SUM(qty) FROM koperasi.pengadaan_produk pp WHERE pp.produk = a.id), 0) "+
            	"	    + "+
            	"	    COALESCE((SELECT SUM(selisih) FROM koperasi.stok_opname so WHERE so.produk = a.id), 0)"+
            	"	    - "+
            	"	    COALESCE((SELECT SUM(qty) FROM koperasi.pembelian pb WHERE pb.produk = a.id), 0)"+
            	"	) "+sqlFilters;
            
            const payload = {
                action: "update_data",
                sql:sqlUpdate,
                tokoId: isAdminProduk<%=rnd%> ? "" : idTokoLogin<%=rnd%>
            };

            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const result = await response.json();

            if (result.status === '00' || result.status === 'success') {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Sinkronisasi Stok Berhasil!")%>', 'bg-success text-white');
                loadDataProdukAPI<%=rnd%>();
            } else {
                showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal melakukan sinkronisasi.")%>', 'bg-danger text-white');
            }
        } catch (error) {
            console.error("Sinkronisasi Error: ", error);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Koneksi terputus saat sinkronisasi.")%>', 'bg-danger text-white');
        } finally {
            btnSync.innerHTML = originalBtnHtml;
            btnSync.disabled = false;
        }
    };

    // ==========================================
    // CREATE / UPDATE (FORM MODAL)
    // ==========================================
    const showAddModal<%=rnd%> = () => {
        document.getElementById('modalTitleProduk<%=rnd%>').innerHTML = '<i class="fas fa-plus-circle text-primary me-2"></i><%=Common.getBahasaConfig("Tambah Produk Baru")%>';
        document.getElementById('formProduk<%=rnd%>').reset();
        document.getElementById('formId<%=rnd%>').value = "";
        
        // Logika Pengaturan Hak Akses Edit Harga
        const canEditPrice = isAdminProduk<%=rnd%> || pedagangBolehUbahHarga<%=rnd%>;
        document.getElementById('formHargaBeli<%=rnd%>').disabled = !canEditPrice;
        document.getElementById('formHargaJual<%=rnd%>').disabled = !canEditPrice;

        // Logika Status Aktif Default Berdasarkan Peran (Role)
        if(isAdminProduk<%=rnd%>) {
            document.getElementById('formToko<%=rnd%>').value = "";
            document.getElementById('formAktif<%=rnd%>').checked = true; // Admin membuat produk langsung aktif
        } else {
            document.getElementById('formAktif<%=rnd%>').checked = false; // Pedagang membuat produk menjadi tidak aktif (menunggu disetujui Admin)
        }
        
        // Sembunyikan area upload foto saat tambah (harus save dulu baru punya ID Produk paten)
        document.getElementById('uploadPhotoArea<%=rnd%>').style.display = 'none';
        activeEditingProdukId<%=rnd%> = "";

        // Reset editor bahan baku (resep)
        bahanBakuList<%=rnd%> = [];
        bbRenderOptions<%=rnd%>();
        bbRender<%=rnd%>();

        // Reset editor pilih ekstra
        ekstraPilihanList<%=rnd%> = [];
        eksRenderOptions<%=rnd%>();
        eksRender<%=rnd%>();

        // Reset tautan barang persediaan (aset)
        var selMaAdd = document.getElementById('formMasterAsset<%=rnd%>');
        if (selMaAdd) selMaAdd.value = '';
        var rkAdd = document.getElementById('rekonAsetForm<%=rnd%>');
        if (rkAdd) rkAdd.innerHTML = '';
        
        if(modalInstanceProduk<%=rnd%> === null) {
            modalInstanceProduk<%=rnd%> = new bootstrap.Modal(document.getElementById('modalFormProduk<%=rnd%>'));
        }
        modalInstanceProduk<%=rnd%>.show();
    };

    const showEditModal<%=rnd%> = (id, kode, nama, idJenis, idToko, hb, hj, stok, ket, aktif, izinkanJualMinusStok, barcode, jenisItem) => {
        document.getElementById('modalTitleProduk<%=rnd%>').innerHTML = '<i class="fas fa-edit text-warning me-2"></i><%=Common.getBahasaConfig("Ubah Data Produk")%>';

        document.getElementById('formId<%=rnd%>').value = id;
        document.getElementById('formKode<%=rnd%>').value = kode;
        document.getElementById('formBarcode<%=rnd%>').value = barcode || '';
        document.getElementById('formNama<%=rnd%>').value = nama;
        document.getElementById('formJenis<%=rnd%>').value = idJenis || '';
        document.getElementById('formJenisItem<%=rnd%>').value = jenisItem || 'JUAL';
        
        if(isAdminProduk<%=rnd%>) {
             document.getElementById('formToko<%=rnd%>').value = idToko || '';
        }
        
        document.getElementById('formHargaBeli<%=rnd%>').value = hb;
        document.getElementById('formHargaJual<%=rnd%>').value = hj;

        // Logika Pengaturan Hak Akses Edit Harga
        const canEditPrice = isAdminProduk<%=rnd%> || pedagangBolehUbahHarga<%=rnd%>;
        document.getElementById('formHargaBeli<%=rnd%>').disabled = !canEditPrice;
        document.getElementById('formHargaJual<%=rnd%>').disabled = !canEditPrice;
        
        const elStok = document.getElementById('formStok<%=rnd%>');
        elStok.value = stok;
        elStok.readOnly = true; 
        elStok.title = "<%=Common.getBahasaConfig("Stok tidak dapat diubah manual. Gunakan mutasi pengadaan/opname.")%>";

        document.getElementById('formKeterangan<%=rnd%>').value = ket;
        
        // Atur status aktif yang didapat dari tabel
        document.getElementById('formAktif<%=rnd%>').checked = aktif;

        document.getElementById('formIzinkanJualMinusStok<%=rnd%>').value = (izinkanJualMinusStok === true) ? 'true' : ((izinkanJualMinusStok === false) ? 'false' : '');

        // Tampilkan Area Upload Foto & Load Gambar
        activeEditingProdukId<%=rnd%> = id;
        document.getElementById('uploadPhotoArea<%=rnd%>').style.display = 'block';
        
        const imgUrl = "<%=Common.ROOT%>/Data?action=file&class=<%=ais.database.model.file.LampiranLain.class.getName()%>&ref=" + id + "&jenis=<%=URLEncoder.encode(Produk.class.getName(), "UTF-8")%>&render=true&time=" + new Date().getTime();
        $('#previewImage<%=rnd%>').attr('src', imgUrl);

        // Muat bahan baku (resep) tersimpan
        bbLoadForEdit<%=rnd%>(id);

        // Muat pilihan ekstra tersimpan
        eksLoadForEdit<%=rnd%>(id);

        if(modalInstanceProduk<%=rnd%> === null) {
            modalInstanceProduk<%=rnd%> = new bootstrap.Modal(document.getElementById('modalFormProduk<%=rnd%>'));
        }
        modalInstanceProduk<%=rnd%>.show();
    };

    const saveProduk<%=rnd%> = async () => {
        const id = document.getElementById('formId<%=rnd%>').value;
        const kode = document.getElementById('formKode<%=rnd%>').value.trim();
        const barcode = document.getElementById('formBarcode<%=rnd%>').value.trim();
        const nama = document.getElementById('formNama<%=rnd%>').value.trim();
        const jenisId = document.getElementById('formJenis<%=rnd%>').value;
        const jenisItem = document.getElementById('formJenisItem<%=rnd%>').value || 'JUAL';
        const tokoId = document.getElementById('formToko<%=rnd%>').value;
        
        // Dapatkan value harga (walaupun field disabled, value tetap bisa ditarik oleh JS di browser modern)
        const hb = parseFloat(document.getElementById('formHargaBeli<%=rnd%>').value) || 0;
        const hj = parseFloat(document.getElementById('formHargaJual<%=rnd%>').value) || 0;

        // HPP otomatis: bila produk punya resep/bahan baku, Harga Modal/Beli = total HPP bahan baku
        const hbFinal = (bahanBakuList<%=rnd%>.length > 0) ? bbHitungTotal<%=rnd%>() : hb;
        
        const stok = parseFloat(document.getElementById('formStok<%=rnd%>').value) || 0;
        const ket = document.getElementById('formKeterangan<%=rnd%>').value.trim();
        
        // Dapatkan Status Aktif, entah disabled/terkunci atau tidak, value-nya tetap bisa dibaca
        const aktif = document.getElementById('formAktif<%=rnd%>').checked;

        const izinkanJualMinusStokVal = document.getElementById('formIzinkanJualMinusStok<%=rnd%>').value;
        const izinkanJualMinusStok = izinkanJualMinusStokVal === '' ? null : (izinkanJualMinusStokVal === 'true');

        if(jenisId === "" || tokoId === "") {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Mohon pilih Jenis Produk dan Toko!")%>', 'bg-warning text-dark');
            return;
        }

        const btnSave = document.getElementById('btnSaveProduk<%=rnd%>');
        const oriBtn = btnSave.innerHTML;
        btnSave.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyimpan...")%>';
        btnSave.disabled = true;

        const payload = {
        	action: "simpanDataRinci", 
        	"class": "<%=Produk.class.getName()%>",
        	log:"true",
            data: {
                id: id,
                kode: kode,
                barcode: (barcode === '' ? null : barcode),
                nama: nama,
                keterangan: ket,
                hargaBeli: hbFinal,
                hargaJual: hj,
                stok: stok,
                aktif: aktif,
                izinkanJualMinusStok: izinkanJualMinusStok,
                jenisProduk: jenisId,
                jenisItem: jenisItem,
                toko: tokoId,
                bahanBaku: JSON.stringify(bahanBakuList<%=rnd%>),
                ekstraPilihan: JSON.stringify(ekstraPilihanList<%=rnd%>),
                masterAsset: (document.getElementById('formMasterAsset<%=rnd%>').value || null)
            }
        };

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const result = await response.json();
            
            if (result.status === '00' || result.status === 'success') {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data produk berhasil disimpan.")%>', 'bg-success text-white');
                modalInstanceProduk<%=rnd%>.hide();
                loadDataProdukAPI<%=rnd%>();
            } else {
                showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan data.")%>', 'bg-danger text-white');
            }
        } catch (error) {
            console.error("Save Error:", error);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Koneksi gagal. Silakan coba lagi.")%>', 'bg-danger text-white');
        } finally {
            btnSave.innerHTML = oriBtn;
            btnSave.disabled = false;
        }
    };

    // ==========================================
    // UPLOAD GAMBAR PRODUK (AJAX)
    // ==========================================
    const generatePreview<%=rnd%> = () => {
        if(activeEditingProdukId<%=rnd%>) {
             const imgUrl = "<%=Common.ROOT%>/Data?action=file&class=<%=ais.database.model.file.LampiranLain.class.getName()%>&ref=" + activeEditingProdukId<%=rnd%> + "&jenis=<%=URLEncoder.encode(Produk.class.getName(), "UTF-8")%>&render=true&time=" + new Date().getTime(); 
             $('#previewImage<%=rnd%>').attr('src', imgUrl);
        }
    };

    const resetUpload<%=rnd%> = () => {
        $('#fileNameDisplay<%=rnd%>').val("");
        $('#progress-bar-<%=rnd%>').css('width', '0%').text('0%').removeClass('bg-success bg-danger').addClass('bg-primary');
        $('#progress-wrapper-<%=rnd%>').hide();
    };

    function uploadFile<%=rnd%>() {
        
        var fileInput = $('#input_dofile_foto_<%=rnd%>')[0];
        if (fileInput.files.length === 0) return;
        if (!activeEditingProdukId<%=rnd%>) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Error: Harap simpan produk terlebih dahulu sebelum mengunggah gambar!")%>', 'bg-danger text-white');
            return;
        }

        var file = fileInput.files[0];
        if (!file.type.match('image.*')) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("File yang dipilih bukan gambar (JPG/PNG).")%>', 'bg-warning text-dark');
            fileInput.value = '';
            return;
        }

        resetUpload<%=rnd%>();
        $('#fileNameDisplay<%=rnd%>').val(file.name); 
        $('#progress-wrapper-<%=rnd%>').fadeIn();
        $('#progress-bar-<%=rnd%>').css('width', '10%').text('10%').removeClass('bg-success bg-danger').addClass('bg-primary progress-bar-animated');

        var formData = new FormData();
        formData.append('nama', file.name);
        formData.append('clazz', '<%=ais.database.model.file.LampiranLain.class.getName()%>');
        formData.append('jenis', '<%=Produk.class.getName()%>'); // Menyimpan nama class Produk sebagai string 'jenis'
        formData.append('id', activeEditingProdukId<%=rnd%>); // Menggunakan Long ID Produk
        formData.append('tanpaLogin', 'true');
        formData.append('fileContent', file);

        $.ajax({
            url: '<%=Common.ROOT%>/DoUpload',
            type: 'POST',
            data: formData,
            cache: false,
            contentType: false,
            processData: false,
            xhr: function() {
                var xhr = new window.XMLHttpRequest();
                xhr.upload.addEventListener("progress", function(evt) {
                    if (evt.lengthComputable) {
                        var percentComplete = Math.round((evt.loaded / evt.total) * 100);
                        $('#progress-bar-<%=rnd%>').css('width', percentComplete + '%').text(percentComplete + '%');
                    }
                }, false);
                return xhr;
            },
            success: function(response) {
                $('#progress-bar-<%=rnd%>').removeClass('bg-primary progress-bar-animated').addClass('bg-success');
                $('#progress-bar-<%=rnd%>').css('width', '100%').text('<%=Common.getBahasaConfigJS("Selesai")%>');

                setTimeout(function(){ $('#progress-wrapper-<%=rnd%>').fadeOut(); }, 1500);

                if(response.status === 'Sukses' || response.status === '00' || response.keterangan === 'Sukses'){
                   generatePreview<%=rnd%>();
                   showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gambar produk berhasil diperbarui!")%>', 'bg-success text-white');
                   loadDataProdukAPI<%=rnd%>(); // Segarkan tabel agar gambar thumbnail juga berubah
                } else {
                   showToast<%=rnd%>(response.keterangan || '<%=Common.getBahasaConfigJS("Gagal menyimpan file ke database.")%>', 'bg-danger text-white');
                }
                fileInput.value = ''; 
            },
            error: function(jqXHR, textStatus, errorThrown) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Koneksi bermasalah: ")%> ' + textStatus, 'bg-danger text-white');
                $('#progress-bar-<%=rnd%>').removeClass('bg-primary progress-bar-animated').addClass('bg-danger');
                $('#progress-bar-<%=rnd%>').text('<%=Common.getBahasaConfigJS("Gagal")%>');
            }
        });
    }

    // ==========================================
    // DELETE PRODUK
    // ==========================================
    const deleteProduk<%=rnd%> = async (id) => {
    	 prosesDeleteData('<%=Produk.class.getName()%>', id, function(){ 
    		 if (document.querySelectorAll('#tableProduk<%=rnd%> tr').length === 1 && currentPageProduk<%=rnd%> > 1) {
                 currentPageProduk<%=rnd%>--;
             }
             loadDataProdukAPI<%=rnd%>();
         });
    };


    // ==========================================
    // DOWNLOAD & UPLOAD EXCEL FEATURE
    // ==========================================
    
    // Unduh Excel -- format PERSIS "Daftar Barang dan Jasa" (Accurate), sama dgn versi POS Desktop/
    // Apk Flutter (aksi server `produk_ekspor_excel`, BUKAN lagi format ID_SISTEM/KODE_PRODUK/dst
    // buatan sendiri) -- supaya file yg diunduh di sini bisa langsung dicocokkan/diedit dgn file
    // Accurate asli lalu diunggah kembali tanpa menata ulang kolom sama sekali.
    const downloadExcelProduk<%=rnd%> = async () => {
        showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Sedang menyiapkan file Excel, mohon tunggu...")%>', 'bg-info text-dark');
        const idToko = isAdminProduk<%=rnd%> ? document.getElementById('filterToko<%=rnd%>').value : idTokoLogin<%=rnd%>;
        if (!idToko) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Pilih Toko terlebih dahulu di filter sebelum mengunduh.")%>', 'bg-warning text-dark');
            return;
        }
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: 'produk_ekspor_excel', toko_id: idToko, hanya_aktif: true })
            });
            const result = await res.json();
            if (result.status !== '00' || !result.fileBase64) {
                throw new Error(result.description || 'Server tidak mengembalikan berkas.');
            }
            const byteChars = atob(result.fileBase64);
            const byteNumbers = new Array(byteChars.length);
            for (let i = 0; i < byteChars.length; i++) byteNumbers[i] = byteChars.charCodeAt(i);
            const blob = new Blob([new Uint8Array(byteNumbers)], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement("a");
            link.download = result.namaFile || 'Katalog.xlsx';
            link.href = url;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            setTimeout(() => URL.revokeObjectURL(url), 100);
        } catch (error) {
            console.error(error);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal mengunduh: ")%>' + error.message, 'bg-danger text-white');
        }
    };

    const showUploadModal<%=rnd%> = () => {
        document.getElementById('fileUploadExcel<%=rnd%>').value = '';
        const statusBox = document.getElementById('uploadStatusBox<%=rnd%>');
        statusBox.style.display = 'none';
        statusBox.innerHTML = '';
        
        if(modalInstanceUpload<%=rnd%> === null) {
            modalInstanceUpload<%=rnd%> = new bootstrap.Modal(document.getElementById('modalUploadExcel<%=rnd%>'));
        }
        modalInstanceUpload<%=rnd%>.show();
    };

    // Impor Excel 2 langkah -- PARSE saja dulu (aksi `produk_impor_excel_preview`, format Accurate
    // yg sama dgn Unduh), tampilkan baris di layar Tinjau supaya bisa diperiksa/diedit, BARU disimpan
    // lewat `produk_impor_excel_komit` saat tombol "Simpan Semua" ditekan -- pola SAMA PERSIS dgn
    // versi POS Desktop/Apk Flutter (ImporExcelProdukScreen), bukan lagi commit langsung tanpa tinjau.
    let modalInstanceTinjau<%=rnd%> = null;
    let barisTinjauImpor<%=rnd%> = [];
    let tokoIdTinjauImpor<%=rnd%> = null;

    const arrayBufferKeBase64<%=rnd%> = (buffer) => {
        let binary = '';
        const bytes = new Uint8Array(buffer);
        const chunk = 0x8000;
        for (let i = 0; i < bytes.length; i += chunk) {
            binary += String.fromCharCode.apply(null, bytes.subarray(i, i + chunk));
        }
        return btoa(binary);
    };

    const prosesUploadExcel<%=rnd%> = () => {
        const fileInput = document.getElementById('fileUploadExcel<%=rnd%>');
        const file = fileInput.files[0];

        if (!file) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Pilih file Excel terlebih dahulu!")%>', 'bg-warning text-dark');
            return;
        }

        const idToko = isAdminProduk<%=rnd%> ? document.getElementById('filterToko<%=rnd%>').value : idTokoLogin<%=rnd%>;
        if (!idToko) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Pilih Toko terlebih dahulu di filter sebelum mengunggah.")%>', 'bg-warning text-dark');
            return;
        }

        const btnProcess = document.getElementById('btnProsesUpload<%=rnd%>');
        const statusBox = document.getElementById('uploadStatusBox<%=rnd%>');

        btnProcess.disabled = true;
        btnProcess.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Membaca File...")%>';
        statusBox.style.display = 'block';
        statusBox.className = 'alert alert-info py-2 small';
        statusBox.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Membaca berkas & mencocokkan dgn katalog saat ini...';

        const reader = new FileReader();
        reader.onload = async (e) => {
            try {
                const fileBase64 = arrayBufferKeBase64<%=rnd%>(e.target.result);
                const res = await fetch('<%=Common.ROOT%>/Data', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ action: 'produk_impor_excel_preview', toko_id: idToko, file_base64: fileBase64, format: 'accurate' })
                });
                const result = await res.json();
                if (result.status !== '00') {
                    throw new Error(result.description || 'Gagal membaca berkas Excel.');
                }
                if (!result.baris || result.baris.length === 0) {
                    throw new Error('Tidak ada baris barang/jasa terbaca dari berkas ini.');
                }

                barisTinjauImpor<%=rnd%> = result.baris;
                tokoIdTinjauImpor<%=rnd%> = idToko;
                tampilkanTinjauImporExcel<%=rnd%>(result.kolomTidakDitemukan || []);

                statusBox.style.display = 'none';
                if (modalInstanceUpload<%=rnd%>) modalInstanceUpload<%=rnd%>.hide();
            } catch (error) {
                console.error("Upload Error:", error);
                statusBox.className = 'alert alert-danger py-2 small';
                statusBox.innerHTML = '<i class="fas fa-exclamation-triangle me-2"></i>Terjadi Kesalahan: ' + error.message;
            } finally {
                btnProcess.disabled = false;
                btnProcess.innerHTML = '<i class="fas fa-cogs me-2"></i><%=Common.getBahasaConfig("Proses & Tinjau")%>';
            }
        };

        reader.readAsArrayBuffer(file);
    };

    const tampilkanTinjauImporExcel<%=rnd%> = (kolomTidakDitemukan) => {
        const peringatanBox = document.getElementById('tinjauPeringatanBox<%=rnd%>');
        if (kolomTidakDitemukan.length > 0) {
            peringatanBox.classList.remove('d-none');
            peringatanBox.innerHTML = '<i class="fas fa-exclamation-triangle me-2"></i><b>Kolom tidak ditemukan di berkas:</b> ' + kolomTidakDitemukan.join(', ') +
                '. Baris di bawah akan tersimpan dgn nilai <b>0</b> utk kolom ini kecuali Anda mengedit manual sebelum Simpan.';
        } else {
            peringatanBox.classList.add('d-none');
        }

        document.getElementById('tinjauRingkasan<%=rnd%>').textContent = barisTinjauImpor<%=rnd%>.length + ' baris siap ditinjau.';

        const tbody = document.getElementById('tinjauTabelBody<%=rnd%>');
        let html = '';
        barisTinjauImpor<%=rnd%>.forEach((b, i) => {
            const badgeStatus = b.baru
                ? '<span class="badge bg-success bg-opacity-25 text-success border border-success">Baru</span>'
                : '<span class="badge bg-warning bg-opacity-25 text-dark border border-warning">Update</span>';
            const selisih = (b.stokBaru || 0) - (b.stokLama || 0);
            const warnaSelisih = selisih === 0 ? 'text-muted' : (selisih > 0 ? 'text-success' : 'text-danger');
            html += '<tr>' +
                '<td class="text-center text-muted">' + (i + 1) + '</td>' +
                '<td class="text-start fw-bold">' + (b.kode || '-') + '</td>' +
                '<td class="text-start">' + (b.nama || '-') + '</td>' +
                '<td class="text-start small text-muted">' + (b.kategoriNama || '-') + '</td>' +
                '<td class="text-center small">' + (b.stokLama || 0) + ' &rarr; <input type="number" class="form-control form-control-sm d-inline-block text-center" style="width:80px;" value="' + (b.stokBaru || 0) + '" oninput="tinjauUbahNilai<%=rnd%>(' + i + ', \'stokBaru\', this.value)"><br><span class="' + warnaSelisih + '">(' + (selisih >= 0 ? '+' : '') + selisih + ')</span></td>' +
                '<td class="text-center"><input type="number" class="form-control form-control-sm text-end" style="width:110px;" value="' + (b.hargaJual || 0) + '" oninput="tinjauUbahNilai<%=rnd%>(' + i + ', \'hargaJual\', this.value)"></td>' +
                '<td class="text-center"><input type="number" class="form-control form-control-sm text-end" style="width:110px;" value="' + (b.hargaBeli || 0) + '" oninput="tinjauUbahNilai<%=rnd%>(' + i + ', \'hargaBeli\', this.value)"></td>' +
                '<td class="text-center">' + badgeStatus + '</td>' +
                '</tr>';
        });
        tbody.innerHTML = html;

        const statusBox = document.getElementById('tinjauStatusBox<%=rnd%>');
        statusBox.classList.add('d-none');
        const btnKomit = document.getElementById('btnKomitImporExcel<%=rnd%>');
        btnKomit.disabled = false;
        btnKomit.innerHTML = '<i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Semua")%>';

        if (modalInstanceTinjau<%=rnd%> === null) {
            modalInstanceTinjau<%=rnd%> = new bootstrap.Modal(document.getElementById('modalTinjauImporExcel<%=rnd%>'));
        }
        modalInstanceTinjau<%=rnd%>.show();
    };

    window.tinjauUbahNilai<%=rnd%> = (idx, field, value) => {
        if (!barisTinjauImpor<%=rnd%>[idx]) return;
        barisTinjauImpor<%=rnd%>[idx][field] = parseFloat(value) || 0;
    };

    window.komitImporExcel<%=rnd%> = async () => {
        const btnKomit = document.getElementById('btnKomitImporExcel<%=rnd%>');
        const statusBox = document.getElementById('tinjauStatusBox<%=rnd%>');
        btnKomit.disabled = true;
        btnKomit.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyimpan...")%>';
        statusBox.classList.remove('d-none');
        statusBox.className = 'alert alert-info py-2 mt-3';
        statusBox.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Menyimpan ' + barisTinjauImpor<%=rnd%>.length + ' baris ke server...';

        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: 'produk_impor_excel_komit', toko_id: tokoIdTinjauImpor<%=rnd%>, baris: barisTinjauImpor<%=rnd%> })
            });
            const result = await res.json();
            if (result.status !== '00') {
                throw new Error(result.description || 'Gagal menyimpan data.');
            }
            statusBox.className = 'alert alert-success py-2 mt-3 fw-bold';
            statusBox.innerHTML = '<i class="fas fa-check-circle me-2"></i>Selesai: ' + (result.dibuat || 0) + ' dibuat, ' + (result.diperbarui || 0) +
                ' diperbarui, ' + (result.dilewati || 0) + ' dilewati' + ((result.error || 0) > 0 ? ', <span class="text-danger">' + result.error + ' gagal</span>' : '') + '.';
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Impor Excel selesai!")%>', 'bg-success text-white');
            setTimeout(() => {
                if (modalInstanceTinjau<%=rnd%>) modalInstanceTinjau<%=rnd%>.hide();
                loadDataProdukAPI<%=rnd%>();
            }, 1800);
        } catch (error) {
            console.error("Komit Error:", error);
            statusBox.className = 'alert alert-danger py-2 mt-3';
            statusBox.innerHTML = '<i class="fas fa-exclamation-triangle me-2"></i>Terjadi Kesalahan: ' + error.message;
            btnKomit.disabled = false;
            btnKomit.innerHTML = '<i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Semua")%>';
        }
    };


    // ==========================================
    // INISIALISASI HALAMAN
    // ==========================================
    document.addEventListener("DOMContentLoaded", () => {
        // Init Modals
        modalInstanceProduk<%=rnd%> = new bootstrap.Modal(document.getElementById('modalFormProduk<%=rnd%>'));

        // Load ComboBox
        loadMasterDataCombo<%=rnd%>();

        // Muat kandidat bahan baku (produk lain) untuk editor resep/HPP
        loadBahanBakuOptions<%=rnd%>();

        // Muat kandidat ekstra (produk lain ber-jenisItem EKSTRA) untuk editor Pilih Ekstra
        loadEkstraOptions<%=rnd%>();

        // Muat daftar barang persediaan (aset) untuk tautan opsional
        loadMasterAssetOptions<%=rnd%>();

        // Listener Filter Enter Key
        const filterInputs = document.querySelectorAll('.filter-input-<%=rnd%>');
        filterInputs.forEach(input => {
            if (input.tagName === 'SELECT') {
                input.addEventListener("change", () => triggerSearchProduk<%=rnd%>());
            } else {
                input.addEventListener("keydown", (event) => {
                    if (event.key === "Enter") {
                        event.preventDefault(); 
                        triggerSearchProduk<%=rnd%>();
                    }
                });
            }
        });

        // First Load
        loadDataProdukAPI<%=rnd%>();

        // Dasbor Statistik Produk
        muatStatistikProduk<%=rnd%>();
    });

    // ==========================================
    // DASBOR STATISTIK PRODUK
    // ==========================================
    const fmtRupiahSp<%=rnd%> = (n) => 'Rp ' + Math.round(Number(n) || 0).toLocaleString('id-ID');

    function buatBarProduk<%=rnd%>(container, data) {
        if (!data || data.length === 0) { container.innerHTML = '<div class="text-muted small">Belum ada data.</div>'; return; }
        const maks = Math.max(...data.map(d => Number(d.jumlah) || 0)) || 1;
        container.innerHTML = data.map((d, i) => {
            const persen = Math.max(4, Math.round(((Number(d.jumlah) || 0) / maks) * 100));
            return '<div class="d-flex align-items-center gap-2 py-1" style="border-bottom:1px solid #f1f3f5;">'
                + '<div class="text-muted" style="width:16px;font-size:10px;font-weight:800;">' + (i + 1) + '</div>'
                + '<div class="flex-grow-1 text-truncate" style="font-size:12px;font-weight:600;">' + (d.label || '-') + '</div>'
                + '<div style="width:80px;height:6px;background:#f1f3f5;border-radius:99px;overflow:hidden;"><div style="width:' + persen + '%;height:100%;background:linear-gradient(90deg,#0d6efd,#7c3aed);"></div></div>'
                + '<div style="width:36px;text-align:right;font-size:11px;font-weight:800;">' + d.jumlah + '</div>'
                + '</div>';
        }).join('');
    }

    async function muatStatistikProduk<%=rnd%>() {
        const tokoKlausa = isAdminProduk<%=rnd%> ? '' : (' AND p.toko = ' + idTokoLogin<%=rnd%>);
        try {
            const kpi = await fetchSqlData<%=rnd%>(
                "SELECT COUNT(*) total, COUNT(CASE WHEN COALESCE(p.aktif,true)=true THEN 1 END) aktif, "
                + "COUNT(CASE WHEN COALESCE(p.aktif,true)=false THEN 1 END) nonaktif, "
                + "COUNT(CASE WHEN COALESCE(p.stok,0)<=0 THEN 1 END) habis, "
                + "COUNT(CASE WHEN COALESCE(p.stok,0)>0 AND COALESCE(p.stok,0)<=5 THEN 1 END) rendah, "
                + "COALESCE(SUM(COALESCE(p.stok,0)*COALESCE(p.hargabeli,0)),0) nilaistok "
                + "FROM koperasi.produk p WHERE 1=1" + tokoKlausa);
            if (kpi && kpi[0]) {
                const k = kpi[0];
                document.getElementById('spTotal<%=rnd%>').textContent = k.total || 0;
                document.getElementById('spAktif<%=rnd%>').textContent = k.aktif || 0;
                document.getElementById('spNonaktif<%=rnd%>').textContent = k.nonaktif || 0;
                document.getElementById('spHabis<%=rnd%>').textContent = k.habis || 0;
                document.getElementById('spRendah<%=rnd%>').textContent = k.rendah || 0;
                document.getElementById('spNilaiStok<%=rnd%>').textContent = fmtRupiahSp<%=rnd%>(k.nilaistok);
            }

            const kategori = await fetchSqlData<%=rnd%>(
                "SELECT COALESCE(j.nama,'Tanpa Kategori') label, COUNT(*) jumlah FROM koperasi.produk p "
                + "LEFT JOIN koperasi.jenis_produk j ON p.jenis_produk = j.id WHERE 1=1" + tokoKlausa
                + " GROUP BY label ORDER BY jumlah DESC LIMIT 8");
            buatBarProduk<%=rnd%>(document.getElementById('spBarKategori<%=rnd%>'), kategori);

            const pemasok = await fetchSqlData<%=rnd%>(
                "SELECT COALESCE(s.nama,'Tanpa Pemasok') label, COUNT(*) jumlah FROM koperasi.produk p "
                + "LEFT JOIN koperasi.pemasok_produk s ON p.pemasok = s.id WHERE 1=1" + tokoKlausa
                + " GROUP BY label ORDER BY jumlah DESC LIMIT 8");
            buatBarProduk<%=rnd%>(document.getElementById('spBarPemasok<%=rnd%>'), pemasok);

            const harga = await fetchSqlData<%=rnd%>(
                "SELECT CASE WHEN COALESCE(p.hargajual,0) < 5000 THEN '< Rp 5rb' "
                + "WHEN COALESCE(p.hargajual,0) < 10000 THEN 'Rp 5rb - 10rb' "
                + "WHEN COALESCE(p.hargajual,0) < 20000 THEN 'Rp 10rb - 20rb' "
                + "WHEN COALESCE(p.hargajual,0) < 50000 THEN 'Rp 20rb - 50rb' "
                + "ELSE '>= Rp 50rb' END label, COUNT(*) jumlah FROM koperasi.produk p WHERE 1=1" + tokoKlausa
                + " GROUP BY label ORDER BY MIN(COALESCE(p.hargajual,0))");
            buatBarProduk<%=rnd%>(document.getElementById('spBarHarga<%=rnd%>'), harga);
        } catch (e) {
            console.error('Gagal memuat statistik produk:', e);
        }
    }
</script>