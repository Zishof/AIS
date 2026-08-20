<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Detailperkuliahan"%>
<%@page import="java.util.ArrayList"%>
<%@page import="ais.action.master.helper.KrsUtilHelper"%>
<%@page
	import="ais.database.model.PembagianKuotaPerkuliahanBerdasarkantahunAngkatan"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.Kurikulum"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="java.util.List"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page
	import="ais.database.model.PembatasanNilaiIPKUntukPengambilanKRS"%>
<%@page import="ais.database.model.Mahasiswa"%>
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
Integer semesterPendek = null;
if (idsmt != null && idsmt.endsWith("3")) {
	semesterPendek = Perkuliahan.SEMESTER_PENDEK;
}
if (idsmt != null) {
	ta = (Integer.parseInt(idsmt.substring(0, 4))) + "/" + (Integer.parseInt(idsmt.substring(0, 4)) + 1);
	ganjil = Integer.parseInt(idsmt.substring(4, 5)) == 1;
}

String smt = (semesterPendek == null ? (ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP) : Perkuliahan.SP);
Mahasiswa mahasiswa = tbmuser.getMahasiswa();
Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), ta, ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP,
		mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

Fakultas fakultas = mahasiswa.getJurusan().getFakultas();
Jurusan jurusan = mahasiswa.getJurusan();

PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRS = semester.equals(1) ? null
		: Common.getIpkUntukPengambilanKRS(mahasiswa, semester, mahasiswa.getTahunangkatan(), fakultas, jurusan,
		mahasiswa.getProgram(), semesterPendek);

Integer maxsks = (Integer) (pembatasanNilaiIPKUntukPengambilanKRS == null
		? PembatasanNilaiIPKUntukPengambilanKRS.getDefaultPembatasanNilaiIpUntukAmbilKRS()
		: pembatasanNilaiIPKUntukPengambilanKRS.getBatasMaksimumIPKYangBolehDiambil());

KrsMahasiswa krsMahasiswa = mahasiswa == null ? null : Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null);

String taSmt = ta + " semester " + smt;

Session mySession = HibernateUtil.currentNativeSession();
Criteria criteria = mySession.createCriteria(Perkuliahan.class).add(Restrictions.eq("jurusan", jurusan))
		.add(Restrictions.eq("program", mahasiswa.getProgram())).add(Restrictions.eq("idSmt", idsmt));

List<Perkuliahan> perkuliahansTemp = ConstantValues
		.simpleList(criteria.setMaxResults(Common.ROWS_COUNT_ON_PAGE_50).setFirstResult(0), Perkuliahan.class);
// mySession.disconnect();
if (mySession.isOpen()) {mySession.disconnect();mySession.close();}
HibernateUtil.closeSession();

List<Perkuliahan> perkuliahans = new ArrayList<Perkuliahan>();
for (Perkuliahan perkuliahan : perkuliahansTemp) {
	if (perkuliahan != null && perkuliahan.getKurikulum() != null && perkuliahan.getKurikulum().bolehAmbil(mahasiswa)) {
		perkuliahans.add(perkuliahan);
	}
}
perkuliahansTemp = null;

List<Long> perkuliahanTelahDiambil = new ArrayList<Long>();

for (Long oid : mahasiswa.ambilDetailperkuliahan()) {
	Detailperkuliahan o = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
			oid.toString());
	if (o != null) {
		if (o.getPerkuliahan() != null) {
			perkuliahanTelahDiambil.add(o.getPerkuliahan().getId());
			if (o.getSemester().equals(semester)) {
				perkuliahanTelahDiambil.add(o.getPerkuliahan().getMatakuliah().getId());
			}
		}
	}
}
%>

