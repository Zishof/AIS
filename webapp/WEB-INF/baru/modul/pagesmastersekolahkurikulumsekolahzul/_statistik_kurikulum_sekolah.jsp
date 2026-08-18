<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.sekolah.Yayasan"%>
<%@page import="ais.database.model.sekolah.KurikulumSekolah"%>
<%@page import="ais.database.model.sekolah.JenisPenilaian"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.transform.Transformers"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Map"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // =========================================================================
    // BLOK TANGKAP VARIABEL RND UNTUK MENCEGAH BENTROK ID
    // =========================================================================
    String rnd = request.getParameter("rnd");
    if (rnd == null || rnd.trim().isEmpty()) {
        rnd = Common.getGeneratedBarCode(7);
    }
    // Prefix unik untuk modul Kurikulum Sekolah
    rnd = "Krk" + rnd;

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
            List<Map<String, Object>> list = sess.createSQLQuery(sql)
                                                 .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                                                 .setFirstResult(start)
                                                 .setMaxResults(limit)
                                                 .list();
                                           
            for (int i = 0; i < list.size(); i++) {
                Map<String, Object> row = list.get(i);
                
                String nama = row.get("nama_kurikulum") != null ? row.get("nama_kurikulum").toString() : "-";
                String sekolah = row.get("sekolah") != null ? row.get("sekolah").toString() : "-";
                String jenis = row.get("jenis_penilaian") != null ? row.get("jenis_penilaian").toString() : "-";
                String keterangan = row.get("keterangan") != null ? row.get("keterangan").toString() : "-";
                String totalMapel = row.get("total_mapel") != null ? row.get("total_mapel").toString() : "0";
                String totalJam = row.get("total_jam") != null ? row.get("total_jam").toString() : "0";
                
                Boolean isAktif = row.get("aktif") != null ? (Boolean) row.get("aktif") : true;
                String status = isAktif ? "<span class='badge bg-success'>Aktif</span>" : "<span class='badge bg-danger'>Tidak Aktif</span>";
                
                %>
                <tr>
                    <td class='text-center'><%= (start + i + 1) %></td>
                    <td class='fw-bold text-primary'><%= nama %></td>
                    <td><%= sekolah %></td>
                    <td><%= jenis %></td>
                    <td class='text-center fw-bold'><%= totalMapel %> MP</td>
                    <td class='text-center fw-bold'><%= totalJam %> Jam</td>
                    <td><%= keterangan %></td>
                    <td class='text-center'><%= status %></td>
                </tr>
                <%
            }
        } catch(Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pagesmastersekolahkurikulumsekolahzul/_statistik_kurikulum_sekolah.jsp:72");
            out.print("<tr><td colspan='8' class='text-center text-danger'>" + Common.getBahasaConfig("Terjadi kesalahan kueri data rincian.") + "</td></tr>");
        } finally {
            try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersekolahkurikulumsekolahzul/_statistik_kurikulum_sekolah.jsp:75");}
            try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersekolahkurikulumsekolahzul/_statistik_kurikulum_sekolah.jsp:76");}
            HibernateUtil.closeSession();
        }
        return; 
    }

    // =========================================================================
    // BLOK HALAMAN UTAMA STATISTIK KURIKULUM
    // =========================================================================
    List<Yayasan> listYayasan = new ArrayList<Yayasan>();
    List<Sekolah> listSekolah = new ArrayList<Sekolah>();
    List<JenisPenilaian> listJenisPenilaian = new ArrayList<JenisPenilaian>();
    
    Session sessFilter = HibernateUtil.openSession();
    try {
        listYayasan = ConstantValues.simpleList(sessFilter.createCriteria(Yayasan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Yayasan.class);
        listSekolah = ConstantValues.simpleList(sessFilter.createCriteria(Sekolah.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Sekolah.class);
        listJenisPenilaian = sessFilter.createQuery("from JenisPenilaian order by jenis").list();
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pagesmastersekolahkurikulumsekolahzul/_statistik_kurikulum_sekolah.jsp:95");
    } finally {
        try { sessFilter.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersekolahkurikulumsekolahzul/_statistik_kurikulum_sekolah.jsp:97");}
        try { sessFilter.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersekolahkurikulumsekolahzul/_statistik_kurikulum_sekolah.jsp:98");}
        HibernateUtil.closeSession();
    }
%>

<div class="container-fluid p-0 animate__animated animate__fadeIn">
    <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3">
        <h5 class="mb-0 fw-bold text-primary">
            <i class="fas fa-sitemap me-2"></i><%= Common.getBahasaConfig("Dashboard Statistik Kurikulum Sekolah") %>
        </h5>
    </div>

    <!-- Filter Section -->
    <div class="card shadow-sm border-0 rounded-4 mb-4">
        <div class="card-body bg-light rounded-4 p-4">
            <div class="row g-3">
                <div class="col-md-12 mb-2">
                    <label class="form-label fw-bold text-secondary small"><i class="fas fa-list me-1"></i><%= Common.getBahasaConfig("Pilih Jenis Laporan:") %></label>
                    <select class="form-select border-primary shadow-sm fw-bold" id="jenisLaporan<%=rnd%>" onchange="loadStatistik<%=rnd%>()">
                        <option value="1"><%= Common.getBahasaConfig("1. Sebaran Jumlah Kurikulum per Sekolah") %></option>
                        <option value="2"><%= Common.getBahasaConfig("2. Sebaran Kurikulum Berdasarkan Jenis Penilaian") %></option>
                        <option value="3"><%= Common.getBahasaConfig("3. Peringkat Kurikulum dengan Jumlah Mata Pelajaran Terbanyak") %></option>
                        <option value="4"><%= Common.getBahasaConfig("4. Peringkat Kurikulum dengan Total Beban Jam Pelajaran Tertinggi") %></option>
                    </select>
                </div>
                
                <div class="col-md-4">
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
                
                <div class="col-md-4">
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

                <div class="col-md-4">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-check-square me-1"></i> <%= Common.getBahasaConfig("Jenis Penilaian") %></label>
                    <select id="filterJenisPenilaianStat<%=rnd%>" class="form-select form-select-sm border-info shadow-none" onchange="loadStatistik<%=rnd%>()">
                        <option value=""><%= Common.getBahasaConfig("Semua Penilaian") %></option>
                        <% for(JenisPenilaian jp : listJenisPenilaian) { %><option value="<%= jp.getId() %>"><%= jp.getJenis() %></option><% } %>
                    </select>
                </div>
            </div>

            <!-- Action Buttons -->
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

    <!-- Chart Area -->
    <div class="card shadow-sm border-0 rounded-4 mb-4">
        <div class="card-body p-4">
            <div style="position: relative; height: 350px; width: 100%;">
                <canvas id="chartKurikulum<%=rnd%>"></canvas>
            </div>
        </div>
    </div>

    <!-- Table Area -->
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
const baseKsJoinSql = " FROM sekolah.kurikulum_sekolah ks " +
                      "LEFT JOIN sekolah.sekolah s ON ks.sekolah_id = s.id " +
                      "LEFT JOIN sekolah.yayasan y ON ks.yayasan_id = y.id " +
                      "LEFT JOIN sekolah.jenis_penilaian jp ON ks.jenis_penilaian_id = jp.id " +
                      "WHERE ks.id IS NOT NULL AND (ks.aktif = true OR ks.aktif IS NULL) ";

const reportConfigs<%=rnd%> = {
    "1": {
        title: "<%= Common.getBahasaConfig("Sebaran Jumlah Kurikulum per Sekolah") %>",
        columns: ["Sekolah", "Total Kurikulum"],
        keys: ["label1", "total"],
        sql: "SELECT s.nama as label1, COUNT(ks.id) as total " + baseKsJoinSql + " {FILTER} GROUP BY s.nama ORDER BY s.nama ASC"
    },
    "2": {
        title: "<%= Common.getBahasaConfig("Sebaran Kurikulum Berdasarkan Jenis Penilaian") %>",
        columns: ["Sekolah", "Jenis Penilaian", "Total Kurikulum"],
        keys: ["label1", "label2", "total"],
        sql: "SELECT s.nama as label1, COALESCE(jp.jenis, 'Belum Diatur') as label2, COUNT(ks.id) as total " + baseKsJoinSql + " {FILTER} GROUP BY s.nama, jp.jenis ORDER BY s.nama ASC, jp.jenis ASC"
    },
    "3": {
        title: "<%= Common.getBahasaConfig("Peringkat Kurikulum dengan Jumlah Mata Pelajaran Terbanyak") %>",
        columns: ["Nama Kurikulum", "Sekolah", "Total Mata Pelajaran (Berjalan)"],
        keys: ["label1", "label2", "total"],
        sql: "SELECT ks.nama as label1, s.nama as label2, " +
             "(SELECT COUNT(kpm.id) FROM sekolah.kurikulum_punya_matapelajaran kpm WHERE kpm.kurikulum_sekolah_id = ks.id AND (kpm.aktif = true OR kpm.aktif IS NULL)) as total " + 
             baseKsJoinSql + " {FILTER} ORDER BY total DESC, s.nama ASC"
    },
    "4": {
        title: "<%= Common.getBahasaConfig("Peringkat Kurikulum dengan Total Beban Jam Pelajaran Tertinggi") %>",
        columns: ["Nama Kurikulum", "Sekolah", "Total Jam Pelajaran"],
        keys: ["label1", "label2", "total"],
        sql: "SELECT ks.nama as label1, s.nama as label2, " +
             "(SELECT COALESCE(SUM(kpm.jumlahjampelajaran), 0) FROM sekolah.kurikulum_punya_matapelajaran kpm WHERE kpm.kurikulum_sekolah_id = ks.id AND (kpm.aktif = true OR kpm.aktif IS NULL)) as total " + 
             baseKsJoinSql + " {FILTER} ORDER BY total DESC, s.nama ASC"
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
    const jenisPnId = document.getElementById('filterJenisPenilaianStat<%=rnd%>').value;

    if (yayId) filterSql += " AND y.id = " + yayId + " ";
    if (sekId) filterSql += " AND s.id = " + sekId + " ";
    if (jenisPnId) filterSql += " AND ks.jenis_penilaian_id = " + jenisPnId + " ";

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
        data: { labels: labels, datasets: [{ label: '<%= Common.getBahasaConfigJS("Total") %>', data: totals, backgroundColor: 'rgba(23, 162, 184, 0.7)', borderColor: 'rgba(23, 162, 184, 1)', borderWidth: 1, borderRadius: 4 }] },
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
    link.download = "Statistik_Kurikulum_Sekolah_" + new Date().getTime() + ".csv";
    document.body.appendChild(link); link.click(); document.body.removeChild(link);
};

// ==========================================
// MODAL POPUP DETAIL KURIKULUM
// ==========================================
let detailDataCache<%=rnd%> = [];
let detailCurrentPage<%=rnd%> = 1;

const safeSqlStr<%=rnd%> = (val) => {
    if(val === '-' || val === 'Belum Diatur') return " IS NULL ";
    return " = '" + val.toString().replace(/'/g, "''") + "' ";
};

const bukaDetailStatistik<%=rnd%> = async (dataIndex, event) => {
    const row = currentData<%=rnd%>[dataIndex];
    const tipe = document.getElementById("jenisLaporan<%=rnd%>").value;
    const btn = event.currentTarget;
    const oriContent = btn.innerHTML;
    btn.innerHTML = "<span class='spinner-border spinner-border-sm'></span>"; btn.disabled = true;

    const globalFilter = getFilterSql<%=rnd%>();
    
    let sqlJson = "SELECT ks.nama as nama_kurikulum, s.nama as sekolah, jp.jenis as jenis_penilaian, ks.keterangan as keterangan, ks.aktif as aktif, " +
                  "(SELECT COUNT(kpm.id) FROM sekolah.kurikulum_punya_matapelajaran kpm WHERE kpm.kurikulum_sekolah_id = ks.id AND (kpm.aktif = true OR kpm.aktif IS NULL)) as total_mapel, " +
                  "(SELECT COALESCE(SUM(kpm.jumlahjampelajaran), 0) FROM sekolah.kurikulum_punya_matapelajaran kpm WHERE kpm.kurikulum_sekolah_id = ks.id AND (kpm.aktif = true OR kpm.aktif IS NULL)) as total_jam " +
                  baseKsJoinSql + globalFilter;
                  
    let sqlHtml = sqlJson; // Menggunakan query yang sama untuk rendering di Java via ALIAS_TO_ENTITY_MAP
    
    let filter = "";
    if(tipe == 1) filter += " AND s.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 2) filter += " AND s.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND jp.jenis " + safeSqlStr<%=rnd%>(row.label2);
    if(tipe == 3) filter += " AND ks.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND s.nama " + safeSqlStr<%=rnd%>(row.label2);
    if(tipe == 4) filter += " AND ks.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND s.nama " + safeSqlStr<%=rnd%>(row.label2);

    sqlJson += filter + " ORDER BY ks.nama ASC";
    sqlHtml += filter + " ORDER BY ks.nama ASC";

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
    
    const modalHtml = 
        "<div class='modal fade' id='" + modalId + "' tabindex='-1' aria-hidden='true'>" +
            "<div class='modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable'>" +
                "<div class='modal-content shadow-lg border-0 rounded-4'>" +
                    "<div class='modal-header bg-light border-0 py-3'>" +
                        "<h5 class='modal-title fw-bold text-dark'>" +
                            "<i class='fas fa-sitemap text-primary me-2'></i>" + title +
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
                                        "<th class='py-3'>Nama Kurikulum</th>" +
                                        "<th class='py-3'>Sekolah</th>" +
                                        "<th class='py-3'>Jenis Penilaian</th>" +
                                        "<th class='text-center py-3'>Total Mapel</th>" +
                                        "<th class='text-center py-3'>Total Jam</th>" +
                                        "<th class='py-3'>Keterangan</th>" +
                                        "<th class='text-center py-3'>Status</th>" +
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
        tbody.innerHTML = "<tr><td colspan='8' class='text-center text-muted p-4'><%= Common.getBahasaConfig("Tidak ada data rincian ditemukan") %></td></tr>";
        document.getElementById("infoPaginasiDetail<%=rnd%>").innerText = "<%= Common.getBahasaConfig("Menampilkan 0 data") %>";
        document.getElementById("paginasiDetail<%=rnd%>").innerHTML = ""; return;
    }

    tbody.innerHTML = "<tr><td colspan='8' class='text-center p-4'><span class='spinner-border text-primary'></span></td></tr>";
    const startIdx = (detailCurrentPage<%=rnd%> - 1) * 10;
    const endIdx = Math.min(startIdx + 10, detailDataCache<%=rnd%>.length);
    const formData = new URLSearchParams();
    formData.append("action_jsp", "get_html_detail");
    formData.append("sql", window.currentDetailSqlHtml<%=rnd%>);
    formData.append("start", startIdx); formData.append("limit", 10);
    
    try {
        const res = await fetch('<%=Common.ROOT%>/dsh?hanya_tampil_jsp=true&p=pagesmastersekolahkurikulumsekolahzul&s=_statistik_kurikulum_sekolah&baru=true&rnd=<%=rnd%>', {
            method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: formData.toString()
        });
        tbody.innerHTML = await res.text();
    } catch (e) {
        tbody.innerHTML = "<tr><td colspan='8' class='text-center text-danger p-3'><%= Common.getBahasaConfig("Gagal menarik data detail.") %></td></tr>";
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
    let csv = "\uFEFFNo;Nama Kurikulum;Sekolah;Jenis Penilaian;Total Mapel;Total Jam;Keterangan;Status\n";
    detailDataCache<%=rnd%>.forEach((row, i) => {
        let status = row.aktif ? "Aktif" : "Tidak Aktif";
        let cols = [ i+1, row.nama_kurikulum || "-", row.sekolah || "-", row.jenis_penilaian || "-", row.total_mapel || "0", row.total_jam || "0", row.keterangan || "-", status ];
        csv += cols.map(v => '"' + String(v).replace(/"/g, '""') + '"').join(";") + "\n";
    });
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement("a"); link.href = URL.createObjectURL(blob);
    link.download = "Detail_Kurikulum_Sekolah_" + new Date().getTime() + ".csv";
    document.body.appendChild(link); link.click(); document.body.removeChild(link);
};

loadStatistik<%=rnd%>();
</script>