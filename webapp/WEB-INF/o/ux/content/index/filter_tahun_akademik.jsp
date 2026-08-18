<!--begin::Actions-->
<%@page import="java.util.Calendar"%>
<%@page import="ais.common.Common"%>
<form action="" method="post">
	<div class="card-toolbar"> 
		<!--begin::Filters-->
		<div class="d-flex flex-stack flex-wrap gap-4">

			<!--begin::Status-->
			<div class="d-flex align-items-center fw-bold">
				<!--begin::Label-->
				<div class="text-gray-400 fs-7 me-2">TA/Smt</div>
				<!--end::Label-->
				<!--begin::Select-->
				<select name="idsmt" id="idsmt"
					class="form-select form-select-transparent text-dark fs-7 lh-1 fw-bold py-0 ps-3 w-auto"
					data-control="select2" data-hide-search="true"
					data-dropdown-css-class="w-150px"
					onchange="submit();"
					data-placeholder="Select an option"
					data-kt-table-widget-4="filter_status">


					<%
					String ta = Common.getCurrentTahunAkademik();
					boolean ganjil = Common.isNowSemensterGanjil();

					String idsmt = request.getParameter("idsmt");

					for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) + 5; i >= ais.ui.util.WaktuUtil.getCalendar()
							.get(Calendar.YEAR) - 15; i--) {
						String v = (i - 1) + "/" + (i);

						String selectGanjil = "";
						String selectGenap = "";
						String selectSP = "";

						if (idsmt != null && idsmt.equals(i + "1")) {
							selectGanjil = "selected";
						} else if (idsmt != null && idsmt.equals(i + "2")) {
							selectGenap = "selected";
						} else if (idsmt != null && idsmt.equals(i + "3")) {
							selectSP = "selected";
						} else if ((ta.equalsIgnoreCase(v) && ganjil)) {
							selectGanjil = "selected";
						} else if ((ta.equalsIgnoreCase(v) && !ganjil)) {
							selectGenap = "selected";
						}

						out.println("<option value=\"" + (i + "3") + "\" " + selectSP + ">" + v + "/SP</option>");

						out.println("<option value=\"" + (i + "2") + "\" " + selectGenap + ">" + v + "/Genap</option>");

						out.println("<option value=\"" + (i + "1") + "\" " + selectGanjil + ">" + v + "/Ganjil</option>");
					%>


					<%
					}
					%>
				</select>
				<!--end::Select-->
			</div>
			<!--end::Status-->
			<!--begin::Search-->
			<div class="position-relative my-1" style="display: none;">
				<!--begin::Svg Icon | path: icons/duotune/general/gen021.svg-->
				<span
					class="svg-icon svg-icon-2 position-absolute top-50 translate-middle-y ms-4">

				</span>
				<!--end::Svg Icon-->
				<input type="text" data-kt-table-widget-4="search"
					class="form-control w-150px fs-7 ps-12" />
			</div>
			<!--end::Search-->
		</div>
		<!--begin::Filters-->
	</div>
</form>
<!--end::Actions-->