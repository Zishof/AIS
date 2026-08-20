<!DOCTYPE html>
<%@page import="ais.database.model.sekolah.JadwalPelajaran"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.Perkuliahan"%>
<html lang="id">
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.sendRedirect(request.getContextPath() + "/logoff");
	return;
}
Long idData = request.getParameter("id") == null ? null : Long.parseLong(request.getParameter("id"));
Perkuliahan perkuliahan = null;
JadwalPelajaran jadwalPelajaran = null;
if (tbmuser.getSiswa() != null) {
	jadwalPelajaran = (JadwalPelajaran) ConstantValues.ambil(JadwalPelajaran.class.getName(), idData);
} else {
	perkuliahan = (Perkuliahan) (idData == null ? null : ConstantValues.ambil(Perkuliahan.class.getName(), idData));
}
request.setAttribute("perkuliahan", perkuliahan);
request.setAttribute("jadwalPelajaran", jadwalPelajaran);
request.setAttribute("info_data",
		(perkuliahan == null ? (jadwalPelajaran == null ? "" : jadwalPelajaran.infoSangatSimple())
		: perkuliahan.infoSangatSimple()));
%>
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
							<jsp:param value="e-Learning" name="judul" />
							<jsp:param value="${info_data}" name="subjudul" />
						</jsp:include>


						<!--begin::Content-->
						<div id="kt_app_content" class="app-content flex-column-fluid">
							<!--begin::Content container-->
							<div id="kt_app_content_container"
								class="app-container container-xxl">

								<jsp:include
									page="/WEB-INF/o/ux/content/elearning_detail/pertemuan_rinci.jsp">
									<jsp:param value="${perkuliahan}" name="perkuliahan" />
									<jsp:param value="${jadwalPelajaran}" name="jadwalPelajaran" />
								</jsp:include>

							</div>
							<!--end::Content container-->
						</div>
						<!--end::Content-->
					</div>
					<!--end::Content wrapper-->
				</div>
				<!--end:::Main-->


			</div>
			<!--end::Wrapper-->
			<jsp:include page="/WEB-INF/o/ux/content/common/app_footer.jsp"></jsp:include>
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