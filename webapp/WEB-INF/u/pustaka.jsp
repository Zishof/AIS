<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
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
    <title>Perpustakaan | <%=judul%></title>
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Font Awesome -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css"
        integrity="sha512-9usAa10IRO0HhonpyAIVpjrylPvoDwiPUiKdWk5t3PyolY1cOd4DSE0Ga+ri4AuTroPR5aQvXU9xC6qOPnzFeg=="
        crossorigin="anonymous" referrerpolicy="no-referrer" />
    <!-- Custom CSS -->
    <link rel="stylesheet" href="<%=request.getContextPath() %>/css/pustaka8.css">
    <link rel="shortcut icon" href="<%=request.getContextPath() %>/img/logo.png" type="image/png"/>
</head>

<body>
    <!-- Header -->
    <header class="bg-primary text-white py-3">
        <div class="container">
            <div class="row align-items-center">
                <div class="col-md-1">
                    <img src="<%=request.getContextPath() %>/img/logo.png" alt="Logo Perpustakaan" class="img-fluid" height="50px">
                </div>
                <div class="col-md-11">
                    <h1>Perpustakaan <%=judul%></h1>
                </div>
            </div>
        </div>
    </header>

    <!-- Search Bar -->
    <section class="bg-light py-3">
        <div class="container">
            <div class="input-group">
                <input type="text" class="form-control" placeholder="Cari berdasarkan judul, pengarang, penerbit, klasifikasi"
                    id="searchInput">
                <button class="btn btn-primary" type="button" id="searchButton">
                    <i class="fas fa-search"></i> Cari
                </button>
            </div>
        </div>
    </section>

    <!-- Book Catalog -->
    <section class="py-5">
        <div class="container">
            <div class="row" id="bookList">
                <!-- Book items will be dynamically added here -->
            </div>
            <!-- Pagination -->
            <nav aria-label="Page navigation">
                <ul class="pagination justify-content-center" id="pagination">
                </ul>
            </nav>
        </div>
    </section>

    <!-- Book Detail Modal -->
    <div class="modal fade" id="bookDetailModal" tabindex="-1" aria-labelledby="bookDetailModalLabel"
        aria-hidden="true">
        <div class="modal-dialog modal-xl">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="bookDetailModalLabel">Detail Buku</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body" id="bookDetailContent">
                    <!-- Book detail content will be dynamically added here -->
                </div>
            </div>
        </div>
    </div>

    <!-- Footer -->
    <footer class="bg-dark text-white py-4">
        <div class="container text-center">
            <p>
                &copy; Perpustakaan <%=judul%>, <%=Alamat1%>
                <br>
                <i class="fas fa-phone"></i> <%=Telepon%> |
                <i class="fas fa-envelope"></i> <%=Email%>
            </p>
        </div>
    </footer>

    <!-- Bootstrap JS Bundle -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
    <!-- Custom JavaScript -->
    <script type="text/javascript">
		const books = <%=request.getAttribute("books")%>
	</script>
    <script src="<%=request.getContextPath() %>/js/pustaka8.js"></script>
</body>

</html>