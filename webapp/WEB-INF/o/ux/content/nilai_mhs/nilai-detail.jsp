<%@page import="ais.database.model.FormatNilai"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Detailperkuliahan"%>
<%@page import="ais.database.model.KrsMahasiswa"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.sendRedirect(request.getContextPath() + "/logoff");
	return;
}
String detailperkuliahanid = request.getParameter("id");
Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
		detailperkuliahanid.toString());
String info = detailperkuliahan.ambilVOPembelajaran().infoSangatSimple();
KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(detailperkuliahan.getMahasiswa(),
		detailperkuliahan.getSemester(), null, null);
request.setAttribute("detailperkuliahan", detailperkuliahan);
request.setAttribute("krsMahasiswa", krsMahasiswa);
String idsmt = detailperkuliahan.getPerkuliahan().getIdSmt();
List<FormatNilai> formatNilais = Common.getFormatNilais(detailperkuliahan.getPerkuliahan());

%>

<!DOCTYPE html>
<html lang="id">
<jsp:include page="/WEB-INF/o/ux/content/common/header.jsp"></jsp:include>
<!--begin::Body-->
<body data-kt-name="metronic" id="kt_app_body"
	data-kt-app-layout="dark-sidebar" data-kt-app-header-fixed="true"
	data-kt-app-sidebar-enabled="true" data-kt-app-sidebar-fixed="true"
	data-kt-app-sidebar-hoverable="true"
	data-kt-app-sidebar-push-header="true"
	data-kt-app-sidebar-push-toolbar="true"
	data-kt-app-sidebar-push-footer="true"
	data-kt-app-toolbar-enabled="true" class="app-default">
	<jsp:include page="/WEB-INF/o/ux/content/common/mode.jsp"></jsp:include>
	<!--begin::App-->
	<div class="d-flex flex-column flex-root app-root" id="kt_app_root">
		<!--begin::Page-->
		<div class="app-page flex-column flex-column-fluid" id="kt_app_page">


			<jsp:include page="/WEB-INF/o/ux/content/common/header_app.jsp"></jsp:include>


			<!--begin::Wrapper-->
			<div class="app-wrapper flex-column flex-row-fluid"
				id="kt_app_wrapper">


				<jsp:include page="/WEB-INF/o/ux/content/common/sidebar.jsp"></jsp:include>


				<!--begin:::Main-->
				<div class="app-main flex-column flex-row-fluid" id="kt_app_main">
					<!--begin::Content wrapper-->
					<div class="d-flex flex-column flex-column-fluid ">

						<!--begin::Content-->
						<div id="kt_app_content" class="app-content flex-column-fluid">


							<jsp:include page="/WEB-INF/o/ux/content/common/page_title.jsp">
								<jsp:param value="Nilai" name="judul" />
								<jsp:param value="Rincian Nilai" name="subjudul" />
								<jsp:param value="<%=info%>" name="subjudul1" />
							</jsp:include>


							<!--begin::Content-->
							<div id="kt_app_content" class="app-content flex-column-fluid">
								<!--begin::Content container-->
								<div id="kt_app_content_container"
									class="app-container container-xxl">
									<!--begin::details View-->
									<div class="card mb-5 mb-xl-10" id="kt_profile_details_view">
										<!--begin::Card header-->
										<div class="card-header">
											<!--begin::Card title-->
											<div class="card-title align-items-start flex-column">
												<h3 class="fw-bold">Daftar Nilai</h3>
											</div>
											<!--end::Card title-->

										</div>
										<!--begin::Card header-->

										<jsp:include page="/WEB-INF/o/ux/content/common/info_mhs.jsp">
											<jsp:param value="${krsMahasiswa}" name="krsMahasiswa" />
											<jsp:param value="${detailperkuliahan}"
												name="detailperkuliahan" />
										</jsp:include>

										<!--begin::Card body-->
										<div class="card-body p-9">



											<div class="hover-scroll-x">
												<div class="d-grid">
													<ul class="nav nav-tabs flex-nowrap text-nowrap">
														<li class="nav-item"><a
															class="nav-link btn btn-active-light btn-color-gray-600 btn-active-color-primary rounded-bottom-0 active"
															data-bs-toggle="tab" href="#kt_tab_pane_1">Nilai
																Total</a></li>
														<li class="nav-item"><a
															class="nav-link btn btn-active-light btn-color-gray-600 btn-active-color-primary rounded-bottom-0"
															data-bs-toggle="tab" href="#kt_tab_pane_2">Nilai
																Tugas</a></li>
														<li class="nav-item"><a
															class="nav-link btn btn-active-light btn-color-gray-600 btn-active-color-primary rounded-bottom-0"
															data-bs-toggle="tab" href="#kt_tab_pane_3">Nilai
																Ujian</a></li>
														<li class="nav-item"><a
															class="nav-link btn btn-active-light btn-color-gray-600 btn-active-color-primary rounded-bottom-0"
															data-bs-toggle="tab" href="#kt_tab_pane_4">Rekap
																Kehadiran</a></li>

													</ul>
												</div>
											</div>

										</div>
										<!--end::Card body-->


										<div class="tab-content" id="myTabContent">
											<div class="tab-pane fade show active" id="kt_tab_pane_1"
												role="tabpanel">
												<div class="card-body pt-2">
													<div class="table-responsive">

														<table id=""
															class="table table-striped table-row-bordered gy-5 gs-7 border rounded w-100 align-middle">
															<thead>
																<tr class="fw-semibold fs-6 text-gray-800">
																	<th class="min-w-150px"><%= Common.getBahasaConfig("Jenis Penilaian") %></th>
																	<th class="min-w-100px"><%= Common.getBahasaConfig("Presentase") %></th>
																	<th class="min-w-100px"><%= Common.getBahasaConfig("Nilai") %></th>
																</tr>
															</thead>
															<tbody>
																<%
																String namaNilai = "";
																for (FormatNilai formatNilai : formatNilais) {
																	String s = formatNilai.getStatusPertemuan().getNama() + " " + Common.numberFormat.get().format(formatNilai.getPersen())
																	+ "%";
																	namaNilai += namaNilai.isEmpty() ? s : " + " + s;
																%>
																<tr>
																	<td><%=formatNilai.getStatusPertemuan().getNama()%></td>
																	<td><%=Common.numberFormat.get().format(formatNilai.getPersen())%>%</td>
																	<td><%=Common.numberFormat.get().format(detailperkuliahan.retreiveDetailNilai(formatNilai))%></td>
																</tr>
																<%
																}
																%>
															</tbody>
															<tfoot>
																<tr class="fw-bold fs-6 border-top align-middle">
																	<td colspan="2">Total Nilai (<%=namaNilai%>)
																	</td>
																	<td><%=Common.numberFormat.get().format(detailperkuliahan.getTotalNilai())%>
																		(<%=detailperkuliahan.getNilaiHuruf()%>)</td>

																</tr>

															</tfoot>

														</table>

													</div>
												</div>

											</div>
										</div>

										<div class="tab-content" id="myTabContent">
											<div class="tab-pane fade" id="kt_tab_pane_2" role="tabpanel">
												<div class="card-body pt-2">
													<div class="table-responsive">

														<table id=""
															class="table table-striped table-row-bordered gy-5 gs-7 border rounded w-100 align-middle">
															<thead>
																<tr class="fw-semibold fs-6 text-gray-800">
																	<th class="min-w-50px"><%= Common.getBahasaConfig("No") %></th>
																	<th class="min-w-100px"><%= Common.getBahasaConfig("Tanggal") %></th>
																	<th class="min-w-100px"><%= Common.getBahasaConfig("Pertemuan ke") %></th>
																	<th class="min-w-150px"><%= Common.getBahasaConfig("Nama Tugas") %></th>
																	<th class="min-w-100px"><%= Common.getBahasaConfig("Nilai") %></th>
																	<th class="min-w-100px"><%= Common.getBahasaConfig("Status") %></th>
																</tr>
															</thead>
															<tbody>
																<tr>
																	<td>1</td>
																	<td>Jumat, 22 Juli 2022</td>
																	<td>15</td>
																	<td>Membuat Makalah ABC</td>
																	<td>0</td>
																	<td><span class="badge badge-warning"
																		data-bs-toggle="tooltip" data-bs-placement="top"
																		title="Tugas Belum di nilai Oleh Dosen">
																			Periksa </span></td>
																</tr>

																<tr>
																	<td>2</td>
																	<td>Jumat, 22 Juli 2022</td>
																	<td>8</td>
																	<td>Tugas Individu</td>
																	<td>60</td>
																	<td><span class="badge badge-danger"
																		data-bs-toggle="tooltip" data-bs-placement="top"
																		title="Silahkan Untuk Segera Memperbaiki Nilai">
																			Remedial </span></td>
																</tr>
																<tr>
																	<td>3</td>
																	<td>Jumat, 22 Juli 2022</td>
																	<td>10</td>
																	<td>Membuat Makalah</td>
																	<td>90</td>
																	<td><span class="badge badge-success"
																		data-bs-toggle="tooltip" data-bs-placement="top"
																		title="Telah Tuntas"> Tuntas </span></td>
																</tr>
															</tbody>


														</table>

													</div>
												</div>
												<!--end::Card body-->
											</div>
										</div>

										<div class="tab-content" id="myTabContent">
											<div class="tab-pane fade" id="kt_tab_pane_3" role="tabpanel">
												<div class="card-body pt-2">
													<div class="table-responsive">

														<table id=""
															class="table table-striped table-row-bordered gy-5 gs-7 border rounded w-100 align-middle">
															<thead>
																<tr class="fw-semibold fs-6 text-gray-800">
																	<th class="min-w-50px"><%= Common.getBahasaConfig("No") %></th>
																	<th class="min-w-100px"><%= Common.getBahasaConfig("Tanggal") %></th>
																	<th class="min-w-150px"><%= Common.getBahasaConfig("Nama Ujian") %></th>
																	<th class="min-w-100px"><%= Common.getBahasaConfig("Nilai") %></th>
																	<th class="min-w-100px"><%= Common.getBahasaConfig("Status") %></th>
																</tr>
															</thead>
															<tbody>

																<tr>
																	<td>1</td>
																	<td>Jumat, 22 Juli 2022</td>
																	<td>UTS</td>
																	<td>90</td>
																	<td><span class="badge badge-success"
																		data-bs-toggle="tooltip" data-bs-placement="top"
																		title="Nilai Telah Tuntas"> Tuntas </span></td>
																</tr>
																<tr>
																	<td>2</td>
																	<td>Jumat, 22 Juli 2022</td>
																	<td>UAS</td>
																	<td>90</td>
																	<td><span class="badge badge-success"
																		data-bs-toggle="tooltip" data-bs-placement="top"
																		title="Nilai Telah Tuntas"> Tuntas </span></td>
																</tr>
															</tbody>

														</table>

													</div>
												</div>
												<!--end::Card body-->
											</div>
										</div>

										<div class="tab-content" id="myTabContent">
											<div class="tab-pane fade" id="kt_tab_pane_4" role="tabpanel">
												<div class="card-body pt-2">
													<div class="table-responsive">

														<table id=""
															class="table table-striped table-row-bordered gy-5 gs-7 border rounded w-100 align-middle">
															<thead>
																<tr class="fw-semibold fs-6 text-gray-800">
																	<th><%= Common.getBahasaConfig("No") %></th>
																	<th class="min-w-150px"><%= Common.getBahasaConfig("Keterangan") %></th>
																	<th class="min-w-150px"><%= Common.getBahasaConfig("Jumlah") %></th>

																</tr>
															</thead>
															<tbody>
																<tr>
																	<td>1</td>
																	<td>Masuk</td>
																	<td>10</td>

																</tr>

																<tr>
																	<td>2</td>
																	<td>Tidak Masuk</td>
																	<td>0</td>

																</tr>

															</tbody>
															<tfoot>
																<tr class="fw-bold fs-6 border-top align-middle">
																	<td colspan="2">Presensi Kehadiran</td>
																	<td>100%</td>

																</tr>

															</tfoot>

														</table>

													</div>
												</div>
												<!--end::Card body-->
											</div>
										</div>

										<div class="card-footer d-flex justify-content-end py-6 px-9">

											<a href="<%=request.getContextPath()%>/pages/ux/nilai.jsp?idsmt=<%=idsmt%>" class="btn btn-primary">Kembali</a>
										</div>





									</div>
									<!--end::details View-->
								</div>
								<!--end::Content container-->
							</div>
							<!--end::Content-->


						</div>
						<!--end::Content-->
					</div>
					<!--end::Content wrapper-->
					<jsp:include page="/WEB-INF/o/ux/content/common/app_footer.jsp"></jsp:include>
				</div>
				<!--end:::Main-->
			</div>
			<!--end::Wrapper-->
		</div>
		<!--end::Page-->
	</div>
	<!--end::App-->


	<!--end::Drawers-->

	<jsp:include page="/WEB-INF/o/ux/content/common/footer.jsp"></jsp:include>
</body>
<!--end::Body-->
</html>