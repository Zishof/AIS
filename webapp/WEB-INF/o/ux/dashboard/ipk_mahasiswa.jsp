<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);

%>

<!DOCTYPE html>
<html lang="id">
<jsp:include page="/WEB-INF/o/ux/content/common/header.jsp"></jsp:include>
<!--begin::Body-->
<body data-kt-name="metronic" id="kt_app_body"
	data-kt-app-layout="dark-sidebar" data-kt-app-header-fixed="false"
	data-kt-app-sidebar-enabled="true" data-kt-app-sidebar-fixed="false"
	data-kt-app-sidebar-hoverable="true"
	data-kt-app-sidebar-push-header="false"
	data-kt-app-sidebar-push-toolbar="false"
	data-kt-app-sidebar-push-footer="true"
	data-kt-app-toolbar-enabled="false" class="app-default">
	<jsp:include page="/WEB-INF/o/ux/content/common/js.jsp"></jsp:include>
	<jsp:include page="/WEB-INF/o/ux/content/common/mode.jsp"></jsp:include>
	<!--begin::App-->
	<div class="d-flex flex-column flex-root app-root" id="kt_app_root">
		<!--begin::Page-->
		<div class="app-page flex-column flex-column-fluid" id="kt_app_page">

			<!--begin::Wrapper-->
			<div class="app-wrapper flex-column flex-row-fluid"
				id="kt_app_wrapper">




				<!--begin:::Main-->
				<div class="app-main flex-column flex-row-fluid" id="kt_app_main">
					<!--begin::Content wrapper-->
					<div class="d-flex flex-column flex-column-fluid ">


						<!--begin::Content-->
						<div id="kt_app_content" class="app-content flex-column-fluid">
							<!--begin::Content container-->
							<div id="kt_app_content_container"
								class="app-container container-fluid">
								<!--begin::Row-->
								<div class="row g-5 g-xl-10 mb-5 mb-xl-10">


									<jsp:include
										page="/WEB-INF/o/ux/dashboard/content/ipk_mahasiswa.jsp"></jsp:include>

									<jsp:include
										page="/WEB-INF/o/ux/dashboard/content/min_ipk_mahasiswa.jsp"></jsp:include>
										
									<jsp:include
										page="/WEB-INF/o/ux/dashboard/content/max_ipk_mahasiswa.jsp"></jsp:include>
								</div>
								<!--end::Row-->

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

	<jsp:include page="/WEB-INF/o/ux/content/common/footer_tanpa_js.jsp"></jsp:include>
<jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp"/>
</body>
<!--end::Body-->
</html>