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
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    String keyword = request.getParameter("keyword");
    String taStart = request.getParameter("taStart");
    String taEnd = request.getParameter("taEnd");
    String tlStart = request.getParameter("tlStart");
    String tlEnd = request.getParameter("tlEnd");
    String fakultasId = request.getParameter("fakultasId");
    String prodiId = request.getParameter("prodiId");
    
    // Parameter Status Keluar
    String statusKeluarId = request.getParameter("statusKeluarId");
    // Jika null (pertama kali dimuat), set default ke Lulus (1).
    // Jika kosong (""), berarti dari combobox dipilih "Semua Status".
    if (statusKeluarId == null) {
        statusKeluarId = "1"; 
    }

    String rnd = request.getParameter("rnd"); 
    String pageStr = request.getParameter("page");
    
    if (rnd == null) rnd = "";
    int currentPage = (pageStr != null && !pageStr.isEmpty()) ? Integer.parseInt(pageStr) : 1;
    int limitPerPage = 6; 

    Session sess = HibernateUtil.openSession();
    List<Mahasiswa> pagedList = new ArrayList<Mahasiswa>();
    
    // Referensi List untuk Filter
    List<Fakultas> listFakultas = new ArrayList<Fakultas>();
    List<Jurusan> listJurusan = new ArrayList<Jurusan>();
    List<StatusKeluar> listStatusKeluar = new ArrayList<StatusKeluar>();
    
    int totalData = 0;
    int totalPages = 0;
    int startIdx = 0;

    try {
        // Ambil data referensi untuk Dropdown Filter
        listFakultas = ConstantValues.simpleList(sess.createCriteria(Fakultas.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Fakultas.class);
        listJurusan = ConstantValues.simpleList(sess.createCriteria(Jurusan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Jurusan.class);
        listStatusKeluar = ConstantValues.simpleList(sess.createCriteria(StatusKeluar.class).addOrder(Order.asc("nama")), StatusKeluar.class);

        Criteria criteria = sess.createCriteria(Mahasiswa.class, "m");
        
        // ===================================================
        // FILTER DASAR: STATUS KELUAR DAN AKTIF
        // ===================================================
        if (statusKeluarId != null && !statusKeluarId.trim().isEmpty()) {
            criteria.add(Restrictions.eq("m.statusKeluar.id", Long.parseLong(statusKeluarId)));
        } else {
            // Jika Combo "Semua Status" dipilih (empty string)
            criteria.add(Restrictions.isNotNull("m.statusKeluar"));
        }
        
        criteria.add(Restrictions.or(Restrictions.isNull("m.aktif"), Restrictions.eq("m.aktif", true)));
        
        // 1. PENCARIAN BERDASARKAN NAMA ATAU NIM
        if (keyword != null && !keyword.trim().isEmpty()) {
            criteria.add(Restrictions.or(
                Restrictions.ilike("m.nama", keyword, MatchMode.ANYWHERE),
                Restrictions.ilike("m.nim", keyword, MatchMode.ANYWHERE)
            ));
        }

        // 2. PENCARIAN RENTANG TAHUN ANGKATAN
        if (taStart != null && !taStart.trim().isEmpty() && taEnd != null && !taEnd.trim().isEmpty()) {
            criteria.add(Restrictions.between("m.tahunangkatan", Integer.parseInt(taStart), Integer.parseInt(taEnd)));
        } else if (taStart != null && !taStart.trim().isEmpty()) {
            criteria.add(Restrictions.ge("m.tahunangkatan", Integer.parseInt(taStart)));
        } else if (taEnd != null && !taEnd.trim().isEmpty()) {
            criteria.add(Restrictions.le("m.tahunangkatan", Integer.parseInt(taEnd)));
        }

        // 3. PENCARIAN RENTANG TAHUN LULUS
        if (tlStart != null && !tlStart.trim().isEmpty() && tlEnd != null && !tlEnd.trim().isEmpty()) {
            criteria.add(Restrictions.between("m.tahunLulus", Integer.parseInt(tlStart), Integer.parseInt(tlEnd)));
        } else if (tlStart != null && !tlStart.trim().isEmpty()) {
            criteria.add(Restrictions.ge("m.tahunLulus", Integer.parseInt(tlStart)));
        } else if (tlEnd != null && !tlEnd.trim().isEmpty()) {
            criteria.add(Restrictions.le("m.tahunLulus", Integer.parseInt(tlEnd)));
        }

        // 4 & 5. PENCARIAN BERDASARKAN FAKULTAS DAN JURUSAN (PRODI)
        if ((fakultasId != null && !fakultasId.trim().isEmpty()) || (prodiId != null && !prodiId.trim().isEmpty())) {
            criteria.createAlias("m.jurusan", "j");
            if (prodiId != null && !prodiId.trim().isEmpty()) {
                criteria.add(Restrictions.eq("j.id", Long.parseLong(prodiId)));
            }
            if (fakultasId != null && !fakultasId.trim().isEmpty()) {
                criteria.createAlias("j.fakultas", "f");
                criteria.add(Restrictions.eq("f.id", Long.parseLong(fakultasId)));
            }
        }
        
        // TAHAP 1: COUNT DATABASE
        criteria.setProjection(Projections.rowCount());
        Object countResult = criteria.uniqueResult();
        if (countResult != null) {
            totalData = ((Number) countResult).intValue();
        }
        
        // TAHAP 2: FETCH PAGINATION DATA
        criteria.setProjection(null);
        criteria.setResultTransformer(Criteria.ROOT_ENTITY);

        criteria.addOrder(Order.desc("m.tahunLulus"));
        criteria.addOrder(Order.asc("m.nama"));
        
        totalPages = (int) Math.ceil((double) totalData / limitPerPage);
        if (currentPage < 1) currentPage = 1;
        if (currentPage > totalPages && totalPages > 0) currentPage = totalPages;
        
        startIdx = (currentPage - 1) * limitPerPage;
        if (startIdx < 0) startIdx = 0;
        
        criteria.setFirstResult(startIdx);
        criteria.setMaxResults(limitPerPage);
        
        pagedList = ConstantValues.simpleList(criteria, Mahasiswa.class);
        
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/alumni/_daftar_alumni.jsp:141");
    } finally {
        try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/_daftar_alumni.jsp:143");}
        try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/_daftar_alumni.jsp:144");}
        HibernateUtil.closeSessionQuietly(sess);
    }
%>

<div>
    <div class="card shadow-sm border-0 rounded-4 mb-4">
        <div class="card-body bg-light rounded-4 p-4">
            <div class="d-flex align-items-center mb-3 border-bottom pb-2">
                <i class="fas fa-filter text-primary me-2 fa-lg"></i>
                <h6 class="fw-bold text-dark mb-0"><%= Common.getBahasaConfig("Filter Pencarian Lulusan & Alumni") %></h6>
            </div>
            
            <div class="row g-3">
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-font me-1"></i> <%= Common.getBahasaConfig("NIM / Nama") %></label>
                    <input type="text" id="filterKeyword<%=rnd%>" class="form-control form-control-sm border-info shadow-none" placeholder="<%= Common.getBahasaConfig("Cari NIM/Nama...") %>" value="<%= keyword != null ? keyword : "" %>" onkeydown="if(event.key === 'Enter') loadDaftarAlumni<%=rnd%>(1)">
                </div>
                
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-university me-1"></i> <%= Common.getBahasaConfig("Fakultas") %></label>
                    <select id="filterFakultas<%=rnd%>" class="form-select form-select-sm border-info shadow-none" onchange="
                        var fakId = this.value;
                        var prodiSelect = document.getElementById('filterProdi<%=rnd%>');
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
                        loadDaftarAlumni<%=rnd%>(1);
                    ">
                        <option value=""><%= Common.getBahasaConfig("Semua Fakultas") %></option>
                        <% for(Fakultas f : listFakultas) { %>
                            <option value="<%= f.getId() %>" <%= fakultasId != null && fakultasId.equals(f.getId().toString()) ? "selected" : "" %>><%= f.getNama() %></option>
                        <% } %>
                    </select>
                </div>
                
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-graduation-cap me-1"></i> <%= Common.getBahasaConfig("Program Studi") %></label>
                    <select id="filterProdi<%=rnd%>" class="form-select form-select-sm border-info shadow-none" onchange="loadDaftarAlumni<%=rnd%>(1)">
                        <option value="" data-fakultas=""><%= Common.getBahasaConfig("Semua Program Studi") %></option>
                        <% for(Jurusan j : listJurusan) { 
                             String fakIdStr = (j.getFakultas() != null) ? j.getFakultas().getId().toString() : "";
                             boolean isHidden = (fakultasId != null && !fakultasId.trim().isEmpty() && !fakultasId.equals(fakIdStr));
                        %>
                            <option value="<%= j.getId() %>" data-fakultas="<%= fakIdStr %>" 
                                    <%= prodiId != null && prodiId.equals(j.getId().toString()) ? "selected" : "" %> 
                                    style="<%= isHidden ? "display:none;" : "" %>"
                                    <%= isHidden ? "disabled" : "" %> >
                                <%= j.getNama() %>
                            </option>
                        <% } %>
                    </select>
                </div>
                
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-sign-out-alt me-1"></i> <%= Common.getBahasaConfig("Status Keluar") %></label>
                    <select id="filterStatusKeluar<%=rnd%>" class="form-select form-select-sm border-info shadow-none" onchange="loadDaftarAlumni<%=rnd%>(1)">
                        <option value=""><%= Common.getBahasaConfig("Semua Status") %></option>
                        <% for(StatusKeluar sk : listStatusKeluar) { %>
                            <option value="<%= sk.getId() %>" <%= statusKeluarId != null && statusKeluarId.equals(sk.getId().toString()) ? "selected" : "" %>><%= sk.getNama() %></option>
                        <% } %>
                    </select>
                </div>
                
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-calendar-alt me-1"></i> <%= Common.getBahasaConfig("Thn Masuk (Dari)") %></label>
                    <input type="number" id="filterTaStart<%=rnd%>" class="form-control form-control-sm border-info shadow-none" placeholder="Cth: 2015" value="<%= taStart != null ? taStart : "" %>" onkeydown="if(event.key === 'Enter') loadDaftarAlumni<%=rnd%>(1)">
                </div>
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-calendar-alt me-1"></i> <%= Common.getBahasaConfig("Thn Masuk (Sampai)") %></label>
                    <input type="number" id="filterTaEnd<%=rnd%>" class="form-control form-control-sm border-info shadow-none" placeholder="Cth: 2019" value="<%= taEnd != null ? taEnd : "" %>" onkeydown="if(event.key === 'Enter') loadDaftarAlumni<%=rnd%>(1)">
                </div>
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-calendar-check me-1"></i> <%= Common.getBahasaConfig("Thn Lulus (Dari)") %></label>
                    <input type="number" id="filterTlStart<%=rnd%>" class="form-control form-control-sm border-info shadow-none" placeholder="Cth: 2019" value="<%= tlStart != null ? tlStart : "" %>" onkeydown="if(event.key === 'Enter') loadDaftarAlumni<%=rnd%>(1)">
                </div>
                <div class="col-md-3">
                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-calendar-check me-1"></i> <%= Common.getBahasaConfig("Thn Lulus (Sampai)") %></label>
                    <input type="number" id="filterTlEnd<%=rnd%>" class="form-control form-control-sm border-info shadow-none" placeholder="Cth: 2023" value="<%= tlEnd != null ? tlEnd : "" %>" onkeydown="if(event.key === 'Enter') loadDaftarAlumni<%=rnd%>(1)">
                </div>
            </div>

            <div class="row mt-3 border-top pt-3">
                <div class="col-12 text-end">
                    <button class="btn btn-primary btn-sm rounded-pill px-4 fw-bold shadow-sm" onclick="loadDaftarAlumni<%=rnd%>(1)">
                        <i class="fas fa-search me-1"></i> <%= Common.getBahasaConfig("Terapkan Filter") %>
                    </button>
                </div>
            </div>
        </div>
    </div>

    <div class="row g-4 mb-3">
        <% 
        if (pagedList == null || pagedList.isEmpty()) { 
        %>
            <div class="col-12 p-5 text-center text-muted bg-light rounded-4 border">
                <i class="fas fa-users-slash fa-3x mb-3 opacity-50 d-block"></i>
                <h6><%= Common.getBahasaConfig("Data alumni tidak ditemukan dengan filter tersebut.") %></h6>
            </div>
        <% 
        } else { 
            java.text.SimpleDateFormat sdf = Common.dateFormat41.get();
            
            for(Mahasiswa m : pagedList) {
                String nim = m.getNim() != null ? m.getNim() : "-";
                String nama = m.getNama() != null ? m.getNama() : "Tanpa Nama";
                String prodi = m.getJurusan() != null ? m.getJurusan().getNama() : "-";
                String statusLulusStr = m.getStatusKeluar() != null ? m.getStatusKeluar().getNama() : "Tidak Diketahui";
                String thnLulus = m.getTahunLulus() != null ? m.getTahunLulus().toString() : "-";
                String tglLulus = m.getTanggalLulus() != null ? sdf.format(m.getTanggalLulus()) : "-";
                String judulSkripsi = m.getJudulSkripsi() != null && !m.getJudulSkripsi().trim().isEmpty() ? m.getJudulSkripsi() : "Tidak Ada Data Skripsi";
                
                String urlFotoMhs = "";
                try {
                    urlFotoMhs = ProfileImageUtil.getUrlFotoPengguna(new Tbmuser(m), 152, 114);
                } catch (Exception e) {
                    urlFotoMhs = Common.ROOT + "/img/administrator-icon_default.png";
                }
        %>
            <div class="col-md-6 col-lg-4 animate__animated animate__fadeIn">
                <div class="card h-100 shadow-sm border-0 rounded-4 card-alumni-<%=rnd%>">
                    <div class="card-body text-center p-4">
                        <div class="mb-3 position-relative d-inline-block">
                            <img src="<%= urlFotoMhs %>" alt="Foto <%= nama %>" class="rounded-circle img-thumbnail shadow-sm p-1" style="width: 110px; height: 110px; object-fit: cover; border-color: #0d6efd;">
                            <span class="position-absolute bottom-0 end-0 bg-success border border-light rounded-circle p-1" style="width: 25px; height: 25px;" title="<%= statusLulusStr %>">
                                <i class="fas fa-check text-white" style="font-size: 10px; display: block; margin-top: 2px;"></i>
                            </span>
                        </div>
                        <h6 class="fw-bold text-dark mb-1" style="line-height: 1.3;"><%= nama %></h6>
                        <p class="text-primary small mb-3 fw-bold"><i class="fas fa-id-card me-1"></i><%= nim %></p>
                        
                        <div class="text-start small text-secondary bg-light p-3 rounded-3">
                            <p class="mb-2 border-bottom pb-2"><i class="fas fa-graduation-cap me-2 text-warning"></i><strong class="text-dark">Prodi:</strong> <%= prodi %></p>
                            <p class="mb-2 border-bottom pb-2">
                                <i class="far fa-calendar-check me-2 text-success"></i>
                                <strong class="text-dark">Status (<%= thnLulus %>):</strong> <%= statusLulusStr %>
                            </p>
                            <div class="mt-2 text-dark" style="display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;" title="<%= judulSkripsi.replace("\"", "&quot;") %>">
                                <i class="fas fa-book me-1 text-info"></i><span class="fst-italic">"<%= judulSkripsi %>"</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        <% 
            } // end loop 
        %>
    </div>

    <% if (totalPages > 1) { %>
        <div class="d-flex justify-content-center mt-2">
            <nav>
                <ul class="pagination pagination-sm shadow-sm rounded-pill mb-0">
                    <li class="page-item <%= (currentPage == 1) ? "disabled" : "" %>">
                        <button class="page-link rounded-start-pill text-primary fw-bold" onclick="loadDaftarAlumni<%=rnd%>(<%= (currentPage - 1) %>)"><i class="fas fa-angle-left"></i></button>
                    </li>
                    <% for (int i = 1; i <= totalPages; i++) { 
                        if (i == 1 || i == totalPages || (i >= currentPage - 1 && i <= currentPage + 1)) {
                    %>
                        <li class="page-item <%= (currentPage == i) ? "active" : "" %>">
                            <button class="page-link <%= (currentPage == i) ? "bg-primary text-white border-primary fw-bold" : "text-secondary" %>" onclick="loadDaftarAlumni<%=rnd%>(<%=i%>)"><%=i%></button>
                        </li>
                    <%  } else if (i == currentPage - 2 || i == currentPage + 2) { %>
                        <li class="page-item disabled"><span class="page-link text-secondary">...</span></li>
                    <%  }
                    } %>
                    <li class="page-item <%= (currentPage == totalPages) ? "disabled" : "" %>">
                        <button class="page-link rounded-end-pill text-primary fw-bold" onclick="loadDaftarAlumni<%=rnd%>(<%= (currentPage + 1) %>)"><i class="fas fa-angle-right"></i></button>
                    </li>
                </ul>
            </nav>
        </div>
    <% } } %>
</div>

<style>
    .card-alumni-<%=rnd%> { transition: all 0.3s ease; }
    .card-alumni-<%=rnd%>:hover { transform: translateY(-5px); box-shadow: 0 10px 20px rgba(0,0,0,0.1) !important; }
</style>