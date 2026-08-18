<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Jurusan"%>
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
    rnd = "Ptm" + rnd; 

    String taCurrent = Common.getCurrentTahunAkademik();
    Boolean sekarangGanjil = Common.isNowSemensterGanjil();
    String currentSemester = (sekarangGanjil != null && sekarangGanjil) ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

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
            List<Map<String, Object>> list = sess.createSQLQuery(sql)
                                      .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                                      .setFirstResult(start)
                                      .setMaxResults(limit)
                                      .list();
                                      
            for (int i = 0; i < list.size(); i++) {
                Map<String, Object> row = list.get(i);
                
                String ta = row.get("ta") != null ? row.get("ta").toString() : "-";
                String smt = row.get("smt") != null ? row.get("smt").toString() : "-";
                String tanggal = row.get("tanggal") != null ? row.get("tanggal").toString() : "-";
                String hariJam = (row.get("waktu_mulai") != null ? row.get("waktu_mulai").toString() : "-") + " s/d " + 
                                 (row.get("waktu_selesai") != null ? row.get("waktu_selesai").toString() : "-");
                
                // Mengambil nilai Topik, Catatan, dan Indikator
                String topik = row.get("topik") != null && !row.get("topik").toString().trim().isEmpty() ? row.get("topik").toString() : "<span class='text-muted fst-italic'>-</span>";
                String catatan = row.get("catatan") != null && !row.get("catatan").toString().trim().isEmpty() ? row.get("catatan").toString() : "<span class='text-muted fst-italic'>-</span>";
                String indikator = row.get("indikator") != null && !row.get("indikator").toString().trim().isEmpty() ? row.get("indikator").toString() : "<span class='text-muted fst-italic'>-</span>";

                if ("dosen".equals(tipeDetail)) {
                    String namaDsn = row.get("nama_dsn") != null ? row.get("nama_dsn").toString() : "-";
                    %>
                    <tr>
                        <td class='text-center align-top'><%= (start + i + 1) %></td>
                        <td class='fw-bold text-primary align-top'><%= namaDsn %></td>
                        <td class='text-center align-top'><%= ta %><br><small class="text-muted"><%= smt %></small></td>
                        <td class='align-top'><div style="max-height: 100px; overflow-y: auto; font-size: 0.85rem;"><%= topik %></div></td>
                        <td class='align-top'><div style="max-height: 100px; overflow-y: auto; font-size: 0.85rem;"><%= catatan %></div></td>
                        <td class='align-top'><div style="max-height: 100px; overflow-y: auto; font-size: 0.85rem;"><%= indikator %></div></td>
                        <td class='text-center align-top'><%= tanggal %></td>
                        <td class='text-center align-top'><%= hariJam %></td>
                    </tr>
                    <%
                } else if ("mahasiswa".equals(tipeDetail)) {
                    String namaMhs = row.get("nama_mhs") != null ? row.get("nama_mhs").toString() : "-";
                    %>
                    <tr>
                        <td class='text-center align-top'><%= (start + i + 1) %></td>
                        <td class='fw-bold text-success align-top'><%= namaMhs %></td>
                        <td class='text-center align-top'><%= ta %><br><small class="text-muted"><%= smt %></small></td>
                        <td class='align-top'><div style="max-height: 100px; overflow-y: auto; font-size: 0.85rem;"><%= topik %></div></td>
                        <td class='align-top'><div style="max-height: 100px; overflow-y: auto; font-size: 0.85rem;"><%= catatan %></div></td>
                        <td class='align-top'><div style="max-height: 100px; overflow-y: auto; font-size: 0.85rem;"><%= indikator %></div></td>
                        <td class='text-center align-top'><%= tanggal %></td>
                        <td class='text-center align-top'><%= hariJam %></td>
                    </tr>
                    <%
                } else {
                    %>
                    <tr>
                        <td class='text-center align-top'><%= (start + i + 1) %></td>
                        <td class='text-center fw-bold align-top'><%= ta %><br><small class="text-muted"><%= smt %></small></td>
                        <td class='align-top'><div style="max-height: 100px; overflow-y: auto; font-size: 0.85rem;"><%= topik %></div></td>
                        <td class='align-top'><div style="max-height: 100px; overflow-y: auto; font-size: 0.85rem;"><%= catatan %></div></td>
                        <td class='align-top'><div style="max-height: 100px; overflow-y: auto; font-size: 0.85rem;"><%= indikator %></div></td>
                        <td class='text-center align-top'><%= tanggal %></td>
                        <td class='text-center fw-bold align-top'><%= hariJam %></td>
                    </tr>
                    <%
                }
            }
        } catch(Exception e) {
            out.print("<tr><td colspan='8' class='text-center text-danger'>" + Common.getBahasaConfig("Terjadi kesalahan kueri data rincian pertemuan.") + "</td></tr>");
        } finally {
            try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/_statistik_pertemuan.jsp:108");}
            ais.common.ElearningSessionUtil.closeQuietly(sess);
            HibernateUtil.closeSession();
        }
        return; 
    }

    List<Fakultas> listFakultas = new ArrayList<Fakultas>();
    List<Jurusan> listJurusan = new ArrayList<Jurusan>();
    Session sessFilter = HibernateUtil.openSession();
    try {
        listFakultas = ConstantValues.simpleList(sessFilter.createCriteria(Fakultas.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Fakultas.class);
        listJurusan = ConstantValues.simpleList(sessFilter.createCriteria(Jurusan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Jurusan.class);
    } catch(Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/_statistik_pertemuan.jsp:121"); } 
    finally { ais.common.ElearningSessionUtil.closeQuietly(sessFilter); HibernateUtil.closeSession(); }
%>

<div class="container-fluid p-0 animate__animated animate__fadeIn">
    <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3">
        <h5 class="mb-0 fw-bold text-primary">
            <i class="fas fa-handshake me-2"></i><%= Common.getBahasaConfig("Dashboard Statistik Pertemuan / Kehadiran") %>
        </h5>
    </div>

    <div class="card shadow-sm border-0 rounded-4 mb-4">
        <div class="card-body bg-light rounded-4 p-4">
            <div class="row g-3">
                <div class="col-md-12 mb-2">
                    <label class="form-label fw-bold text-secondary small"><i class="fas fa-list me-1"></i><%= Common.getBahasaConfig("Pilih Laporan Statistik:") %></label>
                    <select class="form-select border-primary shadow-sm fw-bold" id="jenisLaporan<%=rnd%>" onchange="loadStatistik<%=rnd%>()">
                        <optgroup label="<%= Common.getBahasaConfig("Data Dasar Pertemuan") %>">
                            <option value="1">1. Sebaran Pertemuan per Fakultas & Program Studi</option>
                            <option value="2">2. Sebaran Pertemuan per Tahun Akademik & Semester</option>
                            <option value="3">3. Sebaran Pertemuan per Ruangan</option>
                            <option value="4">4. Sebaran Pertemuan per Tanggal (Hari)</option>
                            <option value="5">5. Sebaran Pertemuan per Jam (Mulai - Selesai)</option>
                            <option value="6">6. Sebaran Status Pertemuan Dikunci (Finalisasi)</option>
                        </optgroup>
                        <optgroup label="<%= Common.getBahasaConfig("Data Keterlibatan (Join Khusus)") %>">
                            <option value="7">7. Total Mengajar/Kehadiran per Dosen</option>
                            <option value="8">8. Total Kehadiran per Mahasiswa</option>
                            <option value="9">9. Sebaran Ruangan yang Digunakan Dosen</option>
                            <option value="10">10. Sebaran Tanggal Kehadiran Mahasiswa</option>
                        </optgroup>
                    </select>
                </div>
                
                <div class="col-md-2">
                    <label class="form-label small fw-bold text-secondary"><%= Common.getBahasaConfig("Fakultas") %></label>
                    <select id="filterFakultasStat<%=rnd%>" class="form-select form-select-sm border-info shadow-none" onchange="
                        var fakId = this.value; var prodiSelect = document.getElementById('filterProdiStat<%=rnd%>');
                        prodiSelect.value = '';
                        for(var i=1; i<prodiSelect.options.length; i++){
                            var opt = prodiSelect.options[i];
                            opt.style.display = (fakId === '' || opt.getAttribute('data-fakultas') === fakId) ? '' : 'none';
                            opt.disabled = (fakId === '' || opt.getAttribute('data-fakultas') === fakId) ? false : true;
                        }
                        loadStatistik<%=rnd%>();
                    "><option value="">Semua Fakultas</option><% for(Fakultas f : listFakultas) { %><option value="<%= f.getId() %>"><%= f.getNama() %></option><% } %></select>
                </div>
                <div class="col-md-2">
                    <label class="form-label small fw-bold text-secondary"><%= Common.getBahasaConfig("Program Studi") %></label>
                    <select id="filterProdiStat<%=rnd%>" class="form-select form-select-sm border-info shadow-none" onchange="loadStatistik<%=rnd%>()">
                        <option value="" data-fakultas="">Semua Prodi</option>
                        <% for(Jurusan j : listJurusan) { String fakIdStr = (j.getFakultas() != null) ? j.getFakultas().getId().toString() : ""; %>
                            <option value="<%= j.getId() %>" data-fakultas="<%= fakIdStr %>"><%= j.getNama() %></option><% } %>
                    </select>
                </div>
                <div class="col-md-2">
                    <label class="form-label small fw-bold text-secondary"><%= Common.getBahasaConfig("Tahun Akademik") %></label>
                    <select class="form-select form-select-sm border-info shadow-none" onchange="loadStatistik<%=rnd%>()" id="filterTahun<%= rnd %>">
                        <option value="">Semua Tahun</option>
                        <% if (Common.tahunAngkatans != null) { 
                            for (String ta : Common.tahunAngkatans) { String selectedTa = (taCurrent != null && taCurrent.equals(ta)) ? "selected" : ""; %>
                                <option value="<%= ta %>" <%= selectedTa %>><%= ta %></option><% } } %>
                    </select>
                </div>
                <div class="col-md-2">
                    <label class="form-label small fw-bold text-secondary"><%= Common.getBahasaConfig("Semester") %></label>
                    <select class="form-select form-select-sm border-info shadow-none" onchange="loadStatistik<%=rnd%>()" id="filterSemester<%= rnd %>">
                        <option value="">Semua</option>
                        <option value="<%= Perkuliahan.GANJIL %>" <%= Perkuliahan.GANJIL.equals(currentSemester) ? "selected" : "" %>>Ganjil</option>
                        <option value="<%= Perkuliahan.GENAP %>" <%= Perkuliahan.GENAP.equals(currentSemester) ? "selected" : "" %>>Genap</option>
                        <option value="Semester Pendek">Pendek</option>
                    </select>
                </div>
                <div class="col-md-2">
                    <label class="form-label small fw-bold text-secondary"><%= Common.getBahasaConfig("Cari Nama Dosen") %></label>
                    <input type="text" id="filterDosenStat<%=rnd%>" class="form-control form-control-sm border-info shadow-none" placeholder="Masukkan nama..." onkeydown="if(event.key === 'Enter') loadStatistik<%=rnd%>()">
                </div>
                <div class="col-md-2">
                    <label class="form-label small fw-bold text-secondary"><%= Common.getBahasaConfig("Cari Nama Mahasiswa") %></label>
                    <input type="text" id="filterMhsStat<%=rnd%>" class="form-control form-control-sm border-info shadow-none" placeholder="Masukkan nama..." onkeydown="if(event.key === 'Enter') loadStatistik<%=rnd%>()">
                </div>
            </div>
            
            <div class="row mt-3 border-top pt-3">
                <div class="col-12 d-flex flex-wrap gap-2 justify-content-start mt-2">
                    <button class="btn btn-primary btn-sm rounded-pill px-4 fw-bold shadow-sm" onclick="loadStatistik<%=rnd%>()"><i class="fas fa-search me-1"></i> Terapkan Filter</button>
                    <button class="btn btn-success btn-sm rounded-pill px-4 fw-bold shadow-sm" onclick="downloadExcel<%=rnd%>()"><i class="fas fa-file-excel me-1"></i> Unduh Rekap (Excel)</button>
                </div>
            </div>
        </div>
    </div>

    <div class="card shadow-sm border-0 rounded-4 mb-4">
        <div class="card-body p-4"><div id="chartPertemuan<%=rnd%>" class="el-css-chart"></div></div>
    </div>

    <div class="card shadow-sm border-0 rounded-4 mb-4">
        <div class="card-header bg-white border-bottom py-3"><h6 class="mb-0 fw-bold text-dark" id="judulTabel<%=rnd%>">Memuat Data...</h6></div>
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover table-striped align-middle mb-0" id="tabelRekap<%=rnd%>">
                    <thead class="bg-light text-secondary" id="theadRekap<%=rnd%>"></thead>
                    <tbody id="tbodyRekap<%=rnd%>"></tbody>
                </table>
            </div>
            <div id="loaderStatistik<%=rnd%>" class="text-center p-5 d-none"><div class="spinner-border text-primary mb-2" role="status"></div><p class="text-muted small fw-bold">Sedang menarik data dari peladen...</p></div>
        </div>
        <div class="card-footer bg-white border-top p-3 d-flex justify-content-between align-items-center">
            <span class="small text-muted fw-bold" id="infoPaginasi<%=rnd%>">Menampilkan 0 data</span>
            <nav><ul class="pagination pagination-sm mb-0 shadow-sm" id="paginasiTabel<%=rnd%>"></ul></nav>
        </div>
    </div>
</div>

<script>
let currentData<%=rnd%> = [];
let currentPage<%=rnd%> = 1;
const itemsPerPage<%=rnd%> = 10;
let currentConfig<%=rnd%> = null;

// ==========================================
// KONFIGURASI SQL REKAPITULASI PERTEMUAN
// ==========================================
const basePtmJoinSql = " FROM pertemuan p LEFT JOIN jurusan j ON p.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN ruang r ON p.ruang = r.id WHERE (p.aktif = true OR p.aktif IS NULL) ";

const baseDsnJoinSql = " FROM pertemuan p JOIN dosen d ON p.dosens LIKE '%,' || CAST(d.id AS VARCHAR) || ',%' LEFT JOIN jurusan j ON p.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN ruang r ON p.ruang = r.id WHERE (p.aktif = true OR p.aktif IS NULL) AND (d.aktif = true OR d.aktif IS NULL) ";

const baseMhsJoinSql = " FROM pertemuan p JOIN mahasiswa m ON p.mahasiswas LIKE '%,' || CAST(m.id AS VARCHAR) || ',%' LEFT JOIN jurusan j ON p.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN ruang r ON p.ruang = r.id WHERE (p.aktif = true OR p.aktif IS NULL) AND (m.aktif = true OR m.aktif IS NULL) ";

const reportConfigs<%=rnd%> = {
    "1": { title: "Sebaran Pertemuan per Fakultas & Program Studi", columns: ["Fakultas", "Program Studi", "Total Pertemuan"], keys: ["label1", "label2", "total"], sql: "SELECT COALESCE(f.nama, 'Belum Diatur') as label1, COALESCE(j.nama, 'Belum Diatur') as label2, COUNT(p.id) as total " + basePtmJoinSql + " {FILTER} GROUP BY f.nama, j.nama ORDER BY f.nama ASC, j.nama ASC" },
    "2": { title: "Sebaran Pertemuan per Tahun Akademik & Semester", columns: ["Tahun Akademik", "Semester", "Total Pertemuan"], keys: ["label1", "label2", "total"], sql: "SELECT COALESCE(p.ta, '-') as label1, COALESCE(p.smt, '-') as label2, COUNT(p.id) as total " + basePtmJoinSql + " {FILTER} GROUP BY p.ta, p.smt ORDER BY p.ta DESC, p.smt ASC" },
    "3": { title: "Sebaran Pertemuan per Ruangan", columns: ["Nama Ruangan", "Total Pertemuan"], keys: ["label1", "total"], sql: "SELECT COALESCE(r.nama, 'Tanpa Ruangan') as label1, COUNT(p.id) as total " + basePtmJoinSql + " {FILTER} GROUP BY r.nama ORDER BY r.nama ASC" },
    "4": { title: "Sebaran Pertemuan per Tanggal", columns: ["Tanggal (Hari)", "Total Pertemuan"], keys: ["label1", "total"], sql: "SELECT COALESCE(CAST(p.tanggal AS VARCHAR), 'Belum Diatur') as label1, COUNT(p.id) as total " + basePtmJoinSql + " {FILTER} GROUP BY p.tanggal ORDER BY p.tanggal DESC" },
    "5": { title: "Sebaran Pertemuan per Jam (Mulai - Selesai)", columns: ["Waktu Mulai - Selesai", "Total Pertemuan"], keys: ["label1", "total"], sql: "SELECT COALESCE(p.waktu_mulai, '-') || ' - ' || COALESCE(p.waktu_selesai, '-') as label1, COUNT(p.id) as total " + basePtmJoinSql + " {FILTER} GROUP BY p.waktu_mulai, p.waktu_selesai ORDER BY p.waktu_mulai ASC" },
    "6": { title: "Sebaran Status Dikunci / Finalisasi", columns: ["Status Dikunci", "Total Pertemuan"], keys: ["label1", "total"], sql: "SELECT CASE WHEN p.dikunci IS NOT NULL THEN 'Sudah Final (Dikunci)' ELSE 'Masih Terbuka' END as label1, COUNT(p.id) as total " + basePtmJoinSql + " {FILTER} GROUP BY (p.dikunci IS NOT NULL)" },

    "7": { title: "Total Mengajar/Kehadiran per Dosen", columns: ["Nama Dosen", "Total Kehadiran"], keys: ["label1", "total"], sql: "SELECT d.nama as label1, COUNT(p.id) as total " + baseDsnJoinSql + " {FILTER} GROUP BY d.nama ORDER BY total DESC, d.nama ASC" },
    "8": { title: "Total Kehadiran per Mahasiswa", columns: ["Nama Mahasiswa", "Total Kehadiran"], keys: ["label1", "total"], sql: "SELECT m.nama as label1, COUNT(p.id) as total " + baseMhsJoinSql + " {FILTER} GROUP BY m.nama ORDER BY total DESC, m.nama ASC" },
    "9": { title: "Sebaran Ruangan yang Digunakan Dosen", columns: ["Nama Dosen", "Ruangan", "Total Pertemuan"], keys: ["label1", "label2", "total"], sql: "SELECT d.nama as label1, COALESCE(r.nama, 'Tanpa Ruangan') as label2, COUNT(p.id) as total " + baseDsnJoinSql + " {FILTER} GROUP BY d.nama, r.nama ORDER BY d.nama ASC" },
    "10": { title: "Sebaran Tanggal Kehadiran Mahasiswa", columns: ["Nama Mahasiswa", "Tanggal Hadir", "Jumlah Sesi"], keys: ["label1", "label2", "total"], sql: "SELECT m.nama as label1, COALESCE(CAST(p.tanggal AS VARCHAR), 'Belum Diatur') as label2, COUNT(p.id) as total " + baseMhsJoinSql + " {FILTER} GROUP BY m.nama, p.tanggal ORDER BY m.nama ASC, p.tanggal DESC" }
};

const fetchDataAPI<%=rnd%> = async (sql) => {
    try {
        const res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ sql: sql, action: "sql", tanpaLogin: "true" }) });
        const result = await res.json(); return result.data || [];
    } catch (e) { return []; }
};

