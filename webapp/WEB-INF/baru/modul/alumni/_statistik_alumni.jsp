<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.StatusKeluar"%>
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
                String thnLulus = m.getTahunLulus() != null ? m.getTahunLulus().toString() : "-";
                String prog = m.getProgram() != null ? m.getProgram() : "-";
                String thnMasuk = m.getTahunangkatan() != null ? m.getTahunangkatan().toString() : "-";
                String skripsi = m.getJudulSkripsi() != null ? m.getJudulSkripsi() : "-";
                
                %>
                <tr>
                    <td class='text-center'><%= (start + i + 1) %></td>
                    <td class='text-center'>
                        <img src="<%= urlFotoMhs %>" class="rounded-circle img-thumbnail shadow-sm p-1" style="width: 40px; height: 40px; object-fit: cover;">
                    </td>
                    <td class='fw-bold text-primary'><%= nim %></td>
                    <td class='fw-bold'><%= nama %></td>
                    <td><%= prodi %></td>
                    <td class='text-center'><%= thnLulus %></td>
                    <td class='text-center'><%= prog %></td>
                    <td class='text-center'><%= thnMasuk %></td>
                    <td>
                        <div style="max-width: 250px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;" title="<%= skripsi.replace("\"", "&quot;") %>">
                            <span class="fst-italic text-secondary"><%= skripsi %></span>
                        </div>
                    </td>
                </tr>
                <%
            }
        } catch(Exception e) {
            out.print("<tr><td colspan='9' class='text-center text-danger'>Terjadi kesalahan query data.</td></tr>");
        } finally {
            try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/_statistik_alumni.jsp:76");}
            try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/_statistik_alumni.jsp:77");}
            HibernateUtil.closeSession();
        }
        return; 
    }

    // =========================================================================
    // BLOK HALAMAN UTAMA STATISTIK ALUMNI
    // =========================================================================
    String rnd = request.getParameter("rnd");
    if (rnd == null) rnd = "";
    
    // TAMBAHKAN BARIS INI: Beri awalan unik untuk modul Alumni
    rnd = "Alm" + rnd;

    List<Fakultas> listFakultas = new ArrayList<Fakultas>();
    List<Jurusan> listJurusan = new ArrayList<Jurusan>();
    List<StatusKeluar> listStatusKeluar = new ArrayList<StatusKeluar>();
    
    Session sessFilter = HibernateUtil.openSession();
    try {
        listFakultas = ConstantValues.simpleList(sessFilter.createCriteria(Fakultas.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Fakultas.class);
        listJurusan = ConstantValues.simpleList(sessFilter.createCriteria(Jurusan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Jurusan.class);
        listStatusKeluar = ConstantValues.simpleList(sessFilter.createCriteria(StatusKeluar.class).addOrder(Order.asc("nama")), StatusKeluar.class);
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/alumni/_statistik_alumni.jsp:102");
    } finally {
        try { sessFilter.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/_statistik_alumni.jsp:104");}
        try { sessFilter.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/_statistik_alumni.jsp:105");}
        HibernateUtil.closeSession();
    }
%>

<script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/chart.js"></script>

<div class="container-fluid p-0 animate__animated animate__fadeIn">
    <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3">
        <h5 class="mb-0 fw-bold text-primary">
            <i class="fas fa-chart-bar me-2"></i><%= Common.getBahasaConfig("Dashboard Statistik Alumni & Lulusan") %>
        </h5>
    </div>

    <div class="card shadow-sm border-0 rounded-4 mb-4">
        <div class="card-body bg-light rounded-4 p-4">
            <div class="row g-3">
                <div class="col-md-12 mb-2">
                    <label class="form-label fw-bold text-secondary small"><i class="fas fa-list me-1"></i><%= Common.getBahasaConfig("Pilih Jenis Laporan:") %></label>
                    <select class="form-select border-primary shadow-sm fw-bold" id="jenisLaporan<%=rnd%>" onchange="loadStatistik<%=rnd%>()">
                        <optgroup label="Data Akademik Mahasiswa">
                            <option value="1"><%= Common.getBahasaConfig("1. Sebaran Alumni per Tahun Lulus & Status") %></option>
                            <option value="2"><%= Common.getBahasaConfig("2. Sebaran per Program Studi & Tahun Lulus") %></option>
                            <option value="3"><%= Common.getBahasaConfig("3. Sebaran per Tahun Masuk (Angkatan)") %></option>
                            <option value="4"><%= Common.getBahasaConfig("4. Sebaran per Jenis Kelamin") %></option>
                            <option value="5"><%= Common.getBahasaConfig("5. Sebaran per Program Kelas (Reguler/Eksekutif)") %></option>
                            <option value="6"><%= Common.getBahasaConfig("6. Sebaran per Agama") %></option>
                            <option value="7"><%= Common.getBahasaConfig("7. Sebaran per Status Awal Mahasiswa (Baru/Pindahan)") %></option>
                            <option value="8"><%= Common.getBahasaConfig("8. Sebaran per Jenjang Pendidikan") %></option>
                            <option value="9"><%= Common.getBahasaConfig("9. Rekapitulasi Total per Status Keluar") %></option>
                            <option value="10"><%= Common.getBahasaConfig("10. Sebaran per Fakultas, Prodi, dan Tahun Lulus") %></option>
                        </optgroup>
                        <optgroup label="Data Biodata Mahasiswa & Orang Tua">
                            <option value="11"><%= Common.getBahasaConfig("11. Sebaran per Jenis Pekerjaan Ayah, Prodi, & Thn Lulus") %></option>
                            <option value="12"><%= Common.getBahasaConfig("12. Sebaran per Jenis Pekerjaan Ibu, Prodi, & Thn Lulus") %></option>
                            <option value="13"><%= Common.getBahasaConfig("13. Sebaran per Jenis Pekerjaan Wali, Prodi, & Thn Lulus") %></option>
                            <option value="14"><%= Common.getBahasaConfig("14. Sebaran per Penghasilan Ayah, Prodi, & Thn Lulus") %></option>
                            <option value="15"><%= Common.getBahasaConfig("15. Sebaran per Penghasilan Ibu, Prodi, & Thn Lulus") %></option>
                            <option value="16"><%= Common.getBahasaConfig("16. Sebaran per Penghasilan Wali, Prodi, & Thn Lulus") %></option>
                            <option value="17"><%= Common.getBahasaConfig("17. Sebaran per Jenjang Pendidikan Ayah, Prodi, & Thn Lulus") %></option>
                            <option value="18"><%= Common.getBahasaConfig("18. Sebaran per Jenjang Pendidikan Ibu, Prodi, & Thn Lulus") %></option>
                            <option value="19"><%= Common.getBahasaConfig("19. Sebaran per Jenjang Pendidikan Wali, Prodi, & Thn Lulus") %></option>
                            <option value="20"><%= Common.getBahasaConfig("20. Sebaran per Sekolah Asal (SMA/SMK)") %></option>
                            <option value="21"><%= Common.getBahasaConfig("21. Sebaran per Jenis Tinggal Mahasiswa") %></option>
                            <option value="22"><%= Common.getBahasaConfig("22. Sebaran per Alat Transportasi") %></option>
                            <option value="23"><%= Common.getBahasaConfig("23. Sebaran Kombinasi Pekerjaan (Ayah, Ibu, Wali)") %></option>
                            <option value="24"><%= Common.getBahasaConfig("24. Sebaran Kombinasi Penghasilan (Ayah, Ibu, Wali)") %></option>
                            <option value="25"><%= Common.getBahasaConfig("25. Sebaran Kombinasi Pendidikan (Ayah, Ibu, Wali)") %></option>
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
                
                <div class="col-md-4">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-sign-out-alt me-1"></i> <%= Common.getBahasaConfig("Status Keluar") %></label>
                    <select id="filterStatusKeluarStat<%=rnd%>" class="form-select form-select-sm border-info shadow-none" onchange="loadStatistik<%=rnd%>()">
                        <option value=""><%= Common.getBahasaConfig("Semua Status") %></option>
                        <% for(StatusKeluar sk : listStatusKeluar) { %>
                            <option value="<%= sk.getId() %>" <%= sk.getId() != null && sk.getId() == 1L ? "selected" : "" %>><%= sk.getNama() %></option>
                        <% } %>
                    </select>
                </div>
                
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-calendar-alt me-1"></i> <%= Common.getBahasaConfig("Thn Masuk (Dari)") %></label>
                    <input type="number" id="filterTaStartStat<%=rnd%>" class="form-control form-control-sm border-info shadow-none" placeholder="Cth: 2015" onkeydown="if(event.key === 'Enter') loadStatistik<%=rnd%>()">
                </div>
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-calendar-alt me-1"></i> <%= Common.getBahasaConfig("Thn Masuk (Sampai)") %></label>
                    <input type="number" id="filterTaEndStat<%=rnd%>" class="form-control form-control-sm border-info shadow-none" placeholder="Cth: 2019" onkeydown="if(event.key === 'Enter') loadStatistik<%=rnd%>()">
                </div>
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-calendar-check me-1"></i> <%= Common.getBahasaConfig("Thn Lulus (Dari)") %></label>
                    <input type="number" id="filterTlStartStat<%=rnd%>" class="form-control form-control-sm border-info shadow-none" placeholder="Cth: 2019" onkeydown="if(event.key === 'Enter') loadStatistik<%=rnd%>()">
                </div>
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-calendar-check me-1"></i> <%= Common.getBahasaConfig("Thn Lulus (Sampai)") %></label>
                    <input type="number" id="filterTlEndStat<%=rnd%>" class="form-control form-control-sm border-info shadow-none" placeholder="Cth: 2023" onkeydown="if(event.key === 'Enter') loadStatistik<%=rnd%>()">
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
// KONFIGURASI SQL REKAPITULASI DENGAN {FILTER} DAN JOIN STANDAR
// ==========================================
const reportConfigs<%=rnd%> = {
    "1": {
        title: "Sebaran Alumni per Tahun Lulus",
        columns: ["Tahun Lulus", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "status", "total"],
        sql: "SELECT COALESCE(CAST(m.tahunlulus AS VARCHAR), 'Belum Ada') as label1, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN status_keluar sk ON m.status_keluar = sk.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE 1=1 {FILTER} GROUP BY m.tahunlulus, sk.nama ORDER BY m.tahunlulus DESC"
    },
    "2": {
        title: "Sebaran per Program Studi & Tahun Lulus",
        columns: ["Program Studi", "Tahun Lulus", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "label2", "status", "total"],
        sql: "SELECT j.nama as label1, COALESCE(CAST(m.tahunlulus AS VARCHAR), '-') as label2, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN status_keluar sk ON m.status_keluar = sk.id WHERE 1=1 {FILTER} GROUP BY j.nama, m.tahunlulus, sk.nama ORDER BY j.nama ASC, m.tahunlulus DESC"
    },
    "3": {
        title: "Sebaran per Tahun Masuk (Angkatan)",
        columns: ["Tahun Masuk (Angkatan)", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "status", "total"],
        sql: "SELECT COALESCE(CAST(m.tahunangkatan AS VARCHAR), '-') as label1, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN status_keluar sk ON m.status_keluar = sk.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE 1=1 {FILTER} GROUP BY m.tahunangkatan, sk.nama ORDER BY m.tahunangkatan DESC"
    },
    "4": {
        title: "Sebaran per Jenis Kelamin",
        columns: ["Jenis Kelamin", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "status", "total"],
        sql: "SELECT COALESCE(m.kelamin, 'Tidak Diketahui') as label1, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN status_keluar sk ON m.status_keluar = sk.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE 1=1 {FILTER} GROUP BY m.kelamin, sk.nama"
    },
    "5": {
        title: "Sebaran per Program Kelas",
        columns: ["Program Kelas", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "status", "total"],
        sql: "SELECT COALESCE(m.program, '-') as label1, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN status_keluar sk ON m.status_keluar = sk.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE 1=1 {FILTER} GROUP BY m.program, sk.nama"
    },
    "6": {
        title: "Sebaran per Agama",
        columns: ["Agama", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "status", "total"],
        sql: "SELECT a.nama as label1, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN agama a ON m.agama = a.id LEFT JOIN status_keluar sk ON m.status_keluar = sk.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE 1=1 {FILTER} GROUP BY a.nama, sk.nama ORDER BY a.nama ASC"
    },
    "7": {
        title: "Sebaran per Status Awal Mahasiswa",
        columns: ["Status Awal", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "status", "total"],
        sql: "SELECT sa.nama as label1, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN status_awal_mahasiswa sa ON m.status_awal_mahasiswa = sa.id LEFT JOIN status_keluar sk ON m.status_keluar = sk.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE 1=1 {FILTER} GROUP BY sa.nama, sk.nama"
    },
    "8": {
        title: "Sebaran per Jenjang Pendidikan",
        columns: ["Jenjang Pendidikan", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "status", "total"],
        sql: "SELECT jenjang.nama as label1, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN jenjang jenjang ON m.jenjang = jenjang.id LEFT JOIN status_keluar sk ON m.status_keluar = sk.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE 1=1 {FILTER} GROUP BY jenjang.nama, sk.nama"
    },
    "9": {
        title: "Rekapitulasi Overall per Status Keluar",
        columns: ["Status Keluar", "Total Lulusan/Alumni"],
        keys: ["status", "total"],
        sql: "SELECT sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN status_keluar sk ON m.status_keluar = sk.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id WHERE 1=1 {FILTER} GROUP BY sk.nama"
    },
    "10": {
        title: "Sebaran per Fakultas, Prodi, dan Tahun Lulus",
        columns: ["Fakultas", "Program Studi", "Tahun Lulus", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "label2", "label3", "status", "total"],
        sql: "SELECT f.nama as label1, j.nama as label2, COALESCE(CAST(m.tahunlulus AS VARCHAR), '-') as label3, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN status_keluar sk ON m.status_keluar = sk.id WHERE 1=1 {FILTER} GROUP BY f.nama, j.nama, m.tahunlulus, sk.nama ORDER BY f.nama ASC, j.nama ASC, m.tahunlulus DESC"
    },
    "11": {
        title: "Sebaran per Jenis Pekerjaan Ayah, Prodi, & Tahun Lulus",
        columns: ["Pekerjaan Ayah", "Program Studi", "Tahun Lulus", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "label2", "label3", "status", "total"],
        sql: "SELECT p.nama as label1, j.nama as label2, COALESCE(CAST(m.tahunlulus AS VARCHAR), '-') as label3, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN biodata_mahasiswa bm ON bm.mahasiswa = m.id LEFT JOIN pekerjaan p ON bm.jenis_pekerjaan_ayah = p.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN status_keluar sk ON m.status_keluar = sk.id WHERE 1=1 {FILTER} GROUP BY p.nama, j.nama, m.tahunlulus, sk.nama ORDER BY p.nama ASC, j.nama ASC, m.tahunlulus DESC"
    },
    "12": {
        title: "Sebaran per Jenis Pekerjaan Ibu, Prodi, & Tahun Lulus",
        columns: ["Pekerjaan Ibu", "Program Studi", "Tahun Lulus", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "label2", "label3", "status", "total"],
        sql: "SELECT p.nama as label1, j.nama as label2, COALESCE(CAST(m.tahunlulus AS VARCHAR), '-') as label3, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN biodata_mahasiswa bm ON bm.mahasiswa = m.id LEFT JOIN pekerjaan p ON bm.jenis_pekerjaan_ibu = p.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN status_keluar sk ON m.status_keluar = sk.id WHERE 1=1 {FILTER} GROUP BY p.nama, j.nama, m.tahunlulus, sk.nama ORDER BY p.nama ASC, j.nama ASC, m.tahunlulus DESC"
    },
    "13": {
        title: "Sebaran per Jenis Pekerjaan Wali, Prodi, & Tahun Lulus",
        columns: ["Pekerjaan Wali", "Program Studi", "Tahun Lulus", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "label2", "label3", "status", "total"],
        sql: "SELECT p.nama as label1, j.nama as label2, COALESCE(CAST(m.tahunlulus AS VARCHAR), '-') as label3, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN biodata_mahasiswa bm ON bm.mahasiswa = m.id LEFT JOIN pekerjaan p ON bm.jenis_pekerjaan_wali = p.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN status_keluar sk ON m.status_keluar = sk.id WHERE 1=1 {FILTER} GROUP BY p.nama, j.nama, m.tahunlulus, sk.nama ORDER BY p.nama ASC, j.nama ASC, m.tahunlulus DESC"
    },
    "14": {
        title: "Sebaran per Penghasilan Ayah, Prodi, & Tahun Lulus",
        columns: ["Penghasilan Ayah", "Program Studi", "Tahun Lulus", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "label2", "label3", "status", "total"],
        sql: "SELECT p.nama as label1, j.nama as label2, COALESCE(CAST(m.tahunlulus AS VARCHAR), '-') as label3, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN biodata_mahasiswa bm ON bm.mahasiswa = m.id LEFT JOIN penghasilan p ON bm.jenis_penghasilan_ayah = p.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN status_keluar sk ON m.status_keluar = sk.id WHERE 1=1 {FILTER} GROUP BY p.nama, j.nama, m.tahunlulus, sk.nama ORDER BY p.nama ASC, j.nama ASC, m.tahunlulus DESC"
    },
    "15": {
        title: "Sebaran per Penghasilan Ibu, Prodi, & Tahun Lulus",
        columns: ["Penghasilan Ibu", "Program Studi", "Tahun Lulus", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "label2", "label3", "status", "total"],
        sql: "SELECT p.nama as label1, j.nama as label2, COALESCE(CAST(m.tahunlulus AS VARCHAR), '-') as label3, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN biodata_mahasiswa bm ON bm.mahasiswa = m.id LEFT JOIN penghasilan p ON bm.jenis_penghasilan_ibu = p.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN status_keluar sk ON m.status_keluar = sk.id WHERE 1=1 {FILTER} GROUP BY p.nama, j.nama, m.tahunlulus, sk.nama ORDER BY p.nama ASC, j.nama ASC, m.tahunlulus DESC"
    },
    "16": {
        title: "Sebaran per Penghasilan Wali, Prodi, & Tahun Lulus",
        columns: ["Penghasilan Wali", "Program Studi", "Tahun Lulus", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "label2", "label3", "status", "total"],
        sql: "SELECT p.nama as label1, j.nama as label2, COALESCE(CAST(m.tahunlulus AS VARCHAR), '-') as label3, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN biodata_mahasiswa bm ON bm.mahasiswa = m.id LEFT JOIN penghasilan p ON bm.jenis_penghasilan_wali = p.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN status_keluar sk ON m.status_keluar = sk.id WHERE 1=1 {FILTER} GROUP BY p.nama, j.nama, m.tahunlulus, sk.nama ORDER BY p.nama ASC, j.nama ASC, m.tahunlulus DESC"
    },
    "17": {
        title: "Sebaran per Jenjang Pendidikan Ayah, Prodi, & Tahun Lulus",
        columns: ["Pendidikan Ayah", "Program Studi", "Tahun Lulus", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "label2", "label3", "status", "total"],
        sql: "SELECT jp.nama as label1, j.nama as label2, COALESCE(CAST(m.tahunlulus AS VARCHAR), '-') as label3, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN biodata_mahasiswa bm ON bm.mahasiswa = m.id LEFT JOIN jenjang jp ON bm.jenjang_pendidikan_ayah = jp.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN status_keluar sk ON m.status_keluar = sk.id WHERE 1=1 {FILTER} GROUP BY jp.nama, j.nama, m.tahunlulus, sk.nama ORDER BY jp.nama ASC, j.nama ASC, m.tahunlulus DESC"
    },
    "18": {
        title: "Sebaran per Jenjang Pendidikan Ibu, Prodi, & Tahun Lulus",
        columns: ["Pendidikan Ibu", "Program Studi", "Tahun Lulus", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "label2", "label3", "status", "total"],
        sql: "SELECT jp.nama as label1, j.nama as label2, COALESCE(CAST(m.tahunlulus AS VARCHAR), '-') as label3, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN biodata_mahasiswa bm ON bm.mahasiswa = m.id LEFT JOIN jenjang jp ON bm.jenjang_pendidikan_ibu = jp.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN status_keluar sk ON m.status_keluar = sk.id WHERE 1=1 {FILTER} GROUP BY jp.nama, j.nama, m.tahunlulus, sk.nama ORDER BY jp.nama ASC, j.nama ASC, m.tahunlulus DESC"
    },
    "19": {
        title: "Sebaran per Jenjang Pendidikan Wali, Prodi, & Tahun Lulus",
        columns: ["Pendidikan Wali", "Program Studi", "Tahun Lulus", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "label2", "label3", "status", "total"],
        sql: "SELECT jp.nama as label1, j.nama as label2, COALESCE(CAST(m.tahunlulus AS VARCHAR), '-') as label3, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN biodata_mahasiswa bm ON bm.mahasiswa = m.id LEFT JOIN jenjang jp ON bm.jenjang_pendidikan_wali = jp.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN status_keluar sk ON m.status_keluar = sk.id WHERE 1=1 {FILTER} GROUP BY jp.nama, j.nama, m.tahunlulus, sk.nama ORDER BY jp.nama ASC, j.nama ASC, m.tahunlulus DESC"
    },
    "20": {
        title: "Sebaran per Sekolah Asal (SMA/SMK)",
        columns: ["Sekolah Asal", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "status", "total"],
        sql: "SELECT ns.nama as label1, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN biodata_mahasiswa bm ON bm.mahasiswa = m.id LEFT JOIN nama_sekolah_asal ns ON bm.nama_sekolah_asal = ns.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN status_keluar sk ON m.status_keluar = sk.id WHERE 1=1 {FILTER} GROUP BY ns.nama, sk.nama ORDER BY ns.nama ASC"
    },
    "21": {
        title: "Sebaran per Jenis Tinggal Mahasiswa",
        columns: ["Jenis Tinggal", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "status", "total"],
        sql: "SELECT jt.nama as label1, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN biodata_mahasiswa bm ON bm.mahasiswa = m.id LEFT JOIN jenis_tinggal_mahasiswa jt ON bm.jenis_tinggal_mahasiswa = jt.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN status_keluar sk ON m.status_keluar = sk.id WHERE 1=1 {FILTER} GROUP BY jt.nama, sk.nama ORDER BY jt.nama ASC"
    },
    "22": {
        title: "Sebaran per Alat Transportasi",
        columns: ["Alat Transportasi", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "status", "total"],
        sql: "SELECT at.nama as label1, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN biodata_mahasiswa bm ON bm.mahasiswa = m.id LEFT JOIN alat_transportasi_mahasiswa at ON bm.alat_transportasi_mahasiswa = at.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN status_keluar sk ON m.status_keluar = sk.id WHERE 1=1 {FILTER} GROUP BY at.nama, sk.nama ORDER BY at.nama ASC"
    },
    "23": {
        title: "Sebaran Kombinasi Pekerjaan Orang Tua",
        columns: ["Pekerjaan Ayah", "Pekerjaan Ibu", "Pekerjaan Wali", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "label2", "label3", "status", "total"],
        sql: "SELECT pa.nama as label1, pi.nama as label2, pw.nama as label3, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN biodata_mahasiswa bm ON bm.mahasiswa = m.id LEFT JOIN pekerjaan pa ON bm.jenis_pekerjaan_ayah = pa.id LEFT JOIN pekerjaan pi ON bm.jenis_pekerjaan_ibu = pi.id LEFT JOIN pekerjaan pw ON bm.jenis_pekerjaan_wali = pw.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN status_keluar sk ON m.status_keluar = sk.id WHERE 1=1 {FILTER} GROUP BY pa.nama, pi.nama, pw.nama, sk.nama ORDER BY pa.nama ASC"
    },
    "24": {
        title: "Sebaran Kombinasi Penghasilan Orang Tua",
        columns: ["Penghasilan Ayah", "Penghasilan Ibu", "Penghasilan Wali", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "label2", "label3", "status", "total"],
        sql: "SELECT pa.nama as label1, pi.nama as label2, pw.nama as label3, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN biodata_mahasiswa bm ON bm.mahasiswa = m.id LEFT JOIN penghasilan pa ON bm.jenis_penghasilan_ayah = pa.id LEFT JOIN penghasilan pi ON bm.jenis_penghasilan_ibu = pi.id LEFT JOIN penghasilan pw ON bm.jenis_penghasilan_wali = pw.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN status_keluar sk ON m.status_keluar = sk.id WHERE 1=1 {FILTER} GROUP BY pa.nama, pi.nama, pw.nama, sk.nama ORDER BY pa.nama ASC"
    },
    "25": {
        title: "Sebaran Kombinasi Pendidikan Orang Tua",
        columns: ["Pendidikan Ayah", "Pendidikan Ibu", "Pendidikan Wali", "Status Keluar", "Total Lulusan/Alumni"],
        keys: ["label1", "label2", "label3", "status", "total"],
        sql: "SELECT pa.nama as label1, pi.nama as label2, pw.nama as label3, sk.nama as status, COUNT(m.id) as total FROM mahasiswa m LEFT JOIN biodata_mahasiswa bm ON bm.mahasiswa = m.id LEFT JOIN jenjang pa ON bm.jenjang_pendidikan_ayah = pa.id LEFT JOIN jenjang pi ON bm.jenjang_pendidikan_ibu = pi.id LEFT JOIN jenjang pw ON bm.jenjang_pendidikan_wali = pw.id LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN status_keluar sk ON m.status_keluar = sk.id WHERE 1=1 {FILTER} GROUP BY pa.nama, pi.nama, pw.nama, sk.nama ORDER BY pa.nama ASC"
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
    const statK = document.getElementById('filterStatusKeluarStat<%=rnd%>').value;
    const taS = document.getElementById('filterTaStartStat<%=rnd%>').value;
    const taE = document.getElementById('filterTaEndStat<%=rnd%>').value;
    const tlS = document.getElementById('filterTlStartStat<%=rnd%>').value;
    const tlE = document.getElementById('filterTlEndStat<%=rnd%>').value;

    if (fakId) filterSql += " AND f.id = " + fakId + " ";
    if (prodiId) filterSql += " AND j.id = " + prodiId + " ";
    
    // Logika IS NOT NULL apabila Semua Status dipilih (Empty)
    if (statK) {
        filterSql += " AND m.status_keluar = " + statK + " ";
    } else {
        filterSql += " AND m.status_keluar IS NOT NULL ";
    }

    if (taS) filterSql += " AND m.tahunangkatan >= " + taS + " ";
    if (taE) filterSql += " AND m.tahunangkatan <= " + taE + " ";
    if (tlS) filterSql += " AND m.tahunlulus >= " + tlS + " ";
    if (tlE) filterSql += " AND m.tahunlulus <= " + tlE + " ";

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
    let html = "<tr><th class='text-center' style='width: 50px;'><%=Common.getBahasaConfig("No")%></th>";
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
        currentConfig<%=rnd%>.keys.forEach(k => { if(k !== 'total' && k !== 'status') lbl += row[k] + " "; });
        labels.push(lbl.trim() !== "" ? lbl.trim() : row['status']);
        totals.push(row['total']);
    });

    chartInstance<%=rnd%> = new Chart(ctx, {
        type: 'bar',
        data: { labels: labels, datasets: [{ label: 'Jumlah Mahasiswa', data: totals, backgroundColor: 'rgba(13, 110, 253, 0.7)', borderColor: 'rgba(13, 110, 253, 1)', borderWidth: 1, borderRadius: 4 }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } }
    });
};

const downloadExcel<%=rnd%> = () => {
    if(!currentData<%=rnd%> || currentData<%=rnd%>.length === 0) { alert('<%= Common.getBahasaConfigJS("Mohon maaf, belum ada data yang dapat diunduh.") %>'); return; }
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
    link.download = "Statistik_Rekap_" + new Date().getTime() + ".csv";
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
    if (tipe >= 11) {
        joinExtra += " LEFT JOIN biodata_mahasiswa bm ON bm.mahasiswa = m.id ";
    }
    if (tipe == 11) joinExtra += " LEFT JOIN pekerjaan p ON bm.jenis_pekerjaan_ayah = p.id ";
    if (tipe == 12) joinExtra += " LEFT JOIN pekerjaan p ON bm.jenis_pekerjaan_ibu = p.id ";
    if (tipe == 13) joinExtra += " LEFT JOIN pekerjaan p ON bm.jenis_pekerjaan_wali = p.id ";
    if (tipe == 14) joinExtra += " LEFT JOIN penghasilan p ON bm.jenis_penghasilan_ayah = p.id ";
    if (tipe == 15) joinExtra += " LEFT JOIN penghasilan p ON bm.jenis_penghasilan_ibu = p.id ";
    if (tipe == 16) joinExtra += " LEFT JOIN penghasilan p ON bm.jenis_penghasilan_wali = p.id ";
    if (tipe == 17) joinExtra += " LEFT JOIN jenjang jp ON bm.jenjang_pendidikan_ayah = jp.id ";
    if (tipe == 18) joinExtra += " LEFT JOIN jenjang jp ON bm.jenjang_pendidikan_ibu = jp.id ";
    if (tipe == 19) joinExtra += " LEFT JOIN jenjang jp ON bm.jenjang_pendidikan_wali = jp.id ";
    if (tipe == 20) joinExtra += " LEFT JOIN nama_sekolah_asal ns ON bm.nama_sekolah_asal = ns.id ";
    if (tipe == 21) joinExtra += " LEFT JOIN jenis_tinggal_mahasiswa jt ON bm.jenis_tinggal_mahasiswa = jt.id ";
    if (tipe == 22) joinExtra += " LEFT JOIN alat_transportasi_mahasiswa at ON bm.alat_transportasi_mahasiswa = at.id ";
    if (tipe == 23) joinExtra += " LEFT JOIN pekerjaan pa ON bm.jenis_pekerjaan_ayah = pa.id LEFT JOIN pekerjaan pi ON bm.jenis_pekerjaan_ibu = pi.id LEFT JOIN pekerjaan pw ON bm.jenis_pekerjaan_wali = pw.id ";
    if (tipe == 24) joinExtra += " LEFT JOIN penghasilan pa ON bm.jenis_penghasilan_ayah = pa.id LEFT JOIN penghasilan pi ON bm.jenis_penghasilan_ibu = pi.id LEFT JOIN penghasilan pw ON bm.jenis_penghasilan_wali = pw.id ";
    if (tipe == 25) joinExtra += " LEFT JOIN jenjang pa ON bm.jenjang_pendidikan_ayah = pa.id LEFT JOIN jenjang pi ON bm.jenjang_pendidikan_ibu = pi.id LEFT JOIN jenjang pw ON bm.jenjang_pendidikan_wali = pw.id ";

    let sqlJson = "SELECT m.nim, m.nama, j.nama as prodi, m.tahunlulus, m.program, m.tahunangkatan, m.judul_skripsi FROM mahasiswa m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN status_keluar sk ON m.status_keluar = sk.id LEFT JOIN agama a ON m.agama = a.id LEFT JOIN status_awal_mahasiswa sa ON m.status_awal_mahasiswa = sa.id LEFT JOIN jenjang jenjang ON m.jenjang = jenjang.id " + joinExtra + " WHERE 1=1 " + globalFilter;

    let sqlHtml = "SELECT m.* FROM mahasiswa m LEFT JOIN jurusan j ON m.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN status_keluar sk ON m.status_keluar = sk.id LEFT JOIN agama a ON m.agama = a.id LEFT JOIN status_awal_mahasiswa sa ON m.status_awal_mahasiswa = sa.id LEFT JOIN jenjang jenjang ON m.jenjang = jenjang.id " + joinExtra + " WHERE 1=1 " + globalFilter;

    let filter = "";
    if(tipe == 1) filter += " AND CAST(m.tahunlulus AS VARCHAR) " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 2) filter += " AND j.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND CAST(m.tahunlulus AS VARCHAR) " + safeSqlStr<%=rnd%>(row.label2);
    if(tipe == 3) filter += " AND CAST(m.tahunangkatan AS VARCHAR) " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 4) filter += " AND m.kelamin " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 5) filter += " AND m.program " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 6) filter += " AND a.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 7) filter += " AND sa.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 8) filter += " AND jenjang.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 10) filter += " AND f.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND j.nama " + safeSqlStr<%=rnd%>(row.label2) + " AND CAST(m.tahunlulus AS VARCHAR) " + safeSqlStr<%=rnd%>(row.label3);
    
    if(tipe >= 11 && tipe <= 16) filter += " AND p.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND j.nama " + safeSqlStr<%=rnd%>(row.label2) + " AND CAST(m.tahunlulus AS VARCHAR) " + safeSqlStr<%=rnd%>(row.label3);
    if(tipe >= 17 && tipe <= 19) filter += " AND jp.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND j.nama " + safeSqlStr<%=rnd%>(row.label2) + " AND CAST(m.tahunlulus AS VARCHAR) " + safeSqlStr<%=rnd%>(row.label3);
    
    if(tipe == 20) filter += " AND ns.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 21) filter += " AND jt.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 22) filter += " AND at.nama " + safeSqlStr<%=rnd%>(row.label1);
    
    if(tipe >= 23 && tipe <= 25) filter += " AND pa.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND pi.nama " + safeSqlStr<%=rnd%>(row.label2) + " AND pw.nama " + safeSqlStr<%=rnd%>(row.label3);
    
    filter += " AND sk.nama " + safeSqlStr<%=rnd%>(row.status);
    
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
                                        "<th class='text-center py-3' style='width: 50px;'><%=Common.getBahasaConfig("No")%></th>" +
                                        "<th class='text-center py-3' style='width: 60px;'><%=Common.getBahasaConfig("Foto")%></th>" +
                                        "<th class='py-3'><%=Common.getBahasaConfig("NIM")%></th>" +
                                        "<th class='py-3'><%=Common.getBahasaConfig("Nama Mahasiswa")%></th>" +
                                        "<th class='py-3'><%=Common.getBahasaConfig("Program Studi")%></th>" +
                                        "<th class='text-center py-3'><%=Common.getBahasaConfig("Thn Lulus")%></th>" +
                                        "<th class='text-center py-3'><%=Common.getBahasaConfig("Program")%></th>" +
                                        "<th class='text-center py-3'><%=Common.getBahasaConfig("Thn Masuk")%></th>" +
                                        "<th class='py-3'><%=Common.getBahasaConfig("Judul Skripsi")%></th>" +
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
        tbody.innerHTML = "<tr><td colspan='9' class='text-center text-muted p-4'>Tidak ada data rincian ditemukan</td></tr>";
        document.getElementById("infoPaginasiDetail<%=rnd%>").innerText = "Menampilkan 0 data";
        document.getElementById("paginasiDetail<%=rnd%>").innerHTML = "";
        return;
    }

    tbody.innerHTML = "<tr><td colspan='9' class='text-center p-4'><span class='spinner-border text-primary'></span></td></tr>";

    const startIdx = (detailCurrentPage<%=rnd%> - 1) * 10;
    const endIdx = Math.min(startIdx + 10, detailDataCache<%=rnd%>.length);

    const formData = new URLSearchParams();
    formData.append("action_jsp", "get_html_detail");
    formData.append("sql", window.currentDetailSqlHtml<%=rnd%>);
    formData.append("start", startIdx);
    formData.append("limit", 10);

    try {
        const res = await fetch('<%=Common.ROOT%>/alumni?hanya_tampil_jsp=true&p=alumni&s=_statistik_alumni&baru=true&rnd=<%=rnd%>', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData.toString()
        });
        const htmlServer = await res.text();
        tbody.innerHTML = htmlServer;
    } catch (e) {
        tbody.innerHTML = "<tr><td colspan='9' class='text-center text-danger p-3'>Gagal menarik data profil mahasiswa.</td></tr>";
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
    csv += "No;NIM;Nama Mahasiswa;Program Studi;Tahun Lulus;Program;Tahun Masuk;Judul Skripsi\n";

    detailDataCache<%=rnd%>.forEach((row, i) => {
        let cols = [
            i+1,
            row.nim || "-",
            row.nama || "-",
            row.prodi || "-",
            row.tahunlulus || "-",
            row.program || "-",
            row.tahunangkatan || "-",
            row.judul_skripsi || "-"
        ];
        let rowStr = cols.map(v => '"' + String(v).replace(/"/g, '""') + '"').join(";");
        csv += rowStr + "\n";
    });

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = "Detail_Data_Statistik_" + new Date().getTime() + ".csv";
    document.body.appendChild(link); link.click(); document.body.removeChild(link);
};

// Eksekusi load awal saat file dimuat lewat ajax tab
loadStatistik<%=rnd%>();
</script>