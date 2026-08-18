<%@page import="ais.database.model.sekolah.KelompokMatapelajaran"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.sekolah.Yayasan"%>
<%@page import="ais.database.model.sekolah.Matapelajaran"%>
<%@page import="ais.database.model.sekolah.JenisPenilaian"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // =========================================================================
    // BLOK TANGKAP VARIABEL RND UNTUK MENCEGAH BENTROK ID
    // =========================================================================
    String rnd = request.getParameter("rnd");
    if (rnd == null || rnd.trim().isEmpty()) {
        rnd = Common.getGeneratedBarCode(7);
    }
    // Prefix unik untuk modul Matapelajaran
    rnd = "Mpl" + rnd;

    // =========================================================================
    // BLOK SERVER-SIDE KHUSUS UNTUK MERENDER TABEL DETAIL POPUP
    // =========================================================================
    String actionParams = request.getParameter("action_jsp");
    if ("get_html_detail".equals(actionParams)) {
        String sql = request.getParameter("sql");
        int start = Integer.parseInt(request.getParameter("start") != null ? request.getParameter("start") : "0");
        int limit = Integer.parseInt(request.getParameter("limit") != null ? request.getParameter("limit") : "10");
        
        Session sess = HibernateUtil.openSession();
        try {
            List<Matapelajaran> list = sess.createSQLQuery(sql)
                                           .addEntity(Matapelajaran.class)
                                           .setFirstResult(start)
                                           .setMaxResults(limit)
                                           .list();
                                           
            for (int i = 0; i < list.size(); i++) {
                Matapelajaran m = list.get(i);
                
                String kode = m.getKode() != null ? m.getKode() : "-";
                String nama = m.getNama() != null ? m.getNama() : "-";
                String prodi = m.getSekolah() != null ? m.getSekolah().getNama() : "-";
                String kelompok = m.getKelompokMatapelajaran() != null ? m.getKelompokMatapelajaran().getNama() : "-";
                
                String jenis = "-";
                if (m.getJenisPenilaian() != null) {
                    jenis = m.getJenisPenilaian().getJenis() != null ? m.getJenisPenilaian().getJenis() : "-";
                }
                
                String sksTotal = m.getKkm() != null ? Common.numberFormat.get().format(m.getKkm()) : "0";
                
                // Menampilkan status Keterampilan & Predikat di rincian
                String keterampilan = m.getTerdapatNilaiKeterampilan() != null && m.getTerdapatNilaiKeterampilan() ? "<span class='badge bg-success'>Ada</span>" : "<span class='badge bg-secondary'>Tidak Ada</span>";
                String predikat = m.getTerdapatNilaiPredikat() != null && m.getTerdapatNilaiPredikat() ? "<span class='badge bg-success'>Ada</span>" : "<span class='badge bg-secondary'>Tidak Ada</span>";
                
                %>
                <tr>
                    <td class='text-center'><%= (start + i + 1) %></td>
                    <td class='fw-bold text-primary'><%= kode %></td>
                    <td class='fw-bold'><%= nama %></td>
                    <td><%= prodi %></td>
                    <td class='text-center fw-bold'><%= sksTotal %> KKM</td>
                    <td><%= kelompok %></td>
                    <td><%= jenis %></td>
                    <td class='text-center'><%= keterampilan %></td>
                    <td class='text-center'><%= predikat %></td>
                </tr>
                <%
            }
        } catch(Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pagesmastersekolahmatapelajaranzul/_statistik_matapelajaran.jsp:78");
            out.print("<tr><td colspan='9' class='text-center text-danger'>" + Common.getBahasaConfig("Terjadi kesalahan kueri data.") + "</td></tr>");
        } finally {
            try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersekolahmatapelajaranzul/_statistik_matapelajaran.jsp:81");}
            try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersekolahmatapelajaranzul/_statistik_matapelajaran.jsp:82");}
            HibernateUtil.closeSession();
        }
        return; 
    }

    // =========================================================================
    // BLOK HALAMAN UTAMA STATISTIK MATAPELAJARAN
    // =========================================================================
    List<Yayasan> listYayasan = new ArrayList<Yayasan>();
    List<Sekolah> listSekolah = new ArrayList<Sekolah>();
    List<KelompokMatapelajaran> listKelompok = new ArrayList<KelompokMatapelajaran>();
    List<JenisPenilaian> listJenisPenilaian = new ArrayList<JenisPenilaian>();
    
    Session sessFilter = HibernateUtil.openSession();
    try {
        listYayasan = ConstantValues.simpleList(sessFilter.createCriteria(Yayasan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Yayasan.class);
        listSekolah = ConstantValues.simpleList(sessFilter.createCriteria(Sekolah.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Sekolah.class);
        listKelompok = ConstantValues.simpleList(sessFilter.createCriteria(KelompokMatapelajaran.class).addOrder(Order.asc("nama")), KelompokMatapelajaran.class);
        listJenisPenilaian = sessFilter.createQuery("from JenisPenilaian order by jenis").list();
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pagesmastersekolahmatapelajaranzul/_statistik_matapelajaran.jsp:103");
    } finally {
        try { sessFilter.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersekolahmatapelajaranzul/_statistik_matapelajaran.jsp:105");}
        try { sessFilter.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersekolahmatapelajaranzul/_statistik_matapelajaran.jsp:106");}
        HibernateUtil.closeSession();
    }
%>

<div class="container-fluid p-0 animate__animated animate__fadeIn">
    <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3">
        <h5 class="mb-0 fw-bold text-primary">
            <i class="fas fa-book-open me-2"></i><%= Common.getBahasaConfig("Dashboard Statistik Mata Pelajaran") %>
        </h5>
    </div>

    <div class="card shadow-sm border-0 rounded-4 mb-4">
        <div class="card-body bg-light rounded-4 p-4">
            <div class="row g-3">
                <div class="col-md-12 mb-2">
                    <label class="form-label fw-bold text-secondary small"><i class="fas fa-list me-1"></i><%= Common.getBahasaConfig("Pilih Jenis Laporan:") %></label>
                    <select class="form-select border-primary shadow-sm fw-bold" id="jenisLaporan<%=rnd%>" onchange="loadStatistik<%=rnd%>()">
                        <option value="1"><%= Common.getBahasaConfig("1. Sebaran per Sekolah & Kelompok Mata Pelajaran") %></option>
                        <option value="2"><%= Common.getBahasaConfig("2. Sebaran per Sekolah & Jenis Penilaian") %></option>
                        <option value="3"><%= Common.getBahasaConfig("3. Sebaran Mata Pelajaran Berdasarkan Nilai KKM") %></option>
                        <option value="4"><%= Common.getBahasaConfig("4. Sebaran Mata Pelajaran Berdasarkan Nilai Keterampilan per Sekolah") %></option>
                        <option value="6"><%= Common.getBahasaConfig("5. Sebaran Mata Pelajaran Berdasarkan Nilai Predikat per Sekolah") %></option>
                    </select>
                </div>
                
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-building me-1"></i> <%= Common.getBahasaConfig("Yayasan") %></label>
                    <select id="filterYayasanStat<%=rnd%>" class="form-select form-select-sm border-info shadow-none" onchange="
                        var yayId = this.value;
                        var sekSelect = document.getElementById('filterSekolahStat<%=rnd%>');
                        sekSelect.value = '';
                        for(var i=1; i<sekSelect.options.length; i++){
                            var opt = sekSelect.options[i];
                            if(yayId === '' || opt.getAttribute('data-yayasan') === yayId){
                                opt.style.display = ''; opt.disabled = false;
                            } else {
                                opt.style.display = 'none'; opt.disabled = true;
                            }
                        }
                        loadStatistik<%=rnd%>();
                    ">
                        <option value=""><%= Common.getBahasaConfig("Semua Yayasan") %></option>
                        <% for(Yayasan y : listYayasan) { %><option value="<%= y.getId() %>"><%= y.getNama() %></option><% } %>
                    </select>
                </div>
                
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-school me-1"></i> <%= Common.getBahasaConfig("Sekolah") %></label>
                    <select id="filterSekolahStat<%=rnd%>" class="form-select form-select-sm border-info shadow-none" onchange="loadStatistik<%=rnd%>()">
                        <option value="" data-yayasan=""><%= Common.getBahasaConfig("Semua Sekolah") %></option>
                        <% for(Sekolah s : listSekolah) { 
                             String yayIdStr = (s.getYayasan() != null) ? s.getYayasan().getId().toString() : "";
                        %>
                            <option value="<%= s.getId() %>" data-yayasan="<%= yayIdStr %>"><%= s.getNama() %></option>
                        <% } %>
                    </select>
                </div>

                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-layer-group me-1"></i> <%= Common.getBahasaConfig("Kelompok") %></label>
                    <select id="filterKelompokStat<%=rnd%>" class="form-select form-select-sm border-info shadow-none" onchange="loadStatistik<%=rnd%>()">
                        <option value=""><%= Common.getBahasaConfig("Semua Kelompok") %></option>
                        <% for(KelompokMatapelajaran km : listKelompok) { %><option value="<%= km.getId() %>"><%= km.getNama() %></option><% } %>
                    </select>
                </div>

                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-check-square me-1"></i> <%= Common.getBahasaConfig("Jenis Penilaian") %></label>
                    <select id="filterJenisPenilaianStat<%=rnd%>" class="form-select form-select-sm border-info shadow-none" onchange="loadStatistik<%=rnd%>()">
                        <option value=""><%= Common.getBahasaConfig("Semua Penilaian") %></option>
                        <% for(JenisPenilaian jp : listJenisPenilaian) { %><option value="<%= jp.getId() %>"><%= jp.getJenis() %></option><% } %>
                    </select>
                </div>
            </div>

            <div class="row mt-3 border-top pt-3">
                <div class="col-12 d-flex flex-wrap gap-2 justify-content-start mt-2">
                    <button class="btn btn-primary btn-sm rounded-pill px-4 fw-bold shadow-sm" onclick="loadStatistik<%=rnd%>()">
                        <i class="fas fa-search me-1"></i> <%= Common.getBahasaConfig("Terapkan Filter") %>
                    </button>
                    <button class="btn btn-success btn-sm rounded-pill px-4 fw-bold shadow-sm" onclick="downloadExcel<%=rnd%>()">
                        <i class="fas fa-file-excel me-1"></i><%= Common.getBahasaConfig("Unduh Rekap (Excel)") %>
                    </button>
                </div>
            </div>
        </div>
    </div>

    <div class="card shadow-sm border-0 rounded-4 mb-4">
        <div class="card-body p-4">
            <div style="position: relative; height: 350px; width: 100%;">
                <canvas id="chartMatapelajaran<%=rnd%>"></canvas>
            </div>
        </div>
    </div>

    <div class="card shadow-sm border-0 rounded-4 mb-4">
        <div class="card-header bg-white border-bottom py-3">
            <h6 class="mb-0 fw-bold text-dark" id="judulTabel<%=rnd%>"><%= Common.getBahasaConfig("Memuat Data...") %></h6>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover table-striped align-middle mb-0" id="tabelRekap<%=rnd%>">
                    <thead class="bg-light text-secondary" id="theadRekap<%=rnd%>"></thead>
                    <tbody id="tbodyRekap<%=rnd%>"></tbody>
                </table>
            </div>
            <div id="loaderStatistik<%=rnd%>" class="text-center p-5 d-none">
                <div class="spinner-border text-primary mb-2" role="status"></div>
                <p class="text-muted small fw-bold"><%= Common.getBahasaConfig("Sedang menarik data dari peladen...") %></p>
            </div>
        </div>
        <div class="card-footer bg-white border-top p-3 d-flex justify-content-between align-items-center">
            <span class="small text-muted fw-bold" id="infoPaginasi<%=rnd%>"><%= Common.getBahasaConfig("Menampilkan 0 data") %></span>
            <nav><ul class="pagination pagination-sm mb-0 shadow-sm" id="paginasiTabel<%=rnd%>"></ul></nav>
        </div>
    </div>
</div>

<script>
let chartInstance<%=rnd%> = null;
let currentData<%=rnd%> = [];
let currentPage<%=rnd%> = 1;
const itemsPerPage<%=rnd%> = 10;
let currentConfig<%=rnd%> = null;

// ==========================================
// KONFIGURASI SQL REKAPITULASI MATAPELAJARAN
// ==========================================
const baseMpJoinSql = " FROM sekolah.matapelajaran m " +
                      "LEFT JOIN sekolah.sekolah s ON m.sekolah_id = s.id " +
                      "LEFT JOIN sekolah.yayasan y ON s.yayasan_id = y.id " +
                      "LEFT JOIN sekolah.kelompok_matapelajaran km ON m.kelompok_matapelajaran_id = km.id " +
                      "LEFT JOIN sekolah.jenis_penilaian jp ON m.jenis_penilaian_id = jp.id " +
                      "WHERE m.id IS NOT NULL AND (m.aktif = true OR m.aktif IS NULL) ";

const reportConfigs<%=rnd%> = {
    "1": {
        title: "<%= Common.getBahasaConfig("Sebaran per Sekolah & Kelompok Mata Pelajaran") %>",
        columns: ["Sekolah", "Kelompok Mata Pelajaran", "Total Mata Pelajaran"],
        keys: ["label1", "label2", "total"],
        sql: "SELECT s.nama as label1, COALESCE(km.nama, 'Tanpa Kelompok') as label2, COUNT(m.id) as total " + baseMpJoinSql + " {FILTER} GROUP BY s.nama, km.nama ORDER BY s.nama ASC, km.nama ASC"
    },
    "2": {
        title: "<%= Common.getBahasaConfig("Sebaran per Sekolah & Jenis Penilaian") %>",
        columns: ["Sekolah", "Jenis Penilaian", "Total Mata Pelajaran"],
        keys: ["label1", "label2", "total"],
        sql: "SELECT s.nama as label1, COALESCE(jp.jenis, 'Belum Diatur') as label2, COUNT(m.id) as total " + baseMpJoinSql + " {FILTER} GROUP BY s.nama, jp.jenis ORDER BY s.nama ASC, jp.jenis ASC"
    },
    "3": {
        title: "<%= Common.getBahasaConfig("Sebaran Mata Pelajaran Berdasarkan Nilai KKM") %>",
        columns: ["Nilai KKM", "Total Mata Pelajaran"],
        keys: ["label1", "total"],
        sql: "SELECT COALESCE(CAST(m.kkm AS VARCHAR), '0') as label1, COUNT(m.id) as total " + baseMpJoinSql + " {FILTER} GROUP BY m.kkm ORDER BY m.kkm ASC"
    },
    "4": {
        title: "<%= Common.getBahasaConfig("Sebaran Mata Pelajaran Berdasarkan Nilai Keterampilan per Sekolah") %>",
        columns: ["Sekolah", "Status Nilai Keterampilan", "Total Mata Pelajaran"],
        keys: ["label1", "label2", "total"],
        sql: "SELECT s.nama as label1, CASE WHEN m.terdapatnilaiketerampilan = true THEN 'Ada Keterampilan' ELSE 'Tidak Ada / Belum Diatur' END as label2, COUNT(m.id) as total " + baseMpJoinSql + " {FILTER} GROUP BY s.nama, m.terdapatnilaiketerampilan ORDER BY s.nama ASC"
    },
    "6": {
        title: "<%= Common.getBahasaConfig("Sebaran Mata Pelajaran Berdasarkan Nilai Predikat per Sekolah") %>",
        columns: ["Sekolah", "Status Nilai Predikat", "Total Mata Pelajaran"],
        keys: ["label1", "label2", "total"],
        sql: "SELECT s.nama as label1, CASE WHEN m.terdapatnilaipredikat = true THEN 'Ada Predikat' ELSE 'Tidak Ada / Belum Diatur' END as label2, COUNT(m.id) as total " + baseMpJoinSql + " {FILTER} GROUP BY s.nama, m.terdapatnilaipredikat ORDER BY s.nama ASC"
    }
};

const fetchDataAPI<%=rnd%> = async (sql) => {
    try {
        const res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sql: sql, action: "sql", tanpaLogin: "true" })
        });
        const result = await res.json();
        return result.data || [];
    } catch (e) { return []; }
};

