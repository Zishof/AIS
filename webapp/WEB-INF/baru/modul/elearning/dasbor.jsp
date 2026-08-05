<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.sekolah.Yayasan"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // ====================================================================================
    // 1. OTENTIKASI & IDENTIFIKASI PERAN PENGGUNA
    // ====================================================================================
    String rnd = Common.getGeneratedBarCode(7);
    Tbmuser tbmuser = Common.getCurrentUser(request);
    
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.setStatus(401);
        return;
    }

    boolean isSekolahAtauYayasan = false;
    boolean isSivitas = false;
    
    if (tbmuser.ambilSekolah() != null || tbmuser.ambilYayasan() != null) {
        isSekolahAtauYayasan = true;
    }
    if (tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null || tbmuser.getSiswa() != null || tbmuser.ambilGuru() != null) {
        isSivitas = true;
    }
    
    int totalKolomTabel = isSivitas ? 6 : 5;
    
    boolean[] ptYa = Common.chekPtAtauSekolah(tbmuser);
    boolean apakahPerguruanTinggi = ptYa[0];
    boolean apakahSekolah = ptYa[1];
    String defKategori = apakahPerguruanTinggi ? "perkuliahan" : (apakahSekolah ? "jadwal_pelajaran" : "kegiatan");

    // ====================================================================================
    // 2. PERSIAPAN DATA PENYARING (TAHUN, SEMESTER, FAKULTAS, JURUSAN, YAYASAN, SEKOLAH)
    // ====================================================================================
    String defTa = "";
    String defSmt = "";
    
    List<Fakultas> listFakultas = new ArrayList<Fakultas>();
    List<Jurusan> listJurusan = new ArrayList<Jurusan>();
    List<Sekolah> listSekolah = new ArrayList<Sekolah>();
    List<Yayasan> listYayasan = new ArrayList<Yayasan>();

    Session sessFilter = null;
    try {
        sessFilter = HibernateUtil.openSession();
        
        // Pengambilan konfigurasi bawaan
        defTa = Common.getCurrentTahunAkademik();
        boolean isNowSmtGanjil = Common.isNowSemensterGanjil();
        defSmt = isNowSmtGanjil ? String.valueOf(Perkuliahan.GANJIL) : String.valueOf(Perkuliahan.GENAP);
        
        // Pengambilan daftar entitas pendidikan
        listFakultas = ConstantValues.simpleList(sessFilter.createCriteria(Fakultas.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Fakultas.class);
        listJurusan = ConstantValues.simpleList(sessFilter.createCriteria(Jurusan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Jurusan.class);
        
        if (isSekolahAtauYayasan || apakahSekolah) {
            listSekolah = ConstantValues.simpleList(sessFilter.createCriteria(Sekolah.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Sekolah.class);
            listYayasan = ConstantValues.simpleList(sessFilter.createCriteria(Yayasan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Yayasan.class);
        }
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/dasbor.jsp:76");
    } finally {
        // PENUTUPAN SESI SECARA KETAT UNTUK MENCEGAH KEBOCORAN MEMORI
        if(sessFilter != null && sessFilter.isOpen()) {
            try { 
                sessFilter.clear(); 
                sessFilter.disconnect(); 
                sessFilter.close(); 
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/dasbor.jsp:84");}
        }
        HibernateUtil.closeSessionQuietly(sessFilter);
    }
%>

<style>
    /* Desain Elemen Masukan (Input) */
    .input-elegan-<%=rnd%> { 
        font-size: 0.9rem; border-radius: 0.75rem; border: 1px solid #ced4da; 
        box-shadow: inset 0 2px 4px rgba(0,0,0,0.02); transition: all 0.3s ease-in-out; 
    }
    .input-elegan-<%=rnd%>:focus { border-color: #0d6efd; box-shadow: 0 0 0 0.25rem rgba(13,110,253,0.15); outline: none; }
    
    /* Desain Navigasi Tab (Pills) */
    .nav-tabs-custom-<%=rnd%> { padding: 0.5rem; background-color: #f8f9fa; border-radius: 50px; }
    .nav-tabs-custom-<%=rnd%> .nav-link {
        color: #6c757d; font-weight: 700; border: none; border-radius: 50px;
        padding: 10px 24px; transition: all 0.3s ease; margin: 0 4px;
    }
    .nav-tabs-custom-<%=rnd%> .nav-link:hover { background: #e9ecef; color: #495057; transform: translateY(-1px); }
    .nav-tabs-custom-<%=rnd%> .nav-link.active {
        background: linear-gradient(135deg, #0d6efd 0%, #0a58ca 100%); color: #ffffff;
        box-shadow: 0 4px 10px rgba(13,110,253,0.3); transform: translateY(-1px);
    }
    /* Mobile: compact pill tabs */
    @media (max-width: 767.98px) {
        .nav-tabs-custom-<%=rnd%> { border-radius: 12px; overflow-x: auto; display: flex; flex-wrap: nowrap; -webkit-overflow-scrolling: touch; scrollbar-width: none; }
        .nav-tabs-custom-<%=rnd%>::-webkit-scrollbar { display: none; }
        .nav-tabs-custom-<%=rnd%> .nav-link { padding: 7px 14px; font-size: 0.75rem; white-space: nowrap; margin: 0 2px; flex: 0 0 auto; }
        /* Compact header on mobile */
        #wrapper-dasbor-<%=rnd%> .card-header { padding: 0.75rem !important; }
        #wrapper-dasbor-<%=rnd%> .card-header h4 { font-size: 1rem !important; }
    }

    /* Pengaturan Header Tabel */
    .table-rekap-<%=rnd%> thead th { 
        text-transform: uppercase; font-size: 0.75rem; font-weight: 800; letter-spacing: 0.8px; 
        background-color: #f8f9fa; color: #495057; border-bottom: 2px solid #dee2e6; border-top: 1px solid #e9ecef;
        padding: 1.1rem 0.75rem; vertical-align: middle; white-space: nowrap;
    }
    .table-rekap-<%=rnd%> td { vertical-align: middle; font-size: 0.85rem; padding: 14px 12px; border-bottom: 1px solid #f1f3f5; }
    
    /* Paginasi Berbentuk Lingkaran Presisi */
    .custom-pagination-<%=rnd%> { display: flex; padding-left: 0; list-style: none; gap: 0.5rem; justify-content: center; align-items: center; margin-top: 1rem; }
    .custom-pagination-<%=rnd%> .page-link {
        display: flex; align-items: center; justify-content: center; width: 40px; height: 40px;
        border-radius: 50% !important; border: 1px solid #dee2e6; color: #0d6efd; font-weight: 700;
        background-color: #fff; transition: all 0.2s; text-decoration: none;
    }
    .custom-pagination-<%=rnd%> .page-link:hover { background-color: #0d6efd; color: #fff; transform: translateY(-3px); box-shadow: 0 5px 10px rgba(13,110,253,0.2); }
    .custom-pagination-<%=rnd%> .page-item.active .page-link { background: #0d6efd; color: #fff; border-color: #0d6efd; }
    .custom-pagination-<%=rnd%> .page-item.disabled .page-link { color: #adb5bd; background-color: #f8f9fa; cursor: not-allowed; }

    /* Efek Melayang (Hover Elevation) */
    .hover-shadow-sm { transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1); transform: translateY(0); }
    .hover-shadow-sm:hover { box-shadow: 0 10px 20px rgba(0, 0, 0, 0.08) !important; transform: translateY(-4px); }

    /* Progress bar tipis di dalam kartu */
    .dsb-prog-wrap-<%=rnd%> { height:5px; border-radius:99px; background:#e9ecef; overflow:hidden; }
    .dsb-prog-bar-<%=rnd%>  { height:100%; border-radius:99px; transition:width 1.1s ease; }

    /* Kartu Ringkasan (Mini Stat Card) */
    .mini-stat-card-<%=rnd%> { border-radius: 20px; background: #ffffff; position: relative; overflow: hidden; border: 1px solid #f1f3f5; transition: 0.3s; z-index: 1;}
    .mini-stat-card-<%=rnd%>:hover { transform: translateY(-5px); box-shadow: 0 10px 20px rgba(0,0,0,0.08); }
    .mini-stat-card-<%=rnd%>::before { content: ""; position: absolute; top: 0; left: 0; width: 6px; height: 100%; z-index: 2; border-radius: 20px 0 0 20px; }
    
    .border-left-primary::before { background-color: #0d6efd; }
    .border-left-info::before { background-color: #0dcaf0; }
    .border-left-warning::before { background-color: #ffc107; }
    .border-left-danger::before { background-color: #dc3545; }
    .border-left-success::before { background-color: #198754; }
    .border-left-purple::before { background-color: #6f42c1; }

    .icon-box-<%=rnd%> { 
        width: 48px; height: 48px; border-radius: 14px; display: flex; align-items: center; justify-content: center; 
        font-size: 1.3rem; transition: transform 0.3s ease;
    }
    .mini-stat-card-<%=rnd%>:hover .icon-box-<%=rnd%> { transform: scale(1.1) rotate(5deg); }
</style>

<div class="container-fluid py-4 px-xl-5" id="wrapper-dasbor-<%=rnd%>">
    
    <div class="row align-items-center mb-4 pb-3 border-bottom">
        <div class="col-12">
            <div class="d-flex justify-content-between align-items-center flex-wrap gap-3">
                <div class="d-flex align-items-center">
                    <div class="bg-primary text-white rounded-4 shadow-sm me-3 d-flex align-items-center justify-content-center hover-shadow-sm" style="width: 56px; height: 56px; background-image: linear-gradient(135deg, rgba(255,255,255,0.25), rgba(255,255,255,0));">
                        <i class="fas fa-chart-pie fa-fw fs-4"></i>
                    </div>
                    <div>
                        <h6 class="text-primary small fw-bolder mb-1" style="text-transform: uppercase; letter-spacing: 1px;"><%= Common.getBahasaConfig("Modul Analitik") %></h6>
                        <h4 class="fw-bold text-dark mb-0"><%= Common.getBahasaConfig("Dasbor Interaktif Pembelajaran") %></h4>
                    </div>
                </div>
                <button class="btn btn-primary bg-gradient rounded-pill shadow-sm fw-bold px-4 py-2 hover-shadow-sm border-0" onclick="muatDataDasbor<%=rnd%>(true)">
                    <i class="fas fa-sync-alt me-2" id="icon-refresh-<%=rnd%>"></i> <%=Common.getBahasaConfig("Segarkan Dasbor")%>
                </button>
            </div>
        </div>
    </div>

    <div class="row">
        <div class="col-12">
            
            <div class="card border-0 shadow-sm rounded-4 mb-4 bg-white hover-shadow-sm">
                <div class="card-body p-3 px-4">
                    <form id="form-search-<%=rnd%>" onsubmit="event.preventDefault(); muatDataDasbor<%=rnd%>(false);" class="m-0">
                        <div class="row align-items-end g-3">
                            
                            <div class="col-md-4 col-xl-2 mb-2">
                                <label class="text-muted mb-2 small fw-bold text-uppercase" style="letter-spacing: 0.5px;">
                                    <i class="far fa-calendar-alt me-1 text-primary"></i><%=Common.getBahasaConfig("Tahun Ajaran")%>
                                </label>
                                <select class="form-select input-elegan-<%=rnd%> py-2 fw-semibold text-dark" id="filter-ta-<%=rnd%>" onchange="muatDataDasbor<%=rnd%>(false)">
                                    <option value=""><%=Common.getBahasaConfig("Semua")%></option>
                                    <% if (Common.tahunAngkatans != null) { 
                                        for (String strTa : Common.tahunAngkatans) { 
                                            String selectedTa = (strTa != null && strTa.equals(defTa)) ? "selected" : ""; %>
                                        <option value="<%=strTa%>" <%=selectedTa%>><%=strTa%></option>
                                    <% } } %>
                                </select>
                            </div>
                            <div class="col-md-4 col-xl-2 mb-2">
                                <label class="text-muted mb-2 small fw-bold text-uppercase" style="letter-spacing: 0.5px;">
                                    <i class="fas fa-hourglass-half me-1 text-primary"></i><%=Common.getBahasaConfig("Semester")%>
                                </label>
                                <select class="form-select input-elegan-<%=rnd%> py-2 fw-semibold text-dark" id="filter-smt-<%=rnd%>" onchange="muatDataDasbor<%=rnd%>(false)">
                                    <option value=""><%=Common.getBahasaConfig("Semua")%></option>
                                    <option value="<%=Perkuliahan.GANJIL%>" <%=String.valueOf(Perkuliahan.GANJIL).equals(defSmt) ? "selected" : ""%>><%=Common.getBahasaConfig("Ganjil")%></option>
                                    <option value="<%=Perkuliahan.GENAP%>" <%=String.valueOf(Perkuliahan.GENAP).equals(defSmt) ? "selected" : ""%>><%=Common.getBahasaConfig("Genap")%></option>
                                    <option value="<%=Perkuliahan.SP%>" <%=String.valueOf(Perkuliahan.SP).equals(defSmt) ? "selected" : ""%>><%=Common.getBahasaConfig("Pendek")%></option>
                                </select>
                            </div>

                            <% if (apakahPerguruanTinggi && !isSivitas) { %>
                            <div class="col-md-4 col-xl-3 mb-2">
                                <label class="text-muted mb-2 small fw-bold text-uppercase" style="letter-spacing: 0.5px;">
                                    <i class="fas fa-university me-1 text-info"></i><%=Common.getBahasaConfig("Fakultas")%>
                                </label>
                                <select class="form-select input-elegan-<%=rnd%> py-2 fw-semibold text-dark" id="filter-fakultas-<%=rnd%>" onchange="ubahFakultas<%=rnd%>()">
                                    <option value=""><%=Common.getBahasaConfig("Semua Fakultas")%></option>
                                    <% for(Fakultas f : listFakultas) { %><option value="<%=f.getId()%>"><%=f.getNama()%></option><% } %>
                                </select>
                            </div>
                            <div class="col-md-6 col-xl-3 mb-2">
                                <label class="text-muted mb-2 small fw-bold text-uppercase" style="letter-spacing: 0.5px;">
                                    <i class="fas fa-graduation-cap me-1 text-info"></i><%=Common.getBahasaConfig("Jurusan")%>
                                </label>
                                <select class="form-select input-elegan-<%=rnd%> py-2 fw-semibold text-dark" id="filter-jurusan-<%=rnd%>" onchange="muatDataDasbor<%=rnd%>(false)">
                                    <option value=""><%=Common.getBahasaConfig("Semua Jurusan")%></option>
                                    <% for(Jurusan j : listJurusan) { %><option value="<%=j.getId()%>"><%=j.getNama()%></option><% } %>
                                </select>
                            </div>
                            <% } %>

                            <% if (isSekolahAtauYayasan || apakahSekolah) { %>
                            <div class="col-md-4 col-xl-3 mb-2">
                                <label class="text-muted mb-2 small fw-bold text-uppercase" style="letter-spacing: 0.5px;">
                                    <i class="fas fa-building me-1 text-success"></i><%=Common.getBahasaConfig("Yayasan")%>
                                </label>
                                <select class="form-select input-elegan-<%=rnd%> py-2 fw-semibold text-dark" id="filter-yayasan-<%=rnd%>" onchange="ubahYayasan<%=rnd%>()">
                                    <option value=""><%=Common.getBahasaConfig("Semua Yayasan")%></option>
                                    <% for(Yayasan y : listYayasan) { %><option value="<%=y.getId()%>"><%=y.getNama()%></option><% } %>
                                </select>
                            </div>
                            <div class="col-md-6 col-xl-3 mb-2">
                                <label class="text-muted mb-2 small fw-bold text-uppercase" style="letter-spacing: 0.5px;">
                                    <i class="fas fa-school me-1 text-success"></i><%=Common.getBahasaConfig("Sekolah")%>
                                </label>
                                <select class="form-select input-elegan-<%=rnd%> py-2 fw-semibold text-dark" id="filter-sekolah-<%=rnd%>" onchange="muatDataDasbor<%=rnd%>(false)">
                                    <option value=""><%=Common.getBahasaConfig("Semua Sekolah")%></option>
                                    <% for(Sekolah s : listSekolah) { %><option value="<%=s.getId()%>"><%=s.getNama()%></option><% } %>
                                </select>
                            </div>
                            <% } %>

                            <div class="col-md-12 col-xl-2 mb-2 d-flex align-items-end justify-content-end">
                                <button type="button" class="btn btn-outline-secondary rounded-pill fw-bold px-3 py-2 me-2" onclick="bersihkanFilter<%=rnd%>()" title="<%=Common.getBahasaConfig("Atur Ulang Penyaring")%>">
                                    <i class="fas fa-undo"></i>
                                </button>
                                <button type="submit" class="btn btn-primary rounded-pill fw-bold px-4 py-2 w-100 shadow-sm">
                                    <i class="fas fa-search me-2"></i><%=Common.getBahasaConfig("Terapkan")%>
                                </button>
                            </div>
                        </div>
                    </form>
                </div>
            </div>

            <!-- Skor kesehatan keterlaksanaan keseluruhan -->
            <div id="dasbor-health-<%=rnd%>" class="card border-0 shadow-sm rounded-4 mb-4 bg-white" style="display:none">
                <div class="card-body p-3 px-4">
                    <div class="d-flex align-items-center justify-content-between mb-2 flex-wrap gap-2">
                        <h6 class="mb-0 fw-bold"><i class="fas fa-heartbeat me-2 text-primary"></i><%=Common.getBahasaConfig("Skor Keterlaksanaan Pembelajaran")%></h6>
                        <span id="dasbor-health-pct-<%=rnd%>" class="fw-bold" style="font-size:1.25rem;color:#0d6efd">—</span>
                    </div>
                    <div class="dsb-prog-wrap-<%=rnd%>" style="height:8px">
                        <div id="dasbor-health-bar-<%=rnd%>" class="dsb-prog-bar-<%=rnd%>" style="width:0%;background:#0d6efd"></div>
                    </div>
                    <div class="d-flex justify-content-between mt-1">
                        <small class="text-muted" id="dasbor-health-sub-<%=rnd%>">Menghitung…</small>
                        <small class="text-muted">Target: semua komponen aktif</small>
                    </div>
                </div>
            </div>

            <div class="row row-cols-1 row-cols-md-2 row-cols-xl-3 g-4 mb-4">
                <% 
                    String[] statKeys = {"pertemuan", "materi", "tugas", "ujian", "video", "audio"};
                    String[] statLabels = {
                        Common.getBahasaConfig("Pertemuan"), 
                        Common.getBahasaConfig("Materi"), 
                        Common.getBahasaConfig("Tugas"), 
                        Common.getBahasaConfig("Ujian"), 
                        Common.getBahasaConfig("Video"), 
                        Common.getBahasaConfig("Audio")
                    };
                    String[] statIcons = {"fas fa-chalkboard-teacher", "fas fa-book-open", "fas fa-tasks", "fas fa-file-signature", "fas fa-play-circle", "fas fa-headphones-alt"};
                    String[] textColors = {"text-primary", "text-info", "text-warning", "text-danger", "text-success", "text-purple"};
                    String[] bgBoxColors = {"rgba(13,110,253,0.15)", "rgba(13,202,240,0.15)", "rgba(255,193,7,0.15)", "rgba(220,53,69,0.15)", "rgba(25,135,84,0.15)", "rgba(111,66,193,0.15)"};
                    String[] borderColors = {"border-left-primary", "border-left-info", "border-left-warning", "border-left-danger", "border-left-success", "border-left-purple"};
                    
                    for(int i=0; i<statKeys.length; i++) {
                %>
                <div class="col">
                    <div class="card border-0 shadow-sm mini-stat-card-<%=rnd%> <%=borderColors[i]%> h-100">
                        <div class="card-body p-4">
                            <div class="d-flex justify-content-between align-items-center mb-3 pb-2 border-bottom">
                                <div class="d-flex align-items-center">
                                    <div class="icon-box-<%=rnd%> me-3 shadow-sm" style="background-color: <%=bgBoxColors[i]%>;"><i class="<%=statIcons[i]%> <%=textColors[i]%>" <%=(i==5)?"style='color:#6f42c1 !important;'":""%>></i></div>
                                    <h6 class="fw-bolder text-dark text-uppercase mb-0" style="font-size: 0.8rem; letter-spacing: 0.5px;"><%=statLabels[i]%></h6>
                                </div>
                                <span class="badge bg-light text-secondary border px-3 py-2 rounded-pill fw-bold shadow-sm" id="count-<%=statKeys[i]%>-<%=rnd%>">
                                    <i class="fas fa-spinner fa-spin"></i>
                                </span>
                            </div>
                            <div class="d-flex align-items-center gap-2 mt-3 mb-1">
                                <div class="dsb-prog-wrap-<%=rnd%> flex-grow-1">
                                    <div class="dsb-prog-bar-<%=rnd%>" id="prog-<%=statKeys[i]%>-<%=rnd%>" style="width:0%;background:#dee2e6"></div>
                                </div>
                                <span id="pct-<%=statKeys[i]%>-<%=rnd%>" style="font-size:.68rem;font-weight:700;color:#adb5bd;white-space:nowrap;min-width:28px;text-align:right">—</span>
                            </div>
                            <div class="row g-1 mt-2 text-center">
                                <div class="col-4 border-end">
                                    <h4 class="fw-bold text-success mb-0" id="sum-done-<%=statKeys[i]%>-<%=rnd%>">0</h4>
                                    <small class="text-muted fw-bold" style="font-size:10px; text-transform:uppercase;"><%=Common.getBahasaConfig("Selesai")%></small>
                                </div>
                                <div class="col-4 border-end">
                                    <h4 class="fw-bold text-danger mb-0" id="sum-notdone-<%=statKeys[i]%>-<%=rnd%>">0</h4>
                                    <small class="text-muted fw-bold" style="font-size:10px; text-transform:uppercase;"><%=Common.getBahasaConfig("Belum")%></small>
                                </div>
                                <div class="col-4">
                                    <h4 class="fw-bold text-secondary mb-0" id="sum-total-<%=statKeys[i]%>-<%=rnd%>">0</h4>
                                    <small class="text-muted fw-bold" style="font-size:10px; text-transform:uppercase;"><%=Common.getBahasaConfig("Target")%></small>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <% } %>
            </div>

            <div class="mb-4">
                <ul class="nav nav-pills nav-tabs-custom-<%=rnd%> shadow-sm d-flex flex-nowrap overflow-auto" id="tabRekap-<%=rnd%>" role="tablist">
                    <% for(int i=0; i<statKeys.length; i++) { %>
                    <li class="nav-item flex-shrink-0">
                        <a class="nav-link <%=i==0?"active":""%>" data-bs-toggle="pill" href="#pane-<%=statKeys[i]%>-<%=rnd%>" role="tab">
                            <i class="<%=statIcons[i]%> me-2"></i><%=statLabels[i]%>
                        </a>
                    </li>
                    <% } %>
                </ul>
            </div>

            <div class="tab-content">
                <% for(int i=0; i<statKeys.length; i++) { String key = statKeys[i]; %>
                <div class="tab-pane fade <%=i==0?"show active":""%>" id="pane-<%=key%>-<%=rnd%>" role="tabpanel">
                    <div class="card border-0 shadow-sm rounded-4 overflow-hidden">
                        <div class="card-header bg-white border-bottom py-3 px-4 d-flex justify-content-between align-items-center">
                            <h6 class="mb-0 fw-bold text-dark"><i class="fas fa-list-alt text-primary me-2"></i><%=Common.getBahasaConfig("Tabel Rincian")%> <%=statLabels[i]%></h6>
                            <button class="btn btn-sm btn-success bg-gradient rounded-pill fw-bold px-4 shadow-sm hover-shadow-sm border-0" onclick="exportKeExcel<%=rnd%>('<%=key%>', '<%=statLabels[i]%>')">
                                <i class="fas fa-file-excel me-2"></i><%=Common.getBahasaConfig("Unduh Excel")%>
                            </button>
                        </div>
                        <div class="table-responsive">
                            <table class="table table-hover table-rekap-<%=rnd%> mb-0 align-middle">
                                <thead>
                                    <tr>
                                        <th class="text-center" width="60"><%=Common.getBahasaConfig("No")%></th>
                                        <th><%=Common.getBahasaConfig("Nama Komponen & Informasi")%></th>
                                        <% if(isSivitas) { %>
                                            <th class="text-center" width="15%"><%=Common.getBahasaConfig("Status Anda")%></th>
                                        <% } %>
                                        <th class="text-center" width="15%"><i class="fas fa-check-circle text-success me-1"></i> <%=Common.getBahasaConfig("Telah Akses")%></th>
                                        <th class="text-center" width="15%"><i class="fas fa-times-circle text-danger me-1"></i> <%=Common.getBahasaConfig("Belum Akses")%></th>
                                        <th class="text-center" width="12%"><i class="fas fa-users text-secondary me-1"></i> <%=Common.getBahasaConfig("Target")%></th>
                                    </tr>
                                </thead>
                                <tbody id="tbody-<%=key%>-<%=rnd%>">
                                    <tr><td colspan="<%=totalKolomTabel%>" class="text-center py-5 text-muted"><i class="fas fa-circle-notch fa-spin me-2"></i><%=Common.getBahasaConfig("Sedang mengambil data...")%></td></tr>
                                </tbody>
                            </table>
                        </div>
                        <div class="card-footer bg-white border-top-0 py-3" id="paging-<%=key%>-<%=rnd%>"></div>
                    </div>
                </div>
                <% } %>
            </div>
        </div>
    </div>
</div>

<script>
    // ==============================================================================
    // DATA PERSIAPAN PENYARING KASKADE (CASCADE FILTER JAVASCRIPT)
    // ==============================================================================
    var dataJurusan<%=rnd%> = [
        <% for (Jurusan j : listJurusan) { Long fId = (j.getFakultas() != null) ? j.getFakultas().getId() : 0L; %>
        { id: "<%=j.getId()%>", nama: "<%=j.getNama()%>", idFak: "<%=fId%>" },
        <% } %>
    ];
    
    var dataSekolah<%=rnd%> = [
        <% for (Sekolah s : listSekolah) { Long yId = (s.getYayasan() != null) ? s.getYayasan().getId() : 0L; %>
        { id: "<%=s.getId()%>", nama: "<%=s.getNama()%>", idYay: "<%=yId%>" },
        <% } %>
    ];

    function ubahFakultas<%=rnd%>() {
        var elFak = document.getElementById("filter-fakultas-<%=rnd%>");
        var elJur = document.getElementById("filter-jurusan-<%=rnd%>");
        if (elFak && elJur) {
            var fId = elFak.value;
            elJur.innerHTML = '<option value=""><%=Common.getBahasaConfig("Semua Jurusan")%></option>';
            dataJurusan<%=rnd%>.forEach(function(j) {
                if (fId === "" || j.idFak === fId) {
                    var opt = document.createElement("option"); opt.value = j.id; opt.text = j.nama; elJur.add(opt);
                }
            });
            muatDataDasbor<%=rnd%>(false);
        }
    }

    function ubahYayasan<%=rnd%>() {
        var elYay = document.getElementById("filter-yayasan-<%=rnd%>");
        var elSek = document.getElementById("filter-sekolah-<%=rnd%>");
        if (elYay && elSek) {
            var yId = elYay.value;
            elSek.innerHTML = '<option value=""><%=Common.getBahasaConfig("Semua Sekolah")%></option>';
            dataSekolah<%=rnd%>.forEach(function(s) {
                if (yId === "" || s.idYay === yId) {
                    var opt = document.createElement("option"); opt.value = s.id; opt.text = s.nama; elSek.add(opt);
                }
            });
            muatDataDasbor<%=rnd%>(false);
        }
    }

    function bersihkanFilter<%=rnd%>() {
        var fTa = document.getElementById('filter-ta-<%=rnd%>'); if(fTa) fTa.value = "";
        var fSmt = document.getElementById('filter-smt-<%=rnd%>'); if(fSmt) fSmt.value = "";
        
        var fFak = document.getElementById('filter-fakultas-<%=rnd%>'); 
        if (fFak) { fFak.value = ""; ubahFakultas<%=rnd%>(); }
        
        var fYay = document.getElementById('filter-yayasan-<%=rnd%>'); 
        if (fYay) { fYay.value = ""; ubahYayasan<%=rnd%>(); }
        
        muatDataDasbor<%=rnd%>(false);
    }

    // ==============================================================================
    // FUNGSI UTAMA PENGAMBILAN DATA (FETCH)
    // ==============================================================================
    var rekapData<%=rnd%> = {};
    var isSivitas<%=rnd%> = <%=isSivitas%>;
    var defKategori<%=rnd%> = '<%=defKategori%>';
    var totalKolomTabel<%=rnd%> = <%=totalKolomTabel%>;

    function muatDataDasbor<%=rnd%>(isRefresh) {
        var iconRefresh = document.getElementById('icon-refresh-<%=rnd%>');
        if (iconRefresh) iconRefresh.classList.add('fa-spin');
        
        var keys = ["pertemuan", "materi", "tugas", "ujian", "video", "audio"];
        
        // Atur status proses pemuatan pada antarmuka
        keys.forEach(function(k) {
            var elBody = document.getElementById('tbody-' + k + '-<%=rnd%>');
            if (elBody) {
                elBody.innerHTML = '<tr><td colspan="' + totalKolomTabel<%=rnd%> + '" class="text-center py-5 text-muted"><i class="fas fa-circle-notch fa-spin fa-2x mb-3 text-primary"></i><br><b><%=Common.getBahasaConfig("Sedang mengambil data dari peladen...")%></b></td></tr>';
            }
            var elPage = document.getElementById('paging-' + k + '-<%=rnd%>');
            if (elPage) elPage.innerHTML = '';
            
            var elCount = document.getElementById('count-' + k + '-<%=rnd%>');
            if (elCount) elCount.innerHTML = '<i class="fas fa-circle-notch fa-spin text-muted"></i>';
            
            ['sum-done', 'sum-notdone', 'sum-total'].forEach(function(prefix) {
                var elStat = document.getElementById(prefix + '-' + k + '-<%=rnd%>');
                if (elStat) elStat.innerHTML = '<i class="fas fa-circle-notch fa-spin fs-6 text-muted"></i>';
            });
        });

        var payload = new URLSearchParams();
        payload.append('kategori', defKategori<%=rnd%>);
        
        var elTa = document.getElementById('filter-ta-<%=rnd%>');
        if(elTa) payload.append('q_ta', elTa.value);
        
        var elSmt = document.getElementById('filter-smt-<%=rnd%>');
        if(elSmt) payload.append('q_smt', elSmt.value);
        
        var elFak = document.getElementById('filter-fakultas-<%=rnd%>');
        if(elFak) payload.append('q_fakultas', elFak.value);
        
        var elJur = document.getElementById('filter-jurusan-<%=rnd%>');
        if(elJur) payload.append('q_jurusan', elJur.value);
        
        var elYay = document.getElementById('filter-yayasan-<%=rnd%>');
        if(elYay) payload.append('q_yayasan', elYay.value);
        
        var elSek = document.getElementById('filter-sekolah-<%=rnd%>');
        if(elSek) payload.append('q_sekolah', elSek.value);
        
        if (isRefresh) payload.append('refresh', 'true');

        fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_dasbor_service', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: payload.toString()
        })
        .then(function(response) { 
            if(!response.ok) throw new Error("<%=Common.getBahasaConfig("Gagal terhubung ke peladen.")%>");
            return response.json(); 
        })
        .then(function(res) {
            if(res.status === 'ok') {
                rekapData<%=rnd%> = res.rekap;
                keys.forEach(function(k) { 
                    var dataList = rekapData<%=rnd%>[k] || [];
                    var totalItems = dataList.length;
                    var sumDone = 0, sumNotDone = 0, sumTarget = 0;
                    
                    dataList.forEach(function(d) { 
                        sumDone += (d.done || 0); 
                        sumNotDone += (d.notDone || 0); 
                        sumTarget += (d.total || 0); 
                    });

                    var elCount = document.getElementById('count-' + k + '-<%=rnd%>');
                    if(elCount) elCount.innerHTML = totalItems + ' <%=Common.getBahasaConfig("Item")%>';
                    
                    var elDone = document.getElementById('sum-done-' + k + '-<%=rnd%>');
                    if(elDone) elDone.innerHTML = sumDone;
                    
                    var elNotDone = document.getElementById('sum-notdone-' + k + '-<%=rnd%>');
                    if(elNotDone) elNotDone.innerHTML = sumNotDone;
                    
                    var elTotal = document.getElementById('sum-total-' + k + '-<%=rnd%>');
                    if(elTotal) elTotal.innerHTML = sumTarget;

                    // Progress bar per kartu
                    var pct = sumTarget > 0 ? Math.round(sumDone / sumTarget * 100) : 0;
                    var pc  = pct >= 75 ? '#198754' : (pct >= 40 ? '#fd7e14' : '#dc3545');
                    var ep  = document.getElementById('prog-' + k + '-<%=rnd%>');
                    var epc = document.getElementById('pct-' + k + '-<%=rnd%>');
                    if (ep)  { setTimeout(function(b,c){ return function(){ b.style.width=c+'%'; b.style.background=pc; }; }(ep,pct), 80); }
                    if (epc) { epc.textContent = pct + '%'; epc.style.color = pc; }

                    renderTabelDasbor<%=rnd%>(k, 1);
                });
                // Overall health score dari semua kategori
                var allDone = 0, allTarget = 0;
                keys.forEach(function(k) {
                    var dl = rekapData<%=rnd%>[k] || [];
                    dl.forEach(function(d) { allDone += (d.done||0); allTarget += (d.total||0); });
                });
                var hPct   = allTarget > 0 ? Math.round(allDone / allTarget * 100) : 0;
                var hColor = hPct >= 75 ? '#198754' : (hPct >= 40 ? '#fd7e14' : '#dc3545');
                var hCard  = document.getElementById('dasbor-health-<%=rnd%>');
                var hBar   = document.getElementById('dasbor-health-bar-<%=rnd%>');
                var hPctEl = document.getElementById('dasbor-health-pct-<%=rnd%>');
                var hSub   = document.getElementById('dasbor-health-sub-<%=rnd%>');
                if (hCard)  hCard.style.display  = '';
                if (hBar)   { setTimeout(function(){ hBar.style.width=hPct+'%'; hBar.style.background=hColor; }, 120); }
                if (hPctEl) { hPctEl.textContent = hPct + '%'; hPctEl.style.color = hColor; }
                if (hSub)   hSub.textContent = allDone + ' dari ' + allTarget + ' interaksi mahasiswa selesai';

            } else {
                if (typeof tampilkanToast === 'function') tampilkanToast(res.msg, 'bg-danger text-white');
            }
        })
        .catch(function(err) { 
            console.error(err); 
            if (typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi masalah jaringan saat mengambil data.")%>', 'bg-danger text-white');
        })
        .finally(function() { 
            if (iconRefresh) iconRefresh.classList.remove('fa-spin'); 
        });
    }

    // ==============================================================================
    // PERENDERAN TABEL DAN PAGINASI DINAMIS
    // ==============================================================================
    function renderTabelDasbor<%=rnd%>(key, page) {
        var dataArray = rekapData<%=rnd%>[key] || [];
        var limit = 5;
        var start = (page - 1) * limit;
        var end = start + limit;
        var pagedData = dataArray.slice(start, end);

        var html = '';
        if (pagedData.length === 0) {
            html = '<tr><td colspan="' + totalKolomTabel<%=rnd%> + '" class="text-center py-5 text-muted"><i class="fas fa-folder-open fa-3x mb-3 opacity-25"></i><br><b><%=Common.getBahasaConfig("Tidak ada data yang ditemukan.")%></b></td></tr>';
        } else {
            pagedData.forEach(function(item, i) {
                var no = start + i + 1;
                var infoTeks = item.info ? '<div class="text-muted mt-1" style="font-size:11px;"><i class="fas fa-info-circle me-1 text-primary"></i>' + item.info + '</div>' : '';
                
                var myStatusTeks = '';
                if (isSivitas<%=rnd%>) {
                    var itemStatus = item.myStatus ? item.myStatus : '-';
                    var isDone = (key === 'tugas' || key === 'ujian') ? (itemStatus === 'Sudah Mengerjakan') : (itemStatus === 'Sudah Akses');
                    
                    var warnaBadge = isDone ? 'bg-success' : 'bg-danger';
                    if (!(key === 'tugas' || key === 'ujian')) { 
                        warnaBadge = isDone ? 'bg-primary' : 'bg-secondary'; 
                    }
                    var textColor = warnaBadge.replace('bg-', 'text-');
                    var borderColor = warnaBadge.replace('bg-', 'border-');
                    var iconStatus = isDone ? (key === 'tugas' || key === 'ujian' ? 'fas fa-check' : 'fas fa-eye') : (key === 'tugas' || key === 'ujian' ? 'fas fa-times' : 'fas fa-eye-slash');

                    myStatusTeks = '<td class="text-center"><span class="badge ' + warnaBadge + ' bg-opacity-10 ' + textColor + ' border ' + borderColor + ' rounded-pill py-1 px-3 fw-bold shadow-sm"><i class="' + iconStatus + ' me-1"></i>' + itemStatus + '</span></td>';
                }

                var btnDownload = item.url ? ' <a href="' + item.url + '" target="_blank" class="badge bg-primary text-white text-decoration-none ms-2 shadow-sm hover-shadow-sm px-2 py-1"><i class="fas fa-download me-1"></i><%=Common.getBahasaConfig("Buka")%></a>' : '';
                var itemNama = item.nama ? item.nama : '-';

                html += '<tr>' +
                    '<td class="text-center fw-bold text-muted">' + no + '</td>' +
                    '<td><div class="fw-bold text-dark d-flex align-items-center flex-wrap">' + itemNama + btnDownload + '</div>' + infoTeks + '</td>' +
                    myStatusTeks +
                    '<td class="text-center"><span class="text-success fw-bolder fs-5">' + item.done + '</span> <span class="text-muted d-block fw-semibold" style="font-size:10px;"><%=Common.getBahasaConfig("Peserta")%></span></td>' +
                    '<td class="text-center"><span class="text-danger fw-bolder fs-5">' + item.notDone + '</span> <span class="text-muted d-block fw-semibold" style="font-size:10px;"><%=Common.getBahasaConfig("Peserta")%></span></td>' +
                    '<td class="text-center"><span class="text-secondary fw-bolder fs-5">' + item.total + '</span> <span class="text-muted d-block fw-semibold" style="font-size:10px;"><%=Common.getBahasaConfig("Target")%></span></td>' +
                '</tr>';
            });
        }
        
        var elBody = document.getElementById('tbody-' + key + '-<%=rnd%>');
        if (elBody) elBody.innerHTML = html;
        
        renderPaginasiDasbor<%=rnd%>(key, page, Math.ceil(dataArray.length / limit));
        
        // Pemicu ulang untuk memuat ikon Font Awesome versi JS
        if (typeof FontAwesome !== 'undefined' && FontAwesome.dom) { 
            setTimeout(function(){ FontAwesome.dom.i2svg(); }, 50);
        }
    }

    function renderPaginasiDasbor<%=rnd%>(key, current, totalPage) {
        var container = document.getElementById('paging-' + key + '-<%=rnd%>');
        if (!container) return;
        if (totalPage <= 1) { container.innerHTML = ''; return; }

        var h = '<ul class="custom-pagination-<%=rnd%>">';
        // Tombol Kembali
        h += '<li class="page-item ' + (current === 1 ? 'disabled' : '') + '"><a class="page-link" href="javascript:void(0)" onclick="if(' + current + '>1) renderTabelDasbor<%=rnd%>(\'' + key + '\', ' + (current - 1) + ')"><i class="fas fa-chevron-left"></i></a></li>';
        
        // Nomor Halaman
        for (var i = 1; i <= totalPage; i++) {
            var activeClass = (i === current) ? 'active' : '';
            h += '<li class="page-item ' + activeClass + '"><a class="page-link" href="javascript:void(0)" onclick="renderTabelDasbor<%=rnd%>(\'' + key + '\', ' + i + ')">' + i + '</a></li>';
        }
        
        // Tombol Lanjut
        h += '<li class="page-item ' + (current === totalPage ? 'disabled' : '') + '"><a class="page-link" href="javascript:void(0)" onclick="if(' + current + '<' + totalPage + ') renderTabelDasbor<%=rnd%>(\'' + key + '\', ' + (current + 1) + ')"><i class="fas fa-chevron-right"></i></a></li>';
        h += '</ul>';
        
        container.innerHTML = h;
    }

    // ==============================================================================
    // FUNGSI EKSPOR DATA KE FORMAT EXCEL
    // ==============================================================================
    function exportKeExcel<%=rnd%>(key, labelKey) {
        var dataArray = rekapData<%=rnd%>[key] || [];
        if (dataArray.length === 0) {
            if (typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Tidak ada data untuk diekspor.")%>', 'bg-warning text-dark');
            return;
        }
        
        var thNo = '<%=Common.getBahasaConfigJS("No")%>';
        var thNama = '<%=Common.getBahasaConfigJS("Nama Komponen")%>';
        var thInfo = '<%=Common.getBahasaConfigJS("Informasi / Waktu")%>';
        var thStatus = '<%=Common.getBahasaConfigJS("Status (Pribadi)")%>';
        var thTelah = '<%=Common.getBahasaConfigJS("Telah Akses/Kerja (Peserta)")%>';
        var thBelum = '<%=Common.getBahasaConfigJS("Belum Akses/Kerja (Peserta)")%>';
        var thTotal = '<%=Common.getBahasaConfigJS("Total Peserta Target")%>';
        var thUrl = '<%=Common.getBahasaConfigJS("URL Berkas")%>';

        var tabelHtml = '<table border="1"><thead><tr><th>' + thNo + '</th><th>' + thNama + '</th><th>' + thInfo + '</th><th>' + thUrl + '</th>';
        
        if (isSivitas<%=rnd%>) {
            tabelHtml += '<th>' + thStatus + '</th>';
        }
        
        tabelHtml += '<th>' + thTelah + '</th><th>' + thBelum + '</th><th>' + thTotal + '</th></tr></thead><tbody>';

        dataArray.forEach(function(item, idx) {
            var itemNama = item.nama ? item.nama : '-';
            var itemInfo = item.info ? item.info : '-';
            var itemUrl = item.url ? item.url : '-';

            tabelHtml += '<tr><td>' + (idx + 1) + '</td><td>' + itemNama + '</td><td>' + itemInfo + '</td><td>' + itemUrl + '</td>';
            
            if (isSivitas<%=rnd%>) {
                var itemStatus = item.myStatus ? item.myStatus : '-';
                tabelHtml += '<td>' + itemStatus + '</td>';
            }

            tabelHtml += '<td>' + item.done + '</td><td>' + item.notDone + '</td><td>' + item.total + '</td></tr>';
        });
        
        tabelHtml += '</tbody></table>';
        
        var blob = new Blob([tabelHtml], { type: 'application/vnd.ms-excel' });
        var a = document.createElement('a'); 
        a.href = URL.createObjectURL(blob); 
        a.download = 'Rekapitulasi_' + labelKey.replace(/\s+/g, '_') + '.xls'; 
        a.click();
        URL.revokeObjectURL(a.href);
    }

    // Pemuatan data otomatis saat antarmuka pertama kali dimuat
    muatDataDasbor<%=rnd%>(false);
</script>