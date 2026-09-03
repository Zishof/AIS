<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
// Inisialisasi variabel dengan nilai default untuk mencegah NullPointerException di UI
String judul = "";
String motto = "";
String telepon = "-";
String alamat1 = "-";
String email = "-";
String backgroundPerguruanTinggi = "";
String logoPerguruanTinggi = "";
String judulHeader = "eCampus";
String cssTema = "";

try {
    ais.database.model.sekolah.Sekolah sekolah = ais.action.master.sekolah.util.SekolahUtil.getSekolah(request);
    ais.database.model.sekolah.Yayasan yayasan = ais.action.master.sekolah.util.SekolahUtil.getYayasan(request);
    PerguruanTinggi pt = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request);

    // Pengambilan data Perguruan Tinggi
    if (pt != null) {
        cssTema = pt.getCss() != null ? pt.getCss() : "";
        judul = pt.getNama() != null ? pt.getNama() : "";
        telepon = pt.getTelepon() != null ? pt.getTelepon() : "-";
        alamat1 = pt.getAlamat1() != null ? pt.getAlamat1() : "-";
        email = pt.getEmail() != null ? pt.getEmail() : "-";
    }

    // Penentuan Background berdasarkan tipe perangkat
    if (Common.isMobile(request)) {
        backgroundPerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "banner_perguruanTinggi_");
    } else {
        backgroundPerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "background_perguruanTinggi_");
    }
    
    logoPerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");

    // Hierarki override data Institusi (Sekolah -> Yayasan)
    if (sekolah != null && sekolah.getId() != null) {
        judul = sekolah.getNama() != null ? sekolah.getNama() : judul;
        telepon = sekolah.getTelp() != null ? sekolah.getTelp() : telepon;
        alamat1 = sekolah.getAlamat() != null ? sekolah.getAlamat() : alamat1;
        email = sekolah.getEmail() != null ? sekolah.getEmail() : email;
        cssTema = sekolah.getCss() != null ? sekolah.getCss() : cssTema;
        
        String logoSekolah = ais.action.master.sekolah.util.SekolahUtil.getSekolahMedia(request, "logo_sekolah_");
        if (logoSekolah != null && !logoSekolah.endsWith("logo.png")) {
            logoPerguruanTinggi = logoSekolah;
        }
    } else if (yayasan != null && yayasan.getId() != null) {
        judul = yayasan.getNama() != null ? yayasan.getNama() : judul;
        telepon = yayasan.getTelp() != null ? yayasan.getTelp() : telepon;
        alamat1 = yayasan.getAlamat() != null ? yayasan.getAlamat() : alamat1;
        email = yayasan.getEmail() != null ? yayasan.getEmail() : email;
        
        String logoYayasan = ais.action.master.sekolah.util.SekolahUtil.getYayasanMedia(request, "logo_yayasan_");
        if (logoYayasan != null && !logoYayasan.endsWith("logo.png")) {
            logoPerguruanTinggi = logoYayasan;
        }
    }

    // Pengambilan Konfigurasi Judul Header
    Konfigurasi konfHeader = Common.getKonfigurasi("judul_header", "eCampus");
    if(konfHeader != null && konfHeader.getNilai() != null){
        judulHeader = konfHeader.getNilai();
    }

} catch (Exception e) {
    System.err.println("Kesalahan saat pengambilan data institusi: " + e.getMessage());
} finally {
    // Memastikan koneksi database selalu tertutup untuk mencegah memory leak
    try {
        ais.database.hibernate.HibernateUtil.closeSession(); // closeSession() aman dipanggil langsung; currentSession() justru membuka session baru
    } catch (Exception ex) {
        System.err.println("Gagal menutup session Hibernate: " + ex.getMessage());
    }
}

boolean hanyaTampilJsp = request.getParameter("hanya_tampil_jsp") != null && request.getParameter("hanya_tampil_jsp").trim().equalsIgnoreCase("true");
String p = request.getParameter("p") != null ? request.getParameter("p").trim() : "";
String s = request.getParameter("s") != null ? request.getParameter("s").trim() : "";

// Identifikasi status error login untuk trigger animasi peringatan
boolean isLoginError = request.getParameter("login_error") != null;