const getFilterSql<%=rnd%> = () => {
    let filterSql = "";
    const yayId = document.getElementById('filterYayasanStat<%=rnd%>').value;
    const sekId = document.getElementById('filterSekolahStat<%=rnd%>').value;
    const kelId = document.getElementById('filterKelompokStat<%=rnd%>').value;
    const jenisPnId = document.getElementById('filterJenisPenilaianStat<%=rnd%>').value;

    if (yayId) filterSql += " AND y.id = " + yayId + " ";
    if (sekId) filterSql += " AND s.id = " + sekId + " ";
    if (kelId) filterSql += " AND m.kelompok_matapelajaran_id = " + kelId + " ";
    if (jenisPnId) filterSql += " AND m.jenis_penilaian_id = " + jenisPnId + " ";

    return filterSql;
};

const loadStatistik<%=rnd%> = async () => {
    const selector = document.getElementById("jenisLaporan<%=rnd%>").value;
    currentConfig<%=rnd%> = reportConfigs<%=rnd%>[selector];
    document.getElementById("judulTabel<%=rnd%>").innerText = currentConfig<%=rnd%>.title;
    document.getElementById("tbodyRekap<%=rnd%>").innerHTML = "";
    document.getElementById("loaderStatistik<%=rnd%>").classList.remove("d-none");
    
    const filterParams = getFilterSql<%=rnd%>();
    const finalSql = currentConfig<%=rnd%>.sql.replace(/{FILTER}/g, filterParams);
    
    currentData<%=rnd%> = await fetchDataAPI<%=rnd%>(finalSql);
    currentPage<%=rnd%> = 1;
    document.getElementById("loaderStatistik<%=rnd%>").classList.add("d-none");
    
    renderHeaderTabel<%=rnd%>(); renderTabel<%=rnd%>(); renderChart<%=rnd%>();
};

