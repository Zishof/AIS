<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.database.model.sekolah.JadwalPelajaran"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getSiswa() == null) {
	response.sendRedirect(request.getContextPath() + "/logoff");
	return;
}
Siswa siswa = tbmuser.getSiswa();

List<JadwalPelajaran> dataJadwals = (List<JadwalPelajaran>) request.getAttribute("dataJadwals");
Set<Long> gurus = new HashSet<Long>();
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
}

String foto = CommonMedia.getUrlFotoPengguna(tbmuser, 152, 114);
%>


<!--begin::Navbar-->
<div class="card mb-5 mb-xl-10">
	<div class="card-body pt-9 pb-0">
		<!--begin::Details-->
		<div class="d-flex flex-wrap flex-sm-nowrap mb-3">
			<!--begin: Pic-->
			<div class="me-7 mb-4">
				<img src="<%=foto%>" alt="image" />
			</div>
			<!--end::Pic-->
			<!--begin::Info-->
			<div class="flex-grow-1">
				<!--begin::Title-->
				<div
					class="d-flex justify-content-between align-items-start flex-wrap mb-2">
					<!--begin::User-->
					<div class="d-flex flex-column">
						<!--begin::Name-->
						<div class="d-flex align-items-center mb-2">
							<a href="javascript:void(0)0"
								class="text-gray-900 text-hover-primary fs-2 fw-bold me-1"><%=tbmuser.getUserNama()%></a>
						</div>
						<!--end::Name-->
						<!--begin::Info-->
						<div class="d-flex flex-wrap fw-semibold fs-6 mb-4 pe-2">
							<a href="javascript:void(0)0"
								class="d-flex align-items-center text-gray-400 text-hover-primary me-5 mb-2">
								<%=(tbmuser.ambilSekolah() != null ? tbmuser.ambilSekolah().getNama() + " " + tbmuser.ambilYayasan().getNama() : "")%></a>
						</div>
						<!--end::Info-->

					</div>
					<!--end::User-->
				</div>
				<!--end::Title-->

				<%
				if (siswa != null) {
				%>
				<!--begin::Stats-->
				<div class="d-flex flex-wrap flex-stack">
					<!--begin::Wrapper-->
					<div class="d-flex flex-column flex-grow-1 pe-8">
						<!--begin::Stats-->
						<div class="d-flex flex-wrap">

							<!--begin::Stat-->
							<div
								class="border border-gray-300 border-dashed rounded min-w-125px py-3 px-4 me-6 mb-3">
								<!--begin::Number-->
								<div class="d-flex align-items-center">

									<div class="fs-2 fw-bold"><%=siswa.getKelas() == null ? "" : siswa.getKelas().getNama()%></div>
								</div>
								<!--end::Number-->
								<!--begin::Label-->
								<div class="fw-semibold fs-6 text-gray-400">Kelas</div>
								<!--end::Label-->
							</div>
							<!--end::Stat-->

							<!--begin::Stat-->
							<div
								class="border border-gray-300 border-dashed rounded min-w-125px py-3 px-4 me-6 mb-3">
								<!--begin::Number-->
								<div class="d-flex align-items-center">

									<div class="fs-2 fw-bold"><%=siswa.getKelas() == null ? "" : siswa.getKelas().getTingkat()%></div>
								</div>
								<!--end::Number-->
								<!--begin::Label-->
								<div class="fw-semibold fs-6 text-gray-400">Tingkat</div>
								<!--end::Label-->
							</div>
							<!--end::Stat-->
							<!--begin::Stat-->
							<div
								class="border border-gray-300 border-dashed rounded min-w-125px py-3 px-4 me-6 mb-3">
								<!--begin::Number-->
								<div class="d-flex align-items-center">

									<div class="fs-2 fw-bold"><%=siswa.getKelas() == null || siswa.getKelas().getKurikulumSekolah() == null ? ""
		: siswa.getKelas().getKurikulumSekolah().getNama()%></div>
								</div>
								<!--end::Number-->
								<!--begin::Label-->
								<div class="fw-semibold fs-6 text-gray-400">Kurikulum</div>
								<!--end::Label-->
							</div>
							<!--end::Stat-->
							<!--begin::Stat-->
							<div
								class="border border-gray-300 border-dashed rounded min-w-125px py-3 px-4 me-6 mb-3">
								<!--begin::Number-->
								<div class="d-flex align-items-center">

									<div class="fs-2 fw-bold"><%=Common.numberFormat.get().format(dataJadwals.size())%></div>
								</div>
								<!--end::Number-->
								<!--begin::Label-->
								<div class="fw-semibold fs-6 text-gray-400">Jadwal
									Pelajaran</div>
								<!--end::Label-->
							</div>
							<!--end::Stat-->
							<!--begin::Stat-->
							<div
								class="border border-gray-300 border-dashed rounded min-w-125px py-3 px-4 me-6 mb-3">
								<!--begin::Number-->
								<div class="d-flex align-items-center">

									<div class="fs-2 fw-bold"><%=Common.numberFormat.get().format(gurus.size())%></div>
								</div>
								<!--end::Number-->
								<!--begin::Label-->
								<div class="fw-semibold fs-6 text-gray-400">Guru</div>
								<!--end::Label-->
							</div>
							<!--end::Stat-->
						</div>
						<!--end::Stats-->
					</div>
					<!--end::Wrapper-->
				</div>
				<!--end::Stats-->




				<%
				}
				%>

			</div>
			<!--end::Info-->
		</div>
		<!--end::Details-->
	</div>

</div>
<!--end::Navbar-->
