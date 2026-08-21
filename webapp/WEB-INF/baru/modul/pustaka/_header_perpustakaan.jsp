<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="java.net.URLEncoder"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    String rnd = request.getParameter("rnd");
    if (rnd == null) rnd = Common.getGeneratedBarCode(7);
    Tbmuser tbmuser = Common.getCurrentUser(request);
    String rolePustaka = tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null
            ? tbmuser.hakAkses().getRoleId().trim().toLowerCase() : "";
    boolean adminPustaka = "am".equals(rolePustaka) || rolePustaka.contains("admin");
    boolean petugasPustaka = adminPustaka || rolePustaka.contains("pustaka") || rolePustaka.contains("library");
    boolean anggotaPustaka = tbmuser != null && !petugasPustaka;
    String sectionPustaka = request.getParameter("s");
    boolean katalogPustaka = sectionPustaka == null || sectionPustaka.trim().isEmpty()
            || "katalog".equals(sectionPustaka) || "populer".equals(sectionPustaka);

    String judulNamaPerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama();
    String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
    String encodedUrl = URLEncoder.encode(Common.ROOT+"/pustaka", "UTF-8");
%>

<div class="page-bg-perpus"></div>

<nav class="navbar navbar-expand-lg navbar-light fixed-top navbar-custom-perpus py-3">
    <div class="container">
        <a class="navbar-brand fw-bold text-primary d-flex align-items-center" href="#">
            <% if (logo_PerguruanTinggi != null && !logo_PerguruanTinggi.trim().isEmpty()) { %>
                <img src="<%=logo_PerguruanTinggi%>" alt="Logo" width="40" height="40" class="me-2">
            <% } else { %>
                <i class="fas fa-book-reader fa-lg me-2"></i>
            <% } %>
            <span class="d-none d-sm-inline"><%= Common.getBahasaConfig("Perpustakaan Digital") %></span>
        </a>
        
        <button class="navbar-toggler border-0 shadow-none" type="button" data-bs-toggle="collapse" data-bs-target="#navbarPerpus<%=rnd%>">
            <span class="navbar-toggler-icon"></span>
        </button>
        
        <div class="collapse navbar-collapse" id="navbarPerpus<%=rnd%>">
            <ul class="navbar-nav ms-auto mb-2 mb-lg-0 fw-medium">
                <li class="nav-item px-1"><a class="nav-link text-dark hover-primary" href="<%=Common.ROOT%>/pustaka"><i class="fas fa-home me-1 text-primary"></i><%= Common.getBahasaConfig("Beranda") %></a></li>
                
                <li class="nav-item dropdown px-1">
                    <a class="nav-link dropdown-toggle text-dark" href="#" id="dropProfil" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                        <i class="fas fa-university me-1 text-primary"></i><%= Common.getBahasaConfig("Profil") %>
                    </a>
                    <ul class="dropdown-menu border-0 shadow-sm rounded-3" aria-labelledby="dropProfil">
                        <li><a class="dropdown-item small" href="#" onclick="panggilMenu<%=rnd%>('sejarah')"><i class="fas fa-history me-2 text-secondary"></i><%= Common.getBahasaConfig("Sejarah") %></a></li>
                        <li><a class="dropdown-item small" href="#" onclick="panggilMenu<%=rnd%>('visi_misi')"><i class="fas fa-bullseye me-2 text-secondary"></i><%= Common.getBahasaConfig("Visi & Misi") %></a></li>
                        <li><a class="dropdown-item small" href="#" onclick="panggilMenu<%=rnd%>('struktur_organisasi')"><i class="fas fa-sitemap me-2 text-secondary"></i><%= Common.getBahasaConfig("Struktur Organisasi") %></a></li>
                        <li><a class="dropdown-item small" href="#" onclick="panggilMenu<%=rnd%>('tata_tertib')"><i class="fas fa-clipboard-list me-2 text-secondary"></i><%= Common.getBahasaConfig("Tata Tertib") %></a></li>
                    </ul>
                </li>

                <li class="nav-item dropdown px-1">
                    <a class="nav-link dropdown-toggle text-dark" href="#" id="dropLayanan" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                        <i class="fas fa-concierge-bell me-1 text-primary"></i><%= Common.getBahasaConfig("Fasilitas & Layanan") %>
                    </a>
                    <ul class="dropdown-menu border-0 shadow-sm rounded-3" aria-labelledby="dropLayanan" style="min-width: 280px;">
                        <li class="dropdown-header fw-bold text-primary text-uppercase small"><%= Common.getBahasaConfig("Fasilitas") %></li>
                        <li><a class="dropdown-item small fw-bold text-dark" href="#" onclick="panggilMenu<%=rnd%>('sarpras')"><i class="fas fa-building me-2 text-secondary"></i><%= Common.getBahasaConfig("Sarana & Prasarana") %></a></li>
                        <li><a class="dropdown-item small ps-4" href="#" onclick="panggilMenu<%=rnd%>('wifi')"><i class="fas fa-wifi me-2 opacity-50"></i><%= Common.getBahasaConfig("Free Wifi") %></a></li>
                        <li><a class="dropdown-item small ps-4" href="#" onclick="panggilMenu<%=rnd%>('ruang_baca')"><i class="fas fa-book-open me-2 opacity-50"></i><%= Common.getBahasaConfig("Ruang Baca") %></a></li>
                        
                        <li><hr class="dropdown-divider"></li>
                        
                        <li class="dropdown-header fw-bold text-primary text-uppercase small"><%= Common.getBahasaConfig("Layanan") %></li>
                        <li><a class="dropdown-item small fw-bold text-dark" href="#" onclick="panggilMenu<%=rnd%>('waktu_layanan')"><i class="fas fa-clock me-2 text-secondary"></i><%= Common.getBahasaConfig("Waktu Layanan") %></a></li>
                        
                        <li><a class="dropdown-item small fw-bold text-dark mt-2" href="#" onclick="panggilMenu<%=rnd%>('sirkulasi')"><i class="fas fa-exchange-alt me-2 text-secondary"></i><%= Common.getBahasaConfig("Layanan Sirkulasi") %></a></li>
                        <li><a class="dropdown-item small ps-4" href="#" onclick="panggilMenu<%=rnd%>('peminjaman')">- <%= Common.getBahasaConfig("Peminjaman") %></a></li>
                        <li><a class="dropdown-item small ps-4" href="#" onclick="panggilMenu<%=rnd%>('peminjaman_online')">- <%= Common.getBahasaConfig("Peminjaman Online") %></a></li>
                        <li><a class="dropdown-item small ps-4" href="#" onclick="panggilMenu<%=rnd%>('pengembalian')">- <%= Common.getBahasaConfig("Pengembalian") %></a></li>
                        <li><a class="dropdown-item small ps-4" href="#" onclick="panggilMenu<%=rnd%>('perpanjangan')">- <%= Common.getBahasaConfig("Perpanjangan") %></a></li>
                        
                        <li><a class="dropdown-item small fw-bold text-dark mt-2" href="#" onclick="panggilMenu<%=rnd%>('referensi')"><i class="fas fa-bookmark me-2 text-secondary"></i><%= Common.getBahasaConfig("Layanan Referensi") %></a></li>
                        <li><a class="dropdown-item small ps-4" href="#" onclick="panggilMenu<%=rnd%>('katalog_bersama')">- <%= Common.getBahasaConfig("Katalog Bersama (E-Library)") %></a></li>
                        <li><a class="dropdown-item small ps-4" href="#" onclick="panggilMenu<%=rnd%>('katalog_nasional')">- <%= Common.getBahasaConfig("Katalog Nasional (OPAC)") %></a></li>
                        <li><a class="dropdown-item small ps-4" href="#" onclick="panggilMenu<%=rnd%>('e_journal')">- <%= Common.getBahasaConfig("E-Journal") %></a></li>
                    </ul>
                </li>

                <li class="nav-item dropdown px-1">
                    <a class="nav-link dropdown-toggle text-dark hover-primary" href="#" id="dropKatalogUtama" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                        <i class="fas fa-search me-1 text-primary"></i><%= Common.getBahasaConfig("Katalog") %>
                    </a>
                    <ul class="dropdown-menu border-0 shadow-sm rounded-3" aria-labelledby="dropKatalogUtama">
                        <li><a class="dropdown-item small" href="#" onclick="panggilMenu<%=rnd%>('katalog')"><i class="fas fa-book me-2 text-primary"></i><%= Common.getBahasaConfig("Pencarian Katalog") %></a></li>
                        <li><a class="dropdown-item small" href="#" onclick="panggilMenu<%=rnd%>('populer')"><i class="fas fa-fire me-2 text-danger"></i><%= Common.getBahasaConfig("Koleksi Populer") %></a></li>
                        <li><hr class="dropdown-divider"></li>
                        <li><a class="dropdown-item small" href="#" onclick="panggilMenu<%=rnd%>('sirkulasi_data')"><i class="fas fa-exchange-alt me-2 text-info"></i><%= Common.getBahasaConfig("Data Sirkulasi Terkini") %></a></li>
                        <li><a class="dropdown-item small" href="#" onclick="panggilMenu<%=rnd%>('kunjungan_data')"><i class="fas fa-shoe-prints me-2 text-success"></i><%= Common.getBahasaConfig("Data Kunjungan") %></a></li>
                    </ul>
                </li>

                <li class="nav-item px-1"><a class="nav-link text-dark hover-primary" href="<%=Common.ROOT%>/pustaka?availability=DIGITAL"><i class="fas fa-tablet-alt me-1 text-primary"></i><%= Common.getBahasaConfig("Koleksi Digital") %></a></li>
                <li class="nav-item px-1"><a class="nav-link text-dark hover-primary" href="#libraryHelp" onclick="if(window.showHelpModal){showHelpModal();return false;}"><i class="far fa-question-circle me-1 text-primary"></i><%= Common.getBahasaConfig("Bantuan") %></a></li>

                <% if (petugasPustaka) { %>
                    <li class="nav-item px-1"><a class="nav-link text-dark hover-primary" href="#" onclick="panggilMenu<%=rnd%>('dashboard')"><i class="fas fa-chart-line me-1 text-primary"></i><%= Common.getBahasaConfig("Dasbor") %></a></li>
                    <li class="nav-item px-1"><a class="nav-link text-dark hover-primary" href="#" onclick="panggilMenu<%=rnd%>('sirkulasi_data')"><%= Common.getBahasaConfig("Sirkulasi") %></a></li>
                    <li class="nav-item px-1"><a class="nav-link text-dark hover-primary" href="#" onclick="panggilMenu<%=rnd%>('katalogisasi')"><%= Common.getBahasaConfig("Katalogisasi") %></a></li>
                    <li class="nav-item dropdown px-1"><a class="nav-link dropdown-toggle text-dark hover-primary" href="#" role="button" data-bs-toggle="dropdown" aria-expanded="false"><%=Common.getBahasaConfig("Operasional")%></a><ul class="dropdown-menu border-0 shadow-sm rounded-3"><li><a class="dropdown-item" href="#" onclick="panggilMenu<%=rnd%>('operasional')"><%=Common.getBahasaConfig("Pengadaan, serial & inventaris")%></a></li><li><a class="dropdown-item" href="#" onclick="panggilMenu<%=rnd%>('operasional')"><%=Common.getBahasaConfig("Anggota & moderasi")%></a></li><li><a class="dropdown-item" href="#" onclick="panggilMenu<%=rnd%>('denda')"><%=Common.getBahasaConfig("Denda")%></a></li><li><a class="dropdown-item" href="#" onclick="panggilMenu<%=rnd%>('dashboard')"><%=Common.getBahasaConfig("Laporan & analytics")%></a></li></ul></li>
                    <% if (adminPustaka) { %><li class="nav-item px-1"><a class="nav-link text-dark hover-primary" href="#" onclick="panggilMenu<%=rnd%>('integrasi')"><%= Common.getBahasaConfig("Integrasi") %></a></li><% } %>
                <% } %>
                
                <% if (anggotaPustaka) { %>
                    <li class="nav-item dropdown px-1"><a class="nav-link dropdown-toggle text-dark hover-primary" href="#" role="button" data-bs-toggle="dropdown" aria-expanded="false"><i class="far fa-id-card me-1 text-secondary"></i><%= Common.getBahasaConfig("Beranda Anggota") %></a><ul class="dropdown-menu border-0 shadow-sm rounded-3"><li><a class="dropdown-item" href="<%=Common.ROOT%>/pustaka?s=beranda_anggota"><%=Common.getBahasaConfig("Ringkasan")%></a></li><li><a class="dropdown-item" href="<%=Common.ROOT%>/pustaka?s=beranda_anggota&tab=loans"><%=Common.getBahasaConfig("Pinjaman Saya")%></a></li><li><a class="dropdown-item" href="<%=Common.ROOT%>/pustaka?s=beranda_anggota&tab=holds"><%=Common.getBahasaConfig("Reservasi")%></a></li><li><a class="dropdown-item" href="<%=Common.ROOT%>/pustaka?s=beranda_anggota&tab=favorites"><%=Common.getBahasaConfig("Favorit")%></a></li><li><a class="dropdown-item" href="<%=Common.ROOT%>/pustaka?s=beranda_anggota#memberNotifications"><%=Common.getBahasaConfig("Notifikasi")%></a></li><li><a class="dropdown-item" href="<%=Common.ROOT%>/pustaka?s=operasional"><%=Common.getBahasaConfig("Pencarian tersimpan")%></a></li></ul></li>
                <% } %>
            </ul>
            
            <div class="d-flex ms-lg-3 mt-3 mt-lg-0 align-items-center">
                <% if (tbmuser == null) { %>
                    <a href="<%=Common.ROOT%>/pustaka?hanya_tampil_jsp=true&p=pustaka&s=login_pustaka" class="btn btn-outline-primary rounded-pill px-4 fw-bold shadow-sm transition-all"><i class="fas fa-sign-in-alt me-2"></i><%= Common.getBahasaConfig("Masuk Sistem") %></a>
                <% } else { %>
                    <a href="#" onclick="panggilMenu<%=rnd%>('beranda_anggota')" class="btn btn-primary rounded-pill px-4 fw-bold shadow-sm me-2 transition-all"><i class="fas fa-user-circle me-2"></i><%= Common.getBahasaConfig("Beranda") %></a>
                    <a href="#" onclick="handleLogout<%=rnd%>(event)" class="btn btn-outline-danger rounded-pill px-4 fw-bold shadow-sm transition-all hover-danger"><i class="fas fa-sign-out-alt me-2"></i><%= Common.getBahasaConfig("Keluar") %></a>
                    <script>
                    function handleLogout<%=rnd%>(event) {
                        event.preventDefault();
                        showConfirmModal('<%= Common.getBahasaConfigJS("Apakah Anda yakin ingin keluar?") %>', function() {
                            window.location.href = '<%=Common.ROOT%>/logoff?param=<%=encodedUrl%>';
                        });
                    }
                    </script>
                <% } %>
            </div>
        </div>
    </div>
