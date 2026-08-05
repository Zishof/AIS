<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    String judulNamaPerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama();
    String Telepon = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getTelepon();
    String Motto = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getMotto();
    String Alamat1 = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getAlamat1();
    String Email = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getEmail();
%>

<!-- Kaki Halaman (Footer) Alumni -->
<footer class="pt-5 pb-3 shadow-lg mt-5 d-print-none footer-alumni-bg">
    <div class="container text-center text-md-start">
        <div class="row text-center text-md-start">
            <div class="col-md-5 col-lg-5 col-xl-5 mx-auto mb-4">
                <h5 class="text-uppercase fw-bold footer-alumni-highlight mb-3"><i class="fas fa-university me-2"></i><%= judulNamaPerguruanTinggi != null ? judulNamaPerguruanTinggi : "" %></h5>
                <p class="text-white-50 small pe-md-4 lh-lg"><%= Common.getBahasaConfig("Pusat layanan Alumni dan Tracer Study. Berkomitmen untuk terus menjalin silaturahmi, memfasilitasi pengembangan karir, dan mencatat rekam jejak kesuksesan para lulusan demi kemajuan bersama.") %></p>
                <% if (Motto != null && !Motto.trim().isEmpty()) { %><p class="footer-alumni-motto fst-italic mt-3 mb-0 border-start border-3 ps-3">"<%= Motto %>"</p><% } %>
            </div>
            <div class="col-md-3 col-lg-3 col-xl-3 mx-auto mb-4">
                <h5 class="text-uppercase fw-bold footer-alumni-highlight mb-3"><%= Common.getBahasaConfig("Tautan Navigasi") %></h5>
                <div class="d-flex flex-column gap-2">
                    <a href="#" class="footer-alumni-link"><i class="fas fa-angle-right me-2 footer-alumni-highlight"></i><%= Common.getBahasaConfig("Panduan Pengisian Kuesioner") %></a>
                    <a href="#" class="footer-alumni-link"><i class="fas fa-angle-right me-2 footer-alumni-highlight"></i><%= Common.getBahasaConfig("Layanan Pusat Karir") %></a>
                    <a href="#" class="footer-alumni-link"><i class="fas fa-angle-right me-2 footer-alumni-highlight"></i><%= Common.getBahasaConfig("Prosedur Legalisir Ijazah") %></a>
                </div>
            </div>
            <div class="col-md-4 col-lg-3 col-xl-3 mx-auto mb-4">
                <h5 class="text-uppercase fw-bold footer-alumni-highlight mb-3"><%= Common.getBahasaConfig("Pusat Bantuan") %></h5>
                <p class="text-white-50 small mb-2"><i class="fas fa-map-marker-alt me-2 text-danger"></i> <%= Alamat1 != null ? Alamat1 : "-" %></p>
                <p class="text-white-50 small mb-2"><i class="fas fa-envelope me-2 text-success"></i> <%= Email != null ? Email : "-" %></p>
                <p class="text-white-50 small mb-4"><i class="fas fa-phone-alt me-2 text-info"></i> <%= Telepon != null ? Telepon : "-" %></p>
                <div class="d-flex gap-2 justify-content-center justify-content-md-start">
                    <a href="#" class="icon-medsos-alumni shadow-sm"><i class="fab fa-facebook-f"></i></a>
                    <a href="#" class="icon-medsos-alumni shadow-sm"><i class="fab fa-twitter"></i></a>
                    <a href="#" class="icon-medsos-alumni shadow-sm"><i class="fab fa-instagram"></i></a>
                </div>
            </div>
        </div>
        <hr class="mb-4 mt-3 opacity-25" style="border-color: #ffffff;">
        <div class="row align-items-center">
            <div class="col-md-12 col-lg-12 text-center">
                <p class="text-white-50 small mb-0">&copy; <%= java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) %> <strong class="footer-alumni-highlight"><%= judulNamaPerguruanTinggi != null ? judulNamaPerguruanTinggi : "" %></strong>. <%= Common.getBahasaConfig("Semua Hak Cipta Dilindungi oleh Undang-Undang.") %></p>
            </div>
        </div>
    </div>
</footer>