const renderHeaderTabel<%=rnd%> = () => {
    let html = "<tr><th class='text-center' style='width: 50px;'>No</th>";
    currentConfig<%=rnd%>.columns.forEach(col => { html += "<th>" + col + "</th>"; });
    html += "</tr>";
    document.getElementById("theadRekap<%=rnd%>").innerHTML = html;
};

const renderTabel<%=rnd%> = () => {
    const tbody = document.getElementById("tbodyRekap<%=rnd%>");
    if(currentData<%=rnd%>.length === 0) {
        tbody.innerHTML = "<tr><td colspan='" + (currentConfig<%=rnd%>.columns.length + 1) + "' class='text-center text-muted p-4'><%= Common.getBahasaConfig("Tidak ada data ditemukan") %></td></tr>";
        document.getElementById("infoPaginasi<%=rnd%>").innerText = "<%= Common.getBahasaConfig("Menampilkan 0 data") %>";
        document.getElementById("paginasiTabel<%=rnd%>").innerHTML = "";
        return;
    }
    
    const startIdx = (currentPage<%=rnd%> - 1) * itemsPerPage<%=rnd%>;
    const endIdx = Math.min(startIdx + itemsPerPage<%=rnd%>, currentData<%=rnd%>.length);
    const pagedData = currentData<%=rnd%>.slice(startIdx, endIdx);
    
    let html = "";
    pagedData.forEach((row, idx) => {
        html += "<tr><td class='text-center fw-bold'>" + (startIdx + idx + 1) + "</td>";
        currentConfig<%=rnd%>.keys.forEach(key => {
            const val = row[key] !== null ? row[key] : "-";
            if (key === 'total') {
                html += "<td><button class='btn btn-sm btn-outline-primary rounded-pill fw-bold shadow-sm' onclick='bukaDetailStatistik<%=rnd%>(" + (startIdx + idx) + ", event)'>" + val + " <i class='fas fa-search-plus ms-1'></i></button></td>";
            } else { html += "<td>" + val + "</td>"; }
        });
        html += "</tr>";
    });
    tbody.innerHTML = html;
    renderPaginasi<%=rnd%>(startIdx, endIdx, currentData<%=rnd%>.length);
};

