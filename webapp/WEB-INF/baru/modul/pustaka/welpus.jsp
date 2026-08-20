<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.common.newui.NewUiCsrfUtil"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // 1. INISIALISASI VARIABEL DASAR
    String rnd = Common.getGeneratedBarCode(7);
    String kioskCsrf = NewUiCsrfUtil.getToken(request.getSession());
    long cacheBuster = System.currentTimeMillis();
    
    // 2. PENGAMBILAN DATA INSTITUSI & TEMA[cite: 5]
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
        PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
        if (pt != null) {
            judul = pt.getNama() != null ? pt.getNama() : "";
            motto = pt.getMotto() != null ? pt.getMotto() : "";
            telepon = pt.getTelepon() != null ? pt.getTelepon() : "";
            alamat1 = pt.getAlamat1() != null ? pt.getAlamat1() : "";
            email = pt.getEmail() != null ? pt.getEmail() : "";
            
            // Mengambil konfigurasi file CSS tema aktif[cite: 6, 7]
            String rawCss = pt.getCss();
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
    }

    // 3. KONFIGURASI ALAMAT LAYANAN & BACKGROUND
    String linkDetail = Common.ROOT + "/pustaka?hanya_tampil_jsp=true&p=pustaka&s=_welpus_service";
    String bgImage = (backgroundPerguruanTinggi != null && !backgroundPerguruanTinggi.isEmpty()) 
                     ? backgroundPerguruanTinggi : Common.ROOT + "/img/office-bg.jpeg";
%>

<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= Common.getBahasaConfig("Buku Tamu Perpustakaan") %> - <%= judul %></title>
    
    <!-- CSS TERPUSAT[cite: 6] -->
    <link href="<%=request.getContextPath() %>/css/baru/base-theme.css?v=<%= cacheBuster %>" rel="stylesheet">
    <% if (!cssTema.isEmpty()) { %>
        <link href="<%=request.getContextPath() %><%= cssTema %>?v=<%= cacheBuster %>" rel="stylesheet">
    <% } %>

    <style>
        body {
            background: url('<%= bgImage %>') no-repeat center center fixed;
            background-size: cover;
            min-height: 100vh;
            display: flex;
            align-items: center;
            position: relative;
        }
        .overlay-bg {
            position: absolute; top: 0; left: 0; width: 100%; height: 100%;
            background: rgba(255, 255, 255, 0.85); z-index: 1;
        }
        .main-wrapper { z-index: 2; width: 100%; max-width: 850px; margin: auto; padding: 20px; }
        .card-scanner {
            background: rgba(255, 255, 255, 0.98); 
            border-radius: 1.5rem; padding: 3rem 2rem;
            box-shadow: 0 15px 35px rgba(0,0,0,0.1); 
            text-align: center;
            border-top: 6px solid var(--theme-primary);
        }
        .instruction-badge {
            background-color: #ffc107; color: var(--theme-primary-dark); 
            font-weight: 800; font-size: 1.4rem; padding: 12px 25px; 
            border-radius: 12px; display: inline-block; 
            box-shadow: 0 4px 10px rgba(0,0,0,0.1); text-transform: uppercase;
        }
        .input-barcode {
            font-size: 2.5rem; text-align: center; font-weight: bold; 
            background-color: var(--theme-light); border: 2px solid var(--theme-primary); 
            border-radius: 15px; height: 80px; letter-spacing: 4px;
        }
        .table-kunjungan th { background: var(--theme-gradient) !important; color: white !important; font-size: 0.85rem; }
        .page-link { color: var(--theme-primary); }
        .page-item.active .page-link { background-color: var(--theme-primary); border-color: var(--theme-primary); }
    </style>
