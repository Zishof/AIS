<%@page import="ais.database.model.Pertemuan"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%
JSONObject item = (JSONObject) request.getAttribute("item");
Pertemuan pertemuan = (Pertemuan) request.getAttribute("pertemuan");
request.setAttribute("pertemuan", pertemuan);
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
					JSONArray guru = item.isNull("guru") ? null : item.getJSONArray("guru");
					if (dosen != null && dosen.length() > 0) {
						JSONObject dsn = dosen.getJSONObject(0);
						String fotoDosen = dsn.isNull("foto") ? "" : dsn.getString("foto");
						out.println("<img alt=\"Foto\" src=\"" + fotoDosen + "\"/>");
					} else if (guru != null && guru.length() > 0) {
						JSONObject dsn = guru.getJSONObject(0);
						String fotoGuru = dsn.isNull("foto") ? "" : dsn.getString("foto");
						out.println("<img alt=\"Foto\" src=\"" + fotoGuru + "\"/>");
					} else {
						out.println("<i class=\"fad fa-file-user\"></i>");
					}
					%>
				</div>
				<div class="d-flex justify-content-start flex-column">
					<a href="#"
						class="text-gray-800 fw-bold text-hover-primary mb-1 fs-6"><%=item.isNull("topik") || item.getString("topik").trim().isEmpty() ? "{Tidak ada topik}"
		: item.getString("topik")%></a> <span
						class="text-gray-400 fw-semibold d-block fs-7"><%=item.isNull("info") || item.getString("info").trim().isEmpty() ? "{Tidak rincian}" : item.getString("info")%>
						(pert. ke-<%=item.isNull("ke") ? "-" : item.getInt("ke")%>)</span>
				</div>
			</div>

			<!--end::Title-->
		</div>
		<!--end::Wrapper-->
		<!--begin::Action-->
		<div class="m-0">

			<jsp:include
				page="/WEB-INF/o/ux/content/elearning_detail/quick_action.jsp">
				<jsp:param value="${pertemuan}" name="pertemuan" />
			</jsp:include>


		</div>
		<!--end::Action-->
	</div>
	<!--end::Info-->
	<!--begin::Customer-->
	<div class="d-flex flex-stack">
		<!--begin::Name-->
		<span class="text-gray-400 fw-bold"><%=!item.isNull("namaDosen") && !item.getString("namaDosen").trim().isEmpty() ? "Dosen"
		: (!item.isNull("namaGuru") && !item.getString("namaGuru").trim().isEmpty() ? "Guru" : "Dosen")%>
			: <a href="javascript:void(0);"
			class="text-gray-800 text-hover-primary fw-bold"><%=item.isNull("namaDosen") || item.getString("namaDosen").trim().isEmpty()
				? (!item.isNull("namaGuru") && !item.getString("namaGuru").trim().isEmpty() ? item.getString("namaGuru")
						: "{Tidak ada}")
				: item.getString("namaDosen")%></a></span>
		<!--end::Name-->
		<!--begin::Label-->
		<span class="badge badge-light-success"><%=item.isNull("waktu") || item.getString("waktu").trim().isEmpty() ? "{Tidak ada waktu}"
		: item.getString("waktu")%></span>
		<!--end::Label-->
	</div>
	<!--end::Customer-->
</div>
<!--end::Item-->
