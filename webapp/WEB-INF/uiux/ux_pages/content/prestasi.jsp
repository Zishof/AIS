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
		<!--begin::Container-->
		<div class="container-fluid">
			<!--begin::Row-->
			<div class="row">
				<div class="col-sm-6">
					<h3 class="mb-0"><%=Common.getBahasaConfig("Prestasi")%></h3>
				</div>
				<div class="col-sm-6">
					<ol class="breadcrumb float-sm-end">
						<li class="breadcrumb-item"><a
							href="<%=request.getContextPath()%>/main2?page=beranda">Home</a></li>
						<li class="breadcrumb-item active" aria-current="page"><%=Common.getBahasaConfig("Prestasi")%>
						</li>
					</ol>
					<div>
						<main>
							<section class="section container is-fluid">
								<div class="columns">
									<div class="column">
										<p>Vanilla JS Datepicker</p>
										<h1 class="title">Demo</h1>
										<div id="sandbox">
											<div class="field">
												<div class="control">
													<input type="text" class="input date">
												</div>
											</div>
										</div>
										<div id="elem-attribs" class="is-flex is-flex-wrap-wrap mt-2">
											<label><%= Common.getBahasaConfig("Attribute:") %></label>
											<div class="px-1">
												<label class="checkbox mx-1"> <input type="checkbox"
													data-target="input" name="readonly" value="">
													readonly
												</label> <label class="checkbox mx-1"> <input
													type="checkbox" data-target="input" name="disabled"
													value=""> disabled
												</label> <label class="checkbox mx-1" style="display: none;">
													<input type="checkbox" data-target=".date" name="tabindex"
													value="0"> tabindex="0"
												</label>
											</div>
										</div>
									</div>
								</div>
								<div class="columns">
									<div class="column content">
										<p>
											Style: <em>Bulma</em> | <a href="bs5.html">Bootstrap</a> | <a
												href="foundation.html">Foundation</a> | <a
												href="plain-css.html">Plain CSS</a>
										</p>
										<p class="is-size-7">
											* This page uses <a
												href="https://wikiki.github.io/elements/tooltip/"
												target="_blank">bulma-tooltip</a> for tooltips.
										</p>
									</div>
								</div>
							</section>
						</main>
					</div>
				</div>
			</div>
			<!--end::Row-->
		</div>
		<!--end::Container-->
	</div>
	<!--end::App Content Header-->
	<!--begin::App Content-->
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
				<!-- Start col -->

				<div class="col-lg-11">
					<h1>Halo</h1>
				</div>
				<div class="col-sm-1 bg-danger h">
					<ul class="list-group list-group-flush">
						<li class="list-group-item">Menu 1</li>
						<li class="list-group-item">Menu 2</li>
						<li class="list-group-item">Menu 3</li>
					</ul>
				</div>

			</div>
			<!-- /.row (main row) -->


		</div>

		<!--end::Container-->

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
	<!--end::App Content-->
	
	<div class="float-end"><button class="btn btn-primary btn-lg" type="button"
			data-bs-toggle="offcanvas" data-bs-target="#offcanvasRight"
			aria-controls="offcanvasRight"><%= Common.getBahasaConfig("Toggle right offcanvas") %></button></div>
		<br>
</main>
<!--end::App Main-->
