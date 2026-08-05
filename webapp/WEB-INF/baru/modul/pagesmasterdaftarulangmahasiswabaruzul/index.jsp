<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.JenisKegiatan"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String activeTab = request.getParameter("tab");
    if (activeTab == null || activeTab.isEmpty()) { activeTab = "bayar"; }
    
    JenisKegiatan jenisKegiatan = ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU;
    String jkId = jenisKegiatan != null ? String.valueOf(jenisKegiatan.getId()) : "";
    
    String p = request.getParameter("p") != null ? request.getParameter("p") : "";
    String s = request.getParameter("s") != null ? request.getParameter("s") : "";
    String baseUrl = "?p=" + p + "&s=" + s;
%>

<style>
    .nav-lazy-tab .nav-link { transition: all 0.3s ease; border: 2px solid transparent; }
    .nav-lazy-tab .nav-link:hover { border-color: rgba(0,0,0,0.1); background-color: #f8f9fa; }
    .nav-lazy-tab .nav-link.active { transform: translateY(-3px); box-shadow: 0 0.5rem 1rem rgba(23,162,184,.2); border-color: transparent; }
</style>

<div class="container-fluid py-3">
    <ul class="nav nav-pills nav-lazy-tab justify-content-center mb-4 border-bottom pb-4" role="tablist">
        <li class="nav-item mx-2">
            <a class="nav-link fw-bold px-5 py-2 rounded-pill shadow-sm <%= "bayar".equals(activeTab) ? "active bg-info text-white" : "bg-white text-secondary border" %>" href="<%=baseUrl%>&tab=bayar">
                <i class="fas fa-id-card me-2"></i><%= Common.getBahasaConfig("Daftar Ulang Maba") %>
            </a>
        </li>
        <li class="nav-item mx-2">
            <a class="nav-link fw-bold px-5 py-2 rounded-pill shadow-sm <%= "riwayat".equals(activeTab) ? "active bg-info text-white" : "bg-white text-secondary border" %>" href="<%=baseUrl%>&tab=riwayat">
                <i class="fas fa-tasks me-2"></i><%= Common.getBahasaConfig("Riwayat Daftar Ulang") %>
            </a>
        </li>
    </ul>

    <div class="tab-content animate__animated animate__fadeIn">
        <% if ("riwayat".equals(activeTab)) { %>
            <jsp:include page="/WEB-INF/baru/modul/bayarmhs/riwayat_pembayaran_mhs.jsp">
                <jsp:param value="<%=jkId%>" name="hanya_untuk_jenis_kegiatan" />
                <jsp:param value="false" name="hanya_untuk_mahasiswa" />
            </jsp:include>
        <% } else { %>
            <jsp:include page="/WEB-INF/baru/modul/bayarmhs/pembayaran_online_mhs.jsp">
                <jsp:param value="<%=jkId%>" name="hanya_untuk_jenis_kegiatan" />
                <jsp:param value="false" name="hanya_untuk_mahasiswa" />
            </jsp:include>
        <% } %>
    </div>
</div>