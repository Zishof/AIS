<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ page import="ais.common.Common" %>






<!-- row  -->
<div class="row col-md-12">
	<div class="col-md-12" style="width: 91%">


		<!--begin::Container-->
		<div class="container-fluid">

			<div class="card card-primary mt-2 rounded-3">
				<!--begin::Body-->
				<div class="card-body bg-primary rounded-3">
					<div class="row d-flex justify-content-between ">
						<div class="col-lg-3 ">
							<a class="navbar-brand " href="#"><h4 style="color: white">MATEMATIKA
									DASAR</h4></a>

						</div>
						<div class="col-lg-5">
							<a class="navbar-brand" href="#"><h4
									style="color: white; margin-left: 23rem">XI A</h4></a>

						</div>


					</div>
				</div>

			</div>

			<!-- container fluid  -->

			<!--begin::Body-->

              

	
            <!-- /.card -->
	
                </div>

			<div class="col-md-12 p-3">

				<!--begin::Accordion-->


				<div class="card mt-1 mb-4 ">
					<!--begin::Header-->
					<div class="card-header p-2">
						<div class="row d-flex justify-content-between">
							<div class="card-title col-md-3 fw-bold">Daftar Pertemuan</div>
							<!--  search bar-->
							<div class="col-md-3">
								<form class="d-flex me-2" role="search">
									<input class="form-control me-1 " type="search"
										placeholder="Cari Pertemuan" aria-label="Cari">
									<button class="btn btn-outline-success" type="submit"><%= Common.getBahasaConfig("Cari") %></button>
								</form>
							</div>
							<!--  search bar-->

						</div>
					</div>

					<!--begin::Body-->
					
					
					<div class="me-5 d-flex justify-content-end">
						<jsp:include
							page="/WEB-INF/uiux/ux_pages/content/modal2/buat_pertemuan.jsp"></jsp:include>
						<jsp:include
							page="/WEB-INF/uiux/ux_pages/content/modal2/upload_kelas.jsp"></jsp:include>

						<a href="<%=request.getContextPath()%>/main2?page=ruang_kelas"
							type="button" class="btn btn-primary btn-sm mt-3 me-3 "
							data-bs-toggle="modal" data-bs-target="#BuatPertemuan"> Buat
							Pertemuan</a> <a
							href="<%=request.getContextPath()%>/main2?page=ruang_kelas"
							type="button" class="btn btn-danger btn-sm mt-3 me-3 ms-3"
							data-bs-toggle="modal" data-bs-target="#UploadKelas"> Upload
							Kelas</a> <a
							href="<%=request.getContextPath()%>/main2?page=ruang_kelas"
							type="button" class="btn btn-success btn-sm mt-3 ms-3">
							Download Kelas</a>
					</div>

					<div class="card-body mb-3 ">
						<div class="accordion " id="accordionExample">
							<div class="accordion-item">
								<h2 class="accordion-header">
									<button class="accordion-button bg-info-subtle" type="button"
										data-bs-toggle="collapse" data-bs-target="#collapseOne"
										aria-expanded="true" aria-controls="collapseOne">
										
										<span class="info-box-icon"> <svg
														xmlns="http://www.w3.org/2000/svg" width="2rem"
														height="2em" viewBox="0 0 14 14">

																<path fill="currentColor" fill-rule="evenodd"
															d="M12.402 8.976H7.259a2.278 2.278 0 0 0-.193-4.547h-1.68A3.095 3.095 0 0 0 4.609 0h7.793a1.35 1.35 0 0 1 1.348 1.35v6.279c0 .744-.604 1.348-1.348 1.348ZM2.898 4.431a1.848 1.848 0 1 0 0-3.695a1.848 1.848 0 0 0 0 3.695m5.195 2.276c0-.568-.46-1.028-1.027-1.028H2.899a2.65 2.65 0 0 0-2.65 2.65v1.205c0 .532.432.963.964.963h.172l.282 2.61A1 1 0 0 0 2.66 14h.502a1 1 0 0 0 .99-.862l.753-5.404h2.16c.567 0 1.027-.46 1.027-1.027Z"
															clip-rule="evenodd" />

														</svg>
												</span>
										<span class=ms-2> <h5>Pertemuan ke-?</h5> </span></button>
								</h2>
								<div id="collapseOne" class="accordion-collapse collapse "
									data-bs-parent="#accordionExample">
									<div class="accordion-body">

										<div class="row col-md-12">




											<!-- Info Boxes Style 2 -->
											<div class="info-box mb-3 text-bg-primary-subtle">
												
												<div>
													<span class="info-box-number">Matematika Dasar</span> <span
														class="info-box-text">Hari Kamis 25 September 2024
													</span> <span class="info-box-text">pukul 09.00 - 12.00 WIB</span>
													<span class="info-box-text">40 siswa</span>




												</div>
											</div>
											<!-- /.info-box-content -->

											<!--begin::Row-->
											<div class="row">
												<!--begin::Col-->
												<div class="col-lg-2 col-6">
													<!--begin::Small Box Widget 1-->
													<div class="small-box" style="background: #9DBDFF">
														<div class="inner">
															<h3>1</h3>
															<p>Catatan Kelas</p>
														</div>
														<svg class="small-box-icon" fill="currentColor"
															viewBox="0 0 15 15" xmlns="http://www.w3.org/2000/svg"
															viewBox="0 0 15 15">
	<path fill="currentColor"
																d="M17.28 8.72a.75.75 0 0 1 0 1.06l-2 2a.75.75 0 0 1-1.06 0l-1-1a.75.75 0 1 1 1.06-1.06l.47.47l1.47-1.47a.75.75 0 0 1 1.06 0m0 6.56a.75.75 0 1 0-1.06-1.06l-1.47 1.47l-.47-.47a.75.75 0 1 0-1.06 1.06l1 1a.75.75 0 0 0 1.06 0zM7 10.25a.75.75 0 0 1 .75-.75h3.5a.75.75 0 0 1 0 1.5h-3.5a.75.75 0 0 1-.75-.75M7.75 15a.75.75 0 0 0 0 1.5h3.5a.75.75 0 0 0 0-1.5zm8.236-11a2.25 2.25 0 0 0-2.236-2h-3.5a2.25 2.25 0 0 0-2.236 2H6.25A2.25 2.25 0 0 0 4 6.25v13.5A2.25 2.25 0 0 0 6.25 22h11.5A2.25 2.25 0 0 0 20 19.75V6.25A2.25 2.25 0 0 0 17.75 4zM10.25 6.5h3.5c.78 0 1.467-.397 1.871-1h2.129a.75.75 0 0 1 .75.75v13.5a.75.75 0 0 1-.75.75H6.25a.75.75 0 0 1-.75-.75V6.25a.75.75 0 0 1 .75-.75h2.129c.404.603 1.091 1 1.871 1m0-3h3.5a.75.75 0 0 1 0 1.5h-3.5a.75.75 0 0 1 0-1.5" />
