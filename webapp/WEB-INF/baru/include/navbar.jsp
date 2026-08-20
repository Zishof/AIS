<%@page import="java.util.List"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="ais.database.model.Menu"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.Tbmrole"%>
<%@page import="ais.database.model.koperasi.AnggotaKoperasi"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // --- 1. INISIALISASI & OPTIMALISASI JAVA ---
    String judulKampus = Common.getBahasaConfig("Sistem Informasi Akademik");
    String mottoKampus = Common.getBahasaConfig("Sistem Informasi Akademik");
    String logoKampus = request.getContextPath() + "/component/uiux/assets/img/logos/default-uni.png";
    
	String p = "";
	if(request.getParameter("p") != null && !request.getParameter("p").trim().isEmpty()){
		p = request.getParameter("p").trim();
	}
	String s = "";
	if(request.getParameter("s") != null && !request.getParameter("s").trim().isEmpty()){
		s = request.getParameter("s").trim();
	}
    
    Tbmuser tbmuser = null;
    boolean isUserAktif = false;
    String rnd = Common.getGeneratedBarCode(7);
    
    try {
        PerguruanTinggi pt = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request);
        if (pt != null) {
            judulKampus = (pt.getNama() != null && !pt.getNama().trim().isEmpty()) ? pt.getNama() : judulKampus;
            mottoKampus = (pt.getMotto() != null && !pt.getMotto().trim().isEmpty()) ? pt.getMotto() : mottoKampus;
        }
        
        String fetchedLogo = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
        if (fetchedLogo != null && !fetchedLogo.trim().isEmpty()) {
            logoKampus = fetchedLogo;
        }
        
        tbmuser = Common.getCurrentUser(request);
        isUserAktif = (tbmuser != null && tbmuser.getUserId() != null && tbmuser.getAktif() != null && tbmuser.getAktif());
    } catch (Exception e) {
        System.err.println("Terjadi kesalahan saat memuat data header: " + e.getMessage());
    } finally {
        // Menutup DB session jika diperlukan
    }

    AnggotaKoperasi curentAnggota = tbmuser == null ? null : tbmuser.getAnggotaKoperasi();
    Tbmrole tbmrole = tbmuser == null ? null : tbmuser.hakAkses();
    boolean isRoleKantin = (tbmrole != null && tbmrole.getKantin() != null) ? tbmrole.getKantin() : false;
    boolean isLoginAnggota = (curentAnggota != null) || isRoleKantin;
    
    if (tbmuser != null && Common.getApakahAdminLain(tbmuser)) {
        isLoginAnggota = false;
    }
    
    Menu menu = null;
    String menuParam = request.getParameter("menu");
    if (menuParam != null && !menuParam.trim().isEmpty()) {
        menu = (Menu) GeneralValueObject.ambilData(Menu.class, menuParam.trim(), true);
    }
    
    List<Tbmrole> tbmroles = tbmuser == null ? null : tbmuser.ambilRoles();
%>

<script data-cfasync="false" src="https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js"></script>

<nav class="navbar navbar-mini-hero navbar-top navbar-expand pb-2 pt-2 pe-2 pe-md-3">
    
    <button class="btn navbar-toggler-humburger-icon navbar-toggler me-1 me-sm-3" type="button" data-bs-toggle="collapse" data-bs-target="#navbarVerticalCollapse" aria-controls="navbarVerticalCollapse" aria-expanded="false" aria-label="<%=Common.getBahasaConfig("Tampilkan Navigasi")%>">
        <span class="navbar-toggle-icon"><span class="toggle-line"></span></span>
    </button>

   <a class="navbar-brand me-1 me-sm-3 d-flex align-items-center gap-3 text-decoration-none" href="<%=request.getContextPath() %>/baru">
        <div class="d-flex align-items-center justify-content-center logo-wrapper flex-shrink-0" style="width: 48px; height: 48px;">
            <img src="<%=logoKampus %>" alt="<%=Common.getBahasaConfig("Logo Institusi") %>" class="img-fluid" style="max-height: 38px; object-fit: contain;" />
        </div>
        
        <div class="d-flex flex-column justify-content-center" style="min-width: 0;">
            <span class="font-sans-serif brand-title text-uppercase text-wrap" style="font-size: 15px; line-height: 1.2;" title="<%=judulKampus%>">
                <%=judulKampus %>
            </span>
            <span class="brand-motto text-wrap mt-1" style="font-size: 11px; letter-spacing: 0.2px; line-height: 1.3;">
                <%=mottoKampus %>
            </span>
        </div>
    </a>

