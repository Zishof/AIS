<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
// Inisialisasi variabel dengan nilai default untuk mencegah NullPointerException di UI
String judul = "";
String motto = "";
String telepon = "";
String alamat1 = "";
String email = "";
String backgroundPerguruanTinggi = "";
String logoPerguruanTinggi = "";
String judulHeader = "";
String cssTema = ""; // Variabel untuk menampung file CSS Tema aktif

try {
    PerguruanTinggi perguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request);
    
    if (perguruanTinggi != null) {
        judul = perguruanTinggi.getNama() != null ? perguruanTinggi.getNama() : "";
        motto = perguruanTinggi.getMotto() != null ? perguruanTinggi.getMotto() : "";
        telepon = perguruanTinggi.getTelepon() != null ? perguruanTinggi.getTelepon() : "";
        alamat1 = perguruanTinggi.getAlamat1() != null ? perguruanTinggi.getAlamat1() : "";
        email = perguruanTinggi.getEmail() != null ? perguruanTinggi.getEmail() : "";
        
        // MENGAMBIL SETTINGAN TEMA AKTIF
        String rawCss = perguruanTinggi.getCss();
        if (rawCss != null && !rawCss.trim().isEmpty()) {
            String fileName = rawCss.substring(rawCss.lastIndexOf("/") + 1);
            cssTema = "/css/baru/" + fileName;
        }
    }

    backgroundPerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "background_perguruanTinggi_");
    logoPerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
    try {
        judulHeader = Common.getKonfigurasi("judul_header", "eCampus").getNilai();
    } catch (Exception e) {
        judulHeader = "eCampus";
    }

} catch (Exception e) {
    System.err.println("Kesalahan pengambilan data institusi: " + e.getMessage());
} finally {
    ais.database.hibernate.HibernateUtil.closeSession(); // closeSession() aman dipanggil langsung; currentSession() justru membuka session baru
}
boolean hanya_tampil_jsp = request.getParameter("hanya_tampil_jsp") != null && request.getParameter("hanya_tampil_jsp").trim().equalsIgnoreCase("true");

String p = "";
if(request.getParameter("p") != null && !request.getParameter("p").trim().isEmpty()){
    p = request.getParameter("p").trim();
}
String s = "";
if(request.getParameter("s") != null && !request.getParameter("s").trim().isEmpty()){
    s = request.getParameter("s").trim();
}

String urlLama = "";
if(request.getParameter("urlLama") != null && !request.getParameter("urlLama").trim().isEmpty()){
    urlLama = request.getParameter("urlLama").trim();
}