</svg>

														<a href="<%=request.getContextPath()%>/main2?page=Catatan"
															class="small-box-footer link-dark link-underline-opacity-0 link-underline-opacity-50-hover">
															Buka </a>




													</div>
													<!--end::Small Box Widget 1-->
												</div>
												<!--end::Col-->
												<div class="col-lg-2 col-6">
													<!--begin::Small Box Widget 2-->
													<div class="small-box text-bg-success">
														<div class="inner">
															<h3>5</h3>
															<p>Tugas Sekolah</p>
														</div>
														<svg class="small-box-icon" fill="currentColor"
															aria-hidden="true" xmlns="http://www.w3.org/2000/svg"
															viewBox="0 0 15 15">
	
		<path fill="currentColor"
																d="m12.594 23.258l-.012.002l-.071.035l-.02.004l-.014-.004l-.071-.036q-.016-.004-.024.006l-.004.01l-.017.428l.005.02l.01.013l.104.074l.015.004l.012-.004l.104-.074l.012-.016l.004-.017l-.017-.427q-.004-.016-.016-.018m.264-.113l-.014.002l-.184.093l-.01.01l-.003.011l.018.43l.005.012l.008.008l.201.092q.019.005.029-.008l.004-.014l-.034-.614q-.005-.019-.02-.022m-.715.002a.02.02 0 0 0-.027.006l-.006.014l-.034.614q.001.018.017.024l.015-.002l.201-.093l.01-.008l.003-.011l.018-.43l-.003-.012l-.01-.01z" />
		<path fill="currentColor"
																d="M7.416 3A5 5 0 0 0 7 5a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2c0-.711-.148-1.388-.416-2H18a2 2 0 0 1 2 2v15a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2zM12 14H9a1 1 0 1 0 0 2h3a1 1 0 1 0 0-2m3-4H9a1 1 0 0 0-.117 1.993L9 12h6a1 1 0 1 0 0-2m-3-8a3 3 0 0 1 2.236 1c.428.478.704 1.093.755 1.772L15 5H9c0-.725.257-1.39.685-1.908L9.764 3c.55-.614 1.348-1 2.236-1" />
	
