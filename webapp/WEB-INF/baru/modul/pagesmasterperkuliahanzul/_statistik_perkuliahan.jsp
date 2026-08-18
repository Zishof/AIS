<%@page import="org.hibernate.transform.Transformers"%>
<%@page import="java.util.Map"%>
<%@page import="ais.database.model.Perkuliahan"%>
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
    rnd = "Prk" + rnd; 

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
            // Mengubah format hasil query menjadi Map berbasis alias (kolom)
            List<Map<String, Object>> list = sess.createSQLQuery(sql)
                                      .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                                      .setFirstResult(start)
                                      .setMaxResults(limit)
                                      .list();
                                      
            for (int i = 0; i < list.size(); i++) {
                Map<String, Object> row = list.get(i);
                
                if ("dosen".equals(tipeDetail)) {
                    // Rincian Data Beban Mengajar Dosen (Menggunakan Alias)
                    String namaDsn = row.get("nama_dsn") != null ? row.get("nama_dsn").toString() : "-";
                    String prodi = row.get("prodi") != null ? row.get("prodi").toString() : "-";
                    String namaMk = row.get("nama_mk") != null ? row.get("nama_mk").toString() : "-";
                    String sks = row.get("sks") != null ? row.get("sks").toString() : "0";
                    String kelas = row.get("kelas") != null ? row.get("kelas").toString() : "-";
                    String ruang = row.get("ruang") != null ? row.get("ruang").toString() : "-";
                    String hariJam = (row.get("hari") != null ? row.get("hari").toString() : "-") + " " + 
                                     (row.get("waktu_mulai") != null ? row.get("waktu_mulai").toString() : "");
                    
                    %>
                    <tr>
                        <td class='text-center'><%= (start + i + 1) %></td>
                        <td class='fw-bold text-primary'><%= namaDsn %></td>
                        <td class='fw-bold'><%= namaMk %></td>
                        <td class='text-center'><%= sks %></td>
                        <td><%= prodi %></td>
                        <td class='text-center'><span class="badge bg-secondary"><%= kelas %></span></td>
                        <td><%= ruang %></td>
                        <td class='text-center'><%= hariJam %></td>
                    </tr>
                    <%
                } else {
                    // Rincian Data Kelas Perkuliahan (Menggunakan Alias)
                    String ta = row.get("tahun_ajaran") != null ? row.get("tahun_ajaran").toString() : "-";
                    String gg = row.get("ganjil_genap") != null ? row.get("ganjil_genap").toString() : "-";
                    String prodi = row.get("prodi") != null ? row.get("prodi").toString() : "-";
                    String namaMk = row.get("nama_mk") != null ? row.get("nama_mk").toString() : "-";
                    String sks = row.get("sks") != null ? row.get("sks").toString() : "0";
                    String kelas = row.get("kelas") != null ? row.get("kelas").toString() : "-";
                    String ruang = row.get("ruang") != null ? row.get("ruang").toString() : "-";
                    String hariJam = (row.get("hari") != null ? row.get("hari").toString() : "-") + " " + 
                                     (row.get("waktu_mulai") != null ? row.get("waktu_mulai").toString() : "");
                    
                    %>
                    <tr>
                        <td class='text-center'><%= (start + i + 1) %></td>
                        <td class='text-center fw-bold'><%= ta %> <br><small class="text-muted"><%= gg %></small></td>
                        <td><%= prodi %></td>
                        <td class='fw-bold text-primary'><%= namaMk %></td>
                        <td class='text-center'><%= sks %></td>
                        <td class='text-center'><span class="badge bg-secondary"><%= kelas %></span></td>
                        <td><%= ruang %></td>
                        <td class='text-center'><%= hariJam %></td>
                    </tr>
                    <%
                }
            }
        } catch(Exception e) {
            out.print("<tr><td colspan='8' class='text-center text-danger'>" + Common.getBahasaConfig("Terjadi kesalahan kueri data rincian perkuliahan.") + "</td></tr>");
        } finally {
            try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmasterperkuliahanzul/_statistik_perkuliahan.jsp:104");}
            try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmasterperkuliahanzul/_statistik_perkuliahan.jsp:105");}
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
    } catch(Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pagesmasterperkuliahanzul/_statistik_perkuliahan.jsp:117"); } 
    finally { try { sessFilter.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmasterperkuliahanzul/_statistik_perkuliahan.jsp:118");} HibernateUtil.closeSession(); }
%>

<div class="container-fluid p-0 animate__animated animate__fadeIn">
    <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3">
        <h5 class="mb-0 fw-bold text-primary">
            <i class="fas fa-chalkboard me-2"></i><%= Common.getBahasaConfig("Dashboard Statistik Perkuliahan") %>
        </h5>
    </div>

    <div class="card shadow-sm border-0 rounded-4 mb-4">
        <div class="card-body bg-light rounded-4 p-4">
            <div class="row g-3">
                <div class="col-md-12 mb-2">
                    <label class="form-label fw-bold text-secondary small"><i class="fas fa-list me-1"></i><%= Common.getBahasaConfig("Pilih Jenis Laporan:") %></label>
                    <select class="form-select border-primary shadow-sm fw-bold" id="jenisLaporan<%=rnd%>" onchange="loadStatistik<%=rnd%>()">
                        <optgroup label="<%= Common.getBahasaConfig("Data Kelas Perkuliahan") %>">
                            <option value="1">1. Sebaran Kelas per Program Studi & Fakultas</option>
                            <option value="2">2. Sebaran Kelas per Tahun Akademik & Semester</option>
                            <option value="3">3. Sebaran Kelas per Semester Angka (1-8)</option>
                            <option value="4">4. Sebaran Kelas per Program (Reguler dll) & Prodi</option>
                            <option value="5">5. Sebaran Kelas per Hari</option>
                            <option value="6">6. Sebaran Kelas per Ruangan</option>
                            <option value="7">7. Sebaran Kelas per Hari & Jam Mulai-Selesai</option>
                            <option value="8">8. Sebaran Kelas per Nama Kelas (KelasRef)</option>
                            <option value="9">9. Sebaran Kelas per Kurikulum Berjalan</option>
                            <option value="10">10. Sebaran Kelas per Jam Perkuliahan</option>
                            <option value="11">11. Sebaran Kelas per Masa Perkuliahan</option>
                            <option value="12">12. Sebaran Status Dikunci / Finalisasi Kelas</option>
                            <option value="13">13. Sebaran Kelas per Jenis Evaluasi</option>
                            <option value="14">14. Sebaran Tipe Pengajar (Tunggal / Team Teaching)</option>
                        </optgroup>
                        <optgroup label="<%= Common.getBahasaConfig("Data Beban Mengajar Dosen") %>">
                            <option value="15">15. Total Jumlah Kelas yang Diajar per Dosen</option>
                            <option value="16">16. Total Beban SKS yang Diajar per Dosen</option>
                            <option value="17">17. Sebaran Dosen Mengajar per Hari</option>
                            <option value="18">18. Sebaran Dosen Mengajar per Hari & Jam</option>
                            <option value="19">19. Sebaran Dosen Mengajar per Semester Angka (1-8)</option>
                            <option value="20">20. Sebaran Dosen Mengajar per Nama Kelas (KelasRef)</option>
                            <option value="21">21. Sebaran Dosen Mengajar per Ruangan</option>
                        </optgroup>
                        <optgroup label="<%= Common.getBahasaConfig("Data Mata Kuliah Terbuka") %>">
                            <option value="22">22. Distribusi Kelas per Mata Kuliah (Kode & Nama)</option>
                            <option value="23">23. Distribusi Kelas per Beban SKS Mata Kuliah</option>
                            <option value="24">24. Distribusi Kelas per Sifat Mata Kuliah (Wajib/Pilihan)</option>
                            <option value="25">25. Distribusi Kelas per Kelompok Mata Kuliah</option>
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
                        <option value="<%= Perkuliahan.SP %>">Pendek</option>
                    </select>
                </div>
                <div class="col-md-2">
                    <label class="form-label small fw-bold text-secondary"><%= Common.getBahasaConfig("Mata Kuliah") %></label>
                    <input type="text" id="filterMkStat<%=rnd%>" class="form-control form-control-sm border-info shadow-none" placeholder="Kode / Nama" onkeydown="if(event.key === 'Enter') loadStatistik<%=rnd%>()">
                </div>
                <div class="col-md-2">
                    <label class="form-label small fw-bold text-secondary"><%= Common.getBahasaConfig("Nama Dosen") %></label>
                    <input type="text" id="filterDosenStat<%=rnd%>" class="form-control form-control-sm border-info shadow-none" placeholder="Cari Dosen..." onkeydown="if(event.key === 'Enter') loadStatistik<%=rnd%>()">
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
        <div class="card-body p-4"><div style="position: relative; height: 350px; width: 100%;"><canvas id="chartPerkuliahan<%=rnd%>"></canvas></div></div>
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
let chartInstance<%=rnd%> = null;
let currentData<%=rnd%> = [];
let currentPage<%=rnd%> = 1;
const itemsPerPage<%=rnd%> = 10;
let currentConfig<%=rnd%> = null;

const basePrkJoinSql = " FROM perkuliahan p LEFT JOIN jurusan j ON p.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN matakuliah m ON p.matakuliah = m.id LEFT JOIN ruang r ON p.ruang = r.id LEFT JOIN kurikulum k ON p.kurikulum = k.id LEFT JOIN jam_perkuliahan jp ON p.jam_perkuliahan = jp.id LEFT JOIN masa_perkuliahan mp ON p.masa_perkuliahan = mp.id LEFT JOIN jenis_evaluasi je ON p.jenis_evaluasi = je.id LEFT JOIN kelompok_matakuliah km ON m.kelompok_matakuliah = km.id WHERE (p.aktif = true OR p.aktif IS NULL) ";

const baseDsnJoinSql = " FROM perkuliahan p JOIN matakuliah m ON p.matakuliah = m.id JOIN dosen d ON d.id IN (p.dosen1, p.dosen2, p.dosen3, p.dosen4, p.dosen5, p.dosen6, p.dosen7, p.dosen8, p.dosen9, p.dosen10) LEFT JOIN jurusan j ON p.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN ruang r ON p.ruang = r.id LEFT JOIN kurikulum k ON p.kurikulum = k.id LEFT JOIN jam_perkuliahan jp ON p.jam_perkuliahan = jp.id LEFT JOIN masa_perkuliahan mp ON p.masa_perkuliahan = mp.id LEFT JOIN jenis_evaluasi je ON p.jenis_evaluasi = je.id LEFT JOIN kelompok_matakuliah km ON m.kelompok_matakuliah = km.id WHERE (p.aktif = true OR p.aktif IS NULL) AND (d.aktif = true OR d.aktif IS NULL) ";

const reportConfigs<%=rnd%> = {
    // === GROUP 1: KELAS ===
    "1": { title: "Sebaran Kelas per Program Studi & Fakultas", columns: ["Fakultas", "Program Studi", "Total Kelas"], keys: ["label1", "label2", "total"], sql: "SELECT COALESCE(f.nama, 'Belum Diatur') as label1, COALESCE(j.nama, 'Belum Diatur') as label2, COUNT(p.id) as total " + basePrkJoinSql + " {FILTER} GROUP BY f.nama, j.nama ORDER BY f.nama ASC, j.nama ASC" },
    "2": { title: "Sebaran Kelas per Tahun Akademik & Jenis Semester", columns: ["Tahun Akademik", "Jenis Semester", "Total Kelas"], keys: ["label1", "label2", "total"], sql: "SELECT COALESCE(p.tahun_ajaran, '-') as label1, COALESCE(p.ganjil_genap, '-') as label2, COUNT(p.id) as total " + basePrkJoinSql + " {FILTER} GROUP BY p.tahun_ajaran, p.ganjil_genap ORDER BY p.tahun_ajaran DESC, p.ganjil_genap ASC" },
    "3": { title: "Sebaran Kelas per Semester (Angka)", columns: ["Semester", "Total Kelas"], keys: ["label1", "total"], sql: "SELECT 'Semester ' || CAST(COALESCE(p.smt, 0) AS VARCHAR) as label1, COUNT(p.id) as total " + basePrkJoinSql + " {FILTER} GROUP BY p.smt ORDER BY p.smt ASC" },
    "4": { title: "Sebaran Kelas per Program & Prodi", columns: ["Program", "Program Studi", "Total Kelas"], keys: ["label1", "label2", "total"], sql: "SELECT COALESCE(p.program, 'Reguler') as label1, COALESCE(j.nama, 'Belum Diatur') as label2, COUNT(p.id) as total " + basePrkJoinSql + " {FILTER} GROUP BY p.program, j.nama ORDER BY p.program ASC, j.nama ASC" },
    "5": { title: "Sebaran Kelas per Hari", columns: ["Hari", "Total Kelas"], keys: ["label1", "total"], sql: "SELECT COALESCE(p.hari, 'Belum Diatur') as label1, COUNT(p.id) as total " + basePrkJoinSql + " {FILTER} GROUP BY p.hari ORDER BY p.hari ASC" },
    "6": { title: "Sebaran Kelas per Ruangan", columns: ["Nama Ruangan", "Total Kelas"], keys: ["label1", "total"], sql: "SELECT COALESCE(r.nama, 'Tanpa Ruangan') as label1, COUNT(p.id) as total " + basePrkJoinSql + " {FILTER} GROUP BY r.nama ORDER BY r.nama ASC" },
    "7": { title: "Sebaran Kelas per Hari & Jam", columns: ["Hari", "Waktu Mulai - Selesai", "Total Kelas"], keys: ["label1", "label2", "total"], sql: "SELECT COALESCE(p.hari, 'Belum Diatur') as label1, COALESCE(p.waktu_mulai, '-') || ' - ' || COALESCE(p.waktu_selesai, '-') as label2, COUNT(p.id) as total " + basePrkJoinSql + " {FILTER} GROUP BY p.hari, p.waktu_mulai, p.waktu_selesai ORDER BY p.hari ASC" },
    //"8": { title: "Sebaran Kelas per Nama Kelas (KelasRef)", columns: ["Nama Kelas", "Total Data"], keys: ["label1", "total"], sql: "SELECT COALESCE(CAST(p.kelasref AS VARCHAR), 'Tidak Ada Kelas') as label1, COUNT(p.id) as total " + basePrkJoinSql + " {FILTER} GROUP BY p.kelasref ORDER BY p.kelasref ASC" },
    "8": { title: "Sebaran Kelas per Nama Kelas", columns: ["Nama Kelas", "Total Data"], keys: ["label1", "total"], sql: "SELECT COALESCE(p.kelas, 'Tidak Ada Kelas') as label1, COUNT(p.id) as total " + basePrkJoinSql + " {FILTER} GROUP BY p.kelas ORDER BY p.kelas ASC" },
    "9": { title: "Sebaran Kelas per Kurikulum", columns: ["Kurikulum", "Total Kelas"], keys: ["label1", "total"], sql: "SELECT COALESCE(k.nama, 'Tidak Terikat Kurikulum') as label1, COUNT(p.id) as total " + basePrkJoinSql + " {FILTER} GROUP BY k.nama ORDER BY k.nama ASC" },
    "10": { title: "Sebaran Kelas per Jam Perkuliahan", columns: ["Jam Perkuliahan", "Total Kelas"], keys: ["label1", "total"], sql: "SELECT COALESCE(jp.nama, 'Belum Diatur') as label1, COUNT(p.id) as total " + basePrkJoinSql + " {FILTER} GROUP BY jp.nama ORDER BY jp.nama ASC" },
    "11": { title: "Sebaran Kelas per Masa Perkuliahan", columns: ["Masa Perkuliahan", "Total Kelas"], keys: ["label1", "total"], sql: "SELECT COALESCE(mp.nama, 'Belum Diatur') as label1, COUNT(p.id) as total " + basePrkJoinSql + " {FILTER} GROUP BY mp.nama ORDER BY mp.nama ASC" },
    "12": { title: "Sebaran Status Dikunci / Finalisasi", columns: ["Status Dikunci", "Total Kelas"], keys: ["label1", "total"], sql: "SELECT CASE WHEN p.dikunci IS NOT NULL THEN 'Sudah Final (Dikunci)' ELSE 'Masih Terbuka' END as label1, COUNT(p.id) as total " + basePrkJoinSql + " {FILTER} GROUP BY (p.dikunci IS NOT NULL)" },
    "13": { title: "Sebaran Kelas per Jenis Evaluasi", columns: ["Jenis Evaluasi", "Total Kelas"], keys: ["label1", "total"], sql: "SELECT COALESCE(je.nama, 'Belum Diatur') as label1, COUNT(p.id) as total " + basePrkJoinSql + " {FILTER} GROUP BY je.nama ORDER BY je.nama ASC" },
    "14": { title: "Sebaran Tipe Pengajar Kelas", columns: ["Tipe Pengajar", "Total Kelas"], keys: ["label1", "total"], sql: "SELECT CASE WHEN p.dosen2 IS NOT NULL THEN 'Team Teaching (Lebih dari 1)' ELSE 'Dosen Tunggal' END as label1, COUNT(p.id) as total " + basePrkJoinSql + " {FILTER} GROUP BY (p.dosen2 IS NOT NULL)" },

    // === GROUP 2: DOSEN ===
    "15": { title: "Total Jumlah Kelas yang Diajar per Dosen", columns: ["Nama Dosen", "Total Kelas Diajar"], keys: ["label1", "total"], sql: "SELECT d.nama as label1, COUNT(p.id) as total " + baseDsnJoinSql + " {FILTER} GROUP BY d.nama ORDER BY d.nama ASC" },
    "16": { title: "Total Beban SKS yang Diajar per Dosen", columns: ["Nama Dosen", "Total Beban SKS"], keys: ["label1", "total"], sql: "SELECT d.nama as label1, SUM(m.sks) as total " + baseDsnJoinSql + " {FILTER} GROUP BY d.nama ORDER BY d.nama ASC" },
    "17": { title: "Sebaran Dosen Mengajar per Hari", columns: ["Nama Dosen", "Hari Mengajar", "Jumlah Kelas"], keys: ["label1", "label2", "total"], sql: "SELECT d.nama as label1, COALESCE(p.hari, 'Belum Diatur') as label2, COUNT(p.id) as total " + baseDsnJoinSql + " {FILTER} GROUP BY d.nama, p.hari ORDER BY d.nama ASC, p.hari ASC" },
    "18": { title: "Sebaran Dosen Mengajar per Hari & Jam", columns: ["Nama Dosen", "Hari & Jam", "Jumlah Kelas"], keys: ["label1", "label2", "total"], sql: "SELECT d.nama as label1, COALESCE(p.hari, 'Belum Diatur') || ', ' || COALESCE(p.waktu_mulai, '') || '-' || COALESCE(p.waktu_selesai, '') as label2, COUNT(p.id) as total " + baseDsnJoinSql + " {FILTER} GROUP BY d.nama, p.hari, p.waktu_mulai, p.waktu_selesai ORDER BY d.nama ASC, p.hari ASC" },
    "19": { title: "Sebaran Dosen Mengajar per Semester (Angka)", columns: ["Nama Dosen", "Semester Kelas", "Jumlah Kelas"], keys: ["label1", "label2", "total"], sql: "SELECT d.nama as label1, 'Semester ' || CAST(COALESCE(p.smt, 0) AS VARCHAR) as label2, COUNT(p.id) as total " + baseDsnJoinSql + " {FILTER} GROUP BY d.nama, p.smt ORDER BY d.nama ASC, p.smt ASC" },
    //"20": { title: "Sebaran Dosen Mengajar per Nama Kelas (KelasRef)", columns: ["Nama Dosen", "Nama Kelas", "Jumlah Kelas"], keys: ["label1", "label2", "total"], sql: "SELECT d.nama as label1, COALESCE(CAST(p.kelasref AS VARCHAR), 'Tidak Ada Kelas') as label2, COUNT(p.id) as total " + baseDsnJoinSql + " {FILTER} GROUP BY d.nama, p.kelasref ORDER BY d.nama ASC" },
    "20": { title: "Sebaran Dosen Mengajar per Nama Kelas", columns: ["Nama Dosen", "Nama Kelas", "Jumlah Kelas"], keys: ["label1", "label2", "total"], sql: "SELECT d.nama as label1, COALESCE(p.kelas, 'Tidak Ada Kelas') as label2, COUNT(p.id) as total " + baseDsnJoinSql + " {FILTER} GROUP BY d.nama, p.kelas ORDER BY d.nama ASC" },
    "21": { title: "Sebaran Dosen Mengajar per Ruangan", columns: ["Nama Dosen", "Ruangan", "Jumlah Kelas"], keys: ["label1", "label2", "total"], sql: "SELECT d.nama as label1, COALESCE(r.nama, 'Tanpa Ruangan') as label2, COUNT(p.id) as total " + baseDsnJoinSql + " {FILTER} GROUP BY d.nama, r.nama ORDER BY d.nama ASC, r.nama ASC" },

    // === GROUP 3: MATAKULIAH ===
    "22": { title: "Distribusi Kelas per Mata Kuliah (Kode & Nama)", columns: ["Kode - Nama Mata Kuliah", "Total Kelas"], keys: ["label1", "total"], sql: "SELECT COALESCE(m.kode, '-') || ' - ' || COALESCE(m.nama, '-') as label1, COUNT(p.id) as total " + basePrkJoinSql + " {FILTER} GROUP BY m.kode, m.nama ORDER BY m.nama ASC" },
    "23": { title: "Distribusi Kelas per Beban SKS Mata Kuliah", columns: ["Beban SKS", "Total Kelas Dibuka"], keys: ["label1", "total"], sql: "SELECT CAST(COALESCE(m.sks, 0) AS VARCHAR) || ' SKS' as label1, COUNT(p.id) as total " + basePrkJoinSql + " {FILTER} GROUP BY m.sks ORDER BY m.sks ASC" },
    "24": { title: "Distribusi Kelas per Sifat Mata Kuliah (Wajib/Pilihan)", columns: ["Sifat Mata Kuliah", "Total Kelas Dibuka"], keys: ["label1", "total"], sql: "SELECT CASE WHEN m.status = 'Wajib' THEN 'Wajib' ELSE 'Pilihan' END as label1, COUNT(p.id) as total " + basePrkJoinSql + " {FILTER} GROUP BY m.status" },
    "25": { title: "Distribusi Kelas per Kelompok Mata Kuliah", columns: ["Kelompok Mata Kuliah", "Total Kelas Dibuka"], keys: ["label1", "total"], sql: "SELECT COALESCE(km.nama, 'Tanpa Kelompok') as label1, COUNT(p.id) as total " + basePrkJoinSql + " {FILTER} GROUP BY km.nama ORDER BY km.nama ASC" }
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
    const gg = document.getElementById('filterSemester<%=rnd%>').value;
    const mk = document.getElementById('filterMkStat<%=rnd%>').value;
    const dsn = document.getElementById('filterDosenStat<%=rnd%>').value;

    if (fakId) filterSql += " AND f.id = " + fakId + " ";
    if (prodiId) filterSql += " AND j.id = " + prodiId + " ";
    if (ta) filterSql += " AND p.tahun_ajaran = '" + ta.replace(/'/g, "''") + "' ";
    if (gg) filterSql += " AND p.ganjil_genap = '" + gg.replace(/'/g, "''") + "' ";
    if (mk) filterSql += " AND (m.kode ILIKE '%" + mk.replace(/'/g, "''") + "%' OR m.nama ILIKE '%" + mk.replace(/'/g, "''") + "%') ";
    
    if (dsn) {
        let lpr = parseInt(document.getElementById("jenisLaporan<%=rnd%>").value);
        if (lpr >= 15 && lpr <= 21) {
            filterSql += " AND d.nama ILIKE '%" + dsn.replace(/'/g, "''") + "%' ";
        } else {
            filterSql += " AND EXISTS (SELECT 1 FROM dosen dx WHERE dx.id IN (p.dosen1, p.dosen2, p.dosen3, p.dosen4, p.dosen5, p.dosen6, p.dosen7, p.dosen8, p.dosen9, p.dosen10) AND dx.nama ILIKE '%" + dsn.replace(/'/g, "''") + "%') ";
        }
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
    let html = "<tr><th class='text-center' style='width: 50px;'>No</th>";
    currentConfig<%=rnd%>.columns.forEach(col => { html += "<th>" + col + "</th>"; });
    html += "</tr>"; document.getElementById("theadRekap<%=rnd%>").innerHTML = html;
};

const renderTabel<%=rnd%> = () => {
    const tbody = document.getElementById("tbodyRekap<%=rnd%>");
    if(currentData<%=rnd%>.length === 0) {
        tbody.innerHTML = "<tr><td colspan='" + (currentConfig<%=rnd%>.columns.length + 1) + "' class='text-center text-muted p-4'>Tidak ada data ditemukan</td></tr>";
        document.getElementById("infoPaginasi<%=rnd%>").innerText = "Menampilkan 0 data";
        document.getElementById("paginasiTabel<%=rnd%>").innerHTML = ""; return;
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
        } else if (i === currentPage<%=rnd%> - 2 || i === currentPage<%=rnd%> + 2) {
            html += "<li class='page-item disabled'><span class='page-link'>...</span></li>";
        }
    }
    html += "<li class='page-item " + (currentPage<%=rnd%> === tPages ? "disabled" : "") + "'><button class='page-link' onclick='ubahHalaman<%=rnd%>(" + (currentPage<%=rnd%> + 1) + ")'><i class='fas fa-angle-right'></i></button></li>";
    document.getElementById("paginasiTabel<%=rnd%>").innerHTML = html;
};

const ubahHalaman<%=rnd%> = (page) => { currentPage<%=rnd%> = page; renderTabel<%=rnd%>(); };

const renderChart<%=rnd%> = () => {
    const ctx = document.getElementById('chartPerkuliahan<%=rnd%>').getContext('2d');
    if(chartInstance<%=rnd%>) chartInstance<%=rnd%>.destroy();
    let labels = [], totals = [];
    currentData<%=rnd%>.slice(0, 20).forEach(row => {
        let lbl = ""; currentConfig<%=rnd%>.keys.forEach(k => { if(k !== 'total') lbl += row[k] + " "; });
        labels.push(lbl.trim()); totals.push(row['total']);
    });
    chartInstance<%=rnd%> = new Chart(ctx, {
        type: 'bar', data: { labels: labels, datasets: [{ label: 'Total', data: totals, backgroundColor: 'rgba(54, 162, 235, 0.7)', borderColor: 'rgba(54, 162, 235, 1)', borderWidth: 1, borderRadius: 4 }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } }
    });
};

const downloadExcel<%=rnd%> = () => {
    if(!currentData<%=rnd%> || currentData<%=rnd%>.length === 0) return;
    let csv = "\uFEFF" + currentConfig<%=rnd%>.columns.join(";") + "\n";
    currentData<%=rnd%>.forEach(row => {
        let rowArr = []; currentConfig<%=rnd%>.keys.forEach(k => { rowArr.push('"' + (row[k] !== null ? String(row[k]).replace(/"/g, '""') : "-") + '"'); });
        csv += rowArr.join(";") + "\n";
    });
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement("a"); link.href = URL.createObjectURL(blob);
    link.download = "Statistik_Perkuliahan_" + new Date().getTime() + ".csv";
    document.body.appendChild(link); link.click(); document.body.removeChild(link);
};

// ==========================================
// MODAL POPUP DETAIL PERKULIAHAN & DOSEN
// ==========================================
let detailDataCache<%=rnd%> = [];
let detailCurrentPage<%=rnd%> = 1;
let globalTipeDetail<%=rnd%> = "perkuliahan";

const safeSqlStr<%=rnd%> = (val) => {
    if(val === '-' || val === 'Belum Diatur' || val === 'Tanpa Ruangan' || val === 'Tidak Ada Nama Kelas' || val === 'Tanpa Kelompok' || val === 'Tidak Terikat Kurikulum') return " IS NULL ";
    return " = '" + val.toString().replace(/'/g, "''") + "' ";
};

const bukaDetailStatistik<%=rnd%> = async (dataIndex, event) => {
    const row = currentData<%=rnd%>[dataIndex];
    const tipe = parseInt(document.getElementById("jenisLaporan<%=rnd%>").value);
    const btn = event.currentTarget; const oriContent = btn.innerHTML;
    btn.innerHTML = "<span class='spinner-border spinner-border-sm'></span>"; btn.disabled = true;

    const globalFilter = getFilterSql<%=rnd%>();
    globalTipeDetail<%=rnd%> = (tipe >= 15 && tipe <= 21) ? "dosen" : "perkuliahan";
    
    let sqlJson = ""; let sqlHtml = "";

    if (globalTipeDetail<%=rnd%> === "perkuliahan") {
        sqlJson = "SELECT p.tahun_ajaran, p.ganjil_genap, j.nama as prodi, m.nama as nama_mk, m.sks, p.kelasref as kelas, r.nama as ruang, p.hari, p.waktu_mulai " + basePrkJoinSql + globalFilter;
        sqlHtml = sqlJson;
        
        if(tipe == 1) sqlHtml += " AND f.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND j.nama " + safeSqlStr<%=rnd%>(row.label2);
        if(tipe == 2) sqlHtml += " AND p.tahun_ajaran " + safeSqlStr<%=rnd%>(row.label1) + " AND p.ganjil_genap " + safeSqlStr<%=rnd%>(row.label2);
        if(tipe == 3) sqlHtml += " AND ('Semester ' || CAST(COALESCE(p.smt, 0) AS VARCHAR)) " + safeSqlStr<%=rnd%>(row.label1);
        if(tipe == 4) sqlHtml += " AND p.program " + safeSqlStr<%=rnd%>(row.label1) + " AND j.nama " + safeSqlStr<%=rnd%>(row.label2);
        if(tipe == 5) sqlHtml += " AND p.hari " + safeSqlStr<%=rnd%>(row.label1);
        if(tipe == 6) sqlHtml += " AND r.nama " + safeSqlStr<%=rnd%>(row.label1);
        if(tipe == 7) sqlHtml += " AND p.hari " + safeSqlStr<%=rnd%>(row.label1) + " AND (COALESCE(p.waktu_mulai, '-') || ' - ' || COALESCE(p.waktu_selesai, '-')) " + safeSqlStr<%=rnd%>(row.label2);
        if(tipe == 8) sqlHtml += " AND p.kelasref " + safeSqlStr<%=rnd%>(row.label1);
        if(tipe == 9) sqlHtml += " AND k.nama " + safeSqlStr<%=rnd%>(row.label1);
        if(tipe == 10) sqlHtml += " AND jp.nama " + safeSqlStr<%=rnd%>(row.label1);
        if(tipe == 11) sqlHtml += " AND mp.nama " + safeSqlStr<%=rnd%>(row.label1);
        if(tipe == 12) sqlHtml += (row.label1 === 'Sudah Final (Dikunci)' ? " AND p.dikunci IS NOT NULL " : " AND p.dikunci IS NULL ");
        if(tipe == 13) sqlHtml += " AND je.nama " + safeSqlStr<%=rnd%>(row.label1);
        if(tipe == 14) sqlHtml += (row.label1 === 'Dosen Tunggal' ? " AND p.dosen2 IS NULL " : " AND p.dosen2 IS NOT NULL ");
        
        // MATAKULIAH GROUP (22-25)
        if(tipe == 22) sqlHtml += " AND (COALESCE(m.kode, '-') || ' - ' || COALESCE(m.nama, '-')) " + safeSqlStr<%=rnd%>(row.label1);
        if(tipe == 23) sqlHtml += " AND (CAST(COALESCE(m.sks, 0) AS VARCHAR) || ' SKS') " + safeSqlStr<%=rnd%>(row.label1);
        if(tipe == 24) sqlHtml += (row.label1 === 'Wajib' ? " AND m.status = 'Wajib' " : " AND (m.status != 'Wajib' OR m.status IS NULL) ");
        if(tipe == 25) sqlHtml += " AND km.nama " + safeSqlStr<%=rnd%>(row.label1);
        
        sqlJson = sqlHtml + " ORDER BY p.tahun_ajaran DESC, p.ganjil_genap ASC, m.nama ASC";
        sqlHtml += " ORDER BY p.tahun_ajaran DESC, p.ganjil_genap ASC, m.nama ASC";
    } else {
        sqlJson = "SELECT d.nama as nama_dsn, j.nama as prodi, m.nama as nama_mk, m.sks, p.kelasref as kelas, r.nama as ruang, p.hari, p.waktu_mulai " + baseDsnJoinSql + globalFilter;
        sqlHtml = sqlJson;
        
        if(tipe == 15 || tipe == 16) sqlHtml += " AND d.nama " + safeSqlStr<%=rnd%>(row.label1);
        if(tipe == 17) sqlHtml += " AND d.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND p.hari " + safeSqlStr<%=rnd%>(row.label2);
        if(tipe == 18) sqlHtml += " AND d.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND (COALESCE(p.hari, 'Belum Diatur') || ', ' || COALESCE(p.waktu_mulai, '') || '-' || COALESCE(p.waktu_selesai, '')) " + safeSqlStr<%=rnd%>(row.label2);
        if(tipe == 19) sqlHtml += " AND d.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND ('Semester ' || CAST(COALESCE(p.smt, 0) AS VARCHAR)) " + safeSqlStr<%=rnd%>(row.label2);
        if(tipe == 20) sqlHtml += " AND d.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND p.kelasref " + safeSqlStr<%=rnd%>(row.label2);
        if(tipe == 21) sqlHtml += " AND d.nama " + safeSqlStr<%=rnd%>(row.label1) + " AND r.nama " + safeSqlStr<%=rnd%>(row.label2);
        
        sqlJson = sqlHtml + " ORDER BY d.nama ASC, m.nama ASC";
        sqlHtml += " ORDER BY d.nama ASC, m.nama ASC";
    }

    window.currentDetailSqlHtml<%=rnd%> = sqlHtml;
    detailDataCache<%=rnd%> = await fetchDataAPI<%=rnd%>(sqlJson);
    detailCurrentPage<%=rnd%> = 1;
    
    btn.innerHTML = oriContent; btn.disabled = false;
    let subTitle = currentConfig<%=rnd%>.title + " (" + row.total + (tipe === 16 ? " SKS)" : " Data)");
    tampilkanModalDetail<%=rnd%>(subTitle);
};

const tampilkanModalDetail<%=rnd%> = (title) => {
    const modalId = 'modalDetailStatistik<%=rnd%>';
    if(document.getElementById(modalId)) document.getElementById(modalId).remove();
    
    let theadHtml = globalTipeDetail<%=rnd%> === "perkuliahan" 
        ? "<tr><th class='text-center py-3' style='width: 50px;'>No</th><th class='text-center py-3'>Tahun & Smt</th><th class='py-3'>Program Studi</th><th class='py-3'>Nama Mata Kuliah</th><th class='text-center py-3'>SKS</th><th class='text-center py-3'>Kelas</th><th class='py-3'>Ruang</th><th class='text-center py-3'>Hari & Jam</th></tr>"
        : "<tr><th class='text-center py-3' style='width: 50px;'>No</th><th class='py-3'>Nama Dosen</th><th class='py-3'>Nama Mata Kuliah</th><th class='text-center py-3'>SKS</th><th class='py-3'>Program Studi</th><th class='text-center py-3'>Kelas</th><th class='py-3'>Ruang</th><th class='text-center py-3'>Hari & Jam</th></tr>";

    const modalHtml = "<div class='modal fade' id='" + modalId + "' tabindex='-1' aria-hidden='true' data-bs-backdrop='static'><div class='modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable'><div class='modal-content shadow-lg border-0 rounded-4'><div class='modal-header bg-light border-0 py-3'><h5 class='modal-title fw-bold text-dark'><i class='fas fa-list-alt text-primary me-2'></i>" + title + "</h5><div><button class='btn btn-sm btn-success rounded-pill fw-bold shadow-sm me-2 px-3' onclick='downloadDetailExcel<%=rnd%>()'><i class='fas fa-file-excel me-1'></i>Unduh Excel</button><button type='button' class='btn-close' data-bs-dismiss='modal' aria-label='Close'></button></div></div><div class='modal-body p-0'><div class='table-responsive'><table class='table table-hover table-striped align-middle mb-0' style='font-size: 0.9rem;'><thead class='bg-primary text-white'>" + theadHtml + "</thead><tbody id='tbodyDetail<%=rnd%>'></tbody></table></div></div><div class='modal-footer bg-white border-top p-3 d-flex justify-content-between align-items-center'><span class='small text-muted fw-bold' id='infoPaginasiDetail<%=rnd%>'></span><nav><ul class='pagination pagination-sm mb-0 shadow-sm' id='paginasiDetail<%=rnd%>'></ul></nav></div></div></div></div>";
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    const modalEl = new bootstrap.Modal(document.getElementById(modalId)); modalEl.show();
    document.getElementById(modalId).addEventListener('hidden.bs.modal', function () { this.remove(); });
    renderTabelDetail<%=rnd%>();
};

const renderTabelDetail<%=rnd%> = async () => {
    const tbody = document.getElementById("tbodyDetail<%=rnd%>");
    if(detailDataCache<%=rnd%>.length === 0) {
        tbody.innerHTML = "<tr><td colspan='8' class='text-center text-muted p-4'>Tidak ada data rincian ditemukan</td></tr>";
        document.getElementById("infoPaginasiDetail<%=rnd%>").innerText = "Menampilkan 0 data";
        document.getElementById("paginasiDetail<%=rnd%>").innerHTML = ""; return;
    }
    tbody.innerHTML = "<tr><td colspan='8' class='text-center p-4'><span class='spinner-border text-primary'></span></td></tr>";
    const startIdx = (detailCurrentPage<%=rnd%> - 1) * 10;
    const endIdx = Math.min(startIdx + 10, detailDataCache<%=rnd%>.length);
    const formData = new URLSearchParams(); formData.append("action_jsp", "get_html_detail"); formData.append("sql", window.currentDetailSqlHtml<%=rnd%>); formData.append("tipe_detail", globalTipeDetail<%=rnd%>); formData.append("start", startIdx); formData.append("limit", 10);
    try {
        const res = await fetch('<%=Common.ROOT%>/dsh?hanya_tampil_jsp=true&p=pagesmasterperkuliahanzul&s=_statistik_perkuliahan&baru=true&rnd=<%=rnd%>', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: formData.toString() });
        tbody.innerHTML = await res.text();
    } catch (e) { tbody.innerHTML = "<tr><td colspan='8' class='text-center text-danger p-3'>Gagal menarik data detail.</td></tr>"; }
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
    if(globalTipeDetail<%=rnd%> === "perkuliahan") {
        csv += "No;Tahun Akademik;Semester;Program Studi;Nama Mata Kuliah;SKS;Kelas;Ruang;Hari & Jam\\n";
        detailDataCache<%=rnd%>.forEach((row, i) => {
            let cols = [ i+1, row.tahun_ajaran || "-", row.ganjil_genap || "-", row.prodi || "-", row.nama_mk || "-", row.sks || "0", row.kelas || "-", row.ruang || "-", (row.hari || "-") + " " + (row.waktu_mulai || "") ];
            csv += cols.map(v => '"' + String(v).replace(/"/g, '""') + '"').join(";") + "\n";
        });
    } else {
        csv += "No;Nama Dosen;Nama Mata Kuliah;SKS;Program Studi;Kelas;Ruang;Hari & Jam\\n";
        detailDataCache<%=rnd%>.forEach((row, i) => {
            let cols = [ i+1, row.nama_dsn || "-", row.nama_mk || "-", row.sks || "0", row.prodi || "-", row.kelas || "-", row.ruang || "-", (row.hari || "-") + " " + (row.waktu_mulai || "") ];
            csv += cols.map(v => '"' + String(v).replace(/"/g, '""') + '"').join(";") + "\n";
        });
    }
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' }); const link = document.createElement("a"); link.href = URL.createObjectURL(blob);
    link.download = "Detail_Data_Perkuliahan_" + new Date().getTime() + ".csv"; document.body.appendChild(link); link.click(); document.body.removeChild(link);
};

loadStatistik<%=rnd%>();
</script>