const renderPaginasi<%=rnd%> = (start, end, total) => {
    document.getElementById("infoPaginasi<%=rnd%>").innerText = "Menampilkan " + (start + 1) + " - " + end + " dari " + total + " data";
    const totalPages = Math.ceil(total / itemsPerPage<%=rnd%>);
    let html = "<li class='page-item " + (currentPage<%=rnd%> === 1 ? "disabled" : "") + "'><button class='page-link' onclick='ubahHalaman<%=rnd%>(" + (currentPage<%=rnd%> - 1) + ")'><i class='fas fa-angle-left'></i></button></li>";
    for(let i = 1; i <= totalPages; i++) {
        if(i === 1 || i === totalPages || (i >= currentPage<%=rnd%> - 1 && i <= currentPage<%=rnd%> + 1)) {
            html += "<li class='page-item " + (currentPage<%=rnd%> === i ? "active" : "") + "'><button class='page-link' onclick='ubahHalaman<%=rnd%>(" + i + ")'>" + i + "</button></li>";
        } else if (i === currentPage<%=rnd%> - 2 || i === currentPage<%=rnd%> + 2) {
            html += "<li class='page-item disabled'><span class='page-link'>...</span></li>";
        }
    }
    html += "<li class='page-item " + (currentPage<%=rnd%> === totalPages ? "disabled" : "") + "'><button class='page-link' onclick='ubahHalaman<%=rnd%>(" + (currentPage<%=rnd%> + 1) + ")'><i class='fas fa-angle-right'></i></button></li>";
    document.getElementById("paginasiTabel<%=rnd%>").innerHTML = html;
};

