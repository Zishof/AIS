<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.sendRedirect(request.getContextPath() + "/logoff");
	return;
}
%>
<!--begin::sidebar-->
<div id="kt_app_sidebar" class="app-sidebar flex-column"
	data-kt-drawer="true" data-kt-drawer-name="app-sidebar"
	data-kt-drawer-activate="{default: true, lg: false}"
	data-kt-drawer-overlay="true" data-kt-drawer-width="225px"
	data-kt-drawer-direction="start"
	data-kt-drawer-toggle="#kt_app_sidebar_mobile_toggle">
	<jsp:include page="/WEB-INF/o/ux/content/common/logo.jsp"></jsp:include>

	<jsp:include page="/WEB-INF/o/ux/content/common/sidebar_menu.jsp"></jsp:include>

</div>
<!--end::sidebar-->