<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<header class="dms-hero py-5 py-lg-6">
    <div class="container py-4 py-lg-5">
        <div class="row align-items-center g-4">
            <div class="col-lg-7">
                <span class="dms-pill mb-3"><i class="fa fa-database"></i> Katalog Dokumen Publik</span>
                <h1 class="display-5 fw-black fw-bold mb-3"><%=Common.getBahasaConfig("Dokumen Manajemen Sistem")%></h1>
                <p class="lead mb-4 opacity-90">
                    Halaman ini menampilkan daftar dokumen resmi yang dapat dilihat oleh publik. Nama dokumen dan susunan arsip tetap terbuka, sedangkan tombol download baru muncul setelah pengguna login.
                </p>
                <div class="d-flex flex-wrap gap-2">
                    <a href="#dokumen" class="btn btn-light btn-lg rounded-pill px-4 fw-bold"><i class="fa fa-eye me-2"></i>Lihat Katalog</a>
                    <button type="button" class="btn btn-outline-light btn-lg rounded-pill px-4 fw-bold" data-dms-login-open="true"><i class="fa fa-lock-open me-2"></i>Login untuk Download</button>
                </div>
            </div>
            <div class="col-lg-5">
                <div class="dms-glass p-4 p-lg-5">
                    <div class="d-flex gap-3 mb-4">
                        <div class="dms-login-icon"><i class="fa fa-folder-open fa-xl"></i></div>
                        <div>
                            <h4 class="fw-bold mb-1">Akses Informasi Transparan</h4>
                            <p class="mb-0 opacity-75">Pengunjung dapat mengetahui dokumen apa saja yang tersedia tanpa perlu login.</p>
                        </div>
                    </div>
                    <div class="row g-3">
                        <div class="col-6">
                            <div class="bg-white bg-opacity-10 rounded-4 p-3 h-100">
                                <i class="fa fa-list-check mb-2"></i>
                                <div class="fw-bold">Katalog Terbuka</div>
                                <div class="small opacity-75">Nama arsip dan deskripsi tampil jelas.</div>
                            </div>
                        </div>
                        <div class="col-6">
                            <div class="bg-white bg-opacity-10 rounded-4 p-3 h-100">
                                <i class="fa fa-shield-halved mb-2"></i>
                                <div class="fw-bold">Download Aman</div>
                                <div class="small opacity-75">File hanya dapat diunduh setelah login.</div>
                            </div>
                        </div>
                    </div>
                    <div class="mt-4 small opacity-75"><i class="fa fa-circle-info me-1"></i>Login dilakukan melalui popup, bukan membuka halaman JSP login secara langsung.</div>
                </div>
            </div>
        </div>
    </div>
</header>

<section class="container py-5">
    <div class="row g-3">
        <div class="col-md-4">
            <div class="dms-feature">
                <i class="fa fa-folder-tree"></i>
                <h5 class="fw-bold">Ruang Arsip</h5>
                <p class="dms-muted mb-0">Panel ini membantu pengguna memahami pengelompokan dokumen resmi seperti akreditasi, audit, sertifikasi, dan pedoman institusi.</p>
            </div>
        </div>
        <div class="col-md-4">
            <div class="dms-feature">
                <i class="fa fa-magnifying-glass"></i>
                <h5 class="fw-bold">Pencarian Cepat</h5>
                <p class="dms-muted mb-0">Pengguna dapat mengetik kata kunci untuk menemukan dokumen berdasarkan nama, kode, atau keterangan tanpa memahami struktur teknis sistem.</p>
            </div>
        </div>
        <div class="col-md-4">
            <div class="dms-feature">
                <i class="fa fa-user-lock"></i>
                <h5 class="fw-bold">Hak Akses Download</h5>
                <p class="dms-muted mb-0">Tombol download disembunyikan dari publik agar file tetap terlindungi dan hanya dibuka oleh pengguna yang sudah terverifikasi.</p>
            </div>
        </div>
    </div>
</section>
