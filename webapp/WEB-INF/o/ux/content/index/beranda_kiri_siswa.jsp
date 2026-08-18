
<%@page import="ais.database.model.sekolah.JadwalPelajaran"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.HashSet"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.model.KrsMahasiswa"%>

<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.sendRedirect(request.getContextPath() + "/logoff");
	return;
}

Siswa siswa = tbmuser.getSiswa();

List<JadwalPelajaran> dataJadwals = (List<JadwalPelajaran>) request.getAttribute("dataJadwals");
Set<Long> gurus = new HashSet<Long>();
Set<Long> matpels = new HashSet<Long>();
for (JadwalPelajaran jadwalPelajaran : dataJadwals) {

	Long matapelajaranid = jadwalPelajaran.getId();
	if (jadwalPelajaran.getGuru() != null) {
		gurus.add(jadwalPelajaran.getGuru().getId());
	} 
	
	if (jadwalPelajaran.getGuru2() != null) {
		gurus.add(jadwalPelajaran.getGuru2().getId());
	}
	if (jadwalPelajaran.getGuru3() != null) {
		gurus.add(jadwalPelajaran.getGuru3().getId());
	}
	if (jadwalPelajaran.getGuru4() != null) {
		gurus.add(jadwalPelajaran.getGuru4().getId());
	}
	if (jadwalPelajaran.getGuru5() != null) {
		gurus.add(jadwalPelajaran.getGuru5().getId());
	}
	if (matapelajaranid > 0L) {
		matpels.add(matapelajaranid);
	}
}
%>

<div class="col-xl-5 mb-xl-10">
	<!--begin::Lists Widget 19-->
	<div class="card card-flush h-xl-100">
		<!--begin::Heading-->
		<div
			class="card-header rounded bgi-no-repeat bgi-size-cover bgi-position-y-top bgi-position-x-center align-items-start h-250px"
			style="background-image: url('<%=request.getContextPath()%>/component/assets/media/svg/shapes/top-green.png')"
			data-theme="light">
			<!--begin::Title-->
			<h3 class="card-title align-items-start flex-column text-white pt-15">
				<span class="fw-bold fs-2x mb-3 text-limit-1-row">Halo, <%=tbmuser.getUserNama()%></span>


				<div class="fs-4 text-white">
					<span class="opacity-75"> Anda Mengikuti <%=dataJadwals.size()%>
						Jadwal Pelajaran Sekarang
					</span>
				</div>

			</h3>
			<!--end::Title-->

		</div>
		<!--end::Heading-->
		<!--begin::Body-->
		<div class="card-body mt-n20">
			<!--begin::Stats-->
			<div class="mt-n20 position-relative">
				<%
				if (siswa != null) {
				%>
				<!--begin::Row-->
				<div class="row g-3 g-lg-6">
					<!--begin::Col-->
					<div class="col-6">
						<!--begin::Items-->
						<div class="bg-gray-100 bg-opacity-70 rounded-2 px-6 py-5">
							<!--begin::Symbol-->
							<div class="symbol symbol-30px me-5 mb-8">
								<span class="symbol-label"> <!--begin::Svg Icon | path: icons/duotune/medicine/med005.svg-->
									<span class="svg-icon icon-dashboard"> <i
										class="fad fa-bars"></i>
								</span> <!--end::Svg Icon-->
								</span>
							</div>
							<!--end::Symbol-->
							<!--begin::Stats-->
							<div class="m-0">
								<!--begin::Number-->
								<span
									class="text-gray-700 fw-bolder d-block fs-2qx lh-1 ls-n1 mb-1"><%=Common.numberFormat.get().format(gurus.size())%></span>
								<!--end::Number-->
								<!--begin::Desc-->
								<span class="text-gray-500 fw-semibold fs-6">Guru
									Mengajar</span>
								<!--end::Desc-->
							</div>
							<!--end::Stats-->
						</div>
						<!--end::Items-->
					</div>
					<!--end::Col-->
					<!--begin::Col-->
					<div class="col-6">
						<!--begin::Items-->
						<div class="bg-gray-100 bg-opacity-70 rounded-2 px-6 py-5">
							<!--begin::Symbol-->
							<div class="symbol symbol-30px me-5 mb-8">
								<span class="symbol-label"> <!--begin::Svg Icon | path: icons/duotune/finance/fin001.svg-->
									<span class="svg-icon icon-dashboard"> <i
										class="fad fa-abacus "></i>
								</span> <!--end::Svg Icon-->
								</span>
							</div>
							<!--end::Symbol-->
							<!--begin::Stats-->
							<div class="m-0">
								<!--begin::Number-->
								<span
									class="text-gray-700 fw-bolder d-block fs-2qx lh-1 ls-n1 mb-1"><%=Common.numberFormat.get().format(matpels.size())%></span>
								<!--end::Number-->
								<!--begin::Desc-->
								<span class="text-gray-500 fw-semibold fs-6">Matapelajaran</span>
								<!--end::Desc-->
							</div>
							<!--end::Stats-->
						</div>
						<!--end::Items-->
					</div>
					<!--end::Col-->
					<!--begin::Col-->
					<div class="col-6">
						<!--begin::Items-->
						<div class="bg-gray-100 bg-opacity-70 rounded-2 px-6 py-5">
							<!--begin::Symbol-->
							<div class="symbol symbol-30px me-5 mb-8">
								<span class="symbol-label"> <!--begin::Svg Icon | path: icons/duotune/general/gen020.svg-->
									<span class="svg-icon icon-dashboard"> <i
										class="fad fa-analytics"></i>
								</span> <!--end::Svg Icon-->
								</span>
							</div>
							<!--end::Symbol-->
							<!--begin::Stats-->
							<div class="m-0">
								<!--begin::Number-->
								<span
									class="text-gray-700 fw-bolder d-block fs-2qx lh-1 ls-n1 mb-1"><%=Common.numberFormat.get().format(dataJadwals.size())%></span>
								<!--end::Number-->
								<!--begin::Desc-->
								<span class="text-gray-500 fw-semibold fs-6">Jadwal</span>
								<!--end::Desc-->
							</div>
							<!--end::Stats-->
						</div>
						<!--end::Items-->
					</div>
					<!--end::Col-->
					<!--begin::Col-->
					<div class="col-6">
						<!--begin::Items-->
						<div class="bg-gray-100 bg-opacity-70 rounded-2 px-6 py-5">
							<!--begin::Symbol-->
							<div class="symbol symbol-30px me-5 mb-8">
								<span class="symbol-label"> <!--begin::Svg Icon | path: icons/duotune/general/gen013.svg-->
									<span class="svg-icon icon-dashboard"> <i
										class="fad fa-graduation-cap"></i>
								</span> <!--end::Svg Icon-->
								</span>
							</div>
							<!--end::Symbol-->
							<!--begin::Stats-->
							<div class="m-0">
								<!--begin::Number-->
								<span
									class="text-gray-700 fw-bolder d-block fs-2qx lh-1 ls-n1 mb-1"><%=siswa.getKelas() == null ? "" : siswa.getKelas().getNama()%></span>
								<!--end::Number-->
								<!--begin::Desc-->
								<span class="text-gray-500 fw-semibold fs-6">Kelas</span>
								<!--end::Desc-->
							</div>
							<!--end::Stats-->
						</div>
						<!--end::Items-->
					</div>
					<!--end::Col-->
				</div>
				<!--end::Row-->

				<%
				}
				%>

			</div>
			<!--end::Stats-->
		</div>
		<!--end::Body-->
	</div>
	<!--end::Lists Widget 19-->
</div>