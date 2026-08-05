<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
    private String dmsAfterH(Object value) {
        if (value == null) return "";
        String s = String.valueOf(value);
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    String userDisplayAfter = request.getAttribute("DMS_USER_DISPLAY") == null ? "Pengguna" : String.valueOf(request.getAttribute("DMS_USER_DISPLAY"));
%>
<header class="dms-hero py-5 py-lg-6">
    <div class="container py-4 py-lg-5">
        <div class="row align-items-center g-4">
            <div class="col-lg-7">
                <span class="dms-pill mb-3"><i class="fa fa-user-check"></i> Sesi Login Aktif</span>
                <h1 class="display-5 fw-bold mb-3"><%=Common.getBahasaConfig("Portal Dokumen Siap Digunakan")%></h1>
                <p class="lead mb-4 opacity-90">
                    Selamat datang, <strong><%=dmsAfterH(userDisplayAfter)%></strong>. Dokumen tetap disusun dalam katalog yang mudah dibaca, dan tombol download akan muncul pada dokumen yang memiliki lampiran.
                </p>
                <div class="d-flex flex-wrap gap-2">
                    <a href="#dokumen" class="btn btn-light btn-lg rounded-pill px-4 fw-bold"><i class="fa fa-download me-2"></i>Lihat Dokumen</a>
                    <a href="#ketentuan" class="btn btn-outline-light btn-lg rounded-pill px-4 fw-bold"><i class="fa fa-shield-halved me-2"></i>Baca Ketentuan</a>
                </div>
            </div>
            <div class="col-lg-5">
                <div class="dms-glass p-4 p-lg-5">
                    <div class="d-flex gap-3 mb-4">
                        <div class="dms-login-icon"><i class="fa fa-file-arrow-down fa-xl"></i></div>
                        <div>
                            <h4 class="fw-bold mb-1">Download Aktif</h4>
                            <p class="mb-0 opacity-75">Setiap dokumen yang memiliki lampiran akan menampilkan tombol download.</p>
                        </div>
                    </div>
                    <div class="row g-3">
                        <div class="col-6">
                            <div class="bg-white bg-opacity-10 rounded-4 p-3 h-100">
                                <i class="fa fa-check-circle mb-2"></i>
                                <div class="fw-bold">Terverifikasi</div>
                                <div class="small opacity-75">Akses file mengikuti sesi login pengguna.</div>
                            </div>
                        </div>
                        <div class="col-6">
                            <div class="bg-white bg-opacity-10 rounded-4 p-3 h-100">
                                <i class="fa fa-clock-rotate-left mb-2"></i>
                                <div class="fw-bold">Mudah Dilacak</div>
                                <div class="small opacity-75">Struktur arsip bertingkat tetap mudah ditelusuri.</div>
                            </div>
                        </div>
                    </div>
                    <div class="mt-4 small opacity-75"><i class="fa fa-circle-info me-1"></i>Jika tombol download belum muncul, muat ulang halaman setelah login berhasil.</div>
                </div>
            </div>
        </div>
    </div>
</header>

<section class="container py-5">
    <div class="row g-3">
        <div class="col-md-4">
            <div class="dms-feature">
                <i class="fa fa-sitemap"></i>
                <h5 class="fw-bold">Navigasi Arsip</h5>
                <p class="dms-muted mb-0">Panel ini menunjukkan posisi pengguna di dalam susunan dokumen agar mudah kembali ke ruang utama atau sub ruang sebelumnya.</p>
            </div>
        </div>
        <div class="col-md-4">
            <div class="dms-feature">
                <i class="fa fa-file-circle-check"></i>
                <h5 class="fw-bold">Status Lampiran</h5>
                <p class="dms-muted mb-0">Setiap kartu menampilkan apakah lampiran sudah tersedia, sehingga pengguna tidak perlu mencoba satu per satu secara manual.</p>
            </div>
        </div>
        <div class="col-md-4">
            <div class="dms-feature">
                <i class="fa fa-arrow-down-wide-short"></i>
                <h5 class="fw-bold">Akses Download</h5>
                <p class="dms-muted mb-0">Tombol download hanya tampil untuk dokumen yang memang memiliki file dan hanya saat sesi login masih aktif.</p>
            </div>
        </div>
    </div>
</section>
