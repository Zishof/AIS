<%@page import="ais.common.Common"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // =========================================================================================
    // 1. INISIALISASI VARIABEL & PENGAMBILAN DATA INSTITUSI
    // =========================================================================================
    String rnd = request.getParameter("rnd");
    if (rnd == null) rnd = Common.getGeneratedBarCode(7);
    String judulNamaPT = Common.getBahasaConfig("Institusi Pendidikan");
    String telepon = "-"; 
    String alamat = "-"; 
    String email = "-"; 
    String motto = "";
    
    String linkFb = ""; 
    String linkTw = ""; 
    String linkIg = ""; 
    String linkYt = "";
    int tahunSekarang = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);

    try {
        PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
        if (pt != null) {
            if (pt.getNama() != null) judulNamaPT = pt.getNama(); 
            if (pt.getTelepon() != null) telepon = pt.getTelepon(); 
            if (pt.getAlamat1() != null) alamat = pt.getAlamat1(); 
            if (pt.getEmail() != null) email = pt.getEmail(); 
            if (pt.getMotto() != null) motto = pt.getMotto();
        }
        
        Konfigurasi konf;
        if ((konf = Common.getKonfigurasi("link_facebook", "")) != null) linkFb = konf.getNilai();
        if ((konf = Common.getKonfigurasi("link_twitter", "")) != null) linkTw = konf.getNilai();
        if ((konf = Common.getKonfigurasi("link_instagram", "")) != null) linkIg = konf.getNilai();
        if ((konf = Common.getKonfigurasi("link_youtube", "")) != null) linkYt = konf.getNilai();
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pustaka/_footer_perpustakaan.jsp:40");
        // Abaikan error agar fallback beroperasi
    }
%>

<footer class="footer-perpus pt-5 pb-3">
    <div class="container text-center text-md-start">
        <div class="row g-4">
            
            <div class="col-md-5 col-lg-5 mx-auto mb-4">
                <h5 class="text-uppercase footer-title mb-3">
                    <i class="fas fa-university me-2"></i><%= judulNamaPT %>
                </h5>
                <p class="footer-text small pe-md-4" style="line-height: 1.6;">
                    <%= Common.getBahasaConfig("Pusat Layanan Informasi dan Perpustakaan Digital. Berkomitmen tinggi dalam menyediakan sumber referensi akademik terbaik untuk mendukung kegiatan Tridharma Perguruan Tinggi.") %>
                </p>
                <% if (motto != null && !motto.trim().isEmpty()) { %>
                    <div class="footer-motto-box p-3 mt-3 rounded-3 shadow-sm">
                        <p class="footer-motto fst-italic fw-medium mb-0">"<%= motto %>"</p>
                    </div>
                <% } %>
            </div>

            <div class="col-md-3 col-lg-3 mx-auto mb-4">
                <h5 class="text-uppercase footer-title mb-3"><%= Common.getBahasaConfig("Bantuan & Referensi") %></h5>
                <div class="d-flex flex-column gap-2">
                    <a href="#" onclick="panggilMenu<%=rnd%>('panduan')" class="footer-link small transition-all">
                        <i class="fas fa-book me-2"></i><%= Common.getBahasaConfig("Panduan Peminjaman") %>
                    </a>
                    <a href="#" onclick="panggilMenu<%=rnd%>('faq')" class="footer-link small transition-all">
                        <i class="fas fa-question-circle me-2"></i><%= Common.getBahasaConfig("Tanya Jawab (FAQ)") %>
                    </a>
                    <a href="#" onclick="panggilMenu<%=rnd%>('waktu_layanan_perpus')" class="footer-link small transition-all">
                        <i class="fas fa-clock me-2"></i><%= Common.getBahasaConfig("Jam Operasional") %>
                    </a>
                </div>
            </div>

            <div class="col-md-4 col-lg-4 mx-auto mb-4">
                <h5 class="text-uppercase footer-title mb-3"><%= Common.getBahasaConfig("Hubungi Kami") %></h5>
                <p class="footer-text small mb-2"><i class="fas fa-map-marker-alt me-2"></i> <%= alamat %></p>
                <p class="footer-text small mb-2"><i class="fas fa-envelope me-2"></i> <%= email %></p>
                <p class="footer-text small mb-4"><i class="fas fa-phone-alt me-2"></i> <%= telepon %></p>
                
                <div class="d-flex gap-2 justify-content-center justify-content-md-start mt-3">
                    <a href="#" onclick="bukaTautanSosmed<%=rnd%>(event, '<%= linkFb %>', 'Facebook')" class="social-icon shadow-sm" title="Facebook"><i class="fab fa-facebook-f"></i></a>
                    <a href="#" onclick="bukaTautanSosmed<%=rnd%>(event, '<%= linkTw %>', 'Twitter')" class="social-icon shadow-sm" title="Twitter"><i class="fab fa-twitter"></i></a>
                    <a href="#" onclick="bukaTautanSosmed<%=rnd%>(event, '<%= linkIg %>', 'Instagram')" class="social-icon shadow-sm" title="Instagram"><i class="fab fa-instagram"></i></a>
                    <a href="#" onclick="bukaTautanSosmed<%=rnd%>(event, '<%= linkYt %>', 'Youtube')" class="social-icon shadow-sm" title="Youtube"><i class="fab fa-youtube"></i></a>
                </div>
            </div>
        </div>

        <hr class="mb-4 mt-2" style="border-color: rgba(255,255,255,0.2);">
        
        <div class="text-center pb-2">
            <p class="footer-text small mb-0">
                &copy; <%= tahunSekarang %> <strong><%= judulNamaPT %></strong>. <%= Common.getBahasaConfig("Hak Cipta Dilindungi Oleh Undang-Undang.") %>
            </p>
        </div>
    </div>
</footer>

<script>
    /**
     * Memvalidasi dan membuka tautan Media Sosial di tab baru
     */
    window.bukaTautanSosmed<%=rnd%> = function(event, url, platform) {
        event.preventDefault();
        if (!url || url.trim() === '' || url === '#') {
            var msg = '<%= Common.getBahasaConfigJS("Tautan untuk") %> ' + platform + ' <%= Common.getBahasaConfig("belum dikonfigurasi oleh administrator.") %>';
            if (typeof window.tampilkanToast === 'function') {
                window.tampilkanToast(msg, 'bg-warning text-dark fw-bold');
            } else {
                alert(msg);
            }
        } else {
            window.open(url, '_blank');
        }
    };
</script>