if (hanyaTampilJsp) {
    // PERBAIKAN KEAMANAN (task_1f9c66d3, rujuk r83764 webapp/WEB-INF/baru/tamu.jsp): sebelumnya p/s
    // diambil mentah tanpa daftar putih -- proksi anonim ke JSP layanan modul APA PUN. Inventarisasi
    // menyeluruh (grep atas seluruh webapp/ utk "hanya_tampil_jsp=true" dan "/login3?") TIDAK
    // menemukan satupun pemanggil sah utk dispatcher root login3.jsp ini. Karena tidak ada pasangan
    // p/s yang sah utk rute ini, daftar putih sengaja dikosongkan (deny-all) alih-alih
    // menebak/membuka kombinasi baru.
    boolean psDiizinkan = false;
    if (!p.isEmpty() && !s.isEmpty() && psDiizinkan) {
        try {
            String pg = "/WEB-INF/baru/modul/" + p + "/" + s + ".jsp";
%>
            <jsp:include page="<%=pg %>"></jsp:include>
<%
        } catch (Exception e) {
%>
            <jsp:include page="/WEB-INF/baru/componen/tidak_ketemu_page.jsp"></jsp:include>
<%
        }
    } else if (!p.isEmpty() && !s.isEmpty()) {
        response.sendError(403);
    }
} else {
    boolean aktifkanIntegrasiGoogle = false;
    try {
        Konfigurasi konfGoogle = Common.getKonfigurasi("aktifkan_integrasi_google", Konfigurasi.AKTIF);
        aktifkanIntegrasiGoogle = (konfGoogle != null && konfGoogle.getNilai().equalsIgnoreCase(Konfigurasi.AKTIF));
    } catch(Exception e) {
        System.err.println("Gagal memuat konfigurasi Google: " + e.getMessage());
    } finally {
        try {
            ais.database.hibernate.HibernateUtil.closeSession(); // closeSession() aman dipanggil langsung; currentSession() justru membuka session baru
        } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/login3.jsp:111");}
    }

    String linkLoginViaGoogle = Common.ROOT + "/google.zul";
    long vCache = System.currentTimeMillis();
%>
<!DOCTYPE html>
<html lang="<%=ais.common.Common.kodeBahasaAktif()%>" dir="<%= "ar".equals(ais.common.Common.kodeBahasaAktif()) ? "rtl" : "ltr" %>">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title><%= judulHeader + " | " + judul %></title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/animate.css/4.1.1/animate.min.css"/>

    <link href="<%=Common.ROOT%>/css/baru/base-login.css?v=<%=vCache%>" rel="stylesheet">
    <% if(!cssTema.isEmpty()){ %>
        
        <link href="<%=Common.ROOT%><%=cssTema%>?v=<%=vCache%>" rel="stylesheet">
    <% } %>

    <style>
        /* Gambar background dikelola via style karena menggunakan variabel server (JSP) */
        @media (min-width: 768px) {
            body.login-page {
                background-image: linear-gradient(rgba(0, 0, 0, 0.5), rgba(0, 0, 0, 0.75)), url('<%=backgroundPerguruanTinggi%>');
                background-size: cover;
                background-position: center;
                background-attachment: fixed;
            }
        }
        .contact-box {
            background: rgba(255, 255, 255, 0.15);
            backdrop-filter: blur(12px);
            border: 1px solid rgba(255, 255, 255, 0.2);
            border-radius: 12px;
            padding: 20px;
        }
        .btn-google {
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            border: 1px solid #dee2e6;
        }
        .btn-google:hover {
            background-color: #f8f9fa;
            transform: translateY(-2px);
            box-shadow: 0 6px 15px rgba(0,0,0,0.1) !important;
            border-color: #d3d9df;
        }
        .form-section {
            background-color: #ffffff;
        }
        
        /* Custom Keyframes untuk efek getar dan otentikasi masuk */
        @keyframes vibrateToSystem {
            0% { transform: translate(0) scale(1); opacity: 1; }
            10% { transform: translate(-3px, 3px) scale(0.99); }
            20% { transform: translate(3px, -3px) scale(0.99); }
            30% { transform: translate(-3px, -3px) scale(0.99); }
            40% { transform: translate(3px, 3px) scale(0.98); }
            50% { transform: translate(-2px, 2px) scale(0.98); }
            60% { transform: translate(2px, -2px) scale(0.97); opacity: 0.9; }
            80% { transform: translate(0) scale(0.95); opacity: 0.7; }
            100% { transform: translate(0) scale(0.9); opacity: 0.4; filter: blur(2px); }
        }
        .vibrate-active {
            animation: vibrateToSystem 1.5s cubic-bezier(.36,.07,.19,.97) forwards !important;
        }
    </style>
    <link rel="shortcut icon" href="<%=logoPerguruanTinggi%>" type="image/png" />
</head>

<body class="login-page">
<jsp:include page="/WEB-INF/baru/include/pemilih_bahasa.jsp" />

    <div class="login-wrapper d-flex align-items-center justify-content-center min-vh-100 p-3">
        <div id="main-login-container" class="desktop-container bg-white shadow-lg rounded-4 overflow-hidden d-flex flex-column flex-md-row animate__animated <%= isLoginError ? "animate__shakeX" : "animate__zoomIn" %> animate__faster" style="max-width: 950px; width: 100%;">
            
            <div class="header-section p-5 text-white d-flex flex-column justify-content-center position-relative" style="background: var(--bs-primary); flex: 1;">
                <div class="logo-box mb-4 text-center animate__animated animate__bounceIn animate__delay-1s">
                    <img src="<%=logoPerguruanTinggi%>" alt="<%= Common.getBahasaConfig("Logo Institusi") %>" class="img-fluid rounded-circle shadow-sm border border-2 border-white" style="max-width: 110px;" onerror="this.onerror=null; this.src='<%=Common.ROOT%>/img/default_logo.png';">
                </div>
                
                <div class="text-center mb-5">
                    <h4 class="fw-bold mb-2 lh-base"><%=judul%></h4>
                    <p class="fs-7 text-white-50 fst-italic mb-0"><%=motto%></p>
                </div>

                <div class="contact-box animate__animated animate__fadeInUp animate__delay-1s mt-auto">
                    <h6 class="fw-bold mb-3 border-bottom border-light border-opacity-25 pb-2 text-uppercase fs-7 letter-spacing-1">
                        <i class="fas fa-headset me-2"></i><%= Common.getBahasaConfig("Informasi Kontak") %>
                    </h6>
                    <div class="d-flex align-items-start mb-2 fs-7">
                        <i class="fas fa-map-marker-alt fa-fw me-3 mt-1 text-light opacity-75"></i> 
                        <span class="lh-sm"><%=alamat1%></span>
                    </div>
                    <div class="d-flex align-items-center mb-2 fs-7">
                        <i class="fas fa-phone-alt fa-fw me-3 text-light opacity-75"></i> 
                        <span><%=telepon%></span>
                    </div>
                    <div class="d-flex align-items-center fs-7">
                        <i class="fas fa-envelope fa-fw me-3 text-light opacity-75"></i> 
                        <span><%=email%></span>
                    </div>
                </div>
            </div>

            <div class="form-section p-4 p-md-5 d-flex flex-column justify-content-center" style="flex: 1.2;">
                <div class="text-center mb-4">
                    <h3 class="fw-bolder text-dark mb-2">
                        <i class="fas fa-sign-in-alt text-primary me-2"></i><%= Common.getBahasaConfig("Autentikasi Sistem") %>
                    </h3>
                    <p class="text-secondary fs-6 mb-0"><%= Common.getBahasaConfig("Selamat datang, silakan masuk menggunakan kredensial Anda.") %></p>
                </div>

                <% if (isLoginError) { %>
                <div class="alert alert-danger d-flex align-items-center p-3 mb-4 rounded-3 shadow-sm border-0 bg-danger bg-opacity-10" role="alert">
                    <i class="fas fa-exclamation-triangle text-danger me-3 fs-4"></i>
                    <small class="fw-medium text-danger"> 
                        <% 
                        try {
                            // Mencegah potensi celah XSS dengan Jsoup parser
                            out.println(org.jsoup.Jsoup.parse(request.getParameter("login_error")).text());
                        } catch (Exception e) {
                            out.print(Common.getBahasaConfig("Autentikasi gagal. Silakan periksa kembali ID Pengguna dan Kata Sandi Anda."));
                        }
                        %>
                    </small>
                </div>
                <% } %>

                <% if (aktifkanIntegrasiGoogle) { %>
                    <a href="<%=linkLoginViaGoogle%>" class="btn btn-white btn-google w-100 py-2 border shadow-sm rounded-3 fw-bold text-dark mb-3 d-flex align-items-center justify-content-center">
                        <i class="fab fa-google text-danger me-2 fs-5"></i>
                        <%= Common.getBahasaConfig("Masuk dengan Akun Google") %>
                    </a>
                    
                    <div class="d-flex align-items-center mb-4">
                        <hr class="flex-grow-1 text-muted opacity-25">
                        <span class="px-3 text-muted fs-7 fw-semibold"><%= Common.getBahasaConfig("Atau masuk menggunakan ID") %></span>
                        <hr class="flex-grow-1 text-muted opacity-25">
                    </div>
                <% } %>

                <form class="form w-100" novalidate="novalidate" method="post" id="kt_sign_in_form" action="j_spring_security_check">
                    <%=ais.common.Common.tampilanSocialLogin2(request, response)%>
                </form>

                <% if(request.getParameter("lupaPasswordUsername") == null){ %>
                <div class="text-center mt-3" id="lupaPasswordFormButtonContainer">
                    <a id="lupaPasswordFormButton" class="text-decoration-none text-primary fw-semibold fs-7" style="cursor: pointer;" 
                       onclick="document.getElementById('lupaPasswordForm').style.display='block'; document.getElementById('lupaPasswordFormButtonContainer').style.display='none'; return false;">
                        <i class="fas fa-unlock-alt me-1"></i> <%= Common.getBahasaConfig("Lupa Password?") %>
                    </a>
                </div>
                <% } %>

                <div id="lupaPasswordForm" class="mt-4 pt-3 border-top" <%= (request.getParameter("lupaPasswordUsername") == null ? "style='display:none'" : "style='display:block'") %>>
                    
                    <%
                        if(request.getParameter("lupaPasswordUsername") != null){
                            String hasil = ais.common.Common.kirimLupaPassword(request.getParameter("lupaPasswordUsername").trim());
                    %>
                            <div class="alert alert-info d-flex align-items-center p-3 mb-3 rounded-3 shadow-sm" style="font-size: 0.85rem;">
                                <i class="fas fa-info-circle fs-4 me-3 text-info"></i>
                                <div><%= hasil %></div>
                            </div>
                    <%
                        }
                    %>

                    <form name="formLupaPassword" action="" method="POST" class="form w-100">
                        <label class="form-label fs-8 fw-semibold text-muted mb-1"><%= Common.getBahasaConfig("Masukkan Username Anda :") %></label>
                        <div class="input-group input-group-sm mb-2 shadow-sm">
                            <span class="input-group-text bg-light border-0"><i class="fas fa-user text-muted"></i></span>
                            <input type="text" name="lupaPasswordUsername" class="form-control border-0 bg-light" placeholder="<%= Common.getBahasaConfig("Ketik username di sini...") %>" required />
                            <button type="submit" class="btn btn-primary px-3 fw-bold"><i class="fas fa-paper-plane me-1"></i> <%= Common.getBahasaConfig("Kirim") %></button>
                        </div>
                        
                        <% if(request.getParameter("lupaPasswordUsername") != null) { %>
                        <div class="text-center mt-3">
                            <a href="?" class="text-decoration-none text-primary fw-semibold fs-8">
                                <i class="fas fa-arrow-left me-1"></i> <%= Common.getBahasaConfig("Kembali ke Halaman Login") %>
                            </a>
                        </div>
                        <% } %>
                    </form>
                </div>
                <div class="text-center mt-auto pt-4">
                    <p class="mb-2 fw-bold text-secondary" style="font-size: 12px;">&copy; <%= java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) %> <%=judul%></p>
                    <div class="d-inline-flex align-items-center justify-content-center px-3 py-1 rounded-pill bg-primary bg-opacity-10 border border-primary border-opacity-25">
                        <i class="fas fa-shield-alt text-primary me-2" style="font-size: 11px;"></i>
                        <span class="text-primary fw-semibold" style="font-size: 11px;"><%= Common.getBahasaConfig("Sistem Terpadu dan Terenkripsi") %></span>
                    </div>
                </div>
            </div>

        </div>
    </div>

    <script>
        document.getElementById('kt_sign_in_form').addEventListener('submit', function(e) {
            const btn = document.getElementById('kt_sign_in_submit') || this.querySelector('button[type="submit"]');
            const container = document.getElementById('main-login-container');

            // 1. Tambahkan efek animasi otentikasi pada kontainer utama
            if (container) {
                // Hapus kelas animasi awal agar tidak saling tumpang tindih
                container.classList.remove('animate__zoomIn', 'animate__shakeX');
                container.style.animation = 'none'; // Paksa reset status animasi
                container.offsetHeight; /* Memicu reflow browser */
                container.style.animation = null;
                // Aplikasikan kelas animasi getar khusus
                container.classList.add('vibrate-active');
            }

            // 2. Transisi visual tombol untuk indikasi pemrosesan (loading)
            if(btn) {
                btn.classList.add('disabled');
                btn.style.pointerEvents = 'none';
                
                const label = btn.querySelector('.indicator-label');
                const progress = btn.querySelector('.indicator-progress');
                
                if (label) label.style.setProperty('display', 'none', 'important');
                if (progress) progress.style.setProperty('display', 'inline-block', 'important');
                
                // Mekanisme pemulihan (fallback) jika respons peladen tertunda melampaui 10 detik
                setTimeout(() => {
                    btn.classList.remove('disabled');
                    btn.style.pointerEvents = 'auto';
                    if (container) container.classList.remove('vibrate-active');
                    
                    if (label) label.style.setProperty('display', 'inline-block', 'important');
                    if (progress) progress.style.setProperty('display', 'none', 'important');
                }, 10000);
            }
        });
    </script>
    <jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp" />
</body>
</html>
<% 
} 
%>