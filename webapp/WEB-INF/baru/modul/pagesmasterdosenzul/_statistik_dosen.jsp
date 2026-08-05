<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.BiodataDosen"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.StatusPegawai"%>
<%@page import="ais.database.model.IkatanKerjaDosen"%>
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
    // BLOK TANGKAP VARIABEL RND UNTUK MENCEGAH BENTROK ID
    // =========================================================================
    String rnd = request.getParameter("rnd");
    if (rnd == null || rnd.trim().isEmpty()) {
        rnd = Common.getGeneratedBarCode(7);
    }
    // Prefix unik untuk modul Dosen
    rnd = "Dsn" + rnd; 

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
            List<Dosen> list = sess.createSQLQuery(sql)
                                   .addEntity(Dosen.class)
                                   .setFirstResult(start)
                                   .setMaxResults(limit)
                                   .list();
            for (int i = 0; i < list.size(); i++) {
                Dosen d = list.get(i);

                String nidn = d.getNidn() != null && !d.getNidn().trim().isEmpty() ? d.getNidn() : "-";
                String nama = d.getNama() != null ? d.getNama() : "-";
                String prodi = d.getJurusan() != null ? d.getJurusan().getNama() : "-";
                String pendidikan = d.getPendidikan() != null ? d.getPendidikan().getNama() : "-";
                String jabatanFung = d.getJabatanFungsionalDosen() != null ? d.getJabatanFungsionalDosen().getNama() : "-";
                String statusPgw = d.getStatusPegawai() != null ? d.getStatusPegawai().getNama() : "-";
                
                String urlFoto = Common.ROOT + "/img/administrator-icon_default.png";
                // Pendekatan standar pengambilan foto
                try {
                    // Coba ambil dari method putPhoto bawaan Dosen
                    java.util.Map<String, String> mapParams = new java.util.HashMap<String, String>();
                    d.putPhoto(mapParams);
                    if(mapParams.containsKey("foto") && mapParams.get("foto") != null && !mapParams.get("foto").contains("administrator-icon_default.png")){
                        urlFoto = Common.ROOT + "/file?file=" + mapParams.get("foto");
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmasterdosenzul/_statistik_dosen.jsp:64");}
                
                %>
                <tr>
                    <td class='text-center'><%= (start + i + 1) %></td>
                    <td class='text-center'>
                        <img src="<%= urlFoto %>" class="rounded-circle img-thumbnail shadow-sm p-1" style="width: 40px; height: 40px; object-fit: cover;">
                    </td>
                    <td class='fw-bold text-primary'><%= nidn %></td>
                    <td class='fw-bold'><%= nama %></td>
                    <td><%= prodi %></td>
                    <td><%= pendidikan %></td>
                    <td><%= jabatanFung %></td>
                    <td class='text-center'><span class="badge bg-info text-dark"><%= statusPgw %></span></td>
                </tr>
                <%
            }
        } catch(Exception e) {
            out.print("<tr><td colspan='8' class='text-center text-danger'>" + Common.getBahasaConfig("Terjadi kesalahan kueri data rincian dosen.") + "</td></tr>");
        } finally {
            try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmasterdosenzul/_statistik_dosen.jsp:84");}
            try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmasterdosenzul/_statistik_dosen.jsp:85");}
            HibernateUtil.closeSession();
        }
        return; 
    }

    // =========================================================================
    // BLOK HALAMAN UTAMA STATISTIK DOSEN
    // =========================================================================
    List<Fakultas> listFakultas = new ArrayList<Fakultas>();
    List<Jurusan> listJurusan = new ArrayList<Jurusan>();
    List<StatusPegawai> listStatusPegawai = new ArrayList<StatusPegawai>();
    List<IkatanKerjaDosen> listIkatanKerja = new ArrayList<IkatanKerjaDosen>();
    
    Session sessFilter = HibernateUtil.openSession();
    try {
        listFakultas = ConstantValues.simpleList(sessFilter.createCriteria(Fakultas.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Fakultas.class);
        listJurusan = ConstantValues.simpleList(sessFilter.createCriteria(Jurusan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Jurusan.class);
        listStatusPegawai = ConstantValues.simpleList(sessFilter.createCriteria(StatusPegawai.class).addOrder(Order.asc("nama")), StatusPegawai.class);
        listIkatanKerja = ConstantValues.simpleList(sessFilter.createCriteria(IkatanKerjaDosen.class).addOrder(Order.asc("nama")), IkatanKerjaDosen.class);
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pagesmasterdosenzul/_statistik_dosen.jsp:106");
    } finally {
        try { sessFilter.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmasterdosenzul/_statistik_dosen.jsp:108");}
        try { sessFilter.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmasterdosenzul/_statistik_dosen.jsp:109");}
        HibernateUtil.closeSession();
    }
%>

<div class="container-fluid p-0 animate__animated animate__fadeIn">
    <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3">
        <h5 class="mb-0 fw-bold text-primary">
            <i class="fas fa-chalkboard-teacher me-2"></i><%= Common.getBahasaConfig("Dashboard Statistik Tenaga Pendidik & Dosen") %>
        </h5>
    </div>

    <div class="card shadow-sm border-0 rounded-4 mb-4">
        <div class="card-body bg-light rounded-4 p-4">
            <div class="row g-3">
                <div class="col-md-12 mb-2">
                    <label class="form-label fw-bold text-secondary small"><i class="fas fa-list me-1"></i><%= Common.getBahasaConfig("Pilih Jenis Laporan:") %></label>
                    <select class="form-select border-primary shadow-sm fw-bold" id="jenisLaporan<%=rnd%>" onchange="loadStatistik<%=rnd%>()">
                        <optgroup label="<%= Common.getBahasaConfig("Data Pegawai & Akademik Dosen") %>">
                            <option value="1"><%= Common.getBahasaConfig("1. Sebaran per Golongan Pegawai (Yayasan)") %></option>
                            <option value="2"><%= Common.getBahasaConfig("2. Sebaran per Golongan Kepangkatan PNS") %></option>
                            <option value="3"><%= Common.getBahasaConfig("3. Sebaran per Jenjang Pendidikan Terakhir") %></option>
                            <option value="4"><%= Common.getBahasaConfig("4. Sebaran per Program Studi (Homebase)") %></option>
                            <option value="5"><%= Common.getBahasaConfig("5. Sebaran per Fakultas") %></option>
                            <option value="6"><%= Common.getBahasaConfig("6. Sebaran per Ikatan Kerja Dosen") %></option>
                            <option value="7"><%= Common.getBahasaConfig("7. Sebaran per Status Kepegawaian") %></option>
                            <option value="8"><%= Common.getBahasaConfig("8. Sebaran per Jenis Pendidik & Tenaga Kependidikan") %></option>
                            <option value="9"><%= Common.getBahasaConfig("9. Sebaran per Lembaga Pengangkat") %></option>
                            <option value="10"><%= Common.getBahasaConfig("10. Sebaran per Sumber Gaji") %></option>
                            <option value="11"><%= Common.getBahasaConfig("11. Sebaran per Jabatan Fungsional Akademik (JFA)") %></option>
                            <option value="12"><%= Common.getBahasaConfig("12. Sebaran per Status Pegawai (Aktif/Cuti/Tugas Belajar)") %></option>
                        </optgroup>
                        <optgroup label="<%= Common.getBahasaConfig("Data Biodata Dosen") %>">
                            <option value="13"><%= Common.getBahasaConfig("13. Sebaran per Pekerjaan Ayah") %></option>
                            <option value="14"><%= Common.getBahasaConfig("14. Sebaran per Pekerjaan Ibu") %></option>
                            <option value="15"><%= Common.getBahasaConfig("15. Sebaran per Riwayat Menetap di Luar Negeri") %></option>
                            <option value="16"><%= Common.getBahasaConfig("16. Sebaran per Kewarganegaraan") %></option>
                            <option value="17"><%= Common.getBahasaConfig("17. Sebaran per Agama") %></option>
                            <option value="18"><%= Common.getBahasaConfig("18. Sebaran per Kecamatan Tempat Tinggal") %></option>
                            <option value="19"><%= Common.getBahasaConfig("19. Sebaran per Kota/Kabupaten Tempat Tinggal") %></option>
                            <option value="20"><%= Common.getBahasaConfig("20. Sebaran per Provinsi Tempat Tinggal") %></option>
                            <option value="21"><%= Common.getBahasaConfig("21. Sebaran per Pekerjaan Suami/Istri") %></option>
                        </optgroup>
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
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-user-tag me-1"></i> <%= Common.getBahasaConfig("Status Pegawai") %></label>
                    <select id="filterStatusPegawaiStat<%=rnd%>" class="form-select form-select-sm border-info shadow-none" onchange="loadStatistik<%=rnd%>()">
                        <option value=""><%= Common.getBahasaConfig("Semua Status Pegawai") %></option>
                        <% for(StatusPegawai sp : listStatusPegawai) { %><option value="<%= sp.getId() %>"><%= sp.getNama() %></option><% } %>
                    </select>
                </div>

                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-briefcase me-1"></i> <%= Common.getBahasaConfig("Ikatan Kerja") %></label>
                    <select id="filterIkatanKerjaStat<%=rnd%>" class="form-select form-select-sm border-info shadow-none" onchange="loadStatistik<%=rnd%>()">
                        <option value=""><%= Common.getBahasaConfig("Semua Ikatan Kerja") %></option>
                        <% for(IkatanKerjaDosen ikd : listIkatanKerja) { %><option value="<%= ikd.getId() %>"><%= ikd.getNama() %></option><% } %>
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
            <div style="position: relative; height: 350px; width: 100%;"><canvas id="chartDosen<%=rnd%>"></canvas></div>
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
// KONFIGURASI SQL REKAPITULASI DOSEN
// ==========================================
const baseJoinSql = " FROM dosen d LEFT JOIN jurusan j ON d.jurusan = j.id LEFT JOIN fakultas f ON j.fakultas = f.id LEFT JOIN status_pegawai sp ON d.status_pegawai = sp.id LEFT JOIN ikatan_kerja_dosen ik ON d.ikatan_kerja_dosen = ik.id ";
const baseBiodataJoinSql = " LEFT JOIN biodata_dosen bd ON bd.dosen = d.id ";

const reportConfigs<%=rnd%> = {
    "1": {
        title: "<%= Common.getBahasaConfig("Sebaran per Golongan Pegawai (Yayasan)") %>",
        columns: ["Golongan Pegawai", "Total Dosen"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(gp.nama, 'Tidak Ada Golongan') as label1, COUNT(d.id) as total " + baseJoinSql + " LEFT JOIN employ.golongan gp ON d.golongan_pegawai = gp.id WHERE d.id IS NOT NULL {FILTER} GROUP BY gp.nama ORDER BY gp.nama ASC"
    },
    "2": {
        title: "<%= Common.getBahasaConfig("Sebaran per Golongan Kepangkatan PNS") %>",
        columns: ["Golongan PNS", "Total Dosen"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(gpns.nama, 'Tidak Ada Golongan/Bukan PNS') as label1, COUNT(d.id) as total " + baseJoinSql + " LEFT JOIN golongan_pns gpns ON d.golongan_pns = gpns.id WHERE d.id IS NOT NULL {FILTER} GROUP BY gpns.nama ORDER BY gpns.nama ASC"
    },
    "3": {
        title: "<%= Common.getBahasaConfig("Sebaran per Jenjang Pendidikan Terakhir") %>",
        columns: ["Pendidikan Terakhir", "Total Dosen"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(p.nama, 'Tidak Diketahui') as label1, COUNT(d.id) as total " + baseJoinSql + " LEFT JOIN employ.pendidikan p ON d.pendidikan = p.id WHERE d.id IS NOT NULL {FILTER} GROUP BY p.nama ORDER BY p.nama ASC"
    },
    "4": {
        title: "<%= Common.getBahasaConfig("Sebaran per Program Studi (Homebase)") %>",
        columns: ["Program Studi (Homebase)", "Total Dosen"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(j.nama, 'Belum Diatur') as label1, COUNT(d.id) as total " + baseJoinSql + " WHERE d.id IS NOT NULL {FILTER} GROUP BY j.nama ORDER BY j.nama ASC"
    },
    "5": {
        title: "<%= Common.getBahasaConfig("Sebaran per Fakultas") %>",
        columns: ["Fakultas", "Total Dosen"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(f.nama, 'Belum Diatur') as label1, COUNT(d.id) as total " + baseJoinSql + " WHERE d.id IS NOT NULL {FILTER} GROUP BY f.nama ORDER BY f.nama ASC"
    },
    "6": {
        title: "<%= Common.getBahasaConfig("Sebaran per Ikatan Kerja Dosen") %>",
        columns: ["Ikatan Kerja", "Total Dosen"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(ik.nama, 'Belum Diatur') as label1, COUNT(d.id) as total " + baseJoinSql + " WHERE d.id IS NOT NULL {FILTER} GROUP BY ik.nama ORDER BY ik.nama ASC"
    },
    "7": {
        title: "<%= Common.getBahasaConfig("Sebaran per Status Kepegawaian") %>",
        columns: ["Status Kepegawaian", "Total Dosen"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(sk.nama, 'Belum Diatur') as label1, COUNT(d.id) as total " + baseJoinSql + " LEFT JOIN status_kepegawaian sk ON d.status_kepegawaian = sk.id WHERE d.id IS NOT NULL {FILTER} GROUP BY sk.nama ORDER BY sk.nama ASC"
    },
    "8": {
        title: "<%= Common.getBahasaConfig("Sebaran per Jenis Pendidik & Tenaga Kependidikan") %>",
        columns: ["Jenis Pendidik/Kependidikan", "Total Dosen"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(jp.nama, 'Belum Diatur') as label1, COUNT(d.id) as total " + baseJoinSql + " LEFT JOIN jenis_pendidik_dan_tenaga_kependidikan jp ON d.jenis_pendidik_dan_tenaga_kependidikan = jp.id WHERE d.id IS NOT NULL {FILTER} GROUP BY jp.nama ORDER BY jp.nama ASC"
    },
    "9": {
        title: "<%= Common.getBahasaConfig("Sebaran per Lembaga Pengangkat") %>",
        columns: ["Lembaga Pengangkat", "Total Dosen"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(lp.nama, 'Belum Diatur') as label1, COUNT(d.id) as total " + baseJoinSql + " LEFT JOIN lembaga_pengangkat lp ON d.lembaga_pengangkat = lp.id WHERE d.id IS NOT NULL {FILTER} GROUP BY lp.nama ORDER BY lp.nama ASC"
    },
    "10": {
        title: "<%= Common.getBahasaConfig("Sebaran per Sumber Gaji") %>",
        columns: ["Sumber Gaji", "Total Dosen"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(sg.nama, 'Belum Diatur') as label1, COUNT(d.id) as total " + baseJoinSql + " LEFT JOIN sumber_gaji sg ON d.sumber_gaji = sg.id WHERE d.id IS NOT NULL {FILTER} GROUP BY sg.nama ORDER BY sg.nama ASC"
    },
    "11": {
        title: "<%= Common.getBahasaConfig("Sebaran per Jabatan Fungsional Akademik (JFA)") %>",
        columns: ["Jabatan Fungsional", "Total Dosen"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(jf.nama, 'Belum Memiliki JFA') as label1, COUNT(d.id) as total " + baseJoinSql + " LEFT JOIN jabatan_fungsional_dosen jf ON d.jabatan_fungsional_dosen = jf.id WHERE d.id IS NOT NULL {FILTER} GROUP BY jf.nama ORDER BY jf.nama ASC"
    },
    "12": {
        title: "<%= Common.getBahasaConfig("Sebaran per Status Pegawai") %>",
        columns: ["Status Pegawai", "Total Dosen"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(sp.nama, 'Belum Diatur') as label1, COUNT(d.id) as total " + baseJoinSql + " WHERE d.id IS NOT NULL {FILTER} GROUP BY sp.nama ORDER BY sp.nama ASC"
    },
    "13": {
        title: "<%= Common.getBahasaConfig("Sebaran per Pekerjaan Ayah") %>",
        columns: ["Pekerjaan Ayah", "Total Dosen"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(pa.nama, 'Tidak Diketahui') as label1, COUNT(d.id) as total " + baseJoinSql + baseBiodataJoinSql + " LEFT JOIN pekerjaan pa ON bd.id_pekerjaan_ayah = pa.id WHERE d.id IS NOT NULL {FILTER} GROUP BY pa.nama ORDER BY pa.nama ASC"
    },
    "14": {
        title: "<%= Common.getBahasaConfig("Sebaran per Pekerjaan Ibu") %>",
        columns: ["Pekerjaan Ibu", "Total Dosen"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(pi.nama, 'Tidak Diketahui') as label1, COUNT(d.id) as total " + baseJoinSql + baseBiodataJoinSql + " LEFT JOIN pekerjaan pi ON bd.id_pekerjaan_ibu = pi.id WHERE d.id IS NOT NULL {FILTER} GROUP BY pi.nama ORDER BY pi.nama ASC"
    },
    "15": {
        title: "<%= Common.getBahasaConfig("Sebaran per Riwayat Menetap di Luar Negeri") %>",
        columns: ["Riwayat Luar Negeri", "Total Dosen"], keys: ["label1", "total"],
        sql: "SELECT CASE WHEN bd.pernah_menetap_di_luar_negeri = 1 THEN 'Pernah Menetap' ELSE 'Belum/Tidak Pernah' END as label1, COUNT(d.id) as total " + baseJoinSql + baseBiodataJoinSql + " WHERE d.id IS NOT NULL {FILTER} GROUP BY bd.pernah_menetap_di_luar_negeri"
    },
    "16": {
        title: "<%= Common.getBahasaConfig("Sebaran per Kewarganegaraan") %>",
        columns: ["Negara Asal", "Total Dosen"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(n.nama_negara, 'Indonesia') as label1, COUNT(d.id) as total " + baseJoinSql + baseBiodataJoinSql + " LEFT JOIN negara n ON bd.negara = n.id WHERE d.id IS NOT NULL {FILTER} GROUP BY n.nama_negara ORDER BY n.nama_negara ASC"
    },
    "17": {
        title: "<%= Common.getBahasaConfig("Sebaran per Agama") %>",
        columns: ["Agama", "Total Dosen"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(a.nama, 'Tidak Diketahui') as label1, COUNT(d.id) as total " + baseJoinSql + baseBiodataJoinSql + " LEFT JOIN agama a ON bd.id_agama = a.id WHERE d.id IS NOT NULL {FILTER} GROUP BY a.nama ORDER BY a.nama ASC"
    },
    "18": {
        title: "<%= Common.getBahasaConfig("Sebaran per Kecamatan Tempat Tinggal") %>",
        columns: ["Kecamatan", "Total Dosen"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(kec.nama, 'Tidak Diketahui') as label1, COUNT(d.id) as total " + baseJoinSql + baseBiodataJoinSql + " LEFT JOIN wilayah kec ON bd.kecamatan_wilayah = kec.id WHERE d.id IS NOT NULL {FILTER} GROUP BY kec.nama ORDER BY kec.nama ASC"
    },
    "19": {
        title: "<%= Common.getBahasaConfig("Sebaran per Kota/Kabupaten Tempat Tinggal") %>",
        columns: ["Kota/Kabupaten", "Total Dosen"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(k.nama, 'Tidak Diketahui') as label1, COUNT(d.id) as total " + baseJoinSql + baseBiodataJoinSql + " LEFT JOIN kota k ON bd.kota = k.id WHERE d.id IS NOT NULL {FILTER} GROUP BY k.nama ORDER BY k.nama ASC"
    },
    "20": {
        title: "<%= Common.getBahasaConfig("Sebaran per Provinsi Tempat Tinggal") %>",
        columns: ["Provinsi", "Total Dosen"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(pr.nama, 'Tidak Diketahui') as label1, COUNT(d.id) as total " + baseJoinSql + baseBiodataJoinSql + " LEFT JOIN propinsi pr ON bd.propinsi = pr.id WHERE d.id IS NOT NULL {FILTER} GROUP BY pr.nama ORDER BY pr.nama ASC"
    },
    "21": {
        title: "<%= Common.getBahasaConfig("Sebaran per Pekerjaan Suami/Istri") %>",
        columns: ["Pekerjaan Suami/Istri", "Total Dosen"], keys: ["label1", "total"],
        sql: "SELECT COALESCE(ps.nama, 'Tidak Diketahui') as label1, COUNT(d.id) as total " + baseJoinSql + baseBiodataJoinSql + " LEFT JOIN pekerjaan ps ON bd.pekerjaan_suami_istri = ps.id WHERE d.id IS NOT NULL {FILTER} GROUP BY ps.nama ORDER BY ps.nama ASC"
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
    const spId = document.getElementById('filterStatusPegawaiStat<%=rnd%>').value;
    const ikId = document.getElementById('filterIkatanKerjaStat<%=rnd%>').value;

    if (fakId) filterSql += " AND f.id = " + fakId + " ";
    if (prodiId) filterSql += " AND j.id = " + prodiId + " ";
    if (spId) filterSql += " AND d.status_pegawai = " + spId + " ";
    if (ikId) filterSql += " AND d.ikatan_kerja_dosen = " + ikId + " ";

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
    const ctx = document.getElementById('chartDosen<%=rnd%>').getContext('2d');
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
        data: { labels: labels, datasets: [{ label: '<%= Common.getBahasaConfigJS("Total Dosen") %>', data: totals, backgroundColor: 'rgba(20, 164, 77, 0.7)', borderColor: 'rgba(20, 164, 77, 1)', borderWidth: 1, borderRadius: 4 }] },
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
    link.download = "Statistik_Data_Dosen_" + new Date().getTime() + ".csv";
    document.body.appendChild(link); link.click(); document.body.removeChild(link);
};

// ==========================================
// MODAL POPUP DETAIL DOSEN
// ==========================================
let detailDataCache<%=rnd%> = [];
let detailCurrentPage<%=rnd%> = 1;

const safeSqlStr<%=rnd%> = (val) => {
    if(val === '-' || val === 'Belum Diatur' || val === 'Tidak Diketahui' || val === 'Belum Memiliki JFA' || val === 'Tidak Ada Golongan' || val === 'Tidak Ada Golongan/Bukan PNS') return " IS NULL ";
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
    if (tipe >= 13) {
        joinExtra += " LEFT JOIN biodata_dosen bd ON bd.dosen = d.id ";
    }
    if (tipe == 13) joinExtra += " LEFT JOIN pekerjaan pa ON bd.id_pekerjaan_ayah = pa.id ";
    if (tipe == 14) joinExtra += " LEFT JOIN pekerjaan pi ON bd.id_pekerjaan_ibu = pi.id ";
    if (tipe == 16) joinExtra += " LEFT JOIN negara n ON bd.negara = n.id ";
    if (tipe == 17) joinExtra += " LEFT JOIN agama a ON bd.id_agama = a.id ";
    if (tipe == 18) joinExtra += " LEFT JOIN wilayah kec ON bd.kecamatan_wilayah = kec.id ";
    if (tipe == 19) joinExtra += " LEFT JOIN kota k ON bd.kota = k.id ";
    if (tipe == 20) joinExtra += " LEFT JOIN propinsi pr ON bd.propinsi = pr.id ";
    if (tipe == 21) joinExtra += " LEFT JOIN pekerjaan ps ON bd.pekerjaan_suami_istri = ps.id ";

    let sqlJson = "SELECT d.nidn, d.nama, j.nama as prodi, p.nama as pendidikan, jf.nama as jabatan, sp.nama as status FROM dosen d LEFT JOIN jurusan j ON d.jurusan = j.id LEFT JOIN fakultas f ON d.fakultas = f.id LEFT JOIN status_pegawai sp ON d.status_pegawai = sp.id LEFT JOIN pendidikan p ON d.pendidikan = p.id LEFT JOIN jabatan_fungsional_dosen jf ON d.jabatan_fungsional_dosen = jf.id LEFT JOIN ikatan_kerja_dosen ik ON d.ikatan_kerja_dosen = ik.id " + joinExtra + " WHERE d.id IS NOT NULL " + globalFilter;
    let sqlHtml = "SELECT d.* FROM dosen d LEFT JOIN jurusan j ON d.jurusan = j.id LEFT JOIN fakultas f ON d.fakultas = f.id LEFT JOIN status_pegawai sp ON d.status_pegawai = sp.id LEFT JOIN pendidikan p ON d.pendidikan = p.id LEFT JOIN jabatan_fungsional_dosen jf ON d.jabatan_fungsional_dosen = jf.id LEFT JOIN ikatan_kerja_dosen ik ON d.ikatan_kerja_dosen = ik.id " + joinExtra + " WHERE d.id IS NOT NULL " + globalFilter;
    
    let filter = "";
    if(tipe == 1) filter += " AND (SELECT gp.nama FROM golongan gp WHERE d.golongan_pegawai = gp.id) " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 2) filter += " AND (SELECT gpns.nama FROM golongan_pns gpns WHERE d.golongan_pns = gpns.id) " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 3) filter += " AND p.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 4) filter += " AND j.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 5) filter += " AND f.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 6) filter += " AND ik.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 7) filter += " AND (SELECT sk.nama FROM status_kepegawaian sk WHERE d.status_kepegawaian = sk.id) " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 8) filter += " AND (SELECT jp.nama FROM jenis_pendidik_dan_tenaga_kependidikan jp WHERE d.jenis_pendidik_dan_tenaga_kependidikan = jp.id) " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 9) filter += " AND (SELECT lp.nama FROM lembaga_pengangkat lp WHERE d.lembaga_pengangkat = lp.id) " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 10) filter += " AND (SELECT sg.nama FROM sumber_gaji sg WHERE d.sumber_gaji = sg.id) " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 11) filter += " AND jf.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 12) filter += " AND sp.nama " + safeSqlStr<%=rnd%>(row.label1);
    
    // Biodata Filters
    if(tipe == 13) filter += " AND pa.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 14) filter += " AND pi.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 15) filter += (row.label1 === 'Pernah Menetap' ? " AND bd.pernah_menetap_di_luar_negeri = 1 " : " AND (bd.pernah_menetap_di_luar_negeri != 1 OR bd.pernah_menetap_di_luar_negeri IS NULL) ");
    if(tipe == 16) filter += " AND n.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 17) filter += " AND a.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 18) filter += " AND kec.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 19) filter += " AND k.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 20) filter += " AND pr.nama " + safeSqlStr<%=rnd%>(row.label1);
    if(tipe == 21) filter += " AND ps.nama " + safeSqlStr<%=rnd%>(row.label1);

    sqlJson += filter + " ORDER BY d.nama ASC";
    sqlHtml += filter + " ORDER BY d.nama ASC";

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
                                    "<th class='text-center py-3' style='width: 60px;'>Foto</th>" +
                                    "<th class='py-3'>NIDN</th><th class='py-3'>Nama Dosen</th>" +
                                    "<th class='py-3'>Program Studi</th><th class='py-3'>Pendidikan</th>" +
                                    "<th class='py-3'>JFA</th><th class='text-center py-3'>Status</th>" +
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
        // Sesuaikan rute '/kepegawaian' ini dengan rute controller Dosen yang Anda gunakan
        const res = await fetch('<%=Common.ROOT%>/dsh?hanya_tampil_jsp=true&p=pagesmasterdosenzul&s=_statistik_dosen&baru=true&rnd=<%=rnd%>', {
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
    let csv = "\uFEFFNo;NIDN;Nama Dosen;Program Studi;Pendidikan;JFA;Status\n";
    detailDataCache<%=rnd%>.forEach((row, i) => {
        let cols = [ i+1, row.nidn || "-", row.nama || "-", row.prodi || "-", row.pendidikan || "-", row.jabatan || "-", row.status || "-" ];
        csv += cols.map(v => '"' + String(v).replace(/"/g, '""') + '"').join(";") + "\n";
    });
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement("a"); link.href = URL.createObjectURL(blob);
    link.download = "Detail_Data_Dosen_" + new Date().getTime() + ".csv";
    document.body.appendChild(link); link.click(); document.body.removeChild(link);
};

loadStatistik<%=rnd%>();
</script>