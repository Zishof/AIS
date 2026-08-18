<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
// =================================================================================
// LOGIKA PENGAMBILAN DATA INSTITUSI & TEMA
// =================================================================================
String judul = "";
String motto = "";
String telepon = "";
String alamat1 = "";
String email = "";
String backgroundPerguruanTinggi = "";
String logoPerguruanTinggi = "";
String judulHeader = "";
String cssTema = ""; 

try {
    PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);
    
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

    backgroundPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "background_perguruanTinggi_");
    logoPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
    
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

long cacheBuster = System.currentTimeMillis();
String rnd = Common.getGeneratedBarCode(7);

// Link URL untuk Iframe Pendaftaran Mahasiswa Baru
String linkServicePMB = Common.ROOT + "/pmb";
%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= Common.getBahasaConfig("Anjungan Layanan Mandiri") %> | <%=judul%></title>
    
    <link rel="icon" href="<%=logoPerguruanTinggi%>" type="image/x-icon">
    
    <!-- Pustaka Gaya & Ikon -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
    
    <!-- PUSTAKA PEMINDAI QR CODE KAMERA -->
    <script data-cfasync="false" src="https://unpkg.com/html5-qrcode" type="text/javascript"></script>

    <!-- MENGIMPOR TEMA GENERAL DAN CSS KHUSUS ANJUNGAN -->
    <link href="<%=request.getContextPath() %>/css/baru/base-theme.css?v=<%= cacheBuster %>" rel="stylesheet">
    <link href="<%=request.getContextPath() %>/css/baru/base-anjungan.css?v=<%= cacheBuster %>" rel="stylesheet">
    
    <% if (!cssTema.isEmpty()) { %>
        <link href="<%=request.getContextPath() %><%=cssTema%>?v=<%= cacheBuster %>" rel="stylesheet">
    <% } %>

    <style>
        .glass-card {
            background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.8) 100%), url('<%=backgroundPerguruanTinggi%>') center/cover no-repeat !important;
            backdrop-filter: blur(15px); border: 1px solid rgba(255, 255, 255, 0.6); border-radius: 24px;
            padding: 4rem 3rem; text-align: center; box-shadow: 0 20px 50px rgba(0,0,0,0.15);
            max-width: 650px; width: 100%; animation: fadeInUp 0.8s ease-out; position: relative; overflow: hidden;
        }

        .modal-content.glass-modal {
            background: linear-gradient(135deg, rgba(255, 255, 255, 0.98) 0%, rgba(248, 250, 252, 0.95) 100%), url('<%=backgroundPerguruanTinggi%>') center/cover no-repeat !important;
            backdrop-filter: blur(20px); border-radius: 24px; border: 1px solid rgba(255,255,255,0.8); box-shadow: 0 30px 60px rgba(0,0,0,0.3);
        }

        .footer-info {
            position: absolute; bottom: 20px; left: 0; width: 100%; text-align: center;
            color: var(--bs-heading-color); text-shadow: 1px 1px 4px rgba(0,0,0,0.6); z-index: 10;
        }

        /* TOMBOL UTAMA ANJUNGAN */
        .btn-masuk-anjungan, .btn-pmb-anjungan {
            display: inline-flex; align-items: center; justify-content: center;
            color: white; font-weight: 700; font-size: 1.1rem; padding: 12px 28px; border-radius: 50px;
            border: 2px solid rgba(255, 255, 255, 0.5); transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            text-decoration: none; width: 100%; cursor: pointer; gap: 8px;
        }
        
        .btn-masuk-anjungan { background: linear-gradient(135deg, #1a4087 0%, #355c9e 100%); box-shadow: 0 10px 20px rgba(26, 64, 135, 0.3); }
        .btn-pmb-anjungan { background: linear-gradient(135deg, #10b981 0%, #059669 100%); box-shadow: 0 10px 20px rgba(16, 185, 129, 0.3); }

        .btn-masuk-anjungan:hover { transform: translateY(-5px) scale(1.02); box-shadow: 0 15px 30px rgba(26, 64, 135, 0.4); border-color: white; color: white;}
        .btn-pmb-anjungan:hover { transform: translateY(-5px) scale(1.02); box-shadow: 0 15px 30px rgba(16, 185, 129, 0.4); border-color: white; color: white;}

        /* MODAL PMB */
        .modal-pmb-dialog { max-width: 98%; width: 98%; height: 98vh; margin: 1vh auto; }
        .modal-pmb-content { height: 100%; background: rgba(255, 255, 255, 0.95); backdrop-filter: blur(20px); border-radius: 20px; border: 1px solid rgba(255, 255, 255, 0.5); overflow: hidden; display: flex; flex-direction: column; }
        .modal-pmb-header { background: linear-gradient(90deg, #1a4087 0%, #355c9e 100%); color: white; padding: 1rem 1.5rem; display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid rgba(255,255,255,0.1); }
        .modal-pmb-body { flex-grow: 1; padding: 0; margin: 0; position: relative; }
        .pmb-iframe { width: 100%; height: 100%; border: none; background: #ffffff; }
        
        .modal.fade .modal-pmb-dialog { transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); transform: scale(0.9) translateY(50px); opacity: 0; }
        .modal.show .modal-pmb-dialog { transform: scale(1) translateY(0); opacity: 1; }

        /* KUSTOMISASI MODAL LOGIN & TABS */
        .modal-login-dialog { max-width: 600px; }
        
        .login-tabs-wrapper {
            background: #f1f5f9;
            border-radius: 50px;
            padding: 6px;
            display: inline-flex;
            box-shadow: inset 0 2px 5px rgba(0,0,0,0.05);
            border: 1px solid rgba(0,0,0,0.05);
            margin-bottom: 1.5rem;
        }

        .nav-pills.login-tabs .nav-link { 
            color: #64748b; 
            border-radius: 50px; 
            padding: 10px 24px; 
            font-weight: 700; 
            font-size: 1.05rem;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px; /* Jarak proporsional ikon dan teks */
            transition: all 0.3s ease;
            white-space: nowrap;
        }

        /* OVERRIDE PAKSA UNTUK IKON FONT-AWESOME AGAR TIDAK BENTROK */
        .nav-pills.login-tabs .nav-link i {
            position: static !important;
            font-size: 1.25rem !important;
            transform: none !important;
            opacity: 1 !important;
            margin: 0 !important; 
        }

        .nav-pills.login-tabs .nav-link.active { 
            background: linear-gradient(135deg, #1a4087 0%, #355c9e 100%); 
            color: white !important; 
            box-shadow: 0 4px 12px rgba(26, 64, 135, 0.3); 
        }
        .nav-pills.login-tabs .nav-link:hover:not(.active) {
            background: rgba(26, 64, 135, 0.1);
            color: #1a4087;
        }

        /* CAMERA & SCANNER BOX */
        .camera-wrapper {
            border: 2px dashed rgba(26, 64, 135, 0.3);
            border-radius: 16px;
            padding: 8px;
            background: #ffffff;
        }

        .scanner-box {
            background-color: #f8fafc;
            background-image: radial-gradient(rgba(26, 64, 135, 0.1) 1px, transparent 1px);
            background-size: 20px 20px;
            border-radius: 16px;
            border: 2px solid rgba(26, 64, 135, 0.1);
            transition: all 0.3s ease;
        }
        .scanner-box:focus-within {
            border-color: #355c9e;
            box-shadow: 0 0 0 4px rgba(53, 92, 158, 0.1);
        }
    </style>
</head>
<body>

    <video autoplay loop muted playsinline class="video-background" id="bg-video">
        <source src="<%=Common.ROOT%>/img/video_background_anjungan.mp4" type="video/mp4">
    </video>

    <div class="overlay-cerah"></div>

    <div class="anjungan-wrapper">
        <div class="glass-card">
            <img src="<%=logoPerguruanTinggi%>" alt="Logo Institusi" class="anjungan-logo">
            <h1 class="anjungan-title"><%=judul%></h1>
            <p class="anjungan-subtitle fst-italic mb-2 text-primary fw-bold"><%=motto%></p>
            <p class="text-secondary mb-4"><%= Common.getBahasaConfig("Pusat Layanan Administrasi Akademik & Informasi Terpadu Mandiri") %></p>
            
            <div class="d-flex flex-column align-items-center gap-3 w-100 mt-4 mx-auto" style="max-width: 350px;">
                <button type="button" class="btn-masuk-anjungan" data-bs-toggle="modal" data-bs-target="#loginAnjunganModal" onclick="startQRScanner()">
                    <i class="fas fa-fingerprint"></i> <%= Common.getBahasaConfig("Mulai Sesi Anda") %>
                </button>
                
                <button type="button" class="btn-pmb-anjungan" onclick="bukaModalPMB()">
                    <i class="fas fa-user-graduate"></i> <%= Common.getBahasaConfig("Pendaftaran Mahasiswa Baru") %>
                </button>
            </div>
            
            <div class="mt-4 text-muted small fw-semibold">
                <i class="fas fa-shield-alt text-success me-1"></i> <%=judulHeader%> - Sistem Terenkripsi dan Terlindungi
            </div>
        </div>
    </div>

    <div class="footer-info d-none d-md-block">
        <p class="mb-1 fw-bold fs-5"><%=judulHeader%> - <%=judul%></p>
        <p class="mb-0 small fw-semibold">
            <i class="fas fa-map-marker-alt text-warning me-1"></i> <%=alamat1%> &nbsp;|&nbsp; 
            <i class="fas fa-phone-alt text-success me-1"></i> <%=telepon%> &nbsp;|&nbsp; 
            <i class="fas fa-envelope text-info me-1"></i> <%=email%>
        </p>
    </div>

    <!-- MODAL POPUP LOGIN -->
    <div class="modal fade" id="loginAnjunganModal" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
        <div class="modal-dialog modal-dialog-centered modal-login-dialog">
            <div class="modal-content glass-modal">
                <div class="modal-header-custom border-bottom-0 pb-0 text-center">
                    <button type="button" class="btn-close position-absolute top-0 end-0 m-4" data-bs-dismiss="modal" aria-label="Tutup" onclick="stopQRScanner()"></button>
                    
                    <h4 class="modal-title-custom mb-4 mt-2 w-100"><%= Common.getBahasaConfig("Otentikasi Pengguna") %></h4>
                    
                    <!-- TAB NAVIGASI EYE-CATCHING -->
                    <div class="text-center w-100">
                        <div class="login-tabs-wrapper">
                            <ul class="nav nav-pills login-tabs" id="loginTabs" role="tablist">
                                <li class="nav-item" role="presentation">
                                    <button class="nav-link active" id="qr-tab" data-bs-toggle="tab" data-bs-target="#qr-pane" type="button" role="tab" onclick="startQRScanner()">
                                        <i class="fas fa-qrcode"></i> Pindai QR
                                    </button>
                                </li>
                                <li class="nav-item" role="presentation">
                                    <button class="nav-link" id="manual-tab" data-bs-toggle="tab" data-bs-target="#manual-pane" type="button" role="tab" onclick="stopQRScanner(); setTimeout(() => document.getElementById('username<%=rnd%>').focus(), 300);">
                                        <i class="fas fa-keyboard"></i> Ketik Manual
                                    </button>
                                </li>
                            </ul>
                        </div>
                    </div>
                </div>
                
                <div class="modal-body px-4 pb-5 pt-0">
                    <div id="alertBox<%=rnd%>" class="alert-custom"></div>

                    <div class="tab-content" id="loginTabsContent">
                        
                        <!-- PANEL 1: LOGIN SCAN QR CODE & SCANNER FISIK -->
                        <div class="tab-pane fade show active text-center" id="qr-pane" role="tabpanel">
                            
                            <!-- Kamera Web -->
                            <div class="camera-wrapper shadow-sm mb-4">
                                <div id="qr-reader" style="width: 100%; border-radius: 12px; overflow: hidden; min-height: 150px;"></div>
                            </div>
                            
                            <!-- Area Tangkapan Scanner Fisik -->
                            <div class="scanner-box p-4 shadow-sm cursor-pointer" onclick="document.getElementById('qrManualInput<%=rnd%>').focus();">
                                <label for="qrManualInput<%=rnd%>" class="form-label fw-bold text-primary mb-2 fs-5">
                                    <i class="fas fa-barcode me-2"></i>Menggunakan Scanner Fisik?
                                </label>
                                <p class="text-secondary small mb-3">Arahkan *Barcode Scanner* Anda dan pastikan kursor berkedip di kotak bawah ini:</p>
                                
                                <div class="input-group input-group-lg shadow-sm">
                                    <span class="input-group-text bg-white border-primary border-end-0"><i class="fas fa-keyboard text-primary"></i></span>
                                    <input type="password" id="qrManualInput<%=rnd%>" class="form-control border-primary border-start-0 text-center fw-bold text-primary fs-5 bg-white" placeholder="Tembakkan ke sini..." autocomplete="off">
                                </div>
                            </div>

                        </div>

                        <!-- PANEL 2: LOGIN MANUAL (KARTU/USERID) -->
                        <div class="tab-pane fade" id="manual-pane" role="tabpanel">
                            <div class="p-4 bg-light rounded-4 border border-secondary border-opacity-25 shadow-sm">
                                <p class="text-secondary small text-center fw-semibold mb-4">
                                    <i class="fas fa-id-badge text-primary me-2 fa-lg"></i>Silakan pindai kartu pintar Anda (RFID) atau masukkan identitas secara manual.
                                </p>
                                <form id="formLogin<%=rnd%>" onsubmit="prosesLoginManual(event)">
                                    <div class="input-group-custom bg-white shadow-sm border border-secondary border-opacity-25 mb-3">
                                        <i class="fas fa-id-card text-primary ms-3"></i>
                                        <input type="text" id="username<%=rnd%>" name="username" placeholder="Nomor Induk / ID Pengguna" class="form-control border-0 p-3 fs-6" required autocomplete="off">
                                    </div>

                                    <div class="input-group-custom bg-white shadow-sm border border-secondary border-opacity-25 mb-4">
                                        <i class="fas fa-key text-primary ms-3"></i>
                                        <input type="password" id="password<%=rnd%>" name="password" placeholder="Kata Sandi / PIN" class="form-control border-0 p-3 fs-6" required>
                                    </div>

                                    <button type="submit" id="btnSubmit<%=rnd%>" class="btn-masuk-anjungan py-3 shadow">
                                        <i class="fas fa-sign-in-alt"></i> Masuk Sistem Anjungan
                                    </button>
                                </form>
                            </div>
                        </div>

                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Modal Iframe PMB 98% -->
    <div class="modal fade" id="pmbAnjunganModal" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
        <div class="modal-dialog modal-pmb-dialog">
            <div class="modal-content modal-pmb-content">
                <div class="modal-pmb-header">
                    <h4 class="modal-title m-0 fw-bold">
                        <i class="fas fa-user-plus me-2 text-warning"></i> <%= Common.getBahasaConfig("Portal Pendaftaran Mahasiswa Baru") %>
                    </h4>
                    <button type="button" class="btn btn-danger fw-bold rounded-pill px-4 shadow-sm" data-bs-dismiss="modal">
                        <i class="fas fa-times me-1"></i> <%= Common.getBahasaConfig("Tutup Portal") %>
                    </button>
                </div>
                <div class="modal-pmb-body">
                    <div id="pmbLoader" class="d-flex flex-column justify-content-center align-items-center h-100 bg-light position-absolute w-100 z-1">
                        <div class="spinner-border text-primary" style="width: 4rem; height: 4rem;" role="status"></div>
                        <h4 class="mt-3 fw-bold text-secondary">Membuka Portal Pendaftaran...</h4>
                    </div>
                    <iframe id="pmbIframe" class="pmb-iframe position-relative z-2" style="visibility: hidden;"></iframe>
                </div>
            </div>
        </div>
    </div>

    <script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>

    <script>
        // ==========================================
        // AUTO FULLSCREEN LAYAR SENTUH
        // ==========================================
        function autoFullscreen() {
            var docElm = document.documentElement;
            if (!document.fullscreenElement && !document.mozFullScreenElement && !document.webkitFullscreenElement && !document.msFullscreenElement) {
                if (docElm.requestFullscreen) docElm.requestFullscreen();
                else if (docElm.msRequestFullscreen) docElm.msRequestFullscreen();
                else if (docElm.mozRequestFullScreen) docElm.mozRequestFullScreen();
                else if (docElm.webkitRequestFullScreen) docElm.webkitRequestFullScreen();
            }
        }
        document.addEventListener('click', autoFullscreen, { once: true });
        document.addEventListener('touchstart', autoFullscreen, { once: true });

        // ==========================================
        // FOKUS INPUT SCANNER FISIK (TAB QR)
        // ==========================================
        document.getElementById('loginAnjunganModal').addEventListener('shown.bs.modal', function () {
            document.getElementById('qrManualInput<%=rnd%>').focus();
        });

        document.getElementById('qrManualInput<%=rnd%>').addEventListener('keypress', function (e) {
            if (e.key === 'Enter' || e.keyCode === 13) {
                e.preventDefault();
                const qrDataScanned = this.value.trim();
                if(qrDataScanned) {
                    this.value = ''; // Bersihkan kolom agar siap dipakai lagi
                    prosesVerifikasiKredensialQR(qrDataScanned);
                }
            }
        });

        document.getElementById('qr-pane').addEventListener('click', function() {
            document.getElementById('qrManualInput<%=rnd%>').focus();
        });


        // ==========================================
        // SISTEM KAMERA QR CODE
        // ==========================================
        let html5QrcodeScanner;

        function startQRScanner() {
            setTimeout(() => { document.getElementById('qrManualInput<%=rnd%>').focus(); }, 100);

            if (html5QrcodeScanner) return; 
            
            html5QrcodeScanner = new Html5QrcodeScanner(
                "qr-reader", 
                { fps: 10, qrbox: {width: 250, height: 250} },
                false
            );
            html5QrcodeScanner.render(onScanSuccess, onScanFailure);
        }

        function stopQRScanner() {
            if (html5QrcodeScanner) {
                html5QrcodeScanner.clear().catch(error => { console.warn("Pembersihan kamera tertunda.", error); });
                html5QrcodeScanner = null;
            }
        }

        function onScanSuccess(decodedText, decodedResult) {
            prosesVerifikasiKredensialQR(decodedText);
        }

        function onScanFailure(error) {
            // Abaikan error konstan jika kamera nyala tapi belum melihat QR
        }

        document.getElementById('loginAnjunganModal').addEventListener('hidden.bs.modal', function () {
            stopQRScanner();
            document.getElementById('qrManualInput<%=rnd%>').value = '';
        });


        // ==========================================
        // PROSES VERIFIKASI QR (Kamera/Fisik)
        // ==========================================
        async function prosesVerifikasiKredensialQR(qrDataStr) {
            stopQRScanner(); 
            
            const alertBox = document.getElementById('alertBox<%=rnd%>');
            alertBox.style.display = 'block';
            alertBox.className = 'alert-custom alert-info-custom';
            alertBox.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i> Mengotentikasi Kode...';

            try {
                const payload = new URLSearchParams();
                payload.append('qr_data', qrDataStr);

                const response = await fetch('<%=Common.ROOT%>/anjungan?hanya_tampil_jsp=true&p=anjungan&s=_login_qrcode_service', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: payload.toString()
                });

                const result = await response.json();

                if (result.status === 'success') {
                    alertBox.className = 'alert-custom alert-success-custom';
                    alertBox.innerHTML = '<i class="fas fa-check-circle me-2"></i> ' + result.message;
                    setTimeout(() => { window.location.reload(); }, 1000);
                } else {
                    alertBox.className = 'alert-custom alert-danger-custom';
                    alertBox.innerHTML = '<i class="fas fa-exclamation-circle me-2"></i> ' + result.message;
                    
                    setTimeout(() => { 
                        alertBox.style.display = 'none';
                        startQRScanner(); 
                    }, 3000);
                }
            } catch (error) {
                alertBox.className = 'alert-custom alert-danger-custom';
                alertBox.innerHTML = '<i class="fas fa-wifi me-2"></i> Gagal menghubungi peladen verifikasi QR.';
                setTimeout(() => { startQRScanner(); }, 3000);
            }
        }


        // ==========================================
        // PROSES LOGIN MANUAL (Kartu/Username)
        // ==========================================
        async function prosesLoginManual(event) {
            event.preventDefault();
            autoFullscreen(); 
            
            const btnSubmit = document.getElementById('btnSubmit<%=rnd%>');
            const alertBox = document.getElementById('alertBox<%=rnd%>');
            const usernameVal = document.getElementById('username<%=rnd%>').value;
            const passwordVal = document.getElementById('password<%=rnd%>').value;

            btnSubmit.disabled = true;
            btnSubmit.innerHTML = '<i class="fas fa-circle-notch fa-spin me-2"></i> Memverifikasi...';
            alertBox.style.display = 'none';
            alertBox.className = 'alert-custom';

            try {
                const payload = new URLSearchParams();
                payload.append('username', usernameVal);
                payload.append('password', passwordVal);

                const response = await fetch('<%=Common.ROOT%>/anjungan?hanya_tampil_jsp=true&p=anjungan&s=_login_pustaka_service', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: payload.toString()
                });

                const result = await response.json();

                if (result.status === 'success') {
                    alertBox.classList.add('alert-success-custom');
                    alertBox.innerHTML = '<i class="fas fa-check-circle me-2"></i> ' + result.message;
                    alertBox.style.display = 'block';
                    setTimeout(() => { window.location.reload(); }, 1000);
                } else {
                    alertBox.classList.add('alert-danger-custom');
                    alertBox.innerHTML = '<i class="fas fa-exclamation-circle me-2"></i> ' + result.message;
                    alertBox.style.display = 'block';
                    btnSubmit.disabled = false;
                    btnSubmit.innerHTML = '<i class="fas fa-sign-in-alt me-2"></i> Masuk Sistem Anjungan';
                }
            } catch (error) {
                alertBox.classList.add('alert-danger-custom');
                alertBox.innerHTML = '<i class="fas fa-wifi me-2"></i> Gagal terhubung ke peladen utama.';
                alertBox.style.display = 'block';
                btnSubmit.disabled = false;
                btnSubmit.innerHTML = '<i class="fas fa-sign-in-alt me-2"></i> Masuk Sistem Anjungan';
            }
        }


        // ==========================================
        // FITUR BUKA IFRAME & AUTO-CLOSE (IDLE 3 MENIT)
        // ==========================================
        let pmbIdleTimer;
        const IDLE_TIMEOUT_MS = 3 * 60 * 1000; 
        let isPmbOpen = false;

        function resetPmbIdleTimer() {
            if (!isPmbOpen) return;
            clearTimeout(pmbIdleTimer);
            pmbIdleTimer = setTimeout(() => {
                const pmbModalEl = document.getElementById('pmbAnjunganModal');
                const modal = bootstrap.Modal.getInstance(pmbModalEl);
                if (modal) modal.hide(); 
            }, IDLE_TIMEOUT_MS);
        }

        function setupIframeActivityTracking() {
            const iframe = document.getElementById('pmbIframe');
            try {
                const iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
                if (iframeDoc) {
                    ['mousemove', 'keydown', 'touchstart', 'click', 'scroll'].forEach(evt => {
                        iframeDoc.addEventListener(evt, resetPmbIdleTimer, true);
                    });
                }
            } catch (e) {
                console.warn("Iframe beda Origin, deteksi Idle hanya mengandalkan layar luar.");
            }
        }

        function bukaModalPMB() {
            autoFullscreen(); 
            const pmbModalEl = document.getElementById('pmbAnjunganModal');
            const pmbModal = new bootstrap.Modal(pmbModalEl);
            const iframe = document.getElementById('pmbIframe');
            const loader = document.getElementById('pmbLoader');
            
            loader.style.display = 'flex';
            iframe.style.visibility = 'hidden';
            iframe.src = '<%=linkServicePMB%>';
            
            pmbModal.show();
            
            isPmbOpen = true;
            resetPmbIdleTimer();
            
            ['mousemove', 'keydown', 'touchstart', 'click', 'scroll'].forEach(evt => {
                document.addEventListener(evt, resetPmbIdleTimer, true);
            });
            
            iframe.onload = function() {
                loader.style.display = 'none';
                iframe.style.visibility = 'visible';
                setupIframeActivityTracking(); 
            };
        }

        function tutupModalPMB() {
            isPmbOpen = false;
            clearTimeout(pmbIdleTimer);
            const iframe = document.getElementById('pmbIframe');
            iframe.src = ''; 
            
            ['mousemove', 'keydown', 'touchstart', 'click', 'scroll'].forEach(evt => {
                document.removeEventListener(evt, resetPmbIdleTimer, true);
            });
        }

        document.getElementById('pmbAnjunganModal').addEventListener('hidden.bs.modal', function () {
            tutupModalPMB();
        });
    </script>
</body>
</html>