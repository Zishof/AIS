<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.KelompokMatakuliah"%>
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
    // Prefix unik untuk modul Matakuliah
    rnd = "Mtk" + rnd; 

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
            List<Matakuliah> list = sess.createSQLQuery(sql)
                                       .addEntity(Matakuliah.class)
                                       .setFirstResult(start)
                                       .setMaxResults(limit)
                                       .list();
            for (int i = 0; i < list.size(); i++) {
                Matakuliah m = list.get(i);

                String kode = m.getKode() != null ? m.getKode() : "-";
                String nama = m.getNama() != null ? m.getNama() : "-";
                String prodi = m.getJurusan() != null ? m.getJurusan().getNama() : "-";
                String kelompok = m.getKelompokMatakuliah() != null ? m.getKelompokMatakuliah().getNama() : "-";
                String jenis = m.getJenisMatakuliah() != null ? m.getJenisMatakuliah() : "-";
                String sksTotal = m.getSks() != null ? m.getSks().toString() : "0";
                
                // Pengecekan Kelengkapan Dokumen
                String statusDokumen = "";
                if(m.getAdaSap() != null && m.getAdaSap()) statusDokumen += "<span class='badge bg-success me-1'>SAP</span>";
                if(m.getAdaSilabus() != null && m.getAdaSilabus()) statusDokumen += "<span class='badge bg-info me-1'>Silabus</span>";
                if(m.getAdaBahanAjar() != null && m.getAdaBahanAjar()) statusDokumen += "<span class='badge bg-primary me-1'>Bhn Ajar</span>";
                if(statusDokumen.isEmpty()) statusDokumen = "<span class='text-muted fst-italic'>Belum Ada</span>";
                
                %>
                <tr>
                    <td class='text-center'><%= (start + i + 1) %></td>
                    <td class='fw-bold text-primary'><%= kode %></td>
                    <td class='fw-bold'><%= nama %></td>
                    <td><%= prodi %></td>
                    <td class='text-center fw-bold'><%= sksTotal %> SKS</td>
                    <td><%= kelompok %></td>
                    <td><%= jenis %></td>
                    <td><%= statusDokumen %></td>
                </tr>
                <%
            }
        } catch(Exception e) {
            out.print("<tr><td colspan='8' class='text-center text-danger'>" + Common.getBahasaConfig("Terjadi kesalahan kueri data.") + "</td></tr>");
        } finally {
            try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastermatakuliahzul/_statistik_matakuliah.jsp:74");}
            try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastermatakuliahzul/_statistik_matakuliah.jsp:75");}
            HibernateUtil.closeSession();
        }
        return; 
    }

    // =========================================================================
    // BLOK HALAMAN UTAMA STATISTIK MATAKULIAH
    // =========================================================================
    List<Fakultas> listFakultas = new ArrayList<Fakultas>();
    List<Jurusan> listJurusan = new ArrayList<Jurusan>();
    List<KelompokMatakuliah> listKelompok = new ArrayList<KelompokMatakuliah>();
    List<String> listJenis = new ArrayList<String>();
    
    Session sessFilter = HibernateUtil.openSession();
    try {
        listFakultas = ConstantValues.simpleList(sessFilter.createCriteria(Fakultas.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Fakultas.class);
        listJurusan = ConstantValues.simpleList(sessFilter.createCriteria(Jurusan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Jurusan.class);
        listKelompok = ConstantValues.simpleList(sessFilter.createCriteria(KelompokMatakuliah.class).addOrder(Order.asc("nama")), KelompokMatakuliah.class);
        
        // Mengambil daftar Jenis Matakuliah yang unik
        listJenis = sessFilter.createQuery("select distinct m.jenisMatakuliah from Matakuliah m where m.jenisMatakuliah is not null order by m.jenisMatakuliah").list();
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pagesmastermatakuliahzul/_statistik_matakuliah.jsp:98");
    } finally {
        try { sessFilter.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastermatakuliahzul/_statistik_matakuliah.jsp:100");}
        try { sessFilter.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastermatakuliahzul/_statistik_matakuliah.jsp:101");}
        HibernateUtil.closeSession();
    }
%>

<div class="container-fluid p-0 animate__animated animate__fadeIn">
    <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3">
        <h5 class="mb-0 fw-bold text-primary">
            <i class="fas fa-book-open me-2"></i><%= Common.getBahasaConfig("Dashboard Statistik Mata Kuliah") %>
        </h5>
    </div>

    <div class="card shadow-sm border-0 rounded-4 mb-4">
        <div class="card-body bg-light rounded-4 p-4">
            <div class="row g-3">
                <div class="col-md-12 mb-2">
                    <label class="form-label fw-bold text-secondary small"><i class="fas fa-list me-1"></i><%= Common.getBahasaConfig("Pilih Jenis Laporan:") %></label>
                    <select class="form-select border-primary shadow-sm fw-bold" id="jenisLaporan<%=rnd%>" onchange="loadStatistik<%=rnd%>()">
                        <option value="1"><%= Common.getBahasaConfig("1. Sebaran per Program Studi & Kelompok Mata Kuliah") %></option>
                        <option value="2"><%= Common.getBahasaConfig("2. Sebaran per Program Studi & Jenis Mata Kuliah") %></option>
                        <option value="3"><%= Common.getBahasaConfig("3. Sebaran Mata Kuliah Berdasarkan Jumlah SKS") %></option>
                        <option value="4"><%= Common.getBahasaConfig("4. Sebaran Komponen (Teori, Praktik, Simulasi, Lapangan) per Prodi") %></option>
                        <option value="5"><%= Common.getBahasaConfig("5. Sebaran Kelengkapan Dokumen (SAP, Silabus, dll) per Prodi") %></option>
                        <option value="6"><%= Common.getBahasaConfig("6. Sebaran Mata Kuliah Ekstrakurikuler per Prodi") %></option>
                    </select>
                </div>
                
                <div class="col-md-3">
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
                
                <div class="col-md-3">
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

                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-layer-group me-1"></i> <%= Common.getBahasaConfig("Kelompok") %></label>
                    <select id="filterKelompokStat<%=rnd%>" class="form-select form-select-sm border-info shadow-none" onchange="loadStatistik<%=rnd%>()">
                        <option value=""><%= Common.getBahasaConfig("Semua Kelompok") %></option>
                        <% for(KelompokMatakuliah km : listKelompok) { %><option value="<%= km.getId() %>"><%= km.getNama() %></option><% } %>
                    </select>
                </div>

                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-tags me-1"></i> <%= Common.getBahasaConfig("Jenis") %></label>
                    <select id="filterJenisStat<%=rnd%>" class="form-select form-select-sm border-info shadow-none" onchange="loadStatistik<%=rnd%>()">
                        <option value=""><%= Common.getBahasaConfig("Semua Jenis") %></option>
                        <% for(String jenis : listJenis) { %><option value="<%= jenis %>"><%= jenis %></option><% } %>
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
            <div style="position: relative; height: 350px; width: 100%;"><canvas id="chartMatakuliah<%=rnd%>"></canvas></div>
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
// KONFIGURASI SQL REKAPITULASI MATAKULIAH
// ==========================================
const reportConfigs<%=rnd%> = {
    "1": {
        title: "<%= Common.getBahasaConfig("Sebaran per Program Studi & Kelompok Mata Kuliah") %>",
        columns: ["Program Studi", "Kelompok Mata Kuliah", "Total Mata Kuliah"],
        keys: ["label1", "label2", "total"],
        sql: "SELECT j.nama as label1, COALESCE(km.nama, 'Tanpa Kelompok') as label2, COUNT(m.id) as total FROM matakuliah m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN kelompok_matakuliah km ON m.kelompok_matakuliah = km.id WHERE m.id IS NOT NULL AND (m.aktif = true OR m.aktif IS NULL) {FILTER} GROUP BY j.nama, km.nama ORDER BY j.nama ASC, km.nama ASC"
    },
    "2": {
        title: "<%= Common.getBahasaConfig("Sebaran per Program Studi & Jenis Mata Kuliah") %>",
        columns: ["Program Studi", "Jenis Mata Kuliah", "Total Mata Kuliah"],
        keys: ["label1", "label2", "total"],
        sql: "SELECT j.nama as label1, COALESCE(m.jenis_matakuliah, 'Tidak Diketahui') as label2, COUNT(m.id) as total FROM matakuliah m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.id IS NOT NULL AND (m.aktif = true OR m.aktif IS NULL) {FILTER} GROUP BY j.nama, m.jenis_matakuliah ORDER BY j.nama ASC, m.jenis_matakuliah ASC"
    },
    "3": {
        title: "<%= Common.getBahasaConfig("Sebaran Mata Kuliah Berdasarkan SKS") %>",
        columns: ["Beban SKS", "Total Mata Kuliah"],
        keys: ["label1", "total"],
        sql: "SELECT COALESCE(CAST(m.sks AS VARCHAR), '0') || ' SKS' as label1, COUNT(m.id) as total FROM matakuliah m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.id IS NOT NULL AND (m.aktif = true OR m.aktif IS NULL) {FILTER} GROUP BY m.sks ORDER BY m.sks ASC"
    },
    "4": {
        title: "<%= Common.getBahasaConfig("Sebaran Komponen (Teori, Praktik, Simulasi, Lapangan) per Prodi") %>",
        columns: ["Program Studi", "Komponen Pembelajaran", "Total Mata Kuliah"],
        keys: ["label1", "label2", "total"],
        sql: "SELECT j.nama as label1, 'Teori/Diskusi' as label2, COUNT(m.id) as total FROM matakuliah m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.sksdiskusi > 0 AND (m.aktif = true OR m.aktif IS NULL) {FILTER} GROUP BY j.nama UNION SELECT j.nama, 'Praktik', COUNT(m.id) FROM matakuliah m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.skspraktek > 0 AND (m.aktif = true OR m.aktif IS NULL) {FILTER} GROUP BY j.nama UNION SELECT j.nama, 'Simulasi', COUNT(m.id) FROM matakuliah m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.skssimulasi > 0 AND (m.aktif = true OR m.aktif IS NULL) {FILTER} GROUP BY j.nama UNION SELECT j.nama, 'Praktek Lapangan', COUNT(m.id) FROM matakuliah m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.sksprakteklapangan > 0 AND (m.aktif = true OR m.aktif IS NULL) {FILTER} GROUP BY j.nama ORDER BY label1 ASC, label2 ASC"
    },
    "5": {
        title: "<%= Common.getBahasaConfig("Sebaran Kelengkapan Dokumen per Prodi") %>",
        columns: ["Program Studi", "Jenis Dokumen", "Mata Kuliah Lengkap"],
        keys: ["label1", "label2", "total"],
        sql: "SELECT j.nama as label1, 'SAP' as label2, COUNT(m.id) as total FROM matakuliah m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.adasap = true AND (m.aktif = true OR m.aktif IS NULL) {FILTER} GROUP BY j.nama UNION SELECT j.nama, 'Silabus', COUNT(m.id) FROM matakuliah m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.adasilabus = true AND (m.aktif = true OR m.aktif IS NULL) {FILTER} GROUP BY j.nama UNION SELECT j.nama, 'Bahan Ajar', COUNT(m.id) FROM matakuliah m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.adabahanajar = true AND (m.aktif = true OR m.aktif IS NULL) {FILTER} GROUP BY j.nama UNION SELECT j.nama, 'Acara Praktek', COUNT(m.id) FROM matakuliah m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.adaacarapraktek = true AND (m.aktif = true OR m.aktif IS NULL) {FILTER} GROUP BY j.nama UNION SELECT j.nama, 'Diktat', COUNT(m.id) FROM matakuliah m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.adadiktat = true AND (m.aktif = true OR m.aktif IS NULL) {FILTER} GROUP BY j.nama ORDER BY label1 ASC, label2 ASC"
    },
    "6": {
        title: "<%= Common.getBahasaConfig("Sebaran Mata Kuliah Ekstrakurikuler per Prodi") %>",
        columns: ["Program Studi", "Sifat Mata Kuliah", "Total"],
        keys: ["label1", "label2", "total"],
        sql: "SELECT j.nama as label1, CASE WHEN m.extrakulikuler = true THEN 'Ekstrakurikuler' ELSE 'Reguler/Kurikuler' END as label2, COUNT(m.id) as total FROM matakuliah m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.id IS NOT NULL AND (m.aktif = true OR m.aktif IS NULL) {FILTER} GROUP BY j.nama, m.extrakulikuler ORDER BY j.nama ASC"
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
    const kelId = document.getElementById('filterKelompokStat<%=rnd%>').value;
    const jenisMk = document.getElementById('filterJenisStat<%=rnd%>').value;

    if (fakId) filterSql += " AND f.id = " + fakId + " ";
    if (prodiId) filterSql += " AND j.id = " + prodiId + " ";
    if (kelId) filterSql += " AND m.kelompok_matakuliah = " + kelId + " ";
    if (jenisMk) filterSql += " AND m.jenis_matakuliah = '" + jenisMk.replace(/'/g, "''") + "' ";

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
    const ctx = document.getElementById('chartMatakuliah<%=rnd%>').getContext('2d');
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
        data: { labels: labels, datasets: [{ label: '<%= Common.getBahasaConfigJS("Jumlah MK") %>', data: totals, backgroundColor: 'rgba(253, 126, 20, 0.7)', borderColor: 'rgba(253, 126, 20, 1)', borderWidth: 1, borderRadius: 4 }] },
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
    link.download = "Statistik_Mata_Kuliah_" + new Date().getTime() + ".csv";
    document.body.appendChild(link); link.click(); document.body.removeChild(link);
};

// ==========================================
// MODAL POPUP DETAIL MATAKULIAH
// ==========================================
let detailDataCache<%=rnd%> = [];
let detailCurrentPage<%=rnd%> = 1;

const safeSqlStr<%=rnd%> = (val) => {
    if(val === '-' || val === 'Tanpa Kelompok' || val === 'Tidak Diketahui') return " IS NULL ";
    return " = '" + val.toString().replace(/'/g, "''") + "' ";
};

const bukaDetailStatistik<%=rnd%> = async (dataIndex, event) => {
    const row = currentData<%=rnd%>[dataIndex];
    const tipe = document.getElementById("jenisLaporan<%=rnd%>").value;
    const btn = event.currentTarget;
    const oriContent = btn.innerHTML;
    btn.innerHTML = "<span class='spinner-border spinner-border-sm'></span>"; btn.disabled = true;

    const globalFilter = getFilterSql<%=rnd%>();
    
    // Menambahkan kriteria: (m.aktif = true OR m.aktif IS NULL) ke kueri dasar detail
    let sqlJson = "SELECT m.kode, m.nama, j.nama as prodi, m.sks, km.nama as kelompok, m.jenis_matakuliah, m.adasap, m.adasilabus, m.adabahanajar FROM matakuliah m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN kelompok_matakuliah km ON m.kelompok_matakuliah = km.id WHERE m.id IS NOT NULL AND (m.aktif = true OR m.aktif IS NULL) " + globalFilter;
    let sqlHtml = "SELECT m.* FROM matakuliah m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN kelompok_matakuliah km ON m.kelompok_matakuliah = km.id WHERE m.id IS NOT NULL AND (m.aktif = true OR m.aktif IS NULL) " + globalFilter;
    
    let filter = "";
    if(tipe == 1) filter += " AND j.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND km.nama " + safeSqlStr<%=rnd%>(row.label2);
    if(tipe == 2) filter += " AND j.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND m.jenis_matakuliah " + safeSqlStr<%=rnd%>(row.label2);
    if(tipe == 3) filter += " AND CAST(m.sks AS VARCHAR) || ' SKS' = '" + row.label1 + "'";
    
    // Penanganan khusus untuk kueri dengan UNION (tipe 4 & 5)
    if(tipe == 4) {
        filter += " AND j.nama " + safeSqlStr<%=rnd%>(row.label1);
        if(row.label2 === 'Teori/Diskusi') filter += " AND m.sksdiskusi > 0 ";
        if(row.label2 === 'Praktik') filter += " AND m.skspraktek > 0 ";
        if(row.label2 === 'Simulasi') filter += " AND m.skssimulasi > 0 ";
        if(row.label2 === 'Praktek Lapangan') filter += " AND m.sksprakteklapangan > 0 ";
    }
    if(tipe == 5) {
        filter += " AND j.nama " + safeSqlStr<%=rnd%>(row.label1);
        if(row.label2 === 'SAP') filter += " AND m.adasap = true ";
        if(row.label2 === 'Silabus') filter += " AND m.adasilabus = true ";
        if(row.label2 === 'Bahan Ajar') filter += " AND m.adabahanajar = true ";
        if(row.label2 === 'Acara Praktek') filter += " AND m.adaacarapraktek = true ";
        if(row.label2 === 'Diktat') filter += " AND m.adadiktat = true ";
    }
    if(tipe == 6) {
        filter += " AND j.nama " + safeSqlStr<%=rnd%>(row.label1);
        if(row.label2 === 'Ekstrakurikuler') filter += " AND m.extrakulikuler = true ";
        else filter += " AND (m.extrakulikuler = false OR m.extrakulikuler IS NULL) ";
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
    if(document.getElementById(modalId)) document.getElementById(modalId).remove();
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
                                "<thead class='bg-primary text-white'><tr>" +
                                    "<th class='text-center py-3' style='width: 50px;'>No</th>" +
                                    "<th class='py-3'>Kode MK</th><th class='py-3'>Nama Mata Kuliah</th>" +
                                    "<th class='py-3'>Program Studi</th><th class='text-center py-3'>SKS</th>" +
                                    "<th class='py-3'>Kelompok</th><th class='py-3'>Jenis</th><th class='py-3'>Dokumen</th>" +
                                "</tr></thead>" +
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
        const res = await fetch('<%=Common.ROOT%>/dsh?hanya_tampil_jsp=true&p=pagesmastermatakuliahzul&s=_statistik_matakuliah&baru=true&rnd=<%=rnd%>', {
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
    let csv = "\uFEFFNo;Kode MK;Nama Mata Kuliah;Program Studi;SKS;Kelompok;Jenis\n";
    detailDataCache<%=rnd%>.forEach((row, i) => {
        let cols = [ i+1, row.kode || "-", row.nama || "-", row.prodi || "-", row.sks || "-", row.kelompok || "-", row.jenis_matakuliah || "-" ];
        csv += cols.map(v => '"' + String(v).replace(/"/g, '""') + '"').join(";") + "\n";
    });
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement("a"); link.href = URL.createObjectURL(blob);
    link.download = "Detail_Mata_Kuliah_" + new Date().getTime() + ".csv";
    document.body.appendChild(link); link.click(); document.body.removeChild(link);
};

loadStatistik<%=rnd%>();
</script>