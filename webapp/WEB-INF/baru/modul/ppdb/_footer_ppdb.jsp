<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    String rnd = Common.getGeneratedBarCode(7);
    // Asumsi utilitas master menggunakan objek yang sama (PerguruanTinggiUtil dapat mengembalikan data master Sekolah)
    String judulNamaSekolah = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama();
    String Telepon = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getTelepon();
    String Motto = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getMotto();
    String Alamat1 = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getAlamat1();
    String Email = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getEmail();
%>

<style>
    .footer-ppdb-custom-<%=rnd%> { background: linear-gradient(135deg, #0a2540 0%, #173b5e 100%); color: white; border-top: 5px solid #ffc107; position: relative; z-index: 10; margin-top: 4rem; }
    .footer-ppdb-custom-<%=rnd%> h5 { letter-spacing: 1px; }
    .social-icon-<%=rnd%> { width: 38px; height: 38px; display: inline-flex; align-items: center; justify-content: center; border-radius: 50%; background: rgba(255,255,255,0.1); color: white; transition: all 0.3s ease; margin-right: 0.5rem; text-decoration: none; }
    .social-icon-<%=rnd%>:hover { background: #0d6efd; transform: translateY(-4px); box-shadow: 0 4px 10px rgba(0,0,0,0.3); color: white; }
    .hover-text-warning-<%=rnd%>:hover { color: #ffc107 !important; padding-left: 5px; transition: all 0.3s; }
</style>

<footer class="footer-ppdb-custom-<%=rnd%> pt-5 pb-3">
    <div class="container text-center text-md-start">
        <div class="row text-center text-md-start">
            
            <div class="col-md-5 col-lg-5 col-xl-5 mx-auto mb-4">
                <h5 class="text-uppercase fw-bold text-warning mb-4">
                    <i class="fas fa-school me-2"></i><%= judulNamaSekolah != null ? judulNamaSekolah : "" %>
                </h5>
                <p class="text-white-50 small pe-md-4 lh-lg">
                    <%= Common.getBahasaConfig("Pusat layanan Penerimaan Peserta Didik Baru (PPDB). Kami berkomitmen penuh untuk memberikan pendidikan terbaik guna mencetak generasi penerus bangsa yang unggul, inovatif, dan berintegritas tinggi.") %>
                </p>
                <% if (Motto != null && !Motto.trim().isEmpty()) { %>
                    <p class="text-info fst-italic fw-bold mt-2 mb-0">"<%= Motto %>"</p>
                <% } %>
            </div>

            <div class="col-md-3 col-lg-3 col-xl-3 mx-auto mb-4">
                <h5 class="text-uppercase fw-bold text-warning mb-4"><%= Common.getBahasaConfig("Pusat Bantuan") %></h5>
                <p class="mb-3"><a href="javascript:void(0)" class="text-white-50 text-decoration-none small hover-text-warning-<%=rnd%>"><i class="fas fa-angle-right me-2"></i><%= Common.getBahasaConfig("Panduan Pendaftaran Lengkap") %></a></p>
                <p class="mb-3"><a href="javascript:void(0)" class="text-white-50 text-decoration-none small hover-text-warning-<%=rnd%>"><i class="fas fa-angle-right me-2"></i><%= Common.getBahasaConfig("Persyaratan Kelengkapan Berkas") %></a></p>
                <p class="mb-3"><a href="javascript:void(0)" class="text-white-50 text-decoration-none small hover-text-warning-<%=rnd%>"><i class="fas fa-angle-right me-2"></i><%= Common.getBahasaConfig("Informasi Biaya Pendidikan") %></a></p>
                <p class="mb-2"><a href="javascript:void(0)" class="text-white-50 text-decoration-none small hover-text-warning-<%=rnd%>"><i class="fas fa-angle-right me-2"></i><%= Common.getBahasaConfig("Tanya Jawab (FAQ)") %></a></p>
            </div>

            <div class="col-md-4 col-lg-3 col-xl-3 mx-auto mb-4">
                <h5 class="text-uppercase fw-bold text-warning mb-4"><%= Common.getBahasaConfig("Hubungi Kami") %></h5>
                <p class="text-white-50 small mb-3"><i class="fas fa-map-marker-alt me-3 text-white"></i><%= Alamat1 != null ? Alamat1 : "-" %></p>
                <p class="text-white-50 small mb-3"><i class="fas fa-envelope me-3 text-white"></i><%= Email != null ? Email : "-" %></p>
                <p class="text-white-50 small mb-4"><i class="fas fa-phone-alt me-3 text-white"></i><%= Telepon != null ? Telepon : "-" %></p>
                
                <div class="mt-4">
                    <a href="javascript:void(0)" class="social-icon-<%=rnd%>"><i class="fab fa-facebook-f"></i></a>
                    <a href="javascript:void(0)" class="social-icon-<%=rnd%>"><i class="fab fa-twitter"></i></a>
                    <a href="javascript:void(0)" class="social-icon-<%=rnd%>"><i class="fab fa-instagram"></i></a>
                    <a href="javascript:void(0)" class="social-icon-<%=rnd%>"><i class="fab fa-youtube"></i></a>
                </div>
            </div>
        </div>

        <hr class="mb-4 mt-3" style="border-color: rgba(255,255,255,0.15);">
        
        <div class="row align-items-center">
            <div class="col-md-12 col-lg-12 text-center">
                <p class="text-white-50 small mb-0">
                    &copy; <%= java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) %> <strong class="text-warning"><%= judulNamaSekolah != null ? judulNamaSekolah : "" %></strong>. <%= Common.getBahasaConfig("Hak Cipta Dilindungi Undang-Undang.") %>
                </p>
            </div>
        </div>
    </div>
</footer>