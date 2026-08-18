
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.database.model.Mahasiswa"%>
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
Mahasiswa mahasiswa = tbmuser.getMahasiswa();
String foto = CommonMedia.getUrlFotoPengguna(tbmuser, 152, 114);

String ta = Common.getCurrentTahunAkademik();
boolean ganjil = Common.isNowSemensterGanjil();
String idsmt = request.getParameter("idsmt");

try {
	if (idsmt == null) {
		idsmt = ta.substring(0, 4) + (ganjil ? "1" : "2");
	}
} catch (Exception e) {
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/o/ux/keuangan/tagihan.jsp:26");
}

//System.out.println("idsmt -> " + idsmt);

Integer semesterPendek = null;
if (idsmt != null && idsmt.endsWith("3")) {
	semesterPendek = Perkuliahan.SEMESTER_PENDEK;
}
if (idsmt != null) {
	ta = (Integer.parseInt(idsmt.substring(0, 4))) + "/" + (Integer.parseInt(idsmt.substring(0, 4)) + 1);
	ganjil = Integer.parseInt(idsmt.substring(4, 5)) == 1;
}

Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), ta, ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP,
		mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
KrsMahasiswa krsMahasiswa = mahasiswa == null ? null : Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null);
request.setAttribute("krsMahasiswa", krsMahasiswa);
request.setAttribute("idsmt", idsmt);

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

						<jsp:include page="/WEB-INF/o/ux/content/common/page_title.jsp">
							<jsp:param value="Keuangan" name="judul" />
							<jsp:param value="Tagihan" name="subjudul" />
							<jsp:param value="Daftar Tagihan" name="subjudul1" />
						</jsp:include>

						<!--begin::Content-->
						<div id="kt_app_content" class="app-content flex-column-fluid">
							<!--begin::Content container-->
							<div id="kt_app_content_container"
								class="app-container container-xxl">
								<!--begin::Layout-->
								<div class="d-flex flex-column flex-xl-row">
									<!--begin::Sidebar-->
									<div
										class="flex-column flex-lg-row-auto w-100 w-xl-250px mb-10">
										<!--begin::Card-->
										<div class="card mb-5 mb-xl-8">
											<!--begin::Card body-->
											<div class="card-body pt-7">
												<!--begin::Summary-->
												<div class="d-flex flex-center flex-column mb-5">
													<!--begin::Avatar-->
													<div class="symbol symbol-150px symbol-circle mb-6">
														<img src="<%=foto%>" alt="image" />
													</div>
													<!--end::Avatar-->

												</div>
												<!--end::Summary-->
												<div class="separator separator-dashed my-3"></div>

												<jsp:include
													page="/WEB-INF/o/ux/content/common/info_mhs_vertical.jsp">
													<jsp:param value="${krsMahasiswa}" name="krsMahasiswa" />
												</jsp:include>


											</div>
											<!--end::Card body-->
										</div>
										<!--end::Card-->
									</div>
									<!--end::Sidebar-->
									<div class="flex-lg-row-fluid ms-lg-8">
										<!--begin:::Tabs-->
										<ul class="nav nav-tabs nav-pills pb-5">
											<!--begin:::Tab item-->
											<li class="nav-item"><a
												class="nav-link text-active-light fw-bold pb-4 pt-4 active"
												data-bs-toggle="tab" href="#kt_ecommerce_customer_overview">Tagihan</a>
											</li>
											<!--end:::Tab item-->
											<!--begin:::Tab item-->
											<li class="nav-item"><a
												class="nav-link text-active-light fw-bold pb-4 pt-4"
												data-bs-toggle="tab" href="#kt_ecommerce_customer_general">Riwayat
													Pembayaran</a></li>
											<!--end:::Tab item-->
										</ul>
										<!--end:::Tabs-->

										<jsp:include page="/WEB-INF/o/ux/content/keuangan/tagihan.jsp">
											<jsp:param value="${krsMahasiswa}" name="krsMahasiswa" />
										</jsp:include>

										<jsp:include
											page="/WEB-INF/o/ux/content/keuangan/riwayat_pembayaran.jsp">
											<jsp:param value="${krsMahasiswa}" name="krsMahasiswa" />
										</jsp:include>

									</div>
									<!--end::Content-->
								</div>
								<!--end::Layout-->
							</div>
							<!--end::Content container-->
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