</svg>

														<a
															href="<%=request.getContextPath()%>/main2?page=tugas_sekolah2"
															class="small-box-footer link-dark link-underline-opacity-0 link-underline-opacity-50-hover">
															Buka </a>
													</div>
													<!--end::Small Box Widget 2-->
												</div>
												<!--end::Col-->
												<div class="col-lg-2 col-6">
													<!--begin::Small Box Widget 3-->
													<div class="small-box text-bg-warning">
														<div class="inner">
															<h3>2</h3>
															<p>Ujian Kelas</p>
														</div>
														<svg class="small-box-icon"
															xmlns="http://www.w3.org/2000/svg" viewBox="0 0 15 15"
															aria-hidden="true" fill="currentColor">
	<path
																d="M3 6.25A3.25 3.25 0 0 1 6.25 3h11.5A3.25 3.25 0 0 1 21 6.25v11.5A3.25 3.25 0 0 1 17.75 21H6.25A3.25 3.25 0 0 1 3 17.75zm9.5 3c0 .414.336.75.75.75h3.5a.75.75 0 0 0 0-1.5h-3.5a.75.75 0 0 0-.75.75m.75 4.75a.75.75 0 1 0 0 1.5h3.5a.75.75 0 1 0 0-1.5zm-2.47-5.22a.75.75 0 1 0-1.06-1.06L8.25 9.19l-.47-.47a.75.75 0 0 0-1.06 1.06l1 1a.75.75 0 0 0 1.06 0zm0 4.44a.75.75 0 0 0-1.06 0l-1.47 1.47l-.47-.47a.75.75 0 0 0-1.06 1.06l1 1a.75.75 0 0 0 1.06 0l2-2a.75.75 0 0 0 0-1.06" />
