<!--begin:::Tab content-->
<%@page import="java.util.Map"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.TreeMap"%>
<%@page
	import="ais.database.model.PembagianKuotaPerkuliahanBerdasarkantahunAngkatan"%>
<%@page import="ais.action.master.helper.KrsUtilHelper"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page
	import="ais.database.model.PembatasanNilaiIPKUntukPengambilanKRS"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.KrsMahasiswa"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.model.Kurikulum"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.Detailperkuliahan"%>

<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getMahasiswa() == null) {
	response.sendRedirect(request.getContextPath() + "/logoff");
	return;
}
Mahasiswa mahasiswa = tbmuser.getMahasiswa();

String ta = Common.getCurrentTahunAkademik();
boolean ganjil = Common.isNowSemensterGanjil();
String idsmt = request.getParameter("idsmt");

try {
	if (idsmt == null) {
		idsmt = ta.substring(0, 4) + (ganjil ? "1" : "2");
	}
} catch (Exception e) {
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/o/ux/absen.jsp:45");
}

//System.out.println("idsmt -> " + idsmt);

Integer semesterPendek = null;
if (idsmt != null && idsmt.endsWith("3")) {
	semesterPendek = Perkuliahan.SEMESTER_PENDEK;
}
if (idsmt != null) {
	ta = (Integer.parseInt(idsmt.substring(0, 4))) + "/" + (Integer.parseInt(idsmt.substring(0, 4)) + 1);
	ganjil = Integer.parseInt(idsmt.substring(4, 5)) == 1;
}

Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), ta, ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP,
		mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
KrsMahasiswa krsMahasiswa = mahasiswa == null ? null : Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null);

Fakultas fakultas = mahasiswa.getJurusan().getFakultas();
Jurusan jurusan = mahasiswa.getJurusan();

PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRS = semester.equals(1) ? null
		: Common.getIpkUntukPengambilanKRS(mahasiswa, semester, mahasiswa.getTahunangkatan(), fakultas, jurusan,
		mahasiswa.getProgram(), semesterPendek);

Integer maxsks = (Integer) (pembatasanNilaiIPKUntukPengambilanKRS == null
		? PembatasanNilaiIPKUntukPengambilanKRS.getDefaultPembatasanNilaiIpUntukAmbilKRS()
		: pembatasanNilaiIPKUntukPengambilanKRS.getBatasMaksimumIPKYangBolehDiambil());

