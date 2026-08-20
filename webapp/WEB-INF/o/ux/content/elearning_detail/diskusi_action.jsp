<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.common.Common"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.sendRedirect(request.getContextPath() + "/logoff");
	return;
}
Long index = request.getParameter("index") == null ? 0L : Long.parseLong(request.getParameter("index"));
Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, request.getParameter("id"));
request.setAttribute("pertemuan", pertemuan);
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
								<jsp:param value="E-Learning" name="judul" />
								<jsp:param value="Diskusi - ${pertemuan}" name="subjudul" />
							</jsp:include>


							<!--begin::Content container-->
							<div id="kt_app_content_container"
								class="app-container container-xxl">

								<div class="card mb-6 mb-xl-9">
									<div class="card-body pt-9 pb-0">
										<!--begin::Details-->
										<div class="d-flex flex-wrap flex-sm-nowrap mb-6">


											<jsp:include
												page="/WEB-INF/o/ux/content/component/diskusi/diskusi_elearning.jsp">
												<jsp:param value="${pertemuan}" name="pertemuan" />
											</jsp:include>



										</div>
									</div>
								</div>





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
<jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp"/>
</body>
<!--end::Body-->
</html>