<%
if (isUserAktif) {
    String namaPengguna = (tbmuser.getUserNama() != null) ? tbmuser.getUserNama() : Common.getBahasaConfig("Pengguna Anonim");
    String rolePengguna = (tbmrole != null && tbmrole.getRoleName() != null) ? tbmrole.getRoleName() : Common.getBahasaConfig("Pengguna");
    String idPengguna = tbmuser.getUserId();
    String urlFotoDB = CommonMedia.getUrlFotoPengguna(tbmuser);
    String linkFoto = (urlFotoDB == null || urlFotoDB.trim().isEmpty())
                    ? request.getContextPath() + "/component/uiux/assets/img/team/avatar.png"
                    : urlFotoDB;
    String _lgAktifNav = Common.currentLang();
    String _lgKode = ais.common.Common.kodeBahasaAktif();
    String _lgFlag = "en".equals(_lgKode) ? "united-kingdom"
            : ("ar".equals(_lgKode) ? "saudi-arabia" : ("zh".equals(_lgKode) ? "china" : "indonesia"));
%>

    <style>
    /* ======================================================================
       HEADER KANAN JSP (mobile & desktop) — SEJAJARKAN TINGGI + BADGE NOTIF
       ----------------------------------------------------------------------
       Sengaja ditaruh menyatu dengan navbar.jsp karena halaman JSP TIDAK
       memuat css_utama.css (memakai theme Falcon: theme.css/user.css).

       1) TINGGI SERAGAM: bendera bahasa, tombol tema, lonceng, dan profil
          disamakan 44px (40px pada layar kecil) lalu ter-tengah vertikal.
          CATATAN: display pada .nav-item TIDAK disetel, karena item tombol
          tema memakai kelas "d-none d-sm-block" — memaksa display akan
          membuatnya ikut tampil di layar kecil.
       2) LINGKARAN MERAH: theme Falcon punya .notification-indicator::before,
          yaitu lingkaran 1rem ber-border yang SELALU tampil tanpa peduli ada
          atau tidaknya notifikasi. Karena kita sudah memakai badge angka
          sendiri (#notifUnreadBadge, hanya muncul bila ada yang BELUM dibaca),
          indikator bawaan itu dimatikan agar tidak ada lingkaran nyangkut.
       ====================================================================== */
    .navbar-nav-icons > .nav-item {
        height: 44px !important;
    }
    .navbar-nav-icons > .nav-item > a,
    .navbar-nav-icons > .nav-item > .theme-control-toggle {
        display: inline-flex !important;
        align-items: center !important;
        height: 100% !important;
    }
    .navbar-nav-icons .user-profile-wrapper {
        height: 100% !important;
        align-items: center !important;
    }
    /* Indikator bawaan theme dimatikan (redundan + selalu tampil) */
    .navbar-nav-icons .notification-indicator::before {
        display: none !important;
        content: none !important;
    }
    /* Badge angka: benar-benar hilang saat tidak ada yang belum dibaca */
    .navbar-nav-icons .indicator-badge.d-none {
        display: none !important;
    }
    @media (max-width: 992px) {
        .navbar-nav-icons > .nav-item {
            height: 40px !important;
        }
    }
    </style>

    <ul class="navbar-nav navbar-nav-icons ms-auto flex-row align-items-center gap-2 gap-sm-3">

        <li id="ais-lang-switcher-navbar" class="nav-item ais-lang-dd" style="position:relative;" title="<%=Common.getBahasaConfig("Pilih Bahasa")%>">
            <a href="javascript:void(0)" onclick="aisLangDD(this)" style="display:inline-flex;align-items:center;gap:3px;cursor:pointer;text-decoration:none;">
                <img src="<%=request.getContextPath()%>/component/assets/media/flags/<%=_lgFlag%>.svg" style="width:20px;height:13px;border-radius:3px;box-shadow:0 0 0 1px rgba(0,0,0,.15);vertical-align:middle;">
                <span style="font-size:10px;color:#94a3b8;">&#9662;</span>
            </a>
            <div class="ais-lang-menu" style="display:none;position:absolute;right:0;top:100%;margin-top:6px;background:#fff;border:1px solid #e2e8f0;border-radius:8px;box-shadow:0 6px 18px rgba(0,0,0,.18);padding:6px;z-index:30000;">
                <a href="?lang=id" title="Indonesia" style="display:block;padding:5px;border-radius:4px;"><img src="<%=request.getContextPath()%>/component/assets/media/flags/indonesia.svg" style="width:30px;height:20px;border-radius:3px;display:block;"></a>
                <a href="?lang=en" title="English" style="display:block;padding:5px;border-radius:4px;"><img src="<%=request.getContextPath()%>/component/assets/media/flags/united-kingdom.svg" style="width:30px;height:20px;border-radius:3px;display:block;"></a>
                <a href="?lang=ar" title="العربية" style="display:block;padding:5px;border-radius:4px;"><img src="<%=request.getContextPath()%>/component/assets/media/flags/saudi-arabia.svg" style="width:30px;height:20px;border-radius:3px;display:block;"></a>
                <a href="?lang=zh" title="中文" style="display:block;padding:5px;border-radius:4px;"><img src="<%=request.getContextPath()%>/component/assets/media/flags/china.svg" style="width:30px;height:20px;border-radius:3px;display:block;"></a>
            </div>
        </li>
        <script>
        function aisLangDD(el){var m=el.parentNode.querySelector('.ais-lang-menu');if(!m)return;m.style.display=(m.style.display==='block')?'none':'block';}
        if(!window.__aisLangDDInit){window.__aisLangDDInit=true;document.addEventListener('click',function(e){var ms=document.getElementsByClassName('ais-lang-menu');for(var i=0;i<ms.length;i++){if(!ms[i].parentNode.contains(e.target))ms[i].style.display='none';}});}
        </script>

        <li class="nav-item d-none d-sm-block">
            <div class="theme-control-toggle fa-icon-wait px-2" title="<%=Common.getBahasaConfig("Ubah Konfigurasi Tampilan Gelap/Terang")%>">
                <input class="form-check-input ms-0 theme-control-toggle-input" id="themeControlToggle" type="checkbox" data-theme-control="theme" value="dark" />
                <label class="mb-0 theme-control-toggle-label theme-control-toggle-light" for="themeControlToggle" data-bs-toggle="tooltip" data-bs-placement="left" title="<%=Common.getBahasaConfig("Beralih ke Tema Gelap")%>"><span class="fas fa-moon fs-0"></span></label>
                <label class="mb-0 theme-control-toggle-label theme-control-toggle-dark" for="themeControlToggle" data-bs-toggle="tooltip" data-bs-placement="left" title="<%=Common.getBahasaConfig("Beralih ke Tema Terang")%>"><span class="fas fa-sun fs-0"></span></label>
            </div>
        </li>

        <li class="nav-item dropdown">
            <a class="nav-link notification-indicator px-0 fa-icon-wait" id="navbarDropdownNotification" role="button" data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false" title="<%=Common.getBahasaConfig("Pusat Pemberitahuan Sistem")%>">
                <span class="fas fa-bell transition-base" style="font-size: 1.25rem;"></span>
                <%-- Badge angka: HANYA menghitung notifikasi yang BELUM dibaca (diisi oleh
                     renderNotifikasiKeDOM). Bentuk kapsul bulat rapi, angka ter-tengah, dan
                     digeser sedikit ke KIRI agar tidak menutup ikon lonceng. --%>
                <span class="indicator-badge bg-danger position-absolute d-none border border-light" id="notifUnreadBadge<%=rnd%>" style="top:-3px; right:9px; left:auto; min-width:18px; height:18px; line-height:1; padding:0 5px; border-radius:999px; font-size:10px; font-weight:800; color:#fff; display:inline-flex; align-items:center; justify-content:center; box-sizing:border-box; box-shadow:0 1px 3px rgba(15,23,42,.28); pointer-events:none;"></span>
            </a>
            
            <div class="dropdown-menu dropdown-caret dropdown-menu-end dropdown-menu-card dropdown-menu-notification dropdown-caret-bg" aria-labelledby="navbarDropdownNotification">
                <div class="card card-notification shadow-sm border-0">
                    <div class="card-header border-bottom bg-light">
                        <div class="row justify-content-between align-items-center">
                            <div class="col-auto">
                                <h6 class="card-header-title mb-0 text-primary fw-bold"><%=Common.getBahasaConfig("Pemberitahuan Sistem") %></h6>
                            </div>
                            <div class="col-auto ps-0 ps-sm-3">
                                <a class="card-link fw-normal fs--1 text-decoration-none" href="javascript:void(0);" onclick="tandaiSemuaDibaca<%=rnd%>()"><%=Common.getBahasaConfig("Tandai Semua Telah Dibaca") %></a>
                            </div>
                        </div>
                    </div>
                    <div class="scrollbar-overlay" style="max-height:19rem">
                        <div class="list-group list-group-flush fw-normal fs--1" id="listNotifikasiSistem<%=rnd%>">
                            <div class="list-group-item text-center py-4 text-muted">
                                <i class="fas fa-spinner fa-spin me-2"></i> <%=Common.getBahasaConfig("Sistem sedang memuat pemberitahuan...")%>
                            </div>
                        </div>
                    </div>
                    <div class="card-footer text-center border-top bg-light">
                        <a class="card-link d-block text-decoration-none fw-bold text-primary" href="javascript:void(0);" onclick="bukaModalRiwayatNotifikasi<%=rnd%>()"><%=Common.getBahasaConfig("Tampilkan Seluruh Pemberitahuan") %></a>
                    </div>
                </div>
            </div>
        </li>

        <li class="nav-item dropdown">
            <a class="nav-link pe-0 ps-2" id="navbarDropdownUser" role="button" data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false" title="<%=Common.getBahasaConfig("Pengaturan Akun")%>">
                <div class="d-flex align-items-center rounded-pill p-1 user-profile-wrapper">
                    <div class="avatar avatar-xl">
                        <img class="rounded-circle shadow-sm" src="<%=linkFoto %>" alt="<%=Common.getBahasaConfig("Foto Profil") %>" style="object-fit: cover; border: 2px solid #fff;" />
                    </div>
                    <div class="d-none d-xl-block ms-2 me-3 text-start">
                        <h6 class="mb-0 user-profile-name fs--1 line-height-1 text-truncate" style="max-width: 150px;"><%=namaPengguna %></h6>
                        <p class="mb-0 user-profile-role fs--2"><%=rolePengguna %></p>
                    </div>
                    <span class="fas fa-chevron-down ms-1 me-2 d-none d-xl-block fs--2"></span>
                </div>
            </a>
            
            <div class="dropdown-menu dropdown-caret dropdown-menu-end py-0 mt-2 shadow-sm border-0" aria-labelledby="navbarDropdownUser">
                <div class="bg-white dark__bg-1000 rounded-3 py-2">
                    
                    <div class="px-3 pt-2 pb-3 border-bottom mb-2 text-center">
                        <div class="avatar avatar-3xl mb-2">
                            <img class="rounded-circle shadow-sm object-fit-cover" src="<%=linkFoto%>" alt="Foto Profil" onerror="this.onerror=null; this.src='<%=request.getContextPath()%>/component/uiux/assets/img/team/avatar.png';"/>
                        </div>
                        <h6 class="mb-0 fw-bolder text-dark"><%=namaPengguna%></h6>
                        <small class="text-muted fw-semibold"><%=idPengguna%></small>
                    </div>

                    <a class="dropdown-item px-3 py-2 text-700 hover-bg-100 transition-base" href="javascript:void(0);" onclick="tampilModalUbahProfil();">
                        <span class="far fa-user-circle me-2 text-primary text-center" style="width: 20px;"></span><%=Common.getBahasaConfig("Profil Pengguna") %>
                    </a>
                    <a class="dropdown-item px-3 py-2 text-700 hover-bg-100 transition-base" href="javascript:void(0);" onclick="tampilModalGantiPassword(false);">
                        <span class="fas fa-cog me-2 text-secondary text-center" style="width: 20px;"></span><%=Common.getBahasaConfig("Pengaturan Kata Sandi") %>
                    </a>
                    <% if (!isLoginAnggota) { %>
                    <div class="dropdown-divider"></div>
                    <a class="dropdown-item px-3 py-2 text-warning hover-bg-warning-subtle fw-medium transition-base" href="<%=request.getContextPath()%>/main?versilama=true">
                        <span class="fas fa-undo me-2 text-center" style="width: 20px;"></span><%=Common.getBahasaConfig("Kembali ke Tampilan Klasik") %>
                    </a>
                    <%
					if (tbmroles != null && tbmroles.size() > 1) {
					%>
					    <a class="dropdown-item px-3 py-2 text-warning hover-bg-warning-subtle fw-medium transition-base d-flex align-items-center" href="<%=Common.ROOT%>/baru?hak_akses=true">
					        <span class="fas fa-user-shield me-2 text-center" style="width: 20px;"></span>
					        <%= Common.getBahasaConfig("Ganti Hak Akses") %>
					    </a>
					<%
					}
					%>
                    <% } %>
                    
                    <div class="dropdown-divider"></div>
                    <a class="dropdown-item px-3 py-2 text-danger hover-bg-danger-subtle fw-medium transition-base" href="javascript:void(0);" onclick="tampilkanPopupKeluar()">
                        <span class="fas fa-sign-out-alt me-2 text-center" style="width: 20px;"></span><%=Common.getBahasaConfig("Keluar Dari Sistem") %>
                    </a>
                </div>
            </div>
        </li>
    </ul>
<% } %>
</nav>

