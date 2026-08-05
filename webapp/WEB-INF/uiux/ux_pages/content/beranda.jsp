<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
ais.database.model.sekolah.Sekolah sekolah = ais.action.master.sekolah.util.SekolahUtil.getSekolah(request);
String judul = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama();
String Telepon = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getTelepon();
String Alamat1 = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getAlamat1();
String Email = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getEmail();
String background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request,
		"background_perguruanTinggi_");
String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request,
		"logo_perguruanTinggi_");
Tbmuser tbmuser = Common.getCurrentUser(request);
String namaPengguna = tbmuser == null ? "" : tbmuser.getUserNama();
String jenisPenguna = tbmuser == null || tbmuser.hakAkses() == null ? "" : tbmuser.hakAkses().getRoleName();
String urlFoto = tbmuser == null ? "" : CommonMedia.getUrlFotoPengguna(tbmuser);
String linkFoto = (urlFoto == null || urlFoto.isEmpty()
		? request.getContextPath() + "/component/adminlte/assets/img/user2-160x160.jpg"
		: urlFoto);
%>
					
<main class="app-main">
	<!--begin::App Content Header-->
	<div class="app-content-header">
		
	</div>
	<!--end::App Content Header-->
	<!--begin::App Content-->
	<div class="app-content">
		<!--begin::Container-->
		<div class="app-content">
		<!--begin::Container-->
		<div class="container-fluid">
			<!--begin::Row-->
			<div class="row">
				<!--begin::Col-->

			</div>
			<!--end::Row-->
			<!--begin::Row-->
			<div class="row">
			
			
			
