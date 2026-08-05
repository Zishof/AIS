<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.Statusabsensi"%>
<%@page import="java.util.TreeMap"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.database.model.Pertemuan"%>
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
Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();
String info = detailperkuliahan.ambilVOPembelajaran().infoSangatSimple();
KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(detailperkuliahan.getMahasiswa(),
		detailperkuliahan.getSemester(), null, null);
request.setAttribute("detailperkuliahan", detailperkuliahan);
request.setAttribute("krsMahasiswa", krsMahasiswa);
String idsmt = detailperkuliahan.getPerkuliahan().getIdSmt();
TreeMap<String, Long> pertemuans = perkuliahan.ambilPertemuan();

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
												<h3 class="fw-bold">Daftar Kehadiran</h3>
											</div>
											<!--end::Card title-->

										</div>
										<!--begin::Card header-->

										<jsp:include page="/WEB-INF/o/ux/content/common/info_mhs.jsp">
											<jsp:param value="${krsMahasiswa}" name="krsMahasiswa" />
											<jsp:param value="${detailperkuliahan}"
												name="detailperkuliahan" />
										</jsp:include>



										<div class="tab-pane fade show active" id="kt_tab_pane_1"
											role="tabpanel">
											<div class="card-body pt-2">
												<div class="table-responsive">

													<table id=""
														class="table table-striped table-row-bordered gy-5 gs-7 border rounded w-100 align-middle">
														<thead>
															<tr class="fw-semibold fs-6 text-gray-800">
																<th class="min-w-250px"><%= Common.getBahasaConfig("Topik/Pembahasan") %></th>
																<th class="min-w-100px"><%= Common.getBahasaConfig("Kehadiran") %></th>
																<th class="min-w-150px"><%= Common.getBahasaConfig("Keterangan") %></th>
															</tr>
														</thead>
														<tbody>
															<%
															Map<String, Integer> jumlah = new HashMap<String, Integer>();
															for (Long pertemuanid : pertemuans.values()) {
																Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
																if (pertemuan != null) {
																	Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
																	pertemuan.retreiveAbsensiId(detailperkuliahan.getMahasiswa().getId()));
																	if (statusabsensi == null) {
																statusabsensi = ConstantValues.BELUM_ABSEN;
																	}

																	String kode = statusabsensi.getNama();
																	if (!kode.equalsIgnoreCase("-")) {
																if (jumlah.containsKey(kode)) {
																	jumlah.put(kode, jumlah.get(kode) + 1);
																} else {
																	jumlah.put(kode, 1);
																}
																	}

																	String ket = pertemuan.retreiveAbsensiKeterangan(detailperkuliahan.getMahasiswa().getId());
															%>
															<tr>
																<td>Pert.ke-<%=pertemuan.getPertemuanKe()%> <%=pertemuan.getTopik()%></td>
																<td><%=statusabsensi.getNama()%></td>
																<td><%=ket%></td>
															</tr>
															<%
															}
															}
															%>
														</tbody>
														<tfoot>
															<tr class="fw-bold fs-6 border-top align-middle">
																<td colspan="2">Total Kehadiran</td>
																<td><%=jumlah.toString()%></td>

															</tr>

														</tfoot>

													</table>

												</div>
											</div>

										</div>



									</div>
									<!--end::Card body-->
								</div>
							</div>

							<div class="card-footer d-flex justify-content-end py-6 px-9">

								<a
									href="<%=request.getContextPath()%>/pages/ux/absen.jsp?idsmt=<%=idsmt%>"
									class="btn btn-primary">Kembali</a>
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