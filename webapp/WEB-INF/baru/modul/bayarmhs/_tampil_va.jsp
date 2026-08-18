<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="java.net.URLEncoder"%>

<%
    // --- MENGAMANKAN AKSES PENGGUNA ---
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.sendRedirect(Common.ROOT + "/login.jsp");
        return;
    } 

    String rnd = request.getParameter("rnd") != null ? request.getParameter("rnd") : Common.getGeneratedBarCode(5);

    // --- EKSTRAKSI PARAMETER ---
    String va = request.getParameter("va");
    String kodeBankLain = request.getParameter("kodeBankLain");
    String nama = request.getParameter("nama");
    String nominal = request.getParameter("nominal");
    String biayaAdministrasi = request.getParameter("biayaAdministrasi");
    String biayaTotal = request.getParameter("biayaTotal");
    String terbilang = request.getParameter("terbilang");
    String kadalurasa = request.getParameter("kadalurasa");
    boolean tampilBiayaAdministrasi = "true".equalsIgnoreCase(request.getParameter("tampilBiayaAdministrasi"));
    
    String qrImageUrl = request.getParameter("qr");
    String rawBarcode = request.getParameter("rawBarcode");
    boolean isQRIS = "true".equalsIgnoreCase(request.getParameter("isQRIS"));

    // --- LOGIKA PENAMPILAN KODE BANK LAIN ---
    boolean tampilBankLain = false;
    if (kodeBankLain != null && !kodeBankLain.trim().isEmpty()) {
        if (va == null || !kodeBankLain.trim().equalsIgnoreCase(va.trim())) {
            tampilBankLain = true;
        }
    }

    // --- KONFIGURASI VISIBILITAS CETAK ---
    boolean showCetak = false;
    try {
        showCetak = Common.getKonfigurasi("tampilkan_cetak_bayar", Konfigurasi.AKTIF).getNilai().equalsIgnoreCase(Konfigurasi.AKTIF);
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_tampil_va.jsp:43");}

    // --- MERAKIT URL PENCETAKAN LAPORAN ---
    String keterangan1 = Common.getBahasaConfig("* Catatlah kode pembayaran di atas, Anda dapat melakukan transaksi melalui saluran ATM, Teller, atau Mobile Banking.");
    if (isQRIS) {
        keterangan1 = Common.getBahasaConfig("* Silakan pindai (scan) QR Code di atas menggunakan aplikasi dompet digital atau Mobile Banking Anda.");
    }
    
    String queryString = request.getQueryString() != null ? request.getQueryString() : "";
    String cetakUrl = "";
    try {
        cetakUrl = Common.ROOT + "/LaporanVa?" + queryString + "&keteranganNominal1=" + URLEncoder.encode(keterangan1, "UTF-8");
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_tampil_va.jsp:55");}
%>

<style>
    .va-box-<%=rnd%> { transition: transform 0.2s ease, box-shadow 0.2s ease; }
    .va-box-<%=rnd%>:hover { transform: translateY(-2px); box-shadow: 0 .5rem 1rem rgba(0,0,0,.1) !important; }
    .w-30px-<%=rnd%> { width: 30px; text-align: center; }
    .btn-salin-<%=rnd%> { transition: all 0.2s; }
    .btn-salin-<%=rnd%>:hover { transform: scale(1.1); }
</style>

<div class="container py-5 animate__animated animate__fadeIn">
    <div class="row justify-content-center">
        <div class="col-lg-8 col-xl-7">
            
            <div class="alert alert-success shadow-sm border-0 rounded-4 mb-4 d-flex align-items-center p-4">
                <i class="fas fa-check-circle fa-3x me-4 text-success opacity-75"></i>
                <div>
                    <h5 class="fw-bold mb-1 text-dark"><%= Common.getBahasaConfig("Instruksi Pembayaran Berhasil Dibuat!") %></h5>
                    <span class="text-secondary"><%= Common.getBahasaConfig("Silakan selesaikan kewajiban pembayaran Anda sebelum batas waktu berakhir.") %></span>
                </div>
            </div>

            <div class="card border-0 shadow-lg rounded-4 overflow-hidden">
                
                <div class="card-header bg-gradient bg-primary text-white p-4 border-0 d-flex align-items-center">
                    <div class="bg-white bg-opacity-25 p-3 rounded-circle me-4 shadow-sm">
                        <i class="<%= isQRIS ? "fas fa-qrcode" : "fas fa-file-invoice-dollar" %> fa-2x"></i>
                    </div>
                    <h4 class="fw-bold mb-0 tracking-wide"><%= isQRIS ? Common.getBahasaConfig("Pindai Kode QRIS") : Common.getBahasaConfig("Rincian Akun Virtual (VA)") %></h4>
                </div>
                
                <div class="card-body p-4 p-md-5 bg-white">
                    <div class="row gy-4">
                        
                        <% if (isQRIS && rawBarcode != null && !rawBarcode.trim().isEmpty()) { 
                            String fallbackUrl = "https://api.qrserver.com/v1/create-qr-code/?size=350x350&data=" + URLEncoder.encode(rawBarcode, "UTF-8");
                        %>
                            <div class="col-12 text-center mb-2 pb-4 border-bottom">
                                <p class="text-secondary fw-bold text-uppercase tracking-wide mb-3">
                                    <i class="fas fa-mobile-alt me-2 text-primary"></i><%= Common.getBahasaConfig("Pindai Gambar Ini Menggunakan Aplikasi Anda") %>
                                </p>
                                <div class="d-inline-block bg-white p-3 rounded-4 shadow-sm border border-2 border-primary mb-3">
                                    <img src="<%= qrImageUrl != null ? qrImageUrl : "" %>" 
                                         onerror="this.onerror=null; this.src='<%= fallbackUrl %>';" 
                                         alt="QRIS Barcode" class="img-fluid" style="width: 250px; height: 250px; object-fit: contain;">
                                </div>
                                <div class="bg-light rounded-3 p-2 mx-auto border" style="max-width: 450px; word-wrap: break-word;">
                                    <small class="text-muted d-block text-truncate" style="font-size: 0.70rem;" title="<%= rawBarcode %>"><%= rawBarcode %></small>
                                </div>
                            </div>
                        <% } %>

                        <% if (!isQRIS && va != null && !va.trim().isEmpty() && !va.equals("-")) { %>
                            <div class="col-12 text-center mb-2">
                                <p class="text-secondary fw-bold text-uppercase tracking-wide mb-2"><%= Common.getBahasaConfig("Nomor Referensi / Kode Pembayaran") %></p>
                                <div class="va-box-<%=rnd%> d-inline-flex align-items-center justify-content-between bg-light border border-2 border-primary rounded-4 py-3 px-4 shadow-sm w-100" style="max-width: 450px;">
                                    <h2 class="display-6 fw-bold text-info mb-0 flex-grow-1" id="txtVA_<%=rnd%>"><%= va %></h2>
                                    <button class="btn btn-primary rounded-circle shadow-sm p-3 ms-3 btn-salin-<%=rnd%>" onclick="window.salinTeks<%=rnd%>('txtVA_<%=rnd%>')" title="<%= Common.getBahasaConfig("Salin Kode Pembayaran") %>" style="width: 52px; height: 52px;">
                                        <i class="far fa-copy fa-lg"></i>
                                    </button>
                                </div>
                            </div>
                        <% } %>

                        <% if (!isQRIS && tampilBankLain) { %>
                        <div class="col-12 text-center mb-2 pt-2">
                            <p class="text-secondary fw-bold text-uppercase tracking-wide mb-2"><%= Common.getBahasaConfig("Kode Khusus Melalui Saluran Bank Lain") %></p>
                            <div class="va-box-<%=rnd%> d-inline-flex align-items-center justify-content-between bg-light border border-2 border-info rounded-4 py-2 px-4 shadow-sm w-100" style="max-width: 400px;">
                                <h3 class="fw-bold text-info mb-0 flex-grow-1" id="txtVABankLain_<%=rnd%>"><%= kodeBankLain %></h3>
                                <button class="btn btn-info text-white rounded-circle shadow-sm p-2 ms-3 btn-salin-<%=rnd%>" onclick="window.salinTeks<%=rnd%>('txtVABankLain_<%=rnd%>')" title="<%= Common.getBahasaConfig("Salin Kode Khusus") %>" style="width: 42px; height: 42px;">
                                    <i class="far fa-copy"></i>
                                </button>
                            </div>
                        </div>
                        <% } %>

                        <div class="col-12"><hr class="text-muted opacity-20 my-1"></div>

                        <div class="col-md-12">
                            <table class="table table-borderless table-sm mb-0 align-middle">
                                <tbody>
                                    <tr>
                                        <td class="text-muted fw-semibold py-2" style="width: 40%;"><i class="fas fa-user text-secondary me-2 w-30px-<%=rnd%>"></i> <%= Common.getBahasaConfig("Atas Nama") %></td>
                                        <td class="fw-bold fs-5 text-dark py-2 text-end"><%= nama != null ? nama : "-" %></td>
                                    </tr>
                                    <tr>
                                        <td class="text-muted fw-semibold py-2"><i class="far fa-clock text-secondary me-2 w-30px-<%=rnd%>"></i> <%= Common.getBahasaConfig("Batas Waktu") %></td>
                                        <td class="fw-bold fs-5 text-danger py-2 text-end"><%= kadalurasa != null ? kadalurasa : "-" %></td>
                                    </tr>
                                    <tr>
                                        <td class="text-muted fw-semibold py-2"><i class="fas fa-money-bill-wave text-secondary me-2 w-30px-<%=rnd%>"></i> <%= Common.getBahasaConfig("Nominal Tagihan") %></td>
                                        <td class="fw-bold fs-5 text-dark py-2 text-end"><%= nominal != null ? nominal : "-" %></td>
                                    </tr>
                                    
                                    <% if (tampilBiayaAdministrasi) { %>
                                    <tr>
                                        <td class="text-muted fw-semibold py-2"><i class="fas fa-plus-circle text-secondary me-2 w-30px-<%=rnd%>"></i> <%= Common.getBahasaConfig("Biaya Administrasi") %></td>
                                        <td class="fw-bold fs-5 text-warning py-2 text-end"><%= biayaAdministrasi != null ? biayaAdministrasi : "Rp 0" %></td>
                                    </tr>
                                    <tr>
                                        <td class="text-dark fw-bold py-3 border-top"><i class="fas fa-wallet text-primary me-2 w-30px-<%=rnd%>"></i> <%= Common.getBahasaConfig("Total Pembayaran") %></td>
                                        <td class="fw-bold fs-3 text-primary py-3 border-top text-end">
                                            <%= biayaTotal != null ? biayaTotal : nominal %>
                                        </td>
                                    </tr>
                                    <% } %>
                                </tbody>
                            </table>
                            
                            <div class="bg-success bg-opacity-10 p-3 rounded-4 mt-3 border-start border-4 border-success shadow-sm">
                                <small class="text-success d-block fw-bold mb-1 text-uppercase"><%= Common.getBahasaConfig("Terbilang:") %></small>
                                <span class="fw-bold text-dark fst-italic">"<%= terbilang != null ? terbilang : "-" %>"</span>
                            </div>
                        </div>
                        
                        <div class="col-12"><hr class="text-muted opacity-20 my-1"></div>

                        <div class="col-md-12">
                            <div class="alert alert-warning border-0 shadow-sm bg-warning bg-opacity-10 mb-0 rounded-4 p-4">
                                <div class="d-flex mb-3">
                                    <i class="fas fa-exclamation-triangle text-warning fa-2x me-3 mt-1"></i>
                                    <div>
                                        <h6 class="fw-bold text-dark mb-1"><%= Common.getBahasaConfig("Petunjuk Penting Transaksi") %></h6>
                                        <p class="text-secondary small mb-0"><%= keterangan1 %></p>
                                    </div>
                                </div>
                                <div class="bg-white p-3 rounded-3 border border-warning shadow-sm">
                                    <span class="text-dark"><%= Common.getBahasaConfig("Sangat disarankan untuk memastikan nominal transaksi Anda") %> <b><%= Common.getBahasaConfig("SAMA PERSIS") %></b> <%= Common.getBahasaConfig("dengan total tagihan, yakni senilai") %> <span class="fw-bold fs-5 text-danger"><%= (tampilBiayaAdministrasi && biayaTotal != null) ? biayaTotal : nominal %></span>.</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                
                <div class="card-footer bg-light border-top p-4 text-center">
                    <div class="d-flex justify-content-center flex-wrap gap-3">
                        <button class="btn btn-secondary px-5 py-2 rounded-pill fw-bold shadow-sm transition-all" data-bs-dismiss="modal" onclick="window.tutupDanSegarkanRiwayatVA<%=rnd%>()">
                            <i class="fas fa-sync-alt me-2"></i><%= Common.getBahasaConfig("Tutup & Segarkan Riwayat") %>
                        </button>

                        <% if (showCetak) { %>
                        <button class="btn btn-success px-5 py-2 rounded-pill fw-bold shadow-sm transition-all" onclick="window.tampilkanPdfCetakVA<%=rnd%>('<%=cetakUrl%>')">
                            <i class="fas fa-print me-2"></i><%= Common.getBahasaConfig("Cetak Instruksi") %>
                        </button>
                        <% } %>
                    </div>
                </div>
            </div>

        </div>
    </div>
</div>

<script>
    // --- 1. MODAL IFRAME UNTUK CETAK VA ---
    window.tampilkanPdfCetakVA<%=rnd%> = function(urlCetak) {
        const modalId = 'modalCetakVA<%=rnd%>'; 
        const existingModal = document.getElementById(modalId);
        
        if (existingModal) { 
            existingModal.remove(); 
            document.querySelectorAll('.modal-backdrop.va-cetak').forEach(b => b.remove()); 
        }

        const modalHtml = '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true" style="z-index: 1060;">' +
                          '  <div class="modal-dialog modal-xl modal-dialog-centered">' +
                          '    <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">' +
                          '      <div class="modal-header bg-gradient bg-primary text-white border-0 py-3">' +
                          '        <h5 class="modal-title fw-bold"><i class="fas fa-file-invoice me-2"></i> <%= Common.getBahasaConfig("Instruksi Pembayaran") %></h5>' +
                          '        <button type="button" class="btn-close btn-close-white shadow-none" data-bs-dismiss="modal"></button>' +
                          '      </div>' +
                          '      <div class="modal-body p-0 bg-light">' +
                          '        <iframe src="' + urlCetak + '" width="100%" height="600px" style="border: none;"></iframe>' +
                          '      </div>' +
                          '      <div class="modal-footer border-0 bg-white d-flex justify-content-center py-3">' +
                          '        <button type="button" class="btn btn-secondary px-5 py-2 rounded-pill fw-bold shadow-sm" data-bs-dismiss="modal"><i class="fas fa-times me-2"></i><%= Common.getBahasaConfig("Tutup") %></button>' +
                          '      </div>' +
                          '    </div>' +
                          '  </div>' +
                          '</div>';

        document.body.insertAdjacentHTML('beforeend', modalHtml);
        const modalEl = document.getElementById(modalId);
        
        const modalObj = new bootstrap.Modal(modalEl, { backdrop: 'static' });
        modalObj.show();
        
        setTimeout(() => {
            const backdrops = document.querySelectorAll('.modal-backdrop');
            if(backdrops.length > 0) backdrops[backdrops.length - 1].classList.add('va-cetak');
        }, 100);
        
        modalEl.addEventListener('hidden.bs.modal', function () { this.remove(); });
    };

    // --- 2. FUNGSI TUTUP & SEGARKAN RIWAYAT ---
    window.tutupDanSegarkanRiwayatVA<%=rnd%> = function() {
        if(typeof tampilkanToast === 'function') {
            tampilkanToast('<%=Common.getBahasaConfigJS("Menyegarkan data tagihan...")%>', 'bg-info text-white');
        }
        
        document.querySelectorAll('.modal.show').forEach(function(modalEl) {
            try {
                var mObj = bootstrap.Modal.getInstance(modalEl);
                if(mObj) mObj.hide();
            } catch(e) {}
        });

        setTimeout(function() {
            let currentUrl = new URL(window.location.href);
            currentUrl.searchParams.set('refresh', 'true');
            window.location.href = currentUrl.toString();
        }, 300);
    };

    // --- 3. FITUR SALIN TEKS (COPY TO CLIPBOARD) ---
    window.salinTeks<%=rnd%> = function(elementId) {
        var el = document.getElementById(elementId);
        if (!el) return;
        var teks = el.innerText;
        
        if (navigator.clipboard && window.isSecureContext) {
            navigator.clipboard.writeText(teks).then(function() {
                if (typeof tampilkanToast === 'function') {
                    tampilkanToast('<%=Common.getBahasaConfigJS("Kode berhasil disalin ke papan klip!")%>', 'bg-success text-white');
                } else {
                    alert('<%=Common.getBahasaConfigJS("Kode berhasil disalin!")%>');
                }
            }).catch(function() {
                window.fallbackSalinTeks<%=rnd%>(teks);
            });
        } else {
            window.fallbackSalinTeks<%=rnd%>(teks);
        }
    };

    window.fallbackSalinTeks<%=rnd%> = function(teks) {
        var textArea = document.createElement("textarea");
        textArea.value = teks;
        textArea.style.top = "0";
        textArea.style.left = "0";
        textArea.style.position = "fixed";

        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();

        try {
            var successful = document.execCommand('copy');
            if (successful) {
                if (typeof tampilkanToast === 'function') {
                    tampilkanToast('<%=Common.getBahasaConfigJS("Kode berhasil disalin ke papan klip!")%>', 'bg-success text-white');
                } else {
                    alert('<%=Common.getBahasaConfigJS("Kode berhasil disalin!")%>');
                }
            } else {
                if (typeof tampilkanToast === 'function') {
                    tampilkanToast('<%=Common.getBahasaConfigJS("Gagal menyalin kode. Sistem peramban Anda menolak akses.")%>', 'bg-danger text-white');
                }
            }
        } catch (err) {
            if (typeof tampilkanToast === 'function') {
                tampilkanToast('<%=Common.getBahasaConfigJS("Fitur salin tidak didukung oleh peramban ini.")%>', 'bg-danger text-white');
            }
        }

        document.body.removeChild(textArea);
    };
</script>