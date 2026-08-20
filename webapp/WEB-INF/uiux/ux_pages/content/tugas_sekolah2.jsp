<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Insert title here</title>
</head>
<body>

	<div class="row">



		<div class="col-md-12">

			<div class="card-header">
				<h3 class="card-title fw-bold">Tugas Sekolah</h3>

			</div>
			<!-- /.card-header -->
			<div class="card-body">
			
			<div class="col-sm-12">
							<div class="me-5 d-flex justify-content-end">
								<a href="<%=request.getContextPath()%>/main2?page=ruang_kelas"
									type="button" class="btn btn-primary btn-sm mt-3 me-3 ">
									Buat Tugas</a> <a
									href="<%=request.getContextPath()%>/main2?page=ruang_kelas"
									type="button" class="btn btn-danger btn-sm mt-3 me-3 ms-3">
									Upload Tugas</a> <a
									href="<%=request.getContextPath()%>/main2?page=ruang_kelas"
									type="button" class="btn btn-success btn-sm mt-3 ms-3">
									Download Tugas</a>
							</div>
						</div>
			
				<!--begin::Row-->
				<div class="row">
					<div class="row row-cols-1 row-cols-md-5 g-4 ms-3 me-3 mt-2 mb-5">

						

						<div class="col">
							<div class="card h-150">
								<img src="<%=request.getContextPath()%>/img/buku-contoh.png"
									class="card-img-top" alt="...">
								<div class="card-body">
									<h4 class="card-title">Judul Tugas</h4>
								</div>
								<div>
									<p class="card-text ms-3 mb-3">keterangan Tugas</p>
									

									<div class="d-grid gap-2 me-2 ms-2 mb-2">
										<a class="btn btn-primary" type="button" href="<%=request.getContextPath()%>/main2?page=tugas_ya">Buka
											Tugas</a>
									</div>


								</div>

							</div>
						</div>

						<div class="col">
							<div class="card h-150">
								<img src="<%=request.getContextPath()%>/img/buku-contoh.png"
									class="card-img-top me-3" alt="...">
								<div class="card-body">
									<h4 class="card-title">Judul Tugas</h4>

								</div>

								<div>
									<p class="card-text ms-3 mb-3">keterangan Tugas</p>


									<div class="d-grid gap-2 me-2 ms-2 mb-2">
										<button class="btn btn-primary" type="Buka Tugas">Buka
											Tugas</button>
									</div>

								</div>

							</div>

						</div>

						<div class="col">
							<div class="card h-150">
								<img src="<%=request.getContextPath()%>/img/buku-contoh.png"
									class="card-img-top" alt="...">
								<div class="card-body">
									<h4 class="card-title">Judul Tugas</h4>

								</div>
								<div>
									<p class="card-text ms-3 mb-3">keterangan Tugas</p>

									<div class="d-grid gap-2 me-2 ms-2 mb-2">
										<button class="btn btn-primary" type="Buka Tugas">Buka
											Tugas</button>
									</div>
								</div>
							</div>
						</div>
						<div class="col">
							<div class="card h-150">
								<img src="<%=request.getContextPath()%>/img/buku-contoh.png"
									class="card-img-top" alt="...">
								<div class="card-body">
									<h4 class="card-title">Judul Tugas</h4>

								</div>
								<div>
									<p class="card-text ms-3 mb-3">keterangan Tugas</p>

									<div class="d-grid gap-2 me-2 ms-2 mb-2">
										<button class="btn btn-primary" type="Buka Tugas">Buka
											Tugas</button>
									</div>



								</div>
							</div>
						</div>

					</div>
				</div>
				<!--end::Row-->
			</div>
			<!-- ./card-body -->





		</div>
	</div>

<jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp"/>
</body>
</html>