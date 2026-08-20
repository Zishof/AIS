<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // 1. INISIALISASI VARIABEL SESUAI PERMINTAAN
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
        PerguruanTinggi perguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request);
        
        if (perguruanTinggi != null) {
            judul = perguruanTinggi.getNama() != null ? perguruanTinggi.getNama() : "";
            motto = perguruanTinggi.getMotto() != null ? perguruanTinggi.getMotto() : "";
            telepon = perguruanTinggi.getTelepon() != null ? perguruanTinggi.getTelepon() : "";
            alamat1 = perguruanTinggi.getAlamat1() != null ? perguruanTinggi.getAlamat1() : "";
            email = perguruanTinggi.getEmail() != null ? perguruanTinggi.getEmail() : "";
            
            // MENGAMBIL SETTINGAN TEMA AKTIF[cite: 5, 6]
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
    }

    String rnd = Common.getGeneratedBarCode(7);
    long cacheBuster = System.currentTimeMillis();
    String linkService = Common.ROOT + "/tamu?hanya_tampil_jsp=true&p=tamu&s=_tamu_service";
    
    // Logika Background[cite: 6]
    String bgImage = (backgroundPerguruanTinggi != null && !backgroundPerguruanTinggi.isEmpty()) 
                     ? backgroundPerguruanTinggi : Common.ROOT + "/img/office-bg.jpeg";
%>

