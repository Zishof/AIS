<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.database.model.sekolah.JadwalPelajaran"%>
<%@page import="ais.database.model.sekolah.Guru"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.List"%>

<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.sendRedirect(request.getContextPath() + "/logoff");
	return;
}
Siswa siswa = tbmuser.getSiswa();

List<JadwalPelajaran> dataJadwals = (List<JadwalPelajaran>) request.getAttribute("dataJadwals");
%>


<!--begin::Col-->
<div class="col-xl-7 mb-5 mb-xl-10">
	<!--begin::Tables widget 14-->
	<div class="card card-flush h-md-100">
		<!--begin::Header-->
		<div class="card-header pt-7">
			<!--begin::Title-->
			<h3 class="card-title align-items-start flex-column">
				<span class="card-label fw-bold text-gray-800">e-Learning</span> <span
					class="text-gray-400 mt-1 fw-semibold fs-6"><%=dataJadwals.size()%>
					Jadwal Aktif</span>
			</h3>
			<!--end::Title-->
			<jsp:include page="/WEB-INF/o/ux/content/index/filter_tahun_akademik.jsp"></jsp:include>
		</div>
		<!--end::Header-->
		<!--begin::Body-->
		<div class="card-body pt-6">
			<!--begin::Table container-->
			<div class="table-responsive">
				<!--begin::Table-->
				<table class="table table-row-dashed align-middle gs-0 gy-3 my-0">
					<!--begin::Table head-->
					<thead>
						<tr class="fs-7 fw-bold text-gray-400 border-bottom-0">
							<th class="p-0 pb-3 min-w-225px text-start"><%= Common.getBahasaConfig("Matapelajaran") %></th>
							<th class="p-0 pb-3 min-w-100px text-end"><%= Common.getBahasaConfig("Detail") %></th>
						</tr>
					</thead>
					<!--end::Table head-->
					<!--begin::Table body-->
					<tbody>
						<%
						for (JadwalPelajaran jumlahJadwal : dataJadwals) {
							Long jadwalId = jumlahJadwal.getId();
							String matapelajaran = jumlahJadwal.getMasaJadwalPelajaran() == null ? ""
							: jumlahJadwal.getMatapelajaran().getNama();
							String taa = jumlahJadwal.getTahunAjaran() + "/"
							+ (jumlahJadwal.getSemester().equals(1) ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

							String fotoDosen = "<i class=\"fad fa-user\"></i>";
							if (jumlahJadwal.getGuru() != null) {
								fotoDosen = "<img alt=\"Foto guru\" src=\""
								+ CommonMedia.getUrlFotoPengguna(new Tbmuser(jumlahJadwal.getGuru()), 152, 114) + "\" />";
							}
						%>
						<tr>
							<td>
								<div class="d-flex align-items-center">
									<div class="symbol me-3 icon-elearning">
										<%=fotoDosen%>
									</div>
									<div class="d-flex justify-content-start flex-column">
										<a
											href="<%=request.getContextPath()%>/pages/ux/elearning_detail.jsp?id=<%=jadwalId%>"
											class="text-gray-800 fw-bold text-hover-primary mb-1 fs-6"><%=matapelajaran%>
											| <%=taa%></a> <span
											class="text-gray-400 fw-semibold d-block fs-7"><%=(jumlahJadwal.getHari() == null ? "" : jumlahJadwal.getHari())%>
											<%=(jumlahJadwal.getWaktuMulai() == null ? "" : "(" + jumlahJadwal.getWaktuMulai())%>
											<%=(jumlahJadwal.getWaktuSelesai() == null ? "" : "s.d " + jumlahJadwal.getWaktuSelesai() + ")")%></span>
										<span class="text-gray-400 fw-semibold d-block fs-7"><%=jumlahJadwal.getGuru() == null ? "" : jumlahJadwal.getGuru().getNama()%></span>
									</div>
								</div>
							</td>


							<td class="text-end"><a
								href="<%=request.getContextPath()%>/pages/ux/elearning_detail.jsp?id=<%=jadwalId%>"
								class="btn btn-sm btn-icon btn-bg-light btn-active-color-primary w-30px h-30px">
									<!--begin::Svg Icon | path: icons/duotune/arrows/arr001.svg-->
									<span class="svg-icon svg-icon-5 svg-icon-gray-700"> <svg
											width="24" height="24" viewBox="0 0 24 24" fill="none"
											xmlns="http://www.w3.org/2000/svg">
																					<path
												d="M14.4 11H3C2.4 11 2 11.4 2 12C2 12.6 2.4 13 3 13H14.4V11Z"
												fill="currentColor" />
																					<path opacity="0.3"
												d="M14.4 20V4L21.7 11.3C22.1 11.7 22.1 12.3 21.7 12.7L14.4 20Z"
												fill="currentColor" />
																				</svg>
								</span> <!--end::Svg Icon-->
							</a></td>
						</tr>
						<%
						}
						%>
					</tbody>
					<!--end::Table body-->
				</table>
			</div>
			<!--end::Table-->
		</div>
		<!--end: Card Body-->
	</div>
	<!--end::Tables widget 14-->
</div>
<!--end::Col-->