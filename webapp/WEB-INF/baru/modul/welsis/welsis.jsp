<%@page import="java.util.List"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    String judul = "";
    String motto = "";
    String logoPerguruanTinggi = "";
    String backgroundPerguruanTinggi = "";
    String cssTema = ""; 

    try {
        PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);
        if (perguruanTinggi != null) {
            judul = perguruanTinggi.getNama() != null ? perguruanTinggi.getNama() : "";
            motto = perguruanTinggi.getMotto() != null ? perguruanTinggi.getMotto() : "";
            logoPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
            backgroundPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "background_perguruanTinggi_");
            
            String rawCss = perguruanTinggi.getCss();
            if (rawCss != null && !rawCss.trim().isEmpty()) {
                String fileName = rawCss.substring(rawCss.lastIndexOf("/") + 1);
                cssTema = "/css/baru/" + fileName;
            }
        }
    } catch (Exception e) {
        System.err.println("Kesalahan institusi: " + e.getMessage());
    }

    String rnd = Common.getGeneratedBarCode(7);
    long cacheBuster = System.currentTimeMillis();
    String linkService = Common.ROOT + "/welsis?hanya_tampil_jsp=true&p=welsis&s=_welsis_service";
    String bgImage = (backgroundPerguruanTinggi != null && !backgroundPerguruanTinggi.isEmpty()) 
                     ? backgroundPerguruanTinggi : Common.ROOT + "/img/main.jpg";
%>

