<%@page import="java.util.List"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Tbmrole"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
// Inisialisasi variabel dengan nilai default untuk mencegah NullPointerException
String judul = "";
String motto = "";
String telepon = "";
String alamat1 = "";
String email = "";
String backgroundPerguruanTinggi = "";
String logoPerguruanTinggi = "";
String judulHeader = "eCampus";
String cssTema = "";

long rnd = System.currentTimeMillis();

try {
    PerguruanTinggi perguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request);
    
    if (perguruanTinggi != null) {
        judul = perguruanTinggi.getNama() != null ? perguruanTinggi.getNama() : "";
        motto = perguruanTinggi.getMotto() != null ? perguruanTinggi.getMotto() : "";
        telepon = perguruanTinggi.getTelepon() != null ? perguruanTinggi.getTelepon() : "";
        alamat1 = perguruanTinggi.getAlamat1() != null ? perguruanTinggi.getAlamat1() : "";
        email = perguruanTinggi.getEmail() != null ? perguruanTinggi.getEmail() : "";
        
        String rawCss = perguruanTinggi.getCss();
        if (rawCss != null && !rawCss.trim().isEmpty()) {
            String fileName = rawCss.substring(rawCss.lastIndexOf("/") + 1);
            cssTema = "/css/baru/" + fileName;
        }
    }

    backgroundPerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "background_perguruanTinggi_");
    logoPerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
    
    try {
        Konfigurasi konfHeader = Common.getKonfigurasi("judul_header", "eCampus");
        if(konfHeader != null && konfHeader.getNilai() != null) {
            judulHeader = konfHeader.getNilai();
        }
    } catch (Exception e) {
        System.err.println("Gagal mengambil konfigurasi judul_header: " + e.getMessage());
    }

} catch (Exception e) {
    System.err.println("Kesalahan sistem saat pengambilan data institusi: " + e.getMessage());
} finally {
    try {
        ais.database.hibernate.HibernateUtil.closeSession(); // closeSession() aman dipanggil langsung; currentSession() justru membuka session baru
    } catch (Exception ex) {
        System.err.println("Kesalahan saat menutup session Hibernate: " + ex.getMessage());
    }
}

// Logika pengambilan user dan roles sesuai revisi Anda
Tbmuser tbmuser = Common.getCurrentUser(request);
List<Tbmrole> tbmroles = tbmuser == null ? null : tbmuser.ambilRoles();

// Menangkap pesan error dari URL
String pesanError = request.getParameter("error");
%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title><%= Common.getBahasaConfig("Pilih Hak Akses") %> | <%= judul %></title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/animate.css/4.1.1/animate.min.css"/>

    <link href="<%=Common.ROOT%>/css/baru/base-login.css?v=<%=rnd%>" rel="stylesheet">
    <% if(!cssTema.isEmpty()){ %>
        <link href="<%=Common.ROOT%><%=cssTema%>?v=<%=rnd%>" rel="stylesheet">
    <% } %>

    <style>
        .body-layout-<%=rnd%> {
            background-color: #f0f2f5;
        }
        
        /* Gambar latar belakang utama halaman */
        @media (min-width: 768px) {
            .body-layout-<%=rnd%> {
                background-image: url('<%=backgroundPerguruanTinggi%>');
                background-size: cover;
                background-position: center;
                background-attachment: fixed;
            }
        }

        /* Latar belakang bagian kiri (Informasi) */
        .left-panel-<%=rnd%> {
            background: linear-gradient(135deg, rgba(13, 110, 253, 0.9) 0%, rgba(11, 94, 215, 0.95) 100%), url('<%=backgroundPerguruanTinggi%>');
            background-size: cover;
            background-position: center;
        }

        /* Animasi dan interaksi kartu role */
        .role-card-<%=rnd%> {
            transition: all 0.25s cubic-bezier(0.02, 0.01, 0.47, 1);
            border: 1px solid rgba(0,0,0,0.06);
            background-color: #ffffff;
        }
        .role-card-<%=rnd%>:hover {
            transform: translateY(-4px);
            box-shadow: 0 0.75rem 1.5rem rgba(0,0,0,0.08) !important;
            border-color: #0d6efd;
            background-color: #f8fbff;
        }
    </style>
    <link rel="shortcut icon" href="<%=logoPerguruanTinggi%>" type="image/png" />
