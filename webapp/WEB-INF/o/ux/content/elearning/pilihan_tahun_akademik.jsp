
<!--begin::Navbar-->
<%@page import="java.util.Calendar"%>
<%@page import="ais.common.Common"%>
<form action="<%=request.getRequestURI()%>#pilihan_smt" method="post">
	<div class="card mb-6 mb-xl-9">
		<div class="card-body pt-9 pb-0">
			<!--begin::Details-->
			<div class="d-flex flex-wrap flex-sm-nowrap mb-6">

				<select class="form-select form-select-solid" data-control="select2" id="pilihan_smt"
					name="idsmt" data-placeholder="Pilih Semester" onchange="submit();"
					data-allow-clear="true">

					<%
					String ta = Common.getCurrentTahunAkademik();
					boolean ganjil = Common.isNowSemensterGanjil();

					String idsmt = request.getParameter("idsmt");
					boolean sudah = false;
					for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) + 5; i >= ais.ui.util.WaktuUtil.getCalendar()
							.get(Calendar.YEAR) - 15; i--) {
						String v = (i - 1) + "/" + (i);

						String selectGanjil = "";
						String selectGenap = "";
						String selectSP = "";

						if (!sudah) {
							if (idsmt != null && idsmt.equals((i - 1) + "1")) {
						selectGanjil = "selected";
						sudah = true;
							} else if (idsmt != null && idsmt.equals((i - 1) + "2")) {
						selectGenap = "selected";
						sudah = true;
							} else if (idsmt != null && idsmt.equals((i - 1) + "3")) {
						selectSP = "selected";
						sudah = true;
							} else if ((ta.equalsIgnoreCase(v) && ganjil)) {
						selectGanjil = "selected";
						sudah = true;
							} else if ((ta.equalsIgnoreCase(v) && !ganjil)) {
						selectGenap = "selected";
						sudah = true;
							}
						}

						out.println("<option value=\"" + ((i - 1) + "3") + "\" " + selectSP + ">Tahun Akademik " + v
						+ " (Semester Pendek)</option>");

						out.println("<option value=\"" + ((i - 1) + "2") + "\" " + selectGenap + ">Tahun Akademik " + v
						+ " (Semester Genap)</option>");

						out.println("<option value=\"" + ((i - 1) + "1") + "\" " + selectGanjil + ">Tahun Akademik " + v
						+ " (Semester Ganjil)</option>");
					%>


					<%
					}
					%>
				</select>
			</div>

		</div>
	</div>
</form>
<!--end::Navbar-->