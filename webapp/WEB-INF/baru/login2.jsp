<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
// Pengecekan parameter untuk mode komponen AJAX
boolean hanya_tampil_jsp = request.getParameter("hanya_tampil_jsp") != null && request.getParameter("hanya_tampil_jsp").trim().equalsIgnoreCase("true");

// Cek apakah user sudah login sebelumnya (memiliki sesi aktif)
if (!hanya_tampil_jsp && (request.getUserPrincipal() != null || request.getRemoteUser() != null)) {
    response.sendRedirect(Common.ROOT + "/main");
    return; // Hentikan eksekusi kode JSP di bawahnya
}

boolean ptData = true;
boolean yaData = true;
try {
	boolean[] ptYa = Common.chekPtAtauSekolah(null);
	ptData = ptYa[0];
	yaData = ptYa[1];
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/login2.jsp:22");
} finally {
    // Blok finally untuk memastikan penutupan session atau pelepasan resource (jika ada pemanggilan koneksi database secara langsung).
}


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
    
 // MENGAMBIL SETTINGAN TEMA AKTIF
    String rawCss = pt != null ? pt.getCss() : null;
    if (rawCss != null && !rawCss.trim().isEmpty()) {
        String fileName = rawCss.substring(rawCss.lastIndexOf("/") + 1);
        cssTema = "/css/baru/" + fileName;
    }
    
    backgroundPerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "background_perguruanTinggi_");
    logoPerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
    try {
        judulHeader = Common.getKonfigurasi("judul_header", "eCampus").getNilai();
    } catch (Exception e) {
        judulHeader = "eCampus";
    }

    // Penentuan Background berdasarkan tipe perangkat
    if (Common.isMobile(request)) {
        backgroundPerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "banner_perguruanTinggi_");
    } else {
        backgroundPerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "background_perguruanTinggi_");
    }
    
	
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
        judulHeader = "eSchool";
        // MENGAMBIL SETTINGAN TEMA AKTIF
        rawCss = sekolah.getCss();
        if (rawCss != null && !rawCss.trim().isEmpty()) {
            String fileName = rawCss.substring(rawCss.lastIndexOf("/") + 1);
            cssTema = "/css/baru/" + fileName;
        }
    } else if (yaData && !ptData && yayasan != null && yayasan.getId() != null) {
        judul = yayasan.getNama() != null ? yayasan.getNama() : judul;
        telepon = yayasan.getTelp() != null ? yayasan.getTelp() : telepon;
        alamat1 = yayasan.getAlamat() != null ? yayasan.getAlamat() : alamat1;
        email = yayasan.getEmail() != null ? yayasan.getEmail() : email;
        
        String logoYayasan = ais.action.master.sekolah.util.SekolahUtil.getYayasanMedia(request, "logo_yayasan_");
        if (logoYayasan != null && !logoYayasan.endsWith("logo.png")) {
            logoPerguruanTinggi = logoYayasan;
        }
        

    }
}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/login2.jsp:107");}

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
    // PERBAIKAN KEAMANAN (task_1f9c66d3, rujuk r83764 webapp/WEB-INF/baru/tamu.jsp): sebelumnya p/s
    // diambil mentah tanpa daftar putih -- proksi anonim ke JSP layanan modul APA PUN. Inventarisasi
    // menyeluruh (grep atas seluruh webapp/ utk "hanya_tampil_jsp=true" dan "/login2?") TIDAK
    // menemukan satupun pemanggil sah utk dispatcher root login2.jsp ini. Karena tidak ada pasangan
    // p/s yang sah utk rute ini, daftar putih sengaja dikosongkan (deny-all) alih-alih
    // menebak/membuka kombinasi baru.
    boolean psDiizinkan = false;
    if(!p.trim().isEmpty() && !s.trim().isEmpty() && psDiizinkan){
          try{
              String pg = "/WEB-INF/baru/modul/"+p+"/"+s+".jsp";
                  %>
                  <jsp:include page="<%=pg %>"></jsp:include>
                  <%
          }catch(Exception e){
              e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/login2.jsp:131");
              %>
              <jsp:include page="/WEB-INF/baru/componen/tidak_ketemu_page.jsp"></jsp:include>
              <%
          }
    } else if(!p.trim().isEmpty() && !s.trim().isEmpty()){
        response.sendError(403);
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

    <!-- Library SweetAlert2 -->
    <script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>

    <link href="<%=Common.ROOT%>/css/baru/base-login.css?v=<%=vCache%>" rel="stylesheet">
    <% if(!cssTema.isEmpty()){ %>
        <link href="<%=Common.ROOT%><%=cssTema%>?v=<%=vCache%>" rel="stylesheet">
    <% } %>

    <style>
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
        <div id="loginBoxContainer" class="desktop-container animate__animated animate__zoomIn animate__faster">
            
            <div class="header-section">
                <div class="logo-box mx-auto animate__animated animate__bounceIn">
                    <img src="<%=logoPerguruanTinggi%>" alt="Logo" onerror="this.onerror=null; this.src='<%=Common.ROOT%>/img/logo.png';">
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
                <% 
                // Generate UI Login via method baru
                String htmlSocialLogin = ais.common.Common.tampilanSocialLogin3(request, response);
                
                // Jika method mengembalikan script SweetAlert2 (karena Remember Me),
                // maka sembunyikan form login dan biarkan script redirect berjalan.
                boolean isAutoLogin = htmlSocialLogin.contains("Swal.fire");
                
                if (isAutoLogin) { 
                %>
                    <div class="text-center mb-4 animate__animated animate__fadeIn">
                        <div class="mb-3 mt-4">
                            <i class="fas fa-circle-notch fa-spin text-primary" style="font-size: 3rem;"></i>
                        </div>
                        <h4 class="fw-bolder text-dark mb-2 mt-3">
                            <%= Common.getBahasaConfig("Sesi Ditemukan") %>
                        </h4>
                        <p class="text-secondary small mb-0"><%= Common.getBahasaConfig("Browser Anda mengingat akun ini. Harap tunggu...") %></p>
                    </div>
                    
                    <!-- Eksekusi script SweetAlert2 dari method -->
                    <%= htmlSocialLogin %>

                <% } else { %>
                    <!-- Form Login Normal -->
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

                    <div id="ajaxLoginAlert" class="alert alert-warning d-none align-items-start p-3 mb-4 rounded-3 shadow-sm animate__animated" role="alert">
                        <i class="fas fa-exclamation-circle me-3 fs-5 mt-1"></i>
                        <small id="ajaxLoginAlertMessage" class="fw-medium"></small>
                    </div>

                    <% if (aktifkan_integrasi_google) { %>
                        <a href="<%=linkLoginViaGoogle%>" class="btn btn-light w-100 py-2 border shadow-sm rounded-3 fw-bold text-dark mb-2 d-flex align-items-center justify-content-center">
                            <img src="https://upload.wikimedia.org/wikipedia/commons/c/c1/Google_%22G%22_logo.svg" alt="Google Logo" style="width: 20px; height: 20px; margin-right: 10px;">
                            <%=Common.getBahasaConfig("Masuk menggunakan akun Google")%>
                        </a>
                        <div class="separator-content my-4 text-muted">
                            <span class="bg-white px-3 fw-semibold fs-7"><%= Common.getBahasaConfig("Atau menggunakan ID Pengguna") %></span>
                        </div>
                    <% } %>

                    <!-- Form AJAX Login -->
                    <form class="form w-100" novalidate="novalidate" id="kt_sign_in_form">
                        <%= htmlSocialLogin %>
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
                <% } %> <!-- Tutup blok else isAutoLogin -->

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

    <!-- Script Pemrosesan AJAX -->
    <script>
        function showLoginWarning(message) {
            const alertBox = document.getElementById('ajaxLoginAlert');
            const alertMessage = document.getElementById('ajaxLoginAlertMessage');
            const finalMessage = message || '<%=Common.getBahasaConfigJS("Gagal masuk, periksa kembali ID Pengguna dan Kata Sandi Anda.")%>';

            if (alertMessage) {
                alertMessage.textContent = finalMessage;
            }
            if (alertBox) {
                alertBox.classList.remove('d-none');
                alertBox.classList.add('d-flex', 'animate__shakeX');
                setTimeout(() => {
                    alertBox.classList.remove('animate__shakeX');
                }, 900);
            }
        }

        function clearLoginWarning() {
            const alertBox = document.getElementById('ajaxLoginAlert');
            const alertMessage = document.getElementById('ajaxLoginAlertMessage');
            if (alertMessage) {
                alertMessage.textContent = '';
            }
            if (alertBox) {
                alertBox.classList.add('d-none');
                alertBox.classList.remove('d-flex', 'animate__shakeX');
            }
        }

        function shakeLoginBox() {
            const boxContainer = document.getElementById('loginBoxContainer');
            if (boxContainer) {
                boxContainer.classList.remove('animate__zoomIn', 'animate__faster');
                boxContainer.classList.add('animate__shakeX');
                setTimeout(() => {
                    boxContainer.classList.remove('animate__shakeX');
                }, 1000);
            }
        }

        function extractLoginMessageFromHtml(html) {
            if (!html) {
                return '';
            }

            try {
                const parser = new DOMParser();
                const doc = parser.parseFromString(html, 'text/html');
                const text = (doc.body && doc.body.innerText ? doc.body.innerText : html)
                    .replace(/\s+/g, ' ')
                    .trim();
                const lower = text.toLowerCase();
                const keywords = [
                    'akun anda tidak aktif',
                    'akun anda tidak bisa login',
                    'pengguna tidak ditemukan',
                    'tidak diizinkan',
                    'satuan kerja',
                    'diblokir',
                    'kuota',
                    'kata sandi',
                    'password',
                    'gagal login'
                ];

                for (let i = 0; i < keywords.length; i++) {
                    const idx = lower.indexOf(keywords[i]);
                    if (idx >= 0) {
                        let end = text.indexOf('.', idx);
                        if (end < 0 || end - idx > 260) {
                            end = Math.min(text.length, idx + 260);
                        }
                        return text.substring(idx, end).trim();
                    }
                }
            } catch (e) {
                // Abaikan parsing HTML dan gunakan pesan default di pemanggil.
            }
            return '';
        }

        async function prosesLogin(event) {
            event.preventDefault();

            const btn = document.getElementById('kt_sign_in_submit');
            const usernameInput = document.getElementById('username');
            const passwordInput = document.getElementById('password');

            const usernameVal = usernameInput ? usernameInput.value.trim() : '';
            const passwordVal = passwordInput ? passwordInput.value : '';

            clearLoginWarning();

            if (!usernameVal || !passwordVal) {
                const emptyMessage = '<%=Common.getBahasaConfigJS("Nama pengguna dan kata sandi tidak boleh kosong.")%>';
                showLoginWarning(emptyMessage);
                shakeLoginBox();
                if (!usernameVal && usernameInput) {
                    usernameInput.focus();
                } else if (passwordInput) {
                    passwordInput.focus();
                }
                return;
            }

            if (btn) {
                btn.disabled = true;
            }

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
                const payload = new URLSearchParams();
                payload.append('username', usernameVal);
                payload.append('password', passwordVal);

                const rememberMeCheckbox = document.getElementById('checkbox');
                if (rememberMeCheckbox && rememberMeCheckbox.checked) {
                    payload.append('rememberMe', 'true');
                }

                const response = await fetch('<%=Common.ROOT%>/login?action=ajax_login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
                    credentials: 'include',
                    body: payload.toString()
                });

                const responseText = await response.text();
                let result = null;
                try {
                    result = JSON.parse(responseText);
                } catch (parseError) {
                    result = {
                        status: 'error',
                        message: extractLoginMessageFromHtml(responseText) || '<%=Common.getBahasaConfigJS("Gagal memproses respon login. Silakan coba lagi.")%>'
                    };
                }

                if (!response.ok && result.status === 'success') {
                    result.status = 'error';
                    result.message = '<%=Common.getBahasaConfigJS("Login gagal diproses oleh server. Silakan coba lagi.")%>';
                }

                if (result.status === 'success') {
                    const rememberMeCheckbox = document.getElementById('checkbox');
                    if (rememberMeCheckbox && rememberMeCheckbox.checked && result.cookie_val) {
                        const maxAge = 15552000;
                        document.cookie = 'userinfo=' + encodeURIComponent(result.cookie_val) + '; max-age=' + maxAge + '; path=/';
                        document.cookie = 'userid=' + encodeURIComponent(usernameVal) + '; max-age=' + maxAge + '; path=/';
                    }

                    Swal.fire({
                        icon: 'success',
                        title: '<%=Common.getBahasaConfigJS("Berhasil!")%>',
                        text: result.message || '<%=Common.getBahasaConfigJS("Otentikasi berhasil. Mengalihkan ke dasbor...")%>',
                        showConfirmButton: false,
                        timer: 1200,
                        showClass: { popup: 'animate__animated animate__zoomIn' },
                        hideClass: { popup: 'animate__animated animate__zoomOut' }
                    }).then(() => {
                        window.location.replace(result.redirect || '<%=Common.ROOT%>/main');
                    });
                } else {
                    const errorMessage = result.message || '<%=Common.getBahasaConfigJS("Nama pengguna atau kata sandi tidak valid. Silakan periksa kembali.")%>';
                    showLoginWarning(errorMessage);

                    Swal.fire({
                        icon: 'warning',
                        title: '<%=Common.getBahasaConfigJS("Login Belum Berhasil")%>',
                        text: errorMessage,
                        confirmButtonText: '<%=Common.getBahasaConfigJS("Coba Lagi")%>',
                        confirmButtonColor: '#dc3545'
                    });

                    shakeLoginBox();
                    if (passwordInput) {
                        passwordInput.focus();
                        passwordInput.select();
                    }
                    if (btn) {
                        btn.disabled = false;
                    }
                }

            } catch (error) {
                const networkMessage = '<%=Common.getBahasaConfigJS("Gagal terhubung ke peladen. Periksa koneksi Anda atau coba beberapa saat lagi.")%>';
                showLoginWarning(networkMessage);
                Swal.fire({
                    icon: 'warning',
                    title: '<%=Common.getBahasaConfigJS("Kesalahan Jaringan")%>',
                    text: networkMessage,
                    confirmButtonColor: '#0d6efd'
                });

                if (btn) {
                    btn.disabled = false;
                }
            }
        }

        const loginForm = document.getElementById('kt_sign_in_form');
        if (loginForm) {
            loginForm.addEventListener('submit', prosesLogin);
        }
    </script>
    <jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp" />
</body>
</html>
<% 
} 
%>