<% if (isUserAktif && tbmuser != null && tbmuser.getUserId() != null) { %>
<script>
    // ==========================================
    // LOGIKA INJEKSI MODAL & AJAX POLLING
    // ==========================================
    let currentPageRiwayatNotif<%=rnd%> = 1;
    const limitRiwayatNotif<%=rnd%> = 10;
    let totalRiwayatNotif<%=rnd%> = 0;

    const injectModalNotifikasi<%=rnd%> = () => {
        if (!document.getElementById('modalDetailNotifikasi<%=rnd%>')) {
            const modalHtml = '<div class="modal fade" id="modalDetailNotifikasi<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">' +
                '<div class="modal-dialog modal-dialog-centered">' +
                    '<div class="modal-content rounded-4 border-0 shadow-lg">' +
                        '<div class="modal-header bg-light border-bottom-0 pb-2 pt-3 px-4">' +
                            '<h5 class="modal-title fw-bold text-dark">' +
                                '<i class="fas fa-bell text-primary me-2"></i><%=Common.getBahasaConfig("Rincian Pemberitahuan")%>' +
                            '</h5>' +
                            '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<%=Common.getBahasaConfig("Tutup")%>"></button>' +
                        '</div>' +
                        '<div class="modal-body p-4">' +
                            '<div class="d-flex align-items-center mb-3">' +
                                '<div class="avatar avatar-3xl me-3">' +
                                     '<div class="avatar-name rounded-circle text-white shadow-sm" id="notifModalIconBg<%=rnd%>">' +
                                        '<span id="notifModalIcon<%=rnd%>"></span>' +
                                     '</div>' +
                                '</div>' +
                                '<div>' +
                                    '<h6 class="mb-0 fw-bold text-dark"><%=Common.getBahasaConfig("Pemberitahuan Sistem")%></h6>' +
                                    '<small class="text-muted fw-bold" id="notifModalTime<%=rnd%>"></small>' +
                                '</div>' +
                            '</div>' +
                            '<div class="bg-light p-3 rounded-3 border">' +
                                '<p class="mb-0 text-dark" id="notifModalMessage<%=rnd%>" style="font-size: 15px; line-height: 1.6;"></p>' +
                            '</div>' +
                        '</div>' +
                        '<div class="modal-footer border-top-0 justify-content-end pt-0 bg-white rounded-bottom-4">' +
                             '<button type="button" class="btn btn-success rounded-pill px-4 fw-bold shadow-sm d-none" id="notifModalOpenBtn<%=rnd%>"><i class="fas fa-external-link-alt me-2"></i><%=Common.getBahasaConfig("Buka Halaman Terkait")%></button>' +
                             '<button type="button" class="btn btn-primary rounded-pill px-4 fw-bold shadow-sm" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Tutup Jendela")%></button>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
            document.body.insertAdjacentHTML('beforeend', modalHtml);
        }

        if (!document.getElementById('modalRiwayatNotifikasi<%=rnd%>')) {
            const modalRiwayatHtml = '<div class="modal fade" id="modalRiwayatNotifikasi<%=rnd%>" tabindex="-1" aria-hidden="true">' +
                '<div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">' +
                     '<div class="modal-content rounded-4 border-0 shadow-lg">' +
                        '<div class="modal-header bg-primary text-white border-bottom-0 pb-3 pt-4 px-4">' +
                            '<h5 class="modal-title fw-bold">' +
                                 '<i class="fas fa-history me-2"></i><%=Common.getBahasaConfig("Riwayat Seluruh Pemberitahuan")%>' +
                            '</h5>' +
                            '<button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="<%=Common.getBahasaConfig("Tutup")%>"></button>' +
                        '</div>' +
                        '<div class="modal-body p-0 bg-light">' +
                            '<div class="table-responsive" style="min-height: 400px;">' +
                                '<table class="table table-hover align-middle mb-0" id="tabelRiwayatNotif<%=rnd%>">' +
                                    '<thead class="table-light text-secondary">' +
                                        '<tr>' +
                                            '<th class="fw-bold small py-3 px-4" style="width: 150px;"><%=Common.getBahasaConfig("Waktu")%></th>' +
                                            '<th class="fw-bold small"><%=Common.getBahasaConfig("Tipe")%></th>' +
                                            '<th class="fw-bold small"><%=Common.getBahasaConfig("Rincian Pesan")%></th>' +
                                            '<th class="fw-bold small text-center" style="width: 100px;"><%=Common.getBahasaConfig("Status")%></th>' +
                                         '</tr>' +
                                    '</thead>' +
                                    '<tbody id="tbodyRiwayatNotif<%=rnd%>">' +
                                         '<tr><td colspan="4" class="text-center py-5"><i class="fas fa-spinner fa-spin text-primary fs-3"></i></td></tr>' +
                                    '</tbody>' +
                                 '</table>' +
                            '</div>' +
                            '<div class="bg-white p-3 border-top d-flex flex-column flex-md-row justify-content-between align-items-center gap-3">' +
                                '<div class="small fw-bold text-muted" id="pagingInfoRiwayatNotif<%=rnd%>"></div>' +
                                '<div class="d-flex align-items-center bg-light rounded-pill shadow-sm border p-1">' +
                                    '<button class="btn btn-sm btn-white rounded-pill px-3 fw-bold text-primary" id="btnPrevPageRN<%=rnd%>" onclick="changePageRiwayatNotif<%=rnd%>(-1)">' +
                                        '<i class="fas fa-chevron-left me-1"></i><span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Sebelumnya")%></span>' +
                                    '</button>' +
                                    '<div class="px-3 fw-bold text-dark small"><span id="lblCurrentPageRN<%=rnd%>">1</span> / <span id="lblTotalPageRN<%=rnd%>">1</span></div>' +
                                    '<button class="btn btn-sm btn-white rounded-pill px-3 fw-bold text-primary" id="btnNextPageRN<%=rnd%>" onclick="changePageRiwayatNotif<%=rnd%>(1)">' +
                                        '<span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Selanjutnya")%></span><i class="fas fa-chevron-right ms-1"></i>' +
                                    '</button>' +
                                 '</div>' +
                            '</div>' +
                        '</div>' +
                        '<div class="modal-footer border-top bg-white">' +
                            '<button type="button" class="btn btn-light px-4 rounded-pill fw-bold border shadow-sm me-auto" data-bs-dismiss="modal"><i class="fas fa-times me-2 text-danger"></i><%=Common.getBahasaConfig("Tutup")%></button>' +
                            '<button type="button" class="btn btn-success px-4 rounded-pill fw-bold shadow-sm" onclick="downloadExcelRiwayatNotif<%=rnd%>()"><i class="fas fa-file-excel me-2"></i><%=Common.getBahasaConfig("Unduh Data (Excel)")%></button>' +
                         '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
            document.body.insertAdjacentHTML('beforeend', modalRiwayatHtml);
        }
    };

    let lastNotifId_<%= rnd %> = localStorage.getItem("lastNotifId_<%= tbmuser.getUserId() %>") || 0;
    const apiUrlNotifikasi<%=rnd%> = '<%=request.getContextPath()%>/baru?hanya_tampil_jsp=true&p=common&s=_api_notifikasi_sistem';

    /* Endpoint penanda "sudah dibaca". WAJIB lewat NotifikasiCache (bukan SQL mentah ke
       /Data): daftar notifikasi dilayani dari cache, jadi update DB langsung tidak akan
       menyegarkan badge/warna sampai TTL berakhir. */
    const apiUrlBacaNotif<%=rnd%> = '<%=request.getContextPath()%>/baru?hanya_tampil_jsp=true&p=common&s=_api_notifikasi_baca';

    const tandaiSemuaDibaca<%=rnd%> = async () => {
        const badge = document.getElementById('notifUnreadBadge<%=rnd%>');
        if (badge) badge.classList.add('d-none');
        try {
            await fetch(apiUrlBacaNotif<%=rnd%> + '&semua=true');
            tarikNotifikasiBerkala<%=rnd%>();
        } catch(e) { console.error(e); }
    };

    const bukaHalamanNotif<%=rnd%> = (bukaJsp) => {
        if (!bukaJsp) return;
        let fullUrl = bukaJsp;
        if (!/^https?:\/\//i.test(bukaJsp)) {
            const ctx = '<%=request.getContextPath()%>';
            fullUrl = ctx + (bukaJsp.charAt(0) === '/' ? '' : '/') + bukaJsp;
        }
        // Langsung menuju halaman terkait pada tab yang sama.
        window.location.href = fullUrl;
    };

    const bukaNotifikasi<%=rnd%> = async (id, msgEncoded, type, time, isRead, element, bukaJspEncoded) => {
        const bukaJsp = bukaJspEncoded ? decodeURIComponent(bukaJspEncoded) : '';

        // 1) Tandai sudah dibaca (UI + server) terlebih dahulu.
        if (!isRead) {
            try {
                if (element) {
                    /* Ubah tampilan menjadi gaya "sudah dibaca" seketika: hilangkan garis
                       aksen kiri, buat agak pudar, redupkan teks/ikon, dan buang titik
                       penanda "belum dibaca". */
                    element.classList.remove('bg-primary-subtle', 'border-primary', 'bg-danger-subtle', 'border-danger', 'bg-warning-subtle', 'border-warning');
                    element.classList.add('bg-light', 'text-secondary', 'border');
                    element.style.borderLeft = '3px solid transparent';
                    element.style.opacity = '.72';
                    const paragraf = element.querySelector('p');
                    if (paragraf) { paragraf.classList.remove('text-dark', 'fw-bold'); paragraf.classList.add('text-muted'); }
                    const avatar = element.querySelector('.avatar-name');
                    if (avatar) { avatar.classList.remove('bg-danger', 'bg-warning', 'bg-primary'); avatar.classList.add('bg-secondary'); }
                    const titik = element.querySelectorAll('.ais-notif-dot');
                    if (titik) { titik.forEach(function(d){ d.remove(); }); }
                }
                /* Kurangi badge seketika (tidak menunggu polling berikutnya). */
                const badgeNow = document.getElementById('notifUnreadBadge<%=rnd%>');
                if (badgeNow) {
                    let sisa = parseInt(badgeNow.textContent, 10);
                    if (isNaN(sisa)) { sisa = 0; }
                    sisa = sisa > 0 ? sisa - 1 : 0;
                    badgeNow.textContent = sisa > 99 ? '99+' : String(sisa);
                    if (sisa > 0) { badgeNow.classList.remove('d-none'); } else { badgeNow.classList.add('d-none'); }
                }
                await fetch(apiUrlBacaNotif<%=rnd%> + '&id=' + encodeURIComponent(id));
            } catch(e) { console.error(e); }
        }

        // 2) KLIK = LANGSUNG menuju halaman terkait bila notifikasi membawa tujuan (bukaJsp).
        if (bukaJsp && bukaJsp.trim() !== '') {
            try { tarikNotifikasiBerkala<%=rnd%>(); } catch(e) {}
            bukaHalamanNotif<%=rnd%>(bukaJsp);
            return;
        }

        // 3) Tidak ada halaman tujuan -> tampilkan rincian pesan pada modal.
        document.getElementById('notifModalMessage<%=rnd%>').innerHTML = decodeURIComponent(msgEncoded);
        document.getElementById('notifModalTime<%=rnd%>').innerText = time;
        let bgColor = "bg-primary"; let icon = "fas fa-info-circle";
        if (type === "DANGER") { bgColor = "bg-danger"; icon = "fas fa-ban"; }
        else if (type === "WARNING") { bgColor = "bg-warning"; icon = "fas fa-exclamation-triangle"; }
        document.getElementById('notifModalIconBg<%=rnd%>').className = "avatar-name rounded-circle text-white shadow-sm " + bgColor;
        document.getElementById('notifModalIcon<%=rnd%>').className = icon;
        const openBtn = document.getElementById('notifModalOpenBtn<%=rnd%>');
        if (openBtn) { openBtn.classList.add('d-none'); openBtn.onclick = null; }
        const modalEl = document.getElementById('modalDetailNotifikasi<%=rnd%>');
        if (modalEl) { const modalInstance = new bootstrap.Modal(modalEl); modalInstance.show(); }
        try {
            tarikNotifikasiBerkala<%=rnd%>();
            const modalRw = document.getElementById('modalRiwayatNotifikasi<%=rnd%>');
            if (modalRw && modalRw.classList.contains('show')) { loadDataRiwayatNotifAPI<%=rnd%>(); }
        } catch(e) { console.error(e); }
    };

    const renderNotifikasiKeDOM<%=rnd%> = (notifArray) => {
        const listContainer = document.getElementById('listNotifikasiSistem<%=rnd%>');
        if (!listContainer) return;
        let htmlContent = ''; let unreadCount = 0;

        if (notifArray.length === 0) {
            htmlContent = '<div class="list-group-item text-center py-4 text-muted border-0"><i class="fas fa-bell-slash fa-2x mb-2 opacity-50"></i><br><%=Common.getBahasaConfig("Tidak ada pemberitahuan baru saat ini.")%></div>';
        } else {
            notifArray.forEach(data => {
                if (!data.isRead) unreadCount++;

                let bgColor = "bg-primary"; let icon = "fas fa-info-circle";
                let notifClass = data.isRead ? "bg-light text-secondary border" : "bg-primary-subtle border border-primary border-opacity-25";
                let textClass = data.isRead ? "text-muted" : "text-dark fw-bold";
                let aksen = "#2563eb";

                if (data.type === "DANGER") {
                    bgColor = data.isRead ? "bg-secondary" : "bg-danger"; icon = "fas fa-ban";
                    notifClass = data.isRead ? "bg-light text-secondary border" : "bg-danger-subtle border border-danger border-opacity-25";
                    aksen = "#dc3545";
                } else if (data.type === "WARNING") {
                    bgColor = data.isRead ? "bg-secondary" : "bg-warning"; icon = "fas fa-exclamation-triangle";
                    notifClass = data.isRead ? "bg-light text-secondary border" : "bg-warning-subtle text-dark border border-warning border-opacity-50";
                    aksen = "#f59e0b";
                }

                /* Pembeda tegas (selaras lonceng ZK): BELUM dibaca = garis aksen di kiri +
                   titik penanda + tampil pekat. SUDAH dibaca = tanpa aksen + agak pudar. */
                const gayaItem = data.isRead
                    ? 'border-left:3px solid transparent; opacity:.72;'
                    : 'border-left:3px solid ' + aksen + ';';
                const titikBaru = data.isRead ? ''
                    : '<span class="ais-notif-dot" title="<%=Common.getBahasaConfig("Belum dibaca")%>" style="display:inline-block;width:8px;height:8px;border-radius:50%;background:' + aksen + ';margin-left:6px;vertical-align:middle;"></span>';

                htmlContent += '<div class="list-group-item p-1 border-0 animate__animated animate__fadeIn">' +
                    '<a class="notification notification-flush rounded-3 m-0 p-3 ' + notifClass + '" style="' + gayaItem + '" href="javascript:void(0);" onclick="bukaNotifikasi<%=rnd%>(' + data.id + ', \'' + encodeURIComponent(data.message) + '\', \'' + data.type + '\', \'' + data.time + '\', ' + data.isRead + ', this, \'' + encodeURIComponent(data.bukaJsp || '') + '\')">' +
                        '<div class="notification-avatar"><div class="avatar avatar-2xl me-3"><div class="avatar-name rounded-circle ' + bgColor + ' text-white shadow-sm"><span class="' + icon + '"></span></div></div></div>' +
                        '<div class="notification-body">' +
                            '<p class="mb-1 ' + textClass + '" style="display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;">' + data.message + '</p>' +
                            '<span class="notification-time text-secondary fw-semibold small"><span class="far fa-clock me-1"></span>' + data.time + '</span>' + titikBaru +
                        '</div></a></div>';
            });
        }
        
        listContainer.innerHTML = htmlContent;
        /* Badge merah menampilkan JUMLAH notifikasi yang BELUM dibaca. Notifikasi yang
           sudah diklik/dibaca tetap tampil di daftar (redup) tapi TIDAK dihitung lagi. */
        const badge = document.getElementById('notifUnreadBadge<%=rnd%>');
        if (badge) {
            badge.textContent = unreadCount > 99 ? '99+' : String(unreadCount);
            badge.title = unreadCount > 0 ? (unreadCount + ' <%=Common.getBahasaConfig("belum dibaca")%>') : '';
            if (unreadCount > 0) { badge.classList.remove('d-none'); } else { badge.classList.add('d-none'); }
        }
    };

    const tarikNotifikasiBerkala<%=rnd%> = async () => {
        try {
            const response = await fetch(apiUrlNotifikasi<%=rnd%>);
            const resData = await response.json();
            if (resData.status === 'success' && resData.data) {
                const notifArray = resData.data;
                renderNotifikasiKeDOM<%=rnd%>(notifArray);
                if (notifArray.length > 0) {
                    const latestId = notifArray[0].id;
                    if (latestId > lastNotifId_<%= rnd %>) {
                        /* Badge TIDAK dipaksa tampil di sini: renderNotifikasiKeDOM sudah
                           menetapkannya dari jumlah yang benar-benar belum dibaca. */
                        if (typeof showToastUI === "function") {
                            const terbaru = notifArray[0];
                            const alertWarna = terbaru.type === 'DANGER' ? 'bg-danger text-white' : 'bg-warning text-dark';
                            showToastUI(terbaru.message, alertWarna);
                        }
                        lastNotifId_<%= rnd %> = latestId;
                        localStorage.setItem("lastNotifId_<%= tbmuser.getUserId() %>", latestId);
                    }
                }
            }
        } catch (error) {}
    };

    const bukaModalRiwayatNotifikasi<%=rnd%> = () => {
        currentPageRiwayatNotif<%=rnd%> = 1;
        const modalEl = document.getElementById('modalRiwayatNotifikasi<%=rnd%>');
        if (modalEl) { const modalInstance = new bootstrap.Modal(modalEl); modalInstance.show(); loadDataRiwayatNotifAPI<%=rnd%>(); }
    };

    const changePageRiwayatNotif<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRiwayatNotif<%=rnd%> / limitRiwayatNotif<%=rnd%>);
        if (direction === -1 && currentPageRiwayatNotif<%=rnd%> > 1) { currentPageRiwayatNotif<%=rnd%>--; loadDataRiwayatNotifAPI<%=rnd%>(); } 
        else if (direction === 1 && currentPageRiwayatNotif<%=rnd%> < totalPages) { currentPageRiwayatNotif<%=rnd%>++; loadDataRiwayatNotifAPI<%=rnd%>(); }
    };

    const loadDataRiwayatNotifAPI<%=rnd%> = async () => {
        const tbody = document.getElementById('tbodyRiwayatNotif<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="4" class="text-center py-5"><div class="spinner-border text-primary" role="status"></div><br><span class="text-muted fw-medium mt-2 d-block"><%=Common.getBahasaConfig("Mengambil seluruh data pemberitahuan...")%></span></td></tr>';
        try {
            const urlTarget = apiUrlNotifikasi<%=rnd%> + '&page=' + currentPageRiwayatNotif<%=rnd%> + '&limit=' + limitRiwayatNotif<%=rnd%>;
            const response = await fetch(urlTarget); const resData = await response.json();
            if (resData.status === 'success') {
                totalRiwayatNotif<%=rnd%> = resData.totalRecords || 0;
                const notifArray = resData.data || []; let htmlList = '';
                if (notifArray.length === 0) {
                    htmlList = '<tr><td colspan="4" class="text-center py-5 text-muted"><i class="fas fa-folder-open fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Belum ada riwayat pemberitahuan untuk Anda.")%></td></tr>';
                } else {
                    notifArray.forEach(data => {
                        let badgeType = '<span class="badge bg-primary px-3 py-2 w-100 shadow-sm"><i class="fas fa-info-circle me-1"></i>Info</span>';
                        if (data.type === 'DANGER') { badgeType = '<span class="badge bg-danger px-3 py-2 w-100 shadow-sm"><i class="fas fa-ban me-1"></i>Peringatan Keras</span>'; } 
                        else if (data.type === 'WARNING') { badgeType = '<span class="badge bg-warning text-dark px-3 py-2 w-100 shadow-sm"><i class="fas fa-exclamation-triangle me-1"></i>Perhatian</span>'; }

                        let badgeStatus = data.isRead ? '<span class="text-success fw-bold small"><i class="fas fa-check-double me-1"></i>Dibaca</span>' : '<span class="text-danger fw-bold small"><i class="fas fa-envelope me-1"></i>Belum</span>';
                        const trClass = data.isRead ? "" : "table-light fw-bold";
                        
                        htmlList += '<tr class="' + trClass + ' cursor-pointer" onclick="bukaNotifikasi<%=rnd%>(' + data.id + ', \'' + encodeURIComponent(data.message) + '\', \'' + data.type + '\', \'' + data.time + '\', ' + data.isRead + ', null, \'' + encodeURIComponent(data.bukaJsp || '') + '\')">' +
                            '<td class="px-4 text-nowrap"><i class="far fa-clock text-muted me-1"></i>' + data.time + '</td>' +
                            '<td class="text-center">' + badgeType + '</td><td>' + data.message + '</td><td class="text-center">' + badgeStatus + '</td></tr>';
                    });
                }
                tbody.innerHTML = htmlList; updatePaginationRiwayatNotifUI<%=rnd%>();
            } else { throw new Error("Gagal mengambil data"); }
        } catch (error) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal mengambil data dari server.")%></td></tr>';
        }
    };

    const updatePaginationRiwayatNotifUI<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRiwayatNotif<%=rnd%> / limitRiwayatNotif<%=rnd%>) || 1;
        document.getElementById('lblCurrentPageRN<%=rnd%>').innerText = currentPageRiwayatNotif<%=rnd%>;
        document.getElementById('lblTotalPageRN<%=rnd%>').innerText = totalPages;
        const startIdx = totalRiwayatNotif<%=rnd%> > 0 ? ((currentPageRiwayatNotif<%=rnd%> - 1) * limitRiwayatNotif<%=rnd%> + 1) : 0;
        const endIdx = Math.min(currentPageRiwayatNotif<%=rnd%> * limitRiwayatNotif<%=rnd%>, totalRiwayatNotif<%=rnd%>);
        document.getElementById('pagingInfoRiwayatNotif<%=rnd%>').innerHTML = '<%=Common.getBahasaConfigJS("Menampilkan")%> <b class="text-dark">' + startIdx + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari")%> <b class="text-dark">' + totalRiwayatNotif<%=rnd%> + '</b> <%=Common.getBahasaConfig("pemberitahuan")%>';
        document.getElementById('btnPrevPageRN<%=rnd%>').disabled = (currentPageRiwayatNotif<%=rnd%> === 1);
        document.getElementById('btnNextPageRN<%=rnd%>').disabled = (currentPageRiwayatNotif<%=rnd%> === totalPages);
    };

    const downloadExcelRiwayatNotif<%=rnd%> = async () => {
        if (typeof showToastUI === "function") { showToastUI('<%=Common.getBahasaConfigJS("Sedang menyiapkan dokumen Excel...")%>', 'bg-info text-white'); }
        try {
            const urlTarget = apiUrlNotifikasi<%=rnd%> + '&all=true';
            const response = await fetch(urlTarget); const resData = await response.json();
            if (resData.status === 'success' && resData.data && resData.data.length > 0) {
                const notifArray = resData.data;
                let tableHtml = '<html xmlns:x="urn:schemas-microsoft-com:office:excel"><head><meta charset="UTF-8"><style>th { background-color: #0d6efd; color: white; font-weight: bold; border: 1px solid black; padding: 5px; } td { border: 1px solid black; padding: 5px; }</style></head><body>';
                tableHtml += '<h4><%=Common.getBahasaConfig("Laporan Riwayat Pemberitahuan Sistem")%></h4><p><%=Common.getBahasaConfig("Pengguna:")%> <b><%=tbmuser.getUserId()%></b></p><table><thead><tr><th><%=Common.getBahasaConfig("WAKTU")%></th><th><%=Common.getBahasaConfig("TIPE_PEMBERITAHUAN")%></th><th><%=Common.getBahasaConfig("RINCIAN_PESAN")%></th><th><%=Common.getBahasaConfig("STATUS_DIBACA")%></th></tr></thead><tbody>';
                notifArray.forEach(row => {
                    const statusBuka = row.isRead ? 'SUDAH DIBACA' : 'BELUM DIBACA';
                    tableHtml += '<tr><td style="text-align: center;">' + row.time + '</td><td style="text-align: center;">' + row.type + '</td><td>' + row.message + '</td><td style="text-align: center;">' + statusBuka + '</td></tr>';
                });
                tableHtml += '</tbody></table></body></html>';
                const blob = new Blob([tableHtml], { type: 'application/vnd.ms-excel' });
                const url = URL.createObjectURL(blob); const link = document.createElement("a");
                const timestamp = new Date().toISOString().slice(0, 10).replace(/-/g, "");
                link.download = "Riwayat_Notifikasi_" + timestamp + ".xls"; link.href = url;
                document.body.appendChild(link); link.click(); document.body.removeChild(link);
                setTimeout(() => URL.revokeObjectURL(url), 100);
            } else {
                if (typeof showToastUI === "function") { showToastUI('<%=Common.getBahasaConfigJS("Tidak ada data untuk diunduh.")%>', 'bg-warning text-dark'); } else { alert('<%=Common.getBahasaConfigJS("Tidak ada data untuk diunduh.")%>'); }
            }
        } catch (error) {
            console.error("Export Error:", error);
            if (typeof showToastUI === "function") { showToastUI('<%=Common.getBahasaConfigJS("Gagal mengunduh dokumen.")%>', 'bg-danger text-white'); } else { alert('<%=Common.getBahasaConfigJS("Gagal mengunduh dokumen.")%>'); }
        }
    };

    document.addEventListener("DOMContentLoaded", function() {
        injectModalNotifikasi<%=rnd%>();
        tarikNotifikasiBerkala<%=rnd%>();
        setInterval(tarikNotifikasiBerkala<%=rnd%>, 15000); 
    });
</script>
<% } %>