</svg>
														<!-- <svg class="small-box-icon" fill="currentColor"
																viewBox="0 0 15 15" xmlns="http://www.w3.org/2000/svg"
																aria-hidden="true">
                                    <path
																	d="M6.25 6.375a4.125 4.125 0 118.25 0 4.125 4.125 0 01-8.25 0zM3.25 19.125a7.125 7.125 0 0114.25 0v.003l-.001.119a.75.75 0 01-.363.63 13.067 13.067 0 01-6.761 1.873c-2.472 0-4.786-.684-6.76-1.873a.75.75 0 01-.364-.63l-.001-.122zM19.75 7.5a.75.75 0 00-1.5 0v2.25H16a.75.75 0 000 1.5h2.25v2.25a.75.75 0 001.5 0v-2.25H22a.75.75 0 000-1.5h-2.25V7.5z"></path>
                                </svg> -->
														<a
															href="<%=request.getContextPath()%>/main2?page=Ujian_Kelas"
															class="small-box-footer link-dark link-underline-opacity-0 link-underline-opacity-50-hover">
															Buka </a>
													</div>
													<!--end::Small Box Widget 3-->
												</div>
												<!--end::Col-->
												<div class="col-lg-2 col-6">
													<!--begin::Small Box Widget 4-->
													<div class="small-box text-bg-danger">
														<div class="inner">
															<h3>5</h3>
															<p>Materi Kelas</p>
														</div>

														<svg class="small-box-icon"
															xmlns="http://www.w3.org/2000/svg" fill="currentColor"
															viewBox="0 0 15 15" aria-hidden="true">
	<path
																d="M17.5 4.5c-1.95 0-4.05.4-5.5 1.5c-1.45-1.1-3.55-1.5-5.5-1.5c-1.45 0-2.99.22-4.28.79C1.49 5.62 1 6.33 1 7.14v11.28c0 1.3 1.22 2.26 2.48 1.94c.98-.25 2.02-.36 3.02-.36c1.56 0 3.22.26 4.56.92c.6.3 1.28.3 1.87 0c1.34-.67 3-.92 4.56-.92c1 0 2.04.11 3.02.36c1.26.33 2.48-.63 2.48-1.94V7.14c0-.81-.49-1.52-1.22-1.85c-1.28-.57-2.82-.79-4.27-.79M21 17.23c0 .63-.58 1.09-1.2.98c-.75-.14-1.53-.2-2.3-.2c-1.7 0-4.15.65-5.5 1.5V8c1.35-.85 3.8-1.5 5.5-1.5c.92 0 1.83.09 2.7.28c.46.1.8.51.8.98z" />
	<path fill="currentColor"
																d="M13.98 11.01c-.32 0-.61-.2-.71-.52c-.13-.39.09-.82.48-.94c1.54-.5 3.53-.66 5.36-.45c.41.05.71.42.66.83s-.42.71-.83.66c-1.62-.19-3.39-.04-4.73.39c-.08.01-.16.03-.23.03m0 2.66c-.32 0-.61-.2-.71-.52c-.13-.39.09-.82.48-.94c1.53-.5 3.53-.66 5.36-.45c.41.05.71.42.66.83s-.42.71-.83.66c-1.62-.19-3.39-.04-4.73.39a1 1 0 0 1-.23.03m0 2.66c-.32 0-.61-.2-.71-.52c-.13-.39.09-.82.48-.94c1.53-.5 3.53-.66 5.36-.45c.41.05.71.42.66.83s-.42.7-.83.66c-1.62-.19-3.39-.04-4.73.39a1 1 0 0 1-.23.03" />
</svg>
														<!-- <svg class="small-box-icon" fill="currentColor"
																viewBox="0 0 15 15" xmlns="http://www.w3.org/2000/svg"
																aria-hidden="true">
                                    <path clip-rule="evenodd"
																	fill-rule="evenodd"
																	d="M2.25 13.5a8.25 8.25 0 018.25-8.25.75.75 0 01.75.75v6.75H18a.75.75 0 01.75.75 8.25 8.25 0 01-16.5 0z"></path>
                                    <path clip-rule="evenodd"
																	fill-rule="evenodd"
																	d="M12.75 3a.75.75 0 01.75-.75 8.25 8.25 0 018.25 8.25.75.75 0 01-.75.75h-7.5a.75.75 0 01-.75-.75V3z"></path>
                                </svg> -->

														<a
															href="<%=request.getContextPath()%>/main2?page=materi_kelas"
															class="small-box-footer link-dark link-underline-opacity-0 link-underline-opacity-50-hover">
															Buka </a>

													</div>
													<!--end::Small Box Widget 4-->
												</div>
												<!--end::Col-->
												<div class="col-lg-2 col-6">
													<!--begin::Small Box Widget 4-->
													<div class="small-box" style="background: #FFAD60">
														<div class="inner">
															<h3>3</h3>
															<p>Absensi Kelas</p>
														</div>
														<svg class="small-box-icon" fill="currentColor"
															viewBox="0 0 15 15" xmlns="http://www.w3.org/2000/svg"
															aria-hidden="true">
                                    <path
																d="M6.25 6.375a4.125 4.125 0 118.25 0 4.125 4.125 0 01-8.25 0zM3.25 19.125a7.125 7.125 0 0114.25 0v.003l-.001.119a.75.75 0 01-.363.63 13.067 13.067 0 01-6.761 1.873c-2.472 0-4.786-.684-6.76-1.873a.75.75 0 01-.364-.63l-.001-.122zM19.75 7.5a.75.75 0 00-1.5 0v2.25H16a.75.75 0 000 1.5h2.25v2.25a.75.75 0 001.5 0v-2.25H22a.75.75 0 000-1.5h-2.25V7.5z"></path>
                                </svg>
														<a
															href="<%=request.getContextPath()%>/main2?page=absensi_kelas2"
															class="small-box-footer link-dark link-underline-opacity-0 link-underline-opacity-50-hover">
															Buka </a>

													</div>
													<!--end::Small Box Widget 4-->
												</div>
												<!--end::Col-->
												<div class="col-lg-2 col-6">
													<!--begin::Small Box Widget 4-->
													<div class="small-box " style="background: #7E60BF">
														<div class="inner">
															<h3>1</h3>
															<p>Kelas Online</p>
														</div>

														<svg class="small-box-icon" aria-hidden="true"
															xmlns="http://www.w3.org/2000/svg" fill="currentColor"
															class="bi bi-easel-fill" viewBox="0 0 15 15">
  <path
																d="M8.473.337a.5.5 0 0 0-.946 0L6.954 2H2a1 1 0 0 0-1 1v7a1 1 0 0 0 1 1h1.85l-1.323 3.837a.5.5 0 1 0 .946.326L4.908 11H7.5v2.5a.5.5 0 0 0 1 0V11h2.592l1.435 4.163a.5.5 0 0 0 .946-.326L12.15 11H14a1 1 0 0 0 1-1V3a1 1 0 0 0-1-1H9.046z" />