<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= Common.getBahasaConfig("Pendaftaran Kunjungan") %> - <%= judul %></title>
    
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/animate.css/4.1.1/animate.min.css"/>
    
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
        body::before {
            content: "";
            position: absolute; top: 0; left: 0; width: 100%; height: 100%;
            background: rgba(255, 255, 255, 0.8); z-index: 0;
        }
        .container-wrapper { z-index: 1; position: relative; width: 100%; }
        .card-main {
            background: white; border-radius: 20px;
            box-shadow: 0 15px 35px rgba(0,0,0,0.1);
            border-top: 6px solid var(--theme-primary);
        }
        .btn-register { font-size: 1.3rem; padding: 0.8rem; border-radius: 50px; }
        .footer-info { font-size: 0.85rem; color: #6c757d; }
    </style>
</head>
<body>

    <div class="container-wrapper">
        <div class="container">
            <div class="row justify-content-center">
                <div class="col-md-10 col-lg-7">
                    <div class="card card-main p-4 p-md-5 text-center animate__animated animate__fadeInUp">
                        
                        <% if (!logoPerguruanTinggi.isEmpty()) { %>
                            <img src="<%= logoPerguruanTinggi %>" width="130" class="mx-auto mb-3">
                        <% } %>
                        
                        <h1 class="fw-bold text-primary mb-1"><%= judul %></h1>
                        <% if (!motto.isEmpty()) { %>
                            <p class="fst-italic text-muted mb-4">"<%= motto %>"</p>
                        <% } %>
                        
                        <div class="py-4">
                            <h4 class="mb-4 fw-semibold"><%= Common.getBahasaConfig("Buku Tamu Pengunjung") %></h4>
                            <div class="d-grid gap-3">
                                <button class="btn btn-primary btn-register shadow" onclick="bukaModalTamu<%=rnd%>()">
                                    <i class="fas fa-user-edit me-2"></i> <%= Common.getBahasaConfig("Isi Data Kunjungan") %>
                                </button>
                                <button class="btn btn-outline-secondary rounded-pill py-2" onclick="bukaModalRiwayat<%=rnd%>(0)">
                                    <i class="fas fa-history me-2"></i> <%= Common.getBahasaConfig("Lihat Kunjungan Hari Ini") %>
                                </button>
                            </div>
                        </div>

                        <!-- INFORMASI KONTAK INSTITUSI -->
                        <div class="footer-info mt-4 pt-4 border-top">
                            <div class="row g-2">
                                <div class="col-12"><i class="fas fa-map-marker-alt me-2 text-primary"></i> <%= alamat1 %></div>
                                <div class="col-md-6"><i class="fas fa-phone me-2 text-primary"></i> <%= telepon %></div>
                                <div class="col-md-6"><i class="fas fa-envelope me-2 text-primary"></i> <%= email %></div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- MODAL FORM TAMU (GUEST) -->
    <div class="modal fade" id="modalTamu<%=rnd%>" tabindex="-1" data-bs-backdrop="static">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 shadow-lg rounded-4">
                <div class="modal-header bg-primary text-white">
                    <h5 class="modal-title fw-bold"><i class="fas fa-user-tag me-2"></i><%= Common.getBahasaConfig("Formulir Tamu") %></h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4">
                    <form id="formTamu<%=rnd%>">
                        <div class="mb-3">
                            <label class="form-label fw-bold text-muted small"><%= Common.getBahasaConfig("Nama Lengkap") %> *</label>
                            <input type="text" id="namaTamu<%=rnd%>" class="form-control form-control-lg border-2" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label fw-bold text-muted small"><%= Common.getBahasaConfig("Alamat / Instansi") %> *</label>
                            <input type="text" id="alamatTamu<%=rnd%>" class="form-control border-2" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label fw-bold text-muted small"><%= Common.getBahasaConfig("Nomor HP / WhatsApp") %> *</label>
                            <input type="tel" id="hpTamu<%=rnd%>" class="form-control border-2" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label fw-bold text-muted small"><%= Common.getBahasaConfig("Keperluan") %> *</label>
                            <textarea id="keperluanTamu<%=rnd%>" class="form-control border-2" rows="3" required></textarea>
                        </div>
                        <div class="mb-3">
                            <label class="form-label fw-bold text-muted small"><%= Common.getBahasaConfig("Keterangan") %></label>
                            <input type="text" id="ketTamu<%=rnd%>" class="form-control border-2">
                        </div>
                    </form>
                </div>
                <div class="modal-footer bg-light border-0">
                    <button type="button" class="btn btn-link text-decoration-none text-muted" data-bs-dismiss="modal"><%= Common.getBahasaConfig("Batal") %></button>
                    <button type="button" class="btn btn-primary px-5 rounded-pill fw-bold" onclick="simpanTamu<%=rnd%>()"><%= Common.getBahasaConfig("Simpan") %></button>
                </div>
            </div>
        </div>
    </div>

    <!-- MODAL RIWAYAT KUNJUNGAN -->
    <div class="modal fade" id="modalRiwayat<%=rnd%>" tabindex="-1">
        <div class="modal-dialog modal-lg modal-dialog-centered">
            <div class="modal-content border-0 shadow-lg rounded-4">
                <div class="modal-header bg-dark text-white">
                    <h5 class="modal-title fw-bold"><i class="fas fa-list-ul me-2"></i><%= Common.getBahasaConfig("Daftar Kunjungan Hari Ini") %></h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover mb-0">
                            <thead class="table-light">
                                <tr>
                                    <th class="ps-3"><%= Common.getBahasaConfig("Nama") %></th>
                                    <th><%= Common.getBahasaConfig("Alamat/Instansi") %></th>
                                    <th><%= Common.getBahasaConfig("Keperluan") %></th>
                                    <th class="text-center"><%= Common.getBahasaConfig("Jam") %></th>
                                </tr>
                            </thead>
                            <tbody id="tabelRiwayat<%=rnd%>"></tbody>
                        </table>
                    </div>
                </div>
                <div class="modal-footer bg-light">
                    <nav><ul class="pagination pagination-sm mb-0" id="pagingCtrl<%=rnd%>"></ul></nav>
                </div>
            </div>
        </div>
    </div>

    <script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
    
    <script>
        let instModalTamu;
        function bukaModalTamu<%=rnd%>() {
            document.getElementById('formTamu<%=rnd%>').reset();
            instModalTamu = new bootstrap.Modal(document.getElementById('modalTamu<%=rnd%>'));
            instModalTamu.show();
        }

        async function simpanTamu<%=rnd%>() {
            const data = {
                action: 'guest',
                nama: document.getElementById('namaTamu<%=rnd%>').value.trim(),
                alamat: document.getElementById('alamatTamu<%=rnd%>').value.trim(),
                hp: document.getElementById('hpTamu<%=rnd%>').value.trim(),
                keperluan: document.getElementById('keperluanTamu<%=rnd%>').value.trim(),
                keterangan: document.getElementById('ketTamu<%=rnd%>').value.trim()
            };

            if(!data.nama || !data.alamat || !data.hp || !data.keperluan) {
                Swal.fire('Peringatan', '<%= Common.getBahasaConfigJS("Mohon isi semua kolom bertanda *") %>', 'warning');
                return;
            }

            try {
                const resp = await fetch('<%= linkService %>', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                    body: new URLSearchParams(data)
                });
                const res = await resp.json();
                if(res.status === 'success') {
                    Swal.fire({ icon: 'success', title: 'Berhasil', text: res.message, timer: 2000, showConfirmButton: false });
                    instModalTamu.hide();
                } else {
                    Swal.fire('Gagal', res.message, 'error');
                }
            } catch (e) { Swal.fire('Error', 'Gagal menghubungi server.', 'error'); }
        }

        async function bukaModalRiwayat<%=rnd%>(page) {
            const modal = new bootstrap.Modal(document.getElementById('modalRiwayat<%=rnd%>'));
            const tbody = document.getElementById('tabelRiwayat<%=rnd%>');
            tbody.innerHTML = '<tr><td colspan="4" class="text-center py-4">Memuat data...</td></tr>';
            modal.show();

            try {
                const resp = await fetch('<%= linkService %>&action=list&page=' + page);
                const res = await resp.json();
                if(res.status === 'success') {
                    tbody.innerHTML = res.data.length ? '' : '<tr><td colspan="4" class="text-center py-3 text-muted">Belum ada data hari ini.</td></tr>';
                    res.data.forEach(function(d) {
                        var row = '<tr>' +
                                  '<td class="ps-3"><b>' + d.nama + '</b></td>' +
                                  '<td>' + d.alamat + '</td>' +
                                  '<td>' + d.keperluan + '</td>' +
                                  '<td class="text-center">' + d.waktu + '</td>' +
                                  '</tr>';
                        tbody.innerHTML += row;
                    });
                }
            } catch (e) { tbody.innerHTML = '<tr><td colspan="4" class="text-center py-3 text-danger">Gagal memuat riwayat.</td></tr>'; }
        }
    </script>
<jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp"/>
</body>
</html>