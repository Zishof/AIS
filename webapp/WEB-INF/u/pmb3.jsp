<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common" %>
    <%

	String judul = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama();
	String Telepon = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getTelepon();
	String Alamat1 = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getAlamat1();
	String Email = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getEmail();
	String background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "background_perguruanTinggi_");
	String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
	%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sistem Penerimaan Mahasiswa Baru | <%=judul%></title>
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">
    <link rel="stylesheet" href="<%=request.getContextPath() %>/css/pmb3.css">
    <link rel="shortcut icon" href="<%=request.getContextPath() %>/img/logo.png" type="image/png"/>
</head>
<body>
    <div class="background"></div>
    <div class="container">
        <header class="text-center mb-4 header-bg">
        
        	<div class="row align-items-center">
                <div class="col-md-1">
                    <img src="<%=request.getContextPath() %>/img/logo.png" alt="Logo Perpustakaan" class="img-fluid" height="50px">
                </div>
                <div class="col-md-11" style="text-align: left;">
                    <h1>Sistem Penerimaan Mahasiswa Baru</h1>
                    <h3><%=judul%></h3>
                </div>
            </div>
            <div class="header-banner">
                <img src="<%=request.getAttribute("banner")%>" alt="Banner Calon Mahasiswa" class="img-fluid">
            </div>
            <button class="btn btn-primary mt-3" id="loginBtn"><i class="fas fa-sign-in-alt"></i> Login</button>
        </header>

        <div class="modal fade" id="loginModal" tabindex="-1" aria-labelledby="loginModalLabel" aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="loginModalLabel">Form Login</h5>
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                            <span aria-hidden="true">&times;</span>
                        </button>
                    </div>
                    <div class="modal-body">
                        <form>
                            <div class="form-group">
                                <label for="username"><%= Common.getBahasaConfig("No. Registrasi / Email / Nama Lengkap") %></label>
                                <input type="text" class="form-control" id="username" placeholder="Masukkan No. Registrasi, Email, atau Nama Lengkap">
                                <small class="text-muted">Username Anda adalah Nomor Pendaftaran, Email, atau Nama Lengkap yang terdaftar.</small>
                            </div>
                            <div class="form-group">
                                <label for="password"><%= Common.getBahasaConfig("Tanggal Lahir (Kata Sandi)") %></label>
                                <input type="text" class="form-control" id="password" placeholder="Contoh: 2000-01-15">
                                <small class="text-muted">Kata sandi Anda adalah Tanggal Lahir dengan format YYYY-MM-DD.</small>
                            </div>
                            <button type="submit" class="btn btn-primary"><%= Common.getBahasaConfig("Login") %></button>
                        </form>
                    </div>
                </div>
            </div>
        </div>

        <main>
            <ul class="nav nav-tabs" id="myTab" role="tablist">
                <li class="nav-item">
                    <a class="nav-link active" id="pengumuman-tab" data-toggle="tab" href="#pengumuman" role="tab" aria-controls="pengumuman" aria-selected="true"><i class="fas fa-bullhorn"></i> Pengumuman</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" id="pendaftaran-tab" data-toggle="tab" href="#pendaftaran" role="tab" aria-controls="pendaftaran" aria-selected="false"><i class="fas fa-list-alt"></i> Jalur Pendaftaran</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" id="prodi-tab" data-toggle="tab" href="#prodi" role="tab" aria-controls="prodi" aria-selected="false"><i class="fas fa-graduation-cap"></i> Informasi Prodi</a>
                </li>
            </ul>
            <div class="tab-content mt-4" id="myTabContent">
                <div class="tab-pane fade show active" id="pengumuman" role="tabpanel" aria-labelledby="pengumuman-tab">
                    <div class="input-group mb-3">
                        <input type="text" class="form-control" id="searchPengumuman" placeholder="Cari pengumuman..." aria-label="Cari pengumuman">
                        <div class="input-group-append">
                            <button class="btn btn-outline" type="button" id="btnSearchPengumuman"><i class="fas fa-search"></i> Cari</button>
                        </div>
                    </div>
                    <div id="listPengumuman">
                        </div>
                    <nav aria-label="Pengumuman Paging">
                        <ul class="pagination justify-content-center" id="pagingPengumuman">
                            </ul>
                    </nav>
                </div>
                <div class="tab-pane fade" id="pendaftaran" role="tabpanel" aria-labelledby="pendaftaran-tab">
                    <div class="input-group mb-3">
                        <input type="text" class="form-control" id="searchPendaftaran" placeholder="Cari jalur pendaftaran..." aria-label="Cari jalur pendaftaran">
                        <div class="input-group-append">
                            <button class="btn btn-outline" type="button" id="btnSearchPendaftaran"><i class="fas fa-search"></i> Cari</button>
                        </div>
                    </div>
                    <div id="listPendaftaran">
                        </div>
                    <nav aria-label="Pendaftaran Paging">
                        <ul class="pagination justify-content-center" id="pagingPendaftaran">
                            </ul>
                    </nav>
                </div>
                <div class="tab-pane fade" id="prodi" role="tabpanel" aria-labelledby="prodi-tab">
                    <div class="input-group mb-3">
                        <input type="text" class="form-control" id="searchProdi" placeholder="Cari informasi prodi..." aria-label="Cari informasi prodi">
                        <div class="input-group-append">
                            <button class="btn btn-outline" type="button" id="btnSearchProdi"><i class="fas fa-search"></i> Cari</button>
                        </div>
                    </div>
                    <div id="listProdi">
                        </div>
                    <nav aria-label="Prodi Paging">
                        <ul class="pagination justify-content-center" id="pagingProdi">
                            </ul>
                    </nav>
                </div>
            </div>
        </main>

        <footer class="mt-5 text-center footer-bg">
            <hr>
            <p>
                &copy; Sistem Penerimaan Mahasiswa Baru 
                <br><strong><%=judul%></strong> 
                <br><i class="fas fa-map-marker-alt"></i> <%=Alamat1%>
                <br><i class="fas fa-phone"></i> <%=Telepon%> 
                <br><i class="fas fa-envelope"></i> <%=Email%>
            </p>
        </footer>
    </div>

    <div class="modal fade" id="detailPengumumanModal" tabindex="-1" aria-labelledby="detailPengumumanModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-lg modal-dialog-scrollable">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="detailPengumumanModalLabel">Detail Pengumuman</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <div class="row">
                        <div class="col-md-4">
                            <img id="pengumumanDetailCover" src="https://via.placeholder.com/300" alt="Cover Pengumuman" class="img-fluid rounded mb-3">
                        </div>
                        <div class="col-md-8">
                            <h4 id="pengumumanDetailTitle"></h4>
                            <p id="pengumumanDetailContent"></p>
                            <p id="pengumumanDetailLink"></p>
                        </div>
                    </div>
                    <hr>
                    <h5>Forum Tanya Jawab</h5>
                    <div id="forumPengumuman">
                        </div>
                    <button class="btn btn-success btn-sm mt-2" id="btnAddCommentPengumuman"><i class="fas fa-plus"></i> Tambah Komentar</button>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="addCommentPengumumanModal" tabindex="-1" aria-labelledby="addCommentPengumumanModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="addCommentPengumumanModalLabel">Tambah Komentar</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <div class="form-group">
                        <label for="newComment"><%= Common.getBahasaConfig("Komentar Anda") %></label>
                        <textarea class="form-control" id="newComment" rows="3"></textarea>
                    </div>
                    <button type="button" class="btn btn-primary" id="btnSaveCommentPengumuman"><i class="fas fa-save"></i> Simpan Komentar</button>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="detailPendaftaranModal" tabindex="-1" aria-labelledby="detailPendaftaranModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-lg modal-dialog-scrollable">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="detailPendaftaranModalLabel">Detail Jalur Pendaftaran</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <div class="row">
                        <div class="col-md-4">
                            <img id="pendaftaranDetailCover" src="https://via.placeholder.com/300" alt="Cover Jalur Pendaftaran" class="img-fluid rounded mb-3">
                        </div>
                        <div class="col-md-8">
                            <h4 id="pendaftaranDetailNama"></h4>
                            <p id="pendaftaranDetailInfo"></p>
                            <p id="pendaftaranDetailBrosur"></p>
                            <p><strong>Biaya Pendaftaran:</strong> <span id="pendaftaranDetailBiaya"></span></p>
                            <button class="btn btn-success"><i class="fas fa-check-circle"></i> Daftar Sekarang</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="detailProdiModal" tabindex="-1" aria-labelledby="detailProdiModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-lg modal-dialog-scrollable">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="detailProdiModalLabel">Detail Informasi Prodi</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <div class="row">
                        <div class="col-md-4">
                            <img id="prodiDetailCover" src="https://via.placeholder.com/300" alt="Cover Informasi Prodi" class="img-fluid rounded mb-3">
                        </div>
                        <div class="col-md-8">
                            <h4 id="prodiDetailNama"></h4>
                            <p id="prodiDetailDeskripsi"></p>
                            <p id="prodiDetailLink"></p>
                            <hr>
                            <p id="prodiDetailInfoRinci"></p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://code.jquery.com/jquery-3.5.1.slim.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/@popperjs/core@2.5.3/dist/umd/popper.min.js"></script>
    <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/js/bootstrap.min.js"></script>
    <script type="text/javascript">
		const pengumumanData = <%=request.getAttribute("pengumumanData")%>
		const pendaftaranData = <%=request.getAttribute("jalurData")%>
		const prodiData = <%=request.getAttribute("prodiData")%>
		const forumData = <%=request.getAttribute("forumData")%>
	</script>
    <script src="<%=request.getContextPath() %>/js/pmb3.js"></script>
</body>
</html>