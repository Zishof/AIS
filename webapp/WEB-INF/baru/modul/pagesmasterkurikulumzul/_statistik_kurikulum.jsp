<%@page import="ais.database.model.Kurikulum"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Jurusan"%>
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
    // Prefix unik untuk modul Kurikulum
    rnd = "Kur" + rnd; 

    // =========================================================================
    // BLOK SERVER-SIDE KHUSUS UNTUK MERENDER TABEL DETAIL POPUP
    // =========================================================================
    String actionParams = request.getParameter("action_jsp");
    if ("get_html_detail".equals(actionParams)) {
        String sql = request.getParameter("sql");
        String tipeDetail = request.getParameter("tipe_detail");
        int start = Integer.parseInt(request.getParameter("start") != null ? request.getParameter("start") : "0");
        int limit = Integer.parseInt(request.getParameter("limit") != null ? request.getParameter("limit") : "10");
        
        Session sess = HibernateUtil.openSession();
        try {
            if ("kpm".equals(tipeDetail)) {
                // Rincian untuk Laporan KurikulumPunyaMatakuliah (Tipe 5-7)
                List<Object[]> list = sess.createSQLQuery(sql)
                                          .setFirstResult(start)
                                          .setMaxResults(limit)
                                          .list();
                for (int i = 0; i < list.size(); i++) {
                    Object[] row = list.get(i);
                    String prodi = row[0] != null ? row[0].toString() : "-";
                    String kurikulum = row[1] != null ? row[1].toString() : "-";
                    String semester = row[2] != null ? row[2].toString() : "-";
                    String kodeMk = row[3] != null ? row[3].toString() : "-";
                    String namaMk = row[4] != null ? row[4].toString() : "-";
                    String sks = row[5] != null ? row[5].toString() : "0";
                    String wajib = row[6] != null && (Boolean)row[6] ? "<span class='badge bg-success'>Wajib</span>" : "<span class='badge bg-secondary'>Pilihan</span>";
                    
                    %>
                    <tr>
                        <td class='text-center'><%= (start + i + 1) %></td>
                        <td class='fw-bold text-primary'><%= kurikulum %></td>
                        <td><%= prodi %></td>
                        <td class='text-center fw-bold'>Smt <%= semester %></td>
                        <td><%= kodeMk %></td>
                        <td class='fw-bold'><%= namaMk %></td>
                        <td class='text-center'><%= sks %></td>
                        <td class='text-center'><%= wajib %></td>
                    </tr>
                    <%
                }
            } else {
                // Rincian untuk Laporan Kurikulum Utama (Tipe 1-4)
                List<Kurikulum> list = sess.createSQLQuery(sql)
                                           .addEntity(Kurikulum.class)
                                           .setFirstResult(start)
                                           .setMaxResults(limit)
                                           .list();
                for (int i = 0; i < list.size(); i++) {
                    Kurikulum k = list.get(i);
                    String namaKurikulum = k.getNama() != null ? k.getNama() : "-";
                    String prodi = k.getJurusan() != null ? k.getJurusan().getNama() : "-";
                    String tahun = k.getTahun() != null ? k.getTahun().toString() : "-";
                    String ta = k.getTahunAkademik() != null ? k.getTahunAkademik() : "-";
                    String smt = k.getJenisSemester() != null ? k.getJenisSemester() : "-";
                    String obe = k.getObe() != null && k.getObe() ? "<span class='badge bg-primary'>OBE</span>" : "<span class='badge bg-light text-dark'>Non-OBE</span>";
                    
                    %>
                    <tr>
                        <td class='text-center'><%= (start + i + 1) %></td>
                        <td class='fw-bold text-primary'><%= namaKurikulum %></td>
                        <td><%= prodi %></td>
                        <td class='text-center fw-bold'><%= tahun %></td>
                        <td class='text-center'><%= ta %></td>
                        <td class='text-center'><%= smt %></td>
                        <td class='text-center'><%= obe %></td>
                    </tr>
                    <%
                }
            }
        } catch(Exception e) {
            out.print("<tr><td colspan='8' class='text-center text-danger'>" + Common.getBahasaConfig("Terjadi kesalahan kueri data rincian kurikulum.") + "</td></tr>");
        } finally {
            try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmasterkurikulumzul/_statistik_kurikulum.jsp:98");}
            try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmasterkurikulumzul/_statistik_kurikulum.jsp:99");}
            HibernateUtil.closeSession();
        }
        return; 
    }

    // =========================================================================
    // BLOK HALAMAN UTAMA STATISTIK KURIKULUM
    // =========================================================================
    List<Fakultas> listFakultas = new ArrayList<Fakultas>();
    List<Jurusan> listJurusan = new ArrayList<Jurusan>();
    
    Session sessFilter = HibernateUtil.openSession();
    try {
        listFakultas = ConstantValues.simpleList(sessFilter.createCriteria(Fakultas.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Fakultas.class);
        listJurusan = ConstantValues.simpleList(sessFilter.createCriteria(Jurusan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Jurusan.class);
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pagesmasterkurikulumzul/_statistik_kurikulum.jsp:116");
    } finally {
        try { sessFilter.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmasterkurikulumzul/_statistik_kurikulum.jsp:118");}
        try { sessFilter.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmasterkurikulumzul/_statistik_kurikulum.jsp:119");}
        HibernateUtil.closeSession();
    }
%>

<div class="container-fluid p-0 animate__animated animate__fadeIn">
    <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3">
        <h5 class="mb-0 fw-bold text-primary">
            <i class="fas fa-sitemap me-2"></i><%= Common.getBahasaConfig("Dashboard Statistik Kurikulum") %>
        </h5>
    </div>

    <div class="card shadow-sm border-0 rounded-4 mb-4">
        <div class="card-body bg-light rounded-4 p-4">
            <div class="row g-3">
                <div class="col-md-12 mb-2">
                    <label class="form-label fw-bold text-secondary small"><i class="fas fa-list me-1"></i><%= Common.getBahasaConfig("Pilih Jenis Laporan:") %></label>
                    <select class="form-select border-primary shadow-sm fw-bold" id="jenisLaporan<%=rnd%>" onchange="loadStatistik<%=rnd%>()">
                        <optgroup label="<%= Common.getBahasaConfig("Data Dasar Kurikulum") %>">
                            <option value="1"><%= Common.getBahasaConfig("1. Sebaran Kurikulum per Fakultas & Program Studi") %></option>
                            <option value="2"><%= Common.getBahasaConfig("2. Sebaran Kurikulum per Tahun") %></option>
                            <option value="3"><%= Common.getBahasaConfig("3. Sebaran Kurikulum per Tahun Akademik & Semester") %></option>
                            <option value="4"><%= Common.getBahasaConfig("4. Sebaran Kurikulum Berbasis OBE (Outcome-Based Education)") %></option>
                        </optgroup>
                        <optgroup label="<%= Common.getBahasaConfig("Distribusi Mata Kuliah Kurikulum") %>">
                            <option value="5"><%= Common.getBahasaConfig("5. Distribusi Total Mata Kuliah per Kurikulum") %></option>
                            <option value="6"><%= Common.getBahasaConfig("6. Distribusi Sebaran Semester per Kurikulum") %></option>
                            <option value="7"><%= Common.getBahasaConfig("7. Distribusi Sifat Mata Kuliah (Wajib/Pilihan) per Kurikulum") %></option>
                        </optgroup>
                    </select>
                </div>
                
                <div class="col-md-4">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-university me-1"></i> <%= Common.getBahasaConfig("Fakultas") %></label>
                    <select id="filterFakultasStat<%=rnd%>" class="form-select form-select-sm border-info shadow-none" onchange="
                        var fakId = this.value;
                        var prodiSelect = document.getElementById('filterProdiStat<%=rnd%>');
                        prodiSelect.value = '';
                        for(var i=1; i<prodiSelect.options.length; i++){
                            var opt = prodiSelect.options[i];
                            if(fakId === '' || opt.getAttribute('data-fakultas') === fakId){
                                opt.style.display = ''; opt.disabled = false;
                            } else {
                                opt.style.display = 'none'; opt.disabled = true;
                            }
                        }
                        loadStatistik<%=rnd%>();
                    ">
                        <option value=""><%= Common.getBahasaConfig("Semua Fakultas") %></option>
                        <% for(Fakultas f : listFakultas) { %><option value="<%= f.getId() %>"><%= f.getNama() %></option><% } %>
                    </select>
                </div>
                
                <div class="col-md-4">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-graduation-cap me-1"></i> <%= Common.getBahasaConfig("Program Studi") %></label>
                    <select id="filterProdiStat<%=rnd%>" class="form-select form-select-sm border-info shadow-none" onchange="loadStatistik<%=rnd%>()">
                        <option value="" data-fakultas=""><%= Common.getBahasaConfig("Semua Program Studi") %></option>
                        <% for(Jurusan j : listJurusan) { 
                             String fakIdStr = (j.getFakultas() != null) ? j.getFakultas().getId().toString() : "";
                        %>
                            <option value="<%= j.getId() %>" data-fakultas="<%= fakIdStr %>"><%= j.getNama() %></option>
                        <% } %>
                    </select>
                </div>

                <div class="col-md-4">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-check-circle me-1"></i> <%= Common.getBahasaConfig("Status OBE") %></label>
                    <select id="filterObeStat<%=rnd%>" class="form-select form-select-sm border-info shadow-none" onchange="loadStatistik<%=rnd%>()">
                        <option value=""><%= Common.getBahasaConfig("Semua Status") %></option>
                        <option value="true"><%= Common.getBahasaConfig("Hanya Berbasis OBE") %></option>
                        <option value="false"><%= Common.getBahasaConfig("Non-OBE") %></option>
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
            <div style="position: relative; height: 350px; width: 100%;"><canvas id="chartKurikulum<%=rnd%>"></canvas></div>
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
// KONFIGURASI SQL REKAPITULASI KURIKULUM
// ==========================================
// Jika tabel di database menggunakan penamaan lain, sesuaikan kolom (contoh: k.tahunakademik vs k.tahun_akademik)
const baseKurikulumJoinSql = " FROM kurikulum k LEFT JOIN jurusan j ON k.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE (k.aktif = true OR k.aktif IS NULL) ";
const baseKPMJoinSql = " FROM kurikulum_punya_matakuliah kpm JOIN kurikulum k ON kpm.kurikulum = k.id JOIN matakuliah m ON kpm.matakuliah = m.id LEFT JOIN jurusan j ON k.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE (k.aktif = true OR k.aktif IS NULL) AND (kpm.aktif = true OR kpm.aktif IS NULL) AND (m.aktif = true OR m.aktif IS NULL) ";

const reportConfigs<%=rnd%> = {
    "1": {
        title: "<%= Common.getBahasaConfig("Sebaran Kurikulum per Fakultas & Program Studi") %>",
        columns: ["Fakultas", "Program Studi", "Total Kurikulum"], keys: ["label1", "label2", "total"],
        sql: "SELECT COALESCE(f.nama, 'Belum Diatur') as label1, j.nama as label2, COUNT(k.id) as total " + baseKurikulumJoinSql + " {FILTER} GROUP BY f.nama, j.nama ORDER BY f.nama ASC, j.nama ASC"
    },
    "2": {
        title: "<%= Common.getBahasaConfig("Sebaran Kurikulum per Tahun") %>",
        columns: ["Tahun Kurikulum", "Total Kurikulum"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(CAST(k.tahun AS VARCHAR), '-') as label1, COUNT(k.id) as total " + baseKurikulumJoinSql + " {FILTER} GROUP BY k.tahun ORDER BY k.tahun DESC"
    },
    "3": {
        title: "<%= Common.getBahasaConfig("Sebaran Kurikulum per Tahun Akademik & Semester") %>",
        columns: ["Tahun Akademik", "Semester", "Total Kurikulum"], keys: ["label1", "label2", "total"],
        sql: "SELECT COALESCE(k.tahunakademik, '-') as label1, COALESCE(k.jenissemester, '-') as label2, COUNT(k.id) as total " + baseKurikulumJoinSql + " {FILTER} GROUP BY k.tahunakademik, k.jenissemester ORDER BY k.tahunakademik DESC"
    },
    "4": {
        title: "<%= Common.getBahasaConfig("Sebaran Kurikulum Berbasis OBE") %>",
        columns: ["Status OBE", "Total Kurikulum"], keys: ["label1", "total"],
        sql: "SELECT CASE WHEN k.obe = true THEN 'Berbasis OBE' ELSE 'Non-OBE' END as label1, COUNT(k.id) as total " + baseKurikulumJoinSql + " {FILTER} GROUP BY k.obe"
    },
    "5": {
        title: "<%= Common.getBahasaConfig("Distribusi Total Mata Kuliah per Kurikulum") %>",
        columns: ["Nama Kurikulum", "Program Studi", "Total Mata Kuliah"], keys: ["label1", "label2", "total"],
        sql: "SELECT k.nama as label1, j.nama as label2, COUNT(kpm.id) as total " + baseKPMJoinSql + " {FILTER} GROUP BY k.nama, j.nama ORDER BY k.nama ASC"
    },
    "6": {
        title: "<%= Common.getBahasaConfig("Distribusi Sebaran Semester per Kurikulum") %>",
        columns: ["Nama Kurikulum", "Semester", "Total MK di Semester"], keys: ["label1", "label2", "total"],
        sql: "SELECT k.nama as label1, 'Semester ' || CAST(kpm.semester AS VARCHAR) as label2, COUNT(kpm.id) as total " + baseKPMJoinSql + " {FILTER} GROUP BY k.nama, kpm.semester ORDER BY k.nama ASC, kpm.semester ASC"
    },
    "7": {
        title: "<%= Common.getBahasaConfig("Distribusi Sifat Mata Kuliah (Wajib/Pilihan) per Kurikulum") %>",
        columns: ["Nama Kurikulum", "Sifat Mata Kuliah", "Total MK"], keys: ["label1", "label2", "total"],
        sql: "SELECT k.nama as label1, CASE WHEN m.status = 'Wajib' THEN 'Wajib' ELSE 'Pilihan' END as label2, COUNT(kpm.id) as total " + baseKPMJoinSql + " {FILTER} GROUP BY k.nama, m.status ORDER BY k.nama ASC"
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
    const fakId = document.getElementById('filterFakultasStat<%=rnd%>').value;
    const prodiId = document.getElementById('filterProdiStat<%=rnd%>').value;
    const obe = document.getElementById('filterObeStat<%=rnd%>').value;

    if (fakId) filterSql += " AND f.id = " + fakId + " ";
    if (prodiId) filterSql += " AND j.id = " + prodiId + " ";
    if (obe !== "") filterSql += " AND k.obe = " + obe + " ";

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
    const ctx = document.getElementById('chartKurikulum<%=rnd%>').getContext('2d');
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
        data: { labels: labels, datasets: [{ label: '<%= Common.getBahasaConfigJS("Total Data") %>', data: totals, backgroundColor: 'rgba(111, 66, 193, 0.7)', borderColor: 'rgba(111, 66, 193, 1)', borderWidth: 1, borderRadius: 4 }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } }
    });
};

const downloadExcel<%=rnd%> = () => {
    if(!currentData<%=rnd%> || currentData<%=rnd%>.length === 0) return;
    let csv = "\uFEFF"; csv += currentConfig<%=rnd%>.columns.join(";") + "\n";
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
    link.download = "Statistik_Kurikulum_" + new Date().getTime() + ".csv";
    document.body.appendChild(link); link.click(); document.body.removeChild(link);
};

// ==========================================
// MODAL POPUP DETAIL KURIKULUM & MATAKULIAH
// ==========================================
let detailDataCache<%=rnd%> = [];
let detailCurrentPage<%=rnd%> = 1;
let globalTipeDetail<%=rnd%> = "kurikulum";

const safeSqlStr<%=rnd%> = (val) => {
    if(val === '-' || val === 'Belum Diatur') return " IS NULL ";
    return " = '" + val.toString().replace(/'/g, "''") + "' ";
};

const bukaDetailStatistik<%=rnd%> = async (dataIndex, event) => {
    const row = currentData<%=rnd%>[dataIndex];
    const tipe = parseInt(document.getElementById("jenisLaporan<%=rnd%>").value);
    const btn = event.currentTarget;
    const oriContent = btn.innerHTML;
    btn.innerHTML = "<span class='spinner-border spinner-border-sm'></span>"; btn.disabled = true;

    const globalFilter = getFilterSql<%=rnd%>();
    globalTipeDetail<%=rnd%> = tipe <= 4 ? "kurikulum" : "kpm";
    
    let sqlJson = "";
    let sqlHtml = "";

    if (globalTipeDetail<%=rnd%> === "kurikulum") {
        sqlJson = "SELECT k.nama as nama_kurik, j.nama as prodi, k.tahun, k.tahunakademik, k.jenissemester, k.obe " + baseKurikulumJoinSql + globalFilter;
        sqlHtml = "SELECT k.* " + baseKurikulumJoinSql + globalFilter;
        
        if(tipe == 1) sqlHtml += " AND f.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND j.nama " + safeSqlStr<%=rnd%>(row.label2);
        if(tipe == 2) sqlHtml += " AND CAST(k.tahun AS VARCHAR) " + safeSqlStr<%=rnd%>(row.label1);
        if(tipe == 3) sqlHtml += " AND k.tahunakademik " + safeSqlStr<%=rnd%>(row.label1) + " AND k.jenissemester " + safeSqlStr<%=rnd%>(row.label2);
        if(tipe == 4) sqlHtml += (row.label1 === 'Berbasis OBE' ? " AND k.obe = true " : " AND (k.obe = false OR k.obe IS NULL) ");
        
        sqlJson = sqlHtml.replace("SELECT k.*", "SELECT k.nama as nama_kurik, j.nama as prodi, k.tahun, k.tahunakademik, k.jenissemester, k.obe") + " ORDER BY k.nama ASC";
        sqlHtml += " ORDER BY k.nama ASC";
        
    } else {
        // Ubah kpm.wajib menjadi m.status as status_mk
        sqlJson = "SELECT j.nama as prodi, k.nama as kurikulum, kpm.semester, m.kode, m.nama as nama_mk, m.sks, m.status as status_mk " + baseKPMJoinSql + globalFilter;
        sqlHtml = "SELECT j.nama as prodi, k.nama as kurikulum, kpm.semester, m.kode, m.nama as nama_mk, m.sks, m.status as status_mk " + baseKPMJoinSql + globalFilter;
        
        if(tipe == 5) sqlHtml += " AND k.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND j.nama " + safeSqlStr<%=rnd%>(row.label2);
        if(tipe == 6) sqlHtml += " AND k.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND ('Semester ' || CAST(kpm.semester AS VARCHAR)) " + safeSqlStr<%=rnd%>(row.label2);
        
        // Sesuaikan filter Wajib/Pilihan menggunakan m.status
        if(tipe == 7) sqlHtml += " AND k.nama " + safeSqlStr<%=rnd%>(row.label1) + (row.label2 === 'Wajib' ? " AND m.status = 'Wajib' " : " AND (m.status != 'Wajib' OR m.status IS NULL) ");
        
        sqlJson = sqlHtml + " ORDER BY k.nama ASC, kpm.semester ASC, m.nama ASC";
        sqlHtml += " ORDER BY k.nama ASC, kpm.semester ASC, m.nama ASC";
    }

    window.currentDetailSqlHtml<%=rnd%> = sqlHtml;
    detailDataCache<%=rnd%> = await fetchDataAPI<%=rnd%>(sqlJson);
    detailCurrentPage<%=rnd%> = 1;
    
    btn.innerHTML = oriContent; btn.disabled = false;
    let subTitle = currentConfig<%=rnd%>.title + " (" + row.total + " Data)";
    tampilkanModalDetail<%=rnd%>(subTitle);
};

const tampilkanModalDetail<%=rnd%> = (title) => {
    const modalId = 'modalDetailStatistik<%=rnd%>';
    if(document.getElementById(modalId)) document.getElementById(modalId).remove();
    
    let theadHtml = "";
    if(globalTipeDetail<%=rnd%> === "kurikulum") {
        theadHtml = "<tr><th class='text-center py-3' style='width: 50px;'>No</th><th class='py-3'>Nama Kurikulum</th><th class='py-3'>Program Studi</th><th class='text-center py-3'>Tahun</th><th class='text-center py-3'>Thn Akademik</th><th class='text-center py-3'>Semester</th><th class='text-center py-3'>OBE</th></tr>";
    } else {
        theadHtml = "<tr><th class='text-center py-3' style='width: 50px;'>No</th><th class='py-3'>Kurikulum</th><th class='py-3'>Program Studi</th><th class='text-center py-3'>Semester</th><th class='py-3'>Kode MK</th><th class='py-3'>Nama Mata Kuliah</th><th class='text-center py-3'>SKS</th><th class='text-center py-3'>Sifat</th></tr>";
    }

    const modalHtml = 
        "<div class='modal fade' id='" + modalId + "' tabindex='-1' aria-hidden='true' data-bs-backdrop='static'>" +
            "<div class='modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable'>" +
                "<div class='modal-content shadow-lg border-0 rounded-4'>" +
                    "<div class='modal-header bg-light border-0 py-3'>" +
                        "<h5 class='modal-title fw-bold text-dark'><i class='fas fa-list-alt text-primary me-2'></i>" + title + "</h5>" +
                        "<div>" +
                            "<button class='btn btn-sm btn-success rounded-pill fw-bold shadow-sm me-2 px-3' onclick='downloadDetailExcel<%=rnd%>()'><i class='fas fa-file-excel me-1'></i><%= Common.getBahasaConfig("Unduh Excel") %></button>" +
                            "<button type='button' class='btn-close' data-bs-dismiss='modal' aria-label='Close'></button>" +
                        "</div>" +
                    "</div>" +
                    "<div class='modal-body p-0'>" +
                        "<div class='table-responsive'>" +
                            "<table class='table table-hover table-striped align-middle mb-0' style='font-size: 0.9rem;'>" +
                                "<thead class='bg-primary text-white'>" + theadHtml + "</thead>" +
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
    const modalEl = new bootstrap.Modal(document.getElementById(modalId));
    modalEl.show();
    document.getElementById(modalId).addEventListener('hidden.bs.modal', function () { this.remove(); });
    renderTabelDetail<%=rnd%>();
};

const renderTabelDetail<%=rnd%> = async () => {
    const tbody = document.getElementById("tbodyDetail<%=rnd%>");
    let colSpan = globalTipeDetail<%=rnd%> === "kurikulum" ? 7 : 8;
    
    if(detailDataCache<%=rnd%>.length === 0) {
        tbody.innerHTML = "<tr><td colspan='" + colSpan + "' class='text-center text-muted p-4'><%= Common.getBahasaConfig("Tidak ada data rincian ditemukan") %></td></tr>";
        document.getElementById("infoPaginasiDetail<%=rnd%>").innerText = "<%= Common.getBahasaConfig("Menampilkan 0 data") %>";
        document.getElementById("paginasiDetail<%=rnd%>").innerHTML = ""; return;
    }

    tbody.innerHTML = "<tr><td colspan='" + colSpan + "' class='text-center p-4'><span class='spinner-border text-primary'></span></td></tr>";
    const startIdx = (detailCurrentPage<%=rnd%> - 1) * 10;
    const endIdx = Math.min(startIdx + 10, detailDataCache<%=rnd%>.length);
    const formData = new URLSearchParams();
    formData.append("action_jsp", "get_html_detail");
    formData.append("sql", window.currentDetailSqlHtml<%=rnd%>);
    formData.append("tipe_detail", globalTipeDetail<%=rnd%>);
    formData.append("start", startIdx); formData.append("limit", 10);

    try {
        const res = await fetch('<%=Common.ROOT%>/dsh?hanya_tampil_jsp=true&p=pagesmasterkurikulumzul&s=_statistik_kurikulum&baru=true&rnd=<%=rnd%>', {
            method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: formData.toString()
        });
        tbody.innerHTML = await res.text();
    } catch (e) {
        tbody.innerHTML = "<tr><td colspan='" + colSpan + "' class='text-center text-danger p-3'><%= Common.getBahasaConfig("Gagal menarik data detail.") %></td></tr>";
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
    let csv = "\uFEFF";
    
    if(globalTipeDetail<%=rnd%> === "kurikulum") {
        csv += "No;Nama Kurikulum;Program Studi;Tahun;Thn Akademik;Semester;OBE\n";
        detailDataCache<%=rnd%>.forEach((row, i) => {
            let cols = [ i+1, row.nama_kurik || "-", row.prodi || "-", row.tahun || "-", row.tahunakademik || "-", row.jenissemester || "-", row.obe === true ? "OBE" : "Non-OBE" ];
            csv += cols.map(v => '"' + String(v).replace(/"/g, '""') + '"').join(";") + "\n";
        });
    } else {
        csv += "No;Kurikulum;Program Studi;Semester;Kode MK;Nama Mata Kuliah;SKS;Sifat\n";
        detailDataCache<%=rnd%>.forEach((row, i) => {
            let cols = [ i+1, row.kurikulum || "-", row.prodi || "-", row.semester || "-", row.kode || "-", row.nama_mk || "-", row.sks || "0", row.wajib === true ? "Wajib" : "Pilihan" ];
            csv += cols.map(v => '"' + String(v).replace(/"/g, '""') + '"').join(";") + "\n";
        });
    }
    
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement("a"); link.href = URL.createObjectURL(blob);
    link.download = "Detail_Data_Kurikulum_" + new Date().getTime() + ".csv";
    document.body.appendChild(link); link.click(); document.body.removeChild(link);
};

loadStatistik<%=rnd%>();
</script>