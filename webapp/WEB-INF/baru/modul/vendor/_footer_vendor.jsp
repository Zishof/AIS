<%@page import="java.util.Calendar"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
    private String vf(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
    private String vcfg(String key, String def) {
        try { return Common.getKonfigurasi(key, def).getNilai(); } catch (Exception e) { return def; }
    }
    private boolean vaktif(String key, String def) {
        String v = vcfg(key, def);
        return "Aktif".equalsIgnoreCase(v) || "Ya".equalsIgnoreCase(v) || "true".equalsIgnoreCase(v);
    }
%>
<%
    String judul = ""; String alamat = ""; String telp = ""; String email = ""; String motto = "";
    try {
        PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
        if (pt != null) { judul = pt.getNama() == null ? "" : pt.getNama(); alamat = pt.getAlamat1() == null ? "" : pt.getAlamat1(); telp = pt.getTelepon() == null ? "" : pt.getTelepon(); email = pt.getEmail() == null ? "" : pt.getEmail(); motto = pt.getMotto() == null ? "" : pt.getMotto(); }
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/_footer_vendor.jsp:24");}
    int year = Calendar.getInstance().get(Calendar.YEAR);
    String rootVendor = Common.ROOT + "/vendor";
    String deskripsi = vcfg("informasi_pengenalan_vendor", "Pusat Manajemen Pengadaan dan Layanan Rekanan membantu vendor mendaftar, melengkapi dokumen, dan mengikuti informasi pengadaan secara lebih tertib.");
    String footerMenuTitle = vcfg("vendor_footer_menu_title", "Menu Vendor");
    String footerHelpTitle = vcfg("vendor_footer_help_title", "Layanan Bantuan");
    String labelPendaftaran = vcfg("vendor_footer_label_pendaftaran", "Pendaftaran Vendor");
    String labelSyarat = vcfg("vendor_footer_label_syarat", "Syarat & Ketentuan");
    String labelPengumuman = vcfg("vendor_footer_label_pengumuman", "Pengumuman Proyek");
    String labelKontak = vcfg("vendor_footer_label_kontak", "Kontak");
    String labelPanduan = vcfg("vendor_footer_label_panduan_pendaftaran", "Panduan Pendaftaran");
    String labelPrivasi = vcfg("vendor_footer_label_kebijakan_privasi", "Kebijakan Privasi");
    String labelBantuan = vcfg("vendor_footer_label_bantuan", "Bantuan Vendor");
    String linkFb = vcfg("link_facebook", "");
    String linkIg = vcfg("link_instagram", "");
    String linkYt = vcfg("link_youtube", "");
%>
<footer class="mt-5 pt-5 pb-4 text-white vendor-footer" style="background:linear-gradient(135deg,#0f172a,#1e293b);">
    <div class="container">
        <div class="row g-4">
            <div class="col-lg-5">
                <h5 class="fw-bold text-white mb-3"><i class="fas fa-university me-2 text-warning"></i><%=vf(judul)%></h5>
                <p class="text-white-50 lh-lg small"><%=vf(deskripsi)%></p>
                <% if (motto != null && !motto.trim().isEmpty()) { %><div class="small text-warning fst-italic">"<%=vf(motto)%>"</div><% } %>
            </div>
            <div class="col-lg-3">
                <h6 class="fw-bold text-white mb-3"><i class="fas fa-link me-2 text-warning"></i><%=vf(footerMenuTitle)%></h6>
                <ul class="list-unstyled small lh-lg">
                    <% if (vaktif("vendor_footer_tampilkan_pendaftaran", "Aktif")) { %><li><a class="text-white-50 text-decoration-none" href="<%=rootVendor%>#pendaftaran"><i class="fas fa-angle-right me-2"></i><%=vf(labelPendaftaran)%></a></li><% } %>
                    <% if (vaktif("vendor_footer_tampilkan_syarat", "Aktif")) { %><li><a class="text-white-50 text-decoration-none" href="<%=rootVendor%>?footer=syarat_ketentuan"><i class="fas fa-angle-right me-2"></i><%=vf(labelSyarat)%></a></li><% } %>
                    <% if (vaktif("vendor_footer_tampilkan_pengumuman", "Aktif")) { %><li><a class="text-white-50 text-decoration-none" href="<%=rootVendor%>#pengumuman"><i class="fas fa-angle-right me-2"></i><%=vf(labelPengumuman)%></a></li><% } %>
                    <% if (vaktif("vendor_footer_tampilkan_kontak", "Aktif")) { %><li><a class="text-white-50 text-decoration-none" href="<%=rootVendor%>#kontak"><i class="fas fa-angle-right me-2"></i><%=vf(labelKontak)%></a></li><% } %>
                </ul>
            </div>
            <div class="col-lg-4">
                <h6 class="fw-bold text-white mb-3"><i class="fas fa-headset me-2 text-warning"></i><%=vf(footerHelpTitle)%></h6>
                <ul class="list-unstyled small lh-lg mb-3">
                    <% if (vaktif("vendor_footer_tampilkan_panduan_pendaftaran", "Aktif")) { %><li><a class="text-white-50 text-decoration-none" href="<%=rootVendor%>?footer=panduan_pendaftaran"><i class="far fa-file-lines me-2"></i><%=vf(labelPanduan)%></a></li><% } %>
                    <% if (vaktif("vendor_footer_tampilkan_kebijakan_privasi", "Aktif")) { %><li><a class="text-white-50 text-decoration-none" href="<%=rootVendor%>?footer=kebijakan_privasi"><i class="fas fa-shield-halved me-2"></i><%=vf(labelPrivasi)%></a></li><% } %>
                    <% if (vaktif("vendor_footer_tampilkan_bantuan", "Aktif")) { %><li><a class="text-white-50 text-decoration-none" href="<%=rootVendor%>?footer=bantuan_vendor"><i class="far fa-circle-question me-2"></i><%=vf(labelBantuan)%></a></li><% } %>
                </ul>
                <% if (alamat != null && alamat.trim().length() > 0) { %><p class="text-white-50 small mb-2"><i class="fas fa-map-marker-alt me-2"></i><%=vf(alamat)%></p><% } %>
                <% if (telp != null && telp.trim().length() > 0) { %><p class="text-white-50 small mb-2"><i class="fas fa-phone-alt me-2"></i><%=vf(telp)%></p><% } %>
                <% if (email != null && email.trim().length() > 0) { %><p class="text-white-50 small mb-3"><i class="far fa-envelope me-2"></i><a class="text-white-50" href="mailto:<%=vf(email)%>"><%=vf(email)%></a></p><% } %>
                <div class="d-flex gap-2">
                    <% if (linkFb != null && !linkFb.trim().isEmpty()) { %><a href="<%=vf(linkFb)%>" target="_blank" class="btn btn-outline-light btn-sm rounded-circle"><i class="fab fa-facebook-f"></i></a><% } %>
                    <% if (linkIg != null && !linkIg.trim().isEmpty()) { %><a href="<%=vf(linkIg)%>" target="_blank" class="btn btn-outline-light btn-sm rounded-circle"><i class="fab fa-instagram"></i></a><% } %>
                    <% if (linkYt != null && !linkYt.trim().isEmpty()) { %><a href="<%=vf(linkYt)%>" target="_blank" class="btn btn-outline-light btn-sm rounded-circle"><i class="fab fa-youtube"></i></a><% } %>
                </div>
            </div>
        </div>
        <hr class="border-secondary my-4">
        <div class="text-center text-white-50 small">&copy; <%=year%> <strong class="text-warning"><%=vf(judul)%></strong>. Seluruh Hak Cipta Dilindungi.</div>
    </div>
</footer>
<script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
