<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common" %>
<%
    // Inisialisasi Data Perguruan Tinggi khusus untuk Header
    String judulNamaPerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama();
    String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
%>

<!-- Latar Belakang Transparan (Watermark Keseluruhan Halaman) -->
<div class="bg-polos-alumni" style="background-image: url('<%=Common.ROOT%>/img/Reports.png');"></div>

<!-- Kop Header Repository (Terintegrasi dengan Tema) -->
<div class="pt-4 pb-5 mb-5 shadow text-center rounded-bottom-4 d-print-none header-alumni-bg" style="--bg-image-url: url('https://images.unsplash.com/photo-1518531933037-91b2f5f229cc?q=80&w=1920&auto=format&fit=crop');">
    <div class="container pb-4 pt-4">
        <% if (logo_PerguruanTinggi != null && !logo_PerguruanTinggi.trim().isEmpty()) { %>
            <img src="<%=logo_PerguruanTinggi%>" alt="<%= Common.getBahasaConfig("Logo Institusi") %>" class="img-fluid mb-4 animate__animated animate__fadeInDown logo-institusi-alumni" style="max-height: 100px;">
        <% } %>

        <h2 class="fw-bold text-uppercase mb-3 animate__animated animate__fadeInDown header-alumni-title">
            <%= judulNamaPerguruanTinggi != null ? judulNamaPerguruanTinggi : "" %>
        </h2>

        <div class="d-inline-block rounded-pill px-4 py-2 mb-4 animate__animated animate__zoomIn header-alumni-subtitle-box">
            <h3 class="fw-bold mb-0 header-alumni-subtitle">
                <i class="fas fa-layer-group me-2 icon-accent-alumni"></i><%= Common.getBahasaConfig("Repositori Digital Terpadu") %>
            </h3>
        </div>

        <p class="lead fw-normal mb-2 opacity-75 mx-auto animate__animated animate__fadeInUp header-alumni-desc" style="max-width: 800px;">
            <%= Common.getBahasaConfig("Satu pintu pencarian untuk semua koleksi publik, karya ilmiah, arsip, dan sumber elektronik secara tersentralisasi.") %>
        </p>
    </div>
</div>