const getFilterSql<%=rnd%> = () => {
    let filterSql = "";
    const fakId = document.getElementById('filterFakultasStat<%=rnd%>').value;
    const prodiId = document.getElementById('filterProdiStat<%=rnd%>').value;
    const ta = document.getElementById('filterTahun<%=rnd%>').value;
    const smt = document.getElementById('filterSemester<%=rnd%>').value;
    const dsn = document.getElementById('filterDosenStat<%=rnd%>').value;
    const mhs = document.getElementById('filterMhsStat<%=rnd%>').value;

    if (fakId) filterSql += " AND f.id = " + fakId + " ";
    if (prodiId) filterSql += " AND j.id = " + prodiId + " ";
    if (ta) filterSql += " AND p.ta = '" + ta.replace(/'/g, "''") + "' ";
    if (smt) filterSql += " AND p.smt = '" + smt.replace(/'/g, "''") + "' ";
    
    const tipeLpr = parseInt(document.getElementById("jenisLaporan<%=rnd%>").value);
    
    if (dsn) {
        if (tipeLpr === 7 || tipeLpr === 9) { filterSql += " AND d.nama ILIKE '%" + dsn.replace(/'/g, "''") + "%' "; } 
        else { filterSql += " AND EXISTS (SELECT 1 FROM dosen dx WHERE p.dosens LIKE '%,' || CAST(dx.id AS VARCHAR) || ',%' AND dx.nama ILIKE '%" + dsn.replace(/'/g, "''") + "%') "; }
    }
    
    if (mhs) {
        if (tipeLpr === 8 || tipeLpr === 10) { filterSql += " AND m.nama ILIKE '%" + mhs.replace(/'/g, "''") + "%' "; } 
        else { filterSql += " AND EXISTS (SELECT 1 FROM mahasiswa mx WHERE p.mahasiswas LIKE '%,' || CAST(mx.id AS VARCHAR) || ',%' AND mx.nama ILIKE '%" + mhs.replace(/'/g, "''") + "%') "; }
    }
    return filterSql;
};

const loadStatistik<%=rnd%> = async () => {
    const selector = document.getElementById("jenisLaporan<%=rnd%>").value;
    currentConfig<%=rnd%> = reportConfigs<%=rnd%>[selector];
    document.getElementById("judulTabel<%=rnd%>").innerText = currentConfig<%=rnd%>.title;
    document.getElementById("tbodyRekap<%=rnd%>").innerHTML = "";
    document.getElementById("loaderStatistik<%=rnd%>").classList.remove("d-none");
    
    const finalSql = currentConfig<%=rnd%>.sql.replace(/{FILTER}/g, getFilterSql<%=rnd%>());
    currentData<%=rnd%> = await fetchDataAPI<%=rnd%>(finalSql);
    currentPage<%=rnd%> = 1;
    document.getElementById("loaderStatistik<%=rnd%>").classList.add("d-none");
    
    renderHeaderTabel<%=rnd%>(); renderTabel<%=rnd%>(); renderChart<%=rnd%>();
};

const renderHeaderTabel<%=rnd%> = () => {
    let html = "<tr><th class='text-center' style='width: 50px;'><%=Common.getBahasaConfig("No")%></th>";
    currentConfig<%=rnd%>.columns.forEach(col => { html += "<th>" + col + "</th>"; });
    html += "</tr>"; document.getElementById("theadRekap<%=rnd%>").innerHTML = html;
};

const renderTabel<%=rnd%> = () => {
    const tbody = document.getElementById("tbodyRekap<%=rnd%>");
    if(currentData<%=rnd%>.length === 0) {
        tbody.innerHTML = "<tr><td colspan='" + (currentConfig<%=rnd%>.columns.length + 1) + "' class='text-center text-muted p-4'>Tidak ada data ditemukan</td></tr>";
        document.getElementById("infoPaginasi<%=rnd%>").innerText = "Menampilkan 0 data"; document.getElementById("paginasiTabel<%=rnd%>").innerHTML = ""; return;
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
        }); html += "</tr>";
    });
    tbody.innerHTML = html; renderPaginasi<%=rnd%>(startIdx, endIdx, currentData<%=rnd%>.length);
};

const renderPaginasi<%=rnd%> = (start, end, total) => {
    document.getElementById("infoPaginasi<%=rnd%>").innerText = "Menampilkan " + (start + 1) + " - " + end + " dari " + total + " data";
    const tPages = Math.ceil(total / itemsPerPage<%=rnd%>);
    let html = "<li class='page-item " + (currentPage<%=rnd%> === 1 ? "disabled" : "") + "'><button class='page-link' onclick='ubahHalaman<%=rnd%>(" + (currentPage<%=rnd%> - 1) + ")'><i class='fas fa-angle-left'></i></button></li>";
    for(let i = 1; i <= tPages; i++) {
        if(i === 1 || i === tPages || (i >= currentPage<%=rnd%> - 1 && i <= currentPage<%=rnd%> + 1)) {
            html += "<li class='page-item " + (currentPage<%=rnd%> === i ? "active" : "") + "'><button class='page-link' onclick='ubahHalaman<%=rnd%>(" + i + ")'>" + i + "</button></li>";
        } else if (i === currentPage<%=rnd%> - 2 || i === currentPage<%=rnd%> + 2) { html += "<li class='page-item disabled'><span class='page-link'>...</span></li>"; }
    }
    html += "<li class='page-item " + (currentPage<%=rnd%> === tPages ? "disabled" : "") + "'><button class='page-link' onclick='ubahHalaman<%=rnd%>(" + (currentPage<%=rnd%> + 1) + ")'><i class='fas fa-angle-right'></i></button></li>";
    document.getElementById("paginasiTabel<%=rnd%>").innerHTML = html;
};

const ubahHalaman<%=rnd%> = (page) => { currentPage<%=rnd%> = page; renderTabel<%=rnd%>(); };

const renderChart<%=rnd%> = () => {
    const target = document.getElementById('chartPertemuan<%=rnd%>');
    if (!target) return;
    let labels = [], totals = [];
    currentData<%=rnd%>.slice(0, 20).forEach(row => {
        let lbl = ""; currentConfig<%=rnd%>.keys.forEach(k => { if(k !== 'total') lbl += row[k] + " "; });
        labels.push(lbl.trim()); totals.push(row['total']);
    });
    let max = 0; totals.forEach(v => { const n = parseFloat(v) || 0; if(n > max) max = n; });
    let html = "<div class='el-css-chart'>";
    labels.forEach((label, idx) => {
        const raw = parseFloat(totals[idx]) || 0;
        const width = max > 0 ? Math.max(3, (raw / max) * 100) : 0;
        html += "<div class='el-css-chart-row'><div class='el-css-chart-label' title='" + String(label).replace(/'/g, "&#39;") + "'>" + label + "</div>";
        html += "<div class='el-css-chart-track'><div class='el-css-chart-fill' style='width:" + width + "%'></div></div>";
        html += "<div class='el-css-chart-value'>" + raw + "</div></div>";
    });
    html += "</div>";
    target.innerHTML = html;
};

const downloadExcel<%=rnd%> = () => {
    if(!currentData<%=rnd%> || currentData<%=rnd%>.length === 0) return;
    let csv = "\uFEFF" + currentConfig<%=rnd%>.columns.join(";") + "\n";
    currentData<%=rnd%>.forEach(row => {
        let rowArr = []; currentConfig<%=rnd%>.keys.forEach(k => { rowArr.push('"' + (row[k] !== null ? String(row[k]).replace(/"/g, '""') : "-") + '"'); });
        csv += rowArr.join(";") + "\n";
    });
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' }); const link = document.createElement("a"); link.href = URL.createObjectURL(blob);
    link.download = "Statistik_Pertemuan_" + new Date().getTime() + ".csv"; document.body.appendChild(link); link.click(); document.body.removeChild(link);
};

// ==========================================
// MODAL POPUP DETAIL MENGGUNAKAN MAP ALIAS
// ==========================================
let detailDataCache<%=rnd%> = [];
let detailCurrentPage<%=rnd%> = 1;
let globalTipeDetail<%=rnd%> = "pertemuan";

const safeSqlStr<%=rnd%> = (val) => {
    if(val === '-' || val === 'Belum Diatur' || val === 'Tanpa Ruangan') return " IS NULL ";
    return " = '" + val.toString().replace(/'/g, "''") + "' ";
};

const bukaDetailStatistik<%=rnd%> = async (dataIndex, event) => {
    const row = currentData<%=rnd%>[dataIndex];
    const tipe = parseInt(document.getElementById("jenisLaporan<%=rnd%>").value);
    const btn = event.currentTarget; const oriContent = btn.innerHTML;
    btn.innerHTML = "<span class='spinner-border spinner-border-sm'></span>"; btn.disabled = true;

    const globalFilter = getFilterSql<%=rnd%>();
    if (tipe === 7 || tipe === 9) globalTipeDetail<%=rnd%> = "dosen";
    else if (tipe === 8 || tipe === 10) globalTipeDetail<%=rnd%> = "mahasiswa";
    else globalTipeDetail<%=rnd%> = "pertemuan";
    
    let sqlJson = ""; let sqlHtml = "";

    if (globalTipeDetail<%=rnd%> === "pertemuan") {
        sqlJson = "SELECT p.ta as ta, p.smt as smt, p.topik as topik, p.catatan as catatan, p.indikator as indikator, CAST(p.tanggal AS VARCHAR) as tanggal, p.waktu_mulai as waktu_mulai, p.waktu_selesai as waktu_selesai " + basePtmJoinSql + globalFilter;
        sqlHtml = sqlJson;
        
        if(tipe == 1) sqlHtml += " AND f.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND j.nama " + safeSqlStr<%=rnd%>(row.label2);
        if(tipe == 2) sqlHtml += " AND p.ta " + safeSqlStr<%=rnd%>(row.label1) + " AND p.smt " + safeSqlStr<%=rnd%>(row.label2);
        if(tipe == 3) sqlHtml += " AND r.nama " + safeSqlStr<%=rnd%>(row.label1);
        if(tipe == 4) sqlHtml += " AND CAST(p.tanggal AS VARCHAR) " + safeSqlStr<%=rnd%>(row.label1);
        if(tipe == 5) sqlHtml += " AND (COALESCE(p.waktu_mulai, '-') || ' - ' || COALESCE(p.waktu_selesai, '-')) " + safeSqlStr<%=rnd%>(row.label1);
        if(tipe == 6) sqlHtml += (row.label1 === 'Sudah Final (Dikunci)' ? " AND p.dikunci IS NOT NULL " : " AND p.dikunci IS NULL ");
        
        sqlJson = sqlHtml + " ORDER BY p.tanggal DESC, p.waktu_mulai ASC";
        
    } else if (globalTipeDetail<%=rnd%> === "dosen") {
        sqlJson = "SELECT d.nama as nama_dsn, p.ta as ta, p.smt as smt, p.topik as topik, p.catatan as catatan, p.indikator as indikator, CAST(p.tanggal AS VARCHAR) as tanggal, p.waktu_mulai as waktu_mulai, p.waktu_selesai as waktu_selesai " + baseDsnJoinSql + globalFilter;
        sqlHtml = sqlJson;
        
        if(tipe == 7) sqlHtml += " AND d.nama " + safeSqlStr<%=rnd%>(row.label1);
        if(tipe == 9) sqlHtml += " AND d.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND r.nama " + safeSqlStr<%=rnd%>(row.label2);
        
        sqlJson = sqlHtml + " ORDER BY d.nama ASC, p.tanggal DESC";
        
    } else {
        sqlJson = "SELECT m.nama as nama_mhs, p.ta as ta, p.smt as smt, p.topik as topik, p.catatan as catatan, p.indikator as indikator, CAST(p.tanggal AS VARCHAR) as tanggal, p.waktu_mulai as waktu_mulai, p.waktu_selesai as waktu_selesai " + baseMhsJoinSql + globalFilter;
        sqlHtml = sqlJson;
        
        if(tipe == 8) sqlHtml += " AND m.nama " + safeSqlStr<%=rnd%>(row.label1);
        if(tipe == 10) sqlHtml += " AND m.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND CAST(p.tanggal AS VARCHAR) " + safeSqlStr<%=rnd%>(row.label2);
        
        sqlJson = sqlHtml + " ORDER BY m.nama ASC, p.tanggal DESC";
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
    if (globalTipeDetail<%=rnd%> === "dosen") {
        theadHtml = "<tr><th class='text-center py-3' style='width: 50px;'>No</th><th class='py-3'>Nama Dosen</th><th class='text-center py-3'>Tahun & Smt</th><th class='py-3'>Topik</th><th class='py-3'>Catatan</th><th class='py-3'>Indikator</th><th class='text-center py-3'>Tanggal</th><th class='text-center py-3'>Jam Pertemuan</th></tr>";
    } else if (globalTipeDetail<%=rnd%> === "mahasiswa") {
        theadHtml = "<tr><th class='text-center py-3' style='width: 50px;'>No</th><th class='py-3'>Nama Mahasiswa</th><th class='text-center py-3'>Tahun & Smt</th><th class='py-3'>Topik</th><th class='py-3'>Catatan</th><th class='py-3'>Indikator</th><th class='text-center py-3'>Tanggal</th><th class='text-center py-3'>Jam Pertemuan</th></tr>";
    } else {
        theadHtml = "<tr><th class='text-center py-3' style='width: 50px;'>No</th><th class='text-center py-3'>Tahun & Smt</th><th class='py-3'>Topik</th><th class='py-3'>Catatan</th><th class='py-3'>Indikator</th><th class='text-center py-3'>Tanggal</th><th class='text-center py-3'>Jam Pertemuan</th></tr>";
    }

    const modalHtml = "<div class='modal fade' id='" + modalId + "' tabindex='-1' aria-hidden='true' data-bs-backdrop='static'><div class='modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable'><div class='modal-content shadow-lg border-0 rounded-4'><div class='modal-header bg-light border-0 py-3'><h5 class='modal-title fw-bold text-dark'><i class='fas fa-list-alt text-primary me-2'></i>" + title + "</h5><div><button class='btn btn-sm btn-success rounded-pill fw-bold shadow-sm me-2 px-3' onclick='downloadDetailExcel<%=rnd%>()'><i class='fas fa-file-excel me-1'></i>Unduh Excel</button><button type='button' class='btn-close' data-bs-dismiss='modal' aria-label='Close'></button></div></div><div class='modal-body p-0'><div class='table-responsive'><table class='table table-hover table-striped align-middle mb-0' style='font-size: 0.9rem;'><thead class='bg-primary text-white'>" + theadHtml + "</thead><tbody id='tbodyDetail<%=rnd%>'></tbody></table></div></div><div class='modal-footer bg-white border-top p-3 d-flex justify-content-between align-items-center'><span class='small text-muted fw-bold' id='infoPaginasiDetail<%=rnd%>'></span><nav><ul class='pagination pagination-sm mb-0 shadow-sm' id='paginasiDetail<%=rnd%>'></ul></nav></div></div></div></div>";
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    const modalEl = new bootstrap.Modal(document.getElementById(modalId)); modalEl.show();
    document.getElementById(modalId).addEventListener('hidden.bs.modal', function () { this.remove(); });
    renderTabelDetail<%=rnd%>();
};

const renderTabelDetail<%=rnd%> = async () => {
    const tbody = document.getElementById("tbodyDetail<%=rnd%>");
    let colSpan = globalTipeDetail<%=rnd%> === "pertemuan" ? 7 : 8;
    if(detailDataCache<%=rnd%>.length === 0) {
        tbody.innerHTML = "<tr><td colspan='" + colSpan + "' class='text-center text-muted p-4'>Tidak ada data rincian ditemukan</td></tr>";
        document.getElementById("infoPaginasiDetail<%=rnd%>").innerText = "Menampilkan 0 data"; document.getElementById("paginasiDetail<%=rnd%>").innerHTML = ""; return;
    }
    tbody.innerHTML = "<tr><td colspan='" + colSpan + "' class='text-center p-4'><span class='spinner-border text-primary'></span></td></tr>";
    const startIdx = (detailCurrentPage<%=rnd%> - 1) * 10;
    const endIdx = Math.min(startIdx + 10, detailDataCache<%=rnd%>.length);
    const formData = new URLSearchParams(); formData.append("action_jsp", "get_html_detail"); formData.append("sql", window.currentDetailSqlHtml<%=rnd%>); formData.append("tipe_detail", globalTipeDetail<%=rnd%>); formData.append("start", startIdx); formData.append("limit", 10);
    try {
        const res = await fetch('<%=Common.ROOT%>/dsh?hanya_tampil_jsp=true&p=elearning&s=_statistik_pertemuan&baru=true&rnd=<%=rnd%>', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: formData.toString() });
        tbody.innerHTML = await res.text();
    } catch (e) { tbody.innerHTML = "<tr><td colspan='" + colSpan + "' class='text-center text-danger p-3'>Gagal menarik data detail.</td></tr>"; }
    document.getElementById("infoPaginasiDetail<%=rnd%>").innerText = "Menampilkan " + (startIdx + 1) + " - " + endIdx + " dari " + detailDataCache<%=rnd%>.length;
    const tPages = Math.ceil(detailDataCache<%=rnd%>.length / 10);
    let htmlPg = "<li class='page-item " + (detailCurrentPage<%=rnd%> === 1 ? "disabled" : "") + "'><button class='page-link' onclick='ubahHalamanDetail<%=rnd%>(" + (detailCurrentPage<%=rnd%> - 1) + ")'><i class='fas fa-angle-left'></i></button></li>";
    for(let i = 1; i <= tPages; i++) {
        if(i === 1 || i === tPages || (i >= detailCurrentPage<%=rnd%> - 1 && i <= detailCurrentPage<%=rnd%> + 1)) {
            htmlPg += "<li class='page-item " + (detailCurrentPage<%=rnd%> === i ? "active" : "") + "'><button class='page-link' onclick='ubahHalamanDetail<%=rnd%>(" + i + ")'>" + i + "</button></li>";
        } else if (i === detailCurrentPage<%=rnd%> - 2 || i === detailCurrentPage<%=rnd%> + 2) { htmlPg += "<li class='page-item disabled'><span class='page-link'>...</span></li>"; }
    }
    htmlPg += "<li class='page-item " + (detailCurrentPage<%=rnd%> === tPages ? "disabled" : "") + "'><button class='page-link' onclick='ubahHalamanDetail<%=rnd%>(" + (detailCurrentPage<%=rnd%> + 1) + ")'><i class='fas fa-angle-right'></i></button></li>";
    document.getElementById("paginasiDetail<%=rnd%>").innerHTML = htmlPg;
};

const ubahHalamanDetail<%=rnd%> = (page) => { detailCurrentPage<%=rnd%> = page; renderTabelDetail<%=rnd%>(); };

const downloadDetailExcel<%=rnd%> = () => {
    if(!detailDataCache<%=rnd%> || detailDataCache<%=rnd%>.length === 0) return; let csv = "\uFEFF";
    
    if (globalTipeDetail<%=rnd%> === "dosen") {
        csv += "No;Nama Dosen;Tahun;Semester;Topik;Catatan;Indikator;Tanggal;Jam Pertemuan\\n";
        detailDataCache<%=rnd%>.forEach((row, i) => {
            let cols = [ i+1, row.nama_dsn || "-", row.ta || "-", row.smt || "-", row.topik || "-", row.catatan || "-", row.indikator || "-", row.tanggal || "-", (row.waktu_mulai || "") + " - " + (row.waktu_selesai || "") ];
            csv += cols.map(v => '"' + String(v).replace(/"/g, '""') + '"').join(";") + "\n";
        });
    } else if (globalTipeDetail<%=rnd%> === "mahasiswa") {
        csv += "No;Nama Mahasiswa;Tahun;Semester;Topik;Catatan;Indikator;Tanggal;Jam Pertemuan\\n";
        detailDataCache<%=rnd%>.forEach((row, i) => {
            let cols = [ i+1, row.nama_mhs || "-", row.ta || "-", row.smt || "-", row.topik || "-", row.catatan || "-", row.indikator || "-", row.tanggal || "-", (row.waktu_mulai || "") + " - " + (row.waktu_selesai || "") ];
            csv += cols.map(v => '"' + String(v).replace(/"/g, '""') + '"').join(";") + "\n";
        });
    } else {
        csv += "No;Tahun;Semester;Topik;Catatan;Indikator;Tanggal;Jam Pertemuan\\n";
        detailDataCache<%=rnd%>.forEach((row, i) => {
            let cols = [ i+1, row.ta || "-", row.smt || "-", row.topik || "-", row.catatan || "-", row.indikator || "-", row.tanggal || "-", (row.waktu_mulai || "") + " - " + (row.waktu_selesai || "") ];
            csv += cols.map(v => '"' + String(v).replace(/"/g, '""') + '"').join(";") + "\n";
        });
    }
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' }); const link = document.createElement("a"); link.href = URL.createObjectURL(blob);
    link.download = "Detail_Data_Pertemuan_" + new Date().getTime() + ".csv"; document.body.appendChild(link); link.click(); document.body.removeChild(link);
};

loadStatistik<%=rnd%>();
</script>