<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.ProfileImageUtil"%>
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
    // BLOK SERVER-SIDE KHUSUS UNTUK MERENDER TABEL DETAIL POPUP DENGAN FOTO
    // =========================================================================
    String actionParams = request.getParameter("action_jsp");
    if ("get_html_detail".equals(actionParams)) {
        String sql = request.getParameter("sql");
        int start = Integer.parseInt(request.getParameter("start") != null ? request.getParameter("start") : "0");
        int limit = Integer.parseInt(request.getParameter("limit") != null ? request.getParameter("limit") : "10");
        Session sess = HibernateUtil.openSession();
        try {
            List<Mahasiswa> list = sess.createSQLQuery(sql)
                                       .addEntity(Mahasiswa.class)
                                       .setFirstResult(start)
                                       .setMaxResults(limit)
                                       .list();
            for (int i = 0; i < list.size(); i++) {
                Mahasiswa m = list.get(i);
                String urlFotoMhs = "";
                try {
                    urlFotoMhs = ProfileImageUtil.getUrlFotoPengguna(new Tbmuser(m), 152, 114);
                } catch (Exception e) {
                    urlFotoMhs = Common.ROOT + "/img/administrator-icon_default.png";
                }

                String nim = m.getNim() != null ? m.getNim() : "-";
                String nama = m.getNama() != null ? m.getNama() : "-";
                String prodi = m.getJurusan() != null ? m.getJurusan().getNama() : "-";
                String prog = m.getProgram() != null ? m.getProgram() : "-";
                String thnMasuk = m.getTahunangkatan() != null ? m.getTahunangkatan().toString() : "-";
                
                %>
                <tr>
                    <td class='text-center'><%= (start + i + 1) %></td>
                    <td class='text-center'>
                        <img src="<%= urlFotoMhs %>" class="rounded-circle img-thumbnail shadow-sm p-1" style="width: 40px; height: 40px; object-fit: cover;">
                    </td>
                    <td class='fw-bold text-primary'><%= nim %></td>
                    <td class='fw-bold'><%= nama %></td>
                    <td><%= prodi %></td>
                    <td class='text-center'><%= prog %></td>
                    <td class='text-center fw-bold'><%= thnMasuk %></td>
                </tr>
                <%
            }
        } catch(Exception e) {
            out.print("<tr><td colspan='7' class='text-center text-danger'>Terjadi kesalahan query data.</td></tr>");
        } finally {
            try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastermahasiswazul/_statistik_mahasiswa_aktif.jsp:64");}
            try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastermahasiswazul/_statistik_mahasiswa_aktif.jsp:65");}
            HibernateUtil.closeSession();
        }
        return; 
    }

    // =========================================================================
    // BLOK HALAMAN UTAMA STATISTIK MAHASISWA AKTIF
    // =========================================================================
    String rnd = request.getParameter("rnd");
    if (rnd == null) rnd = "";
    
    // TAMBAHKAN BARIS INI: Beri awalan unik untuk modul Mahasiswa Aktif
    rnd = "Mhs" + rnd;

    List<Fakultas> listFakultas = new ArrayList<Fakultas>();
    List<Jurusan> listJurusan = new ArrayList<Jurusan>();
    
    Session sessFilter = HibernateUtil.openSession();
    try {
        listFakultas = ConstantValues.simpleList(sessFilter.createCriteria(Fakultas.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Fakultas.class);
        listJurusan = ConstantValues.simpleList(sessFilter.createCriteria(Jurusan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Jurusan.class);
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pagesmastermahasiswazul/_statistik_mahasiswa_aktif.jsp:88");
    } finally {
        try { sessFilter.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastermahasiswazul/_statistik_mahasiswa_aktif.jsp:90");}
        try { sessFilter.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastermahasiswazul/_statistik_mahasiswa_aktif.jsp:91");}
        HibernateUtil.closeSession();
    }
%>

<script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/chart.js"></script>

<div class="container-fluid p-0 animate__animated animate__fadeIn">
    <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3">
        <h5 class="mb-0 fw-bold text-primary">
            <i class="fas fa-users me-2"></i><%= Common.getBahasaConfig("Dashboard Statistik Mahasiswa Aktif") %>
        </h5>
    </div>

    <div class="card shadow-sm border-0 rounded-4 mb-4">
        <div class="card-body bg-light rounded-4 p-4">
            <div class="row g-3">
                <div class="col-md-12 mb-2">
                    <label class="form-label fw-bold text-secondary small"><i class="fas fa-list me-1"></i><%= Common.getBahasaConfig("Pilih Jenis Laporan:") %></label>
                    <select class="form-select border-primary shadow-sm fw-bold" id="jenisLaporan<%=rnd%>" onchange="loadStatistik<%=rnd%>()">
                        <optgroup label="Data Akademik Mahasiswa Aktif">
                            <option value="1"><%= Common.getBahasaConfig("1. Sebaran per Tahun Masuk (Angkatan)") %></option>
                            <option value="2"><%= Common.getBahasaConfig("2. Sebaran per Program Studi & Angkatan") %></option>
                            <option value="3"><%= Common.getBahasaConfig("3. Sebaran per Jenis Kelamin") %></option>
                            <option value="4"><%= Common.getBahasaConfig("4. Sebaran per Program Kelas (Reguler/Eksekutif)") %></option>
                            <option value="5"><%= Common.getBahasaConfig("5. Sebaran per Agama") %></option>
                            <option value="6"><%= Common.getBahasaConfig("6. Sebaran per Status Awal (Baru/Pindahan)") %></option>
                            <option value="7"><%= Common.getBahasaConfig("7. Sebaran per Fakultas, Prodi, dan Angkatan") %></option>
                        </optgroup>
                        <optgroup label="Data Biodata Mahasiswa & Orang Tua">
                            <option value="8"><%= Common.getBahasaConfig("8. Sebaran per Jenis Pekerjaan Ayah & Prodi") %></option>
                            <option value="9"><%= Common.getBahasaConfig("9. Sebaran per Jenis Pekerjaan Ibu & Prodi") %></option>
                            <option value="10"><%= Common.getBahasaConfig("10. Sebaran per Penghasilan Ayah & Prodi") %></option>
                            <option value="11"><%= Common.getBahasaConfig("11. Sebaran per Sekolah Asal (SMA/SMK)") %></option>
                            <option value="12"><%= Common.getBahasaConfig("12. Sebaran per Jenis Tinggal Mahasiswa") %></option>
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
                                opt.style.display = '';
                                opt.disabled = false;
                            } else {
                                opt.style.display = 'none';
                                opt.disabled = true;
                            }
                        }
                        loadStatistik<%=rnd%>();
                    ">
                        <option value=""><%= Common.getBahasaConfig("Semua Fakultas") %></option>
                        <% for(Fakultas f : listFakultas) { %>
                            <option value="<%= f.getId() %>"><%= f.getNama() %></option>
                        <% } %>
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
                
                <div class="col-md-2">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-calendar-alt me-1"></i> <%= Common.getBahasaConfig("Angkatan (Dari)") %></label>
                    <input type="number" id="filterTaStartStat<%=rnd%>" class="form-control form-control-sm border-info shadow-none" placeholder="Cth: 2020" onkeydown="if(event.key === 'Enter') loadStatistik<%=rnd%>()">
                </div>
                <div class="col-md-2">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-calendar-alt me-1"></i> <%= Common.getBahasaConfig("Angkatan (Sampai)") %></label>
                    <input type="number" id="filterTaEndStat<%=rnd%>" class="form-control form-control-sm border-info shadow-none" placeholder="Cth: 2024" onkeydown="if(event.key === 'Enter') loadStatistik<%=rnd%>()">
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
                <canvas id="chartAlumni<%=rnd%>"></canvas>
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
            <span class="small text-muted fw-bold" id="infoPaginasi<%=rnd%>">Menampilkan 0 data</span>
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
// KONFIGURASI SQL REKAPITULASI MAHASISWA AKTIF
// Filter Wajib: m.status_keluar IS NULL
// ==========================================
const reportConfigs<%=rnd%> = {
    "1": {
        title: "Sebaran per Tahun Masuk (Angkatan)",
        columns: ["Tahun Masuk (Angkatan)", "Total Mahasiswa Aktif"],
        keys: ["label1", "total"],
        sql: "SELECT COALESCE(CAST(m.tahunangkatan AS VARCHAR), '-') as label1, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.status_keluar IS NULL {FILTER} GROUP BY m.tahunangkatan ORDER BY m.tahunangkatan DESC"
    },
    "2": {
        title: "Sebaran per Program Studi & Angkatan",
        columns: ["Program Studi", "Tahun Masuk (Angkatan)", "Total Mahasiswa Aktif"],
        keys: ["label1", "label2", "total"],
        sql: "SELECT j.nama as label1, COALESCE(CAST(m.tahunangkatan AS VARCHAR), '-') as label2, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.status_keluar IS NULL {FILTER} GROUP BY j.nama, m.tahunangkatan ORDER BY j.nama ASC, m.tahunangkatan DESC"
    },
    "3": {
        title: "Sebaran per Jenis Kelamin",
        columns: ["Jenis Kelamin", "Total Mahasiswa Aktif"],
        keys: ["label1", "total"],
        sql: "SELECT COALESCE(m.kelamin, 'Tidak Diketahui') as label1, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.status_keluar IS NULL {FILTER} GROUP BY m.kelamin"
    },
    "4": {
        title: "Sebaran per Program Kelas",
        columns: ["Program Kelas", "Total Mahasiswa Aktif"],
        keys: ["label1", "total"],
        sql: "SELECT COALESCE(m.program, '-') as label1, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.status_keluar IS NULL {FILTER} GROUP BY m.program"
    },
    "5": {
        title: "Sebaran per Agama",
        columns: ["Agama", "Total Mahasiswa Aktif"],
        keys: ["label1", "total"],
        sql: "SELECT a.nama as label1, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN agama a ON m.agama = a.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.status_keluar IS NULL {FILTER} GROUP BY a.nama ORDER BY a.nama ASC"
    },
    "6": {
        title: "Sebaran per Status Awal Mahasiswa",
        columns: ["Status Awal", "Total Mahasiswa Aktif"],
        keys: ["label1", "total"],
        sql: "SELECT sa.nama as label1, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN status_awal_mahasiswa sa ON m.status_awal_mahasiswa = sa.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.status_keluar IS NULL {FILTER} GROUP BY sa.nama"
    },
    "7": {
        title: "Sebaran per Fakultas, Prodi, dan Angkatan",
        columns: ["Fakultas", "Program Studi", "Tahun Masuk", "Total Mahasiswa Aktif"],
        keys: ["label1", "label2", "label3", "total"],
        sql: "SELECT f.nama as label1, j.nama as label2, COALESCE(CAST(m.tahunangkatan AS VARCHAR), '-') as label3, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.status_keluar IS NULL {FILTER} GROUP BY f.nama, j.nama, m.tahunangkatan ORDER BY f.nama ASC, j.nama ASC, m.tahunangkatan DESC"
    },
    "8": {
        title: "Sebaran per Jenis Pekerjaan Ayah & Prodi",
        columns: ["Pekerjaan Ayah", "Program Studi", "Total Mahasiswa Aktif"],
        keys: ["label1", "label2", "total"],
        sql: "SELECT p.nama as label1, j.nama as label2, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN biodata_mahasiswa bm ON bm.mahasiswa = m.id LEFT JOIN pekerjaan p ON bm.jenis_pekerjaan_ayah = p.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.status_keluar IS NULL {FILTER} GROUP BY p.nama, j.nama ORDER BY p.nama ASC, j.nama ASC"
    },
    "9": {
        title: "Sebaran per Jenis Pekerjaan Ibu & Prodi",
        columns: ["Pekerjaan Ibu", "Program Studi", "Total Mahasiswa Aktif"],
        keys: ["label1", "label2", "total"],
        sql: "SELECT p.nama as label1, j.nama as label2, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN biodata_mahasiswa bm ON bm.mahasiswa = m.id LEFT JOIN pekerjaan p ON bm.jenis_pekerjaan_ibu = p.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.status_keluar IS NULL {FILTER} GROUP BY p.nama, j.nama ORDER BY p.nama ASC, j.nama ASC"
    },
    "10": {
        title: "Sebaran per Penghasilan Ayah & Prodi",
        columns: ["Penghasilan Ayah", "Program Studi", "Total Mahasiswa Aktif"],
        keys: ["label1", "label2", "total"],
        sql: "SELECT p.nama as label1, j.nama as label2, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN biodata_mahasiswa bm ON bm.mahasiswa = m.id LEFT JOIN penghasilan p ON bm.jenis_penghasilan_ayah = p.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.status_keluar IS NULL {FILTER} GROUP BY p.nama, j.nama ORDER BY p.nama ASC, j.nama ASC"
    },
    "11": {
        title: "Sebaran per Sekolah Asal (SMA/SMK)",
        columns: ["Sekolah Asal", "Total Mahasiswa Aktif"],
        keys: ["label1", "total"],
        sql: "SELECT ns.nama as label1, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN biodata_mahasiswa bm ON bm.mahasiswa = m.id LEFT JOIN nama_sekolah_asal ns ON bm.nama_sekolah_asal = ns.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.status_keluar IS NULL {FILTER} GROUP BY ns.nama ORDER BY ns.nama ASC"
    },
    "12": {
        title: "Sebaran per Jenis Tinggal Mahasiswa",
        columns: ["Jenis Tinggal", "Total Mahasiswa Aktif"],
        keys: ["label1", "total"],
        sql: "SELECT jt.nama as label1, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN biodata_mahasiswa bm ON bm.mahasiswa = m.id LEFT JOIN jenis_tinggal_mahasiswa jt ON bm.jenis_tinggal_mahasiswa = jt.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE m.status_keluar IS NULL {FILTER} GROUP BY jt.nama ORDER BY jt.nama ASC"
    }
};

const fetchDataAPI<%=rnd%> = async (sql) => {
    try {
        const res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sql: sql, action: "sql", tanpaLogin: "true" })
        });
        const result = await res.json();
        return result.data || [];
    } catch (e) { 
        console.error("API Error:", e);
        return []; 
    }
};

