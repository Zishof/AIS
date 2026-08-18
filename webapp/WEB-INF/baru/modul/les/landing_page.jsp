<%@page import="java.util.Date"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.sekolah.Guru"%>
<%@page import="ais.database.model.sekolah.KelasLesSiswaPunyaSiswa"%>

<%
    // Mengambil Informasi Perguruan Tinggi
    PerguruanTinggi pt = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request);
    String judulNamaPerguruanTinggi = pt != null && pt.getNama() != null ? pt.getNama() : "";
    String Telepon = pt != null && pt.getTelepon() != null ? pt.getTelepon() : "";
    String Motto = pt != null && pt.getMotto() != null ? pt.getMotto() : "";
    String Alamat1 = pt != null && pt.getAlamat1() != null ? pt.getAlamat1() : "";
    String Email = pt != null && pt.getEmail() != null ? pt.getEmail() : "";
    String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");

    String rnd = request.getParameter("rnd");
    if(rnd == null){
        rnd = Common.getGeneratedBarCode(7);
    }

    // Mengambil Data Pengguna Saat Ini
    Tbmuser tbmuser = Common.getCurrentUser(request);
    Siswa peserta = tbmuser == null ? null : tbmuser.getSiswa();
    Guru pengajar = tbmuser == null ? null : tbmuser.getGuru();
    
    boolean isSiswaLogin = (peserta != null);
    boolean isGuruLogin = (pengajar != null);
    Long idPeserta = isSiswaLogin ? peserta.getId() : null;
%>

