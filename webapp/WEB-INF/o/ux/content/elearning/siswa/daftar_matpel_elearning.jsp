<%@page import="ais.database.model.sekolah.Matapelajaran"%>
<%@page import="ais.database.model.sekolah.JadwalPelajaran"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
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
%>


<!--begin::Row-->
<div class="row g-5 g-xl-8">

	<%
	for (JadwalPelajaran jadwalPelajaran : dataJadwals) {

		if (jadwalPelajaran != null) {
			Matapelajaran matapelajaran = jadwalPelajaran.getMatapelajaran();
			if (matapelajaran != null) {

		String guru = jadwalPelajaran.getGuru() != null ? jadwalPelajaran.getGuru().getNama() : "";
	%>

	<div class="col-xl-4">
		<!--begin::Statistics Widget 1-->
		<div class="card bgi-no-repeat card-xl-stretch mb-5 mb-xl-8"
			style="background-position: right top; background-size: 30% auto; background-image: url('<%=request.getContextPath()%>/component/assets/media/svg/shapes/abstract-1.svg');">
			<!--begin::Body-->
			<div class="card-body">
				<a href="javascript:void(0)0"
					class="card-title fw-bold text-muted text-hover-primary fs-4"><%=matapelajaran.getNama()%></a>
				<div class="fw-bold text-primary my-6"><%=guru%></div>
				<div class="fw-plain text-gray my-6" style="font-size: 11px"><%=jadwalPelajaran.getDeskripsiPembelajaran()%></div>
				<div class="fw-plain text-gray my-6" style="font-size: 11px"><%=jadwalPelajaran.getCapaianPembelajaranProdi()%></div>

				<div class="row mb-7">
					<!--begin::Label-->
					<label class="col-lg-4 fw-semibold text-muted"><%= Common.getBahasaConfig("Jadwal") %></label>
					<!--end::Label-->
					<!--begin::Col-->
					<div class="col-lg-8 fv-row">
						<span class="fw-semibold text-gray-800 fs-6"> <%=(jadwalPelajaran.getHari() == null ? "" : jadwalPelajaran.getHari())%>
							<%=(jadwalPelajaran.getWaktuMulai() == null ? "" : "(" + jadwalPelajaran.getWaktuMulai())%>
							<%=(jadwalPelajaran.getWaktuSelesai() == null ? "" : "s.d " + jadwalPelajaran.getWaktuSelesai() + ")")%>
						</span>
					</div>
					<!--end::Col-->
				</div>
				<div class="row mb-7">
					<!--begin::Label-->
					<label class="col-lg-4 fw-semibold text-muted"><%= Common.getBahasaConfig("Jumlah Tugas") %></label>
					<!--end::Label-->
					<!--begin::Col-->
					<div class="col-lg-8 fv-row">
						<span class="badge badge-success">0</span>
					</div>
					<!--end::Col-->
				</div>
				<div class="row mb-7">
					<!--begin::Label-->
					<label class="col-lg-4 fw-semibold text-muted"><%= Common.getBahasaConfig("Jumlah Ujian") %></label>
					<!--end::Label-->
					<!--begin::Col-->
					<div class="col-lg-8 fv-row">
						<span class="badge badge-success">0</span>
					</div>
					<!--end::Col-->
				</div>
				<div class="row mb-7">
					<!--begin::Label-->
					<label class="col-lg-4 fw-semibold text-muted"><%= Common.getBahasaConfig("Jumlah Materi") %></label>
					<!--end::Label-->
					<!--begin::Col-->
					<div class="col-lg-8 fv-row">
						<span class="badge badge-success">0</span>
					</div>
					<!--end::Col-->
				</div>

			</div>
			<!--end::Body-->
			<div class="card-footer">
				<a
					href="<%=request.getContextPath()%>/pages/ux/elearning_detail.jsp?id=<%=jadwalPelajaran.getId()%>"
					class="btn btn-light-primary">Masuk</a> <i
					class="fas fa-exclamation-circle text-danger ms-4 fs-3"
					data-bs-toggle="tooltip" title="Ada Tugas yang belum dikumpulkan!"></i>
			</div>
		</div>
		<!--end::Statistics Widget 1-->
	</div>

	<%
	}
	}
	}
	%>

</div>
<!--end::Row-->