const ubahHalaman<%=rnd%> = (page) => { currentPage<%=rnd%> = page; renderTabel<%=rnd%>(); };

const renderChart<%=rnd%> = () => {
    const ctx = document.getElementById('chartMatapelajaran<%=rnd%>').getContext('2d');
    if(chartInstance<%=rnd%>) chartInstance<%=rnd%>.destroy();
    
    let labels = [], totals = [];
    const chartData = currentData<%=rnd%>.slice(0, 20);
    
    chartData.forEach(row => {
        let lbl = "";
        currentConfig<%=rnd%>.keys.forEach(k => { if(k !== 'total') lbl += row[k] + " "; });
        labels.push(lbl.trim());
        totals.push(row['total']);
    });
    
    chartInstance<%=rnd%> = new Chart(ctx, {
        type: 'bar',
        data: { labels: labels, datasets: [{ label: '<%= Common.getBahasaConfigJS("Jumlah MP") %>', data: totals, backgroundColor: 'rgba(253, 126, 20, 0.7)', borderColor: 'rgba(253, 126, 20, 1)', borderWidth: 1, borderRadius: 4 }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } }
    });
};

const downloadExcel<%=rnd%> = () => {
    if(!currentData<%=rnd%> || currentData<%=rnd%>.length === 0) return;
    let csv = "\uFEFF";
    csv += currentConfig<%=rnd%>.columns.join(";") + "\n";
    currentData<%=rnd%>.forEach(row => {
        let rowArr = [];
        currentConfig<%=rnd%>.keys.forEach(k => {
            let val = row[k] !== null ? row[k].toString() : "-";
            rowArr.push('"' + val.replace(/"/g, '""') + '"');
        });
        csv += rowArr.join(";") + "\n";
    });
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement("a"); link.href = URL.createObjectURL(blob);
    link.download = "Statistik_Mata_Pelajaran_" + new Date().getTime() + ".csv";
    document.body.appendChild(link); link.click(); document.body.removeChild(link);
};

// ==========================================
// MODAL POPUP DETAIL MATAPELAJARAN
// ==========================================
let detailDataCache<%=rnd%> = [];
let detailCurrentPage<%=rnd%> = 1;

const safeSqlStr<%=rnd%> = (val) => {
    if(val === '-' || val === 'Tanpa Kelompok' || val === 'Belum Diatur') return " IS NULL ";
    return " = '" + val.toString().replace(/'/g, "''") + "' ";
};

const bukaDetailStatistik<%=rnd%> = async (dataIndex, event) => {
    const row = currentData<%=rnd%>[dataIndex];
    const tipe = document.getElementById("jenisLaporan<%=rnd%>").value;
    const btn = event.currentTarget;
    const oriContent = btn.innerHTML;
    btn.innerHTML = "<span class='spinner-border spinner-border-sm'></span>"; btn.disabled = true;

    const globalFilter = getFilterSql<%=rnd%>();
    
    let sqlJson = "SELECT m.kode, m.nama, s.nama as prodi, m.kkm as sks, km.nama as kelompok, jp.jenis as jenis_penilaian, m.terdapatnilaiketerampilan, m.terdapatnilaipredikat " + baseMpJoinSql + globalFilter;
    let sqlHtml = "SELECT m.* " + baseMpJoinSql + globalFilter;
    
    let filter = "";
    if(tipe == 1) filter += " AND s.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND km.nama " + safeSqlStr<%=rnd%>(row.label2);
    if(tipe == 2) filter += " AND s.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND jp.jenis " + safeSqlStr<%=rnd%>(row.label2);
    if(tipe == 3) filter += " AND CAST(m.kkm AS VARCHAR) = '" + row.label1 + "'";
    
    // Filter Keterampilan
    if(tipe == 4) {
        filter += " AND s.nama " + safeSqlStr<%=rnd%>(row.label1);
        if(row.label2 === 'Ada Keterampilan') filter += " AND m.terdapatnilaiketerampilan = true ";
        else filter += " AND (m.terdapatnilaiketerampilan = false OR m.terdapatnilaiketerampilan IS NULL) ";
    }
    // Filter Predikat
    if(tipe == 6) {
        filter += " AND s.nama " + safeSqlStr<%=rnd%>(row.label1);
        if(row.label2 === 'Ada Predikat') filter += " AND m.terdapatnilaipredikat = true ";
        else filter += " AND (m.terdapatnilaipredikat = false OR m.terdapatnilaipredikat IS NULL) ";
    }

    sqlJson += filter + " ORDER BY m.nama ASC";
    sqlHtml += filter + " ORDER BY m.nama ASC";

    window.currentDetailSqlHtml<%=rnd%> = sqlHtml;
    detailDataCache<%=rnd%> = await fetchDataAPI<%=rnd%>(sqlJson);
    detailCurrentPage<%=rnd%> = 1;
    
    btn.innerHTML = oriContent; btn.disabled = false;
    let subTitle = currentConfig<%=rnd%>.title + " (" + row.total + " Data)";
    tampilkanModalDetail<%=rnd%>(subTitle);
};

const tampilkanModalDetail<%=rnd%> = (title) => {
    const modalId = 'modalDetailStatistik<%=rnd%>';
    
    const existingModal = document.getElementById(modalId);
    if(existingModal) {
        try {
            const oldInstance = bootstrap.Modal.getInstance(existingModal);
            if (oldInstance) oldInstance.dispose();
        } catch(e) {}
        existingModal.remove();
    }
    
    // PENGGABUNGAN STRING STANDAR UNTUK MENGHINDARI BENTROK DENGAN JSP EL
    const modalHtml = 
        "<div class='modal fade' id='" + modalId + "' tabindex='-1' aria-hidden='true'>" +
            "<div class='modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable'>" +
                "<div class='modal-content shadow-lg border-0 rounded-4'>" +
                    "<div class='modal-header bg-light border-0 py-3'>" +
                        "<h5 class='modal-title fw-bold text-dark'>" +
                            "<i class='fas fa-list-alt text-primary me-2'></i>" + title +
                        "</h5>" +
                        "<div>" +
                            "<button class='btn btn-sm btn-success rounded-pill fw-bold shadow-sm me-2 px-3' onclick='downloadDetailExcel<%=rnd%>()'>" +
                                "<i class='fas fa-file-excel me-1'></i><%= Common.getBahasaConfig("Unduh Excel") %>" +
                            "</button>" +
                            "<button type='button' class='btn-close' data-bs-dismiss='modal' aria-label='Close'></button>" +
                        "</div>" +
                    "</div>" +
                    "<div class='modal-body p-0'>" +
                        "<div class='table-responsive'>" +
                            "<table class='table table-hover table-striped align-middle mb-0' style='font-size: 0.9rem;'>" +
                                "<thead class='bg-primary text-white'>" +
                                    "<tr>" +
                                        "<th class='text-center py-3' style='width: 50px;'>No</th>" +
                                        "<th class='py-3'>Kode MP</th>" +
                                        "<th class='py-3'>Nama Mata Pelajaran</th>" +
                                        "<th class='py-3'>Sekolah</th>" +
                                        "<th class='text-center py-3'>Nilai KKM</th>" +
                                        "<th class='py-3'>Kelompok</th>" +
                                        "<th class='py-3'>Jenis Penilaian</th>" +
                                        "<th class='text-center py-3'>Keterampilan</th>" +
                                        "<th class='text-center py-3'>Predikat</th>" +
                                    "</tr>" +
                                "</thead>" +
                                "<tbody id='tbodyDetail<%=rnd%>'></tbody>" +
                            "</table>" +
                        "</div>" +
                    "</div>" +
                    "<div class='modal-footer bg-white border-top p-3 d-flex justify-content-between align-items-center'>" +
                        "<span class='small text-muted fw-bold' id='infoPaginasiDetail<%=rnd%>'></span>" +
                        "<nav><ul class='pagination pagination-sm mb-0 shadow-sm' id='paginasiDetail<%=rnd%>'></ul></nav>" +
                    "</div>" +
                "</div>" +
            "</div>" +
        "</div>";
        
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    
    setTimeout(() => {
        const modalElement = document.getElementById(modalId);
        if (modalElement) {
            const modalEl = new bootstrap.Modal(modalElement, {
                backdrop: 'static',
                keyboard: true
            });
            modalEl.show();
            
            modalElement.addEventListener('hidden.bs.modal', function () { 
                this.remove(); 
            });
        }
    }, 10);

    renderTabelDetail<%=rnd%>();
};

const renderTabelDetail<%=rnd%> = async () => {
    const tbody = document.getElementById("tbodyDetail<%=rnd%>");
    
    if(detailDataCache<%=rnd%>.length === 0) {
        tbody.innerHTML = "<tr><td colspan='9' class='text-center text-muted p-4'><%= Common.getBahasaConfig("Tidak ada data rincian ditemukan") %></td></tr>";
        document.getElementById("infoPaginasiDetail<%=rnd%>").innerText = "<%= Common.getBahasaConfig("Menampilkan 0 data") %>";
        document.getElementById("paginasiDetail<%=rnd%>").innerHTML = ""; return;
    }

    tbody.innerHTML = "<tr><td colspan='9' class='text-center p-4'><span class='spinner-border text-primary'></span></td></tr>";
    const startIdx = (detailCurrentPage<%=rnd%> - 1) * 10;
    const endIdx = Math.min(startIdx + 10, detailDataCache<%=rnd%>.length);
    const formData = new URLSearchParams();
    formData.append("action_jsp", "get_html_detail");
    formData.append("sql", window.currentDetailSqlHtml<%=rnd%>);
    formData.append("start", startIdx); formData.append("limit", 10);
    
    try {
        const res = await fetch('<%=Common.ROOT%>/dsh?hanya_tampil_jsp=true&p=pagesmastersekolahmatapelajaranzul&s=_statistik_matapelajaran&baru=true&rnd=<%=rnd%>', {
            method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: formData.toString()
        });
        tbody.innerHTML = await res.text();
    } catch (e) {
        tbody.innerHTML = "<tr><td colspan='9' class='text-center text-danger p-3'><%= Common.getBahasaConfig("Gagal menarik data detail.") %></td></tr>";
    }

    document.getElementById("infoPaginasiDetail<%=rnd%>").innerText = "Menampilkan " + (startIdx + 1) + " - " + endIdx + " dari " + detailDataCache<%=rnd%>.length;
    const tPages = Math.ceil(detailDataCache<%=rnd%>.length / 10);
    let htmlPg = "<li class='page-item " + (detailCurrentPage<%=rnd%> === 1 ? "disabled" : "") + "'><button class='page-link' onclick='ubahHalamanDetail<%=rnd%>(" + (detailCurrentPage<%=rnd%> - 1) + ")'><i class='fas fa-angle-left'></i></button></li>";
    for(let i = 1; i <= tPages; i++) {
        if(i === 1 || i === tPages || (i >= detailCurrentPage<%=rnd%> - 1 && i <= detailCurrentPage<%=rnd%> + 1)) {
            htmlPg += "<li class='page-item " + (detailCurrentPage<%=rnd%> === i ? "active" : "") + "'><button class='page-link' onclick='ubahHalamanDetail<%=rnd%>(" + i + ")'>" + i + "</button></li>";
        } else if (i === detailCurrentPage<%=rnd%> - 2 || i === detailCurrentPage<%=rnd%> + 2) {
            htmlPg += "<li class='page-item disabled'><span class='page-link'>...</span></li>";
        }
    }
    htmlPg += "<li class='page-item " + (detailCurrentPage<%=rnd%> === tPages ? "disabled" : "") + "'><button class='page-link' onclick='ubahHalamanDetail<%=rnd%>(" + (detailCurrentPage<%=rnd%> + 1) + ")'><i class='fas fa-angle-right'></i></button></li>";
    document.getElementById("paginasiDetail<%=rnd%>").innerHTML = htmlPg;
};

const ubahHalamanDetail<%=rnd%> = (page) => { detailCurrentPage<%=rnd%> = page; renderTabelDetail<%=rnd%>(); };

const downloadDetailExcel<%=rnd%> = () => {
    if(!detailDataCache<%=rnd%> || detailDataCache<%=rnd%>.length === 0) return;
    let csv = "\uFEFFNo;Kode MP;Nama Mata Pelajaran;Sekolah;Nilai KKM;Kelompok;Jenis Penilaian;Keterampilan;Predikat\n";
    detailDataCache<%=rnd%>.forEach((row, i) => {
        let ket = row.terdapatnilaiketerampilan ? "Ada Keterampilan" : "Tidak Ada";
        let pred = row.terdapatnilaipredikat ? "Ada Predikat" : "Tidak Ada";
        
        let cols = [ i+1, row.kode || "-", row.nama || "-", row.prodi || "-", row.sks || "-", row.kelompok || "-", row.jenis_penilaian || "-", ket, pred ];
        csv += cols.map(v => '"' + String(v).replace(/"/g, '""') + '"').join(";") + "\n";
    });
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement("a"); link.href = URL.createObjectURL(blob);
    link.download = "Detail_Mata_Pelajaran_" + new Date().getTime() + ".csv";
    document.body.appendChild(link); link.click(); document.body.removeChild(link);
};

loadStatistik<%=rnd%>();
</script>