</head>
<body>

    <div class="overlay-bg"></div>

    <div class="main-wrapper">
        <div class="card-scanner animate__animated animate__fadeInDown">
            
            <% if (logoPerguruanTinggi != null && !logoPerguruanTinggi.isEmpty()) { %>
                <img src="<%= logoPerguruanTinggi %>" alt="Logo" width="110" class="mb-3">
            <% } %>
            
            <h2 class="fw-bold text-dark mb-1"><%= judul %></h2>
            <p class="fst-italic text-muted mb-4">"<%= motto %>"</p>
            
            <div class="instruction-badge mb-4">
                <i class="fas fa-barcode me-2"></i><%= Common.getBahasaConfig("Silakan Scan KTM / ID Card") %>
            </div>

            <p class="text-muted fs-5 mb-3">
                <%= Common.getBahasaConfig("Atau masukkan NIM / NIDN / NIK Anda di bawah ini:") %>
            </p>

            <form id="formKunjungan<%=rnd%>" onsubmit="catatKunjungan<%=rnd%>(event)">
                <div class="mb-4 px-md-5">
                    <input type="text" id="kodeAnggota<%=rnd%>" class="form-control input-barcode shadow-sm" autocomplete="off" autofocus required>
                </div>
            </form>

            <div class="small text-muted mb-4">
                <i class="fas fa-map-marker-alt me-1"></i> <%= alamat1 %> | 
                <i class="fas fa-phone-alt me-1"></i> <%= telepon %>
            </div>

            <hr class="my-4" style="opacity: 0.1;">

            <div class="d-flex justify-content-center gap-3">
                <button type="button" class="btn btn-outline-primary rounded-pill px-4 py-2 fw-bold transition-all" onclick="bukaModalTamu<%=rnd%>()">
                    <i class="fas fa-user-plus me-2"></i><%= Common.getBahasaConfig("Pengunjung Bukan Anggota") %>
                </button>
                <button type="button" class="btn btn-primary rounded-pill px-4 py-2 fw-bold transition-all shadow-sm" onclick="bukaModalRiwayat<%=rnd%>(0)">
                    <i class="fas fa-history me-2"></i><%= Common.getBahasaConfig("Riwayat Kunjungan") %>
                </button>
            </div>
        </div>
    </div>

    <!-- MODAL 1: FORM TAMU UMUM -->
    <div class="modal fade" id="modalTamu<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">
                <div class="modal-header bg-primary text-white py-3">
                    <h5 class="modal-title fw-bold"><i class="fas fa-user-edit me-2"></i><%= Common.getBahasaConfig("Buku Tamu Eksternal") %></h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4 bg-light">
                    <form id="formTamu<%=rnd%>">
                        <div class="mb-3">
                            <label class="form-label fw-bold text-muted small required"><%= Common.getBahasaConfig("Nama Lengkap") %></label>
                            <input type="text" id="namaTamu<%=rnd%>" class="form-control" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label fw-bold text-muted small required"><%= Common.getBahasaConfig("Alamat / Instansi") %></label>
                            <textarea id="alamatTamu<%=rnd%>" class="form-control" rows="2" required></textarea>
                        </div>
                        <div class="mb-3">
                            <label class="form-label fw-bold text-muted small"><%= Common.getBahasaConfig("Keterangan Keperluan") %></label>
                            <input type="text" id="ketTamu<%=rnd%>" class="form-control">
                        </div>
                    </form>
                </div>
                <div class="modal-footer bg-light border-top-0">
                    <button type="button" class="btn btn-secondary rounded-pill px-4" data-bs-dismiss="modal"><%= Common.getBahasaConfig("Batal") %></button>
                    <button type="button" class="btn btn-primary rounded-pill px-4 fw-bold" onclick="simpanTamu<%=rnd%>()">
                        <i class="fas fa-save me-2"></i><%= Common.getBahasaConfig("Simpan Kunjungan") %>
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- MODAL 2: RIWAYAT KUNJUNGAN TERAKHIR (DENGAN PAGING)[cite: 5] -->
    <div class="modal fade" id="modalRiwayat<%=rnd%>" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-lg modal-dialog-centered">
            <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">
                <div class="modal-header bg-primary text-white py-3">
                    <h5 class="modal-title fw-bold"><i class="fas fa-list-ul me-2"></i><%= Common.getBahasaConfig("Daftar Kunjungan Hari Ini") %></h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover mb-0 table-kunjungan">
                            <thead>
                                <tr>
                                    <th class="ps-4" width="10%">No</th>
                                    <th width="45%"><%= Common.getBahasaConfig("Nama Pengunjung") %></th>
                                    <th width="20%"><%= Common.getBahasaConfig("Status") %></th>
                                    <th width="25%" class="text-center"><%= Common.getBahasaConfig("Waktu Masuk") %></th>
                                </tr>
                            </thead>
                            <tbody id="tabelBodyKunjungan<%=rnd%>">
                                <!-- Data dimuat melalui AJAX -->
                            </tbody>
                        </table>
                    </div>
                </div>
                <div class="modal-footer bg-light border-top-0 d-flex justify-content-between align-items-center">
                    <div id="infoPaging<%=rnd%>" class="small text-muted fw-bold"></div>
                    <nav><ul class="pagination pagination-sm mb-0" id="paginationCtrl<%=rnd%>"></ul></nav>
                </div>
            </div>
        </div>
    </div>

    <script data-cfasync="false" src="<%=Common.ROOT%>/component/uiux/vendors/bootstrap/bootstrap.min.js"></script>
    <script data-cfasync="false" src="<%=Common.ROOT%>/component/uiux/vendors/fontawesome/all.min.js"></script>
    
    <script>
        document.addEventListener("DOMContentLoaded", function() {
            document.getElementById('kodeAnggota<%=rnd%>').focus();
        });

        // 1. PENCATATAN ANGGOTA (SCAN KTM)
        async function catatKunjungan<%=rnd%>(event) {
            event.preventDefault();
            const inputField = document.getElementById('kodeAnggota<%=rnd%>');
            const identitas = inputField.value.trim();
            if (!identitas) return;
            inputField.disabled = true; 

            try {
                const req = await fetch('<%= linkDetail %>', {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8', 'X-NUI-CSRF': '<%=kioskCsrf%>' },
                    body: 'action=scan&nui_csrf=<%=kioskCsrf%>&kode=' + encodeURIComponent(identitas)
                });
                const response = await req.json();
                window.alert(response.message);
            } catch (error) {
                window.alert('<%= Common.getBahasaConfigJS("Terjadi kesalahan jaringan.") %>');
            } finally {
                inputField.value = '';
                inputField.disabled = false;
                setTimeout(function() { inputField.focus(); }, 500);
            }
        }

        // 2. PENCATATAN TAMU UMUM
        let modalInstTamu;
        function bukaModalTamu<%=rnd%>() {
            document.getElementById('formTamu<%=rnd%>').reset();
            const modalEl = document.getElementById('modalTamu<%=rnd%>');
            modalInstTamu = new bootstrap.Modal(modalEl);
            modalInstTamu.show();
            modalEl.addEventListener('shown.bs.modal', function() { document.getElementById('namaTamu<%=rnd%>').focus(); }, {once: true});
        }

        async function simpanTamu<%=rnd%>() {
            const nama = document.getElementById('namaTamu<%=rnd%>').value.trim();
            const alamat = document.getElementById('alamatTamu<%=rnd%>').value.trim();
            const ket = document.getElementById('ketTamu<%=rnd%>').value.trim();
            if (!nama || !alamat) {
                window.alert('<%= Common.getBahasaConfigJS("Nama dan Alamat wajib diisi.") %>');
                return;
            }
            try {
                const params = new URLSearchParams();
                params.append('action', 'guest');
                params.append('nama', nama);
                params.append('alamat', alamat);
                params.append('keterangan', ket);
                params.append('nui_csrf', '<%=kioskCsrf%>');

                const req = await fetch('<%= linkDetail %>', {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8', 'X-NUI-CSRF': '<%=kioskCsrf%>' },
                    body: params.toString()
                });
                const response = await req.json();
                if (response.status === 'success') {
                    window.alert(response.message);
                    modalInstTamu.hide();
                } else {
                    window.alert(response.message);
                }
            } catch (error) {
                window.alert('<%= Common.getBahasaConfigJS("Gagal menyimpan data.") %>');
            }
        }

        // 3. LIHAT RIWAYAT DENGAN AJAX PAGING (MENGGUNAKAN KONKATENASI STRING UNTUK JSP)[cite: 5]
        let modalInstRiwayat;
        async function bukaModalRiwayat<%=rnd%>(page) {
            const body = document.getElementById('tabelBodyKunjungan<%=rnd%>');
            const paging = document.getElementById('paginationCtrl<%=rnd%>');
            body.innerHTML = '<tr><td colspan="4" class="text-center py-4"><div class="spinner-border text-primary"></div></td></tr>';
            
            if (!modalInstRiwayat) modalInstRiwayat = new bootstrap.Modal(document.getElementById('modalRiwayat<%=rnd%>'));
            modalInstRiwayat.show();

            try {
                const req = await fetch('<%= linkDetail %>&action=list&page=' + page);
                const res = await req.json();
                
                if (res.status === 'success') {
                    body.innerHTML = res.data.length ? '' : '<tr><td colspan="4" class="text-center py-4 text-muted">Belum ada kunjungan hari ini.</td></tr>';
                    
                    res.data.forEach(function(item, index) {
                        const no = (page * res.limit) + (index + 1);
                        const statusBadge = item.status === 'Anggota' ? 'bg-info' : 'bg-secondary';
                        
                        let rowHtml = '<tr>' +
                                '<td class="ps-4">' + no + '</td>' +
                                '<td class="fw-bold text-dark">' + String(item.nama).replace(/[&<>"']/g, function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];}) + '</td>' +
                                '<td><span class="badge ' + statusBadge + ' px-3 rounded-pill">' + item.status + '</span></td>' +
                                '<td class="text-center text-muted fw-medium">' + item.waktu + '</td>' +
                            '</tr>';
                        body.innerHTML += rowHtml;
                    });

                    // Render Kontrol Navigasi Halaman (Tanpa Simbol EL)
                    const totalPage = Math.ceil(res.total / res.limit);
                    document.getElementById('infoPaging<%=rnd%>').innerText = 'Total: ' + res.total + ' Data';
                    
                    paging.innerHTML = '';
                    for (let i = 0; i < totalPage; i++) {
                        const activeClass = (i === page) ? 'active' : '';
                        const labelHal = i + 1;
                        let pageHtml = '<li class="page-item ' + activeClass + '">' +
                                '<a class="page-link shadow-none" href="javascript:void(0)" onclick="bukaModalRiwayat<%=rnd%>(' + i + ')">' + labelHal + '</a>' +
                            '</li>';
                        paging.innerHTML += pageHtml;
                    }
                }
            } catch (error) {
                body.innerHTML = '<tr><td colspan="4" class="text-center text-danger">Terjadi kesalahan saat memuat data.</td></tr>';
            }
        }
    </script>
<jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp"/>
</body>
</html>
