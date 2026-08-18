<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.database.model.Ruang"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.Detailperkuliahan"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.KrsMahasiswa"%>
<%@ page import="ais.common.Common" %>
<%
KrsMahasiswa krsMahasiswa = (KrsMahasiswa) request.getAttribute("krsMahasiswa");
Mahasiswa mahasiswa = krsMahasiswa.getMahasiswa();
Detailperkuliahan detailperkuliahan = (Detailperkuliahan) request.getAttribute("detailperkuliahan");
%>

<!--begin::Card body-->
<div class="card-body p-9">
	<!--begin::Row-->
	<div class="row mb-5">
		<!--begin::Label-->
		<label class="col-lg-4 fw-semibold text-muted"><%= Common.getBahasaConfig("Nama Mahasiswa") %></label>
		<!--end::Label-->
		<!--begin::Col-->
		<div class="col-lg-8">
			<span class="fw-semibold text-gray-800 fs-6"><%=mahasiswa.getNama()%></span>
		</div>
		<!--end::Col-->
	</div>
	<!--end::Row-->
	<!--begin::Input group-->
	<div class="row mb-5">
		<!--begin::Label-->
		<label class="col-lg-4 fw-semibold text-muted"><%= Common.getBahasaConfig("NIM") %></label>
		<!--end::Label-->
		<!--begin::Col-->
		<div class="col-lg-8 fv-row">
			<span class="fw-semibold text-gray-800 fs-6"><%=mahasiswa.getNim()%></span>
		</div>
		<!--end::Col-->
	</div>
	<!--end::Input group-->
	<!--begin::Input group-->
	<div class="row mb-5">
		<!--begin::Label-->
		<label class="col-lg-4 fw-semibold text-muted"><%= Common.getBahasaConfig("Program") %></label>
		<!--end::Label-->
		<!--begin::Col-->
		<div class="col-lg-8">
			<span class="fw-semibold text-gray-800 fs-6"><%=mahasiswa.getProgram()%></span>
		</div>
		<!--end::Col-->
	</div>
	<!--end::Input group-->
	<!--begin::Input group-->
	<div class="row mb-5">
		<!--begin::Label-->
		<label class="col-lg-4 fw-semibold text-muted"><%= Common.getBahasaConfig("Prodi") %> </label>
		<!--end::Label-->
		<!--begin::Col-->
		<div class="col-lg-8">
			<span class="fw-semibold text-gray-800 fs-6"><%=mahasiswa.getJurusan().getNama()%></span>
		</div>
		<!--end::Col-->
	</div>
	<!--end::Input group-->
	<!--begin::Input group-->
	<div class="row mb-5">
		<!--begin::Label-->
		<label class="col-lg-4 fw-semibold text-muted"><%= Common.getBahasaConfig("Fakultas") %></label>
		<!--end::Label-->
		<!--begin::Col-->
		<div class="col-lg-8">
			<span class="fw-semibold text-gray-800 fs-6"><%=mahasiswa.getJurusan().getFakultas().getNama()%></span>
		</div>
		<!--end::Col-->
	</div>
	<!--end::Input group-->

	<!--begin::Input group-->
	<div class="row mb-5">
		<!--begin::Label-->
		<label class="col-lg-4 fw-semibold text-muted"><%= Common.getBahasaConfig("Semester / Kelas") %></label>
		<!--end::Label-->
		<!--begin::Col-->
		<div class="col-lg-8">
			<span class="fw-semibold text-gray-800 fs-6"><%=krsMahasiswa.getSemester() + " / " + krsMahasiswa.getKelas()%></span>
		</div>
		<!--end::Col-->
	</div>
	<!--end::Input group-->

	<!--begin::Input group-->
	<div class="row mb-5">
		<!--begin::Label-->
		<label class="col-lg-4 fw-semibold text-muted"><%= Common.getBahasaConfig("Dosen Pembimbing/Wali") %></label>
		<!--end::Label-->
		<!--begin::Col-->
		<div class="col-lg-8">
			<span class="fw-semibold text-gray-800 fs-6"><%=krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNama()%></span>
		</div>
		<!--end::Col-->
	</div>
	<!--end::Input group-->


	<%
	if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null) {
		Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();
		Matakuliah matakuliah = perkuliahan.getMatakuliah();
		Ruang ruang = perkuliahan.getRuang();
		String dosen = "";
		List<Dosen> dosens = perkuliahan.populateDosenBuNama();
		for (Dosen dd : dosens) {
			dosen += dosen.isEmpty() ? dd.getNama() : ", " + dd.getNama();
		}
	%>


	<!--begin::Row-->
	<div class="row mb-5">
		<!--begin::Label-->
		<label class="col-lg-4 fw-semibold text-muted"><%= Common.getBahasaConfig("Mata Kuliah") %></label>
		<!--end::Label-->
		<!--begin::Col-->
		<div class="col-lg-8">
			<span class="fw-bold text-gray-800 fs-6"><%=matakuliah.getKode()%>-<%=matakuliah.getNama()%></span>
		</div>
		<!--end::Col-->
	</div>
	<!--end::Row-->
	<!--begin::Row-->
	<div class="row mb-5">
		<!--begin::Label-->
		<label class="col-lg-4 fw-semibold text-muted"><%= Common.getBahasaConfig("Jumlah SKS") %></label>
		<!--end::Label-->
		<!--begin::Col-->
		<div class="col-lg-8">
			<span class="fw-bold text-gray-800 fs-6"><%=matakuliah.getSks()%>
				SKS</span>
		</div>
		<!--end::Col-->
	</div>
	<!--end::Row-->
	<!--begin::Row-->
	<div class="row mb-5">
		<!--begin::Label-->
		<label class="col-lg-4 fw-semibold text-muted"><%= Common.getBahasaConfig("Dosen") %></label>
		<!--end::Label-->
		<!--begin::Col-->
		<div class="col-lg-8">
			<span class="fw-bold text-gray-800 fs-6"><%=dosen%></span>
		</div>
		<!--end::Col-->
	</div>
	<!--end::Row-->
	<!--begin::Row-->
	<div class="row mb-5">
		<!--begin::Label-->
		<label class="col-lg-4 fw-semibold text-muted"><%= Common.getBahasaConfig("Ruang") %></label>
		<!--end::Label-->
		<!--begin::Col-->
		<div class="col-lg-8">
			<span class="fw-bold text-gray-800 fs-6"><%=ruang == null ? "" : ruang.getNama()%></span>
		</div>
		<!--end::Col-->
	</div>
	<!--end::Row-->
	<!--begin::Row-->
	<div class="row mb-10">
		<!--begin::Label-->
		<label class="col-lg-4 fw-semibold text-muted"><%= Common.getBahasaConfig("Jadwal") %></label>
		<!--end::Label-->
		<!--begin::Col-->
		<div class="col-lg-8">
			<span class="fw-bold text-gray-800 fs-6"><%=perkuliahan.getHari()%>
				Pukul <%=(perkuliahan.getWaktuMulai() == null ? "" : "(" + perkuliahan.getWaktuMulai())%>
				<%=(perkuliahan.getWaktuSelesai() == null ? "" : "s.d " + perkuliahan.getWaktuSelesai() + ")")%></span>
		</div>
		<!--end::Col-->
	</div>
	<!--end::Row-->


	<%
	}
	%>

</div>
<!--end::Card body-->