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

    String faIcon = "fas fa-layer-group";
    String judulRaw = "Ringkasan Pembelajaran";
    
    // Logik Hak Akses Menu Ringkasan
    boolean[] ptYa = Common.chekPtAtauSekolah(tbmuser);
    boolean apakahPerguruanTInggi = ptYa[0];
    boolean apakahSekolah = ptYa[1];
    
    boolean isSiswa = tbmuser.getSiswa() != null;
    boolean isGuru = tbmuser.ambilGuru() != null;
    boolean isMahasiswa = tbmuser.getMahasiswa() != null;
    boolean isDosen = tbmuser.ambilDosen() != null;
    boolean isCalonSiswa = tbmuser.getCalonSiswa() != null;
    boolean isBiodataCalonMhs = tbmuser.getBiodataCalonMahasiswa() != null;
    boolean isSivitas = isSiswa || isGuru || isMahasiswa || isDosen;

    // Pentadbir / Pensyarah dibenarkan mengubah struktur pertemuan
    boolean bolehUbahPertemuan = !isSivitas || isDosen;

    // Hak KELOLA (buat/ubah data pembelajaran): Dosen, Guru, dan Admin/Operator DIBENARKAN.
    // DISEMBUNYIKAN untuk Mahasiswa, Siswa, Calon Siswa, dan Biodata Calon Mahasiswa (peserta didik).
    boolean bolehKelola = !isMahasiswa && !isSiswa && !isCalonSiswa && !isBiodataCalonMhs;

    boolean showPerkuliahan = apakahPerguruanTInggi;
    boolean showJadwalPelajaran = apakahSekolah;
    boolean showTugasAkhir = apakahPerguruanTInggi;
    boolean showSkripsi = apakahPerguruanTInggi;
    boolean showKkn = apakahPerguruanTInggi;
    boolean showPkl = apakahPerguruanTInggi;
    boolean showKrs = apakahPerguruanTInggi;
    boolean showKegiatan = true;
    boolean showLes = apakahSekolah;
    
    String defKategori = "kegiatan";
    if (apakahPerguruanTInggi) defKategori = "perkuliahan";
    else if (apakahSekolah) defKategori = "jadwal_pelajaran";

    Fakultas myFakultas = (!isSivitas) ? tbmuser.ambilFakultas() : null;
    Jurusan myJurusan = (!isSivitas) ? tbmuser.ambilJurusan() : null;
    Yayasan myYayasan = (!isSivitas) ? tbmuser.ambilYayasan() : null;
    Sekolah mySekolah = (!isSivitas) ? tbmuser.ambilSekolah() : null;

    List<Fakultas> listFakultas = new ArrayList<Fakultas>();
    List<Jurusan> listJurusan = new ArrayList<Jurusan>();
    List<Sekolah> listSekolah = new ArrayList<Sekolah>();
    List<Yayasan> listYayasan = new ArrayList<Yayasan>();

    Session sessFilter = null;
    try {
        sessFilter = HibernateUtil.openSession();
        if (apakahPerguruanTInggi && !isSivitas) {
            listFakultas = ConstantValues.simpleList(sessFilter.createCriteria(Fakultas.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Fakultas.class);
            listJurusan = ConstantValues.simpleList(sessFilter.createCriteria(Jurusan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Jurusan.class);
        }
        if (apakahSekolah && !isSivitas) {
            listSekolah = ConstantValues.simpleList(sessFilter.createCriteria(Sekolah.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Sekolah.class);
            listYayasan = ConstantValues.simpleList(sessFilter.createCriteria(Yayasan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Yayasan.class);
        }
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/ringkasan.jsp:84");
    } finally {
        ais.common.ElearningSessionUtil.closeQuietly(sessFilter);
        HibernateUtil.closeSessionQuietly(sessFilter);
    }
%>

<style>
    .fade-in-<%=rnd%> { animation: fadeInAnim<%=rnd%> 0.5s ease-in-out; }
    @keyframes fadeInAnim<%=rnd%> { from { opacity: 0; transform: translateY(15px); } to { opacity: 1; transform: translateY(0); } }
    .input-elegan-<%=rnd%> { font-size: 0.85rem; border-radius: 0.5rem; border: 1px solid #ced4da; box-shadow: inset 0 1px 2px rgba(0,0,0,.05); transition: border-color .15s ease-in-out,box-shadow .15s ease-in-out; }
    .input-elegan-<%=rnd%>:focus { border-color: #80bdff; box-shadow: 0 0 0 0.2rem rgba(0,123,255,.25); }
    .input-elegan-<%=rnd%>[disabled] { background-color: #e9ecef; opacity: 1; font-weight: 600; }
    .sidebar-pills-<%=rnd%> .nav-link { color: #6c757d; font-weight: 600; border-radius: 0.5rem; margin-bottom: 0.5rem; transition: all 0.2s; cursor: pointer; padding: 12px 15px; }
    .sidebar-pills-<%=rnd%> .nav-link:hover { background-color: #f1f3f5; }
    .sidebar-pills-<%=rnd%> .nav-link.active { background-color: #f4f6f9; color: #17a2b8; border-left: 4px solid #17a2b8; box-shadow: 0 0.125rem 0.25rem rgba(0, 0, 0, 0.05); }
    .card-ringkasan-<%=rnd%> { transition: all 0.3s ease; border: 1px solid #eef2f5 !important; }
    .card-ringkasan-<%=rnd%>:hover { box-shadow: 0 .5rem 1rem rgba(0,0,0,.1)!important; transform: translateY(-2px); }
    @media (min-width: 768px) { .border-right-md { border-right: 1px solid #eaeaea; } }
    .stat-box-<%=rnd%> { background-color: #f8f9fa; border: 1px solid #edf2f7; border-radius: 6px; padding: 8px 4px; text-align: center; height: 100%; display: flex; flex-direction: column; justify-content: center; align-items: center; transition: all 0.2s; }
    .stat-box-<%=rnd%>:hover { background-color: #e9ecef; border-color: #dee2e6; }
    .stat-box-<%=rnd%> i { font-size: 14px; margin-bottom: 4px; }
    .stat-label-<%=rnd%> { font-size: 9.5px; font-weight: 600; color: #555; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 100%; }
    @media (max-width: 767.98px) {
        #wrapper-ringkasan-<%=rnd%> .col-lg-3 { margin-bottom: 0.5rem; }
        #wrapper-ringkasan-<%=rnd%> .sticky-top { position: static !important; }
        #wrapper-ringkasan-<%=rnd%> .card-body.p-3 { padding: 0.6rem !important; }
        #form-search-<%=rnd%> .col-xl-3, #form-search-<%=rnd%> .col-xl-2, #form-search-<%=rnd%> .col-xl-auto { width: 100% !important; flex: 0 0 100%; max-width: 100%; }
        #form-search-<%=rnd%> .form-control { font-size: 16px !important; }
        .sidebar-pills-<%=rnd%> .nav-link { padding: 10px 12px; font-size: 0.85rem; }
    }
    @media (max-width: 575.98px) {
        #form-search-<%=rnd%> .col-md-6, #form-search-<%=rnd%> .col-md-3 { width: 50% !important; flex: 0 0 50%; max-width: 50%; }
        #form-search-<%=rnd%> .col-xl-auto { width: 100% !important; flex: 0 0 100%; max-width: 100%; }
        #form-search-<%=rnd%> .d-flex.justify-content-end { justify-content: stretch !important; }
        #form-search-<%=rnd%> .d-flex.justify-content-end .btn { flex: 1 1 0; }
    }
</style>

<div id="wrapper-ringkasan-<%=rnd%>" class="container-fluid py-4 px-xl-5">
    <div class="row align-items-center mb-3 pb-2 border-bottom">
        <div class="col-12 mb-2 mb-lg-0">
            <div class="d-flex align-items-center">
                <div class="bg-primary text-white rounded shadow-sm mr-3 me-3 d-flex align-items-center justify-content-center" style="width: 48px; height: 48px; background-image: linear-gradient(180deg, rgba(255,255,255,0.15), rgba(255,255,255,0));">
                    <i class="<%= faIcon %> fa-fw fa-lg"></i>
                </div>
                <div>
                    <h6 class="text-muted small font-weight-bold mb-0" style="text-transform: uppercase; letter-spacing: 0.5px;"><%= Common.getBahasaConfig("Modul") %></h6>
                    <h5 class="fw-bold font-weight-bold text-dark mb-0"><%= Common.getBahasaConfig(judulRaw) %></h5>
                </div>
            </div>
        </div>
    </div>

    <div class="row">
        <div class="col-lg-3 col-xl-2 mb-4">
            <div class="bg-white rounded-lg shadow-sm p-3 p-lg-2 sticky-top" style="top: 20px; z-index: 10;">
                <button class="btn btn-outline-info btn-sm btn-block d-lg-none mb-2 font-weight-bold text-left d-flex justify-content-between align-items-center" type="button" onclick="toggleKategoriMobile<%=rnd%>()">
                    <span><i class="fas fa-list mr-2"></i> <%=Common.getBahasaConfig("Pilihan Kategori")%></span>
                    <i class="fas fa-chevron-down"></i>
                </button>

                <div class="collapse d-lg-block" id="collapseKategori-<%=rnd%>">
                    <div class="nav flex-column nav-pills sidebar-pills-<%=rnd%> mt-2 mt-lg-0" id="v-pills-tab-<%=rnd%>" role="tablist" aria-orientation="vertical">
                        <% if(showPerkuliahan){ %><a class="nav-link <%=defKategori.equals("perkuliahan")?"active":""%>" onclick="ubahKategori<%=rnd%>('perkuliahan', this)"><i class="fas fa-book mr-2 w-20px text-center"></i> <%=Common.getBahasaConfig("Perkuliahan")%></a><% } %>
                        <% if(showJadwalPelajaran){ %><a class="nav-link <%=defKategori.equals("jadwal_pelajaran")?"active":""%>" onclick="ubahKategori<%=rnd%>('jadwal_pelajaran', this)"><i class="fas fa-chalkboard-teacher mr-2 w-20px text-center"></i> <%=Common.getBahasaConfig("Jadwal Pelajaran")%></a><% } %>
                        <% if(showTugasAkhir){ %><a class="nav-link <%=defKategori.equals("tugas_akhir")?"active":""%>" onclick="ubahKategori<%=rnd%>('tugas_akhir', this)"><i class="fas fa-user-tie mr-2 w-20px text-center"></i> <%=Common.getBahasaConfig("Bimbingan TA/Seminar")%></a><% } %>
                        <% if(showSkripsi){ %><a class="nav-link <%=defKategori.equals("skripsi")?"active":""%>" onclick="ubahKategori<%=rnd%>('skripsi', this)"><i class="fas fa-user-graduate mr-2 w-20px text-center"></i> <%=Common.getBahasaConfig("Sidang TA/Skripsi")%></a><% } %>
                        <% if(showKkn){ %><a class="nav-link <%=defKategori.equals("kkn")?"active":""%>" onclick="ubahKategori<%=rnd%>('kkn', this)"><i class="fas fa-map-marked-alt mr-2 w-20px text-center"></i> <%=Common.getBahasaConfig("Kelompok KKN")%></a><% } %>
                        <% if(showPkl){ %><a class="nav-link <%=defKategori.equals("pkl")?"active":""%>" onclick="ubahKategori<%=rnd%>('pkl', this)"><i class="fas fa-briefcase mr-2 w-20px text-center"></i> <%=Common.getBahasaConfig("Kelompok PKL")%></a><% } %>
                        <% if(showKrs){ %><a class="nav-link <%=defKategori.equals("krs")?"active":""%>" onclick="ubahKategori<%=rnd%>('krs', this)"><i class="fas fa-users mr-2 w-20px text-center"></i> <%=Common.getBahasaConfig("Bimb. Akademik")%></a><% } %>
                        <% if(showKegiatan){ %><a class="nav-link <%=defKategori.equals("kegiatan")?"active":""%>" onclick="ubahKategori<%=rnd%>('kegiatan', this)"><i class="fas fa-running mr-2 w-20px text-center"></i> <%=Common.getBahasaConfig("Kegiatan Akademik")%></a><% } %>
                        <% if(showLes){ %><a class="nav-link <%=defKategori.equals("les")?"active":""%>" onclick="ubahKategori<%=rnd%>('les', this)"><i class="fas fa-chalkboard mr-2 w-20px text-center"></i> <%=Common.getBahasaConfig("Kelas Les / Privat")%></a><% } %>
                    </div>
                </div>
            </div>
        </div>

        <div class="col-lg-9 col-xl-10">
            <div class="card border-0 shadow-sm rounded-lg mb-4">
                <div class="card-body p-3">
                    <form id="form-search-<%=rnd%>" onsubmit="event.preventDefault(); resetDanMuatUlang<%=rnd%>();" class="m-0">
                        <div class="row align-items-end">
                            <div class="col-xl-3 col-lg-4 col-md-6 mb-3 px-2">
                                <label class="label-filter-<%=rnd%> text-muted mb-1"><i class="fas fa-search mr-1"></i> <%=Common.getBahasaConfig("Cari Judul / Topik")%></label>
                                <input type="text" id="filter-cari-<%=rnd%>" class="form-control form-control-sm input-elegan-<%=rnd%> py-2" placeholder="<%=Common.getBahasaConfig("Ketik kata kunci...")%>">
                            </div>
                            <div class="col-xl-2 col-lg-2 col-md-3 col-6 mb-3 px-2">
                                <label class="label-filter-<%=rnd%> text-muted mb-1"><i class="far fa-calendar-alt mr-1"></i> <%=Common.getBahasaConfig("T.A")%></label>
                                <select class="form-control form-control-sm input-elegan-<%=rnd%>" id="filter-ta-<%=rnd%>" onchange="resetDanMuatUlang<%=rnd%>()">
                                    <option value=""><%=Common.getBahasaConfig("Semua")%></option>
                                    <% if (Common.tahunAngkatans != null) { for (String strTa : Common.tahunAngkatans) { %><option value="<%=strTa%>"><%=strTa%></option><% } } %>
                                </select>
                            </div>
                            <div class="col-xl-2 col-lg-2 col-md-3 col-6 mb-3 px-2">
                                <label class="label-filter-<%=rnd%> text-muted mb-1"><i class="fas fa-hourglass-half mr-1"></i> <%=Common.getBahasaConfig("Smt")%></label>
                                <select class="form-control form-control-sm input-elegan-<%=rnd%>" id="filter-smt-<%=rnd%>" onchange="resetDanMuatUlang<%=rnd%>()">
                                    <option value=""><%=Common.getBahasaConfig("Semua")%></option>
                                    <option value="<%=Perkuliahan.GANJIL%>"><%=Common.getBahasaConfig("Ganjil")%></option>
                                    <option value="<%=Perkuliahan.GENAP%>"><%=Common.getBahasaConfig("Genap")%></option>
                                    <option value="<%=Perkuliahan.SP%>"><%=Common.getBahasaConfig("Pendek")%></option>
                                </select>
                            </div>

                            <% if (apakahPerguruanTInggi && !isSivitas) { %>
                            <div class="col-xl-2 col-lg-2 col-md-6 col-6 mb-3 px-2">
                                <label class="label-filter-<%=rnd%> text-muted mb-1"><%=Common.getBahasaConfig("Fakultas")%></label>
                                <select class="form-control form-control-sm input-elegan-<%=rnd%>" id="filter-fakultas-<%=rnd%>" onchange="ubahFakultas<%=rnd%>()" <%= (myFakultas != null || myJurusan != null) ? "disabled" : "" %>>
                                    <% if (myFakultas != null) { %><option value="<%=myFakultas.getId()%>" selected><%=myFakultas.getNama()%></option><% } else { %>
                                    <option value=""><%=Common.getBahasaConfig("Semua")%></option>
                                    <% for(Fakultas f : listFakultas) { %><option value="<%=f.getId()%>"><%=f.getNama()%></option><% } } %>
                                </select>
                            </div>
                            <div class="col-xl-3 col-lg-2 col-md-6 col-6 mb-3 px-2">
                                <label class="label-filter-<%=rnd%> text-muted mb-1"><%=Common.getBahasaConfig("Jurusan")%></label>
                                <select class="form-control form-control-sm input-elegan-<%=rnd%>" id="filter-jurusan-<%=rnd%>" onchange="resetDanMuatUlang<%=rnd%>()" <%= (myJurusan != null) ? "disabled" : "" %>>
                                    <% if (myJurusan != null) { %><option value="<%=myJurusan.getId()%>" selected><%=myJurusan.getNama()%></option><% } else { %>
                                    <option value=""><%=Common.getBahasaConfig("Semua")%></option>
                                    <% for(Jurusan j : listJurusan) { %><option value="<%=j.getId()%>"><%=j.getNama()%></option><% } } %>
                                </select>
                            </div>
                            <% } %>

                            <% if (apakahSekolah && !isSivitas) { %>
                            <div class="col-xl-2 col-lg-2 col-md-6 col-6 mb-3 px-2">
                                <label class="label-filter-<%=rnd%> text-muted mb-1"><%=Common.getBahasaConfig("Yayasan")%></label>
                                <select class="form-control form-control-sm input-elegan-<%=rnd%>" id="filter-yayasan-<%=rnd%>" onchange="ubahYayasan<%=rnd%>()" <%= (myYayasan != null || mySekolah != null) ? "disabled" : "" %>>
                                    <% if (myYayasan != null) { %><option value="<%=myYayasan.getId()%>" selected><%=myYayasan.getNama()%></option><% } else { %>
                                    <option value=""><%=Common.getBahasaConfig("Semua")%></option>
                                    <% for(Yayasan y : listYayasan) { %><option value="<%=y.getId()%>"><%=y.getNama()%></option><% } } %>
                                </select>
                            </div>
                            <div class="col-xl-3 col-lg-2 col-md-6 col-6 mb-3 px-2">
                                <label class="label-filter-<%=rnd%> text-muted mb-1"><%=Common.getBahasaConfig("Sekolah")%></label>
                                <select class="form-control form-control-sm input-elegan-<%=rnd%>" id="filter-sekolah-<%=rnd%>" onchange="resetDanMuatUlang<%=rnd%>()" <%= (mySekolah != null) ? "disabled" : "" %>>
                                    <% if (mySekolah != null) { %><option value="<%=mySekolah.getId()%>" selected><%=mySekolah.getNama()%></option><% } else { %>
                                    <option value=""><%=Common.getBahasaConfig("Semua")%></option>
                                    <% for(Sekolah s : listSekolah) { %><option value="<%=s.getId()%>"><%=s.getNama()%></option><% } } %>
                                </select>
                            </div>
                            <% } %>

                            <div class="col-xl-auto col-lg-auto col-md-12 mb-3 px-2 d-flex justify-content-end ml-auto">
                                <button type="button" class="btn btn-outline-secondary btn-sm font-weight-bold px-3 mr-2" onclick="bersihkanFilter<%=rnd%>()"><i class="fas fa-undo"></i></button>
                                <button type="submit" class="btn btn-info btn-sm font-weight-bold px-4 shadow-sm"><i class="fas fa-search mr-2"></i> <%=Common.getBahasaConfig("Terapkan")%></button>
                            </div>
                        </div>
                    </form>
                </div>
            </div>

            <div id="ringkasan-container-<%=rnd%>" class="position-relative min-vh-50"></div>
            
            <div id="wrap-loadmore-<%=rnd%>" class="text-center mt-3 mb-5" style="display:none;">
                <button id="btn-loadmore-<%=rnd%>" class="btn btn-primary rounded-pill px-5 shadow-sm font-weight-bold" onclick="loadRingkasan<%=rnd%>(false)">
                    <i class="fas fa-chevron-down mr-2"></i> <%=Common.getBahasaConfig("Muat Data Selanjutnya")%>
                </button>
            </div>
        </div>
    </div>
</div>

<script>
    // =================================================================================
    // DEKLARASI PEMBOLEH UBAH GLOBAL MODUL
    // =================================================================================
    var rootUrl<%=rnd%> = "<%=Common.ROOT%>";
    var startIdx<%=rnd%> = 0;
    var limit<%=rnd%> = 10;
    var activeKategori<%=rnd%> = "<%=defKategori%>";
    
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
    
    // =================================================================================
    // FUNGSI KAWALAN ANTAR MUKA (UI)
    // =================================================================================
    function ubahFakultas<%=rnd%>() {
        var elFak = document.getElementById("filter-fakultas-<%=rnd%>");
        var elJur = document.getElementById("filter-jurusan-<%=rnd%>");
        if (elFak && elJur && !elJur.disabled) {
            var fId = elFak.value;
            elJur.innerHTML = '<option value=""><%=Common.getBahasaConfig("Semua")%></option>';
            dataJurusan<%=rnd%>.forEach(function(j) {
                if (fId === "" || j.idFakultas === fId) {
                    var opt = document.createElement("option"); opt.value = j.id; opt.text = j.nama; elJur.add(opt);
                }
            });
            resetDanMuatUlang<%=rnd%>();
        }
    }

    function ubahYayasan<%=rnd%>() {
        var elYay = document.getElementById("filter-yayasan-<%=rnd%>");
        var elSek = document.getElementById("filter-sekolah-<%=rnd%>");
        if (elYay && elSek && !elSek.disabled) {
            var yId = elYay.value;
            elSek.innerHTML = '<option value=""><%=Common.getBahasaConfig("Semua")%></option>';
            dataSekolah<%=rnd%>.forEach(function(s) {
                if (yId === "" || s.idYayasan === yId) {
                    var opt = document.createElement("option"); opt.value = s.id; opt.text = s.nama; elSek.add(opt);
                }
            });
            resetDanMuatUlang<%=rnd%>();
        }
    }
    
    function toggleKategoriMobile<%=rnd%>() {
        var collapseEl = document.getElementById("collapseKategori-<%=rnd%>");
        if (collapseEl) {
            if (collapseEl.classList.contains('show')) collapseEl.classList.remove('show');
            else collapseEl.classList.add('show');
        }
    }
    
    function ubahKategori<%=rnd%>(kat, element) {
        activeKategori<%=rnd%> = kat;
        var links = document.querySelectorAll('.sidebar-pills-<%=rnd%> .nav-link');
        links.forEach(function(link){ link.classList.remove('active'); });
        element.classList.add('active');
        
        var collapseEl = document.getElementById("collapseKategori-<%=rnd%>");
        if (collapseEl && window.innerWidth < 992) { 
            collapseEl.classList.remove('show');
        }
        
        resetDanMuatUlang<%=rnd%>();
    }
    
    function bersihkanFilter<%=rnd%>() {
        document.getElementById('filter-cari-<%=rnd%>').value = "";
        document.getElementById('filter-ta-<%=rnd%>').value = "";
        document.getElementById('filter-smt-<%=rnd%>').value = "";
        var fFak = document.getElementById('filter-fakultas-<%=rnd%>');
        if (fFak && !fFak.disabled) { fFak.value = ""; ubahFakultas<%=rnd%>(); }
        
        var fYay = document.getElementById('filter-yayasan-<%=rnd%>');
        if (fYay && !fYay.disabled) { fYay.value = ""; ubahYayasan<%=rnd%>(); }
        
        resetDanMuatUlang<%=rnd%>();
    }
    
    // =================================================================================
    // FUNGSI UTAMA UNTUK MEREFRESH KESELURUHAN TABEL MELALUI _ringkasan_service.jsp
    // =================================================================================
    function resetDanMuatUlang<%=rnd%>() {
        startIdx<%=rnd%> = 0;
        var container = document.getElementById("ringkasan-container-<%=rnd%>");
        if (container) container.innerHTML = "";
        loadRingkasan<%=rnd%>(true);
    }
    
    // =================================================================================
    // FUNGSI RELOAD SPESIFIK UNTUK SATU ITEM KAD (PARTIAL RELOAD VIA load_ringkasan.jsp)
    // =================================================================================
    async function reloadSatuRingkasan<%=rnd%>(idVop) {
        var cardContainer = document.getElementById('card_vop_' + idVop);
        if (cardContainer) {
            try {
                cardContainer.style.opacity = '0.5';
                cardContainer.style.transition = 'opacity 0.3s ease';
                
                var res = await fetch(rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning&s=load_ringkasan&item=" + idVop);
                if (res.ok) {
                    var html = await res.text();
                    if (html.trim().length > 10) {
                        cardContainer.innerHTML = html;
                        cardContainer.style.opacity = '1';
                        return; // Berjaya memuat ulang, tamatkan fungsi di sini
                    }
                }
            } catch(e) {
                console.error("Gagal memuat ulang data ringkasan individu", e);
            }
        }
        // Fallback: Muat ulang keseluruhan data jika ID gagal diakses
        resetDanMuatUlang<%=rnd%>();
    }
    
    // =================================================================================
    // PEMBINAAN ELEMEN HTML UNTUK SETIAP KAD
    // =================================================================================
    function createCardHTML<%=rnd%>(item) {
        var pengajarNama = "<%=Common.getBahasaConfig("Belum Ada Keterangan")%>";
        var pengajarFoto = rootUrl<%=rnd%> + "/images/default_user.png"; 
        
        if (item.pengajars && item.pengajars.length > 0) {
            pengajarNama = item.pengajars[0].nama;
            if (item.pengajars[0].foto && item.pengajars[0].foto.trim() !== "") {
                var urlFoto = item.pengajars[0].foto.trim();
                pengajarFoto = (urlFoto.indexOf("http") === 0 || urlFoto.indexOf("data:image") === 0 || urlFoto.indexOf(rootUrl<%=rnd%>) === 0) ? urlFoto : rootUrl<%=rnd%> + (urlFoto.charAt(0) === '/' ? urlFoto : '/' + urlFoto);
            }
            if (item.pengajars.length > 1) {
                pengajarNama += " (+" + (item.pengajars.length - 1) + " <%=Common.getBahasaConfig("Lainnya")%>)";
            }
        }

        var badgesHtml = '';
        if (item.subtitle1) {
            var s1parts = item.subtitle1.split(" - ");
            s1parts.forEach(function(p) { if(p.trim()!=="") badgesHtml += '<span class="badge badge-light border text-dark mr-1 mb-1 px-2 py-1">' + p.trim() + '</span>'; });
        }
        if (item.subtitle3) {
            var s3parts = item.subtitle3.split("|");
            s3parts.forEach(function(p) { if(p.trim()!=="") badgesHtml += '<span class="badge badge-light border text-dark mr-1 mb-1 px-2 py-1">' + p.trim() + '</span>'; });
        }

        var listHtml = '';
        if (item.subtitle2 && item.subtitle2.trim() !== "") {
            listHtml += '<li class="mb-1"><i class="fas fa-check text-success mr-2"></i>' + item.subtitle2 + '</li>';
        }

        var safeTitle = encodeURIComponent(item.title || "").replace(/'/g, "%27");
        var safeJenis = (item.jenis_vop || "").replace(/'/g, "\\'");
        var safeProp = (item.prop_name || "").replace(/'/g, "\\'");
        
        var vopParams = item.id + ", '" + safeJenis + "', '" + safeProp + "', '" + safeTitle + "', " + (item.is_obe ? "true" : "false") + ", '" + (item.id_kur || "") + "'";
        var workspaceParams = item.id + ", '" + safeJenis + "', '" + safeProp + "', '" + safeTitle + "'";
        
        // Membalut kad di dalam ID unik 'card_vop_{id}' supaya boleh disegarkan secara individu
        var html = `<div id="card_vop_` + item.id + `">
        <div class="card mb-3 shadow-sm border-0 fade-in-<%=rnd%> card-ringkasan-<%=rnd%>">
          <div class="card-body p-3">
             <div class="row m-0">
                <div class="col-md-2 col-lg-2 text-center mb-3 mb-md-0 d-flex flex-column align-items-center justify-content-center border-right-md py-1">
                   <img src="` + pengajarFoto + `" class="rounded-circle mb-2 shadow-sm border" width="65" height="65" style="object-fit:cover; background-color:#fff; cursor: pointer;" onclick="tampilGambar(this)" alt="`+pengajarNama+`" onerror="this.src='` + rootUrl<%=rnd%> + `/images/default_user.png'">
                   <div style="font-size:12px; font-weight:bold; color:#333; line-height:1.2; word-break:break-word;" title="`+pengajarNama+`">`+pengajarNama+`</div>
                   <div style="font-size:10px; color:#777; margin-top:3px; text-transform:uppercase;"><%=Common.getBahasaConfig("Pengajar Utama")%></div>
                </div>
                
                <div class="col-md-5 col-lg-5 mb-3 mb-md-0 px-md-3 py-1">
                   <h6 class="font-weight-bold text-primary mb-2" style="line-height:1.4;" title="`+item.title+`">`+item.title+`</h6>
                   <div class="mb-3">` + badgesHtml + `</div>
                   <ul class="list-unstyled mb-0" style="font-size: 12.5px; color: #555;">` + listHtml + `</ul>
                   <button type="button" class="btn btn-primary btn-sm rounded-pill px-3 mt-3 font-weight-bold" onclick="bukaWorkspace<%=rnd%>(`+workspaceParams+`)"><i class="fas fa-arrow-right mr-1"></i><%=Common.getBahasaConfig("Buka Workspace")%></button>
                </div>
                
                <div class="col-md-5 col-lg-5 py-1">
                   <div class="row no-gutters h-100">
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                           <div class="stat-box-<%=rnd%>" style="cursor:pointer;" onclick="bukaModalDetail<%=rnd%>('peserta', `+vopParams+`)">
                               <i class="fas fa-users text-info"></i>
                               <div class="stat-label-<%=rnd%>"><%=Common.getBahasaConfig("Peserta")%> (`+item.count_mahasiswa+`)</div>
                           </div>
                       </div>
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                           <div class="stat-box-<%=rnd%>" style="cursor:pointer;" onclick="bukaModalDetail<%=rnd%>('pertemuan', `+vopParams+`)">
                               <i class="fas fa-calendar-alt text-danger"></i>
                               <div class="stat-label-<%=rnd%>"><%=Common.getBahasaConfig("Pertemuan")%> (`+item.count_pertemuan+`)</div>
                           </div>
                       </div>
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                           <div class="stat-box-<%=rnd%>">
                               <i class="fas fa-check-circle text-warning"></i>
                               <div class="stat-label-<%=rnd%>"><%=Common.getBahasaConfig("Sesuai RPS")%> (`+item.count_sesuai_rps+`)</div>
                           </div>
                       </div>
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                           <div class="stat-box-<%=rnd%>">
                               <i class="fas fa-comments text-primary"></i>
                               <div class="stat-label-<%=rnd%>"><%=Common.getBahasaConfig("Diskusi")%> (`+item.count_diskusi+`)</div>
                           </div>
                       </div>
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                            <div class="stat-box-<%=rnd%>">
                               <i class="fas fa-file-alt text-info"></i>
                               <div class="stat-label-<%=rnd%>"><%=Common.getBahasaConfig("Materi")%> (`+item.count_materi+`)</div>
                           </div>
                       </div>
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                           <div class="stat-box-<%=rnd%>" style="cursor:pointer;" onclick="bukaModalDetail<%=rnd%>('tugas', `+vopParams+`)">
                               <i class="fas fa-clipboard-list text-primary"></i>
                               <div class="stat-label-<%=rnd%>"><%=Common.getBahasaConfig("Tugas")%> (`+item.count_tugas+`)</div>
                           </div>
                       </div>
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                           <div class="stat-box-<%=rnd%>" style="cursor:pointer;" onclick="bukaModalDetail<%=rnd%>('ujian', `+vopParams+`)">
                               <i class="fas fa-check-square text-success"></i>
                               <div class="stat-label-<%=rnd%>"><%=Common.getBahasaConfig("Ujian")%> (`+item.count_ujian+`)</div>
                           </div>
                       </div>
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                           <div class="stat-box-<%=rnd%>" style="cursor:pointer;" onclick="bukaModalDetail<%=rnd%>('tugas_kelompok', `+vopParams+`)">
                               <i class="fas fa-users-cog text-info"></i>
                               <div class="stat-label-<%=rnd%>"><%=Common.getBahasaConfig("Tgs Kelompok")%> (`+item.count_tugas_kelompok+`)</div>
                           </div>
                       </div>
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                           <div class="stat-box-<%=rnd%>">
                               <i class="fas fa-microphone text-danger"></i>
                               <div class="stat-label-<%=rnd%>"><%=Common.getBahasaConfig("Audio")%> (`+item.count_audio+`)</div>
                           </div>
                       </div>
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                           <div class="stat-box-<%=rnd%>">
                               <i class="fas fa-video text-primary"></i>
                               <div class="stat-label-<%=rnd%>"><%=Common.getBahasaConfig("Video")%> (`+item.count_video+`)</div>
                           </div>
                       </div>
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                           <div class="stat-box-<%=rnd%>">
                               <i class="fas fa-book text-warning"></i>
                               <div class="stat-label-<%=rnd%>"><%=Common.getBahasaConfig("Buku Ref.")%> (`+item.count_buku_referensi+`)</div>
                           </div>
                       </div>
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                           <div class="stat-box-<%=rnd%>">
                               <i class="fas fa-book-open text-success"></i>
                               <div class="stat-label-<%=rnd%>"><%=Common.getBahasaConfig("Buku Ajar")%> (`+item.count_buku_ajar+`)</div>
                           </div>
                       </div>
                   </div>
                </div>
             </div>
          </div>
        </div>
        </div>`;
        return html;
    }

    function bukaWorkspace<%=rnd%>(id, jenisVop, propName, encodedTitle) {
        var title = "";
        try { title = decodeURIComponent(encodedTitle || ""); } catch (ignore) { title = encodedTitle || ""; }
        var workspace = document.querySelector('[data-elearning-workspace]');
        if (!workspace) return;
        workspace.dispatchEvent(new CustomEvent('ais:elearning:course', { detail: {
            id: String(id || ""), jenis: jenisVop || "", propName: propName || "",
            title: title, open: 'Linimasa'
        }}));
    }

    // =================================================================================
    // FUNGSI SUNTIKAN MODAL & MUAT TURUN (EXPORT)
    // =================================================================================
    function unduhLaporan<%=rnd%>(format, tipe, idVop, jenisVop, propName) {
        var urlApi = rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_" + tipe + "_service&id=" + idVop + "&jenis=" + jenisVop + "&propName=" + propName + "&format=" + format;
        window.open(urlApi, '_blank');
    }

    // =================================================================================
    // PENTING: Menutup modal dan MENYEGARKAN HANYA 1 KAD SAJA (Partial Reload)
    // =================================================================================
    function tutupModalDetail<%=rnd%>(modalId, idVop) {
        if (typeof $ !== 'undefined') { $('#' + modalId).modal('hide'); }
        setTimeout(function() {
            var el = document.getElementById(modalId);
            if (el) el.remove();
            
            var bd = document.querySelectorAll('.modal-backdrop');
            bd.forEach(function(b) { b.remove(); });
            document.body.classList.remove('modal-open');
            document.body.style.paddingRight = '';
            
            // Muat Ulang Data Spesifik (Hanya 1 Kad) via load_ringkasan.jsp
            if (idVop) {
                reloadSatuRingkasan<%=rnd%>(idVop);
            } else {
                resetDanMuatUlang<%=rnd%>();
            }
        }, 300);
    }

    function bukaModalFormUbah<%=rnd%>(url, tipe, idVop, jenisVop, propName, judulVopEncoded, isObe, idKur) {
        if (typeof window.variable === 'undefined') window.variable = 0;
        var cm = 'cm_' + (++window.variable);
        
        loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Pengaturan Pertemuan")%>', url + '&contentModal=' + cm, '80%', 'hidden', '', cm);
        
        var checkExist = setInterval(function() {
            var modalEl = document.getElementById(cm);
            if (modalEl) {
                clearInterval(checkExist);
                $(modalEl).on('hidden.bs.modal', function () {
                    // Memuat ulang jadual pertemuan supaya data terkini dipaparkan
                    bukaModalDetail<%=rnd%>(tipe, idVop, jenisVop, propName, judulVopEncoded, isObe, idKur);
                });
            }
        }, 200);
    }
    
    // =================================================================================
    // HAK AKSES & FUNGSI BUAT DATA BARU (Ujian / Tugas / Tugas Kelompok / Pertemuan)
    // Hanya untuk Dosen, Guru, dan Admin. Disembunyikan untuk peserta didik.
    // =================================================================================
    var bolehKelola<%=rnd%> = <%= bolehKelola %>;

    // Konteks modal detail terakhir supaya boleh dibuka semula selepas menyimpan data baru
    window.detailCtx<%=rnd%> = null;

    // Segarkan kad + buka semula modal detail (jika ada konteks) selepas berjaya menyimpan
    function segarkanSetelahBuat<%=rnd%>(idVop) {
        reloadSatuRingkasan<%=rnd%>(idVop);
        var ctx = window.detailCtx<%=rnd%>;
        if (ctx && String(ctx.idVop) === String(idVop)) {
            setTimeout(function() {
                bukaModalDetail<%=rnd%>(ctx.tipe, ctx.idVop, ctx.jenisVop, ctx.propName, ctx.judulVopEncoded, ctx.isObe, ctx.idKur);
            }, 450);
        }
    }

    // Buka borang BUAT data baru untuk satu pertemuan tertentu
    function bukaFormBuat<%=rnd%>(tipeBuat, pertemuanId, idVop) {
        if (!bolehKelola<%=rnd%>) return;
        if (typeof window.variable === 'undefined') window.variable = 0;
        var cm = 'cm_' + (++window.variable);
        var afterSaveEnc = encodeURIComponent("segarkanSetelahBuat<%=rnd%>('" + idVop + "');");
        var base = rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true";
        var url = "", title = "", saveFn = "simpanDataHelper_" + cm + "();";
        if (tipeBuat === 'ujian') {
            title = "<%=Common.getBahasaConfig("Buat Ujian Baru")%>";
            url = base + "&p=elearning%2Fujian&s=ujian_baru&pertemuan=" + pertemuanId + "&afterSave=" + afterSaveEnc;
        } else if (tipeBuat === 'tugas') {
            title = "<%=Common.getBahasaConfig("Buat Tugas Baru")%>";
            url = base + "&p=elearning%2Ftugas&s=tugas_baru&jenis=Pertemuan&pertemuan=" + pertemuanId + "&afterSave=" + afterSaveEnc;
        } else if (tipeBuat === 'tugas_kelompok') {
            title = "<%=Common.getBahasaConfig("Buat Tugas Kelompok Baru")%>";
            url = base + "&p=elearning%2Ftugas&s=tugas_baru&jenis=TugasKelompok&pertemuan=" + pertemuanId + "&afterSave=" + afterSaveEnc;
        } else { return; }
        loadModalContentCustomSimpan(title, url + '&contentModal=' + cm, '75%', true, saveFn, cm);
    }

    // Buka halaman KELOLA pertemuan penuh (materi/audio/video/diskusi/tugas/ujian)
    function kelolaPertemuan<%=rnd%>(pertemuanId, idVop) {
        if (!bolehKelola<%=rnd%>) return;
        if (typeof window.variable === 'undefined') window.variable = 0;
        var cm = 'cm_' + (++window.variable);
        var url = rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning&s=daftar_peserta_absen&pertemuan=" + pertemuanId + "&contentModal=" + cm;
        loadModalContentCustomSimpan("<%=Common.getBahasaConfig("Kelola Pertemuan")%>", url, '90%', null, '', cm);
        var checkExist = setInterval(function() {
            var modalEl = document.getElementById(cm);
            if (modalEl) {
                clearInterval(checkExist);
                if (typeof $ !== 'undefined') {
                    $(modalEl).on('hidden.bs.modal', function () { segarkanSetelahBuat<%=rnd%>(idVop); });
                }
            }
        }, 200);
    }

    // Pemilih pertemuan sebelum buka borang buat (untuk butang "Buat Baru" di header modal detail)
    async function pilihPertemuanLaluBuat<%=rnd%>(tipeBuat, idVop, jenisVop, propName) {
        if (!bolehKelola<%=rnd%>) return;
        var pickId = 'pickPtm_' + tipeBuat + '_' + idVop;
        var existing = document.getElementById(pickId);
        if (existing) existing.remove();

        if (typeof window.variable === 'undefined') { window.variable = 0; }
        window.variable++;
        var zIndexModal = 1080 + (window.variable * 2);

        var labelBuat = tipeBuat === 'ujian' ? "<%=Common.getBahasaConfig("Ujian")%>" : (tipeBuat === 'tugas_kelompok' ? "<%=Common.getBahasaConfig("Tugas Kelompok")%>" : "<%=Common.getBahasaConfig("Tugas")%>");

        var html = `
        <div class="modal" id="`+pickId+`" tabindex="-1" role="dialog" style="z-index:`+zIndexModal+`; background:rgba(0,0,0,0.5);">
          <div class="modal-dialog modal-dialog-centered" role="document">
            <div class="modal-content border-0 shadow-lg">
              <div class="modal-header bg-light">
                <h6 class="modal-title font-weight-bold"><i class="fas fa-calendar-check mr-2 text-info"></i> <%=Common.getBahasaConfig("Pilih Pertemuan")%></h6>
                <button type="button" class="close" onclick="document.getElementById('`+pickId+`').remove();"><span>&times;</span></button>
              </div>
              <div class="modal-body">
                <p class="text-muted mb-2" style="font-size:0.85rem;"><%=Common.getBahasaConfig("Pilih pertemuan yang akan ditambahkan")%> `+labelBuat+`:</p>
                <select id="`+pickId+`_sel" class="form-control mb-2"><option value=""><%=Common.getBahasaConfig("Memuat pertemuan...")%></option></select>
                <div id="`+pickId+`_msg" class="small text-danger"></div>
              </div>
              <div class="modal-footer bg-white">
                <button type="button" class="btn btn-secondary btn-sm" onclick="document.getElementById('`+pickId+`').remove();"><%=Common.getBahasaConfig("Batal")%></button>
                <button type="button" id="`+pickId+`_ok" class="btn btn-primary btn-sm" disabled><i class="fas fa-arrow-right mr-1"></i> <%=Common.getBahasaConfig("Lanjut")%></button>
              </div>
            </div>
          </div>
        </div>`;
        document.body.insertAdjacentHTML('beforeend', html);
        var modalPick = document.getElementById(pickId);
        modalPick.style.display = 'block';

        try {
            var urlApi = rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_pertemuan_service&id=" + idVop + "&jenis=" + jenisVop + "&propName=" + propName;
            var res = await fetch(urlApi);
            var arr = await res.json();
            var sel = document.getElementById(pickId + '_sel');
            var okBtn = document.getElementById(pickId + '_ok');
            if (!arr || arr.length === 0) {
                sel.innerHTML = '<option value="">-</option>';
                document.getElementById(pickId + '_msg').innerHTML = "<%=Common.getBahasaConfig("Belum ada pertemuan. Silakan buat pertemuan terlebih dahulu.")%>";
                return;
            }
            var opts = '<option value=""><%=Common.getBahasaConfig("-- Pilih Pertemuan --")%></option>';
            arr.forEach(function(p) {
                var topik = (p.topik_raw || '-');
                opts += '<option value="' + p.id + '">' + "<%=Common.getBahasaConfig("Pertemuan")%> " + (p.pertemuanKe || '-') + ' - ' + topik + '</option>';
            });
            sel.innerHTML = opts;
            sel.onchange = function() { okBtn.disabled = !sel.value; };
            okBtn.onclick = function() {
                if (!sel.value) return;
                var pid = sel.value;
                modalPick.remove();
                bukaFormBuat<%=rnd%>(tipeBuat, pid, idVop);
            };
        } catch(e) {
            document.getElementById(pickId + '_msg').innerHTML = "<%=Common.getBahasaConfig("Gagal memuat daftar pertemuan.")%>";
        }
    }

    async function bukaModalDetail<%=rnd%>(tipe, idVop, jenisVop, propName, judulVopEncoded, isObe, idKur) {
        var judulVop = decodeURIComponent(judulVopEncoded);
        var modalId = "modalDetail_" + tipe + "_" + idVop;

        // Simpan konteks untuk auto-refresh selepas buat data baru
        window.detailCtx<%=rnd%> = { tipe: tipe, idVop: idVop, jenisVop: jenisVop, propName: propName, judulVopEncoded: judulVopEncoded, isObe: isObe, idKur: idKur };

        var existingModal = document.getElementById(modalId);
        if (existingModal) existingModal.remove();
        
        var judulHeader = "";
        var iconHeader = "";
        if (tipe === 'peserta') { judulHeader = "<%=Common.getBahasaConfig("Daftar Peserta")%>"; iconHeader = "fas fa-users"; }
        else if (tipe === 'pertemuan') { judulHeader = "<%=Common.getBahasaConfig("Daftar Pertemuan")%>"; iconHeader = "fas fa-calendar-alt"; }
        else if (tipe === 'ujian') { judulHeader = "<%=Common.getBahasaConfig("Daftar Ujian")%>"; iconHeader = "fas fa-check-square"; }
        else if (tipe === 'tugas') { judulHeader = "<%=Common.getBahasaConfig("Daftar Tugas")%>"; iconHeader = "fas fa-clipboard-list"; }
        else if (tipe === 'tugas_kelompok') { judulHeader = "<%=Common.getBahasaConfig("Daftar Tugas Kelompok")%>"; iconHeader = "fas fa-users-cog"; }

        if (!document.getElementById('css-modal-pertemuan-<%=rnd%>')) {
            var css = `
            @media (min-width: 992px) { 
                .modal-dialog-pertemuan { max-width: 95% !important; } 
                .modal-dialog-pertemuan .modal-content { height: 85vh !important; }
                .modal-dialog-pertemuan .table-responsive { max-height: calc(85vh - 140px) !important; }
            }`;
            document.head.insertAdjacentHTML('beforeend', '<style id="css-modal-pertemuan-<%=rnd%>">'+css+'</style>');
        }

        var isPertemuan = (tipe === 'pertemuan');
        var classUkuranModal = isPertemuan ? "modal-dialog-pertemuan" : "modal-lg";
        
        var btnUbahPertemuan = "";
        var btnBuatBaru = "";
        <% if (bolehKelola) { %>
            if (isPertemuan && jenisVop === 'Perkuliahan') {
                if (isObe) {
                    var urlObe = rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_edit_rps&id=" + idKur + "&perkuliahan=" + idVop;
                    btnUbahPertemuan = `<button class="btn btn-sm btn-outline-primary font-weight-bold shadow-sm mr-1" onclick="bukaModalFormUbah<%=rnd%>('`+urlObe+`', '`+tipe+`', '`+idVop+`', '`+jenisVop+`', '`+propName+`', '`+judulVopEncoded+`', `+isObe+`, '`+idKur+`')"><i class="fas fa-edit mr-1"></i> <%=Common.getBahasaConfig("Buat / Ubah Pertemuan (OBE)")%></button>`;
                } else {
                    var urlNonObe = rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning&s=buat_pertemuan&perkuliahan=" + idVop;
                    btnUbahPertemuan = `<button class="btn btn-sm btn-outline-primary font-weight-bold shadow-sm mr-1" onclick="bukaModalFormUbah<%=rnd%>('`+urlNonObe+`', '`+tipe+`', '`+idVop+`', '`+jenisVop+`', '`+propName+`', '`+judulVopEncoded+`', `+isObe+`, '`+idKur+`')"><i class="fas fa-calendar-plus mr-1"></i> <%=Common.getBahasaConfig("Buat / Ubah Pertemuan")%></button>`;
                }
            }

            // Butang "Buat Baru" di header modal Ujian / Tugas / Tugas Kelompok (pilih pertemuan dahulu)
            if (tipe === 'ujian') {
                btnBuatBaru = `<button class="btn btn-sm btn-success font-weight-bold shadow-sm mr-1" onclick="pilihPertemuanLaluBuat<%=rnd%>('ujian', '`+idVop+`', '`+jenisVop+`', '`+propName+`')"><i class="fas fa-plus mr-1"></i> <%=Common.getBahasaConfig("Buat Ujian Baru")%></button>`;
            } else if (tipe === 'tugas') {
                btnBuatBaru = `<button class="btn btn-sm btn-success font-weight-bold shadow-sm mr-1" onclick="pilihPertemuanLaluBuat<%=rnd%>('tugas', '`+idVop+`', '`+jenisVop+`', '`+propName+`')"><i class="fas fa-plus mr-1"></i> <%=Common.getBahasaConfig("Buat Tugas Baru")%></button>`;
            } else if (tipe === 'tugas_kelompok') {
                btnBuatBaru = `<button class="btn btn-sm btn-success font-weight-bold shadow-sm mr-1" onclick="pilihPertemuanLaluBuat<%=rnd%>('tugas_kelompok', '`+idVop+`', '`+jenisVop+`', '`+propName+`')"><i class="fas fa-plus mr-1"></i> <%=Common.getBahasaConfig("Buat Tugas Kelompok Baru")%></button>`;
            }
        <% } %>

        if (typeof window.variable === 'undefined') { window.variable = 0; }
        window.variable++;
        var zIndexModal = 1050 + (window.variable * 2);
        
        var modalHtml = `
        <div class="modal" id="`+modalId+`" tabindex="-1" role="dialog" aria-hidden="true" style="z-index: `+zIndexModal+`; background: rgba(0,0,0,0.5);">
          <div class="modal-dialog `+classUkuranModal+` modal-dialog-centered" role="document">
            <div class="modal-content border-0 shadow-lg">
              <div class="modal-header bg-light border-bottom-0 pb-3">
                <h5 class="modal-title font-weight-bold text-dark"><i class="`+iconHeader+` mr-2 text-info"></i> `+judulHeader+`</h5>
                <button type="button" class="close" onclick="tutupModalDetail<%=rnd%>('`+modalId+`', '`+idVop+`')" aria-label="<%=Common.getBahasaConfig("Tutup")%>"><span aria-hidden="true">&times;</span></button>
              </div>
              <div class="modal-body p-0">
                <div class="p-3 border-bottom bg-white d-flex flex-column flex-md-row justify-content-between align-items-md-center">
                  <div class="mb-2 mb-md-0"><span class="badge badge-info mb-1">`+jenisVop+`</span><br><strong class="text-secondary">` + judulVop + `</strong></div>
                  <div>
                     `+btnBuatBaru+`
                     `+btnUbahPertemuan+`
                     <button class="btn btn-sm btn-outline-success font-weight-bold shadow-sm mr-1" onclick="unduhLaporan<%=rnd%>('excel', '`+tipe+`', '`+idVop+`', '`+jenisVop+`', '`+propName+`')"><i class="fas fa-file-excel mr-1"></i> <%=Common.getBahasaConfig("Unduh Excel")%></button>
                     <button class="btn btn-sm btn-outline-danger font-weight-bold shadow-sm" onclick="unduhLaporan<%=rnd%>('pdf', '`+tipe+`', '`+idVop+`', '`+jenisVop+`', '`+propName+`')"><i class="fas fa-file-pdf mr-1"></i> <%=Common.getBahasaConfig("Cetak PDF")%></button>
                  </div>
                </div>
                <div class="table-responsive bg-light" style="max-height: 450px; overflow-y: auto;">
                  <table class="table table-hover table-bordered mb-0" style="font-size:0.85rem;">
                     <thead class="thead-dark" id="th-`+modalId+`"></thead>
                     <tbody id="tb-`+modalId+`" class="bg-white">
                        <tr><td colspan="6" class="text-center py-5"><i class="fas fa-circle-notch fa-spin fa-2x text-info mb-2"></i><br> <%=Common.getBahasaConfig("Memuat data, mohon tunggu...")%></td></tr>
                     </tbody>
                  </table>
                </div>
              </div>
              <div class="modal-footer bg-white border-top-0 pt-2">
                <button type="button" class="btn btn-secondary btn-sm px-4 rounded-pill font-weight-bold" onclick="tutupModalDetail<%=rnd%>('`+modalId+`', '`+idVop+`')"><%=Common.getBahasaConfig("Tutup Panel")%></button>
              </div>
            </div>
          </div>
        </div>`;

        document.body.insertAdjacentHTML('beforeend', modalHtml);
        if (typeof $ !== 'undefined') { 
            var $modal = $('#' + modalId);
            $modal.modal({ backdrop: false, keyboard: false }); 
            $modal.modal('show'); 
        } else {
            document.getElementById(modalId).style.display = 'block';
        }

        var modalBody = document.getElementById("body-" + modalId);

        try {
            var urlApi = rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_" + tipe + "_service&id=" + idVop + "&jenis=" + jenisVop + "&propName=" + propName;
            var response = await fetch(urlApi);
            if (!response.ok) throw new Error("Gagal mengambil data.");
            var json = await response.json();
            var headHtml = ""; var bodyHtml = "";
            
            if (!json || json.length === 0) {
                bodyHtml = `<tr><td colspan="6" class="text-center py-4 text-muted"><i class="fas fa-info-circle fa-2x mb-2 text-warning"></i><br><%=Common.getBahasaConfig("Data belum tersedia.")%></td></tr>`;
            } else {
                if (tipe === 'peserta') {
                    headHtml = `<tr><th width="5%" class="text-center align-middle"><%=Common.getBahasaConfig("No")%></th><th width="10%" class="text-center align-middle"><%=Common.getBahasaConfig("Foto")%></th><th width="25%" class="align-middle"><%=Common.getBahasaConfig("NIM / No. Induk")%></th><th class="align-middle"><%=Common.getBahasaConfig("Nama Peserta")%></th></tr>`;
                    json.forEach((j, idx) => { 
                        var fotoHtml = `<img src="`+(j.foto||rootUrl<%=rnd%>+'/images/default_user.png')+`" class="rounded-circle shadow-sm border" style="width:35px; height:35px; object-fit:cover; cursor:pointer;" onclick="tampilGambar(this)" onerror="this.src='`+rootUrl<%=rnd%>+`/images/default_user.png'">`;
                        bodyHtml += `<tr><td class="text-center align-middle">`+(idx+1)+`</td><td class="text-center align-middle">`+fotoHtml+`</td><td class="align-middle"><span class="badge badge-light border text-dark font-weight-bold px-2 py-1">`+(j.nim||'-')+`</span></td><td class="font-weight-bold text-dark align-middle">`+(j.nama||'-')+`</td></tr>`; 
                    });
                } else if (tipe === 'pertemuan') {
                    var thAksiPtm = bolehKelola<%=rnd%> ? `<th width="12%" class="text-center"><%=Common.getBahasaConfig("Aksi")%></th>` : ``;
                    headHtml = `<tr><th class="text-center" width="5%">Ke</th><th width="14%">Topik & Info</th><th width="13%">Waktu & Ruang</th><th width="18%">Metode & Indikator</th><th width="22%">Pengalaman Belajar & Tugas</th><th width="16%">Rujukan & Catatan</th>`+thAksiPtm+`</tr>`;
                    json.forEach(function(item) {
                        var tdAksiPtm = ``;
                        if (bolehKelola<%=rnd%>) {
                            tdAksiPtm = `<td class="text-center align-middle" style="white-space:nowrap;" data-aksi-baris>
                                <div class="d-flex flex-wrap justify-content-center" style="gap:4px;">
                                    <button class="btn btn-outline-secondary btn-sm py-0 px-2" title="<%=Common.getBahasaConfig("Kelola Pertemuan")%>" onclick="kelolaPertemuan<%=rnd%>('`+item.id+`','`+idVop+`')"><i class="fas fa-cog"></i></button>
                                    <button class="btn btn-outline-success btn-sm py-0 px-2" title="<%=Common.getBahasaConfig("Buat Ujian Baru")%>" onclick="bukaFormBuat<%=rnd%>('ujian','`+item.id+`','`+idVop+`')"><i class="fas fa-file-signature"></i></button>
                                    <button class="btn btn-outline-primary btn-sm py-0 px-2" title="<%=Common.getBahasaConfig("Buat Tugas Baru")%>" onclick="bukaFormBuat<%=rnd%>('tugas','`+item.id+`','`+idVop+`')"><i class="fas fa-clipboard-list"></i></button>
                                    <button class="btn btn-outline-info btn-sm py-0 px-2" title="<%=Common.getBahasaConfig("Buat Tugas Kelompok Baru")%>" onclick="bukaFormBuat<%=rnd%>('tugas_kelompok','`+item.id+`','`+idVop+`')"><i class="fas fa-users-cog"></i></button>
                                </div>
                            </td>`;
                        }
                        bodyHtml += `<tr><td class="text-center font-weight-bold align-middle" style="font-size: 1.1rem;">` + (item.pertemuanKe || '-') + `</td><td><div class="mb-1" style="font-size: 0.95rem;">` + (item.topik || '-') + `</div><small class="text-muted"><i class="fas fa-info-circle text-info"></i> ` + (item.infoPertemuan || '-') + `</small></td><td><small class="font-weight-bold text-dark"><i class="far fa-calendar-alt text-primary mr-1"></i> ` + (item.tanggal || '-') + `</small><br><small><i class="far fa-clock text-primary mr-1"></i> ` + (item.mulai || '-') + ` - ` + (item.selesai || '-') + `</small><br><small><i class="fas fa-map-marker-alt text-primary mr-1"></i> ` + (item.ruang || '-') + `</small></td><td><small><b>Metode:</b> <span class="text-info">` + (item.metodePembelajaran || '-') + `</span> (` + (item.waktupembelajaran || '-') + `)</small><div class="mt-1 pt-1" style="font-size: 0.75rem; border-top: 1px dashed #ddd;"><b>Indikator:</b><br><span class="text-muted">` + (item.indikator || '-') + `</span></div></td><td><small><b>Pengalaman Belajar:</b><br><span class="text-muted">` + (item.pengalamanBelajar || '-') + `</span></small><div class="mt-1 pt-1" style="font-size: 0.75rem; border-top: 1px dashed #ddd;"><b>Tugas & Penilaian:</b><br><span class="text-muted">` + (item.tugasDanPenilaian || '-') + `</span></div></td><td><small><b>Ref 1:</b> <span class="text-muted">` + (item.bukuRujukan1 || '-') + `</span></small><br><small><b>Ref 2:</b> <span class="text-muted">` + (item.bukuRujukan2 || '-') + `</span></small><div class="mt-1 pt-1" style="font-size: 0.75rem; border-top: 1px dashed #ddd;"><b>Catatan:</b><br><span class="text-muted">` + (item.catatan || '-') + `</span></div></td>`+tdAksiPtm+`</tr>`;
                    });
                } else if (tipe === 'ujian') {
                    headHtml = `<tr><th width="5%" class="text-center"><%=Common.getBahasaConfig("No")%></th><th><%=Common.getBahasaConfig("Nama Ujian")%></th><th width="20%"><%=Common.getBahasaConfig("Tanggal Ujian")%></th><th width="25%"><%=Common.getBahasaConfig("Sifat Ujian")%></th></tr>`;
                    json.forEach((j, idx) => { bodyHtml += `<tr><td class="text-center">`+(idx+1)+`</td><td class="font-weight-bold">`+(j.nama||'-')+`</td><td>`+(j.tanggal||'-')+`</td><td>`+(j.sifat||'-')+`</td></tr>`; });
                } else if (tipe === 'tugas' || tipe === 'tugas_kelompok') {
                    headHtml = `<tr><th width="5%" class="text-center"><%=Common.getBahasaConfig("No")%></th><th><%=Common.getBahasaConfig("Judul Tugas")%></th><th width="22%"><%=Common.getBahasaConfig("Tanggal Mulai")%></th><th width="22%"><%=Common.getBahasaConfig("Tanggal Sampai")%></th></tr>`;
                    json.forEach((j, idx) => { bodyHtml += `<tr><td class="text-center">`+(idx+1)+`</td><td class="font-weight-bold">`+(j.judul||'-')+`</td><td><span class="text-info"><i class="far fa-calendar-alt mr-1"></i> `+(j.mulai||'-')+`</span></td><td><span class="text-danger"><i class="far fa-clock mr-1"></i> `+(j.sampai||'-')+`</span></td></tr>`; });
                }
            }

            document.getElementById("th-" + modalId).innerHTML = headHtml;
            document.getElementById("tb-" + modalId).innerHTML = bodyHtml;

        } catch (err) {
            document.getElementById("tb-" + modalId).innerHTML = `<tr><td colspan="6" class="text-center py-4 text-danger"><i class="fas fa-times-circle fa-2x mb-2"></i><br><%=Common.getBahasaConfig("Gagal memuat rincian data.")%></td></tr>`;
        }
    }

    async function loadRingkasan<%=rnd%>(isInit) {
        var container = document.getElementById("ringkasan-container-<%=rnd%>");
        var btnLoad = document.getElementById("btn-loadmore-<%=rnd%>");
        var wrapLoad = document.getElementById("wrap-loadmore-<%=rnd%>");
        
        if (isInit) {
            container.innerHTML = '<div class="text-center text-secondary py-5 fade-in-<%=rnd%>"><i class="fas fa-circle-notch fa-spin fa-3x text-info mb-3"></i><p class="font-weight-bold"><%=Common.getBahasaConfig("Menyiapkan ringkasan...")%></p></div>';
            wrapLoad.style.display = "none";
        } else {
            if (btnLoad) {
                btnLoad.disabled = true;
                btnLoad.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i> <%=Common.getBahasaConfig("Memuat...")%>';
            }
        }

        var q_cari = encodeURIComponent(document.getElementById('filter-cari-<%=rnd%>').value.trim());
        var q_ta = encodeURIComponent(document.getElementById('filter-ta-<%=rnd%>').value);
        var q_smt = encodeURIComponent(document.getElementById('filter-smt-<%=rnd%>').value);
        
        var q_fak = document.getElementById('filter-fakultas-<%=rnd%>') ? encodeURIComponent(document.getElementById('filter-fakultas-<%=rnd%>').value) : "";
        var q_jur = document.getElementById('filter-jurusan-<%=rnd%>') ? encodeURIComponent(document.getElementById('filter-jurusan-<%=rnd%>').value) : "";
        var q_yay = document.getElementById('filter-yayasan-<%=rnd%>') ? encodeURIComponent(document.getElementById('filter-yayasan-<%=rnd%>').value) : "";
        var q_sek = document.getElementById('filter-sekolah-<%=rnd%>') ? encodeURIComponent(document.getElementById('filter-sekolah-<%=rnd%>').value) : "";

        var urlApi = rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_ringkasan_service&kategori=" + activeKategori<%=rnd%> + "&q_cari=" + q_cari + "&q_ta=" + q_ta + "&q_smt=" + q_smt + "&q_fakultas=" + q_fak + "&q_jurusan=" + q_jur + "&q_yayasan=" + q_yay + "&q_sekolah=" + q_sek + "&mulai=" + startIdx<%=rnd%> + "&banyak=" + limit<%=rnd%>;
        try {
            var response = await fetch(urlApi);
            if (!response.ok) throw new Error("Gagal mengambil data dari server.");
            var json = await response.json();
            
            if (isInit) container.innerHTML = "";
            if (json.data && json.data.length > 0) {
                var htmlInject = "";
                json.data.forEach(function(item){ htmlInject += createCardHTML<%=rnd%>(item); });
                container.insertAdjacentHTML('beforeend', htmlInject);
                
                startIdx<%=rnd%> += json.data.length;
                if (startIdx<%=rnd%> < json.total) {
                    wrapLoad.style.display = "block";
                } else {
                    wrapLoad.style.display = "none";
                }
            } else {
                if (isInit) {
                    container.innerHTML = '<div class="text-center text-muted py-5 fade-in-<%=rnd%>"><i class="fas fa-folder-open fa-4x mb-3 text-light"></i><h5 class="font-weight-bold text-secondary mt-3"><%=Common.getBahasaConfig("Tidak Ada Data")%></h5><p class="text-muted"><%=Common.getBahasaConfig("Belum ada ringkasan yang sesuai untuk kategori ini.")%></p></div>';
                }
                wrapLoad.style.display = "none";
            }
        } catch (error) {
            console.error(error);
            if (isInit) container.innerHTML = '<div class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-3x mb-3"></i><h5><%=Common.getBahasaConfig("Terjadi Kesalahan")%></h5><p>'+error.message+'</p></div>';
        } finally {
            if (!isInit && btnLoad) {
                btnLoad.disabled = false;
                btnLoad.innerHTML = '<i class="fas fa-chevron-down mr-2"></i> <%=Common.getBahasaConfig("Muat Data Selanjutnya")%>';
            }
        }
    }

    // Pemulaan automatik paparan keseluruhan
    loadRingkasan<%=rnd%>(true);

</script>
