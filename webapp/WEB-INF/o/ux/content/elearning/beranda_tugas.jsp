<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%
JSONArray data = (JSONArray) request.getAttribute("data");
%>

<!--begin::Col-->
<div class="col-xl-5 mb-xl-10">
	<!--begin::List widget 5-->
	<div class="card card-flush h-xl-100">
		<!--begin::Header-->
		<div class="card-header pt-7">
			<!--begin::Title-->
			<h3 class="card-title align-items-start flex-column">
				<span class="card-label fw-bold text-dark">Materi, Tugas, dan
					Ujian</span> <span class="text-gray-400 mt-1 fw-semibold fs-6">Daftar
					Linimasa Materi, Tugas, dan Ujian</span>
			</h3>
			<!--end::Title-->
		</div>
		<!--end::Header-->

		<!--begin::Body-->
		<div class="card-body">
			<!--begin::Scroll-->
			<div class="hover-scroll-overlay-y pe-6 me-n6">


				<%
				for (int i = 0; i < data.length(); i++) {
					JSONObject item = data.getJSONObject(i);
					request.setAttribute("item", item);
				%>
				
				<jsp:include page="/WEB-INF/o/ux/content/component/tampil_tugas.jsp">
					<jsp:param value="${item}" name="item" />
				</jsp:include>

				<%
				}
				%>
			</div>
			<!--end::Scroll-->
		</div>
		<!--end::Body-->
	</div>
	<!--end::List widget 5-->
</div>
<!--end::Col-->

