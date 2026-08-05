<%@page import="ais.common.Common"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // 1. Pengambilan data Institusi secara aman [cite: 604]
    PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
    String rnd = Common.getGeneratedBarCode(7);
    
    // Identitas Institusi
    String judulNamaPerguruanTinggi = (pt != null && pt.getNama() != null) ? pt.getNama() : Common.getBahasaConfig("Institusi Pendidikan");
    String Telepon = (pt != null && pt.getTelepon() != null) ? pt.getTelepon() : "-";
    String Alamat1 = (pt != null && pt.getAlamat1() != null) ? pt.getAlamat1() : "-";
    String Email = (pt != null && pt.getEmail() != null) ? pt.getEmail() : "-";
    
    // LOGIKA MOTTO SESUAI INSTRUKSI
    String Motto = (pt != null && pt.getMotto() != null) ? pt.getMotto() : "-";
    
    int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);

    // 2. Pengambilan Konfigurasi Deskripsi Dinamis
    // Default teks diatur lebih formal dan detail [cite: 611]
    String deskripsiPmb = Common.getKonfigurasi("informasi_pengenalan_pmb", 
        "Pusat Layanan Penerimaan Mahasiswa Baru (PMB) berkomitmen penuh untuk memberikan pendidikan terbaik guna mencetak generasi penerus bangsa yang unggul, inovatif, dan berintegritas tinggi.").getNilai();

    // 3. Tautan Media Sosial [cite: 617-618]
    String linkFb = Common.getKonfigurasi("link_facebook", "").getNilai();
    String linkTw = Common.getKonfigurasi("link_twitter", "").getNilai();
    String linkIg = Common.getKonfigurasi("link_instagram", "").getNilai();
    String linkYt = Common.getKonfigurasi("link_youtube", "").getNilai();
%>

<footer class="footer-pmb-custom pt-5 pb-3 transition-base bg-dark text-white">
    <div class="container text-center text-md-start">
        <div class="row">
            
            <div class="col-md-5 col-lg-5 col-xl-5 mx-auto mb-4">
                <h5 class="text-uppercase fw-bold text-theme mb-3">
                    <i class="fas fa-building-columns me-2 text-primary"></i><%= judulNamaPerguruanTinggi %>
                </h5>
                
                <% if (Motto != null && !Motto.trim().equals("-") && !Motto.trim().isEmpty()) { %>
                    <p class="text-info fst-italic fw-semibold mb-3 small">
                        "<%= Motto %>"
                    </p>
                <% } %>

                <p class="text-white-50 small pe-md-4 lh-lg" style="text-align: justify;">
                    <%= deskripsiPmb %>
                </p>
                
                <div class="mt-4 d-flex justify-content-center justify-content-md-start gap-2">
                    <a href="#" onclick="window.bukaTautanSosmed<%=rnd%>(event, '<%= linkFb %>', 'Facebook');" class="btn btn-outline-light rounded-circle social-icon-btn" title="Facebook">
                        <i class="fab fa-facebook-f"></i>
                    </a>
                    <a href="#" onclick="window.bukaTautanSosmed<%=rnd%>(event, '<%= linkTw %>', 'Twitter / X');" class="btn btn-outline-light rounded-circle social-icon-btn" title="Twitter / X">
                        <i class="fab fa-twitter"></i>
                    </a>
                    <a href="#" onclick="window.bukaTautanSosmed<%=rnd%>(event, '<%= linkIg %>', 'Instagram');" class="btn btn-outline-light rounded-circle social-icon-btn" title="Instagram">
                        <i class="fab fa-instagram"></i>
                    </a>
                    <a href="#" onclick="window.bukaTautanSosmed<%=rnd%>(event, '<%= linkYt %>', 'YouTube');" class="btn btn-outline-light rounded-circle social-icon-btn" title="YouTube">
                        <i class="fab fa-youtube"></i>
                    </a>
                </div>
            </div>

            <div class="col-md-3 col-lg-3 col-xl-3 mx-auto mb-4">
                <h5 class="text-uppercase fw-bold text-theme mb-4">
                    <i class="fas fa-circle-info me-2 text-primary"></i><%= Common.getBahasaConfig("Pusat Informasi") %>
                </h5>
                <ul class="list-unstyled">
                    <li class="mb-3">
                        <a href="#" onclick="window.bukaModalInformasiPMB<%=rnd%>('<%= Common.getBahasaConfigJS("Panduan Pendaftaran Lengkap") %>', 'fas fa-book', 'panduan'); return false;" class="text-white-50 text-decoration-none small hover-text-theme transition-base">
                            <i class="fas fa-angle-right me-2 opacity-50"></i><%= Common.getBahasaConfig("Panduan Pendaftaran Lengkap") %>
                        </a>
                    </li>
                    <li class="mb-3">
                        <a href="#" onclick="window.bukaModalInformasiPMB<%=rnd%>('<%= Common.getBahasaConfigJS("Persyaratan Kelengkapan Berkas") %>', 'fas fa-file-contract', 'berkas'); return false;" class="text-white-50 text-decoration-none small hover-text-theme transition-base">
                            <i class="fas fa-angle-right me-2 opacity-50"></i><%= Common.getBahasaConfig("Persyaratan Kelengkapan Berkas") %>
                        </a>
                    </li>
                    <li class="mb-3">
                        <a href="#" onclick="window.bukaModalInformasiPMB<%=rnd%>('<%= Common.getBahasaConfigJS("Informasi Biaya Pendidikan") %>', 'fas fa-wallet', 'biaya'); return false;" class="text-white-50 text-decoration-none small hover-text-theme transition-base">
                            <i class="fas fa-angle-right me-2 opacity-50"></i><%= Common.getBahasaConfig("Informasi Biaya Pendidikan") %>
                        </a>
                    </li>
                    <li class="mb-2">
                        <a href="#" onclick="window.bukaModalInformasiPMB<%=rnd%>('<%= Common.getBahasaConfigJS("Pertanyaan yang Sering Diajukan (FAQ)") %>', 'fas fa-circle-question', 'faq'); return false;" class="text-white-50 text-decoration-none small hover-text-theme transition-base">
                            <i class="fas fa-angle-right me-2 opacity-50"></i><%= Common.getBahasaConfig("Tanya Jawab (FAQ)") %>
                        </a>
                    </li>
                </ul>
            </div>

            <div class="col-md-4 col-lg-4 col-xl-3 mx-auto mb-4">
                <h5 class="text-uppercase fw-bold text-theme mb-4">
                    <i class="fas fa-headset me-2 text-primary"></i><%= Common.getBahasaConfig("Layanan Bantuan") %>
                </h5>
                <p class="text-white-50 small mb-3 lh-base">
                    <i class="fas fa-location-dot me-3 text-white opacity-75"></i><%= Alamat1 %>
                </p>
                <p class="text-white-50 small mb-3">
                    <i class="fas fa-envelope me-3 text-white opacity-75"></i>
                    <a href="mailto:<%= Email %>" class="text-white-50 text-decoration-none hover-text-theme"><%= Email %></a>
                </p>
                <p class="text-white-50 small mb-4">
                    <i class="fas fa-phone me-3 text-white opacity-75"></i><%= Telepon %>
                </p>
            </div>
        </div>

        <hr class="mb-4 mt-3 border-secondary opacity-25">
        
        <div class="row align-items-center">
            <div class="col-md-12 text-center">
                <p class="text-white-50 small mb-0">
                    &copy; <%= currentYear %> <strong class="text-theme"><%= judulNamaPerguruanTinggi %></strong>.
                    <%= Common.getBahasaConfig("Hak Cipta Dilindungi Undang-Undang.") %>
                </p>
            </div>
        </div>
    </div>
