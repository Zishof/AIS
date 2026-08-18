<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="java.util.HashMap"%>
<%@page import="ais.database.model.KategoriPengumuman"%>
<%@page import="java.util.Map"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.PengumumanAkademis"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.action.master.TampilanPengumumanAkademisAction"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Detailperkuliahan"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.model.KrsMahasiswa"%>

<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
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

List<Long> detailperkuliahansData = tbmuser.getMahasiswa() == null ? new ArrayList<Long>()
		: Common.getDetailperkuliahans(tbmuser.getMahasiswa(), krsMahasiswa.getSemester(), krsMahasiswa.getTahapan(),
		null, krsMahasiswa.getSemesterPendek(), false, false, false, false);
%>


<!--begin::Row-->
<div class="row g-5 g-xl-8">

	<%
	for (Long detailperkuliahanid : detailperkuliahansData) {
		Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
		detailperkuliahanid.toString());
		if (detailperkuliahan != null) {
			Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();
			if (perkuliahan != null) {
		Matakuliah matakuliah = perkuliahan.getMatakuliah();

		String dosen = "";
		List<Dosen> dosens = perkuliahan.populateDosenBuNama();
		for (Dosen dd : dosens) {
			dosen += dosen.isEmpty() ? dd.getNama() : ", " + dd.getNama();
		}
	%>

	<div class="col-xl-4">
		<!--begin::Statistics Widget 1-->
		<div class="card bgi-no-repeat card-xl-stretch mb-5 mb-xl-8"
			style="background-position: right top; background-size: 30% auto; background-image: url('<%=request.getContextPath()%>/component/assets/media/svg/shapes/abstract-1.svg');">
			<!--begin::Body-->
			<div class="card-body">
				<a href="javascript:void(0)0"
					class="card-title fw-bold text-muted text-hover-primary fs-4"><%=matakuliah.getNama()%></a>
				<div class="fw-bold text-primary my-6"><%=dosen%></div>
				<div class="fw-plain text-gray my-6" style="font-size: 11px"><%=perkuliahan.getDeskripsiPembelajaran()%></div>
				<div class="fw-plain text-gray my-6" style="font-size: 11px"><%=perkuliahan.getCapaianPembelajaranProdi()%></div>
				<div class="row mb-7">
					<!--begin::Label-->
					<label class="col-lg-4 fw-semibold text-muted"><%= Common.getBahasaConfig("Jumlah SKS") %></label>
					<!--end::Label-->
					<!--begin::Col-->
					<div class="col-lg-8 fv-row">
						<span class="fw-semibold text-gray-800 fs-6"><%=matakuliah.getSks()%>
							SKS</span>
					</div>
					<!--end::Col-->
				</div>
				<div class="row mb-7">
					<!--begin::Label-->
					<label class="col-lg-4 fw-semibold text-muted"><%= Common.getBahasaConfig("Jadwal") %></label>
					<!--end::Label-->
					<!--begin::Col-->
					<div class="col-lg-8 fv-row">
						<span class="fw-semibold text-gray-800 fs-6"> <%=(perkuliahan.getHari() == null ? "" : perkuliahan.getHari())%>
							<%=(perkuliahan.getWaktuMulai() == null ? "" : "(" + perkuliahan.getWaktuMulai())%>
							<%=(perkuliahan.getWaktuSelesai() == null ? "" : "s.d " + perkuliahan.getWaktuSelesai() + ")")%>
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
					href="<%=request.getContextPath()%>/pages/ux/elearning_detail.jsp?id=<%=perkuliahan.getId()%>"
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