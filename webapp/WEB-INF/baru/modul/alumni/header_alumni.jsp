<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    String judulNamaPerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama();
    String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
%>

<!-- Latar Belakang Transparan (Watermark) -->
<div class="bg-polos-alumni" style="background-image: url('<%=Common.ROOT%>/img/pmb_bg.jpg');"></div>

<!-- Kop Header Alumni (Terintegrasi dengan Tema) -->
<div class="pt-4 pb-5 mb-5 shadow text-center rounded-bottom-4 d-print-none header-alumni-bg" style="--bg-image-url: url('<%=Common.ROOT%>/img/pmb_bg.jpg');">
    <div class="container pb-4 pt-4">
        <% if (logo_PerguruanTinggi != null && !logo_PerguruanTinggi.trim().isEmpty()) { %>
            <img src="<%=logo_PerguruanTinggi%>" alt="<%= Common.getBahasaConfig("Logo Institusi") %>" class="img-fluid mb-4 animate__animated animate__fadeInDown logo-institusi-alumni">
        <% } %>

        <h2 class="fw-bold text-uppercase mb-3 animate__animated animate__fadeInDown header-alumni-title">
            <%= judulNamaPerguruanTinggi != null ? judulNamaPerguruanTinggi : "" %>
        </h2>

        <div class="d-inline-block rounded-pill px-4 py-2 mb-4 animate__animated animate__zoomIn header-alumni-subtitle-box">
            <h3 class="fw-bold mb-0 header-alumni-subtitle">
                <i class="fas fa-user-graduate me-2 icon-accent-alumni"></i><%= Common.getBahasaConfig("Portal Alumni & Tracer Study") %>
            </h3>
        </div>

        <p class="lead fw-normal mb-2 opacity-75 mx-auto animate__animated animate__fadeInUp header-alumni-desc">
            <%= Common.getBahasaConfig("Selamat datang para lulusan! Temukan informasi terkini, peluang karir, serta bantu kami meningkatkan kualitas institusi melalui pengisian kuesioner Tracer Study.") %>
        </p>
    </div>
</div>