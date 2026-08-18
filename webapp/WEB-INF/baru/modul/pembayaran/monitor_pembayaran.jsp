<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.JenisKegiatan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="java.util.List"%>
<%@page import="java.util.TreeSet"%>
<%@page import="java.util.Calendar"%>

<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.sendRedirect(Common.ROOT + "/login.jsp");
        return;
    }

    String rnd = request.getParameter("rnd") != null ? request.getParameter("rnd") : Common.getGeneratedBarCode(6);
    
    Session sess = null;
    TreeSet<String> listTa = Common.tahunAngkatans; 
    List<Fakultas> listFakultas = null;
    List<Jurusan> listJurusan = null;
    List<JenisKegiatan> listJk = null;
    
    String currentTa = Common.getCurrentTahunAkademik();
    boolean isCurrentTaExist = false;
    int currentYear = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
    
    Fakultas currentFakultas = tbmuser.ambilFakultas();
    Jurusan currentJurusan = tbmuser.ambilJurusan();
    
    Long currFakId = currentFakultas != null ? currentFakultas.getId() : null;
    Long currJurId = currentJurusan != null ? currentJurusan.getId() : null;
    
    try {
        sess = HibernateUtil.openSession();
        listFakultas = ConstantValues.simpleList(sess.createCriteria(Fakultas.class).add(Restrictions.eq("aktif", true)).addOrder(Order.asc("nama")), Fakultas.class);
        listJurusan = ConstantValues.simpleList(sess.createCriteria(Jurusan.class).add(Restrictions.eq("aktif", true)).addOrder(Order.asc("nama")), Jurusan.class);
        listJk = ConstantValues.simpleList(sess.createCriteria(JenisKegiatan.class).add(Restrictions.eq("aktif", true)).addOrder(Order.asc("namaKegiatan")), JenisKegiatan.class);
        
        if (listTa != null && currentTa != null) {
            for (String t : listTa) {
                if (currentTa.equals(t)) { isCurrentTaExist = true; break; }
            }
        }
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pembayaran/monitor_pembayaran.jsp:52");
    } finally {
        if(sess != null && sess.isOpen()){ sess.disconnect(); sess.close(); }
        try{ HibernateUtil.closeSessionQuietly(sess); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/monitor_pembayaran.jsp:55");}
    }
%>

<script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<script data-cfasync="false" src="https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js"></script>

<style>
    .hover-elevate-<%=rnd%> { transition: transform 0.2s ease, box-shadow 0.2s ease; }
    .hover-elevate-<%=rnd%>:hover { transform: translateY(-2px); box-shadow: 0 0.5rem 1rem rgba(0,0,0,.1) !important; }
    .chart-container-<%=rnd%> { position: relative; height: 360px; width: 100%; }
    
    .progress-custom-<%=rnd%> { height: 25px; border-radius: 12px; background-color: #e9ecef; overflow: hidden; box-shadow: inset 0 1px 2px rgba(0,0,0,.1); }
    .progress-bar-custom-<%=rnd%> { display: flex; align-items: center; justify-content: center; color: white; font-weight: bold; background-image: linear-gradient(45deg,rgba(255,255,255,.15) 25%,transparent 25%,transparent 50%,rgba(255,255,255,.15) 50%,rgba(255,255,255,.15) 75%,transparent 75%,transparent); background-size: 1rem 1rem; animation: progress-bar-stripes 1s linear infinite; }
    
    .progress-custom-main-<%=rnd%> { height: 35px; border-radius: 18px; background-color: #e9ecef; overflow: hidden; box-shadow: inset 0 2px 4px rgba(0,0,0,.15); }
    .progress-bar-custom-main-<%=rnd%> { display: flex; align-items: center; justify-content: center; color: white; font-weight: bold; font-size: 1.1rem; background-image: linear-gradient(45deg,rgba(255,255,255,.15) 25%,transparent 25%,transparent 50%,rgba(255,255,255,.15) 50%,rgba(255,255,255,.15) 75%,transparent 75%,transparent); background-size: 1.5rem 1.5rem; animation: progress-bar-stripes 1s linear infinite; }
    
    .table-sticky-header-<%=rnd%> thead th { position: sticky; top: 0; z-index: 1; background-color: #212529; }
    .table-container-scroll-<%=rnd%> { max-height: 600px; overflow-y: auto; }
    .input-locked-<%=rnd%> { background-color: #f8f9fa !important; pointer-events: none; opacity: 0.85; }
    
    .filter-panel-<%=rnd%> { background-color: #fcfcfc; border: 1px solid rgba(0,0,0,0.08); border-radius: 0.75rem; padding: 1.25rem; }
    .advanced-options-<%=rnd%> { background-color: #f8f9fa; border: 1px dashed #dee2e6; border-radius: 0.75rem; padding: 1.25rem; margin-top: 1rem; }
    
    /* Style untuk dropdown autocomplete */
    .autocomplete-dropdown-<%=rnd%> { position: absolute; top: 100%; left: 0; z-index: 1050; display: none; width: 100%; max-height: 250px; overflow-y: auto; background-color: #fff; border: 1px solid rgba(0,0,0,.15); border-radius: .375rem; box-shadow: 0 0.5rem 1rem rgba(0,0,0,.175); padding: .5rem 0; margin-top: .125rem; }
    .autocomplete-item-<%=rnd%> { display: block; width: 100%; padding: .5rem 1rem; clear: both; font-weight: 400; color: #212529; text-align: inherit; text-decoration: none; white-space: nowrap; background-color: transparent; border: 0; cursor: pointer; border-bottom: 1px solid #f1f1f1; }
    .autocomplete-item-<%=rnd%>:hover, .autocomplete-item-<%=rnd%>:focus { color: #1e2125; background-color: #e9ecef; }
</style>

<div class="container-fluid py-4 animate__animated animate__fadeIn">
    
    <div class="d-flex align-items-center mb-4 p-4 bg-white shadow-sm rounded-4 border-start border-5 border-primary">
        <div class="bg-primary text-white rounded-circle d-flex align-items-center justify-content-center me-3 shadow-sm" style="width: 55px; height: 55px;">
            <i class="fas fa-chart-pie fs-3"></i>
        </div>
        <div>
            <h4 class="mb-1 fw-bold text-dark"><%= Common.getBahasaConfig("Dasbor Monitor Pembayaran (Rekapitulasi & Piutang)") %></h4>
            <p class="text-muted mb-0 small"><%= Common.getBahasaConfig("Analisis komprehensif Tagihan, Pelunasan, Piutang, Arus Kas, Kanal Bayar, dan Buku Besar Mahasiswa.") %></p>
        </div>
    </div>

    <div class="card border-0 shadow-sm rounded-4 mb-4">
        <div class="card-header bg-light border-bottom py-3 d-flex align-items-center">
            <i class="fas fa-sliders-h text-primary me-2 fs-5"></i>
            <h6 class="fw-bold mb-0 text-primary"><%= Common.getBahasaConfig("Konfigurasi Parameter Dasbor") %></h6>
        </div>
        <div class="card-body bg-white p-4">
            <form id="formMonitorPembayaran<%=rnd%>" onsubmit="event.preventDefault(); window.eksekusiDasbor<%=rnd%>();">
                
                <div class="filter-panel-<%=rnd%> mb-3">
                    <div class="row g-3 mb-3">
                        <div class="col-lg-4 col-md-6">
                            <label class="form-label fw-bold text-secondary small"><i class="fas fa-layer-group me-1 text-info"></i> <%= Common.getBahasaConfig("Dimensi Dasbor") %></label>
                            <select class="form-select border-secondary border-opacity-25 bg-white fw-bold text-primary shadow-sm" name="jenisDashboard" id="fDashboardType<%=rnd%>" onchange="window.toggleFilterKhusus<%=rnd%>();" required>
                                <option value="_service_piutang_mahasiswa">1. <%= Common.getBahasaConfig("Piutang Mahasiswa (Per Prodi & Jenis)") %></option>
                                <option value="_service_piutang_mahasiswa_per_angkatan">2. <%= Common.getBahasaConfig("Piutang Mahasiswa (Per Angkatan)") %></option>
                                <option value="_service_piutang_mahasiswa_status_awal">3. <%= Common.getBahasaConfig("Piutang Mahasiswa (Status Awal)") %></option>
                                <option value="_service_piutang_mahasiswa_program">4. <%= Common.getBahasaConfig("Piutang Mahasiswa (Per Program)") %></option>
                                <option value="_service_piutang_mahasiswa_per_validator">5. <%= Common.getBahasaConfig("Piutang Mahasiswa (Per Validator)") %></option>
                                <option value="_service_kartu_piutang_rinci">6. <%= Common.getBahasaConfig("Kartu Piutang Rinci (Detail Transaksi)") %></option>
                                <option value="_service_proses_tagihan">7. <%= Common.getBahasaConfig("Proses Tagihan Mahasiswa (Hitung Ulang)") %></option>
                                <option value="_service_trend_pembayaran">8. <%= Common.getBahasaConfig("Tren Pembayaran Mahasiswa (Grafik & Waktu)") %></option>
                                <option value="_service_umur_piutang">9. <%= Common.getBahasaConfig("Analisis Umur Piutang (Aging Schedule)") %></option>
                                <option value="_service_piutang_item_biaya">10. <%= Common.getBahasaConfig("Rekapitulasi per Komponen Item Biaya") %></option>
                                <option value="_service_top_debtors">11. <%= Common.getBahasaConfig("Peringkat Penunggak Terbesar (Top Debtors)") %></option>
                                <option value="_service_distribusi_pelunasan">12. <%= Common.getBahasaConfig("Peta Distribusi Status Pelunasan (Headcount)") %></option>
                                <option value="_service_arus_kas">13. <%= Common.getBahasaConfig("Target vs Realisasi Arus Kas (Cashflow)") %></option>
                                <option value="_service_kanal_pembayaran">14. <%= Common.getBahasaConfig("Analisis Kanal / Metode Pembayaran (Channels)") %></option>
                                <option value="_service_ledger_mahasiswa">15. <%= Common.getBahasaConfig("Informasi Tagihan & Pembayaran Per Mahasiswa") %></option>
                            </select>
                        </div>
                        <div class="col-lg-2 col-md-3">
                            <label class="form-label fw-bold text-secondary small"><i class="far fa-calendar-alt me-1 text-warning"></i> <%= Common.getBahasaConfig("TA Patokan") %></label>
                            <select class="form-select border-secondary border-opacity-25 shadow-sm" name="tahunAkademik" required>
                                <% if (currentTa != null && !currentTa.trim().isEmpty() && !isCurrentTaExist) { %> <option value="<%= currentTa %>" selected><%= currentTa %></option> <% } %>
                                <% if(listTa != null) { for(String t : listTa) { String sel = (t != null && t.equals(currentTa)) ? "selected" : ""; %> <option value="<%= t %>" <%= sel %>><%= t %></option> <% } } %>
                            </select>
                        </div>
                        <div class="col-lg-3 col-md-3">
                            <label class="form-label fw-bold text-secondary small"><i class="fas fa-list-ol me-1 text-secondary"></i> <%= Common.getBahasaConfig("Semester") %></label>
                            <select class="form-select border-secondary border-opacity-25 shadow-sm" name="semester">
                                <option value=""><%= Common.getBahasaConfig("-- Semua Semester --") %></option>
                                <option value="Ganjil"><%= Common.getBahasaConfig("Semester Ganjil") %></option>
                                <option value="Genap"><%= Common.getBahasaConfig("Semester Genap") %></option>
                            </select>
                        </div>
                        <div class="col-lg-3 col-md-12">
                            <label class="form-label fw-bold text-secondary small"><i class="fas fa-receipt me-1 text-danger"></i> <%= Common.getBahasaConfig("Jenis Pembayaran") %></label>
                            <select class="form-select border-secondary border-opacity-25 shadow-sm" name="jkId">
                                <option value=""><%= Common.getBahasaConfig("-- Semua Jenis Bayar --") %></option>
                                <% if(listJk != null) { for(JenisKegiatan jk : listJk) { %> <option value="<%= jk.getId() %>"><%= jk.getNamaKegiatan() %></option> <% } } %>
                            </select>
                        </div>
                    </div>
                    <div class="row g-3" id="rowProdiFakultas<%=rnd%>">
                        <div class="col-lg-5 col-md-6">
                            <label class="form-label fw-bold text-secondary small"><i class="fas fa-university me-1 text-primary"></i> <%= Common.getBahasaConfig("Fakultas") %></label>
                            <select class="form-select border-secondary border-opacity-25 shadow-sm <%= currFakId != null ? "input-locked-" + rnd : "" %>" name="fakultasId" id="fFakultas<%=rnd%>" onchange="window.filterJurusan<%=rnd%>(this.value);">
                                <% if (currFakId == null) { %>
                                    <option value=""><%= Common.getBahasaConfig("-- Semua Fakultas --") %></option>
                                <% } %>
                                <% if(listFakultas != null) { 
                                    for(Fakultas f : listFakultas) { 
                                        if (currFakId != null && !f.getId().equals(currFakId)) continue; 
                                %> 
                                    <option value="<%= f.getId() %>" <%= currFakId != null ? "selected" : "" %>><%= f.getNama() %></option> 
                                <% } } %>
                            </select>
                        </div>
                        <div class="col-lg-7 col-md-6">
                            <label class="form-label fw-bold text-secondary small"><i class="fas fa-graduation-cap me-1 text-success"></i> <%= Common.getBahasaConfig("Program Studi") %></label>
                            <select class="form-select border-secondary border-opacity-25 shadow-sm <%= currJurId != null ? "input-locked-" + rnd : "" %>" name="jurusanId" id="fJurusan<%=rnd%>">
                                <% if (currJurId == null) { %>
                                    <option value=""><%= Common.getBahasaConfig("-- Semua Program Studi --") %></option>
                                <% } %>
                                <% if(listJurusan != null) { 
                                    for(Jurusan j : listJurusan) { 
                                        if (currFakId != null && j.getFakultas() != null && !j.getFakultas().getId().equals(currFakId)) continue;
                                        if (currJurId != null && !j.getId().equals(currJurId)) continue;
                                %> 
                                    <option value="<%= j.getId() %>" data-fak-id="<%= j.getFakultas() != null ? j.getFakultas().getId() : "" %>" <%= currJurId != null ? "selected" : "" %>><%= j.getNama() %></option> 
                                <% } } %>
                            </select>
                        </div>
                    </div>
                </div>

                <div id="wrapperAdvancedFilter<%=rnd%>" class="advanced-options-<%=rnd%> animate__animated animate__fadeIn" style="display: none;">
                    <h6 class="fw-bold text-dark mb-3"><i class="fas fa-cogs me-2 text-secondary"></i><span id="titleFilterLanjutan<%=rnd%>"><%= Common.getBahasaConfig("Parameter Lanjutan Khusus Dasbor") %></span></h6>
                    <div class="row g-3 align-items-end">
                        
                        <div class="col-lg-4 col-md-12" id="colSearch<%=rnd%>" style="display: none;">
                            <label class="form-label fw-bold text-secondary small" id="lblSearch<%=rnd%>"><i class="fas fa-search me-1 text-primary"></i> <%= Common.getBahasaConfig("Identitas (NIM/Nama)") %></label>
                            <div class="position-relative">
                                <input type="text" class="form-control border-secondary border-opacity-25 shadow-sm" name="q" id="fSearchInput<%=rnd%>" placeholder="<%= Common.getBahasaConfig("Kata kunci pencarian...") %>" autocomplete="off">
                                <div class="autocomplete-dropdown-<%=rnd%>" id="fSearchDropdown<%=rnd%>"></div>
                            </div>
                        </div>

                        <div class="col-lg-4 col-md-12" id="colHanyaBelumLunas<%=rnd%>" style="display: none;">
                            <div class="form-check bg-white p-2 rounded border border-warning border-opacity-50 shadow-sm d-flex align-items-center h-100">
                                <input class="form-check-input ms-1" type="checkbox" name="hanyaBelumLunas" value="true" id="chkHanyaBelumLunas<%=rnd%>">
                                <label class="form-check-label fw-bold text-dark small ms-2" for="chkHanyaBelumLunas<%=rnd%>"><%= Common.getBahasaConfig("Tampilkan Hanya Tagihan Belum Lunas") %></label>
                            </div>
                        </div>

                        <div class="col-lg-3 col-md-6" id="colPagingStart<%=rnd%>" style="display: none;">
                            <label class="form-label fw-bold text-secondary small"><i class="fas fa-play me-1 text-success"></i> <%= Common.getBahasaConfig("Mulai Baris") %></label>
                            <input type="number" class="form-control border-secondary border-opacity-25 text-primary fw-bold shadow-sm" name="start" id="fStart<%=rnd%>" value="0" min="0">
                        </div>
                        <div class="col-lg-5 col-md-6" id="colPagingLimit<%=rnd%>" style="display: none;">
                            <label class="form-label fw-bold text-secondary small"><i class="fas fa-bars me-1 text-info"></i> <%= Common.getBahasaConfig("Jumlah Data") %></label>
                            <input type="number" class="form-control border-secondary border-opacity-25 text-primary fw-bold shadow-sm" name="limit" id="fLimit<%=rnd%>" value="500" min="1" max="5000">
                        </div>

                        <div class="col-lg-2 col-md-4" id="colTaSampai<%=rnd%>" style="display: none;">
                            <label class="form-label fw-bold text-secondary small"><i class="far fa-calendar-check me-1 text-warning"></i> <%= Common.getBahasaConfig("TA Sampai") %></label>
                            <select class="form-select border-secondary border-opacity-25 shadow-sm" name="tahunAkademikSampai">
                                <% if(listTa != null) { for(String t : listTa) { String sel = (t != null && t.equals(currentTa)) ? "selected" : ""; %> <option value="<%= t %>" <%= sel %>><%= t %></option> <% } } %>
                            </select>
                        </div>
                        <div class="col-lg-2 col-md-4" id="colAngkatanMulai<%=rnd%>" style="display: none;">
                            <label class="form-label fw-bold text-secondary small"><i class="fas fa-hourglass-start me-1 text-secondary"></i> <%= Common.getBahasaConfig("Angk. Mulai") %></label>
                            <input type="number" class="form-control border-secondary border-opacity-25 shadow-sm" name="angkatanMulai" value="<%= currentYear - 5 %>">
                        </div>
                        <div class="col-lg-2 col-md-4" id="colAngkatanSampai<%=rnd%>" style="display: none;">
                            <label class="form-label fw-bold text-secondary small"><i class="fas fa-hourglass-end me-1 text-secondary"></i> <%= Common.getBahasaConfig("Angk. Sampai") %></label>
                            <input type="number" class="form-control border-secondary border-opacity-25 shadow-sm" name="angkatanSampai" value="<%= currentYear %>">
                        </div>
                        <div class="col-lg-2 col-md-6" id="colWorker<%=rnd%>" style="display: none;">
                            <label class="form-label fw-bold text-secondary small" title="<%= Common.getBahasaConfig("Jumlah maksimal thread paralel.") %>"><i class="fas fa-microchip me-1 text-dark"></i> <%= Common.getBahasaConfig("Jml Utas") %></label>
                            <input type="number" class="form-control border-secondary border-opacity-25 text-primary fw-bold shadow-sm" name="jumlahWorker" value="10" min="1" max="50">
                        </div>
                        
                        <div class="col-lg-8 col-md-12 mt-3" id="colHitungUlang<%=rnd%>" style="display: none;">
                            <div class="form-check bg-white p-2 rounded border border-secondary border-opacity-25 shadow-sm d-inline-block px-4">
                                <input class="form-check-input" type="checkbox" name="hitungUlang" value="true" id="chkHitungUlang<%=rnd%>" checked>
                                <label class="form-check-label fw-bold text-dark small ms-2" for="chkHitungUlang<%=rnd%>"><%= Common.getBahasaConfig("Lakukan Kalkulasi Ulang Tagihan Secara Menyeluruh") %></label>
                            </div>
                        </div>

                        <div class="col-lg-3 col-md-4" id="colRentangWaktu<%=rnd%>" style="display: none;">
                            <label class="form-label fw-bold text-secondary small"><i class="far fa-clock me-1 text-primary"></i> <%= Common.getBahasaConfig("Rentang Waktu Data") %></label>
                            <select class="form-select border-secondary border-opacity-25 shadow-sm" name="rentangWaktu" id="fRentangWaktu<%=rnd%>" onchange="window.toggleCustomDate<%=rnd%>();">
                                <option value="1_minggu"><%= Common.getBahasaConfig("1 Minggu Terakhir") %></option>
                                <option value="3_minggu"><%= Common.getBahasaConfig("3 Minggu Terakhir") %></option>
                                <option value="1_bulan"><%= Common.getBahasaConfig("1 Bulan Terakhir") %></option>
                                <option value="3_bulan"><%= Common.getBahasaConfig("3 Bulan Terakhir") %></option>
                                <option value="6_bulan"><%= Common.getBahasaConfig("6 Bulan Terakhir") %></option>
                                <option selected value="1_tahun"><%= Common.getBahasaConfig("1 Tahun Terakhir") %></option>
                                <option value="3_tahun"><%= Common.getBahasaConfig("3 Tahun Terakhir") %></option>
                                <option value="semua"><%= Common.getBahasaConfig("Sepanjang Masa (All Time)") %></option>
                                <option value="custom"><%= Common.getBahasaConfig("Pilih Tanggal Custom") %></option>
                            </select>
                        </div>
                        <div class="col-lg-3 col-md-4 custom-date-<%=rnd%>" style="display: none;">
                            <label class="form-label fw-bold text-secondary small"><i class="far fa-calendar-plus me-1 text-success"></i> <%= Common.getBahasaConfig("Dari Tanggal") %></label>
                            <input type="date" class="form-control border-secondary border-opacity-25 shadow-sm" name="startDate" id="fStartDate<%=rnd%>">
                        </div>
                        <div class="col-lg-3 col-md-4 custom-date-<%=rnd%>" style="display: none;">
                            <label class="form-label fw-bold text-secondary small"><i class="far fa-calendar-check me-1 text-danger"></i> <%= Common.getBahasaConfig("Sampai Tanggal") %></label>
                            <input type="date" class="form-control border-secondary border-opacity-25 shadow-sm" name="endDate" id="fEndDate<%=rnd%>">
                        </div>
                    </div>
                </div>
                
                <input type="hidden" name="page" id="fPage<%=rnd%>" value="1">

                <hr class="mt-4 mb-3 border-secondary border-opacity-25">
                <div class="d-flex justify-content-end">
                    <button type="submit" id="btnSubmitDasbor<%=rnd%>" class="btn btn-primary fw-bold shadow px-5 py-2 hover-elevate-<%=rnd%>">
                        <i class="fas fa-sync-alt me-2"></i> <span id="txtBtnSubmit<%=rnd%>"><%= Common.getBahasaConfig("Terapkan dan Muat Dasbor") %></span>
                    </button>
                </div>
            </form>
        </div>
    </div>

    <div id="loadingContainer<%=rnd%>" class="text-center py-5" style="display: none;">
        <div class="spinner-border text-primary" style="width: 4rem; height: 4rem; border-width: 0.35em;"></div>
        <h5 class="mt-4 text-secondary fw-bold"><%= Common.getBahasaConfig("Sedang Mengambil dan Mengalkulasi Data...") %></h5>
    </div>

    <div id="progressContainer<%=rnd%>" class="card border-0 shadow-sm rounded-4 mb-4 p-5 text-center" style="display: none;">
        <i class="fas fa-cogs fa-4x text-primary mb-3 shadow-sm rounded-circle p-4 bg-light d-inline-block"></i>
        <h4 class="fw-bold text-dark"><%= Common.getBahasaConfig("Sistem Sedang Memproses Perhitungan Ulang Tagihan...") %></h4>
        <p class="text-muted fw-semibold mb-4" id="progressStatusText<%=rnd%>"><%= Common.getBahasaConfig("Menginisialisasi pekerjaan latar belakang (Background Job)...") %></p>
        
        <div class="row justify-content-center mb-3">
            <div class="col-md-8 d-flex justify-content-between align-items-end px-3">
                <div class="text-start">
                    <div class="small fw-bold text-secondary text-uppercase mb-1" style="letter-spacing: 1px;"><%= Common.getBahasaConfig("Data Terselesaikan") %></div>
                    <h5 class="fw-bold text-dark mb-0"><span id="lblDataSelesai<%=rnd%>" class="text-success fs-4">0</span> <small class="text-muted fs-6">/ <span id="lblDataTotal<%=rnd%>">0</span></small></h5>
                </div>
                <div class="text-end">
                    <div class="small fw-bold text-secondary text-uppercase mb-1" style="letter-spacing: 1px;"><%= Common.getBahasaConfig("Sisa Antrean Data") %></div>
                    <h5 class="fw-bold text-danger mb-0 fs-4" id="lblDataSisa<%=rnd%>">0</h5>
                </div>
            </div>
        </div>

        <div class="progress-custom-main-<%=rnd%> w-75 mx-auto mb-4 shadow-sm border border-secondary border-opacity-25">
            <div id="progressBar<%=rnd%>" class="progress-bar-custom-main-<%=rnd%> bg-primary" style="width: 0%; transition: width 0.5s;">0%</div>
        </div>
        
        <hr class="w-75 mx-auto opacity-25">
        <h6 class="fw-bold text-secondary text-uppercase mt-4 mb-3"><i class="fas fa-network-wired me-2 text-primary"></i> <%= Common.getBahasaConfig("Status Utas Pekerja Paralel (Worker Threads)") %></h6>
        
        <div id="workersProgressContainer<%=rnd%>" class="w-100 px-4"></div>
    </div>

    <div id="chartContainerWrapper<%=rnd%>" style="display: none;">
        
        <div class="row g-4 mb-4" id="grafikRowWrapper<%=rnd%>">
            <div class="col-lg-6" id="colChart1<%=rnd%>">
                <div class="card border-0 shadow-sm rounded-4 h-100">
                    <div class="card-header bg-white border-bottom py-3 text-center">
                        <h6 class="fw-bold text-danger mb-0" id="titleChartPiutang<%=rnd%>"><i class="fas fa-chart-line me-2"></i><%= Common.getBahasaConfig("Visualisasi Grafik Utama") %></h6>
                    </div>
                    <div class="card-body p-4">
                        <div class="chart-container-<%=rnd%>"><canvas id="chartPiutang<%=rnd%>"></canvas></div>
                    </div>
                </div>
            </div>
            <div class="col-lg-6" id="colChart2<%=rnd%>">
                <div class="card border-0 shadow-sm rounded-4 h-100">
                    <div class="card-header bg-white border-bottom py-3 text-center">
                        <h6 class="fw-bold text-success mb-0" id="titleChartLunas<%=rnd%>"><i class="fas fa-chart-pie me-2"></i><%= Common.getBahasaConfig("Visualisasi Grafik Sekunder") %></h6>
                    </div>
                    <div class="card-body p-4">
                        <div class="chart-container-<%=rnd%>"><canvas id="chartLunas<%=rnd%>"></canvas></div>
                    </div>
                </div>
            </div>
        </div>

        <div class="card border-0 shadow-sm rounded-4 overflow-hidden mb-5" id="cardTabelBiasa<%=rnd%>">
            <div class="card-header bg-light border-bottom py-3 d-flex justify-content-between align-items-center flex-wrap gap-2">
                <h6 class="fw-bold mb-0 text-dark"><i class="fas fa-table me-2 text-primary"></i> <span id="labelTabel<%=rnd%>"><%= Common.getBahasaConfig("Rincian Data Tabular") %></span></h6>
                
                <div class="d-flex align-items-center gap-2">
                    <div id="paginationControls<%=rnd%>" style="display: none;" class="d-flex align-items-center me-3">
                        <button class="btn btn-sm btn-outline-secondary me-1 shadow-sm" onclick="window.gantiHalaman<%=rnd%>(-1)" id="btnPrevPage<%=rnd%>"><i class="fas fa-chevron-left"></i></button>
                        <span class="badge bg-secondary px-3 py-2 fw-bold shadow-sm" id="lblInfoHalaman<%=rnd%>"><%= Common.getBahasaConfig("Hal 1 dari 1") %></span>
                        <button class="btn btn-sm btn-outline-secondary ms-1 shadow-sm" onclick="window.gantiHalaman<%=rnd%>(1)" id="btnNextPage<%=rnd%>"><i class="fas fa-chevron-right"></i></button>
                    </div>

                    <button type="button" class="btn btn-sm btn-success fw-bold shadow-sm hover-elevate-<%=rnd%>" onclick="window.downloadExcel<%=rnd%>()">
                        <i class="fas fa-file-excel me-2"></i><%= Common.getBahasaConfig("Unduh Berkas Excel") %>
                    </button>
                </div>
            </div>
            <div class="table-responsive table-container-scroll-<%=rnd%>" id="tableContainer<%=rnd%>">
                <table class="table table-hover table-bordered align-middle mb-0 table-sticky-header-<%=rnd%>" id="tabelRekapDasbor<%=rnd%>" style="width: 100%; font-size: 0.85rem; white-space: nowrap;">
                    <thead class="table-dark text-center" id="theadRekap<%=rnd%>"></thead>
                    <tbody id="tbodyRekap<%=rnd%>"></tbody>
                    <tfoot class="table-secondary fw-bold text-end" id="tfootRekap<%=rnd%>"></tfoot>
                </table>
            </div>
            
            <div class="card-footer bg-light border-top text-center text-muted small" id="footerTotalInfo<%=rnd%>" style="display:none;">
                <%= Common.getBahasaConfig("Menampilkan") %> <span id="lblJmlTampil<%=rnd%>" class="fw-bold text-dark">0</span> <%= Common.getBahasaConfig("dari total") %> <span id="lblJmlTotal<%=rnd%>" class="fw-bold text-dark">0</span> <%= Common.getBahasaConfig("data yang ditemukan.") %>
            </div>
        </div>

        <div id="containerBukuBesarMahasiswa<%=rnd%>" style="display: none;"></div>

    </div>
</div>

<script>
    var chartP<%=rnd%> = null; var chartL<%=rnd%> = null;
    var jobInterval<%=rnd%> = null;
    const fmtRp = new Intl.NumberFormat('id-ID', { minimumFractionDigits: 0 });

    window.filterJurusan<%=rnd%> = function(fakultasId) {
        let selJurusan = document.getElementById('fJurusan<%=rnd%>');
        if (selJurusan.classList.contains('input-locked-<%=rnd%>')) return;
        
        let options = selJurusan.options;
        selJurusan.value = ''; 
        
        for (let i = 1; i < options.length; i++) { 
            let optionFakId = options[i].getAttribute('data-fak-id');
            if (fakultasId === '' || optionFakId === fakultasId) {
                options[i].style.display = 'block';
            } else {
                options[i].style.display = 'none';
            }
        }
    };

    window.toggleCustomDate<%=rnd%> = function() {
        const val = document.getElementById('fRentangWaktu<%=rnd%>').value;
        const customElems = document.querySelectorAll('.custom-date-<%=rnd%>');
        if (val === 'custom') {
            customElems.forEach(function(el) { el.style.display = 'block'; });
        } else {
            customElems.forEach(function(el) { el.style.display = 'none'; });
        }
    };

    window.toggleFilterKhusus<%=rnd%> = function() {
        const sel = document.getElementById('fDashboardType<%=rnd%>').value;
        const isProsesTagihan = sel === '_service_proses_tagihan';
        const isRinci = sel === '_service_kartu_piutang_rinci';
        const isTrend = sel === '_service_trend_pembayaran';
        const isItemBiaya = sel === '_service_piutang_item_biaya';
        const isTopDebtors = sel === '_service_top_debtors';
        const isDistribusi = sel === '_service_distribusi_pelunasan';
        const isArusKas = sel === '_service_arus_kas';
        const isKanal = sel === '_service_kanal_pembayaran';
        const isLedgerMhs = sel === '_service_ledger_mahasiswa';

        const advWrapper = document.getElementById('wrapperAdvancedFilter<%=rnd%>');
        if (isProsesTagihan || isRinci || isTrend || isItemBiaya || isTopDebtors || isDistribusi || isArusKas || isKanal || isLedgerMhs) {
            advWrapper.style.display = 'block';
        } else {
            advWrapper.style.display = 'none';
        }

        // Toggles untuk Kolom Pencarian
        document.getElementById('colSearch<%=rnd%>').style.display = (isRinci || isProsesTagihan || isItemBiaya || isTopDebtors || isDistribusi || isArusKas || isKanal || isLedgerMhs) ? 'block' : 'none';
        
        // Modifikasi Visual Label Wajib jika Opsi 15 (Mendukung Nama)
        const fSearchInput = document.getElementById('fSearchInput<%=rnd%>');
        const lblSearch = document.getElementById('lblSearch<%=rnd%>');
        if (isLedgerMhs) {
            lblSearch.innerHTML = '<i class="fas fa-search me-1 text-danger"></i> <%= Common.getBahasaConfig("Wajib Isi: NIM / Nama Mahasiswa") %> *';
            fSearchInput.placeholder = '<%= Common.getBahasaConfigJS("Ketik NIM atau Nama...") %>';
            fSearchInput.required = true;
            document.getElementById('rowProdiFakultas<%=rnd%>').style.display = 'none';
            document.getElementById('titleFilterLanjutan<%=rnd%>').innerText = '<%= Common.getBahasaConfigJS("Pencarian Spesifik Individu (Buku Besar)") %>';
        } else {
            lblSearch.innerHTML = '<i class="fas fa-search me-1 text-primary"></i> <%= Common.getBahasaConfig("Identitas (NIM/Nama)") %>';
            fSearchInput.placeholder = '<%= Common.getBahasaConfigJS("Kata kunci pencarian...") %>';
            fSearchInput.required = false;
            document.getElementById('rowProdiFakultas<%=rnd%>').style.display = 'flex';
            document.getElementById('titleFilterLanjutan<%=rnd%>').innerText = '<%= Common.getBahasaConfigJS("Parameter Lanjutan Khusus Dasbor") %>';
            document.getElementById('fSearchDropdown<%=rnd%>').style.display = 'none';
        }

        document.getElementById('colHanyaBelumLunas<%=rnd%>').style.display = isLedgerMhs ? 'block' : 'none';
        document.getElementById('colPagingStart<%=rnd%>').style.display = (isRinci || isTopDebtors) ? 'block' : 'none';
        document.getElementById('colPagingLimit<%=rnd%>').style.display = (isRinci || isTopDebtors) ? 'block' : 'none';
        
        document.getElementById('colTaSampai<%=rnd%>').style.display = isProsesTagihan ? 'block' : 'none';
        document.getElementById('colAngkatanMulai<%=rnd%>').style.display = isProsesTagihan ? 'block' : 'none';
        document.getElementById('colAngkatanSampai<%=rnd%>').style.display = isProsesTagihan ? 'block' : 'none';
        document.getElementById('colWorker<%=rnd%>').style.display = isProsesTagihan ? 'block' : 'none';
        document.getElementById('colHitungUlang<%=rnd%>').style.display = isProsesTagihan ? 'block' : 'none';
        
        document.getElementById('colRentangWaktu<%=rnd%>').style.display = (isTrend || isKanal) ? 'block' : 'none';
        window.toggleCustomDate<%=rnd%>(); 
        
        document.getElementById('fPage<%=rnd%>').value = "1";

        if (isProsesTagihan) {
            document.getElementById('txtBtnSubmit<%=rnd%>').innerText = '<%= Common.getBahasaConfigJS("Jalankan Proses Perhitungan Keuangan") %>';
            document.getElementById('btnSubmitDasbor<%=rnd%>').classList.replace('btn-primary', 'btn-danger');
            
            document.getElementById('chartContainerWrapper<%=rnd%>').style.display = 'none';
            document.getElementById('progressContainer<%=rnd%>').style.display = 'none';
        } else {
            document.getElementById('txtBtnSubmit<%=rnd%>').innerText = isLedgerMhs ? '<%= Common.getBahasaConfigJS("Tarik Data Buku Besar") %>' : '<%= Common.getBahasaConfigJS("Terapkan dan Muat Dasbor") %>';
            document.getElementById('btnSubmitDasbor<%=rnd%>').classList.replace('btn-danger', 'btn-primary');
            
            window.eksekusiDasbor<%=rnd%>();
        }
    };

    // Logika Search Autocomplete untuk Mahasiswa
    (function() {
        const searchInput = document.getElementById('fSearchInput<%=rnd%>');
        const searchDropdown = document.getElementById('fSearchDropdown<%=rnd%>');
        let searchTimeout;

        searchInput.addEventListener('input', function() {
            clearTimeout(searchTimeout);
            const val = this.value.trim();
            if (document.getElementById('fDashboardType<%=rnd%>').value !== '_service_ledger_mahasiswa') {
                searchDropdown.style.display = 'none';
                return;
            }

            if (val.length < 3) {
                searchDropdown.style.display = 'none';
                return;
            }

            searchTimeout = setTimeout(() => {
                const url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=pembayaran&s=_service_ledger_mahasiswa&action=SEARCH_MHS&q=' + encodeURIComponent(val);
                fetch(url).then(r => r.json()).then(res => {
                    if (res.status === '00' && res.data.length > 0) {
                        // PERBAIKAN: Menggunakan string concatenation murni untuk menghindari bentrokan dengan Expression Language JSP
                        searchDropdown.innerHTML = res.data.map(function(m) {
                            return '<button type="button" class="autocomplete-item-<%=rnd%> text-start" ' +
                                'onclick="document.getElementById(\'fSearchInput<%=rnd%>\').value = \'' + m.nim + '\'; document.getElementById(\'fSearchDropdown<%=rnd%>\').style.display=\'none\'; window.eksekusiDasbor<%=rnd%>();">' +
                                '<div class="fw-bold text-primary">' + m.nim + '</div>' +
                                '<small class="text-secondary">' + m.nama + '</small>' +
                            '</button>';
                        }).join('');
                        searchDropdown.style.display = 'block';
                    } else {
                        searchDropdown.innerHTML = '<span class="autocomplete-item-<%=rnd%> text-muted text-center py-2"><i class="fas fa-search-minus me-2"></i>Tidak ada data ditemukan</span>';
                        searchDropdown.style.display = 'block';
                    }
                }).catch(e => { searchDropdown.style.display = 'none'; });
            }, 400);
        });

        document.addEventListener('click', function(e) {
            if (!searchInput.contains(e.target) && !searchDropdown.contains(e.target)) {
                searchDropdown.style.display = 'none';
            }
        });
    })();

    window.eksekusiDasbor<%=rnd%> = function() {
        const sel = document.getElementById('fDashboardType<%=rnd%>').value;
        const qSearch = document.getElementById('fSearchInput<%=rnd%>').value.trim();
        
        if (sel === '_service_ledger_mahasiswa' && qSearch === '') {
            return; 
        }

        if (sel === '_service_kartu_piutang_rinci' || sel === '_service_top_debtors') {
            const limitVal = parseInt(document.getElementById('fLimit<%=rnd%>').value);
            if (limitVal > 5000) {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Batas maksimal data yang ditampilkan adalah 5.000 baris.") %>', 'bg-warning text-dark');
                document.getElementById('fLimit<%=rnd%>').value = 5000;
                return;
            }
        }

        if (sel === '_service_proses_tagihan') {
            window.mulaiProsesTagihanBackground<%=rnd%>();
        } else {
            window.loadDataDasbor<%=rnd%>();
        }
    };

    window.gantiHalaman<%=rnd%> = function(increment) {
        let currentPageInput = document.getElementById('fPage<%=rnd%>');
        let newPage = parseInt(currentPageInput.value) + increment;
        if (newPage < 1) newPage = 1;
        
        currentPageInput.value = newPage;
        window.eksekusiDasbor<%=rnd%>();
    }

    window.mulaiProsesTagihanBackground<%=rnd%> = function() {
        const form = document.getElementById('formMonitorPembayaran<%=rnd%>');
        const formData = new URLSearchParams(new FormData(form));
        formData.set('action', 'START');

        const btn = document.getElementById('btnSubmitDasbor<%=rnd%>');
        const progressCont = document.getElementById('progressContainer<%=rnd%>');
        const wrapper = document.getElementById('chartContainerWrapper<%=rnd%>');
        
        btn.disabled = true; progressCont.style.display = 'block'; wrapper.style.display = 'none';
        document.getElementById('loadingContainer<%=rnd%>').style.display = 'none';
        document.getElementById('progressBar<%=rnd%>').style.width = '0%'; document.getElementById('progressBar<%=rnd%>').innerText = '0%';
        document.getElementById('progressStatusText<%=rnd%>').innerText = '<%= Common.getBahasaConfigJS("Memulai proses perhitungan latar belakang...") %>';
        
        document.getElementById('lblDataSelesai<%=rnd%>').innerText = '0'; document.getElementById('lblDataTotal<%=rnd%>').innerText = '0'; document.getElementById('lblDataSisa<%=rnd%>').innerText = '0';
        document.getElementById('workersProgressContainer<%=rnd%>').innerHTML = '';

        const url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=pembayaran&s=_service_proses_tagihan';
        
        fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: formData.toString() })
        .then(async res => { const t = await res.text(); try { return JSON.parse(t); } catch(e) { throw new Error(t); } })
        .then(result => {
            if(result && result.status === 'started') {
                const jobId = result.jobId; jobInterval<%=rnd%> = setInterval(function() { window.pollProgressJob<%=rnd%>(jobId); }, 1000);
            } else {
                btn.disabled = false; progressCont.style.display = 'none';
                if(typeof tampilkanToast === 'function') tampilkanToast(result.message || '<%= Common.getBahasaConfigJS("Gagal memulai background job.") %>', 'bg-danger text-white');
            }
        }).catch(err => { btn.disabled = false; progressCont.style.display = 'none'; alert('<%= Common.getBahasaConfigJS("Galat memulai proses.") %>'); });
    };

    window.pollProgressJob<%=rnd%> = function(jobId) {
        const url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=pembayaran&s=_service_proses_tagihan';
        const formData = new URLSearchParams(); formData.append('action', 'PROGRESS'); formData.append('jobId', jobId);

        fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: formData.toString() })
        .then(res => res.json())
        .then(res => {
            if(res.status === '00') {
                let pBar = document.getElementById('progressBar<%=rnd%>');
                let cur = res.current; let tot = res.total; let pct = tot > 0 ? ((cur / tot) * 100).toFixed(1) : 0;
                
                pBar.style.width = pct + '%'; pBar.innerText = pct + '%';
                document.getElementById('progressStatusText<%=rnd%>').innerText = res.statusText;
                document.getElementById('lblDataSelesai<%=rnd%>').innerText = fmtRp.format(cur); document.getElementById('lblDataTotal<%=rnd%>').innerText = fmtRp.format(tot); document.getElementById('lblDataSisa<%=rnd%>').innerText = fmtRp.format(tot - cur);
                if (res.workersHtml) document.getElementById('workersProgressContainer<%=rnd%>').innerHTML = res.workersHtml;
                if (res.isDone) { clearInterval(jobInterval<%=rnd%>); document.getElementById('progressStatusText<%=rnd%>').innerText = '<%= Common.getBahasaConfigJS("Menyiapkan tabel rincian hasil komputasi...") %>'; window.fetchResultJob<%=rnd%>(jobId); }
            }
        });
    };

    window.fetchResultJob<%=rnd%> = function(jobId) {
        const url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=pembayaran&s=_service_proses_tagihan';
        const formData = new URLSearchParams(); formData.append('action', 'RESULT'); formData.append('jobId', jobId);

        fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: formData.toString() })
        .then(res => res.json())
        .then(res => {
            document.getElementById('btnSubmitDasbor<%=rnd%>').disabled = false;
            document.getElementById('progressContainer<%=rnd%>').style.display = 'none';
            document.getElementById('chartContainerWrapper<%=rnd%>').style.display = 'block';
            document.getElementById('grafikRowWrapper<%=rnd%>').style.display = 'none'; 
            document.getElementById('cardTabelBiasa<%=rnd%>').style.display = 'block';
            document.getElementById('containerBukuBesarMahasiswa<%=rnd%>').style.display = 'none';
            document.getElementById('paginationControls<%=rnd%>').style.display = 'none';
            document.getElementById('footerTotalInfo<%=rnd%>').style.display = 'none';
            
            document.getElementById('labelTabel<%=rnd%>').innerText = '<%= Common.getBahasaConfigJS("Hasil Kalkulasi Rinci Tagihan Mahasiswa") %>';
            document.getElementById('tableContainer<%=rnd%>').innerHTML = res.html;
        });
    };

    window.hitungUlangMhs<%=rnd%> = function(nim) {
        if (!confirm('<%= Common.getBahasaConfigJS("Apakah Anda yakin ingin menghitung ulang seluruh tagihan untuk mahasiswa ini?") %>')) return;

        const formData = new URLSearchParams();
        formData.append('jenisDashboard', '_service_proses_tagihan');
        formData.append('q', nim);
        formData.append('hitungUlang', 'true');
        formData.append('action', 'START');

        document.getElementById('chartContainerWrapper<%=rnd%>').style.display = 'none';
        document.getElementById('cardTabelBiasa<%=rnd%>').style.display = 'none';
        document.getElementById('containerBukuBesarMahasiswa<%=rnd%>').style.display = 'none';

        const btn = document.getElementById('btnSubmitDasbor<%=rnd%>');
        const progressCont = document.getElementById('progressContainer<%=rnd%>');
        
        btn.disabled = true; progressCont.style.display = 'block';
        document.getElementById('loadingContainer<%=rnd%>').style.display = 'none';
        document.getElementById('progressBar<%=rnd%>').style.width = '0%'; document.getElementById('progressBar<%=rnd%>').innerText = '0%';
        document.getElementById('progressStatusText<%=rnd%>').innerText = '<%= Common.getBahasaConfigJS("Memulai proses perhitungan latar belakang untuk mahasiswa...") %>';
        
        document.getElementById('lblDataSelesai<%=rnd%>').innerText = '0'; document.getElementById('lblDataTotal<%=rnd%>').innerText = '0'; document.getElementById('lblDataSisa<%=rnd%>').innerText = '0';
        document.getElementById('workersProgressContainer<%=rnd%>').innerHTML = '';

        const url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=pembayaran&s=_service_proses_tagihan';
        
        fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: formData.toString() })
        .then(res => res.json())
        .then(result => {
            if(result && result.status === 'started') {
                const jobId = result.jobId; 
                jobInterval<%=rnd%> = setInterval(function() { window.pollProgressJobMhs<%=rnd%>(jobId, nim); }, 1000);
            } else {
                btn.disabled = false; progressCont.style.display = 'none';
                if(typeof tampilkanToast === 'function') tampilkanToast(result.message || '<%= Common.getBahasaConfigJS("Gagal memulai background job.") %>', 'bg-danger text-white');
            }
        }).catch(err => { btn.disabled = false; progressCont.style.display = 'none'; alert('<%= Common.getBahasaConfigJS("Galat memulai proses.") %>'); });
    };

    window.pollProgressJobMhs<%=rnd%> = function(jobId, nim) {
        const url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=pembayaran&s=_service_proses_tagihan';
        const formData = new URLSearchParams(); formData.append('action', 'PROGRESS'); formData.append('jobId', jobId);

        fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: formData.toString() })
        .then(res => res.json())
        .then(res => {
            if(res.status === '00') {
                let pBar = document.getElementById('progressBar<%=rnd%>');
                let cur = res.current; let tot = res.total; let pct = tot > 0 ? ((cur / tot) * 100).toFixed(1) : 0;
                
                pBar.style.width = pct + '%'; pBar.innerText = pct + '%';
                document.getElementById('progressStatusText<%=rnd%>').innerText = res.statusText;
                document.getElementById('lblDataSelesai<%=rnd%>').innerText = fmtRp.format(cur); document.getElementById('lblDataTotal<%=rnd%>').innerText = fmtRp.format(tot); document.getElementById('lblDataSisa<%=rnd%>').innerText = fmtRp.format(tot - cur);
                
                if (res.isDone) { 
                    clearInterval(jobInterval<%=rnd%>); 
                    if(typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Perhitungan ulang berhasil.") %>', 'bg-success text-white');
                    document.getElementById('progressContainer<%=rnd%>').style.display = 'none';
                    document.getElementById('btnSubmitDasbor<%=rnd%>').disabled = false;
                    document.getElementById('fSearchInput<%=rnd%>').value = nim;
                    window.eksekusiDasbor<%=rnd%>();
                }
            }
        });
    };

    window.loadDataDasbor<%=rnd%> = function() {
        const form = document.getElementById('formMonitorPembayaran<%=rnd%>');
        const formData = new URLSearchParams(new FormData(form));
        
        const loader = document.getElementById('loadingContainer<%=rnd%>');
        const wrapper = document.getElementById('chartContainerWrapper<%=rnd%>');
        const progressCont = document.getElementById('progressContainer<%=rnd%>');
        
        const serviceEndpoint = formData.get('jenisDashboard');
        const isRinci = serviceEndpoint === '_service_kartu_piutang_rinci';
        const isTrend = serviceEndpoint === '_service_trend_pembayaran';
        const isUmur = serviceEndpoint === '_service_umur_piutang';
        const isTopDebtors = serviceEndpoint === '_service_top_debtors';
        const isDistribusi = serviceEndpoint === '_service_distribusi_pelunasan';
        const isArusKas = serviceEndpoint === '_service_arus_kas';
        const isKanal = serviceEndpoint === '_service_kanal_pembayaran';
        const isLedgerMhs = serviceEndpoint === '_service_ledger_mahasiswa';
        
        loader.style.display = 'block'; wrapper.style.display = 'none'; progressCont.style.display = 'none';
        
        const url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=pembayaran&s=' + serviceEndpoint;
        
        fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: formData.toString() })
        .then(async res => { const text = await res.text(); try { return JSON.parse(text); } catch(e) { throw new Error(text); } })
        .then(result => {
            loader.style.display = 'none';
            if(result && result.status === '00') {
                wrapper.style.display = 'block';
                
                if (isLedgerMhs) {
                    document.getElementById('grafikRowWrapper<%=rnd%>').style.display = 'none';
                    document.getElementById('cardTabelBiasa<%=rnd%>').style.display = 'none';
                    document.getElementById('containerBukuBesarMahasiswa<%=rnd%>').style.display = 'block';
                    
                    window.renderLedgerMahasiswa<%=rnd%>(result);
                } else {
                    document.getElementById('cardTabelBiasa<%=rnd%>').style.display = 'block';
                    document.getElementById('containerBukuBesarMahasiswa<%=rnd%>').style.display = 'none';
                    document.getElementById('tableContainer<%=rnd%>').innerHTML = '<table class="table table-hover table-bordered align-middle mb-0 table-sticky-header-<%=rnd%>" id="tabelRekapDasbor<%=rnd%>" style="width: 100%; font-size: 0.85rem; white-space: nowrap;"><thead class="table-dark text-center" id="theadRekap<%=rnd%>"></thead><tbody id="tbodyRekap<%=rnd%>"></tbody><tfoot class="table-secondary fw-bold text-end" id="tfootRekap<%=rnd%>"></tfoot></table>';

                    const sel = document.getElementById('fDashboardType<%=rnd%>');
                    const titleDasbor = sel.options[sel.selectedIndex].text.substring(4);
                    
                    if (isRinci) {
                        document.getElementById('grafikRowWrapper<%=rnd%>').style.display = 'none'; 
                        document.getElementById('labelTabel<%=rnd%>').innerText = '<%= Common.getBahasaConfigJS("Data Kartu Piutang Rinci Mahasiswa") %>';
                        window.renderTabelRinci<%=rnd%>(result);
                    } else if (isTrend) {
                        document.getElementById('grafikRowWrapper<%=rnd%>').style.display = 'flex'; 
                        document.getElementById('labelTabel<%=rnd%>').innerText = '<%= Common.getBahasaConfigJS("Rincian Tren Pembayaran Mahasiswa") %>';
                        document.getElementById('paginationControls<%=rnd%>').style.display = 'none';
                        document.getElementById('footerTotalInfo<%=rnd%>').style.display = 'none';
                        window.renderTrendPembayaran<%=rnd%>(result);
                    } else if (isUmur) {
                        document.getElementById('grafikRowWrapper<%=rnd%>').style.display = 'flex'; 
                        document.getElementById('labelTabel<%=rnd%>').innerText = '<%= Common.getBahasaConfigJS("Rincian Data Umur Piutang Mahasiswa") %>';
                        document.getElementById('paginationControls<%=rnd%>').style.display = 'none';
                        document.getElementById('footerTotalInfo<%=rnd%>').style.display = 'none';
                        window.renderUmurPiutang<%=rnd%>(result.data, formData.get('tahunAkademik'), formData.get('semester'));
                    } else if (isTopDebtors) {
                        document.getElementById('grafikRowWrapper<%=rnd%>').style.display = 'none'; 
                        document.getElementById('labelTabel<%=rnd%>').innerText = '<%= Common.getBahasaConfigJS("Peringkat Penunggak Terbesar (Top Debtors)") %>';
                        window.renderTopDebtors<%=rnd%>(result);
                    } else if (isDistribusi) {
                        document.getElementById('grafikRowWrapper<%=rnd%>').style.display = 'flex'; 
                        document.getElementById('labelTabel<%=rnd%>').innerText = '<%= Common.getBahasaConfigJS("Rincian Peta Distribusi Headcount Pelunasan") %>';
                        document.getElementById('paginationControls<%=rnd%>').style.display = 'none';
                        document.getElementById('footerTotalInfo<%=rnd%>').style.display = 'none';
                        window.renderDistribusiPelunasan<%=rnd%>(result.data, formData.get('tahunAkademik'), formData.get('semester'));
                    } else if (isArusKas) {
                        document.getElementById('grafikRowWrapper<%=rnd%>').style.display = 'flex'; 
                        document.getElementById('labelTabel<%=rnd%>').innerText = '<%= Common.getBahasaConfigJS("Rincian Target vs Realisasi Arus Kas") %>';
                        document.getElementById('paginationControls<%=rnd%>').style.display = 'none';
                        document.getElementById('footerTotalInfo<%=rnd%>').style.display = 'none';
                        window.renderArusKas<%=rnd%>(result.data, formData.get('tahunAkademik'), formData.get('semester'));
                    } else if (isKanal) {
                        document.getElementById('grafikRowWrapper<%=rnd%>').style.display = 'flex'; 
                        document.getElementById('labelTabel<%=rnd%>').innerText = '<%= Common.getBahasaConfigJS("Rekapitulasi Kanal / Metode Pembayaran") %>';
                        document.getElementById('paginationControls<%=rnd%>').style.display = 'none';
                        document.getElementById('footerTotalInfo<%=rnd%>').style.display = 'none';
                        window.renderKanalPembayaran<%=rnd%>(result.data, formData.get('tahunAkademik'), formData.get('semester'));
                    } else {
                        document.getElementById('grafikRowWrapper<%=rnd%>').style.display = 'flex'; 
                        document.getElementById('labelTabel<%=rnd%>').innerText = '<%= Common.getBahasaConfigJS("Rincian Data Tabular") %>';
                        document.getElementById('paginationControls<%=rnd%>').style.display = 'none';
                        document.getElementById('footerTotalInfo<%=rnd%>').style.display = 'none';
                        
                        let headerTitle = '<%= Common.getBahasaConfigJS("Program Studi") %>';
                        if(serviceEndpoint.includes('angkatan')) headerTitle = '<%= Common.getBahasaConfigJS("Tahun Angkatan") %>';
                        else if(serviceEndpoint.includes('status')) headerTitle = '<%= Common.getBahasaConfigJS("Status Awal Masuk") %>';
                        else if(serviceEndpoint.includes('program')) headerTitle = '<%= Common.getBahasaConfigJS("Program Kelas") %>';
                        else if(serviceEndpoint.includes('validator')) headerTitle = '<%= Common.getBahasaConfigJS("Validator Transaksi") %>';
                        else if(serviceEndpoint.includes('item_biaya')) headerTitle = '<%= Common.getBahasaConfigJS("Komponen Item Biaya") %>';

                        window.renderGrafikDanTabelAgregat<%=rnd%>(result.data, titleDasbor, formData.get('tahunAkademik'), formData.get('semester'), headerTitle);
                    }
                }

            } else {
                if(typeof tampilkanToast === 'function') tampilkanToast(result.message || '<%= Common.getBahasaConfigJS("Gagal memuat data dasbor.") %>', 'bg-danger text-white');
            }
        })
        .catch(err => { loader.style.display = 'none'; if(typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Terjadi galat koneksi ke peladen.") %>', 'bg-danger text-white'); });
    };

    window.renderLedgerMahasiswa<%=rnd%> = function(result) {
        const container = document.getElementById('containerBukuBesarMahasiswa<%=rnd%>');
        const profile = result.profilMahasiswa;
        const dataRinci = result.data;
        
        let htmlBukuBesar = '';

        htmlBukuBesar += '<div class="card border-0 shadow-sm rounded-4 mb-4 bg-primary bg-gradient text-white">' +
            '<div class="card-body p-4 d-flex align-items-center justify-content-between flex-wrap gap-3">' +
                '<div class="d-flex align-items-center">' +
                    '<div class="bg-white bg-opacity-25 rounded-circle d-flex align-items-center justify-content-center me-4 shadow-sm" style="width: 70px; height: 70px;">' +
                        '<i class="fas fa-user-graduate fs-2"></i>' +
                    '</div>' +
                    '<div>' +
                        '<h4 class="fw-bold mb-1">' + profile.nama + ' <span class="badge bg-white text-primary ms-2 fs-6">' + profile.nim + '</span></h4>' +
                        '<div class="d-flex flex-wrap gap-3 mt-2" style="font-size: 0.85rem;">' +
                            '<span><i class="fas fa-university me-1 text-warning"></i> ' + profile.prodi + ' (' + profile.jenjang + ')</span>' +
                            '<span><i class="fas fa-calendar-alt me-1 text-info"></i> <%= Common.getBahasaConfig("Angkatan") %> ' + profile.tahunMasuk + '</span>' +
                            '<span><i class="fas fa-layer-group me-1 text-light"></i> ' + profile.program + ' - ' + profile.statusAwal + '</span>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
                '<div>' +
                    '<button type="button" class="btn btn-light text-danger fw-bold rounded-pill shadow-sm px-4" onclick="window.hitungUlangMhs<%=rnd%>(\'' + profile.nim + '\')">' +
                        '<i class="fas fa-sync-alt me-2"></i><%= Common.getBahasaConfig("Hitung Ulang Tagihan") %>' +
                    '</button>' +
                '</div>' +
            '</div>' +
        '</div>';

        if (dataRinci.length === 0) {
            htmlBukuBesar += '<div class="alert alert-info text-center p-5 shadow-sm rounded-4 border-0">' +
                    '<i class="fas fa-check-circle fa-4x mb-3 text-success opacity-50"></i>' +
                    '<h5 class="fw-bold text-dark"><%= Common.getBahasaConfig("Tidak ada data tagihan yang sesuai.") %></h5>' +
                    '<p class="text-secondary"><%= Common.getBahasaConfig("Mahasiswa ini mungkin sudah lunas atau belum diterbitkan tagihannya untuk kriteria yang dipilih.") %></p>' +
                '</div>';
            container.innerHTML = htmlBukuBesar;
            return;
        }

        dataRinci.forEach(function(grup) {
            let htmlTbody = '';
            let isSemuaLunas = true;

            grup.rincian.forEach(function(item) {
                let lunasIcon = item.sisa <= 0.01 
                    ? '<i class="fas fa-check-circle text-success" title="<%= Common.getBahasaConfig("Lunas") %>"></i>'
                    : '<i class="fas fa-clock text-warning" title="<%= Common.getBahasaConfig("Belum Lunas") %>"></i>';
                
                if (item.sisa > 0.01) isSemuaLunas = false;

                let dendaBadge = '';
                if (item.denda > 0) dendaBadge = ' <span class="badge bg-danger rounded-pill ms-1" style="font-size:0.65rem;">Denda: Rp ' + fmtRp.format(item.denda) + '</span>';

                htmlTbody += '<tr>' +
                        '<td class="text-center bg-light">' + lunasIcon + '</td>' +
                        '<td class="fw-bold text-dark">' + item.namaItem + dendaBadge + '</td>' +
                        '<td class="text-end fw-semibold text-secondary d-none d-md-table-cell">' + fmtRp.format(item.tagihanAwal) + '</td>' +
                        '<td class="text-end text-success d-none d-md-table-cell"><small>-' + fmtRp.format(item.potonganBeasiswa) + '</small></td>' +
                        '<td class="text-end fw-bold text-primary">' + fmtRp.format(item.tagihanAkhir) + '</td>' +
                        '<td class="text-end fw-bold text-success bg-success bg-opacity-10">' + fmtRp.format(item.dibayar) + '</td>' +
                        '<td class="text-end fw-bold ' + (item.sisa > 0 ? 'text-danger' : 'text-dark') + '">' + fmtRp.format(item.sisa) + '</td>' +
                        '<td class="text-center fw-bold ' + (item.persen >= 100 ? 'text-success' : 'text-primary') + '">' + item.persen + '%</td>' +
                    '</tr>';
            });

            let statusKeseluruhanTeks = isSemuaLunas ? '<span class="badge bg-success fs-6"><i class="fas fa-check me-1"></i> <%= Common.getBahasaConfig("LUNAS") %></span>' : '<span class="badge bg-warning text-dark fs-6"><i class="fas fa-exclamation-circle me-1"></i> <%= Common.getBahasaConfig("BELUM LUNAS") %></span>';

            htmlBukuBesar += '<div class="card border-0 shadow-sm rounded-4 mb-4 overflow-hidden">' +
                    '<div class="card-header bg-white border-bottom p-3 d-flex justify-content-between align-items-center">' +
                        '<div>' +
                            '<h6 class="fw-bold text-dark mb-1"><i class="fas fa-folder-open text-primary me-2"></i>' + grup.jenisKegiatanNama + '</h6>' +
                            '<div class="small text-secondary fw-semibold">' +
                                '<span class="me-3"><i class="fas fa-layer-group me-1"></i><%= Common.getBahasaConfig("Semester") %>: <b class="text-dark">' + grup.semester + '</b></span>' +
                                '<span><i class="far fa-calendar-alt me-1"></i>TA: ' + grup.tahunAkademik + '</span>' +
                            '</div>' +
                        '</div>' +
                        '<div class="text-end">' +
                            statusKeseluruhanTeks +
                        '</div>' +
                    '</div>' +
                    '<div class="table-responsive">' +
                        '<table class="table table-hover align-middle mb-0" style="font-size: 0.85rem;">' +
                            '<thead class="table-dark text-center">' +
                                '<tr>' +
                                    '<th style="width: 40px;"><i class="fas fa-shield-alt"></i></th>' +
                                    '<th class="text-start"><%= Common.getBahasaConfig("Deskripsi Item Biaya") %></th>' +
                                    '<th class="text-end d-none d-md-table-cell"><%= Common.getBahasaConfig("Tagihan (Rp)") %></th>' +
                                    '<th class="text-end d-none d-md-table-cell"><%= Common.getBahasaConfig("Potongan") %></th>' +
                                    '<th class="text-end text-primary"><%= Common.getBahasaConfig("Net Tagihan") %></th>' +
                                    '<th class="text-end text-success"><%= Common.getBahasaConfig("Dibayar") %></th>' +
                                    '<th class="text-end text-danger"><%= Common.getBahasaConfig("Sisa / Hutang") %></th>' +
                                    '<th class="text-center"><%= Common.getBahasaConfig("% Lunas") %></th>' +
                                '</tr>' +
                            '</thead>' +
                            '<tbody>' +
                                htmlTbody +
                            '</tbody>' +
                            '<tfoot class="bg-light border-top border-2">' +
                                '<tr>' +
                                    '<td colspan="4" class="text-end fw-bold text-uppercase d-none d-md-table-cell"><%= Common.getBahasaConfig("Total Akumulasi") %></td>' +
                                    '<td colspan="2" class="text-end fw-bold text-uppercase d-table-cell d-md-none"><%= Common.getBahasaConfig("Total Akumulasi") %></td>' +
                                    '<td class="text-end fw-bolder text-primary fs-6">' + fmtRp.format(grup.totalTagihanAkhir) + '</td>' +
                                    '<td class="text-end fw-bolder text-success fs-6">' + fmtRp.format(grup.totalDibayar) + '</td>' +
                                    '<td class="text-end fw-bolder ' + (grup.totalSisa > 0 ? 'text-danger' : 'text-dark') + ' fs-6">' + fmtRp.format(grup.totalSisa) + '</td>' +
                                    '<td class="text-center fw-bolder ' + (grup.persenTotal >= 100 ? 'text-success' : 'text-primary') + '">' + grup.persenTotal + '%</td>' +
                                '</tr>' +
                            '</tfoot>' +
                        '</table>' +
                    '</div>' +
                '</div>';
        });

        container.innerHTML = htmlBukuBesar;
    };

    window.renderKanalPembayaran<%=rnd%> = function(dataArr, ta, smt) {
        document.getElementById('colChart1<%=rnd%>').className = 'col-lg-6';
        document.getElementById('colChart2<%=rnd%>').className = 'col-lg-6';
        document.getElementById('colChart2<%=rnd%>').style.display = 'block';

        let smtText = (smt === "") ?
            "<%= Common.getBahasaConfig("Semua Semester") %>" : "<%= Common.getBahasaConfig("Smt") %> " + smt;
        document.getElementById('titleChartPiutang<%=rnd%>').innerHTML = '<i class="fas fa-chart-pie me-2"></i> <%= Common.getBahasaConfig("Komposisi Volume Transaksi") %>';
        document.getElementById('titleChartLunas<%=rnd%>').innerHTML = '<i class="fas fa-chart-bar me-2"></i> <%= Common.getBahasaConfig("Distribusi Nominal Pembayaran (Rp)") %>';
        document.getElementById('theadRekap<%=rnd%>').innerHTML = '<tr>' +
            '<th><%= Common.getBahasaConfig("Kanal / Metode Pembayaran") %></th>' +
            '<th class="text-end bg-primary bg-opacity-10"><i class="fas fa-receipt text-primary me-1"></i> <%= Common.getBahasaConfig("Jumlah Transaksi") %></th>' +
            '<th class="text-center bg-primary bg-opacity-10"><%= Common.getBahasaConfig("% Transaksi") %></th>' +
            '<th class="text-end bg-success text-black bg-opacity-10"><i class="fas fa-money-bill-wave text-success me-1"></i> <%= Common.getBahasaConfig("Total Nominal Masuk (Rp)") %></th>' +
            '<th class="text-center bg-success text-black bg-opacity-10"><%= Common.getBahasaConfig("% Nominal") %></th>' +
        '</tr>';
        let labels = []; 
        let dTrx = []; 
        let dNominal = []; 
        let htmlTbody = '';
        let grandTrx = 0, grandNominal = 0;

        if (dataArr.length === 0) {
            htmlTbody = '<tr><td colspan="5" class="text-center py-4 text-muted fw-bold"><i class="fas fa-box-open fa-2x mb-3 mt-2 opacity-50"></i><br><%= Common.getBahasaConfig("Tidak ada data transaksi pembayaran ditemukan pada kriteria ini.") %></td></tr>';
        } else {
            dataArr.forEach(function(r) {
                grandTrx += r.jmlTrx;
                grandNominal += r.nominal;
            });
            dataArr.forEach(function(r) {
                labels.push(r.kanal); 
                dTrx.push(r.jmlTrx);
                dNominal.push(r.nominal);
                
                let pctTrx = grandTrx > 0 ? (r.jmlTrx * 100.0) / grandTrx : 0;
                let pctNom = grandNominal > 0 ? (r.nominal * 100.0) / grandNominal : 0;

                htmlTbody += '<tr>' +
                    '<td class="fw-bold">' + r.kanal + '</td>' +
                    '<td class="text-end text-primary fw-bold" data-t="n" data-v="' + r.jmlTrx + '">' + fmtRp.format(r.jmlTrx) + '</td>' +
                    '<td class="text-center text-primary" data-t="n" data-v="' + pctTrx + '">' + pctTrx.toFixed(2) + '%</td>' +
                    '<td class="text-end text-success fw-bold" data-t="n" data-v="' + r.nominal + '">' + fmtRp.format(r.nominal) + '</td>' +
                    '<td class="text-center text-success" data-t="n" data-v="' + pctNom + '">' + pctNom.toFixed(2) + '%</td>' +
                '</tr>';
            });
        }

        document.getElementById('tbodyRekap<%=rnd%>').innerHTML = htmlTbody;
        document.getElementById('tfootRekap<%=rnd%>').innerHTML = '<tr>' +
            '<td class="text-center text-uppercase"><%= Common.getBahasaConfig("Total Akumulasi Keseluruhan") %></td>' +
            '<td class="text-primary fw-bold fs-6" data-t="n" data-v="' + grandTrx + '">' + fmtRp.format(grandTrx) + '</td>' +
            '<td class="text-primary fw-bold fs-6">100%</td>' +
            '<td class="text-success fw-bold fs-6" data-t="n" data-v="' + grandNominal + '">' + fmtRp.format(grandNominal) + '</td>' +
            '<td class="text-success fw-bold fs-6">100%</td>' +
        '</tr>';

        if (chartP<%=rnd%>) chartP<%=rnd%>.destroy();
        if (chartL<%=rnd%>) chartL<%=rnd%>.destroy();
        
        const colorPalette = ['#0d6efd', '#198754', '#ffc107', '#dc3545', '#6f42c1', '#0dcaf0', '#fd7e14', '#20c997', '#6c757d', '#343a40'];
        chartP<%=rnd%> = new Chart(document.getElementById('chartPiutang<%=rnd%>').getContext('2d'), { 
            type: 'doughnut', 
            data: { 
                labels: labels, 
                datasets: [{ 
                    data: dTrx, 
                    backgroundColor: colorPalette, 
                    borderWidth: 2 
                }] 
            }, 
            options: { 
                responsive: true, 
                maintainAspectRatio: false, 
                plugins: { legend: { position: 'right' } } 
            } 
        });
        chartL<%=rnd%> = new Chart(document.getElementById('chartLunas<%=rnd%>').getContext('2d'), { 
            type: 'bar', 
            data: { 
                labels: labels, 
                datasets: [{ 
                    label: '<%= Common.getBahasaConfigJS("Nominal Masuk (Rp)") %>', 
                    data: dNominal, 
                    backgroundColor: 'rgba(25, 135, 84, 0.85)', 
                    borderColor: 'rgba(25, 135, 84, 1)', 
                    borderWidth: 1, 
                    borderRadius: 4 
                }] 
            }, 
            options: { 
                indexAxis: 'y', 
                responsive: true, 
                maintainAspectRatio: false, 
                plugins: { legend: { display: false } } 
            } 
        });
    };

    window.renderArusKas<%=rnd%> = function(dataObj, ta, smt) {
        document.getElementById('colChart1<%=rnd%>').className = 'col-lg-8';
        document.getElementById('colChart2<%=rnd%>').className = 'col-lg-4';
        document.getElementById('colChart2<%=rnd%>').style.display = 'block';

        let smtText = (smt === "") ?
            "<%= Common.getBahasaConfig("Semua Semester") %>" : "<%= Common.getBahasaConfig("Smt") %> " + smt;
        document.getElementById('titleChartPiutang<%=rnd%>').innerHTML = '<i class="fas fa-chart-area me-2"></i> <%= Common.getBahasaConfig("Progres Realisasi Arus Kas TA") %> ' + ta + ' (' + smtText + ')';
        document.getElementById('titleChartLunas<%=rnd%>').innerHTML = '<i class="fas fa-bullseye me-2"></i> <%= Common.getBahasaConfig("Total Capaian Target") %>';
        document.getElementById('theadRekap<%=rnd%>').innerHTML = '<tr>' +
            '<th><%= Common.getBahasaConfig("Bulan Transaksi") %></th>' +
            '<th class="text-end bg-primary bg-opacity-10"><%= Common.getBahasaConfig("Target Semester (Rp)") %></th>' +
            '<th class="text-end text-success"><%= Common.getBahasaConfig("Masuk Bulan Ini (Rp)") %></th>' +
            '<th class="text-end text-success bg-success bg-opacity-10"><%= Common.getBahasaConfig("Akumulasi Realisasi (Rp)") %></th>' +
            '<th class="text-end text-danger"><%= Common.getBahasaConfig("Sisa Target (Rp)") %></th>' +
            '<th class="text-center fw-bold"><%= Common.getBahasaConfig("% Ketercapaian") %></th>' +
        '</tr>';
        let labels = []; 
        let dBulanan = []; 
        let dAkumulasi = []; 
        let dTargetLine = [];
        let htmlTbody = '';
        let rincian = dataObj.rincian || [];
        let totalTarget = dataObj.target || 0;
        let grandAkumulasi = 0;
        if (rincian.length === 0) {
            htmlTbody = '<tr><td colspan="6" class="text-center py-4 text-muted fw-bold"><i class="fas fa-box-open fa-2x mb-3 mt-2 opacity-50"></i><br><%= Common.getBahasaConfig("Tidak ada data arus kas ditemukan pada kriteria ini.") %></td></tr>';
        } else {
            rincian.forEach(function(r) {
                labels.push(r.bulan); 
                dBulanan.push(r.nominal);
                dAkumulasi.push(r.akumulasi);
                dTargetLine.push(totalTarget);
                
                grandAkumulasi = r.akumulasi;
                let sisaBulan = totalTarget - r.akumulasi;
                if(sisaBulan < 0) sisaBulan = 0;

                htmlTbody += '<tr>' +
                    '<td class="fw-bold">' + r.bulan + '</td>' +
                    '<td class="text-end fw-bold text-primary" data-t="n" data-v="' + totalTarget + '">' + fmtRp.format(totalTarget) + '</td>' +
                    '<td class="text-end text-success" data-t="n" data-v="' + r.nominal + '">' + fmtRp.format(r.nominal) + '</td>' +
                    '<td class="text-end fw-bold text-success" data-t="n" data-v="' + r.akumulasi + '">' + fmtRp.format(r.akumulasi) + '</td>' +
                    '<td class="text-end text-danger" data-t="n" data-v="' + sisaBulan + '">' + fmtRp.format(sisaBulan) + '</td>' +
                    '<td class="text-center fw-bold ' + (r.persen >= 100 ?
                    'text-success' : 'text-primary') + '" data-t="n" data-v="' + r.persen + '">' + r.persen.toFixed(2) + '%</td>' +
                '</tr>';
            });
        }

        document.getElementById('tbodyRekap<%=rnd%>').innerHTML = htmlTbody;
        
        let finalSisa = totalTarget - grandAkumulasi;
        if(finalSisa < 0) finalSisa = 0;
        let finalPersen = totalTarget > 0 ? (grandAkumulasi * 100.0) / totalTarget : 0;

        document.getElementById('tfootRekap<%=rnd%>').innerHTML = '<tr>' +
            '<td class="text-center text-uppercase"><%= Common.getBahasaConfig("Total Pencapaian Akhir") %></td>' +
            '<td class="text-primary fw-bold fs-6" data-t="n" data-v="' + totalTarget + '">' + fmtRp.format(totalTarget) + '</td>' +
            '<td class="text-success fw-bold fs-6" data-t="n" data-v="' + grandAkumulasi + '">-</td>' +
            '<td class="text-success fw-bold fs-6" data-t="n" data-v="' + grandAkumulasi + '">' + fmtRp.format(grandAkumulasi) + '</td>' +
            '<td class="text-danger fw-bold fs-6" data-t="n" data-v="' + finalSisa + '">' + fmtRp.format(finalSisa) + '</td>' +
            '<td class="text-center fw-bold ' + (finalPersen >= 100 ?
            'text-success' : 'text-primary') + '" data-t="n" data-v="' + finalPersen + '">' + finalPersen.toFixed(2) + '%</td>' +
        '</tr>';

        if (chartP<%=rnd%>) chartP<%=rnd%>.destroy();
        if (chartL<%=rnd%>) chartL<%=rnd%>.destroy();
        
        chartP<%=rnd%> = new Chart(document.getElementById('chartPiutang<%=rnd%>').getContext('2d'), { 
            type: 'bar', 
            data: { 
                labels: labels, 
                datasets: [
                    {
                        type: 'line',
                        label: '<%= Common.getBahasaConfigJS("Target Semester (Rp)") %>',
                        data: dTargetLine,
                        borderColor: '#dc3545',
                        borderWidth: 2,
                        borderDash: [5, 5],
                        fill: false,
                        pointRadius: 0
                    },
                    {
                        type: 'line',
                        label: '<%= Common.getBahasaConfigJS("Akumulasi Realisasi (Rp)") %>',
                        data: dAkumulasi,
                        borderColor: '#0d6efd',
                        backgroundColor: 'rgba(13, 110, 253, 0.1)',
                        borderWidth: 3,
                        fill: true,
                        tension: 0.3
                    },
                    {
                        type: 'bar',
                        label: '<%= Common.getBahasaConfigJS("Realisasi Bulanan (Rp)") %>',
                        data: dBulanan,
                        backgroundColor: '#198754',
                        borderRadius: 4
                    }
                ] 
            }, 
            options: { 
                responsive: true, 
                maintainAspectRatio: false,
                interaction: { mode: 'index', intersect: false },
                plugins: { legend: { position: 'bottom' } },
                scales: { y: { beginAtZero: true } }
            } 
        });
        chartL<%=rnd%> = new Chart(document.getElementById('chartLunas<%=rnd%>').getContext('2d'), { 
            type: 'doughnut', 
            data: { 
                labels: ['<%= Common.getBahasaConfigJS("Realisasi Tercapai") %>', '<%= Common.getBahasaConfigJS("Sisa Target") %>'], 
                datasets: [{ 
                    data: [grandAkumulasi, finalSisa], 
                    backgroundColor: ['#0d6efd', '#e9ecef'], 
                    borderWidth: 2 
                }] 
            }, 
            options: { 
                responsive: true, 
                maintainAspectRatio: false, 
                plugins: { legend: { position: 'bottom' } } 
            } 
        });
    };

    window.renderDistribusiPelunasan<%=rnd%> = function(dataArr, ta, smt) {
        document.getElementById('colChart1<%=rnd%>').className = 'col-lg-8';
        document.getElementById('colChart2<%=rnd%>').className = 'col-lg-4';
        document.getElementById('colChart2<%=rnd%>').style.display = 'block';

        let smtText = (smt === "") ?
            "<%= Common.getBahasaConfig("Semua Semester") %>" : "<%= Common.getBahasaConfig("Smt") %> " + smt;
        document.getElementById('titleChartPiutang<%=rnd%>').innerHTML = '<i class="fas fa-users me-2"></i> <%= Common.getBahasaConfig("Distribusi Status Pelunasan TA") %> ' + ta + ' (' + smtText + ')';
        document.getElementById('titleChartLunas<%=rnd%>').innerHTML = '<i class="fas fa-chart-pie me-2"></i> <%= Common.getBahasaConfig("Komposisi Status Mahasiswa") %>';
        document.getElementById('theadRekap<%=rnd%>').innerHTML = '<tr>' +
            '<th><%= Common.getBahasaConfig("Kelompok Entitas") %></th>' +
            '<th class="text-end bg-success text-black bg-opacity-10"><i class="fas fa-check-circle text-success me-1"></i> <%= Common.getBahasaConfig("Lunas 100% (Orang)") %></th>' +
            '<th class="text-end bg-warning text-black bg-opacity-10"><i class="fas fa-adjust text-warning me-1"></i> <%= Common.getBahasaConfig("Mencicil (Orang)") %></th>' +
            '<th class="text-end bg-danger text-black bg-opacity-10"><i class="fas fa-times-circle text-danger me-1"></i> <%= Common.getBahasaConfig("Belum Bayar (Orang)") %></th>' +
            '<th class="text-end fw-bold"><i class="fas fa-users text-primary me-1"></i> <%= Common.getBahasaConfig("Total Mahasiswa") %></th>' +
            '<th class="text-center fw-bold"><%= Common.getBahasaConfig("% Pelunasan") %></th>' +
        '</tr>';
        let labels = []; 
        let dLunas = []; let dMencicil = []; let dBelum = [];
        let htmlTbody = '';
        let grandLunas = 0, grandMencicil = 0, grandBelum = 0, grandTotal = 0;
        if (dataArr.length === 0) {
            htmlTbody = '<tr><td colspan="6" class="text-center py-4 text-muted fw-bold"><i class="fas fa-box-open fa-2x mb-3 mt-2 opacity-50"></i><br><%= Common.getBahasaConfig("Tidak ada data distribusi ditemukan pada kriteria ini.") %></td></tr>';
        } else {
            dataArr.forEach(function(r) {
                labels.push((r.kelompok).substring(0, 30)); 
                dLunas.push(r.lunas);
                dMencicil.push(r.mencicil);
                dBelum.push(r.belum_bayar);
                
                grandLunas += r.lunas;
                grandMencicil += r.mencicil;
                grandBelum += r.belum_bayar;
                grandTotal += r.total_mhs;

                let persenRow = r.total_mhs > 0 ? (r.lunas * 100.0) / r.total_mhs : 0;

                htmlTbody += '<tr>' +
                    '<td class="fw-bold">' + r.kelompok + '</td>' +
                    '<td class="text-end text-success fw-bold fs-6" data-t="n" data-v="' + r.lunas + '">' + fmtRp.format(r.lunas) + '</td>' +
                    '<td class="text-end text-warning text-darken-2 fw-bold fs-6" data-t="n" data-v="' + r.mencicil + '">' + fmtRp.format(r.mencicil) + '</td>' +
                    '<td class="text-end text-danger fw-bold fs-6" data-t="n" data-v="' + r.belum_bayar + '">' + fmtRp.format(r.belum_bayar) + '</td>' +
                    '<td class="text-end fw-bold text-dark fs-6" data-t="n" data-v="' + r.total_mhs + '">' + fmtRp.format(r.total_mhs) + '</td>' +
                    '<td class="text-center fw-bold ' + (persenRow >= 100 ?
                    'text-success' : 'text-danger') + '" data-t="n" data-v="' + persenRow + '">' + persenRow.toFixed(2) + '%</td>' +
                '</tr>';
            });
        }

        document.getElementById('tbodyRekap<%=rnd%>').innerHTML = htmlTbody;
        
        let grandPersen = grandTotal > 0 ? (grandLunas * 100.0) / grandTotal : 0;

        document.getElementById('tfootRekap<%=rnd%>').innerHTML = 
            '<tr>' +
            '<td class="text-center text-uppercase"><%= Common.getBahasaConfig("Total Akumulasi Keseluruhan") %></td>' +
            '<td class="text-success fw-bold fs-5" data-t="n" data-v="' + grandLunas + '">' + fmtRp.format(grandLunas) + '</td>' +
            '<td class="text-warning text-darken-2 fw-bold fs-5" data-t="n" data-v="' + grandMencicil + '">' + fmtRp.format(grandMencicil) + '</td>' +
            '<td class="text-danger fw-bold fs-5" data-t="n" data-v="' + grandBelum + '">' + fmtRp.format(grandBelum) + '</td>' +
            '<td class="text-dark fw-bold fs-5" data-t="n" data-v="' + grandTotal + '">' + fmtRp.format(grandTotal) + '</td>' +
            '<td class="text-center ' + (grandPersen >= 100 ?
            'text-success' : 'text-danger') + '" data-t="n" data-v="' + grandPersen + '">' + grandPersen.toFixed(2) + '%</td>' +
        '</tr>';

        if (chartP<%=rnd%>) chartP<%=rnd%>.destroy();
        if (chartL<%=rnd%>) chartL<%=rnd%>.destroy();
        
        chartP<%=rnd%> = new Chart(document.getElementById('chartPiutang<%=rnd%>').getContext('2d'), { 
            type: 'bar', 
            data: { 
                labels: labels, 
                datasets: [
                    { label: '<%= Common.getBahasaConfigJS("Lunas 100% (Orang)") %>', data: dLunas, backgroundColor: '#198754' },
                    { label: '<%= Common.getBahasaConfigJS("Mencicil (Orang)") %>', data: dMencicil, backgroundColor: '#ffc107' },
                    { label: '<%= Common.getBahasaConfigJS("Belum Bayar (Orang)") %>', data: dBelum, backgroundColor: '#dc3545' }
                ] 
            }, 
            options: { 
                indexAxis: 'y', 
                responsive: true, 
                maintainAspectRatio: false,
                scales: {
                    x: { stacked: true },
                    y: { stacked: true }
                },
                plugins: { legend: { position: 'bottom' } } 
            } 
        });
        chartL<%=rnd%> = new Chart(document.getElementById('chartLunas<%=rnd%>').getContext('2d'), { 
            type: 'doughnut', 
            data: { 
                labels: ['<%= Common.getBahasaConfigJS("Lunas 100%") %>', '<%= Common.getBahasaConfigJS("Mencicil") %>', '<%= Common.getBahasaConfigJS("Belum Bayar") %>'], 
                datasets: [{ 
                    data: [grandLunas, grandMencicil, grandBelum], 
                    backgroundColor: ['#198754', '#ffc107', '#dc3545'], 
                    borderWidth: 2 
                }] 
            }, 
            options: { 
                responsive: true, 
                maintainAspectRatio: false, 
                plugins: { legend: { position: 'bottom' } } 
            } 
        });
    };

    window.renderTopDebtors<%=rnd%> = function(result) {
        const dataArr = result.data;
        const totalRecords = result.totalRecords || dataArr.length;
        const totalPages = result.totalPages || 1;
        const currentPage = result.currentPage || 1;
        const offset = result.offset || 0;

        const pagControls = document.getElementById('paginationControls<%=rnd%>');
        const footerInfo = document.getElementById('footerTotalInfo<%=rnd%>');
        
        pagControls.style.display = 'flex';
        footerInfo.style.display = 'block';
        document.getElementById('lblInfoHalaman<%=rnd%>').innerText = '<%= Common.getBahasaConfigJS("Hal") %> ' + currentPage + ' <%= Common.getBahasaConfig("dari") %> ' + totalPages;
        document.getElementById('btnPrevPage<%=rnd%>').disabled = currentPage <= 1;
        document.getElementById('btnNextPage<%=rnd%>').disabled = currentPage >= totalPages;
        
        document.getElementById('lblJmlTampil<%=rnd%>').innerText = fmtRp.format(dataArr.length);
        document.getElementById('lblJmlTotal<%=rnd%>').innerText = fmtRp.format(totalRecords);
        document.getElementById('theadRekap<%=rnd%>').innerHTML = '<tr>' +
            '<th><%= Common.getBahasaConfig("Peringkat") %></th>' +
            '<th><%= Common.getBahasaConfig("NIM / No. Registrasi") %></th>' +
            '<th><%= Common.getBahasaConfig("Nama Mahasiswa") %></th>' +
            '<th><%= Common.getBahasaConfig("Fakultas") %></th>' +
            '<th><%= Common.getBahasaConfig("Program Studi") %></th>' +
            '<th class="text-end"><%= Common.getBahasaConfig("Total Tagihan (Rp)") %></th>' +
            '<th class="text-end"><%= Common.getBahasaConfig("Total Dibayar (Rp)") %></th>' +
            '<th class="text-end bg-danger bg-opacity-10"><%= Common.getBahasaConfig("Sisa Piutang (Rp)") %></th>' +
        '</tr>';
        let htmlTbody = ''; let grandTagihan=0, grandDibayar=0, grandPiutang=0;
        
        if (dataArr.length === 0) {
            htmlTbody = '<tr><td colspan="8" class="text-center py-4 text-muted fw-bold"><i class="fas fa-box-open fa-2x mb-3 mt-2 opacity-50"></i><br><%= Common.getBahasaConfig("Tidak ada data penunggak ditemukan pada kriteria ini.") %></td></tr>';
        } else {
            dataArr.forEach(function(r, index) {
                grandTagihan += r.tagihan; grandDibayar += r.dibayar; grandPiutang += r.piutang;
                
                let rankNum = offset + index + 1;
                let rankBadge = '<span class="badge bg-secondary px-3">' + rankNum + '</span>';
                if (rankNum === 1) rankBadge = '<span class="badge bg-danger px-3 fs-6"><i class="fas fa-trophy me-1"></i> ' + rankNum + '</span>';
                else if (rankNum === 2) rankBadge = '<span class="badge bg-warning text-dark px-3 fs-6">' + rankNum + '</span>';
                else if (rankNum === 3) rankBadge = '<span class="badge bg-info text-dark px-3 fs-6">' + rankNum + '</span>';

                htmlTbody += '<tr>' +
                    '<td class="text-center">' + rankBadge + '</td>' +
                    '<td class="fw-bold">' + r.nim + '</td>' +
                    '<td class="fw-bold text-dark">' + r.nama + '</td>' +
                    '<td>' + r.fakultas + '</td>' +
                    '<td>' + r.jurusan + '</td>' +
                    '<td class="text-end fw-semibold text-primary" data-t="n" data-v="' + r.tagihan + '">' + fmtRp.format(r.tagihan) + '</td>' +
                    '<td class="text-end fw-semibold text-success" data-t="n" data-v="' + r.dibayar + '">' + fmtRp.format(r.dibayar) + '</td>' +
                    '<td class="text-end fw-bold text-danger fs-6" data-t="n" data-v="' + r.piutang + '">' + fmtRp.format(r.piutang) + '</td>' +
                '</tr>';
            });
        }

        document.getElementById('tbodyRekap<%=rnd%>').innerHTML = htmlTbody;
        document.getElementById('tfootRekap<%=rnd%>').innerHTML = '<tr>' +
            '<td colspan="5" class="text-center text-uppercase"><%= Common.getBahasaConfig("Total Akumulasi Halaman Ini") %></td>' +
            '<td class="text-primary fw-bold" data-t="n" data-v="' + grandTagihan + '">' + fmtRp.format(grandTagihan) + '</td>' +
            '<td class="text-success fw-bold" data-t="n" data-v="' + grandDibayar + '">' + fmtRp.format(grandDibayar) + '</td>' +
            '<td class="text-danger fw-bold fs-6" data-t="n" data-v="' + grandPiutang + '">' + fmtRp.format(grandPiutang) + '</td>' +
        '</tr>';
    };
    
    window.renderUmurPiutang<%=rnd%> = function(dataArr, ta, smt) {
        document.getElementById('colChart1<%=rnd%>').className = 'col-lg-8';
        document.getElementById('colChart2<%=rnd%>').className = 'col-lg-4';
        document.getElementById('colChart2<%=rnd%>').style.display = 'block';

        let smtText = (smt === "") ?
            "<%= Common.getBahasaConfig("Semua Semester") %>" : "<%= Common.getBahasaConfig("Smt") %> " + smt;
        document.getElementById('titleChartPiutang<%=rnd%>').innerHTML = '<i class="fas fa-layer-group me-2"></i> <%= Common.getBahasaConfig("Sebaran Umur Piutang TA") %> ' + ta + ' (' + smtText + ')';
        document.getElementById('titleChartLunas<%=rnd%>').innerHTML = '<i class="fas fa-chart-pie me-2"></i> <%= Common.getBahasaConfig("Komposisi Umur Piutang Keseluruhan") %>';
        document.getElementById('theadRekap<%=rnd%>').innerHTML = '<tr>' +
            '<th><%= Common.getBahasaConfig("Kelompok Entitas") %></th>' +
            '<th class="text-end bg-primary bg-opacity-10"><%= Common.getBahasaConfig("Semester Berjalan") %></th>' +
            '<th class="text-end bg-warning text-black bg-opacity-10"><%= Common.getBahasaConfig("Menunggak 1 Smt") %></th>' +
            '<th class="text-end bg-danger text-black bg-opacity-10"><%= Common.getBahasaConfig("Menunggak 2 Smt") %></th>' +
            '<th class="text-end bg-dark text-white"><%= Common.getBahasaConfig("Menunggak > 1 Tahun") %></th>' +
            '<th class="text-end fw-bold"><%= Common.getBahasaConfig("Total Piutang (Rp)") %></th>' +
        '</tr>';
        let labels = []; 
        let dBerjalan = []; let dSmt1 = []; let dSmt2 = []; let dLama = [];
        let htmlTbody = '';
        
        let grandBerjalan = 0, grandSmt1 = 0, grandSmt2 = 0, grandLama = 0, grandTotal = 0;
        if (dataArr.length === 0) {
            htmlTbody = '<tr><td colspan="6" class="text-center py-4 text-muted fw-bold"><i class="fas fa-box-open fa-2x mb-3 mt-2 opacity-50"></i><br><%= Common.getBahasaConfig("Tidak ada data piutang ditemukan pada kriteria ini.") %></td></tr>';
        } else {
            dataArr.forEach(function(r) {
                labels.push((r.kelompok).substring(0, 30)); 
                dBerjalan.push(r.berjalan);
                dSmt1.push(r.smt1);
                dSmt2.push(r.smt2);
                dLama.push(r.lama);
                
                let totRow = r.berjalan + r.smt1 + r.smt2 + r.lama;
                
                grandBerjalan += r.berjalan;
                grandSmt1 += r.smt1;
                grandSmt2 += r.smt2;
                grandLama += r.lama;
                grandTotal += totRow;

                htmlTbody += '<tr>' +
                    '<td class="fw-bold">' + r.kelompok + '</td>' +
                    '<td class="text-end text-primary" data-t="n" data-v="' + r.berjalan + '">' + fmtRp.format(r.berjalan) + '</td>' +
                    '<td class="text-end text-warning text-darken-2" data-t="n" data-v="' + r.smt1 + '">' + fmtRp.format(r.smt1) + '</td>' +
                    '<td class="text-end text-danger" data-t="n" data-v="' + r.smt2 + '">' + fmtRp.format(r.smt2) + '</td>' +
                    '<td class="text-end fw-bold text-dark" data-t="n" data-v="' + r.lama + '">' + fmtRp.format(r.lama) + '</td>' +
                    '<td class="text-end fw-bold text-danger" data-t="n" data-v="' + totRow + '">' + fmtRp.format(totRow) + '</td>' +
                '</tr>';
            });
        }

        document.getElementById('tbodyRekap<%=rnd%>').innerHTML = htmlTbody;
        document.getElementById('tfootRekap<%=rnd%>').innerHTML = '<tr>' +
            '<td class="text-center text-uppercase"><%= Common.getBahasaConfig("Total Akumulasi Keseluruhan") %></td>' +
            '<td class="text-primary fw-bold fs-6" data-t="n" data-v="' + grandBerjalan + '">' + fmtRp.format(grandBerjalan) + '</td>' +
            '<td class="text-warning text-darken-2 fw-bold fs-6" data-t="n" data-v="' + grandSmt1 + '">' + fmtRp.format(grandSmt1) + '</td>' +
            '<td class="text-danger fw-bold fs-6" data-t="n" data-v="' + grandSmt2 + '">' + fmtRp.format(grandSmt2) + '</td>' +
            '<td class="text-dark fw-bold fs-6" data-t="n" data-v="' + grandLama + '">' + fmtRp.format(grandLama) + '</td>' +
            '<td class="text-danger fw-bold fs-6" data-t="n" data-v="' + grandTotal + '">' + fmtRp.format(grandTotal) + '</td>' +
        '</tr>';
        if (chartP<%=rnd%>) chartP<%=rnd%>.destroy();
        if (chartL<%=rnd%>) chartL<%=rnd%>.destroy();
        
        chartP<%=rnd%> = new Chart(document.getElementById('chartPiutang<%=rnd%>').getContext('2d'), { 
            type: 'bar', 
            data: { 
                labels: labels, 
                datasets: [
                    { label: '<%= Common.getBahasaConfigJS("Semester Berjalan") %>', data: dBerjalan, backgroundColor: '#0d6efd' },
                    { label: '<%= Common.getBahasaConfigJS("1 Smt Lalu") %>', data: dSmt1, backgroundColor: '#ffc107' },
                    { label: '<%= Common.getBahasaConfigJS("2 Smt Lalu") %>', data: dSmt2, backgroundColor: '#dc3545' },
                    { label: '<%= Common.getBahasaConfigJS("> 1 Tahun") %>', data: dLama, backgroundColor: '#212529' }
                ] 
            }, 
            options: { 
                indexAxis: 'y', 
                responsive: true, 
                maintainAspectRatio: false,
                scales: {
                    x: { stacked: true },
                    y: { stacked: true }
                },
                plugins: { legend: { position: 'bottom' } } 
            } 
        });
        chartL<%=rnd%> = new Chart(document.getElementById('chartLunas<%=rnd%>').getContext('2d'), { 
            type: 'doughnut', 
            data: { 
                labels: ['<%= Common.getBahasaConfigJS("Smt Berjalan") %>', '<%= Common.getBahasaConfigJS("1 Smt Lalu") %>', '<%= Common.getBahasaConfigJS("2 Smt Lalu") %>', '<%= Common.getBahasaConfigJS("> 1 Tahun") %>'], 
                datasets: [{ 
                    data: [grandBerjalan, grandSmt1, grandSmt2, grandLama], 
                    backgroundColor: ['#0d6efd', '#ffc107', '#dc3545', '#212529'], 
                    borderWidth: 2 
                }] 
            }, 
            options: { 
                responsive: true, 
                maintainAspectRatio: false, 
                plugins: { legend: { position: 'bottom' } } 
            } 
        });
    };

    window.renderTabelRinci<%=rnd%> = function(result) {
        const dataArr = result.data;
        const totalRecords = result.totalRecords || dataArr.length;
        const totalPages = result.totalPages || 1;
        const currentPage = result.currentPage || 1;
        const pagControls = document.getElementById('paginationControls<%=rnd%>');
        const footerInfo = document.getElementById('footerTotalInfo<%=rnd%>');
        
        pagControls.style.display = 'flex';
        footerInfo.style.display = 'block';
        document.getElementById('lblInfoHalaman<%=rnd%>').innerText = '<%= Common.getBahasaConfigJS("Hal") %> ' + currentPage + ' <%= Common.getBahasaConfig("dari") %> ' + totalPages;
        document.getElementById('btnPrevPage<%=rnd%>').disabled = currentPage <= 1;
        document.getElementById('btnNextPage<%=rnd%>').disabled = currentPage >= totalPages;
        
        document.getElementById('lblJmlTampil<%=rnd%>').innerText = fmtRp.format(dataArr.length);
        document.getElementById('lblJmlTotal<%=rnd%>').innerText = fmtRp.format(totalRecords);
        document.getElementById('theadRekap<%=rnd%>').innerHTML = '<tr>' +
            '<th><%= Common.getBahasaConfig("NIM / No. Registrasi") %></th>' +
            '<th><%= Common.getBahasaConfig("Nama Lengkap") %></th>' +
            '<th><%= Common.getBahasaConfig("Jenis Kegiatan") %></th>' +
            '<th><%= Common.getBahasaConfig("Item Biaya") %></th>' +
            '<th><%= Common.getBahasaConfig("Bulan") %></th>' +
            '<th><%= Common.getBahasaConfig("T.A") %></th>' +
            '<th><%= Common.getBahasaConfig("Smt") %></th>' +
            '<th class="text-end"><%= Common.getBahasaConfig("Tagihan (Rp)") %></th>' +
            '<th class="text-end"><%= Common.getBahasaConfig("Dibayar (Rp)") %></th>' +
            '<th class="text-end"><%= Common.getBahasaConfig("Sisa Piutang (Rp)") %></th>' +
        '</tr>';
        let htmlTbody = ''; let grandTagihan=0, grandDibayar=0, grandPiutang=0;
        
        if (dataArr.length === 0) {
            htmlTbody = '<tr><td colspan="10" class="text-center py-4 text-muted fw-bold"><i class="fas fa-box-open fa-2x mb-3 mt-2 opacity-50"></i><br><%= Common.getBahasaConfig("Tidak ada data ditemukan pada kriteria pencarian ini.") %></td></tr>';
        } else {
            dataArr.forEach(function(r) {
                grandTagihan += r.tagihan; grandDibayar += r.dibayar; grandPiutang += r.sisa;
                htmlTbody += '<tr>' +
                    '<td class="fw-bold">' + r.kodeTransaksi + '</td>' +
                    '<td>' + r.nama + '</td>' +
                    '<td>' + r.jenisKegiatan + '</td>' +
                    '<td>' + r.itemBiaya + '</td>' +
                    '<td class="text-center">' + (r.bulan || '-') + '</td>' +
                    '<td class="text-center">' + r.ta + '</td>' +
                    '<td class="text-center">' + r.smt + '</td>' +
                    '<td class="text-end fw-semibold text-danger" data-t="n" data-v="' + r.tagihan + '">' + fmtRp.format(r.tagihan) + '</td>' +
                    '<td class="text-end fw-semibold text-success" data-t="n" data-v="' + r.dibayar + '">' + fmtRp.format(r.dibayar) + '</td>' +
                    '<td class="text-end fw-bold" data-t="n" data-v="' + r.sisa + '">' + fmtRp.format(r.sisa) + '</td>' +
                '</tr>';
            });
        }

        document.getElementById('tbodyRekap<%=rnd%>').innerHTML = htmlTbody;
        document.getElementById('tfootRekap<%=rnd%>').innerHTML = '<tr>' +
            '<td colspan="7" class="text-center text-uppercase"><%= Common.getBahasaConfig("Total Akumulasi Halaman Ini") %></td>' +
            '<td class="text-danger" data-t="n" data-v="' + grandTagihan + '">' + fmtRp.format(grandTagihan) + '</td>' +
            '<td class="text-success" data-t="n" data-v="' + grandDibayar + '">' + fmtRp.format(grandDibayar) + '</td>' +
            '<td data-t="n" data-v="' + grandPiutang + '">' + fmtRp.format(grandPiutang) + '</td>' +
        '</tr>';
    };

    window.renderTrendPembayaran<%=rnd%> = function(result) {
        const dataArr = result.data;
        document.getElementById('colChart1<%=rnd%>').className = 'col-lg-12';
        document.getElementById('colChart2<%=rnd%>').style.display = 'none';
        
        document.getElementById('titleChartPiutang<%=rnd%>').innerHTML = '<i class="fas fa-chart-line me-2"></i>' + '<%= Common.getBahasaConfigJS("Grafik Tren Pembayaran Mahasiswa (Harian)") %>';
        document.getElementById('theadRekap<%=rnd%>').innerHTML = '<tr>' +
            '<th>' + '<%= Common.getBahasaConfigJS("Tanggal Transaksi") %>' + '</th>' +
            '<th class="text-end">' + '<%= Common.getBahasaConfigJS("Jumlah Transaksi Valid") %>' + '</th>' +
            '<th class="text-end">' + '<%= Common.getBahasaConfigJS("Total Nominal Masuk (Rp)") %>' + '</th>' +
        '</tr>';
        let labels = []; let dNominal = []; let htmlTbody = '';
        let grandTrx = 0; let grandNominal = 0;
        if (dataArr.length === 0) {
            htmlTbody = '<tr><td colspan="3" class="text-center py-4 text-muted fw-bold"><i class="fas fa-box-open fa-2x mb-3 mt-2 opacity-50"></i><br>' + '<%= Common.getBahasaConfigJS("Tidak ada data tren transaksi ditemukan pada rentang waktu ini.") %>' + '</td></tr>';
        } else {
            dataArr.forEach(function(r) {
                labels.push(r.tanggal);
                dNominal.push(r.nominal);
                grandTrx += r.jmlTrx;
                grandNominal += r.nominal;
                
                htmlTbody += '<tr>' +
                    '<td class="fw-bold">' + r.tanggal + '</td>' +
                    '<td class="text-end" data-t="n" data-v="' + r.jmlTrx + '">' + fmtRp.format(r.jmlTrx) + '</td>' +
                    '<td class="text-end text-success fw-bold" data-t="n" data-v="' + r.nominal + '">' + fmtRp.format(r.nominal) + '</td>' +
                '</tr>';
            });
        }
        
        document.getElementById('tbodyRekap<%=rnd%>').innerHTML = htmlTbody;
        document.getElementById('tfootRekap<%=rnd%>').innerHTML = '<tr>' +
            '<td class="text-center text-uppercase">' + '<%= Common.getBahasaConfigJS("Total Akumulasi Rentang Waktu") %>' + '</td>' +
            '<td class="text-end fw-bold" data-t="n" data-v="' + grandTrx + '">' + fmtRp.format(grandTrx) + '</td>' +
            '<td class="text-end text-success fw-bold fs-6" data-t="n" data-v="' + grandNominal + '">' + fmtRp.format(grandNominal) + '</td>' +
        '</tr>';
        if (chartP<%=rnd%>) chartP<%=rnd%>.destroy();
        if (chartL<%=rnd%>) chartL<%=rnd%>.destroy();
        
        chartP<%=rnd%> = new Chart(document.getElementById('chartPiutang<%=rnd%>').getContext('2d'), {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: '<%= Common.getBahasaConfigJS("Nominal Pembayaran (Rp)") %>',
                    data: dNominal,
                    backgroundColor: 'rgba(13, 110, 253, 0.15)',
                    borderColor: 'rgba(13, 110, 253, 1)',
                    borderWidth: 2,
                    fill: true,
                    tension: 0.3,
                    pointRadius: 4,
                    pointBackgroundColor: 'rgba(13, 110, 253, 1)'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: true } },
                scales: { y: { beginAtZero: true } }
            }
        });
    };
    
    window.renderGrafikDanTabelAgregat<%=rnd%> = function(dataArr, titleDasbor, ta, smt, headerTitle) {
        document.getElementById('colChart1<%=rnd%>').className = 'col-lg-6';
        document.getElementById('colChart2<%=rnd%>').style.display = 'block';

        if (chartP<%=rnd%>) chartP<%=rnd%>.destroy(); if (chartL<%=rnd%>) chartL<%=rnd%>.destroy();
        
        let smtText = (smt === "") ?
            "<%= Common.getBahasaConfig("Semua Semester") %>" : "<%= Common.getBahasaConfig("Smt") %> " + smt;
        document.getElementById('titleChartPiutang<%=rnd%>').innerHTML = '<i class="fas fa-chart-line me-2"></i> <%= Common.getBahasaConfig("Piutang TA") %> ' + ta + ' (' + smtText + ')';
        document.getElementById('titleChartLunas<%=rnd%>').innerHTML = '<i class="fas fa-chart-pie me-2"></i> <%= Common.getBahasaConfig("Persentase Lunas TA") %> ' + ta + ' (' + smtText + ')';
        document.getElementById('theadRekap<%=rnd%>').innerHTML = '<tr>' +
            '<th>' + headerTitle + '</th>' +
            '<th><%= Common.getBahasaConfig("Jenis Pembayaran") %></th>' +
            '<th class="text-end"><%= Common.getBahasaConfig("Tagihan (Rp)") %></th>' +
            '<th class="text-end"><%= Common.getBahasaConfig("Jml Trx Tagihan") %></th>' +
            '<th class="text-end"><%= Common.getBahasaConfig("Dibayar (Rp)") %></th>' +
            '<th class="text-end"><%= Common.getBahasaConfig("Jml Trx Dibayar") %></th>' +
            '<th class="text-end"><%= Common.getBahasaConfig("Piutang (Rp)") %></th>' +
            '<th class="text-end"><%= Common.getBahasaConfig("Jml Trx Piutang") %></th>' +
            '<th class="text-center"><%= Common.getBahasaConfig("% Lunas") %></th>' +
        '</tr>';
        let labels = []; let dPiutang = []; let dPersen = []; let htmlTbody = '';
        let grandTagihan=0, grandDibayar=0, grandPiutang=0;
        let grandJmlTagihan=0, grandJmlDibayar=0, grandJmlPiutang=0;

        if (dataArr.length === 0) {
            htmlTbody = '<tr><td colspan="9" class="text-center py-4 text-muted fw-bold"><i class="fas fa-box-open fa-2x mb-3 mt-2 opacity-50"></i><br><%= Common.getBahasaConfig("Tidak ada data ditemukan pada kriteria pencarian ini.") %></td></tr>';
        } else {
            dataArr.forEach(function(r) {
                labels.push((r.kelompok + ' - ' + r.jenisKegiatan).substring(0, 30)); 
                dPiutang.push(r.piutang); dPersen.push(r.persen);
                grandTagihan += r.tagihan; grandDibayar += r.dibayar; grandPiutang += r.piutang;
                grandJmlTagihan += r.jmlTagihan; grandJmlDibayar += r.jmlDibayar; grandJmlPiutang += r.jmlPiutang;

                htmlTbody += '<tr>' +
                    '<td class="fw-bold">' + r.kelompok + '</td>' +
                    '<td>' + r.jenisKegiatan + '</td>' +
                    '<td class="text-end text-primary" data-t="n" data-v="' + r.tagihan + '">' + fmtRp.format(r.tagihan) + '</td>' +
                    '<td class="text-end" data-t="n" data-v="' + r.jmlTagihan + '">' + fmtRp.format(r.jmlTagihan) + '</td>' +
                    '<td class="text-end text-success" data-t="n" data-v="' + r.dibayar + '">' + fmtRp.format(r.dibayar) + '</td>' +
                    '<td class="text-end" data-t="n" data-v="' + r.jmlDibayar + '">' + fmtRp.format(r.jmlDibayar) + '</td>' +
                    '<td class="text-end text-danger" data-t="n" data-v="' + r.piutang + '">' + fmtRp.format(r.piutang) + '</td>' +
                    '<td class="text-end" data-t="n" data-v="' + r.jmlPiutang + '">' + fmtRp.format(r.jmlPiutang) + '</td>' +
                    '<td class="text-center fw-bold ' + (r.persen >= 100 ? 'text-success' : 'text-danger') + '" data-t="n" data-v="' + r.persen + '">' + r.persen.toFixed(2) + '%</td>' +
                '</tr>';
            });
        }

        document.getElementById('tbodyRekap<%=rnd%>').innerHTML = htmlTbody;
        
        let grandPersen = grandTagihan > 0 ? (grandDibayar / grandTagihan) * 100 : 0;
        
        document.getElementById('tfootRekap<%=rnd%>').innerHTML = '<tr>' +
            '<td colspan="2" class="text-center text-uppercase"><%= Common.getBahasaConfig("Total Akumulasi Keseluruhan") %></td>' +
            '<td class="text-primary" data-t="n" data-v="' + grandTagihan + '">' + fmtRp.format(grandTagihan) + '</td>' +
            '<td data-t="n" data-v="' + grandJmlTagihan + '">' + fmtRp.format(grandJmlTagihan) + '</td>' +
            '<td class="text-success" data-t="n" data-v="' + grandDibayar + '">' + fmtRp.format(grandDibayar) + '</td>' +
            '<td data-t="n" data-v="' + grandJmlDibayar + '">' + fmtRp.format(grandJmlDibayar) + '</td>' +
            '<td class="text-danger" data-t="n" data-v="' + grandPiutang + '">' + fmtRp.format(grandPiutang) + '</td>' +
            '<td data-t="n" data-v="' + grandJmlPiutang + '">' + fmtRp.format(grandJmlPiutang) + '</td>' +
            '<td class="text-center ' + (grandPersen >= 100 ? 'text-success' : 'text-danger') + '" data-t="n" data-v="' + grandPersen + '">' + grandPersen.toFixed(2) + '%</td>' +
        '</tr>';

        chartP<%=rnd%> = new Chart(document.getElementById('chartPiutang<%=rnd%>').getContext('2d'), { type: 'bar', data: { labels: labels, datasets: [{ label: '<%= Common.getBahasaConfigJS("Piutang (Rp)") %>', data: dPiutang, backgroundColor: 'rgba(220, 53, 69, 0.85)', borderColor: 'rgba(220, 53, 69, 1)', borderWidth: 1, borderRadius: 4 }] }, options: { indexAxis: 'y', responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } } } });
        chartL<%=rnd%> = new Chart(document.getElementById('chartLunas<%=rnd%>').getContext('2d'), { type: 'bar', data: { labels: labels, datasets: [{ label: '<%= Common.getBahasaConfigJS("Pelunasan (%)") %>', data: dPersen, backgroundColor: 'rgba(25, 135, 84, 0.85)', borderColor: 'rgba(25, 135, 84, 1)', borderWidth: 1, borderRadius: 4 }] }, options: { indexAxis: 'y', responsive: true, maintainAspectRatio: false, scales: { x: { max: 100 } }, plugins: { legend: { display: false } } } });
    };

    window.downloadExcel<%=rnd%> = function() {
        if(typeof XLSX === 'undefined') { alert('<%= Common.getBahasaConfigJS("Pustaka Excel belum termuat, silakan coba beberapa saat lagi.") %>'); return; }
        
        let table = document.getElementById("tabelRekapDasbor<%=rnd%>") || document.querySelector("#tableContainer<%=rnd%> table");
        let wb = XLSX.utils.table_to_book(table, {sheet:"Laporan Keuangan", raw: false});
        XLSX.writeFile(wb, "Data_Export_Pembayaran_Terpadu.xlsx");
    };
    
    // Inisialisasi otomatis tampilan pertama
    setTimeout(function() { window.toggleFilterKhusus<%=rnd%>(); }, 300);
</script>