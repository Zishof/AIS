<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.JenisKegiatan"%>
<%@page import="ais.database.model.JenisPembayaran"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.action.master.helper.PembayaranGatewayKatalog"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>

<%
    // --- 1. PENGAMANAN SESI PENGGUNA ---
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.sendRedirect(Common.ROOT + "/login.jsp");
        return;
    }

    String rnd = request.getParameter("rnd") != null ? request.getParameter("rnd") : Common.getGeneratedBarCode(6);

    // --- 2. EKSTRAKSI & VALIDASI PARAMETER ---
    // Parameter digaungkan kembali ke konteks <script>/CSS di bawah, jadi WAJIB whitelist
    // ketat (anti injeksi JS/HTML lewat query string).
    if (!rnd.matches("[A-Za-z0-9]+")) rnd = Common.getGeneratedBarCode(6);
    String idStr = request.getParameter("id");
    String jkIdStr = request.getParameter("jkId");
    String isMahasiswaStr = request.getParameter("isMahasiswa");
    String smtStr = request.getParameter("smt");
    String payload = request.getParameter("payload");

    if (payload == null || payload.trim().isEmpty() || jkIdStr == null || jkIdStr.trim().isEmpty()) {
        out.print("<div class='alert alert-danger m-3 shadow-sm rounded-4'><i class='fas fa-exclamation-triangle me-2'></i>" + Common.getBahasaConfig("Data pembayaran tidak sah atau rincian muatan kosong.") + "</div>");
        return;
    }
    if (!payload.matches("[A-Za-z0-9_|.,\\-]+")
            || !jkIdStr.matches("[0-9]+")
            || (idStr != null && !idStr.matches("[0-9]*"))
            || (smtStr != null && !smtStr.matches("[0-9]*"))
            || (isMahasiswaStr != null && !isMahasiswaStr.matches("(?i)true|false|"))) {
        out.print("<div class='alert alert-danger m-3 shadow-sm rounded-4'><i class='fas fa-exclamation-triangle me-2'></i>" + Common.getBahasaConfig("Parameter pembayaran mengandung karakter tidak sah.") + "</div>");
        return;
    }

    // --- 3. KALKULASI TOTAL TAGIHAN ---
    double totalBayar = 0.0;
    String[] items = payload.split(",");
    for (String item : items) {
        String[] parts = item.split("\\|");
        if (parts.length == 2) {
            try { 
                totalBayar += Double.parseDouble(parts[1].trim());
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_lanjut_bayar.jsp:56");}
        }
    }

    if (totalBayar <= 0) {
        out.print("<div class='alert alert-warning m-3 shadow-sm rounded-4'><i class='fas fa-exclamation-circle me-2'></i>" + Common.getBahasaConfig("Total pembayaran tidak boleh bernilai nol. Silakan periksa kembali rincian tagihan Anda.") + "</div>");
        return;
    }

    // --- 4. VALIDASI AKSES & PENGATURAN SALURAN PEMBAYARAN ---
    // Aturan on/off saluran terpusat di PembayaranGatewayKatalog (single source of
    // truth yang sama dengan wizard ZK dan DaftarUlangMahasiswa*Action), sehingga
    // konfigurasi aktifkan_pembayaran_* berperilaku identik di semua tampilan.
    Session sess = null;
    JenisKegiatan jk = null;
    boolean isAdmin = (tbmuser.getMahasiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null);
    boolean showBayarTunai = false;

    List<JenisPembayaran> listCaraBayar = new ArrayList<JenisPembayaran>();
    String defaultDate = new java.text.SimpleDateFormat("yyyy-MM-dd").format(ais.ui.util.WaktuUtil.getDate());

    try {
        sess = HibernateUtil.openSession();
        jk = (JenisKegiatan) ConstantValues.ambil(JenisKegiatan.class.getName(), Long.parseLong(jkIdStr), true);

        try {
            listCaraBayar = ConstantValues.simpleList(sess.createCriteria(JenisPembayaran.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))), JenisPembayaran.class);
        } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_lanjut_bayar.jsp:83");}

        showBayarTunai = isAdmin
                && !PembayaranGatewayKatalog.adminDilarangBayarLangsung(tbmuser.getUserId())
                && PembayaranGatewayKatalog.tunaiAktif(jk);

    } catch(Exception e) {
        out.print("<div class='alert alert-danger m-3 shadow-sm rounded-4'><i class='fas fa-exclamation-triangle me-2'></i>" + Common.getBahasaConfig("Terjadi galat teknis saat memvalidasi jenis kegiatan pembayaran.") + "</div>");
        return;
    } finally {
        try { HibernateUtil.closeSessionQuietly(sess); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_lanjut_bayar.jsp:93");}
    }

    // --- 5. PERSIAPAN DATA DAFTAR METODE MANUAL ---
    boolean adaDefaultCaraBayar = false;
    for (JenisPembayaran jp : listCaraBayar) {
        try {
            if (jp.getDefaultPembayaran() != null && jp.getDefaultPembayaran()) {
                adaDefaultCaraBayar = true;
                break;
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_lanjut_bayar.jsp:104");}
    }

    StringBuilder sbCaraBayarHtml = new StringBuilder();
    for (JenisPembayaran jp : listCaraBayar) {
        boolean sel = false;
        try {
            if (adaDefaultCaraBayar) {
                sel = (jp.getDefaultPembayaran() != null && jp.getDefaultPembayaran());
            } else {
                sel = (ConstantValues.TUNAI != null && jp.getId().equals(ConstantValues.TUNAI.getId()));
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_lanjut_bayar.jsp:116");}
        
        sbCaraBayarHtml.append("<option value=\"").append(jp.getId()).append("\"")
                       .append(sel ? " selected" : "").append(">")
                       .append(jp.getNama()).append("</option>");
    }

    // --- 6. PERSIAPAN DATA GERBANG PEMBAYARAN DARING (PAYMENT GATEWAYS) ---
    /* Daftar saluran kini diambil dari PembayaranGatewayKatalog - definisi kunci
       konfigurasi, label tombol, filter jenis kegiatan, DAN cek konfigurasi per
       Perguruan Tinggi (aktifkan_pembayaran_via_*_pt_<id>, yang sebelumnya luput di
       tampilan JSP) semuanya satu sumber dengan DaftarUlangMahasiswa*Action dan
       wizard ZK. KAPABILITAS_CHECKOUT_JSP menjaga hanya gateway yang benar-benar
       diimplementasikan _lanjut_bayar_services.jsp yang tampil (riwayat bug: tombol
       BNI dsb tampil lalu diam-diam dialihkan ke host "online" sehingga VA salah bank). */
    List<PembayaranGatewayKatalog.Gateway> activeGateways =
            PembayaranGatewayKatalog.yangTampil(jk, PembayaranGatewayKatalog.KAPABILITAS_CHECKOUT_JSP);

    /* Kompatibilitas mundur Finpay: sebelum katalog, tombol Finpay checkout JSP menyala
       dari kunci lama aktifkan_pembayaran_via_finpay (kini milik gateway langsung ZK).
       Bila kunci baru aktifkan_pembayaran_via_bank_finpay belum di-set tetapi kunci lama
       aktif, tombol tetap ditampilkan agar tidak lenyap senyap pada instansi lama. */
    boolean adaBankFinpay = false;
    for (PembayaranGatewayKatalog.Gateway g : activeGateways) {
        if ("bank_finpay".equals(g.id)) { adaBankFinpay = true; break; }
    }
    if (!adaBankFinpay
            && PembayaranGatewayKatalog.tampil(PembayaranGatewayKatalog.cari("finpay"), jk)) {
        PembayaranGatewayKatalog.Gateway bf = PembayaranGatewayKatalog.cari("bank_finpay");
        if (bf != null) activeGateways.add(bf);
    }

    /* Guard VA-salah-bank: di checkout JSP tombol "BAYAR VIA BSI" dieksekusi melalui host
       seri "online" (lihat routing services). Bila host online tidak terkonfigurasi,
       sembunyikan tombolnya daripada menerbitkan VA di bank yang salah. */
    try {
        String hostOnline = Common.getKonfigurasi("online_bank_host_ip", "").getNilai();
        if (hostOnline == null || hostOnline.trim().isEmpty()) {
            for (java.util.Iterator<PembayaranGatewayKatalog.Gateway> it = activeGateways.iterator(); it.hasNext();) {
                if ("bsi".equals(it.next().id)) it.remove();
            }
        }
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_lanjut_bayar.jsp:158");}

    /* Pemetaan id katalog ke parameter "gateway" yang dikenali _lanjut_bayar_services.jsp
       (satu-satunya beda penamaan: bank_finpay memakai id legacy "finpay"). */
    Map<String, String> idKeServiceId = new HashMap<String, String>();
    for (PembayaranGatewayKatalog.Gateway g : activeGateways) {
        idKeServiceId.put(g.id, "bank_finpay".equals(g.id) ? "finpay" : g.id);
    }
%>

<style>
    /* Transisi Halus & Efek Hover Modern */
    .btn-gateway-<%=rnd%> {
        transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
        border-width: 2px;
        min-height: 108px; /* target sentuh nyaman di layar sentuh */
    }
    .btn-gateway-<%=rnd%>:hover {
        transform: translateY(-4px);
        box-shadow: 0 10px 20px rgba(0,0,0,0.08) !important;
        background-color: #f8f9fa;
    }
    .btn-tunai-<%=rnd%> {
        transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
    }
    .btn-tunai-<%=rnd%>:hover {
        transform: translateY(-4px) scale(1.02);
        box-shadow: 0 12px 24px rgba(40, 167, 69, 0.25) !important;
    }
    /* Penyesuaian layar sempit (ponsel): kartu lebih ringkas, teks tidak terpotong */
    @media (max-width: 575.98px) {
        .btn-gateway-<%=rnd%> { min-height: 92px; padding: 0.75rem 0.5rem !important; }
        .btn-gateway-<%=rnd%> i { font-size: 1.4rem !important; margin-bottom: 0.5rem !important; }
        .btn-gateway-<%=rnd%> span { font-size: 0.68rem !important; line-height: 1.25; }
        .total-tagihan-<%=rnd%> h1 { font-size: 1.9rem !important; }
        .btn-batal-<%=rnd%> { width: 100%; }
    }
</style>

<div class="container-fluid px-4 py-3">
    <div class="alert alert-info border-0 shadow-sm rounded-4 text-center p-4 mb-4 bg-opacity-10 bg-primary total-tagihan-<%=rnd%>">
        <h6 class="text-uppercase fw-bold text-primary mb-2" style="letter-spacing: 1px;"><%= Common.getBahasaConfig("Total Tagihan Terpilih") %></h6>
        <h1 class="display-5 fw-bold text-dark mb-0">Rp <%= String.format("%,.0f", totalBayar) %></h1>
    </div>

    <div class="d-flex align-items-center mb-3">
        <i class="fas fa-wallet text-success fs-5 me-2"></i>
        <h6 class="fw-bold text-dark mb-0"><%= Common.getBahasaConfig("Saluran Pembayaran Tersedia") %></h6>
    </div>
    
    <div class="row g-3 justify-content-center">
        <% if (showBayarTunai) { %>
            <div class="col-12 mb-3">
                <button class="btn btn-success w-100 py-3 rounded-4 shadow-sm fw-bold d-flex align-items-center justify-content-center btn-tunai-<%=rnd%>" onclick="window.bukaPopupFormTunai<%=rnd%>()">
                    <div class="bg-white bg-opacity-25 rounded-circle p-2 me-3 d-flex align-items-center justify-content-center" style="width: 45px; height: 45px;">
                        <i class="fas fa-cash-register fs-4 text-white"></i>
                    </div>
                    <span style="font-size: 1.15rem; letter-spacing: 0.5px;"><%= Common.getBahasaConfig("PEMBAYARAN MANUAL DI KASIR") %></span>
                </button>
            </div>
        <% } %>
        
        <% if (activeGateways.isEmpty() && !showBayarTunai) { %>
            <div class="col-12 text-center py-4">
                <div class="alert alert-warning border-0 shadow-sm rounded-4 p-4 mb-0">
                    <i class="fas fa-exclamation-circle fa-2x text-warning mb-3"></i>
                    <h6 class="fw-bold text-dark"><%= Common.getBahasaConfig("Saluran Pembayaran Tidak Tersedia") %></h6>
                    <p class="mb-0 text-secondary small"><%= Common.getBahasaConfig("Mohon maaf, saat ini belum ada saluran pembayaran daring yang aktif untuk transaksi ini. Silakan hubungi administrator sistem.") %></p>
                </div>
            </div>
        <% } else {
            for (PembayaranGatewayKatalog.Gateway gw : activeGateways) { %>
                <div class="col-6 col-md-4 col-lg-3">
                    <button class="btn btn-outline-primary w-100 p-3 rounded-4 shadow-sm fw-bold h-100 d-flex flex-column align-items-center justify-content-center bg-white btn-gateway-<%=rnd%>" onclick="window.prosesGatewayDaring<%=rnd%>('<%=idKeServiceId.get(gw.id)%>')">
                        <i class="<%=gw.ikon%> fa-2x mb-3 text-primary"></i>
                        <span style="font-size: 0.75rem;" class="text-uppercase text-dark"><%= Common.getBahasaConfig(gw.label) %></span>
                    </button>
                </div>
        <%  }
           } %>
    </div>

    <div class="d-flex justify-content-center mt-5 pt-3 border-top">
        <button type="button" class="btn btn-secondary px-5 py-2 rounded-pill fw-bold shadow-sm btn-batal-<%=rnd%>" data-bs-dismiss="modal">
            <i class="fas fa-times me-2"></i><%= Common.getBahasaConfig("Batalkan Transaksi") %>
        </button>
    </div>
</div>

<script>
    // --- 1. PROSES PEMBAYARAN TUNAI (MENAMPILKAN FORM MANUAL) ---
    window.bukaPopupFormTunai<%=rnd%> = function() {
        const modalId = 'modalTunaiManual<%=rnd%>';
        const existingModal = document.getElementById(modalId);
        
        if (existingModal) { 
            existingModal.remove(); 
            document.querySelectorAll('.modal-backdrop').forEach(b => b.remove()); 
        }

        const modalHtml = 
            '<div class="modal fade animate__animated animate__fadeIn" id="' + modalId + '" tabindex="-1" aria-hidden="true" data-bs-backdrop="static" data-bs-keyboard="false">' +
            '  <div class="modal-dialog modal-dialog-centered">' +
            '    <div class="modal-content border-0 shadow-lg rounded-4">' +
            '      <form id="formTunaiManual<%=rnd%>">' +
            '        <div class="modal-header bg-gradient bg-success text-white border-0 py-3">' +
            '          <h5 class="modal-title fw-bold"><i class="fas fa-cash-register me-2"></i><%=Common.getBahasaConfig("Rincian Transaksi Manual")%></h5>' +
            '          <button type="button" class="btn-close btn-close-white shadow-none" data-bs-dismiss="modal"></button>' +
            '        </div>' +
            '        <div class="modal-body p-4 bg-light">' +
            '          <div class="mb-3">' +
            '              <label class="form-label fw-bold text-secondary"><%=Common.getBahasaConfig("Metode Pembayaran")%></label>' +
            '              <select class="form-select rounded-3" name="caraBayar" required>' +
            '                  <%=sbCaraBayarHtml.toString()%>' +
            '              </select>' +
            '          </div>' +
            '          <div class="row">' +
            '              <div class="col-6 mb-3">' +
            '                  <label class="form-label fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal Pembayaran")%></label>' +
            '                  <input type="date" class="form-control rounded-3" name="tglBayar" value="<%=defaultDate%>" required>' +
            '              </div>' +
            '              <div class="col-6 mb-3">' +
            '                  <label class="form-label fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal Kuitansi")%></label>' +
            '                  <input type="date" class="form-control rounded-3" name="tglKwitansi" value="<%=defaultDate%>" required>' +
            '              </div>' +
            '          </div>' +
            '          <div class="mb-0">' +
            '              <label class="form-label fw-bold text-secondary"><%=Common.getBahasaConfig("Keterangan Tambahan")%></label>' +
            '              <textarea class="form-control rounded-3" name="keterangan" rows="2" placeholder="<%=Common.getBahasaConfig("Catatan opsional...")%>"></textarea>' +
            '          </div>' +
            '        </div>' +
            '        <div class="modal-footer bg-white border-top p-3 d-flex justify-content-between">' +
            '          <button type="button" class="btn btn-light rounded-pill px-4 fw-bold shadow-sm" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>' +
            '          <button type="submit" class="btn btn-success rounded-pill px-4 fw-bold shadow-sm"><i class="fas fa-check-circle me-2"></i><%=Common.getBahasaConfig("Proses Pembayaran")%></button>' +
            '        </div>' +
            '      </form>' +
            '    </div>' +
            '  </div>' +
            '</div>';
            
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        const modalEl = document.getElementById(modalId);
        const modalObj = new bootstrap.Modal(modalEl);
        modalObj.show();
        
        modalEl.addEventListener('hide.bs.modal', function() { if(document.activeElement) document.activeElement.blur(); });
        modalEl.addEventListener('hidden.bs.modal', function () { modalEl.remove(); });

        document.getElementById('formTunaiManual<%=rnd%>').addEventListener('submit', function(e) {
            e.preventDefault();
            const formData = new FormData(e.target);
            eksekusiTunaiLanjutan<%=rnd%>(formData.get('caraBayar'), formData.get('tglBayar'), formData.get('tglKwitansi'), formData.get('keterangan'));
        });
    };

    // --- 2. EKSEKUSI PENYELESAIAN TUNAI ---
    var eksekusiTunaiLanjutan<%=rnd%> = function(caraBayar, tglBayar, tglKwitansi, keterangan) {
        if (typeof tampilkanToast === 'function') {
            tampilkanToast('<%=Common.getBahasaConfigJS("Merekam rincian transaksi ke basis data...")%>', 'bg-success text-white');
        }

        // Menutup semua modal agar antarmuka tidak bertumpuk
        document.querySelectorAll('.modal.show').forEach(m => {
            const inst = bootstrap.Modal.getInstance(m);
            if (inst) inst.hide();
        });
        
        // Jeda waktu agar animasi penutupan Bootstrap selesai
        setTimeout(() => {
            const modalId = 'modalProsesTunai<%=rnd%>';
            const existingModal = document.getElementById(modalId);
            if (existingModal) { 
                existingModal.remove(); 
                document.querySelectorAll('.modal-backdrop').forEach(b => b.remove()); 
            }

            const modalHtml = 
                '<div class="modal fade animate__animated animate__fadeIn" id="' + modalId + '" tabindex="-1" aria-hidden="true" data-bs-backdrop="static" data-bs-keyboard="false">' +
                '  <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">' +
                '    <div class="modal-content border-0 shadow-lg rounded-4">' +
                '      <div class="modal-header bg-gradient bg-primary text-white border-0 py-3">' +
                '        <h5 class="modal-title fw-bold"><i class="fas fa-cash-register me-2"></i><%=Common.getBahasaConfig("Penyelesaian Transaksi Tunai")%></h5>' +
                '        <button type="button" class="btn-close btn-close-white shadow-none" data-bs-dismiss="modal"></button>' +
                '      </div>' +
                '      <div class="modal-body p-0 bg-light" id="modalBodyProsesTunai<%=rnd%>">' +
                '        <div class="text-center py-5 animate__animated animate__fadeIn">' +
                '            <div class="spinner-border text-primary" style="width: 4rem; height: 4rem; border-width: 0.35em;"></div>' +
                '            <h5 class="mt-4 fw-bold text-secondary"><%=Common.getBahasaConfig("Sedang memproses pembayaran tunai...")%></h5>' +
                '        </div>' +
                '      </div>' +
                '    </div>' +
                '  </div>' +
                '</div>';
            
            document.body.insertAdjacentHTML('beforeend', modalHtml);
            const modalEl = document.getElementById(modalId);
            const modalObj = new bootstrap.Modal(modalEl);
            modalObj.show();
            
            modalEl.addEventListener('hide.bs.modal', function() { if(document.activeElement) document.activeElement.blur(); });
            modalEl.addEventListener('hidden.bs.modal', function () { modalEl.remove(); });

            const checkoutTunaiUrl = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=bayarmhs&s=_bayar_tunai_service' + 
                                '&id=<%=idStr%>&jkId=<%=jkIdStr%>&smt=<%=smtStr%>&isMahasiswa=<%=isMahasiswaStr%>' + 
                                '&payload=' + encodeURIComponent('<%=payload%>') +
                                '&caraBayar=' + encodeURIComponent(caraBayar) +
                                '&tglBayar=' + encodeURIComponent(tglBayar) +
                                '&tglKwitansi=' + encodeURIComponent(tglKwitansi) +
                                '&keterangan=' + encodeURIComponent(keterangan);
                                
            fetch(checkoutTunaiUrl)
                .then(res => res.text())
                .then(html => {
                    const mBody = document.getElementById('modalBodyProsesTunai<%=rnd%>');
                    if(mBody) {
                        mBody.innerHTML = html;
                        mBody.querySelectorAll('script').forEach(s => {
                            const ns = document.createElement('script');
                            if (s.src) ns.src = s.src; else ns.textContent = s.textContent;
                            document.body.appendChild(ns); document.body.removeChild(ns);
                        });
                    }
                })
                .catch(err => {
                    const mBody = document.getElementById('modalBodyProsesTunai<%=rnd%>');
                    if(mBody) {
                        mBody.innerHTML = '<div class="alert alert-danger m-4 shadow-sm rounded-4"><i class="fas fa-exclamation-triangle me-2"></i> <%=Common.getBahasaConfig("Gagal memproses transaksi tunai.")%></div>';
                    }
                });
        }, 350); 
    };

    // --- 3. PROSES PEMBAYARAN DARING (VIRTUAL ACCOUNT) ---
    window.prosesGatewayDaring<%=rnd%> = function(kodeBank) {
        if (typeof showConfirmModal === 'function') {
            showConfirmModal('<%=Common.getBahasaConfigJS("Sistem akan menghubungi peladen gerbang pembayaran untuk merakit Virtual Account. Lanjutkan?")%>', function() {
                eksekusiDaring<%=rnd%>(kodeBank);
            });
        } else {
            if (confirm('<%=Common.getBahasaConfigJS("Sistem akan merakit Virtual Account. Lanjutkan?")%>')) {
                eksekusiDaring<%=rnd%>(kodeBank);
            }
        }
    };

    var eksekusiDaring<%=rnd%> = function(kodeBank) {
        if (typeof tampilkanToast === 'function') {
            tampilkanToast('<%=Common.getBahasaConfigJS("Menghubungkan ke layanan bank, mohon tunggu sebentar...")%>', 'bg-info text-white');
        }

        const modalId = 'modalProsesDaring<%=rnd%>';
        const existingModal = document.getElementById(modalId);
        if (existingModal) { 
            existingModal.remove();
            document.querySelectorAll('.modal-backdrop').forEach(b => b.remove()); 
        }

        const modalHtml = 
            '<div class="modal fade animate__animated animate__fadeIn" id="' + modalId + '" tabindex="-1" aria-hidden="true" data-bs-backdrop="static" data-bs-keyboard="false">' +
            '  <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">' +
            '    <div class="modal-content border-0 shadow-lg rounded-4">' +
            '      <div class="modal-header bg-gradient bg-primary text-white border-0 py-3">' +
            '        <h5 class="modal-title fw-bold"><i class="fas fa-shopping-cart me-2"></i><%=Common.getBahasaConfig("Penyelesaian Transaksi Pembayaran")%></h5>' +
            '        <button type="button" class="btn-close btn-close-white shadow-none" data-bs-dismiss="modal"></button>' +
            '      </div>' +
            '      <div class="modal-body p-0 bg-light" id="modalBodyProsesDaring<%=rnd%>">' +
            '        <div class="text-center py-5 animate__animated animate__fadeIn">' +
            '            <div class="spinner-border text-primary" style="width: 4rem; height: 4rem; border-width: 0.35em;"></div>' +
            '            <h5 class="mt-4 fw-bold text-secondary"><%=Common.getBahasaConfig("Sedang memproses saluran pembayaran...")%></h5>' +
            '        </div>' +
            '      </div>' +
            '    </div>' +
            '  </div>' +
            '</div>';
        
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        const modalEl = document.getElementById(modalId);
        const modalObj = new bootstrap.Modal(modalEl);
        modalObj.show();
        
        modalEl.addEventListener('hide.bs.modal', function() { if(document.activeElement) document.activeElement.blur(); });
        modalEl.addEventListener('hidden.bs.modal', function () { modalEl.remove(); });

        const payloadData = '<%=payload%>';
        
        // PERBAIKAN: Menambahkan &rnd=<%=rnd%> untuk menjaga konsistensi state UI saat me-render form dinamis Bankaltimtara
        const checkoutDaringUrl = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=bayarmhs&s=_lanjut_bayar_services' + 
                            '&gateway=' + kodeBank + 
                            '&rnd=<%=rnd%>' + 
                            '&id=<%=idStr%>&jkId=<%=jkIdStr%>&smt=<%=smtStr%>&isMahasiswa=<%=isMahasiswaStr%>' + 
                            '&payload=' + encodeURIComponent(payloadData);
                            
        fetch(checkoutDaringUrl)
            .then(res => res.text())
            .then(html => {
                const mBody = document.getElementById('modalBodyProsesDaring<%=rnd%>');
                if(mBody) {
                    mBody.innerHTML = html;
                    mBody.querySelectorAll('script').forEach(s => {
                        const ns = document.createElement('script');
                        if (s.src) ns.src = s.src; else ns.textContent = s.textContent;
                        document.body.appendChild(ns); document.body.removeChild(ns);
                    });
                }
            })
            .catch(err => {
                const mBody = document.getElementById('modalBodyProsesDaring<%=rnd%>');
                if(mBody) {
                    mBody.innerHTML = '<div class="alert alert-danger m-4 shadow-sm rounded-4"><i class="fas fa-exclamation-triangle me-2"></i> <%=Common.getBahasaConfig("Gagal memproses saluran pembayaran.")%></div>';
                }
            });
    };
</script>