if(hanya_tampil_jsp){
    if(!p.trim().isEmpty() && !s.trim().isEmpty()){
          try{
              String pg = "/WEB-INF/baru/modul/"+p+"/"+s+".jsp";
                  %>
                  <jsp:include page="<%=pg %>"></jsp:include>
                  <%
          }catch(Exception e){
              e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/login5.jsp:72");
              %>
              <jsp:include page="/WEB-INF/baru/componen/tidak_ketemu_page.jsp"></jsp:include>
              <%
          }
    }
} else {
    boolean aktifkan_integrasi_google = Common.getKonfigurasi("aktifkan_integrasi_google", Konfigurasi.AKTIF).getNilai().equalsIgnoreCase(Konfigurasi.AKTIF);
    String linkLoginViaGoogle = Common.ROOT+"/google.zul"; 
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
    <script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>

    <link href="<%=Common.ROOT%>/css/baru/base-login.css?v=<%=vCache%>" rel="stylesheet">
    <% if(!cssTema.isEmpty()){ %>
        <link href="<%=Common.ROOT%><%=cssTema%>?v=<%=vCache%>" rel="stylesheet">
    <% } %>

    <style>
        /* Gambar background dikelola via style karena menggunakan variabel server (JSP) */
        @media (min-width: 768px) {
            body.login-page {
                background-image: url('<%=backgroundPerguruanTinggi%>');
                background-size: cover;
                background-position: center;
                background-attachment: fixed;
            }
        }
    </style>
    <link rel="shortcut icon" href="<%=logoPerguruanTinggi%>" type="image/png" />
</head>

<body class="login-page">
<jsp:include page="/WEB-INF/baru/include/pemilih_bahasa.jsp" />

    <div class="login-wrapper">
        <div id="loginBoxContainer" class="desktop-container position-relative animate__animated animate__zoomIn animate__faster">
            
            <img src="https://cdni.iconscout.com/illustration/premium/thumb/students-sitting-on-books-and-reading-4620436-3833055.png" 
                 alt="Mahasiswa" 
                 class="animate__animated animate__pulse animate__infinite animate__slower d-none d-md-block" 
                 style="position: absolute; top: -85px; left: -60px; width: 260px; z-index: 1050; pointer-events: none; filter: drop-shadow(0 15px 15px rgba(0,0,0,0.4));"
                 onerror="this.style.display='none'">

            <div class="header-section">
                <div class="logo-box mx-auto animate__animated animate__bounceIn">
                    <img src="<%=logoPerguruanTinggi%>" alt="Logo" onerror="this.onerror=null; this.src='<%=Common.ROOT%>/img/default_logo.png';">
                </div>
                
                <h5 class="fw-bold mb-1 fs-5"><%=judul%></h5>
                <p class="fs-8 text-white-50 fst-italic mb-0"><%=motto%></p>

                <div class="contact-box shadow-sm animate__animated animate__fadeInUp">
                    <h6 class="fw-bold mb-3 border-bottom border-light border-opacity-25 pb-2 text-uppercase fs-8">
                        <i class="fas fa-headset me-2"></i><%= Common.getBahasaConfig("Hubungi Kami") %>
                    </h6>
                    <div class="d-flex align-items-start mb-2 fs-8">
                        <i class="fas fa-map-marker-alt fa-fw me-2 mt-1 opacity-75"></i> <span><%=alamat1%></span>
                    </div>
                    <div class="d-flex align-items-center mb-2 fs-8">
                        <i class="fas fa-phone-alt fa-fw me-2 opacity-75"></i> <span><%=telepon%></span>
                    </div>
                    <div class="d-flex align-items-center fs-8">
                        <i class="fas fa-envelope fa-fw me-2 opacity-75"></i> <span><%=email%></span>
                    </div>
                </div>
            </div>

            <div class="form-section">
                <div class="text-center mb-4">
                    <h4 class="fw-bolder text-dark mb-2">
                        <i class="fas fa-sign-in-alt text-primary me-2"></i><%= Common.getBahasaConfig("Masuk") %> <%=judulHeader%>
                    </h4>
                    <p class="text-secondary small mb-0"><%= Common.getBahasaConfig("Selamat datang kembali, silakan masuk ke akun Anda.") %></p>
                </div>

                <% if (request.getParameter("login_error") != null) { %>
                <div class="alert alert-danger d-flex align-items-center p-3 mb-4 rounded-3 shadow-sm animate__animated animate__shakeX" role="alert">
                    <i class="fas fa-exclamation-triangle me-3 fs-5"></i>
                    <small class="fw-medium"> 
                        <% 
                        try {
                            out.println(org.jsoup.Jsoup.parse(request.getParameter("login_error")).text());
                        } catch (Exception e) {
                            out.print(Common.getBahasaConfig("Gagal masuk, periksa kembali ID Pengguna dan Kata Sandi Anda."));
                        }
                        %>
                    </small>
                </div>
                <% } %>

                <% if (aktifkan_integrasi_google) { %>
                    <a href="<%=linkLoginViaGoogle%>" class="btn btn-light w-100 py-2 border shadow-sm rounded-3 fw-bold text-dark mb-2 d-flex align-items-center justify-content-center">
                        <img src="https://upload.wikimedia.org/wikipedia/commons/c/c1/Google_%22G%22_logo.svg" alt="Google Logo" style="width: 20px; height: 20px; margin-right: 10px;">
                        <%=Common.getBahasaConfig("Masuk menggunakan akun Google")%>
                    </a>
                    
                    <div class="separator-content my-4 text-muted">
                        <span class="bg-white px-3 fw-semibold fs-7"><%= Common.getBahasaConfig("Atau menggunakan ID Pengguna") %></span>
                    </div>
                <% } %>

                <form class="form w-100" id="kt_sign_in_form" onsubmit="prosesLogin(event)">
                    <div class="input-group mb-3 shadow-sm rounded-3">
                        <span class="input-group-text bg-light border-0"><i class="fas fa-user text-muted"></i></span>
                        <input type="text" id="username" name="username" class="form-control border-0 bg-light py-2" placeholder="<%=Common.getBahasaConfig("Nomor Induk / Nama Pengguna")%>" required autocomplete="off">
                    </div>
                    
                    <div class="input-group mb-4 shadow-sm rounded-3">
                        <span class="input-group-text bg-light border-0"><i class="fas fa-lock text-muted"></i></span>
                        <input type="password" id="password" name="password" class="form-control border-0 bg-light py-2" placeholder="<%=Common.getBahasaConfig("Kata Sandi")%>" required>
                    </div>

                    <button type="submit" id="kt_sign_in_submit" class="btn btn-primary w-100 py-2 fw-bold text-uppercase d-flex justify-content-center align-items-center shadow-sm">
                        <span class="indicator-label" id="btnTextLabel"><i class="fas fa-sign-in-alt me-2"></i> <%= Common.getBahasaConfig("Masuk") %></span>
                    </button>
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
                <div class="text-center mt-auto pt-4 border-top mt-4">
                    <p class="mb-1 fw-bold text-secondary" style="font-size: 11px;">&copy; <%= java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) %> <%=judul%></p>
                    <div class="d-inline-flex align-items-center justify-content-center px-3 py-1 rounded-pill bg-primary bg-opacity-10 mt-1">
                        <i class="fas fa-shield-alt text-primary me-2" style="font-size: 10px;"></i>
                        <span class="text-primary fw-semibold" style="font-size: 10px;"><%=Common.getBahasaConfig("Sistem Informasi Terpadu & Terenkripsi")%></span>
                    </div>
                </div>
            </div>

        </div>
    </div>

    <script>
        async function prosesLogin(event) {
            event.preventDefault();
            
            const btn = document.getElementById('kt_sign_in_submit');
            const boxContainer = document.getElementById('loginBoxContainer');
            
            const usernameVal = document.getElementById('username').value;
            const passwordVal = document.getElementById('password').value;

            // Nonaktifkan tombol agar tidak diklik berkali-kali
            btn.disabled = true;

            // Munculkan Pop-up Loading dari SweetAlert
            Swal.fire({
                title: '<%=Common.getBahasaConfigJS("Memverifikasi...")%>',
                html: '<%=Common.getBahasaConfigJS("Mohon tunggu sebentar, sedang memproses otentikasi akun Anda.")%>',
                allowOutsideClick: false,
                allowEscapeKey: false,
                showConfirmButton: false,
                didOpen: () => {
                    Swal.showLoading();
                }
            });

            try {
                // Menyiapkan Data POST
                const payload = new URLSearchParams();
                payload.append('username', usernameVal);
                payload.append('password', passwordVal);

                // Kirim request ke backend Pustaka Service
                const response = await fetch('<%=Common.ROOT%>/pustaka?hanya_tampil_jsp=true&p=pustaka&s=_login_pustaka_service', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: payload.toString()
                });

                const result = await response.json();

                if (result.status === 'success') {
                    // Animasi Sukses (Ganti konten Pop-up Loading menjadi Sukses)
                    Swal.fire({
                        icon: 'success',
                        title: '<%=Common.getBahasaConfigJS("Berhasil!")%>',
                        text: result.message,
                        showConfirmButton: false,
                        timer: 1500,
                        showClass: { popup: 'animate__animated animate__zoomIn' },
                        hideClass: { popup: 'animate__animated animate__zoomOut' }
                    });

                    // Redirect ke /main
                    setTimeout(() => {
                        window.location.href = '<%=Common.ROOT%>/main';
                    }, 1500);

                } else {
                    // Animasi Gagal (Ganti konten Pop-up Loading menjadi Error)
                    Swal.fire({
                        icon: 'error',
                        title: '<%=Common.getBahasaConfigJS("Gagal Masuk")%>',
                        text: result.message,
                        confirmButtonText: 'Coba Lagi',
                        confirmButtonColor: '#dc3545'
                    });

                    // Efek Shake pada box form desktop
                    boxContainer.classList.remove('animate__zoomIn', 'animate__faster');
                    boxContainer.classList.add('animate__shakeX');
                    
                    // Reset animasi shake agar bisa dimainkan lagi jika error kembali
                    setTimeout(() => {
                        boxContainer.classList.remove('animate__shakeX');
                    }, 1000);

                    // Aktifkan kembali tombol
                    btn.disabled = false;
                }

            } catch (error) {
                // Penanganan Error Server/Internet Mati
                Swal.fire({
                    icon: 'warning',
                    title: 'Kesalahan Jaringan',
                    text: '<%=Common.getBahasaConfigJS("Gagal terhubung ke peladen. Periksa koneksi Anda.")%>',
                    confirmButtonColor: '#0d6efd'
                });

                // Aktifkan kembali tombol
                btn.disabled = false;
            }
        }
    </script>
    <jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp" />
</body>
</html>
<% 
} 
%>