</svg>
														<!-- 			<svg class="small-box-icon" fill="currentColor"
																viewBox="0 0 15 15" xmlns="http://www.w3.org/2000/svg"
																aria-hidden="true">
                                    <path clip-rule="evenodd"
																	fill-rule="evenodd"
																	d="M2.25 13.5a8.25 8.25 0 018.25-8.25.75.75 0 01.75.75v6.75H18a.75.75 0 01.75.75 8.25 8.25 0 01-16.5 0z"></path>
                                    <path clip-rule="evenodd"
																	fill-rule="evenodd"
																	d="M12.75 3a.75.75 0 01.75-.75 8.25 8.25 0 018.25 8.25.75.75 0 01-.75.75h-7.5a.75.75 0 01-.75-.75V3z"></path>
                                </svg> -->
														<a
															href="<%=request.getContextPath()%>/main2?page=tugas_sekolah"
															class="small-box-footer link-dark link-underline-opacity-0 link-underline-opacity-50-hover">
															Buka </a>

													</div>
													<!--end::Small Box Widget 4-->
												</div>
												<!--end::Col-->
											</div>
											<!--end::Row-->


											<!-- isi menu -->
											<div class="container text-end">
												<div class="row align-items-end"></div>
											</div>


											<!-- isi menu -->
										</div>
										<!-- /.info-box -->
									</div>

								</div>
							</div>
						</div>

						<!--end::Body-->

					</div>
					<!--end::Header-->


				</div>
				<!--end::Accordion-->

			</div>


		</div>
		<!--end::Body-->
	</div>

	<div class="col-lg-1">

		<!-- sidebar 2 -->
		<div
			class="container-fluid position-absolute  mt-0 top-0 end-0 col-md-1 shadow p-1 bg-body-tertiary border-primary"
			style="height: 100%; margin-top: 25%">
			<ul class="nav flex-column p-2 mt-5 me-0">
				<li class="nav-brand bg-primary-subtle rounded border "><h5
						class="nav-link text-dark fs-7 d-flex justify-content-center"
						aria-current="page" href="#">Menu e-learning</h5></li>

				<li class="nav-item "><a class="nav-link" aria-current="page"
					href="#"><svg xmlns="http://www.w3.org/2000/svg" width="16"
							height="16" fill="currentColor" class="bi bi-easel-fill"
							viewBox="0 0 16 16">
  <path
								d="M8.473.337a.5.5 0 0 0-.946 0L6.954 2H2a1 1 0 0 0-1 1v7a1 1 0 0 0 1 1h1.85l-1.323 3.837a.5.5 0 1 0 .946.326L4.908 11H7.5v2.5a.5.5 0 0 0 1 0V11h2.592l1.435 4.163a.5.5 0 0 0 .946-.326L12.15 11H14a1 1 0 0 0 1-1V3a1 1 0 0 0-1-1H9.046z" />