</nav>

<% if (!katalogPustaka) { %><div class="hero-section-perpus pb-5 mb-5 text-center">
    <div class="container pb-4 pt-4">
        <h2 class="fw-bold text-white text-uppercase tracking-wide-perpus text-shadow-perpus mb-2 animate__animated animate__fadeInDown">
            <%= judulNamaPerguruanTinggi != null ? judulNamaPerguruanTinggi : "" %>
        </h2>
        <div class="d-inline-block badge-perpus rounded-pill px-4 py-2 mb-3 mt-3 animate__animated animate__zoomIn shadow-sm">
            <h3 class="fw-bold mb-0 text-white text-shadow-perpus"><i class="fas fa-book-reader me-2 text-warning"></i><%= Common.getBahasaConfig("Portal Perpustakaan Digital") %></h3>
        </div>
        <p class="lead fw-normal mb-2 text-white opacity-75 mx-auto animate__animated animate__fadeInUp" style="max-width: 850px; font-size: 1.15rem;">
            <%= Common.getBahasaConfig("Selamat datang di layanan perpustakaan terpadu. Jelajahi koleksi pustaka, temukan buku populer, serta pantau aktivitas sirkulasi dan kunjungan anggota secara langsung.") %>
        </p>
    </div>
</div><% } %>