<!DOCTYPE html>
<html lang="id">
<jsp:include page="/WEB-INF/o/ux/content/common/header.jsp"></jsp:include>
<!--begin::Body-->
<body data-kt-name="metronic" id="kt_app_body"
	data-kt-app-layout="dark-sidebar" data-kt-app-header-fixed="true"
	data-kt-app-sidebar-enabled="true" data-kt-app-sidebar-fixed="true"
	data-kt-app-sidebar-hoverable="true"
	data-kt-app-sidebar-push-header="true"
	data-kt-app-sidebar-push-toolbar="true"
	data-kt-app-sidebar-push-footer="true"
	data-kt-app-toolbar-enabled="true" class="app-default">
	<jsp:include page="/WEB-INF/o/ux/content/common/mode.jsp"></jsp:include>
	<!--begin::App-->
	<div class="d-flex flex-column flex-root app-root" id="kt_app_root">
		<!--begin::Page-->
		<div class="app-page flex-column flex-column-fluid" id="kt_app_page">


			<jsp:include page="/WEB-INF/o/ux/content/common/header_app.jsp"></jsp:include>


			<!--begin::Wrapper-->
			<div class="app-wrapper flex-column flex-row-fluid"
				id="kt_app_wrapper">


				<jsp:include page="/WEB-INF/o/ux/content/common/sidebar.jsp"></jsp:include>


				<!--begin:::Main-->
				<div class="app-main flex-column flex-row-fluid" id="kt_app_main">
					<!--begin::Content wrapper-->
					<div class="d-flex flex-column flex-column-fluid ">

						<jsp:include page="/WEB-INF/o/ux/content/common/page_title.jsp">
							<jsp:param value="KRS" name="judul" />
							<jsp:param value="Rencana Studi Mahasiswa" name="subjudul" />
							<jsp:param value="Tambah KRS" name="subjudul1" />
						</jsp:include>


						<form id="myForm<%=krsMahasiswa.getId()%>"
							action="<%=request.getContextPath()%>/pages/ux/krs.jsp"
							method="post">

							<!--begin::Content-->
							<div id="kt_app_content" class="app-content flex-column-fluid">
								<!--begin::Content container-->
								<div id="kt_app_content_container"
									class="app-container container-xxl">
									<!--begin::details View-->
									<div class="card mb-5 mb-xl-10" id="kt_profile_details_view">
										<!--begin::Card header-->
										<div class="card-header">
											<!--begin::Card title-->
											<div class="card-title align-items-start flex-column">
												<h3 class="fw-bold">Tambah Rencana Studi Mahasiswa</h3>
											</div>
											<!--end::Card title-->
											<!--begin::Action-->
											<div class="card-toolbar">

												<a href="#"
													onclick="document.getElementById('myForm<%=krsMahasiswa.getId()%>').submit();"
													class="btn btn-primary align-self-center btn-sm me-3">
													<span class="svg-icon svg-icon-2"> <i
														class="fas fa-check"></i>
												</span> Simpan
												</a> <a
													href="<%=request.getContextPath()%>/pages/ux/krs.jsp?idsmt=<%=idsmt%>"
													class="btn btn-danger align-self-center btn-sm"> <span
													class="svg-icon svg-icon-2"> <i class="fas fa-redo"></i>
												</span> Batal
												</a>

											</div>
										</div>
										<!--begin::Card header-->
										<!--begin::Card body-->
										<div class="card-body p-9">
											<!--begin::Row-->
											<div class="row mb-2">
												<!--begin::Label-->
												<label class="col-lg-4 text-muted"><%= Common.getBahasaConfig("Tahun Akademik") %></label>
												<!--end::Label-->
												<!--begin::Col-->
												<div class="col-lg-8">
													<span class="fw-semibold fs-6 text-gray-800"><%=taSmt%></span>
												</div>
												<!--end::Col-->
											</div>
											<!--end::Row-->
											<!--begin::Row-->
											<div class="row mb-2">
												<!--begin::Label-->
												<label class="col-lg-4 text-muted"><%= Common.getBahasaConfig("Maksimal Jumlah SKS yang dapat diambil") %></label>
												<!--end::Label-->
												<!--begin::Col-->
												<div class="col-lg-8">
													<span class="fw-semibold fs-6 text-gray-800"><%=maxsks%></span>
												</div>
												<!--end::Col-->
											</div>
											<!--end::Row-->
											<!--begin::Row-->
											<div class="row">
												<!--begin::Label-->
												<label class="col-lg-4 text-muted"><%= Common.getBahasaConfig("Jumlah SKS yang telah dipilih") %></label>
												<!--end::Label-->
												<!--begin::Col-->
												<div class="col-lg-8">
													<span class="fw-semibold fs-6 text-gray-800"><%=krsMahasiswa.getSksYangDiambil()%></span>
												</div>
												<!--end::Col-->
											</div>
											<!--end::Row-->
										</div>
										<!--end::Card body-->
										<!--begin:::Tab content-->
										<div class="tab-content" id="myTabContent">
											<!--begin:::Tab pane-->
											<div class="tab-pane fade show active"
												id="kt_ecommerce_customer_general" role="tabpanel">
												<div class="card-body pt-2">
													<div class="table-responsive">
														<!--begin::Table-->
														<table
															class="table table-rounded table-striped border gy-5 gs-7"
															id="kt_table_widget_4_table">
															<!--begin::Table head-->
															<thead>
																<!--begin::Table row-->
																<tr
																	class="fw-semibold fs-6 text-gray-800 border-bottom-2 border-gray-200">
																	<th class="min-w-50px"></th>
																	<th class="min-w-50px"><%= Common.getBahasaConfig("Kode") %></th>
																	<th class="min-w-150px"><%= Common.getBahasaConfig("Nama Mata Kuliah") %></th>
																	<th class="min-w-50px"><%= Common.getBahasaConfig("Kelas") %></th>
																	<th class="min-w-150px"><%= Common.getBahasaConfig("Dosen Pengajar") %></th>
																	<th class="min-w-100px"><%= Common.getBahasaConfig("Ruang") %></th>
																	<th class="min-w-50px"><%= Common.getBahasaConfig("Hari") %></th>
																	<th class="min-w-100px"><%= Common.getBahasaConfig("Waktu") %></th>
																	<th class="min-w-50px"><%= Common.getBahasaConfig("SKS") %></th>
																	<th class="min-w-50px"><%= Common.getBahasaConfig("Kap.") %></th>
																	<th class="min-w-50px"><%= Common.getBahasaConfig("Status") %></th>
																</tr>
																<!--end::Table row-->
															</thead>
															<!--end::Table head-->
															<!--begin::Table body-->
															<tbody class="text-gray-600">

																<%
																for (Perkuliahan perkuliahan : perkuliahans) {
																	Matakuliah matakuliah = perkuliahan.getMatakuliah();
																	Kurikulum kurikulum = perkuliahan.getKurikulum();
																	String dosen = "";
																	List<Dosen> dosens = perkuliahan.populateDosenBuNama();
																	for (Dosen dd : dosens) {
																		dosen += dosen.isEmpty() ? dd.getNama() : ", " + dd.getNama();
																	}

																	Integer kapasitasKelas = perkuliahan.getKapasitasKelas();
																	mySession = HibernateUtil.currentNativeSession();
																	PembagianKuotaPerkuliahanBerdasarkantahunAngkatan pembagianKuotaPerkuliahanBerdasarkantahunAngkatan = KrsUtilHelper
																	.ambilPembagianKuotaPerkuliahanBerdasarkantahunAngkatan(mySession, perkuliahan,
																			mahasiswa.getTahunangkatan(), false);

																	Number kuota = pembagianKuotaPerkuliahanBerdasarkantahunAngkatan == null ? null
																	: pembagianKuotaPerkuliahanBerdasarkantahunAngkatan.getKuota();
																	if (kuota != null) {
																		kapasitasKelas = kuota.intValue();
																	}

																	Integer jumlahUdahMasuk = KrsUtilHelper.ambilJumlahDetailperkuliahan(mySession, perkuliahan, false);
																	// mySession.disconnect();
																	if (mySession.isOpen()) {mySession.disconnect();mySession.close();}
																	HibernateUtil.closeSession();
																%>

																<tr>

																	<%
																	if (perkuliahanTelahDiambil.contains(perkuliahan.getId()) || jumlahUdahMasuk >= kapasitasKelas) {
																	%>
																	<!--begin::Checkbox-->
																	<td></td>
																	<!--end::Checkbox-->

																	<%
																	} else {
																	%>
																	<!--begin::Checkbox-->
																	<td>
																		<div
																			class="form-check form-check-sm form-check-custom form-check-solid">
																			<input class="form-check-input" type="checkbox"
																				name="perkuliahan" value="<%=perkuliahan.getId()%>" />
																		</div>
																	</td>
																	<!--end::Checkbox-->
																	<%
																	}
																	%>


																	<td class=""><%=matakuliah.getKode()%></td>
																	<td class=""><%=matakuliah.getNama()%> (<%=kurikulum == null ? "" : kurikulum.getNama()%>)</td>
																	<td class=""><%=perkuliahan.getKelas()%></td>
																	<td class=""><%=dosen%></td>
																	<td class=""><%=perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama()%></td>
																	<td class=""><%=perkuliahan.getHari()%></td>
																	<td class=""><%=(perkuliahan.getWaktuMulai() == null ? "" : "(" + perkuliahan.getWaktuMulai())%>
																		<%=(perkuliahan.getWaktuSelesai() == null ? "" : "s.d " + perkuliahan.getWaktuSelesai() + ")")%></td>
																	<td class=""><%=matakuliah.getSks()%></td>
																	<td class=""><%=jumlahUdahMasuk%>/<%=kapasitasKelas%></td>

																	<%
																	if (perkuliahanTelahDiambil.contains(perkuliahan.getId())) {
																	%>

																	<td class=""><span
																		class="badge badge-light-danger fw-bold fs-8">Dipilih</span></td>
																	<%
																	} else if (jumlahUdahMasuk >= kapasitasKelas) {
																	%>
																	<td class=""><span
																		class="badge badge-light-danger fw-bold fs-8">Penuh</span></td>
																	<%
																	} else {
																	%>
																	<td class=""><span
																		class="badge badge-light-success fw-bold fs-8">Tersedia</span></td>
																	<%
																	}
																	%>
																</tr>
																<%
																}
																%>
															</tbody>
															<!--end::Table body-->
														</table>
														<!--end::Table-->
													</div>
												</div>
												<!--end::Card body-->
											</div>
											<!--end::tab pane-->
										</div>
										<!--end::tab content-->
									</div>
									<!--end::details View-->
								</div>
								<!--end::Content container-->
							</div>
							<!--end::Content-->
						</form>

					</div>
					<!--end::Content wrapper-->
					<jsp:include page="/WEB-INF/o/ux/content/common/app_footer.jsp"></jsp:include>
				</div>
				<!--end:::Main-->
			</div>
			<!--end::Wrapper-->
		</div>
		<!--end::Page-->
	</div>
	<!--end::App-->


	<!--end::Drawers-->

	<jsp:include page="/WEB-INF/o/ux/content/common/footer.jsp"></jsp:include>
<jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp"/>
</body>
<!--end::Body-->
</html>