<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<section id="dokumen" class="container py-5">
    <div class="row justify-content-center mb-4">
        <div class="col-lg-9 text-center">
            <span class="badge rounded-pill text-bg-primary px-3 py-2 mb-3"><i class="fa fa-layer-group me-1"></i>Daftar Dokumen</span>
            <h2 class="dms-section-title mb-3">Katalog Dokumen Manajemen Sistem</h2>
            <p class="dms-muted mb-0">
                Panel ini menampilkan kategori, sub ruang, dan dokumen yang tersimpan di database. Pengunjung umum hanya melihat nama dan susunan dokumen, sedangkan pengguna yang sudah login dapat mengunduh lampiran yang tersedia.
            </p>
        </div>
    </div>

    <div class="dms-help-panel mb-4">
        <div class="row g-3 align-items-center">
            <div class="col-lg-8">
                <div class="d-flex gap-3 align-items-start">
                    <div class="dms-metric-icon"><i class="fa fa-circle-info"></i></div>
                    <div>
                        <h5 class="fw-bold mb-1">Cara membaca katalog</h5>
                        <p class="mb-0 dms-muted">Klik tombol <strong>Buka Ruang</strong> untuk melihat isi dokumen di dalamnya. Gunakan kolom pencarian untuk menemukan dokumen berdasarkan nama, kode, atau keterangan.</p>
                    </div>
                </div>
            </div>
            <div class="col-lg-4">
                <div class="dms-css-bar" aria-label="Indikator visual katalog"><span style="width:82%"></span></div>
                <div class="small dms-muted mt-2">Visual ini hanya indikator CSS ringan, bukan JFreeChart.</div>
            </div>
        </div>
    </div>

    <div class="dms-card p-3 p-lg-4 dms-service-box">
        <div id="dmsExplorer">
            <jsp:include page="/WEB-INF/baru/modul/dms/_dms_service.jsp"></jsp:include>
        </div>
    </div>
</section>

<section id="ketentuan" class="container pb-5">
    <div class="row g-3">
        <div class="col-lg-4">
            <div class="dms-card p-4 h-100">
                <div class="dms-metric-icon mb-3"><i class="fa fa-eye"></i></div>
                <h5 class="fw-bold">Akses Publik</h5>
                <p class="dms-muted mb-0">Nama dokumen ditampilkan agar pengguna dapat mengetahui arsip apa saja yang tersedia tanpa harus mengunduh file.</p>
            </div>
        </div>
        <div class="col-lg-4">
            <div class="dms-card p-4 h-100">
                <div class="dms-metric-icon mb-3"><i class="fa fa-lock"></i></div>
                <h5 class="fw-bold">Perlindungan File</h5>
                <p class="dms-muted mb-0">Lampiran hanya dapat diunduh oleh pengguna yang sudah login, sehingga file resmi tetap terlindungi dari akses langsung tanpa sesi.</p>
            </div>
        </div>
        <div class="col-lg-4">
            <div class="dms-card p-4 h-100">
                <div class="dms-metric-icon mb-3"><i class="fa fa-database"></i></div>
                <h5 class="fw-bold">Sumber Database</h5>
                <p class="dms-muted mb-0">Katalog berasal dari data Akreditasi dan DokumenAkreditasi, bukan dari pembacaan folder fisik server.</p>
            </div>
        </div>
    </div>
</section>
