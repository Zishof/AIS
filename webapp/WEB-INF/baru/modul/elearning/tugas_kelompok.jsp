<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.sekolah.Yayasan"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>

<%
    String rnd = Common.getGeneratedBarCode(7);
    Tbmuser tbmuser = Common.getCurrentUser(request);
    
    // Variabel Header Modul Khusus Tugas Kelompok
    String faIcon = "fas fa-users-cog";
    String judulRaw = "Linimasa Tugas Kelompok";
    
    boolean isSekolahAtauYayasan = false;
    boolean isSivitas = false;
    
    if (tbmuser != null) {
        if (tbmuser.ambilSekolah() != null || tbmuser.ambilYayasan() != null) isSekolahAtauYayasan = true;
        if (tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null || tbmuser.getSiswa() != null || tbmuser.ambilGuru() != null) isSivitas = true;
    }

    List<Fakultas> listFakultas = new ArrayList<Fakultas>();
    List<Jurusan> listJurusan = new ArrayList<Jurusan>();
    List<Sekolah> listSekolah = new ArrayList<Sekolah>();
    List<Yayasan> listYayasan = new ArrayList<Yayasan>();

    Session sessFilter = HibernateUtil.openSession();
    try {
        listFakultas = ConstantValues.simpleList(sessFilter.createCriteria(Fakultas.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Fakultas.class);
        listJurusan = ConstantValues.simpleList(sessFilter.createCriteria(Jurusan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Jurusan.class);
        
        if (isSekolahAtauYayasan) {
            listSekolah = ConstantValues.simpleList(sessFilter.createCriteria(Sekolah.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Sekolah.class);
            listYayasan = ConstantValues.simpleList(sessFilter.createCriteria(Yayasan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Yayasan.class);
        }
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/tugas_kelompok.jsp:47");
    } finally {
        try { sessFilter.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/tugas_kelompok.jsp:49");}
        ais.common.ElearningSessionUtil.closeQuietly(sessFilter);
        HibernateUtil.closeSessionQuietly(sessFilter);
    }
%>

<style>
    .fade-in-<%=rnd%> { animation: fadeInAnim<%=rnd%> 0.5s ease-in-out; }
    @keyframes fadeInAnim<%=rnd%> { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
    .input-elegan-<%=rnd%> { font-size: 0.85rem; border-radius: 0.5rem; border: 1px solid #ced4da; box-shadow: inset 0 1px 2px rgba(0,0,0,.05); transition: border-color .15s ease-in-out,box-shadow .15s ease-in-out; }
    .input-elegan-<%=rnd%>:focus { border-color: #80bdff; box-shadow: 0 0 0 0.2rem rgba(0,123,255,.25); }
    .nav-pills-custom-<%=rnd%> .nav-link { color: #6c757d; font-weight: 600; border-radius: 0.5rem; margin-right: 0.5rem; margin-bottom: 0.5rem; background-color: #f8f9fa; transition: all 0.2s; cursor: pointer; }
    .nav-pills-custom-<%=rnd%> .nav-link.active { background-color: #17a2b8; color: #fff; box-shadow: 0 0.125rem 0.25rem rgba(0, 0, 0, 0.075); }
    .label-filter-<%=rnd%> { font-size: 0.8rem; font-weight: 600; color: #5a5c69; text-transform: uppercase; }
    @media (max-width: 767.98px) {
        #pills-tab-<%=rnd%> { flex-wrap: nowrap; overflow-x: auto; -webkit-overflow-scrolling: touch; padding-bottom: 0.5rem; scrollbar-width: none; }
        #pills-tab-<%=rnd%>::-webkit-scrollbar { display: none; }
        #pills-tab-<%=rnd%> .nav-item { flex: 0 0 auto; }
        .nav-pills-custom-<%=rnd%> .nav-link { white-space: nowrap; font-size: 0.78rem; padding: 0.4rem 0.65rem !important; margin-bottom: 0; }
        #wrapper-linimasa-<%=rnd%> .col-xl-6:last-child .btn-group { width: 100% !important; }
        #wrapper-linimasa-<%=rnd%> .col-xl-6:last-child .btn { flex: 1 1 0; font-size: 0.78rem; padding: 0.4rem !important; }
    }
    /* ====== ROMBAK MOBILE (rentang tanggal/angka stack + target sentuh + no-overflow) ====== */
    .rentang-group-<%=rnd%> { display: flex; align-items: center; gap: .4rem; }
    .rentang-group-<%=rnd%> .form-control { text-align: center; }
    .rentang-sep-<%=rnd%> { color: #adb5bd; flex: 0 0 auto; }
    @media (max-width: 767.98px) {
        #wrapper-linimasa-<%=rnd%> { padding-left: .6rem !important; padding-right: .6rem !important; }
        #wrapper-linimasa-<%=rnd%> .card-body { padding: 1rem !important; }
        #wrapper-linimasa-<%=rnd%> .rentang-group-<%=rnd%> { flex-direction: column; align-items: stretch; gap: .35rem; }
        #wrapper-linimasa-<%=rnd%> .rentang-group-<%=rnd%> .rentang-sep-<%=rnd%> { display: none; }
        #wrapper-linimasa-<%=rnd%> .rentang-group-<%=rnd%> .form-control { width: 100% !important; }
        #wrapper-linimasa-<%=rnd%> .btn { min-height: 40px; }
        #wrapper-linimasa-<%=rnd%> .label-filter-<%=rnd%> { font-size: 0.72rem; }
    }
</style>

<div id="wrapper-linimasa-<%=rnd%>" class="container-fluid px-2 px-md-4 py-4">

    <div class="row align-items-center mb-4 pb-2">
        <div class="col-xl-6 col-lg-6 col-md-12 mb-3 mb-lg-0">
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
        
        <div class="col-xl-6 col-lg-6 col-md-12 d-flex justify-content-lg-end justify-content-start">
            <div class="btn-group shadow-sm" role="group">
                <button class="btn btn-white border text-secondary font-weight-bold px-4 py-2" style="background-color: #ffffff;" type="button" onclick="resetFilter<%=rnd%>()" title="<%=Common.getBahasaConfig("Segarkan dan muat ulang data")%>">
                    <i class="fas fa-sync-alt mr-2"></i> <%=Common.getBahasaConfig("Segarkan")%>
                </button>
                <button class="btn btn-white border text-info font-weight-bold px-4 py-2" style="background-color: #ffffff;" type="button" onclick="toggleFilter<%=rnd%>()">
                    <i class="fas fa-filter mr-2"></i> <%=Common.getBahasaConfig("Saring Pencarian")%>
                </button>
            </div>
        </div>
    </div>

    <div class="collapse mb-4" id="collapseFilter-<%=rnd%>">
        <div class="card border-0 shadow-sm rounded-lg" style="background-color: #ffffff;">
            <div class="card-body p-4">
                
                <ul class="nav nav-pills nav-pills-custom-<%=rnd%> mb-4 border-bottom pb-3" id="pills-tab-<%=rnd%>" role="tablist">
                    <li class="nav-item"><a class="nav-link active" onclick="gantiTab<%=rnd%>(event, 'pane-akademik-<%=rnd%>')"><i class="fas fa-university mr-1"></i> <%=Common.getBahasaConfig("Akademik")%></a></li>
                    <li class="nav-item"><a class="nav-link" onclick="gantiTab<%=rnd%>(event, 'pane-personal-<%=rnd%>')"><i class="fas fa-users mr-1"></i> <%=Common.getBahasaConfig("Personal")%></a></li>
                    <li class="nav-item"><a class="nav-link" onclick="gantiTab<%=rnd%>(event, 'pane-sumber-<%=rnd%>')"><i class="fas fa-project-diagram mr-1"></i> <%=Common.getBahasaConfig("Sumber Kegiatan")%></a></li>
                    <li class="nav-item"><a class="nav-link" onclick="gantiTab<%=rnd%>(event, 'pane-materi-<%=rnd%>')"><i class="fas fa-book-open mr-1"></i> <%=Common.getBahasaConfig("Substansi Tugas")%></a></li>
                    <li class="nav-item"><a class="nav-link" onclick="gantiTab<%=rnd%>(event, 'pane-fasilitas-<%=rnd%>')"><i class="fas fa-calendar-alt mr-1"></i> <%=Common.getBahasaConfig("Waktu & Ruang")%></a></li>
                </ul>

                <form id="form-filter-<%=rnd%>" onsubmit="event.preventDefault(); terapkanFilter<%=rnd%>();">
                    <div class="tab-content" id="pills-tabContent-<%=rnd%>">
                        <div class="tab-pane fade show active" id="pane-akademik-<%=rnd%>" role="tabpanel">
                            <div class="row align-items-end">
                                <div class="col-md-3 mb-3">
                                    <label class="label-filter-<%=rnd%>"><%=Common.getBahasaConfig("Tahun Akademik")%></label>
                                    <select class="form-control input-elegan-<%=rnd%>" id="filter-ta-<%=rnd%>" onchange="terapkanFilter<%=rnd%>()">
                                        <option value=""><%=Common.getBahasaConfig("Semua TA")%></option>
                                        <% if (Common.tahunAngkatans != null) { for (String strTa : Common.tahunAngkatans) { %><option value="<%=strTa%>"><%=strTa%></option><% } } %>
                                    </select>
                                </div>
                                <div class="col-md-3 mb-3">
                                    <label class="label-filter-<%=rnd%>"><%=Common.getBahasaConfig("Semester")%></label>
                                    <select class="form-control input-elegan-<%=rnd%>" id="filter-smt-<%=rnd%>" onchange="terapkanFilter<%=rnd%>()">
                                        <option value=""><%=Common.getBahasaConfig("Semua")%></option>
                                        <option value="<%=Perkuliahan.GANJIL%>"><%=Common.getBahasaConfig("Ganjil")%></option>
                                        <option value="<%=Perkuliahan.GENAP%>"><%=Common.getBahasaConfig("Genap")%></option>
                                        <option value="<%=Perkuliahan.SP%>"><%=Common.getBahasaConfig("Pendek")%></option>
                                    </select>
                                </div>
                                <div class="col-md-3 mb-3">
                                    <label class="label-filter-<%=rnd%>"><%=Common.getBahasaConfig("Fakultas")%></label>
                                    <select class="form-control input-elegan-<%=rnd%>" id="filter-fakultas-<%=rnd%>" onchange="ubahFakultas<%=rnd%>()">
                                        <option value=""><%=Common.getBahasaConfig("Semua")%></option>
                                        <% for(Fakultas f : listFakultas) { %><option value="<%=f.getId()%>"><%=f.getNama()%></option><% } %>
                                    </select>
                                </div>
                                <div class="col-md-3 mb-3">
                                    <label class="label-filter-<%=rnd%>"><%=Common.getBahasaConfig("Jurusan")%></label>
                                    <select class="form-control input-elegan-<%=rnd%>" id="filter-jurusan-<%=rnd%>">
                                        <option value=""><%=Common.getBahasaConfig("Semua")%></option>
                                        <% for(Jurusan j : listJurusan) { %><option value="<%=j.getId()%>"><%=j.getNama()%></option><% } %>
                                    </select>
                                </div>
                                <% if (isSekolahAtauYayasan) { %>
                                <div class="col-md-6 mb-3">
                                    <label class="label-filter-<%=rnd%>"><%=Common.getBahasaConfig("Yayasan")%></label>
                                    <select class="form-control input-elegan-<%=rnd%>" id="filter-yayasan-<%=rnd%>" onchange="ubahYayasan<%=rnd%>()">
                                        <option value=""><%=Common.getBahasaConfig("Semua")%></option>
                                        <% for(Yayasan y : listYayasan) { %><option value="<%=y.getId()%>"><%=y.getNama()%></option><% } %>
                                    </select>
                                </div>
                                <div class="col-md-6 mb-3">
                                    <label class="label-filter-<%=rnd%>"><%=Common.getBahasaConfig("Sekolah")%></label>
                                    <select class="form-control input-elegan-<%=rnd%>" id="filter-sekolah-<%=rnd%>">
                                        <option value=""><%=Common.getBahasaConfig("Semua")%></option>
                                        <% for(Sekolah s : listSekolah) { %><option value="<%=s.getId()%>"><%=s.getNama()%></option><% } %>
                                    </select>
                                </div>
                                <% } %>
                            </div>
                            <h6 class="text-primary font-weight-bold border-bottom pb-2 mb-3 mt-3"><i class="fas fa-book mr-2"></i> <%=Common.getBahasaConfig("Pelajaran & Kelas")%></h6>
                            <div class="row">
                                <div class="col-md-6 mb-3">
                                    <label class="label-filter-<%=rnd%>"><%=Common.getBahasaConfig("Matakuliah (Kode / Nama)")%></label>
                                    <input type="text" id="filter-matakuliah-<%=rnd%>" class="form-control input-elegan-<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Matakuliah...")%>">
                                </div>
                                <div class="col-md-6 mb-3">
                                    <label class="label-filter-<%=rnd%>"><%=Common.getBahasaConfig("Kelas Perkuliahan")%></label>
                                    <input type="text" id="filter-kelas-kuliah-<%=rnd%>" class="form-control input-elegan-<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Nama Kelas...")%>">
                                </div>
                                <% if (isSekolahAtauYayasan) { %>
                                <div class="col-md-6 mb-3"><label class="label-filter-<%=rnd%>"><%=Common.getBahasaConfig("Matapelajaran (Kode / Nama)")%></label><input type="text" id="filter-matapelajaran-<%=rnd%>" class="form-control input-elegan-<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Matapelajaran...")%>"></div>
                                <div class="col-md-6 mb-3"><label class="label-filter-<%=rnd%>"><%=Common.getBahasaConfig("Kelas Siswa")%></label><input type="text" id="filter-kelas-siswa-<%=rnd%>" class="form-control input-elegan-<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Nama Kelas...")%>"></div>
                                <% } %>
                            </div>
                        </div>

                        <div class="tab-pane fade" id="pane-personal-<%=rnd%>" role="tabpanel">
                            <div class="row align-items-end">
                                <div class="col-md-6 col-lg mb-3"><label class="label-filter-<%=rnd%>"><%=Common.getBahasaConfig("Dosen")%></label><input type="text" id="filter-dosen-<%=rnd%>" class="form-control input-elegan-<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Nama / NIDN")%>"></div>
                                <div class="col-md-6 col-lg mb-3"><label class="label-filter-<%=rnd%>"><%=Common.getBahasaConfig("Mahasiswa")%></label><input type="text" id="filter-mahasiswa-<%=rnd%>" class="form-control input-elegan-<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Nama / NIM")%>"></div>
                                <% if (isSekolahAtauYayasan) { %>
                                <div class="col-md-6 col-lg mb-3"><label class="label-filter-<%=rnd%>"><%=Common.getBahasaConfig("Guru")%></label><input type="text" id="filter-guru-<%=rnd%>" class="form-control input-elegan-<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Nama / NUPTK")%>"></div>
                                <div class="col-md-6 col-lg mb-3"><label class="label-filter-<%=rnd%>"><%=Common.getBahasaConfig("Siswa")%></label><input type="text" id="filter-siswa-<%=rnd%>" class="form-control input-elegan-<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Nama / No. Induk")%>"></div>
                                <% } %>
                            </div>
                        </div>

                        <div class="tab-pane fade" id="pane-sumber-<%=rnd%>" role="tabpanel">
                            <div class="row align-items-end">
                                <% 
                                    String[] namaSumber = {"Perkuliahan", "Jadwal Ujian PMB", "Request Tugas Akhir", "Kelompok KKN", "Kelompok PKL", "Skripsi", "KRS Mahasiswa", "Jadwal Ujian PSB", "Pertemuan PSB", "Jadwal Ujian Pegawai", "Jadwal Pelajaran", "Kelas Les Siswa", "Grup Pertemuan", "Formulir Kegiatan", "Data Produk Kursus"};
                                    String[] idSumber = {"perkuliahan", "jadwalUjianPMB", "mahasiswaRequestTugasAkhir", "kelompokKkn", "kelompokPkl", "skripsi", "krsMahasiswa", "jadwalUjianPSB", "jadwalPertemuanPSB", "jadwalUjianPegawai", "jadwalPelajaran", "kelasLesSiswa", "pertemuanPunyaGrupPertemuan", "formulirKegiatan", "komponenDataProdukKursus"};
                                    
                                    for(int i=0; i<namaSumber.length; i++) {
                                %>
                                <div class="col-md-3 col-sm-4 col-6 mb-3">
                                    <label class="label-filter-<%=rnd%>" style="font-size:0.75rem;"><%=Common.getBahasaConfig(namaSumber[i])%></label>
                                    <select class="form-control input-elegan-<%=rnd%>" id="filter-has-<%=idSumber[i]%>-<%=rnd%>">
                                        <option value=""><%=Common.getBahasaConfig("Semua")%></option>
                                        <option value="1"><%=Common.getBahasaConfig("Ya")%></option>
                                        <option value="0"><%=Common.getBahasaConfig("Tidak")%></option>
                                    </select>
                                </div>
                                <% } %>
                            </div>
                        </div>

                        <div class="tab-pane fade" id="pane-materi-<%=rnd%>" role="tabpanel">
                            <div class="row align-items-end">
                                <div class="col-md-12 col-lg-6 mb-3">
                                    <label class="label-filter-<%=rnd%> text-primary"><%=Common.getBahasaConfig("Judul Tugas Kelompok")%></label>
                                    <input type="text" id="filter-nama-tugas-<%=rnd%>" class="form-control input-elegan-<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketikkan judul tugas...")%>">
                                </div>
                                <div class="col-md-6 col-lg-6 mb-3"><label class="label-filter-<%=rnd%>"><%=Common.getBahasaConfig("Topik / Tema Pertemuan")%></label><input type="text" id="filter-topik-<%=rnd%>" class="form-control input-elegan-<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Kata kunci topik...")%>"></div>
                                <div class="col-md-4 col-lg mb-3"><label class="label-filter-<%=rnd%>"><%=Common.getBahasaConfig("Indikator")%></label><input type="text" id="filter-indikator-<%=rnd%>" class="form-control input-elegan-<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Kata kunci indikator...")%>"></div>
                                <div class="col-md-4 col-lg mb-3"><label class="label-filter-<%=rnd%>"><%=Common.getBahasaConfig("Metode")%></label><input type="text" id="filter-metode-<%=rnd%>" class="form-control input-elegan-<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: Diskusi, Kelompok")%>"></div>
                                <div class="col-md-4 col-lg mb-3"><label class="label-filter-<%=rnd%>"><%=Common.getBahasaConfig("Catatan")%></label><input type="text" id="filter-catatan-<%=rnd%>" class="form-control input-elegan-<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Catatan tambahan...")%>"></div>
                            </div>
                        </div>

                        <div class="tab-pane fade" id="pane-fasilitas-<%=rnd%>" role="tabpanel">
                            <div class="row align-items-end">
                                <div class="col-md-4 mb-3"><label class="label-filter-<%=rnd%>"><%=Common.getBahasaConfig("Ruangan")%></label><input type="text" id="filter-ruang-<%=rnd%>" class="form-control input-elegan-<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Nama atau Kode Ruang")%>"></div>
                                <div class="col-md-4 mb-3"><label class="label-filter-<%=rnd%>"><%=Common.getBahasaConfig("Rujukan Utama")%></label><input type="text" id="filter-buku1-<%=rnd%>" class="form-control input-elegan-<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Judul Buku 1")%>"></div>
                                <div class="col-md-4 mb-3"><label class="label-filter-<%=rnd%>"><%=Common.getBahasaConfig("Rujukan Tambahan")%></label><input type="text" id="filter-buku2-<%=rnd%>" class="form-control input-elegan-<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Judul Buku 2")%>"></div>
                                
                                <div class="col-md-3 mb-3">
                                    <label class="label-filter-<%=rnd%> text-primary"><%=Common.getBahasaConfig("Pilihan Rentang")%></label>
                                    <select class="form-control input-elegan-<%=rnd%> border-primary" id="filter-rentang-combo-<%=rnd%>" onchange="kalkulasiRentangWaktu<%=rnd%>()">
                                        <option value="custom" selected><%=Common.getBahasaConfig("Tanggal Custom")%></option>
                                        <option value="-1"><%=Common.getBahasaConfig("1 Hari")%></option>
                                        <option value="0"><%=Common.getBahasaConfig("3 Hari")%></option>
                                        <option value="1"><%=Common.getBahasaConfig("1 Minggu")%></option>
                                        <option value="2"><%=Common.getBahasaConfig("3 Minggu")%></option>
                                        <option value="3"><%=Common.getBahasaConfig("1 Bulan")%></option>
                                        <option value="4"><%=Common.getBahasaConfig("3 Bulan")%></option>
                                        <% if(isSivitas) { %>
                                        <option value="5"><%=Common.getBahasaConfig("1 Semester")%></option>
                                        <option value="6"><%=Common.getBahasaConfig("1 Tahun")%></option>
                                        <% } %>
                                    </select>
                                </div>

                                <div class="col-md-5 mb-3">
                                    <label class="label-filter-<%=rnd%>"><%=Common.getBahasaConfig("Rentang Tanggal Mulai")%></label>
                                    <div class="rentang-group-<%=rnd%>">
                                        <input type="date" id="filter-tgl-mulai-<%=rnd%>" class="form-control input-elegan-<%=rnd%>">
                                        <span class="rentang-sep-<%=rnd%>"><i class="fas fa-minus"></i></span>
                                        <input type="date" id="filter-tgl-selesai-<%=rnd%>" class="form-control input-elegan-<%=rnd%>">
                                    </div>
                                </div>
                                <div class="col-md-4 mb-3">
                                    <label class="label-filter-<%=rnd%>"><%=Common.getBahasaConfig("Pertemuan Ke-")%></label>
                                    <div class="rentang-group-<%=rnd%>">
                                        <input type="number" id="filter-ke-mulai-<%=rnd%>" class="form-control input-elegan-<%=rnd%>" placeholder="Min">
                                        <span class="rentang-sep-<%=rnd%>"><i class="fas fa-arrow-right"></i></span>
                                        <input type="number" id="filter-ke-selesai-<%=rnd%>" class="form-control input-elegan-<%=rnd%>" placeholder="Max">
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="row mt-3 pt-3 border-top">
                        <div class="col-12 text-end text-right">
                            <button type="submit" class="btn btn-info btn-sm rounded-pill px-5 shadow-sm font-weight-bold">
                                <i class="fas fa-search mr-2"></i> <%=Common.getBahasaConfig("Terapkan Pencarian")%>
                            </button>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <div id="paging-top-<%=rnd%>" class="d-flex justify-content-center mb-4">
        <button id="btn-load-next-<%=rnd%>" class="btn btn-primary btn-sm rounded-pill px-4 py-2 shadow-sm font-weight-bold" style="display: none;">
            <i class="fas fa-chevron-circle-up mr-2"></i> 
            <span id="label-btn-next-<%=rnd%>"><%=Common.getBahasaConfig("Muat Tugas Kelompok Selanjutnya")%></span> 
            <span class="badge badge-light text-primary ml-2 rounded-pill" id="count-next-<%=rnd%>">0</span>
        </button>
    </div>

    <div id="timeline-container-<%=rnd%>" class="timeline-container position-relative">
        <div id="loading-init-<%=rnd%>" class="text-center text-secondary py-5 fade-in-<%=rnd%>">
            <i class="fas fa-circle-notch fa-spin fa-3x text-info mb-3"></i>
            <p class="mb-0 font-weight-bold"><%=Common.getBahasaConfig("Sedang memuat data tugas kelompok...")%></p>
        </div>
        <div id="timeline-content-next-<%=rnd%>"></div>
        <div id="timeline-content-prev-<%=rnd%>"></div>
    </div>

    <div id="paging-bottom-<%=rnd%>" class="d-flex justify-content-center mt-4 mb-5">
        <button id="btn-load-back-<%=rnd%>" class="btn btn-outline-secondary btn-sm rounded-pill px-4 py-2 shadow-sm font-weight-bold bg-white" style="display: none;">
            <i class="fas fa-history mr-2"></i>
            <span id="label-btn-back-<%=rnd%>"><%=Common.getBahasaConfig("Muat Tugas Kelompok Sebelumnya")%></span> 
            <span class="badge badge-secondary ml-2 rounded-pill" id="count-back-<%=rnd%>">0</span>
        </button>
    </div>

