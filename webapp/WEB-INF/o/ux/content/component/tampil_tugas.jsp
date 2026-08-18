<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%
JSONObject item = (JSONObject) request.getAttribute("item");
JSONArray tugases = item.getJSONArray("tugas");

for (int j = 0; j < tugases.length(); j++) {
	JSONObject tugas = tugases.getJSONObject(j);
%>

<!--begin::Item-->
<div class="border border-dashed border-gray-300 rounded px-7 py-3 mb-6">
	<!--begin::Info-->
	<div class="d-flex flex-stack mb-3">
		<!--begin::Wrapper-->
		<div class="me-3">
			<div class="d-flex align-items-center">
				<div class="symbol symbol-50px me-3 icon-elearning">



					<%
					JSONArray dosen = item.isNull("dosen") ? null : item.getJSONArray("dosen");
					if (dosen == null || dosen.length() == 0) {
						out.println("<i class=\"fad fa-file-user\"></i>");
					} else {
						JSONObject dsn = dosen.getJSONObject(0);
						String fotoDosen = dsn.isNull("foto") ? "" : dsn.getString("foto");
						out.println("<img alt=\"Foto\" src=\"" + fotoDosen + "\"/>");
					}
					%>
				</div>
				<div class="d-flex justify-content-start flex-column">
					<a href="#"
						class="text-gray-800 fw-bold text-hover-primary mb-1 fs-6"><%=tugas.isNull("judulTugas") || tugas.getString("judulTugas").trim().isEmpty() ? "{Tidak ada judul tugas}"
		: tugas.getString("judulTugas")%></a>

					<%
					String isiTugas = tugas.isNull("isiTugas") || tugas.getString("isiTugas").trim().isEmpty() ? "{Tidak ada isi tugas}"
							: tugas.getString("isiTugas");

					if (isiTugas.length() > 100) {
						isiTugas = isiTugas.substring(0, 100);
					}
					%>

					<span class="text-gray-400 fw-semibold d-block fs-7"><%=isiTugas%></span>
				</div>
			</div>

			<!--end::Title-->
		</div>
		<!--end::Wrapper-->
		<!--begin::Action-->
		<div class="m-0">
			<!--begin::Menu-->
			<button
				class="btn btn-icon btn-color-gray-400 btn-active-color-primary justify-content-end"
				data-kt-menu-trigger="click" data-kt-menu-placement="bottom-end"
				data-kt-menu-overflow="true">
				<!--begin::Svg Icon | path: icons/duotune/general/gen023.svg-->
				<span class="svg-icon svg-icon-1"> <i class="fad fa-bars"></i>
				</span>
				<!--end::Svg Icon-->
			</button>
			<!--begin::Menu 2-->
			<div
				class="menu menu-sub menu-sub-dropdown menu-column menu-rounded menu-gray-800 menu-state-bg-light-primary fw-semibold w-200px"
				data-kt-menu="true">
				<!--begin::Menu item-->
				<div class="menu-item px-3">
					<div class="menu-content fs-6 text-dark fw-bold px-3 py-4">Quick
						Actions</div>
				</div>
				<!--end::Menu item-->
				<!--begin::Menu separator-->
				<div class="separator mb-3 opacity-75"></div>
				<!--end::Menu separator-->
				<!--begin::Menu item-->
				<div class="menu-item px-3">
					<a href="#" class="menu-link px-3">Lihat Tugas</a>
				</div>
				<!--end::Menu item-->
				<!--begin::Menu item-->
				<div class="menu-item px-3">
					<a href="#" class="menu-link px-3">Upload</a>
				</div>
				<!--end::Menu item-->

			</div>
			<!--end::Menu 2-->
			<!--end::Menu-->
		</div>
		<!--end::Action-->
	</div>
	<!--end::Info-->
	<!--begin::Customer-->
	<div class="d-flex flex-stack">
		<!--begin::Name-->
		<span class="text-gray-400 fw-bold">Dosen : <a
			href="javascript:void(0);"
			class="text-gray-800 text-hover-primary fw-bold"><%=item.isNull("namaDosen") || item.getString("namaDosen").trim().isEmpty() ? "{Tidak ada dosen}"
		: item.getString("namaDosen")%></a></span>
		<!--end::Name-->
	</div>
	<!--end::Customer-->

	<!--begin::Customer-->
	<div class="d-flex flex-stack">
		<!--begin::Label-->
		<span class="badge badge-light-success"><%=tugas.isNull("mulai") || tugas.getString("mulai").trim().isEmpty() ? "{Tidak ada waktu mulai}"
		: tugas.getString("mulai")%></span>
		<!--end::Label-->
	</div>
	<!--end::Customer-->
	<!--begin::Customer-->
	<div class="d-flex flex-stack">
		<!--begin::Label-->
		<span class="badge badge-light-success">sd <%=tugas.isNull("sampai") || tugas.getString("sampai").trim().isEmpty() ? "{Tidak ada waktu sampai}"
		: tugas.getString("sampai")%></span>
		<!--end::Label-->
	</div>
	<!--end::Customer-->
</div>
<!--end::Item-->

<%
}
%>