const getFilterSql<%=rnd%> = () => {
    let filterSql = "";
    const fakId = document.getElementById('filterFakultasStat<%=rnd%>').value;
    const prodiId = document.getElementById('filterProdiStat<%=rnd%>').value;
    const taS = document.getElementById('filterTaStartStat<%=rnd%>').value;
    const taE = document.getElementById('filterTaEndStat<%=rnd%>').value;

    if (fakId) filterSql += " AND f.id = " + fakId + " ";
    if (prodiId) filterSql += " AND j.id = " + prodiId + " ";
    if (taS) filterSql += " AND m.tahunangkatan >= " + taS + " ";
    if (taE) filterSql += " AND m.tahunangkatan <= " + taE + " ";

    return filterSql;
};

const loadStatistik<%=rnd%> = async () => {
    const selector = document.getElementById("jenisLaporan<%=rnd%>").value;
    currentConfig<%=rnd%> = reportConfigs<%=rnd%>[selector];
    document.getElementById("judulTabel<%=rnd%>").innerText = currentConfig<%=rnd%>.title;
    document.getElementById("tbodyRekap<%=rnd%>").innerHTML = "";
    document.getElementById("loaderStatistik<%=rnd%>").classList.remove("d-none");
    
    const filterParams = getFilterSql<%=rnd%>();
    const finalSql = currentConfig<%=rnd%>.sql.replace("{FILTER}", filterParams);
    
    currentData<%=rnd%> = await fetchDataAPI<%=rnd%>(finalSql);
    currentPage<%=rnd%> = 1;
    document.getElementById("loaderStatistik<%=rnd%>").classList.add("d-none");
    
    renderHeaderTabel<%=rnd%>();
    renderTabel<%=rnd%>();
    renderChart<%=rnd%>();
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
        tbody.innerHTML = "<tr><td colspan='" + (currentConfig<%=rnd%>.columns.length + 1) + "' class='text-center text-muted p-4'>Tidak ada data ditemukan</td></tr>";
        document.getElementById("infoPaginasi<%=rnd%>").innerText = "Menampilkan 0 data";
        document.getElementById("paginasiTabel<%=rnd%>").innerHTML = "";
        return;
    }
    
    const startIdx = (currentPage<%=rnd%> - 1) * itemsPerPage<%=rnd%>;
    const endIdx = Math.min(startIdx + itemsPerPage<%=rnd%>, currentData<%=rnd%>.length);
    const pagedData = currentData<%=rnd%>.slice(startIdx, endIdx);
    
    let html = "";
    pagedData.forEach((row, idx) => {
        html += "<tr>";
        html += "<td class='text-center fw-bold'>" + (startIdx + idx + 1) + "</td>";
        
        currentConfig<%=rnd%>.keys.forEach(key => {
            const val = row[key] !== null ? row[key] : "-";
            if (key === 'total') {
                html += "<td><button class='btn btn-sm btn-outline-primary rounded-pill fw-bold shadow-sm' onclick='bukaDetailStatistik<%=rnd%>(" + (startIdx + idx) + ", event)'>" + val + " <i class='fas fa-search-plus ms-1'></i></button></td>";
            } else {
                html += "<td>" + val + "</td>";
            }
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
    const ctx = document.getElementById('chartAlumni<%=rnd%>').getContext('2d');
    if(chartInstance<%=rnd%>) chartInstance<%=rnd%>.destroy();
    
    let labels = [];
    let totals = [];
    const chartData = currentData<%=rnd%>.slice(0, 20); 
    
    chartData.forEach(row => {
        let lbl = "";
        currentConfig<%=rnd%>.keys.forEach(k => { if(k !== 'total') lbl += row[k] + " "; });
        labels.push(lbl.trim());
        totals.push(row['total']);
    });
    
    chartInstance<%=rnd%> = new Chart(ctx, {
        type: 'bar',
        data: { labels: labels, datasets: [{ label: 'Jumlah Mahasiswa Aktif', data: totals, backgroundColor: 'rgba(25, 135, 84, 0.7)', borderColor: 'rgba(25, 135, 84, 1)', borderWidth: 1, borderRadius: 4 }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } }
    });
};

const downloadExcel<%=rnd%> = () => {
    if(!currentData<%=rnd%> || currentData<%=rnd%>.length === 0) { alert('<%= Common.getBahasaConfigJS("Belum ada data yang dapat diunduh. Silakan muat data terlebih dahulu.") %>'); return; }
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
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = "Statistik_Mahasiswa_Aktif_" + new Date().getTime() + ".csv";
    document.body.appendChild(link); link.click(); document.body.removeChild(link);
};

// ==========================================
// MODAL POPUP DETAIL MAHASISWA & FOTO
// ==========================================
let detailDataCache<%=rnd%> = [];
let detailCurrentPage<%=rnd%> = 1;

const safeSqlStr<%=rnd%> = (val) => {
    if(val === '-' || val === 'Belum Ada' || val === 'Tidak Diketahui') return " IS NULL ";
    return " = '" + val.toString().replace(/'/g, "''") + "' ";
};

const bukaDetailStatistik<%=rnd%> = async (dataIndex, event) => {
    const row = currentData<%=rnd%>[dataIndex];
    const tipe = document.getElementById("jenisLaporan<%=rnd%>").value;
    const btn = event.currentTarget;
    const oriContent = btn.innerHTML;
    btn.innerHTML = "<span class='spinner-border spinner-border-sm'></span>";
    btn.disabled = true;

    const globalFilter = getFilterSql<%=rnd%>();
    let joinExtra = "";
    if (tipe >= 8) {
        joinExtra += " LEFT JOIN biodata_mahasiswa bm ON bm.mahasiswa = m.id ";
    }
    if (tipe == 8) joinExtra += " LEFT JOIN pekerjaan p ON bm.jenis_pekerjaan_ayah = p.id ";
    if (tipe == 9) joinExtra += " LEFT JOIN pekerjaan p ON bm.jenis_pekerjaan_ibu = p.id ";
    if (tipe == 10) joinExtra += " LEFT JOIN penghasilan p ON bm.jenis_penghasilan_ayah = p.id ";
    if (tipe == 11) joinExtra += " LEFT JOIN nama_sekolah_asal ns ON bm.nama_sekolah_asal = ns.id ";
    if (tipe == 12) joinExtra += " LEFT JOIN jenis_tinggal_mahasiswa jt ON bm.jenis_tinggal_mahasiswa = jt.id ";
    
    let sqlJson = "SELECT m.nim, m.nama, j.nama as prodi, m.program, m.tahunangkatan FROM mahasiswa m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN agama a ON m.agama = a.id LEFT JOIN status_awal_mahasiswa sa ON m.status_awal_mahasiswa = sa.id " + joinExtra + " WHERE m.status_keluar IS NULL " + globalFilter;
    let sqlHtml = "SELECT m.* FROM mahasiswa m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN agama a ON m.agama = a.id LEFT JOIN status_awal_mahasiswa sa ON m.status_awal_mahasiswa = sa.id " + joinExtra + " WHERE m.status_keluar IS NULL " + globalFilter;
    
    let filter = "";
    if(tipe == 1) filter += " AND CAST(m.tahunangkatan AS VARCHAR) " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 2) filter += " AND j.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND CAST(m.tahunangkatan AS VARCHAR) " + safeSqlStr<%=rnd%>(row.label2);
    if(tipe == 3) filter += " AND m.kelamin " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 4) filter += " AND m.program " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 5) filter += " AND a.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 6) filter += " AND sa.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 7) filter += " AND f.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND j.nama " + safeSqlStr<%=rnd%>(row.label2) + " AND CAST(m.tahunangkatan AS VARCHAR) " + safeSqlStr<%=rnd%>(row.label3);
    if(tipe >= 8 && tipe <= 10) filter += " AND p.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND j.nama " + safeSqlStr<%=rnd%>(row.label2);
    if(tipe == 11) filter += " AND ns.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 12) filter += " AND jt.nama " + safeSqlStr<%=rnd%>(row.label1);

    sqlJson += filter + " ORDER BY m.nama ASC";
    sqlHtml += filter + " ORDER BY m.nama ASC";

    window.currentDetailSqlHtml<%=rnd%> = sqlHtml;

    detailDataCache<%=rnd%> = await fetchDataAPI<%=rnd%>(sqlJson);
    detailCurrentPage<%=rnd%> = 1;
    btn.innerHTML = oriContent;
    btn.disabled = false;

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
                            "<button class='btn btn-sm btn-success rounded-pill fw-bold shadow-sm me-2 px-3' onclick='downloadDetailExcel<%=rnd%>()'>" +
                                "<i class='fas fa-file-excel me-1'></i>Unduh Excel" +
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
                                        "<th class='text-center py-3' style='width: 60px;'>Foto</th>" +
                                        "<th class='py-3'>NIM</th>" +
                                        "<th class='py-3'>Nama Mahasiswa</th>" +
                                        "<th class='py-3'>Program Studi</th>" +
                                        "<th class='text-center py-3'>Program</th>" +
                                        "<th class='text-center py-3'>Thn Masuk</th>" +
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
    const modalEl = new bootstrap.Modal(document.getElementById(modalId));
    modalEl.show();
    document.getElementById(modalId).addEventListener('hidden.bs.modal', function () { this.remove(); });

    renderTabelDetail<%=rnd%>();
};