<!-- buat chattingan -->
		<div class="offcanvas offcanvas-md offcanvas-end" tabindex="-1"
			id="offcanvasRight" aria-labelledby="offcanvasRightLabel">

			<div class="offcanvas-body">





				<!-- DIRECT CHAT -->
				<div class="card direct-chat card direct-chat-primary mt-3 mb-4">
					<div class="card-header">
						<h3 class="card-title">Direct Chat</h3>
						<div class="card-tools">
							<span title="3 New Messages" class="badge text-bg-primary">
								3 </span>

							<button type="button" class="btn btn-tool" title="Contacts"
								data-lte-toggle="chat-panel">
								<i class="bi bi-chat-text-fill"></i>
							</button>

						</div>
					</div>
					<!-- /.card-header -->
					<div class="card-body">
						<!-- Conversations are loaded here -->
						<div class="direct-chat-messages" style="height: 37rem">
							<!-- Message. Default to the start -->
							<div class="direct-chat-msg">
								<div class="direct-chat-infos clearfix">
									<span class="direct-chat-name float-start"> Alexander
										Pierce </span> <span class="direct-chat-timestamp float-end">
										23 Jan 2:00 pm </span>
								</div>
								<!-- /.direct-chat-infos -->
								<img class="direct-chat-img"
									src="<%=request.getContextPath()%>/component/adminlte/assets/img/user1-128x128.jpg"
									alt="message user image">
								<!-- /.direct-chat-img -->
								<div class="direct-chat-text">Is this template really for
									free? That's unbelievable!</div>
								<!-- /.direct-chat-text -->
							</div>
							<!-- /.direct-chat-msg -->
							<!-- Message to the end -->
							<div class="direct-chat-msg end">
								<div class="direct-chat-infos clearfix">
									<span class="direct-chat-name float-end"> Sarah Bullock
									</span> <span class="direct-chat-timestamp float-start"> 23 Jan
										2:05 pm </span>
								</div>
								<!-- /.direct-chat-infos -->
								<img class="direct-chat-img"
									src="<%=request.getContextPath()%>/component/adminlte/assets/img/user3-128x128.jpg"
									alt="message user image">
								<!-- /.direct-chat-img -->
								<div class="direct-chat-text">You better believe it!</div>
								<!-- /.direct-chat-text -->
							</div>
							<!-- /.direct-chat-msg -->
							<!-- Message. Default to the start -->
							<div class="direct-chat-msg">
								<div class="direct-chat-infos clearfix">
									<span class="direct-chat-name float-start"> Alexander
										Pierce </span> <span class="direct-chat-timestamp float-end">
										23 Jan 5:37 pm </span>
								</div>
								<!-- /.direct-chat-infos -->
								<img class="direct-chat-img"
									src="<%=request.getContextPath()%>/component/adminlte/assets/img/user1-128x128.jpg"
									alt="message user image">
								<!-- /.direct-chat-img -->
								<div class="direct-chat-text">Working with AdminLTE on a
									great new app! Wanna join?</div>
								<!-- /.direct-chat-text -->
							</div>
							<!-- /.direct-chat-msg -->
							<!-- Message to the end -->
							<div class="direct-chat-msg end">
								<div class="direct-chat-infos clearfix">
									<span class="direct-chat-name float-end"> Sarah Bullock
									</span> <span class="direct-chat-timestamp float-start"> 23 Jan
										6:10 pm </span>
								</div>
								<!-- /.direct-chat-infos -->
								<img class="direct-chat-img"
									src="<%=request.getContextPath()%>/component/adminlte/assets/img/user3-128x128.jpg"
									alt="message user image">
								<!-- /.direct-chat-img -->
								<div class="direct-chat-text">I would love to.</div>
								<!-- /.direct-chat-text -->
							</div>
							<!-- /.direct-chat-msg -->
						</div>
						<!-- /.direct-chat-messages-->
						<!-- Contacts are loaded here -->
						<div class="direct-chat-contacts">
							<ul class="contacts-list">
								<li><a href="#"> <img class="contacts-list-img"
										src="<%=request.getContextPath()%>/component/adminlte/assets/img/user1-128x128.jpg"
										alt="User Avatar">
										<div class="contacts-list-info">
											<span class="contacts-list-name"> Count Dracula <small
												class="contacts-list-date float-end"> 2/28/2023 </small>
											</span> <span class="contacts-list-msg"> How have you been? I
												was... </span>
										</div> <!-- /.contacts-list-info -->
								</a></li>
								<!-- End Contact Item -->
								<li><a href="#"> <img class="contacts-list-img"
										src="<%=request.getContextPath()%>/component/adminlte/assets/img/user7-128x128.jpg"
										alt="User Avatar">
										<div class="contacts-list-info">
											<span class="contacts-list-name"> Sarah Doe <small
												class="contacts-list-date float-end"> 2/23/2023 </small>
											</span> <span class="contacts-list-msg"> I will be waiting
												for... </span>
										</div> <!-- /.contacts-list-info -->
								</a></li>
								<!-- End Contact Item -->
								<li><a href="#"> <img class="contacts-list-img"
										src="<%=request.getContextPath()%>/component/adminlte/assets/img/user3-128x128.jpg"
										alt="User Avatar">
										<div class="contacts-list-info">
											<span class="contacts-list-name"> Nadia Jolie <small
												class="contacts-list-date float-end"> 2/20/2023 </small>
											</span> <span class="contacts-list-msg"> I'll call you back
												at... </span>
										</div> <!-- /.contacts-list-info -->
								</a></li>
								<!-- End Contact Item -->
								<li><a href="#"> <img class="contacts-list-img"
										src="<%=request.getContextPath()%>/component/adminlte/assets/img/user5-128x128.jpg"
										alt="User Avatar">
										<div class="contacts-list-info">
											<span class="contacts-list-name"> Nora S. Vans <small
												class="contacts-list-date float-end"> 2/10/2023 </small>
											</span> <span class="contacts-list-msg"> Where is your new...
											</span>
										</div> <!-- /.contacts-list-info -->
								</a></li>
								<!-- End Contact Item -->
								<li><a href="#"> <img class="contacts-list-img"
										src="<%=request.getContextPath()%>/component/adminlte/assets/img/user6-128x128.jpg"
										alt="User Avatar">
										<div class="contacts-list-info">
											<span class="contacts-list-name"> John K. <small
												class="contacts-list-date float-end"> 1/27/2023 </small>
											</span> <span class="contacts-list-msg"> Can I take a look
												at... </span>
										</div> <!-- /.contacts-list-info -->
								</a></li>
								<!-- End Contact Item -->
								<li><a href="#"> <img class="contacts-list-img"
										src="<%=request.getContextPath()%>/component/adminlte/assets/img/user8-128x128.jpg"
										alt="User Avatar">
										<div class="contacts-list-info">
											<span class="contacts-list-name"> Kenneth M. <small
												class="contacts-list-date float-end"> 1/4/2023 </small>
											</span> <span class="contacts-list-msg"> Never mind I
												found... </span>
										</div> <!-- /.contacts-list-info -->
								</a></li>
								<!-- End Contact Item -->
							</ul>
							<!-- /.contacts-list -->
						</div>
						<!-- /.direct-chat-pane -->
					</div>
					<!-- /.card-body -->
					<div class="card-footer">
						<form action="#" method="post">
							<div class="input-group">
								<input type="text" name="message" placeholder="Type Message ..."
									class="form-control"> <span class="input-group-append">
									<button type="button" class="btn btn-primary"><%= Common.getBahasaConfig("Send") %></button>
								</span>
							</div>
						</form>
					</div>
					<!-- /.card-footer-->
				</div>
				<!-- /.direct-chat -->



			</div>
		</div>
	</div>
	
	<!-- buat chatingan -->
		<div class="container-fluid">
		
			<!--begin::Row-->
			<div class="row">
				<!--begin::Col-->
				<div class="col-lg-6 col-7">
					<!--begin::Small Box Widget 1-->
					<div class="small-box text-bg-primary">
						<div class="inner">
							<h3>eLearning</h3>
							<p>Pembelajaran Online</p>
						</div>
						<svg class="small-box-icon" fill="currentColor"
							viewBox="0 0 16 16" xmlns="http://www.w3.org/2000/svg"
							 aria-hidden="true">
  <path
								d="M8 1.783C7.015.936 5.587.81 4.287.94c-1.514.153-3.042.672-3.994 1.105A.5.5 0 0 0 0 2.5v11a.5.5 0 0 0 .707.455c.882-.4 2.303-.881 3.68-1.02 1.409-.142 2.59.087 3.223.877a.5.5 0 0 0 .78 0c.633-.79 1.814-1.019 3.222-.877 1.378.139 2.8.62 3.681 1.02A.5.5 0 0 0 16 13.5v-11a.5.5 0 0 0-.293-.455c-.952-.433-2.48-.952-3.994-1.105C10.413.809 8.985.936 8 1.783" />
