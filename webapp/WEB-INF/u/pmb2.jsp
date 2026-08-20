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
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css"
        integrity="sha512-9usAa10IRO0HhonpyAIVpjrylPvoDwiPUiKdWk5t3PyolY1cOd4DSE0Ga+ri4AuTroPR5aQvXU9xC6qOPnzFeg=="
        crossorigin="anonymous" referrerpolicy="no-referrer" />
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="<%=request.getContextPath() %>/css/pmb2.css">
    <link rel="shortcut icon" href="<%=request.getContextPath() %>/img/logo.png" type="image/png"/>
</head>

<body>
    <header class="jumbotron text-center">
        <div class="logo-space">
            <img src="<%=request.getContextPath() %>/img/logo.png" height="50px" alt="Logo Universitas Demo" class="logo">
        </div>
        <h1>Sistem Penerimaan Mahasiswa Baru</h1>
        <h3><%=judul%></h3>
      <div class="btn-group" role="group" aria-label="Basic example">
        <button id="daftarSekarangBtn" class="btn btn-primary mb-3"><%= Common.getBahasaConfig("Daftar Sekarang") %></button>
        <button id="loginBtn" class="btn btn-secondary mb-3 float-end"><i class="fas fa-sign-in-alt"></i> Login</button>
</div>
    </header>

    <div class="container">
      
        <ul class="nav nav-tabs" id="myTab" role="tablist">
            <li class="nav-item" role="presentation">
                <button class="nav-link active" id="pengumuman-tab" data-bs-toggle="tab"
                    data-bs-target="#pengumuman" type="button" role="tab" aria-controls="pengumuman"
                    aria-selected="true"><i class="fas fa-bullhorn"></i> Pengumuman</button>
            </li>
            <li class="nav-item" role="presentation">
                <button class="nav-link" id="jalur-tab" data-bs-toggle="tab" data-bs-target="#jalur" type="button"
                    role="tab" aria-controls="jalur" aria-selected="false"><i class="fas fa-road"></i> Jalur
                    Pendaftaran</button>
            </li>
            <li class="nav-item" role="presentation">
                <button class="nav-link" id="prodi-tab" data-bs-toggle="tab" data-bs-target="#prodi" type="button"
                    role="tab" aria-controls="prodi" aria-selected="false"><i class="fas fa-graduation-cap"></i>
                    Informasi Prodi</button>
            </li>
        </ul>

        <div class="tab-content" id="myTabContent">
            <!-- Pengumuman -->
            <div class="tab-pane fade show active" id="pengumuman" role="tabpanel" aria-labelledby="pengumuman-tab">
                <div class="input-group mb-3">
                    <input type="text" class="form-control" id="cariPengumuman" placeholder="Cari Pengumuman..."
                        aria-label="Cari Pengumuman">
                </div>
                <div id="daftarPengumuman">
                    <!-- Daftar pengumuman akan ditambahkan di sini -->
                </div>
                <nav aria-label="Halaman Pengumuman">
                    <ul class="pagination justify-content-center" id="pengumumanPagination">
                        <!-- Pagination akan ditambahkan di sini -->
                    </ul>
                </nav>
            </div>

            <!-- Jalur Pendaftaran -->
            <div class="tab-pane fade" id="jalur" role="tabpanel" aria-labelledby="jalur-tab">
                <div class="input-group mb-3">
                    <input type="text" class="form-control" id="cariJalur" placeholder="Cari Jalur Pendaftaran..."
                        aria-label="Cari Jalur">
                </div>
                <div id="daftarJalur">
                    <!-- Daftar jalur pendaftaran akan ditambahkan di sini -->
                </div>
                <nav aria-label="Halaman Jalur Pendaftaran">
                    <ul class="pagination justify-content-center" id="jalurPagination">
                        <!-- Pagination akan ditambahkan di sini -->
                    </ul>
                </nav>
            </div>

            <!-- Informasi Prodi -->
            <div class="tab-pane fade" id="prodi" role="tabpanel" aria-labelledby="prodi-tab">
                <div class="input-group mb-3">
                    <input type="text" class="form-control" id="cariProdi" placeholder="Cari Informasi Prodi..."
                        aria-label="Cari Prodi">
                </div>
                <div id="daftarProdi">
                    <!-- Daftar informasi prodi akan ditambahkan di sini -->
                </div>
                <nav aria-label="Halaman Informasi Prodi">
                    <ul class="pagination justify-content-center" id="prodiPagination">
                        <!-- Pagination akan ditambahkan di sini -->
                    </ul>
                </nav>
            </div>
        </div>
    </div>

    <!-- Modal Login -->
    <div class="modal fade" id="loginModal" tabindex="-1" aria-labelledby="loginModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="loginModalLabel">Login</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <form>
                        <div class="mb-3">
                            <label for="username" class="form-label"><%= Common.getBahasaConfig("No. Registrasi / Email / Nama Lengkap") %></label>
                            <input type="text" class="form-control" id="username" placeholder="Masukkan No. Registrasi, Email, atau Nama Lengkap">
                            <small class="text-muted">Username Anda adalah Nomor Pendaftaran, Email, atau Nama Lengkap yang terdaftar.</small>
                        </div>
                        <div class="mb-3">
                            <label for="password" class="form-label"><%= Common.getBahasaConfig("Tanggal Lahir (Kata Sandi)") %></label>
                            <input type="text" class="form-control" id="password" placeholder="Contoh: 2000-01-15">
                            <small class="text-muted">Kata sandi Anda adalah Tanggal Lahir dengan format YYYY-MM-DD.</small>
                        </div>
                        <button type="submit" class="btn btn-primary"><%= Common.getBahasaConfig("Login") %></button>
                    </form>
                </div>
            </div>
        </div>
    </div>

    <!-- Modal Detail Pengumuman -->
    <div class="modal fade" id="detailPengumumanModal" tabindex="-1" aria-labelledby="detailPengumumanModalLabel"
        aria-hidden="true">
        <div class="modal-dialog modal-lg">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="detailPengumumanModalLabel">Detail Pengumuman</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body" id="detailPengumumanContent">
                    <!-- Konten detail pengumuman akan ditambahkan di sini -->
                </div>
            </div>
        </div>
    </div>

    <!-- Modal Detail Jalur -->
    <div class="modal fade" id="detailJalurModal" tabindex="-1" aria-labelledby="detailJalurModalLabel"
        aria-hidden="true">
        <div class="modal-dialog modal-lg">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="detailJalurModalLabel">Detail Jalur Pendaftaran</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body" id="detailJalurContent">
                    <!-- Konten detail jalur pendaftaran akan ditambahkan di sini -->
                </div>
            </div>
        </div>
    </div>

    <!-- Modal Detail Prodi -->
    <div class="modal fade" id="detailProdiModal" tabindex="-1" aria-labelledby="detailProdiModalLabel"
        aria-hidden="true">
        <div class="modal-dialog modal-lg">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="detailProdiModalLabel">Detail Informasi Prodi</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body" id="detailProdiContent">
                    <!-- Konten detail prodi akan ditambahkan di sini -->
                </div>
            </div>
        </div>
    </div>

    <!-- Modal Tambah Komentar -->
    <div class="modal fade" id="tambahKomentarModal" tabindex="-1" aria-labelledby="tambahKomentarModalLabel"
        aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="tambahKomentarModalLabel">Tambah Komentar</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <form id="formTambahKomentar">
                        <div class="mb-3">
                            <label for="namaPengguna" class="form-label"><%= Common.getBahasaConfig("Nama Anda") %></label>
                            <input type="text" class="form-control" id="namaPengguna"
                                placeholder="Masukkan nama Anda">
                        </div>
                        <div class="mb-3">
                            <label for="isiKomentar" class="form-label"><%= Common.getBahasaConfig("Komentar") %></label>
                            <textarea class="form-control" id="isiKomentar" rows="3"
                                placeholder="Masukkan komentar Anda"></textarea>
                        </div>
                        <button type="submit" class="btn btn-primary"><%= Common.getBahasaConfig("Simpan Komentar") %></button>
                    </form>
                </div>
            </div>
        </div>
    </div>

    <footer class="footer mt-5 py-3 text-center">
        <p>
            &copy; Sistem Penerimaan Mahasiswa Baru <%=judul%>
            <br><i class="fas fa-map-marked"></i> <%=Alamat1%>
            <br><i class="fas fa-phone"></i> <%=Telepon%> 
            <br><i class="fas fa-envelope"></i>  <%=Email%>
        </p>
    </footer>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script type="text/javascript">
		const pengumumanData = <%=request.getAttribute("pengumumanData")%>
		const jalurData = <%=request.getAttribute("jalurData")%>
		const prodiData = <%=request.getAttribute("prodiData")%>
	</script>
    <script src="<%=request.getContextPath() %>/js/pmb2.js"></script>
<jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp"/>
</body>

</html>