const renderTabelDetail<%=rnd%> = async () => {
    const tbody = document.getElementById("tbodyDetail<%=rnd%>");
    if(detailDataCache<%=rnd%>.length === 0) {
        tbody.innerHTML = "<tr><td colspan='7' class='text-center text-muted p-4'>Tidak ada data rincian ditemukan</td></tr>";
        document.getElementById("infoPaginasiDetail<%=rnd%>").innerText = "Menampilkan 0 data";
        document.getElementById("paginasiDetail<%=rnd%>").innerHTML = "";
        return;
    }

    tbody.innerHTML = "<tr><td colspan='7' class='text-center p-4'><span class='spinner-border text-primary'></span></td></tr>";
    const startIdx = (detailCurrentPage<%=rnd%> - 1) * 10;
    const endIdx = Math.min(startIdx + 10, detailDataCache<%=rnd%>.length);

    const formData = new URLSearchParams();
    formData.append("action_jsp", "get_html_detail");
    formData.append("sql", window.currentDetailSqlHtml<%=rnd%>);
    formData.append("start", startIdx);
    formData.append("limit", 10);

    try {
        const res = await fetch('<%=Common.ROOT%>/dsh?hanya_tampil_jsp=true&p=pagesmastermahasiswazul&s=_statistik_mahasiswa_aktif&baru=true&rnd=<%=rnd%>', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData.toString()
        });
        const htmlServer = await res.text();
        tbody.innerHTML = htmlServer;
    } catch (e) {
        tbody.innerHTML = "<tr><td colspan='7' class='text-center text-danger p-3'>Gagal menarik data profil mahasiswa.</td></tr>";
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

const ubahHalamanDetail<%=rnd%> = (page) => { 
    detailCurrentPage<%=rnd%> = page; 
    renderTabelDetail<%=rnd%>(); 
};

const downloadDetailExcel<%=rnd%> = () => {
    if(!detailDataCache<%=rnd%> || detailDataCache<%=rnd%>.length === 0) return;
    let csv = "\uFEFF";
    csv += "No;NIM;Nama Mahasiswa;Program Studi;Program;Tahun Masuk\n";

    detailDataCache<%=rnd%>.forEach((row, i) => {
        let cols = [
            i+1,
            row.nim || "-",
            row.nama || "-",
            row.prodi || "-",
            row.program || "-",
            row.tahunangkatan || "-"
        ];
        let rowStr = cols.map(v => '"' + String(v).replace(/"/g, '""') + '"').join(";");
        csv += rowStr + "\n";
    });

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = "Detail_Mahasiswa_Aktif_" + new Date().getTime() + ".csv";
    document.body.appendChild(link); link.click(); document.body.removeChild(link);
};

// Eksekusi load awal saat file dimuat lewat ajax tab
loadStatistik<%=rnd%>();
</script>