</svg>

						<a href="<%=request.getContextPath()%>/main2?page=e-Learning"
							class="small-box-footer link-light link-underline-opacity-0 link-underline-opacity-50-hover">
							Buka </a>
					</div>
					<!--end::Small Box Widget 1-->
				</div>
				<!--end::Col-->
				<div class="col-lg-2 col-3">
					<!--begin::Small Box Widget 2-->
					<div class="small-box text-bg-success">
						<div class="inner">
							<h3>
								53<sup class="fs-5">%</sup>
							</h3>
							<p>Progress Kelas</p>
						</div>
						<svg class="small-box-icon" fill="currentColor"
							viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"
							aria-hidden="true">
                                    <path
								d="M18.375 2.25c-1.035 0-1.875.84-1.875 1.875v15.75c0 1.035.84 1.875 1.875 1.875h.75c1.035 0 1.875-.84 1.875-1.875V4.125c0-1.036-.84-1.875-1.875-1.875h-.75zM9.75 8.625c0-1.036.84-1.875 1.875-1.875h.75c1.036 0 1.875.84 1.875 1.875v11.25c0 1.035-.84 1.875-1.875 1.875h-.75a1.875 1.875 0 01-1.875-1.875V8.625zM3 13.125c0-1.036.84-1.875 1.875-1.875h.75c1.036 0 1.875.84 1.875 1.875v6.75c0 1.035-.84 1.875-1.875 1.875h-.75A1.875 1.875 0 013 19.875v-6.75z"></path>
                                </svg>
						<a href="#"
							class="small-box-footer link-light link-underline-opacity-0 link-underline-opacity-50-hover">
							Buka <i class="bi bi-link-45deg"></i>
						</a>
					</div>
					<!--end::Small Box Widget 2-->
				</div>
				<!--end::Col-->
				<div class="col-lg-2 col-3">
					<!--begin::Small Box Widget 3-->
					<div class="small-box text-bg-warning">
						<div class="inner">
							<h3>
								44<sup class="fs-5">%</sup>
							</h3>
							<p>Progress Ujian</p>
						</div>
						<svg class="small-box-icon" fill="currentColor"
							viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"
							aria-hidden="true">
                                    <path
								d="M6.25 6.375a4.125 4.125 0 118.25 0 4.125 4.125 0 01-8.25 0zM3.25 19.125a7.125 7.125 0 0114.25 0v.003l-.001.119a.75.75 0 01-.363.63 13.067 13.067 0 01-6.761 1.873c-2.472 0-4.786-.684-6.76-1.873a.75.75 0 01-.364-.63l-.001-.122zM19.75 7.5a.75.75 0 00-1.5 0v2.25H16a.75.75 0 000 1.5h2.25v2.25a.75.75 0 001.5 0v-2.25H22a.75.75 0 000-1.5h-2.25V7.5z"></path>
                                </svg>
						<a href="#"
							class="small-box-footer link-dark link-underline-opacity-0 link-underline-opacity-50-hover">
							Buka <i class="bi bi-link-45deg"></i>
						</a>
					</div>
					<!--end::Small Box Widget 3-->
				</div>
				<!--end::Col-->
				<div class="col-lg-2 col-3">
					<!--begin::Small Box Widget 4-->
					<div class="small-box text-bg-danger">
						<div class="inner">
							<h3>
								65<sup class="fs-5">%</sup>
							</h3>
							<p>Progress Tugas</p>
						</div>
						<svg class="small-box-icon" fill="currentColor"
							viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"
							aria-hidden="true">
                                    <path clip-rule="evenodd"
								fill-rule="evenodd"
								d="M2.25 13.5a8.25 8.25 0 018.25-8.25.75.75 0 01.75.75v6.75H18a.75.75 0 01.75.75 8.25 8.25 0 01-16.5 0z"></path>
                                    <path clip-rule="evenodd"
								fill-rule="evenodd"
								d="M12.75 3a.75.75 0 01.75-.75 8.25 8.25 0 018.25 8.25.75.75 0 01-.75.75h-7.5a.75.75 0 01-.75-.75V3z"></path>
                                </svg>
						<a href="#"
							class="small-box-footer link-light link-underline-opacity-0 link-underline-opacity-50-hover">
							Buka <i class="bi bi-link-45deg"></i>
						</a>
					</div>
					<!--end::Small Box Widget 4-->
				</div>
				<!--end::Col-->
			</div>
			<!--end::Row-->
			<!--begin::Row-->
			<div class="row">
				<!-- Start col -->


				<div class="col-lg-7 connectedSortable">
					<div class="card mb-4">
						<div class="card-header">
							<h3 class="card-title">Monitor Pembelajaran</h3>
						</div>
						<div class="card-body">
							<div id="revenue-chart"></div>
						</div>
					</div>
					<!-- /.card -->
					
				</div>
				<!-- /.Start col -->
				<!-- Start col -->
				<div class="col-lg-5 connectedSortable">
					<div
						class="card text-white bg-primary bg-gradient border-primary mb-4">
						<div class="card-header border-0">
							<h3 class="card-title">Monitor Siswa</h3>
							<div class="card-tools">
								<button type="button" class="btn btn-primary btn-sm"
									data-lte-toggle="card-collapse">
									<i data-lte-icon="expand" class="bi bi-plus-lg"></i> <i
										data-lte-icon="collapse" class="bi bi-dash-lg"></i>
								</button>
							</div>
						</div>
						<div class="card-body">
							<div id="world-map" style="height: 220px"></div>
						</div>
						<div class="card-footer border-0">
							<!--begin::Row-->
							<div class="row">
								<div class="col-4 text-center">
									<div id="sparkline-1" class="text-dark"></div>
									<div class="text-white">Hadir</div>
								</div>
								<div class="col-4 text-center">
									<div id="sparkline-2" class="text-dark"></div>
									<div class="text-white">Izin</div>
								</div>
								<div class="col-4 text-center">
									<div id="sparkline-3" class="text-dark"></div>
									<div class="text-white">Bolos</div>
								</div>
							</div>
							<!--end::Row-->
						</div>
					</div>

				</div>
				<!-- /.Start col -->
			</div>
			<!-- /.row (main row) -->
				
		</div>
		<!--end::Container-->
		<!-- tombol chat -->
			<div class="float-end"><button class="btn btn-primary btn-lg" type="button"
			data-bs-toggle="offcanvas" data-bs-target="#offcanvasRight"
			aria-controls="offcanvasRight">Kirim Pesan<i class="bi bi-chat-dots-fill ms-3"></i></button></div><!-- tombol chat -->
	</div>
	<!--end::App Content-->
</main>
<!--end::App Main-->