</div>

<script>
(function() {
    var rootUrl<%=rnd%> = "<%=Common.ROOT%>";
    var startNext<%=rnd%> = 0; 
    var startBack<%=rnd%> = 0; 
    var limit<%=rnd%> = 10;
    var angkaUnik<%=rnd%> = new Set();
    
    var dataJurusan<%=rnd%> = [
        <% for (Jurusan j : listJurusan) { Long fId = (j.getFakultas() != null) ? j.getFakultas().getId() : 0L; %>
        { id: "<%=j.getId()%>", nama: "<%=j.getNama()%>", idFakultas: "<%=fId%>" },
        <% } %>
    ];
    <% if (isSekolahAtauYayasan) { %>
    var dataSekolah<%=rnd%> = [
        <% for (Sekolah s : listSekolah) { Long yId = (s.getYayasan() != null) ? s.getYayasan().getId() : 0L; %>
        { id: "<%=s.getId()%>", nama: "<%=s.getNama()%>", idYayasan: "<%=yId%>" },
        <% } %>
    ];
    <% } %>

    window.gantiTab<%=rnd%> = function(event, targetPaneId) {
        event.preventDefault(); 
        var semuaTab = document.querySelectorAll('#pills-tab-<%=rnd%> .nav-link');
        semuaTab.forEach(function(tab) { tab.classList.remove('active'); });
        event.currentTarget.classList.add('active');
        var semuaPane = document.querySelectorAll('#pills-tabContent-<%=rnd%> .tab-pane');
        semuaPane.forEach(function(pane) { pane.classList.remove('show', 'active'); });
        var paneTujuan = document.getElementById(targetPaneId);
        if (paneTujuan) paneTujuan.classList.add('show', 'active');
    };

    window.ubahFakultas<%=rnd%> = function(isReset) {
        var fId = document.getElementById("filter-fakultas-<%=rnd%>").value;
        var jSel = document.getElementById("filter-jurusan-<%=rnd%>");
        jSel.innerHTML = '<option value=""><%=Common.getBahasaConfig("Semua Jurusan")%></option>';
        dataJurusan<%=rnd%>.forEach(function(j) {
            if (fId === "" || j.idFakultas === fId) {
                var opt = document.createElement("option");
                opt.value = j.id; opt.text = j.nama; jSel.add(opt);
            }
        });
    };

    window.ubahYayasan<%=rnd%> = function(isReset) {
        <% if (isSekolahAtauYayasan) { %>
        var yId = document.getElementById("filter-yayasan-<%=rnd%>").value;
        var sSel = document.getElementById("filter-sekolah-<%=rnd%>");
        sSel.innerHTML = '<option value=""><%=Common.getBahasaConfig("Semua Sekolah")%></option>';
        dataSekolah<%=rnd%>.forEach(function(s) {
            if (yId === "" || s.idYayasan === yId) {
                var opt = document.createElement("option");
                opt.value = s.id; opt.text = s.nama; sSel.add(opt);
            }
        });
        <% } %>
    };

    window.toggleFilter<%=rnd%> = function() {
        var elemenFilter = document.getElementById("collapseFilter-<%=rnd%>");
        if (elemenFilter) {
            if (typeof $ !== 'undefined') $(elemenFilter).collapse('toggle');
            else elemenFilter.classList.toggle("show");
        }
    };

    function formatTglLocal(date) {
        var m = '' + (date.getMonth() + 1), d = '' + date.getDate(), y = date.getFullYear();
        if (m.length < 2) m = '0' + m; if (d.length < 2) d = '0' + d;
        return [y, m, d].join('-');
    }

    window.kalkulasiRentangWaktu<%=rnd%> = function() {
        var val = document.getElementById('filter-rentang-combo-<%=rnd%>').value;
        var tglMulai = document.getElementById('filter-tgl-mulai-<%=rnd%>');
        var tglSelesai = document.getElementById('filter-tgl-selesai-<%=rnd%>');

        if (val === 'custom') {
            tglMulai.disabled = false;
            tglSelesai.disabled = false;
            tglMulai.value = '';
            tglSelesai.value = '';
        } else {
            var offsetHari = 0;
            if(val == '-1') offsetHari = 1;
            else if(val == '0') offsetHari = 3;
            else if(val == '1') offsetHari = 7;
            else if(val == '2') offsetHari = 21;
            else if(val == '3') offsetHari = 30;
            else if(val == '4') offsetHari = 90;
            else if(val == '5') offsetHari = 180;
            else if(val == '6') offsetHari = 365;

            var dMulai = new Date(); dMulai.setDate(dMulai.getDate() - offsetHari);
            var dSelesai = new Date(); dSelesai.setDate(dSelesai.getDate() + offsetHari);

            tglMulai.value = formatTglLocal(dMulai);
            tglSelesai.value = formatTglLocal(dSelesai);

            tglMulai.disabled = true;
            tglSelesai.disabled = true;
        }
    };

    kalkulasiRentangWaktu<%=rnd%>();

    var getFilterVal<%=rnd%> = function(id) {
        var el = document.getElementById(id);
        return el ? encodeURIComponent(el.value.trim()) : "";
    };

    var bangunQueryFilter<%=rnd%> = function() {
        var q = "";
        q += "&q_nama_tugas=" + getFilterVal<%=rnd%>("filter-nama-tugas-<%=rnd%>");
        
        q += "&q_ta=" + getFilterVal<%=rnd%>("filter-ta-<%=rnd%>");
        q += "&q_smt=" + getFilterVal<%=rnd%>("filter-smt-<%=rnd%>");
        q += "&q_fakultas=" + getFilterVal<%=rnd%>("filter-fakultas-<%=rnd%>");
        q += "&q_jurusan=" + getFilterVal<%=rnd%>("filter-jurusan-<%=rnd%>");
        q += "&q_matakuliah=" + getFilterVal<%=rnd%>("filter-matakuliah-<%=rnd%>");
        q += "&q_kelas_kuliah=" + getFilterVal<%=rnd%>("filter-kelas-kuliah-<%=rnd%>");
        
        <% if (isSekolahAtauYayasan) { %>
            q += "&q_yayasan=" + getFilterVal<%=rnd%>("filter-yayasan-<%=rnd%>");
            q += "&q_sekolah=" + getFilterVal<%=rnd%>("filter-sekolah-<%=rnd%>");
            q += "&q_guru=" + getFilterVal<%=rnd%>("filter-guru-<%=rnd%>");
            q += "&q_siswa=" + getFilterVal<%=rnd%>("filter-siswa-<%=rnd%>");
            q += "&q_matapelajaran=" + getFilterVal<%=rnd%>("filter-matapelajaran-<%=rnd%>");
            q += "&q_kelas_siswa=" + getFilterVal<%=rnd%>("filter-kelas-siswa-<%=rnd%>");
        <% } %>

        q += "&q_dosen=" + getFilterVal<%=rnd%>("filter-dosen-<%=rnd%>");
        q += "&q_mahasiswa=" + getFilterVal<%=rnd%>("filter-mahasiswa-<%=rnd%>");
        q += "&q_topik=" + getFilterVal<%=rnd%>("filter-topik-<%=rnd%>");
        q += "&q_catatan=" + getFilterVal<%=rnd%>("filter-catatan-<%=rnd%>");
        q += "&q_indikator=" + getFilterVal<%=rnd%>("filter-indikator-<%=rnd%>");
        q += "&q_metode=" + getFilterVal<%=rnd%>("filter-metode-<%=rnd%>");
        q += "&q_buku1=" + getFilterVal<%=rnd%>("filter-buku1-<%=rnd%>");
        q += "&q_buku2=" + getFilterVal<%=rnd%>("filter-buku2-<%=rnd%>");
        q += "&q_ruang=" + getFilterVal<%=rnd%>("filter-ruang-<%=rnd%>");
        q += "&q_tgl_mulai=" + getFilterVal<%=rnd%>("filter-tgl-mulai-<%=rnd%>");
        q += "&q_tgl_selesai=" + getFilterVal<%=rnd%>("filter-tgl-selesai-<%=rnd%>");
        q += "&q_ke_mulai=" + getFilterVal<%=rnd%>("filter-ke-mulai-<%=rnd%>");
        q += "&q_ke_selesai=" + getFilterVal<%=rnd%>("filter-ke-selesai-<%=rnd%>");
        
        var sumberFields = ["perkuliahan", "jadwalUjianPMB", "mahasiswaRequestTugasAkhir", "kelompokKkn", "kelompokPkl", "skripsi", "krsMahasiswa", "jadwalUjianPSB", "jadwalPertemuanPSB", "jadwalUjianPegawai", "jadwalPelajaran", "kelasLesSiswa", "pertemuanPunyaGrupPertemuan", "formulirKegiatan", "komponenDataProdukKursus"];
        for(var i=0; i<sumberFields.length; i++) {
            q += "&q_has_" + sumberFields[i] + "=" + getFilterVal<%=rnd%>("filter-has-" + sumberFields[i] + "-<%=rnd%>");
        }
        
        return q;
    };

    var setStatusTombol<%=rnd%> = function(arah, sedangMemuat) {
        var btnId = arah === 'next' ? "btn-load-next-<%=rnd%>" : "btn-load-back-<%=rnd%>";
        var labelId = arah === 'next' ? "label-btn-next-<%=rnd%>" : "label-btn-back-<%=rnd%>";
        var btn = document.getElementById(btnId);
        var labelElemen = document.getElementById(labelId);
        if (!btn || !labelElemen) return;
        
        if (sedangMemuat) {
            btn.disabled = true;
            if (!labelElemen.hasAttribute('data-asli')) { labelElemen.setAttribute('data-asli', labelElemen.innerHTML); }
            labelElemen.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i> <%=Common.getBahasaConfig("Memproses...")%>';
        } else {
            btn.disabled = false;
            var htmlAsli = labelElemen.getAttribute('data-asli');
            if (htmlAsli) { labelElemen.innerHTML = htmlAsli; labelElemen.removeAttribute('data-asli'); }
        }
    };

    async function muatLinimasa<%=rnd%>(arah, isFilterBaru) {
        if (isFilterBaru) {
            startNext<%=rnd%> = 0; startBack<%=rnd%> = 0; angkaUnik<%=rnd%>.clear();
            var divContainer = document.getElementById("timeline-container-<%=rnd%>");
            if(divContainer) {
                divContainer.innerHTML = '<div id="loading-init-<%=rnd%>" class="text-center text-secondary py-5 fade-in-<%=rnd%>"><i class="fas fa-circle-notch fa-spin fa-3x text-info mb-3"></i><p class="mb-0 font-weight-bold"><%=Common.getBahasaConfig("Sedang memuat data tugas kelompok...")%></p></div>' +
                                         '<div id="timeline-content-next-<%=rnd%>"></div>' +
                                         '<div id="timeline-content-prev-<%=rnd%>"></div>';
            }
        }

        var nilaiMulai<%=rnd%> = 0; var parameterArah<%=rnd%> = "";
        if (arah === 'next') { nilaiMulai<%=rnd%> = startNext<%=rnd%>++; parameterArah<%=rnd%> = "&dir=next"; } 
        else if (arah === 'prev') { nilaiMulai<%=rnd%> = startBack<%=rnd%>++; parameterArah<%=rnd%> = "&dir=prev"; }
        
        if (arah === 'next') { nilaiMulai<%=rnd%> = startNext<%=rnd%> - 1; } 
        else if (arah === 'prev') { nilaiMulai<%=rnd%> = startBack<%=rnd%> - 1; }

        if (arah !== 'init') setStatusTombol<%=rnd%>(arah, true);

        try {
            var queryFilter<%=rnd%> = bangunQueryFilter<%=rnd%>();
            var servletUrlApi<%=rnd%> = rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_linimasa_tugas_kelompok&mulai=" + (arah === 'next' ? startNext<%=rnd%> : startBack<%=rnd%>) + "&banyak=" + limit<%=rnd%> + parameterArah<%=rnd%> + queryFilter<%=rnd%>;
            
            var responApi<%=rnd%> = await fetch(servletUrlApi<%=rnd%>);
            if (!responApi<%=rnd%>.ok) throw new Error('<%=Common.getBahasaConfigJS("Terjadi kesalahan komunikasi HTTP.")%>');
            
            var dataRespon<%=rnd%> = await responApi<%=rnd%>.json();
            var hitungSelanjutnya<%=rnd%> = dataRespon<%=rnd%>.countNext || 0;
            var hitungSebelumnya<%=rnd%> = dataRespon<%=rnd%>.countBack || 0;
            var dataJson<%=rnd%> = dataRespon<%=rnd%>.data;

            if (!dataJson<%=rnd%> || !Array.isArray(dataJson<%=rnd%>) || dataJson<%=rnd%>.length === 0) {
                perbaruiTampilanPaginasi<%=rnd%>(hitungSelanjutnya<%=rnd%>, hitungSebelumnya<%=rnd%>);
                
                if (arah === 'next' && nilaiMulai<%=rnd%> === 0 && hitungSebelumnya<%=rnd%> > 0) {
                    await muatLinimasa<%=rnd%>('prev', false); 
                    return; 
                }

                if (isFilterBaru || nilaiMulai<%=rnd%> === 0) {
                    var divContainer = document.getElementById("timeline-container-<%=rnd%>");
                    if(divContainer) divContainer.innerHTML = '<div class="text-center text-muted py-5 fade-in-<%=rnd%>"><i class="fas fa-search-minus fa-3x mb-3 text-black-50"></i><h5 class="font-weight-bold text-secondary mt-3"><%=Common.getBahasaConfig("Data Tidak Ditemukan")%></h5><p class="text-muted"><%=Common.getBahasaConfig("Tidak terdapat riwayat tugas kelompok yang sesuai dengan kriteria filter pencarian Anda.")%></p></div>';
                }
                return; 
            }

            var arrayIdPermintaan<%=rnd%> = [];
            dataJson<%=rnd%>.forEach(function(item) {
                if (!angkaUnik<%=rnd%>.has(item)) { arrayIdPermintaan<%=rnd%>.push(item); angkaUnik<%=rnd%>.add(item); }
            });
            
            if (arrayIdPermintaan<%=rnd%>.length === 0) {
                perbaruiTampilanPaginasi<%=rnd%>(hitungSelanjutnya<%=rnd%>, hitungSebelumnya<%=rnd%>);
                return;
            }

            var fetchPromises<%=rnd%> = arrayIdPermintaan<%=rnd%>.map(function(itemReq) {
                var urlHtml = rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning&s=load_tugas_kelompok&item=" + encodeURIComponent(itemReq);
                return fetch(urlHtml).then(function(res) {
                    if (!res.ok) throw new Error('<%=Common.getBahasaConfigJS("Sistem gagal merender antarmuka tugas kelompok.")%>');
                    return res.text();
                });
            });
            
            var htmlArray<%=rnd%> = await Promise.all(fetchPromises<%=rnd%>);

            if (arah === 'next') { htmlArray<%=rnd%>.reverse(); }

            var hasilHtml<%=rnd%> = htmlArray<%=rnd%>.join('');
            var elemenDibungkus<%=rnd%> = '<div class="fade-in-<%=rnd%>">' + hasilHtml<%=rnd%> + '</div>';

            if (arah === 'next') {
                var divNext = document.getElementById("timeline-content-next-<%=rnd%>");
                if (divNext) {
                    divNext.insertAdjacentHTML('afterbegin', elemenDibungkus<%=rnd%>);
                    startNext<%=rnd%> += dataJson<%=rnd%>.length; 
                }
            } else {
                var divPrev = document.getElementById("timeline-content-prev-<%=rnd%>");
                if (divPrev) {
                    divPrev.insertAdjacentHTML('beforeend', elemenDibungkus<%=rnd%>);
                    startBack<%=rnd%> += dataJson<%=rnd%>.length;
                }
            }

            perbaruiTampilanPaginasi<%=rnd%>(hitungSelanjutnya<%=rnd%>, hitungSebelumnya<%=rnd%>);

        } catch (error) {
            console.error("<%=Common.getBahasaConfig("Kendala pada proses pemuatan linimasa: ")%>", error);
            if (typeof tampilkanToast === "function") {
                tampilkanToast(error.message || "<%=Common.getBahasaConfig("Terjadi gangguan saat mengambil data riwayat tugas kelompok.")%>", 'bg-danger');
            } else { alert(error.message); }
        } finally {
            if (arah !== 'init') setStatusTombol<%=rnd%>(arah, false);
            
            var elemenLoadingAwal<%=rnd%> = document.getElementById("loading-init-<%=rnd%>");
            if (elemenLoadingAwal<%=rnd%>) elemenLoadingAwal<%=rnd%>.remove();
        }
    }

    function perbaruiTampilanPaginasi<%=rnd%>(hitungSelanjutnya, hitungSebelumnya) {
        var tombolSelanjutnya<%=rnd%> = document.getElementById("btn-load-next-<%=rnd%>"); 
        var teksSelanjutnya<%=rnd%> = document.getElementById("count-next-<%=rnd%>");
        var tombolSebelumnya<%=rnd%> = document.getElementById("btn-load-back-<%=rnd%>"); 
        var teksSebelumnya<%=rnd%> = document.getElementById("count-back-<%=rnd%>");

        if (tombolSelanjutnya<%=rnd%> && teksSelanjutnya<%=rnd%>) {
            if (hitungSelanjutnya > startNext<%=rnd%>) { 
                tombolSelanjutnya<%=rnd%>.style.display = 'inline-block';
                teksSelanjutnya<%=rnd%>.innerText = hitungSelanjutnya - startNext<%=rnd%>;
            } else { tombolSelanjutnya<%=rnd%>.style.display = 'none'; }
        }

        if (tombolSebelumnya<%=rnd%> && teksSebelumnya<%=rnd%>) {
            if (hitungSebelumnya > startBack<%=rnd%>) { 
                tombolSebelumnya<%=rnd%>.style.display = 'inline-block';
                teksSebelumnya<%=rnd%>.innerText = hitungSebelumnya - startBack<%=rnd%>;
            } else { tombolSebelumnya<%=rnd%>.style.display = 'none'; }
        }
    }

    window.terapkanFilter<%=rnd%> = function() { muatLinimasa<%=rnd%>('next', true); };
    
    window.resetFilter<%=rnd%> = async function() {
        var formEl = document.getElementById("form-filter-<%=rnd%>");
        if (formEl) formEl.reset();
        
        ubahFakultas<%=rnd%>(true); 
        if (typeof ubahYayasan<%=rnd%> === "function") ubahYayasan<%=rnd%>(true);
        
        document.getElementById('filter-rentang-combo-<%=rnd%>').value = 'custom';
        kalkulasiRentangWaktu<%=rnd%>();
        
        await muatLinimasa<%=rnd%>('next', true);
    };

    var elemenBtnLanjut<%=rnd%> = document.getElementById("btn-load-next-<%=rnd%>"); 
    var elemenBtnKembali<%=rnd%> = document.getElementById("btn-load-back-<%=rnd%>"); 
    if (elemenBtnLanjut<%=rnd%>) elemenBtnLanjut<%=rnd%>.addEventListener('click', function() { muatLinimasa<%=rnd%>('next', false); });
    if (elemenBtnKembali<%=rnd%>) elemenBtnKembali<%=rnd%>.addEventListener('click', function() { muatLinimasa<%=rnd%>('prev', false); });

    // Load Pertama Kali (Otomatis Reset Container & Tampilkan "Tidak Ditemukan" Jika Kosong)
    muatLinimasa<%=rnd%>('next', true);

})();
</script>