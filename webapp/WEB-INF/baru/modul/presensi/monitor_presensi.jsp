<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.rab.SatuanKerja"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Tbmrole"%>
<%@page import="ais.database.model.Statusabsensi"%>
<%@page import="ais.common.Common"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Calendar"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>

<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.sendRedirect(Common.ROOT + "/login.jsp");
        return;
    }

    String rnd = request.getParameter("rnd") != null ? request.getParameter("rnd") : Common.getGeneratedBarCode(6);
    
    // --- 1. Logika Default Tanggal (Berdasarkan Konfigurasi) ---
    int tanggalMulaiAbsensi = 1;
    try {
        ais.database.model.Konfigurasi conf = Common.getKonfigurasi("tanggal_mulai_absensi", "1");
        if (conf != null) tanggalMulaiAbsensi = Integer.parseInt(conf.getNilai());
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/presensi/monitor_presensi.jsp:28");}

    Calendar calUtama = ais.ui.util.WaktuUtil.getCalendar();
    calUtama.add(Calendar.MONTH, -1);
    calUtama.set(Calendar.DATE, tanggalMulaiAbsensi);
    String tglMulaiDefault = Common.databaseDateFormat.get().format(calUtama.getTime());

    calUtama.add(Calendar.MONTH, 1);
    calUtama.add(Calendar.DATE, -1);
    String tglSampaiDefault = Common.databaseDateFormat.get().format(calUtama.getTime());
    
    // --- 2. Mengambil Konfigurasi Hari Aktif ---
    String hariDefaultTidakAktif = "";
    try {
        ais.database.model.Konfigurasi confHari = Common.getKonfigurasi("hari_default_tidak_aktif", ",1,7,");
        if (confHari != null) hariDefaultTidakAktif = confHari.getNilai();
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/presensi/monitor_presensi.jsp:44");}

    // --- 3. Memuat Data Master & Cek Hak Akses ---
    List<Statusabsensi> listStatus = null;
    List<SatuanKerja> listSatker = null;
    
    Tbmrole tbmrole = tbmuser.hakAkses();
    boolean bolehLihatSatuanKerja = tbmrole != null && Boolean.TRUE.equals(tbmrole.getMelihatDataSatkerLain());
    SatuanKerja currentSatker = tbmuser.ambilSatuanKerja();
    Session sessDb = null;
    try {
        sessDb = HibernateUtil.openSession();
        listStatus = ConstantValues.simpleList( sessDb.createCriteria(Statusabsensi.class).add(Restrictions.eq("aktif", true)).addOrder(Order.asc("nama")), Statusabsensi.class);
                           
        org.hibernate.Criteria cSatker = sessDb.createCriteria(SatuanKerja.class).add(Restrictions.eq("defaultItem", true)).addOrder(Order.asc("nama"));
        if (!bolehLihatSatuanKerja && currentSatker != null) { cSatker.add(Restrictions.eq("id", currentSatker.getId())); }
        listSatker = ConstantValues.simpleList( cSatker, SatuanKerja.class);
                           
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/presensi/monitor_presensi.jsp:63");
    } finally {
        if (sessDb != null && sessDb.isOpen()) { try { sessDb.disconnect(); sessDb.close(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/presensi/monitor_presensi.jsp:65");} }
        try { HibernateUtil.closeSessionQuietly(sessDb); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/presensi/monitor_presensi.jsp:66");}
    }

    Long defaultStatusId = ais.common.ConstantValues.MASUK != null ? ais.common.ConstantValues.MASUK.getId() : -1L;
%>

<script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<script data-cfasync="false" src="https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js"></script>

<style>
    .hover-elevate-<%=rnd%> { transition: transform 0.2s ease, box-shadow 0.2s ease; }
    .hover-elevate-<%=rnd%>:hover { transform: translateY(-2px); box-shadow: 0 0.5rem 1rem rgba(0,0,0,.1) !important; }
    .chart-container-<%=rnd%> { position: relative; height: 320px; width: 100%; }
    .img-thumbnail-<%=rnd%>:hover { filter: brightness(0.9); transform: scale(1.02); cursor: zoom-in; }
    .x-small-<%=rnd%> { font-size: 0.7rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; }
    #tabelRekapPresensi<%=rnd%> td, #tabelRekapPresensi<%=rnd%> th { vertical-align: middle !important; padding: 1.25rem 0.75rem; }
    .bg-soft-primary-<%=rnd%> { background-color: rgba(13, 110, 253, 0.05); }
    .bg-soft-success-<%=rnd%> { background-color: rgba(25, 135, 84, 0.05); }
</style>

<div class="container-fluid py-4 animate__animated animate__fadeIn">
    
    <div class="d-flex align-items-center mb-4 p-4 bg-white shadow-sm rounded-4 border-start border-5 border-success">
        <div class="bg-success bg-gradient text-white rounded-circle d-flex align-items-center justify-content-center me-3 shadow-sm" style="width: 55px; height: 55px;">
            <i class="fas fa-user-clock fs-3"></i>
        </div>
        <div>
            <h4 class="mb-1 fw-bold text-dark"><%= Common.getBahasaConfig("Dasbor Pemantauan Presensi Terpadu") %></h4>
            <p class="text-muted mb-0 small"><%= Common.getBahasaConfig("Analisis multi-dimensi untuk kehadiran Pegawai, Dosen, Guru, Siswa, dan Mahasiswa.") %></p>
        </div>
    </div>

    <div class="card border-0 shadow-sm rounded-4 mb-4">
        <div class="card-header bg-white border-bottom-0 pt-4 pb-2">
            <h6 class="fw-bold mb-0 text-success"><i class="fas fa-sliders-h me-2"></i><%= Common.getBahasaConfig("Konfigurasi Parameter Dasbor") %></h6>
        </div>
        <div class="card-body px-4 pb-4">
            <form id="formMonitorPresensi<%=rnd%>" onsubmit="event.preventDefault(); window.loadDataDasborPresensi<%=rnd%>();">
                
                <div class="p-4 bg-light rounded-4 border border-secondary border-opacity-10 mb-4">
                    <div class="row g-3">
                        <div class="col-lg-5 col-md-12">
                            <label class="form-label fw-bold text-secondary small"><i class="fas fa-layer-group me-1 text-primary"></i> <%= Common.getBahasaConfig("Dimensi Dasbor") %></label>
                            <select class="form-select border-secondary border-opacity-25 bg-white fw-bold text-success shadow-sm" name="jenisDashboard" id="fDashboardType<%=rnd%>" onchange="window.loadDataDasborPresensi<%=rnd%>();">
							    <option value="_service_presensi_harian">1. <%= Common.getBahasaConfig("Rincian Log Presensi (Detail Harian)") %></option>
							    <option value="_service_absensi_pegawai">2. <%= Common.getBahasaConfig("Rekapitulasi Kehadiran (Agregat Per Personil)") %></option>
							    <option value="_service_absensi_pegawai_per_orang">3. <%= Common.getBahasaConfig("Matriks Kehadiran Per Orang (Vertikal)") %></option>
							    <option value="_service_absensi_pegawai_per_orang_horizontal">4. <%= Common.getBahasaConfig("Matriks Kehadiran Horizontal (Per Tanggal)") %></option>
							    <option value="_service_absensi_pegawai_per_hari">5. <%= Common.getBahasaConfig("Matriks Kehadiran Per Tanggal (Vertikal)") %></option>
							    <option value="_service_absensi_pegawai_per_orang_rinci">6. <%= Common.getBahasaConfig("Rincian dan Rekapitulasi Komprehensif (Per Pegawai)") %></option>
							</select>
                        </div>
                        <div class="col-lg-3 col-md-6">
                            <label class="form-label fw-bold text-secondary small"><i class="fas fa-building me-1"></i> <%= Common.getBahasaConfig("Satuan Kerja") %></label>
                            <select class="form-select border-secondary border-opacity-25 shadow-sm" name="satuanKerjaId">
                                <option value=""><%= Common.getBahasaConfig("-- Semua Satuan Kerja --") %></option>
                                <% if (listSatker != null) {
                                    for(SatuanKerja sk : listSatker) { %>
                                        <option value="<%= sk.getId() %>"><%= sk.getNama() %></option>
                                <% }} %>
                            </select>
                        </div>
                        <div class="col-lg-4 col-md-6">
                            <label class="form-label fw-bold text-secondary small"><i class="fas fa-users me-1"></i> <%= Common.getBahasaConfig("Entitas") %></label>
                            <select class="form-select border-secondary border-opacity-25 shadow-sm" name="jenisEntitas">
                                <option value="ALL"><%= Common.getBahasaConfig("-- Semua --") %></option>
                                <option value="PEGAWAI"><%= Common.getBahasaConfig("Pegawai") %></option>
                                <option value="DOSEN"><%= Common.getBahasaConfig("Dosen") %></option>
                                <option value="GURU"><%= Common.getBahasaConfig("Guru") %></option>
                                <option value="MAHASISWA"><%= Common.getBahasaConfig("Mahasiswa") %></option>
                                <option value="SISWA"><%= Common.getBahasaConfig("Siswa") %></option>
                            </select>
                        </div>
                        <div class="col-lg-4 col-md-6">
                            <label class="form-label fw-bold text-secondary small"><i class="fas fa-clipboard-check me-1"></i> <%= Common.getBahasaConfig("Status") %></label>
                            <select class="form-select border-secondary border-opacity-25 shadow-sm" name="statusAbsensiId">
                                <option value=""><%= Common.getBahasaConfig("-- Semua Status --") %></option>
                                <% if (listStatus != null) {
                                    for (Statusabsensi sa : listStatus) {
                                        String selected = sa.getId().equals(defaultStatusId) ? "selected" : ""; %>
                                        <option value="<%= sa.getId() %>" <%= selected %>><%= sa.getNama() %></option>
                                <% } } %>
                            </select>
                        </div>
                        
                        <div class="col-lg-8 col-md-12">
                            <label class="form-label fw-bold text-secondary small"><i class="fas fa-search me-1"></i> <%= Common.getBahasaConfig("Kata Kunci Pencarian") %></label>
                            <input type="text" class="form-control border-secondary border-opacity-25 shadow-sm" name="q" placeholder="<%= Common.getBahasaConfig("Nama / Kode / NIP / NIDN / NIM...") %>">
                        </div>
                        <div class="col-lg-6 col-md-6">
                            <label class="form-label fw-bold text-secondary small"><i class="far fa-calendar-alt me-1"></i> <%= Common.getBahasaConfig("Mulai Tanggal") %></label>
                            <input type="date" class="form-control border-secondary border-opacity-25 shadow-sm" name="startDate" id="fStartDate<%=rnd%>" value="<%= tglMulaiDefault %>" required>
                        </div>
                        <div class="col-lg-6 col-md-6">
                            <label class="form-label fw-bold text-secondary small"><i class="far fa-calendar-check me-1"></i> <%= Common.getBahasaConfig("Sampai Tanggal") %></label>
                            <input type="date" class="form-control border-secondary border-opacity-25 shadow-sm" name="endDate" id="fEndDate<%=rnd%>" value="<%= tglSampaiDefault %>" required>
                        </div>

                        <div class="col-12 mt-3 pt-2 border-top border-secondary border-opacity-25">
                            <label class="form-label fw-bold text-secondary small mb-2"><i class="far fa-calendar-plus me-1 text-success"></i> <%= Common.getBahasaConfig("Penyaringan Hari Aktif") %></label>
                            <div class="d-flex flex-wrap gap-4">
                                <% 
                                    int idxHari = 1;
                                    for (String h : Common.haris) {
                                        boolean isChecked = !hariDefaultTidakAktif.contains("," + idxHari + ",");
                                %>
                                <div class="form-check form-switch">
                                    <input class="form-check-input" type="checkbox" name="hariAktif" value="<%=idxHari%>" id="hari<%=idxHari%><%=rnd%>" <%=isChecked ? "checked" : ""%>>
                                    <label class="form-check-label fw-bold text-dark" for="hari<%=idxHari%><%=rnd%>"><%= Common.getBahasaConfig(h) %></label>
                                </div>
                                <% 
                                        idxHari++;
                                    } 
                                %>
                            </div>
                        </div>
                    </div>
                </div>
                
                <div class="d-flex justify-content-end">
                    <button type="submit" class="btn btn-success fw-bold shadow-sm px-5 py-2 hover-elevate-<%=rnd%>">
                        <i class="fas fa-sync-alt me-2"></i> <%= Common.getBahasaConfig("Muat Ulang Dasbor") %>
                    </button>
                </div>
            </form>
        </div>
    </div>

    <div id="loadingContainer<%=rnd%>" class="text-center py-5" style="display: none;">
        <div class="spinner-border text-success" style="width: 3.5rem; height: 3.5rem; border-width: 0.3em;"></div>
        <h6 class="mt-4 text-secondary fw-bold"><%= Common.getBahasaConfig("Menerapkan Hak Akses dan Memproses Komputasi...") %></h6>
    </div>

    <div id="chartContainerWrapper<%=rnd%>" style="display: none;">
        <div class="row g-4 mb-4">
            <div class="col-lg-6">
                <div class="card border-0 shadow-sm rounded-4 h-100">
                    <div class="card-header bg-white py-3 border-bottom"><h6 class="fw-bold text-dark mb-0"><i class="fas fa-chart-pie text-success me-2"></i><%= Common.getBahasaConfig("Komposisi Kehadiran") %></h6></div>
                    <div class="card-body p-4"><div class="chart-container-<%=rnd%>"><canvas id="chartStatusPresensi<%=rnd%>"></canvas></div></div>
                </div>
            </div>
            <div class="col-lg-6">
                <div class="card border-0 shadow-sm rounded-4 h-100">
                    <div class="card-header bg-white py-3 border-bottom"><h6 class="fw-bold text-dark mb-0"><i class="fas fa-chart-bar text-info me-2"></i><%= Common.getBahasaConfig("Distribusi Partisipasi") %></h6></div>
                    <div class="card-body p-4"><div class="chart-container-<%=rnd%>"><canvas id="chartEntitas<%=rnd%>"></canvas></div></div>
                </div>
            </div>
        </div>

        <div class="card border-0 shadow-sm rounded-4 overflow-hidden mb-5">
            <div class="card-header bg-white border-bottom py-3 d-flex justify-content-between align-items-center flex-wrap gap-2">
                <h6 class="fw-bold mb-0 text-dark" id="lblJudulTabel<%=rnd%>"><i class="fas fa-list-ul text-success me-2"></i><%= Common.getBahasaConfig("Rincian Data Presensi") %></h6>
                <button type="button" class="btn btn-sm btn-outline-success fw-bold shadow-sm hover-elevate-<%=rnd%>" onclick="window.downloadExcelPresensi<%=rnd%>()">
                    <i class="fas fa-file-excel me-2"></i> <%= Common.getBahasaConfig("Unduh Excel") %>
                </button>
            </div>
            <div class="table-responsive">
                <table class="table table-hover table-bordered align-middle mb-0" id="tabelRekapPresensi<%=rnd%>" style="white-space: nowrap;">
                    <thead class="table-dark text-center text-uppercase" id="theadRekap<%=rnd%>"></thead>
                    <tbody id="tbodyRekap<%=rnd%>" class="border-top-0"></tbody>
                </table>
            </div>
            <div class="card-footer bg-light border-top py-3 d-flex justify-content-between align-items-center flex-wrap">
                <div class="text-muted small fw-bold" id="paginationInfo<%=rnd%>"></div>
                <nav><ul class="pagination pagination-sm mb-0 shadow-sm" id="paginationControls<%=rnd%>"></ul></nav>
            </div>
        </div>
    </div>
</div>

<script>
    var chartStatus<%=rnd%> = null, chartEntitas<%=rnd%> = null;
    var allData<%=rnd%> = [], dynamicCols<%=rnd%> = [];
    var currentPage<%=rnd%> = 1, rowsPerPage<%=rnd%> = 10, activeDimension<%=rnd%> = '_service_presensi_harian';

    window.formatLinkKeterangan<%=rnd%> = function(text) {
        if (!text || text.trim() === '') return '-';
        var urlRegex = /(https?:\/\/[^\s]+)/g;
        return text.replace(urlRegex, function(url) {
            return '<a href="' + url + '" target="_blank" class="badge bg-primary bg-opacity-10 text-primary border border-primary border-opacity-25 text-decoration-none hover-elevate-<%=rnd%> mx-1 px-2 py-1 d-inline-block" style="font-size: 0.75rem;"><i class="fas fa-external-link-alt me-1"></i><%= Common.getBahasaConfig("Klik") %></a>';
        });
    };

    window.loadDataDasborPresensi<%=rnd%> = function() {
        const form = document.getElementById('formMonitorPresensi<%=rnd%>');
        const formData = new URLSearchParams(new FormData(form));
        activeDimension<%=rnd%> = formData.get('jenisDashboard');
        
        document.getElementById('loadingContainer<%=rnd%>').style.display = 'block';
        document.getElementById('chartContainerWrapper<%=rnd%>').style.display = 'none';

        const url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=presensi&s=' + activeDimension<%=rnd%>;
        fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: formData.toString() })
        .then(res => res.json())
        .then(result => {
            document.getElementById('loadingContainer<%=rnd%>').style.display = 'none';
            if(result && result.status === '00') {
                document.getElementById('chartContainerWrapper<%=rnd%>').style.display = 'block';
                allData<%=rnd%> = result.data; 
                dynamicCols<%=rnd%> = result.columns || []; 
                currentPage<%=rnd%> = 1;
                
                let tglM = document.getElementById('fStartDate<%=rnd%>').value;
                let tglS = document.getElementById('fEndDate<%=rnd%>').value;
                let th = document.getElementById('theadRekap<%=rnd%>');

                if (activeDimension<%=rnd%> === '_service_presensi_harian') {
                    document.getElementById('lblJudulTabel<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-success me-2"></i><%= Common.getBahasaConfig("Rincian Log Presensi Harian") %>';
                    th.innerHTML = '<tr><th class="py-3 px-3" style="width: 50px;">No</th><th class="py-3 px-3 text-start" style="width: 15%;"><%= Common.getBahasaConfig("Personil") %></th><th class="py-3 px-3"><%= Common.getBahasaConfig("Tanggal") %></th><th class="py-3 px-3"><%= Common.getBahasaConfig("Kedatangan") %></th><th class="py-3 px-3"><%= Common.getBahasaConfig("Kepulangan") %></th><th class="py-3 px-3 text-start"><%= Common.getBahasaConfig("Keterangan") %></th></tr>';
                } else if (activeDimension<%=rnd%> === '_service_absensi_pegawai') {
                    document.getElementById('lblJudulTabel<%=rnd%>').innerHTML = '<i class="fas fa-users-cog text-primary me-2"></i><%= Common.getBahasaConfig("Buku Rekapitulasi Presensi Agregat Per Individu") %>';
                    th.innerHTML = '<tr><th class="py-3 px-3">No</th><th class="py-3 px-3 text-start"><%= Common.getBahasaConfig("Identitas Personil") %></th><th class="py-3 px-3"><%= Common.getBahasaConfig("Hadir") %></th><th class="py-3 px-3"><%= Common.getBahasaConfig("Tepat Wkt") %></th><th class="py-3 px-3 text-danger"><%= Common.getBahasaConfig("Terlambat") %></th><th class="py-3 px-3 text-danger"><%= Common.getBahasaConfig("Plg Cepat") %></th><th class="py-3 px-3 text-warning"><%= Common.getBahasaConfig("Tdk Absen Plg") %></th><th class="py-3 px-3 text-info"><%= Common.getBahasaConfig("Sakit/Izin/Cuti") %></th><th class="py-3 px-3 bg-danger text-white"><%= Common.getBahasaConfig("Alpa") %></th></tr>';
                } else if (activeDimension<%=rnd%> === '_service_absensi_pegawai_per_orang') {
                    document.getElementById('lblJudulTabel<%=rnd%>').innerHTML = '<i class="fas fa-address-book text-dark me-2"></i><%= Common.getBahasaConfig("Matriks Kehadiran Per Orang (Vertikal)") %>';
                    th.innerHTML = ''; 
                } else if (activeDimension<%=rnd%> === '_service_absensi_pegawai_per_orang_horizontal') {
                    document.getElementById('lblJudulTabel<%=rnd%>').innerHTML = '<i class="fas fa-calendar-alt text-dark me-2"></i><%= Common.getBahasaConfig("LAPORAN KEHADIRAN HARIAN PERIODE") %> ' + tglM + ' s.d ' + tglS;
                    let headerHtml = '<tr>';
                    dynamicCols<%=rnd%>.forEach(c => { headerHtml += '<th class="py-2 bg-secondary text-white border-end border-light" title="' + c.fullFormat + '" style="min-width: 60px;">' + c.label + '</th>'; });
                    headerHtml += '</tr>';
                    th.innerHTML = headerHtml;
                } else if (activeDimension<%=rnd%> === '_service_absensi_pegawai_per_hari') {
                    document.getElementById('lblJudulTabel<%=rnd%>').innerHTML = '<i class="fas fa-calendar-day text-dark me-2"></i><%= Common.getBahasaConfig("Matriks Kehadiran Per Tanggal") %>';
                    th.innerHTML = ''; 
                } else if (activeDimension<%=rnd%> === '_service_absensi_pegawai_per_orang_rinci') {
                    document.getElementById('lblJudulTabel<%=rnd%>').innerHTML = '<i class="fas fa-clipboard-list text-dark me-2"></i><%= Common.getBahasaConfig("Rekap Rincian Kehadiran Personil Komprehensif") %>';
                    th.innerHTML = ''; 
                }

                window.renderTabelPaging<%=rnd%>();
                window.renderGrafikPresensi<%=rnd%>(result.statistik);
            } else {
                if(typeof tampilkanToast === 'function') tampilkanToast(result.message || 'Galat', 'bg-danger text-white');
            }
        });
    };

    window.renderTabelPaging<%=rnd%> = function() {
        var tbody = document.getElementById('tbodyRekap<%=rnd%>');
        var start = (currentPage<%=rnd%> - 1) * rowsPerPage<%=rnd%>;
        var end = start + rowsPerPage<%=rnd%>;
        var pagedData = allData<%=rnd%>.slice(start, end);
        var html = '';
        const formatDec = new Intl.NumberFormat('id-ID', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        
        if (allData<%=rnd%>.length === 0) {
            html = '<tr><td colspan="15" class="text-center py-5 text-muted"><i class="fas fa-folder-open fa-2x mb-3 opacity-50"></i><br><%= Common.getBahasaConfig("Data tidak ditemukan.") %></td></tr>';
        } else {
        	if (activeDimension<%=rnd%> === '_service_presensi_harian') {
                pagedData.forEach((r, idx) => {
                    html += '<tr>' +
                        '<td class="text-center text-muted small px-3">' + (start + idx + 1) + '</td>' +
                        '<td class="text-start px-3">' +
                            '<div class="fw-bold text-dark fs-6">' + r.nama + '</div>' +
                            '<div class="small text-secondary mt-1"><i class="fas fa-id-badge me-1"></i> ' + r.statusAbsen + '</div>' +
                        '</td>' +
                        '<td class="text-center fw-bold px-3">' + r.tanggal + '</td>' +
                        '<td class="bg-soft-success-<%=rnd%> border-start px-3" style="width: 25%;">' + window.buildPresenceCell<%=rnd%>(r.datang, 'DATANG') + '</td>' +
                        '<td class="bg-soft-primary-<%=rnd%> border-start px-3" style="width: 25%;">' + window.buildPresenceCell<%=rnd%>(r.pulang, 'PULANG') + '</td>' +
                        '<td class="text-start border-start small text-muted italic px-3" style="max-width:250px; white-space:normal; line-height: 1.6;">' + 
                            window.formatLinkKeterangan<%=rnd%>(r.keterangan) + 
                        '</td>' +
                    '</tr>';
                });

            } else if (activeDimension<%=rnd%> === '_service_absensi_pegawai') {
                pagedData.forEach((r, idx) => {
                    let textIzinSakit = 'S: ' + r.sakit + ' | I: ' + r.izin + ' | C: ' + r.cuti;
                    html += '<tr>' +
                        '<td class="text-center text-muted small px-3">' + (start + idx + 1) + '</td>' +
                        '<td class="text-start border-end px-3">' +
                            '<div class="fw-bold text-dark fs-6 text-uppercase">' + r.nama + '</div>' +
                            '<div class="small mt-1">' +
                                '<span class="badge bg-secondary opacity-75">' + r.jenisEntitas + '</span> ' +
                                '<span class="text-muted"><i class="fas fa-fingerprint ms-2 me-1"></i> ' + r.nip + '</span>' +
                            '</div>' +
                        '</td>' +
                        '<td class="text-center bg-soft-success-<%=rnd%> fw-bold fs-5 text-success px-3">' + r.hadir + '</td>' +
                        '<td class="text-center fw-bold text-primary px-3">' + r.tepatWaktu + '</td>' +
                        '<td class="text-center fw-bold text-danger px-3">' + r.terlambat + '</td>' +
                        '<td class="text-center fw-bold text-danger px-3">' + r.pulangCepat + '</td>' +
                        '<td class="text-center fw-bold text-warning px-3" style="color: #856404 !important;">' + r.tidakAbsenPulang + '</td>' +
                        '<td class="text-center fw-bold text-info px-3" style="letter-spacing: 0.5px;">' + textIzinSakit + '</td>' +
                        '<td class="text-center fw-bold text-danger fs-5 bg-danger bg-opacity-10 px-3">' + r.alpa + '</td>' +
                    '</tr>';
                });
            } else if (activeDimension<%=rnd%> === '_service_absensi_pegawai_per_orang') {
                pagedData.forEach((person, idx) => {
                    html += '<tr><td colspan="8" class="bg-light border-0 pt-5 pb-3 px-4">';
                    html += '<h5 class="fw-bold text-dark text-uppercase mb-0"><%= Common.getBahasaConfig("Nama") %> <span class="ms-4 me-2">:</span> ' + person.nama + '</h5>';
                    html += '</td></tr>';
                    
                    html += '<tr class="table-dark text-center align-middle">';
                    html += '   <th class="py-3 bg-secondary text-white" style="width: 20%;"><%= Common.getBahasaConfig("Hari") %></th>';
                    html += '   <th class="py-3 bg-secondary text-white"><%= Common.getBahasaConfig("Jam Kerja") %></th>';
                    html += '   <th class="py-3 bg-secondary text-white"><%= Common.getBahasaConfig("Jam Masuk") %></th>';
                    html += '   <th class="py-3 bg-secondary text-white"><%= Common.getBahasaConfig("Jam Keluar") %></th>';
                    html += '   <th class="py-3 bg-secondary text-white"><%= Common.getBahasaConfig("Terlambat") %></th>';
                    html += '   <th class="py-3 bg-secondary text-white"><%= Common.getBahasaConfig("Cepat Pulang") %></th>';
                    html += '   <th class="py-3 bg-secondary text-white"><%= Common.getBahasaConfig("Lembur") %></th>';
                    html += '   <th class="py-3 bg-secondary text-white text-start" style="width: 30%;"><%= Common.getBahasaConfig("Catatan") %></th>';
                    html += '</tr>';
                    
                    let details = person.rincian || [];
                    details.forEach((d) => {
                        var catatanTerformat = window.formatLinkKeterangan<%=rnd%>(d.catatan);
                        html += '<tr>';
                        html += '   <td class="text-start px-3 text-dark">' + d.hari + '</td>';
                        html += '   <td class="text-center">' + d.jamKerja + '</td>';
                        html += '   <td class="text-center text-success fw-bold">' + d.jamMasuk + '</td>';
                        html += '   <td class="text-center text-primary fw-bold">' + d.jamKeluar + '</td>';
                        html += '   <td class="text-center">' + formatDec.format(d.terlambat) + '</td>';
                        html += '   <td class="text-center">' + formatDec.format(d.cepatPulang) + '</td>';
                        html += '   <td class="text-center">' + formatDec.format(d.lembur) + '</td>';
                        html += '   <td class="text-start small text-muted px-3" style="max-width:300px; white-space:normal;">' + catatanTerformat + '</td>';
                        html += '</tr>';
                    });
                });
            } else if (activeDimension<%=rnd%> === '_service_absensi_pegawai_per_orang_horizontal') {
                pagedData.forEach((p) => {
                    let colCount = dynamicCols<%=rnd%>.length;
                    html += '<tr><td colspan="' + colCount + '" class="bg-light border-0 pt-4 pb-2 px-3"><h5 class="fw-bold text-dark mb-0">' + p.identitas + '</h5></td></tr>';
                    
                    html += '<tr>';
                    dynamicCols<%=rnd%>.forEach(col => {
                        let logs = p.log || {};
                        let valHTML = logs[col.tglKey] || '';
                        html += '<td class="text-center border-end ' + (valHTML === '' ? '' : 'fw-bold fs-6') + '" style="min-width: 60px; line-height: 1.3;">' + valHTML + '</td>';
                    });
                    html += '</tr>';
                });
            } else if (activeDimension<%=rnd%> === '_service_absensi_pegawai_per_hari') {
                pagedData.forEach((dateItem) => {
                    html += '<tr><td colspan="9" class="bg-light border-0 pt-5 pb-3 px-4">';
                    html += '<h5 class="fw-bold text-dark text-uppercase mb-0"><i class="far fa-calendar-check me-2 text-success"></i>' + dateItem.tanggalStr + '</h5>';
                    html += '</td></tr>';

                    html += '<tr class="table-dark text-center align-middle">';
                    html += '   <th class="py-3 bg-secondary text-white" style="width: 50px;">No</th>';
                    html += '   <th class="py-3 bg-secondary text-white text-start"><%= Common.getBahasaConfig("Identitas Personil") %></th>';
                    html += '   <th class="py-3 bg-secondary text-white"><%= Common.getBahasaConfig("Jam Kerja") %></th>';
                    html += '   <th class="py-3 bg-secondary text-white"><%= Common.getBahasaConfig("Jam Masuk") %></th>';
                    html += '   <th class="py-3 bg-secondary text-white"><%= Common.getBahasaConfig("Jam Keluar") %></th>';
                    html += '   <th class="py-3 bg-secondary text-white"><%= Common.getBahasaConfig("Terlambat") %></th>';
                    html += '   <th class="py-3 bg-secondary text-white"><%= Common.getBahasaConfig("Cepat Pulang") %></th>';
                    html += '   <th class="py-3 bg-secondary text-white"><%= Common.getBahasaConfig("Lembur") %></th>';
                    html += '   <th class="py-3 bg-secondary text-white text-start" style="width: 25%;"><%= Common.getBahasaConfig("Catatan") %></th>';
                    html += '</tr>';

                    let details = dateItem.rincian || [];
                    details.forEach((d, i) => {
                        var catatanTerformat = window.formatLinkKeterangan<%=rnd%>(d.catatan);
                        html += '<tr>';
                        html += '   <td class="text-center px-3 text-muted">' + (i + 1) + '</td>';
                        html += '   <td class="text-start px-3"><div class="fw-bold text-dark">' + d.nama + '</div><div class="small text-secondary mt-1"><span class="badge bg-light text-dark border">' + d.entitas + '</span> ' + d.nip + '</div></td>';
                        html += '   <td class="text-center">' + d.jamKerja + '</td>';
                        html += '   <td class="text-center text-success fw-bold">' + d.jamMasuk + '</td>';
                        html += '   <td class="text-center text-primary fw-bold">' + d.jamKeluar + '</td>';
                        html += '   <td class="text-center">' + formatDec.format(d.terlambat) + '</td>';
                        html += '   <td class="text-center">' + formatDec.format(d.cepatPulang) + '</td>';
                        html += '   <td class="text-center">' + formatDec.format(d.lembur) + '</td>';
                        html += '   <td class="text-start small text-muted px-3" style="max-width:250px; white-space:normal;">' + catatanTerformat + '</td>';
                        html += '</tr>';
                    });
                });
            } else if (activeDimension<%=rnd%> === '_service_absensi_pegawai_per_orang_rinci') {
                pagedData.forEach((p) => {
                    let s = p.summary || {};
                    html += '<tr><td colspan="10" class="p-0 border-0 bg-light"><div class="card shadow-sm m-4 border-0 rounded-4">';
                    
                    html += '<div class="card-header bg-white border-bottom py-3 px-4">';
                    html += '  <h5 class="fw-bold text-dark text-uppercase mb-1">' + p.nama + '</h5>';
                    html += '  <div class="small text-secondary"><span class="badge bg-secondary me-2">' + p.entitas + '</span> NIP: ' + p.nip + ' | Satker: ' + p.dept + '</div>';
                    html += '</div>';

                    html += '<div class="card-body bg-light bg-opacity-50 p-4 border-bottom">';
                    html += '  <div class="row g-3 text-center">';
                    html += '    <div class="col-md-3 col-sm-6"><div class="p-3 border rounded-3 bg-white shadow-sm"><div class="small text-muted fw-bold text-uppercase mb-1"><%= Common.getBahasaConfig("Total Masuk") %></div><h4 class="text-success mb-0">' + s.sHadir + ' <small class="fs-6 text-muted fw-normal"><%= Common.getBahasaConfig("Hari") %></small></h4></div></div>';
                    html += '    <div class="col-md-3 col-sm-6"><div class="p-3 border rounded-3 bg-white shadow-sm"><div class="small text-muted fw-bold text-uppercase mb-1"><%= Common.getBahasaConfig("Terlambat") %></div><h4 class="text-danger mb-0">' + s.sTelatH + ' <small class="fs-6 text-muted fw-normal"><%= Common.getBahasaConfig("Hari") %></small></h4><div class="small text-muted mt-1">' + formatDec.format(s.sTelatJ) + ' Jam</div></div></div>';
                    html += '    <div class="col-md-3 col-sm-6"><div class="p-3 border rounded-3 bg-white shadow-sm"><div class="small text-muted fw-bold text-uppercase mb-1"><%= Common.getBahasaConfig("Cepat Keluar") %></div><h4 class="text-warning text-darken-2 mb-0">' + s.sCepatH + ' <small class="fs-6 text-muted fw-normal"><%= Common.getBahasaConfig("Hari") %></small></h4><div class="small text-muted mt-1">' + formatDec.format(s.sCepatJ) + ' Jam</div></div></div>';
                    html += '    <div class="col-md-3 col-sm-6"><div class="p-3 border rounded-3 bg-white shadow-sm"><div class="small text-muted fw-bold text-uppercase mb-1"><%= Common.getBahasaConfig("Lembur") %></div><h4 class="text-primary mb-0">' + formatDec.format(s.sLemburJ) + ' <small class="fs-6 text-muted fw-normal"><%= Common.getBahasaConfig("Jam") %></small></h4></div></div>';
                    
                    html += '    <div class="col-md-3 col-sm-6"><div class="p-3 border rounded-3 bg-danger bg-opacity-10"><div class="small text-danger fw-bold text-uppercase mb-1"><%= Common.getBahasaConfig("Total Alpa") %></div><h5 class="text-danger mb-0">' + s.sAlpa + ' <small class="fs-6 fw-normal"><%= Common.getBahasaConfig("Hari") %></small></h5></div></div>';
                    html += '    <div class="col-md-3 col-sm-6"><div class="p-3 border rounded-3 bg-info bg-opacity-10"><div class="small text-info text-darken-2 fw-bold text-uppercase mb-1"><%= Common.getBahasaConfig("Total Sakit") %></div><h5 class="text-info text-darken-2 mb-0">' + s.sSakit + ' <small class="fs-6 fw-normal"><%= Common.getBahasaConfig("Hari") %></small></h5></div></div>';
                    html += '    <div class="col-md-3 col-sm-6"><div class="p-3 border rounded-3 bg-info bg-opacity-10"><div class="small text-info text-darken-2 fw-bold text-uppercase mb-1"><%= Common.getBahasaConfig("Total Izin") %></div><h5 class="text-info text-darken-2 mb-0">' + s.sIzin + ' <small class="fs-6 fw-normal"><%= Common.getBahasaConfig("Hari") %></small></h5></div></div>';
                    html += '    <div class="col-md-3 col-sm-6"><div class="p-3 border rounded-3 bg-primary bg-opacity-10"><div class="small text-primary fw-bold text-uppercase mb-1"><%= Common.getBahasaConfig("Total Cuti") %></div><h5 class="text-primary mb-0">' + s.sCuti + ' <small class="fs-6 fw-normal"><%= Common.getBahasaConfig("Hari") %></small></h5></div></div>';
                    html += '  </div></div>';

                    html += '<div class="card-body p-0">';
                    html += '  <table class="table table-hover table-sm mb-0">';
                    html += '    <thead class="table-dark text-center align-middle">';
                    html += '       <tr><th class="py-3 px-2">No</th><th class="py-3 px-3 text-start"><%= Common.getBahasaConfig("Hari, Tanggal") %></th><th class="py-3 px-3"><%= Common.getBahasaConfig("Jam Kerja") %></th><th class="py-3 px-3"><%= Common.getBahasaConfig("Masuk") %></th><th class="py-3 px-3"><%= Common.getBahasaConfig("Pulang") %></th><th class="py-3 px-3"><%= Common.getBahasaConfig("Terlambat") %></th><th class="py-3 px-3"><%= Common.getBahasaConfig("Cepat KLR") %></th><th class="py-3 px-3"><%= Common.getBahasaConfig("Lembur") %></th><th class="py-3 px-3 text-start" style="width: 25%;"><%= Common.getBahasaConfig("Keterangan") %></th></tr>';
                    html += '    </thead><tbody>';
                    
                    let details = p.rincian || [];
                    details.forEach((d, i) => {
                        var catatanTerformat = window.formatLinkKeterangan<%=rnd%>(d.catatan);
                        html += '<tr>';
                        html += '   <td class="text-center text-muted px-2">' + (i + 1) + '</td>';
                        html += '   <td class="text-start px-3 text-dark fw-bold">' + d.hari + '</td>';
                        html += '   <td class="text-center px-3">' + d.jamKerja + '</td>';
                        html += '   <td class="text-center px-3 text-success fw-bold">' + d.jamMasuk + '</td>';
                        html += '   <td class="text-center px-3 text-primary fw-bold">' + d.jamKeluar + '</td>';
                        html += '   <td class="text-center px-3">' + formatDec.format(d.terlambat) + '</td>';
                        html += '   <td class="text-center px-3">' + formatDec.format(d.cepatPulang) + '</td>';
                        html += '   <td class="text-center px-3">' + formatDec.format(d.lembur) + '</td>';
                        html += '   <td class="text-start px-3 small text-muted" style="max-width:300px; white-space:normal;">' + catatanTerformat + '</td>';
                        html += '</tr>';
                    });
                    
                    html += '    </tbody></table></div>';
                    html += '</div></td></tr>';
                });
            }
        }
        tbody.innerHTML = html;
        window.updatePaginationControls<%=rnd%>();
    };

    window.buildPresenceCell<%=rnd%> = function(info, type) {
        if (!info || info.jam === '-') return '<div class="text-muted small italic">-- <%= Common.getBahasaConfig("Tidak Ada Data") %> --</div>';
        var isDatang = type === 'DATANG';
        var badgeColor = isDatang ? 'bg-success' : 'bg-info';
        var out = '<div class="d-flex flex-column gap-2">';
        out += '<div><span class="fs-4 fw-bold d-block">' + info.jam + '</span><span class="badge ' + badgeColor + ' bg-opacity-10 ' + info.css + ' border border-opacity-25">' + info.status + '</span></div>';
        if (info.foto) out += '<div><label class="text-muted x-small-<%=rnd%> d-block mb-1"><i class="fas fa-camera me-1"></i> ' + (isDatang ? 'Foto Datang' : 'Foto Pulang') + '</label><img src="' + info.foto + '" class="img-fluid rounded shadow-sm border p-1 img-thumbnail-<%=rnd%>" style="max-width:140px;" onclick="window.open(\''+info.foto+'\')"></div>';
        if (info.lokasi) out += '<div><label class="text-muted x-small-<%=rnd%> d-block mb-1"><i class="fas fa-map-marker-alt me-1"></i> ' + (isDatang ? 'Lokasi Datang' : 'Lokasi Pulang') + '</label><div class="rounded border overflow-hidden shadow-sm" style="height:120px;"><iframe style="width:100%; height:100%; border:0;" src="' + info.lokasi + '&output=embed"></iframe></div></div>';
        out += '</div>'; return out;
    };
    
    window.updatePaginationControls<%=rnd%> = function() {
        var totalPages = Math.ceil(allData<%=rnd%>.length / rowsPerPage<%=rnd%>);
        var info = document.getElementById('paginationInfo<%=rnd%>');
        var controls = document.getElementById('paginationControls<%=rnd%>');
        var startItem = allData<%=rnd%>.length === 0 ? 0 : (currentPage<%=rnd%> - 1) * rowsPerPage<%=rnd%> + 1;
        var endItem = Math.min(currentPage<%=rnd%> * rowsPerPage<%=rnd%>, allData<%=rnd%>.length);
        info.innerHTML = '<%= Common.getBahasaConfigJS("Menampilkan") %> ' + startItem + ' - ' + endItem + ' <%= Common.getBahasaConfig("dari") %> ' + allData<%=rnd%>.length + ' <%= Common.getBahasaConfig("catatan") %>';
        var html = '<li class="page-item ' + (currentPage<%=rnd%> === 1 ? 'disabled' : '') + '"><a class="page-link" href="javascript:void(0)" onclick="window.changePage<%=rnd%>(' + (currentPage<%=rnd%> - 1) + ')"><i class="fas fa-chevron-left"></i></a></li>';
        for (var i = 1; i <= Math.min(5, totalPages); i++) html += '<li class="page-item ' + (i === currentPage<%=rnd%> ? 'active' : '') + '"><a class="page-link" href="javascript:void(0)" onclick="window.changePage<%=rnd%>(' + i + ')">' + i + '</a></li>';
        if (totalPages > 5) html += '<li class="page-item disabled"><span class="page-link">...</span></li>';
        html += '<li class="page-item ' + (currentPage<%=rnd%> === totalPages || totalPages === 0 ? 'disabled' : '') + '"><a class="page-link" href="javascript:void(0)" onclick="window.changePage<%=rnd%>(' + (currentPage<%=rnd%> + 1) + ')"><i class="fas fa-chevron-right"></i></a></li>';
        controls.innerHTML = html;
    };

    window.changePage<%=rnd%> = function(p) { currentPage<%=rnd%> = p; window.renderTabelPaging<%=rnd%>(); };
    
    window.renderGrafikPresensi<%=rnd%> = function(stat) {
        if (!stat) return;
        if (chartStatus<%=rnd%>) chartStatus<%=rnd%>.destroy();
        if (chartEntitas<%=rnd%>) chartEntitas<%=rnd%>.destroy();
        chartStatus<%=rnd%> = new Chart(document.getElementById('chartStatusPresensi<%=rnd%>').getContext('2d'), { type: 'doughnut', data: { labels: ['<%= Common.getBahasaConfigJS("Tepat Waktu") %>', '<%= Common.getBahasaConfigJS("Terlambat") %>', '<%= Common.getBahasaConfigJS("Cepat Pulang") %>', '<%= Common.getBahasaConfigJS("Lainnya") %>'], datasets: [{ data: [stat.tepatWaktu, stat.terlambat, stat.cepatPulang, stat.tidakAbsen], backgroundColor: ['#198754', '#ffc107', '#0dcaf0', '#dc3545'], borderWidth: 2 }] }, options: { responsive: true, maintainAspectRatio: false, cutout: '70%', plugins: { legend: { position: 'bottom' } } } });
        chartEntitas<%=rnd%> = new Chart(document.getElementById('chartEntitas<%=rnd%>').getContext('2d'), { type: 'bar', data: { labels: ['<%= Common.getBahasaConfigJS("Pegawai") %>', '<%= Common.getBahasaConfigJS("Dosen") %>', '<%= Common.getBahasaConfigJS("Guru") %>', '<%= Common.getBahasaConfigJS("Mahasiswa") %>', '<%= Common.getBahasaConfigJS("Siswa") %>'], datasets: [{ label: '<%= Common.getBahasaConfigJS("Frekuensi Kehadiran") %>', data: [stat.jmlPegawai, stat.jmlDosen, stat.jmlGuru, stat.jmlMahasiswa, stat.jmlSiswa], backgroundColor: 'rgba(13, 110, 253, 0.85)', borderRadius: 6 }] }, options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } } });
    };

    window.downloadExcelPresensi<%=rnd%> = function() {
        if(typeof XLSX === 'undefined') return;
        var table = document.getElementById("tabelRekapPresensi<%=rnd%>");
        
        let fileName = "Data_Presensi.xlsx";
        if (activeDimension<%=rnd%> === '_service_presensi_harian') fileName = "Log_Presensi_Harian.xlsx";
        else if (activeDimension<%=rnd%> === '_service_absensi_pegawai') fileName = "Rekapitulasi_Kehadiran_Individu.xlsx";
        else if (activeDimension<%=rnd%> === '_service_absensi_pegawai_per_orang') fileName = "Matriks_Personil_Vertikal.xlsx";
        else if (activeDimension<%=rnd%> === '_service_absensi_pegawai_per_orang_horizontal') fileName = "Matriks_Horizontal.xlsx";
        else if (activeDimension<%=rnd%> === '_service_absensi_pegawai_per_hari') fileName = "Matriks_Per_Tanggal.xlsx";
        else if (activeDimension<%=rnd%> === '_service_absensi_pegawai_per_orang_rinci') fileName = "Rekap_Dan_Rincian_Pegawai.xlsx";
        
        XLSX.writeFile(XLSX.utils.table_to_book(table, {sheet: "Data Presensi", raw: false}), fileName);
    };
    
    setTimeout(function() { window.loadDataDasborPresensi<%=rnd%>(); }, 400);
</script>