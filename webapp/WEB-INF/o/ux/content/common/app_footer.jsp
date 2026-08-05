<!--begin::Footer-->
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%
int tahun = 2022;
PerguruanTinggi perguruanTinggi  = PerguruanTinggiUtil.getPerguruanTinggi(request);
%>
<div id="kt_app_footer" class="app-footer">
	<!--begin::Footer container-->
	<div
		class="app-container container-fluid d-flex flex-column flex-md-row flex-center flex-md-stack py-3">
		<!--begin::Copyright-->
		<div class="text-dark order-2 order-md-1">
			<span class="text-muted fw-semibold me-1">© <%=tahun %> <script
					type="text/javascript">
				new Date().getFullYear() > <%=tahun %>
						&& document.write("- " + new Date().getFullYear());
			</script> <a href="javascript:void(0);" target="_blank"
				class="text-gray-800 text-hover-primary"><%=perguruanTinggi.getNama()%></a>
		</div>
		<!--end::Copyright-->
		<!--begin::Menu-->
		<ul class="menu menu-gray-600 menu-hover-primary fw-semibold order-1">
			<li class="menu-item"><a href="<%=perguruanTinggi.getWebsite() %>"
				target="_blank" class="menu-link px-2">About</a></li>
		</ul>
		<!--end::Menu-->
	</div>
	<!--end::Footer container-->
</div>
<!--end::Footer-->