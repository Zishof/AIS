<%@page import="java.net.URLEncoder"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
// =================================================================================
// 1. PENGAMBILAN DATA PENGGUNA DARI CONTROLLER
// =================================================================================
Tbmuser tbmuser = (Tbmuser) request.getAttribute("tbmuser");

// =================================================================================
// 2. LOGIKA PENGAMBILAN DATA INSTITUSI & TEMA
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

// =================================================================================
// 3. LOGIKA HAK AKSES PENGGUNA & PEMBENTUKAN URL LAPORAN/PEMBAYARAN
// =================================================================================
boolean merupakanAdmin = false;
boolean merupakanMahasiswa = false;
String baseReportUrl = "";
String urlBayar = "";
Mahasiswa mhs = null;

if (tbmuser != null) {
    merupakanAdmin = Common.getApakahAdminLain(tbmuser);
    mhs = tbmuser.getMahasiswa();
    
    if (mhs != null) {
        merupakanMahasiswa = true;
    }
    
    // Membentuk URL Dasar Laporan secara dinamis
    String pLaporan = URLEncoder.encode("laporan/general_mahasiswa", "UTF-8");
    baseReportUrl = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=" + pLaporan + "&s=index";
    
    // Membentuk URL Pembayaran (Tanpa Iframe, Full Modal Ajax)
    urlBayar = Common.ROOT + "/pmb?hanya_tampil_jsp=true&p=bayarmhs&s=pembayaran_online_mhs&refresh=false&autoLoad=true";
    
    // Hanya sisipkan parameter mahasiswa jika entitas mahasiswa valid
    if (mhs != null && mhs.getId() != null) {
        baseReportUrl += "&mahasiswa=" + mhs.getId().toString();
        urlBayar += "&mahasiswa=" + mhs.getId().toString();
    }
}
%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= Common.getBahasaConfig("Dasbor Anjungan Mandiri") %> | <%=judul%></title>
    
    <link rel="icon" href="<%=logoPerguruanTinggi%>" type="image/x-icon">
    
    <!-- Pustaka Gaya & Ikon -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/animate.css/4.1.1/animate.min.css"/>
    
    <!-- MENGIMPOR TEMA GENERAL DAN CSS KHUSUS ANJUNGAN -->
    <link href="<%=request.getContextPath() %>/css/baru/base-theme.css?v=<%= cacheBuster %>" rel="stylesheet">
    <link href="<%=request.getContextPath() %>/css/baru/base-anjungan.css?v=<%= cacheBuster %>" rel="stylesheet">
    
    <% if (!cssTema.isEmpty()) { %>
        <link href="<%=request.getContextPath() %><%=cssTema%>?v=<%= cacheBuster %>" rel="stylesheet">
    <% } %>
    
    <style>
        /* PASTIKAN HALAMAN DAPAT DI-SCROLL DENGAN BAIK */
        body, html {
            margin: 0; padding: 0; font-family: 'Poppins', 'Segoe UI', Tahoma, sans-serif; 
            min-height: 100vh; display: flex; flex-direction: column; overflow-y: auto; overflow-x: hidden;
        }

        .video-background {
            position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; object-fit: cover; z-index: -2;
        }

        .overlay-cerah {
            position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
            background: linear-gradient(135deg, rgba(255, 255, 255, 0.85) 0%, rgba(248, 250, 252, 0.7) 100%);
            backdrop-filter: blur(12px); z-index: -1;
        }

        /* HEADER PREMIUM */
        .header-dasbor {
            background: linear-gradient(90deg, rgba(26, 64, 135, 0.95) 0%, rgba(53, 92, 158, 0.9) 100%), url('<%=backgroundPerguruanTinggi%>') center/cover no-repeat !important;
            color: #ffffff; padding: 2.5rem 3rem; border-radius: 0 0 2rem 2rem;
            box-shadow: 0 15px 35px rgba(0,0,0,0.2); margin-bottom: 3rem; position: relative; z-index: 10;
        }

        /* MENU DOKUMEN (GLASSMORPHISM) */
        .card-menu {
            background: linear-gradient(145deg, rgba(255, 255, 255, 0.95), rgba(248, 250, 252, 0.85));
            backdrop-filter: blur(20px); border: 1px solid rgba(255, 255, 255, 0.8);
            border-radius: 20px; transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
            box-shadow: 0 10px 30px rgba(0,0,0,0.06); text-decoration: none; color: inherit; display: flex;
            flex-direction: column; justify-content: center; align-items: center; padding: 2.5rem 1.5rem;
            height: 100%; cursor: pointer; position: relative; z-index: 10;
        }
        
        .card-menu:hover {
            transform: translateY(-10px) scale(1.02); box-shadow: 0 25px 50px rgba(26, 64, 135, 0.15);
            border-bottom: 5px solid #1a4087 !important; background: #ffffff;
        }
        
        .icon-menu {
            font-size: 3.5rem; color: #1a4087; margin-bottom: 1.5rem; transition: transform 0.4s ease;
        }
        .card-menu:hover .icon-menu { transform: scale(1.15); color: #355c9e; }
        
        /* MENU PEMBAYARAN (SPECIAL HIGHLIGHT) */
        .card-payment {
            background: linear-gradient(135deg, #10b981 0%, #059669 100%);
            border: none; border-radius: 20px; transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
            box-shadow: 0 15px 35px rgba(16, 185, 129, 0.3); text-decoration: none; color: white; display: flex;
            flex-direction: column; justify-content: center; align-items: center; padding: 2.5rem 1.5rem;
            height: 100%; cursor: pointer; position: relative; z-index: 10; overflow: hidden;
        }

        .card-payment::before {
            content: ''; position: absolute; top: -50%; left: -50%; width: 200%; height: 200%;
            background: radial-gradient(circle, rgba(255,255,255,0.2) 0%, transparent 60%); pointer-events: none;
        }

        .card-payment:hover {
            transform: translateY(-10px) scale(1.02); box-shadow: 0 25px 50px rgba(16, 185, 129, 0.4);
        }
        
        .card-payment .icon-menu { color: #ffffff !important; }

        .section-label {
            font-weight: 800; color: #1a4087; text-transform: uppercase;
            letter-spacing: 1.5px; margin-bottom: 2rem; border-bottom: 3px solid rgba(26, 64, 135, 0.15); 
            padding-bottom: 0.8rem; display: inline-block; font-size: 1.3rem;
        }
        
        .main-content-wrapper { position: relative; z-index: 10; flex: 1; margin-bottom: 3rem; }
        
        /* TOMBOL AKSI CEPAT */
        .btn-quick-action {
            border-radius: 50px; padding: 0.8rem 1.8rem; font-weight: 700; font-size: 0.95rem;
            box-shadow: 0 5px 15px rgba(0,0,0,0.05); transition: all 0.3s ease;
            background: #ffffff; border: 1px solid #e2e8f0; color: #475569;
        }
        .btn-quick-action:hover {
            transform: translateY(-3px); box-shadow: 0 10px 25px rgba(26, 64, 135, 0.2);
            background: #1a4087 !important; color: white !important; border-color: #1a4087 !important;
        }
        .btn-quick-action:hover i { color: white !important; }
        
        .modal-header-custom-anjungan { background: linear-gradient(90deg, #1a4087 0%, #355c9e 100%) !important; color: white !important; }
        .modal-header-custom-anjungan .btn-close { filter: invert(1) grayscale(100%) brightness(200%); }
    </style>
</head>
<body>

    <!-- Latar Belakang Video Dinamis -->
    <video autoplay loop muted playsinline class="video-background" id="bg-video">
        <source src="<%=Common.ROOT%>/img/video_background_anjungan.mp4" type="video/mp4">
    </video>

    <!-- Lapisan Kaca (Overlay) -->
    <div class="overlay-cerah"></div>

    <!-- HEADER DASBOR -->
    <div class="header-dasbor d-flex justify-content-between align-items-center">
        <div class="d-flex align-items-center">
            <img src="<%=logoPerguruanTinggi%>" alt="<%= Common.getBahasaConfig("Logo Institusi") %>" height="90" class="me-4 bg-white p-2 rounded-4 shadow-lg">
            <div>
                <h2 class="mb-1 fw-bold" style="letter-spacing: 0.5px; text-shadow: 1px 2px 5px rgba(0,0,0,0.5);"><%=judul%></h2>
                <h5 class="opacity-100 d-block mb-3 fst-italic text-warning fw-semibold" style="text-shadow: 1px 1px 3px rgba(0,0,0,0.6);"><%=motto%></h5>
                <div class="badge bg-white text-dark rounded-pill px-4 py-2 shadow-sm border border-light border-opacity-25" style="font-size: 0.95rem;">
                    <i class="fas fa-user-circle me-2 text-primary"></i> <%= Common.getBahasaConfig("Sesi Aktif") %>: <strong class="text-primary fs-6"><%= (tbmuser != null) ? tbmuser.getUserId() : "Pengguna" %></strong>
                </div>
            </div>
        </div>
        <div class="d-flex align-items-center gap-3">
            <button type="button" class="btn btn-outline-light rounded-circle shadow-sm border-2 p-3" onclick="toggleFullscreen()" title="<%= Common.getBahasaConfig("Layar Penuh") %>">
                <i class="fas fa-expand fa-lg"></i>
            </button>
            
            <a href="<%=Common.ROOT%>/logoff" class="btn btn-danger fw-bold rounded-pill shadow-lg px-4 py-3" style="font-size: 1.1rem; border: 2px solid rgba(255,255,255,0.3);">
                <i class="fas fa-power-off me-2"></i> <%= Common.getBahasaConfig("Akhiri Sesi") %>
            </a>
        </div>
    </div>

    <!-- KONTEN UTAMA ANJUNGAN -->
    <div class="container-fluid px-5 main-content-wrapper">
        
        <% 
        // -------------------------------------------------------------------------
        // KONDISI: Jika Pengguna adalah MAHASISWA atau ADMIN
        // -------------------------------------------------------------------------
        if (merupakanAdmin || merupakanMahasiswa) { 
        %>
            
            <div class="text-center mb-5 animate__animated animate__fadeInUp">
                
                <!-- PANEL TOMBOL PINTAS (QUICK ACTIONS) -->
                <div class="card shadow-sm mb-5" style="border-radius: 20px; border: 1px solid rgba(255,255,255,0.8); background: rgba(255,255,255,0.95); backdrop-filter: blur(10px);">
                    <div class="card-header bg-transparent border-bottom py-3 px-4">
                        <h5 class="mb-0 fw-bold text-start" style="color: #1a4087;">
                            <i class="fas fa-bolt me-2 text-warning"></i><%=Common.getBahasaConfig("Layanan Akses Cepat")%>
                        </h5>
                    </div>
                    <div class="card-body py-4 px-4">
                        <div class="d-flex flex-wrap justify-content-center gap-3">
                            <button class="btn btn-quick-action" type="button" onclick="window.location.reload();">
                                <i class="fas fa-sync-alt text-primary me-2"></i> <%=Common.getBahasaConfig("Muat Ulang Layar") %>
                            </button>
                            <button class="btn btn-quick-action" type="button">
                                <i class="fas fa-tasks text-success me-2"></i> <%=Common.getBahasaConfig("Lihat Kartu Rencana Studi") %>
                            </button>
                            <button class="btn btn-quick-action" type="button">
                                <i class="far fa-check-circle text-info me-2"></i> <%=Common.getBahasaConfig("Periksa Nilai Akademik") %>
                            </button>
                            <button class="btn btn-quick-action" type="button">
                                <i class="fas fa-fingerprint text-warning me-2"></i> <%=Common.getBahasaConfig("Informasi Kehadiran") %>
                            </button>
                            <button class="btn btn-quick-action" type="button">
                                <i class="fas fa-clipboard-list text-danger me-2"></i> <%=Common.getBahasaConfig("Aktivitas Mahasiswa") %>
                            </button>
                            <button class="btn btn-quick-action" type="button" onclick="bukaModalLaporan('<%=Common.getBahasaConfigJS("Pencetakan Surat Keterangan Pendamping Ijazah")%>', 'Cetak_SKPI', 'Surat+Keterangan+Pendamping+Ijazah')">
                                <i class="fas fa-medal text-primary me-2"></i> <%=Common.getBahasaConfig("Cetak SKPI") %>
                            </button>
                        </div>
                    </div>
                </div>

                <!-- MENU LAYANAN TRANSAKSI & CETAK DOKUMEN -->
                <h4 class="section-label mt-2"><i class="fas fa-concierge-bell me-2"></i><%=Common.getBahasaConfig("Pusat Layanan Administrasi")%></h4>
                <div class="row g-4 justify-content-center mt-1">
                    
                    <!-- KOTAK SPESIAL PEMBAYARAN (MEMBUKA MODAL FULL DENGAN JAVASCRIPT DINAMIS) -->
                    <div class="col-6 col-md-4 col-xl-3">
                        <div class="card card-payment" onclick="bukaModalPembayaran()">
                            <i class="fas fa-wallet icon-menu"></i>
                            <h4 class="fw-bold mt-2"><%=Common.getBahasaConfig("Pembayaran Daring") %></h4>
                            <p class="text-white-50 small mb-0 fw-semibold"><%=Common.getBahasaConfig("Selesaikan Kewajiban Finansial Anda") %></p>
                        </div>
                    </div>

                    <div class="col-6 col-md-4 col-xl-3">
                        <div class="card card-menu" onclick="bukaModalLaporan('<%=Common.getBahasaConfigJS("Pencetakan Kartu Rencana Studi")%>', 'Cetak_KRS_Mahasiswa', 'Kartu+Rencana+Studi')">
                            <i class="fas fa-file-invoice icon-menu"></i>
                            <h5 class="fw-bold"><%=Common.getBahasaConfig("Cetak KRS") %></h5>
                            <p class="text-muted small mb-0 fw-semibold"><%=Common.getBahasaConfig("Kartu Rencana Studi") %></p>
                        </div>
                    </div>

                    <div class="col-6 col-md-4 col-xl-3">
                        <div class="card card-menu" onclick="bukaModalLaporan('<%=Common.getBahasaConfigJS("Pencetakan Kartu Hasil Studi")%>', 'Kartu_Hasil_Studi', 'Kartu+Hasil+Studi')">
                            <i class="fas fa-file-alt icon-menu"></i>
                            <h5 class="fw-bold"><%=Common.getBahasaConfig("Cetak KHS") %></h5>
                            <p class="text-muted small mb-0 fw-semibold"><%=Common.getBahasaConfig("Kartu Hasil Studi Akhir Semester") %></p>
                        </div>
                    </div>

                    <div class="col-6 col-md-4 col-xl-3">
                        <div class="card card-menu" onclick="bukaModalLaporan('<%=Common.getBahasaConfigJS("Pencetakan Kartu Peserta Ujian Tengah Semester")%>', 'Cetak_KUTS_Mahasiswa', 'Kartu+UTS')">
                            <i class="far fa-id-badge icon-menu"></i>
                            <h5 class="fw-bold"><%=Common.getBahasaConfig("Kartu Ujian UTS") %></h5>
                            <p class="text-muted small mb-0 fw-semibold"><%=Common.getBahasaConfig("Syarat Mengikuti Ujian Tengah Semester") %></p>
                        </div>
                    </div>

                    <div class="col-6 col-md-4 col-xl-3">
                        <div class="card card-menu" onclick="bukaModalLaporan('<%=Common.getBahasaConfigJS("Pencetakan Kartu Peserta Ujian Akhir Semester")%>', 'Cetak_KUAS_Mahasiswa', 'Kartu+UAS')">
                            <i class="fas fa-id-badge icon-menu"></i>
                            <h5 class="fw-bold"><%=Common.getBahasaConfig("Kartu Ujian UAS") %></h5>
                            <p class="text-muted small mb-0 fw-semibold"><%=Common.getBahasaConfig("Syarat Mengikuti Ujian Akhir Semester") %></p>
                        </div>
                    </div>

                    <div class="col-6 col-md-4 col-xl-3">
                        <div class="card card-menu" onclick="bukaModalLaporan('<%=Common.getBahasaConfigJS("Pencetakan Transkrip Nilai Akademik")%>', 'Transkrip_Akademik', 'Transkrip+Akademik')">
                            <i class="fas fa-graduation-cap icon-menu"></i>
                            <h5 class="fw-bold"><%=Common.getBahasaConfig("Transkrip Akademik") %></h5>
                            <p class="text-muted small mb-0 fw-semibold"><%=Common.getBahasaConfig("Rekapitulasi Nilai Akademik Keseluruhan") %></p>
                        </div>
                    </div>

                    <div class="col-6 col-md-4 col-xl-3">
                        <div class="card card-menu" onclick="bukaModalLaporan('<%=Common.getBahasaConfigJS("Pencetakan Rekaman Nilai dan Indeks Prestasi")%>', 'Rekaman_Nilai', 'Transkrip+IPK')">
                            <i class="fas fa-chart-line icon-menu"></i>
                            <h5 class="fw-bold"><%=Common.getBahasaConfig("Rekaman IPK") %></h5>
                            <p class="text-muted small mb-0 fw-semibold"><%=Common.getBahasaConfig("Laporan Indeks Prestasi Kumulatif") %></p>
                        </div>
                    </div>

                </div>
            </div>

            <!-- FUNGSI JAVASCRIPT PEMANGGIL MODAL FETCH AJAX (TANPA IFRAME SAMA SEKALI) -->
            <script>
                // 1. FUNGSI UNTUK MENGINJEKSI & MENGEKSEKUSI SKRIP
                function eksekusiScriptDariAJAX(containerElement) {
                    var scripts = containerElement.querySelectorAll('script');
                    scripts.forEach(function(oldScript) {
                        var scriptType = oldScript.type ? oldScript.type.toLowerCase() : '';
                        if (scriptType === '' || scriptType === 'text/javascript' || scriptType === 'application/javascript' || scriptType.indexOf('rocket-loader') >= 0) {
                            try {
                                var newScript = document.createElement('script');
                                Array.from(oldScript.attributes).forEach(function(attr) {
                                    if (attr.name.toLowerCase() !== 'type') newScript.setAttribute(attr.name, attr.value);
                                });
                                newScript.type = 'text/javascript';
                                newScript.appendChild(document.createTextNode(oldScript.innerHTML));
                                oldScript.parentNode.replaceChild(newScript, oldScript);
                            } catch(e) {
                                console.warn('<%=Common.getBahasaConfigJS("Peringatan: Terdapat skrip yang gagal diurai.")%>', e.message);
                            }
                        }
                    });
                }

                // 2. FUNGSI UNTUK MEMBUKA MODAL PEMBAYARAN (FULL MODAL 98%)
                function bukaModalPembayaran() {
                    autoFullscreen(); // Pastikan masuk mode layar penuh
                    
                    if (typeof window.variable === 'undefined') { window.variable = 0; }
                    window.variable++;
                    
                    var modalId = 'modalPembayaran_' + window.variable;
                    var zIndexModal = 1050 + (window.variable * 2);
                    
                    // Bangun HTML Modal Secara Dinamis via JS
                    var modalHtml = 
                        '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true" data-bs-backdrop="static" style="z-index: ' + zIndexModal + ';">' +
                            '<div class="modal-dialog modal-dialog-scrollable mt-3 animate__animated animate__zoomIn" style="max-width: 98%; width: 98%; height: 94vh;">' +
                                '<div class="modal-content shadow-lg border-0" style="border-radius: 20px; overflow: hidden; height: 100%;">' +
                                    '<div class="modal-header p-4" style="background: linear-gradient(90deg, #059669 0%, #10b981 100%); color: white;">' +
                                        '<h4 class="modal-title fw-bold m-0"><i class="fas fa-wallet me-3 text-warning"></i> <%= Common.getBahasaConfig("Portal Pembayaran Daring") %></h4>' +
                                        '<button type="button" class="btn btn-light text-success fw-bold rounded-pill px-4 shadow-sm" data-bs-dismiss="modal"><i class="fas fa-times me-2"></i> <%= Common.getBahasaConfig("Tutup Portal Transaksi") %></button>' +
                                    '</div>' +
                                    '<div class="modal-body p-0" id="body_' + modalId + '" style="background-color: #f8fafc; overflow-y: auto;">' +
                                        '<div class="d-flex flex-column justify-content-center align-items-center h-100 py-5 text-success">' +
                                            '<div class="spinner-border text-success mb-4" style="width: 4rem; height: 4rem;" role="status"></div>' +
                                            '<h4 class="fw-bold"><%=Common.getBahasaConfig("Menyiapkan Jalur Pembayaran Aman...")%></h4>' +
                                        '</div>' +
                                    '</div>' +
                                '</div>' +
                            '</div>' +
                        '</div>';

                    // Suntikkan ke DOM
                    document.body.insertAdjacentHTML('beforeend', modalHtml);
                    
                    var modalElement = document.getElementById(modalId);
                    var myModal = new bootstrap.Modal(modalElement);
                    var modalBody = document.getElementById('body_' + modalId);
                    
                    // Bersihkan HTML saat Modal Ditutup
                    modalElement.addEventListener('hidden.bs.modal', function () {
                        modalElement.remove(); 
                    });

                    // Tampilkan Modal
                    myModal.show();

                    // Panggil AJAX Fetch
                    fetch('<%=urlBayar%>', { credentials: 'same-origin' })
                        .then(function(response) {
                            if (!response.ok) throw new Error('<%=Common.getBahasaConfigJS("Koneksi jaringan bermasalah atau berkas tidak ditemukan.")%>');
                            return response.text();
                        })
                        .then(function(html) {
                            modalBody.innerHTML = html;
                            // Aktifkan Skrip JS di dalam Body Modal
                            eksekusiScriptDariAJAX(modalBody);
                        })
                        .catch(function(error) {
                            modalBody.innerHTML = 
                                '<div class="alert alert-danger m-5 p-5 text-center shadow-sm" style="border-radius: 20px;">' +
                                    '<i class="fas fa-exclamation-triangle fa-4x mb-3 text-danger"></i>' +
                                    '<h4 class="fw-bold"><%=Common.getBahasaConfig("Gagal Memuat Modul Pembayaran")%></h4>' +
                                    '<p class="mb-0">' + error.message + '</p>' +
                                '</div>';
                        });
                }

                // 3. FUNGSI UNTUK MEMBUKA MODAL LAPORAN (DINAMIS & AMAN)
                function loadModalContentCustomSimpan(title, fileUrl, lebarPopup, simpandata, fungsiSimpan, modalId) {
                    if (typeof window.variable === 'undefined') { window.variable = 0; }
                    window.variable++;
                    
                    var contentModal = modalId != null && modalId != '' ? modalId : 'contentModal' + variable;
                    
                    var simpanButton = (simpandata == null ? "" : '<button type="button" onclick="' + fungsiSimpan + '" class="btn btn-primary fw-bold px-4 rounded-pill"><i class="far fa-save me-2"></i> <%=Common.getBahasaConfig("Simpan Dokumen") %></button>');
                    
                    var zIndexModal = 1050 + (window.variable * 2);
                    
                    var modalHtml = 
                        '<div class="modal fade" id="' + contentModal + '" tabindex="-1" aria-hidden="true" ' + (simpandata != null && simpandata == 'hidden' ? '' : 'data-bs-backdrop="static"') + ' style="z-index: ' + zIndexModal + ';">' +
                            '<div class="modal-dialog modal-dialog-scrollable mt-5 animate__animated animate__rotateInDownLeft">' +
                                '<div class="modal-content shadow-lg border-0" style="border-radius: 20px; overflow: hidden;">' +
                                    '<div class="modal-header modal-header-custom-anjungan p-4">' +
                                        '<h5 class="modal-title fw-bold" style="font-size: 1.25rem;"><i class="fas fa-print me-3 text-warning"></i> <label id="labelHeader' + variable + '" class="modal-title mb-0"></label></h5>' +
                                        '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<%=Common.getBahasaConfig("Tutup Jendela") %>"></button>' +
                                    '</div>' +
                                    '<div class="modal-body p-0" id="modalBodyContent' + variable + '" style="background-color: #f8fafc; min-height: 400px; overflow-y: auto;">' +
                                        '<div class="d-flex flex-column justify-content-center align-items-center h-100 py-5 text-primary">' +
                                            '<i class="fas fa-spinner fa-spin fa-3x mb-3"></i>' +
                                            '<h5 class="fw-bold"><%=Common.getBahasaConfig("Sedang Mempersiapkan Dokumen...")%></h5>' +
                                        '</div>' +
                                    '</div>' +
                                    (simpandata != null && simpandata == 'hidden' ? '' : 
                                    '<div class="modal-footer bg-light border-top-0 p-4 justify-content-between">' +
                                        '<button type="button" class="btn btn-outline-secondary fw-bold px-4 rounded-pill" data-bs-dismiss="modal"><i class="fas fa-times me-2"></i> <%=Common.getBahasaConfig("Tutup Jendela") %></button>' +
                                        simpanButton +
                                    '</div>') +
                                '</div>' +
                            '</div>' +
                        '</div>';

                    document.body.insertAdjacentHTML('beforeend', modalHtml);
                    
                    var labelHeader = 'labelHeader' + variable;
                    var modalBodyContent = 'modalBodyContent' + variable;
                    
                    var modalElement = document.getElementById(contentModal);
                    var myModal = new bootstrap.Modal(modalElement);
                    var modalDialog = modalElement.querySelector('.modal-dialog');
                    
                    document.getElementById(labelHeader).innerText = title;
                    
                    modalElement.addEventListener('show.bs.modal', function () {
                        setTimeout(function() {
                            var backdrops = document.querySelectorAll('.modal-backdrop');
                            if (backdrops.length > 0) {
                                backdrops[backdrops.length - 1].style.zIndex = zIndexModal - 1;
                            }
                        }, 10);
                    });

                    modalDialog.classList.remove('animate__rotateInDownLeft', 'animate__jackInTheBox', 'animate__zoomOut'); 
                    void modalDialog.offsetWidth; 
                    modalDialog.classList.add('animate__jackInTheBox', 'animate__rotateInDownLeft');
                    
                    if (lebarPopup) {
                        modalDialog.style.maxWidth = lebarPopup;
                        modalDialog.style.width = lebarPopup;
                    }

                    myModal.show();
                    
                    modalElement.addEventListener('hidePrevented.bs.modal', function () {
                        modalDialog.classList.add('animate__headShake');
                        modalDialog.classList.remove('animate__rotateInDownLeft', 'animate__jackInTheBox', 'animate__zoomOut');
                        modalDialog.addEventListener('animationend', function () {
                            modalDialog.classList.remove('animate__headShake');
                        }, { once: true });
                    });
                    
                    modalElement.addEventListener('hide.bs.modal', function (e) {
                        modalDialog.classList.add('animate__rotateInDownLeft', 'animate__jackInTheBox');
                        
                        if (!modalDialog.classList.contains('animate__zoomOut')) {
                            e.preventDefault();
                            modalDialog.classList.add('animate__zoomOut'); 

                            modalDialog.innerHTML = '';

                            setTimeout(() => {
                                myModal.hide();
                                setTimeout(() => { modalElement.remove(); }, 300);
                            }, 500);
                        }
                    });

                    // PENGAMBILAN DATA LAPORAN (AJAX) 
                    fetch(fileUrl, { credentials: 'same-origin' })
                        .then(function(response) {
                            if (!response.ok) throw new Error('<%=Common.getBahasaConfigJS("Koneksi jaringan bermasalah atau berkas tidak ditemukan.")%>');
                            return response.text();
                        })
                        .then(function(html) {
                            var container = document.getElementById(modalBodyContent);
                            container.innerHTML = html; 
                            eksekusiScriptDariAJAX(container);
                        })
                        .catch(function(error) {
                            document.getElementById(modalBodyContent).innerHTML = 
                                '<div class="alert alert-danger m-4 border-0 shadow-sm rounded-4 p-4">' +
                                    '<h5 class="fw-bold text-danger"><i class="fas fa-exclamation-triangle me-2"></i> <%=Common.getBahasaConfig("Gagal Memuat Dokumen")%></h5>' +
                                    '<p class="mb-0 mt-2 text-dark"><%=Common.getBahasaConfig("Pastikan koneksi jaringan stabil dan modul pelaporan dalam keadaan aktif.")%></p><hr>' +
                                    '<small class="text-muted">Error Detail: ' + error.message + '</small>' +
                                '</div>';
                        });
                }

                // Fungsi Pemicu Parameter Laporan
                function bukaModalLaporan(labelTombol, namaFile, judulLaporan) {
                    if (typeof window.variable === 'undefined') { window.variable = 0; }
                    var cm = 'contentModal_' + (++window.variable);
                    
                    var baseUrl = '<%=baseReportUrl%>'; 
                    var fullUrl = baseUrl + '&fileLaporan=' + namaFile + '&judulLaporan=' + judulLaporan + '&contentModal=' + cm;

                    loadModalContentCustomSimpan(labelTombol, fullUrl, '90%', 'hidden', '', cm);
                }
            </script>

        <% 
        } else { 
            // -------------------------------------------------------------------------
            // KONDISI: Jika Pengguna BUKAN MAHASISWA atau ADMIN
            // -------------------------------------------------------------------------
        %>
            <div class="text-center mt-5 animate__animated animate__fadeIn">
                <div class="alert alert-warning d-inline-block shadow-lg p-5" style="border-radius: 24px; background: rgba(255, 251, 235, 0.95); border: 2px solid #fcd34d;">
                    <i class="fas fa-lock fa-5x mb-4 text-warning"></i>
                    <h2 class="fw-bold text-dark"><%=Common.getBahasaConfig("Akses Dibatasi")%></h2>
                    <p class="mb-0 text-muted fs-5 mt-3"><%=Common.getBahasaConfig("Akun Anda saat ini tidak teridentifikasi sebagai Mahasiswa atau Administrator. Fasilitas pencetakan dokumen dan pembayaran pada Anjungan Layanan Mandiri belum dapat diakses menggunakan peran Anda saat ini.")%></p>
                </div>
            </div>
        <% } %>

    </div>

    <!-- FOOTER INFORMASI INSTITUSI -->
    <footer class="mt-auto py-4 text-center text-white" style="position: relative; z-index: 10; background: linear-gradient(180deg, transparent 0%, rgba(0,0,0,0.9) 100%);">
        <div class="container pt-4">
            <h5 class="fw-bold mb-2 fs-5"><%=judulHeader%> - <%=judul%></h5>
            <p class="small opacity-100 mb-3 fw-semibold text-warning fst-italic"><%=motto%></p>
            <p class="small opacity-75 mb-1"><i class="fas fa-map-marker-alt me-2 text-warning"></i> <%=alamat1%></p>
            <p class="small opacity-75 mb-0">
                <i class="fas fa-phone-alt me-2 text-success"></i> <%=telepon%> &nbsp;|&nbsp; 
                <i class="fas fa-envelope me-2 text-info"></i> <%=email%>
            </p>
        </div>
    </footer>

    <!-- PUSTAKA DEPENDENSI -->
    <script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>

    <!-- SKRIP FULLSCREEN MANUAL / OTOMATIS -->
    <script>
        function toggleFullscreen() {
            var doc = window.document;
            var docEl = doc.documentElement;
            var requestFullScreen = docEl.requestFullscreen || docEl.mozRequestFullScreen || docEl.webkitRequestFullScreen || docEl.msRequestFullscreen;
            var cancelFullScreen = doc.exitFullscreen || doc.mozCancelFullScreen || doc.webkitExitFullscreen || doc.msExitFullscreen;
            if(!doc.fullscreenElement && !doc.mozFullScreenElement && !doc.webkitFullscreenElement && !doc.msFullscreenElement) {
                requestFullScreen.call(docEl);
            } else {
                cancelFullScreen.call(doc);
            }
        }

        function autoFullscreen() {
            var docElm = document.documentElement;
            if (!document.fullscreenElement && !document.mozFullScreenElement && !document.webkitFullscreenElement && !document.msFullscreenElement) {
                if (docElm.requestFullscreen) docElm.requestFullscreen();
                else if (docElm.msRequestFullscreen) docElm.msRequestFullscreen();
                else if (docElm.mozRequestFullScreen) docElm.mozRequestFullScreen();
                else if (docElm.webkitRequestFullScreen) docElm.webkitRequestFullScreen();
            }
        }

        // Pemicu Fullscreen saat ada sentuhan pertama di layar Kiosk
        document.addEventListener('click', autoFullscreen, { once: true });
        document.addEventListener('touchstart', autoFullscreen, { once: true });
    </script>
</body>
</html>