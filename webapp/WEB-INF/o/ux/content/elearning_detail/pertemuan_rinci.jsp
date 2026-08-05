<%@page import="ais.database.model.sekolah.JadwalPelajaran"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="java.util.ArrayList"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="java.util.List"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%
Perkuliahan perkuliahan = (Perkuliahan) request.getAttribute("perkuliahan");
JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) request.getAttribute("jadwalPelajaran");
String dosen = "";
String fotoDosen = "";
if (perkuliahan != null) {
	//perkuliahan.belum();
	List<Dosen> dosens = perkuliahan.populateDosenBuNama();
	for (Dosen dd : dosens) {
		dosen += dosen.isEmpty() ? dd.getNama() : ", " + dd.getNama();
		fotoDosen += "<img alt=\"Foto dosen\" src=\"" + CommonMedia.getUrlFotoPengguna(new Tbmuser(dd), 152, 114)
		+ "\" />";
	}
} else if (jadwalPelajaran != null && jadwalPelajaran.getGuru() != null) {
	dosen += dosen.isEmpty() ? jadwalPelajaran.getGuru().getNama() : ", " + jadwalPelajaran.getGuru().getNama();
	fotoDosen += "<img alt=\"Foto guru\" src=\""
	+ CommonMedia.getUrlFotoPengguna(new Tbmuser(jadwalPelajaran.getGuru()), 152, 114) + "\" />";
}

List<Pertemuan> pertemuans = perkuliahan == null
		? (jadwalPelajaran != null ? jadwalPelajaran.ambilPertemuanList() : new ArrayList<Pertemuan>())
		: perkuliahan.ambilPertemuanList();
%>
<!--begin::details View-->
<div class="card mb-5 mb-xl-10" id="kt_profile_details_view">
	<!--begin::Card header-->
	<div class="card-header cursor-pointer">
		<!--begin::Card title-->
		<div class="card-title m-0">
			<h3 class="fw-bold m-0"><%=perkuliahan == null || perkuliahan.getMatakuliah() == null
		? (jadwalPelajaran == null || jadwalPelajaran.getMatapelajaran() == null ? ""
				: jadwalPelajaran.getMatapelajaran().getNama())
		: perkuliahan.getMatakuliah().getKode() + " | " + perkuliahan.getMatakuliah().getNama()%></h3>
		</div>
		<!--end::Card title-->
	</div>
	<!--begin::Card header-->
	<!--begin::Card body-->
	<div class="card-body p-9">
		<!--begin::Input group-->
		<div class="row mb-7">
			<!--begin::Label-->
			<label class="col-lg-2 fw-semibold text-muted"><%=jadwalPelajaran != null ? "Guru" : "Dosen"%></label>
			<!--end::Label-->
			<!--begin::Col-->
			<div class="col-lg-10 fv-row">

				<div class="d-flex align-items-center">

					<div class="symbol me-3 icon-elearning">
						<%=fotoDosen%>
					</div>
					<div class="d-flex justify-content-start flex-column">
						<%=dosen%>
					</div>
				</div>
			</div>
			<!--end::Col-->
		</div>
		<!--end::Input group-->

		<%
		if (perkuliahan != null) {
		%>
		<!--begin::Row-->
		<div class="row mb-7">
			<!--begin::Label-->
			<label class="col-lg-2 fw-semibold text-muted"><%= Common.getBahasaConfig("Jumlah SKS") %></label>
			<!--end::Label-->
			<!--begin::Col-->
			<div class="col-lg-10">
				<span class="fw-bold fs-6 text-gray-800"><%=perkuliahan == null ? "" : perkuliahan.getMatakuliah().getSks()%>
					SKS</span>
			</div>
			<!--end::Col-->
		</div>
		<!--end::Row-->
		<%
		}
		%>

		<!--begin::Input group-->
		<div class="row mb-7">
			<!--begin::Label-->
			<label class="col-lg-2 fw-semibold text-muted"><%=jadwalPelajaran != null ? "Tahun Ajaran" : "Tahun Akademik"%></label>
			<!--end::Label-->
			<!--begin::Col-->
			<div class="col-lg-10 fv-row">
				<span class="fw-semibold text-gray-800 fs-6"><%=perkuliahan == null ? (jadwalPelajaran != null ? jadwalPelajaran.getTahunAjaran() : "")
		: perkuliahan.getTahunAjaran()%></span>
			</div>
			<!--end::Col-->
		</div>
		<!--end::Input group-->
		<!--begin::Input group-->
		<div class="row mb-7">
			<!--begin::Label-->
			<label class="col-lg-2 fw-semibold text-muted"><%= Common.getBahasaConfig("Semester") %></label>
			<!--end::Label-->
			<!--begin::Col-->
			<div class="col-lg-10 fv-row">
				<span class="fw-semibold text-gray-800 fs-6"><%=perkuliahan == null
		? (jadwalPelajaran != null ? (jadwalPelajaran.getSemester().equals(1) ? Perkuliahan.GANJIL : Perkuliahan.GENAP)
				: "")
		: perkuliahan.getGanjilGenap()%></span>
			</div>
			<!--end::Col-->
		</div>
		<!--end::Input group-->

	</div>
	<!--end::Card body-->
</div>
<!--end::details View-->
<%
for (Pertemuan pertemuan : pertemuans) {
	if (pertemuan.getAktif()) {
		request.setAttribute("pertemuan", pertemuan);
	}
%>

<jsp:include page="/WEB-INF/o/ux/content/component/tampil_pertemuan.jsp">
	<jsp:param value="${pertemuan}" name="pertemuan" />
</jsp:include>
<%
}
%>
