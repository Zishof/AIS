<%@page import="java.util.Calendar"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.KarirConfigUtil"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
    private String fk(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
    private String fsafeHref(String root, String href, String fallback) {
        String fb = fallback == null ? (root == null ? "" : root + "/karir") : fallback;
        if (href == null || href.trim().length() == 0) return fb;
        href = href.trim();
        String lower = href.toLowerCase();
        if (href.startsWith("#")) return (root == null ? "" : root + "/karir") + href;
        if (href.startsWith("/")) return (root == null ? "" : root) + href;
        if (lower.startsWith("http://") || lower.startsWith("https://")) return href;
        return fb;
    }
%>
<%
    String rootFooter = request.getContextPath();
    String judul = "Instansi"; String alamat = ""; String telp = ""; String email = ""; String motto = "";
    try {
        PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
        if (pt != null) {
            judul = pt.getNama() == null ? judul : pt.getNama();
            alamat = pt.getAlamat1() == null ? "" : pt.getAlamat1();
            telp = pt.getTelepon() == null ? "" : pt.getTelepon();
            email = pt.getEmail() == null ? "" : pt.getEmail();
            motto = pt.getMotto() == null ? "" : pt.getMotto();
        }
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_footer_karir.jsp:35");}
    int year = Calendar.getInstance().get(Calendar.YEAR);
    String deskripsi = KarirConfigUtil.text("informasi_pengenalan_karir", "Portal KARIR membantu pendaftaran, unggah dokumen, verifikasi, jadwal seleksi, dan pengumuman berjalan lebih rapi.");
    String footerMenuJudul = KarirConfigUtil.text("karir_footer_menu_judul", "Menu KARIR");
    String footerPanduanJudul = KarirConfigUtil.text("karir_footer_panduan_judul", "Panduan & Informasi");
    String labelLowongan = KarirConfigUtil.text("karir_menu_label_lowongan", "Info Lowongan Kerja");
    String labelPendaftaran = KarirConfigUtil.text("karir_menu_label_pendaftaran", "Pendaftaran Calon Pegawai");
    String labelSyarat = KarirConfigUtil.text("karir_menu_label_syarat", "Syarat & Ketentuan");
    String labelLogin = KarirConfigUtil.text("karir_menu_label_login_pengumuman", "Login Pengumuman");
    String labelPanduan = KarirConfigUtil.text("karir_footer_label_panduan_pendaftaran", "Panduan Pendaftaran");
    String labelPrivasi = KarirConfigUtil.text("karir_footer_label_kebijakan_privasi", "Kebijakan Privasi");
    String labelBantuan = KarirConfigUtil.text("karir_footer_label_bantuan", "Bantuan Pelamar");
    String linkFb = Common.getKonfigurasi("link_facebook", "").getNilai();
    String linkIg = Common.getKonfigurasi("link_instagram", "").getNilai();
    String linkYt = Common.getKonfigurasi("link_youtube", "").getNilai();
%>
<footer class="mt-5 pt-5 pb-4 text-white karir-footer">
    <div class="container">
        <div class="row g-4">
            <div class="col-lg-4">
                <h5 class="fw-bold text-white mb-3"><i class="fas fa-briefcase me-2 text-warning"></i><%=fk(judul)%></h5>
                <p class="text-white-50 lh-lg small"><%=fk(deskripsi)%></p>
                <% if (motto != null && motto.trim().length() > 0) { %><div class="small text-warning fst-italic">&quot;<%=fk(motto)%>&quot;</div><% } %>
            </div>
            <div class="col-lg-2 col-md-4">
                <h6 class="fw-bold text-white mb-3"><i class="fas fa-link me-2 text-warning"></i><%=fk(footerMenuJudul)%></h6>
                <ul class="list-unstyled small lh-lg karir-footer-list">
                    <% if (KarirConfigUtil.active("karir_footer_tampilkan_info_lowongan", true)) { %><li><a class="text-white-50 text-decoration-none" href="<%=rootFooter%>/karir#lowongan"><i class="fas fa-angle-right me-2"></i><%=fk(labelLowongan)%></a></li><% } %>
                    <% if (KarirConfigUtil.active("karir_footer_tampilkan_pendaftaran", true)) { %><li><a class="text-white-50 text-decoration-none" href="<%=rootFooter%>/karir#pendaftaran"><i class="fas fa-angle-right me-2"></i><%=fk(labelPendaftaran)%></a></li><% } %>
                    <% if (KarirConfigUtil.active("karir_footer_tampilkan_syarat", true)) { %><li><a class="text-white-50 text-decoration-none" href="<%=rootFooter%>/karir?halaman=syarat_ketentuan"><i class="fas fa-angle-right me-2"></i><%=fk(labelSyarat)%></a></li><% } %>
                    <% if (KarirConfigUtil.active("karir_footer_tampilkan_login_pengumuman", true)) { %><li><a class="text-white-50 text-decoration-none" href="javascript:void(0)" onclick="karirOpenLoginModal()"><i class="fas fa-angle-right me-2"></i><%=fk(labelLogin)%></a></li><% } %>
                </ul>
            </div>
            <div class="col-lg-3 col-md-4">
                <h6 class="fw-bold text-white mb-3"><i class="fas fa-book-open me-2 text-warning"></i><%=fk(footerPanduanJudul)%></h6>
                <ul class="list-unstyled small lh-lg karir-footer-list">
                    <% if (KarirConfigUtil.active("karir_footer_tampilkan_panduan_pendaftaran", true)) { %><li><a class="text-white-50 text-decoration-none" href="<%=rootFooter%>/karir?halaman=panduan_pendaftaran"><i class="fas fa-angle-right me-2"></i><%=fk(labelPanduan)%></a></li><% } %>
                    <% if (KarirConfigUtil.active("karir_footer_tampilkan_syarat", true)) { %><li><a class="text-white-50 text-decoration-none" href="<%=rootFooter%>/karir?halaman=syarat_ketentuan"><i class="fas fa-angle-right me-2"></i><%=fk(labelSyarat)%></a></li><% } %>
                    <% if (KarirConfigUtil.active("karir_footer_tampilkan_kebijakan_privasi", true)) { %><li><a class="text-white-50 text-decoration-none" href="<%=rootFooter%>/karir?halaman=kebijakan_privasi"><i class="fas fa-angle-right me-2"></i><%=fk(labelPrivasi)%></a></li><% } %>
                    <% if (KarirConfigUtil.active("karir_footer_tampilkan_bantuan", true)) { %><li><a class="text-white-50 text-decoration-none" href="<%=rootFooter%>/karir?halaman=bantuan"><i class="fas fa-angle-right me-2"></i><%=fk(labelBantuan)%></a></li><% } %>
                </ul>
            </div>
            <div class="col-lg-3 col-md-4">
                <h6 class="fw-bold text-white mb-3"><i class="fas fa-headset me-2 text-warning"></i>Layanan Bantuan</h6>
                <% if (alamat != null && alamat.trim().length() > 0) { %><p class="text-white-50 small mb-2"><i class="fas fa-map-marker-alt me-2"></i><%=fk(alamat)%></p><% } %>
                <% if (telp != null && telp.trim().length() > 0) { %><p class="text-white-50 small mb-2"><i class="fas fa-phone-alt me-2"></i><%=fk(telp)%></p><% } %>
                <% if (email != null && email.trim().length() > 0) { %><p class="text-white-50 small mb-3"><i class="far fa-envelope me-2"></i><a class="text-white-50" href="mailto:<%=fk(email)%>"><%=fk(email)%></a></p><% } %>
                <div class="d-flex gap-2">
                    <% if (linkFb != null && linkFb.trim().length() > 0) { %><a href="<%=fk(fsafeHref(rootFooter, linkFb, rootFooter + "/karir"))%>" target="_blank" class="btn btn-outline-light btn-sm rounded-circle karir-footer-social"><i class="fab fa-facebook-f"></i></a><% } %>
                    <% if (linkIg != null && linkIg.trim().length() > 0) { %><a href="<%=fk(fsafeHref(rootFooter, linkIg, rootFooter + "/karir"))%>" target="_blank" class="btn btn-outline-light btn-sm rounded-circle karir-footer-social"><i class="fab fa-instagram"></i></a><% } %>
                    <% if (linkYt != null && linkYt.trim().length() > 0) { %><a href="<%=fk(fsafeHref(rootFooter, linkYt, rootFooter + "/karir"))%>" target="_blank" class="btn btn-outline-light btn-sm rounded-circle karir-footer-social"><i class="fab fa-youtube"></i></a><% } %>
                </div>
            </div>
        </div>
        <hr class="border-secondary my-4">
        <div class="text-center text-white-50 small">&copy; <%=year%> <strong class="text-warning"><%=fk(judul)%></strong>. Seluruh Hak Cipta Dilindungi.</div>
    </div>
</footer>
<script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