</footer>

<script>
    // FUNGSI UNTUK MEMBUKA MODAL PUSAT BANTUAN SECARA DINAMIS
    window.bukaModalInformasiPMB<%=rnd%> = function(judul, iconClass, jenisKonten) {
        const modalId = 'modalInfoPMB<%=rnd%>';
        if (document.getElementById(modalId)) document.getElementById(modalId).remove();

        const url = '<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_alur_pendaftaran&jenis=' + jenisKonten;
        const modalHtml = 
            '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true" style="z-index: 1060;">' +
                '<div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">' +
                    '<div class="modal-content rounded-4 border-0 shadow-lg">' +
                        '<div class="modal-header bg-primary text-white py-3">' +
                            '<h5 class="modal-title fw-bold"><i class="' + iconClass + ' me-2"></i>' + judul + '</h5>' +
                            '<button type="button" class="btn-close btn-close-white shadow-none" data-bs-dismiss="modal"></button>' +
                        '</div>' +
                        '<div class="modal-body p-0 bg-light position-relative" style="height: 85vh;">' +
                            '<div class="position-absolute top-50 start-50 translate-middle text-center" id="loader' + modalId + '">' +
                                '<div class="spinner-border text-primary" style="width: 3rem; height: 3rem;" role="status"></div>' +
                                '<div class="mt-3 text-muted small fw-bold"><%= Common.getBahasaConfig("Memuat Dokumen...") %></div>' +
                            '</div>' +
                            '<iframe src="' + url + '" class="iframe-doc-pmb" ' +
                                    'onload="document.getElementById(\'loader' + modalId + '\').style.display=\'none\'; this.style.opacity=\'1\';"></iframe>' +
                        '</div>' +
                        '<div class="modal-footer bg-light py-2 border-top-0">' +
                            '<button type="button" class="btn btn-secondary px-4 rounded-pill fw-bold shadow-sm" data-bs-dismiss="modal"><i class="fas fa-xmark me-2"></i><%= Common.getBahasaConfig("Tutup") %></button>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
            
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        new bootstrap.Modal(document.getElementById(modalId)).show();
    };

    // FUNGSI UNTUK MEMBUKA TAUTAN MEDIA SOSIAL DENGAN VALIDASI TOAST
    window.bukaTautanSosmed<%=rnd%> = function(event, url, platformName) {
        event.preventDefault();
        if (!url || url.trim() === '' || url === '#') {
            const pesan = platformName + ' <%= Common.getBahasaConfigJS("belum ditentukan oleh administrator institusi.") %>';
            if (typeof window.tampilkanToast === 'function') {
                window.tampilkanToast(pesan, 'bg-warning text-dark fw-bold');
            } else {
                alert(pesan);
            }
        } else {
            window.open(url, '_blank', 'noopener,noreferrer');
        }
    };
</script><script>
(function() {
    if (!window.bootstrap) {
        var script = document.createElement('script');
        script.src = 'https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js';
        script.defer = true;
        document.body.appendChild(script);
    }
})();
</script>
