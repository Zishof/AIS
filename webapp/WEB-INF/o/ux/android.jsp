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

int tinggi = 650;
%>

<!DOCTYPE html>
<html lang="id">
<jsp:include page="content/common/header.jsp"></jsp:include>
<!--begin::Body-->
<body data-kt-name="metronic" id="kt_app_body"
	data-kt-app-layout="dark-sidebar" data-kt-app-header-fixed="true"
	data-kt-app-sidebar-enabled="true" data-kt-app-sidebar-fixed="true"
	data-kt-app-sidebar-hoverable="true"
	data-kt-app-sidebar-push-header="true"
	data-kt-app-sidebar-push-toolbar="true"
	data-kt-app-sidebar-push-footer="true"
	data-kt-app-toolbar-enabled="true" class="app-default">
	<jsp:include page="content/common/mode.jsp"></jsp:include>
	<!--begin::App-->
	<div class="d-flex flex-column flex-root app-root" id="kt_app_root">
		<!--begin::Page-->
		<div class="app-page flex-column flex-column-fluid" id="kt_app_page">


			<jsp:include page="content/common/header_app.jsp"></jsp:include>


			<!--begin::Wrapper-->
			<div class="app-wrapper flex-column flex-row-fluid"
				id="kt_app_wrapper">


				<jsp:include page="content/common/sidebar.jsp"></jsp:include>


				<!--begin:::Main-->
				<div class="app-main flex-column flex-row-fluid" id="kt_app_main">
					<!--begin::Content wrapper-->
					<div class="d-flex flex-column flex-column-fluid ">

						<!--begin::Content-->
						<div id="kt_app_content" class="app-content flex-column-fluid">


							<jsp:include page="content/common/page_title.jsp">
								<jsp:param value="Profil" name="judul" />
								<jsp:param value="Android" name="subjudul" />
								<jsp:param value="QRCode" name="subjudul1" />
							</jsp:include>


							<!--begin::Content container-->
							<div id="kt_app_content_container"
								class="app-container container-xxl">

								<!--begin::Navbar-->
								<div class="card mb-6 mb-xl-9">
									<div class="card-body pt-9 pb-0">

										<iframe src="<%=request.getContextPath() %>/common/mobile/android_code.zul" width="100%"
											style="border: none;" height="<%=tinggi%>px"></iframe>

									</div>
								</div>
								<!--end::Navbar-->


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

	<jsp:include page="content/common/footer.jsp"></jsp:include>
<jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp"/>
</body>
<!--end::Body-->
</html>