<style>
    /* Latar Belakang & Bagian Utama (Hero) */
    .page-bg-<%=rnd%> { 
        position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; 
        background: url('<%=Common.ROOT%>/img/pmb_bg.jpg') center/cover no-repeat; 
        opacity: 0.05; z-index: -999; pointer-events: none; 
    }
    .hero-section-<%=rnd%> { 
        background-color: #0d6efd; 
        background: linear-gradient(rgba(13, 110, 253, 0.85), rgba(10, 88, 202, 0.95)), url('<%=Common.ROOT%>/img/pmb_bg.jpg') center/cover no-repeat; 
        color: white; border-radius: 0 0 2rem 2rem; box-shadow: 0 8px 25px rgba(0,0,0,0.15); 
        position: relative; z-index: 2; padding: 3.5rem 2rem; margin-bottom: 2.5rem; text-align: center; 
    }
    
    /* Logo & Teks Efek */
    .logo-shadow-<%=rnd%> { filter: drop-shadow(0px 6px 8px rgba(0,0,0,0.5)); }
    .text-shadow-<%=rnd%> { text-shadow: 2px 2px 6px rgba(0,0,0,0.4); }
    .tracking-wide-<%=rnd%> { letter-spacing: 1.5px; }

    /* Gaya Kartu untuk Sistem Manajemen Pembelajaran (LMS) */
    .course-card-<%=rnd%> { 
        border: none; border-radius: 15px; transition: transform 0.3s ease, box-shadow 0.3s ease; 
        overflow: hidden; background: #ffffff; box-shadow: 0 4px 10px rgba(0,0,0,0.05); 
        height: 100%; display: flex; flex-direction: column; 
    }
    .course-card-<%=rnd%>:hover { transform: translateY(-5px); box-shadow: 0 10px 20px rgba(0,0,0,0.1); }
    .course-icon-wrapper-<%=rnd%> { 
        height: 140px; background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%); 
        display: flex; align-items: center; justify-content: center; font-size: 3.5rem; color: #0d6efd; 
    }
    .course-body-<%=rnd%> { padding: 1.5rem; flex-grow: 1; display: flex; flex-direction: column; }
    .course-title-<%=rnd%> { 
        font-size: 1.25rem; font-weight: 700; color: #2c3e50; margin-bottom: 0.5rem; 
        display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; 
    }
    
    /* Gaya Navigasi Tab */
    .nav-tabs-custom-<%=rnd%> { border-bottom: 2px solid #e9ecef; margin-bottom: 2rem; }
    .nav-tabs-custom-<%=rnd%> .nav-link { border: none; color: #6c757d; font-weight: 600; padding: 1rem 1.5rem; transition: all 0.3s ease; }
    .nav-tabs-custom-<%=rnd%> .nav-link.active { color: #0d6efd; background: transparent; border-bottom: 3px solid #0d6efd; }
    .nav-tabs-custom-<%=rnd%> .nav-link:hover:not(.active) { border-bottom: 3px solid #dee2e6; }

    /* Footer Styles */
    .footer-pmb-<%=rnd%> { background: linear-gradient(135deg, #0a2540 0%, #173b5e 100%); color: white; border-top: 5px solid #0d6efd; position: relative; z-index: 10; margin-top: 5rem; }
    .footer-pmb-<%=rnd%> h5 { letter-spacing: 1px; }
    .footer-pmb-<%=rnd%> .social-icon { width: 35px; height: 35px; display: inline-flex; align-items: center; justify-content: center; border-radius: 50%; background: rgba(255,255,255,0.1); color: white; transition: 0.3s; margin-right: 0.5rem; text-decoration: none; }
    .footer-pmb-<%=rnd%> .social-icon:hover { background: #0d6efd; transform: translateY(-3px); }
</style>

<div class="page-bg-<%=rnd%>"></div>

<div class="container-fluid p-0">
    <div class="hero-section-<%=rnd%>">
        <div class="container pb-2 pt-2">
            <% if (logo_PerguruanTinggi != null && !logo_PerguruanTinggi.trim().isEmpty()) { %>
                <img src="<%= logo_PerguruanTinggi %>" alt="Logo Institusi" 
                     class="img-fluid mb-3 logo-shadow-<%=rnd%> animate__animated animate__fadeInDown" 
                     style="max-height: 130px; object-fit: contain;">
            <% } %>

            <h1 class="display-5 fw-bold mb-3 text-white text-shadow-<%=rnd%> tracking-wide-<%=rnd%> animate__animated animate__fadeInDown">
                <i class="fas fa-chalkboard-teacher me-3 text-warning"></i><%=Common.getBahasaConfig("Portal Kelas Les & Privat")%>
            </h1>
            <h4 class="fw-light mb-4 text-white text-shadow-<%=rnd%> animate__animated animate__fadeInDown"><%=judulNamaPerguruanTinggi%></h4>
            
            <% if (Motto != null && !Motto.trim().isEmpty()) { %>
                <p class="lead fw-normal mb-2 text-white opacity-75 fst-italic mx-auto animate__animated animate__fadeInUp" style="max-width: 800px;">
                    "<%= Motto %>"
                </p>
            <% } else { %>
                <p class="lead fw-normal mb-2 text-white opacity-75 mx-auto animate__animated animate__fadeInUp" style="max-width: 800px;">
                    <%=Common.getBahasaConfig("Tingkatkan potensi akademik Anda dengan bimbingan terbaik dari instruktur kami.")%>
                </p>
            <% } %>
        </div>
    </div>
</div>

<div class="container mb-5" style="min-height: 45vh;">
    <ul class="nav nav-tabs nav-tabs-custom-<%=rnd%> justify-content-center" id="lesTab<%=rnd%>" role="tablist">
        <li class="nav-item" role="presentation">
            <button class="nav-link active" id="katalog-tab-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#katalog-<%=rnd%>" type="button" role="tab">
                <i class="fas fa-list-ul me-2"></i><%=Common.getBahasaConfig("Katalog Kelas Les")%>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link" id="kelasku-tab-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#kelasku-<%=rnd%>" type="button" role="tab">
                <i class="fas fa-book-reader me-2"></i><%=Common.getBahasaConfig("Kelas Les Saya")%>
            </button>
        </li>
    </ul>

    <div class="tab-content" id="lesTabContent<%=rnd%>">
        
        <div class="tab-pane fade show active" id="katalog-<%=rnd%>" role="tabpanel">
            <div class="row mb-4 justify-content-between align-items-center">
                <div class="col-md-5 mb-2 mb-md-0">
                    <div class="input-group shadow-sm border-0 rounded-pill overflow-hidden">
                        <span class="input-group-text bg-white border-0"><i class="fas fa-search text-muted"></i></span>
                        <input type="text" class="form-control border-0 shadow-none" id="searchLes<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari nama kelas atau mata pelajaran...")%>" onkeyup="filterKatalog<%=rnd%>()">
                    </div>
                </div>
            </div>

            <div class="row g-4" id="katalogGrid<%=rnd%>">
                <div class="col-12 text-center py-5">
                    <div class="spinner-border text-primary" role="status"></div>
                    <p class="mt-2 text-muted"><%=Common.getBahasaConfig("Memuat katalog kelas les...")%></p>
                </div>
            </div>
        </div>

        <div class="tab-pane fade" id="kelasku-<%=rnd%>" role="tabpanel">
            <% if(!isSiswaLogin) { %>
                <div class="text-center py-5 bg-white rounded-4 shadow-sm border">
                    <div class="display-1 text-muted mb-3"><i class="fas fa-user-lock"></i></div>
                    <h3 class="text-dark"><%=Common.getBahasaConfig("Akses Terbatas")%></h3>
                    <p class="text-muted"><%=Common.getBahasaConfig("Silakan masuk menggunakan akun Siswa terlebih dahulu untuk melihat daftar kelas les Anda.")%></p>
                </div>
            <% } else { %>
                <div class="row g-4" id="kelaskuGrid<%=rnd%>">
                    <div class="col-12 text-center py-5">
                        <div class="spinner-border text-success" role="status"></div>
                        <p class="mt-2 text-muted"><%=Common.getBahasaConfig("Memuat data kelas les Anda...")%></p>
                    </div>
                </div>
            <% } %>
        </div>
    </div>
</div>

<footer class="footer-pmb-<%=rnd%> pt-5 pb-3">
    <div class="container text-center text-md-start">
        <div class="row text-center text-md-start">
            
            <div class="col-md-5 col-lg-5 col-xl-5 mx-auto mb-4">
                <h5 class="text-uppercase fw-bold text-warning mb-3">
                    <i class="fas fa-university me-2"></i><%= judulNamaPerguruanTinggi != null ? judulNamaPerguruanTinggi : "" %>
                </h5>
                <p class="text-white-50 small pe-md-4">
                    <%= Common.getBahasaConfig("Platform layanan pembelajaran dan bimbingan belajar khusus (private les). Berkomitmen untuk mencetak generasi penerus bangsa yang unggul, inovatif, dan berintegritas tinggi.") %>
                </p>
                <% if (Motto != null && !Motto.trim().isEmpty()) { %>
                    <p class="text-info fst-italic mt-2 mb-0">"<%= Motto %>"</p>
                <% } %>
            </div>

            <div class="col-md-3 col-lg-3 col-xl-3 mx-auto mb-4">
                <h5 class="text-uppercase fw-bold text-warning mb-3"><%= Common.getBahasaConfig("Bantuan & Informasi") %></h5>
                <p class="mb-2"><a href="#" class="text-white-50 text-decoration-none small hover-text-white"><i class="fas fa-angle-right me-2"></i><%= Common.getBahasaConfig("Cara Berlangganan") %></a></p>
                <p class="mb-2"><a href="#" class="text-white-50 text-decoration-none small hover-text-white"><i class="fas fa-angle-right me-2"></i><%= Common.getBahasaConfig("Tanya Jawab (FAQ)") %></a></p>
                <p class="mb-2"><a href="#" class="text-white-50 text-decoration-none small hover-text-white"><i class="fas fa-angle-right me-2"></i><%= Common.getBahasaConfig("Syarat dan Ketentuan") %></a></p>
            </div>

            <div class="col-md-4 col-lg-3 col-xl-3 mx-auto mb-4">
                <h5 class="text-uppercase fw-bold text-warning mb-3"><%= Common.getBahasaConfig("Hubungi Kami") %></h5>
                <p class="text-white-50 small mb-2"><i class="fas fa-map-marker-alt me-2 text-white"></i> <%= Alamat1 != null ? Alamat1 : "-" %></p>
                <p class="text-white-50 small mb-2"><i class="fas fa-envelope me-2 text-white"></i> <%= Email != null ? Email : "-" %></p>
                <p class="text-white-50 small mb-4"><i class="fas fa-phone-alt me-2 text-white"></i> <%= Telepon != null ? Telepon : "-" %></p>
                
                <div>
                    <a href="#" class="social-icon"><i class="fab fa-facebook-f"></i></a>
                    <a href="#" class="social-icon"><i class="fab fa-twitter"></i></a>
                    <a href="#" class="social-icon"><i class="fab fa-instagram"></i></a>
                    <a href="#" class="social-icon"><i class="fab fa-youtube"></i></a>
                </div>
            </div>
        </div>

        <hr class="mb-4 mt-2" style="border-color: rgba(255,255,255,0.2);">
        
        <div class="row align-items-center">
            <div class="col-md-12 col-lg-12 text-center">
                <p class="text-white-50 small mb-0">
                    &copy; <%= java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) %> <strong class="text-warning"><%= judulNamaPerguruanTinggi != null ? judulNamaPerguruanTinggi : "" %></strong>. <%= Common.getBahasaConfig("Hak Cipta Dilindungi Undang-Undang.") %>
                </p>
            </div>
        </div>
    </div>
</footer>

<script>
    // Konfigurasi Global
    const isSiswaLogin<%=rnd%> = <%=isSiswaLogin%>;
    const idSiswa<%=rnd%> = <%= idPeserta != null ? idPeserta : "null" %>;
    let dataKatalogLes<%=rnd%> = [];
    let modalInstance<%=rnd%> = null;

    // Fungsi Injeksi Modal via JavaScript
    const injectModal<%=rnd%> = () => {
        // Cek agar tidak duplikat saat reload komponen
        if (document.getElementById('modalDetailLes<%=rnd%>')) return;

        const modalHtml = `
        <div class="modal fade" id="modalDetailLes<%=rnd%>" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered modal-lg">
                <div class="modal-content border-0 shadow-lg rounded-4">
                    <div class="modal-header bg-light border-bottom-0 pb-0 rounded-top-4 pt-4 px-4">
                        <h4 class="modal-title fw-bold text-dark" id="modalDetailLesLabel<%=rnd%>"></h4>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<%=Common.getBahasaConfig("Tutup")%>"></button>
                    </div>
                    <div class="modal-body pt-3 px-4 pb-4">
                        <div class="d-flex align-items-center mb-3">
                            <span class="badge bg-primary me-2 px-3 py-2" id="modalMataPelajaran<%=rnd%>"></span>
                            <span class="text-muted small"><i class="fas fa-user-tie me-1"></i> <span id="modalPengajar<%=rnd%>"></span></span>
                        </div>
                        
                        <h6 class="fw-bold mt-4"><%=Common.getBahasaConfig("Deskripsi Kelas:")%></h6>
                        <p class="text-muted" id="modalKeterangan<%=rnd%>"></p>
                        
                        <h6 class="fw-bold mt-4"><%=Common.getBahasaConfig("Jadwal Pelajaran:")%></h6>
                        <div class="table-responsive">
                            <table class="table table-bordered table-sm mt-2">
                                <thead class="table-light text-center small">
                                    <tr>
                                        <th><%=Common.getBahasaConfig("Hari")%></th>
                                        <th><%=Common.getBahasaConfig("Waktu Mulai")%></th>
                                        <th><%=Common.getBahasaConfig("Waktu Selesai")%></th>
                                    </tr>
                                </thead>
                                <tbody id="modalJadwalBody<%=rnd%>" class="text-center text-muted small">
                                    </tbody>
                            </table>
                        </div>

                        <div class="bg-light p-4 rounded-4 mt-4 d-flex justify-content-between align-items-center border">
                            <div>
                                <span class="d-block text-muted small"><%=Common.getBahasaConfig("Tingkat Kelas")%></span>
                                <h4 class="mb-0 text-dark fw-bold" id="modalTingkat<%=rnd%>"></h4>
                            </div>
                            <input type="hidden" id="hiddenIdLes<%=rnd%>">
                            <button type="button" class="btn btn-primary btn-lg px-4 rounded-pill shadow-sm" id="btnBerlangganan<%=rnd%>" onclick="prosesLangganan<%=rnd%>()">
                                <i class="fas fa-check-circle me-2"></i><%=Common.getBahasaConfig("Berlangganan Sekarang")%>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>`;
        
        document.body.insertAdjacentHTML('beforeend', modalHtml);
    };

    document.addEventListener("DOMContentLoaded", () => {
        // Injeksi Modal HTML ke Document Body
        injectModal<%=rnd%>();
        
        // Inisialisasi Modal
        modalInstance<%=rnd%> = new bootstrap.Modal(document.getElementById('modalDetailLes<%=rnd%>'));
        
        // Memuat Data
        loadKatalogLes<%=rnd%>();
        if(isSiswaLogin<%=rnd%>) {
            loadKelasLesSaya<%=rnd%>();
        }
    });

    // ==========================================
    // 1. UTILITAS PENGAMBILAN DATA (SQL NATIVE)
    // ==========================================
    const fetchDataSQL<%=rnd%> = async (sqlString) => {
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sql: sqlString, action: "sql", tanpaLogin: "true" })
            });
            const result = await res.json();
            return result.data || [];
        } catch (e) { 
            console.error("Kesalahan Fetch SQL:", e);
            return []; 
        }
    };

    // ==========================================
    // 2. MEMUAT KATALOG KELAS LES
    // ==========================================
    const loadKatalogLes<%=rnd%> = async () => {
        const sql = "SELECT k.id, k.nama AS nama_kelas, k.keterangan, k.tingkat, " +
                    "m.nama AS nama_pelajaran, g.nama_guru " +
                    "FROM sekolah.kelas_les k " +
                    "LEFT JOIN sekolah.matapelajaran m ON k.matapelajaran_id = m.id " +
                    "LEFT JOIN sekolah.guru g ON k.guru_pembina = g.id " +
                    "WHERE k.aktif = true ORDER BY k.nama ASC";
        dataKatalogLes<%=rnd%> = await fetchDataSQL<%=rnd%>(sql);
        renderKatalogLes<%=rnd%>(dataKatalogLes<%=rnd%>);
    };

    const filterKatalog<%=rnd%> = () => {
        const search = document.getElementById("searchLes<%=rnd%>").value.toLowerCase();
        const filtered = dataKatalogLes<%=rnd%>.filter(item => {
            const matchNama = item.nama_kelas && item.nama_kelas.toLowerCase().includes(search);
            const matchMapel = item.nama_pelajaran && item.nama_pelajaran.toLowerCase().includes(search);
            return matchNama || matchMapel;
        });
        renderKatalogLes<%=rnd%>(filtered);
    };

    const renderKatalogLes<%=rnd%> = (data) => {
        const grid = document.getElementById("katalogGrid<%=rnd%>");
        grid.innerHTML = "";

        if(data.length === 0) {
            grid.innerHTML = `<div class="col-12 text-center text-muted py-5">
                                <i class="fas fa-folder-open fs-1 mb-3"></i>
                                <p><%=Common.getBahasaConfig("Belum ada kelas les yang tersedia.")%></p>
                              </div>`;
            return;
        }

        data.forEach(item => {
            const mapel = item.nama_pelajaran || '<%=Common.getBahasaConfigJS("Umum")%>';
            const pengajar = item.nama_guru || '<%=Common.getBahasaConfigJS("Belum Ditentukan")%>';
            const keterangan = item.keterangan || '<%=Common.getBahasaConfigJS("Tidak ada deskripsi")%>';
            const tingkat = item.tingkat ? '<%=Common.getBahasaConfigJS("Tingkat ")%>' + item.tingkat : '-';
            
            const card = `
                <div class="col-md-6 col-lg-4">
                    <div class="card course-card-<%=rnd%> border border-light">
                        <div class="course-icon-wrapper-<%=rnd%>">
                            <i class="fas fa-book-open"></i>
                        </div>
                        <div class="course-body-<%=rnd%>">
                            <span class="text-primary small fw-bold mb-2"><i class="fas fa-tag me-1"></i> ` + mapel + `</span>
                            <h5 class="course-title-<%=rnd%>">` + item.nama_kelas + `</h5>
                            <p class="text-muted small mb-3 text-truncate"><i class="fas fa-user-tie me-1"></i> ` + pengajar + `</p>
                            <div class="mt-auto pt-3 border-top d-flex justify-content-between align-items-center">
                                <span class="badge bg-light text-dark border"><i class="fas fa-layer-group me-1"></i> ` + tingkat + `</span>
                                <button class="btn btn-outline-primary btn-sm rounded-pill px-3" onclick="bukaDetailLes<%=rnd%>(` + item.id + `)">
                                    <%=Common.getBahasaConfig("Lihat Detail")%>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>`;
            grid.insertAdjacentHTML('beforeend', card);
        });
    };

    // ==========================================
    // 3. MEMUAT KELAS LES SAYA
    // ==========================================
    const loadKelasLesSaya<%=rnd%> = async () => {
        if(!isSiswaLogin<%=rnd%>) return;
        
        const sql = "SELECT p.id, k.nama AS nama_kelas, m.nama AS nama_pelajaran, " +
                    "g.nama_guru, p.aktif " +
                    "FROM sekolah.kelas_les_punya_siswa p " +
                    "JOIN sekolah.kelas_les k ON p.kelas_id = k.id " +
                    "LEFT JOIN sekolah.matapelajaran m ON k.matapelajaran_id = m.id " +
                    "LEFT JOIN sekolah.guru g ON k.guru_pembina = g.id " +
                    "WHERE p.siswa_id = " + idSiswa<%=rnd%> + " ORDER BY p.id DESC";
        
        const data = await fetchDataSQL<%=rnd%>(sql);
        const grid = document.getElementById("kelaskuGrid<%=rnd%>");
        grid.innerHTML = "";

        if(data.length === 0) {
            grid.innerHTML = `
                <div class="col-12 text-center text-muted py-5">
                    <i class="fas fa-box-open display-4 mb-3"></i>
                    <h4><%=Common.getBahasaConfig("Belum Ada Kelas")%></h4>
                    <p><%=Common.getBahasaConfig("Anda belum berlangganan kelas les apapun. Silakan jelajahi katalog kami.")%></p>
                </div>`;
            return;
        }

        data.forEach(item => {
            const statusBadge = item.aktif 
                ? '<span class="badge bg-success"><i class="fas fa-check-circle me-1"></i><%=Common.getBahasaConfig("Aktif")%></span>' 
                : '<span class="badge bg-danger"><i class="fas fa-times-circle me-1"></i><%=Common.getBahasaConfig("Tidak Aktif")%></span>';
            const mapel = item.nama_pelajaran || '<%=Common.getBahasaConfigJS("Umum")%>';
            const namaGuru = item.nama_guru || '-';
            const btnState = !item.aktif ? 'disabled' : '';
            
            const card = `
                <div class="col-md-6 col-lg-4">
                    <div class="card course-card-<%=rnd%> border border-light shadow-sm">
                        <div class="course-body-<%=rnd%>">
                            <div class="d-flex justify-content-between mb-3">
                                <span class="text-secondary small fw-bold"><i class="fas fa-bookmark me-1 text-primary"></i> ` + mapel + `</span>
                                ` + statusBadge + `
                            </div>
                            <h5 class="course-title-<%=rnd%> mb-3">` + item.nama_kelas + `</h5>
                            <p class="text-muted small mb-0"><i class="fas fa-user-tie me-1"></i> ` + namaGuru + `</p>
                            <div class="mt-auto pt-3 border-top d-grid">
                                <button class="btn btn-primary btn-sm rounded-pill" ` + btnState + `>
                                    <i class="fas fa-chalkboard-teacher me-2"></i><%=Common.getBahasaConfig("Masuk Kelas")%>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>`;
            grid.insertAdjacentHTML('beforeend', card);
        });
    };

 // ==========================================
    // 4. MEMBUKA DETAIL & JADWAL (MODAL)
    // ==========================================
    const bukaDetailLes<%=rnd%> = async (idKelas) => {
        const item = dataKatalogLes<%=rnd%>.find(x => String(x.id) === String(idKelas));
        
        if(!item) {
            tampilkanToast('<%=Common.getBahasaConfigJS("Data kelas tidak ditemukan.")%>', 'bg-danger');
            return;
        }

        document.getElementById("modalDetailLesLabel<%=rnd%>").innerText = item.nama_kelas;
        document.getElementById("modalMataPelajaran<%=rnd%>").innerHTML = '<i class="fas fa-book me-1"></i> ' + (item.nama_pelajaran || '<%=Common.getBahasaConfigJS("Umum")%>');
        document.getElementById("modalPengajar<%=rnd%>").innerText = item.nama_guru || '<%=Common.getBahasaConfigJS("Belum Ditentukan")%>';
        document.getElementById("modalKeterangan<%=rnd%>").innerText = item.keterangan || '<%=Common.getBahasaConfigJS("Tidak ada deskripsi tersedia.")%>';
        document.getElementById("modalTingkat<%=rnd%>").innerText = item.tingkat ? '<%=Common.getBahasaConfigJS("Kelas ")%>' + item.tingkat : '-';
        document.getElementById("hiddenIdLes<%=rnd%>").value = item.id;

        // Memuat Jadwal Pelajaran (Menggunakan UNION untuk 12 slot jam_pelajaran)
        const tbodyJadwal = document.getElementById("modalJadwalBody<%=rnd%>");
	    tbodyJadwal.innerHTML = '<tr><td colspan="3"><span class="spinner-border spinner-border-sm text-primary"></span> <%=Common.getBahasaConfig("Memuat jadwal...")%></td></tr>';
	    
	    // Perbaikan: Nama kolom adalah jam_pelajaran_id (untuk ke-1) 
	    // dan jam_pelajaran2_id s/d jam_pelajaran12_id (untuk ke-2 sampai 12)
	    const sqlJadwal = "SELECT * FROM (" +
	        "SELECT j.hari, jp.mulai, jp.sampais as sampai, 1 as urut FROM sekolah.jadwal_pelajaran j JOIN sekolah.jam_pelajaran jp ON j.jam_pelajaran_id = jp.id WHERE j.kelas_les_siswa = " + idKelas +
	        " UNION ALL SELECT j.hari, jp.mulai, jp.sampais, 2 FROM sekolah.jadwal_pelajaran j JOIN sekolah.jam_pelajaran jp ON j.jam_pelajaran2_id = jp.id WHERE j.kelas_les_siswa = " + idKelas +
	        " UNION ALL SELECT j.hari, jp.mulai, jp.sampais, 3 FROM sekolah.jadwal_pelajaran j JOIN sekolah.jam_pelajaran jp ON j.jam_pelajaran3_id = jp.id WHERE j.kelas_les_siswa = " + idKelas +
	        " UNION ALL SELECT j.hari, jp.mulai, jp.sampais, 4 FROM sekolah.jadwal_pelajaran j JOIN sekolah.jam_pelajaran jp ON j.jam_pelajaran4_id = jp.id WHERE j.kelas_les_siswa = " + idKelas +
	        " UNION ALL SELECT j.hari, jp.mulai, jp.sampais, 5 FROM sekolah.jadwal_pelajaran j JOIN sekolah.jam_pelajaran jp ON j.jam_pelajaran5_id = jp.id WHERE j.kelas_les_siswa = " + idKelas +
	        " UNION ALL SELECT j.hari, jp.mulai, jp.sampais, 6 FROM sekolah.jadwal_pelajaran j JOIN sekolah.jam_pelajaran jp ON j.jam_pelajaran6_id = jp.id WHERE j.kelas_les_siswa = " + idKelas +
	        " UNION ALL SELECT j.hari, jp.mulai, jp.sampais, 7 FROM sekolah.jadwal_pelajaran j JOIN sekolah.jam_pelajaran jp ON j.jam_pelajaran7_id = jp.id WHERE j.kelas_les_siswa = " + idKelas +
	        " UNION ALL SELECT j.hari, jp.mulai, jp.sampais, 8 FROM sekolah.jadwal_pelajaran j JOIN sekolah.jam_pelajaran jp ON j.jam_pelajaran8_id = jp.id WHERE j.kelas_les_siswa = " + idKelas +
	        " UNION ALL SELECT j.hari, jp.mulai, jp.sampais, 9 FROM sekolah.jadwal_pelajaran j JOIN sekolah.jam_pelajaran jp ON j.jam_pelajaran9_id = jp.id WHERE j.kelas_les_siswa = " + idKelas +
	        " UNION ALL SELECT j.hari, jp.mulai, jp.sampais, 10 FROM sekolah.jadwal_pelajaran j JOIN sekolah.jam_pelajaran jp ON j.jam_pelajaran10_id = jp.id WHERE j.kelas_les_siswa = " + idKelas +
	        " UNION ALL SELECT j.hari, jp.mulai, jp.sampais, 11 FROM sekolah.jadwal_pelajaran j JOIN sekolah.jam_pelajaran jp ON j.jam_pelajaran11_id = jp.id WHERE j.kelas_les_siswa = " + idKelas +
	        " UNION ALL SELECT j.hari, jp.mulai, jp.sampais, 12 FROM sekolah.jadwal_pelajaran j JOIN sekolah.jam_pelajaran jp ON j.jam_pelajaran12_id = jp.id WHERE j.kelas_les_siswa = " + idKelas +
	        ") AS gabungan WHERE mulai IS NOT NULL ORDER BY hari ASC, mulai ASC";
	                      
	    const dataJadwal = await fetchDataSQL<%=rnd%>(sqlJadwal);
        
        tbodyJadwal.innerHTML = "";
        if(dataJadwal.length === 0) {
            tbodyJadwal.innerHTML = `<tr><td colspan="3" class="text-muted fst-italic"><%=Common.getBahasaConfig("Jadwal belum tersedia.")%></td></tr>`;
        } else {
            dataJadwal.forEach(jadwal => {
                const hr = jadwal.hari || '-';
                const ml = jadwal.mulai ? jadwal.mulai.substring(0,5) : '-';
                const sm = jadwal.sampai ? jadwal.sampai.substring(0,5) : '-'; // Tetap panggil .sampai karena di SELECT pertama di-alias 'as sampai'
                
                const tr = `<tr>
                                <td class="fw-bold">` + hr + `</td>
                                <td>` + ml + `</td>
                                <td>` + sm + `</td>
                            </tr>`;
                tbodyJadwal.insertAdjacentHTML('beforeend', tr);
            });
        }

        modalInstance<%=rnd%>.show();
    };

    // ==========================================
    // 5. TRANSAKSI BERLANGGANAN (SIMPAN)
    // ==========================================
    const prosesLangganan<%=rnd%> = async () => {
        if(!isSiswaLogin<%=rnd%>) {
            tampilkanToast('<%=Common.getBahasaConfigJS("Anda harus masuk menggunakan akun Siswa terlebih dahulu untuk berlangganan!")%>', 'bg-danger');
            return;
        }

        const idKelas = document.getElementById("hiddenIdLes<%=rnd%>").value;
        const btn = document.getElementById("btnBerlangganan<%=rnd%>");
        
        const dataObj = {
            kelasLesSiswa: parseInt(idKelas),
            siswa: parseInt(idSiswa<%=rnd%>),
            aktif: true
        };

        const payload = { 
            action: "simpanDataRinci", 
            log: "true",
            tanpaLogin: "true",
            class: "<%=KelasLesSiswaPunyaSiswa.class.getName()%>", 
            data: dataObj
        };

        const oriBtn = btn.innerHTML;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Memproses...")%>';
        btn.disabled = true;

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const dataResponse = await response.json();
            
            if (dataResponse.status === '00' || dataResponse.status === 'success' || dataResponse.id) {
                tampilkanToast('<%=Common.getBahasaConfigJS("Berhasil berlangganan kelas les! Silakan cek di menu Kelas Les Saya.")%>', 'bg-success');
                modalInstance<%=rnd%>.hide();
                loadKelasLesSaya<%=rnd%>();
                
                // Pindah Otomatis ke tab Kelas Saya
                const tabTrigger = new bootstrap.Tab(document.querySelector('#kelasku-tab-<%=rnd%>'));
                tabTrigger.show();
            } else {
                tampilkanToast(dataResponse.description || '<%=Common.getBahasaConfigJS("Gagal memproses langganan. Silakan coba lagi.")%>', 'bg-danger');
            }
        } catch (e) {
            console.error(e);
            tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi saat memproses langganan.")%>', 'bg-danger');
        } finally {
            btn.innerHTML = oriBtn;
            btn.disabled = false;
        }
    };
</script>