<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.database.model.KrsMahasiswa"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getMahasiswa() == null) {
	response.sendRedirect(request.getContextPath() + "/logoff");
	return;
}

String ta = Common.getCurrentTahunAkademik();
boolean ganjil = Common.isNowSemensterGanjil();
String idsmt = request.getParameter("idsmt");

if (idsmt != null) {
	ta = (Integer.parseInt(idsmt.substring(0, 4)) - 1) + "/" + (idsmt.substring(0, 4));
	ganjil = Integer.parseInt(idsmt.substring(4, 5)) == 1;
}

Integer semester = Common.getSemester(tbmuser.getMahasiswa().getTahunangkatan(), ta,
		ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP, tbmuser.getMahasiswa().getPindahKeKampusIniMasukSemester(),
		tbmuser.getMahasiswa().getSemesterMulai());

KrsMahasiswa krsMahasiswa = tbmuser.getMahasiswa() == null ? null
		: Common.singkronkanKrsMahasiswa(tbmuser.getMahasiswa(), semester, null, null);
String foto = CommonMedia.getUrlFotoPengguna(tbmuser, 152, 114);

String keteranganMahasiswa = ais.action.master.helper.KrsDetailHelper
		.rubahKeteranganPengambilanKRS(tbmuser.getMahasiswa(), krsMahasiswa.getSemester(),
				krsMahasiswa.getTahapan(), krsMahasiswa.getSemesterPendek(), krsMahasiswa, false);
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
								<%=(tbmuser.ambilJurusan() != null
		? tbmuser.ambilJurusan().getJenjang().getNama() + " " + tbmuser.ambilJurusan().getNama()
		: (tbmuser.ambilSekolah() != null ? tbmuser.ambilSekolah().getNama() : "-"))%></a>
						</div>
						<!--end::Info-->

					</div>
					<!--end::User-->
				</div>
				<!--end::Title-->

				<%
				if (krsMahasiswa != null) {
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

									<div class="fs-2 fw-bold"><%=semester%></div>
								</div>
								<!--end::Number-->
								<!--begin::Label-->
								<div class="fw-semibold fs-6 text-gray-400">Semester</div>
								<!--end::Label-->
							</div>
							<!--end::Stat-->

							<!--begin::Stat-->
							<div
								class="border border-gray-300 border-dashed rounded min-w-125px py-3 px-4 me-6 mb-3">
								<!--begin::Number-->
								<div class="d-flex align-items-center">

									<div class="fs-2 fw-bold"><%=krsMahasiswa.getSksYangDiambil()%></div>
								</div>
								<!--end::Number-->
								<!--begin::Label-->
								<div class="fw-semibold fs-6 text-gray-400">SKS Semester</div>
								<!--end::Label-->
							</div>
							<!--end::Stat-->
							<!--begin::Stat-->
							<div
								class="border border-gray-300 border-dashed rounded min-w-125px py-3 px-4 me-6 mb-3">
								<!--begin::Number-->
								<div class="d-flex align-items-center">

									<div class="fs-2 fw-bold"><%=krsMahasiswa.getSksk()%></div>
								</div>
								<!--end::Number-->
								<!--begin::Label-->
								<div class="fw-semibold fs-6 text-gray-400">SKS Kumulatif</div>
								<!--end::Label-->
							</div>
							<!--end::Stat-->
							<!--begin::Stat-->
							<div
								class="border border-gray-300 border-dashed rounded min-w-125px py-3 px-4 me-6 mb-3">
								<!--begin::Number-->
								<div class="d-flex align-items-center">

									<div class="fs-2 fw-bold"><%=Common.numberFormat.get().format(krsMahasiswa.getIps())%></div>
								</div>
								<!--end::Number-->
								<!--begin::Label-->
								<div class="fw-semibold fs-6 text-gray-400">IP Semester</div>
								<!--end::Label-->
							</div>
							<!--end::Stat-->
							<!--begin::Stat-->
							<div
								class="border border-gray-300 border-dashed rounded min-w-125px py-3 px-4 me-6 mb-3">
								<!--begin::Number-->
								<div class="d-flex align-items-center">

									<div class="fs-2 fw-bold"><%=Common.numberFormat.get().format(krsMahasiswa.getIpk())%></div>
								</div>
								<!--end::Number-->
								<!--begin::Label-->
								<div class="fw-semibold fs-6 text-gray-400">IP Kumulatif</div>
								<!--end::Label-->
							</div>
							<!--end::Stat-->
						</div>
						<!--end::Stats-->
					</div>
					<!--end::Wrapper-->
				</div>
				<!--end::Stats-->


				<!--begin::Info-->
				<div class="d-flex flex-wrap fw-semibold fs-6 mb-4 pe-2">
					<div style='font-size: 11px;'><%=keteranganMahasiswa%></div>
				</div>
				<!--end::Info-->


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