<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= Common.getBahasaConfig("Anjungan Absensi Siswa") %> - <%= judul %></title>
    
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
            display: flex; align-items: center;
        }
        body::before {
            content: ""; position: absolute; top: 0; left: 0; width: 100%; height: 100%;
            background: rgba(240, 248, 255, 0.85); z-index: -1;
        }
        .container { z-index: 1; }
        .card-anjungan {
            background: white; border-radius: 25px;
            box-shadow: 0 15px 40px rgba(0,0,0,0.15);
            border-top: 8px solid var(--theme-primary, #0d6efd);
        }
        .clock-display {
            font-size: 3rem; font-weight: 800; color: var(--theme-primary, #0d6efd);
            text-shadow: 2px 2px 4px rgba(0,0,0,0.1); letter-spacing: 2px;
        }
        .date-display { font-size: 1.2rem; font-weight: 600; color: #6c757d; }
        .input-barcode {
            font-size: 2.5rem; text-align: center; font-weight: bold; 
            background-color: #f8f9fa; border: 3px solid var(--theme-primary, #0d6efd); 
            border-radius: 15px; height: 90px; letter-spacing: 5px;
            box-shadow: inset 0 3px 6px rgba(0,0,0,0.1);
        }
        .input-barcode:focus { background-color: #fff; box-shadow: 0 0 15px rgba(13, 110, 253, 0.3); }
        .instruction-text { font-size: 1.3rem; font-weight: 600; color: #495057; }
    </style>
</head>
<body>

    <div class="container">
        <div class="row justify-content-center">
            <div class="col-md-10 col-lg-8">
                <div class="card card-anjungan p-4 p-md-5 text-center animate__animated animate__zoomIn">
                    
                    <div class="mb-4">
                        <div class="clock-display" id="realtimeClock<%=rnd%>">00:00:00</div>
                        <div class="date-display" id="realtimeDate<%=rnd%>">Memuat Tanggal...</div>
                    </div>

                    <% if (!logoPerguruanTinggi.isEmpty()) { %>
                        <img src="<%= logoPerguruanTinggi %>" width="110" class="mx-auto mb-3">
                    <% } %>
                    <h2 class="fw-bold text-dark"><%= judul %></h2>
                    <% if (!motto.isEmpty()) { %>
                        <p class="fst-italic text-muted">"<%= motto %>"</p>
                    <% } %>

                    <hr class="my-4 opacity-25">

                    <p class="instruction-text mb-3">
                        <i class="fas fa-id-badge me-2 text-warning fs-3"></i> 
                        <%= Common.getBahasaConfig("Silakan Scan Kartu Siswa Anda Di Sini") %>
                    </p>

                    <form id="formAbsen<%=rnd%>" onsubmit="prosesAbsen<%=rnd%>(event)">
                        <div class="mb-4 px-md-4">
                            <input type="text" id="kodeSiswa<%=rnd%>" class="form-control input-barcode" autocomplete="off" autofocus required placeholder="SCAN BARCODE">
                        </div>
                    </form>

                    <div class="d-flex justify-content-center mt-3">
                        <button type="button" class="btn btn-dark rounded-pill px-4 py-2 fw-bold shadow-sm" onclick="bukaRiwayat<%=rnd%>(0)">
                            <i class="fas fa-list-alt me-2"></i> <%= Common.getBahasaConfig("Lihat Log Absensi Hari Ini") %>
                        </button>
                    </div>

                </div>
            </div>
        </div>
    </div>

    <!-- MODAL: RIWAYAT ABSENSI SISWA -->
    <div class="modal fade" id="modalRiwayat<%=rnd%>" tabindex="-1">
        <div class="modal-dialog modal-xl modal-dialog-centered">
            <div class="modal-content border-0 shadow-lg rounded-4">
                <div class="modal-header bg-dark text-white">
                    <h5 class="modal-title fw-bold"><i class="fas fa-history me-2"></i><%= Common.getBahasaConfig("Log Kunjungan Siswa") %></h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover mb-0">
                            <thead class="table-light">
                                <tr>
                                    <th class="ps-4">No.</th>
                                    <th><%= Common.getBahasaConfig("Nama Siswa") %></th>
                                    <th>NIS / NISN</th>
                                    <th><%= Common.getBahasaConfig("Kelas") %></th>
                                    <th class="text-center"><%= Common.getBahasaConfig("Jam Absen") %></th>
                                </tr>
                            </thead>
                            <tbody id="tabelRiwayat<%=rnd%>"></tbody>
                        </table>
                    </div>
                </div>
                <div class="modal-footer bg-light justify-content-between">
                    <span id="infoTotal<%=rnd%>" class="fw-bold text-muted small"></span>
                    <nav><ul class="pagination pagination-sm mb-0" id="pagingCtrl<%=rnd%>"></ul></nav>
                </div>
            </div>
        </div>
    </div>

    <script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
    
    <script>
        // Realtime Clock
        function updateClock() {
            const now = new Date();
            const timeOptions = { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false };
            const dateOptions = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
            document.getElementById('realtimeClock<%=rnd%>').innerText = now.toLocaleTimeString('id-ID', timeOptions);
            document.getElementById('realtimeDate<%=rnd%>').innerText = now.toLocaleDateString('id-ID', dateOptions);
        }
        setInterval(updateClock, 1000);
        updateClock();

        // Kunci Focus ke Input Barcode
        document.addEventListener("DOMContentLoaded", function() {
            const inputField = document.getElementById('kodeSiswa<%=rnd%>');
            inputField.focus();
            document.addEventListener('click', function() {
                if(!document.getElementById('modalRiwayat<%=rnd%>').classList.contains('show')) {
                    inputField.focus();
                }
            });
        });

        // Proses Scan Absensi Siswa
        async function prosesAbsen<%=rnd%>(event) {
            event.preventDefault();
            const inputField = document.getElementById('kodeSiswa<%=rnd%>');
            const identitas = inputField.value.trim();
            
            if (!identitas) return;
            inputField.disabled = true; 

            try {
                const req = await fetch('<%= linkService %>', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: 'action=scan&kode=' + encodeURIComponent(identitas)
                });
                const response = await req.json();
                
                if (response.status === 'success') {
                    Swal.fire({ title: 'Berhasil', text: response.message, icon: 'success', timer: 2000, showConfirmButton: false });
                } else {
                    Swal.fire({ title: 'Ditolak', text: response.message, icon: 'error', timer: 3000, showConfirmButton: false });
                }
            } catch (error) {
                Swal.fire('Error', 'Gangguan koneksi jaringan.', 'error');
            } finally {
                inputField.value = '';
                inputField.disabled = false;
                setTimeout(function() { inputField.focus(); }, 100);
            }
        }

        // Lihat Riwayat Absensi
        async function bukaRiwayat<%=rnd%>(page) {
            const modal = new bootstrap.Modal(document.getElementById('modalRiwayat<%=rnd%>'));
            const tbody = document.getElementById('tabelRiwayat<%=rnd%>');
            
            tbody.innerHTML = '<tr><td colspan="5" class="text-center py-4"><div class="spinner-border text-primary"></div></td></tr>';
            modal.show();

            try {
                const req = await fetch('<%= linkService %>&action=list&page=' + page);
                const res = await req.json();
                
                if(res.status === 'success') {
                    tbody.innerHTML = res.data.length ? '' : '<tr><td colspan="5" class="text-center py-4 text-muted">Belum ada absensi hari ini.</td></tr>';
                    
                    res.data.forEach(function(d, idx) {
                        const no = (page * res.limit) + (idx + 1);
                        var row = '<tr>' +
                                  '<td class="ps-4">' + no + '</td>' +
                                  '<td class="fw-bold text-primary">' + d.nama + '</td>' +
                                  '<td>' + d.identitas + '</td>' +
                                  '<td><span class="badge bg-secondary">' + d.kelas + '</span></td>' +
                                  '<td class="text-center fw-bold text-success">' + d.waktu + '</td>' +
                                  '</tr>';
                        tbody.innerHTML += row;
                    });

                    document.getElementById('infoTotal<%=rnd%>').innerText = 'Total: ' + res.total + ' Absensi';
                    const paging = document.getElementById('pagingCtrl<%=rnd%>');
                    paging.innerHTML = '';
                    const totalPage = Math.ceil(res.total / res.limit);
                    
                    for (let i = 0; i < totalPage; i++) {
                        const act = (i === page) ? 'active' : '';
                        paging.innerHTML += '<li class="page-item ' + act + '"><a class="page-link shadow-none" href="javascript:void(0)" onclick="bukaRiwayat<%=rnd%>(' + i + ')">' + (i+1) + '</a></li>';
                    }
                }
            } catch (e) { 
                tbody.innerHTML = '<tr><td colspan="5" class="text-center py-3 text-danger">Gagal memuat log absensi.</td></tr>'; 
            }
        }
    </script>
<jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp"/>
</body>
</html>