</svg>Kelas</a></li>
				<li class="nav-item"><a class="nav-link" href="#"><svg
							xmlns="http://www.w3.org/2000/svg" width="16" height="16"
							fill="currentColor" class="bi bi-person-badge"
							viewBox="0 0 16 16">
  <path
								d="M6.5 2a.5.5 0 0 0 0 1h3a.5.5 0 0 0 0-1zM11 8a3 3 0 1 1-6 0 3 3 0 0 1 6 0" />
  <path
								d="M4.5 0A2.5 2.5 0 0 0 2 2.5V14a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V2.5A2.5 2.5 0 0 0 11.5 0zM3 2.5A1.5 1.5 0 0 1 4.5 1h7A1.5 1.5 0 0 1 13 2.5v10.795a4.2 4.2 0 0 0-.776-.492C11.392 12.387 10.063 12 8 12s-3.392.387-4.224.803a4.2 4.2 0 0 0-.776.492z" />
</svg>Siswa</a></li>
				<li class="nav-item"><a class="nav-link" href="#"><svg
							xmlns="http://www.w3.org/2000/svg" width="16" height="16"
							fill="currentColor" class="bi bi-book" viewBox="0 0 16 16">
  <path
								d="M1 2.828c.885-.37 2.154-.769 3.388-.893 1.33-.134 2.458.063 3.112.752v9.746c-.935-.53-2.12-.603-3.213-.493-1.18.12-2.37.461-3.287.811zm7.5-.141c.654-.689 1.782-.886 3.112-.752 1.234.124 2.503.523 3.388.893v9.923c-.918-.35-2.107-.692-3.287-.81-1.094-.111-2.278-.039-3.213.492zM8 1.783C7.015.936 5.587.81 4.287.94c-1.514.153-3.042.672-3.994 1.105A.5.5 0 0 0 0 2.5v11a.5.5 0 0 0 .707.455c.882-.4 2.303-.881 3.68-1.02 1.409-.142 2.59.087 3.223.877a.5.5 0 0 0 .78 0c.633-.79 1.814-1.019 3.222-.877 1.378.139 2.8.62 3.681 1.02A.5.5 0 0 0 16 13.5v-11a.5.5 0 0 0-.293-.455c-.952-.433-2.48-.952-3.994-1.105C10.413.809 8.985.936 8 1.783" />
</svg>Tugas</a></li>
				<li class="nav-item "><a class="nav-link" href="#"><svg
							xmlns="http://www.w3.org/2000/svg" width="16" height="16"
							fill="currentColor" class="bi bi-archive-fill"
							viewBox="0 0 16 16">
  <path
								d="M12.643 15C13.979 15 15 13.845 15 12.5V5H1v7.5C1 13.845 2.021 15 3.357 15zM5.5 7h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1 0-1M.8 1a.8.8 0 0 0-.8.8V3a.8.8 0 0 0 .8.8h14.4A.8.8 0 0 0 16 3V1.8a.8.8 0 0 0-.8-.8z" />
</svg>Materi</a></li>
				<li class="nav-item"><a class="nav-link" href="#"><svg
							xmlns="http://www.w3.org/2000/svg" width="16" height="16"
							fill="currentColor" class="bi bi-card-checklist"
							viewBox="0 0 16 16">
  <path
								d="M14.5 3a.5.5 0 0 1 .5.5v9a.5.5 0 0 1-.5.5h-13a.5.5 0 0 1-.5-.5v-9a.5.5 0 0 1 .5-.5zm-13-1A1.5 1.5 0 0 0 0 3.5v9A1.5 1.5 0 0 0 1.5 14h13a1.5 1.5 0 0 0 1.5-1.5v-9A1.5 1.5 0 0 0 14.5 2z" />
  <path
								d="M7 5.5a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5m-1.496-.854a.5.5 0 0 1 0 .708l-1.5 1.5a.5.5 0 0 1-.708 0l-.5-.5a.5.5 0 1 1 .708-.708l.146.147 1.146-1.147a.5.5 0 0 1 .708 0M7 9.5a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5m-1.496-.854a.5.5 0 0 1 0 .708l-1.5 1.5a.5.5 0 0 1-.708 0l-.5-.5a.5.5 0 0 1 .708-.708l.146.147 1.146-1.147a.5.5 0 0 1 .708 0" />
</svg>Ujian</a></li>


			</ul>
		</div>
		<!-- sidebar 2 -->

	</div>
	<!-- row  -->

	<!--end::Container-->




</div>

