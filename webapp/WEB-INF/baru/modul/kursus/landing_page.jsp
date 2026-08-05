<%@page import="java.util.Date"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.database.model.kursus.PesertaKursus"%>
<%@page import="ais.database.model.kursus.PesertaPunyaProdukKursus"%>

<%
    // Mengambil Informasi Perguruan Tinggi
    PerguruanTinggi pt = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request);
    String judulNamaPerguruanTinggi = pt != null && pt.getNama() != null ? pt.getNama() : "";
    String Motto = pt != null && pt.getMotto() != null ? pt.getMotto() : "";
    String Alamat1 = pt != null && pt.getAlamat1() != null ? pt.getAlamat1() : "";
    String Email = pt != null && pt.getEmail() != null ? pt.getEmail() : "";
    String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");

    String rnd = request.getParameter("rnd");
    if (rnd == null) {
        rnd = Common.getGeneratedBarCode(7);
    }

    // Mengambil Data Pengguna Saat Ini
    Tbmuser tbmuser = Common.getCurrentUser(request);
    boolean isLogin = (tbmuser != null && tbmuser.getUserId() != null);

    String serviceUrl = Common.ROOT + "/krrs?hanya_tampil_jsp=true&p=kursus&s=_kursus_service";
    String namaTampilTbmuser = "";
    if (isLogin) {
        namaTampilTbmuser = tbmuser.getUserNama() == null || tbmuser.getUserNama().trim().isEmpty() ? tbmuser.getUserId() : tbmuser.getUserNama();
        namaTampilTbmuser = namaTampilTbmuser.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
%>

<style>
    :root {
        --kr-primary: #4f46e5;
        --kr-primary-dark: #4338ca;
        --kr-primary-light: #eef2ff;
        --kr-violet: #7c3aed;
        --kr-accent: #f59e0b;
        --kr-success: #16a34a;
        --kr-success-light: #ecfdf5;
        --kr-danger: #dc2626;
        --kr-ink: #0f172a;
        --kr-muted: #64748b;
        --kr-border: rgba(15,23,42,.08);
        --kr-radius: 1.25rem;
        --kr-shadow-sm: 0 2px 10px rgba(15,23,42,.06);
        --kr-shadow-md: 0 12px 32px rgba(15,23,42,.10);
        --kr-shadow-lg: 0 24px 60px rgba(15,23,42,.16);
    }

    .page-bg-<%=rnd%> { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: #f8fafc; z-index: -999; pointer-events: none; }

    .hero-section-<%=rnd%> {
        position: relative; overflow: hidden; z-index: 2; color: #fff;
        background: linear-gradient(120deg, #3730a3 0%, var(--kr-primary) 45%, var(--kr-violet) 100%);
        border-radius: 0 0 2.5rem 2.5rem; box-shadow: var(--kr-shadow-lg);
    }
    .hero-section-<%=rnd%>::before, .hero-section-<%=rnd%>::after {
        content: ""; position: absolute; border-radius: 50%; filter: blur(60px); pointer-events: none;
    }
    .hero-section-<%=rnd%>::before { width: 340px; height: 340px; background: rgba(245,158,11,.35); top: -140px; right: -80px; }
    .hero-section-<%=rnd%>::after { width: 280px; height: 280px; background: rgba(255,255,255,.14); bottom: -160px; left: -60px; }
    .hero-inner-<%=rnd%> { position: relative; z-index: 1; }
    .logo-shadow-<%=rnd%> { filter: drop-shadow(0 8px 16px rgba(0,0,0,.25)); background: #fff; border-radius: 20px; padding: 10px; }
    .text-shadow-<%=rnd%> { text-shadow: 0 2px 12px rgba(0,0,0,.18); }
    .tracking-wide-<%=rnd%> { letter-spacing: .3px; }
    .badge-pmb-<%=rnd%> { background: rgba(255,255,255,.14); backdrop-filter: blur(10px); border: 1px solid rgba(255,255,255,.28); box-shadow: var(--kr-shadow-sm); }

    /* ===== Filter bar ===== */
    .kr-filter-card-<%=rnd%> { background: #fff; border: 1px solid var(--kr-border); border-radius: var(--kr-radius); box-shadow: var(--kr-shadow-sm); padding: 1.1rem 1.25rem; margin-bottom: 1.75rem; }
    .kr-filter-card-<%=rnd%> .form-control, .kr-filter-card-<%=rnd%> .form-select, .kr-filter-card-<%=rnd%> .input-group-text {
        border-color: var(--kr-border); border-radius: .85rem; background: #f8fafc;
    }
    .kr-filter-card-<%=rnd%> .form-control:focus, .kr-filter-card-<%=rnd%> .form-select:focus { border-color: var(--kr-primary); box-shadow: 0 0 0 .2rem rgba(79,70,229,.12); background: #fff; }

    /* ===== Tabs (segmented pill switcher) ===== */
    .nav-tabs-custom-<%=rnd%> { border-bottom: 0; margin: 2rem 0 1.75rem; gap: .4rem; background: #eef1f8; border-radius: 999px; padding: .35rem; display: inline-flex; }
    .nav-tabs-custom-<%=rnd%> .nav-item { flex: 0 0 auto; }
    .nav-tabs-custom-<%=rnd%> .nav-link { border: none; color: var(--kr-muted); font-weight: 700; padding: .65rem 1.4rem; border-radius: 999px; transition: all .2s ease; }
    .nav-tabs-custom-<%=rnd%> .nav-link.active { color: #fff; background: linear-gradient(120deg, var(--kr-primary), var(--kr-violet)); box-shadow: 0 8px 18px rgba(79,70,229,.28); }
    .nav-tabs-custom-<%=rnd%> .nav-link:hover:not(.active) { color: var(--kr-primary); }

    /* ===== Course cards ===== */
    .course-card-<%=rnd%> { border: 1px solid var(--kr-border); border-radius: var(--kr-radius); transition: transform .25s ease, box-shadow .25s ease; overflow: hidden; background: #fff; box-shadow: var(--kr-shadow-sm); height: 100%; display: flex; flex-direction: column; }
    .course-card-<%=rnd%>:hover { transform: translateY(-6px); box-shadow: var(--kr-shadow-md); }
    .course-icon-wrapper-<%=rnd%> { height: 152px; background: linear-gradient(135deg, var(--kr-primary-light) 0%, #e0e7ff 100%); display: flex; align-items: center; justify-content: center; font-size: 3.25rem; color: var(--kr-primary); overflow: hidden; position: relative; }
    .course-icon-wrapper-<%=rnd%> img { width: 100%; height: 100%; object-fit: cover; }
    .course-body-<%=rnd%> { padding: 1.25rem; flex-grow: 1; display: flex; flex-direction: column; }
    .course-title-<%=rnd%> { font-size: 1.04rem; font-weight: 700; color: var(--kr-ink); margin-bottom: .4rem; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; line-height: 1.35; }
    .course-category-<%=rnd%> { display: inline-block; font-size: .72rem; font-weight: 700; text-transform: uppercase; letter-spacing: .04em; margin-bottom: .6rem; background: var(--kr-primary-light); color: var(--kr-primary); padding: .22rem .6rem; border-radius: 999px; }
    .course-price-<%=rnd%> { font-size: 1.15rem; font-weight: 800; color: var(--kr-success); margin-top: auto; padding-top: .85rem; border-top: 1px dashed var(--kr-border); }
    .course-rating-<%=rnd%> { color: var(--kr-accent); font-size: .82rem; }

    .btn-kr-primary-<%=rnd%> { background: linear-gradient(120deg, var(--kr-primary), var(--kr-violet)); border: 0; color: #fff; box-shadow: 0 8px 18px rgba(79,70,229,.25); }
    .btn-kr-primary-<%=rnd%>:hover { color: #fff; filter: brightness(1.05); transform: translateY(-1px); }
    .btn-kr-outline-<%=rnd%> { border: 1.5px solid var(--kr-primary); color: var(--kr-primary); background: #fff; }
    .btn-kr-outline-<%=rnd%>:hover { background: var(--kr-primary-light); color: var(--kr-primary-dark); }

    /* ===== Soft cards / stats / player / kurikulum ===== */
    .kr-stat-card-<%=rnd%> { border: 1px solid var(--kr-border); border-radius: 1rem; background: #fff; box-shadow: var(--kr-shadow-sm); border-left: 4px solid var(--kr-primary); padding: 1.1rem 1.25rem; height: 100%; transition: box-shadow .2s ease; }
    .kr-stat-card-<%=rnd%>:hover { box-shadow: var(--kr-shadow-md); }
    .kr-soft-card-<%=rnd%> { border: 1px solid var(--kr-border); border-radius: var(--kr-radius); background: #fff; box-shadow: var(--kr-shadow-sm); }
    .kr-section-item-<%=rnd%> { border: 1px solid var(--kr-border); border-radius: 1rem; margin-bottom: .7rem; overflow: hidden; }
    .kr-materi-row-<%=rnd%> { border-top: 1px solid #f1f3f5; padding: .6rem .9rem; display:flex; align-items:center; gap:.6rem; }
    .kr-player-sidebar-<%=rnd%> { max-height: 70vh; overflow-y: auto; }
    .kr-materi-link-<%=rnd%> { cursor: pointer; padding: .6rem .8rem; border-radius: .7rem; display:flex; align-items:center; gap:.6rem; transition: background .15s ease; }
    .kr-materi-link-<%=rnd%>:hover { background: var(--kr-primary-light); }
    .kr-materi-link-<%=rnd%>.active { background: var(--kr-primary-light); color: var(--kr-primary-dark); font-weight: 700; }
    .kr-lock-<%=rnd%> { color:#adb5bd; }

    .kr-modul-card-<%=rnd%> { cursor: pointer; background: #fff; border: 1px solid var(--kr-border); border-radius: 1rem; padding: 1rem 1.1rem; height: 100%; transition: box-shadow .2s ease, transform .2s ease; box-shadow: var(--kr-shadow-sm); }
    .kr-modul-card-<%=rnd%>:hover { box-shadow: var(--kr-shadow-md); transform: translateY(-3px); }
    .kr-modul-badge-<%=rnd%> { font-weight: 800; font-size: .78rem; background: var(--kr-primary-light); color: var(--kr-primary); padding: .18rem .55rem; border-radius: 999px; white-space: nowrap; }
    .kr-modul-badge-<%=rnd%>.done { background: var(--kr-success-light); color: var(--kr-success); }
    .kr-modul-item-<%=rnd%> { cursor: pointer; display:flex; align-items:center; gap:.75rem; padding: .75rem .5rem; border-bottom: 1px solid #f1f3f5; transition: background .15s ease; }
    .kr-modul-item-<%=rnd%>:last-child { border-bottom: 0; }
    .kr-modul-item-<%=rnd%>:hover { background: var(--kr-primary-light); border-radius: .6rem; }

    .empty-state-<%=rnd%> { text-align: center; padding: 3.5rem 1.5rem; background: #fff; border: 1px dashed var(--kr-border); border-radius: var(--kr-radius); color: var(--kr-muted); }
    .empty-state-<%=rnd%> i { font-size: 2.75rem; color: #c7cbe0; margin-bottom: 1rem; display: block; }

    .kr-modal-header-<%=rnd%> { background: linear-gradient(120deg, var(--kr-primary-light), #f5f3ff); border-bottom: 1px solid var(--kr-border); }
    #krModalDetail<%=rnd%> .modal-content, #krModalForm<%=rnd%> .modal-content, #krModalKurikulum<%=rnd%> .modal-content, #krModalModulPembelajaran<%=rnd%> .modal-content { border-radius: 1.25rem; overflow: hidden; }

    .kr-auth-widget-<%=rnd%> { position: absolute; top: 1.1rem; right: 1.5rem; z-index: 4; }
    .kr-btn-login-<%=rnd%> { background: rgba(255,255,255,.16); backdrop-filter: blur(10px); border: 1px solid rgba(255,255,255,.32); color: #fff; font-weight: 700; padding: .5rem 1.15rem; border-radius: 999px; transition: .2s; }
    .kr-btn-login-<%=rnd%>:hover { background: #fff; color: var(--kr-primary); }

    .footer-pmb-<%=rnd%> { background: linear-gradient(135deg, #0b1220 0%, #1e1b4b 100%); color: #fff; position: relative; z-index: 10; margin-top: 5rem; }
    .footer-pmb-<%=rnd%> h5 { letter-spacing: .3px; }
    .footer-pmb-<%=rnd%> .social-icon { width: 36px; height: 36px; display: inline-flex; align-items: center; justify-content: center; border-radius: 50%; background: rgba(255,255,255,0.08); color: #fff; transition: .2s; margin-right: 0.5rem; text-decoration: none; }
    .footer-pmb-<%=rnd%> .social-icon:hover { background: var(--kr-primary); transform: translateY(-3px); }
</style>

<div class="page-bg-<%=rnd%>"></div>

<div class="container-fluid p-0">
    <div class="hero-section-<%=rnd%> pt-4 pb-5 mb-5 text-center">
        <div class="kr-auth-widget-<%=rnd%>">
            <% if (isLogin) { %>
                <div class="dropdown">
                    <button class="btn kr-btn-login-<%=rnd%> dropdown-toggle" type="button" data-bs-toggle="dropdown">
                        <i class="fas fa-user-circle me-1"></i> <%= namaTampilTbmuser %>
                    </button>
                    <ul class="dropdown-menu dropdown-menu-end shadow-sm">
                        <li><a class="dropdown-item" href="javascript:void(0)" onclick="krKonfirmasiLogout()"><i class="fas fa-sign-out-alt me-2 text-danger"></i><%=Common.getBahasaConfig("Keluar")%></a></li>
                    </ul>
                </div>
            <% } else { %>
                <button class="btn kr-btn-login-<%=rnd%> me-2" onclick="krBukaModalDaftar()"><i class="fas fa-user-plus me-1"></i> <%=Common.getBahasaConfig("Daftar")%></button>
                <button class="btn kr-btn-login-<%=rnd%>" onclick="krBukaModalLogin()"><i class="fas fa-sign-in-alt me-1"></i> <%=Common.getBahasaConfig("Masuk")%></button>
            <% } %>
        </div>
        <div class="container pb-4 pt-2 hero-inner-<%=rnd%>">
            <div class="d-inline-flex align-items-center gap-2 badge-pmb-<%=rnd%> rounded-pill px-3 py-2 mb-4 animate__animated animate__fadeInDown">
                <i class="fas fa-graduation-cap text-warning"></i>
                <span class="fw-semibold small text-white"><%= Common.getBahasaConfig("Portal Kursus & Pelatihan") %></span>
            </div>

            <% if (logo_PerguruanTinggi != null && !logo_PerguruanTinggi.trim().isEmpty()) { %>
                <img src="<%= logo_PerguruanTinggi %>" alt="Logo Institusi"
                     class="img-fluid mb-3 animate__animated animate__fadeInDown logo-shadow-<%=rnd%>"
                     style="max-height: 96px; object-fit: contain;">
            <% } %>

            <h1 class="fw-bold text-white tracking-wide-<%=rnd%> text-shadow-<%=rnd%> mb-2 animate__animated animate__fadeInDown" style="font-size:2.15rem;">
                <%= judulNamaPerguruanTinggi != null ? judulNamaPerguruanTinggi : "" %>
            </h1>

            <% if (Motto != null && !Motto.trim().isEmpty()) { %>
                <p class="fw-light text-white-50 fst-italic mb-1 animate__animated animate__fadeInUp">"<%= Motto %>"</p>
            <% } %>
            <p class="text-white-50 mb-0 animate__animated animate__fadeInUp" style="max-width:560px;margin-inline:auto;">
                <%= Common.getBahasaConfig("Belajar dari kursus pilihan, atau buat dan jual kursus buatan Anda sendiri.") %>
            </p>
        </div>
    </div>
</div>

<% if (!isLogin) { %>
<div class="container mt-n4 mb-5" style="position: relative; z-index: 3;">
    <div class="kr-soft-card-<%=rnd%> p-4 p-md-5 text-center" style="border-top: 4px solid var(--kr-accent);">
        <i class="fas fa-user-graduate mb-2" style="font-size:2.25rem;color:var(--kr-accent);"></i>
        <h5 class="fw-bold mb-2"><%=Common.getBahasaConfig("Bukan Mahasiswa, Siswa, Dosen, Guru, Pegawai, atau Anggota Koperasi?")%></h5>
        <p class="text-muted mb-3" style="max-width:640px;margin-inline:auto;">
            <%=Common.getBahasaConfig("Anda tetap bisa bergabung sebagai Member baru. Sebagai Member, Anda bisa belajar dari kursus yang tersedia (Peserta) maupun membuat dan menjual kursus buatan Anda sendiri (Kreator/Instruktur).")%>
        </p>
        <button class="btn btn-kr-primary-<%=rnd%> rounded-pill px-4" onclick="krBukaModalDaftar()"><i class="fas fa-user-plus me-2"></i><%=Common.getBahasaConfig("Daftar Sebagai Member Baru")%></button>
    </div>
</div>
<% } %>

<div class="container mb-5" style="position: relative; z-index: 1; min-height: 50vh;">

    <div class="text-center">
    <ul class="nav nav-tabs nav-tabs-custom-<%=rnd%> justify-content-center" id="kursusTab<%=rnd%>" role="tablist">
        <li class="nav-item" role="presentation">
            <button class="nav-link active" id="katalog-tab-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#katalog-<%=rnd%>" type="button">
                <i class="fas fa-store me-2"></i><%=Common.getBahasaConfig("Katalog Kursus")%>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link" id="belajar-tab-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#belajar-<%=rnd%>" type="button" onclick="krLoadKursusSaya()">
                <i class="fas fa-book-reader me-2"></i><%=Common.getBahasaConfig("Kursus Saya")%>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link" id="instruktur-tab-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#instruktur-<%=rnd%>" type="button" onclick="krLoadInstruktur()">
                <i class="fas fa-chalkboard-teacher me-2"></i><%=Common.getBahasaConfig("Instruktur")%>
            </button>
        </li>
    </ul>
    </div>

    <div class="tab-content" id="kursusTabContent<%=rnd%>">

        <!-- =============== TAB KATALOG =============== -->
        <div class="tab-pane fade show active" id="katalog-<%=rnd%>" role="tabpanel">
            <div class="kr-filter-card-<%=rnd%> row align-items-center g-2">
                <div class="col-md-4">
                    <div class="input-group">
                        <span class="input-group-text border-end-0"><i class="fas fa-search text-muted"></i></span>
                        <input type="text" class="form-control border-start-0" id="krSearch<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari nama kursus...")%>" onkeyup="krDebounceFilter()">
                    </div>
                </div>
                <div class="col-md-3">
                    <select class="form-select" id="krFilterKategori<%=rnd%>" onchange="krLoadKatalog()">
                        <option value=""><%=Common.getBahasaConfig("Semua Kategori")%></option>
                    </select>
                </div>
                <div class="col-md-2">
                    <select class="form-select" id="krFilterTingkat<%=rnd%>" onchange="krLoadKatalog()">
                        <option value=""><%=Common.getBahasaConfig("Semua Level")%></option>
                    </select>
                </div>
                <div class="col-md-3">
                    <select class="form-select" id="krFilterHarga<%=rnd%>" onchange="krLoadKatalog()">
                        <option value=""><%=Common.getBahasaConfig("Semua Harga")%></option>
                        <option value="gratis"><%=Common.getBahasaConfig("Gratis")%></option>
                        <option value="berbayar"><%=Common.getBahasaConfig("Berbayar")%></option>
                    </select>
                </div>
            </div>

            <div class="row g-4" id="krKatalogGrid<%=rnd%>">
                <div class="col-12 text-center py-5">
                    <div class="spinner-border text-primary" role="status"></div>
                    <p class="mt-2 text-muted"><%=Common.getBahasaConfig("Memuat katalog kursus...")%></p>
                </div>
            </div>
        </div>

        <!-- =============== TAB KURSUS SAYA (BELAJAR) =============== -->
        <div class="tab-pane fade" id="belajar-<%=rnd%>" role="tabpanel">
            <% if (!isLogin) { %>
                <div class="empty-state-<%=rnd%>">
                    <i class="fas fa-user-lock"></i>
                    <h5 class="fw-bold text-dark"><%=Common.getBahasaConfig("Anda Belum Masuk")%></h5>
                    <p><%=Common.getBahasaConfig("Silakan masuk terlebih dahulu untuk melihat daftar kursus yang Anda ikuti.")%></p>
                    <button class="btn btn-kr-primary-<%=rnd%> rounded-pill px-4" onclick="krBukaModalLogin()"><i class="fas fa-sign-in-alt me-2"></i><%=Common.getBahasaConfig("Masuk Sekarang")%></button>
                </div>
            <% } else { %>
                <div id="krBelajarWrap<%=rnd%>">
                    <div class="row g-4" id="krBelajarGrid<%=rnd%>">
                        <div class="col-12 text-center py-5">
                            <div class="spinner-border text-success" role="status"></div>
                        </div>
                    </div>
                </div>
                <div id="krPlayerWrap<%=rnd%>" style="display:none;"></div>
            <% } %>
        </div>

        <!-- =============== TAB INSTRUKTUR =============== -->
        <div class="tab-pane fade" id="instruktur-<%=rnd%>" role="tabpanel">
            <% if (!isLogin) { %>
                <div class="empty-state-<%=rnd%>">
                    <i class="fas fa-chalkboard-teacher"></i>
                    <h5 class="fw-bold text-dark"><%=Common.getBahasaConfig("Anda Belum Masuk")%></h5>
                    <p><%=Common.getBahasaConfig("Masuk untuk mulai membuat dan menjual kursus Anda sendiri.")%></p>
                    <button class="btn btn-kr-primary-<%=rnd%> rounded-pill px-4" onclick="krBukaModalLogin()"><i class="fas fa-sign-in-alt me-2"></i><%=Common.getBahasaConfig("Masuk Sekarang")%></button>
                </div>
            <% } else { %>
                <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
                    <h5 class="fw-bold mb-0"><%=Common.getBahasaConfig("Dashboard Instruktur")%></h5>
                    <button class="btn btn-kr-primary-<%=rnd%> rounded-pill px-4" onclick="krBukaFormProduk(null)"><i class="fas fa-plus me-2"></i><%=Common.getBahasaConfig("Buat Kursus Baru")%></button>
                </div>
                <div class="row g-3 mb-4" id="krStatCards<%=rnd%>"></div>
                <div class="kr-soft-card-<%=rnd%> p-3">
                    <div class="table-responsive">
                        <table class="table align-middle mb-0">
                            <thead>
                                <tr class="text-muted small text-uppercase">
                                    <th></th><th><%=Common.getBahasaConfig("Judul")%></th><th><%=Common.getBahasaConfig("Status")%></th>
                                    <th><%=Common.getBahasaConfig("Harga")%></th><th><%=Common.getBahasaConfig("Siswa")%></th><th></th>
                                </tr>
                            </thead>
                            <tbody id="krKursusInstrukturBody<%=rnd%>">
                                <tr><td colspan="6" class="text-center text-muted py-4"><%=Common.getBahasaConfig("Memuat...")%></td></tr>
                            </tbody>
                        </table>
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
                <h5 class="text-uppercase fw-bold text-warning mb-3"><i class="fas fa-university me-2"></i><%= judulNamaPerguruanTinggi %></h5>
                <p class="text-white-50 small pe-md-4"><%= Common.getBahasaConfig("Pusat Layanan Pembelajaran (LMS). Siapa saja bisa belajar maupun membuat dan menjual kursus sendiri.") %></p>
                <% if (Motto != null && !Motto.trim().isEmpty()) { %><p class="text-info fst-italic mt-2 mb-0">"<%= Motto %>"</p><% } %>
            </div>
            <div class="col-md-3 col-lg-3 col-xl-3 mx-auto mb-4">
                <h5 class="text-uppercase fw-bold text-warning mb-3"><%= Common.getBahasaConfig("Bantuan") %></h5>
                <p class="mb-2"><a href="#" class="text-white-50 text-decoration-none small"><i class="fas fa-angle-right me-2"></i><%= Common.getBahasaConfig("Panduan Pembelajaran") %></a></p>
                <p class="mb-2"><a href="#" class="text-white-50 text-decoration-none small"><i class="fas fa-angle-right me-2"></i><%= Common.getBahasaConfig("Syarat dan Ketentuan") %></a></p>
            </div>
            <div class="col-md-4 col-lg-3 col-xl-3 mx-auto mb-4">
                <h5 class="text-uppercase fw-bold text-warning mb-3"><%= Common.getBahasaConfig("Hubungi Kami") %></h5>
                <p class="text-white-50 small mb-2"><i class="fas fa-map-marker-alt me-2 text-white"></i> <%= Alamat1 != null ? Alamat1 : "-" %></p>
                <p class="text-white-50 small mb-4"><i class="fas fa-envelope me-2 text-white"></i> <%= Email != null ? Email : "-" %></p>
            </div>
        </div>
        <hr class="mb-4 mt-2" style="border-color: rgba(255,255,255,0.2);">
        <div class="row align-items-center">
            <div class="col-md-12 col-lg-12 text-center">
                <p class="text-white-50 small mb-0">&copy; <%= java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) %> <strong class="text-warning"><%= judulNamaPerguruanTinggi %></strong>. <%= Common.getBahasaConfig("Hak Cipta Dilindungi Undang-Undang.") %></p>
            </div>
        </div>
    </div>
</footer>

<!-- ============================================================= MODAL DETAIL KURSUS ============================================================= -->
<div class="modal fade" id="krModalDetail<%=rnd%>" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-xl modal-dialog-scrollable">
        <div class="modal-content border-0 shadow-lg">
            <div class="modal-header kr-modal-header-<%=rnd%>">
                <h5 class="modal-title fw-bold" id="krDetailJudul<%=rnd%>"></h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body" id="krDetailBody<%=rnd%>"></div>
        </div>
    </div>
</div>

<!-- ============================================================= MODAL FORM KURSUS (INSTRUKTUR) ============================================================= -->
<div class="modal fade" id="krModalForm<%=rnd%>" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-lg">
        <div class="modal-content border-0 shadow-lg">
            <div class="modal-header kr-modal-header-<%=rnd%>">
                <h5 class="modal-title fw-bold" id="krFormJudulModal<%=rnd%>"><%=Common.getBahasaConfig("Kursus Baru")%></h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <input type="hidden" id="krFId<%=rnd%>">
                <div class="mb-3">
                    <label class="form-label fw-semibold"><%=Common.getBahasaConfig("Judul Kursus")%> *</label>
                    <input type="text" class="form-control" id="krFNama<%=rnd%>">
                </div>
                <div class="row g-2 mb-3">
                    <div class="col-md-6">
                        <label class="form-label fw-semibold"><%=Common.getBahasaConfig("Kategori")%></label>
                        <select class="form-select" id="krFKategori<%=rnd%>"><option value=""></option></select>
                    </div>
                    <div class="col-md-6">
                        <label class="form-label fw-semibold"><%=Common.getBahasaConfig("Level")%></label>
                        <select class="form-select" id="krFTingkat<%=rnd%>"><option value=""></option></select>
                    </div>
                </div>
                <div class="mb-3">
                    <label class="form-label fw-semibold"><%=Common.getBahasaConfig("Deskripsi Singkat")%></label>
                    <textarea class="form-control" id="krFKeterangan<%=rnd%>" rows="2"></textarea>
                </div>
                <div class="mb-3">
                    <label class="form-label fw-semibold"><%=Common.getBahasaConfig("Deskripsi Lengkap")%></label>
                    <textarea class="form-control" id="krFDeskripsi<%=rnd%>" rows="4"></textarea>
                </div>
                <div class="row g-2 align-items-end mb-3">
                    <div class="col-md-4">
                        <div class="form-check form-switch pt-4">
                            <input class="form-check-input" type="checkbox" id="krFGratis<%=rnd%>" onchange="document.getElementById('krFHargaWrap<%=rnd%>').style.display = this.checked ? 'none' : 'block';">
                            <label class="form-check-label fw-semibold" for="krFGratis<%=rnd%>"><%=Common.getBahasaConfig("Kursus Gratis")%></label>
                        </div>
                    </div>
                    <div class="col-md-8" id="krFHargaWrap<%=rnd%>">
                        <label class="form-label fw-semibold"><%=Common.getBahasaConfig("Harga (Rp)")%></label>
                        <input type="number" min="0" class="form-control" id="krFHarga<%=rnd%>" value="0">
                    </div>
                </div>
                <div class="mb-2">
                    <label class="form-label fw-semibold"><%=Common.getBahasaConfig("Thumbnail Kursus")%></label>
                    <input type="file" class="form-control" id="krFThumbnail<%=rnd%>" accept="image/*">
                </div>
            </div>
            <div class="modal-footer border-top-0">
                <button class="btn btn-outline-secondary rounded-pill px-4" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
                <button class="btn btn-kr-primary-<%=rnd%> rounded-pill px-4" id="krFSimpanBtn<%=rnd%>" onclick="krSimpanProduk()"><%=Common.getBahasaConfig("Simpan")%></button>
            </div>
        </div>
    </div>
</div>

<!-- ============================================================= MODAL KURIKULUM ============================================================= -->
<div class="modal fade" id="krModalKurikulum<%=rnd%>" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-lg modal-dialog-scrollable">
        <div class="modal-content border-0 shadow-lg">
            <div class="modal-header kr-modal-header-<%=rnd%>">
                <h5 class="modal-title fw-bold"><%=Common.getBahasaConfig("Kelola Kurikulum")%></h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <input type="hidden" id="krKurProdukId<%=rnd%>">
                <div id="krKurikulumList<%=rnd%>"></div>
                <div class="input-group mt-3">
                    <input type="text" class="form-control" id="krKurSeksiBaru<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Judul section baru...")%>">
                    <button class="btn btn-outline-primary" onclick="krTambahSeksi()"><i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Tambah Section")%></button>
                </div>
            </div>
            <div class="modal-footer border-top">
                <button type="button" class="btn btn-outline-secondary rounded-pill px-3" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Tutup")%></button>
                <button type="button" class="btn btn-kr-primary-<%=rnd%> rounded-pill px-4" onclick="krSimpanKurikulum()"><i class="fas fa-save me-1"></i><%=Common.getBahasaConfig("Simpan Kurikulum")%></button>
            </div>
        </div>
    </div>
</div>

<!-- ============================================================= MODAL DAFTAR ITEM MODUL (PEMBELAJARAN) ============================================================= -->
<div class="modal fade" id="krModalModulPembelajaran<%=rnd%>" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-lg modal-dialog-scrollable">
        <div class="modal-content border-0 shadow-lg">
            <div class="modal-header kr-modal-header-<%=rnd%>">
                <h5 class="modal-title fw-bold" id="krModulModalJudul<%=rnd%>"></h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body" id="krModulModalBody<%=rnd%>"></div>
        </div>
    </div>
</div>

<!-- ============================================================= MODAL KELOLA SOAL KUIS (INSTRUKTUR) ============================================================= -->
<div class="modal fade" id="krModalSoalKuis<%=rnd%>" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-lg modal-dialog-scrollable">
        <div class="modal-content border-0 shadow-lg">
            <div class="modal-header kr-modal-header-<%=rnd%>">
                <h5 class="modal-title fw-bold"><i class="fas fa-list-check me-2"></i><%=Common.getBahasaConfig("Kelola Soal Kuis")%> — <span id="krSoalKuisJudulMateri<%=rnd%>"></span></h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <input type="hidden" id="krSoalKuisMateriId<%=rnd%>">
                <div class="kr-soft-card-<%=rnd%> p-3 mb-3">
                    <h6 class="fw-bold small text-uppercase text-muted"><%=Common.getBahasaConfig("Konfigurasi Kuis")%></h6>
                    <div class="row g-2">
                        <div class="col-md-3">
                            <label class="form-label small"><%=Common.getBahasaConfig("Nilai Lulus (KKM)")%></label>
                            <input type="number" class="form-control form-control-sm" id="krKuisNilaiLulus<%=rnd%>" value="60">
                        </div>
                        <div class="col-md-3">
                            <label class="form-label small"><%=Common.getBahasaConfig("Batas Waktu (menit)")%></label>
                            <input type="number" class="form-control form-control-sm" id="krKuisBatasWaktu<%=rnd%>" placeholder="Tanpa batas">
                        </div>
                        <div class="col-md-3">
                            <label class="form-label small"><%=Common.getBahasaConfig("Batas Percobaan")%></label>
                            <input type="number" class="form-control form-control-sm" id="krKuisBatasPercobaan<%=rnd%>" placeholder="Tanpa batas">
                        </div>
                        <div class="col-md-3">
                            <label class="form-label small"><%=Common.getBahasaConfig("Jumlah Soal Ditampilkan")%></label>
                            <input type="number" class="form-control form-control-sm" id="krKuisJumlahTampil<%=rnd%>" placeholder="Semua soal">
                        </div>
                        <div class="col-md-6 form-check mt-2">
                            <input type="checkbox" class="form-check-input" id="krKuisAcakSoal<%=rnd%>">
                            <label class="form-check-label small" for="krKuisAcakSoal<%=rnd%>"><%=Common.getBahasaConfig("Acak urutan soal")%></label>
                        </div>
                        <div class="col-md-6 form-check mt-2">
                            <input type="checkbox" class="form-check-input" id="krKuisAcakJawaban<%=rnd%>">
                            <label class="form-check-label small" for="krKuisAcakJawaban<%=rnd%>"><%=Common.getBahasaConfig("Acak urutan pilihan jawaban")%></label>
                        </div>
                    </div>
                    <button class="btn btn-sm btn-kr-primary-<%=rnd%> rounded-pill px-3 mt-2" onclick="krSimpanConfigKuis()"><%=Common.getBahasaConfig("Simpan Konfigurasi")%></button>
                </div>

                <h6 class="fw-bold small text-uppercase text-muted"><%=Common.getBahasaConfig("Daftar Soal")%></h6>
                <div id="krDaftarSoalKuis<%=rnd%>"></div>

                <div class="kr-soft-card-<%=rnd%> p-3 mt-3">
                    <h6 class="fw-bold small text-uppercase text-muted"><%=Common.getBahasaConfig("Tambah Soal")%></h6>
                    <input type="hidden" id="krSoalEditId<%=rnd%>">
                    <div class="mb-2">
                        <textarea class="form-control form-control-sm" id="krSoalTeks<%=rnd%>" rows="2" placeholder="<%=Common.getBahasaConfig("Tulis pertanyaan di sini...")%>"></textarea>
                    </div>
                    <div class="row g-2 mb-2">
                        <div class="col-md-4">
                            <select class="form-select form-select-sm" id="krSoalJenisKoreksi<%=rnd%>" onchange="krToggleJenisSoal()">
                                <option value="otomatis"><%=Common.getBahasaConfig("Pilihan Ganda / Benar-Salah (otomatis)")%></option>
                                <option value="manual"><%=Common.getBahasaConfig("Esai (koreksi manual)")%></option>
                            </select>
                        </div>
                        <div class="col-md-4" id="krSoalJenisPgWrap<%=rnd%>">
                            <select class="form-select form-select-sm" id="krSoalJenisPg<%=rnd%>" onchange="krToggleJenisPg()">
                                <option value="Multiple choice"><%=Common.getBahasaConfig("Pilihan Ganda")%></option>
                                <option value="Benar Salah"><%=Common.getBahasaConfig("Benar / Salah")%></option>
                            </select>
                        </div>
                        <div class="col-md-2">
                            <input type="number" class="form-control form-control-sm" id="krSoalSkor<%=rnd%>" placeholder="Skor benar" value="1">
                        </div>
                        <div class="col-md-2">
                            <input type="number" class="form-control form-control-sm" id="krSoalSkorSalah<%=rnd%>" placeholder="Skor salah" value="0">
                        </div>
                    </div>
                    <div id="krSoalPilihanWrap<%=rnd%>"></div>
                    <button class="btn btn-sm btn-outline-secondary mt-2" onclick="krTambahBarisPilihan()" id="krBtnTambahPilihan<%=rnd%>"><i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Tambah Pilihan")%></button>
                    <div class="mt-3 text-end">
                        <button class="btn btn-sm btn-outline-secondary rounded-pill px-3" onclick="krResetFormSoal()"><%=Common.getBahasaConfig("Batal")%></button>
                        <button class="btn btn-sm btn-kr-primary-<%=rnd%> rounded-pill px-3" onclick="krSimpanSoalKuis()"><%=Common.getBahasaConfig("Simpan Soal")%></button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- ============================================================= MODAL LOGIN ============================================================= -->
<div class="modal fade" id="krModalLogin<%=rnd%>" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow-lg">
            <div class="modal-header kr-modal-header-<%=rnd%>">
                <h5 class="modal-title fw-bold"><i class="fas fa-sign-in-alt me-2"></i><%=Common.getBahasaConfig("Masuk")%></h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <div id="krLoginAlert<%=rnd%>" class="alert alert-danger d-none py-2 small"></div>
                <form id="krLoginForm<%=rnd%>">
                    <div class="mb-3">
                        <label class="form-label fw-semibold small"><%=Common.getBahasaConfig("ID Pengguna")%></label>
                        <input type="text" class="form-control" id="krLoginUsername<%=rnd%>" autocomplete="username" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold small"><%=Common.getBahasaConfig("Kata Sandi")%></label>
                        <input type="password" class="form-control" id="krLoginPassword<%=rnd%>" autocomplete="current-password" required>
                    </div>
                    <div class="form-check mb-3">
                        <input class="form-check-input" type="checkbox" id="krLoginRemember<%=rnd%>">
                        <label class="form-check-label small" for="krLoginRemember<%=rnd%>"><%=Common.getBahasaConfig("Ingat saya")%></label>
                    </div>
                    <button type="submit" class="btn btn-kr-primary-<%=rnd%> w-100 rounded-pill" id="krLoginSubmitBtn<%=rnd%>"><%=Common.getBahasaConfig("Masuk")%></button>
                </form>
                <p class="text-center small text-muted mt-3 mb-0">
                    <%=Common.getBahasaConfig("Belum punya akun?")%>
                    <a href="javascript:void(0)" class="fw-semibold text-decoration-none" onclick="krBukaModalDaftar()"><%=Common.getBahasaConfig("Daftar sebagai Member baru")%></a>
                </p>
            </div>
        </div>
    </div>
</div>

<!-- ============================================================= MODAL DAFTAR MEMBER BARU ============================================================= -->
<div class="modal fade" id="krModalDaftar<%=rnd%>" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow-lg">
            <div class="modal-header kr-modal-header-<%=rnd%>">
                <h5 class="modal-title fw-bold"><i class="fas fa-user-plus me-2"></i><%=Common.getBahasaConfig("Daftar Sebagai Member Baru")%></h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <p class="text-muted small">
                    <%=Common.getBahasaConfig("Khusus untuk Anda yang bukan mahasiswa, siswa, dosen, guru, pegawai, atau anggota koperasi dari sistem ini. Sebagai Member, Anda bisa menjadi Peserta (belajar) maupun Kreator/Instruktur (membuat & menjual kursus).")%>
                </p>
                <div id="krDaftarAlert<%=rnd%>" class="alert alert-danger d-none py-2 small"></div>
                <form id="krDaftarForm<%=rnd%>">
                    <div class="mb-3">
                        <label class="form-label fw-semibold small"><%=Common.getBahasaConfig("Nama Lengkap")%> *</label>
                        <input type="text" class="form-control" id="krDNama<%=rnd%>" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold small"><%=Common.getBahasaConfig("Email")%> *</label>
                        <input type="email" class="form-control" id="krDEmail<%=rnd%>" autocomplete="email" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold small"><%=Common.getBahasaConfig("No. HP")%></label>
                        <input type="text" class="form-control" id="krDHp<%=rnd%>" autocomplete="tel">
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold small"><%=Common.getBahasaConfig("Username")%> *</label>
                        <input type="text" class="form-control" id="krDUsername<%=rnd%>" autocomplete="username" required minlength="4">
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold small"><%=Common.getBahasaConfig("Kata Sandi")%> *</label>
                        <input type="password" class="form-control" id="krDPassword<%=rnd%>" autocomplete="new-password" required minlength="6">
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold small"><%=Common.getBahasaConfig("Ulangi Kata Sandi")%> *</label>
                        <input type="password" class="form-control" id="krDPassword2<%=rnd%>" autocomplete="new-password" required minlength="6">
                    </div>
                    <button type="submit" class="btn btn-kr-primary-<%=rnd%> w-100 rounded-pill" id="krDaftarSubmitBtn<%=rnd%>"><%=Common.getBahasaConfig("Daftar Sekarang")%></button>
                </form>
                <p class="text-center small text-muted mt-3 mb-0">
                    <%=Common.getBahasaConfig("Sudah punya akun?")%>
                    <a href="javascript:void(0)" class="fw-semibold text-decoration-none" onclick="krBukaModalLogin()"><%=Common.getBahasaConfig("Masuk di sini")%></a>
                </p>
            </div>
        </div>
    </div>
</div>

<script>
    const krRnd = "<%=rnd%>";
    const krServiceUrl = "<%=serviceUrl%>";
    const krIsLogin = <%=isLogin%>;
    let krDataKategori = [];
    let krDataTingkat = [];
    let krFilterTimer = null;

    document.addEventListener("DOMContentLoaded", () => {
        krLoadFilterOptions();
        krLoadKatalog();
        const loginForm = document.getElementById("krLoginForm" + krRnd);
        if (loginForm) loginForm.addEventListener("submit", krProsesLogin);
        const daftarForm = document.getElementById("krDaftarForm" + krRnd);
        if (daftarForm) daftarForm.addEventListener("submit", krProsesDaftar);
    });

    function krTutupModalJikaTerbuka(id) {
        const el = document.getElementById(id);
        const instance = el ? bootstrap.Modal.getInstance(el) : null;
        if (instance) instance.hide();
    }

    /* =================== LOGIN / LOGOUT =================== */
    function krBukaModalLogin() {
        krTutupModalJikaTerbuka("krModalDaftar" + krRnd);
        const alertBox = document.getElementById("krLoginAlert" + krRnd);
        alertBox.classList.add("d-none");
        alertBox.innerText = "";
        document.getElementById("krLoginForm" + krRnd).reset();
        new bootstrap.Modal(document.getElementById("krModalLogin" + krRnd)).show();
    }
    function krKonfirmasiLogout() {
        if (confirm("<%=Common.getBahasaConfig("Yakin ingin keluar?")%>")) {
            window.location.href = "<%=request.getContextPath()%>/j_spring_security_logout";
        }
    }
    async function krProsesLogin(event) {
        event.preventDefault();
        const alertBox = document.getElementById("krLoginAlert" + krRnd);
        alertBox.classList.add("d-none");

        const username = document.getElementById("krLoginUsername" + krRnd).value.trim();
        const password = document.getElementById("krLoginPassword" + krRnd).value;
        const remember = document.getElementById("krLoginRemember" + krRnd).checked;
        if (!username || !password) {
            alertBox.innerText = "<%=Common.getBahasaConfig("ID Pengguna dan kata sandi tidak boleh kosong.")%>";
            alertBox.classList.remove("d-none");
            return;
        }

        const btn = document.getElementById("krLoginSubmitBtn" + krRnd);
        const oriBtnHtml = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Memverifikasi...")%>';

        try {
            const payload = new URLSearchParams();
            payload.append("username", username);
            payload.append("password", password);
            if (remember) payload.append("rememberMe", "true");

            const response = await fetch("<%=Common.ROOT%>/login?action=ajax_login", {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8" },
                credentials: "include",
                body: payload.toString()
            });
            const responseText = await response.text();
            let result = null;
            try { result = JSON.parse(responseText); }
            catch (parseError) { result = { status: "error", message: "<%=Common.getBahasaConfig("Gagal memproses respon login. Silakan coba lagi.")%>" }; }

            if (result.status === "success") {
                if (remember && result.cookie_val) {
                    const maxAge = 15552000;
                    document.cookie = "userinfo=" + encodeURIComponent(result.cookie_val) + "; max-age=" + maxAge + "; path=/";
                    document.cookie = "userid=" + encodeURIComponent(username) + "; max-age=" + maxAge + "; path=/";
                }
                krToast(result.message || "<%=Common.getBahasaConfig("Berhasil masuk.")%>", "success");
                window.location.reload();
            } else {
                alertBox.innerText = result.message || "<%=Common.getBahasaConfig("ID Pengguna atau kata sandi tidak valid.")%>";
                alertBox.classList.remove("d-none");
                btn.disabled = false;
                btn.innerHTML = oriBtnHtml;
            }
        } catch (e) {
            alertBox.innerText = "<%=Common.getBahasaConfig("Gagal terhubung ke peladen. Periksa koneksi Anda.")%>";
            alertBox.classList.remove("d-none");
            btn.disabled = false;
            btn.innerHTML = oriBtnHtml;
        }
    }

    /* =================== DAFTAR MEMBER BARU =================== */
    function krBukaModalDaftar() {
        krTutupModalJikaTerbuka("krModalLogin" + krRnd);
        const alertBox = document.getElementById("krDaftarAlert" + krRnd);
        alertBox.classList.add("d-none");
        alertBox.innerText = "";
        document.getElementById("krDaftarForm" + krRnd).reset();
        new bootstrap.Modal(document.getElementById("krModalDaftar" + krRnd)).show();
    }
    async function krProsesDaftar(event) {
        event.preventDefault();
        const alertBox = document.getElementById("krDaftarAlert" + krRnd);
        alertBox.classList.add("d-none");

        const nama = document.getElementById("krDNama" + krRnd).value.trim();
        const email = document.getElementById("krDEmail" + krRnd).value.trim();
        const hp = document.getElementById("krDHp" + krRnd).value.trim();
        const userId = document.getElementById("krDUsername" + krRnd).value.trim();
        const password = document.getElementById("krDPassword" + krRnd).value;
        const password2 = document.getElementById("krDPassword2" + krRnd).value;

        if (!nama || !email || !userId || !password) {
            alertBox.innerText = "<%=Common.getBahasaConfig("Semua kolom bertanda * harus diisi.")%>";
            alertBox.classList.remove("d-none");
            return;
        }
        if (password !== password2) {
            alertBox.innerText = "<%=Common.getBahasaConfig("Kata sandi dan ulangi kata sandi tidak sama.")%>";
            alertBox.classList.remove("d-none");
            return;
        }

        const btn = document.getElementById("krDaftarSubmitBtn" + krRnd);
        const oriBtnHtml = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Mendaftarkan...")%>';

        const r = await krApi("daftar_member_baru", { nama: nama, email: email, hp: hp, userId: userId, password: password });

        if (r.status === "success") {
            krToast(r.message || "<%=Common.getBahasaConfig("Pendaftaran berhasil.")%>", "success");
            bootstrap.Modal.getInstance(document.getElementById("krModalDaftar" + krRnd)).hide();
            setTimeout(() => {
                krBukaModalLogin();
                const loginUsername = document.getElementById("krLoginUsername" + krRnd);
                if (loginUsername) loginUsername.value = userId;
            }, 400);
            btn.disabled = false;
            btn.innerHTML = oriBtnHtml;
        } else {
            alertBox.innerText = r.message || "<%=Common.getBahasaConfig("Gagal mendaftar. Silakan coba lagi.")%>";
            alertBox.classList.remove("d-none");
            btn.disabled = false;
            btn.innerHTML = oriBtnHtml;
        }
    }

    async function krApi(action, params) {
        const body = new URLSearchParams(Object.assign({ action: action }, params || {}));
        try {
            const res = await fetch(krServiceUrl, { method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded" }, body: body });
            return await res.json();
        } catch (e) {
            console.error(e);
            return { status: "error", message: "Kesalahan koneksi." };
        }
    }
    async function krApiUpload(action, formData) {
        try {
            const res = await fetch(krServiceUrl + "&action=" + encodeURIComponent(action), { method: "POST", body: formData });
            return await res.json();
        } catch (e) {
            console.error(e);
            return { status: "error", message: "Kesalahan koneksi." };
        }
    }
    function krToast(msg, type) {
        if (typeof tampilkanToast === "function") { tampilkanToast(msg, type === "danger" ? "bg-danger" : (type === "warning" ? "bg-warning" : "bg-success")); return; }
        alert(msg);
    }
    function krStars(rating) {
        rating = Math.round(rating || 0);
        let s = "";
        for (let i = 1; i <= 5; i++) s += "<i class='" + (i <= rating ? "fas" : "far") + " fa-star'></i>";
        return s;
    }
    function krFormatRupiah(v) { return "Rp " + Math.round(v || 0).toLocaleString("id-ID"); }
    function krEsc(s) {
        if (s === null || s === undefined) return "";
        return String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#39;");
    }

    async function krLoadFilterOptions() {
        const rKat = await krApi("list_kategori", {});
        if (rKat.status === "success") {
            krDataKategori = rKat.data;
            const sel1 = document.getElementById("krFilterKategori" + krRnd);
            const sel2 = document.getElementById("krFKategori" + krRnd);
            rKat.data.forEach(k => {
                sel1.insertAdjacentHTML("beforeend", "<option value='" + k.id + "'>" + k.nama + "</option>");
                if (sel2) sel2.insertAdjacentHTML("beforeend", "<option value='" + k.id + "'>" + k.nama + "</option>");
            });
        }
        const rTing = await krApi("list_tingkat", {});
        if (rTing.status === "success") {
            krDataTingkat = rTing.data;
            const sel1 = document.getElementById("krFilterTingkat" + krRnd);
            const sel2 = document.getElementById("krFTingkat" + krRnd);
            rTing.data.forEach(t => {
                sel1.insertAdjacentHTML("beforeend", "<option value='" + t.id + "'>" + t.nama + "</option>");
                if (sel2) sel2.insertAdjacentHTML("beforeend", "<option value='" + t.id + "'>" + t.nama + "</option>");
            });
        }
    }

    function krDebounceFilter() {
        clearTimeout(krFilterTimer);
        krFilterTimer = setTimeout(krLoadKatalog, 350);
    }

    async function krLoadKatalog() {
        const grid = document.getElementById("krKatalogGrid" + krRnd);
        grid.innerHTML = '<div class="col-12 text-center py-5"><div class="spinner-border text-primary"></div></div>';
        const params = {
            q: document.getElementById("krSearch" + krRnd).value,
            kategoriId: document.getElementById("krFilterKategori" + krRnd).value,
            tingkatId: document.getElementById("krFilterTingkat" + krRnd).value,
            harga: document.getElementById("krFilterHarga" + krRnd).value
        };
        const r = await krApi("list_katalog", params);
        grid.innerHTML = "";
        if (r.status !== "success" || r.data.length === 0) {
            grid.innerHTML = '<div class="col-12"><div class="empty-state-' + krRnd + '"><i class="fas fa-folder-open"></i><p class="mb-0">Belum ada kursus yang tersedia.</p></div></div>';
            return;
        }
        r.data.forEach(item => {
            const harga = item.gratis ? "<span class='text-primary'>Gratis</span>" : krFormatRupiah(item.harga);
            const thumb = item.thumbnail ? "<img src='" + item.thumbnail + "'>" : "<i class='" + (item.icon ? item.icon.replace('pe-7s-', 'fas fa-') : 'fas fa-laptop-code') + "'></i>";
            const card =
                '<div class="col-md-6 col-lg-4">' +
                    '<div class="card course-card-' + krRnd + '">' +
                        '<div class="course-icon-wrapper-' + krRnd + '">' + thumb + '</div>' +
                        '<div class="course-body-' + krRnd + '">' +
                            '<span class="text-primary course-category-' + krRnd + '"><i class="fas fa-tag me-1"></i>' + krEsc(item.kategori || 'Umum') + '</span>' +
                            '<h5 class="course-title-' + krRnd + '">' + krEsc(item.nama) + '</h5>' +
                            '<div class="course-rating-' + krRnd + ' mb-2">' + krStars(item.rating) + ' <span class="text-muted small">(' + item.jumlahUlasan + ')</span></div>' +
                            '<div class="small text-muted mb-2"><i class="fas fa-user-tie me-1"></i>' + krEsc(item.instruktur || '-') + ' &middot; <i class="fas fa-users me-1"></i>' + item.jumlahSiswa + ' siswa</div>' +
                            '<div class="course-price-' + krRnd + ' d-flex justify-content-between align-items-center">' +
                                '<span>' + harga + '</span>' +
                                '<button class="btn btn-kr-outline-' + krRnd + ' btn-sm rounded-pill px-3" onclick="krBukaDetail(' + item.id + ')">Lihat Detail</button>' +
                            '</div>' +
                        '</div>' +
                    '</div>' +
                '</div>';
            grid.insertAdjacentHTML("beforeend", card);
        });
    }

    let krDetailData = null;
    async function krBukaDetail(id) {
        const r = await krApi("get_produk", { id: id });
        if (r.status !== "success") { krToast(r.message || "Gagal memuat detail.", "danger"); return; }
        krDetailData = r.data;
        document.getElementById("krDetailJudul" + krRnd).innerText = r.data.nama;
        document.getElementById("krDetailBody" + krRnd).innerHTML = krRenderDetailBody(r.data);
        new bootstrap.Modal(document.getElementById("krModalDetail" + krRnd)).show();
    }

    function krRenderDetailBody(d) {
        let kurikulumHtml = '<div class="accordion" id="krAccKurikulum' + krRnd + '">';
        d.kurikulum.forEach((s, si) => {
            kurikulumHtml += '<div class="accordion-item"><h2 class="accordion-header">' +
                '<button class="accordion-button ' + (si > 0 ? 'collapsed' : '') + '" type="button" data-bs-toggle="collapse" data-bs-target="#krSeksi' + krRnd + '_' + s.id + '">' +
                krEsc(s.judul) + ' <span class="text-muted small ms-2">(' + s.materi.length + ' materi)</span></button></h2>' +
                '<div id="krSeksi' + krRnd + '_' + s.id + '" class="accordion-collapse collapse ' + (si === 0 ? 'show' : '') + '" data-bs-parent="#krAccKurikulum' + krRnd + '"><div class="accordion-body p-0">';
            s.materi.forEach(m => {
                const bisaAkses = m.preview || d.statusBeli === "<%=PesertaPunyaProdukKursus.TERBELI%>";
                kurikulumHtml += '<div class="kr-materi-row-' + krRnd + '">' +
                    '<i class="' + krIkonTipeKonten(m.tipeKonten) + '"></i>' +
                    '<span class="flex-grow-1">' + krEsc(m.judul) + (m.preview ? ' <span class="badge bg-info ms-1">Preview</span>' : '') + '</span>' +
                    '<span class="text-muted small">' + (m.durasiMenit || 0) + ' mnt</span>' +
                    (bisaAkses ? '' : '<i class="fas fa-lock kr-lock-' + krRnd + '"></i>') +
                    '</div>';
            });
            kurikulumHtml += '</div></div></div>';
        });
        kurikulumHtml += '</div>';

        let reviewHtml = '<div class="mt-2">';
        if (d.ulasan.length === 0) reviewHtml += '<p class="text-muted small">Belum ada ulasan.</p>';
        d.ulasan.forEach(u => {
            reviewHtml += '<div class="border-bottom pb-2 mb-2"><div class="d-flex justify-content-between"><b>' + krEsc(u.nama) + '</b><span class="course-rating-' + krRnd + '">' + krStars(u.rating) + '</span></div>' +
                '<div class="small text-muted">' + krEsc(u.tanggal) + '</div><div>' + krEsc(u.komentar) + '</div></div>';
        });
        reviewHtml += '</div>';

        let ctaHtml = '';
        if (d.isOwner) {
            ctaHtml = '<span class="badge bg-secondary">Ini kursus Anda sendiri</span>';
        } else if (d.statusBeli === "<%=PesertaPunyaProdukKursus.TERBELI%>") {
            ctaHtml = '<button class="btn btn-kr-primary-' + krRnd + ' rounded-pill px-4 w-100" onclick="krMulaiBelajarDariDetail(' + d.id + ')"><i class="fas fa-play-circle me-2"></i>Lanjutkan Belajar</button>';
        } else if (d.statusBeli === "<%=PesertaPunyaProdukKursus.PESAN%>") {
            ctaHtml = '<span class="badge bg-warning text-dark">Menunggu pembayaran</span>';
        } else if (d.gratis) {
            ctaHtml = '<button class="btn btn-kr-primary-' + krRnd + ' rounded-pill px-4 w-100" onclick="krBeliKursus(' + d.id + ')"><i class="fas fa-unlock me-2"></i>Daftar Gratis</button>';
        } else {
            ctaHtml = '<div class="input-group input-group-sm mb-2">' +
                '<input type="text" class="form-control" id="krKuponInput' + krRnd + '" placeholder="Kode kupon (opsional)">' +
                '</div><button class="btn btn-kr-primary-' + krRnd + ' rounded-pill px-4 w-100" onclick="krBeliKursus(' + d.id + ')"><i class="fas fa-shopping-cart me-2"></i>Beli Sekarang</button>';
        }

        return '<div class="row g-4">' +
            '<div class="col-md-8">' +
                '<div class="d-flex flex-wrap gap-2 mb-2">' +
                    '<span class="badge bg-primary">' + krEsc(d.kategori || 'Umum') + '</span>' +
                    '<span class="badge bg-secondary">' + krEsc(d.tingkat || '-') + '</span>' +
                '</div>' +
                '<p class="text-muted">' + krEsc(d.deskripsi || d.keterangan || '') + '</p>' +
                '<h6 class="fw-bold mt-4">Kurikulum</h6>' + kurikulumHtml +
                '<h6 class="fw-bold mt-4">Ulasan (' + d.jumlahUlasan + ')</h6>' + reviewHtml +
            '</div>' +
            '<div class="col-md-4">' +
                '<div class="kr-soft-card-' + krRnd + ' p-3">' +
                    '<div class="mb-2"><i class="fas fa-user-tie me-2"></i><b>' + krEsc(d.instruktur || '-') + '</b></div>' +
                    '<div class="mb-2 course-rating-' + krRnd + '">' + krStars(d.rating) + ' <span class="text-muted small">(' + d.jumlahUlasan + ' ulasan, ' + d.jumlahSiswa + ' siswa)</span></div>' +
                    '<h3 class="fw-bold ' + (d.gratis ? 'text-primary' : 'text-success') + '">' + (d.gratis ? 'Gratis' : krFormatRupiah(d.harga)) + '</h3>' +
                    '<div id="krCheckoutInfo' + krRnd + '" class="mt-2"></div>' +
                    '<div class="mt-2">' + ctaHtml + '</div>' +
                '</div>' +
            '</div>' +
        '</div>';
    }

    async function krBeliKursus(produkKursusId) {
        if (!krIsLogin) { krToast("Silakan masuk terlebih dahulu.", "warning"); return; }
        const kuponEl = document.getElementById("krKuponInput" + krRnd);
        const r = await krApi("beli_kursus", { produkKursusId: produkKursusId, kodeKupon: kuponEl ? kuponEl.value : "" });
        if (r.status !== "success") { krToast(r.message || "Gagal memproses pembelian.", "danger"); return; }
        if (r.gratis) {
            krToast(r.message || "Berhasil!", "success");
            bootstrap.Modal.getInstance(document.getElementById("krModalDetail" + krRnd)).hide();
        } else {
            document.getElementById("krCheckoutInfo" + krRnd).innerHTML =
                '<div class="alert alert-info mt-2"><b>Bayar via VA BNI</b><br>No. VA: <b>' + r.va + '</b><br>Total: ' + krFormatRupiah(r.total) +
                '<br>Kadaluwarsa: ' + r.kadaluwarsa + '<br>' +
                '<button class="btn btn-sm btn-outline-primary mt-2" onclick="krCekStatus(' + r.enrollmentId + ')">Cek Status Pembayaran</button></div>';
        }
    }
    async function krCekStatus(enrollmentId) {
        const r = await krApi("cek_status_pesanan", { enrollmentId: enrollmentId });
        if (r.status === "success") krToast("Status pesanan: " + r.statusPesanan, r.statusPesanan === "<%=PesertaPunyaProdukKursus.TERBELI%>" ? "success" : "warning");
    }

    function krMulaiBelajarDariDetail(produkKursusId) {
        bootstrap.Modal.getInstance(document.getElementById("krModalDetail" + krRnd)).hide();
        const tabTrigger = new bootstrap.Tab(document.querySelector("#belajar-tab-" + krRnd));
        tabTrigger.show();
        setTimeout(() => krBukaPlayer(produkKursusId), 300);
    }

    /* =================== KURSUS SAYA (BELAJAR) =================== */
    async function krLoadKursusSaya() {
        if (!krIsLogin) return;
        document.getElementById("krPlayerWrap" + krRnd).style.display = "none";
        document.getElementById("krBelajarWrap" + krRnd).style.display = "block";
        const grid = document.getElementById("krBelajarGrid" + krRnd);
        const r = await krApi("list_kursus_saya_belajar", {});
        grid.innerHTML = "";
        if (r.status !== "success" || r.data.length === 0) {
            grid.innerHTML = '<div class="col-12"><div class="empty-state-' + krRnd + '"><i class="fas fa-box-open"></i><h5 class="fw-bold text-dark">Belum Ada Kursus</h5><p class="mb-0">Jelajahi katalog untuk mulai belajar.</p></div></div>';
            return;
        }
        r.data.forEach(item => {
            let badge = '<span class="badge bg-warning text-dark">Menunggu Pembayaran</span>';
            if (item.status === "<%=PesertaPunyaProdukKursus.TERBELI%>") badge = '<span class="badge bg-success">Aktif</span>';
            else if (item.status === "<%=PesertaPunyaProdukKursus.BATAL%>") badge = '<span class="badge bg-danger">Dibatalkan</span>';
            const disabledAttr = item.status === "<%=PesertaPunyaProdukKursus.TERBELI%>" ? "" : "disabled";
            const thumb = item.thumbnail ? "<img src='" + item.thumbnail + "'>" : "<i class='fas fa-laptop-code'></i>";
            grid.insertAdjacentHTML("beforeend",
                '<div class="col-md-6 col-lg-4"><div class="card course-card-' + krRnd + ' border shadow-sm">' +
                '<div class="course-icon-wrapper-' + krRnd + '">' + thumb + '</div>' +
                '<div class="course-body-' + krRnd + '">' +
                '<div class="d-flex justify-content-between mb-2"><span class="text-secondary small fw-bold">' + krEsc(item.kategori || '') + '</span>' + badge + '</div>' +
                '<h5 class="course-title-' + krRnd + ' mb-3">' + krEsc(item.nama) + '</h5>' +
                '<div class="mt-auto pt-3 border-top d-grid"><button class="btn btn-kr-primary-' + krRnd + ' btn-sm rounded-pill" ' + disabledAttr + ' onclick="krBukaPlayer(' + item.produkKursusId + ')">' +
                '<i class="fas fa-play-circle me-2"></i>Mulai Belajar</button></div></div></div></div>');
        });
    }

    let krPlayerData = null;
    let krModulAktifIdx = null;
    let krMateriAktifIdx = null;

    function krIkonTipeKonten(tipe) {
        if (tipe === "Video") return "fas fa-circle-play text-primary";
        if (tipe === "Tugas") return "fas fa-file-arrow-up";
        if (tipe === "Quiz") return "fas fa-circle-question text-warning";
        return "fas fa-file-lines text-info";
    }

    async function krBukaPlayer(produkKursusId) {
        const r = await krApi("get_pembelajaran", { produkKursusId: produkKursusId });
        if (r.status !== "success") { krToast(r.message || "Gagal memuat kursus.", "danger"); return; }
        krPlayerData = r.data;
        krModulAktifIdx = null;
        krMateriAktifIdx = null;
        document.getElementById("krBelajarWrap" + krRnd).style.display = "none";
        const wrap = document.getElementById("krPlayerWrap" + krRnd);
        wrap.style.display = "block";
        krRenderPlayerShell();
        krRenderModulGrid();
        document.getElementById("krPlayerContent" + krRnd).innerHTML =
            '<div class="empty-state-' + krRnd + ' mb-0"><i class="fas fa-hand-pointer"></i><p class="mb-0">Pilih salah satu modul di atas untuk mulai belajar.</p></div>';
    }

    function krRenderPlayerShell() {
        const wrap = document.getElementById("krPlayerWrap" + krRnd);
        wrap.innerHTML =
            '<div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">' +
                '<h5 class="fw-bold mb-0">' + krEsc(krPlayerData.nama) + '</h5>' +
                '<button class="btn btn-sm btn-outline-secondary rounded-pill" onclick="krTutupPlayer()"><i class="fas fa-arrow-left me-1"></i>Kembali</button>' +
            '</div>' +
            '<div class="d-flex align-items-center gap-2 mb-4">' +
                '<div class="progress flex-grow-1" style="height:10px;"><div class="progress-bar bg-success" style="width:' + krPlayerData.progressPersen + '%"></div></div>' +
                '<span class="small fw-semibold text-muted">' + krPlayerData.progressPersen + '%</span>' +
                (krPlayerData.sertifikat && krPlayerData.sertifikat.ada ?
                    '<button class="btn btn-sm btn-kr-primary-' + krRnd + ' rounded-pill px-3" onclick="krUnduhSertifikat()"><i class="fas fa-certificate me-1"></i>Unduh Sertifikat</button>' : '') +
            '</div>' +
            '<div class="row g-3 mb-4" id="krModulGrid' + krRnd + '"></div>' +
            '<div id="krPlayerContent' + krRnd + '"></div>' +
            '<div class="kr-soft-card-' + krRnd + ' p-3 mt-3"><h6 class="fw-bold">Beri Ulasan</h6>' +
                '<select class="form-select form-select-sm mb-2" id="krUlasanRating' + krRnd + '" style="max-width:180px;">' +
                    '<option value="5">5 - Sangat Bagus</option><option value="4">4 - Bagus</option><option value="3">3 - Cukup</option><option value="2">2 - Kurang</option><option value="1">1 - Buruk</option>' +
                '</select>' +
                '<textarea class="form-control form-control-sm mb-2" id="krUlasanKomentar' + krRnd + '" placeholder="Bagikan pendapat Anda..." rows="2"></textarea>' +
                '<button class="btn btn-sm btn-kr-primary-' + krRnd + ' rounded-pill px-3" onclick="krKirimUlasan(' + krPlayerData.produkKursusId + ')">Kirim Ulasan</button>' +
            '</div>';
    }

    function krRenderModulGrid() {
        const grid = document.getElementById("krModulGrid" + krRnd);
        grid.innerHTML = "";
        krPlayerData.kurikulum.forEach((s, idx) => {
            const done = s.progressPersen >= 100;
            grid.insertAdjacentHTML("beforeend",
                '<div class="col-sm-6 col-lg-4">' +
                    '<div class="kr-modul-card-' + krRnd + '" onclick="krBukaModulModal(' + idx + ')">' +
                        '<div class="d-flex justify-content-between align-items-start mb-2">' +
                            '<span class="fw-bold">' + krEsc(s.judul) + '</span>' +
                            '<span class="kr-modul-badge-' + krRnd + ' ' + (done ? "done" : "") + '">' + s.progressPersen + '%</span>' +
                        '</div>' +
                        '<div class="small text-muted">' + s.materi.length + ' item &middot; ' + (done ? "Selesai" : "Sedang berjalan") + '</div>' +
                    '</div>' +
                '</div>');
        });
    }

    function krTutupPlayer() {
        document.getElementById("krPlayerWrap" + krRnd).style.display = "none";
        document.getElementById("krBelajarWrap" + krRnd).style.display = "block";
        krLoadKursusSaya();
    }

    function krUnduhSertifikat() {
        window.open(krServiceUrl.replace("s=_kursus_service", "s=sertifikat_cetak") + "&enrollmentId=" + krPlayerData.enrollmentId, "_blank");
    }

    function krBukaModulModal(seksiIdx) {
        krModulAktifIdx = seksiIdx;
        const s = krPlayerData.kurikulum[seksiIdx];
        document.getElementById("krModulModalJudul" + krRnd).innerText = s.judul;
        const body = document.getElementById("krModulModalBody" + krRnd);
        body.innerHTML = "";
        s.materi.forEach((m, midx) => {
            const statusPill = m.selesai
                ? '<span class="badge rounded-pill" style="background:var(--kr-success-light);color:var(--kr-success);"><i class="fas fa-check me-1"></i>Selesai</span>'
                : '<span class="badge rounded-pill bg-light text-muted border">Belum</span>';
            body.insertAdjacentHTML("beforeend",
                '<div class="kr-modul-item-' + krRnd + '" onclick="krBukaMateriDariModal(' + seksiIdx + ',' + midx + ')">' +
                    '<i class="' + krIkonTipeKonten(m.tipeKonten) + '"></i>' +
                    '<span class="flex-grow-1">' + krEsc(m.judul) + '</span>' +
                    statusPill +
                '</div>');
        });
        new bootstrap.Modal(document.getElementById("krModalModulPembelajaran" + krRnd)).show();
    }

    function krBukaMateriDariModal(seksiIdx, materiIdx) {
        bootstrap.Modal.getInstance(document.getElementById("krModalModulPembelajaran" + krRnd)).hide();
        krModulAktifIdx = seksiIdx;
        krMateriAktifIdx = materiIdx;
        krRenderMateriAktif();
        setTimeout(() => {
            const target = document.getElementById("krPlayerContent" + krRnd);
            if (target) target.scrollIntoView({ behavior: "smooth", block: "start" });
        }, 250);
    }
    function krRenderMateriAktif() {
        if (krModulAktifIdx == null || krMateriAktifIdx == null) return;
        krHentikanHeartbeatVideo();
        const m = krPlayerData.kurikulum[krModulAktifIdx].materi[krMateriAktifIdx];
        if (m.tipeKonten === "Tugas") krRenderTugasMateri(m);
        else if (m.tipeKonten === "Quiz") krRenderKuisAwal(m);
        else krRenderKontenMateri(m);
    }
    /* Ambil ulang data pembelajaran (progress terbaru) tanpa mereset tampilan ke placeholder. */
    async function krSegarkanPlayer() {
        const r = await krApi("get_pembelajaran", { produkKursusId: krPlayerData.produkKursusId });
        if (r.status !== "success") return;
        krPlayerData = r.data;
        krRenderPlayerShell();
        krRenderModulGrid();
        if (krModulAktifIdx != null && krMateriAktifIdx != null) {
            krRenderMateriAktif();
        } else {
            document.getElementById("krPlayerContent" + krRnd).innerHTML =
                '<div class="empty-state-' + krRnd + ' mb-0"><i class="fas fa-hand-pointer"></i><p class="mb-0">Pilih salah satu modul di atas untuk mulai belajar.</p></div>';
        }
    }

    /* =================== TRACKING PROGRESS (§13) =================== */
    let krVideoHeartbeatHandle = null;
    function krHentikanHeartbeatVideo() {
        if (krVideoHeartbeatHandle) { clearInterval(krVideoHeartbeatHandle); krVideoHeartbeatHandle = null; }
    }

    function krRenderKontenMateri(m) {
        let contentHtml = "";
        const videoId = "krVideoPlayer" + krRnd;
        if (m.tipeKonten === "Video" && m.video) {
            contentHtml = '<video id="' + videoId + '" src="' + m.video + '" controls style="width:100%;max-height:420px;background:#000;border-radius:.75rem;"></video>';
        } else if (m.video) {
            contentHtml = '<a href="' + m.video + '" target="_blank" class="btn btn-kr-outline-' + krRnd + ' rounded-pill"><i class="fas fa-download me-2"></i>Buka Materi</a>';
        } else {
            contentHtml = '<p class="text-muted mb-0">Konten belum tersedia.</p>';
        }
        const progress = m.progress || { persentase: 0 };
        document.getElementById("krPlayerContent" + krRnd).innerHTML =
            '<div class="kr-soft-card-' + krRnd + ' p-3 mb-3">' +
                '<div class="d-flex justify-content-between align-items-start mb-2">' +
                    '<h6 class="fw-bold mb-0"><i class="' + krIkonTipeKonten(m.tipeKonten) + ' me-2"></i>' + krEsc(m.judul) + '</h6>' +
                    (m.selesai ? '<span class="badge rounded-pill" style="background:var(--kr-success-light);color:var(--kr-success);"><i class="fas fa-check me-1"></i>Selesai</span>' : '') +
                '</div>' +
                (m.keterangan ? '<p class="text-muted small">' + krEsc(m.keterangan) + '</p>' : '') +
                (m.tipeKonten === "Video" && progress.persentase > 0 && !m.selesai ?
                    '<div class="progress mb-2" style="height:6px;"><div class="progress-bar" style="width:' + progress.persentase + '%;background:var(--kr-primary);"></div></div>' : '') +
                '<div class="mb-3">' + contentHtml + '</div>' +
                '<button class="btn btn-kr-primary-' + krRnd + ' btn-sm rounded-pill px-3" onclick="krTandaiSelesai(' + m.id + ')"><i class="fas fa-check me-1"></i>Tandai Selesai</button>' +
            '</div>';

        /* Akses tercatat sekali per pembukaan materi (bukan tiap detik) -- jumlahAkses & waktuMulai/terakhir. */
        krApi("mulai_lihat_materi", { enrollmentId: krPlayerData.enrollmentId, materiKursusId: m.id });

        if (m.tipeKonten === "Video" && m.video) {
            const video = document.getElementById(videoId);
            const resumeDetik = progress.detikVideoTerakhir || 0;
            if (resumeDetik > 0) {
                video.addEventListener("loadedmetadata", function () {
                    if (resumeDetik < video.duration - 5) video.currentTime = resumeDetik;
                }, { once: true });
            }
            const kirimHeartbeat = function () {
                if (!video.isConnected) { krHentikanHeartbeatVideo(); return; }
                krApi("heartbeat_progress", {
                    enrollmentId: krPlayerData.enrollmentId, materiKursusId: m.id,
                    detikSaatIni: Math.floor(video.currentTime || 0)
                });
            };
            /* Heartbeat periodik (bukan per-buka), plus simpan langsung saat jeda/selesai. */
            video.addEventListener("pause", kirimHeartbeat);
            video.addEventListener("ended", kirimHeartbeat);
            krVideoHeartbeatHandle = setInterval(kirimHeartbeat, 20000);
        }
    }

    function krRenderTugasMateri(m) {
        const p = m.pengumpulan || { ada: false };
        let statusHtml = "";
        if (p.ada) {
            statusHtml =
                '<table class="table table-sm mb-3"><tbody>' +
                    '<tr><td class="text-muted" style="width:180px;">Berkas Terkirim</td><td><a href="' + p.link + '" target="_blank">' + krEsc(p.namaFile) + '</a></td></tr>' +
                    '<tr><td class="text-muted">Waktu Kumpul</td><td>' + krEsc(p.waktuKumpul) + '</td></tr>' +
                    '<tr><td class="text-muted">Status</td><td><span class="badge" style="background:' + (p.status === "Dinilai" ? "var(--kr-success)" : "var(--kr-accent)") + ';">' + krEsc(p.status) + '</span></td></tr>' +
                    (p.status === "Dinilai" ? '<tr><td class="text-muted">Nilai</td><td class="fw-bold">' + (p.nilai == null ? "-" : p.nilai) + '</td></tr>' : '') +
                    (p.status === "Dinilai" && p.catatanPenilaian ? '<tr><td class="text-muted">Catatan</td><td>' + krEsc(p.catatanPenilaian) + '</td></tr>' : '') +
                '</tbody></table>' +
                '<button class="btn btn-outline-danger btn-sm rounded-pill" onclick="krHapusPengumpulan(' + p.id + ')"><i class="fas fa-trash me-1"></i>Hapus Pengajuan</button>';
        }
        document.getElementById("krPlayerContent" + krRnd).innerHTML =
            '<div class="kr-soft-card-' + krRnd + ' p-3 mb-3">' +
                '<div class="d-flex justify-content-between align-items-start mb-2">' +
                    '<h6 class="fw-bold mb-0"><i class="' + krIkonTipeKonten(m.tipeKonten) + ' me-2"></i>' + krEsc(m.judul) + '</h6>' +
                    (m.selesai ? '<span class="badge rounded-pill" style="background:var(--kr-success-light);color:var(--kr-success);"><i class="fas fa-check me-1"></i>Selesai</span>' : '') +
                '</div>' +
                (m.keterangan ? '<div class="alert alert-info small">' + krEsc(m.keterangan) + '</div>' : '') +
                statusHtml +
                '<div class="input-group">' +
                    '<input type="file" class="form-control" id="krTugasFile' + krRnd + '">' +
                    '<button class="btn btn-kr-primary-' + krRnd + '" onclick="krKirimTugas(' + m.id + ')"><i class="fas fa-paper-plane me-2"></i>' + (p.ada ? "Kirim Ulang" : "Kirim Tugas") + '</button>' +
                '</div>' +
            '</div>';
    }

    /* =================== KUIS (PESERTA) =================== */
    let krKuisTimerHandle = null;
    let krKuisPercobaanAktif = null;

    async function krRenderKuisAwal(m) {
        if (krKuisTimerHandle) { clearInterval(krKuisTimerHandle); krKuisTimerHandle = null; }
        const rh = await krApi("get_riwayat_kuis", { materiKursusId: m.id, enrollmentId: krPlayerData.enrollmentId });
        let riwayatHtml = "";
        if (rh.status === "success" && rh.data.length > 0) {
            riwayatHtml = '<table class="table table-sm mb-3"><thead><tr><th>Percobaan</th><th>Nilai</th><th>Status</th></tr></thead><tbody>';
            rh.data.forEach(p => {
                const statusBadge = p.status === "Berlangsung" ? '<span class="badge bg-warning text-dark">Belum Selesai</span>'
                    : (p.lulus ? '<span class="badge" style="background:var(--kr-success);">Lulus</span>' : '<span class="badge bg-danger">Belum Lulus</span>');
                riwayatHtml += '<tr><td>#' + p.nomorPercobaan + '</td><td>' + p.totalNilai + '</td><td>' + statusBadge + '</td></tr>';
            });
            riwayatHtml += '</tbody></table>';
        }
        document.getElementById("krPlayerContent" + krRnd).innerHTML =
            '<div class="kr-soft-card-' + krRnd + ' p-3 mb-3">' +
                '<div class="d-flex justify-content-between align-items-start mb-2">' +
                    '<h6 class="fw-bold mb-0"><i class="' + krIkonTipeKonten(m.tipeKonten) + ' me-2"></i>' + krEsc(m.judul) + '</h6>' +
                    (m.selesai ? '<span class="badge rounded-pill" style="background:var(--kr-success-light);color:var(--kr-success);"><i class="fas fa-check me-1"></i>Selesai</span>' : '') +
                '</div>' +
                (m.keterangan ? '<p class="text-muted small">' + krEsc(m.keterangan) + '</p>' : '') +
                riwayatHtml +
                '<button class="btn btn-kr-primary-' + krRnd + ' rounded-pill px-4" onclick="krMulaiKuis(' + m.id + ')"><i class="fas fa-play me-2"></i>Mulai Kuis</button>' +
            '</div>';
    }

    async function krMulaiKuis(materiId) {
        const r = await krApi("mulai_kuis", { materiKursusId: materiId, enrollmentId: krPlayerData.enrollmentId });
        if (r.status !== "success") { krToast(r.message || "Gagal memulai kuis.", "danger"); return; }
        krKuisPercobaanAktif = { percobaanId: r.percobaanId, jawabanTerpilih: {} };
        krRenderSoalKuis(r);
    }

    function krRenderSoalKuis(r) {
        let soalHtml = "";
        r.soal.forEach((soal, idx) => {
            soalHtml += '<div class="kr-soft-card-' + krRnd + ' p-3 mb-2">' +
                '<div class="fw-semibold mb-2">' + (idx + 1) + '. ' + krEsc(soal.soal) + '</div>';
            if (soal.pilihan) {
                soal.pilihan.forEach(p => {
                    soalHtml += '<div class="form-check mb-1">' +
                        '<input class="form-check-input" type="radio" name="krJawab' + krRnd + '_' + soal.id + '" id="krJawab' + krRnd + '_' + soal.id + '_' + p.id + '" onchange="krJawabSoalKuis(' + soal.id + ', ' + p.id + ')">' +
                        '<label class="form-check-label" for="krJawab' + krRnd + '_' + soal.id + '_' + p.id + '">' + krEsc(p.jawaban) + '</label>' +
                    '</div>';
                });
            } else {
                soalHtml += '<textarea class="form-control" rows="3" placeholder="Tulis jawaban Anda..." onblur="krJawabSoalKuisEsai(' + soal.id + ', this.value)"></textarea>';
            }
            soalHtml += '</div>';
        });
        document.getElementById("krPlayerContent" + krRnd).innerHTML =
            '<div class="d-flex justify-content-between align-items-center mb-3">' +
                '<h6 class="fw-bold mb-0">Mengerjakan Kuis</h6>' +
                '<span id="krKuisTimer' + krRnd + '" class="badge bg-dark"></span>' +
            '</div>' +
            soalHtml +
            '<button class="btn btn-kr-primary-' + krRnd + ' rounded-pill px-4" onclick="krKumpulkanKuis()"><i class="fas fa-flag-checkered me-2"></i>Kumpulkan Kuis</button>';

        if (r.batasWaktuMenit) {
            const batasMs = r.batasWaktuMenit * 60 * 1000;
            const deadline = r.waktuMulaiMs + batasMs;
            const timerEl = document.getElementById("krKuisTimer" + krRnd);
            krKuisTimerHandle = setInterval(() => {
                const sisaMs = deadline - Date.now();
                if (sisaMs <= 0) {
                    clearInterval(krKuisTimerHandle);
                    krKuisTimerHandle = null;
                    krToast("Waktu habis, kuis otomatis dikumpulkan.", "warning");
                    krKumpulkanKuis();
                    return;
                }
                const menit = Math.floor(sisaMs / 60000);
                const detik = Math.floor((sisaMs % 60000) / 1000);
                if (timerEl) timerEl.innerText = "Sisa waktu: " + menit + ":" + (detik < 10 ? "0" : "") + detik;
            }, 1000);
        }
    }

    async function krJawabSoalKuis(soalId, bankSoalDetailId) {
        await krApi("jawab_soal_kuis", { percobaanId: krKuisPercobaanAktif.percobaanId, soalId: soalId, bankSoalDetailId: bankSoalDetailId });
    }
    async function krJawabSoalKuisEsai(soalId, jawabanEsai) {
        if (!jawabanEsai.trim()) return;
        await krApi("jawab_soal_kuis", { percobaanId: krKuisPercobaanAktif.percobaanId, soalId: soalId, jawabanEsai: jawabanEsai });
    }
    async function krKumpulkanKuis() {
        if (krKuisTimerHandle) { clearInterval(krKuisTimerHandle); krKuisTimerHandle = null; }
        const r = await krApi("selesai_kuis", { percobaanId: krKuisPercobaanAktif.percobaanId });
        if (r.status !== "success") { krToast(r.message || "Gagal mengumpulkan kuis.", "danger"); return; }
        const lulusHtml = r.data.lulus === true ? '<span class="badge" style="background:var(--kr-success);font-size:1rem;">LULUS</span>'
            : r.data.lulus === false ? '<span class="badge bg-danger" style="font-size:1rem;">BELUM LULUS</span>' : '';
        document.getElementById("krPlayerContent" + krRnd).innerHTML =
            '<div class="kr-soft-card-' + krRnd + ' p-4 text-center">' +
                '<h5 class="fw-bold mb-3">Hasil Kuis</h5>' +
                '<div class="display-5 fw-bold mb-2">' + r.data.totalNilai + '</div>' +
                '<div class="mb-3">' + lulusHtml + '</div>' +
                '<p class="text-muted">Benar ' + r.data.jumlahBenar + ' dari ' + r.data.jumlahSoal + ' soal.</p>' +
                '<button class="btn btn-kr-outline-' + krRnd + ' rounded-pill px-4" onclick="krSegarkanPlayer()"><i class="fas fa-arrow-left me-2"></i>Kembali</button>' +
            '</div>';
        krToast("Kuis selesai dikumpulkan.", "success");
    }

    async function krTandaiSelesai(materiId) {
        const r = await krApi("tandai_selesai", { enrollmentId: krPlayerData.enrollmentId, materiKursusId: materiId });
        if (r.status === "success") { krToast("Materi ditandai selesai.", "success"); await krSegarkanPlayer(); }
        else krToast(r.message || "Gagal menandai selesai.", "danger");
    }
    async function krKirimTugas(materiId) {
        const fileInput = document.getElementById("krTugasFile" + krRnd);
        if (!fileInput.files || !fileInput.files[0]) { krToast("Pilih berkas tugas terlebih dahulu.", "warning"); return; }
        const fd = new FormData();
        fd.append("enrollmentId", krPlayerData.enrollmentId);
        fd.append("materiKursusId", materiId);
        fd.append("file", fileInput.files[0]);
        const r = await krApiUpload("upload_tugas", fd);
        krToast(r.message || (r.status === "success" ? "Terkirim." : "Gagal mengirim tugas."), r.status === "success" ? "success" : "danger");
        if (r.status === "success") await krSegarkanPlayer();
    }
    async function krHapusPengumpulan(id) {
        if (!confirm("Hapus pengajuan tugas ini?")) return;
        const r = await krApi("hapus_pengumpulan_tugas", { id: id });
        krToast(r.message || (r.status === "success" ? "Terhapus." : "Gagal."), r.status === "success" ? "success" : "danger");
        if (r.status === "success") await krSegarkanPlayer();
    }
    async function krKirimUlasan(produkKursusId) {
        const rating = document.getElementById("krUlasanRating" + krRnd).value;
        const komentar = document.getElementById("krUlasanKomentar" + krRnd).value;
        const r = await krApi("simpan_ulasan", { produkKursusId: produkKursusId, rating: rating, komentar: komentar });
        krToast(r.message || (r.status === "success" ? "Terima kasih!" : "Gagal mengirim ulasan."), r.status === "success" ? "success" : "danger");
    }

    /* =================== INSTRUKTUR =================== */
    async function krLoadInstruktur() {
        if (!krIsLogin) return;
        const r = await krApi("list_kursus_saya_instruktur", {});
        if (r.status !== "success") return;
        const stat = r.stat;
        document.getElementById("krStatCards" + krRnd).innerHTML =
            krStatCard("Draft/Ditolak", stat.draft, "fa-file-pen", "#64748b") + krStatCard("Menunggu Review", stat.pending, "fa-hourglass-half", "#f59e0b") +
            krStatCard("Published", stat.published, "fa-circle-check", "#16a34a") + krStatCard("Total Siswa", stat.totalSiswa, "fa-users", "#4f46e5") +
            krStatCard("Estimasi Pendapatan", krFormatRupiah(stat.pendapatan), "fa-sack-dollar", "#7c3aed");

        const body = document.getElementById("krKursusInstrukturBody" + krRnd);
        body.innerHTML = "";
        if (r.data.length === 0) { body.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">Belum ada kursus. Buat kursus pertama Anda!</td></tr>'; return; }
        r.data.forEach(p => {
            let badge = '<span class="badge bg-secondary">Draft</span>';
            if (p.status === "Pending Review") badge = '<span class="badge" style="background:var(--kr-accent);">Menunggu Review</span>';
            else if (p.status === "Published") badge = '<span class="badge" style="background:var(--kr-success);">Published</span>';
            else if (p.status === "Rejected") badge = '<span class="badge" style="background:var(--kr-danger);">Ditolak</span>';
            const thumb = p.thumbnail ? "<img src='" + p.thumbnail + "' style='width:56px;height:40px;object-fit:cover;border-radius:8px;'>" : "<div class='rounded d-flex align-items-center justify-content-center' style='width:56px;height:40px;background:var(--kr-primary-light);color:var(--kr-primary);'><i class='fas fa-image small'></i></div>";
            let aksi = '<div class="btn-group btn-group-sm">' +
                '<button class="btn btn-kr-outline-' + krRnd + '" onclick=\'krBukaFormProduk(' + JSON.stringify(p).replace(/'/g, "&#39;") + ')\' title="Edit"><i class="fas fa-edit"></i></button>' +
                '<button class="btn btn-kr-outline-' + krRnd + '" onclick="krBukaKurikulum(' + p.id + ')" title="Kurikulum"><i class="fas fa-list"></i></button>';
            if (p.status === "Draft" || p.status === "Rejected") {
                aksi += '<button class="btn btn-outline-success" onclick="krAjukanPublikasi(' + p.id + ')" title="Ajukan Publikasi"><i class="fas fa-paper-plane"></i></button>' +
                    '<button class="btn btn-outline-danger" onclick="krHapusProduk(' + p.id + ')" title="Hapus"><i class="fas fa-trash"></i></button>';
            }
            aksi += '</div>';
            body.insertAdjacentHTML("beforeend",
                '<tr><td>' + thumb + '</td><td class="fw-semibold">' + krEsc(p.nama) + '</td><td>' + badge + '</td>' +
                '<td>' + (p.gratis ? "Gratis" : krFormatRupiah(p.harga)) + '</td><td>' + p.jumlahSiswa + '</td><td>' + aksi + '</td></tr>');
        });
    }
    function krStatCard(label, value, icon, color) {
        return '<div class="col-6 col-md-4 col-xl">' +
            '<div class="kr-stat-card-' + krRnd + '" style="border-left-color:' + color + ';">' +
                '<div class="d-flex align-items-center gap-2 mb-1">' +
                    '<i class="fas ' + icon + '" style="color:' + color + ';"></i>' +
                    '<div class="small text-muted">' + label + '</div>' +
                '</div>' +
                '<div class="h4 fw-bold mb-0">' + value + '</div>' +
            '</div>' +
        '</div>';
    }

    function krBukaFormProduk(p) {
        document.getElementById("krFId" + krRnd).value = p ? p.id : "";
        document.getElementById("krFormJudulModal" + krRnd).innerText = p ? "Ubah Kursus" : "Kursus Baru";
        document.getElementById("krFNama" + krRnd).value = p ? p.nama : "";
        document.getElementById("krFGratis" + krRnd).checked = p ? !!p.gratis : false;
        document.getElementById("krFHarga" + krRnd).value = p ? (p.harga || 0) : 0;
        document.getElementById("krFHargaWrap" + krRnd).style.display = (p && p.gratis) ? "none" : "block";
        document.getElementById("krFKeterangan" + krRnd).value = "";
        document.getElementById("krFDeskripsi" + krRnd).value = "";
        document.getElementById("krFKategori" + krRnd).value = "";
        document.getElementById("krFTingkat" + krRnd).value = "";
        document.getElementById("krFThumbnail" + krRnd).value = "";
        if (p) {
            krApi("get_produk", { id: p.id }).then(r => {
                if (r.status === "success") {
                    document.getElementById("krFKeterangan" + krRnd).value = r.data.keterangan || "";
                    document.getElementById("krFDeskripsi" + krRnd).value = r.data.deskripsi || "";
                    if (r.data.kategoriId) document.getElementById("krFKategori" + krRnd).value = r.data.kategoriId;
                }
            });
        }
        new bootstrap.Modal(document.getElementById("krModalForm" + krRnd)).show();
    }
    async function krSimpanProduk() {
        const id = document.getElementById("krFId" + krRnd).value;
        const nama = document.getElementById("krFNama" + krRnd).value.trim();
        if (!nama) { krToast("Judul kursus harus diisi.", "warning"); return; }
        const params = {
            id: id, nama: nama,
            keterangan: document.getElementById("krFKeterangan" + krRnd).value,
            deskripsi: document.getElementById("krFDeskripsi" + krRnd).value,
            kategoriId: document.getElementById("krFKategori" + krRnd).value,
            tingkatId: document.getElementById("krFTingkat" + krRnd).value,
            gratis: document.getElementById("krFGratis" + krRnd).checked,
            harga: document.getElementById("krFHarga" + krRnd).value
        };
        const r = await krApi("simpan_produk", params);
        if (r.status !== "success") { krToast(r.message || "Gagal menyimpan.", "danger"); return; }

        const fileInput = document.getElementById("krFThumbnail" + krRnd);
        if (fileInput.files && fileInput.files[0]) {
            const fd = new FormData();
            fd.append("id", r.id);
            fd.append("file", fileInput.files[0]);
            await krApiUpload("upload_thumbnail", fd);
        }
        krToast(r.message || "Tersimpan.", "success");
        bootstrap.Modal.getInstance(document.getElementById("krModalForm" + krRnd)).hide();
        krLoadInstruktur();
    }
    async function krAjukanPublikasi(id) {
        const r = await krApi("ajukan_publikasi", { id: id });
        krToast(r.message || (r.status === "success" ? "Diajukan." : "Gagal."), r.status === "success" ? "success" : "danger");
        krLoadInstruktur();
    }
    async function krHapusProduk(id) {
        if (!confirm("Hapus kursus ini beserta seluruh kurikulumnya?")) return;
        const r = await krApi("hapus_produk", { id: id });
        krToast(r.message || (r.status === "success" ? "Terhapus." : "Gagal."), r.status === "success" ? "success" : "danger");
        krLoadInstruktur();
    }

    /* =================== KURIKULUM EDITOR =================== */
    async function krBukaKurikulum(produkKursusId) {
        document.getElementById("krKurProdukId" + krRnd).value = produkKursusId;
        await krRenderKurikulum();
        new bootstrap.Modal(document.getElementById("krModalKurikulum" + krRnd)).show();
    }
    let krKurikulumDataInstruktur = [];
    async function krRenderKurikulum() {
        const produkKursusId = document.getElementById("krKurProdukId" + krRnd).value;
        const r = await krApi("get_kurikulum", { produkKursusId: produkKursusId });
        const list = document.getElementById("krKurikulumList" + krRnd);
        if (r.status !== "success") { list.innerHTML = '<p class="text-danger">' + (r.message || "Gagal memuat.") + '</p>'; return; }
        krKurikulumDataInstruktur = r.data;
        list.innerHTML = "";
        r.data.forEach(s => {
            let materiHtml = "";
            s.materi.forEach(m => {
                materiHtml += '<div class="kr-materi-row-' + krRnd + '">' +
                    '<i class="fas fa-grip-vertical text-muted"></i>' +
                    '<span class="flex-grow-1">' + krEsc(m.judul) + ' <span class="badge bg-light text-dark">' + krEsc(m.tipeKonten) + '</span>' + (m.preview ? ' <span class="badge bg-info">Preview</span>' : '') + '</span>' +
                    (m.tipeKonten === "Quiz" ? '<button class="btn btn-sm btn-kr-outline-' + krRnd + '" onclick="krBukaSoalKuis(' + m.id + ')"><i class="fas fa-list-check"></i></button>' : '') +
                    '<button class="btn btn-sm btn-outline-danger" onclick="krHapusMateri(' + m.id + ')"><i class="fas fa-trash"></i></button>' +
                    '</div>';
            });
            list.insertAdjacentHTML("beforeend",
                '<div class="kr-section-item-' + krRnd + ' p-2">' +
                    '<div class="d-flex justify-content-between align-items-center">' +
                        '<b>' + krEsc(s.judul) + '</b>' +
                        '<button class="btn btn-sm btn-outline-danger" onclick="krHapusSeksi(' + s.id + ')"><i class="fas fa-trash"></i></button>' +
                    '</div>' + materiHtml +
                    '<div class="row g-1 mt-2">' +
                        '<div class="col-4"><input type="text" class="form-control form-control-sm" id="krMJudul' + krRnd + '_' + s.id + '" placeholder="Judul materi"></div>' +
                        '<div class="col-2"><select class="form-select form-select-sm" id="krMTipe' + krRnd + '_' + s.id + '" onchange="krToggleMTipe(' + s.id + ')"><option value="Video">Video</option><option value="Artikel">Artikel</option><option value="Quiz">Quiz</option><option value="Tugas">Tugas (Unggah)</option></select></div>' +
                        '<div class="col-2"><input type="number" class="form-control form-control-sm" id="krMDurasi' + krRnd + '_' + s.id + '" placeholder="Menit"></div>' +
                        '<div class="col-2" id="krMFileWrap' + krRnd + '_' + s.id + '"><input type="file" class="form-control form-control-sm" id="krMFile' + krRnd + '_' + s.id + '"></div>' +
                        '<div class="col-1 d-flex align-items-center"><input type="checkbox" class="form-check-input" id="krMPreview' + krRnd + '_' + s.id + '" title="Free preview"></div>' +
                        '<div class="col-1"><button class="btn btn-sm btn-kr-primary-' + krRnd + ' w-100" onclick="krTambahMateri(' + s.id + ')"><i class="fas fa-plus"></i></button></div>' +
                        '<div class="col-12 mt-1"><input type="text" class="form-control form-control-sm" id="krMKet' + krRnd + '_' + s.id + '" placeholder="Deskripsi / petunjuk pengerjaan (opsional)"></div>' +
                    '</div>' +
                '</div>');
        });
    }
    async function krTambahSeksi() {
        const produkKursusId = document.getElementById("krKurProdukId" + krRnd).value;
        const judulEl = document.getElementById("krKurSeksiBaru" + krRnd);
        if (!judulEl.value.trim()) return;
        const r = await krApi("simpan_seksi", { produkKursusId: produkKursusId, judul: judulEl.value.trim() });
        if (r.status === "success") { judulEl.value = ""; krRenderKurikulum(); } else krToast(r.message || "Gagal.", "danger");
    }
    async function krHapusSeksi(id) {
        if (!confirm("Hapus section ini beserta seluruh materinya?")) return;
        await krApi("hapus_seksi", { id: id });
        krRenderKurikulum();
    }
    function krToggleMTipe(seksiId) {
        const tipe = document.getElementById("krMTipe" + krRnd + "_" + seksiId).value;
        const wrap = document.getElementById("krMFileWrap" + krRnd + "_" + seksiId);
        if (wrap) wrap.style.display = (tipe === "Tugas") ? "none" : "block";
    }
    async function krTambahMateri(seksiId) {
        const judul = document.getElementById("krMJudul" + krRnd + "_" + seksiId).value.trim();
        if (!judul) { krToast("Judul materi harus diisi.", "warning"); return; }
        const fd = new FormData();
        fd.append("seksiId", seksiId);
        fd.append("judul", judul);
        fd.append("keterangan", document.getElementById("krMKet" + krRnd + "_" + seksiId).value);
        fd.append("tipeKonten", document.getElementById("krMTipe" + krRnd + "_" + seksiId).value);
        fd.append("durasiMenit", document.getElementById("krMDurasi" + krRnd + "_" + seksiId).value || 0);
        fd.append("preview", document.getElementById("krMPreview" + krRnd + "_" + seksiId).checked);
        const fileInput = document.getElementById("krMFile" + krRnd + "_" + seksiId);
        if (fileInput.files && fileInput.files[0]) fd.append("file", fileInput.files[0]);
        const r = await krApiUpload("simpan_materi", fd);
        if (r.status === "success") krRenderKurikulum(); else krToast(r.message || "Gagal menambah materi.", "danger");
    }
    async function krHapusMateri(id) {
        if (!confirm("Hapus materi ini?")) return;
        await krApi("hapus_materi", { id: id });
        krRenderKurikulum();
    }

    /* =================== KELOLA SOAL KUIS (INSTRUKTUR) =================== */
    let krSoalKuisDataInstruktur = [];
    async function krBukaSoalKuis(materiId) {
        let judul = "";
        krKurikulumDataInstruktur.forEach(s => s.materi.forEach(m => { if (m.id === materiId) judul = m.judul; }));
        document.getElementById("krSoalKuisMateriId" + krRnd).value = materiId;
        document.getElementById("krSoalKuisJudulMateri" + krRnd).innerText = judul;
        await krMuatSoalKuis();
        krResetFormSoal();
        new bootstrap.Modal(document.getElementById("krModalSoalKuis" + krRnd)).show();
    }
    async function krMuatSoalKuis() {
        const materiId = document.getElementById("krSoalKuisMateriId" + krRnd).value;
        const r = await krApi("get_soal_kuis", { materiKursusId: materiId });
        if (r.status !== "success") { krToast(r.message || "Gagal memuat soal.", "danger"); return; }
        document.getElementById("krKuisNilaiLulus" + krRnd).value = r.data.nilaiLulus;
        document.getElementById("krKuisBatasWaktu" + krRnd).value = r.data.batasWaktuMenit == null ? "" : r.data.batasWaktuMenit;
        document.getElementById("krKuisBatasPercobaan" + krRnd).value = r.data.batasPercobaan == null ? "" : r.data.batasPercobaan;
        document.getElementById("krKuisJumlahTampil" + krRnd).value = r.data.jumlahSoalDitampilkan == null ? "" : r.data.jumlahSoalDitampilkan;
        document.getElementById("krKuisAcakSoal" + krRnd).checked = !!r.data.acakSoal;
        document.getElementById("krKuisAcakJawaban" + krRnd).checked = !!r.data.acakJawaban;
        krSoalKuisDataInstruktur = r.data.soal;
        const wrap = document.getElementById("krDaftarSoalKuis" + krRnd);
        wrap.innerHTML = "";
        if (r.data.soal.length === 0) {
            wrap.innerHTML = '<p class="text-muted small mb-2">Belum ada soal.</p>';
            return;
        }
        r.data.soal.forEach((soal, idx) => {
            const jenisLabel = soal.jenisKoreksi === "Hasil dikoreksi manual" ? "Esai" : (soal.jenisPilihanGanda === "Benar Salah" ? "Benar/Salah" : "Pilihan Ganda");
            wrap.insertAdjacentHTML("beforeend",
                '<div class="kr-materi-row-' + krRnd + '">' +
                    '<span class="badge bg-light text-dark">' + jenisLabel + '</span>' +
                    '<span class="flex-grow-1">' + krEsc(soal.soal) + '</span>' +
                    '<button class="btn btn-sm btn-kr-outline-' + krRnd + '" onclick="krMuatSoalKeForm(' + idx + ')"><i class="fas fa-pen"></i></button>' +
                    '<button class="btn btn-sm btn-outline-danger" onclick="krHapusSoalKuis(' + soal.id + ')"><i class="fas fa-trash"></i></button>' +
                '</div>');
        });
    }
    async function krSimpanConfigKuis() {
        const materiId = document.getElementById("krSoalKuisMateriId" + krRnd).value;
        const r = await krApi("simpan_config_kuis", {
            materiKursusId: materiId,
            nilaiLulus: document.getElementById("krKuisNilaiLulus" + krRnd).value || 60,
            batasWaktuMenit: document.getElementById("krKuisBatasWaktu" + krRnd).value,
            batasPercobaan: document.getElementById("krKuisBatasPercobaan" + krRnd).value,
            jumlahSoalDitampilkan: document.getElementById("krKuisJumlahTampil" + krRnd).value,
            acakSoal: document.getElementById("krKuisAcakSoal" + krRnd).checked,
            acakJawaban: document.getElementById("krKuisAcakJawaban" + krRnd).checked
        });
        krToast(r.message || (r.status === "success" ? "Tersimpan." : "Gagal."), r.status === "success" ? "success" : "danger");
    }
    function krToggleJenisSoal() {
        const manual = document.getElementById("krSoalJenisKoreksi" + krRnd).value === "manual";
        document.getElementById("krSoalJenisPgWrap" + krRnd).style.display = manual ? "none" : "block";
        document.getElementById("krSoalPilihanWrap" + krRnd).style.display = manual ? "none" : "block";
        document.getElementById("krBtnTambahPilihan" + krRnd).style.display = manual ? "none" : "inline-block";
        if (!manual) krToggleJenisPg();
    }
    function krToggleJenisPg() {
        const wrap = document.getElementById("krSoalPilihanWrap" + krRnd);
        const benarSalah = document.getElementById("krSoalJenisPg" + krRnd).value === "Benar Salah";
        document.getElementById("krBtnTambahPilihan" + krRnd).style.display = benarSalah ? "none" : "inline-block";
        wrap.innerHTML = "";
        if (benarSalah) {
            krTambahBarisPilihan("Benar", true);
            krTambahBarisPilihan("Salah", false);
        } else {
            krTambahBarisPilihan("", true);
            krTambahBarisPilihan("", false);
        }
    }
    function krTambahBarisPilihan(teks, benar) {
        const wrap = document.getElementById("krSoalPilihanWrap" + krRnd);
        const idx = wrap.children.length;
        const huruf = String.fromCharCode(65 + idx);
        const row = document.createElement("div");
        row.className = "row g-1 mb-1 align-items-center kr-pilihan-row";
        row.innerHTML =
            '<div class="col-1 text-center fw-bold">' + huruf + '</div>' +
            '<div class="col-8"><input type="text" class="form-control form-control-sm kr-pilihan-teks" placeholder="Teks pilihan" value="' + krEsc(teks || "") + '"></div>' +
            '<div class="col-2 form-check"><input type="radio" name="krPilihanBenar' + krRnd + '" class="form-check-input kr-pilihan-benar" ' + (benar ? "checked" : "") + '> <label class="form-check-label small">Benar</label></div>' +
            '<div class="col-1"><button type="button" class="btn btn-sm btn-outline-danger" onclick="this.closest(\'.kr-pilihan-row\').remove()"><i class="fas fa-times"></i></button></div>';
        wrap.appendChild(row);
    }
    function krResetFormSoal() {
        document.getElementById("krSoalEditId" + krRnd).value = "";
        document.getElementById("krSoalTeks" + krRnd).value = "";
        document.getElementById("krSoalJenisKoreksi" + krRnd).value = "otomatis";
        document.getElementById("krSoalJenisPg" + krRnd).value = "Multiple choice";
        document.getElementById("krSoalSkor" + krRnd).value = 1;
        document.getElementById("krSoalSkorSalah" + krRnd).value = 0;
        krToggleJenisSoal();
    }
    function krMuatSoalKeForm(idx) {
        const soal = krSoalKuisDataInstruktur[idx];
        document.getElementById("krSoalEditId" + krRnd).value = soal.id;
        document.getElementById("krSoalTeks" + krRnd).value = soal.soal;
        const manual = soal.jenisKoreksi === "Hasil dikoreksi manual";
        document.getElementById("krSoalJenisKoreksi" + krRnd).value = manual ? "manual" : "otomatis";
        document.getElementById("krSoalSkor" + krRnd).value = soal.skor;
        document.getElementById("krSoalSkorSalah" + krRnd).value = soal.skorSalah;
        if (!manual) {
            document.getElementById("krSoalJenisPg" + krRnd).value = soal.jenisPilihanGanda || "Multiple choice";
            document.getElementById("krSoalJenisKoreksi" + krRnd).dispatchEvent(new Event("change"));
            const wrap = document.getElementById("krSoalPilihanWrap" + krRnd);
            wrap.innerHTML = "";
            (soal.pilihan || []).forEach(p => krTambahBarisPilihan(p.jawaban, p.betul));
        } else {
            document.getElementById("krSoalJenisKoreksi" + krRnd).dispatchEvent(new Event("change"));
        }
        document.getElementById("krSoalTeks" + krRnd).scrollIntoView({ behavior: "smooth", block: "center" });
    }
    async function krSimpanSoalKuis() {
        const materiId = document.getElementById("krSoalKuisMateriId" + krRnd).value;
        const teksSoal = document.getElementById("krSoalTeks" + krRnd).value.trim();
        if (!teksSoal) { krToast("Teks soal harus diisi.", "warning"); return; }
        const manual = document.getElementById("krSoalJenisKoreksi" + krRnd).value === "manual";
        const params = {
            materiKursusId: materiId,
            soalId: document.getElementById("krSoalEditId" + krRnd).value,
            soal: teksSoal,
            jenisKoreksi: manual ? "manual" : "otomatis",
            skor: document.getElementById("krSoalSkor" + krRnd).value || 1,
            skorSalah: document.getElementById("krSoalSkorSalah" + krRnd).value || 0
        };
        if (!manual) {
            params.jenisPilihanGanda = document.getElementById("krSoalJenisPg" + krRnd).value;
            const pilihan = [];
            document.querySelectorAll("#krSoalPilihanWrap" + krRnd + " .kr-pilihan-row").forEach((row, idx) => {
                const teks = row.querySelector(".kr-pilihan-teks").value.trim();
                const benar = row.querySelector(".kr-pilihan-benar").checked;
                if (teks) pilihan.push({ huruf: String.fromCharCode(65 + idx), jawaban: teks, betul: benar });
            });
            if (pilihan.length < 2) { krToast("Minimal 2 pilihan jawaban harus diisi.", "warning"); return; }
            if (!pilihan.some(p => p.betul)) { krToast("Tandai salah satu pilihan sebagai jawaban benar.", "warning"); return; }
            params.pilihan = JSON.stringify(pilihan);
        }
        const r = await krApi("simpan_soal_kuis", params);
        if (r.status === "success") { krResetFormSoal(); await krMuatSoalKuis(); } else krToast(r.message || "Gagal menyimpan soal.", "danger");
    }
    async function krHapusSoalKuis(soalId) {
        if (!confirm("Hapus soal ini?")) return;
        const r = await krApi("hapus_soal_kuis", { soalId: soalId });
        if (r.status === "success") await krMuatSoalKuis(); else krToast(r.message || "Gagal menghapus soal.", "danger");
    }

    /* =================== SIMPAN KURIKULUM =================== */
    async function krSimpanKurikulum() {
        const produkKursusId = document.getElementById("krKurProdukId" + krRnd).value;
        if (!produkKursusId) { krToast("ID produk tidak ditemukan.", "danger"); return; }
        krToast("Kurikulum berhasil disimpan.", "success");
        setTimeout(() => {
            bootstrap.Modal.getInstance(document.getElementById("krModalKurikulum" + krRnd)).hide();
        }, 500);
    }
</script>
