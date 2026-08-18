<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.Perkuliahan"%>
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

<%
    String rnd = Common.getGeneratedBarCode(7);
    Tbmuser tbmuser = Common.getCurrentUser(request);
    
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.setStatus(401);
        return;
    }

    String faIcon = "fas fa-calendar-alt";
    String judulRaw = "Kalender Pembelajaran";
    
    boolean[] ptYa = Common.chekPtAtauSekolah(tbmuser);
    boolean apakahPerguruanTInggi = ptYa[0];
    boolean apakahSekolah = ptYa[1];
    
    boolean isSiswa = tbmuser.getSiswa() != null;
    boolean isGuru = tbmuser.ambilGuru() != null;
    boolean isMahasiswa = tbmuser.getMahasiswa() != null;
    boolean isDosen = tbmuser.ambilDosen() != null;
    boolean isSivitas = isSiswa || isGuru || isMahasiswa || isDosen;

    Fakultas myFakultas = (!isSivitas) ? tbmuser.ambilFakultas() : null;
    Jurusan myJurusan = (!isSivitas) ? tbmuser.ambilJurusan() : null;
    Yayasan myYayasan = (!isSivitas) ? tbmuser.ambilYayasan() : null;
    Sekolah mySekolah = (!isSivitas) ? tbmuser.ambilSekolah() : null;
    
    List<Fakultas> listFakultas = new ArrayList<Fakultas>();
    List<Jurusan> listJurusan = new ArrayList<Jurusan>();
    List<Sekolah> listSekolah = new ArrayList<Sekolah>();
    List<Yayasan> listYayasan = new ArrayList<Yayasan>();
    
    Session sessFilter = HibernateUtil.openSession();
    try {
        if (apakahPerguruanTInggi && !isSivitas) {
            listFakultas = ConstantValues.simpleList(sessFilter.createCriteria(Fakultas.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Fakultas.class);
            listJurusan = ConstantValues.simpleList(sessFilter.createCriteria(Jurusan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Jurusan.class);
        }
        if (apakahSekolah && !isSivitas) {
            listSekolah = ConstantValues.simpleList(sessFilter.createCriteria(Sekolah.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Sekolah.class);
            listYayasan = ConstantValues.simpleList(sessFilter.createCriteria(Yayasan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Yayasan.class);
        }
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/kalender.jsp:59");
    } finally {
        try { sessFilter.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/kalender.jsp:61");}
        ais.common.ElearningSessionUtil.closeQuietly(sessFilter);
        HibernateUtil.closeSessionQuietly(sessFilter);
    }
%>

<link href='https://cdn.jsdelivr.net/npm/fullcalendar@5.11.3/main.min.css' rel='stylesheet' />
<script data-cfasync="false" src='https://cdn.jsdelivr.net/npm/fullcalendar@5.11.3/main.min.js'></script>
<script data-cfasync="false" src='https://cdn.jsdelivr.net/npm/fullcalendar@5.11.3/locales/id.js'></script>

<style>
    .fade-in-<%=rnd%> { animation: fadeInAnim<%=rnd%> 0.5s ease-in-out; }
    @keyframes fadeInAnim<%=rnd%> { from { opacity: 0; transform: translateY(15px); } to { opacity: 1; transform: translateY(0); } }
    .input-elegan-<%=rnd%> { font-size: 0.85rem; border-radius: 0.5rem; border: 1px solid #ced4da; box-shadow: inset 0 1px 2px rgba(0,0,0,.05); transition: border-color .15s ease-in-out,box-shadow .15s ease-in-out; }
    .input-elegan-<%=rnd%>:focus { border-color: #80bdff; box-shadow: 0 0 0 0.2rem rgba(0,123,255,.25); }
    .input-elegan-<%=rnd%>[disabled] { background-color: #e9ecef; opacity: 1; font-weight: 600; }
    .fc-event { cursor: pointer; border-radius: 4px; border: none; padding: 2px 4px; box-shadow: 0 1px 2px rgba(0,0,0,0.1); }
    .fc-event-title { font-weight: bold; font-size: 0.8rem; }
    .fc-toolbar-title { font-weight: 700; color: #333; text-transform: uppercase; letter-spacing: 1px; }
    .fc-button-primary { background-color: #17a2b8 !important; border-color: #17a2b8 !important; font-weight: bold; text-transform: uppercase; letter-spacing: 0.5px; }
    .fc-button-primary:not(:disabled):active, .fc-button-primary:not(:disabled).fc-button-active { background-color: #138496 !important; border-color: #117a8b !important; }
    
    /* Lebar Modal Dinamis 90% */
    .modal-dialog-90-sub { width: 100%; max-width: 100%; margin: 0; }
    .modal-dialog-90-sub .modal-content { min-height: 100vh; border-radius: 0; }
    @media (min-width: 768px) {
        .modal-dialog-90-sub { width: 90%; max-width: 90%; margin: 1.75rem auto; }
        .modal-dialog-90-sub .modal-content { min-height: auto; border-radius: 0.3rem; }
    }
    .close-header-besar { font-size: 2.8rem !important; opacity: 1 !important; text-shadow: 0 2px 4px rgba(0,0,0,0.6) !important; padding: 0.5rem 1rem !important; margin: -1rem -1rem -1rem auto !important; line-height: 1 !important; }
    .close-header-besar:hover { color: #ffcccc !important; }
    
    /* Custom Checkbox Kategori */
    .chk-kategori-label { font-weight: 600; font-size: 0.85rem; color: #495057; cursor: pointer; display: flex; align-items: center; }
    .chk-kategori-label input { width: 16px; height: 16px; margin-right: 8px; cursor: pointer; }
    /* Mobile: compact filter row */
    @media (max-width: 767.98px) {
        #wrapper-kalender-<%=rnd%> .row.align-items-end .col-xl-3,
        #wrapper-kalender-<%=rnd%> .row.align-items-end .col-xl-2,
        #wrapper-kalender-<%=rnd%> .row.align-items-end .col-lg-3,
        #wrapper-kalender-<%=rnd%> .row.align-items-end .col-lg-2 { width: 50% !important; flex: 0 0 50%; max-width: 50%; }
        #wrapper-kalender-<%=rnd%> .row.align-items-end .col-xl-auto { width: 100% !important; flex: 0 0 100%; max-width: 100%; }
        .chk-kategori-label { font-size: 0.78rem !important; }
        /* Kategori checkboxes: 2 per row on mobile */
        #wrapper-kalender-<%=rnd%> .d-flex.flex-wrap .chk-kategori-label { width: 47%; flex: 0 0 47%; }
    }
</style>

<div id="wrapper-kalender-<%=rnd%>" class="container-fluid py-4 px-xl-5 fade-in-<%=rnd%>">
    
    <div class="row align-items-center mb-3 pb-2 border-bottom">
        <div class="col-12 mb-2 mb-lg-0">
            <div class="d-flex align-items-center">
                <div class="bg-primary text-white rounded shadow-sm mr-3 me-3 d-flex align-items-center justify-content-center" style="width: 48px; height: 48px; background-image: linear-gradient(180deg, rgba(255,255,255,0.15), rgba(255,255,255,0));">
                    <i class="<%= faIcon %> fa-fw fa-lg"></i>
                </div>
                <div>
                    <h6 class="text-muted small font-weight-bold mb-0" style="text-transform: uppercase; letter-spacing: 0.5px;"><%= Common.getBahasaConfig("Modul Kalender") %></h6>
                    <h5 class="fw-bold font-weight-bold text-dark mb-0"><%= Common.getBahasaConfig(judulRaw) %></h5>
                </div>
            </div>
        </div>
    </div>

    <div class="card border-0 shadow-sm rounded-lg mb-4">
        <div class="card-body p-3">
            <form id="form-search-<%=rnd%>" onsubmit="event.preventDefault(); window.refreshKalender<%=rnd%>();" class="m-0">
                <div class="row align-items-end">
                    <div class="col-xl-3 col-lg-3 col-md-6 mb-3 px-2">
                        <label class="font-weight-bold text-muted mb-1" style="font-size:0.8rem;"><i class="far fa-calendar-alt mr-1"></i> <%=Common.getBahasaConfig("Tahun Akademik")%></label>
                        <select class="form-control form-control-sm input-elegan-<%=rnd%>" id="filter-ta-<%=rnd%>" onchange="window.refreshKalender<%=rnd%>()">
                            <option value=""><%=Common.getBahasaConfig("Semua TA")%></option>
                            <% if (Common.tahunAngkatans != null) { for (String strTa : Common.tahunAngkatans) { %><option value="<%=strTa%>"><%=strTa%></option><% } } %>
                        </select>
                    </div>
                    <div class="col-xl-2 col-lg-2 col-md-6 mb-3 px-2">
                        <label class="font-weight-bold text-muted mb-1" style="font-size:0.8rem;"><i class="fas fa-hourglass-half mr-1"></i> <%=Common.getBahasaConfig("Semester")%></label>
                        <select class="form-control form-control-sm input-elegan-<%=rnd%>" id="filter-smt-<%=rnd%>" onchange="window.refreshKalender<%=rnd%>()">
                            <option value=""><%=Common.getBahasaConfig("Semua")%></option>
                            <option value="<%=Perkuliahan.GANJIL%>"><%=Common.getBahasaConfig("Ganjil")%></option>
                            <option value="<%=Perkuliahan.GENAP%>"><%=Common.getBahasaConfig("Genap")%></option>
                            <option value="<%=Perkuliahan.SP%>"><%=Common.getBahasaConfig("Pendek")%></option>
                        </select>
                    </div>

                    <% if (apakahPerguruanTInggi && !isSivitas) { %>
                    <div class="col-xl-2 col-lg-2 col-md-6 col-6 mb-3 px-2">
                        <label class="font-weight-bold text-muted mb-1" style="font-size:0.8rem;"><%=Common.getBahasaConfig("Fakultas")%></label>
                        <select class="form-control form-control-sm input-elegan-<%=rnd%>" id="filter-fakultas-<%=rnd%>" onchange="ubahFakultas<%=rnd%>()" <%= (myFakultas != null || myJurusan != null) ? "disabled" : "" %>>
                            <% if (myFakultas != null) { %><option value="<%=myFakultas.getId()%>" selected><%=myFakultas.getNama()%></option><% } else { %>
                            <option value=""><%=Common.getBahasaConfig("Semua")%></option>
                            <% for(Fakultas f : listFakultas) { %><option value="<%=f.getId()%>"><%=f.getNama()%></option><% } } %>
                        </select>
                    </div>
                    <div class="col-xl-3 col-lg-3 col-md-6 col-6 mb-3 px-2">
                        <label class="font-weight-bold text-muted mb-1" style="font-size:0.8rem;"><%=Common.getBahasaConfig("Jurusan")%></label>
                        <select class="form-control form-control-sm input-elegan-<%=rnd%>" id="filter-jurusan-<%=rnd%>" onchange="window.refreshKalender<%=rnd%>()" <%= (myJurusan != null) ? "disabled" : "" %>>
                            <% if (myJurusan != null) { %><option value="<%=myJurusan.getId()%>" selected><%=myJurusan.getNama()%></option><% } else { %>
                            <option value=""><%=Common.getBahasaConfig("Semua")%></option>
                            <% for(Jurusan j : listJurusan) { %><option value="<%=j.getId()%>"><%=j.getNama()%></option><% } } %>
                        </select>
                    </div>
                    <% } %>

                    <% if (apakahSekolah && !isSivitas) { %>
                    <div class="col-xl-2 col-lg-2 col-md-6 col-6 mb-3 px-2">
                        <label class="font-weight-bold text-muted mb-1" style="font-size:0.8rem;"><%=Common.getBahasaConfig("Yayasan")%></label>
                        <select class="form-control form-control-sm input-elegan-<%=rnd%>" id="filter-yayasan-<%=rnd%>" onchange="ubahYayasan<%=rnd%>()" <%= (myYayasan != null || mySekolah != null) ? "disabled" : "" %>>
                            <% if (myYayasan != null) { %><option value="<%=myYayasan.getId()%>" selected><%=myYayasan.getNama()%></option><% } else { %>
                            <option value=""><%=Common.getBahasaConfig("Semua")%></option>
                            <% for(Yayasan y : listYayasan) { %><option value="<%=y.getId()%>"><%=y.getNama()%></option><% } } %>
                        </select>
                    </div>
                    <div class="col-xl-3 col-lg-3 col-md-6 col-6 mb-3 px-2">
                        <label class="font-weight-bold text-muted mb-1" style="font-size:0.8rem;"><%=Common.getBahasaConfig("Sekolah")%></label>
                        <select class="form-control form-control-sm input-elegan-<%=rnd%>" id="filter-sekolah-<%=rnd%>" onchange="window.refreshKalender<%=rnd%>()" <%= (mySekolah != null) ? "disabled" : "" %>>
                            <% if (mySekolah != null) { %><option value="<%=mySekolah.getId()%>" selected><%=mySekolah.getNama()%></option><% } else { %>
                            <option value=""><%=Common.getBahasaConfig("Semua")%></option>
                            <% for(Sekolah s : listSekolah) { %><option value="<%=s.getId()%>"><%=s.getNama()%></option><% } } %>
                        </select>
                    </div>
                    <% } %>

                    <div class="col-xl-auto col-lg-2 col-md-12 mb-3 px-2 d-flex justify-content-end ml-auto">
                        <button type="button" class="btn btn-outline-secondary btn-sm font-weight-bold px-3 mr-2" onclick="bersihkanFilter<%=rnd%>()"><i class="fas fa-undo"></i></button>
                        <button type="submit" class="btn btn-info btn-sm font-weight-bold px-4 shadow-sm"><i class="fas fa-sync-alt mr-2"></i> <%=Common.getBahasaConfig("Terapkan")%></button>
                    </div>
                </div>
            </form>
            
            <div class="d-flex flex-wrap mt-2 pt-3 border-top">
                <label class="chk-kategori-label mr-4 mb-2">
                    <input type="checkbox" id="chk-pertemuan-<%=rnd%>" checked onchange="window.refreshKalender<%=rnd%>()">
                    <span class="badge" style="background-color: #0275d8; color:#fff; padding: 0.35rem 0.5rem; margin-right: 5px;">&nbsp;&nbsp;</span> <%=Common.getBahasaConfig("Pertemuan / Kelas")%>
                </label>
                
                <label class="chk-kategori-label mr-4 mb-2">
                    <input type="checkbox" id="chk-ujian-<%=rnd%>" checked onchange="window.refreshKalender<%=rnd%>()">
                    <span class="badge" style="background-color: #d9534f; color:#fff; padding: 0.35rem 0.5rem; margin-right: 5px;">&nbsp;&nbsp;</span> <%=Common.getBahasaConfig("Jadwal Ujian")%>
                </label>

                <label class="chk-kategori-label mr-4 mb-2">
                    <input type="checkbox" id="chk-tugas-<%=rnd%>" checked onchange="window.refreshKalender<%=rnd%>()">
                    <span class="badge" style="background-color: #218838; color:#fff; padding: 0.35rem 0.5rem; margin-right: 5px;">&nbsp;&nbsp;</span> <%=Common.getBahasaConfig("Tugas Individu")%>
                </label>

                <label class="chk-kategori-label mr-4 mb-2">
                    <input type="checkbox" id="chk-tugas-kelompok-<%=rnd%>" checked onchange="window.refreshKalender<%=rnd%>()">
                    <span class="badge" style="background-color: #6610f2; color:#fff; padding: 0.35rem 0.5rem; margin-right: 5px;">&nbsp;&nbsp;</span> <%=Common.getBahasaConfig("Tugas Kelompok")%>
                </label>
            </div>
            
        </div>
    </div>

    <div class="card border-0 shadow-sm rounded-lg mb-5 position-relative">
        <div id="overlay-loading-<%=rnd%>" class="position-absolute w-100 h-100 d-flex flex-column justify-content-center align-items-center" style="top: 0; left: 0; background: rgba(255, 255, 255, 0.85); z-index: 50; display: none !important; border-radius: inherit;">
            <i class="fas fa-circle-notch fa-spin fa-4x text-info mb-3"></i>
            <h6 class="font-weight-bold text-muted"><%=Common.getBahasaConfig("Memuat jadwal kalender...")%></h6>
        </div>

        <div class="card-body p-4 p-md-5">
            <div id="calendar-<%=rnd%>"></div>
        </div>
    </div>
</div>

<script>
(function() {
    var rootUrl<%=rnd%> = "<%=Common.ROOT%>";
    var calendarEl = document.getElementById('calendar-<%=rnd%>');
    var kalenderInstance<%=rnd%>;

    window.globalZIndexCounter = window.globalZIndexCounter || 106000;

    var dataJurusan<%=rnd%> = [
        <% for (Jurusan j : listJurusan) { Long fId = (j.getFakultas() != null) ? j.getFakultas().getId() : 0L; %>
        { id: "<%=j.getId()%>", nama: "<%=j.getNama()%>", idFakultas: "<%=fId%>" },
        <% } %>
    ];
    var dataSekolah<%=rnd%> = [
        <% for (Sekolah s : listSekolah) { Long yId = (s.getYayasan() != null) ? s.getYayasan().getId() : 0L; %>
        { id: "<%=s.getId()%>", nama: "<%=s.getNama()%>", idYayasan: "<%=yId%>" },
        <% } %>
    ];

    window.ubahFakultas<%=rnd%> = function() {
        var elFak = document.getElementById("filter-fakultas-<%=rnd%>");
        var elJur = document.getElementById("filter-jurusan-<%=rnd%>");
        if(elFak && elJur && !elJur.disabled) {
            var fId = elFak.value;
            elJur.innerHTML = '<option value=""><%=Common.getBahasaConfig("Semua")%></option>';
            dataJurusan<%=rnd%>.forEach(function(j) {
                if (fId === "" || j.idFakultas === fId) {
                    var opt = document.createElement("option"); opt.value = j.id; opt.text = j.nama; elJur.add(opt);
                }
            });
            window.refreshKalender<%=rnd%>();
        }
    };

    window.ubahYayasan<%=rnd%> = function() {
        var elYay = document.getElementById("filter-yayasan-<%=rnd%>");
        var elSek = document.getElementById("filter-sekolah-<%=rnd%>");
        if(elYay && elSek && !elSek.disabled) {
            var yId = elYay.value;
            elSek.innerHTML = '<option value=""><%=Common.getBahasaConfig("Semua")%></option>';
            dataSekolah<%=rnd%>.forEach(function(s) {
                if (yId === "" || s.idYayasan === yId) {
                    var opt = document.createElement("option"); opt.value = s.id; opt.text = s.nama; elSek.add(opt);
                }
            });
            window.refreshKalender<%=rnd%>();
        }
    };
    
    window.bersihkanFilter<%=rnd%> = function() {
        document.getElementById('filter-ta-<%=rnd%>').value = "";
        document.getElementById('filter-smt-<%=rnd%>').value = "";
        var fFak = document.getElementById('filter-fakultas-<%=rnd%>');
        if(fFak && !fFak.disabled) { fFak.value = ""; ubahFakultas<%=rnd%>(); }
        var fYay = document.getElementById('filter-yayasan-<%=rnd%>');
        if(fYay && !fYay.disabled) { fYay.value = ""; ubahYayasan<%=rnd%>(); }
        
        // Reset checkoxes ke true
        document.getElementById('chk-pertemuan-<%=rnd%>').checked = true;
        document.getElementById('chk-ujian-<%=rnd%>').checked = true;
        document.getElementById('chk-tugas-<%=rnd%>').checked = true;
        document.getElementById('chk-tugas-kelompok-<%=rnd%>').checked = true;

        window.refreshKalender<%=rnd%>();
    };
    
    window.refreshKalender<%=rnd%> = function() {
        if(kalenderInstance<%=rnd%>) kalenderInstance<%=rnd%>.refetchEvents();
    };

    window.tutupModalDetail<%=rnd%> = function(modalId) {
        var el = document.getElementById(modalId);
        if (typeof $ !== 'undefined') $('#' + modalId).modal('hide');
        setTimeout(function() {
            if (el) el.remove();
            if (document.querySelectorAll('.modal.show').length === 0) {
                var bd = document.querySelectorAll('.modal-backdrop');
                bd.forEach(function(b) { b.remove(); });
                document.body.classList.remove('modal-open');
                document.body.style.paddingRight = '';
            } else {
                document.body.classList.add('modal-open');
            }
        }, 300);
    };

    function inisialisasiKalender() {
        var elLoading = document.getElementById('overlay-loading-<%=rnd%>');
        var isMobile = window.innerWidth < 576;

        kalenderInstance<%=rnd%> = new FullCalendar.Calendar(calendarEl, {
            locale: 'id',
            initialView: isMobile ? 'listWeek' : 'dayGridMonth',
            headerToolbar: isMobile ? {
                left: 'prev,next',
                center: 'title',
                right: 'listWeek,dayGridMonth'
            } : {
                left: 'prev,next today',
                center: 'title',
                right: 'dayGridMonth,timeGridWeek,timeGridDay,listWeek'
            },
            buttonText: {
                today: '<%=Common.getBahasaConfigJS("Hari Ini")%>',
                month: '<%=Common.getBahasaConfigJS("Bulan")%>',
                week: '<%=Common.getBahasaConfigJS("Minggu")%>',
                day: '<%=Common.getBahasaConfigJS("Hari")%>',
                list: isMobile ? '<%=Common.getBahasaConfigJS("Agenda")%>' : '<%=Common.getBahasaConfigJS("Daftar Agenda")%>'
            },
            height: 'auto',
            navLinks: !isMobile,
            editable: false,
            dayMaxEvents: isMobile ? 2 : true, 
            
            loading: function(isLoading) {
                if (elLoading) {
                    if (isLoading) elLoading.style.setProperty('display', 'flex', 'important');
                    else elLoading.style.setProperty('display', 'none', 'important');
                }
            },
            
            events: function(info, successCallback, failureCallback) {
                var q_ta = document.getElementById('filter-ta-<%=rnd%>') ? encodeURIComponent(document.getElementById('filter-ta-<%=rnd%>').value) : "";
                var q_smt = document.getElementById('filter-smt-<%=rnd%>') ? encodeURIComponent(document.getElementById('filter-smt-<%=rnd%>').value) : "";
                var q_fak = document.getElementById('filter-fakultas-<%=rnd%>') ? encodeURIComponent(document.getElementById('filter-fakultas-<%=rnd%>').value) : "";
                var q_jur = document.getElementById('filter-jurusan-<%=rnd%>') ? encodeURIComponent(document.getElementById('filter-jurusan-<%=rnd%>').value) : "";
                var q_yay = document.getElementById('filter-yayasan-<%=rnd%>') ? encodeURIComponent(document.getElementById('filter-yayasan-<%=rnd%>').value) : "";
                var q_sek = document.getElementById('filter-sekolah-<%=rnd%>') ? encodeURIComponent(document.getElementById('filter-sekolah-<%=rnd%>').value) : "";

                // PERBAIKAN: Ambil state checkbox
                var show_pertemuan = document.getElementById('chk-pertemuan-<%=rnd%>') ? document.getElementById('chk-pertemuan-<%=rnd%>').checked : true;
                var show_ujian = document.getElementById('chk-ujian-<%=rnd%>') ? document.getElementById('chk-ujian-<%=rnd%>').checked : true;
                var show_tugas = document.getElementById('chk-tugas-<%=rnd%>') ? document.getElementById('chk-tugas-<%=rnd%>').checked : true;
                var show_tugas_kelompok = document.getElementById('chk-tugas-kelompok-<%=rnd%>') ? document.getElementById('chk-tugas-kelompok-<%=rnd%>').checked : true;

                var urlApi = rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_kalender_service" +
                             "&start=" + info.startStr + "&end=" + info.endStr +
                             "&q_ta=" + q_ta + "&q_smt=" + q_smt + 
                             "&q_fakultas=" + q_fak + "&q_jurusan=" + q_jur + 
                             "&q_yayasan=" + q_yay + "&q_sekolah=" + q_sek +
                             "&show_pertemuan=" + show_pertemuan +
                             "&show_ujian=" + show_ujian +
                             "&show_tugas=" + show_tugas +
                             "&show_tugas_kelompok=" + show_tugas_kelompok;

                fetch(urlApi)
                .then(response => response.json())
                .then(data => { successCallback(data); })
                .catch(error => {
                    console.error('Kesalahan pengambilan kalender:', error);
                    failureCallback(error);
                });
            },
            
            eventClick: async function(info) {
                var ev = info.event;
                var safeHtmlId = ev.id.replace(/[^a-zA-Z0-9_]/g, "");
                var modalId = "modalEvent_" + safeHtmlId;
                
                var existingModal = document.getElementById(modalId);
                if (existingModal) existingModal.remove();

                var kategori = ev.extendedProps.kategori || 'Kegiatan';
                
                var rawId = ev.id;
                var prefix = "";
                if(rawId.indexOf('_') > -1) {
                    var firstScore = rawId.indexOf('_');
                    prefix = rawId.substring(0, firstScore);
                    rawId = rawId.substring(firstScore + 1);
                }

                var urlHtmlLoad = "";
                if (prefix === 'P') urlHtmlLoad = rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning&s=load_timeline_perkuliahan&item=" + encodeURIComponent(rawId);
                else if (prefix === 'U') urlHtmlLoad = rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning&s=load_ujian&item=" + encodeURIComponent(rawId);
                else if (prefix === 'T') urlHtmlLoad = rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning&s=load_tugas&item=" + encodeURIComponent(rawId);
                else if (prefix === 'TK') urlHtmlLoad = rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning&s=load_tugas_kelompok&item=" + encodeURIComponent(rawId);

                var startFmt = ev.start ? ev.start.toLocaleString('id-ID', { dateStyle: 'full', timeStyle: 'short' }) : "-";
                var endFmt = ev.end ? ev.end.toLocaleString('id-ID', { dateStyle: 'full', timeStyle: 'short' }) : "-";

                var modalSize = (urlHtmlLoad !== "") ? "modal-dialog-90-sub" : "";

                if (typeof window.variable === 'undefined') { window.variable = 0; }
                window.variable++;
                var zIndexModal = 1050 + (window.variable * 2);

                var modalHtml = `
                <div class="modal fade" id="`+modalId+`" tabindex="-1" role="dialog" aria-hidden="true" style="background: rgba(0,0,0,0.7);">
                  <div class="modal-dialog `+modalSize+` modal-dialog-centered" role="document">
                    <div class="modal-content border-0 shadow-lg">
                      <div class="modal-header text-white" style="background-color: `+ev.backgroundColor+`; padding: 1rem 1.5rem; align-items: center;">
                        <h5 class="modal-title font-weight-bold m-0 text-truncate" style="max-width: 85%;"><i class="`+(ev.extendedProps.icon || 'fas fa-calendar-check')+` mr-2"></i> `+kategori+`</h5>
                        <button type="button" class="close text-white close-header-besar" onclick="tutupModalDetail<%=rnd%>('`+modalId+`')" aria-label="Close">
                            <span aria-hidden="true">&times;</span>
                        </button>
                      </div>
                      <div class="modal-body p-3 p-md-4" id="body-`+modalId+`" style="overflow-y: auto; max-height: calc(100vh - 140px);">
                        <div class="text-center py-5">
                            <i class="fas fa-circle-notch fa-spin fa-3x text-info mb-3"></i>
                            <p class="font-weight-bold text-muted mt-2"><%=Common.getBahasaConfig("Memuat detail rincian kegiatan...")%></p>
                        </div>
                      </div>
                      <div class="modal-footer bg-light border-top shadow-sm" style="justify-content: flex-end; padding: 1rem;">
                        <button type="button" class="btn btn-danger btn-lg px-5 font-weight-bold shadow-sm" onclick="tutupModalDetail<%=rnd%>('`+modalId+`')" style="border-radius: 50px; letter-spacing: 1px;">
                            <i class="fas fa-times-circle mr-2"></i> <%=Common.getBahasaConfig("TUTUP")%>
                        </button>
                      </div>
                    </div>
                  </div>
                </div>`;

                document.body.insertAdjacentHTML('beforeend', modalHtml);
                
                var newModalElement = document.getElementById(modalId);
                newModalElement.style.setProperty('z-index', zIndexModal, 'important');
                
                if (typeof $ !== 'undefined') { 
                    $('#' + modalId).modal({ backdrop: false, keyboard: false }); 
                    $('#' + modalId).modal('show'); 
                } else {
                    newModalElement.classList.add('show');
                    newModalElement.style.display = 'block';
                }

                var modalBody = document.getElementById("body-" + modalId);

                if (urlHtmlLoad !== "") {
                    try {
                        var res = await fetch(urlHtmlLoad);
                        if (!res.ok) throw new Error("Gagal mengambil respon dari server");
                        var htmlContent = await res.text();
                        modalBody.innerHTML = htmlContent;
                    } catch (e) {
                        modalBody.innerHTML = '<div class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-3x mb-3"></i><h5 class="font-weight-bold"><%=Common.getBahasaConfig("Gagal Memuat Rincian")%></h5><p class="text-muted"><%=Common.getBahasaConfig("Periksa koneksi jaringan Anda dan coba lagi.")%></p></div>';
                    }
                } else {
                    modalBody.innerHTML = `
                        <h5 class="font-weight-bold text-dark mb-3">` + ev.title + `</h5>
                        <table class="table table-borderless table-sm mb-0">
                            <tr><td width="35%" class="text-muted font-weight-bold"><i class="far fa-clock mr-2"></i> Mulai</td><td>: ` + startFmt + `</td></tr>
                            <tr><td class="text-muted font-weight-bold"><i class="far fa-clock mr-2"></i> Selesai</td><td>: ` + endFmt + `</td></tr>
                            <tr><td class="text-muted font-weight-bold"><i class="fas fa-info-circle mr-2"></i> Keterangan</td><td>: ` + (ev.extendedProps.keterangan || "-") + `</td></tr>
                        </table>`;
                }
            }
        });
        kalenderInstance<%=rnd%>.render();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", inisialisasiKalender);
    } else {
        setTimeout(inisialisasiKalender, 300);
    }

})();
</script>