</head>

<body class="body-layout-<%=rnd%> d-flex align-items-center justify-content-center min-vh-100 p-3 p-md-0">

    <div class="container">
        <div class="card border-0 shadow-lg overflow-hidden rounded-4 animate__animated animate__zoomIn animate__faster mx-auto" style="max-width: 1000px;">
            <div class="row g-0 align-items-stretch">
                
                <div class="col-lg-5 left-panel-<%=rnd%> text-white p-4 p-md-5 d-flex flex-column justify-content-between position-relative">
                    
                    <div class="text-center text-lg-start mb-4 animate__animated animate__fadeInDown">
                        <img src="<%=logoPerguruanTinggi%>" alt="Logo Institusi" class="img-fluid bg-white p-2 rounded-circle shadow-sm mb-4" style="width: 90px; height: 90px; object-fit: contain;" onerror="this.onerror=null; this.src='<%=Common.ROOT%>/img/default_logo.png';">
                        <h4 class="fw-bold mb-2 lh-base"><%=judul%></h4>
                        <p class="fs-7 text-white-50 fst-italic mb-0"><%=motto%></p>
                    </div>

                    <div class="mt-auto animate__animated animate__fadeInUp">
                        <div class="bg-white bg-opacity-10 rounded-4 p-4 backdrop-blur shadow-sm border border-white border-opacity-25">
                            <h6 class="fw-bold mb-3 pb-2 border-bottom border-white border-opacity-25 text-uppercase fs-8 text-white">
                                <i class="fas fa-headset me-2"></i><%= Common.getBahasaConfig("Pusat Informasi") %>
                            </h6>
                            <ul class="list-unstyled mb-0 small fs-8 text-white-50">
                                <li class="d-flex align-items-start mb-3">
                                    <i class="fas fa-map-marker-alt fa-fw me-3 mt-1 text-white"></i> 
                                    <span class="text-white"><%=alamat1%></span>
                                </li>
                                <li class="d-flex align-items-center mb-3">
                                    <i class="fas fa-phone-alt fa-fw me-3 text-white"></i> 
                                    <span class="text-white"><%=telepon%></span>
                                </li>
                                <li class="d-flex align-items-center">
                                    <i class="fas fa-envelope fa-fw me-3 text-white"></i> 
                                    <span class="text-white"><%=email%></span>
                                </li>
                            </ul>
                        </div>
                    </div>

                </div>

                <div class="col-lg-7 bg-white p-4 p-md-5 d-flex flex-column">
                    
                    <div class="mb-4 text-center text-lg-start">
                        <h4 class="fw-bolder text-dark mb-2">
                            <i class="fas fa-user-shield text-primary me-2"></i><%= Common.getBahasaConfig("Pilih Hak Akses") %>
                        </h4>
                        <p class="text-secondary small mb-0"><%= Common.getBahasaConfig("Silakan pilih peran pengguna yang ingin Anda gunakan untuk mengakses sistem.") %></p>
                    </div>

                    <% if (pesanError != null && !pesanError.trim().isEmpty()) { %>
                    <div class="alert alert-danger d-flex align-items-center p-3 mb-4 rounded-3 shadow-sm animate__animated animate__shakeX" role="alert">
                        <i class="fas fa-exclamation-triangle me-3 fs-5"></i>
                        <small class="fw-medium"><%= org.jsoup.Jsoup.parse(pesanError).text() %></small>
                    </div>
                    <% } %>

                    <div class="flex-grow-1 overflow-auto pe-2" style="max-height: 50vh;">
                        <% if (tbmuser != null && tbmroles != null && !tbmroles.isEmpty()) { %>
                            <div class="d-flex flex-column gap-3 pb-2">
                                <% for (Tbmrole tbmrole : tbmroles) { %>
                                    
                                    <div class="card rounded-4 shadow-sm role-card-<%=rnd%>">
                                        <div class="card-body p-3 p-md-4 d-flex align-items-center justify-content-between flex-wrap gap-3">
                                            
                                            <div class="d-flex align-items-center overflow-hidden flex-grow-1">
                                                <div class="flex-shrink-0 bg-primary bg-opacity-10 rounded-circle d-flex align-items-center justify-content-center" style="width: 50px; height: 50px;">
                                                    <i class="fas fa-user-check text-primary fs-4"></i>
                                                </div>
                                                <div class="ms-3 text-truncate">
                                                    <h6 class="mb-1 fw-bold text-dark text-truncate fs-6">
                                                        <%= tbmuser.getUserNama() %> 
                                                        <span class="text-muted fw-normal fs-8">(<%= tbmuser.getUserId() %>)</span>
                                                    </h6>
                                                    <span class="badge bg-primary bg-opacity-10 text-primary border border-primary-subtle rounded-pill fw-medium fs-8 px-3 py-1">
                                                        <i class="far fa-id-badge me-1"></i> <%= tbmrole.getRoleName() %>
                                                    </span>
                                                </div>
                                            </div>
                                            
                                            <div class="flex-shrink-0">
                                                <form method="post" action="<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=common&s=_pilih_hak_akses_service" class="m-0" onsubmit="return showLoadingAnimation(this)">
                                                    <input type="hidden" name="role_id" value="<%= tbmrole.getRoleId() %>">
                                                    <button type="submit" class="btn btn-primary rounded-pill px-4 py-2 fw-medium d-flex align-items-center btn-masuk-sistem shadow-sm">
                                                        <span class="indicator-label"><i class="fas fa-sign-in-alt me-2"></i><%= Common.getBahasaConfig("Masuk") %></span>
                                                        <span class="indicator-progress" style="display: none;">
                                                            <span class="spinner-border spinner-border-sm align-middle me-2"></span><%= Common.getBahasaConfig("Memproses...") %>
                                                        </span>
                                                    </button>
                                                </form>
                                            </div>

                                        </div>
                                    </div>

                                <% } %>
                            </div>
                        <% } else { %>
                            <div class="alert alert-warning border-warning border-opacity-25 bg-warning bg-opacity-10 text-center rounded-4 mb-4 d-flex flex-column align-items-center justify-content-center py-5 h-100" role="alert">
                                <div class="bg-warning bg-opacity-25 rounded-circle p-3 mb-3">
                                    <i class="fas fa-exclamation-triangle fs-1 text-warning"></i>
                                </div>
                                <h6 class="fw-bold text-dark"><%= Common.getBahasaConfig("Data Tidak Ditemukan") %></h6>
                                <span class="small text-muted mt-1 px-3"><%= Common.getBahasaConfig("Sistem tidak dapat menemukan data pengguna atau daftar hak akses yang sesuai untuk akun Anda.") %></span>
                            </div>
                        <% } %>
                    </div>

                    <div class="mt-4 pt-4 border-top d-flex flex-column flex-md-row align-items-center justify-content-between gap-3">
                        <a href="<%=Common.ROOT%>/main" class="btn btn-light rounded-pill px-4 fw-medium shadow-sm border text-secondary">
                            <i class="fas fa-arrow-left me-2"></i><%= Common.getBahasaConfig("Batal") %>
                        </a>
                        
                        <div class="text-center text-md-end">
                            <div class="d-inline-flex align-items-center px-3 py-1 bg-success bg-opacity-10 rounded-pill mb-2">
                                <i class="fas fa-shield-check text-success me-2" style="font-size: 0.75rem;"></i>
                                <span class="text-success fw-semibold" style="font-size: 0.75rem;"><%= Common.getBahasaConfig("Koneksi Aman & Terenkripsi") %></span>
                            </div>
                            <p class="mb-0 fw-medium text-muted" style="font-size: 0.7rem;">
                                &copy; <%= java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) %> <%=judulHeader%>
                            </p>
                        </div>
                    </div>

                </div>

            </div>
        </div>
    </div>

    <script>
        /**
         * Mencegah multiple submit dan memberikan feedback visual "Memproses..."
         */
        function showLoadingAnimation(form) {
            const btn = form.querySelector('.btn-masuk-sistem');
            if (btn) {
                btn.style.pointerEvents = 'none';
                btn.classList.add('disabled');
                
                const label = btn.querySelector('.indicator-label');
                const progress = btn.querySelector('.indicator-progress');
                
                if (label) label.style.setProperty('display', 'none', 'important');
                if (progress) progress.style.setProperty('display', 'inline-flex', 'important');
            }
            return true;
        }
    </script>
</body>
</html>