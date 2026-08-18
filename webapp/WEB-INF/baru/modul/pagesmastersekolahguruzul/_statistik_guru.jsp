<%@page import="ais.database.model.sekolah.Guru"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.sekolah.Yayasan"%>
<%@page import="ais.database.model.StatusPegawai"%>
<%@page import="ais.database.model.StatusKepegawaian"%>
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
    // Prefix unik untuk modul Guru
    rnd = "Gru" + rnd; 

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
            List<Guru> list = sess.createSQLQuery(sql)
                                  .addEntity(Guru.class)
                                  .setFirstResult(start)
                                  .setMaxResults(limit)
                                  .list();
            for (int i = 0; i < list.size(); i++) {
                Guru g = list.get(i);

                String nip = g.getNip() != null && !g.getNip().trim().isEmpty() ? g.getNip() : (g.getNuptk() != null ? g.getNuptk() : "-");
                String nama = g.getNama() != null ? g.getNama() : "-";
                String sekolah = g.getSekolah() != null ? g.getSekolah().getNama() : "-";
                String pendidikan = g.getPendidikan() != null ? g.getPendidikan().getNama() : "-";
                String jenisGuru = g.getJenisGuru() != null ? g.getJenisGuru().toString() : "-";
                String statusPgw = g.getStatusPegawai() != null ? g.getStatusPegawai().getNama() : "-";
                
                String urlFoto = Common.ROOT + "/img/administrator-icon_default.png";
                try {
                    java.util.Map<String, String> mapParams = new java.util.HashMap<String, String>();
                    g.putPhoto(mapParams);
                    if(mapParams.containsKey("foto") && mapParams.get("foto") != null && !mapParams.get("foto").contains("administrator-icon_default.png")){
                        urlFoto = Common.ROOT + "/file?file=" + mapParams.get("foto");
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersekolahguruzul/_statistik_guru.jsp:59");}
                
                %>
                <tr>
                    <td class='text-center'><%= (start + i + 1) %></td>
                    <td class='text-center'>
                        <img src="<%= urlFoto %>" class="rounded-circle img-thumbnail shadow-sm p-1" style="width: 40px; height: 40px; object-fit: cover;">
                    </td>
                    <td class='fw-bold text-primary'><%= nip %></td>
                    <td class='fw-bold'><%= nama %></td>
                    <td><%= sekolah %></td>
                    <td><%= pendidikan %></td>
                    <td><%= jenisGuru %></td>
                    <td class='text-center'><span class="badge bg-info text-dark"><%= statusPgw %></span></td>
                </tr>
                <%
            }
        } catch(Exception e) {
            out.print("<tr><td colspan='8' class='text-center text-danger'>" + Common.getBahasaConfig("Terjadi kesalahan kueri data rincian guru.") + "</td></tr>");
        } finally {
            try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersekolahguruzul/_statistik_guru.jsp:79");}
            try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersekolahguruzul/_statistik_guru.jsp:80");}
            HibernateUtil.closeSession();
        }
        return; 
    }

    // =========================================================================
    // BLOK HALAMAN UTAMA STATISTIK GURU
    // =========================================================================
    List<Yayasan> listYayasan = new ArrayList<Yayasan>();
    List<Sekolah> listSekolah = new ArrayList<Sekolah>();
    List<StatusPegawai> listStatusPegawai = new ArrayList<StatusPegawai>();
    List<StatusKepegawaian> listStatusKepegawaian = new ArrayList<StatusKepegawaian>();
    
    Session sessFilter = HibernateUtil.openSession();
    try {
        listYayasan = ConstantValues.simpleList(sessFilter.createCriteria(Yayasan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Yayasan.class);
        listSekolah = ConstantValues.simpleList(sessFilter.createCriteria(Sekolah.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Sekolah.class);
        listStatusPegawai = ConstantValues.simpleList(sessFilter.createCriteria(StatusPegawai.class).addOrder(Order.asc("nama")), StatusPegawai.class);
        listStatusKepegawaian = ConstantValues.simpleList(sessFilter.createCriteria(StatusKepegawaian.class).addOrder(Order.asc("nama")), StatusKepegawaian.class);
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pagesmastersekolahguruzul/_statistik_guru.jsp:101");
    } finally {
        try { sessFilter.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersekolahguruzul/_statistik_guru.jsp:103");}
        try { sessFilter.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersekolahguruzul/_statistik_guru.jsp:104");}
        HibernateUtil.closeSession();
    }
%>

<div class="container-fluid p-0 animate__animated animate__fadeIn">
    <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3">
        <h5 class="mb-0 fw-bold text-primary">
            <i class="fas fa-chalkboard-teacher me-2"></i><%= Common.getBahasaConfig("Dashboard Statistik Tenaga Pendidik & Guru") %>
        </h5>
    </div>

    <div class="card shadow-sm border-0 rounded-4 mb-4">
        <div class="card-body bg-light rounded-4 p-4">
            <div class="row g-3">
                <div class="col-md-12 mb-2">
                    <label class="form-label fw-bold text-secondary small"><i class="fas fa-list me-1"></i><%= Common.getBahasaConfig("Pilih Jenis Laporan:") %></label>
                    <select class="form-select border-primary shadow-sm fw-bold" id="jenisLaporan<%=rnd%>" onchange="loadStatistik<%=rnd%>()">
                        <optgroup label="<%= Common.getBahasaConfig("Data Akademik & Kepegawaian Guru") %>">
                            <option value="1"><%= Common.getBahasaConfig("1. Sebaran per Sekolah (Homebase)") %></option>
                            <option value="2"><%= Common.getBahasaConfig("2. Sebaran per Yayasan") %></option>
                            <option value="3"><%= Common.getBahasaConfig("3. Sebaran per Status Pegawai (Aktif/Cuti)") %></option>
                            <option value="4"><%= Common.getBahasaConfig("4. Sebaran per Status Kepegawaian (GTY/Honor/PNS)") %></option>
                            <option value="5"><%= Common.getBahasaConfig("5. Sebaran per Golongan Kepangkatan PNS") %></option>
                            <option value="6"><%= Common.getBahasaConfig("6. Sebaran per Jenjang Pendidikan Terakhir") %></option>
                            <option value="7"><%= Common.getBahasaConfig("7. Sebaran per Jenis PTK") %></option>
                            <option value="8"><%= Common.getBahasaConfig("8. Sebaran per Lembaga Pengangkat") %></option>
                            <option value="9"><%= Common.getBahasaConfig("9. Sebaran per Sumber Gaji") %></option>
                        </optgroup>
                        <optgroup label="<%= Common.getBahasaConfig("Data Biodata Guru") %>">
                            <option value="10"><%= Common.getBahasaConfig("10. Sebaran per Jenis Kelamin") %></option>
                            <option value="11"><%= Common.getBahasaConfig("11. Sebaran per Agama") %></option>
                            <option value="12"><%= Common.getBahasaConfig("12. Sebaran per Pekerjaan Ayah") %></option>
                            <option value="13"><%= Common.getBahasaConfig("13. Sebaran per Pekerjaan Ibu") %></option>
                            <option value="14"><%= Common.getBahasaConfig("14. Sebaran per Pekerjaan Suami/Istri") %></option>
                        </optgroup>
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
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-user-tag me-1"></i> <%= Common.getBahasaConfig("Status Pegawai") %></label>
                    <select id="filterStatusPegawaiStat<%=rnd%>" class="form-select form-select-sm border-info shadow-none" onchange="loadStatistik<%=rnd%>()">
                        <option value=""><%= Common.getBahasaConfig("Semua Status Pegawai") %></option>
                        <% for(StatusPegawai sp : listStatusPegawai) { %><option value="<%= sp.getId() %>"><%= sp.getNama() %></option><% } %>
                    </select>
                </div>

                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-briefcase me-1"></i> <%= Common.getBahasaConfig("Status Kepegawaian") %></label>
                    <select id="filterStatusKepegawaianStat<%=rnd%>" class="form-select form-select-sm border-info shadow-none" onchange="loadStatistik<%=rnd%>()">
                        <option value=""><%= Common.getBahasaConfig("Semua Status") %></option>
                        <% for(StatusKepegawaian sk : listStatusKepegawaian) { %><option value="<%= sk.getId() %>"><%= sk.getNama() %></option><% } %>
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
            <div style="position: relative; height: 350px; width: 100%;"><canvas id="chartGuru<%=rnd%>"></canvas></div>
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
// KONFIGURASI SQL REKAPITULASI GURU
// ==========================================
const baseJoinSql = " FROM sekolah.guru g LEFT JOIN sekolah.sekolah s ON g.sekolah_id = s.id LEFT JOIN sekolah.yayasan y ON g.yayasan_id = y.id LEFT JOIN status_pegawai sp ON g.status_pegawai = sp.id LEFT JOIN status_kepegawaian sk ON g.status_kepegawaian = sk.id ";

const reportConfigs<%=rnd%> = {
    "1": {
        title: "<%= Common.getBahasaConfig("Sebaran per Sekolah (Homebase)") %>",
        columns: ["Sekolah", "Total Guru"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(s.nama, 'Belum Diatur') as label1, COUNT(g.id) as total " + baseJoinSql + " WHERE g.id IS NOT NULL {FILTER} GROUP BY s.nama ORDER BY s.nama ASC"
    },
    "2": {
        title: "<%= Common.getBahasaConfig("Sebaran per Yayasan") %>",
        columns: ["Yayasan", "Total Guru"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(y.nama, 'Belum Diatur') as label1, COUNT(g.id) as total " + baseJoinSql + " WHERE g.id IS NOT NULL {FILTER} GROUP BY y.nama ORDER BY y.nama ASC"
    },
    "3": {
        title: "<%= Common.getBahasaConfig("Sebaran per Status Pegawai") %>",
        columns: ["Status Pegawai", "Total Guru"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(sp.nama, 'Belum Diatur') as label1, COUNT(g.id) as total " + baseJoinSql + " WHERE g.id IS NOT NULL {FILTER} GROUP BY sp.nama ORDER BY sp.nama ASC"
    },
    "4": {
        title: "<%= Common.getBahasaConfig("Sebaran per Status Kepegawaian") %>",
        columns: ["Status Kepegawaian", "Total Guru"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(sk.nama, 'Belum Diatur') as label1, COUNT(g.id) as total " + baseJoinSql + " WHERE g.id IS NOT NULL {FILTER} GROUP BY sk.nama ORDER BY sk.nama ASC"
    },
    "5": {
        title: "<%= Common.getBahasaConfig("Sebaran per Golongan Kepangkatan PNS") %>",
        columns: ["Golongan PNS", "Total Guru"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(gpns.nama, 'Tidak Ada Golongan/Bukan PNS') as label1, COUNT(g.id) as total " + baseJoinSql + " LEFT JOIN golongan_pns gpns ON g.golongan_pns = gpns.id WHERE g.id IS NOT NULL {FILTER} GROUP BY gpns.nama ORDER BY gpns.nama ASC"
    },
    "6": {
        title: "<%= Common.getBahasaConfig("Sebaran per Jenjang Pendidikan Terakhir") %>",
        columns: ["Pendidikan Terakhir", "Total Guru"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(p.nama, 'Tidak Diketahui') as label1, COUNT(g.id) as total " + baseJoinSql + " LEFT JOIN employ.pendidikan p ON g.pendidikan = p.id WHERE g.id IS NOT NULL {FILTER} GROUP BY p.nama ORDER BY p.nama ASC"
    },
    "7": {
        title: "<%= Common.getBahasaConfig("Sebaran per Jenis Pendidik & Tenaga Kependidikan") %>",
        columns: ["Jenis PTK", "Total Guru"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(jp.nama, 'Belum Diatur') as label1, COUNT(g.id) as total " + baseJoinSql + " LEFT JOIN jenis_pendidik_dan_tenaga_kependidikan jp ON g.jenis_pendidik_dan_tenaga_kependidikan = jp.id WHERE g.id IS NOT NULL {FILTER} GROUP BY jp.nama ORDER BY jp.nama ASC"
    },
    "8": {
        title: "<%= Common.getBahasaConfig("Sebaran per Lembaga Pengangkat") %>",
        columns: ["Lembaga Pengangkat", "Total Guru"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(lp.nama, 'Belum Diatur') as label1, COUNT(g.id) as total " + baseJoinSql + " LEFT JOIN lembaga_pengangkat lp ON g.lembaga_pengangkat = lp.id WHERE g.id IS NOT NULL {FILTER} GROUP BY lp.nama ORDER BY lp.nama ASC"
    },
    "9": {
        title: "<%= Common.getBahasaConfig("Sebaran per Sumber Gaji") %>",
        columns: ["Sumber Gaji", "Total Guru"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(sg.nama, 'Belum Diatur') as label1, COUNT(g.id) as total " + baseJoinSql + " LEFT JOIN sumber_gaji sg ON g.sumber_gaji = sg.id WHERE g.id IS NOT NULL {FILTER} GROUP BY sg.nama ORDER BY sg.nama ASC"
    },
    "10": {
        title: "<%= Common.getBahasaConfig("Sebaran per Jenis Kelamin") %>",
        columns: ["Jenis Kelamin", "Total Guru"], keys: ["label1", "total"],
        sql: "SELECT CASE WHEN g.jenis_kelamin IS NULL OR g.jenis_kelamin = '' THEN 'Tidak Diketahui' ELSE g.jenis_kelamin END as label1, COUNT(g.id) as total " + baseJoinSql + " WHERE g.id IS NOT NULL {FILTER} GROUP BY g.jenis_kelamin"
    },
    "11": {
        title: "<%= Common.getBahasaConfig("Sebaran per Agama") %>",
        columns: ["Agama", "Total Guru"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(a.nama, 'Tidak Diketahui') as label1, COUNT(g.id) as total " + baseJoinSql + " LEFT JOIN agama a ON g.agama_id = a.id WHERE g.id IS NOT NULL {FILTER} GROUP BY a.nama ORDER BY a.nama ASC"
    },
    "12": {
        title: "<%= Common.getBahasaConfig("Sebaran per Pekerjaan Ayah") %>",
        columns: ["Pekerjaan Ayah", "Total Guru"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(pa.nama, 'Tidak Diketahui') as label1, COUNT(g.id) as total " + baseJoinSql + " LEFT JOIN pekerjaan pa ON g.id_pekerjaan_ayah = pa.id WHERE g.id IS NOT NULL {FILTER} GROUP BY pa.nama ORDER BY pa.nama ASC"
    },
    "13": {
        title: "<%= Common.getBahasaConfig("Sebaran per Pekerjaan Ibu") %>",
        columns: ["Pekerjaan Ibu", "Total Guru"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(pi.nama, 'Tidak Diketahui') as label1, COUNT(g.id) as total " + baseJoinSql + " LEFT JOIN pekerjaan pi ON g.id_pekerjaan_ibu = pi.id WHERE g.id IS NOT NULL {FILTER} GROUP BY pi.nama ORDER BY pi.nama ASC"
    },
    "14": {
        title: "<%= Common.getBahasaConfig("Sebaran per Pekerjaan Suami/Istri") %>",
        columns: ["Pekerjaan Suami/Istri", "Total Guru"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(ps.nama, 'Tidak Diketahui') as label1, COUNT(g.id) as total " + baseJoinSql + " LEFT JOIN pekerjaan ps ON g.pekerjaan_suami_istri = ps.id WHERE g.id IS NOT NULL {FILTER} GROUP BY ps.nama ORDER BY ps.nama ASC"
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
    const spId = document.getElementById('filterStatusPegawaiStat<%=rnd%>').value;
    const skId = document.getElementById('filterStatusKepegawaianStat<%=rnd%>').value;

    if (yayId) filterSql += " AND y.id = " + yayId + " ";
    if (sekId) filterSql += " AND s.id = " + sekId + " ";
    if (spId) filterSql += " AND g.status_pegawai = " + spId + " ";
    if (skId) filterSql += " AND g.status_kepegawaian = " + skId + " ";

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
    const ctx = document.getElementById('chartGuru<%=rnd%>').getContext('2d');
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
        data: { labels: labels, datasets: [{ label: '<%= Common.getBahasaConfigJS("Total Guru") %>', data: totals, backgroundColor: 'rgba(20, 164, 77, 0.7)', borderColor: 'rgba(20, 164, 77, 1)', borderWidth: 1, borderRadius: 4 }] },
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
    link.download = "Statistik_Data_Guru_" + new Date().getTime() + ".csv";
    document.body.appendChild(link); link.click(); document.body.removeChild(link);
};

// ==========================================
// MODAL POPUP DETAIL GURU
// ==========================================
let detailDataCache<%=rnd%> = [];
let detailCurrentPage<%=rnd%> = 1;

const safeSqlStr<%=rnd%> = (val) => {
    if(val === '-' || val === 'Belum Diatur' || val === 'Tidak Diketahui' || val === 'Tidak Ada Golongan/Bukan PNS') return " IS NULL ";
    return " = '" + val.toString().replace(/'/g, "''") + "' ";
};

const bukaDetailStatistik<%=rnd%> = async (dataIndex, event) => {
    const row = currentData<%=rnd%>[dataIndex];
    const tipe = document.getElementById("jenisLaporan<%=rnd%>").value;
    const btn = event.currentTarget;
    const oriContent = btn.innerHTML;
    btn.innerHTML = "<span class='spinner-border spinner-border-sm'></span>"; btn.disabled = true;

    const globalFilter = getFilterSql<%=rnd%>();
    
    let joinExtra = "";
    if (tipe == 5) joinExtra += " LEFT JOIN golongan_pns gpns ON g.golongan_pns = gpns.id ";
    if (tipe == 6) joinExtra += " LEFT JOIN employ.pendidikan p ON g.pendidikan = p.id ";
    if (tipe == 7) joinExtra += " LEFT JOIN jenis_pendidik_dan_tenaga_kependidikan jp ON g.jenis_pendidik_dan_tenaga_kependidikan = jp.id ";
    if (tipe == 8) joinExtra += " LEFT JOIN lembaga_pengangkat lp ON g.lembaga_pengangkat = lp.id ";
    if (tipe == 9) joinExtra += " LEFT JOIN sumber_gaji sg ON g.sumber_gaji = sg.id ";
    if (tipe == 11) joinExtra += " LEFT JOIN agama a ON g.agama_id = a.id ";
    if (tipe == 12) joinExtra += " LEFT JOIN pekerjaan pa ON g.id_pekerjaan_ayah = pa.id ";
    if (tipe == 13) joinExtra += " LEFT JOIN pekerjaan pi ON g.id_pekerjaan_ibu = pi.id ";
    if (tipe == 14) joinExtra += " LEFT JOIN pekerjaan ps ON g.pekerjaan_suami_istri = ps.id ";

    let sqlJson = "SELECT g.nip, g.nama_guru as nama, s.nama as sekolah, p.nama as pendidikan, jg.nama as jenis_guru, sp.nama as status FROM sekolah.guru g LEFT JOIN sekolah.sekolah s ON g.sekolah_id = s.id LEFT JOIN sekolah.yayasan y ON g.yayasan_id = y.id LEFT JOIN status_pegawai sp ON g.status_pegawai = sp.id LEFT JOIN status_kepegawaian sk ON g.status_kepegawaian = sk.id LEFT JOIN sekolah.jenis_guru jg ON g.jenis_guru_id = jg.id LEFT JOIN employ.pendidikan p ON g.pendidikan = p.id " + joinExtra + " WHERE g.id IS NOT NULL " + globalFilter;
    let sqlHtml = "SELECT g.* FROM sekolah.guru g LEFT JOIN sekolah.sekolah s ON g.sekolah_id = s.id LEFT JOIN sekolah.yayasan y ON g.yayasan_id = y.id LEFT JOIN status_pegawai sp ON g.status_pegawai = sp.id LEFT JOIN status_kepegawaian sk ON g.status_kepegawaian = sk.id LEFT JOIN sekolah.jenis_guru jg ON g.jenis_guru_id = jg.id LEFT JOIN employ.pendidikan p ON g.pendidikan = p.id " + joinExtra + " WHERE g.id IS NOT NULL " + globalFilter;
    
    let filter = "";
    if(tipe == 1) filter += " AND s.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 2) filter += " AND y.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 3) filter += " AND sp.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 4) filter += " AND sk.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 5) filter += " AND gpns.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 6) filter += " AND p.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 7) filter += " AND jp.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 8) filter += " AND lp.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 9) filter += " AND sg.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 10) filter += " AND g.jenis_kelamin " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 11) filter += " AND a.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 12) filter += " AND pa.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 13) filter += " AND pi.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 14) filter += " AND ps.nama " + safeSqlStr<%=rnd%>(row.label1);

    sqlJson += filter + " ORDER BY g.nama_guru ASC";
    sqlHtml += filter + " ORDER BY g.nama_guru ASC";

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
                                "<thead class='bg-primary text-white'><tr>" +
                                    "<th class='text-center py-3' style='width: 50px;'>No</th>" +
                                    "<th class='text-center py-3' style='width: 60px;'>Foto</th>" +
                                    "<th class='py-3'>NIP/NUPTK</th><th class='py-3'>Nama Guru</th>" +
                                    "<th class='py-3'>Sekolah</th><th class='py-3'>Pendidikan</th>" +
                                    "<th class='py-3'>Jenis Guru</th><th class='text-center py-3'>Status</th>" +
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
        const res = await fetch('<%=Common.ROOT%>/dsh?hanya_tampil_jsp=true&p=pagesmastersekolahguruzul&s=_statistik_guru&baru=true&rnd=<%=rnd%>', {
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
    let csv = "\uFEFFNo;NIP/NUPTK;Nama Guru;Sekolah;Pendidikan;Jenis Guru;Status\n";
    detailDataCache<%=rnd%>.forEach((row, i) => {
        let cols = [ i+1, row.nip || "-", row.nama || "-", row.prodi || "-", row.pendidikan || "-", row.jenis_guru || "-", row.status || "-" ];
        csv += cols.map(v => '"' + String(v).replace(/"/g, '""') + '"').join(";") + "\n";
    });
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement("a"); link.href = URL.createObjectURL(blob);
    link.download = "Detail_Data_Guru_" + new Date().getTime() + ".csv";
    document.body.appendChild(link); link.click(); document.body.removeChild(link);
};

loadStatistik<%=rnd%>();
</script>