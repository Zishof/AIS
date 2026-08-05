<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
    private String vb(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    String rnd = Common.getGeneratedBarCode(7);
    String judul = "";
    String motto = "";
    String alamat = "";
    String telp = "";
    String email = "";
    String logo = "";
    String bg = "";
    String heroBadge = "Portal e-Procurement & Rekanan";
    String heroTitle = "Pendaftaran Vendor Terintegrasi ERP";
    String heroDesc = "Daftarkan perusahaan, pantau pengumuman proyek, lengkapi legalitas, unggah dokumen kualifikasi, dan kelola profil rekanan secara mandiri dalam satu portal modern.";
    String daftarInfo = "Isi form pendaftaran awal. Satu email hanya dapat digunakan untuk satu vendor. Apabila akun pernah dibuat namun email akses hilang, gunakan fitur kirim ulang akses.";
    String vendorMenuDaftar = "Daftar Vendor";
    String vendorMenuLogin = "Login Vendor";
    try {
        PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
        if (pt != null) {
            judul = pt.getNama() == null ? "" : pt.getNama();
            motto = pt.getMotto() == null ? "" : pt.getMotto();
            alamat = pt.getAlamat1() == null ? "" : pt.getAlamat1();
            telp = pt.getTelepon() == null ? "" : pt.getTelepon();
            email = pt.getEmail() == null ? "" : pt.getEmail();
        }
        logo = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
        bg = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "background_perguruanTinggi_");
        heroBadge = Common.getKonfigurasi("vendor_teks_hero_badge", heroBadge).getNilai();
        heroTitle = Common.getKonfigurasi("vendor_teks_hero_title", heroTitle).getNilai();
        heroDesc = Common.getKonfigurasi("vendor_teks_hero_deskripsi", heroDesc).getNilai();
        daftarInfo = Common.getKonfigurasi("vendor_teks_daftar_info", daftarInfo).getNilai();
        vendorMenuDaftar = Common.getKonfigurasi("vendor_menu_daftar", vendorMenuDaftar).getNilai();
        vendorMenuLogin = Common.getKonfigurasi("vendor_menu_login", vendorMenuLogin).getNilai();
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/sebelum_login.jsp:43");}
    if (judul == null || judul.trim().isEmpty()) judul = "Instansi";
    if (logo == null || logo.trim().isEmpty()) logo = Common.ROOT + "/img/logo.png";
    if (bg == null || bg.trim().isEmpty()) bg = Common.ROOT + "/img/bg1.png";
%>
<section class="position-relative overflow-hidden" style="background:linear-gradient(135deg, rgba(15,23,42,.90), rgba(29,78,216,.78)), url('<%=bg%>') center/cover no-repeat;">
    <div class="container py-5 py-lg-6 position-relative">
        <div class="row align-items-center g-5 py-4">
            <div class="col-lg-7 text-white animate__animated animate__fadeInLeft">
                <span class="badge rounded-pill px-3 py-2 mb-3" style="background:rgba(255,255,255,.16);border:1px solid rgba(255,255,255,.26);">
                    <i class="fas fa-shield-alt me-1 text-warning"></i> <%=vb(heroBadge)%>
                </span>
                <h1 class="display-5 fw-black text-white mb-3" style="font-weight:900;letter-spacing:-.04em;"><%=vb(heroTitle)%></h1>
                <p class="lead text-white-50 mb-4" style="max-width:740px;"><%=vb(heroDesc)%></p>
                <div class="d-flex flex-wrap gap-3">
                    <button type="button" class="btn btn-warning btn-lg rounded-pill fw-bold px-4 shadow" onclick="vendorOpenRegisterModal()"><i class="fas fa-user-plus me-2"></i><%=vb(vendorMenuDaftar)%></button>
                    <button class="btn btn-outline-light btn-lg rounded-pill fw-bold px-4" onclick="vendorOpenLoginModal()"><i class="fas fa-sign-in-alt me-2"></i><%=vb(vendorMenuLogin)%></button>
                    <a href="#kirim-ulang-akses" class="btn btn-outline-light btn-lg rounded-pill fw-bold px-4"><i class="fas fa-envelope-open-text me-2"></i>Kirim Ulang Akses</a>
                    <a href="#syarat" class="btn btn-light btn-lg rounded-pill fw-bold px-4"><i class="fas fa-file-contract me-2"></i>Lihat Syarat</a>
                </div>
                <div class="row g-3 mt-4">
                    <div class="col-sm-4"><div class="p-3 rounded-4" style="background:rgba(255,255,255,.12);"><i class="fas fa-check-circle text-warning me-2"></i><strong>Terintegrasi</strong><br><small class="text-white-50">langsung ke ERP</small></div></div>
                    <div class="col-sm-4"><div class="p-3 rounded-4" style="background:rgba(255,255,255,.12);"><i class="fas fa-cloud-upload-alt text-warning me-2"></i><strong>Dokumen</strong><br><small class="text-white-50">legalitas online</small></div></div>
                    <div class="col-sm-4"><div class="p-3 rounded-4" style="background:rgba(255,255,255,.12);"><i class="fas fa-bullhorn text-warning me-2"></i><strong>Pengumuman</strong><br><small class="text-white-50">proyek terbaru</small></div></div>
                </div>
            </div>
            <div class="col-lg-5 animate__animated animate__fadeInRight">
                <div class="card vendor-card-<%=rnd%> border-0 shadow-lg">
                    <div class="card-body p-4 p-lg-5">
                        <div class="d-flex align-items-center gap-3 mb-4">
                            <img src="<%=logo%>" style="width:64px;height:64px;object-fit:contain;" class="rounded-4 bg-white shadow-sm p-2" alt="Logo">
                            <div>
                                <h5 class="fw-bold mb-1"><%=vb(judul)%></h5>
                                <div class="text-muted small"><%=vb(motto)%></div>
                            </div>
                        </div>
                        <h4 class="fw-bold mb-3"><i class="fas fa-handshake text-primary me-2"></i>Mulai kerja sama sebagai vendor</h4>
                        <p class="text-muted mb-4"><%=vb(daftarInfo)%></p>
                        <button type="button" class="btn vendor-btn-gradient-<%=rnd%> rounded-pill w-100 py-3 fw-bold" onclick="vendorOpenRegisterModal()"><i class="fas fa-user-plus me-2"></i>Buka Form Pendaftaran</button>
                        <a href="#kirim-ulang-akses" class="btn btn-outline-primary rounded-pill w-100 py-3 fw-bold mt-2"><i class="fas fa-paper-plane me-2"></i>Kirim Ulang Username & Password</a>
                    </div>
                </div>
            </div>
        </div>
    </div>
</section>

<section id="pendaftaran" class="container py-5">
    <div class="row g-4 align-items-stretch">
        <div class="col-lg-5">
            <div class="vendor-soft-card-<%=rnd%> p-4 h-100">
                <span class="vendor-icon-box-<%=rnd%> mb-3"><i class="fas fa-user-tie"></i></span>
                <h2 class="vendor-section-title-<%=rnd%> mb-3">Pendaftaran Vendor</h2>
                <p class="text-muted">Daftar sekali untuk membuat akun vendor. Setelah akun dibuat, data perusahaan dan dokumen bisa dilengkapi dari dashboard vendor.</p>
                <div class="alert alert-info d-flex gap-3 mt-4 rounded-4">
                    <i class="fas fa-info-circle fs-4 mt-1"></i>
                    <div><strong>Catatan:</strong> gunakan email aktif karena sistem akan mengirimkan username dan password portal vendor ke alamat email tersebut.</div>
                </div>
            </div>
        </div>
        <div class="col-lg-7">
            <div class="vendor-soft-card-<%=rnd%> p-4 p-lg-5 h-100 d-flex flex-column justify-content-center">
                <div class="d-flex align-items-start gap-3 mb-4">
                    <div class="vendor-icon-box-<%=rnd%> flex-shrink-0"><i class="fas fa-window-restore"></i></div>
                    <div>
                        <h4 class="fw-bold mb-2">Form pendaftaran dibuka dalam popup proporsional</h4>
                        <p class="text-muted mb-0">Form dibuat ringkas agar mudah dipakai. Data awal ini menjadi dasar pembuatan akun dan pengiriman username serta password ke email vendor.</p>
                    </div>
                </div>
                <div class="row g-3 mb-4">
                    <div class="col-md-4"><div class="p-3 rounded-4 bg-light h-100"><i class="fas fa-building text-primary me-2"></i><strong>Data Vendor</strong><br><small class="text-muted">nama perusahaan & kontak</small></div></div>
                    <div class="col-md-4"><div class="p-3 rounded-4 bg-light h-100"><i class="fas fa-envelope text-primary me-2"></i><strong>Email Aktif</strong><br><small class="text-muted">untuk akun login</small></div></div>
                    <div class="col-md-4"><div class="p-3 rounded-4 bg-light h-100"><i class="fas fa-paper-plane text-primary me-2"></i><strong>Akun Otomatis</strong><br><small class="text-muted">username & password</small></div></div>
                </div>
                <div class="d-flex flex-wrap gap-2">
                    <button type="button" class="btn vendor-btn-gradient-<%=rnd%> rounded-pill px-4 py-3 fw-bold" onclick="vendorOpenRegisterModal()"><i class="fas fa-user-plus me-2"></i>Buka Form Pendaftaran Vendor</button>
                    <a href="#kirim-ulang-akses" class="btn btn-outline-primary rounded-pill px-4 py-3 fw-bold"><i class="fas fa-envelope-open-text me-2"></i>Kirim Ulang Akses</a>
                </div>
            </div>
        </div>
    </div>
</section>

<section id="kirim-ulang-akses" class="container pb-5">
    <div class="row g-4 align-items-stretch">
        <div class="col-lg-5">
            <div class="vendor-soft-card-<%=rnd%> p-4 h-100">
                <span class="vendor-icon-box-<%=rnd%> mb-3"><i class="fas fa-envelope-open-text"></i></span>
                <h2 class="vendor-section-title-<%=rnd%> mb-3">Kirim Ulang Akses Vendor</h2>
                <p class="text-muted">Gunakan fitur ini jika email username/password tidak terkirim, terhapus, atau tidak ditemukan. Masukkan email kontak vendor yang sudah pernah terdaftar. Demi keamanan dan untuk menghindari data ganda, sistem tidak akan membuat vendor baru untuk email yang sama.</p>
                <div class="alert alert-info d-flex gap-3 mt-4 rounded-4">
                    <i class="fas fa-shield-alt fs-4 mt-1"></i>
                    <div><strong>Keamanan:</strong> sistem akan membuat password baru dan mengirimkannya kembali ke email terdaftar. Password lama otomatis tidak digunakan lagi.</div>
                </div>
            </div>
        </div>
        <div class="col-lg-7">
            <div class="vendor-soft-card-<%=rnd%> p-4 p-lg-5 h-100">
                <form id="formKirimUlangAkses<%=rnd%>">
                    <div class="mb-3">
                        <label class="form-label vendor-required fw-semibold"><%= Common.getBahasaConfig("Email Vendor Terdaftar") %></label>
                        <input type="email" name="email" class="form-control form-control-lg rounded-3" required placeholder="vendor@email.com">
                    </div>
                    <p class="text-muted small mb-4">Jika email ditemukan, sistem akan mengirim ulang informasi akun vendor berupa username, password baru, dan tautan login. Periksa folder Inbox, Spam, Promosi, atau Junk Mail setelah proses pengiriman.</p>
                    <div class="d-flex flex-wrap gap-2 justify-content-end">
                        <button type="reset" class="btn btn-light rounded-pill px-4 fw-bold"><i class="fas fa-undo me-2"></i>Reset</button>
                        <button type="submit" class="btn vendor-btn-gradient-<%=rnd%> rounded-pill px-4 fw-bold"><i class="fas fa-paper-plane me-2"></i>Kirim Ulang Akses</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</section>

<section id="syarat" class="py-5 bg-white border-top border-bottom">
    <div class="container">
        <div class="text-center mb-5">
            <span class="badge vendor-badge-soft rounded-pill px-3 py-2 mb-3"><i class="fas fa-clipboard-check me-1"></i>Kualifikasi Rekanan</span>
            <h2 class="vendor-section-title-<%=rnd%>">Syarat dan Ketentuan Vendor</h2>
            <p class="text-muted mx-auto" style="max-width:760px;">Syarat dibagi menjadi kualifikasi umum dan khusus sesuai bidang pekerjaan.</p>
        </div>
        <div class="row g-4">
            <div class="col-lg-6">
                <div class="vendor-soft-card-<%=rnd%> p-4 h-100">
                    <h4 class="fw-bold mb-3"><i class="fas fa-file-shield text-primary me-2"></i>UMUM</h4>
                    <ol class="lh-lg text-muted">
                        <li>Memiliki NIB sesuai OSS.</li><li>Memiliki Izin Usaha Efektif sesuai klasifikasi bidang usaha.</li><li>Memiliki Izin Lokasi sesuai OSS.</li><li>Memiliki NPWP.</li><li>Memiliki Surat Pengukuhan PKP untuk proyek di atas Rp 30.000.000.</li><li>Memenuhi kewajiban perpajakan tahun pajak terakhir.</li><li>Akta Pendirian Perusahaan dan/atau perubahannya.</li><li>KTP Pimpinan/Direktur.</li><li>Laporan Keuangan Tahun Terakhir.</li><li>Tidak termasuk dalam daftar hitam.</li><li>Membuat surat pernyataan kebenaran data dan tidak sedang dikenakan sanksi.</li>
                    </ol>
                </div>
            </div>
            <div class="col-lg-6">
                <div class="vendor-soft-card-<%=rnd%> p-4 h-100">
                    <h4 class="fw-bold mb-3"><i class="fas fa-hard-hat text-primary me-2"></i>KHUSUS</h4>
                    <div class="accordion" id="accordionSyaratVendor<%=rnd%>">
                        <div class="accordion-item border-0 shadow-sm rounded-4 mb-3 overflow-hidden">
                            <h2 class="accordion-header"><button class="accordion-button fw-bold" type="button" data-bs-toggle="collapse" data-bs-target="#konstruksi<%=rnd%>"><i class="fas fa-building me-2"></i>Konstruksi</button></h2>
                            <div id="konstruksi<%=rnd%>" class="accordion-collapse collapse show" data-bs-parent="#accordionSyaratVendor<%=rnd%>"><div class="accordion-body text-muted"><ol class="lh-lg"><li>Memiliki IUJK yang masih berlaku/teregistrasi.</li><li>Memiliki SBU bidang jasa konstruksi.</li><li>Memiliki pengalaman pekerjaan serupa minimum 1 tahun.</li><li>Untuk pekerjaan khusus, memiliki tenaga ahli bersertifikat SKA.</li></ol></div></div>
                        </div>
                        <div class="accordion-item border-0 shadow-sm rounded-4 overflow-hidden">
                            <h2 class="accordion-header"><button class="accordion-button collapsed fw-bold" type="button" data-bs-toggle="collapse" data-bs-target="#barang<%=rnd%>"><i class="fas fa-boxes-stacked me-2"></i>Pengadaan Barang</button></h2>
                            <div id="barang<%=rnd%>" class="accordion-collapse collapse" data-bs-parent="#accordionSyaratVendor<%=rnd%>"><div class="accordion-body text-muted"><ol class="lh-lg"><li>Klasifikasi izin usaha perdagangan sesuai jenis tender.</li><li>Menyediakan layanan purna jual.</li><li>Melampirkan merk dan tipe barang.</li><li>Melampirkan spesifikasi teknis lengkap.</li><li>Melampirkan surat dukungan pabrikan/distributor/agen.</li><li>Surat pernyataan kesediaan layanan garansi.</li><li>Pengalaman tender barang serupa minimum tiga tahun terakhir.</li></ol></div></div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</section>

<section class="container py-5" id="pengumuman">
    <div class="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4">
        <div><span class="badge vendor-badge-soft rounded-pill px-3 py-2 mb-2"><i class="fas fa-bullhorn me-1"></i>Pengumuman Vendor</span><h2 class="vendor-section-title-<%=rnd%> mb-1">Informasi Proyek Pekerjaan</h2><p class="text-muted mb-0">Hanya menampilkan pengumuman yang diperuntukkan untuk vendor/penyedia.</p></div>
        <button class="btn btn-outline-primary rounded-pill fw-bold" onclick="vendorLoadPengumuman(1)"><i class="fas fa-sync-alt me-2"></i>Muat Ulang</button>
    </div>
    <div id="vendorPengumumanContainer" class="row g-4"><div class="col-12 text-center py-5"><div class="spinner-border text-primary"></div><div class="small text-muted mt-2">Memuat pengumuman...</div></div></div>
</section>

<section id="kontak" class="py-5 bg-white border-top">
    <div class="container">
        <div class="row g-4">
            <div class="col-lg-5">
                <h2 class="vendor-section-title-<%=rnd%>">Kontak Layanan Vendor</h2>
                <p class="text-muted">Hubungi kami untuk bantuan pendaftaran vendor, klarifikasi dokumen, atau informasi proyek pekerjaan.</p>
            </div>
            <div class="col-lg-7">
                <div class="row g-3">
                    <div class="col-md-4"><div class="vendor-soft-card-<%=rnd%> p-4 h-100"><i class="fas fa-map-marker-alt text-primary fs-4 mb-3"></i><h6 class="fw-bold">Alamat</h6><div class="small text-muted"><%=vb(alamat)%></div></div></div>
                    <div class="col-md-4"><div class="vendor-soft-card-<%=rnd%> p-4 h-100"><i class="fas fa-phone-alt text-primary fs-4 mb-3"></i><h6 class="fw-bold">Telepon</h6><div class="small text-muted"><%=vb(telp)%></div></div></div>
                    <div class="col-md-4"><div class="vendor-soft-card-<%=rnd%> p-4 h-100"><i class="far fa-envelope text-primary fs-4 mb-3"></i><h6 class="fw-bold">Email</h6><div class="small text-muted"><%=vb(email)%></div></div></div>
                </div>
            </div>
        </div>
    </div>
</section>

<style>
    #vendorRegisterModal .modal-dialog { max-width: 980px; }
    #vendorRegisterModal .modal-content { border-radius: 1.35rem; overflow: hidden; }
    #vendorRegisterModal .modal-body { max-height: calc(100vh - 210px); overflow-y: auto; }
    #vendorRegisterModal .vendor-popup-summary { background: linear-gradient(135deg, rgba(15,23,42,.05), rgba(37,99,235,.08)); border: 1px solid rgba(148,163,184,.22); }
    @media (max-width: 767.98px) {
        #vendorRegisterModal .modal-dialog { margin: .75rem; }
        #vendorRegisterModal .modal-body { max-height: calc(100vh - 170px); padding: 1rem !important; }
    }
</style>

<div class="modal fade" id="vendorRegisterModal" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
    <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable modal-xl">
        <div class="modal-content border-0 shadow-lg">
            <div class="modal-header py-3 px-4">
                <div class="d-flex align-items-center gap-3">
                    <span class="d-inline-flex align-items-center justify-content-center rounded-4 bg-primary bg-opacity-10 text-primary" style="width:46px;height:46px;"><i class="fas fa-user-plus"></i></span>
                    <div>
                        <h5 class="modal-title fw-bold mb-0">Form Pendaftaran Vendor / Penyedia Baru</h5>
                        <small class="text-white-50">Lengkapi data awal. Sistem akan membuat akun dan mengirimkan akses ke email vendor.</small>
                    </div>
                </div>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Tutup"></button>
            </div>
            <form id="formDaftarVendor<%=rnd%>">
                <div class="modal-body p-4 p-lg-5 bg-light">
                    <div class="vendor-popup-summary rounded-4 p-3 p-lg-4 mb-4">
                        <div class="row g-3 align-items-center">
                            <div class="col-lg-8">
                                <div class="d-flex gap-3 align-items-start">
                                    <i class="fas fa-circle-info text-primary fs-4 mt-1"></i>
                                    <div>
                                        <h6 class="fw-bold mb-1">Informasi penting pendaftaran</h6>
                                        <p class="text-muted small mb-0">Satu alamat email hanya dapat dipakai untuk satu vendor. Setelah formulir dikirim, sistem otomatis membuat username dan password, kemudian mengirimkan informasi akses ke email kontak vendor. Jika email sudah pernah terdaftar, gunakan fitur <strong>Kirim Ulang Akses Vendor</strong>.</p>
                                    </div>
                                </div>
                            </div>
                            <div class="col-lg-4">
                                <div class="d-flex flex-column gap-2 small">
                                    <span><i class="fas fa-check-circle text-success me-2"></i>Data masuk ke ERP</span>
                                    <span><i class="fas fa-lock text-success me-2"></i>Akun dibuat otomatis</span>
                                    <span><i class="fas fa-envelope text-success me-2"></i>Email akses dikirim ke vendor</span>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="row g-3">
                        <div class="col-md-6">
                            <label class="form-label vendor-required fw-semibold"><%= Common.getBahasaConfig("Nama Perusahaan / Vendor") %></label>
                            <div class="input-group input-group-lg">
                                <span class="input-group-text bg-white"><i class="fas fa-building text-primary"></i></span>
                                <input type="text" name="nama" class="form-control" required placeholder="Contoh: PT Rekanan Sejahtera">
                            </div>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label vendor-required fw-semibold"><%= Common.getBahasaConfig("Nama Kontak Vendor") %></label>
                            <div class="input-group input-group-lg">
                                <span class="input-group-text bg-white"><i class="fas fa-user-tie text-primary"></i></span>
                                <input type="text" name="kontak" class="form-control" required placeholder="Nama PIC yang dapat dihubungi">
                            </div>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label vendor-required fw-semibold"><%= Common.getBahasaConfig("Email Kontak Vendor") %></label>
                            <div class="input-group input-group-lg">
                                <span class="input-group-text bg-white"><i class="far fa-envelope text-primary"></i></span>
                                <input type="email" name="email" class="form-control" required placeholder="vendor@email.com">
                            </div>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold"><%= Common.getBahasaConfig("No. HP / Telepon") %></label>
                            <div class="input-group input-group-lg">
                                <span class="input-group-text bg-white"><i class="fas fa-phone-alt text-primary"></i></span>
                                <input type="text" name="telp" class="form-control" placeholder="08xxxxxxxxxx">
                            </div>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label vendor-required fw-semibold">NPWP</label>
                            <div class="input-group input-group-lg">
                                <span class="input-group-text bg-white"><i class="fas fa-id-card text-primary"></i></span>
                                <input type="text" name="npwp" class="form-control" required placeholder="Nomor NPWP perusahaan (wajib diisi)">
                            </div>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold"><%= Common.getBahasaConfig("Bidang Usaha") %></label>
                            <div class="input-group input-group-lg">
                                <span class="input-group-text bg-white"><i class="fas fa-briefcase text-primary"></i></span>
                                <select name="bidang" class="form-select">
                                    <option value="Umum">Umum</option>
                                    <option value="Konstruksi">Konstruksi</option>
                                    <option value="Pengadaan Barang">Pengadaan Barang</option>
                                    <option value="Jasa">Jasa</option>
                                </select>
                            </div>
                        </div>
                        <div class="col-12">
                            <label class="form-label vendor-required fw-semibold"><%= Common.getBahasaConfig("Isi Pesan") %></label>
                            <textarea name="pesan" class="form-control rounded-3" rows="5" required placeholder="Tuliskan profil singkat perusahaan, bidang usaha, pengalaman, dan minat kerja sama..."></textarea>
                        </div>
                    </div>
                </div>
                <div class="modal-footer bg-white px-4 py-3 d-flex flex-wrap gap-2 justify-content-between">
                    <div class="small text-muted"><i class="fas fa-shield-alt text-primary me-1"></i>Data pendaftaran akan diverifikasi oleh admin pengadaan.</div>
                    <div class="d-flex flex-wrap gap-2">
                        <button type="reset" class="btn btn-light rounded-pill px-4 fw-bold"><i class="fas fa-undo me-2"></i>Reset</button>
                        <button type="button" class="btn btn-outline-secondary rounded-pill px-4 fw-bold" data-bs-dismiss="modal"><i class="fas fa-times me-2"></i>Tutup</button>
                        <button type="submit" class="btn vendor-btn-gradient-<%=rnd%> rounded-pill px-4 fw-bold"><i class="fas fa-paper-plane me-2"></i>Kirim Pendaftaran</button>
                    </div>
                </div>
            </form>
        </div>
    </div>
</div>

<div class="modal fade" id="vendorLoginModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 rounded-4 shadow-lg">
            <div class="modal-header"><h5 class="modal-title fw-bold"><i class="fas fa-sign-in-alt me-2"></i>Login Vendor</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
            <div class="modal-body p-4">
                <form id="vendorLoginForm<%=rnd%>">
                    <div class="mb-3"><label class="form-label fw-semibold"><%= Common.getBahasaConfig("Email / Username") %></label><input name="username" type="text" class="form-control form-control-lg rounded-3" required></div>
                    <div class="mb-3"><label class="form-label fw-semibold"><%= Common.getBahasaConfig("Password") %></label><input name="password" type="password" class="form-control form-control-lg rounded-3" required></div>
                    <button type="submit" class="btn vendor-btn-gradient-<%=rnd%> rounded-pill w-100 py-3 fw-bold"><i class="fas fa-lock-open me-2"></i>Masuk</button>
                </form>
            </div>
        </div>
    </div>
</div>

<script>
function vendorOpenLoginModal(){ new bootstrap.Modal(document.getElementById('vendorLoginModal')).show(); }
function vendorOpenRegisterModal(){ new bootstrap.Modal(document.getElementById('vendorRegisterModal')).show(); }
function vendorHtmlEscape(s){ return (s || '').replace(/[&<>'"]/g, function(c){ return {'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;',"'":'&#39;'}[c]; }); }
async function vendorLoadPengumuman(page){
    var c = document.getElementById('vendorPengumumanContainer');
    c.innerHTML = '<div class="col-12 text-center py-5"><div class="spinner-border text-primary"></div><div class="small text-muted mt-2">Memuat pengumuman...</div></div>';
    try{
        var res = await fetch(window.vendorServiceUrl + '&action=list_pengumuman&page=' + page);
        var json = await res.json();
        if(json.status !== 'success') throw new Error(json.message || 'Gagal');
        if(!json.data || json.data.length === 0){ c.innerHTML = '<div class="col-12"><div class="alert alert-info rounded-4"><i class="fas fa-info-circle me-2"></i>Belum ada pengumuman khusus vendor yang ditampilkan.</div></div>'; return; }
        var html = '';
        json.data.forEach(function(p){ html += '<div class="col-md-6 col-lg-4"><div class="vendor-soft-card-<%=rnd%> p-4 h-100"><span class="badge vendor-badge-soft rounded-pill mb-3"><i class="far fa-calendar-alt me-1"></i>'+vendorHtmlEscape(p.tanggal)+'</span><h5 class="fw-bold mb-3">'+vendorHtmlEscape(p.judul)+'</h5><p class="text-muted small mb-0">'+vendorHtmlEscape(p.isi)+'</p></div></div>'; });
        c.innerHTML = html;
    }catch(e){ c.innerHTML = '<div class="col-12"><div class="alert alert-danger rounded-4"><i class="fas fa-triangle-exclamation me-2"></i>Gagal memuat pengumuman.</div></div>'; }
}
document.addEventListener('DOMContentLoaded', function(){
    vendorLoadPengumuman(1);
    var f = document.getElementById('formDaftarVendor<%=rnd%>');
    f.addEventListener('submit', async function(e){
        e.preventDefault(); vendorShowLoading();
        try{
            var fd = new FormData(f); fd.append('action','register');
            var res = await fetch(window.vendorServiceUrl, {method:'POST', body:new URLSearchParams(fd)});
            var json = await res.json(); vendorHideLoading();
            if(json.status === 'success'){ vendorToast(json.message || 'Pendaftaran berhasil. Cek email untuk username dan password.', 'success'); f.reset(); var mEl=document.getElementById('vendorRegisterModal'); var m=bootstrap.Modal.getInstance(mEl); if(m) m.hide(); }
            else {
                // EMAIL_REGISTERED & NPWP_REGISTERED sama-sama berarti vendor sudah terdaftar
                // (selaras pencegahan data ganda di modul admin) → arahkan ke "Kirim Ulang Akses".
                var sudahTerdaftar = (json.code === 'EMAIL_REGISTERED' || json.code === 'NPWP_REGISTERED');
                vendorToast(json.message || 'Pendaftaran gagal.', sudahTerdaftar ? 'warning' : 'danger');
                if(sudahTerdaftar){
                    var resendEmail = document.querySelector('#formKirimUlangAkses<%=rnd%> input[name="email"]');
                    var daftarEmail = f.querySelector('input[name="email"]');
                    if(resendEmail && daftarEmail) resendEmail.value = daftarEmail.value;
                    var mEl=document.getElementById('vendorRegisterModal'); var m=bootstrap.Modal.getInstance(mEl); if(m) m.hide(); setTimeout(function(){ var el = document.getElementById('kirim-ulang-akses'); if(el) el.scrollIntoView({behavior:'smooth', block:'start'}); }, 400);
                }
            }
        }catch(err){ vendorHideLoading(); vendorToast('Terjadi kesalahan jaringan.', 'danger'); }
    });
    var resendForm = document.getElementById('formKirimUlangAkses<%=rnd%>');
    if(resendForm){
        resendForm.addEventListener('submit', async function(e){
            e.preventDefault(); vendorShowLoading();
            try{
                var fd = new FormData(resendForm); fd.append('action','resend_credentials');
                var res = await fetch(window.vendorServiceUrl, {method:'POST', body:new URLSearchParams(fd)});
                var json = await res.json(); vendorHideLoading();
                if(json.status === 'success'){ vendorToast(json.message || 'Informasi akun berhasil dikirim ulang.', 'success'); resendForm.reset(); }
                else vendorToast(json.message || 'Gagal mengirim ulang akses vendor.', 'danger');
            }catch(err){ vendorHideLoading(); vendorToast('Terjadi kesalahan jaringan.', 'danger'); }
        });
    }
    document.getElementById('vendorLoginForm<%=rnd%>').addEventListener('submit', async function(e){
        e.preventDefault(); vendorShowLoading();
        try{
            var fd = new FormData(this); fd.append('action','login');
            var res = await fetch(window.vendorServiceUrl, {method:'POST', body:new URLSearchParams(fd)});
            var json = await res.json(); vendorHideLoading();
            if(json.status === 'success'){ vendorToast('Login berhasil.', 'success'); setTimeout(function(){ window.location.href = window.vendorRootUrl; }, 650); }
            else vendorToast(json.message || 'Login gagal.', 'danger');
        }catch(err){ vendorHideLoading(); vendorToast('Terjadi kesalahan jaringan.', 'danger'); }
    });
});
</script>