List<Long> detailperkuliahansTemp = mahasiswa.ambilDetailperkuliahan(semester, null, semesterPendek);
request.setAttribute("krsMahasiswa", krsMahasiswa);
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
							<jsp:param value="Kehadiran" name="judul" />
							<jsp:param value="Absensi Kehadiran Mahasiswa" name="subjudul" />
							<jsp:param value="Rekap Kehadiran Mahasiswa" name="subjudul1" />
						</jsp:include>



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
											<h3 class="fw-bold">Daftar Kehadiran</h3>
										</div>
										<!--end::Card title-->

									</div>
									<!--begin::Card header-->

									<jsp:include page="/WEB-INF/o/ux/content/common/info_mhs.jsp">
										<jsp:param value="${krsMahasiswa}" name="krsMahasiswa" />
									</jsp:include>

									<jsp:include
										page="/WEB-INF/o/ux/content/elearning/pilihan_tahun_akademik.jsp"></jsp:include>

									<!--begin:::Tab content-->
									<div class="tab-content" id="myTabContent">
										<!--begin:::Tab pane-->
										<div class="tab-pane fade show active" id="kt_tab_pane_1"
											role="tabpanel">
											<div class="card-body pt-2">
												<div class="table-responsive">

													<table id="kt_datatable_nilai"
														class="table table-striped table-row-bordered gy-5 gs-7 border rounded w-100 align-middle">
														<thead>
															<tr class="fw-semibold fs-6 text-gray-800">
																<th class="min-w-50px"><%= Common.getBahasaConfig("Kode") %></th>
																<th class="min-w-150px"><%= Common.getBahasaConfig("Mata kuliah") %></th>
																<th class="min-w-50px"><%= Common.getBahasaConfig("Kelas") %></th>
																<th class="min-w-100px"><%= Common.getBahasaConfig("Dosen") %></th>
																<th class="min-w-50px"><%= Common.getBahasaConfig("SKS") %></th>
																<th class="min-w-50px"><%= Common.getBahasaConfig("Hadir") %></th>
																<th class="min-w-50px"><%= Common.getBahasaConfig("Izin") %></th>
																<th class="min-w-50px"><%= Common.getBahasaConfig("Sakit") %></th>
																<th class="min-w-50px"><%= Common.getBahasaConfig("Alpa") %></th>
																<th class="min-w-50px"><%= Common.getBahasaConfig("TDK Hadir") %></th>
																<th class="min-w-50px"><%= Common.getBahasaConfig("Blm") %></th>
																<th class="min-w-75px"><%= Common.getBahasaConfig("Action") %></th>
															</tr>
														</thead>
														<tbody>
															<%
															for (Long detailperkuliahanid : detailperkuliahansTemp) {
																Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
																detailperkuliahanid.toString());

																if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null
																&& detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)) {

																	Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();

																	TreeMap<String, Long> pertemuanss = perkuliahan.ambilPertemuan();
																	List<Pertemuan> pertemuans = new ArrayList<Pertemuan>();
																	for (Long pertemuanid : pertemuanss.values()) {
																Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
																if (pertemuan != null) {
																	pertemuans.add(pertemuan);
																}
																	}
																	pertemuanss.clear();
																	pertemuanss = null;

																	Object[] jml = perkuliahan.ambilJumlahPertemuanStatistik(pertemuans, mahasiswa, null, true, true);
																	Map<String, Integer> statuses = (Map<String, Integer>) (jml == null || jml[4] == null ? null : jml[4]);

																	Matakuliah matakuliah = detailperkuliahan.getPerkuliahan().getMatakuliah();
																	Kurikulum kurikulum = detailperkuliahan.getPerkuliahan().getKurikulum();
																	String dosen = "";
																	List<Dosen> dosens = detailperkuliahan.getPerkuliahan().populateDosenBuNama();
																	for (Dosen dd : dosens) {
																dosen += dosen.isEmpty() ? dd.getNama() : ", " + dd.getNama();
																	}
															%>
															<tr>
																<td class=""><%=matakuliah.getKode()%></td>
																<td class=""><%=matakuliah.getNama()%> (<%=kurikulum == null ? "" : kurikulum.getNama()%>)</td>
																<td class=""><%=detailperkuliahan.getPerkuliahan().getKelas()%></td>
																<td class=""><%=dosen%></td>

																<td class=""><%=matakuliah.getSks()%></td>
																<td class=""><%=statuses.get("M") == null ? 0 : statuses.get("M")%></td>
																<td class=""><%=statuses.get("I") == null ? 0 : statuses.get("I")%></td>
																<td class=""><%=statuses.get("S") == null ? 0 : statuses.get("S")%></td>
																<td class=""><%=statuses.get("A") == null ? 0 : statuses.get("A")%></td>
																<td class=""><%=(statuses.get("I") == null ? 0 : statuses.get("I")) + (statuses.get("S") == null ? 0 : statuses.get("S"))
		+ (statuses.get("A") == null ? 0 : statuses.get("A"))%></td>
																<td class=""><%=(pertemuans.size() - ((statuses.get("M") == null ? 0 : statuses.get("M"))
		+ (statuses.get("I") == null ? 0 : statuses.get("I")) + (statuses.get("S") == null ? 0 : statuses.get("S"))
		+ (statuses.get("A") == null ? 0 : statuses.get("A"))))%></td>
																	<td><a
																	href="<%=request.getContextPath()%>/pages/ux/content/absensi/absensi-detail.jsp?id=<%=detailperkuliahanid%>"
																	class="btn btn-light-primary btn-sm">Lihat</a></td>
															</tr>


															<%
															}
															}
															%>

														</tbody>
														<tfoot>
															<tr class="fw-bold fs-6 border-top align-middle">
																<td colspan="2">Jumlah SKS</td>
																<td><%=Common.numberFormat.get().format(krsMahasiswa.getSksYangDiambil())%></td>
																<td colspan="2">IPS</td>
																<td><%=Common.numberFormat.get().format(krsMahasiswa.getIps())%></td>
																<td colspan="2">IPK</td>
																<td><%=Common.numberFormat.get().format(krsMahasiswa.getIpk())%></td>
															</tr>

														</tfoot>



													</table>
													<!-- <table id="kt_datatable_nilai_2" class="table table-striped table-row-bordered gy-5 gs-7 border rounded w-100">
                                                        
                                                            <tfoot>
                                                                <tr class="fw-semibold fs-6 text-gray-800 border align-middle">
                                                                    <td rowspan="2">Jumlah</td>
                                                                    <td colspan="2">SKS</td>
                                                                    <td colspan="2">Nilai</td>
                                                                    <td colspan="2">IPKs</td>
                                                                    
                                                                </tr>
                                                                <tr class="fw-semibold fs-6 text-gray-800 border align-middle">
                                                                    <td colspan="2">35</td>
                                                                    <td colspan="2">126</td>
                                                                    <td colspan="2">3,60</td>
                                                                    
                                                                </tr>
                                                            </tfoot>
                                                        </table> -->
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
</body>
<!--end::Body-->
</html>